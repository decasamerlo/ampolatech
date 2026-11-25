import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// The port compose.yaml publishes the backend on, read as Compose reads
// it: 8080 when unset or empty.
const backendPort = process.env.AMPOLATECH_BACKEND_PORT || "8080";

export default defineConfig({
	// The router's plugin reads tsr.config.json, as `tsr generate` does, and
	// comes before React's.
	plugins: [tanstackRouter(), react(), tailwindcss()],
	// `@/` names src/, as `paths` in tsconfig.json has it for tsc. With
	// `paths` alone the build resolves the alias and the dev server
	// doesn't: it answers 500 for a module that imports through it. Vite's
	// `resolve.tsconfigPaths` would read `paths` instead; Vite's guide says
	// that has a performance cost, and that the TypeScript team discourages
	// `paths` as a way to steer other tools.
	resolve: { alias: { "@": path.resolve(import.meta.dirname, "./src") } },
	server: { proxy: { "/api": `http://127.0.0.1:${backendPort}` } },
});
