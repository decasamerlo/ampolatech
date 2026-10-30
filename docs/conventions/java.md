# Java

The rules for the backend's Java that google-java-format leaves open. No checker enforces them: review does. Comments follow [comments.md](comments.md).

## Layout

google-java-format decides layout, at its defaults. Whitespace, line breaks and the order of imports are whatever it leaves. `./gradlew build` fails on Java it would change, and `./gradlew spotlessApply` formats it.

## Local variables

**Declare the type unless the line already names it.** The author reviews diffs on GitHub, where no IDE says what a `var` stands for. `var` is allowed in two cases:

- **The right-hand side is a constructor call, a cast, or a call to a method named exactly after the type**: a static factory such as `UserId.of(…)`, or `data.pharmacy(…)` returning a `Pharmacy`.
- **The variable is named exactly after the type**, in lower camel case: `user` for a `User`.

```java
var pharmacy = new Pharmacy(PharmacyId.of(UUID.randomUUID()), name); // a constructor
var primavera = data.pharmacy("Farmácia Primavera");                   // pharmacy() returns a Pharmacy
var actor = currentActor.get();                                         // actor holds an Actor
Actor signedIn = currentActor.get();                                    // neither get() nor signedIn names Actor
String[] activeProfiles = environment.getActiveProfiles();              // no name says String[]
```

## Member order

- **A class reads from the top down**: constants, fields, constructors, public methods, and each private helper directly below the first method that calls it. Overloads are never split up.
- **Nested types come last**, read like helpers after the code that uses them. A record keeps a nested type at the bottom of its body even when one of its components has that type.
- **A class reads whole, without section comments**; one long enough to need them is split.

## Methods

- **A method is written in paragraphs**, one per step, with a blank line between them and none inside one. In a test, setup, the call under test and the checks are separate paragraphs.
- **A paragraph that needs a name gets one**: it moves into a private method called by that name, so the caller reads as a list of steps at a single level of detail. There is no line limit; split a method when the result reads better.
- **Guard clauses come first**: the exceptional case returns or throws at the top, so the main path stays unindented.
- **Braces go around every body**, even a single statement, as Google Java Style requires. google-java-format doesn't add missing ones.

## Names and imports

- **Names are whole words**, taken from the glossary or the discipline: `pharmacyId`, never `phId`. A method's name and parameters tell a reader what it does without opening it. Google Java Style covers the rest, from the prefixes it forbids (`mName`) to underscores in test method names.
- **A class is referred to by its simple name, through an import.** When two classes in one file share a simple name, the one the file mentions less is written fully qualified.

## Sources

- [Google Java Style](https://google.github.io/styleguide/javaguide.html): braces (§4.1.1), overloads (§3.4.2.1) and naming (§5). Its §3.4.2 leaves member order to each class; the order above is this repository's.
- OpenJDK's [local variable type inference style guidelines](https://openjdk.org/projects/amber/guides/lvti-style-guide), G3: `var` when the initializer tells the reader the type.
- Robert C. Martin, *Clean Code*, chapters 3 (Functions) and 5 (Formatting): one level of abstraction per method, vertical spacing and top-down order.
