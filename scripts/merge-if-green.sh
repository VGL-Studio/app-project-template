#!/usr/bin/env bash
# Merge một PR vào nhánh chính, chỉ khi qua đủ cổng chất lượng (ADR-0010).
# Đây là cách DUY NHẤT được phép merge vào nhánh chính, cho cả người và AI.
#
# Cách dùng:   scripts/merge-if-green.sh <số PR>
# Mã thoát:    0 đã merge
#              1 từ chối (thiếu điều kiện, xem thông báo)
#              2 kiểm tra còn đang chạy, gọi lại sau
#              3 nhánh chưa cập nhật theo nhánh chính, cần cập nhật rồi chờ CI chạy lại
#
# Chỉ dùng REST API của GitHub qua `gh api`.
# Thay đổi file này là thay đổi cổng chất lượng: plan rủi ro cao, bắt buộc duyệt chéo.

set -euo pipefail

BASE_BRANCH="main"
# Kiểm tra bắt buộc phải xanh. Tên trùng với tên job trong .github/workflows/pr-check.yml.
REQUIRED_CHECKS=("changes" "build-and-test")
# Kiểm tra được phép ở trạng thái bỏ qua (khi PR không chạm vào giao diện).
SKIPPABLE_CHECKS=("ui-test")

die()  { echo "TỪ CHỐI: $*" >&2; exit 1; }
wait_() { echo "CHƯA XONG: $*" >&2; exit 2; }

[[ $# -eq 1 && "$1" =~ ^[0-9]+$ ]] || die "cần đúng một tham số là số PR."
PR="$1"

command -v gh >/dev/null || die "không tìm thấy gh."
command -v jq >/dev/null || die "không tìm thấy jq."

# Lấy owner/repo từ remote origin.
REMOTE_URL="$(git config --get remote.origin.url)"
REPO="$(echo "$REMOTE_URL" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
[[ "$REPO" =~ ^[^/]+/[^/]+$ ]] || die "không xác định được repo từ remote: $REMOTE_URL"

PR_JSON="$(gh api "repos/$REPO/pulls/$PR")"
STATE="$(jq -r .state <<<"$PR_JSON")"
DRAFT="$(jq -r .draft <<<"$PR_JSON")"
BASE="$(jq -r .base.ref <<<"$PR_JSON")"
HEAD_SHA="$(jq -r .head.sha <<<"$PR_JSON")"
HEAD_REF="$(jq -r .head.ref <<<"$PR_JSON")"
TITLE="$(jq -r .title <<<"$PR_JSON")"

[[ "$STATE" == "open" ]] || die "PR #$PR không ở trạng thái mở ($STATE)."
[[ "$DRAFT" == "false" ]] || die "PR #$PR đang là bản nháp."
[[ "$BASE" == "$BASE_BRANCH" ]] || die "PR #$PR nhắm vào '$BASE', không phải '$BASE_BRANCH'."

# Nhánh chính đang có sự cố thì không merge thêm gì, trừ chính PR revert.
BROKEN="$(gh api "repos/$REPO/issues?state=open&labels=main-broken&per_page=10" --jq '[.[] | select(.pull_request != null) | .number] | join(",")')"
if [[ -n "$BROKEN" && ",$BROKEN," != *",$PR,"* ]]; then
  die "nhánh chính đang đỏ (PR mang nhãn main-broken: #$BROKEN). Xử lý xong rồi mới merge tiếp."
fi

# Nhánh của PR phải chứa commit mới nhất của nhánh chính.
BEHIND_BY="$(gh api "repos/$REPO/compare/$BASE_BRANCH...$HEAD_SHA" --jq .behind_by)"
if [[ "$BEHIND_BY" != "0" ]]; then
  echo "CẦN CẬP NHẬT: nhánh '$HEAD_REF' chậm $BEHIND_BY commit so với '$BASE_BRANCH'. Cập nhật nhánh, chờ CI chạy lại rồi gọi lại script." >&2
  exit 3
fi

# Kết quả các kiểm tra trên đúng commit cuối của PR.
RUNS="$(gh api "repos/$REPO/commits/$HEAD_SHA/check-runs?per_page=100" --jq '.check_runs')"

# Không chấp nhận bất kỳ kiểm tra nào thất bại, kể cả kiểm tra không có trong danh sách.
FAILED="$(jq -r '[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")' <<<"$RUNS")"
[[ -z "$FAILED" ]] || die "có kiểm tra không qua: $FAILED."

check_one() {
  local name="$1" allow_skip="$2" latest status conclusion
  latest="$(jq -c --arg n "$name" '[.[] | select(.name==$n)] | sort_by(.started_at) | last // empty' <<<"$RUNS")"
  [[ -n "$latest" ]] || wait_ "chưa thấy kiểm tra '$name' trên commit ${HEAD_SHA:0:7}."
  status="$(jq -r .status <<<"$latest")"
  conclusion="$(jq -r .conclusion <<<"$latest")"
  [[ "$status" == "completed" ]] || wait_ "kiểm tra '$name' đang ở trạng thái $status."
  if [[ "$conclusion" == "success" ]]; then return 0; fi
  if [[ "$allow_skip" == "yes" && "$conclusion" == "skipped" ]]; then return 0; fi
  die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."
}

for c in "${REQUIRED_CHECKS[@]}"; do check_one "$c" no; done
for c in "${SKIPPABLE_CHECKS[@]}"; do check_one "$c" yes; done

# Merge gộp một commit, kèm mã commit đã kiểm tra: GitHub từ chối nếu PR có commit mới.
gh api -X PUT "repos/$REPO/pulls/$PR/merge" \
  -f merge_method=squash \
  -f sha="$HEAD_SHA" \
  -f commit_title="$TITLE (#$PR)" >/dev/null \
  || die "GitHub từ chối merge PR #$PR."

# Xóa nhánh đã merge nếu đó là nhánh của phiên tự chạy.
if [[ "$HEAD_REF" == claude/* ]]; then
  gh api -X DELETE "repos/$REPO/git/refs/heads/$HEAD_REF" >/dev/null 2>&1 || true
fi

echo "ĐÃ MERGE: PR #$PR ($TITLE) tại commit ${HEAD_SHA:0:7}."
