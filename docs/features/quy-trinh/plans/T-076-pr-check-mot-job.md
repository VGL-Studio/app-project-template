---
ticket: T-076
title: pr-check bỏ job changes riêng
status: draft
version: 1
risk_score: 7
risk_level: high
token_estimate: 5.4M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-076: PR-CHECK BỎ JOB CHANGES RIÊNG

## 1. Tóm tắt

Bước lọc đường dẫn `dorny/paths-filter` chuyển từ job `changes` vào đầu job `build-and-test` (bước `id: filter`), các bước sau đọc `steps.filter.outputs`; `pr-check` chỉ còn một job, bớt khoảng 309 phút Actions mỗi tuần. `scripts/merge-if-green.sh` chỉ đòi check `build-and-test`. Một bước kiểm ngay sau bộ lọc dừng job khi kết quả lọc rỗng hay không phải `true`/`false` (ví dụ đổi `id` của bước lọc), để lỗi nối như vậy không đưa PR code sang nhánh chỉ chạy test của script mà vẫn xanh. Bước này không bắt được file code mà không mẫu nào của bộ lọc khớp (`code=false` hợp lệ); trường hợp đó do test PR mẫu (P6) giữ, với các đường dẫn mẫu. Kiểm bằng test của `scripts/tests/run.sh`: cấu trúc workflow, PR mẫu chạm code và PR mẫu chỉ tài liệu đi qua bộ lọc thật của workflow, và mọi kiểm tra của `merge-if-green.sh` với dữ liệu một job (L-058).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 4. Quyết định của anh Nhật ngày 2026-10-29: áp dụng cả sáu cách; T-074 tới T-077 làm có người (`auto_run: false`).

**Giả định về T-075 (phụ thuộc; plan draft ở PR #318, chưa duyệt).** Plan này viết cho trạng thái sau khi T-075 merge, khớp mục 3.1 "Trạng thái sau T-075" của plan T-075 bản draft ngày 2026-10-30: `pr-check.yml` còn hai job, `changes` (bộ lọc, ra `code` và `release`) và `build-and-test` (`needs: changes`, các bước đọc `needs.changes.outputs.code` và `needs.changes.outputs.release` như hôm nay); không còn job `ui-test` và bộ lọc `ui`; chú thích đầu file có thêm dòng về job `ui-test` của `main-guard.yml` (ADR-0043); `merge-if-green.sh` có `REQUIRED_CHECKS=("changes" "build-and-test")`, không còn `SKIPPABLE_CHECKS`; `scripts/tests/run.sh` có `pr_missing_jobs` mong `names=2`, dữ liệu `code_runs` (`changes`, `build-and-test` xanh) và test 21 tới 23 của T-075 (`merge-if-green: a code PR without ui-test is ready`, `... a running ui-test is not waited for`, `... a red ui-test left from before is still refused`); `green_runs` và `mig_run` giữ dòng `ui-test` `skipped`. Mọi diff và số đếm ở mục 3.1 và mục 4 tính từ trạng thái đó. T-075 merge với hình dạng khác (plan T-075 đổi sau rà soát, còn bộ lọc `ui`, còn `SKIPPABLE_CHECKS`, đổi tên bước hay tên test) thì plan phải đối chiếu lại trước khi duyệt (mục 8, câu 1).

**Giữ cho T-077.** Sau ticket này `pr-check` chỉ có một job `build-and-test`, bước lọc nằm trong job với `id: filter`, các bước đọc `steps.filter.outputs.code` và `steps.filter.outputs.release`. Tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)` giữ nguyên (`scripts/main-guard-verified.sh` đọc, ADR-0039).

| Yêu cầu (định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| DoD 1 | `pr-check` của PR chỉ tài liệu và PR code đều chỉ có một job; PR code vẫn chạy `check.sh` | Workflow có đúng một job; bộ lọc chọn nhánh `check.sh` cho PR chạm code, nhánh `scripts/tests/run.sh` cho PR chỉ tài liệu. Kiểm bằng test cấu trúc và test PR mẫu (mục 4), rồi trên GitHub thật: PR của chính ticket này (PR code, vì `pr-check.yml` nằm trong bộ lọc `code`) và PR tài liệu kế tiếp |
| DoD 2 | Script merge và chờ CI không đọc job `changes`, có test | `merge-if-green.sh` chỉ đòi `build-and-test`. `scripts/wait-run.sh` không đọc tên job nào để quyết xanh hay đỏ (đọc kết luận của cả lần chạy, chỉ nêu tên job đỏ): không sửa, dữ liệu test đổi sang một job |
| DoD 3 | CI qua toàn bộ, đã merge | Như mọi task; merge bằng `merge-if-green.sh` của chính nhánh task (mục 3.1, Chuyển tiếp) |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (cổng chất lượng mềm, mục 1: "toàn bộ kiểm tra của `pr-check.yml`" vẫn đúng với một job), ADR-0039 (`main-guard-verified.sh` đọc tên job và bước của `pr-check.yml`), ADR-0029 (phạm vi file), ADR-0031 (viết lại câu khẳng định cũ). Không cần ADR mới (mục 8, câu 5).

## 3. Giải pháp

### 3.1. Cách làm

**`.github/workflows/pr-check.yml`** (diff so với trạng thái sau T-075):

```diff
 # Quality gate for every PR into main (ADR-0002, ADR-0010).
-# Job names (changes, build-and-test) are checked by scripts/merge-if-green.sh: rename in both places.
+# One job on purpose (T-076): a job is billed at least one minute, so the path filter is the first step of
+# build-and-test (id: filter) and later steps read steps.filter.outputs. The job name build-and-test is checked by
+# scripts/merge-if-green.sh: rename in both places.
 # <dòng chú thích về job ui-test của main-guard.yml (ADR-0043) mà T-075 thêm: giữ nguyên>
 # scripts/main-guard-verified.sh reads the job build-and-test and its step "Cổng chất lượng (scripts/check.sh)" (ADR-0039):
 ...
 jobs:
-  changes:
-    runs-on: ubuntu-latest
-    outputs:
-      code: ${{ steps.filter.outputs.code }}
-      release: ${{ steps.filter.outputs.release }}
-    # No checkout needed: on pull_request events paths-filter reads the changed files from the API.
-    steps:
-      - uses: dorny/paths-filter@v3
-        id: filter
-        with:
-          <khối filters: code, release>
-
   build-and-test:
-    needs: changes
     runs-on: ubuntu-latest
     timeout-minutes: 45
     steps:
+      # On pull_request events paths-filter reads the changed files from the API (pull-requests: read), no checkout.
+      - name: Lọc đường dẫn (dorny/paths-filter)
+        id: filter
+        uses: dorny/paths-filter@v3
+        with:
+          <khối filters: code, release, chép nguyên từ job changes, kể cả chú thích>
+
+      # An empty or odd output (a renamed step id, a changed action) would send a PR with code down the docs-only
+      # path below, which passes without check.sh: stop the job instead. It cannot catch a code file that no pattern
+      # matches (code=false is valid); the sample-PR tests of scripts/tests/run.sh keep the patterns right.
+      # The message also goes to build.log, which the failure comment prints.
+      - name: Kiểm kết quả lọc đường dẫn
+        env:
+          CODE: ${{ steps.filter.outputs.code }}
+          RELEASE: ${{ steps.filter.outputs.release }}
+        run: |
+          for value in "code=$CODE" "release=$RELEASE"; do
+            case "${value#*=}" in
+              true | false) ;;
+              *)
+                echo "Bộ lọc đường dẫn trả '${value#*=}' cho ${value%%=*}, cần true hoặc false." | tee -a build.log
+                echo "::error::Bộ lọc đường dẫn trả '${value#*=}' cho ${value%%=*}, cần true hoặc false."
+                exit 1 ;;
+            esac
+          done
+
       - uses: actions/checkout@v5
 ...
-        if: needs.changes.outputs.code != 'true'
+        if: steps.filter.outputs.code != 'true'
 ...
-      - if: needs.changes.outputs.code == 'true'
+      - if: steps.filter.outputs.code == 'true'
         uses: gradle/actions/setup-gradle@v4
 ...
-        if: needs.changes.outputs.code == 'true'
+        if: steps.filter.outputs.code == 'true'
 ...
-        if: needs.changes.outputs.code == 'true' && needs.changes.outputs.release == 'true'
+        if: steps.filter.outputs.code == 'true' && steps.filter.outputs.release == 'true'
 ...
-        if: always() && needs.changes.outputs.code == 'true'
+        if: always() && steps.filter.outputs.code == 'true'
```

- Khối `filters` (danh sách `code`, `release` và chú thích của chúng) chép nguyên, không thêm bớt đường dẫn. T-075 đã bỏ bộ lọc `ui`; test P4 kiểm mỗi bộ lọc khai báo đều có bước đọc, nên bộ lọc thừa sau này cũng bị báo (mục 8, câu 2).
- Bước "Kiểm kết quả lọc đường dẫn" chặn: kết quả rỗng hay khác `true`/`false` của `code` hoặc `release` (đổi `id: filter`, action đổi tên output, bộ lọc bị xóa khỏi khối `filters`). Không chặn: file code mà không mẫu nào của `code` khớp (kết quả `false` hợp lệ, PR đi nhánh tài liệu và xanh). Trường hợp sau do test PR mẫu P6 giữ, chỉ với các đường dẫn mẫu đã liệt kê; mẫu mới của bộ lọc thì thêm đường dẫn mẫu cùng PR.
- Điều kiện của nhánh chỉ tài liệu giữ dạng `!= 'true'`, không đổi thành `== 'false'`: đổi thì kết quả rỗng làm cả hai nhánh bị bỏ qua và job xanh mà không chạy test nào. Kết quả rỗng hay lạ bị bước "Kiểm kết quả lọc đường dẫn" chặn trước.
- Bộ lọc lỗi (API của GitHub trả lỗi): bước lỗi, job đỏ. Hành vi mới: bước "Bình luận lỗi vào PR" (`if: failure()`) nay chạy và đăng bình luận "build-and-test thất bại" với "Không có log." (lỗi của action không vào `build.log`; người đọc log của bước, chấp nhận). Trước đây job `changes` đỏ thì `build-and-test` bị bỏ qua, không có bình luận, và `merge-if-green.sh` từ chối; kết quả merge như cũ: PR đỏ, không merge. Bước kiểm kết quả lọc đỏ thì bình luận có dòng lý do (ghi vào `build.log`).
- `permissions:` giữ `contents: read`, `pull-requests: write` (đủ cho `paths-filter` đọc danh sách file qua API). Không thêm quyền.
- `concurrency`, `timeout-minutes: 45`, tên job `build-and-test`, tên bước `Cổng chất lượng (scripts/check.sh)`, `Build bản release (R8)` (`id: release`), các bước khác giữ nguyên.

**`scripts/merge-if-green.sh`:**

```diff
 # Checks that must be green. Names match the job names in .github/workflows/pr-check.yml.
-REQUIRED_CHECKS=("changes" "build-and-test")
+# pr-check has one job (T-076): the path filter is a step of build-and-test.
+REQUIRED_CHECKS=("build-and-test")
```

Không đổi gì khác: danh sách kiểm tra đỏ (`failure`, `cancelled`, ...) vẫn quét mọi check run trên commit, kể cả check `changes` cũ còn sót trên commit được kiểm trước T-076 (test M6).

**Chuyển tiếp.**

- PR của chính ticket này: `pr-check` chạy theo `pr-check.yml` của PR (sự kiện `pull_request` dùng workflow của commit ghép), nên chỉ có job `build-and-test`. `merge-if-green.sh` của nhánh chính lúc đó còn đòi `changes`, sẽ chờ mãi (mã 2, không bao giờ merge nhầm). Vì vậy merge PR này bằng `scripts/merge-if-green.sh` của chính nhánh task (thư mục làm việc ở nhánh `claude/T-076-...`). Ghi vào nhật ký.
- PR khác đang mở lúc T-076 merge: commit cuối của chúng được kiểm bằng hai job; sau khi T-076 merge chúng chậm hơn nhánh chính, nên `merge-if-green.sh --update` cập nhật nhánh và `pr-check` chạy lại theo workflow một job. Không cần làm gì thêm với chính PR đó.
- Bản `merge-if-green.sh` cũ còn ở nơi khác sau khi T-076 merge sẽ chờ check `changes` mãi (mã 2, `CHƯA XONG: chưa thấy kiểm tra 'changes'`), mà skill `merge-pr` dạy "mã 2 thì gọi lại sau". Không merge nhầm, nhưng kẹt. Ba chỗ:
  - worktree hay nhánh `claude/*` tạo trước khi T-076 merge, nhất là phiên tự chạy đang tạm dừng (nhánh báo cáo, nhánh task gọi script của chính nó): chỉ merge T-076 khi không có phiên nào `running` hay tạm dừng (danh sách chạy trên nhánh `claude/RUN-*-report`, tra như `merge-if-green.sh`), ghi vào nhật ký lúc kiểm;
  - thư mục nhánh chính của dev khác chưa kéo: sau khi merge, báo anh Nhật và Tuấn kéo nhánh chính (`git pull`) trước lần merge kế tiếp, ghi vào mục Việc còn lại của nhật ký;
  - PR đồng bộ của app qua `scripts/template-sync.sh` (ADR-0030): `pr-check` của PR đồng bộ chạy theo workflow một job của chính PR, còn `merge-if-green.sh` của nhánh chính app còn đòi `changes`. Merge PR đồng bộ đó bằng `merge-if-green.sh` chạy trong thư mục ở nhánh đồng bộ của app, như PR của chính T-076. Ghi cách này vào mô tả PR đồng bộ; đưa nó vào `docs/ops/to-app.md` là việc của plan khác (mục 8, câu 7).
- `main-guard` của commit merge T-076 chạy `full`, vì commit đổi `pr-check.yml` (một file của `GATE_FILES`, ADR-0039).
- Các app nhận thay đổi qua `scripts/template-sync.sh`: `pr-check.yml` và `merge-if-green.sh` đi cùng một đợt đồng bộ (cách merge PR đồng bộ ở trên).

**Tìm mọi nơi đọc tên job `changes`** (`grep -rnE '"changes"|^ *changes:|needs[.:] ?changes|MIG_CHANGES|\(changes,' scripts/ .github/ docs/ops/ docs/auto-run.md docs/adr/ CLAUDE.md`, và `grep -rn 'job .changes.' docs/ops/ docs/auto-run.md docs/adr/`, cùng `scripts/main-guard-status.sh`, `scripts/find-breaking-commit.sh`, `scripts/run-check.py`, `scripts/status.py`, ngày 2026-10-30):

- `scripts/merge-if-green.sh` dòng 27: sửa (trên).
- `.github/workflows/pr-check.yml`: chú thích dòng 2, job `changes`, `needs: changes`, năm điều kiện `needs.changes.outputs`: sửa (trên).
- `scripts/tests/run.sh`: hàm `pr_docs_steps`, `pr_gradle_gated`, `pr_docs_step_tools` (dòng khoảng 197 tới 215), hai bản sao hỏng (dòng khoảng 245, 250), dữ liệu `green_runs` (dòng khoảng 551), `code_runs` của T-075, chú thích dòng khoảng 563, `mig_run` và `MIG_CHANGES` (dòng khoảng 648, 659, 694), dữ liệu của `wait-run` (dòng khoảng 936): sửa (mục 4, mục 3.5). Số dòng là của nhánh chính ngày 2026-10-30, trước T-075.
- `scripts/main-guard-status.sh`, `scripts/find-breaking-commit.sh`: đọc check run `guard` của `main-guard.yml` (`CHECK_NAME`), không đọc job của `pr-check`. Không sửa.
- `scripts/main-guard-verified.sh`: đọc job `build-and-test` và bước `Cổng chất lượng (scripts/check.sh)`, cả hai giữ tên. Không sửa; test có sẵn `main-guard-verified: pr-check.yml has the job and the step it reads` giữ hai bên khớp.
- `scripts/wait-run.sh`: đọc kết luận của cả lần chạy `pr-check.yml`, chỉ in tên job đỏ. Không sửa.
- `scripts/run-check.py`, `scripts/status.py`, `scripts/run-watchdog.py`: không đọc tên job của `pr-check` (`run-watchdog.py` đọc tên workflow). Không sửa.
- `docs/ops/`, `docs/auto-run.md`, `docs/adr/`, `CLAUDE.md`: không nêu job `changes`; câu nói về `build-and-test` và "toàn bộ kiểm tra của `pr-check.yml`" vẫn đúng. Không sửa. Plan và nhật ký cũ của feature khác ("`changes`, `build-and-test`, `ui-test` đều qua") là lịch sử, không sửa.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `.github/workflows/pr-check.yml` | Bỏ job `changes`; bước `Lọc đường dẫn (dorny/paths-filter)` (`id: filter`) và bước `Kiểm kết quả lọc đường dẫn` ở đầu `build-and-test`; điều kiện đọc `steps.filter.outputs`; chú thích đầu file (mục 3.1) |
| Sửa | `scripts/merge-if-green.sh` | `REQUIRED_CHECKS=("build-and-test")` và một dòng chú thích |
| Sửa | `scripts/tests/run.sh` | Test mới P1 tới P6, M1 tới M7; viết lại hàm và dữ liệu của test `pr-check`, `merge-if-green`, `wait-run` có sẵn theo một job (mục 4) |
| Sửa | `docs/features/quy-trinh/plans/T-076-pr-check-mot-job.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quy-trinh/tickets/T-076-pr-check-mot-job.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020):

- `pr-check.yml` được đọc bởi `scripts/main-guard-verified.sh` (`GATE_FILES`, tên job và bước: giữ), `scripts/tests/run.sh` (trong danh sách), `.github/workflows/main-guard.yml` và `.github/workflows/device-session.yml` (chỉ chú thích nhắc tên), `scripts/tests/playbook/PB-02.log` (log mẫu có tên workflow). Plan không đổi tên workflow, tên job hay tên bước cổng chất lượng: không ảnh hưởng.
- `merge-if-green.sh` được nhắc ở nhiều script, workflow và tài liệu (`scripts/merge-if-green.sh` là cách merge duy nhất): `scripts/is-exact-revert.sh`, `scripts/main-guard-status.sh`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/template-sync.sh`, `scripts/plan-check.py`, `scripts/run-check.py`, `scripts/status.py`, `scripts/resume-check.py`, `scripts/cloud-env-check.sh`, `scripts/push-on-commit.sh`, `.github/workflows/main-guard.yml`, `.github/workflows/start-run.yml`, `.github/workflows/resume-run.yml`. Chúng gọi script hay nhắc tên trong chú thích, thông báo; không đọc `REQUIRED_CHECKS`. Cách gọi, tham số và mã thoát không đổi: không ảnh hưởng.
- `scripts/tests/run.sh` được nhắc ở `scripts/check.sh`, `scripts/local-setup.sh`, `scripts/main-guard-verified.sh` (`GATE_FILES`), `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`: chúng chạy bộ test, không đọc nội dung test. Không ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong `pr-check.yml` giữ `contents: read`, `pull-requests: write`.
- **Thư viện và SDK:** Không thêm. Action có sẵn: `dorny/paths-filter@v3` (dời từ job `changes`), `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`, `actions/upload-artifact@v4`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `dorny/paths-filter@v3` chạy trong job có bước `actions/checkout`. Với sự kiện `pull_request`, action đọc danh sách file qua API bằng token mặc định `github.token` (không cần checkout, như job `changes` hiện nay); bước lọc đứng trước checkout nên thư mục làm việc còn trống. Chưa chạy thử trong job có checkout.
- **Điều kiện chuyển:** trên PR của chính ticket này, bước "Lọc đường dẫn" lỗi, hay bước "Kiểm kết quả lọc đường dẫn" báo kết quả không phải `true`/`false`, hay `code` ra `false` dù PR đổi `pr-check.yml`.
- **Cách làm thay:** đặt bước lọc (và bước kiểm ngay sau nó) sau `actions/checkout@v5`, vẫn trước mọi bước đọc kết quả. Không khai báo `token:`: giá trị mặc định đã là `github.token`, khai báo lại không đổi gì. Tiêu chí và test ở mục 4 giữ nguyên: P3 viết sẵn cho cả hai cách (chỉ `actions/checkout@v5` được đứng trước bước lọc).

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Hành vi của workflow và script mà test có sẵn của `scripts/tests/run.sh` khẳng định, và bị đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Các bước của `build-and-test` đọc `needs.changes.outputs.code` | Đọc `steps.filter.outputs.code` | `pr-check: build-and-test runs scripts/tests/run.sh when the PR changes no code`, `pr-check: the docs-only path of build-and-test uses no Gradle and no Android SDK`, `pr-check: the docs-only step checks python3, jq and java first and has its own timeout` (hàm `pr_docs_steps`, `pr_gradle_gated`, `pr_docs_step_tools`) | Câu mới: cùng ý định, chuỗi điều kiện là `steps.filter.outputs.code != 'true'` / `== 'true'`; số mong đợi giữ (1, `yes`, 5). Bản sao hỏng của `pr_gradle_gated` (perl) bỏ `- if: steps.filter.outputs.code == 'true'` của `setup-gradle`, vẫn mong `no` |
| `build-and-test` có `needs: changes` | Không có `needs` | `pr-check: the job-level if check fails on a copy that skips build-and-test` (bản sao chèn `if:` sau dòng `needs: changes`) | Câu mới: bản sao chèn `    if: github.event_name == 'pull_request'` (biểu thức GitHub cho phép ở cấp job; `steps.` thì không) ngay sau dòng `  build-and-test:`; vẫn mong 1 |
| `merge-if-green.sh` đòi `changes` và `build-and-test` | Chỉ đòi `build-and-test` | `pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml` (mong `names=2 missing= none` sau T-075), `pr-check: the job name check names a renamed job` | Câu mới: `names=1 missing= none`; bản sao đổi tên: `names=1 missing= build-and-test` |
| Dữ liệu check run của PR xanh có `changes` | Chỉ có `build-and-test` | Mọi test dùng `green_runs`, `red_docs_runs`, `skipped_docs_runs`, `lagging_runs`, `mig_run` | Dữ liệu bỏ dòng `changes`, câu khẳng định giữ nguyên. Giữ dòng `ui-test` `skipped` trong `green_runs` và `mig_run`: T-075 giữ nó để các test đó kiểm thêm "check run lạ bị bỏ qua không chặn" (`ui-test` cũ trên commit kiểm trước T-075); ý định đó vẫn đúng sau T-076. `MIG_CHANGES` đổi thành `MIG_BUILD` (kết luận của `build-and-test`); test `merge-if-green does not merge a red PR when the API fails` dùng `MIG_BUILD=failure`, vẫn mong 1 |
| `code_runs` của T-075 (`changes`, `build-and-test` xanh, không có `ui-test`) | Chỉ có `build-and-test` | Test 21 tới 23 của T-075: `merge-if-green: a code PR without ui-test is ready` (mong 0), `... a running ui-test is not waited for` (thêm `ui-test` `in_progress`, mong 0), `... a red ui-test left from before is still refused` (thêm `ui-test` `failure`, mong 1, `có kiểm tra không qua: ui-test.`) | Giữ ba test và câu khẳng định; dữ liệu `code_runs` bỏ dòng `changes`, thành dữ liệu một job, và chính là `one_job_runs` của bảng L-058 (dùng chung một biến, không thêm biến mới). Test 21 nay kiểm cùng điều M1 với tên khác; giữ test 21 nguyên tên và câu để không phải viết lại câu nào của T-075, M1 giữ cho bảng L-058 của plan này (trùng nhau, rẻ) |
| Dữ liệu job của `wait-run` có `changes` | Lần chạy chỉ có `build-and-test` | `wait-run: a red PR check names the failed job` | Dữ liệu: `[{"name": "build-and-test", "conclusion": "failure"}]`, mong `1 ĐỎ` như cũ; thêm câu dòng đỏ nêu `job: build-and-test` (W1) |

Các câu này viết lại ngay trong commit test của bước 3, là test viết trước của plan, không phải câu viết lại giữa phiên theo ADR-0031 (không tính vào giới hạn 3 câu).

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`, bản sao hỏng của workflow trong `"$work/prwf"` (L-055), `gh` giả `scripts/tests/fake-gh` với `FIXTURE`, `merge_routes`/`merge_run` và `mig_run` có sẵn. Toàn bộ thay đổi của file test (test mới, hàm phụ trợ, dữ liệu một job, câu viết lại ở mục 3.5) vào commit test của bước 3; commit code của bước 4 chỉ sửa `pr-check.yml` và `merge-if-green.sh` (L-025). Trên code cũ (trạng thái sau T-075), các test sau thất bại đúng lý do: P1 (hai job), P2 (còn `needs`), P3 tới P6 (không có bước `id: filter` trong `build-and-test`), M1, M3, M4, M5, M7 (script cũ chờ check `changes`: mã 2), các test có sẵn dùng dữ liệu một job (mã 2 thay vì 0), câu viết lại của `pr_docs_steps`, `pr_gradle_gated`, `pr_docs_step_tools`, `pr_missing_jobs`.

Hàm phụ trợ mới (commit test):

- `pr_jobs <workflow>`: in tên các job (dòng `^  [A-Za-z0-9_-]+:$` sau dòng `^jobs:$`), cách nhau dấu cách.
- `pr_filter_check <workflow> <code> <release>`: lấy khối `run: |` của bước `name: Kiểm kết quả lọc đường dẫn` trong `build-and-test` (từ `pr_steps`, bỏ thụt lề), chạy bằng `bash -eo pipefail -c` (như `shell: bash` của GitHub) trong một thư mục tạm của `$work`, với `CODE=<code> RELEASE=<release>`, in mã thoát; khi mã khác 0 in thêm số dòng của `build.log` trong thư mục đó.
- `pr_sim <workflow> <đường dẫn>...`: một đoạn `python3 -I` đọc workflow: (1) lấy khối `filters` của bước có `id: filter` trong `build-and-test`, đổi mỗi mẫu glob thành biểu thức chính quy, chỉ nhận bốn dạng đang dùng: `thư-mục/**` (mọi file dưới thư mục), `*.đuôi` hay `*.gradle.kts` ở gốc (không có `/`), `**/tên` (tên file ở gốc hay ở thư mục bất kỳ), và đường dẫn đúng tên (`gradlew`, `.github/workflows/pr-check.yml`); mẫu khác (có `{`, `[`, `!`, `?`, `**` giữa mẫu, hay `*` trong thư mục) thì in `ERROR mẫu chưa hỗ trợ: <mẫu>`, để không đoán sai ngữ nghĩa của picomatch; tính `code`, `release` cho danh sách đường dẫn của PR mẫu; (2) với từng bước của `build-and-test`, tính điều kiện `if:` bằng cách thay `steps.filter.outputs.<x>` bằng giá trị vừa tính, `always()` bằng đúng, `failure()` bằng sai, `&&` bằng `and`; (3) in `code=<..> release=<..>` rồi tên (hay dòng `run`/`uses` đầu tiên) của các bước sẽ chạy, mỗi bước một dòng. Bước không có `if:` luôn chạy. Không có bước `id: filter`, hay điều kiện còn biểu thức lạ (ví dụ `needs.`), thì in `ERROR <lý do>` (test thất bại).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| DoD 1: một job | Test cấu trúc | P1. `pr-check: has exactly one job, build-and-test`: `pr_jobs "$pr_wf"` mong `build-and-test`. Bản sao hỏng chép job `build-and-test` thành job thứ hai `extra:`: mong `build-and-test extra` (khác, test bắt được) |
| DoD 1: không job nào chờ job khác | Test cấu trúc | P2. `pr-check: no job needs another and no step reads needs outputs`: `grep -cE '^    needs:|needs\.[a-z_-]+\.outputs' "$pr_wf"` mong 0. Bản sao hỏng thêm `    needs: changes` vào `build-and-test`: mong 1 |
| DoD 1: bộ lọc đứng trước mọi bước đọc nó | Test cấu trúc | P3. `pr-check: the path filter and its check come before every step that reads them`: trong `pr_steps "$pr_wf"`, (a) bước có `id: filter` dùng `uses: dorny/paths-filter@v3`, không có `if:`; (b) bước ngay sau nó là `name: Kiểm kết quả lọc đường dẫn`, không có `if:`; (c) mọi bước đứng trước bước lọc chỉ có thể là `uses: actions/checkout@v5` (không bước nào khác); (d) mọi bước có `steps.filter.outputs` đứng sau bước lọc; số bước có `steps.filter.outputs` là 6 (bước kiểm, nhánh tài liệu, `setup-gradle`, cổng chất lượng, release, lưu báo cáo; bước bình luận đọc `steps.release.outcome`, không tính). Mong `yes yes yes yes 6`, đúng cho cả cách chính (bước lọc đầu tiên) và cách dự phòng (sau checkout, mục 3.4). Bản sao hỏng: dời bước lọc xuống cuối job (mong (d) `no`); chèn bước `setup-java` lên trước bước lọc (mong (c) `no`); thêm `if: always()` vào bước kiểm (mong (b) `no`) |
| Lọc sai thì PR code không lọt sang nhánh tài liệu | Test cấu trúc | P4. `pr-check: every filter output read is defined, and every filter is read`: tập tên `steps.filter.outputs.<x>` trong workflow bằng đúng tập khóa của khối `filters` (`code release`), và không có `steps.<id>.outputs` với `<id>` khác `filter` (bước `steps.release.outcome` không tính: là `outcome`). Mong `reads=code release defined=code release other=0`. Hai bản sao hỏng: đổi một `steps.filter.outputs.code` thành `steps.filter.outputs.cod` (mong khác), và đổi `id: filter` thành `id: paths` (mong khác) |
| Kết quả lọc rỗng hay lạ dừng job | Test chạy bước | P5. `pr-check: the filter check stops the job unless both outputs are true or false`: `pr_filter_check "$pr_wf"` với `true false` mong 0, `false true` mong 0, `false false` mong 0, `'' false` mong 1, `true ''` mong 1, `yes false` mong 1; ca `'' false` còn mong `build.log` có 1 dòng lý do; thêm câu bước kiểm đặt `CODE: ${{ steps.filter.outputs.code }}` và `RELEASE: ${{ steps.filter.outputs.release }}` (grep trong bước, mong `1 1`) |
| PR mẫu chạm code chạy `check.sh` (ticket, Rủi ro sơ bộ) | Test PR mẫu | P6a. `pr-check: a PR touching app code runs check.sh, not the docs-only path`: `pr_sim "$pr_wf" app/src/main/java/A.kt` mong `code=true release=false`, các bước chạy có `Cổng chất lượng (scripts/check.sh)` và `setup-gradle`, không có `Test của script và tài liệu (scripts/tests/run.sh)`, không có `Build bản release (R8)`. Lặp cho mỗi đường dẫn mẫu của bộ lọc `code`: `core/ads/src/main/A.kt`, `feature/sample/src/test/B.kt`, `build-logic/convention/C.kt`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `.editorconfig`, `scripts/merge-if-green.sh`, `scripts/tests/run.sh`, `.github/workflows/pr-check.yml`: mong `code=true` mỗi lần |
| PR mẫu chỉ tài liệu không cần Gradle | Test PR mẫu | P6b. `pr-check: a docs-only PR runs only scripts/tests/run.sh`: `pr_sim "$pr_wf" docs/a.md README.md` mong `code=false release=false`, bước chạy có nhánh `scripts/tests/run.sh`, không có `Cổng chất lượng`, không có `setup-gradle`. Thêm: `.github/workflows/staging.yml` một mình mong `code=false` (workflow khác ngoài bộ lọc) |
| PR đổi file build chạy build bản release | Test PR mẫu | P6c. `pr-check: a PR changing a build file also builds the release`: `pr_sim "$pr_wf" app/build.gradle.kts` và `pr_sim "$pr_wf" app/proguard-rules.pro` mong `code=true release=true`, bước chạy có `Build bản release (R8)`. Thêm: `build.gradle.kts` ở gốc mong `code=true release=true` (khớp mẫu đúng tên `build.gradle.kts`); `proguard-rules.pro` ở gốc mong `code=false release=true` (chỉ mẫu `**/proguard-rules.pro` khớp được nó, chứng minh `**/` khớp cả file ở gốc; `code=false` nên bước `Build bản release (R8)` không chạy, đúng điều kiện hiện có); `docs/x.gradle.kts` mong `code=false release=false` (`*.gradle.kts` chỉ ở gốc, `**/build.gradle.kts` cần đúng tên). Đối chứng L-055: PR gồm `docs/a.md` và `app/src/main/java/A.kt` (một file code giữa tài liệu) mong `code=true`. Mẫu chưa hỗ trợ: `pr_sim` trên bản sao thêm mẫu `'app/{a,b}/**'` vào `code` mong dòng `ERROR mẫu chưa hỗ trợ` |
| Bộ lọc của PR mẫu đọc đúng workflow | Test PR mẫu, đối chứng | P6d. Mọi bản sao dựng bằng `sed`/`awk` trên `$pr_wf` trong `"$work/prwf"`, không lấy từ git. (1) Bỏ dòng `- 'scripts/**'` khỏi bộ lọc `code`: `pr_sim` với `scripts/merge-if-green.sh` mong `code=false` (P6a sẽ đỏ trên bản đó). (2) `sed 's/steps\.filter\.outputs/needs.changes.outputs/g'`: mong dòng `ERROR` (điều kiện lạ, như workflow trước T-076). (3) Bỏ dòng `id: filter`: mong dòng `ERROR` (không có bước lọc) |
| DoD 2: merge-if-green chỉ đòi `build-and-test` | Test script (L-058) | M1 tới M7, bảng dưới |
| DoD 2: wait-run không đọc job `changes` | Test script | W1. `wait-run: the red line names build-and-test`: lần chạy đỏ, jobs `[{"name": "build-and-test", "conclusion": "failure"}]`, dòng in có `job: build-and-test`. Qua cả trên code cũ (không sửa `wait-run.sh`): đây là kiểm dữ liệu một job, ghi vào ngoại lệ dưới |
| Tên job, bước mà `main-guard-verified.sh` đọc vẫn còn | Test có sẵn | `main-guard-verified: pr-check.yml has the job and the step it reads`, giữ nguyên |
| DoD 1 trên GitHub thật | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | PR của chính ticket này (PR code): `scripts/wait-run.sh --pr <số>` xanh; `gh api "repos/VGL-Studio/app-project-template/actions/runs/<id>/jobs?per_page=50" --jq '[.jobs[] | {name, steps: [.steps[] | select(.name | test("Lọc|Kiểm kết quả|Cổng chất lượng|scripts/tests/run.sh")) | {name, conclusion}]}]'` cho đúng một job `build-and-test`, bước `Cổng chất lượng (scripts/check.sh)` `success`, bước nhánh tài liệu `skipped`. Sau khi merge, PR tài liệu kế tiếp: cùng lệnh, một job, `Cổng chất lượng` `skipped`, nhánh tài liệu `success`. Ghi số lần chạy vào nhật ký |
| DoD 3 | Kiểm tra thủ công (người: merge và main-guard chỉ có trên GitHub thật) | Merge bằng `scripts/merge-if-green.sh <số PR> --update` chạy trong thư mục ở nhánh task (mục 3.1, Chuyển tiếp); `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` xanh (cách `full`, vì đổi `pr-check.yml`) |

**Bảng L-058: PR có `pr-check` một job đi qua từng kiểm tra của `merge-if-green.sh`.** Dữ liệu `one_job_runs` là `code_runs` của T-075 sau khi bỏ dòng `changes`: check run duy nhất `build-and-test` `completed`/`success` trên `abc1234`. `green_runs` (dùng chung) sau commit test có `build-and-test` `success` và dòng `ui-test` `skipped` giữ từ T-075.

| Kiểm tra hiện có | PR một job | Test |
| --- | --- | --- |
| PR mở, không phải bản nháp, nhắm `main`, không mang `needs-human` | Như cũ: từ chối trước khi đọc check | Test có sẵn (`merge-if-green refuses a PR labelled needs-human`, `... still refuses a PR into another branch`, `... does not reach the merge of a needs-human PR`), chạy trên dữ liệu một job sau commit test, giữ câu |
| PR đã nằm trên nhánh chính (L-080) | Như cũ | Test có sẵn của khối `mig_run`, dữ liệu một job |
| `main-broken` đang mở, ngoại lệ bản revert đúng (ADR-0025) | Như cũ | Test có sẵn (`merge-if-green does not merge while main is broken`, khối bản revert), dữ liệu một job |
| Phiên tự chạy đang chạy | Như cũ | Test có sẵn (`merge-if-green refuses a person's PR while a run is running`, `... lets the session's task PR through`), dữ liệu một job |
| Nhánh chậm hơn nhánh chính, `--update` | Cập nhật, chờ CI một job, merge | M7. `merge-if-green --update: merges once the single build-and-test check of the new commit is green`: như test `--update` có sẵn, `commits/def5678/check-runs` là `__sequence__` `[]` rồi `one_job_runs`; mong 0 và lần ghi `PUT pulls/12/update-branch;PUT pulls/12/merge;...`. Code cũ: mã 2 (chờ `changes`) |
| Mọi check đỏ bị từ chối, kể cả check ngoài danh sách | Như cũ | M6. `merge-if-green: a leftover red changes check still refuses`: `build-and-test` `success` cạnh `changes` `failure` (commit được kiểm trước T-076); mong `1` và `có kiểm tra không qua: changes.` |
| Check bắt buộc xanh | Chỉ `build-and-test` | M1. `merge-if-green: a PR whose only check is build-and-test is ready`: `one_job_runs`, `--dry-run`, mong 0 (code cũ: 2, `chưa thấy kiểm tra 'changes'`). M2. `... a red build-and-test is refused`: mong `1` và `có kiểm tra không qua: build-and-test.` |
| Check bắt buộc bị bỏ qua | Từ chối | M3. `merge-if-green: a skipped build-and-test is refused in the one-job shape`: mong `1` và `kiểm tra 'build-and-test' kết thúc với kết quả 'skipped'` (code cũ: 2) |
| Check chưa xong, job trễ trạng thái (L-017) | Chờ; job trễ của lần chạy xanh coi là xanh | M4. `merge-if-green: build-and-test still running waits`: `in_progress`, lần chạy `in_progress`: mong 2 và `đang ở trạng thái in_progress`. Test có sẵn `a lagging job of a green run counts as green` chạy trên `lagging_runs` dựng từ dữ liệu một job, mong 0 (code cũ: 2) |
| Chưa có check nào | Chờ, nêu đúng tên check | M5. `merge-if-green: no check yet names build-and-test`: `{"check_runs": []}`, mong 2 và `chưa thấy kiểm tra 'build-and-test'` (code cũ: nêu `changes`). Đối chứng L-055 cùng khối: chỉ có check `changes` `success` (không có `build-and-test`): mong 2, không merge |
| Cảnh báo commit mới, `docs/STATUS.md`, xóa nhánh sau merge | Như cũ | Test có sẵn dùng `green_runs`, dữ liệu một job, giữ câu |
| GitHub báo lỗi khi merge mà commit đã vào nhánh chính: kích hoạt `main-guard` (L-080) | Như cũ | Test có sẵn của khối `mig_run` với `MIG_FAIL='["PUT pulls/12/merge"]'` (`merge-if-green asks for main-guard after such a merge`) và với `MIG_FAIL` có thêm `POST actions/workflows/main-guard.yml/dispatches` (`... still reports the merge when main-guard cannot be started`), dữ liệu một job, giữ câu |

**Ngoại lệ viết test trước:** chú thích đầu `pr-check.yml` và chú thích cạnh `REQUIRED_CHECKS`: không có hành vi để kiểm. W1, M2, M6 và các test có sẵn của nhóm "Như cũ" qua cả trên code cũ hay chỉ đổi dữ liệu: chúng giữ cho trường hợp một job đi qua đúng các kiểm tra cũ (L-058), không phải test thất bại trước.

### 4.1. Chạy thử

Không cần (E dưới 2): `dorny/paths-filter@v3` đã dùng ở job `changes`; điều kiện `if:` đọc `steps.<id>.outputs` đã dùng ở `main-guard.yml` (`steps.mode.outputs.mode`, T-074); test cấu trúc workflow bằng `awk`/`grep`, bản sao hỏng và `fake-gh` đã có. Phần mới duy nhất là `pr_sim` đổi glob thành biểu thức chính quy trong `python3 -I` của test; nó chỉ nhận bốn dạng mẫu đang dùng và in `ERROR` với mẫu khác, kiểm bằng P6c (gốc và thư mục con) và đối chứng P6d. Hành vi thật của bộ lọc trong job có checkout kiểm trên PR của chính ticket này (mục 3.4, mục 4 dòng kiểm tra thủ công).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `.github/workflows/pr-check.yml`, `scripts/merge-if-green.sh`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một workflow, một dòng của script merge, test; không chạm code app, không thêm script |
| B. Độ rõ của yêu cầu | 2 | Ticket rõ, nhưng plan viết trên trạng thái giả định sau T-075 (plan T-075 còn draft, PR #318); hình dạng thật của T-075 có thể đổi diff và số đếm của test (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Cấu trúc workflow, đường đi của PR mẫu qua bộ lọc thật và mọi kiểm tra của script merge có test; cách `paths-filter` đọc API trong job có checkout chỉ thấy trên GitHub thật (một lần xem, dễ) |
| D. Ảnh hưởng chéo | 2 | Sửa `pr-check.yml`, `merge-if-green.sh`, `scripts/tests/run.sh` mà T-075 và T-077 cũng sửa; T-077 dựa vào cấu trúc mới. Không 3: hành vi mà nơi khác dựa vào (PR code chạy `check.sh`, PR đỏ không merge, tên job và bước mà `main-guard-verified.sh` đọc) giữ nguyên |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: dời một bước giữa hai job; `steps.<id>.outputs` trong `if:` đã có ở `main-guard.yml`; `pr_sim` là phần mới nhỏ trong test |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

`token_estimate: 5.4M` là số gợi ý của `scripts/token-usage.py estimate` cho cỡ S (trung vị 12 task). Phần test nhiều hơn phần code (khoảng 15 test mới, `pr_sim`, dữ liệu một job), nhưng không nâng: theo L-107, task thật thường dùng 0,4 tới 0,7 lần số nâng tay, và T-074 cỡ M cùng kiểu dùng 11.2M. Nếu task vượt thì ghi vào nhật ký để lần sau chỉnh cỡ.

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `pr-check.yml` (mọi PR), `merge-if-green.sh` (mọi lần merge), `scripts/tests/run.sh`.
- Task khác có thể xung đột: T-075 (cùng ba file, làm trước; plan này viết trên kết quả của nó); T-077 (sửa `pr-check.yml` thêm `types` và điều kiện PR nháp ở cấp job, sửa `merge-if-green.sh`; giả định đúng cấu trúc một job, bước `id: filter`). Test có sẵn `pr-check: build-and-test has no job-level if` sẽ phải đổi ở T-077 (điều kiện PR nháp), không phải ở T-076. T-078 (runner tự host) có thể đổi `runs-on` của `build-and-test`: một job thì chỉ một chỗ.
- `scripts/main-guard-verified.sh` (T-074): đọc job `build-and-test` và bước `Cổng chất lượng (scripts/check.sh)`, cả hai giữ tên; commit merge T-076 chạy `full` vì đổi `pr-check.yml`.
- Các plan ads đang mở không chạm các file này.
- Các app nhận qua `scripts/template-sync.sh`: workflow, script và test là của dự án mẫu.
- Đề xuất thứ tự: làm trong phiên có người, ngay sau khi T-075 merge, trước T-077.

## 7. Điều kiện dừng riêng

- Không giữ được tên job `build-and-test` hay tên bước `Cổng chất lượng (scripts/check.sh)` (ví dụ phải tách job lại): dừng, vì `merge-if-green.sh`, `main-guard-verified.sh` và T-077 đọc hai tên này.
- Cần thêm quyền cho `GITHUB_TOKEN` của `pr-check.yml`, hay thêm action mới: dừng.
- Trên PR của chính ticket này, PR code mà bước `Cổng chất lượng (scripts/check.sh)` bị bỏ qua (`code` ra `false`) sau phương án dự phòng ở mục 3.4: dừng, không merge.

## 8. Câu hỏi cho người duyệt

1. **Đối chiếu lại khi T-075 merge.** Plan giả định `pr-check.yml` sau T-075 có hai job `changes` (ra `code`, `release`) và `build-and-test`, không còn `ui-test`, và `merge-if-green.sh` có `REQUIRED_CHECKS=("changes" "build-and-test")`, không còn `SKIPPABLE_CHECKS`, cùng `code_runs` và test 21 tới 23 (khớp plan T-075 draft, PR #318). Đề xuất: chỉ duyệt plan sau khi T-075 merge; người duyệt (hay AI) đối chiếu `pr-check.yml`, `merge-if-green.sh` và các test `pr-check:`, `merge-if-green` có sẵn của nhánh chính lúc đó với mục 3.1, 3.5, 4; lệch thì soạn phiên bản 2.
2. **Bộ lọc không còn ai đọc.** Plan T-075 bỏ bộ lọc `ui`; nếu bản merge vẫn để lại, T-076 bỏ nó (test P4 đòi mỗi bộ lọc khai báo đều có bước đọc). Đề xuất đồng ý; test emulator sau merge (T-075) có danh sách riêng (`scripts/ui-test-needed.sh`).
3. **Bước kiểm kết quả lọc.** Thêm một bước vài giây sau bộ lọc, dừng job khi `code` hay `release` rỗng hoặc không phải `true`/`false`, để lỗi nối (đổi `id`, action đổi output) không làm PR code xanh mà chưa chạy `check.sh`. Bước này không bắt file code mà không mẫu nào khớp; phần đó chỉ test PR mẫu P6 giữ, với các đường dẫn mẫu đã liệt kê. Đề xuất giữ.
4. **Merge PR của chính T-076 bằng `merge-if-green.sh` của nhánh task**, vì bản trên nhánh chính lúc đó còn chờ check `changes` (mục 3.1, Chuyển tiếp); chỉ merge khi không có phiên tự chạy `running` hay tạm dừng; sau merge báo anh Nhật và Tuấn kéo nhánh chính. Đề xuất đồng ý; ghi vào nhật ký.
5. **Không viết ADR mới, không ghi chú vào ADR có sẵn.** Không ADR nào nêu job `changes`; ADR-0010 mục 1 ("toàn bộ kiểm tra của `pr-check.yml`") và ADR-0039 (tên job, tên bước) vẫn đúng. Cấu trúc một job ghi ở chú thích đầu `pr-check.yml`. Đề xuất đồng ý; nếu người duyệt muốn ghi lại, thêm một dòng ghi chú vào ADR-0010 (không lấy số 0040 tới 0042 đã giữ cho T-075, T-077, T-078).
6. **Chi phí chuyển tiếp.** PR đang mở lúc T-076 merge phải cập nhật nhánh và chạy lại `pr-check` một lần (việc này vẫn xảy ra với mọi commit mới của nhánh chính). Đề xuất chấp nhận.
7. **Bản `merge-if-green.sh` cũ chờ check `changes` mãi.** Sau khi T-076 merge, bản script cũ (worktree cũ, thư mục chưa kéo, nhánh chính của app trước đợt đồng bộ) trả mã 2 `chưa thấy kiểm tra 'changes'` mãi, và skill `merge-pr` dạy gọi lại sau. Plan xử lý bằng thứ tự chuyển tiếp (mục 3.1). Đề xuất thêm, ngoài phạm vi T-076 (cần mẫu log và test của `failure-kind.sh`, và một câu trong `docs/ops/to-app.md`): một dòng bảng tra lỗi cho mẫu `chưa thấy kiểm tra 'changes'` với cách xử lý "kéo nhánh chính, chạy lại bằng script mới; PR đồng bộ của app thì chạy script của nhánh đồng bộ". Người chọn: làm ở một PR quy trình riêng sau T-076, hay gộp vào T-076 (khi đó mục 3.2 thêm `docs/ops/failure-playbook.md`, `scripts/tests/playbook/` và `docs/ops/to-app.md`).

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
