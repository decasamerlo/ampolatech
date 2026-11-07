# Hexagonal architecture

Where the backend's code lives, how its ports are named, and who implements them. The backend is hexagonal ([ADR 0002](../adr/0002-stack.md)): the domain, the ports and the application services depend on no framework, and adapters reach them only through ports. The compiler and `ArchitectureTest` enforce where code lives and what it may use ([ADR 0007](../adr/0007-backend-modules.md)), and `ArchitectureTest` that a port is an interface; review enforces the rest, the names among them.

## Modules and packages

| Module | Packages, under `dev.ampolatech` | Depends on |
| --- | --- | --- |
| `core` | `domain`, `port.in`, `port.out`, `application` | No module and no library |
| `app` | the composition root, in the package `dev.ampolatech` itself, and the adapters, in `adapter..` | `core` and the libraries the adapters use |

- **An adapter's package is `adapter.{in,out}.{category}.{implementation}`**: its direction, what it does, and which one it is, such as `adapter.in.web.rest`, `adapter.in.signin.demo` and `adapter.out.persistence.postgresql`. The implementation is written even when it is the only one, so a second never moves the first.
- **The Liquibase changelogs live in `app`'s resources, under `db/postgresql/changelog/`.** The path names the database, not an adapter, because every adapter that uses PostgreSQL shares its schema.
- **An adapter reaches the application through a port alone.** Only the composition root, which builds the application services, reaches a service or an adapter.
- **An adapter turns an aggregate into its own model, or back, in that aggregate's mapper beside it, `{Aggregate}{Category}Mapper`**: `UserPersistenceMapper` turns a `UserEntity` into a `User`, and `PharmacyWebMapper` a `Pharmacy` into a `PharmacyDto`. The mapper is a `@Component` the adapter takes in its constructor, and one whose model holds another aggregate's takes that aggregate's mapper, as `UserWebMapper` takes `PharmacyWebMapper`. Each adapter's conversions then sit in the same kind of place, and a second class that needs one calls it rather than copying it.

### Writing `ArchitectureTest`'s rules

- **A new adapter category gets its own layer and allow-list in `ArchitectureTest`**, in the change that adds it. Every class must sit in a layer, so the test fails until then.
- **An allow-list names the packages its classes use, one by one, and grows a package at a time**, in the change that needs it, where review sees it, never by a root such as `org.springframework..`. Only the repository's own packages and the JDK's `java.lang`, `java.util`, `java.time` and `java.math` take the `..` that covers the packages below them: a root lets in more than one technology, as `java..` holds `java.sql` and `org.springframework.boot..` holds Boot's JDBC and web support alike.
- **`scripts/architecture-violations.sh` reads a failed run's report**: for each rule that failed, every package its violations reach, with the classes that reach it, one line per package, so a package an allow-list lacks reads at a glance.
- **The domain, the ports and the services share one allow-list**, of `core`'s packages and those four of the JDK. Every module compiles against the whole JDK, so it is this list, not `core`'s classpath, that stops them from using `java.sql` or `java.net.http`.
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

**The application service of the operation's aggregate**, named `{Aggregate}ApplicationService`, in `core`'s `application` package: `UserApplicationService` implements `GetSignedInUserQuery`. One service holds every operation of its aggregate, and each caller sees only the port it uses. Like the rest of `core`, it depends on no framework, so it carries no annotation and the composition root builds it.

**An operation writes through one driven port.** Its write then needs no transaction beyond the one its adapter runs, and no application service opens one. An operation that would write through two needs a decision first on where its transaction begins, since an application service can't carry Spring's `@Transactional` ([ADR 0007](../adr/0007-backend-modules.md)); #105 weighs the options.

## Sources

- Alistair Cockburn, [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/): the application talks to the outside only through ports, which adapters call or implement.
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, and its example code, [buckpal](https://github.com/thombergs/buckpal): one interface per operation in `port.in`, taking a command for a write and a query for a read. buckpal has a service per operation; this repository keeps one per aggregate.
- nesto's [ADR 014](https://github.com/decasamerlo/nesto/blob/main/docs/adr/014-backend-module-layout.md): the same two modules, and the adapter package format.
- [nesto#19](https://github.com/decasamerlo/nesto/issues/19), which proposes the same pairing for nesto: an application service per aggregate, with an interface per operation.
- Martin Fowler, [CommandQuerySeparation](https://martinfowler.com/bliki/CommandQuerySeparation.html): why a read and a write are named apart.
- Joshua Bloch, *Effective Java*, 3rd edition, item 54, "Return empty collections or arrays, not nulls": why a `list…` answers absence with an empty `List`.
