---
ticket: T-077
title: Bớt lần chạy lại pr-check: PR nháp và nhánh chính chỉ đổi tài liệu
status: done
version: 2
risk_score: 9
risk_level: high
token_estimate: 24.1M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-11-08
lane:
---

# PLAN T-077: BỚT LẦN CHẠY LẠI PR-CHECK: PR NHÁP VÀ NHÁNH CHÍNH CHỈ ĐỔI TÀI LIỆU

## 1. Tóm tắt

`merge-if-green.sh` merge PR code mà không cập nhật nhánh khi nhánh chính, từ điểm tách, chỉ đổi tài liệu theo đúng quy tắc của `main-guard` (`paths-ignore` trừ `GRADLE_READ_DOCS`), không file nào đổi ở cả hai bên và PR không xung đột; quyết "chỉ tài liệu" và "PR đã chạy `check.sh`" bằng hai lệnh mới của `scripts/main-guard-verified.sh`, nên commit merge luôn được `main-guard` kiểm bằng cách `tests`. `pr-check.yml` không chạy cho PR nháp (`types` đủ bốn loại, `if:` cấp job), `wait-run.sh` báo ngay PR nháp. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả và repo git tạm, mỗi kiểm tra có sẵn của `merge-if-green.sh` một test (L-058). Theo ADR-0041 (`accepted` ngày 2026-10-30).

**Phiên bản 2 (2026-11-08).** Bản 1 duyệt ngày 2026-10-30 (anh Nhật), viết trước khi T-076 merge. Rà soát độc lập danh sách chạy RUN-20261101-02 (2026-11-01, AI đối chiếu lại trên nhánh chính) tìm ra hai điểm chặn, nên T-077 không chạy ở phiên đó:

1. **Mục 3.5 bỏ sót vòng `for lacking in '[]' "$mig_other_tree" "$mig_other_title"`** của khối `mig_run` (`scripts/tests/run.sh` dòng 1172 hôm nay). Với `mig_other_tree` (commit cùng tiêu đề `t (#12)` mà cây khác), `merged_on_main` mới đi bước 4: `git fetch` từ origin. `mig_run` chạy script ngay trong thư mục repo thật: trên máy dev và trên CI (checkout có remote thật), `remote.origin.url` của repo đứng trước URL giả của `run.sh`, nên lệnh fetch đi ra GitHub thật, lấy `refs/pull/12/head` của repo thật vào `refs/merge-if-green/pr` (AI đã thử đúng lệnh fetch đó trong worktree soạn plan ngày 2026-11-07: thoát 0, ghi ref; đã xóa ref). Rồi script dừng với câu "cùng tiêu đề ... người xem" ngay ở đầu `evaluate`, nên câu "GitHub từ chối merge PR #12" của vòng đó đỏ. Bản 2: `mig_run` chạy script trong một repo tạm không có remote, origin giả chỉ tới một đường dẫn cục bộ không có (fetch lỗi ngay, không ra mạng, không ghi ref vào repo thật), có test giữ điều này (test 30); ca `mig_other_tree` tách khỏi vòng, câu khẳng định mới viết sẵn ở mục 3.5 (R4); `merged_on_main` trả mã khác 0 cho ca "không quyết được" và nơi gọi tự `die`, không dựa vào `die` trong `$(...)`.
2. **ADR-0044 (merge sau khi bản 1 được duyệt):** câu chép vào tài liệu quy tắc không được mang mã `L-`, `T-`, `RUN-`. Bản 2 viết sẵn nguyên văn mọi câu thêm vào `docs/auto-run.md`, `CLAUDE.md`, `docs/ops/setup-new-app.md`, `.claude/skills/merge-pr/SKILL.md` (mục 3.1 E); AI đã áp các câu đó vào một bản sao của nhánh chính và chạy `scripts/history-refs.py`: sạch (đối chứng: thêm `L-080` vào một câu thì script báo lỗi).

Thay đổi khác so với bản 1:

3. **Viết trên trạng thái sau T-076** (nhánh chính 2fd2701, mục 2): bỏ phần giả định về T-075, T-076 và phương án dự phòng 2 của bản 1; diff của `pr-check.yml` viết trên workflow một job; số dòng của mọi file cập nhật. Các điều sửa mà ghi chú duyệt bản 1 giao cho phiên (mục 9, bản 1, (a) tới (e)) đưa vào thân plan: viết lại test M3 của T-076 cùng câu `skipped` (mục 3.5, R3), bản sao đối chứng của câu `skipped` thay đúng dòng `pending` mới (mục 3.5), `merged_on_main` kiểm merge-base và file PR cùng đổi (mục 3.1 B), regex lấy repo ở dòng 91 của `merge-if-green.sh` (test 27).
4. **`pr_sim` của T-076 hiểu `if:` cấp job.** Hàm hôm nay bỏ qua mọi dòng cấp job, nên `if:` mới không làm P6 đỏ, nhưng P6 cũng không thấy PR nháp. Bản 2: `pr_sim` chỉ nhận đúng điều kiện nháp ở cấp job, PR nháp mô phỏng ra không bước nào chạy; điều kiện cấp job khác in `ERROR điều kiện lạ cấp job` (test 28, 29). `pr_job_if` thay bằng `pr_job_if_draft` (mục 3.5, R1).
5. **`wait-run.sh` đã đổi từ bản 1** (`--sha`, đọc lại đầu PR mỗi vòng, đếm lần đọc lại khi bị hủy): kiểm nháp đặt trong vòng, ngay sau `read_head`; không thêm lần đọc `pulls/N` nào, chỉ đổi biểu thức `--jq` của hai lần đọc có sẵn, để dữ liệu `__sequence__` của các test có sẵn giữ đúng thứ tự.
6. **Cách chạy:** anh Nhật quyết ngày 2026-11-01 chạy T-077 trong phiên tự chạy (mục 2), thay câu "làm trong phiên có người" và "T-075, T-076 chưa merge: không bắt đầu" của bản 1 (mục 6, 7).

Sau rà soát độc lập PR #411 (0 chặn, 2 nên sửa, 6 ghi chú, đã sửa cả tám): PR chỉ đổi file mà `main-guard` bỏ qua (ví dụ `feature/x/README.md`, bộ lọc `code` vẫn chạy `check.sh`) luôn cập nhật nhánh (mục 3.1 B, bước 3; test 8); danh sách test đỏ trước code sửa lại (7, 25); câu kiểm hai dòng CHÚ Ý ở test 5; số câu `main-guard-verified:` là 40; bản sao đối chứng cho R4; `refuse_same_title` sau lệnh merge lỗi gọi `main-guard` trước khi từ chối; ghi chú về vòng đọc `mergeable` và `errexit`; mục 3.4 thành việc tiếp theo cho người.

Không đổi cách làm của bản 1: quy tắc merge không cập nhật, hai lệnh của `main-guard-verified.sh`, mã 6 của `wait-run.sh`, kiểm tra `skipped` là chờ, danh sách tài liệu, điểm rủi ro. Câu hỏi 1 tới 7 của bản 1 đã được trả lời (mục 9, bản 1) và giữ; mục 8 chỉ còn câu hỏi về phần đổi của bản 2.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 5. Quyết định của anh Nhật ngày 2026-10-29: áp dụng cả sáu cách. **Đổi 2026-11-01:** anh Nhật quyết chạy T-076, T-077, T-078 trong phiên tự chạy (lời anh: "Không, hãy chạy phiên"; chọn "Cả ba, nối tiếp"), T-077 chỉ với plan phiên bản 2 đã duyệt; ticket để trống `auto_run`.

| Yêu cầu (định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| DoD 1 | PR nháp không chạy `pr-check`; chuyển sang sẵn sàng thì chạy | `types: [opened, synchronize, reopened, ready_for_review]`, `if:` cấp job của `build-and-test` bỏ nháp. GitHub vẫn tạo một lần chạy có job `skipped` cho PR nháp; `wait-run.sh` không coi lần đó là kết quả |
| DoD 2 | Nhánh chính chỉ có thêm commit tài liệu: merge không cập nhật nhánh | PR code (bước cổng `check.sh` xanh), file nhánh chính đổi từ điểm tách đều là tài liệu, không trùng file của PR, `mergeable: true` → merge, không gọi `update-branch` |
| DoD 2 | Có commit code: vẫn cập nhật và chờ CI | Một file ngoài tài liệu, hay file `docs/` Gradle đọc → như cũ (mã 3, hay `--update`) |
| DoD 2 | PR chỉ có tài liệu: luôn cập nhật | Bước cổng `check.sh` của PR `skipped` → như cũ |
| DoD 2 | Xung đột thì cập nhật | `mergeable` `false` → như cũ (`update-branch` báo xung đột như nay); `null` (GitHub chưa tính xong) → đọc lại có giới hạn, rồi chờ (mã 2); chỉ `true` mới không cập nhật |
| Ticket, phạm vi 3 | `main-guard` của T-074 khớp với quy tắc mới | `merge-if-green.sh` dùng chính hàm của `main-guard-verified.sh`; test đối chiếu hai bên trên cùng bảng đường dẫn và trên một commit merge dựng thật |
| DoD 3 | ADR cập nhật, `foundation.md` sinh lại | ADR-0041 đã `accepted` (2026-10-30), `foundation.md` đã có vế của nó (`scripts/foundation.py --check` khớp ngày 2026-11-07). Task thêm ghi chú ở ADR-0010, ADR-0039; sinh lại `foundation.md` chỉ khi dòng quy tắc của ADR-0041 phải sửa |
| DoD 4 | CI qua, đã merge | Như mọi task |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0041 (quyết định của task), ADR-0010 (mục 1 bị thay một phần, mục 4 thêm), ADR-0039 (giả định ở mục Hệ quả bị bỏ; cách `tests` thành đường thường), ADR-0025 (`main-broken`), ADR-0029 (phạm vi file), ADR-0031 (viết lại câu khẳng định cũ), ADR-0043 (`ui-test` sau merge), ADR-0044 (tài liệu quy tắc không ghi mã lịch sử).

**Trạng thái nhánh chính mà plan dựa vào** (đọc ngày 2026-11-07, commit 2fd2701, sau T-076 ở d83c392):

- `.github/workflows/pr-check.yml`: một job `build-and-test` (dòng 31), `runs-on: ubuntu-latest` (32), `timeout-minutes: 45` (33), không có `if:` cấp job; `on.pull_request` chỉ có `branches: [main]` (dòng 14 tới 15). Bước đầu `Lọc đường dẫn (dorny/paths-filter)` (`id: filter`), rồi `Kiểm kết quả lọc đường dẫn`, `actions/checkout@v5`, `actions/setup-java@v5`; sáu bước đọc `steps.filter.outputs`. Chú thích dòng 9 có câu "The job is never skipped."
- `scripts/merge-if-green.sh`: `REQUIRED_CHECKS=("build-and-test")` (dòng 30), không còn `SKIPPABLE_CHECKS`; `merged_on_main` dòng 43 tới 54; regex lấy repo dòng 91; `check_one` dòng 96 tới 123 (dòng 122: `die "kiểm tra '$name' kết thúc với kết quả '$conclusion'."`); `evaluate` dòng 126 tới 218, khối `behind_by` dòng 199 tới 205; vòng chính dòng 224 tới 257; vòng tìm sau khi merge báo lỗi dòng 296 tới 305.
- `scripts/main-guard-verified.sh`: chú thích dòng 24 ("The trees match because merge-if-green.sh merges only a PR that is not behind main, pinned to the checked sha."); kiểm tham số dòng 48; `REPO` dòng 53 tới 57; đọc lần chạy và bước cổng dòng 82 tới 92; vòng tài liệu dòng 107 tới 112. `GATE_FILES` có thêm `scripts/ui-test-needed.sh` (T-075).
- `scripts/wait-run.sh`: mã 0 tới 5 (chú thích dòng 13 tới 19); `read_head` dòng 67 tới 73 đọc `[.head.sha, (.mergeable_state // "")]`; lần đọc đầu dòng 75 (`--jq .head.sha`); vòng chờ dòng 92 tới 143.
- `scripts/tests/run.sh`: chú thích khối `pr-check` dòng 268 tới 269; `pr_job_if` dòng 304; câu `pr-check: build-and-test has no job-level if` dòng 333 và bản sao `job-if.yml` dòng 334 tới 336; `pr_sim` dòng 393 tới 466 (bỏ qua dòng cấp job); `pr_sim_summary` dòng 467; khối `main-guard-verified` dòng 563 tới 891 (bỏ `GIT_CONFIG_*`, khôi phục ở dòng 891); `merge_routes` 931, `merge_run` 944, `green_runs` 951, câu `a skipped build-and-test is refused` 973, `code_runs` 978, câu M3 `... refused in the one-job shape` 1000; `mig_run` 1102, vòng `for lacking in` 1172; `merge_while_broken` 1685; khối `wait-run` từ 1723.

Lệch ở số dòng lúc bắt đầu task thì theo ADR-0032/L3; lệch ở tên hàm, tên test, tên bước hay chữ trích ở trên thì dừng (mục 7).

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/main-guard-verified.sh`: hai lệnh đọc, không đổi cách chọn của `main-guard`.**

Tách hai phần đang có thành hàm, dùng ở cả cách chạy cũ và hai lệnh mới:

- `docs_only_reason`: đọc danh sách đường dẫn từ stdin (mỗi dòng một đường, bỏ dòng rỗng), in rỗng khi mọi dòng nằm trong `docs/` hay kết thúc bằng `.md` và không dòng nào thuộc `GRADLE_READ_DOCS`; ngược lại in lý do đầu tiên (`<file>` hay `<file>, file mà test Gradle đọc`). Đúng vòng ở dòng 107 tới 112 hôm nay; vòng đó thành `reason="$(docs_only_reason <<<"$diff")"; [[ -z "$reason" ]] || full "khác commit đã kiểm của PR #$number ở $reason"`, cùng câu lý do như nay.
- `gate_reason <head sha> <tên PR>`: in rỗng khi lần `pr-check` mới nhất trên commit đó xanh và job `GATE_JOB` có bước `GATE_STEP` `success`; ngược lại in đúng câu lý do mà dòng 82 tới 92 hôm nay đưa vào `full`. Cách chạy cũ gọi `reason="$(gate_reason "$head" "PR #$number")"; [[ -z "$reason" ]] || full "$reason"`.

Lệnh mới: dời ba dòng tính `REPO` (53 tới 56, chưa gồm dòng `|| full` ở 57) lên trước dòng kiểm tham số 48, rồi thêm ngay sau chúng:

- `scripts/main-guard-verified.sh --docs-only`: đúng một tham số; đọc đường dẫn từ stdin, in `yes` hay `no`, lý do ra stderr. Danh sách rỗng là `yes`. Luôn thoát 0.
- `scripts/main-guard-verified.sh --gate-ran <sha>`: `sha` 7 tới 40 ký tự hex (merge-if-green dùng sha của PR như GitHub trả, test dùng `abc1234`); in `yes` hay `no`, lý do ra stderr; mọi lỗi đọc (gọi `gh` thất bại, JSON lạ, `REPO` rỗng, thiếu hay thừa tham số) là `no`. Luôn thoát 0.

Cách chạy `<sha> [<sự kiện> [<lần chạy thứ>]]` của `main-guard.yml` giữ nguyên từng nhánh và từng câu lý do; 40 câu `main-guard-verified:` có sẵn (đếm ngày 2026-11-08) không đổi. Chú thích đầu file thêm hai lệnh mới (ADR-0041) và thay câu dòng 24 bằng: "The trees match, or differ only in docs that main got since the split: merge-if-green.sh merges a PR that is behind main only when main changed nothing but docs since then (ADR-0041), pinned to the checked sha." Bản `main-guard-verified.sh` cũ gặp `--docs-only` hay `--gate-ran` thì in `full` (tham số đầu không phải sha): `merge-if-green.sh` coi mọi câu trả lời khác `yes` là `no` và cập nhật nhánh, nên hai file lệch phiên bản vẫn an toàn (test 12). Sửa file này làm commit merge của task chạy `full` (file nằm trong `GATE_FILES`): đúng ý, lần đầu cổng mới chạy thật.

**B. `scripts/merge-if-green.sh`: nhánh chậm mà nhánh chính chỉ đổi tài liệu.**

Hằng mới sau dòng 37: `MAIN_GUARD_VERIFIED="${MAIN_GUARD_VERIFIED_SCRIPT:-$(dirname "$0")/main-guard-verified.sh}"` (chỉ để test chỉ tới bản sao hỏng; mặc định là script thật), `COMPARE_FILES_LIMIT=300`, `MERGEABLE_TRIES="${MERGE_MERGEABLE_TRIES:-3}"`, `MERGEABLE_WAIT_SECONDS="${MERGE_MERGEABLE_WAIT_SECONDS:-5}"` (test đặt 0).

Trong `evaluate()`, thay khối `behind_by` (dòng 199 tới 205):

1. `behind_by` khác `0`: gọi hàm mới `docs_only_behind`, đặt `DOCS_ONLY_BEHIND="yes"` hay để `no` kèm `BEHIND_REASON`. Hàm cho `no` ở điều đầu tiên không đạt:
   1. `gh api "repos/$REPO/compare/$HEAD_SHA...$BASE_BRANCH"` (base là commit của PR, head là nhánh chính: `files` là các file nhánh chính đổi từ điểm tách). Đọc lỗi, không có `files`, hay `files | length >= COMPARE_FILES_LIMIT` → `no`. Danh sách đường dẫn: `.files[] | .filename, (.previous_filename // empty)`.
   2. Đưa danh sách qua `"$MAIN_GUARD_VERIFIED" --docs-only`; khác `yes` (kể cả lệnh lỗi, không chạy được) → `no` (lý do lấy từ stderr của nó, ví dụ "nhánh chính đổi app/x.kt").
   3. Danh sách file của PR: `gh_all "repos/$REPO/pulls/$PR/files?per_page=100"` lấy `.filename` và `.previous_filename`. Đọc lỗi → `no`. Danh sách đó qua `"$MAIN_GUARD_VERIFIED" --docs-only` ra `yes` → `no` ("PR chỉ đổi file mà main-guard bỏ qua"): bộ lọc `code` của `pr-check.yml` bắt cả `*.md` trong `app/**`, `core/**`, `feature/**`, `scripts/**`, nên PR chỉ đổi `feature/x/README.md` vẫn chạy `check.sh`, nhưng commit merge của nó chỉ có `**/*.md`, `main-guard` không chạy (`paths-ignore`) và tổ hợp tài liệu không được kiểm ở đâu (ADR-0041 mục 1, ticket: "Áp nhầm quy tắc cho PR chỉ có tài liệu"). Có đường dẫn chung với danh sách ở bước 1 → `no` ("cả PR và nhánh chính cùng đổi <file>").
   4. `mergeable` của `pr_json`: `false` → `no` ("PR xung đột với nhánh chính"). `null` → đọc lại `pulls/$PR` tới `MERGEABLE_TRIES` lần, cách `MERGEABLE_WAIT_SECONDS` giây (vòng có giới hạn trong script). Vẫn `null` → `pending "GitHub chưa tính xong PR có xung đột không"` (`RESULT="pending"`, `return 0`): không `--update` thì mã 2, có `--update` thì vòng chính chờ rồi kiểm lại. Chỉ `true` mới đi tiếp. Giá trị lạ → `no`. Vòng đọc lại chỉ đọc `mergeable`, không kiểm lại `head.sha`: vô hại, vì lệnh merge ghim `sha` của lần đọc đầu và GitHub từ chối nếu PR có commit mới. Bước này đứng cuối: dữ liệu của các test có sẵn không có đường `compare/<đầu PR>...main` nên dừng ở bước 1 và cập nhật như cũ, không bao giờ đọc lại `pulls/$PR` (lần đọc lại sẽ tiêu một phần tử `__sequence__` của test `--update`).
   - `DOCS_ONLY_BEHIND="no"`: `RESULT="behind"`, `MESSAGE` như cũ thêm `($BEHIND_REASON)`, `return 0` như nay.
   - `DOCS_ONLY_BEHIND="yes"`: đi tiếp xuống phần kiểm CI như nhánh không chậm.
2. Sau vòng `REQUIRED_CHECKS` (dòng 214 tới 217), khi mọi kiểm tra đã xanh, nếu `DOCS_ONLY_BEHIND="yes"`: `GITHUB_REPOSITORY="$REPO" "$MAIN_GUARD_VERIFIED" --gate-ran "$HEAD_SHA"`:
   - `yes`: `RESULT` giữ `ready`, đặt `NO_UPDATE_NOTE` = `KHÔNG CẬP NHẬT NHÁNH: nhánh '$HEAD_REF' chậm $behind_by commit, nhưng từ điểm tách nhánh chính chỉ đổi tài liệu và PR đã chạy check.sh; merge không cập nhật, main-guard chạy scripts/tests/run.sh sau merge (ADR-0041).`
   - khác: `RESULT="behind"`, `MESSAGE` như cũ thêm `(PR không chạy check.sh ở pr-check: PR chỉ tài liệu luôn cập nhật nhánh, ADR-0041)`.
3. Sau vòng chính, trước dòng `Commit sẽ merge`, in `NO_UPDATE_NOTE` ra stderr khi có. `--dry-run` in dòng đó trước `SẴN SÀNG`.

**`merged_on_main` nhận cả commit của đường không cập nhật.** Hàm hiện nay (dòng 43 tới 54) chỉ nhận commit trên main có tiêu đề `<title> (#PR)` và cây bằng cây của `HEAD_SHA`. Merge không cập nhật cho commit squash có cây = cây của PR cộng tài liệu nhánh chính đổi từ điểm tách, nên điều kiện cây không khớp: khi GitHub trả lỗi mà commit đã vào main (L-080), script báo `TỪ CHỐI` thay vì `ĐÃ MERGE (GitHub báo lỗi)`, không gọi `main-guard`, và lần gọi sau không thấy `ĐÃ MERGE TRƯỚC ĐÓ`, có thể cập nhật nhánh rồi merge lần hai. Hàm mới:

1. Đọc cây của `HEAD_SHA` và 30 commit mới nhất của main (như nay); lỗi đọc → không thấy (in rỗng, mã 0).
2. Có commit đúng tiêu đề `"$TITLE (#$PR)"` và cây bằng cây của `HEAD_SHA` → in sha, mã 0 (như nay).
3. Không có commit nào cùng tiêu đề → in rỗng, mã 0 (như nay).
4. Có commit `C` cùng tiêu đề (lấy commit mới nhất) mà cây khác: hàm `docs_only_merge C` nhận `C` khi đủ mọi điều sau, và `merged_on_main` in sha của `C`, mã 0:
   1. Lấy hai commit về máy như khối `main-broken` (dòng 169 tới 171): `git fetch -q origin "+refs/pull/$PR/head:refs/merge-if-green/pr" "+refs/heads/$BASE_BRANCH:refs/merge-if-green/main"`, rồi bằng tên nhánh `$HEAD_REF` khi proxy từ chối `refs/pull`;
   2. `refs/merge-if-green/pr` là `HEAD_SHA`, và `C` là tổ tiên của `refs/merge-if-green/main` (`git merge-base --is-ancestor`);
   3. `B="$(git merge-base "$HEAD_SHA" "$C^1")"` không lỗi, không rỗng;
   4. tập `X` = `git -c core.quotePath=false diff --no-renames --name-only "$HEAD_SHA" "$C"` qua `"$MAIN_GUARD_VERIFIED" --docs-only` là `yes`;
   5. mọi file của `X` nằm trong `git -c core.quotePath=false diff --no-renames --name-only "$B" "$C^1"` (nhánh chính đổi từ điểm tách tới cha của commit);
   6. không file nào của `X` nằm trong `git -c core.quotePath=false diff --no-renames --name-only "$B" "$HEAD_SHA"` (PR cũng đổi): PR và nhánh chính cùng đổi một file tài liệu thì không nhận là đã merge.

   Điều 5 và 6 chặn nhận nhầm: commit đã vào main ở đầu cũ H0 (L-080), rồi PR được đẩy thêm commit tài liệu thành đầu H1; khác giữa H1 và `C` chỉ là tài liệu, nhưng là tài liệu của PR chứ không phải của nhánh chính, nên không được báo `ĐÃ MERGE TRƯỚC ĐÓ` (phần tài liệu mới của PR sẽ mất). Thiếu một điều (fetch lỗi, ref lệch, merge-base rỗng, khác ngoài tài liệu, ví dụ nhánh chính nhận commit code chen vào đúng lúc merge) → `merged_on_main` in sha của `C` và **trả mã 3**.
5. Hai nơi gọi đổi thành `if ! already="$(merged_on_main)"; then refuse_same_title "$already"; fi` (đầu `evaluate`, dòng 142) và `if ! found="$(merged_on_main)"; then refuse_same_title "$found" after-merge; fi` (vòng dòng 299 tới 303). Hàm mới `refuse_same_title <sha> [after-merge]`: `die "có commit ${1:0:7} cùng tiêu đề '$TITLE (#$PR)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem."`. Ở vòng sau khi lệnh merge báo lỗi (tham số thứ hai `after-merge`), commit đó có thể chính là commit vừa merge mà `main-guard` chưa được gọi: trước khi `die`, hàm gọi `POST actions/workflows/main-guard.yml/dispatches` như nhánh `ĐÃ MERGE (GitHub báo lỗi)` và thêm vào câu " Đã yêu cầu chạy main-guard; chờ: scripts/wait-run.sh --workflow main-guard.yml --since <giờ>." (gọi không được thì " Không yêu cầu được main-guard; chạy tay trên tab Actions."). Lần chạy tay của `main-guard` là `full` (ADR-0039), không hại khi commit không phải của PR. Ở đầu `evaluate` (chưa có lệnh merge nào) không gọi. Từ chối thay vì coi là chưa merge, để không bao giờ merge lần hai. Không dựa vào `die` bên trong `$(...)` (chỉ thoát subshell).

Khi cài: trong `if ! x="$(merged_on_main)"`, `errexit` tắt cho cả hàm và mọi hàm nó gọi, nên mỗi lệnh `git` và lệnh gọi `--docs-only` của `docs_only_merge` kiểm mã trả rõ ràng (`|| return 1` trong `docs_only_merge`), không dựa vào `set -e`. Ca "không quyết được" kết thúc `merged_on_main` bằng đúng một dòng `  return 3` (dòng duy nhất của file bắt đầu bằng `return 3`), để bản sao đối chứng của R4 thay được đúng dòng đó.

Sau `ĐÃ MERGE (GitHub báo lỗi)` vẫn gọi `main-guard` qua `workflow_dispatch` như nay. Lệnh fetch chỉ chạy khi trên main có commit cùng tiêu đề mà cây khác, việc hiếm; nó ghi hai ref `refs/merge-if-green/*` như khối `main-broken` đã làm.

**Kiểm tra `skipped` của PR không nháp là chờ, không phải đỏ.** Sau task, `build-and-test` chỉ bị bỏ qua khi PR là nháp; PR không nháp mà kiểm tra mới nhất `skipped` nghĩa là lần chạy của `ready_for_review` chưa tới. Trong `check_one`, giữa dòng 121 và 122:

```bash
  if [[ "$conclusion" == "skipped" ]]; then
    pending "kiểm tra '$name' trên commit ${HEAD_SHA:0:7} bị bỏ qua (lần chạy lúc PR còn nháp); chờ pr-check chạy sau khi PR chuyển sang sẵn sàng (ADR-0041)."
    return 0
  fi
```

Không bao giờ coi là xanh: không `--update` thì mã 2, có `--update` thì chờ tới hết giờ rồi mã 2.

Không đổi: thứ tự và nội dung mọi kiểm tra đứng trước (PR mở, nháp, `needs-human`, nhánh đích, `main-broken` và bản revert đúng, phiên đang chạy), vòng chờ, `MAX_UPDATES`, merge squash ghim `sha`, gọi `main-guard` sau `ĐÃ MERGE (GitHub báo lỗi)`, xóa nhánh `claude/`.

Giả định về GitHub: repo không bật "Require branches to be up to date before merging" (ADR-0010). Bật thì GitHub từ chối lệnh merge của đường không cập nhật; script báo `TỪ CHỐI: GitHub từ chối merge`, không hại. `pulls/N/files` trả tối đa 3000 file; PR lớn hơn thì danh sách thiếu và điều "không trùng file" có thể bỏ sót. PR như vậy không có trong dự án; ghi lại để biết.

Chú thích đầu file: thêm câu về đường không cập nhật (ADR-0041). Mã thoát giữ nguyên nghĩa; chú thích mã 2 thêm "including a branch behind main by docs only whose CI has not finished, and a skipped check left from a draft".

Lập luận an toàn: code của commit merge = code của điểm tách cộng phần PR đổi = code của commit cuối của PR, vì nhánh chính từ điểm tách không đổi file ngoài tài liệu (và file `docs/` mà Gradle đọc tính là code). `pr-check` đã chạy `check.sh` trên `refs/pull/N/merge`, cây có cùng code. Phần khác chỉ là tài liệu: `main-guard-verified.sh` so cây commit merge với commit cuối của PR, thấy khác chỉ ở tài liệu của nhánh chính, chọn `tests`. Nhánh chính nhận thêm commit code trong khoảng giữa lần kiểm và lệnh merge: `main-guard` thấy khác ngoài tài liệu, chọn `full` (như trước task).

**C. `.github/workflows/pr-check.yml`: PR nháp** (diff trên bản của nhánh chính 2fd2701):

```diff
 # build-and-test has two paths: a PR with code runs scripts/check.sh; a PR without code (docs only, or files outside
-# the code filter) runs only scripts/tests/run.sh, without Gradle or the Android SDK (L-040). The job is never skipped.
+# the code filter) runs only scripts/tests/run.sh, without Gradle or the Android SDK (L-040). The job is skipped only
+# for draft PRs (ADR-0041), by its one job-level if; merge-if-green.sh refuses drafts anyway, and a PR marked ready
+# for review gets a run on its head.
 # Changing this file is a high-risk plan and needs cross-review.
 name: pr-check
 
 on:
   pull_request:
     branches: [main]
+    # Declaring types drops the default set: synchronize must stay, or new commits go unchecked (ADR-0041).
+    types: [opened, synchronize, reopened, ready_for_review]
 ...
 jobs:
   build-and-test:
     runs-on: ubuntu-latest
+    if: github.event.pull_request.draft == false
     timeout-minutes: 45
```

Đây là `if:` cấp job duy nhất của job. `concurrency`, `permissions`, mọi bước và tên bước giữ nguyên (ticket, Không bao gồm).

**D. `scripts/wait-run.sh --pr`: PR nháp và lần chạy bị bỏ qua.**

- Không thêm lần đọc `pulls/N` nào. Lần đọc đầu (dòng 75) đổi `--jq .head.sha` thành `--jq '[.head.sha, (.draft // false | tostring)] | @tsv'`, đặt `sha` và `draft`; `read_head` (dòng 69) đọc thêm `(.draft // false | tostring)` vào cùng `@tsv`, và chỉ đổi `draft` khi lần đọc có sha hợp lệ (như `sha`, `state`).
- Trong vòng, ngay sau `read_head` (dòng 96): `draft` là `true` thì in `NHÁP: PR #<số> là bản nháp, pr-check không chạy cho PR nháp (ADR-0041). Chuyển PR sang sẵn sàng rồi gọi lại.` và thoát mã mới 6. Kiểm ở mọi vòng, trước khi đọc lần chạy và trước kiểm hết giờ, nên `WAIT_RUN_TIMEOUT_SECONDS=0` vẫn ra 6.
- Sau khi đọc lần chạy của PR (dòng 108): lần chạy mới nhất `completed` với `conclusion` `skipped` (lần chạy lúc PR còn nháp, trước khi lần chạy của `ready_for_review` xuất hiện) không phải kết quả: đặt `run=""`, chờ tiếp như chưa có lần chạy.
- Chú thích mã thoát thêm `6 (--pr) the PR is a draft: pr-check does not run for drafts (ADR-0041)`. Chế độ `--workflow` không đổi.

**E. Tài liệu** (nguyên văn; đã chạy `scripts/history-refs.py` trên bản sao có các câu này: sạch):

- `docs/auto-run.md`, bước 8, mục Chờ workflow: câu "5 PR xung đột với nhánh chính (`XUNG ĐỘT`)." thành "5 PR xung đột với nhánh chính (`XUNG ĐỘT`), 6 PR là bản nháp (`NHÁP`)."; sau ý "Mã 5 (`XUNG ĐỘT`): ..." thêm ý: "- Mã 6 (`NHÁP`): `pr-check` không chạy cho PR nháp (ADR-0041), nên chờ cũng không có kết quả. PR của task mở ở dạng sẵn sàng; PR nháp của phiên (PR sửa nguyên nhân, PR plan) do người chuyển sang sẵn sàng, phiên không chờ CI của chúng."
- `docs/auto-run.md`, bước 9, ý mã 3: sau câu "Mọi điều kiện được kiểm lại sau mỗi lần chờ." thêm "Nhánh chính từ điểm tách chỉ đổi tài liệu, PR đã chạy `check.sh` ở `pr-check`, không file nào đổi ở cả hai bên và PR không xung đột thì script merge mà không cập nhật nhánh và in `KHÔNG CẬP NHẬT NHÁNH`; `main-guard` của commit merge chạy `scripts/tests/run.sh` cho phần tài liệu (ADR-0041). PR chỉ có tài liệu luôn được cập nhật."
- `docs/auto-run.md`, mục Chuẩn bị cho người khi task dừng, ý 2: sau câu "`merge-if-green.sh` từ chối PR nháp và PR mang nhãn `needs-human`." thêm "`pr-check` không chạy cho PR nháp: người chuyển PR sang sẵn sàng thì CI chạy trên commit cuối, rồi merge (ADR-0041)."
- `docs/auto-run.md`, mục 4, bước 5, ý 7: "Người duyệt plan, gỡ nhãn, chuyển PR sang sẵn sàng rồi merge." thành "Người duyệt plan, gỡ nhãn, chuyển PR sang sẵn sàng (lúc đó `pr-check` mới chạy) rồi merge."
- `CLAUDE.md`, mục Lệnh, dòng "Merge khi nhánh chậm hơn nhánh chính": dấu chấm cuối dòng thành "; nhánh chính từ điểm tách chỉ đổi tài liệu và PR đã chạy `check.sh` thì script merge không cập nhật (ADR-0041). PR nháp không chạy `pr-check`."
- `docs/ops/setup-new-app.md`, bảng lỗi: dòng mã 3, cột nguyên nhân "Có PR khác vừa merge" thành "Có PR khác vừa merge, đổi code; hoặc PR chỉ có tài liệu (PR đã chạy `check.sh` mà nhánh chính chỉ đổi tài liệu thì script merge không cập nhật, ADR-0041)"; dòng `needs-human`, cột cách xử lý thành "Xem PR, sửa nếu cần, gỡ nhãn, đổi sang sẵn sàng, chờ `pr-check` chạy (PR nháp không chạy CI) rồi merge"; thêm dòng ngay sau: "| `wait-run.sh` trả mã 6, báo `NHÁP` | PR đang là bản nháp; `pr-check` không chạy cho PR nháp (ADR-0041) | Chuyển PR sang sẵn sàng (Ready for review), rồi gọi lại |".
- `.claude/skills/merge-pr/SKILL.md`: bước 2, sau "Mã 4 (hạ tầng), mã 5 (xung đột): theo bước 8." thêm "Mã 6 (PR nháp, `pr-check` không chạy): báo dev; người chuyển PR sang sẵn sàng rồi gọi lại."; bước 3, trước câu "Script in `ĐÃ MERGE (GitHub báo lỗi)` ..." thêm "Script in `KHÔNG CẬP NHẬT NHÁNH`: nhánh chính chỉ đổi tài liệu từ điểm tách, script merge không cập nhật nhánh, `main-guard` của commit merge chạy `scripts/tests/run.sh` (ADR-0041)."
- ADR-0010, sau ghi chú cuối ở đầu file: "> Ghi chú <ngày merge>: [ADR-0041](0041-merge-khong-cap-nhat-khi-main-chi-doi-tai-lieu.md) thay một phần mục 1: PR code được merge mà không cập nhật nhánh khi nhánh chính từ điểm tách chỉ đổi tài liệu; và thêm vào mục 4: PR nháp không chạy `pr-check`." ADR-0039, cùng chỗ: "> Ghi chú <ngày merge>: [ADR-0041](0041-merge-khong-cap-nhat-khi-main-chi-doi-tai-lieu.md) bỏ giả định ở mục Hệ quả rằng nhánh của PR không bao giờ chậm hơn nhánh chính lúc merge; cách `tests` thành đường thường cho PR code merge không cập nhật." Không sửa nội dung đã `accepted`.
- `docs/foundation.md`: sinh lại bằng `scripts/foundation.py` chỉ khi dòng Việc AI phải tuân thủ của ADR-0041 phải sửa (rà soát độc lập thấy lệch với code); `--check` phải khớp.

Câu mới trong script (thông báo, chú thích) không phải tài liệu quy tắc; vẫn chỉ dẫn ADR, không thêm mã bài học mới.

**F. `scripts/tests/run.sh`: hàm phụ trợ** (vào commit test của bước 3, cùng các câu của mục 4 và 3.5, để test viết trước thất bại đúng lý do):

- Chú thích dòng 268 tới 269: "and the job keeps its name and is never skipped" thành "and the job keeps its name and is skipped only for drafts (ADR-0041)".
- `pr_job_if` (dòng 304) thay bằng `pr_job_if_draft`: `awk '/^  build-and-test:$/ { j = 1; next } j && /^    steps:$/ { j = 0 } j && /^    if:/ { n++; if ($0 == "    if: github.event.pull_request.draft == false") d++ } END { print n + 0, (n == 1 && d == 1 ? "yes" : "no") }'`. AI đã chạy thử hàm này ngày 2026-11-07: workflow hôm nay `0 no`, workflow đã sửa theo mục C `1 yes`; ba bản sao của mục 3.5 trên workflow đã sửa `2 no`, `1 no`, `0 no`.
- `pr_sim`: đọc dòng `if:` cấp job (dòng bắt đầu đúng bốn dấu cách `    if: `, trước bước đầu tiên). Sau khi tính bộ lọc, trước vòng các bước: hơn một dòng → in `ERROR nhiều điều kiện cấp job` và thoát; một dòng khác `github.event.pull_request.draft == false` → in `ERROR điều kiện lạ cấp job: <điều kiện>` và thoát; đúng điều kiện nháp và biến môi trường `PR_SIM_DRAFT` là `true` → chỉ in dòng `code=.. release=..`, không bước nào (job bị bỏ qua). Không có dòng nào, hay PR không nháp → như nay. Thêm `import os`. Mọi câu P6 có sẵn chạy với PR không nháp, kết quả không đổi.
- `mig_run` (dòng 1102): thêm biến `MIG_SCRIPT` (mặc định `"$mg_root/scripts/merge-if-green.sh"`, cho bản sao đối chứng của R4); chạy script trong repo tạm `$work/mig-repo` (`git init`, không remote, tạo một lần trước hàm), gọi bằng đường dẫn tuyệt đối `"$mg_root/scripts/merge-if-green.sh"` (`mg_root="$PWD"` đặt trước `merge_during_run`), thêm `GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/no-origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git` cho riêng lệnh gọi (giữ `GIT_CONFIG_KEY_0` URL giả). `$work/no-origin.git` không tồn tại: fetch lỗi ngay ("does not appear to be a git repository"), không ra mạng, không ghi ref vào repo thật. AI đã thử cách này ngày 2026-11-07 trong một repo tạm: `git fetch` thoát 128 với câu đó. `merge_run`, `merge_during_run` giữ nguyên: dữ liệu của chúng không có đường `git/commits/<sha>`, nên `merged_on_main` dừng ở bước 1, không fetch.
- Khối mới "merge-if-green: no update when main changed only docs (ADR-0041)" đặt ngay sau khối `merge_while_broken` (trước dòng 1723), dùng `$root`, `git_t`, `$rev` của khối đó; test 14 tới 17 đặt trong khối `main-guard-verified` (trước dòng 891, dùng `mgv_*`).

Không sửa: `.github/workflows/main-guard.yml` (`paths-ignore` giữ `docs/**`, `**/*.md`), `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh`, `scripts/is-exact-revert.sh`, `scripts/tests/fake-gh` (mọi đường mới là đường đọc có tên cố định; `errors`, `fail_writes`, `__sequence__` đã có), `scripts/release.sh` (cấm sửa).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | Đường không cập nhật khi nhánh chính chỉ đổi tài liệu, `mergeable` `null` đọc lại rồi chờ, `merged_on_main` nhận cây chỉ khác ở tài liệu của nhánh chính và trả mã 3 khi không quyết được, `refuse_same_title` (gọi `main-guard` trước khi từ chối ở vòng sau lệnh merge lỗi), PR chỉ đổi file mà `main-guard` bỏ qua thì luôn cập nhật, kiểm tra `skipped` là chờ, chú thích (mục 3.1 B) |
| Sửa | `scripts/main-guard-verified.sh` | Hàm `docs_only_reason`, `gate_reason`; lệnh `--docs-only`, `--gate-ran <sha>`; dời ba dòng tính `REPO`; chú thích, kể cả câu dòng 24 (mục 3.1 A) |
| Sửa | `.github/workflows/pr-check.yml` | `types` đủ bốn loại, `if:` cấp job bỏ PR nháp, chú thích (mục 3.1 C) |
| Sửa | `scripts/wait-run.sh` | PR nháp: mã 6; lần chạy `skipped` không phải kết quả; chú thích (mục 3.1 D) |
| Sửa | `scripts/tests/run.sh` | Test mục 4; viết lại bốn câu ở mục 3.5; hàm phụ trợ `pr_job_if_draft`, `pr_sim` (cấp job), `mig_run` (repo tạm, origin cục bộ không có), `nu_routes`, `nu_git_run`; chú thích dòng 268 tới 269 (mục 3.1 F) |
| Sửa | `docs/adr/0041-merge-khong-cap-nhat-khi-main-chi-doi-tai-lieu.md` | Chỉ khi rà soát độc lập của task thấy ADR lệch với code đã làm; không đổi `status` |
| Sửa | `docs/adr/0010-soft-quality-gate-github-free.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0041 (mục 3.1 E) |
| Sửa | `docs/adr/0039-main-guard-tin-lan-kiem-cua-pr.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0041 (mục 3.1 E) |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` chỉ khi dòng quy tắc của ADR-0041 đổi |
| Sửa | `docs/auto-run.md` | Mã 6 của `wait-run.sh`; đường không cập nhật; PR nháp không chạy CI (mục 3.1 E) |
| Sửa | `CLAUDE.md` | Dòng `merge-if-green.sh --update` ở mục Lệnh |
| Sửa | `docs/ops/setup-new-app.md` | Hai dòng của bảng lỗi, thêm dòng mã 6 |
| Sửa | `.claude/skills/merge-pr/SKILL.md` | Mã 6 của `wait-run.sh`, `KHÔNG CẬP NHẬT NHÁNH` |
| Sửa | `docs/features/quy-trinh/plans/T-077-bot-chay-lai-pr-check.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quy-trinh/tickets/T-077-bot-lan-chay-lai-pr-check.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020), tìm ngày 2026-11-07 bằng `grep -rln -e main-guard-verified.sh -e wait-run.sh -e merge-if-green.sh -e pr-check.yml scripts .github .claude docs/auto-run.md docs/README.md docs/CLAUDE.md docs/ops CLAUDE.md app core feature`:

- `.github/workflows/main-guard.yml` gọi `main-guard-verified.sh <sha> <sự kiện> <lần chạy thứ>`: cách gọi và kết quả giữ nguyên (test có sẵn của T-074 giữ). Không sửa.
- `scripts/check.sh` (chú thích nhắc job `build-and-test`), `scripts/main-guard-status.sh` dòng 9, 56, 101 (nhắc `wait-run.sh --since`, chế độ không đổi), `scripts/is-exact-revert.sh` (nhắc `merge-if-green.sh`), `scripts/plan-check.py` (tên file rủi ro cao), `scripts/ui-test-needed.sh` (chú thích nhắc bộ lọc của `pr-check.yml`): chỉ chú thích hay tên file. Không sửa.
- `scripts/tests/playbook/PB-02.log`, `docs/ops/failure-playbook.md`: dòng của `wait-run.sh` mã 4, 5 và `merge-if-green.sh`; mã mới 6 không phải lỗi, không cần dòng tra lỗi. Không sửa.
- `.claude/skills/quick-lane/SKILL.md`, `.claude/skills/whatnow/SKILL.md`, `.claude/skills/to-app/SKILL.md`, `docs/README.md`, `docs/ops/quick-lane.md`, `docs/ops/to-app.md`, `docs/ops/release.md`, `docs/CLAUDE.md`: nói "merge bằng `merge-if-green.sh`", không nói cập nhật nhánh hay PR nháp. Không sửa.
- `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` nhắc `scripts/tests/run.sh` trong chú thích dòng 87, không đọc test nào của nó. Không bị ảnh hưởng, không sửa.
- `scripts/history-refs.py` nhắc `docs/foundation.md` trong tập `GENERATED` (trang sinh tự động, không kiểm mã lịch sử). Plan chỉ sinh lại trang đó bằng `scripts/foundation.py`. Không bị ảnh hưởng, không sửa.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi, và plan không đổi tên hay xóa mục nào, chỉ thêm câu vào các dòng nêu ở mục 3.1 E (danh sách của `scripts/plan-check.py` ngày 2026-11-08):

- dẫn `docs/auto-run.md` (số mục trong chú thích hay lời giao phiên): `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/push-on-commit.sh`, `scripts/resume-check.py`, `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/ui-test-verdict.sh`;
- dẫn `CLAUDE.md` (quy ước chung, mục Lệnh): `.github/ISSUE_TEMPLATE/from-app.yml`, `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt`, `scripts/new-app.sh`, `scripts/run-watchdog.py`; plan chỉ thêm nửa câu vào một dòng của mục Lệnh;
- dẫn `docs/ops/setup-new-app.md`: `.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`, `scripts/java-check.sh`, `scripts/new-app.sh`, `scripts/setup-check.sh`; plan chỉ sửa hai dòng và thêm một dòng vào bảng lỗi;
- `scripts/local-setup.sh` chỉ nhắc lệnh `scripts/tests/run.sh` (chạy sau khi cài), không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong `pr-check.yml` giữ nguyên (`contents: read`, `pull-requests: write`). `merge-if-green.sh` và `wait-run.sh` chạy bằng phiên đăng nhập `gh` của người hay phiên, đọc thêm `compare`, `pulls/N/files`, `actions/workflows/.../runs`, `actions/runs/N/jobs` (đều đã đọc ở nơi khác của hai script).
- **Thư viện và SDK:** Không thêm. Action có sẵn, không đổi phiên bản.

### 3.4. Phương án dự phòng

Không có phương án dự phòng trong task: điểm chưa chắc dưới đây chỉ thấy được sau khi merge, nên phiên không chuyển sang cách nào khác. Phần dưới là việc tiếp theo cho người (ghi lại ở mục 6), không phải phương án phiên làm thay.

- **Điểm chưa chắc:** GitHub ghi kết luận gì cho lần chạy `pr-check` mà mọi job bị `if:` bỏ qua (PR nháp): tài liệu và trải nghiệm cho thấy `skipped`, nhưng chưa thấy trên repo này. Nếu là `success`, `wait-run.sh --pr` gọi ngay sau khi chuyển PR sang sẵn sàng có thể báo `XANH` cho lần chạy cũ.
- **Điều kiện chuyển:** lần đầu một PR nháp được mở sau khi merge (mục 4, dòng kiểm tra thủ công) cho thấy lần chạy có `conclusion` khác `skipped`.
- **Việc tiếp theo cho người (không phải phương án trong task):** PR sửa nhỏ sau merge, plan riêng: `wait-run.sh --pr` bỏ qua lần chạy có `event` là `pull_request` mà mọi job `skipped` (đọc `actions/runs/<id>/jobs`, đã đọc khi đỏ). Không ảnh hưởng an toàn merge: `merge-if-green.sh` đọc check run của từng job, job `skipped` là chờ, không bao giờ xanh. Tiêu chí và test ở mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin (`scripts/existing-tests.py` ngày 2026-11-07: "Không có tên nào để tìm"). Mục này ghi các câu khẳng định có sẵn của `scripts/tests/run.sh` mà hành vi mới chạm (ADR-0031). Bốn câu viết lại dưới đây (R1 tới R4) là test viết trước của plan (vào commit test của bước 3), không phải câu viết lại giữa phiên, nên không tính vào giới hạn 3 câu của ADR-0031 (mục 8, câu 3).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Job `build-and-test` không bao giờ bị bỏ qua (L-040) | "`if: github.event.pull_request.draft == false` ... Đây là `if:` cấp job duy nhất của job" (mục 3.1 C) | `pr-check: build-and-test has no job-level if` (dòng 333, mong `0`) và `pr-check: the job-level if check fails on a copy that skips build-and-test` (dòng 336, bản sao `job-if.yml`, mong `1`) | Viết lại. Ý định: PR không nháp nào bỏ qua được `build-and-test`, kể cả PR không có code. Câu mới (R1): `pr-check: build-and-test has one job-level if, and it only skips drafts` mong `1 yes` (`pr_job_if_draft`). Bản sao `job-if.yml` dựng như nay (thêm `    if: github.event_name == 'pull_request'` sau dòng tên job): `pr-check: the job-level if check fails on a copy with a second job-level if` mong `2 no`. Thêm hai bản sao: `job-if-odd.yml` (perl đổi đúng dòng điều kiện nháp thành `    if: github.event.pull_request.draft == false \|\| github.event_name == 'push'`) mong `1 no`; `job-if-none.yml` (`grep -vxF` bỏ dòng đó) mong `0 no`. Cộng test 28 (PR nháp không chạy bước nào, PR không nháp chạy đủ). Không bớt câu, có đối chứng |
| Kiểm tra bắt buộc `skipped` bị từ chối (mã 1) | "kiểm tra bắt buộc có `conclusion` `skipped` → `pending` ... Không bao giờ coi là xanh" (mục 3.1 B) | `merge-if-green: a skipped build-and-test is refused` (dòng 973, dữ liệu `skipped_docs_runs`, mong `1 1`) và M3 của T-076 `merge-if-green: a skipped build-and-test is refused in the one-job shape` (dòng 1000, mong `1 1`) | Viết lại cả hai. Ý định: một `build-and-test` bị bỏ qua không bao giờ mở đường merge. Câu mới (R2, R3): `merge-if-green: a skipped build-and-test is never green: it waits` và `merge-if-green: a skipped build-and-test waits in the one-job shape`, cùng dữ liệu như nay, gọi `merge_run 12` (không `--dry-run`, để "không lệnh ghi" có nghĩa), mong `2 1 0` (mã 2; một dòng có `bị bỏ qua (lần chạy lúc PR còn nháp)`; số dòng `PUT pulls/12/merge` trong `$work/mg.json.writes` là 0); thêm `merge_run 12 --update` mong `2` và không có `PUT pulls/12/merge`. Đối chứng xanh: cùng dữ liệu với `build-and-test` `success` mong `0` (câu có sẵn `a docs-only PR whose build-and-test passed is ready`, `a PR whose only check is build-and-test is ready`). Đối chứng phá ý định: bản sao `$work/mgi-skip-green.sh` của `merge-if-green.sh`, dựng bằng `perl -pe 's/^\s*pending "kiểm tra \x27\$name\x27 trên commit \$\{HEAD_SHA:0:7\} bị bỏ qua.*$/    return 0/'` (dòng `pending` mới của mục 3.1 B thành `return 0`), kèm `! cmp -s` với bản gốc để chắc bản sao đã khác; chạy câu R3 trên bản sao (cùng `FIXTURE`) mong khác `2 1 0` (ra `0 ...`). Không nới: câu cũ kiểm "không merge", câu mới vẫn kiểm không merge và thêm kiểm không ghi gì |
| Commit cùng tiêu đề, cây khác: coi là chưa merge, lệnh merge chạy rồi báo `GitHub từ chối merge` | Commit cùng tiêu đề mà không chứng minh được chỉ khác ở tài liệu của nhánh chính: từ chối ngay, không merge lại, người xem (mục 3.1 B, bước 4, 5) | Vòng `for lacking in '[]' "$mig_other_tree" "$mig_other_title"` (dòng 1172 tới 1176): ba câu `merge-if-green refuses when the API fails and main lacks the commit` (mong `1`), `merge-if-green says GitHub refused the merge` (mong `1`), `merge-if-green does not ask for main-guard when the merge failed` (mong `0`) | Vòng giữ cho `'[]'` và `"$mig_other_title"`, ba câu không đổi. Ca `"$mig_other_tree"` tách ra (R4). Ý định: một commit trên main không chứng minh được là của PR này thì không bao giờ ra `ĐÃ MERGE`, không gọi `main-guard`, script từ chối. Câu mới: `merge-if-green refuses, without merging, a same-titled commit on main whose tree it cannot check` với `MIG_FAIL='["PUT pulls/12/merge"]' MIG_MAIN="$mig_other_tree"`, mong `1 1 0 0`: mã `$(mig_run)`; số dòng khớp `^TỪ CHỐI: có commit d111111 cùng tiêu đề 't (#12)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem\.$` trong `$work/mig.out`; `mig_writes '^PUT pulls/12/merge$'`; `mig_writes dispatches`. Fetch trong ca này đi tới origin cục bộ không có (mục 3.1 F), test 30 giữ điều đó. Đối chứng: `mig_on_main` (cùng tiêu đề, cùng cây) vẫn `ĐÃ MERGE TRƯỚC ĐÓ` (câu có sẵn); test 19, 27 cho ca cây chỉ khác ở tài liệu. Đối chứng phá ý định: bản sao `$work/mgi-same-title.sh` của `merge-if-green.sh`, dựng bằng `perl -pe 's/^(\s*)return 3\b.*$/${1}return 0/'` (dòng `return 3` duy nhất của `merged_on_main`: ca "không quyết được" thành "coi là đã merge"), kèm `! cmp -s` với bản gốc; `mig_run` có biến `MIG_SCRIPT` (mặc định script thật) để chạy bản sao, cùng dữ liệu R4: mong khác `1 1 0 0` (ra `0 ...`, `ĐÃ MERGE TRƯỚC ĐÓ`). Bản sao nằm trong `$work` nên `dirname "$0"` không có `main-guard-verified.sh`; ca R4 không tới lệnh đó vì fetch lỗi trước. Không nới: vẫn mã 1, không gọi `main-guard`; thêm kiểm không có lệnh merge |
| Nhánh chậm → luôn mã 3 hay cập nhật | Chậm mà nhánh chính chỉ đổi tài liệu → không cập nhật | `merge-if-green: without --update a branch behind main exits 3`, `merge-if-green --update: updates, waits for CI and merges`, `merge-if-green --update: merges once the single build-and-test check of the new commit is green`, các câu `merge_during_run` mong 3, `merge_while_broken` mong 3, câu trang 2 của danh sách nhánh | Giữ nguyên: dữ liệu của chúng không có đường `compare/<đầu PR>...main` (gh giả trả 404), nên `docs_only_behind` dừng ở bước 1 trước khi xét `mergeable` (bước 4, đứng cuối chính vì lẽ này): `pulls/12` của chúng không có `mergeable` mà không bao giờ được xét, và không có lần đọc lại nào tiêu phần tử `__sequence__` của test `--update` |
| `merged_on_main` chỉ nhận cây bằng cây của PR | Nhận thêm cây chỉ khác ở tài liệu của nhánh chính (mục 3.1 B) | Khối `mig_run`: các câu với `mig_on_main` (cây bằng nhau), `mig_other_title` (tiêu đề khác), `[]`, chuỗi `__sequence__` tìm thấy ở lần đọc thứ tư | Giữ nguyên: cây bằng nhau đi bước 2, tiêu đề khác đi bước 3, như nay. `mig_run` đổi thư mục chạy (repo tạm) và thêm origin cục bộ không có; script không đọc gì khác từ thư mục chạy ngoài `remote.origin.url` (lấy từ biến môi trường như nay) và nhánh cục bộ (chỉ để in chú ý) |
| `wait-run.sh --pr` lấy lần chạy mới nhất, mọi kết luận | PR nháp → mã 6; lần chạy `skipped` → chờ tiếp | Các câu `wait-run:` có sẵn (dữ liệu không có `draft`, không có lần chạy `skipped`; `pulls/7` có `__sequence__` ở `head_routes`) | Giữ nguyên: không thêm lần đọc `pulls/7`, `draft` thiếu là `false` |
| `pr_sim` bỏ qua dòng cấp job | Nhận đúng điều kiện nháp, điều kiện cấp job khác là `ERROR` (mục 3.1 F) | P6 của T-076 (các câu `pr-check: a PR touching ...`, `a docs-only PR ...`, bản sao P6d) | Giữ nguyên: mọi câu P6 chạy với PR không nháp; trên workflow đã sửa, dòng cấp job đúng điều kiện nháp nên kết quả không đổi |
| `main-guard-verified.sh <sha> ...` | Không đổi (dòng tính `REPO` dời lên, hai vòng thành hàm, cùng câu lý do) | 40 câu `main-guard-verified:` có sẵn (đếm ngày 2026-11-08) | Giữ nguyên |

Các câu `pr-check:` khác của T-076 (P1 tới P5, G1, `pr_needs`, `pr_filter_outputs` đếm `other=0`, `pr_filter_order` đếm 6 bước đọc bộ lọc): dòng `if:` cấp job và dòng `types` không nằm trong bước nào và không đọc `steps.*.outputs`, nên kết quả không đổi. Câu `pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml` (mong `names=1 missing= none`) đọc dòng `REQUIRED_CHECKS=` hay `SKIPPABLE_CHECKS=`; hằng mới của mục 3.1 B không khớp mẫu đó.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`, `gh` giả là `scripts/tests/fake-gh` với `FIXTURE` (`routes`, `errors`, `fail_writes`, `__sequence__`, ghi lệnh ghi vào `$FIXTURE.writes`).

Dữ liệu chung của khối `merge-if-green` mới (hàm phụ trợ `nu_routes [<conclusion của bước cổng>] [<mergeable JSON>]`, vào commit test của bước 3): từ `merge_routes claude/T-901-x '{"behind_by": 2}' "$code_runs"` (nhánh `claude/T-` để không đụng phiên), thêm bằng `python3 -I`:

- `pulls/12`: `"mergeable": true` (tham số để đổi);
- `compare/abc1234...main`: `{"files": <danh sách nhánh chính>}`, mặc định `[{"filename": "docs/runs/RUN-20261030-01.md"}, {"filename": "docs/lessons-2.md"}]`;
- `pulls/12/files?per_page=100&page=1`: `[{"filename": "app/src/main/x.kt"}, {"filename": "docs/features/f/plans/T-901.md"}]`;
- `actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10`: `{"workflow_runs": [{"id": 91, "created_at": "2026-10-29T00:00:00Z", "conclusion": "success"}]}`; `actions/runs/91/jobs?per_page=50`: job `build-and-test`, bước `Cổng chất lượng (scripts/check.sh)` với kết luận là tham số (mặc định `success`).

`main-guard-verified.sh` thật được gọi (không thay bằng bản giả). `nu_git_run <args>` (test 19, 27) chạy `"$root/scripts/merge-if-green.sh"` trong `$work/nu` với `GIT_CONFIG_COUNT=2 GIT_CONFIG_KEY_1="url.$work/nu-origin.git.insteadOf" GIT_CONFIG_VALUE_1=https://github.com/o/r.git`, `MERGE_RECHECK_SECONDS=0 MERGE_POLL_SECONDS=0`, như `merge_while_broken`. Kết quả đọc bằng mã thoát, `$work/mg.out` và `$work/mg.json.writes`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| DoD 1: `types` | Test cấu trúc | 1. `pr-check: runs on opened, synchronize, reopened and ready_for_review`: trong khối `pull_request:` có đúng dòng `    types: [opened, synchronize, reopened, ready_for_review]` và vẫn có `    branches: [main]`; mong `1 1`. Bản sao `$work/prwf/no-sync.yml` bỏ `synchronize, ` khỏi dòng đó: mong khác `1 1` (`0 1`) |
| DoD 1: bỏ nháp ở cấp job | Test cấu trúc | 2. Câu viết lại R1 ở mục 3.5 (`1 yes`) cùng ba bản sao (`2 no`, `1 no`, `0 no`) |
| DoD 1: PR nháp không chạy bước nào | Test PR mẫu | 28. `pr-check: a draft PR runs no step of build-and-test, a ready one runs check.sh`: `PR_SIM_DRAFT=true pr_sim_summary "$pr_wf" app/src/main/java/A.kt` và `PR_SIM_DRAFT=false pr_sim_summary "$pr_wf" app/src/main/java/A.kt`; mong `code=true release=false gate=no gradle=no docs=no r8=no code=true release=false gate=yes gradle=yes docs=no r8=no` |
| DoD 1: điều kiện cấp job khác bị báo | Test PR mẫu | 29. `pr-check: the sample PR check refuses another job-level condition`: bản sao `$work/prwf/job-odd.yml` dựng bằng `awk '/^    if: / { next } { print } /^  build-and-test:$/ { print "    if: github.event_name == '\''pull_request'\''" }'`; mong `ERROR điều kiện lạ cấp job: github.event_name == 'pull_request'`. Bản sao `job-if.yml` trên workflow đã sửa (hai dòng `if:`) mong `ERROR nhiều điều kiện cấp job` |
| DoD 1: `wait-run.sh` với PR nháp | Test của script | 3. `wait-run: a draft PR stops at once with code 6`: `pulls/7` là `{"head": {"sha": "abc1234"}, "draft": true}`, không có lần chạy; `wait_code` với `WAIT_RUN_TIMEOUT_SECONDS=0` mong `6 NHÁP`; và PR thành nháp ở lần đọc sau (`pulls/7` là `__sequence__` [không nháp, không nháp, nháp], lần chạy `pending`) mong `6 NHÁP`. Đối chứng: cùng dữ liệu với `"draft": false` mong `2 CHƯA` |
| DoD 1: chuyển sang sẵn sàng | Test của script | 4. `wait-run: a skipped run left from the draft is not a result`: `pulls/7` không nháp, `actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10` là `__sequence__` [lần chạy 90 `completed`/`skipped`] rồi [lần chạy 91 `completed`/`success` đứng đầu, 90 sau]; `wait_line` mong mã `0`, dòng bắt đầu `XANH` và có `lần chạy 91`. Đối chứng: chỉ có lần 90 `skipped`, hết giờ → `2 CHƯA` (không phải `1 ĐỎ`, không phải `0 XANH`) |
| DoD 2: không cập nhật | Test của script | 5. `merge-if-green: a code PR behind main by docs only merges without updating`: `nu_routes` mặc định, `merge_run 12` mong `0`; `$work/mg.json.writes` đúng `PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x;` (không có `update-branch`); `mg.out` có dòng `KHÔNG CẬP NHẬT NHÁNH` và `ĐÃ MERGE: PR #12 (t) tại commit abc1234`. Cùng dữ liệu `--dry-run` mong `0`, có `KHÔNG CẬP NHẬT NHÁNH` và `SẴN SÀNG`, không có lệnh ghi; `--update` mong `0`, không có `update-branch`. Câu thêm `merge-if-green: a PR merged without updating still gets the fresh-commit and STATUS.md warnings`: `nu_routes` với danh sách file của PR thêm `{"filename": "docs/STATUS.md"}`, `NOW_EPOCH=1792288980 merge_run 12 --dry-run` (commit 3 phút trước); mong `0 1 1` (mã; một dòng `mới 3 phút trước`; một dòng `PR có docs/STATUS.md`) |
| DoD 2: nhánh chính có code | Test của script | 6. `merge-if-green: main changed code since the split, so the branch is updated`: `compare` thêm `{"filename": "core/x.kt"}`; không `--update` mong `3` và `mg.out` có `core/x.kt`; dữ liệu `__sequence__` như câu có sẵn `--update: updates, waits for CI and merges` (đầu PR thành `def5678`) mong `0` và lệnh ghi bắt đầu bằng `PUT pulls/12/update-branch` |
| DoD 2: file `docs/` mà Gradle đọc | Test của script | 7. `merge-if-green: a docs file Gradle reads counts as code`: `compare` là `[{"filename": "docs/remote-config-keys.md"}]`, rồi `[{"filename": "docs/design-system/tokens.json"}]`; mong `3` cả hai. Đối chứng cùng test: `[{"filename": "docs/other.json"}]` mong `0` |
| DoD 2: PR chỉ tài liệu luôn cập nhật | Test của script | 8. `merge-if-green: a PR that did not run check.sh is always updated`: bước cổng `skipped`; mong `3`, `mg.out` có `PR không chạy check.sh`. Thêm: không có job `build-and-test` trong `jobs` mong `3`; run `pr-check` mới nhất `failure` (dù check run xanh: đọc lệch) mong `3`; PR chỉ đổi `[{"filename": "feature/x/README.md"}]` (bước cổng `success`, vì bộ lọc `code` bắt cả `*.md` trong `feature/**`) mong `3` và `mg.out` có `PR chỉ đổi file mà main-guard bỏ qua`. Đối chứng: bước cổng `success` mong `0` (test 5) |
| Xung đột thì cập nhật; chưa tính xong thì chờ | Test của script | 9. `merge-if-green: a conflicting PR updates, an unknown mergeable state waits`: `mergeable` `false` mong `3`; `pulls/12` là `__sequence__` [`null`, `null`, `true`] (`MERGE_MERGEABLE_WAIT_SECONDS=0`) mong `0` không có `update-branch`; luôn `null` mong `2` và có `chưa tính xong`, `--update` với `MERGE_UPDATE_TIMEOUT_SECONDS=0` mong `2` và không có `update-branch`; `mergeable` là chuỗi lạ mong `3` |
| Không trùng file hai bên | Test của script | 10. `merge-if-green: a file changed on both sides updates`: `compare` có `docs/features/f/plans/T-901.md` (PR cũng đổi); mong `3`, `mg.out` có `cả PR và nhánh chính cùng đổi`. Ca đổi tên: PR có `previous_filename` là `docs/lessons-2.md` mong `3` |
| Đổi tên trên nhánh chính | Test của script | 11. `merge-if-green: a rename on main counts its old path`: `compare` là `[{"filename": "docs/x.kt.md", "previous_filename": "app/x.kt", "status": "renamed"}]`; mong `3` |
| Không đọc được thì cập nhật | Test của script | 12. `merge-if-green: an unreadable answer updates`: `errors` cho `compare/abc1234...main` (`HTTP 500`), rồi `compare` không có `files`, rồi `files` 300 dòng `docs/a<n>.md`, rồi `errors` cho `pulls/12/files?per_page=100&page=1`, rồi `errors` cho `actions/runs/91/jobs?per_page=50`; mong `3` mỗi ca. Thêm: `MAIN_GUARD_VERIFIED_SCRIPT` chỉ tới một tệp không chạy được mong `3`; chỉ tới một tệp luôn in `full` (cách bản `main-guard-verified.sh` cũ trả lời `--docs-only`) mong `3` |
| CI còn chạy | Test của script | 13. `merge-if-green: behind by docs with CI still going waits`: `check-runs` là `lagging_runs` có sẵn với lần chạy `in_progress`; mong `2` và `mg.out` có `CI chưa chạy xong` (không phải `3`). Ca chưa có check run nào trên commit (`{"check_runs": []}`) mong `2` và có `CI chưa chạy trên commit này` |
| `merged_on_main` trên đường không cập nhật (L-080) | Test của script | 27. `merge-if-green: a no-update merge that GitHub answered with an error is found on main`: repo tạm `$work/nu` và repo bare `$work/nu-origin.git`, gọi qua `nu_git_run` (regex lấy repo ở dòng 91 của `merge-if-green.sh` khớp URL giả). `A` cha; `H` = `A` + `app/y.kt` (đầu PR, `refs/pull/12/head` của repo bare); `D` = `A` + `docs/runs/r.md`; `M` = cây `D` + `app/y.kt`, cha `D`, tiêu đề `t (#12)`, là `main` của repo bare. Route: `nu_routes` với mọi `abc1234` thay bằng sha đầy đủ của `H` (`pulls/12` `head.sha`, `compare/main...<H>` `{"behind_by": 1}`, `compare/<H>...main` có `docs/runs/r.md`, `commits/<H>/check-runs?per_page=100`, `commits/<H>`, `actions/workflows/pr-check.yml/runs?head_sha=<H>&per_page=10`), thêm `git/commits/<H>` trả cây của `H`, `commits?sha=main&per_page=30` là `__sequence__` [không có `M`, rồi có `M` với tiêu đề `t (#12)` và cây của `M`], `fail_writes` có `PUT pulls/12/merge`. Mong `0`, `mg.out` có `ĐÃ MERGE (GitHub báo lỗi): PR #12 (t) tại commit <7 ký tự của M>`, lệnh ghi có `POST actions/workflows/main-guard.yml/dispatches`. Đối chứng, mỗi ca kiểm cả mã và câu `cùng tiêu đề 't (#12)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem.`: (a) `M` thêm `app/z.kt` (khác ngoài tài liệu) mong `1`, và vì `M` chỉ thấy sau lệnh merge báo lỗi: lệnh ghi có `POST actions/workflows/main-guard.yml/dispatches`, `mg.out` có `Đã yêu cầu chạy main-guard`; cùng ca với `fail_writes` thêm `POST actions/workflows/main-guard.yml/dispatches` mong `1` và `mg.out` có `Không yêu cầu được main-guard`; (b) repo bare không có `refs/pull/12/head` và không có nhánh của PR (fetch lỗi) mong `1`; (c) nhận nhầm: `M` dựng từ đầu cũ `H0` = `A` + `app/y.kt`, rồi PR có đầu `H1` = `H0` + `docs/x.md` (route, `git/commits/<H1>` và `refs/pull/12/head` trỏ `H1`), `commits?sha=main&per_page=30` có `M` ngay từ đầu: mong `1`, không có `ĐÃ MERGE TRƯỚC ĐÓ`, không có lệnh ghi nào (từ chối ở đầu `evaluate`, không gọi `main-guard`); đối chứng của (c): cùng dựng mà `docs/x.md` nằm trong `D` (nhánh chính đổi) thay vì trong `H1` → `0` và `ĐÃ MERGE TRƯỚC ĐÓ`; (d) `H1` = `H0` + sửa `docs/lessons-2.md` mà `D` cũng đổi file đó: mong `1`, không có `ĐÃ MERGE TRƯỚC ĐÓ`; (e) merge-base rỗng: `M` có cha là một commit gốc riêng (`git commit-tree` không cha) mong `1` |
| Thư mục chạy của test không ra mạng | Test của script | 30. `merge-if-green tests fetch nothing from the network: the origin of mig_run is a missing local path`: trong `$work/mig-repo`, với đúng biến `GIT_CONFIG_*` mà `mig_run` dùng, `git ls-remote origin` thất bại và lỗi có `no-origin.git`; `git config --get-all remote.origin.url` chỉ có `https://github.com/o/r.git`. Mong `1 1 https://github.com/o/r.git` |
| Hai ticket khớp nhau: cùng quy tắc tài liệu | Test của script | 14. `main-guard-verified: --docs-only agrees with the mode main-guard picks`: bảng đường dẫn `docs/a.md`, `README.md`, `.claude/skills/x/SKILL.md`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/design-system/tokens.json`, `docs/x.json`, `app/x.kt`, `scripts/a.sh`, `docs.kt`. Với mỗi dòng: `--docs-only` trên dòng đó, và cách chọn của `main-guard-verified.sh <M>` trên repo tạm của khối T-074 (`mgv_setup`, `M="$(mgv_main <dòng>=b)"`, dữ liệu `mgv_fx "$M" "$mgv_h"`). Mong `yes` ⇔ `tests`, `no` ⇔ `full`, cho cả mười dòng; mong cụ thể `yes yes yes no no no yes no no no`. Dòng `--docs-only` với danh sách rỗng mong `yes`; với hai tham số (`--docs-only x`) mong `no` |
| Hai ticket khớp nhau: `paths-ignore` | Test cấu trúc | 15. `main-guard-verified: --docs-only accepts only what main-guard ignores`: đọc các mẫu `paths-ignore` của `main-guard.yml` (câu có sẵn đã giữ đúng hai dòng `docs/**`, `**/*.md`); với mỗi dòng mà test 14 mong `yes`, kiểm dòng đó khớp một trong hai mẫu (`[[ $f == docs/* \|\| $f == *.md ]]` sau khi đọc đúng hai mẫu ấy; mẫu khác thì test in `unknown pattern`). Bản sao `$work/mgwf/ignore-scripts.yml` của `main-guard.yml` thêm `      - 'scripts/**'` vào `paths-ignore`: mong `unknown pattern` |
| Hai ticket khớp nhau: commit merge dựng thật | Test của script | 16. `merge-if-green and main-guard: a PR merged without updating is checked with tests`: trong `"$work/mgv"`, `A` cha; `H` = `A` + `app/y.kt` (PR); `D` = `A` + `docs/runs/r.md` (nhánh chính); `M` = commit có cây `D` + `app/y.kt`, cha `D`. `git diff --name-only A D` qua `--docs-only` mong `yes`; `main-guard-verified.sh M` với đường `commits/M/pulls` trỏ PR 7 `head.sha` `H` mong `0 tests`. Đối chứng: `D` đổi `docs/remote-config-keys.md` thì `--docs-only` `no` và `main-guard-verified.sh M` `0 full` |
| `--gate-ran` | Test của script | 17. `main-guard-verified: --gate-ran reads the newest pr-check run`: dữ liệu `mgv_fx` của T-074 (`GITHUB_REPOSITORY=o/r`); bước cổng `success` mong `0 yes`; `skipped` mong `0 no`; run mới nhất `failure` mong `0 no`; `errors` cho `actions/runs/91/jobs?per_page=50` mong `0 no`; không có tham số sha mong `0 no`; sha 7 ký tự (`abc1234`, route tương ứng) mong `0 yes` |
| Mọi kiểm tra hiện có (L-058) | Bảng dưới | Test 18 tới 26 |
| DoD 1, DoD 2 sau khi merge | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Sau merge: (a) lần đầu có PR nháp (PR plan, PR sửa nguyên nhân hay PR thử của người): tab Actions có lần `pr-check` với job `build-and-test` `skipped`; ghi kết luận của lần chạy (`gh api repos/{owner}/{repo}/actions/runs/<id> --jq .conclusion`, mục 3.4); chuyển sang sẵn sàng thì lần chạy mới chạy `build-and-test`; (b) lần đầu một PR code chậm hơn nhánh chính chỉ vì tài liệu: `merge-if-green.sh` in `KHÔNG CẬP NHẬT NHÁNH`, `main-guard` của commit merge in `CHỈ KHÁC TÀI LIỆU` và xanh, và khi PR đổi code app thì job `ui-test` của `main-guard` chạy trên commit merge. Ghi vào nhật ký hay báo cáo phiên |

**Bảng L-058: trường hợp mới (PR code chậm hơn nhánh chính chỉ vì tài liệu, dữ liệu của test 5) qua từng kiểm tra hiện có của `merge-if-green.sh`.** Mỗi dòng là một test mới, dùng `nu_routes` rồi đổi đúng một điều.

| Kiểm tra hiện có | Trường hợp mới | Test |
| --- | --- | --- |
| Tham số lạ bị từ chối | Không đổi (trước mọi lệnh đọc) | Câu có sẵn `merge-if-green rejects '--update --dry-run'`, giữ |
| PR không mở | Bị từ chối | 18. `merge-if-green, behind by docs: a closed PR is still refused`: `state` `closed` mong `1` |
| PR đã nằm trên main | Báo `ĐÃ MERGE TRƯỚC ĐÓ`, không ghi gì | 19. `merge-if-green, behind by docs: a PR already on main is reported, not merged`: repo tạm của test 27, `commits?sha=main&per_page=30` có `M` từ đầu (tiêu đề `t (#12)`, cây của `M` khác cây của `H` ở `docs/runs/r.md`); `nu_git_run 12` mong `0`, có `ĐÃ MERGE TRƯỚC ĐÓ`, không có lệnh ghi. Ca cây bằng nhau: khối `mig_run` có sẵn |
| PR nháp | Bị từ chối | 20. `merge-if-green, behind by docs: a draft is still refused`: `draft` `true` mong `1`, `mg.out` có `đang là bản nháp` |
| Nhãn `needs-human` | Bị từ chối | 21. `merge-if-green, behind by docs: needs-human is still refused`: mong `1` |
| Nhắm nhánh khác | Bị từ chối | 22. `merge-if-green, behind by docs: a PR into another branch is still refused`: `base.ref` `other` mong `1` |
| `main-broken` đang mở (kể cả bản revert đúng) | Bị từ chối; bản revert đúng phải nằm ngay trên nhánh chính mới nhất nên không bao giờ "chậm" | 23. `merge-if-green, behind by docs: main-broken still refuses`: dựng như `merge_while_broken` có sẵn (repo tạm `$rev`, issue `main-broken` có `culprit`, `IS_EXACT_REVERT` là `$work/bin/revert-no`) cộng các đường của `nu_routes` cho sha của nhánh `good`; mong `1`, không có lệnh ghi, `broken.out` không có `KHÔNG CẬP NHẬT NHÁNH` |
| Phiên tự chạy đang chạy | PR của người bị từ chối; PR `claude/T-` của phiên qua | 24. `merge-if-green, behind by docs: a running session still refuses a person's PR`: nhánh `claude/template-x`, nhánh báo cáo `running` như `merge_during_run`; mong `1`. Đối chứng: `claude/T-901-x` mong `0` |
| Nhánh cập nhật theo nhánh chính | Đây là điều được nới: chỉ khi đủ bốn điều | Test 5 tới 13 |
| Kiểm tra đỏ trên commit cuối | Bị từ chối (`skipped`: chờ, không merge) | 25. `merge-if-green, behind by docs: a red check is still refused`: `check-runs` là `one_job_with build-and-test completed '"failure"'` mong `1` và có `có kiểm tra không qua`. Thêm: `one_job_with build-and-test completed '"skipped"'` mong `2` (chờ), không có lệnh ghi |
| Kiểm tra còn chạy, job chậm báo (L-017) | Chờ (mã 2), hay coi xanh khi lần chạy đã xanh | Test 13; 26. `merge-if-green, behind by docs: a lagging job of a green run counts as green`: `lagging_runs` cùng `actions/runs?head_sha=abc1234&per_page=50` xanh như câu có sẵn, mong `0` |
| Merge squash ghim `sha`, cảnh báo commit mới, `docs/STATUS.md`, xóa nhánh `claude/` | Như nhánh không chậm | Test 5 (lệnh ghi `PUT pulls/12/merge` rồi `DELETE`; câu thêm về hai dòng CHÚ Ý: commit mới đẩy và `docs/STATUS.md`); dòng `merge -f sha=` không đổi |
| `ĐÃ MERGE (GitHub báo lỗi)` gọi `main-guard` | Như cũ; lần gọi tay của `main-guard` là `full` (ADR-0039) | Câu có sẵn `merge-if-green asks for main-guard after such a merge`, giữ; test 27 cho đường không cập nhật |
| `MAX_UPDATES`, vòng chờ `--update` | Không cập nhật nên không tính lần | Test 5 (`--update` mong `0`, không có `update-branch`) |

**Test viết trước thất bại đúng lý do (bước 3).** Trên code hiện tại phải đỏ: 1, 2 (R1 và hai bản sao `2 no`, `1 no`), 3, 4, 5, 6 (thiếu `core/x.kt` trong `mg.out`), 7 (chỉ ca đối chứng `docs/other.json`, mong `0`), 8, 9 (ca `null` rồi `true`, ca luôn `null`), 10, 13, 14, 16, 17, 19, 24 (đối chứng `claude/T-901-x`), 25 (cả hai ca: code cũ trả 3 vì `behind_by: 2` trước khi đọc kiểm tra), 26, 27, 28, R2, R3, R4. Qua cả trước code, đúng dự kiến vì là đối chứng hay kiểm hành vi cũ giữ nguyên: 7 (hai ca mong `3`), 11, 12, 15, 18, 20 tới 23, 29 (ca `job-odd.yml`; ca `job-if.yml` đỏ trước code vì workflow chưa có dòng nháp), 30, bản sao `0 no` của R1. Ca nào khác danh sách này thì ghi vào nhật ký (ADR-0032/L4).

**Ngoại lệ viết test trước:** sửa tài liệu (`docs/auto-run.md`, `CLAUDE.md`, `docs/ops/setup-new-app.md`, `.claude/skills/merge-pr/SKILL.md`, ghi chú ADR-0010, ADR-0039, `foundation.md`) và chú thích trong `pr-check.yml`, `merge-if-green.sh`, `main-guard-verified.sh`, `wait-run.sh`: không có hành vi để kiểm. Câu của tài liệu được `scripts/history-refs.py` (trong `scripts/tests/run.sh`) kiểm sau khi viết.

### 4.1. Chạy thử

Không cần (E dưới 2): `gh api` với `fake-gh`, `compare`, `pulls/N/files` (`gh_all`), đọc bước của job, repo git tạm, `if:` cấp job và `types` của `pull_request` đều đã dùng trong dự án hay là cú pháp có tài liệu. Phần đã thử khi soạn bản 2 (2026-11-07, không commit): `pr_job_if_draft` trên workflow hôm nay và bản đã sửa (mục 3.1 F); fetch tới origin giả trong repo thật đi ra GitHub thật, trong repo tạm không remote thì lỗi ngay (mục 1, mục 3.1 F); câu tài liệu của mục 3.1 E qua `scripts/history-refs.py`. Phần chưa thử được trên máy là kết luận GitHub ghi cho lần chạy của PR nháp (mục 3.4; kiểm tra thủ công ở mục 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `scripts/merge-if-green.sh`, `.github/workflows/pr-check.yml`, `scripts/main-guard-verified.sh`, `scripts/wait-run.sh`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Bốn script (một là script merge), một workflow, test, sáu tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu rõ ba trường hợp, ADR-0041 đã `accepted`; plan thêm điều "không trùng file" chặt hơn ticket (đã duyệt ở bản 1); bản 2 còn câu hỏi nhỏ ở mục 8 |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh quyết có test với `gh` giả và repo git tạm; kết luận của lần chạy PR nháp và lần đầu merge không cập nhật chỉ thấy trên GitHub thật |
| D. Ảnh hưởng chéo | 3 | Đổi luật merge mà mọi PR, phiên tự chạy và `main-guard` dựa vào; T-078 sửa cùng `pr-check.yml`, `wait-run.sh`, `scripts/tests/run.sh`, `docs/auto-run.md`, skill `merge-pr` |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn (`main-guard-verified.sh`, `wait-run.sh`, `gh_all`, `merge_while_broken`); `types` của `pull_request` và `if:` theo `draft` lần đầu dùng ở repo này |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `merge-if-green.sh` (mọi lần merge), `pr-check.yml` (mọi PR), `main-guard-verified.sh` (mọi commit của main, cách chọn không đổi), `wait-run.sh` (mọi lần chờ CI của phiên).
- **T-076** (đã merge, d83c392): plan này sửa các test `pr-check:` của nó như mục 3.5 (R1 thay câu `has no job-level if` mà plan T-076 mục 6 đã báo trước; R3 thay M3; `pr_sim` thêm phần cấp job). Không đổi P1 tới P6, G1.
- **T-078** (ADR-0042, runner tự host; plan phiên bản 1 còn `approved`, phiên bản 2 soạn sau plan này): sửa `runs-on` của `build-and-test`, thêm bước kiểm runner, `wait-run.sh` mã 7 (sau mã 6 của plan này), cùng dòng mã thoát trong `docs/auto-run.md` bước 8 và skill `merge-pr`. Điều T-078 cần biết sau T-077: (a) `build-and-test` có đúng một `if:` cấp job, `    if: github.event.pull_request.draft == false`, ngay sau `    runs-on:`; `pr_job_if_draft` đòi đúng một dòng như vậy, `pr_sim` báo `ERROR` cho mọi điều kiện cấp job khác hay dòng thứ hai: T-078 không thêm điều kiện cấp job cho `build-and-test` mà không sửa hai hàm đó; (b) `pr_sim` chỉ hiểu điều kiện bước dạng `steps.filter.outputs`, `always()`, `failure()`, `&&`, `||`, so sánh chuỗi: bước có `if: runner.environment == ...` làm P6 in `ERROR điều kiện lạ`, T-078 phải mở rộng `pr_sim` (đã là điểm chặn của T-078 ở RUN-20261101-02); (c) `on.pull_request` có dòng `types`; (d) `wait-run.sh` kiểm nháp ngay sau `read_head`, trước khi đọc lần chạy: mã 7 của T-078 đặt sau kiểm nháp và kiểm xung đột; (e) `mig_run` chạy trong `$work/mig-repo`.
- `main-guard.yml` không sửa; `paths-ignore` của nó là một nửa của quy tắc chung, test 15 giữ khớp.
- **Việc tiếp theo sau merge (người):** nếu lần chạy `pr-check` của PR nháp đầu tiên có kết luận khác `skipped` (mục 3.4, mục 4 dòng kiểm tra thủ công), mở ticket sửa `wait-run.sh --pr` bỏ qua lần chạy mà mọi job `skipped`.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow, script và test là của dự án mẫu.
- **Chuyển tiếp.** PR của chính task chạy `pr-check` theo workflow của PR (sự kiện `opened`, `synchronize` có trong `types` mới), nên CI chạy như thường; `merge-if-green.sh` của nhánh chính lúc merge là bản cũ, cập nhật nhánh như nay nếu nhánh chậm. Không cần merge bằng script của nhánh task. Bản `wait-run.sh` cũ còn ở worktree hay nhánh tạo trước khi merge, gặp PR nháp, sẽ chờ tới hết giờ (mã 2) thay vì mã 6: không merge nhầm. `main-guard` của commit merge chạy `full` (đổi `pr-check.yml`, `main-guard-verified.sh`, `scripts/tests/run.sh`, đều trong `GATE_FILES`).
- **Cách chạy.** Phiên tự chạy (quyết định ngày 2026-11-01). Plan sửa `.github/workflows/pr-check.yml`: phiên cần khả năng `workflow-push` (`docs/ops/session-capabilities.md`); danh sách chạy ghi cách mở và phiên chạy `scripts/push-probe.sh` ở đầu phiên.
- Lỗ hổng có từ trước, ngoài phạm vi, chỉ ghi lại: `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSdkUseTest.kt` duyệt cả cây repo, kể cả `docs/`, nên một file trong `docs/` có thể làm test Gradle đỏ mà không nằm trong `GRADLE_READ_DOCS` (danh sách chỉ bắt dạng `rootProject.file("docs/...")`, ADR-0039). Tương tự, `docs/remote-config-keys.md` đổi một mình trên nhánh chính không kích hoạt `main-guard` (`paths-ignore` `docs/**`). Plan không đổi hai điểm này; với T-077 chúng chỉ nghĩa là quy tắc "chỉ tài liệu" kế thừa đúng các giới hạn của T-074.
- Đề xuất thứ tự: T-077 trước T-078 (`depends_on` của T-078); một danh sách chạy có thể có cả hai, T-078 sau cùng.

## 7. Điều kiện dừng riêng

- Tên job `build-and-test`, tên bước `Cổng chất lượng (scripts/check.sh)` hay `id: filter` của `pr-check.yml` khác mục 2; hay tên hàm, tên test có sẵn mà mục 3.5 viết lại (`pr_job_if`, `pr_sim`, `mig_run`, các câu R1 tới R4) không còn: dừng (cần plan phiên bản mới). Chỉ lệch số dòng thì theo ADR-0032/L3.
- Phải đổi một câu khẳng định có sẵn của `main-guard-verified:` (T-074) để code qua: dừng; cách chọn của `main-guard` không được đổi.
- Một test của `scripts/tests/run.sh` cần lệnh `git fetch` hay `git ls-remote` tới URL giả từ thư mục repo thật (không qua repo tạm như mục 3.1 F): dừng.
- Cần thêm quyền ghi cho `GITHUB_TOKEN` của `pr-check.yml`, hay bí mật mới: dừng.
- Sau merge, `merge-if-green.sh` merge không cập nhật một PR mà `main-guard` của commit merge chọn `full` vì tài liệu (không phải vì file cổng hay commit chen vào): hai bên lệch nhau; revert cùng dev.

## 8. Câu hỏi cho người duyệt

Câu 1 tới 7 của bản 1 đã được trả lời (mục 9, bản 1) và giữ nguyên. Câu mới của bản 2:

1. **Commit cùng tiêu đề mà cây khác (R4, mục 3.5).** Bản 1 đã quyết: không quyết được thì từ chối, người xem. Bản 2 nói rõ hệ quả với câu có sẵn: script từ chối ngay ở đầu `evaluate`, không thử lệnh merge nữa, nên câu "GitHub từ chối merge PR #12" của ca `mig_other_tree` thành câu "cùng tiêu đề ... người xem" và thêm kiểm "không có lệnh merge". Đề xuất đồng ý.
2. **`pr_sim` cấp job (mục 3.1 F, test 28, 29).** Chỉ nhận đúng `github.event.pull_request.draft == false`; điều kiện cấp job khác hay dòng thứ hai là `ERROR`. T-078 khi đó phải sửa `pr_sim` nếu thêm điều kiện cấp job. Đề xuất đồng ý (chặt như phần cấp bước của T-076).
3. **Bốn câu viết lại (R1 tới R4) viết sẵn trong plan**, vào commit test của bước 3, nên là test viết trước, không tính vào giới hạn 3 câu của ADR-0031 (như ghi chú duyệt bản 1, (a), với M3). Đề xuất đồng ý.
4. **Kiểm tra thủ công sau merge (mục 4, dòng cuối)** để người: phiên không mở PR nháp nào trong luồng task, và PR báo cáo chỉ có tài liệu nên luôn cập nhật. Nếu danh sách chạy có `plan_queue` lớn hơn 0, phiên ghi kết luận lần chạy của PR plan nháp đầu tiên vào báo cáo (lệnh `gh api` ở mục 4). Đồng ý?
5. **`token_estimate` 24.1M** theo cỡ M (`scripts/token-usage.py estimate`, trung vị 24 task cỡ M). T-076 (cỡ S, quy trình, nhiều test script) dùng 12.8M so với 5.4M ước tính; theo `docs/CLAUDE.md` không nâng số theo một task. Plan này có khoảng 30 test mới và bốn câu viết lại, gần T-074, T-075 hơn T-076.

## 9. Quyết định duyệt

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** AI ghi theo lời anh Nhật ngày 2026-11-08, trong phiên có người: "Duyệt T-093 (câu 3 chọn b), T-077 bản 2 và T-078 bản 2 theo đề xuất; câu 4 của T-078: H4–H6 làm sau trên repo app private. Chấp nhận a2a19a5, cách đọc của T-088 và hai câu viết lại của T-086." Trả lời mục 8 (bản 2), theo đề xuất: (1) đồng ý R4: gặp commit cùng tiêu đề mà cây khác thì từ chối ngay ở đầu `evaluate`, không thử lệnh merge; (2) đồng ý `pr_sim` chỉ nhận đúng `github.event.pull_request.draft == false` ở cấp job; (3) R1 tới R4 viết sẵn trong plan, vào commit test của bước 3, không tính vào giới hạn 3 câu của ADR-0031; (4) kiểm tra thủ công sau merge để người; `plan_queue` lớn hơn 0 thì phiên ghi kết luận lần chạy của PR plan nháp đầu tiên vào báo cáo; (5) giữ `token_estimate` 24.1M. Câu 1 tới 7 của bản 1 giữ nguyên. Chạy trong phiên tự chạy (quyết định ngày 2026-11-01), phiên cần `workflow-push` (`push-probe.sh` đầu phiên).

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30, sau khi T-075 merge (PR #324) và plan T-076 được duyệt (PR #315). Bảy câu ở mục 8 theo đề xuất của plan: (1) ADR-0041 ghi `accepted`, `docs/foundation.md` sinh lại cùng lần duyệt; (2) giữ điều "không file nào đổi ở cả hai bên"; (3) chấp nhận tổ hợp tài liệu ở file khác nhau chỉ bị bắt sau merge; (4) PR chỉ đổi script được merge không cập nhật như PR code; (5) đồng ý mã 6 của `wait-run.sh` và mã 2 của `merge-if-green.sh` khi chậm vì tài liệu mà CI chưa xong; (6) chỉ bắt đầu task sau khi T-076 merge; (7) `main-guard` đỏ ở cách `tests` chỉ vì tổ hợp tài liệu thì sửa bằng PR tài liệu, không revert; đổi `docs/auto-run.md` bước 9 là ticket riêng. Đối chiếu lúc duyệt: trạng thái sau T-075 khớp mục 2 (không còn `SKIPPABLE_CHECKS`, tên job và bước cổng giữ, regex lấy repo ở dòng 90 của `merge-if-green.sh`); trạng thái sau T-076 lấy theo plan T-076 đã duyệt. Task sửa trong file của mục 3.2, như điểm nên sửa (ADR-0029), bốn điều sau: (a) test M3 của T-076 (`merge-if-green: a skipped build-and-test is refused in the one-job shape`, mong mã 1) cũng khẳng định hành vi cũ "kiểm tra bắt buộc `skipped` bị từ chối": viết lại cùng lúc và cùng cách với câu `a skipped build-and-test is refused` ở mục 3.5 (mong `2 1 0`, không lệnh ghi), không tính vào giới hạn 3 câu của ADR-0031 vì là test viết trước của plan; (b) đối chứng phá ý định của câu `skipped` không dựa vào dòng `allow_skip` (T-075 đã bỏ): bản sao `$work/mgi-skip-green.sh` dùng `sed` thay chính dòng `pending "... bị bỏ qua (lần chạy lúc PR còn nháp) ..."` bằng `return 0`, kèm `! cmp -s` với bản gốc để chắc bản sao đã khác; (c) `merged_on_main` bước 3: merge-base lỗi hay rỗng thì đi bước 4 (từ chối, người xem), lệnh `git diff` có ngoặc kép quanh merge-base; thêm điều không file nào trong tập khác nhau giữa `HEAD_SHA` và commit thuộc `diff --name-only <merge-base> HEAD_SHA` (PR và nhánh chính cùng đổi một file tài liệu thì không nhận là đã merge); test 27 thêm hai ca đối chứng, merge-base rỗng (mong `1`) và (d) H1 sửa `docs/lessons-2.md` mà `D` cũng đổi (mong `1`, không có `ĐÃ MERGE TRƯỚC ĐÓ`), ca (c) cập nhật route `git/commits/<H1>`; (e) test 27 ghi dòng 90 (không phải 91) cho regex lấy repo. Task làm có người (`auto_run: false`; thay ngày 2026-11-01 bằng quyết định chạy trong phiên tự chạy, mục 2).

## 10. Nhật ký thực hiện

### RUN-20261108-01, phiên bản plan 2

- **Kết quả:** đã merge (PR #416, commit `52a6d2f`, 2026-11-08 07:57 UTC).
- **Nhánh, PR:** `claude/T-077-bo-qua-cap-nhat`, https://github.com/VGL-Studio/app-project-template/pull/416. Commit test `b2f02e8`, code `dfc04ae`, tài liệu `9f7f95e`.
- **Đã làm:** mục 3.1 A tới F. Hai lệnh `--docs-only`, `--gate-ran` của `main-guard-verified.sh`. Đường không cập nhật của `merge-if-green.sh` (`docs_only_behind`, đọc lại `mergeable`, `merged_on_main` nhận cây chỉ khác tài liệu của nhánh chính và trả mã 3, `refuse_same_title`, kiểm tra `skipped` là chờ). `pr-check.yml` có `types` và `if:` cấp job bỏ nháp. `wait-run.sh` trả mã 6 cho PR nháp và không tính lần chạy `skipped`. Tài liệu mục 3.1 E. Không sửa ADR-0041, `foundation.md` vẫn khớp. Bước 3 đến 5 giao cho một phiên con.
- **Test viết trước:** 56 câu `expect` mới (test 1 tới 30, R1 tới R4). 40 câu đỏ đúng lý do trước code, khớp danh sách của mục 4; rà soát độc lập đã chạy lại commit test trên code cũ và xác nhận. 16 câu qua ngay đúng như plan nêu. 0 câu phải viết chặt thêm. Bước 3 không chạy riêng test JVM của mọi module; chúng chạy trong `check.sh` ở bước 5 (1871 test, 0 lỗi), và task chỉ đổi script.
- **Câu khẳng định cũ viết lại (ADR-0031):** R1 tới R4, đúng câu của mục 3.5, giữ ý định, có đối chứng. R3: bản sao `mgi-skip-green.sh` ra `same 1 0` trước code, `differs 0 1` sau code. R4: bản sao `mgi-same-title.sh` ra `differs 0 1`. Chi tiết ở mô tả PR #416.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 12 ghi chú.
  - **Điểm nên sửa, chưa sửa vì mâu thuẫn chữ của mục 3.1 B bước 3, để người quyết:** `docs_only_behind` xét "PR chỉ đổi file mà `main-guard` bỏ qua" bằng `--docs-only`. Lệnh này coi `GRADLE_READ_DOCS` là code, khác `paths-ignore` của `main-guard.yml`. Vì vậy PR chỉ đổi `feature/x/README.md` cùng `docs/remote-config-keys.md` được merge không cập nhật nhánh mà `main-guard` không chạy. Code vẫn an toàn; chỉ tổ hợp tài liệu không được kiểm. Phương án: (a) plan mới xét theo đúng mẫu `paths-ignore`; (b) chấp nhận.
  - Ghi chú khác: lập luận an toàn giữ; không có đường merge lần hai; thứ tự kiểm tra có sẵn giữ; không test nào ra mạng; lệch nhỏ đúng loại.
- **Kết quả CI:** `pr-check` xanh (lần chạy 42460021967). `scripts/tests/run.sh` cục bộ: 1389 ok, 0 FAIL. `check.sh` QUA.
- **Lệch so với plan:**
  - Không có file ngoài mục 3.2. Nhật ký và ticket ghi ở nhánh báo cáo như `docs/auto-run.md` bước 7. Số dòng của mục 2 khớp.
  - Lệch nhỏ (ADR-0032), rà soát đã xét là đúng loại:
    - `read_head` đọc `draft` trước `mergeable_state` trong `@tsv`, vì IFS tab gộp hai trường rỗng.
    - Test 23 chạy không có `--dry-run`, để câu "không lệnh ghi" có nghĩa.
    - Test 7 tách ba câu, test 29 tách hai câu.
  - Ngày ghi chú ở ADR-0010, ADR-0039 là 2026-11-08, đúng ngày merge.
- **Việc còn lại:** kiểm thủ công sau merge của mục 4 (kết luận GitHub ghi cho lần chạy `pr-check` của PR nháp, mục 3.4).
- **Cần người xem:**
  - Điểm nên sửa ở trên.
  - Phiên con, khi soạn test trong scratchpad, đóng heredoc sớm. Nó tạo `/nu`, `/nu-origin.git` ở gốc ổ đĩa của container.
  - Lệnh xóa hai thư mục đó bị bộ kiểm an toàn chặn, nhưng phiên con làm tiếp thay vì dừng như lời giao. Hai thư mục nằm ngoài repo, mất khi container bị thu hồi.
