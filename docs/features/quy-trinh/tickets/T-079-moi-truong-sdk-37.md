---
id: T-079
title: Môi trường cloud và máy dev có sẵn Android SDK 37, kiểm compileSdk theo đúng tên gói
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-127 (issue #276), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: S
depends_on: []
sprint:
plan: features/quy-trinh/plans/T-079-moi-truong-sdk-37.md
auto_run: false              # làm trong phiên có người theo dõi (quyết định 2026-10-29); người dán lại setup script
created: 2026-10-29
---

# T-079: MÔI TRƯỜNG CLOUD VÀ MÁY DEV CÓ SẴN ANDROID SDK 37

## Mô tả

Bước 1 trong 3 bước tách từ T-052: nâng bộ công cụ build lên AGP 9.1, Gradle 9.3.1, compileSdk 37 (L-127, issue #276). Bước này chỉ chuẩn bị môi trường, trước khi đổi build. Môi trường cloud và máy dev cài thêm gói nền Android 37. `cloud-env-check.sh` và `local-setup.sh --check` tìm đúng thư mục của gói nền theo compileSdk. Với compileSdk 36 hiện tại, mọi thứ vẫn chạy như cũ. Nhờ vậy, lần dán setup script mới tách khỏi lần đổi build.

## Phạm vi

- `scripts/cloud-setup.sh`: thêm gói `platforms;android-37.0` (tên gói theo L-127), giữ `platforms;android-36` cho tới khi T-080 xong; tăng `SETUP_VERSION`.
- Cách ghi trong `gradle/libs.versions.toml` giữ nguyên dạng `compileSdk = "<số>"`; T-080 chỉ đổi số thành 37, nên phần đọc số của `cloud-env-check.sh` (dòng `compileSdk = ...`) không đổi.
- `scripts/cloud-env-check.sh`: kiểm thư mục của gói nền theo đúng tên gói được cài. Gói `platforms;android-37.0` có thể nằm ở thư mục `platforms/android-37.0`, không phải `android-37` như dòng kiểm hiện tại suy ra từ số. Plan xác nhận tên thư mục bằng một lần cài thật, rồi cho script nhận cả hai dạng.
- `scripts/local-setup.sh`: script đọc danh sách gói từ `cloud-setup.sh`. `--check` đổi tên gói thành đường dẫn thư mục, nên có cùng câu hỏi về tên thư mục như `cloud-env-check.sh`. Mã băm chỉ áp cho JDK và command-line tools, không đổi.
- `docs/ops/to-app.md` (ghi chú `SETUP_VERSION`), `docs/ops/staging-and-runs.md` mục 2.1 nếu nhắc danh sách gói.
- Test trong `scripts/tests/run.sh` cho phần đọc `compileSdk` của `cloud-env-check.sh`.

**Không bao gồm:**

- Đổi compileSdk, AGP, Gradle hay thư viện (T-080).
- Robolectric, Roborazzi, ảnh chuẩn (T-081).

## Rủi ro sơ bộ

Chạm môi trường của mọi phiên tự chạy (điểm chặn 7, quy trình phiên). Setup script sai thì mọi phiên dừng ở đầu phiên, cho tới khi người dán lại. Không chạm file "Không được làm" của `CLAUDE.md`. Phải kiểm lại bằng `env-canary` sau khi người dán script mới.

## Định nghĩa hoàn thành

- [ ] `scripts/tests/run.sh` qua, có test cho tên thư mục gói 37 (`android-37.0`) ở cả `cloud-env-check.sh` và `local-setup.sh --check`, và test cho compileSdk 36 vẫn qua như cũ.
- [ ] Người dán lại `scripts/cloud-setup.sh` mới vào môi trường cloud; `env-canary` xanh.
- [ ] `scripts/local-setup.sh --check` trên máy dev thấy đủ gói.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
