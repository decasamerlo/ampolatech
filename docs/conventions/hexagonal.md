# Hexagonal architecture

Where the backend's ports live, how they are named, and who implements them. The backend is hexagonal ([ADR 0002](../adr/0002-stack.md)): `domain` and `application` depend on no framework, and adapters reach them only through ports. Beyond what the Gradle modules forbid, no checker enforces these rules: review does.

## Ports

A port is an interface the application owns. Both kinds sit in the `domain` module: a driving port in `port.in`, a driven port in `port.out`. A module of adapters depends on `domain` alone, so the compiler keeps its adapters from reaching an application service, which sits in `application`. Only the composition root, the `app` module, sees every other module, and review holds an adapter placed there to the same rule.

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

- **`{Aggregate}RepositoryPort`** persists an aggregate, such as `UserRepositoryPort`. Every method takes the `PharmacyId` it works within (ADR 0003), and its Javadoc says so.
- **`{Capability}Port`** names any other need by what it provides.

### `find`, `get` and `list`

A method's prefix says what absence means, in driving and driven ports alike:

- **`find…` returns an `Optional`**: absence is an answer the caller handles, such as a user id that `UserRepositoryPort.findById` doesn't find in the pharmacy.
- **`get…` returns the value or throws**: absence breaks an invariant. `CurrentActor.get()` throws with nobody signed in, and `getSignedInUser` throws for an actor whose user or pharmacy doesn't exist.
- **`list…` returns a `List`, empty when nothing matches**: absence is an empty answer. A method that returns several values takes this prefix; `find…` and `get…` each return one.

### Who implements a driving port

**The application service of the operation's aggregate**, named `{Aggregate}ApplicationService`, in the `application` module: `UserApplicationService` implements `GetSignedInUserQuery`. One service holds every operation of its aggregate, and each caller sees only the port it uses. Like the rest of `application`, it depends on no framework, so it carries no annotation and the composition root builds it.

## Sources

- Alistair Cockburn, [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/): the application talks to the outside only through ports, which adapters call or implement.
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture*, and its example code, [buckpal](https://github.com/thombergs/buckpal): one interface per operation in `port.in`, taking a command for a write and a query for a read. buckpal has a service per operation; this repository keeps one per aggregate.
- [nesto#19](https://github.com/decasamerlo/nesto/issues/19), which proposes the same pairing for nesto: an application service per aggregate, with an interface per operation.
- Martin Fowler, [CommandQuerySeparation](https://martinfowler.com/bliki/CommandQuerySeparation.html): why a read and a write are named apart.
- Joshua Bloch, *Effective Java*, 3rd edition, item 54, "Return empty collections or arrays, not nulls": why a `list…` answers absence with an empty `List`.
