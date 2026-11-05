import { createRootRoute } from "@tanstack/react-router";
import { Layout } from "../layout/layout";
import { NotFoundPage } from "../not-found/not-found-page";

export const Route = createRootRoute({
	component: Layout,
	notFoundComponent: NotFoundPage,
});
