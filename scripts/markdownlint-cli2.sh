#!/bin/sh
# Runs markdownlint-cli2 with the arguments it's given, from the
# repository root, at the version mise.toml pins and on the Node it pins.
#
#   scripts/markdownlint-cli2.sh [<argument>...]
#
# Exits with the linter's exit code. When it can't start the linter, it
# exits 2, and its last line starts "markdownlint-cli2.sh: could not run: ".
set -eu

refuse() { # message
  echo "markdownlint-cli2.sh: could not run: $1" >&2
  exit 2
}

cd "$(dirname "$0")/.."

command -v mise >/dev/null 2>&1 || refuse "mise isn't on PATH"
if ! node=$(mise which node 2>/dev/null) || ! mise which markdownlint-cli2 >/dev/null 2>&1; then
  refuse "Node or markdownlint-cli2 isn't installed at the version mise.toml pins; run mise install"
fi

# markdownlint-cli2's launcher runs the first node on PATH, and the PATH
# mise exec hands its command keeps the entries that come before mise's
# shims, such as nvm's Node, ahead of the pinned tools. The inner shell
# puts the pinned Node first.
# shellcheck disable=SC2016 # the inner shell expands PATH, after mise exec sets it
exec mise exec -- sh -c 'PATH=$1:$PATH; shift; exec markdownlint-cli2 "$@"' sh "${node%/*}" "$@"
