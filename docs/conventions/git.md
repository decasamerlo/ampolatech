# Git

How changes reach `main`: branch names, commits, pull requests, and GitHub's native stacks (`gh stack`) for changes that depend on each other. The stacking workflow is adapted from nesto's [stacked-PRs workflow](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/stacked-prs.md), which also covers the co-developers and forks this repo doesn't have.

## Rules

1. **Every change reaches `main` through a pull request.**
2. **One PR, one commit.** Amend instead of adding a commit; squash merge makes that commit the one on `main`.
3. **A PR changes up to ~400 lines.** A bigger one says why in its Summary, such as generated code or a research note read as a whole; anything else is split into smaller PRs, stacked where one depends on another.
4. **Only the author merges.** An agent merges only when asked to in the session.
5. **An agent pushes its work without asking.** Once its review is clean, an agent that implements an issue pushes its branch and opens its PR, or submits its stack, and reports each PR's link. An amend to an open PR, one that answers a review included, goes up the same way once its own review is clean: `git push --force-with-lease --force-if-includes` for a lone PR, `gh stack submit` for a stack. The PR is where the author reviews the change, and the merge stays theirs (rule 4). Each push goes through `.githooks/pre-push`: skipping it with `--no-verify` is the author's call (*Protecting `main`*).

## Vocabulary

**Layer**: one PR of a stack, one minimal reviewable change, one commit.

**Stack**: a chain of layers, each depending on the one below. The bottom layer targets `main`, and each layer above targets the one below it. Changes that don't depend on each other are best kept as lone PRs, so each can merge on its own.

**Trunk**: `main`, squash-only.

## Branch names

`<type>/<issue-number>-<slug>`: `feat/3-backend-persistence`, `docs/13-git-conventions`. The types are the commit types below. Every branch names the issue it works on, so a change with no issue gets one first.

## Commits

The commit is the permanent record. Squash merge takes the subject and the body from the commit, so the PR body never reaches `main`.

```text
<type>(<scope>): <summary in the imperative, lower case, no period, at most 72 characters>

<Why the change was needed and why it takes this shape, in prose
wrapped at 72 columns. Name the alternative you rejected and the cost
you took on. If users or callers will notice a difference, describe it
in a paragraph of its own.>

Refs: #N
Co-Authored-By: <the agent, when one wrote the change>
```

- **Types**: `feat`, `fix`, `docs`, `chore`, `build`. **The scope** is the top-level folder the change lands in (`backend`, `web`) or the docs folder (`adr`, `conventions`), left out when the change has no single home. `CLAUDE.md` points into every folder, so updating its pointer keeps the scope of the change it goes with.
- **The body is required for `feat` and `fix`**, and recommended for the other types when the why isn't obvious. It takes 1 to 3 paragraphs, about 12 lines; a longer why means two concerns, which split into two PRs, or a decision that belongs in an ADR.
- **The body is prose**, read in `git log` in an 80-column terminal: no headings, lists, tables or file lists.
- **The trailers** close the message: `Refs: #N` for the branch's issue, and `Co-Authored-By:` when an agent wrote the change. A commit never carries a closing keyword: closing is the PR body's job.
- **Every reference must last**: an ADR, a convention, an issue, code in the repository, or the constraint itself written out.
- **`scripts/check-commits.sh <commit>...` checks the rules above that concern form**: the subject's shape and length, a scope against the changed files, a body for `feat` and `fix`, the prose's wrap (a line that is only a URL may run longer), no list or table in the body, no line starting with `#`, which git drops as a comment when the message is edited, the `Refs:` trailer and no closing keyword. A body over 15 lines of text gets a warning, not a failure. Run it on a draft before committing it, `scripts/check-commits.sh --message <file>`, which checks all but the scope, and on each new commit, `scripts/check-commits.sh HEAD`, before building on it. `.githooks/pre-push` runs it on each commit a push sends, and review checks the rest, a missing scope among them.

## PR title and body

- **The title is the commit subject.**
- **The body follows [the PR template](../../.github/pull_request_template.md)**, in English, written for the reviewer. It says what the diff and the commit body don't, and its Evidence is what was run and seen, never checkboxes. AI attribution goes only in the commit's trailer; `.claude/settings.json` turns off the line Claude Code would add to a PR body.
- **The issue line** is `Closes #N.` on the PR that finishes the issue, and `Refs #N.` on one that contributes to it without finishing it. When a stack merges, each layer's `Closes #N.` closes its issue, the upper layers' included, although only the bottom layer is based on `main`.

## Building a stack

```bash
gh stack init feat/12-first-layer      # a new branch off main, tracked as a stack
git commit                             # the layer's one commit
gh stack add feat/12-second-layer      # from the top layer: a new branch on top
git commit
gh stack rebase                        # onto the current origin/main, before the first submit
gh stack submit --auto                 # push, and open one PR per layer
```

- **Name the branch** in `gh stack add`: without a name it prompts, and outside a terminal it fails.
- **`gh stack submit --auto` opens new PRs as drafts**, titled with the commit subject. Set each body with `gh pr edit <n> --body-file <file>`, then `gh pr ready <n>`.
- **Move a stack branch only with `gh stack`** (`rebase`, `sync`). Each branch records its parent's tip, so a branch moved by `git rebase` or `git reset --hard` restacks into conflicts. After amending a lower layer, `gh stack rebase --no-trunk` moves the layers above onto it.
- The tracking lives in `.git/gh-stack`; nothing about the stack is committed.

A lone PR is a branch off `main`: `git switch -c <branch> main`, commit, `git push -u origin <branch>`, `gh pr create`.

## Merging

The author merges a stack in GitHub's interface, squash, bottom-up. After each layer merges, GitHub rebases the next layer onto `main` and force-pushes it, and the local branches catch up in the main checkout:

```bash
gh stack sync                  # from a stack branch: main fast-forwarded, the rest rebased and pushed
git branch -D <merged-branch>  # sync keeps it
gh stack unstack <n> --local   # after the top layer: drops "Stack #<n>" from .git/gh-stack
```

Each merged stack left in the tracking keeps `main` as its trunk, and once two do, `gh stack view` on `main` stops at `branch "main" belongs to multiple stacks`.

An agent asked to merge runs `gh stack merge <pr> --yes --squash`, which merges the stack up to and including that PR, and the same steps follow. Pass both: without the PR number it merges the whole stack, and without `--squash`, `--yes` uses whichever merge method was used last. Not yet tried in this repo.

## Protecting `main`

The repository is public, and on GitHub the ruleset `main-protection` guards `main`. It requires a pull request but no approval, since the author can't approve their own PR, and an approval rule would force every layer out of its stack to merge, as it does in nesto. It takes squash merges only, refuses a force-push and the deletion of `main`, and has no bypass, so it binds the author too. It also requires CI's three jobs to pass (below). The repository's settings also allow squash merge only, and delete a PR's branch once it merges.

Before a push leaves the machine, `.githooks/pre-push` refuses it when:

- it goes to `main`;
- a commit `origin` doesn't have yet breaks a rule `scripts/check-commits.sh` checks (see *Commits*);
- a branch's commit tracks a `.gitignore` anywhere but the root, which `scripts/check-gitignore.sh` checks: an entry goes in the root file under its folder, `/web/dist/` for `web/dist/` alone or `/backend/**/build/` for a `build/` at any depth below `backend/`, and the comment that opens [the root `.gitignore`](../../.gitignore) gives the reason;
- a branch whose new commits change `backend/` fails `./gradlew spotlessCheck`, since google-java-format would change its Java;
- a branch whose new commits change `api/` or `backend/` fails `ArchitectureTest`, which checks where the backend's code lives and what it may use ([hexagonal.md](hexagonal.md)), or doesn't compile, as when the spec in `api/` adds an operation no controller implements;
- a branch whose new commits change `api/` or `web/` fails `pnpm install --frozen-lockfile`, `pnpm generate`, `pnpm typecheck` or `pnpm check` in `web/`, with the pnpm `mise.toml` pins, as when the spec renames a property a page reads ([ADR 0006](../adr/0006-openapi-contract-first.md));
- a branch whose new commits change `.githooks/` or `scripts/` fails shellcheck, at the version `mise.toml` pins, over the hook and `scripts/*.sh`;
- a branch whose new commits change a Markdown file, `.markdownlint-cli2.jsonc`, `scripts/lint-docs.sh`, `scripts/markdownlint-cli2.sh`, `mise.toml` or `mise.lock` fails `scripts/lint-docs.sh` (see [markdown.md](markdown.md));
- a branch whose new commits change `.githooks/` or `scripts/` fails `scripts/test-pre-push.sh`, which pushes a fixture branch through the branch's hook for each case in this list. A new check comes with its fixtures there.

A branch's checks read its commit from Git, exported where a check reads files, so the checkout and what isn't committed play no part, and a check whose tool didn't run refuses the push too. Turn the hook on once per clone with `git config core.hooksPath .githooks`. A refused push is fixed and pushed again; `git push --no-verify` skips the hook, deliberately, and is the author's call.

CI, in `.github/workflows/ci.yml`, runs on each pull request, whatever it changes, and on each commit `main` gets, in three jobs: `backend`, `web` and `repository`. It runs every check in the list above but the refusal of a push to `main`, and every test, the HTTP tests and the browser tests among them, which the hook leaves out. On a pull request, it runs on the merge GitHub builds of the pull request into its base, the tree that would land, and checks the messages of the commits the pull request brings; on `main`, where the commits are history, it leaves the messages alone. A failed `backend` keeps its test reports as an artifact of the run, and a failed `web` keeps the browser tests' report, traces and `stack.log`.
