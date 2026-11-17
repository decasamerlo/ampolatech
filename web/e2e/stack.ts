import { execFileSync, type StdioOptions } from "node:child_process";
import { createHash } from "node:crypto";
import { dirname } from "node:path";
import { fileURLToPath } from "node:url";

// The whole app from the repository's compose.yaml, on ports of the tests'
// own and under a Compose project of this checkout's own, so a run leaves
// alone the stack `docker compose up` runs and the stack another checkout's
// run started. The project is named after a hash of the checkout's path, not
// after its folder: folders Compose names alike would share a project, and
// the folder's own name is the project `docker compose up` runs there.
const composeFile = fileURLToPath(
	new URL("../../compose.yaml", import.meta.url),
);
const checkout = dirname(composeFile);
const project = `ampolatech-e2e-${createHash("sha256").update(checkout).digest("hex").slice(0, 8)}`;

// The same in every checkout, so one run goes at a time on a machine.
export const ports = { web: 28081, backend: 28080, postgres: 25432 };

// Each Docker call this file makes has a time limit. Past it, Node stops the
// call and the function that made it throws, so a call that hangs fails the
// run as any other failure does. On CI, the longest path, counted with every
// call at its limit, must stay under the `web` job's `timeout-minutes` in
// .github/workflows/ci.yml: a job past its timeout is cancelled, not failed,
// and the step that keeps the browser tests' record runs on failure() alone.
// That path is the setup's `down` (1 minute) and its three `docker ps` port
// checks (3 × 1), then `up --wait` (10), then the teardown's `logs` (1) and
// `down` (1), 16 minutes, plus the tests' own time and the job's other
// steps.

/** Minutes `up --wait` may run: it builds both images, then waits until the app is healthy. */
export const upLimitMinutes = 10;

/** Minutes every Docker call but `up --wait` may run. */
export const otherCallLimitMinutes = 1;

/** Runs `docker compose` on the tests' stack, its ports set here whatever the shell holds, and stops it after `limitMinutes`. */
export function compose(limitMinutes: number, ...args: string[]): void {
	dockerCompose("inherit", limitMinutes, args);
}

/** Runs `docker compose` on the tests' stack as `compose` does, writing its output to the open file `fd`. */
export function composeInto(
	fd: number,
	limitMinutes: number,
	...args: string[]
): void {
	dockerCompose(fd, limitMinutes, args);
}

/**
 * Throws while containers of another Compose project publish one of the
 * tests' ports, naming each such project and the command that removes it.
 * It removes nothing itself: a stack that a stopped run left looks the same
 * as the stack of a run under way. A port held by anything else, a container
 * outside Compose included, is left for `up` to refuse.
 */
export function stopIfAnotherProjectHoldsThePorts(): void {
	const holders = Object.values(ports)
		.flatMap(containersPublishing)
		.filter((holder) => holder.project !== "" && holder.project !== project);
	if (holders.length === 0) {
		return;
	}
	const projects = [...new Set(holders.map((holder) => holder.project))];
	throw new Error(
		[
			"Containers of another Compose project hold the browser tests' ports, so this run stops before it builds anything.",
			...projects.map((name) =>
				describeProject(holders.filter((holder) => holder.project === name)),
			),
		].join("\n\n"),
	);
}

type PortHolder = {
	port: number;
	project: string;
	folder: string;
	composeFiles: string;
	container: string;
	status: string;
};

// One JSON object per container. A container outside Compose has none of
// the labels, and `.Label` gives "" for each.
const holderFormat =
	'{"project":{{json (.Label "com.docker.compose.project")}},' +
	'"folder":{{json (.Label "com.docker.compose.project.working_dir")}},' +
	'"composeFiles":{{json (.Label "com.docker.compose.project.config_files")}},' +
	'"container":{{json .Names}},"status":{{json .Status}}}';

function containersPublishing(port: number): PortHolder[] {
	const lines = docker(
		["ps", "--filter", `publish=${port}`, "--format", holderFormat],
		otherCallLimitMinutes,
		{ stdio: ["ignore", "pipe", "inherit"] },
	);
	if (lines === null) {
		throw new Error(
			"`docker ps` gave no output to read: its stdout must be piped",
		);
	}
	return lines
		.split("\n")
		.filter((line) => line !== "")
		.map((line) => ({
			port,
			...(JSON.parse(line) as Omit<PortHolder, "port">),
		}));
}

function describeProject(holders: PortHolder[]): string {
	const [{ project: name, folder, composeFiles }] = holders as [PortHolder];
	return [
		`  Project:       ${name}`,
		`  Started from:  ${folder}`,
		`  Compose file:  ${composeFiles}`,
		...holders.map(
			({ port, container, status }) =>
				`  Port ${port}:    ${container}, ${status}`,
		),
		"",
		"A browser-test run may be under way there. If none is, this removes the stack:",
		"",
		`  docker compose --project-name ${name} down --volumes --rmi local`,
	].join("\n");
}

function dockerCompose(
	stdout: "inherit" | number,
	limitMinutes: number,
	args: string[],
): void {
	docker(
		["compose", "--file", composeFile, "--project-name", project, ...args],
		limitMinutes,
		{
			stdio: ["inherit", stdout, "inherit"],
			env: {
				...process.env,
				AMPOLATECH_WEB_PORT: String(ports.web),
				AMPOLATECH_BACKEND_PORT: String(ports.backend),
				AMPOLATECH_POSTGRES_PORT: String(ports.postgres),
			},
		},
	);
}

/**
 * Runs `docker` with `args` and returns what it writes to stdout when
 * `options.stdio` pipes it, or null when it doesn't. Once it runs past
 * `limitMinutes`, Node sends it SIGTERM and waits for it to exit, and this
 * throws an error that names the command and the limit, which Node's own,
 * `spawnSync docker ETIMEDOUT`, leaves out.
 */
function docker(
	args: string[],
	limitMinutes: number,
	options: { stdio: StdioOptions; env?: NodeJS.ProcessEnv },
): string | null {
	try {
		return execFileSync("docker", args, {
			...options,
			encoding: "utf8",
			timeout: limitMinutes * 60_000,
			killSignal: "SIGTERM",
		});
	} catch (error) {
		if (
			error instanceof Error &&
			"code" in error &&
			error.code === "ETIMEDOUT"
		) {
			throw new Error(
				`\`docker ${shellLine(args)}\` ran past its time limit, ${limitMinutes} ${limitMinutes === 1 ? "minute" : "minutes"}, and was stopped.`,
				{ cause: error },
			);
		}
		throw error;
	}
}

/** `args` as one line a shell splits back into the same words: an argument with any character but letters, digits and `_@%+=:,./-` goes in single quotes. */
function shellLine(args: string[]): string {
	return args
		.map((arg) =>
			/^[\w@%+=:,./-]+$/.test(arg) ? arg : `'${arg.replaceAll("'", `'\\''`)}'`,
		)
		.join(" ");
}
