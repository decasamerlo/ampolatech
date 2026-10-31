#!/bin/sh
# Lints the repository's Markdown with markdownlint-cli2, at the version
# mise.toml pins and on the Node it pins. .markdownlint-cli2.jsonc holds
# the rules and names the files.
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

command -v mise >/dev/null 2>&1 || verdict 2 "could not run: mise isn't on PATH"
if ! node=$(mise which node 2>/dev/null) || ! mise which markdownlint-cli2 >/dev/null 2>&1; then
  verdict 2 "could not run: Node or markdownlint-cli2 isn't installed at the version mise.toml pins; run mise install"
fi

# markdownlint-cli2's launcher runs the first node on PATH, and the PATH
# mise exec hands its command keeps the entries that come before mise's
# shims, such as nvm's Node, ahead of the pinned tools. The inner shell
# puts the pinned Node first.
status=0
# shellcheck disable=SC2016 # the inner shell expands PATH, after mise exec sets it
report=$(mise exec -- sh -c 'PATH=$1:$PATH; exec markdownlint-cli2' sh "${node%/*}" </dev/null 2>&1) || status=$?
printf '%s\n' "$report"

# markdownlint-cli2 exits 0 when it lints no file at all, as when the
# globs match nothing, so the count it prints decides first.
file_count=$(printf '%s\n' "$report" | sed -n 's/^Linting: \([0-9][0-9]*\) files\{0,1\}$/\1/p')
case $file_count in
  '') verdict 2 "could not run: markdownlint-cli2 stopped with exit $status before it linted, above" ;;
  0) verdict 2 "could not run: no file linted; check the Finding line above against .markdownlint-cli2.jsonc" ;;
esac
case $status in
  0) verdict 0 "ok, $file_count files" ;;
  1) verdict 1 "$(printf '%s\n' "$report" | sed -n 's/^Summary: //p'), above" ;;
  *) verdict 2 "could not run: markdownlint-cli2 stopped with exit $status, above" ;;
esac
