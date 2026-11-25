# Testing

Which seam a test goes through, and what a test at each seam may use. No checker enforces it: review does.

## Seams

A seam is where a test meets the code: what it calls, and what it treats as the outside world. Each behaviour is tested at one seam.

- **The domain types**, for a rule a domain type enforces: a value it refuses, a value it derives, a change it allows. The test calls the type's constructors and methods in the `core` module, with JUnit and AssertJ alone. `core` depends on nothing but Lombok, which works at compile time only ([ADR 0007](../adr/0007-backend-modules.md), [ADR 0011](../adr/0011-lombok-in-both-backend-modules.md)), so a test that needs anything more is testing something other than the domain. `core` also holds the application services, and its tests are still the domain types' alone: a service is tested at the HTTP API.
- **The HTTP API**, for behaviour that crosses layers: what a client sees, what is stored, who is signed in, and the pharmacy filter of [ADR 0003](../adr/0003-one-instance-for-all-pharmacies.md). Application services and driving adapters are tested here too (below). The test starts the app against PostgreSQL in Testcontainers, with every module real, and calls it only over HTTP. Every response is checked against the API's spec, `api/openapi.yaml` ([ADR 0006](../adr/0006-openapi-contract-first.md)): a status, content type or body the spec doesn't allow fails the test that made the request. #2's *Testing Decisions* list what it covers in the first slice.
- **The app's start**, for a check that refuses to start the app, which no HTTP call can reach. The test calls `SpringApplication.run` with the profiles and properties under test and checks that the start fails, as `DemoSignInGuardTest` does.
- **The web app end to end**, for a few journeys in a browser against the real backend, as #2's *Testing Decisions* describe. *The web app end to end* below says what a test there uses.

`ArchitectureTest`, in `app`, sits outside the four: it checks the backend's layout, where code lives and what it may use ([hexagonal.md](hexagonal.md)), not a behaviour.

## Domain unit tests

- **Every rule a domain type enforces has a test**, in the same change as the rule. A refused `null` counts: it makes a missing value fail where it comes in, not later, far from its cause.
- **A test pins the rule's boundary**: the values the rule refuses, and the closest one it accepts. `UserTest` checks that `User` refuses `""`, `"   "` and `"\t\n"` as a name and accepts `"A"`.
- **The rule's cases live here.** At the HTTP API, one case shows what a client sees when the rule refuses a request: the status, the body, and nothing stored.

## Application services and driving adapters

An application service is tested through the HTTP API, against the real adapters. It only coordinates ([hexagonal.md](hexagonal.md)), so a unit test would replace its ports with mocks and could then check only which calls the service made: it would break whenever the service changed how it calls them, with behaviour intact. How such a test fixes the time is in hexagonal.md's *Who implements a driving port*.

A driving adapter is tested there too, with the framework running: sign-in and the HTTP binding are what it adds.

## The web app end to end

A browser test, in `web/e2e/`, opens the app at nginx's port with Playwright and checks what a user sees there, through the page alone.

- **The whole stack, fresh.** Playwright's global setup starts `compose.yaml`'s stack on an empty database, on ports of the tests' own and under a Compose project of the checkout's own, `ampolatech-e2e-` followed by a hash of the checkout's path (`web/e2e/stack.ts`), so it runs beside the stack `docker compose up` runs and leaves another checkout's browser-test stack alone. Every checkout's run uses the same ports, so one run goes at a time on a machine: when another Compose project's containers hold the ports, the setup stops before it builds anything, with a message that names that project, its folder and the command that removes it. An agent that meets that message reports the holder and stops: a run may be under way there, and removing another checkout's stack is the author's to decide. The global teardown, which runs after a failed setup too, writes the stack's logs to `web/test-results/stack.log`, then removes the stack, its volume and built images included. Each Docker call the setup and the teardown make has a time limit, 10 minutes on `up --wait`, which builds both images and waits until the app is healthy, and 1 minute on every other: a call past its limit fails the run as any other failure does, with an error that names the call and the limit.
- **The demo sign-in.** Every test runs as the user the local profile seeds, whom the demo sign-in signs everyone in as, and expects that seed's values.
- **Two projects, both Chromium**, the engine of the Chrome and Edge [ADR 0001](../adr/0001-hosted-web-app.md) names: Desktop Chrome, and Playwright's Galaxy Tab S9, an Android tablet 640 px wide, until the first pharmacy's tablet model is known.
- **Run by CI** ([git.md](git.md)), and by hand with `pnpm test:e2e` in `web/`, which needs Docker.

## Sources

- nesto's [domain-layer isolation](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/domain-layer-isolation.md): "Domain logic must be unit-testable with zero Spring context and zero DB."
- Vladimir Khorikov, [How to do painless TDD](https://enterprisecraftsmanship.com/posts/painless-tdd/): "you need to unit test only the domain-related code", since a domain model "is self-contained, it doesn't depend on the external world. It means that you don't need to use any mocks to test it, nor should you." Code that only coordinates "doesn't contain any essential logic itself", and unit-testing it "brings more costs than profits".
