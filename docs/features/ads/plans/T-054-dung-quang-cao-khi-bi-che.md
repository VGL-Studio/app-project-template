---
ticket: T-054
title: Quảng cáo trong màn hình dừng khi native toàn màn hình che
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 24M          # cỡ M, số gợi ý 23.9M (scripts/token-usage.py estimate, 2026-10-29); một file code mới, sửa sáu file code, mười file test, hai tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-30
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-054: QUẢNG CÁO TRONG MÀN HÌNH DỪNG KHI NATIVE TOÀN MÀN HÌNH CHE

## 1. Tóm tắt

Khi native toàn màn hình của `:core:ads` đang hiện (sau inter hay độc lập), banner và native của màn bên dưới không gửi yêu cầu, không làm mới, không thử lại, quảng cáo đang có giữ nguyên; màn che đóng thì chạy lại theo nhịp của chúng (KB-27, câu hỏi 36, ADR-0036 quyết định 1). Cách làm: một hàm thuần `inScreenAdsMayRun(shown, covered)` cho điều kiện "quảng cáo trong màn được chạy lúc này", dựng thành một cửa sổ (`StateFlow` đặt đồng bộ) từ foreground và cờ "đang hiện" của cổng toàn màn hình (F-01), cờ này bàn giao liền mạch khi inter nối sang native; mọi đường gửi của banner, native một quảng cáo và pool đọc cửa sổ này thay cho foreground (L-072). Kiểm chứng bằng unit test của hàm thuần, một test cho mỗi đường gửi (lượt đầu, thử lại, nạp tiếp, nạp lại tay, làm mới, nạp bù pool) trên đồng hồ ảo, và test của `AdsController` thật (với loader native giả) cho cửa sổ, cổng gửi, nhịp làm mới, `canLoadNow` của pool và lúc inter nối sang native.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-27 | Khi native toàn màn hình đang hiện thì banner và native ở màn bên dưới không gửi yêu cầu mới, không làm mới, không thử lại (câu hỏi 36) | "Đang hiện" là cờ `FullScreenAdPresence.showing` (`AdsController.fullScreenAdShowing`), cờ chung của cổng toàn màn hình (F-01). Cờ này bật cho cả inter, app open, rewarded; các loại đó là Activity của SDK nên màn bên dưới đã dừng theo vòng đời, đọc chung một cờ không đổi gì thêm cho chúng. Áp cho: banner cố định và banner thu gọn (`AdSlot`, lớp banner của thanh đáy), native nhỏ và lớp native thu gọn (nguồn một quảng cáo, T-040, T-030), pool native (medium, thẻ trong tập hợp, thẻ ở bước của luồng, T-028, T-032). Không áp cho lượt nạp của chính native toàn màn hình (nó phải nạp được trong lúc màn che đang chờ quảng cáo, K-04) |
| KB-27 | Quảng cáo đang có ở màn bên dưới giữ nguyên, không bị hủy | Banner chỉ `pause()`, không `destroy()`; native đang hiện không bị hủy (nhịp làm mới bị bỏ, quảng cáo cũ giữ); pool không đóng |
| KB-27 | Khi native toàn màn hình đóng thì banner chạy lại và native làm mới lại theo nhịp của chúng; lượt làm mới tới hạn trong lúc bị che thì bỏ, không dồn | Native: nhịp làm mới 60 giây rơi lúc bị che thì bỏ như nhịp rơi lúc ở nền (CL-02), nhịp sau tính 60 giây từ nhịp bị bỏ; đóng màn che không làm mới ngay. Banner: "tạm dừng như khi màn hình không hiện" (trả lời câu 36): `resume()` khi màn che đóng, tự làm mới của AdMob chạy lại theo nhịp của đơn vị; lượt thử lại hay nạp lại tay tới hạn lúc bị che thì gửi một lần khi màn che đóng, như lượt tới hạn khi màn không hiện hôm nay (B-02, B-05). Một lượt, không dồn; xem câu hỏi 2 ở mục 8 |
| Trả lời câu hỏi 36 | Trang native toàn trang ở bước của luồng không tính | `fullScreenStep` không bật cờ (đã vậy từ T-032); test khẳng định cửa sổ vẫn mở khi trang toàn trang hiện |
| KB-18, KB-24 (giữ) | Banner không tải lại hay thử lại khi app ở nền; native không gửi yêu cầu khi app ở nền | Cửa sổ đóng khi không ở foreground, như hôm nay; mọi test ở nền có sẵn giữ nguyên |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md): ticket không đổi giao diện.

ADR liên quan: ADR-0036 (quyết định 1: dừng khi bị che), ADR-0038 (JBase làm chuẩn: MAX dừng tự làm mới banner và MREC khi có quảng cáo toàn màn hình, `pauseAutoRefresh: stopped (fullscreen showing)`), ADR-0026 (luật PARTNER còn lại giữ nguyên, ghi điểm lệch vào mục 12 của `ads-business-rules.md`), ADR-0019 (log), ADR-0031 (mục 3.5).

Các mục của `docs/CLAUDE.md`, Khi viết plan, áp dụng cho plan này: mục 3.2 đủ và cụ thể; file người khác đọc (`ads-business-rules.md`, `ads-guide.md`: `scripts/ads-plan.py` chép tay bảng định dạng, cột làm mới vẫn đúng nên không đổi); mục 3.5 có kết quả `scripts/existing-tests.py`; mỗi tiêu chí có test; quyền và thư viện (không thêm); L-072 (một hàm thuần, mỗi đường một test, mục 4); L-061 (cờ tắt giữa chừng: rút đồng ý trong lúc bị che, mục 4); L-068 (hai khe cùng vị trí dưới một màn che: cửa sổ là một cho cả process, không đếm theo khe, mục 4); L-055 (mỗi test "không gửi" có bước đối chứng gửi được khi màn che đóng); L-091 (app ra nền lúc đang bị che, mục 4); `token_estimate`; câu hỏi ở mục 8. Không áp dụng: L-029 (không phải lỗi giao diện đo được), L-042 (E dưới 2), L-083 (không thêm thư viện), L-050 (không có ảnh chuẩn, không có R2), L-051 (không đổi hàm `suspend` mà vỏ app gọi), L-085 (kiểm thủ công cần quảng cáo thật, ghi `máy thật`).

## 3. Giải pháp

### 3.1. Cách làm

**A. Hàm thuần và cửa sổ (file mới `InScreenAdWindow.kt`).**

```kotlin
/**
 * KB-27, ADR-0036 decision 1, question 36 of the ads spec: an ad inside a screen (banner, native small, collapsible
 * layer, pool cells) may send, refresh or retry only while its screen [shown] and no full-screen ad drawn over the app
 * [covered] it (the showing flag of the full-screen gate, F-01). The one condition every send path reads (L-072).
 */
internal fun inScreenAdsMayRun(shown: Boolean, covered: Boolean): Boolean = shown && !covered

/** [inScreenAdsMayRun] of two flows, for the banner of one screen: its lifecycle [visible] and [covered]. */
internal fun inScreenAdWindowOf(visible: Flow<Boolean>, covered: Flow<Boolean>): Flow<Boolean> =
    combine(visible, covered, ::inScreenAdsMayRun).distinctUntilChanged()

/**
 * The window of the in-screen ads of the process: [open] is [inScreenAdsMayRun] of the last [onForeground] and
 * [onCovered], set on the calling stack (no coroutine between a flag and the window). Logs when a cover pauses or
 * resumes the in-screen ads. Main thread only.
 */
internal class InScreenAdWindow(private val logger: AppLogger) {
    private var foreground = false
    private var covered = false
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    fun onForeground(value: Boolean) { foreground = value; update() }

    fun onCovered(value: Boolean) {
        if (value == covered) return
        covered = value
        logger.info(TAG, if (value) "In-screen ads paused (full-screen ad on screen)" else "In-screen ads resumed (full-screen ad closed)")
        update()
    }

    private fun update() { _open.value = inScreenAdsMayRun(foreground, covered) }
}
```

Vì sao không dùng `stateIn`: đã chạy thử bằng một test Robolectric tạm (không commit, đã xóa, 2026-10-29, `./gradlew :core:ads:testDebugUnitTest --tests '*TmpWindowTrialTest*'`, `BUILD SUCCESSFUL`). Cửa sổ `combine(...).stateIn(scope trên Dispatchers.Main.immediate)`: hạ cờ thì cửa sổ thành `true` ngay và một lượt chờ `window.first { it }` chạy ngay trên cùng ngăn xếp (`sends=1`); bật lại cờ ngay sau đó thì cửa sổ **vẫn đọc `true`** cho tới khi luồng chính chạy hết việc chờ (`window=true` rồi mới `false` sau `idle()`). Tức là vừa có khoảng hở gửi được, vừa có lúc đọc giá trị cũ. Bản `MutableStateFlow` đặt đồng bộ: cả hai chiều đổi ngay (`window=true` rồi `false`, không cần `idle()`), và khi cờ được đổi trong một khối rồi chỉ công bố một lần ở cuối thì không có giá trị `true` nào lọt ra (`sends=0`, chuỗi giá trị `[false]`). Plan dùng bản đồng bộ.

**B. Bàn giao cờ khi inter nối sang native (`FullScreenAdPresence.kt`, `AdsController.kt`).** Hôm nay `endTurn` (dòng 1284 tới 1290) gọi `presence.onClosed()` (cờ thành `false`) rồi `turn.complete(reason)`; với lượt inter có combo, `complete` chạy `NativeFullScreenTurns.afterInterstitial` ngay trên cùng ngăn xếp, và `beginPresence()` (dòng 105, 106 của `NativeFullScreenTurns.kt`) bật cờ lại. Trên `Dispatchers.Main.immediate` một lượt đang chờ trong `awaitSendAllowed` có thể chạy ngay trong khoảng `false` đó và gửi yêu cầu dưới màn che, và log "paused, resumed" ghi ở mỗi lần đóng inter. Sửa:

- `FullScreenAdPresence` nhận tham số `onPublished: (Boolean) -> Unit = {}` và thêm `fun <T> publishAfter(block: () -> T): T`: trong `block`, `tryBegin`, `onClosed` đổi trạng thái bên trong (`isBusy`, `current` vẫn đúng ngay, F-01 không đổi), còn `showing` và `onPublished` chỉ đổi **một lần** ở cuối khối, theo giá trị cuối. Ngoài khối, mọi hàm công bố ngay như hôm nay. Gọi lồng nhau thì chỉ khối ngoài cùng công bố. Công bố nằm trong `finally`: `FullScreenCompletion.complete` gọi `onFinished` của nơi gọi không bắt lỗi (`FullScreenCompletion.kt`, dòng 22 tới 27), nên nếu `onFinished` ném lỗi trong khối thì giá trị cuối vẫn được công bố rồi lỗi đi tiếp như hôm nay; không có `finally` thì `showing` và cửa sổ kẹt ở `true`, chặn mọi quảng cáo trong màn và hộp thoại mạng tới lượt hiện sau.
- Thêm `val showingNow: Boolean` (trạng thái bên trong, đổi ngay cả trong khối). `ConsentCoordinator` đọc nó thay cho `showing.value`: `fullScreenShowing = { presence.showingNow }` (dòng 239 của `AdsController.kt`). Lý do: trong khối, `showing.value` còn `true`; nếu `onFinished` gây một lượt hỏi lại đồng ý (nạp sẵn, foreground đổi, mạng về) thì `runRegatherIfPossible` (`ConsentCoordinator.kt`, dòng 235) sẽ thấy "đang hiện" và hoãn tới lần resume sau, khác hôm nay. Đọc trạng thái bên trong giữ đúng hành vi cũ. Các nơi đọc khác (hộp thoại mạng, `preloader.onActivityResumed`, cửa sổ) đọc giá trị đã công bố.
- `endTurn` bọc cả hai lời gọi: `presence.publishAfter { if (currentTurn === turn) { currentTurn = null; presence.onClosed() }; turn.complete(reason) }`. Lượt inter có combo: cờ đọc từ ngoài đi thẳng từ `true` sang `true`, không có `false` nào. Lượt không có combo (hay combo tắt khóa ngay ở cổng): cuối khối công bố `false` như hôm nay. Combo tắt khóa sau khi đọc `comboEnabled()` (trong coroutine): `endPresence()` hạ cờ sau đó, như hôm nay.
- `AdsController` dựng `inScreenAdWindowState = InScreenAdWindow(logger)` trước `presence`, rồi `presence = FullScreenAdPresence(onPublished = inScreenAdWindowState::onCovered)`; `onForegroundChanged` (dòng 524 tới 527) gọi thêm `inScreenAdWindowState.onForeground(foreground)`.

**C. Native và pool (`AdsController.kt`).** Hôm nay mọi đường gửi native đi qua `awaitSendAllowed(foregroundState, ::mayRequestNow)` (T-040, T-028), nhịp làm mới đọc `foreground.value`, và `canLoadNow` của pool đọc `foregroundState.value`. Plan thêm:

- `internal val inScreenAdWindow: StateFlow<Boolean> = inScreenAdWindowState.open`.
- `internal val inScreenSendGate: suspend () -> Boolean = { awaitSendAllowed(inScreenAdWindow, ::mayRequestNow) }`: cổng gửi duy nhất của khe trong màn.
- `internal fun poolCanLoadNow(): Boolean = _sdkReady.value && inScreenAdWindow.value && mayRequestNow()` (tách khỏi lambda để test được).
- Thay ở đúng năm chỗ: `sendGate` của `NativeAdSources` (dòng 1361) và của `NativeAdPool` (dòng 1386) thành `inScreenSendGate`; `canLoadNow` của pool (dòng 1387) thành `::poolCanLoadNow`; tham số `foreground` của `singleAdSlotStates` (dòng 1474) và của `retryingNativeAdStates` (dòng 1496) thành `inScreenAdWindow`.
- Điểm nối cho test (L-072, rà soát độc lập điểm 3): `@VisibleForTesting internal var nativeLoaderForTest: NativeAdLoader? = null` và hàm riêng `newNativeLoader(events, options)` trả nó nếu có, không thì `SdkNativeAdLoader(context, adRequest(), events, options)`; dùng ở ba chỗ dựng loader của khe trong màn (dòng 1417 pool, 1489 nhánh không có nguồn, 1518 nguồn một quảng cáo). Dòng 1561 (`loadNativeAd`, hàng trong danh sách) để T-059. Thêm `@VisibleForTesting internal fun setAppForegroundForTest(foreground: Boolean)` (đặt `foregroundState` và gọi `inScreenAdWindowState.onForeground`) và `@VisibleForTesting internal fun markSdkReadyForTest()` (đặt `_sdkReady`), như `openConsentGateForTest` của T-026. Test không có `AdsActivityTracker` hay SDK.
- **Giữ** `foregroundState` ở `FullScreenReloads` (dòng 303) và `preloadNativeFullScreen` (dòng 1683, 1684): lượt nạp của chính native toàn màn hình không chờ cửa sổ, vì màn che sau inter mở trước khi quảng cáo của nó về và chờ chính lượt nạp đó (K-04, `nativeLoadResults`). Ghi KDoc ở hai chỗ này để phiên sau không "sửa" nhầm.
- KDoc của `fullScreenAdShowing` (dòng 252 tới 257): thêm "quảng cáo trong màn đọc cờ này qua `inScreenAdWindow` (KB-27); inter nối sang native giữ cờ liền mạch (`publishAfter`)".

Các hàm `retryingNativeAdStates`, `refreshingNativeAdStates`, `singleAdSlotStates`, `NativeAdSource`, `NativeAdPool` **không đổi chữ ký**: tham số `foreground` của chúng giờ nhận cửa sổ. Hệ quả có sẵn, đúng tiêu chí, không cần code mới:

- Lượt đầu, thử lại 5 giây khi chưa có quảng cáo, nạp tiếp sau khi lấy, lượt thử lại một lần của chỗ thua (O-02), thử lại sau khi làm mới lỗi, nạp bù và thử lại của pool: đều chờ `foreground.first { it }` trong `awaitSendAllowed`, nên chờ tới khi màn che đóng rồi mới gửi, một lượt.
- Nhịp làm mới: `if (!foreground.value) continue@ticks` nên nhịp rơi lúc bị che bị bỏ, nhịp sau 60 giây sau (KB-27, tiêu chí 3). Đóng màn che không làm mới ngay.
- Pool: `settled()` và `exhausted` đọc `canLoadNow`, nên lúc bị che pool coi như "không gửi được lúc này" như ở nền: kế hoạch nạp sẵn đi tiếp (S-22) thay vì đứng chờ một pool không gửi được, và ô chưa có quảng cáo theo P-06 như ở nền (câu hỏi 3 ở mục 8).
- Chỗ hẹp đã biết: nhịp làm mới qua kiểm cửa sổ ngay trước khi màn che bật, rồi chờ quảng cáo tối đa 10 giây (`claimWithin`): lượt gửi của nguồn chờ cửa sổ nên không có yêu cầu nào đi trong lúc bị che, nhưng hết 10 giây thì nhịp đó tính là một lần lỗi và hẹn thử lại 2 giây (thử lại lại chờ cửa sổ). Không gửi gì sai luật; chỉ thừa một lần đếm lỗi. Test `refreshClaimWhileCovered_sendsNothingUntilUncovered` ghim phần không gửi.
- Log `Native <mã> refresh: skipped (background)` giữ nguyên chữ (test có sẵn khẳng định); lúc bị che, log của cửa sổ ở trên cho biết lý do.
**D. Banner (`AdSlot.kt`).** `BannerSlotHost` thêm tham số bắt buộc `covered: Flow<Boolean>` (không có mặc định, để mọi nơi gọi phải truyền; hai test gọi thẳng `BannerSlotHost` là `BannerSlotHostTest` và `BottomAdBarTest` truyền `covered = MutableStateFlow(false)` trong commit test của bước 3). Bên trong, `val window = remember(visible, covered) { inScreenAdWindowOf(visible, covered) }` và truyền `window` vào `BannerFrameContent` thay cho `visible`. Từ đó cả hai chỗ dùng đều đọc cửa sổ: `bannerShouldRun(window, frame)` (`pause()` khi bị che, `resume()` khi đóng, `AdView` không bị hủy) và `runBannerLoads(visible = window, ...)` (lượt đầu, thử lại, nạp lại tay chờ cửa sổ). `bannerShouldRun` và `runBannerLoads` không đổi. `BannerSlot` truyền `covered = ads.fullScreenAdShowing` (cờ đã công bố, nên không thấy khoảng `false` lúc inter nối sang native, mục B). Banner MREC (T-062) dùng lại đúng `BannerSlotHost` này.

**E. Không đổi:**

- `loadNativeAd` của hàng native nhỏ trong danh sách (`InListAds`): T-059 chạy sau T-054 và thêm chỗ gọi thứ sáu (`inListRowStates`), truyền `inScreenAdWindow` của plan này ở đó. Hôm nay hàng chỉ nạp một lần khi được dựng (câu hỏi 1 ở mục 8).
- Hẹn giờ bung lại của lớp thu gọn (CL-03): không phải lượt gửi; bung lại dưới màn che không gửi gì.
- `fullScreenStep` (trang toàn trang của bước luồng): không bật cờ, đúng trả lời câu 36.

**F. Tài liệu.** `ads-business-rules.md`: thêm B-07 (mục 5.5; T-062 lấy B-08), D-06 (mục 5.6), O-10 (mục 5.7, luật chung của native), câu "Bị che thì bỏ nhịp như ở nền" ở CL-02 (mục 5.8) và "đang bị che" vào danh sách "không nạp được" của P-05 (mục 5.9), cột AdsKit ghi "Không có (ADR-0036, JBase `pauseAutoRefresh`)"; mục 12 thêm điểm lệch có chủ đích vào đoạn "Chỗ ta lệch có chủ đích" kèm ADR-0036. `ads-guide.md`: đoạn banner và native ở mục 3 (dòng 141), mục "Native không nạp ở nền" (dòng 156), thêm câu về màn che và hai dòng log mới.

Mẫu làm theo: cổng gửi chung `awaitSendAllowed` của T-040, cách kiểm "ở nền" của từng đường trong `NativeAdRetryTest`, `NativeAdRefreshTest`, `NativeAdSourceTest`, `NativeAdPoolTest`, `BannerSlotHostTest.hiddenScreen_pausesView_visibleAgain_resumes`; test controller dùng `beginStandaloneNativeTurn`, `beginInterstitialTurn` như `AdsControllerNativeComboTest`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InScreenAdWindow.kt` | `inScreenAdsMayRun`, `inScreenAdWindowOf` cho banner, lớp `InScreenAdWindow` (mục 3.1 A) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresence.kt` | `onPublished`, `publishAfter` (công bố trong `finally`), `showingNow` (mục 3.1 B) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `inScreenAdWindowState`, `presence` với `onPublished`, `endTurn` bọc trong `publishAfter`, `onForegroundChanged`, `fullScreenShowing = { presence.showingNow }` của `ConsentCoordinator` (mục 3.1 B); `inScreenAdWindow`, `inScreenSendGate`, `poolCanLoadNow`, năm chỗ thay, `newNativeLoader` và `nativeLoaderForTest`, `setAppForegroundForTest`, `markSdkReadyForTest` (mục 3.1 C); KDoc của `fullScreenAdShowing`, `FullScreenReloads` và `preloadNativeFullScreen` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerSlotHost(covered)`, cửa sổ truyền vào `BannerFrameContent`, `BannerSlot` truyền `ads.fullScreenAdShowing`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeSendGate.kt` | Chỉ KDoc: tham số `foreground` là cửa sổ của khe trong màn (foreground và không bị che); thêm đường "nạp bù pool" vào danh sách đường gửi |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRefresh.kt` | Chỉ KDoc: nhịp rơi lúc ở nền hay bị che thì bỏ (KB-27) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt` | Chỉ KDoc của `canLoadNow` và P-05: "foreground và không bị che" |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurns.kt` | (dự phòng, mục 3.4) `afterInterstitial` gọi `handOver` thay `beginPresence` khi cờ do lượt inter đó giữ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InScreenAdWindowTest.kt` | Bảng của hàm thuần, lớp cửa sổ, cửa sổ của banner |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresenceTest.kt` | Năm test của `publishAfter` và `showingNow` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInScreenWindowTest.kt` | Controller thật (Robolectric) với `nativeLoaderForTest = FakeNativeAdLoader()`: cửa sổ, cổng gửi, nhịp làm mới, nạp bù pool, `poolCanLoadNow`, inter nối sang native |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | Chỉ lời gọi `BannerSlotHost` ở dòng 125 truyền `covered = MutableStateFlow(false)` (commit test của bước 3); câu khẳng định giữ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerSlotHostTest.kt` | Hàm `setHost` truyền `covered` (commit test của bước 3, cùng lúc với tham số mới); bốn test mới ở mục 4 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | Hai test bị che |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRefreshTest.kt` | Bốn test bị che |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourceTest.kt` | Hai test bị che |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolTest.kt` | Bốn test bị che |
| Sửa | `docs/ops/ads-business-rules.md` | B-07, D-06, O-10, CL-02, P-05, mục 12 (mục 3.1 E) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3.1 E |

Không sửa, kèm lý do:

- `BannerFrame.kt` (`bannerShouldRun`), `BannerLoads.kt` (`runBannerLoads`): nhận cửa sổ qua tham số `visible` có sẵn; luật của chúng không đổi. `BannerLoadsTest`, `BannerFrameTest` giữ nguyên.
- `NativeAdRetry.kt`, `NativeAdSource.kt`, `NativeAdSources.kt`, `NativePoolCell.kt`, `NativeAdPools.kt`: chữ ký không đổi, nhận cửa sổ từ `AdsController`. `NativeAdSourcesTest`, `NativePoolCellTest`, `NativeAdPoolsTest` giữ nguyên.
- `CollapsibleNativeAd.kt`, `BottomAdBar.kt`, `NativeAdSlot.kt`, `NativeAdCell.kt`, `NativeFlowStep.kt`, `InListAds.kt`: gọi `ads.nativeAd`, `ads.pooledCell`, `ads.stepAd`, `BottomBannerSlot` như cũ. T-061, T-065 sửa `BottomAdBar.kt`, `NativeAdSlot.kt`; plan này không chạm.
- `NativeFullScreenOverlay.kt`, và `NativeFullScreenTurns.kt` ở phương án chính: cờ "đang hiện" bật và hạ đúng lúc (T-026, T-044); khoảng hạ rồi bật lại khi inter nối sang native được bịt ở `endTurn` (mục 3.1 B).
- `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt`: đọc `fullScreenAdShowing` cho hộp thoại mạng; cờ không đổi.
- `scripts/ads-plan.py`: bảng định dạng chép tay ghi "Tự làm mới theo đơn vị trên AdMob", "Nạp lại tay mỗi 30 giây"; vẫn đúng.
- `docs/features/ads/spec.md`: KB-27 đã có tiêu chí; ô đánh dấu do người tick khi nghiệm thu.
- Test nhắc tới `docs/ops/ads-business-rules.md` (L-020): chúng chỉ trích mục hay bảng mà plan không sửa (MC-02, mục 1, mục 4, mục 5.7 kích thước, mục 8.2, 8.3, mục 10), chép tay trong chú thích, không đọc file lúc chạy; plan chỉ thêm dòng ở 5.5, 5.6, 5.7 (luật chung), 5.8, 5.9 và mục 12, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Luật luôn bật, không có khóa Remote Config (JBase không có công tắc cho luật này).
- Quảng cáo: Không thêm hay bớt vị trí, không đổi định dạng. Đổi lúc gửi và lúc làm mới của mọi vị trí banner và native trong màn (KB-27), nên chạm điểm chặn 1.
- Sự kiện phân tích: Không chạm. Không có sự kiện mới; lượt không gửi lúc bị che không ghi gì, như lượt không gửi lúc ở nền.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Dùng `kotlinx-coroutines` (`combine`, `distinctUntilChanged`, `MutableStateFlow`) đã có trên classpath biên dịch của `:core:ads` (các file `AdsController.kt`, `BannerFrame.kt` đã import).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `publishAfter` bọc `turn.complete(reason)`, mà hàm này chạy `onFinished` của nơi gọi (điều hướng, lưu) trên cùng ngăn xếp; trong khối, `fullScreenAdShowing` còn đọc giá trị cũ (`true`). Nơi đọc cờ trong lúc đó hôm nay: hộp thoại mạng của `MainActivity` (chờ cờ hạ, chỉ trễ tới cuối khối), `preloader.onActivityResumed`; `ConsentCoordinator` chuyển sang đọc `showingNow` (mục 3.1 B) nên không đổi hành vi. Nếu một test có sẵn của các nơi này đỏ vì đọc giá trị cũ thì cách bàn giao chung không dùng được.
- **Điều kiện chuyển:** một test có sẵn (ngoài các test mới của plan) đỏ vì `fullScreenAdShowing` còn `true` trong `onFinished`, sau hai lần sửa.
- **Cách làm thay:** chỉ bàn giao cho lượt inter có combo: `endTurn` không gọi `presence.onClosed()` khi lượt đó sẽ mở combo (khóa `is_show_native_after_inter` không đọc được đồng bộ, nên dùng cách `NativeFullScreenTurns` báo lại: `FullScreenAdPresence.handOver(placement)` đổi `current` sang `NATIVE_AFTER_INTER` mà không hạ `showing`, gọi từ `afterInterstitial` thay cho `beginPresence()` khi cờ đang do lượt inter đó giữ). File đã có trong mục 3.2 (`FullScreenAdPresence.kt`, `AdsController.kt`, dự phòng) và thêm `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurns.kt` (dự phòng). Hàm thuần, test viết trước (nhất là `comboTurn_noOpenWindowAcrossInterClose`) và tiêu chí giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Chạy `python3 -I scripts/existing-tests.py docs/features/ads/plans/T-054-dung-quang-cao-khi-bi-che.md` (2026-10-29, lần 3, sau rà soát lại): 306 test dùng tên hay tag của các file plan sửa; "Cần xem trước" 0 test; "Nên xem" 57 test plan chưa nhắc tới, plan đã nhắc 54 test; 195 test chỉ dùng tên plan không nêu. Đã đọc các nhóm của "Nên xem" (theo file test). Hành vi plan đổi và test có sẵn:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Native nhỏ, lớp thu gọn, pool gửi và làm mới khi app ở foreground, kể cả khi native toàn màn hình đang hiện | Chỉ khi foreground **và** không bị che (`inScreenAdWindow`) | Không có test khẳng định "gửi khi đang che". Các test ở nền tự dựng `foreground` riêng: `NativeAdRetryTest.background_sendsNothingUntilForeground`, `firstLoad_inBackground_waitsForForeground`; `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s`, `backToForeground_noImmediateRefresh`, `refreshRetry_inBackground_waitsForForeground`; `NativeAdSourceTest.nextLoadAfterClaim_inBackground_waitsForForeground`, `loserRetry_inBackground_waitsForForeground`; `NativeAdPoolTest.claimLoad_inBackground_waitsForForeground`, `refillAfterClaim_inBackground_waitsForForeground`, `refillAfterImpression_inBackground_waitsForForeground`, `retry_inBackground_waitsForForeground` | Giữ: chữ ký không đổi, các test này truyền `foreground` trực tiếp, câu khẳng định vẫn đúng |
| `canLoadNow` của pool thật đọc foreground | Đọc cửa sổ: lúc bị che pool `settled()` và `exhausted` như ở nền | `NativeAdPoolTest.exhausted_table`, `exhausted_whileSendWaitsForForeground_isTrue`, `PreloadPlanRunnerTest.poolSlot_inSecondGroup_startsAfterFirstSettles`, `NativeStepAdStatesTest.exhausted_emitsUnavailable_retriesEvery2s_lateAdShows` dựng `canLoadNow` của riêng test | Giữ. Câu mới: `NativeAdPoolTest.covered_settled_andExhaustedLikeBackground` (mục 4) |
| Banner dừng (`pause`) và chờ gửi chỉ khi màn không `RESUMED` | Thêm: dừng và chờ khi bị che; `BannerSlotHost` có tham số bắt buộc `covered` | `BannerSlotHostTest.hiddenScreen_pausesView_visibleAgain_resumes` và mọi test của `BannerSlotHostTest` gọi `setHost` | Giữ mọi câu khẳng định; chỉ `setHost` truyền thêm `covered = MutableStateFlow(false)` (commit test của bước 3). `BannerLoadsTest.retryDueWhileHidden_waitsForNextVisible`, `manualReloadDueWhileHidden_waitsForNextVisible`, `firstSend_waitsUntilVisible`: giữ, `runBannerLoads` không đổi |
| Cổng gửi của nguồn và pool trong controller là `awaitSendAllowed(foregroundState, ...)` | `inScreenSendGate` = `awaitSendAllowed(inScreenAdWindow, ...)` | `NativeSendGateTest.foreground_openGate_returnsTrue`, `waitsForForeground_thenReadsGate`, `gateClosed_returnsFalse`; `NativeAdSourcesTest.everySend_callsSendGate`; `NativeAdPoolsTest.everySend_goesThroughSendGate` | Giữ: `awaitSendAllowed` không đổi; hai test "mọi lượt gửi qua cổng" dựng cổng của riêng test |
| Lượt nạp và màn của native toàn màn hình (sau inter, độc lập, trang toàn trang của bước luồng) | Không đổi: vẫn đọc `foregroundState`, không chờ cửa sổ | `AdsControllerNativeComboTest` (mọi test, ví dụ `closed_opensCombo`, `standalone_withAd_throughShow_opensTurn`, `standalone_noCurrentTurn_presenceUntilClose`), `AdsControllerFunnelTest.combo_recordsNativeEligibleAfterInter`, `standalone_recordsEligible_onlyWithAd`, `NativeFullScreenStepRecreateTest.recreate_consentClosed_notTaken` | Giữ; đỏ thì dừng (mục 7) |
| Rewarded, resume, đồng ý: đọc `presence`, `mayRequestNow`, `openConsentGateForTest` | Không đổi | `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce`, `backgroundWhileDecisionWaits_switchInTime_closesOnce`, các test `realController_*` của `RewardedActionTest`, `RewardedTurnsTest.decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd`, `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing`, `ConsentGateTest`, `ConsentCoordinatorTest.privacyFormWithdrawn_closesEveryGate` | Giữ: plan không chạm đường của rewarded, resume hay đồng ý |
| Cờ `fullScreenAdShowing` hạ ngay trong `endTurn` rồi `turn.complete` mới chạy; với combo, cờ có một khoảng `false` trên cùng ngăn xếp | `endTurn` trong `publishAfter`: cờ công bố một lần ở cuối, combo không có khoảng `false`; lượt không combo vẫn công bố `false`, chỉ sau `onFinished` thay vì trước | `AdsControllerNativeComboTest.closed_opensCombo`, `showFailed_opensCombo`, `adLost_opensCombo`, `stuckRecovered_opensCombo`, `hostDestroyed_opensCombo` (khẳng định "No gap": cờ đúng ngay sau khi đóng inter), `capExpired_reachesCombo_busyFinishesAtOnce`, `onFinished_runsOnMainThread`, `startupTurn_neverOpensCombo`, `blockedAtGate_noAd_finishesOnce_noView`; `AdsControllerFunnelTest.comboOff_recordsInterEligibleOnly`, `interTurn_recordsEligibleOnce` | Giữ: các test này đọc cờ sau khi lời gọi trả về (giá trị cuối không đổi), hay đọc trong combo (vẫn đúng). Không test nào đọc cờ bên trong `onFinished` của lượt không combo; nếu có test đỏ vì thế thì theo mục 3.4 |
| `FullScreenAdPresence()` không tham số, công bố ngay | Thêm `onPublished` (mặc định không làm gì) và `publishAfter`; ngoài khối công bố ngay như cũ | `FullScreenAdPresenceTest.startsHidden`, `onShowing_setsTrue`, `onClosed_setsFalse`, `closedTwice_staysFalse`, `showingFlow_emitsShowAndClose`, `secondShowWhileShowing_isDropped`, `showingFlag_expiresAfterTenMinutes`, `current_followsTheAdOnScreen`; `NativeFullScreenTurnsTest.noGap_presenceAndHoldTakenBeforeAnySuspension`, `comboWhileStandaloneOpen_presenceFree_skipsBusy`, `comboOff_finishesAtOnce_noView_noLoad`, `holdsResumeAds_fromStartUntilClose`, `secondTurn_whilePresenceBusy_finishesAtOnce`, `standalone_opensAtOnce_withCountdown_marksOwnPlacement` (dựng `FullScreenAdPresence()` riêng) | Giữ |
| `BannerSlotHost` không có `covered` | Tham số bắt buộc `covered` | `BottomAdBarTest` (lời gọi ở dòng 125, mọi test của lớp dựng thanh đáy có lớp banner) | Giữ mọi câu khẳng định; lời gọi truyền `covered = MutableStateFlow(false)` (commit test của bước 3) |
| Màn native sau inter vẽ như cũ | Không đổi (chỉ phương án dự phòng chạm `NativeFullScreenTurns.kt`, không chạm phần vẽ) | `NativeFullScreenOverlayTest.smallScreen_countdownAndCtaInside` (đo vị trí đếm ngược và CTA) | Giữ |
| `ConsentCoordinator` hỏi "đang hiện" qua `presence.showing.value` | Qua `presence.showingNow` (cùng giá trị ngoài khối `publishAfter`; trong khối là trạng thái bên trong) | `ConsentCoordinatorTest` dựng `fullScreenShowing` riêng; không test controller nào gọi lượt hỏi lại đồng ý trong `onFinished` | Giữ. Câu mới: `FullScreenAdPresenceTest.showingNow_readsInnerStateInsideBlock` |
| Log `Native <mã> refresh: skipped (background)` | Giữ chữ; lúc bị che có thêm log của cửa sổ | `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s` (dòng 183) | Giữ |
| Các test quét mã nguồn | Không đổi | `NativeEligibleCoverageTest.everyFileDrawingANativeAd_reportsEligible` (file mới `InScreenAdWindow.kt` không vẽ native), `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags` (plan không đọc Remote Config) | Giữ |

## 4. Kiểm thử

Các đường gửi của khe trong màn (L-072) và test của mỗi đường. Test mức hàm (đường 1 tới 12) dùng `InScreenAdWindow(logger)`, đúng lớp controller dùng, với `onForeground(true)`, rồi `onCovered(true)` hay `onCovered(false)`, và truyền `window.open`; nguồn và pool giả nhận `sendGate = { awaitSendAllowed(window.open) { mayRequest } }`. Vì các hàm đó đã tôn trọng một `foreground` bất kỳ, test mức hàm chỉ chứng minh đường gửi chờ đúng cửa sổ; phần controller có nối đúng cửa sổ vào từng đường được test riêng ở các đường (14) tới (17), với `AdsController` thật và `nativeLoaderForTest = FakeNativeAdLoader()` (rà soát độc lập, điểm 3). Mỗi test "không gửi lúc bị che" có bước đối chứng (L-055): hạ `covered` thì gửi đúng một lượt.

| Đường | Nơi gửi | Test |
| --- | --- | --- |
| (1) Banner, lượt đầu | `runBannerLoads.sendWhenAllowed` | `BannerSlotHostTest.coveredAtStart_sendsNothing_untilUncovered` |
| (2) Banner, thử lại sau lỗi | như (1) | `BannerSlotHostTest.retryDueWhileCovered_sendsOnceWhenUncovered` |
| (3) Banner thu gọn, nạp lại tay 30 giây | như (1) | `BannerSlotHostTest.manualReloadDueWhileCovered_sendsOnceWhenUncovered` |
| (4) Banner, tự làm mới của AdMob | `bannerShouldRun`, `pause()` | `BannerSlotHostTest.covered_pausesView_keepsAd_uncovered_resumes` |
| (5) Native một quảng cáo, lượt đầu | `retryingNativeAdStates` | `NativeAdRetryTest.firstLoad_whileCovered_waitsUntilUncovered` |
| (6) Native, thử lại 5 giây khi chưa có quảng cáo | như (5) | `NativeAdRetryTest.retryDueWhileCovered_sendsOnceWhenUncovered` |
| (7) Nguồn, nạp tiếp sau khi lấy | `NativeAdSource.sendAndLoad` | `NativeAdSourceTest.nextLoadAfterClaim_whileCovered_waitsUntilUncovered` |
| (8) Nguồn, lượt thử lại một lần của chỗ thua (O-02) | như (7) | `NativeAdSourceTest.loserRetry_whileCovered_waitsUntilUncovered` |
| (9) Làm mới 60 giây (native nhỏ và lớp thu gọn) | `refreshingNativeAdStates` | `NativeAdRefreshTest.tickWhileCovered_isSkipped_keepsAd_nextTickAt120s`, `uncovered_noImmediateRefresh`, `refreshClaimWhileCovered_sendsNothingUntilUncovered` |
| (10) Thử lại sau khi làm mới lỗi | như (9) | `NativeAdRefreshTest.refreshRetryWhileCovered_waitsUntilUncovered` |
| (11) Pool, nạp bù (sau khi lấy, sau lượt hiển thị, sau hết hạn: cùng `sendNext`) | `NativeAdPool.sendNext` | `NativeAdPoolTest.refillAfterClaim_whileCovered_waitsUntilUncovered` |
| (12) Pool, thử lại sau lỗi | như (11) | `NativeAdPoolTest.retry_whileCovered_waitsUntilUncovered` |
| (13) Cổng mà controller giao cho nguồn và pool | `AdsController.inScreenSendGate` | `AdsControllerInScreenWindowTest.inScreenSendGate_waitsWhileCovered_thenSends` |
| (14) Controller, native nhỏ: lượt đầu và nhịp làm mới (dòng 1474) | `nativeAd(SAMPLE_NATIVE_SMALL)` qua nguồn | `AdsControllerInScreenWindowTest.realController_smallSlot_refreshTickWhileCovered_isSkipped` |
| (15) Controller, nhánh không có nguồn (dòng 1496) | `nativeAd` của định dạng không dùng nguồn hay pool | `AdsControllerInScreenWindowTest.realController_retryingSlot_firstLoadWaitsWhileCovered` |
| (16) Controller, pool: nạp bù và `canLoadNow` (dòng 1386, 1387) | `pooledCell(GALLERY_NATIVE_MEDIUM)`, `poolCanLoadNow` | `AdsControllerInScreenWindowTest.realController_poolRefillWhileCovered_waits`, `poolCanLoadNow_followsWindow` |
| (17) Controller, inter nối sang native | `endTurn` trong `publishAfter` | `AdsControllerInScreenWindowTest.comboTurn_noOpenWindowAcrossInterClose` |

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hàm thuần | Unit test | `InScreenAdWindowTest.mayRun_table`: (hiện, không che) đúng; (hiện, che), (không hiện, không che), (không hiện, che) sai |
| Lớp cửa sổ theo foreground và cờ | Unit test | `InScreenAdWindowTest.window_followsForegroundAndCover_synchronously`: mới dựng: sai; `onForeground(true)`: đúng ngay (đọc `open.value` không chạy dispatcher); `onCovered(true)`: sai ngay; `onCovered(false)`: đúng; ra nền khi đang che rồi hết che: vẫn sai tới khi `onForeground(true)` (L-091); log `In-screen ads paused (full-screen ad on screen)` một lần khi che, `In-screen ads resumed (full-screen ad closed)` một lần khi hết che, không log khi chỉ foreground đổi hay `onCovered` lặp cùng giá trị |
| Bàn giao cờ trong một khối | Unit test | `FullScreenAdPresenceTest.publishAfter_closeThenBegin_publishesNoFalse`: cờ đang bật, trong `publishAfter { onClosed(); tryBegin(...) }`: chuỗi giá trị của `showing` thu được là `[true]`, `onPublished` không được gọi; `publishAfter_closeOnly_publishesFalseOnceAtEnd`: trong khối `showing.value` còn `true`, `isBusy` đã sai; cuối khối `false` một lần; `publishAfter_nested_outerPublishes` (đối chứng L-055: ngoài khối, `onClosed()` công bố `false` ngay); `publishAfter_blockThrows_stillPublishesFinalValue`: cờ đang bật, khối gọi `onClosed()` rồi ném `IllegalStateException`: lỗi đi ra ngoài (`assertThrows`), `showing.value` là `false` và `onPublished(false)` được gọi một lần; `showingNow_readsInnerStateInsideBlock`: trong khối sau `onClosed()`, `showingNow` sai còn `showing.value` đúng; sau khối cả hai sai |
| Cửa sổ của banner | Unit test | `InScreenAdWindowTest.flowWindow_combinesVisibleAndCover_distinct`: chuỗi giá trị thu được là `[true, false, true]` khi che rồi hết che, không lặp giá trị khi `visible` phát lại cùng giá trị |
| KB-27 tiêu chí 1, đường (1) | Compose UI test (Robolectric) | `BannerSlotHostTest.coveredAtStart_sendsNothing_untilUncovered`: `covered = true` lúc dựng, đi 60 giây: `FakeBannerAdView.sends == 0`, view bị `pause` (không `resume`); hạ `covered`: `sends == 1` |
| KB-27 tiêu chí 1, đường (2) | Compose UI test | `BannerSlotHostTest.retryDueWhileCovered_sendsOnceWhenUncovered`: lượt đầu lỗi (thử lại sau 2 giây), che ở 1 giây, đi tới 30 giây: vẫn 1 lượt gửi; hết che: đúng 2 lượt gửi, không thêm lượt nào trong 1 giây kế |
| KB-27 tiêu chí 1, đường (3) | Compose UI test | `BannerSlotHostTest.manualReloadDueWhileCovered_sendsOnceWhenUncovered`: `BannerRefresh.MANUAL`, nạp được, che ở 10 giây, đi tới 100 giây (ba lần 30 giây tới hạn): 1 lượt gửi; hết che: đúng 2 lượt gửi (một lượt, không dồn ba) |
| KB-27 tiêu chí 1 và 2, đường (4) | Compose UI test | `BannerSlotHostTest.covered_pausesView_keepsAd_uncovered_resumes`: có quảng cáo; che: `pauses == 1`, `destroys == 0`, khung giữ chiều cao, quảng cáo còn hiện; hết che: `resumes == 1` |
| KB-27 tiêu chí 1, đường (5) | Unit test | `NativeAdRetryTest.firstLoad_whileCovered_waitsUntilUncovered`: che trước khi thu, đi 60 giây: không lượt nạp, trạng thái `Loading`; hết che: 1 lượt nạp |
| KB-27 tiêu chí 1, đường (6) | Unit test | `NativeAdRetryTest.retryDueWhileCovered_sendsOnceWhenUncovered`: lượt đầu lỗi ở 0 giây, che ở 1 giây, đi tới 60 giây: vẫn 1 lượt; hết che: 2 lượt |
| KB-27 tiêu chí 1, đường (7) | Unit test | `NativeAdSourceTest.nextLoadAfterClaim_whileCovered_waitsUntilUncovered`: lấy được quảng cáo khi đang che: không có lượt nạp tiếp; hết che: 1 lượt |
| KB-27 tiêu chí 1, đường (8) | Unit test | `NativeAdSourceTest.loserRetry_whileCovered_waitsUntilUncovered`: hai chỗ cùng chờ một lượt nạp, che trước khi quảng cáo về: chỗ thua không gửi; hết che: đúng một lượt thử lại |
| KB-27 tiêu chí 1, 2, 3, đường (9) | Unit test | `NativeAdRefreshTest.tickWhileCovered_isSkipped_keepsAd_nextTickAt120s`: che từ 50 tới 70 giây: nhịp 60 giây không lấy quảng cáo, quảng cáo đầu không bị hủy (`destroyCount == 0`); nhịp 120 giây lấy quảng cáo mới |
| KB-27 tiêu chí 3, đường (9) | Unit test | `NativeAdRefreshTest.uncovered_noImmediateRefresh`: che từ 50 tới 70 giây, đi tới 119 giây: không lượt lấy nào sau lúc hết che |
| KB-27 tiêu chí 1, đường (9) | Unit test | `NativeAdRefreshTest.refreshClaimWhileCovered_sendsNothingUntilUncovered`: nhịp 60 giây bắt đầu lấy rồi che ở 60,1 giây: nguồn không gửi yêu cầu nào tới lúc hết che; quảng cáo đầu không bị hủy |
| KB-27 tiêu chí 1, đường (10) | Unit test | `NativeAdRefreshTest.refreshRetryWhileCovered_waitsUntilUncovered`: làm mới lỗi ở 70 giây (thử lại sau 2 giây), che ở 71 giây, đi tới 100 giây: không gửi; hết che: 1 lượt |
| KB-27 tiêu chí 1, đường (11) | Unit test | `NativeAdPoolTest.refillAfterClaim_whileCovered_waitsUntilUncovered`: pool đầy, che, lấy một quảng cáo: không gửi; hết che: 1 lượt nạp bù |
| KB-27 tiêu chí 1, đường (12) | Unit test | `NativeAdPoolTest.retry_whileCovered_waitsUntilUncovered`: nạp lỗi (thử lại 5 giây), che ở 1 giây, đi tới 60 giây: 1 lượt; hết che: 2 lượt |
| Pool lúc bị che không giữ kế hoạch nạp sẵn | Unit test | `NativeAdPoolTest.covered_settled_andExhaustedLikeBackground`: pool rỗng, che: `settled()` đúng, `exhausted` đúng; hết che: cả hai sai (đối chứng) |
| KB-27 tiêu chí 2, pool | Unit test | `NativeAdPoolTest.covered_keepsAdsInUseAndFree`: pool có hai quảng cáo rảnh và một đang dùng, che 10 phút: không quảng cáo nào bị hủy, `claim` vẫn trả quảng cáo rảnh |
| Cờ tắt giữa chừng (L-061) | Unit test | `NativeAdRefreshTest.consentWithdrawnWhileCovered_destroysAd_noSendAfterUncover`: che, rồi nguồn bị đóng (rút đồng ý): quảng cáo hủy, `Unavailable`; hết che: không lượt gửi nào |
| Hai khe dưới một màn che (L-068) | Unit test | `NativeAdSourceTest.twoSlotsCovered_neitherSends_bothResumeOnce`: hai khe cùng vị trí, che: không khe nào gửi; hết che: mỗi lượt chờ đi đúng một lần, tổng số lượt bằng số lượt đang chờ |
| Cửa sổ của controller theo cờ chung | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.standaloneNativeTurn_closesWindow_closeOpensIt`: `setAppForegroundForTest(true)`: cửa sổ đúng; `beginStandaloneNativeTurn`: sai; `closeNativeFullScreen`: đúng; log `In-screen ads paused (full-screen ad on screen)` rồi `In-screen ads resumed (full-screen ad closed)` |
| Màn native sau inter, không khoảng hở | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.comboTurn_noOpenWindowAcrossInterClose`: khóa combo bật, `openConsentGateForTest`, `setAppForegroundForTest(true)`; thu mọi giá trị của `inScreenAdWindow` và `fullScreenAdShowing` vào danh sách bằng coroutine trên `Dispatchers.Main.immediate`, và chờ `inScreenSendGate()` trong một `async`; `beginInterstitialTurn(combo = true)`, `onInterstitialClosed`, đi 2,1 giây (màn native mở): danh sách của cửa sổ là `[true, false]`, của cờ là `[false, true]` (không có `true` của cửa sổ hay `false` của cờ sau lúc bật), `async` chưa xong, log "paused" đúng một lần và chưa có "resumed"; `closeNativeFullScreen`: cửa sổ thêm `true`, `async` trả đúng, log "resumed" một lần. Đối chứng: lượt inter không combo (khóa tắt ở cổng) thì cửa sổ mở lại khi inter đóng |
| Cổng nối đúng vào nhịp làm mới (đường 14) | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.realController_smallSlot_refreshTickWhileCovered_isSkipped`: `FakeRemoteSettings` bật `is_show_sample_native_small`, mở cổng, `setAppForegroundForTest(true)`, `nativeLoaderForTest` giả; thu `nativeAd(SAMPLE_NATIVE_SMALL)`, trả quảng cáo cho lượt đầu (và lượt nạp tiếp); che ở 50 giây (`beginStandaloneNativeTurn`), đi tới 70 giây: số yêu cầu của loader giả không tăng sau lúc che, quảng cáo đầu không bị hủy; đóng ở 70 giây, đi tới 119 giây: không tăng; 120 giây: một lượt lấy mới. Nếu dòng 1474 hay 1361 còn `foregroundState`, nhịp 60 giây chạy dưới màn che và test đỏ |
| Cổng nối đúng vào nhánh không có nguồn (đường 15) | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.realController_retryingSlot_firstLoadWaitsWhileCovered`: hôm nay không composable nào gọi nhánh này (định dạng của mọi lời gọi `nativeAd` đi qua nguồn hay pool); test gọi thẳng `nativeAd(AdPlacement.SAMPLE_NATIVE_SMALL, AdFormat.NATIVE_MEDIUM)`, định dạng khác định dạng của vị trí nên đi nhánh `retryingNativeAdStates`. Che trước khi thu: không yêu cầu nào tới khi hết che; hết che: một yêu cầu |
| Cổng nối đúng vào pool (đường 16) | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.realController_poolRefillWhileCovered_waits`: bật `is_show_gallery_native_medium`, che, thu `pooledCell(GALLERY_NATIVE_MEDIUM)`: loader giả không nhận yêu cầu trong 60 giây; hết che: có yêu cầu. `poolCanLoadNow_followsWindow`: `markSdkReadyForTest`, mở cổng, foreground đúng: `poolCanLoadNow()` đúng; mở lượt native độc lập: sai; đóng: đúng |
| Trang toàn trang của bước luồng không tính | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.flowStepFullPage_keepsWindowOpen`: `openConsentGateForTest`, `putPreloadedNativeFullScreenForTest`, thu `fullScreenStep(placement)` tới `Loaded`: cửa sổ vẫn đúng, `fullScreenAdShowing` sai |
| App ở nền (giữ KB-18, KB-24) | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.background_closesWindow_withoutCover`: `setAppForegroundForTest(false)`: cửa sổ sai, không có log "paused (full-screen ad on screen)" |
| Đường (13), cổng controller giao đi | Unit test (Robolectric) | `AdsControllerInScreenWindowTest.inScreenSendGate_waitsWhileCovered_thenSends`: `openConsentGateForTest`, foreground đúng, mở lượt native độc lập, gọi `inScreenSendGate()` trong `async`: chưa xong sau 60 giây; đóng lượt: trả đúng. Đối chứng: không che thì trả đúng ngay |
| Dừng và chạy lại trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, màn mẫu có thanh đáy (native nhỏ và lớp thu gọn), bật `is_show_native_after_inter` và inter của màn xem thử; `adb logcat -v threadtime \| grep ' VGL\.'`. Ở màn mẫu 50 giây rồi mở inter, đóng inter, để màn native sau inter mở 30 giây: thấy `In-screen ads paused (full-screen ad on screen)`, không có dòng `Native ... refresh` hay `Banner ...` nào tới lúc đóng; đóng: `In-screen ads resumed (full-screen ad closed)`, quảng cáo cũ vẫn ở thanh đáy, không làm mới ngay. Lặp với banner (tắt `is_show_native_bottom`) |
| Tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | `ads-business-rules.md` B-07, D-06, O-10, CL-02, P-05, mục 12 và `ads-guide.md` khớp mục 3.1 |

**Ngoại lệ viết test trước:**

- `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`: tài liệu.
- KDoc của `NativeSendGate.kt`, `NativeAdRefresh.kt`, `NativeAdPool.kt`: chỉ chú thích.
- Dòng `covered = ads.fullScreenAdShowing` trong `BannerSlot`: `BannerSlot` cần `AdsController` thật và `AdView` của SDK; cùng lý do T-029 để `BannerSlot` ngoài test. `BannerSlotHost` có test mỗi đường banner, và cờ nó đọc (`fullScreenAdShowing`) có test không khoảng hở (đường 17). Các dòng nối của native và pool có test ở đường (14) tới (16).

Commit test của bước 3 gồm cả phần sửa `setHost` của `BannerSlotHostTest` và lời gọi `BannerSlotHost` của `BottomAdBarTest` (truyền `covered`), để test viết trước biên dịch được khi phiên thêm tham số ở bước 4 cùng lúc; nếu tham số chưa có thì test thất bại vì biên dịch, đúng lý do (L-025).

### 4.1. Chạy thử

Không bắt buộc (E dưới 2). Đã chạy thử một điểm mà rà soát độc lập hỏi: cửa sổ dựng bằng `stateIn` hay đặt đồng bộ.

- **Đã chạy thử:** test Robolectric tạm `TmpWindowTrialTest` trong `core/ads/src/test` (không commit, đã xóa), 2026-10-29, trên nhánh `claude/plan-T-054-dung-khi-bi-che` (gốc `c29ce2b`, chưa có code của task). Hai trường hợp: `combine(...).stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), Eagerly, false)` với một lượt chờ `window.first { it }`; và `MutableStateFlow` đặt đồng bộ, có công bố một lần ở cuối khối.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TmpWindowTrialTest*' -i` sau `source` tệp `env.sh` của `scripts/local-setup.sh`. Không chạy `scripts/check.sh`: không bắt buộc vì E dưới 2, và test tạm chỉ đo thứ tự giá trị.
- **Kết quả:** `BUILD SUCCESSFUL`. Bản `stateIn`: hạ cờ rồi bật lại ngay trên cùng ngăn xếp thì lượt chờ đã chạy (`sends=1`) và cửa sổ còn đọc `true` sau khi bật lại, tới khi `idle()` mới thành `false` (chuỗi `[false, true, false]`). Bản đồng bộ: `open.value` đổi ngay cả hai chiều không cần `idle()`; đổi cờ nhiều lần trong một khối rồi công bố một lần thì lượt chờ không chạy (`sends=0`, chuỗi `[false]`). Kết luận: plan dùng bản đồng bộ (mục 3.1 A) và bàn giao cờ trong một khối (mục 3.1 B); phương án `stateIn` cũ bỏ.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi lúc gửi và lúc làm mới của mọi banner và native trong màn), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`), một file mới, sửa sáu file code (ba chỉ đổi KDoc); đổi chữ ký nội bộ `BannerSlotHost` và cách công bố cờ của `FullScreenAdPresence`; API công khai cho feature không đổi |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, câu hỏi 36 đã trả lời, ADR-0036 đã chấp nhận; còn vài chi tiết AI tự đề xuất (mục 8: hàng trong danh sách, nạp lại tay của banner thu gọn, ô pool chưa có quảng cáo, trần 10 phút) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường gửi có unit test trên đồng hồ ảo, cửa sổ của controller có test Robolectric; dừng thật của SDK dưới màn che chỉ thấy trên máy (log) |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi của mọi banner và native trong màn mà `:feature:sample`, màn xem thử và mọi app dựa vào; `AdsController.kt` cũng được T-055, T-059, T-063 sửa; `AdSlot.kt` được T-062, T-067 sửa |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu đã có (cổng gửi T-040, cờ `visible` của banner T-029, các hàm `...ForTest` của T-026, T-044); cách bàn giao cờ trong một khối là mới nhưng nhỏ, đã chạy thử (mục 4.1) |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn; cần duyệt chéo; người phụ trách sản phẩm biết trước vì đổi lúc gửi quảng cáo) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (năm dòng nối, `endTurn`, cửa sổ, cổng, điểm nối loader và ba hàm cho test), `FullScreenAdPresence.kt` (cờ mà hộp thoại mạng của `MainActivity` và `ConsentCoordinator` cũng đọc: chỉ trễ tới cuối khối, không đổi giá trị cuối), `AdSlot.kt` (`BannerSlotHost`), `ads-business-rules.md`, `ads-guide.md`.
- Task khác trong sprint có thể xung đột:
  - T-062 (banner MREC, `depends_on: [T-054]`): dùng `BannerSlotHost(covered)` của plan này; chạy sau.
  - T-059 (hàng native nhỏ trong danh sách thử lại): chạy sau T-054 và thêm chỗ gọi thứ sáu `inListRowStates`; plan của T-059 truyền `inScreenAdWindow` (hay `inScreenSendGate`) của plan này ở đó, và có thể dùng `nativeLoaderForTest` cho test mức controller. Cùng sửa `AdsController.kt` (khác vùng).
  - T-055 (kiểm màn hình trước khi hiện toàn màn hình), T-063 (cổng không hiện quảng cáo), T-066 (`CACHED_EARLY`): cùng sửa `AdsController.kt`, khác vùng; T-063 có thể đổi `mayRequestNow`, mà `inScreenSendGate` gọi.
  - T-067 (banner cỡ thích ứng thường): sửa `BannerSlot` trong `AdSlot.kt` gần dòng `adSize`; plan này thêm `covered = ads.fullScreenAdShowing` vào lời gọi `BannerSlotHost` ngay bên dưới, nên có thể có xung đột chữ nhỏ khi gộp.
  - T-062: lấy luật B-08 ở mục 5.5; plan này giữ B-07.
  - T-053 (đếm ngược 5 giây): sửa màn native sau inter; không đổi cờ "đang hiện".
- Thứ tự chạy (đã thống nhất giữa các plan): T-054, rồi T-067, rồi T-062; T-054 chạy trước T-059 và T-055. Độc lập với T-053, T-063 (xung đột chỉ là gộp dòng trong cùng file).

## 7. Điều kiện dừng riêng

- Một test có sẵn của `AdsControllerNativeComboTest`, `NativeFullScreenTurnsTest` hay `NativeFullScreenStepRecreateTest` đỏ sau khi nối cửa sổ: có thể lượt nạp của chính native toàn màn hình đã bị chặn bởi cửa sổ (trái mục 3.1 C). Dừng, không đổi câu khẳng định của các test đó.
- Phải đổi cách cờ `FullScreenAdPresence.showing` bật hay hạ ngoài phần `publishAfter` của mục 3.1 B (thời điểm bật, trần 10 phút, hồi phục khi kẹt) để test qua: ngoài phạm vi, dừng.

## 8. Câu hỏi cho người duyệt

1. **Hàng native nhỏ trong danh sách (`InListAds`, `loadNativeAd`)** hôm nay nạp một lần khi hàng được dựng, không chờ foreground (T-059 sửa). AI đề xuất: plan này không chạm, T-059 chạy sau và đưa hàng qua cửa sổ (mục 6). Hệ quả: tới khi T-059 xong, một hàng mới cuộn vào dưới màn che vẫn gửi yêu cầu. Nếu muốn T-054 chặn luôn: thêm `awaitSendAllowed(inScreenAdWindow, ::mayRequestNow)` vào `loadNativeAd` và một test, nhưng hàng khi đó cũng chờ foreground (đổi hành vi ở nền mà T-059 định làm).
2. **Banner thu gọn nạp lại tay 30 giây tới hạn lúc bị che** (và lượt thử lại của banner): plan theo trả lời câu 36 "tạm dừng như khi màn hình không hiện", nên gửi đúng một lượt khi màn che đóng (B-05). KB-27 tiêu chí 3 nói "lượt làm mới tới hạn trong lúc bị che thì bỏ"; một lượt khi đóng không dồn, nhưng không "bỏ" hẳn. Đề xuất giữ như B-05 (một luật cho "không hiện" và "bị che"); nếu cần bỏ hẳn thì `runBannerLoads` phải phân biệt hai lý do, thêm một tham số và hai test.
3. **Ô pool chưa có quảng cáo lúc bị che** coi là "cạn" như ở nền (P-05), nên ô ẩn hay hiện khung tĩnh dưới màn che, rồi hiện lại khi quảng cáo về sau lúc đóng. Lý do: để kế hoạch nạp sẵn đi tiếp, không đứng chờ pool. Người dùng không thấy vì ô bị che. Đề xuất giữ.
4. **Cờ kẹt:** cửa sổ đọc `presence.showing`, không đọc trần 10 phút của F-02 (`isBusy`). Nếu cờ kẹt mà không có pause và resume của Activity (F-03 không chạy, ví dụ màn native bị mất callback đóng), quảng cáo trong màn dừng tới khi cờ hạ. Màn native do `:core:ads` vẽ và hạ cờ khi Dialog đóng hay Activity bị hủy (T-026, T-044), nên rủi ro thấp. Đề xuất giữ cờ đơn giản; nếu muốn có trần thì `FullScreenAdPresence` cần một `StateFlow` tự hạ sau 10 phút (thêm file vào mục 3.2).
5. **Điểm nối cho test trong `AdsController`:** plan thêm `nativeLoaderForTest`, `setAppForegroundForTest`, `markSdkReadyForTest` (`@VisibleForTesting`, như `openConsentGateForTest`) để test mức controller chứng minh từng dòng nối (rà soát độc lập, điểm 3). Cách khác là tham số mới của constructor, nhưng controller dựng bằng Hilt nên tham số có giá trị mặc định không dùng được. Đề xuất giữ cách `@VisibleForTesting`.
6. **Inter, app open, rewarded** cũng bật cờ chung; plan để chúng đóng cửa sổ luôn (màn bên dưới đã dừng theo vòng đời nên không đổi gì thấy được, và khớp "cờ đang hiện của cổng chung" trong trả lời câu 36). Có cần chỉ tính native toàn màn hình không? Đề xuất: không.
7. Spec và ticket ghi "ADR-0036, điểm 2", nhưng ADR-0036 hiện đánh số luật này là quyết định 1 (sau khi bỏ hạn dùng interstitial). Plan ghi "ADR-0036, quyết định 1" trong tài liệu mới; sửa chữ của spec và ticket là việc của người.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt các plan."), cùng 11 plan khác của đợt 4 quảng cáo, sau khi xem bảng tóm tắt plan và câu hỏi AI gửi trong phiên. Mọi câu ở mục 8 theo đề xuất của AI. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Trả lời: (1) plan này không chạm hàng native trong danh sách, T-059 sửa. (2) giữ như B-05: một lượt khi màn che đóng. (3) ô pool chưa có quảng cáo lúc bị che coi là cạn. (4) giữ cờ đơn giản, không thêm trần. (5) giữ điểm nối `@VisibleForTesting`. (6) không: mọi quảng cáo toàn màn hình đều đóng cửa sổ. (7) số điểm ADR-0036 trong spec và ticket đã sửa ở PR #305.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261030-01, phiên bản plan 1

- **Kết quả:** đã merge (commit f93c638, 2026-10-30 04:19 UTC).
- **Nhánh, PR:** `claude/T-054-dung-quang-cao-khi-bi-che`, #322.
- **Đã làm:** bước 3 đến 5 giao phiên con trong worktree `../task-T-054`. `InScreenAdWindow.kt` mới; `publishAfter`, `showingNow`, `onPublished` của `FullScreenAdPresence`; cửa sổ nối vào năm chỗ của `AdsController`, `endTurn` trong `publishAfter`, `ConsentCoordinator` đọc `showingNow`; `BannerSlotHost(covered)`; tài liệu B-07, D-06, O-10, CL-02, P-05, mục 12, `ads-guide.md`. Không dùng phương án dự phòng (mục 3.4).
- **Test viết trước:** 35 test (commit 1d04dac), đỏ đúng lý do trên khung rỗng (cửa sổ bỏ qua cờ che, `publishAfter` công bố ngay). 7 test viết chặt thêm đối chứng (L-055). Test JVM mọi module sau bước 3: chỉ 35 test mới đỏ. Commit test có sẵn các điểm nối thật `poolCanLoadNow`, `newNativeLoader` (ghi chú của rà soát).
- **Rà soát độc lập:** vòng 1: 1 chặn, 1 nên sửa, 5 ghi chú. Nên sửa: dòng nối `sendGate = inScreenSendGate` của `NativeAdSources` chưa có test mức controller; thêm `AdsControllerInScreenWindowTest.realController_smallSlot_nextLoadAfterClaimWhileCovered_waits` (commit 68b5e1b), đột biến dòng nối về `foregroundState`: chỉ test mới đỏ (`expected:<1> but was:<2>`), hoàn tác thì xanh. Chặn: mốc thời gian của hai test khác mục 4 (dòng Lệch so với plan); rà soát lại 1 đồng ý hạ xuống ghi chú. Rà soát lại 1: 0 chặn, 0 nên sửa.
- **Kết quả CI:** `pr-check` xanh trên 68b5e1b (lần chạy 41000354801) và trên commit gộp nhánh chính f93c638 (`merge-if-green.sh --update`, vì #320 T-080 do người merge giữa phiên).
- **Lệch so với plan:** không có file ngoài mục 3.2; không viết lại câu khẳng định cũ nào. Mốc thời gian của hai test viết trước khác mục 4, xếp theo `docs/auto-run.md` bước 4 (test sai ở chi tiết không đổi điều nó kiểm), không phải L4 của ADR-0032: `NativeAdRefreshTest.refreshRetryWhileCovered_waitsUntilUncovered` làm mới lỗi ở 65 giây, che ở 66 giây (plan: 70, 71 giây; ở 70 giây lỗi do hết 10 giây chờ nên yêu cầu cũ còn bay, lượt thử lại không gửi yêu cầu mới); `NativeAdRefreshTest.refreshClaimWhileCovered_sendsNothingUntilUncovered` che ở 60 giây ngay sau lượt kiểm cửa sổ (plan: 60,1 giây, lúc đó nguồn đã gửi). `NativeAdSourceTest.twoSlotsCovered_neitherSends_bothResumeOnce`: một lượt nạp chung rồi một lượt thử lại của chỗ thua (đúng "tổng số lượt bằng số lượt đang chờ" của plan).
- **Việc còn lại:** kiểm tra thủ công trên máy thật (mục 4 của plan, dòng "Dừng và chạy lại trên máy").
- **Cần người xem:** hai mốc thời gian nói trên; ghi chú của rà soát: `publish()` đặt `_showing` trước `onPublished` (khoảnh khắc cửa sổ còn mở khi cờ đã bật, chỉ hộp thoại mạng thu cờ), `refreshClaimWhileCovered_...` dựa vào thứ tự của bộ lập lịch ảo (xác định).
