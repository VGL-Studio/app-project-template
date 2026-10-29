---
id: T-052
title: Nâng bộ công cụ build lên AGP 9.1, Gradle 9.3.1, compileSdk 37
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-127 (issue #276), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: L
depends_on: []
sprint:
plan:
created: 2026-10-29
---

# T-052: NÂNG BỘ CÔNG CỤ BUILD LÊN AGP 9.1, GRADLE 9.3.1, COMPILESDK 37

## Mô tả

Dự án mẫu đang ở AGP 8.13.0, Gradle 8.14.3, compileSdk 36. Photune đã build, test và phát staging bằng AGP 9.1.0, Gradle 9.3.1, compileSdk 37 qua 2 phiên và khoảng 15 PR (L-127, issue #276). Nâng cấp giúp các app mới bắt đầu từ bộ công cụ hiện hành, và giúp Photune về khung nhiều module của mẫu mà không phải hạ AGP. Sau ticket này, mọi module của mẫu build và qua `scripts/check.sh` trên bộ công cụ mới.

## Phạm vi

- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`: AGP 9.1.0, Gradle 9.3.1, Kotlin, KSP, Hilt, Robolectric, Roborazzi, ktlint-gradle theo các bản đã chạy ở Photune (L-127); kiểm lại các thư viện chỉ mẫu dùng (Firebase, quảng cáo, mediation, AppsFlyer).
- `build-logic/`: làm lại phần cấu hình Kotlin theo AGP 9 (Kotlin dựng sẵn trong AGP).
- `scripts/cloud-setup.sh` (gói `platforms;android-37.0`, tăng `SETUP_VERSION`), `scripts/cloud-env-check.sh` (đọc `compileSdk`), `scripts/local-setup.sh` nếu đọc cùng danh sách gói.
- Plan chạy thử `scripts/check.sh` và `./gradlew :app:assembleStagingRelease` trên nhánh nâng cấp (mục 4.1), so manifest gộp trước và sau, ghi mọi ảnh chuẩn screenshot bị đổi.

**Không bao gồm:**

- Nâng targetSdk lên 37.
- Thư viện riêng của Photune (Room, Media3).

## Rủi ro sơ bộ

Chạm `build-logic/` và `gradle/libs.versions.toml` (file "Không được làm" của `CLAUDE.md`), mọi module và cả môi trường cloud: cần plan rủi ro cao được duyệt chéo. Ảnh chuẩn screenshot có thể đổi theo Robolectric và Roborazzi mới. Bản production chưa được kiểm trên bộ mới ở app nào.

## Định nghĩa hoàn thành

- [ ] Mọi module build, `scripts/check.sh` và `scripts/tests/run.sh` qua trên bộ công cụ mới.
- [ ] `./gradlew :app:assembleStagingRelease` qua, manifest gộp không thêm quyền.
- [ ] Môi trường cloud dựng lại được bằng `cloud-setup.sh` mới; `env-canary` xanh.
- [ ] Ảnh chuẩn đổi (nếu có) được liệt kê trong plan và người duyệt đã xem.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
