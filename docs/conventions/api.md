# API

How `api/openapi.yaml` is written, and how the backend's web adapter meets it. The backend's build generates code from it, and [ADR 0006](../adr/0006-openapi-contract-first.md) has the web app's do the same, so a name in it reaches both sides, and renaming it later changes both. No checker enforces these rules: review does, as with [java.md](java.md).

## Who decides

**Every path, tag, schema, property and enum value in the spec is a name or a shape a client sees**, so *Choosing or asking* in [author-values.md](author-values.md) applies: when the issue leaves one open, the agent asks the author first, with a proposal. A name taken from a probe, a draft or an example is a proposal too.

## Names

- **A tag is the plural noun of the collection its operations serve**: `users`. A collection takes the plural, as in resource paths ([AIP-122](https://google.aip.dev/122)), and the backend's generator names the interface after the tag: `UsersApi`.
- **A schema is named after what it holds, in the glossary's terms**: `Pharmacy`, or `SignedInUser`, the glossary's user with a qualifier that says which one. A qualifier that names a shape instead, such as `Summary` or `Detail`, belongs beside a second shape of the same thing, to tell the two apart. Alone, it says another shape exists when none does: a misleading name, which [author-values.md](author-values.md) counts as a defect.

## The generated code

- **A schema takes the bare name, and its Java model adds `Dto`**: `Pharmacy` in the spec, `PharmacyDto` in the backend (`modelNameSuffix` in `backend/app/build.gradle.kts`). The suffix is the backend's: it keeps a Java model from sharing a simple name with a domain type, such as `Pharmacy`. A client sees the bare name.
- **The web adapter's mapper turns a domain enum into its model with an exhaustive `switch`** ([hexagonal.md](hexagonal.md) says where the mapper sits), and the generator maps no schema to a domain type (`schemaMappings`, `importMappings`, `typeMappings`). A constant the domain adds then fails the compile until someone decides what the API says about it, and renaming a domain constant leaves the API as it was.

## Errors

**An error answers `application/problem+json` with the spec's `Problem` schema**, RFC 9457's problem details written flat, as ADR 0006 says. `ProblemDetailsAdvice` writes every problem body in it, and an HTTP-seam test that gets one back holds it to the spec. How errors are coded and grouped is #85's to decide.

## Sources

- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), Problem Details for HTTP APIs.
- Google's [AIP-122](https://google.aip.dev/122), Resource names: "Collection identifiers must be plural."
- openapi-generator's [`spring` generator options at 7.27.0](https://github.com/OpenAPITools/openapi-generator/blob/v7.27.0/docs/generators/spring.md): `useTags` names each interface after its tag.
