#!/bin/sh
# Tests .githooks/pre-push as a commit holds it. It builds a throwaway
# repository from the commit's tree, with origin/main at it, and feeds
# the hook a push of one fixture branch per case in "Protecting main" in
# docs/conventions/git.md, matching the exit and what the hook printed.
#
#   scripts/test-pre-push.sh [<commit>]    # HEAD by default
#
# Prints a line per case and ends on its verdict: exit 0 when every case
# behaves as expected, 1 when any doesn't, and 2 when it can't run.
set -eu

verdict() { # exit-code message
  finished=1
  echo "test-pre-push: $2"
  exit "$1"
}

finished=""
work=""
# Whatever stops it before its verdict, its last line says it didn't run.
# shellcheck disable=SC2329 # the EXIT trap runs it
on_exit() {
  code=$?
  [ -z "$work" ] || rm -rf "$work"
  [ -n "$finished" ] || verdict 2 "could not run: stopped with exit $code, above"
}
trap on_exit EXIT
trap 'exit 2' HUP INT TERM

[ "$#" -le 1 ] || verdict 2 "could not run: it takes one commit at most"
commit=${1:-HEAD}
sha=$(git rev-parse --verify --quiet "$commit^{commit}") || verdict 2 "could not run: $commit isn't a commit in this repository"
work=$(mktemp -d)
git archive "$sha" | tar -x -C "$work"

# A variable such as GIT_DIR, set where the push ran, reaches this
# script through the hook, and every command below works in the
# throwaway repository.
# shellcheck disable=SC2046 # one word per variable
unset $(git rev-parse --local-env-vars)
export GIT_AUTHOR_NAME=test-pre-push GIT_AUTHOR_EMAIL=test-pre-push@example.invalid
export GIT_COMMITTER_NAME=test-pre-push GIT_COMMITTER_EMAIL=test-pre-push@example.invalid
cd "$work"
git init -q -b main
# Git's default, which a user's config can turn off. The cases with a
# file named outside ASCII need git to quote that name, so they show
# the hook reads it all the same.
git config core.quotePath true
git add -A -f # the commit's whole tree, the paths .gitignore lists included
git commit -q -m "chore: the commit under test" -m "Refs: #1"
git update-ref refs/remotes/origin/main main

branch() { # name
  git switch -q -c "$1" main
}
commit_probe() {
  git add -A
  git commit -q -m "chore: probe" -m "Refs: #1"
}

branch bad-message
echo probe >probe.txt
git add -A
git commit -q -m "Probe"
branch scope-non-ascii
echo probe >scripts/ação.txt
git add -A
git commit -q -m "chore(scripts): probe" -m "Refs: #1"
branch gitignore-nested
printf 'dist/\n' >web/.gitignore
mkdir web/ação
printf 'generated/\n' >web/ação/.gitignore
commit_probe
# Its first commit adds a .gitignore and its second moves the entry to
# the root one: its new commits change docs/.gitignore, and its commit
# holds no such file.
branch gitignore-moved
printf 'probe/\n' >docs/.gitignore
commit_probe
git rm -q docs/.gitignore
printf '/docs/**/probe/\n' >>.gitignore
commit_probe
# Origin has its commit already, as after a push that skipped the hook,
# so a push of it sends no new commit.
branch gitignore-known
printf 'dist/\n' >web/.gitignore
commit_probe
git update-ref refs/remotes/origin/gitignore-known gitignore-known
branch java-unformatted
printf 'final class Probe{  }\n' >backend/app/src/main/java/Probe.java
commit_probe
branch java-formatted
printf 'final class Probe {}\n' >backend/app/src/main/java/Probe.java
commit_probe
branch build-unconfigurable
echo 'this is not Kotlin' >>backend/build.gradle.kts
commit_probe
branch java-uncompilable
# javac repeats the line it can't compile, comment included, among the
# lines Gradle prints; the hook must read only Gradle's own.
printf 'final class Probe {\n  Missing field; // There were failing tests\n}\n' >backend/app/src/main/java/Probe.java
commit_probe
branch java-unparseable
printf 'final class Probe {\n' >backend/app/src/main/java/Probe.java
commit_probe
branch java-unlayered
printf 'package dev.ampolatech.adapter.in.web.rest;\n\nimport dev.ampolatech.application.UserApplicationService;\n\nfinal class Probe {\n  private UserApplicationService service;\n}\n' >backend/app/src/main/java/dev/ampolatech/adapter/in/web/rest/Probe.java
commit_probe
branch spec-unimplemented
{
  sed -n '1,/^paths:$/p' api/openapi.yaml
  printf '  /api/probe:\n    get:\n      operationId: getProbe\n      tags: [users]\n      responses:\n        "204":\n          description: The probe.\n'
  sed '1,/^paths:$/d' api/openapi.yaml
} >api/probe.yaml
mv api/probe.yaml api/openapi.yaml
commit_probe
branch spec-role-unlabelled
sed 's/enum: \[PHARMACIST, SALES_AGENT\]/enum: [PHARMACIST, SALES_AGENT, PROBE]/' api/openapi.yaml >api/probe.yaml
mv api/probe.yaml api/openapi.yaml
commit_probe
branch web-lockfile-stale
sed 's/"react": "19.3.1"/"react": "19.3.0"/' web/package.json >web/probe.json
mv web/probe.json web/package.json
commit_probe
branch web-ungenerated
sed 's|../api/openapi.yaml|../api/missing.yaml|' web/orval.config.ts >web/probe.ts
mv web/probe.ts web/orval.config.ts
commit_probe
branch web-missing
git rm -q -r web
commit_probe
branch web-unchecked
printf 'import { useGetSignedInUser } from "../api/generated/users/users";\n\nexport function OrdersPage() {\n\tconst query = useGetSignedInUser();\n\treturn <h1>{query.data?.email}</h1>;\n}\n' >web/src/orders/orders-page.tsx
commit_probe
branch web-unformatted
printf 'export const probe  =  1;\n' >web/src/probe.ts
commit_probe
branch web-unformatted-non-ascii
printf 'export const probe  =  1;\n' >web/src/ação.ts
commit_probe
branch web-clean
printf 'export const probe = 1;\n' >web/src/probe.ts
commit_probe
# shellcheck disable=SC2016 # the fixture script's own $1
{
  branch script-flagged
  printf '#!/bin/sh\necho $1\n' >scripts/probe.sh
  commit_probe
  branch script-clean
  printf '#!/bin/sh\necho "$1"\n' >scripts/probe.sh
  commit_probe
}
branch md-failing
# shellcheck disable=SC2016 # the backticks open a Markdown code block
printf '# Probe\n\n```\ntext\n```\n' >docs/probe.md
commit_probe
branch md-clean
printf '# Probe\n\nText.\n' >docs/probe.md
commit_probe
branch md-unlinted
git rm -q scripts/lint-docs.sh
printf '# Probe\n\nText.\n' >docs/probe.md
commit_probe
branch lint-config
echo '// probe' >>.markdownlint-cli2.jsonc
commit_probe
branch lint-script
echo '# probe' >>scripts/lint-docs.sh
commit_probe
branch lint-wrapper
echo '# probe' >>scripts/markdownlint-cli2.sh
commit_probe
branch mise-pins
echo '# probe' >>mise.toml
commit_probe
branch unchecked
echo probe >probe.txt
commit_probe
git switch -q main

zero=0000000000000000000000000000000000000000
push() { # branch: the line Git hands the hook to push it to its namesake
  printf 'refs/heads/%s %s refs/heads/%s %s\n' "$1" "$(git rev-parse "refs/heads/$1")" "$1" "$zero"
}
count=0
failed=0
expect() { # name exit pattern push-line, the pattern matched against all the hook printed
  count=$((count + 1))
  status=0
  out=$(printf '%s\n' "$4" | .githooks/pre-push origin "$work" 2>&1) || status=$?
  # shellcheck disable=SC2254 # $3 is a pattern
  case $status:$out in
    "$2":$3) echo "ok    $1" ;;
    *)
      failed=$((failed + 1))
      echo "FAIL  $1: exit $status, expected $2; the hook printed:"
      printf '%s\n' "$out" | sed 's/^/        /'
      ;;
  esac
}

expect "a push to main is refused" 1 '*main takes changes only through pull requests*' "$(push main)"
expect "a commit message check-commits.sh refuses is refused" 1 '*commit messages above break*' "$(push bad-message)"
expect "a scope holding a file named outside ASCII passes" 0 '*' "$(push scope-non-ascii)"
# The refusal's own example names web/.gitignore, so the pattern matches
# the lines that list the files, each after its commit.
expect "a .gitignore anywhere but the root is refused, each one named" 1 '*: web/.gitignore*: web/ação/.gitignore*gitignore-nested tracks*on that branch and amend' "$(push gitignore-nested)"
expect "a .gitignore a later commit moves to the root passes with nothing printed" 0 '' "$(push gitignore-moved)"
expect "a .gitignore on a branch at a commit origin has is refused" 1 '*: web/.gitignore*gitignore-known tracks*on that branch and amend' "$(push gitignore-known)"
expect "Java google-java-format would change is refused" 1 '*fails the format check*' "$(push java-unformatted)"
expect "Java google-java-format leaves alone passes" 0 '*' "$(push java-formatted)"
expect "a build Gradle can't configure is refused as not format-checked" 1 '*Gradle stopped on build-unconfigurable before it ran the format check*' "$(push build-unconfigurable)"
expect "Java google-java-format can't parse is refused, the parse named" 1 '*fails the format check above: google-java-format can*t parse*' "$(push java-unparseable)"
expect "Java that doesn't compile is refused as not tested" 1 '*Gradle stopped on java-uncompilable before it ran the architecture test*' "$(push java-uncompilable)"
expect "a web adapter using an application service is refused, its rule and class named" 1 '*the_layers_should_reach_each_other_only_through_the_ports FAILED*Probe.service*fails the architecture test*' "$(push java-unlayered)"
expect "an operation the spec adds and no controller implements is refused as not tested" 1 '*does not override abstract method getProbe*Gradle stopped on spec-unimplemented before it ran the architecture test*' "$(push spec-unimplemented)"
expect "a role the spec adds and the web app has no label for is refused, the type-check named" 1 "*Property 'PROBE' is missing*fails the web app's type-check*" "$(push spec-role-unlabelled)"
expect "a web/ lockfile out of step with package.json is refused" 1 '*ERR_PNPM_OUTDATED_LOCKFILE*fails pnpm install --frozen-lockfile*' "$(push web-lockfile-stale)"
expect "a client Orval can't generate is refused" 1 '*missing.yaml*fails pnpm generate*' "$(push web-ungenerated)"
expect "a change to web/ that leaves no web/ is refused as not checked" 1 '*has no web/ to check*' "$(push web-missing)"
expect "a page reading a property the spec's SignedInUser lacks is refused, the type-check named" 1 "*Property 'email' does not exist on type 'SignedInUser'*fails the web app's type-check*" "$(push web-unchecked)"
expect "web/ code Biome would reformat is refused" 1 "*fails Biome's check*" "$(push web-unformatted)"
expect "web/ code Biome would reformat, in a file named outside ASCII, is refused" 1 "*ação.ts*fails Biome's check*" "$(push web-unformatted-non-ascii)"
expect "web/ code Biome leaves alone passes" 0 '*' "$(push web-clean)"
expect "a script shellcheck flags is refused" 1 '*fails shellcheck*' "$(push script-flagged)"
expect "a clean script passes" 0 '*' "$(push script-clean)"
expect "Markdown the lint fails is refused" 1 '*fails the Markdown lint*' "$(push md-failing)"
expect "clean Markdown passes the lint" 0 '*lint-docs: ok*' "$(push md-clean)"
expect "Markdown on a branch without scripts/lint-docs.sh is refused as not linted" 1 '*did not lint*' "$(push md-unlinted)"
expect "a change to .markdownlint-cli2.jsonc runs the lint" 0 '*lint-docs: ok*' "$(push lint-config)"
expect "a change to scripts/lint-docs.sh runs the lint" 0 '*lint-docs: ok*' "$(push lint-script)"
expect "a change to scripts/markdownlint-cli2.sh runs the lint" 0 '*lint-docs: ok*' "$(push lint-wrapper)"
expect "a change to mise.toml runs the lint" 0 '*lint-docs: ok*' "$(push mise-pins)"
expect "a push that deletes a branch passes" 0 '' "(delete) $zero refs/heads/gone $(git rev-parse main)"
expect "a change no check covers passes with nothing printed" 0 '' "$(push unchecked)"

[ "$failed" -eq 0 ] || verdict 1 "$failed of $count cases failed, above"
verdict 0 "ok, $count cases"
