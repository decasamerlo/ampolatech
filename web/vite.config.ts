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
	server: { proxy: { "/api": `http://127.0.0.1:${backendPort}` } },
});
