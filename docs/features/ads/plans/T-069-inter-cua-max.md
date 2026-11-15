---
ticket: T-069
title: Interstitial của MAX qua cổng toàn màn hình chung
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 19.4M        # cỡ M sau khi tách rewarded (scripts/token-usage.py estimate, 2026-11-15)
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật
cross_reviewer: Nhật         # nhóm một dev (risk-scoring.md, mục 3)
approved_at:
lane:                        # để trống: phiên tự chạy
---

# PLAN T-069: INTERSTITIAL CỦA MAX QUA CỔNG TOÀN MÀN HÌNH CHUNG

## 1. Tóm tắt

Interstitial có thêm nền tảng AppLovin MAX. Mỗi lượt hiện vẫn qua đúng một cổng toàn màn hình (đồng ý, công tắc, đang hiện, sẵn sàng, mốc `ads_interval`), rồi chọn nền tảng theo `ads_mediation_order`: nền tảng đứng trước có quảng cáo thì hiện nó, không thì nền tảng sau; một lượt, một cơ hội, một hàm hoàn tất, combo native sau inter như với AdMob. MAX có một khe interstitial dùng chung cho mọi vị trí (một mã đơn vị mỗi định dạng, M-10), nạp sẵn khi MAX có trong thứ tự, thử lại theo F-05, nạp lại 2 giây sau khi đóng (F-06). Inter lúc khởi động (`open_app_inter`) chỉ AdMob; inter khi quay lại app (`resume_inter`) và inter trong app theo thứ tự. Rewarded của MAX tách sang T-106 (anh Nhật chọn ngày 2026-11-15).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31, tiêu chí 1 | Khi nền tảng thứ nhất theo `ads_mediation_order` (mặc định MAX) chưa có quảng cáo cho một lượt hiện và nền tảng thứ hai có thì người dùng thấy quảng cáo của nền tảng thứ hai; một lượt chỉ ghi một cơ hội. | Cho interstitial: sau khi qua cổng, hỏi nền tảng theo thứ tự, hiện cái đầu tiên đã có quảng cáo; một sự kiện cơ hội cho cả lượt |
| KB-31, tiêu chí 2 | Khi `ads_mediation_order` là `admob` hay `max` thì chỉ nền tảng đó được nạp và hiện. | `admob`: khe MAX không nạp, không hiện; `max`: vị trí interstitial (trừ `open_app_inter`) không nạp AdMob, chỉ MAX |
| KB-31, tiêu chí 3 | Quảng cáo mở app lúc khởi động chỉ là app open của AdMob; app open của MAX dùng khi quay lại app. | Mở rộng theo quyết định ngày 2026-11-15 (câu hỏi 41, dòng đổi trong PR này): `open_app_inter` cũng chỉ AdMob; `resume_inter` theo thứ tự |
| KB-31, tiêu chí 5 | MAX và APS chỉ khởi động sau khi cổng đồng ý của app mở; không bao giờ gửi yêu cầu khi chưa có đồng ý. | Mọi lượt gửi của khe MAX hỏi `mayRequestNow()` ngay trước khi gửi (S-15, M-08), như AdMob |
| KB-31, tiêu chí 6 | Mọi luật toàn màn hình (không chồng nhau, khoảng cách `ads_interval`, gỡ kẹt, chặn quảng cáo tự hiện) áp như nhau cho quảng cáo của MAX. | Inter của MAX đi qua `decideFullScreenTurn`, `beginInterstitialTurn` (cờ đang hiện, combo), `onInterstitialClosed` (mốc trước tiên, I-05), F-03, F-08, như AdMob |
| KB-31, tiêu chí 7 (doanh thu) | Doanh thu của MAX ghi ở Firebase và AppsFlyer... | Không thuộc plan này (T-072); inter của MAX chưa ghi doanh thu |

Thiết kế tham chiếu: Không có thiết kế riêng (quảng cáo toàn màn hình do SDK vẽ).

ADR liên quan: ADR-0045 (mục 2.2, 2.3: mỗi lượt hỏi nền tảng thứ nhất rồi thứ hai trong cùng một lượt quyết định; nền tảng có trong thứ tự thì được nạp sẵn; mục 5.5: mã MAX lúc build), ADR-0049 (không), ADR-0031, ADR-0029.

Bài học: L-068 (nhiều yêu cầu cùng lúc của một vị trí), L-072 (mọi đường gửi có test, kể cả lượt nạp sẵn đầu).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu banner của MAX (T-071: ranh giới SDK `MaxBannerSdkView`, bộ đếm tách `.../max`, `maxLoadFailed`, MAX chưa sẵn sàng thì coi như không có quảng cáo) và đường interstitial của AdMob trong `AdsController` (một cổng, một lượt, `FullScreenCompletion`). Phần mới tách ra lớp nhỏ có test JVM (mục 13 của luật); `AdsController` chỉ nối.

**Bước 1. Ranh giới SDK (`MaxInterstitial.kt`, mới).**

```kotlin
internal interface MaxInterstitialListener {
    fun onLoaded()
    fun onLoadFailed(code: Int)
    fun onDisplayed()
    fun onHidden()
    fun onDisplayFailed(code: Int)
    fun onClicked()
}

internal interface MaxInterstitialSdkAd {
    var listener: MaxInterstitialListener?
    fun load()
    fun show(activity: Activity)
    fun destroy()
}
```

`AppLovinMaxInterstitial(adUnitId, context)` bọc `com.applovin.mediation.ads.MaxInterstitialAd(adUnitId, context)`, chuyển `MaxAdListener` thành `MaxInterstitialListener` (`onAdLoadFailed(_, error)` → `onLoadFailed(error.code)`, `onAdDisplayFailed(_, error)` → `onDisplayFailed(error.code)`), `show` gọi `showAd(activity)`. Đối tượng thật chỉ được dựng ở lần nạp đầu, khi `maxReady` đã bật (SDK đã khởi tạo; tránh lỗi dựng khi SDK chưa khởi tạo mà T-071 gặp với `MaxAdView`).

`MaxPlatform` (`MaxSdk.kt`) thêm `fun newInterstitial(adUnitId: String): MaxInterstitialSdkAd`; `AppLovinMaxPlatform` trả `AppLovinMaxInterstitial(adUnitId, context)`. `MaxConfig.MAX_FORMATS` thêm `AdFormat.INTERSTITIAL` (mã khe `interstitial`, M-10); KDoc của `unitFor` đổi theo.

**Bước 2. Khe interstitial của MAX (`MaxInterstitialSlot.kt`, mới).** Một khe cho cả app (một mã đơn vị mỗi định dạng), giữ tối đa một quảng cáo đã nạp:

```kotlin
internal class MaxInterstitialSlot(
    private val scope: CoroutineScope,
    private val clock: MonotonicClock,
    private val unitId: () -> String?,               // maxConfig.unitFor(INTERSTITIAL)
    private val maxReady: StateFlow<Boolean>,
    private val shouldLoad: suspend () -> Boolean,   // MAX trong thứ tự hiệu lực và có vị trí inter (trừ open_app_inter) bật
    private val mayRequestNow: () -> Boolean,        // S-15
    private val newAd: (String) -> MaxInterstitialSdkAd,
    private val telemetry: AdTelemetry,
    private val failuresInARow: () -> Int,           // loadCounter.failuresInARow(SLOT)
    private val logger: AppLogger,
) {
    val state: FullScreenLoadState                   // Ready khi có quảng cáo; Loading; WaitingRetry; Idle
    val lastLoadError: String?                       // nhãn load_state của lỗi gần nhất (giá trị của maxLoadFailed), null khi nạp được
    fun loadIfNeeded()                               // F-04
    fun take(): MaxInterstitialSdkAd?                // lấy ra để hiện; khe về Idle
    fun onClosedOrShowFailed()                       // F-06: nạp lại sau reloadAfterCloseMillis(INTERSTITIAL) = 2 giây
    companion object { const val SLOT = "preload:interstitial/max" }
}
```

- **Mọi đường tới lượt gửi đi qua một hàm riêng `send()`**: lượt nạp do controller gọi (`loadIfNeeded`), lượt chờ `maxReady` (cờ `wanted`), lượt thử lại F-05 khi tới hạn, lượt nạp lại F-06 sau khi đóng hay hiện lỗi. `send()` kiểm theo thứ tự. Phần không treo, đồng bộ: có mã đơn vị; `needsLoad(state, now, gateOpen = true)`; `maxReady` (sai thì `wanted = true`, dừng). Qua thì **đặt `state = Loading` ngay** (trước mọi lời gọi treo, như `preloadInterstitial` đặt `Loading` trước `launch`; L-068: hai lời gọi trong cùng một nhịp chỉ một lượt gửi), rồi `scope.launch`: `shouldLoad()` (treo; sai thì về `Idle`, log `"Interstitial (MAX) not requested: not in order or placements off"`, không thử lại); `mayRequestNow()` (sai thì về `Idle`, log `"Interstitial (MAX) not requested: consent gate closed"`, không thử lại, như `onSendBlocked`). Qua hết thì `telemetry.requestSent(SLOT, now)`, `ad.load()`.
- `wanted`: khe theo dõi `maxReady` trong `scope`; khi `maxReady` bật và `wanted` thì gọi `send()` một lần (lượt nạp sẵn đầu chạy trước khi MAX sẵn sàng, `AdsSdkStart.kt`; L-072). Khe chưa được hỏi lần nào thì `maxReady` bật không gửi.
- Một đối tượng `MaxInterstitialSdkAd` cho khe, dựng lười ở lần gửi đầu bằng `newAd(unitId)`, dùng lại cho mọi lượt nạp.
- `onLoaded`: `state = Ready`, `lastLoadError = null`, `telemetry.loadSucceeded(INTERSTITIAL, SLOT, "preload", now)`, `telemetry.apiCalled(INTERSTITIAL, unitId, null)` (`af_inters_api_called` như đường AdMob), log `"Interstitial (MAX) loaded"`. `onLoadFailed(code)`: `lastLoadError = telemetry.maxLoadFailed(INTERSTITIAL, SLOT, "preload", code, now)`, hẹn thử lại sau `AdRetryPolicy.fullScreenDelayMillis(failuresInARow())` (F-05), `state = WaitingRetry(dueAt)`; tới hạn gọi `send()`. Một lượt hẹn đang chờ (thử lại hay nạp lại): lượt mới thay lượt cũ.
- `take()`: trả quảng cáo khi `state == Ready`, về `Idle`; không thì `null`. Quảng cáo đang giữ khi thứ tự đổi bỏ MAX vẫn nằm trong khe (không hiện, vì controller không chọn MAX), không hủy.
- Không bao giờ ghi mã đơn vị vào log.

**Bước 3. Chọn nền tảng (`FullScreenPlatforms.kt`, mới, hàm thuần).**

```kotlin
/** Platforms an interstitial of [placement] may load and show from, in order (KB-31, question 41). */
internal fun interstitialPlatforms(
    placement: AdPlacement,
    order: MediationOrder,
    hasAdmobUnit: Boolean,
    hasMaxUnit: Boolean,
): List<AdPlatform>

/** The first of [platforms] holding an ad now, or null. */
internal fun readyPlatform(platforms: List<AdPlatform>, admobReady: Boolean, maxReady: Boolean): AdPlatform?

/** The load state of a turn: Ready when a platform holds an ad, else the state of the first platform; Idle when none. */
internal fun turnLoadState(platforms: List<AdPlatform>, admob: FullScreenLoadState, max: FullScreenLoadState): FullScreenLoadState

/** For the preload plan (S-22): Loading while any of [platforms] loads, else [turnLoadState]. */
internal fun preloadSettleState(platforms: List<AdPlatform>, admob: FullScreenLoadState, max: FullScreenLoadState): FullScreenLoadState
```

`interstitialPlatforms`: `placement == AdPlacement.OPEN_APP_INTER` thì `[ADMOB]` (nếu có mã AdMob), bỏ qua thứ tự; còn lại `order.platforms` bỏ nền tảng không có mã (như `bannerPlatforms`).

**Bước 4. Nối vào `AdsController`.** Trạng thái gộp chỉ dùng để quyết định lượt hiện; việc nạp mỗi nền tảng đọc trạng thái của riêng nó.

- Khai báo `maxInterstitials` sau `maxReady` và `maxStarter` (khe theo dõi `maxReady` ngay khi dựng, `scope` là `Main.immediate`). Thứ tự `max` mà không có mã MAX `interstitial`: `platformsOf` rỗng, inter không nạp, không hiện, cơ hội `not_ready` nhãn `idle`, như `bannerPlatforms`; dòng log thiếu mã đã báo.
- Thuộc tính `maxInterstitials = MaxInterstitialSlot(scope, clock, { maxConfig.unitFor(AdFormat.INTERSTITIAL) }, maxReady, shouldLoad = ::maxInterstitialWanted, mayRequestNow = ::mayRequestNow, maxPlatform::newInterstitial, telemetry, { loadCounter.failuresInARow(MaxInterstitialSlot.SLOT) }, logger)`.
- `private suspend fun maxInterstitialWanted(): Boolean`: `AdPlatform.MAX in mediationOrder.value.platforms` và có ít nhất một vị trí trong `fullScreenPlacements()` có định dạng `INTERSTITIAL`, khác `OPEN_APP_INTER`, mà `isEnabled(it).first()`.
- `private fun platformsOf(placement) = interstitialPlatforms(placement, mediationOrder.value, unitIds.of(placement, placement.format) != null, maxConfig.unitFor(INTERSTITIAL) != null)` (đọc `unitIds.of` thẳng, không qua `unitId(...)` vì hàm đó ghi `Log.e` mỗi lần thiếu mã, mà `platformsOf` được đọc mỗi 200 ms).
- `private fun admobInterstitialState(placement)`: `Ready` khi `placement in loadedInterstitials`, không thì `loadStates[placement] ?: Idle` (đúng phần AdMob của `fullScreenLoadState` hôm nay).
- `fullScreenLoadState(placement)`, nhánh `INTERSTITIAL`: `turnLoadState(platformsOf(placement), admobInterstitialState(placement), maxInterstitials.state)`. Nhờ vậy `isFullScreenAdReady`, `decideFullScreenTurn` không đổi chữ ký.
- `opportunityLoadState(placement)`, với inter: nhãn lỗi theo nền tảng đầu của `platformsOf(placement)`: `MAX` thì `maxInterstitials.lastLoadError`, `ADMOB` thì `lastLoadErrors[placement]` như hôm nay (mục 8.2).
- `preloadInterstitial(placement)` đổi dòng kiểm đầu thành `needsLoad(admobInterstitialState(placement), ...)` (không đọc trạng thái gộp).
- `loadFullScreenAdIfNeeded(placement)` và `preloadNow(placement)`, nhánh `INTERSTITIAL`: giữ chỗ chặn sớm `if (!canRequestAds.value) return` của hôm nay (cổng đóng thì không đặt `Loading`, không log, không hủy lượt hẹn, như `needsLoad(..., canRequestAds.value)` hôm nay); không kiểm trạng thái gộp; `ADMOB in platformsOf(p)` thì `preloadInterstitial(p)` (tự kiểm trạng thái AdMob); `MAX in platformsOf(p)` thì `maxInterstitials.loadIfNeeded()` (khe tự kiểm trạng thái, `shouldLoad`, cổng). Thứ tự `max` thì vị trí không nạp AdMob (KB-31, tiêu chí 2); thứ tự hai nền tảng thì cả hai cùng nạp sẵn (ADR-0045, mục 2.3), lượt nạp lại F-06 hay F-07 của nền tảng này không bị trạng thái của nền tảng kia chặn.
- `preloadSlots`: vị trí inter dùng `state = { preloadSettleState(platformsOf(p), admobInterstitialState(p), maxInterstitials.state) }`, để nhóm nạp sẵn chờ cả hai nền tảng (S-22).
- Điểm nối test: `@VisibleForTesting internal var interstitialRequestForTest: ((AdPlacement) -> InterstitialAd?)? = null`, như `nativeFullScreenRequestForTest`; trong `preloadInterstitial`, ngay sau `telemetry.requestSent(slot, ...)`, nếu khác `null` thì gọi nó thay cho `InterstitialAd.load(...)`: trả quảng cáo thì chạy đúng nhánh nạp được (`loadStates -= placement`, `loadedInterstitials[placement] = ad`, số liệu như `onAdLoaded`, không có `responseInfo` nên bỏ dòng `networkLog` và `apiCalled` khi đối tượng giả không có thông tin adapter), trả `null` thì `onInterstitialLoadFailed(placement, slot, loadPlacement, NO_FILL_CODE_FOR_TEST, null)`. Mọi kiểm trước đó giữ nguyên. `putLoadedInterstitialForTest` thêm `loadStates -= placement`, để trạng thái AdMob là `Ready` khi test đặt quảng cáo sẵn.
- `showInterstitialTurn`: sau `beginInterstitialTurn(...)` (cổng đã qua, cờ đã bật, `af_inters_ad_eligible`, combo đã gói), chọn `readyPlatform(platformsOf(placement), placement in loadedInterstitials, maxInterstitials.state == Ready)`:
  - `ADMOB`: đường hôm nay, không đổi.
  - `MAX`: `val ad = maxInterstitials.take()`; `null` thì `onInterstitialLost(placement, turn, loadState)`. Có thì `recordShownOpportunity(placement)`, gắn `ad.listener`: `onDisplayed` → `telemetry.displayed(INTERSTITIAL, mã MAX, null)` và đếm `interDisplayCounter` như AdMob (N-05); `onHidden` → `onInterstitialClosed(placement, turn)` rồi `maxInterstitials.onClosedOrShowFailed()`; `onDisplayFailed(code)` → `onInterstitialShowFailed(placement, turn, code)` rồi `maxInterstitials.onClosedOrShowFailed()`; `onClicked` → `telemetry.clicked(INTERSTITIAL, placement)`. Log `"Interstitial shown (MAX): ${placement.key}"`, rồi `ad.show(activity)`.
  - `null` (cả hai không còn quảng cáo giữa cổng và lúc hiện): `onInterstitialLost`.
- Sau khi đóng hay hiện lỗi có hai lịch nạp lại 2 giây: `fullScreenReloads.onClosedOrShowFailed(placement)` (lịch của vị trí, như hôm nay, đi qua `loadFullScreenAdIfNeeded` nên nạp mọi nền tảng còn thiếu) và `maxInterstitials.onClosedOrShowFailed()` (lịch của khe MAX, cho khe dùng chung khi vị trí vừa hiện không còn MAX trong thứ tự hay đã tắt). Hai lịch cùng mốc; `needsLoad` của mỗi nền tảng chặn gửi trùng.
- Đường khởi động (`showStartupAd`) và đường quay lại (`ResumeAdPort.showInterstitial`) giữ lời gọi `showInterstitialTurn` như cũ; khác nhau chỉ ở `interstitialPlatforms`.
- Gỡ kẹt (F-03), chủ biến mất (F-08), trần 10 phút, combo, mốc: không đổi, vì đi qua `presence`, `FullScreenCompletion`, `onInterstitialClosed` chung. Lỗi hiện của MAX ghi qua `telemetry.showFailed` với mã của MAX; `showFailReasonOf` trả `unknown` cho mã ngoài 0 tới 4 (câu hỏi 2).
- Không thêm lý do nạp sẵn mới. Khe MAX nạp ở lượt nạp sẵn của vị trí interstitial (S-22), cờ `wanted` giữ lượt khi MAX chưa sẵn sàng; cổng mở lại thì `GATE_REOPENED` của kế hoạch nạp sẵn gọi lại. Thứ tự đổi từ `admob` sang có MAX giữa phiên: khe MAX nạp ở lượt `not_ready` kế tiếp (F-07) hay lượt nạp sẵn kế tiếp; chấp nhận, không thêm phản ứng khi khóa đổi.
- Không có bước chờ có giới hạn mới trước khi hiện quảng cáo, nên không cần test đưa activity ra nền.

**Bước 5. Tài liệu.**

- `docs/ops/ads-business-rules.md`: M-10 thêm khe `interstitial` ("một khe interstitial MAX dùng chung mọi vị trí"); luật mới N-06 ở mục 5.1: "Interstitial hỏi nền tảng theo `ads_mediation_order` sau khi qua cổng; `open_app_inter` chỉ AdMob; nền tảng có trong thứ tự thì nạp sẵn; khe MAX dùng chung, thử lại F-05, nạp lại F-06; MAX chưa sẵn sàng thì coi như không có quảng cáo (ADR-0045, mục 2.2, 2.3; spec `ads`, câu hỏi 41)"; dòng khóa `ads_mediation_order` (mục 10) bỏ "interstitial ... của MAX chưa có", ghi interstitial đã theo thứ tự, rewarded và app open khi quay lại chưa.
- `docs/ops/ads-guide.md`, mục 7, tiểu mục MAX: thêm một gạch "Interstitial của MAX (N-06, M-10)": khe dùng chung, mã `interstitial` trong `-PmaxAdUnits` / `ads.maxAdUnits`, log `Interstitial (MAX) loaded`, `Interstitial shown (MAX)`, cách thử trên staging (khóa và mã trong `local.properties`, `ads_mediation_order`).
- `docs/ops/ads-guide.md`: câu "phễu của MAX ... chưa có" ở gạch banner và MREC của MAX sửa theo: inter của MAX có phễu `af_inters_*`.
- `docs/ops/release.md`, mục 1.2: dòng mã đơn vị MAX đã có `interstitial`, không đổi.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxInterstitial.kt` | Bước 1: `MaxInterstitialListener`, `MaxInterstitialSdkAd`, `AppLovinMaxInterstitial` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxInterstitialSlot.kt` | Bước 2 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenPlatforms.kt` | Bước 3 |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxSdk.kt` | `MAX_FORMATS` thêm `INTERSTITIAL`; `MaxPlatform.newInterstitial`; `AppLovinMaxPlatform` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeMaxInterstitialSdkAd.kt` | Bản giả: ghi `load`, `show`, `destroy`; `emitLoaded`, `emitLoadFailed(code)`, `emitDisplayed`, `emitHidden`, `emitDisplayFailed(code)`, `emitClicked` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeMaxPlatform.kt` | `newInterstitial` trả `FakeMaxInterstitialSdkAd`, giữ danh sách đã dựng (`interstitials`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxInterstitialSlotTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenPlatformsTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerMaxInterstitialTest.kt` | Mục 4 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerMaxTest.kt` | Dữ liệu chuỗi log thiếu mã (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxSdkStarterTest.kt` | Câu viết lại của `maxConfig_unitFor_bannerAndMrecOnly_noKeyNoUnit` (mục 3.5) |
| Sửa | `docs/ops/ads-business-rules.md` | M-10, N-06, dòng khóa `ads_mediation_order` |
| Sửa | `docs/ops/ads-guide.md` | Mục 7, gạch interstitial của MAX |

Đã sửa trong PR của plan, không phải việc của task: spec `ads` câu hỏi 41 (dòng đổi, `changed`, chờ xác nhận), ticket T-069 (đổi tên file thành `T-069-inter-cua-max.md`, phạm vi chỉ interstitial), ticket mới T-106 (rewarded của MAX).

Nơi đọc các file bị sửa (`grep -rn` trong `scripts/ .github/ */src/test`):

- `MaxSdk.kt` (`MaxConfig`, `MAX_FORMATS`): `AdsController.missingMaxUnits` (dòng log thiếu mã, nay kể thêm `interstitial`; test `AdsControllerMaxTest.start_keyWithoutBannerUnits_logsMissingOnce_noUnitInLog` trong bảng), `MaxConfig.toString` (đếm khe có mã; `MaxSdkStarterTest.maxConfig_unitFor_bannerAndMrecOnly_noKeyNoUnit` so `units=2`, trong bảng và mục 3.5), `app/src/main/kotlin/com/vglstudio/template/di/MaxConfigModule.kt` (chỉ dựng, không đổi).
- `MaxPlatform`: `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/MaxPlatformModule.kt` dựng `AppLovinMaxPlatform` (không đổi), `FakeMaxPlatform` (trong bảng).
- `AdsController.kt`: mọi test `AdsController*` dựng qua `testAdsController` (`maxConfig = MaxConfig.NONE` mặc định: thứ tự hiệu lực là `admob`, `interstitialPlatforms` trả `[ADMOB]`, đường AdMob không đổi).
- `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`: chú thích trong test `:core:ads`, `:core:analytics` (`core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`) chỉ dẫn mã luật (`grep` mọi dòng nhắc hai file đều là chú thích, không đọc file); plan thêm N-06, sửa M-10, không đổi mã luật nào. `scripts/history-refs.py` quét hai tài liệu (không ghi mã ticket); `scripts/ads-plan.py` đọc `docs/remote-config-keys.md`, không đọc hai tài liệu này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có (thứ tự từ khóa `ads_mediation_order` có sẵn)
- Quảng cáo: không thêm vị trí; interstitial có thêm nền tảng MAX (KB-31, câu hỏi 41, 42)
- Sự kiện phân tích: dùng sự kiện có sẵn cho lượt nạp, cơ hội, `af_inters_api_called`, hiện, hiện lỗi, bấm của MAX (cùng tên, như banner MAX ở T-071); không thêm sự kiện; doanh thu MAX là T-072
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `com.applovin.mediation.ads.MaxInterstitialAd`, `MaxAdListener`, `MaxAd`, `MaxError` nằm trong `applovin-sdk` 13.6.4 (`libs.applovin.sdk`, đã là `implementation` của `:core:ads` từ T-068); đã kiểm có trong `classes.jar` của bản AAR trên máy dev

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** dựng `MaxInterstitialAd(adUnitId, context)` trước khi SDK khởi tạo có thể ném lỗi (T-071 gặp với `MaxAdView`); test JVM dùng bản giả nên không lộ ra.
- **Điều kiện chuyển:** không có trong phiên (đối tượng thật chỉ dựng khi `maxReady` đã bật). Nếu kiểm thủ công 1 thấy lỗi dựng: dựng bằng `MaxInterstitialAd(adUnitId, AppLovinSdk.getInstance(context), context)`.
- **Cách làm thay:** đổi một dòng trong `AppLovinMaxInterstitial`; test giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-11-15): 343 test dùng tên của `MaxSdk.kt`, `AdsController.kt`; "Cần xem trước" 4, "Nên xem" 67.

- "Cần xem trước": `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `BannerAdSizeTest.bottomBannerSlot_withMaxBannerUnit_reportsTallestHeight` (dựng `MaxConfig` có khóa và mã banner: `unitFor(BANNER)`, `unitFor(BANNER_MREC)` không đổi; thêm `INTERSTITIAL` vào `MAX_FORMATS` không chạm chiều cao banner), `InListAdsTest.positions_followEvery` (đường native trong danh sách, không qua interstitial), `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction` (rewarded của AdMob, plan không đổi đường rewarded): giữ nguyên.
- "Nên xem" (67): dựng controller bằng `testAdsController` với `MaxConfig.NONE` (thứ tự hiệu lực `admob`, `interstitialPlatforms` trả `[ADMOB]`), hay đọc `unitFor` của `AdUnitIds` (AdMob, không phải `MaxConfig.unitFor`): giữ nguyên.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Dòng log thiếu mã MAX kể `banner`, `banner_mrec` | `MAX_FORMATS` thêm `INTERSTITIAL`, dòng log kể thêm `interstitial` (bước 1) | `AdsControllerMaxTest.start_keyWithoutBannerUnits_logsMissingOnce_noUnitInLog`: chuỗi `"MAX ad units missing for banner_mrec; ..."` và `"... for banner, banner_mrec; ..."` | Cập nhật dữ liệu theo dòng enum plan thêm (ADR-0029): `"MAX ad units missing for banner_mrec, interstitial; those slots run without MAX"` và `"MAX ad units missing for banner, banner_mrec, interstitial; those slots run without MAX"`; câu không lộ mã và câu không khóa giữ nguyên |
| `maxUnitId(INTERSTITIAL)` luôn `null` | Có mã `interstitial` thì trả mã | `AdsControllerMaxTest.maxUnitId_readsConfig`: `assertNull(controller.maxUnitId(AdFormat.INTERSTITIAL))` với cấu hình `"banner=u1;banner_mrec=m2"` | Giữ: cấu hình đó không có mã `interstitial`, vẫn `null` |
| `MaxConfig` chỉ có khe banner, MREC: `unitFor(INTERSTITIAL)` luôn `null`, `toString` đếm 2 khe | `MAX_FORMATS` thêm `INTERSTITIAL` (bước 1) | `MaxSdkStarterTest.maxConfig_unitFor_bannerAndMrecOnly_noKeyNoUnit`: với `"banner=a1;banner_mrec=b2;interstitial=c3"`, `assertNull(config.unitFor(AdFormat.INTERSTITIAL))` và `"units=2" in text` | Viết lại (ADR-0031), đổi tên `maxConfig_unitFor_maxFormatsOnly_noKeyNoUnit`. Câu của plan nói điều mới: "`MaxConfig.MAX_FORMATS` thêm `AdFormat.INTERSTITIAL`" (bước 1). Câu ý định: chỉ các khe MAX có mã; định dạng ngoài danh sách luôn `null`. Câu mới: `assertEquals("c3", config.unitFor(AdFormat.INTERSTITIAL))`; cấu hình thêm `banner_collapsible=x4` và câu `assertNull(config.unitFor(AdFormat.BANNER_COLLAPSIBLE))` có sẵn nay kiểm thật (định dạng không bao giờ là khe MAX; không dùng `rewarded`, vì T-106 sẽ thêm khe đó); `"units=3" in text`; `assertFalse(text, "c3" in text)`. Mọi câu khác giữ. Đối chứng: tạm bỏ `AdFormat.INTERSTITIAL` khỏi `MAX_FORMATS` thì đỏ |
| Interstitial chỉ nạp và hiện AdMob | Theo `interstitialPlatforms`; `MaxConfig.NONE` (mọi test cũ) cho thứ tự hiệu lực `admob`, nên `[ADMOB]` | Mọi test inter trong `AdsControllerNativeComboTest`, `AdsControllerStartupInterComboTest`, `AdsControllerResumeTest`, `AdsControllerHostTest`, `FullScreenAdsTest` | Giữ: không câu nào đổi, vì thứ tự hiệu lực không khóa là `admob` |

## 4. Kiểm thử

Test viết trước, ở commit test. `MaxInterstitialSlotTest`, `FullScreenPlatformsTest` là unit test thuần: khe chạy trong `backgroundScope` của `runTest` (khe theo dõi `maxReady` mãi, `scope` thường làm test treo), đồng hồ giả, `maxReady`, `shouldLoad`, `mayRequestNow` là biến của test. `AdsControllerMaxInterstitialTest` chạy Robolectric (`@RunWith(AndroidJUnit4::class)`), trình tự dựng:

1. `testAdsController(maxConfig = MaxConfig("FAKE_MAX_KEY_0123", "interstitial=FAKE_UNIT_A"), maxPlatform = fakePlatform, flags = DefaultAdsFlags(remote))`, `remote` là `FakeRemoteSettings` có `ads_mediation_order` và công tắc `is_show_<vị trí>` của vị trí thử bật.
2. `controller.interstitialRequestForTest = { requested += it; admobAnswer }` (điểm nối mới của bước 4: nhận vị trí, trả `admobAnswer`; `null` là lỗi nạp, một `FakeInterstitialAd` là nạp được; thay lời gọi SDK `InterstitialAd.load`). Câu "AdMob được gửi / không được gửi" đọc `requested`.
3. `start(...)`, `openConsentGateForTest()`, `admobReadyForTest()`; MAX sẵn sàng bằng `fakePlatform.finishInitialization()`.
4. Test kế hoạch nạp sẵn: `setPreloadPlan(...)` có vị trí thử, rồi `requestPreloadForTest()`.
5. Quảng cáo AdMob có sẵn bằng `putLoadedInterstitialForTest(placement, FakeInterstitialAd(...))`; quảng cáo MAX bằng `fakePlatform.interstitials.single().emitLoaded()`.

Mã giả có chữ `FAKE`, không khớp mẫu của `scripts/secret-scan.py`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Chọn nền tảng theo thứ tự, `open_app_inter` chỉ AdMob | Unit test | `FullScreenPlatformsTest.interstitialPlatforms_followOrder_dropMissingUnits` (bốn thứ tự × có/không mã mỗi nền tảng); `interstitialPlatforms_openAppInter_admobOnly_anyOrder` (kể cả `max`: `[ADMOB]`); `interstitialPlatforms_resumeInter_followsOrder` |
| Hiện nền tảng đầu đã có quảng cáo; trạng thái của lượt và của nạp sẵn | Unit test | `FullScreenPlatformsTest.readyPlatform_firstWithAd_elseNext_elseNull`; `turnLoadState_readyWhenAny_elseFirstPlatformState_idleWhenNone`; `preloadSettleState_loadingWhileAnyLoads` |
| KB-31 tiêu chí 1: thứ nhất chưa có, thứ hai có thì hiện thứ hai, một cơ hội | Robolectric | `AdsControllerMaxInterstitialTest.maxFirst_noMaxAd_admobReady_showsAdmob_oneOpportunity` (thứ tự `max_admob`, chỉ AdMob có quảng cáo: `FakeInterstitialAd` được `show`, khe MAX không `show`, đúng một sự kiện cơ hội `shown` trong `RecordingAdMetrics`); `admobFirst_noAdmobAd_maxReady_showsMax_oneOpportunity`; `bothReady_firstPlatformShown` |
| ADR-0045 mục 2.3: nền tảng trong thứ tự đều nạp sẵn, không chặn nhau | Robolectric | `maxAdmob_preload_bothPlatformsLoad` (một lượt nạp sẵn: `requested` có vị trí và khe MAX `load()` một lần); `maxReady_admobEmpty_notReadyTurn_loadsAdmob` (thứ tự `admob_max`, `admobAnswer = null`: AdMob lỗi, chờ thử lại; MAX có quảng cáo; tới hạn thử lại F-05 AdMob vẫn được gửi lần hai dù trạng thái gộp là `Ready`); `admobShown_maxWaitingRetry_admobReloadsAfterTwoSeconds` (`admobAnswer` là `FakeInterstitialAd`, khe MAX lỗi đang chờ thử lại, AdMob hiện rồi đóng: sau 2 s `requested` có vị trí lần hai) |
| KB-31 tiêu chí 2: `admob`/`max` chỉ nạp, hiện nền tảng đó | Robolectric | `orderAdmob_maxSlotNeverLoads` (nạp sẵn: `fakePlatform.interstitials` rỗng); `orderMax_noAdmobRequest_onlyMaxLoads` (nạp sẵn: `requested` rỗng, khe MAX `load()` một lần); `orderMax_onlyMaxShown` |
| Khóa thứ tự hay công tắc đổi giữa chừng khi khe MAX đang giữ tài nguyên | Unit test + Robolectric | `orderChangedToAdmob_whileMaxHoldsAd_maxNotShown_admobShown`; `MaxInterstitialSlotTest.retryDue_shouldLoadFalse_noRequest_idle` (đổi thứ tự sang `admob` khi khe chờ thử lại); `reloadAfterClose_shouldLoadFalse_noRequest`; `maxReadyLater_shouldLoadFalse_noRequest`; controller: `allInterPlacementsOff_maxRetryDue_noRequest` (tắt mọi vị trí inter khi khe chờ thử lại: tới hạn không `load()`) |
| KB-31 tiêu chí 3 (mở rộng): `open_app_inter` chỉ AdMob, `resume_inter` theo thứ tự | Robolectric | `startupInter_maxOrder_loadsAndShowsAdmobOnly` (`showStartupAd` với `StartupAdTurn(OPEN_APP_INTER)`, thứ tự `max`, thêm vị trí `gallery_interstitial` bật để khe MAX được nạp và có quảng cáo: chỉ AdMob được gửi (`requested`) và hiện, khe MAX không `show`); `resumeInter_maxFirst_showsMax` |
| KB-31 tiêu chí 5: không gửi khi cổng đồng ý đóng, mọi đường, cả sau khi mở rồi đóng | Unit test + Robolectric | `MaxInterstitialSlotTest.load_gateClosedAtSend_noRequest_idle_noRetry` (đường nạp do controller gọi); `retryDue_gateClosed_noRequest` (thử lại F-05); `reloadAfterClose_gateClosed_noRequest` (nạp lại F-06); `maxReadyLater_gateClosed_noRequest` (đường chờ `maxReady`); controller: `consentClosedAfterLoad_maxAdNotShown` (cổng đóng sau khi khe có quảng cáo: `CONSENT_CLOSED`, không `show`); `gateClosedAtSend_thenReopened_maxLoadsOnce` (khe MAX lỗi, đang chờ thử lại F-05; `closeConsentGateForTest()`; tới hạn thử lại: `send()` thấy `mayRequestNow()` sai, khe về `Idle`, không `load()`; `reopenConsentGateForTest()`: lượt `GATE_REOPENED` của kế hoạch nạp sẵn làm khe MAX `load()` đúng một lần); `gateClosed_loadFullScreenAdIfNeeded_noStateChange` (cổng đóng, gọi thẳng `loadFullScreenAdIfNeeded(p)`: không `requested`, khe MAX không đổi trạng thái, giữ hành vi hôm nay) |
| Lượt nạp sẵn đầu trước khi MAX sẵn sàng (L-072) | Unit test + Robolectric | `MaxInterstitialSlotTest.load_beforeMaxReady_sentOnceWhenReady`; `maxReady_notWanted_noRequest` (đối chứng); controller: `preloadPlan_beforeMaxReady_maxLoadsWhenReady` (trình tự bước 4 trước bước 3 phần MAX: kế hoạch chạy, `fakePlatform.interstitials` rỗng; `finishInitialization()`: một `load()`) |
| F-05: thử lại sau lỗi nạp | Unit test | `MaxInterstitialSlotTest.loadFailed_retriesAfterFullScreenDelay` (lỗi lần 1: chờ 2 s, lần 2: 4 s theo `failuresInARow`; `state` là `WaitingRetry(dueAt)` đúng mốc; tới hạn `load()` lại); `loadFailed_metricsHaveMaxLabels_lastLoadErrorKept` (`maxLoadFailed` với bộ đếm `preload:interstitial/max`; `lastLoadError` bằng nhãn trả về; nạp được thì `null`) |
| F-06: nạp lại 2 giây sau khi đóng hay hiện lỗi | Unit test + Robolectric | `MaxInterstitialSlotTest.closed_reloadsAfterTwoSeconds` (1999 ms chưa `load`, 2000 ms có); `showFailed_reloadsAfterTwoSeconds`; controller: `maxShown_hidden_reloadsMaxSlot_noAdmobWhenOrderMax` |
| L-068: hai yêu cầu chồng nhau | Unit test + Robolectric | `MaxInterstitialSlotTest.loadTwiceWhileLoading_oneRequest` (một `load()`, một `requestSent`); controller: `twoPlacementsShareSlot_secondTurnNotShownSameMaxAd` (hai vị trí inter cùng khe MAX: lượt thứ nhất lấy quảng cáo, lượt thứ hai `not_ready` hay hiện AdMob, quảng cáo MAX không hiện hai lần); `MaxInterstitialSlotTest.loadTwice_whileShouldLoadSuspended_oneRequest` (`shouldLoad` treo bằng `CompletableDeferred`, `loadIfNeeded` hai lần: một `load()` sau khi mở); controller: `twoInterPlacementsInOnePreloadGroup_oneMaxLoad` |
| KB-31 tiêu chí 6: một cổng, cờ đang hiện, mốc, combo, gỡ kẹt cho MAX | Robolectric | `maxShown_raisesPresence_secondTurnBusy`; `maxHidden_recordsIntervalMarkFirst_thenFinishes` (mốc ghi trước khi `onFinished` chạy, I-05; lượt sau trong `ads_interval` bị `THROTTLED`); `maxHidden_opensCombo_whenNativeAfterInterOn` (như `AdsControllerNativeComboTest.closed_opensCombo`); `maxDisplayFailed_finishesOnce_showFailMetric`; `maxStuck_recovered_finishesOnce` (F-03); `maxHostGone_skipsBeforeTurn` (F-08: `shown` rồi `ad_show_fail` `app_not_foreground`, khe MAX không `show`) |
| Số liệu của lượt MAX | Unit test + Robolectric | `MaxInterstitialSlotTest.loaded_recordsApiCalled` (`af_inters_api_called` một lần, trong `RecordingAdFunnel`); controller: `maxDisplayed_funnelAndInterCount` (`displayed` một lần, bộ đếm N-05 tăng); `maxClicked_recordsClick`; `maxFirst_maxFailed_opportunityHasMaxLoadState` (thứ tự `max_admob`, khe MAX lỗi, AdMob chưa lỗi: cơ hội `not_ready` mang nhãn lỗi của MAX) |
| Log không lộ mã | Unit test | `MaxInterstitialSlotTest.logs_neverContainUnitId` |
| Không khóa MAX: như hôm nay | Robolectric | Các test inter có sẵn chạy không đổi (`MaxConfig.NONE`); thêm `noMaxKey_interstitialAdmobOnly_noMaxObjectBuilt` |
| Thử trên máy thật | Kiểm tra thủ công (máy thật: quảng cáo thật, cần khóa và mã MAX thật trong `local.properties`) | Bản staging dựng trên máy dev có `ads.maxSdkKey`, `ads.maxAdUnits` (thêm `interstitial=<mã>`), `ads_mediation_order` = `max_admob` trên Firebase staging: (1) vào "Ad gallery", bấm "Interstitial": log thẻ `Ads` có `Interstitial (MAX) loaded` rồi `Interstitial shown (MAX)`, quảng cáo MAX hiện, đóng xong app tiếp tục; (2) đặt `admob_max`: hiện AdMob; (3) mở app lần đầu với `open_app_inter` bật: inter lúc khởi động là AdMob |

**Ngoại lệ viết test trước:**

- `AppLovinMaxInterstitial` (bọc SDK thật) và `AppLovinMaxPlatform.newInterstitial`: không chạy được trong test JVM vì cần SDK đã khởi tạo; kiểm bằng kiểm thủ công.
- Tài liệu (`ads-business-rules.md`, `ads-guide.md`): chỉ chữ.

### 4.1. Chạy thử

Không cần (E dưới 2): biến thể của banner MAX (T-071) và đường interstitial AdMob có sẵn; lớp MAX đã có trên classpath biên dịch.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (interstitial có thêm nền tảng quảng cáo), 7 (module core dùng chung `:core:ads`). Mức Cao, duyệt chéo bắt buộc.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Nhiều phần trong `:core:ads` (3 lớp mới, `AdsController`, `MaxSdk.kt`), có đổi giao diện nội bộ `MaxPlatform` |
| B. Độ rõ của yêu cầu | 2 | KB-31 và câu hỏi 41, 42 đã chốt; dòng đổi của câu hỏi 41 (`open_app_inter`) chờ xác nhận; câu hỏi 1 tới 3 ở mục 8 |
| C. Khả năng kiểm chứng tự động | 1 | Phần lớn có test với bản giả; hiện quảng cáo MAX thật chỉ kiểm thủ công |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi interstitial mà mọi app dùng (mặc định `max_admob` khi có khóa); T-070, T-072, T-106 dựa vào khe và cách chọn nền tảng này |
| E. Độ mới của kỹ thuật | 1 | Biến thể của banner MAX; SDK interstitial MAX lần đầu dùng |
| **Tổng** | 9 | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `MaxSdk.kt` (`MaxPlatform`, `MaxConfig`), `FakeMaxPlatform`.
- Task khác: T-106 (rewarded của MAX) dùng `FullScreenPlatforms.kt` (thêm hàm cho rewarded) và mẫu `MaxInterstitialSlot`; T-070 (app open của MAX khi quay lại) và T-072 (doanh thu MAX: thêm listener doanh thu vào `MaxInterstitialSdkAd`) chạy sau. Issue #498 (`collapsibleLayer`) không chạm file của plan.
- Đề xuất thứ tự chạy: trước T-106, T-070, T-072. Không cùng phiên với plan khác sửa `AdsController.kt`.
- Phát hành: inter của MAX chưa ghi doanh thu tới khi T-072 xong (KB-31, tiêu chí 7). App bật MAX ở production (có khóa) nên phát hành sau T-072, hoặc chấp nhận thiếu doanh thu MAX trên Firebase, AppsFlyer trong khoảng đó.

## 7. Điều kiện dừng riêng

- Cần đổi chữ ký hay hành vi của `decideFullScreenTurn`, `beginInterstitialTurn`, `FullScreenAdPresence`, `FullScreenReloads`, hay đường combo (`NativeFullScreenTurns`).
- Test inter có sẵn đỏ ngoài các dòng của mục 3.5.

## 8. Câu hỏi cho người duyệt

1. **Phạm vi (đã trả lời).** Anh Nhật chọn ngày 2026-11-15: T-069 chỉ interstitial, rewarded thành T-106 (phụ thuộc T-069); `open_app_inter` chỉ AdMob, `resume_inter` theo thứ tự. Spec câu hỏi 41 thêm dòng đổi trong PR này, chờ xác nhận cùng lời duyệt plan.
2. **Lý do hiện lỗi của MAX.** `ad_show_fail` của MAX ghi mã MAX, lý do `unknown` (bảng `showFailReasonOf` chỉ có mã AdMob 0 tới 4). Đề xuất: giữ `unknown` trong T-069; T-072 (số liệu MAX) thêm bảng quy đổi mã hiện lỗi của MAX nếu nhóm UA cần.
3. **Phễu AppsFlyer với mã MAX.** `af_inters_ad_displayed` của lượt MAX mang mã đơn vị MAX và tên adapter rỗng. Đề xuất: giữ như vậy (cùng sự kiện cho cả hai nền tảng, câu hỏi 46); T-072 thêm giá trị nền tảng nếu cần.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
