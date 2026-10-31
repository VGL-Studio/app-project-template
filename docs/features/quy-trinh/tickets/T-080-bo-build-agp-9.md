---
id: T-080
title: Bộ build AGP 9.1, Gradle 9.3.1, compileSdk 37, không đổi ảnh chuẩn
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-127 (issue #276), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: on-staging
risk: high
size: L
depends_on: [T-079]
sprint:
plan: features/quy-trinh/plans/T-080-bo-build-agp-9.md
auto_run: false              # làm trong phiên có người theo dõi (quyết định 2026-10-29)
created: 2026-10-29
---

# T-080: BỘ BUILD AGP 9.1, GRADLE 9.3.1, COMPILESDK 37

## Mô tả

Bước 2 trong 3 bước tách từ T-052 (L-127, issue #276). Bước này nâng bộ build lên các bản đã chạy được ở Photune: AGP 9.1.0 (có Kotlin dựng sẵn), Gradle 9.3.1, compileSdk 37, kèm KSP, Hilt và ktlint-gradle. `build-logic` được viết lại theo cách AGP 9 cấu hình Kotlin. Robolectric giữ bản cũ; Roborazzi lên 1.77.0 vì bản 1.43 cần DSL cũ của AGP (plan T-080, mục 8, câu 1). Ảnh chuẩn không được đổi: ảnh nào đổi ở bước này thì đó là thay đổi của bộ build, cần tìm nguyên nhân.

## Phạm vi

- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`:
  - AGP 9.1.0, Gradle 9.3.1, KSP 2.3.12, Hilt 2.59.2, ktlint-gradle 14.2.0, Navigation Compose 2.9.8;
  - Kotlin giữ 2.2.10;
  - bỏ plugin `kotlin-android`;
  - compileSdk 37, targetSdk giữ 36.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` đi theo lần nâng wrapper.
- `build-logic/`:
  - bỏ `org.jetbrains.kotlin.android` trong `AndroidApplicationConventionPlugin` và `AndroidLibraryConventionPlugin`;
  - `KotlinAndroid.kt` cấu hình Kotlin theo AGP 9: `KotlinAndroidProjectExtension` và kiểu `CommonExtension` đổi;
  - `AndroidComposeConventionPlugin.kt` cũng dùng `CommonExtension`;
  - `build-logic/convention/build.gradle.kts` (`kotlin.gradlePlugin`).
- `build.gradle.kts` gốc: bỏ alias `kotlin.android`. File build của từng module (`app/build.gradle.kts`, `core/*/build.gradle.kts`) đổi khi DSL của AGP 9 khác.
- `gradle.properties`: cờ của AGP 9 nếu cần. KSP 2.3.12 chạy với mặc định; bản cũ hơn cần `android.disallowKotlinSourceSets=false`, dùng làm phương án dự phòng của plan.
- `scripts/java-check.sh`: bảng giới hạn Java theo bản Gradle chưa có dòng cho Gradle 9.3 (trả rỗng, không có giới hạn trên). Thêm giới hạn của Gradle 9.3, kèm test, để Java quá mới vẫn bị chặn (L-059).
- Kiểm lại trên bộ mới các thư viện và plugin chỉ mẫu dùng: Firebase, quảng cáo, mediation, AppsFlyer, plugin `googleServices` và `firebaseCrashlyticsGradle`.
- ktlint bản mới: chạy `ktlintFormat`. Kiểm lại luật dòng dài của T-050. File Kotlin bị định dạng lại thì liệt kê trong plan.
- `CLAUDE.md`, mục Công nghệ (compileSdk 37, targetSdk 36).
- ADR-0011 (`accepted`) ghi Gradle 8.14.3, AGP 8.13, compileSdk 36. Không sửa ADR đó: viết ADR mới thay phần bộ công cụ của ADR-0011, rồi chạy `scripts/foundation.py`.
- `cloud-setup.sh` không đổi: giữ `platforms;android-36` (vô hại), để lần đổi build không kéo theo một lần dán lại setup script. Bỏ gói đó, nếu muốn, là việc của một lần dán sau.
- Plan chạy thử trên nhánh nâng cấp (mục 4.1): `scripts/check.sh`, `./gradlew :app:assembleStagingRelease` (R8), so manifest gộp trước và sau.

**Không bao gồm:**

- Robolectric và ghi lại ảnh chuẩn (T-081).
- Nâng targetSdk lên 37.
- Thư viện riêng của Photune (Room, Media3).

## Rủi ro sơ bộ

Chạm `build-logic/` và `gradle/libs.versions.toml` (file "Không được làm" của `CLAUDE.md`) và mọi module, nên cần plan rủi ro cao được duyệt chéo. Chưa chắc Roborazzi 1.43 và Robolectric 4.15 chạy được trên AGP 9 và Gradle 9. Plan chạy thử điều này trước. Nếu không chạy được, plan ghi phương án dự phòng: gộp phần thư viện test của T-081 vào ticket này, nhưng vẫn không ghi lại ảnh chuẩn. Bản production chưa được kiểm trên bộ mới ở app nào.

Ảnh hưởng chéo: các ticket quảng cáo T-053 tới T-068 đang được soạn plan, có ticket thêm SDK (`libs.versions.toml`); PR #292 (T-074 tới T-078, giảm phút Actions) có thể chạm cùng workflow CI. Thứ tự merge với các ticket đó do người quyết khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi module build. `scripts/check.sh` và `scripts/tests/run.sh` qua trên bộ mới, không ảnh chuẩn nào đổi.
- [ ] `./gradlew :app:assembleStagingRelease` qua, manifest gộp không thêm quyền.
- [ ] File Kotlin bị ktlint mới định dạng lại được liệt kê và người duyệt đã xem.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính. `main-guard` và `staging` xanh.
