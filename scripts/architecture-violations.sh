#!/bin/sh
# Names what each ArchitectureTest rule that failed on the last run
# depends on: every package its violations reach, with the classes that
# reach it, which for an allow-list is what the list lacks. It reads the
# JUnit report that run left, so it answers for that run.
#
#   scripts/architecture-violations.sh
#
# Exits 0 when no rule failed, 1 when one did, and 2 when it has no
# report to read, and its last line is its own verdict.
# docs/conventions/hexagonal.md says when to run it.
set -eu
export LC_ALL=C

verdict() { # exit-code message
  echo "architecture-violations: $2"
  exit "$1"
}

[ "$#" -eq 0 ] || verdict 2 "could not run: it takes no arguments"
cd "$(dirname "$0")/.."

report=backend/app/build/test-results/test/TEST-dev.ampolatech.ArchitectureTest.xml
[ -f "$report" ] ||
  verdict 2 "could not run: no report at $report; in backend/, run ./gradlew :app:test --tests dev.ampolatech.ArchitectureTest"

# Tab-separated records, sorted: a rule that failed, alone; a dependency,
# as the rule, the package reached and the class reaching it; or a
# violation that names no dependency, as the rule and the violation. The
# report writes each violation twice, in the failure's message and in its
# body, and only the body has one per line.
records=$(awk '
  function unescape(text) {
    gsub(/&lt;/, "<", text)
    gsub(/&gt;/, ">", text)
    gsub(/&quot;/, "\"", text)
    gsub(/&apos;/, "\047", text)
    gsub(/&amp;/, "\\&", text)
    return text
  }
  # The segments of a dotted name before the first that starts upper
  # case, which make its package; with class set, that segment too,
  # without a nested class, which make its top-level class.
  function leading(name, class,   n, segment, i, result) {
    n = split(name, segment, ".")
    result = ""
    for (i = 1; i <= n; i++) {
      if (segment[i] ~ /^[A-Z]/) {
        if (class) {
          sub(/\$.*/, "", segment[i])
          result = result (result == "" ? "" : ".") segment[i]
        }
        break
      }
      result = result (result == "" ? "" : ".") segment[i]
    }
    return result
  }
  /<testcase / {
    rule = $0
    sub(/.*<testcase name="/, "", rule)
    sub(/".*/, "", rule)
  }
  /<failure / { print rule }
  /^[A-Z][A-Za-z ]* &lt;/ {
    line = unescape($0)
    sub(/ in \([^()]*\)$/, "", line)
    # Each " <" opens a name. The first is the member or class the
    # violation is in, or for a parameter its type, which the member
    # follows; the last is the dependency, when the violation names one.
    n = 0
    at = 0
    while ((found = index(substr(line, at + 1), " <")) > 0) {
      at += found
      opened[++n] = at + 2
    }
    parameter = line ~ /^Parameter /
    if (n == 1 + parameter) {
      print rule "\t" line
      next
    }
    reaching = substr(line, opened[1 + parameter])
    reaching = leading(substr(reaching, 1, index(reaching " ", "> ") - 1), 1)
    sub(/.*\./, "", reaching)
    print rule "\t" leading(substr(line, opened[n]), 0) "\t" reaching
  }
' "$report" | sort -u)

failed=$(printf '%s\n' "$records" | awk -F '\t' 'NF == 1 && $0 != "" { n++ } END { print n + 0 }')
[ "$failed" -gt 0 ] || verdict 0 "no rule failed on the last run"

printf '%s\n' "$records" | awk -F '\t' '
  function flush() {
    if (line != "") print line
    line = ""
  }
  $1 != rule { flush(); if (rule != "") print ""; rule = $1; print rule ":"; package = "" }
  NF == 1 { next }
  NF == 2 { flush(); print "  " $2; next }
  $2 != package { flush(); package = $2; line = "  " package ":" }
  { line = line " " $3 }
  END { flush() }
'
[ "$failed" -eq 1 ] && verdict 1 "1 rule failed, above"
verdict 1 "$failed rules failed, above"
