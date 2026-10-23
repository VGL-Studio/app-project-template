---
ticket: T-007
title: Khóa Remote Config có kiểu, khai báo một chỗ, bảng khóa sinh tự động
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 25M
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
---

# PLAN T-007: KHÓA REMOTE CONFIG CÓ KIỂU, KHAI BÁO MỘT CHỖ, BẢNG KHÓA SINH TỰ ĐỘNG

## 1. Tóm tắt

Thêm kiểu `RemoteKey<T>` (boolean, long, double, string) vào `:core:config` và cho `RemoteSettings` đọc theo khóa có kiểu. Cờ tính năng, khóa của `:core:ads` (kể cả khóa bật tắt từng `AdPlacement`) và 5 khóa mới của spec mục 8 đều khai báo bằng `RemoteKey`; khóa mới chỉ khai báo, chưa dùng. Một test ở `:app` thấy mọi khóa của app, sinh và kiểm `docs/remote-config-keys.md` cùng `docs/remote-config-template.json`, giống cách `DesignTokensTest` giữ `tokens.json`. Không đổi giá trị mặc định nào; unit test giữ từng giá trị mặc định.

## 2. Hiểu yêu cầu

Ticket xử lý kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-10 | Khi thêm một khóa trong code mà chưa sinh lại bảng khóa thì kiểm tra tự động báo lỗi | Test ở `:app` so nội dung sinh từ code với hai file trong repo; lệch thì thất bại và in lệnh ghi lại. Test chạy trong `testStagingDebugUnitTest` mà `scripts/check.sh` đã gọi, nên không phải sửa `check.sh`. Cờ tính năng mới và vị trí quảng cáo mới tự vào bảng (lấy từ `FeatureFlag.entries`, `AdPlacement.entries`); khóa mới khai báo trong `AdsSettings` mà quên đưa vào danh sách thì một test của `:core:ads` báo lỗi |
| KB-10 | Bảng khóa liệt kê mọi khóa với kiểu, giá trị mặc định, module và mô tả; có file mẫu nhập được vào Firebase | `docs/remote-config-keys.md` có bảng 5 cột; `docs/remote-config-template.json` theo định dạng template của Firebase Remote Config (`parameters`, mỗi khóa có `defaultValue.value`, `description`, `valueType`) |
| KB-10 | Module core đọc được khóa kiểu số thực và chuỗi | `RemoteFlagSource` có thêm `double` và `string`; `RemoteSettings.value(key)` đọc đủ 4 kiểu, trả mặc định khi chưa có giá trị hay giá trị sai kiểu |

Ngoài ra, theo ticket: khai báo 5 khóa mới của spec mục 8 (`ads_enabled` đã tính là một trong 5), cập nhật `docs/ops/remote-config.md` bỏ ghi chú đợt 2 của phần kiểu double, string và bảng khóa (phần giá trị cục bộ cho staging vẫn là đợt 2, T-008).

Thiết kế tham chiếu: Không có thiết kế riêng (không có giao diện).

ADR liên quan: ADR-0003 (cờ tính năng), ADR-0012 (nối Firebase, `RemoteFlagSource`), ADR-0014 (giá trị mặc định của quảng cáo, không đổi), ADR-0023 (quy ước khóa, bảng khóa sinh từ code).

**Sinh bảng bằng test, không bằng script:** ADR-0023 mục 4 đã ghi "một test (chạy trong cổng chất lượng) sinh và kiểm"; plan làm đúng như vậy, theo mẫu `DesignTokensTest`, vì chỉ code Kotlin mới đọc được khóa và mặc định một cách chắc chắn. Riêng mục Hệ quả của ADR còn chữ "thêm script" (câu hỏi 1).

## 3. Giải pháp

### 3.1. Cách làm

**`RemoteKey<T>` (`:core:config`, file mới `RemoteKey.kt`, public).**

```kotlin
class RemoteKey<T : Any> private constructor(
    val name: String,
    val type: Type,
    val default: T,
    val description: String,
) {
    enum class Type { BOOLEAN, LONG, DOUBLE, STRING }

    companion object {
        fun boolean(name: String, default: Boolean, description: String): RemoteKey<Boolean>
        fun long(name: String, default: Long, description: String): RemoteKey<Long>
        fun double(name: String, default: Double, description: String): RemoteKey<Double>
        fun string(name: String, default: String, description: String): RemoteKey<String>
    }
}
```

- Hàm tạo kiểm tra bằng `require` (ném `IllegalArgumentException`): tên theo ADR-0023 (`^[a-z][a-z0-9]*(_[a-z0-9]+)*$`); mô tả không rỗng và không quá 256 ký tự (giới hạn mô tả tham số của Firebase).
- `equals` và `hashCode` theo cả 4 trường, `toString` trả tên. Cần cho `AdsSettings.placementEnabled(placement)` (mỗi lần gọi tạo đối tượng mới) và cho T-008 dùng khóa làm khóa của map.
- Không dùng `data class`: hàm tạo riêng của `data class` vẫn lộ qua `copy`.

**`RemoteFlagSource` và `FirebaseRemoteFlagSource`.** Thêm `double(key): Flow<Double?>` và `string(key): Flow<String?>`, cùng hợp đồng như `boolean`, `long` (null khi chưa có giá trị từ xa hay sai kiểu). Bản Firebase dùng `asDouble()` và `asString()`. Tách phần quyết định của `read` thành một hàm `internal` không gọi Firebase hay `android.util.Log`, để test trên JVM:

```kotlin
internal fun <T> activeValueOrNull(source: Int, convert: () -> T, onWrongType: (IllegalArgumentException) -> Unit): T?
```

Trả null khi `source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC`; bắt `IllegalArgumentException` của `convert`, gọi `onWrongType` (nơi gọi ghi `Log.w` như hiện nay) và trả null. `asString()` của Firebase không ném lỗi; khóa chuỗi chỉ trả null khi nguồn là `STATIC`.

**`RemoteSettings`.** Thay hai hàm `boolean(key: String, default)`, `long(key: String, default)` bằng một hàm:

```kotlin
fun <T : Any> value(key: RemoteKey<T>): Flow<T>
```

`DefaultRemoteSettings` chọn hàm của `RemoteFlagSource` theo `key.type`, ép kiểu (một `@Suppress("UNCHECKED_CAST")` có chú thích vì sao an toàn) rồi `map { it ?: key.default }`.

Quyết định bỏ hai hàm cũ, không giữ để chuyển dần: nơi gọi duy nhất là `AdsController` (đã `grep`), được chuyển trong cùng task; giữ hàm nhận tên khóa dạng chuỗi thì khóa mới có thể bỏ qua chỗ khai báo chung, trái ADR-0023 mục 3. App đã tạo từ dự án mẫu có module core riêng gọi hàm cũ thì đổi sang `RemoteKey` khi kéo thay đổi về (xem câu hỏi 2).

**`FeatureFlag`.** Thêm tham số `description: String` (tiếng Anh, một câu) cho từng cờ và thuộc tính `val remoteKey: RemoteKey<Boolean>` dựng từ `key`, `defaultValue`, `description`. Giữ `key` và `defaultValue` để `StagingFeatureFlagsModule`, `:feature:sample`, `:feature:loading` và test hiện có không đổi. `DefaultFeatureFlags` đọc `remote.boolean(flag.remoteKey.name)` với mặc định `flag.remoteKey.default`; hành vi không đổi.

**`AdsSettings` (`:core:ads`).** Chuyển từ `internal object` sang `public object`, để `:app` liệt kê được khóa và T-008 bật được khóa vị trí ở `app/src/staging`. Hàm `requestsNonPersonalizedAds` vẫn `internal`. Nội dung:

| Thành phần | Khóa | Kiểu | Mặc định | Ghi chú |
| --- | --- | --- | --- | --- |
| `ENABLED` | `ads_enabled` | boolean | `true` | Mới, chưa dùng (T-009) |
| `PERSONALIZED_IN_REGULATED_REGIONS` | `ads_personalized_regulated_regions_enabled` | boolean | `true` | Đã có, giữ tên và mặc định (ADR-0014) |
| `INTERSTITIAL_MIN_INTERVAL_SECONDS` | `ads_interstitial_min_interval_seconds` | long | `45` | Đã có, giữ (ADR-0014) |
| `FIRST_SESSION_GRACE_SECONDS` | `ads_first_session_grace_seconds` | long | `180` | Mới, chưa dùng |
| `INTERSTITIAL_MAX_PER_SESSION_COUNT` | `ads_interstitial_max_per_session_count` | long | `4` | Mới, chưa dùng |
| `INLIST_EVERY_COUNT` | `ads_inlist_every_count` | long | `8` | Mới, chưa dùng |
| `NATIVE_LOAD_TIMEOUT_SECONDS` | `ads_native_load_timeout_seconds` | long | `10` | Mới, chưa dùng |
| `placementEnabled(placement): RemoteKey<Boolean>` | `ads_<placement.key>_enabled` | boolean | `false` | Thay `placementEnabledKey` và `PLACEMENT_ENABLED_DEFAULT` |
| `all(): List<RemoteKey<*>>` | | | | Mọi khóa trên theo thứ tự khai báo, rồi `placementEnabled` của từng `AdPlacement.entries` |

Bỏ các hằng `*_DEFAULT` và `placementEnabledKey`: mặc định nằm trong `RemoteKey`. Mô tả khóa viết tiếng Anh như mọi chuỗi trong code, ý theo bảng spec mục 8 và chú thích hiện có.

**`AdsController`.** Ba chỗ đọc đổi sang `settings.value(AdsSettings.INTERSTITIAL_MIN_INTERVAL_SECONDS)`, `settings.value(AdsSettings.PERSONALIZED_IN_REGULATED_REGIONS)`, `settings.value(AdsSettings.placementEnabled(placement))`. Không đổi logic. `AdSlot.kt` gọi `ads.isEnabled(placement)`, chữ ký không đổi, nên không sửa.

**Chỗ liệt kê mọi khóa của app (`:app`, file mới `config/AppRemoteKeys.kt`).** `:app` là module duy nhất thấy mọi module core (ADR-0001), nên chỗ gom đặt ở đây:

```kotlin
/** Every Remote Config key of the app, by owning module (ADR-0023). A new core module with keys is added here. */
object AppRemoteKeys {
    val byModule: Map<String, List<RemoteKey<*>>> = linkedMapOf(
        ":core:config" to FeatureFlag.entries.map { it.remoteKey },
        ":core:ads" to AdsSettings.all(),
    )
}
```

Đặt trong `src/main`, không trong `src/test`: T-014 (màn hình xem thử) và T-008 có thể cần đọc danh sách này. Xem câu hỏi 3.

**Test sinh bảng (`:app`, file mới `src/test/.../config/RemoteConfigKeysTest.kt`).** Làm theo `DesignTokensTest`:

- Hai hàm `internal` trong file test nhận `Map<String, List<RemoteKey<*>>>`: `keysMarkdown(byModule)` và `firebaseTemplateJson(byModule)`. Module theo thứ tự của map, khóa trong mỗi module xếp theo tên, để file ổn định giữa các lần sinh. Không dùng thư viện JSON: dựng chuỗi bằng tay như `tokensJson()`, với hàm thoát ký tự `"` và `\` trong chuỗi JSON, `|` trong ô bảng Markdown.
- Hai test so file: đọc đường dẫn từ system property `remoteConfigKeys.markdown`, `remoteConfigKeys.template`; khi `remoteConfigKeys.update` là `true` thì ghi file trước khi so. Thông báo lỗi (tiếng Việt, cho nhóm): "docs/remote-config-keys.md đã cũ so với code. Chạy ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true rồi commit."
- `app/build.gradle.kts` thêm khối `tasks.withType<Test>().configureEach` như `core/designsystem/build.gradle.kts`: ba system property trên, và khai báo hai file là `inputs` của task test (`inputs.files(...).withPropertyName("remoteConfigKeysDocs")`), để sửa tay một file thì test chạy lại chứ không bị Gradle coi là `UP-TO-DATE`.
- Lệnh ghi lại khi thêm khóa: `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Ghi vào `docs/ops/remote-config.md` và vào chú thích lớp test.

**Định dạng `docs/remote-config-keys.md`** (tiếng Việt, theo docs/CLAUDE.md, mục Cách viết):

```markdown
# BẢNG KHÓA REMOTE CONFIG

<!-- Sinh từ code bởi RemoteConfigKeysTest (:app). Không sửa tay. Sinh lại: ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true -->

Mọi khóa Remote Config của bản app hiện tại (ADR-0023). File mẫu để nhập vào Firebase: `remote-config-template.json`.

| Khóa | Kiểu | Mặc định | Module | Mô tả |
| --- | --- | --- | --- | --- |
| `feature_loading_enabled` | boolean | `false` | `:core:config` | ... |
```

Mặc định viết như giá trị trên Firebase: `true`, `45`, `0.5`; chuỗi trong ngoặc kép (`"vi,en"`, chuỗi rỗng là `""`).

**Định dạng `docs/remote-config-template.json`** (template của Firebase Remote Config):

```json
{
  "conditions": [],
  "parameters": {
    "ads_enabled": {
      "defaultValue": { "value": "true" },
      "description": "...",
      "valueType": "BOOLEAN"
    }
  }
}
```

`valueType`: boolean là `BOOLEAN`, long và double là `NUMBER`, string là `STRING`. Giá trị luôn là chuỗi. Thứ tự khóa như bảng Markdown.

**Nơi khác đọc file plan đổi:** `grep -rn` trên `scripts/`, `.github/`, `build-logic/`, `*/src/test`: không nơi nào đọc `AdsSettings.kt`, `RemoteSettings.kt`, `FeatureFlag.kt` hay `docs/ops/remote-config.md` theo tên. `scripts/release.sh` nhắc tới `app/build.gradle.kts` nhưng không bị ảnh hưởng: plan chỉ thêm cấu hình cho task `Test` (system property, `inputs`), không đổi biến thể, tham số `-P` hay task build mà bản phát hành dùng; plan không đọc hay sửa `release.sh` (CLAUDE.md, mục Không được làm). `scripts/new-app.sh` chỉ thay tên gói trong mọi file, nên `app/src/test` mới cũng được đổi gói. `scripts/check.sh` (cả bản sửa ở PR #81, so ảnh chuẩn của `:app`) chỉ xử lý thư mục test có `captureRoboImage`; `app/src/test` của plan không có ảnh nên không bị ảnh hưởng. `docs/ops/ads-guide.md` mục 5 (thêm vị trí) phải nhắc sinh lại bảng khóa: có trong danh sách file. Hai file docs mới chưa có nơi đọc; T-010 đến T-016 sinh lại chúng khi thêm vị trí hay khóa.

**Thứ tự commit (L-025):**

1. Commit test (bước 3): `RemoteKeyTest.kt`, `FirebaseRemoteValueTest.kt`, các test `:core:config` và `:core:ads` đã sửa, `FakeRemoteFlagSource.kt` (thêm `double`, `string`), `RemoteConfigKeysTest.kt` gồm cả hai hàm sinh. Commit này không biên dịch được vì `RemoteKey`, `RemoteSettings.value`, `AdsSettings.all` chưa có: đó là lý do thất bại đúng mong đợi. Ghi rõ trong nhật ký.
2. Commit code (bước 4): mọi file `src/main`, `app/build.gradle.kts`.
3. Commit sinh file: chạy lệnh ghi lại, thêm `docs/remote-config-keys.md`, `docs/remote-config-template.json`; trước khi chạy lệnh, xác nhận hai test so file thất bại vì thiếu file.
4. Commit tài liệu: `docs/ops/remote-config.md`, `docs/ops/ads-guide.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/template/core/config/RemoteKey.kt` | Kiểu `RemoteKey<T>`, `RemoteKey.Type`, 4 hàm tạo có kiểm tra tên và mô tả |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/RemoteSettings.kt` | `value(key: RemoteKey<T>): Flow<T>` thay `boolean`, `long`; `DefaultRemoteSettings` theo kiểu khóa |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/RemoteFlagSource.kt` | Thêm `double`, `string` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/FirebaseRemoteFlagSource.kt` | `double`, `string` qua `asDouble`, `asString`; tách `activeValueOrNull` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/FeatureFlag.kt` | Tham số `description`, thuộc tính `remoteKey` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/DefaultFeatureFlags.kt` | Đọc theo `flag.remoteKey`, hành vi không đổi |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/template/core/config/RemoteKeyTest.kt` | Test hàm tạo, kiểm tra tên, mô tả, so sánh bằng |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/template/core/config/FirebaseRemoteValueTest.kt` | Test `activeValueOrNull` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/template/core/config/DefaultRemoteSettingsTest.kt` | Viết lại theo `RemoteKey`, thêm double, string, sai kiểu |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/template/core/config/FakeRemoteFlagSource.kt` | Thêm `doubles`, `strings` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/template/core/config/DefaultFeatureFlagsTest.kt` | Thêm test `remoteKey` của cờ |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Public, khai báo bằng `RemoteKey`, 5 khóa mới, `placementEnabled`, `all()` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Đọc qua `settings.value(...)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Giữ mọi mặc định, tên khóa, `all()` đủ |
| Sửa | `app/build.gradle.kts` | System property và `inputs` cho test sinh bảng khóa |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/config/AppRemoteKeys.kt` | Chỗ gom mọi khóa của app theo module |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` | Hai hàm sinh, test so file, test quy ước khóa |
| Tạo | `docs/remote-config-keys.md` | Sinh bằng lệnh ghi lại, không viết tay |
| Tạo | `docs/remote-config-template.json` | Sinh bằng lệnh ghi lại, không viết tay |
| Sửa | `docs/ops/remote-config.md` | Bỏ "(đợt 2)" của double, string, bảng khóa (dòng 5, 21, 31, 58; giữ "(đợt 2)" của giá trị cục bộ staging và của `ads_enabled` ở dòng 50 vì T-008, T-009 mới làm); ghi lệnh sinh lại; chỗ khai báo khóa quảng cáo là `AdsSettings` với `RemoteKey`; khóa mới cần mô tả tiếng Anh; lưu ý nhập file mẫu chỉ cho project mới |
| Sửa | `docs/ops/ads-guide.md` | Mục 5, bước 3: khóa bật tắt của vị trí mới tự vào bảng khóa qua `AdsSettings.all()`; sinh lại bảng khóa bằng lệnh của `remote-config.md` trong cùng PR |

File được bảo vệ (`scripts/check.sh`, `.github/`, `build-logic/`, `gradle/libs.versions.toml`): không chạm. Test mới ở `:app` chạy trong `testStagingDebugUnitTest` sẵn có của `check.sh`; không thêm thư viện.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: không thêm cờ. Hai cờ hiện có (`SAMPLE`, `LOADING`) chỉ thêm mô tả, tên và mặc định giữ nguyên.
- Quảng cáo: không đổi vị trí, định dạng, tần suất hay giá trị mặc định nào (ADR-0014). Khai báo 5 khóa của spec mục 8, chưa đọc ở đâu; hành vi quảng cáo không đổi tới T-009.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-10, tiêu chí 3: đọc double | Unit test | `DefaultRemoteSettingsTest.doubleKey_withoutRemoteValue_usesDefault` (khóa `some_ratio`, mặc định `0.5`); `doubleKey_remoteValue_replacesDefault` (fake trả `0.25`) |
| KB-10, tiêu chí 3: đọc string | Unit test | `DefaultRemoteSettingsTest.stringKey_withoutRemoteValue_usesDefault` (mặc định `"vi,en"`); `stringKey_remoteValue_replacesDefault` (fake trả `"hi"`); `stringKey_remoteEmptyString_isKept` (fake trả `""`, kết quả `""`, không về mặc định) |
| KB-10, tiêu chí 3: boolean, long giữ hành vi cũ | Unit test | `DefaultRemoteSettingsTest.booleanAndLong_withoutRemoteValue_useDefault`, `booleanAndLong_remoteValue_replacesDefault`, `remoteValueActivatedLater_reachesCollectors` (Turbine: `120`, rồi `45` khi fake đổi) |
| Giá trị sai kiểu dùng mặc định | Unit test | `DefaultRemoteSettingsTest.valueOfAnotherType_usesDefault`: fake chỉ có `some_ratio` trong `longs`, đọc bằng khóa double, kết quả là mặc định |
| Firebase: nguồn tĩnh và sai kiểu | Unit test | `FirebaseRemoteValueTest.staticSource_returnsNull` (`VALUE_SOURCE_STATIC`, `convert` không được gọi); `remoteSource_returnsConvertedValue` (`VALUE_SOURCE_REMOTE`, trả `0.25`); `wrongType_returnsNullAndReportsIt` (`convert` ném `IllegalArgumentException`, kết quả null, `onWrongType` được gọi một lần, so loại lỗi bằng `is`) |
| Khóa đúng quy ước, mô tả hợp lệ | Unit test | `RemoteKeyTest.factories_keepNameTypeDefaultAndDescription` (4 hàm tạo, đúng `Type`); `invalidName_isRejected` (`"Ads_Enabled"`, `"ads-enabled"`, `"_ads"`, `"ads__enabled"`, `""` đều ném `IllegalArgumentException`); `blankDescription_isRejected`; `descriptionOver256Chars_isRejected` (257 ký tự ném, 256 ký tự qua); `sameFields_areEqual` (hai lần `RemoteKey.boolean("a_b", false, "x")` bằng nhau, cùng `hashCode`; khác mặc định thì khác nhau) |
| Cờ tính năng có khóa đúng | Unit test | `DefaultFeatureFlagsTest.everyFlag_remoteKeyMatchesKeyAndDefault`: với mọi `FeatureFlag`, `remoteKey.name == key`, `remoteKey.default == false`, `remoteKey.type == BOOLEAN`. Các test hiện có giữ nguyên và vẫn qua |
| Không đổi mặc định (ADR-0014), 5 khóa mới đúng spec mục 8 | Unit test | `AdsSettingsTest`: `placementsAreOffByDefault` (mọi `AdPlacement`: `placementEnabled(p).default == false`, tên `ads_<key>_enabled`); `personalizedAdsInRegulatedRegionsAreOnByDefault` (tên `ads_personalized_regulated_regions_enabled`, `true`); `interstitialMinIntervalIs45SecondsByDefault`; `adsAreEnabledByDefault` (`ads_enabled`, `true`); `firstSessionGraceIs180Seconds`; `interstitialMaxPerSessionIs4`; `inListEveryIs8`; `nativeLoadTimeoutIs10Seconds` (mỗi test so cả tên khóa, kiểu và mặc định) |
| KB-10, tiêu chí 1: khóa khai báo trong `AdsSettings` không bị sót khỏi danh sách | Unit test | `AdsSettingsTest.allKeys_containsEveryDeclaredKey`: mọi field kiểu `RemoteKey` của `AdsSettings::class.java.declaredFields` có trong `all()`; `allKeys_containsEveryPlacement`; `allKeys_haveUniqueNames` |
| KB-10, tiêu chí 1: bảng lệch code thì báo lỗi | Unit test | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`: so `keysMarkdown(AppRemoteKeys.byModule)` và `firebaseTemplateJson(...)` với file trong repo. Khi viết test (bước 3), xác nhận thất bại vì thiếu file; khi rà soát, thử thêm một cờ tạm vào `FeatureFlag` (không commit) và xác nhận test thất bại với thông báo có lệnh ghi lại |
| KB-10, tiêu chí 2: đủ cột, đúng định dạng | Unit test | `RemoteConfigKeysTest.markdown_formatsEveryType`: với map giả `":core:x"` gồm 4 khóa (boolean `true`, long `45`, double `0.5`, string `"vi,en"`), mỗi khóa đúng một dòng `` | `<tên>` | <kiểu> | `<mặc định>` | `:core:x` | <mô tả> | ``; `markdown_sortsKeysByNameWithinModule`; `markdown_escapesPipeInDescription` |
| KB-10, tiêu chí 2: file mẫu Firebase | Unit test | `RemoteConfigKeysTest.json_mapsTypes` (BOOLEAN, NUMBER, NUMBER, STRING; giá trị là chuỗi `"true"`, `"45"`, `"0.5"`, `"vi,en"`); `json_escapesQuoteAndBackslash` (mô tả `say "hi" \ ok` thành `say \"hi\" \\ ok`) |
| Mọi khóa của app đúng ADR-0023 | Unit test | `RemoteConfigKeysTest.everyKeyName_isUniqueAcrossModules`; `everyNumberKey_endsWithUnit` (long và double kết thúc bằng `_seconds`, `_count` hoặc `_percent`); `everyFeatureFlagAndPlacement_isListed` (mọi `FeatureFlag` và mọi `AdPlacement` có khóa trong `AppRemoteKeys.byModule`) |
| Hành vi app không đổi | Bộ test hiện có | `AdUnitIdsTest`, `FullScreenAdGuardTest`, test của `:feature:sample`, `:feature:loading`, test trên emulator trong CI qua mà không sửa |
| File mẫu nhập được vào Firebase | Kiểm tra thủ công | Trên một project Firebase thử (không phải production): Remote Config, menu "Publish from a file" hay tương đương, chọn `docs/remote-config-template.json`, xem đủ khóa, kiểu và mặc định; không bấm Publish trên project đang dùng. Nhập file thay cả bộ cấu hình, nên chỉ dùng cho project mới (câu hỏi 4) |

**Ngoại lệ viết test trước:** `docs/remote-config-keys.md` và `docs/remote-config-template.json` sinh ra từ code bằng lệnh ghi lại, không viết trước được; test so file đã viết trước và thất bại vì thiếu file. Đoạn sửa `docs/ops/remote-config.md` và `docs/ops/ads-guide.md` là tài liệu, không có test.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** mục 7 (module core dùng chung): sửa `:core:config` và `:core:ads`. Không chạm điểm chặn 1 (quảng cáo): không đổi vị trí, định dạng hay tần suất, khóa mới chưa được đọc. Không chạm điểm chặn 9: khóa Remote Config không phải dữ liệu lưu trên máy.

Điểm 5 tiêu chí, chấm để người duyệt thấy mức độ dù plan đã xếp cao vì điểm chặn:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:config`, `:core:ads`, `:app`), đổi giao diện công khai của `RemoteSettings` và `AdsSettings` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, ticket đã nêu API; AI tự quyết bỏ hàm cũ, chỗ đặt danh sách khóa, định dạng hai file sinh (câu hỏi 1 đến 3) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có unit test; chỉ việc nhập file mẫu vào Firebase là thủ công, và đọc thật từ Firebase không chạy được trên JVM |
| D. Ảnh hưởng chéo | 2 | T-008, T-009 cùng sửa `AdsSettings.kt`, `RemoteSettings.kt` và phụ thuộc API này |
| E. Độ mới của kỹ thuật | 1 | Biến thể của `DesignTokensTest`; test đơn vị đầu tiên của `:app` |
| **Tổng** | **8** | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RemoteSettings` (public API của `:core:config`), `RemoteFlagSource`, `FeatureFlag`, `AdsSettings` (từ internal sang public), `AdsController`, `app/build.gradle.kts`.
- Task khác trong sprint có thể xung đột: T-008 (sửa `RemoteSettings.kt`, `DefaultRemoteSettingsTest.kt`, `docs/ops/remote-config.md`; dùng `AdsSettings.placementEnabled` từ `app/src/staging`, nên cần `AdsSettings` public như plan này) và T-009 (sửa `AdsController.kt`, đọc `ENABLED` và các khóa mới) phụ thuộc T-007. T-010, T-014 đổi `AdPlacement` và T-016 thêm `":core:network"` vào `AppRemoteKeys.byModule`: các task đó chạy lệnh ghi lại trong cùng PR (ADR-0023). T-014 sửa mô tả của `placementEnabled` cho vị trí `stagingOnly`: `RemoteKey.equals` so cả mô tả, nhưng T-008 tra theo tên khóa nên không ảnh hưởng.
- Đề xuất thứ tự chạy: chạy T-007 đầu tiên trong chuỗi quảng cáo, trước T-008, T-009 và mọi ticket native.
- `token_estimate`: 25M, cao hơn số gợi ý 20M của cỡ M (`scripts/token-usage.py estimate`, chưa có task đã đo) vì task chạm ba module và có thêm bước sinh file.

## 7. Điều kiện dừng riêng

- Cần đổi bất kỳ giá trị mặc định nào, hay tên một khóa đã có, để test qua.
- Test ở `:app` không chạy trong `testStagingDebugUnitTest` của `scripts/check.sh` (ví dụ biến thể không có unit test), tức là phải sửa `check.sh` hay `build-logic/` mới chạy được.
- `:app` không biên dịch được test vì thiếu thư viện test, cần sửa `build-logic/` hay `gradle/libs.versions.toml`.
- Một module khác ngoài `:core:ads` gọi `RemoteSettings.boolean` hay `RemoteSettings.long` (lúc viết plan không có), cần sửa file ngoài danh sách.
- Lint hay ktlint báo lỗi ở `AppRemoteKeys` (ví dụ không được dùng) mà chỉ tránh được bằng cách tắt quy tắc.

## 8. Câu hỏi cho người duyệt

1. **ADR-0023 mục 4 đã ghi "một test" sinh bảng khóa, plan làm đúng; mục Hệ quả của ADR còn câu "đổi `:core:config` và thêm script".** Đề xuất: sửa chữ "thêm script" thành "thêm test sinh bảng" trong ADR-0023 (đang `proposed`) ở PR tài liệu của đợt này, ngoài T-007.
2. **Bỏ `RemoteSettings.boolean/long` nhận tên khóa dạng chuỗi.** Đề xuất: bỏ, vì chỉ `AdsController` dùng và giữ lại thì khóa có thể né chỗ khai báo chung. App đã tạo từ dự án mẫu có module core riêng gọi hàm cũ sẽ phải đổi khi kéo thay đổi về. Nếu muốn chuyển dần thì giữ hai hàm, đánh `@Deprecated`, bỏ ở ticket sau.
3. **Chỗ gom khóa `AppRemoteKeys` đặt ở `app/src/main`, `AdsSettings` thành public.** Đề xuất: đồng ý, để T-008 (bật vị trí ở staging) và T-014 (màn hình xem thử) dùng được. Phương án khác: chỉ đặt trong `app/src/test`, giữ `AdsSettings` internal và thêm một hàm public trả danh sách khóa.
4. **Nhập file mẫu vào Firebase thay cả bộ cấu hình của project.** Đề xuất: `docs/ops/remote-config.md` ghi rõ chỉ nhập cho project mới; project đang chạy thì thêm từng khóa bằng tay theo bảng. Chưa kiểm tra được trên Firebase thật vì staging dùng file giả (ADR-0012); người duyệt có muốn kiểm tra thủ công này trước khi đóng ticket không?
5. **Mô tả khóa viết tiếng Anh** (chuỗi trong code, CLAUDE.md mục Quy ước code), còn tiêu đề và chữ của `remote-config-keys.md` viết tiếng Việt. Đề xuất: giữ như vậy; người vận hành đọc mô tả tiếng Anh trên Firebase.
6. **Khóa số phải kết thúc bằng `_seconds`, `_count` hay `_percent`** (test `everyNumberKey_endsWithUnit`). ADR-0023 chỉ nêu ba đơn vị này làm ví dụ. Đề xuất: test cứng ba đơn vị; cần đơn vị mới thì thêm vào test cùng plan của khóa đó.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
