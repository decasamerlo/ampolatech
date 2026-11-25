# Feature-Sliced Design

Where the web app's code lives in `web/src/`, what may import what, and the check that holds it. The code is laid out by [Feature-Sliced Design](https://fsd.how) ([ADR 0012](../adr/0012-feature-sliced-web-app.md)). dependency-cruiser enforces the import rules, as `ArchitectureTest` enforces the backend's ([hexagonal.md](hexagonal.md)); review enforces the rest. Names and exports are in [typescript.md](typescript.md). Paths below are under `web/`.

## Layers, slices and segments

Five **layers**, from the top. Each imports only the layers below it.

| Layer | Holds |
| --- | --- |
| `app` | The entry point (`main.tsx`, `index.css`), the app-wide layout (`layouts/`), and the route files with their route tree (*Route files*). |
| `pages` | A slice per screen, such as `pages/orders`. A composition one screen uses stays in its page. |
| `features` | A slice per interaction, such as `features/register-order`. A composition reused across pages goes here. |
| `entities` | A slice per domain noun, such as `entities/order`. |
| `shared` | `api/`: the mutator and the generated client (*Generated code*). |

- **A layer arrives with its first slice**: `src/` holds no empty `features/` or `entities/`.
- **The method's `widgets` layer is left out** (ADR 0012). A block of UI that would go there goes where the table puts a composition.
- **`pages`, `features` and `entities` are divided into slices**, a folder each.
- **A slice holds segments (`ui/`, `api/`, `model/`) and an `index.ts`**, and the rest of the app imports the slice through that file.
- **`app` and `shared` aren't divided into slices**: each holds its segments directly.

## Import rules

| Rule | Its name in `.dependency-cruiser.cjs` |
| --- | --- |
| 1. A layer imports only the layers below it. | `<layer>-imports-only-below` |
| 2. A slice never imports another slice on its own layer. | `slices-never-each-other` |
| 3. Code outside a slice imports it through its `index.ts`. | `<layer>-through-their-index` |
| 4. Every import resolves. | `every-import-resolves` |

Rule 4 is there for the other three: each matches the file an import reaches, so an import that reaches no file passes them all.

### The check

```bash
pnpm check:imports   # from web/, after pnpm generate
```

It runs dependency-cruiser over `src/` with the rules in `.dependency-cruiser.cjs`, and reads the client and the route tree `pnpm generate` wrote, since an import of either must resolve. A violation is printed as the rule's name, the importing file and the file it reaches. The pre-push hook and CI's `web` job run it after `pnpm check` ([git.md](git.md)).

**A new or changed rule goes red before it lands**, as hexagonal.md asks of `ArchitectureTest`'s: plant the break, run the check, delete the break, and show the run in the PR's Evidence. One line per rule, with the break that turns it red; a new rule adds its own, and a changed rule updates it:

- rule 1: a file in `src/pages/orders/ui/` importing `@/app/layouts/layout`;
- rule 2: a file in `src/pages/orders/ui/` importing `@/pages/not-found`;
- rule 3: a file in `src/app/routes/` importing `@/pages/orders/ui/orders-page`;
- rule 4: a file importing `@/pages/no-such-page`.

**The parser is `@swc/core`, until TypeScript 7.1 is stable.** The pinned TypeScript 7.0 lacks the compiler API dependency-cruiser parses TypeScript through, and dependency-cruiser's docs mark its `swc` parser deprecated. With no parser it can load, the cruise reads no module and exits 0, so the config loads `@swc/core` itself and fails when it can't.

- **The trigger for dropping `@swc/core`**: a stable TypeScript 7.1, which dependency-cruiser reads on its own. The change that pins it removes the `parser` line, the `require` above the rules and the `@swc/core` dependency with its `allowBuilds` entry.
- **A dependency-cruiser release that drops the `swc` parser waits for that TypeScript.**

## The alias

**An import across slices or layers goes through `@/`**, an alias for `src/`: `@/pages/orders`, `@/shared/api/generated/model`. **An import inside a slice, inside `app` or inside `shared` is relative**: `__root.tsx` imports `../layouts/layout`.

The alias is set in three places, and a change to it changes all three:

- `tsconfig.json`: `paths`, with no `baseUrl`, which TypeScript 7 refuses.
- `vite.config.ts`: `resolve.alias`. With `paths` alone the build passes, and the dev server answers 500 for a file that imports through the alias. Vite's `resolve.tsconfigPaths` would read `paths` instead; [Vite's guide](https://github.com/vitejs/vite/blob/v8.3.5/docs/guide/features.md) says it has a performance cost, and that the TypeScript team discourages `paths` as a way to steer other tools.
- `.dependency-cruiser.cjs`: `tsConfig`, which reads the `paths` above.

## Route files

The route files sit in `src/app/routes/`, where `tsr.config.json` points TanStack Router, so `main.tsx`, the route tree, the routes and the layout share a layer.

**A route file is thin**: it holds the route's options, such as `index.tsx`'s `beforeLoad` redirect or `__root.tsx`'s `notFoundComponent`, and mounts one page or layout. It has no JSX of its own, no hook and no `fetch`. Review holds this.

## Generated code

`pnpm generate` writes the API's client to `src/shared/api/generated/` and the route tree to `src/app/routeTree.gen.ts`, and neither is committed ([ADR 0006](../adr/0006-openapi-contract-first.md)).

- **A slice calls the generated hooks directly.** A per-noun wrapper around them waits for a second slice that needs the same logic.
- **Hand-written code stays out of `generated/`**: Orval empties the folder on each run (`clean: true` in `orval.config.ts`). The mutator, `call-api.ts`, sits beside it in `src/shared/api/`.
- **Each further generated output gets a folder of its own under `src/shared/api/`**, listed in the root `.gitignore`.
- **A slice's own request code goes in its `api/` segment.**

## Sources

- Feature-Sliced Design's reference, at the commit this convention was written against: [layers](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/reference/layers.mdx), [slices and segments](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/reference/slices-segments.mdx) and [the public API](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/reference/public-api.mdx), which asks for relative imports inside a slice.
- dependency-cruiser 18.8.0's [rules reference](https://github.com/sverweij/dependency-cruiser/blob/v18.8.0/doc/rules-reference.md) and [options reference](https://github.com/sverweij/dependency-cruiser/blob/v18.8.0/doc/options-reference.md), where `parser` marks `swc` deprecated. Its [`package.json`](https://github.com/sverweij/dependency-cruiser/blob/v18.8.0/package.json#L347) lists the TypeScript versions it reads: `>=2.0.0 <7.0.0||>=7.1.0 <8.0.0`.
