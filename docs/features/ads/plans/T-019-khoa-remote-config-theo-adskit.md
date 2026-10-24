---
ticket: T-019
title: Khóa Remote Config quảng cáo theo tên AdsKit và danh sách vị trí theo mô hình chung
status: done                    # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, build)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý 15M (scripts/token-usage.py estimate, 2026-10-24); nâng vì khoảng 40 file ở ba module
blocking_points: [ads, core, build]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                # người duyệt
cross_reviewer: Nhật          # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-019: KHÓA REMOTE CONFIG QUẢNG CÁO THEO TÊN ADSKIT VÀ DANH SÁCH VỊ TRÍ THEO MÔ HÌNH CHUNG

## 1. Tóm tắt

Thay toàn bộ khóa quảng cáo của đợt 2 trong `AdsSettings` bằng bảng khóa ở `docs/ops/ads-business-rules.md`, mục 10 (tên AdsKit, kể cả `show_resum_ads`), thêm 11 công tắc `is_log_af_*` ở `:core:analytics`, và cho `AdPlacement` mang định dạng theo danh sách đóng MC-02, nhóm giãn cách và tham số `native_every`, từ đó suy ra khóa `is_show_<mã vị trí>`, `ads_interval_<nhóm>`, `<tham số>_<mã vị trí>`. Một bộ cờ `AdsFlags` dạng `Flow` (công tắc tổng đã ghép sẵn) thay mọi chỗ `:core:ads` đọc Remote Config; bộ tần suất của T-009 (ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu trên máy) bị gỡ. Kiểm chứng bằng unit test cho từng khóa, từng mặc định, đọc lại sau khi đổi giá trị, vị trí giả, test chứng minh lớp và khóa cũ không còn, bảng khóa sinh lại khớp code.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23, câu hỏi 7, 8, 10, 19, 20, 21, 26, 27, 28 đã trả lời):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-7 | Khi người vận hành tắt công tắc tổng trên Firebase thì ở lần cập nhật cấu hình tiếp theo app không còn tải hay hiện quảng cáo nào, ở mọi vị trí. | Công tắc tổng là `is_show_ads` (boolean, mặc định bật, câu hỏi 7). `AdsFlags.placementEnabled(placement)` trả `is_show_ads && is_show_<mã vị trí>`; mọi đường tải và hiện của `:core:ads` (banner, native, trong danh sách, thu gọn, interstitial, native toàn màn hình, tải sẵn) đã đi qua `AdsController.isEnabled` từ T-009, nay `isEnabled` đọc `AdsFlags`. Cờ toàn app `showOpenAds`, `showOpenAdsFirstOpen`, `showResumeAds`, `showNativeAfterInter` của `AdsGlobalFlags` cũng đã ghép công tắc tổng, để T-024, T-025, T-026 không phải tự ghép. |
| KB-7 | Khi bật lại thì các vị trí đang bật hoạt động lại mà không cần cập nhật app. | Các flow phát lại giá trị mới khi `is_show_ads` đổi; vị trí có `is_show_<mã>` tắt vẫn tắt. |
| KB-10 | Khi thêm một khóa trong code mà chưa sinh lại bảng khóa thì kiểm tra tự động báo lỗi. | Đã có từ T-007 (`RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`). Task này sinh lại hai file bằng lệnh của T-007 và giữ hai test đó. |
| KB-10 | Bảng khóa liệt kê mọi khóa với kiểu, giá trị mặc định, module và mô tả; có file mẫu nhập được vào Firebase. | Bảng sinh từ `AppRemoteKeys`; thêm module `:core:analytics` (11 khóa `is_log_af_*`). Khóa "tên do ta đặt" (không có ở `DG:171-181`) ghi chú ngay trong mô tả; khóa suy ra theo vị trí ghi "Derived" trong mô tả (câu hỏi 9 ở mục 8). |
| KB-10 | Module core đọc được khóa kiểu số thực và chuỗi. | Kiểu số thực đã có test ở T-007 (`DefaultRemoteSettingsTest`). Chuỗi: `open_ad_type`, `resume_ad_type` là hai khóa chuỗi đầu tiên `:core:ads` đọc thật, có test đọc qua `RemoteSettings`. |
| KB-10 | Mọi khóa quảng cáo có tên giống hệt bảng khóa ở mục 8 (tên AdsKit, kể cả `show_resum_ads`). | 14 khóa chung của mục 10.1 (trừ 11 khóa `is_log_af_*` khai báo ở `:core:analytics`) có đúng tên, kiểu, mặc định của bảng; không khai báo `time_reload_collap_ad`, `resume_ads_interval`, `time_reload_bottom_ad`; không còn khóa nào của đợt 2. `is_show_rw_get_item`, `is_show_native_onboarding` là khóa suy ra: chỉ có khi app khai báo vị trí `rw_get_item`, `native_onboarding` (MC-03); dự án mẫu không có hai vị trí đó nên bảng khóa của dự án mẫu không có hai dòng này, test dùng vị trí giả để chứng minh chuỗi khóa trùng. |
| KB-25 | Khi dev khai báo vị trí trong spec của app và trong danh sách vị trí (mã, định dạng, thuộc tính) thì vị trí dùng được mà không sửa module core. | Khai báo là một dòng của `AdPlacement` (MC-10 coi dòng này là phần khai báo, không phải sửa logic). Khóa và cờ suy ra từ khai báo, không cần sửa `AdsSettings`, `AdsFlags` hay `AdsController`. Dòng phải giữ dạng `scripts/release.sh` đọc được (câu hỏi 1 ở mục 8). |
| KB-25 | Khi khai báo vị trí thì khóa bật tắt `is_show_<mã vị trí>` có trong bảng khóa sinh tự động, mặc định tắt. | `AdsSettings.all()` suy ra từ khai báo của mọi `AdPlacement`; bảng khóa sinh từ `AdsSettings.all()`. Test với vị trí giả (khai báo `AdPlacementDeclaration` trong test, vì `AdPlacement` là enum không thêm được phần tử giả). |
| KB-25 | Khi vị trí inter khai báo nhóm giãn cách riêng thì khóa `ads_interval_<nhóm>` có trong bảng khóa, và inter của nhóm đó không làm dời khoảng cách của nhóm mặc định. | Task này: khóa có trong bảng, mặc định theo khai báo; `AdsFlags.intervalSeconds(nhóm)` đọc khóa của nhóm, độc lập với `ads_interval`. Phần mốc riêng của từng nhóm (inter của nhóm riêng không dời mốc chung) là phạm vi của T-022 (ticket T-022, "Nhóm giãn cách"), có test ở đó. |
| KB-25 | Khi chọn mã vị trí trùng tên AdsKit (`rw_get_item`, `native_onboarding`, nhóm `sounds`) thì khóa trùng đúng chuỗi khóa mà app AdsKit đang dùng trên Firebase. | Test vị trí giả cho ba trường hợp, so chuỗi khóa. |
| KB-25 | Doanh thu của vị trí ghi với `ad_placement` bằng mã vị trí. | Ghi doanh thu là T-023 (ticket T-023: "Giá trị `ad_placement` là mã vị trí trong `AdPlacement`"). Task này chỉ giữ mã vị trí duy nhất, chữ thường gạch dưới (test có sẵn), và không đổi mã nào. |

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0003, ADR-0013, ADR-0014, ADR-0019, ADR-0023 (mục 2 tới 6), ADR-0026 (quyết định 5, 9, 11, 12; thay mục 1 của ADR-0023 cho khóa quảng cáo).

Hiện trạng đã đọc trên nhánh chính (sau đợt 2, commit `adf9450`):

- `AdsSettings` có 7 khóa (`ads_enabled`, `ads_personalized_regulated_regions_enabled`, `ads_interstitial_min_interval_seconds`, `ads_first_session_grace_seconds`, `ads_interstitial_max_per_session_count`, `ads_inlist_every_count`, `ads_native_load_timeout_seconds`) và `placementEnabled(placement)` ra `ads_<vị trí>_enabled`.
- `AdsRemoteRules.kt` (`placementActive`, `fullScreenAdLimits`) và `AdsController` đọc thẳng `RemoteSettings`. Bộ tần suất T-009: `FullScreenAdPolicy.kt`, `FullScreenAdSession.kt`, `FullScreenAdHistoryStore.kt` (hai khóa DataStore), `FullScreenAdPacer.kt`, `di/AdsDataModule.kt`. `FullScreenAdGuard` (khoảng cách trong bộ nhớ, đồng hồ đơn điệu, chờ ngừng chạm 1 giây) là của đợt 1.
- `AdFormat` có 6 giá trị; `AdPlacement(key, format)` có 8 vị trí: `sample_native_small` và 7 vị trí `gallery_*` của màn xem thử.
- `StagingRemoteValuesModule` bật `ads_<vị trí>_enabled` của mọi vị trí.

## 3. Giải pháp

### 3.1. Cách làm

**Định dạng, nhóm giãn cách, vị trí** (`AdPlacement.kt`):

- `enum class AdFormat(val code: String)`: giữ 6 giá trị cũ (thêm mã), thêm 5 giá trị ở cuối cho đủ danh sách đóng MC-02: `BANNER("banner")`, `INTERSTITIAL("interstitial")`, `NATIVE_SMALL("native_small")`, `NATIVE_MEDIUM("native_medium")`, `NATIVE_COLLAPSIBLE("native_collap")`, `NATIVE_FULL_SCREEN("native_fullscreen")`, `APP_OPEN("app_open")`, `REWARDED("rewarded")`, `BANNER_COLLAPSIBLE("banner_collapsible")`, `NATIVE_STEP("native_step")`, `NATIVE_IN_COLLECTION("native_in_collection")`. Năm định dạng mới chưa có composable hay hàm hiện (T-024, T-027, T-028, T-029, T-031); `isFullScreen` giữ nghĩa "hiện bằng `rememberShowFullScreenAd`" (inter và native toàn màn hình). `nativeLoadSize()` trả `null` cho năm định dạng mới (chú thích: pool của T-027, T-028 nạp); `GoogleTestAdUnitIds.unitFor` có mã thử cho cả 11: app open `ca-app-pub-3940256099942544/9257395921`, rewarded `ca-app-pub-3940256099942544/5224354917`, banner thu gọn dùng mã banner tự thích ứng `ca-app-pub-3940256099942544/9214589741`, `NATIVE_STEP` và `NATIVE_IN_COLLECTION` dùng mã native video `ca-app-pub-3940256099942544/1044960115`.
- `class AdIntervalGroup` (công khai): `AdIntervalGroup.DEFAULT` dùng `ads_interval`; `AdIntervalGroup.named(name, defaultSeconds)` dùng `ads_interval_<name>` với mặc định theo spec của app (MC-04). Hai nhóm bằng nhau khi cùng tên và cùng mặc định.
- `enum class AdPlacement(val key: String, val format: AdFormat, val intervalGroup: AdIntervalGroup = AdIntervalGroup.DEFAULT, val nativeEvery: Int? = null)`. `key` vẫn là mã vị trí (MC-01, cũng là giá trị `ad_placement`); giữ tên thuộc tính để không chạm khoảng 10 file đang dùng (câu hỏi 11). Mỗi phần tử có `internal val declaration: AdPlacementDeclaration`.
- `internal data class AdPlacementDeclaration(code, format, intervalGroup, nativeEvery, stagingOnly)`: kiểm khai báo ngay khi tạo: nhóm khác `DEFAULT` chỉ cho `INTERSTITIAL`; `nativeEvery` chỉ cho `NATIVE_SMALL`, `NATIVE_MEDIUM`, `NATIVE_IN_COLLECTION` và phải từ 1 trở lên; mã chữ thường gạch dưới. Khai báo sai thì ném `IllegalArgumentException` ngay khi lớp `AdPlacement` được nạp, test nào chạm `AdPlacement` cũng lộ lỗi.
- Hai vị trí trong danh sách của màn xem thử có `nativeEvery = 8` (giữ N cũ của `ads_inlist_every_count`): `GALLERY_INLIST_SMALL("gallery_inlist_small", AdFormat.NATIVE_SMALL, nativeEvery = 8),` và tương tự cho medium. Dòng của vị trí không bắt đầu bằng `gallery_` giữ đúng một dòng `TEN("ma", AdFormat.X),` (xem ràng buộc với `release.sh` bên dưới).

**Khóa** (`AdsSettings.kt`, viết lại; giữ tên đối tượng và hàm `placementEnabled`, `all` mà `:app` đang gọi):

| Hằng số | Khóa | Kiểu | Mặc định | Ghi chú trong mô tả |
| --- | --- | --- | --- | --- |
| `IS_SHOW_ADS` | `is_show_ads` | boolean | true | tên do ta đặt |
| `SHOW_OPEN_ADS` | `show_open_ads` | boolean | false | |
| `SHOW_OPEN_ADS_FIRST_OPEN` | `show_open_ads_first_open` | boolean | false | |
| `OPEN_AD_TYPE` | `open_ad_type` | string | `aoa` | |
| `SHOW_RESUM_ADS` | `show_resum_ads` | boolean | false | giữ lỗi chính tả của AdsKit |
| `RESUME_AD_TYPE` | `resume_ad_type` | string | `aoa` | |
| `ADS_INTERVAL` | `ads_interval` | long (giây) | 45 | |
| `IS_SHOW_NATIVE_AFTER_INTER` | `is_show_native_after_inter` | boolean | false | |
| `IS_SHOW_NATIVE_BOTTOM` | `is_show_native_bottom` | boolean | false | |
| `IS_SHOW_NATIVE_COLLAP` | `is_show_native_collap` | boolean | true | tên do ta đặt |
| `TIME_EXPAND_COLLAP_AD` | `time_expand_collap_ad` | long (giây) | 30 | tên do ta đặt |
| `IS_ENABLE_CONSENT_SHORTCUT` | `is_enable_consent_shortcut` | boolean | false (câu hỏi 21) | tên do ta đặt |
| `IS_LOG_AD_FULLSCREEN_EVENTS` | `is_log_ad_fullscreen_events` | boolean | false | tên do ta đặt |
| `IS_LOG_AD_SLOT_EVENTS` | `is_log_ad_slot_events` | boolean | false | tên do ta đặt |

- Khóa suy ra: `placementEnabled(placement)` ra `is_show_<mã>` (boolean, false); `interval(group)` ra `ADS_INTERVAL` cho `DEFAULT`, `ads_interval_<tên>` (long, mặc định của nhóm) cho nhóm khác; `nativeEvery(placement)` ra `native_every_<mã>` (long, mặc định `nativeEvery`) hoặc `null` khi vị trí không khai báo. Mẫu tên `<tham số>_<mã vị trí>` nằm ở một hàm nội bộ `placementParamKey(param, code, ...)` để T-031 thêm `rw_no_ad_policy_<mã>` cùng cách.
- `internal fun keysFor(placements: List<AdPlacementDeclaration>): List<RemoteKey<*>>`: 14 khóa chung theo thứ tự trên, rồi theo từng vị trí: `is_show_<mã>`, `native_every_<mã>` nếu có; rồi một `ads_interval_<nhóm>` cho mỗi nhóm riêng (gộp nhóm trùng). Tên trùng thì ném `IllegalArgumentException` (hai nhóm cùng tên khác mặc định bị bắt ở đây). `all() = keysFor(AdPlacement.entries.map { it.declaration })`.
- Mô tả (tiếng Anh, hiện cả trên Firebase, tối đa 256 ký tự): khóa chung "tên do ta đặt" kết thúc bằng `Name set by the template, not an AdsKit key.`; khóa suy ra kết thúc bằng `Derived key of placement <mã> (<mã định dạng>).` hay `Derived key of interval group <tên>.`; khóa số ghi đơn vị trong mô tả (`in seconds`, `items`), không ở cuối tên (ADR-0026, quyết định 9). Danh sách tên của `DG:171-181` để quyết ghi chú nằm trong một hằng số nội bộ `ADSKIT_KEY_NAMES`.
- Gỡ: `ENABLED`, `PERSONALIZED_IN_REGULATED_REGIONS` và hàm `requestsNonPersonalizedAds` (câu hỏi 8: chuỗi đồng ý của UMP quyết; `adRequest()` không còn thêm `npa`), `INTERSTITIAL_MIN_INTERVAL_SECONDS`, `FIRST_SESSION_GRACE_SECONDS`, `INTERSTITIAL_MAX_PER_SESSION_COUNT`, `INLIST_EVERY_COUNT` (câu hỏi 19), `NATIVE_LOAD_TIMEOUT_SECONDS` (câu hỏi 20; thời gian chờ thành hằng số `NATIVE_LOAD_TIMEOUT_MILLIS = 10_000` trong `AdsController`, giữ hành vi hiện tại tới T-027). Câu hỏi 4 ở mục 8 hỏi có giữ hai việc gỡ khóa cá nhân hóa và khóa thời gian chờ trong task này không.

**Khóa AppsFlyer** (`:core:analytics`, chủ là `analytics` theo mục 10.1): `object AppsFlyerSettings` với 11 `RemoteKey.boolean` mặc định `true`: `is_log_af_ad_revenue`, `is_log_af_inters_ad_eligible`, `is_log_af_inters_api_called`, `is_log_af_inters_displayed`, `is_log_af_rewarded_ad_eligible`, `is_log_af_rewarded_api_called`, `is_log_af_rewarded_ad_displayed`, `is_log_af_native_ad_eligible`, `is_log_af_native_api_called`, `is_log_af_native_ad_displayed`, `is_log_af_inters_displayed_n_times`; mỗi mô tả có ghi chú tên do ta đặt; `all()`. Chỉ khai báo, chưa có code đọc (T-033 đọc). `core/analytics/build.gradle.kts` thêm `implementation(project(":core:config"))` để dùng `RemoteKey`, như `:core:network` (core phụ thuộc core, ADR-0001; `checkModuleDependencies` chấp nhận). `AppRemoteKeys` thêm dòng `":core:analytics" to AppsFlyerSettings.all()`.

**Bộ cờ quảng cáo dạng `Flow`** (`AdsFlags.kt`, mới, công khai cho `:core:ads` và vỏ app):

```kotlin
interface AdsFlags {
    /** Global ad switches, master switch already applied. Emits the current value at once, then on every change. */
    val global: Flow<AdsGlobalFlags>
    /** is_show_ads && is_show_<code>. */
    fun placementEnabled(placement: AdPlacement): Flow<Boolean>
    /** ads_interval for DEFAULT, ads_interval_<name> otherwise, in seconds, never negative. */
    fun intervalSeconds(group: AdIntervalGroup): Flow<Long>
    /** native_every_<code>, or null when the placement declares no nativeEvery. */
    fun nativeEvery(placement: AdPlacement): Flow<Long?>
}

data class AdsGlobalFlags(
    val adsEnabled: Boolean,              // is_show_ads
    val showOpenAds: Boolean,             // is_show_ads && show_open_ads
    val showOpenAdsFirstOpen: Boolean,    // is_show_ads && show_open_ads_first_open
    val openAdType: OpenAdType,           // open_ad_type
    val showResumeAds: Boolean,           // is_show_ads && show_resum_ads
    val resumeAdType: OpenAdType,         // resume_ad_type
    val adsIntervalSeconds: Long,         // ads_interval, negative read as 0
    val showNativeAfterInter: Boolean,    // is_show_ads && is_show_native_after_inter
    val showNativeBottom: Boolean,        // is_show_native_bottom
    val showNativeCollap: Boolean,        // is_show_native_bottom && is_show_native_collap (D-01)
    val expandCollapAfterSeconds: Long,   // time_expand_collap_ad, negative read as 0, 0 = never
    val consentShortcutEnabled: Boolean,  // is_enable_consent_shortcut
    val logFullScreenEvents: Boolean,     // is_log_ad_fullscreen_events
    val logSlotEvents: Boolean,           // is_log_ad_slot_events
)

enum class OpenAdType { APP_OPEN, INTERSTITIAL }
```

- `OpenAdType`: chuỗi đúng bằng `inter` là `INTERSTITIAL`, mọi giá trị khác (kể cả `INTER`, rỗng) là `APP_OPEN` (A-02, A-12), dùng chung một hàm cho hai khóa.
- `internal class DefaultAdsFlags @Inject constructor(settings: RemoteSettings) : AdsFlags`, ghép bằng `combine` và `distinctUntilChanged` như `placementActive` của T-009. Bản nội bộ nhận `AdPlacementDeclaration` (`placementEnabled(declaration)`, `nativeEvery(declaration)`) để test được vị trí giả; bản công khai gọi bản nội bộ với `placement.declaration`. Số âm về 0, số quá lớn chặn ở `Long.MAX_VALUE / 1_000` như `fullScreenAdLimits` cũ.
- Hilt: `di/AdsFlagsModule.kt` (`@Binds DefaultAdsFlags` thành `AdsFlags`, `SingletonComponent`), thay `di/AdsDataModule.kt` bị xóa. Vỏ app chưa dùng ở task này; T-020 (công tắc nhánh rút gọn), T-024 (app open ở màn loading) tiêm `AdsFlags`.

**Nối vào `AdsController`:**

- Constructor bỏ `settings: RemoteSettings` và `historyStore`, thêm `flags: AdsFlags`. Không còn chỗ nào trong `:core:ads` đọc `RemoteSettings` ngoài `DefaultAdsFlags`.
- `isEnabled(placement) = flags.placementEnabled(placement)`; `inListEvery(placement)` ghép `canRequestAds`, `isEnabled`, `flags.nativeEvery(placement)`; vị trí không khai báo `nativeEvery` thì ra `null` (không chèn hàng) và ghi một dòng `AppLogger.warn` thẻ `Ads`. Không thêm `require` vào `rememberInListAds`, vì `InListAdsTest.publicApi_withoutAdsController_addsNoAdRow` dùng `SAMPLE_NATIVE_SMALL`.
- `awaitFullScreenTurn`: `canRequestAds`, `isEnabled`, rồi `FullScreenAdGuard.decide(now, flags.global.first().adsIntervalSeconds * 1_000)`; bỏ `fullScreenAdLimits` và `pacer.check`. `onFullScreenShown` chỉ còn `guard.onShown`. `onConsentFlowFinished` không còn ghi thời điểm mở app lần đầu. `onAppForeground`, `onAppBackground` giữ chữ ký (MainActivity gọi), chỉ còn đếm activity và ghi log chuyển tiền cảnh, nền (T-025 dùng lại).
- `nativeAd`, `loadNativeAd` dùng hằng số `NATIVE_LOAD_TIMEOUT_MILLIS`; `adRequest()` không còn `npa`.

**Gỡ bộ tần suất của T-009** (câu hỏi 10): xóa `FullScreenAdPolicy.kt`, `FullScreenAdSession.kt`, `FullScreenAdHistoryStore.kt`, `FullScreenAdPacer.kt` (cả `AdClock`, `SystemAdClock`), `AdsRemoteRules.kt`, `di/AdsDataModule.kt` và các test chỉ phục vụ chúng. `FullScreenAdGuard` giữ nguyên: sau task, khoảng cách giữa hai quảng cáo toàn màn hình chỉ sống trong process, đo bằng đồng hồ đơn điệu, đọc `ads_interval` (I-03, AR-01); mốc vẫn ghi lúc hiện và vẫn tính cả native toàn màn hình, chờ ngừng chạm 1 giây vẫn còn. Ba điểm này là phạm vi của T-022 (mốc theo I-01 tới I-05, nhóm giãn cách) và câu hỏi 9 của spec; câu hỏi 8 ở mục 8 xin xác nhận ranh giới. `core/ads/build.gradle.kts` giữ `:core:data` (T-024, T-033 cần lưu dấu mở lần đầu và số lần inter).

**Giá trị cục bộ của staging** (`StagingRemoteValuesModule`): bật `is_show_<mã>` của mọi vị trí (vị trí thêm sau tự có), và `show_open_ads`, `show_open_ads_first_open`, `show_resum_ads`, `is_show_native_after_inter`, `is_show_native_bottom`, `is_log_ad_fullscreen_events`, `is_log_ad_slot_events`; không đặt `is_enable_consent_shortcut` (giữ tắt, cần cả cấu hình lúc build, S-12), `is_show_ads` (mặc định đã bật), loại quảng cáo và khoảng cách (giữ mặc định production). Các khóa chưa có code đọc thì chưa đổi gì trên staging; chúng có tác dụng khi T-024 tới T-030 merge (câu hỏi 3 ở mục 8).

**Ràng buộc với `scripts/release.sh`** (AI không sửa file này): script đọc vị trí bằng `sed -nE 's/^[[:space:]]*[A-Z0-9_]+\("([a-z0-9_]+)", AdFormat\.[A-Z_]+\),?$/\1/p'` (dòng 42) và bỏ mã `gallery_*`. Đã chạy thử ngày 2026-10-24 bằng `scripts/release.sh --list-placements` trên `AdPlacement.kt` hiện tại (ra `sample_native_small`) và trên hai dòng mẫu: dòng `X_Y("x_y", AdFormat.INTERSTITIAL, intervalGroup = AdIntervalGroup.named("sounds", 30)),` **không** được đọc, dòng `A_B("a_b", AdFormat.NATIVE_SMALL),` được đọc. Vị trí bị bỏ sót thì bản phát hành không đòi mã production cho nó và vị trí im lặng không có quảng cáo. Vì vậy:

- Trong task này mọi vị trí không phải `gallery_*` giữ đúng dạng một dòng script đọc được (dự án mẫu chỉ có `sample_native_small`, không có nhóm riêng hay tham số).
- Thêm test `AdPlacementSourceTest` đọc `src/main/.../AdPlacement.kt`, áp đúng biểu thức của `release.sh` (chép lại, chú thích dẫn dòng 42), so tập mã đọc được với `AdPlacement.entries.filterNot { it.stagingOnly }`. App khai báo vị trí có nhóm riêng hay tham số mà chưa sửa script thì test này đỏ, thay vì bản phát hành thiếu mã. Sửa biểu thức trong `release.sh` là việc của người (câu hỏi 1 ở mục 8).
- `docs/ops/ads-guide.md` mục 5 bước 2 (câu hỏi 1 ở mục 8): bỏ dạng `TEN_VI_TRI("ten_vi_tri", AdFormat.X, ...),`, ghi dạng đúng `TEN_VI_TRI("ten_vi_tri", AdFormat.X),` và giới hạn hiện tại: dòng của vị trí không phải `gallery_*` phải là một dòng, không có thuộc tính (nhóm giãn cách riêng, `nativeEvery`, `ignoreInterval`...) sau định dạng, cho tới khi người sửa biểu thức dòng 42 của `release.sh`; dòng có thuộc tính làm `AdPlacementSourceTest` đỏ. Vị trí cần thuộc tính thì hỏi người sửa script trước khi khai báo.

**Hệ quả chuyển đổi cho vị trí của đợt 2:**

| Vị trí | Khóa cũ | Khóa mới |
| --- | --- | --- |
| `sample_native_small` | `ads_sample_native_small_enabled` | `is_show_sample_native_small` |
| `gallery_native_small`, `gallery_native_medium`, `gallery_native_collapsible`, `gallery_interstitial`, `gallery_native_full_screen` | `ads_gallery_<...>_enabled` | `is_show_gallery_<...>` |
| `gallery_inlist_small`, `gallery_inlist_medium` | `ads_gallery_inlist_<...>_enabled`, `ads_inlist_every_count` | `is_show_gallery_inlist_<...>`, `native_every_gallery_inlist_<...>` (8) |
| Mọi vị trí | `ads_enabled` | `is_show_ads` |
| Inter, native toàn màn hình | `ads_interstitial_min_interval_seconds` | `ads_interval` |

- Firebase: khóa mới mặc định tắt. App nào đã bật vị trí bằng khóa cũ thì trước khi phát hành bản có T-019 phải tạo khóa mới trên Firebase; khóa cũ giữ cho bản app cũ tới khi không còn ai dùng (`remote-config.md`, mục 2). Ghi vào mô tả PR để người vận hành biết.
- Staging: giá trị cục bộ mới bật đủ, người thử không thấy khác.
- Máy người dùng: hai khóa DataStore `ads_first_open_at_millis`, `ads_last_full_screen_shown_at_millis` không còn được đọc hay ghi; không xóa, không chuyển đổi (điểm chặn 9 không chạm; câu hỏi 7 ở mục 8).
- Hành vi: không còn ân hạn 180 giây sau khi cài, không còn tối đa 4 quảng cáo toàn màn hình mỗi phiên, khoảng cách không còn tính qua lần mở app (KB-6 đổi 2026-10-23).

Log (ADR-0019): giữ thẻ `Ads`; không ghi tên khóa kèm giá trị của người dùng, chỉ ghi lý do ("in-list placement has no nativeEvery").

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `AdFormat(code)` 11 giá trị; `AdIntervalGroup`; `AdPlacement` thêm `intervalGroup`, `nativeEvery`, `declaration`; `AdPlacementDeclaration` có kiểm khai báo; `nativeEvery = 8` cho hai vị trí `gallery_inlist_*`; KDoc theo khóa mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Viết lại theo bảng ở mục 3.1: 14 khóa chung, khóa suy ra, `keysFor`, `all`, `ADSKIT_KEY_NAMES`; gỡ 7 khóa cũ và `requestsNonPersonalizedAds` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsFlags.kt` | `AdsFlags`, `AdsGlobalFlags`, `OpenAdType`, `DefaultAdsFlags` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/AdsFlagsModule.kt` | Hilt `@Binds` `DefaultAdsFlags` thành `AdsFlags` |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/AdsDataModule.kt` | Kho lịch sử của T-009 bị gỡ |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Nhận `AdsFlags` thay `RemoteSettings` và kho lịch sử; `isEnabled`, `inListEvery`, `awaitFullScreenTurn`, `onFullScreenShown`, `onConsentFlowFinished`, `onAppForeground`, `onAppBackground`, `adRequest`, hằng số `NATIVE_LOAD_TIMEOUT_MILLIS`; `nativeLoadSize()` có nhánh cho 5 định dạng mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | `GoogleTestAdUnitIds.unitFor` cho 5 định dạng mới (mã thử của Google ở mục 3.1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Chỉ KDoc của `rememberInListAds`: `is_show_ads`, `is_show_<mã>`, `native_every_<mã>` thay khóa cũ |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsRemoteRules.kt` | Thay bằng `AdsFlags.kt` |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPolicy.kt` | Ân hạn, giới hạn mỗi phiên, khoảng cách lưu trên máy (câu hỏi 10 của spec) |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdSession.kt` | Bộ đếm phiên của giới hạn mỗi phiên |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdHistoryStore.kt` | Hai khóa DataStore của T-009 |
| Xóa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPacer.kt` | Bộ điều phối T-009, `AdClock`, `SystemAdClock` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Viết lại: bảng khóa chung, mặc định, khóa suy ra, vị trí giả (KB-25), khóa cũ đã gỡ, ghi chú tên do ta đặt (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | KB-7, mặc định, đọc lại sau khi đổi, khóa chuỗi, nhóm giãn cách, `native_every`, chỉ đọc khóa đã khai báo |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementSourceTest.kt` | Dòng khai báo vị trí đọc được bằng biểu thức của `release.sh` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RemovedPacingRulesTest.kt` | Lớp T-009 không còn; `AdsController` không nhận `RemoteSettings` hay kho lịch sử |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Thêm test kiểm khai báo (nhóm, `nativeEvery`) và `nativeEvery` của vị trí hiện có; giữ ba test cũ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` | Thêm test mã định dạng là danh sách đóng MC-02; giữ test cũ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Thêm test mã thử của 5 định dạng mới; xóa `nonPersonalizedAds_onlyInRegulatedRegionsWithoutRemoteSwitch` (hàm bị gỡ theo câu hỏi 8 của spec) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeRemoteSettings.kt` | Ghi lại tên khóa đã đọc (`readNames`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdsTest.kt` | Dựng `AdsController` với `flags = DefaultAdsFlags(FakeRemoteSettings())`, bỏ `historyStore`, `settings` |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsRemoteRulesTest.kt` | KB-7 chuyển sang `AdsFlagsTest`; giới hạn T-009 bị gỡ |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPolicyTest.kt` | Lớp bị gỡ |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdSessionTest.kt` | Lớp bị gỡ |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdHistoryStoreTest.kt` | Lớp bị gỡ |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPacerTest.kt` | Lớp bị gỡ |
| Xóa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeAdTestDoubles.kt` | Chỉ phục vụ các test bị xóa (`FakeAdClock`, `InMemoryFullScreenAdHistoryStore`, `InMemoryDataStore`, `UnreadableDataStore`) |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettings.kt` | 11 khóa `is_log_af_*`, mặc định true, `all()` |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt` | Tên, kiểu, mặc định, ghi chú tên do ta đặt |
| Sửa | `core/analytics/build.gradle.kts` | `implementation(project(":core:config"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/config/AppRemoteKeys.kt` | Thêm `":core:analytics" to AppsFlyerSettings.all()` |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` | `everyNumberKey_endsWithUnit` bỏ qua `:core:ads` (ADR-0026, quyết định 9); thêm test đơn vị trong mô tả khóa số của `:core:ads`, khóa `:core:analytics` có trong bảng, khóa `is_show_<mã>` của mọi vị trí có dòng trong bảng với `false` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModule.kt` | Giá trị cục bộ theo mục 3.1 |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModuleTest.kt` | Viết lại theo khóa mới (mục 4) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | Chỉ KDoc dòng 93: `native_every_<mã vị trí>` thay `ads_inlist_every_count` |
| Ghi lại | `docs/remote-config-keys.md` | Sinh lại bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`, không sửa tay |
| Ghi lại | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `docs/ops/remote-config.md` | Bỏ ghi chú đợt 2 (dòng 5, 26, 58), mục 3 bước 2 nêu khóa suy ra (`placementEnabled`, `interval`, `nativeEvery`) và `AppsFlyerSettings` |
| Sửa | `docs/ops/ads-guide.md` | Mục 5 bước 2: dạng dòng khai báo và giới hạn của `release.sh` (mục 3.1, câu hỏi 1); bỏ ghi chú "tới khi T-019" (mục 5 bước 3, mục 6), bảng "Hiện có (đợt 2)" của mục 6 thay bằng câu dẫn về mục 10 của `ads-business-rules.md`; N của native trong danh sách là `native_every_<mã vị trí>` |
| Sửa | `docs/features/ads/tickets/T-019-khoa-remote-config-theo-adskit.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-019-khoa-remote-config-theo-adskit.md` | Nhật ký thực hiện |

Không sửa, và vì sao không ảnh hưởng (L-020, đã tìm `grep -rn` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/testStaging`):

- `scripts/release.sh`: đọc `AdPlacement.kt` (dòng 38 tới 46, 152 tới 156). Không đổi dạng dòng của vị trí không phải `gallery_*`; hai dòng `gallery_inlist_*` thêm tham số nhưng script vốn bỏ qua `gallery_*`. Đã chạy thử ở mục 3.1. AI không được sửa file này.
- `scripts/tests/run.sh` và `scripts/tests/fixtures/release/AdPlacement.kt`: test của `release.sh` dùng file mẫu riêng, không đọc `AdPlacement.kt` thật.
- `scripts/secret-scan.py`: mã thử mới thuộc publisher `3940256099942544`, nằm trong `ALLOWED_PUBLISHERS`.
- `app/build.gradle.kts` (dòng 98 tới 106): chỉ trỏ tới hai file bảng khóa; không đổi.
- `core/config/src/test/kotlin/com/vglstudio/template/core/config/RemoteKeyTest.kt`: dùng chuỗi `ads_enabled` làm tên ví dụ cho `RemoteKey`, không đọc `AdsSettings`.
- `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt`, `InListAdsStateTest.kt`, `InListAdPositionsTest.kt`, `NativeAdSlotTest.kt`, `CollapsibleNativeAdTest.kt`, `FreshAdCacheTest.kt`, `FullScreenAdGuardTest.kt`: dùng `AdPlacement.SAMPLE_NATIVE_SMALL`, `inListEvery(adsAllowed, everyCount)` hay `FullScreenAdGuard`, không đổi.
- `app/src/test/kotlin/com/vglstudio/template/LocalRemoteValuesSourceSetTest.kt`, `AdGallerySourceSetTest.kt`: quét chuỗi `LocalRemoteValues`, `AdGallery`; task không thêm hai chuỗi này ngoài `src/staging` (L-062).
- `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/*`, `feature/sample`: dùng hằng số `AdPlacement` không đổi tên.
- `MainActivity.kt`, `TemplateApplication.kt`, `app/src/production/.../ProductionAdsModule.kt`, `app/src/staging/.../StagingAdsModule.kt`: không đổi chữ ký mà chúng gọi (`onAppForeground`, `onAppBackground`, `onUserInteraction`, `AdUnitIds`).
- `core/ads/build.gradle.kts`: giữ `:core:data` cho T-024, T-033.
- `docs/features/ads/spec.md`, `docs/ops/ads-business-rules.md`, ADR: không sửa (spec `confirmed`, ADR `accepted`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; đổi khóa bật tắt của mọi vị trí, công tắc tổng, khóa khoảng cách; gỡ ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên (đổi tần suất, điểm chặn 1); thêm 5 định dạng vào danh sách đóng, chưa có vị trí nào dùng.
- Sự kiện phân tích: Không chạm. Khai báo khóa công tắc `is_log_ad_*`, `is_log_af_*` nhưng không ghi sự kiện nào (T-023, T-033).
- Dữ liệu trên máy: không thêm khóa; hai khóa DataStore của T-009 thôi được đọc và ghi, không chuyển đổi, không xóa.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Không gọi API Android mới; `RemoteSettings`, `Flow`, Hilt đã có; không sửa manifest.
- **Thư viện và SDK:** Không thêm. `:core:analytics` thêm phụ thuộc module `:core:config` của dự án (để có `RemoteKey`), không phải thư viện mới; `:core:config` chỉ phụ thuộc `:core:logging` và Firebase Remote Config nên không có vòng phụ thuộc. `:core:ads` dùng `kotlinx.coroutines.flow.combine`, `distinctUntilChanged` đã có trên classpath.

### 3.4. Phương án dự phòng

- Không có. Tiêu chí E là 1: khóa, `Flow`, Hilt `@Binds`, test phản chiếu và test đọc file nguồn đều đã có mẫu trong dự án (mục 5).

## 4. Kiểm thử

Unit test JVM. `settings = FakeRemoteSettings()` (sau bước 3 có `readNames`), `flags = DefaultAdsFlags(settings)`, Turbine cho flow. Vị trí giả là `AdPlacementDeclaration` dựng trong test: `rw = (code = "rw_get_item", REWARDED)`, `onboarding = (code = "native_onboarding", NATIVE_STEP)`, `sounds = (code = "inter_sounds", INTERSTITIAL, AdIntervalGroup.named("sounds", 30))`, `list = (code = "list_x", NATIVE_IN_COLLECTION, nativeEvery = 6)`, `export = (code = "rw_export", REWARDED)`.

Bước 3 (test viết trước) có khung rỗng để biên dịch: `AdsFlags.kt` với `DefaultAdsFlags` trả `flowOf` giá trị mặc định cố định và bỏ qua `RemoteSettings`; `AdIntervalGroup`, `AdPlacementDeclaration` không kiểm gì; `AdsSettings` có hằng số mới nhưng `all()` và `keysFor` còn trả danh sách cũ; `AppsFlyerSettings` có `all()` rỗng; `AdFormat` có thêm 5 giá trị và `code` rỗng. Phần test sau vào commit test của bước 3: `FakeRemoteSettings.readNames`; mọi test mới hay viết lại ở bảng dưới; thay đổi `RemoteConfigKeysTest` (L-025). Phần vào commit code của bước 4: sửa `FullScreenAdsTest` (chỉ biên dịch được khi constructor đổi), xóa các test của lớp bị gỡ cùng lúc xóa lớp, xóa test `nonPersonalizedAds_...` cùng lúc xóa hàm.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-7, tiêu chí 1 | Unit test, Turbine | `AdsFlagsTest.masterSwitchOff_turnsEveryPlacementOff`: mỗi `AdPlacement` có `is_show_<mã>` = true, flow `placementEnabled` phát `true`; đặt `is_show_ads` = false, phát `false`. Khung rỗng thất bại vì luôn phát `false` (mặc định), dòng `true` đầu tiên không có |
| KB-7, tiêu chí 1 (app open, resume, combo) | Unit test, Turbine | `AdsFlagsTest.masterSwitchOff_turnsAppOpenResumeAndComboOff`: đặt `show_open_ads`, `show_open_ads_first_open`, `show_resum_ads`, `is_show_native_after_inter` = true, `global` có bốn cờ đó `true`; đặt `is_show_ads` = false, bản phát kế tiếp có `adsEnabled`, bốn cờ đều `false`. Khung rỗng thất bại vì không đọc giá trị đã đặt |
| KB-7, tiêu chí 2 | Unit test, Turbine | `AdsFlagsTest.masterSwitchBackOn_restoresEnabledPlacements`: vị trí bật, `is_show_ads` false rồi true, phát `false` rồi `true`; vị trí tắt vẫn `false` và không phát thêm (`expectNoEvents`) |
| Vị trí mặc định tắt (ADR-0014) | Unit test | `AdsFlagsTest.placementOffByDefault_evenWithMasterSwitchOn`: chưa đặt gì, `false`. Đối chứng (L-055): cùng vị trí trên `FakeRemoteSettings` khác có `is_show_<mã>` = true ra `true` |
| KB-7 qua `AdsController` | Unit test phản chiếu | `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags`: không constructor nào của `AdsController` có tham số kiểu `RemoteSettings` hay kiểu có tên chứa `HistoryStore`; có tham số kiểu `AdsFlags`. Thất bại trước code vì constructor hiện nhận `RemoteSettings` |
| KB-10, tiêu chí 1 | Unit test có sẵn | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`: đỏ ở bước 3 sau khi `keysFor` đổi (bảng cũ), xanh khi sinh lại ở bước 4 |
| KB-10, tiêu chí 2 | Unit test | `RemoteConfigKeysTest.analyticsKeys_areListed`: `AppRemoteKeys.byModule[":core:analytics"] == AppsFlyerSettings.all()` và có 11 phần tử (thất bại trên khung: chưa có dòng của module, `all()` rỗng); `everyFeatureFlagAndPlacement_isListed` có sẵn giữ nguyên |
| KB-10, tiêu chí 2 (tên do ta đặt) | Unit test | `AdsSettingsTest.keysNamedByTemplate_areMarked`: khóa của `all()` có tên không thuộc danh sách chép tay của `DG:171-181` (11 tên, chép trong test, không dùng `ADSKIT_KEY_NAMES`) thì mô tả có `not an AdsKit key` hoặc `Derived key`; khóa có tên thuộc danh sách thì không có `not an AdsKit key`. `AppsFlyerSettingsTest.everyKey_isMarkedAsTemplateName` tương tự |
| KB-10, tiêu chí 2 (đơn vị) | Unit test | `RemoteConfigKeysTest.adsNumberKeys_stateUnitInDescription`: mọi khóa long của `:core:ads` có `seconds` hoặc `items` trong mô tả. `everyNumberKey_endsWithUnit` đổi thành chỉ xét module khác `:core:ads` (`network_lost_delay_seconds` vẫn được kiểm) |
| KB-10, tiêu chí 3 | Unit test | `AdsFlagsTest.openAdType_isReadFromStringKey`: `open_ad_type` = `inter` ra `INTERSTITIAL`, `aoa` ra `APP_OPEN`, `INTER`, `x`, rỗng ra `APP_OPEN`; `resumeAdType_onlyExactInterIsInterstitial` cùng cách với `resume_ad_type`; `readNames` có `open_ad_type`, `resume_ad_type`. Thất bại trên khung vì luôn `APP_OPEN`. Số thực: `DefaultRemoteSettingsTest` có sẵn (T-007) |
| KB-10, tiêu chí 4 | Unit test | `AdsSettingsTest.globalKeys_matchAdsKitTable`: 14 bộ (tên, kiểu, mặc định) chép tay từ mục 10.1 bằng đúng tập khóa của `all()` trừ khóa suy ra (`is_show_*` của vị trí, `native_every_*`, `ads_interval_*`). Thất bại trên khung vì `all()` còn khóa cũ |
| KB-10, tiêu chí 4 (`show_resum_ads`) | Unit test | `AdsSettingsTest.resumeKey_keepsAdsKitTypo`: `SHOW_RESUM_ADS.name == "show_resum_ads"` và không có khóa nào tên `show_resume_ads` trong `all()` |
| KB-10, tiêu chí 4 (khóa không khai báo) | Unit test | `AdsSettingsTest.keysAdsKitDropped_areNotDeclared`: `time_reload_collap_ad`, `resume_ads_interval`, `time_reload_bottom_ad` không có trong `all()`. Đối chứng: test cùng lớp `globalKeys_matchAdsKitTable` thấy 14 khóa, nên `all()` không rỗng |
| Mặc định quan trọng (câu hỏi 21, 26, 27 của spec) | Unit test | `AdsSettingsTest.consentShortcut_isOffByDefault`, `nativeCollap_isOnByDefault`, `masterSwitch_isOnByDefault`; `AppsFlyerSettingsTest.everyKey_isOnByDefault` (11 tên chép tay, kiểu boolean, mặc định true; thất bại trên khung vì `all()` rỗng). Đối chứng cho ba test đầu (L-055): `AdsFlagsTest.global_followsEveryRemoteValue` |
| Mặc định qua bộ cờ | Unit test | `AdsFlagsTest.global_usesTableDefaultsWhenNothingSet`: ra đúng `AdsGlobalFlags(true, false, false, APP_OPEN, false, APP_OPEN, 45, false, false, false, 30, false, false, false)` (`showNativeCollap` false vì `is_show_native_bottom` mặc định tắt). Đối chứng (L-055): `AdsFlagsTest.global_followsEveryRemoteValue` đặt cả 14 khóa khác mặc định (`is_show_ads` giữ true) rồi kiểm từng trường đổi theo; khung rỗng qua test mặc định nhưng thất bại ở test đối chứng |
| Đọc lại sau khi đổi giữa chừng (L-061) | Unit test, Turbine | `AdsFlagsTest.global_emitsAgainWhenAValueChanges`: đang thu `global`, đổi `ads_interval` 45 sang 60, bản phát kế tiếp có 60; đặt lại 60 không phát thêm (`distinctUntilChanged`) |
| Lớp thu gọn theo khe đáy (D-01) | Unit test | `AdsFlagsTest.nativeCollap_requiresNativeBottom`: bottom false, collap true ra `false`; bottom true, collap true ra `true`; bottom true, collap false ra `false` |
| Số âm | Unit test | `AdsFlagsTest.negativeSeconds_areReadAsZero`: `ads_interval` = -5, `time_expand_collap_ad` = -1 ra 0 và 0. Đối chứng: trước khi đặt, 45 và 30 |
| Chỉ đọc khóa đã khai báo, không đọc khóa cũ | Unit test | `AdsFlagsTest.readsOnlyDeclaredKeys`: lấy `first()` của `global`, `placementEnabled` mọi vị trí, `intervalSeconds(DEFAULT)`, `nativeEvery` mọi vị trí; `readNames` là tập con của tên trong `AdsSettings.all()` và không chứa tên nào của danh sách khóa cũ (7 tên và `ads_<vị trí>_enabled` của 8 vị trí, chép tay). Đối chứng: `readNames` không rỗng. Khung rỗng thất bại ở đối chứng vì không đọc gì |
| Khóa cũ đã gỡ (câu hỏi 10, 19, 20 của spec) | Unit test | `AdsSettingsTest.wave2Keys_areGone`: không tên nào của `all()` thuộc 7 tên cũ hay khớp `^ads_.+_enabled$`. Thất bại trên khung vì `all()` còn khóa cũ |
| Đường code T-009 đã gỡ | Unit test phản chiếu | `RemovedPacingRulesTest.t009PacingClasses_areGone`: `Class.forName` ném `ClassNotFoundException` cho `FullScreenAdPacer`, `FullScreenAdSession`, `FullScreenAdHistoryStore`, `DataStoreFullScreenAdHistoryStore`, `FullScreenAdLimits`, `FullScreenAdPolicyKt`, `AdsRemoteRulesKt`, `AdClock` (gói `com.vglstudio.template.core.ads`). Đối chứng: `Class.forName` của `FullScreenAdGuard` cùng gói thành công. Thất bại trước code vì các lớp còn đó. Dùng tên lớp, không quét chuỗi trong mã nguồn (L-062) |
| KB-25, tiêu chí 1 | Unit test | `AdsSettingsTest.fakePlacements_deriveEveryKeyWithoutCoreChanges`: `keysFor(listOf(rw, onboarding, sounds, list, export))` có `is_show_rw_get_item`, `is_show_native_onboarding`, `is_show_inter_sounds`, `is_show_list_x`, `is_show_rw_export` (boolean, false), `ads_interval_sounds` (long, 30), `native_every_list_x` (long, 6). `AdsFlagsTest.fakePlacement_followsDerivedKeys`: `placementEnabled(rw)` false, đặt `is_show_rw_get_item` true ra true; `nativeEvery(list)` 6, đặt `native_every_list_x` = 3 ra 3. Thất bại trên khung vì `keysFor` trả danh sách cũ, flags trả mặc định |
| KB-25, tiêu chí 1 (dòng `release.sh` đọc được) | Unit test (test bảo vệ) | `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement`: tập mã đọc từ `src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` bằng biểu thức của `release.sh` dòng 42, bỏ `gallery_*`, bằng `AdPlacement.entries.filterNot { it.stagingOnly }.map { it.key }`; đối chứng: tập không rỗng, và biểu thức không đọc dòng mẫu có `intervalGroup = ...` (ghi rõ giới hạn). Qua cả trước và sau code: đây là test bảo vệ, không thuộc nhóm phải đỏ ở bước 3 |
| Khai báo sai bị chặn | Unit test | `AdPlacementTest.intervalGroupOnNonInterstitial_isRejected`, `nativeEveryOnNonListFormat_isRejected`, `nativeEveryBelowOne_isRejected`: dựng `AdPlacementDeclaration` sai, `assertThrows(IllegalArgumentException)`. Đối chứng: khai báo đúng tương ứng (`sounds`, `list`) không ném. Thất bại trên khung vì không kiểm |
| Vị trí hiện có | Unit test | `AdPlacementTest.inListGalleryPlacements_declareNativeEvery8`: `GALLERY_INLIST_SMALL`, `GALLERY_INLIST_MEDIUM` có `nativeEvery == 8`, vị trí khác `null`; `everyPlacement_isInDefaultGroup` |
| KB-25, tiêu chí 2 | Unit test | `AdsSettingsTest.placementsAreOffByDefault`: mọi `AdPlacement` có `is_show_<mã>`, boolean, false trong `all()`. `AdsSettingsTest.all_isDerivedFromDeclaredPlacements`: `all() == keysFor(AdPlacement.entries.map { it.declaration })`. `RemoteConfigKeysTest.everyPlacementShowKey_isInTableOff`: `keysMarkdown(AppRemoteKeys.byModule)` có đúng một dòng bắt đầu bằng `` | `is_show_<mã>` | boolean | `false` | `:core:ads` | `` cho mỗi vị trí. Thất bại trên khung vì bảng còn `ads_<mã>_enabled` |
| KB-25, tiêu chí 3 | Unit test, Turbine | `AdsFlagsTest.customGroup_readsItsOwnKey`: `intervalSeconds(sounds.intervalGroup)` ra 30; đặt `ads_interval_sounds` = 90 thì nhóm riêng ra 90, `intervalSeconds(DEFAULT)` vẫn 45 và không phát thêm; đặt `ads_interval` = 60 thì nhóm riêng vẫn 90. `AdsSettingsTest.sharedGroup_isOneKey`: hai vị trí giả cùng nhóm `sounds` 30 ra một khóa `ads_interval_sounds`; `conflictingGroupDefaults_areRejected`: cùng tên, mặc định 30 và 40, `keysFor` ném `IllegalArgumentException`. Phần mốc riêng: test của T-022 |
| KB-25, tiêu chí 4 | Unit test | `AdsSettingsTest.adsKitCodes_giveAdsKitKeyStrings`: `is_show_rw_get_item`, `is_show_native_onboarding`, `ads_interval_sounds` đúng từng ký tự, và hai khóa đầu không có ghi chú `not an AdsKit key` |
| KB-25, tiêu chí 5 | Unit test có sẵn, ticket khác | `AdUnitIdsTest.everyPlacementKey_isUniqueAndUsableInRemoteConfig` giữ mã duy nhất, chữ thường; ghi `ad_placement` có test ở T-023 |
| Danh sách định dạng MC-02 | Unit test | `AdFormatTest.codes_areTheClosedListOfMc02`: tập `code` bằng đúng 11 chuỗi của MC-02, không trùng. Thất bại trên khung vì `code` rỗng. `AdFormatTest.fullScreenFormats_areInterstitialAndNativeFullScreen` có sẵn giữ nguyên |
| Mã thử cho định dạng mới | Unit test | `AdUnitIdsTest.googleTestUnits_newFormats`: `APP_OPEN` ra `.../9257395921`, `REWARDED` ra `.../5224354917`, `BANNER_COLLAPSIBLE` ra `.../9214589741`, `NATIVE_STEP`, `NATIVE_IN_COLLECTION` ra `.../1044960115`. `googleTestUnit_existsForEveryFormat` có sẵn phủ cả 11 |
| Staging bật đủ khóa | Unit test | `StagingRemoteValuesModuleTest.everyAdPlacement_isShown`: tập tên có giá trị true chứa `AdsSettings.placementEnabled(it).name` của mọi vị trí; `globalShowKeys_areOn`: 7 khóa ở mục 3.1 có giá trị true; `consentShortcutAndMasterSwitch_areNotSet`: không có `is_enable_consent_shortcut`, `is_show_ads`; `everyLocalValue_isADeclaredKey`: mọi tên thuộc `AppRemoteKeys.byModule`; tên không trùng. Thất bại trên khung vì module còn khóa cũ |
| App vẫn khởi động, Hilt đủ đồ thị | Build và test có sẵn | `scripts/check.sh` biên dịch Hilt (thiếu `AdsFlags` thì lỗi biên dịch); `AppLaunchTest` trên emulator của CI |
| Quảng cáo trên staging sau khi đổi khóa | Kiểm tra thủ công | Cài bản staging mới, mở màn xem thử: native small, medium, thu gọn, trong danh sách (quảng cáo sau mỗi 8 mục), interstitial, native toàn màn hình vẫn hiện bằng mã thử; mở màn mẫu thấy native ở đáy. Gọi interstitial hai lần liền: lần hai bị chặn (khoảng cách 45 giây trong process); mở lại app rồi gọi ngay: hiện được (không còn khoảng cách lưu qua lần mở, không còn ân hạn). `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt` (đổi constructor, `awaitFullScreenTurn`, `adRequest`, hằng số thời gian chờ), `di/AdsFlagsModule.kt`, `core/analytics/build.gradle.kts`, KDoc của `InListAds.kt` và `AdGalleryScreen.kt`, hai file bảng khóa sinh lại, `remote-config.md`, `ads-guide.md`. `AdsController` gắn với SDK và không có unit test riêng (như T-009); phần quyết định của nó nằm trong `AdsFlags` và `FullScreenAdGuard` đã có test, còn constructor được kiểm bằng `RemovedPacingRulesTest` và `FullScreenAdsTest`. Hai file bảng khóa sinh sau code theo đúng quy trình T-007. Thay đổi `everyNumberKey_endsWithUnit` là đổi quy tắc theo ADR-0026 (quyết định 9), không phải nới test để qua; người duyệt xác nhận (câu hỏi 6). Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

Không cần (E dưới 2). Đã kiểm trước một điểm đo được bằng lệnh có sẵn, không cần test tạm: `scripts/release.sh --list-placements` trên `AdPlacement.kt` hiện tại và trên dòng mẫu có tham số (kết quả ở mục 3.1).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi khóa bật tắt của mọi vị trí, gỡ ân hạn, giới hạn mỗi phiên và khoảng cách qua phiên, thêm định dạng vào danh sách; điểm 1), module core dùng chung `:core:ads`, `:core:analytics` và file build `core/analytics/build.gradle.kts` (điểm 7). Điểm 9 không chạm: không chuyển đổi dữ liệu cũ, hai khóa DataStore chỉ thôi được dùng. Điểm 5 không chạm: không đổi luồng đồng ý hay thứ tự khởi tạo SDK; `is_enable_consent_shortcut` chỉ được khai báo. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đổi tần suất và tên khóa mà người vận hành dùng.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:core:analytics`, `:app`), đổi API công khai `AdFormat`, `AdPlacement`, `AdsSettings`, constructor `AdsController`, thêm `AdsFlags` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, câu hỏi liên quan đã trả lời; AI còn tự quyết vài chi tiết: giá trị cục bộ của staging, ghi chú "tên do ta đặt" trong mô tả, mã vị trí mẫu khác ví dụ của spec (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi khóa, mặc định, khóa suy ra, đường đọc có unit test; phần nối trong `AdsController` và hiển thị trên máy kiểm bằng mắt dễ dàng ở màn xem thử |
| D. Ảnh hưởng chéo | 3 | Đổi tên khóa mà người vận hành và mọi app đang dựa vào; T-020 tới T-036 dựa vào API mới; đổi hành vi tần suất mà quảng cáo toàn màn hình đang dựa vào |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: `RemoteKey` (T-007), `combine` của `placementActive` (T-009), test phản chiếu của `AdsSettingsTest`, test đọc file nguồn như `AdGallerySourceSetTest` |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt`, `AdsSettings.kt`, `AdsController.kt` (constructor), `AppRemoteKeys.kt`, `StagingRemoteValuesModule.kt`, hai file bảng khóa, `core/analytics/build.gradle.kts`. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng: không đổi giao diện; test không có `AdsController` nên không có quảng cáo.
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- Task khác có thể xung đột: chưa có plan nào của đợt 3 được duyệt. T-020, T-021, T-022, T-023 phụ thuộc T-019 và sẽ sửa `AdsController.kt`, `AdsSettings.kt`, `AdPlacement.kt`; plan của chúng viết sau khi T-019 merge, dùng các tên ở mục 3.1. T-022 thay `FullScreenAdGuard` và dùng `AdsFlags.intervalSeconds`, `AdPlacement.intervalGroup`; T-031 thêm `rw_no_ad_policy_<mã>` qua `placementParamKey`; T-033 đọc `AppsFlyerSettings`.
- Đề xuất thứ tự chạy: T-019 chạy trước mọi task của đợt 3 và không chung phiên với task khác sửa `AdsController.kt` hay `AdPlacement.kt`. Không phụ thuộc task nào chưa merge (T-007, T-008, T-009 đã merge).

## 7. Điều kiện dừng riêng

- Cần sửa `scripts/release.sh`, hay `AdPlacementSourceTest` đỏ với dòng của vị trí không phải `gallery_*`: dừng, không đổi biểu thức trong test cho khớp.
- Có module hay file ngoài mục 3.2 (feature, `MainActivity`, `:core:config`) đọc khóa cũ hay gọi hàm bị gỡ mà grep ở mục 3.2 không thấy.
- Hilt không dựng được đồ thị (thiếu `AdsFlags`, `RemoteSettings`) mà không sửa `:core:config`, `build-logic/` hay module của `:app` ngoài danh sách.
- `checkModuleDependencies` hay biên dịch từ chối `:core:analytics` phụ thuộc `:core:config`: dừng, không chuyển khóa `is_log_af_*` sang chỗ khác mà chưa hỏi (câu hỏi 5).
- Bảng khóa sinh lại có khóa ngoài danh sách ở mục 3.1 (14 khóa chung, `is_show_<mã>` của 8 vị trí, 2 khóa `native_every_gallery_inlist_*`, 11 khóa `is_log_af_*`, khóa của `:core:config`, `:core:network`) hay thiếu khóa trong đó.
- Cần đổi chữ ký mà `MainActivity`, `AdGalleryActivity`, `feature/sample` gọi.
- Cần thêm code đọc dấu "đã mở app lần đầu" hay lưu dữ liệu mới trên máy (thuộc T-024).

## 8. Câu hỏi cho người duyệt

1. **`scripts/release.sh` không đọc được dòng vị trí có thuộc tính.** Đã chạy thử (mục 3.1): dòng `X_Y("x_y", AdFormat.INTERSTITIAL, intervalGroup = ...)` bị bỏ qua, nên bản phát hành không đòi mã cho vị trí đó. `ads-guide.md` mục 5 bước 2 đang viết dạng `TEN_VI_TRI("ten_vi_tri", AdFormat.X, ...)`, mâu thuẫn với script.
   - Đề xuất của AI: T-019 giữ dạng một dòng không thuộc tính cho mọi vị trí không phải `gallery_*` và thêm `AdPlacementSourceTest` để lỗi lộ ra ở CI. Người sửa biểu thức dòng 42 của `release.sh` thành `...AdFormat\.[A-Z_]+(,.*)?\),?$` (đọc cả dòng có thuộc tính sau định dạng) cùng lúc với việc đổi mã theo khe (câu hỏi 14 của spec), trước khi app nào khai báo vị trí có nhóm riêng hay tham số. `ads-guide.md` mục 5 bước 2 ghi rõ giới hạn này cho tới lúc đó.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
2. **Mã vị trí của màn mẫu khác ví dụ trong tài liệu.** Spec mục 5 và `ads-business-rules.md` mục 10.2 dùng `is_show_sample_bottom_banner`, còn code (T-010, theo câu hỏi 3 của spec) có vị trí `sample_native_small`, nên khóa mới là `is_show_sample_native_small`.
   - Đề xuất của AI: giữ mã `sample_native_small` trong T-019 (đổi mã vị trí cũng đổi tên trong `release.properties`); T-030 (thanh đáy) quyết mã của vị trí thanh đáy ở màn mẫu và sửa ví dụ của spec khi đó.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
3. **Giá trị cục bộ của staging.** Ticket ghi "bật đủ khóa mới".
   - Đề xuất của AI: bật `is_show_<mã>` mọi vị trí và `show_open_ads`, `show_open_ads_first_open`, `show_resum_ads`, `is_show_native_after_inter`, `is_show_native_bottom`, `is_log_ad_fullscreen_events`, `is_log_ad_slot_events`; không đặt `is_enable_consent_shortcut` (giữ tắt, nhánh rút gọn là quyết định về đồng ý), loại quảng cáo (`aoa`) và khoảng cách (45 giây) để staging giống production. Khi T-024, T-025 merge, staging sẽ có app open lúc mở và khi quay lại; người thử tắt từng khóa trên Firebase staging nếu cần.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
4. **Gỡ khóa cá nhân hóa và khóa thời gian chờ native trong T-019?** Câu hỏi 8 và 20 của spec đã trả lời "bỏ" nhưng không ghi ticket nào làm.
   - Đề xuất của AI: gỡ trong T-019 để bảng khóa khớp mục 10. Gỡ khóa cá nhân hóa không đổi hành vi với giá trị mặc định (mặc định bật thì app vốn không thêm `npa`); thời gian chờ thành hằng số 10 giây, giữ hành vi tới T-027. Nếu muốn T-020 hay T-027 làm thì bỏ hai việc này khỏi mục 3.1 và giữ hai khóa cũ tới lúc đó.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
5. **Chỗ khai báo `is_log_af_*`.** Mục 10.1 ghi chủ là `analytics`.
   - Đề xuất của AI: `AppsFlyerSettings` trong `:core:analytics`, thêm phụ thuộc `:core:config` (như `:core:network`). Phương án khác: để trong `AdsSettings`, không sửa file build; nhưng T-023 dự kiến cho `:core:ads` phụ thuộc `:core:analytics`, khi đó `:core:analytics` không đọc ngược được `AdsSettings` và T-033 phải chuyển khóa.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
6. **Quy tắc đơn vị ở cuối tên khóa số.** `RemoteConfigKeysTest.everyNumberKey_endsWithUnit` (T-007) đòi `_seconds`, `_count`, `_percent`; ADR-0026 quyết định 9 đổi cho khóa quảng cáo (`ads_interval`, `time_expand_collap_ad`, `native_every_<mã>`).
   - Đề xuất của AI: test chỉ xét module khác `:core:ads`, thêm test đòi đơn vị trong mô tả khóa số của `:core:ads`. Đây là sửa một test có sẵn theo ADR đã duyệt; xin người duyệt xác nhận.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
7. **Hai khóa DataStore của T-009 trên máy người dùng.**
   - Đề xuất của AI: để nguyên (hai số long, không ai đọc). Xóa chúng cần một bước dọn khi khởi động, thêm code và test chỉ để tiết kiệm vài byte; app chưa phát hành bản có hai khóa này thì càng không cần.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
8. **Ranh giới với T-022.** Sau T-019, khoảng cách vẫn do `FullScreenAdGuard`: mốc ghi lúc hiện (không phải lúc inter rời màn), tính cả native toàn màn hình, chờ ngừng chạm 1 giây (spec câu hỏi 9 đã bỏ) vẫn còn; nhóm giãn cách có khóa nhưng chưa có mốc riêng.
   - Đề xuất của AI: để nguyên ba điểm này cho T-022 (ticket T-022 đã có "Thay bộ tần suất của T-009 bằng mốc `ads_interval` theo I-01 tới I-05" và nhóm giãn cách), để T-019 chỉ đổi khóa và gỡ phần T-009 mà PARTNER không có. Nếu người duyệt muốn bỏ chờ ngừng chạm ngay ở T-019 thì thêm `FullScreenAdGuard.kt`, `FullScreenAdGuardTest.kt`, `MainActivity.kt` vào mục 3.2.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
9. **Ghi chú "tên do ta đặt" ở đâu trong bảng khóa.**
   - Đề xuất của AI: câu cuối của mô tả khóa (tiếng Anh, `Name set by the template, not an AdsKit key.`), nên người vận hành thấy cả trên Firebase. Phương án khác: thêm cột "Nguồn tên" vào bảng, cần thêm trường vào `RemoteKey` của `:core:config` (thêm một module core vào task).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
10. **Thêm 5 định dạng chưa có code hiện.** Ticket đòi `AdPlacement` mang định dạng theo danh sách đóng MC-02.
    - Đề xuất của AI: thêm ngay để app khai báo vị trí theo đúng danh sách; dùng vị trí định dạng mới với composable hiện có thì ném lỗi như hôm nay (`require` của `AdSlot`, `NativeAdSlot`, `rememberShowFullScreenAd`). `NATIVE_COLLAPSIBLE` (medium thu gọn thành small của T-012) mang mã `native_collap` tới khi T-030 làm lại theo D-01.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).
11. **Tên thuộc tính của mã vị trí.** MC-01 gọi là "mã"; code đang là `AdPlacement.key`.
    - Đề xuất của AI: giữ `key` trong T-019 để không chạm khoảng 10 file khác; KDoc ghi rõ đây là mã vị trí và giá trị `ad_placement`.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-24, sau khi anh trả lời các câu ở mục 8 và sau rà soát độc lập trước phiên của RUN-20261024-02 (các điểm nên sửa đã sửa trong plan trước khi duyệt).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261024-02, phiên bản plan 1

- **Kết quả:** đã merge
- **Nhánh, PR:** `claude/T-019-khoa-remote-config-theo-adskit`, PR #123, merge commit `2000bf1` lúc 04:57 UTC. Bước 3 đến 5 do phiên con làm trong worktree riêng.
- **Đã làm:** như mục 3.1. `AdFormat` 11 định dạng của MC-02; `AdIntervalGroup`, `AdPlacementDeclaration` kiểm khai báo; `gallery_inlist_*` có `nativeEvery = 8`. `AdsSettings` 14 khóa của mục 10.1, khóa suy ra `is_show_<mã>`, `native_every_<mã>`, `ads_interval_<nhóm>` qua `placementParamKey`, `keysFor`, `ADSKIT_KEY_NAMES`. `AdsFlags`, `AdsGlobalFlags`, `OpenAdType`, `DefaultAdsFlags`, gắn bằng `di/AdsFlagsModule`. `AdsController` nhận `AdsFlags`; khoảng cách toàn màn hình theo `ads_interval`, chỉ trong process; không thêm `npa`; `NATIVE_LOAD_TIMEOUT_MILLIS = 10_000`; `inListEvery` trả `null` và ghi cảnh báo khi vị trí không có `nativeEvery`. Gỡ các lớp của T-009 và test của chúng. `AppsFlyerSettings` 11 khóa `is_log_af_*` (mặc định bật), `:core:analytics` phụ thuộc `:core:config`. Staging bật `is_show_<mã>` mọi vị trí và 7 công tắc chung. Sinh lại bảng khóa bằng `-PremoteConfigKeys.update=true`; sửa `remote-config.md`, `ads-guide.md`.
- **Test viết trước:** 50 test mới hoặc viết lại, cộng `everyNumberKey_endsWithUnit` đổi quy tắc theo câu hỏi 6; commit `aed5a75` trước code. 47 test thất bại đúng lý do trên khung rỗng, ví dụ: `wave2Keys_areGone` "ads_enabled is a wave 2 key"; `placementsAreOffByDefault` mong `is_show_sample_native_small`, nhận `ads_sample_native_small_enabled`; `AdsFlagsTest` mong INTERSTITIAL nhận APP_OPEN, mong 30 nhận 45; `RemovedPacingRulesTest` constructor còn nhận `RemoteSettings`; `AppsFlyerSettingsTest` mong 11 khóa, nhận 0; `StagingRemoteValuesModuleTest` "show_open_ads is not on". Ba test qua trước khi có code, đúng mục 4: `global_usesTableDefaultsWhenNothingSet` (đối chứng `global_followsEveryRemoteValue` thất bại) và hai test chặn của `AdPlacementSourceTest`. 14 test có bước đối chứng (L-055). Không test nào của bước 3 bị sửa ở bước 4; `FullScreenAdsTest` đổi constructor và bỏ `nonPersonalizedAds_*` trong commit code, đúng mục 4.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, các ghi chú. Nên sửa: `ads-guide.md` mục 8 còn tả `ads_inlist_every_count`, ân hạn phiên đầu và phiên 30 phút; đã sửa (commit `01e5cf3`). Ghi chú: chữ cũ ngoài mục 3.2 ở KDoc `FullScreenAds.kt:13`, `NativeAdSlot.kt:18` (`ads_native_load_timeout_seconds`) và `CLAUDE.md` dòng 62 ("tới T-019", người sửa); `DefaultAdsFlags` không `@Singleton`; mốc riêng theo nhóm giãn cách thuộc T-022 (câu hỏi 8). Sáu điểm lệch dưới đây được rà soát chấp nhận.
- **Kết quả CI:** `scripts/check.sh` cục bộ qua lần đầu (453 test JVM, 0 lỗi, 42 ảnh, không ảnh chuẩn nào đổi); `pr-check.yml` xanh lần đầu (40063763594).
- **Lệch so với plan:** (1) `StagingRemoteValuesModule` thêm hàm `globalAdSwitchesOn()` riêng, khung rỗng của nó vào commit test để test biên dịch được. (2) `core/analytics/build.gradle.kts` vào commit test vì khung `AppsFlyerSettings` cần `RemoteKey`. (3) `AdPlacementSourceTest`: kiểm "dòng có `intervalGroup` không đọc được" thành test riêng `releaseScript_skipsLinesWithAttributes` thay vì bước đối chứng trong test chính. (4) `ads-guide.md` sửa thêm mục 4 (khóa thời gian chờ native đã gỡ, nay "sau 10 giây") và đoạn sau bảng đợt 2 ở mục 6. (5) `AdIntervalGroup.named` từ chối thêm tên không phải snake case và mặc định âm. (6) KDoc cũ ở `FullScreenAds.kt:13` để nguyên vì ngoài mục 3.2.
- **Việc còn lại:** sửa KDoc `FullScreenAds.kt`, `NativeAdSlot.kt` ở task sau chạm hai file đó (T-022, T-021); người sửa `CLAUDE.md` dòng 62 và biểu thức dòng 42 của `scripts/release.sh` (câu hỏi 1).
- **Cần người xem:** tạo khóa `is_show_*` trên Firebase cho app đang bật vị trí bằng khóa cũ trước khi phát hành; kiểm thủ công ở mục 4 trên bản staging.
