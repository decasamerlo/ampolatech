# AmpolaTech backend

Java 25 and Spring Boot 4, in a hexagonal layout ([ADR 0002](../docs/adr/0002-stack.md)).

## Set up a machine

Install Docker and [mise](https://mise.jdx.dev), then run `mise install` at the repository root: it installs the Java that `mise.toml` pins. With mise activated in your shell, `./gradlew` finds that Java; otherwise run it as `mise exec -- ./gradlew`.

## Run it locally

The whole app, the backend among it, runs from the repository root ([README.md](../README.md)). The demo pharmacy and sales agent the `local` profile seeds come from `db/postgresql/changelog/db.changelog-local.yaml`.

The `local` profile is the only one that runs on a machine: the others take the database from the environment, and nothing in them signs anyone in. To run the backend outside Docker, start only the database and run the app under that profile:

```bash
docker compose up -d postgres
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew :app:bootRun
```

The local profile connects to PostgreSQL on the port Compose publishes it on, since both read `AMPOLATECH_POSTGRES_PORT`.

When a changeset that already ran on your local database changes, Liquibase refuses to start on it; when it moves to another path, Liquibase runs it again, and fails. Start again from an empty database, as [README.md](../README.md) says.

## Build and test

From `backend/`:

```bash
./gradlew build                                             # every module, every test and the format check
./gradlew spotlessApply                                     # format the Java with google-java-format
./gradlew :app:test --tests 'dev.ampolatech.SignedInUserTest'   # one test class
```

The tests start PostgreSQL with Testcontainers on a random port, so they need Docker running but no free port.

The build generates the web adapter's interfaces and models from the API's spec, `api/openapi.yaml` ([ADR 0006](../docs/adr/0006-openapi-contract-first.md)), into `app/build/generated/openapi/`. None of it is committed.

An editor formats the way the build checks when it runs google-java-format's command line, at the version in `gradle/libs.versions.toml` and with its default options.

## Modules

| Module | Holds |
| --- | --- |
| `core` | The domain types, the ports and the application services, in plain Java. `app`'s `ApplicationServiceConfiguration` builds the services as beans. |
| `app` | The Spring Boot entry point and its configuration, the adapters (the controllers, the demo sign-in and the Spring Data JPA repositories), and the Liquibase SQL changelogs. `ArchitectureTest` checks what each layer may use, in both modules. |

[ADR 0007](../docs/adr/0007-backend-modules.md) says why there are two, and [hexagonal.md](../docs/conventions/hexagonal.md) where code goes in each.

## Database roles

`postgres/initdb/create-roles.sh` creates the owner role, which Liquibase connects as, and the runtime role the app connects as, the first time PostgreSQL starts on an empty volume ([ADR 0003](../docs/adr/0003-one-instance-for-all-pharmacies.md)). Its header says what each may do. Docker Compose mounts the script, and the tests copy it into their container, so both run against the same roles.
