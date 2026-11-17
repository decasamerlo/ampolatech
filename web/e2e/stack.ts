import { execFileSync } from "node:child_process";
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

/** Runs `docker compose` on the tests' stack, its ports set here whatever the shell holds. */
export function compose(...args: string[]): void {
	dockerCompose("inherit", args);
}

/** Runs `docker compose` on the tests' stack as `compose` does, writing its output to the open file `fd`. */
export function composeInto(fd: number, ...args: string[]): void {
	dockerCompose(fd, args);
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
	const lines = execFileSync(
		"docker",
		["ps", "--filter", `publish=${port}`, "--format", holderFormat],
		{ encoding: "utf8", stdio: ["ignore", "pipe", "inherit"] },
	);
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

function dockerCompose(stdout: "inherit" | number, args: string[]): void {
	execFileSync(
		"docker",
		["compose", "--file", composeFile, "--project-name", project, ...args],
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
