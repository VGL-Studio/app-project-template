---
ticket: T-075
title: Test emulator chạy sau khi merge, không chạy ở từng PR
status: draft
version: 1
risk_score: 11
risk_level: high
token_estimate: 23.9M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-075: TEST EMULATOR CHẠY SAU KHI MERGE, KHÔNG CHẠY Ở TỪNG PR

## 1. Tóm tắt

Bỏ job `ui-test` khỏi `pr-check.yml` và khỏi `merge-if-green.sh`; thêm job `ui-test` vào `main-guard.yml`, chạy sau `guard` khi commit đổi code app (`scripts/ui-test-needed.sh`). Job chạy test emulator tối đa hai lần (`scripts/ui-test-on-emulator.sh`), rồi `scripts/ui-test-verdict.sh` kết luận `green`, `flaky`, `infra` hay `red`; chỉ `red` mở `main-broken` (issue có dòng `<!-- check:ui-test -->`, commit gây lỗi tìm bằng `find-breaking-commit.sh --check ui-test`), đỏ vì hạ tầng thì job đỏ mà không chặn merge. Kiểm bằng test của `scripts/tests/run.sh` (repo git tạm, `adb` và `gradlew` giả, `gh` giả, test cấu trúc của hai workflow) và một lần chạy thật trên emulator của máy dev. Theo ADR-0040 (đề xuất cùng PR plan này).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 3. Quyết định của anh Nhật ngày 2026-10-29: áp dụng cả sáu cách; T-074 tới T-077 làm có người (`auto_run: false`). T-074 đã merge (commit d7d5bc6, ADR-0039).

| Yêu cầu (phạm vi, định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| DoD 1 | PR chạm code app không chạy test emulator; `merge-if-green.sh` merge được khi `build-and-test` xanh | `pr-check.yml` không còn job `ui-test`; `SKIPPABLE_CHECKS` và vòng đọc nó bỏ khỏi `merge-if-green.sh`. Kiểm tra nào đỏ trên commit cuối vẫn bị từ chối, kể cả `ui-test` cũ |
| DoD 2 | Commit code trên main chạy test emulator một lần | Job `ui-test` của `main-guard.yml`, `needs: guard`, chạy khi `scripts/ui-test-needed.sh` in `yes` (danh sách đường dẫn là bộ lọc `ui` cũ của `pr-check.yml`). Chạy tay luôn `yes`. "Một lần" là một job; bên trong job chạy lại một lần khi lần đầu đỏ (mục 8, câu 3) |
| DoD 2 | Đỏ thì xử lý đúng cách đã chọn, có test | Đề xuất: đỏ thật mở `main-broken` như `guard` (mục 8, câu 2). Đỏ vì hạ tầng (PB-01, PB-11, PB-13, PB-15, không có log, không test nào chạy) không mở `main-broken` |
| Phạm vi: nơi chạy | Plan so với cách chạy mỗi đêm | Mục 3.1, "Vì sao trong `main-guard`"; người chọn ở mục 8, câu 1 |
| Phạm vi: nơi đọc kết quả | `find-breaking-commit.sh`, `main-guard-status.sh`, bước `main-broken`, `release.sh` thấy đúng | `find-breaking-commit.sh` thêm `--check ui-test`; `main-guard-status.sh` và `release.sh` không đổi (bảng L-058, mục 4) |
| Phạm vi: bảng tra lỗi | Viết lại PB-01, PB-10, PB-11, PB-13, PB-15 | Cột Thấy ở và Việc phải làm cho trường hợp sau merge; giữ mẫu và loại để test có sẵn của bảng không đổi |
| Phạm vi: tài liệu | ADR mới thay ADR-0010 mục 1 (ý emulator) và mục 4; `auto-run.md`, `run-check.py`, `CLAUDE.md` | ADR-0040; ghi chú ở đầu ADR-0010 và ADR-0019; danh sách đủ ở mục 3.2 |
| Phạm vi: `device-session` | Ghi hai lớp khác nhau thế nào | ADR-0040 mục 6; `docs/ops/test-sessions.md`, bảng nguồn |
| DoD 3 | Tài liệu và ADR cập nhật, `foundation.md` sinh lại | ADR-0040 `proposed` trong PR plan này; người ghi `accepted` khi duyệt (mục 8, câu 1, 2) |
| DoD 4 | CI qua, đã merge | Như mọi task |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0010 (mục 1 ý emulator, mục 4 bị thay), ADR-0019 (chẩn đoán emulator dời sang `main-guard`), ADR-0025 (chạy lại một lần, `main-broken`, bản revert đúng), ADR-0039 (`main-guard-verified.sh`, `GATE_FILES`), ADR-0040 (mới), ADR-0029 (phạm vi file), ADR-0031 (viết lại câu khẳng định).

## 3. Giải pháp

### 3.1. Cách làm

**Vì sao trong `main-guard`, sau `guard` (đề xuất cho mục 8, câu 1).** Số đo: `ui-test` của dự án mẫu chạy khoảng 4 tới 5 phút (lần chạy 40985194036 và 40919748520 của `pr-check`), 637 phút mỗi tuần, khoảng 1,7 lần mỗi PR. So sánh:

| | Job `ui-test` trong `main-guard` (đề xuất) | Workflow riêng mỗi đêm |
| --- | --- | --- |
| Phút mỗi tuần (dự án mẫu) | Khoảng 375 (mỗi commit code app một lần, bớt khoảng 260) | Khoảng 35 (bớt khoảng 600) |
| Khi nào biết lỗi | 5 tới 15 phút sau merge | Tới 24 giờ sau |
| Commit gây lỗi | Thường chính commit đỏ; có bản revert đúng (ADR-0025) | Một lần chạy gộp nhiều commit; không có commit gây lỗi, không có bản revert |
| Phiên tự chạy (chạy đêm) | Chờ `main-guard` sau mỗi merge nên thấy lỗi của chính task trước task sau, revert theo ADR-0025 mục 4 | Không dừng được; lỗi lộ ra sau phiên |
| `release.sh` (không sửa) | Đọc kết luận của cả workflow `main-guard.yml`: emulator đỏ chặn phát hành khi commit đó là commit đổi code mới nhất (mục 8, câu 8) | Không thấy kết quả: phát hành được commit có emulator đỏ |
| Phút chờ của phiên, đường làm nhanh | Thêm khoảng 5 phút `main-guard` cho commit code app | Không thêm |

`needs: guard` thay vì chạy song song: `guard` đỏ thì không tốn phút emulator; quyết định chạy hay không nằm trong `guard` (một bước `git diff`, không thêm máy như một job lọc riêng, xem T-076); thời gian thêm khoảng 1 phút vì `guard` ở cách `skip` xong trong khoảng 1 phút (ADR-0039).

**Vì sao đỏ thật là `main-broken` (đề xuất cho mục 8, câu 2).** Lỗi kiểu L-051 làm app dừng khi mở: bản staging và mọi PR sau đều đứng trên một app hỏng. `main-broken` có sẵn: commit gây lỗi, nhánh revert, ngoại lệ bản revert đúng của `merge-if-green.sh`, quy tắc phiên revert commit của chính phiên, tự đóng khi xanh. Nhãn nhẹ hơn cần thêm một quy tắc dừng cho phiên và một đường revert không có kiểm nội dung. Phần làm `main-broken` an toàn với emulator là lần chạy lại trong job, kết luận `infra` cho đúng các lỗi hạ tầng đã biết của emulator (danh sách trắng PB, thiết bị hỏng), và `find-breaking-commit.sh` không đổ lỗi cho commit đỏ vì hạ tầng.

**Script `scripts/ui-test-needed.sh <sha> [<sự kiện>]`.** In đúng một dòng `yes` hay `no` ra stdout, lý do ra stderr, luôn thoát 0. Lối của `main-guard-verified.sh`: bash, `set -euo pipefail`, chỉ `git`.

1. Sự kiện (mặc định `push`) khác `push` → `yes` ("chạy tay").
2. `git -c core.quotePath=false diff --no-renames --name-only <sha>^ <sha>` lỗi (commit không có, không có cha trong `fetch-depth: 50`) → `yes` ("không đọc được thay đổi").
3. Một dòng khớp `UI_PATHS` → `yes`, lý do nêu file đầu tiên khớp. Không dòng nào khớp → `no`.

`UI_PATHS` (hằng khai báo một lần ở đầu script, là bộ lọc `ui` hiện nay của `pr-check.yml`, đổi `.github/workflows/pr-check.yml` thành `main-guard.yml` và thêm các script của job): tiền tố thư mục `app/`, `core/`, `feature/`, `build-logic/`, `gradle/`; file ở gốc khớp `*.gradle.kts` (không có `/`); file đúng tên `gradle.properties`, `gradlew`, `scripts/test-summary.sh`, `scripts/ui-test-on-emulator.sh`, `scripts/ui-test-verdict.sh`, `scripts/ui-test-needed.sh`, `.github/workflows/main-guard.yml`. So khớp bằng `case` của bash.

**Script `scripts/ui-test-on-emulator.sh`.** Chạy bên trong `reactivecircus/android-emulator-runner@v2` (dòng `script:`), thay dòng `bash -c '...'` hiện có của `pr-check.yml`. Lệnh lấy từ biến `UI_TEST_ADB` (mặc định `adb`) và `UI_TEST_GRADLEW` (mặc định `./gradlew`), để test thay bằng bản giả. Một lần chạy: `adb logcat -c`, `adb logcat -v threadtime` chạy nền ghi vào tệp log, `./gradlew --no-daemon --console=plain :app:connectedStagingDebugAndroidTest 2>&1 | tee <log>`, lấy mã thoát, dừng logcat. Lần 1 ghi `ui-test.log`, `logcat.txt`; xanh thì thoát 0. Đỏ: đổi tên thành `ui-test-first.log`, `logcat-first.txt`, chép `app/build/outputs/androidTest-results/` (nếu có) sang `ui-test-first-results/`, in `Lần đầu đỏ, chạy lại một lần trên cùng emulator (ADR-0040).`, chạy lần 2 ghi `ui-test.log`, `logcat.txt`, thoát với mã của lần 2. `set -uo pipefail` (không `-e`: cần mã của lần 1).

**Script `scripts/ui-test-verdict.sh <kết quả bước emulator> [<thư mục>]`.** Kết quả là `steps.emulator.outcome` (`success`, `failure`, hay giá trị khác); thư mục mặc định `.`. In đúng một từ `green`, `flaky`, `infra` hay `red`, lý do ra stderr, luôn thoát 0. Log gộp của một lần chạy = `ui-test.log` (hay `ui-test-first.log`) + `ui-summary.log` + các dòng `VGL...` và `AndroidRuntime` của `logcat.txt` (hay `logcat-first.txt`), ghi vào `${TMPDIR:-/tmp}`, tra bằng `scripts/failure-kind.sh --playbook`. Log gộp của lần 1 không có `ui-summary.log` (kết quả XML và tóm tắt là của lần 2, lần 1 chỉ còn `ui-test-first.log` và `logcat-first.txt`).

Các hằng khai báo một lần ở đầu script (rà soát độc lập, điểm 2 và 5):

- `INFRA_PB=(PB-01 PB-11 PB-13 PB-15)`: danh sách trắng các dòng bảng là lỗi hạ tầng của emulator. Mã PB khác (kể cả loại `lich-thu-lai` như PB-09) không làm thành hạ tầng.
- `CRASH_RE='Process crashed|Instrumentation run failed|FATAL EXCEPTION'`: app dừng (kể cả trong `Application.onCreate`, khi 0 test chạy), tìm trong log Gradle và logcat.
- `DEVICE_RE='No connected devices|device offline|could not connect to TCP port'`: emulator hay thiết bị hỏng.
- `TEST_FAILED_RE=' > .+ FAILED$'`: một test thất bại trong log Gradle (dùng cho lần 1, không có tóm tắt).

Gọi N là số test của `ui-summary.log` (dòng `Test emulator: N test, M lỗi.`), không có dòng đó thì N = 0. "Khớp danh sách trắng" là dòng `PB-NN` mà `failure-kind.sh` in có mã trong `INFRA_PB`. "Lần 1 đỏ thật" là: có `ui-test-first.log`, log gộp lần 1 không khớp danh sách trắng, và có `CRASH_RE` hay `TEST_FAILED_RE`.

- Kết quả `success`:
  - N = 0 (không có tóm tắt hay 0 test) → `infra` (rà soát, điểm 3: bước tóm tắt có `|| true`, nên không test nào chạy mà vẫn xanh phải lộ ra);
  - không có `ui-test-first.log` → `green`;
  - lần 1 đỏ thật → `flaky`; lần 1 khớp danh sách trắng hay không có dấu hiệu nào → `green` (lý do nêu mã PB nếu có).
- Kết quả `failure`, xét theo thứ tự, dừng ở dòng đầu tiên đúng:
  1. không có `ui-test.log` → `infra` (emulator không khởi động, script không chạy: PB-15 thường rơi vào đây);
  2. lần 1 đỏ thật → `red` (rà soát, điểm 4: lần 2 đỏ vì hạ tầng không che lỗi thật của lần 1);
  3. log gộp lần 2 có `CRASH_RE` → `red` (rà soát, điểm 2: app dừng khi khởi động là lỗi thật dù 0 test chạy);
  4. N = 0: log gộp khớp danh sách trắng hay có `DEVICE_RE` → `infra`; không → `red`;
  5. N > 0: log gộp khớp danh sách trắng → `infra`, lý do nêu dòng `PB-NN <loại>`; không → `red`.
- Kết quả khác (`cancelled`, `skipped`, rỗng) → `infra`.

**`scripts/find-breaking-commit.sh`: thêm `--check <tên>`.** Cách dùng mới `scripts/find-breaking-commit.sh [--check guard|ui-test] <sha>`; mặc định `guard`, mọi hành vi cũ giữ nguyên. Tên khác hai tên trên → thoát 1 với thông báo cách dùng. Với `ui-test`:

- đọc `commits/<sha>/check-runs?check_name=ui-test&per_page=20`;
- kết luận `skipped` → bỏ qua commit đó, đi tiếp, coi như chưa biết (commit không đổi code app, hay `guard` đỏ nên `ui-test` không chạy: hai trường hợp cùng cách xử lý, vì không có kết quả emulator nào để dừng lại);
- kết luận `failure` → đọc annotations của check run đó (`check-runs/<id>/annotations`, như `wait-run.sh` đọc annotation của job đỏ); có annotation với `message` bắt đầu bằng `HẠ TẦNG:` (bước `check` của job in khi kết luận `infra`) → chưa biết, đi tiếp như `skipped`; không có, hay đọc lỗi → đỏ, commit thành nghi phạm như nay (rà soát độc lập, điểm 1: commit đỏ vì hạ tầng không bị đổ lỗi và bị revert);
- không có check run, commit chỉ đổi tài liệu → bỏ qua như nay;
- không có check run, commit có code: đọc `contents/.github/workflows/main-guard.yml?ref=<sha>` (dạng raw, như `merge-if-green.sh` đọc danh sách chạy); không có dòng `  ui-test:` → dừng, coi như xanh (commit trước T-075: emulator đã chạy ở PR); có dòng đó hay đọc lỗi → chờ như nay (`needs: guard` nên check run `ui-test` chỉ có sau khi `guard` xong);
- `success`, đang chạy, kết luận khác: như `guard`.

**`main-guard.yml`.**

- Chú thích đầu file: thêm gạch đầu dòng về job `ui-test` (ADR-0040).
- Job `guard`: thêm `outputs:` `ui: ${{ steps.ui.outputs.ui }}` (trước `steps:`); thêm bước `id: ui`, tên `Có cần test trên emulator (scripts/ui-test-needed.sh)`, sau bước `mode`, chạy `echo "ui=$(scripts/ui-test-needed.sh "${GITHUB_SHA}" "${GITHUB_EVENT_NAME}")" >> "$GITHUB_OUTPUT"`, không có `if:` (chạy cả ở cách `skip` và `tests`). Bước `Đánh dấu nhánh chính đang đỏ`: dòng tìm issue đang mở chỉ lấy issue không có `<!-- check:ui-test -->` (`--json number,body --jq '[.[] | select(.body | contains("<!-- check:ui-test -->") | not)][0].number // empty'`). Bước `Đóng issue main-broken ...`: bỏ qua issue có dòng đó (thêm `select(.body | contains("<!-- check:ui-test -->") | not)` trước `capture`). Điều kiện `if:` của mọi bước cũ giữ nguyên.
- Job mới `ui-test`, đặt sau `guard`: `needs: guard`, `if: needs.guard.outputs.ui != 'no'` (output rỗng, ví dụ bước `ui` không ghi được, thì vẫn chạy emulator: mặc định an toàn, rà soát độc lập điểm 6), `runs-on: ubuntu-latest`, `timeout-minutes: 60`. Các bước:
  1. `actions/checkout@v5` với `fetch-depth: 50`; `actions/setup-java@v5` (temurin 17); `gradle/actions/setup-gradle@v4`; `Giải phóng đĩa cho emulator`; `Bật KVM cho emulator` (chép nguyên từ `pr-check.yml`).
  2. `Test trên emulator`, `id: emulator`, `continue-on-error: true`, `uses: reactivecircus/android-emulator-runner@v2` với cùng `api-level`, `arch`, `target`, `disable-animations`, `emulator-options` như nay, `script: scripts/ui-test-on-emulator.sh`.
  3. `Tóm tắt test trên emulator`, `if: always()`, `scripts/test-summary.sh android 2>&1 | tee ui-summary.log` (thêm `|| true` để bước không đỏ: kết luận do bước 5).
  4. `Chi tiết test trên emulator` và `Lưu báo cáo test trên emulator` (artifact `ui-test-report`, thêm `ui-test-first.log`, `logcat-first.txt`, `ui-test-first-results/`), `if: steps.emulator.outcome == 'failure' || hashFiles('ui-test-first.log') != ''`.
  5. `Phân loại kết quả emulator (scripts/ui-test-verdict.sh)`, `id: verdict`, `if: always()`, `echo "verdict=$(scripts/ui-test-verdict.sh "${{ steps.emulator.outcome }}")" >> "$GITHUB_OUTPUT"`.
  6. `Kết quả test trên emulator`, `id: check`, `if: always()`: `green` và `flaky` thoát 0; `infra` in `::error::HẠ TẦNG: emulator đỏ không vì code (xem bước Phân loại); không mở main-broken. Chạy lại job đỏ theo docs/ops/failure-playbook.md (ADR-0040).` rồi thoát 1; còn lại thoát 1.
  7. `Ghi test chập chờn`, `if: steps.verdict.outputs.verdict == 'flaky'`, `continue-on-error: true`: chép bước của `guard`, đọc `ui-test-first.log`, tiêu đề issue `flaky: <test> (emulator)`.
  8. `Đánh dấu nhánh chính đang đỏ (emulator)`, `if: failure() && steps.verdict.outputs.verdict == 'red'`: chép bước của `guard`, khác ở: `culprit="$(scripts/find-breaking-commit.sh --check ui-test "${GITHUB_SHA}")"`; tìm issue đang mở chỉ lấy issue có `<!-- check:ui-test -->`; thân issue mới có thêm dòng `<!-- check:ui-test -->` ngay sau dòng `culprit`, câu đầu "Test trên emulator không qua", đoạn cuối log là 60 dòng cuối của `ui-test.log` và lỗi từ kết quả XML; tiêu đề `main-broken: <7 ký tự> (emulator)`; nhánh `revert/<7 ký tự>` như cũ.
  9. `Đóng issue main-broken của emulator khi xanh trở lại`, `if: success()`: chép bước đóng của `guard`, chỉ xét issue có `<!-- check:ui-test -->`.

**`pr-check.yml`.** Xóa job `ui-test`. Job `changes`: xóa bộ lọc `ui` và output `ui` (không còn ai đọc). Dòng chú thích thứ hai: `Job names (changes, build-and-test) are checked by scripts/merge-if-green.sh: rename in both places.`, thêm một dòng: emulator chạy sau merge ở job `ui-test` của `main-guard.yml` (ADR-0040). Job `build-and-test` không đổi một dòng nào.

**Trạng thái sau T-075, để T-076 và T-077 dựa vào.**

- `pr-check.yml`: hai job. `changes` (dorny/paths-filter, output `code`, `release`) và `build-and-test` (`needs: changes`, các bước giữ nguyên tên: `Test của script và tài liệu (scripts/tests/run.sh)`, `Cổng chất lượng (scripts/check.sh)`, `Build bản release (R8)`, `Lưu báo cáo test và ảnh screenshot`, `Bình luận lỗi vào PR`). Không còn `ui-test`, không còn bộ lọc `ui`.
- `merge-if-green.sh`: `REQUIRED_CHECKS=("changes" "build-and-test")`, không còn `SKIPPABLE_CHECKS`; vòng `for c in "${REQUIRED_CHECKS[@]}"` giữ. Test `pr_missing_jobs` của `run.sh` mong `names=2`.
- `main-guard.yml`: hai job. `guard` (bước `mode`, bước mới `ui`, output `ui`, phần còn lại như T-074) và `ui-test` (`needs: guard`).
- `main-guard-verified.sh`: `GATE_FILES` thêm `scripts/ui-test-needed.sh`; `GATE_JOB`, `GATE_STEP` không đổi.

**Chi tiết của `merge-if-green.sh` và script mới.** Hàm `check_one` bỏ tham số `allow_skip` và dòng cho qua `skipped` (không còn ai gọi với `yes`); `build-and-test` `skipped` vẫn bị từ chối như test có sẵn `merge-if-green: a skipped build-and-test is refused`. Ba script mới commit với quyền `100755` (`git update-index --chmod=+x` nếu tạo trên máy không giữ quyền); test 25 kiểm.

**Merge chính PR của T-075 (chuyển tiếp, rà soát độc lập điểm 7).** `merge-if-green.sh` trên nhánh chính lúc đó còn `SKIPPABLE_CHECKS=("ui-test")`, còn `pr-check` của PR (chạy bản `pr-check.yml` của nhánh) không có job `ui-test`: script của nhánh chính chờ mãi kiểm tra `ui-test` (mã 2). Vì vậy merge PR này bằng `scripts/merge-if-green.sh` của nhánh task (đang checkout nhánh `claude/T-075-...` khi gọi), như plan T-076 làm với `changes`. Đây vẫn là `merge-if-green.sh`, không phải cách merge khác (ADR-0010 mục 1). Ghi vào nhật ký. PR khác mở sau lúc merge dùng script mới của nhánh chính.

**Tài liệu.** Danh sách ở mục 3.2. Ý chính:

- `docs/auto-run.md`: mục 1 ý 6 (khoảng dòng 23) thêm "khoảng 5 phút `ui-test` của `main-guard` khi task đổi code app (ADR-0040)"; bảng các bước, dòng bước 8 (khoảng dòng 75) bỏ "thêm test trên emulator"; dòng khoảng 195 thành "Test trên emulator chạy sau merge, ở job `ui-test` của `main-guard` (ADR-0040)"; dòng khoảng 213 nói `ui-test` của `main-guard` và lần chạy `main-guard`; bước 9 (khoảng dòng 224 tới 227) thêm thời gian `ui-test` và hai gạch đầu dòng: (1) `main-guard` đỏ ở job `ui-test` mà không có issue `main-broken` là lỗi hạ tầng, tra bảng trên log của job, làm theo dòng bảng, không revert; (2) giới hạn (rà soát độc lập điểm 8): hai commit đổi code app liên tiếp trên nhánh chính có `ui-test` kết luận `infra` mà chưa chạy lại xanh thì phiên không merge thêm task nào đổi code app (`scripts/ui-test-needed.sh` của commit cuối task in `yes`), chỉ làm task không đổi code app, và ghi vào Cần người kiểm tra; một lần `ui-test` xanh sau đó gỡ giới hạn; mục 4, bảng điều kiện dừng, thêm dòng này; dòng khoảng 289 giữ (lần chờ dài nhất vẫn dưới 40 phút).
- `docs/ops/failure-playbook.md`: PB-01, PB-10, PB-11, PB-13, PB-15 giữ mã, mẫu, loại; cột Thấy ở thành `job ui-test của main-guard` (PB-11: `build-and-test` của `pr-check`, `guard` hay `ui-test` của `main-guard`); cột Việc phải làm: `main-guard` đã chạy lại một lần trong job và không mở `main-broken`; chạy lại riêng job đỏ (`gh api -X POST "repos/{owner}/{repo}/actions/runs/<id>/rerun-failed-jobs"`, `guard` không chạy lại) theo số lần cũ; lệnh bị từ chối hay hết số lần thì ghi vào báo cáo, mục Cần người kiểm tra, rồi làm tiếp task sau, không revert; không còn đẩy commit rỗng. PB-01 lần thứ 3 khi task đổi `core/assets`, `feature/loading`: kết thúc phiên và ghi cho người. PB-10: `main-guard` đã mở `main-broken`, làm theo bước 9. PB-13: người tải file hay xóa bộ đệm rồi chạy lại job.
- Các chỗ khác nhắc `ui-test` của PR: `CLAUDE.md` dòng 57; `docs/CLAUDE.md` dòng 93 (L-051); `docs/ops/pre-run-review.md` dòng 18; `docs/ops/setup-new-app.md` dòng 52, 141, 142; `docs/ops/test-sessions.md` bảng nguồn (thêm dòng `ui-test` của `main-guard` và một câu về hai lớp); `docs/ops/quick-lane.md` dòng 9; `scripts/run-check.py` dòng 45 và 350 ("chỉ lộ ra sau merge, ở job ui-test của main-guard (L-045, ADR-0040)"); `.github/workflows/device-session.yml` dòng 86 (chú thích); `docs/ops/release.md` mục 2.1 bước 2 (một câu về `ui-test`).
- ADR-0010 và ADR-0019: thêm một dòng ghi chú ở đầu, dẫn tới ADR-0040, như ghi chú ADR-0039 đã thêm. ADR-0039: không sửa (đã `accepted`); ADR-0040 ghi danh sách `GATE_FILES` thêm một file.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/ui-test-needed.sh` | Chọn `yes`/`no` cho job `ui-test` (mục 3.1) |
| Tạo | `scripts/ui-test-on-emulator.sh` | Chạy test emulator tối đa hai lần, giữ log lần đầu (mục 3.1) |
| Tạo | `scripts/ui-test-verdict.sh` | Kết luận `green`, `flaky`, `infra`, `red` (mục 3.1) |
| Sửa | `scripts/find-breaking-commit.sh` | Tham số `--check guard\|ui-test`; với `ui-test`, `failure` có annotation `HẠ TẦNG:` là chưa biết (mục 3.1) |
| Sửa | `scripts/merge-if-green.sh` | Bỏ `SKIPPABLE_CHECKS` và vòng đọc nó; chú thích dòng 26 tới 29 |
| Sửa | `scripts/main-guard-verified.sh` | `GATE_FILES` thêm `scripts/ui-test-needed.sh`; chú thích dòng 15 |
| Sửa | `scripts/run-check.py` | Chú thích dòng 45 và thông báo dòng 350 |
| Sửa | `.github/workflows/main-guard.yml` | Output `ui` và bước `ui` của `guard`; lọc `check:ui-test` ở hai bước `main-broken` của `guard`; job `ui-test`; chú thích đầu file |
| Sửa | `.github/workflows/pr-check.yml` | Xóa job `ui-test`, bộ lọc và output `ui`; chú thích đầu file |
| Sửa | `.github/workflows/device-session.yml` | Chú thích dòng 86 (`ui-test` của `main-guard.yml`) |
| Sửa | `scripts/tests/run.sh` | Test mục 4; dữ liệu `mgv_gate_files` thêm `scripts/ui-test-needed.sh`; `pr_missing_jobs` mong `names=2` (mục 3.5) |
| Sửa | `docs/adr/0040-test-emulator-sau-merge.md` | Có trong PR plan này (`proposed`); task chỉ sửa khi rà soát độc lập thấy ADR lệch với code đã làm, không đổi `status` |
| Sửa | `docs/adr/0010-soft-quality-gate-github-free.md` | Một dòng ghi chú ở đầu dẫn tới ADR-0040 |
| Sửa | `docs/adr/0019-app-logging-and-device-test-diagnostics.md` | Một dòng ghi chú ở đầu: chẩn đoán emulator nay ở job `ui-test` của `main-guard.yml` (ADR-0040) |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` khi trạng thái ADR-0040 đổi |
| Sửa | `docs/auto-run.md` | Mục 1 ý 6; bảng bước 8; dòng khoảng 195, 213; bước 9 |
| Sửa | `docs/ops/failure-playbook.md` | PB-01, PB-10, PB-11, PB-13, PB-15: cột Thấy ở, Việc phải làm |
| Sửa | `docs/ops/pre-run-review.md` | Dòng 18: lỗi vỏ app lộ ra sau merge |
| Sửa | `docs/ops/setup-new-app.md` | Dòng 52, 141, 142: `ui-test` của `main-guard` |
| Sửa | `docs/ops/test-sessions.md` | Bảng nguồn: dòng `ui-test` của `main-guard`, câu về hai lớp |
| Sửa | `docs/ops/quick-lane.md` | Dòng 9: thời gian `main-guard` thêm `ui-test` |
| Sửa | `docs/ops/release.md` | Mục 2.1 bước 2: `main-guard.yml` nay gồm job `ui-test`; PR tăng `versionCode` đổi `app/build.gradle.kts` nên emulator chạy trên chính commit phát hành; `ui-test` đỏ vì hạ tầng thì chạy lại job tới khi xanh rồi mới phát hành (rà soát độc lập điểm 8, 15) |
| Sửa | `docs/CLAUDE.md` | Dòng 93 (L-051): lỗi chỉ lộ ra sau merge |
| Sửa | `CLAUDE.md` | Dòng 57: test emulator chạy ở job `ui-test` của `main-guard` sau merge |
| Sửa | `docs/features/quy-trinh/plans/T-075-emulator-sau-merge.md` | Nhật ký thực hiện |
| Sửa | `docs/features/quy-trinh/tickets/T-075-test-emulator-sau-merge.md` | Trạng thái ticket |

Nơi đọc file và tên bị đổi (L-020), tìm bằng `grep -rn 'ui-test' scripts/ .github/ docs/ CLAUDE.md` và `grep -rn 'main-guard\|find-breaking-commit\|SKIPPABLE' scripts/ .github/ docs/ops/`:

- Đã có trong bảng trên: `scripts/merge-if-green.sh` dòng 29, `scripts/run-check.py` dòng 45, 350, `.github/workflows/pr-check.yml`, `.github/workflows/device-session.yml` dòng 86, `docs/auto-run.md` dòng 75, 195, 213, `docs/ops/failure-playbook.md` dòng 14, 15, 24, 25, 28, `docs/ops/pre-run-review.md` dòng 18, `docs/ops/setup-new-app.md` dòng 141, 142, `docs/CLAUDE.md` dòng 93, `docs/adr/0019-...` dòng 26.
- `scripts/tests/run.sh` dòng 553, 650 (dữ liệu `green_runs`, `mig_run` có check run `ui-test` `skipped`): giữ nguyên; sau task, check run không có trong danh sách bắt buộc bị bỏ qua, nên các test đó vẫn mong cùng kết quả và nay kiểm thêm "check run lạ `skipped` không chặn". Dòng 230 (`pr_missing_jobs` đọc `(REQUIRED|SKIPPABLE)_CHECKS`): regex giữ, mong `names=2` (mục 3.5). Dòng 764, 779 (tên test nhắc `ui-test` của bảng tra lỗi): mã và loại PB giữ nên test giữ. Dòng 936 tới 991 (`wait-run.sh` với job tên `ui-test`): dữ liệu của `wait-run.sh`, script đọc tên job bất kỳ; `wait-run.sh --workflow main-guard.yml` vẫn in tên job đỏ (`job: ui-test`), không sửa.
- `scripts/tests/playbook/PB-11.log`: log thật có tên job `ui-test`; mẫu không đọc tên job. Không sửa.
- `scripts/check.sh` dòng 6 ("they run in the ui-test job of CI"): vẫn đúng (job `ui-test` của CI, nay ở `main-guard`). Không sửa, để không chạm file của mục Không được làm khi không cần.
- `scripts/find-breaking-commit.sh` được gọi ở `main-guard.yml` (job `guard`, không tham số: hành vi mặc định giữ) và test `run.sh` dòng 32 tới 75 (giữ nguyên).
- `scripts/main-guard-status.sh`, `.github/workflows/start-run.yml`: đọc check run `guard` (`check_name=guard`), không đọc `ui-test`. Không sửa (ADR-0040 mục 5).
- `scripts/release.sh` dòng 187 tới 190: đọc kết luận workflow `main-guard.yml`. Không sửa (cấm); ADR-0040 mục 5 ghi nghĩa mới, người chấp nhận ở mục 8, câu 1.
- `scripts/wait-run.sh`: không đọc tên job cố định. Không sửa.
- `docs/lessons.md` dòng 140, 196, 200, 426, 660, 665, 774 tới 779; `docs/adr/0034-...` dòng 24, 87; `docs/features/ads/feedback/FB-20261025-01-...` dòng 37; `docs/features/quy-trinh/requests/...`, ticket T-075, T-076: ghi chuyện đã xảy ra hay lý do lúc quyết định, đúng tại thời điểm viết. Không sửa (tài liệu đã chốt, ADR `accepted`).
- `docs/ops/release.md` dòng 90, `docs/ops/staging-and-runs.md` dòng 113, 114, 167, `docs/ops/session-capabilities.md` dòng 21: nói `main-guard` xanh hay đỏ và emulator chỉ có trong CI; vẫn đúng. Không sửa.
- Các file khác chỉ nhắc tên hay đường dẫn của file plan sửa (chú thích, thông báo, liên kết, lời giao phiên, số mục), không đọc nội dung các dòng bị đổi; plan không đổi tên file, tên mục hay số mục nào:
  - nhắc `docs/auto-run.md`: `.github/workflows/resume-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/token-usage.py`;
  - nhắc `CLAUDE.md` hay `docs/CLAUDE.md`: `.github/ISSUE_TEMPLATE/from-app.yml`, `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt` (chú thích dẫn bước 10 của `CLAUDE.md`, plan không sửa bước 10), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt`, `scripts/device-session.sh`, `scripts/local-setup.sh`, `scripts/new-app.sh`, `scripts/run-watchdog.py`;
  - nhắc `docs/ops/setup-new-app.md`: `.github/workflows/env-canary.yml`, `scripts/java-check.sh`, `scripts/new-app.sh`, `scripts/setup-check.sh`;
  - nhắc `.github/workflows/device-session.yml` hay `docs/ops/test-sessions.md`: `scripts/device-session.sh` (plan chỉ sửa một dòng chú thích của workflow và thêm một dòng vào bảng nguồn); `scripts/tests/playbook/PB-14.log` (log mẫu);
  - nhắc `docs/ops/failure-playbook.md`: `scripts/push-probe.sh` (dẫn PB-14, plan không đổi PB-14); `scripts/failure-kind.sh` đọc bảng, mẫu và loại của các dòng plan sửa giữ nguyên;
  - nhắc `scripts/merge-if-green.sh`: `scripts/is-exact-revert.sh` (chú thích; plan không đổi phần `main-broken` của script);
  - nhắc `scripts/run-check.py`: `scripts/plan-queue.py` (gọi cùng quy tắc, không đọc câu thông báo);
  - nhắc `scripts/tests/run.sh`: `scripts/local-setup.sh` (chỉ gọi tệp);
  - nhắc `.github/workflows/pr-check.yml`: `scripts/tests/playbook/PB-02.log` (log mẫu; plan không đổi tên workflow, job `build-and-test` hay `changes`).
- App nhận thay đổi qua `scripts/template-sync.sh` (ADR-0030): app đã sửa job `ui-test` của `pr-check.yml` (camera-pose dùng `-camera-back emulated` ở `device-session`, chưa thấy ở `ui-test`) phải chép phần sửa sang job `ui-test` của `main-guard.yml`; ghi ở mục 6.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm (test emulator tải file R2 như nay; lỗi tải là PB-01, PB-13)
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong `main-guard.yml` giữ nguyên (`contents: write` để đẩy nhánh revert, `issues: write`, `checks: read`, `actions: read`, `pull-requests: read`); job `ui-test` dùng quyền cấp workflow như `guard`. `find-breaking-commit.sh --check ui-test` đọc `contents/...` (có `contents`) và `check-runs` (có `checks: read`).
- **Thư viện và SDK:** Không thêm. Action có sẵn: `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`, `reactivecircus/android-emulator-runner@v2`, `actions/upload-artifact@v4`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** lần chạy thứ hai của `:app:connectedStagingDebugAndroidTest` trên cùng emulator, ngay sau lần đầu đỏ (Gradle cài lại app và gói test; lần đầu đỏ giữa chừng có thể để app còn cài hay emulator ở màn hình khác). Chưa từng chạy trong dự án. Kiểm ở dòng kiểm tra thủ công của mục 4 (emulator của máy dev) trước khi mở PR.
- **Điều kiện chuyển:** lần chạy thử đó cho thấy lần 2 đỏ vì cài đặt hay kết nối (`INSTALL_FAILED`, `device offline`, `No connected devices`), không vì test.
- **Cách làm thay:** trước lần 2, `ui-test-on-emulator.sh` gọi `adb uninstall` cho gói app và gói test (tên gói đọc từ `app/build/intermediates/` hay khai báo `applicationId` của `app/build.gradle.kts`, ghi trong chú thích của script), rồi `adb wait-for-device` và chạy lần 2. Test 8 và 9 của mục 4 giữ nguyên câu khẳng định (đếm lần gọi `gradlew`, tên tệp log); thêm một câu đếm lệnh `uninstall` mà `adb` giả ghi. Không đổi lần chạy lại thành bước emulator thứ hai.

### 3.5. Hành vi đã có bị đổi

Không đổi code Kotlin. Hành vi của script và workflow mà test có sẵn của `scripts/tests/run.sh` khẳng định:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `merge-if-green.sh` có hai danh sách, ba tên kiểm tra, mỗi tên là một job của `pr-check.yml` | Một danh sách, hai tên (`SKIPPABLE_CHECKS` bỏ) | `pr-check: every check merge-if-green.sh requires or may skip is a job of pr-check.yml` mong `names=3 missing= none`; `pr-check: the job name check names a renamed job` mong `names=3 missing= build-and-test` | Ý định: mỗi tên kiểm tra mà script đòi là một job của `pr-check.yml`, và đổi tên job thì test báo. Câu mới: `names=2 missing= none` và `names=2 missing= build-and-test`; đối chứng giữ (bản đổi tên vẫn thất bại). Viết lại theo ADR-0031 (plan nói rõ ở mục 3.1) |
| Test `main-guard-verified` lặp qua tám file `GATE_FILES` | Chín file | `main-guard-verified: a commit changing a file the guard job runs is full`, mảng `mgv_gate_files` | Dữ liệu: thêm `scripts/ui-test-needed.sh` vào mảng (ADR-0029); câu khẳng định (`${#mgv_gate_files[@]}` lần `full`) giữ. Vào commit test của bước 3, cùng các test mới của mục 4 |
| `find-breaking-commit.sh <sha>` | Giữ; thêm `--check` | Các test `find_breaking` dòng 32 tới 75 | Giữ nguyên |
| Job `guard` và các bước của nó (T-074, test 13) | Thêm `outputs`, bước `ui`, lọc `check:ui-test` ở hai bước `main-broken` | `main-guard: chooses the mode before the gate and skips inside the guard job` và bản sao hỏng | Giữ nguyên: các câu (a) tới (j) không đọc dòng tìm issue; job `ui-test` đặt sau `guard` nên `mg_steps` dừng ở `  ui-test:` |
| Trigger, `paths-ignore`, `if:` cấp job của `guard` | Không đổi | `main-guard: has workflow_dispatch and keeps the push trigger with the same paths-ignore` | Giữ nguyên |
| `pr-check.yml` job `build-and-test` | Không đổi | Các test `pr-check:` dòng 237 tới 253 | Giữ nguyên |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`, `gh` giả là `scripts/tests/fake-gh` với `FIXTURE` (`routes` được tra trước đường `check-runs` theo mã commit, nên test đặt đúng đường `commits/<sha>/check-runs?check_name=ui-test&per_page=20` để chứng minh script hỏi đúng tên). Phần git chạy trong repo tạm `"$work/uin"` như khối `mgv` (bỏ `GIT_CONFIG_*` ở đầu khối, đặt lại ở cuối, L-082). Hàm phụ trợ vào commit test của bước 3 (L-025): `uin <sự kiện> <file...>` (tạo commit đổi các file đó, in `<mã thoát> <stdout>`), `uoe <kịch bản gradlew>` (chạy `ui-test-on-emulator.sh` trong thư mục tạm với `UI_TEST_ADB`, `UI_TEST_GRADLEW` là bản giả: `adb` giả ghi tham số vào `adb.calls`, `gradlew` giả đọc dòng kế tiếp của kịch bản `pass`/`fail`, in một dòng có số lần gọi, thoát theo dòng đó), `uv <kết quả> <thư mục>` (chạy `ui-test-verdict.sh`, in `<mã thoát> <stdout>`), `ui_steps <workflow>` (như `mg_steps`, cho job `ui-test`). Log mẫu của `uv` lấy từ `scripts/tests/playbook/PB-*.log` có sẵn cộng một `ui-summary.log` viết trong test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| DoD 2: commit code app chạy emulator | Test của script | 1. `ui-test-needed: a commit changing app code is yes`: lặp qua `app/x.kt`, `core/c/x.kt`, `feature/f/x.kt`, `build-logic/x.kts`, `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew`, `scripts/test-summary.sh`, `scripts/ui-test-on-emulator.sh`, `scripts/ui-test-verdict.sh`, `scripts/ui-test-needed.sh`, `.github/workflows/main-guard.yml`; mong mỗi lần `0 yes` |
| DoD 2: commit không đổi code app không chạy | Test của script | 2. `ui-test-needed: a commit without app code is no`: `scripts/status.py`, `docs/a.md`, `README.md`, `.github/workflows/pr-check.yml`, `featurex/a.kt`, `appendix.txt`, `docs/app/x.kt`; mong mỗi lần `0 no`. Đối chứng cùng test: commit đổi `scripts/status.py` và `core/c/x.kt` → `0 yes` (L-055) |
| Chạy tay luôn chạy | Test của script | 3. `ui-test-needed: workflow_dispatch is yes`: commit chỉ `docs/a.md`, sự kiện `workflow_dispatch` → `0 yes`; đối chứng sự kiện `push` → `0 no` |
| Không chắc thì chạy | Test của script | 4. `ui-test-needed: an unreadable commit is yes, exit 0`: sha không có trong repo → `0 yes`, stderr có `không đọc được`; commit gốc (không có cha) → `0 yes` |
| Đổi tên file không lọt | Test của script | 5. `ui-test-needed: code moved into docs/ is yes`: `git mv app/x.kt docs/x.kt` → `0 yes` |
| Mọi module nằm trong `UI_PATHS` | Test cấu trúc | 6. `ui-test-needed: every Gradle module is under UI_PATHS`: mỗi `include(":a:b")` của `settings.gradle.kts` đổi thành thư mục `a/b/`, gọi script trên commit đổi `a/b/x.kt`; mong mọi module `yes`. Bản sao hỏng của script bỏ `core/`: mong ít nhất một module `no` |
| Chạy lại một lần trên cùng emulator | Test của script | 7. `ui-test-on-emulator: a green first run runs once`: kịch bản `pass` → mã 0, `gradlew` gọi 1 lần, có `ui-test.log`, không có `ui-test-first.log`; `adb.calls` có `logcat -c` 1 lần |
| | Test của script | 8. `ui-test-on-emulator: a red first run runs once more and keeps its log`: kịch bản `fail`, `pass`; thư mục tạm có sẵn `app/build/outputs/androidTest-results/a.xml` → mã 0, `gradlew` gọi 2 lần, `ui-test-first.log` chứa dòng của lần 1, `ui-test.log` chứa dòng của lần 2, `ui-test-first-results/a.xml` có, `logcat -c` 2 lần |
| | Test của script | 9. `ui-test-on-emulator: two red runs fail`: `fail`, `fail` → mã khác 0, `gradlew` gọi 2 lần (không lần 3) |
| Kết luận `green`, `flaky` | Test của script | 10. `ui-test-verdict: a green run is green`: `success`, không có `ui-test-first.log`, summary `3 test, 0 lỗi` → `0 green`. Đối chứng (rà soát, điểm 3): `success` mà không có `ui-summary.log`, và `success` với `Test emulator: 0 test, 0 lỗi.` → `0 infra` cả hai. 11. `ui-test-verdict: red then green is flaky`: `success`, `ui-test-first.log` có `> Task :app:connectedStagingDebugAndroidTest FAILED` và `AppLaunchTest > launches FAILED` (không khớp dòng hạ tầng) → `0 flaky`; đối chứng: lần đầu là log PB-01 (`logcat-first.txt` có dòng `VGL.Loading: Task HELLO failed`) → `0 green`. Lần 1 không có `ui-summary.log` riêng: test dựng `ui-summary.log` của lần 2 (`3 test, 0 lỗi`) để chứng minh script không đọc nó cho lần 1 |
| Đỏ vì hạ tầng không mở `main-broken` | Test của script | 12. `ui-test-verdict: infrastructure is infra`: `failure` với (a) không có `ui-test.log`; (b) summary `0 test, 0 lỗi` và `ui-test.log` có `No connected devices`; (c) summary `0 test` và log PB-15 cộng `Không có test nào chạy trên emulator`; (d) log có dòng của PB-01 trong `logcat.txt`, summary `3 test, 1 lỗi`; (e) PB-11 (`scripts/tests/playbook/PB-11.log`, 0 test); (f) PB-13, summary `3 test, 1 lỗi`. Mong `0 infra` cả sáu, stderr của (c) tới (f) có đúng mã PB |
| Đỏ thật | Test của script | 13. `ui-test-verdict: a real test failure is red`: `failure`, log có `AppLaunchTest > launches FAILED`, summary `3 test, 1 lỗi`, `logcat.txt` không có dòng hạ tầng → `0 red`; log PB-10 (`ComposeTimeoutException`), không có dòng PB-01 → `0 red`. Đối chứng L-055: cùng log PB-10 cộng một dòng PB-01 trong `logcat.txt` → `0 infra` (PB-01 đứng trước PB-10 trong bảng) |
| App dừng khi khởi động là đỏ thật (rà soát, điểm 2) | Test của script | 13b. `ui-test-verdict: an app that crashes at start is red`: `failure`, summary `0 test, 0 lỗi`, (a) `ui-test.log` có `Process crashed`; (b) `ui-test.log` có `Instrumentation run failed`; (c) `logcat.txt` có `FATAL EXCEPTION: main` và `AndroidRuntime`. Mong `0 red` cả ba. Đối chứng: summary `0 test`, không có dấu hiệu dừng app, log PB-15 → `0 infra` (12c); summary `0 test`, không có dấu hiệu nào → `0 red` |
| Danh sách trắng PB (rà soát, điểm 5) | Test của script | 13c. `ui-test-verdict: only the emulator infrastructure rows are infra`: `failure`, summary `3 test, 1 lỗi`, `logcat.txt` có dòng `AndroidRuntime` chứa `java.net.SocketTimeoutException: Read timed out` (khớp PB-09, loại `lich-thu-lai`, không thuộc `INFRA_PB`) → `0 red`. Kiểm thêm: `INFRA_PB` của script đúng bằng `PB-01 PB-11 PB-13 PB-15` và mỗi mã có trong `docs/ops/failure-playbook.md` |
| Lỗi thật của lần 1 không bị che (rà soát, điểm 4) | Test của script | 13d. `ui-test-verdict: a real first failure stays red when the rerun fails on infrastructure`: `failure`, `ui-test-first.log` có `AppLaunchTest > launches FAILED` (không khớp dòng hạ tầng), lần 2 là log PB-01 với summary `3 test, 1 lỗi` → `0 red`. Đối chứng: lần 1 cũng là log PB-01 → `0 infra` |
| Kết quả lạ | Test của script | 14. `ui-test-verdict: an unknown outcome is infra`: `cancelled`, `skipped`, rỗng → `0 infra` |
| Tìm commit gây lỗi của emulator | Test của script | 15. `find-breaking-commit --check ui-test: blames the oldest red ui-test after the newest green`: D đỏ, C `skipped`, B đỏ, A xanh (đều qua `routes` `check_name=ui-test`) → `bbbbbb2`. Đối chứng: không `--check`, cùng dữ liệu `states` của `guard` khác → kết quả theo `guard` |
| Commit đỏ vì hạ tầng không bị đổ lỗi (rà soát, điểm 1) | Test của script | 15b. `find-breaking-commit --check ui-test: an infrastructure failure is not blamed`: D `failure` (check run id 41, annotations rỗng), C `failure` (id 31, `check-runs/31/annotations` trả `[{"message": "HẠ TẦNG: emulator đỏ không vì code ..."}]`), B `success` → `ddddd04`. Đối chứng: `check-runs/31/annotations` trả `[]` → `ccccc03`; annotations của C đọc lỗi (`errors`) → `ccccc03` |
| | Test của script | 16. `find-breaking-commit --check ui-test: stops at a commit whose main-guard.yml has no ui-test job`: C không có check run, có code, `contents/.github/workflows/main-guard.yml?ref=ccccc03` không có `  ui-test:` → `ddddd04`, chạy xong ngay (không có dòng `CẢNH BÁO: chờ quá lâu`). Đối chứng: nội dung có `  ui-test:` → in `CẢNH BÁO: chờ quá lâu` (chờ với `MAX_WAIT_SECONDS=1` và `POLL_SECONDS=0`, như hàm `find_breaking` có sẵn) |
| | Test của script | 17. `find-breaking-commit: --check accepts only guard and ui-test`: `--check foo ddddd04` → mã 1; `--check guard ddddd04` cho cùng kết quả như không có `--check` |
| Job `ui-test` nối đúng | Test cấu trúc | 18. `main-guard: ui-test runs after guard only for app code`. Câu: (a) có job `  ui-test:` sau `  guard:`, có `needs: guard` và đúng một `if:` cấp job `needs.guard.outputs.ui != 'no'`; (b) `guard` có `outputs:` với `ui: ${{ steps.ui.outputs.ui }}` và bước `id: ui` gọi `scripts/ui-test-needed.sh "${GITHUB_SHA}" "${GITHUB_EVENT_NAME}"` sau `actions/checkout`, bước đó không có `if:` (chạy cả ở cách `skip`); (c) bước `id: emulator` có `continue-on-error: true`, `android-emulator-runner` và `script: scripts/ui-test-on-emulator.sh`; (d) bước `id: verdict` có `if: always()` và gọi `scripts/ui-test-verdict.sh "${{ steps.emulator.outcome }}"`; (e) bước `id: check` thoát 0 chỉ cho `green` và `flaky`; (f) `Ghi test chập chờn` có `if: steps.verdict.outputs.verdict == 'flaky'`; (g) `Đánh dấu nhánh chính đang đỏ (emulator)` có `if: failure() && steps.verdict.outputs.verdict == 'red'`, gọi `scripts/find-breaking-commit.sh --check ui-test`, ghi `<!-- check:ui-test -->`, và dòng tìm issue đang mở để bình luận chỉ chọn issue chứa `<!-- check:ui-test -->` (`select(.body | contains("<!-- check:ui-test -->"))`); (h) bước đóng của `ui-test` có `if: success()` và chỉ chọn issue chứa `<!-- check:ui-test -->`; (i) `timeout-minutes` có. Mỗi câu chạy cả trên một bản sao hỏng trong `$work` (bỏ `needs`; `if:` cấp job là `needs.guard.outputs.ui == 'yes'`; bước `ui` có `if: steps.mode.outputs.mode != 'skip'`; đổi `if` của verdict thành `success()`; bỏ `continue-on-error`; mark step không có `--check`; mark step tìm issue không lọc; đóng không lọc); mong mỗi bản thất bại đúng câu |
| `guard` không đụng issue của emulator | Test cấu trúc | 19. `main-guard: guard comments on and closes only its own main-broken issues`: bước `Đánh dấu nhánh chính đang đỏ` của `guard` và bước `Đóng issue main-broken khi nhánh chính xanh trở lại` đều có `contains("<!-- check:ui-test -->") | not`; bản sao bỏ lọc ở mỗi bước: thất bại |
| DoD 1: PR không chạy emulator | Test cấu trúc | 20. `pr-check: has no emulator job`: `pr-check.yml` không có dòng `  ui-test:`, `android-emulator-runner`, `ui: ${{` hay bộ lọc `ui:`; bản sao trong `$work` có thêm ở cuối một job `  ui-test:` với bước `uses: reactivecircus/android-emulator-runner@v2` (thêm bằng `printf`, không lấy từ git): thất bại |
| DoD 1: `merge-if-green.sh` không đòi `ui-test` | Test của script | 21. `merge-if-green: a code PR without ui-test is ready`: `code_runs` = `changes`, `build-and-test` xanh, không có `ui-test` → `0` (`--dry-run`); trên bash 3.2 của macOS và bash của CI (bắt lỗi mảng rỗng với `set -u`). 22. `merge-if-green: a running ui-test is not waited for`: thêm `ui-test` `in_progress` → `0`. 23. `merge-if-green: a red ui-test left from before is still refused`: thêm `ui-test` `failure` → `1`, thông báo `có kiểm tra không qua: ui-test.` |
| `main-broken` của emulator vẫn có bản revert đúng | Test của script | 24. `merge-if-green: an exact revert passes a main-broken issue of ui-test`: `merge_while_broken` với thân issue `<!-- culprit:<sha> -->\n<!-- check:ui-test -->\n...` và `revert-yes` → `3` (qua kiểm tra `main-broken`, dừng ở "chậm" của dữ liệu); đối chứng `revert-no` → `1` |
| Script mới chạy được (rà soát, ghi chú 13) | Test cấu trúc | 25. `ui-test scripts are executable in git`: `git ls-files -s` cho ba script mới (`scripts/ui-test-needed.sh`, `scripts/ui-test-on-emulator.sh`, `scripts/ui-test-verdict.sh`) in `100755` cả ba |
| Mọi kiểm tra hiện có (L-058) | Bảng dưới | Bảng L-058 |
| Lần chạy thứ hai trên cùng emulator (mục 3.4) | Kiểm tra thủ công (người: CI của PR không chạy `main-guard`, cần emulator của máy dev) | Trên máy dev có emulator API 34 đang chạy, ở nhánh task: tạm thêm một test emulator luôn thất bại (không commit), chạy `scripts/ui-test-on-emulator.sh`; mong hai lần gọi Gradle, lần 2 đỏ vì test (không vì cài đặt), có `ui-test-first.log`. Bỏ test tạm, chạy lại: mong một lần, xanh. Chạy `scripts/test-summary.sh android` rồi `scripts/ui-test-verdict.sh success`: `green`. Ghi lệnh và kết quả vào nhật ký |
| DoD 2 sau khi merge | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Sau khi merge: lần `main-guard` của commit merge (đổi `main-guard.yml`: `guard` chạy `full`, `ui` là `yes`) có job `ui-test` xanh, log bước `Phân loại` in `green`. Rồi lần merge PR đầu tiên chỉ đổi script: `ui-test` `skipped`, workflow `success`, `scripts/main-guard-status.sh` in `XANH`. Lần đầu có `ui-test` đỏ và được chạy lại riêng (`rerun-failed-jobs`): xem job chạy lại vẫn nhận output `ui` của `guard`, không bị bỏ qua (rà soát, ghi chú 14). Ghi vào nhật ký |

**Bảng L-058: trường hợp mới qua từng kiểm tra hiện có.** Trường hợp mới: (P) PR code app không có check run `ui-test`; (G) commit trên main, `ui-test` `green`/`flaky`; (I) `ui-test` `infra`; (R) `ui-test` `red`; (S) `ui-test` `skipped`.

| Kiểm tra hiện có | Trường hợp mới | Test |
| --- | --- | --- |
| `merge-if-green.sh`: PR mở, chưa ở main, không nháp, không `needs-human`, nhắm `main` | (P) qua như PR có `ui-test`: các kiểm tra này đọc PR, không đọc check run | Test có sẵn (`merge_during_run`, `mig_run` với `MIG_DRAFT`, `MIG_LABELS`, `MIG_BASE`) giữ nguyên; dữ liệu `ui-test` `skipped` trong đó trở thành check run không bắt buộc |
| `merge-if-green.sh`: `main-broken` đang mở | (P) bị chặn như mọi PR; (R) mở `main-broken` có dòng `check:ui-test`, bản revert đúng vẫn qua | Test có sẵn `merge-if-green refuses with two main-broken issues`, `... an issue without culprit`; test 24 |
| `merge-if-green.sh`: phiên đang chạy | (P) như mọi PR | Test có sẵn `merge-if-green refuses a person's PR while a run is running`, `lets the session's task PR through` |
| `merge-if-green.sh`: nhánh chậm (`behind_by`) | (P) như mọi PR, mã 3 | Test có sẵn `merge-if-green: without --update a branch behind main exits 3` |
| `merge-if-green.sh`: check run đỏ bất kỳ | (P) không có `ui-test` thì không có gì đỏ; `ui-test` đỏ còn sót vẫn chặn | Test 21, 23 |
| `merge-if-green.sh`: kiểm tra bắt buộc chưa xong | (P) chỉ chờ `changes`, `build-and-test`; `ui-test` đang chạy không chờ | Test có sẵn `a job of a run still going waits` (giữ); test 22 |
| `merge-if-green.sh`: kiểm tra được phép bỏ qua | Bỏ cả kiểm tra (danh sách rỗng) | Test 21 (chạy hết `evaluate` không lỗi mảng rỗng); test 3.5 `names=2` |
| `main-guard`: trigger, `paths-ignore`, `if:` chỉ trên `main` | Không đổi; `ui-test` cần `guard` nên chạy tay trên nhánh khác thì `guard` và `ui-test` cùng bị bỏ qua | Test có sẵn (giữ); test 18 (a) |
| `main-guard`: chọn cách kiểm (ADR-0039) | Không đổi; `GATE_FILES` thêm `scripts/ui-test-needed.sh` | Test có sẵn `mgv` (giữ) với dữ liệu thêm một file (mục 3.5) |
| `main-guard`: chạy lại một lần, ghi `flaky` (ADR-0025 mục 1) | (G) `flaky` ghi issue `flaky (emulator)`; `guard` không đổi | Test 8, 11; test 18 (f) |
| `main-guard`: mở `main-broken`, tìm commit gây lỗi, đẩy nhánh revert | (R) mở issue riêng của emulator; (I) không mở; `guard` bỏ qua issue của emulator khi tìm issue để bình luận | Test 12, 13, 13b, 13c, 13d, 15, 15b, 16; test 18 (g); test 19 |
| `main-guard`: tự đóng `main-broken` (ADR-0025 mục 2) | `ui-test` xanh chỉ đóng issue của emulator; `guard` xanh không đóng issue đó | Test 18 (h), test 19 |
| `find-breaking-commit.sh` (job `guard`) | Không đổi khi không có `--check` | Test có sẵn dòng 32 tới 75 (giữ); test 17 |
| `main-guard-status.sh`, `start-run.yml` | (G), (I), (R), (S): đọc `guard`, không đổi; (R) bị chặn qua issue `main-broken` mà `start-run.yml` và đầu phiên kiểm | Test có sẵn `main-guard-status: ...` và `start-run: main-guard step runs after the run-file checks ...` (giữ) |
| `wait-run.sh --workflow main-guard.yml` | Chờ cả hai job; (I), (R) in `ĐỎ ... job: ui-test` | Test có sẵn `wait-run: the red line names ui-test` (giữ) |
| `release.sh` chỉ build commit có `main-guard.yml` `success` | Script chỉ đọc commit đổi code (ngoài `docs/`, `*.md`) mới nhất. Commit đó là (G), (S): mở cổng; (I), (R): chặn tới khi chạy lại xanh. Commit code app cũ hơn có (I) mà commit mới nhất chỉ đổi script ((S)): không chặn; bù bằng giới hạn hai lần `infra` của bước 9 và câu mới ở `docs/ops/release.md` (PR tăng `versionCode` đổi code app nên luôn chạy emulator) | Không sửa, không test mới (cấm chạm); người chấp nhận ở mục 8, câu 1 |

**Ngoại lệ viết test trước:** sửa tài liệu (`docs/auto-run.md`, `docs/ops/*.md` trong mục 3.2, `docs/CLAUDE.md`, `CLAUDE.md`, ghi chú ADR-0010 và ADR-0019, `foundation.md`), chú thích của `pr-check.yml`, `main-guard.yml`, `device-session.yml`, `merge-if-green.sh`, `main-guard-verified.sh`, và thông báo của `run-check.py` (test có sẵn chỉ đếm `L-045`, câu chữ không có hành vi): không có hành vi để kiểm. Các bước chép nguyên từ `pr-check.yml` sang job `ui-test` (cài Java, Gradle, giải phóng đĩa, KVM, chi tiết, lưu artifact) đã chạy ở `pr-check`; test 18 kiểm phần nối.

### 4.1. Chạy thử

Không cần (E dưới 2): `android-emulator-runner`, job có `needs` và `outputs`, `if:` cấp job đọc output của job trước (đều có ở `pr-check.yml`, job `ui-test` cũ), đọc `contents/...?ref=` dạng raw (`merge-if-green.sh`), `failure-kind.sh --playbook`, dòng đánh dấu trong thân issue (`culprit`) và `gh` giả, repo git tạm (khối `mgv` của T-074) đều đã dùng. Phần mới là chạy Gradle hai lần trên cùng emulator: kiểm ở dòng kiểm tra thủ công của mục 4 trước khi mở PR task, có phương án dự phòng ở mục 3.4. Job `ui-test` thật chỉ chạy được sau khi merge (`main-guard` chỉ chạy trên `main`); dòng kiểm tra thủ công cuối của mục 4 và điều kiện dừng ở mục 7 lo phần đó.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml`, `scripts/merge-if-green.sh`, `scripts/find-breaking-commit.sh`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba script mới, ba script sửa, ba workflow, test, mười hai tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 2 | Ticket để người chọn hai điều ảnh hưởng tới cách làm (nơi chạy, cách xử lý đỏ); plan đề xuất, mục 8 |
| C. Khả năng kiểm chứng tự động | 2 | Quyết định có test với `adb`, `gradlew`, `gh` giả và repo tạm; lần chạy thật của job `ui-test` và hai lần Gradle trên cùng emulator chỉ kiểm bằng tay (máy dev, sau merge) |
| D. Ảnh hưởng chéo | 3 | Đổi điều mọi PR phải qua trước merge, cách `main-broken` mở và đóng, điều `release.sh` coi là xanh, thời gian chờ của phiên; T-076, T-077 sửa cùng hai file |
| E. Độ mới của kỹ thuật | 1 | Biến thể của các mẫu đã có (mục 4.1); chạy Gradle hai lần trên một emulator là điểm duy nhất chưa làm, có dự phòng |
| **Tổng** | 11 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `main-guard.yml` (mọi commit code của main), `pr-check.yml` (mọi PR), `merge-if-green.sh` (mọi lần merge), `find-breaking-commit.sh`, `docs/auto-run.md` mục 1 và bước 8, 9, `docs/ops/failure-playbook.md`.
- T-076 (bỏ job `changes`) sửa `pr-check.yml` và `REQUIRED_CHECKS` của `merge-if-green.sh`: dựa trên trạng thái ở mục 3.1 ("Trạng thái sau T-075"); `pr_missing_jobs` khi đó mong `names=1`. T-076 không cần đọc bộ lọc `ui` (đã bỏ).
- T-077 sửa `pr-check.yml` (`types`, bỏ qua PR nháp) và `merge-if-green.sh` (không cập nhật nhánh khi main chỉ đổi tài liệu): không đụng phần T-075 đổi. Với T-077, commit merge khác cây PR ở tài liệu: `ui-test-needed.sh` so commit với commit cha, không so với PR, nên vẫn đúng.
- T-078 (runner tự host) có thể đổi `runs-on` của job `ui-test` mới; emulator cần KVM trên máy tự host.
- T-074 đã merge; plan giữ `GATE_JOB`, `GATE_STEP` (không đổi `build-and-test`), thêm một file vào `GATE_FILES`.
- Các plan ads đang mở (T-055, T-059, T-062, T-068): không chạm các file này, nhưng PR code app của chúng sẽ không chạy emulator ở PR sau khi T-075 merge; lỗi kiểu L-051 lộ ra ở `main-guard`.
- Các app nhận qua `scripts/template-sync.sh`: app đã sửa job `ui-test` của `pr-check.yml` (emulator, `script`) phải chép phần đó sang job `ui-test` của `main-guard.yml`; app có nhiều test emulator thì `main-guard` lâu hơn, cần xem `timeout-minutes: 60`.
- Merge chính PR của T-075: bằng `scripts/merge-if-green.sh` của nhánh task, vì script trên nhánh chính còn chờ `ui-test` (mục 3.1, "Merge chính PR của T-075"). T-076 cũng merge như vậy với `changes`.
- Đề xuất thứ tự: làm trong phiên có người, sau T-074 (đã xong), trước T-076 và T-077.

## 7. Điều kiện dừng riêng

- Cần thêm bí mật, token hay quyền mới cho `main-guard.yml`: dừng.
- Không giữ được check run `guard` có kết luận như nay (ví dụ phải đổi `if:` cấp job của `guard`): dừng, vì `find-breaking-commit.sh`, `main-guard-status.sh` và `release.sh` sẽ hiểu sai.
- Phải sửa `scripts/release.sh` hay `scripts/check.sh` để mọi thứ chạy: dừng.
- Quy tắc cho phiên tự chạy sau task này (ghi vào `docs/auto-run.md` bước 9 và mục 4): hai commit đổi code app liên tiếp có `ui-test` kết luận `infra` mà chưa chạy lại xanh thì không merge thêm task đổi code app trong phiên đó (rà soát độc lập điểm 8).
- Sau khi merge, lần `main-guard` đầu tiên đỏ ở job `ui-test` vì lỗi của chính task (bước `verdict`, `check`, YAML), hay `guard` đỏ ở bước `ui`: revert cùng dev. `ui-test` đỏ vì hạ tầng (kết luận `infra` đúng) thì không phải lỗi của task: chạy lại job.

## 8. Câu hỏi cho người duyệt

1. **Nơi chạy emulator: job `ui-test` trong `main-guard`, sau `guard` (đề xuất), hay workflow riêng mỗi đêm.** Đề xuất trong `main-guard` vì: emulator đỏ chặn phát hành qua `release.sh` mà không sửa script đó (khi commit đỏ là commit đổi code mới nhất, câu 8); phiên tự chạy thấy lỗi của chính task trước task sau và revert được theo ADR-0025; commit gây lỗi rõ. Đổi lại tốn khoảng 375 phút mỗi tuần ở dự án mẫu thay vì khoảng 35 nếu chạy mỗi đêm (bảng ở mục 3.1). Chọn mỗi đêm thì plan phải viết lại: workflow mới có `schedule`, không có commit gây lỗi, `release.sh` không thấy kết quả (cần người kiểm tay trước phát hành), phiên đêm không dừng được. Đồng ý đề xuất thì khi duyệt plan, ghi ADR-0040 `status: accepted` và chạy `scripts/foundation.py`, commit cùng lần duyệt.
2. **Emulator đỏ thật: `main-broken` như `guard` (đề xuất), hay nhãn nhẹ hơn (ví dụ `ui-broken`) không chặn merge.** Đề xuất `main-broken`: lỗi kiểu L-051 làm app dừng khi mở, mọi PR sau đứng trên app hỏng; dùng lại đúng quy trình revert của ADR-0025. Một lần chập chờn không mở `main-broken` nhờ lần chạy lại trong job và kết luận `infra` cho các lỗi hạ tầng đã biết (câu 4). Chọn nhãn nhẹ thì thay bước 8 của job `ui-test` bằng mở issue `ui-broken`, bỏ thay đổi của `find-breaking-commit.sh` và bộ lọc `check:ui-test` của `guard`, và cần quy tắc mới cho phiên (dừng hay làm tiếp khi có `ui-broken`).
3. **Chạy lại một lần trong job khi lần đầu đỏ, ghi `flaky` nếu lần sau xanh** (như ADR-0025 cho `check.sh`). Đỏ thì job dài gấp đôi phần test (khoảng 2 tới 4 phút ở dự án mẫu). Đề xuất đồng ý.
4. **Phạm vi của `infra`** (đã thu hẹp theo rà soát độc lập): chỉ các dòng PB-01, PB-11, PB-13, PB-15 của bảng tra lỗi, không có `ui-test.log`, và 0 test kèm dấu hiệu thiết bị hỏng. App dừng (`Process crashed`, `Instrumentation run failed`, `FATAL EXCEPTION`), 0 test không rõ lý do, và mọi lỗi khác là `red`. Còn lại một rủi ro: lỗi thật mà log giống hệt PB-01 (code tải R2 hỏng) thành `infra`; workflow vẫn đỏ, phiên đọc log theo bảng, quá hai lần thì dừng merge code app (câu 8). Đề xuất chấp nhận.
5. **Hai issue khi `guard` và `ui-test` cùng đỏ ở hai commit khác nhau**: mỗi job chỉ bình luận vào issue của chính nó, nên có thể có hai issue `main-broken`, và `merge-if-green.sh` khi đó từ chối mọi PR, kể cả bản revert (như nay khi có hai issue). Phương án khác: `guard` bình luận vào issue của emulator như nay, nhưng khi issue đó tự đóng thì lỗi của `guard` mất dấu. Đề xuất hai issue.
6. **`main-guard-status.sh` không đọc `ui-test`** (phiên có thể bắt đầu khi emulator của commit cuối đang chạy hay đỏ vì hạ tầng). Sửa script đó thì phạm vi lớn hơn; đỏ thật vẫn chặn qua `main-broken`. Đề xuất giữ, xem lại sau vài phiên.
7. **PR đang mở lúc merge T-075 có `ui-test` đỏ** vẫn bị từ chối tới khi có commit mới (thường là `--update`). Đề xuất chấp nhận, không thêm ngoại lệ.
8. **Đỏ vì hạ tầng kéo dài** (rà soát độc lập điểm 8). `release.sh` chỉ đọc commit đổi code mới nhất: commit code app X có `ui-test` `infra`, rồi một commit chỉ đổi script (ui-test `skipped`, workflow xanh), thì phát hành được mà emulator của X chưa xanh. Plan bù bằng: phiên dừng merge task đổi code app sau hai lần `infra` liên tiếp; `docs/ops/release.md` dặn chạy lại `ui-test` tới khi xanh (PR tăng `versionCode` đổi `app/build.gradle.kts` nên emulator luôn chạy trên commit phát hành). Không sửa `release.sh` (cấm). Đề xuất đồng ý; nếu muốn chặn cứng thì cần một ticket riêng cho `release.sh` có người làm.
9. **Repo công khai.** Từ 2026-10-30 `app-project-template` và `camera-pose` là repo công khai, phút Actions không tính tiền. Phần tiết kiệm của T-075 khi đó chỉ còn ở các app private nhận thay đổi qua `scripts/template-sync.sh` (ADR-0030); đổi lại dự án mẫu mất lớp emulator trước merge. Hai hướng: (a) vẫn làm, để mẫu và app giống nhau và app private được lợi (đề xuất, vì mọi app mới tạo từ mẫu và có thể private); (b) hoãn T-075 tới khi có app private cần. Người chọn.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:**, **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
