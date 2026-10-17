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

# merge-if-green.sh refuses other PRs while an auto-run session is running, but lets that session's PRs through.
merge_during_run() {
  local head_ref="$1" run_status="$2" labels="${3:-[]}"
  cat > "$work/merge.json" << JSON
{"routes": {
  "pulls/12": {"state": "open", "draft": false, "base": {"ref": "main"}, "head": {"sha": "abc1234", "ref": "$head_ref"}, "title": "t", "labels": $labels},
  "issues?state=open&labels=main-broken&per_page=10": [],
  "branches?per_page=100": [{"name": "main"}, {"name": "claude/RUN-20261015-01-report"}, {"name": "claude/RUN-20261016-01-report"}],
  "contents/docs/runs/RUN-20261015-01.md?ref=claude/RUN-20261015-01-report": "---\nid: RUN-20261015-01\nstatus: finished\n---\n",
  "contents/docs/runs/RUN-20261016-01.md?ref=claude/RUN-20261016-01-report": "---\nid: RUN-20261016-01\nstatus: $run_status   # draft | approved | running | finished\n---\n",
  "compare/main...abc1234": {"behind_by": 2}
}}
JSON
  local code=0
  PATH="$work/bin:$PATH" FIXTURE="$work/merge.json" scripts/merge-if-green.sh 12 --dry-run > /dev/null 2>&1 || code=$?
  echo "$code"
}
# Exit 3 ("branch is behind main") means the run check let the PR through; 1 means it was refused.
expect "merge-if-green refuses a person's PR while a run is running" 1 "$(merge_during_run claude/template-x running)"
expect "merge-if-green lets the session's task PR through" 3 "$(merge_during_run claude/T-001-loading running)"
expect "merge-if-green lets the session's report PR through" 3 "$(merge_during_run claude/RUN-20261016-01-report running)"
expect "merge-if-green ignores finished runs" 3 "$(merge_during_run claude/template-x finished)"
expect "merge-if-green refuses a PR labelled needs-human" 1 \
  "$(merge_during_run claude/fix-T-002-x finished '[{"name": "needs-human"}]')"
expect "merge-if-green accepts other labels" 3 "$(merge_during_run claude/template-x finished '[{"name": "docs"}]')"

# DesignCompare: an identical image differs by 0%, a design without a screenshot is reported missing.
java_cmd="java"
[ -z "${JAVA_HOME:-}" ] || java_cmd="${JAVA_HOME}/bin/java"
mkdir -p "$work/design" "$work/shots"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/design/Same.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/shots/Same.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_dark.png "$work/design/Dark.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/shots/Dark.png"
cp feature/sample/src/test/screenshots/SampleScreen_ready_light.png "$work/design/Missing.png"
"$java_cmd" -Djava.awt.headless=true scripts/design-compare/DesignCompare.java "$work/design" "$work/shots" "$work/review" > /dev/null
expect "design-compare: identical images differ by 0%" "| Same | 0.0% | \`Same_compare.png\` |" "$(grep '^| Same ' "$work/review/summary.md")"
expect "design-compare: light against dark differs" yes "$(grep -qE '^\| Dark \| ([1-9][0-9]?|100)\.[0-9]%' "$work/review/summary.md" && echo yes || echo no)"
expect "design-compare: missing screenshot is listed" 1 "$(grep -c '^| Missing | thiếu ảnh chuẩn' "$work/review/summary.md")"
expect "design-compare: writes side-by-side images" yes "$([ -s "$work/review/Same_compare.png" ] && echo yes || echo no)"

# figma-export.py (offline): context files match frames by name and map values to tokens.json.
# The fixture tokens.json is a fixed copy, so changing the real design system never breaks these tests (L-020).
FIGMA_EXPORT_FEATURES_DIR="$work/features" FIGMA_EXPORT_TOKENS=scripts/tests/fixtures/tokens.json \
  python3 -I scripts/figma-export.py --offline \
  scripts/tests/fixtures/figma-nodes.json loading > /dev/null
context="$work/features/loading/design/context"
expect "figma-export: only frames named like screenshots" "LoadingScreen_failed_light.md" "$(ls "$context")"
expect "figma-export: text style matched to a token" 1 "$(grep -c 'kiểu titleMedium (16/24, đậm 500)' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: color matched to light roles" 1 "$(grep -c '| màu nền | #FEF7FF | background, surface |' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: unknown values flagged" 2 "$(grep -c '| ngoài hệ thống |' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: hidden layers left out" 0 "$(grep -c 'Hidden' "$context/LoadingScreen_failed_light.md")"
expect "figma-export: spacing matched" 1 "$(grep -c 'cách nhau 16 (md)' "$context/LoadingScreen_failed_light.md")"

# release.sh must refuse a release folder that is incomplete or holds staging/test values, before touching git.
release_dir="$work/release"
release_fails_with() {
  local name="$1" pattern="$2" output
  if output="$(scripts/release.sh --config "$release_dir" "${@:3}" 2>&1 < /dev/null)"; then
    expect "release.sh refuses: $name" refused accepted
  elif [[ "$output" == *"$pattern"* ]]; then
    expect "release.sh refuses: $name" refused refused
  else
    expect "release.sh refuses: $name" "message with '$pattern'" "$output"
  fi
}
write_release_properties() {
  printf '%s\n' "admobAppId=$1" "adUnits=$2" "assetsBaseUrl=$3" "uploadKeyAlias=upload" > "$release_dir/release.properties"
}
mkdir -p "$release_dir"
release_fails_with "missing files" "/release/google-services.json"
touch "$release_dir/google-services.json" "$release_dir/upload.jks"
write_release_properties "ca-app-pub-3940256099942544~3347511713" "" "https://cdn.example.vn/"
release_fails_with "Google test app id" "mã thử của Google"
write_release_properties "ca-app-pub-1234567890123456/1111111111" "" "https://cdn.example.vn/"
release_fails_with "ad unit given as app id" "admobAppId phải có dạng"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "sample_bottom_banner=ca-app-pub-3940256099942544/9214589741" "https://cdn.example.vn/"
release_fails_with "Google test ad unit" "mã quảng cáo thử"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "sample_bottom_banner" "https://cdn.example.vn/"
release_fails_with "malformed ad unit entry" "mục adUnits"
write_release_properties "ca-app-pub-1234567890123456~1111111111" "" "http://cdn.example.vn/"
release_fails_with "assets address without https" "assetsBaseUrl phải"
release_fails_with "unknown argument" "tham số không hợp lệ" --upload

# new-app.sh renames the template into a new repository and leaves out the template's own files.
new_app="$work/new-app"
# CI runners have no git identity; the new repository needs one for its first commit.
if GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com \
  scripts/new-app.sh --dest "$new_app" --package com.example.notes --name "Ghi chú" \
  --staging-assets-url https://notes.example.vn/staging/ > /dev/null 2>&1; then
  expect "new-app.sh succeeds" ok ok
  expect "new-app.sh leaves out the roadmap" absent "$([ -e "$new_app/TEMPLATE-ROADMAP.md" ] && echo present || echo absent)"
  expect "new-app.sh leaves out itself" absent "$([ -e "$new_app/scripts/new-app.sh" ] && echo present || echo absent)"
  expect "new-app.sh replaces the package outside ADRs" "" \
    "$(grep -rlIF com.vglstudio.template "$new_app" --exclude-dir=.git --exclude-dir=adr || true)"
  expect "new-app.sh moves sources" yes \
    "$([ -f "$new_app/app/src/main/kotlin/com/example/notes/MainActivity.kt" ] && echo yes || echo no)"
  expect "new-app.sh sets the app name" 1 "$(grep -c '>Ghi chú</string>' "$new_app/app/src/main/res/values/strings.xml")"
  expect "new-app.sh sets the staging id of staging.yml" 1 \
    "$(grep -c 'STAGING_APPLICATION_ID: com.example.notes.staging$' "$new_app/.github/workflows/staging.yml")"
  expect "new-app.sh drops links to template-only files" 0 \
    "$(grep -cE 'TEMPLATE-ROADMAP|ops/new-app\.md' "$new_app/README.md" || true)"
  expect "new-app.sh leaves out the from-app issue form" absent \
    "$([ -e "$new_app/.github/ISSUE_TEMPLATE/from-app.yml" ] && echo present || echo absent)"
  expect "new-app.sh starts an empty lessons log" 0 "$(grep -c '^### L-' "$new_app/docs/lessons.md" || true)"
  expect "new-app.sh commits everything" "" "$(git -C "$new_app" status --porcelain)"
else
  expect "new-app.sh succeeds" ok failed
fi
for args in "--package com.vglstudio.template.notes" "--package Notes" "--staging-assets-url https://app-project-template.vgl.io.vn/"; do
  # shellcheck disable=SC2086
  if scripts/new-app.sh --dest "$work/rejected" --package com.example.notes --name "Ghi chú" \
    --staging-assets-url https://notes.example.vn/ $args > /dev/null 2>&1; then
    expect "new-app.sh rejects '$args'" rejected accepted
  else
    expect "new-app.sh rejects '$args'" rejected rejected
  fi
done

# cloud-env-check.sh passes only when the environment was built from the current cloud-setup.sh.
setup_version="$(sed -n 's/^SETUP_VERSION=\([0-9][0-9]*\)$/\1/p' scripts/cloud-setup.sh)"
mkdir -p "$work/gradle-home/init.d"
touch "$work/gradle-home/init.d/maven-central-mirror.gradle"
env_check() {
  echo "$1" > "$work/cloud-setup-version"
  if CLOUD_SETUP_VERSION_FILE="$work/cloud-setup-version" GRADLE_USER_HOME="$2" \
    scripts/cloud-env-check.sh > /dev/null 2>&1; then echo pass; else echo stop; fi
}
expect "cloud-setup.sh has a version" yes "$([[ -n "$setup_version" ]] && echo yes || echo no)"
expect "cloud-env-check.sh passes on the current version" pass "$(env_check "$setup_version" "$work/gradle-home")"
expect "cloud-env-check.sh stops on an older version" stop "$(env_check 1 "$work/gradle-home")"
expect "cloud-env-check.sh stops without the mirror init script" stop \
  "$(env_check "$setup_version" "$work/no-gradle-home")"

# plan-check.py finds, from the plan alone, what would otherwise stop a task during the night (L-020).
repo="$work/plan-repo"
mkdir -p "$repo/scripts" "$repo/docs/data" "$repo/docs/features/demo/plans" "$repo/docs/features/demo/tickets" \
  "$repo/docs/features/demo/design/screens" "$repo/feature/demo/src/test/screenshots" "$repo/core/x"
echo '{}' > "$repo/docs/data/values.json"
echo 'open("docs/data/values.json")' > "$repo/scripts/reader.py"
touch "$repo/feature/demo/A.kt" "$repo/core/x/B.kt" "$repo/docs/features/demo/design/screens/DemoScreen_main_light.png"
write_plan() { # <file> <risk> <status> <rows> [extra text]
  printf -- '---\nticket: %s\nstatus: %s\nrisk_level: %s\nreviewer: A\ncross_reviewer:\napproved_at: 2026-10-17\n---\n\n### 3.2. Danh sách file\n\n| Thao tác | Đường dẫn | Nội dung |\n| --- | --- | --- |\n%b\n\n%s\n\n## 4. Kiểm thử\n' \
    "$(basename "$1" | cut -d- -f1-2)" "$3" "$2" "$4" "${5:-}" > "$1"
}
plan_check() { if PLAN_CHECK_ROOT="$repo" python3 -I scripts/plan-check.py "$1" > "$work/plan-check.out" 2>&1; then echo pass; else echo stop; fi; }
# One row of the file list of a plan; the backticks are Markdown, not command substitution.
# shellcheck disable=SC2016
row() { printf '| %s | `%s` | x |' "$1" "$2"; }
plans="$repo/docs/features/demo/plans"
write_plan "$plans/T-901-a.md" medium draft "$(row Sửa feature/demo/A.kt)"
expect "plan-check: a correct plan passes" pass "$(plan_check "$plans/T-901-a.md")"
write_plan "$plans/T-902-a.md" medium draft "$(row Sửa feature/demo/Missing.kt)"
expect "plan-check: changing a missing file stops" stop "$(plan_check "$plans/T-902-a.md")"
write_plan "$plans/T-903-a.md" medium draft "$(row Sửa docs/data/values.json)"
expect "plan-check: a script reading a changed file stops" stop "$(plan_check "$plans/T-903-a.md")"
expect "plan-check: names the reader" 1 "$(grep -c 'scripts/reader.py nhắc tới docs/data/values.json' "$work/plan-check.out")"
write_plan "$plans/T-904-a.md" medium draft "$(row Sửa docs/data/values.json)" 'scripts/reader.py chỉ đọc khóa a, không đổi.'
expect "plan-check: a reader the plan explains passes" pass "$(plan_check "$plans/T-904-a.md")"
write_plan "$plans/T-905-a.md" medium draft "$(row Sửa core/x/B.kt)"
expect "plan-check: core module needs a high-risk plan" stop "$(plan_check "$plans/T-905-a.md")"
write_plan "$plans/T-906-a.md" high approved "$(row Sửa core/x/B.kt)"
expect "plan-check: approved high-risk plan needs a cross reviewer" stop "$(plan_check "$plans/T-906-a.md")"
write_plan "$plans/T-907-a.md" medium draft "$(row Sửa feature/demo/A.kt)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image without screenshot stops" stop "$(plan_check "$plans/T-907-a.md")"
write_plan "$plans/T-908-a.md" medium draft \
  "$(row Tạo feature/demo/src/test/screenshots/DemoScreen_main_light.png)" 'Chạy scripts/design-compare.sh demo.'
expect "plan-check: design image with its screenshot passes" pass "$(plan_check "$plans/T-908-a.md")"
write_plan "$plans/T-909-a.md" medium draft "$(row Tạo feature/demo/New.kt)"
printf -- '---\nid: T-910\ndepends_on: [T-909]\n---\n' > "$repo/docs/features/demo/tickets/T-910-a.md"
write_plan "$plans/T-910-a.md" medium draft "$(row Sửa feature/demo/New.kt)"
expect "plan-check: a file created by a dependency passes" pass "$(plan_check "$plans/T-910-a.md")"
write_plan "$plans/T-911-a.md" high draft "$(row Sửa scripts/release.sh)"
expect "plan-check: release.sh is never allowed" stop "$(plan_check "$plans/T-911-a.md")"

# env-canary.sh opens an env-broken issue when the environment is red and closes it when it is green again.
printf '#!/usr/bin/env bash\nexit 0\n' > "$work/pass.sh"
printf '#!/usr/bin/env bash\necho "Could not GET https://repo.maven.apache.org/x"\nexit 1\n' > "$work/fail.sh"
chmod +x "$work/pass.sh" "$work/fail.sh"
canary() { # <env check> <check> <open issues JSON>; prints the writes, one per line
  echo "{\"routes\": {\"issues?state=open&labels=env-broken&per_page=10\": $3}}" > "$work/canary.json"
  rm -f "$work/canary.json.writes"
  PATH="$work/bin:$PATH" FIXTURE="$work/canary.json" CANARY_CHECKOUT=no RETRY_WAIT_SECONDS=0 \
    ENV_CHECK_CMD="$1" CHECK_CMD="$2" scripts/env-canary.sh > /dev/null 2>&1 || true
  cat "$work/canary.json.writes" 2>/dev/null | tr '\n' ';'
}
expect "env-canary: red opens an env-broken issue" "POST issues;" "$(canary "$work/pass.sh" "$work/fail.sh" '[]')"
expect "env-canary: red again comments on the open issue" "POST issues/7/comments;" \
  "$(canary "$work/pass.sh" "$work/fail.sh" '[{"number": 7}]')"
expect "env-canary: old setup script is red" "POST issues;" "$(canary "$work/fail.sh" "$work/pass.sh" '[]')"
expect "env-canary: green closes the open issue" "POST issues/7/comments;PATCH issues/7;" \
  "$(canary "$work/pass.sh" "$work/pass.sh" '[{"number": 7}]')"
expect "env-canary: green with nothing open changes nothing" "" "$(canary "$work/pass.sh" "$work/pass.sh" '[]')"

if [[ "$failures" -gt 0 ]]; then
  echo "$failures script test(s) failed"
  exit 1
fi
echo "All script tests passed"
