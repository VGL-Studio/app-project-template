---
ticket: T-085
title: Dọn test và luật biên của khung banner hai nền tảng
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 7.3M         # cỡ S, số gợi ý 7.3M của scripts/token-usage.py estimate (2026-11-01, trung vị 18 task S); không chỉnh
blocking_points: [core, ads]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-085: DỌN TEST VÀ LUẬT BIÊN CỦA KHUNG BANNER HAI NỀN TẢNG

## 1. Tóm tắt

Xử lý bốn ghi chú của rà soát độc lập T-071 về `OrderedBannerAdView` (`PlatformBanners.kt`): thêm test bắt đột biến cho nhánh bỏ kết quả giả cũ (`sendNo`, ghi chú 1); bỏ lệnh `resumeChild` không đường nào cần ở nhánh `Loaded` khi khung chạy, và thêm test cho nhánh kề nó mà đường "gửi khi khung đang dừng" cần (ghi chú 2); khi một con khác còn đang chờ kết quả lượt gửi, khung giữ tối đa một lỗi làm mới của con đang hiện cho mỗi lượt gửi, để kết quả của MAX không bị bước xả hộp thư của `sendWhenAllowed` bỏ, và lỗi làm mới thứ hai trong cùng lượt vẫn đi qua làm lối thoát khi con chờ không trả lời (ghi chú 3, phương án "B, giữ tối đa 1 lỗi" anh Nhật chọn ở câu hỏi 1); sửa chú thích quanh dòng 174 cho khớp KDoc (ghi chú 6). Đã đo trên code hiện tại bằng đột biến và test tạm (mục 4.1): hai đột biến của ghi chú 1, 2 sống sót qua 72 test có sẵn; ca của ghi chú 3 tái hiện được, do cả luật của khung (lỗi làm mới đi qua khi còn lượt chờ) lẫn bước xả của `sendWhenAllowed`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31, tiêu chí 1 | Khi nền tảng thứ nhất theo `ads_mediation_order` chưa có quảng cáo cho một lượt hiện và nền tảng thứ hai có thì người dùng thấy quảng cáo của nền tảng thứ hai; một lượt chỉ ghi một cơ hội | Với banner (B-09): kết quả của nền tảng được hỏi trong lượt gửi là câu trả lời của lượt đó, không bị kết quả giả cũ hay lỗi làm mới của con khác thay. Ghi chú 1 và 3 thuộc tiêu chí này |
| KB-31, tiêu chí 2 | Khi `ads_mediation_order` là `admob` hay `max` thì chỉ nền tảng đó được nạp và hiện | Không đổi; khung một con không đi vào nhánh mới (con đang hiện cũng là con chờ, nhánh `pending` xử lý trước) |
| KB-18, tiêu chí 1, 2 | Banner tự đổi theo nhịp của đơn vị; khi app ở nền thì banner không tải lại hay thử lại | Giữ nguyên (ticket: "hành vi KB-18 giữ nguyên"). Con vừa thành con hiện sau khi từng bị dừng vẫn tự làm mới khi khung chạy (đã có `shownAfterPaused_isResumed`); khung đang dừng thì con MAX vừa nạp giữ dừng tới khi khung chạy lại (test mới của ghi chú 2) |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); không đổi giao diện, không đổi ảnh chuẩn.

ADR liên quan: ADR-0037 (MAX là nguồn thứ hai), ADR-0026 (nghiệp vụ quảng cáo theo luật, luật thuần có test), ADR-0029 (phần ngoài mục 3.2), ADR-0031 (câu khẳng định cũ), ADR-0032 (đẩy nhánh). Luật: `docs/ops/ads-business-rules.md` B-02, B-09.

Nguồn: báo cáo `docs/runs/RUN-20261031-01.md`, mục Cần người kiểm tra, dòng "Điểm rà soát mức ghi chú (T-071)" (anh Nhật quyết 2026-11-01 gom (1), (2), (3), (6) vào T-085); nhật ký plan T-071 bản 3.

## 3. Giải pháp

### 3.1. Cách làm

Chỉ sửa `OrderedBannerAdView` trong `PlatformBanners.kt` và test của nó; `runBannerLoads` (`BannerLoads.kt`), `MaxBannerAdView`, `BannerSlotHost`, `AdSlot.kt` không đổi. Test theo mẫu có sẵn của `PlatformBannersTest`: hàm `collect(view)` (thu trên `UnconfinedTestDispatcher`) cho luật của khung, hàm `runLoads(view)` (chạy `runBannerLoads` thật trên dispatcher chuẩn của `runTest`) cho đường qua vòng gửi; bản giả `FakeBannerAdView`.

1. **Ghi chú 1, nhánh `sendNo`** (chỉ thêm test, code giữ nguyên): `handle` bỏ kết quả giả có `sendNo` khác `sendCount` (dòng 185). Nhánh này chỉ chạy khi khung nhận hai lượt `send()` trước khi lần thu xử lý kết quả giả của lượt đầu. Trong app, `runBannerLoads` chỉ gửi lại sau khi nhận một kết quả, nên đường này không xảy ra qua vòng gửi (đo ở mục 4.1: không test có sẵn nào chạm nó); nhánh là phòng thủ cho hợp đồng của lớp (`send()` gọi hai lần liền). Test mới gọi thẳng `send()` hai lần trên dispatcher chuẩn: lượt đầu MAX chưa sẵn sàng (kết quả giả vào hộp thư), lượt hai MAX đã sẵn sàng (gửi thật). Kết quả giả cũ không được coi là câu trả lời của lượt hai: không `TryNext`, không dừng MAX; `Loaded` của MAX sau đó là câu trả lời.
2. **Ghi chú 2, `resumeChild` ở nhánh `Loaded`** (bỏ một lệnh, thêm test cho nhánh kề):
   - Bỏ `resumeChild(index)` khi `running` ở nhánh `Loaded` của `answer`. Khối `if (running) { resumeChild(index) } else if (platform == AdPlatform.MAX) { pauseChild(index) }` thành `if (!running && platform == AdPlatform.MAX) pauseChild(index)`. Lý do (chứng minh bằng liệt kê, đo ở mục 4.1): khi khung chạy, con `pending` không bao giờ đang `paused`. `pauseChild` chỉ được gọi ở bốn chỗ: `pause()` của khung (đặt `running = false`; `resume()` sau đó `resumeChild(pending)`); nhánh `Loaded` cho các con khác con vừa nạp (đã đặt `pending = null` trước); nhánh `Failed` của con MAX (đã đặt `pending = null`); nhánh `Loaded` khi khung dừng (đã đặt `pending = null`). Còn `send()` luôn `resumeChild(index)` trước `send()` của con, trừ lượt MAX chưa sẵn sàng, mà lượt đó chỉ có câu trả lời là kết quả giả `Failed` (MAX chưa dựng view nên không có `Loaded` thật; `maxReady` chỉ đổi từ `false` sang `true`, `MaxSdk.kt` dòng 92 đến 121). Đột biến bỏ lệnh này sống sót qua cả 72 test của ba lớp và cả 1347 test của `:core:ads` khi chạy kèm bản thử của bước 3 (mục 4.1).
   - Nhánh kề (`else if (platform == AdPlatform.MAX) pauseChild(index)`, khung dừng thì con MAX vừa nạp giữ dừng) có đường cần nó nhưng chưa có test (đột biến bỏ nhánh này cũng sống sót, mục 4.1): `BannerFrameContent` chạy hai `LaunchedEffect` độc lập, một gọi `pause()`/`resume()` theo `visible`, một chạy `runBannerLoads`; khi màn hiện lại, `sendWhenAllowed` có thể gọi `send()` trước khi vòng kia kịp `resume()` khung. Khi đó `send()` chạy con MAX (`resumeChild`) trong lúc khung còn dừng; nếu `Loaded` đến trước `resume()` thì chính nhánh này dừng lại MAX. Test mới cho đúng đường này (`pause()`, `send()`, `Loaded`, rồi `resume()`).
   - KDoc của lớp không đổi ("A child paused is resumed before it sends" vẫn đúng, và là lý do lệnh bị bỏ thừa).
3. **Ghi chú 3, lỗi làm mới của con đang hiện khi còn lượt chờ** (đổi luật biên, phương án "B, giữ tối đa 1 lỗi" của câu hỏi 1):
   - Hiện nay (đo ở mục 4.1, thứ tự `[MAX, ADMOB]`, AdMob đang hiện): AdMob làm mới lỗi lần 1 thì khung phát `Failed`, `runBannerLoads` chờ 2 giây rồi gửi, khung hỏi MAX (`pending` là MAX). AdMob làm mới lỗi lần 2 trong lúc MAX còn chờ: nhánh `showing` của `handle` cho `Failed` đi qua, `runBannerLoads` tính lỗi thứ 2 và chờ 4 giây. MAX nạp được trong lúc chờ: khung xử lý đúng (`showing` thành MAX, chạy MAX, phát `Loaded`), nhưng `Loaded` nằm trong hộp thư của `runBannerLoads` và bị bước xả của `sendWhenAllowed` (`BannerLoads.kt` dòng 91) bỏ khi hết 4 giây. Hậu quả đo được: `onLoaded` không chạy, bộ đếm lỗi không về 0 (lần lỗi sau chờ 6 giây thay 2 giây), và khung gửi thêm một lượt `loadAd()` cho MAX đang hiện (MAX nhận 3 lượt gửi thay 2). Thử bỏ bước xả (đột biến tạm): `onLoaded` có chạy nhưng lượt gửi thừa vẫn còn, nên gốc là luật của khung (lỗi làm mới đi qua khi đã có lượt gửi đang chờ), bước xả chỉ làm mất kết quả. MAX lỗi trong lúc chờ thì không hại: `TryNext` bị xả, lượt gửi sau hỏi AdMob (`next = 1`), như mong muốn. Ca đã đo có đúng một lỗi làm mới của AdMob trong lúc lượt gửi cho MAX còn chờ (lỗi lần 1 xảy ra trước lượt gửi, không có con chờ, nên là lượt thử lại bình thường), nên luật giữ tối đa một lỗi vẫn sửa được ca này: không tính lỗi thừa, nghe được `Loaded` của MAX, không gửi thừa (đo bằng bản thử, mục 4.1).
   - Cách sửa: thêm trạng thái riêng tư `heldFailures` (số lỗi làm mới của con đang hiện đã giữ trong lượt gửi hiện tại), `send()` đặt về 0 cùng chỗ tăng `sendCount`. Trong `handle`, nhánh `showing` (con đang hiện mà không phải con `pending`): kết quả là `Failed`, `pending` khác `null` và `heldFailures < 1` thì tăng `heldFailures`, không phát gì, log `"Banner <key> <platform đang hiện> refresh failed, waiting for <platform đang chờ>"`; phiên tách chuỗi log thành hai dòng nối bằng `+` như log `"no ad, trying"` có sẵn (ktlint 120 ký tự), chữ ghép lại đúng như câu mà test khẳng định. Lỗi làm mới thứ hai trong cùng lượt gửi (`heldFailures` đã là 1) thì phát `Failed` như hôm nay. Lượt gửi đang chờ chính là lượt thử lại của B-02; kết quả của nó quyết định: `Loaded` thì khung hiện con đó và `runBannerLoads` nhận `Loaded` (đếm lỗi về 0, `onLoaded`); `Failed` của nền tảng trước thì `TryNext`, của nền tảng cuối thì một lần lỗi. `Loaded` của con đang hiện khi còn lượt chờ vẫn đi qua như hôm nay (không đổi). Con đang hiện cũng là con chờ (khung một con, hay con thứ nhất đang hiện được hỏi lại) thì nhánh `pending` xử lý trước, không đổi.
   - Không đổi `runBannerLoads`: bước xả vẫn cần cho banner một nền tảng và kết quả cũ trước một yêu cầu mới (`BannerLoadsTest.staleResultBeforeStart_isIgnored`); sửa ở đó ảnh hưởng mọi banner (phương án D của câu hỏi 1, không đề xuất).
   - Lối thoát khi con chờ không trả lời: không có bằng chứng rằng con `pending` luôn trả lời (rà soát độc lập nêu hai đường có thể mất câu trả lời: `tryEmit` của `SharedFlow` không phát lại cho người thu đến muộn, và `stopAutoRefresh()` giữa lượt nạp). Vì vậy khung chỉ giữ một lỗi mỗi lượt gửi: con đang hiện làm mới lỗi lần nữa thì `Failed` đi qua, `runBannerLoads` tính lỗi, chờ theo B-02 rồi gửi lượt mới (lượt mới ghi đè `pending` và đặt lại `heldFailures`). Điều đã biết, đo ở mục 4.1: với bản B thuần (giữ mọi lỗi), MAX chờ không trả lời và AdMob làm mới lỗi 5 lần trong khoảng 55 giây thì vòng gửi đứng ở `receive()` (`failures` 1, MAX 2 lượt gửi); với bản giữ tối đa một lỗi, cùng ca cho `failures` 3, MAX 4 lượt gửi, vòng gửi chạy tiếp; code hiện tại cho `failures` 6, MAX 7 lượt gửi. Con đang hiện không làm mới lỗi lần hai (ví dụ nó đã dừng) thì vòng gửi chờ câu trả lời của con `pending` như hôm nay, không tệ hơn code hiện tại. Lần thu bị hủy giữa chừng (khung thôi `ACTIVE`) thì vòng mới bắt đầu bằng một lượt gửi, đặt lại `pending`.
   - KDoc của lớp, gạch "The refreshes of the child shown pass through (Failed is retried from the first platform)" thành: "The refreshes of the child shown pass through (Failed is retried from the first platform), except the first Failed of a send while another child asked has not answered: that answer decides the turn; a second Failed of the same send passes, so a child that never answers does not stop the retries (B-09)".
   - `docs/ops/ads-business-rules.md`, dòng B-09: thêm vế sau "lượt thử lại bắt đầu lại từ nền tảng thứ nhất": "; nền tảng đang hiện làm mới lỗi trong lúc lượt thử lại còn chờ nền tảng khác thì lần lỗi đầu của lượt đó không tính, kết quả của lượt đang chờ quyết định; lần lỗi thứ hai trong cùng lượt tính như một lần lỗi của B-02, để lượt chờ không trả lời không làm ngừng thử lại (T-085)".
4. **Ghi chú 6, chú thích ở dòng 174 đến 175**: chú thích nói "A platform shorter than the frame takes its own height", nhưng code cho mọi nền tảng có chiều cao trong `heightsDp` vẽ ở chiều cao của nó, không chỉ nền tảng thấp hơn khung, và nền tảng không có chiều cao nhận `modifier` của khung (đúng như KDoc dòng 87, 88). Chú thích mới: "// A platform with a height in heightsDp is drawn at that height, full width, at the top of the frame (B-09): an AdView of AdMob given the height of a taller frame would centre its ad in it. A platform without one takes the frame's modifier." Chỉ đổi chú thích, không đổi code.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PlatformBanners.kt` | Bước 2: bỏ `resumeChild(index)` ở nhánh `Loaded` khi khung chạy, giữ nhánh dừng MAX khi khung dừng. Bước 3: trạng thái `heldFailures`, đặt lại ở `send()`; nhánh `showing` của `handle` giữ lỗi làm mới đầu tiên của mỗi lượt gửi khi còn con `pending`, kèm log, lỗi thứ hai đi qua; KDoc gạch về làm mới của con đang hiện. Bước 4: chú thích trong `Content` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PlatformBannersTest.kt` | Năm test mới của mục 4 |
| Sửa | `docs/ops/ads-business-rules.md` | Dòng B-09 thêm một vế (bước 3) |

Không tạo, không sửa ảnh chuẩn. `FakeBannerAdView.kt`, `BannerLoadsTest.kt`, `BannerSlotHostTest.kt`, `BannerLoads.kt`, `MaxBannerAdView.kt`, `AdSlot.kt` không đổi: test mới dùng các hàm có sẵn của bản giả (`emitLoaded`, `emitFailed`, `isRunning`, `sendCount`, `pauseCount`) và các hàm phụ có sẵn của `PlatformBannersTest` (`ordered`, `collect`, `runLoads`). Không file nào đọc chữ của dòng B-09: `grep -rln "ads-business-rules" scripts/ .github/ core/*/src/test` chỉ thấy chú thích và chuỗi nguồn (`scripts/ads-plan.py` chỉ dẫn tên tài liệu, không phân tích bảng B); `grep -rn "B-09"` trong `scripts/` và test chỉ thấy PB-09 (bảng tra lỗi) và một chú thích của `BannerSlotHostTest`. Các test sau nhắc tới `docs/ops/ads-business-rules.md` chỉ trong chú thích hay KDoc dẫn nguồn của luật khác (MC-02, mục 8, A-..., N-...), không đọc file, nên không bị ảnh hưởng khi thêm một vế vào B-09: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng. Đổi luật biên của lượt thử lại banner hai nền tảng (bước 3): bớt một lượt gửi thừa cho MAX và một lần lỗi B-02 tính thừa trong ca hiếm của ghi chú 3; vì thế ghi điểm chặn 1 cho chắc (mục 5).
- Sự kiện phân tích: không thêm, không đổi tham số. Ca của ghi chú 3 bớt một lượt `ad_load_*` của MAX do lượt gửi thừa (số liệu theo đúng lượt gửi thật, không phải đổi sự kiện).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Chỉ dùng `kotlinx-coroutines` và test có sẵn của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Bản thử của bước 2, 3 đã chạy trên máy dev: 1347 test của `:core:ads` qua (mục 4.1).

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-085-don-khung-hai-nen-tang.md` (2026-11-01), kết quả ở mục 4.1. Đã đọc các test của `PlatformBannersTest`, `BannerSlotHostTest` (nhóm hai nền tảng), `BannerLoadsTest`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Con vừa nạp khi khung chạy được `resumeChild` ở nhánh `Loaded` (thừa: `send()` đã chạy nó) | Bước 2: bỏ lệnh; con vẫn chạy nhờ `send()` hay `resume()` của khung | `PlatformBannersTest.shownAfterPaused_isResumed` (`max.resumeCount` 1, `max.isRunning`), `singleChild_pausedWhileLoading_resumedThenLoaded_runs`, `BannerSlotHostTest.covered_maxShown_stopsAutoRefresh_uncovered_starts` | Giữ: đều qua với bản thử đã bỏ lệnh (mục 4.1) |
| Con đang hiện làm mới lỗi khi con khác còn chờ: khung phát `Failed` | Bước 3: lỗi đầu của lượt gửi không phát, kết quả của con chờ quyết định; lỗi thứ hai của cùng lượt phát `Failed` như cũ | Không test nào dựng ca này (đo: 1347 test qua với bản thử). `refreshOfShown_passesThrough_otherIgnored` kiểm lỗi làm mới khi không có con chờ; `firstShownRefreshFails_isFailed_notTryNext` kiểm con thứ nhất đang hiện | Giữ cả hai: ca của chúng không có con chờ khác con đang hiện |
| `runBannerLoads` bỏ kết quả còn trong hộp thư khi gửi yêu cầu mới | Không đổi | `BannerLoadsTest.staleResultBeforeStart_isIgnored`, `tryNext_sendsAtOnce_noFailureCounted_noOnFailed` | Giữ |
| Nhánh `sendNo`, nhánh dừng MAX khi khung dừng | Không đổi code, thêm test | Không có (đột biến sống sót) | Test mới ở mục 4 |
| Chú thích trong `Content` nói hẹp hơn code | Bước 4: chỉ đổi chú thích, code vẽ giữ nguyên | `BannerSlotHostTest.shorterPlatform_drawnAtFrameTop_atItsOwnHeight` (nhóm "Cần xem trước" của `existing-tests.py`: đo vị trí, kích thước) | Giữ: code vẽ không đổi |
| Khung một con MREC bị che rồi hết che, lỗi thử lại sau 2 giây | Không đổi: khung một con, con đang hiện cũng là con chờ, nhánh `pending` xử lý trước nhánh mới của bước 3 | `BannerSlotHostTest.mrec_covered_pausesView_keepsAd_noRetry_uncoveredResumes` (nhóm "Nên xem") | Giữ: qua với bản thử (mục 4.1) |

Không có câu khẳng định cũ nào phải viết lại.

## 4. Kiểm thử

Mọi test trong `PlatformBannersTest`, JVM thuần (không Robolectric), theo mẫu của lớp. Tên `KEY` là `gallery_banner`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-31 tiêu chí 1, ghi chú 1: kết quả giả cũ không là câu trả lời của lượt gửi mới | Unit test | `PlatformBannersTest.staleSkippedTurn_isNotAnswerOfNextSend`: khung `[MAX, ADMOB]`, thu bằng `backgroundScope.launch { view.results.collect { got += it } }` trên dispatcher chuẩn (không `UnconfinedTestDispatcher`, để kết quả giả còn trong hộp thư), `runCurrent()`. `ready = false`, `send()`, `ready = true`, `send()`, `runCurrent()`: `got` rỗng, `max.sendCount` 1, `max.pauseCount` 0. `max.emitLoaded()`, `runCurrent()`: `got == [Loaded]`. Đối chứng trong cùng test: khung mới `ordered(MAX to max2, ADMOB to admob2)` với bản giả mới `max2`, `admob2` (như các test có sẵn), thu `got2` cùng cách, `ready = false`, một `send()`, `runCurrent()`: `got2 == [TryNext]`, `max2.sendCount` 0 (kết quả giả của đúng lượt vẫn được nghe) |
| KB-18, ghi chú 2: khung đang dừng thì con MAX vừa nạp giữ dừng tới khi khung chạy lại, kể cả khi `send()` đến trước `resume()` | Unit test | `PlatformBannersTest.sendWhilePaused_loadedBeforeResume_maxStaysPausedUntilResume`: khung `[MAX, ADMOB]`, `collect(view)`, `view.pause()`, `view.send()` (con MAX chạy lại để gửi), `max.emitLoaded()`, `runCurrent()`: `got == [Loaded]`, `assertFalse(max.isRunning)`, `max.pauseCount` 2. `view.resume()`: `assertTrue(max.isRunning)` |
| KB-31 tiêu chí 1, ghi chú 3: lỗi làm mới của con đang hiện không thay câu trả lời của con đang chờ (luật của khung) | Unit test | `PlatformBannersTest.shownRefreshFails_whileOtherPending_waitsForPending`: khung `[MAX, ADMOB]`, `collect(view)`; `send()`, `max.emitFailed(204)`, `send()`, `admob.emitLoaded()`, `runCurrent()`: `[TryNext, Loaded]`. `admob.emitFailed(3)`, `runCurrent()`: `[TryNext, Loaded, Failed(3)]`. `send()` (MAX chờ, `max.sendCount` 2), `admob.emitFailed(3)`, `runCurrent()`: danh sách không đổi, log có `"Banner $KEY ADMOB refresh failed, waiting for MAX"`. `max.emitLoaded()`, `runCurrent()`: `[TryNext, Loaded, Failed(3), Loaded]`, `max.isRunning`. Đối chứng (lối thoát): khung thứ hai với bản giả mới `max2`, `admob2`, dựng như trên tới lượt `send()` thứ ba (MAX chờ); `admob2.emitFailed(3)` bị giữ (danh sách không đổi), `admob2.emitFailed(5)` lần hai trong cùng lượt gửi đi qua: danh sách thêm `Failed(5)`; rồi `max2.emitLoaded()` vẫn là câu trả lời: thêm `Loaded`. Đối chứng thứ hai: `Loaded` của con đang hiện trong lúc con khác chờ vẫn đi qua (khung thứ ba, bản giả mới, `admob3.emitLoaded()` khi MAX chờ: danh sách thêm `Loaded`) |
| KB-31 tiêu chí 1, ghi chú 3: qua vòng gửi thật, kết quả của MAX được nghe, không gửi thừa | Unit test | `PlatformBannersTest.shownRefreshFails_whileMaxPending_throughRunBannerLoads_maxLoadHeard`: `runLoads(ordered(MAX to max, ADMOB to admob))`, `runCurrent()`; `max.emitFailed(204)`, `runCurrent()`; `admob.emitLoaded()`, `runCurrent()` (`loads` 1); `admob.emitFailed(3)`, `runCurrent()`, `advanceTimeBy(2_001)`, `runCurrent()`: `max.sendCount` 2. `admob.emitFailed(3)`, `runCurrent()`: `failures` 1 (đo trên code hiện tại: `expected:<1> but was:<2>`; đây là lỗi làm mới duy nhất trong lúc MAX chờ, lỗi bị giữ). `max.emitLoaded()`, `runCurrent()`: `loads` 2. `advanceTimeBy(10_001)`, `runCurrent()`: `max.sendCount` 2 (hiện tại 3), `admob.sendCount` 1 |
| KB-31 tiêu chí 1, B-02, ghi chú 3: con chờ không trả lời thì lỗi làm mới thứ hai của lượt gửi vẫn làm vòng gửi chạy tiếp (lối thoát) | Unit test | `PlatformBannersTest.shownRefreshFailsTwice_whileMaxPendingSilent_throughRunBannerLoads_retriesGoOn`: dựng như test 4 tới khi `max.sendCount` 2 (`failures` 1, MAX chờ và không bao giờ trả lời trong test). `admob.emitFailed(3)`, `runCurrent()`: `failures` 1. Đối chứng: `advanceTimeBy(30_000)`, `runCurrent()`: `failures` 1, `max.sendCount` 2 (một lỗi thì giữ, vòng gửi chờ MAX). `admob.emitFailed(3)` lần hai, `runCurrent()`: `failures` 2. `advanceTimeBy(4_001)`, `runCurrent()`: `max.sendCount` 3 (lượt gửi mới). `admob.emitFailed(3)`, `runCurrent()`: `failures` 2 (lượt gửi mới đặt lại bộ đếm, lỗi đầu lại bị giữ); log `"Banner $KEY ADMOB refresh failed, waiting for MAX"` có đúng 2 dòng. Trên code hiện tại đỏ ở câu đầu (`expected:<1> but was:<2>`, đã đo) |
| Khung một con, con thứ nhất đang hiện, lỗi làm mới khi không có con chờ: không đổi | Unit test có sẵn | `admobOnly_failure_doesNotPause_retrySendsOnRunningView`, `lastPlatformFails_afterPause_isFailed`, `refreshOfShown_passesThrough_otherIgnored`, `firstShownRefreshFails_isFailed_notTryNext`, `bothFail_oneFailure_retryStartsFromFirst` giữ nguyên và phải qua |
| Bỏ `resumeChild` không đổi KB-18 | Unit test có sẵn | `shownAfterPaused_isResumed`, `singleChild_pausedWhileLoading_resumedThenLoaded_runs`, `pauseAll_resumeOnlyShown`, `BannerSlotHostTest.covered_maxShown_stopsAutoRefresh_uncovered_starts` giữ nguyên và phải qua |
| Chú thích khớp KDoc (ghi chú 6) | Kiểm tra thủ công (người: đọc chú thích) | Người rà soát đọc chú thích trong `Content` so với KDoc dòng 87, 88 |
| Luật B-09 mới đúng ý người duyệt | Kiểm tra thủ công (người: đọc tài liệu luật) | Đọc dòng B-09 của `docs/ops/ads-business-rules.md` sau khi sửa |

Thứ tự viết test của phiên: test 3, 4 và 5 đỏ trên code hiện tại (đã đo, mục 4.1) và xanh sau bước 3. Test 1 và 2 kiểm hành vi đã có nên qua ngay trên code hiện tại; để chúng thất bại đúng lý do, phiên chạy chúng một lần với đột biến tạm (không commit) trước khi commit test: bỏ dòng `if (tagged.sendNo != null && tagged.sendNo != sendCount) return null` thì test 1 đỏ `expected:<[]> but was:<[TryNext]>`; bỏ nhánh `else if (platform == AdPlatform.MAX) { pauseChild(index) }` thì test 2 đỏ ở `assertFalse(max.isRunning)` (cả hai đã đo, mục 4.1). Ghi kết quả vào nhật ký (L-108). Sau bước 2, chạy lại test 2 với đột biến trên dòng mới `if (!running && platform == AdPlatform.MAX) pauseChild(index)` (xóa dòng) để chắc test vẫn bắt.

**Ngoại lệ viết test trước:** (1) test 1 và 2 qua ngay trên code hiện tại vì chúng giữ hành vi đã có, không phải hành vi mới; bằng chứng thất bại đúng lý do là lần chạy với đột biến tạm ở trên (anh Nhật đồng ý, câu hỏi 3). (2) Bỏ lệnh `resumeChild` (bước 2) không có test đỏ trước được: lệnh không đường nào chạy tới (chứng minh ở mục 3.1 bước 2, đột biến sống sót qua 1347 test); các test có sẵn của dòng "Bỏ `resumeChild` không đổi KB-18" giữ hành vi. (3) Chú thích (bước 4) và dòng B-09 là chữ, không có test.

### 4.1. Chạy thử

Không bắt buộc (E dưới 2); ghi lại các lần đo của ghi chú 1, 2, 3 (L-029), máy dev, 2026-11-01, nhánh từ `origin/main` tại `8610b63e`, JDK 21 của `scripts/local-setup.sh`. Mọi đột biến và test tạm đã hoàn tác (`git status` sạch).

- **Đã chạy thử:** đột biến tạm trên `PlatformBanners.kt` và lớp test tạm `TmpT085Test.kt` (không commit), bản thử của bước 2, 3.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*PlatformBanners*' --tests '*BannerSlotHost*' --tests '*BannerLoads*'` (thêm `--tests '*TmpT085*'` khi có lớp tạm); `./gradlew :core:ads:testDebugUnitTest` cho bản thử. Không chạy `scripts/check.sh` (E dưới 2, không ảnh chuẩn nào đổi).
- **Kết quả:**
  - Code hiện tại: 72 test của ba lớp, 0 lỗi.
  - Ghi chú 1, đột biến bỏ dòng `sendNo` (dòng 185): 72 test, 0 lỗi (sống sót). Test tạm (tương đương test 1 của mục 4): qua trên code hiện tại; với đột biến đỏ `expected:<[]> but was:<[TryNext]>`. Với `collect` trên `UnconfinedTestDispatcher` thì kết quả giả được xử lý ngay trong `send()`, nên test phải thu trên dispatcher chuẩn.
  - Ghi chú 2, đột biến bỏ `resumeChild(index)` ở nhánh `Loaded` khi khung chạy: 72 test, 0 lỗi (sống sót). Đột biến bỏ nhánh `else if (platform == AdPlatform.MAX) pauseChild(index)`: 72 test, 0 lỗi (sống sót). Test tạm `pause()`, `send()`, `Loaded`, `resume()`: trên code hiện tại sau `send()` MAX chạy (`resumeCount` 1, khung vẫn dừng), sau `Loaded` MAX dừng (`pauseCount` 2), sau `resume()` MAX chạy; với đột biến bỏ nhánh `else if` đỏ ở `assertFalse(max.isRunning)`. Test tạm `pause()`, `send()`, `resume()`, `Loaded`: `resumeCount` không đổi khi `Loaded` đến (lệnh `resumeChild` ở nhánh `Loaded` không làm gì: con đã chạy).
  - Ghi chú 3, test tạm qua `runBannerLoads` thật (tương đương test 4): sau lỗi làm mới thứ hai của AdMob trong lúc MAX chờ, `failures` 2; MAX nạp trong lúc chờ 4 giây thì sau khi hết chờ `loads` 1 (không tăng), `max.sendCount` 3, `admob.sendCount` 1, log `retry in 2000 ms (failures 1, code 3)` rồi `retry in 4000 ms (failures 2, code 3)`; MAX trả lời lượt gửi thừa thì `loads` mới thành 2. Biến thể MAX lỗi trong lúc chờ: hết chờ thì khung hỏi AdMob (`admob.sendCount` 2), `failures` 2. Đối chứng không có lỗi thứ hai: MAX nạp thì `loads` 2, `failures` 1, `max.sendCount` 2. Thử bỏ bước xả `while (inbox.tryReceive().isSuccess) Unit` của `sendWhenAllowed` (đột biến tạm): `loads` thành 2 ngay sau khi hết chờ nhưng `max.sendCount` vẫn 3; kết luận: bước xả làm mất kết quả, luật của khung gây lượt gửi thừa. Câu khẳng định của test 4 trên code hiện tại đỏ `second AdMob failure while MAX loads expected:<1> but was:<2>`.
  - Bản thử 1 của bước 2 và 3 (phương án B thuần, giữ mọi lỗi làm mới khi còn con chờ; rồi hoàn tác): 81 test (72 có sẵn và 9 test tạm) qua; cả `:core:ads:testDebugUnitTest`: 1347 test, 0 lỗi.
  - Phép dò của rà soát độc lập (bằng chứng cho lối thoát): MAX chờ không trả lời, AdMob đang hiện làm mới lỗi 5 lần, mỗi lần cách 11 giây (khoảng 55 giây). Bản B thuần (rà soát đo): vòng gửi đứng ở `receive()`, `failures` 1, MAX 2 lượt gửi. Code hiện tại (rà soát đo, AI đo lại 2026-11-01 bằng test tạm): `failures` 6, MAX 7 lượt gửi. Bản thử 2 (giữ tối đa một lỗi mỗi lượt gửi, AI đo): `failures` 3, MAX 4 lượt gửi, `admob.sendCount` 1; vòng gửi chạy tiếp.
  - Bản thử 2 của bước 2 và 3 (phương án anh Nhật chọn: `heldFailures`, đặt lại ở `send()`, giữ khi `heldFailures < 1`; rồi hoàn tác), với 5 test tạm (tương đương test 3, 4, 5, đối chứng một lỗi, và phép dò trên): trên code hiện tại 4 test tạm đỏ (`measuredCase` và `escape` ở `expected:<1> but was:<2>`, tầng khung ở danh sách có thêm `Failed(3)`), phép dò chỉ in số. Với bản thử 2: ba lớp và 5 test tạm, 77 test, 0 lỗi; cả `:core:ads:testDebugUnitTest`: 1343 test (1338 có sẵn và 5 tạm), 0 lỗi. Ca đã đo của test 4 có đúng một lỗi làm mới trong lúc chờ, nên bản thử 2 cũng bỏ được lượt gửi thừa (`max.sendCount` 2) và nghe được `Loaded` của MAX (`loads` 2).
  - `python3 scripts/existing-tests.py docs/features/ads/plans/T-085-don-khung-hai-nen-tang.md`: file của plan `PlatformBanners.kt`, 101 test có sẵn dùng tên hay tag của nó; "Cần xem trước" 1 (`BannerSlotHostTest.shorterPlatform_drawnAtFrameTop_atItsOwnHeight`), "Nên xem" 1 (`BannerSlotHostTest.mrec_covered_pausesView_keepsAd_noRetry_uncoveredResumes`), cả hai đã đưa vào mục 3.5; plan đã nhắc 8 test; 91 test còn lại chỉ dùng tên plan không nêu.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core `:core:ads`). 1 (quảng cáo), ghi cho chắc: bước 3 đổi khi nào lượt thử lại banner hai nền tảng được tính và gửi (bớt một lượt gửi thừa của MAX trong ca hiếm), gần với "đổi tần suất". Không chạm 2, 3, 4 (không đổi sự kiện hay tham số), 5, 6, 8, 9.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code và một file test trong `:core:ads`, cộng một dòng tài liệu luật; không đổi giao diện công khai của lớp nào |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu rõ ghi chú 1, 2, 6; ghi chú 3 là đổi luật B-09, anh Nhật đã chọn "B, giữ tối đa 1 lỗi" (câu hỏi 1, 2026-11-01); còn chi tiết nhỏ AI tự quyết (chữ log, chữ của vế B-09) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có test JVM, đã đo; nhịp gọi lại thật của hai SDK (làm mới lỗi trong một lượt nạp, con chờ không trả lời) chỉ thấy trên máy có mã thật, và không cần kiểm thủ công riêng |
| D. Ảnh hưởng chéo | 2 | `OrderedBannerAdView` là đường gửi của mọi banner cố định, MREC, thanh đáy; luật mới chỉ chạm ca hai nền tảng có con đang hiện khác con chờ. T-072 (doanh thu MAX, chưa có plan) có thể sửa cùng nhóm file MAX |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của luật và test có sẵn (T-071) |
| **Tổng** | 6 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `PlatformBanners.kt` (`OrderedBannerAdView`, mọi khung banner cố định và MREC), `docs/ops/ads-business-rules.md` (B-09).
- Task khác trong sprint có thể xung đột: T-084 (`approved`, theme của test `:core:ads`) không sửa `PlatformBanners.kt` hay `PlatformBannersTest.kt`, chạy cùng phiên được. T-072 (backlog, chưa plan) có thể sửa `MaxBannerAdView.kt` và cách ghi số liệu của MAX; plan T-072 đọc lại luật mới của B-09.
- Ngoài phạm vi, có từ trước T-085 (ghi chú rà soát): trên đường `pause()` rồi `send()` (mục 3.1 bước 2), con AdMob được chạy lại khi khung còn dừng, mà nhánh `Loaded` khi khung dừng chỉ dừng lại MAX; AdMob tự làm mới tới khi `resume()` của khung tới, khoảng rất ngắn. Plan không đổi điều này.
- Đề xuất thứ tự chạy: không phụ thuộc task nào chưa merge; chạy trước T-072 để T-072 dựng trên luật đã chốt.

## 7. Điều kiện dừng riêng

- Một test có sẵn của `BannerLoadsTest` đỏ sau bước 3: luật mới không được chạm `runBannerLoads` hay banner một nền tảng; dừng thay vì sửa `BannerLoads.kt`.
- Ảnh chuẩn nào đổi: plan không đổi giao diện; dừng.
- Luật giữ lỗi làm cho một test có sẵn của khung một con đổi kết quả: luật chỉ áp khi con đang hiện khác con chờ; dừng thay vì nới điều kiện.

## 8. Câu hỏi cho người duyệt

1. **Ghi chú 3 là đổi luật B-09.** Khi AdMob (nền tảng thứ hai) đang hiện làm mới lỗi trong lúc lượt thử lại còn chờ MAX, chọn:
   - (B, AI đề xuất) Khung không tính lỗi làm mới đó; kết quả của MAX quyết định lượt (nạp được thì hiện MAX, đếm lỗi về 0; lỗi thì hỏi AdMob ngay). Thêm vế vào B-09 như mục 3.1 bước 3. Bớt một lượt gửi thừa cho MAX và một lần lỗi B-02 tính thừa. Quảng cáo AdMob cũ vẫn hiện tới khi MAX có quảng cáo, như hôm nay.
   - (C) Chấp nhận hành vi hiện tại, ghi vào B-09: "nền tảng đang hiện làm mới lỗi trong lúc lượt thử lại còn chờ thì tính thêm một lần lỗi; kết quả của lượt đang chờ có thể bị bỏ và lượt thử lại sau gửi lại cho nền tảng thứ nhất". Khi đó bỏ test 3, 4 và bước 3; AI soạn plan bản 2.
   - (D) Sửa `runBannerLoads` để kết quả đến trong lúc chờ thử lại cắt ngắn lần chờ: ảnh hưởng mọi banner kể cả một nền tảng, không đề xuất.
   - Trả lời (anh Nhật, 2026-11-01): chọn "B, giữ tối đa 1 lỗi": khi còn con khác `pending`, khung giữ tối đa một lỗi làm mới của con đang hiện cho mỗi lượt gửi (không phát, ghi log); lỗi làm mới thứ hai trong cùng lượt gửi thì phát `Failed` như hôm nay (lối thoát khi con chờ không trả lời); lượt gửi mới đặt lại bộ đếm. Plan đã sửa mục 3.1 bước 3, vế B-09, mục 4 (thêm test 5) và mục 4.1.
2. **Ghi chú 2: bỏ lệnh `resumeChild` thừa** (mục 3.1 bước 2, chứng minh bằng liệt kê và đột biến) thay vì giữ làm phòng thủ? AI đề xuất bỏ, vì không đường nào chạy tới, và hành vi KB-18 đã có test giữ qua đường `send()` và `resume()`. Nếu anh muốn giữ, phiên chỉ thêm chú thích "defensive: send() already resumes the child" và bỏ dòng bước 2 của mục 3.2.
   - Trả lời (anh Nhật, 2026-11-01): bỏ lệnh `resumeChild` thừa, như plan.
3. **Test giữ hành vi đã có qua ngay** (test 1, 2): đồng ý ngoại lệ viết test trước, với bằng chứng là lần chạy có đột biến tạm ghi vào nhật ký?
   - Trả lời (anh Nhật, 2026-11-01): đồng ý ngoại lệ cho `staleSkippedTurn_isNotAnswerOfNextSend` và `sendWhilePaused_loadedBeforeResume_maxStaysPausedUntilResume`; phiên chạy chúng một lần với đột biến tạm (bỏ dòng `sendNo`; bỏ nhánh dừng MAX khi khung dừng), thấy đỏ, ghi vào nhật ký (L-108).
4. **Điểm chặn 1 (quảng cáo):** AI ghi cho chắc vì bước 3 đổi khi nào lượt thử lại được tính; mức rủi ro vẫn Cao do điểm chặn 7. Anh có muốn bỏ `ads` khỏi `blocking_points` không (không đổi mức)?
   - Trả lời (anh Nhật, 2026-11-01): giữ `blocking_points: [core, ads]`.
5. **Rà soát độc lập của plan bản 1** (2026-11-01, một phiên AI riêng): 0 chặn, 1 nên sửa, 4 ghi chú. Nên sửa: phương án B thuần mất lối thoát khi con chờ không trả lời (phép dò: vòng gửi đứng ở `receive()`), câu "không treo vòng gửi" chưa có bằng chứng; đã sửa theo câu trả lời 1 (giữ tối đa một lỗi mỗi lượt gửi, test 5, phép dò ghi ở mục 4.1, câu "không treo" thay bằng điều đã biết và lối thoát). Ghi chú: nhánh kề với con AdMob trên đường `pause()` rồi `send()` (đã ghi ở mục 6, ngoài phạm vi); đối chứng của test 1 dùng bản giả mới (đã ghi rõ); tách dòng log mới theo ktlint (đã ghi ở mục 3.1 bước 3); lập luận ghi chú 2 kín, không cần sửa.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
