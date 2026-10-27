---
ticket: T-047
title: "Rewarded: lượt quyết định treo không giữ mãi các lần bấm sau"
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 4.6M         # cỡ S, số gợi ý 4.6M (scripts/token-usage.py estimate, 2026-10-27); giữ nguyên: một file code, hai file test, ba chỗ tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-28
---

# PLAN T-047: REWARDED: LƯỢT QUYẾT ĐỊNH TREO KHÔNG GIỮ MÃI CÁC LẦN BẤM SAU

Phiên bản 2 (2026-10-27): sửa theo rà soát độc lập của bản 1 và theo trả lời mục 8 của anh Nhật. Đổi chính: một hạn chung 5 giây cho cả lần quyết định; lượt nạp cũng đọc khóa có giới hạn; test app ra nền giữa lúc chờ (L-091); phương án dự phòng giữ nguyên test viết trước; T-046 chạy trước.

Sửa trong bản 2 theo rà soát vòng 2 (2026-10-27, chưa duyệt nên giữ số phiên bản): test `decisionStuckAtSwitch_noAd_endsAfterWait` viết lại để phân biệt hạn chung với hai hạn riêng; test L-091 hết hạn khi app đang ở nền; hết hạn đọc cờ mạng (không chờ) để báo kiểm tra kết nối đúng KB-19; dòng mục 11 nói rõ lượt nạp hết giờ không hẹn thử lại; số đo mới ở mục 4 và 4.1.

## 1. Tóm tắt

Đã chạy thử trên code hiện tại (mục 4.1). Cả hai chỗ của ticket đều đỏ với bản giả, nhưng hôm nay không có đường nào trong app thật dẫn tới chúng:

- **Lượt quyết định treo:** lỗi có thật trong `RewardedTurns`. Lần bấm đầu treo ở `port.enabled()` hay ở lượt nạp thì mọi lần bấm sau của vị trí không bao giờ có kết quả, kể cả sau 10 phút và sau `onStuck()`. Lượt nạp treo ở lần đọc khóa thì giữ `deciding` mãi, mọi lượt nạp sau của vị trí không gửi gì. Trong app hôm nay, `port.enabled()` chỉ chờ một nhịp (Remote Config phát giá trị ngay lúc đăng ký). Plan thêm một hạn chung 5 giây cho mọi bước chờ của một lần quyết định (trả lời câu hỏi 1), và cùng hạn đó cho lần đọc khóa của lượt nạp. Hết hạn thì lần quyết định đi đường "không có quảng cáo": ghi `not_ready`, áp chính sách MC-05, lần bấm nhập vào ghi `busy`, không hiện gì sau lúc chờ. `onStuck()` không xóa `decisions` (trả lời câu hỏi 2).
- **`held` khi SDK gọi đồng bộ:** hành động vẫn chạy đúng một lần (test tạm xanh). Chỗ lệch: người gọi đầu mất `OnScreen`, khi có lần bấm nhập vào thì thưởng trao cho người gọi đầu thay vì người gọi mới nhất, và một bộ xử lý dùng cho hai lần bấm nhận `[Granted, OnScreen]` (sai thứ tự). AdMob không gọi đồng bộ, nên người dùng không thấy. Plan sửa nhỏ: `held` giữ cả danh sách kết quả và giao lại theo đúng luật của lượt không đồng bộ (trả lời câu hỏi 3).

## 2. Hiểu yêu cầu

Ticket xử lý KB-19 trong `features/ads/spec.md` (spec `confirmed`), luật MC-05, R-02, R-03, R-04, R-06 và mục 8.2 (`ad_opportunity`) của `docs/ops/ads-business-rules.md`. Ticket đến từ ghi chú vòng 3 của rà soát T-042 (RUN-20261026-05, plan T-042 mục 10).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | 4. Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí: mặc định bị chặn và người dùng thử lại được; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng. | Lần quyết định chưa xong sau 5 giây (chờ khóa, chờ lượt nạp, cộng lại) thì coi như quảng cáo chưa sẵn sàng: chính sách áp một lần, hộp thoại đóng, người dùng thử lại được. Lần bấm sau đó là một lần quyết định mới, có kết quả bình thường |
| KB-19 | 6. Khi bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì app làm như AdsKit (không chặn) và số liệu ghi lần bấm đó là "bận" (`busy`). | Giữ nguyên (R-06, T-042): lần bấm nhập vào lần quyết định hết hạn ghi `busy`, người gọi mới nhất nhận chính sách |
| KB-19 | 3. Khi người dùng xem đủ thì nhận thưởng ngay lúc quảng cáo báo thưởng. | SDK gọi `onShowed`, `onReward` ngay trong lời gọi hiện: thưởng trao đúng một lần, cho người gọi mới nhất (như lượt không đồng bộ); mỗi bộ xử lý nhận `OnScreen` một lần, trước `Granted` |
| Ticket | Lần bấm sau một lượt treo vẫn có kết quả theo luật. | Có kết quả sau tối đa 5 giây. `onStuck()` chỉ chạy khi một quảng cáo rewarded đã nhận cờ "đang hiện", tức lượt đã có (`turns`); lần quyết định chưa hiện gì nên không bao giờ là nguyên nhân của `onStuck()` (câu hỏi 2) |

Thiết kế tham chiếu: Không có thiết kế riêng (chỉ logic, không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013, ADR-0026 (nghiệp vụ theo AdsKit PARTNER), ADR-0027 (số liệu quảng cáo), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

Chỗ chờ của rewarded và đường dẫn tới chúng (L-072):

- (a) `decide` gọi `port.enabled()`: hôm nay là `isEnabled(placement).first()` của `AdsController`. Với vị trí rewarded (không phải vị trí toàn app), luồng này là `combine` của hai khóa Remote Config; `FirebaseRemoteFlagSource` phát giá trị đã kích hoạt ngay lúc đăng ký (`onStart { emit(Unit) }`), nên chỉ chờ một nhịp của `Dispatchers.Main.immediate` (đo ở T-042).
- (b) `decide` gọi `port.loadIfNeeded()` ở đường "không có quảng cáo": hôm nay là `loadFullScreenAdIfNeeded` của controller, hàm thường. Nó không chờ: lượt nạp thật chạy trong `scope.launch { rewardedTurns.loadIfNeeded(...) }` (`AdsController.kt`, `preloadNow`). Vì vậy ở app thật, hạn của `decide` không hủy được lượt nạp; chỉ bản giả (`loadGates`) mới làm `decide` chờ ở đây.
- (c) `RewardedTurns.loadIfNeeded` đọc khóa (`port.enabled()` trong `rewardedLoadStep`) không giới hạn, trong lúc giữ vị trí trong `deciding`. Mọi đường nạp đi qua đây: nạp sẵn, thử lại (F-05), nạp lại (F-06), "nạp nếu cần" (F-07). Treo ở đây thì mọi lượt nạp sau của vị trí không gửi gì.
- (d) `port.show()`: không chờ; SDK gọi lại sau. Bản giả và một SDK mediation lạ có thể gọi đồng bộ.

Các bước:

1. **Một hạn chung cho lần quyết định.** Hằng số `DECISION_WAIT_MILLIS = 5_000L` trong `companion object` của `RewardedTurns`. Trong `decide`, bọc toàn bộ phần có thể chờ bằng một `withTimeoutOrNull(DECISION_WAIT_MILLIS)`: đọc khóa (a), rồi nếu không có quảng cáo thì ghi `not_ready` và gọi lượt nạp (b). Lượt nạp vì vậy chỉ chờ phần còn lại của 5 giây; tổng thời gian chờ không bao giờ quá 5 giây. Khối trả về bước tiếp theo (enum riêng `private`: tắt, cổng đồng ý đóng, không có quảng cáo, hiện). Biến cục bộ ghi đã ghi `not_ready` chưa và cờ lỗi mạng.
   - Khóa trả `false`: như hôm nay, hành động làm thẳng (`Granted`). Cổng đồng ý đóng: như hôm nay.
   - Hết hạn (`null`), ở bất kỳ bước nào: `logger.warn(TAG, "Rewarded <code> decision not done in time")`, rồi đi đường "không có quảng cáo". Cờ mạng: nếu đã qua bước đọc lý do thì dùng cờ đã đọc; nếu hết hạn khi còn chờ khóa thì đọc ngay lúc đó `port.offline() || lastLoadNetworkError[declaration] == true` (hai lần đọc không chờ), để lỗi mạng vẫn báo "kiểm tra kết nối" (KB-19 tiêu chí 4, R-03). Ghi `not_ready` nếu chưa ghi, với `load_state` đọc lúc hết hạn; không gọi lượt nạp nữa (lượt nạp sẵn, thử lại vẫn chạy theo lịch riêng). Không đọc `ready`, không `take`, không hiện.
   - Đường "không có quảng cáo" sau hạn: như hôm nay, đếm chính sách (`countNoAd`), log `skipped`, `busy` cho mỗi lần bấm nhập vào, `endDecision`. Thứ tự ghi số liệu không đổi.
   - `take()` nằm trong khối nhưng không chờ, nên không thể hết hạn sau khi đã lấy quảng cáo ra.
2. **Lượt nạp đọc khóa có giới hạn** (c). Trong `loadIfNeeded`: `withTimeoutOrNull(DECISION_WAIT_MILLIS) { port.enabled(declaration) }`. Hết hạn: `logger.warn(TAG, "Rewarded <code> load round: switch not read in time")`, bước là `NOT_NEEDED` (không gửi, không gọi `clearLoading` hay `sendBlocked`), `finally` gỡ `deciding` như hôm nay, lượt nạp sau thử lại được. Thay đổi nhỏ, cùng hàm chờ với bước 1, có test riêng (mục 4).
3. **`held` giữ cả danh sách.** Trong nhánh hiện quảng cáo của `decide`: `held` thêm mỗi kết quả vào `heldOutcomes: MutableList<RewardedOutcome>` thay vì ghi đè một biến.
   - Lượt đã kết thúc ngay trong lời gọi (`turns[declaration] == null`): như hôm nay, kết quả giao cho `endDecision` là kết quả cuối của danh sách (`lastOrNull()`, mặc định `NoAd(false, false)`).
   - Lượt còn: thay `held` bằng `first.onOutcome` trong `turn.callers`, gọi `showAgain` cho từng lần bấm nhập vào (R-06) như hôm nay, rồi giao lại danh sách theo thứ tự đã nhận, theo luật chung: `OnScreen` và `NoAd` cho `first.onOutcome`, `Granted` cho `turn.callers.last()` (người gọi mới nhất, câu hỏi 9 của T-031). Nếu `first.onOutcome` cũng là bộ xử lý của một lần bấm nhập vào (một `RewardedAction` bấm hai lần), bỏ `OnScreen` khi giao lại, vì bộ xử lý đó đã nhận `OnScreen` trong `showAgain`. Lần bấm nhập vào nhận `OnScreen` trong `showAgain` khi `turn.onScreen` đã `true`, như hôm nay.
4. **`onStuck()` không đổi** (câu hỏi 2). Thêm một câu vào KDoc của `onStuck`: lần quyết định đang chạy không bị gỡ, nó tự kết thúc sau tối đa `DECISION_WAIT_MILLIS`.
5. **App ra nền giữa lúc chờ (L-091).** Lần quyết định chạy trong `scope` của controller, không theo vòng đời activity, như hôm nay.
   - Hết hạn khi app ở nền hay đã quay lại: không hiện gì; hộp thoại đóng khi `RewardedAction` nhận kết quả (`NoAd`), hành động chạy nếu chính sách cho. Với `allow` (hay `allow_after_<n>` đã đủ lần), hành động chạy ngay cả khi app đang ở nền, như mọi kết quả rewarded hôm nay (`RewardedAction` không chờ app quay lại); hành động là hàm của ViewModel nên chạy được ở nền. Quay lại thì hộp thoại đã đóng.
   - Khóa có giá trị trong 5 giây: đi tiếp như hôm nay. Có quảng cáo thì gọi hiện trên activity của lần bấm; activity đó đã dừng thì SDK báo lỗi hiện, đi `onShowFailed`, áp chính sách (R-04, như hôm nay khi app ra nền ngay sau lần bấm). Không có quảng cáo thì chính sách như thường.
   - Hộp thoại đã mở trước khi ra nền giữ nguyên khi quay lại (`backgroundWhileDialogOpen_sameDialogOnReturn`, không đổi).
6. **Tài liệu luật** (trả lời câu hỏi 1, và ghi chú bổ sung dưới câu trả lời):
   - Mục 11 của `docs/ops/ads-business-rules.md` thêm dòng "Chờ của một lần quyết định rewarded (khóa và lượt nạp, cộng lại); lượt nạp rewarded đọc khóa | 5 giây; lần quyết định hết giờ coi như chưa có quảng cáo (R-03), không hiện gì sau lúc chờ; lượt nạp hết giờ đọc khóa thì không gửi, không hẹn thử lại (không phải lỗi nạp của F-05), lượt nạp sau đến từ "nạp nếu cần" (F-07) hay nạp sẵn | Không có (T-047)".
   - Dòng R-06 thêm nửa câu: "lần quyết định quá 5 giây (mục 11) kết thúc như chưa có quảng cáo, lần bấm sau là lần quyết định mới".
   - Mục 8.2, câu "Khi nào ghi `ad_opportunity`", thêm nửa câu: "lần quyết định rewarded hết giờ chờ (mục 11) ghi `not_ready` với `load_state` lúc hết giờ, có thể là `ready` khi quảng cáo đã nạp xong trong lúc chờ khóa".
   - `docs/ops/ads-guide.md`, gạch đầu dòng **Rewarded**: thêm "chờ khóa và lượt nạp tối đa 5 giây; quá thì coi như không có quảng cáo".
7. KDoc của `request`, `decide`, `loadIfNeeded`: nêu hạn chờ.

Mẫu có sẵn: `deciding` và `try/finally` của `loadIfNeeded`; `Turn.callers` và `showAgain` của T-042.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt` | `DECISION_WAIT_MILLIS`; một `withTimeoutOrNull` bọc phần chờ của `decide` (enum bước riêng); đọc khóa có giới hạn trong `loadIfNeeded`; `heldOutcomes` và luật giao lại; KDoc của `request`, `decide`, `loadIfNeeded`, `onStuck` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | 10 test mới (mục 4); bản giả `FakeRewardedPort` thêm `syncShowedReward` và `syncDismiss` (trong `show`, gọi `turns.onShowed`, `turns.onReward`, rồi `turns.onDismissed` nếu bật), thuộc commit test của bước 3 (L-025) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | 2 test mới (L-091, mục 4); lớp giả `HoldingFlags` (bọc `DefaultAdsFlags`, giữ lần thu kế tiếp của `placementEnabled` tới khi một `CompletableDeferred` xong) và hàm dựng `holdingController()`, thuộc commit test của bước 3 |
| Sửa | `docs/ops/ads-business-rules.md` | Mục 11: một dòng hằng số; dòng R-06: nửa câu; mục 8.2: nửa câu về `not_ready` khi hết giờ (mục 3.1, bước 6) |
| Sửa | `docs/ops/ads-guide.md` | Gạch đầu dòng Rewarded: hạn chờ 5 giây (mục 3.1, bước 6) |
| Sửa | `docs/features/ads/tickets/T-047-rewarded-quyet-dinh-treo.md` | `status`, nhật ký theo phiên |
| Sửa | `docs/features/ads/plans/T-047-rewarded-quyet-dinh-treo.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): `RewardedTurns` chỉ có `AdsController.kt` tạo và gọi (`request`, `onShowed`, `onReward`, `onDismissed`, `onShowFailed`, `onStuck`, `loadIfNeeded`, `onLoadFailed`); chữ ký công khai không đổi. `RewardedActionTest.kt` (Robolectric, `realController()`) đi qua `RewardedTurns` thật; bản phác thảo đã qua toàn bộ test này (mục 4.1). `ads-guide.md`: không script, workflow hay test nào đọc nội dung gạch đầu dòng Rewarded. `ads-business-rules.md`: không script hay workflow nào đọc; các test nhắc tới nó chỉ trong chú thích, không mở file, và chỉ nhắc MC-02, mục S, 4, 5.7, 8, 10: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` (mục 10, chép tay), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Plan thêm một dòng mục 11, sửa dòng R-06 và nửa câu mục 8.2 về rewarded; `AdTelemetryRulesTest`, `LoadAttemptCounterTest`, `FirebaseAdMetricsTest` chỉ nhắc mục 8.2 trong chú thích, không chép câu `ad_opportunity` của rewarded, nên không test nào bị ảnh hưởng. Không ảnh chuẩn nào đổi: không đổi giao diện.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi hành vi của định dạng rewarded chỉ khi lần quyết định hay lần đọc khóa của lượt nạp chờ quá 5 giây (hôm nay không xảy ra trong app thật) và khi SDK gọi đồng bộ. Không đổi khóa Remote Config.
- Sự kiện phân tích: không thêm sự kiện. Lần quyết định hết giờ ghi `ad_opportunity` `not_ready` (lần bấm đầu) và `busy` (lần bấm nhập vào), như đường "không có quảng cáo" hôm nay; tổ hợp mới `not_ready` với `load_state = ready` có thể xảy ra (mục 3.1, bước 6). Hôm nay lần quyết định treo không ghi gì.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `withTimeoutOrNull` thuộc `kotlinx.coroutines` (`kotlinx-coroutines-core`), đã có trên classpath biên dịch của `:core:ads` (`RewardedTurns.kt` đã dùng hàm `suspend`; bản phác thảo đã biên dịch, mục 4.1). Test dùng `CompletableDeferred`, `flow`, `emitAll` của cùng thư viện.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `withTimeoutOrNull` đặt một hẹn giờ trên `Dispatchers.Main.immediate` của `AdsController`. Bản phác thảo qua toàn bộ test JVM của `:core:ads`, gồm `RewardedActionTest` với controller thật trên Robolectric và hai test L-091 mới, nhưng chưa chạy `ui-test` trên emulator.
- **Điều kiện chuyển:** một trong các dấu hiệu sau, sau 2 lần sửa: (1) một test có sẵn dùng controller thật trên Robolectric đỏ vì hẹn giờ còn treo hay vì thứ tự chạy đổi; (2) job `ui-test` của CI đỏ ở một test rewarded của `app/src/androidTest` (hay báo coroutine chưa xong khi test kết thúc) mà cùng test xanh trên nhánh chính; (3) `RewardedActionTest` báo `IdlingResource` hay `waitForIdle` hết giờ vì hẹn giờ 5 giây trên looper chính.
- **Cách làm thay:** hẹn giờ qua cổng. `RewardedPort` thêm `fun schedule(delayMillis: Long, block: () -> Unit): Cancel` (hủy được); `AdsController` cài bằng `scope.launch { delay(delayMillis); block() }` trả hàm hủy job; bản giả dùng `TestScope` của test (`backgroundScope.launch { delay(...) }`), nên `advanceTimeBy` vẫn điều khiển. `decide` và `loadIfNeeded` chạy phần chờ trong một `suspendCancellableCoroutine` / `CompletableDeferred` mà hẹn giờ kết thúc với `null`, thay cho `withTimeoutOrNull`. Hành vi và test viết trước của mục 4 giữ nguyên, kể cả hai test treo và hai test L-091; chỉ thêm một dòng gắn `turns` với scope của test trong hàm dựng của bản giả.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | (dự phòng) `schedule()` của `RewardedPort` |

### 3.5. Hành vi đã có bị đổi

Kết quả `scripts/existing-tests.py docs/features/ads/plans/T-047-rewarded-quyet-dinh-treo.md` (2026-10-27): 269 test dùng tên của `RewardedTurns.kt`. Nhóm "Cần xem trước" có 7 test, đều của native (`NativeFlowStepLayoutTest`, `NativeFlowStepTest`, `NativeEligibleReportTest`), chỉ trùng tên chung `show`, `take` của lớp khác; không test nào đo vị trí hay kích thước của rewarded. Nhóm "Nên xem" có 179 test (chạy lại 2026-10-27 trên bản 2); phần liên quan thật là 42 test của `RewardedTurnsTest` (gọi thẳng `RewardedTurns`) và các test của `RewardedActionTest` (controller thật); phần còn lại (`AdPreloaderTest`, `BannerLoadsTest`, `AppOpenRulesTest`...) chỉ trùng tên chung `request`, `ready`, `show`. Đã đọc các test của `RewardedTurnsTest` và `RewardedActionTest` có câu khẳng định gần các hành vi plan đổi; bản phác thảo qua toàn bộ test này (mục 4.1).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lần quyết định chờ `port.enabled()` không giới hạn | Một hạn chung 5 giây, hết hạn đi đường "không có quảng cáo" | Không test nào để cổng chờ quá 5 giây ảo: `twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce`, `doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity`, `decisionsOfTwoPlacementsApart`, hàm dựng `doubleTapAwaitingSwitch` mở cổng bằng `runCurrent()`, không tiến thời gian | Giữ. Bản phác thảo qua các test này không sửa |
| Lần quyết định chờ lượt nạp không giới hạn | Chờ phần còn lại của hạn chung, rồi đếm chính sách | `secondTapWhileLoading_joinsDecision` mở `loadGates` bằng `runCurrent()` | Giữ |
| Lượt nạp đọc khóa không giới hạn, giữ `deciding` | Đọc tối đa 5 giây, hết giờ không gửi, gỡ `deciding` | `loadIfNeeded_switchedOff_sendsNothing`, `loadIfNeeded_gateClosed_reportsSendBlocked_noSend`, `loadIfNeeded_open_sendsOnce` | Giữ |
| `ad_opportunity` của rewarded: `not_ready` luôn đi với `load_state` khác `ready` (quảng cáo sẵn sàng thì hiện) | Hết giờ chờ khóa mà quảng cáo đã nạp: `not_ready` với `load_state = ready` | `notReady_defaultBlock_noGrant_loadsIfNeeded_recordsNotReady`, `AdTelemetryRulesTest.loadStateAtDecision_snapshot` (không khẳng định cặp này) | Giữ; test mới `decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy` khẳng định cặp mới; luật mục 8.2 ghi cặp này (câu hỏi 1, ghi chú bổ sung) |
| SDK gọi `onShowFailed` trong lời gọi hiện: kết quả cuối là `NoAd` | Như cũ: kết quả cuối của danh sách `heldOutcomes` | `showFailsInsideTheCall_endsTurnOnce`, `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy` (T-046 sửa đối chứng của test này, chạy trước) | Giữ |
| SDK gọi `onShowed`, `onReward` trong lời gọi hiện: người gọi đầu chỉ nhận `Granted`; có lần bấm nhập vào thì người gọi đầu nhận thưởng; một bộ xử lý cho hai lần bấm nhận `[Granted, OnScreen]` | Luật giao lại chung của bước 3: mỗi bộ xử lý `OnScreen` một lần rồi `Granted` cho người gọi mới nhất | Không test nào: bản giả hôm nay không gọi đồng bộ | Test mới (mục 4) |
| `onStuck()` chỉ gỡ `turns` | Không đổi | `stuck_endsPendingTurnsWithoutOutcome` | Giữ |
| App ra nền khi hộp thoại mở: hộp thoại giữ nguyên | Không đổi | `RewardedActionTest.backgroundWhileDialogOpen_sameDialogOnReturn` | Giữ |

## 4. Kiểm thử

Số đo trên code hiện tại (L-029), bằng test tạm không commit, nhánh `claude/plan-T-047-rewarded-treo` (bản 1 từ `6547211`, bản 2 từ `7110e9f`; lệnh và kết quả ở mục 4.1):

- Treo ở khóa (`switchGates` một `CompletableDeferred` không bao giờ xong), lần bấm hai sau `onStuck()`, có quảng cáo: hai người gọi `[]` và `[]`, `shows = []`. Đỏ.
- Treo ở khóa, `advanceTimeBy(600_000)`, lần bấm hai: `[]`, `shows = []`. Đỏ.
- Treo ở lượt nạp (`loadGates` không xong), `rw_allow`, `onStuck()`, `advanceTimeBy(600_000)`, lần bấm hai: `[]`, cơ hội `[(rw_allow, not_ready, idle)]`. Đỏ.
- Bản 2, hạn chung (`ready = false`, cổng khóa mở `true` lúc 3 000 ms, `loadGates` một cổng không bao giờ xong): lúc 4 999 ms `[]`, `loads = [rw_get_item]`; lúc 5 001 ms vẫn `[]`. Đỏ ở câu khẳng định kết quả (`AssertionError`), không phải vì coroutine chưa xong. Với hai hạn riêng, kết quả chỉ tới lúc 8 000 ms, nên test cũng đỏ với cách làm đó.
- Treo ở khóa, có quảng cáo, `advanceTimeBy(5_001)`: `[]`, `loads = []`. Đỏ.
- Treo ở khóa, máy không có mạng (`port.offline = true`), `advanceTimeBy(5_001)`: `[]`. Đỏ.
- Lượt nạp treo ở lần đọc khóa, `advanceTimeBy(5_001)`, lượt nạp sau: `sends = []`. Đỏ.
- Trong lúc `rw_get_item` treo, `request(rwAllow)`: `[NoAd(false, true)]` ngay. Xanh (đối chứng L-068).
- SDK gọi đồng bộ `onShowed`, `onReward`, một người gọi: `[Granted]`. Xanh về số lần, mất `OnScreen`.
- SDK gọi đồng bộ cả `onDismissed`: `[Granted]`; lần bấm sau hiện quảng cáo mới (`shows` 2). Xanh.
- SDK gọi đồng bộ, hai người gọi khác nhau cùng chờ khóa: `a = [Granted]`, `b = [OnScreen]`. Đỏ. Cùng một bộ xử lý: `[Granted, OnScreen]`. Đỏ.
- Controller thật trên Robolectric, khóa giữ lại (`HoldingFlags`) sau khi hộp thoại mở, bấm "Watch ad", ra nền rồi trở lại: khóa có giá trị trước 5 giây thì hộp thoại đóng, `granted = 1` (`allow`). Xanh. Không có giá trị, `idleFor(5_001 ms)`: hộp thoại vẫn mở, `granted = 0`. Đỏ.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Ticket, treo ở khóa: lần bấm nhập vào có kết quả sau hạn chung; hai vị trí không trộn (L-068) | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy`: `port.ready["rw_get_item"] = true`; `port.switchGates += CompletableDeferred<Boolean>()` (không bao giờ xong); `launch { request { a += it } }`, `runCurrent()`, `launch { request { b += it } }`, `runCurrent()`. Đối chứng L-068: `request(rwAllow) { other += it }` ngay lúc này: `other == [NoAd(false, true)]`. `advanceTimeBy(4_999)`, `runCurrent()`: `a`, `b` rỗng. `advanceTimeBy(2)`, `runCurrent()`: `a == [NoAd(false, false)]`, `b == [NoAd(false, false)]` (`block`), `port.shows` rỗng (không hiện sau lúc chờ), `port.loads` rỗng (hết hạn ở khóa thì không gọi lượt nạp), cơ hội của `rw_get_item` `[(rw_get_item, not_ready, ready), (rw_get_item, busy, ready)]`, log có một dòng `warn` chứa `not done in time`. Đối chứng L-068 sau hạn: `request(rwAllow)` lần nữa vẫn `NoAd(false, true)` (`allow` không có bộ đếm; hết hạn của `rw_get_item` không đổi `noAdInARow` của `rw_allow`). Hôm nay đỏ: `[]`, `[]` (đã đo) |
| Ticket, hạn chung, không quá 5 giây cho khóa và lượt nạp cộng lại | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_noAd_endsAfterWait`: `ready = false`; `val gate = CompletableDeferred<Boolean>()`, `port.switchGates += gate`; `port.loadGates += CompletableDeferred<Unit>()` (không bao giờ xong); `launch { request() }`, `runCurrent()`; `advanceTimeBy(3_000)`, `gate.complete(true)`, `runCurrent()`; `advanceTimeBy(1_999)`, `runCurrent()`: `outcomes` rỗng; `advanceTimeBy(2)`, `runCurrent()`: `outcomes == [NoAd(false, false)]`, `port.loads == [rw_get_item]` (một lần), cơ hội `[(rw_get_item, not_ready, idle)]`. Hai hạn riêng chỉ cho kết quả lúc 8 000 ms nên đỏ. Hôm nay đỏ: `[]` lúc 5 001 ms, đỏ ở câu khẳng định (đã đo) |
| KB-19 tiêu chí 4, hết hạn khi không có mạng | Unit test | `RewardedTurnsTest.decisionStuckAtSwitch_offline_reportsNetwork`: `port.offline = true`; `switchGates += CompletableDeferred<Boolean>()` (không xong); `launch { request() }`, `runCurrent()`, `advanceTimeBy(5_001)`, `runCurrent()`: `outcomes == [NoAd(network = true, granted = false)]`. Đối chứng (L-055): `offline = false`, lần quyết định treo mới, hết hạn: `NoAd(network = false, granted = false)`. Hôm nay đỏ: `[]` (đã đo) |
| Ticket, lần bấm sau lượt treo là lần quyết định mới | Unit test | `RewardedTurnsTest.afterStuckDecision_nextTapShowsAd`: như dòng đầu tới hết hạn; rồi `request { c += it }` (hàng cổng đã rỗng, khóa bật, quảng cáo vẫn sẵn sàng vì lần quyết định hết hạn không lấy ra): `port.shows == [rw_get_item]`, `port.showAgains` rỗng; `fireShowed()`, `fireReward()`: `c == [OnScreen, Granted]`. Đối chứng (L-055): `a`, `b` không nhận thêm gì. Hôm nay đỏ: `shows = []` (đã đo) |
| Ticket, treo ở lượt nạp, chính sách `allow` | Unit test | `RewardedTurnsTest.decisionStuckAtLoad_endsAfterWait_policyOnce`: `port.loadGates += CompletableDeferred<Unit>()` (không xong); hai `launch { request(rwAllow) { ... } }`, `runCurrent()`; `advanceTimeBy(5_001)`, `runCurrent()`: người gọi đầu `[NoAd(false, false)]`, người gọi sau `[NoAd(false, true)]` (chính sách một lần), cơ hội `[(rw_allow, not_ready, idle), (rw_allow, busy, idle)]`, log có `not done in time`. Đối chứng: một `request(rwAllow)` nữa ra `NoAd(false, true)` ngay (lượt nạp không còn treo). Test này không phân biệt hạn chung với hai hạn riêng (khóa trả ngay); `decisionStuckAtSwitch_noAd_endsAfterWait` làm việc đó. Hôm nay đỏ: không kết quả nào (đã đo) |
| Lượt nạp đọc khóa có giới hạn (L-072, đường nạp sẵn, thử lại, nạp lại) | Unit test | `RewardedTurnsTest.loadRound_switchStuck_releasesAfterWait`: `switchGates += CompletableDeferred<Boolean>()` (không xong); `launch { turns.loadIfNeeded(rwGetItem) }`, `runCurrent()`; `turns.loadIfNeeded(rwGetItem)`: `port.sends` rỗng (vòng đang chờ giữ vị trí); `advanceTimeBy(5_001)`, `runCurrent()`; `turns.loadIfNeeded(rwGetItem)`: `port.sends == [rw_get_item]`, log có `load round: switch not read in time`. Hôm nay đỏ: `sends = []` (đã đo) |
| Ticket, `onStuck()` không gỡ lần quyết định, lần quyết định vẫn hết hạn (câu hỏi 2) | Unit test | `RewardedTurnsTest.stuckDuringDecision_decisionStillEndsAfterWait`: treo ở khóa như dòng đầu; `turns.onStuck()`, `runCurrent()`: chưa có kết quả, `port.presenceLowered == 0`; `advanceTimeBy(5_001)`, `runCurrent()`: đúng một `NoAd(false, false)`. Hôm nay đỏ ở câu cuối: không kết quả (đã đo) |
| KB-19 tiêu chí 3, SDK gọi đồng bộ, một người gọi | Unit test | `RewardedTurnsTest.syncShowedAndReward_oneCaller_onScreenThenGrantOnce`: `port.syncShowedReward = true`, `ready = true`, `request()`: `outcomes == [OnScreen, Granted]`, `port.rewards == [rw_get_item]`; `fireShowed()`, `fireReward()`, `fireDismissed()`: không đổi. Hôm nay đỏ: `[Granted]` (đã đo) |
| KB-19 tiêu chí 3 và R-06, SDK gọi đồng bộ, lần bấm nhập vào (L-068) | Unit test | `RewardedTurnsTest.syncShowedAndReward_joinedTap_latestGranted`: `syncShowedReward = true`, `ready = true`, một `switchGates` mở sau khi hai `launch { request { a += it } }`, `launch { request { b += it } }` đã chờ: `a == [OnScreen]`, `b == [OnScreen, Granted]`, `port.rewards` một phần tử, `port.showAgains == [rw_get_item]`. Phần hai, cùng một bộ xử lý (`collect` dùng chung, như một `RewardedAction` bấm hai lần), lượt mới với `ready = true` và một cổng mới: `outcomes == [OnScreen, Granted]` (mỗi kết quả một lần, đúng thứ tự). Hôm nay đỏ: `a = [Granted]`, `b = [OnScreen]`; bộ xử lý chung `[Granted, OnScreen]` (đã đo) |
| KB-19 tiêu chí 3, SDK gọi đồng bộ cả lúc đóng | Unit test | `RewardedTurnsTest.syncShowedRewardDismissed_grantOnce_nextTapShowsAgain`: `syncShowedReward = true`, `syncDismiss = true`, `ready = true`, `request()`: đúng một `Granted`; tắt `syncShowedReward`, `ready = true`, `request()`: `port.shows.size == 2`. Canh giữ, xanh cả hôm nay (đã đo) |
| L-091, app ra nền giữa lúc chờ, khóa có giá trị trong hạn | Robolectric | `RewardedActionTest.backgroundWhileDecisionWaits_switchInTime_closesOnce`: `holdingController()` (khóa `gallery_rw_action` bật), `openConsentGateForTest()`, `setRealContent(controller, override = Allow)`, `start()` (hộp thoại mở); `flags.hold = CompletableDeferred()`; `tapWatch()`: `cards() == 1`, `granted == 0`; `scenario.moveToState(CREATED)`, rồi `RESUMED`; `runOnUiThread { hold.complete(Unit) }`, `waitForIdle()`: `cards() == 0`, `granted == 1` (không SDK nên không có quảng cáo, `allow`). Canh giữ, xanh cả hôm nay (đã đo) |
| L-091, hết hạn khi app đang ở nền | Robolectric | `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce`: như trên nhưng không mở `hold`; `tapWatch()`, `scenario.moveToState(CREATED)`; khi đang ở nền, `shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5_001))`: `granted == 1` (`allow` cho hành động chạy cả khi ở nền), đúng một dòng log `Ads` chứa `not done in time`; rồi `scenario.moveToState(RESUMED)`, `waitForIdle()`: `cards() == 0`, `granted == 1`. Đối chứng: `hold.complete(Unit)` sau đó, `waitForIdle()`: `granted` vẫn `1` (tối đa một lần). Hôm nay đỏ: `granted == 0` khi ở nền (đã đo) |
| Hành vi cũ giữ nguyên | Unit test, Robolectric | Mọi test có sẵn của `RewardedTurnsTest`, `RewardedActionTest` và các test của `AdsController` qua không sửa (mục 3.5) |
| Luật mới đọc được | Kiểm tra thủ công (người: đọc luật) | Người duyệt đọc dòng mới của mục 11, R-06, mục 8.2 trong `ads-business-rules.md` và gạch đầu dòng Rewarded của `ads-guide.md` |

**Ngoại lệ viết test trước:** hai test canh giữ xanh cả trên code hiện tại (đã đo): `syncShowedRewardDismissed_grantOnce_nextTapShowsAgain` (giữ kết quả khi lượt kết thúc ngay trong lời gọi) và `backgroundWhileDecisionWaits_switchInTime_closesOnce` (L-091, đường khóa có giá trị trong hạn không đổi). Đối chứng L-068 trong `decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy` cũng xanh hôm nay, nhưng nằm trong một test đỏ. Mười test còn lại đỏ trên code hiện tại, lý do đã đo ở đầu mục này. Đường "có quảng cáo, activity đã dừng, SDK báo lỗi hiện" của L-091 không có test mới: không SDK trong test, và đường `onShowFailed` đã có test (`showFailed_endsTurnWithNoAd_policyApplies`). Dòng của `ads-business-rules.md` và `ads-guide.md` là tài liệu, không có test. Không có test emulator: lỗi không xảy ra trong app thật hôm nay (mục 1), và chờ 5 giây chỉ điều khiển được trên JVM.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, nhánh `claude/plan-T-047-rewarded-treo`.
  - Bản 1 (từ `6547211`): 7 test tạm trong `RewardedTurnsTest` trên code hiện tại; bản phác thảo với hai hạn riêng.
  - Bản 2 (từ `7110e9f`): bản phác thảo không commit của bước 1 đến 3 ở mục 3.1 (một `withTimeoutOrNull` cho cả lần quyết định với enum bước, đọc khóa có giới hạn trong `loadIfNeeded`, `heldOutcomes` với luật giao lại và bỏ `OnScreen` trùng), 4 test tạm mới trong `RewardedTurnsTest` (hai cổng treo không có quảng cáo, vị trí khác trong lúc treo, lượt nạp treo ở khóa, bộ xử lý chung khi SDK đồng bộ) và 2 test tạm trong `RewardedActionTest` (`HoldingFlags`, ra nền rồi trở lại, khóa trong hạn và hết hạn). Chạy các test tạm này trên code hiện tại (gỡ bản phác thảo) để đo: số ở đầu mục 4.
  - Bản 2, sau rà soát vòng 2: bản phác thảo thêm đọc cờ mạng khi hết hạn ở khóa; 4 test tạm: hạn chung (khóa mở lúc 3 000 ms, lượt nạp treo), treo ở khóa có quảng cáo (`loads` rỗng), treo ở khóa khi không có mạng, L-091 hết hạn khi app ở nền. Đo trên code hiện tại, rồi chạy cả module với bản phác thảo.
  - Đã gỡ bản phác thảo và test tạm, không commit.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*RewardedTurnsTest*'` và `--tests '*RewardedTurnsTest.tmp*' --tests '*RewardedActionTest.tmp*'` (đo trên code hiện tại), `./gradlew :core:ads:testDebugUnitTest --continue` (bản phác thảo trong cây); JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`. Không chạy `scripts/check.sh`: E dưới 2, không có kỹ thuật hay thư viện mới (L-042 không áp dụng; ADR-0029).
- **Kết quả:**
  - Trên code hiện tại: bản 1, 49 test của `RewardedTurnsTest`, 5 đỏ; bản 2, 6 test tạm, 5 đỏ: hai cổng treo, lượt nạp treo ở khóa, bộ xử lý chung, L-091 hết hạn (đều đỏ ở câu khẳng định); "vị trí khác trong lúc treo" đỏ chỉ vì coroutine treo không xong khi test kết thúc, câu khẳng định L-068 của nó xanh. 1 xanh (L-091 khóa trong hạn). Sau rà soát vòng 2: 4 test tạm, cả 4 đỏ ở câu khẳng định (`AssertionError`): hạn chung `[]` lúc 5 001 ms, `loads = [rw_get_item]`; treo ở khóa `[]`, `loads = []`; không mạng `[]`; L-091 ở nền `granted = 0`.
  - Với bản phác thảo bản 2: `:core:ads:testDebugUnitTest` BUILD SUCCESSFUL, mọi test có sẵn và mọi test tạm qua, gồm `RewardedActionTest` với controller thật và `idleFor(5_001 ms)` trên looper chính. Hẹn giờ của `withTimeoutOrNull` chạy được trên Robolectric. Sau rà soát vòng 2, cả module lại BUILD SUCCESSFUL với 4 test tạm mới: hạn chung lúc 4 999 ms `[]`, lúc 5 001 ms `[NoAd(false, false)]`, `loads = [rw_get_item]`, cơ hội `[(rw_get_item, not_ready, idle)]`; treo ở khóa `[NoAd(false, false)]`, `loads = []`; không mạng `[NoAd(network = true, granted = false)]`; L-091 ở nền `granted = 1`, quay lại `cards() = 0`, `granted = 1`.
  - Không đổi giao diện nên không ghi lại ảnh chuẩn. Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi luồng rewarded khi lần quyết định chờ quá lâu; thêm một hằng số vào luật nghiệp vụ; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện; số liệu chỉ thêm ở đường hôm nay không ghi gì. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code, hai file test trong một module, ba chỗ tài liệu luật và một câu của hướng dẫn; chữ ký công khai không đổi |
| B. Độ rõ của yêu cầu | 1 | Anh Nhật đã trả lời ba câu hỏi (thời gian chờ, `onStuck`, `held`); còn tổ hợp số liệu mới đã ghi dưới câu trả lời 1 |
| C. Khả năng kiểm chứng tự động | 0 | Mọi đường có test JVM điều khiển thời gian ảo, đường L-091 có test Robolectric với controller thật; bản phác thảo đã qua toàn bộ test của module |
| D. Ảnh hưởng chéo | 1 | T-046 sửa đối chứng và chú thích trong cùng `RewardedTurnsTest.kt`, chạy trước (mục 6) |
| E. Độ mới của kỹ thuật | 1 | `withTimeoutOrNull` lần đầu dùng trong `RewardedTurns`, nhưng thuộc `kotlinx.coroutines` đã dùng khắp dự án; đã chạy thử trên Robolectric |
| **Tổng** | 4 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RewardedTurns.kt` của `:core:ads`; `docs/ops/ads-business-rules.md` (mục 11, dòng R-06, mục 8.2); `docs/ops/ads-guide.md` (gạch đầu dòng Rewarded). `AdsController.kt` không sửa (trừ phương án dự phòng).
- T-046 sửa `RewardedTurnsTest.kt`: đối chứng của `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy` (`port.switchGates.clear()` trước lần gọi thứ hai, để đi đúng nhánh nhập vào) và chú thích của `doubleTapAwaitingSwitch`. T-047 thêm test ở cuối lớp và hai trường vào bản giả, không sửa hai chỗ đó. Đối chứng mới của T-046 đi qua nhánh hiện quảng cáo của `decide` mà bước 3 đổi (`heldOutcomes`): đối chứng đó (lượt không lỗi, lần bấm sau nhận `Granted`, lần bấm đầu không nhận gì) phải xanh với `heldOutcomes`; phiên chạy lại nó sau bước 4, đỏ thì sửa code, không sửa test (mục 7).
- T-046 (đã duyệt) cũng sửa `docs/ops/ads-guide.md`, ở bảng kích thước native; T-047 sửa gạch đầu dòng Rewarded của cùng file, chỗ khác. Chạy nối tiếp nên không xung đột; `scripts/run-check.py` sẽ báo hai plan cùng sửa một file.
- Đề xuất thứ tự chạy: T-046 trước T-047, khớp plan T-046. T-047 bắt đầu từ nhánh chính đã có T-046, nên test viết trước của T-047 đặt sau phần T-046 đã sửa.

## 7. Điều kiện dừng riêng

- Cách làm buộc phải đổi câu khẳng định của một test có sẵn về R-06 hay T-042 (`secondRequestBeforeOnScreen_showsAgain_recordsBusy`, `twoCallersSamePlacement_*`, `doubleTap*`, `secondTapWhileLoading_joinsDecision`, `showFailsInsideTheCall_endsTurnOnce`), hay đối chứng mà T-046 vừa sửa: dừng, vì đó là luật đã chốt.
- Muốn hiện quảng cáo sau khi hết hạn, cho làm hành động (`Granted`) khi hết hạn chờ khóa, hay cho tổng thời gian chờ quá 5 giây: dừng, vì trái trả lời câu hỏi 1.
- Muốn cho `onStuck()` gỡ `decisions`: dừng, vì trái trả lời câu hỏi 2.

## 8. Câu hỏi cho người duyệt

1. Thời gian chờ của một lần quyết định rewarded (chờ khóa `is_show_<mã>` và chờ lượt nạp) là bao lâu, và hết giờ thì làm gì? Luật hôm nay chưa nói; AdsKit không có bước chờ này. Phương án: (a) 5 giây, hết giờ coi như quảng cáo chưa sẵn sàng (R-03): ghi `not_ready`, chính sách MC-05, không hiện gì sau lúc chờ; (b) 5 giây, hết giờ coi như vị trí tắt: hành động làm thẳng; (c) không thêm thời gian chờ, chỉ ghi vào hợp đồng của `RewardedPort` rằng hai hàm này không được treo, và bỏ ba test treo. **Đề xuất của AI:** (a). Hộp thoại chỉ mở khi khóa đã bật, nên coi khóa là bật và quảng cáo chưa có là đúng nhất; (b) cho làm hành động không có quảng cáo khi khóa đang bật; (c) để lỗi sống lại khi lượt nạp thành `suspend` (T-042, câu hỏi 4). 5 giây đủ dài so với một nhịp chờ thật, đủ ngắn để người dùng không bỏ đi. Ghi hằng số vào mục 11 và nửa câu vào R-06 (đổi luật nghiệp vụ, cần người duyệt).
   - **Trả lời (anh Nhật, 2026-10-27):** (a), chờ 5 giây, hết giờ coi như quảng cáo chưa sẵn sàng; duyệt thêm hằng số ở mục 11 và nửa câu ở R-06 của luật.
   - **AI bổ sung sau rà soát (bản 2, 2026-10-27):** (1) 5 giây là một hạn chung cho cả lần quyết định (chờ khóa và lượt nạp cộng lại), và cùng hạn cho lần đọc khóa của lượt nạp, để không bao giờ chờ quá 5 giây (mục 3.1, bước 1, 2). (2) Hết giờ chờ khóa khi quảng cáo đã nạp xong thì `ad_opportunity` ghi `not_ready` với `load_state = ready`, tổ hợp hôm nay không có; plan ghi thêm nửa câu vào mục 8.2 của luật (mục 3.1, bước 6). Người duyệt xem lại hai điểm này khi duyệt bản 2.
2. `onStuck()` có gỡ lần quyết định đang chạy (`decisions`) không? Phương án: (a) có: mọi lần bấm đã nhập vào nhận `NoAd(false, false)`, hộp thoại đóng; (b) không: lần quyết định tự kết thúc sau thời gian chờ của câu hỏi 1. **Đề xuất của AI:** (b). `onStuck()` chỉ chạy khi một quảng cáo rewarded đã nhận cờ "đang hiện" (`recoverStuckFullScreen`, `onAppActivityDestroyed`), tức lượt đã có trong `turns`; lần quyết định chưa hiện gì nên không bao giờ gây ra `onStuck()`. Gỡ theo (a) chỉ cắt ngang lần quyết định của vị trí khác đang chạy bình thường, mất kết quả của nó.
   - **Trả lời (anh Nhật, 2026-10-27):** (b), `onStuck()` không gỡ lần quyết định, theo đề xuất.
3. Sửa `held` khi SDK gọi đồng bộ (mục 3.1, bước 2), hay chỉ thêm test canh giữ? Hành động đã chạy đúng một lần (đã đo); chỗ lệch là người gọi đầu mất `OnScreen` và thưởng trao cho người gọi đầu thay vì người gọi mới nhất khi có lần bấm nhập vào. AdMob không gọi đồng bộ, nên người dùng không thấy; `RewardedAction` dùng một bộ xử lý cho mọi lần bấm, nên chỉ hai hành động khác nhau của cùng vị trí mới thấy khác. Phương án: (a) sửa như bước 2 (vài dòng, bản phác thảo đã qua mọi test); (b) không sửa code, chỉ giữ `syncShowedRewardDismissed_grantOnce_nextTapShowsAgain` và một test canh giữ "một người gọi, một `Granted`"; bỏ `syncShowedAndReward_oneCaller_onScreenThenGrantOnce` và `syncShowedAndReward_joinedTap_latestGranted`. **Đề xuất của AI:** (a), vì sửa nhỏ và đưa đường đồng bộ về cùng luật với đường không đồng bộ (câu hỏi 9 của T-031), một SDK mediation lạ gọi đồng bộ cũng không đổi người nhận thưởng.
   - **Trả lời (anh Nhật, 2026-10-27):** (a), sửa `held`, theo đề xuất.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-28 trong phiên giám sát cùng AI, gồm phần đổi luật nghiệp vụ (hạn chờ 5 giây ở mục 11, nửa câu ở R-06 và mục 8.2 của `ads-business-rules.md`). Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua hai vòng rà soát độc lập, sửa theo điểm chặn và điểm nên sửa. Chạy sau T-046.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
