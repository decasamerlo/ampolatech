#!/usr/bin/env bash
# Checks that each commit it is given tracks no .gitignore but the root
# one, whose opening comment gives the reason.
#
#   scripts/check-gitignore.sh <commit>...
#
# Prints each .gitignore below the root with its commit, then how to
# move its entries to the root one, and exits 1 when there is one. It
# prints nothing and exits 0 when there is none, and exits 2 when it is
# given nothing to check or something that isn't a commit, or git can't
# list a commit's files.
set -euo pipefail

if [ "$#" -eq 0 ]; then
  echo "check-gitignore: name the commits to check, such as HEAD" >&2
  exit 2
fi
for commit in "$@"; do
  if ! git rev-parse --verify --quiet "$commit^{commit}" >/dev/null 2>&1; then
    echo "check-gitignore: $commit isn't a commit in this repository" >&2
    exit 2
  fi
done

status=0
for commit in "$@"; do
  id=$(git rev-parse --short "$commit")
  # -z prints each path as it is, where git would quote one holding a
  # character git calls unusual, such as a byte outside ASCII, and the
  # closing quote would hide the name it ends with.
  if ! nested=$(git ls-tree -r -z --name-only "$commit" |
    while IFS= read -r -d '' path; do
      if [[ $path =~ /\.gitignore$ ]]; then
        printf '%s: %s\n' "$id" "$path"
      fi
    done); then
    echo "check-gitignore: git couldn't list the files of $id, above" >&2
    exit 2
  fi
  if [ -n "$nested" ]; then
    printf '%s\n' "$nested" >&2
    status=1
  fi
done

if [ "$status" -eq 1 ]; then
  echo "check-gitignore: the repository keeps one ignore file, at its root, and each entry of the files above moves there under its folder: /web/dist/ ignores web/dist/ alone and /web/**/dist/ a dist/ at any depth below web/, which is what dist/ in web/.gitignore ignores; the narrow form fits when the tool writes in one place" >&2
fi
exit "$status"
