---
ticket: T-025
title: Quảng cáo khi quay lại app, bỏ lượt sau khi bấm quảng cáo
status: draft                # draft | approved | stopped | done
version: 1                   # sửa theo rà soát độc lập 2026-10-25, trước khi duyệt (chưa duyệt nên giữ 1)
risk_score:                  # để trống: chạm điểm chặn (ads, analytics, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý 15.0M (scripts/token-usage.py estimate, 2026-10-25, tạm vì chưa đủ 5 task M); nâng vì nối vào AdsController.kt ở bốn chỗ, bốn file test mới và năm file test sửa
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-025: QUẢNG CÁO KHI QUAY LẠI APP, BỎ LƯỢT SAU KHI BẤM QUẢNG CÁO

## 1. Tóm tắt

**Sửa theo rà soát độc lập (2026-10-25, trước khi duyệt):** `resume_inter` thành câu hỏi đổi spec kèm điều kiện dừng, không tự sửa `ads-business-rules.md`; bỏ khẳng định "kiểm lại chặn bắt được form đồng ý" (lượt hỏi lại đồng ý chạy không đồng bộ), ghi thành rủi ro chấp nhận ở câu hỏi 2; lượt resume im lặng tới khi lượt quảng cáo của màn loading xong (câu hỏi 7); test đối chứng trong cùng test cho mọi khẳng định "không có gì xảy ra" (L-055); test controller `AdsControllerResumeTest` (L-072); cập nhật mục 6 theo T-026 `14c04d9`, T-040 `e14f5c4`, T-031 `1e1bf18`.

`:core:ads` theo dõi foreground của cả process (mọi Activity trong process, kể cả Activity của SDK quảng cáo; xoay màn hình không tính), và mỗi lần app lên foreground, trừ lần đầu của process, xét các cổng của A-10 tới A-15 bằng một hàm thuần: im lặng khi chưa được phục vụ, rồi đang bị chặn, `show_resum_ads` tắt, đang có quảng cáo toàn màn hình, khóa 1 giây sau app open, chưa có quảng cáo. Qua hết thì lượt hiện chạy ở lần Activity của app hiện lại kế tiếp: app open qua khe app open của T-024 (vị trí toàn app mới `resume`), hoặc interstitial (vị trí toàn app mới `resume_inter`, bỏ cổng `ads_interval`; spec chưa có vị trí này, cần người sửa spec và xác nhận lại trước khi task chạy, câu hỏi 1) nối combo native qua bộ điều phối `NativeFullScreenTurns` của T-026. Trong lúc màn loading chưa xong lượt quảng cáo mở app, lượt resume im lặng (câu hỏi 7). Mọi lượt bấm quảng cáo đi qua `AdTelemetry.clicked` đặt dấu "rời app", nên lượt quay lại kế tiếp bị bỏ (A-14). Kiểm chứng bằng unit test cho từng cổng và cho bộ điều phối lượt resume, test Robolectric dựng lại Activity và đưa app về nền rồi lên lại (không có lượt resume khi xoay màn hình, khi Activity của SDK che app), và kiểm thủ công trên máy thật.

## 2. Hiểu yêu cầu

Ticket xử lý KB-14 trong `features/ads/spec.md` (spec `confirmed`, xác nhận lại 2026-10-24; câu hỏi 11 đã trả lời: `:core:ads` làm quảng cáo khi quay lại), luật A-10 tới A-15, G-01 tới G-06, mục 8.2, AS-01 của `docs/ops/ads-business-rules.md`.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-14 | Khi người vận hành bật quảng cáo khi quay lại thì mỗi lần app trở lại màn hình có quảng cáo mở app (hoặc interstitial nếu chọn loại "inter", có thể nối native toàn màn hình). | "Trở lại màn hình" là process lên foreground: số Activity đang `started` của cả process (kể cả `AdActivity` và Activity của adapter mediation) đi từ 0 lên 1, trừ khi Activity vừa dừng là do đổi cấu hình (xoay màn hình, đổi chế độ tối) (A-10, "theo dõi vòng đời của cả process"). Khóa: `show_resum_ads` (đã gộp `is_show_ads`, `AdsGlobalFlags.showResumeAds`). `resume_ad_type` đúng bằng `inter` thì vị trí `resume_inter` (interstitial, `ignoreInterval = true`: bỏ cổng `ads_interval`, vẫn đẩy mốc khi đóng), giá trị khác thì vị trí `resume` (app open) (A-12, `OpenAdType.of`). Spec mục 5 chỉ có một dòng vị trí `resume`, A-15 và PL-02 nói vị trí của resume "luôn là `resume`": `resume_inter` là thay đổi spec, người quyết (câu hỏi 1); task chỉ chạy khi spec đã có dòng này (mục 7). Inter của resume hoàn tất thì nối combo khi `is_show_native_after_inter` bật lúc đó, qua `NativeFullScreenTurns.afterInterstitial` của T-026 (K-01, HD-05). Lượt hiện chạy ở lần `onActivityResumed` kế tiếp của một Activity của app, trên chính Activity đó, sau bước gỡ kẹt và hỏi lại đồng ý, trước bước nhả dấu rời app (G-06). |
| KB-14 | Khi người dùng vừa bấm vào một quảng cáo rồi quay lại thì lần quay lại đó không có quảng cáo. | A-14: mọi định dạng (interstitial, app open, native ở mọi khe, native toàn màn hình và combo, banner, và rewarded của T-031) báo lượt bấm qua một hàm duy nhất `AdTelemetry.clicked`; hàm này gọi `onClicked` trước khi xét công tắc số liệu, `AdsController` nối `onClicked` với `ResumeSuppression.markAdClicked()` đặt dấu rời app (cùng dấu của G-03 cách 3, nguồn log `ad_click`). Dấu làm lượt resume kế tiếp ra `SUPPRESSED`; dấu nhả ở lần Activity của app hiện lại (bước cuối của `ActivityResumeSteps`, G-05). Bấm nhiều lần chỉ giữ một dấu (dấu là cờ, không phải bộ đếm). |
| KB-14 | Khi người dùng quay lại từ màn chọn ảnh, camera, chia sẻ hay hộp thoại đồng ý mà app tự mở thì không có quảng cáo. | Cổng "đang bị chặn" đọc `ResumeSuppression.resumeAdsSuppressed` có sẵn (T-022): bộ đếm chặn (màn của hệ thống, trang toàn màn hình, form UMP của T-020, combo của T-026) cộng dấu rời app; chặn chỉ hạ khi vỏ app báo lại trong composition (G-02, G-04). Kiểm lại một lần nữa ngay trước khi hiện. Lần kiểm này chỉ bắt được form UMP đã giữ chặn lúc đó: lượt hỏi lại đồng ý (S-09) chạy không đồng bộ, `ConsentCoordinator` chỉ giữ chặn `CONSENT_FORM` trong `onUpdateSucceeded` (`ConsentCoordinator.kt:182`), sau khi UMP trả lời, nên form cần hiện của lượt hỏi lại có thể tới sau khi app open đã hiện. AdsKit có cùng khe hở (chặn chỉ bật khi form thật sự hiện, S-16). Plan không khẳng định bắt được trường hợp này, ghi là rủi ro chấp nhận (câu hỏi 2). Lượt mở app đầu tiên, khi form thường hiện nhất, không có resume (A-10). |
| KB-14 | Khi đang có một quảng cáo toàn màn hình, hoặc quảng cáo mở app vừa đóng chưa tới 1 giây, thì không hiện thêm. | Cổng "đang có quảng cáo toàn màn hình": `FullScreenAdPresence.isBusy(now)` (inter, app open, native toàn màn hình, combo; trần 10 phút, F-02) ra `BUSY`. Khóa 1 giây: `appOpenLockPassed(lastAppOpenClosedAt, now)` của T-024, **chỉ khi** vị trí resume là app open (A-11), ra `THROTTLED`. Resume không có cổng thời gian nào khác: không thời gian tối thiểu ở nền, không `ads_interval` (A-11, AS-01). |
| KB-14 | Khi quảng cáo chưa sẵn sàng thì lần này không hiện, app tải sẵn cho lần sau. | `NOT_READY`: ghi cơ hội `not_ready` với `load_state` chụp trước, rồi `loadFullScreenAdIfNeeded(vị trí)` (F-04: không nạp khi đang nạp, đã có hay chờ thử lại). Bị chặn ở cổng trước thì không kích nạp (A-13). Khe resume còn được kế hoạch nạp sẵn nạp như mọi khe toàn màn hình khi khóa của nó bật (`AdPreloadPlan.fromPlacements`, app open ở nhóm đầu, S-20). |
| KB-14 | Lần mở app đầu tiên (khởi động mới) không tính là quay lại. | Lần lên foreground đầu tiên của process bị bỏ (`FIRST_FOREGROUND`), kể cả khi đồng ý chưa mở; không ghi cơ hội, không kích nạp. Cờ "lần đầu" bị tiêu ở chính lượt đó, kể cả khi lượt đó chưa được phục vụ (A-10). Process mới (mở lại sau khi bị hệ thống đóng) lại có lần đầu của nó. |

Luật trong phạm vi ticket không có tiêu chí riêng trong spec, vẫn có test: A-10 (chưa được phục vụ thì im lặng: không ghi cơ hội, không kích nạp), A-11 (thứ tự cổng), A-13, A-15 (vị trí của resume), mục 8.2 (resume bị chặn ghi `ad_opportunity` vị trí của resume; lần đầu và resume tắt **không** phải cơ hội, L-A05; không bao giờ `throttled` vì `ads_interval`, AS-01), G-05 (dấu rời app nhả sau đúng một lượt).

Không thuộc ticket: combo (T-026: màn native, đếm ngược, mốc của cụm); khe rewarded (T-031: lượt bấm rewarded tự đi qua `AdTelemetry.clicked` nên được A-14 nhờ task này); sự kiện AppsFlyer (T-033).

Test nghiệm thu gợi ý `AT-RS-*` nằm trong bản đặc tả độc lập của AdsKit (repo riêng, không có trong repo này). Plan ánh xạ thẳng từ KB-14 và A-10 tới A-15 (bản viết lại trong `ads-business-rules.md`); mục 4 có một test cho mỗi cổng và mỗi luật.

Thiết kế tham chiếu: Không có thiết kế riêng. Quảng cáo app open và interstitial do SDK vẽ; màn native của combo là của T-026 (docs/ops/design-workflow.md).

ADR liên quan: ADR-0026 (hành vi PARTNER), ADR-0004, ADR-0013 (quảng cáo qua `:core:ads`, cổng đồng ý), ADR-0014 (mặc định tắt), ADR-0019 (log), ADR-0023 (không thêm khóa), ADR-0001 (`:app` chỉ gọi `:core:ads`).

Dựa trên code đã merge (nhánh chính, commit `3d4f165`, sau T-024), dùng đúng tên: `AdsActivityTracker` (`Listener`, `onForegroundChanged`, `onActivityResumed`), `StartedActivityCounter`, `ActivityResumeSteps`, `ResumeSuppression` (`resumeAdsSuppressed`, `markLeavingApp`, `onActivityResumed`), `ForegroundAdSuppressor`, `FullScreenAdPresence.isBusy`, `appOpenLockPassed`, `lastAppOpenClosedAt`, `showAppOpen`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, `opportunityLoadState`, `AdTelemetry.opportunity`, `AdTelemetry.clicked`, `latestGlobalFlags`, `AdsGlobalFlags.showResumeAds`, `resumeAdType`, `OpenAdType`, `placementSwitch`, `startupAdPlacement`, `StartupAdTurn`, `AdPlacement.appWide`. Từ plan T-026 (`draft`, nhánh `origin/claude/plan-T-026-inter-native-sau-inter`, commit `c05ebff`), task phụ thuộc: `showInterstitialTurn(activity, placement, combo, onFinished)`, `NativeFullScreenTurns.afterInterstitial`.

## 3. Giải pháp

### 3.1. Cách làm

Theo mẫu của `:core:ads`: luật là hàm thuần có unit test (như `decideFullScreenShow`, `startupAdPlacement`), trạng thái theo thời gian là một lớp chạy trên luồng chính nói chuyện với `AdsController` qua một `interface` cổng để test truyền bản giả (như `StartupAdProbe` của T-024, `NativeFullScreenPort` của T-026). `AdsController` chỉ cài cổng bằng các trường đã có.

**1. Vị trí toàn app của resume** (`AdPlacement.kt`, MC-09, A-15; câu hỏi 1). Dòng `RESUME` có trong spec (mục 5, vị trí `resume`). Dòng `RESUME_INTER` chỉ làm khi spec đã được sửa và xác nhận lại cho vị trí này; plan không tự duyệt chỗ lệch với A-15 (mục 7). Người chọn phương án "chỉ `RESUME`" ở câu hỏi 1 thì bỏ dòng `RESUME_INTER`, nhánh inter của bước 2, 5 và các test của nó, và plan sửa thành phiên bản 2 trước khi chạy.

- Hai dòng mới, mỗi dòng một dòng (dạng `scripts/release.sh` đọc), ngay sau `OPEN_APP_INTER` (hay sau `NATIVE_AFTER_INTER` nếu T-026 đã merge):
  - `RESUME("resume", AdFormat.APP_OPEN, appWide = true),`
  - `RESUME_INTER("resume_inter", AdFormat.INTERSTITIAL, ignoreInterval = true, appWide = true),`
- `recordInterval` giữ mặc định `true`: inter của resume đẩy mốc nhóm mặc định khi đóng (I-02, MC-04 "resume luôn ở nhóm mặc định"). App open của resume không đẩy mốc (`pushesIntervalMark` sai cho `APP_OPEN`, I-02).
- KDoc của `AdFormat.APP_OPEN`: "Shown by the loading screen through [StartupAds] (T-024) and when the app comes back to foreground (`resume`, T-025)".
- Vị trí toàn app không có `is_show_<mã>` (T-024, `AdsSettings.keysFor`), nên bảng khóa sinh tự động (`docs/remote-config-keys.md`, `docs/remote-config-template.json`) không đổi. Production cần mã đơn vị `resume=` và `resume_inter=` trong `release.properties` (`ConfiguredAdUnitIds.missing`, `scripts/release.sh`), ghi vào `ads-guide.md`.

**2. Khóa của vị trí resume** (`StartupAdChoice.kt`):

```kotlin
/** A-12: the placement of the resume ad, or null when `show_resum_ads` (with `is_show_ads`) is off. */
internal fun resumeAdPlacement(flags: AdsGlobalFlags): AdPlacement? {
    if (!flags.showResumeAds) return null
    return if (flags.resumeAdType == OpenAdType.INTERSTITIAL) AdPlacement.RESUME_INTER else AdPlacement.RESUME
}
```

`placementSwitch`: thêm nhánh trước nhánh của app open lúc mở app: `RESUME`, `RESUME_INTER` ra `flags.global.map { resumeAdPlacement(it) == placement }.distinctUntilChanged()` (không chờ `firstOpen`). Mọi đường nạp và hiện đọc khóa qua `AdsController.isEnabled` như hôm nay (L-072), nên chỉ khe của loại đang chọn được nạp sẵn (`collectPreloadTriggers` nạp khi khóa bật từ xa). `startupAdPlacement` không bao giờ trả vị trí resume (giữ nguyên).

`StartupAds.kt`: `StartupAdTurn.init` chỉ nhận đúng hai vị trí lúc mở app: `require(placement == AdPlacement.OPEN_APP || placement == AdPlacement.OPEN_APP_INTER) { "${placement.key} is not a startup placement" }` (thay `require(placement.appWide)`; nếu T-026 đã merge thì điều kiện này thay luôn phần từ chối `NATIVE_FULL_SCREEN` của T-026, cùng ý). `rememberShowFullScreenAd` đã từ chối mọi vị trí toàn app (T-024), nên feature không gọi thẳng vị trí resume được.

**3. Foreground của cả process** (`ProcessForegroundCounter.kt`, mới; `AdsActivityTracker.kt`):

```kotlin
/**
 * Counts the started activities of the whole process, the ads SDK's included (A-10), so a full-screen ad covering
 * the app is not a trip to background. A stop for a configuration change (rotation, dark mode) and the start of the
 * recreated activity report nothing. Main thread only.
 */
internal class ProcessForegroundCounter {
    /** True only when this start brings the process to foreground (0 -> 1), not when it restarts a recreated activity. */
    fun onStart(): Boolean
    /** True only when this stop sends the process to background (1 -> 0) and is not for a configuration change. */
    fun onStop(changingConfigurations: Boolean): Boolean
}
```

Cách tính: `onStop` hạ bộ đếm (không xuống âm); về 0 mà `changingConfigurations` thì đặt cờ `restarting`, không báo; `onStart` lên 1 mà có `restarting` thì xóa cờ, không báo. Chạy thử (mục 4.1): trong `ActivityScenario.recreate()` và `ActivityController.recreate()` của Robolectric, `onActivityStopped` thấy `isChangingConfigurations == true` và `onStop` của Activity cũ chạy trước `onStart` của Activity mới, nên không có cờ này thì xoay màn hình là một lần về nền rồi lên lại.

`AdsActivityTracker`: thêm trường `private val process = ProcessForegroundCounter()` và hàm `Listener.onProcessForegroundChanged(foreground: Boolean) {}` (mặc định rỗng như các hàm khác). `onActivityStarted`, `onActivityStopped` gọi bộ đếm này **trước** bộ lọc `filter.isAppActivity` (mọi Activity của process), rồi giữ nguyên phần của Activity của app (`foreground`, `onForegroundChanged` cho nạp native và đồng ý, không đổi hành vi). Không dùng `ProcessLifecycleOwner`: thư viện vòng đời process của AndroidX chưa có trong `gradle/libs.versions.toml` (thư viện mới, điểm chặn 2).

**4. Thứ tự cổng resume** (`ResumeAdRules.kt`, mới, A-10, A-11, A-15, mục 8.2):

```kotlin
internal enum class ResumeDecision { FIRST_FOREGROUND, STARTUP, NOT_SERVING, SUPPRESSED, OFF, BUSY, THROTTLED, NOT_READY, SHOW }

/** What the resume gate reads when the process comes to foreground. [placement] is [resumeAdPlacement], null when off. */
internal data class ResumeGate(
    val firstForeground: Boolean,
    val startupFinished: Boolean,  // the loading screen's startup ad turn is over (question 7)
    val serving: Boolean,          // consent gate open and SDK started (canRequestAds)
    val suppressed: Boolean,       // ResumeSuppression.resumeAdsSuppressed
    val placement: AdPlacement?,
    val busy: Boolean,             // FullScreenAdPresence.isBusy(now)
    val appOpenLockPassed: Boolean,
    val ready: Boolean,            // isFullScreenAdReady(placement); false when placement is null
)

/** A-10, A-11: first foreground, not served, blocked, switched off, full-screen ad on screen, app open lock, no ad. */
internal fun decideResumeAd(gate: ResumeGate): ResumeDecision = when {
    gate.firstForeground -> FIRST_FOREGROUND
    !gate.startupFinished -> STARTUP
    !gate.serving -> NOT_SERVING
    gate.suppressed -> SUPPRESSED
    gate.placement == null -> OFF
    gate.busy -> BUSY
    gate.placement.format == AdFormat.APP_OPEN && !gate.appOpenLockPassed -> THROTTLED
    !gate.ready -> NOT_READY
    else -> SHOW
}

/** `opportunity_result` of a blocked resume (section 8.2); null when the turn is no opportunity or shows. */
internal fun ResumeDecision.opportunityResult(): String?   // SUPPRESSED suppressed, BUSY busy, THROTTLED throttled, NOT_READY not_ready
```

`STARTUP` (câu hỏi 7): người dùng rời app trong lúc màn loading còn chờ hay đang hiện quảng cáo mở app, rồi quay lại: lượt im lặng như lần đầu (không ghi, không nạp), để không có app open của resume rồi app open của màn loading liền nhau. `AdsController` giữ `startupFinished` (`MutableStateFlow(false)`), bật khi `awaitStartupAd` trả `null`, khi hàm hoàn tất của `showStartupAd` chạy, và chậm nhất 45 giây sau `start()` (trên hạn 40 giây của A-03, để app không gọi màn loading thì resume không tắt mãi).

Luật ghi cơ hội: một lượt resume ghi tối đa một `ad_opportunity`, chỉ khi `opportunityResult() != null` **và** `placement != null`. Nhờ đó `SUPPRESSED` lúc resume đang tắt không phải cơ hội (L-A05, mục 8.2 "resume tắt không phải cơ hội", câu hỏi 3). `SHOW` không ghi ở đây: hàm hiện (`showAppOpen`, `showInterstitialTurn`) ghi `shown`, hay kết quả bị chặn của chính nó nếu trạng thái đổi giữa lúc quyết và lúc hiện, nên mỗi lượt vẫn đúng một kết quả. Inter của resume không có `THROTTLED` (AS-01): hàm không nhận tham số `ads_interval` nào.

**5. Bộ điều phối lượt resume** (`ResumeAdTurns.kt`, mới, internal, chỉ chạy trên luồng chính):

```kotlin
internal interface ResumeAdPort {
    /** The gate read now, with firstForeground false (ResumeAdTurns owns that flag). */
    fun readGate(): ResumeGate
    /** `load_state` of [placement], taken now (section 8.2). */
    fun loadStateOf(placement: AdPlacement): String
    fun recordOpportunity(placement: AdPlacement, result: String, loadState: String)
    fun loadIfNeeded(placement: AdPlacement)                       // loadFullScreenAdIfNeeded (F-04)
    /** [resumeAdPlacement] of the flags read now, and the block read now (checked again right before showing). */
    fun placementNow(): AdPlacement?
    fun suppressedNow(): Boolean
    fun showAppOpen(activity: Activity, placement: AdPlacement)
    fun showInterstitial(activity: Activity, placement: AdPlacement, combo: Boolean)
}

internal class ResumeAdTurns(private val port: ResumeAdPort, private val logger: AppLogger) {
    fun onProcessForegroundChanged(foreground: Boolean)
    fun onAppActivityResumed(activity: Activity)
}
```

- Lên foreground: `decideResumeAd(port.readGate().copy(firstForeground = !seenForeground))`, rồi `seenForeground = true` (tiêu cờ ở mọi nhánh, A-10). `FIRST_FOREGROUND`, `STARTUP`, `NOT_SERVING`, `OFF`: chỉ log. `SUPPRESSED`, `BUSY`, `THROTTLED`: chụp `loadStateOf`, ghi cơ hội nếu có vị trí, không nạp (A-13). `NOT_READY`: chụp `loadStateOf`, ghi cơ hội, rồi `loadIfNeeded` (thứ tự này để `load_state` chụp trước lượt nạp, mục 8.2). `SHOW`: giữ `pending = placement`.
- Xuống nền: `pending = null` (lượt chưa kịp hiện thì bỏ, không ghi gì).
- Activity của app hiện lại: `pending` rỗng thì không làm gì (mỗi lượt foreground hiện tối đa một lần; Activity hiện lại sau khi đóng quảng cáo không mở lượt mới). Có `pending`: xóa nó; `suppressedNow()` thì ghi `suppressed` cho vị trí đó (form UMP vừa mở ở bước hỏi lại đồng ý), không hiện; `placementNow() != pending` (khóa tắt hay đổi loại giữa chừng, L-061) thì log, không ghi gì, không hiện; còn lại `APP_OPEN` thì `showAppOpen(activity, p)`, `INTERSTITIAL` thì `showInterstitial(activity, p, combo = true)`.
- Log thẻ `Ads` ở mỗi lượt: `Resume ad <decision>: <mã vị trí hoặc none>`, `Resume ad dropped: background`, `Resume ad dropped: switched`, `Resume ad shown on resume: <mã>`. Không ghi mã đơn vị.

**6. Thứ tự việc khi Activity của app hiện lại** (`ActivityResumeSteps.kt`, G-06): thêm bước `showResumeAd: (Activity) -> Unit` giữa `regatherConsent` và `releaseLeaveMark`: gỡ kẹt (F-03), hỏi lại đồng ý (S-09), hiện lượt resume đang chờ, rồi nhả dấu rời app. Dấu rời app (kể cả dấu bấm quảng cáo) đã được đọc lúc lên foreground, trước bước này, nên nhả sau là đúng A-14.

**7. Bấm quảng cáo thì bỏ lượt resume kế tiếp** (A-14):

- `AdTelemetry.kt`: tham số hàm dựng cuối `private val onClicked: () -> Unit = {}`; `clicked(format, placement)` gọi `onClicked()` ở dòng đầu, trước `lifecycleOn(format)` (lượt bấm luôn đặt dấu, kể cả khi số liệu vòng đời tắt), bọc `try/catch` như bộ ghi số liệu để lỗi không làm vỡ callback của SDK. Mọi định dạng đã gọi hàm này: `AdsController.kt` (interstitial dòng 609, app open dòng 664, `NativeAdMetrics.onClicked` dòng 992 cho native mọi khe và native toàn màn hình), `BannerAdView.kt` dòng 127 (`grep -n "lick" core/ads/src/main/kotlin/.../*.kt`, 2026-10-25); rewarded của T-031 cũng gọi `telemetry.clicked`.
- `ResumeSuppression.kt`: `fun markAdClicked()` đặt cùng `leaveMark` như `markLeavingApp`, log `Resume ads held: ad_click` (hằng `AD_CLICK`); nhả ở `onActivityResumed` như dấu rời app (G-05).

**8. Nối trong `AdsController.kt`:**

- Dựng `private val resumeAds = ResumeAdTurns(port = object : ResumeAdPort { ... }, logger)`:
  - `readGate()`: `startupFinished = startupFinished.value`; `serving = canRequestAds.value`; `suppressed = resumeAdsSuppressed.value`; `placement = latestGlobalFlags.value?.let(::resumeAdPlacement)` (chưa đọc khóa thì coi là tắt); `busy = presence.isBusy(now)`; `appOpenLockPassed = appOpenLockPassed(lastAppOpenClosedAt, now)`; `ready = placement != null && isFullScreenAdReady(placement)`; `now = clock.nowMillis()`.
  - `loadStateOf = opportunityLoadState`; `recordOpportunity` gọi `telemetry.opportunity(placement.format, placement, result, loadState)`; `loadIfNeeded = loadFullScreenAdIfNeeded`.
  - `showAppOpen`: `scope.launch { showAppOpen(activity, placement, onFinished = {}) }` (hàm của T-024: cổng đồng ý, khóa vị trí, chồng nhau, khóa 1 giây, khe app open có hạn 1 giờ, ghi `shown` hay kết quả bị chặn).
  - `showInterstitial`: `scope.launch { showInterstitialTurn(activity, placement, combo, onFinished = {}) }` (hàm của T-026; `combo = true` nối `NativeFullScreenTurns.afterInterstitial(RESUME_INTER, ...)` khi `is_show_native_after_inter` bật lúc inter hoàn tất, K-01).
- `telemetry` dựng với `onClicked = { resumeSuppression.markAdClicked() }`.
- `startupFinished` (bước 4, câu hỏi 7): `awaitStartupAd` đặt `true` khi trả `null`; `showStartupAd` đặt `true` trong hàm hoàn tất (trước `onFinished`); `start()` hẹn `scope.launch { delay(45_000); startupFinished.value = true }`.
- Để test controller (mục 4): `internal fun readResumeGate(): ResumeGate` (chính hàm mà cổng gọi) và `internal fun onProcessForegroundChanged(foreground: Boolean)` (thân của bộ nghe trong `start()`), như T-026 tách ba hàm vòng đời `internal`.
- `start()`: listener của `AdsActivityTracker` thêm `override fun onProcessForegroundChanged(foreground: Boolean) = resumeAds.onProcessForegroundChanged(foreground)`; `resumeSteps` thêm `showResumeAd = { resumeAds.onAppActivityResumed(it) }`.
- Bỏ `startedActivities`, `onAppForeground()`, `onAppBackground()` (chỉ ghi log, KDoc ghi "kept for the resume ad, T-025"; foreground nay do `AdsActivityTracker` theo dõi từ `start`, HD-01). `StartedActivityCounter` vẫn dùng trong `AdsActivityTracker`. KDoc của lớp: đoạn resume (T-025), bỏ `onAppForeground`, `onAppBackground` khỏi đoạn đầu.

**9. Vỏ app** (`MainActivity.kt`): bỏ lời gọi `ads.onAppForeground()` trong `onStart` và hàm `onStop` (chỉ gọi `ads.onAppBackground()`). `networkDialog.onAppForeground()` giữ nguyên. Không thêm trạng thái nào ở vỏ app.

**10. Tài liệu:**

- `docs/ops/ads-guide.md`: dòng 40 (bảng định dạng: resume "Có (T-025)", vị trí toàn app `resume`, `resume_inter`, không `is_show_<mã>`, nối combo khi `is_show_native_after_inter` bật); dòng 119 (bấm bất kỳ quảng cáo nào thì bỏ lượt quay lại kế tiếp; xoay màn hình hay quảng cáo toàn màn hình che app không tính là quay lại); dòng 197 (staging: `resume`, `resume_inter` bật bằng `show_resum_ads`, đã bật sẵn); mục thử trên staging (cách thử resume, dòng log); dòng 206 (danh sách phát hành: mã production cho `resume`, `resume_inter`).
- `docs/ops/ads-business-rules.md`, spec: task **không** sửa. A-15, MC-09, PL-02 và dòng vị trí của spec mục 5 do người sửa ở lượt sửa spec (câu hỏi 1).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Bước 1: dòng `RESUME`, `RESUME_INTER`; KDoc của `AdFormat.APP_OPEN` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAdChoice.kt` | Bước 2: `resumeAdPlacement`, nhánh resume trong `placementSwitch` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAds.kt` | Bước 2: `StartupAdTurn` chỉ nhận `OPEN_APP`, `OPEN_APP_INTER` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ProcessForegroundCounter.kt` | Bước 3; (dự phòng) mục 3.4: báo về nền trễ 700 ms |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsActivityTracker.kt` | Bước 3: bộ đếm của cả process, `Listener.onProcessForegroundChanged`; (dự phòng) mục 3.4: truyền `scope` cho bộ đếm |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdRules.kt` | Bước 4: `ResumeDecision`, `ResumeGate`, `decideResumeAd`, `opportunityResult` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdTurns.kt` | Bước 5: `ResumeAdTurns`, `ResumeAdPort` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ActivityResumeSteps.kt` | Bước 6: bước `showResumeAd` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` | Bước 7: tham số `onClicked`, gọi ở dòng đầu của `clicked` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeSuppression.kt` | Bước 7: `markAdClicked`, hằng `AD_CLICK` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 8: cài `ResumeAdPort`, `resumeAds`, `onClicked` của `telemetry`, listener và `resumeSteps`, `startupFinished`, `internal fun readResumeGate`, `onProcessForegroundChanged`, bỏ `startedActivities`, `onAppForeground`, `onAppBackground`; KDoc |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | Bước 9: bỏ `ads.onAppForeground()`, hàm `onStop` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdRulesTest.kt` | Cổng và thứ tự cổng (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdTurnsTest.kt` | Bộ điều phối với `FakeResumeAdPort` trong cùng file (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ProcessForegroundCounterTest.kt` | Bộ đếm thuần (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdLifecycleTest.kt` | Robolectric: `AdsActivityTracker` thật, `ResumeAdTurns` thật, `ResumeSuppression` thật, `ActivityResumeSteps` thật, cổng giả; dựng lại Activity, về nền, Activity của SDK che app (mục 4). Bộ lọc `AppActivityFilter { it !is FakeSdkActivity }`, vì `ActivityScenario.launch(ComponentActivity::class.java)` (Activity có sẵn trong manifest test, đã chạy ở mục 4.1) không nằm trong gói của test |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Robolectric, `AdsController` thật dựng như `FullScreenAdsTest.kt:106` (mục 4, L-072) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsActivityTrackerTest.kt` | Test foreground của cả process (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ActivityResumeStepsTest.kt` | Danh sách thứ tự có thêm bước `showResumeAd` (dữ liệu của test cũ, ADR-0029 quyết định 2) và test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeSuppressionTest.kt` | Test dấu bấm quảng cáo (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | Test `onClicked` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StartupAdChoiceTest.kt` | Test khóa của vị trí resume (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Test vị trí resume, `StartupAdTurn` từ chối vị trí resume; dữ liệu của `appWidePlacements_areExactlyOpenApp` và `intervalAttributes_defaultAndValidated` (đỏ ở chạy thử, mục 4.1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Dữ liệu của `configuredUnits_areParsedPerPlacementKey` (đỏ ở chạy thử) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Dữ liệu của `allKeys_haveUniqueNames` (đỏ ở chạy thử) |
| Sửa | `docs/ops/ads-guide.md` | Bước 10 |

Người đọc các file bị đổi (L-020), tra ngày 2026-10-25:

- `AdPlacement.kt`: `scripts/release.sh` (đọc dòng khai báo một dòng, đòi mã production cho mọi vị trí không bắt đầu bằng `gallery_`: hai dòng mới viết một dòng; mã production là việc của người, ghi ở `ads-guide.md`); `scripts/status.py` (in mã và định dạng mỗi vị trí lên trang tiến độ: thêm hai dòng, không lỗi); `scripts/tests/run.sh` dùng bản sao `scripts/tests/fixtures/release/AdPlacement.kt`, không đọc file thật. Test lặp `AdPlacement.entries` đã chạy thật với hai dòng mới (mục 4.1): 4 test đỏ, đều ở `:core:ads`, đều đã có trong bảng trên; `:app` (staging) 85 test, các module khác, 0 đỏ (`RemoteConfigKeysTest`, `StagingRemoteValuesModuleTest`, `AdsFlagsTest`, `AdPreloadPlanTest` qua).
- Bảng khóa sinh tự động (`docs/remote-config-keys.md`, `docs/remote-config-template.json`): không đổi (vị trí toàn app không có khóa; `RemoteConfigKeysTest` qua ở chạy thử).
- `AdTelemetry.kt`, `AdsActivityTracker.kt`, `ActivityResumeSteps.kt`, `ResumeSuppression.kt`, `StartupAds.kt`, `StartupAdChoice.kt`: chỉ `:core:ads` và test của nó đọc; `grep -rn` trên `scripts/`, `.github/` không thấy. `AdTelemetry(` dựng ở `AdsController.kt` và `AdTelemetryTest.kt`: tham số mới có mặc định.
- `AdsController.onAppForeground`, `onAppBackground`: chỉ `MainActivity.kt` gọi (`grep -rn` trên `app`, `core`, `feature`, `docs`, `scripts`); `NetworkDialogController.onAppForeground` là hàm khác, giữ.
- `StartupAdTurn(`: `LoadingRouteTest`, `LoadingViewModelTest`, `FullScreenAdsTest`, `AdPlacementTest` chỉ dựng với `OPEN_APP`, `OPEN_APP_INTER`, vẫn qua.
- `docs/ops/ads-business-rules.md`, spec: task không sửa (bước 10).
- Ảnh chuẩn, `tokens.json`: không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Khóa Remote Config dùng lại khóa đã có trong spec mục 6 và `ads-business-rules.md` mục 10.1: `show_resum_ads`, `resume_ad_type`, `is_show_native_after_inter`, `is_show_ads`. Không thêm khóa (không có `resume_ads_interval`, mục 10.1, ghi chú cuối). Staging đã bật `show_resum_ads`, `is_show_native_after_inter` bằng giá trị cục bộ (`StagingRemoteValuesModule.globalAdSwitchesOn`, T-019); `resume_ad_type` giữ `aoa` như production.
- Quảng cáo: vị trí `resume` của spec mục 5 (KB-14), dòng toàn app `resume` (`app_open`). **Spec chưa có `resume_inter`** (`interstitial`, bỏ cổng `ads_interval`): spec mục 5 chỉ có `resume`, A-15 và PL-02 nói vị trí của resume luôn là `resume`; cần người sửa spec và xác nhận lại trước khi task chạy (câu hỏi 1, mục 7). Điểm chặn 1: thêm vị trí, quảng cáo hiện mỗi lần quay lại app khi khóa bật. Production cần hai mã đơn vị mới.
- Sự kiện phân tích: không thêm tên sự kiện. `ad_opportunity` có thêm giá trị `ad_placement` = `resume`, `resume_inter` và lần đầu dùng `opportunity_result` = `suppressed` (đã có trong mục 8.2); `ad_impression_placement`, `ad_load_*`, `ad_show_fail`, `ad_clicked` của hai vị trí qua hàm có sẵn. Điểm chặn 4.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `Application.ActivityLifecycleCallbacks`, `Activity.isChangingConfigurations` không cần quyền trên API 26 tới 36.
- **Thư viện và SDK:** Không thêm. Code mới chỉ dùng lớp đã có trên classpath biên dịch của `:core:ads`: `android.app.Activity`, `Application` (SDK), `kotlinx.coroutines`, `play-services-ads` (`libs.play.services.ads`, đã có). Test dùng `androidx.test.core` (`ActivityScenario`, `ApplicationProvider`) và Robolectric đã có trong cấu hình test chung (chạy thử biên dịch và chạy được, mục 4.1). Không dùng `ProcessLifecycleOwner`: thư viện vòng đời process của AndroidX chưa có trong `gradle/libs.versions.toml`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** bộ đếm dựa vào `isChangingConfigurations` lúc `onStop` (bước 3). Chạy thử thấy giá trị `true` trên Robolectric cho cả `ActivityScenario.recreate()` và `ActivityController.recreate()`, nhưng vỏ app sau này có thể dựng lại Activity theo cách khác (ví dụ `Activity.recreate()` từ đổi ngôn ngữ riêng của app) mà giá trị khác.
- **Điều kiện chuyển:** `ResumeAdLifecycleTest.recreate_showsNothing` hay `AdsActivityTrackerTest.processForeground_recreate_reportsNothing` vẫn đỏ vì có sự kiện về nền rồi lên lại trong lúc dựng lại, sau 2 lần sửa.
- **Cách làm thay:** như `ProcessLifecycleOwner`: `ProcessForegroundCounter` không báo về nền ngay khi bộ đếm về 0 mà hẹn 700 ms (đồng hồ của coroutine, `scope` của `AdsController` truyền vào); Activity bắt đầu trong 700 ms đó thì hủy hẹn và không báo gì. Test và tiêu chí ở mục 4 giữ nguyên (test chạy trên `runTest` với thời gian ảo, đối chứng: về nền thật thì báo sau đúng 700 ms). Chỉ sửa `ProcessForegroundCounter.kt` và chỗ dựng nó trong `AdsActivityTracker.kt`.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-14 tiêu chí 1, A-12 | Unit test | `StartupAdChoiceTest.resumeAdPlacement_followsResumeKeys`: `show_resum_ads` bật, `resume_ad_type = aoa` thì `RESUME`; `inter` thì `RESUME_INTER`; `INTER`, rỗng thì `RESUME`; `show_resum_ads` tắt thì `null`; `is_show_ads` tắt thì `null` |
| KB-14 tiêu chí 1, khóa của vị trí | Unit test | `StartupAdChoiceTest.placementSwitch_resume_followsResumeKeys`: `RESUME` `true`, `RESUME_INTER` `false` khi loại `aoa`; đảo lại khi `inter`; tắt `show_resum_ads` giữa chừng thì cả hai phát `false` (L-061); đối chứng: `OPEN_APP` vẫn theo `show_open_ads` và không đổi khi `show_resum_ads` đổi. `startupAdPlacement_neverResume` (mọi tổ hợp khóa) |
| KB-14 tiêu chí 1, vị trí | Unit test | `AdPlacementTest.resumePlacements_areAppWide`: mã `resume` (`APP_OPEN`), `resume_inter` (`INTERSTITIAL`, `ignoreInterval`, `recordInterval`), `appWide`, không `stagingOnly`, `AdsSettings.all()` không có `is_show_resume`, `is_show_resume_inter`. `startupTurn_rejectsResumePlacements` (`StartupAdTurn(RESUME)`, `StartupAdTurn(RESUME_INTER)` ném `IllegalArgumentException`; đối chứng `OPEN_APP`, `OPEN_APP_INTER` qua) |
| KB-14 tiêu chí 1, hiện trên Activity hiện lại | Unit test | `ResumeAdTurnsTest.show_waitsForActivityResume_thenShowsAppOpenOnce`: lên foreground lần hai với cổng qua hết thì chưa gọi hiện; `onAppActivityResumed(a)` thì `showAppOpen(a, RESUME)` đúng một lần; `onAppActivityResumed(a)` lần nữa (đóng quảng cáo xong) thì không gọi thêm |
| KB-14 tiêu chí 1, nối combo | Unit test | `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`: vị trí `RESUME_INTER` thì `showInterstitial(a, RESUME_INTER, combo = true)`, không gọi `showAppOpen`. Combo chạy hay không theo `is_show_native_after_inter` lúc inter hoàn tất là test của T-026 (`NativeFullScreenTurnsTest.comboOn_afterInter_opensNative_finishesOnlyOnClose`, `comboOff_...`) |
| KB-14 tiêu chí 2, A-14 (đặt dấu) | Unit test | `AdTelemetryTest.clicked_runsOnClicked_evenWhenLifecycleLogsOff`: `onClicked` đếm 1 khi khóa nhóm tắt và chưa phục vụ, không có sự kiện `Clicked`; đối chứng: khóa nhóm bật, đang phục vụ thì cả `onClicked` và sự kiện. `clicked_onClickedThrows_isSwallowed` |
| KB-14 tiêu chí 2, A-14 (một dấu, nhả sau một lượt) | Unit test | `ResumeSuppressionTest.adClick_suppressesUntilNextActivityResume`: `markAdClicked()` thì `resumeAdsSuppressed` `true`; `onActivityResumed()` thì `false`. `adClickTwice_keepsOneMark` (hai lần bấm, một lần hiện lại thì hết chặn). `adClick_doesNotReleaseHolds` (đang có `hold` thì hiện lại vẫn chặn tới khi vỏ app báo, G-01) |
| KB-14 tiêu chí 2, cả vòng | Compose UI test (Robolectric) | `ResumeAdLifecycleTest.adClick_skipsNextReturn_thenNextReturnShows`: Activity của app chạy (lần đầu đã tiêu); `markAdClicked()`; `moveToState(CREATED)` rồi `RESUMED`: không gọi hiện, cơ hội `suppressed` vị trí `resume` ghi đúng một lần; về nền rồi lên lại lần nữa: gọi hiện đúng một lần |
| KB-14 tiêu chí 3 | Unit test | `ResumeAdRulesTest.suppressed_blocksBeforeEverythingButFirstAndServing` (`suppressed = true` với mọi cổng sau qua thì `SUPPRESSED`); `ResumeAdTurnsTest.suppressed_recordsSuppressed_loadsNothing`; `suppressedAtResume_recordsSuppressedOnce_showsNothing` (quyết lúc foreground là `SHOW`, `suppressedNow()` thành `true` trước `onAppActivityResumed`: không hiện, đúng một cơ hội `suppressed`) |
| KB-14 tiêu chí 3, form đồng ý (KB-11) | Unit test | `ActivityResumeStepsTest.resumeAdRunsAfterConsentRegather_beforeLeaveMarkRelease`: thứ tự `recoverStuck`, `regatherConsent`, `showResumeAd`, `releaseLeaveMark`. `unstickRunsBeforeConsentRegather` cập nhật danh sách (dữ liệu) |
| KB-14 tiêu chí 4 (đang có toàn màn hình) | Unit test | `ResumeAdRulesTest.busy_beforeLockAndReady`; `ResumeAdTurnsTest.busy_recordsBusy_loadsNothing` |
| KB-14 tiêu chí 4 (khóa 1 giây) | Unit test | `ResumeAdRulesTest.lock_appOpenOnly_throttled` (`RESUME`, khóa chưa qua thì `THROTTLED`); `lock_ignoredForInter` (`RESUME_INTER`, khóa chưa qua, sẵn sàng thì `SHOW`: AS-01); `lock_beforeNotReady` (khóa chưa qua và chưa sẵn sàng thì `THROTTLED`, không `NOT_READY`); `ResumeAdTurnsTest.throttled_recordsThrottled_loadsNothing` |
| KB-14 tiêu chí 4 (quảng cáo toàn màn hình che app không phải quay lại) | Compose UI test (Robolectric) | `ResumeAdLifecycleTest.sdkActivityCoversApp_thenAppReturns_noTurn`: Activity của SDK giả (`FakeSdkActivity`) bắt đầu rồi Activity của app dừng, rồi SDK đóng và app hiện lại: không quyết lượt nào (cổng giả đếm `readGate` 0 lần). Đối chứng trong cùng test (L-055): sau đó Home thật (Activity của app `pause().stop()`) rồi mở lại (`start().resume()`): `readGate` đúng 1 lần, gọi hiện 1 lần. Thêm: `AdsActivityTrackerTest.sdkActivity_isIgnored` có sẵn cho thấy `foreground` của Activity app về `false` trong chuỗi che |
| KB-14 tiêu chí 5, A-13 | Unit test | `ResumeAdTurnsTest.notReady_recordsNotReadyWithStateBeforeLoad_thenLoads`: thứ tự lời gọi `loadStateOf`, `recordOpportunity(RESUME, "not_ready", "idle")`, `loadIfNeeded(RESUME)`; `blockedEarlier_neverLoads` (mỗi kết quả `SUPPRESSED`, `BUSY`, `THROTTLED`, `OFF`, `NOT_SERVING`, `STARTUP`: `loadIfNeeded` 0 lần; đối chứng trong cùng test, cùng cổng giả: mở cổng chặn đó, để lại `ready = false`, thì lượt kế tiếp `loadIfNeeded` đúng 1 lần) |
| KB-14 tiêu chí 6, A-10 | Unit test | `ResumeAdTurnsTest.firstForeground_isSkipped_recordsNothing` (cổng qua hết nhưng `ready = false`: lượt đầu không ghi, không nạp; đối chứng trong cùng test: về nền rồi lên lại với cùng cổng thì ghi `not_ready` 1 lần và nạp 1 lần); `firstForeground_consumedWhileNotServing` (lần đầu với `serving = false`, lần hai với cổng qua thì `SHOW`: cờ đã tiêu); đối chứng: lần hai với cổng như lần đầu thì `NOT_SERVING`, không ghi gì, không nạp |
| KB-14 tiêu chí 6, cả vòng | Compose UI test (Robolectric) | `ResumeAdLifecycleTest.launch_isFirstForeground_noShow` (`ActivityScenario.launch` với cổng qua hết: `readGate` 1 lần, không gọi hiện; đối chứng trong cùng test: `moveToState(CREATED)` rồi `RESUMED` thì gọi hiện 1 lần); `backgroundThenForeground_showsOnResumedActivity` (`moveToState(CREATED)` rồi `RESUMED`: `showAppOpen` một lần với chính Activity của scenario) |
| Xoay màn hình không phải quay lại (L-061) | Compose UI test (Robolectric) | `ResumeAdLifecycleTest.recreate_showsNothing`: sau lần đầu, `scenario.recreate()` hai lần: `readGate` 0 lần, không gọi hiện; đối chứng trong cùng test: về nền rồi lên lại thì gọi hiện một lần. `AdsActivityTrackerTest.processForeground_recreate_reportsNothing` (cả `ActivityScenario.recreate()` và `ActivityController.recreate()`); `processForeground_backgroundAndBack_reportsFalseThenTrue`; `processForeground_sdkActivityCoversApp_staysTrue` |
| Bộ đếm của cả process | Unit test | `ProcessForegroundCounterTest.firstStart_isForeground`; `handOverBetweenActivities_reportsNothing` (Activity mới bắt đầu trước khi Activity cũ dừng); `lastStop_isBackground`; `configChangeStopThenStart_reportsNothing`; `configChangeThenRealStop_reportsBackground` (dựng lại xong, rồi về nền thật thì báo; đối chứng cờ `restarting` đã xóa); `stopWithoutStart_neverNegative` |
| A-11, thứ tự cổng | Unit test | `ResumeAdRulesTest.order_firstBeatsAll`, `startup_beforeServing`, `notServing_beforeSuppressed`, `suppressed_beforeOff`, `off_beforeBusy`, `busy_beforeLockAndReady`, `allPass_show`: mỗi test đặt cổng đang xét chặn và **mọi** cổng sau cũng chặn, rồi đối chứng: mở cổng đang xét thì ra kết quả của cổng kế tiếp |
| A-11, không có cổng thời gian | Unit test | `ResumeAdTurnsTest.immediateReturn_isNotTimeGated`: về nền rồi lên lại ngay (không có đồng hồ trong cổng) thì `SHOW`; `ResumeGate` không có trường thời gian ở nền hay `ads_interval` (kiểm khi đọc code) |
| Mục 8.2, L-A05, câu hỏi 3 | Unit test | `ResumeAdRulesTest.opportunityResult_mapping` (`FIRST_FOREGROUND`, `STARTUP`, `NOT_SERVING`, `OFF`, `SHOW` ra `null`; `SUPPRESSED` `suppressed`, `BUSY` `busy`, `THROTTLED` `throttled`, `NOT_READY` `not_ready`). `ResumeAdTurnsTest.suppressedWhileOff_recordsNothing` (đối chứng trong cùng test: bật thì ghi `suppressed`); `off_recordsNothing` (cổng khác qua hết, `placement = null`: không ghi, không nạp, không hiện; đối chứng trong cùng test: `placement = RESUME`, `ready = false` thì ghi `not_ready` 1 lần) |
| Mục 8.2, mỗi lượt một kết quả (L-068) | Unit test | `ResumeAdTurnsTest.twoReturns_eachAtMostOneOpportunity`: hai lượt foreground liền nhau (một `BUSY`, một `NOT_READY`): đúng hai cơ hội, mỗi lượt một. `background_beforeResume_dropsTurn_recordsNothing` (quyết `SHOW`, về nền trước khi Activity hiện lại, rồi Activity hiện lại: không hiện, không ghi; đối chứng trong cùng test: lên foreground lại rồi Activity hiện lại thì hiện 1 lần). Lượt hiện của feature chồng lên lượt resume: cổng chồng nhau của `presence.tryBegin` trong hàm hiện có sẵn quyết (test của T-022, `FullScreenAdPresenceTest`) |
| L-061, khóa tắt giữa chừng | Unit test | `ResumeAdTurnsTest.switchedOffBeforeResume_showsNothing_recordsNothing` (`placementNow()` thành `null`: không hiện, không ghi; đối chứng trong cùng test: bật lại, về nền, lên foreground, Activity hiện lại thì hiện 1 lần); `typeChangedBeforeResume_showsNothing` (`RESUME` thành `RESUME_INTER`: không gọi `showAppOpen` lẫn `showInterstitial`; đối chứng trong cùng test: lượt kế tiếp với loại mới gọi `showInterstitial(..., RESUME_INTER, combo = true)` 1 lần) |
| A-15, hiện qua hàm có hồi phục kẹt | Unit test | `ActivityResumeStepsTest.resumeAdRunsAfterConsentRegather_beforeLeaveMarkRelease` (gỡ kẹt chạy trước bước hiện resume trong cùng lần hiện lại). Lượt hiện đi qua `showAppOpen`, `showInterstitialTurn` có cổng chồng nhau và hàm hoàn tất một lần (T-022, T-024, T-026) |
| Màn loading chưa xong lượt mở app (câu hỏi 7) | Unit test | `ResumeAdRulesTest.startup_beforeServing` (`startupFinished = false` với mọi cổng sau chặn thì `STARTUP`; đối chứng: `true` thì `NOT_SERVING`); `ResumeAdTurnsTest.startupNotFinished_isSilent_thenShowsAfterFinish`: lượt đầu đã tiêu, `startupFinished = false`, cổng khác qua hết: lên foreground rồi Activity hiện lại thì không ghi, không nạp, không hiện; đối chứng trong cùng test: `startupFinished = true`, về nền, lên lại, Activity hiện lại thì `showAppOpen` 1 lần |
| Phần nối trong `AdsController` (L-072) | Unit test (Robolectric) | `AdsControllerResumeTest`, `AdsController` thật dựng như `FullScreenAdsTest.kt:106` (`FakeRemoteSettings`, `RecordingAdMetrics`, `RecordingAppLogger`), `start(application, AppActivityFilter { true })`: `adClick_raisesResumeSuppression` (`resumeAdsSuppressed` sai; `telemetry.clicked(AdFormat.BANNER, null)` thì đúng; đối chứng: `resumeSuppression.onActivityResumed()` thì về sai); `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` (`show_resum_ads` tắt: `readResumeGate().placement == null`; `onProcessForegroundChanged(true)`, `(false)`, `(true)`: `RecordingAdMetrics` không có `Opportunity`, log có `Resume ad` kết quả không phải `SHOW`; đối chứng trong cùng test: bật `show_resum_ads` thì `readResumeGate().placement == RESUME`, đổi `resume_ad_type = inter` thì `RESUME_INTER`); `readResumeGate_mapsControllerState` (trước khi đọc khóa: `placement == null`, `serving == false` (chưa đồng ý), `appOpenLockPassed == true` (chưa app open nào đóng), `ready == false`, `busy == false`, `startupFinished == false`; `markAdClicked()` thì `suppressed == true`; `awaitStartupAd()` trả `null` (khóa mở app tắt) thì `startupFinished == true`; đối chứng 45 giây: dựng controller khác, `ShadowLooper.idleFor(45 s)` thì `startupFinished == true`, `idleFor(44 s)` thì còn `false`) |
| Vị trí mới không làm hỏng dữ liệu test cũ | Unit test | Cập nhật dữ liệu (ADR-0029 quyết định 2), mỗi test vẫn kiểm phần mới như phần cũ: `AdPlacementTest.appWidePlacements_areExactlyOpenApp` (danh sách có thêm `RESUME`, `RESUME_INTER`, giữ khẳng định không `stagingOnly`); `intervalAttributes_defaultAndValidated` (tập vị trí bỏ cổng có thêm `RESUME_INTER`, khẳng định `RESUME_INTER.ignoreInterval`, `recordInterval` riêng); `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (chuỗi cấu hình thêm `resume=unit/5;resume_inter=unit/6`, đối chứng `missing == listOf(OPEN_APP, OPEN_APP_INTER, RESUME, RESUME_INTER)`); `AdsSettingsTest.allKeys_haveUniqueNames` (đối chứng `14 + AdPlacement.entries.count { !it.appWide }`). Nếu T-026 đã merge, danh sách có cả `NATIVE_AFTER_INTER` |
| Quảng cáo khi quay lại trên máy (app open, inter, combo, bấm quảng cáo, màn hệ thống, xoay) | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging (đã bật `show_resum_ads`, `is_show_native_after_inter`), đồng ý quảng cáo. Mở app, rồi Home và mở lại: thấy app open thử của Google; `adb logcat -v threadtime \| grep ' VGL\.'` có `Resume ad SHOW: resume` và `App open shown: resume`. Xoay màn hình: không có dòng `Resume ad`. Bấm vào quảng cáo native ở màn mẫu, quay lại: không có quảng cáo, log `Resume ads held: ad_click` rồi `Resume ad SUPPRESSED: resume`; Home và mở lại lần nữa: có quảng cáo. Đóng app open rồi Home, mở lại ngay trong 1 giây: `Resume ad THROTTLED`. Đặt `resume_ad_type = inter` (giá trị cục bộ của staging hay Firebase staging, như cách thử `open_ad_type` ở `ads-guide.md`): quay lại thì thấy inter rồi màn native đếm ngược 3 giây. Đang hiện inter thì Home rồi mở lại: `Resume ad BUSY` |
| Không ghi gì khi chưa đồng ý | Kiểm tra thủ công (máy thật: khác, trình chạy kịch bản chưa có lệnh đưa app về nền và mở lại, nên không viết được tệp kịch bản) | Bản staging mới cài, từ chối đồng ý; Home và mở lại hai lần: log `Resume ad NOT_SERVING: none`, không có `ad_opportunity` trong DebugView |
| Đọc lại tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | `ads-guide.md` khớp mục 3.1 và câu trả lời của câu hỏi 1; spec mục 5 và A-15, MC-09, PL-02 (người sửa) có `resume_inter` trước khi task chạy |

Mỗi đường tới một lượt resume (L-072): chỉ một đường, `AdsActivityTracker` báo process lên foreground, rồi `ResumeAdTurns.onProcessForegroundChanged`, rồi `onAppActivityResumed` từ `ActivityResumeSteps`; cả ba có test ở bảng trên, `ResumeAdLifecycleTest` chạy chúng nối với nhau. Đường tới một lượt nạp của khe resume: kế hoạch nạp sẵn, thử lại F-05, nạp lại F-06, nạp do lượt `NOT_READY` (A-13): đều qua `loadFullScreenAdIfNeeded` rồi `preloadAppOpen` hay `preloadInterstitial`, nơi đã đọc `isEnabled` (nay có nhánh resume, có test) và `mayRequestNow` (T-021, T-024). Đường tới một dấu "bấm quảng cáo": mọi callback bấm đều gọi `AdTelemetry.clicked` (danh sách ở bước 7), hàm này có test.

**Ngoại lệ viết test trước:** (1) phần nối trong `AdsController.kt` còn lại sau `AdsControllerResumeTest` (lời gọi `showAppOpen`, `showInterstitialTurn` từ cổng, `recordOpportunity` sang `telemetry.opportunity` khi đang phục vụ): cần `AppOpenAd`, `InterstitialAd` thật của SDK mà `AdsController` chỉ có từ lượt nạp của SDK (như T-022, T-024, T-026, T-031); mọi quyết định nằm ở `decideResumeAd`, `ResumeAdTurns`, `ProcessForegroundCounter`, `AdsActivityTracker`, `ActivityResumeSteps`, `ResumeSuppression`, `AdTelemetry.clicked` có test ở trên, và `ResumeAdLifecycleTest` chạy đúng các lớp đó nối như trong `AdsController`. (2) Bỏ hai lời gọi trong `MainActivity.kt` và hai hàm chỉ ghi log của `AdsController`: không có hành vi để kiểm; `scripts/check.sh` biên dịch `:app`, test trên emulator `AppLaunchTest` vẫn mở app. (3) Tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-25, trên nhánh chính `3d4f165` (worktree `wt-T-025`), không commit: (a) thêm tạm hai dòng `RESUME`, `RESUME_INTER` vào `AdPlacement.kt` đúng như bước 1; (b) một test tạm `ZzDryRunT025Test.kt` trong `core/ads/src/test` ghi chuỗi callback của `Application.ActivityLifecycleCallbacks` (kèm `isChangingConfigurations`) khi `ActivityController.recreate()`, `ActivityScenario.launch(ComponentActivity).recreate()`, `moveToState(CREATED)` rồi `RESUMED`, và khi `FakeSdkActivity` che `AppTestActivity`. Đã hoàn tác cả hai sau khi chạy.
- **Lệnh:** `export ANDROID_HOME=$HOME/Library/Android/sdk JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; ./gradlew testDebugUnitTest testStagingDebugUnitTest --continue -q`. Không chạy `scripts/check.sh`: tiêu chí E là 1 (mục 5), không có thư viện hay kỹ thuật mới cần L-042.
- **Kết quả:**
  - Biên dịch được, không lỗi biên dịch nào. `:core:ads` 448 test, 4 đỏ, đúng các test đã đưa vào mục 3.2: `AdPlacementTest.intervalAttributes_defaultAndValidated` (`resume_inter` có `ignoreInterval`), `AdPlacementTest.appWidePlacements_areExactlyOpenApp` (danh sách có thêm `RESUME`, `RESUME_INTER`), `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (`missing` có thêm hai vị trí), `AdsSettingsTest.allKeys_haveUniqueNames` (đối chứng `14 + entries.size` giả định mỗi vị trí một khóa). `:app` (staging) 85 test, `:core:analytics` 9, `:core:assets` 30, `:core:config` 39, `:core:designsystem` 22, `:core:logging` 4, `:core:network` 40, `:feature:loading` 40, `:feature:sample` 24: 0 đỏ.
  - Dựng lại Activity (cả hai cách): `paused:changing=true, stopped:changing=true, destroyed:changing=true, created, started, resumed`. `onStop` của Activity cũ chạy trước `onStart` của Activity mới và thấy `isChangingConfigurations == true`: cơ sở của bước 3.
  - Về nền rồi lên lại bằng `ActivityScenario`: `paused:changing=false, stopped:changing=false, started, resumed`: đối chứng cho `ResumeAdLifecycleTest`.
  - Activity của SDK che app: `created:FakeSdkActivity, started:FakeSdkActivity, resumed:FakeSdkActivity, paused:AppTestActivity, stopped:AppTestActivity, ..., stopped:FakeSdkActivity, destroyed:FakeSdkActivity, started:AppTestActivity, resumed:AppTestActivity`: Activity của SDK bắt đầu trước khi Activity của app dừng, nên lúc mở quảng cáo bộ đếm của cả process không về 0. Lúc đóng, chuỗi mà test tạm dựng theo `sdkActivity_isIgnored` có sẵn (SDK `pause().stop().destroy()` xong rồi app mới `start()`) có một quãng 0, tức một lần "về nền rồi lên lại" giả. Trên máy, Activity bên dưới `onStart`, `onResume` trước khi Activity trên cùng vừa đóng `onStop`, nên test `sdkActivityCoversApp_thenAppReturns_noTurn` dựng đúng thứ tự đó (SDK `pause()`, app `start().resume()`, rồi SDK `stop().destroy()`, như hai Activity của app chuyển cho nhau ở `AdsActivityTrackerTest.foreground_followsStartedActivities`). Thứ tự thật của `AdActivity` và Activity của adapter mediation chỉ kiểm được trên máy (dòng "Đang hiện inter thì Home rồi mở lại" và "đóng app open" của mục 4; câu hỏi 2).
  - Lần 2 (sau rà soát, cùng ngày, không commit): test tạm dựng `AdsController` thật như `FullScreenAdsTest.kt:106` với `FakeRemoteSettings` bật `show_resum_ads`, gọi `start(application, AppActivityFilter { true })`, `ShadowLooper.idleMainLooper()`, rồi `telemetry.clicked(AdFormat.BANNER, null)`. Lệnh: `./gradlew :core:ads:testDebugUnitTest --tests '*ZzDryRunT025ControllerTest*' -q`. Kết quả: qua; `start()` chạy được trên Robolectric (không ném), `resumeAdsSuppressed` sai trước khi bấm, `canRequestAds` sai (chưa đồng ý). Đây là cơ sở của `AdsControllerResumeTest`; hàm `readResumeGate`, `markAdClicked` chưa có nên chưa đo giá trị sau khi bấm. Đã xóa test tạm.
  - Chưa chạy (cần T-026): lượt inter của resume qua `showInterstitialTurn(..., combo = true)` và `NativeFullScreenTurns.afterInterstitial` chưa có trên nhánh chính (câu hỏi 5). Phần còn lại của plan không cần code chưa merge.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thêm hai vị trí toàn app `resume`, `resume_inter`; quảng cáo hiện mỗi lần quay lại app khi khóa bật, không có khoảng chờ, ADR-0026), 4 (số liệu: giá trị `ad_placement` mới, kết quả `suppressed` lần đầu được ghi), 7 (module core `:core:ads`, và vỏ app).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads` (ba file mới, tám file sửa), `:app` (`MainActivity`), tài liệu; bỏ hai hàm công khai của `AdsController` |
| B. Độ rõ của yêu cầu | 2 | KB-14 và A-10 tới A-15 rõ, đã chốt; nhưng vị trí `resume_inter` chưa có trong spec (câu hỏi 1, cần người sửa và xác nhận lại spec), còn lúc quyết lượt, rủi ro form đồng ý và lượt resume trong lúc màn loading (câu hỏi 2, 7) |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời ứng dụng (về nền, lên lại, dựng lại Activity, Activity của SDK che app): Robolectric mô phỏng được thứ tự callback nhưng không mô phỏng được thứ tự thật của `AdActivity`, adapter mediation, Custom Tabs; quảng cáo thật chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi mà mọi app tạo từ mẫu dựa vào (quảng cáo khi quay lại, mọi lượt bấm quảng cáo); `AdsController.kt`, `AdPlacement.kt`, `AdTelemetry.kt`, `StartupAdChoice.kt`, `StartupAds.kt`, ba test dữ liệu chung với T-026, T-031, T-040 |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn: `StartedActivityCounter`, `AdsActivityTracker`, `ActivityResumeSteps`, cổng thuần như `decideFullScreenShow`, bộ điều phối qua cổng như `StartupAdProbe` |
| **Tổng** | 12 | **Mức: Cao** (và chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (listener của tracker, `resumeSteps`, dựng `telemetry`, bỏ `onAppForeground`/`onAppBackground`), `AdPlacement.kt` (hai dòng), `StartupAdChoice.kt` (`placementSwitch`), `StartupAds.kt` (`StartupAdTurn`), `AdTelemetry.kt` (tham số `onClicked`), `AdsActivityTracker.kt`, `ActivityResumeSteps.kt`, `ResumeSuppression.kt`, `MainActivity.kt`, `docs/ops/ads-guide.md`. Hành vi mà feature dựa vào: mọi lượt bấm quảng cáo (của mọi màn) nay bỏ lượt quay lại kế tiếp; khi `show_resum_ads` bật, mỗi lần người dùng quay lại app có một quảng cáo toàn màn hình.
- Task khác có thể xung đột (đọc nhánh của từng plan ngày 2026-10-25):
  - **T-026** (nhánh `origin/claude/plan-T-026-inter-native-sau-inter`, plan `draft`, đọc lại ở commit `14c04d9`, **phụ thuộc, merge trước**): đã đối chiếu chữ ký ở bản `14c04d9`: `private suspend fun showInterstitialTurn(activity, placement, combo: Boolean, onFinished)` và `fun afterInterstitial(source: AdPlacement, onFinished: () -> Unit)` của `NativeFullScreenTurns` (hàm thường, giữ chỗ và chặn resume đồng bộ trước mọi điểm treo, K-05); T-026 cũng tách `internal fun beginInterstitialTurn(...)` và ba hàm vòng đời `internal fun onAppActivityResumed/Paused/Destroyed(activity)` của `AdsController`: bước `showResumeAd` của task này nằm trong `resumeSteps` mà `onAppActivityResumed` đó gọi, và `onProcessForegroundChanged` của task này theo cùng kiểu `internal`. T-026 §2 ghi "resume nối combo (T-025, sẽ gọi `NativeFullScreenTurns.afterInterstitial`)". Không tạo lại bộ điều phối combo, không sửa `NativeFullScreenTurns.kt`. Cùng sửa: `AdPlacement.kt` (T-026 thêm `NATIVE_AFTER_INTER` sau `OPEN_APP_INTER` và cho `appWide` nhận `NATIVE_FULL_SCREEN`; task này thêm dòng sau dòng đó, khác dòng), `StartupAdChoice.kt` (nhánh `NATIVE_AFTER_INTER` cạnh nhánh resume), `StartupAds.kt` (T-026 từ chối `NATIVE_FULL_SCREEN` trong `StartupAdTurn`; điều kiện "đúng hai vị trí lúc mở app" của task này bao luôn ý đó), `AdsController.kt` (T-026 tách `showInterstitialTurn`, dựng `nativeTurns`; task này chỉ gọi), ba test dữ liệu `AdPlacementTest`, `AdUnitIdsTest`, `AdsSettingsTest` (ai merge sau thêm vị trí của mình vào danh sách; `allKeys_haveUniqueNames` cả hai cùng đổi sang `count { !it.appWide }`), `ads-guide.md`. Ở bản `14c04d9`, T-026 không còn sửa MC-09 (chỉ K-03 của `ads-business-rules.md`); task này không sửa `ads-business-rules.md`. **Hai thay đổi spec cùng một lượt sửa của người:** `native_combo` (câu hỏi 1 của T-026) và `resume_inter` (câu hỏi 1 của plan này) đều là dòng vị trí mới ở spec mục 5 kèm sửa luật (MC-09; A-15, PL-02), nên người sửa một lần và xác nhận lại spec một lần. `ResumeSuppression`: T-026 dùng `hold(FULL_SCREEN_PAGE)` có sẵn, task này thêm `markAdClicked`: khác hàm.
  - **T-031** (nhánh `origin/claude/plan-T-031-rewarded`, plan `draft` commit `1e1bf18`): cùng sửa `AdsController.kt` (T-031: khe rewarded, `preloadNow` qua `fullScreenLoaderOf`, `fullScreenLoadState`, `recoverStuckFullScreen` gọi `rewardedTurns.onStuck()`; task này: listener, `resumeSteps`, dựng `telemetry`; khác khối), `AdTelemetry.kt` (T-031 thêm `rewardEarned`, `rewardDialog`; task này thêm tham số hàm dựng và một dòng trong `clicked`), `AdPlacement.kt` (T-031 thêm tham số cuối `noAdPolicy` và `REWARDED` trong `hasFullScreenSlot`; hai dòng của task này dùng đối số có tên nên không đổi), `ads-guide.md`. Không file nào cả hai cùng tạo. Lượt bấm rewarded đi qua `telemetry.clicked` nên tự có A-14. Rewarded bật cờ "đang hiện" khi cờ đang trống (test `RewardedTurnsTest.presenceRaisedByRewarded_loweredOnDismiss` của T-031: `presenceRaised == 1`, hạ khi đóng), nên lượt quay lại trong lúc rewarded hiện ra `BUSY` (F-01).
  - **T-040** (nhánh `origin/claude/plan-T-040-nguon-mot-quang-cao`, plan `draft`, đọc lại ở commit `e14f5c4`): cùng sửa `AdsController.kt` (T-040: `NativeAdSources`, `nativeAd(placement, format, firstRefreshDelayMillis)`, `preloadedNativeFullScreen`, `nativeAdMetrics(placement, unitId, use, format)`; bản `e14f5c4` chỉ thêm tham số `format` cho `nativeAdMetrics`, không đổi `NativeAdMetrics.onClicked`, nên lượt bấm native vẫn đi qua `telemetry.clicked`) và `AdTelemetry.kt` (`NativeLoadUse.REFRESH`): khác khối. Không file nào cả hai cùng tạo.
  - **T-027** (nhánh `origin/claude/plan-T-027-native-adskit`): sửa khối `NativeAdOptions` của `AdsController.kt`, không chạm phần của task này.
  - **T-033** (AppsFlyer, chưa có plan): không phụ thuộc.
- File cả hai plan cùng tạo (L-054): không có. `ProcessForegroundCounter.kt`, `ResumeAdRules.kt`, `ResumeAdTurns.kt` và bốn file test mới chỉ task này tạo.
- Test quét mã nguồn (L-062): task này không thêm test quét.
- Đề xuất thứ tự chạy: sau T-026 (bắt buộc, ticket `depends_on: [T-024, T-026]`). Sau T-031 và T-040 nếu cùng danh sách chạy, để gộp `AdsController.kt`, `AdTelemetry.kt` một lần ở phía task này (phần của task này nhỏ hơn); chạy trước hai task đó cũng được, khi đó chúng gộp. T-024 đã merge (`3d4f165`).

## 7. Điều kiện dừng riêng

- T-026 chưa merge khi tới lượt task này (không có `showInterstitialTurn`, `NativeFullScreenTurns` trên nhánh chính): dừng; không tự dựng combo hay gọi `showInterstitial` không nối combo.
- Cần khóa Remote Config mới cho resume (thời gian tối thiểu ở nền, `resume_ads_interval`, khóa bật riêng cho `resume_inter`): dừng; A-11 nói resume không có cổng thời gian, mục 10.1 nói không cần các khóa đó.
- Cần thư viện vòng đời process của AndroidX (`ProcessLifecycleOwner`) hay thư viện khác để theo dõi foreground của process: dừng (điều kiện 2 chung); phương án dự phòng ở mục 3.4 không cần thư viện.
- Để test qua phải cho resume bị `ads_interval` chặn, hay ghi cơ hội khi resume tắt hay ở lần đầu của process: dừng (AS-01, L-A05).
- Khi task bắt đầu mà spec ads chưa có vị trí `resume_inter` (mục 5 của spec có dòng vị trí, `status: confirmed` với ngày xác nhận sau 2026-10-25): dừng trước bước 3 (thêm vị trí ngoài spec là điểm chặn 1; plan không tự duyệt chỗ lệch với A-15, PL-02).
- Câu hỏi 1 trả lời khác đề xuất (chỉ `RESUME`, hay một mã `resume` cho cả hai loại): bước 1, 2, 5 đổi; không tự làm, dừng chờ plan phiên bản 2.

## 8. Câu hỏi cho người duyệt

1. **Sửa spec: thêm vị trí `resume_inter` cho resume loại inter? (câu hỏi đổi spec, cần người quyết và xác nhận lại spec)** Spec mục 5 (dòng vị trí `resume`) chỉ có một vị trí cho resume; A-15 nói "vị trí của resume luôn là `resume`", PL-02 chỉ liệt kê `resume`. Nhưng mã đơn vị đi theo vị trí (`ConfiguredAdUnitIds`, `scripts/release.sh`), mà app open và interstitial là hai đơn vị AdMob khác định dạng, nên một vị trí không chở được cả hai. Plan không tự duyệt chỗ lệch: task chỉ chạy khi spec đã sửa (mục 7), và task không sửa `ads-business-rules.md`.
   - Đề xuất của AI: thêm vị trí toàn app `resume_inter` (interstitial, bỏ cổng `ads_interval`, vẫn đẩy mốc), như `open_app_inter` của T-024; `ad_placement` của inter khi quay lại là `resume_inter`. Người sửa ở một lượt sửa spec: spec mục 5 thêm mục "Đổi 2026-10-2x" cho dòng vị trí `resume` (hai vị trí `resume`, `resume_inter`), rồi xác nhận lại spec; luật A-15, MC-09, PL-02 của `ads-business-rules.md` ghi hai vị trí. Gộp chung lượt sửa với `native_combo` của T-026 (câu hỏi 1 của plan đó). **Chi phí:** mọi app tạo từ mẫu phải khai mã production cho `resume` và `resume_inter`, kể cả khi để resume tắt hay chỉ dùng một loại.
   - Phương án khác 1, chỉ `RESUME` (spec đã có): resume luôn là app open; `resume_ad_type = inter` không có tác dụng trong dự án mẫu. Trái KB-14 tiêu chí 1 ("hoặc interstitial nếu chọn loại inter, có thể nối native") và A-12, nên vẫn cần người sửa spec (bỏ vế inter), nhưng không thêm vị trí; bỏ dòng `RESUME_INTER`, nhánh inter và test của nó (plan phiên bản 2).
   - Phương án khác 2: một vị trí `resume` có thêm thuộc tính "mã đơn vị inter" để `ad_placement` luôn là `resume` (đúng chữ A-15); đổi `ConfiguredAdUnitIds`, `release.sh` (việc của người) và cách khai vị trí.
2. **Lúc quyết lượt và cái gì tính là "quay lại".** AdsKit xét lượt ở `ON_START` của `ProcessLifecycleOwner` (mọi Activity của process, trễ 700 ms khi về nền).
   - Đề xuất của AI: quyết ở lúc process lên foreground (Activity đầu tiên của process `onStart`, kể cả Activity của SDK), hiện ở lần Activity của app `onResume` kế tiếp; xoay màn hình không tính (`isChangingConfigurations`); khóa rồi mở màn hình, hay Custom Tabs của trình duyệt sau khi bấm quảng cáo, là về nền thật (như AdsKit). Lượt quay lại lúc cờ "đang hiện" còn kẹt ra `BUSY`, cờ được gỡ ở bước đầu của lần hiện lại (F-03) và lần quay lại sau mới có quảng cáo. Phương án khác: quyết cả lượt ở bước hiện lại (sau khi gỡ kẹt), khi đó một quảng cáo toàn màn hình vừa đóng có thể kéo theo một quảng cáo resume nếu callback đóng của SDK tới sau `onResume` của Activity app.
   - **Rủi ro chấp nhận, form đồng ý của lượt hỏi lại:** mỗi lần lên foreground `ConsentCoordinator` hỏi lại đồng ý (S-09) không đồng bộ; chặn `CONSENT_FORM` chỉ bật trong `onUpdateSucceeded` khi UMP báo cần form (`ConsentCoordinator.kt:182`). Nếu lúc đó app open của resume đã hiện, form có thể hiện cùng lúc với quảng cáo (KB-11, trường hợp đặc biệt 1, không giữ được trong trường hợp này). AdsKit có cùng khe hở. Đề xuất của AI: chấp nhận (form ở lượt hỏi lại hiếm: chỉ khi trạng thái đồng ý đổi; lượt mở app, nơi form thường hiện, không có resume), ghi vào `ads-guide.md`. Phương án khác: `ConsentCoordinator` công bố "đang có lượt hỏi" (`running`), lượt resume chờ lượt hỏi xong (có trần, ví dụ 2 giây) rồi mới hiện, hay bỏ lượt; bỏ lượt thì gần như mọi lần quay lại đều mất quảng cáo, vì mỗi lần lên foreground đều có lượt hỏi lại.
3. **Resume đang tắt mà cũng đang bị chặn.** A-11 xét "đang bị chặn" trước "`show_resum_ads` tắt"; mục 8.2 và L-A05 nói resume tắt không phải cơ hội.
   - Đề xuất của AI: kết quả vẫn là `SUPPRESSED` (giữ thứ tự A-11 cho log), nhưng chỉ ghi `ad_opportunity` khi resume bật (có vị trí); khi tắt không ghi gì. Phương án khác: xét "tắt" trước "bị chặn".
4. **Bỏ `AdsController.onAppForeground()`, `onAppBackground()`.** Hai hàm công khai chỉ ghi log, KDoc ghi "kept for the resume ad, T-025"; foreground nay do `AdsActivityTracker` theo dõi từ `Application` (HD-01).
   - Đề xuất của AI: bỏ, cùng hai lời gọi trong `MainActivity`. Phương án khác: giữ (vỏ app không đổi), ghi KDoc là chỉ để log.
5. **Chạy thử phần cần T-026.** Lượt inter của resume nối combo cần `showInterstitialTurn` và `NativeFullScreenTurns` chưa merge (mục 4.1, "chưa chạy").
   - Đề xuất của AI: duyệt plan khi T-026 đã duyệt; phiên chạy task này sau khi T-026 merge (mục 6, mục 7). Phiên kiểm tên hàm của T-026 trên nhánh chính trước bước 3; tên khác thì đổi lời gọi trong `ResumeAdPort` (phần nối), không đổi `ResumeAdTurns` và test. Phương án khác: chờ T-026 merge rồi chạy thử lại và sửa plan thành phiên bản 2 trước khi duyệt.
6. **Kiểm thủ công không thành tệp kịch bản.** Trình chạy `scripts/device-scenario.py` chưa có lệnh đồng ý quảng cáo (chỉ `consent decline`) và lệnh đưa app về nền rồi mở lại, nên các tiêu chí của KB-14 chưa viết được thành tệp `deviceScenarios`.
   - Đề xuất của AI: kiểm trên máy thật theo mục 4 lần này; đề xuất riêng cho dự án mẫu thêm lệnh `home` và `reopen` cho trình chạy (ngoài task, vì `scripts/` dùng chung). Phương án khác: thêm hai lệnh đó trong task này (tăng phạm vi sang `scripts/`).

7. **Rời app trong lúc màn loading còn chờ hay đang hiện quảng cáo mở app.** Lần đầu của process đã tiêu ngay lúc mở app; người dùng bấm Home khi màn loading đang chờ (tới 40 giây, A-03) rồi quay lại thì lượt resume có thể hiện app open `resume` trên màn loading, rồi màn loading hiện tiếp `open_app`: hai app open liền nhau. Bản đặc tả không nói.
   - Đề xuất của AI: lượt resume im lặng (kết quả `STARTUP`: không ghi cơ hội, không nạp) tới khi lượt quảng cáo của màn loading xong: `awaitStartupAd` trả `null`, hay hàm hoàn tất của `showStartupAd` chạy, chậm nhất 45 giây sau khi `:core:ads` khởi động (để app không có màn loading không tắt resume mãi). Test `ResumeAdTurnsTest.startupNotFinished_isSilent_thenShowsAfterFinish`, `AdsControllerResumeTest.readResumeGate_mapsControllerState`. Phương án khác: để resume chạy bình thường và dựa vào khóa 1 giây (A-08) cùng cờ "đang hiện": vẫn có thể có hai app open cách nhau hơn 1 giây.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
