---
ticket: T-077
title: Bớt lần chạy lại pr-check: PR nháp và nhánh chính chỉ đổi tài liệu
status: approved
version: 1
risk_score: 9
risk_level: high
token_estimate: 23.9M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-30
lane:
---

# PLAN T-077: BỚT LẦN CHẠY LẠI PR-CHECK: PR NHÁP VÀ NHÁNH CHÍNH CHỈ ĐỔI TÀI LIỆU

## 1. Tóm tắt

`merge-if-green.sh` merge PR code mà không cập nhật nhánh khi nhánh chính, từ điểm tách, chỉ đổi tài liệu theo đúng quy tắc của `main-guard` (`paths-ignore` trừ `GRADLE_READ_DOCS`), không file nào đổi ở cả hai bên và PR không xung đột; quyết "chỉ tài liệu" và "PR đã chạy `check.sh`" bằng hai lệnh mới của `scripts/main-guard-verified.sh`, nên commit merge luôn được `main-guard` kiểm bằng cách `tests`. `pr-check.yml` không chạy cho PR nháp (`types` đủ bốn loại, `if:` cấp job), `wait-run.sh` báo ngay PR nháp. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả và repo git tạm, mỗi kiểm tra có sẵn của `merge-if-green.sh` một test (L-058). Theo ADR-0041 (đề xuất cùng PR plan này).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 5. Quyết định của anh Nhật ngày 2026-10-29: áp dụng cả sáu cách; T-074 tới T-077 làm có người (`auto_run: false`).

| Yêu cầu (định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| DoD 1 | PR nháp không chạy `pr-check`; chuyển sang sẵn sàng thì chạy | `types: [opened, synchronize, reopened, ready_for_review]`, `if:` cấp job của `build-and-test` bỏ nháp. GitHub vẫn tạo một lần chạy có job `skipped` cho PR nháp; `wait-run.sh` không coi lần đó là kết quả |
| DoD 2 | Nhánh chính chỉ có thêm commit tài liệu: merge không cập nhật nhánh | PR code (bước cổng `check.sh` xanh), file nhánh chính đổi từ điểm tách đều là tài liệu, không trùng file của PR, `mergeable: true` → merge, không gọi `update-branch` |
| DoD 2 | Có commit code: vẫn cập nhật và chờ CI | Một file ngoài tài liệu, hay file `docs/` Gradle đọc → như cũ (mã 3, hay `--update`) |
| DoD 2 | PR chỉ có tài liệu: luôn cập nhật | Bước cổng `check.sh` của PR `skipped` → như cũ |
| DoD 2 | Xung đột thì cập nhật | `mergeable` `false` → như cũ (`update-branch` báo xung đột như nay); `null` (GitHub chưa tính xong) → đọc lại có giới hạn, rồi chờ (mã 2); chỉ `true` mới không cập nhật |
| Ticket, phạm vi 3 | `main-guard` của T-074 khớp với quy tắc mới | `merge-if-green.sh` dùng chính hàm của `main-guard-verified.sh`; test đối chiếu hai bên trên cùng bảng đường dẫn và trên một commit merge dựng thật |
| DoD 3 | ADR cập nhật, `foundation.md` sinh lại | ADR-0041 `proposed` trong PR plan này; người ghi `accepted` khi duyệt (mục 8, câu 1); ghi chú ở ADR-0010, ADR-0039 trong PR của task |
| DoD 4 | CI qua, đã merge | Như mọi task |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (mục 1 bị thay một phần, mục 4 thêm), ADR-0039 (giả định ở mục Hệ quả bị bỏ; cách `tests` thành đường thường), ADR-0025 (`main-broken`), ADR-0029 (phạm vi file), ADR-0031 (viết lại câu khẳng định cũ), ADR-0041 (mới).

**Giả định về T-075, T-076 (chưa merge khi soạn plan).** Plan viết cho trạng thái sau cả hai:

- T-075 bỏ job `ui-test` khỏi `pr-check.yml`; `SKIPPABLE_CHECKS` của `merge-if-green.sh` rỗng hay bị bỏ.
- T-076 bỏ job `changes`: `pr-check.yml` còn một job `build-and-test`, bước lọc đường dẫn nằm trong job (`id: filter`, điều kiện dạng `steps.filter.outputs.code == 'true'`); `REQUIRED_CHECKS=("build-and-test")`; tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)` giữ nguyên (ADR-0039 đòi vậy, test 12 của T-074 giữ).

Khi bắt đầu task: đọc `pr-check.yml`, `REQUIRED_CHECKS`, `SKIPPABLE_CHECKS` và các test `pr-check:` của `scripts/tests/run.sh` trên nhánh chính mới nhất. Lệch giả định (ví dụ job `build-and-test` đã có `if:` cấp job khác, hay tên bước đổi) thì làm theo mục 3.4. T-075, T-076 chưa merge thì không bắt đầu task (mục 7).

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/main-guard-verified.sh`: hai lệnh đọc, không đổi cách chọn của `main-guard`.**

Tách hai phần đang có thành hàm, dùng ở cả cách chạy cũ và hai lệnh mới:

- `docs_only_reason`: đọc danh sách đường dẫn (mỗi dòng một đường), trả rỗng khi mọi dòng nằm trong `docs/` hay kết thúc bằng `.md` và không dòng nào thuộc `GRADLE_READ_DOCS`; ngược lại trả lý do đầu tiên (`<file>` hay `<file>, file mà test Gradle đọc`). Đúng vòng ở dòng 105 tới 110 hiện nay.
- `gate_ran <head sha>`: lần `pr-check` mới nhất trên commit đó xanh và job `GATE_JOB` có bước `GATE_STEP` `success`. Đúng dòng 80 tới 90 hiện nay; in lý do khi không.

Lệnh mới, đặt trước dòng kiểm tham số hiện có:

- `scripts/main-guard-verified.sh --docs-only`: đọc đường dẫn từ stdin, in `yes` hay `no`; lý do ra stderr. Danh sách rỗng là `yes`. Luôn thoát 0.
- `scripts/main-guard-verified.sh --gate-ran <sha>`: in `yes` hay `no`; mọi lỗi đọc (gọi `gh` thất bại, JSON lạ, không biết repo) là `no`. Luôn thoát 0. `REPO` lấy như hiện nay (`GITHUB_REPOSITORY`, rồi `remote.origin.url`).

Cách chạy `<sha> [<sự kiện> [<lần chạy thứ>]]` của `main-guard.yml` giữ nguyên từng nhánh và từng dòng lý do; các test có sẵn của T-074 không đổi. Chú thích đầu file thêm hai lệnh mới và ADR-0041, và viết lại câu ở dòng 23 ("The trees match because merge-if-green.sh merges only a PR that is not behind main, pinned to the checked sha."), câu này sai sau task: "The trees match, or differ only in docs that main got since the split: merge-if-green.sh merges a PR that is behind main only when main changed nothing but docs since then (ADR-0041), pinned to the checked sha." Bản `main-guard-verified.sh` cũ (trước task) gặp `--docs-only` hay `--gate-ran` thì in `full` (tham số đầu không phải sha): `merge-if-green.sh` coi mọi câu trả lời khác `yes` là `no` và cập nhật nhánh, nên hai file lệch phiên bản vẫn an toàn (test 12). Sửa file này làm commit merge của task chạy `full` (file nằm trong `GATE_FILES`): đúng ý, lần đầu cổng mới chạy thật.

**B. `scripts/merge-if-green.sh`: nhánh chậm mà nhánh chính chỉ đổi tài liệu.**

Hằng mới ở đầu file: `MAIN_GUARD_VERIFIED="${MAIN_GUARD_VERIFIED_SCRIPT:-$(dirname "$0")/main-guard-verified.sh}"` (chỉ để test chỉ tới bản sao hỏng; mặc định là script thật), `COMPARE_FILES_LIMIT=300`, `MERGEABLE_TRIES="${MERGE_MERGEABLE_TRIES:-3}"`, `MERGEABLE_WAIT_SECONDS="${MERGE_MERGEABLE_WAIT_SECONDS:-5}"` (test đặt 0).

Trong `evaluate()`, thay khối `behind_by` (dòng 199 tới 205):

1. `behind_by` khác `0`: gọi hàm mới `docs_only_behind`, đặt `DOCS_ONLY_BEHIND="yes"` hay để `no` kèm `BEHIND_REASON`. Hàm cho `no` ở điều đầu tiên không đạt:
   1. `gh api "repos/$REPO/compare/$HEAD_SHA...$BASE_BRANCH"` (base là commit của PR, head là nhánh chính: `files` là các file nhánh chính đổi từ điểm tách). Đọc lỗi, không có `files`, hay `files | length >= COMPARE_FILES_LIMIT` → `no`. Danh sách đường dẫn: `.files[] | .filename, (.previous_filename // empty)`.
   2. Đưa danh sách qua `"$MAIN_GUARD_VERIFIED" --docs-only`; khác `yes` → `no` (lý do lấy từ stderr của nó, ví dụ "nhánh chính đổi app/x.kt").
   3. Danh sách file của PR: `gh_all "repos/$REPO/pulls/$PR/files?per_page=100"` lấy `.filename` và `.previous_filename`. Đọc lỗi → `no`. Có đường dẫn chung với danh sách ở bước 1 → `no` ("cả PR và nhánh chính cùng đổi <file>").
   4. `mergeable` của `pr_json`: `false` → `no` ("PR xung đột với nhánh chính"). `null` (GitHub tính lười, thường gặp ngay sau khi nhánh chính đổi) → đọc lại `pulls/$PR` tới `MERGEABLE_TRIES` lần, cách `MERGEABLE_WAIT_SECONDS` giây (vòng có giới hạn trong script). Vẫn `null` → không cập nhật nhánh vội mà `pending "GitHub chưa tính xong PR có xung đột không"` (`RESULT="pending"`, `return 0`): không `--update` thì mã 2, có `--update` thì vòng chính chờ rồi kiểm lại. Chỉ `true` mới đi tiếp. Giá trị lạ → `no`. Bước này đứng cuối (sau `compare`, `--docs-only` và kiểm file trùng): dữ liệu của các test có sẵn không có `compare/...` nên dừng ở bước 1 và cập nhật như cũ, không bao giờ đọc lại `pulls/$PR` (lần đọc lại sẽ tiêu một phần tử `__sequence__` của test `--update`).
   - `DOCS_ONLY_BEHIND="no"`: `RESULT="behind"`, `MESSAGE` như cũ thêm `($BEHIND_REASON)`, `return 0` như nay.
   - `DOCS_ONLY_BEHIND="yes"`: đi tiếp xuống phần kiểm CI như nhánh không chậm.
2. Sau vòng `REQUIRED_CHECKS` (sau T-075 không còn `SKIPPABLE_CHECKS`; nếu T-075 giữ một mảng rỗng thì sau cả hai vòng), khi mọi kiểm tra đã xanh, nếu `DOCS_ONLY_BEHIND="yes"`: `GITHUB_REPOSITORY="$REPO" "$MAIN_GUARD_VERIFIED" --gate-ran "$HEAD_SHA"`:
   - `yes`: `RESULT` giữ `ready`, đặt `NO_UPDATE_NOTE` = `KHÔNG CẬP NHẬT NHÁNH: nhánh '$HEAD_REF' chậm $behind_by commit, nhưng từ điểm tách nhánh chính chỉ đổi tài liệu và PR đã chạy check.sh; merge không cập nhật, main-guard chạy scripts/tests/run.sh sau merge (ADR-0041).`
   - khác: `RESULT="behind"`, `MESSAGE` như cũ thêm `(PR không chạy check.sh ở pr-check: PR chỉ tài liệu luôn cập nhật nhánh, ADR-0041)`.
3. Sau vòng chính, trước dòng `Commit sẽ merge`, in `NO_UPDATE_NOTE` ra stderr khi có. `--dry-run` in thêm dòng đó trước `SẴN SÀNG`.

**`merged_on_main` nhận cả commit của đường không cập nhật.** Hàm hiện nay (dòng 45 tới 53) chỉ nhận commit trên main có tiêu đề `<title> (#PR)` và cây bằng cây của `HEAD_SHA`. Merge không cập nhật cho commit squash có cây = cây của PR cộng tài liệu nhánh chính đổi từ điểm tách, nên điều kiện cây không bao giờ khớp: khi GitHub trả lỗi mà commit đã vào main (L-080), script báo `TỪ CHỐI` thay vì `ĐÃ MERGE (GitHub báo lỗi)`, không gọi `main-guard`, và lần gọi sau không thấy `ĐÃ MERGE TRƯỚC ĐÓ`, có thể cập nhật nhánh rồi merge lần hai. Sửa:

1. Tìm commit có đúng tiêu đề `"$TITLE (#$PR)"` trong 30 commit mới nhất của main (như nay). Không có → không thấy (như nay).
2. Cây bằng cây của `HEAD_SHA` → đã merge (như nay).
3. Cây khác: lấy hai commit về máy như khối `main-broken` (fetch `+refs/pull/$PR/head:refs/merge-if-green/pr` và `+refs/heads/$BASE_BRANCH:refs/merge-if-green/main`, rồi bằng tên nhánh khi proxy từ chối `refs/pull`), kiểm `refs/merge-if-green/pr` là `HEAD_SHA` và commit tìm thấy là tổ tiên của `refs/merge-if-green/main`, rồi lấy danh sách file khác nhau giữa `HEAD_SHA` và commit đó (`diff --no-renames --name-only`, `core.quotePath=false`, như `main-guard-verified.sh`) qua `"$MAIN_GUARD_VERIFIED" --docs-only`, và mọi file trong danh sách đó phải nằm trong danh sách file mà nhánh chính đổi từ điểm tách tới cha của commit (`diff --no-renames --name-only $(merge-base HEAD_SHA <commit>^1) <commit>^1`): cả hai đạt → đã merge (đúng cách `main-guard` chọn `tests` cho commit đó). Điều thứ hai chặn nhận nhầm: commit đã vào main ở đầu cũ H0 (L-080), rồi PR được đẩy thêm commit tài liệu thành đầu H1; khác giữa H1 và commit chỉ là tài liệu, nhưng là tài liệu của PR chứ không phải của nhánh chính, nên không được báo `ĐÃ MERGE TRƯỚC ĐÓ` (phần tài liệu mới của PR sẽ mất). Khi đó đi bước 4 (từ chối, người xem).
4. Có commit cùng tiêu đề mà không quyết được (fetch lỗi, ref lệch, khác ngoài tài liệu, ví dụ nhánh chính nhận commit code chen vào đúng lúc merge) → `die "có commit <7 ký tự> cùng tiêu đề '$TITLE (#$PR)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem."`. Từ chối thay vì coi là chưa merge, để không bao giờ merge lần hai. Hàm vì vậy in sha, in rỗng, hay dừng script.

Hai nơi gọi hàm (đầu `evaluate`, và vòng tìm sau khi lệnh merge báo lỗi) giữ nguyên; sau `ĐÃ MERGE (GitHub báo lỗi)` vẫn gọi `main-guard` qua `workflow_dispatch` như nay.

**Kiểm tra `skipped` của PR không nháp là chờ, không phải đỏ.** Sau task, `build-and-test` chỉ bị bỏ qua khi PR là nháp; PR không nháp mà kiểm tra mới nhất `skipped` nghĩa là lần chạy của `ready_for_review` chưa tới. `check_one`: kiểm tra bắt buộc có `conclusion` `skipped` → `pending "kiểm tra '$name' trên commit ${HEAD_SHA:0:7} bị bỏ qua (lần chạy lúc PR còn nháp); chờ pr-check chạy sau khi PR chuyển sang sẵn sàng (ADR-0041)."`. Không bao giờ coi là xanh: không `--update` thì mã 2, có `--update` thì chờ tới hết giờ rồi mã 2. Nhánh `allow_skip` của `SKIPPABLE_CHECKS` (nếu T-075 còn giữ) không đổi.

Không đổi: thứ tự và nội dung mọi kiểm tra đứng trước (PR mở, nháp, `needs-human`, nhánh đích, `main-broken` và bản revert đúng, phiên đang chạy), vòng chờ, `MAX_UPDATES`, merge squash ghim `sha`, gọi `main-guard` sau `ĐÃ MERGE (GitHub báo lỗi)`, xóa nhánh `claude/`.

Giả định về GitHub: repo không bật "Require branches to be up to date before merging" (gói Free, repo private, không có bảo vệ nhánh: ADR-0010). Bật thì GitHub từ chối lệnh merge của đường không cập nhật; script báo `TỪ CHỐI: GitHub từ chối merge`, không hại. `pulls/N/files` trả tối đa 3000 file; PR lớn hơn thì danh sách thiếu, và điều "không trùng file" có thể bỏ sót. PR như vậy không có trong dự án; ghi lại để biết.

Chú thích đầu file: thêm câu về đường không cập nhật (ADR-0041). Mã thoát giữ nguyên nghĩa; chú thích mã 2 thêm "kể cả nhánh chậm khi nhánh chính chỉ đổi tài liệu và CI chưa xong".

Lập luận an toàn: code của commit merge = code của điểm tách cộng phần PR đổi = code của commit cuối của PR, vì nhánh chính từ điểm tách không đổi file ngoài tài liệu (và file `docs/` mà Gradle đọc tính là code). `pr-check` đã chạy `check.sh` trên `refs/pull/N/merge`, cây có cùng code. Phần khác chỉ là tài liệu: `main-guard-verified.sh` so cây commit merge với commit cuối của PR, thấy khác chỉ ở tài liệu của nhánh chính, chọn `tests`. Nhánh chính nhận thêm commit code trong khoảng giữa lần kiểm và lệnh merge: `main-guard` thấy khác ngoài tài liệu, chọn `full` (như trước task).

**C. `.github/workflows/pr-check.yml`: PR nháp.**

- `on.pull_request`: thêm `types: [opened, synchronize, reopened, ready_for_review]`, giữ `branches: [main]`.
- Job `build-and-test`: `if: github.event.pull_request.draft == false` ở cấp job, dòng ngay sau `runs-on:` (hay sau dòng đầu của job nếu T-076 đổi thứ tự). Đây là `if:` cấp job duy nhất của job.
- Chú thích đầu file: thay câu "The job is never skipped" bằng "The job is skipped only for draft PRs (ADR-0041); merge-if-green.sh refuses drafts anyway." `concurrency` giữ nguyên (ticket, Không bao gồm).

**D. `scripts/wait-run.sh --pr`: PR nháp và lần chạy bị bỏ qua.**

- `read_head` và lần đọc đầu đọc thêm `.draft`. PR nháp: in `NHÁP: PR #<số> là bản nháp, pr-check không chạy cho PR nháp (ADR-0041). Chuyển PR sang sẵn sàng rồi gọi lại.` và thoát mã mới 6, ngay ở vòng đầu và ở vòng nào thấy PR thành nháp.
- PR không nháp: lần chạy mới nhất đã xong với `conclusion` `skipped` (lần chạy lúc PR còn nháp, trước khi lần chạy của `ready_for_review` xuất hiện) không phải kết quả: chờ tiếp như chưa có lần chạy.
- Chú thích mã thoát thêm `6 (--pr) the PR is a draft: pr-check does not run for drafts (ADR-0041)`. Chế độ `--workflow` không đổi.

**E. Tài liệu.**

- `docs/auto-run.md`: bước 8 (dòng khoảng 201, danh sách mã của `wait-run.sh`) thêm mã 6; dòng khoảng 219 (mã 3, `--update`) thêm "nhánh chính chỉ đổi tài liệu và PR đã chạy `check.sh` thì script merge không cập nhật (ADR-0041)"; mục 3 bước 7 (dòng khoảng 175), mục Chuẩn bị cho người khi task dừng ý 2 (dòng khoảng 256), mục 4 bước 5 ý 3 và 7 (dòng khoảng 348, 352): PR nháp không chạy `pr-check`; người chuyển PR sang sẵn sàng thì CI chạy, rồi merge.
- `CLAUDE.md` mục Lệnh, dòng "Merge khi nhánh chậm hơn nhánh chính": thêm "nhánh chính chỉ đổi tài liệu thì script merge không cập nhật (ADR-0041)".
- `docs/ops/setup-new-app.md` dòng 133 (bảng lỗi, mã 3) và dòng 135 (`needs-human`): cùng ý, và "đổi sang sẵn sàng thì `pr-check` mới chạy".
- `.claude/skills/merge-pr/SKILL.md`: PR nháp thì chuyển sang sẵn sàng trước; `wait-run.sh` mã 6.
- ADR-0010: một dòng ghi chú ở đầu dẫn tới ADR-0041, như ghi chú của ADR-0025, ADR-0039. ADR-0039: một dòng ghi chú ở đầu (giả định "nhánh vẫn chậm, `merge-if-green.sh` từ chối" ở mục Hệ quả không còn; cách `tests` thành đường thường). Không sửa nội dung đã `accepted`.
- `docs/foundation.md`: sinh lại bằng `scripts/foundation.py` khi trạng thái ADR-0041 đổi.

Không sửa: `.github/workflows/main-guard.yml` (`paths-ignore` giữ `docs/**`, `**/*.md`), `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh`, `scripts/is-exact-revert.sh`, `scripts/release.sh` (cấm sửa). Lý do ở bảng L-058, mục 4.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | Đường không cập nhật khi nhánh chính chỉ đổi tài liệu (mục 3.1 B), `mergeable` `null` đọc lại rồi chờ, `merged_on_main` nhận cây chỉ khác ở tài liệu, kiểm tra `skipped` là chờ, chú thích |
| Sửa | `scripts/main-guard-verified.sh` | Hàm `docs_only_reason`, `gate_ran`; lệnh `--docs-only`, `--gate-ran <sha>`; chú thích, kể cả câu ở dòng 23 (mục 3.1 A) |
| Sửa | `.github/workflows/pr-check.yml` | `types` đủ bốn loại, `if:` cấp job bỏ PR nháp, chú thích (mục 3.1 C) |
| Sửa | `scripts/wait-run.sh` | PR nháp: mã 6; lần chạy `skipped` không phải kết quả (mục 3.1 D) |
| Sửa | `scripts/tests/run.sh` | Test mục 4; viết lại câu `pr-check: build-and-test has no job-level if` và `merge-if-green: a skipped build-and-test is refused` (mục 3.5); sửa chú thích "is never skipped" ở đầu khối test `pr-check.yml` (dòng khoảng 187) thành "is skipped only for drafts" |
| Sửa | `docs/adr/0041-merge-khong-cap-nhat-khi-main-chi-doi-tai-lieu.md` | Chỉ khi rà soát độc lập của task thấy ADR lệch với code đã làm; không đổi `status` |
| Sửa | `docs/adr/0010-soft-quality-gate-github-free.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0041 |
| Sửa | `docs/adr/0039-main-guard-tin-lan-kiem-cua-pr.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0041 |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` khi cần |
| Sửa | `docs/auto-run.md` | Mã 6 của `wait-run.sh`; đường không cập nhật; PR nháp không chạy CI (mục 3.1 E) |
| Sửa | `CLAUDE.md` | Dòng `merge-if-green.sh --update` ở mục Lệnh |
| Sửa | `docs/ops/setup-new-app.md` | Dòng 133, 135 của bảng lỗi |
| Sửa | `.claude/skills/merge-pr/SKILL.md` | PR nháp, mã 6 của `wait-run.sh` |
| Sửa | `docs/features/quy-trinh/plans/T-077-bot-chay-lai-pr-check.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quy-trinh/tickets/T-077-bot-lan-chay-lai-pr-check.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020), tìm bằng `grep -rln -e main-guard-verified.sh -e wait-run.sh -e merge-if-green.sh -e pr-check.yml scripts .github .claude docs/auto-run.md docs/README.md docs/CLAUDE.md docs/ops CLAUDE.md`:

- `.github/workflows/main-guard.yml` gọi `main-guard-verified.sh <sha> <sự kiện> <lần chạy thứ>`: cách gọi và kết quả giữ nguyên (test có sẵn của T-074 giữ). Không sửa.
- `scripts/check.sh` dòng 4 (chú thích nhắc job `build-and-test`), `scripts/main-guard-status.sh` dòng 9, 56, 101 (nhắc `wait-run.sh --since`, chế độ không đổi), `scripts/is-exact-revert.sh` dòng 3 (nhắc `merge-if-green.sh`), `scripts/plan-check.py` dòng 74 (tên file rủi ro cao), `.github/workflows/device-session.yml` dòng 86 (chú thích nhắc `ui-test`, việc của T-075), `scripts/ui-test-needed.sh` dòng 12 (T-075, chú thích nhắc bộ lọc `ui` cũ của `pr-check.yml`): chỉ chú thích hay tên file. Không sửa.
- `scripts/tests/playbook/PB-02.log`, `docs/ops/failure-playbook.md` dòng 16 tới 18: dòng của `wait-run.sh` mã 4, 5 và `merge-if-green.sh`; mã mới 6 không phải lỗi, không cần dòng tra lỗi. Không sửa.
- `.claude/skills/quick-lane/SKILL.md`, `.claude/skills/whatnow/SKILL.md`, `.claude/skills/to-app/SKILL.md`, `docs/README.md`, `docs/ops/quick-lane.md`, `docs/ops/to-app.md`, `docs/ops/release.md`, `docs/CLAUDE.md`: nói "merge bằng `merge-if-green.sh`", không nói cập nhật nhánh hay PR nháp. Không sửa.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi, và plan không đổi tên hay xóa mục nào, chỉ thêm câu vào các dòng nêu ở mục 3.1 E:

- dẫn `docs/auto-run.md` (số mục trong chú thích hay lời giao phiên): `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py`, `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/token-usage.py`, `scripts/ui-test-verdict.sh` (T-075, thêm sau khi soạn plan);
- dẫn `CLAUDE.md` (quy ước chung, mục Lệnh): `.github/ISSUE_TEMPLATE/from-app.yml`, `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt`, `scripts/local-setup.sh`, `scripts/new-app.sh`, `scripts/run-watchdog.py`; plan chỉ thêm nửa câu vào một dòng của mục Lệnh;
- dẫn `docs/ops/setup-new-app.md`: `.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`, `scripts/java-check.sh`, `scripts/new-app.sh`, `scripts/setup-check.sh`; plan chỉ thêm ý vào hai dòng của bảng lỗi;
- `scripts/local-setup.sh` chỉ nhắc `scripts/tests/run.sh` (lệnh chạy sau khi cài), không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong `pr-check.yml` giữ nguyên (`contents: read`, `pull-requests: write`). `merge-if-green.sh` và `wait-run.sh` chạy bằng phiên đăng nhập `gh` của người hay phiên, đọc thêm `compare`, `pulls/N/files`, `actions/workflows/.../runs`, `actions/runs/N/jobs` (đều đã đọc ở nơi khác của hai script).
- **Thư viện và SDK:** Không thêm. Action có sẵn, không đổi phiên bản.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** GitHub ghi kết luận gì cho lần chạy `pr-check` mà mọi job bị `if:` bỏ qua (PR nháp): tài liệu và trải nghiệm cho thấy `skipped`, nhưng chưa thấy trên repo này. Nếu là `success`, `wait-run.sh --pr` gọi ngay sau khi chuyển PR sang sẵn sàng có thể báo `XANH` cho lần chạy cũ.
  - **Điều kiện chuyển:** lần đầu một PR nháp được mở sau khi merge (mục 4, dòng kiểm tra thủ công) cho thấy lần chạy có `conclusion` khác `skipped`.
  - **Cách làm thay:** PR sửa nhỏ sau merge: `wait-run.sh --pr` bỏ qua lần chạy có `event` là `pull_request` mà mọi job `skipped` (đọc `actions/runs/<id>/jobs`, đã đọc khi đỏ). Không ảnh hưởng an toàn merge: `merge-if-green.sh` đọc check run của từng job, job `skipped` là chờ, không bao giờ xanh. Tiêu chí và test ở mục 4 giữ nguyên.
- **Điểm chưa chắc 2:** trạng thái `pr-check.yml` sau T-075, T-076 khác giả định ở mục 2 (job có sẵn một `if:` cấp job, hay bước lọc đổi tên).
  - **Điều kiện chuyển:** đọc `pr-check.yml` lúc bắt đầu task thấy job `build-and-test` đã có `if:` cấp job.
  - **Cách làm thay:** nối điều kiện nháp vào `if:` đó bằng `&&` (`github.event.pull_request.draft == false && (<điều kiện cũ>)`); test 1 kiểm dòng `if:` có `github.event.pull_request.draft == false` và đúng một dòng `if:` cấp job. Tên job hay tên bước cổng đổi thì dừng (mục 7).

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin; mục này ghi các câu khẳng định có sẵn của `scripts/tests/run.sh` mà hành vi mới chạm (ADR-0031).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Job `build-and-test` không bao giờ bị bỏ qua (L-040) | "Job `build-and-test`: `if: github.event.pull_request.draft == false` ở cấp job ... Đây là `if:` cấp job duy nhất" (mục 3.1 C) | `pr-check: build-and-test has no job-level if` (mong `0`) và bản sao hỏng `job-if.yml` (mong `1`) | Viết lại. Ý định: PR không nháp nào bỏ qua được `build-and-test`, kể cả PR không có code. Câu mới: `pr-check: build-and-test has one job-level if, and it only skips drafts` mong `1 yes` (số dòng `if:` cấp job, và dòng đó đúng bằng `    if: github.event.pull_request.draft == false`). Bản sao hỏng đặt `if:` theo bộ lọc code (hay thêm một `if:` thứ hai) mong `1 no` hay `2 ...`, tức thất bại. Không bớt câu, có đối chứng |
| Nhánh chậm → luôn mã 3 hay cập nhật | Chậm mà nhánh chính chỉ đổi tài liệu → không cập nhật | `merge-if-green: without --update a branch behind main exits 3`, `merge-if-green --update: updates, waits for CI and merges`, các test `merge_during_run` mong 3, `merge_while_broken` mong 3 | Giữ nguyên: dữ liệu của chúng không có đường `compare/<đầu PR>...main` (gh giả trả 404), nên `docs_only_behind` dừng ở bước 1 ("không đọc được → cập nhật như cũ") trước khi xét `mergeable` (bước 4, đứng cuối chính vì lẽ này): `pulls/12` của chúng không có `mergeable` (đọc ra `null`) mà không bao giờ được xét, và không có lần đọc lại nào tiêu phần tử `__sequence__` của test `--update` |
| Kiểm tra bắt buộc `skipped` bị từ chối (mã 1) | "kiểm tra bắt buộc có `conclusion` `skipped` → `pending` ... Không bao giờ coi là xanh" (mục 3.1 B) | `merge-if-green: a skipped build-and-test is refused` (mong `1 1`: mã 1 và câu `kết thúc với kết quả 'skipped'`) | Viết lại. Ý định: một `build-and-test` bị bỏ qua không bao giờ mở đường merge. Câu mới: `merge-if-green: a skipped build-and-test is never green: it waits` mong `2 1 0` (mã 2, có câu `bị bỏ qua (lần chạy lúc PR còn nháp)`, không có lệnh ghi nào); thêm `--update` với `MERGE_UPDATE_TIMEOUT_SECONDS=0` mong `2` và không có `PUT pulls/12/merge`. Đối chứng: cùng dữ liệu với `build-and-test` `success` mong `0` (test có sẵn `a docs-only PR whose build-and-test passed is ready`). Đối chứng phá ý định: chạy câu mới trên bản sao `$work/mgi-skip-green.sh` của `merge-if-green.sh` mà `check_one` coi `skipped` là xanh (sed đổi điều kiện `allow_skip` thành luôn đúng), mong câu mới thất bại (`0 ...` khác `2 1 0`). Không nới: câu cũ kiểm "không merge", câu mới vẫn kiểm không merge và thêm kiểm không ghi gì |
| `merged_on_main` chỉ nhận cây bằng cây của PR | Nhận thêm cây chỉ khác ở tài liệu; khác ngoài tài liệu thì từ chối (mục 3.1 B) | Khối `mig_run` (`merge-if-green reports a PR already on main ...`, cây bằng nhau) | Giữ nguyên: dữ liệu của chúng có cây bằng nhau, đi bước 2 như nay |
| `wait-run.sh --pr` lấy lần chạy mới nhất, mọi kết luận | PR nháp → mã 6; lần chạy `skipped` → chờ tiếp | Các test `wait-run:` có sẵn (dữ liệu không có `draft`, không có lần chạy `skipped`) | Giữ nguyên |
| `main-guard-verified.sh <sha> ...` | Không đổi | 50 câu `main-guard-verified:` của T-074 | Giữ nguyên |

Câu `pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml` (mong `names=N missing= none`) do T-075, T-076 viết lại; plan này không chạm.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`, `gh` giả là `scripts/tests/fake-gh` với `FIXTURE` (`routes`, `errors`, `__sequence__`, ghi lệnh ghi vào `$FIXTURE.writes`). Không cần đổi `fake-gh`: mọi đường mới là đường đọc có tên cố định.

Dữ liệu chung của khối `merge-if-green` mới (hàm phụ trợ `nu_routes`, vào commit test của bước 3, L-025): từ `merge_routes claude/T-901-x '{"behind_by": 2}' "$green_runs"` (nhánh `claude/T-` để không đụng phiên), thêm bằng `python3 -I`:

- `pulls/12`: `"mergeable": true` (tham số để đổi);
- `compare/abc1234...main`: `{"files": <danh sách nhánh chính>}`, mặc định `[{"filename": "docs/runs/RUN-20261030-01.md"}, {"filename": "docs/lessons-2.md"}]`;
- `pulls/12/files?per_page=100&page=1`: `[{"filename": "app/src/main/x.kt"}, {"filename": "docs/features/f/plans/T-901.md"}]`;
- `actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10`: run 91 `success`; `actions/runs/91/jobs?per_page=50`: job `build-and-test`, bước `Cổng chất lượng (scripts/check.sh)` với kết luận là tham số (mặc định `success`).

`main-guard-verified.sh` thật được gọi (không thay bằng bản giả), với `remote.origin.url` giả có sẵn của `run.sh` (dòng 19). Kết quả đọc bằng mã thoát, `$work/mg.out` và `$work/mg.json.writes`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| DoD 1: `types` | Test cấu trúc | 1. `pr-check: runs on opened, synchronize, reopened and ready_for_review`: trong khối `pull_request:` có đúng dòng `    types: [opened, synchronize, reopened, ready_for_review]` và vẫn có `branches: [main]`; mong `1 1`. Bản sao hỏng trong `$work/prwf` bỏ `synchronize`: mong khác `1 1` |
| DoD 1: bỏ nháp ở cấp job | Test cấu trúc | 2. Câu viết lại ở mục 3.5: `pr-check: build-and-test has one job-level if, and it only skips drafts` mong `1 yes`; bản sao `if:` theo bộ lọc code, bản sao không có `if:` (mong `0 no`), bản sao thêm `if:` thứ hai: mỗi bản thất bại |
| DoD 1: `wait-run.sh` với PR nháp | Test của script | 3. `wait-run: a draft PR stops at once with code 6`: `pulls/7` có `"draft": true`, không có lần chạy; mong `6 NHÁP` (mã và từ đầu dòng), và không chờ (`WAIT_RUN_TIMEOUT_SECONDS=0` vẫn ra 6, không ra 2). Đối chứng: cùng dữ liệu với `"draft": false` mong `2 CHƯA` |
| DoD 1: chuyển sang sẵn sàng | Test của script | 4. `wait-run: a skipped run left from the draft is not a result`: `pulls/7` không nháp, `actions/workflows/pr-check.yml/runs?head_sha=abc1234&per_page=10` là `__sequence__` [lần chạy 90 `completed`/`skipped`] rồi [lần chạy 91 `completed`/`success` đứng đầu, 90 sau]; mong `0 XANH` với `lần chạy 91`. Đối chứng: chỉ có lần 90 `skipped`, hết giờ → `2 CHƯA` (không phải `1 ĐỎ`) |
| DoD 2: không cập nhật | Test của script | 5. `merge-if-green: a code PR behind main by docs only merges without updating`: `nu_routes` mặc định, `merge_run 12` mong `0`; `$work/mg.json.writes` đúng `PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x;` (không có `update-branch`); `mg.out` có dòng `KHÔNG CẬP NHẬT NHÁNH` và `ĐÃ MERGE: PR #12 (t) tại commit abc1234`. Cùng dữ liệu `--dry-run` mong `0`, có `KHÔNG CẬP NHẬT NHÁNH` và `SẴN SÀNG`, không có lệnh ghi; `--update` mong `0`, không có `update-branch` |
| DoD 2: nhánh chính có code | Test của script | 6. `merge-if-green: main changed code since the split, so the branch is updated`: `compare` thêm `{"filename": "core/x.kt"}`; không `--update` mong `3` và `mg.out` có `core/x.kt`; dữ liệu `__sequence__` như test có sẵn `--update: updates, waits for CI and merges` (đầu PR thành `def5678`) mong `0` và lệnh ghi bắt đầu bằng `PUT pulls/12/update-branch` |
| DoD 2: file `docs/` mà Gradle đọc | Test của script | 7. `merge-if-green: a docs file Gradle reads counts as code`: `compare` là `[{"filename": "docs/remote-config-keys.md"}]`, rồi `[{"filename": "docs/design-system/tokens.json"}]`; mong `3` cả hai. Đối chứng cùng test: `[{"filename": "docs/other.json"}]` mong `0` |
| DoD 2: PR chỉ tài liệu luôn cập nhật | Test của script | 8. `merge-if-green: a PR that did not run check.sh is always updated`: bước cổng `skipped`; mong `3`, `mg.out` có `PR không chạy check.sh`. Thêm: không có job `build-and-test` trong `jobs` mong `3`; run `pr-check` mới nhất `failure` (dù check run xanh: đọc lệch) mong `3`. Đối chứng: bước cổng `success` mong `0` (test 5) |
| Xung đột thì cập nhật; chưa tính xong thì chờ | Test của script | 9. `merge-if-green: a conflicting PR updates, an unknown mergeable state waits`: `mergeable` `false` mong `3`; `pulls/12` là `__sequence__` [`null`, `null`, `true`] (`MERGE_MERGEABLE_WAIT_SECONDS=0`) mong `0` không có `update-branch`; luôn `null` mong `2` và có `chưa tính xong`, `--update` với `MERGE_UPDATE_TIMEOUT_SECONDS=0` mong `2` và không có `update-branch`; `mergeable` là chuỗi lạ mong `3` |
| Không trùng file hai bên | Test của script | 10. `merge-if-green: a file changed on both sides updates`: `compare` có `docs/features/f/plans/T-901.md` (PR cũng đổi); mong `3`, `mg.out` có `cả PR và nhánh chính cùng đổi`. Ca đổi tên: PR có `previous_filename` là `docs/lessons-2.md` mong `3` |
| Đổi tên trên nhánh chính | Test của script | 11. `merge-if-green: a rename on main counts its old path`: `compare` là `[{"filename": "docs/x.kt.md", "previous_filename": "app/x.kt", "status": "renamed"}]`; mong `3` |
| Không đọc được thì cập nhật | Test của script | 12. `merge-if-green: an unreadable answer updates`: `errors` cho `compare/abc1234...main` (`HTTP 500`), rồi `compare` không có `files`, rồi `files` 300 dòng `docs/a<n>.md`, rồi `errors` cho `pulls/12/files?per_page=100&page=1`, rồi `errors` cho `actions/runs/91/jobs?per_page=50`; mong `3` mỗi ca. Thêm: `MAIN_GUARD_VERIFIED_SCRIPT` chỉ tới một tệp không chạy được mong `3`; chỉ tới một tệp luôn in `full` (cách bản `main-guard-verified.sh` cũ trả lời `--docs-only`) mong `3` |
| CI còn chạy | Test của script | 13. `merge-if-green: behind by docs with CI still going waits`: `check-runs` là `lagging_runs` có sẵn với lần chạy `in_progress`; mong `2` và `mg.out` có `CI chưa chạy xong` (không phải `3`). Ca chưa có check run nào trên commit (`{"check_runs": []}`) mong `2` và có `CI chưa chạy trên commit này` |
| `merged_on_main` trên đường không cập nhật (L-080) | Test của script | 27. `merge-if-green: a no-update merge that GitHub answered with an error is found on main`: dựng như `merge_while_broken` có sẵn: repo tạm `$work/nu` và repo bare `$work/nu-origin.git`, giữ URL GitHub giả của `run.sh` (dòng 19, để regex lấy repo ở dòng 91 của `merge-if-green.sh` khớp) và đặt `GIT_CONFIG_COUNT=2`, `GIT_CONFIG_KEY_1="url.$work/nu-origin.git.insteadOf"`, `GIT_CONFIG_VALUE_1=https://github.com/o/r.git` cho riêng lệnh gọi script (không bỏ `GIT_CONFIG_*`). `A` cha; `H` = `A` + `app/y.kt` (đầu PR, `refs/pull/12/head` của repo bare); `D` = `A` + `docs/runs/r.md`; `M` = cây `D` + `app/y.kt`, cha `D`, tiêu đề `t (#12)`, là `main` của repo bare. Route: `nu_routes` (test 5), thay mọi `abc1234` bằng sha đầy đủ của `H` (`pulls/12` `head.sha`, `compare/<H>...main` có `docs/runs/r.md`, `commits/<H>/check-runs?per_page=100`, `commits/<H>`, `actions/workflows/pr-check.yml/runs?head_sha=<H>&per_page=10`), thêm `git/commits/<H>` trả cây của `H`, `commits?sha=main&per_page=30` là `__sequence__` [không có `M`, rồi có `M` với tiêu đề `t (#12)` và cây của `M`], `fail_writes` có `PUT pulls/12/merge`; `MERGE_RECHECK_SECONDS=0`. Mong `0`, `mg.out` có `ĐÃ MERGE (GitHub báo lỗi): PR #12 (t) tại commit <7 ký tự của M>`, lệnh ghi có `POST actions/workflows/main-guard.yml/dispatches`. Đối chứng, mỗi ca kiểm cả mã và câu: (a) `M` thêm `app/z.kt` (khác ngoài tài liệu) mong `1` và `mg.out` có `cùng tiêu đề 't (#12)' trên nhánh chính nhưng cây khác commit của PR ngoài tài liệu; không merge lại, người xem.`; (b) repo bare không có `refs/pull/12/head` và không có nhánh của PR (fetch lỗi) mong `1` và cùng câu đó; (c) nhận nhầm (rà soát lại, điểm nên sửa 1): `M` dựng từ đầu cũ `H0` = `A` + `app/y.kt`, rồi PR có đầu `H1` = `H0` + `docs/x.md` (route và `refs/pull/12/head` trỏ `H1`), `commits?sha=main&per_page=30` có `M` ngay từ đầu: mong `1`, `mg.out` có câu trên và không có `ĐÃ MERGE TRƯỚC ĐÓ`. Đối chứng của (c): cùng dựng mà `docs/x.md` nằm trong `D` (nhánh chính đổi) thay vì trong `H1` → `0` và `ĐÃ MERGE TRƯỚC ĐÓ` |
| Hai ticket khớp nhau: cùng quy tắc tài liệu | Test của script | 14. `main-guard-verified: --docs-only agrees with the mode main-guard picks`: bảng đường dẫn `docs/a.md`, `README.md`, `.claude/skills/x/SKILL.md`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/design-system/tokens.json`, `docs/x.json`, `app/x.kt`, `scripts/a.sh`, `docs.kt`. Với mỗi dòng: `--docs-only` trên dòng đó, và cách chọn của `main-guard-verified.sh <M>` trên repo tạm của khối T-074 (`"$work/mgv"`, `M` = cây của `H` cộng đúng file đó, dữ liệu `mgv_fx` của test 1 T-074). Mong `yes` ⇔ `tests`, `no` ⇔ `full`, cho cả mười dòng; mong cụ thể `yes yes yes no no no yes no no no`. Dòng `--docs-only` với danh sách rỗng mong `yes` |
| Hai ticket khớp nhau: `paths-ignore` | Test cấu trúc | 15. `main-guard-verified: --docs-only accepts only what main-guard ignores`: đọc hai mẫu `paths-ignore` của `main-guard.yml` (test có sẵn đã giữ đúng hai dòng `docs/**`, `**/*.md`); với mỗi dòng mà test 14 mong `yes`, kiểm dòng đó khớp một trong hai mẫu (bằng `[[ $f == docs/* \|\| $f == *.md ]]` sau khi đọc đúng hai mẫu ấy từ workflow; mẫu khác thì test báo `unknown pattern`). Bản sao hỏng của `main-guard.yml` thêm `'scripts/**'` vào `paths-ignore`: mong `unknown pattern` |
| Hai ticket khớp nhau: commit merge dựng thật | Test của script | 16. `merge-if-green and main-guard: a PR merged without updating is checked with tests`: trong `"$work/mgv"`, `A` cha; `H` = `A` + `app/y.kt` (PR); `D` = `A` + `docs/runs/r.md` (nhánh chính); `M` = commit có cây `D` + `app/y.kt`, cha `D`. `git diff --name-only A D` qua `--docs-only` mong `yes`; `main-guard-verified.sh M` với đường `commits/M/pulls` trỏ PR 7 `head.sha` `H` mong `0 tests`. Đối chứng: `D` đổi `docs/remote-config-keys.md` thì `--docs-only` `no` và `main-guard-verified.sh M` `0 full` |
| `--gate-ran` | Test của script | 17. `main-guard-verified: --gate-ran reads the newest pr-check run`: dữ liệu `mgv_fx` của T-074; bước cổng `success` mong `0 yes`; `skipped` mong `0 no`; run mới nhất `failure` mong `0 no`; `errors` cho `actions/runs/91/jobs?per_page=50` mong `0 no`; không có tham số sha mong `0 no` |
| Mọi kiểm tra hiện có (L-058) | Bảng dưới | Test 18 tới 27 |
| DoD 1, DoD 2 sau khi merge | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Sau merge: (a) mở một PR nháp, xem tab Actions có lần `pr-check` với job `build-and-test` `skipped` và kết luận của lần chạy (mục 3.4, điểm 1), chuyển sang sẵn sàng thì lần chạy mới chạy `build-and-test`; (b) lần đầu một PR code chậm hơn nhánh chính chỉ vì tài liệu: `merge-if-green.sh` in `KHÔNG CẬP NHẬT NHÁNH`, `main-guard` của commit merge in `CHỈ KHÁC TÀI LIỆU` và xanh, và khi PR đổi code app thì job `ui-test` của `main-guard` (T-075 chuyển test emulator sang sau merge) chạy trên commit merge. Ghi vào nhật ký |

**Bảng L-058: trường hợp mới (PR code chậm hơn nhánh chính chỉ vì tài liệu, dữ liệu của test 5) qua từng kiểm tra hiện có của `merge-if-green.sh`.** Mỗi dòng là một test mới, dùng `nu_routes` rồi đổi đúng một điều.

| Kiểm tra hiện có | Trường hợp mới | Test |
| --- | --- | --- |
| Tham số lạ bị từ chối | Không đổi (trước mọi lệnh đọc) | Test có sẵn `merge-if-green rejects '...'`, giữ |
| PR không mở | Bị từ chối | 18. `... behind by docs: a closed PR is still refused`: `state` `closed` mong `1` |
| PR đã nằm trên main | Báo `ĐÃ MERGE TRƯỚC ĐÓ`, không ghi gì | 19. `... behind by docs: a PR already on main is reported, not merged`: dữ liệu giống thực tế của đường mới: repo tạm của test 27, `commits?sha=main&per_page=30` có `M` (tiêu đề `t (#12)`, cây của `M` khác cây của `H` ở `docs/runs/r.md`); mong `0`, có `ĐÃ MERGE TRƯỚC ĐÓ`, không có lệnh ghi, không có `update-branch`. Ca cây bằng nhau: khối `mig_run` có sẵn |
| PR nháp | Bị từ chối | 20. `... behind by docs: a draft is still refused`: `draft` `true` mong `1`, `mg.out` có `đang là bản nháp` |
| Nhãn `needs-human` | Bị từ chối | 21. `... behind by docs: needs-human is still refused`: mong `1` |
| Nhắm nhánh khác | Bị từ chối | 22. `... behind by docs: a PR into another branch is still refused`: `base.ref` `other` mong `1` |
| `main-broken` đang mở (kể cả bản revert đúng) | Bị từ chối; bản revert đúng phải nằm ngay trên nhánh chính mới nhất nên không bao giờ "chậm" | 23. `... behind by docs: main-broken still refuses`: dựng như `merge_while_broken` có sẵn (repo tạm `$rev`, issue `main-broken` có `culprit`, `IS_EXACT_REVERT` là bản giả thoát 1) cộng các đường của `nu_routes`; mong `1`, không có lệnh ghi. Kiểm thêm thứ tự: `mg.out` không có `KHÔNG CẬP NHẬT NHÁNH` |
| Phiên tự chạy đang chạy | PR của người bị từ chối; PR `claude/T-` của phiên qua | 24. `... behind by docs: a running session still refuses a person's PR`: nhánh `claude/template-x`, nhánh báo cáo `running` như `merge_during_run`; mong `1`. Đối chứng: `claude/T-901-x` mong `0` |
| Nhánh cập nhật theo nhánh chính | Đây là điều được nới: chỉ khi đủ bốn điều (test 5 tới 13) | Test 5 tới 13 |
| Kiểm tra đỏ trên commit cuối | Bị từ chối (`skipped`: chờ, không merge) | 25. `... behind by docs: a red check is still refused`: `red_docs_runs` có sẵn mong `1` và có `có kiểm tra không qua`. Thêm: `skipped_docs_runs` mong `2` (chờ), không có lệnh ghi |
| Kiểm tra còn chạy, job chậm báo (L-017) | Chờ (mã 2), hay coi xanh khi lần chạy đã xanh | Test 13; 26. `... behind by docs: a lagging job of a green run counts as green`: `lagging_runs` cùng `actions/runs?head_sha=abc1234&per_page=50` xanh như test có sẵn, mong `0` |
| Merge squash ghim `sha`, cảnh báo commit mới, `docs/STATUS.md`, xóa nhánh `claude/` | Như nhánh không chậm | Test 5 (lệnh ghi `PUT pulls/12/merge` rồi `DELETE`); dòng `merge -f sha=` không đổi (không có test mới: phần code không chạm) |
| `ĐÃ MERGE (GitHub báo lỗi)` gọi `main-guard` | Như cũ; lần gọi tay của `main-guard` là `full` (ADR-0039) | Test có sẵn `merge-if-green asks for main-guard after such a merge`, giữ; test 27 cho đường không cập nhật |
| `MAX_UPDATES`, vòng chờ `--update` | Không cập nhật nên không tính lần | Test 5 (`--update` mong `0`, không có `update-branch`) |

**Ngoại lệ viết test trước:** sửa tài liệu (`docs/auto-run.md`, `CLAUDE.md`, `docs/ops/setup-new-app.md`, `.claude/skills/merge-pr/SKILL.md`, ghi chú ADR-0010, ADR-0039, `foundation.md`) và chú thích trong `pr-check.yml`, `merge-if-green.sh`, `main-guard-verified.sh`, `wait-run.sh`: không có hành vi để kiểm.

### 4.1. Chạy thử

Không cần (E dưới 2): `gh api` với `fake-gh`, `compare`, `pulls/N/files` (`gh_all`), đọc bước của job, repo git tạm, `if:` cấp job và `types` của `pull_request` đều đã dùng trong dự án hay là cú pháp có tài liệu. Phần chưa chạy thử được trên máy là kết luận GitHub ghi cho lần chạy của PR nháp (mục 3.4, điểm 1; kiểm tra thủ công ở mục 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `scripts/merge-if-green.sh`, `.github/workflows/pr-check.yml`, `scripts/main-guard-verified.sh`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Ba script (một là script merge), một workflow, test, sáu tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu rõ ba trường hợp; plan thêm điều "không trùng file" chặt hơn ticket và còn câu hỏi về `main-broken` sau merge (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh quyết có test với `gh` giả và repo git tạm; kết luận của lần chạy PR nháp và lần đầu merge không cập nhật chỉ thấy trên GitHub thật |
| D. Ảnh hưởng chéo | 3 | Đổi luật merge mà mọi PR, phiên tự chạy và `main-guard` dựa vào; T-075, T-076 sửa cùng `pr-check.yml`, `merge-if-green.sh`, `scripts/tests/run.sh` |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn (`main-guard-verified.sh`, `wait-run.sh`, `gh_all`); `types` của `pull_request` và `if:` theo `draft` lần đầu dùng ở repo này |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `merge-if-green.sh` (mọi lần merge), `pr-check.yml` (mọi PR), `main-guard-verified.sh` (mọi commit của main, cách chọn không đổi), `wait-run.sh` (mọi lần chờ CI của phiên).
- **T-075** (ADR-0043) bỏ `ui-test`: sửa `pr-check.yml`, `SKIPPABLE_CHECKS` của `merge-if-green.sh`, các test `pr-check:`, `docs/auto-run.md`. **T-076** bỏ job `changes`: sửa `pr-check.yml` (bước lọc vào job, `if:` của các bước), `REQUIRED_CHECKS`, các test `pr-check:` (kể cả `pr_job_if`, bản sao `job-if.yml` hiện chèn sau `needs: changes`). T-077 làm sau cả hai (`depends_on: [T-074, T-075, T-076]`). Lúc bắt đầu task đối chiếu lại: số dòng trong mục 3.1, tên test có sẵn ở mục 3.5, cách dựng bản sao hỏng của test 2. Plan nào merge trước mà đổi giả định ở mục 2 thì plan này cần phiên bản mới trước khi chạy.
- **T-078** (ADR-0042, runner tự host) có thể đổi `runs-on` của `pr-check.yml`: không chạm `on:` hay `if:` của job; ai làm sau thì gộp.
- `main-guard.yml` không sửa; `paths-ignore` của nó là một nửa của quy tắc chung, test 15 giữ khớp.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow, script và test là của dự án mẫu.
- Lỗ hổng có từ trước, ngoài phạm vi, chỉ ghi lại: `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSdkUseTest.kt` duyệt cả cây repo, kể cả `docs/`, nên một file trong `docs/` có thể làm test Gradle đỏ mà không nằm trong `GRADLE_READ_DOCS` (danh sách chỉ bắt dạng `rootProject.file("docs/...")`, ADR-0039). Tương tự, `docs/remote-config-keys.md` đổi một mình trên nhánh chính không kích hoạt `main-guard` (`paths-ignore` `docs/**`). Plan không đổi hai điểm này; với T-077 chúng chỉ nghĩa là quy tắc "chỉ tài liệu" kế thừa đúng các giới hạn của T-074.
- Đề xuất thứ tự: làm trong phiên có người, sau khi T-075, T-076 merge và `main-guard` của chúng xanh.

## 7. Điều kiện dừng riêng

- T-075 hay T-076 chưa merge: không bắt đầu.
- Tên job `build-and-test` hay tên bước `Cổng chất lượng (scripts/check.sh)` đã đổi so với `main-guard-verified.sh`: dừng (hai ticket lệch nhau, cần plan phiên bản mới).
- Phải đổi một câu khẳng định có sẵn của `main-guard-verified:` (T-074) để code qua: dừng; cách chọn của `main-guard` không được đổi.
- Cần thêm quyền ghi cho `GITHUB_TOKEN` của `pr-check.yml`, hay bí mật mới: dừng.
- Sau merge, `merge-if-green.sh` merge không cập nhật một PR mà `main-guard` của commit merge chọn `full` vì tài liệu (không phải vì file cổng hay commit chen vào): hai bên lệch nhau; revert cùng dev.

## 8. Câu hỏi cho người duyệt

1. **ADR-0041.** Đồng ý thì khi duyệt plan ghi ADR-0041 `status: accepted`, chạy `scripts/foundation.py`, commit cùng lần duyệt (`docs/CLAUDE.md`, mục Khi viết ADR). Số 0041 đã giữ chỗ (T-075 dùng 0043 vì 0040 thuộc T-080, T-078 dùng 0042). Chọn ADR mới thay vì thêm mục vào ADR-0039 vì: ADR-0039 đã `accepted` (không sửa nội dung, `docs/CLAUDE.md`), và quyết định này thay luật merge của ADR-0010 mục 1 và mục 4, không phải luật của `main-guard`.
2. **Điều "không file nào đổi ở cả hai bên"** chặt hơn ticket: PR code cùng sửa `docs/lessons.md`, `docs/foundation.md` hay cùng một plan với nhánh chính thì vẫn cập nhật. Đề xuất giữ, để bài học trùng số và `foundation.md` lệch được bắt trước merge trong ca hay gặp nhất. Bỏ thì bớt thêm lần chạy nhưng `main-broken` dễ xảy ra hơn.
3. **Tổ hợp tài liệu ở file khác nhau chỉ bị bắt sau merge** (ví dụ PR thêm ADR mới cùng số với ADR vừa vào nhánh chính ở tên file khác): `main-guard` cách `tests` đỏ, mở `main-broken`, chặn mọi lần merge tới khi người xử lý. Ticket đã nêu; đề xuất chấp nhận.
4. **PR chỉ đổi script** (bước cổng `check.sh` có chạy vì `scripts/**` thuộc bộ lọc code): được merge không cập nhật như PR code. PR chỉ tài liệu luôn cập nhật. Đề xuất đồng ý.
5. **Mã thoát mới:** `wait-run.sh` mã 6 cho PR nháp; `merge-if-green.sh` không `--update` trả 2 thay vì 3 khi nhánh chậm vì tài liệu mà CI còn chạy (ADR-0041, Hệ quả). Đề xuất đồng ý.
6. **Thứ tự với T-075, T-076** (mục 2, mục 6): plan giả định trạng thái sau cả hai. Nếu một trong hai bị bỏ hay đổi hướng, plan cần phiên bản mới. Đồng ý chờ?
7. **`main-guard` đỏ ở cách `tests` chỉ vì tổ hợp tài liệu** sau một merge không cập nhật: theo `auto-run.md` bước 9 hiện nay, phiên revert cả task code dù code không sai. Plan không đổi bước 9 (quy trình phiên, ngoài phạm vi ticket); ADR-0041 ghi vào mục Phải chấp nhận. Đề xuất: người xử lý `main-broken` loại này sửa bằng một PR tài liệu (ví dụ đổi số bài học, sinh lại `foundation.md`) thay vì revert; nếu đồng ý thì mở ticket riêng sửa `auto-run.md` bước 9 để phiên cũng làm vậy. Đồng ý?

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30, sau khi T-075 merge (PR #324) và plan T-076 được duyệt (PR #315). Bảy câu ở mục 8 theo đề xuất của plan: (1) ADR-0041 ghi `accepted`, `docs/foundation.md` sinh lại cùng lần duyệt; (2) giữ điều "không file nào đổi ở cả hai bên"; (3) chấp nhận tổ hợp tài liệu ở file khác nhau chỉ bị bắt sau merge; (4) PR chỉ đổi script được merge không cập nhật như PR code; (5) đồng ý mã 6 của `wait-run.sh` và mã 2 của `merge-if-green.sh` khi chậm vì tài liệu mà CI chưa xong; (6) chỉ bắt đầu task sau khi T-076 merge; (7) `main-guard` đỏ ở cách `tests` chỉ vì tổ hợp tài liệu thì sửa bằng PR tài liệu, không revert; đổi `docs/auto-run.md` bước 9 là ticket riêng. Đối chiếu lúc duyệt: trạng thái sau T-075 khớp mục 2 (không còn `SKIPPABLE_CHECKS`, tên job và bước cổng giữ, regex lấy repo ở dòng 90 của `merge-if-green.sh`); trạng thái sau T-076 lấy theo plan T-076 đã duyệt. Task sửa trong file của mục 3.2, như điểm nên sửa (ADR-0029), bốn điều sau: (a) test M3 của T-076 (`merge-if-green: a skipped build-and-test is refused in the one-job shape`, mong mã 1) cũng khẳng định hành vi cũ "kiểm tra bắt buộc `skipped` bị từ chối": viết lại cùng lúc và cùng cách với câu `a skipped build-and-test is refused` ở mục 3.5 (mong `2 1 0`, không lệnh ghi), không tính vào giới hạn 3 câu của ADR-0031 vì là test viết trước của plan; (b) đối chứng phá ý định của câu `skipped` không dựa vào dòng `allow_skip` (T-075 đã bỏ): bản sao `$work/mgi-skip-green.sh` dùng `sed` thay chính dòng `pending "... bị bỏ qua (lần chạy lúc PR còn nháp) ..."` bằng `return 0`, kèm `! cmp -s` với bản gốc để chắc bản sao đã khác; (c) `merged_on_main` bước 3: merge-base lỗi hay rỗng thì đi bước 4 (từ chối, người xem), lệnh `git diff` có ngoặc kép quanh merge-base; thêm điều không file nào trong tập khác nhau giữa `HEAD_SHA` và commit thuộc `diff --name-only <merge-base> HEAD_SHA` (PR và nhánh chính cùng đổi một file tài liệu thì không nhận là đã merge); test 27 thêm hai ca đối chứng, merge-base rỗng (mong `1`) và (d) H1 sửa `docs/lessons-2.md` mà `D` cũng đổi (mong `1`, không có `ĐÃ MERGE TRƯỚC ĐÓ`), ca (c) cập nhật route `git/commits/<H1>`; (e) test 27 ghi dòng 90 (không phải 91) cho regex lấy repo. Task làm có người (`auto_run: false`).

## 10. Nhật ký thực hiện
