# The web app's code is laid out by Feature-Sliced Design, with every layer from the start

The code in `web/src/` is laid out by [Feature-Sliced Design](https://fsd.how) (FSD): five layers, `app`, `pages`, `features`, `entities` and `shared`, each importing only the layers below it, with `pages`, `features` and `entities` divided into slices that the rest of the app imports through an `index.ts`. [feature-sliced.md](../conventions/feature-sliced.md) holds the rules.

The next web issues, #6 to #9, add the order list, the register form and the prescriptions: code that belongs to a domain noun or to one interaction, where `web/src/` had a folder per screen. A layout settled before #6 keeps each of those pull requests from deciding it again.

- **Every layer is used from #6**, each arriving with its first slice: `entities/order` with #6, `features/register-order` with #7.
- **FSD's `widgets` layer is left out.** [FSD's reference on layers](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/reference/layers.mdx) discourages it, since a block of UI with its logic overlaps a feature: a composition one screen uses stays in its page, and one reused across pages is a feature.
- **The route files sit in `app`**, in `src/app/routes/`, with the route tree beside them. `main.tsx` imports the tree, the tree the routes, and the root route the layout, all in one layer, so the import rules need no exception. FSD has no TanStack Router guide. Where a framework lets its routes' folder move, FSD's guide moves it into `app` ([Nuxt's](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/guides/tech/with-nuxtjs.mdx), [SvelteKit's](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/guides/tech/with-sveltekit.mdx)), and TanStack Router reads the folder from `web/tsr.config.json`.
- **[dependency-cruiser](https://github.com/sverweij/dependency-cruiser) holds the import rules**, in the pre-push hook and in CI, as `ArchitectureTest` holds the backend's ([ADR 0007](0007-backend-modules.md)).

## Considered options

- **Feature-Sliced Design, pages first.** [FSD's guide to v2.1](https://github.com/feature-sliced/documentation/blob/972ae332cbd362b56a8262c82b85f1e678a498ef/src/content/docs/docs/guides/migration/from-v2-0.mdx) recommends "starting with pages, and possibly even stopping there", moving code to a layer below once several pages reuse it. Each of #6 to #9 would then decide whether its code had reached that point.
- **Feature folders**, as [bulletproof-react's project structure](https://github.com/alan2207/bulletproof-react/blob/9506629ed003a561c6627735480cce4994244bb4/docs/project-structure.md) lays them out: `src/features/<feature>/` holds a feature's API calls, components and hooks; `src/components/`, `src/hooks/` and `src/lib/` hold what features share; the app's routes and providers sit in `src/app/`. Its import rules run on ESLint, which #4 found can't run on the pinned TypeScript 7.
- **Code beside the route files.** TanStack Router skips a file or folder in the routes' folder whose name starts with `-` (`routeFileIgnorePrefix` in [its file-based routing options](https://github.com/TanStack/router/blob/94f36c4a9ecf3062b326a8eb772e73b00c0644d9/docs/router/api/file-based-routing.md)), so a route's components can sit beside its route file.
- **By kind**: `components/`, `hooks/`, `pages/`. Common in small apps; a feature's code ends up spread across folders.

## Consequences

- A screen with one component still gets a slice, a `ui/` segment and an `index.ts`.
- The check adds two development dependencies, dependency-cruiser and `@swc/core`. dependency-cruiser's docs mark its `swc` parser deprecated, and the pinned TypeScript 7.0 lacks the compiler API it would otherwise parse with; feature-sliced.md records the trigger for dropping `@swc/core`, a stable TypeScript 7.1.
- An import across slices or layers is written with the `@/` alias, which feature-sliced.md covers.
