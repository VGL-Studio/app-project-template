---
ticket: T-055
title: Kiểm màn hình trước khi hiện quảng cáo toàn màn hình, xoay máy
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5.4M         # cỡ S, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29); giữ nguyên, không nâng tay (L-107)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-30
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-055: KIỂM MÀN HÌNH TRƯỚC KHI HIỆN QUẢNG CÁO TOÀN MÀN HÌNH, XOAY MÁY

Sửa theo rà soát độc lập (2026-10-29, chưa duyệt nên giữ số phiên bản): dấu che của activity cũ không chuyển sang activity mới, test trường hợp B viết lại theo đó và mục 3.4 là phương án dự phòng thật; trạng thái chờ activity mới nằm trong lượt của `FullScreenAdPresence`, có test đóng giữa lúc chờ; lần hiện lại R-06 bị bỏ chỉ gửi `ad_show_fail`, không thêm cơ hội, không kết thúc lượt; thêm test rewarded khi activity chủ bị che rồi xoay; thêm T-059 và thứ tự chạy ở mục 6. `StuckFullScreenRecovery.kt` ra khỏi mục 3.2.

## 1. Tóm tắt

Trước mọi lần gọi `show` của SDK (interstitial, app open, rewarded; MAX sau này gọi cùng hàm), `:core:ads` kiểm activity sẽ nhận quảng cáo: chưa đóng, chưa hủy và đang là activity resumed của app. Không đạt thì quảng cáo nằm lại trong khe, hàm hoàn tất chạy một lần và có một `ad_show_fail` lý do `app_not_foreground`. Phần xoay máy đã tái hiện được bằng test tạm trên nhánh chính: activity bị tạo lại vì đổi cấu hình làm hoàn tất cưỡng bức chạy ngay, trước khi quảng cáo báo đóng; plan bỏ qua lần hủy có `isChangingConfigurations` và giao lượt cho activity mới của app.

**Phiên bản 2 (2026-10-30).** Bản 1 đã duyệt ngày 2026-10-30 (anh Nhật). Theo rà soát độc lập danh sách chạy RUN-20261030-01: (1) spec đã thêm ngoại lệ "Đổi 2026-10-30" vào KB-16 tiêu chí 7 (inter không được gọi hiện vì màn hình của app không còn dùng được thì không có native, dẫn KB-26 tiêu chí 3); plan dẫn tới ngoại lệ này ở mục 3.1 B, mục 4 (test `interstitial_hostGone_comboOn_noNativeFullScreen`), câu hỏi 2 ở mục 8 và mục 9. (2) Mục 3.2 sửa chỗ kiểm activity trong `showAgain` cho khớp mục 3.1 B và code thật: ở đầu hàm, trước khi đổi `turn.callers`, trước `turn.shows++` và trước lần ghi `busy` (bản 1 ghi "sau `busy`, trước `turn.shows++`", vị trí không có trong code). (3) Mục 6 thêm một câu: nếu T-054 dùng phương án dự phòng `FullScreenAdPresence.handOver(placement)` thì hàm `handOver()` của plan này đổi tên thành `awaitNewHost()`. Giải pháp, danh sách file, test khác và chấm rủi ro không đổi. Mục 9 chờ duyệt lại.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-26 | ~~Interstitial đã tải từ đủ 1 giờ trở lên thì không hiện~~ (bỏ 2026-10-29, câu hỏi 35) | Không làm. Không đụng hạn dùng của interstitial; app open và rewarded giữ hạn 1 giờ như cũ (A-07) |
| KB-26 | ~~Interstitial tải chưa tới 1 giờ thì hiện như trước~~ (bỏ cùng tiêu chí trên) | Không làm |
| KB-26 | Khi màn hình của app không còn dùng được lúc gọi hiện (đang đóng, đã hủy, không ở foreground) thì quảng cáo không được gọi hiện, vẫn giữ trong khe cho lần sau; người dùng đi tiếp ngay và có một sự kiện lỗi hiển thị lý do `app_not_foreground` | "Không ở foreground" hiểu là activity đó không phải activity resumed hiện tại của app (lý do ở mục 3.1 A). Kiểm sau cổng chung (đồng ý, công tắc, đang bận, sẵn sàng, giãn cách) và trước khi lấy quảng cáo ra khỏi khe, nên quảng cáo vẫn trong khe với đúng thời điểm tải cũ. Áp cho mọi đường hiện: interstitial của feature, interstitial và app open lúc mở app, app open và interstitial khi quay lại app, app open của màn xem thử, rewarded (lần hiện đầu và lần hiện lại R-06). Native toàn màn hình vẽ trong cây Compose, không gọi `show` của SDK nên không có kiểm này |
| KB-26 | Khi người dùng xoay máy lúc quảng cáo toàn màn hình đang hiện thì hành động sau quảng cáo (sang màn sau, lưu) chỉ chạy khi quảng cáo đóng, đúng một lần | Lần hủy activity chủ có `isChangingConfigurations` không phải N-04: lượt giữ nguyên, activity mới của app nhận vai activity chủ (để gỡ kẹt F-03 và hoàn tất cưỡng bức N-04 vẫn chạy với nó). Hủy thật (không đổi cấu hình) vẫn hoàn tất cưỡng bức như HD-03 |

Luật liên quan không có tiêu chí riêng mà plan giữ: F-01, F-02 (trần 10 phút), F-03 (gỡ kẹt cần "che rồi hiện lại"), F-07, N-02, N-04, HD-03, A-05, R-04, R-06, mục 8.2 (`ad_show_fail`, `ad_opportunity`).

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); ticket không đổi giao diện.

ADR liên quan: ADR-0026 (hành vi theo luật nghiệp vụ, luật thuần có unit test), ADR-0036 (đợt học từ JBase), ADR-0038 (JBase làm chuẩn, dẫn chứng bằng lớp của bytecode), ADR-0035 (cách ghi "không tái hiện", ở đây không dùng vì đã tái hiện), ADR-0013 (feature không gọi SDK), ADR-0019 (log), ADR-0031 (mục 3.5).

Dẫn chứng JBase (ADR-0038, đọc bytecode của `admobadshelper-release.aar`, không chép code): `com.jb.admobadshelper.AdmobHelper.getCurrentActivity()`, `AdmobHelper$1.getSafeCurrentActivity(String)` và `AdmobHelper.runSafelyOnUiThread(Activity, Runnable)` gọi `Activity.isFinishing()` và `Activity.isDestroyed()` rồi bỏ thao tác, ghi log cảnh báo. JBase không kiểm trạng thái resumed và không có `isChangingConfigurations`. Phần "không ở foreground" là của spec (người phụ trách sản phẩm quyết, đứng trên JBase theo ADR-0038, quyết định 1).

## 3. Giải pháp

### 3.1. Cách làm

**A. Hàm kiểm chung và vì sao chọn "resumed" chứ không phải "started".**

Tạo `FullScreenHost.kt` với hàm thuần `fullScreenHostUsable(finishing: Boolean, destroyed: Boolean, resumed: Boolean): Boolean` (đúng khi không đóng, không hủy, đang resumed). `AdsController` thêm `internal fun isHostUsable(activity: Activity?): Boolean = activity != null && fullScreenHostUsable(activity.isFinishing, activity.isDestroyed, activity === tracker?.resumedActivity)`. Mọi đường gọi `show` của SDK gọi hàm này; MAX (T-069) gọi cùng hàm, ghi trong KDoc.

Lý do chọn resumed:

- `rememberShowFullScreenAd` và `rememberShowAppOpenAd` qua `rememberFullScreenAdAction` lấy `LocalActivity.current` lúc dựng composition, rồi `scope.launch` của composition gọi `showFullScreenAd(activity, ...)`. Từ lúc bấm tới `ad.show(activity)` có các điểm treo (`isEnabled(placement).first()`, đọc `ads_interval`), rewarded chờ tới 5 giây (`DECISION_WAIT_MILLIS`), quảng cáo lúc mở app chờ tới 10 giây (T-024) và `rememberShowStartupAd` cũng giữ activity của composition. Activity đã lấy có thể đã ra nền hay bị tạo lại khi tới lúc hiện. Số đo ở mục 4: nhánh chính gọi `show` của SDK trên activity đã dừng (`state=CREATED`), đang đóng và đã hủy.
- "Started" vẫn đúng khi activity còn thấy nhưng bị che bởi activity khác (hộp thoại quyền, bảng chia sẻ, activity trong suốt, đa cửa sổ dưới API 29). Hiện quảng cáo lúc đó là hiện trên màn người dùng không thao tác. "Resumed" là đúng nghĩa "ở foreground và đang nhận thao tác".
- Đọc resumed từ `AdsActivityTracker.resumedActivity`, không từ `Lifecycle.currentState`: tracker đặt activity resumed trước khi báo listener, nên quảng cáo khi quay lại app (hiện từ `onActivityResumed` qua `ResumeAdTurns`) vẫn qua. Trên API 29 trở lên `Lifecycle` chỉ lên `RESUMED` ở `onActivityPostResumed`, nên đọc `Lifecycle` trong `onActivityResumed` sẽ chặn nhầm quảng cáo quay lại. `ConsentCoordinator` đã dùng đúng mẫu này (`isResumed = { it === tracker?.resumedActivity }`, `AdsController.kt:238`).
- Chưa `start()` thì không có tracker, hàm trả `false`. Lúc đó cổng đồng ý đóng nên không lượt nào tới được bước này; test của plan gọi `start()` như `FullScreenAdsTest.start_startsAppsFlyerAt10s_notBefore`.

**B. Không đạt thì làm gì (một hàm cho mọi định dạng).**

`AdsController` thêm `private fun skipHostGone(placement, onFinished): Boolean`: log `Full-screen <key> skipped: host not usable` (ADR-0019, không ghi mã đơn vị), `telemetry.opportunity(format, placement, "shown", READY)` (câu hỏi 1), `telemetry.showFailed(format, placement, SHOW_FAILED_APP_NOT_FOREGROUND)` với hằng số mới `SHOW_FAILED_APP_NOT_FOREGROUND = 3` cạnh `SHOW_FAILED_INTERNAL` (`showFailReasonOf(3)` đã trả `app_not_foreground`, `AdTelemetryRules.kt:82`), rồi chạy `onFinished` một lần qua `FullScreenCompletion` như `skipTurn`, trả `false`. Không bật cờ "đang hiện", không ghi mốc `ads_interval`, không nạp (quảng cáo còn trong khe), không mở native sau inter (câu hỏi 2; spec KB-16 tiêu chí 7 có ngoại lệ "Đổi 2026-10-30" cho đúng trường hợp này).

Chỗ gọi:

1. `showInterstitialTurn`: sau `decision == SHOW`, trước `beginInterstitialTurn` và `loadedInterstitials.remove`. Không gọi `beginInterstitialTurn` nên không có `af_inters_ad_eligible` và không vào combo.
2. `showAppOpen`: sau `decision == SHOW`, trước `presence.tryBegin` và `loadedAppOpens.take`. Đường này dùng cho lúc mở app, quay lại app và màn xem thử.
3. `showStartupAd`: bỏ kiểm sớm `activity.isFinishing || activity.isDestroyed` (dòng 648, hoàn tất `activity_gone`, không có sự kiện): kiểm ở bước 1 và 2 thay nó, nên lượt mở app có `ad_show_fail` như tiêu chí 3. Cổng đóng hay chưa có quảng cáo thì vẫn bỏ lượt như cũ, `onFinished(false)` (mục 3.5).
4. Rewarded: `RewardedPort` thêm `fun hostUsable(activity: Activity?): Boolean` và `fun recordShowFailed(declaration, code: Int)` (chỉ gửi `ad_show_fail`, `telemetry.showFailed(REWARDED, placement, code)`). Trong `RewardedTurns.decide`, khi đã có quảng cáo (`reason == null`) và trước `port.take`: host không dùng được thì bước mới `DecisionStep.HOST_GONE`: `port.recordOpportunity(declaration, SHOWN, READY)` rồi `port.recordShowFailed(declaration, 3)` (một kết quả cho lần bấm đầu, như bước B), mỗi lần bấm nhập vào ghi `busy` như `endWithNoAd`, kết quả theo R-04 như hiện lỗi: `RewardedOutcome.NoAd(network = false, granted = countNoAd(...))` (câu hỏi 3). Quảng cáo không bị lấy ra khỏi `loadedRewardeds`, nên thời điểm tải và hạn 1 giờ giữ đúng. Lần hiện lại R-06 (`showAgain`): kiểm activity đặt ở **đầu** hàm, trước khi đổi thứ tự `turn.callers` (`RewardedTurns.kt:367-368`), vì `onReward` chỉ giao `Granted` cho `turn.callers.last()` (dòng 407): nếu kiểm sau, người gọi có activity không dùng được thành người cuối và lấy mất thưởng của người gọi trước còn sống. Activity không dùng được thì: không đổi `turn.callers`, không tăng `shows`, không gọi `port.showAgain`, không gọi `onShowFailed`; vẫn ghi `busy` cho lần bấm (kết quả duy nhất của lần bấm theo mục 8.2) và `port.recordShowFailed(declaration, 3)`; người gọi của lần bấm này, nếu chưa có trong `turn.callers`, nhận ngay `NoAd(network = false, granted = false)` để hộp thoại của nó đóng (như người gọi bị chặn ở `endDecision`); đã có trong `turn.callers` thì giữ nguyên chỗ và không nhận gì thêm. Lượt vẫn chờ callback của lần hiện đầu, và vì `shows` không tăng nên một lỗi hiện của lần đầu vẫn kết thúc lượt như cũ. `showRewardedAd` của controller giữ nhánh `activity == null`.

**C. Xoay máy: đã tái hiện, cách sửa.**

Số đo (mục 4): `recreate()` của activity chủ trong lúc lượt interstitial đang mở làm `onAppActivityDestroyed` chạy hoàn tất cưỡng bức (`host_destroyed`), `onFinished` chạy 1 lần và cờ hạ ngay, trước `onInterstitialClosed`. Cả khi activity chủ đang resumed lẫn khi nó đã bị che (pause, stop) trước lúc xoay.

Thứ tự trên máy thật (suy luận, chưa đo; mục 4 có dòng kiểm trên máy): `AdActivity` của SDK tự xử lý xoay (khai báo `configChanges` trong manifest của SDK), không bị tạo lại. `MainActivity` của dự án mẫu không khai báo `configChanges`, nằm sau ở trạng thái stopped; hệ thống chỉ tạo lại activity đã stopped khi nó sắp hiện lại, tức khi `AdActivity` đóng. Rà soát T-022 (mục 10 của plan đó) đã thấy callback đóng của SDK tới sau `onResume` của activity chủ. Vậy khi xoay lúc quảng cáo đang hiện, thứ tự hay gặp là: người dùng đóng quảng cáo, activity chủ cũ bị hủy với `isChangingConfigurations = true` (hoàn tất cưỡng bức chạy ở đây, chỉ vài chục ms trước), activity mới `onCreate`, `onResume`, rồi mới tới `onAdDismissedFullScreenContent`. Còn trường hợp activity chủ bị tạo lại khi quảng cáo còn trên màn: xoay trong khoảng giữa `ad.show` và lúc `AdActivity` che xong, đa cửa sổ, hay đổi cấu hình khác áp ngay cho activity còn thấy (chế độ tối, cỡ chữ, ngôn ngữ). Ở đó `onFinished` chạy khi quảng cáo còn hiện, đúng lỗi của tiêu chí 4. Hai trường hợp đều sửa bằng cùng một quy tắc.

Cách sửa:

1. `FullScreenHost.kt` thêm hàm thuần `hostDestroyedAction(activityId, hostId: Int?, changingConfigurations): HostDestroyedAction` trả `IGNORE` (không có lượt hay không phải activity chủ), `FORCE_COMPLETE` (hủy thật) hay `HAND_OVER` (đổi cấu hình). Trạng thái "đang chờ activity mới" nằm trong lượt, không trong một lớp riêng: `FullScreenAdPresence.Shown` thêm `awaitingHost: Boolean = false`; `FullScreenAdPresence.handOver()` đặt cờ đó cho `current`; `FullScreenAdPresence.rehostIfAwaiting(activityId): Boolean` đổi `hostActivityId` và hạ cờ khi `current` đang chờ, trả `false` (không làm gì) khi `current` là `null` hay không chờ. Mọi đường kết thúc lượt (`onClosed`, gọi từ ít nhất sáu chỗ của `AdsController.kt`: 318, 458, 980, 1080, 1287, 1295) và mọi lượt mới (`begin`) thay `current`, nên trạng thái chờ không bao giờ sống sang lượt sau.
2. `onAppActivityDestroyed`: `HAND_OVER` thì log `Full-screen <key>: host recreated, turn kept`, không gọi `endCurrentTurn`, không gọi `rewardedTurns.onStuck()`. `FORCE_COMPLETE` giữ nguyên như nay.
3. `onAppActivityResumed`: bước đầu, trước `resumeSteps.onResumed` (G-06): `presence.rehostIfAwaiting(id)` (giữ `startedAt`, nên trần 10 phút tính từ lần hiện thật). Dấu "đã bị che" của activity cũ **không** chuyển sang activity mới: activity mới chỉ bị coi là kẹt sau một lần che rồi hiện lại của chính nó (F-03). Vì vậy lần resumed đầu của activity mới không bao giờ gỡ kẹt, lượt chờ callback đóng thật.
4. `onAppActivityPaused` và `StuckFullScreenRecovery` không đổi. Dấu che ghi cho activity cũ (kể cả pause của lần dựng lại, Robolectric báo `changing=true` ở pause đó) gắn với id của activity cũ, nên không bao giờ khớp activity mới; nó bị bỏ ở lần resumed kế tiếp của bất kỳ activity nào có cùng id, như hôm nay.
5. Kết quả, cùng một quy tắc cho cả hai thứ tự: (a) xoay khi quảng cáo che activity chủ (thứ tự hay gặp trên máy): hủy đổi cấu hình giao lượt; activity mới resumed nhận lượt nhưng không có dấu che, nên không gỡ kẹt; callback đóng (hay callback thưởng rồi đóng của rewarded) kết thúc lượt, một lần. Khác với lần đóng không xoay (T-022: `stuck_recovered` có thể tới trước `closed`), ở đây lượt luôn kết thúc bằng callback thật, nên mốc `ads_interval` ghi ở callback đóng (I-05) và rewarded không mất kết quả (`rewardedTurns.onStuck()` không chạy). (b) activity chủ bị tạo lại khi quảng cáo chưa che: như (a); `AdActivity` che activity mới rồi đóng thì callback đóng, hay gỡ kẹt nếu activity mới bị che rồi hiện lại mà callback chưa tới, kết thúc lượt, một lần. Callback đóng thất lạc sau khi xoay: lượt chờ lần che rồi hiện lại kế tiếp của activity mới, hay trần 10 phút (F-02) (câu hỏi 5). Trên nhánh chính (đọc code, chưa đo): hoàn tất cưỡng bức lúc xoay gọi `rewardedTurns.onStuck()`, lượt rewarded bị xóa, nên callback thưởng tới sau (`onReward`) không còn lượt và người dùng mất thưởng.
6. Activity mới thay activity cũ, nên hàm hoàn tất mà feature truyền vào chạy trên đối tượng mà feature đã giữ lúc gọi (lambda của composition cũ, hay ViewModel như `LoadingViewModel.onAdFinished`). Đây là việc sẵn có trước plan, không đổi; mục 8, câu hỏi 4 ghi rõ.

**D. Hàm thử cho test.** `AdsController` thêm ba hàm `@VisibleForTesting internal` như `putPreloadedNativeFullScreenForTest`: `putLoadedInterstitialForTest(placement, ad)`, `putLoadedAppOpenForTest(placement, ad)`, `putLoadedRewardedForTest(placement, ad)` (đặt vào khe với thời điểm `clock.nowMillis()`). Lớp giả `FakeInterstitialAd`, `FakeAppOpenAd`, `FakeRewardedAd` kế thừa lớp trừu tượng của SDK (hàm dựng công khai, đã kiểm bằng `javap` trên `play-services-ads-api-25.5.0`), đếm lần `show` và giữ activity được truyền vào.

**E. Tài liệu.** `docs/ops/ads-business-rules.md`: thêm dòng F-08 ở mục 3 (kiểm activity trước mọi `show`, không đạt thì giữ khe, hoàn tất một lần, `shown` rồi `ad_show_fail` `app_not_foreground`; nguồn JBase như mục 2, ADR-0038); thêm nửa câu ở N-04 và HD-03 (hủy vì đổi cấu hình không phải hủy activity chủ; activity mới của app nhận lượt); mục 8.2, luật đi kèm: `ad_show_fail` mã 3 cũng do `:core:ads` gửi khi activity không dùng được.

Mẫu theo: `skipTurn`, `FullScreenCompletion`, `StuckFullScreenRecovery`, `isActivityGone` của `ConsentCoordinator` (dòng 237), test của `AdsControllerNativeComboTest`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenHost.kt` | `fullScreenHostUsable`, `hostDestroyedAction`, `HostDestroyedAction` (mục 3.1 A, C) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `isHostUsable`, `skipHostGone`, `SHOW_FAILED_APP_NOT_FOREGROUND`; kiểm ở `showInterstitialTurn`, `showAppOpen`; bỏ kiểm sớm của `showStartupAd`; hai hàm mới của port rewarded; `onAppActivityDestroyed`, `onAppActivityResumed` theo mục 3.1 C; ba hàm `put...ForTest`; KDoc của `showInterstitial`, `showStartupAd`, `onAppActivityDestroyed` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresence.kt` | `Shown.awaitingHost`, `handOver()`, `rehostIfAwaiting(activityId)` giữ `startedAt` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt` | `RewardedPort.hostUsable`, `RewardedPort.recordShowFailed`; `DecisionStep.HOST_GONE` trong `decide` trước `port.take`; kiểm trong `showAgain` ở đầu hàm (mục 3.1 B): trước khi đổi `turn.callers` (dòng 366-367), trước `turn.shows++` (dòng 368) và trước lần ghi `busy` (`port.recordOpportunity(declaration, BUSY, ...)`, dòng 369); nhánh activity không dùng được tự ghi `busy`, gửi `recordShowFailed` rồi trả về, không chạm `callers`, `shows`; KDoc |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenHostTest.kt` | Test thuần của mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeFullScreenSdkAds.kt` | `FakeInterstitialAd`, `FakeAppOpenAd`, `FakeRewardedAd` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerHostTest.kt` | Test Robolectric của controller thật (mục 4): kiểm trước khi hiện, xoay máy; activity thử `ChangingActivity` (lớp con của `ComponentActivity`, cờ `changing` mà `isChangingConfigurations()` trả về) khai báo trong file |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | `FakeRewardedPort` thêm `hostUsable` (mặc định `true`), `showFailsRecorded`, thuộc commit test của bước 3 (L-025); 3 test mới |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresenceTest.kt` | 2 test của `handOver`, `rehostIfAwaiting` |
| Sửa | `docs/ops/ads-business-rules.md` | F-08, N-04, HD-03, mục 8.2 (mục 3.1 E) |
| Sửa | `docs/features/ads/tickets/T-055-hien-toan-man-hinh-an-toan.md` | `status`, `plan` |
| Sửa | `docs/features/ads/plans/T-055-hien-toan-man-hinh-an-toan.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): `RewardedPort` chỉ có hai bản thực thi, bản trong `AdsController` và `FakeRewardedPort` của `RewardedTurnsTest`; cả hai trong bảng. `FullScreenAdPresence` chỉ thêm hàm và một tham số có mặc định của `Shown`; chỗ dựng `Shown` và test so `Shown(...)` bằng `==` giữ đúng vì `awaitingHost` mặc định `false`. Nơi nhắc `docs/ops/ads-business-rules.md` (đã `grep`, 2026-10-29): `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` và `scripts/ads-plan.py`. Không bị ảnh hưởng: các test chỉ nhắc tên file trong chú thích và giữ bảng chép tay (khóa mục 10, mã định dạng mục 8.1, sự kiện AppsFlyer mục 8.3), không đọc file lúc chạy; `scripts/ads-plan.py` chỉ dẫn tên file trong chữ và bảng chép tay, đọc `settings.gradle.kts`, bảng khóa và mã nguồn chứ không đọc file luật. Plan thêm dòng F-08 và nửa câu ở N-04, HD-03, mục 8.2, không đổi bảng nào trong số đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: không thêm vị trí hay định dạng; đổi lúc gọi hiện của interstitial, app open, rewarded (KB-26). Điểm chặn quảng cáo
- Sự kiện phân tích: dùng `ad_show_fail` và `ad_opportunity` đã có trong spec (mục 8.2), giá trị `app_not_foreground` đã có; không thêm sự kiện hay tham số. Điểm chặn dữ liệu chỉ vì thêm một đường gửi sự kiện cũ (câu hỏi 1)
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. `Activity.isFinishing`, `isDestroyed`, `isChangingConfigurations` không cần quyền
- **Thư viện và SDK:** Không thêm. Lớp `InterstitialAd`, `AppOpenAd`, `RewardedAd` của `play-services-ads` (đã có trong `gradle/libs.versions.toml`, `:core:ads` đã phụ thuộc) cho lớp giả trong test

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** các test `rotation_*` của mục 4 dựa vào chuỗi vòng đời mà `ActivityController.recreate()` của Robolectric tạo ra, đã đo ở mục 4: activity cũ bị che trước thì activity mới đi `created`, `started`, `resumed`, `paused`, `stopped` (Robolectric đưa nó về trạng thái cũ). Lần `paused` đó của activity mới là "lần che" mà `rotation_closeLost_*` dùng để gỡ kẹt sau `start().resume()`. Bản Robolectric khác có thể không đưa activity mới qua `paused` như vậy.
- **Điều kiện chuyển:** `rotation_coveredHost_*` hay `rotation_closeLost_*` thất bại sau 2 lần sửa vì chuỗi vòng đời của `recreate()` khác chuỗi đã đo (ghi chuỗi thật vào nhật ký), trong khi `FullScreenHostTest` và `handover_closeDuringHandover_*` đã qua.
- **Cách làm thay:** không dùng `recreate()` trong các test đó; dựng chuỗi bằng tay với `ChangingActivity` khai báo trong file test: activity cũ `pause()`, `stop()` (bị che, `changing = false`), đặt `changing = true`, `destroy()`; activity mới `Robolectric.buildActivity(ChangingActivity::class.java).setup()`; "lần che" của activity mới bằng `pause().stop()` rồi `start().resume()`. Câu khẳng định và tên test giữ nguyên; chỉ phần dựng đổi.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` chạy lại ngày 2026-10-29 trên plan sửa theo rà soát (nhánh chính `c29ce2b`, không còn `StuckFullScreenRecovery.kt`): 382 test dùng tên của các file plan sửa; 8 "Cần xem trước", 139 "Nên xem". Lần chạy đầu (396, 10, 155) có thêm hai test "Cần xem trước" ghi dưới, giữ để người duyệt thấy. Đã đọc:

- "Cần xem trước": `AdGalleryScreenTest.flowSteps_*`, `NativeEligibleReportTest.fullScreenStep_*`, `NativeFlowStepLayoutTest.*`, `NativeFlowStepTest.stepCard_doesNotSuppress` khớp chỉ vì tên chung `show`, `take` (trang native của bước, không qua `show` của SDK): không đổi. `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` dùng controller thật không có quảng cáo: lượt hết giờ trước khi có quảng cáo, không tới bước kiểm activity: giữ.
- "Nên xem": `RewardedActionTest.realController_*`, `backgroundWhileDecisionWaits_switchInTime_closesOnce` (controller thật, không có quảng cáo, không tới `take`), `RewardedActionTest` khác (không qua SDK), `ResumeAdTurnsTest.*` (port giả; kiểm activity nằm trong `AdsController`, không trong `ResumeAdTurns`), `AdsControllerResumeTest.*`, `AdsControllerFunnelTest.*` (mở lượt bằng `beginInterstitialTurn`, không qua bước kiểm), `FullScreenAdPresenceTest.*` (chỉ thêm `rehost`), `FreshAdCacheTest.*`, `AppOpenRulesTest.*` và test ảnh chuẩn khớp tên chung `take`, `OnScreen`: giữ, không câu khẳng định nào đổi. Các test có đổi nghĩa gần nhất ghi trong bảng dưới.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Activity chủ bị hủy (mọi lý do) khi lượt chưa báo đóng thì hoàn tất cưỡng bức | Hủy có `isChangingConfigurations` thì giữ lượt và giao cho activity mới; hủy thật như cũ (mục 3.1 C) | `AdsControllerNativeComboTest.hostDestroyed_opensCombo`, `startupTurn_neverOpensCombo`, `standalone_noCurrentTurn_presenceUntilClose` gọi `onAppActivityDestroyed(activity)` với activity chưa đổi cấu hình | Giữ: activity của các test này không đổi cấu hình (`isChangingConfigurations == false`), nên vẫn hoàn tất cưỡng bức |
| `showStartupAd` với activity đang đóng hay đã hủy: `onFinished(false)` ngay, không sự kiện | Kiểm ở `showAppOpen`, `showInterstitialTurn`: cổng đóng hay chưa có quảng cáo thì bỏ lượt như cũ; cổng cho hiện thì `shown` rồi `ad_show_fail` mã 3, `onFinished(false)` (mục 3.1 B, bước 3) | `AdsControllerNativeComboTest.showStartupAd_blocked_neverOpensCombo` (activity sống, cổng đóng) | Giữ: cổng đóng nên kết quả vẫn `[false]`, không combo |
| Rewarded có quảng cáo thì luôn lấy ra khỏi khe và gọi `show` | Activity không dùng được thì không lấy, ghi `shown` và `ad_show_fail` mã 3, kết quả theo R-04 (mục 3.1 B, bước 4) | `RewardedTurnsTest` (mọi test có `take`) | Giữ: `FakeRewardedPort.hostUsable` mặc định `true` |

## 4. Kiểm thử

Số đo trên code hiện tại (L-029), test tạm `TmpT055ProbeTest` trong `core/ads/src/test`, không commit, đã xóa; commit `c29ce2b` (nhánh chính), 2026-10-29. Lệnh `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TmpT055ProbeTest'`: 8 test, 0 lỗi, 19 giây (Gradle có cache). Controller thật từ `testAdsController`, `start(application, AppActivityFilter.inPackageOf(ComponentActivity::class.java))` để có tracker, activity `Robolectric.buildActivity(ComponentActivity::class.java).setup()`.

Phần 1 (gọi hiện khi activity không dùng được). Cổng mở bằng `openConsentGateForTest`, `is_show_ads` và `is_show_gallery_interstitial` bật, một `InterstitialAd` giả đặt vào `loadedInterstitials` bằng reflection (chỉ trong test tạm), gọi `showInterstitial(activity, GALLERY_INTERSTITIAL)`:

| Activity | `show` của SDK được gọi | Trả về | `onFinished` | Cờ đang hiện | Sự kiện |
| --- | --- | --- | --- | --- | --- |
| resumed (đối chứng) | 1 lần, `state=RESUMED` | `true` | 0 | `true` | không có |
| đã `pause().stop()` | 1 lần, `state=CREATED` | `true` | 0 | `true` | không có |
| `finish()` | 1 lần, `finishing=true` | `true` | 0 | `true` | không có |
| `pause().stop().destroy()` | 1 lần, `destroyed=true` | `true` | 0 | `true` | không có |

`showStartupAd(activity đã hủy, OPEN_APP)`: kết quả `[false]`, không sự kiện nào (không có `ad_show_fail`). Vậy tiêu chí 3 hỏng ở mọi trạng thái: SDK được gọi hiện trên activity không dùng được, cờ bật chờ callback, không có `app_not_foreground`.

Phần 2 (xoay máy). `beginInterstitialTurn(GALLERY_INTERSTITIAL, id của activity, combo = false)`, rồi:

| Trường hợp | Chuỗi vòng đời đo được | Sau khi dựng lại | Sau `onInterstitialClosed` |
| --- | --- | --- | --- |
| A. Activity chủ đang resumed, `recreate()` | `paused changing=true`, `stopped changing=true`, `destroyed changing=true`, activity mới `created`, `started`, `resumed` | `onFinished` = 1, cờ `false`, log `host destroyed before close`, `Full-screen turn finished: host_destroyed` | `onFinished` = 1 (không chạy lần hai) |
| B. Activity chủ đã `pause().stop()` (bị `AdActivity` che), rồi `recreate()` | `paused changing=false`, `stopped changing=false`, `destroyed changing=true`, activity mới `created`, `started`, `resumed`, `paused changing=false`, `stopped changing=false` | `onFinished` = 1, cờ `false`, cùng log | `onFinished` = 1 |
| C. Đối chứng: activity chủ bị che rồi `finish()`, `destroy()` | `destroyed changing=false finishing=true` | `onFinished` = 1, cờ `false` (đúng HD-03) | `onFinished` = 1 |

Kết luận: tái hiện được. Trên code hiện tại, dựng lại activity vì đổi cấu hình chạy hàm hoàn tất ngay khi activity cũ bị hủy, trước khi quảng cáo báo đóng, và hạ cờ "đang hiện" trong lúc quảng cáo có thể còn trên màn (một quảng cáo khác hay native sau inter có thể mở chồng, F-01). Theo ticket, plan sửa phần này (mục 3.1 C). Thứ tự thật trên máy (callback đóng tới trước hay sau lần hủy) chưa đo được trên JVM, nên có dòng kiểm trên máy thật ở bảng dưới.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-26, tiêu chí 3: hàm kiểm | Unit test | `FullScreenHostTest.hostUsable_onlyWhenAliveAndResumed`: 8 tổ hợp `(finishing, destroyed, resumed)`, chỉ `(false, false, true)` trả `true` |
| KB-26, tiêu chí 3: interstitial, activity đã dừng | Unit test Robolectric | `AdsControllerHostTest.interstitial_stoppedHost_notShown_keptInSlot_showFailOnce`: controller `start()`, cổng mở, khóa bật, `putLoadedInterstitialForTest(GALLERY_INTERSTITIAL, fake)`; activity `pause().stop()`; `showInterstitial` trả `false`, `fake.shows == 0`, `onFinished` 1 lần, `isFullScreenAdReady(GALLERY_INTERSTITIAL) == true`, `fullScreenAdShowing == false`, `RecordingAdMetrics` có đúng một `ShowFail("INTER", "gallery_interstitial", "3", "app_not_foreground")` và một cơ hội `shown`. Đối chứng trong cùng test: activity thứ hai resumed, gọi lại thì `fake.shows == 1`, trả `true`. Trên nhánh chính: `fake.shows == 1` ở activity đã dừng (bảng trên) |
| KB-26, tiêu chí 3: interstitial, activity đang đóng, đã hủy | Unit test Robolectric | `interstitial_finishingHost_notShown_keptInSlot`, `interstitial_destroyedHost_notShown_keptInSlot`: như trên với `finish()` và `pause().stop().destroy()` |
| KB-26, tiêu chí 3: không mở native sau inter | Unit test Robolectric | `interstitial_hostGone_comboOn_noNativeFullScreen`: `IS_SHOW_NATIVE_AFTER_INTER` bật, activity đã dừng; sau `idleFor(2_100)` `nativeFullScreenView.value == null`, `onFinished` 1 lần; đối chứng: code hôm nay mở native khi inter lỗi hiển thị, nên câu này đỏ trước phần sửa. Khớp ngoại lệ "Đổi 2026-10-30" của KB-16 tiêu chí 7 trong spec |
| KB-26, tiêu chí 3: app open (màn xem thử) | Unit test Robolectric | `appOpen_stoppedHost_notShown_keptInSlot_showFailOnce`: `putLoadedAppOpenForTest(GALLERY_APP_OPEN, fake)`, `showStagingAppOpen(activity đã dừng, ...)`: `fake.shows == 0`, `onFinished` 1, quảng cáo còn trong khe, `ShowFail("APP_OPEN", "gallery_app_open", "3", "app_not_foreground")` một lần; đối chứng activity resumed: `fake.shows == 1` |
| KB-26, tiêu chí 3: lúc mở app | Unit test Robolectric | `startup_destroyedHost_appOpen_showFail_finishesFalse`: khóa chọn `open_app` của lần mở này (như `StartupAdChoiceTest`), `putLoadedAppOpenForTest(OPEN_APP, fake)`, `showStartupAd(activity đã hủy, StartupAdTurn(OPEN_APP))`: kết quả `[false]`, `fake.shows == 0`, một `ShowFail` mã 3. Trên nhánh chính: `[false]`, không sự kiện (bảng trên) |
| KB-26, tiêu chí 3: quay lại app vẫn hiện (đối chứng của cách đọc resumed) | Unit test Robolectric | `resumeAd_fromOnActivityResumed_passesHostCheck`: theo cách dựng của `AdsControllerResumeTest` (resume bật, app open sẵn trong khe bằng `putLoadedAppOpenForTest(RESUME, fake)`), đưa activity ra nền rồi về (`pause().stop()`, `start().resume()`): `fake.shows == 1` và `show` nhận đúng activity đó. Đột biến: đọc `Lifecycle.currentState` thay cho tracker thì test này đỏ |
| KB-26, tiêu chí 3: rewarded, lần hiện đầu | Unit test (JVM) và Robolectric | `RewardedTurnsTest.hostGone_notTaken_showFailRecorded_outcomeByPolicy`: port giả `hostUsable = false`, có quảng cáo: `takes == 0`, `shows == 0`, cơ hội ghi đúng `[shown]`, `showFailsRecorded == [3]`, kết quả `NoAd(network = false, granted = <theo chính sách>)` một lần; `hostGone_joinedTaps_busy`: hai lần bấm, lần sau ghi `busy`. `AdsControllerHostTest.rewarded_stoppedHost_notShown_keptInSlot`: `putLoadedRewardedForTest(GALLERY_RW_ACTION, fake)`, `showRewarded(activity đã dừng, ...)`, `idle()`: `fake.shows == 0`, `isFullScreenAdReady == true`, `ShowFail("REWARDED", "gallery_rw_action", "3", "app_not_foreground")` một lần |
| KB-26, tiêu chí 3: rewarded, lần hiện lại R-06 | Unit test | `RewardedTurnsTest.showAgain_hostGone_noSecondShow_turnGoesOn`: lượt đang chờ, lần bấm thứ hai bằng người gọi khác (`second`) với `hostUsable = false`: `showAgains == 0`, cơ hội của lần bấm đó đúng `[busy]` (không có `shown` thêm), `showFailsRecorded == [3]`, `second` nhận đúng một `NoAd(network = false, granted = false)`, lượt vẫn chờ; quảng cáo đầu báo thưởng thì người gọi đầu (`first`, activity còn dùng được) nhận `Granted` đúng một lần và `second` không nhận `Granted`. Đột biến: kiểm activity sau khi đổi thứ tự `turn.callers` thì `Granted` sang `second` và test đỏ. Đối chứng trong cùng file, `showAgain_hostGone_firstShowFails_endsTurnByPolicy`: sau lần bấm hai bị bỏ, `turns.onShowFailed(d, 0)` của lần hiện đầu kết thúc lượt ngay (vì `shows` không tăng), người gọi sau cùng nhận `NoAd(granted = <theo chính sách>)` một lần |
| KB-26, tiêu chí 4: phần thuần | Unit test | `FullScreenHostTest.hostDestroyedAction_configChange_handsOver_realDestroy_forces`: `(host, host, changing=true)` là `HAND_OVER`, `(host, host, false)` là `FORCE_COMPLETE`, activity khác hay `hostId == null` là `IGNORE`. `FullScreenAdPresenceTest.rehostIfAwaiting_onlyOnce_keepsStartedAt`: `tryBegin(p, 1)`, `handOver()`, `rehostIfAwaiting(2)` trả `true`, `current.hostActivityId == 2`, lần sau `rehostIfAwaiting(3)` trả `false`; `isBusy` hết đúng ở 600 000 ms từ `tryBegin`; không `handOver` thì `rehostIfAwaiting` luôn `false`. `handOver_clearedByCloseAndNewTurn`: `tryBegin(p, 1)`, `handOver()`, `onClosed()`, `rehostIfAwaiting(9)` trả `false` (current `null`); `tryBegin(p, 2)`, `rehostIfAwaiting(9)` trả `false`, `current.hostActivityId == 2` |
| KB-26, tiêu chí 4: activity chủ resumed bị tạo lại (trường hợp A) | Unit test Robolectric | `AdsControllerHostTest.rotation_resumedHost_recreated_turnKept_finishesOnClose`: tracker có, `beginInterstitialTurn(..., id của activity, combo = false)`, `controller.recreate()`: `onFinished == 0`, `fullScreenAdShowing == true`; `onInterstitialClosed` thì 1; `idleFor(5_000)` vẫn 1. Trên nhánh chính: 1 ngay sau `recreate()` (bảng trên) |
| KB-26, tiêu chí 4: activity chủ bị che rồi bị tạo lại (trường hợp B) | Unit test Robolectric | `rotation_coveredHost_recreated_turnKept_finishesOnClose`: `pause().stop()`, `recreate()` (activity mới đi `resumed` rồi `paused`, `stopped` như đo ở trên; lần `resumed` đó nhận lượt và không gỡ kẹt vì không mang dấu che của activity cũ): ngay sau `recreate()` `onFinished == 0`, cờ `true`, log có `host recreated, turn kept`, không có `stuck_recovered`; `onInterstitialClosed` thì 1, đúng một lần. Trên nhánh chính: 1 ngay sau `recreate()` |
| KB-15, tiêu chí 5 sau khi xoay (callback đóng thất lạc) | Unit test Robolectric | `rotation_closeLost_newHostCoveredThenBack_recoversOnce`: trường hợp B, không gọi `onInterstitialClosed`; activity mới đã có một lần che của chính nó (lần `paused` của `recreate()` ở trên), rồi `start().resume()`: `onFinished == 1`, cờ `false`, log `stuck_recovered`; gọi `onInterstitialClosed` sau đó thì vẫn 1. Câu này cho biết lượt thất lạc được gỡ ở lần che rồi hiện lại kế tiếp của activity mới, không sớm hơn (câu hỏi 5) |
| Trạng thái chờ không sống sang lượt sau | Unit test Robolectric | `handover_closeDuringHandover_nextTurnForcedOnce`: gọi thẳng hàm của controller như `AdsControllerNativeComboTest`; lượt 1 trên `a1` (`ChangingActivity`, `changing = true`), `onAppActivityDestroyed(a1)` (giao lượt), `onInterstitialClosed(lượt 1)` trước khi activity mới nào resumed: `onFinished1 == 1`; lượt 2 trên `a2`; `onAppActivityResumed(a3)` (activity mới khác): `current.hostActivityId` vẫn là `a2`; `onAppActivityDestroyed(a2)` (`changing = false`): `onFinished2 == 1` đúng một lần, cờ `false`. Đột biến: giữ trạng thái chờ ngoài lượt (không xóa ở `onClosed`) thì `onFinished2 == 0` |
| HD-03 giữ nguyên | Unit test Robolectric | `realDestroy_coveredHost_forcesCompletion`: trường hợp C với tracker thật: `onFinished == 1` ngay sau `destroy()`, cờ `false` |
| Rewarded khi xoay, activity chủ đang resumed (trường hợp A) | Unit test Robolectric | `rotation_rewardedTurn_resumedHost_kept`: `putLoadedRewardedForTest`, `showRewarded(activity resumed)`, `idle()` (lượt đã mở, `fake.shows == 1`), `recreate()`: lượt vẫn mở (`fullScreenAdShowing == true`, chưa có kết quả nào ngoài `OnScreen` nếu callback đã báo); callback thưởng của `fake` sau đó cho `Granted` một lần, callback đóng hạ cờ |
| Rewarded khi xoay, activity chủ bị che rồi bị tạo lại (trường hợp B, thứ tự hay gặp trên máy) | Unit test Robolectric | `rotation_rewardedTurn_coveredHost_rewardKept`: như trên, rồi `fake` báo `onAdShowedFullScreenContent` (người gọi nhận `OnScreen`), activity `pause().stop()`, `recreate()`: lượt vẫn mở, không có log `stuck`; `fake` báo thưởng: người gọi nhận `Granted` đúng một lần; `fake` báo đóng: cờ `false`; activity mới `start().resume()` sau đó: không thêm kết quả nào. Kết quả mong đợi: thưởng được giao. Trên nhánh chính (đọc code): `recreate()` chạy `rewardedTurns.onStuck()`, callback thưởng tới sau không có lượt, `Granted == 0` |
| Ra nền giữa lúc chờ (L-091) | Unit test Robolectric | Đã gồm ở các test "activity đã dừng" trên: activity ra nền trước khi tới `show`; `rewarded_stoppedHost_*` là đúng trường hợp chờ quyết định rồi mới hiện |
| Xoay máy với quảng cáo thật của SDK | Kiểm tra thủ công (máy thật: khác, thứ tự giữa callback đóng của `AdActivity` thật và lần dựng lại `MainActivity` chỉ có trên máy với SDK thật, Robolectric không có `AdActivity`) | Bản staging, đồng ý quảng cáo, màn xem thử, bấm Interstitial; khi quảng cáo thử đang hiện thì xoay máy, đợi 3 giây, đóng quảng cáo. Đạt: màn xem thử hiện lại (dựng lại theo chiều mới), log `Ads` có `host recreated, turn kept` rồi đúng một dòng `Full-screen turn finished:` (`stuck_recovered` hay `closed`), không có `host_destroyed`. Làm lại với Rewarded (thưởng một lần) và với app open của màn xem thử. Ghi vào báo cáo: callback đóng tới trước hay sau `onResume` của activity mới |
| Bấm rồi về nền với quảng cáo thật | Kiểm tra thủ công (máy thật: khác, quảng cáo thử của SDK thật chưa nạp xong thì không có lượt hiện; JVM đã có test cho logic) | Màn xem thử, bấm Rewarded khi quảng cáo chưa nạp, ngay lập tức bấm Home; mở lại app sau 6 giây: không có quảng cáo nào bung lên, log có `skipped: host not usable` hoặc hết 5 giây không có quảng cáo |

**Ngoại lệ viết test trước:** Hai dòng kiểm trên máy thật (quảng cáo của SDK thật). Phần tài liệu ở mục 3.1 E. Không có ngoại lệ khác: mọi đường gọi `show` (interstitial của feature, lúc mở app, quay lại app, màn xem thử, rewarded lần đầu và lần lại) có test riêng (L-072).

### 4.1. Chạy thử

Không cần (E dưới 2): kiểm activity theo mẫu `isActivityGone` và `isResumed` của `ConsentCoordinator`; dựng lại activity bằng `ActivityController.recreate()` đã dùng ở T-022 và T-024. Test tạm của mục 4 đã chạy bằng `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TmpT055ProbeTest'` (8 test, 0 lỗi, 19 giây, 2026-10-29, commit `c29ce2b`, JDK 21 và SDK từ `scripts/local-setup.sh`); chưa chạy `scripts/check.sh` vì E dưới 2 không bắt buộc. Lần chạy đầu của test tạm bị treo hơn 10 phút vì `runBlocking` trên luồng chính của Robolectric chờ `Dispatchers.Main.immediate`; test của plan gọi hàm `suspend` bằng `CoroutineScope(Dispatchers.Main).launch` rồi `shadowOf(Looper.getMainLooper()).idle()`, như `AdsControllerNativeComboTest`.

Kết quả `scripts/existing-tests.py` (nhóm "Cần xem trước", "Nên xem") đã đọc và ghi ở mục 3.5.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi lúc gọi hiện của ba định dạng toàn màn hình, điểm 1), thu thập dữ liệu (thêm một đường gửi `ad_show_fail` đã có, điểm 4), module core dùng chung `:core:ads` (điểm 7). Mức Cao, bắt buộc duyệt chéo; người phụ trách sản phẩm biết trước khi duyệt (quảng cáo, dữ liệu).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Bốn file code và năm file test trong một module; đổi giao diện nội bộ `RewardedPort` (hai hàm mới) |
| B. Độ rõ của yêu cầu | 2 | Tiêu chí rõ, nhưng còn câu hỏi ảnh hưởng tới cách làm: ghi cơ hội `shown` hay không (câu 1), có native sau inter không (câu 2, có thể trái KB-16 tiêu chí 7), kết quả của rewarded (câu 3) |
| C. Khả năng kiểm chứng tự động | 3 | Vòng đời ứng dụng: dựng lại activity, thứ tự giữa callback của SDK và lần dựng lại chỉ thấy trên máy thật; Robolectric không có `AdActivity` |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt` cũng được T-054, T-058, T-063 sửa (plan chưa merge); T-069 (MAX) dựa vào hàm kiểm của plan |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn (`ConsentCoordinator`, `StuckFullScreenRecovery`, `recreate()` của T-022, T-024) |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `RewardedTurns.kt`, `FullScreenAdPresence.kt`, `docs/ops/ads-business-rules.md`. Mọi feature gọi quảng cáo toàn màn hình qua `rememberShowFullScreenAd`, `rememberRewardedAction`, màn loading (`rememberShowStartupAd`) và vỏ app (quảng cáo khi quay lại) đi qua phần đổi.
- Task khác trong sprint có thể xung đột: T-054 (sửa `AdsController.kt` ở phần quảng cáo trong màn hình, `ads-business-rules.md` các dòng B-07, D-06, O-10, CL-02, P-05, mục 12), T-058 (`AdsController.kt` phần khởi động SDK và callback tải, `AdsControllerFixtures.kt`), T-059 (`AdsController.kt`, phần native trong danh sách thử lại khi lỗi), T-063 (KDoc của `isEnabled`, `ads-business-rules.md` MC-11): khác vùng code và khác dòng luật, chỉ xung đột vị trí khi merge. T-069 (MAX) phải gọi `isHostUsable` trước `show` của MAX, ghi trong KDoc của hàm.
- Trùng tên với T-054: plan T-054 có phương án dự phòng `FullScreenAdPresence.handOver(placement)` (nghĩa khác). Nếu T-054 rơi vào phương án đó thì T-055 đổi tên hàm `handOver()` của mình (mục 3.1 C, 3.2, 4) thành `awaitNewHost()`; hành vi không đổi.
- Đề xuất thứ tự chạy (theo rà soát độc lập): T-054, rồi T-058, rồi T-059, rồi T-055 nếu chúng cùng danh sách (vùng sửa của chúng trong `AdsController.kt` lớn hơn); T-055 trước T-069, vì MAX phải gọi `isHostUsable`.

## 7. Điều kiện dừng riêng

- Một test có sẵn kiểm thứ tự vòng đời hay gỡ kẹt (`AdsActivityTrackerTest`, `ResumeAdLifecycleTest`, `AdsControllerResumeTest`, `StuckFullScreenRecoveryTest`, `AdsControllerNativeComboTest.stuckRecovered_opensCombo`) đỏ: dừng, vì plan không đổi F-03 và `StuckFullScreenRecovery`.
- Quảng cáo khi quay lại app (`ResumeAdTurns`) không còn hiện ở test `resumeAd_fromOnActivityResumed_passesHostCheck` mà không sửa được trong cách đọc resumed của mục 3.1 A: dừng, không chuyển sang đọc `Lifecycle`.

## 8. Câu hỏi cho người duyệt

1. **Lượt bị bỏ vì activity không dùng được có ghi `ad_opportunity` không, giá trị gì?** Mục 8.2 nói mỗi lượt gọi hiện có đúng một kết quả. Đề xuất của AI: ghi `shown` (cổng đã cho hiện) rồi `ad_show_fail` `app_not_foreground`, giống hệt khi SDK tự báo lỗi hiện mã 3, để tỷ lệ lỗi hiện (`ad_show_fail` trên `shown`) giữ một cách tính. Phương án khác: ghi `not_ready`, hay không ghi cơ hội.
2. **Interstitial bị bỏ vì activity không dùng được có mở native sau inter không?** KB-16, tiêu chí 7 nói inter đã được phép hiện mà không lên được màn ("lỗi hiển thị") thì vẫn có native; KB-26, tiêu chí 3 nói "người dùng đi tiếp ngay". Hai câu có thể mâu thuẫn ở trường hợp này. Đề xuất của AI: không mở native, đi tiếp ngay theo KB-26 (kịch bản mới, nói thẳng trường hợp này), vì native vẽ trong cây Compose của chính activity đang đóng hay ở nền: không ai thấy và đếm ngược chạy ở nền. Cần anh Nhật chốt và ghi vào spec nếu chọn khác. Đã chốt: spec KB-16 tiêu chí 7 có ngoại lệ "Đổi 2026-10-30" (trừ khi interstitial không được gọi hiện vì màn hình của app không còn dùng được, KB-26 tiêu chí 3), nên hai tiêu chí không còn mâu thuẫn.
3. **Rewarded bị bỏ vì activity không dùng được thì hành động có chạy không?** Đề xuất của AI: như hiện lỗi (R-04): `NoAd(network = false, granted = <theo chính sách vị trí>)`, có tính một lần không có quảng cáo. Người dùng thường đã ra nền hay xoay máy giữa lúc chờ; hộp thoại của rewarded vẫn ở màn khi họ quay lại. Phương án khác: `NoAd(granted = false)` không tính lần thử.
4. **Hàm hoàn tất của feature giữ đối tượng của activity cũ.** Sau khi xoay, `onFinished` mà feature truyền vào `rememberShowFullScreenAd` là lambda của composition cũ (ví dụ một `navController` đã gỡ); điều hướng trong đó có thể mất. Plan không đổi phần này (ticket không nêu, và màn loading dùng `LoadingViewModel.onAdFinished` nên không bị). Đề xuất của AI: ghi vào `docs/ops/ads-guide.md` ở một ticket sau: hành động sau quảng cáo đi qua ViewModel hay trạng thái giữ qua dựng lại. Có làm trong ticket này không?
5. **Callback đóng thất lạc sau khi xoay (cả hai trường hợp của mục 3.1 C):** dấu che của activity cũ không chuyển sang activity mới, nên lượt chờ lần che rồi hiện lại kế tiếp của activity mới, hay trần 10 phút (F-02), chứ không gỡ ngay khi người dùng về app như KB-15, tiêu chí 5 ở lượt không xoay. Lý do chọn: chuyển dấu che làm lần resumed đầu của activity mới gỡ kẹt ngay, trước callback đóng thật, nên rewarded mất thưởng và tiêu chí 4 của KB-26 không giữ được. Cùng loại khoảng hở đã biết, rà soát lại chấp nhận là ghi chú: lượt thất lạc chỉ được trần 10 phút của F-02 gỡ khi activity mới không bị che lần nào; app có nhiều activity thì activity đầu tiên của app resumed sau `HAND_OVER` nhận lượt, có thể không phải bản mới của activity chủ; activity mới bị hủy trước khi kịp resumed thì lượt vẫn chờ tới trần. Đề xuất của AI: chấp nhận các khoảng hở này cho trường hợp hiếm; không thêm hẹn giờ.

## 9. Quyết định duyệt

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt lại trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt."), cùng năm plan khác sửa theo rà soát độc lập danh sách chạy RUN-20261030-01, sau khi xem tóm tắt các điểm đã sửa và câu hỏi AI gửi trong phiên. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Các trả lời ở mục 9 của bản trước giữ nguyên. Bản này chỉ dẫn ngoại lệ mới của KB-16 tiêu chí 7, sửa chỗ kiểm activity trong `showAgain` ở mục 3.2, thêm cách đổi tên khi T-054 dùng dự phòng.

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt các plan."), cùng 11 plan khác của đợt 4 quảng cáo, sau khi xem bảng tóm tắt plan và câu hỏi AI gửi trong phiên. Mọi câu ở mục 8 theo đề xuất của AI. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Trả lời: (1) ghi `shown` rồi `ad_show_fail` `app_not_foreground`. (2) không mở native, đi tiếp ngay theo KB-26. (3) như R-04: `NoAd(network = false, granted = <theo chính sách vị trí>)`, có tính một lần. (4) không làm trong ticket này; hướng dẫn "hành động sau quảng cáo đi qua ViewModel" ghi ở một ticket sau. (5) chấp nhận các khoảng hở cho trường hợp hiếm, không thêm hẹn giờ.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
