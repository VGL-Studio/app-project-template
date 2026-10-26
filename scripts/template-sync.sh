#!/usr/bin/env bash
# Brings the changes of this template into an app made from it (ADR-0030, docs/ops/to-app.md).
#
# Usage, from the template repository:
#   scripts/template-sync.sh --app <app folder> [--to <template commit>]   every change since the app's base
#   scripts/template-sync.sh --app <app folder> --take <path>              a held path, up to the app's base
#
# The app records its sync state in docs/template-sync.txt (new-app.sh writes it):
#   base=<template commit the app is synced to>
#   package=<the app's package>
#   hold=<path> <template commit>    a path, named as in the template, kept at an older template commit
#
# How it works: the template at the two commits is exported, renamed to the app's package like new-app.sh does, and
# committed twice in a scratch repository. The second commit is cherry-picked onto a new branch of the app, so git
# merges only the template's own changes into the app's files, three-way, and marks a conflict where the app changed
# the same lines. Nothing is pushed. The template's history and pages generated from it are never synced.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail
cd "$(dirname "$0")/.."
template="$(pwd -P)"

die() { echo "LỖI: $*" >&2; exit 1; }

TEMPLATE_PACKAGE="com.vglstudio.template"
SYNC_FILE="docs/template-sync.txt"
# The template's own history and tools, the app's own data, and files generated from the app's own documents or code.
# A generated page is rebuilt in the app after the sync instead.
NEVER_SYNCED=(
  scripts/new-app.sh scripts/template-sync.sh docs/ops/new-app.md .github/ISSUE_TEMPLATE
  docs/lessons.md docs/runs docs/features docs/metrics docs/inbox docs/test-sessions docs/design-system/tokens.json
  docs/ops/required-assets.txt docs/foundation.md docs/STATUS.md docs/remote-config-keys.md
  docs/remote-config-template.json
)
# Parts the app does not change (docs/ops/to-app.md, section 2): on a conflict the template's side is usually right.
# AdPlacement.kt lists the app's own placements, so it is shared even inside :core:ads.
TEMPLATE_OWNED='^(scripts/|\.github/|build-logic/|docs/auto-run\.md|docs/risk-scoring\.md|docs/templates/|docs/ops/|docs/adr/0|core/(logging|network|assets|ads)/)'
APP_SHARED_INSIDE_OWNED='/AdPlacement\.kt$'

app=""
to=""
take=""
while [ "$#" -gt 0 ]; do
  [ "$#" -ge 2 ] || die "thiếu giá trị cho $1."
  case "$1" in
    --app) app="$2" ;;
    --to) to="$2" ;;
    --take) take="${2%/}" ;;
    *) die "tham số không hợp lệ: '$1'. Xem cách dùng ở đầu scripts/template-sync.sh." ;;
  esac
  shift 2
done
[ -n "$app" ] || die "cần --app <thư mục app>."
[ -z "$take" ] || [ -z "$to" ] || die "--take không đi cùng --to: phần giữ lại được đưa lên đúng mốc base của app."
app="$(cd "$app" 2> /dev/null && pwd -P)" || die "không có thư mục app '$app'."
[ "$(git -C "$app" rev-parse --show-toplevel 2> /dev/null)" = "$app" ] || die "$app không phải gốc của một repo git."
[ "$app" != "$template" ] || die "--app phải là repo của app, không phải dự án mẫu."
[ -z "$(git -C "$app" status --porcelain)" ] || die "app có thay đổi chưa commit. Commit hoặc cất đi trước khi đồng bộ."
[ -f "$app/$SYNC_FILE" ] || die "app chưa có $SYNC_FILE. Tạo file theo docs/ops/to-app.md, mục 3."

# Reads one key of the sync file; hold lines are read separately.
sync_value() { sed -n "s/^$1=//p" "$app/$SYNC_FILE" | head -1; }
commit_of() { git rev-parse --verify --quiet "$1^{commit}" || die "dự án mẫu không có commit '$1'. Chạy git fetch trong dự án mẫu."; }

base="$(commit_of "$(sync_value base)")"
package="$(sync_value package)"
[[ "$package" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || die "dòng package= trong $SYNC_FILE không hợp lệ: '$package'."
holds="$(sed -n 's/^hold=//p' "$app/$SYNC_FILE")"
hold_paths=()
while read -r path commit; do
  [ -n "$path" ] || continue
  [ -n "$commit" ] || die "dòng hold=$path thiếu commit của dự án mẫu."
  commit_of "$commit" > /dev/null
  hold_paths+=("${path%/}")
done <<< "$holds"

if [ -n "$take" ]; then
  held_at="$(awk -v p="$take" '{ sub(/\/$/, "", $1) } $1 == p { print $2 }' <<< "$holds" | head -1)"
  [ -n "$held_at" ] || die "không có dòng hold=$take trong $SYNC_FILE."
  from="$(commit_of "$held_at")"
  target="$base"
  label="$take từ ${from:0:7} lên ${target:0:7}"
  branch="template-sync/take-${take//\//-}-${target:0:7}"
else
  if [ -z "$to" ]; then
    git fetch --quiet origin main 2> /dev/null || echo "CHÚ Ý: không lấy được origin/main mới nhất; dùng bản đã có trên máy."
    to="origin/main"
  fi
  from="$base"
  target="$(commit_of "$to")"
  if ! git merge-base --is-ancestor "$from" "$target"; then
    # An app made from a branch commit that was merged later: the commit of the target's history with the same files.
    same="$(git log --format='%H %T' "$target" | awk -v t="$(git rev-parse "$from^{tree}")" '!found && $2 == t { print $1; found = 1 }')"
    [ -n "$same" ] || die "mốc base ${from:0:7} của app không nằm trước ${target:0:7}, cũng không có commit nào cùng nội dung."
    echo "CHÚ Ý: ${from:0:7} không nằm trên lịch sử của ${target:0:7}; dùng commit cùng nội dung ${same:0:7}."
    from="$same"
  fi
  label="${from:0:7}..${target:0:7}"
  branch="template-sync/${target:0:7}"
fi
[ "$from" != "$target" ] || { echo "Không có gì mới: app đã ở mốc ${target:0:7} của dự án mẫu."; exit 0; }
! git -C "$app" rev-parse --verify --quiet "refs/heads/$branch" > /dev/null || die "app đã có nhánh $branch."

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# Writes the template at commit $1 into folder $2, renamed to the app's package, with only the synced paths.
export_tree() {
  local commit="$1" dir="$2" path
  mkdir -p "$dir"
  if [ -n "$take" ]; then
    # A held path may not exist yet at the older commit: then everything in it is new.
    if [ -n "$(git ls-tree --name-only "$commit" -- "$take")" ]; then
      git archive --format=tar "$commit" -- "$take" | tar -x -C "$dir"
    fi
  else
    git archive --format=tar "$commit" | tar -x -C "$dir"
    for path in "${hold_paths[@]+"${hold_paths[@]}"}"; do rm -rf "${dir:?}/$path"; done
  fi
  for path in "${NEVER_SYNCED[@]}"; do rm -rf "${dir:?}/$path"; done

  # The same renaming as new-app.sh: source folders, then the package in text outside the ADRs.
  local package_path="${package//.//}" template_path="${TEMPLATE_PACKAGE//.//}" found root
  find "$dir" -type d -path "*/src/*/kotlin/$template_path" | while read -r found; do
    root="${found%/"$template_path"}"
    mv "$found" "$root/.package"
    rmdir -p "$root/${template_path%/*}" 2> /dev/null || true
    mkdir -p "$(dirname "$root/$package_path")"
    mv "$root/.package" "$root/$package_path"
  done
  (grep -rlIF -e "$TEMPLATE_PACKAGE" -e "$template_path" "$dir" --exclude-dir=adr || true) | while read -r found; do
    FROM="$TEMPLATE_PACKAGE" TO="$package" perl -pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$found"
    FROM="$template_path" TO="$package_path" perl -pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$found"
  done
}

echo "== Dựng dự án mẫu ${from:0:7} và ${target:0:7} theo tên gói $package"
export_tree "$from" "$work/from"
export_tree "$target" "$work/to"
scratch=(git --git-dir="$work/sync.git" -c user.name=template-sync -c user.email=template-sync@localhost
  -c core.autocrlf=false -c commit.gpgsign=false)
git init --quiet --bare "$work/sync.git"
"${scratch[@]}" --work-tree="$work/from" add -A
"${scratch[@]}" --work-tree="$work/from" commit --quiet -m "template ${from:0:7}"
"${scratch[@]}" --work-tree="$work/to" add -A
message="Đồng bộ dự án mẫu: $label

Dựng bằng scripts/template-sync.sh của dự án mẫu (ADR-0030, docs/ops/to-app.md)."
"${scratch[@]}" --work-tree="$work/to" commit --quiet --allow-empty -m "$message"
"${scratch[@]}" update-ref refs/heads/sync HEAD
if [ "$("${scratch[@]}" rev-parse HEAD^{tree})" = "$("${scratch[@]}" rev-parse HEAD~1^{tree})" ]; then
  echo "Không có gì mới cho app: thay đổi của dự án mẫu từ ${from:0:7} chỉ nằm ở phần không đồng bộ."
  exit 0
fi

echo "== Gộp vào nhánh $branch của app"
git -C "$app" fetch --quiet --no-tags "$work/sync.git" sync
git -C "$app" switch --quiet -c "$branch"
conflict=0
git -C "$app" -c merge.renames=true cherry-pick --no-commit FETCH_HEAD > /dev/null 2>&1 || conflict=1

# The new base (or one hold line less) goes into the same commit.
if [ -n "$take" ]; then
  TAKE="$take" perl -ni -e 'print unless /^hold=\Q$ENV{TAKE}\E\/?\s/' "$app/$SYNC_FILE"
else
  TARGET="$target" perl -pi -e 's/^base=.*/base=$ENV{TARGET}/' "$app/$SYNC_FILE"
fi
git -C "$app" add -- "$SYNC_FILE"

conflicts="$(git -C "$app" diff --name-only --diff-filter=U)"
stat="$(git -C "$app" diff --cached --shortstat | sed 's/^ *//')"
if [ -z "$conflicts" ] && [ "$conflict" -eq 0 ]; then
  if [ -f "$app/scripts/foundation.py" ]; then
    (cd "$app" && python3 -I scripts/foundation.py > /dev/null) && git -C "$app" add -- docs/foundation.md
  fi
  printf '%s\n' "$message" > "$work/message"
  git -C "$app" commit --quiet -F "$work/message"
  git -C "$app" cherry-pick --quit 2> /dev/null || true
  cat << EOF
== Xong, không có xung đột: nhánh $branch của app ($stat).
Việc tiếp theo trong app:
   scripts/check.sh, rồi đẩy nhánh và mở PR như mọi thay đổi (docs/ops/to-app.md, mục 4).
EOF
  exit 0
fi

echo "== Có xung đột: nhánh $branch của app ($stat)"
owned="$(grep -E "$TEMPLATE_OWNED" <<< "$conflicts" | grep -vE "$APP_SHARED_INSIDE_OWNED" || true)"
shared="$conflicts"
# An empty pattern list would match every line, so filter only when some file is the template's.
[ -z "$owned" ] || shared="$(grep -vxF -f <(printf '%s\n' "$owned") <<< "$conflicts" || true)"
if [ -n "$owned" ]; then
  echo "Phần của dự án mẫu (thường lấy bản mẫu: git checkout --theirs -- <file>, sau khi chắc phần sửa của app đã về dự án mẫu):"
  sed 's/^/   /' <<< "$owned"
fi
if [ -n "$shared" ]; then
  echo "Phần app cũng sửa (gộp tay, giữ phần riêng của app):"
  sed 's/^/   /' <<< "$shared"
fi
cat << EOF
Việc tiếp theo trong app:
   1. Sửa xung đột, git add từng file đã sửa.
   2. python3 scripts/foundation.py, git add docs/foundation.md.
   3. git commit (lời commit đã soạn sẵn), scripts/check.sh, rồi đẩy nhánh và mở PR (docs/ops/to-app.md, mục 4).
EOF
exit 1
