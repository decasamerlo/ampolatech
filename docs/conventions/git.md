# Git

How changes reach `main`: branch names, commits, pull requests, and GitHub's native stacks (`gh stack`) for changes that depend on each other. The stacking workflow is adapted from nesto's [stacked-PRs workflow](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/stacked-prs.md), which also covers the co-developers and forks this repo doesn't have.

## Rules

1. **Every change reaches `main` through a pull request.**
2. **One PR, one commit.** Amend instead of adding a commit; squash merge makes that commit the one on `main`.
3. **A PR changes up to ~400 lines.** A bigger one says why in its Summary, such as generated code or a research note read as a whole; anything else is split into smaller PRs, stacked where one depends on another.
4. **Only the author merges.** An agent merges only when asked to in the session.

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
- **`scripts/check-commits.sh <commit>...` checks the rules above that concern form**: the subject's shape and length, a scope against the changed files, a body for `feat` and `fix`, the prose's wrap (a line that is only a URL may run longer), no list or table in the body, no line starting with `#`, which git drops as a comment when the message is edited, the `Refs:` trailer and no closing keyword. A body over 15 lines of text gets a warning, not a failure. Run it on each new commit, `scripts/check-commits.sh HEAD`, before building on it. `.githooks/pre-push` runs it on each commit a push sends, and review checks the rest, a missing scope among them.

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

Squash, bottom-up. `gh stack merge <pr> --yes --squash` merges the stack up to and including that PR, and `gh stack sync` then rebases what remains onto `main` and pushes it. Pass both: without the PR number it merges the whole stack, and without `--squash`, `--yes` uses whichever merge method was used last. Not yet tried in this repo.

## Protecting `main`

The repository is private on GitHub's free plan, which allows no ruleset, so GitHub itself doesn't stop a push to `main`. Until it does:

- `.githooks/pre-push` refuses a push when:

  - it goes to `main`;
  - a commit `origin` doesn't have yet breaks a rule `scripts/check-commits.sh` checks (see *Commits*);
  - a branch whose new commits change `backend/` fails `./gradlew spotlessCheck`, since google-java-format would change its Java;
  - a branch whose new commits change `.githooks/` or `scripts/` fails shellcheck, at the version `mise.toml` pins, over the hook and `scripts/*.sh`;
  - a branch whose new commits change a Markdown file, `.markdownlint-cli2.jsonc`, `scripts/lint-docs.sh`, `mise.toml` or `mise.lock` fails `scripts/lint-docs.sh` (see [markdown.md](markdown.md));
  - a branch whose new commits change `.githooks/` or `scripts/test-pre-push.sh` fails `scripts/test-pre-push.sh`, which pushes a fixture branch through the branch's hook for each case in this list. A new check comes with its fixtures there.

  A branch's checks run on its commit exported from Git, so the checkout and what isn't committed play no part, and a check whose tool didn't run refuses the push too. Turn the hook on once per clone with `git config core.hooksPath .githooks`. A refused push is fixed and pushed again; `git push --no-verify` skips the hook, deliberately, and is the author's call.
- The repository allows squash merge only, taking the commit's title and messages, and deletes a PR's branch once it merges.

Once the repository is public, a ruleset on `main` takes over: a pull request required, squash only, no force-push. It requires no approval. The author can't approve their own PR, and an approval rule would force every layer out of its stack to merge, as it does in nesto.
