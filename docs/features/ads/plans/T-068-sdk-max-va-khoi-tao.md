---
ticket: T-068
title: SDK AppLovin MAX và khởi tạo sau cổng đồng ý
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 23.9M        # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-29, trung vị 14 task M); chạy thử không cho thấy việc lớn hơn cỡ M (mục 4.1)
blocking_points: [ads, sdk, consent, init-order, core, build-config]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-068: SDK APPLOVIN MAX VÀ KHỞI TẠO SAU CỔNG ĐỒNG Ý

## 1. Tóm tắt

Thêm SDK AppLovin MAX (`com.applovin:applovin-sdk:13.6.4`) và adapter Google của MAX (`com.applovin.mediation:google-adapter:25.5.0.0`) vào `:core:ads`. `AdsController` khởi tạo MAX đúng một lần, sau cổng đồng ý của `:core:ads` và **sau** lời gọi `MobileAds.initialize`, chỉ khi build có khóa SDK MAX và thứ tự `ads_mediation_order` có MAX. MAX đọc chuỗi đồng ý TCF mà UMP ghi; app không gọi `setHasUserConsent` hay `setDoNotSell`. Khóa đi vào bằng tham số build `-PmaxSdkKey` ở production (thiếu thì `preProduction*Build` hỏng khi adapter Google của MAX có trong bản build) và `ads.maxSdkKey` trong `local.properties` ở staging; không có khóa thì MAX không khởi tạo và thứ tự coi như `admob`. Khóa Remote Config `ads_mediation_order` (string, mặc định `max_admob`, giá trị lạ dùng mặc định) vào `AdsSettings`, `AdsFlags`, bảng khóa và file mẫu. `secret-scan.py` chặn khóa MAX trên dòng có tên tham số. Không hiện quảng cáo nào của MAX (T-069 tới T-071).

Chạy thử ngày 2026-10-29 (mục 4.1): cả hai tọa độ có trên Maven Central, **không cần kho Maven mới**; manifest gộp `stagingDebug` **giống hệt** trước và sau (adapter Google của MAX không khai báo gì, SDK MAX là đúng thư viện adapter AppLovin của AdMob đã kéo vào, cùng bản 13.6.4); `stagingReleaseRuntimeClasspath` chỉ thêm `google-adapter:25.5.0.0`; `scripts/check.sh` và `:app:assembleStagingRelease` qua. Phát hiện quan trọng khi đọc bytecode: lần đầu MAX khởi tạo adapter Google, adapter gọi `MobileAds.disableMediationAdapterInitialization`, nên MAX phải khởi tạo sau `MobileAds.initialize` của ta, nếu không chín adapter mediation của AdMob có thể không được khởi tạo (mục 2, câu hỏi 3).

Plan thêm SDK, chạm đồng ý và thứ tự khởi tạo SDK, module core, cấu hình build và file được bảo vệ `gradle/libs.versions.toml`: rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt (chạm quảng cáo và đồng ý, `risk-scoring.md` mục 3).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31 | Khi `ads_mediation_order` là `admob` hay `max` thì chỉ nền tảng đó được nạp và hiện. | T-068 chỉ làm phần nền: khóa, cách đọc, thứ tự có hiệu lực (`MediationOrder.effective`), và không khởi tạo MAX khi thứ tự không có MAX. Phần nạp và hiện là T-069 tới T-071, đọc `AdsController.mediationOrder` của plan này |
| KB-31 | MAX và APS chỉ khởi động sau khi cổng đồng ý của app mở; không bao giờ gửi yêu cầu khi chưa có đồng ý. | MAX khởi tạo ở `startSdk` của `AdsController`, chỉ chạy khi cổng mở lần đầu (`onGateOpened(firstTime = true)`), sau `MobileAds.initialize`. Không có thành phần nào của MAX chạy lúc mở tiến trình: `AppLovinInitProvider` đã bị bỏ từ T-015 và vẫn bị bỏ (manifest gộp giống hệt, mục 4.1). APS không làm (chưa có tài khoản, ADR-0037) |
| KB-31 | Khóa SDK MAX và mã APS không nằm trong repo; bản staging chạy được khi không có chúng. | Khóa chỉ đến từ `-PmaxSdkKey` hay `local.properties` (không vào git, `.gitignore` dòng 4); `secret-scan.py` chặn khóa trên dòng có `maxSdkKey`, `max_sdk_key`, `applovin`. Staging không có khóa: MAX không khởi tạo, thứ tự có hiệu lực là `admob`, app chạy như hôm nay |
| KB-9, vế "Thêm 2026-10-29" | Khóa SDK MAX đi vào app lúc build phát hành (câu hỏi 43); vế "khóa không nằm trong app" chỉ còn đúng cho adapter AppLovin của AdMob. | Khóa MAX vào `BuildConfig.MAX_SDK_KEY`, truyền cho SDK bằng code (`AppLovinSdkInitializationConfiguration.builder(sdkKey)`), không qua meta-data `applovin.sdk.key`: test `MergedManifestTest.mergedManifest_hasNoAppLovinSdkKey` của T-034 giữ nguyên |
| KB-11, KB-12 (S-01 tới S-21) | Không có yêu cầu quảng cáo nào khi chưa có đồng ý; rút đồng ý thì không còn yêu cầu mới. | Khởi tạo MAX theo đúng cổng của AdMob. Rút đồng ý: SDK MAX không có lệnh "tắt"; mọi yêu cầu MAX của T-069 tới T-071 phải hỏi `mayRequestNow()` như AdMob (S-15). MAX đọc lại chuỗi TCF mỗi lần UMP ghi lại. T-068 không gửi yêu cầu quảng cáo nào |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0037 (MAX là nền tảng thứ hai; khởi tạo sau cổng đồng ý, đọc UMP, không viết cứng đồng ý; khóa lúc build, staging không khóa thì không khởi tạo; không `+`; APS chưa làm), ADR-0038 (JBase làm chuẩn, trừ chỗ trái luật: JBase khởi tạo MAX trước đồng ý và viết cứng `setHasUserConsent(true)`), ADR-0004 và ADR-0013 (SDK quảng cáo chỉ trong `:core:ads`, khởi tạo sau đồng ý, mã thật không vào repo), ADR-0026 (luật PARTNER, mục 10 không đưa khóa vào repo), ADR-0027 (mẫu tham số build và kiểm `preProduction*Build` theo classpath). Luật: `ads-business-rules.md` S-01 tới S-21, M-01 tới M-07. Spec ads câu hỏi 18 (đo quyền, đưa vào mục 6), 40 (`local.properties` của máy dev), 41 tới 47.

### Tra cứu: phiên bản và kho

Tra ngày 2026-10-29 từ `maven-metadata.xml` của Maven Central:

| Thư viện | Tọa độ chọn | `latest`, `release` trong metadata | Kho | POM: phụ thuộc |
| --- | --- | --- | --- | --- |
| SDK AppLovin MAX | `com.applovin:applovin-sdk:13.6.4` | `13.6.4` (cập nhật 2026-08-08) | Maven Central: https://repo1.maven.org/maven2/com/applovin/applovin-sdk/maven-metadata.xml | `play-services-ads-identifier:17.1.0`, `play-services-appset:16.0.0`, `androidx.browser:1.4.0` (đã có bản mới hơn trên classpath) |
| Adapter Google của MAX | `com.applovin.mediation:google-adapter:25.5.0.0` | `25.5.0.0` (cập nhật 2026-09-17) | Maven Central: https://repo1.maven.org/maven2/com/applovin/mediation/google-adapter/maven-metadata.xml | `play-services-ads:25.5.0`, khớp đúng bản của dự án. POM không kéo `applovin-sdk` (adapter giả định app đã khai báo SDK) |

- Bản adapter Google của MAX đặt tên theo bản Google Mobile Ads (`25.5.0` cộng số phụ `.0`); `25.5.0.0` là bản dựng cho `play-services-ads` 25.5.0 của dự án. Không chọn bản 25.4 hay cũ hơn.
- SDK MAX và SDK mà adapter AppLovin của AdMob (`com.google.ads.mediation:applovin:13.6.4.2`) kéo vào là **cùng một thư viện** (`com.applovin:applovin-sdk`). Adapter AdMob ghi "Built and tested with AppLovin SDK version 13.6.4" (https://developers.google.com/admob/android/mediation/applovin, Changelog 13.6.4.2), trùng bản mới nhất. Khai báo riêng `13.6.4` không đổi bản Gradle chọn (đo: cả hai đường cùng ra 13.6.4, mục 4.1). Khai báo riêng là cần: code của `:core:ads` gọi thẳng lớp của SDK, không dựa vào thư viện chỉ có nhờ phụ thuộc bắc cầu (L-046). Nâng một bên thì nâng bên kia trong cùng plan.
- Ticket nhắc "kho Maven nếu cần": **không cần**. Không sửa `settings.gradle.kts`; L-067 (chạy thử tải trong môi trường cloud) không áp dụng vì Maven Central đã có trong chính sách mạng của phiên (mọi phiên đã tải từ đó).
- JBase còn dùng `google-ad-manager-adapter` của MAX (câu hỏi 45). Dự án mẫu không dùng Google Ad Manager; plan không thêm (câu hỏi 5).

### Tra cứu: đồng ý của MAX

- Trang quyền riêng tư của MAX cho Android (https://support.applovin.com/en/max/android/overview/privacy/, đọc 2026-10-29): với CMP không phải luồng UMP do MAX tự bật, "you must ensure that the CMP establishes consent status before you initialize the SDK"; các cờ đồng ý thì "you must set these values before you initialize the AppLovin SDK". Trang không nói rõ SDK có tự đọc chuỗi TCF khi app dùng UMP của riêng mình hay không. Về mạng con của MAX: Google, InMobi, Amazon "read the TCF v2 consent strings from ... SharedPreferences"; AppLovin, ironSource, Unity... nhận trạng thái qua chuỗi AC.
- Một trả lời của AppLovin được dẫn trên nhóm hỗ trợ của Google Mobile Ads (https://groups.google.com/g/google-admob-ads-sdk/c/G-OoK0VGnG4, diễn đàn, không phải tài liệu chính thức): từ SDK 12.0.0 AppLovin đọc chuỗi đồng ý từ SharedPreferences; `setHasUserConsent` chỉ cần khi CMP không ghi chuỗi đó hay SDK cũ hơn 12.0.0. Trang mediation AppLovin của Google: "AppLovin Android adapter version 12.0.0.0 and later supports passing GDPR consent signals from UMP SDK".
- Đọc bytecode SDK `13.6.4` (2026-10-29, trong bộ nhớ đệm Gradle, không lưu vào repo): các lớp nội bộ `com.applovin.impl.e5`, `com.applovin.impl.sdk.m`, `com.applovin.impl.y5` có các chuỗi khóa `IABTCF_TCString`, `IABTCF_AddtlConsent`, `IABTCF_gdprApplies`, `IABTCF_PurposeConsents`, `IABTCF_VendorConsents`, `IABTCF_CmpSdkID`: SDK đọc các khóa TCF chuẩn mà UMP ghi vào SharedPreferences mặc định. Adapter Google của MAX đọc `MaxAdapterParameters.hasUserConsent()` và `isDoNotSell()` rồi đặt `npa` khi cần (bytecode của `GoogleMediationAdapter`).
- **Kết luận của plan:** không gọi `AppLovinPrivacySettings.setHasUserConsent` hay `setDoNotSell`, không bật luồng đồng ý của MAX (Terms and Privacy Policy Flow, mặc định tắt). MAX đọc chuỗi TCF và AC của UMP; vì MAX chỉ khởi tạo sau khi cổng mở, UMP đã có kết quả (hay đồng ý cũ của `CACHED_EARLY`). **Điểm chưa chắc:** tài liệu chính thức không viết thẳng câu "SDK đọc TCF khi app tự dùng UMP"; bằng chứng là chuỗi khóa trong bytecode và trả lời trên diễn đàn. Kiểm trên máy bằng Mediation Debugger của MAX (hiện trạng thái TC và AC theo từng mạng) ở kiểm thủ công 2; nếu Mediation Debugger báo không có đồng ý trong khi UMP đã ghi, dùng phương án dự phòng ở mục 3.4 (câu hỏi 2). AppLovin chỉ có trong chuỗi AC khi "AppLovin Corp." là đối tác trong thông điệp GDPR trên AdMob (đã là việc của người từ T-015, `ads-guide.md` mục 7).

### Tra cứu: adapter Google của MAX gọi gì khi khởi tạo

Đọc `GoogleMediationAdapter.initialize` của `google-adapter:25.5.0.0` bằng `javap -c` (2026-10-29): lần đầu (cờ tĩnh `initialized`), adapter gọi `MobileAds.disableMediationAdapterInitialization(context)` rồi `MobileAds.initialize(context, listener)`. Theo tài liệu Google, `disableMediationAdapterInitialization` phải gọi **trước** `initialize` mới có tác dụng và tắt việc SDK tự khởi tạo các adapter mediation. Với dự án mẫu, AdMob có chín adapter (T-015, T-034): nếu MAX khởi tạo adapter Google trước `MobileAds.initialize` của `AdsController`, chín adapter có thể không được khởi tạo và mediation AdMob mất đấu giá. JBase không gặp vì AdMob của JBase không có adapter mediation nào (câu hỏi 45). Plan: MAX chỉ khởi tạo sau khi lời gọi `MobileAds.initialize(context, listener)` của `AdsController` đã trả về (mục 3.1 bước 5), có test thứ tự (`AdsSdkStartTest`), và kiểm trên máy rằng log `Adapter ...` vẫn liệt kê đủ chín adapter khi có khóa MAX (kiểm thủ công 1). Chưa chắc lời gọi `disableMediationAdapterInitialization` sau `initialize` hoàn toàn không ảnh hưởng (Google không viết); câu hỏi 3.

### Manifest gộp: SDK MAX và adapter Google kéo vào gì

Đo ngày 2026-10-29, `./gradlew :app:processStagingDebugMainManifest` trên `main` `c29ce2b` trước và sau khi thêm hai thư viện và code thử (mục 4.1), so bằng một script tạm theo thẻ:

| Thành phần | Trước | Sau | Thêm | Bỏ | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `uses-permission` | 11 | 11 | Không | Không | Đúng 11 quyền của `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet` |
| `activity` | 69 | 69 | Không | Không | Activity của SDK AppLovin có sẵn từ T-015 |
| `service` | 15 | 15 | Không | Không | |
| `receiver` | 11 | 11 | Không | Không | |
| `provider` | 7 | 7 | Không | Không | `AppLovinInitProvider` vẫn bị bỏ bằng `tools:node="remove"` (T-015) |
| `meta-data` | 23 | 23 | Không | Không | Không có `applovin.sdk.key` |
| `<queries>` (gói, action) | 16, 25 | 16, 25 | Không | Không | |

File gộp trước và sau giống hệt từng dòng (889 dòng, `diff` rỗng). Lý do: manifest trong AAR của `google-adapter:25.5.0.0` chỉ có `uses-sdk minSdkVersion="23"`; SDK `13.6.4` đã nằm trong bản build qua adapter AppLovin của AdMob, nên các quyền nó khai báo (`INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE`, `AD_ID`, `CHECK_LICENSE`, `ACCESS_WIFI_STATE`) và provider đã được T-015 xử lý (giữ `VIBRATE`, bỏ `CHECK_LICENSE`, `ACCESS_WIFI_STATE` và `AppLovinInitProvider`). Không có quyền mới, không thêm dòng nào vào `core/ads/src/main/AndroidManifest.xml`. Đề xuất ghi vào spec mục 6: câu hỏi 1.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo các mẫu đã có: thư viện trong catalog và chỉ `:core:ads` phụ thuộc (T-015, T-034); tham số build và kiểm `preProduction*Build` theo classpath (T-033, `appsFlyerDevKey`); cấu hình của build cấp qua Hilt từ `:app` như `ConsentShortcutConfig`, `AppsFlyerConfig`; khóa Remote Config khai báo trong `AdsSettings`, đọc qua `AdsFlags` (T-019); luật thuần tách thành hàm, test bằng bản giả (ADR-0026).

1. `gradle/libs.versions.toml` (file được bảo vệ, `CLAUDE.md` mục Không được làm; plan nêu rõ để phiên được sửa): nhóm `# Ads`, ngay dưới `unityAds = "4.21.0"`, thêm `applovinSdk = "13.6.4"` và `maxMediationGoogle = "25.5.0.0"`. Ngay dưới thư viện `unity-ads`: `applovin-sdk = { group = "com.applovin", name = "applovin-sdk", version.ref = "applovinSdk" }` và `max-mediation-google = { group = "com.applovin.mediation", name = "google-adapter", version.ref = "maxMediationGoogle" }`. Phiên bản cụ thể, không `+` (ADR-0037). Không đổi phiên bản nào khác. Lý do sửa file được bảo vệ: thư viện mới phải vào catalog (ADR-0037, M-02, `plan-check.py` quy tắc 8).
2. `core/ads/build.gradle.kts`: hai dòng `implementation(libs.applovin.sdk)` và `implementation(libs.max.mediation.google)` ngay dưới `implementation(libs.unity.ads)`. Chỉ `:core:ads` thấy hai thư viện (ADR-0037). Không cần `consumer-rules.pro`: R8 qua không cần luật giữ lớp (mục 4.1; AAR của SDK có `proguard.txt` riêng).
3. `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MediationOrder.kt` (tạo):
   - `enum class MediationOrder(val code: String, val usesAdmob: Boolean, val usesMax: Boolean)`: `ADMOB("admob", true, false)`, `MAX("max", false, true)`, `ADMOB_MAX("admob_max", true, true)`, `MAX_ADMOB("max_admob", true, true)`; thuộc tính `platforms: List<AdPlatform>` theo đúng thứ tự (`enum class AdPlatform { ADMOB, MAX }`, cùng file), để T-069 hỏi nền tảng thứ nhất rồi thứ hai.
   - `companion object { val DEFAULT = MAX_ADMOB; fun of(value: String): MediationOrder; fun effective(value: MediationOrder, maxAvailable: Boolean): MediationOrder }`. `of`: so **đúng** chuỗi (phân biệt hoa thường, không bỏ khoảng trắng, như `OpenAdType.of` của A-12); giá trị khác, kể cả rỗng, là `DEFAULT` (ADR-0037, ticket: "invalid => default"). `effective`: không có MAX (`maxAvailable = false`) thì luôn `ADMOB`, kể cả khi giá trị là `max` (câu hỏi 43: staging không khóa thì thứ tự coi như `admob`; câu hỏi 4 về `max` khi không có khóa).
4. `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxSdk.kt` (tạo):
   - `data class MaxConfig(val sdkKey: String)` (public, `:app` cấp), `val hasKey get() = sdkKey.isNotBlank()`, `companion object { val NONE = MaxConfig("") }`, `toString()` che khóa (`MaxConfig(sdkKey=<4 ký tự đầu>…)` hay `MaxConfig(no key)`), để khóa không lọt vào log hay thông báo lỗi.
   - `internal interface MaxPlatform { fun initialize(sdkKey: String, onInitialized: () -> Unit) }`.
   - `internal class AppLovinMaxPlatform(context: Context) : MaxPlatform`: dựng `AppLovinSdkInitializationConfiguration.builder(sdkKey).setMediationProvider(AppLovinMediationProvider.MAX).build()` rồi `AppLovinSdk.getInstance(context).initialize(configuration) { onInitialized() }`. Không gọi `AppLovinPrivacySettings` (mục 2), không gọi `getSettings().getTermsAndPrivacyPolicyFlowSettings()`, không đặt máy thử (việc của T-058 nếu cần). Đã biên dịch được ở chạy thử (đúng chữ ký của bản 13.6.4, đọc bằng `javap`: `builder(String)`, `initialize(AppLovinSdkInitializationConfiguration, SdkInitializationListener)`).
   - `internal class MaxSdkStarter(config: MaxConfig, platform: MaxPlatform, logger: AppLogger)`: giữ trạng thái `gateOpened`, `started`, và `ready: StateFlow<Boolean>`. Hai hàm, cả hai chạy trên luồng chính:
     - `onAdmobStarted(order: MediationOrder)`: gọi đúng một lần từ `startSdk` sau `MobileAds.initialize` (bước 5); đặt `gateOpened = true` rồi gọi `maybeStart(order)`.
     - `onOrderChanged(order: MediationOrder)`: khi `ads_mediation_order` đổi từ xa; gọi `maybeStart(order)`.
     - `maybeStart(order)` (private, hàm duy nhất gọi `platform.initialize`, L-072): không làm gì khi `!gateOpened`, `started`, `!config.hasKey` hay `!order.usesMax`; ngược lại `started = true`, log `"MAX started"`, gọi `platform.initialize`, callback đặt `ready = true` và log `"MAX ready"`. Không có khóa: log một lần `"MAX skipped: no SDK key"` lúc `onAdmobStarted` (staging). Không bao giờ log khóa.
     - Không có đường dừng: rút đồng ý (S-14, S-15) không "tắt" SDK; cổng đóng chặn mọi yêu cầu MAX sau này qua `mayRequestNow()` (T-069). Đồng ý lại khi SDK đã khởi tạo thì không khởi tạo lại (S-14 vế "đã khởi tạo thì nạp sẵn").
5. `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSdkStart.kt` (tạo): `internal suspend fun startAdsSdks(initAdmob: suspend () -> Unit, startMax: () -> Unit)`: gọi `initAdmob()` (chờ lời gọi `MobileAds.initialize` trả về, không chờ callback của nó), rồi mới `startMax()`. Chú thích nêu lý do: adapter Google của MAX gọi `MobileAds.disableMediationAdapterInitialization` (mục 2). Hàm nhỏ nhưng là chỗ test được thứ tự mà không cần SDK thật.
6. `AdsController.kt` (sửa):
   - Constructor thêm `private val maxConfig: MaxConfig` và `maxPlatform: MaxPlatform` (sau `funnel`). Hilt: `MaxConfig` do `:app` cấp; `MaxPlatform` do module mới `di/MaxPlatformModule.kt` của `:core:ads` cấp (`@Provides fun maxPlatform(@ApplicationContext context: Context): MaxPlatform = AppLovinMaxPlatform(context)`, `internal`).
   - Trường `private val maxStarter = MaxSdkStarter(maxConfig, maxPlatform, logger)`.
   - `startSdk()`: thay khối `withContext(Dispatchers.IO) { MobileAds.initialize(context) { ... } }` bằng `startAdsSdks(initAdmob = { withContext(Dispatchers.IO) { MobileAds.initialize(context) { ... } } }, startMax = { maxStarter.onAdmobStarted(mediationOrder.value) })`; phần còn lại (`sdkStarted.value = true`, chờ `_sdkReady`, nạp sẵn) giữ nguyên thứ tự. `startMax` chạy trên luồng chính (sau `withContext` quay về `Dispatchers.Main.immediate` của `scope`); SDK MAX tự làm việc nặng ở luồng nền của nó.
   - `internal val mediationOrder: StateFlow<MediationOrder>`: `globalFlags.map { MediationOrder.effective(it.mediationOrder, maxConfig.hasKey) }.stateIn(scope, SharingStarted.Eagerly, MediationOrder.effective(MediationOrder.DEFAULT, maxConfig.hasKey))`. T-069 tới T-071 đọc nó. Trong `start()`, thêm một `scope.launch { mediationOrder.collect { maxStarter.onOrderChanged(it) } }` cạnh các collector đã có.
   - `internal val maxReady: StateFlow<Boolean> = maxStarter.ready` (T-069 đọc).
   - Sửa KDoc của lớp: một đoạn về MAX (khởi tạo sau cổng và sau `MobileAds.initialize`, không cờ đồng ý, không khóa thì không khởi tạo, thứ tự `ads_mediation_order`).
   - Không đổi `onGateOpened`, `ConsentGate.kt`, `ConsentCoordinator.kt`: MAX đi theo đúng đường của AdMob (`onGateOpened(firstTime = true)` gọi `startSdk`).
7. `AdsSettings.kt` (sửa): `val ADS_MEDIATION_ORDER = stringKey(name = "ads_mediation_order", default = "max_admob", description = "Order of the two ad platforms for banner, MREC, interstitial, rewarded and resume app open ads: admob, max, admob_max or max_admob; any other value uses max_admob. Without a MAX SDK key in the build, admob.")` (hậu tố "Name set by the template, not an AdsKit key." tự thêm vì tên không có trong `ADSKIT_KEY_NAMES`). Thêm vào `GLOBAL_KEYS` sau `IS_ENABLE_CONSENT_SHORTCUT`. Mặc định là chuỗi `MediationOrder.DEFAULT.code`, viết bằng hằng số đó để một nơi giữ giá trị.
8. `AdsFlags.kt` (sửa): `AdsGlobalFlags` thêm `val mediationOrder: MediationOrder = MediationOrder.DEFAULT` (ở cuối, có giá trị mặc định để các chỗ dựng `AdsGlobalFlags` có sẵn vẫn biên dịch: `DEFAULT_GLOBAL_FLAGS` của `AdsController.kt`, `FakeAdsFlags` của `AdGalleryViewModelTest.kt`, hai test của `AdsFlagsTest.kt`). KDoc: "ads_mediation_order, read as written; [AdsController.mediationOrder] applies the SDK key". `DefaultAdsFlags.global` thêm `settings.value(AdsSettings.ADS_MEDIATION_ORDER)` vào cuối danh sách `combine` và `mediationOrder = MediationOrder.of(values[14] as String)`. Không thêm hàm vào giao diện `AdsFlags`.
9. `:app`:
   - `app/build.gradle.kts`: dòng đầu `import java.util.Properties` (chạy thử: viết `java.util.Properties()` trong khối `android { }` không biên dịch được vì `java` là extension của Gradle, mục 4.1). Flavor `staging`: đọc `ads.maxSdkKey` từ `local.properties` ở gốc repo nếu file có (`rootProject.file("local.properties")`, `Properties().apply { file.inputStream().use { load(it) } }`), không có thì rỗng; `buildConfigField("String", "MAX_SDK_KEY", "\"<giá trị>\"")`. Không đọc `-PmaxSdkKey` ở staging, để CI (không có `local.properties` có khóa) luôn ra bản staging không khóa. Flavor `production`: `buildConfigField("String", "MAX_SDK_KEY", "\"${providers.gradleProperty("maxSdkKey").getOrElse("")}\"")`, như `APPSFLYER_DEV_KEY`. Chú thích đầu khối `productFlavors` thêm dòng `-PmaxSdkKey=<AppLovin MAX SDK key>`. Kiểm `preProduction*Build`: thêm `hasMax` (một thành phần trên runtime classpath có nhóm `com.applovin.mediation`, tức adapter Google của MAX; adapter AppLovin của AdMob có nhóm `com.google.ads.mediation` nên không tính) và `mapOf("maxSdkKey" to "ADR-0037")` khi `hasMax` và tham số rỗng, cùng cách với `hasAppsFlyer`. Cách hiểu "khi thứ tự mặc định có MAX": mặc định `max_admob` nằm trong code của `:core:ads` và có MAX; app đổi mặc định sang `admob` mà vẫn giữ MAX trong bản build thì vẫn cần khóa để bật MAX từ xa; app bỏ hẳn MAX thì bỏ thư viện và không cần khóa (câu hỏi 6).
   - `app/src/main/kotlin/com/vglstudio/template/di/MaxConfigModule.kt` (tạo): `@Provides fun maxConfig(): MaxConfig = MaxConfig(BuildConfig.MAX_SDK_KEY)`, như `AnalyticsConfigModule`.
10. `scripts/secret-scan.py`: `LINE_FILTERS["khóa SDK AppLovin"]` đổi thành `re.compile(r"applovin|max[._-]?sdk[._-]?key", re.IGNORECASE)`, để mẫu 86 ký tự cũng áp cho dòng `ads.maxSdkKey=...`, `-PmaxSdkKey=...`, `ORG_GRADLE_PROJECT_maxSdkKey=...`, `max_sdk_key: ...` (khóa SDK MAX chính là khóa SDK AppLovin, 86 ký tự). Docstring sửa câu "on a line that mentions applovin" thành "on a line that mentions applovin or a MAX SDK key name (plan T-068)". Tên loại giữ "khóa SDK AppLovin". Không thêm mẫu mã đơn vị MAX (chưa có mã nào; T-073 quyết khi có bảng mã, câu hỏi 7).
11. `scripts/tests/run.sh`, cạnh các test `secret-scan` của AppLovin: hai test mới (mục 4), khóa dựng từ mảnh bằng `printf` như test của T-034.
12. Tài liệu:
    - `docs/remote-config-keys.md`, `docs/remote-config-template.json`: sinh lại bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` (dòng `ads_mediation_order`, string, `max_admob`, `:core:ads`), không sửa tay.
    - `docs/ops/ads-business-rules.md`: mục 10.1 thêm dòng `ads_mediation_order` (string, `max_admob`, bốn giá trị, giá trị lạ dùng mặc định, build không khóa MAX thì `admob`; Ai đọc: ads; Nguồn tên: tên do ta đặt; AdsKit: không có, ADR-0037, JBase `AdsConfig.Network`, `MaxPrimary`). Mục 9 thêm hai dòng: M-08 "MAX khởi tạo đúng một lần sau cổng đồng ý và sau lời gọi `MobileAds.initialize`, không cờ đồng ý viết cứng, MAX đọc chuỗi TCF và AC của UMP (ADR-0037; JBase khởi tạo trước đồng ý và viết cứng đồng ý, không theo)"; M-09 "Khóa SDK MAX là cấu hình của app, vào lúc build bằng `-PmaxSdkKey`; staging đọc `ads.maxSdkKey` của `local.properties`; không khóa thì MAX không khởi tạo (ADR-0037, thay M-03 cho MAX)". Dẫn chứng JBase ghi theo ADR-0038 (đường dẫn và dòng lấy từ ADR-0037 và yêu cầu `requests/2026-10-29-hoc-tu-jbase.md`; không có thì ghi "theo ADR-0037", không theo trí nhớ).
    - `docs/ops/ads-guide.md` mục 7: tiểu mục mới "AppLovin MAX (T-068)": thư viện và phiên bản, cùng SDK với adapter AppLovin của AdMob (nâng cùng nhau); không kho mới; manifest không đổi; khởi tạo sau cổng và sau `MobileAds.initialize`, lý do `disableMediationAdapterInitialization`; đồng ý qua TCF và AC (thêm "AppLovin Corp." vào đối tác của thông điệp GDPR, đã có); khóa `-PmaxSdkKey`, `ads.maxSdkKey` trong `local.properties` để thử ở staging, không có khóa thì log `MAX skipped: no SDK key`; nhóm mediation AdMob không có AppLovin khi app dùng MAX (câu hỏi 45, chi tiết ở T-073); cách kiểm trên máy (Mediation Debugger của MAX: `AppLovinSdk.getInstance(context).showMediationDebugger()` chỉ gọi tay khi thử, không có trong code).
    - Không sửa `docs/ops/release.md` và `scripts/release.sh` (T-073, `auto_run: false`); xem câu hỏi 8 về khoảng giữa hai ticket.
    - Không sửa spec (spec `confirmed`; đề xuất sửa mục 6 và mục 8 ở câu hỏi 1) và không sửa ADR.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. Hai phiên bản, hai thư viện (mục 3.1, bước 1) |
| Sửa | `core/ads/build.gradle.kts` | Hai dòng `implementation` (bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MediationOrder.kt` | `MediationOrder`, `AdPlatform`, `of`, `effective` (bước 3) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxSdk.kt` | `MaxConfig`, `MaxPlatform`, `AppLovinMaxPlatform`, `MaxSdkStarter` (bước 4) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSdkStart.kt` | `startAdsSdks` (bước 5) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/MaxPlatformModule.kt` | Cấp `MaxPlatform` (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Constructor, `startSdk`, `mediationOrder`, `maxReady`, collector trong `start`, KDoc (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Khóa `ads_mediation_order` (bước 7) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsFlags.kt` | `AdsGlobalFlags.mediationOrder`, đọc khóa (bước 8) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MediationOrderTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxSdkStarterTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSdkStartTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerMaxTest.kt` | Mục 4 (nối vào `AdsController`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeMaxPlatform.kt` | Bản giả đếm lời gọi, gọi callback khi test muốn |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerFixtures.kt` | `testAdsController` thêm `maxConfig = MaxConfig.NONE`, `maxPlatform = FakeMaxPlatform()` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MediationAdapterClasspathTest.kt` | Test SDK MAX và adapter Google của MAX (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | `globalKeys_matchAdsKitTable` thêm khóa (14 thành 15), test mới (mục 3.5, mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | `global_followsEveryRemoteValue` thêm khóa, test mới (mục 3.5, mục 4) |
| Sửa | `app/build.gradle.kts` | `import`, `MAX_SDK_KEY` hai flavor, kiểm `preProduction*Build` (bước 9) |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/di/MaxConfigModule.kt` | Cấp `MaxConfig` (bước 9) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` | Hai test mới về `maxSdkKey` (mục 4) |
| Ghi lại | `docs/remote-config-keys.md` | Sinh lại bằng `RemoteConfigKeysTest` (bước 12) |
| Ghi lại | `docs/remote-config-template.json` | Sinh lại bằng `RemoteConfigKeysTest` (bước 12) |
| Sửa | `scripts/secret-scan.py` | Bộ lọc dòng của khóa AppLovin, docstring (bước 10) |
| Sửa | `scripts/tests/run.sh` | Hai test `secret-scan` cho khóa MAX (bước 11) |
| Sửa | `docs/ops/ads-business-rules.md` | Mục 9 (M-08, M-09), mục 10.1 (bước 12) |
| Sửa | `docs/ops/ads-guide.md` | Mục 7, tiểu mục MAX (bước 12) |
| Sửa | `docs/features/ads/plans/T-068-sdk-max-va-khoi-tao.md` | Nhật ký thực hiện (mục 10) |
| Sửa | `docs/features/ads/tickets/T-068-sdk-max-va-khoi-tao.md` | Trạng thái ticket |

Nơi đọc các file bị đổi (L-020), tìm bằng `grep -rn <tên file> scripts/ .github/ */src/test */*/src/test`:

- `gradle/libs.versions.toml`: `build-logic/settings.gradle.kts` nạp làm catalog, chỉ đọc khóa đã có; `scripts/cloud-env-check.sh` chỉ đọc dòng `compileSdk`; `scripts/plan-check.py` chỉ có tên file trong danh sách đường dẫn rủi ro cao và để dò thư viện mới; `scripts/tests/run.sh` chỉ dùng bản giả trong repo tạm; `.github/workflows/pr-check.yml` dòng 68 dùng tên file làm bộ lọc đường dẫn để build bản release khi catalog đổi (đúng ý: PR của task sẽ chạy build release). Không bị ảnh hưởng.
- `app/build.gradle.kts`: `ProductionPropertiesTest` đọc file (đã trong danh sách; hai test cũ giữ nguyên câu khẳng định, mục 3.5); `scripts/new-app.sh` dòng 97 thay địa chỉ R2 staging, `scripts/setup-check.sh` dòng 37, 39 đọc `ASSETS_BASE_URL` và `applicationId`, `scripts/release.sh` dòng 126, 194, 195 đọc `applicationId`, `versionCode`, `versionName`: plan không đổi các dòng đó. `scripts/release.sh` dòng 163 chỉ là chú thích về kiểm classpath. `scripts/check.sh` dòng 48 chỉ kiểm file có tồn tại. Không bị ảnh hưởng. `scripts/release.sh` không truyền `maxSdkKey`: build phát hành sẽ hỏng ở `preProductionReleaseBuild` sau khi task này merge cho tới T-073 (câu hỏi 8); đúng ý của tham số bắt buộc, không phải lỗi của task.
- `core/ads/build.gradle.kts`: không script hay workflow nào đọc nội dung.
- `docs/remote-config-keys.md`: `scripts/ads-plan.py` dòng 34 đọc bảng để dựng bảng tính phát hành, đọc mọi dòng theo cột, dòng mới không phá cách đọc; test của nó trong `scripts/tests/run.sh` dòng 4487 dùng bảng giả riêng. `scripts/template-sync.sh` dòng 32, 33 chỉ chép hai file khi đồng bộ dự án mẫu. `RemoteConfigKeysTest` của `:app` so file với code (sinh lại ở bước 12).
- `scripts/secret-scan.py`: chỉ `scripts/tests/run.sh` gọi; `scripts/check.sh` chạy `scripts/tests/run.sh`, nên quét cả repo, kể cả plan này. Plan không chứa chuỗi 86 ký tự nào. Bộ lọc dòng rộng hơn có thể bắt dòng có chữ `maxSdkKey` đi cùng chuỗi 86 ký tự khác không phải khóa; repo hiện không có dòng như vậy (test "this repository has no real ad IDs or keys" qua ở chạy thử là trước khi đổi bộ lọc; phiên chạy lại).
- `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`: không script hay workflow nào đọc. Các test nhắc `ads-business-rules.md` chỉ trong chú thích (T-034 đã liệt kê; `AdsSettingsTest.kt` dòng 38 chú thích "Copied by hand from ... section 10.1", nên plan sửa test đó cùng bảng 10.1).
- `AdsFlags.kt`, `AdsGlobalFlags`: `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModelTest.kt` dòng 217, 219 (`FakeAdsFlags` dựng `AdsGlobalFlags` bằng tên tham số) và `RewardedActionTest.kt` dòng 802 (`HoldingFlags` ủy quyền `AdsFlags`): trường mới có mặc định, giao diện `AdsFlags` không đổi, nên không sửa. `AdsController.kt` dòng 1990 `DEFAULT_GLOBAL_FLAGS` cũng vậy.
- Constructor `AdsController`: mọi test dựng qua `testAdsController` (`AdsControllerFixtures.kt`, đã trong danh sách); Hilt dựng ở app.
- `scripts/plan-check.py` (quy tắc 2) báo thêm các file sau; đã đọc từng dòng nhắc tới, không file nào bị ảnh hưởng:
  - `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt`: so `docs/remote-config-keys.md`, `docs/remote-config-template.json` với code, và ghi lại hai file khi chạy với `-PremoteConfigKeys.update=true`. Không sửa test; plan dùng chính test này để sinh lại hai file (bước 12).
  - `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` dòng 57: chỉ là chú thích "Library versions are managed in gradle/libs.versions.toml" (về lint), không đọc nội dung.
  - `scripts/save-branch.sh` dòng 20, 190 tới 192: gọi `secret-scan.py` với `SECRET_SCAN_ROOT` để quét patch trước khi ghi lên nhánh báo cáo. Bộ lọc dòng rộng hơn chỉ làm script chặn thêm dòng có `maxSdkKey` cùng chuỗi 86 ký tự, đúng ý; cách gọi và mã trả về không đổi.
  - Các test chỉ nhắc `docs/ops/ads-business-rules.md` trong chú thích để dẫn nguồn, không mở file: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt` (mục 8), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt` (mục S), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt` (mục 4), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt` (mục 8.2), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` và `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` (mục 5.7), `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` (mục 1, 8, 10.1). Plan chỉ thêm dòng ở mục 9 và 10.1, không đổi luật mà các chú thích dẫn; `AppsFlyerSettingsTest` chép tay khóa `is_log_af_*` của 10.1, plan không đổi các dòng đó.
- `docs/STATUS.md`: không đưa vào PR (L-059). Ảnh chuẩn screenshot không đổi: test và preview không có `AdsController`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có cờ tính năng. Khóa Remote Config mới `ads_mediation_order` (string, mặc định `max_admob`), đã chốt ở ADR-0037 và spec câu hỏi 42; chưa có trong bảng khóa ở spec mục 8 (câu hỏi 1).
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng, tần suất. Thêm nền tảng thứ hai được khởi tạo (ADR-0037); chưa có lượt nạp hay hiện nào của MAX (T-069 tới T-071). Staging không có khóa nên không đổi gì trên bản staging của CI.
- Sự kiện phân tích: Không chạm. Khi có khóa, SDK MAX tự gửi dữ liệu về AppLovin lúc khởi tạo (cấu hình, thông tin máy, mã quảng cáo của máy theo đồng ý), sau cổng đồng ý. Data safety của app có MAX khai AppLovin là bên nhận dữ liệu (đã có từ T-015 vì cùng SDK); người khai cần biết AppLovin giờ nhận cả vai nền tảng điều phối.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Đã đo manifest gộp `stagingDebug` trước và sau (mục 2, mục 4.1): `uses-permission` không đổi, 11 quyền. Không thêm: `com.android.vending.CHECK_LICENSE` và `android.permission.ACCESS_WIFI_STATE` mà SDK AppLovin khai báo, vẫn bỏ như T-015; `VIBRATE`, `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có.
- **Thư viện và SDK:** thêm, chỉ ở `:core:ads` (khóa catalog `applovin-sdk`, `max-mediation-google` trong `gradle/libs.versions.toml`, bước 1): `com.applovin:applovin-sdk:13.6.4` (giấy phép AppLovin Corporation SDK EULA, https://www.applovin.com/eula; đã có trên classpath chạy qua `com.google.ads.mediation:applovin:13.6.4.2`, nay khai báo trực tiếp), `com.applovin.mediation:google-adapter:25.5.0.0` (AppLovin Corporation Mediation Adapter EULA; kéo `com.google.android.gms:play-services-ads:25.5.0`, đã có). Quyền mà hai thư viện kéo vào manifest gộp: không có quyền mới; không thêm thành phần nào (mục 2). Thư viện đã có giữ nguyên phiên bản (đo ở mục 4.1): `play-services-ads` 25.5.0, `user-messaging-platform` 4.0.0, `com.google.ads.mediation:applovin` 13.6.4.2.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** SDK MAX không lấy được đồng ý từ chuỗi TCF và AC mà UMP ghi (tài liệu chính thức không viết thẳng điều này, mục 2), nên MAX coi người dùng ở EEA là chưa đồng ý dù UMP đã ghi đồng ý.
- **Điều kiện chuyển:** chỉ phát hiện được trên máy có khóa thật (kiểm thủ công 2), không trong phiên tự chạy. Khi người kiểm thấy Mediation Debugger báo GDPR không có đồng ý cho AppLovin trong khi UMP đã ghi đồng ý, người tạo phản hồi; plan phiên bản sau chọn phương án dự phòng, cần duyệt lại vì đổi cách truyền đồng ý (điểm chặn 5). Phiên tự chạy không tự chuyển.
- **Cách làm thay (đề xuất cho phiên bản sau, câu hỏi 2):** trước `initialize`, `AppLovinMaxPlatform` gọi `AppLovinPrivacySettings.setHasUserConsent(consent, context)` với `consent` đọc từ chuỗi TCF của UMP (mục đích 1 "lưu và truy cập thông tin" có đồng ý và AppLovin có đồng ý trong chuỗi AC), không bao giờ là hằng `true`; đọc lại và đặt lại mỗi lần cổng đóng hay mở. File: `MaxSdk.kt`, `MaxSdkStarterTest.kt` (đã có trong mục 3.2). Tiêu chí và test ở mục 4 giữ nguyên.
- Điểm chưa chắc thứ hai (thứ tự với `disableMediationAdapterInitialization`, câu hỏi 3) không có phương án dự phòng trong phiên: test thứ tự (`AdsSdkStartTest`) bảo đảm điều plan kiểm soát được; phần còn lại kiểm trên máy (kiểm thủ công 1).

### 3.5. Hành vi đã có bị đổi

`python3 -I scripts/existing-tests.py docs/features/ads/plans/T-068-sdk-max-va-khoi-tao.md` (2026-10-29) liệt kê test của các file Kotlin plan sửa. Đã đọc các test ở nhóm "Cần xem trước" và "Nên xem" liên quan tới phần plan đổi (kết quả đầy đủ của script ở mô tả PR):

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Có đúng 14 khóa chung của ads | Thêm `ads_mediation_order` (string, `max_admob`), 15 khóa (bước 7) | `AdsSettingsTest.globalKeys_matchAdsKitTable` (`assertEquals(14, expected.size)`, tập `expected`) | Câu mới (ADR-0031, plan nói rõ): `expected` thêm `Triple("ads_mediation_order", RemoteKey.Type.STRING, "max_admob")`, `assertEquals(15, expected.size)`; ý định giữ: tập khóa chung trong code bằng đúng bảng 10.1 chép tay. Đối chứng: bỏ dòng khỏi `GLOBAL_KEYS` thì test đỏ |
| `AdsGlobalFlags` theo mọi khóa chung | Thêm trường `mediationOrder` (bước 8) | `AdsFlagsTest.global_followsEveryRemoteValue` ("Every key but the master switch set away from its default") | Câu mới: thêm `settings.set("ads_mediation_order", "admob_max")` và `mediationOrder = MediationOrder.ADMOB_MAX` trong giá trị mong đợi, để ý định "mọi khóa" còn đúng |
| Giá trị mặc định khi chưa đặt gì | Trường mới mặc định `MAX_ADMOB` | `AdsFlagsTest.global_usesTableDefaultsWhenNothingSet` | Giữ: dựng `AdsGlobalFlags` không có tham số mới, mặc định của trường bằng mặc định của khóa, nên vẫn bằng nhau. Test mới `globalMediationOrder_defaultIsMaxAdmob` khẳng định thẳng |
| Chỉ đọc khóa đã khai báo | Đọc thêm `ads_mediation_order`, đã khai báo | `AdsFlagsTest.readsOnlyDeclaredKeys` | Giữ, qua không sửa |
| `startSdk` gọi `MobileAds.initialize` rồi đặt `sdkStarted`, chờ `sdkReady`, nạp sẵn | Cùng thứ tự; thêm bước khởi tạo MAX giữa `MobileAds.initialize` và `sdkStarted` (bước 6) | Không có test gọi `startSdk` thật (test dùng `openConsentGateForTest`, không chạy SDK); các test của `AdsControllerFunnelTest`, `AdsControllerResumeTest`, `AdsControllerNativeComboTest` dựng qua `testAdsController` | Giữ: không câu khẳng định nào đổi; `testAdsController` cấp `MaxConfig.NONE` nên MAX không bao giờ chạy trong các test đó |
| Production cần `admobAppId`, `adUnits`, `assetsBaseUrl`, `appsFlyerDevKey` (khi có SDK) | Thêm `maxSdkKey` khi có adapter Google của MAX (bước 9) | `ProductionPropertiesTest.appsFlyerDevKey_isRequiredForProductionWithTheSdk`, `stagingKey_isFixedEmpty` | Giữ: các chuỗi chúng tìm vẫn có nguyên văn (khối kiểm AppsFlyer không đổi, `throw GradleException(` vẫn sau `if (missing.isNotEmpty())`). Hàm `flavor()` cắt khối flavor tới `named(` kế tiếp; phần đọc `local.properties` thêm vào khối staging không chứa `named(`, nên khối production vẫn đọc đúng |
| Manifest gộp, classpath của chín adapter | Không đổi (đo, mục 2) | `MergedManifestTest` (mọi test), `MediationAdapterClasspathTest` (ba test cũ) | Giữ, qua không sửa |
| `AdsController` chỉ đọc Remote Config qua `AdsFlags` | Constructor thêm `MaxConfig`, `MaxPlatform`, vẫn nhận `AdsFlags`, không nhận `RemoteSettings` | `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags` | Giữ, qua không sửa |
| Khóa tên do ta đặt có ghi chú trong mô tả | Khóa mới dùng `stringKey`, tự thêm "Name set by the template, not an AdsKit key." | `AdsSettingsTest.keysNamedByTemplate_areMarked`, `allKeys_haveUniqueNames`, `allKeys_containsEveryDeclaredKey` | Giữ, qua không sửa |
| Bảng khóa liệt kê mọi khóa | Thêm một dòng, sinh lại (bước 12) | `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed` và các test so file của lớp này | Giữ; đỏ tới khi sinh lại file, đúng ý |

Bảy test của nhóm "Cần xem trước" (`AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`, `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`, bốn test bố cục của `RewardedActionTest`) chỉ trùng tên hàm `start` (của `ActivityScenario` hay `AdsController.start`) và đo bố cục mà plan không đổi; `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` dựng controller qua `testAdsController`, nhận `MaxConfig.NONE`. Giữ, không câu khẳng định nào đổi. Các test "Nên xem" khác cùng lý do (trùng `start`, `AdsSettings.all`, `AdsGlobalFlags` dựng bằng tên tham số).

## 4. Kiểm thử

Test JVM, chạy trong `scripts/check.sh`. Tên lớp SDK viết thẳng trong test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| ADR-0037: SDK MAX và adapter Google của MAX có trong bản build | Unit test `:core:ads` | `MediationAdapterClasspathTest.maxSdkAndGoogleAdapter_areOnClasspath`: `Class.forName("com.applovin.sdk.AppLovinSdk", false, ...)` và `Class.forName("com.applovin.mediation.adapters.GoogleMediationAdapter", false, ...)` không ném lỗi; lớp adapter gán được cho `com.applovin.mediation.adapter.MaxAdapter`. Trước khi thêm thư viện: `ClassNotFoundException: com.applovin.mediation.adapters.GoogleMediationAdapter` (SDK đã có nhờ adapter AdMob, nên chỉ vế adapter đỏ; đã thấy ở chạy thử là test qua sau khi thêm) |
| KB-31: bốn giá trị của `ads_mediation_order` | Unit test | `MediationOrderTest.of_readsEachValue`: `admob`, `max`, `admob_max`, `max_admob` ra đúng bốn hằng, `platforms` theo đúng thứ tự (`MAX_ADMOB.platforms == listOf(MAX, ADMOB)`, `ADMOB_MAX.platforms == listOf(ADMOB, MAX)`, `MAX.platforms == listOf(MAX)`, `ADMOB.platforms == listOf(ADMOB)`) |
| Ticket: giá trị lạ dùng mặc định | Unit test | `MediationOrderTest.of_invalidIsDefault`: `""`, `"MAX"`, `" max"`, `"max,admob"`, `"applovin"` ra `MAX_ADMOB`; đối chứng (L-055): cùng test khẳng định `of("admob") == ADMOB`, để test không qua chỉ vì mọi giá trị ra mặc định |
| KB-31, câu hỏi 43: không khóa thì thứ tự coi như `admob` | Unit test | `MediationOrderTest.effective_withoutMaxIsAdmob`: với `maxAvailable = false`, cả bốn hằng ra `ADMOB`; với `true`, mỗi hằng ra chính nó |
| Khóa Remote Config | Unit test | `AdsSettingsTest.mediationOrder_isStringDefaultMaxAdmob`: `ADS_MEDIATION_ORDER` tên `ads_mediation_order`, kiểu `STRING`, mặc định `"max_admob"`, bằng `MediationOrder.DEFAULT.code`, mô tả kết thúc bằng câu "Name set by the template, not an AdsKit key."; cùng các câu sửa ở mục 3.5 |
| Đọc khóa | Unit test | `AdsFlagsTest.globalMediationOrder_defaultIsMaxAdmob` (chưa đặt gì: `MAX_ADMOB`; đối chứng: đặt `"admob"` thì ra `ADMOB`), `AdsFlagsTest.globalMediationOrder_invalidIsDefault` (đặt `"x"`: `MAX_ADMOB`), `AdsFlagsTest.globalMediationOrder_followsChange` (Turbine: `max_admob` rồi đặt `admob`, phát lại với `ADMOB`) |
| KB-31: MAX chỉ khởi động sau khi cổng đồng ý mở | Unit test | `MaxSdkStarterTest.notStarted_beforeAdmobStarted`: khóa giả, `onOrderChanged(MAX_ADMOB)` nhiều lần khi chưa `onAdmobStarted`: `FakeMaxPlatform.calls == 0`. Đây là đường thứ hai tới `initialize` (Remote Config về trước khi có đồng ý, L-072) |
| Như trên, đường thứ nhất | Unit test | `MaxSdkStarterTest.started_onceWhenAdmobStarted`: khóa giả, `onAdmobStarted(MAX_ADMOB)` rồi gọi lại `onAdmobStarted`, `onOrderChanged(MAX)`: `calls == 1`, khóa truyền vào đúng khóa giả; `ready` `false` tới khi bản giả gọi callback, rồi `true` |
| Câu hỏi 43: không khóa thì không khởi tạo | Unit test | `MaxSdkStarterTest.noKey_neverStarts`: `MaxConfig.NONE` và `MaxConfig("   ")`, `onAdmobStarted(MAX_ADMOB)`, `onOrderChanged(MAX)`: `calls == 0`; log có đúng một dòng `MAX skipped: no SDK key`, không dòng nào chứa khóa |
| KB-31: thứ tự `admob` không khởi tạo MAX; đổi sang có MAX thì khởi tạo | Unit test | `MaxSdkStarterTest.admobOrder_waitsForOrderWithMax`: khóa giả, `onAdmobStarted(ADMOB)`: `calls == 0`; `onOrderChanged(ADMOB_MAX)`: `calls == 1`; `onOrderChanged(ADMOB)` rồi `MAX`: vẫn 1 |
| Khóa không lọt vào log | Unit test | `MaxSdkStarterTest.keyNeverLogged`: khóa giả `"FAKE" + "k".repeat(82)`, mọi lần gọi ở trên: không dòng log nào của `RecordingAppLogger` chứa khóa; `MaxConfig(...).toString()` không chứa khóa |
| Thứ tự khởi tạo: MAX sau `MobileAds.initialize` (mục 2) | Unit test | `AdsSdkStartTest.maxStartsAfterAdmobInitReturns`: `initAdmob` là hàm `suspend` giả ghi `"admob start"`, chờ một `CompletableDeferred`, ghi `"admob returned"`; `startMax` ghi `"max"`. Chạy `startAdsSdks` trong `runTest`, kiểm danh sách chỉ có `"admob start"` khi deferred chưa xong, rồi `["admob start", "admob returned", "max"]` sau khi hoàn tất |
| Nối vào `AdsController` | Unit test Robolectric | `AdsControllerMaxTest`, dựng bằng `testAdsController(maxConfig = ..., maxPlatform = fake, flags = DefaultAdsFlags(settings))`: `mediationOrder_isAdmobWithoutKey` (mặc định không khóa: `mediationOrder.value == ADMOB`; đối chứng: khóa giả và cờ mặc định ra `MAX_ADMOB`); `mediationOrder_followsRemoteKey` (khóa giả, đặt `ads_mediation_order = "admob"` trong `FakeRemoteSettings`: `ADMOB`); `max_notStartedWithoutConsentGate` (khóa giả, gọi `start(...)` với `Application` của Robolectric nhưng không mở cổng, đổi `ads_mediation_order` qua lại: `fake.calls == 0` sau khi chạy hết hàng đợi của luồng chính, S-19) |
| Câu hỏi 43: production cần khóa khi có MAX | Unit test `:app` | `ProductionPropertiesTest.maxSdkKey_isRequiredForProductionWithMax`: khối kiểm `preProduction` có `?.group == "com.applovin.mediation"`, `if (hasMax && providers.gradleProperty("maxSdkKey").orNull.isNullOrBlank())`, `mapOf("maxSdkKey" to "ADR-0037")`; khối production có `"MAX_SDK_KEY"` và `gradleProperty("maxSdkKey")` |
| Staging: khóa chỉ từ `local.properties`, không từ tham số | Unit test `:app` | `ProductionPropertiesTest.stagingMaxKey_readsLocalPropertiesOnly`: khối staging có `"MAX_SDK_KEY"`, `local.properties`, `"ads.maxSdkKey"`, không có `gradleProperty("maxSdkKey")`; đối chứng: khối production không có `ads.maxSdkKey` |
| KB-31: khóa MAX không vào repo | Test script `scripts/tests/run.sh` | `secret-scan: a MAX SDK key fails`: thư mục quét tạm có `local.properties` (hay `notes.md`) với dòng `ads.maxSdkKey=` cộng chuỗi 86 ký tự dựng bằng `printf` từ mảnh, và một dòng `-PmaxSdkKey=` cộng cùng chuỗi: script trả 1, báo 2 dòng, không in đủ chuỗi. `secret-scan: a fake MAX SDK key passes`: cùng hai dòng với chuỗi có `FAKE`: trả 0. Trước khi sửa bộ lọc: test đầu thất bại (trả 0, dòng không có chữ `applovin`). Test "this repository has no real ad IDs or keys" có sẵn kiểm cả repo |
| Bảng khóa và file mẫu khớp code | Unit test `:app` có sẵn | `RemoteConfigKeysTest`: đỏ sau bước 7 khi chưa sinh lại; sinh lại ở bước 12 thì qua |
| Manifest và quyền không đổi | Unit test `:app` có sẵn, lệnh | `MergedManifestTest` (mọi test, đặc biệt `permissionsAreExactlyTheAllowedSet`, `hasNoAppLovinInitProvider`, `hasNoAppLovinSdkKey`) qua không sửa. `./gradlew :app:processStagingDebugMainManifest` trước và sau: file gộp giống hệt (mục 2); AI ghi vào nhật ký |
| Phiên bản đúng | Lệnh, AI ghi vào nhật ký | `./gradlew :app:dependencies --configuration stagingReleaseRuntimeClasspath`: có `com.applovin:applovin-sdk:13.6.4` (qua cả hai đường) và `com.applovin.mediation:google-adapter:25.5.0.0`; `play-services-ads` 25.5.0, `user-messaging-platform` 4.0.0; so với trước chỉ thêm đúng `google-adapter` (mục 4.1). Lệch thì dừng (mục 7) |
| R8 của bản release (L-083) | Lệnh, AI ghi vào nhật ký | `./gradlew :app:assembleStagingRelease`: qua; cảnh báo "Missing class" không thêm so với nhánh chính |
| Build, lint, ktlint, test, ảnh chuẩn cũ | `scripts/check.sh` | Qua toàn bộ (đã chạy thử, mục 4.1) |
| Kiểm thủ công 1, mục 2 (thứ tự với `disableMediationAdapterInitialization`) | Kiểm tra thủ công (máy thật: quảng cáo thật, cần khóa SDK MAX của một tài khoản AppLovin trong `local.properties`) | Đặt `ads.maxSdkKey` trong `local.properties`, build `stagingDebug`, xóa dữ liệu, đồng ý: logcat thẻ `Ads` có dòng `MAX started`, `MAX ready` và vẫn có 10 dòng `Adapter ...` (chín adapter cùng `MobileAds`) như trước T-068. Thiếu dòng adapter nào thì tạo phản hồi (câu hỏi 3) |
| Kiểm thủ công 2, KB-31 và đồng ý (mục 2, mục 3.4) | Kiểm tra thủ công (máy thật: quảng cáo thật, cần khóa SDK MAX) | Cùng bản: trước khi trả lời hộp thoại đồng ý, logcat không có dòng của thẻ `AppLovinSdk` hay `MAX started`. Sau khi đồng ý, mở Mediation Debugger của MAX (gọi tạm `showMediationDebugger()` trong bản thử của dev, không commit): mục quyền riêng tư cho thấy CMP là Google, chuỗi TC và AC có, AppLovin có đồng ý. Từ chối trong hộp thoại: Mediation Debugger báo không có đồng ý. Rút đồng ý qua nút quyền riêng tư: không có dòng `MAX started` mới. Kết quả khác thì phản hồi theo mục 3.4 |
| Kiểm thủ công 3, staging không khóa | Kiểm tra thủ công (người: đọc log của bản staging do `staging.yml` dựng) | Bản staging của CI: thẻ `Ads` có đúng một dòng `MAX skipped: no SDK key`, không có thẻ `AppLovinSdk` báo khởi tạo MAX; 10 dòng `Adapter ...` như cũ |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.startSdk` (gọi `startAdsSdks` với `MobileAds.initialize` thật) và `AppLovinMaxPlatform` (gọi SDK thật) không chạy được trong test JVM: thứ tự kiểm bằng `AdsSdkStartTest`, điều kiện khởi tạo bằng `MaxSdkStarterTest`, nối bằng `AdsControllerMaxTest` (không mở cổng thì không gọi) và kiểm thủ công 1, 2. `MaxPlatformModule.kt`, `MaxConfigModule.kt` là nối Hilt, kiểm bằng `scripts/check.sh` (build `:app` với Hilt). Tài liệu không có test. Người duyệt đồng ý ngoại lệ này.

Thứ tự commit (L-025): commit test của bước 3 gồm mọi test trên (cả câu sửa của `AdsSettingsTest`, `AdsFlagsTest`, `ProductionPropertiesTest`, hai test script) cùng khung rỗng tối thiểu để biên dịch: `MediationOrder.kt`, `MaxSdk.kt`, `AdsSdkStart.kt` với thân `TODO()` hay trả giá trị sai, tham số mới của `testAdsController` và constructor `AdsController`. Khi đó test classpath đỏ (`ClassNotFoundException` adapter Google), test thuần đỏ, test script `a MAX SDK key fails` đỏ (trả 0), `ProductionPropertiesTest` mới đỏ. AI ghi lần chạy đó vào nhật ký. Commit code của bước 4 thêm thư viện, code, cấu hình build, rồi sinh lại bảng khóa.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-29, trong worktree riêng của nhánh tạm `claude/trial-T-068-local` tạo từ `origin/main` `c29ce2b`, không commit, không push; xóa worktree và nhánh sau khi chạy. JDK và SDK từ `scripts/local-setup.sh` (`source` tệp `env.sh`), `local.properties` chỉ có `sdk.dir` (không có khóa MAX). Code thử, gần đúng mục 3.1: hai phiên bản và hai thư viện trong catalog (bước 1), hai dòng `implementation` (bước 2); `MaxSdk.kt` thử gộp `MaxConfig`, `MediationOrder` (`of`, `effective`), `MaxPlatform`, `AppLovinMaxPlatform` gọi đúng `AppLovinSdkInitializationConfiguration.builder(sdkKey).setMediationProvider(AppLovinMediationProvider.MAX).build()` và `AppLovinSdk.getInstance(context).initialize(...)`, `MaxSdkStarter`; `AdsController` thêm tham số `maxConfig`, khởi tạo MAX trong `startSdk` sau `withContext(Dispatchers.IO) { MobileAds.initialize(...) }`; `testAdsController` cấp `MaxConfig.NONE`; `app/build.gradle.kts` đọc `maxSdkKey` của `local.properties` ở staging, `-PmaxSdkKey` ở production, kiểm `preProduction*Build` theo nhóm `com.applovin.mediation`; `MaxConfigModule.kt` của `:app`; test tạm `MaxTrialTest` (`:core:ads`, 3 test: hai lớp có trên classpath và adapter là `MaxAdapter`; `of`, `effective`; bộ khởi tạo chỉ chạy một lần khi có khóa, không chạy khi không khóa, bằng bản giả). Chưa thử phần `secret-scan.py` (script Python thuần, cùng mẫu đã có của T-034) và phần khóa Remote Config (mẫu đã có của T-019).
- **Lệnh:** `./gradlew :app:processStagingDebugMainManifest` trước và sau, so bằng script Python tạm và `diff`; `./gradlew :app:dependencies --configuration stagingReleaseRuntimeClasspath` trước và sau, so danh sách tọa độ; `javap` trên `applovin-sdk-13.6.4.aar` và `google-adapter-25.5.0.0.aar` trong bộ nhớ đệm Gradle; `scripts/check.sh`; `./gradlew :app:assembleStagingRelease`.
- **Kết quả:**
  - Tải thư viện: lần đầu tải `google-adapter:25.5.0.0` từ Maven Central; không cần kho mới.
  - Lỗi biên dịch đầu tiên của `app/build.gradle.kts`: `java.util.Properties()` trong khối `android { }` báo "Unresolved reference: util" (`java` ở đó là extension `java` của Gradle). Sửa bằng `import java.util.Properties` ở đầu file; plan ghi cách này (bước 9).
  - Manifest gộp: giống hệt trước và sau (mục 2, bảng). 11 quyền, `AppLovinInitProvider` vẫn bị bỏ.
  - Phụ thuộc: so tập tọa độ trước và sau, chỉ thêm một dòng `com.applovin.mediation:google-adapter:25.5.0.0`. `com.applovin:applovin-sdk:13.6.4` có ở cả đường của adapter AdMob và đường khai báo trực tiếp, cùng bản. `play-services-ads` 25.5.0, `user-messaging-platform` 4.0.0 không đổi; không thư viện nào bị nâng.
  - `scripts/check.sh` lần 1 (356 giây): **thất bại** chỉ ở `:core:ads:ktlintTestSourceSetCheck`, một dòng quá 120 ký tự trong test tạm `MaxTrialTest` (`Class.forName(...)` trên một dòng); lint, unit test (`MaxTrialTest` 3/3 qua), screenshot, `assembleStagingDebug` đều chạy và qua. Ngắt dòng đó (lỗi của test tạm, không phải của thư viện). Lần 2 (304 giây): **qua**, "Cổng chất lượng: QUA", exit 0; 1458 test JVM, 0 lỗi (`:app` 148, `:core:ads` 1069), 89 ảnh screenshot, không ảnh mới hay đổi; 1148 test script `ok`, 0 `not ok`. Lint không báo lỗi mới từ SDK MAX hay adapter Google.
  - `./gradlew :app:assembleStagingRelease` (R8, 140 giây): **qua**, exit 0, ra `app-staging-release-unsigned.apk`; không cần luật giữ lớp. 22 cảnh báo "Missing class", đúng 22 cảnh báo đã biết từ T-034 (20 lớp `com.android.billingclient.api.*` của SDK TikTok đi kèm Pangle, 2 lớp `com.bytedance.sdk`); không cảnh báo nào từ `com.applovin`. Thư viện gốc `libapplovin-native-crash-reporter.so` của SDK được đóng gói nguyên (đã có từ T-015).
  - Chưa thử trên máy (không có khóa SDK MAX thật): MAX khởi tạo xong, đồng ý qua TCF, chín adapter AdMob vẫn được khởi tạo khi MAX khởi tạo sau (kiểm thủ công 1 đến 3).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

1. Quảng cáo: thêm nền tảng quảng cáo thứ hai và adapter mediation của nó (điểm 1).
2. SDK bên thứ ba mới: SDK MAX dùng trực tiếp lần đầu và adapter Google của MAX (điểm 2).
3. Đồng ý và thứ tự khởi tạo SDK: MAX khởi tạo sau cổng đồng ý và sau `MobileAds.initialize`, đồng ý qua chuỗi TCF (điểm 5).
4. Module core `:core:ads`, cấu hình build (`app/build.gradle.kts`, file được bảo vệ `gradle/libs.versions.toml`) và script dùng chung `scripts/secret-scan.py` (điểm 7).

Không chạm điểm 3 (quyền mới): manifest gộp không đổi (đã đo). Không chạm điểm 4 (sự kiện phân tích).

Mức: **Cao**, không tính tổng điểm. Bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt vì chạm quảng cáo và đồng ý (`risk-scoring.md`, mục 3). Chấm năm tiêu chí để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads` (bốn file mới, ba file sửa), `:app` (build, Hilt, test), catalog, script dùng chung, ba tài liệu |
| B. Độ rõ của yêu cầu | 2 | ADR-0037 và câu hỏi 41 tới 47 đã chốt, nhưng còn câu ảnh hưởng cách làm: đồng ý qua TCF chưa có tài liệu chính thức nói thẳng (câu hỏi 2), thứ tự với `disableMediationAdapterInitialization` (câu hỏi 3), `max` khi không khóa (câu hỏi 4), cách hiểu "khi thứ tự mặc định có MAX" (câu hỏi 6), spec mục 6 và 8 chờ sửa (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 2 | Luật thuần, khóa, thứ tự, classpath, build, script có test; việc MAX thật sự khởi tạo, đọc đồng ý và không phá mediation AdMob chỉ kiểm trên máy có khóa thật |
| D. Ảnh hưởng chéo | 3 | Mọi app tạo từ dự án mẫu nhận SDK MAX trực tiếp; build phát hành hỏng tới khi T-073 truyền `maxSdkKey` (câu hỏi 8); T-053, T-066 cũng sửa `AdsSettings.kt`, `AdsFlags.kt` hay `AdsController.kt`; T-058 cũng đọc `local.properties` trong `app/build.gradle.kts`; T-069 tới T-071 dựa vào `mediationOrder`, `maxReady` |
| E. Độ mới của kỹ thuật | 2 | Thư viện trong catalog, tham số build, khóa Remote Config đã có mẫu; mới là gọi thẳng SDK MAX, đọc `local.properties` trong build, và thứ tự giữa hai SDK đều khởi tạo Google Mobile Ads |
| **Tổng** | 12 | **Mức: Cao** (đã cao vì điểm chặn) |

Rủi ro riêng: nếu MAX không đọc được đồng ý từ chuỗi TCF, MAX phục vụ quảng cáo không cá nhân hóa ở EEA (mất doanh thu, không phải lỗi tuân thủ theo hướng xấu); nếu đọc sai theo hướng ngược lại thì là lỗi tuân thủ GDPR, nên kiểm thủ công 2 phải làm trước bản production đầu tiên có MAX. Adapter Google của MAX tắt khởi tạo adapter mediation của AdMob nếu chạy trước (mục 2). AppLovin hai vai: nếu nhóm mediation AdMob còn "AppLovin (Bidding)", adapter AppLovin của AdMob và MAX cùng khởi tạo một đối tượng `AppLovinSdk` với hai bên điều phối khác nhau; câu hỏi 45 đã chốt bỏ AppLovin khỏi nhóm AdMob khi app dùng MAX (việc của người, T-073), code không chặn được (câu hỏi 5).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `core/ads/build.gradle.kts`, `AdsController.kt`, `AdsSettings.kt`, `AdsFlags.kt`, `AdsControllerFixtures.kt`, `ProductionPropertiesTest.kt`, `scripts/secret-scan.py`, `scripts/tests/run.sh`, bảng khóa sinh tự động, hai tài liệu vận hành.
- Task khác có thể xung đột:
  - T-053 (đếm ngược và bung lại theo JBase) thêm `time_close_native` và đổi mặc định `time_expand_collap_ad` trong `AdsSettings.kt`, `AdsFlags.kt`, `AdsSettingsTest.globalKeys_matchAdsKitTable`, `AdsFlagsTest`, bảng khóa: cùng chỗ của plan này (số khóa, chỉ số trong `combine`). Task chạy sau gộp phần của task trước (số khóa 15 thành 16, chỉ số `values[...]`); không chạy song song.
  - T-066 (`CACHED_EARLY` luôn bật) sửa đồng ý trong `AdsController.kt`, `ConsentCoordinator.kt`, mô tả `is_enable_consent_shortcut`: khác hàm với `startSdk`, nhưng cùng file `AdsController.kt`. MAX đi theo `onGateOpened(firstTime = true)`, nên `CACHED_EARLY` mở cổng sớm thì MAX cũng khởi tạo sớm bằng đồng ý thật của lần trước, đúng S-03.
  - T-058 (máy thử và log từng mạng) đọc `ads.testDeviceIds` từ `local.properties` trong `app/build.gradle.kts` (spec câu hỏi 40): cùng cách đọc, cùng file. Plan nào chạy trước tạo đoạn đọc `local.properties` (một biến `localProperties` ở đầu khối staging); plan kia dùng lại, không đọc file hai lần (L-054). Tên khóa cùng tiền tố `ads.`.
  - T-056 (mã đơn vị theo khe) có thể sửa `app/build.gradle.kts` hay `ProductionAdsModule.kt`: khác dòng.
  - T-069 tới T-071 phụ thuộc plan này: đọc `AdsController.mediationOrder`, `maxReady`, `MaxPlatform` (mở rộng thêm hàm nạp và hiện), `mayRequestNow()`. T-072 ghi doanh thu MAX. T-073 (`auto_run: false`) truyền `maxSdkKey` trong `scripts/release.sh` và hướng dẫn bỏ AppLovin khỏi nhóm mediation AdMob.
- Đề xuất thứ tự chạy: sau T-053 và T-066 nếu cùng danh sách (để chúng giữ thứ tự khóa của bảng 10.1, plan này thêm khóa ở cuối), trước T-069. T-073 nên được làm ngay sau khi task này merge, trước bản phát hành kế tiếp (câu hỏi 8). Không chạy cùng phiên với task khác sửa `gradle/libs.versions.toml`.

## 7. Điều kiện dừng riêng

1. Một tọa độ ở mục 3.3 không tải được từ `google()` hay `mavenCentral()`, cần kho Maven khác, hoặc Gradle chọn `applovin-sdk` khác 13.6.4, `play-services-ads` khác 25.5.0, hay đổi phiên bản của bất kỳ thư viện nào khác so với mục 4.1.
2. Manifest gộp khác trước khi thêm thư viện (quyền, provider, receiver, meta-data, `<queries>`), hay cần ghi gì vào `core/ads/src/main/AndroidManifest.xml`.
3. Build hay R8 cần luật giữ lớp (`consumer-rules.pro`, `proguard-rules.pro`).
4. API của SDK `13.6.4` lúc chạy khác mục 3.1 bước 4 (chữ ký `builder`, `initialize`), hay tài liệu chính thức lúc chạy yêu cầu gọi `setHasUserConsent`, `setDoNotSell` khi dùng UMP: dừng, không tự chọn cách truyền đồng ý (điểm chặn 5).
5. Cần gọi bất kỳ API nào của MAX trước khi cổng đồng ý mở, gọi `AppLovinPrivacySettings` với hằng số, hay bật luồng đồng ý riêng của MAX.
6. Cần khóa SDK MAX, mã đơn vị MAX hay khóa thật nào ở bất kỳ đâu trong repo, kể cả trong test (khóa giả luôn có `FAKE`).
7. Cần sửa `scripts/release.sh` hay `docs/ops/release.md` (T-073).

## 8. Câu hỏi cho người duyệt

1. **Đề xuất sửa spec ads mục 6 và mục 8 (câu hỏi 18; ADR-0037 "đo ở plan, ghi vào spec mục 6").** AI không sửa spec `confirmed`; đề xuất người thêm:
   - Mục 6, Quyền của app và dịch vụ bên ngoài, thay đoạn "Thêm 2026-10-29 (ADR-0037, chờ câu hỏi 41 tới 47)" bằng một đoạn mới (giữ đoạn cũ gạch bỏ như lệ của spec):

     > **Thêm 2026-10-29 (đo ở plan T-068):** SDK AppLovin MAX `13.6.4` (cùng thư viện mà adapter AppLovin của AdMob đã kéo vào từ T-015) và adapter Google của MAX `25.5.0.0` không thêm quyền hay thành phần nào vào manifest gộp; vẫn bỏ `AppLovinInitProvider`, `CHECK_LICENSE`, `ACCESS_WIFI_STATE` như T-015, giữ `VIBRATE`. MAX chỉ khởi động sau khi cổng đồng ý của app mở và sau Google Mobile Ads, đọc đồng ý từ chuỗi TCF của UMP; app không viết cứng đồng ý. Khóa SDK MAX đưa vào lúc build phát hành, không vào repo; bản staging không có khóa thì MAX không khởi động. Play Console: An toàn dữ liệu đã khai AppLovin (T-015); APS chưa làm.
   - Mục 8, bảng khóa "Thêm 2026-10-23": dòng `ads_mediation_order` | string | `max_admob` | Thứ tự hai nền tảng cho banner, MREC, interstitial, rewarded, app open khi quay lại: `admob`, `max`, `admob_max`, `max_admob`; giá trị khác dùng `max_admob`; bản không có khóa SDK MAX dùng `admob` (ADR-0037, tên do ta đặt).
   - Plan chờ người xác nhận hai đoạn này (spec chuyển `changed` rồi `confirmed` lại) trước khi duyệt.
2. **Đồng ý của MAX qua chuỗi TCF, không gọi `setHasUserConsent`.** Bằng chứng: SDK `13.6.4` có các khóa `IABTCF_*` trong bytecode; trả lời của AppLovin trên diễn đàn của Google (từ 12.0.0 đọc chuỗi từ SharedPreferences); trang của AppLovin chỉ nói CMP phải có kết quả trước khi khởi tạo. Chưa có câu chính thức "SDK tự đọc TCF khi app dùng UMP của mình".
   - Đề xuất của AI: theo plan (không gọi cờ, MAX đọc TCF và AC), vì đúng chữ ADR-0037 "đọc chuỗi đồng ý của UMP" và tránh hai nguồn đồng ý lệch nhau; kiểm bằng Mediation Debugger trước bản production đầu tiên có MAX (kiểm thủ công 2); sai thì phiên bản sau dùng mục 3.4 (đọc TCF rồi đặt `setHasUserConsent`, không bao giờ hằng `true`).
   - Cách khác: gọi `setHasUserConsent` ngay ở T-068 từ chuỗi TCF. Không đề xuất trước khi có số đo: cờ nhị phân thay chuỗi TCF có thể làm MAX bỏ qua chi tiết mục đích và nhà cung cấp.
   - Luật các bang Mỹ (`setDoNotSell`): UMP không có thông điệp các bang Mỹ trong dự án mẫu; plan không gọi, như T-015 câu hỏi 4. Người quyết có cần ticket riêng không.
3. **Thứ tự với `MobileAds.disableMediationAdapterInitialization`.** Adapter Google của MAX gọi hàm này mỗi lần khởi tạo đầu tiên. Plan bảo đảm `MobileAds.initialize` của ta được gọi trước (test thứ tự), nhưng Google không viết hàm này làm gì khi gọi sau `initialize`. Chín adapter AdMob có còn được khởi tạo không chỉ kiểm được trên máy có khóa MAX (kiểm thủ công 1).
   - Đề xuất của AI: theo plan; nếu kiểm thủ công 1 thấy thiếu adapter, phản hồi và plan sau chờ callback của `MobileAds.initialize` (tối đa 35 giây, S-21) rồi mới khởi tạo MAX.
4. **`ads_mediation_order = max` khi build không có khóa.** ADR-0037 ghi "staging không có khóa thì thứ tự coi như `admob`". Plan áp cho mọi giá trị, kể cả `max` (chỉ MAX): build không khóa thì vẫn hiện AdMob thay vì không có quảng cáo nào.
   - Đề xuất của AI: theo plan. Cách khác: `max` khi không khóa là không có quảng cáo ở năm định dạng; không đề xuất vì staging của CI luôn không khóa và người vận hành có thể đặt `max` trên Firebase staging.
5. **Phạm vi adapter.** (a) Không thêm `google-ad-manager-adapter` của MAX mà JBase có (dự án mẫu không dùng Google Ad Manager); (b) giữ adapter AppLovin của AdMob (`com.google.ads.mediation:applovin`) trong bản build dù câu hỏi 45 chốt bỏ AppLovin khỏi nhóm mediation AdMob khi app dùng MAX: adapter chỉ khởi tạo khi nhóm mediation có AppLovin, và app có thể chọn `ads_mediation_order = admob` cùng AppLovin trong nhóm AdMob.
   - Đề xuất của AI: theo (a) và (b); T-073 ghi trong hướng dẫn phát hành rằng app dùng MAX bỏ "AppLovin (Bidding)" khỏi mọi nhóm mediation AdMob. Người duyệt quyết nếu muốn bỏ adapter AppLovin của AdMob như JBase (khi đó là ticket riêng, sửa `MergedManifestTest`, T-015).
6. **"`preProduction*Build` báo lỗi khi thiếu mà thứ tự mặc định có MAX".** Mặc định nằm trong code Kotlin của `:core:ads`, build Gradle không đọc được. Plan đòi `-PmaxSdkKey` khi adapter Google của MAX có trên classpath chạy của bản production (như `appsFlyerDevKey`, issue #261): với mặc định `max_admob` của dự án mẫu, đúng chữ của ticket; app muốn bỏ MAX thì bỏ thư viện.
   - Đề xuất của AI: theo plan. Cách khác: thêm tham số `-PadsMediationOrderDefault` để build biết mặc định; không đề xuất vì thêm một nơi giữ mặc định thứ hai.
7. **Mẫu mã đơn vị MAX trong `secret-scan.py`.** Plan chỉ mở rộng mẫu khóa SDK (86 ký tự) cho dòng có tên tham số MAX. Mã đơn vị MAX (16 ký tự hex) chưa có ở đâu và dễ trùng chuỗi hex khác.
   - Đề xuất của AI: để T-073 (bảng mã MAX trong `release.properties`) quyết mẫu và tên tham số.
8. **Khoảng giữa T-068 và T-073.** Sau khi task này merge, `scripts/release.sh` (chưa truyền `maxSdkKey`) dừng ở `preProductionReleaseBuild` với "Build production cần tham số thật: -PmaxSdkKey (ADR-0037)". Phiên tự chạy không được sửa `scripts/release.sh`.
   - Đề xuất của AI: chấp nhận; anh làm T-073 trước bản phát hành kế tiếp có MAX. Cần phát hành gấp trước T-073 thì đặt biến `ORG_GRADLE_PROJECT_maxSdkKey` trong shell trước khi chạy `release.sh` (Gradle đọc biến này như `-PmaxSdkKey`; `release.sh` không xóa môi trường khi gọi Gradle).
9. **Người phụ trách sản phẩm biết trước khi duyệt** (`risk-scoring.md` mục 3: rủi ro cao chạm quảng cáo và đồng ý): anh Nhật vừa là người phụ trách sản phẩm vừa là người duyệt; xác nhận đã đọc mục 2 (đồng ý qua TCF) và câu hỏi 2.
10. **Duyệt chéo khi nhóm chỉ có một dev:** theo `risk-scoring.md` mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
