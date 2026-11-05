# AmpolaTech

## Working mode

AmpolaTech is a product to ship, not a learning project. The agent writes the code; the author reviews it and makes the decisions. nesto's coach mode does not apply here.

**Weighing two ways to do something, drawn to one because it means less rework or a faster result, or moving or renaming code?** The author takes the more correct option and pays for the rework, and counts a misleading name as wrong even when nothing breaks. See `docs/conventions/author-values.md`.

Every change reaches `main` through a pull request, stacked when it depends on another.

## Conventions

- **Creating a branch, a commit or a pull request, or building or merging a stack?** The commit message is the only text a squash merge keeps on `main`. See `docs/conventions/git.md`.
- **Writing or changing a code comment?** A comment outlives the conversation that produced it, and agents read it as instruction. See `docs/conventions/comments.md`.
- **Writing or changing Markdown, the lint's config or its pin?** Run `scripts/lint-docs.sh` before committing; its verdict, the exit code and the last line, is the whole result. See `docs/conventions/markdown.md`.
- **Writing or changing Java?** google-java-format fixes the layout, not when `var` fits, the order of members, the shape of a method, or names and imports. See `docs/conventions/java.md`.
- **Writing or changing `api/openapi.yaml` or the web adapter?** A name or a shape a client sees is the author's to decide when the issue leaves it open. See `docs/conventions/api.md`.
- **Adding or changing a port, an adapter, an application service or a rule in `ArchitectureTest`?** Ports and services live in `core` and adapters in `app`, and `ArchitectureTest` fails a class that uses a package its layer's allow-list doesn't name; a driving port takes the actor as an input. See `docs/conventions/hexagonal.md`.
- **Writing or changing a test, or behaviour that needs one?** Each behaviour is tested at one seam, the domain types, the HTTP API, the app's start or the web app end to end, and the seam decides what the test may use. See `docs/conventions/testing.md`.
- **Adding a table, a constraint or a JPA entity?** PostgreSQL reserves some of the most natural table names, `user` and `order` among them. See `docs/conventions/database-naming.md`.

## Running the code

- **Running the whole app?** Every checkout runs as the same Compose project, `ampolatech`: from a second checkout or a probe, give `docker compose` a `-p` and its own port variables, or it takes over the first one's containers and database. See `README.md`.
- **Building, testing or running the backend?** `./gradlew` runs on the Java `mise.toml` pins, and the tests and the local run need Docker. See `backend/README.md`.
- **Building, checking or running the web app?** `pnpm` runs on the pnpm `mise.toml` pins, and the API's client and the route tree come from `pnpm generate`, never from git. See `web/README.md`.

## References

- **nesto** ([`decasamerlo/nesto`](https://github.com/decasamerlo/nesto), public) is the same author's project and the starting point for this repo's conventions: `docs/conventions/` (ADRs, commits, markdown, test fixtures, domain-layer isolation, database naming), `GLOSSARY.md` for the glossary style; its backend, [`decasamerlo/nesto-backend`](https://github.com/decasamerlo/nesto-backend) (public), for a hexagonal Java layout. Where this repo's own `docs/conventions/` covers the same topic, it wins.
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

**Asked whether the repo uses a tool or a pattern, or recommending one?** The answer may be an issue titled `Decide …`: open while the decision is pending, closed once it is made. Search the issues before the code: `gh issue list --state all --search '<term>'`.

### Triage labels

The five default labels, each named after its role: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
