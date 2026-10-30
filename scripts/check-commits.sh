#!/usr/bin/env bash
# Checks each commit it is given against the rules in
# docs/conventions/git.md that concern a commit message's form. Review
# checks the rest, from the why in the body to a missing scope.
#
#   scripts/check-commits.sh <commit>...
#
# Prints each breach with its commit and exits 1 when there is one. A
# body longer than 15 lines of text gets a warning that doesn't fail the
# check.
set -euo pipefail
# The wrap is counted in characters, which a Git client running without
# a UTF-8 locale would count in bytes.
export LC_ALL=C.UTF-8

SUBJECT_RE='^(feat|fix|docs|chore|build)(\(([a-z0-9.-]+)\))?: (.+)$'
CLOSING_RE='\b(close[sd]?|fix(e[sd])?|resolve[sd]?):?[[:space:]]+(([[:alnum:]_.-]+/[[:alnum:]_.-]+)?#[0-9]+|https://github\.com/[^[:space:]]+/issues/[0-9]+)'
status=0

breach() {
  echo "$id: $1" >&2
  status=1
}

# The trailers are the paragraphs at the end of the body made only of
# git.md's trailers, Refs and Co-Authored-By: GitHub's squash merge puts
# Co-authored-by in a paragraph of its own.
body_part() { # prose | trailers
  printf '%s\n' "$body" | awk -v RS= -v part="$1" '
    function trailers(paragraph,   n, line, i) {
      n = split(paragraph, line, "\n")
      for (i = 1; i <= n; i++) if (tolower(line[i]) !~ /^(refs|co-authored-by): /) return 0
      return 1
    }
    { p[NR] = $0 }
    END {
      k = NR + 1
      while (k > 1 && trailers(p[k - 1])) k--
      from = part == "prose" ? 1 : k
      to = part == "prose" ? k - 1 : NR
      for (i = from; i <= to; i++) print p[i] "\n"
    }'
}

for commit in "$@"; do
  id=$(git rev-parse --short "$commit")
  subject=$(git show -s --format=%s "$commit")
  # GitHub appends " (#N)" to the subject when it squash-merges a PR.
  if git merge-base --is-ancestor "$commit" origin/main 2>/dev/null; then
    subject=${subject% (#*)}
  fi
  body=$(git show -s --format=%b "$commit")
  prose=$(body_part prose)
  trailers=$(body_part trailers)

  if [ "${#subject}" -gt 72 ]; then
    breach "subject: ${#subject} characters, over 72"
  fi
  if ! [[ $subject =~ $SUBJECT_RE ]]; then
    breach "subject: not <type>(<scope>): <summary>, with a type of feat, fix, docs, chore or build"
  else
    type=${BASH_REMATCH[1]}
    scope=${BASH_REMATCH[3]}
    summary=${BASH_REMATCH[4]}
    if [[ $summary =~ ^[A-Z] ]]; then
      breach "subject: the summary starts in upper case"
    fi
    if [[ $summary == *. ]]; then
      breach "subject: the summary ends with a period"
    fi
    if [ -n "$scope" ]; then
      # CLAUDE.md points into every folder, so it goes with any scope.
      outside=$(git diff-tree --root --no-commit-id --name-only -r "$commit" |
        while IFS= read -r file; do
          [[ $file == "$scope/"* || $file == "docs/$scope/"* || $file == CLAUDE.md ]] || printf '%s\n' "$file"
        done)
      if [ -n "$outside" ]; then
        breach "scope: ($scope) doesn't hold $(wc -l <<<"$outside") of the changed files, $(head -1 <<<"$outside") among them; a change with no single home leaves the scope out"
      fi
    fi
    if [[ $type == feat || $type == fix ]] && [ -z "$(printf '%s' "$prose" | tr -d '[:space:]')" ]; then
      breach "body: a $type commit needs one"
    fi
  fi

  while IFS= read -r line; do
    if [ "${#line}" -gt 72 ] && ! [[ $line =~ ^https?://[^[:space:]]+$ ]]; then
      breach "body: a line of ${#line} columns, over 72: ${line:0:40}…"
    fi
  done <<<"$prose"
  if printf '%s\n' "$prose" | grep -Eq '^(#+[[:space:]]|[[:space:]]*([-*+]|[0-9]{1,2}[.)])[[:space:]]|\|)'; then
    breach "body: a heading, a list item or a table row, where the body is prose"
  fi
  lines=$(printf '%s\n' "$prose" | grep -c '[^[:space:]]' || true)
  if [ "$lines" -gt 15 ]; then
    echo "$id: warning: a body of $lines lines of text; git.md asks for about 12, and a longer one often holds two concerns" >&2
  fi

  if ! printf '%s\n' "$trailers" | grep -Eq '^Refs: #[0-9]+'; then
    breach "trailers: no Refs: #N"
  fi
  closing=$(git show -s --format=%B "$commit" | grep -Eio "$CLOSING_RE" | head -1 || true)
  if [ -n "$closing" ]; then
    breach "message: \"$closing\" closes an issue, which is the PR body's job; the commit takes Refs: #N"
  fi
done

exit "$status"
