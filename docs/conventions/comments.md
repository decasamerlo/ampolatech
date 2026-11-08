# Code comments

What a comment in this repository may say. It applies to every comment in the code the repository writes, Javadoc included, whatever the language; generated files such as the Gradle wrapper keep the comments they came with. It covers content, not layout.

## The test

A comment is read in a **fresh checkout**: the repository at that commit and nothing else, years after the conversation, issue thread or pull request that produced it. Before writing one, ask:

1. **Does the code already say it?** Names, types and tests carry what the code does. A comment earns its place with what they can't carry, most often the reason.
2. **Will it still be true and clear in a fresh checkout?** If it needs the conversation to make sense, or can turn false without anyone editing it, it stays out.

Comments go on the code a change touches. A comment there that the change makes false is corrected or removed in the same change.

Before the change's review, run the writing-for-agents skill over each comment the change writes or edits, and over every other place that states the same facts. Where the skill and this convention differ, this convention wins.

## Worth a comment

- **The reason behind a public type or method**, in its Javadoc, when the signature leaves it out: what it's for, what a caller must ensure first, what it promises. When the name and types already say all of that, it gets no Javadoc.
- **The business or regulatory reason** for a rule, in the glossary's words.
- **An invariant** the types can't express, and what keeps it true.
- **A choice that looks wrong but isn't**, or the alternative that was tried and dropped, so the next reader keeps the choice.
- **Something left out on purpose**, such as an empty `catch`.
- **A `TODO` with its issue**, `TODO(#12): …`, so it can be found and closed.

## Left out

A comment describes the code as it stands, unconditionally. Each of these fails the test's second question:

- **How the change came about**: who asked for it, what was agreed, or a retelling of the diff.
- **Plans**: "for now", "until sign-in exists", "in a later ticket".
- **Time and history**: "now", "currently", "recently", what the code used to do, old code kept in a comment, debug prints.
- **References that won't last**: a path or symbol missing from a fresh checkout, or a link to a chat, a draft or anything else that can move or disappear.
- **Words of its own**: the vocabulary is the code's, `GLOSSARY.md`'s and the discipline's.

References that last are welcome where they help: an ADR, a convention in `docs/conventions/`, a published standard or regulation, the issue in a `TODO`, or, better than any of them, the constraint itself written out.

## Inside the hexagon

The domain, the ports and the application services, in the `core` module, depend on no framework, database or transport ([ADR 0002](../adr/0002-stack.md), [ADR 0007](../adr/0007-backend-modules.md)), and their comments match: they use the glossary's language and name no framework, database, protocol or adapter. A port says what it guarantees; how an adapter meets the guarantee is the adapter's to explain. An adapter's comments may name its own technology and the domain types it converts to and from.
