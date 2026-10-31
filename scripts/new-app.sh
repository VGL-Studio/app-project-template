#!/usr/bin/env bash
# Creates a new app from this template (ADR-0017), as a new git repository in its own folder.
#
# Usage:  scripts/new-app.sh --dest <folder> --package <package> --name "<app name>" --staging-assets-url <url>
#   --dest                folder of the new app; must not exist yet. Its name becomes the Gradle project name.
#   --package             package name and applicationId, e.g. com.vglstudio.notes
#   --name                app name shown to users
#   --staging-assets-url  https address of the app's staging folder on R2, ending with '/'
#
# Only committed files of the template are copied (git archive), so build outputs and local secrets never
# reach the new app. The template-only scripts (this one, template-sync.sh) and doc docs/ops/new-app.md are left out.
# The app records the template commit it was made from in docs/template-sync.txt, the base of its first sync (ADR-0030).
# :feature:sample is kept: CLAUDE.md tells every feature to follow it.
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail
cd "$(dirname "$0")/.."

die() { echo "LỖI: $*" >&2; exit 1; }

TEMPLATE_PACKAGE="com.vglstudio.template"
TEMPLATE_STAGING_ASSETS_URL="https://app-project-template.vgl.io.vn/"

dest=""
package=""
name=""
assets_url=""
while [ "$#" -gt 0 ]; do
  [ "$#" -ge 2 ] || die "thiếu giá trị cho $1."
  case "$1" in
    --dest) dest="$2" ;;
    --package) package="$2" ;;
    --name) name="$2" ;;
    --staging-assets-url) assets_url="$2" ;;
    *) die "tham số không hợp lệ: '$1'. Xem cách dùng ở đầu scripts/new-app.sh." ;;
  esac
  shift 2
done

[ -n "$dest" ] && [ -n "$package" ] && [ -n "$name" ] && [ -n "$assets_url" ] ||
  die "cần đủ --dest, --package, --name và --staging-assets-url."
[[ "$package" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] ||
  die "tên gói '$package' không hợp lệ. Dùng chữ thường, số, dấu gạch dưới, ví dụ com.vglstudio.notes."
[ "$package" != "$TEMPLATE_PACKAGE" ] || die "tên gói phải khác tên gói của dự án mẫu."
case "$package" in "$TEMPLATE_PACKAGE".*) die "tên gói không được nằm trong $TEMPLATE_PACKAGE." ;; esac
[[ "$name" != *[\<\>\&\"\\]* ]] || die "tên app không được chứa < > & \" hay \\."
[[ "$assets_url" =~ ^https://[^/]+/.*$ ]] && [[ "$assets_url" == */ ]] ||
  die "địa chỉ R2 staging phải là https và kết thúc bằng '/' (ADR-0015)."
[ "$assets_url" != "$TEMPLATE_STAGING_ASSETS_URL" ] || die "mỗi app cần thư mục staging riêng trên R2."
[ ! -e "$dest" ] || die "$dest đã tồn tại."
project_name="$(basename "$dest")"
[[ "$project_name" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]] ||
  die "tên thư mục '$project_name' chỉ được chứa chữ, số, dấu chấm, gạch ngang, gạch dưới."
[ -z "$(git status --porcelain --untracked-files=no)" ] ||
  echo "CHÚ Ý: dự án mẫu có thay đổi chưa commit; chỉ phần đã commit được chép."

template_commit="$(git rev-parse --short HEAD)"
template_commit_full="$(git rev-parse HEAD)"
today="$(date +%Y-%m-%d)"

echo "== Chép dự án mẫu (${template_commit}) sang $dest"
mkdir -p "$dest"
git archive --format=tar HEAD | tar -x -C "$dest"
cd "$dest"
rm -f scripts/new-app.sh scripts/template-sync.sh docs/ops/new-app.md .github/ISSUE_TEMPLATE/from-app.yml
rmdir .github/ISSUE_TEMPLATE 2> /dev/null || true

# Replaces text in every file that contains it; fails when nothing matched, so a renamed template file is noticed.
replace_in() {
  local file="$1" from="$2" to="$3"
  grep -qF -- "$from" "$file" || die "không thấy '$from' trong $file. Script cần cập nhật theo dự án mẫu."
  FROM="$from" TO="$to" perl -0pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$file"
}

echo "== Đổi tên gói thành $package"
package_path="${package//.//}"
template_path="${TEMPLATE_PACKAGE//.//}"
find . -type d -path "*/src/*/kotlin/$template_path" | while read -r dir; do
  root="${dir%/"$template_path"}"
  mv "$dir" "$root/.package"
  rmdir -p "$root/${template_path%/*}" 2> /dev/null || true
  mkdir -p "$(dirname "$root/$package_path")"
  mv "$root/.package" "$root/$package_path"
done
# Accepted ADRs are kept word for word (docs/CLAUDE.md); they describe the template's decisions.
grep -rlIF -e "$TEMPLATE_PACKAGE" -e "$template_path" . --exclude-dir=.git --exclude-dir=adr |
  while read -r file; do
    FROM="$TEMPLATE_PACKAGE" TO="$package" perl -pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$file"
    FROM="$template_path" TO="$package_path" perl -pi -e 's/\Q$ENV{FROM}\E/$ENV{TO}/g' "$file"
  done

echo "== Đặt tên app, tên project và địa chỉ R2 staging"
# Android resources need apostrophes escaped.
replace_in app/src/main/res/values/strings.xml '<string name="app_name">App Template</string>' \
  "<string name=\"app_name\">${name//\'/\\\'}</string>"
replace_in settings.gradle.kts 'rootProject.name = "app-project-template"' "rootProject.name = \"$project_name\""
replace_in app/build.gradle.kts "$TEMPLATE_STAGING_ASSETS_URL" "$assets_url"
title="$(perl -CSA -e 'print uc $ARGV[0]' "$name")"
replace_in README.md '# APP PROJECT TEMPLATE' "# $title"
replace_in README.md 'Dự án mẫu cho các app Android của nhóm. Mọi app mới được tạo từ dự án này,' \
  "App $name, tạo từ dự án mẫu của nhóm ngày $today (commit $template_commit),"
replace_in README.md '| Tạo app mới từ dự án mẫu | [docs/ops/new-app.md](docs/ops/new-app.md) |
' ''
replace_in CLAUDE.md 'Đây là dự án mẫu cho các app Android của nhóm. Mọi app mới được tạo từ dự án này.' \
  "Đây là app $name, tạo từ dự án mẫu của nhóm."

# The new app starts its own lessons log; the template's lessons stay in the template repo.
grep -qxF '<!-- Các bài học ghi dưới dòng này. -->' docs/lessons.md ||
  die "không thấy dòng đánh dấu trong docs/lessons.md. Script cần cập nhật theo dự án mẫu."
LESSONS_NOTE="Bài học trước ngày $today nằm trong sổ bài học của repo dự án mẫu (commit $template_commit)." \
  perl -0pi -e 's/(<!-- Các bài học ghi dưới dòng này\. -->\n).*\z/$1\n$ENV{LESSONS_NOTE}\n/s' docs/lessons.md

# The new app starts its own run history: the template's reports would list the template's unchecked manual checks
# as the app's release blockers (status.py, section 6). They stay in the template repo.
find docs/runs -name 'RUN-*.md' -delete

# The new app numbers its own tickets from T-001 (docs/CLAUDE.md takes the next number over every features/*/tickets/,
# L-141). Specs and designs of the modules it ships stay, since their code points to them; the template's tickets, plans,
# feedback, requests and its process feature stay in the template repo.
rm -rf docs/features/quy-trinh
find docs/features -mindepth 2 -maxdepth 2 -type d \( -name tickets -o -name plans -o -name feedback -o -name requests \) \
  -exec rm -rf {} +

# Pages generated from the documents must describe the new app, not the template (its lessons, for one).
python3 -I scripts/status.py > /dev/null
python3 -I scripts/foundation.py > /dev/null

# The base of the first sync with the template (scripts/template-sync.sh of the template, docs/ops/to-app.md).
printf 'base=%s\npackage=%s\n' "$template_commit_full" "$package" > docs/template-sync.txt

left="$(grep -rlIF "$TEMPLATE_PACKAGE" . --exclude-dir=.git --exclude-dir=adr || true)"
[ -z "$left" ] || die "còn tên gói cũ trong: $left"

echo "== Tạo repo git"
git init --quiet -b main
git add -A
git commit --quiet -m "Khởi tạo $name từ dự án mẫu ($template_commit)"

cat << EOF
== Xong: $dest
Việc tiếp theo:
   Làm theo docs/ops/setup-new-app.md, từ phần 2 (package staging: $package.staging).
   Trước hết chạy scripts/check.sh trong thư mục app mới để chắc app build được.
EOF
