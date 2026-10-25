---
ticket: T-024
title: Quảng cáo mở app ở màn loading (lần đầu sau khi cài và lần thường)
status: done       # draft | approved | stopped | done
version: 3                   # 2: rà soát trước phiên RUN-20261026-01, thêm AdsFlagsTest.kt; 3: RUN-20261026-01 dừng ở bước 3, thêm AdUnitIdsTest.kt (mục 3.2, 4, 7, câu hỏi 3, 7)
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 25M          # cỡ M, số gợi ý 15M (scripts/token-usage.py estimate, 2026-10-25); nâng vì khoảng 35 file ở ba module và hai API công khai mới
blocking_points: [ads, core, analytics]
run_order: 1                    # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                  # người duyệt
cross_reviewer: Nhật            # bắt buộc khi risk_level là high
approved_at: 2026-10-25
---

# PLAN T-024: QUẢNG CÁO MỞ APP Ở MÀN LOADING (LẦN ĐẦU SAU KHI CÀI VÀ LẦN THƯỜNG)

## 1. Tóm tắt

**Phiên bản 3 (2026-10-25, phiên RUN-20261026-01 soạn, chờ duyệt).** Bản 2 dừng ở bước 3 (mục 7, gạch 5; `risk-scoring.md` mục 4): thêm hai dòng `OPEN_APP`, `OPEN_APP_INTER` vào `AdPlacement.kt` làm `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` đỏ. Test này chỉ cấu hình mã đơn vị cho `sample_native_small` rồi khẳng định `missing.isEmpty()`; nay `missing` có thêm `open_app`, `open_app_inter`. `AdUnitIdsTest.kt` không có trong mục 3.2. Vì sao plan chưa đủ tốt: phần L-020 của mục 3.2 ghi `AdUnitIdsTest` "qua mà không sửa" mà không chạy thử test đó với hai dòng mới; cùng loại thiếu sót với `AdsFlagsTest` mà rà soát trước phiên đã bắt ở phiên bản 2 (mọi test lặp `AdPlacement.entries` cần được chạy thử, không chỉ đọc). Không có cách sửa code nào vừa đúng ý vừa làm test qua: bỏ vị trí toàn app khỏi `missing` thì bản production không bị đòi mã đơn vị cho hai vị trí mới, trái mục 3.3. Thay đổi so với phiên bản 2: mục 3.1 thêm cách tiếp tục trên nhánh cũ; mục 3.2 thêm `AdUnitIdsTest.kt`; mục 4 thêm cách sửa test đó; mục 7 bỏ `AdUnitIdsTest` khỏi điều kiện dừng; mục 8 thêm câu 7 (ba điểm phiên đã tự quyết); mục 9 thêm chỗ cho phiên bản 3. Cách làm, các file khác, chấm rủi ro không đổi.

Thêm khe app open vào `:core:ads` (hạn 1 giờ, khóa 1 giây sau khi đóng, nạp lại ngay, chỉ bỏ đúng quảng cáo vừa chiếu) và hai vị trí "toàn app" cho lúc mở app: `open_app` (app open) và `open_app_inter` (interstitial bỏ cổng `ads_interval`, vẫn đẩy mốc), bật bằng `show_open_ads_first_open` hay `show_open_ads` theo dấu "đã mở lần đầu" lưu trong DataStore, loại theo `open_ad_type`. Màn loading, sau khi chuẩn bị xong và đủ 2 giây, gọi API công khai mới `StartupAds.awaitStartupAd()`: chờ có giới hạn (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây, nhịp 250 ms), chỉ dò không đo, tự bỏ thì ghi một cơ hội `not_ready`; có quảng cáo thì hiện qua `rememberShowStartupAd()` có đủ cổng của `:core:ads`, đóng rồi mới vào màn chính. Kiểm chứng bằng unit test thời gian ảo cho từng luật thuần, test ViewModel, test Robolectric dựng lại activity và kết thúc từ luồng nền, một kịch bản emulator và kiểm thủ công trên máy thật.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed`, xác nhận lại 2026-10-24; câu hỏi 11, 12 đã trả lời):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-13 | Khi là lần mở đầu sau khi cài và người vận hành bật quảng cáo lần đầu thì màn loading hiện quảng cáo mở app nếu quảng cáo về kịp. | A-01: process này là lần mở đầu khi DataStore chưa có dấu `ads_first_open_done`; khi đó khóa là `show_open_ads_first_open`. Dấu được đọc rồi ghi ngay một lần mỗi process, lúc `:core:ads` khởi động (`AdsController.start`, HD-01), nên mọi lượt đọc trong process thấy cùng kết quả (câu hỏi 6). "Về kịp" theo A-03 (dòng tiêu chí 4). |
| KB-13 | Khi là lần mở thứ hai trở đi và người vận hành bật quảng cáo mở app thì màn loading hiện quảng cáo mở app nếu quảng cáo về kịp. | A-01: có dấu thì khóa là `show_open_ads`. Hai khóa độc lập: lần đầu chỉ đọc khóa lần đầu, lần sau chỉ đọc khóa lần thường. Công tắc tổng `is_show_ads` đã nằm trong `AdsGlobalFlags` (T-019). |
| KB-13 | Khi người vận hành chọn loại "inter" thì lúc mở app hiện interstitial thay cho quảng cáo mở app. | A-02: `open_ad_type` đúng bằng `inter` (`OpenAdType.of`, T-019) thì dùng vị trí `open_app_inter` (interstitial, `ignoreInterval = true`, `recordInterval = true`: bỏ cổng khi đọc, vẫn đẩy mốc khi đóng); giá trị khác dùng `open_app` (app open). Chỉ vị trí được chọn được nạp sẵn. |
| KB-13 | Khi quảng cáo không về trong thời gian chờ (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây) thì app vào màn chính, không hiện gì. | A-03, A-04, A-06: ba hạn tính từ lúc `:core:ads` khởi động (cùng lúc mở app, HD-01): đồng ý có kết quả cuối trước 10 giây, SDK sẵn sàng trước 35 giây, quảng cáo nạp xong trước 40 giây; kiểm mỗi 250 ms (câu hỏi 4). Trong lúc chờ chỉ gọi "đã sẵn sàng chưa" và "nạp nếu cần" (không ghi cơ hội, không gắn vị trí). Đồng ý chưa xong hay cổng đóng thì bỏ, không ghi gì (cổng đóng không phải cơ hội, mục 8.2). SDK hay quảng cáo không kịp thì ghi đúng một `ad_opportunity` `not_ready` cho vị trí được chọn. Quảng cáo đã sẵn sàng thì hiện ngay, kể cả khi đã quá hạn (ví dụ người dùng bấm "Thử lại" sau một phút). |
| KB-13 | Khi khóa tắt thì màn loading không chờ quảng cáo. | Khóa của lần mở này tắt (hay công tắc tổng tắt) thì `awaitStartupAd()` trả `null` ngay, không chờ đồng ý hay SDK; màn loading vào màn chính đúng như trước (đủ 2 giây). Khóa tắt giữa lúc chờ (Remote Config đổi) thì cũng trả `null` ngay ở nhịp kế tiếp, không ghi cơ hội (L-061). |

Luật trong phạm vi ticket mà không có tiêu chí riêng trong spec, vẫn có test: A-05 (hiện qua hàm có cổng đồng ý, chồng nhau, hồi phục kẹt), A-07 (hết hạn từ đủ 1 giờ sau khi nạp), A-08 (không hiện app open trong 1 giây sau lần app open trước đóng; ra `THROTTLED`, AS-01), A-09 (đóng hay hiện lỗi thì nạp lại ngay, chỉ bỏ đúng quảng cáo vừa chiếu), HD-06, I-02 (app open không đẩy mốc `ads_interval`), PL-03 (doanh thu ghi vị trí của lượt hiện).

Không thuộc task này: quảng cáo khi quay lại app (T-025, sẽ dùng khe app open của task này với vị trí `resume`); combo native sau inter lúc mở app (T-026; giá trị "đã hiện inter" mà task này trả về để dành cho nó, hôm nay màn loading chỉ ghi log); mã đơn vị theo khe (câu hỏi 14 của spec, cần sửa `scripts/release.sh`, việc của người).

**Mâu thuẫn tài liệu:** spec `features/loading/spec.md` (confirmed) mục 5 ghi "Màn hình loading không hiện quảng cáo" và KB-1 ghi chuyển sang màn chính ngay khi xong 4 việc và đủ 2 giây; spec ads (confirmed sau, 2026-10-23) mục 4, 5 và KB-13 đặt quảng cáo mở app ở màn loading. Không tự chọn bên nào: câu hỏi 1.

Thiết kế tham chiếu: Không có thiết kế riêng. Màn loading giữ nguyên giao diện (đứng ở 100% trong lúc chờ quảng cáo); quảng cáo do SDK vẽ (docs/ops/design-workflow.md).

ADR liên quan: ADR-0018 (giới hạn chờ của màn loading; câu hỏi 12 của spec: thêm ghi chú), ADR-0026 (hành vi PARTNER), ADR-0013 (phần còn hiệu lực), ADR-0014 (mặc định tắt), ADR-0019 (log), ADR-0023 (khóa Remote Config), ADR-0001 (feature chỉ phụ thuộc core).

Dựa trên code đã merge (nhánh chính, commit `bf7a671`), dùng đúng tên: `AdsController.consentFlowFinished`, `sdkReady`, `canRequestAds`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, `decideFullScreenTurn`, `skipTurn`, `beginTurn`, `endTurn`, `FullScreenReloads`, `FullScreenPreloadSlot`, `FreshAdCache`, `AdTelemetry.opportunity`, `loadStateAtDecision`, `AdsGlobalFlags` (`showOpenAds`, `showOpenAdsFirstOpen`, `openAdType`), `OpenAdType`, `AdPreloadPlan.fromPlacements()` (app open ở nhóm đầu).

## 3. Giải pháp

### 3.1. Cách làm

**Tiếp tục (phiên bản 3):** trên nhánh cũ `claude/T-024-app-open-luc-mo-app` (chưa có PR). Nhánh có commit test `891ecec` (bước 3 làm dở): khung rỗng của `:core:ads` và `:feature:loading`, test mới và test sửa của `:core:ads` (34 test mới, 4 test sửa lọc `!appWide`; 29 thất bại đúng lý do, `templatePlan_hasAppOpenFirst` qua ngay đúng như plan), `FakeStartupAds` trong hàm dựng `viewModel()` của `LoadingViewModelTest`. Chưa viết: test mới của `LoadingViewModelTest`, `LoadingRouteTest`, phần sửa `StagingRemoteValuesModuleTest`, `RemoteConfigKeysTest`, sửa `AdUnitIdsTest` (mục 4, phiên bản 3), tệp kịch bản emulator. Phiên cập nhật nhánh theo nhánh chính, làm nốt bước 3 cho các test chưa viết (thất bại đúng lý do trên khung rỗng; test của `:core:ads` đã thấy thất bại không cần làm lại, chỉ chạy lại để chắc vẫn thất bại đúng lý do sau khi gộp nhánh chính), rồi đi tiếp từ bước 4.

Mọi luật mới là hàm hay lớp thuần, nhận thời gian và trạng thái từ ngoài, có unit test JVM với thời gian ảo (như T-021, T-022). `AdsController` nối chúng với SDK. Màn loading chỉ gọi API công khai của `:core:ads`, không gọi SDK, không đọc khóa Remote Config của quảng cáo.

**1. Vị trí "toàn app"** (`AdPlacement.kt`, MC-09):

- `AdPlacement` thêm tham số cuối `val appWide: Boolean = false` ("bật bằng khóa chung của mục 10.1, không có `is_show_<mã>`"); `AdPlacementDeclaration` thêm trường cùng tên; kiểm khai báo: `appWide` chỉ cho `APP_OPEN` và `INTERSTITIAL`, sai thì `IllegalArgumentException`.
- Hai dòng mới, mỗi dòng một dòng (dạng `scripts/release.sh` đọc): `OPEN_APP("open_app", AdFormat.APP_OPEN, appWide = true),` và `OPEN_APP_INTER("open_app_inter", AdFormat.INTERSTITIAL, ignoreInterval = true, appWide = true),` (tên mã: câu hỏi 3).
- `internal val AdFormat.hasFullScreenSlot: Boolean` = `isFullScreen || this == APP_OPEN`: định dạng có khe toàn màn hình nạp sẵn. `isFullScreen` giữ nguyên (định dạng feature gọi qua `rememberShowFullScreenAd`), nên `pushesIntervalMark` vẫn false cho app open (I-02) và app open vẫn không khai báo được `ignoreInterval`, `recordInterval` (test có sẵn giữ nguyên).
- KDoc của `AdFormat.APP_OPEN`: "Shown by the loading screen through [StartupAds] (T-024); resume ads come with T-025".

**2. Khóa** (`AdsSettings.kt`): `keysFor` bỏ `placementEnabled(it)` của vị trí `appWide`, nên bảng khóa sinh tự động (`docs/remote-config-keys.md`, `docs/remote-config-template.json`) không đổi: hai vị trí mới không thêm khóa nào. `StagingRemoteValuesModule.everyAdPlacementEnabled()` lọc `!it.appWide` (staging đã bật `show_open_ads`, `show_open_ads_first_open`).

**3. Chọn vị trí lúc mở app** (`StartupAdChoice.kt`, mới, A-01, A-02):

```kotlin
/** A-01, A-02: the placement shown when the app opens, or null when the key of this open is off. */
internal fun startupAdPlacement(flags: AdsGlobalFlags, firstOpen: Boolean): AdPlacement?

/**
 * The switch of [placement]: for an app-wide placement, whether [startupAdPlacement] chooses it (waits for
 * [firstOpen]); for any other placement, `is_show_ads && is_show_<code>` as before. Every load and show path reads
 * it through AdsController.isEnabled (L-072).
 */
internal fun placementSwitch(placement: AdPlacement, flags: AdsFlags, firstOpen: Flow<Boolean>): Flow<Boolean>
```

`startupAdPlacement`: khóa = `if (firstOpen) flags.showOpenAdsFirstOpen else flags.showOpenAds` (đã gồm `is_show_ads`); tắt thì `null`; bật thì `OPEN_APP_INTER` khi `openAdType == INTERSTITIAL`, còn lại `OPEN_APP`. `AdsController.isEnabled(placement)` thành một dòng gọi `placementSwitch(placement, flags, firstOpen)`. Vị trí toàn app khác thêm sau (T-025 `resume`) sẽ thêm nhánh của mình ở đây.

**4. Dấu "đã mở lần đầu"** (`FirstOpenMark.kt`, mới; DataStore của `:core:data`, đã là phụ thuộc của `:core:ads`):

```kotlin
internal class FirstOpenMark @Inject constructor(private val dataStore: DataStore<Preferences>, private val logger: AppLogger) {
    /** True when this is the first open since install or since the data was cleared; then writes the mark. */
    suspend fun consume(): Boolean
}
```

Khóa DataStore `ads_first_open_done` (boolean, mới, không cần chuyển đổi dữ liệu cũ). Đọc lỗi `IOException`: coi là lần thường (`false`), log cảnh báo. Ghi lỗi `IOException`: vẫn trả `true` cho process này, log cảnh báo (lần sau có thể lại là "lần đầu"). `AdsController` gọi `consume()` một lần, lười, trong `scope` của nó (`firstOpen: Flow<Boolean>` từ một `Deferred` dùng chung), lần đầu có nơi đọc công tắc của vị trí toàn app: collector `collectPreloadTriggers` trong `start()` đọc ngay, nên thực tế là lúc mở app.

**5. Luật của khe app open** (`AppOpenRules.kt`, mới):

```kotlin
/** A-07: an app open ad expires once it is one hour old. */
internal const val APP_OPEN_MAX_AGE_MILLIS = 3_600_000L
/** A-08: no app open within one second after the previous one closed; a negative gap counts as passed (AR-01). */
internal fun appOpenLockPassed(lastClosedAtMillis: Long?, nowMillis: Long): Boolean
```

**6. Chờ quảng cáo lúc mở app** (`StartupAdWait.kt`, mới, A-03, A-04, A-06):

```kotlin
internal interface StartupAdProbe {
    fun nowMillis(): Long
    val consentFinished: Boolean
    val consentOpen: Boolean
    val sdkReady: Boolean
    /** The placement the keys choose now (re-read at every tick, L-061). */
    fun chosen(): AdPlacement?
    fun isReady(placement: AdPlacement): Boolean          // reads only (F-04)
    fun ensureLoading(placement: AdPlacement)              // load if needed (F-04), records nothing (A-04)
    fun recordNotReady(placement: AdPlacement)             // one `ad_opportunity` not_ready (A-06)
}

internal data class StartupAdLimits(
    val consentMillis: Long = 10_000, val sdkMillis: Long = 35_000, val adMillis: Long = 40_000, val tickMillis: Long = 250,
)

/** Returns the placement to show, or null. Deadlines count from [startedAtMillis] (question 4). */
internal suspend fun awaitStartupAd(probe: StartupAdProbe, startedAtMillis: Long, limits: StartupAdLimits = StartupAdLimits()): AdPlacement?
```

Thứ tự trong một nhịp: `chosen()` null thì trả `null` ngay (cả ở lần gọi đầu: không chờ gì, KB-13 tiêu chí 5); có quảng cáo sẵn sàng thì trả vị trí; pha đồng ý: chưa xong mà hết hạn thì `null`; xong mà `!consentOpen` thì `null`; pha SDK: chưa sẵn sàng mà hết hạn thì `recordNotReady`, `null`; pha quảng cáo: hết hạn thì `recordNotReady`, `null`, chưa hết hạn thì `ensureLoading`; rồi `delay(tickMillis)`. Nhịp đầu chạy ngay lúc gọi, các nhịp sau cách nhau đúng `tickMillis`. Mọi kết thúc ghi một dòng log thẻ `Ads` có lý do (`startup ad: key off`, `consent timeout`, `consent closed`, `sdk timeout`, `ad timeout`, `ready`) và thời gian từ lúc khởi động.

**7. API công khai cho màn loading** (`StartupAds.kt`, mới):

```kotlin
/** The ad of the loading screen when the app opens (KB-13). Bound to AdsController by Hilt. */
interface StartupAds {
    /** Waits within the limits of A-03 and returns the turn to show, or null; null at once when the key is off. */
    suspend fun awaitStartupAd(): StartupAdTurn?
}

/** One ad to show when the app opens. Only app-wide placements are accepted. */
class StartupAdTurn(val placement: AdPlacement) { init { require(placement.appWide) } }

/**
 * Returns an action that shows a [StartupAdTurn] through the full-screen gate of :core:ads (A-02, A-05).
 * onFinished(interShown) runs exactly once, on the main thread: at once when nothing shows, else after the ad
 * closes, fails to show, is found stuck or loses its activity. With no ads controller (tests, previews) it runs at
 * once with false.
 */
@Composable
fun rememberShowStartupAd(): (StartupAdTurn, (interShown: Boolean) -> Unit) -> Unit
```

`di/StartupAdsModule.kt` (mới): `@Binds AdsController` thành `StartupAds` (`SingletonComponent`).

**8. Nối vào `AdsController`:**

- Constructor thêm `firstOpenMark: FirstOpenMark` (Hilt tiêm; `FullScreenAdsTest` dựng tay nên sửa theo). Lớp cài `StartupAds`.
- `startedAtMillis` ghi trong `start()` bằng `clock.nowMillis()` (gốc của ba hạn). `awaitStartupAd()` gọi `awaitStartupAd(probe, startedAtMillis)` trên `Dispatchers.Main.immediate`, probe nối: `consentFlowFinished.value`, `canRequestAds.value`, `_sdkReady.value`, vị trí đang chọn (giá trị mới nhất của `placementSwitch`), `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, `telemetry.opportunity(format, placement, "not_ready", opportunityLoadState(placement))`. Trước `start()` (test, preview) trả `null`.
- Khe app open: `loadedAppOpens = FreshAdCache<AppOpenAd>(APP_OPEN_MAX_AGE_MILLIS, onDiscard = { appOpenCells -= it })` (A-07: lượt nạp thay quảng cáo cũ, quảng cáo hết hạn bị bỏ khi đọc). `fullScreenLoadState` thêm nhánh `APP_OPEN` (`hasFresh` thì `Ready`). `fullScreenPlacements()` lọc theo `hasFullScreenSlot`, nên `OPEN_APP` có `FullScreenPreloadSlot` trong kế hoạch (nhóm đầu, HD-02) và được bật tắt theo `isEnabled`.
- `preloadNow`: nhánh `APP_OPEN` gọi `preloadAppOpen(placement)` mới, cùng khuôn với `preloadInterstitial`: `needsLoad`, `unitId`, `isEnabled(placement).first()`, `mayRequestNow()` ngay trước khi gửi (S-15), `telemetry.requestSent`, `AppOpenAd.load(fullScreenLoadContext(...), unitId, adRequest(), callback)`; nạp được thì gắn `setOnPaidEventListener` ghi doanh thu với vị trí đọc lúc sự kiện về (PL-03), `loadSucceeded`; lỗi thì `loadFailed` rồi `fullScreenReloads.onLoadFailed` (F-05); không dựng được yêu cầu thì như lỗi mã 0 (O-09).
- `showAppOpen(activity, placement, onFinished)` (riêng tư): như `showInterstitial`, nhưng cổng thứ năm của `decideFullScreenShow` là `appOpenLockPassed(lastAppOpenClosedAt, now)` thay mốc `ads_interval` (A-08; `THROTTLED`). `SHOW`: `presence.tryBegin`, `loadedAppOpens.take` (A-09: quảng cáo vừa chiếu rời khe ngay, quảng cáo nạp trong lúc chiếu được giữ), `recordShownOpportunity`, `beginTurn`. Callback đóng: dòng đầu ghi `lastAppOpenClosedAt`, không ghi mốc `ads_interval` (I-02), `endTurn(turn, "closed")`, `fullScreenReloads.onClosedOrShowFailed` (nạp lại ngay, F-06). Hiện lỗi: `telemetry.showFailed`, `endTurn(turn, "show_failed")`, nạp lại ngay. Bấm: `telemetry.clicked`.
- `internal fun showStartupAd(activity, turn, onFinished: (Boolean) -> Unit)`: chạy trong `scope` của `AdsController` (không phải scope của composition, để activity bị tạo lại không hủy lượt giữa chừng); activity đang đóng hay đã hủy thì `onFinished(false)` ngay, log; `OPEN_APP` qua `showAppOpen`, `OPEN_APP_INTER` qua `showInterstitial` (bỏ cổng nhờ `ignoreInterval`, đẩy mốc khi đóng); `interShown` là `true` chỉ khi interstitial được gọi hiện (`SHOW`). Hàm hoàn tất chạy đúng một lần (`FullScreenCompletion`), trên luồng chính.
- `FullScreenAds.kt`: `rememberShowFullScreenAd` thêm `require(!placement.appWide)`: feature không gọi được vị trí toàn app; `rememberShowStartupAd` nằm trong `StartupAds.kt`, dùng `LocalAdsController`, `LocalActivity` như `rememberFullScreenAdAction`.

**9. Màn loading** (`:feature:loading`):

- `LoadingViewModel` thêm tham số `startupAds: StartupAds`. Sau khi bốn việc xong và đủ 2 giây (`runPending`, như cũ), gọi `startupAds.awaitStartupAd()` (lỗi không phải hủy thì log cảnh báo, coi như `null`). `null` thì `finish()` như trước. Có lượt thì `uiState` giữ `InProgress(1f)` và `pendingAd: StateFlow<StartupAdTurn?>` có giá trị. Thêm `fun takePendingAd(): StartupAdTurn?` (trả lượt đúng một lần, rồi `null`, để activity tạo lại không hiện lần hai) và `fun onAdFinished(interShown: Boolean)` (log `Startup ad finished, inter shown: <b>`, rồi `finish()`; gọi lần hai không làm gì). `finish()` ghi `loading_completed` một lần và chuyển `Finished` (câu hỏi 5). Cờ `feature_loading_enabled` tắt thì không gọi `awaitStartupAd()` (câu hỏi 2). Chuẩn bị lỗi thì không gọi cho tới khi "Thử lại" xong.
- `LoadingRoute` thêm tham số cuối `showAd: (StartupAdTurn, (Boolean) -> Unit) -> Unit = rememberShowStartupAd()` (như `bottomAd` của `:feature:sample`, để test truyền bản giả). Đọc `pendingAd` bằng `collectAsStateWithLifecycle`; khác `null` thì `LaunchedEffect(pendingAd) { viewModel.takePendingAd()?.let { showAd(it, viewModel::onAdFinished) } }`. Điều hướng giữ nguyên: `Finished` thì `LaunchedEffect` gọi `onFinished` trên luồng chính; `AppNavHost` không đổi.
- `LoadingModule`: không đổi (`StartupAds` do `:core:ads` gắn).

**10. Tài liệu:**

- `docs/adr/0018-loading-waits-for-remote-config.md`: thêm mục mới `## Ghi chú 2026-10-25 (T-024)` sau mục Hệ quả, không sửa chữ cũ, không đổi "Việc AI phải tuân thủ" (nên `docs/foundation.md` không đổi): khi khóa quảng cáo mở app của lần mở này bật, màn loading chờ thêm theo PARTNER (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây, tính từ lúc mở app) sau khi chuẩn bị xong; khóa tắt thì giữ giới hạn của ADR này (câu hỏi 12 của spec ads).
- `docs/ops/ads-guide.md`: mục 3 dòng "App open lúc mở app" ghi vị trí `open_app`, `open_app_inter` thay "(đợt 3)"; mục 6, gạch "Kế hoạch mặc định": bỏ câu "Dự án mẫu hôm nay chỉ có một nhóm", ghi nhóm đầu là `APP_OPEN`; mục 8 thêm cách thử app open trên staging (lần đầu sau khi xóa dữ liệu, lần sau, `open_ad_type = inter` trên Firebase staging, log `startup ad: ...`); mục 9 ghi bản production cần mã đơn vị cho `open_app` và `open_app_inter` (`scripts/release.sh` tự đòi), và app có quảng cáo mở app thì phải bật `feature_loading_enabled`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `appWide`, kiểm khai báo, `OPEN_APP`, `OPEN_APP_INTER`, `hasFullScreenSlot`, KDoc `APP_OPEN` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | `keysFor` bỏ `is_show_<mã>` của vị trí toàn app; KDoc |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAdChoice.kt` | `startupAdPlacement`, `placementSwitch` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FirstOpenMark.kt` | Dấu "đã mở lần đầu" trong DataStore |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AppOpenRules.kt` | A-07, A-08 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAdWait.kt` | `StartupAdProbe`, `StartupAdLimits`, `awaitStartupAd` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAds.kt` | `StartupAds`, `StartupAdTurn`, `rememberShowStartupAd` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/StartupAdsModule.kt` | `@Binds AdsController` thành `StartupAds` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Mục 3.1 bước 4 và 8: khe app open, `showAppOpen`, `showStartupAd`, `awaitStartupAd`, `isEnabled` qua `placementSwitch`, `FirstOpenMark` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAds.kt` | `require(!placement.appWide)` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StartupAdChoiceTest.kt` | A-01, A-02, `placementSwitch` (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FirstOpenMarkTest.kt` | Đọc, ghi, lỗi đọc ghi |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AppOpenRulesTest.kt` | A-07, A-08, A-09 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StartupAdWaitTest.kt` | A-03, A-04, A-06, KB-13 tiêu chí 4, 5 (thời gian ảo) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Test mới của hai vị trí và `appWide`; `intervalAttributes_defaultAndValidated` loại `OPEN_APP_INTER` khỏi vòng lặp, thêm khẳng định riêng cho nó (câu hỏi 3) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | `placementsAreOffByDefault` lọc `!appWide`, test mới: vị trí toàn app không có khóa (câu hỏi 3) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | `configuredUnits_areParsedPerPlacementKey`: chuỗi cấu hình thêm `open_app=unit/3;open_app_inter=unit/4`, khẳng định `missing.isEmpty()` giữ nguyên, có đối chứng: cùng chuỗi không có hai khóa đó thì `missing == listOf(OPEN_APP, OPEN_APP_INTER)` (vị trí toàn app vẫn cần mã đơn vị ở bản production, mục 3.3; phiên bản 3) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | `readsOnlyDeclaredKeys` và `masterSwitchOff_turnsEveryPlacementOff` lọc `!appWide` (vị trí toàn app không có khóa `is_show_<mã>` trong `AdsSettings.all()`), mỗi test có đối chứng: một vị trí không phải toàn app vẫn được đọc và vẫn tắt theo công tắc tổng (câu hỏi 3; phiên bản 2) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPreloadPlanTest.kt` | Test mới: kế hoạch mặc định của dự án mẫu có `APP_OPEN` ở nhóm đầu |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdsTest.kt` | Tham số `firstOpenMark` khi dựng `AdsController`; test `rememberShowStartupAd` không có controller; `rememberShowFullScreenAd` từ chối vị trí toàn app |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/template/feature/loading/LoadingViewModel.kt` | `StartupAds`, `pendingAd`, `takePendingAd`, `onAdFinished`, `finish` |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/template/feature/loading/LoadingScreen.kt` | `LoadingRoute` thêm `showAd`, hiện lượt đang chờ; `LoadingScreen` không đổi |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingViewModelTest.kt` | `FakeStartupAds` trong hàm dựng; test mới ở mục 4 (test có sẵn giữ nguyên ý) |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingRouteTest.kt` | Robolectric: hiện một lần, dựng lại activity, kết thúc từ luồng nền (L-051, L-061) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModule.kt` | `everyAdPlacementEnabled` lọc `!appWide` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModuleTest.kt` | `everyAdPlacement_isShown` lọc `!appWide`, có đối chứng (câu hỏi 3) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` | `everyFeatureFlagAndPlacement_isListed`, `everyPlacementShowKey_isInTableOff` lọc `!appWide`, có đối chứng (câu hỏi 3) |
| Tạo | `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt` | Kịch bản emulator (mục 4) |
| Sửa | `docs/adr/0018-loading-waits-for-remote-config.md` | Thêm mục `## Ghi chú 2026-10-25 (T-024)` (mục 3.1 bước 10) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3, 6, 8, 9 (mục 3.1 bước 10) |
| Sửa | `docs/features/ads/tickets/T-024-app-open-luc-mo-app.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-024-app-open-luc-mo-app.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020; đã `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `build.gradle.kts`, `src/test`, `src/testStaging`, `src/androidTest` trên commit `bf7a671`):

- `AdPlacement.kt`: `scripts/release.sh` đọc dòng vị trí một dòng (`--list-placements`): hai dòng mới viết một dòng, nên lần phát hành sau đòi mã đơn vị `open_app`, `open_app_inter` (đúng ý, mục 3.3); AI không sửa `release.sh`. `scripts/status.py` đọc cùng dạng dòng cho mục "Sẵn sàng phát hành": hai khóa mới hiện ở đó, đúng ý. `scripts/tests/run.sh` dùng file mẫu `scripts/tests/fixtures/release/AdPlacement.kt` và một `AdPlacement.kt` tạm trong thư mục làm việc của test, không đọc file thật. `AdPlacementSourceTest` so dòng đọc được với `AdPlacement.entries` không phải staging: tự gồm hai vị trí mới. Test lặp `AdPlacement.entries`: `AdPlacementTest`, `AdsSettingsTest`, `RemoteConfigKeysTest`, `StagingRemoteValuesModuleTest` (có trong bảng); `AdUnitIdsTest` (`missing` tính từ `entries`, mã thử theo định dạng có cho `APP_OPEN`, `INTERSTITIAL`; số vị trí `gallery_` vẫn 7; phiên bản 3: sai với `configuredUnits_areParsedPerPlacementKey`, nay có trong bảng), `AdPreloadPlanTest.fromPlacements_coversEveryTemplateFormat` (tính từ `entries`), `AdsFlagsTest.masterSwitchOff_turnsEveryPlacementOff` (gọi `AdsFlags.placementEnabled`, hàm không đổi, vẫn đọc `is_show_<mã>` do test đặt) qua mà không sửa.
- `AdsSettings.kt`: `RemoteConfigKeysTest` sinh lại và so `docs/remote-config-keys.md`, `docs/remote-config-template.json`: không khóa nào thêm hay bớt nên hai file không đổi; `AdsSettingsTest` (có trong bảng).
- `AdsController.kt`: `FullScreenAdsTest` dựng tay (có trong bảng); không script nào đọc.
- `FullScreenAds.kt`: `FullScreenAdsTest` (có trong bảng); `AdGalleryScreen.kt`, `feature/sample` gọi `rememberShowFullScreenAd` với vị trí không phải toàn app, không đổi.
- `LoadingViewModel.kt`, `LoadingScreen.kt`: `LoadingViewModelTest` (có trong bảng); `LoadingScreenTest`, `LoadingScreenScreenshotTest` gọi `LoadingScreen` (không đổi), ảnh chuẩn không đổi; `AppLaunchTest` (emulator) chờ `sample_screen`: staging bật khóa mở app, nhưng emulator chưa đồng ý quảng cáo (hộp thoại EEA phủ lên) nên cổng đóng, màn loading không chờ thêm (hạn đồng ý 10 giây, trong giới hạn chờ của test; xem mục 7).
- `StagingRemoteValuesModule.kt`: `StagingRemoteValuesModuleTest` (có trong bảng), `LocalRemoteValuesSourceSetTest` chỉ quét tên qualifier, không đổi.
- `docs/adr/0018-...md`: `scripts/foundation.py` chỉ đọc vế "Việc AI phải tuân thủ" (biểu thức dừng ở dòng `#` kế tiếp), không đổi, nên `docs/foundation.md` không đổi; `scripts/status.py` chỉ đọc mã, tiêu đề, trạng thái. `docs/ops/ads-guide.md`: không script hay test nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm. Dùng cờ có sẵn `feature_loading_enabled`: tắt thì không có quảng cáo mở app (câu hỏi 2).
- Quảng cáo: thêm hai vị trí của dòng "quảng cáo mở app (màn loading)" ở mục 5 của spec ads: `open_app` (app open), `open_app_inter` (interstitial); bật bằng `show_open_ads_first_open`, `show_open_ads`, loại theo `open_ad_type` (khóa đã có từ T-019, không thêm khóa). Điểm chặn 1.
- Sự kiện phân tích: không thêm sự kiện. `ad_opportunity`, `ad_load_*`, `ad_show_fail`, `ad_clicked`, `ad_impression_placement`, `af_ad_revenue` (T-023) có thêm giá trị `ad_placement` `open_app`, `open_app_inter`. `loading_completed` ghi lúc chuyển màn như spec loading, nên `duration_ms` gồm cả thời gian chờ và xem quảng cáo khi có (câu hỏi 5). Điểm chặn 4.
- Dữ liệu: khóa DataStore mới `ads_first_open_done` (boolean; mục 6 của spec ads "dấu đã mở app lần đầu"); không đổi khóa cũ, không cần chuyển đổi.
- File R2: Không chạm.
- Mã đơn vị production: bản phát hành sau cần thêm `open_app=<mã>` và `open_app_inter=<mã>` vào `release.properties` (việc của người, `docs/ops/release.md`); AI không chạm cấu hình production.
- **Quyền truy cập:** Không thêm. `AppOpenAd`, DataStore, `SystemClock` không cần quyền ngoài `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có của Google Mobile Ads.
- **Thư viện và SDK:** Không thêm. `com.google.android.gms.ads.appopen.AppOpenAd` thuộc `play-services-ads` (`libs.play.services.ads`, đã có trên classpath biên dịch của `:core:ads`, kiểm ở mục 4.1); `androidx.datastore.preferences` có qua `api` của `:core:data`, đã là phụ thuộc của `:core:ads` (kiểm ở mục 4.1). `:feature:loading` đã có `:core:ads` qua plugin `template.android.feature`; không thêm `:core:data` cho feature. Test dùng Robolectric, Turbine, `kotlinx-coroutines-test` đã có. Không thêm thư viện nên không cần `:app:assembleStagingRelease` (L-083); `AppOpenAd` cùng AAR với `InterstitialAd` đã qua R8 ở bản release.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `LoadingRouteTest` dựng `LoadingRoute` trong một activity thử khai báo trong file test, tạo lại bằng `Robolectric.buildActivity(...).recreate()` và chạy khung hình bằng `shadowOf(Looper.getMainLooper()).idleFor(...)`, không qua compose test rule. Đã chạy thử qua (mục 4.1), nhưng `LoadingRoute` thật dùng `collectAsStateWithLifecycle` (cần vòng đời `RESUMED`, khác bản thử dùng `collectAsState`).
- **Điều kiện chuyển:** `LoadingRouteTest` không thấy `LaunchedEffect` chạy (bộ đếm `showAd` vẫn 0 sau `idleFor`) hay hết thời gian, sau 2 lần sửa, vì cách chạy khung hình chứ không vì code.
- **Cách làm thay:** cùng ba test, dựng bằng `createAndroidComposeRule<ComponentActivity>()` (activity của rule đã ở `RESUMED`, nên `collectAsStateWithLifecycle` nhận giá trị; không thêm thư viện test nào); "dựng lại activity" thay bằng gỡ rồi đặt lại nội dung với cùng `LoadingViewModel` (`mutableStateOf` bật tắt nhánh chứa `LoadingRoute`), là đúng điều ViewModel giữ qua lần tạo lại. Tiêu chí và tên test giữ nguyên. File không đổi (cùng `LoadingRouteTest.kt`).

## 4. Kiểm thử

Unit test JVM, thời gian ảo của `runTest` (`testScheduler.currentTime` làm đồng hồ, `delay` thật của coroutine); `FakeStartupAdProbe` (lớp trong file test: trạng thái do test đặt theo thời điểm, ghi lại lời gọi `ensureLoading`, `recordNotReady`); `DefaultAdsFlags(FakeRemoteSettings())` có sẵn cho `placementSwitch`; DataStore giả trong bộ nhớ (như `DataStoreCounterRepositoryTest` của `:feature:sample`: một bản thường, một bản ném `IOException` khi đọc, một bản ném khi ghi); `FakeStartupAds` trong `LoadingViewModelTest` (trả một `CompletableDeferred<StartupAdTurn?>` do test hoàn tất, đếm số lần gọi).

Bước 3 (test viết trước) có khung rỗng để biên dịch: hai dòng `AdPlacement` mới và `appWide` (không kiểm khai báo); `hasFullScreenSlot` = `isFullScreen`; `startupAdPlacement` luôn `null`; `placementSwitch` luôn trả `flags.placementEnabled(placement)`; `FirstOpenMark.consume` luôn `false`; `appOpenLockPassed` luôn `true`; `APP_OPEN_MAX_AGE_MILLIS = 0`; `awaitStartupAd` (hàm thuần) trả `null` ngay; `StartupAds`, `StartupAdTurn` (không `require`), `rememberShowStartupAd` trả hành động không làm gì; `AdsController` cài `StartupAds` trả `null`; `LoadingViewModel` nhận `StartupAds` nhưng không gọi, `pendingAd` luôn `null`, `takePendingAd` trả `null`, `onAdFinished` không làm gì; `LoadingRoute` có tham số `showAd` nhưng không gọi. Commit test của bước 3: mọi file test mới, phần sửa của sáu file test có sẵn (kể cả bốn chỗ lọc `!appWide`, câu hỏi 3), hàm dựng `viewModel()` của `LoadingViewModelTest` truyền `FakeStartupAds` (L-025).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-13, tiêu chí 1 (A-01, lần đầu) | Unit test | `StartupAdChoiceTest.firstOpen_readsFirstOpenKeyOnly`: cờ `showOpenAdsFirstOpen = true`, `showOpenAds = false`, `firstOpen = true`: `OPEN_APP`. Đối chứng (L-055): cùng cờ, `firstOpen = false`: `null`. Khung rỗng thất bại ở vế đầu |
| KB-13, tiêu chí 2 (A-01, lần thường) | Unit test | `StartupAdChoiceTest.laterOpen_readsShowOpenAdsOnly`: `showOpenAds = true`, `showOpenAdsFirstOpen = false`, `firstOpen = false`: `OPEN_APP`; `firstOpen = true`: `null`. `masterSwitchOff_isNull`: `AdsGlobalFlags` dựng từ `DefaultAdsFlags` với `is_show_ads = false`, hai khóa bật: `null` cả hai lần |
| KB-13, tiêu chí 1, 2 (dấu lần đầu) | Unit test | `FirstOpenMarkTest.firstConsume_isTrue_thenFalse`: DataStore rỗng: `consume()` true, lần hai false, DataStore có `ads_first_open_done = true`. `existingMark_isFalse`: DataStore có sẵn dấu: false (đối chứng của vế đầu, L-055). `readError_isLaterOpen`: đọc ném `IOException`: false, log `WARN` thẻ `Ads`. `writeError_stillFirstOpen`: ghi ném `IOException`: true, log `WARN`, không ném. Khung rỗng thất bại ở `firstConsume_isTrue_thenFalse` và `writeError_stillFirstOpen` |
| KB-13, tiêu chí 3 (A-02) | Unit test | `StartupAdChoiceTest.interType_choosesOpenAppInter`: `openAdType = INTERSTITIAL`, khóa của lần mở bật: `OPEN_APP_INTER`; `APP_OPEN`: `OPEN_APP`; khóa tắt với `INTERSTITIAL`: `null`. `AdPlacementTest.openAppInter_ignoresIntervalAndPushesMark`: `OPEN_APP_INTER.declaration.readsInterval()` false, `pushesIntervalMark()` true, `format == INTERSTITIAL`, `appWide`. `openApp_neverPushesMark`: `OPEN_APP.declaration.pushesIntervalMark()` false (I-02), `format == APP_OPEN`, `appWide` |
| KB-13, tiêu chí 1 đến 3 (công tắc của mọi đường nạp và hiện, L-072) | Unit test, Turbine | Mọi đường nạp app open (kế hoạch nạp sẵn nhóm đầu với mọi `PreloadReason`, "nạp nếu cần" của màn loading, thử lại F-05, nạp lại sau khi đóng F-06, lượt `NOT_READY`) đều vào `loadFullScreenAdIfNeeded`, rồi `preloadAppOpen`, đọc `isEnabled` trước khi gửi; lượt hiện đọc `isEnabled` trong `decideFullScreenTurn`; `isEnabled` chỉ là `placementSwitch`. `StartupAdChoiceTest.placementSwitch_followsKeysAndFirstOpen`: `firstOpen = flowOf(true)`, `show_open_ads_first_open` bật: `OPEN_APP` ra true, `OPEN_APP_INTER` ra false; đổi `open_ad_type` thành `inter`: `OPEN_APP` ra false, `OPEN_APP_INTER` ra true. `placementSwitch_keyTurnedOff_emitsFalse` (L-061): đang true, tắt `show_open_ads_first_open` giữa chừng: phát false. `placementSwitch_waitsForFirstOpen`: `firstOpen` chưa phát: chưa có giá trị nào trong 1 giây ảo. `placementSwitch_otherPlacements_unchanged`: `SAMPLE_NATIVE_SMALL` theo `is_show_sample_native_small` như `AdsFlags.placementEnabled`. Khung rỗng thất bại vì vị trí toàn app đọc `is_show_open_app` (luôn false) |
| KB-13, tiêu chí 4 (A-03: đồng ý) | Unit test, thời gian ảo | `StartupAdWaitTest.consentNotFinished_givesUpAt10s_recordsNothing`: probe chọn `OPEN_APP`, đồng ý không bao giờ xong: trả `null` ở đúng 10 000 ms, `recordNotReady` 0 lần, `ensureLoading` 0 lần. `consentClosed_givesUpAtOnce_recordsNothing`: đồng ý xong ở 2 000 với cổng đóng: `null` ở 2 000, 0 lần ghi. Khung rỗng thất bại vì trả ở 0 |
| KB-13, tiêu chí 4 (A-03: SDK) | Unit test, thời gian ảo | `StartupAdWaitTest.sdkNotReady_givesUpAt35s_recordsNotReadyOnce`: đồng ý xong ở 1 000 (cổng mở), SDK không bao giờ sẵn sàng: `null` ở 35 000, `recordNotReady(OPEN_APP)` đúng 1 lần |
| KB-13, tiêu chí 4 (A-03, A-04, A-06: quảng cáo) | Unit test, thời gian ảo | `StartupAdWaitTest.adNotReady_givesUpAt40s_probesEveryTick_recordsOnce`: đồng ý ở 1 000, SDK ở 5 000, quảng cáo không bao giờ sẵn sàng: `null` ở 40 000; `ensureLoading` được gọi đúng 140 lần (mỗi nhịp từ 5 000 tới 39 750) và không ghi gì trong lúc dò; `recordNotReady` đúng 1 lần, ở 40 000. `adReady_returnsAtNextTick`: quảng cáo sẵn sàng ở 12 100: trả `OPEN_APP` ở 12 250, `recordNotReady` 0 lần. Bước đối chứng của hai test là cùng probe với thời điểm sẵn sàng khác nhau. Chạy thử nhịp 250 ms trên thời gian ảo ở mục 4.1 |
| KB-13, tiêu chí 4 (đã sẵn sàng thì không chờ) | Unit test, thời gian ảo | `StartupAdWaitTest.readyAfterDeadlines_showsAtOnce`: `startedAtMillis = 0`, gọi ở 60 000 (sau "Thử lại"), quảng cáo đã sẵn sàng: trả `OPEN_APP` ở 60 000. Đối chứng: cùng thời điểm, chưa sẵn sàng: `null` ở 60 000, `recordNotReady` 1 lần |
| KB-13, tiêu chí 5 (khóa tắt thì không chờ) | Unit test, thời gian ảo | `StartupAdWaitTest.keyOff_returnsAtOnce_withoutProbing`: `chosen()` null, đồng ý chưa xong: trả `null` ở 0, không gọi `ensureLoading`, `recordNotReady`. `keyTurnedOffDuringWait_returnsNextTick` (L-061): đang chờ quảng cáo, `chosen()` thành null từ 7 000: trả `null` ở 7 000, 0 lần ghi. Đối chứng: `adNotReady_givesUpAt40s...` ở trên chờ tới 40 000 |
| KB-13, tiêu chí 5 (màn loading) | Unit test | `LoadingViewModelTest.noStartupAd_finishesAtTwoSecondsAsBefore`: `FakeStartupAds` trả `null` ngay: `Finished` ở 2 000, `loading_completed` 1 lần với `duration_ms = 2000`. Các test có sẵn của `LoadingViewModelTest` giữ nguyên ý với bản giả này |
| KB-13, tiêu chí 1 đến 4 (màn loading chờ rồi hiện) | Unit test | `LoadingViewModelTest.startupAd_isAskedOnlyAfterPreparationAndMinDuration`: bốn việc xong ở 500: chưa gọi `awaitStartupAd` trước 2 000, gọi đúng 1 lần ở 2 000. `startupAdTurn_keepsScreenAt100_untilAdFinished`: trả lượt `StartupAdTurn(OPEN_APP)` ở 3 000: `uiState == InProgress(1f)`, `pendingAd` có lượt, chưa ghi `loading_completed`; `onAdFinished(false)` ở 9 000: `Finished`, `loading_completed` 1 lần với `duration_ms = 9000`. `takePendingAd_returnsTurnOnce`: lần đầu trả lượt, lần hai `null`, `pendingAd` thành `null`. `onAdFinished_twice_logsOnce`. `preparationFailed_noStartupAdUntilRetrySucceeds`: một việc lỗi: `Failed`, 0 lần gọi; "Thử lại" xong: 1 lần. `flagOff_neverAsksForStartupAd`: cờ loading tắt: `Finished`, 0 lần gọi. `awaitThrows_finishesWithoutAd`: `awaitStartupAd` ném `IllegalStateException`: `Finished`, log `WARN`. Khung rỗng thất bại vì không gọi `awaitStartupAd` |
| Hiện một lần, dựng lại activity, luồng chính (L-061, L-051) | Unit test Robolectric | `LoadingRouteTest` (activity thử khai báo trong file test, `setContent { AppTheme { LoadingRoute(onFinished, viewModel, showAd = fake) } }` với `LoadingViewModel` thật dùng chung qua các lần tạo, kho chuẩn bị giả xong ngay, `FakeStartupAds` trả lượt ngay, đồng hồ `SystemClock::elapsedRealtime` của Robolectric; không dùng `MainDispatcherRule`, nên `viewModelScope` chạy trên looper chính của Robolectric; `Robolectric.buildActivity(...).setup()`, rồi `shadowOf(Looper.getMainLooper()).idleFor(3, SECONDS)` sau mỗi bước để qua 2 giây tối thiểu): `pendingAd_isShownOnce_thenNavigatesAfterFinish`: `showAd` 1 lần với lượt `OPEN_APP`, `onFinished` 0 lần; gọi hàm hoàn tất: `onFinished` 1 lần. `recreateWhileAdShows_doesNotShowAgain`: sau lần hiện, `controller.recreate()`: `showAd` vẫn 1 lần; gọi hàm hoàn tất (giữ từ lần đầu): `onFinished` 1 lần trên activity mới. `finishFromBackgroundThread_navigatesOnMainThread`: hàm hoàn tất gọi từ `Executors.newSingleThreadExecutor()`: `onFinished` chạy 1 lần, `Looper.myLooper() == Looper.getMainLooper()`. Đối chứng của test đầu: `FakeStartupAds` trả `null`: `showAd` 0 lần, `onFinished` 1 lần. Khung rỗng thất bại vì `showAd` 0 lần. Kỹ thuật đã chạy thử ở mục 4.1 |
| A-02, A-05 (hàm hiện của vỏ app) | Unit test Robolectric | `FullScreenAdsTest.showStartupAd_withoutController_finishesOnceWithFalse`: `rememberShowStartupAd()` không có `LocalAdsController`: hành động gọi `onFinished` đúng 1 lần với `false`. `showFullScreenAd_rejectsAppWidePlacement`: `rememberShowFullScreenAd(OPEN_APP_INTER)` ném `IllegalArgumentException`; đối chứng `GALLERY_INTERSTITIAL` không ném. `AdPlacementTest.startupTurn_acceptsAppWideOnly`: `StartupAdTurn(SAMPLE_NATIVE_SMALL)` ném, `StartupAdTurn(OPEN_APP)` không |
| A-07 | Unit test | `AppOpenRulesTest.appOpen_expiresAtOneHour`: `FreshAdCache(APP_OPEN_MAX_AGE_MILLIS)`: nạp ở 0, `hasFresh` ở 3 599 999 true, ở 3 600 000 false, `take` ở 3 600 000 trả `null` và gọi `onDiscard` 1 lần. Khung rỗng (hạn 0) thất bại ở vế 3 599 999 |
| A-08 | Unit test | `AppOpenRulesTest.lockOfOneSecondAfterClose`: `appOpenLockPassed(null, 0)` true; đóng ở 10 000: ở 10 999 false, ở 11 000 true; ở 9 000 (đồng hồ sau mốc âm, AR-01) true. Khung rỗng thất bại ở 10 999 |
| A-09 | Unit test | `AppOpenRulesTest.adLoadedWhileShowing_isKept`: `put(OPEN_APP, a, 0)`, `take` (lượt hiện) trả `a`; trong lúc chiếu `put(OPEN_APP, b, 5_000)`; lượt đóng không gỡ gì: `hasFresh` ở 6 000 true, `take` trả `b`, `onDiscard` 0 lần. Đây là đúng cách `showAppOpen` dùng khe (lấy ra lúc hiện, không gỡ lúc đóng) |
| Hai vị trí toàn app, khóa (MC-03, MC-09) | Unit test | `AdPlacementTest.appWide_onlyForAppOpenAndInterstitial`: `AdPlacementDeclaration("x", NATIVE_SMALL, appWide = true)` ném; `APP_OPEN`, `INTERSTITIAL` không. `appWidePlacements_areExactlyOpenApp`: `entries.filter { it.appWide }` là `[OPEN_APP, OPEN_APP_INTER]`, không phải staging. `AdsSettingsTest.appWidePlacements_haveNoSwitchKey`: `AdsSettings.all()` không có `is_show_open_app`, `is_show_open_app_inter`; đối chứng: có `is_show_sample_native_small`. Khung rỗng thất bại vì `keysFor` còn hai khóa đó |
| Kế hoạch nạp sẵn (HD-02) | Unit test | `AdPreloadPlanTest.templatePlan_hasAppOpenFirst`: `AdPreloadPlan.fromPlacements().groups.first() == setOf(AdFormat.APP_OPEN)`. Khung rỗng của bước 3 đã có `OPEN_APP` nên test này qua ngay: đối chứng là `default_withoutAppOpenOrStep_isOneGroup` có sẵn (kế hoạch không có app open là một nhóm) |
| Bảng khóa và staging (MC-09) | Unit test | `RemoteConfigKeysTest` sinh lại bảng: không đổi file nào (bảng khóa sinh tự động không có khóa mới); hai test lọc `!appWide` có đối chứng `assertTrue(AdPlacement.entries.any { it.appWide })`. `StagingRemoteValuesModuleTest.appWidePlacements_haveNoLocalValue`: không có giá trị cục bộ `is_show_open_app*` |
| Giao diện màn loading | Screenshot test có sẵn | `LoadingScreenScreenshotTest`: ảnh chuẩn không đổi (`LoadingScreen` không đổi; trong lúc chờ quảng cáo màn hình là trạng thái 100% đã có) |
| Từ chối đồng ý thì không chờ, vào màn chính | Kiểm tra thủ công (emulator: `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt`) | `fresh`, `consent decline 30`, `wait id=sample_screen 60`, `expect_no id=loading_screen`, `shot main`. Staging bật khóa mở app, nên tệp chứng minh màn loading không treo khi cổng đóng. Đã chạy `scripts/device-scenario.py check` (mục 4.1) |
| KB-13, tiêu chí 1 đến 4 trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging (mã thử của Google), đồng ý quảng cáo: 1) Xóa dữ liệu, mở app: màn loading đứng 100% rồi hiện app open thử, đóng thì vào màn chính; log `VGL.Ads` có `startup ad: ready`, `ad_placement=open_app`. 2) Mở lại: lần thường, có app open. 3) Firebase staging đặt `open_ad_type = inter`: interstitial thử; bấm mở inter ở màn xem thử ngay sau đó: log `THROTTLED` (mốc đã đẩy). 4) Firebase staging tắt `show_open_ads`: mở lại, vào màn chính sau khoảng 2 giây, log `startup ad: key off`. 5) Chế độ máy bay khi mở app, bật lại sau 50 giây rồi "Thử lại": không có quảng cáo nếu chưa nạp được, log `ad timeout` hay `sdk timeout`. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.'` |
| Ghi chú ADR-0018, hướng dẫn | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc mục ghi chú mới của ADR-0018 và các mục 3, 6, 8, 9 của `ads-guide.md`; `scripts/foundation.py --check` không báo lệch |
| Vị trí toàn app vẫn cần mã đơn vị ở bản production (mục 3.3; phiên bản 3) | Unit test có sẵn, sửa | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`: chuỗi cấu hình có thêm `open_app`, `open_app_inter` thì `missing` rỗng; đối chứng: không có hai khóa đó thì `missing == listOf(OPEN_APP, OPEN_APP_INTER)`. `placementWithoutUnit_isReportedMissingAndShowsNoAd` không sửa, tự gồm hai vị trí mới. Thất bại trên khung rỗng của bước 3 nếu `missing` bỏ vị trí toàn app; qua với khung rỗng hiện có (`missing` tính từ `entries`): là test chặn hành vi, ghi vào nhật ký |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt` (trường `loadedAppOpens`, `lastAppOpenClosedAt`, `startedAtMillis`, `firstOpen`, `preloadAppOpen` với callback của SDK, `showAppOpen`, `showStartupAd`, probe của `awaitStartupAd`, nhánh `APP_OPEN` của `preloadNow`, `fullScreenLoadState`, `fullScreenPlacements`, `isEnabled` một dòng); `di/StartupAdsModule.kt` (một `@Binds`); phần SDK của `rememberShowStartupAd` (gọi `showStartupAd` khi có controller và activity); KDoc; `docs/adr/0018-...md`, `docs/ops/ads-guide.md`; tệp kịch bản emulator (chạy ở `device-session` cuối phiên, không "thất bại trước" được). `AdsController` gắn với SDK và không có unit test riêng (như T-019 tới T-023); mọi quyết định nằm trong các hàm thuần ở trên. Điều kiện áp cho mọi đường nạp (L-072) nằm ở `placementSwitch` và `needsLoad`, mọi đường đi qua `loadFullScreenAdIfNeeded` (bảng trên). Vỏ app không để trần (L-051): `LoadingRoute` có test Robolectric dựng lại activity và kết thúc từ luồng nền; `AppNavHost`, `MainActivity` không đổi. Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-25, worktree `claude/plan-T-024-app-open` trên commit `bf7a671` (nhánh chính), ba file tạm không commit, đã gỡ hết trước khi commit plan: (1) `core/ads/src/main/.../TrialAppOpen.kt` gọi `AppOpenAd.load(Context, String, AdRequest, AppOpenAd.AppOpenAdLoadCallback)`, `setOnPaidEventListener`, `responseInfo.loadedAdapterResponseInfo`, `fullScreenContentCallback`, `show(Activity)`, và DataStore (`booleanPreferencesKey`, `edit`, `data.first()`) trong source set main của `:core:ads` (L-046); (2) `core/ads/src/test/.../TrialStartupWaitTest.kt`: vòng chờ nhịp 250 ms với hạn trên thời gian ảo của `runTest`; (3) `feature/loading/src/test/.../TrialRecreateTest.kt`: activity thử khai báo trong file test, không có trong manifest, `setContent` trong `onCreate`, `Robolectric.buildActivity(...).setup()`, `idleFor(1 s)`, `recreate()`, một `LaunchedEffect` lấy lượt chờ một lần, và một `StateFlow` đặt từ luồng nền làm `LaunchedEffect` điều hướng.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TrialStartupWaitTest*' :feature:loading:testDebugUnitTest --tests '*TrialRecreateTest*'`, rồi `scripts/check.sh` (`JAVA_HOME` của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`).
- **Kết quả:** lệnh gradle riêng: 2 test, 0 lỗi; `TrialStartupWaitTest` in đúng mốc (đồng ý xong ở 3 100 thì vòng thấy ở 3 250; quảng cáo ở 12 000 thấy ở 12 000; không bao giờ sẵn sàng thì bỏ ở đúng 40 000), nên số mốc trong mục 4 dùng được như ghi. `TrialRecreateTest`: `shows=1` trước và sau `recreate()`, `navigations=1`, điều hướng chạy trên luồng chính khi `StateFlow` được đặt từ luồng nền; activity không khai báo trong manifest vẫn dựng được bằng `Robolectric.buildActivity` (như `AdsActivityTrackerTest`). `scripts/check.sh` (08:36 tới 08:39 UTC, có ba file tạm): "Test JVM: 658 test, 0 lỗi. Ảnh screenshot: 42", "Cổng chất lượng: QUA"; ktlint, lint và build staging qua với `TrialAppOpen.kt` trong source set main, nên `AppOpenAd` và DataStore có trên classpath biên dịch của `:core:ads`. `scripts/device-scenario.py check` với tệp kịch bản tạm có đúng nội dung ở mục 4: thoát 0. Chưa chạy thử: `collectAsStateWithLifecycle` trong activity thử (phương án dự phòng ở mục 3.4), callback thật của `AppOpenAd` (kiểm thủ công trên máy thật). Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (thêm hai vị trí và một định dạng hiện được, điểm 1), sự kiện phân tích (`loading_completed` đổi nghĩa `duration_ms` khi có quảng cáo, `ad_placement` mới trong sự kiện đã có; điểm 4), module core dùng chung `:core:ads` (điểm 7). Khóa DataStore mới không cần chuyển đổi dữ liệu cũ (không chạm điểm 9). Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là quảng cáo ngay lúc mở app.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:loading`, `:app` staging) cùng tài liệu; hai API công khai mới (`StartupAds`, `rememberShowStartupAd`), đổi `AdPlacement` và hàm dựng của `LoadingViewModel` |
| B. Độ rõ của yêu cầu | 2 | Luật có dẫn chứng, nhưng còn câu hỏi ảnh hưởng tới cách làm: mâu thuẫn với spec loading (câu 1), cờ loading tắt (câu 2), mã vị trí và thuộc tính toàn app (câu 3), gốc của ba hạn chờ (câu 4) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi luật thuần, ViewModel và luồng dựng lại có test; callback thật của SDK, thời gian chờ trên máy và việc quảng cáo hiện được chỉ kiểm bằng tay trên máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi lúc mở app của mọi app tạo từ mẫu (màn loading có thể chờ tới 40 giây); `AdsController.kt`, `AdPlacement.kt`, `ads-guide.md` dùng chung; T-025, T-026, T-036 dựa vào khe app open và vị trí toàn app |
| E. Độ mới của kỹ thuật | 2 | `AppOpenAd` và test Robolectric dựng lại activity chứa Compose (không qua compose test rule) lần đầu dùng trong dự án; đã chạy thử với `scripts/check.sh` (mục 4.1) |
| **Tổng** | 12 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `AdPlacement.kt`, `AdsSettings.kt`, `FullScreenAds.kt`, `StagingRemoteValuesModule.kt`, `LoadingViewModel.kt`, `LoadingScreen.kt` (chỉ `LoadingRoute`), ADR-0018 (thêm mục), `ads-guide.md`. Ảnh chuẩn screenshot, `tokens.json`, bảng khóa sinh tự động không đổi.
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ). Sau khi merge, mục "Sẵn sàng phát hành" của trang tự liệt kê `open_app`, `open_app_inter` cần mã đơn vị thật (đúng ý).
- Plan đang mở: T-038 (nhánh `claude/plan-T-038-xoa-luot-giu`, chưa merge) sửa `AdPreloader.kt`, `AdPreloaderTest.kt` (task này không sửa) và `docs/ops/ads-guide.md` mục 6, gạch "Lúc chạy" và gạch log; task này sửa mục 6 gạch "Kế hoạch mặc định" (khác gạch) và các mục 3, 8, 9: merge thường không xung đột, chạy trước hay sau đều được.
- Task sau dùng kết quả của task này (L-054): T-025 thêm vị trí toàn app `resume` và nhánh của nó trong `placementSwitch`, dùng `showAppOpen`, `appOpenLockPassed`, khe app open; T-026 có thể dùng giá trị "đã hiện inter" mà `rememberShowStartupAd` trả cho combo lúc mở app; T-036 thêm nút thử app open ở màn xem thử. Ba task đó ghi Sửa cho `StartupAdChoice.kt`, `AppOpenRules.kt`, `StartupAds.kt`, không tạo lại.
- Đề xuất thứ tự chạy: sau khi câu hỏi 1 đã xử lý (spec loading sửa và xác nhận lại); độc lập với T-038; trước T-025, T-026, T-036.

## 7. Điều kiện dừng riêng

- Spec `features/loading/spec.md` chưa được sửa và xác nhận lại theo câu hỏi 1 khi task chạy: dừng (mâu thuẫn spec, điều kiện 3).
- Tên đã dùng ở mục 2 không còn trên nhánh chính (`consentFlowFinished`, `sdkReady`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, `decideFullScreenTurn`, `FreshAdCache`, `FullScreenReloads`, `AdTelemetry.opportunity`, `AdsGlobalFlags.showOpenAdsFirstOpen`): dừng.
- `AdPlacementSourceTest` đỏ sau khi thêm hai dòng: dừng, không đổi dạng dòng, không sửa `scripts/release.sh`.
- `RemoteConfigKeysTest` báo bảng khóa sinh tự động đổi (`docs/remote-config-keys.md`, `docs/remote-config-template.json`): dừng, vì plan nói hai vị trí mới không thêm khóa.
- Cần sửa ảnh chuẩn nào, `LoadingScreenTest` hay `AppLaunchTest` để qua: dừng. Test có sẵn khác đỏ: xử lý theo ADR-0029 (`risk-scoring.md` mục 4), không dừng. (Phiên bản 3, sửa lúc duyệt 2026-10-25 theo ADR-0029.)
- `AppLaunchTest` (emulator, `ui-test` của CI) đỏ vì màn loading chờ quảng cáo: dừng, không nới thời gian chờ của test.
- Cần thêm `:core:data` hay thư viện vào `:feature:loading`, hay đổi `AppNavHost`, `MainActivity`.

## 8. Câu hỏi cho người duyệt

1. **Spec loading mâu thuẫn với spec ads.** `features/loading/spec.md` (confirmed) mục 5 ghi "Màn hình loading không hiện quảng cáo", KB-1 ghi chuyển màn ngay khi xong bốn việc và đủ 2 giây; spec ads (confirmed sau) KB-13 và câu hỏi 11, 12 đặt quảng cáo mở app ở màn loading.
   - Đề xuất của AI: theo spec ads. Trước khi duyệt plan, AI sửa spec loading trong một PR tài liệu riêng: mục 5 trỏ sang KB-13 của spec ads, KB-1 thêm vế "khi khóa quảng cáo mở app của lần mở này bật thì chờ quảng cáo trong giới hạn của KB-13 rồi mới chuyển", ghi lịch sử, chuyển `changed`; anh xác nhận lại. Plan không sửa spec.
   - Trả lời:

2. **Cờ `feature_loading_enabled` tắt (mặc định ở production) thì có quảng cáo mở app không?** Khi cờ tắt, app vào thẳng màn chính, không có chỗ chờ quảng cáo.
   - Đề xuất của AI: không có quảng cáo mở app khi cờ loading tắt; không chờ, không ghi cơ hội. `ads-guide.md` mục 9 ghi rõ: app muốn quảng cáo mở app phải bật cả `feature_loading_enabled` (Remote Config hay mặc định của app) lẫn `show_open_ads`, `show_open_ads_first_open`. Cách khác (hiện một màn chờ riêng khi cờ loading tắt) trái KB-4 của spec loading.
   - Trả lời:

3. **Mã vị trí và thuộc tính "toàn app".** Spec ads mục 5 có dòng quảng cáo mở app nhưng chưa có mã; MC-09 nói app open là cấu hình toàn app, không có khóa `is_show_<mã>`. Mã đơn vị hôm nay theo vị trí (câu hỏi 14 của spec chưa làm), nên inter lúc mở app cần vị trí interstitial riêng.
   - Đề xuất của AI: hai vị trí `open_app` (app open) và `open_app_inter` (interstitial, bỏ cổng `ads_interval`, vẫn đẩy mốc), thuộc tính mới `appWide = true`: không sinh `is_show_<mã>`, bật theo `show_open_ads_first_open`, `show_open_ads`, `open_ad_type`, feature không gọi được. Bản production cần hai mã đơn vị mới. Vì vậy bốn test có sẵn giả định "mọi vị trí có `is_show_<mã>`" hay "mọi vị trí không bỏ cổng" phải lọc `!appWide` (`AdPlacementTest.intervalAttributes_defaultAndValidated`, `AdsSettingsTest.placementsAreOffByDefault`, `RemoteConfigKeysTest` hai test, `StagingRemoteValuesModuleTest.everyAdPlacement_isShown`, và từ phiên bản 2 `AdsFlagsTest.readsOnlyDeclaredKeys`, `AdsFlagsTest.masterSwitchOff_turnsEveryPlacementOff`), mỗi chỗ có đối chứng; đây là đổi luật theo MC-09, không phải nới test để code qua (như câu hỏi 4 của plan T-022). Xin anh xác nhận tên mã và việc sửa các test này.
   - Trả lời:

4. **Ba hạn chờ (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây) tính từ đâu?** A-03 không nói rõ là cùng tính từ lúc mở app hay mỗi pha tính từ lúc pha trước xong (khi đó tối đa 85 giây).
   - Đề xuất của AI: cùng tính từ lúc `:core:ads` khởi động (lúc mở app), nên màn loading chờ quảng cáo tối đa 40 giây kể từ lúc mở app; ba số tăng dần 10, 35, 40 hợp với cách hiểu này, và trần 35 giây của SDK (S-21) cũng tính từ lúc khởi tạo. Quảng cáo đã sẵn sàng thì hiện ngay dù đã quá hạn. Nếu anh chọn mỗi pha một hạn riêng thì chỉ đổi `awaitStartupAd` và số trong test, file không đổi.
   - Trả lời:

5. **`loading_completed` ghi lúc nào khi có quảng cáo mở app?** Spec loading: ghi khi chuyển sang màn chính, `duration_ms` tới lúc chuyển. Có quảng cáo thì lúc chuyển là sau khi quảng cáo đóng, nên `duration_ms` gồm cả thời gian chờ và xem quảng cáo (đổi nghĩa số liệu đang có).
   - Đề xuất của AI: giữ đúng chữ spec (ghi lúc chuyển, gồm cả quảng cáo), vì sự kiện trả lời "mở app tới màn chính mất bao lâu"; ghi ngày đổi lên dashboard. Cách khác: ghi lúc bốn việc xong và đủ 2 giây (trước quảng cáo), giữ nghĩa cũ của `duration_ms`; khi đó spec loading cần sửa cùng câu hỏi 1.
   - Trả lời:

6. **Dấu "đã mở lần đầu" lưu ở đâu và ghi lúc nào?** A-01 để việc này cho app chủ.
   - Đề xuất của AI: `:core:ads` giữ dấu (DataStore của `:core:data`, khóa `ads_first_open_done`), đọc và ghi một lần mỗi process khi `:core:ads` cần công tắc của vị trí toàn app lần đầu, tức ngay lúc mở app. Màn loading không cần DataStore hay khóa quảng cáo. Hệ quả: lần mở đầu mà app bị tắt trước khi quảng cáo hiện thì lần sau tính là lần thường; ghi dấu lỗi thì lần sau có thể lại là lần đầu.
   - Trả lời:

**Trả lời của anh Nhật (2026-10-25):** câu 1 đến 6: theo đề xuất của AI. Câu 1: theo spec ads; spec loading mục 5 và KB-1 sửa trong PR duyệt danh sách RUN-20261026-01 (lịch sử của spec ghi lại), anh Nhật xác nhận.

7. **(Phiên bản 3) Ba điểm phiên RUN-20261026-01 đã tự quyết ở bước 3, có trên nhánh cũ:**
   - `awaitStartupAd` thêm tham số cuối không bắt buộc `logger: AppLogger? = null`, vì chữ ký ở mục 3.1 không có chỗ ghi log lý do kết thúc mà bước 6 đòi.
   - `di/StartupAdsModule.kt` có ngay trong khung rỗng của commit test, vì thiếu nó thì `:app` không dựng được đồ thị Hilt (file đã có trong mục 3.2).
   - Thêm một test ngoài bảng mục 4: `StartupAdWaitTest.interPlacement_isProbedAndRecorded`.
   - Đề xuất của AI: giữ cả ba. Phương án khác: bỏ tham số `logger` và ghi log ở `AdsController` (phần nối, không có test).
   - Trả lời: theo đề xuất của AI, giữ cả ba (anh Nhật, 2026-10-25).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật ("Duyệt các plan"), 2026-10-25. Câu trả lời ở cuối mục 8. Chạy thứ 3 trong RUN-20261026-01.
- **Phiên bản 3:** Duyệt. Người duyệt: Nhật, duyệt chéo: Nhật, 2026-10-25. AI ghi theo lời anh Nhật ("Duyệt v3 ngay, chạy liền"). Câu hỏi 7: theo đề xuất của AI. Lúc duyệt, mục 7 gạch 5 sửa theo ADR-0029 (cùng PR): test có sẵn khác đỏ thì sửa tiếp, không dừng; ảnh chuẩn, `LoadingScreenTest`, `AppLaunchTest` vẫn dừng. Không rà soát độc lập lại trước phiên, theo quyết định chạy liền. Chạy thứ 1 trong RUN-20261026-02.
- **Phiên bản 2:** rà soát độc lập trước phiên (2026-10-25) thấy `AdsFlagsTest.readsOnlyDeclaredKeys` đỏ khi `keysFor` bỏ khóa của vị trí toàn app (đã chạy thử). Thêm `AdsFlagsTest.kt` vào mục 3.2, bỏ khỏi điều kiện dừng ở mục 7, ghi vào câu hỏi 3. Anh Nhật duyệt lại phiên bản 2, 2026-10-25.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-01, phiên bản plan 2

- **Kết quả:** dừng ở bước 3 (mục 7, gạch 5; `risk-scoring.md` mục 4, điều kiện 1 và 5).
- **Nhánh, PR:** `claude/T-024-app-open-luc-mo-app`, chưa có PR. Commit test `891ecec` (bước 3 làm dở, ghi rõ trong thông điệp). Bước 3 do phiên con làm trong worktree `../task-T-024` (L-052).
- **Đã làm:** khung rỗng của `:core:ads` (8 file main, gồm `di/StartupAdsModule.kt`) và `:feature:loading` (`LoadingViewModel.kt`, `LoadingScreen.kt`); test của `:core:ads`.
- **Test viết trước:** 34 test mới (`StartupAdChoiceTest` 8, `FirstOpenMarkTest` 4, `AppOpenRulesTest` 3, `StartupAdWaitTest` 11, `AdPlacementTest` 5, `AdsSettingsTest` 1, `AdPreloadPlanTest` 1, `FullScreenAdsTest` 2) và 4 test sửa lọc `!appWide` có đối chứng (vẫn qua). 29 thất bại đúng lý do (khẳng định so với khung rỗng); `templatePlan_hasAppOpenFirst` qua ngay đúng như plan. Test viết chặt bằng bước đối chứng: 4. Lỗi thứ 30 là `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (file ngoài mục 3.2): lý do dừng. Chưa viết: test mới của `LoadingViewModelTest`, `LoadingRouteTest`, phần sửa `StagingRemoteValuesModuleTest`, `RemoteConfigKeysTest`, tệp kịch bản emulator. Phiên chính đã đọc `AdUnitIdsTest.kt` và `AdUnitIds.missing` để xác nhận lý do dừng.
- **Rà soát độc lập:** chưa tới bước 6.
- **Kết quả CI:** chưa tới bước 8. Trên máy: `:core:ads:testDebugUnitTest` 444 test, 30 thất bại như trên; `ktlintCheck` của `:core:ads`, `:feature:loading` qua; `:app:assembleStagingDebug` qua.
- **Lệch so với plan:** `awaitStartupAd` thêm tham số `logger: AppLogger? = null`; `di/StartupAdsModule.kt` vào khung rỗng; thêm `StartupAdWaitTest.interPlacement_isProbedAndRecorded` (câu hỏi 7 của phiên bản 3). Ngoài mục 3.2: không có. Phương án dự phòng 3.4: chưa tới.
- **Việc còn lại:** duyệt plan phiên bản 3 (mục 1, 3.1), đưa vào danh sách chạy `RUN-20261026-02` (nháp) để tiếp tục trên nhánh cũ.
- **Cần người xem:** câu hỏi 7 của phiên bản 3; cách sửa `AdUnitIdsTest` ở mục 3.2 và mục 4.

### RUN-20261026-02, phiên bản plan 3

- **Kết quả:** đã merge (PR #170, commit squash `3d4f165`, 2026-10-25 16:01 UTC).
- **Nhánh, PR:** `claude/T-024-app-open-luc-mo-app` (tiếp trên nhánh cũ, gộp nhánh chính `b2ee8a8`), PR #170. Commit test `891ecec` (RUN-20261026-01) và `d4e2cef`, commit code `7ac997d`. Bước 3 đến 5 do phiên con làm trong worktree `../task-T-024` (L-052).
- **Đã làm:** mọi phần của mục 3.1: khe app open và hai vị trí toàn app `open_app`, `open_app_inter` trong `:core:ads`; `StartupAds`, `rememberShowStartupAd`; màn loading chờ và hiện quảng cáo mở app; staging lọc vị trí toàn app; kịch bản emulator; ghi chú ADR-0018 và `ads-guide.md` mục 3, 6, 8, 9.
- **Test viết trước:** lần này thêm 12 test mới (`LoadingViewModelTest` 8, `LoadingRouteTest` 3, `StagingRemoteValuesModuleTest` 1) và sửa 4 test (`StagingRemoteValuesModuleTest.everyAdPlacement_isShown`, `RemoteConfigKeysTest` hai test lọc `!appWide` có đối chứng, `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` như mục 4 phiên bản 3). Cả 12 test mới thất bại đúng lý do trên khung rỗng. Bốn test qua ngay, đều là test chặn hay test lọc: `AdUnitIdsTest` (như plan nói), `flagOff_neverAsksForStartupAd` (đối chứng là các test cờ bật), `everyFeatureFlagAndPlacement_isListed`, `everyAdPlacement_isShown`. Test viết chặt bằng bước đối chứng: 4. 29 test của `:core:ads` từ phiên trước chạy lại vẫn thất bại đúng lý do. Chạy test JVM mọi module trên khung rỗng (ADR-0029): `:core:ads` 29 lỗi, `:feature:loading` 10, `:app` 4 (2 test mới sửa cùng `keysFile_matchesCode`, `templateFile_matchesCode` vì `keysFor` của khung rỗng còn khóa `is_show_open_app*`); không có test cũ nào đỏ vì dữ liệu hay vì hành vi cũ hỏng.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 8 ghi chú; không sửa điểm nào.
  - Nên sửa, chờ người quyết: người dùng rời app (Home) trong lúc màn loading chờ quảng cáo thì vòng chờ vẫn chạy, và lượt quảng cáo hiện khi người dùng quay lại (dưới 1 giờ). Thực chất đó là một quảng cáo khi quay lại app nhưng không theo `show_resum_ads` (KB-14). Spec KB-13 ("khởi động mới") và plan không nói tới trường hợp này. Code làm đúng plan; sửa nó là đổi hành vi quảng cáo ngoài plan và spec, nên không sửa trong phiên (bước 6, điểm mâu thuẫn hay thiếu ở spec). Phương án: chỉ hiện nếu app chưa rời foreground từ lúc gọi `awaitStartupAd`, nếu không thì `onAdFinished(false)`. Test viết trước được trong `LoadingRouteTest`: `pause().stop()` trước khi lượt về, rồi `resume()`, kỳ vọng `showAd` 0 lần, `onFinished` 1 lần. Đề xuất làm cùng T-025.
  - Ghi chú: khóa 1 giây (A-08) không ghi khi lượt app open kết thúc bằng đường hồi phục (kẹt, mất activity), nên ghi ở T-025; KDoc `showInterstitial` ("never when the app opens") và `fullScreenAdShowing` còn chữ cũ; trước vòng chờ, `awaitStartupAd` chờ không giới hạn lúc đọc dấu lần đầu (lỗi DataStore đã được bắt); `interShown` là `false` khi SDK báo lỗi hiện ngay trong lúc gọi `show()`; phần nối SDK không có unit test (ngoại lệ đã duyệt); `AppLaunchTest` có thể chờ lâu nếu hộp thoại đồng ý không tải được trên emulator (lần này `ui-test` xanh); lệch `consentOpen` và sửa bản giả, ghi ở dòng dưới.
- **Kết quả CI:** `pr-check` xanh ở lần đầu (lần chạy 40208942588): `changes`, `build-and-test`, `ui-test` đều qua. Trên máy: `scripts/check.sh` 737 test JVM, 0 lỗi, 42 ảnh screenshot (không ảnh mới hay đổi), Cổng chất lượng: QUA (lần chạy đầu đỏ vì một lỗi ktlint thiếu dòng trống trong `LoadingViewModelTest`, sửa một lần).
- **Lệch so với plan:**
  - Probe đọc `consentOpen` từ `gate.state.value.allowed` thay `canRequestAds.value`, vì `canRequestAds` còn cần SDK đã khởi động. Nếu đọc `canRequestAds` thì vòng chờ sẽ bỏ nhầm ("consent closed") giữa lúc đồng ý xong và lúc SDK sẵn sàng.
  - Vị trí đang chọn được thu từ `start()` vào một `StateFlow`. `awaitStartupAd` chờ giá trị đầu tiên của nó rồi mới chạy vòng chờ, để không trả "key off" khi dấu lần đầu chưa đọc xong.
  - Thêm hằng `APP_OPEN_LOCK_MILLIS`, và đổi `onInterstitialLoadFailed` thành `onFullScreenLoadFailed` dùng chung cho app open. Dòng log là `startup ad: ready (open_app) after N ms`.
  - `ads-guide.md` mục 8 sửa thêm một dòng có sẵn: staging bật mọi vị trí bằng giá trị cục bộ, trừ hai vị trí toàn app.
  - Test viết trước sửa sau commit test: `FakeStartupAds` trong `LoadingViewModelTest` (có từ `891ecec`) đổi `CompletableDeferred(null)` thành `CompletableDeferred(value = null)`. Bản cũ gọi nhầm hàm dựng nhận job cha nên không bao giờ hoàn tất, làm 7 test cũ treo. Đây là lỗi của bản giả, không đổi điều test kiểm (bước 4).
  - Ngoài mục 3.2: không có. Phương án dự phòng 3.4: không dùng (`LoadingRouteTest` qua với `collectAsStateWithLifecycle`).
- **Việc còn lại:** T-025 (quay lại app) dùng khe app open; xét điểm nên sửa ở trên và mốc khóa 1 giây ở đường hồi phục khi làm T-025. Bản phát hành sau cần mã đơn vị `open_app`, `open_app_inter` (việc của người).
- **Cần người xem:** kiểm thủ công trên máy thật (mục 4, dòng "KB-13, tiêu chí 1 đến 4 trên máy"); điểm nên sửa của rà soát (rời app trong lúc chờ); kết quả kịch bản emulator `T-024-mo-app-tu-choi-dong-y.txt` ở `device-session` cuối phiên.
