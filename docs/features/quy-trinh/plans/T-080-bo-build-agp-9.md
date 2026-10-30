---
ticket: T-080
title: Bộ build AGP 9.1, Gradle 9.3.1, compileSdk 37, không đổi ảnh chuẩn
status: draft
version: 1
risk_score: 11
risk_level: high
token_estimate: 30.0M
blocking_points: [build, sdk]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-080: BỘ BUILD AGP 9.1, GRADLE 9.3.1, COMPILESDK 37

## 1. Tóm tắt

Plan nâng bộ build lên các bản Photune đang chạy được: AGP 9.1.0 (có Kotlin dựng sẵn), Gradle 9.3.1, compileSdk 37, KSP 2.3.12, Hilt 2.59.2, ktlint-gradle 14.2.0, Navigation Compose 2.9.8. Plugin `kotlin-android` bỏ khỏi `build-logic`, và `KotlinAndroid.kt` viết theo `CommonExtension` của AGP 9. Roborazzi lên 1.77.0 ngay trong plan này, vì Roborazzi 1.43 không chạy với DSL mới của AGP 9 (mục 4.1); Robolectric giữ 4.15.1. Lần dựng thử trên máy dev (mục 4.1) cho kết quả: `scripts/check.sh` qua với 1455 test JVM và 89 ảnh chuẩn, không ảnh nào đổi; `./gradlew :app:assembleStagingRelease` qua; manifest gộp không đổi. Đây là bước 2 trong 3 bước tách từ T-052 (L-127, issue #276).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-127 và quyết định của anh Nhật ngày 2026-10-29: tách T-052 thành 3 bước; bước này đổi bộ build mà không đổi ảnh chuẩn.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | Mọi module build; `scripts/check.sh` và `scripts/tests/run.sh` qua trên bộ mới, không ảnh chuẩn nào đổi | `check.sh` qua; số test JVM không giảm so với `main-guard` gần nhất (1455 test, 89 ảnh ngày 2026-10-30); `git status --short -- '*/src/test/screenshots/'` rỗng |
| Định nghĩa hoàn thành 2 | `./gradlew :app:assembleStagingRelease` qua, manifest gộp không thêm quyền | So `uses-permission` của manifest gộp `stagingDebug` và `stagingRelease` trước và sau. Bản production không dựng được khi không có tham số thật (ADR-0013), nên không so |
| Định nghĩa hoàn thành 3 | File Kotlin bị ktlint mới định dạng lại được liệt kê, người duyệt đã xem | Lần thử: ktlint 14.2.0 không định dạng lại file nào (mục 4.1). Phiên chạy lại `./gradlew ktlintFormat` và liệt kê file đổi, nếu có |
| Định nghĩa hoàn thành 4 | CI qua, đã merge; `main-guard` và `staging` xanh | Như mọi PR, cộng một lần chạy `staging.yml` sau khi merge |
| Phạm vi: `java-check.sh` | Java quá mới vẫn bị chặn với Gradle 9.3 (L-059) | Bảng tương thích của Gradle 9.3.1 (tra 2026-10-30): Gradle 9.1 trở lên chạy trên Java 25, Java 26 chưa được hỗ trợ. Giới hạn của 9.1 tới 9.3 là Java 25; bản mới hơn bảng thì `java-check.sh` không đặt giới hạn |
| Phạm vi: ADR | Viết ADR mới thay phần bộ công cụ của ADR-0011, không sửa ADR-0011 | ADR-0040 viết trong PR plan này ở trạng thái `proposed`, `supersedes: ADR-0011` chỉ phần Phiên bản, như ADR-0025 thay một phần ADR-0010 và như ADR-0039 được viết trong PR plan T-074; người ghi `accepted` khi duyệt plan. Phiên thêm một dòng ghi chú dưới tiêu đề ADR-0011, không đổi nội dung (mục 8, câu 6) |

Có một chỗ lệch với ticket: Roborazzi. Ticket để Roborazzi cho T-081, và ghi phương án dự phòng là đưa thư viện test của T-081 vào ticket này nếu bản cũ không chạy trên AGP 9. Lần thử cho thấy Roborazzi 1.43 chỉ chạy được khi tắt DSL mới của AGP 9 (`android.newDsl=false`). Cờ này AGP báo là sắp bỏ và sẽ bị gỡ ở AGP 10. Plan chọn nâng Roborazzi lên 1.77.0 ngay ở đây (phương án B của mục 4.1): 89 ảnh chuẩn vẫn khớp, và đối chứng cho thấy ảnh lệch vẫn bị bắt. Cách giữ ticket như cũ (phương án A, `android.newDsl=false`) cũng đã thử và qua; nó là phương án dự phòng ở mục 3.4. Người duyệt chọn ở mục 8, câu 1. Theo phương án chính, T-081 còn Robolectric 4.17 và việc ghi lại ảnh chuẩn; plan sửa phạm vi của ticket T-081 cho khớp.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0011 (bộ công cụ build; plan viết ADR-0040 thay phần Phiên bản), ADR-0001 (`checkModuleDependencies`, chạy như cũ), ADR-0012 (file Firebase giả của staging, không đổi), ADR-0013 (bản production cần tham số thật, không dựng).

## 3. Giải pháp

### 3.1. Cách làm

Mọi bước đã chạy trên nhánh thử `claude/trial-T-080` (chỉ có trên máy dev, chưa đẩy; mục 4.1). Phiên làm lại đúng các bước dưới đây, không cần nhánh thử.

**1. `gradle/libs.versions.toml`** (file được bảo vệ, plan nêu rõ để phiên được sửa):
- `compileSdk = "37"`. Giữ đúng dạng một dòng `compileSdk = "<số>"`, vì `scripts/cloud-env-check.sh` và `scripts/tests/run.sh` đọc dòng này. `targetSdk = "36"`, `minSdk = "26"` giữ nguyên.
- `androidGradlePlugin = "9.1.0"`, `ksp = "2.3.12"`, `ktlintGradle = "14.2.0"`, `androidxNavigation = "2.9.8"`, `hilt = "2.59.2"`, `roborazzi = "1.77.0"`. `kotlin = "2.2.10"` giữ nguyên.
- Xóa dòng `kotlin-android = { id = "org.jetbrains.kotlin.android", ... }` ở `[plugins]`. Thư viện `kotlin-gradlePlugin` giữ, vì `build-logic` vẫn cần kiểu `KotlinAndroidProjectExtension` và `JvmTarget` lúc biên dịch.

**2. Gradle wrapper:** đổi `distributionUrl` trong `gradle/wrapper/gradle-wrapper.properties` sang `gradle-9.3.1-bin.zip` trước, rồi chạy `./gradlew wrapper --gradle-version 9.3.1`. Gradle 8.14.3 cũ không cấu hình được AGP 9, nên phải đổi file trước. Lệnh sinh lại `gradle/wrapper/gradle-wrapper.jar`, `gradlew` và `gradlew.bat`. File jar phải là bản Gradle sinh ra, không tải từ nơi khác: `gradle/actions/setup-gradle@v4` của CI tự kiểm mã băm của wrapper.

**3. `build.gradle.kts` gốc:** xóa dòng `alias(libs.plugins.kotlin.android) apply false`.

**4. `build-logic/`** (được bảo vệ):
- `AndroidApplicationConventionPlugin.kt`, `AndroidLibraryConventionPlugin.kt`: xóa dòng `pluginManager.apply("org.jetbrains.kotlin.android")`. AGP 9 có Kotlin dựng sẵn và đăng ký `KotlinAndroidProjectExtension` (lần thử thấy extension này trong danh sách của `:app`).
- `KotlinAndroid.kt`: `CommonExtension` của AGP 9 không còn tham số kiểu, và không còn các hàm khối `defaultConfig { }`, `compileOptions { }`, `testOptions { }`, `packaging { }`, chỉ còn thuộc tính (đã xem bằng `javap` trên `gradle-api-9.1.0.jar`). Đổi như sau (đúng diff của lần thử):

```diff
+import org.gradle.api.tasks.testing.Test
 import org.gradle.kotlin.dsl.configure
 import org.gradle.kotlin.dsl.dependencies
+import org.gradle.kotlin.dsl.withType
 ...
-internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
+internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
     commonExtension.apply {
         compileSdk = libs.versionInt("compileSdk")
-        defaultConfig {
+        defaultConfig.apply {
 ...
-        compileOptions {
+        compileOptions.apply {
 ...
-        testOptions {
+        testOptions.apply {
 ...
-        packaging {
+        packaging.apply {
 ...
+    // Gradle 9 fails a test task that finds no test, and Hilt gives a module without src/test generated test
+    // classes. Only such a module may have no test; elsewhere an empty run still fails.
+    if (!file("src/test").exists()) {
+        tasks.withType<Test>().configureEach {
+            failOnNoDiscoveredTests.set(false)
+        }
+    }
+
     dependencies {
```

  Khối `extensions.configure<KotlinAndroidProjectExtension> { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` giữ nguyên. Phần `failOnNoDiscoveredTests`: Gradle 9 cho task test đỏ khi có "nguồn test" mà không tìm thấy test nào. `:core:data` không có thư mục `src/test`, vậy mà `:core:data:testDebugUnitTest` đỏ với thông báo "There are test sources present ... did not discover any tests" (lần thử thứ 2, mục 4.1). Giả thuyết: lớp do Hilt sinh cho source set unit test (log có `hiltSyncDebugUnitTest`, `hiltAggregateDepsDebugUnitTest`) được Gradle tính là nguồn test; bằng chứng chắc chỉ là thông báo lỗi. Plan chỉ tắt kiểm tra này ở module không có `src/test`. Module có `src/test` mà không chạy được test nào vẫn đỏ như Gradle 9 muốn.
- `AndroidComposeConventionPlugin.kt` dùng `extensions.getByType(CommonExtension::class.java)` và `extension.buildFeatures.compose = true`: biên dịch được trên AGP 9 mà không sửa (lần thử). Ticket ghi file này trong phạm vi; plan không sửa nó.
- `AndroidFeatureConventionPlugin.kt` (`RoborazziExtension.outputDir`), `HiltConventionPlugin.kt`, `ProjectExtensions.kt`, `build-logic/convention/build.gradle.kts`, `build-logic/settings.gradle.kts`: không sửa. `kotlin-dsl` và `JvmTarget.JVM_17` của `build-logic` chạy trên Gradle 9.3.1 (lần thử).

**5. `gradle.properties`:** không sửa ở phương án chính. KSP 2.3.12 chạy với Kotlin dựng sẵn mà không cần `android.disallowKotlinSourceSets=false`, và Roborazzi 1.77 chạy với DSL mới nên không cần `android.newDsl=false`. Phương án dự phòng (mục 3.4) thêm `android.newDsl=false`.

**6. `scripts/java-check.sh`:** bảng `max_java_for_gradle`, dòng của Gradle 9:

```sh
    9)
      if [ "$minor" -ge 4 ]; then echo ""
      elif [ "$minor" -ge 1 ]; then echo 25
      else echo 24
      fi ;;
```

Theo bảng tương thích của Gradle 9.3.1: Gradle 9.0 chạy tới Java 24, Gradle 9.1 trở lên chạy tới Java 25, Java 26 chưa được hỗ trợ. Gradle mới hơn bảng thì không có giới hạn trên, như chú thích đầu file đã ghi. Không có dòng này thì 5 test có sẵn đỏ ngay khi wrapper lên 9.3.1, vì Java 26 lọt qua (mục 3.5).

**7. `CLAUDE.md`, mục Công nghệ:** "minSdk 26, compileSdk 37, targetSdk 36. Phiên bản thư viện nằm ở `gradle/libs.versions.toml`; bộ công cụ build ở ADR-0040."

**8. ADR:** ADR-0040 (`docs/adr/0040-bo-build-agp-9.md`) đã viết trong PR plan này, trạng thái `proposed`, cùng `docs/foundation.md` sinh lại. Người duyệt plan ghi `accepted` và chạy lại `scripts/foundation.py` khi duyệt. Phiên không sửa ADR-0040. Phiên thêm vào `docs/adr/0011-toolchain-and-code-checks.md`, ngay dưới tiêu đề, một dòng ghi chú như ADR-0010 có với ADR-0025 (mục 8, câu 6), không đổi nội dung nào khác:

```markdown
> Ghi chú 2026-10-30: [ADR-0040](0040-bo-build-agp-9.md) thay phần Phiên bản (Gradle 9.3.1, AGP 9.1.0 với Kotlin dựng sẵn, compileSdk 37, targetSdk 36). Các quyết định còn lại của ADR-0011 giữ nguyên.
```

Ngày trong dòng là ngày phiên làm task. Rồi chạy `scripts/foundation.py`: dòng ghi chú không nằm trong mục Việc AI phải tuân thủ nên `docs/foundation.md` không đổi; đổi thì commit cùng PR.

**9. `scripts/tests/run.sh`:**
- Test mới ở mục 4, ngay sau khối `java-check` có sẵn. Hàm `java_check_gradle` chép `java-check.sh` vào một thư mục tạm có wrapper giả, nên test không phụ thuộc phiên bản Gradle của repo và đỏ được ngay trong commit test, khi wrapper vẫn là 8.14.3.
- Fixture `sdk-ok` của khối `cloud-env-check.sh` giữ `platforms/android-$compile_sdk` (T-079 đã cho `cloud-env-check.sh` nhận cả `android-37` lẫn `android-37.0`; test `passes for compileSdk 37 with platforms/android-37.0` của T-079 đã kiểm dạng `.0`). Không sửa.
- Test có sẵn `java-check: Java 26 is too new for Gradle 8.14` chỉ chạy khi wrapper là Gradle 8.14 trở lên của nhánh 8, nên sẽ không chạy sau khi nâng. Test mới `Java 26 is too new for Gradle 9.3.1` kiểm cùng ý định cho bản mới. Không xóa test cũ.

**10. Ticket:** `T-080-bo-build-agp-9.md`: `plan:`, trạng thái theo plan, và theo phương án chính sửa Mô tả ("Robolectric và Roborazzi giữ bản cũ" thành "Robolectric giữ bản cũ; Roborazzi lên 1.77.0 vì bản 1.43 cần DSL cũ của AGP") và dòng Không bao gồm ("Robolectric và ghi lại ảnh chuẩn (T-081)"). Theo phương án dự phòng, chỉ đổi `plan:` và trạng thái. `T-081-robolectric-roborazzi-anh-chuan.md`: theo phương án chính, phần Phạm vi bỏ Roborazzi (đã lên 1.77.0 ở T-080), còn Robolectric 4.17 và ghi lại ảnh chuẩn; tiêu đề đổi thành "Robolectric 4.17 và ghi lại ảnh chuẩn screenshot". Theo phương án dự phòng, T-081 thêm việc gỡ `android.newDsl=false` khi nâng Roborazzi.

Không đổi: `scripts/cloud-setup.sh` (giữ `platforms;android-36` và `platforms;android-37.0`, `SETUP_VERSION=5`; init script `maven-central-mirror.gradle` chạy được trên Gradle 9.3.1, mục 4.1), `scripts/cloud-env-check.sh`, `scripts/local-setup.sh`, `.github/workflows/`, `scripts/check.sh`, file build của từng module (`app/build.gradle.kts`, `core/*/build.gradle.kts`, `feature/*/build.gradle.kts`: biên dịch được trên AGP 9 với DSL mới, không cảnh báo, mục 4.1), `settings.gradle.kts`, `robolectric.properties` (`sdk=35`), ảnh chuẩn.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. compileSdk 37, AGP 9.1.0, KSP 2.3.12, ktlint-gradle 14.2.0, Navigation 2.9.8, Hilt 2.59.2, Roborazzi 1.77.0; xóa plugin `kotlin-android` (mục 3.1, bước 1) |
| Sửa | `gradle/wrapper/gradle-wrapper.properties` | `gradle-9.3.1-bin.zip` |
| Sửa | `gradle/wrapper/gradle-wrapper.jar` | Sinh bằng `./gradlew wrapper --gradle-version 9.3.1` |
| Sửa | `gradlew` | Sinh bằng lệnh trên |
| Sửa | `gradlew.bat` | Sinh bằng lệnh trên |
| Sửa | `build.gradle.kts` | Xóa alias `kotlin.android` |
| Sửa | `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` | File được bảo vệ. Xóa dòng áp dụng `org.jetbrains.kotlin.android` |
| Sửa | `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` | File được bảo vệ. Xóa dòng áp dụng `org.jetbrains.kotlin.android` |
| Sửa | `build-logic/convention/src/main/kotlin/KotlinAndroid.kt` | File được bảo vệ. `CommonExtension` của AGP 9, khối thành thuộc tính, `failOnNoDiscoveredTests` cho module không có `src/test` (mục 3.1, bước 4) |
| Sửa | `gradle.properties` | (dự phòng) Thêm `android.newDsl=false` kèm chú thích, chỉ ở phương án dự phòng (mục 3.4) |
| Sửa | `scripts/java-check.sh` | Giới hạn Java của Gradle 9.0 và 9.1 tới 9.3 (mục 3.1, bước 6) |
| Sửa | `scripts/tests/run.sh` | Hàm `java_check_gradle` và 5 test ở mục 4 |
| Sửa | `CLAUDE.md` | Mục Công nghệ: compileSdk 37, targetSdk 36, dẫn ADR-0040 |
| Sửa | `docs/adr/0011-toolchain-and-code-checks.md` | Một dòng ghi chú dưới tiêu đề, dẫn ADR-0040 (mục 3.1, bước 8). Không đổi nội dung |
| Sửa | `docs/foundation.md` | Chỉ khi `scripts/foundation.py` sinh ra khác (mục 3.1, bước 8) |
| Sửa | `docs/features/quy-trinh/tickets/T-080-bo-build-agp-9.md` | `plan:`, trạng thái theo plan |
| Sửa | `docs/features/quy-trinh/tickets/T-081-robolectric-roborazzi-anh-chuan.md` | Phạm vi và tiêu đề theo phương án được chọn (mục 3.1, bước 10) |

Các file đọc file plan sửa (L-020; `grep -rn` trong `scripts/`, `.github/`, `docs/`, `CLAUDE.md`, `build-logic/`, mọi `src/test`, 2026-10-30):
- `gradle/libs.versions.toml` được đọc bởi `build-logic/settings.gradle.kts` (nạp làm catalog; plan không đổi tên khóa nào mà `build-logic` dùng, chỉ xóa plugin `kotlin-android` mà `build-logic` không gọi qua catalog), `build.gradle.kts` gốc (alias plugin, bước 3), `scripts/cloud-env-check.sh` và `scripts/tests/run.sh` (dòng `compileSdk = "<số>"`, giữ dạng), `scripts/plan-check.py` (chỉ dùng tên file để dò thư viện mới và đường dẫn rủi ro cao). `.github/workflows/pr-check.yml` chỉ có tên file trong bộ lọc đường dẫn (PR này chạy cả build release).
- `gradle/wrapper/gradle-wrapper.properties` được đọc bởi `scripts/java-check.sh` (dòng `distributionUrl`, giữ dạng) và `scripts/tests/run.sh` (test `java-check` có sẵn đọc số phụ của Gradle).
- `scripts/java-check.sh` được gọi bởi `scripts/check.sh`, `scripts/cloud-env-check.sh` (`JAVA_CHECK_CMD`) và `scripts/local-setup.sh`. Ba script chỉ dùng mã thoát và thông báo; test của chúng ở mục 3.5.
- `gradle.properties` có trong bộ lọc đường dẫn của `pr-check.yml`; `scripts/cloud-setup.sh` chỉ nhắc tới nó trong chú thích (ghi vào `~/.gradle/gradle.properties`, không phải file của repo).
- `build-logic/` được dùng bởi mọi module qua plugin `template.android.*`. Hai plan quảng cáo cũng sửa file build (mục 6).

- `CLAUDE.md` được `scripts/new-app.sh` sửa khi tạo app mới, nhưng chỉ câu mở đầu ("Đây là dự án mẫu cho các app Android của nhóm..."); plan chỉ đổi dòng của mục Công nghệ, nên lệnh thay của script vẫn khớp.
- `build.gradle.kts`: `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` đọc `File("build.gradle.kts")` từ thư mục của `:app`, tức `app/build.gradle.kts`, không phải file gốc mà plan sửa.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay danh sách chọn, không đọc nội dung bị đổi: `scripts/run-watchdog.py` (lời nhắc phiên đọc `CLAUDE.md`), `.github/ISSUE_TEMPLATE/from-app.yml` (mục chọn "docs/, CLAUDE.md"), `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt` và `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt` (KDoc dẫn bước 10 của `CLAUDE.md`), `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`, `docs/ops/staging-and-runs.md`, `docs/ops/to-app.md`, các plan và ticket cũ.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm. Thư viện quảng cáo và mediation giữ bản cũ.
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền. Manifest gộp của `stagingDebug` và `stagingRelease` giống hệt trước và sau khi nâng (mục 4.1).
- **Thư viện và SDK:** Không thêm thư viện. Nâng các thư viện và plugin đã có trong `gradle/libs.versions.toml`: `com.android.tools.build:gradle` 8.13.0 lên 9.1.0 (plugin `android-application`, `android-library`, thư viện `android-gradlePlugin` của `build-logic`), `com.google.devtools.ksp` 2.2.10-2.0.2 lên 2.3.12, `org.jlleitschuh.gradle.ktlint` 13.0.0 lên 14.2.0, `androidx.navigation:navigation-compose` 2.9.3 lên 2.9.8 (module feature), `com.google.dagger:hilt-android` và `hilt-android-compiler` 2.57.1 lên 2.59.2 (plugin `template.hilt`), `io.github.takahirom.roborazzi` 1.43.0 lên 1.77.0 (`roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule`, plugin Gradle; chỉ ở test). Gradle 8.14.3 lên 9.3.1. Bỏ plugin `org.jetbrains.kotlin.android`. Firebase, quảng cáo, mediation, AppsFlyer, `googleServices` 4.5.0 và `firebaseCrashlyticsGradle` 3.0.8 giữ bản cũ và chạy được trên bộ mới (bản staging release dựng qua, mục 4.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** Roborazzi 1.77.0 trên runner CI. Lần thử chỉ chạy trên máy dev macOS với JDK 21; CI chạy Linux với Java 17 (`setup-java` của `pr-check`, `main-guard`, `staging`, `device-session`). Nếu cách vẽ hay cách so của bản 1.77 khác ở đó, `pr-check` sẽ báo ảnh lệch.
- **Điều kiện chuyển:** (a) lần đầu `scripts/check.sh` hay `pr-check` báo một ảnh chuẩn lệch ở phương án chính: chuyển ngay, không sửa gì, vì ảnh lệch không có gì để sửa ngoài ghi lại ảnh, mà plan cấm; (b) Roborazzi 1.77 lỗi cấu hình hay lỗi chạy (không phải ảnh lệch) sau 2 lần sửa cho cùng một lỗi. Phương án dự phòng cũng báo ảnh lệch thì dừng theo mục 7.
- **Cách làm thay (phương án A của mục 4.1):** giữ `roborazzi = "1.43.0"`, thêm vào `gradle.properties`:

  ```properties
  # Roborazzi 1.43 needs the old Android DSL types (TestedExtension). T-081 upgrades Roborazzi and removes this line;
  # AGP 10 drops the option.
  android.newDsl=false
  ```

  Mọi phần khác của mục 3.1 giữ nguyên. Đã thử: `check.sh` qua, 1455 test, 89 ảnh; mỗi file build của module in một cảnh báo Kotlin rằng `android { }` kiểu cũ sắp bỏ, và AGP in "The option setting 'android.newDsl=false' is deprecated". Ticket T-081 ghi thêm việc gỡ dòng này. Tiêu chí và test ở mục 4 không đổi.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` chỉ dò test của code Kotlin trong module app và core; `build-logic` không có test riêng. Mọi test JVM của các module là test hồi quy của plan: chúng phải qua nguyên vẹn trên bộ mới. Hành vi của build và script bị đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `java-check.sh` không có giới hạn Java trên với Gradle 9.1 trở lên | Gradle 9.1 tới 9.3: tối đa Java 25 | `cloud-env-check.sh stops on Java 26 and names Java`, `names Java 26 in the message`, `points a Java problem at JAVA_TOOL_OPTIONS`; `local-setup.sh stops when java-check.sh refuses the JDK`, `names java-check.sh and the JDK dir to delete` (5 test, dùng `java-check.sh` thật với wrapper của repo) | Giữ cả 5. Trên wrapper 9.3.1 mà không có bước 6 thì cả 5 đỏ (lần thử 1, mục 4.1); có bước 6 thì qua |
| `java-check: Java 26 is too new for Gradle 8.14` chạy | Không chạy nữa (điều kiện Gradle 8) | Chính test đó | Giữ nguyên. Ý định của nó được test mới `Java 26 is too new for Gradle 9.3.1` kiểm cho bản mới |
| Module không có test chạy task test mà không báo gì | Như cũ, nhưng nhờ `failOnNoDiscoveredTests` chỉ ở module không có `src/test` | Không có test; `check.sh` in `./core/data: 0 test, 0 lỗi` | Kiểm ở mục 4 bằng dòng tóm tắt của `check.sh` |
| Ảnh chuẩn so bằng Roborazzi 1.43 | So bằng Roborazzi 1.77.0 | 89 ảnh chuẩn của mọi module | Giữ: không ảnh nào đổi. Đối chứng ở mục 4.1: ảnh hỏng làm test đỏ |

Không có câu khẳng định nào của test cũ bị viết lại (ADR-0031).

## 4. Kiểm thử

Bộ build không có test đơn vị riêng: bằng chứng là toàn bộ cổng chất lượng chạy trên bộ mới, cộng các test script của `java-check.sh`. Test mới ở `scripts/tests/run.sh`, ngay sau khối `java-check` có sẵn; hàm `java_check_gradle <bản Gradle> <bản Java>` tạo thư mục có `scripts/java-check.sh` chép từ repo, `gradle/wrapper/gradle-wrapper.properties` với `distributionUrl=...gradle-<bản>-bin.zip` và `build-logic/KotlinAndroid.kt` chứa `JavaVersion.VERSION_17`, rồi chạy script với JDK giả của hàm `java_check` có sẵn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Gradle 9.3 nhận Java 25 | Test script | `java-check: Gradle 9.3.1 runs on Java 25`: mã 0 (đối chứng, qua cả trước khi sửa) |
| Java quá mới bị chặn với Gradle 9.3 | Test script | `java-check: Java 26 is too new for Gradle 9.3.1`: mã 1. Trước khi sửa: 0 |
| Thông báo nêu bản Gradle và khoảng Java | Test script | `java-check: names Gradle 9.3.1 and Java 17 to 25`: có đúng một dòng khớp `^LỖI: Java 26 quá mới .*Gradle 9.3.1 chỉ chạy trên Java 17 đến 25.$`. Trước khi sửa: 0 dòng |
| Gradle 9.0 giữ giới hạn Java 24 | Test script | `java-check: Java 25 is too new for Gradle 9.0`: mã 1 (đối chứng, qua cả trước khi sửa) |
| Gradle mới hơn bảng không có giới hạn trên | Test script | `java-check: a Gradle newer than the table has no upper bound`: Gradle 9.4.0, Java 26: mã 0 (đối chứng) |
| Java quá mới bị chặn trên wrapper thật của repo | Test script | 5 test có sẵn của mục 3.5, chạy với wrapper 9.3.1 sau bước 2 |
| Mọi module build, mọi test qua, không ảnh nào đổi | Cổng chất lượng | `scripts/check.sh`: in `== Cổng chất lượng: QUA`; dòng `Test JVM:` có ít nhất 1455 test, 0 lỗi, `Ảnh screenshot: 89` (cộng ảnh mà các PR merge trước đó thêm); `git status --short -- '*/src/test/screenshots/'` rỗng |
| Bản release dựng được với R8 | Build | `./gradlew :app:assembleStagingRelease`: `BUILD SUCCESSFUL` |
| Manifest gộp không thêm quyền | So tệp | Trên nhánh chính (trước bước 1) và sau khi nâng: `./gradlew :app:processStagingDebugMainManifest :app:processStagingReleaseMainManifest`, chép `app/build/intermediates/merged_manifest/<biến thể>/*/AndroidManifest.xml` ra `${TMPDIR:-/tmp}/t080-before-<biến thể>.xml` và `t080-after-<biến thể>.xml`; `diff <(grep -o 'uses-permission[^>]*' before | sort) <(grep -o 'uses-permission[^>]*' after | sort)` rỗng cho cả hai biến thể |
| ktlint mới không làm đổi code ngoài ý muốn | Lệnh | `./gradlew ktlintFormat` rồi `git status --short -- '*.kt' '*.kts'`: chỉ còn file của mục 3.2. File nào khác đổi thì liệt kê trong nhật ký và mô tả PR (định nghĩa hoàn thành 3) |
| Luật dòng dài của T-050 giữ trên ktlint mới | Cổng chất lượng | `ktlintCheck` trong `check.sh` qua, trong khi repo có sẵn các dòng chỉ có chú thích dài hơn 120 ký tự trong `.kt` và `.kts` (đếm bằng `grep -rnE '^\s*(//\|\*).{117,}' --include='*.kt' --include='*.kts' .`, khác 0): ktlint mới vẫn không kiểm độ dài dòng chú thích |
| Init script của môi trường cloud chạy trên Gradle 9.3.1 | Lệnh | `sed -n "/^cat > .*maven-central-mirror.gradle.*<< 'GRADLE'\$/,/^GRADLE\$/p" scripts/cloud-setup.sh \| sed '1d;\$d' > "${TMPDIR:-/tmp}/mirror.gradle"`, rồi `./gradlew -I "${TMPDIR:-/tmp}/mirror.gradle" :core:logging:testDebugUnitTest --rerun`: `BUILD SUCCESSFUL` |
| CI dựng được trên bộ mới | Kiểm tra thủ công (người: GitHub Actions) | `pr-check` của PR xanh, gồm build release và test trên emulator |
| Môi trường cloud chạy được bộ mới | Kiểm tra thủ công (người: phiên cloud có người, sau khi merge) | Mở một phiên cloud mới trên nhánh chính, chạy `scripts/cloud-env-check.sh`: mã 0, dòng qua có `platforms/android-37.0`. Rồi `scripts/check.sh`: QUA. Đây là lần đầu Gradle 9.3.1 được tải qua proxy của môi trường (Photune đã tải được qua môi trường của nó) |
| Bản staging chạy được | Kiểm tra thủ công (máy thật: khác, bản staging mới cài từ Firebase App Distribution) | Sau khi merge, chạy tay **Actions > staging > Run workflow**: xanh. Cài bản staging lên máy thật: app mở, qua màn hình loading, hộp thoại đồng ý quảng cáo hiện, màn hình sample hiện |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** nâng phiên bản trong `gradle/libs.versions.toml`, wrapper, `build.gradle.kts`, `build-logic/`, `CLAUDE.md`, dòng ghi chú của ADR-0011 và ticket. Chúng không có test riêng có thể đỏ trước; bằng chứng là cổng chất lượng ở trên. Test viết trước là 5 test `java-check` mới; 2 test đỏ trước khi sửa, 3 test đối chứng qua, đi trong commit test của bước 3 khi wrapper còn là 8.14.3.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, trên máy dev (macOS, JDK 21.0.13 của `scripts/local-setup.sh`, SDK `~/android-sdk` có `platforms/android-36` và `platforms/android-37.0`), nhánh thử `claude/trial-T-080` từ main `07780cb`, chưa đẩy. Commit `dd9be5f` (phương án A), `9fb626e` (phương án B), `5b76e98` (test `java-check`).
- **Lệnh:** `scripts/check.sh`; `./gradlew :app:assembleStagingRelease`; `./gradlew :app:processStagingDebugMainManifest :app:processStagingReleaseMainManifest` trước và sau; `./gradlew -I <init script của cloud-setup.sh> --warning-mode all :core:logging:testDebugUnitTest --rerun`; `./gradlew help --warning-mode all`; `./gradlew ktlintFormat`.
- **Kết quả:**
  1. Đổi phiên bản và `CommonExtension<*, *, *, *, *, *>` thành `CommonExtension`: `build-logic` không biên dịch được, `Unresolved reference 'defaultConfig'` (và `compileOptions`, `testOptions`, `packaging`). `javap` trên `gradle-api-9.1.0.jar` cho thấy `CommonExtension` của AGP 9 chỉ còn thuộc tính. Đổi sang `.apply { }`: biên dịch được.
  2. Roborazzi 1.43.0: `Failed to apply plugin 'io.github.takahirom.roborazzi'`, `Extension of type 'TestedExtension' does not exist`. Với `android.newDsl=false` thì qua (phương án A).
  3. `check.sh` lần 1 (phương án A): 5 test script đỏ (mục 3.5), vì `java-check.sh` không có giới hạn trên cho Gradle 9.3. Thêm bước 6 thì `run.sh` qua.
  4. `check.sh` lần 2: `:core:data:testDebugUnitTest` đỏ, "There are test sources present and no filters are applied, but the test task did not discover any tests to execute". `:core:data` không có `src/test`. Thêm `failOnNoDiscoveredTests` cho module không có `src/test`.
  5. `check.sh` lần 3 (phương án A): QUA. `Test JVM: 1455 test, 0 lỗi. Ảnh screenshot: 89`, bằng đúng `main-guard` xanh gần nhất trên main (lần chạy 40914384117). Không ảnh nào đổi. Có 11 cảnh báo Kotlin `'fun Project.android(...)' is deprecated` (mỗi file build module một cảnh báo) và cảnh báo của AGP về `android.newDsl=false`.
  6. `assembleStagingRelease` (phương án A): `BUILD SUCCESSFUL`. Manifest gộp `stagingDebug` và `stagingRelease` giống hệt bản của main, từng byte (`diff` rỗng).
  7. Phương án B (Roborazzi 1.77.0, bỏ `android.newDsl=false`), chạy lại trọn một lần trên `HEAD` của nhánh thử (`5b76e98`, gồm cả commit test) với log lưu riêng từng bước, 2026-10-30 00:48 tới 00:55 UTC: `check.sh` QUA, `Test JVM: 1455 test, 0 lỗi. Ảnh screenshot: 89`, `git status --short -- '*/src/test/screenshots/'` rỗng; `assembleStagingRelease` `BUILD SUCCESSFUL`; manifest gộp `stagingDebug` và `stagingRelease` giống hệt bản của main từng byte (`cmp`); không log nào có `newDsl`; chỉ còn 2 cảnh báo cũ ở `AppsFlyerEventsTest.kt` (`Locale` kiểu cũ, có trên main).
  8. Init script `maven-central-mirror.gradle` của `cloud-setup.sh` (tách bằng lệnh `sed` của mục 4) chạy trên Gradle 9.3.1 ở cả hai phương án: `./gradlew -I <tệp> --warning-mode all :core:logging:testDebugUnitTest --rerun` `BUILD SUCCESSFUL`. `./gradlew help --warning-mode all` ở phương án B: 0 dòng `deprecat`.
  9. `./gradlew ktlintFormat` với ktlint-gradle 14.2.0: không đổi file nào. Cấu hình `ktlint` của `:core:logging` phân giải ra engine ktlint 1.5.0, đúng bản mà luật dòng dài của T-050 dựa vào (`.editorconfig`).
  10. Cảnh báo của AGP còn lại ở cả hai phương án: "We recommend using a newer Android Gradle plugin to use compile SDK version 37.0 ... This Android Gradle plugin (9.1.0) was tested up to compile SDK version 36.1" (mục 8, câu 2).
  11. Test `java_check_gradle` (mục 4) chạy trên `java-check.sh` của main: `Java 26 is too new for Gradle 9.3.1` và `names Gradle 9.3.1 and Java 17 to 25` đỏ, ba đối chứng qua; trên `java-check.sh` mới thì cả 5 qua.
  12. Đối chứng ảnh lệch nhỏ, cả hai bản Roborazzi, bằng đúng task mà `check.sh` dùng (`:feature:sample:verifyRoborazziDebug :feature:sample:testDebugUnitTest --rerun`): đảo màu một khối 4 x 4 (16 điểm ảnh, 0,0005% của ảnh 3.295.809 điểm) hay 181 x 181 (32.761 điểm ảnh, khoảng 1%) giữa `SampleScreen_ad-loading_light.png`. Roborazzi 1.77.0 (`5b76e98`) và 1.43.0 (`dd9be5f`): cả bốn lần đều đỏ, "27 tests completed, 1 failed"; trả ảnh về thì `git status` rỗng. Hai bản có cùng độ nhạy ở mức này. Chỉ chạy `testDebugUnitTest` (không có task `verify`) thì cả bốn lần qua, vì khi đó Roborazzi không so ảnh: đó là lý do lệnh đối chứng phải gồm `verifyRoborazziDebug`.
- **Chưa thử:** runner Linux của CI và môi trường cloud (mục 4, kiểm tay); bản production (cần tham số thật, ADR-0013).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (cấu hình build: `build-logic/`, `gradle/libs.versions.toml`, wrapper, mọi module). Điểm 2 (SDK) tính là chạm vì plan đổi phiên bản trong `gradle/libs.versions.toml` (`scripts/plan-check.py` cũng tính vậy): không thêm SDK nào, thư viện chạy trong app chỉ lên bản phụ (Navigation 2.9.3 lên 2.9.8, Hilt 2.57.1 lên 2.59.2); AGP, Gradle, KSP, ktlint, Roborazzi là công cụ build và test, không vào APK. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Mọi module qua `build-logic` và catalog |
| B. Độ rõ của yêu cầu | 2 | Ticket chốt các bản, nhưng mục 8 còn câu hỏi mở đổi cách làm: câu 1 (Roborazzi ở ticket nào: khác file và phiên bản) và câu 2 (AGP 9.1 hay 9.5) |
| C. Khả năng kiểm chứng tự động | 1 | Cổng chất lượng, build release và test script kiểm phần lớn; CI Linux, môi trường cloud và bản staging trên máy kiểm tay |
| D. Ảnh hưởng chéo | 3 | Đổi bộ build mà mọi module và mọi plan dựa vào; các plan quảng cáo đã duyệt chạy thử trên AGP 8.13; T-068 cũng sửa `gradle/libs.versions.toml` |
| E. Độ mới của kỹ thuật | 2 | AGP 9 có Kotlin dựng sẵn, DSL mới: chưa có trong dự án, đã chạy ở Photune (một module) |
| **Tổng** | 11 | **Mức: Cao (điểm chặn 2, 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `gradle/libs.versions.toml`, `build-logic/`, wrapper, `build.gradle.kts`, `scripts/java-check.sh`.
- Task khác có thể xung đột:
  - T-068 (SDK AppLovin MAX, plan đã duyệt, PR #307 chưa merge) thêm hai phiên bản và hai thư viện vào nhóm `# Ads` của `gradle/libs.versions.toml`, ở dòng khác với plan này: git gộp được. Nhưng phần chạy thử của T-068 làm trên AGP 8.13. Chạy T-068 sau T-080 thì SDK MAX được kiểm trên bộ mới ngay trong task của nó.
  - T-058 (máy thử, plan đã duyệt, PR #303) sửa `app/build.gradle.kts`; plan này không sửa file đó.
  - Các plan quảng cáo khác đã duyệt (T-055, T-059, T-060, T-062, T-063) chỉ nhắc `gradle/libs.versions.toml` để ghi thư viện đã có, không sửa nó.
  - Phần chạy thử (`check.sh`, Roborazzi 1.43) của mọi plan quảng cáo đã duyệt (T-053, T-054, T-055, T-058, T-059, T-060, T-062, T-063, T-066, T-067, T-068) làm trên AGP 8.13. Chạy sau T-080 thì mỗi task tự kiểm lại trên bộ mới ở bước 5 của nó.
  - PR #312 (T-074) sửa `.github/workflows/`; plan này không sửa workflow. Sau khi T-074 merge, `main-guard` có thể không chạy lại `check.sh` cho commit merge của T-080 (ADR-0039, cách `skip`): khi đó `main-guard` xanh không thêm bằng chứng nào ngoài `pr-check`.
- Các app nhận qua `scripts/template-sync.sh`: nhận cả bộ build mới. App có module hay thư viện riêng phải tự dựng lại; camera-pose do phiên khác lo. Photune đã ở AGP 9.1, Gradle 9.3.1 nên gần mẫu hơn.
- Đề xuất thứ tự chạy: làm trong phiên có người theo dõi, trước khi có danh sách chạy gồm các plan quảng cáo, nhất là T-068. Sau đó tới T-081.

## 7. Điều kiện dừng riêng

- Một ảnh chuẩn nào đổi ở cả hai phương án: dừng, không ghi lại ảnh (ảnh đổi là việc của T-081).
- Số test JVM của `check.sh` ít hơn số của `main-guard` gần nhất trên nhánh chính: dừng, tìm test bị mất.
- Manifest gộp có quyền mới: dừng.
- Cần sửa file build của một module (`app/build.gradle.kts`, `core/*/build.gradle.kts`, `feature/*/build.gradle.kts`) để build qua: dừng, vì lần thử không cần, nên đó là dấu hiệu bộ build khác lần thử.

## 8. Câu hỏi cho người duyệt

1. Roborazzi ở ticket nào. Đề xuất: nâng Roborazzi 1.77.0 ngay trong T-080 (phương án B). Lý do: Roborazzi 1.43 chỉ chạy với `android.newDsl=false`, cờ mà AGP báo sắp bỏ, kèm 11 cảnh báo trong file build; Roborazzi 1.77 so ảnh như cũ, 89 ảnh vẫn khớp, ảnh hỏng vẫn bị bắt. T-081 còn Robolectric 4.17 và ghi lại ảnh. Nếu anh muốn giữ đúng ticket: phương án A, `android.newDsl=false` ở T-080, T-081 nâng Roborazzi và gỡ cờ.
2. compileSdk 37 với AGP 9.1.0. AGP 9.1.0 chỉ được kiểm tới compileSdk 36.1, và mỗi lần build in một cảnh báo dài. Hiện đã có AGP 9.5.0 bản ổn định (danh sách bản của Google Maven, 2026-10-30). Đề xuất: giữ AGP 9.1.0 và compileSdk 37 như ticket, giống Photune (bản đã chạy thật qua khoảng 15 PR), không tắt cảnh báo; nâng AGP 9.5 là một ticket sau, khi Photune hay mẫu cần. Cách khác: (a) đặt `android.suppressUnsupportedCompileSdk=37.0` để log gọn; (b) nâng thẳng AGP 9.5 trong plan này, nhưng cần chạy thử lại, và Photune chưa dùng bản đó.
3. `failOnNoDiscoveredTests` chỉ cho module không có `src/test` (hiện chỉ `:core:data`). Đề xuất như mục 3.1. Cách khác: thêm một test thật cho `:core:data`; việc đó ngoài phạm vi ticket.
4. Thứ tự với các plan quảng cáo đã duyệt (mục 6). Đề xuất: merge T-080 trước khi lập danh sách chạy có T-068.
5. Bản production chưa được dựng trên bộ mới, vì cần tham số thật (ADR-0013) và plan không chạm cấu hình production. Bản release đầu tiên của một app dựng trên bộ mới sẽ là lần kiểm đầu tiên. Đề xuất: chấp nhận, và ghi việc này vào mục Cần người kiểm tra của nhật ký.
6. Ticket ghi "Không sửa ADR-0011". Đề xuất: không đổi nội dung, chỉ thêm một dòng ghi chú dưới tiêu đề dẫn tới ADR-0040, như ADR-0010 có dòng dẫn ADR-0025, để người đọc ADR-0011 không gặp thông tin cũ ("Chưa dùng AGP 9") mà không biết đã có ADR thay. Không đồng ý thì bỏ dòng ADR-0011 khỏi mục 3.2 và bước 8.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
