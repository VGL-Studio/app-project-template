---
ticket: T-042
title: "Rewarded: bấm \"Xem quảng cáo\" hai lần khi chưa có quảng cáo chỉ làm hành động một lần"
status: done                    # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, data, core)
risk_level: high             # low | medium | high
token_estimate: 5M           # cỡ S, số gợi ý 4.1M (scripts/token-usage.py estimate, 2026-10-26); nâng nhẹ vì hai file code, hai file test với 12 test mới và một vòng rà soát luật số liệu
blocking_points: [ads, data, core]
run_order: 2                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Nhật              # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-042: REWARDED: BẤM "XEM QUẢNG CÁO" HAI LẦN KHI CHƯA CÓ QUẢNG CÁO CHỈ LÀM HÀNH ĐỘNG MỘT LẦN

## 1. Tóm tắt

Lỗi có thật, đã đo trên code hiện tại (mục 4): với `AdsController` thật, bấm "Watch ad" hai lần trong cùng một khung hình khi chưa có quảng cáo thì `onGranted` chạy hai lần với `allow`, `allow_after_2` cho làm ngay ở lần mở hộp thoại đầu, `block` ghi hai cơ hội `not_ready` và hai lượt "Load if needed". Nguyên nhân: lần bấm đầu dừng ở `port.enabled()` (`combine(...).first()` treo một nhịp trên `Dispatchers.Main.immediate`), hộp thoại còn mở, lần bấm hai vào `RewardedTurns.request` khi chưa có lượt (`turns`) nào và tự đi hết đường "không có quảng cáo". Plan thêm vào `RewardedTurns` một "lần quyết định đang chạy" theo vị trí: lần bấm tới khi một lần quyết định của cùng vị trí chưa xong thì nhập vào lần đó, không chờ khóa, không đếm thêm lần thử; kết quả cuối giao một lần cho mỗi người gọi, người gọi mới nhất nhận chính sách. Lần quyết định ra "hiện quảng cáo" thì mỗi lần bấm nhập vào gọi hiện lại và ghi `busy` như hôm nay (R-06). `RewardedAction` thêm chốt thứ hai: một lần mở hộp thoại chạy `onGranted` nhiều nhất một lần. Kiểm chứng bằng 8 test JVM của `RewardedTurnsTest` và 4 test Robolectric của `RewardedActionTest` (3 test qua `AdsController` thật).

## 2. Hiểu yêu cầu

Ticket xử lý KB-19 trong `features/ads/spec.md` (spec `confirmed`), luật MC-05, R-02, R-03, R-06 và mục 8.2 (`ad_opportunity`) của `docs/ops/ads-business-rules.md`. Ticket đến từ điểm "nên sửa, không sửa" vòng 3 của rà soát T-031 (RUN-20261026-04, plan T-031 mục 10).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | 4. Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí: mặc định bị chặn và người dùng thử lại được; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng. | Chính sách áp một lần cho mỗi lần quyết định, không cho mỗi lần bấm: bấm "Watch ad" lần hai trước khi lần đầu có kết quả thì không chạy `onGranted` lần nữa (`allow`), không đếm thêm một lần thử của `allow_after_<n>`, không ghi thêm `not_ready`, không gọi "Load if needed" thêm. Bấm lại sau khi hộp thoại đã đóng (lần mở mới) vẫn là một lần thử mới, đếm như hôm nay |
| KB-19 | 6. Khi bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì app làm như AdsKit (không chặn) và số liệu ghi lần bấm đó là "bận" (`busy`). | Giữ nguyên (R-06): lần bấm hai vẫn tới `showRewarded` (`telemetry.eligible` mỗi lần bấm, T-033); lần quyết định ra "hiện quảng cáo" thì lần bấm hai gọi hiện lại (`port.showAgain`) và ghi `busy`, thưởng trao một lần (R-02). Lần quyết định ra "không có quảng cáo" thì lần bấm hai ghi `busy` thay cho `not_ready` thứ hai (câu hỏi 1) |
| KB-19 | 3. Khi người dùng xem đủ thì nhận thưởng ngay lúc quảng cáo báo thưởng. | Không đổi: `onReward` trao một lần cho người gọi mới nhất của lượt |
| Ticket | Mỗi lần mở hộp thoại cho làm hành động nhiều nhất một lần. | Chốt ở `RewardedAction`: từ lúc hộp thoại mở (`ASKING`) tới lần mở sau, `onGranted` chạy nhiều nhất một lần, dù bộ điều phối có giao hai kết quả cho làm |

Thiết kế tham chiếu: Không có thiết kế riêng (chỉ logic, không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013, ADR-0026 (nghiệp vụ theo AdsKit PARTNER), ADR-0027 (số liệu quảng cáo), ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

Mọi đường dẫn tới kết quả của một lần bấm "Watch ad" hôm nay (L-072), và chỗ lần bấm hai có thể chen vào:

| Đường | Chỗ treo của lần bấm đầu | Hôm nay, lần bấm hai | Sau task |
| --- | --- | --- | --- |
| a. Khóa tắt sau khi chờ | `port.enabled()` | tự chờ khóa, tự ra `Granted` (hai lần `onGranted`) | nhập vào, một kết quả |
| b. Cổng đồng ý đóng | `port.enabled()` | tự ra `Granted` (hai lần) | nhập vào, một kết quả |
| c. Không có quảng cáo, đang chờ khóa | `port.enabled()` | tự ra `NoAd` theo chính sách (đo được: hai lần với `allow`) | nhập vào, một lần đếm, `busy` |
| d. Không có quảng cáo, đang chờ "Load if needed" | `port.loadIfNeeded()` (`suspend` trong cổng) | như c | nhập vào, như c |
| e. Có quảng cáo, đang chờ khóa | `port.enabled()` | lấy quảng cáo, hiện; lần đầu gọi hiện lại, `busy` (R-06) | lần đầu hiện, lần hai gọi hiện lại, `busy` (cùng kết quả số liệu, thứ tự gọi hiện đổi) |
| f. Đã có lượt (`turns`) | không treo | `showAgain`, `busy` (R-06) | không đổi |
| g. Hiện lỗi, thưởng, đóng (callback SDK) | không treo | không đổi: mỗi lượt giao mỗi kết quả một lần (T-031) | không đổi |

Các bước:

1. `RewardedTurns.kt`:
   - Thêm `private class Deciding` (danh sách lần bấm nhập vào, mỗi lần giữ `onOutcome` và `activity`) và `private val decisions = mutableMapOf<AdPlacementDeclaration, Deciding>()`, theo vị trí như `turns` (L-068: hai vị trí không trộn).
   - `request`: giữ nguyên đoạn đặt lại bộ đếm khi đổi chính sách và nhánh R-06 (`turns[declaration]` có thì `showAgain`). Tiếp đó, `decisions[declaration]` có thì thêm lần bấm vào `followers`, ghi log `Rewarded <mã> tap joins the one being decided`, trả về ngay (không chờ khóa). Không có thì tạo `Deciding`, đặt vào `decisions`, chạy phần quyết định hôm nay (tách thành `private suspend fun decide(...)`), `finally` gỡ `decisions[declaration]` nếu chưa gỡ (lần quyết định bị hủy thì lần bấm nhập vào không nhận gì, như `onStuck`).
   - Khối "xét lại `turns` sau khi chờ khóa" bỏ đi: trong lúc chờ không lần bấm nào khác của cùng vị trí tạo được lượt, vì mọi lần bấm đó đều nhập vào. Test `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce` (L-061) vẫn qua (đã chạy thử, mục 4.1) nhưng vì lý do khác: lần bấm hai nhập vào lần quyết định, không còn đọc khóa, nên cổng `false` thứ hai không bao giờ được dùng và trường hợp L-061 (lần bấm hai đọc khóa tắt) không còn xảy ra. Phiên chỉ sửa chú thích của test này cho đúng lý do mới (ADR-0029), không đổi câu khẳng định.
   - Lần bấm nhập vào mang chính sách khác (màn xem thử đổi chính sách giữa chừng): đoạn đầu của `request` vẫn chạy cho lần bấm đó, nên `lastPolicy` và `noAdInARow` có thể được đặt lại trước khi nó nhập vào; kết quả vẫn theo chính sách của lần bấm đầu (`decide` đã nhận `policy` của nó), và `countNoAd` của lần quyết định đếm trên bộ đếm lúc đó. Chính sách của lần bấm đầu thắng; chỉ xảy ra ở staging (`noAdPolicyOverride`).
   - Hàm riêng `endDecision(...)` gỡ `decisions[declaration]` trước rồi mới giao kết quả (người gọi có thể gọi lại ngay trong callback). Danh sách người gọi: người bấm đầu rồi các lần nhập vào, bỏ trùng theo `onOutcome`, mới nhất ở cuối (như `Turn.callers`). Người gọi mới nhất nhận kết quả của lần quyết định; người gọi khác nhận `NoAd(network = <cờ của lần quyết định>, granted = false)` để hộp thoại của họ đóng, như `onShowFailed` hôm nay.
   - Kết quả "khóa tắt" và "cổng đồng ý đóng" (đường a, b): `Granted` cho người gọi mới nhất; không ghi cơ hội cho lần bấm nhập vào (MC-05: không ghi cơ hội khi cổng đóng hay vị trí tắt).
   - Kết quả "không có quảng cáo" (đường c, d): giữ thứ tự hôm nay của người bấm đầu (`not_ready` với `load_state` trước lượt nạp, `loadIfNeeded`, `countNoAd` một lần, log `skipped`), rồi ghi `busy` cho mỗi lần bấm nhập vào với `port.loadState()` lúc đó, rồi giao kết quả. Hàm `noAd` cũ gộp vào nhánh này.
   - Kết quả "hiện quảng cáo" (đường e): `show(...)` như hôm nay tạo lượt cho người bấm đầu, gỡ `decisions[declaration]`, rồi gọi `showAgain(declaration, turn, tap.activity, tap.onOutcome)` cho từng lần bấm nhập vào theo thứ tự: mỗi lần gọi hiện lại và ghi `busy` (R-06), y như lần bấm hai tới sau khi lượt đã có.
   - KDoc của lớp và của `request` ghi luật mới: "một lần quyết định cho mọi lần bấm của vị trí tới khi có kết quả".
2. `RewardedAction.kt`:
   - Thêm `private var grantedThisOpening = false`, đặt lại `false` ở chỗ hộp thoại mở (`phase.value = ASKING` trong `start()`). Hàm riêng `grantOnce()` chạy `callbacks.value.onGranted()` khi cờ còn `false` rồi đặt `true`. `onOutcome` gọi `grantOnce()` thay cho `onGranted()` ở `Granted` và `NoAd(granted = true)`. Đường khóa tắt hay không có bộ điều khiển của `start()` (không có hộp thoại) giữ nguyên, không qua cờ.
   - Cờ không lưu qua dựng lại activity: sau dựng lại, `RewardedAction` mới có cờ `false`, và kết quả của lượt trước vẫn tới bộ xử lý cũ của controller (như hôm nay, `recreateAfterWatchTapped_resultOnce`).
   - KDoc của `rememberRewardedAction`: "[onGranted] runs at most once per opening of the dialog" thay cho "at most once per tap".
3. `docs/ops/ads-business-rules.md`, mục 8.2 (câu "rewarded bấm lần hai khi quảng cáo chưa đóng là `busy`"): thêm "kể cả khi không có quảng cáo: lần bấm nhập vào lần quyết định đang chạy ghi `busy`, `load_state` đọc sau lượt nạp, thường là `loading`". Dòng R-06: thêm một câu "Lần bấm thứ hai khi lần bấm đầu chưa có kết quả (chưa biết có quảng cáo hay không) nhập vào lần đầu: chính sách MC-05 áp một lần, lần bấm hai ghi `busy` (T-042)" (câu hỏi 1, 2).

Mẫu có sẵn làm theo: `Turn.callers` và `onShowFailed` của `RewardedTurns` (giao cho người gọi mới nhất, người khác bị chặn); `deciding` của `loadIfNeeded` (một lần quyết định cho mọi lời gọi chồng nhau).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt` | `Deciding`, `decisions`, `decide`, `endDecision`; `request` cho lần bấm nhập vào lần quyết định đang chạy; bỏ khối xét lại sau khi chờ khóa; gộp `noAd`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedAction.kt` | `grantedThisOpening`, `grantOnce()`; KDoc của `rememberRewardedAction` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | 8 test mới (mục 4); bản giả `FakeRewardedPort` thêm `loadGates` (giống `switchGates`, cho `loadIfNeeded` chờ), thuộc commit test của bước 3 (L-025) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | 4 test mới (mục 4), dùng `realController()`, `setRealContent(...)` và `Subject()` sẵn có |
| Sửa | `docs/ops/ads-business-rules.md` | Dòng R-06: luật lần bấm hai khi chưa có kết quả (bước 3); mục 8.2, câu "rewarded bấm lần hai khi quảng cáo chưa đóng là `busy`": thêm trường hợp không có quảng cáo, `load_state` của lần bấm nhập vào đọc sau lượt nạp, thường là `loading` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` (test có sẵn) | Chỉ chú thích của `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce` (mục 3.1, bước 1), không đổi câu khẳng định |
| Sửa | `docs/features/ads/tickets/T-042-rewarded-bam-dup-thuong-mot-lan.md` | `status`, nhật ký theo phiên |
| Sửa | `docs/features/ads/plans/T-042-rewarded-bam-dup-thuong-mot-lan.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): `grep -rn "RewardedTurns\|rememberRewardedAction\|RewardedOutcome" --include='*.kt'` cho `AdsController.kt` (tạo `RewardedTurns`, gọi `request`, `onShowed`, `onReward`, `onDismissed`, `onShowFailed`, `onStuck`, `loadIfNeeded`, `onLoadFailed`; chữ ký không đổi), `AdPlacement.kt` (chỉ KDoc), `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` (gọi `rememberRewardedAction`, chữ ký không đổi), `FullScreenAdsTest.kt`, `FullScreenPreloadSlotTest.kt` (đường nạp rewarded, không đi qua `request`). Không file nào trong `scripts/`, `.github/`, `build-logic/` đọc các file trên. `ads-business-rules.md`: không script hay workflow nào đọc; các test nhắc tới nó chỉ trong chú thích, không mở file: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` (mục 10, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Plan chỉ sửa dòng R-06, không chạm MC-02, mục 8 hay mục 10 mà các test chép tay, nên không test nào bị ảnh hưởng. Không ảnh chuẩn nào đổi: không đổi giao diện.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; đổi hành vi của định dạng rewarded khi bấm hai lần trước khi có kết quả (đường a đến e của mục 3.1). Không đổi khóa Remote Config.
- Sự kiện phân tích: không thêm sự kiện; `ad_opportunity` của vị trí rewarded ghi `busy` thay cho `not_ready` thứ hai ở lần bấm hai khi không có quảng cáo (câu hỏi 1). `ad_eligible` của T-033 vẫn ghi mỗi lần bấm. `reward_dialog`, `ad_reward_earned` không đổi.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code mới chỉ dùng lớp Kotlin chuẩn và `kotlinx.coroutines` đã có trên classpath biên dịch của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Bản phác thảo của cách làm đã chạy qua toàn bộ test JVM trong lúc lập plan (mục 4.1); không còn điểm kỹ thuật chưa chắc.

## 4. Kiểm thử

Số đo trên code hiện tại (L-029), bằng test tạm không commit, nhánh `claude/plan-T-042` từ `6077815`:

- `AdsController` thật (`realController()`, cổng đồng ý mở, không SDK nên không có quảng cáo), trong một `runOnUiThread { action.watch(); action.watch() }`: sau lần `watch()` đầu `dialogVisible` vẫn `true` (lần bấm đầu treo ở `port.enabled()`); `allow`: `granted = 2`; `allow_after_2`: `granted = 1` ngay ở lần mở hộp thoại đầu; `block`: `granted = 0`, hộp thoại đóng, log có 2 dòng `skipped`.
- `RewardedTurns` với bản giả, hai lần `request` cùng chờ `switchGates` rồi cả hai mở: `allow`: `[NoAd(false, true), NoAd(false, true)]`, cơ hội `[(rw_allow, not_ready, idle), (rw_allow, not_ready, loading)]`; `allow_after_2`: `[NoAd(false, false), NoAd(false, true)]`.
- Cũng với bản giả: khóa tắt khi cả hai lần bấm đang chờ khóa: `[Granted, Granted]`, không cơ hội; hai bộ xử lý khác nhau với `allow`: cả hai `NoAd(false, true)`; lần bấm hai khi lần đầu chờ ở `loadIfNeeded` (bản giả thêm `loadGates`): lần hai ra `NoAd(false, true)` ngay, `loads == [rw_allow, rw_allow]`, rồi lần đầu thêm một kết quả nữa, hai `not_ready`; có quảng cáo, hai lần bấm cùng chờ khóa: `[OnScreen, Granted]`, `shows` 1, `showAgains` 1, cơ hội `[(shown, ready), (busy, idle)]` (đã đúng R-06); `rw_allow` đang chờ khóa và `request(rwExport)`: mỗi vị trí một kết quả, một `not_ready` (đã đúng).
- `RewardedAction` với `start` giả, hai lần bấm "Watch ad" cùng một bộ xử lý (`handlers[0] === handlers[1]`), giao `NoAd(granted = true)` hai lần: `granted = 2`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-19 tiêu chí 4, `allow`, đường c | Unit test | `RewardedTurnsTest.doubleTapNoAd_allow_grantsOnce_secondTapBusy`: hai `switchGates` cùng chờ, `launch { request(rwAllow) }` hai lần, `runCurrent()`, mở cả hai cổng `true`, `runCurrent()`. Kết quả `[NoAd(network = false, granted = true)]`; cơ hội `[(rw_allow, not_ready, idle), (rw_allow, busy, loading)]`; `port.loads == [rw_allow]`; `port.switchGates.size == 1` sau khi có kết quả (lần bấm hai nhập vào, không tự chờ khóa). Hôm nay đỏ: hai kết quả, hai `not_ready` (đã đo) |
| KB-19 tiêu chí 4, `allow_after_<n>` | Unit test | `RewardedTurnsTest.doubleTapNoAd_allowAfter2_countsOneTry`: như trên với `rwExport` (`allow_after_2`): `[NoAd(false, false)]`, `port.switchGates.size == 1`; xóa hàng cổng (`port.switchGates.clear()`), rồi một `request(rwExport)` nữa (lần mở sau): kết quả thứ hai `granted = true`. Đối chứng (L-055): lần thử thứ hai liên tiếp vẫn được cho làm, nên test không qua nhờ bộ đếm bị bỏ. Hôm nay đỏ: `[false, true]` ngay ở hai lần bấm (đã đo) |
| KB-19 tiêu chí 4, `block` | Unit test | `RewardedTurnsTest.doubleTapNoAd_block_oneOutcome_userMayTryAgain`: như trên với `rwGetItem`: đúng một `NoAd(false, false)`, một `not_ready`, một `busy`, `port.switchGates.size == 1`; đặt `ready = true` rồi `request()`: quảng cáo hiện (`port.shows == [rw_get_item]`), người dùng thử lại được |
| KB-19 tiêu chí 4, đường d | Unit test | `RewardedTurnsTest.secondTapWhileLoading_joinsDecision`: bản giả thêm `loadGates`; lần đầu qua khóa, ghi `not_ready`, chờ ở `loadIfNeeded`; `launch { request(rwAllow) }` lần hai, `runCurrent()`: chưa có kết quả, `port.loads` vẫn một phần tử; mở `loadGates`: đúng một `NoAd(false, true)`, cơ hội `[not_ready, busy]`. Hôm nay đỏ: lần hai ra kết quả ngay, hai lượt nạp, rồi hai kết quả, hai `not_ready` (đã đo) |
| KB-19 tiêu chí 2, đường a | Unit test | `RewardedTurnsTest.doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity`: hai `switchGates`, mở cổng đầu `false`: kết quả `[Granted]` một lần, `port.switchGates.size == 1`, `port.opportunities` rỗng, `port.shows` rỗng |
| Hai người gọi chồng nhau (L-068) | Unit test | `RewardedTurnsTest.twoCallersOverlapNoAd_allow_onlyLatestGranted`: `request(rwAllow) { a += it }` và `request(rwAllow) { b += it }` cùng chờ khóa: `a == [NoAd(false, false)]`, `b == [NoAd(false, true)]`. Hôm nay đỏ: cả hai `granted = true` (đã đo) |
| KB-19 tiêu chí 6 (R-06) giữ nguyên, đường e | Unit test | `RewardedTurnsTest.doubleTapWithAdAwaitingSwitch_showsAgainBusy_grantOnce`: `ready = true`, hai lần bấm cùng chờ khóa, mở: `port.shows == [rw_get_item]`, `port.showAgains == [rw_get_item]`, cơ hội `[(rw_get_item, shown, ready), (rw_get_item, busy, idle)]`; `fireShowed()`, `fireReward()`: `[OnScreen, Granted]`. Test canh giữ hành vi, qua cả hôm nay (ngoại lệ dưới) |
| Hai vị trí không trộn (L-068) | Unit test | `RewardedTurnsTest.decisionsOfTwoPlacementsApart`: `rwAllow` đang chờ khóa, `request(rwExport)` không nhập vào lần quyết định của `rw_allow`: mỗi vị trí một kết quả (`rw_export` `granted = false`, `rw_allow` `granted = true`), mỗi vị trí một `not_ready`, không `busy`. Test canh giữ, qua cả hôm nay (đã đo) |
| KB-19 tiêu chí 4, `allow`, controller thật | Robolectric | `RewardedActionTest.realController_allowDoubleWatchBeforeOutcome_runsActionOnce`: `setRealContent(controller, override = Allow)`, `start()`, `runOnUiThread { action!!.watch(); action!!.watch() }`, `waitForIdle()`: `granted == 1`, `cards() == 0`, log có đúng một dòng bắt đầu `Rewarded gallery_rw_action skipped:` và đúng một dòng chứa `tap joins` (để test không qua chỉ nhờ chốt `grantOnce`). Hôm nay đỏ: `granted = 2` (đã đo) |
| KB-19 tiêu chí 4, `allow_after_2`, controller thật | Robolectric | `RewardedActionTest.realController_allowAfter2DoubleWatch_countsOneTry`: như trên với `AllowAfter(2)`: `granted == 0`; `start()` lần nữa, một `watch()`: `granted == 1`. Hôm nay đỏ: `granted = 1` sau lần mở đầu (đã đo) |
| KB-19 tiêu chí 4, `block`, controller thật | Robolectric | `RewardedActionTest.realController_blockDoubleWatch_skipsOnce`: không ghi đè: `granted == 0`, `cards() == 0`, log có đúng một dòng bắt đầu `Rewarded gallery_rw_action skipped:`. Hôm nay đỏ: hai dòng (đã đo) |
| Một lần mở hộp thoại, một lần làm | Robolectric | `RewardedActionTest.sameOpening_secondGrant_ignored_nextOpeningRunsAgain`: `Subject()`, `start()`, hai lần `tapWatch()`, giao `NoAd(granted = true)` cho `handlers[0]` rồi `handlers[1]`: `granted == 1`; đối chứng (L-055): `start()`, `tapWatch()`, giao `Granted`: `granted == 2`. Hôm nay đỏ: `granted = 2` ngay sau lần mở đầu (đã đo) |
| Hành vi cũ giữ nguyên | Unit test, Robolectric | Mọi test có sẵn của `RewardedTurnsTest` (27 test của T-031 và các test của T-036) và `RewardedActionTest` qua không sửa, nhất là `secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce`, `notReady_allowAfter2_grantsOnSecondTryInARow_thenCountsAgain` (lần bấm tuần tự vẫn đếm từng lần), `secondWatchTap_callsStartAgain`, `backAfterWatch_closesDialog_resultStillOnce`, `recreateAfterWatchTapped_resultOnce` |
| Bấm đúp thật trên máy với quảng cáo thật, `allow` và `block` | Kiểm tra thủ công (máy thật: quảng cáo thật) | Trên bản staging, màn xem thử, trang Rewarded: tắt mạng sau khi mở app (không có quảng cáo), chọn `allow`, mở hộp thoại, bấm nhanh "Watch ad" hai lần: bộ đếm hành động của trang tăng 1. Bật mạng, chờ quảng cáo nạp, bấm nhanh hai lần: một quảng cáo, thưởng một lần |

**Ngoại lệ viết test trước:** hai test canh giữ qua cả trên code hiện tại (đã đo): `doubleTapWithAdAwaitingSwitch_showsAgainBusy_grantOnce` (R-06, kết quả số liệu của đường e không đổi, chỉ thứ tự gọi hiện đổi) và `decisionsOfTwoPlacementsApart` (L-068); giữ để cách làm mới không làm mất lần gọi hiện lại, `busy`, hay trộn hai vị trí. Mười test còn lại đỏ trên code hiện tại, lý do đã đo ở đầu mục này. Dòng R-06 của `ads-business-rules.md` là tài liệu, không có test.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, nhánh `claude/plan-T-042` từ `6077815`. (1) Test tạm trên code hiện tại để đo lỗi (số đo ở đầu mục 4). (2) Bản phác thảo không commit của bước 1 và 2 ở mục 3.1 (`decisions`, `decide`, `endDecision`, `grantOnce`) cùng các test tạm tương ứng: `allow` controller thật `granted = 1`, hộp thoại đóng; `allow_after_2` controller thật `0` rồi `1` ở lần mở sau; `block` controller thật một `skipped`, một dòng nhập vào; bản giả `allow` `[NoAd(false, true)]`, cơ hội `[not_ready idle, busy loading]`, một lượt nạp; bản giả có quảng cáo `[OnScreen, Granted]`, `shows` 1, `showAgains` 1, cơ hội `[shown ready, busy idle]`. Mọi test có sẵn của `:core:ads` qua với bản phác thảo. Đã gỡ bản phác thảo và test tạm, không commit.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*tmpProbe*'` (đo lỗi), `./gradlew :core:ads:testDebugUnitTest` và `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue` (bản phác thảo trong cây; JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`). Không chạy `scripts/check.sh`: E dưới 2, không có kỹ thuật hay thư viện mới (L-042 không áp dụng).
- **Kết quả:** trên code hiện tại, 12 test tạm cho đúng các số đo ghi ở đầu mục 4 (BUILD FAILED vì 3 test tạm có câu khẳng định đỏ đúng lý do: hai kết quả `granted`). Với bản phác thảo trong cây, `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue`: BUILD SUCCESSFUL, 1371 test JVM (gồm 7 test tạm), 0 lỗi; không test có sẵn nào đỏ, nên mục 3.2 không có test cũ phải cập nhật dữ liệu. Không đổi giao diện nên không ghi lại ảnh chuẩn nào. Không thêm thư viện hay module nên không chạy `:app:assembleStagingRelease` (L-083).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi hành vi của định dạng rewarded khi bấm hai lần; điểm 1), thu thập dữ liệu (`ad_opportunity` ghi `busy` thay cho `not_ready` thứ hai; điểm 4), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì số `not_ready` của rewarded giảm khi người dùng bấm đúp.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai file code, hai file test trong một module, không đổi giao diện công khai (mọi lớp, hàm mới là `private`) |
| B. Độ rõ của yêu cầu | 1 | Ticket và R-06 rõ; AI tự quyết số liệu của lần bấm hai khi không có quảng cáo (`busy`) và cách giao cho hai người gọi khác nhau, ghi ở câu hỏi 1, 3 |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường a đến g có test JVM hay Robolectric, ba test qua `AdsController` thật; bấm đúp với SDK thật chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 1 | Không file nào của plan bị T-041, T-043, T-044 sửa; `RewardedTurns` chỉ có `AdsController` dùng, chữ ký không đổi |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu `Turn.callers` và `deciding` có sẵn trong `RewardedTurns` |
| **Tổng** | 5 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RewardedTurns.kt`, `RewardedAction.kt` của `:core:ads`; `docs/ops/ads-business-rules.md` (dòng R-06 và một câu mục 8.2). Màn xem thử (`AdGalleryScreen.kt`, trang Rewarded) dùng `rememberRewardedAction`, không phải sửa.
- Task khác trong sprint có thể xung đột: T-043 sửa `ResumeAdTurns` và test của nó; T-044 sửa test của inter, native và `AdGalleryScreenTest` (ticket loại trừ test rewarded bấm đúp); T-041 sửa `NativeFlowStep` và trang Flow steps của `AdGalleryScreen.kt`. Không file nào trùng với mục 3.2. Nếu plan của T-043 hay T-041 thêm dòng vào `docs/ops/ads-business-rules.md`, hai plan sửa hai dòng khác nhau của cùng file; chạy theo thứ tự thì không xung đột.
- `decide` ném ngoại lệ (ví dụ cổng lỗi): `finally` gỡ lần quyết định, các lần bấm nhập vào không nhận kết quả, giống một lần bấm đơn hôm nay ném ngoại lệ; không phải hồi quy.
- Đề xuất thứ tự chạy: theo thứ tự đã định T-043, T-042, T-044, T-041; T-042 không phụ thuộc T-043. Chạy sau mọi task của T-031, T-036 (đã merge).

## 7. Điều kiện dừng riêng

- Cách làm buộc phải đổi điều một test có sẵn của R-06 kiểm (`secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `secondShow_duplicateCallbacks_grantOnce`, `twoCallersSamePlacement_*`, `twoTapsAwaitingSwitch_*`): dừng, vì R-06 là luật đã chốt.
- Muốn chặn lần bấm hai ở `RewardedAction.watch()` (không gọi `start` nữa): dừng, vì trái R-06 và tiêu chí 6 của KB-19.

## 8. Câu hỏi cho người duyệt

1. Lần bấm hai nhập vào một lần quyết định ra "không có quảng cáo" thì `ad_opportunity` ghi gì? Hôm nay ghi `not_ready` lần hai (kèm một lượt "Load if needed" thừa). Phương án: (a) ghi `busy` với `load_state` lúc có kết quả; (b) không ghi gì; (c) ghi `not_ready` như hôm nay. **Đề xuất của AI:** (a), vì mục 8.2 ghi "mỗi lượt gọi hiện đúng một kết quả" và R-06 gọi lần bấm hai trước khi có kết quả là `busy`; số `not_ready` khi đó đếm lần quyết định, không đếm lần bấm. Trả lời: (a), lần bấm nhập vào ghi `busy` (Nhật, 2026-10-26).
2. Ghi luật mới vào dòng R-06 của `docs/ops/ads-business-rules.md` (mục 3.1, bước 3), hay để ở KDoc của `RewardedTurns` thôi? **Đề xuất của AI:** ghi vào R-06, vì đây là hành vi nghiệp vụ mà app clone sẽ đọc ở tài liệu luật, và luật hôm nay chỉ nói trường hợp có quảng cáo. Trả lời: ghi vào dòng R-06 (Nhật, 2026-10-26).
3. Hai hành động khác nhau của cùng vị trí (hai `rememberRewardedAction` cùng `placement`, ví dụ hai màn) bấm chồng nhau khi chưa có quảng cáo: plan cho người bấm sau cùng nhận chính sách, người kia nhận `NoAd` bị chặn (hộp thoại đóng, không làm), như `onShowFailed` hôm nay. Phương án khác: mỗi hành động một kết quả riêng theo chính sách (tách theo bộ xử lý, không theo vị trí). **Đề xuất của AI:** giữ như plan, cùng luật với lượt có quảng cáo (`onReward` chỉ trao cho người gọi mới nhất); trường hợp này hầu như không xảy ra vì mỗi lúc chỉ một hộp thoại trên màn. Trả lời: giữ như plan, người gọi mới nhất nhận chính sách (Nhật, 2026-10-26).
4. Lỗi chỉ xảy ra khi `port.enabled()` treo, tức với `combine(...).first()` của khóa Remote Config trên `Dispatchers.Main.immediate` (đã đo trên Robolectric). Có cần thêm test ở mức `AdsController` cho đường d (treo ở "Load if needed") không? Hôm nay `loadFullScreenAdIfNeeded` của controller không treo, nên đường d chỉ có ở bản giả. **Đề xuất của AI:** không, test bản giả của đường d đủ; đường đó sẽ thật khi lượt nạp trở thành `suspend`. Trả lời: không thêm test controller thật cho đường d (Nhật, 2026-10-26).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-26 trong phiên giám sát cùng AI. Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua rà soát độc lập, sửa theo điểm chặn và điểm nên sửa. Chạy thứ 2 trong RUN-20261026-05. 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-05, phiên bản plan 1

- **Kết quả:** đã merge. PR #207, commit đầu `8c0de19`, trên nhánh chính 2026-10-26 17:00 UTC.
- **Nhánh, PR:** `claude/T-042-rewarded-bam-dup` (từ nhánh chính `d852310`). Commit test `0943fa4`, code `3ff4208`; sửa theo rà soát: test `78036ee`, code `482d32b`, test `179d66c`, code `8c0de19`. Bước 3 đến 5 do phiên con làm trong worktree `../task-T-042` (L-052); phần sửa theo rà soát do phiên chính làm.
- **Đã làm:** mọi phần của mục 3.1: "lần quyết định đang chạy" theo vị trí trong `RewardedTurns` (`Tap`, `Deciding`, `decide`, `endDecision`), bỏ khối xét lại sau khi chờ khóa; chốt `grantOnce` theo lần mở hộp thoại ở `RewardedAction`; dòng R-06 và câu `busy` mục 8.2 của `ads-business-rules.md`; sửa chú thích của `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce` (không đổi câu khẳng định).
- **Test viết trước:** 12 test mới (8 `RewardedTurnsTest`, 4 `RewardedActionTest`); 10 đỏ đúng lý do trên code cũ (hai kết quả, hai `not_ready`, `granted = 2`, hai dòng `skipped`). Qua ngay như plan nêu (test canh giữ): `doubleTapWithAdAwaitingSwitch_showsAgainBusy_grantOnce`, `decisionsOfTwoPlacementsApart`. Viết chặt bằng đối chứng: 2 (`doubleTapNoAd_allowAfter2_countsOneTry`, `sameOpening_secondGrant_ignored_nextOpeningRunsAgain`). Test JVM mọi module với test viết trước: chỉ 10 test dự kiến đỏ. Thêm 2 test theo rà soát, cả hai đỏ trước khi sửa.
- **Rà soát độc lập:**
  - Vòng 1: 0 chặn, 1 nên sửa, 3 ghi chú. Nên sửa (đã sửa): hiện quảng cáo báo lỗi ngay trong lời gọi (`showRewardedAd` khi không có quảng cáo hay activity) làm `turns.getValue` ném `NoSuchElementException` trong scope không bắt lỗi, app dừng; test `showFailsInsideTheCall_endsTurnOnce`.
  - Vòng 2: 0 chặn, 1 nên sửa (đã sửa): cùng đường đó, người bấm đầu nhận kết quả theo chính sách thay vì người gọi mới nhất, lần bấm nhập vào không ghi `busy`; test `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy`.
  - Vòng 3: 0 chặn, 1 nên sửa không sửa (hết 2 vòng rà soát lại, auto-run.md bước 6): phần đối chứng của test vòng 2 đi qua nhánh R-06 (`turns[declaration]`) thay vì nhánh nhập vào, vì cổng thừa của lượt nạp còn trong hàng; cách sửa: `port.switchGates.clear()` trước lần gọi thứ hai, kiểm thêm `showAgains`.
  - Ghi chú: chú thích của cổng thừa trong `doubleTapAwaitingSwitch` chưa chính xác và gắn với việc lượt nạp đọc khóa đúng một lần; lần quyết định treo mãi ở `port.enabled()` giữ mọi lần bấm sau ở trạng thái nhập vào và `onStuck` không gỡ `decisions` (không phải hồi quy); `held` chỉ giữ kết quả cuối nếu SDK gọi đồng bộ cả `onShowed` và `onReward` (AdMob không làm vậy); KDoc của `request` chưa nêu đường lỗi ngay trong lời gọi.
- **Kết quả CI:** `pr-check` xanh lần đầu (lần chạy 40362279248). Trên máy: `scripts/check.sh` 1380 test JVM, 0 lỗi, 82 ảnh, Cổng chất lượng: QUA.
- **Lệch so với plan:** hàm dựng `doubleTapAwaitingSwitch` thêm một cổng `CompletableDeferred(on)` cho lượt nạp của lần bấm đầu (`rewardedLoadStep` cũng gọi `port.enabled()`), sửa trong commit code `3ff4208`; câu khẳng định `switchGates.size == 1` giữ nguyên, 6 test vẫn đỏ đúng lý do trên code cũ. Sửa theo rà soát thêm nhánh "hiện lỗi ngay trong lời gọi" trong `decide` (lambda tạm `held`).
- **Ngoài mục 3.2 (ADR-0029):** không có.
- **Ảnh chuẩn:** không có ảnh mới hay đổi.
- **Việc còn lại:** điểm nên sửa vòng 3 (đối chứng của test) để người quyết.
- **Cần người xem:** điểm nên sửa vòng 3; cách giao kết quả khi hiện quảng cáo lỗi ngay trong lời gọi.
