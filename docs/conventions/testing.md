# Testing

Which seam a test goes through, and what a test at each seam may use. No checker enforces it: review does.

## Seams

A seam is where a test meets the code: what it calls, and what it treats as the outside world. Each behaviour is tested at one seam.

- **The domain types**, for a rule a domain type enforces: a value it refuses, a value it derives, a change it allows. The test calls the type's constructors and methods in the `domain` module, with JUnit and AssertJ alone. `domain` depends on nothing ([ADR 0002](../adr/0002-stack.md)), so a test that needs anything more is testing something other than the domain.
- **The HTTP API**, for behaviour that crosses layers: what a client sees, what is stored, who is signed in, and the pharmacy filter of [ADR 0003](../adr/0003-one-instance-for-all-pharmacies.md). Application services and driving adapters are tested here too (below). The test starts the app against PostgreSQL in Testcontainers, with every module real, and calls it only over HTTP. #2's *Testing Decisions* list what it covers in the first slice.
- **The app's start**, for a check that refuses to start the app, which no HTTP call can reach. The test calls `SpringApplication.run` with the profiles and properties under test and checks that the start fails, as `DemoSignInGuardTest` does.
- **The web app end to end**, for a few journeys in a browser against the real backend, as #2's *Testing Decisions* describe.

## Domain unit tests

- **Every rule a domain type enforces has a test**, in the same change as the rule. A refused `null` counts: it makes a missing value fail where it comes in, not later, far from its cause.
- **A test pins the rule's boundary**: the values the rule refuses, and the closest one it accepts. `UserTest` checks that `User` refuses `""`, `"   "` and `"\t\n"` as a name and accepts `"A"`.
- **The rule's cases live here.** At the HTTP API, one case shows what a client sees when the rule refuses a request: the status, the body, and nothing stored.

## Application services and driving adapters

An application service is tested through the HTTP API, against the real adapters. It only coordinates: it reads through a port, calls the domain, and writes through another port. A unit test would replace its ports with mocks and could then check only which calls the service made, so it would break whenever the service changed how it calls them, with behaviour intact.

A driving adapter is tested there too, with the framework running: sign-in and the HTTP binding are what it adds.

## Sources

- nesto's [domain-layer isolation](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/domain-layer-isolation.md): "Domain logic must be unit-testable with zero Spring context and zero DB."
- Vladimir Khorikov, [How to do painless TDD](https://enterprisecraftsmanship.com/posts/painless-tdd/): "you need to unit test only the domain-related code", since a domain model "is self-contained, it doesn't depend on the external world. It means that you don't need to use any mocks to test it, nor should you." Code that only coordinates "doesn't contain any essential logic itself", and unit-testing it "brings more costs than profits".
