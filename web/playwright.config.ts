import { defineConfig, devices } from "@playwright/test";
import { ports } from "./e2e/stack";

export default defineConfig({
	testDir: "e2e",
	globalSetup: "./e2e/global-setup.ts",
	globalTeardown: "./e2e/global-teardown.ts",
	// On CI, a test.only left in a commit fails the run, rather than skipping
	// every other test.
	forbidOnly: !!process.env.CI,
	// A test that passes on a retry hides a race, so a test that fails once
	// fails the run, though Playwright's docs suggest two retries on CI.
	retries: 0,
	// A failed run's record, which the next run replaces: each failed test's
	// trace and the stack's log (e2e/global-teardown.ts) in test-results/, and
	// the HTML report in playwright-report/.
	outputDir: "test-results",
	reporter: [
		["list"],
		["html", { open: "never", outputFolder: "playwright-report" }],
	],
	use: {
		baseURL: `http://127.0.0.1:${ports.web}`,
		trace: "retain-on-failure",
	},
	// docs/conventions/testing.md's "The web app end to end" names these two
	// projects, and why.
	projects: [
		{ name: "Desktop Chrome", use: devices["Desktop Chrome"] },
		{ name: "Galaxy Tab S9", use: devices["Galaxy Tab S9"] },
	],
});
