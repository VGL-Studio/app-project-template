---
ticket: T-058
title: Máy thử và log từng mạng ở bản staging
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, build)
risk_level: high             # low | medium | high
token_estimate: 8M           # cỡ S theo ticket, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29); nâng lên khoảng 1,5 lần vì task chạm hai module và file build: một khối đọc local.properties trong app/build.gradle.kts, hai kiểu mới của :core:ads, bảy đường tải của AdsController, banner và native, hai module Hilt
blocking_points: [ads, core, build]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-058: MÁY THỬ VÀ LOG TỪNG MẠNG Ở BẢN STAGING

## 1. Tóm tắt

Dev ghi mã máy thử của mình vào khóa `ads.testDeviceIds` trong `local.properties` (không vào git). Chỉ khối `staging` của `app/build.gradle.kts` đọc khóa này, thành trường `BuildConfig.ADS_TEST_DEVICE_IDS` mà bản production không có; `StagingAdsModule` đưa nó vào `:core:ads` qua Hilt bằng kiểu mới `AdsDebugOptions`, giống cách `ConsentDebugOptions` đang làm. `AdsController` đặt `RequestConfiguration.setTestDeviceIds` ngay trước `MobileAds.initialize`; ở staging, mỗi kết quả tải (thành công hay lỗi) của mọi định dạng ghi một dòng log thẻ `Ads` cho từng mạng trong `ResponseInfo.adapterResponses`: tên lớp adapter, thời gian, mã lỗi; không có mã đơn vị, mã máy hay thông báo lỗi của SDK. Kiểm chứng bằng unit test của hàm thuần và của từng đường tải, test quét file build và source set (production không có đường đọc khóa), và kiểm tra thủ công trên máy thật cho Ad Inspector.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-28 | Khi dev khai báo máy của mình là máy thử trong cấu hình trên máy dev (không trong repo) thì bản staging gửi yêu cầu như máy thử và Ad Inspector mở được trên máy đó (câu hỏi 40) | Cấu hình là khóa `ads.testDeviceIds` của `local.properties` ở gốc repo (đã có trong `.gitignore`), giá trị là các mã máy thử mà SDK in ra logcat (`setTestDeviceIds(Arrays.asList("..."))`), cách nhau bằng dấu phẩy. Chỉ bản staging dựng trên máy dev có khóa; `staging.yml` và mọi CI không có khóa nên bản gửi cho nhóm không có máy thử. "Gửi yêu cầu như máy thử" là `MobileAds.setRequestConfiguration` có `testDeviceIds` trước lần khởi tạo SDK và trước mọi yêu cầu. Ad Inspector mở bằng nút "Open Ad Inspector" của màn xem thử (T-014): Google cho mở trên máy khai bằng `setTestDeviceIds`, không cần đăng ký trên AdMob |
| KB-28 | Khi không khai báo máy thử thì bản staging chạy như hiện tại; bản production không bao giờ có máy thử | Không có khóa, khóa rỗng hay không có `local.properties`: không gọi `setRequestConfiguration`, không có dòng log về máy thử. Production: khối `production` của file build không đọc khóa, `BuildConfig` production không có trường, `ProductionAdsModule` cấp `AdsDebugOptions.NONE` (không máy thử, không log từng mạng). `scripts/release.sh` chép `local.properties` của máy người phát hành vào thư mục build (dòng 251), nên điều này phải đúng cả khi file có khóa: chỉ khối `staging` đọc nó |
| KB-28 | Mỗi lượt tải quảng cáo ở staging ghi log từng mạng đã thử (tên mạng, thời gian, lỗi), không ghi mã đơn vị | Theo câu trả lời 40: một dòng thẻ `Ads` mỗi mục của `ResponseInfo.adapterResponses`, theo thứ tự SDK trả, dạng `Network <khóa>: <tên lớp adapter>, <ms> ms, <kết quả>`; `<khóa>` là khóa vị trí (hay khóa khe, hay `pool`) mà dòng log tải hiện có ở chỗ đó đang dùng; `<kết quả>` là `loaded` cho mạng của `loadedAdapterResponseInfo`, `error <mã>` khi mạng có lỗi, `no error` khi không lỗi mà không được chọn. Danh sách rỗng hay không có `ResponseInfo` thì một dòng `Network <khóa>: none tried`. Áp cho mọi lượt tải có kết quả từ SDK: native (đơn, pool, hàng của danh sách, lớp thu gọn), native toàn màn hình, interstitial, app open, rewarded, banner (kể cả lượt SDK tự làm mới). Không đọc `getCredentials()` (chứa mã đơn vị của mạng), `toString()`, tên phiên bản nguồn quảng cáo, hay `AdError.message` (`ads-business-rules.md`, mục 8: thông báo của SDK có thể chứa mã đơn vị) |
| KB-8, tiêu chí 2 | Mở được Ad Inspector từ màn xem thử | Không đổi code của nút; trước T-058 chỉ mở được trên emulator hay máy đăng ký trên AdMob (`ads-guide.md`, mục 8). Sau T-058 mở được trên máy thật đã khai báo |
| KB-9, tiêu chí 2 | Log trạng thái liệt kê adapter | Không đổi `adapterStatusLines` (T-015). Log từng mạng là log mới, theo lượt tải, không thay log trạng thái lúc khởi tạo |

Trên staging của dự án mẫu, mã app và mã đơn vị là mã thử của Google (ADR-0007, M-06), không có nhóm mediation (FB-20261023-01), nên mỗi lượt tải chỉ có một dòng của adapter AdMob (`com.google.ads.mediation.admob.AdMobAdapter`, giả định, chưa thấy trên máy). Log nhiều mạng chỉ thấy ở app có mã AdMob thật cho staging hoặc khi dev thử bằng "single ad source testing" của Ad Inspector. Ghi điều này trong hướng dẫn và câu hỏi 1 ở mục 8.

Thiết kế tham chiếu: Không có thiết kế riêng (công cụ cho dev, không đổi giao diện; `docs/ops/design-workflow.md`).

ADR liên quan: ADR-0007 (tách staging và production, không viết cứng cấu hình môi trường), ADR-0012 (cấu hình theo biến thể, bản giả cho build không bí mật), ADR-0013 (mã quảng cáo theo biến thể, cài đặt gỡ lỗi chỉ ở staging), ADR-0019 (log không ghi mã định danh, mức `info` vào Crashlytics), ADR-0026 (luật quảng cáo theo `ads-business-rules.md`), ADR-0038 (JBase: có log waterfall của MAX ở lượt doanh thu, không có máy thử; câu 40 không theo JBase vì đây là công cụ cho dev).

Luật nghiệp vụ liên quan: S-17 (cài đặt gỡ lỗi chỉ ở staging), M-06 (mã thử của Google ở staging), mục 8 (không gửi thông báo lỗi của SDK), mục 13 (log bằng `AppLogger`, không ghi mã đơn vị). Phản hồi: FB-20261023-01 (`wontfix`, giải thích vì sao staging chỉ có AdMob), FB-20261024-02 (`new`, log trạng thái chỉ có bốn adapter; T-058 không sửa, nhưng log từng mạng giúp kiểm giả thuyết của phản hồi đó trên app có mã thật).

## 3. Giải pháp

### 3.1. Cách làm

**Chọn cách đưa giá trị (BuildConfig chỉ ở staging, rồi Hilt).** `:core:ads` không đọc được `BuildConfig` của `:app`, nên dù chọn gì cũng phải có một kiểu cấu hình cấp qua Hilt. Câu hỏi thật là giá trị đi từ máy dev vào APK bằng đường nào:

- **Chọn: trường `BuildConfig` chỉ khai báo trong khối `named("staging")`.** Production không có trường: code production nào nhắc tới nó thì không biên dịch được. Giá trị nằm trong APK staging dựng trên máy dev, không vào repo. Theo đúng mẫu `APPSFLYER_DEV_KEY` của staging và `ConsentDebugOptions` của `StagingAdsModule`.
- Không chọn: `resValue` hay tệp trong `assets` sinh lúc build: thêm task sinh tệp, khó kiểm hơn, vẫn phải chỉ làm ở staging.
- Không chọn: tham số `-P` như `-PadUnits`: dev phải gõ mỗi lần build, Android Studio không truyền; câu 40 đã chọn `local.properties`.

Hôm nay không file build nào đọc `local.properties`: Android Gradle Plugin tự đọc `sdk.dir`, `scripts/cloud-setup.sh` ghi `sdk.dir`, `scripts/release.sh` chép tệp vào thư mục build production. Không cần `build-logic/`: cờ `buildConfig = true` đã bật trong `AndroidApplicationConventionPlugin.kt`, khối flavor nằm trong `app/build.gradle.kts`.

**Bước 1, `app/build.gradle.kts`.** Dòng đầu của file thêm `import java.util.Properties` (trong khối `android { }`, tên `java` trỏ tới phần mở rộng `java` của Gradle, nên viết đủ `java.util.Properties` không biên dịch được; chạy thử ở mục 4.1). Trong khối `named("staging")`, sau dòng `APPSFLYER_DEV_KEY`:

```kotlin
// A developer's own phones as AdMob test devices (T-058, KB-28, question 40 of the ads spec): the key
// ads.testDeviceIds of local.properties, never in git. Only this flavor reads it; CI has no such key.
val adsTestDeviceIds = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
    .asText.orNull
    ?.let { text -> Properties().apply { load(text.reader()) }.getProperty("ads.testDeviceIds") }
    .orEmpty()
    .split(',')
    .map { it.trim() }
    .filter { it.matches(Regex("[0-9A-Za-z]+")) }
    .joinToString(",")
buildConfigField("String", "ADS_TEST_DEVICE_IDS", "\"$adsTestDeviceIds\"")
```

- Lọc theo ký tự để giá trị không phá chuỗi Java của `BuildConfig` (dấu ngoặc kép, dấu `\`). Mã sai dạng bị bỏ, không làm build hỏng: khối flavor cấu hình ở mọi build, kể cả build production của `release.sh`, nên ném lỗi ở đây sẽ chặn bản phát hành vì một dòng của máy dev. Dev thấy số máy đã đặt qua dòng log ở bước 4.
- Không in giá trị ra log của Gradle.
- Khối `production` không đổi.

**Bước 2, `:core:ads`, tệp mới `AdsDebugOptions.kt`** (theo mẫu `ConsentDebugOptions.kt`):

- `data class AdsDebugOptions(val testDeviceIds: List<String>, val logNetworkResponses: Boolean)`, `companion object`: `NONE = AdsDebugOptions(emptyList(), false)` và `fun staging(rawTestDeviceIds: String) = AdsDebugOptions(testDeviceIdsOf(rawTestDeviceIds), logNetworkResponses = true)`.
- `internal fun testDeviceIdsOf(raw: String): List<String>`: tách theo dấu phẩy, bỏ khoảng trắng, bỏ mục rỗng, bỏ mục trùng, giữ thứ tự.
- `internal fun requestConfigurationWithTestDevices(base: RequestConfiguration, ids: List<String>): RequestConfiguration?`: `null` khi `ids` rỗng; không thì `base.toBuilder().setTestDeviceIds(ids).build()` (giữ các trường khác của cấu hình hiện có, để task sau đặt thêm trường không bị đè).
- `internal fun applyTestDevices(options: AdsDebugOptions, current: () -> RequestConfiguration, set: (RequestConfiguration) -> Unit, logger: AppLogger)`: gọi hàm trên; có cấu hình thì `set(it)` và `logger.info(TAG, "Test devices set: <số mã>")`; chỉ ghi số mã, không ghi mã.

**Bước 3, `:core:ads`, tệp mới `AdNetworkResponses.kt`:**

- `internal data class NetworkResponse(val adapterClassName: String, val latencyMillis: Long, val errorCode: Int?)`.
- `internal fun networkResponsesOf(info: ResponseInfo?): List<NetworkResponse>`: chỉ đọc `adapterResponses` và ba getter `adapterClassName`, `latencyMillis`, `adError?.code` của mỗi mục.
- `internal fun networkResponseLines(key: String, responses: List<NetworkResponse>, loadedAdapterClassName: String?): List<String>`: một dòng mỗi mục theo dạng của mục 2; rỗng thì `listOf("Network $key: none tried")`. Hàm thuần.
- `internal class AdNetworkLog(private val enabled: Boolean, private val logger: AppLogger)`: `fun loaded(key: String, info: ResponseInfo?)` (lấy `info?.loadedAdapterResponseInfo?.adapterClassName` làm mạng được chọn) và `fun failed(key: String, error: LoadAdError)` (đọc `error.responseInfo`, không bao giờ `error.message`); `enabled` tắt thì không đọc gì, không ghi gì. Mức `info`, như dòng `Adapter ...` của T-015 (câu hỏi 2). `companion object { val OFF }` không ghi gì.

**Bước 4, `AdsController.kt`:**

- Tham số mới của constructor `debugOptions: AdsDebugOptions` (sau `consentDebug`); `internal val networkLog = AdNetworkLog(debugOptions.logNetworkResponses, logger)`.
- `startSdk()`: dòng đầu của `scope.launch` ngoài, trước `withContext(Dispatchers.IO)` và `MobileAds.initialize`, gọi `applyTestDevices(debugOptions, MobileAds::getRequestConfiguration, MobileAds::setRequestConfiguration, logger)`. Không có máy thử thì không gọi SDK (production như hôm nay).
- Gọi `networkLog` ở mọi callback tải mà SDK trả về:
  - `preloadInterstitial`: `onAdLoaded` gọi `networkLog.loaded(placement.key, ad.responseInfo)`, `onAdFailedToLoad` gọi `networkLog.failed(placement.key, error)`;
  - tải app open (`onAdLoaded`, `onAdFailedToLoad`, cùng chỗ với `telemetry` hiện có): như trên;
  - `sendRewardedLoad`: như trên;
  - native toàn màn hình (`forNativeAd` và `onAdFailedToLoad` của `AdListener` trong `preloadNativeFullScreen`): như trên, khóa `placement.key`;
  - bốn chỗ tạo `SdkNativeAdLoader` (pool, nguồn một quảng cáo, `nativeLoadOf`, `loadNativeAd`): truyền `networkLog` và đúng khóa `placementKey` mà `nativeLoadOnce` hay `loadOneNativeAd` ở chỗ đó nhận (`POOL_LOG_KEY`, `placement.key`, `nativeSlotKey(placement, format)`).
- Đường "request not built" (ngoại lệ trước khi gửi, mã 0) không có `ResponseInfo` và không phải kết quả của SDK: không ghi dòng `Network`.

**Bước 5, `SdkNativeAdLoader.kt`:** `SdkNativeAdLoader` thêm `networkLog: AdNetworkLog = AdNetworkLog.OFF` và `logKey: String = ""`; trong `forNativeAd` gọi `networkLog.loaded(logKey, ad.responseInfo)`; `NativeAdLoadListener` thêm `onLoadFailedResponse: (LoadAdError) -> Unit = {}` gọi trong `onAdFailedToLoad` trước `onFailed`, và `SdkNativeAdLoader` truyền `{ networkLog.failed(logKey, it) }`. Giá trị mặc định giữ nguyên hai test có sẵn của `NativeAdLoadListenerTest`; test quét ở mục 4 bắt chỗ tạo trong `src/main` quên truyền.

**Bước 6, `BannerAdView.kt` và `AdSlot.kt`:** `SdkBannerAdView` thêm `networkLog: AdNetworkLog = AdNetworkLog.OFF`; `onAdLoaded` gọi `networkLog.loaded(slotKey, adView.responseInfo)`, `onAdFailedToLoad` gọi `networkLog.failed(slotKey, error)`. `AdSlot.kt` truyền `networkLog = ads.networkLog`.

**Bước 7, `:app`.** `StagingAdsModule`: `@Provides fun adsDebugOptions(): AdsDebugOptions = AdsDebugOptions.staging(BuildConfig.ADS_TEST_DEVICE_IDS)`. `ProductionAdsModule`: `@Provides fun adsDebugOptions(): AdsDebugOptions = AdsDebugOptions.NONE`. KDoc của hai module nói thêm một câu.

**Bước 8, `docs/ops/ads-guide.md`.** Mục 7, thêm mục con "### Thử mediation trên máy thật (T-058)":

- lấy mã máy: cài bản staging, đồng ý quảng cáo, `adb logcat | grep setTestDeviceIds`, chép mã trong `Arrays.asList("...")`;
- ghi `ads.testDeviceIds=<mã>,<mã>` vào `local.properties` ở gốc repo (không commit; tệp đã có trong `.gitignore`), dựng lại bằng `./gradlew :app:installStagingDebug`; bản của `staging.yml` không bao giờ có máy thử;
- nhận biết: log thẻ `VGL.Ads` có `Test devices set: <n>`; không có dòng này thì kiểm lại khóa (mã sai dạng bị bỏ);
- log từng mạng: dạng dòng `Network ...`, ý nghĩa `loaded`, `no error`, `error <mã>`, `none tried`; trên staging của dự án mẫu chỉ có adapter AdMob (FB-20261023-01); app có mã thật mới thấy cả nhóm mediation; mã lỗi tra ở trang `LoadAdError` của Google;
- bản dựng có mã máy thử là của dev, không gửi cho người khác; log của SDK (thẻ `Ads` của Google, không phải `VGL.Ads`) có in mã máy: không dán logcat thô vào repo (`docs/CLAUDE.md`, mục Khi phân tích một phiên thử).

Mục 8, câu Ad Inspector: thêm "hoặc máy đã khai báo trong `ads.testDeviceIds` (mục 7)".

Không đổi spec, `ads-business-rules.md` (đây là công cụ cho dev, câu trả lời 40), hay ADR.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/build.gradle.kts` | `import java.util.Properties` ở đầu file; khối `named("staging")` đọc `ads.testDeviceIds` của `local.properties`, lọc ký tự, trường `ADS_TEST_DEVICE_IDS` (bước 1) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsDebugOptions.kt` | `AdsDebugOptions`, `testDeviceIdsOf`, `requestConfigurationWithTestDevices`, `applyTestDevices` (bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdNetworkResponses.kt` | `NetworkResponse`, `networkResponsesOf`, `networkResponseLines`, `AdNetworkLog` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Tham số `debugOptions`, `networkLog`, gọi `applyTestDevices` trong `startSdk`, gọi `networkLog` ở callback tải của interstitial, app open, rewarded, native toàn màn hình, truyền vào bốn chỗ tạo `SdkNativeAdLoader` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | `networkLog`, `logKey` của `SdkNativeAdLoader`; `onLoadFailedResponse` của `NativeAdLoadListener` (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerAdView.kt` | `networkLog` của `SdkBannerAdView` (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | Truyền `ads.networkLog` (bước 6) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/di/StagingAdsModule.kt` | `adsDebugOptions()` từ `BuildConfig.ADS_TEST_DEVICE_IDS` (bước 7) |
| Sửa | `app/src/production/kotlin/com/vglstudio/template/di/ProductionAdsModule.kt` | `adsDebugOptions()` trả `AdsDebugOptions.NONE` (bước 7) |
| Sửa | `docs/ops/ads-guide.md` | Mục 7, mục con thử trên máy thật; mục 8, câu Ad Inspector (bước 8) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsDebugOptionsTest.kt` | Test của bước 2 (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt` | Test của bước 3 (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkLogWiringTest.kt` | Test quét `src/main` của `:core:ads`: mọi đường tải gọi `networkLog`, `applyTestDevices` trước `MobileAds.initialize` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerFixtures.kt` | `testAdsController` thêm `debugOptions: AdsDebugOptions = AdsDebugOptions.NONE` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/SdkBannerAdViewTest.kt` | Hai test mới cho log từng mạng của banner (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdLoadListenerTest.kt` | Một test mới cho `onLoadFailedResponse` (mục 4) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` | Hai test mới: chỉ khối staging đọc khóa, production không có trường (mục 4) |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/AdsDebugOptionsSourceSetTest.kt` | Test quét source set của `:app`: chỉ `src/staging` nhắc `ADS_TEST_DEVICE_IDS`, `src/production` cấp `AdsDebugOptions.NONE` (mục 4) |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/di/StagingAdsModuleTest.kt` | Test `StagingAdsModule.adsDebugOptions()` (mục 4) |

Nơi đọc các file này (L-020):

- `app/build.gradle.kts`: `ProductionPropertiesTest` (trong bảng); `scripts/setup-check.sh` đọc `ASSETS_BASE_URL` của staging bằng `grep` dòng `"ASSETS_BASE_URL", *"\"https://` và `applicationId` (không đổi hai dòng đó); `scripts/release.sh` đọc `applicationId`, `versionCode`, `versionName` (không đổi); `scripts/new-app.sh` thay địa chỉ R2 staging (không đổi); `scripts/tests/run.sh` (dòng 1311 tới 1322) kiểm mọi tham số `release.sh` xuất ra đều được đọc bằng `gradleProperty(...)` và đọc khối `missingProductionProperties` (không đổi; khóa mới đọc từ `local.properties`, không phải `gradleProperty`); `scripts/check.sh` và `.github/workflows/pr-check.yml` chỉ dò tên file build, không đọc nội dung. Không ảnh hưởng.
- `AdsController.kt`, `SdkNativeAdLoader.kt`, `BannerAdView.kt`, `AdSlot.kt`: `NativeRequestOptionsTest.onlyNativeRequestOptions_buildsNativeAdOptions` quét `src/main` của `:core:ads` tìm `NativeAdOptions`; hai tệp mới không import lớp đó. `scripts/ads-plan.py` chỉ nhắc `ads-guide.md` trong chuỗi, không đọc tệp.
- `StagingAdsModule.kt`, `ProductionAdsModule.kt`: không script hay test nào đọc theo tên. `AdGallerySourceSetTest` quét `src/` của `:app` tìm chữ `AdGallery`, không đụng.
- `ads-guide.md`: không script, workflow hay test nào đọc nội dung mục 7, 8.
- `local.properties`: không có trong repo, plan không tạo; `scripts/cloud-setup.sh` chỉ ghi thêm `sdk.dir` (không đụng khóa mới).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Không thêm khóa Remote Config.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; không đổi định dạng, tần suất, SDK hay adapter. Đổi cấu hình yêu cầu ở staging khi dev khai báo máy thử (`RequestConfiguration.testDeviceIds`), đặt trong `startSdk` trước `MobileAds.initialize`: thứ tự khởi tạo SDK không đổi (vẫn sau cổng đồng ý, S-01 tới S-19). Production không gọi `setRequestConfiguration`.
- Sự kiện phân tích: Không chạm. Dòng log mức `info` của staging đi vào Crashlytics và chỉ gửi kèm báo cáo crash (ADR-0019); nội dung là tên lớp adapter, số mili giây, mã lỗi số, khóa vị trí: không có dữ liệu cá nhân, mã đơn vị hay mã máy.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `RequestConfiguration`, `ResponseInfo`, `AdapterResponseInfo`, `LoadAdError.responseInfo` thuộc `play-services-ads` (khóa `play-services-ads`, `playServicesAds = "25.5.0"` trong `gradle/libs.versions.toml`), đã là phụ thuộc `implementation` của `:core:ads`; `:app` không gọi lớp nào của SDK. `java.util.Properties` của file build thuộc JDK (import ở đầu file, bước 1). Test dùng `mockk` đã có.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** (a) `mockk` có giả được lớp `final` `ResponseInfo` và `AdapterResponseInfo` của SDK trong test Robolectric của `:core:ads` không (dự án hôm nay chỉ giả lớp trừu tượng như `NativeAd`); (b) `RequestConfiguration.Builder` và `toBuilder()` có chạy được trên JVM của Robolectric không; (c) `providers.fileContents(...)` trong khối flavor có chạy với mọi task của `check.sh` không.
- **Điều kiện chuyển:** (a), (b): test của `AdNetworkResponsesTest` hay `AdsDebugOptionsTest` đỏ vì không tạo được bản giả hay đối tượng của SDK (lỗi `MockKException`, `NoClassDefFoundError`, `RuntimeException("Stub!")`) sau 2 lần sửa; (c): Gradle báo lỗi cấu hình ở khối staging.
- **Cách làm thay:** (a) test `networkResponseLines` trên danh sách `NetworkResponse` dựng tay, và test `AdNetworkLog.failed` với `LoadAdError(3, ..., null)` thật (`responseInfo` rỗng, cho dòng `none tried`); `networkResponsesOf` (ánh xạ getter) vào ngoại lệ viết test trước, rà soát đọc tay rằng nó không gọi `getCredentials`, `toString`, `message`. (b) `requestConfigurationWithTestDevices` đổi thành hàm thuần trả `List<String>?` (`null` khi rỗng), `applyTestDevices` nhận `set: (List<String>) -> Unit`, phần dựng `RequestConfiguration` nằm trong lambda của `AdsController` và vào ngoại lệ viết test trước; test quét thứ tự trong `startSdk` giữ nguyên. (c) đọc bằng `rootProject.file("local.properties").takeIf { it.isFile }?.reader()`, cùng lọc ký tự; dự án không bật configuration cache (`gradle.properties`). Tiêu chí và tên test ở mục 4 giữ nguyên.
- **Kết quả chạy thử (2026-10-29, mục 4.1):** cả ba điểm qua, không cần cách làm thay nào. Một lỗi biên dịch không nằm trong ba điểm trên (viết đủ `java.util.Properties` trong khối `android { }`) đã sửa thẳng ở bước 1 bằng `import`, không phải phương án dự phòng.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py docs/features/ads/plans/T-058-may-thu-va-log-tung-mang.md` (2026-10-29): xem kết quả ở cuối mục này.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `AdsController` không có tham số cấu hình gỡ lỗi quảng cáo | Tham số `debugOptions` (bước 4) | Mọi test dựng `AdsController` qua `testAdsController` (`AdsControllerFixtures.kt`) | Giữ: tham số mặc định `AdsDebugOptions.NONE`, không dòng log mới, không gọi SDK mới; chỉ thêm tham số vào fixture |
| Callback tải ghi telemetry và dòng log hiện có | Thêm `networkLog.loaded`/`failed` khi `logNetworkResponses` bật (bước 4 tới 6) | `SdkBannerAdViewTest` (dựng `SdkBannerAdView` không có `networkLog`), `NativeAdLoadListenerTest` (dựng `NativeAdLoadListener(events) {}`), các test so cả danh sách `RecordingAppLogger.entries` của controller | Giữ: mặc định `AdNetworkLog.OFF` và fixture `NONE` không ghi gì, nên danh sách log và sự kiện của test cũ không đổi |
| `startSdk` chỉ gọi `MobileAds.initialize` | Gọi `applyTestDevices` trước (bước 4), không gọi SDK khi không có máy thử | Không test nào chạy `startSdk` (cổng đồng ý thật cần UMP) | Giữ: không có câu khẳng định cũ |
| `StagingAdsModule`, `ProductionAdsModule` cấp ba giá trị | Cấp thêm `AdsDebugOptions` (bước 7) | `StagingRemoteValuesModuleTest`, `GalleryAdStatusTest` không đọc hai module này | Giữ |
| Khối `staging` của `app/build.gradle.kts` không đọc `local.properties` | Đọc khóa `ads.testDeviceIds` (bước 1) | `ProductionPropertiesTest.stagingKey_isFixedEmpty` khẳng định khối staging không chứa `appsFlyerDevKey` và có `APPSFLYER_DEV_KEY` rỗng | Giữ: khối mới không nhắc AppsFlyer |
| Native toàn màn hình, interstitial, app open, rewarded, pool, nguồn một quảng cáo, banner: vòng nạp, thử lại, telemetry | Không đổi | Cần xem trước: `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` (đếm dòng log có `not done in time`, dựng controller thật qua `testAdsController`). Nên xem: `AdsControllerNativeComboTest.standalone_noAd_showsNothing_finishesAtOnce`, `FullScreenAdGuardTest.quietPeriodRule_isGone`, `FullScreenAdsTest.withoutController_actionDoesNothing`, `FullScreenPreloadSlotTest.rewardedSlot_loadsThroughLoadFullScreenAdIfNeeded`, `FullScreenReloadsTest.perPlacementKeys_keepFailuresAndLatencyApart`, `FullScreenReloadsTest.successResetsDelay`, `FullScreenReloadsTest.twoPlacements_failingTogether_retryOnTheirOwnSchedule`, `LoadAttemptCounterTest.attemptCountsFailuresInARow`, `LoadAttemptCounterTest.callbackWithoutRequest_hasNoLatency`, `NativeAdLoadListenerTest.impressionBeforeLoaded_doesNotCrash`, `NativeAdLoadListenerTest.onAdImpression_marksLoadedAd_andForwardsEvent`, `NativeAdRefreshTest.loaderThrows_countsAsCode0_keepsAd`, `NativeAdRetryTest.loaderNotBuilt_reportsCode0ToEventsAndRetriesAt5s`, `NativeAdSourcesTest.twoSlotsOverlap_loadAttemptAndLatencyPerRequest`, `NativeEligibleCoverageTest.everyFileDrawingANativeAd_reportsEligible`, `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags`, `RewardedTurnsTest.decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd`, `SdkBannerAdViewTest.barBanner_reportsBannerFormatAndBarPlacement`, `SdkBannerAdViewTest.listenerResults_reachResultsAndMetrics` | Giữ. Đã đọc từng test: chúng đi qua `testAdsController` (mặc định `NONE`), dựng `NativeAdLoadListener` và `SdkBannerAdView` với giá trị mặc định mới (`OFF`), hay chỉ dùng `onFailed`, `onLoaded` của `NativeAdEvents` và bộ đếm, mà plan không đổi. `RemovedPacingRulesTest` kiểm constructor không nhận `RemoteSettings` hay kho lịch sử và có nhận `AdsFlags`: tham số `AdsDebugOptions` mới không phải hai loại đó, câu vẫn đúng. `NativeEligibleCoverageTest` quét tệp gọi `BoundNativeAd(`: hai tệp mới không gọi. `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` đếm dòng chứa `not done in time`: dòng `Network` không chứa chuỗi đó và fixture không bật log |

Kết quả `existing-tests.py`: 272 test dùng tên hay tag của sáu tệp Kotlin plan sửa; 1 test "Cần xem trước" và 19 test "Nên xem", đều ở dòng cuối của bảng; 251 test còn lại chỉ dùng tên mà plan không nêu. Không có câu khẳng định nào phải viết lại: mọi thay đổi chỉ bật ở staging qua tham số mới có giá trị mặc định tắt.

## 4. Kiểm thử

Test JVM của `:core:ads` chạy bằng Robolectric (`AndroidJUnit4`), như `SdkBannerAdViewTest`; bản giả lớp SDK bằng `mockk`. Mã máy mẫu trong test là chuỗi giả `33BE2250B43518CCDA7DE426D04EE231` (mã ví dụ trong tài liệu của Google, không phải máy thật); mã đơn vị mẫu là mã thử của Google `ca-app-pub-3940256099942544/1033173712`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-28, tiêu chí 1: đọc khóa, tách mã | Unit test | `AdsDebugOptionsTest.testDeviceIdsOf_splitsTrimsDropsBlankAndDuplicates`: `" 33BE2250B43518CCDA7DE426D04EE231 , ,ABCDEF0123456789ABCDEF0123456789,33BE2250B43518CCDA7DE426D04EE231,"` cho đúng hai mã theo thứ tự; đối chứng: `""` cho danh sách rỗng, `"A"` cho `["A"]` (ghi rồi đọc lại, L-055) |
| KB-28, tiêu chí 1: staging bật log, có mã | Unit test | `AdsDebugOptionsTest.staging_keepsIdsAndLogsNetworks`: `AdsDebugOptions.staging("A,B")` có `testDeviceIds == ["A", "B"]` và `logNetworkResponses == true`; `staging("")` có danh sách rỗng và vẫn bật log; đối chứng `NONE`: rỗng, tắt |
| KB-28, tiêu chí 1: cấu hình yêu cầu có máy thử, giữ trường khác | Unit test | `AdsDebugOptionsTest.requestConfiguration_hasTestDevices_keepsOtherFields`: `base = RequestConfiguration.Builder().setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G).build()`; với `["A", "B"]` kết quả có `testDeviceIds == ["A", "B"]` và `maxAdContentRating == MAX_AD_CONTENT_RATING_G`; với `[]` kết quả `null` |
| KB-28, tiêu chí 1: đặt một lần, log chỉ có số mã | Unit test | `AdsDebugOptionsTest.applyTestDevices_setsOnce_logsCountOnly`: với `staging("33BE2250B43518CCDA7DE426D04EE231")`, lambda `set` nhận đúng một cấu hình có mã đó; `RecordingAppLogger` có đúng một mục `INFO`, thẻ `Ads`, chữ `Test devices set: 1`; không mục nào chứa `33BE2250` |
| KB-28, tiêu chí 1: đặt trước khi khởi tạo SDK | Unit test (quét mã nguồn) | `AdNetworkLogWiringTest.startSdk_setsTestDevicesBeforeInitialize`: trong thân `private fun startSdk()` của `AdsController.kt` (bỏ dòng chú thích), `applyTestDevices(` có mặt và đứng trước `MobileAds.initialize(`; đối chứng: thân hàm tìm được có `MobileAds.initialize(` |
| KB-28, tiêu chí 1: Ad Inspector mở trên máy thật đã khai báo | Kiểm tra thủ công (máy thật: khác, emulator luôn là máy thử của SDK nên không phân biệt được có khai báo hay không) | Dev ghi mã máy của mình vào `ads.testDeviceIds`, `./gradlew :app:installStagingDebug`, đồng ý quảng cáo: logcat `VGL.Ads` có `Test devices set: 1`; dòng gợi ý `setTestDeviceIds` của SDK (thẻ `Ads` của Google) không còn; màn xem thử, "Open Ad Inspector" mở Ad Inspector, không có thông báo lỗi kèm mã. Đối chứng: xóa khóa, dựng lại: không có `Test devices set`, dòng gợi ý của SDK trở lại, máy chưa đăng ký trên AdMob thì nút báo lỗi kèm mã như trước |
| KB-28, tiêu chí 2: không khai báo thì như hiện tại | Unit test | `AdsDebugOptionsTest.applyTestDevices_none_callsNothing`: với `NONE` và với `staging("")`, lambda `set` và `current` không được gọi, logger không có mục nào; đối chứng: cùng lambda với `staging("A")` được gọi một lần (cùng test, để test không qua trên khung rỗng) |
| KB-28, tiêu chí 2: chỉ khối staging đọc khóa | Unit test (quét file build) | `ProductionPropertiesTest.testDeviceIds_readOnlyByStagingFlavor`: khối `staging` (hàm `flavor` có sẵn) chứa `"ads.testDeviceIds"`, `local.properties` và `buildConfigField("String", "ADS_TEST_DEVICE_IDS"`; số lần `ads.testDeviceIds` xuất hiện trong cả file bằng số lần trong khối staging (không có chỗ đọc ở cấp trên cùng mà khối khác dùng được); đối chứng: khối đọc được có `ASSETS_BASE_URL` |
| KB-28, tiêu chí 2: production không có trường | Unit test (quét file build) | `ProductionPropertiesTest.production_hasNoTestDeviceField`: khối `production` không chứa `ADS_TEST_DEVICE_IDS`, `testDeviceIds` hay `local.properties`; đối chứng: khối có `AD_UNITS` |
| KB-28, tiêu chí 2: production không có đường đọc | Unit test (quét source set) | `AdsDebugOptionsSourceSetTest.onlyStaging_readsTestDeviceField`: trong `app/src`, trừ `staging`, `test`, `testStaging`, không tệp `.kt` nào có ký hiệu `ADS_TEST_DEVICE_IDS` ở dòng không phải chú thích; đối chứng: `src/staging/.../StagingAdsModule.kt` có ký hiệu đó. `AdsDebugOptionsSourceSetTest.production_providesNone`: `src/production/.../ProductionAdsModule.kt` có `AdsDebugOptions.NONE`, không có `AdsDebugOptions.staging(` |
| KB-28, tiêu chí 2: staging lấy từ BuildConfig | Unit test (`testStaging`) | `StagingAdsModuleTest.adsDebugOptions_readsBuildConfig_andLogsNetworks`: `StagingAdsModule.adsDebugOptions() == AdsDebugOptions.staging(BuildConfig.ADS_TEST_DEVICE_IDS)`, `logNetworkResponses == true`, và `BuildConfig.ADS_TEST_DEVICE_IDS` chỉ gồm `[0-9A-Za-z,]` (bộ lọc của file build chạy, kể cả trên máy dev có khóa). Không khẳng định giá trị rỗng: trên máy dev có khóa, giá trị khác rỗng là đúng |
| KB-28, tiêu chí 3: một dòng mỗi mạng, theo thứ tự SDK | Unit test | `AdNetworkResponsesTest.lines_onePerNetwork_inSdkOrder`: ba `NetworkResponse` (`com.google.ads.mediation.admob.AdMobAdapter`, 120, 3), (`com.google.ads.mediation.applovin.AppLovinMediationAdapter`, 80, null), (`com.google.ads.mediation.facebook.FacebookMediationAdapter`, 40, null), mạng được chọn là AppLovin, khóa `gallery_interstitial`: đúng ba dòng `Network gallery_interstitial: com.google.ads.mediation.admob.AdMobAdapter, 120 ms, error 3`, `... AppLovinMediationAdapter, 80 ms, loaded`, `... FacebookMediationAdapter, 40 ms, no error` |
| KB-28, tiêu chí 3: không mạng nào | Unit test | `AdNetworkResponsesTest.lines_noNetwork_noneTried`: danh sách rỗng cho đúng `["Network gallery_interstitial: none tried"]`; `networkResponsesOf(null)` rỗng |
| KB-28, tiêu chí 3: chỉ đọc tên lớp, thời gian, mã | Unit test | `AdNetworkResponsesTest.networkResponsesOf_readsOnlyClassLatencyAndCode`: `ResponseInfo` giả (`mockk`) có hai `AdapterResponseInfo` giả, mục đầu có `adError` giả mã 3 với `message` chứa mã đơn vị mẫu, cả hai có `credentials` (`Bundle` chứa mã đơn vị mẫu): kết quả đúng hai `NetworkResponse`; `verify(exactly = 0)` với `getCredentials()`, `getAdSourceInstanceName()`, `AdError.getMessage()` |
| KB-28, tiêu chí 3: không mã đơn vị, không thông báo SDK, chỉ ở staging | Unit test | `AdNetworkResponsesTest.log_enabled_writesLines_disabled_writesNothing`: `AdNetworkLog(true, logger).failed("gallery_interstitial", LoadAdError(3, "No fill for ca-app-pub-3940256099942544/1033173712", "com.google.android.gms.ads", null, null))` cho đúng một mục `INFO` thẻ `Ads` `Network gallery_interstitial: none tried`; `loaded("k", <ResponseInfo giả của dòng trên>)` cho hai dòng; không mục nào chứa `ca-app-pub` hay `No fill`. Đối chứng: `AdNetworkLog(false, logger)` với cùng lời gọi không ghi mục nào, và `AdNetworkLog.OFF` cũng vậy |
| KB-28, tiêu chí 3: banner, mọi kết quả (L-072) | Unit test | `SdkBannerAdViewTest.networkLog_onLoadResults_logsAtStaging`: `SdkBannerAdView(..., networkLog = AdNetworkLog(true, logger))`, `onAdFailedToLoad(LoadAdError(3, ..., null))` rồi `onAdLoaded()` (AdView của Robolectric không có `responseInfo`): đúng hai mục `Network sample_native_small: none tried`. `SdkBannerAdViewTest.networkLog_defaultOff_logsNothing`: banner dựng như test cũ (mặc định) không có mục `Network` nào sau hai callback |
| KB-28, tiêu chí 3: native qua `SdkNativeAdLoader`, lượt lỗi | Unit test | `NativeAdLoadListenerTest.onAdFailedToLoad_handsErrorToResponseLog_beforeOnFailed`: `NativeAdLoadListener(events, onFailed = { order += "failed:$it" }, onLoadFailedResponse = { order += "response:${it.code}" })`, `onAdFailedToLoad(LoadAdError(3, ...))`: `order == ["response:3", "failed:3"]` và `events.onFailed(3, ...)` được gọi một lần |
| KB-28, tiêu chí 3: mọi đường tải gọi log (L-072) | Unit test (quét mã nguồn) | `AdNetworkLogWiringTest.everySdkLoadCallback_logsNetworks`: với mỗi tệp `.kt` của `core/ads/src/main/kotlin` (bỏ dòng chú thích), số `override fun onAdLoaded(` cộng số `.forNativeAd {` bằng số `networkLog.loaded(`, và số `override fun onAdFailedToLoad(` bằng số `networkLog.failed(` (ở `SdkNativeAdLoader.kt`, lời gọi nằm trong lambda truyền cho `onLoadFailedResponse`); đối chứng: `AdsController.kt` có 4 và 4, `BannerAdView.kt` 1 và 1, `SdkNativeAdLoader.kt` 1 và 1 (đúng như mã hôm nay, mục 3.1). `AdNetworkLogWiringTest.everyLoaderInMain_getsTheNetworkLog`: mỗi lời gọi `SdkNativeAdLoader(` trong `AdsController.kt` (4) có `networkLog` trong cặp ngoặc của nó, và lời gọi `SdkBannerAdView(` trong `AdSlot.kt` có `networkLog =` |
| KB-28, tiêu chí 3: dòng log thật trên máy | Kiểm tra thủ công (máy thật: khác, dòng log chỉ đọc được trong logcat; tệp kịch bản emulator chỉ kiểm giao diện) | Bản staging dựng trên máy dev, đồng ý quảng cáo, mở màn xem thử: `adb logcat -v threadtime \| grep ' VGL\.Ads'` có ít nhất một dòng `Network <khóa>: ...` sau mỗi lượt tải (native small, interstitial, banner); không dòng `Network` nào có `ca-app-pub` hay chuỗi 32 ký tự của mã máy. Ghi lại tên lớp adapter thấy được vào nhật ký (trả lời giả định ở mục 2) |
| Hướng dẫn trong `ads-guide.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người đọc mục 7 "Thử mediation trên máy thật (T-058)" và câu mới của mục 8; làm theo được từ đầu tới cuối trên máy của mình |

**Ngoại lệ viết test trước:** lời văn `docs/ops/ads-guide.md` và KDoc của các tệp (chỉ là chữ). Hai dòng kiểm tra thủ công trên máy thật (không có cách tự động phân biệt máy thử trên emulator). Không có phần nối nào nằm ngoài test: mọi đường tải có test quét gọi log, phần đọc khóa của file build và source set production có test quét.

### 4.1. Chạy thử

- **Đã chạy thử:** rồi, 2026-10-29, trên máy dev (macOS, JDK 21.0.13 và Android SDK từ `scripts/local-setup.sh`, `source` tệp `env.sh`). Nhánh tạm `claude/trial-T-058-local` tách từ `origin/main` (`9cb0cad`), không commit, không đẩy lên, đã xóa sau khi chạy. Code tạm: khối đọc khóa của bước 1 dán vào khối `named("staging")` của `app/build.gradle.kts`, và test tạm `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/TrialSdkObjectsTest.kt` (Robolectric, ba test). `local.properties` của nhánh tạm có `sdk.dir` và `ads.testDeviceIds=33BE2250B43518CCDA7DE426D04EE231, bad"id`.
- **Điểm 1, lớp `final` của SDK và `RequestConfiguration`:** test `finalSdkClasses_canBeMocked` dựng `mockk<ResponseInfo>`, hai `mockk<AdapterResponseInfo>`, `mockk<AdError>` (mã 3, `message` chứa mã đơn vị mẫu), đọc `adapterResponses` ra đúng hai bộ (tên lớp, thời gian, mã lỗi), đọc `loadedAdapterResponseInfo?.adapterClassName`, và `verify(exactly = 0)` với `credentials` và `message` qua. Test `realLoadAdError_withoutResponseInfo_hasNoAdapters`: `LoadAdError(3, "No fill", "com.google.android.gms.ads", null, null)` thật dựng được, `responseInfo` là `null`. Test `requestConfiguration_toBuilder_keepsOtherFields`: `RequestConfiguration.Builder().setMaxAdContentRating(MAX_AD_CONTENT_RATING_G).build().toBuilder().setTestDeviceIds(listOf("A", "B")).build()` có `testDeviceIds == ["A", "B"]` và giữ `maxAdContentRating`. Lệnh `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TrialSdkObjectsTest' :app:generateStagingDebugBuildConfig`: `BUILD SUCCESSFUL in 21s`, `tests="3" failures="0" errors="0"`. Không có `MockKException`, `NoClassDefFoundError` hay `RuntimeException("Stub!")`: điểm chưa chắc (a), (b) của mục 3.4 không xảy ra.
- **Điểm 2, khối đọc khóa của file build:** lần chạy đầu với đúng code của bước 1 (bản version 1 của plan) hỏng lúc biên dịch script: `Line 60: ... java.util.Properties() ... Unresolved reference: util`, kéo theo `Unresolved reference: load` và `it` (4 lỗi, `BUILD FAILED in 3s`). Lý do: trong khối `android { }`, `java` là phần mở rộng `java` của Gradle, không phải gói. Sửa lần 1: thêm `import java.util.Properties` ở đầu file, gọi `Properties()`; bước 1 của mục 3.1 đã ghi theo cách này. Sau khi sửa: `app/build/generated/source/buildConfig/staging/debug/com/vglstudio/template/BuildConfig.java` có `ADS_TEST_DEVICE_IDS = "33BE2250B43518CCDA7DE426D04EE231"` (mã `bad"id` bị bỏ); bỏ khóa khỏi `local.properties` rồi chạy lại `./gradlew :app:generateStagingDebugBuildConfig`: `ADS_TEST_DEVICE_IDS = ""`. Bản production: `./gradlew :app:generateProductionDebugBuildConfig -PadmobAppId=... -PadUnits=x -PassetsBaseUrl=https://example.invalid/ -PappsFlyerDevKey=x` (`BUILD SUCCESSFUL in 1s`): `BuildConfig.java` của production không có `ADS_TEST_DEVICE_IDS`. Không có tham số `-P` thì task này dừng ở `preProductionDebugBuild` với lỗi "Build production cần tham số thật" như hôm nay (khối kiểm của file build, không liên quan tới khóa mới). `providers.fileContents(...)` chạy được với mọi task: điểm chưa chắc (c) không xảy ra.
- **Điểm 3, toàn bộ `scripts/check.sh`:** lần đầu `exit 1` sau 366 giây, chỉ vì `:core:ads:ktlintTestSourceSetCheck` báo một dòng quá 120 ký tự trong chính test tạm (dòng `assertEquals` của `loadedAdapterResponseInfo`); `:app:ktlintKotlinScriptCheck` của file build đã sửa thì qua. Tách dòng đó rồi chạy lại: `exit 0` sau 308 giây, `Test JVM: 1458 test, 0 lỗi. Ảnh screenshot: 89`, `./core/ads: 1069 test, 0 lỗi` (gồm 3 test tạm, `failures="0"`), `./app: 148 test, 0 lỗi`, dòng cuối `== Cổng chất lượng: QUA`.
- **Thêm, vì file build đổi:** `./gradlew :app:assembleStagingRelease`: `BUILD SUCCESSFUL in 2m 10s`, ra `app-staging-release-unsigned.apk`; `BuildConfig.java` của staging release có `ADS_TEST_DEVICE_IDS = "33BE2250B43518CCDA7DE426D04EE231"`.
- **Kết luận:** kỹ thuật dùng được như mục 3.1 (đã sửa bước 1 bằng `import`); không cần phương án dự phòng nào của mục 3.4. Phiên làm task vẫn chạy `scripts/check.sh` như thường; `local.properties` của máy dev có khóa `ads.testDeviceIds` thì `StagingAdsModuleTest` thấy giá trị khác rỗng (mục 4 đã tính).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi cấu hình yêu cầu của SDK quảng cáo ở staging, log trên mọi đường tải), 7 (module core `:core:ads`, cấu hình build `app/build.gradle.kts`). Không chạm 4 (không thêm sự kiện hay dữ liệu gửi đi; log `info` chỉ đi kèm báo cáo crash, ADR-0019), 5 (thứ tự khởi tạo SDK và cổng đồng ý giữ nguyên; `setRequestConfiguration` không khởi động SDK nào), 2, 3 (không thêm SDK hay quyền). Không cần `build-logic/`.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app`) và file build; đổi constructor của `AdsController` và hai lớp nội bộ |
| B. Độ rõ của yêu cầu | 1 | Câu 40 đã trả lời; AI tự quyết dạng dòng log, mức log, cách xử lý mã sai dạng (câu hỏi 2, 5) |
| C. Khả năng kiểm chứng tự động | 2 | Logic và nối có test; tiêu chí 1 (Ad Inspector trên máy thật) chỉ kiểm được trên máy thật đã khai báo |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `AdSlot.kt`, `BannerAdView.kt` và hai module Hilt cũng là file của T-054, T-055, T-062, T-063, T-066, T-068 trong cùng đợt (mục 6) |
| E. Độ mới của kỹ thuật | 2 | Lần đầu file build đọc `local.properties`; lần đầu dùng `RequestConfiguration`, `ResponseInfo.adapterResponses`; lần đầu giả lớp `final` của SDK bằng `mockk`. Kỹ thuật phổ biến, nên 2 chứ không 3 |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn; tổng điểm cũng từ 9) |

Plan rủi ro cao chạm quảng cáo: người phụ trách sản phẩm (anh Nhật) biết trước khi duyệt (`risk-scoring.md`, mục 3). Nhóm một dev: duyệt chéo theo ngoại lệ của mục 3.

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController` (constructor, `startSdk`, bốn callback tải), `SdkNativeAdLoader`, `NativeAdLoadListener`, `SdkBannerAdView`, `AdSlot`, `StagingAdsModule`, `ProductionAdsModule`, khối staging của `app/build.gradle.kts`, `AdsControllerFixtures.kt` (mọi test của controller dùng).
- Task khác trong đợt có thể xung đột:
  - T-068 (SDK MAX): cũng đọc `local.properties` ở staging (khóa MAX) và khởi tạo trong `startSdk`. Đề xuất: plan chạy sau dùng lại cách đọc của plan chạy trước; nếu T-068 chạy sau thì tách khối đọc thành một hàm ở cấp trên cùng `fun localProperty(name: String): String?` và test quét của T-058 vẫn đếm chữ `ads.testDeviceIds` trong khối staging (câu hỏi 4). `applyTestDevices` đặt trước mọi phần khởi tạo của T-068.
  - T-054, T-055, T-066: sửa `AdsController.kt` (cổng toàn màn hình, đồng ý); xung đột dòng có thể ở callback tải và `startSdk`, không xung đột ý.
  - T-062 (banner MREC): sửa `AdSlot.kt`, `BannerAdView.kt`; banner MREC đi qua `SdkBannerAdView` nên tự có log từng mạng.
  - T-063 (cổng không hiện quảng cáo): thêm một giá trị Hilt cấp từ `:app`, có thể vào cùng hai module `StagingAdsModule`, `ProductionAdsModule` và constructor của `AdsController`, `AdsControllerFixtures.kt`.
  - T-056 (mã theo khe): sửa `ProductionAdsModule` (`ConfiguredAdUnitIds`) nếu đổi chữ ký.
- Đề xuất thứ tự chạy: T-058 độc lập về ý, chạy được trước hay sau các task trên; nên chạy trước T-068 để T-068 có sẵn chỗ đặt `RequestConfiguration` và cách đọc `local.properties`. Task chạy sau cập nhật dữ liệu của `AdsControllerFixtures.kt` theo ADR-0029.

## 7. Điều kiện dừng riêng

- Phải đọc `local.properties` hay khóa `ads.testDeviceIds` ở chỗ mà khối `production` hay mã của `src/production` dùng được (cấp trên cùng của file build mà production gọi, `ProductionAdsModule`), hay phải sửa `scripts/release.sh` để bản production không có máy thử: dừng (điều kiện 7, cấu hình production).
- Một dòng log mới phải có mã đơn vị, mã máy, `getCredentials()` hay thông báo lỗi của SDK thì mới kiểm được tiêu chí: dừng (trái ADR-0019 và `ads-business-rules.md`, mục 8).
- Đặt máy thử làm đổi thứ tự khởi tạo hay cổng đồng ý (ví dụ phải gọi SDK trước khi cổng mở): dừng (điểm chặn 5, plan không nêu).

## 8. Câu hỏi cho người duyệt

1. **Tiêu chí 3 trên staging của dự án mẫu.** Staging dùng mã thử của Google, không có nhóm mediation, nên mỗi lượt tải chỉ có một dòng của adapter AdMob; chín mạng chỉ thấy ở app có mã thật (và chỉ khi staging của app đó dùng mã thật, điều ADR-0007 không cho). Đề xuất: coi tiêu chí đạt khi mỗi lượt tải có dòng `Network`, ghi giới hạn này vào `ads-guide.md` như FB-20261023-01; log nhiều mạng kiểm ở app đầu tiên có mã thật, bằng "single ad source testing" của Ad Inspector. Anh muốn thêm gì khác?
2. **Mức log.** Đề xuất `info` như dòng `Adapter ...` của T-015: thấy trong logcat và trong vệt log đi kèm báo cáo crash của staging. Mỗi lượt tải có 1 tới 10 dòng, nên vệt log của Crashlytics (giữ phần cuối) có thể bị đẩy mất dòng cũ khi màn xem thử tải nhiều. Phương án khác: `debug` (chỉ logcat). Anh chọn?
3. **Hộp thoại đồng ý trên máy thật.** Theo tài liệu UMP, vùng giả EEA của staging (S-17) chỉ áp cho emulator và máy khai báo bằng `ConsentDebugSettings.Builder.addTestDeviceHashedId`, nên trên máy thật hôm nay có thể không thấy hộp thoại EEA. Có thể dùng cùng khóa `ads.testDeviceIds` (mã của UMP là mã băm, có thể khác mã của AdMob: chưa kiểm). Đề xuất: không làm trong T-058 (chạm hộp thoại đồng ý, điểm chặn 5); tách ticket mới nếu anh muốn. Anh đồng ý?
4. **Đọc `local.properties` chung với T-068.** Đề xuất ở mục 6: T-058 đọc ngay trong khối staging; plan nào chạy sau mà cần khóa thứ hai thì tách thành hàm chung ở cấp trên cùng của file build. Anh đồng ý, hay muốn T-058 tạo sẵn hàm chung?
5. **Mã sai dạng.** Đề xuất bỏ mã sai dạng mà không làm build hỏng (khối staging cấu hình cả ở build production của `release.sh`), dev nhận biết qua `Test devices set: <n>`. Phương án khác: báo lỗi lúc build staging bằng một kiểm tra trong `doFirst` của task `preStaging*Build` (thêm code build). Anh chọn?
6. **Chạy thử:** đã chạy thử 2026-10-29 (mục 4.1). Ba điểm qua, `scripts/check.sh` qua; code của bước 1 cần thêm `import java.util.Properties` mới biên dịch được, đã sửa ở mục 3.1 (plan version 2). Không dùng phương án dự phòng nào. Không còn câu hỏi ở đây; anh xem lại bước 1 đã sửa.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
