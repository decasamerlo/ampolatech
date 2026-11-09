import { expect, test } from "@playwright/test";

// The demo sign-in signs everyone in as the sales agent the local profile
// seeds.
test("the layout shows the signed-in user's name, roles and pharmacy", async ({
	page,
}) => {
	await page.goto("/orders");

	await expect(page.getByRole("heading", { name: "Pedidos" })).toBeVisible();
	const header = page.getByRole("banner");
	await expect(
		header.getByText("Joana Exemplo", { exact: true }),
	).toBeVisible();
	await expect(header.getByText("Vendedor", { exact: true })).toBeVisible();
	await expect(
		header.getByText("Farmácia Exemplo", { exact: true }),
	).toBeVisible();
});
