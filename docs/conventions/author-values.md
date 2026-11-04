# The author's values

How an agent weighs one way of doing something against another: a layout, a tool, a library, a place for code, a fix. It applies when an agent makes a choice, recommends one, or reviews a change. The author makes the decisions; this convention says which option an agent puts forward, and when it takes one without asking.

## The values

Each value is a question to ask of every option.

- **Correct over fast.** Which option is right for the pinned versions and the domain, even when another one gets there sooner?
- **Explicit over magic.** Can a reader tell from the code alone what happens, without knowing a framework default, a naming rule or reflection?
- **Principled.** Does the reason hold beyond this case? A documented practice, a published standard or a constraint the code can prove holds; "it works" and "it's quicker" don't.
- **Extra code is no cost** when it buys one of the three above.

## What right looks like

When sources disagree, the higher one wins:

1. **This repository's ADRs and conventions.** Departing from one takes a new ADR or a change to the convention.
2. **A published standard or regulation** for the subject, such as RFC 9457 or RDC 67/2007.
3. **The tool's own documentation for the version the repository pins.** It is usually where the tool writes down its own state of the art.
4. **The state of the art**: the newest approach that does better on the values than the market standard, released as stable (no preview, alpha, beta or incubating release), working with the pinned versions of the rest of the stack, and maintained. Few users is no cost here: one author, a young codebase, nothing to migrate.
5. **The market standard**: what mature projects in the ecosystem do.
6. **A scaffold, a template, a blog post or an answer.** Never a reason on its own.

A departure from 2 to 5 needs a reason that is one of the values. The reason goes in the commit body, and in a comment too when the code would otherwise look wrong ([comments.md](comments.md)).

Two checks come before a reason is used:

- **Check what's current.** An agent's first answer is the market standard as its training data knew it. Before recommending a library, a tool or an approach, read the release pages, the changelogs and the tool's docs for the pinned version, and name the state-of-the-art option whenever one exists, even when recommending another.
- **Check the reason here.** A reason that decides a choice is checked in this repository at the pinned version. One that can't be is written down as its source's claim, with the source.

## What wrong looks like

These are signs, not bans: each one stays when its reason is written beside it.

- **A workaround** makes a symptom go away and leaves its cause: a swallowed exception, a retry or a sleep around a race, a skipped test, a suppressed warning, `--no-verify`, a hard-coded value where data should flow, a flag that bypasses a check.
- **Misplaced code** works, but sits where the layout says it doesn't belong: a business rule in an adapter, a framework type in the domain, build logic outside the build's documented home, a helper copied instead of shared.
- **A misleading name** works, but tells a reader something the code isn't: a package named for a direction its code doesn't take, a driven port the application never calls, a Java type carrying a prefix only its table needs. No compiler or test catches it, and every reader pays for it until it is renamed. A move or a rename leaves one behind most often: whoever moves or renames something checks the packages, names and comments that described it, including the layers above it in a stack.
- **Inherited code** is there only because a scaffold, a template or the code before it did it that way.

## Explicit over magic

- **The standard tool is used with its defaults.** "Explicit" governs this repository's code, not a framework's internals. Adopting a tool that works mainly through lookups a reader can't see takes an ADR.
- **A default the code relies on is written out**, unless it never varies: a reader predicts it every time, and writing it out would change nothing.
- **Once one member of a category needs an explicit value, the whole category is written out**, so a reader never has to tell an override from a derived value. A category is what a reader sees side by side: the fields of one entity, the tables of one schema, the beans of one module.

## When rework decides

The more correct option often costs rework: restacking layers, redoing what a lower layer added, a second PR. Take it and state the cost. Who would pay decides whether the cost counts:

- **The author's own work** never tips the choice, reviewed or not, stacked or not.
- **History on `main`** stays as merged, so there the more correct option becomes a follow-up PR with its own issue.
- **A pharmacy waiting for a fix** gets the narrow fix first, in its own PR, and the correct one follows under an issue. The cost decides the order, never where the code ends up.

Something wrong found outside the task is fixed in the layer that introduced it, when that layer is in the stack being worked on. On `main`, it gets an issue labelled `needs-triage` and a line in the agent's report, and the current PR keeps its scope.

## Choosing or asking

An agent takes the more correct option without stopping when the values settle the choice and its cost falls only on the author. It says so in its report, and the commit body names the alternative it rejected and the cost it took on, as [git.md](git.md) asks. It asks the author first when:

- the cost falls on someone else;
- the options are close on the values;
- the choice is hard to reverse, the kind an ADR records;
- the choice is a name or a shape a client sees and the issue leaves it open, such as a schema in the API's spec ([api.md](api.md)).

## In review

A reviewer weighs its own suggested fixes by this convention. In a diff, it flags these as judgement calls:

- a commit body that gives speed or less rework as the reason for a choice;
- a departure from a tool's documented practice with no reason given;
- a claim that decides a choice and is neither checked here nor attributed to its source.

It reports a misleading name as a defect rather than a judgement call, fixed in the layer that introduced it (*When rework decides*), and looks for one past the diff, as the sign in *What wrong looks like* describes.

## Examples

- **`build-logic` over `buildSrc`.** Gradle 9.8.2's `init` puts convention plugins in `buildSrc`; its [best-practices page](https://docs.gradle.org/9.8.2/userguide/best_practices_structuring_builds.html) prefers an included build named `build-logic`. The documentation outranks the scaffold. Moving an existing plugin means restacking unreviewed layers, a cost only to the author, so it doesn't decide.
- **Every JPA column named.** "A two-word field maps to the wrong column" is false on Spring Boot 4.1.1, whose default naming strategy maps `pharmacyId` to `pharmacy_id`. The reason that holds is renames: a derived name ties the column to the Java field, so renaming the field renames the column.
- **Records.** The language generates a record's accessors, `equals`, `hashCode` and `toString` the same way every time: a default that never varies.
- **TanStack Router over the more widely used React Router** ([ADR 0002](../adr/0002-stack.md)): newer, stable and maintained, with type-checked routes and search params.
- **OpenAPI 3.1, not 3.2.** 3.2 is the newest version of the specification, but the pinned openapi-generator rejects it ([API contract research](../research/api-contract.md)), so it fails the test of working with the pinned versions.
