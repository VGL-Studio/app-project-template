---
ticket: T-020
title: Đồng ý quảng cáo và khởi động SDK theo PARTNER
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, consent, core)
risk_level: high             # low | medium | high
token_estimate: 30M          # cỡ L, số gợi ý 30M (scripts/token-usage.py estimate, 2026-10-24)
blocking_points: [ads, consent, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-020: ĐỒNG Ý QUẢNG CÁO VÀ KHỞI ĐỘNG SDK THEO PARTNER

## 1. Tóm tắt

Tách cổng đồng ý của `:core:ads` thành một máy trạng thái thuần (`ConsentGate`) và một bộ điều phối (`ConsentCoordinator`) làm việc qua interface `ConsentPlatform` bọc UMP, theo luật S-01 tới S-19 của `docs/ops/ads-business-rules.md`: khởi tạo sớm bằng đồng ý cũ (`CACHED_EARLY`), quyền tạm khi UMP lỗi mạng (`ERROR_ALLOWED`) chỉ ở foreground, hỏi lại khi mạng về hay app lên foreground, đóng cổng ngay khi UMP báo cần hỏi lại hay người dùng rút đồng ý, nhánh rút gọn cần cả cấu hình lúc build và `is_enable_consent_shortcut`. `:core:ads` được khởi động trong `Application` (HD-01), theo dõi activity bằng callback vòng đời của `Application`, dùng theo dõi Internet chung của `:core:network`. Kiểm chứng bằng unit test cho từng luật với UMP giả, một test Robolectric cho bộ theo dõi activity, và kiểm thủ công trên staging (giả lập EEA).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23; câu hỏi 21 đã trả lời: nhánh rút gọn mặc định tắt):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-11 | Khi người dùng đã đồng ý ở lần mở trước thì quảng cáo bắt đầu tải ngay lúc mở app, không chờ hộp thoại đồng ý cập nhật xong. | S-03 `CACHED_EARLY`: ngay sau khi gửi lượt cập nhật, UMP đã có `canRequestAds() == true` thì mở cổng và khởi tạo SDK, không chờ callback. Theo S-12, chỉ khi nhánh rút gọn bật (cả cấu hình build và khóa từ xa). Nhánh tắt thì chờ lượt cập nhật xong (tiêu chí 6). |
| KB-11 | Khi người dùng ở ngoài vùng có luật thì không có hộp thoại, quảng cáo bắt đầu tải gần như ngay. | Cập nhật thành công, `consentStatus` là `NOT_REQUIRED`, `canRequestAds` true: đường `GRANTED`, chốt (S-06), khởi tạo SDK ngay trong callback. Không hiện form. |
| KB-11 | Khi việc hỏi đồng ý lỗi vì mạng và app đang mở trên màn hình thì quảng cáo vẫn được tải; khi mạng về hay app được mở lại thì app hỏi lại đồng ý. | S-04, S-09: lỗi mã `INTERNET_ERROR` (2) hay `TIME_OUT` (4) ở bước cập nhật hay bước form, nhánh rút gọn bật: đường `ERROR_ALLOWED`, cổng mở, không chốt. Mạng về (`InternetConnectivity` từ false sang true) hay app lên foreground: hỏi lại trên đúng activity đã hỏi, chỉ khi nó đang resume và không có quảng cáo toàn màn hình. |
| KB-11 | Khi app ở nền thì không có yêu cầu quảng cáo nào dựa trên quyền tạm khi lỗi mạng. | S-08: `mayRequestNow()` false khi đường là `ERROR_ALLOWED` và app ở nền (không activity nào ở `started`). Mọi lượt gửi của `:core:ads` hỏi `mayRequestNow()` ngay trước khi gửi (S-15). |
| KB-11 | Khi việc hỏi lại cho biết cần hỏi người dùng thì app dừng tải và không hiện quảng cáo đã tải bằng quyền tạm, cho tới khi người dùng trả lời. | S-10, S-11: đang có quyền tạm (`CACHED_EARLY` hay `ERROR_ALLOWED`) mà lượt cập nhật trả `canRequestAds` false: đóng cổng nạp và cổng hiện ngay, trước khi gọi form. `canRequestAds` (flow công khai) về false, các slot native và banner bỏ quảng cáo; quảng cáo toàn màn hình đã nạp không hiện. Cổng mở lại thì nạp sẵn lại một lần (lượt giữ lại của S-10; kế hoạch đầy đủ là S-23 của T-021). |
| KB-11 | Khi app chưa bật quyền tạm (mặc định tắt, cả cấu hình của app lẫn công tắc từ xa) thì quảng cáo chỉ tải khi hộp thoại đồng ý cho phép; lỗi mạng của việc hỏi đồng ý không cho tải. | S-12: `shortcutActive = enabledInBuild && is_enable_consent_shortcut`. Tắt thì không có `CACHED_EARLY`, không có `ERROR_ALLOWED`: cổng chỉ mở sau lượt cập nhật (và form nếu cần) với `canRequestAds` true. Đây là đổi hành vi so với code đợt 1 (hiện khởi tạo ngay khi `canRequestAds()` true sau lượt gửi). |
| KB-11 | Khi người vận hành tắt quyền tạm từ xa thì quyền tạm khi lỗi mạng bị thu hồi ngay; quyền người dùng đã cho vẫn giữ. | S-12: khóa từ xa đổi sang false giữa phiên: đường `ERROR_ALLOWED` mất quyền ngay (cổng đóng), không chốt; `CACHED_EARLY` và `GRANTED` giữ. Bật khóa không thu hồi gì. |
| KB-11 | Khi quyền tạm tắt mà hộp thoại đồng ý lỗi sau khi người dùng đã cho phép thì kết quả được giữ cho cả lần mở app, không hỏi lại. | S-06, nhánh tắt: form lỗi mà UMP đã `canRequestAds` true thì chốt; lượt hỏi sau trong process trả ngay, không gọi UMP. Trạng thái nhánh đọc lúc có kết quả. |
| KB-11, đặc biệt | Khi hộp thoại đồng ý đang mở thì không có quảng cáo khi quay lại app. | S-16: chỉ khi `consentStatus` là `REQUIRED` (form thật sự hiện), bộ điều phối giữ một suất của bộ đếm chặn `ForegroundAdSuppressor` trước khi gọi form, nhả đúng một lần khi form đóng, kể cả khi lỗi. Form quyền riêng tư luôn giữ. Quảng cáo khi quay lại app là T-025: nó đọc `suppressed` của bộ đếm; task này kiểm ở mức bộ đếm. |
| KB-11, đặc biệt | Khi hộp thoại bị ngắt (xoay máy, đóng màn) thì lần sau app hỏi lại, không coi là người dùng đã trả lời. | S-05: form lỗi khi activity đã hủy, đang đóng hay đổi cấu hình là `FORM_INTERRUPTED`: đường `INTERRUPTED_REGATHER`, không chốt, không cấp quyền tạm; lượt hỏi sau gọi UMP lại. |
| KB-12 | Khi người dùng rút đồng ý qua nút quyền riêng tư thì không còn yêu cầu quảng cáo mới nào và quảng cáo đã tải không hiện nữa. | S-14, S-15: form quyền riêng tư đóng, đọc lại `canRequestAds`: false thì cổng nạp và cổng hiện đóng (`DENIED_OR_REQUIRED`), `mayRequestNow()` false ở mọi lượt gửi, kể cả lượt nạp lại; `canRequestAds` công khai về false nên slot bỏ quảng cáo. Nút chỉ hiện khi `privacyOptionsRequirementStatus` là `REQUIRED` (có sẵn, giữ). |
| KB-12 | Khi người dùng đồng ý lại thì quảng cáo tải lại mà không cần mở lại app. | S-14: form đóng, `canRequestAds` true: SDK chưa khởi tạo thì khởi tạo; đã khởi tạo thì cổng mở lại và gọi nạp sẵn lại (`preloadAfterGateOpened`). Slot native thấy `canRequestAds` true và nạp lại. |

Luật trong phạm vi nhưng không có tiêu chí riêng trong spec, vẫn có test: S-01 (khởi động một lần), S-02, S-07 (gộp lượt hỏi), S-13 (đường đồng ý ghi log), S-17 (cài đặt gỡ lỗi dùng lại), S-18 (đặt lại đồng ý chỉ ở staging), S-19 (trước khi khởi động: mọi cổng đóng, tín hiệu đọc được), HD-01, HD-04.

Không thuộc task này: kế hoạch nạp sẵn, chờ SDK tối đa 35 giây, thử lại, nạp lại khi mạng về (S-20 tới S-25, T-021); app open ở màn loading và trần chờ đồng ý 10 giây (A-03, T-024); bộ chặn quảng cáo tự hiện cho feature và hợp đồng vỏ app (G-02 tới G-06, T-022); quảng cáo khi quay lại app (T-025).

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; nút quyền riêng tư giữ nguyên; docs/ops/design-workflow.md).

ADR liên quan: ADR-0001, ADR-0013 (phần còn hiệu lực: UMP chỉ trong `:core:ads`, hỏi mỗi lần mở app, `PrivacyOptionsButton`, staging giả lập EEA), ADR-0019, ADR-0024, ADR-0026 (quyết định 3), ADR-0027 (khởi động AppsFlyer cùng lúc với SDK quảng cáo, T-033).

Dựa trên T-019 (plan `docs/features/ads/plans/T-019-khoa-remote-config-theo-adskit.md`, chưa merge khi viết plan này; dùng đúng tên): `AdsFlags.global: Flow<AdsGlobalFlags>` với trường `consentShortcutEnabled` (khóa `is_enable_consent_shortcut`, mặc định false), constructor `AdsController` nhận `flags: AdsFlags` và không còn `RemoteSettings`, `historyStore`; `onAppForeground`, `onAppBackground` giữ chữ ký.

## 3. Giải pháp

### 3.1. Cách làm

Mọi luật thuần nằm trong hai lớp không gọi Android hay UMP, chạy trên JVM như `FullScreenAdGuard`, `NetworkDialogController`. `AdsController` chỉ nối chúng với UMP, SDK và vòng đời.

**1. Máy trạng thái thuần** (`ConsentGate.kt`):

```kotlin
/** How ads were allowed (or not) in this process (S-13). Public: the loading screen (T-024) and the gallery read it. */
enum class ConsentPath { CACHED_EARLY, GRANTED, DENIED_OR_REQUIRED, ERROR_ALLOWED, INTERRUPTED_REGATHER }

internal enum class UmpFailure { NETWORK, CONFIGURATION, INTERRUPTED }

/** S-04, S-05: INTERNET_ERROR (2), TIME_OUT (4) are NETWORK; a form error while the activity is gone is INTERRUPTED first. */
internal fun umpFailureOf(errorCode: Int, activityGone: Boolean): UmpFailure

/** S-12: both the build switch of :app and is_enable_consent_shortcut. */
internal fun shortcutActive(enabledInBuild: Boolean, remoteEnabled: Boolean): Boolean

internal data class ConsentGateState(
    val path: ConsentPath?,        // null before the first result (S-19)
    val allowed: Boolean,          // load and show gate (S-10, S-11, S-15)
    val temporary: Boolean,        // allowed only by CACHED_EARLY or ERROR_ALLOWED
    val settled: Boolean,          // S-06
)

internal class ConsentGate {
    val state: StateFlow<ConsentGateState>
    fun onShortcutChanged(active: Boolean)                              // S-12
    fun onRequestSent(umpCanRequestAds: Boolean)                        // S-03
    fun onUpdateSucceeded(umpCanRequestAds: Boolean)                    // S-10
    fun onUpdateFailed(failure: UmpFailure, umpCanRequestAds: Boolean)  // S-04, S-05, S-06
    fun onFormFinished(failure: UmpFailure?, umpCanRequestAds: Boolean) // S-05, S-06
    fun onPrivacyFormClosed(umpCanRequestAds: Boolean)                  // S-14
    fun onReset()                                                       // S-18
    /** S-08, S-15: may a request be sent right now. */
    fun mayRequestNow(foreground: Boolean): Boolean
    /** S-09: a regather is due (ERROR_ALLOWED and not settled). */
    val regatherDue: Boolean
}
```

Luật của máy, mỗi luật một nhóm test (mục 4):

- S-03: `onRequestSent(true)` khi nhánh bật và chưa có đường: `CACHED_EARLY`, `allowed`, `temporary`.
- S-04: `onUpdateFailed(NETWORK, _)` hay `onFormFinished(NETWORK, _)` khi nhánh bật: `ERROR_ALLOWED`, `allowed`, `temporary`, không chốt. Đã có `CACHED_EARLY` thì giữ `CACHED_EARLY` (quyền thật của phiên trước), không hạ xuống quyền tạm lỗi mạng.
- S-05: `CONFIGURATION`: không cấp quyền tạm; nếu đã có `CACHED_EARLY` thì giữ. `INTERRUPTED` (chỉ ở bước form): `INTERRUPTED_REGATHER`, không chốt, `allowed` theo `umpCanRequestAds` của UMP.
- S-06: kết quả sạch (cập nhật thành công và form đóng không lỗi) luôn chốt. Nhánh tắt: cập nhật lỗi không chốt; form lỗi chốt khi `umpCanRequestAds` true. Nhánh bật: chỉ chốt kết quả sạch. Trạng thái nhánh đọc lúc có kết quả (`onUpdateFailed`, `onFormFinished`), không lúc gửi.
- S-10: đang `temporary` mà `onUpdateSucceeded(false)`: `allowed` về false ngay (đường `DENIED_OR_REQUIRED`), trước bước form.
- S-12: `onShortcutChanged(false)` khi đường là `ERROR_ALLOWED` và chưa chốt: `allowed` false, đường giữ `ERROR_ALLOWED` cho log, `regatherDue` false; `CACHED_EARLY`, `GRANTED` không đổi. `onShortcutChanged(true)` không đổi `allowed`.
- S-08: `mayRequestNow(foreground)` = `allowed && (foreground || path != ERROR_ALLOWED)`.
- S-14: `onPrivacyFormClosed(false)`: `DENIED_OR_REQUIRED`, `allowed` false, chốt; `onPrivacyFormClosed(true)`: `GRANTED`, `allowed`, chốt.
- S-18: `onReset()`: về trạng thái đầu (`path` null, không chốt), giữ trạng thái nhánh.
- S-19: trạng thái đầu là `path = null`, `allowed = false`.

**2. Bộ điều phối** (`ConsentCoordinator.kt`), lớp nội bộ, chạy trên luồng chính, không gọi UMP trực tiếp:

```kotlin
internal interface ConsentPlatform {           // ConsentPlatform.kt, implemented by UmpConsentPlatform
    fun canRequestAds(): Boolean
    fun consentFormRequired(): Boolean           // consentStatus == REQUIRED (S-16)
    fun privacyOptionsRequired(): Boolean        // privacyOptionsRequirementStatus == REQUIRED (S-14)
    fun requestUpdate(activity: Activity, onSuccess: () -> Unit, onFailure: (errorCode: Int) -> Unit)
    fun showFormIfRequired(activity: Activity, onClosed: (errorCode: Int?) -> Unit)
    fun showPrivacyForm(activity: Activity, onClosed: (errorCode: Int?) -> Unit)
    fun reset()                                  // S-18
}

internal class ConsentCoordinator(
    platform: ConsentPlatform,
    gate: ConsentGate,
    suppressor: ForegroundAdSuppressor,
    isActivityGone: (Activity) -> Boolean,       // isFinishing || isDestroyed || isChangingConfigurations
    allowReset: Boolean,                         // ConsentDebugOptions.allowConsentReset
    logger: AppLogger,
    onGateOpened: (firstTime: Boolean) -> Unit,  // controller: init the SDK the first time, else preload again
)
```

- `start()`: đánh dấu đã khởi động; trả false ở lần gọi thứ hai (S-01). Trước `start()`, `gather` không gọi UMP, chỉ ghi log `warn` (S-19).
- `gather(activity)` (S-02): chưa biết trạng thái nhánh (chưa có giá trị đầu từ `AdsFlags.global`) thì giữ lượt hỏi, chạy ngay khi `onShortcutChanged` đầu tiên tới (HD-04). Đã chốt thì trả ngay, không gọi UMP (S-06). Đang có lượt chạy thì không gửi lượt thứ hai (S-07). Còn lại: `platform.requestUpdate`, rồi `gate.onRequestSent(platform.canRequestAds())` (S-03); thành công thì `gate.onUpdateSucceeded`, rồi nếu `consentFormRequired()` thì giữ một suất của bộ đếm chặn (S-16), gọi `showFormIfRequired`, nhả suất đúng một lần trong callback, rồi `gate.onFormFinished(umpFailureOf(code, isActivityGone(activity)), canRequestAds())`. Nhớ activity của lượt hỏi (WeakReference) cho S-09.
- `onForegroundChanged(foreground)`, `onInternetChanged(connected)`, `onPreloadRequested()` (T-021 gọi khi nạp sẵn lại): nếu `gate.regatherDue`, activity đã hỏi còn và đang resume (`isResumed`), không có quảng cáo toàn màn hình (`fullScreenShowing()`), thì `gather` lại trên activity đó (S-09). Chỉ cạnh false sang true của Internet và của foreground mới kích.
- `showPrivacyOptions(activity)` (S-14, S-16): giữ suất chặn, gọi `showPrivacyForm`, nhả trong callback, `gate.onPrivacyFormClosed(canRequestAds())`.
- `reset()` (S-18): `allowReset` false thì không làm gì, ghi log; true thì `platform.reset()`, `gate.onReset()`.
- Mỗi lần `gate.state.allowed` từ false sang true: `onGateOpened(firstTime)`. Lần đầu khởi tạo SDK; các lần sau nạp sẵn lại (S-10 giữ một lượt, S-14 cấp lại).
- Log (ADR-0019, thẻ `Ads`): mỗi đường đồng ý (`Consent path: CACHED_EARLY`), mỗi lượt hỏi và kết quả (mã lỗi số, không ghi thông báo của UMP), mỗi lần chốt, thu hồi, hỏi lại, đặt lại. Không ghi mã định danh máy hay chuỗi đồng ý.

`UmpConsentPlatform` (Android, trong `ConsentPlatform.kt`): bọc `UserMessagingPlatform.getConsentInformation`, `requestConsentInfoUpdate`, `loadAndShowConsentFormIfRequired`, `showPrivacyOptionsForm`, `ConsentInformation.reset()`. `ConsentRequestParameters` dựng một lần từ `ConsentDebugOptions` và dùng lại cho mọi lượt hỏi lại (S-17). Lớp này không có unit test (gọi thẳng UMP), như `SdkNativeAdLoader`.

**3. Bộ đếm chặn** (`ForegroundAdSuppressor.kt`, G-01): `acquire(source: String): Hold`, `Hold.release()` chỉ có tác dụng lần đầu; bộ đếm không xuống âm; `suppressed: StateFlow<Boolean>` là `count > 0`. Task này tạo bộ đếm và dùng cho S-16. T-022 thêm ba cách dùng cho feature, báo xuống theo G-02, hợp đồng vỏ app G-04 và dùng lại lớp này không sửa (mục 6).

**4. Theo dõi activity** (`AdsActivityTracker.kt`): cài `Application.ActivityLifecycleCallbacks`, đăng ký trong `AdsController.start(application)`. Đếm activity ở `started` bằng `StartedActivityCounter` có sẵn (foreground của S-08, S-09), giữ activity đang resume (WeakReference), báo `onActivityResumed`, `onActivityPaused`, `onActivityDestroyed` cho `AdsController`. Cả `AdGalleryActivity` của staging cũng được đếm mà không cần sửa. `onAppForeground`, `onAppBackground` mà `MainActivity` gọi giữ nguyên (T-019 giữ chúng cho log; T-025 quyết có bỏ không).

**5. Theo dõi Internet chung** (`:core:network`, S-09, S-24): thêm interface công khai `InternetConnectivity { val connected: Flow<Boolean> }` (giá trị hiện tại ngay, rồi mỗi lần đổi), cài bằng `DefaultInternetConnectivity` bọc `ConnectivitySource.connectivity()` có sẵn (đòi `VALIDATED`, ADR-0024), bind trong `NetworkBindingsModule`. Không viết bộ theo dõi thứ hai. `core/ads/build.gradle.kts` thêm `implementation(project(":core:network"))` (core phụ thuộc core, ADR-0001; `:core:network` không phụ thuộc `:core:ads` nên không có vòng). T-021 dùng lại interface này cho nạp lại khi mạng về.

**6. Nối vào `AdsController`:**

- Constructor thêm `connectivity: InternetConnectivity`, `shortcutConfig: ConsentShortcutConfig` (cùng `flags: AdsFlags` của T-019). `consentInformation`, `onConsentUpdated`, `onConsentFlowFinished`, `initialized` cũ bị thay bằng bộ điều phối.
- `fun start(application: Application)` (công khai, HD-01): gọi một lần từ `TemplateApplication.onCreate`; lần sau không làm gì (S-01). Đăng ký `AdsActivityTracker`; thu `flags.global.map { it.consentShortcutEnabled }` ghép với `shortcutConfig.enabledInBuild` vào `gate.onShortcutChanged`; thu `connectivity.connected` vào `coordinator.onInternetChanged`.
- `gatherConsent(activity)`, `showPrivacyOptionsForm(activity)`: chuyển cho bộ điều phối, giữ chữ ký (`MainActivity`, `AdGalleryActivity`, `PrivacyOptionsButton` không đổi).
- `canRequestAds: StateFlow<Boolean>` giữ tên và nghĩa với giao diện: `gate.allowed && sdkInitialized`. Về false ngay khi cổng đóng (S-10, S-14), nên `AdSlot`, `NativeAdSlot`, `CollapsibleNativeAdSlot`, `rememberInListAds` bỏ quảng cáo đang có mà không sửa các file đó (S-11, S-15).
- `internal fun mayRequestNow(): Boolean` = `canRequestAds.value && gate.mayRequestNow(tracker.foreground)`; gọi ngay trước mỗi lượt gửi trong `AdsController` (`preloadInterstitial` trước `InterstitialAd.load`, `preloadNativeFullScreen` trước `loadAd`, `nativeAd` và `loadNativeAd` trước `loader.load`); false thì kết thúc lượt, không gửi, ghi log, không hẹn gì (S-15).
- Hiện: `showInterstitial`, `showNativeFullScreen` (qua `awaitFullScreenTurn`) đòi `gate.allowed` (S-11); `isEnabled` không đổi.
- Thêm công khai `consentPath: StateFlow<ConsentPath?>`, `consentFlowFinished: StateFlow<Boolean>` (lượt hỏi của lần mở này đã có kết quả cuối hay đã chốt từ trước; T-024 chờ tín hiệu này tối đa 10 giây), `sdkReady: StateFlow<Boolean>` (callback của `MobileAds.initialize` đã về; T-021 thêm trần 35 giây). Trước `start()`: `canRequestAds`, `sdkReady`, `consentFlowFinished` false, `consentPath` null (S-19).
- `internal fun resetConsent()` (S-18) và composable công khai `rememberResetConsent(): () -> Unit` (`ConsentReset.kt`, cùng kiểu `rememberOpenAdInspector`): không có `LocalAdsController` hay activity thì không làm gì. Nút trên màn xem thử là T-036 (câu hỏi 4).
- `onGateOpened(firstTime)`: lần đầu `MobileAds.initialize` như hôm nay (luồng IO, log trạng thái adapter), `sdkReady` true trong callback; các lần sau gọi lại nạp sẵn các vị trí toàn màn hình đang bật (cùng hàm `preloadWhenEnabled` hiện có), cho tới khi T-021 thay bằng kế hoạch nạp sẵn.
- `onActivityResumed(activity)` từ bộ theo dõi: gọi `coordinator.onActivityResumed`, việc hỏi lại đồng ý (S-09). T-022 chèn việc gỡ cờ kẹt **trước** dòng này (G-06), nên dòng này là một lời gọi riêng, có chú thích.

**7. Việc của `:app`:**

- `TemplateApplication`: `@Inject lateinit var ads: AdsController`, `onCreate` gọi `ads.start(this)` sau `super.onCreate()` (HD-01). Trial ở mục 4.1 cho thấy tiêm vào `Application` của Hilt và đăng ký callback chạy được.
- Cấu hình lúc build của nhánh rút gọn: `data class ConsentShortcutConfig(val enabledInBuild: Boolean)` trong `:core:ads` (`OFF` là mặc định), bind theo flavor như `ConsentDebugOptions`: `ProductionAdsModule` trả `ConsentShortcutConfig.OFF`; `StagingAdsModule` trả `ConsentShortcutConfig(enabledInBuild = true)` để người thử bật `is_enable_consent_shortcut` trên Firebase staging mà không cần build khác (khóa từ xa vẫn mặc định tắt, nên staging mặc định vẫn đi theo UMP; câu hỏi 2).
- HD-04 (đưa công tắc vào trước bước hỏi đồng ý, đưa lại khi Remote Config đổi): giá trị từ xa do `:core:ads` tự đọc từ `AdsFlags` (khóa thuộc `:core:ads`, mục 10.1); bộ điều phối giữ lượt hỏi tới khi có giá trị đầu (giá trị đã lưu hay mặc định, phát ngay theo hợp đồng của `RemoteSettings`). `:app` chỉ cấp phần lúc build (câu hỏi 3).
- `ConsentDebugOptions` thêm `allowConsentReset: Boolean = false`; `EEA_TEST_DEVICE` có `true`, `NONE` có `false` (S-18: production không có đường đặt lại).

Thứ tự khởi tạo sau task: `Application.onCreate` (Hilt, `ads.start`: callback vòng đời, thu khóa nhánh, thu Internet) rồi `MainActivity.onCreate` (`gatherConsent`) rồi SDK (theo đường đồng ý). AppsFlyer (T-033) sẽ khởi động ở cùng chỗ `onGateOpened(firstTime = true)` (ADR-0027 mục 3); task này không đụng `:core:analytics`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentGate.kt` | `ConsentPath`, `UmpFailure`, `umpFailureOf`, `shortcutActive`, `ConsentGateState`, `ConsentGate` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentCoordinator.kt` | Bộ điều phối lượt hỏi, hỏi lại, form quyền riêng tư, đặt lại, log |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentPlatform.kt` | Interface `ConsentPlatform`, `UmpConsentPlatform` bọc UMP |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ForegroundAdSuppressor.kt` | Bộ đếm chặn quảng cáo tự hiện (G-01) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsActivityTracker.kt` | Callback vòng đời của `Application`: foreground, activity đang resume |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentShortcutConfig.kt` | Cấu hình lúc build của nhánh rút gọn |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentReset.kt` | `rememberResetConsent()` (S-18) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `start`, constructor, nối bộ điều phối, `canRequestAds`, `mayRequestNow` trước mỗi lượt gửi, cổng hiện, `consentPath`, `consentFlowFinished`, `sdkReady`, `resetConsent`, `onGateOpened`, `onActivityResumed` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentDebugOptions.kt` | `allowConsentReset` |
| Sửa | `core/ads/build.gradle.kts` | `implementation(project(":core:network"))` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/InternetConnectivity.kt` | Interface công khai và `DefaultInternetConnectivity` |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/template/core/network/di/NetworkModule.kt` | `@Binds` `InternetConnectivity` trong `NetworkBindingsModule` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/TemplateApplication.kt` | Tiêm `AdsController`, `ads.start(this)` (HD-01) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/di/StagingAdsModule.kt` | `ConsentShortcutConfig(enabledInBuild = true)` |
| Sửa | `app/src/production/kotlin/com/vglstudio/template/di/ProductionAdsModule.kt` | `ConsentShortcutConfig.OFF` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt` | Luật của máy trạng thái (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentCoordinatorTest.kt` | Lượt hỏi, gộp, hỏi lại, form, rút, đặt lại, S-19 (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeConsentPlatform.kt` | UMP giả: ghi lại lời gọi, trả lời theo kịch bản |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ForegroundAdSuppressorTest.kt` | G-01 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsActivityTrackerTest.kt` | Robolectric: foreground, resume, pause, hủy |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentDebugOptionsTest.kt` | Chỉ cấu hình staging cho đặt lại |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdsTest.kt` | Dựng `AdsController` với tham số mới (`connectivity` giả, `ConsentShortcutConfig.OFF`) |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/DefaultInternetConnectivityTest.kt` | Phát lại đúng `connectivity()` của nguồn |
| Sửa | `docs/features/ads/tickets/T-020-dong-y-va-khoi-dong-partner.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-020-dong-y-va-khoi-dong-partner.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã tìm `git grep` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/testStaging`, `*/src/androidTest`, mọi `build.gradle.kts`):

- `AdsController.kt`: `FullScreenAdsTest.kt` dựng thẳng `AdsController` (có trong danh sách). `NativeAdSlotTest`, `InListAdsTest` chỉ nhắc tên trong tên test, không dựng. `AdGalleryActivity.kt`, `MainActivity.kt` gọi `gatherConsent`, `showPrivacyOptionsForm` qua `PrivacyOptionsButton`, `onAppForeground`, `onAppBackground`: chữ ký giữ nguyên nên không sửa.
- `TemplateApplication.kt`: chỉ manifest nhắc. `app/src/test/resources/robolectric.properties` dùng `android.app.Application`, nên test Robolectric của `:app` không chạy `TemplateApplication`; `AppLaunchTest` (emulator) chạy nó và là chỗ lộ lỗi khởi tạo.
- `ConsentDebugOptions.kt`: hai module flavor của `:app` (có trong danh sách) và `FullScreenAdsTest` (dùng `NONE`, không đổi).
- `NetworkModule.kt`: không test nào nhắc.
- `core/ads/build.gradle.kts`: `build-logic` kiểm quy tắc phụ thuộc module (`checkModuleDependencies`) theo tên module, không đọc file này theo đường dẫn; core phụ thuộc core được phép.
- `scripts/release.sh`, `scripts/run-check.py`, `scripts/tests/run.sh` nhắc `MainActivity` hay `AdPlacement.kt`; task không sửa hai file đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay tần suất. Đổi thời điểm khởi tạo SDK và cổng đồng ý (điểm chặn 5). Nhánh rút gọn tắt ở production; ở staging cấu hình build bật, khóa từ xa vẫn tắt.
- Sự kiện phân tích: Không chạm.
- Dữ liệu: không thêm dữ liệu lưu trên máy; chuỗi đồng ý do UMP lưu như hôm nay. Đặt lại đồng ý chỉ ở staging.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `InternetConnectivity` dùng `ConnectivityManager` qua `:core:network`, đã có `ACCESS_NETWORK_STATE`; `Application.registerActivityLifecycleCallbacks` không cần quyền.
- **Thư viện và SDK:** Không thêm. UMP (`user-messaging-platform` 4.0.0, `libs.user.messaging.platform`) và `play-services-ads` đã có trong `:core:ads`; `ConsentInformation.ConsentStatus`, `FormError.ErrorCode`, `ConsentInformation.reset()` có trên classpath biên dịch (chạy thử ở mục 4.1). `:core:ads` thêm phụ thuộc module `:core:network` của dự án, không phải thư viện mới.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test Robolectric `AdsActivityTrackerTest` dùng `Robolectric.buildActivity(ComponentActivity::class.java)` và callback đăng ký trên `ApplicationProvider.getApplicationContext<Application>()`; chạy thử đã qua (mục 4.1), nhưng thứ tự callback khi `recreate()` có thể khác trên bản Robolectric sau này.
- **Điều kiện chuyển:** `AdsActivityTrackerTest` thất bại vì thứ tự hay số lần callback của Robolectric (không vì logic đếm) sau 2 lần sửa.
- **Cách làm thay:** tách phần quyết định của bộ theo dõi thành hàm thuần nhận chuỗi sự kiện (`created`, `started`, `resumed`, `paused`, `stopped`, `destroyed` với mã activity), test trên JVM không Robolectric; `AdsActivityTracker` chỉ chuyển callback sang hàm đó. Test viết trước giữ nguyên ý (foreground, resume, hủy).

## 4. Kiểm thử

Unit test JVM với `FakeConsentPlatform` (ghi lại lời gọi `requestUpdate`, `showFormIfRequired`, `showPrivacyForm`, `reset` và activity; callback chỉ chạy khi test gọi `completeUpdate()`, `failUpdate(code)`, `closeForm(code?)`; `canRequestAds`, `consentFormRequired`, `privacyOptionsRequired` do test đặt), `ConsentGate` thật, `ForegroundAdSuppressor` thật, `RecordingAppLogger`. Activity là `mockk<Activity>(relaxed = true)` hay `Robolectric.buildActivity` khi cần trạng thái thật; `isActivityGone` và `isResumed` là lambda do test đặt. Không có đồng hồ: các luật đồng ý không có khoảng chờ (trần 10 giây là T-024).

Bước 3 (test viết trước) có khung rỗng để biên dịch: `ConsentGate` có trạng thái đầu cố định và hàm sự kiện không làm gì; `ConsentCoordinator` có hàm rỗng; `ForegroundAdSuppressor.acquire` trả `Hold` không đếm; `AdsActivityTracker` có callback rỗng; `umpFailureOf` trả `CONFIGURATION`, `shortcutActive` trả false; `InternetConnectivity`, `DefaultInternetConnectivity` trả `flowOf(false)`; `ConsentShortcutConfig`, `allowConsentReset` có sẵn. Commit test của bước 3 gồm mọi file test mới ở mục 3.2 và `FakeConsentPlatform`; sửa `FullScreenAdsTest` vào commit code của bước 4 (chỉ biên dịch được khi constructor đổi) (L-025).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-11, tiêu chí 1 (S-03) | Unit test | `ConsentCoordinatorTest.cachedConsent_shortcutOn_opensBeforeUpdateReturns`: nhánh bật, `canRequestAds = true` lúc gửi; ngay sau `gather` (chưa `completeUpdate`): `state.path == CACHED_EARLY`, `allowed`, `onGateOpened(true)` gọi 1 lần. Khung rỗng thất bại vì `path` null. Đối chứng (L-055): `cachedConsent_shortcutOff_waitsForUpdate` cùng đầu vào, nhánh tắt: chưa mở trước `completeUpdate()`, mở sau đó với `GRANTED` |
| KB-11, tiêu chí 2 | Unit test | `ConsentCoordinatorTest.outsideRegulatedRegion_grantsWithoutForm`: `completeUpdate()`, `consentFormRequired = false`, `canRequestAds = true`: `GRANTED`, `settled`, `allowed`; `showFormIfRequired` vẫn được gọi một lần (UMP tự bỏ form) và bộ đếm chặn không tăng |
| KB-11, tiêu chí 3 (S-04) | Unit test | `ConsentGateTest.networkFailure_shortcutOn_allowsWithoutSettling`: `onUpdateFailed(NETWORK, false)` ra `ERROR_ALLOWED`, `allowed`, không `settled`, `regatherDue`; cùng với `onFormFinished(NETWORK, false)`. `ConsentGateTest.umpFailureOf_mapsCodes`: 2 và 4 ra `NETWORK`; 1, 3, 99 ra `CONFIGURATION`; mọi mã với `activityGone = true` ra `INTERRUPTED`. Khung rỗng thất bại vì luôn `CONFIGURATION` |
| KB-11, tiêu chí 3 (S-09, mạng về) | Unit test | `ConsentCoordinatorTest.errorAllowed_internetBack_regathersOnSameActivity`: sau `ERROR_ALLOWED` trên activity A đang resume: `onInternetChanged(false)` rồi `true` thì `requestUpdate` lần 2 trên A; `completeUpdate()` với `canRequestAds = true` ra `GRANTED`, `settled`. Đối chứng: `onInternetChanged(true)` khi đường là `GRANTED` không gọi lại |
| KB-11, tiêu chí 3 (S-09, mở lại app) | Unit test | `ConsentCoordinatorTest.errorAllowed_foreground_regathers`: `onForegroundChanged(false)` rồi `true` gọi lại trên A. `regather_skippedWhenActivityPausedOrOtherOrAdShowing`: A không resume, hay activity đã hỏi là A mà sự kiện tới khi chỉ B còn, hay `fullScreenShowing = true`: không gọi lại |
| KB-11, tiêu chí 4 (S-08) | Unit test | `ConsentGateTest.errorAllowed_background_mayNotRequest`: `ERROR_ALLOWED`: `mayRequestNow(foreground = false)` false, `true` thì true. Đối chứng: `GRANTED` và `CACHED_EARLY` ở nền vẫn true. Khung rỗng thất bại vì `allowed` luôn false nên vế foreground cũng false |
| KB-11, tiêu chí 4 (foreground thật) | Unit test Robolectric | `AdsActivityTrackerTest.foreground_followsStartedActivities`: `buildActivity(ComponentActivity).setup()`: `foreground` true; `stop()`: false; hai activity chuyển qua nhau (A start, B start, A stop): vẫn true. `resumedActivity` là activity vừa resume, `null` sau `pause()` |
| KB-11, tiêu chí 5 (S-10, S-11) | Unit test | `ConsentCoordinatorTest.temporaryThenRequired_closesBeforeForm`: `CACHED_EARLY` rồi `completeUpdate()` với `canRequestAds = false`, `consentFormRequired = true`: tại lúc `showFormIfRequired` được gọi (test đọc trạng thái trong lời gọi giả), `allowed` đã false; `path == DENIED_OR_REQUIRED`. Đóng form với `canRequestAds = true`: `allowed`, `onGateOpened(false)` gọi 1 lần (nạp sẵn lại) |
| KB-11, tiêu chí 6 (S-12, nhánh tắt) | Unit test | `ConsentGateTest.shortcutOff_networkFailureGivesNothing`: nhánh tắt, `onRequestSent(true)` không mở; `onUpdateFailed(NETWORK, _)` không mở, `path` không phải `ERROR_ALLOWED`. `ConsentGateTest.shortcutActive_needsBoth`: bảng 4 dòng, chỉ (true, true) ra true |
| KB-11, tiêu chí 7 (S-12, thu hồi) | Unit test | `ConsentGateTest.shortcutTurnedOff_revokesErrorAllowedOnly`: `ERROR_ALLOWED` rồi `onShortcutChanged(false)`: `allowed` false, không `settled`, `regatherDue` false. `CACHED_EARLY` rồi tắt: `allowed` vẫn true. `GRANTED` rồi tắt: vẫn true. Tắt rồi bật lại: không đổi gì (`turningOn_revokesNothing`) |
| KB-11, tiêu chí 8 (S-06, nhánh tắt) | Unit test | `ConsentGateTest.shortcutOff_formErrorAfterGrant_settles`: nhánh tắt, `onFormFinished(CONFIGURATION, true)`: `settled`, `allowed`. `ConsentCoordinatorTest.settled_nextGatherSkipsUmp`: sau khi chốt, `gather` lần 2 không gọi `requestUpdate` (đếm lời gọi bằng 1). Đối chứng: `shortcutOn_formError_doesNotSettle` cùng đầu vào, nhánh bật: không `settled`, `gather` lần 2 gọi UMP. `settleRule_readsShortcutAtResultTime`: gửi khi bật, tắt trước khi form trả lỗi: chốt theo luật nhánh tắt |
| KB-11, đặc biệt 1 (S-16) | Unit test | `ConsentCoordinatorTest.requiredForm_holdsSuppressionWhileOpen`: `consentFormRequired = true`: trong lời gọi `showFormIfRequired`, `suppressor.suppressed` true; `closeForm(null)` rồi false; `closeForm(1)` (đóng có lỗi) cũng nhả; gọi callback hai lần thì bộ đếm không xuống âm. Đối chứng: `consentFormRequired = false` thì `suppressed` không lên lúc gọi form. `privacyForm_alwaysHoldsSuppression` tương tự cho form quyền riêng tư |
| KB-11, đặc biệt 2 (S-05) | Unit test | `ConsentCoordinatorTest.interruptedForm_regathersNextTime`: `closeForm(1)` với `isActivityGone = true`: `INTERRUPTED_REGATHER`, không `settled`; `gather` lần sau gọi `requestUpdate` lần 2. `ConsentGateTest.configurationError_givesNoTemporaryRight`: nhánh bật, `onUpdateFailed(CONFIGURATION, false)`: `allowed` false |
| KB-12, tiêu chí 1 (S-14, S-15) | Unit test | `ConsentCoordinatorTest.privacyFormWithdrawn_closesEveryGate`: `GRANTED`, `showPrivacyOptions`, `closeForm(null)` với `canRequestAds = false`: `allowed` false, `path == DENIED_OR_REQUIRED`, `mayRequestNow(true)` false. Khung rỗng thất bại ở đối chứng: trước khi rút, `mayRequestNow(true)` true |
| KB-12, tiêu chí 2 (S-14) | Unit test | `ConsentCoordinatorTest.privacyFormRegranted_initsOrPreloads`: rút rồi cấp lại khi SDK đã khởi tạo: `onGateOpened(firstTime = false)` 1 lần; trường hợp chưa từng mở (đường `DENIED_OR_REQUIRED` từ đầu) rồi cấp: `onGateOpened(true)` 1 lần |
| S-01, HD-01 | Unit test | `ConsentCoordinatorTest.start_twice_isIgnored`: `start()` lần 1 true, lần 2 false. Lời gọi `ads.start(this)` trong `TemplateApplication`: ngoại lệ viết test trước (dưới) |
| S-02 | Unit test | `ConsentCoordinatorTest.gather_updatesThenShowsFormThenReads`: thứ tự lời gọi của `FakeConsentPlatform` là `requestUpdate`, `showFormIfRequired`, rồi `canRequestAds` được đọc sau `closeForm` |
| S-07 | Unit test | `ConsentCoordinatorTest.secondGatherWhileRunning_sharesTheFirst`: `gather(A)` rồi `gather(A)` trước `completeUpdate`: `requestUpdate` 1 lần, `showFormIfRequired` 1 lần sau khi xong |
| S-13 | Unit test | `ConsentCoordinatorTest.everyPath_isLogged`: mỗi đường trong 5 đường có đúng một dòng `info` thẻ `Ads` chứa tên đường; không dòng nào chứa thông báo lỗi của UMP (giả trả thông báo `secret-message`, kiểm không xuất hiện) |
| S-17 | Unit test | `ConsentCoordinatorTest.regather_reusesFirstParameters`: `FakeConsentPlatform` ghi số lần dựng tham số; hỏi lại không dựng lại. Phần `ConsentRequestParameters` của UMP trong `UmpConsentPlatform`: kiểm thủ công (form EEA ở staging) |
| S-18 | Unit test | `ConsentCoordinatorTest.reset_onlyWhenAllowed`: `allowReset = false`: `platform.reset()` 0 lần, trạng thái giữ; `true`: 1 lần, `path` null, lượt `gather` sau gọi UMP. `ConsentDebugOptionsTest.onlyStagingAllowsReset`: `EEA_TEST_DEVICE.allowConsentReset` true, `NONE` false |
| S-19 | Unit test | `ConsentCoordinatorTest.beforeStart_gatesClosedAndSignalsReadable`: chưa `start()`: `state.path` null, `allowed` false, `gather` không gọi `requestUpdate` và ghi một dòng `warn`; sau `start()` thì gọi. Đối chứng cho trạng thái đầu (L-055): cùng test, sau `start()` và `completeUpdate()` với `canRequestAds = true`, `allowed` true |
| HD-04 | Unit test | `ConsentCoordinatorTest.gatherWaitsForShortcutValue`: `start()`, `gather(A)` trước `onShortcutChanged`: không gọi UMP; `onShortcutChanged(true)` thì gọi, và lỗi mạng sau đó ra `ERROR_ALLOWED` (nhánh bật được dùng cho lượt đầu) |
| G-01 (dùng cho S-16) | Unit test | `ForegroundAdSuppressorTest.countsSources_neverNegative`: hai `Hold`; nhả một thì vẫn `suppressed`; nhả cả hai thì false; nhả lại một `Hold` không làm bộ đếm âm (thêm `Hold` mới thì `suppressed` lại true). Khung rỗng thất bại vì không đếm |
| Theo dõi Internet chung | Unit test | `DefaultInternetConnectivityTest.followsSourceConnectivity`: `FakeConnectivitySource` (có sẵn trong `core/network/src/test`) phát false, true: `connected` phát false, true. Khung rỗng thất bại vì luôn false |
| Activity của lượt hỏi bị hủy (S-09) | Unit test Robolectric | `AdsActivityTrackerTest.destroyedActivity_isReported`: `destroy()` báo `onActivityDestroyed` một lần với `isChangingConfigurations` đúng giá trị (`recreate()` ra true) |
| Đồ thị Hilt, app khởi động | Build và test có sẵn | `scripts/check.sh` biên dịch Hilt của `:app` (thiếu binding `InternetConnectivity` hay `ConsentShortcutConfig` thì lỗi biên dịch); `AppLaunchTest` trên emulator của CI chạy `TemplateApplication.onCreate` thật |
| Đồng ý trên máy (staging, EEA giả) | Kiểm tra thủ công | 1) Cài mới: form hiện; log `VGL.Ads` có `Consent path:` và không có `Adapter ...` trước khi trả lời; đồng ý thì có `GRANTED` và dòng adapter. 2) Mở lại: không có form, `GRANTED` (nhánh tắt). 3) Bật chế độ máy bay, xóa dữ liệu app, mở: không có quảng cáo, log lỗi mạng của lượt hỏi, không có `ERROR_ALLOWED`. 4) Trên Firebase staging bật `is_enable_consent_shortcut`, lặp bước 3: `ERROR_ALLOWED`, tắt chế độ máy bay: log hỏi lại. 5) Nút quyền riêng tư ở màn mẫu: rút đồng ý thì native ở đáy biến mất, log `DENIED_OR_REQUIRED`; đồng ý lại thì native hiện lại không cần mở lại app. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt` (`start`, constructor, kiểm `mayRequestNow` ở bốn chỗ gửi, cổng hiện, `MobileAds.initialize` trong `onGateOpened`), `UmpConsentPlatform` (gọi thẳng UMP), `ConsentReset.kt` (bọc `LocalAdsController`, như `rememberOpenAdInspector` của T-014), `TemplateApplication.kt` (một lời gọi), hai module flavor, `NetworkModule.kt`, `core/ads/build.gradle.kts`. `AdsController` gắn với SDK và không có unit test riêng (như T-009, T-019); mọi quyết định nằm trong `ConsentGate`, `ConsentCoordinator`, `ForegroundAdSuppressor`, `AdsActivityTracker` đã có test. `TemplateApplication` không chạy trong test Robolectric của `:app` (`robolectric.properties` dùng `android.app.Application`); nó chạy trong `AppLaunchTest` trên emulator. Task không đổi hàm `suspend` nào mà vỏ app gọi (`gatherConsent`, `start` không phải `suspend`), nên không cần test dựng vỏ app theo L-051. Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-24, trong worktree của người viết plan, trên nhánh chính commit `adf9450` (chưa có T-019), code thử không commit, đã gỡ hết bằng `git checkout --` và xóa file mới sau khi chạy (`git status` sạch, chỉ còn plan). Chung một lần chạy với plan T-022 và T-023. Code thử cho phần mới của task này:
  - `TemplateApplication`: `@Inject lateinit var ads: AdsController`, `onCreate` gọi `ads.start(this)`; `AdsController.start(application)` (chặn lần gọi hai bằng `AtomicBoolean`) đăng ký một `Application.ActivityLifecycleCallbacks` thử và thu `InternetConnectivity.connected`.
  - `:core:network`: interface công khai `InternetConnectivity`, `DefaultInternetConnectivity` bọc `ConnectivitySource.connectivity()`, `@Binds` trong `NetworkBindingsModule`; `core/ads/build.gradle.kts` thêm `implementation(project(":core:network"))` (và `:core:analytics` cho T-023); `AdsController` nhận `InternetConnectivity` qua Hilt.
  - Hàm thử đọc tên của UMP: `ConsentInformation.ConsentStatus.REQUIRED`, `OBTAINED`, `NOT_REQUIRED`, `UNKNOWN`, `FormError.ErrorCode.INTERNAL_ERROR`, `INTERNET_ERROR`, `INVALID_OPERATION`, `TIME_OUT`, `ConsentInformation.reset()`.
  - Test tạm `core/ads/src/test/.../TrialStartupTest.kt` (Robolectric, `createAndroidComposeRule<ComponentActivity>()`): callback đăng ký trên `ApplicationProvider.getApplicationContext<Application>()`, `Robolectric.buildActivity(ComponentActivity::class.java).setup()`, `pause().resume()`, `recreate()`, `pause().stop().destroy()`; và in giá trị số của các tên UMP.
- **Lệnh:** `scripts/check.sh` với `JAVA_HOME` của Android Studio (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) và `ANDROID_HOME=/Users/khacnhat/Library/Android/sdk`, ba lần.
- **Kết quả:**
  - Lần 1: lỗi biên dịch test `FullScreenAdsTest.kt:81` "No value passed for parameter 'connectivity'": test này dựng thẳng `AdsController`, nên mọi tham số mới của constructor phải sửa nó (đã đưa vào mục 3.2). Code chính, Hilt của `:app` và tên UMP biên dịch qua.
  - Lần 2: một assertion của test tạm sai (tôi so trùng giữa mã trạng thái và mã lỗi, vốn dùng chung số). Giá trị đo được: `ConsentStatus` `REQUIRED=2`, `OBTAINED=3`, `NOT_REQUIRED=1`, `UNKNOWN=0`; `FormError.ErrorCode` `INTERNAL_ERROR=1`, `INTERNET_ERROR=2`, `INVALID_OPERATION=3`, `TIME_OUT=4`, khớp S-04, S-05. Chuỗi callback: `created, started, resumed, paused, resumed, paused, stopped, destroyed:true, created, started, resumed, paused, stopped, destroyed:false` (`recreate()` báo `isChangingConfigurations` true, hủy thật báo false).
  - Lần 3: "Module dependency rule OK." (`:core:ads` phụ thuộc `:core:network`, `:core:analytics`), ktlint, lint `:app:lintStagingDebug`, build staging, "Test JVM: 457 test, 0 lỗi ... ./core/ads: 201 test, 0 lỗi", "Cổng chất lượng: QUA".
  - Không chạy thử: UMP thật trên máy (form, lỗi mạng thật), vì JVM không có UMP; kiểm thủ công ở mục 4. Phương án dự phòng ở mục 3.4 chưa cần.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** hộp thoại xin đồng ý và thứ tự khởi tạo SDK (điểm 5: khởi tạo trong `Application`, init sớm, quyền tạm), quảng cáo (điểm 1: khi nào được gửi yêu cầu và được hiện), module core dùng chung `:core:ads`, `:core:network` và file build `core/ads/build.gradle.kts` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là quyết định về đồng ý (GDPR) (risk-scoring.md, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:core:network`, `:app`), thêm API công khai (`start`, `consentPath`, `consentFlowFinished`, `sdkReady`, `InternetConnectivity`, `ConsentShortcutConfig`, `rememberResetConsent`) |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, luật có dẫn chứng; AI tự quyết vài chi tiết: cấu hình build của staging, HD-04 do `:core:ads` tự đọc khóa, nút đặt lại để T-036 (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi luật có unit test với UMP giả; hành vi thật của UMP (form EEA, lỗi mạng thật, rút đồng ý) chỉ kiểm được bằng tay trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi lúc SDK khởi tạo mà mọi định dạng dựa vào; `AdsController.kt` cũng do T-019, T-021, T-022, T-023 sửa; T-021, T-024, T-025, T-033 dựa vào API mới |
| E. Độ mới của kỹ thuật | 2 | Khởi động module trong `Application` qua Hilt và callback vòng đời của `Application` chưa có trong dự án; phổ biến, có tài liệu; đã chạy thử (mục 4.1) |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (constructor, khởi tạo SDK, cổng), `ConsentDebugOptions.kt`, `core/ads/build.gradle.kts`, `NetworkModule.kt`, `TemplateApplication.kt`, hai module flavor. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **Phụ thuộc T-019** (chưa merge khi viết plan): dùng `AdsFlags`, `AdsGlobalFlags.consentShortcutEnabled`, constructor `AdsController(..., flags: AdsFlags, ...)` không còn `RemoteSettings`, `historyStore`. Task chạy sau khi T-019 merge.
- **File chung với các plan đợt 3 (L-054):**

| File | T-019 | T-020 (plan này) | T-022 | T-023 |
| --- | --- | --- | --- | --- |
| `core/ads/.../AdsController.kt` | Sửa | Sửa | Sửa | Sửa |
| `core/ads/.../AdsFlags.kt` | Tạo | Dùng, không sửa | Dùng, không sửa | Dùng, không sửa |
| `core/ads/.../ForegroundAdSuppressor.kt` | | **Tạo** | Dùng, không sửa | |
| `core/ads/.../AdsActivityTracker.kt` | | **Tạo** | Dùng, không sửa | |
| `core/network/.../InternetConnectivity.kt` | | **Tạo** | | Dùng, không sửa |
| `core/ads/build.gradle.kts` | | Sửa (`:core:network`) | | Sửa (`:core:analytics`) |
| `core/ads/src/test/.../FullScreenAdsTest.kt` | Sửa | Sửa | không sửa (constructor giữ) | Sửa |
| `core/ads/.../FullScreenAdGuard.kt` | giữ | không sửa | Sửa (viết lại theo I-01 tới I-05) | |
| `MainActivity.kt` | | không sửa | Sửa | |

- Task khác có thể xung đột: T-021 (nạp sẵn) dùng `canRequestAds`, `mayRequestNow()`, `onGateOpened`, `coordinator.onPreloadRequested()`, `InternetConnectivity`, `sdkReady`; T-021 thay lượt nạp sẵn lại đơn giản trong `onGateOpened(false)` bằng kế hoạch nạp sẵn và lượt giữ lại của S-23. T-022 dùng `ForegroundAdSuppressor`, `AdsActivityTracker` và chèn gỡ cờ kẹt trước `coordinator.onActivityResumed` (G-06). T-024 dùng `consentFlowFinished`, `consentPath`, `sdkReady`. T-025 đọc `ForegroundAdSuppressor.suppressed`. T-033 khởi động AppsFlyer ở `onGateOpened(firstTime = true)`. T-036 đặt nút `rememberResetConsent()` và hiện `consentPath`.
- Đề xuất thứ tự chạy: T-019, rồi T-020, rồi T-022 (T-022 dùng hai file T-020 tạo; câu hỏi 1), rồi T-023, rồi T-021. Không chung phiên với task khác sửa `AdsController.kt` mà chưa xếp theo thứ tự này.

## 7. Điều kiện dừng riêng

- Tên của T-019 khác khi task chạy: không có `AdsFlags`, `AdsGlobalFlags.consentShortcutEnabled`, hay constructor `AdsController` còn `RemoteSettings`. Dừng, không tự đọc `RemoteSettings` trong `:core:ads`.
- `checkModuleDependencies` hay Hilt từ chối `:core:ads` phụ thuộc `:core:network` (vòng phụ thuộc, thiếu binding) mà không sửa file ngoài danh sách.
- Cần đổi chữ ký mà `MainActivity`, `AdGalleryActivity`, `PrivacyOptionsButton` hay `feature/sample` gọi.
- `AppLaunchTest` trên CI đỏ vì `TemplateApplication.onCreate` (khởi động trong `Application` làm hỏng khởi động app hay Firebase).
- UMP không cho biết form lỗi vì activity đã hủy theo cách của mục 3.1 (`isFinishing`, `isDestroyed`, `isChangingConfigurations`) và cần một cách đo khác.
- Cần sửa `AdSlot.kt`, `NativeAdSlot.kt`, `CollapsibleNativeAd.kt`, `InListAds.kt` để slot bỏ quảng cáo khi cổng đóng (mục 3.1 dựa vào việc chúng đã đọc `canRequestAds`).

## 8. Câu hỏi cho người duyệt

1. **T-022 dùng hai file T-020 tạo (`ForegroundAdSuppressor.kt`, `AdsActivityTracker.kt`), nhưng ticket T-022 chỉ phụ thuộc T-019, T-009.** Ticket T-020 ghi S-16 (form chặn quảng cáo khi quay lại), T-022 ghi bộ đếm chặn; G-06 đòi gỡ cờ kẹt chạy trước hỏi lại đồng ý, tức T-022 chèn vào chỗ T-020 tạo.
   - Đề xuất của AI: thêm T-020 vào `depends_on` của T-022 và chạy T-020 trước. Phương án khác: T-022 tạo bộ đếm, T-020 phụ thuộc T-022; khi đó T-020 (cỡ L, rủi ro nhất) chờ thêm một task.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

2. **Cấu hình lúc build của nhánh rút gọn ở staging.** S-12 ghi mặc định cả hai tắt.
   - Đề xuất của AI: production tắt; staging bật phần lúc build, khóa từ xa vẫn mặc định tắt. Staging mặc định vẫn theo UMP, nhưng người thử bật được `is_enable_consent_shortcut` trên Firebase staging để thử `CACHED_EARLY`, `ERROR_ALLOWED` (kiểm thủ công bước 4). Nếu muốn cả hai flavor tắt thì bước 4 cần một bản build riêng.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

3. **HD-04 là việc của `:app` trong bản đặc tả, nhưng khóa `is_enable_consent_shortcut` thuộc `:core:ads`.**
   - Đề xuất của AI: `:core:ads` tự đọc khóa qua `AdsFlags` và giữ lượt hỏi tới khi có giá trị đầu; `:app` chỉ cấp phần lúc build (`ConsentShortcutConfig`). Ít chỗ nối hơn, cùng kết quả: lượt hỏi đầu luôn dùng giá trị đã lưu hay mặc định.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

4. **Nút đặt lại đồng ý (S-18) đặt ở đâu.** Màn xem thử có ảnh chuẩn; thêm nút làm đổi 3 ảnh chuẩn của trang mặc định.
   - Đề xuất của AI: task này làm API (`rememberResetConsent`, chỉ chạy khi `allowConsentReset`) và test; T-036 (đã có việc hiện trạng thái đồng ý trên màn xem thử) đặt nút và ghi lại ảnh chuẩn. Nếu muốn có nút ngay thì thêm `AdGalleryScreen.kt`, ba ảnh `AdGalleryScreen_default_*.png` và test của chúng vào mục 3.2.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

5. **Đổi hành vi khi nhánh rút gọn tắt.** Code hiện tại khởi tạo SDK ngay khi `canRequestAds()` của UMP là true sau lượt gửi (ADR-0013: "không chờ cập nhật"). Theo S-12, nhánh tắt thì chờ lượt cập nhật.
   - Đề xuất của AI: theo S-12 (ADR-0026 thay phần này của ADR-0013). Người dùng đã đồng ý ở vùng có luật sẽ thấy quảng cáo muộn hơn vài trăm mili giây ở production cho tới khi bật nhánh rút gọn.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

6. **Ticket cỡ L, gợi ý tách.**
   - Đề xuất của AI: giữ một task. Máy trạng thái là một lớp; tách "khởi tạo sớm và quyền tạm" khỏi "rút đồng ý" làm hai task cùng sửa `ConsentGate` và `AdsController` liền nhau. Nếu tách, phần 2 gồm S-14, S-15, S-18 và KB-12.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

7. **Nạp sẵn lại khi cổng mở lại.** S-10 giữ một lượt, S-14 nạp sẵn khi cấp lại; kế hoạch nạp sẵn và lượt giữ lại đầy đủ là S-23 của T-021.
   - Đề xuất của AI: task này gọi lại `preloadWhenEnabled` hiện có cho các vị trí toàn màn hình đang bật, mỗi lần cổng mở lại; T-021 thay bằng kế hoạch.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
