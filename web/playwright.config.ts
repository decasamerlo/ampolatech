import { defineConfig, devices } from "@playwright/test";
import { ports } from "./e2e/stack";

export default defineConfig({
	testDir: "e2e",
	globalSetup: "./e2e/global-setup.ts",
	globalTeardown: "./e2e/global-teardown.ts",
	use: { baseURL: `http://127.0.0.1:${ports.web}` },
	// docs/conventions/testing.md's "The web app end to end" names these two
	// projects, and why.
	projects: [
		{ name: "Desktop Chrome", use: devices["Desktop Chrome"] },
		{ name: "Galaxy Tab S9", use: devices["Galaxy Tab S9"] },
	],
});
