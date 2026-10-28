---
ticket: T-048
title: Dọn rewarded theo ghi chú rà soát T-047
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 4.9M         # cỡ S, số gợi ý 4.9M (scripts/token-usage.py estimate, 2026-10-28); việc nhỏ, không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-28
---

# PLAN T-048: DỌN REWARDED THEO GHI CHÚ RÀ SOÁT T-047

## 1. Tóm tắt

Sửa sau rà soát độc lập của plan (2026-10-28, chưa duyệt nên giữ số phiên bản): đổi đề xuất ở câu hỏi 1 sang phương án (c) (chỉ bỏ `OnScreen` lặp trong một lần quyết định); thêm hai test cổng đổi trong lúc chờ khóa (L-072); thêm câu R-06 vào phần sửa luật; thêm dòng mục 3.5 cho lượt cũ còn treo.

Gom bốn ghi chú mà rà soát độc lập của T-047 nêu (báo cáo RUN-20261028-01, mục Cần người kiểm tra). Đã chạy thử cả bốn trên code hiện tại (mục 4.1):

- **Test lượt nạp hết giờ đọc khóa:** thêm hai câu khẳng định "không gọi `clearLoading`, `sendBlocked`". Câu mới qua trên code hôm nay; đột biến `NOT_NEEDED` thành `SWITCHED_OFF` hay `GATE_CLOSED` làm nó đỏ, còn test cũ vẫn xanh (đã đo).
- **`OnScreen` nhiều lần cho một bộ xử lý:** đúng như ghi chú, một bộ xử lý nhập vào một lần quyết định từ ba lần bấm trở lên nhận `OnScreen` mỗi lần `showAgain` khi SDK gọi đồng bộ (đã đo `[OnScreen, OnScreen, Granted]`). Ngoài ra, bộ xử lý bấm lần nữa sau khi quảng cáo đã lên màn cũng nhận `OnScreen` lần nữa (cả SDK không đồng bộ). Lần lặp thứ hai này có ích: nó đóng hộp thoại khi một lượt cũ còn treo (mục 3.5). Plan chỉ bỏ lần lặp trong một lần quyết định, giữ lần thứ hai, và sửa KDoc cho đúng (phương án (c), câu hỏi 1).
- **KDoc của `decide`:** "asks for no load" thành "asks for no further load".
- **Hết giờ chờ khóa khi cổng đồng ý đóng:** hôm nay ghi `not_ready` và áp chính sách. Theo MC-05, cổng đóng thì dù khóa bật hay tắt, hành động đều làm thẳng, không ghi cơ hội. Plan làm đúng như vậy khi hết giờ lúc còn chờ khóa. Đây là đổi luật mục 11, anh Nhật đã duyệt (câu hỏi 2).

Người dùng không thấy gì đổi ở đường thường: lần `OnScreen` lặp mà plan bỏ đến khi hộp thoại đã đóng (cùng lần quyết định), nên không làm gì. Một chỗ người dùng có thể thấy khác, hiếm: hết giờ chờ khóa khi cổng đồng ý đóng thì hành động chạy (phần D, câu hỏi 2). Lượt cũ còn treo vẫn đóng hộp thoại như hôm nay, vì phương án (c) giữ `OnScreen` cho lần bấm đến sau khi đã lên màn (mục 3.5).

## 2. Hiểu yêu cầu

Ticket xử lý KB-19 trong `features/ads/spec.md` (spec `confirmed`), luật MC-05, R-03, R-06, mục 8.2 và mục 11 của `docs/ops/ads-business-rules.md`. Nguồn: báo cáo `RUN-20261028-01`, dòng "Điểm rà soát mức ghi chú: T-047", ghi chú (1) đến (4); plan T-047 mục 10.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | 4. Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí (...) | Lượt nạp hết giờ đọc khóa không gửi, không gọi `clearLoading` hay `sendBlocked` (đúng chữ plan T-047, mục 3.1 bước 2). Test khóa thêm điều đó |
| KB-19 | 3. Khi người dùng xem đủ thì nhận thưởng ngay lúc quảng cáo báo thưởng | Trong một lần quyết định, mỗi bộ xử lý nhận `OnScreen` một lần, rồi `Granted` cho người gọi mới nhất; đúng cách hiểu plan T-047 đã duyệt ("mỗi bộ xử lý nhận `OnScreen` một lần, trước `Granted`"). Lần bấm đến sau khi quảng cáo đã lên màn vẫn nhận `OnScreen` ngay, như hôm nay; KDoc của lớp nói rõ ngoại lệ này |
| KB-19 | 6. Bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì như AdsKit, ghi `busy` | Giữ nguyên: lần bấm sau vẫn gọi `showAgain`, vẫn ghi `busy`; chỉ bỏ `OnScreen` lặp cho bộ xử lý đã nhận trong cùng lần quyết định |
| KB-19 | 2. Khi vị trí tắt thì hành động làm ngay, không có hộp thoại | MC-05: cổng đồng ý đóng mà khóa bật cũng coi như vị trí tắt. Hết giờ khi còn chờ khóa mà cổng đóng: khóa bật hay tắt đều cho cùng kết quả, nên làm như cổng đóng (`Granted`, không ghi cơ hội). Đã duyệt (câu hỏi 2) |

Thiết kế tham chiếu: Không có thiết kế riêng (chỉ logic, không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013, ADR-0026 (nghiệp vụ theo AdsKit PARTNER), ADR-0027 (số liệu quảng cáo), ADR-0029, ADR-0031, ADR-0032.

## 3. Giải pháp

### 3.1. Cách làm

Bốn phần, đều trong `RewardedTurns.kt` và `RewardedTurnsTest.kt`, cộng ba câu tài liệu.

**A. Test lượt nạp hết giờ đọc khóa (ghi chú 1).** Trong `loadRound_switchStuck_releasesAfterWait`, ngay sau `advanceTimeBy(5_001)`, `runCurrent()` và trước lượt `turns.loadIfNeeded(rwGetItem)` thứ ba, thêm:

```kotlin
// The round that timed out reads no step (plan T-047): nothing cleared, nothing blocked.
assertTrue(port.cleared.isEmpty())
assertTrue(port.sendBlocked.isEmpty())
```

Không đổi code `main`. Câu khẳng định cũ giữ nguyên.

**B. Mỗi bộ xử lý nhận `OnScreen` một lần trong một lần quyết định (ghi chú 2, câu hỏi 1, phương án (c)).** Chỗ lặp: trong `showDecided`, khi SDK đã gọi `onShowed` ngay trong lời gọi hiện, `turn.onScreen` là `true`, và `showAgain` giao `OnScreen` cho từng lần bấm nhập vào, kể cả khi hai lần bấm dùng chung một bộ xử lý. Sửa:

1. `showAgain` thêm tham số `giveOnScreen: Boolean = true`; chỉ gọi `onOutcome(RewardedOutcome.OnScreen)` khi `turn.onScreen && giveOnScreen`. Nơi gọi từ `request` (lần bấm đến khi lượt đã có) dùng mặc định, nên giữ như hôm nay.
2. `showDecided`: tập `val screened = mutableSetOf<(RewardedOutcome) -> Unit>()`; với mỗi lần bấm nhập vào: `val give = it.onOutcome !in screened`, gọi `showAgain(declaration, turn, it.activity, it.onOutcome, give)`, rồi `if (turn.onScreen && give) screened += it.onOutcome`. Giữ `firstJoinedToo` như hôm nay (bộ xử lý của lần bấm đầu đã nhận `OnScreen` trong `showAgain` thì không nhận lại khi giao `heldOutcomes`).
3. KDoc của `showDecided`: "otherwise every handler gets [RewardedOutcome.OnScreen] once for this decision, however many taps it joined with, and the latest caller the reward." KDoc của lớp, gạch đầu dòng về callback: "Every outcome is given at most once per turn and caller, so callbacks the SDK repeats for the second show run nothing twice (section 3.4); the one exception is [RewardedOutcome.OnScreen], given again at once to a tap that comes after the ad covered the screen, so its dialog closes (plan T-048)." KDoc của `showAgain` thêm: "[giveOnScreen] is false for a handler that already got [RewardedOutcome.OnScreen] in the same decision."

Thứ tự giao không đổi: lần bấm nhập vào khác bộ xử lý vẫn nhận `OnScreen` trong `showAgain`, trước bộ xử lý của lần bấm đầu, như hôm nay.

Phương án (a) không được chọn (câu hỏi 1): không bỏ `OnScreen` cho lần bấm đến sau khi lượt đã lên màn, không bỏ `firstJoinedToo`.

**C. KDoc của `decide` (ghi chú 4).** "and asks for no load" thành "and asks for no further load". Thêm câu của phần D.

**D. Hết giờ chờ khóa khi cổng đồng ý đóng (ghi chú 3, câu hỏi 2, phương án (b), anh Nhật đã duyệt đổi luật).** Trong `decide`:

1. Biến cục bộ `var switchRead = false`, gán `true` ngay sau khi `port.enabled(declaration)` trả `true` (trong khối `withTimeoutOrNull`).
2. Kết quả của khối đặt tên `waited`. Sau khối: `if (waited == null) logger.warn(TAG, "Rewarded $code decision not done in time")` (dời lên từ nhánh `NO_AD, null`; vẫn đúng một dòng mỗi lần hết giờ).
3. `val step = waited ?: if (!switchRead && !port.consentOpen()) DecisionStep.CONSENT_CLOSED else null`, với chú thích: "MC-05: a closed consent gate runs the action whether the switch is on or off, so a timeout at the switch needs no switch."
4. Nhánh `CONSENT_CLOSED` không đổi: log `granted: consent gate closed`, `endDecision(..., Granted)`. Nghĩa là: lần bấm mới nhất nhận `Granted`, lần bấm nhập vào khác bộ xử lý nhận `NoAd(false, false)` (như `doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity`); không ghi cơ hội nào (kể cả `busy`); không gọi lượt nạp; không đếm lần thử của chính sách (`noAdInARow` giữ nguyên).
5. Hết giờ sau khi đã đọc khóa (đang chờ lượt nạp) thì không đổi, dù cổng đóng trong lúc chờ: `not_ready` đã ghi, chính sách áp như T-047. Lý do: lúc đọc lý do, cổng còn mở, cơ hội đã ghi.
6. KDoc của `decide`: "(...) past it the decision goes on as one with no ad (R-03), with the state read at that moment, and asks for no further load; when the switch was still awaited and the consent gate is closed, it grants like a closed gate (MC-05, plan T-048)." KDoc của `request`, câu cuối: "(...) not done in time, it ends like one with no ad, or like a closed consent gate when the switch was still awaited and the gate is closed (plan T-048), and shows nothing, so the next tap is a new decision."
7. Tài liệu luật:
   - `docs/ops/ads-business-rules.md`, mục 11, dòng "Chờ của một lần quyết định rewarded": sau "không hiện gì sau lúc chờ;" thêm "hết giờ lúc còn chờ khóa mà cổng đồng ý đóng thì như cổng đóng (MC-05): hành động làm thẳng, không ghi cơ hội (T-048);". Cột cuối "Không có (T-047)" thành "Không có (T-047, T-048)".
   - Dòng R-06, nửa câu "lần quyết định quá 5 giây (mục 11) kết thúc như chưa có quảng cáo, lần bấm sau là lần quyết định mới (T-047)": sau "kết thúc như chưa có quảng cáo" thêm ", trừ khi hết giờ lúc còn chờ khóa mà cổng đồng ý đóng: hành động làm thẳng (MC-05, T-048)".
   - Mục 8.2, nửa câu "lần quyết định rewarded hết giờ chờ (mục 11) ghi `not_ready` (...) (T-047)": thêm "; hết giờ lúc còn chờ khóa mà cổng đồng ý đóng thì không ghi `not_ready` hay `busy` (MC-05, T-048)" trước dấu chấm phẩy kết thúc nửa câu.
   - `docs/ops/ads-guide.md`, gạch đầu dòng Rewarded: "quá thì coi như không có quảng cáo (T-047)" thành "quá thì coi như không có quảng cáo (T-047), trừ khi hết giờ lúc còn chờ khóa mà cổng đồng ý đóng: khi đó hành động làm thẳng như MC-05 (T-048)".

Mẫu có sẵn: `doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity`, `consentClosed_grantsAtOnce_noOpportunity` (đường cổng đóng hôm nay); `decisionStuckAtSwitch_*` của T-047 (hết giờ).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt` | `showAgain`: tham số `giveOnScreen`; `showDecided`: tập `screened`, không giao `OnScreen` hai lần cho một bộ xử lý trong một lần quyết định; `decide`: `switchRead`, `waited`, cổng đóng khi hết giờ ở khóa; KDoc của lớp, `showDecided`, `showAgain`, `decide`, `request` (mục 3.1 B, C, D) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | Hai câu khẳng định trong `loadRound_switchStuck_releasesAfterWait`; 10 test mới (mục 4). Không đổi bản giả |
| Sửa | `docs/ops/ads-business-rules.md` | Mục 11, dòng chờ rewarded; dòng R-06, nửa câu hết giờ; mục 8.2, nửa câu `not_ready` khi hết giờ (mục 3.1 D, bước 7) |
| Sửa | `docs/ops/ads-guide.md` | Gạch đầu dòng Rewarded: ngoại lệ cổng đồng ý đóng (mục 3.1 D, bước 7) |
| Sửa | `docs/features/ads/tickets/T-048-don-sau-ra-soat-t-047.md` | `status`, nhật ký theo phiên |
| Sửa | `docs/features/ads/plans/T-048-don-sau-ra-soat-t-047.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): `RewardedTurns` chỉ có `AdsController.kt` tạo và gọi; chữ ký không đổi. `RewardedAction.kt` dùng một bộ xử lý cho mọi lần mở hộp thoại (`RewardedAction.kt:120-123`); nhận `OnScreen` thì chỉ đóng hộp thoại khi đang `WATCHING`. Lần lặp mà phần B bỏ đến khi hộp thoại đã đóng (cùng lần quyết định), nên không đổi gì; lần lặp có ích khi lượt cũ còn treo được giữ (mục 3.5). `RewardedActionTest.kt` (Robolectric, controller thật) đi qua `RewardedTurns` thật; bản phác thảo đã qua toàn bộ test của `:core:ads` (mục 4.1). `grep -rn "ads-business-rules.md\|ads-guide.md" scripts/ .github/`: không có. Các test nhắc `ads-business-rules.md` chỉ trong chú thích, không mở file, và không chép dòng mục 11 hay câu 8.2 về rewarded, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` (mục 10), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` (danh sách và cách kiểm như plan T-047, mục 3.2). Không ảnh chuẩn nào đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi khóa Remote Config. Đổi hành vi rewarded ở hai chỗ hẹp: bộ xử lý đã nhận `OnScreen` trong cùng lần quyết định không nhận lại; hết giờ chờ khóa khi cổng đồng ý đóng thì làm như cổng đóng (câu hỏi 2, đã duyệt).
- Sự kiện phân tích: không thêm sự kiện. Phần D bớt `ad_opportunity` `not_ready` và `busy` ở đường hết giờ chờ khóa khi cổng đồng ý đóng, đúng câu "không ghi cơ hội" của MC-05.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code chỉ dùng lớp đã có; test dùng `CompletableDeferred`, `advanceTimeBy`, `runCurrent` như T-047.

### 3.4. Phương án dự phòng

Không có. Bản phác thảo của phần B (phương án (c)) và D đã qua toàn bộ test JVM của `:core:ads` (1 057 test, mục 4.1).

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py docs/features/ads/plans/T-048-don-sau-ra-soat-t-047.md` (2026-10-28): 279 test dùng tên của `RewardedTurns.kt`. Nhóm "Cần xem trước": 6 (chạy lại sau khi sửa plan); sáu test này (`NativeEligibleReportTest.fullScreenStep_belowMinHeight_noEligible` và năm test `NativeFlowStepLayoutTest.*`) chỉ trùng tên chung `show`, không liên quan. Nhóm "Nên xem": 125 test; phần liên quan thật là 37 test của `RewardedTurnsTest` (gọi thẳng `RewardedTurns`) và 10 test của `RewardedActionTest` mà script liệt kê (lớp có 36 test; 6 test `realController_*` đi qua `RewardedTurns` thật). Phần còn lại (`AdPreloaderTest`, `NativeAdCardScreenshotTest`, `NativeAdPool*`, `NativeAdSource*`, `BannerLoadsTest`...) chỉ trùng tên chung `request`, `loadIfNeeded`, `show` của lớp khác. Đã đọc các test của `RewardedTurnsTest` có câu khẳng định gần hai hành vi đổi; bản phác thảo qua toàn bộ test này không sửa (mục 4.1).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| SDK đồng bộ: một bộ xử lý nhập vào một lần quyết định từ nhiều lần bấm nhận `OnScreen` mỗi lần `showAgain` | Một lần cho mỗi bộ xử lý trong lần quyết định (mục 3.1 B) | Không test nào khẳng định lần lặp. Gần nhất: `syncShowedAndReward_joinedTap_latestGranted` (hai lần bấm, một bộ xử lý: `[OnScreen, Granted]`), `twoCallersSamePlacement_bothGetOnScreen_grantOnlyLatest`, `secondShow_duplicateCallbacks_grantOnce`, `switchedOffWhileTurnPending_secondTapShowsAgain_grantOnce` | Giữ cả bốn; đã qua với bản phác thảo |
| Bộ xử lý đã có trong lượt, bấm lần nữa khi quảng cáo đã lên màn, nhận `OnScreen` lần nữa ngay. Trường hợp có ích: lượt cũ còn `onScreen = true` vì mất `onAdDismissed` mà không có `onStuck` (lượt rewarded có `raisedPresence = false` khi quảng cáo toàn màn hình khác giữ cờ, nên `recoverStuckFullScreen`, `onAppActivityDestroyed` ở `AdsController.kt:719, 768` không gọi `onStuck`); người dùng mở lại hộp thoại, bấm "Watch ad": `OnScreen` lặp đóng hộp thoại ngay | Phương án (c): không đổi. Phương án (a): không giao `OnScreen`; SDK báo lỗi hiện trên quảng cáo đã dùng, `onShowFailed` cho lượt chạy tiếp (lượt đã `onScreen`), hộp thoại đứng ở `WATCHING` tới khi người dùng đóng. Cả hai không chạy hành động | Không có | Câu mới canh giữ: `tapAfterOnScreen_sameHandler_onScreenAgain` (mục 4), xanh hôm nay và với (c) |
| Bộ xử lý khác, bấm khi quảng cáo đã lên màn, nhận `OnScreen` ngay | Không đổi | Không test nào; test mới `tapAfterOnScreen_otherHandler_onScreenAtOnce` (mục 4) canh giữ | Câu mới, xanh cả hôm nay |
| Lần bấm sau vẫn gọi `showAgain`, ghi `busy` (R-06) | Không đổi | `secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `doubleTapWithAdAwaitingSwitch_showsAgainBusy_grantOnce` | Giữ |
| Hết giờ chờ khóa, cổng đồng ý đóng lúc hết giờ (đọc lúc đó, không phải lúc bấm): `not_ready`, `busy`, chính sách MC-05 đếm một lần | `Granted` cho lần bấm mới nhất, `NoAd(false, false)` cho lần bấm nhập vào, không cơ hội, không đếm (mục 3.1 D) | Không test nào: mọi test `decisionStuckAtSwitch_*`, `afterStuckDecision_nextTapShowsAd`, `stuckDuringDecision_decisionStillEndsAfterWait` để cổng mở (mặc định của bản giả) | Giữ cả các test đó (chúng là đối chứng cổng mở); câu mới ở mục 4 |
| Hết giờ khi chờ lượt nạp: `not_ready`, chính sách | Không đổi, kể cả khi cổng đóng trong lúc chờ | `decisionStuckAtLoad_endsAfterWait_policyOnce`, `decisionStuckAtSwitch_noAd_endsAfterWait` | Giữ; test canh giữ mới cho cổng đóng trong lúc chờ |
| Một dòng `warn` "not done in time" mỗi lần hết giờ | Không đổi (dời lên trước `when`) | `decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy`, `decisionStuckAtLoad_endsAfterWait_policyOnce` (`warnings("not done in time") == 1`) | Giữ |

## 4. Kiểm thử

Số đo trên code hiện tại (L-029), bằng test tạm không commit trên nhánh `claude/plan-T-048-don-rewarded` từ `8d9bc55` (lệnh ở mục 4.1):

- Lượt nạp hết giờ đọc khóa: `cleared = []`, `sendBlocked = []`, `sends = []`. Câu mới xanh. Đột biến `NOT_NEEDED` thành `SWITCHED_OFF`: câu mới đỏ `cleared [rw_get_item]`, `loadRound_switchStuck_releasesAfterWait` cũ vẫn xanh. Đột biến thành `GATE_CLOSED`: đỏ `sendBlocked [rw_get_item]`, test cũ vẫn xanh.
- SDK đồng bộ, một bộ xử lý ba lần bấm cùng chờ khóa: `[OnScreen, OnScreen, Granted]`, `showAgains` 2, cơ hội `shown`, `busy`, `busy`, `rewards` 1. Đỏ.
- SDK đồng bộ, `a` rồi `b` hai lần: `a = [OnScreen]`, `b = [OnScreen, OnScreen, Granted]`. Đỏ.
- SDK đồng bộ, `a`, `b`, `a`: `a = [OnScreen, Granted]`, `b = [OnScreen]`. Xanh.
- Không đồng bộ, `request()`, `fireShowed()`, `request()` cùng bộ xử lý, `fireReward()`: `[OnScreen, OnScreen, Granted]`, cơ hội `shown`, `busy`. Phương án (c) giữ (test canh giữ).
- Không đồng bộ, `a` rồi `fireShowed()` rồi `b`: `b` nhận `[OnScreen]` ngay; sau `fireReward()`: `a = [OnScreen]`, `b = [OnScreen, Granted]`. Xanh.
- Cổng đồng ý đóng, khóa treo, có quảng cáo, `a` rồi `b`, `advanceTimeBy(5_001)`: `a = [NoAd(false, false)]`, `b = [NoAd(false, false)]`, cơ hội `[(rw_get_item, not_ready, ready), (rw_get_item, busy, ready)]`, `loads = []`, `shows = []`, một dòng `not done in time`. Đỏ.
- Cổng đóng, khóa treo, `rw_export` (`allow_after_2`), hết giờ, rồi cổng mở, không có quảng cáo: `[NoAd(false, false), NoAd(false, true)]` (lần hết giờ được đếm). Đỏ.
- Lượt nạp treo, `rw_allow`, cổng đóng trong lúc chờ, hết giờ: `[NoAd(false, true)]`, cơ hội `[(rw_allow, not_ready, idle)]`. Xanh.
- Khóa treo, cổng mở lúc bấm, đóng trong lúc chờ, hết giờ: `[NoAd(false, false)]`, cơ hội `[(rw_get_item, not_ready, ready)]`. Đỏ.
- Khóa treo, cổng đóng lúc bấm, mở trong lúc chờ, hết giờ: `[NoAd(false, false)]`, cơ hội `[(rw_get_item, not_ready, ready)]`. Xanh.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-19 tiêu chí 4: lượt nạp hết giờ đọc khóa không gọi `clearLoading`, `sendBlocked` (ghi chú 1) | Unit test | `RewardedTurnsTest.loadRound_switchStuck_releasesAfterWait`, thêm hai câu của mục 3.1 A. Qua ngay (code đúng). **Đối chứng (L-055, L-108) ở bước 3:** tạm đổi `RewardedLoadStep.NOT_NEEDED` của nhánh hết giờ trong `loadIfNeeded` thành `SWITCHED_OFF`, chạy, thấy đỏ ở câu `cleared`; đổi thành `GATE_CLOSED`, thấy đỏ ở câu `sendBlocked`; trả về `NOT_NEEDED`. Ghi hai thông báo vào nhật ký. Đã đo cả hai (đầu mục này) |
| KB-19 tiêu chí 3: một bộ xử lý, ba lần bấm, SDK đồng bộ | Unit test | `RewardedTurnsTest.syncShowed_oneHandlerThreeTaps_onScreenOnce`: `port.syncShowedReward = true`, `port.ready["rw_get_item"] = true`, `val gate = CompletableDeferred<Boolean>()`, `port.switchGates += gate`; `repeat(3) { launch { request() } }`, `runCurrent()`; `gate.complete(true)`, `runCurrent()`: `outcomes == [OnScreen, Granted]`, `port.showAgains == [rw_get_item, rw_get_item]`, `port.opportunities.map { it.second } == [shown, busy, busy]` (R-06 giữ), `port.rewards == [rw_get_item]`. Hôm nay đỏ: `[OnScreen, OnScreen, Granted]` (đã đo) |
| KB-19 tiêu chí 3, hai bộ xử lý (L-068): bộ xử lý nhập vào hai lần | Unit test | `RewardedTurnsTest.syncShowed_handlerJoinsTwice_onScreenOnce`: như trên, `launch { request { a += it } }`, rồi hai `launch { request(to = hb) }` với cùng `val hb: (RewardedOutcome) -> Unit = { b += it }`: `a == [OnScreen]`, `b == [OnScreen, Granted]`. Hôm nay đỏ: `b = [OnScreen, OnScreen, Granted]` (đã đo) |
| KB-19 tiêu chí 3, bộ xử lý của lần bấm đầu nhập vào lần nữa (canh bước 2 của mục 3.1 B) | Unit test | `RewardedTurnsTest.syncShowed_firstHandlerJoinsAgain_onScreenOnce`: thứ tự `ha`, `{ b += it }`, `ha`: `a == [OnScreen, Granted]`, `b == [OnScreen]`. Canh giữ, xanh hôm nay và với bản phác thảo (c) (đã đo); đỏ nếu `firstJoinedToo` bị bỏ mà `showAgain` vẫn giao `OnScreen` cho bộ xử lý của lần bấm đầu |
| R-06, lượt cũ còn treo: bấm lần nữa khi quảng cáo đã lên màn vẫn đóng hộp thoại (mục 3.5) | Unit test | `RewardedTurnsTest.tapAfterOnScreen_sameHandler_onScreenAgain`: `ready = true`, `request()`, `port.fireShowed()`, `request()`: ngay lúc đó `outcomes == [OnScreen, OnScreen]`; `port.fireReward()`: `outcomes == [OnScreen, OnScreen, Granted]`, `port.showAgains == [rw_get_item]`, cơ hội `[(rw_get_item, shown, ready), (rw_get_item, busy, idle)]`. Canh giữ, xanh hôm nay và với bản phác thảo (c) (đã đo `[OnScreen, OnScreen, Granted]`) |
| R-06: bộ xử lý mới bấm khi quảng cáo đã lên màn vẫn đóng hộp thoại ngay (đối chứng của dòng trên) | Unit test | `RewardedTurnsTest.tapAfterOnScreen_otherHandler_onScreenAtOnce`: `request { a += it }`, `fireShowed()`, `request { b += it }`: ngay lúc đó `b == [OnScreen]`; `fireReward()`: `a == [OnScreen]`, `b == [OnScreen, Granted]`. Canh giữ, xanh hôm nay (đã đo); phương án (c) không chạm đường này (`showAgain` gọi từ `request` dùng mặc định) |
| KB-19 tiêu chí 2 và MC-05: hết giờ chờ khóa khi cổng đồng ý đóng (câu hỏi 2, đã duyệt) | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_consentClosed_grantsLikeClosedGate`: `port.consentOpen = false`, `ready = true`, `port.switchGates += CompletableDeferred<Boolean>()` (không xong); `launch { request { a += it } }`, `runCurrent()`, `launch { request { b += it } }`, `runCurrent()`; `advanceTimeBy(5_001)`, `runCurrent()`: `a == [NoAd(false, false)]`, `b == [Granted]`, `port.opportunities` rỗng, `port.loads` rỗng, `port.shows` rỗng, `warnings("not done in time") == 1`, một dòng log `Ads` bắt đầu bằng `Rewarded rw_get_item granted: consent gate closed`. Đối chứng cổng mở là `decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy` có sẵn. Hôm nay đỏ: `b = [NoAd(false, false)]`, cơ hội `not_ready`, `busy` (đã đo) |
| MC-05: lần hết giờ đó không đếm lần thử của chính sách | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_consentClosed_countsNoTry`: `consentOpen = false`, khóa treo, `launch { request(rwExport) }`, `runCurrent()`, `advanceTimeBy(5_001)`, `runCurrent()`; `consentOpen = true`, `request(rwExport)` (không có quảng cáo): `outcomes == [Granted, NoAd(false, false)]` (lần thử đầu của `allow_after_2`). Hôm nay đỏ: `[NoAd(false, false), NoAd(false, true)]` (đã đo) |
| R-03: hết giờ khi chờ lượt nạp giữ đường T-047 dù cổng đóng trong lúc chờ | Unit test | `RewardedTurnsTest.decisionStuckAtLoad_consentClosedMeanwhile_staysNoAd`: `port.loadGates += CompletableDeferred<Unit>()` (không xong), `launch { request(rwAllow) }`, `runCurrent()`, `port.consentOpen = false`, `advanceTimeBy(5_001)`, `runCurrent()`: `outcomes == [NoAd(false, true)]`, cơ hội `[(rw_allow, not_ready, idle)]`. Canh giữ, xanh hôm nay. **Đối chứng (L-108):** bản phác thảo bỏ điều kiện `!switchRead` thì đỏ `but was:<[Granted]>` (đã đo); phiên làm lại đột biến này ở bước 4 và ghi vào nhật ký |
| MC-05 và L-072: cổng đóng trong lúc chờ khóa (cổng đọc lúc hết giờ, không phải lúc bấm) | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_consentClosesMeanwhile_grants`: `ready = true`, cổng mở, `port.switchGates += CompletableDeferred<Boolean>()` (không xong), `launch { request() }`, `runCurrent()`, `port.consentOpen = false`, `advanceTimeBy(5_001)`, `runCurrent()`: `outcomes == [Granted]`, `port.opportunities` rỗng. Hôm nay đỏ: `[NoAd(false, false)]`, cơ hội `[(rw_get_item, not_ready, ready)]` (đã đo). **Đối chứng (L-108) ở bước 4:** đột biến `gateAtStart` (đọc `port.consentOpen()` trước `withTimeoutOrNull`, dùng giá trị đó khi hết giờ) làm test đỏ `but was:<[NoAd(network=false, granted=false)]>` (đã đo); ghi vào nhật ký |
| MC-05 và L-072: cổng mở trong lúc chờ khóa | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd`: `consentOpen = false`, `ready = true`, khóa treo, `launch { request() }`, `runCurrent()`, `port.consentOpen = true`, `advanceTimeBy(5_001)`, `runCurrent()`: `outcomes == [NoAd(false, false)]`, cơ hội `[(rw_get_item, not_ready, ready)]`. Canh giữ, xanh hôm nay và với bản phác thảo (đã đo). **Đối chứng (L-108) ở bước 4:** đột biến `gateAtStart` làm test đỏ `but was:<[Granted]>` (đã đo); ghi vào nhật ký |
| Hành vi cũ giữ nguyên | Unit test, Robolectric | Mọi test có sẵn của `RewardedTurnsTest`, `RewardedActionTest` và các test của `AdsController` qua không sửa (mục 3.5) |
| Luật mới đọc được | Kiểm tra thủ công (người: đọc luật) | Người duyệt đọc dòng mục 11, nửa câu R-06, nửa câu mục 8.2 của `ads-business-rules.md` và gạch đầu dòng Rewarded của `ads-guide.md` |

**Ngoại lệ viết test trước:** (1) câu mới của `loadRound_switchStuck_releasesAfterWait` qua ngay vì code đúng; bước đột biến ở dòng 1 thay cho lần đỏ trước. (2) Năm test canh giữ xanh cả hôm nay (`syncShowed_firstHandlerJoinsAgain_onScreenOnce`, `tapAfterOnScreen_sameHandler_onScreenAgain`, `tapAfterOnScreen_otherHandler_onScreenAtOnce`, `decisionStuckAtLoad_consentClosedMeanwhile_staysNoAd`, `decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd`); chúng chặn sửa quá tay, hai test cổng có đột biến đối chứng. (3) KDoc và tài liệu luật là chữ. Năm test còn lại đỏ trên code hiện tại, lý do đã đo ở đầu mục. Không có test emulator: cả hai đường chỉ dựng được bằng bản giả (SDK gọi lại, khóa treo 5 giây).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-28, worktree của nhánh `claude/plan-T-048-don-rewarded` từ nhánh chính `8d9bc55`.
  - 9 test tạm cuối `RewardedTurnsTest` trên code hiện tại (số đo ở đầu mục 4).
  - Đột biến tạm trong `loadIfNeeded`: `NOT_NEEDED` thành `SWITCHED_OFF`, rồi `GATE_CLOSED`; chạy test tạm và `loadRound_switchStuck_releasesAfterWait` cũ.
  - Bản phác thảo phương án (a) của phần B cùng phần D (không commit), chạy cả module với 8 test tạm đầu; rồi đột biến bỏ `!switchRead`.
  - Bản phác thảo chỉ có bước 1 của mục 3.1 B (giữ `firstJoinedToo`), chạy `RewardedTurnsTest` với test tạm thứ chín (bộ xử lý khác bấm sau `OnScreen`).
  - Sau rà soát plan: bản phác thảo phương án (c) của phần B cùng phần D, 6 test tạm (ba lần bấm một bộ xử lý, `a`/`b`/`b`, `a`/`b`/`a`, bấm lại sau `OnScreen`, cổng đóng trong lúc chờ khóa, cổng mở trong lúc chờ khóa), chạy cả module; rồi đột biến `gateAtStart`; rồi chạy 6 test tạm trên code hiện tại.
  - Đã gỡ mọi sửa tạm (`git checkout -- core/ads`), worktree sạch.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*RewardedTurnsTest.tmp*' -i` (đo), `./gradlew :core:ads:testDebugUnitTest --continue -i` (bản phác thảo); JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`. Không chạy `scripts/check.sh`: E = 0, không kỹ thuật hay thư viện mới (L-042 không áp dụng; ADR-0029).
- **Kết quả:**
  - Code hiện tại: 9 test tạm, 5 đỏ đúng câu khẳng định (`AssertionError`), 4 xanh (lượt nạp, `a`/`b`/`a`, lượt nạp treo khi cổng đóng, bộ xử lý khác sau `OnScreen`).
  - Đột biến lượt nạp: test tạm đỏ `cleared [rw_get_item]`, rồi `sendBlocked [rw_get_item]`; test cũ xanh cả hai lần. Xác nhận ghi chú 1.
  - Phương án (a) và D: `:core:ads:testDebugUnitTest` BUILD SUCCESSFUL, 1 059 test, 0 lỗi, gồm `RewardedActionTest` với controller thật và 8 test tạm đầu. Số đo: ba lần bấm một bộ xử lý `[OnScreen, Granted]`; `b = [OnScreen, Granted]`; không đồng bộ `[OnScreen, Granted]`; cổng đóng hết giờ `a = [NoAd(false, false)]`, `b = [Granted]`, cơ hội rỗng, một dòng `not done in time`; `rw_export` `[Granted, NoAd(false, false)]`.
  - Đột biến bỏ `!switchRead`: `decisionStuckAtLoad_consentClosedMeanwhile_staysNoAd` (tạm) đỏ `but was:<[Granted]>`.
  - Phương án (a), chỉ bước đầu: test tạm thứ chín xanh (`b` nhận `[OnScreen]` ngay); `syncShowedAndReward_joinedTap_latestGranted` đỏ `expected:<[OnScreen, Granted]> but was:<[Granted]>`. Hai bước của (a) phải đi cùng nhau (mục 3.1 B, đoạn phương án (a)).
  - Phương án (c) và D: `:core:ads:testDebugUnitTest` BUILD SUCCESSFUL, 1 057 test, 0 lỗi. Ba lần bấm một bộ xử lý `[OnScreen, Granted]`; `a`/`b`/`b`: `a = [OnScreen]`, `b = [OnScreen, Granted]`; `a`/`b`/`a`: `a = [OnScreen, Granted]`, `b = [OnScreen]`; bấm lại sau `OnScreen` `[OnScreen, OnScreen, Granted]` (giữ); cổng đóng trong lúc chờ `[Granted]`, cơ hội rỗng; cổng mở trong lúc chờ `[NoAd(false, false)]`, một `not_ready`.
  - Đột biến `gateAtStart`: cổng đóng trong lúc chờ đỏ `but was:<[NoAd(network=false, granted=false)]>`; cổng mở trong lúc chờ đỏ `but was:<[Granted]>`.
  - Code hiện tại, 6 test tạm: ba lần bấm một bộ xử lý đỏ `[OnScreen, OnScreen, Granted]`; `a`/`b`/`b` đỏ `b = [OnScreen, OnScreen, Granted]`; cổng đóng trong lúc chờ đỏ `[NoAd(false, false)]`; ba test còn lại xanh.
  - Không đổi giao diện nên không ghi lại ảnh chuẩn. Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi luồng rewarded ở hai đường hẹp, sửa dòng mục 11 của luật nghiệp vụ; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện; số liệu chỉ bớt ở đường cổng đóng. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code, một file test trong một module, ba câu tài liệu; chữ ký không đổi |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu từng điểm; hai lựa chọn chờ người (câu hỏi 1, 2), một là đổi luật |
| C. Khả năng kiểm chứng tự động | 0 | Mọi đường có test JVM với thời gian ảo; bản phác thảo qua cả module |
| D. Ảnh hưởng chéo | 0 | Không plan nào khác đang sửa `RewardedTurns.kt` (mục 6) |
| E. Độ mới của kỹ thuật | 0 | Lặp lại mẫu của T-042, T-047 |
| **Tổng** | 2 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RewardedTurns.kt`, `RewardedTurnsTest.kt` của `:core:ads`; `docs/ops/ads-business-rules.md` (mục 11, dòng R-06, mục 8.2); `docs/ops/ads-guide.md` (gạch đầu dòng Rewarded). `AdsController.kt`, `RewardedAction.kt` không sửa.
- `docs/STATUS.md`: không đưa vào PR.
- Task khác: hôm nay không plan `draft` hay `approved` nào khác sửa `RewardedTurns.kt`, `RewardedTurnsTest.kt` hay hai tài liệu trên (`grep -l "RewardedTurns\|ads-business-rules\|ads-guide" docs/features/*/plans/*.md` chỉ ra plan đã `done`). Nếu cùng danh sách chạy với một plan khác sửa hai tài liệu, `scripts/run-check.py` sẽ báo.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: độc lập. `depends_on` (T-047) đã merge.

## 7. Điều kiện dừng riêng

- Cách làm buộc phải đổi câu khẳng định của một test có sẵn về R-06, T-042 hay T-047 (`secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `twoCallersSamePlacement_*`, `doubleTap*`, `secondTapWhileLoading_joinsDecision`, `syncShowedAndReward_*`, `decisionStuck*`, `afterStuckDecision_nextTapShowsAd`): dừng, vì đó là luật đã chốt.
- Phần B làm một bộ xử lý mất `OnScreen` hẳn (một test có sẵn hay mới nhận `[Granted]` thiếu `OnScreen`) mà không sửa được bằng đúng các bước của mục 3.1 B: dừng, báo số đo.
- Cách làm buộc phải bỏ `OnScreen` cho lần bấm đến sau khi lượt đã lên màn (`tapAfterOnScreen_sameHandler_onScreenAgain` đỏ): dừng, vì đó là phương án (a), không được chọn (câu hỏi 1).
- Phần D: muốn cho làm hành động khi hết giờ **sau** khi đã đọc khóa, hay khi cổng mở: dừng, vì trái trả lời câu hỏi 1 của T-047.
- Phần D: muốn ghi cơ hội nào (kể cả `busy`) ở đường hết giờ khi cổng đóng: dừng, vì trái MC-05.

## 8. Câu hỏi cho người duyệt

1. **`OnScreen` lặp: sửa code hay sửa chữ (KDoc của `showDecided`)?** Rà soát T-047 thấy một bộ xử lý nhập vào một lần quyết định từ ba lần bấm trở lên nhận `OnScreen` mỗi lần `showAgain` khi SDK gọi đồng bộ. Chạy thử thấy thêm: bộ xử lý bấm lần nữa sau khi quảng cáo đã lên màn cũng nhận `OnScreen` lần nữa, cả khi SDK không đồng bộ (AdMob). `RewardedAction` dùng một bộ xử lý cho mọi lần mở hộp thoại, và `OnScreen` đóng hộp thoại nếu đang chờ. Lần lặp thứ hai có ích khi lượt cũ còn treo (mất `onAdDismissed`, không có `onStuck`; mục 3.5): nó đóng ngay hộp thoại mới mở. Phương án:
   - (a) Mỗi bộ xử lý một `OnScreen` mỗi lượt: `showAgain` bỏ `OnScreen` cho bộ xử lý đã có trong lượt, `showDecided` bỏ `firstJoinedToo`. **Cái giá:** khi lượt cũ còn treo, người dùng mở lại hộp thoại và bấm "Watch ad" thì hộp thoại đứng ở trạng thái chờ tới khi người dùng tự đóng; hôm nay hộp thoại đóng ngay. Hiếm, không chạy hành động, nhưng người dùng thấy kẹt.
   - (b) Không sửa code, chỉ sửa KDoc của `showDecided` và của lớp cho đúng cả hai lần lặp; bỏ hai test đỏ của phần B. **Cái giá:** lặp trong một lần quyết định vẫn còn; người gọi sau này đếm `OnScreen` sẽ sai.
   - (c) Chỉ bỏ `OnScreen` lặp trong cùng một lần quyết định (lần bấm nhập vào, trong `showDecided`), giữ `OnScreen` cho lần bấm đến sau khi lượt đã lên màn; KDoc của lớp ghi rõ ngoại lệ đó (mục 3.1 B). **Cái giá:** hợp đồng "mỗi kết quả một lần mỗi lượt mỗi người gọi" có một ngoại lệ ghi trong KDoc, người gọi đếm `OnScreen` phải biết; thêm một tham số cho `showAgain` và một tập trong `showDecided`.
   - **Đề xuất của AI:** (c). Nó sửa đúng chỗ rà soát T-047 nêu và đúng cách hiểu plan T-047 đã duyệt (mỗi bộ xử lý `OnScreen` một lần trong lần quyết định), mà không làm hộp thoại kẹt khi lượt cũ còn treo. Bản phác thảo qua cả module (1 057 test). Bản trước rà soát đề xuất (a); đổi vì rà soát chỉ ra trường hợp lượt treo.
   - **Trả lời (anh Nhật, 2026-10-28):** (c), chỉ bỏ `OnScreen` lặp trong cùng một lần quyết định, theo đề xuất.
2. **Hết giờ chờ khóa khi cổng đồng ý đóng: giữ hay đổi? Đổi là đổi luật nghiệp vụ, cần người duyệt.** Hôm nay lần quyết định ghi `not_ready` (và `busy` cho lần bấm nhập vào), áp chính sách MC-05 (mặc định chặn), đúng chữ plan T-047 và dòng mục 11. Trường hợp này chưa xảy ra trong app thật: hộp thoại chỉ mở khi cổng mở, và khóa trả ngay. Phương án: (a) giữ nguyên, ghi lý do vào KDoc của `decide` (mục 3.1, đoạn cuối phần D); (b) hết giờ lúc còn chờ khóa mà cổng đóng thì làm như cổng đóng: hành động làm thẳng (`Granted`), không ghi cơ hội, không đếm lần thử; sửa dòng mục 11, nửa câu mục 8.2 của `ads-business-rules.md` và câu của `ads-guide.md` (mục 3.1 D). Hết giờ khi đã đọc khóa giữ như T-047. **Đề xuất của AI:** (b). MC-05 nói cổng đóng thì khóa bật cũng như tắt, và khóa tắt thì hành động làm thẳng: dù khóa trả gì, kết quả đều là `Granted` không ghi cơ hội, nên không cần đợi khóa. (a) ghi `ad_opportunity` lúc cổng đóng, trái câu "không ghi cơ hội" của MC-05, và chặn hành động mà luật cho làm. (b) không trái trả lời câu hỏi 1 của T-047 (hết giờ coi như chưa có quảng cáo) ở đường cổng mở; chỉ thêm một ngoại lệ suy ra từ MC-05. Nếu anh chọn (a), AI sửa plan trước khi duyệt.
   - **Trả lời (anh Nhật, 2026-10-28):** đổi: hết giờ chờ khóa khi cổng đồng ý đóng thì làm thẳng hành động, không ghi cơ hội (MC-05); duyệt phần đổi luật ở R-06, mục 11 và 8.2.
3. **Điểm chặn.** Plan đổi hành vi rewarded và một dòng luật, nên giữ `blocking_points: [ads, core]` như T-046, T-047. Nếu anh chọn (b) ở câu 1 và (a) ở câu 2, plan chỉ còn test và KDoc, vẫn chạm `:core:ads` (điểm 7). **Đề xuất của AI:** giữ `[ads, core]`, mức Cao.
   - **Trả lời (anh Nhật, 2026-10-28):** giữ `[ads, core]`, theo đề xuất.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-28 trong phiên giám sát cùng AI, gồm phần đổi luật nghiệp vụ (hết giờ chờ khóa khi cổng đồng ý đóng thì làm thẳng; R-06, mục 11, mục 8.2 của `ads-business-rules.md`). Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua hai vòng rà soát độc lập, sửa theo điểm nên sửa.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261028-02, phiên bản plan 1

- **Kết quả:** đã merge (2026-10-28 08:15 UTC, commit `c9dfb2b` trên nhánh chính).
- **Nhánh, PR:** `claude/T-048-don-rewarded`, #242.
- **Đã làm:** mục 3.1 A tới D đúng chữ của plan: `giveOnScreen` của `showAgain`, tập `screened` trong `showDecided`; `switchRead`, `waited` và nhánh cổng đóng khi hết giờ ở khóa trong `decide`; KDoc của lớp, `showDecided`, `showAgain`, `decide`, `request`; ba chỗ của `ads-business-rules.md` (mục 11, R-06, mục 8.2) và gạch đầu dòng Rewarded của `ads-guide.md` (không thêm dòng).
- **Test viết trước:** commit `b3f136a` trước commit code. 10 test mới và 2 câu mới trong `loadRound_switchStuck_releasesAfterWait`. Trên code cũ: 5 test đỏ đúng lý do của mục 4 (`syncShowed_oneHandlerThreeTaps_onScreenOnce` và `syncShowed_handlerJoinsTwice_onScreenOnce` `[OnScreen, OnScreen, Granted]`; `decisionStuckAtSwitch_consentClosed_grantsLikeClosedGate` `b = [NoAd(false, false)]`; `decisionStuckAtSwitch_consentClosed_countsNoTry` `[NoAd(false, false), NoAd(false, true)]`; `decisionStuckAtSwitch_consentClosesMeanwhile_grants` `[NoAd(false, false)]`); 5 test canh giữ và 2 câu mới xanh như plan nói. Đối chứng (L-055, L-108): `NOT_NEEDED` thành `SWITCHED_OFF` đỏ ở câu `cleared` (dòng 897), thành `GATE_CLOSED` đỏ ở câu `sendBlocked` (dòng 898); bỏ `!switchRead` làm `decisionStuckAtLoad_consentClosedMeanwhile_staysNoAd` đỏ `but was:<[Granted]>`; đột biến `gateAtStart` làm `decisionStuckAtSwitch_consentClosesMeanwhile_grants` đỏ `but was:<[NoAd(network=false, granted=false)]>` và `decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd` đỏ `but was:<[Granted]>`. Test JVM mọi module trên khung test: chỉ 5 test trên đỏ.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú: (1) chú thích nhánh `CONSENT_CLOSED` của `decide` chỉ dẫn "Question 8 of the plan, option (a)", nay nhánh này cũng phục vụ đường hết giờ của T-048 (KDoc của `decide` đã nói); (2) chú thích nhánh `NO_AD, null` "Timed out while the switch was awaited" vẫn đúng (chỉ còn khi cổng mở) nhưng không nhắc ngoại lệ cổng đóng; (3) ticket và plan đi qua nhánh báo cáo.
- **Kết quả CI:** `scripts/check.sh` cục bộ QUA (1445 test JVM, 0 lỗi); `pr-check` xanh một vòng (lần chạy 40653652163).
- **Lệch so với plan:** Không có. Không có phần ngoài mục 3.2, không có câu khẳng định cũ viết lại.
- **Việc còn lại:** Không có.
- **Cần người xem:** người đọc luật mới (mục 11, R-06, mục 8.2 của `ads-business-rules.md`; Rewarded của `ads-guide.md`); hai ghi chú chú thích của rà soát.
