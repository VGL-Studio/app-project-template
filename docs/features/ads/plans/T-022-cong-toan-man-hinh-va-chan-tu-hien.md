---
ticket: T-022
title: Cổng quảng cáo toàn màn hình, mốc ads_interval, hồi phục kẹt, chặn quảng cáo tự hiện
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý 15M (scripts/token-usage.py estimate, 2026-10-24); nâng vì khoảng 30 file ở hai module
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                # người duyệt
cross_reviewer: Nhật          # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-022: CỔNG QUẢNG CÁO TOÀN MÀN HÌNH, MỐC ADS_INTERVAL, HỒI PHỤC KẸT, CHẶN QUẢNG CÁO TỰ HIỆN

## 1. Tóm tắt

Viết lại cổng quảng cáo toàn màn hình của `:core:ads` theo `docs/ops/ads-business-rules.md`, mục 3, 4 và 6: một thứ tự quyết định thuần (đồng ý, vị trí, đang bận, chưa sẵn sàng, giãn cách), cờ "đang hiện" có trần 10 phút, mốc `ads_interval` theo nhóm giãn cách chỉ ghi khi inter đóng, sống trong process và đo bằng đồng hồ đơn điệu, hồi phục kẹt khi activity hiện quảng cáo bị che rồi hiện lại, hàm hoàn tất chạy đúng một lần kể cả khi activity bị hủy. Thêm chặn quảng cáo tự hiện cho feature (ba cách dùng của G-03) trên bộ đếm do T-020 tạo, với hợp đồng vỏ app G-04 qua composable `AdsHost` mà `MainActivity` đặt ở gốc giao diện; bỏ rào chắn "chờ ngừng chạm 1 giây" (câu hỏi 9 của spec). Kiểm chứng bằng unit test với đồng hồ giả cho từng luật, test Robolectric cho đồng hồ đơn điệu và cho `AdsHost`, và kiểm thủ công trên màn xem thử của staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23; câu hỏi 9, 10, 30 đã trả lời):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | (Đổi 2026-10-23) Khoảng cách chỉ tính trong một lần mở app (KB-15). | Mốc giữ trong bộ nhớ của `FullScreenAdGuard` (một đối tượng mỗi process, trong `AdsController` singleton). Process mới bắt đầu không có mốc. T-019 đã gỡ phần lưu qua phiên của T-009. |
| KB-6 | (Đổi 2026-10-23) Không có ân hạn; lần mở đầu có app open khi bật `show_open_ads_first_open` (KB-13). | Inter đầu tiên của process không bị chặn bởi thời gian (chưa có mốc thì qua, I-01). App open là T-024. |
| KB-6 | (Đổi 2026-10-23) Không có giới hạn mỗi phiên. | Không có bộ đếm số lần; chỉ có mốc. Test: nhiều inter cách nhau đủ `ads_interval` đều qua. |
| KB-15 | Khi một interstitial (hay cả cụm interstitial và native) vừa đóng thì interstitial sau chỉ hiện khi đã qua `ads_interval` giây (mặc định 45). | I-01, I-02, I-05: mốc ghi ở callback đóng thật của SDK (dòng đầu của `onAdDismissedFullScreenContent`), không lúc hiện. Cổng mở khi `now - mốc >= ads_interval`. Đọc khoảng cách bằng `AdsFlags.intervalSeconds(placement.intervalGroup)` của T-019. Cụm inter và native là T-026: task này cho hàm ghi mốc theo nhóm mà bộ điều phối combo gọi khi đóng cả cụm (K-05). |
| KB-15 | Khi người dùng đổi giờ của máy thì khoảng cách vẫn tính đúng. | AR-01: mọi thời điểm lấy từ `MonotonicClock`, bản thật là `SystemClock.elapsedRealtime()`. Khoảng âm (đồng hồ nhỏ hơn mốc) coi như đã qua cổng. |
| KB-15 | Khi mở lại app (khởi động mới) thì khoảng cách bắt đầu lại từ đầu. | I-03: như dòng KB-6 đầu. |
| KB-15 | Không bao giờ có hai quảng cáo toàn màn hình chồng nhau; lượt bị chặn thì bỏ, không xếp hàng. | F-01: `FullScreenAdPresence.tryBegin` trả false khi đang có quảng cáo hiện; lượt đó ra `BUSY`, hàm hoàn tất chạy ngay, không có hàng đợi. Cờ chỉ hạ ở callback thật (đóng, hiện lỗi), hồi phục kẹt, hoàn tất cưỡng bức, hoặc khi quá trần 10 phút (F-02). Áp cho mọi định dạng toàn màn hình đang có (inter, native toàn màn hình). |
| KB-15 | Khi quảng cáo toàn màn hình bị kẹt (không báo đóng) mà người dùng đã về lại app thì app tự gỡ kẹt và người dùng đi tiếp được. | F-03: activity đã dùng để hiện quảng cáo bị `pause` rồi `resume` trong khi cờ còn bật: hạ cờ, ghi mốc của nhóm vị trí lúc phát hiện, chạy hàm hoàn tất đang chờ đúng một lần. Phải có `pause` trước `resume`; tín hiệu bị tiêu sau lần dùng đầu. Không tự đóng màn hình quảng cáo. Sự kiện vòng đời lấy từ `AdsActivityTracker` của T-020. G-06: việc này chạy trước việc hỏi lại đồng ý của T-020 trong cùng `onActivityResumed`. |
| KB-23 | Khi app mở màn chọn ảnh, camera, hộp thoại quyền, chia sẻ hay đường link rồi người dùng quay lại thì không có quảng cáo khi quay lại. | G-03, cách 1 (màn của hệ thống): feature gọi `rememberAdSuppression()`, `hold()` ngay trước khi mở, `release()` ở dòng đầu của callback; rời màn thì tự nhả (lưới an toàn). Cách 3 (chia sẻ, link, Play): `rememberMarkLeavingApp()` gọi sau khi mở thành công; dấu nhả ở lần activity của app hiện lại kế tiếp. Kết quả là `resumeAdsSuppressed` (nội bộ) true lúc quay lại; quảng cáo khi quay lại app (T-025) đọc cờ này. Task này kiểm ở mức cờ. |
| KB-23 | Khi một màn hình của app đang chiếm toàn màn (native sau inter, trang native giới thiệu) thì không có quảng cáo khi quay lại. | G-03, cách 2: composable `SuppressResumeAds(active)` giữ một suất khi `active` và còn trong cây giao diện. T-026, T-032 dùng nó. |
| KB-23 | Khi việc mở màn kia thất bại thì quảng cáo khi quay lại hoạt động lại từ lần sau. | Mở thất bại: feature gọi `release()` (hay lưới an toàn khi rời màn); dấu rời app chỉ đặt sau khi mở thành công, và luôn bị tiêu ở lần hiện lại kế tiếp (G-05: không bao giờ giữ quá một lượt). G-02: hạ chặn chỉ khi vỏ app báo ở lần dựng lại sau, nên cờ hạ sau một lần dựng lại. |

Luật trong phạm vi ticket mà không có tiêu chí riêng trong spec, vẫn có test: F-04 ("đã sẵn sàng chưa" không nạp, "nạp nếu cần" tôn trọng lịch thử lại), N-04 và HD-03 (hoàn tất cưỡng bức khi activity hiện quảng cáo bị hủy), MC-04 và câu hỏi 30 (nhóm riêng độc lập hai chiều; `ignoreInterval`, `recordInterval` là thuộc tính của vị trí), G-01 tới G-06.

Không thuộc task này: luật thử lại và lịch hẹn giờ (F-05, F-06, T-021: task này chỉ có trạng thái "đang chờ thử lại" để "nạp nếu cần" tôn trọng); app open và resume (T-024, T-025); combo và đếm lần inter (T-026); ghi `ad_opportunity` (T-023).

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013 (phần còn hiệu lực), ADR-0019, ADR-0024 (popup mạng chờ quảng cáo toàn màn hình đóng, `fullScreenAdShowing`), ADR-0026 (quyết định 5).

Dựa trên hai plan chưa merge khi viết plan này, dùng đúng tên:

- T-019 (`docs/features/ads/plans/T-019-khoa-remote-config-theo-adskit.md`): `AdsFlags.intervalSeconds(group: AdIntervalGroup): Flow<Long>`, `AdPlacement(key, format, intervalGroup = AdIntervalGroup.DEFAULT, nativeEvery = null)`, `AdIntervalGroup.DEFAULT`, `AdIntervalGroup.named(name, defaultSeconds)`, `internal data class AdPlacementDeclaration`, `AdFormat.APP_OPEN`, `AdFormat.REWARDED`. Sau T-019, `FullScreenAdGuard` vẫn còn (mốc ghi lúc hiện, tính cả native toàn màn hình, chờ ngừng chạm 1 giây), đúng như câu hỏi 8 của plan T-019 để dành ba điểm này cho task này.
- T-020 (`docs/features/ads/plans/T-020-dong-y-va-khoi-dong-partner.md`): `ForegroundAdSuppressor` (`acquire(source): Hold`, `suppressed: StateFlow<Boolean>`), `AdsActivityTracker` (báo `onActivityResumed`, `onActivityPaused`, `onActivityDestroyed`, `foreground`; chỉ cho activity của app qua `AppActivityFilter`, activity của SDK bị bỏ qua), `AdsController.onActivityResumed` gọi `coordinator.onActivityResumed` (hỏi lại đồng ý, S-09), `canRequestAds` là cổng đồng ý.

## 3. Giải pháp

### 3.1. Cách làm

Mọi luật thuần là lớp hay hàm không gọi Android, nhận thời điểm từ ngoài (đồng hồ giả trong test), như `FullScreenAdGuard` và `StartedActivityCounter` hiện có. `AdsController` nối chúng với SDK và vòng đời.

**1. Đồng hồ đơn điệu** (`MonotonicClock.kt`, mới): `internal fun interface MonotonicClock { fun nowMillis(): Long }`; hai nguồn thời gian được truyền vào, `internal interface TimeSources { fun elapsedRealtimeMillis(): Long; fun wallClockMillis(): Long }`, bản thật `AndroidTimeSources` trả `SystemClock.elapsedRealtime()` và `System.currentTimeMillis()`; `internal fun monotonicClockOf(sources: TimeSources): MonotonicClock` chỉ đọc `elapsedRealtimeMillis()`; `internal object SystemMonotonicClock : MonotonicClock by monotonicClockOf(AndroidTimeSources)`. Giờ của máy có trong `TimeSources` để test đổi được nó bằng nguồn giả và chứng minh đồng hồ không đọc nó (mục 4); code của task không đọc giờ của máy ở chỗ nào khác. `AdsController` dùng `SystemMonotonicClock` ở mọi chỗ của task này (AR-01). Test chạy trên JVM, không nhờ Robolectric lùi giờ hệ thống (sửa theo rà soát trước phiên, 2026-10-24).

**2. Mốc giãn cách** (`FullScreenAdGuard.kt`, viết lại; giữ tên lớp vì T-019 dùng tên này làm đối chứng trong `RemovedPacingRulesTest`):

```kotlin
internal class FullScreenAdGuard {
    /** I-01: no mark lets it through; at the boundary it lets it through; a negative gap counts as passed (AR-01). */
    fun allows(group: AdIntervalGroup, intervalMillis: Long, now: Long): Boolean
    /** I-02, I-05: called from the SDK's close callback (or stuck recovery, or the combo's close). */
    fun record(group: AdIntervalGroup, now: Long)
}

/** I-02: only interstitial-family placements that record (MC-04) move a mark; app open and rewarded never do. */
internal fun AdPlacementDeclaration.pushesIntervalMark(): Boolean
/** I-04, MC-04: the gate is read unless the placement declares ignoreInterval. */
internal fun AdPlacementDeclaration.readsInterval(): Boolean
```

- Một mốc cho mỗi nhóm (`AdIntervalGroup`): nhóm riêng chỉ đọc và ghi mốc của mình, không đọc, không ghi mốc chung, và ngược lại (câu hỏi 30). App open, resume, combo luôn ở nhóm mặc định (T-024 tới T-026 khai báo).
- Bỏ `onUserInteraction`, `Decision.Wait`, `QUIET_PERIOD_MILLIS`, `MAX_WAIT_MILLIS` (câu hỏi 9 của spec: bỏ rào chắn chờ ngừng chạm).
- Native toàn màn hình đứng riêng (định dạng của T-013, vị trí `gallery_native_full_screen`) xử như inter: đọc cổng và ghi mốc khi đóng, giữ hành vi của đợt 2 (câu hỏi 2).

**3. Thuộc tính của vị trí** (`AdPlacement.kt`): `AdPlacement` thêm `val ignoreInterval: Boolean = false`, `val recordInterval: Boolean = true` sau các tham số của T-019; `AdPlacementDeclaration` thêm hai trường cùng tên. Kiểm khai báo như T-019: giá trị khác mặc định chỉ cho `INTERSTITIAL` và `NATIVE_FULL_SCREEN`, sai thì `IllegalArgumentException` khi lớp được nạp. Mọi dòng vị trí hiện có giữ nguyên (mặc định), nên dạng dòng mà `scripts/release.sh` đọc không đổi.

**4. Cờ đang hiện** (`FullScreenAdPresence.kt`, sửa; giữ `showing` mà T-017 và `fullScreenAdShowing` dùng):

- `tryBegin(placement: AdPlacement, hostActivityId: Int, now: Long): Boolean`: false khi đang hiện và chưa quá trần (F-01); quá trần 10 phút (`SHOWING_CAP_MILLIS = 600_000`) thì coi như đã hạ, cho lượt mới (F-02).
- `onClosed()`: hạ cờ; gọi lại không đổi gì. `current: Shown?` (vị trí, activity hiện) cho hồi phục kẹt và hoàn tất cưỡng bức.
- `onShowing()` và `onClosed()` cũ giữ cho tương thích test của T-013; `onShowing()` gọi `tryBegin` với activity 0.

**5. Thứ tự quyết định** (`FullScreenShowDecision.kt`, mới):

```kotlin
internal enum class FullScreenShowDecision { SHOW, CONSENT_CLOSED, DISABLED, BUSY, NOT_READY, THROTTLED }

internal fun decideFullScreenShow(
    consentOpen: Boolean, enabled: Boolean, busy: Boolean, ready: Boolean, intervalPassed: Boolean,
): FullScreenShowDecision
```

Thứ tự: đồng ý, vị trí bật, đang bận (F-01), chưa sẵn sàng, giãn cách. Ba kết quả `BUSY`, `NOT_READY`, `THROTTLED` theo đúng thứ tự mà mục 8.2 dùng cho `ad_opportunity` (T-023 ánh xạ). `intervalPassed` là `!readsInterval || guard.allows(...)`.

**6. Hồi phục kẹt** (`StuckFullScreenRecovery.kt`, mới, F-03):

```kotlin
internal class StuckFullScreenRecovery {
    fun onActivityPaused(activityId: Int, showingHostId: Int?)
    /** True once when the host was paused and is resumed while an ad still shows; the signal is then used up. */
    fun onActivityResumed(activityId: Int, showingHostId: Int?): Boolean
}
```

**7. Hàm hoàn tất một lần** (`FullScreenCompletion.kt`, mới, N-02, N-04): `set(onFinished)`, `complete(reason)` chạy hàm đúng một lần dù đến từ SDK, hồi phục kẹt, hoàn tất cưỡng bức hay lượt bị chặn; ghi log lý do. Lượt mới thay hàm cũ chỉ khi hàm cũ đã chạy.

**8. Thứ tự việc khi activity hiện lại** (`ActivityResumeSteps.kt`, mới, G-06): `internal class ActivityResumeSteps(recoverStuck: (Activity) -> Unit, regatherConsent: (Activity) -> Unit, releaseLeaveMark: () -> Unit)`, `onResumed(activity)` chạy theo đúng thứ tự: gỡ cờ kẹt, hỏi lại đồng ý (T-020), nhả dấu rời app. `AdsController.onActivityResumed` (T-020 tạo) gọi `steps.onResumed`.

**9. Nạp nếu cần** (`FullScreenLoadState.kt`, mới, F-04):

```kotlin
internal sealed interface FullScreenLoadState {
    data object Idle : FullScreenLoadState
    data object Loading : FullScreenLoadState
    data object Ready : FullScreenLoadState
    data class WaitingRetry(val dueAtMillis: Long) : FullScreenLoadState
}
/** Never loads while loading, ready, or waiting for a retry that is not due; a retry that fell due while the consent gate was closed no longer counts as waiting (A 03:30-32). */
internal fun needsLoad(state: FullScreenLoadState, now: Long, gateOpen: Boolean): Boolean
```

`AdsController` giữ trạng thái này cho mỗi vị trí toàn màn hình (thay `loadedInterstitials` chỉ biết có hay không và `nativeFullScreenLoading`); `WaitingRetry` chưa có ai đặt cho tới T-021. Thêm `internal fun isFullScreenAdReady(placement): Boolean` (chỉ đọc, không nạp) và `internal fun loadFullScreenAdIfNeeded(placement)` (nạp khi `needsLoad`), cho T-024 (màn loading dò app open) và T-021.

**10. Chặn quảng cáo tự hiện** (G-01 tới G-05):

- `ResumeSuppression.kt` (mới, nội bộ), trên `ForegroundAdSuppressor` của T-020:

```kotlin
internal class ResumeSuppression(suppressor: ForegroundAdSuppressor) {
    /** What resume ads (T-025) read. Raised at once when a hold starts (G-02), lowered only by the shell's report. */
    val resumeAdsSuppressed: StateFlow<Boolean>
    /** G-04: the root reads the suppressor in composition and reports here on the next recomposition. */
    fun onShellReported(suppressedInComposition: Boolean)
    /** G-03, way 3: after leaving the app on purpose succeeded. */
    fun markLeavingApp()
    /** G-03 way 3, G-05: the next resume of an app activity releases the mark, whether or not the app went to background; SDK activities are filtered out by AdsActivityTracker. */
    fun onActivityResumed()
}
```

  `resumeAdsSuppressed` = `leaveMark || reportedOrRaised`, trong đó `reportedOrRaised` lên ngay khi bộ đếm từ 0 lên 1, chỉ xuống khi `onShellReported(false)`.
- `AdSuppression.kt` (mới, công khai cho feature và vỏ app; không có `LocalAdsController` thì không làm gì, như các composable khác của `:core:ads`):
  - `@Composable fun rememberAdSuppression(): AdSuppression` với `hold()` (bật ngay trước khi mở màn của hệ thống) và `release()` (dòng đầu của callback); `DisposableEffect` nhả khi composable rời màn (G-03, cách 1).
  - `@Composable fun SuppressResumeAds(active: Boolean)` giữ một suất khi `active` (G-03, cách 2).
  - `@Composable fun rememberMarkLeavingApp(): () -> Unit` (G-03, cách 3).
  - `@Composable fun AdsHost(ads: AdsController?, content: @Composable () -> Unit)`: cấp `LocalAdsController` và đọc `suppressor.suppressed` trong composition, báo `onShellReported` bằng `SideEffect` (G-04). Bản nội bộ `AdsHostContent(suppression, suppressor, content)` cho test. T-026 đặt lớp phủ combo trong cùng composable (HD-05).
- Log thẻ `Ads`: mỗi lần giữ, nhả, đặt và tiêu dấu rời app, kèm nguồn (`system_screen`, `full_screen_page`, `leave_app`, `consent_form`), không ghi tên màn hay dữ liệu người dùng.

**11. Nối vào `AdsController`:**

- Bỏ `guard.onUserInteraction`, hàm `onUserInteraction()` và vòng chờ trong `awaitFullScreenTurn`. `awaitFullScreenTurn` thành `decideFullScreenTurn(placement): FullScreenShowDecision` (`suspend`, đọc `isEnabled(placement).first()` và `flags.intervalSeconds(placement.intervalGroup).first()`).
- `showInterstitial(activity, placement, onFinished: () -> Unit = {})` và `showNativeFullScreen(placement, onFinished)`: quyết định khác `SHOW` thì gọi `onFinished` ngay (N-02, F-07), `NOT_READY` thì `loadFullScreenAdIfNeeded` (F-04, F-07). `SHOW`: `presence.tryBegin(placement, System.identityHashCode(activity), now)`, `completion.set(onFinished)`, rồi hiện. Callback đóng thật: dòng đầu `guard.record(group, now)` nếu `pushesIntervalMark` (I-05), rồi `presence.onClosed()`, `completion.complete("closed")`, nạp lại. Hiện lỗi: `presence.onClosed()`, `completion.complete("show_failed")`, nạp lại; không ghi mốc. `closeNativeFullScreenAd` như callback đóng.
- `onFullScreenShown()` bỏ (mốc không còn ghi lúc hiện).
- Từ `AdsActivityTracker` (T-020; chỉ activity của app, activity của SDK như `AdActivity` hay activity của adapter bị bộ lọc của T-020 bỏ qua, nên `pause`, `resume` của chính activity quảng cáo không gỡ cờ kẹt và không nhả dấu rời app; sửa theo rà soát trước phiên, 2026-10-24): `onActivityPaused` gọi `stuck.onActivityPaused`; `onActivityResumed` qua `ActivityResumeSteps` (gỡ cờ kẹt: hạ cờ, ghi mốc nhóm của vị trí đang hiện, `completion.complete("stuck_recovered")`); `onActivityDestroyed(activity)`: activity đang hiện quảng cáo mà cờ còn bật thì `presence.onClosed()`, `completion.complete("host_destroyed")` (N-04, HD-03; `:app` không cần gọi gì).
- `internal val resumeSuppression`, `internal val resumeAdsSuppressed` cho T-025; `consent form` của T-020 đã dùng cùng bộ đếm.
- `FullScreenAds.kt`: `rememberShowFullScreenAd(placement: AdPlacement, onFinished: () -> Unit = {})`; hàm nội bộ `rememberFullScreenAdAction` giữ chữ ký (test của T-013 dùng), `onFinished` đi qua closure. Gọi cũ không đổi.

**12. Vỏ app:**

- `MainActivity`: thay `CompositionLocalProvider(LocalAdsController provides ads)` bằng `AdsHost(ads) { ... }` (cùng chỗ, nội dung giữ nguyên, `FullScreenAdHost()` vẫn trong `AppTheme`); bỏ `onUserInteraction`. `onStart`, `onStop`, `gatherConsent` giữ nguyên. Hoàn tất cưỡng bức, gỡ cờ kẹt, nhả dấu rời app chạy từ callback vòng đời của T-020, không cần thêm hàm vào `MainActivity`.
- `AdGalleryActivity` (staging): bỏ `onUserInteraction` (hàm bị gỡ), dùng `AdsHost(ads)` như `MainActivity`.
- `docs/ops/ads-guide.md`: mục 5 thêm bước "Mở màn của hệ thống hay rời app" với ba API của G-03; mục 8 bỏ câu "hay trong 180 giây ân hạn phiên đầu" về màn xem thử, ghi "bấm lại trong 45 giây sau khi inter đóng thì không hiện".

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MonotonicClock.kt` | `MonotonicClock`, `TimeSources`, `AndroidTimeSources`, `monotonicClockOf`, `SystemMonotonicClock` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuard.kt` | Viết lại: mốc theo nhóm (I-01 tới I-05, MC-04), `pushesIntervalMark`, `readsInterval`; bỏ chờ ngừng chạm |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresence.kt` | `tryBegin`, trần 10 phút, `current` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `ignoreInterval`, `recordInterval` cho `AdPlacement` và `AdPlacementDeclaration`, kiểm khai báo |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenShowDecision.kt` | Thứ tự quyết định |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StuckFullScreenRecovery.kt` | F-03 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenCompletion.kt` | Hàm hoàn tất một lần |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ActivityResumeSteps.kt` | Thứ tự G-06 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenLoadState.kt` | F-04 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeSuppression.kt` | G-02, G-03 cách 3, G-05 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSuppression.kt` | `rememberAdSuppression`, `SuppressResumeAds`, `rememberMarkLeavingApp`, `AdsHost` (G-03, G-04) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Nối mục 3.1 bước 11; bỏ `onUserInteraction`, `onFullScreenShown`, vòng chờ |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAds.kt` | `onFinished` cho `rememberShowFullScreenAd` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | `AdsHost(ads)`, bỏ `onUserInteraction` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryActivity.kt` | `AdsHost(ads)`, bỏ `onUserInteraction` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt` | Viết lại theo mục 4 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresenceTest.kt` | Thêm F-01, F-02; giữ test cũ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Thêm test hai thuộc tính; giữ test cũ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MonotonicClockTest.kt` | JVM với nguồn thời gian giả: theo thời gian từ lúc khởi động máy, không theo giờ của máy |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenShowDecisionTest.kt` | Thứ tự quyết định |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StuckFullScreenRecoveryTest.kt` | F-03 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenCompletionTest.kt` | N-02, N-04 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ActivityResumeStepsTest.kt` | G-06 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenLoadStateTest.kt` | F-04 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeSuppressionTest.kt` | G-01 tới G-05 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdSuppressionTest.kt` | Robolectric: ba cách dùng của G-03 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsHostTest.kt` | Robolectric: vỏ app báo chặn trong composition (G-04, L-051) |
| Sửa | `docs/ops/ads-guide.md` | Mục 5: bước dùng chặn quảng cáo tự hiện; mục 8: bỏ ân hạn ở màn xem thử |
| Sửa | `docs/features/ads/tickets/T-022-cong-toan-man-hinh-va-chan-tu-hien.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-022-cong-toan-man-hinh-va-chan-tu-hien.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã tìm `git grep` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/testStaging`, `*/src/androidTest`, mọi `build.gradle.kts`):

- `AdPlacement.kt`: `scripts/release.sh` đọc dòng vị trí bằng biểu thức ở dòng 42 (`TEN("ma", AdFormat.X),`). Task không đổi dòng vị trí nào; hai thuộc tính mới có mặc định nên không dòng nào thêm tham số. `scripts/tests/run.sh` dùng file mẫu `scripts/tests/fixtures/release/AdPlacement.kt`, không đọc file thật. `AdPlacementSourceTest` của T-019 kiểm lại dạng dòng ở CI. AI không sửa `release.sh`.
- `FullScreenAdGuard.kt`: `FullScreenAdGuardTest.kt` (có trong danh sách); `RemovedPacingRulesTest.kt` của T-019 chỉ gọi `Class.forName(".../FullScreenAdGuard")` làm đối chứng, lớp vẫn còn tên này nên test giữ qua; KDoc của `FullScreenAdPolicy.kt` nhắc tên, nhưng T-019 xóa file đó.
- `FullScreenAdPresence.kt`: `FullScreenAdPresenceTest.kt` (có trong danh sách).
- `FullScreenAds.kt`: `FullScreenAdsTest.kt` gọi `rememberShowFullScreenAd(placement)` và `rememberFullScreenAdAction(placement, show)`; cả hai giữ cách gọi cũ nên không sửa. `AdGalleryScreen.kt` gọi `rememberShowFullScreenAd(placement)`, không đổi.
- `MainActivity.kt`: `AppLaunchTest` (emulator) mở `MainActivity`; tìm màn bằng `fetchSemanticsNodes`, không đổi. `scripts/run-check.py` dò chữ `MainActivity` trong ngoại lệ viết test trước (mục 4 có giải thích). `scripts/tests/run.sh` chỉ kiểm file `MainActivity.kt` tồn tại trong app mới.
- `AdGalleryActivity.kt`: `AdGallerySourceSetTest` quét chuỗi `AdGallery` ngoài `src/staging`; task không thêm chuỗi đó ở chỗ khác (L-062).
- `AdsController.kt`: constructor không đổi trong task này, nên `FullScreenAdsTest.kt` không sửa.
- `docs/ops/ads-guide.md`: không script hay test nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng. Đổi tần suất (điểm chặn 1): mốc ghi lúc inter đóng thay vì lúc hiện, theo nhóm giãn cách, bỏ chờ ngừng chạm 1 giây; thêm chặn quảng cáo tự hiện (chưa có quảng cáo khi quay lại app nào đọc cho tới T-025).
- Sự kiện phân tích: Không chạm.
- Dữ liệu: không lưu gì trên máy; mốc và cờ chỉ trong bộ nhớ.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `SystemClock.elapsedRealtime()` và callback vòng đời không cần quyền.
- **Thư viện và SDK:** Không thêm. `SideEffect`, `DisposableEffect`, `CompositionLocalProvider` (Compose, có sẵn trong `:core:ads`), `FullScreenContentCallback` (`play-services-ads`, `libs.play.services.ads`) đã có trên classpath biên dịch của `:core:ads`; `SystemClock.elapsedRealtime()` thuộc Android SDK. `MonotonicClockTest` không dùng `ShadowSystemClock`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** Không còn. Bản nháp trước dùng Robolectric lùi giờ hệ thống trong `MonotonicClockTest` và để test JVM làm phương án dự phòng; Robolectric có thể từ chối lùi giờ, hay lùi cả `System.currentTimeMillis` mà code nạp lại đọc, khiến một cài đặt sai vẫn qua. Plan dùng thẳng test JVM với nguồn thời gian giả (mục 3.1 bước 1, mục 4) (sửa theo rà soát trước phiên, 2026-10-24).
- **Điều kiện chuyển:** Không có.
- **Cách làm thay:** Không có. Dòng `AndroidTimeSources` gọi thẳng `SystemClock`, `System` không có unit test (ngoại lệ ở mục 4), như `UmpConsentPlatform` của T-020.

## 4. Kiểm thử

Unit test JVM với đồng hồ giả (số mili giây do test truyền vào), vị trí giả là `AdPlacementDeclaration` dựng trong test như T-019: `inter = ("inter_x", INTERSTITIAL)`, `sounds = ("inter_sounds", INTERSTITIAL, AdIntervalGroup.named("sounds", 30))`, `open = ("open_x", APP_OPEN)`, `rw = ("rw_x", REWARDED)`, `nfs = ("nfs_x", NATIVE_FULL_SCREEN)`. Mặc định `ads_interval` 45 giây. Test Robolectric dùng `createAndroidComposeRule<ComponentActivity>()` và `ForegroundAdSuppressor`, `ResumeSuppression` thật.

Bước 3 (test viết trước) có khung rỗng để biên dịch: `monotonicClockOf` trả `sources.wallClockMillis()` (sai có chủ ý, để `MonotonicClockTest` thất bại đúng lý do); `FullScreenAdGuard` mới với `allows` luôn true, `record` không làm gì; `decideFullScreenShow` luôn `SHOW`; `StuckFullScreenRecovery.onActivityResumed` luôn false; `FullScreenCompletion.complete` không chạy gì; `ActivityResumeSteps.onResumed` không gọi gì; `needsLoad` luôn true; `ResumeSuppression.resumeAdsSuppressed` luôn false; composable của `AdSuppression.kt` rỗng; `FullScreenAdPresence.tryBegin` luôn true; `AdPlacement` có hai thuộc tính mới không kiểm. Commit test của bước 3: mọi file test mới và phần sửa ba file test có sẵn. Test cũ của `FullScreenAdGuardTest` về chờ ngừng chạm bị xóa cùng commit test, vì luật bị bỏ theo câu hỏi 9 của spec (không phải nới test để qua; người duyệt xác nhận ở câu hỏi 4) (L-025).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-6, tiêu chí 1 và KB-15, tiêu chí 3 (I-03) | Unit test | `FullScreenAdGuardTest.newGuard_hasNoMark`: `guard.record(DEFAULT, 10_000)` rồi `allows(DEFAULT, 45_000, 20_000)` false; một `FullScreenAdGuard()` mới (process mới) `allows(DEFAULT, 45_000, 20_000)` true. Khung rỗng thất bại ở vế đầu (luôn true) |
| KB-6, tiêu chí 2 (không ân hạn) | Unit test | `FullScreenAdGuardTest.firstInterOfProcess_isAllowed`: chưa có mốc, `allows(DEFAULT, 45_000, 0)` true. Đối chứng (L-055): cùng test, sau `record(DEFAULT, 0)` thì `allows(DEFAULT, 45_000, 1)` false |
| KB-6, tiêu chí 3 (không giới hạn mỗi phiên) | Unit test | `FullScreenAdGuardTest.manyIntersSpacedByInterval_allAllowed`: 10 vòng `allows` rồi `record` cách nhau 45 000 ms, cả 10 true. Đối chứng: vòng thứ 11 sau 44 999 ms false |
| KB-15, tiêu chí 1 (I-01, biên) | Unit test | `FullScreenAdGuardTest.interval_blocksUntilElapsed_boundaryPasses`: mốc 0; `allows` ở 44 999 false, 45 000 true |
| KB-15, tiêu chí 1 (mốc lúc đóng, I-02, I-05) | Unit test | `FullScreenAdGuardTest.markIsCloseTime_notShowTime`: kịch bản "hiện lúc 0, đóng lúc 30 000" nghĩa là chỉ `record(DEFAULT, 30_000)`; `allows` ở 60 000 false, ở 75 000 true. `pushesIntervalMark_onlyInterFamily`: `inter`, `nfs` true; `open`, `rw` false; `inter` khai báo `recordInterval = false` thì false. Khung rỗng thất bại vì `allows` luôn true |
| KB-15, tiêu chí 1 (nhóm, MC-04, câu hỏi 30) | Unit test | `FullScreenAdGuardTest.customGroupInter_doesNotMoveDefaultMark`: `record(sounds.group, 0)`: `allows(DEFAULT, 45_000, 1)` true, `allows(sounds.group, 30_000, 1)` false. `defaultInter_doesNotBlockCustomGroup`: ngược lại. `ignoreInterval_skipsTheGate`: `readsInterval()` false với `ignoreInterval = true`, true khi mặc định |
| KB-15, tiêu chí 2 (AR-01) | Unit test | `MonotonicClockTest.followsElapsedTime_notWallClock`: `FakeTimeSources` (lớp trong file test, hai số do test đặt) với thời gian từ lúc khởi động 10 000 và giờ máy 1 700 000 000 000; `monotonicClockOf(fake).nowMillis()` là 10 000; lùi giờ máy 1 giờ và tăng thời gian từ lúc khởi động 5 000: 15 000. `intervalDecision_ignoresWallClockJump`: `guard.record(DEFAULT, clock.nowMillis())` ở 10 000; lùi giờ máy 1 giờ, tăng thời gian từ lúc khởi động 10 000: `allows(DEFAULT, 45_000, clock.nowMillis())` false; tăng tới 55 000: true. Khung rỗng của bước 3: `monotonicClockOf` trả `sources.wallClockMillis()`, nên test thất bại đúng lý do (đồng hồ theo giờ máy lùi về trước mốc, khoảng âm coi như đã qua, cổng mở sớm). Không dùng Robolectric hay `ShadowSystemClock`. `FullScreenAdGuardTest.clockBehindMark_isTreatedAsPassed`: `record(DEFAULT, 100_000)`, `allows(DEFAULT, 45_000, 50_000)` true |
| KB-15, tiêu chí 4 (F-01) | Unit test | `FullScreenAdPresenceTest.secondShowWhileShowing_isDropped`: `tryBegin(..., now = 0)` true, lần hai ở 1 000 false, sau `onClosed()` true. `FullScreenShowDecisionTest.busy_beatsNotReadyAndThrottled`: `busy = true`, `ready = false`, `intervalPassed = false` ra `BUSY`. `FullScreenCompletionTest.blockedTurn_completesAtOnce`: lượt không `SHOW` chạy hàm hoàn tất 1 lần ngay, không còn hàm chờ (không xếp hàng) |
| KB-15, tiêu chí 4 (F-02) | Unit test | `FullScreenAdPresenceTest.showingFlag_expiresAfterTenMinutes`: `tryBegin` ở 0; lượt mới ở 599 999 false; ở 600 000 true. Khung rỗng thất bại ở vế 599 999 (luôn true) |
| KB-15, tiêu chí 5 (F-03) | Unit test | `StuckFullScreenRecoveryTest.pausedThenResumedHost_isStuckOnce`: đang hiện trên activity 1; `onActivityPaused(1)`, `onActivityResumed(1)` true; `onActivityResumed(1)` lần nữa false (tín hiệu bị tiêu). `resumeWithoutPause_isNotStuck` (L-A03): `onActivityResumed(1)` không có `pause` trước: false. `otherActivity_isNotStuck`: pause và resume activity 2: false. `notShowing_isNotStuck`: `showingHostId = null`: false |
| KB-15, tiêu chí 5 (hoàn tất một lần) | Unit test | `FullScreenCompletionTest.runsOnce_fromAnySource`: `complete("stuck_recovered")` rồi `complete("closed")` (callback thật tới muộn): hàm chạy 1 lần. `hostDestroyed_forcesCompletion` (N-04): `complete("host_destroyed")` khi chưa chạy: 1 lần |
| G-06 | Unit test | `ActivityResumeStepsTest.unstickRunsBeforeConsentRegather`: ba lambda ghi vào một danh sách; `onResumed(activity)` ra đúng `[recoverStuck, regatherConsent, releaseLeaveMark]`. Khung rỗng thất bại vì danh sách rỗng |
| F-04 | Unit test | `FullScreenLoadStateTest.needsLoad_onlyWhenIdleOrRetryDue`: `Idle` true; `Loading`, `Ready` false; `WaitingRetry(10_000)` ở 9 999 false, ở 10 000 true. `retryDueWhileGateClosed_isNotWaiting`: `WaitingRetry(10_000)` ở 12 000 với `gateOpen = false` rồi cổng mở: `needsLoad` true (A 03:30-32). Khung rỗng thất bại ở `Loading` (luôn true) |
| Thứ tự quyết định | Unit test | `FullScreenShowDecisionTest.order`: bảng 6 dòng, mỗi dòng chỉ một điều kiện sai: `CONSENT_CLOSED` thắng cả bốn điều kiện khác sai; `DISABLED` thắng `BUSY`; `BUSY`, `NOT_READY`, `THROTTLED` theo thứ tự; đủ điều kiện ra `SHOW` |
| KB-23, tiêu chí 1 (G-02, G-03 cách 1) | Unit test | `ResumeSuppressionTest.holdRaisesAtOnce_releaseWaitsForShellReport`: `suppressor.acquire("system_screen")`: `resumeAdsSuppressed` true ngay (chưa có báo cáo); `release()`: vẫn true; `onShellReported(false)`: false. Khung rỗng thất bại ở vế đầu |
| KB-23, tiêu chí 1 (composable) | Unit test Robolectric | `AdSuppressionTest.holdThenRelease_andReleaseOnDispose`: trong `AdsHostContent`, `rememberAdSuppression()`; `hold()`: `suppressor.suppressed` true; `release()` hai lần: bộ đếm về 0, không âm; `hold()` rồi gỡ composable khỏi cây: bộ đếm về 0 (lưới an toàn) |
| KB-23, tiêu chí 1 (rời app, G-03 cách 3) | Unit test | `ResumeSuppressionTest.leaveMark_releasedOnNextActivityResume`: `markLeavingApp()`: true; `onActivityResumed()`: false. `leaveMark_independentFromHolds` (G-01): đang có `Hold` và dấu; tiêu dấu thì vẫn true tới khi nhả `Hold` và có báo cáo |
| KB-23, tiêu chí 2 (G-03 cách 2) | Unit test Robolectric | `AdSuppressionTest.suppressResumeAds_followsActive`: `SuppressResumeAds(active)` với `active` từ `mutableStateOf(true)`: `suppressed` true; đổi false, `waitForIdle`: false |
| KB-23, tiêu chí 3 (G-05) | Unit test | `ResumeSuppressionTest.failedOpen_resumeAdsWorkAgain`: `hold()`, mở thất bại, `release()`, `onShellReported(false)`: false. `leaveMark_neverOutlivesOneResume`: dấu đặt, hai lần `onActivityResumed()` liền: false sau lần đầu, vẫn false sau lần hai; không có nhả âm |
| G-04 (vỏ app, L-051) | Unit test Robolectric | `AdsHostTest.shellReportsSuppressionFromComposition`: `AdsHostContent(suppression, suppressor) { ... }` trong `createAndroidComposeRule<ComponentActivity>()`; `acquire` rồi `release` trên luồng chính, `waitForIdle()`: `resumeAdsSuppressed` false. Đối chứng: cùng nội dung không bọc `AdsHostContent`, sau `release` và `waitForIdle` vẫn true (chỉ vỏ app mới hạ chặn). Chạy thử kỹ thuật đọc trạng thái trong composition và báo bằng `SideEffect` ở mục 4.1 |
| Thuộc tính vị trí (MC-04) | Unit test | `AdPlacementTest.intervalAttributes_defaultAndValidated`: mọi `AdPlacement` có `ignoreInterval == false`, `recordInterval == true`; `AdPlacementDeclaration` `NATIVE_SMALL` với `ignoreInterval = true` ném `IllegalArgumentException`; `INTERSTITIAL` với `recordInterval = false` không ném |
| Bỏ chờ ngừng chạm (câu hỏi 9 của spec) | Unit test phản chiếu | `FullScreenAdGuardTest.quietPeriodRule_isGone`: `FullScreenAdGuard::class.java.declaredMethods` không có `onUserInteraction`; `AdsController::class.java.methods` không có `onUserInteraction`. Đối chứng: có `record`. Thất bại trước code vì hai hàm còn |
| Đồ thị và vỏ app | Build và test có sẵn | `scripts/check.sh` biên dịch `MainActivity`, `AdGalleryActivity` (bỏ `onUserInteraction` mà còn lời gọi thì lỗi biên dịch); `AppLaunchTest` trên emulator; ảnh chuẩn của `:app` không đổi (giao diện không đổi) |
| Trên máy (staging, màn xem thử) | Kiểm tra thủ công | 1) Bấm "Interstitial", đóng, bấm lại ngay: không hiện, log `THROTTLED`; chờ 45 giây sau lúc đóng (không phải lúc hiện): hiện. 2) Bấm "Interstitial" rồi bấm "Native full screen" nhanh: chỉ một cái, log `BUSY`. 3) Đổi giờ máy lùi 1 giờ giữa hai lần bấm: khoảng cách vẫn 45 giây. 4) Đang hiện inter, bấm nút Home rồi mở lại app từ danh sách app gần đây: nếu quảng cáo không còn, log `stuck_recovered` hoặc `closed`, app dùng được. 5) Log có `Consent` (T-020) sau `stuck` khi cả hai xảy ra. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt` (callback của SDK, `tryBegin` trước `show`, ghi mốc ở dòng đầu của callback đóng, đăng ký `ActivityResumeSteps`, `onActivityDestroyed`), `MainActivity.kt` và `AdGalleryActivity.kt` (thay `CompositionLocalProvider` bằng `AdsHost`, bỏ `onUserInteraction`), `FullScreenAds.kt` (thêm tham số có mặc định), `AndroidTimeSources` trong `MonotonicClock.kt` (hai dòng gọi thẳng `SystemClock.elapsedRealtime()`, `System.currentTimeMillis()`), `ads-guide.md`. `AdsController` gắn với SDK và không có unit test riêng; mọi quyết định nằm trong các lớp thuần ở trên. Phần vỏ app không để trần: hợp đồng G-04 được test trên `AdsHostContent` bằng Robolectric (`AdsHostTest`), là đúng composable mà `MainActivity` đặt; `MainActivity` không có hàm `suspend` nào đổi (L-051). Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-24, chung một lần chạy với plan T-020 (mục 4.1 của plan đó), trong worktree của người viết plan trên commit `adf9450`, code thử không commit, đã gỡ hết. Phần của task này trong test tạm `TrialStartupTest.kt` (Robolectric, `createAndroidComposeRule<ComponentActivity>()`): (1) callback vòng đời đăng ký trên `Application` thấy `pause` rồi `resume` của cùng activity (cơ sở của F-03) và `destroy` (N-04); (2) composable đọc một `StateFlow<Int>` bằng `collectAsState` trong composition và báo `count > 0` bằng `SideEffect`, đổi giá trị 0, 1, 0 với `waitForIdle` giữa các lần (cơ sở của G-02, G-04).
- **Lệnh:** `scripts/check.sh` (`JAVA_HOME` của Android Studio, `ANDROID_HOME=/Users/khacnhat/Library/Android/sdk`), ba lần như plan T-020.
- **Kết quả:** (1) chuỗi sự kiện `created, started, resumed, paused, resumed, ...` đúng thứ tự, `destroy` báo `isChangingConfigurations` đúng (true khi `recreate()`, false khi hủy thật). (2) Giá trị báo được đúng `[false, true, false]`: `SideEffect` chạy sau mỗi lần dựng lại và chỉ sau `waitForIdle`. Lần 3 của `scripts/check.sh`: "Test JVM: 457 test, 0 lỗi", "Cổng chất lượng: QUA". Chưa chạy thử: callback của SDK quảng cáo thật (kiểm thủ công). `ShadowSystemClock.advanceBy` và `SystemClock.setCurrentTimeMillis` không còn cần (`MonotonicClockTest` chạy trên JVM, mục 3.4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi tần suất: mốc lúc đóng, theo nhóm, bỏ chờ ngừng chạm; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đổi nhịp quảng cáo toàn màn hình.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app`), thêm API công khai cho feature (`rememberAdSuppression`, `SuppressResumeAds`, `rememberMarkLeavingApp`, `AdsHost`), đổi `AdPlacement`, `rememberShowFullScreenAd` |
| B. Độ rõ của yêu cầu | 1 | Luật có dẫn chứng; AI tự quyết: native toàn màn hình đứng riêng xử như inter, nghĩa "một lượt" của G-05, `AdsHost` thay cho hợp đồng hai bước của bản đặc tả (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi luật có unit test với đồng hồ giả; kẹt thật, callback thật của SDK và vòng đời trên máy chỉ kiểm bằng tay |
| D. Ảnh hưởng chéo | 3 | Đổi nhịp mà mọi quảng cáo toàn màn hình dựa vào; `AdsController.kt` cũng do T-019, T-020, T-023 sửa; T-023 tới T-026 dựa vào API mới |
| E. Độ mới của kỹ thuật | 2 | Hồi phục kẹt theo vòng đời activity và báo trạng thái từ composition xuống bằng `SideEffect` chưa có trong dự án; đã chạy thử (mục 4.1) |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `AdPlacement.kt`, `FullScreenAdGuard.kt`, `FullScreenAdPresence.kt` (T-017 dựa vào `showing`, giữ), `FullScreenAds.kt` (API của feature), `MainActivity.kt`, `AdGalleryActivity.kt`, `ads-guide.md`. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng.
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **Phụ thuộc T-019** (chưa merge): `AdsFlags.intervalSeconds`, `AdIntervalGroup`, `AdPlacementDeclaration`, `AdPlacement(key, format, intervalGroup, nativeEvery)`, `AdFormat.APP_OPEN`, `AdFormat.REWARDED`; `FullScreenAdGuard` còn sau T-019 (câu hỏi 8 của plan T-019). Plan này làm đúng ranh giới đó: T-019 chỉ đổi khóa và gỡ phần T-009; task này đổi mốc sang lúc đóng, theo nhóm, bỏ chờ ngừng chạm. Nếu người duyệt T-019 chọn bỏ chờ ngừng chạm ngay ở T-019 thì bước tương ứng ở đây đã xong; plan vẫn đúng, test `quietPeriodRule_isGone` qua sớm, ghi vào nhật ký.
- **Phụ thuộc T-020** (chưa merge; ticket T-022 chưa ghi, câu hỏi 1): dùng `ForegroundAdSuppressor`, `AdsActivityTracker`, `AdsController.onActivityResumed` và việc hỏi lại đồng ý trong đó. Hai file này do T-020 **tạo**; plan này dùng, không sửa, nên không có trong mục 3.2 (L-054).
- **File chung với các plan đợt 3 (L-054):**

| File | T-019 | T-020 | T-022 (plan này) | T-023 |
| --- | --- | --- | --- | --- |
| `core/ads/.../AdsController.kt` | Sửa | Sửa | Sửa | Sửa |
| `core/ads/.../AdPlacement.kt` | Sửa | | Sửa (hai thuộc tính) | |
| `core/ads/.../FullScreenAdGuard.kt` | giữ | | Sửa (viết lại) | |
| `core/ads/.../ForegroundAdSuppressor.kt` | | Tạo | Dùng | |
| `core/ads/.../AdsActivityTracker.kt` | | Tạo | Dùng | |
| `core/ads/.../FullScreenShowDecision.kt` | | | **Tạo** | Dùng (ánh xạ `ad_opportunity`) |
| `core/ads/.../FullScreenLoadState.kt` | | | **Tạo** | Dùng (`load_state`) |
| `core/ads/src/test/.../AdPlacementTest.kt` | Sửa | | Sửa | |
| `MainActivity.kt` | | | Sửa | |
| `docs/ops/ads-guide.md` | Sửa (mục 5 bước 3, mục 6) | | Sửa (mục 5 bước mới, mục 8) | |

- Task khác có thể xung đột: T-021 đặt `FullScreenLoadState.WaitingRetry` theo F-05 và dùng `loadFullScreenAdIfNeeded`; T-023 ánh xạ `FullScreenShowDecision` sang `opportunity_result` và `FullScreenLoadState` sang `load_state`; T-024 dùng `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`, vị trí `ignoreInterval`; T-025 đọc `resumeAdsSuppressed`; T-026 gọi `FullScreenAdGuard.record` khi đóng cả cụm, dùng `SuppressResumeAds` và đặt lớp phủ trong `AdsHost`.
- Đề xuất thứ tự chạy: T-019, T-020, rồi task này, rồi T-023 (T-023 dùng hai file task này tạo), rồi T-021.

## 7. Điều kiện dừng riêng

- Tên của T-019 khác khi task chạy: không có `AdsFlags.intervalSeconds`, `AdIntervalGroup`, `AdPlacementDeclaration`, hay `FullScreenAdGuard` đã bị T-019 xóa hay đổi tên. Dừng.
- T-020 chưa merge, hoặc tên khác: không có `ForegroundAdSuppressor` (`acquire`, `suppressed`), `AdsActivityTracker` hay `AdsController.onActivityResumed`. Dừng, không tự tạo hai file đó.
- `AdPlacementSourceTest` (T-019) đỏ sau khi sửa `AdPlacement.kt`: dừng, không đổi dạng dòng hay biểu thức.
- Cần sửa `scripts/release.sh`, `FullScreenAdsTest.kt`, `RemovedPacingRulesTest.kt` hay ảnh chuẩn nào để qua.
- Cần đổi chữ ký mà `feature/sample` hay `AdGalleryScreen.kt` gọi (`rememberShowFullScreenAd(placement)` phải còn gọi được không thêm tham số).
- `AppLaunchTest` đỏ vì `AdsHost` trong `MainActivity`.

## 8. Câu hỏi cho người duyệt

1. **Ticket T-022 không phụ thuộc T-020, nhưng plan dùng hai file T-020 tạo** (`ForegroundAdSuppressor.kt` cho G-01, `AdsActivityTracker.kt` cho F-03, N-04) và chèn việc gỡ cờ kẹt trước việc hỏi lại đồng ý của T-020 (G-06).
   - Đề xuất của AI: thêm T-020 vào `depends_on` của ticket T-022, chạy sau T-020 (câu hỏi 1 của plan T-020 hỏi cùng việc).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

2. **Native toàn màn hình đứng riêng (T-013) có đọc và ghi mốc `ads_interval` không?** AdsKit không có định dạng này đứng riêng (chỉ trong combo); I-02 chỉ nói inter.
   - Đề xuất của AI: xử như inter (đọc cổng, ghi mốc khi đóng), giữ hành vi đợt 2 và nghiêng về ít quảng cáo hơn. T-026 có thể đổi khi thay native toàn màn hình bằng combo.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

3. **Nghĩa "một lượt" của G-05** (rời app mà lệnh mở hụt: dấu tự nhả sau đúng một lượt).
   - Đề xuất của AI: dấu chỉ đặt sau khi mở thành công (`rememberMarkLeavingApp()` gọi sau `startActivity` không ném lỗi), và luôn bị tiêu ở lần activity của app hiện lại kế tiếp, dù app có xuống nền hay không. Dấu không bao giờ chặn quá một lượt quay lại.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

4. **Xóa test về chờ ngừng chạm trong `FullScreenAdGuardTest`.** Đây là xóa test có sẵn vì luật bị bỏ (câu hỏi 9 của spec đã trả lời "bỏ"), không phải để code qua.
   - Đề xuất của AI: xóa trong commit test của bước 3, thay bằng `quietPeriodRule_isGone`; xin người duyệt xác nhận như với `everyNumberKey_endsWithUnit` ở T-019.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

5. **Hợp đồng vỏ app G-04.** Bản đặc tả để `MainActivity` tự nhả dấu rời app và giao diện gốc tự đọc và báo trạng thái chặn.
   - Đề xuất của AI: `:core:ads` làm phần nhả dấu và gỡ kẹt qua callback vòng đời (T-020, không cần `MainActivity` gọi), phần đọc và báo trong composable `AdsHost` mà `MainActivity` đặt một lần thay cho `CompositionLocalProvider`. App tạo từ dự án mẫu chỉ cần giữ `AdsHost` ở gốc; thiếu thì chặn không bao giờ hạ (G-04), nên `ads-guide.md` ghi rõ.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-24, sau khi anh trả lời các câu ở mục 8 và sau rà soát độc lập trước phiên của RUN-20261024-02 (các điểm nên sửa đã sửa trong plan trước khi duyệt).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
