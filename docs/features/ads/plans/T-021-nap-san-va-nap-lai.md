---
ticket: T-021
title: Nạp sẵn theo nhóm, chờ SDK, thử lại và nạp lại khi có mạng
status: approved                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý 15M (scripts/token-usage.py estimate, 2026-10-24); nâng vì dựa trên code của bốn plan chưa merge và nối nhiều chỗ trong AdsController
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                # người duyệt
cross_reviewer: Nhật          # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-021: NẠP SẴN THEO NHÓM, CHỜ SDK, THỬ LẠI VÀ NẠP LẠI KHI CÓ MẠNG

## 1. Tóm tắt

**Phiên bản 2 (2026-10-24, `draft`, soạn trước RUN-20261024-03).** Bản 1 chưa chạy: bị bỏ qua ở RUN-20261024-02 vì T-023 dừng. Rà soát độc lập trước RUN-20261024-03 (đối chiếu với code thật của T-019, T-020, T-022 đã merge và nhánh T-023) thấy một điểm chặn và năm điểm nên sửa. Vì sao plan chưa đủ tốt: bản 1 viết trước khi có code của các plan chạy trước, và T-023 phiên bản 2 để lại cho task này quyết khóa bộ đếm của lượt nạp sẵn (`preload:<định dạng>`, dùng chung cho mọi vị trí cùng định dạng); kế hoạch nạp sẵn nạp mọi vị trí đang bật của một định dạng cùng lúc, nên khóa chung trộn thời gian chờ thử lại và đè độ trễ giữa các vị trí, đúng lỗi của L-068. Thay đổi:

- Bộ đếm lượt nạp toàn màn hình khóa theo **từng vị trí** (câu hỏi 9, anh Nhật chọn): hàm thuần `preloadCounterKey(placementKey)` trong `FullScreenReloads.kt`; `AdsController` thay `preloadSlot(format)` của T-023 bằng hàm này cho interstitial và native toàn màn hình; test hai vị trí cùng định dạng có lượt nạp chồng nhau (mục 4).
- `LoadAttemptCounter` thành trường `loadCounter` của `AdsController`, dùng chung cho `AdTelemetry` và `FullScreenReloads` (bản 1 không có chỗ đọc vì `AdTelemetry.counter` là `private`).
- `nativeLoadOnce` nhận hàm dựng `newEvents: () -> NativeAdEvents?` và dựng `NativeAdEvents` mới ở mỗi lượt nạp, vì `NativeAdMetrics` của T-023 giữ trạng thái của một quảng cáo (dự phòng 2 giây, nguồn quảng cáo); nhận thêm `timeoutMillis` vì `NATIVE_LOAD_TIMEOUT_MILLIS` là hằng `private` của `AdsController`.
- `canRequestAds` giữ nguyên (`gate.allowed && sdkStarted`); trần 35 giây chỉ áp cho `sdkReady` mà kế hoạch nạp sẵn chờ (câu hỏi 10, anh Nhật chọn). Bản 1 đổi `canRequestAds`, làm mọi khe banner và native chờ tới 35 giây.
- Sửa câu chữ về code của T-020: `onGateOpened(false)` gọi `preloadNow` cho mọi vị trí toàn màn hình, còn `preloadWhenEnabled` nằm trong `startSdk`.
- Tên của T-023 theo phiên bản 2 (`AdTelemetry.nativeLoadReport`, `NativeLoadUse`, `NativeAdMetrics` nhận `LoadReport`).

Thêm vào `:core:ads` một kế hoạch nạp sẵn theo nhóm khe (S-20 tới S-23), một bộ điều phối giữ lượt nạp sẵn bị chặn, chờ SDK sẵn sàng tối đa 35 giây, nạp lại khi mạng về sau 3 giây ổn định và nạp bù native khi activity hiện lại (S-24, S-25), cùng luật thử lại thuần cho quảng cáo toàn màn hình (F-05, F-06) và native (O-05, O-09). Mọi luật là hàm hay lớp thuần, test trên JVM với đồng hồ giả hay thời gian ảo của coroutine; `AdsController` chỉ nối chúng với SDK, nạp interstitial bằng activity đang mở khi có (câu hỏi 4 của plan T-034). Kiểm chứng bằng unit test cho từng luật và biên, và kiểm thủ công trên staging bằng chế độ máy bay.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-24 | Khi tải quảng cáo toàn màn hình lỗi thì app thử lại sau 2, 4, 6, 8 rồi 10 giây mỗi lần, không bỏ cuộc. | F-05: lần lỗi liên tiếp thứ n chờ `min(n x 2 giây, 10 giây)`, mọi mã lỗi, không giới hạn số lần, không ngẫu nhiên; n về 0 khi nạp thành công. n đọc từ `LoadAttemptCounter.failuresInARow` của T-023 (không viết bộ đếm thứ hai), khóa theo từng vị trí (`preloadCounterKey`, câu hỏi 9). Trong lúc chờ, khe ở `FullScreenLoadState.WaitingRetry(dueAtMillis)` của T-022, nên "nạp nếu cần" (`loadFullScreenAdIfNeeded`) không gửi thêm (F-04). Áp cho interstitial ngay (định dạng toàn màn hình duy nhất đang có SDK); app open (T-024) và rewarded (T-031) dùng cùng lớp `FullScreenReloads`. |
| KB-24 | Khi tải native lỗi thì app thử lại với khoảng chờ tăng dần tới 60 giây; chưa có native nào thì thử lại sau 5 giây. | O-05: còn giữ quảng cáo thì chờ 2, 4, 8, 16, 32 rồi 60 giây (n tối đa 10, trần 60 giây); chưa giữ quảng cáo nào thì cố định 5 giây; mọi mã lỗi; bộ đếm về 0 khi thành công. Cả hai nhánh là hàm thuần có test biên. Khe native một quảng cáo đang có (`NativeAdSlot`, `CollapsibleNativeAdSlot`, qua `AdsController.nativeAd`) chưa giữ quảng cáo khi lỗi, nên dùng nhánh 5 giây ngay ở task này (câu hỏi 1). Nhánh "còn giữ quảng cáo" chỉ xảy ra khi khe tự thay nội dung mỗi 60 giây, việc của T-027; T-027 gọi đúng hàm này. |
| KB-24 | Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại. | S-24: chỉ tính Internet đã xác thực (`InternetConnectivity.connected` của T-020, bọc theo dõi của T-016); gộp các lần đổi trong 3 giây; giá trị ổn định đổi từ `false` sang `true` thì (1) xin nạp sẵn lại theo kế hoạch (khe toàn màn hình đang rảnh nạp ngay, khe đang chờ thử lại nạp khi tới hạn, tối đa 10 giây), (2) đặt lại thời gian chờ thử lại của native: khe native đang chờ gửi lại ngay. Mở app khi đang ở chế độ máy bay rồi tắt cũng tính là mạng về (L-A16). |
| KB-24 | Khi app ở nền thì native không gửi yêu cầu. | O-05, L-A19: vòng thử lại native chờ app lên foreground (activity ở `started`, theo `AdsActivityTracker` của T-020) rồi mới gửi; lượt tới hạn khi ở nền dời tới lúc lên foreground. Lượt gửi đầu cũng chờ foreground. Native toàn màn hình đứng riêng theo cùng luật (câu hỏi 4). |

Luật trong phạm vi ticket mà spec không có tiêu chí riêng, vẫn có test (mục 4):

- S-20: không khe nào tự nạp khi được tạo; mọi lượt nạp sẵn đi qua kế hoạch. Bỏ `preloadWhenEnabled` (mỗi vị trí tự nạp khi được bật) của `AdsController`; vị trí được bật từ xa thì xin một lượt nạp sẵn qua kế hoạch.
- S-21: "SDK sẵn sàng" khi callback khởi tạo về hoặc sau tối đa 35 giây kể từ lúc gọi khởi tạo; chỉ nạp sẵn khi sẵn sàng. T-020 đặt `sdkReady` ở callback; task này thêm trần 35 giây.
- S-22: kế hoạch là danh sách nhóm khe chạy lần lượt; nhóm sau bắt đầu khi mọi khe của nhóm trước xong (có quảng cáo hay lỗi) hoặc sau tối đa 6 giây; kiểm mỗi 200 ms; khe không có trong kế hoạch không được nạp sẵn. Khe theo định dạng (M-05, câu hỏi 14 của spec), nên kế hoạch là các nhóm `AdFormat`; một định dạng chạy mọi vị trí đang bật của nó.
- S-23: lượt bị chặn vì cổng đóng được giữ lại một lượt, chạy đúng một lần khi cổng mở, không chồng lên kế hoạch đang chạy. Không giữ khi SDK chưa khởi tạo, khi chưa từng có lượt nạp sẵn đầu tiên (lượt `SDK_READY`), hay khi quyền thật của UMP là "không" (`consentPath` là `DENIED_OR_REQUIRED`); khi đó lệnh không làm gì. Nạp bù và nạp lại khi mạng về cũng đi qua kế hoạch. Cách hiểu: thử lại sau lỗi (F-05, O-05) và nạp lại sau khi đóng (F-06) là luật riêng của từng khe, không phải "nạp lại" của S-23.
- S-25: activity của app hiện lại thì nạp bù các khe native theo thứ tự kế hoạch, chỉ khi kế hoạch đã chạy xong ít nhất một lần, không có kế hoạch đang chạy và không có quảng cáo toàn màn hình. Hôm nay chưa có khe native nào nạp sẵn (khe native nạp khi lên màn), nên việc này chỉ có tác dụng với native toàn màn hình; T-027, T-028 đăng ký khe native vào kế hoạch.
- S-15: lượt bị chặn ngay trước khi gửi (`mayRequestNow()` của T-020 trả false) kết thúc lượt: không gửi, không tính là lỗi, không hẹn thử lại; khe toàn màn hình về `Idle`. F-04 (A 03:30-32): lượt hẹn giờ tới hạn khi cổng đóng thì khe thôi "đang hẹn giờ" (dựa vào `needsLoad` của T-022).
- F-06: inter (và rewarded sau này) đóng hay hiện lỗi thì nạp lại sau 2 giây; app open nạp ngay.
- O-09: chuẩn bị yêu cầu lỗi (dựng `AdRequest` hay lời gọi nạp ném lỗi, kể cả SDK tự hủy giữa chừng) là một lần nạp lỗi mã 0 và thử lại theo luật của khe.
- S-09: mỗi lần xin nạp sẵn gọi `coordinator.onPreloadRequested()` của T-020 (hỏi lại đồng ý khi đang có `ERROR_ALLOWED`).
- HD-02: kế hoạch mặc định suy ra từ danh sách vị trí; `:app` đặt lại bằng `AdsController.setPreloadPlan(plan)` trong `Application` trước khi đồng ý có kết quả (câu hỏi 3).
- Câu hỏi 4 của plan T-034 (đã trả lời): interstitial nạp bằng activity đang mở khi có, để Unity và ironSource tham gia đấu giá; không có thì dùng `ApplicationContext` như hôm nay.

Test nghiệm thu gợi ý của ticket (`AT-KD-*`, phần thử lại của `AT-FS-*`, `AT-NT-*`) là bản đặc tả độc lập của AdsKit, không có trong repo; plan dùng các luật S-20 tới S-25, F-04 tới F-06, O-05, O-09 của `ads-business-rules.md` làm danh sách test.

Không thuộc task này: khe app open, rewarded (T-024, T-031: dùng `FullScreenReloads` và `FullScreenPreloadSlot` của task này); thay nội dung tại chỗ mỗi 60 giây và khe native một quảng cáo mới (T-027, dùng nhánh "còn giữ quảng cáo"); pool và native trong danh sách (T-028; `loadNativeAd` của `rememberInListAds` giữ nguyên, không thử lại); banner (T-029, B-02 dùng `AdRetryPolicy.fullScreenDelayMillis`); màn loading chờ SDK 35 giây (T-024 đọc `sdkReady`).

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0001, ADR-0004, ADR-0019, ADR-0024 (theo dõi Internet chung, app luôn cần mạng), ADR-0026 (quyết định theo AdsKit PARTNER).

Dựa trên bốn plan chưa merge khi viết plan này (chạy trước trong cùng phiên theo thứ tự T-019, T-020, T-022, T-023, T-021), dùng đúng tên:

- T-019 (`docs/features/ads/plans/T-019-khoa-remote-config-theo-adskit.md`): `AdsFlags` (`placementEnabled`), `AdPlacement(key, format, intervalGroup, nativeEvery)`, 11 giá trị `AdFormat` (`APP_OPEN`, `NATIVE_STEP`, `REWARDED`...), `AdPlacementDeclaration`; `AdsController.isEnabled(placement)` đọc `AdsFlags`.
- T-020 (`docs/features/ads/plans/T-020-dong-y-va-khoi-dong-partner.md`): `AdsController.start(application, appActivities)`, `canRequestAds`, `mayRequestNow()`, `consentPath: StateFlow<ConsentPath?>` với `ConsentPath.DENIED_OR_REQUIRED`, `consentFlowFinished`, `sdkReady`, `onGateOpened(firstTime)`, `coordinator.onPreloadRequested()`, `InternetConnectivity` (`connected: Flow<Boolean>`), `AdsActivityTracker` (foreground, `resumedActivity`, báo `onActivityResumed`; chỉ activity của app qua `AppActivityFilter`, activity của SDK bị bỏ qua). T-020 nạp sẵn ở hai chỗ: `startSdk` gọi `preloadWhenEnabled` cho mọi vị trí toàn màn hình sau `MobileAds.initialize`, và `onGateOpened(false)` gọi `preloadNow` cho mọi vị trí toàn màn hình (`AdsController.kt`, phiên bản 2 sửa câu chữ theo code thật); task này thay cả hai bằng kế hoạch. `onForegroundChanged` của `AdsActivityTracker` đã nối vào `coordinator.onForegroundChanged` trong `AdsController`.
- T-022 (`docs/features/ads/plans/T-022-cong-toan-man-hinh-va-chan-tu-hien.md`): `MonotonicClock`, `SystemMonotonicClock`, `FullScreenLoadState` (`Idle`, `Loading`, `Ready`, `WaitingRetry(dueAtMillis)`), `needsLoad`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, `AdPlacement.ignoreInterval`, `recordInterval`, `ActivityResumeSteps`, `FullScreenAdPresence.showing`.
- T-023 (`docs/features/ads/plans/T-023-doanh-thu-theo-vi-tri.md`, phiên bản 2): `LoadAttemptCounter` (`onRequestSent(slot, now)`, `onFailed(slot, now)`, `failuresInARow(slot)`; cập nhật ở mọi lượt nạp của khe có bộ đếm, kể cả khi số liệu tắt), `AdTelemetry(..., counter, ...)`, `AdTelemetry.requestSent`, `loadSucceeded`, `loadFailed(format, slot, placement, code, sdkMessage, now)`, `AdTelemetry.opportunity`, `LoadReport`, `AdTelemetry.nativeLoadReport(placement, NativeLoadUse)`, `NativeAdEvents` và tham số `events` của `SdkNativeAdLoader`, lớp `NativeAdMetrics` trong `AdsController` (móc doanh thu của native mà `nativeAd` truyền vào). Hàng native trong danh sách không qua bộ đếm (câu hỏi 6 của T-023).

## 3. Giải pháp

### 3.1. Cách làm

Mọi luật là hàm hay lớp thuần trong `:core:ads`, nhận thời điểm qua `MonotonicClock` (T-022) hay chạy trên `CoroutineScope` được truyền vào, để test dùng đồng hồ giả hay thời gian ảo của `kotlinx-coroutines-test` như `NativeAdLoadingTest`. `AdsController` chỉ nối.

**1. Luật thử lại** (`AdRetryPolicy.kt`, mới):

```kotlin
internal object AdRetryPolicy {
    /** F-05, B-02: min(n x 2 s, 10 s) for the n-th failure in a row; n below 1 counts as 1; never gives up. */
    fun fullScreenDelayMillis(failuresInARow: Int): Long
    /** O-05: holding an ad, 2 s doubled up to 60 s (n at most 10); holding none, always 5 s. */
    fun nativeDelayMillis(failuresInARow: Int, holdsAd: Boolean): Long
    /** F-06: interstitial and rewarded wait 2 s after closing or failing to show; app open and native full screen reload at once (O-01). */
    fun reloadAfterCloseMillis(format: AdFormat): Long
}
```

Hằng số: `FULL_SCREEN_STEP_MILLIS = 2_000`, `FULL_SCREEN_MAX_MILLIS = 10_000`, `NATIVE_FIRST_MILLIS = 2_000`, `NATIVE_MAX_MILLIS = 60_000`, `NATIVE_NO_AD_MILLIS = 5_000`, `RELOAD_AFTER_CLOSE_MILLIS = 2_000` (mục 11 của `ads-business-rules.md`). Tính không tràn số với n rất lớn. `reloadAfterCloseMillis` ném `IllegalArgumentException` cho định dạng không phải toàn màn hình.

**2. Kế hoạch nạp sẵn** (`AdPreloadPlan.kt`, mới, công khai cho `:app`, HD-02):

```kotlin
/** Groups of ad slots preloaded one after the other (S-22). A format in no group is never preloaded. */
class AdPreloadPlan(groups: List<Set<AdFormat>>) {
    val groups: List<Set<AdFormat>>   // empty groups dropped; a format in two groups is an IllegalArgumentException
    companion object {
        /** HD-02 default: app open and the first step of a flow first, then every other format the placements use. */
        fun fromPlacements(placements: List<AdPlacement> = AdPlacement.entries): AdPreloadPlan
    }
}
internal fun defaultPreloadPlan(formats: Collection<AdFormat>): AdPreloadPlan
```

Mặc định hai nhóm: `{APP_OPEN, NATIVE_STEP}` (khe người dùng thấy đầu tiên, S-22) rồi mọi định dạng còn lại mà danh sách vị trí dùng (câu hỏi 2). Với danh sách vị trí của dự án mẫu hôm nay (không có app open hay bước của luồng) kế hoạch là một nhóm.

**3. Chạy kế hoạch** (`PreloadPlanRunner.kt`, mới):

```kotlin
/** One slot the plan can preload: a full-screen placement today, native slots from T-027 and T-028. */
internal interface PreloadSlot {
    val format: AdFormat
    /** Starts a load when needed; never when loading, ready or waiting for a retry (F-04). */
    fun loadIfNeeded()
    /** Has an ad, failed or waits for a retry: the group may move on (S-22). */
    fun settled(): Boolean
}

internal class PreloadPlanRunner(private val clock: MonotonicClock, private val logger: AppLogger) {
    /** Runs the groups in order: the next one starts once every slot of the current one settled, or after 6 s; checks every 200 ms. */
    suspend fun run(plan: AdPreloadPlan, slotsOf: (AdFormat) -> List<PreloadSlot>)
}
```

`GROUP_CAP_MILLIS = 6_000`, `POLL_MILLIS = 200`. Kiểm lần đầu ngay sau khi bắt đầu nhóm (nhóm mà mọi khe đã xong, ví dụ vị trí đang tắt, chuyển ngay), rồi mỗi 200 ms. Định dạng không có khe nào tính là xong. Log thẻ `Ads`: `Preload group 1/2 started: APP_OPEN, NATIVE_STEP`, `Preload group 1/2 moved on: settled` hay `timed out`, `Preload plan finished`.

**4. Quyết định một lượt nạp sẵn** (`PreloadDecision.kt`, mới):

```kotlin
internal enum class PreloadReason { SDK_READY, GATE_REOPENED, NETWORK_BACK, PLACEMENT_ENABLED, NATIVE_REFILL }
internal enum class PreloadDecision { RUN, HOLD, IGNORE_SDK_NOT_STARTED, IGNORE_CONSENT_DENIED, IGNORE_NEVER_REQUESTED, IGNORE_RUNNING }

internal data class PreloadGate(
    val sdkStarted: Boolean,     // MobileAds.initialize was called (onGateOpened(firstTime = true))
    val sdkReady: Boolean,       // S-21
    val mayRequest: Boolean,     // mayRequestNow() of T-020: consent gate and foreground for ERROR_ALLOWED
    val consentDenied: Boolean,  // consentPath == DENIED_OR_REQUIRED
)

/** S-21, S-23, in this order: SDK not started, consent denied, never requested, running, then HOLD or RUN. */
internal fun decidePreload(reason: PreloadReason, gate: PreloadGate, everRequested: Boolean, running: Boolean): PreloadDecision

/** S-21: ready at the init callback, or 35 s after init started; never before init started (S-19). */
internal fun sdkReadyAt(initStartedAt: Long?, callbackArrived: Boolean, now: Long): Boolean
```

`SDK_READY_CAP_MILLIS = 35_000`. `everRequested` thành true ở lượt `SDK_READY` đầu tiên.

**5. Bộ điều phối** (`AdPreloader.kt`, mới):

```kotlin
internal class AdPreloader(
    private val scope: CoroutineScope,
    private val runner: PreloadPlanRunner,
    private val slotsOf: (AdFormat) -> List<PreloadSlot>,
    private val gate: () -> PreloadGate,
    private val askConsentAgain: () -> Unit,   // coordinator.onPreloadRequested() of T-020 (S-09)
    private val logger: AppLogger,
) {
    fun setPlan(plan: AdPreloadPlan)            // HD-02; while a plan runs, used from the next run (warn log)
    fun request(reason: PreloadReason)          // S-23: asks consent again, then decidePreload
    fun onGateChanged()                         // runs the held request once when RUN; drops it when IGNORE_CONSENT_DENIED
    fun onNetworkReturned()                     // S-24: request(NETWORK_BACK) and nativeRetryResets + 1
    fun onActivityResumed(fullScreenShowing: Boolean)  // S-25
    val nativeRetryResets: StateFlow<Int>       // read by retryingNativeAdStates
    val running: Boolean
    val finishedOnce: Boolean
}
```

- `request`: `RUN` thì chạy kế hoạch trong `scope`; `HOLD` thì giữ đúng một lượt (lượt thứ hai khi đang giữ không thêm gì); `IGNORE_*` thì ghi log lý do, không giữ.
- `onActivityResumed`: chỉ khi `finishedOnce`, không `running`, `fullScreenShowing` false, và `decidePreload(NATIVE_REFILL, ...)` là `RUN`: chạy kế hoạch chỉ gồm định dạng native (`NATIVE_*`), giữ thứ tự nhóm. Không giữ lượt nạp bù bị chặn.
- Mọi lượt nạp sẵn, nạp lại và nạp bù không ghi `AdTelemetry.opportunity` (lượt dò không đo, A-04, HD-06).

**6. Mạng về** (`NetworkReturns.kt`, mới, S-24):

```kotlin
/** Emits once each time usable Internet, stable for [windowMillis], comes back after a stable loss. A first stable `true` is no return. */
internal fun networkReturns(connected: Flow<Boolean>, windowMillis: Long = NETWORK_SETTLE_MILLIS): Flow<Unit>
```

`NETWORK_SETTLE_MILLIS = 3_000`. Cài bằng `collectLatest` và `delay` (giá trị chỉ được tính khi giữ nguyên đủ 3 giây), cùng cách `LostAfterDelay` của `:core:network` chờ trước khi báo; không dùng `debounce` (API thử nghiệm của coroutine, chưa có trong dự án).

**7. Thử lại native trong khe đang có** (`NativeAdRetry.kt`, mới, O-05, O-09, KB-24 tiêu chí 2 và 4):

```kotlin
/**
 * Loading, then the result of [load]; after a failure, Unavailable and a new try 5 s later (no ad held), sent only
 * in foreground and only when [mayRequestNow]; [retryResets] cuts the wait short (S-24). Never emits Loading again,
 * so an empty slot stays empty until an ad comes. A Loaded ad is destroyed when the collection ends.
 */
internal fun retryingNativeAdStates(
    load: suspend () -> NativeAdState,       // loadOneNativeAd(...) of T-010, unchanged
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState>
```

- Trước mỗi lần gửi: chờ `foreground` true, rồi hỏi `mayRequestNow()`; false thì ghi log `Native <mã> request blocked`, phát `Unavailable` nếu chưa phát, và kết thúc (S-15: không tính lỗi, không hẹn). Khe sẽ được dựng lại khi `canRequestAds` đổi (code có sẵn của `NativeAdSlot`).
- `load` ném lỗi (không phải hủy coroutine) thì là lỗi mã 0 (O-09), log `Native <mã> failed: 0 (request not built)`.
- Chờ thử lại: `withTimeoutOrNull(AdRetryPolicy.nativeDelayMillis(n, holdsAd = false)) { retryResets.first { it != generationAtFailure } }`.
- `internal fun nativeLoadOnce(newEvents: () -> NativeAdEvents?, newLoader: (NativeAdEvents?) -> NativeAdLoader, unitId: String, size: NativeAdSize, timeoutMillis: Long, logger: AppLogger, placementKey: String): suspend () -> NativeAdState` (cùng file): mỗi lần gọi dựng `NativeAdEvents` mới bằng `newEvents()`, dựng loader mới bằng `newLoader(events)` rồi `loadOneNativeAd(loader, unitId, size, timeoutMillis, logger, placementKey)`. `AdsController` truyền `NATIVE_LOAD_TIMEOUT_MILLIS` (hằng `private` của nó). Mỗi lượt một `NativeAdEvents` mới, vì `NativeAdMetrics` của T-023 giữ trạng thái của một quảng cáo (dự phòng 2 giây, `fallbackJob`, nguồn quảng cáo): quảng cáo của lượt trước về muộn rồi bị hủy không được hủy dự phòng của quảng cáo đang hiện. Tách ra để test kiểm được móc của T-023 luôn đi tới loader ở mọi lượt (bước 10).
- `nativeAdStates` và `loadOneNativeAd` (`NativeAdLoading.kt`) giữ nguyên; test của T-010 không đổi.

**8. Khe toàn màn hình** (`FullScreenReloads.kt`, mới, F-05, F-06, S-15, O-09):

```kotlin
internal class FullScreenReloads(
    private val scope: CoroutineScope,
    private val clock: MonotonicClock,
    private val failuresInARow: (AdPlacement) -> Int,   // loadCounter.failuresInARow(preloadCounterKey(placement.key))
    private val loadIfNeeded: (AdPlacement) -> Unit,     // loadFullScreenAdIfNeeded of T-022
    private val foreground: StateFlow<Boolean>,          // native full screen only (O-05)
    private val logger: AppLogger,
) {
    /** F-05 (O-05 for native full screen): the state to keep, and a retry scheduled at its due time. */
    fun onLoadFailed(placement: AdPlacement, code: Int): FullScreenLoadState.WaitingRetry
    /** S-15: nothing sent, not a failure, no retry. */
    fun onSendBlocked(placement: AdPlacement): FullScreenLoadState.Idle
    /** F-06: reload after closing or failing to show. */
    fun onClosedOrShowFailed(placement: AdPlacement)
}
```

- `onLoadFailed`: n là `failuresInARow(placement)` đọc **sau** khi đường nạp đã đếm lỗi này: `LoadAttemptCounter.onFailed` chạy ở mọi lỗi nạp, ở dòng trước, do chính đường nạp cập nhật (lời gọi `AdTelemetry.loadFailed` của T-023 đếm ở dòng đầu, trước và ngoài cổng ghi sự kiện), không phụ thuộc khóa `is_log_ad_*` hay `serving`; test `AdTelemetryTest.loadCounter_updatesWhenLoggingOff` của T-023 giữ điều này (sửa theo rà soát trước phiên, 2026-10-24); chờ `fullScreenDelayMillis(n)` cho `INTERSTITIAL` (sau này `APP_OPEN`, `REWARDED`), `nativeDelayMillis(n, holdsAd = false)` và chờ foreground cho `NATIVE_FULL_SCREEN` (câu hỏi 4). Tới hạn thì gọi `loadIfNeeded`, nơi T-022 xử lý cổng đóng lúc tới hạn (F-04) và T-020 kiểm `mayRequestNow()` trước khi gửi. Log `Retry <mã vị trí> in 4000 ms (failures 2, code 3)`, không ghi mã đơn vị.
- Lượt hẹn mới thay lượt hẹn cũ của cùng vị trí (hủy `Job` cũ), nên không có hai lượt hẹn chồng nhau.
- Khóa bộ đếm theo vị trí (phiên bản 2, câu hỏi 9), cùng file: `internal fun preloadCounterKey(placementKey: String): String = "preload:$placementKey"`. Kế hoạch nạp sẵn nạp mọi vị trí đang bật của một định dạng cùng lúc; mỗi vị trí có số lỗi liên tiếp và thời điểm gửi riêng, nên vị trí A lỗi không làm vị trí B chờ lâu, và độ trễ của hai lượt chồng nhau không đè nhau. `ad_placement` của sự kiện tải vẫn là `preload` (`loadPlacementOf` của T-023, mục 8.2), chỉ khóa của bộ đếm đổi.

`FullScreenPreloadSlot.kt` (mới): `internal class FullScreenPreloadSlot(placement, enabled: () -> Boolean, state: () -> FullScreenLoadState, load: (AdPlacement) -> Unit) : PreloadSlot`; `loadIfNeeded` chỉ gọi `load` khi vị trí đang bật; `settled` là vị trí tắt hoặc trạng thái khác `Loading`.

**9. Context để nạp** (`FullScreenLoadContext.kt`, mới, câu hỏi 4 của plan T-034): `internal fun fullScreenLoadContext(resumed: Activity?, application: Context): Context` trả activity đang resume nếu nó chưa `isFinishing`, chưa `isDestroyed`; không thì `application`. `resumed` là `tracker.resumedActivity`, luôn là activity của app (activity của SDK bị bộ lọc của T-020 bỏ qua), nên quảng cáo không bao giờ nạp bằng `AdActivity` hay activity của adapter. Không giữ activity sau lời gọi nạp.

**10. Nối vào `AdsController`** (không đổi constructor; `FullScreenAdsTest` không phải sửa):

- Trường mới: `loadCounter = LoadAttemptCounter()` (truyền vào `AdTelemetry(counter = loadCounter, ...)` thay cho `LoadAttemptCounter()` dựng tại chỗ của T-023, và đọc ở `FullScreenReloads(failuresInARow = { loadCounter.failuresInARow(preloadCounterKey(it.key)) }, ...)`), `preloader`, `runner`, `fullScreenReloads`, một `FullScreenPreloadSlot` cho mỗi vị trí `isFullScreen`, `enabledNow` (mỗi vị trí toàn màn hình `isEnabled(p).stateIn(scope, SharingStarted.Eagerly, false)`), `foregroundState: MutableStateFlow<Boolean>` cập nhật ở chỗ T-020 báo `coordinator.onForegroundChanged` (hay đọc thẳng nếu `AdsActivityTracker` đã cho `StateFlow`), `sdkInitStartedAt: Long?`.
- `onGateOpened(firstTime = true)`: ghi `sdkInitStartedAt` ngay trước `MobileAds.initialize`, hẹn `delay(SDK_READY_CAP_MILLIS)`; `sdkReady` lên true ở callback hoặc khi hết 35 giây (`sdkReadyAt`), log `SDK ready: callback` hay `SDK ready: 35 s cap`. `canRequestAds` **giữ nguyên** (`gate.allowed && sdkStarted`): khe banner, native một quảng cáo, native thu gọn và số liệu `serving` của T-023 không chờ callback khởi tạo; chỉ kế hoạch nạp sẵn chờ `sdkReady` (qua `PreloadGate.sdkReady`, S-21; câu hỏi 10). `sdkReady` lần đầu lên true thì `preloader.request(SDK_READY)`.
- `onGateOpened(firstTime = false)`: thay `fullScreenPlacements().forEach(::preloadNow)` của T-020 bằng `preloader.request(GATE_REOPENED)` (S-10, S-14).
- Xóa `preloadWhenEnabled` và lời gọi của nó trong `startSdk`. Một collector cho mọi vị trí toàn màn hình: `enabledNow` đổi từ false sang true thì `preloader.request(PLACEMENT_ENABLED)` (S-20, S-23).
- Thu `combine(canRequestAds, sdkReady, consentPath, foregroundState)` gọi `preloader.onGateChanged()`; thu `networkReturns(connectivity.connected)` gọi `preloader.onNetworkReturned()`.
- `onActivityResumed(activity)`: sau `steps.onResumed(activity)` của T-022 (gỡ cờ kẹt, hỏi lại đồng ý, nhả dấu rời app; G-06 để thứ tự việc còn lại cho `:app`), gọi `preloader.onActivityResumed(presence.showing.value)`. Không sửa `ActivityResumeSteps.kt`.
- `setPreloadPlan(plan: AdPreloadPlan)` công khai, chuyển cho `preloader.setPlan`.
- Khóa bộ đếm: thay `preloadSlot(AdFormat.INTERSTITIAL)` và `preloadSlot(AdFormat.NATIVE_FULL_SCREEN)` của T-023 bằng `preloadCounterKey(placement.key)` (interstitial: lời gọi `telemetry.requestSent`, `loadSucceeded`, `loadFailed`; native toàn màn hình: `telemetry.countedLoad(..., slot = preloadCounterKey(placement.key), ...)`); xóa `preloadSlot`.
- Nạp interstitial: `InterstitialAd.load(fullScreenLoadContext(tracker.resumedActivity, context), ...)`. Dựng `adRequest()` và lời gọi nạp bọc `try`/`catch`: lỗi thì đi chung đường với `onAdFailedToLoad` với mã 0 (O-09; T-023 ghi `ad_load_fail` mã `"0"`, `load_state` `internal`). `onAdFailedToLoad`: sau phần ghi của T-023, `loadStates[placement] = fullScreenReloads.onLoadFailed(placement, code)`. Lượt bị `mayRequestNow()` chặn: `loadStates[placement] = fullScreenReloads.onSendBlocked(placement)`.
- Native toàn màn hình: cùng cách (`AdLoader.Builder` vẫn dùng `ApplicationContext`, câu hỏi 4 của T-034 chỉ nói banner, interstitial, rewarded).
- Đóng hay hiện lỗi (chỗ T-022 đang "nạp lại"): `fullScreenReloads.onClosedOrShowFailed(placement)`.
- `nativeAd(placement)`: trả `retryingNativeAdStates(load = nativeLoadOnce(newEvents = { <biểu thức dựng NativeAdMetrics của T-023 trong nativeAd, với telemetry.nativeLoadReport(placement, NativeLoadUse.SINGLE_AD)> }, newLoader = { e -> SdkNativeAdLoader(context, adRequest(), events = e) }, unitId, size, NATIVE_LOAD_TIMEOUT_MILLIS, logger, placement.key), foregroundState, ::mayRequestNow, preloader.nativeRetryResets, logger, placement.key)`; lượt kiểm `mayRequestNow()` của T-020 trước lời gọi nạp chuyển vào vòng này. `loadNativeAd` (danh sách) giữ nguyên.
- Viết lại `nativeAd` **giữ nguyên** móc `NativeAdEvents` (`events`) mà T-023 truyền vào `SdkNativeAdLoader` (plan T-023, mục 3.1 bước 6): mỗi lượt nạp, kể cả lượt thử lại, dựng `NativeAdEvents` mới (cùng vị trí, cùng cách dựng) và `SdkNativeAdLoader` mới, để native vẫn ghi `ad_impression_placement` và dự phòng doanh thu 2 giây. Dùng đúng biểu thức dựng `NativeAdEvents` mà T-023 đã đặt trong `nativeAd`, đặt trong hàm dựng `newEvents`, không dựng lại theo cách khác (sửa theo rà soát trước phiên, 2026-10-24; phiên bản 2: dựng mới ở mỗi lượt).
- Log thẻ `Ads` (ADR-0019): lượt xin nạp sẵn và quyết định (`Preload NETWORK_BACK: HOLD`), lượt giữ được chạy hay bỏ, mạng về, SDK sẵn sàng, mỗi lần hẹn thử lại, lượt bị chặn lúc gửi, nạp bù bị bỏ và lý do. Không ghi mã đơn vị.

**11. Tài liệu:** `docs/ops/ads-guide.md` mục 6 thêm đoạn "Nạp sẵn và nạp lại" (kế hoạch mặc định, cách `:app` đặt kế hoạch bằng `ads.setPreloadPlan(...)` trong `Application.onCreate` trước khi activity đầu tiên hỏi đồng ý, thời gian thử lại, mạng về, native không nạp ở nền). Mục 7: nếu dòng "Unity và ironSource cần `Activity` khi nạp" của T-034 đã có thì ghi interstitial đã nạp bằng activity đang mở từ T-021; chưa có thì không sửa mục 7.

Thứ tự sau task: `Application.onCreate` (`ads.start`, kế hoạch mặc định hay kế hoạch của `:app`) rồi `MainActivity` hỏi đồng ý, cổng mở, `MobileAds.initialize` và đồng hồ 35 giây, SDK sẵn sàng, lượt `SDK_READY` chạy kế hoạch theo nhóm.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdRetryPolicy.kt` | F-05, F-06, O-05 (mục 3.1 bước 1) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPreloadPlan.kt` | `AdPreloadPlan`, `fromPlacements`, `defaultPreloadPlan` (bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PreloadPlanRunner.kt` | `PreloadSlot`, `PreloadPlanRunner` (bước 3) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PreloadDecision.kt` | `PreloadReason`, `PreloadDecision`, `PreloadGate`, `decidePreload`, `sdkReadyAt` (bước 4) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPreloader.kt` | Bộ điều phối S-23, S-24, S-25 (bước 5) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NetworkReturns.kt` | Gộp đổi mạng 3 giây (bước 6) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | `retryingNativeAdStates`, `nativeLoadOnce` (bước 7) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenReloads.kt` | Hẹn thử lại, nạp lại sau khi đóng, lượt bị chặn, `preloadCounterKey` (bước 8) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenPreloadSlot.kt` | Khe toàn màn hình trong kế hoạch (bước 8) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenLoadContext.kt` | Activity đang mở hay `ApplicationContext` (bước 9) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Nối mục 3.1 bước 10; xóa `preloadWhenEnabled`, `preloadSlot`; trường `loadCounter`; khóa bộ đếm theo vị trí; thêm `setPreloadPlan`; trần 35 giây của `sdkReady` (không đổi `canRequestAds`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdRetryPolicyTest.kt` | Biên của ba hàm (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPreloadPlanTest.kt` | Kế hoạch mặc định, kiểm khai báo |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PreloadPlanRunnerTest.kt` | Thứ tự nhóm, trần 6 giây, nhịp 200 ms, thời gian ảo |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PreloadDecisionTest.kt` | Bảng quyết định S-23, trần 35 giây |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPreloaderTest.kt` | Giữ lượt, chạy một lần, mạng về, nạp bù, `setPlan` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NetworkReturnsTest.kt` | Gộp 3 giây, máy bay lúc mở app |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | 5 giây, nền, cổng lúc gửi, đặt lại khi mạng về, lỗi mã 0, móc `NativeAdEvents` của T-023 tới loader |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenReloadsTest.kt` | 2, 4, 6, 8, 10 giây; về 0 sau thành công; F-06; S-15; khóa theo vị trí |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenPreloadSlotTest.kt` | Vị trí tắt, trạng thái nạp |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenLoadContextTest.kt` | Robolectric: activity đang mở, đang đóng, `null` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakePreloadSlot.kt` | Khe giả: đếm và ghi thời điểm `loadIfNeeded`, xong ở thời điểm test đặt |
| Sửa | `docs/ops/ads-guide.md` | Mục 6: nạp sẵn và nạp lại; mục 7 theo mục 3.1 bước 11 |
| Sửa | `docs/features/ads/tickets/T-021-nap-san-va-nap-lai.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-021-nap-san-va-nap-lai.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã tìm `git grep` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/testStaging`, `*/src/androidTest`, mọi `build.gradle.kts`):

- `AdsController.kt`: `FullScreenAdsTest.kt` dựng thẳng `AdsController`; task không đổi constructor (mọi thứ mới dựng bên trong hay lấy từ tham số T-019, T-020, T-023 đã thêm), nên không sửa test này. `InListAdsTest.kt`, `NativeAdSlotTest.kt` chỉ nhắc tên trong tên test và KDoc, không dựng. `MainActivity.kt`, `AdGalleryActivity.kt` và các composable của `:core:ads` gọi `canRequestAds`, `nativeAd`, `isEnabled`, `gatherConsent`: chữ ký giữ nguyên. Không script hay workflow nào đọc file này.
- `docs/ops/ads-guide.md`: không script hay test nào đọc.
- Không đổi file nào của `:app`, không đổi `TemplateApplication.kt` (câu hỏi 3).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng. Đổi lúc và số lần gửi yêu cầu (điểm chặn 1): nạp sẵn theo nhóm, thử lại sau lỗi theo PARTNER (trước đây interstitial không thử lại, native không thử lại), nạp lại khi mạng về, interstitial nạp sau khi đóng 2 giây thay vì ngay. Thử lại sai có thể gửi quá nhiều yêu cầu và bị SDK tự chặn: trần 10 giây và 60 giây có test biên.
- Sự kiện phân tích: Không chạm. Lượt lỗi mã 0 (O-09) đi qua `AdTelemetry.loadFailed` có sẵn của T-023; không thêm sự kiện hay tham số. Lượt nạp sẵn, thử lại, nạp bù không gọi `AdTelemetry.opportunity`.
- Dữ liệu: không lưu gì trên máy; kế hoạch, lượt giữ, bộ đếm chỉ trong bộ nhớ.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Theo dõi Internet đi qua `InternetConnectivity` của `:core:network` (quyền đã có từ T-016); `Activity.isFinishing`, `isDestroyed` và `SystemClock.elapsedRealtime()` (qua `SystemMonotonicClock`) không cần quyền.
- **Thư viện và SDK:** Không thêm. `InterstitialAd.load(Context, ...)` nhận `Activity` (lớp con của `Context`) không đổi API của `play-services-ads` (`libs.play.services.ads`, đã có trong `:core:ads`); `collectLatest`, `withTimeoutOrNull`, `stateIn` thuộc `kotlinx-coroutines-core` có sẵn qua convention plugin; test dùng `kotlinx-coroutines-test` (`libs.kotlinx.coroutines.test`) và Robolectric (`libs.robolectric`) đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `foregroundState`: plan T-020 nói `AdsActivityTracker` đếm activity ở `started` nhưng không nói rõ trạng thái foreground là `Boolean` hay `StateFlow<Boolean>`; vòng thử lại native cần chờ được giá trị đổi.
- **Điều kiện chuyển:** khi task chạy, `AdsActivityTracker` không có `StateFlow` foreground và `AdsController` không có chỗ nào nhận sự kiện foreground đổi (không có `coordinator.onForegroundChanged` hay callback tương đương) để cập nhật `foregroundState`.
- **Cách làm thay:** `AdsController` tự dựng `foregroundState` từ `onAppForeground()`, `onAppBackground()` mà `MainActivity` vẫn gọi (T-019, T-020 giữ chữ ký), qua `StartedActivityCounter` có sẵn. Chỉ sửa `AdsController.kt`. Test viết trước không đổi (`NativeAdRetryTest` nhận `StateFlow` giả).

## 4. Kiểm thử

Unit test JVM. Thời gian là thời gian ảo của `runTest` (`testScheduler.currentTime`), đồng hồ của lớp là `MonotonicClock { testScheduler.currentTime }`, hàm phụ `at(millis)` như `NativeAdLoadingTest`. `FakePreloadSlot(format, settleAt: Long? = null)` ghi `loadCalls` (thời điểm mỗi lần `loadIfNeeded`) và `settled()` true từ `settleAt`. `FakeNativeAdLoader`, `FakeLoadedNativeAd` có sẵn (T-010). `LoadAttemptCounter` thật của T-023. `RecordingAppLogger`. Vị trí thật `AdPlacement.GALLERY_INTERSTITIAL`, `AdPlacement.GALLERY_NATIVE_FULL_SCREEN` (mã `gallery_native_full_screen`, T-013).

Bước 3 (test viết trước) có khung rỗng để biên dịch: `AdRetryPolicy` trả 0 ở mọi hàm; `defaultPreloadPlan` trả một nhóm gồm mọi định dạng; `AdPreloadPlan` không kiểm gì; `PreloadPlanRunner.run` gọi `loadIfNeeded` của mọi khe trong mọi nhóm ngay rồi trả; `decidePreload` luôn `RUN`; `sdkReadyAt` trả `callbackArrived`; `AdPreloader.request` chạy ngay, không giữ, `nativeRetryResets` không đổi; `networkReturns` phát mỗi lần nhận `true`; `retryingNativeAdStates` gọi `load` một lần, không thử lại; `nativeLoadOnce` gọi `newLoader(null)` và không gọi `newEvents` (bỏ móc, sai có chủ ý); `FullScreenReloads` gọi `loadIfNeeded` ngay và trả `WaitingRetry(0)`; `preloadCounterKey` trả `""`; `FullScreenPreloadSlot.settled` luôn true; `fullScreenLoadContext` luôn trả `application`. Commit test của bước 3: mọi file test mới và `FakePreloadSlot.kt`. Không sửa file test có sẵn (L-025).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-24, tiêu chí 1 (thời gian chờ) | Unit test | `AdRetryPolicyTest.fullScreenDelay_growsBy2sUpTo10s`: n = 1, 2, 3, 4, 5, 6, 100, `Int.MAX_VALUE` ra 2 000, 4 000, 6 000, 8 000, 10 000, 10 000, 10 000, 10 000; n = 0 và -1 ra 2 000. Khung rỗng thất bại vì trả 0 |
| KB-24, tiêu chí 1 (hẹn đúng giờ, không bỏ cuộc) | Unit test, thời gian ảo | `FullScreenReloadsTest.failures_retryAt2_4_6_8_10_thenEvery10s`: `failuresInARow` giả trả 1 tới 7 ở bảy lần lỗi liên tiếp, mỗi lần lỗi ngay khi lượt nạp trước được gọi: `loadIfNeeded(GALLERY_INTERSTITIAL)` được gọi ở 2 000, 6 000, 12 000, 20 000, 30 000, 40 000, 50 000; tại 1 999 chưa gọi. Trạng thái trả về `WaitingRetry(dueAtMillis = now + delay)`. Khung rỗng thất bại vì gọi ngay ở 0 |
| KB-24, tiêu chí 1 (bộ đếm về 0 khi thành công) | Unit test | `FullScreenReloadsTest.successResetsDelay`: dùng `LoadAttemptCounter` thật: gửi, lỗi, gửi, lỗi (chờ 4 000), gửi, thành công, gửi, lỗi: lần chờ cuối là 2 000. Đối chứng (L-055): không có thành công ở giữa thì lần chờ thứ ba là 6 000 |
| KB-24, tiêu chí 1 (một lượt hẹn mỗi vị trí) | Unit test, thời gian ảo | `FullScreenReloadsTest.newFailure_replacesPendingRetry`: lỗi ở 0 (hẹn 2 000), lỗi lần nữa ở 1 000 với n = 2 (hẹn 5 000): `loadIfNeeded` gọi đúng 1 lần, ở 5 000 |
| KB-24, tiêu chí 2 (hai nhánh) | Unit test | `AdRetryPolicyTest.nativeDelay_holdingAd_doublesUpTo60s`: n = 1, 2, 3, 4, 5, 6, 10, 11, `Int.MAX_VALUE` ra 2 000, 4 000, 8 000, 16 000, 32 000, 60 000, 60 000, 60 000, 60 000. `nativeDelay_withoutAd_isAlways5s`: n = 1, 3, 50 ra 5 000. Đối chứng (L-055): cùng n = 1, `holdsAd = true` ra 2 000, khác nhánh 5 000; khung rỗng thất bại ở cả hai |
| KB-24, tiêu chí 2 (khe đang có) | Unit test, thời gian ảo | `NativeAdRetryTest.failureWithoutAd_retriesAfter5s`: foreground true; `loader.fail(3)` ở 0: trạng thái `[Loading, Unavailable]`; số yêu cầu 1 ở 4 999, 2 ở 5 000; `deliver` thì `[Loading, Unavailable, Loaded]` (không có `Loading` thứ hai). `keepsFailing_every5s_neverGivesUp`: lỗi liền 5 lần: yêu cầu ở 0, 5 000, 10 000, 15 000, 20 000, 25 000. Khung rỗng thất bại vì chỉ có 1 yêu cầu |
| KB-24, tiêu chí 2 (O-09) | Unit test, thời gian ảo | `NativeAdRetryTest.prepareFailure_countsAsCode0AndRetries`: `load` lần đầu ném `IllegalStateException`: trạng thái `Unavailable`, log thẻ `Ads` chứa `failed: 0`, không chứa thông báo của lỗi; `load` gọi lần 2 ở 5 000. `FullScreenReloadsTest.prepareFailure_isCode0`: `onLoadFailed(GALLERY_INTERSTITIAL, 0)` hẹn như mọi mã (2 000 với n = 1) |
| KB-24, tiêu chí 3 (gộp 3 giây) | Unit test, thời gian ảo | `NetworkReturnsTest.offlineAtStart_thenOnline_returnsOnceAfter3s` (L-A16): `false` ở 0, `true` ở 1 000: một lần phát ở 4 000, không phát ở 3 999. `flappingWithin3s_isOneReturn`: `false` ở 0, `true` ở 5 000, `false` ở 5 500, `true` ở 6 000: một lần phát ở 9 000. `shortDropUnder3s_isNoReturn`: `true` ở 0, `false` ở 5 000, `true` ở 6 000: không phát tới 20 000. `onlineAtStart_isNoReturn`: `true` ở 0: không phát; đối chứng (L-055): rồi `false` ở 10 000, `true` ở 20 000: phát ở 23 000. Khung rỗng thất bại vì phát ngay khi nhận `true` |
| KB-24, tiêu chí 3 (mọi loại nạp lại) | Unit test, thời gian ảo | `AdPreloaderTest.networkReturn_preloadsAndResetsNativeWait`: kế hoạch đã chạy xong, cổng mở: `onNetworkReturned()`: khe `INTERSTITIAL` và `NATIVE_FULL_SCREEN` giả có thêm một `loadCalls`; `nativeRetryResets.value` tăng 1. `NativeAdRetryTest.networkReturn_cutsWaitShort`: lỗi ở 0, tăng `retryResets` ở 1 000: yêu cầu thứ 2 ở 1 000, không chờ tới 5 000. Khung rỗng thất bại ở `nativeRetryResets` và ở yêu cầu thứ 2 |
| KB-24, tiêu chí 4 | Unit test, thời gian ảo | `NativeAdRetryTest.background_sendsNothingUntilForeground`: lỗi ở 0, `foreground = false` ở 1 000: số yêu cầu vẫn 1 ở 5 000 và 60 000; `foreground = true` ở 70 000: yêu cầu 2 ở 70 000. Đối chứng: foreground giữ true thì yêu cầu 2 ở 5 000. `firstLoad_inBackground_waitsForForeground`: bắt đầu với `foreground = false`: 0 yêu cầu tới khi `true`. `FullScreenReloadsTest.nativeFullScreen_retryWaitsForForeground`: `GALLERY_NATIVE_FULL_SCREEN` lỗi ở 0, nền từ 1 000: `loadIfNeeded` không gọi ở 5 000; lên foreground ở 8 000: gọi ở 8 000 |
| S-15 (chặn lúc gửi) | Unit test, thời gian ảo | `NativeAdRetryTest.gateClosedAtSend_endsWithoutRetry`: lỗi ở 0, `mayRequestNow` false từ 1 000: không yêu cầu nào tới 60 000, log có `request blocked`. Đối chứng: `mayRequestNow` true thì yêu cầu ở 5 000. `FullScreenReloadsTest.sendBlocked_isIdleAndSchedulesNothing`: `onSendBlocked` trả `Idle`, `loadIfNeeded` 0 lần tới 60 000, `failuresInARow` không được đọc |
| Câu hỏi 9 (khóa bộ đếm theo vị trí, L-068) | Unit test | `FullScreenReloadsTest.perPlacementKeys_keepFailuresAndLatencyApart`: `LoadAttemptCounter` thật; `a = preloadCounterKey("a_interstitial")`, `b = preloadCounterKey("b_interstitial")` (dự án mẫu chỉ có một vị trí interstitial, nên test dùng mã vị trí dạng chuỗi); `onRequestSent(a, 0)`, `onRequestSent(b, 100)`, `onFailed(a, 250)` ra `LoadOutcome(1, 250)`, `onLoaded(b, 400)` ra `LoadOutcome(1, 300)`; sau đó `failuresInARow(a) == 1`, `failuresInARow(b) == 0`, và `AdRetryPolicy.fullScreenDelayMillis(failuresInARow(a))` là 2 000. `a != b`, cả hai khác `""`. Đối chứng: cùng chuỗi với một khóa chung `"preload:INTER"` (cách của T-023 phiên bản 2) cho độ trễ 150 rồi `null`, và `failuresInARow` về 0 sau lượt thành công của vị trí kia. Khung rỗng thất bại vì `preloadCounterKey` trả `""` cho cả hai. `FullScreenReloadsTest.twoPlacements_failingTogether_retryOnTheirOwnSchedule`, thời gian ảo: `failuresInARow` đọc từ counter thật theo `preloadCounterKey(it.key)`; `GALLERY_INTERSTITIAL` lỗi 2 lần liên tiếp (hẹn 4 000), `GALLERY_NATIVE_FULL_SCREEN` lỗi 1 lần cùng lúc (hẹn 5 000, luật native): `loadIfNeeded` của mỗi vị trí được gọi đúng một lần, ở đúng giờ của nó. Cách nối khóa trong `AdsController` là ngoại lệ dưới |
| F-06 | Unit test | `AdRetryPolicyTest.reloadAfterClose`: `INTERSTITIAL`, `REWARDED` 2 000; `APP_OPEN`, `NATIVE_FULL_SCREEN` 0; `NATIVE_SMALL` ném `IllegalArgumentException`. `FullScreenReloadsTest.closedInterstitial_reloadsAfter2s`: `onClosedOrShowFailed(GALLERY_INTERSTITIAL)` ở 0: gọi ở 2 000, không ở 1 999; `GALLERY_NATIVE_FULL_SCREEN`: gọi ở 0 |
| S-20, S-22 (thứ tự nhóm) | Unit test, thời gian ảo | `PreloadPlanRunnerTest.nextGroupStartsWhenPreviousSettles`: nhóm 1 `{APP_OPEN}` khe xong ở 500, nhóm 2 `{INTERSTITIAL}`: khe nhóm 2 gọi ở 600 (nhịp 200 ms), chưa gọi ở 400. Khung rỗng thất bại vì gọi ở 0 |
| S-22 (trần 6 giây) | Unit test, thời gian ảo | `PreloadPlanRunnerTest.unsettledGroup_movesOnAfter6s`: nhóm 1 không bao giờ xong: nhóm 2 gọi ở 6 000, chưa gọi ở 5 800. `failedSlot_countsAsSettled`: khe lỗi (xong) ở 300: nhóm 2 ở 400. `settledAtStart_movesOnAtOnce`: khe nhóm 1 đã xong từ đầu: nhóm 2 ở 0. `formatWithoutSlots_isSettled` |
| S-22 (khe ngoài kế hoạch) | Unit test | `PreloadPlanRunnerTest.slotNotInPlan_isNeverLoaded`: khe giả `BANNER` có trong `slotsOf` nhưng không có trong kế hoạch: 0 lần gọi. Đối chứng: khe `INTERSTITIAL` trong kế hoạch: 1 lần |
| HD-02 (kế hoạch mặc định) | Unit test | `AdPreloadPlanTest.default_putsAppOpenAndFirstStepFirst`: `{INTERSTITIAL, APP_OPEN, NATIVE_SMALL, NATIVE_STEP}` ra `[{APP_OPEN, NATIVE_STEP}, {INTERSTITIAL, NATIVE_SMALL}]`. `default_withoutAppOpenOrStep_isOneGroup`. `fromPlacements_coversEveryTemplateFormat`: hợp các nhóm bằng tập `format` của `AdPlacement.entries`. `formatInTwoGroups_isRejected`: `IllegalArgumentException`. `emptyGroups_areDropped`. Khung rỗng thất bại ở bài đầu và bài ném lỗi |
| HD-02 (`:app` đặt kế hoạch) | Unit test, thời gian ảo | `AdPreloaderTest.planSetBeforeFirstRun_isUsed`: `setPlan([{INTERSTITIAL}])` rồi `request(SDK_READY)`: khe `INTERSTITIAL` 1 lần, khe `NATIVE_FULL_SCREEN` 0 lần. Đối chứng: kế hoạch mặc định thì cả hai 1 lần. `planSetWhileRunning_appliesNextRun`: log `warn`, lượt sau theo kế hoạch mới |
| S-21 | Unit test | `PreloadDecisionTest.sdkReady_atCallbackOr35s`: khởi tạo ở 1 000, không callback: false ở 35 999, true ở 36 000; callback ở 2 000: true ở 2 000; `initStartedAt = null`: false ở 10 000 000 kể cả khi có callback (S-19). Khung rỗng thất bại ở 36 000. `AdPreloaderTest.heldUntilSdkReady`: `sdkReady` false: `request(SDK_READY)` không gọi khe; `sdkReady` true rồi `onGateChanged()`: 1 lần |
| S-23 (bảng quyết định) | Unit test | `PreloadDecisionTest.order`: bảng 7 dòng, mỗi dòng chỉ một điều kiện đổi so với dòng `RUN`: SDK chưa khởi tạo ra `IGNORE_SDK_NOT_STARTED` (thắng mọi điều kiện khác), quyền thật "không" ra `IGNORE_CONSENT_DENIED`, chưa từng xin với lý do `NETWORK_BACK` ra `IGNORE_NEVER_REQUESTED` (lý do `SDK_READY` thì không), đang chạy ra `IGNORE_RUNNING`, `sdkReady` false hay `mayRequest` false ra `HOLD`. Khung rỗng thất bại vì luôn `RUN` |
| S-23 (giữ và chạy một lần) | Unit test, thời gian ảo | `AdPreloaderTest.heldWhileGateClosed_runsOnceWhenOpen`: đã chạy kế hoạch một lần; `mayRequest` false: hai lần `request(NETWORK_BACK)`: khe không thêm lần gọi; `mayRequest` true, `onGateChanged()` hai lần: khe thêm đúng 1 lần. `consentDenied_dropsHeldRequest`: đang giữ, rồi `consentDenied` true, `onGateChanged()`, rồi cổng mở lại: không chạy; `request(GATE_REOPENED)` sau đó (S-14) thì chạy. `noHold_beforeSdkStarted`, `noHold_beforeFirstPreload` có đối chứng cùng đầu vào đổi một điều kiện thì chạy (L-055). `noOverlap_whileRunning`: khe nhóm 1 chưa xong, `request` lần nữa: khe không được gọi lần 2 |
| S-09 | Unit test | `AdPreloaderTest.everyRequest_asksConsentAgain`: ba lần `request` với ba lý do: `askConsentAgain` gọi 3 lần, trước lần quyết định (đọc thứ tự qua một danh sách sự kiện chung) |
| S-25 | Unit test, thời gian ảo | `AdPreloaderTest.activityResumed_refillsNativeInPlanOrder`: kế hoạch `[{NATIVE_STEP}, {INTERSTITIAL, NATIVE_SMALL}]` đã chạy xong; `onActivityResumed(false)`: khe `NATIVE_STEP` gọi trước khe `NATIVE_SMALL`, khe `INTERSTITIAL` không được gọi thêm. `activityResumed_skipsWhenNeverFinishedRunningOrFullScreenShowing`: ba trường hợp, mỗi trường hợp 0 lần gọi; đối chứng là trường hợp đầu bài |
| Khe toàn màn hình trong kế hoạch | Unit test | `FullScreenPreloadSlotTest.disabled_neverLoads_andIsSettled`; `enabled_loadsAndSettlesByState`: `Idle` thì gọi `load`; `Loading` thì `settled` false; `Ready`, `WaitingRetry` thì true. Khung rỗng thất bại ở `Loading` |
| Câu hỏi 4 của plan T-034 | Unit test Robolectric | `FullScreenLoadContextTest.resumedActivity_isUsed`: `Robolectric.buildActivity(ComponentActivity::class.java).setup()` trả chính activity đó. `finishingDestroyedOrNull_fallsBackToApplication`: `finish()`, `destroy()`, `null` đều trả `application`. Khung rỗng thất bại ở bài đầu |
| Native giữ móc doanh thu của T-023 (KB-22 tiêu chí 1 và 3 không lùi) | Unit test, thời gian ảo | `NativeAdRetryTest.everyNativeLoad_attachesFreshTelemetryEvents`: `newEvents` giả mỗi lần gọi trả một `mockk<NativeAdEvents>(relaxed = true)` mới và ghi lại; `newLoader` giả ghi lại đối số rồi trả `FakeNativeAdLoader`; `retryingNativeAdStates(load = nativeLoadOnce(newEvents, newLoader, ..., timeoutMillis = 10_000, ...))`, lỗi ở 0, thành công ở 5 000: `newEvents` gọi 2 lần, `newLoader` gọi 2 lần, mỗi lần với đúng đối tượng `newEvents` vừa trả cho lượt đó (không `null`, hai đối tượng khác nhau). Khung rỗng thất bại vì nhận `null`. Cách nối trong `AdsController.nativeAd` (truyền đúng `NativeAdEvents` của T-023) là ngoại lệ dưới, kiểm thêm bằng tay ở bước 5 của kiểm thủ công |
| Hủy khi rời màn | Unit test, thời gian ảo | `NativeAdRetryTest.loadedAd_isDestroyedWhenCollectionEnds`: `Loaded` rồi hủy job: `destroyCount == 1`. `cancelDuringWait_sendsNothing` (L-061, vị trí tắt giữa chừng làm khe rời màn): lỗi ở 0, hủy ở 1 000: 1 yêu cầu tới 60 000 |
| Nối trong `AdsController`, app khởi động | Build và test có sẵn | `scripts/check.sh` biên dịch `:core:ads` và Hilt của `:app` (constructor không đổi); `FullScreenAdsTest`, `NativeAdSlotTest` có sẵn qua; `AppLaunchTest` trên emulator của CI |
| Trên máy (staging) | Kiểm tra thủ công | 1) Đồng ý sẵn, bật chế độ máy bay, mở app, mở màn xem thử: log `VGL.Ads` có `Preload SDK_READY` (hoặc `SDK ready: 35 s cap` nếu callback không về), các dòng `Retry gallery_interstitial in 2000 ms`, `4000 ms`... không quá `10000 ms`; native mẫu không hiện, log thử lại mỗi 5 giây. 2) Tắt chế độ máy bay: khoảng 3 giây sau có `Network back` và `Preload NETWORK_BACK: RUN`; native ở đáy hiện mà không cần vào lại màn; interstitial sẵn sàng (bấm "Interstitial" thì hiện). 3) Đang mất mạng, bấm nút Home 1 phút: không có dòng gửi native nào trong lúc ở nền; mở lại thì gửi ngay. 4) Đóng interstitial: dòng nạp lại sau khoảng 2 giây. 5) Native mẫu hiện sau một lần thử lại (bước 2): log `VGL.Ads` vẫn có dòng ghi doanh thu của native (dòng `debug` của T-023, định dạng `NATIVE_SMALL`, vị trí `sample_native_small`), thật hay dự phòng 0 sau 2 giây. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt` (trường mới, `onGateOpened`, đồng hồ 35 giây, collector của cổng, mạng và vị trí, `onActivityResumed`, `setPreloadPlan`, lời gọi `InterstitialAd.load` với context mới, `try`/`catch` của O-09, chỗ gọi `FullScreenReloads`, `nativeAd` dùng vòng thử lại, trường `loadCounter` và khóa `preloadCounterKey` thay `preloadSlot`) và `docs/ops/ads-guide.md`. `AdsController` gắn với SDK và không có unit test riêng (như T-019, T-020, T-022, T-023); mọi quyết định nằm trong các lớp và hàm thuần ở trên, đã có test. Task không đổi hàm `suspend` nào mà vỏ app gọi và không sửa `MainActivity`, `AppNavHost`, `TemplateApplication`, nên không cần test dựng vỏ app theo L-051. Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** Không cần (E dưới 2). Các kỹ thuật đều đã có trong dự án: thời gian ảo của `kotlinx-coroutines-test` với `advanceTimeBy` và `runCurrent` (`NativeAdLoadingTest` của T-010), lớp luật thuần nhận thời điểm từ ngoài (`FullScreenAdGuard`, `NetworkDialogController`), chờ ổn định bằng `delay` như `LostAfterDelay` của T-016, `Robolectric.buildActivity` (chạy thử của plan T-020). Không dùng `debounce` (API thử nghiệm). Lời gọi `InterstitialAd.load` giữ nguyên chữ ký, chỉ đổi đối số context.
- **Lệnh:** `scripts/check.sh` chạy trong phiên ở bước kiểm tra như mọi task; không chạy trước khi duyệt vì code T-019, T-020, T-022, T-023 mà task dựa vào chưa có trên nhánh nào.
- **Kết quả:** chưa có. Điểm chưa chắc về tên của plan chạy trước (foreground của `AdsActivityTracker`) có phương án dự phòng ở mục 3.4; tên khác thì dừng theo mục 7.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi lúc và số lần gửi yêu cầu: nạp sẵn, thử lại, nạp lại; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì thử lại sai có thể làm SDK tự chặn yêu cầu và giảm doanh thu.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`) cộng một tài liệu; nhiều lớp mới, thêm API công khai `AdPreloadPlan`, `AdsController.setPreloadPlan` |
| B. Độ rõ của yêu cầu | 1 | Luật có dẫn chứng và hằng số; AI tự quyết vài chi tiết: nhóm của kế hoạch mặc định, native toàn màn hình theo luật native, thử lại native ở khe đang có, "activity chính" (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi luật thời gian có test với thời gian ảo; mạng thật, callback thật của SDK và nền hay foreground trên máy chỉ kiểm bằng tay |
| D. Ảnh hưởng chéo | 3 | Đổi lúc nạp mà mọi định dạng dựa vào; `AdsController.kt` cũng do T-019, T-020, T-022, T-023 sửa ngay trước trong cùng phiên; T-024, T-027 tới T-031 dùng API mới |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: luật thuần với đồng hồ giả, thời gian ảo của coroutine, chờ ổn định bằng `delay` |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (lúc nạp, `sdkReady`, `nativeAd`, nạp interstitial), `docs/ops/ads-guide.md`. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện; test giao diện không có `AdsController`).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **Phụ thuộc:** ticket ghi T-020, T-016; plan còn dùng tên của T-019, T-022, T-023 (câu hỏi 5). Mọi file các plan đó **tạo** mà task này dùng thì chỉ dùng, không sửa, nên không có trong mục 3.2 (L-054).
- **File chung với các plan chạy trước trong cùng phiên (L-054):**

| File | T-019 | T-020 | T-022 | T-023 | T-034 | T-021 (plan này) |
| --- | --- | --- | --- | --- | --- | --- |
| `core/ads/.../AdsController.kt` | Sửa | Sửa | Sửa | Sửa | | Sửa |
| `core/ads/.../AdPlacement.kt` (`AdFormat`) | Sửa | | Sửa | | | Dùng |
| `core/ads/.../AdsFlags.kt` | Tạo | Dùng | Dùng | Dùng | | Dùng (qua `isEnabled`) |
| `core/ads/.../AdsActivityTracker.kt` | | Tạo | Dùng | | | Dùng (foreground, `resumedActivity`) |
| `core/ads/.../ConsentCoordinator.kt` | | Tạo | | | | Dùng (`onPreloadRequested`) |
| `core/network/.../InternetConnectivity.kt` | | Tạo | | Dùng | | Dùng |
| `core/ads/.../MonotonicClock.kt` | | | Tạo | | | Dùng |
| `core/ads/.../FullScreenLoadState.kt` | | | Tạo | Dùng | | Dùng (đặt `WaitingRetry`, `Idle`) |
| `core/ads/.../ActivityResumeSteps.kt` | | | Tạo | | | không sửa (gọi sau `steps.onResumed` trong `AdsController`) |
| `core/ads/.../LoadAttemptCounter.kt` | | | | Tạo | | Dùng (`failuresInARow`) |
| `core/ads/.../AdTelemetry.kt` | | | | Tạo | | Dùng (`loadFailed` cho mã 0, `countedLoad`, `nativeLoadReport`) |
| `core/ads/.../NativeAdLoading.kt` | | | | | | không sửa (T-027 sửa) |
| `core/ads/build.gradle.kts` | | Sửa | | Sửa | Sửa | không sửa |
| `core/ads/src/test/.../FullScreenAdsTest.kt` | Sửa | Sửa | | Sửa | | không sửa (constructor giữ) |
| `docs/ops/ads-guide.md` | Sửa (mục 5, 6) | | Sửa (mục 5, 8) | | Sửa (mục 7, 9) | Sửa (mục 6; mục 7 nếu có dòng của T-034) |

- Task khác có thể xung đột: T-024 dùng `sdkReady` (trần 35 giây), `FullScreenReloads`, `FullScreenPreloadSlot` cho khe app open và đặt app open ở nhóm đầu; T-027 dùng `AdRetryPolicy.nativeDelayMillis(n, holdsAd = true)`, `retryingNativeAdStates` hay thay nó, đăng ký khe native vào kế hoạch (`PreloadSlot`) và nhận `nativeRetryResets`; T-028 đăng ký pool vào kế hoạch; T-029 dùng `fullScreenDelayMillis` cho banner (B-02); T-031 dùng `FullScreenReloads` cho rewarded; T-026 dùng nạp lại sau khi đóng của interstitial.
- Đề xuất thứ tự chạy: sau T-019, T-020, T-022, T-023 (đúng thứ tự của phiên); T-034 trước hay sau đều được (chỉ chung `ads-guide.md`, khác mục). Trước T-024, T-027, T-029.

## 7. Điều kiện dừng riêng

- Tên của plan chạy trước khác khi task chạy, ngoài phần có phương án dự phòng ở mục 3.4: không có `AdsController.mayRequestNow()`, `sdkReady`, `consentPath` với `ConsentPath.DENIED_OR_REQUIRED`, `onGateOpened(firstTime)`, `coordinator.onPreloadRequested()`, `InternetConnectivity.connected`, `AdsActivityTracker.resumedActivity` (T-020); `MonotonicClock`, `FullScreenLoadState.WaitingRetry`, `loadFullScreenAdIfNeeded`, `ActivityResumeSteps` (T-022); `LoadAttemptCounter.failuresInARow`, `AdTelemetry.loadFailed`, `AdTelemetry.countedLoad`, `nativeLoadReport`, `NativeLoadUse`, lớp `NativeAdMetrics` nhận `LoadReport` (T-023 phiên bản 2); `AdFormat.APP_OPEN`, `AdFormat.NATIVE_STEP` (T-019). Dừng, không tự tạo bản thay hay sửa file các plan đó tạo.
- Một trong T-019, T-020, T-022, T-023 chưa merge khi tới lượt task này.
- `needsLoad` của T-022 vẫn coi `WaitingRetry` đã tới hạn là "đang chờ" khi cổng đóng (không theo A 03:30-32), và phải sửa `FullScreenLoadState.kt` để qua.
- Cần đổi constructor của `AdsController` (phải sửa `FullScreenAdsTest.kt`), hay sửa `NativeAdLoading.kt`, `NativeAdSlot.kt`, `CollapsibleNativeAd.kt`, `InListAds.kt`, `ActivityResumeSteps.kt`, `MainActivity.kt`, `TemplateApplication.kt` để qua.
- `NativeAdSlotTest`, `CollapsibleNativeAdTest` hay `NativeAdLoadingTest` có sẵn đỏ sau khi `nativeAd` dùng vòng thử lại.
- Móc của T-023 không ở chỗ plan này chờ: `SdkNativeAdLoader` không nhận `events: NativeAdEvents?`, hay `AdsController.nativeAd` không truyền một `NativeAdEvents` của vị trí vào `SdkNativeAdLoader`. Dừng, không tự dựng móc doanh thu và không viết lại `nativeAd` mà bỏ móc (native sẽ mất `ad_impression_placement` và dự phòng 2 giây).
- Kiểm thủ công thấy log gửi native trong lúc app ở nền, hay khoảng thử lại toàn màn hình vượt 10 giây hoặc ngắn hơn 2 giây.

## 8. Câu hỏi cho người duyệt

1. **Thử lại native ở khe đang có, hay để T-027?** Ticket ghi "không bao gồm từng định dạng dùng luật này", nhưng KB-24 tiêu chí 2, 3, 4 nói về native.
   - Đề xuất của AI: task này bọc `AdsController.nativeAd` bằng `retryingNativeAdStates` (khe `NativeAdSlot`, `CollapsibleNativeAdSlot` hôm nay thử lại sau 5 giây, chỉ ở foreground, gửi lại ngay khi mạng về), không sửa file giao diện nào; nhánh "còn giữ quảng cáo" (2 tới 60 giây) chỉ có test hàm thuần, T-027 dùng khi thêm thay nội dung mỗi 60 giây. Native trong danh sách (`loadNativeAd`) không thử lại cho tới pool của T-028. Phương án khác: chỉ làm hàm thuần, bỏ `NativeAdRetry.kt`; khi đó KB-24 tiêu chí 2 tới 4 cho native chỉ đạt sau T-027.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

2. **Kế hoạch mặc định có mấy nhóm?** S-22 ghi "app open, native của bước đầu luồng ở nhóm đầu"; HD-02 ghi "app open, rồi khe của bước đầu luồng nếu có, rồi các khe còn lại".
   - Đề xuất của AI: hai nhóm, `{APP_OPEN, NATIVE_STEP}` rồi phần còn lại, theo S-22 (cả hai là thứ người dùng thấy đầu tiên). Phương án khác: ba nhóm theo chữ "rồi" của HD-02; khe bước đầu luồng về muộn hơn tối đa 6 giây.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

3. **`:app` có gọi `setPreloadPlan` không?** HD-02 là việc của `:app` "nếu cần".
   - Đề xuất của AI: không sửa `TemplateApplication.kt`; dự án mẫu dùng kế hoạch mặc định suy ra từ danh sách vị trí. `ads-guide.md` mục 6 ghi cách app đặt kế hoạch riêng trong `Application.onCreate`. Phương án khác: thêm một dòng `ads.setPreloadPlan(AdPreloadPlan.fromPlacements())` vào `TemplateApplication` làm ví dụ (thêm file đó vào mục 3.2, không đổi hành vi).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

4. **Native toàn màn hình đứng riêng (`gallery_native_full_screen`) thử lại theo luật nào?** Nó hiện qua cổng toàn màn hình (T-022) nhưng nạp như native (khe `native_fullscreen` của AdsKit là khe native).
   - Đề xuất của AI: nạp và thử lại theo luật native (O-05: 5 giây khi chưa có quảng cáo, không gửi ở nền), nạp lại ngay sau khi đóng (O-01), như hôm nay. Phương án khác: theo F-05 như interstitial (2 tới 10 giây, cả ở nền).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

5. **Ticket T-021 chỉ ghi `depends_on: [T-020, T-016]`, nhưng plan dùng tên của T-019, T-022, T-023** (`AdFormat` mới, `FullScreenLoadState`, `MonotonicClock`, `LoadAttemptCounter`).
   - Đề xuất của AI: thêm T-022, T-023 vào `depends_on` của ticket (T-019 đã là phụ thuộc của T-020), để phiên bỏ qua task này khi một trong hai dừng. Plan này không sửa file nào các plan đó tạo, nên không cần đổi danh sách file.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

6. **"Activity chính hiện lại" của S-25 là activity nào?** `:core:ads` không biết activity nào là chính.
   - Đề xuất của AI: mọi activity của app mà `AdsActivityTracker` báo resume. Bộ theo dõi chỉ tính activity qua `AppActivityFilter` do `:app` cấp (tên lớp bắt đầu bằng gói của `MainActivity`, plan T-020 mục 3.1 bước 4); activity của SDK (`AdActivity` của Google, activity của adapter T-034) bị bỏ qua, không kích nạp bù. Trong dự án mẫu đó là `MainActivity`, và ở staging thêm `AdGalleryActivity` (cũng hiện quảng cáo). Nạp bù vẫn bị chặn khi có quảng cáo toàn màn hình (sửa theo rà soát trước phiên, 2026-10-24).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

7. **S-09 "hỏi xong mới nạp".** Plan T-020 không cho biết lượt hỏi lại đang chạy hay đã xong.
   - Đề xuất của AI: mỗi lần xin nạp sẵn gọi `coordinator.onPreloadRequested()` rồi quyết ngay; nếu lượt hỏi lại đóng cổng (S-10) thì mọi lượt gửi bị `mayRequestNow()` chặn ngay trước khi gửi (S-15), và lượt nạp sẵn chạy lại khi cổng mở. Không thêm tín hiệu mới vào T-020.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

8. **Duyệt chéo khi nhóm chỉ có một dev:** theo risk-scoring.md mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.
   - Đề xuất của AI: như các plan đợt 3 khác.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

9. **(Phiên bản 2) Khóa bộ đếm của lượt nạp sẵn toàn màn hình.** T-023 dùng một khóa cho mọi vị trí cùng định dạng (`preload:INTER`); kế hoạch nạp sẵn nạp mọi vị trí của định dạng cùng lúc.
   - Đề xuất của AI: khóa theo từng vị trí (`preloadCounterKey`), để thời gian chờ thử lại và độ trễ của từng vị trí không trộn nhau. Phương án khác: giữ khóa theo định dạng (đơn giản hơn, sai khi app có từ hai vị trí cùng định dạng).
   - Trả lời: theo đề xuất của AI, khóa theo từng vị trí (anh Nhật, 2026-10-24).

10. **(Phiên bản 2) Trần 35 giây của `sdkReady` áp cho ai?** Bản 1 đổi `canRequestAds` theo `sdkReady`, làm mọi khe chờ callback khởi tạo, tới 35 giây.
    - Đề xuất của AI: `canRequestAds` giữ nguyên; chỉ kế hoạch nạp sẵn chờ `sdkReady` (S-21 chỉ nói về nạp sẵn).
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-24, sau khi anh trả lời các câu ở mục 8 và sau rà soát độc lập trước phiên của RUN-20261024-02 (các điểm nên sửa đã sửa trong plan trước khi duyệt).

Phiên bản 2: **Duyệt**. Người duyệt: Nhật, duyệt chéo: Nhật. AI ghi theo lời anh Nhật, 2026-10-24, theo câu trả lời câu hỏi 9, 10 (khóa bộ đếm theo vị trí; `canRequestAds` giữ nguyên, chỉ nạp sẵn chờ `sdkReady`), sau rà soát độc lập trước phiên của RUN-20261024-03 (các điểm chặn và nên sửa đã sửa trong phiên bản này).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
