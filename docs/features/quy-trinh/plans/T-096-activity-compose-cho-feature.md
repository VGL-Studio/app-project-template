---
ticket: T-096
title: Plugin feature có activity-compose trên classpath biên dịch, để feature dùng được LocalActivity với AGP 9
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7
risk_level: high
token_estimate: 8.6M
blocking_points: [build]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-096: PLUGIN FEATURE CÓ ACTIVITY-COMPOSE TRÊN CLASSPATH BIÊN DỊCH, ĐỂ FEATURE DÙNG ĐƯỢC LOCALACTIVITY VỚI AGP 9

## 1. Tóm tắt

Convention plugin `template.android.feature` (`AndroidFeatureConventionPlugin.kt`) thêm một dòng `implementation` cho `androidx-activity-compose`, thư viện đã có trong `gradle/libs.versions.toml` và đã là phụ thuộc của `:core:ads` và `:app`. Test viết trước là một test Robolectric của `:feature:sample` đọc `LocalActivity.current`: trên nhánh chính test không biên dịch được (`Unresolved reference 'LocalActivity'`, đã đo ở mục 4.1), thêm dòng của plugin thì qua. Danh sách quyền của manifest gộp và tập thư viện runtime của bản release không đổi (đã đo).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn là issue #369 (camera-pose) và bài học L-160, `accepted` ngày 2026-11-09 (lời nhận ghi trong mô tả PR #432). Anh Nhật giao viết plan ngày 2026-11-10: "Viết plan cho cac ticket T-096 tới T-099".

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 1: plugin thêm `activity-compose` | Mọi module áp plugin `template.android.feature` biên dịch được code dùng `androidx.activity.compose.LocalActivity` | Một dòng `add("implementation", ...)` với khóa catalog `androidx-activity-compose` trong khối `dependencies` của plugin, cạnh các thư viện AndroidX khác. Cùng cấu hình `implementation` như `:core:ads` (`core/ads/build.gradle.kts`, dòng 41), không dùng `api` |
| Phạm vi 2: test chứng minh, có đối chứng đỏ | Test viết trước thất bại đúng lý do trên nhánh chính, qua sau khi thêm dòng | Chọn cách "một test của `:feature:sample` dùng `LocalActivity`", không chọn chỗ dùng tạm: test ở lại repo nên giữ hành vi cho mọi lần đổi plugin sau. Đã đo: `testImplementation` của feature cũng không có `activity-compose`, nên test trong `src/test` đỏ đúng lý do, không cần thêm code vào `src/main` của feature mẫu (mục 4.1) |
| Phạm vi 3: quyền và thư viện | So manifest gộp trước và sau | Đã so: 11 dòng `uses-permission` giống hệt; tập thư viện của `:app` cấu hình `stagingReleaseRuntimeClasspath` giống hệt (288 tọa độ, không thêm không bớt), vì `:app` đã có `activity-compose:1.10.1` (mục 4.1) |
| Định nghĩa hoàn thành: bài học | L-160 `applied` với số PR | Chỗ ghi tùy cách làm (mục 8, câu 2 và 3). Làm có người: trong PR duy nhất của task, như T-100. Phiên tự chạy: trong PR báo cáo cuối phiên, sau khi PR của task đã merge, vì PR của task không chứa tài liệu (`auto-run.md`, bước 7) và bài học chỉ chuyển `applied` khi PR cải tiến đã merge (`auto-run.md`, mục 6, ý 3; `docs/ops/from-app.md`, bước 4). Đóng issue #369 là việc của dev sau khi đồng ý, không thuộc task |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan:
- ADR-0001: feature chỉ phụ thuộc core. `activity-compose` là thư viện ngoài, không phải module, nên `checkModuleDependencies` không đổi.
- ADR-0047: bộ công cụ build (AGP 9). Theo L-160, lỗi lộ ra ở app sau khi lên bộ build AGP 9. Plan không dựa vào nguyên nhân đó: quy tắc mới là feature khai báo thẳng `activity-compose`, không trông vào thư viện khác mang nó tới.

## 3. Giải pháp

### 3.1. Cách làm

`build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt`, khối `dependencies`, thêm một dòng ngay trước dòng `androidx-lifecycle-runtime-compose`, chép đúng dạng dòng đó và chỉ đổi khóa catalog thành `androidx-activity-compose` (đã chạy thử đúng dòng này, mục 4.1). Dòng mới tương đương `implementation(libs.androidx.activity.compose)` của `core/ads/build.gradle.kts`.

KDoc của lớp thêm một câu tiếng Anh: "Features declare activity-compose directly so `LocalActivity` resolves; do not rely on it arriving transitively." Phiên bản đi theo `androidxActivity = "1.10.1"` của `gradle/libs.versions.toml`, không đổi tệp đó.

Test viết trước: `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/FeatureClasspathTest.kt`, chạy bằng Robolectric như `SampleScreenTest`:

```kotlin
/**
 * Keeps the `implementation` declaration of activity-compose in the feature convention plugin:
 * test sources see `implementation` dependencies, so this file stops compiling when the line is removed.
 */
@RunWith(AndroidJUnit4::class)
class FeatureClasspathTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localActivity_resolvesHostActivity() {
        var activity: Activity? = null
        composeRule.setContent { activity = LocalActivity.current }
        composeRule.waitForIdle()
        assertNotNull(activity)
    }
}
```

Import: `android.app.Activity`, `androidx.activity.compose.LocalActivity`, `androidx.compose.ui.test.junit4.createComposeRule`, `androidx.test.ext.junit.runners.AndroidJUnit4`, `org.junit.Assert.assertNotNull`, `org.junit.Rule`, `org.junit.Test`, `org.junit.runner.RunWith`. Không chụp ảnh, không thêm ảnh chuẩn.

Commit theo bước 3 và 4 của `auto-run.md`: commit test (đỏ vì không biên dịch được), rồi commit plugin.

Tài liệu đi theo cách làm (mục 8, câu 2):
- Làm có người (`auto_run: false`): một PR cho cả task, như T-100. PR chứa test, plugin, L-160 `applied`, nhật ký plan, plan `done` và ticket.
- Phiên tự chạy: PR của task chỉ có test và plugin. Nhật ký và trạng thái plan, trạng thái ticket đi qua PR báo cáo cuối phiên (`auto-run.md`, bước 7 và mục 4). L-160 `applied` kèm số PR của task cũng ghi trong PR báo cáo, sau khi PR của task đã merge.

Không đổi `CLAUDE.md` (bước 1 của "Viết một feature mới" vẫn đúng: `build.gradle.kts` chỉ áp plugin). Không ghi vào `docs/ops/to-app.md`: app nhận dòng mới qua `scripts/template-sync.sh`, vì `build-logic/` là phần "của dự án mẫu, app không sửa".

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` | Mục 3.1: một dòng `implementation` cho `androidx-activity-compose`, một câu KDoc |
| Tạo | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/FeatureClasspathTest.kt` | Test viết trước của mục 4 |
| Sửa | `docs/lessons.md` | L-160: `applied`, dòng Áp dụng ghi số PR. Làm có người: trong PR của task. Phiên tự chạy: trong PR báo cáo, sau khi PR của task merge |
| Sửa | `docs/features/quy-trinh/plans/T-096-activity-compose-cho-feature.md` | Mục 10, nhật ký thực hiện, trạng thái plan. Làm có người: lần đẩy cuối của PR task. Phiên tự chạy: PR báo cáo |
| Sửa | `docs/features/quy-trinh/tickets/T-096-activity-compose-cho-feature.md` | Trạng thái ticket theo kết quả. Làm có người: PR của task. Phiên tự chạy: PR báo cáo |

Nơi đọc file bị đổi: plugin được mọi module có `alias(libs.plugins.template.android.feature)` áp dụng, hôm nay là `feature/sample/build.gradle.kts` và `feature/loading/build.gradle.kts`. Cả hai nhận thêm `activity-compose` trên classpath biên dịch, không sửa tệp build của chúng. `.github/workflows/pr-check.yml` (dòng 54, 68) dò `build-logic/**` để quyết định build bản release trong `pr-check`; không sửa, PR của task sẽ chạy bước đó.

Các file khác mà `plan-check.py` (L-020) có thể thấy nhắc tới file của plan; không file nào bị ảnh hưởng, không sửa:
- Nhắc `AndroidFeatureConventionPlugin.kt` hay plugin `template.android.feature`: `CLAUDE.md` (bước 1 của "Viết một feature mới"), `docs/features/loading/plans/T-001-man-hinh-loading.md`, `docs/features/quy-trinh/plans/T-080-bo-build-agp-9.md`. Chỉ nhắc tên plugin hay nói không sửa; plan không đổi tên plugin hay tên tệp.
- Nhắc `docs/lessons.md`:
  - `.github/ISSUE_TEMPLATE/from-app.yml`, `scripts/new-app.sh` dẫn cả file;
  - `scripts/status.py` đọc trạng thái bài học, `applied` là trạng thái nó đã biết;
  - `scripts/history-refs.py` coi `docs/lessons.md` là chỗ dành cho lịch sử và bỏ qua nó, nên dòng Áp dụng ghi mã ticket và số PR không bị báo;
  - `scripts/tests/run.sh` chỉ dùng tên file trong dữ liệu giả (danh sách file của PR, sổ bài học của app mới, của `template-sync.sh`, của `status.py` trong thư mục tạm), không đọc nội dung `docs/lessons.md` của repo.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. `LocalActivity` chỉ đọc activity đang chứa composition, không cần quyền nào từ minSdk 26 tới targetSdk 36. Manifest gộp của `:app` (`processStagingDebugMainManifest`) có cùng 11 dòng `uses-permission` trước và sau khi thêm dòng (mục 4.1).
- **Thư viện và SDK:** Không thêm thư viện mới. `androidx-activity-compose` (`androidx.activity:activity-compose`, phiên bản `androidxActivity` 1.10.1, Apache 2.0) đã có trong `gradle/libs.versions.toml`; `:core:ads` và `:app` đã khai báo. Plan thêm nó vào cấu hình `implementation` của mọi module feature qua plugin `template.android.feature`. Tập thư viện runtime của bản release `:app` không đổi (mục 4.1), nên không có luật R8 mới.

### 3.4. Phương án dự phòng

Không có. Cả hai lần chạy (đỏ trên nhánh chính, xanh khi có dòng của plugin) đã làm trên máy dev ngày 2026-11-10 (mục 4.1).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` báo 0 test "Cần xem trước", 0 test "Nên xem"; 52 test còn lại chỉ dùng tên mà plan không nêu. Plugin của `build-logic` không có test JVM riêng. Plan chỉ thêm một thư viện vào classpath biên dịch của feature; tập thư viện runtime của `:app` không đổi, nên không hành vi nào mà test có sẵn kiểm bị đổi.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Feature không biên dịch được code dùng `LocalActivity` | Biên dịch được | Không có | Câu mới: `FeatureClasspathTest.localActivity_resolvesHostActivity` |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phạm vi 1 và 2: feature dùng được `LocalActivity` | Compose UI test (Robolectric) | `FeatureClasspathTest.localActivity_resolvesHostActivity` (mục 3.1): `setContent` đọc `LocalActivity.current`, `assertNotNull`. Đỏ trước code, đã đo: task `:feature:sample:compileDebugUnitTestKotlin` thất bại với `Unresolved reference 'LocalActivity'.` ở dòng import và dòng đọc `LocalActivity.current`. Sau khi thêm dòng của plugin: 1 test, 0 lỗi |
| Đối chứng đỏ khi bỏ dòng của plugin | Test viết trước | Chính bước đỏ trước code ở trên: commit test trước commit plugin, ghi lỗi biên dịch vào nhật ký. Không cần test riêng |
| Phạm vi 3: quyền không đổi | So sánh bằng lệnh (phiên chạy) | Ngay trước commit plugin (cây làm việc mới có tệp test, chưa có dòng của plugin; tệp test không vào manifest hay runtime của `:app`), ghi bản gốc; sau commit plugin, ghi bản task; `diff` phải rỗng. Mỗi lần: `./gradlew :app:processStagingDebugMainManifest && grep -o 'uses-permission[^/]*' app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml \| sort > "${TMPDIR:-/tmp}/perm-main.txt"` (lần sau ghi `perm-task.txt`), rồi `diff "${TMPDIR:-/tmp}/perm-main.txt" "${TMPDIR:-/tmp}/perm-task.txt"`. Lúc viết plan: 11 dòng mỗi bên, `diff` rỗng (mục 4.1). Kết quả ghi vào nhật ký |
| Phạm vi 3: thư viện runtime không đổi | So sánh bằng lệnh (phiên chạy) | Cùng hai thời điểm như dòng trên. Mỗi lần: `./gradlew -q :app:dependencies --configuration stagingReleaseRuntimeClasspath \| grep -oE -- '--- .*' \| sed -E 's/^--- //; s/ \((\*\|c\|n)\)$//' \| sort -u > "${TMPDIR:-/tmp}/deps-main.txt"` (lần sau ghi `deps-task.txt`), rồi `diff "${TMPDIR:-/tmp}/deps-main.txt" "${TMPDIR:-/tmp}/deps-task.txt"`. Lúc viết plan: 545 dòng mỗi bên, `diff` rỗng (mục 4.1). Kết quả ghi vào nhật ký |
| Bản release vẫn build | Build | `pr-check` build bản release vì PR đổi `build-logic/**`. Phiên chạy `./gradlew :app:assembleStagingRelease` trước khi đẩy |
| Toàn bộ | Build và test | `scripts/check.sh` (phiên cloud, Linux), rồi `pr-check` của PR. Trên máy dev macOS, 6 test script đỏ sẵn trên nhánh chính làm `check.sh` dừng trước Gradle (mục 4.1): so danh sách `FAIL` với nhánh chính, rồi chạy lệnh Gradle của cổng chất lượng như mục 4.1 |

**Ngoại lệ viết test trước:** câu KDoc của plugin (chỉ chú thích), `docs/lessons.md`, nhật ký plan và ticket (chỉ tài liệu).

### 4.1. Chạy thử

Không cần (E dưới 2): thêm một thư viện đã có vào plugin, như `:core:ads` đã làm. Đã chạy thử để ghi số đo thật cho mục 4, ngày 2026-11-10, trên nhánh chính `88b899a`, máy dev macOS, JDK 21 của `scripts/local-setup.sh`. Mọi thay đổi tạm đã trả lại, không commit.

- **Đã chạy thử:**
  1. Chỗ dùng tạm trong `src/main` của `:feature:sample` (một hàm `@Composable` trả `LocalActivity.current`), chạy `./gradlew :feature:sample:compileDebugKotlin`: thất bại, `Unresolved reference 'LocalActivity'.` hai lần (dòng import, dòng dùng). Tái hiện đúng lỗi của issue #369.
  2. Cùng chỗ dùng tạm chuyển sang `src/test`, chạy `./gradlew :feature:sample:compileDebugUnitTestKotlin`: thất bại cùng lỗi. Vậy các thư viện test (`ui-test-junit4`, `androidx-test-core`) không mang `activity-compose` tới classpath biên dịch của test, và test của mục 3.1 đỏ đúng lý do mà không cần code trong `src/main`.
  3. Test của mục 3.1, trên nhánh chính: `./gradlew :feature:sample:testDebugUnitTest --tests '*FeatureClasspathTest'` thất bại ở `compileDebugUnitTestKotlin`, cùng lỗi.
  4. Thêm dòng của mục 3.1 vào plugin (bằng `perl -pi -e`, đã xem `git diff`), chạy lại lệnh 3: qua, `tests="1" failures="0" errors="0"`.
  5. Manifest gộp: lệnh của dòng "quyền không đổi" ở mục 4, trước và sau dòng của plugin: 11 dòng `uses-permission` mỗi bên, `diff` rỗng.
  6. Thư viện runtime của bản release: lệnh của dòng "thư viện runtime không đổi" ở mục 4, trước và sau: 545 dòng mỗi bên, `diff` rỗng; tính theo tọa độ đã phân giải là 288 trước, 288 sau (`activity-compose:1.10.1` đã có qua `:app`).
  7. `./gradlew :app:assembleStagingRelease` với dòng của plugin: `BUILD SUCCESSFUL in 8m 26s` (R8 chạy, `minifyStagingReleaseWithR8` qua).
  8. `scripts/check.sh` với dòng của plugin và test của mục 3.1: dừng ở bước test của script, 1541 `ok` và 6 `FAIL` đã đỏ sẵn trên nhánh chính khi chạy trên macOS (5 test `pr-check: ... order check`/`filter`, 1 `wait-run: a draft PR stops at once with code 6`), như plan T-100 đã ghi; không test nào liên quan tới plan. Vì `check.sh` dừng trước Gradle, chạy riêng đúng lệnh Gradle của cổng chất lượng trong `check.sh` (`--continue checkModuleDependencies ktlintCheck testDebugUnitTest testStagingDebugUnitTest :feature:sample:verifyRoborazziDebug :feature:loading:verifyRoborazziDebug :app:lintStagingDebug :app:assembleStagingDebug`): `BUILD SUCCESSFUL in 4m 10s`. Ktlint qua với tệp test, ảnh chuẩn của hai feature không đổi. Trên CI (Linux) `check.sh` chạy đủ.
- **Lệnh:** như trên, với `JAVA_HOME` và `ANDROID_HOME` từ `~/.local/share/app-project-template/env.sh`.
- **Kết quả:** lỗi tái hiện được ở cả `src/main` và `src/test`; dòng của plugin sửa được cả hai; quyền và thư viện runtime không đổi. Phương án dự phòng không cần.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (cấu hình build). Plan sửa `build-logic/`, tệp trong mục "Không được làm" của `CLAUDE.md` ngoài plan rủi ro cao đã duyệt chéo. Rủi ro Cao.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module: dòng của plugin áp cho mọi module feature (`:feature:sample`, `:feature:loading` và mọi feature của app), dù chỉ sửa một dòng và thêm một tệp test |
| B. Độ rõ của yêu cầu | 1 | Ticket và bài học nêu rõ cách sửa. Còn chọn cách làm task (mục 8, câu 2) |
| C. Khả năng kiểm chứng tự động | 0 | Test viết trước đỏ đúng lý do và xanh sau khi sửa, đã đo. Quyền và thư viện runtime đã so |
| D. Ảnh hưởng chéo | 3 | Thay đổi điều mà mọi feature đang dựa vào: classpath biên dịch do plugin dựng cho mọi feature của mẫu và của app. Runtime của `:app` không đổi (đã đo), nhưng bảng không có mức cho "sửa file dùng chung mà không task nào khác sửa"; mức 2 đòi task khác cùng sửa file (không có), mức 1 là không sửa thành phần dùng chung (sai), nên chọn 3 thay vì hạ |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng khai báo của `:core:ads` |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `AndroidFeatureConventionPlugin.kt`, áp cho `:feature:sample`, `:feature:loading` và mọi feature của app. Chỉ thêm một thư viện vào classpath biên dịch; runtime của `:app` không đổi (mục 4.1).
- Task khác có thể xung đột: T-097, T-098, T-099 (cùng đợt từ PR #432) sửa `:core:ads` và tài liệu quảng cáo, không sửa `build-logic/` hay `feature/sample/src/test/`. Không plan nào khác đang sửa plugin.
- App nhận dòng mới qua `scripts/template-sync.sh`. App như camera-pose đã tự thêm `activity-compose` vào tệp build của feature thì có thể bỏ dòng đó sau khi đồng bộ; để lại cũng không sao. Ghi điều này vào mô tả PR của task.
- Plan không sửa `.github/workflows/`, nên không cần `push-probe.sh`.
- Đề xuất thứ tự: độc lập, xếp trước các task khác của danh sách được.

## 7. Điều kiện dừng riêng

- Test viết trước qua ngay trên nhánh chính (một thư viện khác đã mang `activity-compose` tới classpath của feature, ví dụ sau khi nâng phiên bản): dừng, plan không còn đúng.
- Một trong hai lệnh `diff` của mục 4 (phạm vi 3: `perm-main.txt`/`perm-task.txt`, `deps-main.txt`/`deps-task.txt`) không rỗng: dừng.
- Cần sửa `gradle/libs.versions.toml` (ví dụ khóa `androidx-activity-compose` không còn): dừng.

## 8. Câu hỏi cho người duyệt

1. **Đặt test ở `:feature:sample` hay chạy thử một lần bằng chỗ dùng tạm.** Ticket để plan chọn. Đề xuất: test `FeatureClasspathTest` ở `:feature:sample`, vì nó ở lại repo và giữ hành vi cho mọi lần đổi plugin hay nâng AGP sau; chỗ dùng tạm chỉ chứng minh một lần. Giới hạn: test chỉ kiểm classpath biên dịch của test. Nó giữ được dòng `implementation` (test thấy phụ thuộc `implementation`), nhưng không bắt được trường hợp ai đó đổi dòng thành `testImplementation`: test vẫn qua mà `src/main` của feature lại không biên dịch được. Chấp nhận được vì `CLAUDE.md` cấm sửa `build-logic/` ngoài plan rủi ro cao. App xóa `:feature:sample` thì mất test này, app vẫn nhận plugin.
2. **Làm có người hay để phiên tự chạy làm.** Ticket không ghi `auto_run`.
   - (a) Làm có người, như T-100: một PR chứa test, plugin, L-160 `applied`, nhật ký plan và ticket; ghi `auto_run: false` vào ticket khi duyệt.
   - (b) Phiên tự chạy: PR của task chỉ có test và plugin; nhật ký, trạng thái plan và ticket, L-160 `applied` đi qua PR báo cáo cuối phiên (`auto-run.md`, bước 7 và mục 4). Plan đã ghi đủ lệnh và điều kiện dừng để phiên làm được.
   - Đề xuất: (a). `auto-run.md`, mục 6, ý 3 nói bài học `accepted` được làm thành PR cải tiến trong phiên có người theo dõi, và việc chỉ một dòng nên làm có người không tốn thêm bao nhiêu.
3. **Chỗ ghi L-160 `applied`.** `docs/ops/from-app.md`, bước 4, và `auto-run.md`, mục 6, ý 3: bài học chuyển `applied` khi PR cải tiến merge. Làm có người: ghi trong chính PR của task, kèm số PR đó, như T-100 (bài học lên nhánh chính cùng lúc với phần sửa). Phiên tự chạy: ghi trong PR báo cáo, sau khi PR của task merge. Đề xuất: theo cách làm đã chọn ở câu 2; với (a) thì ghi trong PR của task.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:**
- **Người duyệt:**, **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng). -->
