---
ticket: T-039
title: PR chỉ có tài liệu vẫn chạy test của script
status: done   # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ci)
risk_level: high             # low | medium | high
token_estimate: 5M           # cỡ S, số gợi ý 5.0M (scripts/token-usage.py estimate, 2026-10-25); một workflow, một file test, một đoạn tài liệu
blocking_points: [ci]
run_order:                   # làm trong phiên có người theo dõi, không qua phiên tự chạy
reviewer: Nhật                  # người duyệt
cross_reviewer: Nhật            # bắt buộc khi risk_level là high
approved_at: 2026-10-25
---

# PLAN T-039: PR CHỈ CÓ TÀI LIỆU VẪN CHẠY TEST CỦA SCRIPT

## 1. Tóm tắt

Job `build-and-test` của `pr-check.yml` có thêm một nhánh nhẹ: khi bộ lọc `code` không khớp (PR chỉ có tài liệu, hay file ngoài bộ lọc), job checkout, cài Java 17 rồi chạy `scripts/tests/run.sh`, không Gradle, không Android SDK, khoảng 2 phút. Tên job giữ nguyên nên `scripts/merge-if-green.sh` không đổi: nó đã đòi `build-and-test` xanh, và từ chối mọi kiểm tra đỏ. `run.sh` có thêm một test dựng trang tiến độ từ chính tài liệu của repo, để số bài học trùng cũng làm PR đỏ. Kiểm chứng bằng test cấu trúc workflow và test `merge-if-green.sh` với `fake-gh` trong `scripts/tests/run.sh`, cộng một PR thử chỉ có tài liệu sau khi merge.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-040 (`docs/lessons.md`), mục tiêu đã được người phụ trách duyệt:

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| L-040, ý 1 | Khi một PR chỉ sửa tài liệu thì `scripts/tests/run.sh` chạy trên PR đó | Chạy trong job `build-and-test`, ở bước riêng có `if: needs.changes.outputs.code != 'true'`. Áp dụng cho mọi PR mà bộ lọc `code` không khớp, không chỉ `docs/**` (ví dụ `README.md`, workflow khác ngoài `pr-check.yml`). |
| L-040, ý 2 | Khi PR chỉ có tài liệu thì job nhẹ, không Gradle, không Android SDK, khoảng 1 đến 2 phút | Không `setup-gradle`, không `./gradlew`, không `scripts/check.sh` ở nhánh nhẹ. Java 17 vẫn cài vì test `DesignCompare.java` của `run.sh` cần `java` (dòng 687 đến 695). |
| L-040, ý 3 | Khi `run.sh` đỏ trên PR chỉ có tài liệu thì PR không merge được | `build-and-test` đỏ, `merge-if-green.sh` từ chối ("có kiểm tra không qua"). Không đổi script merge. |
| L-040, ý 4 | Khi PR có code thì `build-and-test` chạy y như trước | `scripts/check.sh` (đã gọi `run.sh`), build release khi bộ lọc `release` khớp, lưu báo cáo, bình luận lỗi. |
| L-040, ý 5 | Khi tài liệu của PR có lỗi mà các script tài liệu bắt được thì PR đỏ | Hiện `run.sh` kiểm trên tài liệu thật của repo: `foundation.md` khớp ADR, quét mã thật toàn repo, danh sách chạy mới nhất (quy tắc 11 của `run-check.py`), `status.py --check` và `foundation.py --check` trên bản sao do `new-app.sh` tạo (bản này có sổ bài học rỗng). Số bài học trùng chỉ có test trên tài liệu giả, nên hôm nay không làm PR đỏ; chỉ `status-page.yml` đỏ sau khi merge. Plan thêm test W8: chép `docs/` vào `$work`, chạy `status.py` với `STATUS_ROOT`. |
| L-073 (đã áp dụng) | PR chỉ sửa script chạy `run.sh` | Giữ nguyên: `scripts/**` vẫn trong bộ lọc `code`, đi nhánh đầy đủ. |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md)

ADR liên quan: ADR-0002, ADR-0010 (cổng chất lượng mềm; điểm 2 "commit chỉ sửa tài liệu thì bỏ qua" ở `main-guard`, Hệ quả "PR chỉ sửa tài liệu thì bỏ qua toàn bộ phần build": plan vẫn không build, chỉ thêm test của script, nên không trái ADR), ADR-0025 (`main-guard`, `find-breaking-commit.sh` và `main-guard-status.sh` bỏ qua commit chỉ có tài liệu).

## 3. Giải pháp

### 3.1. Cách làm

**Hiện trạng (đọc từ code, nhánh chính `bf7a671`):**

- `pr-check.yml`: job `changes` (dorny/paths-filter) ra `code`, `ui`, `release`. `build-and-test` luôn chạy (không có `if:` ở mức job); với PR không có code, bước duy nhất chạy là `echo "PR không chạm vào code, bỏ qua build."`, nên job xanh trong vài giây. `ui-test` có `if: needs.changes.outputs.ui == 'true'`, nên `skipped`.
- `merge-if-green.sh`: `REQUIRED_CHECKS=("changes" "build-and-test")` phải `success`; `SKIPPABLE_CHECKS=("ui-test")` được `success` hay `skipped`; mọi kiểm tra `failure`, `cancelled`, `timed_out`, `action_required`, `startup_failure` đều bị từ chối, kể cả kiểm tra ngoài hai danh sách. Vậy hôm nay PR chỉ có tài liệu có hình dạng `changes` xanh, `build-and-test` xanh (rỗng), `ui-test` bỏ qua, và được merge mà không có test nào.
- `main-guard.yml`: `paths-ignore: ['docs/**', '**/*.md']`, nên commit chỉ có tài liệu trên nhánh chính cũng không chạy gì. `find-breaking-commit.sh` và `main-guard-status.sh` dựa vào điều này (bỏ qua commit chỉ có tài liệu khi tìm commit gây lỗi); `scripts/tests/run.sh` có test giữ nguyên `paths-ignore` này.

**Thay đổi, chỉ ở job `build-and-test` của `pr-check.yml`:**

```yaml
  build-and-test:
    needs: changes
    runs-on: ubuntu-latest
    timeout-minutes: 45
    steps:
      - uses: actions/checkout@v5

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 17

      # A PR without code (docs only, or files outside the code filter) runs only the script tests, which also
      # check the docs: lesson numbers, foundation.md, status.py, secret scan, the latest run list (L-040).
      # No Gradle and no Android SDK here.
      - name: Test của script và tài liệu (scripts/tests/run.sh)
        if: needs.changes.outputs.code != 'true'
        timeout-minutes: 10
        run: |
          for tool in python3 jq java; do
            command -v "$tool" > /dev/null || { echo "::error::CI thiếu $tool để chạy scripts/tests/run.sh."; exit 1; }
          done
          scripts/tests/run.sh 2>&1 | tee build.log

      - if: needs.changes.outputs.code == 'true'
        uses: gradle/actions/setup-gradle@v4

      # ... "Cổng chất lượng (scripts/check.sh)", "Build bản release (R8)", "Lưu báo cáo test và ảnh screenshot":
      #     unchanged, still gated by code == 'true'.

      - name: Bình luận lỗi vào PR
        if: failure()
        # body as today; the build.log grep gains '^FAIL - ' (the failure line of run.sh), and every
        # `tail -n N <log>` becomes `tail -n N <log> 2>/dev/null || echo "Không có log."`, since a failure
        # before the log exists (checkout, Java) must still post the comment.
```

- Bước `echo` "Chỉ thay đổi tài liệu" bị bỏ: bước nhẹ thay nó.
- Checkout và `setup-java` bỏ điều kiện `code == 'true'`: cả hai nhánh cần. `setup-java` thêm khoảng 5 đến 10 giây cho nhánh nhẹ; dùng cùng Java 17 với nhánh đầy đủ thay vì Java có sẵn trên runner (câu hỏi 4).
- `python3` và `jq` có sẵn trên `ubuntu-latest` (nhánh đầy đủ đã chạy `run.sh` qua `check.sh` trên cùng runner). Bước nhẹ kiểm cả ba công cụ trước, để thiếu công cụ ra một dòng lỗi rõ thay vì một test đỏ khó đọc, như `check.sh` làm cho `python3` và `jq`.
- Không thêm job mới, nên không đổi `REQUIRED_CHECKS` hay `SKIPPABLE_CHECKS` (câu hỏi 2). Chú thích đầu `pr-check.yml` ghi thêm: `build-and-test` có hai nhánh, nhánh nhẹ cho PR không có code (L-040).
- `main-guard.yml` không đổi (câu hỏi 1): `merge-if-green.sh` chỉ merge khi nhánh PR chứa commit mới nhất của nhánh chính (`behind_by == 0`) và ghim merge vào đúng commit đã kiểm, nên cây file của commit squash trên nhánh chính trùng cây file mà `build-and-test` vừa kiểm. ADR-0010, điểm 3, cấm đẩy thẳng lên nhánh chính.
- `docs/auto-run.md`, mục 4 (Kết thúc phiên), bước 4 "Merge PR báo cáo": PR báo cáo chỉ có tài liệu giờ có CI khoảng 2 phút; gọi `scripts/merge-if-green.sh <số PR> --update` để script chờ kiểm tra đang chạy; đỏ thì đọc lỗi bằng `scripts/wait-run.sh --pr <số>`, sửa tài liệu trên nhánh báo cáo, đẩy lại (dòng `FAIL - ` nói test nào). Câu cũ "Commit merge chỉ đổi tài liệu thì `main-guard` không chạy" ở bước 9 (Merge) vẫn đúng, giữ nguyên.

**`merge-if-green.sh` thấy PR chỉ có tài liệu thế nào (L-058).** Script không đổi; bảng dưới liệt kê mọi kiểm tra hiện có theo thứ tự trong script, PR chỉ có tài liệu qua hay bị chặn ở từng kiểm tra, và test giữ điều đó (test có sẵn hay test mới của mục 4):

| Kiểm tra của `merge-if-green.sh` | PR chỉ có tài liệu sau plan | Test |
| --- | --- | --- |
| Tham số (`<số PR> [--dry-run \| --update]`) | Như mọi PR | Có sẵn: `merge-if-green rejects '<args>'` |
| PR đang mở | Như mọi PR | Không đổi; dùng trong mọi fixture `merge_routes` |
| Đã có trên nhánh chính (L-080) | Như mọi PR | Có sẵn: `merge-if-green reports a merge that GitHub answered with an error` và các test `ĐÃ MERGE TRƯỚC ĐÓ` |
| Bản nháp | Bị chặn như mọi PR | Có sẵn (khối `merge_routes` bản nháp) |
| Nhãn `needs-human` | Bị chặn như mọi PR (PR plan của hàng chờ là PR chỉ có tài liệu mang nhãn này) | Có sẵn: `merge-if-green refuses a PR labelled needs-human` |
| Nhánh đích `main` | Như mọi PR | Không đổi |
| `main-broken` đang mở | Bị chặn, trừ bản revert đúng; PR chỉ có tài liệu không bao giờ là bản revert của commit code | Có sẵn: các test `main-broken` và `is-exact-revert` |
| Phiên đang chạy | Bị chặn trừ PR của phiên (PR báo cáo là PR chỉ có tài liệu của phiên, vẫn qua) | Có sẵn: `merge-if-green lets the session's report PR through`, `refuses a person's PR while a run is running` |
| Nhánh chậm hơn nhánh chính | `CẦN CẬP NHẬT` hay `--update` như mọi PR; sau khi cập nhật, `build-and-test` chạy lại nhánh nhẹ trên commit mới | Có sẵn: các test `--update` |
| Không kiểm tra nào đỏ | **Mới có ý nghĩa:** `run.sh` đỏ thì `build-and-test` `failure`, bị chặn với "có kiểm tra không qua: build-and-test" | **Mới:** M1 |
| `changes` phải `success` | Như trước | Có sẵn, trong `green_runs` |
| `build-and-test` phải `success` (không được `skipped`) | Xanh chỉ khi `run.sh` qua; job không bao giờ bị bỏ qua vì không có `if:` mức job | Có sẵn: `merge-if-green: a green PR is ready` (đúng hình dạng PR chỉ có tài liệu); **Mới:** M2 (`skipped` bị từ chối), W4 (không có `if:` mức job) |
| `build-and-test` còn chạy | Chờ (mã 2) hay `--update` chờ; nhánh nhẹ khoảng 2 phút | Có sẵn: `a job of a run still going waits`, `a lagging job of a green run counts as green` |
| `ui-test` được `skipped` | Vẫn `skipped` (bộ lọc `ui` không đổi) | Có sẵn, trong `green_runs` |
| Chú ý `docs/STATUS.md`, commit mới, nhánh trên máy lệch | Như mọi PR | Có sẵn |
| Tên job khớp giữa hai file | Không đổi tên | **Mới:** W6 |

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `.github/workflows/pr-check.yml` | Job `build-and-test`: checkout và `setup-java` chạy cho mọi PR; bước nhẹ `scripts/tests/run.sh` khi `code != 'true'` (kiểm `python3`, `jq`, `java`, `timeout-minutes: 10`); bỏ bước `echo`; bước bình luận lỗi `if: failure()`, grep thêm `^FAIL - `, `tail` chịu được log chưa có; chú thích đầu file. Job `changes`, `ui-test`, các bộ lọc không đổi |
| Sửa | `scripts/tests/run.sh` | Khối test mới ngay sau khối test `main-guard.yml` (sau dòng `expect "main-guard: has workflow_dispatch ..."`): W1 đến W7 cho `pr-check.yml`, M1 và M2 cho `merge-if-green.sh` (dùng `merge_routes`, `merge_run` có sẵn, đặt sau định nghĩa của chúng); W8 đặt cuối khối test `status.py` có sẵn |
| Sửa | `docs/auto-run.md` | Mục 4 (Kết thúc phiên), bước 4 "Merge PR báo cáo": PR báo cáo có CI khoảng 2 phút, dùng `--update` để chờ, đỏ thì đọc `wait-run.sh --pr` và sửa tài liệu trên nhánh báo cáo (L-040) |
| Sửa | `docs/lessons.md` | Chỉ dòng **Áp dụng** của L-040, sau khi PR merge: thêm "commit chỉ có tài liệu: nhánh nhẹ của `build-and-test`, PR #<số>" và đổi **Trạng thái** sang `applied`, nếu người duyệt đồng ý ở câu hỏi 5 |
| Sửa | `docs/features/quy-trinh/tickets/T-039-pr-tai-lieu-chay-test-script.md` | Trạng thái ticket theo phiên |
| Sửa | `docs/features/quy-trinh/plans/T-039-pr-tai-lieu-chay-test-script.md` | Nhật ký thực hiện |

Nơi khác đọc các file trên (L-020; `scripts/plan-check.py`, quy tắc 2), không phải sửa:

- `scripts/merge-if-green.sh` đọc tên job của `pr-check.yml` (`changes`, `build-and-test`, `ui-test`): không đổi tên, W6 giữ điều này.
- `scripts/wait-run.sh` chờ lần chạy `pr-check.yml` của commit đầu PR và in job đỏ: không đổi tên workflow hay job, dòng đỏ vẫn nói `build-and-test`.
- `scripts/check.sh` (chú thích dòng 4: là job `build-and-test` của `pr-check.yml`) vẫn đúng cho nhánh đầy đủ; không sửa (`CLAUDE.md`, Không được làm).
- `.github/workflows/device-session.yml` (chú thích nói cùng cách dựng emulator như `ui-test`) và `.github/workflows/status-page.yml` (chú thích nói `pr-check` chạy trên PR vào `main`): vẫn đúng.
- `scripts/tests/playbook/PB-02.log`, `PB-03.log`: log mẫu của bảng tra lỗi có tên `pr-check`, `build-and-test`; vẫn đúng.
- `scripts/check.sh` gọi `scripts/tests/run.sh`: nhánh đầy đủ vẫn chạy `run.sh` qua `check.sh`, không chạy hai lần.
- `docs/lessons.md` được `scripts/status.py`, `scripts/tests/run.sh` (số bài học) đọc: chỉ sửa dòng Áp dụng và Trạng thái theo đúng định dạng có sẵn. `scripts/new-app.sh` làm rỗng sổ bài học cho app mới và `.github/ISSUE_TEMPLATE/from-app.yml` chỉ nhắc tên file: không phụ thuộc nội dung một mục.
- `docs/auto-run.md` được nhắc theo tên và số mục, số bước trong chú thích hay thông báo của `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/resume-check.py`, `scripts/save-branch.sh`: plan chỉ thêm câu vào bước 4 "Merge PR báo cáo" của mục 4, không đổi tiêu đề hay số mục, số bước nào, nên các chỗ nhắc vẫn đúng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. Action GitHub dùng lại đúng bản đang có trong `pr-check.yml` (`actions/checkout@v5`, `actions/setup-java@v5`); không thêm action mới

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `run.sh` chạy trên `ubuntu-latest` mà không có Android SDK. Chạy thử (mục 4.1) làm trên macOS; trên runner, `run.sh` đã chạy qua `check.sh` nhưng sau `setup-gradle` và với Android SDK có sẵn trên ảnh runner.
- **Điều kiện chuyển:** PR thử chỉ có tài liệu (mục 4, dòng kiểm tra thủ công) đỏ ở bước nhẹ vì thiếu một công cụ hay một biến môi trường mà nhánh đầy đủ có (không phải vì lỗi tài liệu cố ý).
- **Cách làm thay:** trong cùng bước nhẹ của `pr-check.yml`, cài hay trỏ tới công cụ thiếu (ví dụ `sudo apt-get install -y <gói>`, hay đặt biến chỉ bằng đường dẫn có sẵn trên runner), vẫn không Gradle và không tải Android SDK. Test W1 đến W7 và M1, M2 giữ nguyên. Thiếu thứ cần Android SDK thật thì dừng task (mục 7).

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code, commit riêng trước commit sửa workflow. Test cấu trúc workflow đọc file bằng `awk`/`grep` như test `main-guard.yml` và `status-page.yml` có sẵn. Mỗi hàm kiểm nhận đường dẫn file, để test chạy được cả trên bản sao bị sửa sai trong `$work` (đối chứng, L-055).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-040, ý 1 | Test script (cấu trúc workflow), W1 | `pr-check: build-and-test runs scripts/tests/run.sh when the PR changes no code`: trong job `build-and-test` (từ dòng `^  build-and-test:$` tới job kế), có đúng một bước có `if: needs.changes.outputs.code != 'true'` và dòng `scripts/tests/run.sh 2>&1 \| tee build.log`. Hiện tại: thất bại (chỉ có bước `echo`) |
| L-040, ý 1 | Test script, W3 | `pr-check: checkout and Java run for every PR`: bước `actions/checkout@v5` và `actions/setup-java@v5` của `build-and-test` không có dòng `if:`. Hiện tại: thất bại (cả hai có `code == 'true'`) |
| L-040, ý 2 | Test script, W2 | `pr-check: the docs-only path of build-and-test uses no Gradle and no Android SDK`: mọi bước của `build-and-test` có `setup-gradle`, `gradlew`, `scripts/check.sh`, `ANDROID_HOME` hay `android-emulator-runner` đều có `if:` chứa `needs.changes.outputs.code == 'true'`. Hiện tại: qua; đối chứng: chép workflow vào `$work`, xóa `if:` của bước `setup-gradle` bằng `sed`, hàm kiểm trả "no" |
| L-040, ý 2 | Test script, W7 | `pr-check: the docs-only step checks python3, jq and java first and has its own timeout`: bước nhẹ có `command -v`, đủ ba tên `python3`, `jq`, `java`, và `timeout-minutes: 10`. Hiện tại: thất bại |
| L-040, ý 3 | Test script (`merge-if-green.sh` với `fake-gh`), M1 | `merge-if-green: a docs-only PR whose build-and-test failed is refused`: `merge_routes claude/template-x '{"behind_by": 0}'` với `changes` `success`, `build-and-test` `failure`, `ui-test` `skipped`; `merge_run 12 --dry-run` ra `1` và `mg.out` có `có kiểm tra không qua: build-and-test.`. Hiện tại: qua (script không đổi); đối chứng: cùng fixture với `build-and-test` `success` ra `0` (cạnh nhau trong cùng khối) |
| L-040, ý 3 | Test script, M2 | `merge-if-green: a skipped build-and-test is refused`: như M1 với `build-and-test` `skipped`; ra `1`, `mg.out` có `kiểm tra 'build-and-test' kết thúc với kết quả 'skipped'`. Hiện tại: qua; đối chứng như M1 |
| L-040, ý 3 | Test script, W4 | `pr-check: build-and-test has no job-level if`: giữa `^  build-and-test:$` và `^    steps:$` không có dòng `^    if:`. Hiện tại: qua; đối chứng: bản sao trong `$work` thêm `    if: needs.changes.outputs.code == 'true'` sau `needs: changes`, hàm kiểm trả "no" |
| L-040, ý 3 | Test script, W5 | `pr-check: the failure comment of build-and-test also runs for docs-only PRs`: bước `Bình luận lỗi vào PR` của `build-and-test` có `if: failure()` (không còn `code == 'true'`) và mẫu grep có `^FAIL - `. Hiện tại: thất bại |
| L-040, ý 5 | Test script, W8 | `status: the progress page builds from this repository's docs`: `cp -R docs "$work/status-real/docs"`, `STATUS_ROOT="$work/status-real" python3 -I scripts/status.py` thoát `0`. Hiện tại: qua (đã chạy thử ở mục 4.1); đối chứng: nối `### L-040. Trùng số` vào `docs/lessons.md` của bản sao, chạy lại thoát `1` và lỗi có `số bài học dùng hai lần: L-040` |
| Tên job khớp (L-058) | Test script, W6 | `pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml`: đọc hai mảng `REQUIRED_CHECKS`, `SKIPPABLE_CHECKS` của `merge-if-green.sh`, mỗi tên có dòng `^  <tên>:$` trong `pr-check.yml`. Hiện tại: qua; đối chứng: bản sao `pr-check.yml` đổi `build-and-test:` thành `build:`, hàm kiểm trả tên thiếu |
| L-040, ý 4 | Test script có sẵn và CI | `scripts/check.sh` vẫn qua trên PR của task (PR sửa `pr-check.yml` nên đi nhánh đầy đủ); W2 giữ mọi bước Gradle trong điều kiện `code == 'true'` |
| Các kiểm tra khác của `merge-if-green.sh` (L-058) | Test script có sẵn | Bảng ở cuối mục 3.1; không đổi |
| Nhánh nhẹ chạy thật trên GitHub | Kiểm tra thủ công (người: chỉ thấy trên GitHub sau khi merge, vì PR của task sửa `pr-check.yml` nên đi nhánh đầy đủ) | Sau khi merge: mở PR thử `claude/thu-L040-tai-lieu`, chỉ sửa `docs/lessons.md` thêm một mục trùng số `L-040` (lỗi cố ý). Kiểm: `build-and-test` đỏ ở bước "Test của script và tài liệu", log không có `gradle`, có dòng `FAIL - status: the progress page builds from this repository's docs`; bình luận lỗi có dòng đó; `scripts/merge-if-green.sh <số> --dry-run` thoát `1` với "có kiểm tra không qua: build-and-test"; ghi thời gian của bước. Bỏ lỗi cố ý, đẩy lại: `build-and-test` xanh, `--dry-run` ra `SẴN SÀNG`. Đóng PR, không merge, xóa nhánh |

**Ngoại lệ viết test trước:** W2, W4, W6, W8, M1, M2 qua ngay trên code hiện tại, vì chúng giữ hành vi có sẵn mà plan dựa vào (script merge không đổi; job không được bỏ qua; tên job khớp; tài liệu hiện tại đúng). Mỗi test có đối chứng trên bản sao sửa sai hay fixture ngược chiều, nằm cùng commit test, để thấy hàm kiểm thất bại đúng lý do. `docs/auto-run.md` và dòng Áp dụng của `docs/lessons.md` chỉ là tài liệu. Kiểm tra trên GitHub (dòng cuối bảng) chỉ làm được sau khi merge.

### 4.1. Chạy thử

- **Đã chạy thử:** `scripts/tests/run.sh` trên nhánh chính `bf7a671`, 2026-10-25, trên máy macOS của dev, trong môi trường sạch giống bước nhẹ: `env -i HOME=$HOME PATH=/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin TMPDIR=$TMPDIR`, không có `ANDROID_HOME`, không có `JAVA_HOME` (`java` là `/usr/bin/java`, JDK 26), không Gradle. Chạy hai lần: trong worktree đầy đủ lịch sử, và trong bản clone nông `git clone --depth 1` (như `actions/checkout` mặc định).
- **Lệnh:** `env -i ... scripts/tests/run.sh`. Không chạy `scripts/check.sh` vì nhánh nhẹ không dùng nó (tiêu chí E là 1, mục này không bắt buộc; ghi theo yêu cầu của người phụ trách).
- **Kết quả:** cả hai lần thoát `0`, 656 dòng `ok`, không dòng `FAIL`, dòng cuối `All script tests passed`. Thời gian: 95 giây (worktree), 98 giây (clone nông). Trên runner cộng khoảng 20 đến 40 giây cho checkout và `setup-java`. `run.sh` cần `bash`, `python3`, `jq` (dòng 2 của file) và `java` (test `DesignCompare.java`, dòng 687 đến 695); test `cloud-env-check.sh` dùng SDK, Java và `gradlew` giả trong `$work`, nên không cần Android SDK thật. Chưa chạy được trên Linux trước khi merge (mục 3.4).
- **Chạy thử W8:** chép `docs/` vào thư mục tạm, `STATUS_ROOT=<tạm> python3 -I scripts/status.py` thoát `0`; nối một mục `### L-040.` trùng số vào bản sao, chạy lại thoát `1` với `LỖI: số bài học dùng hai lần: L-040`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và quy trình phiên tự chạy: `.github/workflows/pr-check.yml`, `docs/auto-run.md`). Rủi ro cao, bắt buộc duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một job của một workflow, một file test, một đoạn tài liệu; không đổi code app hay script merge |
| B. Độ rõ của yêu cầu | 1 | Mục tiêu đã duyệt; còn quyết định `main-guard` và cách đặt ticket quy trình (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Cấu trúc workflow và cách đọc kết quả có test; nhánh nhẹ chạy thật chỉ kiểm được trên GitHub sau khi merge, bằng một PR thử |
| D. Ảnh hưởng chéo | 3 | Đổi cổng chất lượng của mọi PR; PR báo cáo và PR plan của phiên tự chạy giờ chờ khoảng 2 phút và có thể đỏ vì lỗi tài liệu |
| E. Độ mới của kỹ thuật | 1 | Biến thể của các bước có sẵn trong cùng job (bước có `if:` theo bộ lọc, `tee build.log`) |
| **Tổng** | 7 | **Mức: Cao** (chạm điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `.github/workflows/pr-check.yml` (mọi PR), `scripts/tests/run.sh` (file test lớn, PR từ app camera-pose hay sửa; dễ xung đột khi gộp), `docs/auto-run.md`.
- Task khác trong sprint có thể xung đột: plan khác cùng danh sách chạy mà thêm test vào `scripts/tests/run.sh` (khối mới đặt ngay sau khối test `main-guard.yml` để ít chồng dòng). Không task ads nào chạm `pr-check.yml`.
- Sau khi merge: mọi PR chỉ có tài liệu đang mở mà có lỗi tài liệu sẵn sẽ đỏ khi chạy lại (`--update`); đó là đúng ý, nhưng PR báo cáo của phiên có thể bị giữ tới khi sửa xong.
- Đề xuất thứ tự chạy: cuối danh sách, sau các task code, để PR báo cáo của chính phiên này là PR chỉ có tài liệu đầu tiên đi nhánh nhẹ; hoặc làm ngoài phiên, có người theo dõi PR thử.

## 7. Điều kiện dừng riêng

- Bước nhẹ cần Android SDK, Gradle hay một bí mật của repo để `run.sh` qua: dừng, không thêm `setup-gradle` hay bí mật vào nhánh nhẹ.
- Cần đổi `scripts/merge-if-green.sh`, `scripts/check.sh` hay `.github/workflows/main-guard.yml`: dừng (ngoài phạm vi đã duyệt; `main-guard` xem câu hỏi 1).
- Một test có sẵn trong `run.sh` đỏ trên PR của task mà không do task gây ra: dừng, ghi tên test, không sửa test đó.

## 8. Câu hỏi cho người duyệt

1. **`main-guard.yml` có chạy cho commit chỉ có tài liệu không?** Hiện bỏ qua (`paths-ignore: docs/**, **/*.md`). Đề xuất: giữ nguyên. Lý do: `merge-if-green.sh` chỉ merge nhánh không chậm hơn nhánh chính và ghim vào commit đã kiểm, nên cây file trên nhánh chính trùng cây mà nhánh nhẹ vừa kiểm; ADR-0010 cấm đẩy thẳng; `find-breaking-commit.sh` và `main-guard-status.sh` bỏ qua commit chỉ có tài liệu theo đúng giả định này, và `run.sh` có test giữ `paths-ignore`. Phương án khác: một job nhẹ trong `main-guard.yml` cho commit chỉ có tài liệu; khi đó phải sửa cả hai script tìm commit gây lỗi, logic mở và đóng `main-broken` và test của chúng, nên nên là một plan riêng.
2. **Nhánh nhẹ trong `build-and-test` hay một job mới (ví dụ `docs-check`)?** Đề xuất: trong `build-and-test`. Script merge, `wait-run.sh` và bảng tra lỗi không đổi. Job mới phải thêm vào `merge-if-green.sh` (bắt buộc hay được bỏ qua, có điều kiện ngược với `build-and-test`), tức là đổi thêm một file cổng chất lượng và các test của nó.
3. **Ticket quy trình đặt ở đâu?** Repo chưa có ticket nào có `spec:` rỗng. Đề xuất: thư mục `docs/features/quy-trinh/` không có `spec.md` (cách `status.py` nhận ticket quy trình), dùng cho mọi ticket quy trình sau này. Phương án khác: đặt trong thư mục một feature có sẵn với `spec:` rỗng.
4. **Java cho nhánh nhẹ:** Đề xuất `actions/setup-java@v5` với Java 17 như nhánh đầy đủ (thêm khoảng 5 đến 10 giây), để test `DesignCompare.java` chạy cùng phiên bản ở hai nhánh. Phương án khác: dùng Java có sẵn trên runner, nhanh hơn chút nhưng phiên bản đổi theo ảnh runner.
5. **Dòng Áp dụng của L-040:** Đề xuất: PR của task (hay PR báo cáo của phiên) ghi "commit chỉ có tài liệu: nhánh nhẹ của `build-and-test`, PR #<số>" và đổi trạng thái sang `applied` sau khi PR thử ở mục 4 đỏ và xanh đúng như mong đợi. Nếu muốn giữ `docs/lessons.md` cho buổi duyệt bài học, bỏ dòng đó khỏi mục 3.2.
6. **Duyệt chéo:** nhóm hiện có một dev; theo `risk-scoring.md`, mục 3, dev vừa duyệt vừa duyệt chéo và ghi lý do ở mục 9.

**Trả lời của anh Nhật (2026-10-25):** câu 1 đến 6: theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật ("Duyệt các plan"), 2026-10-25. Câu trả lời ở cuối mục 8. Không chạy trong RUN-20261026-01: chưa có bằng chứng phiên cloud đẩy được file workflow (rà soát trước phiên), nên AI làm trong phiên có người theo dõi, anh Nhật đồng ý.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### Phiên giám sát 2026-10-26, phiên bản plan 1

- **Kết quả:** xong trên nhánh, PR #199; rà soát độc lập 0 chặn, 2 nên sửa (đã sửa: dòng này, `pr_unconditional` báo `missing` khi không có bước, thêm đối chứng). Anh Nhật cho làm trong phiên có người theo dõi, 2026-10-26.
- **Nhánh, PR:** `claude/T-039-pr-tai-lieu-chay-test`, PR #199. Commit test `10a802f`, commit workflow `5ef1797`, commit tài liệu là commit chứa mục này.
- **Đã làm:** `pr-check.yml`, job `build-and-test`: checkout và `setup-java` (Java 17) chạy cho mọi PR; bước "Test của script và tài liệu (scripts/tests/run.sh)" khi `code != 'true'`, kiểm `python3`, `jq`, `java` trước, `timeout-minutes: 10`; bỏ bước `echo`; bước bình luận lỗi `if: failure()`, grep thêm `^FAIL - `, `tail` in "Không có log." khi chưa có log; chú thích đầu file. `docs/auto-run.md`, mục 4, bước 4: gọi `merge-if-green.sh <số PR> --update`, CI đỏ thì đọc `wait-run.sh --pr` và sửa tài liệu trên nhánh báo cáo. `docs/lessons.md`: L-040 sang `applied` (câu hỏi 5, anh Nhật đồng ý), dòng Áp dụng ghi "PR #199".
- **Test viết trước:** 12 test mới trong `scripts/tests/run.sh` (W1 đến W8, M1, M2, cộng 4 đối chứng: bản sao `setup-gradle` không điều kiện, bản sao có `if:` mức job, bản sao đổi tên job, M1 với `build-and-test` xanh). Trên workflow cũ đúng 4 test đỏ, đúng lý do của mục 4: W1 (`expected '1', got '0'`, chưa có bước `run.sh`), W3 (`got 'no no'`, checkout và Java còn điều kiện), W7 (`got '0'`), W5 (`got 'no'`, bình luận lỗi chỉ chạy cho code). W2, W4, W6, W8, M1, M2 và các đối chứng qua ngay như plan dự kiến.
- **Kết quả test:** `scripts/tests/run.sh` trên máy (macOS) thoát `0`, 681 dòng `ok`, `All script tests passed`. Chạy lại trong môi trường sạch giống bước nhẹ (`env -i`, không `ANDROID_HOME`, không `JAVA_HOME`): thoát `0`, 681 dòng `ok`. `actionlint` không báo lỗi cho `pr-check.yml`. Không chạy `scripts/check.sh` (không đổi code app hay Gradle; PR của task sửa `pr-check.yml` nên CI đi nhánh đầy đủ).
- **Lệch so với plan:** (1) hàm kiểm W2 và W5 tách các dòng của một bước bằng ký tự tab (`$'\t'`), không có trong plan, chỉ là cách viết. (2) Bước bình luận lỗi có thêm một dòng chú thích nói vì sao bỏ `code == 'true'`. (3) Dòng Áp dụng của L-040 đổi ngay trong PR của task với chữ "PR #199", không chờ PR thử của mục 4 như câu hỏi 5 viết; theo lời anh Nhật. Ngoài mục 3.2: không có.
- **Việc còn lại:** kiểm tra thủ công sau khi merge (mục 4, dòng cuối): PR thử `claude/thu-L040-tai-lieu` chỉ sửa `docs/lessons.md` với một mục `L-040` trùng số, thấy `build-and-test` đỏ ở bước nhẹ, không có Gradle trong log, có dòng `FAIL - status: the progress page builds from this repository's docs`; rồi xanh khi bỏ lỗi; đóng PR, không merge.
- **Cần người xem:** diff của `pr-check.yml` (điểm chặn CI). Số PR trong dòng Áp dụng của L-040 và trong mục này.
