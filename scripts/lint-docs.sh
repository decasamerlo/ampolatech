#!/bin/sh
# Lints the repository's Markdown with markdownlint-cli2, through
# scripts/markdownlint-cli2.sh. .markdownlint-cli2.jsonc holds the rules
# and names the files.
#
#   scripts/lint-docs.sh
#
# Exits 0 when every file passes, 1 when any fails, and 2 when it can't
# run, and its last line is its own verdict. docs/conventions/markdown.md
# says how to read the result and how to bump the version.
set -eu

verdict() { # exit-code message
  echo "lint-docs: $2"
  exit "$1"
}

[ "$#" -eq 0 ] || verdict 2 "could not run: it takes no arguments; .markdownlint-cli2.jsonc names the files"
cd "$(dirname "$0")/.."

status=0
report=$(scripts/markdownlint-cli2.sh </dev/null 2>&1) || status=$?
printf '%s\n' "$report"

# When the wrapper refuses to start the linter, its last line gives why.
refusal=$(printf '%s\n' "$report" | sed -n '$s/^markdownlint-cli2\.sh: could not run: //p')
[ -z "$refusal" ] || verdict 2 "could not run: $refusal"

# markdownlint-cli2 exits 0 when it lints no file at all, as when the
# globs match nothing, so the count it prints decides first.
file_count=$(printf '%s\n' "$report" | sed -n 's/^Linting: \([0-9][0-9]*\) files\{0,1\}$/\1/p')
case $file_count in
  '') verdict 2 "could not run: scripts/markdownlint-cli2.sh exited $status before linting any file, above" ;;
  0) verdict 2 "could not run: no file linted; check the Finding line above against .markdownlint-cli2.jsonc" ;;
esac
case $status in
  0) verdict 0 "ok, $file_count files" ;;
  1) verdict 1 "$(printf '%s\n' "$report" | sed -n 's/^Summary: //p'), above" ;;
  *) verdict 2 "could not run: markdownlint-cli2 stopped with exit $status, above" ;;
esac
