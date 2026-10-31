# How the web app and the API share a contract

Date: 2026-10-21

**Summary.** OpenAPI contract-first fits AmpolaTech best, with three changes from the usual setup:

- The spec is hand-written in OpenAPI 3.1, not 3.2.
- The Java interfaces come from openapi-generator's `spring` generator, with several defaults overridden.
- The TypeScript client and zod schemas come from Orval, not openapi-typescript.

The contract is a reviewed file, and both sides fail to compile when the code drifts from it. Protobuf shares those two properties; OpenAPI is the only one of the two that keeps multipart uploads and RFC 9457 problem details. A new operation or a changed field type broke the Java build, and the spec's `minLength`, `maxLength`, `pattern` and enum values came out as zod rules the forms can reuse (verified locally).

What the generators do not cover has to be closed by hand:

- **Error bodies aren't typed on the server.** The generated interface returns only the success type, so the shape of a problem-details body is held by a response-validation test against the spec, not by the compiler.
- **openapi-generator's defaults need overriding.** With its defaults, a Bean Validation failure on a multipart field escaped as an unhandled `ConstraintViolationException`, a server error instead of a 400 problem (verified locally).
- **openapi-typescript is out.** Its maintainer put it in maintenance mode on 2026-09-19.

The other options each lose on a point the author cares about:

- **Code-first with springdoc** works on Spring Boot 4.1 and keeps Java records as the source. But on the default annotations its spec was wrong in ways nothing flags: multipart text fields became query parameters, required fields were not required, and `ProblemDetail` got a field it never sends (all verified locally). Its Gradle plugin has had no commit since 2024.
- **Hand-written on both sides** costs about twice the client-side code per endpoint (analysis). Drift shows up only on the paths a test happens to exercise.
- **Protobuf with Connect** has the strongest drift checks (`buf breaking`), and protovalidate rules can drive the forms directly. But no maintained Java server speaks the Connect protocol next to Spring MVC: the options are Armeria with a custom error handler, Envoy with an alpha filter, or binary gRPC-Web. A 10 MB upload becomes one base64 or binary message, and errors leave RFC 9457 (all verified locally).

How to read this file:

- Every claim links to the source it was checked against on 2026-10-21: the tool's docs, its GitHub repository, releases or issues, the npm registry or Maven Central, the OpenAPI specification, or Spring's reference docs. GitHub links without a commit point at the default branch as read that day.
- **Verified locally** marks behaviour reproduced on 2026-10-21 in throwaway projects with Java 25.0.4 (Temurin), Gradle 9.8.2, Spring Boot 4.1.1 (Spring Framework 7.0.9, Jackson 3.1.5, Hibernate Validator 9.1.3; [BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)), Node 24.6.0, pnpm 10.33.0, zod 4.6.7 and @tanstack/react-query 5.104.5. Each project used the same small spec: a multipart upload (a file plus a required `patientName` with length limits and an optional `prescriberCrm` with a pattern) and a `GET`, with RFC 9457 error responses that carry an `errors` array of `{field, code}`. The [appendix](#appendix-local-experiments) lists each run.
- Text marked **analysis** is reasoning from the cited facts, not a sourced claim.

## Background: what the contract has to carry

- **Problem details.** RFC 9457 defines `application/problem+json` with the members `type`, `title`, `status`, `detail` and `instance`, and allows extension members ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)). AmpolaTech's `errors` array and stable `code` are extension members, so no tool knows them unless the contract declares them.
- **Spring's side.** Spring MVC renders `ProblemDetail` for its own exceptions through `ResponseEntityExceptionHandler` ([Error Responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)). Constraint annotations on controller method parameters raise `HandlerMethodValidationException`, but only if the controller has no class-level `@Validated`. With `@Validated`, "method validation is applied through an AOP proxy" instead ([Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)), and the exception is a `ConstraintViolationException` that `ResponseEntityExceptionHandler` does not handle. This matters for generated interfaces (section 1).
- **File uploads in OpenAPI.** In 3.0 a file part is `type: string, format: binary`. In 3.1, "the `format` keyword has no effect on the content-encoding of the schema"; JSON Schema's `contentMediaType` and `contentEncoding` are used instead ([OAS 3.1.1, file uploads](https://spec.openapis.org/oas/v3.1.1.html#considerations-for-file-uploads)). Section 1 shows tools that still only recognise `format: binary`.
- **OpenAPI 3.2.0** was published on 19 September 2025 ([OAS 3.2.0](https://spec.openapis.org/oas/v3.2.0.html)). It adds, among other things, a `query` operation for the HTTP QUERY method. Of the Java tools below, only openapi-processor's parser reads it.

## 1. OpenAPI contract-first

The spec lives in the repository. Spring interfaces are generated from it into the REST adapter's build directory, and controllers in the adapter implement them. The TypeScript client, types and zod schemas are generated from the same file in `web/`.

### Server: openapi-generator, `spring` generator

**Status.**

- **Latest release: 7.26.0, 2026-10-06** ([releases](https://github.com/OpenAPITools/openapi-generator/releases/tag/v7.26.0)). Releases came roughly monthly through 2026 (7.19.0 on 2026-01-19 through 7.26.0). The Gradle plugin `org.openapi.generator` is on the Gradle Plugin Portal at the same version ([plugin portal metadata](https://plugins.gradle.org/m2/org/openapi/generator/org.openapi.generator.gradle.plugin/maven-metadata.xml)).
- The `spring` generator is marked `STABLE` ([generator docs at v7.26.0](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/docs/generators/spring.md)).
- **Maintainers.** The project is large: 26.8k stars and 5,776 open issues ([GitHub API](https://api.github.com/repos/OpenAPITools/openapi-generator)). One person, wing328, has 8,169 commits; the next has 1,873 ([contributors](https://github.com/OpenAPITools/openapi-generator/graphs/contributors)).

**Spring Boot 4 and Java 25.**

- **Spring Boot 4 is a supported option, not the default.** `useSpringBoot4` ("Generate code and provide dependencies for use with Spring Boot 4.x") and `useJackson3` ("only allowed when `useSpringBoot4` is set") first appear in the docs of 7.20.0, released 2026-02-16 ([7.20.0 docs](https://github.com/OpenAPITools/openapi-generator/blob/v7.20.0/docs/generators/spring.md); [Jackson 3 PR #22854](https://github.com/OpenAPITools/openapi-generator/pull/22854)). The default is still `useSpringBoot3=true` ([7.26.0 docs](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/docs/generators/spring.md)).
- **Spring Boot 4 fixes are still landing.** In the last two months: JsonNullable with Jackson 3 ([#25151](https://github.com/OpenAPITools/openapi-generator/pull/25151), merged 2026-10-07, after 7.26.0), and a duplicate starter in the Boot 4 POM ([#25209](https://github.com/OpenAPITools/openapi-generator/pull/25209)). Two open issues report Hibernate Validator 9.1 warnings (HV000271) from `@Valid` placement on containers ([#25097](https://github.com/OpenAPITools/openapi-generator/issues/25097), [#25249](https://github.com/OpenAPITools/openapi-generator/issues/25249)).
- The docs state no Java version. **Verified locally:** with `interfaceOnly`, `useSpringBoot4`, `useJackson3` and `useJspecify`, the 7.26.0 Gradle plugin produced code that compiled with Java 25 against Spring Boot 4.1.1, and a `@WebMvcTest` suite passed against it.

**What it generates.** With `interfaceOnly=true` it writes one interface per tag (`useTags`) and one model class per schema ([options](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/docs/generators/spring.md)). From the experiment spec (verified locally):

```java
@RequestMapping(method = RequestMethod.POST, value = PrescriptionsApi.PATH_UPLOAD_PRESCRIPTION,
    produces = { "application/json", "application/problem+json" }, consumes = { "multipart/form-data" })
ResponseEntity<Prescription> uploadPrescription(
     @RequestPart(value = "file", required = true) MultipartFile file,
     @Size(min = 1, max = 120) @Valid @RequestParam(value = "patientName", required = true) String patientName,
     @Pattern(regexp = "^[0-9]{4,6}-[A-Z]{2}$") @Valid @RequestParam(value = "prescriberCrm", required = false) String prescriberCrm);
```

- **The file part works from both a 3.0 and a 3.1 spec.** `format: binary` (3.0) and `contentMediaType` (3.1) both became `MultipartFile` (verified locally).
- **Spec constraints become Bean Validation annotations.** The two text parts became `@RequestParam`, which Spring binds from multipart form fields as well as the query string.
- **The return type is the success type only.** It is `ResponseEntity<Prescription>`, even though the operation declares a 400 `ValidationProblem`. The `Problem` and `ValidationProblem` model classes are generated but appear nowhere in the signature (verified locally). The server reports errors by throwing, and a `@RestControllerAdvice` writes the body. Nothing ties that body to the schema at compile time.
- **In 7.26.0, models are mutable classes with getters, setters and fluent builders, not records.** An `allOf` schema is flattened into one class with all properties (verified locally).
  - **Records are coming, unreleased.** A `useRecords` option for the `spring` generator was merged to `master` on 2026-10-12 ([#25200](https://github.com/OpenAPITools/openapi-generator/pull/25200)), followed by builders for records, `oneOf` members as records, and records as parameter objects ([#25351](https://github.com/OpenAPITools/openapi-generator/pull/25351), 2026-10-23). It is not in 7.26.0 (2026-10-06); the next release, 7.27.0, has a milestone due 2026-11-02 ([milestones](https://github.com/OpenAPITools/openapi-generator/milestones)). Its documentation says a model stays a class when it uses inheritance, `additionalProperties`, an `anyOf` or `oneOf` composition, or a `byte[]` property, among other cases, and that it can't be combined with Lombok or `useOptional` ([spring generator options](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/spring.md)). Not tested here. (Checked on 2026-10-23.)
- **Defaults that need overriding** (all verified locally):
  - **`useSpringBuiltInValidation` defaults to `false`.** The generated interface then carries a class-level `@Validated`, which sends method validation through Spring's AOP proxy. An over-long `patientName` raised `ConstraintViolationException` instead of `HandlerMethodValidationException`. A `ResponseEntityExceptionHandler`-based advice doesn't handle it, so the client gets a server error instead of a 400 problem. With `useSpringBuiltInValidation=true` the same request produced a 400 problem-details body with one entry per field, through `handleHandlerMethodValidationException`.
  - **`skipDefaultInterface` defaults to `false`.** The interface then gets default method bodies that return 501, so a new operation in the spec compiles without an implementation. With `skipDefaultInterface=true`, adding a `DELETE` to the spec failed the build: `PrescriptionController is not abstract and does not override abstract method deletePrescription(UUID)`. Changing a response field from `string` to `integer` failed too: `String cannot be converted to Integer`.
  - **Defaults pull in libraries.** `documentationProvider` defaults to `springdoc`, `annotationLibrary` to `swagger2` and `openApiNullable` to `true` ([options](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/docs/generators/spring.md)). Each adds a dependency that interface-only generation doesn't need; set them to `none`, `none` and `false`.
  - **Two options log warnings until set.** `generateJsonIncludeAnnotations` and `generateJsonSetterNullsAnnotations` log a warning on every run until they are set explicitly.
  - **Nullability.** `useJspecify=true` switches nullability annotations to JSpecify. Without it they come from `org.springframework.lang`.
  - **A Gradle deprecation.** The build logged a Gradle 11 deprecation (`Configuration.setVisible`) at configuration time. The generator plugin was the only third-party plugin in that build.

**OpenAPI versions.**

- **3.1 is "beta".** Every run logged "OpenAPI 3.1 support is still in beta" (verified locally).
- **3.2 is rejected.** `openapi: 3.2.0` fails validation with nine errors, and generation stops (verified locally). The generator parses with swagger-parser, "which does not yet support openapi 3.2.0 spec" (maintainer, [#22728](https://github.com/OpenAPITools/openapi-generator/issues/22728)). swagger-parser's own 3.2 request is open ([swagger-parser #2248](https://github.com/swagger-api/swagger-parser/issues/2248)). Its maintainers posted in March 2026 that backlog work across swagger-core and swagger-parser had been limited ([#2284](https://github.com/swagger-api/swagger-parser/issues/2284)).

### Server alternative: openapi-processor-spring

It generates only interfaces and DTOs, as get/set classes or records ([docs source](https://github.com/openapi-processor/openapi-processor-spring/blob/master/docs/modules/ROOT/pages/index.adoc); [`model-type: record`](https://github.com/openapi-processor/openapi-processor-spring/blob/master/docs/modules/ROOT/pages/processor/configuration.adoc)).

**Status.**

- **Latest release: 2026.3.2, 2026-07-07.** 2026.3.1 fixed generated code that no longer compiled on Spring 7 / Spring Boot 4 ([release](https://github.com/openapi-processor/openapi-processor-spring/releases/tag/v2026.3.1)).
- **One maintainer.** hauner has 1,652 commits; the next human has 1 ([contributors](https://github.com/openapi-processor/openapi-processor-spring/graphs/contributors)).
- **Its parser reads 3.0, 3.1 and 3.2** ([openapi-parser](https://github.com/openapi-processor/openapi-parser)).
- **The docs site was unreachable.** docs.openapiprocessor.io failed TLS verification from this machine ("unable to get local issuer certificate"), so the AsciiDoc sources in the repository were read instead.

**Verified locally** with 2026.3.2 and Gradle plugin 2026.2 against Spring Boot 4.1.1:

- **3.2 works.** It accepted `openapi: 3.2.0` and generated the same code as for 3.1.
- **Records work.** `model-type: record` produced records.
- **The file part needs a mapping file, and only from 3.0.** It became `MultipartFile` only after adding `string:binary => MultipartFile` to the mapping file, as the docs require ([request body docs](https://github.com/openapi-processor/openapi-processor-spring/blob/master/docs/modules/ROOT/pages/processor/requestbody.adoc)). Even then that worked only with a 3.0 spec. With the 3.1 spelling (`contentMediaType`, no `format`), the file part was generated as `String`. `format: uuid` likewise needs a mapping to become `UUID`.
- **An optional multipart field became required.** The optional `prescriberCrm` was generated as `@RequestParam(name = "prescriberCrm") @NotNull`, so a valid request without it got a 400 (`MissingServletRequestParameterException`).
- **Length limits were dropped.** `patientName`'s `minLength`/`maxLength` produced no `@Size`, so an over-long name was accepted with 201.
- I found no issue reporting either of these two bugs (searched for "multipart" across the organisation).
- **`jackson: v3` emits a package that doesn't exist.** That mapping option produces `tools.jackson.annotation.*` imports, which don't compile. Jackson 3 keeps its annotations in `com.fasterxml.jackson.annotation` ([jackson-bom 3.1.5](https://repo1.maven.org/maven2/tools/jackson/jackson-bom/3.1.5/jackson-bom-3.1.5.pom)), as the maintainer notes in [openapi-processor-base #364](https://github.com/openapi-processor/openapi-processor-base/issues/364). Leaving the option at its default compiled.

Records and 3.2 are real advantages. But a single maintainer and two silent multipart validation bugs rule it out for now (analysis).

### Checking the server against the spec in tests

The generated interface pins paths, parameters and success types. It does not pin error bodies, status codes or the values the advice writes into `errors[].code`. A response validator in the MockMvc tests covers that.

**Atlassian's openapi-request-validator.**

- **Releases and home.** 3.0.0 was released in May 2026 ([Maven Central](https://repo1.maven.org/maven2/com/atlassian/oai/openapi-request-validator-mockmvc/maven-metadata.xml)), renamed from swagger-request-validator. Development moved from Bitbucket to [GitHub](https://github.com/atlassian/openapi-request-validator) on 2026-05-15 ([old README](https://bitbucket.org/atlassian/swagger-request-validator)).
- **Compatibility.** Its Spring modules are "Compatible with Spring 7+, Spring Boot 4+ and the Jakarta namespace" ([README](https://github.com/atlassian/openapi-request-validator)). Its docs cover OpenAPI 3.0.x and 3.1.x ([OPENAPIv3.md](https://github.com/atlassian/openapi-request-validator/blob/master/docs/OPENAPIv3.md)).
- **Multipart request bodies are not validated** ("multipart/form-data: No", same page).
- **It is small.** 17 stars, 33 open issues ([GitHub API](https://api.github.com/repos/atlassian/openapi-request-validator)).

Verified locally with 3.0.0:

- **It catches drift in the error body.** Against the 3.0 copy of the spec it validated the 201, 400 and 404 response bodies, `application/problem+json` included; against the 3.1 spec, the 400 needed the change below. Changing the advice to write `too_lng` instead of `too_long` failed the test with "does not have a value in the enumeration".
- **Multipart requests need two ignore rules.** The adapter read the form fields as unexpected query parameters and reported a missing body. Both rules had to be set to `IGNORE` for multipart requests.
- **Composed schemas fail on a 3.1 spec.** The validator injects `additionalProperties: false` into every schema by default. The `allOf` composition of `Problem` and `ValidationProblem` then rejected a valid body, a known interaction ([FAQ](https://github.com/atlassian/openapi-request-validator/blob/master/docs/FAQ.md)). The documented fix, `withResolveCombinators(true)`, worked with the 3.0 copy of the spec but not with the 3.1 spec. With 3.1, declare `ValidationProblem` flat instead of using `allOf`.

### Client: TypeScript generators

| | openapi-typescript + openapi-fetch | @hey-api/openapi-ts | Orval |
| --- | --- | --- | --- |
| Latest stable | 7.13.0 / 0.17.0, 2026-02-11 | 0.99.0, 2026-06-22 | 8.43.0, 2026-10-14 |
| Maintenance | Maintenance mode since 2026-09-19 | One main maintainer; "initial development… pin an exact version" | Several active maintainers |
| OpenAPI 3.0 / 3.1 | Yes / yes | Yes / yes | Yes / yes |
| OpenAPI 3.2 | Read as 3.1; `query` dropped (local) | Read as 3.1; `query` dropped (local) | Read; `query` generated (local) |
| zod output | None | zod 4 (3 and Mini optional) | zod 4 (3 optional) |
| Constraints in zod | n/a | `min`, `max`, `regex`, enums (local) | `min`, `max`, `regex`; enums with `generateReusableSchemas` (local) |
| File field in zod | n/a | `z.string()`, wrong ([#2317](https://github.com/hey-api/hey-api/issues/2317)) | `instanceof(Blob)` with reusable schemas (local) |
| Multipart | Needs a hand-written `bodySerializer`; without one it sends JSON (local) | Builds `FormData` (local) | Builds `FormData` (local) |
| problem+json typed | Yes (local) | Yes (local) | Yes per status; hook error type needs a throwing mutator (local) |
| TanStack Query v5 | Runtime wrapper | `queryOptions`/mutation builders | Hooks and option builders |
| Runtime code | openapi-fetch (about 6 KB) | Client generated into the repo | Inline `fetch` per operation |
| TypeScript 7 | Crashes ([#2841](https://github.com/openapi-ts/openapi-typescript/issues/2841)) | Crashes in 0.99.0; fixed only on `next` ([#4235](https://github.com/hey-api/hey-api/issues/4235)) | Works (local) |

Sources and details per tool below. "Local" results come from a run with each tool's latest release against the experiment spec. Each run had a `usage.ts` that uploads a `File`, reads `errors[0].code` from a typed 400, and reuses the generated zod schema, type-checked with `tsc --noEmit --strict` under TypeScript 6.0.3 and 7.0.2. TypeScript 7.0.2 has been npm's `latest` since 2026-07-08 ([npm registry](https://registry.npmjs.org/typescript)).

**openapi-typescript + openapi-fetch.**

- **Unmaintained.** Its maintainer announced on 2026-09-19 that it "won't be getting any more updates", security patches included, and asked users to migrate ([#2874](https://github.com/openapi-ts/openapi-typescript/issues/2874)).
- **Types only, no zod.** It emits types and no zod ([docs](https://openapi-ts.dev/introduction)).
- **Uploads go out as JSON by default.** openapi-fetch's default body serializer is `JSON.stringify`, so without a `bodySerializer` the upload went out as `application/json` with `"file":{}` and nothing warned (verified locally; [API docs](https://openapi-ts.dev/openapi-fetch/api)). File parts are typed `string` from both 3.0 and 3.1 (verified locally).
- **Error typing is good.** `{ data, error }` types non-2xx bodies for any media type, `application/problem+json` included (verified locally).
- **Ruled out:** unmaintained, and it does not work with the current TypeScript.

**@hey-api/openapi-ts.**

- **Stability.** The docs say it "is in initial development… Please pin an exact version" ([get started](https://heyapi.dev/docs/openapi/typescript/get-started)). There has been no stable release since 0.99.0 on 2026-06-22, only `next` snapshots ([npm](https://registry.npmjs.org/@hey-api/openapi-ts)).
- **Maintainers.** The repository was renamed to [hey-api/hey-api](https://github.com/hey-api/hey-api). One maintainer wrote most commits since 2025-10-21.
- **Multipart and errors work.** The SDK builds the multipart body itself, and errors are typed from the spec (verified locally).
- **zod is good except for files.** The zod plugin emits zod 4 with the constraints and the `FieldError.code` enum ([zod plugin](https://heyapi.dev/docs/openapi/typescript/plugins/zod/v4); verified locally). The exception is the file part, which it emits as `z.string()` from both 3.0 and 3.1 ([#2317](https://github.com/hey-api/hey-api/issues/2317), open since 2025-07). With the SDK's `validator: true`, every upload then failed before `fetch` with a ZodError (verified locally).
- **TanStack Query and runtime.** The TanStack plugin emits `queryOptions` and mutation builders for v5 ([plugin](https://heyapi.dev/docs/openapi/typescript/plugins/tanstack-query)). TypeScript 7 support exists only on `next`.
- **Advisories.** 0.99.0's dependency tree carried three high-severity js-yaml advisories (`pnpm audit`, verified locally); [#4474](https://github.com/hey-api/hey-api/issues/4474) is closed, but no stable release has shipped since.

**Orval.**

- **Activity.** 8.43.0 came out on 2026-10-14, after 68 stable releases in the past year ([releases](https://github.com/orval-labs/orval/releases)). v8 is ESM-only, needs Node 22.18 or later, and uses `fetch` as the default HTTP client ([v8 notes](https://orval.dev/docs/versions/v8)). Several maintainers share the commits, and there are 24 open issues.
- **OpenAPI 3.2.** It converts every input to 3.1 internally. It generated the 3.2 `query` operation (verified locally; [PR #3769](https://github.com/orval-labs/orval/pull/3769)).
- **zod.** Output targets zod 4 ([zod guide](https://orval.dev/docs/guides/zod)). With `override.zod.generateReusableSchemas: true` it emitted one schema per component (verified locally):

  ```ts
  export const PrescriptionUpload = zod.object({
    "file": zod.instanceof(Blob),
    "patientName": zod.string().min(1).max(prescriptionUploadPatientNameMax),
    "prescriberCrm": zod.string().regex(prescriptionUploadPrescriberCrmRegExp).optional()
  });
  export const FieldError = zod.object({ "field": zod.string(),
    "code": zod.enum(['required', 'too_long', 'too_short', 'bad_format', 'unsupported_media_type']), ... });
  ```

  Without that flag it emits only per-operation schemas, and nothing for error responses.
- **Multipart.** The generated function builds `FormData` (verified locally). One quirk: a string passed as the file is wrapped in a `Blob` whose type is the whole `encoding.contentType` list.
- **Errors.** The default fetch client never throws. It returns `{ data, status }` with one type per status, so narrowing on `status === 400` gives a typed `ValidationProblem`. **The generated TanStack hooks, though, declare the 400 type as `TError` while the request resolves successfully.** TanStack reported `status: "success"` with `data.status: 400` (verified locally). A 14-line custom mutator that throws on `!res.ok` fixes it ([custom client guide](https://orval.dev/docs/guides/custom-client)). After that, `mutation.error.body.errors[0].code` is typed and correct (verified locally).
- **Runtime.** No runtime package; each operation is inline `fetch`.

**Others.**

- **Kubb** has zod 4 and TanStack plugins, but has made 233 CLI releases in a year, has one dominant maintainer, and its own adapter still warns that 3.2 is unsupported ([#4284](https://github.com/kubb-labs/kubb/issues/4284)).
- **openapi-zod-client** says it is deprecated in favour of typed-openapi, a one-maintainer project ([typed-openapi](https://github.com/astahmer/typed-openapi)).

### Assessment

- **Hexagonal fit.** Good. The generated interfaces and DTO classes live only in the REST adapter's build output. The controller implements the interface and maps DTOs to domain commands and back in the adapter, so the domain and application modules never see generated types (analysis). The cost is mutable DTO classes in the adapter instead of records.
- **Multipart.** Both sides handle it. The Java side generates `MultipartFile` plus validated form fields; the Orval client builds `FormData` (verified locally). The response validator can't check multipart requests, only their responses.
- **Problem details.** Declared once in the spec. On the client they are fully typed, error codes included, once the throwing mutator is in place. On the server they are written by hand in the advice and checked against the spec only by the response-validation test (verified locally).
- **What breaks the build:**
  - The Java compile breaks when an operation is added or removed (with `skipDefaultInterface`), or when a parameter, request type or response type changes.
  - `tsc` in `web/` breaks when the regenerated client no longer matches its callers.
  - The validator test breaks when an error body, status code or enum value leaves the spec.
  - **Nothing breaks** when a server change bypasses the interface, such as an extra handler method outside it, or when a problem response has no validator test (analysis).
- **Cost per endpoint** (analysis):
  - About 30–60 lines of spec YAML.
  - One controller method with its mapping code.
  - One advice case per new error kind.
  - A validator assertion in the existing MockMvc tests.
  - On the web side, nothing generated is written by hand; what remains is calling the hook and wiring the zod schema into react-hook-form.
- **Activity and compatibility.** openapi-generator 7.26.0 compiles for Boot 4.1 and Java 25, with Boot 4 support eight months old and still being patched. Orval is the most active of the TypeScript generators. OpenAPI 3.2 is out of reach on the Java side.
- **Renovate.** The generator Gradle plugin and Orval are ordinary dependencies. Generated code isn't committed on the Java side, so a generator bump simply shows up as compile or test results in the Renovate PR. For `web/`, Renovate will not regenerate committed files on its hosted app, because post-upgrade commands are blocked by default ([postUpgradeTasks](https://docs.renovatebot.com/configuration-options/#postupgradetasks)). Either generate at build time, or regenerate in CI and fail on `git diff --exit-code` (analysis).
- **Lock-in.** Low. The spec is a standard document, and either generator can be swapped against it. The code that depends on the generator is the controller signatures and the hook calls.

## 2. OpenAPI code-first

Controllers and DTO records are written by hand. springdoc derives the spec from them at runtime. The spec is committed, and the TypeScript side is generated from it as in section 1.

### springdoc-openapi

**Status.**

- **Spring Boot 4 is supported on the 3.x line.** "springdoc-openapi 3.x is compatible with spring-boot 4" ([FAQ](https://springdoc.org/faq.html)).
- **Latest: 3.1.1, 2026-09-06.** 3.1.0 upgraded to Spring Boot 4.1.0 and swagger-core 2.2.52 ([3.1.0 release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.0)). 3.1.1 fixed seven security advisories, most in its new MCP and Scalar modules, and made MCP opt-in ([3.1.1 release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.1)).
- **One maintainer.** bnasslahsen has 2,352 commits; the next contributor has 34 ([contributors](https://github.com/springdoc/springdoc-openapi/graphs/contributors)).
- **Spec version.** Output defaults to OpenAPI 3.1 (`springdoc.api-docs.version`, default `openapi_3_1`, [properties](https://springdoc.org/properties.html)). It cannot emit 3.2, because it builds on swagger-core.

**Generating the spec at build time.**

- **The official Gradle plugin is stale.** [springdoc-openapi-gradle-plugin](https://github.com/springdoc/springdoc-openapi-gradle-plugin) starts the application and downloads `/v3/api-docs`. Its last release is 1.9.0 (2024-06-22) and its last commit 2024-06-23. Open issues ask for Java 25 support ([#172](https://github.com/springdoc/springdoc-openapi-gradle-plugin/issues/172)) and Spring Boot Gradle plugin compatibility ([#173](https://github.com/springdoc/springdoc-openapi-gradle-plugin/issues/173)), and report configuration-cache incompatibility ([#166](https://github.com/springdoc/springdoc-openapi-gradle-plugin/issues/166)).
- **A test does the job without the plugin** (verified locally):
  - A `@SpringBootTest` fetches `/v3/api-docs` through MockMvc and compares it with the committed `openapi.json`. A Gradle property rewrites the file.
  - With `springdoc.writer-with-order-by-keys=true` the output was stable between runs.
  - Changing a controller made the test fail until the file was regenerated, so CI catches a forgotten update.
- **springdoc can stay out of production.** It can be a test-only dependency: production then carries only `swagger-annotations-jakarta`, managed by `springdoc-openapi-bom` since 3.1.1 ([release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.1)), and serves no `/v3/api-docs` (verified locally).

**What the spec says with plain annotations** (verified locally, springdoc 3.1.1 on Spring Boot 4.1.1). On the controller as one would first write it, the spec came out wrong in four places:

1. **`@RequestParam` fields of a multipart request were documented as `in: query`, not as parts of the multipart body.** A client generated from that spec would put the patient's name in the URL, where proxies and access logs keep it. Declaring the text fields as `@RequestPart` fixed the spec. Spring still bound browser-style text parts (no `Content-Type`) to `String` and enforced `@Size` on them.
2. **Record components were not `required`.** Every field of every record came out optional, so the generated TypeScript types became `field?: T` throughout. Adding `@NotNull` to each non-null component produced the `required` lists. JSpecify's `@Nullable` was ignored, so a field the server sends as `null` was typed as a non-nullable optional string.
3. **Error responses are absent unless declared.** Each problem response needed an explicit `@ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ValidationProblem.class)))`.
4. **Spring's `ProblemDetail` documented a `properties` map that is never sent.** Its schema came out with a `properties: object` member. Jackson writes those properties as top-level members instead. A record of our own avoids it.

After those fixes the spec carried `minLength`, `maxLength`, `pattern`, `format: uuid`, the error-code enum and the required lists (verified locally). That is what the TypeScript generators need.

### Assessment

- **Hexagonal fit.** Good, and closer to the first slice's code: hand-written controllers and request/response records in the adapter, with mapping to the domain there (analysis). Spec annotations (`@ApiResponse`, `@Schema`) sit on adapter classes only.
- **Multipart.** Supported, but only correct with `@RequestPart` on the text fields (verified locally).
- **Problem details.** A hand-written record, attached by annotation to each operation. On the client, typed as in section 1.
- **What breaks the build:**
  - The snapshot test breaks when the generated spec differs from the committed one.
  - `tsc` breaks when the regenerated client no longer fits its callers.
  - **Nothing breaks** when the annotations describe the code wrongly. Points 1–4 above all produced a valid but wrong spec with no warning. Review of the committed spec diff is the only check (analysis).
- **Cost per endpoint.** About the same Java as hand-written controllers, plus three to six annotations per operation and `@NotNull` on DTO components. No YAML (analysis).
- **Activity.** springdoc is active, and its Boot 4.1 support was verified locally. The build-time plugin is effectively unmaintained, and the project rests on one maintainer.
- **Renovate.** springdoc and swagger-annotations are ordinary dependencies. A springdoc bump can change the spec (ordering, schema details), which the snapshot test surfaces in the Renovate PR (analysis).
- **Lock-in.** Low to moderate. The spec stays standard, but the annotations tie the controllers to swagger-core's annotation model.
- **Against the author's values.** The contract is derived by reflection from annotations whose defaults were wrong four times in a two-endpoint experiment. That is "magic" in exactly the sense the author wants to avoid (analysis).

## 3. Hand-written on both sides

This is the first slice's current approach. The Java controller and records are written by hand, and so are the TypeScript types, fetch functions, zod schemas and TanStack Query options. Playwright end-to-end tests exercise both. Playwright can also call the API directly, without a browser ([API testing](https://playwright.dev/docs/api-testing)).

### Assessment

- **Hexagonal fit.** Same as code-first: records and mapping in the adapter (analysis).
- **Multipart and problem details.** Whatever the code does. Nothing describes them except the code on each side and the tests (analysis).
- **What breaks the build.** Only a test that exercises the changed path. Renaming or retyping a field fails the end-to-end flow that sends it. These drift silently until some test happens to cover them (analysis):
  - an optional field that the UI rarely sends;
  - a new error code;
  - a changed `maxLength` that the zod schema doesn't mirror;
  - a field that becomes nullable;
  - a status code used only on an error path.

  End-to-end tests seldom cover every error path, because each one needs its own setup.
- **Cost per endpoint** (analysis):
  - On the Java side, about the same as code-first.
  - In TypeScript: the types, a fetch function with error parsing, a zod schema that copies the Bean Validation rules, and query options.
  - At least one test per error shape that the UI depends on.

  Roughly twice the code of a generated client, and the duplicated validation rules are the part most likely to diverge.
- **Activity, compatibility, Renovate.** No contract tooling to update.
- **Lock-in.** None.
- **Contract tests instead of end-to-end.** Consumer-driven contracts would add machinery without a second team (section 5). A JSON-schema check on responses needs a schema, which is a step back to OpenAPI.

## 4. Protobuf contract-first with Connect

The API is defined in `.proto` files in the repository. Java message classes and service base classes are generated from them, and so are the TypeScript messages (`protoc-gen-es`, protobuf-es v2) used by connect-es (`@connectrpc/connect-web`) and connect-query in the browser. The inbound adapter becomes an RPC adapter.

### What a Java server can offer a browser

A browser can speak the Connect protocol or gRPC-Web, but not gRPC over HTTP/2. The FAQ says "Most of Google's gRPC implementations don't support gRPC-Web" and points existing gRPC servers to Envoy ([Connect FAQ](https://connectrpc.com/docs/faq)).

| Server | Browser protocol | In front of it | Same origin as the SPA |
| --- | --- | --- | --- |
| Connect's own JVM server | Does not exist | – | – |
| Armeria 1.42.0 + `armeria-spring-boot4-starter` | gRPC-Web; JSON unary over plain POST, close to but not the Connect protocol | Nothing | Yes: Armeria owns the port and forwards the rest to Spring MVC's Tomcat |
| Spring Boot 4.1 gRPC (Spring gRPC 1.1.1) | Neither | Envoy (`grpc_web` or `connect_grpc_bridge`) | Through Envoy routing |
| grpc-java servlet transport | Neither (HTTP/2 gRPC only) | Envoy | Through Envoy |
| Community JVM Connect servers | Connect | Own Netty or WebFlux server | Mostly separate servers; all pre-1.0 |

- **Connect itself has no Java server.**
  - The [connectrpc organisation](https://github.com/connectrpc) has implementations for Go, ES, Swift, Kotlin, Dart, Python and Rust.
  - [connect-kotlin](https://github.com/connectrpc/connect-kotlin) is a client.
  - The [conformance suite](https://github.com/connectrpc/conformance) lists no JVM server.
  - Connect is a CNCF sandbox project ([project.yaml](https://github.com/connectrpc/.project/blob/main/project.yaml)), and the connect-es maintainers work at Buf ([MAINTAINERS.md](https://github.com/connectrpc/connect-es/blob/main/MAINTAINERS.md)).
- **Armeria** (1.42.0, 2026-10-06, [releases](https://github.com/line/armeria/releases)).
  - **What it speaks.** Its `GrpcService` serves gRPC, gRPC-Web and "unframed" unary requests as `application/json` POSTs ([docs](https://armeria.dev/docs/server/grpc)). That is close to Connect unary but not the Connect protocol: `application/proto` is not accepted ([UnframedGrpcService](https://github.com/line/armeria/blob/main/grpc/src/main/java/com/linecorp/armeria/server/grpc/UnframedGrpcService.java)), and the error hook is `@UnstableApi`.
  - **Spring Boot 4.** Armeria has a Spring Boot 4 starter since 1.35.0 ([PR #6549](https://github.com/line/armeria/pull/6549)) and builds against Spring Boot 4.1.1 ([dependencies.toml](https://github.com/line/armeria/blob/main/dependencies.toml)). Serving Spring MVC on the same port goes through `armeria-tomcat10` ([Spring Boot integration](https://armeria.dev/docs/advanced/spring-boot-integration)), which is built against Tomcat 10.1. Boot 4.1.1 ships Tomcat 11.0.24.
  - **Verified locally** (Armeria with Spring Boot 4.1.1, the SPA, MVC and RPC all on one port):
    - **Errors.** Connect-JSON success worked. Errors reached connect-es as `Internal` with the details lost, until a 74-line custom error handler wrote Connect's error format. gRPC-Web worked with typed error details out of the box.
    - **Rejected requests.** Connect binary got 415 and Connect GET got 405.
    - **Warnings.** Java 25 printed `sun.misc.Unsafe` warnings from Armeria's shaded JCTools.
    - **A second Jackson.** Armeria pulled in Jackson 2.22.3 next to Boot's Jackson 3.1.5.
- **Spring Boot 4.1 gRPC.**
  - **Built into Boot, no browser protocol.** Spring gRPC's autoconfiguration moved into Boot 4.1 ([Spring gRPC 1.1.0](https://github.com/spring-projects/spring-grpc/releases/tag/v1.1.0)), and the Boot 4.1.1 BOM manages `spring-boot-starter-grpc-server`, grpc-java 1.83.1 and protobuf-java 4.35.1 ([BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)). Boot's gRPC docs mention neither gRPC-Web nor Connect ([Boot gRPC docs](https://github.com/spring-projects/spring-boot/blob/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/io/grpc.adoc)). The request for gRPC-Web is open ([spring-grpc #427](https://github.com/spring-projects/spring-grpc/issues/427)).
  - **Envoy has to sit in front.** Envoy's `grpc_web` filter is `stable`. Its `connect_grpc_bridge` filter is `alpha` with `security_posture: unknown` ([extensions_metadata.yaml](https://github.com/envoyproxy/envoy/blob/main/source/extensions/extensions_metadata.yaml)).
  - **Verified locally** (Envoy 1.40.0 in front of Boot's gRPC server, SPA routed to Tomcat):
    - gRPC-Web worked, including a 10 MB upload.
    - Connect JSON failed with `unknown`.
    - Connect binary worked, but error details came back under full `type.googleapis.com/…` names that `findDetails` did not match without a client-side shim.
- **grpc-java's servlet transport** is `@ExperimentalApi` and needs HTTP/2 ([ServletAdapter](https://github.com/grpc/grpc-java/blob/master/servlet/src/main/java/io/grpc/servlet/ServletAdapter.java)). Built-in gRPC-Web support has been an open request since 2018 ([grpc-java #4823](https://github.com/grpc/grpc-java/issues/4823)).
- **Community Connect servers are hobby-scale.** For example, [connect-rpc-java](https://github.com/igor-vovk/connect-rpc-java) is at v0.2.1 with one maintainer, and [connectrpc-spring](https://github.com/neilpmas/connectrpc-spring) is a 0.1.2 pre-release.

### buf

- **Licence.** The CLI is Apache-2.0 (1.74.0, 2026-10-14, [bufbuild/buf](https://github.com/bufbuild/buf)) and installs from npm as `@bufbuild/buf`.
- **`buf breaking` levels.** `WIRE_JSON` "Detects changes that break wire (binary) or JSON encoding", and the docs say to use it with Connect ([rules](https://buf.build/docs/breaking/rules/)). Verified locally: a field rename and an `int64` to `int32` change were flagged under `WIRE_JSON` but not under `WIRE`.
- **Generation works offline.** `buf generate` accepts `local:` plugins, `protoc_builtin:` and `remote:` ([buf.gen.yaml](https://buf.build/docs/configuration/v2/buf-gen-yaml/)). Verified locally: with protoc-gen-es 2.16.0, protoc 36.2 and the grpc-java plugin as local plugins, plus vendored imports, `buf lint`, `buf breaking` and `buf generate` ran with networking disabled.
- **The Buf Schema Registry is optional.** It is free for public repositories ([pricing](https://buf.build/pricing)). Unauthenticated remote code generation is limited to 10 requests per hour per IP ([rate limits](https://buf.build/docs/bsr/rate-limits/)).
- **Renovate.** Renovate has no buf manager ([managers](https://github.com/renovatebot/renovate/tree/main/lib/modules/manager)), and a PR adding a buf datasource was closed unmerged on 2026-09-22 ([#46369](https://github.com/renovatebot/renovate/pull/46369)). Verified locally with Renovate 44.175.0 in extract mode:
  - It found the npm packages (buf, the plugins, the runtimes) and the Gradle protoc and grpc-java artifacts.
  - It ignored `buf.yaml`, `buf.gen.yaml` and `buf.lock`.

### Uploading a prescription

- **No streaming uploads from a browser.** Browsers cannot stream request bodies with `fetch`, so connect-web supports "only server streaming" ([getting started](https://connectrpc.com/docs/web/getting-started); it throws "The fetch API does not support streaming request bodies", [source](https://github.com/connectrpc/connect-es/blob/v2.2.0/packages/connect-web/src/connect-transport.ts)). A 10 MB file is therefore one unary message with a `bytes` field.
- **JSON adds a third.** In JSON, `bytes` are base64 ([JSON mapping](https://protobuf.dev/programming-guides/json/)), so 10,000,000 bytes became a 13,333,336-character body.
- **Size limits differ along the path.** grpc-java's default inbound limit is 4 MiB ([GrpcUtil](https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/internal/GrpcUtil.java)) and Armeria's is 10 MiB ([DefaultFlagsProvider](https://github.com/line/armeria/blob/main/core/src/main/java/com/linecorp/armeria/common/DefaultFlagsProvider.java)), so limits must be raised at every hop.
- **Verified locally:** a 10,000,000-byte upload over JSON got 413 at Armeria's 10 MiB limit, while binary gRPC-Web succeeded. With the limit raised, the JSON upload succeeded, but the Node test client peaked around 706 MB of memory, against about 150 MB for binary.
- **Connect says nothing about uploads.** Its documentation has no guidance on file uploads (searched the docs source). A plain multipart endpoint beside the RPCs remains an option, outside the proto contract (analysis).

### Field errors and validation

- **Connect's error model.** Errors are `application/json` `{code, message, details[]}`, with 16 fixed codes and "no user-defined codes". `invalid_argument` maps to HTTP 400. Each detail is a type name plus base64 binary protobuf ([protocol](https://connectrpc.com/docs/protocol)). It is not RFC 9457: no `application/problem+json`, no free-form members, and an application code has to live inside a protobuf detail.
- **The client reads details as typed messages.** connect-es reads details with `err.findDetails(Schema)`; `google.rpc` types are not bundled and must be generated ([errors](https://connectrpc.com/docs/web/errors)).
- **protovalidate puts the rules in the proto.**
  - Rules such as `min_len`, `max_len`, `pattern`, `uuid` and CEL expressions are declared there ([protovalidate](https://github.com/bufbuild/protovalidate), 1.2.2).
  - Each violation carries a stable `rule_id` such as `string.max_len`, a field path and an English message.
  - The Java runtime is [protovalidate-java](https://github.com/bufbuild/protovalidate-java) 1.3.0. Wiring it into the server is a sample interceptor that one copies ([buf-examples](https://github.com/bufbuild/buf-examples/blob/main/protovalidate/grpc-java/finish/src/main/java/buf/build/example/protovalidate/ValidationInterceptor.java)).
- **The same rules can drive the forms.** protovalidate-es exports a Standard Schema adapter ([standard-schema.ts](https://github.com/bufbuild/protovalidate-es/blob/main/packages/protovalidate/src/standard-schema.ts)), and `@hookform/resolvers` has a `standard-schema` resolver. Verified locally: react-hook-form ran the proto's rules through that resolver, with no zod schema to keep in step. The resolver's issues carry only message and path (no `rule_id`), messages are English, and paths are snake_case in `Violations` but camelCase in the generated TypeScript.
- **No zod generator.** I found no maintained protoc plugin that emits zod.

### Network tab and session

- **JSON by default.** connect-web sends JSON unless `useBinaryFormat` is set ([choosing a protocol](https://connectrpc.com/docs/web/choosing-a-protocol)). A unary call is a readable `POST` with `application/json`. Error details stay base64 binary even then.
- **gRPC-Web is opaque to logs.** It is binary, and a failure arrives as HTTP 200 with `grpc-status: 3` (verified locally), so access logs and proxies see success.
- **Cookies and CSRF.** Same-origin `fetch` carries the session cookie. connect-web sets `redirect: "error"` (same source file), so a backend-for-frontend must answer 401 rather than redirect to a login page. Connect `GET` requests need no CORS preflight, so `GET` must stay limited to side-effect-free methods ([CORS](https://connectrpc.com/docs/cors)).

### Assessment

- **Hexagonal fit.** Good. The adapter extends the generated `…ImplBase` and maps immutable protobuf messages (builders, not records; [Java generated code](https://protobuf.dev/reference/java/java-generated/)) to domain types. The domain stays free of protobuf.
- **Multipart.** Not a concept. The upload is a unary `bytes` field: base64 in JSON, or binary only through gRPC-Web. The size limit must be raised at every hop.
- **Errors.** Typed on both sides as protobuf details (`google.rpc.BadRequest`, or protovalidate `Violations` with `rule_id` as the stable code). This replaces RFC 9457, which AmpolaTech has already chosen.
- **Forms.** protovalidate rules can drive react-hook-form directly (verified locally), the closest any option comes to a single source for validation.
- **What breaks the build:**
  - Regenerated TypeScript stops compiling against its callers (verified locally for a field rename).
  - Java fails the same way (not demonstrated).
  - `buf breaking --against` the main branch at `WIRE_JSON` catches renames, type changes and unreserved deletions before they ship.
  - That is the strongest drift protection of the four options.
- **Cost per endpoint.** An `rpc`, its messages with rules, one adapter method with mapping, one connect-query call (analysis). One-time costs: buf and protobuf Gradle setup, plus one of a custom Connect error handler on Armeria, an Envoy deployment, or acceptance of gRPC-Web.
- **Activity and compatibility.** The Buf-side projects are very active, and everything ran on Java 25. The weak link is the JVM server: no official Connect server, Armeria's Tomcat 10 bridge and Jackson 2 seam, or an alpha Envoy filter.
- **Renovate.** npm and Gradle artifacts update. `buf.lock` and remote plugin pins do not, so local plugins and vendored protos are the Renovate-friendly setup.
- **Lock-in.** The proto contract is portable. The browser protocol and its JVM glue are where lock-in sits.

## 5. Other options

- **Spring REST Docs + restdocs-api-spec (spec produced from tests).**
  - **Drift checks are strong on the server side.** REST Docs fails a test when a payload has an undocumented field or lacks a documented one ([REST Docs](https://docs.spring.io/spring-restdocs/reference/documenting-your-api/request-response-payloads.html)).
  - **Boot 4 support is recent.** restdocs-api-spec 0.20.2 (2026-10-16) supports Spring Boot 4 since 0.20.0 ([releases](https://github.com/ePages-de/restdocs-api-spec/releases)).
  - **But the output format is limited.** It emits only OpenAPI 3.0.1, and multipart support has been an open issue since 2019 ([#105](https://github.com/ePages-de/restdocs-api-spec/issues/105)).
  - **Verdict:** no. It still needs an OpenAPI-to-TypeScript generator, and it fails on the upload.
- **typescript-generator (Java classes to TypeScript types).**
  - **Recently revived.** 4.2.1 came out on 2026-10-20, after a dormant period from 2023 to February 2026. 4.0.0 added Jackson 3 and Spring Boot 4 ([releases](https://github.com/vojtechhabarta/typescript-generator/releases)).
  - **It covers DTO types only.** There are no endpoints beyond its own client shape, no zod, no validation rules ([#997](https://github.com/vojtechhabarta/typescript-generator/issues/997)), and multipart is not supported ([#215](https://github.com/vojtechhabarta/typescript-generator/issues/215)).
  - **Verdict:** no. It is a partial contract from a project that went dormant once already.
- **Vaadin Hilla.**
  - **Version.** Hilla 25.x runs on Spring Boot 4 ([25.0.0](https://github.com/vaadin/hilla/releases/tag/25.0.0)).
  - **It is RPC under `/connect`, not REST** ([client source](https://github.com/vaadin/hilla/blob/25.3.3/packages/ts/frontend/src/Connect.ts)).
  - **It has its own error format.** It is not RFC 9457; that request has been open since 2019 ([#349](https://github.com/vaadin/hilla/issues/349)).
  - **It generates the Vite config.** Its Maven/Gradle goal regenerates the Vite configuration on every run ([vite.generated.ts](https://github.com/vaadin/flow/blob/main/flow-server/src/main/resources/vite.generated.ts)).
  - **It brings its own form library.** It ships one mirroring Bean Validation instead of react-hook-form + zod ([validators](https://github.com/vaadin/hilla/blob/25.3.3/packages/ts/lit-form/src/Validators.ts)).
  - **Verdict:** no. It conflicts with REST, problem details, the chosen form stack and a hand-picked Vite setup.
- **GraphQL (Spring for GraphQL + GraphQL Code Generator).**
  - **Version.** Spring for GraphQL 2.0.x is managed by Boot 4.1.1 ([BOM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom)).
  - **No uploads.** "Spring for GraphQL does not support the `graphql-multipart-request-spec` directly" ([transports](https://docs.spring.io/spring-graphql/reference/transports.html)), so the upload would stay a REST endpoint.
  - **No HTTP error semantics.** After validation, "the HTTP response status is always 200 (OK)" and errors go in the `errors` section (same page). That replaces problem details and HTTP status semantics.
  - **Verdict:** no. It means two API styles and buys query flexibility the app doesn't need.
- **Consumer-driven contracts.**
  - **Pact works with Boot 4, but only for responses.** pact-jvm 4.7.5 (2026-08-10) has a Spring 7 provider module ([pact-jvm](https://github.com/pact-foundation/pact-jvm/tree/master/provider/spring7)). Pact files can be committed instead of using a broker ([sharing pacts](https://docs.pact.io/getting_started/sharing_pacts)). Pact checks response shape, not side effects ([what Pact is good for](https://docs.pact.io/getting_started/what_is_pact_good_for)).
  - **Spring Cloud Contract is gone.** Its repository was archived on 2026-07-06, and Spring is removing it from all future Spring Cloud release trains ([Spring blog](https://spring.io/blog/2026/07/06/spring-cloud-contract-transition-to-stubbornsh); [repository](https://github.com/spring-cloud/spring-cloud-contract)).
  - **Verdict:** no. CDC protects independently deployed consumers. Here one person changes both sides in one commit, and a shared contract plus provider tests gives the same guarantee with less machinery (analysis).

## Options for AmpolaTech (analysis)

Everything in this section is analysis, weighed against [the author's stated values](../conventions/author-values.md): correctness over speed, explicit over magic, principled decisions, and extra code accepted for a cleaner result.

| | 1. OpenAPI contract-first | 2. OpenAPI code-first | 3. Hand-written | 4. Protobuf + Connect |
| --- | --- | --- | --- | --- |
| Source of the contract | A reviewed spec file | Annotations, read by reflection | None; two codebases | Reviewed `.proto` files |
| Server side on Boot 4.1 / Java 25 | openapi-generator 7.26.0, several defaults to override | springdoc 3.1.1; build plugin stale, a test replaces it | Nothing to adopt | No Java Connect server; Armeria or Envoy |
| DTOs in the adapter | Generated mutable classes | Hand-written records | Hand-written records | Generated immutable protobuf messages |
| Multipart upload | Yes | Yes, if text fields are `@RequestPart` | Yes | No; unary `bytes`, base64 in JSON |
| RFC 9457 problems | Declared in the spec; typed on the client; server checked by a validator test | Declared by annotations; same client typing | Hand-typed on both sides | Replaced by Connect errors and protobuf details |
| zod for forms | Generated from the spec (Orval) | Generated from the derived spec | Hand-copied | Not needed: protovalidate through Standard Schema |
| Server drift fails | The Java compile, and the validator test for errors | Only the snapshot diff, reviewed by eye | Only a test that hits the path | The Java compile, and `buf breaking` |
| Client drift fails | `tsc` | `tsc` | A test that hits the path | `tsc` |
| Silent failure mode | Error-body paths with no validator test | A wrong annotation gives a valid, wrong spec | Every untested path | Error details lost by faulty JVM protocol glue (seen with Armeria's default handler) |
| Cost per endpoint | Spec YAML + controller method + mapping | Controller + annotations + mapping | Controller + TS types + fetch + zod + tests | Proto + adapter method + mapping |
| Tool risk | Generator defaults; 3.1 marked beta; no 3.2 | One maintainer; stale build plugin | None | JVM server glue; alpha Envoy filter or pre-1.0 libraries |
| Lock-in | Low | Low to moderate | None | Moderate: the browser protocol |

**Recommendation: option 1, OpenAPI contract-first**, set up as follows:

1. **One spec, OpenAPI 3.1, in a place both builds read** (for example `api/openapi.yaml` at the repository root). Not 3.2: openapi-generator rejects it.
   - Keep problem schemas flat, without `allOf`, so the response validator accepts them.
2. **openapi-generator `spring` in the REST adapter's Gradle build**, generating into `build/` (not committed). Set explicitly:
   - `interfaceOnly`, `useSpringBoot4`, `useJackson3`;
   - `useSpringBuiltInValidation=true`, so validation failures become 400 problems;
   - `skipDefaultInterface=true`, so a new operation fails the build;
   - `useJspecify`, `useTags`;
   - `documentationProvider=none`, `annotationLibrary=none`, `openApiNullable=false`;
   - both `generate…Annotations` flags.

   Controllers implement the interfaces and map generated DTOs to domain commands and results, inside the adapter.
3. **One `@RestControllerAdvice`** that writes every problem body, plus an openapi-request-validator assertion on each error-response test. That turns the spec's error schemas into a checked contract on the server side too.
4. **Orval in `web/`**:
   - the TanStack Query client;
   - zod output with `generateReusableSchemas`, so forms reuse the spec's limits and the error-code enum;
   - a small throwing mutator, so hook errors are typed truthfully.

   Either generate before `tsc` without committing the output, or commit it and fail CI on `git diff --exit-code`. Renovate's hosted app won't regenerate committed files.
5. **Playwright stays for user flows, not for contract drift.**

**Why this one.**

- **It best matches "explicit over magic".** The contract is a file, written on purpose and reviewed like code.
- **The generators fail loudly on both sides.** That held for every drift tried locally except error bodies, and the validator test closes that gap.
- **The author already knows the approach.**
- **The extra code it asks for is the kind the author accepts:** spec YAML (48 lines of paths for two operations in the experiment) and explicit mapping out of generated DTOs.
- **The costs are known.** Generated DTOs are mutable classes in 7.26.0, with records due in a later release (`useRecords`); OpenAPI 3.2 is unavailable until swagger-parser supports it; a handful of generator options must be overridden and kept overridden.

**What would change the recommendation.**

- **Switch to code-first (option 2)** if the author prefers hand-written records and Java as the only source, and accepts that wrong annotations produce a wrong but valid spec. Then:
  - run springdoc only in tests;
  - commit the spec through a snapshot test;
  - declare multipart text fields as `@RequestPart`;
  - mark non-null record components `@NotNull`;
  - use an own problem record instead of `ProblemDetail`.

  Review of the committed spec diff becomes the main safeguard.
- **A middle path.** Generate only the TypeScript side from a hand-written spec, and hold the Java side to the spec with validator tests instead of generated interfaces. It keeps records and loses the compile-time failure on a new or removed operation. Multipart requests also cannot be validated, so it is weaker than option 1.
- **Protobuf with Connect (option 4)** is the most principled contract on paper:
  - `buf breaking` and protovalidate give the strongest drift checks and one validation source for server and forms.
  - It loses here on three facts that hold today: no maintained Java server speaks the Connect protocol next to Spring MVC; the upload doesn't fit (base64 in JSON or opaque binary gRPC-Web); and errors leave RFC 9457.
  - Revisit if an official or well-maintained JVM Connect server appears, or if the author decides problem details are not a requirement.
- **Hand-written (option 3)** is acceptable only while the API is a handful of endpoints. Its cost grows with every endpoint, and its blind spots are the error paths and optional fields that end-to-end tests rarely reach.

## Not verified

- **openapi-generator, outside the experiment.** Its behaviour on larger specs (`oneOf`, discriminators, pagination) was not tested.
- **The Gradle deprecation's source** was not traced.
- **The response validator on a 3.1 spec.** Why `withResolveCombinators(true)` did not flatten `allOf` for the 3.1 spec was not traced to its cause.
- **openapi-processor-spring's two multipart bugs** were reproduced but not reported upstream.
- **springdoc as a production dependency.** Only the test-scoped setup was run.
- **TypeScript generators: counts and causes.**
  - Maintainer counts are commit counts on the default branch since 2025-10-21, not a measure of active maintainers.
  - Why hey-api has had no stable release since 2026-06-22 is unknown; no announcement was found.
  - Orval's 3.2 coverage beyond the `query` operation was not tested.
- **TypeScript generators: settings not tried.** Results under `noUncheckedIndexedAccess` are untested; the type checks used `strict`.
- **TypeScript generators: Renovate.** How Renovate handles hey-api's exact-pinned 0.x versions, and what the Mend-hosted plan allows for post-upgrade commands, were not tested.
- **Connect on the server side.**
  - Whether `armeria-tomcat10` is supported on Tomcat 11; it only ran locally.
  - Which Envoy limit produced a 413 on a 10 MB Connect-binary upload.
  - Browser memory for a 10 MB JSON upload; only a Node client was measured.
  - The Java compile failure on proto drift; only the TypeScript one was shown.
- **Connect: tooling.** Conformance of the community JVM Connect servers was not tested. Neither was a BSR dependency in place of vendored protos.
- **Renovate on buf.** Whether Renovate opens pull requests for buf-related versions was not tested; only its extract mode was run.
- **Other options.**
  - restdocs-api-spec 0.20.x on Spring Boot 4.1.x (it builds against 4.0.5).
  - Hilla with TanStack Router in place of its own routing.
  - pact-jvm's Spring 7 module on JUnit Jupiter 6.
  - Whether pact-js's V4 multipart body can express text parts beside the file.

## Appendix: local experiments

All runs on 2026-10-21, in throwaway projects outside the repository, with the toolchain listed at the top. The experiment spec has two operations: `POST /api/prescriptions` (multipart: `file`, required `patientName` with `minLength: 1` and `maxLength: 120`, optional `prescriberCrm` with a `pattern`; 201 `Prescription` or 400 `application/problem+json` `ValidationProblem`) and `GET /api/prescriptions/{id}` (200, or 404 `Problem`). `ValidationProblem` is `allOf` `Problem` plus a required `errors` array of `FieldError {field, code (enum), message}`. It was written in three copies: 3.0.3 (`format: binary`, `nullable`), 3.1.1 (`contentMediaType`, `type: [string, 'null']`) and the same as 3.2.0. The two operations took 48 lines under `paths` and 82 under `components`.

1. **openapi-generator CLI 7.26.0** (jar from Maven Central, SHA-1 checked). `validate` passed 3.0 and 3.1 and failed 3.2 with nine errors. `generate -g spring` with `interfaceOnly` worked for 3.0 and 3.1, logging the 3.1 beta warning, and failed for 3.2. With `skipDefaultInterface` left `false`, each method got a default body returning `501 NOT_IMPLEMENTED`.
2. **Contract-first Gradle project.**
   - **Setup.** Plugin `org.openapi.generator` 7.26.0, generating into `build/` with `interfaceOnly`, `useSpringBoot4`, `useJackson3`, `useTags`, `skipDefaultInterface`, `useSpringBuiltInValidation`, `useJspecify`, `documentationProvider=none`, `annotationLibrary=none` and `openApiNullable=false`. A controller implements the interface, and an advice extends `ResponseEntityExceptionHandler` to write `ProblemDetail` with `code` and `errors`.
   - **Base tests.** Four `@WebMvcTest` tests passed: upload 201, over-long name plus bad pattern giving a 400 with two `errors`, missing file giving a 400 with `errors[0].field = file`, and a 404 problem.
   - **Drift.** Adding a `DELETE` operation and retyping `fileName` to `integer` produced two compile errors in the controller. Setting `useSpringBuiltInValidation=false` made the 400 test fail with `ConstraintViolationException`.
   - **Response validator.** openapi-request-validator-mockmvc 3.0.0 passed the 201 and 404 checks against the 3.1 spec. It failed the 400 check (allOf plus injected `additionalProperties: false`) even with `withResolveCombinators(true)`, and passed all three against the 3.0 copy. Changing the advice's `too_long` to `too_lng` failed the 400 check on the enum. Two request-side keys (`validation.request.body.missing`, `validation.request.parameter.query.unexpected`) had to be ignored for multipart requests.
3. **openapi-processor-spring 2026.3.2**, Gradle plugin `io.openapiprocessor.openapi-processor` 2026.2, `model-type: record`, `bean-validation: jakarta`.
   - **Parsing.** Generation succeeded for 3.0, 3.1 and 3.2.
   - **Mappings.** `MultipartFile` and `UUID` needed type mappings, and `string:binary` matched only the 3.0 copy.
   - **`jackson: v3`.** It produced `tools.jackson.annotation` imports that did not compile.
   - **Multipart tests.** With the 3.0 copy, two tests failed. A request without the optional `prescriberCrm` got 400 (`MissingServletRequestParameterException`), and a 121-character `patientName` got 201.
4. **Code-first Gradle project: springdoc-openapi 3.1.1** (`springdoc-openapi-starter-webmvc-api`) with hand-written controller and records, `springdoc.writer-with-order-by-keys=true`.
   - **First spec.** The first generated spec documented the multipart text fields as query parameters, had no `required` lists on records, and gave `ProblemDetail` a `properties` member.
   - **Fixes.** `@RequestPart` text fields, `@NotNull` components and a fixed `servers` entry corrected it.
   - **Snapshot test.** A `@SpringBootTest` snapshot test failed after the controller changed and passed after regeneration.
   - **Parts binding.** Two MockMvc tests with `MockPart` text parts (no content type) confirmed binding to `@RequestPart String` and `@Size` enforcement (201, then 400).
   - **Test-only scope.** Moving springdoc to `testImplementation` left only `swagger-annotations-jakarta` 2.2.55 on the runtime classpath, and the tests still passed.
5. **TypeScript generators.**
   - **Versions.** openapi-typescript 7.13.0 with openapi-fetch 0.17.0 and openapi-react-query 0.5.4; @hey-api/openapi-ts 0.99.0 and `0.0.0-next-20260930190945`; Orval 8.43.0. zod 4.6.7, @tanstack/react-query 5.104.5, react 19.3.1, TypeScript 6.0.3 and 7.0.2, tsx 4.23.15 for runtime checks against a fake `fetch`.
   - **What each run did.** Each tool generated from all three spec copies, plus a 3.2 copy with one `query` operation. A strict-mode `usage.ts` and a `negative.ts` (`@ts-expect-error` lines) were type-checked under both TypeScript versions.
   - **TypeScript 7 crashes.** openapi-typescript and hey-api 0.99.0 crashed on TypeScript 7 and ran with 6.0.3.
   - **Minified size.** A two-operation client came to 6.8 KB with openapi-fetch, 10.0 KB with hey-api and 0.8 KB with Orval, excluding React, TanStack and zod.
6. **Protobuf and Connect.**
   - **Versions.** Java 25.0.1 (Temurin), buf 1.74.0 (npm), protoc 36.2, grpc-java plugin 1.85.1, protoc-gen-es 2.16.0, connect-es 2.2.0, connect-query 2.3.1, protovalidate-java 1.3.0, protovalidate-es 1.3.0, `@hookform/resolvers` 6.0.0, Armeria 1.42.0, Spring Boot 4.1.1, Envoy 1.40.0 (Docker), Renovate 44.175.0.
   - **Tooling.** A `PrescriptionService` proto with protovalidate rules passed `buf lint`. A modified copy gave six `buf breaking` findings under `WIRE_JSON` and two under `WIRE`. Generation ran offline with local plugins.
   - **TypeScript.** A strict `usage.ts` compiled under TypeScript 5.9.3 and 7.0.2. It used `createConnectTransport`, `findDetails` for `google.rpc.BadRequest` and `buf.validate.Violations`, connect-query hooks, and the Standard Schema resolver. Renaming a proto field broke `tsc`.
   - **Armeria on one port with Spring MVC and the SPA.**
     - Connect-JSON errors lost their details until a custom error handler was added.
     - gRPC-Web worked out of the box.
     - Connect binary got 415, and GET got 405.
     - A 10,000,000-byte upload: JSON got 413 at 10 MiB and succeeded at 16 MiB; binary gRPC-Web succeeded at 10 MiB.
   - **Boot's gRPC server behind Envoy.** gRPC-Web worked, including the upload. Connect JSON failed. Connect binary needed a client shim for error detail type names.
