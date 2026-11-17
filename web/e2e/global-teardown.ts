import { closeSync, mkdirSync, openSync } from "node:fs";
import { join } from "node:path";
import type { FullConfig } from "@playwright/test";
import { compose, composeInto } from "./stack";

// Playwright runs the teardown after a failed global setup as well as after
// the tests, and `down` deletes the containers' logs with them, so the logs
// are written first: a failed run keeps the backend's log, even when the
// backend failed to start. `--rmi local` removes the two images Compose
// built for the project, which is this checkout's alone, so no other run
// would remove them.
export default function globalTeardown(config: FullConfig): void {
	try {
		writeStackLog(config);
	} finally {
		compose("down", "--volumes", "--rmi", "local");
	}
}

/** Writes the stack's logs to stack.log, beside the traces in the output folder, which Playwright empties when a run starts. */
function writeStackLog(config: FullConfig): void {
	// Both projects write to the one output folder the config sets.
	const outputDir = config.projects[0]?.outputDir;
	if (outputDir === undefined) {
		throw new Error(
			"playwright.config.ts defines no project, so no output folder for stack.log",
		);
	}
	mkdirSync(outputDir, { recursive: true });
	const log = openSync(join(outputDir, "stack.log"), "w");
	try {
		composeInto(log, "logs", "--no-color", "--timestamps");
	} finally {
		closeSync(log);
	}
}
