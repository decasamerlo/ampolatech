import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, RouterProvider } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import { routeTree } from "./routeTree.gen";

const router = createRouter({ routeTree });

declare module "@tanstack/react-router" {
	interface Register {
		router: typeof router;
	}
}

// A failed query is tried 3 more times before its error shows, TanStack
// Query's default.
const queryClient = new QueryClient({
	defaultOptions: { queries: { retry: 3 } },
});

const root = document.getElementById("root");
if (root === null) {
	throw new Error("index.html has no element with the id root");
}
createRoot(root).render(
	<StrictMode>
		<QueryClientProvider client={queryClient}>
			<RouterProvider router={router} />
		</QueryClientProvider>
	</StrictMode>,
);
