---
ticket: T-074
title: main-guard không chạy lại cổng chất lượng cho nội dung đã kiểm ở PR
status: draft
version: 1
risk_score: 8
risk_level: high
token_estimate: 23.9M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-074: MAIN-GUARD KHÔNG CHẠY LẠI CỔNG CHẤT LƯỢNG CHO NỘI DUNG ĐÃ KIỂM Ở PR

## 1. Tóm tắt

Script mới `scripts/main-guard-verified.sh` quyết định job `guard` của `main-guard.yml` kiểm thế nào: `skip` (cây file giống hệt commit mà `pr-check` đã chạy `check.sh` xanh), `tests` (chỉ khác ở tài liệu: chạy `scripts/tests/run.sh`), hay `full` (như nay). Bỏ qua là một bước bên trong job `guard`, nên check run `guard` vẫn `success` và mọi nơi đọc nó không đổi. Script không chắc thì chọn `full`. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả và một repo git tạm, cùng test cấu trúc của workflow. Theo ADR-0039 (đề xuất cùng PR plan này).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 2. Quyết định của anh Nhật ngày 2026-10-29: áp dụng cả sáu cách; T-074 tới T-077 làm có người (`auto_run: false`).

| Yêu cầu (định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| DoD 1 | Commit merge từ PR đã kiểm, cùng nội dung: job `guard` xong trong dưới 1 phút, log ghi số PR và commit đã kiểm | Cách `skip`: không cài Gradle, không chạy `check.sh`; dòng log `ĐÃ KIỂM Ở PR #N (commit <sha>)` |
| DoD 1 | Chỉ khác tài liệu: chỉ chạy `scripts/tests/run.sh` | Cách `tests`: cài Java (run.sh cần), không cài Gradle |
| DoD 2 | Đẩy thẳng lên main, khác ngoài tài liệu, PR chỉ chạy test của script, commit chạm workflow và script của cổng: chạy `check.sh` như cũ | Cách `full`. Thêm: chạy tay, không đọc được GitHub, không lấy được commit đã kiểm |
| DoD 3 | `find-breaking-commit.sh`, `main-guard-status.sh`, `start-run.yml`, tự đóng `main-broken` đúng với kết quả mới; `release.sh` không đổi | Check run `guard` vẫn có kết luận `success`/`failure` như nay; xem bảng L-058 ở mục 4 |
| DoD 4 | ADR mới `accepted`, `foundation.md` sinh lại | ADR-0039 viết trong PR plan này ở trạng thái `proposed`; người ghi `accepted` khi duyệt plan (mục 8, câu 1) |
| DoD 5 | CI qua, đã merge | Như mọi task |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (mục 2 bị thay một phần), ADR-0025 (chạy lại test hỏng, tự đóng `main-broken`), ADR-0039 (mới), ADR-0029 (phạm vi file).

## 3. Giải pháp

### 3.1. Cách làm

**Script `scripts/main-guard-verified.sh <sha> [<sự kiện> [<lần chạy thứ>]]`.** In đúng một dòng ra stdout: `full`, `tests` hay `skip`. Lý do in ra stderr (vào log của job). Luôn thoát 0: mọi lỗi (gọi `gh` thất bại, JSON lạ, `git` lỗi) cho ra `full`. Theo lối của `main-guard-status.sh`: bash, `set -euo pipefail`, chỉ dùng `gh api` và `git`, `REPO` lấy từ `GITHUB_REPOSITORY` hay `remote.origin.url`. Các hằng khai báo một lần ở đầu script: `GATE_FILES`, `GATE_JOB="build-and-test"`, `GATE_STEP="Cổng chất lượng (scripts/check.sh)"`, `GRADLE_READ_DOCS`, `PULLS_TRIES` (mặc định 3), `PULLS_WAIT_SECONDS` (mặc định 5; test đặt 0). Các bước, dừng ở bước đầu tiên cho ra `full`:

1. Sự kiện (tham số 2, mặc định `push`) khác `push` → `full` ("chạy tay"). Lần chạy thứ (tham số 3, mặc định 1) lớn hơn 1 → `full` ("chạy lại": người bấm Re-run để biết commit có thật xanh không, nhất là để đóng `main-broken` theo ADR-0025 mục 2; lần đó phải chạy `check.sh` thật).
2. Commit có thay đổi so với commit cha ở một file của `GATE_FILES` → `full`. Danh sách: `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/check.sh`, `scripts/failure-kind.sh`, `scripts/find-breaking-commit.sh`, `scripts/main-guard-verified.sh`, `scripts/tests/run.sh`, `scripts/tests/fake-gh` (mọi file mà job `guard` gọi hay dựa vào). Đọc bằng `git diff --name-only <sha>^ <sha>` (checkout có `fetch-depth: 50`).
3. Tìm PR: `gh api repos/$REPO/commits/<sha>/pulls`, lấy PR có `merged_at` khác rỗng, `base.ref == "main"` và `merge_commit_sha == <sha>`. GitHub có thể chưa gắn commit với PR vài giây sau lần đẩy: danh sách rỗng thì thử lại tới `PULLS_TRIES` lần, cách `PULLS_WAIT_SECONDS` giây (vòng có giới hạn nằm trong script). Vẫn không có → `full` ("không phải commit merge của PR").
4. Commit đã kiểm `H` = `head.sha` của PR. Lần `pr-check` mới nhất trên `H`: `gh api "repos/$REPO/actions/workflows/pr-check.yml/runs?head_sha=<H>&per_page=10"` (như `merge-if-green.sh`), run có `created_at` lớn nhất. Không có, hay `conclusion != "success"` → `full`.
5. Job `GATE_JOB` của run đó (`gh api "repos/$REPO/actions/runs/<id>/jobs?per_page=50"`, như `scripts/wait-run.sh`): bước tên `GATE_STEP` phải có `conclusion == "success"`. Bị bỏ qua (PR không có code, chỉ chạy `scripts/tests/run.sh`), không có job hay không có bước → `full` ("PR chưa chạy check.sh"). Đọc `.steps[].conclusion` là lần đầu trong dự án.
6. Lấy `H` về máy: có sẵn (`git cat-file -e <H>^{commit}`) thì dùng. Không có (trên GitHub là đường chính: commit merge squash không có `H` làm tổ tiên) thì `git fetch --no-tags --depth=1 origin "refs/pull/<N>/head"`, rồi kiểm `git rev-parse FETCH_HEAD` đúng `H`. Không lấy được, hay ref trỏ commit khác → `full`.
7. `git diff --name-only <H> <sha>`:
   - rỗng → `skip`;
   - mọi dòng nằm trong `docs/` hay kết thúc bằng `.md`, và không dòng nào thuộc `GRADLE_READ_DOCS` → `tests`. `GRADLE_READ_DOCS` là các file trong `docs/` mà Gradle đọc khi chạy test: `docs/remote-config-keys.md`, `docs/remote-config-template.json` (`app/build.gradle.kts` dòng 120, 121, cho `RemoteConfigKeysTest`), `docs/design-system/tokens.json` (`core/designsystem/build.gradle.kts` dòng 21, cho `DesignTokensTest`);
   - còn lại → `full`.

Dòng lý do khi `skip`: `ĐÃ KIỂM Ở PR #<N> (commit <7 ký tự>): cây file giống hệt, không chạy lại check.sh (ADR-0039).` Khi `tests`: `CHỈ KHÁC TÀI LIỆU so với PR #<N>: chỉ chạy scripts/tests/run.sh (ADR-0039).` Khi `full`: `KIỂM ĐẦY ĐỦ: <lý do>.`

Lập luận cây file giống hệt: `pr-check` kiểm `refs/pull/N/merge`; `merge-if-green.sh` chỉ merge khi nhánh không chậm commit nào (`behind_by == 0`), nên cây đã kiểm, cây của `H` và cây của commit merge squash là một (ADR-0039, Bối cảnh).

**`main-guard.yml`, job `guard`** (giữ tên job, `if:` cấp job, `paths-ignore`):

- `permissions:` thêm `actions: read`, `pull-requests: read` (mục 3.4); giữ `contents: write`, `issues: write`, `checks: read`.
- Sau `actions/checkout`, thêm bước `id: mode`, tên `Chọn cách kiểm (scripts/main-guard-verified.sh)`, `env: GH_TOKEN: ${{ github.token }}`, chạy `echo "mode=$(scripts/main-guard-verified.sh "${GITHUB_SHA}" "${GITHUB_EVENT_NAME}" "${GITHUB_RUN_ATTEMPT}")" >> "$GITHUB_OUTPUT"`. Bước này không có `continue-on-error` (script luôn thoát 0); nếu nó vẫn lỗi thì job lỗi trước bước cổng, như lỗi checkout: lỗi hạ tầng, không mở `main-broken`.
- Quy ước: giá trị khác `skip` và khác `tests` (kể cả rỗng) là `full`. Vì vậy:
  - `actions/setup-java`: `if: steps.mode.outputs.mode != 'skip'`;
  - `gradle/actions/setup-gradle`: `if: steps.mode.outputs.mode != 'skip' && steps.mode.outputs.mode != 'tests'`;
  - bước `first` (`Cổng chất lượng`): `if: steps.mode.outputs.mode != 'skip'`; lệnh `if [ "${{ steps.mode.outputs.mode }}" = "tests" ]; then scripts/tests/run.sh 2>&1 | tee build.log; else scripts/check.sh 2>&1 | tee build.log; fi`.
- Bước `kind` và `retry` giữ như cũ. Ở cách `tests`, `failure-kind.sh` không trả `tests` cho log của `run.sh`, nên không chạy lại (plan chấp nhận: `run.sh` không có test chập chờn kiểu Gradle).
- Bước `check` (`Kết quả cổng chất lượng`): thêm nhánh đầu `if [ "${{ steps.mode.outputs.mode }}" = "skip" ]; then echo "Đã kiểm ở PR, xem bước Chọn cách kiểm."; exit 0; fi`. Phần còn lại giữ nguyên.
- Các bước `Ghi test chập chờn`, `Đánh dấu nhánh chính đang đỏ`, `Đóng issue main-broken khi nhánh chính xanh trở lại` không đổi.
- Chú thích đầu file thêm một gạch đầu dòng về ba cách và ADR-0039.

**`pr-check.yml`:** chỉ thêm một dòng chú thích ở đầu file: tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)` được `scripts/main-guard-verified.sh` đọc, đổi thì đổi cả hai nơi.

**Tài liệu:**

- ADR-0010: thêm một dòng ghi chú ở đầu, như ghi chú của ADR-0025 đã có, dẫn tới ADR-0039. Không sửa nội dung cũ.
- `docs/auto-run.md` mục 1 ý 6 (dòng khoảng 23): "10 phút `main-guard`" thành "khoảng 1 phút `main-guard` khi PR đã chạy `check.sh` và được merge mà không có commit khác chen vào, 10 phút khi chạy đầy đủ (ADR-0039)". Bước 9 (dòng khoảng 225, "khoảng 5 đến 12 phút"): thêm "khoảng 1 phút khi đã kiểm ở PR (ADR-0039)".
- `docs/ops/quick-lane.md` dòng 9: cùng con số như bước 9.
- `docs/foundation.md`: sinh lại bằng `scripts/foundation.py` nếu trạng thái ADR-0039 đổi trước khi chạy task (test của script báo khi trang lệch).

Không sửa: `scripts/merge-if-green.sh`, `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh`, `start-run.yml`, `scripts/release.sh` (cấm sửa), `scripts/wait-run.sh`. Lý do ở bảng L-058, mục 4.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/main-guard-verified.sh` | Script quyết định `full`, `tests`, `skip` (mục 3.1) |
| Sửa | `.github/workflows/main-guard.yml` | Thêm `actions: read`, `pull-requests: read` vào `permissions:`; bước `mode`; điều kiện của `setup-java`, `setup-gradle`, `first`; nhánh `skip` của bước `check`; chú thích đầu file |
| Sửa | `.github/workflows/pr-check.yml` | Một dòng chú thích ở đầu file (tên job và bước mà script mới đọc) |
| Sửa | `scripts/tests/run.sh` | Test của script mới và test cấu trúc của `main-guard.yml`, `pr-check.yml` (mục 4) |
| Sửa | `docs/adr/0010-soft-quality-gate-github-free.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0039 |
| Sửa | `docs/adr/0039-main-guard-tin-lan-kiem-cua-pr.md` | Chỉ khi rà soát độc lập của task thấy ADR lệch với code đã làm (tên bước, danh sách `GATE_FILES`); không đổi `status` |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` khi cần |
| Sửa | `docs/auto-run.md` | Mục 1 ý 6 và bước 9: số phút `main-guard` |
| Sửa | `docs/ops/quick-lane.md` | Dòng 9: số phút `main-guard` |
| Sửa | `docs/features/quy-trinh/plans/T-074-main-guard-da-kiem.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quy-trinh/tickets/T-074-main-guard-bo-qua-noi-dung-da-kiem.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020), tìm bằng `grep -rn 'main-guard' scripts/ .github/ docs/`:

- `scripts/check.sh` dòng 5, `scripts/failure-kind.sh` dòng 6 và 8: chỉ chú thích nhắc `main-guard.yml`; ý vẫn đúng (cách `full` vẫn chạy `check.sh`, vẫn chạy lại khi `failure-kind.sh` trả `tests`). Không sửa.
- `scripts/merge-if-green.sh` dòng 155, 322 đến 328: đọc issue `main-broken` và gọi `workflow_dispatch` sau `ĐÃ MERGE (GitHub báo lỗi)`. Lần chạy tay đó dùng `full`. Không sửa.
- `scripts/release.sh` dòng khoảng 187: đọc kết luận của workflow. Không sửa (cấm); ADR-0039 mục 3 ghi nghĩa mới, người chấp nhận khi duyệt.
- `docs/ops/release.md`, `docs/ops/staging-and-runs.md`, `docs/ops/setup-new-app.md`, `docs/ops/pre-run-review.md`, `docs/ops/test-sessions.md`, `docs/ops/token-usage.md`: nói `main-guard` xanh hay đỏ, không nói thời gian hay cách chạy. Không sửa.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi, và plan không đổi tên hay xóa mục nào:

- dẫn `docs/auto-run.md` (số mục trong chú thích hay lời giao phiên): `.github/workflows/device-session.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py`, `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/token-usage.py`. Plan chỉ đổi hai con số trong mục 1 ý 6 và bước 9;
- `scripts/local-setup.sh` chỉ nhắc `scripts/tests/run.sh`, không phụ thuộc test mới;
- `docs/ops/failure-playbook.md` và `.github/workflows/status-page.yml` chỉ nhắc tên `main-guard` hay `pr-check.yml`; plan không đổi tên workflow, job hay bước nào;
- `app/build.gradle.kts` dòng 120, 121 và `core/designsystem/build.gradle.kts` dòng 21 không sửa; script mới đọc danh sách file `docs/` của chúng (`GRADLE_READ_DOCS`), test 14 giữ hai bên khớp nhau;
- `scripts/tests/playbook/PB-02.log` là log mẫu có tên `pr-check.yml`; plan chỉ thêm một dòng chú thích vào `pr-check.yml`, không đổi tên job hay bước.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong `main-guard.yml`: thêm hai quyền chỉ đọc `actions: read`, `pull-requests: read` (mục 3.4); giữ `contents: write`, `issues: write`, `checks: read`.
- **Thư viện và SDK:** Không thêm. Action có sẵn: `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** khối `permissions:` của `main-guard.yml` khai báo rõ ba quyền, nên mọi quyền khác của `GITHUB_TOKEN` là `none`. Đọc `actions/workflows/.../runs`, `actions/runs/<id>/jobs` cần `actions: read`; đọc `commits/<sha>/pulls` cần `pull-requests: read` (hay `contents: read`). Thiếu quyền thì `gh api` trả 403 và script chọn `full`: an toàn nhưng không tiết kiệm gì.
- **Điều kiện chuyển:** luôn áp dụng, không chờ lỗi: thêm `actions: read` và `pull-requests: read` vào `permissions:` của `main-guard.yml` ngay ở bước 4. Test cấu trúc (mục 4, test 13) kiểm hai dòng này.
- **Cách làm thay:** sau khi merge, lần `main-guard` đầu tiên vẫn in `KIỂM ĐẦY ĐỦ: không đọc được ...` vì quyền: đây không phải đỏ. Ghi vào nhật ký, mở PR sửa quyền như việc của người (mục 7). Tiêu chí và test ở mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin. Hành vi đã có của script và workflow mà test có sẵn trong `scripts/tests/run.sh` khẳng định (trigger, `paths-ignore`, `if:` cấp job của `main-guard.yml`; ba job của `pr-check.yml`) giữ nguyên; các test đó không đổi.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`, `gh` giả là `scripts/tests/fake-gh` với `FIXTURE` (đường `routes`, `errors`, `__sequence__`). Phần git chạy trong một repo tạm `"$work/mgv"` (`git init`, commit có `app/x.kt`, `docs/a.md`), script gọi từ trong repo đó bằng đường dẫn tuyệt đối, với `GITHUB_REPOSITORY=o/r` và `PULLS_WAIT_SECONDS=0`. `run.sh` dòng 19 đặt `GIT_CONFIG_COUNT`, `GIT_CONFIG_KEY_0`, `GIT_CONFIG_VALUE_0` (đè `remote.origin.url` thành địa chỉ GitHub) cho mọi lệnh git: khối test này bỏ ba biến đó ở đầu khối và đặt lại ở cuối khối, như khối test của `save-branch.sh`, để `git fetch` đi tới repo bare cục bộ, không ra mạng (L-082). Hàm phụ trợ `mgv <fixture> <sha> [<sự kiện> [<lần chạy thứ>]]` in `<mã thoát> <dòng stdout>`; vào commit test của bước 3 (L-025).

Dữ liệu chung: commit `A` (cha), commit đã kiểm `H` (con của `A`, đổi `app/x.kt`), commit trên main `M` có cây giống `H` (tạo bằng `git commit-tree` với cây của `H`, cha `A`). Đường `commits/M/pulls` trả PR số 7, `merged_at` có giá trị, `base.ref` `main`, `merge_commit_sha` `M`, `head.sha` `H`. Đường `actions/workflows/pr-check.yml/runs?head_sha=H&per_page=10` trả run 91 `success`; `actions/runs/91/jobs?per_page=50` trả job `build-and-test` có bước `Cổng chất lượng (scripts/check.sh)` `success`. Mặc định `H` có sẵn trong repo tạm (bước 6 không fetch); test 10 dựng đường fetch.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| DoD 1: cây giống hệt, PR đã chạy `check.sh` | Test của script | 1. `main-guard-verified: same tree as a PR that ran check.sh is skip` mong `0 skip`, stderr có `PR #7` và 7 ký tự của `H` |
| DoD 1: chỉ khác tài liệu | Test của script | 2. `main-guard-verified: only docs differ from the checked PR is tests`: `M` thêm `docs/b.md` so với `H`, mong `0 tests`. Đối chứng cùng test: thêm `README.md` (đuôi `.md` ngoài `docs/`) cũng `tests`. Ca Gradle đọc tài liệu: chỉ khác `docs/remote-config-keys.md`, hay `docs/design-system/tokens.json` → `0 full` |
| DoD 2: khác ngoài tài liệu | Test của script | 3. `... code differs from the checked PR is full`: `M` thêm `app/y.kt`, mong `0 full` |
| DoD 2: PR chỉ chạy test của script | Test của script | 4. `... a PR that skipped check.sh is full`: bước `Cổng chất lượng` có `conclusion` `skipped`, mong `0 full` |
| DoD 2: `pr-check` không xanh | Test của script | 5. `... a red or missing pr-check run is full`: hai trường hợp, run `failure`, và danh sách run rỗng; mong `0 full` cả hai. Thêm: hai run, cũ `success` mới `failure`, mong `full` (lấy run mới nhất) |
| DoD 2: đẩy thẳng lên main | Test của script | 6. `... a commit without a merged PR is full`: `commits/M/pulls` trả `[]`; và PR có `merge_commit_sha` khác `M`; mong `0 full` cả hai. Ca GitHub chậm gắn PR: `commits/M/pulls` là `__sequence__` `[]` rồi PR số 7 → `0 skip` (thử lại có giới hạn); `[]` cả `PULLS_TRIES` lần → `0 full` |
| DoD 2: commit chạm cổng kiểm | Test của script | 7. `... a commit changing a gate file is full`: vòng qua tám file của `GATE_FILES`, mỗi lần `M` đổi đúng file đó so với `A`, cây vẫn giống `H` (tạo `H` cùng đổi file đó); mong tám lần `full`. Thêm một file ngoài danh sách (`scripts/status.py`) với cùng dựng: mong `skip` |
| DoD 2: chạy tay; lần chạy lại | Test của script | 8. `... workflow_dispatch or a re-run is full`: dữ liệu của test 1; sự kiện `workflow_dispatch` → `0 full`; sự kiện `push`, lần chạy thứ 2 → `0 full`; đối chứng: `push`, lần chạy thứ 1 → `0 skip` |
| Script không chắc thì `full`, luôn thoát 0 | Test của script | 9. `... an unreadable GitHub answer is full`: `errors` cho `commits/M/pulls` (`HTTP 403`), rồi cho `actions/runs/91/jobs?per_page=50` (`HTTP 500`); mong `0 full` và stderr có `KIỂM ĐẦY ĐỦ` |
| Lấy commit đã kiểm qua `refs/pull/N/head` (đường chính trên GitHub) | Test của script | 10. `main-guard-verified: fetches the checked commit from refs/pull/N/head`: repo bare cục bộ `"$work/mgv-origin.git"` làm `origin` (`file://`), `H` chỉ có trong repo bare (repo tạm không có), ba ca: `refs/pull/7/head` trỏ `H` → `0 skip`; trỏ một commit khác → `0 full`; không có ref → `0 full` |
| L-055 đối chứng | Test của script | 11. `... skip needs every condition`: từ dữ liệu test 1, đổi từng điều kiện một (tên bước khác, tên job khác, `base.ref` khác `main`, `merged_at` rỗng); mong `full` mỗi lần. Test này thất bại trên một script luôn in `skip` |
| `pr-check.yml` còn đúng tên mà script đọc | Test cấu trúc | 12. `main-guard-verified: pr-check.yml has the job and step it reads`: đọc `GATE_JOB`, `GATE_STEP` từ script, kiểm `pr-check.yml` có job `  build-and-test:` và bước `name: Cổng chất lượng (scripts/check.sh)` trong job đó (`pr_steps` có sẵn); chạy thêm trên bản sao hỏng trong `$work` (đổi tên bước) mong thất bại |
| Workflow nối đúng | Test cấu trúc | 13. `main-guard: chooses the mode before the gate and skips inside the guard job`. Câu khẳng định, mỗi câu đếm dòng trong job `guard` (awk như các test có sẵn của `main-guard.yml`): (a) job vẫn tên `  guard:`, `if:` cấp job chỉ có dòng `github.ref` có sẵn; (b) bước gọi `scripts/main-guard-verified.sh "${GITHUB_SHA}" "${GITHUB_EVENT_NAME}" "${GITHUB_RUN_ATTEMPT}"` đứng sau `actions/checkout` và trước bước `id: first`; (c) `setup-java` có `if: steps.mode.outputs.mode != 'skip'`; (d) `setup-gradle` có `if: steps.mode.outputs.mode != 'skip' && steps.mode.outputs.mode != 'tests'`; (e) `first` có `if: steps.mode.outputs.mode != 'skip'` và lệnh chọn `scripts/tests/run.sh` khi `tests`, `scripts/check.sh` khi khác; (f) bước `check` có nhánh `skip` thoát 0 đứng trước các dòng cũ; (g) `kind` giữ `if: steps.first.outcome == 'failure'`, `retry` giữ `if: steps.kind.outputs.kind == 'tests'`; (h) `Đánh dấu nhánh chính đang đỏ` giữ `if: failure() && steps.check.outcome == 'failure'`; (i) `Đóng issue main-broken ...` giữ `if: success()`; (j) `permissions:` có `actions: read`, `pull-requests: read`. Chạy cả trên bản sao hỏng trong `$work`, một bản cho mỗi câu (b), (d), (e), (f), (j), và một bản đặt `if:` cấp job dùng `steps.mode` cho (a); mong mỗi bản thất bại |
| `GRADLE_READ_DOCS` đủ | Test cấu trúc | 14. `main-guard-verified: GRADLE_READ_DOCS lists every docs file Gradle reads`: lấy mọi chuỗi `docs/...` trong `rootProject.file(...)` của các `*.gradle.kts` (`git grep`), so với `GRADLE_READ_DOCS` của script; mong khớp. Bản sao hỏng của script thiếu một file: mong thất bại |
| Mọi kiểm tra hiện có (L-058) | Bảng dưới | Bảng L-058 |
| DoD 1 sau khi merge | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Merge PR code đầu tiên sau task này; xem log job `guard` có dòng `ĐÃ KIỂM Ở PR`, thời gian job dưới 1 phút, `scripts/main-guard-status.sh` in `XANH`. Ghi vào nhật ký |

**Bảng L-058: trường hợp mới (`skip`, `tests`) qua từng kiểm tra hiện có của `main-guard` và nơi đọc nó.**

| Kiểm tra hiện có | `skip` | `tests` | Test |
| --- | --- | --- | --- |
| Trigger `push` với `paths-ignore`, `workflow_dispatch`, `if:` chỉ trên `main` | Không đổi | Không đổi | Test có sẵn `main-guard: has workflow_dispatch and keeps the push trigger ...`, giữ nguyên; test 13 thêm "không có `if:` cấp job khác" |
| Chạy lại test hỏng một lần, ghi `flaky` (ADR-0025 mục 1) | Không chạy (bước `first` bỏ qua, `kind` bỏ qua) | Chỉ khi `failure-kind.sh` trả `tests`, không xảy ra với log của `run.sh` | Test 13 (điều kiện `if:` của `first`); `kind`, `retry` giữ điều kiện cũ (`steps.first.outcome == 'failure'`, `steps.kind.outputs.kind == 'tests'`), test 13 kiểm hai dòng này |
| Mở `main-broken`, tìm commit gây lỗi, đẩy nhánh revert | Không chạy (bước `check` thoát 0) | Chạy khi `run.sh` đỏ (ADR-0039 mục 4) | Test 13 (nhánh `skip` của `check`; bước `Đánh dấu nhánh chính đang đỏ` giữ điều kiện `failure() && steps.check.outcome == 'failure'`) |
| Tự đóng `main-broken` khi xanh (ADR-0025 mục 2) | Chạy như lần xanh thường. Lần chạy lại (Re-run) luôn `full`, nên issue chỉ tự đóng nhờ lần chạy lại khi `check.sh` thật xanh | Chạy như lần xanh thường | Test 13 (i); test 8 (lần chạy thứ 2 là `full`). PR revert được merge theo cách `skip` là đúng: PR đã chạy `check.sh` trên cùng cây file |
| `find-breaking-commit.sh` đọc check run `guard` | `success` như lần xanh | `success`/`failure` | Test có sẵn của `find-breaking-commit.sh` (dữ liệu `states` là kết luận của check run), giữ nguyên; test 13 kiểm job vẫn tên `guard`, không bị skip |
| `main-guard-status.sh` (đầu phiên, `start-run.yml`) | `XANH` | `XANH`/`ĐỎ` | Test có sẵn `main-guard-status: green run on the head passes` và các test kế, giữ nguyên |
| `merge-if-green.sh`: từ chối khi có `main-broken`, gọi `main-guard` sau `ĐÃ MERGE (GitHub báo lỗi)` | Không đổi; lần gọi tay dùng `full` | Không đổi | Test 8 (`workflow_dispatch` là `full`); test có sẵn `merge-if-green asks for main-guard after such a merge`, giữ nguyên |
| `wait-run.sh --workflow main-guard.yml` | Đọc kết luận workflow `success` | Như cũ | Test có sẵn của `wait-run.sh`, giữ nguyên |
| `release.sh` chỉ build commit có `main-guard.yml` `success` | Mở cổng phát hành (ADR-0039 mục 3) | Như cũ | Không sửa, không test mới (cấm chạm); người chấp nhận ở mục 8, câu 1 |

**Ngoại lệ viết test trước:** sửa tài liệu (`docs/auto-run.md`, `docs/ops/quick-lane.md`, ghi chú ADR-0010, `foundation.md`) và hai dòng chú thích của `pr-check.yml`, `main-guard.yml`: không có hành vi để kiểm.

### 4.1. Chạy thử

Không cần (E dưới 2): `gh api` với `fake-gh`, `git diff`, `git commit-tree`, repo bare cục bộ và điều kiện `if:` của bước đều đã dùng trong dự án (`main-guard-status.sh`, `merge-if-green.sh`, `wait-run.sh`, test của `save-branch.sh` và `pr-check.yml`). Phần chưa chạy thử được trên máy là quyền của `GITHUB_TOKEN`; mục 3.4 thêm quyền ngay, không chờ lỗi.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, hai workflow (một chỉ chú thích), test, bốn tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 1 | Ticket và ADR-0039 nêu rõ ba cách; còn câu hỏi về nghĩa mới của "xanh" với bản production (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Quyết định có test với `gh` giả và repo git tạm; quyền thật của token và log thật chỉ kiểm sau khi merge |
| D. Ảnh hưởng chéo | 2 | `main-guard` chặn mọi lần merge khi đỏ, quyết định phiên có bắt đầu không, và mở cổng phát hành |
| E. Độ mới của kỹ thuật | 1 | Lần đầu đọc `.steps[].conclusion` của job; đọc `/jobs` đã có ở `scripts/wait-run.sh`, đọc run của `pr-check` theo `head_sha` và fetch `refs/pull/N/head` đã có ở `merge-if-green.sh` |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `main-guard.yml` (mọi commit của main), `pr-check.yml` (chỉ chú thích), `docs/auto-run.md` mục 1 và bước 9.
- Task khác có thể xung đột: T-075 sửa `main-guard.yml`, `pr-check.yml`, `docs/auto-run.md` (cùng dòng ước tính phút ở mục 1 ý 6); T-076 đổi cấu trúc `pr-check.yml` (bỏ job `changes`, có thể dời bước lọc); T-077 sửa `merge-if-green.sh`. Cả ba phụ thuộc T-074 và làm sau. T-076 phải giữ tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)`, hoặc sửa `GATE_JOB`, `GATE_STEP` cùng PR (test 12 báo).
- Các plan ads đang mở (#294 tới #304) không chạm các file này.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow, script và test là của dự án mẫu.
- Đề xuất thứ tự: làm trong phiên có người, trước T-075.

## 7. Điều kiện dừng riêng

- Cần thêm bí mật, token cá nhân hay quyền ghi mới cho `main-guard.yml`: dừng.
- Không giữ được check run `guard` có kết luận `success` ở cách `skip` (ví dụ phải tách job): dừng, vì `find-breaking-commit.sh`, `main-guard-status.sh` và `release.sh` sẽ hiểu sai.
- Sau khi merge, lần `main-guard` đầu tiên đỏ ở bước `mode` hay bước `check` vì lỗi của chính task (không phải code của commit): revert cùng dev. Nếu chỉ in `KIỂM ĐẦY ĐỦ` vì thiếu quyền thì không phải đỏ, xử lý theo mục 3.4.

## 8. Câu hỏi cho người duyệt

1. **ADR-0039 và cổng phát hành.** Sau task này, commit xanh theo cách `skip` cũng đủ để `scripts/release.sh` build bản production, vì script đọc kết luận của `main-guard.yml`. Đề xuất chấp nhận: cây file đã qua `check.sh` đầy đủ ở PR. Đồng ý thì khi duyệt plan, ghi ADR-0039 `status: accepted` và chạy `scripts/foundation.py`, commit cùng lần duyệt (`docs/CLAUDE.md`, mục Khi viết ADR).
2. **Mất lần chạy lại bắt test chập chờn trên main.** Ở cách `skip`, test chập chờn không còn bị `main-guard` ghi `flaky`; nó chỉ lộ ra ở `pr-check`. Đề xuất chấp nhận.
3. **Cách `tests` đỏ mở `main-broken`** (ví dụ hai PR thêm bài học cùng số rồi merge). Như hiện nay khi `check.sh` chạy `run.sh`. Đề xuất giữ.
4. **Thêm quyền đọc** `actions: read`, `pull-requests: read` cho `main-guard.yml` (mục 3.4). Chỉ quyền đọc. Đề xuất đồng ý.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
