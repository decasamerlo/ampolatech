import { defineConfig } from "orval";

// The client the web app calls the API through, generated from the spec
// (ADR 0006).
export default defineConfig({
	ampolatech: {
		input: { target: "../api/openapi.yaml" },
		output: {
			client: "react-query",
			httpClient: "fetch",
			// A folder per tag, as the backend has an interface per tag.
			mode: "tags-split",
			target: "src/api/generated/endpoints.ts",
			schemas: "src/api/generated/model",
			clean: true,
			override: {
				// The mutator resolves with a 2xx's body and throws for every other
				// answer, so the hooks' data is the success's body alone.
				fetch: { includeHttpResponseReturnType: false },
				mutator: { path: "src/api/call-api.ts", name: "callApi" },
			},
		},
	},
});
