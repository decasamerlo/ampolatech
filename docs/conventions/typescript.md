# TypeScript

The rules for the web app's TypeScript that Biome's formatter leaves open. Where code lives and what it may import is in [feature-sliced.md](feature-sliced.md). Comments follow [comments.md](comments.md).

## Names and exports

| Rule | Held by |
| --- | --- |
| A file is named in kebab-case: `orders-page.tsx`. | Biome, `useFilenamingConvention`, over every file `pnpm check` reads |
| A file exports by name, never a default. | Biome, `noDefaultExport`, in `src/` |
| A file holds one component. | Review |

- **`noDefaultExport` stops at `src/`**: outside it, the configs of Vite, Orval and Playwright and Playwright's global setup and teardown each export the default their tool reads.
- **TanStack Router names the route files.** `__root.tsx` passes the name rule. A route with a parameter, such as `orders.$orderId.tsx`, fails it, and the change that adds the first such file adds its exception to `web/biome.json`.
