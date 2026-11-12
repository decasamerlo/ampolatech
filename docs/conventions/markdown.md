# Markdown

How the repository's Markdown is checked: [markdownlint-cli2](https://github.com/DavidAnson/markdownlint-cli2) at the version `mise.toml` pins, with the rules and the files in `.markdownlint-cli2.jsonc`. Adapted from nesto's [Markdown convention](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/markdown.md).

## Running the check

```bash
scripts/lint-docs.sh
```

Run it before committing Markdown, and commit once it exits 0. It takes no arguments, since the config names the files, and lints the same files from any directory. `.githooks/pre-push` runs it too, in the cases [git.md](git.md) lists under *Protecting `main`*.

Its verdict, the exit code and the last line, is the whole result:

| Exit | Last line | Then |
| --- | --- | --- |
| 0 | `lint-docs: ok, N files` | Every file passes. |
| 1 | `lint-docs: N issues in M files, above` | Fix each finding above it, named by file, line and rule (*Fixing findings*), and run it again. |
| 2 | `lint-docs: could not run: <reason>` | Nothing was checked. Act on the reason, such as `mise install` for a missing tool, and run it again. |

The verdict covers what markdownlint-cli2's own output can hide: when the linter fails to start, its output can end in anything, such as the closing brace of a stack trace, and when its globs match no file it exits 0.

## Fixing findings

1. Run `--fix`. It rewrites what a rule can fix on its own, such as a table's separator row or the blank line before a list:

   ```bash
   scripts/markdownlint-cli2.sh --fix
   ```

   Any other run of markdownlint-cli2 goes through that script too, so the linter runs at the pinned version on the pinned Node.
2. Read the diff it made. It also strips the space at either end of a code span, where the space can be the content, as in the commit prefix `` `build(deps): ` ``; restore that text.
3. Fix the rest by hand. When the text a rule flags is the content, as with that prefix, the line keeps its text and ends with `<!-- markdownlint-disable-line <rule> -->`. Every other finding is fixed in the text.
4. Run the check again, until it exits 0.

## What it lints

Every Markdown file `.gitignore` doesn't exclude, tracked or not yet added, so a new file is linted before its first `git add`, and `CLAUDE.local.md` and the drafts in `.scratch/` are not. `.git/info/exclude` isn't read, so a Markdown file that stays out of the lint is listed in `.gitignore`.

The rules are markdownlint's defaults, except:

- **MD013 (line length) is off.** Prose here wraps at the paragraph. The 72-column wrap in [git.md](git.md) is for commit bodies, which `git log` never reflows.
- **MD024 (duplicate headings) flags only siblings**, so a research note can repeat `### Assessment` under each option.
- **MD041 (the first line is a heading) is off for `.github/pull_request_template.md` alone**, which opens with the PR's issue line. Every other rule checks it.

**Changing the config.** It names the files as well as the rules, so the check and `--fix` lint the same files by the same rules. After a change, read the `Finding:` line the check prints: a misspelt key, such as `ignore` for `ignores`, is skipped without a warning.

## The version is pinned

`mise.toml` pins markdownlint-cli2 and the Node it runs on. `mise.lock` locks Node's downloads by their checksums, and markdownlint-cli2's dependency graph, under `.mise/locks/`. A new version can add a rule or change one, and a file that passed starts failing with nobody having edited it. With the pin, those findings arrive only in the change that moves it.

**To bump either pin**, in a change of its own: edit it in `mise.toml`, run `mise lock` and then the check, fix what the new version finds, and commit the pin, the lock and the fixes together once the check exits 0.
