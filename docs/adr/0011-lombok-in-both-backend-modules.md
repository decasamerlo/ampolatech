# Lombok in both backend modules, at its defaults

The backend's Java uses [Lombok](https://projectlombok.org) in both modules, `core` and `app`. It generates the code a class otherwise writes by hand and extends with each field: the constructor that takes its injected dependencies, a JPA entity's no-argument constructor and getters, a logger. [author-values.md](../conventions/author-values.md) asks for an ADR before adopting a tool that works mainly through lookups a reader can't see, and Lombok is one: which fields a generated constructor takes, and in which order, is the annotation's rule, which no line of the class states.

- **Any Lombok annotation may be used, and review decides case by case.** No convention names an allowed set.
- **Lombok works at compile time only.** Each module that uses it declares it twice, as `compileOnly` and as `annotationProcessor`, with a version of its own in the version catalog: Spring Boot's BOM gives Lombok a version on `app`'s compile classpath and none on its annotation processor path. Neither module's runtime classpath holds Lombok, and the executable jar holds none of its classes. So `core` still depends on no framework when the app runs, as [ADR 0002](0002-stack.md) asks.
- **`core`'s compile classpath holds Lombok, and nothing else.** [ADR 0007](0007-backend-modules.md) gave `core` no library at all, and is amended to say so.
- **Lombok runs at its defaults, with no `lombok.config`.** At its defaults it marks every member it generates `@lombok.Generated`, and the class file keeps the marker.
- **`ArchitectureTest`'s allow-lists name the marker's package**, `"lombok"`, written without `..`. Three lists name it: the one the domain, the ports and the services share, the web adapters' and the persistence adapters'. Any other layer's list gains it in the change whose class first uses Lombok, the way [hexagonal.md](../conventions/hexagonal.md) grows every allow-list.

**What it costs.** A generated constructor never shows in a diff on GitHub, where the author reviews. Its parameters follow the order of the class's fields ([Lombok's rule](https://projectlombok.org/features/constructor)), and a caller that builds the class with `new` binds to that order, as `ApplicationServiceConfiguration` does with `UserApplicationService`. The build also gains a dependency to keep current.

Lombok is in [nesto's backend](https://github.com/decasamerlo/nesto-backend) too, on `core`'s compile classpath, a point where ADR 0007 had departed from nesto's [ADR 014](https://github.com/decasamerlo/nesto/blob/main/docs/adr/014-backend-module-layout.md). [buckpal](https://github.com/thombergs/buckpal), which ADR 0007 cites, declares it `compileOnly` and lets its domain model depend on `lombok..` in its dependency rule.

## Considered options

- **Constructors, getters and loggers written by hand**, as before. Every line shows in a diff, and each new dependency of a class costs it a parameter and an assignment.
- **Lombok in `app` alone.** ADR 0007 would stand as written, and `core`'s application services, whose constructors take their ports, would keep writing them by hand.
- **A `lombok.config` that sets `lombok.addLombokGeneratedAnnotation = false`**, so the allow-lists stay as they were. Gradle doesn't track the file: #34 found that after a change to `lombok.config` alone, `:core:compileJava` stayed `UP-TO-DATE`, and that with the build cache on, a build at Lombok's defaults took a class compiled under the other setting from the cache. `ArchitectureTest` would then pass or fail by which compile it happened to read.
