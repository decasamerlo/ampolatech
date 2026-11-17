# AmpolaTech web app

A single-page React app in TypeScript, built with Vite and pnpm ([ADR 0002](../docs/adr/0002-stack.md)). It keeps its own build, beside the backend's ([ADR 0005](../docs/adr/0005-one-repository.md)).

## Set up a machine

Install [mise](https://mise.jdx.dev), then run `mise install` at the repository root: it installs the Node and pnpm that `mise.toml` pins. With mise activated in your shell, `pnpm` is that pnpm; otherwise run it as `mise exec -- pnpm`. Then, from `web/`:

```bash
pnpm install
```

pnpm installs only releases at least 7 days old, and fails the install when a version it needs is younger (`minimumReleaseAge` in `pnpm-workspace.yaml`).

## Develop

From `web/`, with the backend running ([README.md](../README.md)):

```bash
pnpm dev
```

It generates the API's client and the route tree, then starts Vite's dev server on the address it prints, <http://localhost:5173> when that port is free. The server sends `/api` to the backend on `AMPOLATECH_BACKEND_PORT`, 8080 when unset, the port Compose publishes the backend on, so the app and the API share an origin, as they do behind nginx. A change to `api/openapi.yaml` reaches the running server on the next `pnpm generate`.

## Run it with the backend

The whole app runs from the repository root ([README.md](../README.md)), its `web` service built from this folder's `Dockerfile`: nginx, configured by `nginx.conf`, which says what it caches and what it compresses. A change under `src/` shows in `pnpm dev` at once, and in the `web` service after the next `docker compose up`.

## Build and check

From `web/`:

```bash
pnpm build       # generate, type-check, then bundle into dist/
pnpm check       # Biome's format and lint check; a warning fails it too
pnpm fix         # Biome's formatting and safe fixes, written to the files
pnpm generate    # the API's client and the route tree alone
pnpm typecheck   # tsc alone, on the app, the tools' configs and the tests
```

Vite transpiles TypeScript without checking it, so `pnpm build` runs `tsc` before it bundles: code that reads what the spec no longer declares fails the build ([ADR 0006](../docs/adr/0006-openapi-contract-first.md)).

Before committing a change to `web/` or `api/`, run `pnpm generate`, `pnpm typecheck` and `pnpm check` until all three exit 0; `pnpm typecheck` checks against the client `pnpm generate` wrote last. The pre-push hook refuses a push when one fails ([git.md](../docs/conventions/git.md)).

## Test in a browser

Once per machine, from `web/`, install the Chromium Playwright drives:

```bash
pnpm exec playwright install chromium
```

Then, with Docker running:

```bash
pnpm test:e2e
```

It runs the tests in `e2e/` against the whole app, built and started fresh beside the stack `docker compose up` runs, on ports of its own and under a Compose project of this checkout's own (`e2e/stack.ts`), and removes it afterwards, its built images included. [testing.md](../docs/conventions/testing.md) says what a test there uses.

Every checkout's run uses the same three ports, so one run goes at a time on a machine. When another Compose project's containers hold them, the run stops before it builds anything, with a message that names that project, the folder it was started from, how long it has been up and the command that removes it. An agent that meets that message reports the holder and stops, since a run may be under way there: removing another checkout's stack is the author's to decide.

A run leaves its record in two folders, which the next run replaces:

- `test-results/`: `stack.log`, the logs of the stack's containers, and each failed test's trace, which `pnpm exec playwright show-trace <trace.zip>` opens. `stack.log` is empty when the run stopped while Compose pulled or built the images, since it creates no container before both images are built; the run's output then shows where the pull or the build stopped.
- `playwright-report/`: the HTML report, with each failed test's error and trace, which `pnpm exec playwright show-report` opens.

Each Docker call the run makes has a time limit: 10 minutes on `docker compose up --wait`, which builds both images and waits until the app is healthy, and 1 minute on every other. A call past its limit is stopped and fails the run as any other failure does, with an error that names the call and the limit.

## Generated code

`pnpm generate` writes two things, and neither is committed (ADR 0006):

- **The API's client**, in `src/api/generated/`: TanStack Query hooks that Orval generates from `api/openapi.yaml`, configured in `orval.config.ts`. Every request goes through the mutator, `src/api/call-api.ts`, whose errors are the hooks' error type.
- **The route tree**, in `src/routeTree.gen.ts`: TanStack Router's CLI generates it from the files in `src/routes/`, configured in `tsr.config.json`, which the router's Vite plugin reads too. TanStack's FAQ says to commit it; it stays out for ADR 0006's reason, since Renovate's hosted app won't regenerate a committed file.

The repository's one `.gitignore`, at its root, lists both. Biome reads it there (`vcs.root` in `biome.json`) and leaves them alone.
