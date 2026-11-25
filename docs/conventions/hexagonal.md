# Hexagonal architecture

Where the backend's code lives, how its aggregates and ports are written, who implements the ports, and how `app` builds what it runs. The backend is hexagonal ([ADR 0002](../adr/0002-stack.md)): the domain, the ports and the application services depend on no framework, and adapters reach them only through ports. The compiler and `ArchitectureTest` enforce where code lives and what it may use ([ADR 0007](../adr/0007-backend-modules.md)), and `ArchitectureTest` that a port is an interface; review enforces the rest, the names among them.

## Modules and packages

| Module | Packages, under `dev.ampolatech` | Depends on |
| --- | --- | --- |
| `core` | `domain`, `port.in`, `port.out`, `application` | No module, and one library, Lombok, at compile time only ([ADR 0011](../adr/0011-lombok-in-both-backend-modules.md)) |
| `app` | the composition root, in the package `dev.ampolatech` itself, and the adapters, in `adapter..` | `core` and the libraries the adapters use |

- **An adapter's package is `adapter.{in,out}.{category}.{implementation}`**: its direction, what it does, and which one it is, such as `adapter.in.web.rest`, `adapter.in.signin.demo` and `adapter.out.persistence.postgresql`. The implementation is written even when it is the only one, so a second never moves the first.
- **The Liquibase changelogs live in `app`'s resources, under `db/postgresql/changelog/`.** The path names the database, not an adapter, because every adapter that uses PostgreSQL shares its schema.
- **An adapter reaches the application through a port alone.** Only the composition root reaches a service or an adapter.
- **An adapter turns an aggregate into its own model, or back, in that aggregate's mapper beside it, `{Aggregate}{Category}Mapper`**: `UserPersistenceMapper` turns a `UserEntity` into a `User`, and `PharmacyWebMapper` a `Pharmacy` into a `PharmacyDto`. The mapper is a `@Component` the adapter takes in its constructor, and one whose model holds another aggregate's takes that aggregate's mapper, as `UserWebMapper` takes `PharmacyWebMapper`. Each adapter's conversions then sit in the same kind of place, and a second class that needs one calls it rather than copying it.

### Writing `ArchitectureTest`'s rules

- **A new adapter category gets its own layer and allow-list in `ArchitectureTest`**, in the change that adds it. Every class must sit in a layer, so the test fails until then.
- **An allow-list names the packages its classes use, one by one, and grows a package at a time**, in the change that needs it, where review sees it, never by a root such as `org.springframework..`. Only the repository's own packages and the JDK's `java.lang`, `java.util`, `java.time` and `java.math` take the `..` that covers the packages below them: a root lets in more than one technology, as `java..` holds `java.sql` and `org.springframework.boot..` holds Boot's JDBC and web support alike.
- **`scripts/architecture-violations.sh` reads a failed run's report**: for each rule that failed, every package its violations reach, with the classes that reach it, one line per package, so a package an allow-list lacks reads at a glance.
- **The domain, the ports and the services share one allow-list**, of `core`'s packages, those four of the JDK and `lombok` (ADR 0011). Every module compiles against the whole JDK, so it is this list, not `core`'s classpath, that stops them from using `java.sql` or `java.net.http`.
- **A new or changed rule goes red before it lands**, since a rule passes on clean code whether or not it catches anything. An allow-list growing by a package is left to review, as above.

  1. In a production package the rule covers, add a class that breaks what the change adds; `ArchitectureTest` leaves the test sources out.
  2. In `backend/`, run `./gradlew :app:test --tests dev.ampolatech.ArchitectureTest`: the rule fails, naming the class, and any other rule that fails names the same dependency.
  3. Delete the class, and show the run in the PR's Evidence.

  One line per rule, with the break that turns it red; a new rule adds its own, and a changed rule updates it:

  - the layers: a domain type holding a driven port;
  - the allow-list of the domain, the ports and the services: a domain type holding a `java.sql.Connection`;
  - the ports: a driven port written as an abstract class;
  - the web adapters' allow-list: a web adapter holding a `java.sql.Connection`;
  - the sign-in adapters' allow-list: a sign-in adapter holding Boot's `DataSourceBuilder`;
  - the persistence adapters' allow-list: a persistence adapter holding an `HttpServletRequest`.

## The domain

How a type in `core`'s `domain` package is written.

- **An aggregate is a record**, as `User` and `Pharmacy` are.
- **Its compact constructor holds only what is true of every instance, stored ones included**: a component is present, a name isn't blank.
- **A rule that holds only when an aggregate is registered goes in a static `create(…)` on the record.** Registering is the operation that first creates an aggregate, as #109 registers a prescription, and #8's limit on a prescription file's size is such a rule. The application service calls `create(…)`, and the persistence mapper rebuilds a stored aggregate through the canonical constructor, so a registration rule tightened later leaves stored rows loadable.
- **Only review keeps a caller on `create(…)`**, since a record's canonical constructor can't be private: javac refuses it, "attempting to assign stronger access privileges; was public".
- **A value object is a record that checks in its compact constructor the rules true of every instance.** A rule that holds only when its aggregate is registered, such as #8's limit on a prescription file's size, goes in the aggregate's `create(…)` all the same.
- **An aggregate's id is `{Aggregate}Id`**, a record over a `UUID` with a static `of(UUID)`, as `UserId` and `PharmacyId` are.
- **An aggregate refers to another by the other's `{Aggregate}Id`**, as `User` holds a `PharmacyId`, and the persistence adapter's JPA entity maps the reference as a plain `@Column` over the `UUID`, as `UserEntity` maps `pharmacy_id`.

**The first aggregate that changes after it is registered becomes a class**, most likely a user or a pharmacy being renamed: a private constructor, `create(…)` for a new one and `reconstitute(…)` for a stored one. Entities and domain services wait for that aggregate, or for the first aggregate that holds an entity.

## Ports

A port is an interface the application owns, in `core`: a driving port in `port.in`, a driven port in `port.out`. Every top-level type in `port..` is an interface, which `ArchitectureTest` checks; a port's `Command` and `Result` nest inside it. A driving adapter calls a driving port, and a driven adapter implements a driven port, so a request reaches the database or another system only through the application. A driving adapter's allow-list names the driving ports alone.

### Driving ports

What an adapter may ask of the application, one interface per operation.

- **A read is a `…Query`**, such as `GetSignedInUserQuery`. Its method takes its inputs as parameters and returns a domain type or a nested `Result` record.
- **A write is a `…UseCase`.** Its method takes a nested `Command` record that carries every input and returns nothing or a nested `Result`. The `Command` rejects a missing or malformed input in its compact constructor. Reads take no `Command` because the word means a change: a read that took one would claim to change something.
- **The method is named after the operation**: `getSignedInUser`, never `execute`. One application service implements several ports, and two methods with the same name and parameters would clash.
- **A `Result` carries data and no rule.** Rules stay in the domain types it holds.
- **A nested type keeps its port's name** outside the port: `GetSignedInUserQuery.Result`. Every port has its own `Command` or `Result`, so in a service that implements two, a bare `Result` would be ambiguous.
- **The actor is an input.** The adapter takes it from `CurrentActor` and passes it in, as a parameter of a read or inside the `Command` of a write, and the operation acts for the actor's pharmacy ([ADR 0003](../adr/0003-one-instance-for-all-pharmacies.md)). No application code asks for `CurrentActor`, which is why it sits beside `Actor` in the domain rather than among the driven ports.

### Driven ports

What the application needs from outside, each implemented by an adapter.

- **`{Aggregate}RepositoryPort`** persists an aggregate, such as `UserRepositoryPort`. Every method takes the `PharmacyId` it works within as its last parameter (ADR 0003), and its Javadoc says so. What the method acts on comes first and the pharmacy that scopes it after, `findById(UserId id, PharmacyId pharmacyId)`, so the adapter passes them in the same order to the Spring Data method whose name lists them, `findByIdAndPharmacyId`.
- **`{Capability}Port`** names any other need by what it provides.

### `find`, `get` and `list`

A method's prefix says what absence means, in driving and driven ports alike:

- **`find…` returns an `Optional`**: absence is an answer the caller handles, such as a user id that `UserRepositoryPort.findById` doesn't find in the pharmacy.
- **`get…` returns the value or throws**: absence breaks an invariant. `CurrentActor.get()` throws with nobody signed in, and `getSignedInUser` throws for an actor whose user or pharmacy doesn't exist.
- **`list…` returns a `List`, empty when nothing matches**: absence is an empty answer. A method that returns several values takes this prefix; `find…` and `get…` each return one.

### Who implements a driving port

**The application service of the operation's aggregate**, named `{Aggregate}ApplicationService`, in `core`'s `application` package: `UserApplicationService` implements `GetSignedInUserQuery`. One service holds every operation of its aggregate, and each caller sees only the port it uses. Like the rest of `core`, it depends on no framework, so it carries no framework's annotation and the composition root builds it.

**An application service only coordinates**: it reads through a port, calls the domain, and writes through another port. Where the permission check runs is #116's to decide.

**A read that needs more than one aggregate reads each through that aggregate's own repository port, and the application service combines them.** It reads each aggregate once for the whole answer, never once per item: `getSignedInUser` reads the actor's user through `UserRepositoryPort` and its pharmacy through `PharmacyRepositoryPort`, and #6's list of orders reads the orders, then, in one call, the users who registered them. Each `{Aggregate}RepositoryPort` then still persists one aggregate, at a cost of a statement or more per aggregate where a join runs one, and Vernon takes the same default until query overhead becomes a problem (*Sources*).

**A join waits for its trigger**: the first read that must sort, filter or page on another aggregate's column, or whose measured cost is a problem. #106's closing comment records what was found for a join.

**An operation writes through one driven port.** Its write then needs no transaction beyond the one its adapter runs, and no application service opens one. An operation that would write through two needs a decision first on where its transaction begins, since an application service can't carry Spring's `@Transactional` ([ADR 0007](../adr/0007-backend-modules.md)); #105's closing comment records the options weighed.

**A service that needs the time takes a `java.time.Clock` in its constructor**, reads `clock.instant()` once per operation and passes that instant to the domain, so every instant one operation records is the same. The composition root provides one `Clock` bean, `Clock.systemUTC()`, from the change that brings the first service that takes it.

**A service that creates an aggregate takes a `Supplier<{Aggregate}Id>` in its constructor**, so how an id is made is chosen in the composition root, not in each service. It calls `get()` once per new aggregate and passes the id to `create(…)`. The composition root passes the supplier as a lambda in that service's `@Bean` method, such as `() -> PrescriptionId.of(UUID.randomUUID())`, not as a bean of its own, so no test sets the id a service gives a new aggregate: a test that needs it reads it from the response.

**A test at the HTTP API that checks an instant puts a fixed `Clock` in place of the composition root's bean**, since that is the seam a service is tested at.

## The composition root

How `app` builds what it runs, from `AmpolaTechApplication` and the other classes in the package `dev.ampolatech`.

- **Component scanning finds the adapters and the mappers**, by the stereotype each carries, such as `@RestController` or `@Component`, so every adapter class stays package-private: a `@Bean` method in `dev.ampolatech` couldn't call its constructor.
- **`ApplicationServiceConfiguration` builds the application services**, with `new` in `@Bean` methods, since a service carries no framework's annotation for scanning to find.
- **An adapter that configuration turns on gets a `@Configuration` in its own package**, as `DemoSignInConfiguration` in `adapter.in.signin.demo` builds the demo sign-in's `CurrentActor` only when `ampolatech.sign-in.demo.enabled` is true.
- **A persistence adapter is a `@Component`, not a `@Repository`**, so a port's caller receives the exception the adapter threw. The exception translation `@Repository` turns on would rewrap an `IllegalStateException` or an `IllegalArgumentException` thrown in the adapter, by its mapper or a domain type's constructor, as Spring's `InvalidDataAccessApiUsageException`, a type `core` can't name. The Spring Data repository behind the adapter keeps its own translation.

## Sources

- Alistair Cockburn, [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/): the application talks to the outside only through ports, which adapters call or implement.
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, and its example code, [buckpal](https://github.com/thombergs/buckpal): one interface per operation in `port.in`, taking a command for a write and a query for a read. buckpal has a service per operation; this repository keeps one per aggregate.
- Vaughn Vernon, *Implementing Domain-Driven Design*, chapter 10, "Aggregates", and chapter 14, "Application". Chapter 10 has an aggregate refer to another by identity (p. 361) and fills a view from several repositories: "You may have to use multiple Repositories in a single use case to populate views. If query overhead causes performance issues, it may be worth considering the use of theta joins or CQRS." (p. 363)
- nesto's [ADR 007](https://github.com/decasamerlo/nesto/blob/main/docs/adr/007-immutable-node-entity.md): an aggregate that changes, written as a class with a private constructor, `create()` for a new one and `reconstitute()` for a stored one.
- nesto's [ADR 008](https://github.com/decasamerlo/nesto/blob/main/docs/adr/008-application-layer.md): "An application service reads through a port, calls the domain, and writes through a port"; it takes a `Clock` in its constructor and reads `clock.instant()` once per operation; "Adapters stay annotated and component-scanned."
- nesto's [ADR 014](https://github.com/decasamerlo/nesto/blob/main/docs/adr/014-backend-module-layout.md): the same two modules, the adapter package format, and a composition root whose `ApplicationServiceConfiguration` builds the application services, while "Configuration that belongs to one adapter stays in that adapter's package."
- nesto's backend, [`NodePostgresqlRepository`](https://github.com/decasamerlo/nesto-backend/blob/af8b5b95f542dcc72453f3eec61a2f373240c25f/app/src/main/java/dev/nesto/adapter/out/persistence/postgresql/NodePostgresqlRepository.java): a persistence adapter that is a `@Component` and not a `@Repository`, for the same reason.
- Spring Boot's reference, [Spring Beans and Dependency Injection](https://docs.spring.io/spring-boot/4.1/reference/using/spring-beans-and-dependency-injection.html): "We generally recommend using constructor injection to wire up dependencies and @ComponentScan to find beans."
- [nesto#19](https://github.com/decasamerlo/nesto/issues/19), which proposes the same pairing for nesto: an application service per aggregate, with an interface per operation.
- Martin Fowler, [CommandQuerySeparation](https://martinfowler.com/bliki/CommandQuerySeparation.html): why a read and a write are named apart.
- Joshua Bloch, *Effective Java*, 3rd edition, item 54, "Return empty collections or arrays, not nulls": why a `list…` answers absence with an empty `List`.
