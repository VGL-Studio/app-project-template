#!/usr/bin/env bash
# Tests for the shell scripts of the quality gate. Needs bash, python3 and jq.
set -euo pipefail
cd "$(dirname "$0")/../.."

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/bin"
cp scripts/tests/fake-gh "$work/bin/gh"
chmod +x "$work/bin/gh"

failures=0
expect() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "ok   - $name"
  else
    echo "FAIL - $name: expected '$expected', got '$actual'"
    failures=$((failures + 1))
  fi
}

# Commits: A=aaaaaa1, B=bbbbbb2, C=ccccc03, D=ddddd04 (D is the red head).
find_breaking() {
  rm -f "$work/fx.json.calls"
  echo "$1" > "$work/fx.json"
  PATH="$work/bin:$PATH" FIXTURE="$work/fx.json" GITHUB_REPOSITORY=o/r POLL_SECONDS=0 MAX_WAIT_SECONDS=1 \
    scripts/find-breaking-commit.sh ddddd04 2>/dev/null
}

expect "parent green: blame the head" ddddd04 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "ddddd04":{"parent":"aaaaaa1","states":["failure"]}}')"

expect "older red commits: blame the oldest red after the last green" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["failure"]},
  "ccccc03":{"parent":"bbbbbb2","states":["failure"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "pending parent that turns red is blamed" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["pending","failure"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

expect "pending parent that turns green clears it" ddddd04 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["pending","success"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

expect "docs-only commit is skipped" bbbbbb2 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["failure"]},
  "ccccc03":{"parent":"bbbbbb2","files":["docs/a.md"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "run not created yet is waited for" ccccc03 "$(find_breaking '{
  "aaaaaa1":{"parent":"","states":["success"]},
  "bbbbbb2":{"parent":"aaaaaa1","states":["success"]},
  "ccccc03":{"parent":"bbbbbb2","states":[null,"failure"]},
  "ddddd04":{"parent":"ccccc03","states":["failure"]}}')"

expect "start of history: blame the oldest red" bbbbbb2 "$(find_breaking '{
  "bbbbbb2":{"parent":"","states":["failure"]},
  "ddddd04":{"parent":"bbbbbb2","states":["failure"]}}')"

# merge-if-green.sh must reject any argument other than exactly "<PR> [--dry-run]" before doing anything.
for args in "12 --dryrun" "12 -n" "12 --dry_run" "12 --dry-run extra" "abc" ""; do
  # shellcheck disable=SC2086
  if PATH="$work/bin:$PATH" scripts/merge-if-green.sh $args > /dev/null 2>&1; then
    expect "merge-if-green rejects '$args'" rejected accepted
  else
    expect "merge-if-green rejects '$args'" rejected rejected
  fi
done

if [[ "$failures" -gt 0 ]]; then
  echo "$failures script test(s) failed"
  exit 1
fi
echo "All script tests passed"
