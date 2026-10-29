---
id: T-080
title: Bộ build AGP 9.1, Gradle 9.3.1, compileSdk 37, không đổi ảnh chuẩn
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-127 (issue #276), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: L
depends_on: [T-079]
sprint:
plan:
auto_run: false              # làm trong phiên có người theo dõi (quyết định 2026-10-29)
created: 2026-10-29
---

# T-080: BỘ BUILD AGP 9.1, GRADLE 9.3.1, COMPILESDK 37

## Mô tả

Bước 2 trong 3 bước tách từ T-052 (L-127, issue #276). Bước này nâng bộ build lên các bản đã chạy được ở Photune: AGP 9.1.0 (có Kotlin dựng sẵn), Gradle 9.3.1, compileSdk 37, kèm KSP, Hilt và ktlint-gradle. `build-logic` được viết lại theo cách AGP 9 cấu hình Kotlin. Robolectric và Roborazzi giữ bản cũ, nên ảnh chuẩn không được đổi: ảnh nào đổi ở bước này thì đó là thay đổi của bộ build, cần tìm nguyên nhân.

## Phạm vi

- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`:
  - AGP 9.1.0, Gradle 9.3.1, KSP 2.3.12, Hilt 2.59.2, ktlint-gradle 14.2.0, Navigation Compose 2.9.8;
  - Kotlin giữ 2.2.10;
  - bỏ plugin `kotlin-android`;
  - compileSdk 37, targetSdk giữ 36.
- `build-logic/`: bỏ `org.jetbrains.kotlin.android` trong `AndroidApplicationConventionPlugin` và `AndroidLibraryConventionPlugin`, cấu hình Kotlin theo AGP 9.
- `build.gradle.kts` gốc: bỏ alias `kotlin.android`.
- Kiểm lại các thư viện chỉ mẫu dùng trên bộ mới: Firebase, quảng cáo, mediation, AppsFlyer.
- ktlint bản mới: chạy `ktlintFormat`. Kiểm lại luật dòng dài của T-050. File Kotlin bị định dạng lại thì liệt kê trong plan.
- `CLAUDE.md`, mục Công nghệ (compileSdk 37, targetSdk 36).
- `cloud-setup.sh`: bỏ `platforms;android-36` nếu không còn cần.
- Plan chạy thử trên nhánh nâng cấp (mục 4.1): `scripts/check.sh`, `./gradlew :app:assembleStagingRelease` (R8), so manifest gộp trước và sau.

**Không bao gồm:**

- Robolectric, Roborazzi và ghi lại ảnh chuẩn (T-081).
- Nâng targetSdk lên 37.
- Thư viện riêng của Photune (Room, Media3).

## Rủi ro sơ bộ

Chạm `build-logic/` và `gradle/libs.versions.toml` (file "Không được làm" của `CLAUDE.md`) và mọi module, nên cần plan rủi ro cao được duyệt chéo. Chưa chắc Roborazzi 1.43 và Robolectric 4.15 chạy được trên AGP 9 và Gradle 9. Plan chạy thử điều này trước. Nếu không chạy được, plan ghi phương án dự phòng: gộp phần thư viện test của T-081 vào ticket này, nhưng vẫn không ghi lại ảnh chuẩn. Bản production chưa được kiểm trên bộ mới ở app nào.

Ảnh hưởng chéo: các ticket quảng cáo T-053 tới T-068 đang được soạn plan, có ticket thêm SDK (`libs.versions.toml`). Thứ tự merge với các ticket đó do người quyết khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi module build. `scripts/check.sh` và `scripts/tests/run.sh` qua trên bộ mới, không ảnh chuẩn nào đổi.
- [ ] `./gradlew :app:assembleStagingRelease` qua, manifest gộp không thêm quyền.
- [ ] File Kotlin bị ktlint mới định dạng lại được liệt kê và người duyệt đã xem.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính. `main-guard` và `staging` xanh.
