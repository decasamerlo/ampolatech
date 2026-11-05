import { createFileRoute } from "@tanstack/react-router";
import { OrdersPage } from "../orders/orders-page";

export const Route = createFileRoute("/orders")({ component: OrdersPage });
