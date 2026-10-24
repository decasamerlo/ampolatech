# OpenAPI contract-first between the web app and the API

The API is defined in one hand-written OpenAPI 3.1 spec at the repository root, and both sides are generated from it during their builds. The contract is then a file written on purpose, and both builds fail when the code drifts from it, while uploads stay multipart and errors stay RFC 9457 problem details.

The generated code is never committed: the spec is what gets reviewed, and Renovate's hosted app won't regenerate committed output by default. A change to the spec is best reviewed in the same pull request as the code that implements it. That's a hint, not a rule; only the build forces it, when a change stops a controller from compiling.

- **The backend's REST adapter** gets interfaces from openapi-generator's `spring` generator (7.26.0 when decided), with its defaults overridden: interfaces only, Spring Boot 4 and Jackson 3, Spring's built-in validation (otherwise a validation failure on a multipart field becomes a server error instead of a 400 problem), and no default implementations, so that drift fails the build (otherwise a new operation compiles and answers 501). Controllers implement the interfaces and map the generated DTOs to the domain inside the adapter.
- **The web app** gets its TanStack Query client and its zod schemas from Orval, so forms reuse the spec's limits and error codes. A small mutator makes failed requests throw, so the hooks' error types are true.

The generated interfaces type only the success response, so problem-details bodies are held to the spec another way: one `@RestControllerAdvice` writes every problem body, and each error-response test validates the body against the spec. An error path with no such test goes unchecked.

The spec stays on OpenAPI 3.1, because the generator rejects 3.2, and its problem schemas are written flat, because on 3.1 the response validator rejected a valid body composed with `allOf`. The generated DTOs are mutable classes in 7.26.0; adopting openapi-generator's newer `useRecords` option, which still leaves some models as classes, is a later choice.

## Considered options

- **OpenAPI code-first (springdoc).** Java records stay the source, but with default annotations it produced a valid spec that was wrong, with no warning: multipart text fields as query parameters, required fields left optional, and a field on problem details that is never sent.
- **Hand-written types on both sides.** An estimated twice the client code per endpoint, and drift shows only on the paths a test happens to exercise.
- **A spec for the web app only**, with the backend held to it by response-validator tests. Java records stay, but a new or removed operation no longer fails the build, and multipart requests go unchecked.
- **Protobuf with Connect.** The strongest drift checks, but no maintained Java server speaks Connect beside Spring MVC, a file upload becomes one message, and errors leave RFC 9457.
- **Types inferred across the boundary (tRPC-style).** It needs TypeScript on both sides, which [ADR 0002](0002-stack.md) rules out.

The survey behind this is [api-contract.md](../research/api-contract.md); its recommendation lists every generator setting to override.
