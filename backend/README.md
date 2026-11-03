# AmpolaTech backend

Java 25 and Spring Boot 4, in a hexagonal layout ([ADR 0002](../docs/adr/0002-stack.md)).

## Set up a machine

Install Docker and [mise](https://mise.jdx.dev), then run `mise install` at the repository root: it installs the Java that `mise.toml` pins. With mise activated in your shell, `./gradlew` finds that Java; otherwise run it as `mise exec -- ./gradlew`.

## Run it locally

From the repository root:

```bash
docker compose up --build
```

It starts PostgreSQL and the backend under the `local` profile, on <http://localhost:8080>, and seeds a demo pharmacy and its sales agent (`db/postgresql/changelog/db.changelog-local.yaml`). `curl localhost:8080/api/me` answers with that agent, whom the demo sign-in signs everyone in as.

The `local` profile is the only one that runs on a machine: the others take the database from the environment, and nothing in them signs anyone in. To run the backend outside Docker, start only the database and run the app under that profile:

```bash
docker compose up -d postgres
cd backend
SPRING_PROFILES_ACTIVE=local ./gradlew :app:bootRun
```

Compose publishes PostgreSQL on `127.0.0.1:5432`, and the local profile connects there. When another database holds that port, set `AMPOLATECH_POSTGRES_PORT` to a free one in your shell: both Compose and the local profile read it.

When a changeset that already ran on your local database changes, Liquibase refuses to start on it; when it moves to another path, Liquibase runs it again, and fails. `docker compose down -v` deletes the database's volume, and the next start creates it afresh.

## Build and test

From `backend/`:

```bash
./gradlew build                                             # every module, every test and the format check
./gradlew spotlessApply                                     # format the Java with google-java-format
./gradlew :app:test --tests 'dev.ampolatech.SignedInUserTest'   # one test class
```

The tests start PostgreSQL with Testcontainers on a random port, so they need Docker running but no free port.

An editor formats the way the build checks when it runs google-java-format's command line, at the version in `gradle/libs.versions.toml` and with its default options.

## Modules

| Module | Holds |
| --- | --- |
| `core` | The domain types, the ports and the application services, in plain Java. `app`'s `ApplicationServiceConfiguration` builds the services as beans. |
| `app` | The Spring Boot entry point and its configuration, the adapters (the controllers, the demo sign-in and the Spring Data JPA repositories), and the Liquibase SQL changelogs. `ArchitectureTest` checks what each layer may use, in both modules. |

[ADR 0007](../docs/adr/0007-backend-modules.md) says why there are two, and [hexagonal.md](../docs/conventions/hexagonal.md) where code goes in each.

## Database roles

`postgres/initdb/create-roles.sh` creates the owner role, which Liquibase connects as, and the runtime role the app connects as, the first time PostgreSQL starts on an empty volume ([ADR 0003](../docs/adr/0003-one-instance-for-all-pharmacies.md)). Its header says what each may do. Docker Compose mounts the script, and the tests copy it into their container, so both run against the same roles.
