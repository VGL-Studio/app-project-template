---
ticket: T-079
title: Môi trường cloud và máy dev có sẵn Android SDK 37, kiểm compileSdk theo đúng tên gói
status: done
version: 1
risk_score: 4
risk_level: high
token_estimate: 5.4M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-29
lane:
---

# PLAN T-079: MÔI TRƯỜNG CLOUD VÀ MÁY DEV CÓ SẴN ANDROID SDK 37

## 1. Tóm tắt

`scripts/cloud-setup.sh` cài thêm gói `platforms;android-37.0` cạnh `platforms;android-36` và tăng `SETUP_VERSION` lên 5. `scripts/cloud-env-check.sh` nhận thư mục gói nền ở cả hai dạng tên, `platforms/android-<số>` và `platforms/android-<số>.0`. Lý do: gói của Android 37 nằm ở `platforms/android-37.0`, nên dòng kiểm hiện tại sẽ báo thiếu gói ngay khi T-080 đổi compileSdk thành 37. Với compileSdk 36 hiện tại, mọi thứ chạy như cũ. `scripts/local-setup.sh` không cần sửa: nó đọc danh sách gói từ `cloud-setup.sh`, và `--check` đã đổi tên gói thành đúng đường dẫn. Đây là bước 1 trong 3 bước tách từ T-052 (L-127, issue #276).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-127 và quyết định của anh Nhật ngày 2026-10-29: tách T-052 thành 3 bước; bước môi trường làm trước, vô hại với compileSdk 36, để lần dán setup script không đi cùng lần đổi build.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | `scripts/tests/run.sh` qua, có test cho tên thư mục gói 37 ở `cloud-env-check.sh` và `local-setup.sh --check`, và compileSdk 36 vẫn qua như cũ | Test với một danh mục phiên bản giả ghi `compileSdk = "37"` và SDK giả có `platforms/android-37.0`; test `--check` thấy gói `platforms;android-37.0` |
| Định nghĩa hoàn thành 2 | Người dán lại setup script mới, `env-canary` xanh | Kiểm tay (mục 4). `env-canary` chỉ kiểm compileSdk 36 của nhánh chính, nên thêm một lần chạy `cloud-env-check.sh` trong môi trường cloud với danh mục giả `compileSdk = "37"`: chỉ lần này chứng minh môi trường có gói 37 |
| Định nghĩa hoàn thành 3 | `scripts/local-setup.sh --check` trên máy dev thấy đủ gói | Kiểm tay trên máy dev sau khi merge (mục 4) |
| Định nghĩa hoàn thành 4 | CI qua, đã merge | Như mọi PR |

Cách ghi trong `gradle/libs.versions.toml` giữ dạng `compileSdk = "<số>"` (ticket T-079, phần Phạm vi): T-080 chỉ đổi số. Vì vậy phần đọc số của `cloud-env-check.sh` không đổi; chỉ đổi phần suy ra thư mục.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0009 (môi trường phiên tự chạy), ADR-0011 (bộ công cụ: plan này không đổi bộ công cụ, chỉ thêm gói nền). Không đổi quy tắc của ADR nào.

## 3. Giải pháp

### 3.1. Cách làm

**`scripts/cloud-setup.sh`:**
- Danh sách `--install` thêm `"platforms;android-37.0"` ngay sau `"platforms;android-36"`. `build-tools;36.0.0` giữ nguyên: AGP 9.1 vẫn dùng bản này làm mặc định (L-127).
- `SETUP_VERSION=5`.

**`scripts/cloud-env-check.sh`:** thay dòng kiểm

```sh
if [ ! -d "${ANDROID_HOME:-}/platforms/android-${compile_sdk}" ]; then
```

bằng kiểm hai thư mục, `android-${compile_sdk}` hoặc `android-${compile_sdk}.0`, viết `[ -d a ] || [ -d b ]` để chạy được trên bash 3.2 dưới `set -u` (L-075). Không nhận dạng có số phụ khác `.0`: `android-36.1` là một nền tảng khác (`AndroidVersion.ApiLevel=36.1`) và không thay được `android-36`. Câu kiểm thư mục chỉ gần đúng (AGP đọc `package.xml`), như trước; Photune dùng đúng câu kiểm này với AGP 9.1.

Thông báo khi thiếu nêu hai thư mục, không nêu tên gói, vì chỉ một trong hai tên gói có thật (không có gói `platforms;android-37` hay `platforms;android-36.0`): "Android SDK thiếu nền tảng compileSdk <số> (platforms;android-<số>: không có thư mục platforms/android-<số> hay platforms/android-<số>.0, ANDROID_HOME=...)". Dòng giữ chuỗi `platforms;android-<số>` để test có sẵn vẫn khớp (mục 3.5). Dòng in khi qua ghi tên thư mục tìm thấy, ví dụ "SDK (platforms/android-37.0)". Chú thích đầu file thêm một câu về hai dạng tên.

**`scripts/local-setup.sh`:** không sửa. `--check` đổi `platforms;android-37.0` thành `platforms/android-37.0` (`${package//;//}`), và `PACKAGE_PATTERN` đã nhận dấu chấm; test ở mục 4 khẳng định điều này.

**Tài liệu:** không có đoạn nào liệt kê tên gói SDK (`grep -rn 'android-36\|platforms;' docs/ops docs/auto-run.md CLAUDE.md` không có kết quả). `docs/ops/staging-and-runs.md` mục 2.1 đã nói dán lại script mỗi khi `SETUP_VERSION` tăng; `docs/ops/to-app.md` mục 7 đã có ghi chú `SETUP_VERSION` cho các app. Không sửa tài liệu.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/cloud-setup.sh` | Gói `platforms;android-37.0`, `SETUP_VERSION=5` |
| Sửa | `scripts/cloud-env-check.sh` | Nhận thư mục `android-<số>` hay `android-<số>.0`, thông báo nêu hai tên gói, chú thích |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4; dữ liệu của test `local-setup.sh --check lists every package` (mục 3.5) |
| Sửa | `docs/features/quy-trinh/tickets/T-079-moi-truong-sdk-37.md` | `plan:`, trạng thái theo plan |

Các file đọc file plan sửa:
- `scripts/cloud-setup.sh` được đọc bởi `scripts/cloud-env-check.sh` (dòng `SETUP_VERSION=`), `scripts/local-setup.sh` (bản command-line tools và danh sách sau `--install`), và `scripts/tests/run.sh`. Plan giữ nguyên dạng dòng `SETUP_VERSION=<số>` và dạng danh sách `--install` mỗi gói một dòng, nên hai script đọc được như cũ; test của `run.sh` bị ảnh hưởng nằm ở mục 3.5.
- `scripts/cloud-env-check.sh` được gọi bởi `scripts/env-canary.sh` (`ENV_CHECK_CMD`), chỉ dùng mã thoát và thông báo, không đọc nội dung dòng kiểm.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi:
- `CLAUDE.md`, `docs/auto-run.md`, `docs/ops/staging-and-runs.md`, `docs/ops/setup-new-app.md`, `docs/ops/to-app.md`, `docs/ops/session-capabilities.md`, `.github/ISSUE_TEMPLATE/from-app.yml`.
- `.github/workflows/pr-check.yml` và `scripts/check.sh` chạy `scripts/tests/run.sh`, không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền.
- **Thư viện và SDK:** Không thêm thư viện. Thêm một gói nền của Android SDK vào môi trường (`platforms;android-37.0`); build vẫn dùng compileSdk 36 cho tới T-080.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** môi trường cloud tải được gói `platforms;android-37.0` qua proxy (cùng máy chủ `dl.google.com` với các gói hiện có), và gói cài vào `platforms/android-37.0` trên Linux như trên máy dev (mục 4.1).
- **Điều kiện chuyển:** sau khi dán script, kiểm compileSdk 37 trong môi trường cloud (mục 4) không qua: `sdkmanager` không cài được gói, hay gói nằm ở thư mục khác hai dạng tên.
- **Cách làm thay:** chưa merge thì sửa trên nhánh (tên thư mục thật vào câu kiểm, hay gỡ gói), dán lại. Đã merge thì gỡ dòng gói khỏi `cloud-setup.sh` trong một PR mới, tăng `SETUP_VERSION`, dán lại; ghi tên thư mục thật vào ticket để T-080 xử lý cùng lần đổi build. Tiêu chí và test ở mục 4 không đổi.

### 3.5. Hành vi đã có bị đổi

Không có code Kotlin. Hành vi của script bị đổi:

| Hành vi cũ | Hành vi mới | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `cloud-env-check.sh` chỉ nhận `platforms/android-<số>` | Nhận thêm `platforms/android-<số>.0` | `cloud-env-check.sh stops without the compileSdk platform`, `names the missing platform` (grep `platforms;android-$compile_sdk`), `passes with the compileSdk platform` | Giữ cả ba: thông báo mới vẫn chứa `platforms;android-<số>` trên một dòng |
| `cloud-env-check.sh` in "SDK (android-<số>)" khi qua | In tên thư mục tìm thấy, ví dụ "SDK (platforms/android-37.0)" | Không test nào so dòng này (chỉ so `pass` hay `stop`) | Thêm câu kiểm ở mục 4 |
| `cloud-setup.sh` cài 3 gói | Cài 4 gói | `local-setup.sh --check lists every package`: mong `3` dòng `QUA:` của gói | Dữ liệu của test đổi `3` thành `4` vì danh sách có gói plan thêm (ADR-0029); test vẫn kiểm mọi gói, kể cả gói mới. Đổi này vào commit test của bước 3 (L-025), cùng lúc với dòng gói mới chưa có, để test đỏ đúng lý do |
| `SETUP_VERSION` là 4 | Là 5 | `cloud-env-check.sh passes on the current version` đọc số từ file, không viết cứng | Giữ |

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, khối `cloud-env-check.sh` (dùng `env_check`, `VERSIONS_TOML`, `ENV_SDK`) và khối `local-setup.sh` (sdkmanager giả tạo thư mục của từng gói).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Setup script cài gói 37 và giữ gói 36 | Test đọc file | `cloud-setup.sh installs platforms;android-37.0 next to platforms;android-36`: cả hai dòng có trong danh sách `--install` |
| compileSdk 37 qua với thư mục `android-37.0` | Test script | `cloud-env-check.sh passes for compileSdk 37 with platforms/android-37.0`: danh mục giả `compileSdk = "37"`, SDK giả chỉ có `platforms/android-37.0`: `pass`, và `$work/env-check.out` có `platforms/android-37.0`. Trước khi sửa: `stop` |
| compileSdk 37 qua với thư mục `android-37` | Test script | `cloud-env-check.sh passes for compileSdk 37 with platforms/android-37`: `pass` (đối chứng, qua cả trước khi sửa) |
| compileSdk 37 thiếu gói thì dừng, nêu hai tên | Test script | `cloud-env-check.sh names both package names for a missing compileSdk 37`: SDK rỗng: `stop`, có dòng chứa `platforms;android-37.0`. Trước khi sửa: không có dòng đó |
| Số phụ khác `.0` không thay được | Test script | `cloud-env-check.sh does not take platforms/android-36.1 for compileSdk 36`: danh mục giả `compileSdk = "36"` (không dùng compileSdk của repo, để tên test vẫn đúng sau T-080), SDK giả chỉ có `android-36.1`: `stop` (đối chứng, giữ hành vi cũ) |
| compileSdk 36 vẫn qua như cũ | Test script | Các test có sẵn của khối (mục 3.5), không đổi |
| `local-setup.sh --check` thấy gói 37 | Test script | `local-setup.sh --check finds platforms;android-37.0`: sau khi cài giả, có dòng `QUA: platforms;android-37.0`. Trước khi sửa: không có, vì gói chưa có trong `cloud-setup.sh`. sdkmanager giả tạo thư mục bằng cùng phép đổi mà `--check` dùng, nên test này chỉ chứng minh danh sách gói được đọc; tên thư mục thật chứng minh ở bước tay trên máy dev |
| `--check` liệt kê mọi gói | Test script | `local-setup.sh --check lists every package`: `4` (mục 3.5) |
| Môi trường cloud có gói 37 | Kiểm tra thủ công (người: dán setup script vào môi trường cloud trên claude.ai, rồi một phiên cloud có người) | Người dán toàn bộ `scripts/cloud-setup.sh` của nhánh vào Setup script của môi trường (`docs/ops/staging-and-runs.md`, mục 2.1). Trong một phiên cloud mới của môi trường đó, chạy `printf 'compileSdk = "37"\n' > "${TMPDIR:-/tmp}/v37.toml" && VERSIONS_TOML="${TMPDIR:-/tmp}/v37.toml" scripts/cloud-env-check.sh` và `ls "$ANDROID_HOME/platforms"`: mã 0, dòng qua có `platforms/android-37.0` |
| Môi trường cloud dựng được từ script mới | Kiểm tra thủ công (người: GitHub Actions sau khi merge) | Chạy tay **Actions > env-canary > Run workflow**: xanh, không mở issue `env-broken` |
| Máy dev có đủ gói | Kiểm tra thủ công (người: máy dev của anh Nhật) | `scripts/local-setup.sh` rồi `scripts/local-setup.sh --check` (SDK mặc định `~/android-sdk`, hiện chỉ có `android-36`): mọi dòng `QUA:`, mã 0, và `ls ~/android-sdk/platforms` có `android-37.0` |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085). Các test script mới không phải đối chứng thất bại trước khi sửa script, đúng lý do ghi ở cột cuối.

**Ngoại lệ viết test trước:** chú thích đầu `scripts/cloud-env-check.sh`.

### 4.1. Chạy thử

Không cần (E dưới 2). Đã xem trên máy dev, 2026-10-29, ở SDK của Android Studio (không phải `~/android-sdk` của `local-setup.sh`): gói nền Android 37 đã cài ở `~/Library/Android/sdk/platforms/android-37.0`, `package.xml` ghi `path="platforms;android-37.0"`, `source.properties` ghi `AndroidVersion.ApiLevel=37.0`. Cùng thư mục có `android-36` (`ApiLevel=36`) và `android-36.1` (`ApiLevel=36.1`), nên số phụ `.1` là nền tảng khác và không được nhận thay cho `android-36`. Môi trường cloud (Linux, qua proxy) chỉ kiểm được khi dán script; mục 8, câu 1 đề xuất làm việc này trước khi merge.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (môi trường của phiên tự chạy: setup script sai thì mọi phiên dừng ở đầu phiên). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai script và test; không chạm code app |
| B. Độ rõ của yêu cầu | 0 | Ticket chốt phạm vi; tên thư mục đã xem trên máy thật |
| C. Khả năng kiểm chứng tự động | 1 | Logic có test; dựng môi trường cloud chỉ kiểm tay bằng `env-canary` |
| D. Ảnh hưởng chéo | 1 | Mọi phiên dùng môi trường này; app nhận qua đồng bộ phải dán lại |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của dòng kiểm đã có |
| **Tổng** | 4 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `scripts/cloud-setup.sh` (môi trường của mọi phiên), `scripts/cloud-env-check.sh` (kiểm tra đầu phiên, `env-canary`).
- Task khác có thể xung đột: PR #292 (T-074 tới T-078, giảm phút Actions) và các plan quảng cáo T-053 tới T-068 không chạm hai file này. T-080 phụ thuộc ticket này.
- Các app nhận qua `scripts/template-sync.sh`: `SETUP_VERSION` theo ghi chú ở `docs/ops/to-app.md`, mục 7; app dán lại script.
- Đề xuất thứ tự: làm trong phiên có người, trước T-080.
- Cho T-080: CI không cài gói nền (`setup-android` cài `platforms;android-34`, AGP tự tải phần còn lại), nên T-079 không đổi CI. Fixture `sdk-ok/platforms/android-$compile_sdk` của `run.sh` sau T-080 sẽ là `android-37`, khác thư mục thật `android-37.0`; plan T-080 có thể đổi fixture sang dạng `.0`. Sau khi merge, dán script và chạy `env-canary` trước khi bắt đầu phiên tự chạy kế tiếp (đầu phiên báo phiên bản setup cũ và dừng nếu chưa dán).

## 7. Điều kiện dừng riêng

- `env-canary` đỏ sau khi dán script mới mà không phải vì gói 37 (ví dụ tải `build-tools` hay mirror Maven lỗi): dừng, báo người; không sửa thêm phần khác của setup script trong plan này.

## 8. Câu hỏi cho người duyệt

1. Thứ tự kiểm môi trường cloud. Đề xuất: sau khi CI của PR code xanh, trước khi merge, anh dán `scripts/cloud-setup.sh` của nhánh vào môi trường và chạy kiểm compileSdk 37 trong một phiên cloud (mục 4). Qua thì merge ngay, rồi chạy `env-canary`. Không qua thì chưa merge, sửa trên nhánh, khỏi phải làm PR gỡ gói. Trong khe giữa lúc dán và lúc merge, môi trường báo phiên bản 5 còn nhánh chính ở 4, nên phiên nào mở trong khe đó sẽ dừng ở đầu phiên. Hiện không có phiên hay danh sách chờ nào, và khe chỉ vài phút. Nếu anh muốn dán sau khi merge: phiên tự chạy kế tiếp dừng ở đầu phiên cho tới khi dán, và gói lỗi thì cần một PR gỡ (mục 3.4).
2. Giữ `platforms;android-36` trong setup script sau T-080 (vô hại, môi trường lớn hơn khoảng 100 MB). Bỏ nó là việc của một lần sửa setup script sau, không phải của T-080. Đề xuất giữ.

## 9. Quyết định duyệt

Phiên bản 1 (AI ghi theo lời anh Nhật, 2026-10-29):

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua một vòng rà soát độc lập, 1 điểm chặn và 4 điểm nên sửa đã xử lý ở commit 4dec5cd)
- **Ghi chú:** trả lời mục 8: (1) theo đề xuất: dán setup script của nhánh và kiểm compileSdk 37 trong phiên cloud trước khi merge, qua thì merge ngay rồi chạy `env-canary`; (2) giữ `platforms;android-36` sau T-080. Làm trong phiên có người, 2026-10-29.

## 10. Nhật ký thực hiện

### Phiên có người theo dõi 2026-10-29, phiên bản plan 1

- **Kết quả:** đã merge, PR #310 (commit merge `21ad19a`). Anh Nhật cho làm trong phiên có người theo dõi, 2026-10-29.
- **Nhánh, PR:** `claude/T-079-moi-truong-sdk-37`, PR #310. Commit test `2e94ced`, commit code `37ffe9a` (sau khi đưa lên main); PR plan #309.
- **Đã làm:** `scripts/cloud-setup.sh` cài thêm `platforms;android-37.0`, giữ `platforms;android-36`, `SETUP_VERSION=5`. `scripts/cloud-env-check.sh` nhận `platforms/android-<số>` hay `platforms/android-<số>.0`, không nhận số phụ khác; khi thiếu, thông báo nêu hai thư mục; khi qua, dòng in ghi thư mục tìm thấy. `scripts/local-setup.sh` không sửa.
- **Test viết trước:** 6 test mới, cộng dữ liệu `3` thành `4` của `local-setup.sh --check lists every package` (ADR-0029). Trước khi có code, đúng 5 test đỏ theo lý do của mục 4; hai đối chứng (`android-37`, `android-36.1`) qua.
- **Rà soát độc lập:** plan: 1 chặn (`env-canary` không chứng minh có gói 37), 4 nên sửa, đã sửa trước khi duyệt. Code: 0 chặn, 0 nên sửa, 3 ghi chú (thông báo giữ chuỗi `platforms;android-<số>` để khớp test cũ; tên test; nhật ký ghi sau khi merge).
- **Kết quả CI:** `bash scripts/tests/run.sh` trên máy: 1.154 ok, `All script tests passed`. `pr-check` xanh (lần chạy 40910635933, 40913562144). `main-guard` của commit merge xanh (lần chạy 40914384117).
- **Kiểm tra thủ công:**
  - Môi trường cloud có gói 37 (trước khi merge, theo mục 8 câu 1): anh Nhật dán setup script của nhánh; một phiên cloud ở commit `13e4e16` chạy `cloud-env-check.sh` với danh mục giả `compileSdk = "37"`: mã 0, "Môi trường cloud dùng setup script phiên bản 5; Java, Gradle wrapper và SDK (platforms/android-37.0) chạy được." Lần chạy đầu báo không đọc được compileSdk vì dấu ngoặc kép bị mất khi chép lệnh, không phải lỗi của script.
  - `env-canary` chạy tay sau khi merge (lần chạy 40915278455): job `fire` và `setup-check` xanh; kết quả của routine kiểm môi trường xem ở issue `env-broken` (không mở là xanh).
- **Lệch so với plan:** Không có. Ngoài mục 3.2: không có.
- **Việc còn lại:** `scripts/local-setup.sh` rồi `--check` trên máy dev (mục 4, dòng cuối): `~/android-sdk/platforms` có `android-37.0`.
- **Cần người xem:** chưa thấy tận mắt `android-36` còn trong môi trường cloud (kết quả `ls` không được gửi lại); routine `env-canary` với compileSdk 36 sẽ báo nếu thiếu.
