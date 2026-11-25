import { createRootRoute } from "@tanstack/react-router";
import { NotFoundPage } from "@/pages/not-found";
import { Layout } from "../layouts/layout";

export const Route = createRootRoute({
	component: Layout,
	notFoundComponent: NotFoundPage,
});
