import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// The whole app from the repository's compose.yaml, under a Compose project
// and ports of its own, so the tests run beside a stack already running,
// such as one on compose.yaml's default ports.
const project = "ampolatech-e2e";
const composeFile = fileURLToPath(
	new URL("../../compose.yaml", import.meta.url),
);

export const ports = { web: 28081, backend: 28080, postgres: 25432 };

/** Runs `docker compose` on the tests' stack, its ports set here whatever the shell holds. */
export function compose(...args: string[]): void {
	execFileSync(
		"docker",
		["compose", "--file", composeFile, "--project-name", project, ...args],
		{
			stdio: "inherit",
			env: {
				...process.env,
				AMPOLATECH_WEB_PORT: String(ports.web),
				AMPOLATECH_BACKEND_PORT: String(ports.backend),
				AMPOLATECH_POSTGRES_PORT: String(ports.postgres),
			},
		},
	);
}
