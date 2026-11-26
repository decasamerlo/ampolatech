import { createFileRoute } from "@tanstack/react-router";
import { OrdersPage } from "@/pages/orders";

export const Route = createFileRoute("/orders")({ component: OrdersPage });
