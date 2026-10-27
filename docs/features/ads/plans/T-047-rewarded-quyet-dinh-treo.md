---
ticket: T-047
title: "Rewarded: lượt quyết định treo không giữ mãi các lần bấm sau"
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 4.6M         # cỡ S, số gợi ý 4.6M (scripts/token-usage.py estimate, 2026-10-27); giữ nguyên: một file code, một file test, một dòng tài liệu luật
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-047: REWARDED: LƯỢT QUYẾT ĐỊNH TREO KHÔNG GIỮ MÃI CÁC LẦN BẤM SAU

## 1. Tóm tắt

Đã chạy thử trên code hiện tại (mục 4.1). Cả hai chỗ của ticket đều đỏ với bản giả, nhưng hôm nay không có đường nào trong app thật dẫn tới chúng:

- **Lượt quyết định treo:** lỗi có thật trong `RewardedTurns`. Lần bấm đầu treo ở `port.enabled()` hay ở lượt nạp thì mọi lần bấm sau của vị trí không bao giờ có kết quả, kể cả sau 10 phút và sau `onStuck()`. Trong app hôm nay, `port.enabled()` chỉ chờ một nhịp (Remote Config phát giá trị ngay lúc đăng ký) và lượt nạp của controller không treo. Plan thêm một thời gian chờ có giới hạn (5 giây, câu hỏi 1) cho hai chỗ chờ này. Hết giờ thì lần quyết định đi theo đường "không có quảng cáo": ghi `not_ready`, áp chính sách MC-05, lần bấm nhập vào ghi `busy`, không hiện gì sau lúc chờ. `onStuck()` không xóa `decisions` (câu hỏi 2).
- **`held` khi SDK gọi đồng bộ:** hành động vẫn chạy đúng một lần (test tạm xanh). Chỗ lệch: người gọi đầu mất `OnScreen`, và khi có lần bấm nhập vào thì thưởng trao cho người gọi đầu thay vì người gọi mới nhất (câu hỏi 9 của T-031). AdMob không gọi đồng bộ, nên người dùng không thấy. Plan sửa nhỏ: `held` giữ cả danh sách kết quả và giao lại theo đúng luật của lượt không đồng bộ (câu hỏi 3 cho người chọn chỉ giữ test).

## 2. Hiểu yêu cầu

Ticket xử lý KB-19 trong `features/ads/spec.md` (spec `confirmed`), luật MC-05, R-02, R-03, R-04, R-06 và mục 8.2 (`ad_opportunity`) của `docs/ops/ads-business-rules.md`. Ticket đến từ ghi chú vòng 3 của rà soát T-042 (RUN-20261026-05, plan T-042 mục 10).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | 4. Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí: mặc định bị chặn và người dùng thử lại được; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng. | Lần quyết định chờ khóa hay chờ lượt nạp quá 5 giây thì coi như quảng cáo chưa sẵn sàng: chính sách áp một lần, hộp thoại đóng, người dùng thử lại được. Lần bấm sau đó là một lần quyết định mới, có kết quả bình thường |
| KB-19 | 6. Khi bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì app làm như AdsKit (không chặn) và số liệu ghi lần bấm đó là "bận" (`busy`). | Giữ nguyên (R-06, T-042): lần bấm nhập vào lần quyết định hết giờ ghi `busy`, người gọi mới nhất nhận chính sách |
| KB-19 | 3. Khi người dùng xem đủ thì nhận thưởng ngay lúc quảng cáo báo thưởng. | SDK gọi `onShowed`, `onReward` ngay trong lời gọi hiện: thưởng trao đúng một lần, cho người gọi mới nhất (như lượt không đồng bộ), người gọi đầu vẫn nhận `OnScreen` |
| Ticket | Lần bấm sau một lượt treo vẫn có kết quả theo luật. | Có kết quả sau thời gian chờ có giới hạn. `onStuck()` chỉ chạy khi một quảng cáo rewarded đã nhận cờ "đang hiện", tức lượt đã có (`turns`); lần quyết định chưa hiện gì nên không bao giờ là nguyên nhân của `onStuck()` (câu hỏi 2) |

Thiết kế tham chiếu: Không có thiết kế riêng (chỉ logic, không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013, ADR-0026 (nghiệp vụ theo AdsKit PARTNER), ADR-0027 (số liệu quảng cáo), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

Đường dẫn tới một lần quyết định của `RewardedTurns.decide` và chỗ nó có thể chờ (L-072):

- (a) `port.enabled()`: hôm nay là `isEnabled(placement).first()` của `AdsController`. Với vị trí rewarded (không phải vị trí toàn app), luồng này là `combine` của hai khóa Remote Config; `FirebaseRemoteFlagSource` phát giá trị đã kích hoạt ngay lúc đăng ký (`onStart { emit(Unit) }`), nên chỉ chờ một nhịp của `Dispatchers.Main.immediate` (đo ở T-042).
- (b) `port.loadIfNeeded()`: hôm nay là `loadFullScreenAdIfNeeded`, hàm thường, không treo. Hợp đồng của cổng (`RewardedPort`) cho phép cả hai treo.
- (c) `port.show()`: không chờ; SDK gọi lại sau. Bản giả và một SDK mediation lạ có thể gọi đồng bộ.

Các bước:

1. **Chờ có giới hạn.** Hằng số `DECISION_WAIT_MILLIS = 5_000L` trong `companion object` của `RewardedTurns`. Bọc (a) và (b) bằng `withTimeoutOrNull(DECISION_WAIT_MILLIS)`:
   - (a) trả `false`: như hôm nay, hành động làm thẳng (`Granted`). Trả `true`: như hôm nay. Hết giờ (`null`): `logger.warn(TAG, "Rewarded <code> switch not read in time")`, rồi đi đường "không có quảng cáo" với `network = false`. Không đọc `ready`, không `take`, không hiện: sau 5 giây người dùng có thể đã rời màn, nên không hiện gì sau lúc chờ (L-091).
   - (b) hết giờ: `logger.warn(TAG, "Rewarded <code> load not decided in time")`, rồi đi tiếp như lượt nạp đã xong: đếm chính sách, ghi `busy` cho lần bấm nhập vào, kết thúc lần quyết định.
   - Tách khối "không có quảng cáo" của `decide` thành hàm `private suspend fun noAd(declaration, decision, policy, network)` để hai đường hết giờ và đường cũ dùng chung (ghi `not_ready` với `load_state` trước lượt nạp, lượt nạp có giới hạn, `countNoAd`, log `skipped`, `busy` cho lần bấm nhập vào, `endDecision`). Thứ tự ghi số liệu không đổi.
   - Lượt nạp bị hủy khi hết giờ thì `finally` của `loadIfNeeded` gỡ `deciding` như hôm nay.
2. **`held` giữ cả danh sách.** Trong nhánh hiện quảng cáo của `decide`: `held` thêm mỗi kết quả vào `heldOutcomes: MutableList<RewardedOutcome>` thay vì ghi đè một biến.
   - Lượt đã kết thúc ngay trong lời gọi (`turns[declaration] == null`): như hôm nay, kết quả giao cho `endDecision` là kết quả cuối của danh sách (`lastOrNull()`, mặc định `NoAd(false, false)`).
   - Lượt còn: thay `held` bằng `first.onOutcome` trong `turn.callers`, gọi `showAgain` cho từng lần bấm nhập vào (R-06) như hôm nay, rồi giao lại danh sách theo thứ tự: `OnScreen` cho `first.onOutcome`, `Granted` cho `turn.callers.last()` (người gọi mới nhất, câu hỏi 9 của T-031). Lần bấm nhập vào nhận `OnScreen` trong `showAgain` khi `turn.onScreen` đã `true`, như hôm nay.
3. **`onStuck()` không đổi** (câu hỏi 2). Thêm một câu vào KDoc của `onStuck`: lần quyết định đang chạy không bị gỡ, nó tự kết thúc sau tối đa `DECISION_WAIT_MILLIS`.
4. **Tài liệu luật** (câu hỏi 1): mục 11 của `docs/ops/ads-business-rules.md` thêm dòng "Chờ khóa và lượt nạp của một lần quyết định rewarded | 5 giây; hết giờ coi như chưa có quảng cáo (R-03), không hiện gì sau lúc chờ | Không có (T-047)". Dòng R-06 thêm nửa câu: "lần quyết định chờ quá 5 giây (mục 11) thì kết thúc như chưa có quảng cáo, lần bấm sau là lần quyết định mới".
5. KDoc của `request` và `decide`: nêu thời gian chờ.

Mẫu có sẵn: `deciding` và `try/finally` của `loadIfNeeded`; `Turn.callers` và `showAgain` của T-042.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt` | `DECISION_WAIT_MILLIS`; `withTimeoutOrNull` cho `port.enabled()` và `port.loadIfNeeded()` trong `decide`; hàm `noAd`; `heldOutcomes`; KDoc của `request`, `decide`, `onStuck` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | 7 test mới (mục 4); bản giả `FakeRewardedPort` thêm `syncShowedReward` và `syncDismiss` (trong `show`, gọi `turns.onShowed`, `turns.onReward`, rồi `turns.onDismissed` nếu bật), thuộc commit test của bước 3 (L-025) |
| Sửa | `docs/ops/ads-business-rules.md` | Mục 11: một dòng hằng số mới; dòng R-06: nửa câu về lần quyết định hết giờ (mục 3.1, bước 4) |
| Sửa | `docs/features/ads/tickets/T-047-rewarded-quyet-dinh-treo.md` | `status`, nhật ký theo phiên |
| Sửa | `docs/features/ads/plans/T-047-rewarded-quyet-dinh-treo.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): `RewardedTurns` chỉ có `AdsController.kt` tạo và gọi (`request`, `onShowed`, `onReward`, `onDismissed`, `onShowFailed`, `onStuck`, `loadIfNeeded`, `onLoadFailed`); chữ ký công khai không đổi. `RewardedActionTest.kt` (Robolectric, `realController()`) đi qua `RewardedTurns` thật; bản phác thảo đã qua toàn bộ test này (mục 4.1). `ads-business-rules.md`: không script hay workflow nào đọc; các test nhắc tới nó chỉ trong chú thích, không mở file, và chỉ nhắc MC-02, mục S, 4, 5.7, 8, 10: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` (mục 10, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Plan chỉ thêm một dòng mục 11 và sửa dòng R-06, không chạm phần các test chép tay, nên không test nào bị ảnh hưởng. Không ảnh chuẩn nào đổi: không đổi giao diện.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi hành vi của định dạng rewarded chỉ khi lần quyết định chờ quá 5 giây (hôm nay không xảy ra trong app thật) và khi SDK gọi đồng bộ. Không đổi khóa Remote Config.
- Sự kiện phân tích: không thêm sự kiện. Lần quyết định hết giờ ghi `ad_opportunity` `not_ready` (lần bấm đầu) và `busy` (lần bấm nhập vào), như đường "không có quảng cáo" hôm nay. Hôm nay lần quyết định treo không ghi gì.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `withTimeoutOrNull` thuộc `kotlinx.coroutines` (`kotlinx-coroutines-core`), đã có trên classpath biên dịch của `:core:ads` (`RewardedTurns.kt` đã dùng hàm `suspend`; bản phác thảo đã biên dịch, mục 4.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `withTimeoutOrNull` đặt một hẹn giờ trên `Dispatchers.Main.immediate` của `AdsController`. Bản phác thảo qua toàn bộ 1046 test JVM của `:core:ads`, gồm `RewardedActionTest` với controller thật trên Robolectric, nhưng chưa chạy `ui-test` trên emulator.
- **Điều kiện chuyển:** một test có sẵn dùng controller thật (Robolectric hay emulator) đỏ vì hẹn giờ còn treo, hay vì thứ tự chạy đổi, sau 2 lần sửa.
- **Cách làm thay:** không dùng hẹn giờ. `Deciding` ghi thời điểm bắt đầu (cổng thêm `fun nowMillis(): Long`, `AdsController` trả `clock.nowMillis()`); lần bấm tới khi lần quyết định của vị trí đã chạy quá `DECISION_WAIT_MILLIS` thì không nhập vào: lần quyết định cũ bị bỏ (các lần bấm đã nhập vào nhận `NoAd(false, false)` để hộp thoại đóng, ghi `busy`), lần bấm mới bắt đầu lần quyết định mới. Test viết trước giữ nguyên tiêu chí; test của mục 4 điều khiển đồng hồ giả thay cho `advanceTimeBy`. Tiêu chí 1 (lần bấm sau có kết quả) và tiêu chí không hiện gì sau lúc chờ giữ nguyên. Dùng phương án này thì sửa thêm `AdsController.kt` (dự phòng): một dòng `nowMillis` trong `RewardedPort` của controller.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | (dự phòng) `nowMillis()` của `RewardedPort` |

### 3.5. Hành vi đã có bị đổi

Kết quả `scripts/existing-tests.py docs/features/ads/plans/T-047-rewarded-quyet-dinh-treo.md` (2026-10-27): 269 test dùng tên của `RewardedTurns.kt`. Nhóm "Cần xem trước" có 7 test, đều của native (`NativeFlowStepLayoutTest`, `NativeFlowStepTest`, `NativeEligibleReportTest`), chỉ trùng tên chung `show`, `take` của lớp khác; không test nào đo vị trí hay kích thước của rewarded. Nhóm "Nên xem" có 186 test; phần liên quan thật là 42 test của `RewardedTurnsTest` (gọi thẳng `RewardedTurns`) và các test của `RewardedActionTest` (controller thật); phần còn lại (`AdPreloaderTest`, `BannerLoadsTest`, `AppOpenRulesTest`...) chỉ trùng tên chung `request`, `ready`, `show`. Đã đọc các test của `RewardedTurnsTest` có câu khẳng định gần các hành vi plan đổi; bản phác thảo qua toàn bộ 269 test này (mục 4.1).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lần quyết định chờ `port.enabled()` không giới hạn | Chờ tối đa 5 giây, hết giờ đi đường "không có quảng cáo" | Không test nào để cổng chờ quá 5 giây ảo: `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce`, `doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity`, `decisionsOfTwoPlacementsApart`, hàm dựng `doubleTapAwaitingSwitch` mở cổng bằng `runCurrent()`, không tiến thời gian | Giữ. Bản phác thảo qua các test này không sửa |
| Lần quyết định chờ lượt nạp không giới hạn | Chờ tối đa 5 giây, rồi đếm chính sách | `secondTapWhileLoading_joinsDecision` mở `loadGates` bằng `runCurrent()` | Giữ |
| SDK gọi `onShowFailed` trong lời gọi hiện: kết quả cuối là `NoAd` | Như cũ: kết quả cuối của danh sách `heldOutcomes` | `showFailsInsideTheCall_endsTurnOnce`, `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy` | Giữ |
| SDK gọi `onShowed`, `onReward` trong lời gọi hiện: người gọi đầu chỉ nhận `Granted`; có lần bấm nhập vào thì người gọi đầu nhận thưởng | Người gọi đầu nhận `OnScreen` rồi `Granted`; có lần bấm nhập vào thì người gọi mới nhất nhận thưởng | Không test nào: bản giả hôm nay không gọi đồng bộ | Test mới (mục 4) |
| `onStuck()` chỉ gỡ `turns` | Không đổi | `stuck_endsPendingTurnsWithoutOutcome` | Giữ |

## 4. Kiểm thử

Số đo trên code hiện tại (L-029), bằng test tạm không commit trong `RewardedTurnsTest`, nhánh `claude/plan-T-047-rewarded-treo` từ `6547211` (lệnh và kết quả ở mục 4.1):

- Treo ở khóa (`switchGates` một `CompletableDeferred` không bao giờ xong), lần bấm hai sau `onStuck()`, có quảng cáo: hai người gọi `[]` và `[]`, `shows = []`. Đỏ.
- Treo ở khóa, `advanceTimeBy(600_000)`, lần bấm hai: `[]`, `shows = []`. Đỏ.
- Treo ở lượt nạp (`loadGates` không xong), `rw_allow`, `onStuck()`, `advanceTimeBy(600_000)`, lần bấm hai: `[]`, cơ hội `[(rw_allow, not_ready, idle)]`. Đỏ.
- SDK gọi đồng bộ `onShowed`, `onReward`, một người gọi: `[Granted]`, `rewards = [rw_get_item]`; gọi lại `fireShowed`, `fireReward`, `fireDismissed`: vẫn một `Granted`. Xanh (đúng một lần), nhưng mất `OnScreen`.
- SDK gọi đồng bộ cả `onDismissed`: `[Granted]`; lần bấm sau hiện quảng cáo mới (`shows` 2). Xanh.
- SDK gọi đồng bộ, hai người gọi khác nhau cùng chờ khóa: `a = [Granted]`, `b = [OnScreen]`. Đỏ so với luật "thưởng cho người gọi mới nhất".

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Ticket, treo ở khóa: lần bấm nhập vào có kết quả sau thời gian chờ | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy`: `port.ready["rw_get_item"] = true`; `port.switchGates += CompletableDeferred<Boolean>()` (không bao giờ xong); `launch { request { a += it } }`, `runCurrent()`, `launch { request { b += it } }`, `runCurrent()`. `advanceTimeBy(4_999)`, `runCurrent()`: `a`, `b` rỗng. `advanceTimeBy(2)`, `runCurrent()`: `a == [NoAd(false, false)]`, `b == [NoAd(false, false)]` (`block`), `port.shows` rỗng (không hiện sau lúc chờ), cơ hội `[(rw_get_item, not_ready, ready), (rw_get_item, busy, ready)]`, log có một dòng `warn` chứa `switch not read in time`. Hôm nay đỏ: `[]`, `[]` (đã đo) |
| Ticket, lần bấm sau lượt treo là lần quyết định mới | Unit test | `RewardedTurnsTest.afterStuckDecision_nextTapShowsAd`: như trên tới hết giờ; rồi `request { c += it }` (hàng cổng đã rỗng, khóa bật): `port.shows == [rw_get_item]`, `port.showAgains` rỗng; `fireShowed()`, `fireReward()`: `c == [OnScreen, Granted]`. Đối chứng (L-055): `a`, `b` không nhận thêm gì. Hôm nay đỏ: `shows = []` (đã đo) |
| Ticket, treo ở lượt nạp, chính sách `allow` | Unit test | `RewardedTurnsTest.decisionStuckAtLoad_endsAfterWait_policyOnce`: `port.loadGates += CompletableDeferred<Unit>()` (không xong); hai `launch { request(rwAllow) { ... } }`, `runCurrent()`; `advanceTimeBy(5_001)`, `runCurrent()`: người gọi đầu `[NoAd(false, false)]`, người gọi sau `[NoAd(false, true)]` (chính sách một lần), cơ hội `[(rw_allow, not_ready, idle), (rw_allow, busy, idle)]`, log có `load not decided in time`. Đối chứng: một `request(rwAllow)` nữa ra `NoAd(false, true)` ngay (lượt nạp không còn treo). Hôm nay đỏ: không kết quả nào (đã đo) |
| Ticket, `onStuck()` không gỡ lần quyết định, lần quyết định vẫn hết giờ (câu hỏi 2) | Unit test | `RewardedTurnsTest.stuckDuringDecision_decisionStillEndsAfterWait`: treo ở khóa như dòng đầu; `turns.onStuck()`, `runCurrent()`: chưa có kết quả, `port.presenceLowered == 0`; `advanceTimeBy(5_001)`, `runCurrent()`: đúng một `NoAd(false, false)`. Hôm nay đỏ ở câu cuối: không kết quả (đã đo) |
| KB-19 tiêu chí 3, SDK gọi đồng bộ, một người gọi | Unit test | `RewardedTurnsTest.syncShowedAndReward_oneCaller_onScreenThenGrantOnce`: `port.syncShowedReward = true`, `ready = true`, `request()`: `outcomes == [OnScreen, Granted]`, `port.rewards == [rw_get_item]`; `fireShowed()`, `fireReward()`, `fireDismissed()`: không đổi. Hôm nay đỏ: `[Granted]` (đã đo) |
| KB-19 tiêu chí 3 và R-06, SDK gọi đồng bộ, lần bấm nhập vào (L-068) | Unit test | `RewardedTurnsTest.syncShowedAndReward_joinedTap_latestGranted`: `syncShowedReward = true`, `ready = true`, một `switchGates` mở sau khi hai `launch { request { a += it } }`, `launch { request { b += it } }` đã chờ: `a == [OnScreen]`, `b == [OnScreen, Granted]`, `port.rewards` một phần tử, `port.showAgains == [rw_get_item]`. Hôm nay đỏ: `a = [Granted]`, `b = [OnScreen]` (đã đo) |
| KB-19 tiêu chí 3, SDK gọi đồng bộ cả lúc đóng | Unit test | `RewardedTurnsTest.syncShowedRewardDismissed_grantOnce_nextTapShowsAgain`: `syncShowedReward = true`, `syncDismiss = true`, `ready = true`, `request()`: đúng một `Granted`; tắt `syncShowedReward`, `ready = true`, `request()`: `port.shows.size == 2`. Canh giữ, xanh cả hôm nay (đã đo) |
| Hành vi cũ giữ nguyên | Unit test, Robolectric | Mọi test có sẵn của `RewardedTurnsTest`, `RewardedActionTest` và các test của `AdsController` qua không sửa (mục 3.5) |
| Không còn câu hỏi về thời gian chờ trên máy | Kiểm tra thủ công (người: đọc luật) | Người duyệt đọc dòng mới của mục 11 và R-06 trong `ads-business-rules.md` |

**Ngoại lệ viết test trước:** `syncShowedRewardDismissed_grantOnce_nextTapShowsAgain` là test canh giữ, xanh cả trên code hiện tại (đã đo); giữ để cách làm mới của `held` không làm mất kết quả khi lượt kết thúc ngay trong lời gọi. Sáu test còn lại đỏ trên code hiện tại, lý do đã đo ở đầu mục này. Dòng của `ads-business-rules.md` là tài liệu, không có test. Không có test emulator: lỗi không xảy ra trong app thật hôm nay (mục 1), và chờ 5 giây chỉ điều khiển được trên JVM.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, nhánh `claude/plan-T-047-rewarded-treo` từ `6547211`. (1) 7 test tạm trong `RewardedTurnsTest`, bản giả thêm `syncShowedReward`, `syncDismiss`, trên code hiện tại: số đo ở đầu mục 4. (2) Bản phác thảo không commit của bước 1 và 2 ở mục 3.1 (`withTimeoutOrNull` 5 giây, hàm `noAd`, `heldOutcomes`) cùng các test tạm: treo ở khóa hết giờ thì lần bấm sau hiện quảng cáo (`shows = [rw_get_item]`); treo ở lượt nạp hết giờ thì lần bấm sau `[NoAd(false, true)]`; đồng bộ một người gọi `[OnScreen, Granted]`; đồng bộ có lần bấm nhập vào `a = [OnScreen]`, `b = [OnScreen, Granted]`, một thưởng. Test tạm "sau `onStuck()`, không tiến thời gian, lần bấm sau có kết quả ngay" vẫn đỏ, đúng ý câu hỏi 2. Đã gỡ bản phác thảo và test tạm, không commit.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*RewardedTurnsTest*'` (đo trên code hiện tại), `./gradlew :core:ads:testDebugUnitTest --continue` (bản phác thảo trong cây); JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`. Không chạy `scripts/check.sh`: E dưới 2, không có kỹ thuật hay thư viện mới (L-042 không áp dụng; ADR-0029).
- **Kết quả:** trên code hiện tại, 49 test của `RewardedTurnsTest` (42 có sẵn, 7 tạm), 5 đỏ: ba test treo, test đồng bộ mất `OnScreen`, test đồng bộ có lần bấm nhập vào. Với bản phác thảo, `:core:ads:testDebugUnitTest`: 1046 test, 1 đỏ (test tạm `onStuck` không tiến thời gian, cố ý), mọi test có sẵn qua, gồm `RewardedActionTest` với controller thật. Không đổi giao diện nên không ghi lại ảnh chuẩn. Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi luồng rewarded khi lần quyết định chờ quá lâu; thêm một hằng số vào luật nghiệp vụ; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện; số liệu chỉ thêm ở đường hôm nay không ghi gì. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code, một file test trong một module, một dòng và nửa câu tài liệu luật; chữ ký công khai không đổi |
| B. Độ rõ của yêu cầu | 2 | Luật chưa nói thời gian chờ, kết quả khi hết giờ và việc `onStuck` gỡ lần quyết định; AI đề xuất ở câu hỏi 1 đến 3 |
| C. Khả năng kiểm chứng tự động | 0 | Mọi đường có test JVM điều khiển thời gian ảo; bản phác thảo đã qua toàn bộ test của module |
| D. Ảnh hưởng chéo | 1 | T-046 sửa chú thích và đối chứng trong cùng `RewardedTurnsTest.kt` (mục 6) |
| E. Độ mới của kỹ thuật | 1 | `withTimeoutOrNull` lần đầu dùng trong `RewardedTurns`, nhưng thuộc `kotlinx.coroutines` đã dùng khắp dự án |
| **Tổng** | 5 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RewardedTurns.kt` của `:core:ads`; `docs/ops/ads-business-rules.md` (mục 11, dòng R-06). `AdsController.kt` không sửa (trừ phương án dự phòng).
- Task khác có thể xung đột: T-046 sửa `RewardedTurnsTest.kt` (đối chứng của `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy`, chú thích của `doubleTapAwaitingSwitch`). T-047 thêm test ở cuối lớp và hai trường vào bản giả, không sửa hai chỗ đó; chạy lần lượt thì merge không xung đột. Ticket T-046 ghi rõ không làm phần của T-047.
- Đổi `held` chạm đúng nhánh mà đối chứng của T-046 sửa (`doubleTap_showFailsInsideTheCall_*`); câu khẳng định của test đó giữ nguyên với cách làm mới (bản phác thảo đã qua).
- Đề xuất thứ tự chạy: T-047 trước T-046, để T-046 dọn chú thích sau khi `decide` đã ổn định; hai task độc lập về code.

## 7. Điều kiện dừng riêng

- Cách làm buộc phải đổi câu khẳng định của một test có sẵn về R-06 hay T-042 (`secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `twoCallersSamePlacement_*`, `doubleTap*`, `secondTapWhileLoading_joinsDecision`, `showFailsInsideTheCall_endsTurnOnce`): dừng, vì đó là luật đã chốt.
- Muốn hiện quảng cáo sau khi hết giờ chờ, hay cho làm hành động (`Granted`) khi hết giờ chờ khóa: dừng, vì trái câu hỏi 1 đã duyệt.
- Muốn cho `onStuck()` gỡ `decisions` khi người duyệt chọn phương án (a) của câu hỏi 2: dừng, vì trái câu hỏi 2.

## 8. Câu hỏi cho người duyệt

1. Thời gian chờ của một lần quyết định rewarded (chờ khóa `is_show_<mã>` và chờ lượt nạp) là bao lâu, và hết giờ thì làm gì? Luật hôm nay chưa nói; AdsKit không có bước chờ này. Phương án: (a) 5 giây, hết giờ coi như quảng cáo chưa sẵn sàng (R-03): ghi `not_ready`, chính sách MC-05, không hiện gì sau lúc chờ; (b) 5 giây, hết giờ coi như vị trí tắt: hành động làm thẳng; (c) không thêm thời gian chờ, chỉ ghi vào hợp đồng của `RewardedPort` rằng hai hàm này không được treo, và bỏ ba test treo. **Đề xuất của AI:** (a). Hộp thoại chỉ mở khi khóa đã bật, nên coi khóa là bật và quảng cáo chưa có là đúng nhất; (b) cho làm hành động không có quảng cáo khi khóa đang bật; (c) để lỗi sống lại khi lượt nạp thành `suspend` (T-042, câu hỏi 4). 5 giây đủ dài so với một nhịp chờ thật, đủ ngắn để người dùng không bỏ đi. Ghi hằng số vào mục 11 và nửa câu vào R-06 (đổi luật nghiệp vụ, cần người duyệt).
   - **Trả lời (anh Nhật, 2026-10-27):** (a), chờ 5 giây, hết giờ coi như quảng cáo chưa sẵn sàng; duyệt thêm hằng số ở mục 11 và nửa câu ở R-06 của luật.
2. `onStuck()` có gỡ lần quyết định đang chạy (`decisions`) không? Phương án: (a) có: mọi lần bấm đã nhập vào nhận `NoAd(false, false)`, hộp thoại đóng; (b) không: lần quyết định tự kết thúc sau thời gian chờ của câu hỏi 1. **Đề xuất của AI:** (b). `onStuck()` chỉ chạy khi một quảng cáo rewarded đã nhận cờ "đang hiện" (`recoverStuckFullScreen`, `onAppActivityDestroyed`), tức lượt đã có trong `turns`; lần quyết định chưa hiện gì nên không bao giờ gây ra `onStuck()`. Gỡ theo (a) chỉ cắt ngang lần quyết định của vị trí khác đang chạy bình thường, mất kết quả của nó.
   - **Trả lời (anh Nhật, 2026-10-27):** (b), `onStuck()` không gỡ lần quyết định, theo đề xuất.
3. Sửa `held` khi SDK gọi đồng bộ (mục 3.1, bước 2), hay chỉ thêm test canh giữ? Hành động đã chạy đúng một lần (đã đo); chỗ lệch là người gọi đầu mất `OnScreen` và thưởng trao cho người gọi đầu thay vì người gọi mới nhất khi có lần bấm nhập vào. AdMob không gọi đồng bộ, nên người dùng không thấy; `RewardedAction` dùng một bộ xử lý cho mọi lần bấm, nên chỉ hai hành động khác nhau của cùng vị trí mới thấy khác. Phương án: (a) sửa như bước 2 (vài dòng, bản phác thảo đã qua mọi test); (b) không sửa code, chỉ giữ `syncShowedRewardDismissed_grantOnce_nextTapShowsAgain` và một test canh giữ "một người gọi, một `Granted`"; bỏ `syncShowedAndReward_oneCaller_onScreenThenGrantOnce` và `syncShowedAndReward_joinedTap_latestGranted`. **Đề xuất của AI:** (a), vì sửa nhỏ và đưa đường đồng bộ về cùng luật với đường không đồng bộ (câu hỏi 9 của T-031), một SDK mediation lạ gọi đồng bộ cũng không đổi người nhận thưởng.
   - **Trả lời (anh Nhật, 2026-10-27):** (a), sửa `held`, theo đề xuất.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
