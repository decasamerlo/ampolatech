# AmpolaTech

## Working mode

AmpolaTech is a product to ship, not a learning project. The agent writes the code; the author reviews it and makes the decisions. nesto's coach mode does not apply here.

**Weighing two ways to do something, drawn to one because it means less rework or a faster result, or moving or renaming code?** The author takes the more correct option and pays for the rework, and counts a misleading name as wrong even when nothing breaks. See `docs/conventions/author-values.md`.

Every change reaches `main` through a pull request, stacked when it depends on another.

## Conventions

- **Creating a branch, a commit or a pull request, or building or merging a stack?** The commit message is the only text a squash merge keeps on `main`. See `docs/conventions/git.md`.
- **Writing or changing a code comment?** A comment outlives the conversation that produced it, and agents read it as instruction. See `docs/conventions/comments.md`.
- **Writing or changing Java?** google-java-format fixes the layout, not when `var` fits, the order of members, the shape of a method, or names and imports. See `docs/conventions/java.md`.
- **Adding or changing a port, an adapter or an application service?** Every port lives in the `domain` module, and a driving port takes the actor as an input. See `docs/conventions/hexagonal.md`.
- **Writing or changing a test, or behaviour that needs one?** Each behaviour is tested at one seam, the domain types, the HTTP API or the web app end to end, and the seam decides what the test may use. See `docs/conventions/testing.md`.
- **Adding a table, a constraint or a JPA entity?** PostgreSQL reserves some of the most natural table names, `user` and `order` among them. See `docs/conventions/database-naming.md`.

## References

- **nesto** ([`decasamerlo/nesto`](https://github.com/decasamerlo/nesto), public) is the same author's project and the starting point for this repo's conventions: `docs/conventions/` (ADRs, commits, markdown, test fixtures, domain-layer isolation, database naming), `CONTEXT.md` for the glossary style, and `backend/` for a hexagonal Java layout. Where this repo's own `docs/conventions/` covers the same topic, it wins.
- **Other codebases the author has access to** are listed in `CLAUDE.local.md`, which is not committed. They are not public, so *Non-public sources* below applies to anything drawn from them.
- **Pharmacy software and standards** worth reading are surveyed in `docs/research/reference-software.md`.

## Non-public sources

AmpolaTech will be public; some codebases it draws precedent from are not. Adapted from nesto's [non-public-sources convention](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/non-public-sources.md), which gives the reasoning.

- **Verify before naming.** Any repository named in published text must be public: `gh repo view <owner>/<repo> --json isPrivate`, reject on `true`. Read the field: an agent runs authenticated as the author, so a lookup of a private repository succeeds like any other.
- **Cite by shape, never by measurement.** Never publish identifiers, verbatim quotation, or counts of implementations, services, or references taken from a non-public codebase. State precedent as direction only: "the dominant approach", "a minority pattern".
- **Surfaces:** commit messages, files, issue bodies and comments, PR bodies and reviews.
- **Fires twice:** when drafting any of these, and as a checklist item in every review pass, including over text a human wrote.
- **On a trip:** write the generic form and say that you did. Don't stall for a decision, and don't substitute silently.

## Agent skills

### Issue tracker

Issues live in GitHub Issues on `decasamerlo/ampolatech`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default labels, each named after its role: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
