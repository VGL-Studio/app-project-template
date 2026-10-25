---
ticket: T-029
title: Banner cố định và banner thu gọn theo PARTNER
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M, số gợi ý 15.0M (scripts/token-usage.py estimate, 2026-10-25); ba file code mới, bốn file test mới, nối vào AdSlot.kt
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-029: BANNER CỐ ĐỊNH VÀ BANNER THU GỌN THEO PARTNER

## 1. Tóm tắt

`AdSlot(placement)` của `:core:ads` vẽ được hai định dạng banner theo B-01 tới B-06 của `docs/ops/ads-business-rules.md`: banner cố định (`banner`) để AdMob tự làm mới, chỉ thử lại khi lỗi; banner thu gọn (`banner_collapsible`) do `:core:ads` nạp lại tay mỗi 30 giây, mỗi lượt một mã yêu cầu mới. Mọi lượt gửi (lượt đầu, thử lại, nạp lại tay, nạp lại khi đồng ý trở lại) đi qua một vòng lặp thuần duy nhất, chờ màn hình hiện và hỏi cổng đồng ý ngay trước khi gửi; rút đồng ý sau khi đã có quảng cáo thì giữ khung, ẩn quảng cáo, tạm dừng banner. Kiểm chứng bằng unit test với thời gian ảo cho vòng lặp, test Robolectric cho khung và cho lớp bọc `AdView`; phần quảng cáo thật kiểm trên máy khi có vị trí banner (câu hỏi 1).

## 2. Hiểu yêu cầu

Ticket xử lý KB-18 trong `features/ads/spec.md` (spec `confirmed`, xác nhận lại 2026-10-24), luật B-01 tới B-06, AR-03 trong `docs/ops/ads-business-rules.md`. Câu hỏi 25 của spec đã trả lời: giữ khung khi rút đồng ý (B-06).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-18 | Khi đơn vị banner bật tự làm mới trên AdMob thì banner tự đổi theo nhịp của AdMob, app không tự tải lại. | Banner cố định (`AdFormat.BANNER`): sau lượt nạp thành công, app không gọi `loadAd` nữa; lần làm mới do SDK tự làm (B-02). App chỉ gọi lại `loadAd` khi một lượt nạp lỗi (thử lại, B-02) hay khi đồng ý mở lại (B-06). Bật tự làm mới là cài đặt của đơn vị trên AdMob, việc của người (ghi vào `ads-guide.md`). |
| KB-18 | Khi app ở nền thì banner không tải lại hay thử lại. | Hai lớp: (1) `AdView.pause()` khi màn hình chứa banner không còn ở trạng thái `RESUMED` (Activity bị che hay app xuống nền), `resume()` khi hiện lại, nên SDK không tự làm mới ở nền; (2) mọi lượt thử lại và nạp lại tay tới hạn khi màn hình không hiện thì chờ tới lần hiện lại kế tiếp mới gửi (B-02, B-05, AR-03). "Hiện" là `Lifecycle.State.RESUMED` của `LocalLifecycleOwner` (Activity, hay mục điều hướng nằm trong Activity). |
| KB-18 | Khi dùng banner thu gọn thì mỗi lần làm mới là một banner bung ra mới. | Mỗi yêu cầu của banner thu gọn mang phần thêm cho AdMob `collapsible = "bottom"` và `collapsible_request_id` là một UUID mới (B-03, trang hướng dẫn banner thu gọn của Google). Cùng một mã thì SDK coi là cùng một lần bung; mã mới thì banner bung ra lại. |
| KB-18 | Khi chưa có đồng ý thì banner không gửi yêu cầu. | Hai lớp: (1) cổng đóng (`canRequestAds` là `false`) và chưa từng có quảng cáo thì không dựng khung, không tạo `AdView` (như hôm nay, B-01); (2) ngay trước mỗi lượt gửi, ở mọi đường (lượt đầu, thử lại, nạp lại tay, nạp lại khi đồng ý mở lại), vòng lặp hỏi `mayRequestNow()`; `false` thì không gửi, kết thúc vòng, không tính là lỗi, không hẹn thử lại (S-15, L-A14). Vòng chạy lại khi cổng đổi. Lượt tự làm mới của SDK không chạy khi cổng đóng vì `AdView` bị tạm dừng (B-06). |
| KB-18 | Khi người dùng rút đồng ý lúc banner đang hiện thì khung banner giữ nguyên chiều cao, giao diện không nhảy; đồng ý lại thì banner tải lại trong khung đó. | Khung đã có quảng cáo ít nhất một lần mà cổng đóng: giữ chiều cao của khung, ẩn `AdView` (`View.INVISIBLE`, không gỡ khỏi cây giao diện), `pause()`, dừng vòng lặp (B-06). Cổng mở lại: hiện `AdView`, `resume()`, gửi một lượt mới trong cùng `AdView` và cùng khung. Ẩn quảng cáo cũ là cách hiểu của AI để không trái KB-12 ("quảng cáo đã tải không hiện nữa") và S-11 (câu hỏi 2). Cổng đóng khi chưa từng có quảng cáo: không giữ khung (B-06 chỉ nói "khi banner đã có quảng cáo"). |
| KB-18 | Banner thu gọn được tải lại sau mỗi 30 giây, không dùng tự làm mới của AdMob. | `AdFormat.BANNER_COLLAPSIBLE`: sau một lượt nạp thành công, vòng lặp chờ 30 giây (hằng số cứng, mục 11) rồi gửi lượt mới; tới hạn khi màn không hiện thì chờ lần hiện lại (B-05). Đơn vị không bật tự làm mới là cài đặt trên AdMob, việc của người (ghi vào `ads-guide.md`). 30 giây tính từ lúc lượt trước nạp xong (câu hỏi 3). |
| KB-18 (B-02) | Luật trong phạm vi ticket. | Lỗi nạp (lượt của app hay lượt tự làm mới của SDK) thì thử lại sau `min(n x 2 giây, 10 giây)` với lần lỗi liên tiếp thứ n, không giới hạn, đếm riêng từng khe đang hiện; nạp được thì bộ đếm về 0 (câu hỏi 4). Dùng `AdRetryPolicy.fullScreenDelayMillis` có sẵn (KDoc của hàm đã ghi B-02). Áp cho cả hai định dạng. |
| KB-18 (B-04) | Luật trong phạm vi ticket. | Mã đơn vị `null`, rỗng hay chỉ có khoảng trắng: không tạo `AdView`, không gửi, khung cao 0 dp. Hôm nay chỉ xét `null`. |
| KB-24 | Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại. | Banner đang chờ thử lại thì gửi ngay khi mạng về, dùng chung tín hiệu `nativeRetryResets` của `AdPreloader` (S-24, như khe native). Không rút ngắn nhịp 30 giây của banner thu gọn (câu hỏi 5). |
| KB-1, KB-17 | Không thuộc ticket. | Thanh đáy (banner trong thanh đáy khi `is_show_native_bottom` tắt, D-01 tới D-05) là T-030; T-030 sẽ gọi `AdSlot` của ticket này. |

Test nghiệm thu gợi ý `AT-BN-*` của ticket nằm trong bản đặc tả độc lập của AdsKit (repo riêng, không có trong repo này). Plan ánh xạ thẳng từ KB-18 và B-01 tới B-06 (bản viết lại của bản đặc tả trong `ads-business-rules.md`); mục 4 đủ một test cho mỗi luật.

Vị trí: `AdPlacement` hiện không có vị trí banner nào (`sample_bottom_banner` đã đổi sang native ở T-010, câu hỏi 3 của spec). Task không thêm vị trí (điểm chặn 1, cần spec): `AdSlot` mới dùng được ngay cho vị trí mà app khai báo; dự án mẫu thấy banner trên máy khi T-030 (thanh đáy) hay T-036 (màn xem thử) thêm vị trí (câu hỏi 1).

Thiết kế tham chiếu: Không có thiết kế riêng. Banner do SDK vẽ; `design/` của feature ads không có ảnh banner nào, và task không đổi màn hình nào có ảnh thiết kế.

ADR liên quan: ADR-0026 (hành vi theo PARTNER), ADR-0004, ADR-0013 (quảng cáo qua `:core:ads`, cổng đồng ý), ADR-0019 (log qua `AppLogger`), ADR-0023 (không thêm khóa).

## 3. Giải pháp

### 3.1. Cách làm

Theo mẫu của khe native (`NativeAdSlot` cùng `NativeAdSlotHost`, `retryingNativeAdStates` của T-021): luật thuần trong hàm có test thời gian ảo, composable có trạng thái nhận phần SDK qua một giao diện để test Robolectric truyền bản giả, phần chạm SDK mỏng nhất có thể.

1. **`BannerLoads.kt` (mới): vòng gửi của một khe banner.**
   - `internal enum class BannerRefresh { BY_AD_UNIT, MANUAL }` và `internal fun bannerRefreshOf(format: AdFormat): BannerRefresh`: `BANNER` ra `BY_AD_UNIT`, `BANNER_COLLAPSIBLE` ra `MANUAL`, định dạng khác ném `IllegalArgumentException` (thay `require(...)` hiện có của `AdSlot`).
   - `internal sealed interface BannerLoadResult { data object Loaded; data class Failed(val code: Int) }`.
   - `internal const val COLLAPSIBLE_BANNER_RELOAD_MILLIS = 30_000L` (B-03, mục 11).
   - `internal suspend fun runBannerLoads(refresh: BannerRefresh, view: BannerAdView, visible: Flow<Boolean>, mayRequestNow: () -> Boolean, retryResets: StateFlow<Int>, onLoaded: () -> Unit, logger: AppLogger, placementKey: String)`: chạy tới khi bị hủy hay cổng đóng lúc gửi. Đăng ký nhận `view.results` trước lượt gửi đầu; kết quả tới trước khi vòng chạy (ví dụ từ lượt trước khi cổng đóng) bị bỏ. Mỗi lượt gửi đi qua đúng một hàm nội bộ `sendWhenAllowed()`: `visible.first { it }`, rồi `mayRequestNow()`; `false` thì log `Banner <mã> request blocked` và kết thúc vòng (S-15); `true` thì `view.send()`.
     - Lượt đầu: `sendWhenAllowed()` ngay.
     - `Loaded`: bộ đếm lỗi về 0, gọi `onLoaded()`. `BY_AD_UNIT`: chờ kết quả kế tiếp, không gửi gì (lượt tự làm mới của SDK cũng trả `Loaded` hay `Failed` qua `view.results`). `MANUAL`: `delay(30 000)` rồi `sendWhenAllowed()`; log `Banner <mã> reload in 30000 ms`.
     - `Failed(code)`: bộ đếm cộng 1, chờ `AdRetryPolicy.fullScreenDelayMillis(n)`, rút ngắn khi `retryResets` đổi giá trị (`withTimeoutOrNull(wait) { retryResets.first { it != generation } }`, như `retryingNativeAdStates`), rồi `sendWhenAllowed()`; log `Banner <mã> retry in <ms> ms (failures <n>, code <code>)`. Không hiện mã đơn vị trong log.
     - Bộ đếm lỗi là biến cục bộ của một lần chạy vòng: hai khe cùng vị trí đếm riêng (L-068); `LoadAttemptCounter` của số liệu (T-023) không đổi.
     - Thời gian đo bằng `delay` của coroutine trên luồng chính (đồng hồ đơn điệu của `Handler`, không phải giờ hệ thống, AR-01).
2. **`BannerFrame.kt` (mới): khung và lúc chạy.**
   - `internal enum class BannerFrame { NONE, ACTIVE, KEPT_EMPTY }`, `internal fun bannerFrame(gateOpen: Boolean, enabled: Boolean, unitId: String?, hadAd: Boolean): BannerFrame`: `enabled` tắt hay mã `isNullOrBlank()` ra `NONE` (B-04, KB-7); cổng mở ra `ACTIVE`; cổng đóng ra `KEPT_EMPTY` nếu `hadAd`, không thì `NONE` (B-01, B-06).
   - `internal fun bannerShouldRun(visible: Boolean, frame: BannerFrame): Boolean` = `visible && frame == ACTIVE`: khi nào `AdView` chạy (`resume`) hay dừng (`pause`).
   - `internal fun Lifecycle.resumedFlow(): Flow<Boolean>` = `currentStateFlow.map { it.isAtLeast(RESUMED) }.distinctUntilChanged()` (`lifecycle-runtime` 2.9.2, đã có; chạy thử ở mục 4.1).
3. **`BannerAdView.kt` (mới): một banner của SDK.**
   - `internal interface BannerAdView { val results: Flow<BannerLoadResult>; fun send(); fun pause(); fun resume(); fun destroy(); @Composable fun Content(showAd: Boolean, modifier: Modifier) }`.
   - `internal fun bannerAdRequest(refresh: BannerRefresh, requestId: () -> String): AdRequest`: `BY_AD_UNIT` ra `AdRequest.Builder().build()` như `adRequest()` hôm nay; `MANUAL` thêm `addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle)` với `collapsible = "bottom"`, `collapsible_request_id = requestId()` (B-03).
   - `internal class SdkBannerAdView(adView: AdView, refresh, placement, unitId, telemetry: AdTelemetry, now: () -> Long, load: (AdRequest) -> Unit = adView::loadAd, newRequestId: () -> String = { UUID.randomUUID().toString() })`: `send()` ghi `telemetry.requestSent(placement.key, now())` rồi `load(bannerAdRequest(refresh, newRequestId))`; `AdListener` chuyển `onAdLoaded`, `onAdFailedToLoad` sang `results` (một `MutableSharedFlow` không giữ giá trị cũ, `extraBufferCapacity` đủ để `tryEmit` từ callback không mất kết quả) và sang số liệu như `attachBannerMetrics` hôm nay (chuyển nguyên phần đó sang lớp này, không đổi sự kiện); `pause`, `resume`, `destroy` gọi thẳng `AdView`; `Content` vẽ `AndroidView(factory = { adView }, update = { visibility = if (showAd) VISIBLE else INVISIBLE })`.
4. **`AdSlot.kt` (sửa): composable.**
   - `AdSlot(placement, modifier)` nhận cả `BANNER` và `BANNER_COLLAPSIBLE` (`bannerRefreshOf`). Không có `LocalAdsController` thì `Box(modifier)` như hôm nay. Có thì đọc `canRequestAds`, `isEnabled(placement)`, `unitId(placement)` như hôm nay, tính `AdSize.getLargeAnchoredAdaptiveBannerAdSize` như hôm nay (B-01), lấy `LocalLifecycleOwner.current.lifecycle.resumedFlow()`, rồi gọi `BannerSlotHost`. `AdView` tạo bằng `LocalContext.current` (Activity của màn hình: Unity và ironSource cần `Activity` khi nạp banner, `ads-guide.md` mục 7).
   - `@Composable internal fun BannerSlotHost(gateOpen: Boolean, enabled: Boolean, unitId: String?, heightDp: Int, refresh: BannerRefresh, visible: Flow<Boolean>, mayRequestNow: () -> Boolean, retryResets: StateFlow<Int>, logger: AppLogger, placementKey: String, newView: () -> BannerAdView, modifier: Modifier)`:
     - `hadAd` là `mutableStateOf(false)` của khe, đặt `true` ở `onLoaded`, về `false` khi khung là `NONE`.
     - `NONE`: `Box(modifier)` cao 0, không dựng phần trong, nên `AdView` (nếu có) bị hủy.
     - `ACTIVE`, `KEPT_EMPTY`: một composable con giữ `remember { newView() }` và `DisposableEffect { onDispose { view.destroy() } }`; khung `fillMaxWidth().height(heightDp.dp)` (giữ chỗ trước khi có quảng cáo như hôm nay); `view.Content(showAd = frame == ACTIVE)`; `LaunchedEffect(view, frame == ACTIVE) { if (ACTIVE) runBannerLoads(...) }`; theo dõi `bannerShouldRun(visible, frame)` và gọi `pause()`, `resume()` khi giá trị đổi (không gọi ở giá trị đầu khi đang chạy).
     - Bỏ `BannerAd` và `attachBannerMetrics` cũ (chuyển sang `SdkBannerAdView`); bỏ `LifecycleEventObserver` cũ (thay bằng `bannerShouldRun`).
   - KDoc: định dạng nào, khung giữ khi rút đồng ý, feature đặt banner thu gọn ở đáy màn hình.
5. **`AdsController.kt` (sửa nhỏ):** thêm `internal val retryResets: StateFlow<Int> get() = preloader.nativeRetryResets` để `AdSlot` đọc tín hiệu mạng về (S-24); KDoc của lớp thêm một câu về banner (B-01 tới B-06). Không đổi `canRequestAds`, `mayRequestNow`, `unitId`, `adRequest`.
6. **`AdPlacement.kt` (sửa KDoc):** KDoc của `AdFormat.BANNER` (tự làm mới theo đơn vị) và `AdFormat.BANNER_COLLAPSIBLE` (thay "No function shows it yet" bằng `AdSlot`, nạp lại tay 30 giây). Không đổi dòng vị trí nào.
7. **Tài liệu `docs/ops/ads-guide.md`:** mục 3 (dòng banner: có từ T-029), mục 5 bước 4 (dòng "Hiện có": `AdSlot(placement)` vẽ banner cố định hay thu gọn theo định dạng của vị trí; banner thu gọn đặt ở đáy), mục 5 bước 6 (đơn vị AdMob của vị trí `banner` bật tự làm mới, của vị trí `banner_collapsible` tắt tự làm mới), mục 6 (gạch banner: thêm banner thu gọn 30 giây, thử lại, chỉ khi màn hình hiện; các dòng log), mục 7 (dòng Unity và ironSource: banner nạp bằng Activity của màn hình).

Thứ tự trong phiên: commit test (bước 3 của `auto-run.md`) gồm 5 file test của mục 3.2 cùng khung rỗng tối thiểu trong `src/main` để biên dịch được (ba file mới với thân `TODO()` hay giá trị sai có chủ đích, `BannerSlotHost` rỗng), như T-021; commit code thay khung rỗng.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerLoads.kt` | `BannerRefresh`, `bannerRefreshOf`, `BannerLoadResult`, `COLLAPSIBLE_BANNER_RELOAD_MILLIS`, `runBannerLoads` (mục 3.1 bước 1) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerFrame.kt` | `BannerFrame`, `bannerFrame`, `bannerShouldRun`, `Lifecycle.resumedFlow` (bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerAdView.kt` | `BannerAdView`, `bannerAdRequest`, `SdkBannerAdView` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `AdSlot` nhận hai định dạng banner, `BannerSlotHost`; bỏ `BannerAd`, `attachBannerMetrics` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `retryResets`, KDoc (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | KDoc của `AdFormat.BANNER`, `AdFormat.BANNER_COLLAPSIBLE` (bước 6) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerLoadsTest.kt` | Vòng gửi trên thời gian ảo (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerFrameTest.kt` | Bảng khung và lúc chạy (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/SdkBannerAdViewTest.kt` | Yêu cầu, kết quả, số liệu của lớp bọc `AdView`, Robolectric (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerSlotHostTest.kt` | Khung, tạm dừng, hủy, rút đồng ý, cờ tắt giữa chừng, Robolectric Compose (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeBannerAdView.kt` | Bản giả `BannerAdView`: ghi lượt gửi, `pause`, `resume`, `destroy`; phát kết quả theo lệnh; `Content` vẽ một `Box` có `testTag` khi `showAd` |
| Sửa | `docs/ops/ads-guide.md` | Mục 3, 5, 6, 7 (bước 7) |
| Sửa | `docs/features/ads/tickets/T-029-banner-theo-partner.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-029-banner-theo-partner.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, `src/testStaging`, `src/androidTest`, 2026-10-25):

- `AdPlacement.kt`: đọc bởi `scripts/release.sh` (`--list-placements`), `scripts/status.py`, `scripts/tests/run.sh` và `AdPlacementSourceTest.kt`; cả bốn chỉ đọc dòng khai báo vị trí `NAME("mã", AdFormat.X...)`. Task chỉ sửa KDoc của hai mục `AdFormat` (dòng `BANNER("banner"),` không khớp mẫu của script), không thêm, bớt hay đổi dòng vị trí nào, nên không ảnh hưởng.
- `AdSlot.kt`, `AdsController.kt`: không script, workflow hay test nào đọc theo tên file. Chữ `AdSlot` trong `NativeAdSlotTest.kt`, `CollapsibleNativeAdTest.kt` là một phần của `NativeAdSlot`, `CollapsibleNativeAdSlot`. Không nơi nào gọi `AdSlot(...)` ngoài chính file đó (`git grep`), nên đổi chữ ký nội bộ không chạm feature hay `:app`; chữ ký công khai `AdSlot(placement, modifier)` giữ nguyên.
- `docs/ops/ads-guide.md`: không script, workflow hay test nào đọc.
- Ảnh chuẩn screenshot: không ảnh nào đổi. Không screenshot test nào vẽ `AdSlot` (không có vị trí banner; test không có `LocalAdsController`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi định dạng và nhịp gửi yêu cầu (điểm chặn 1): banner thử lại khi lỗi (hôm nay không thử lại), banner thu gọn có cách vẽ và nhịp nạp lại tay 30 giây (hôm nay chưa có), giữ khung khi rút đồng ý (hôm nay co về 0). Không khóa Remote Config mới: banner dùng `is_show_<mã vị trí>` và `is_show_ads` có sẵn.
- Sự kiện phân tích: Không chạm. Lượt gửi, nạp được, lỗi, doanh thu, bấm của banner đi qua `AdTelemetry` có sẵn (T-023) như hôm nay; thử lại và nạp lại tay thêm lượt `ad_load_success`, `ad_load_fail` cùng tên, cùng tham số (khi `is_log_ad_slot_events` bật). Không thêm sự kiện hay tham số.
- Dữ liệu: không lưu gì trên máy; bộ đếm lỗi, `hadAd` chỉ trong bộ nhớ của khe.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `AdView`, `AdRequest`, `Lifecycle.currentStateFlow`, `UUID` không cần quyền ngoài `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có từ Google Mobile Ads.
- **Thư viện và SDK:** Không thêm. `AdView`, `AdRequest`, `AdSize`, `AdListener`, `LoadAdError` và `com.google.ads.mediation.admob.AdMobAdapter` thuộc `play-services-ads` (`libs.play.services.ads`, đã có trong `:core:ads`); `AdMobAdapter` có trên classpath biên dịch của `:core:ads` (chạy thử ở mục 4.1) và được luật R8 đi kèm SDK giữ (`-keep class * implements com.google.ads.mediation.MediationAdapter` trong `proguard.txt` của `play-services-ads` 25.5.0). `Lifecycle.currentStateFlow` thuộc `lifecycle-runtime` 2.9.2 (đến qua `libs.androidx.lifecycle.runtime.compose`, đã có). Test dùng Robolectric, `kotlinx-coroutines-test`, Compose UI test có sẵn trong test của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** vòng gửi nhận kết quả qua `MutableSharedFlow` không giữ giá trị cũ (mục 3.1 bước 3). Nếu khi cài, kết quả đến trước khi vòng kịp đăng ký (bản giả phát ngay trong `send()`), vòng chờ mãi lượt đầu; nếu dùng hàng đợi có đệm thì kết quả cũ từ trước khi cổng đóng có thể bị đọc nhầm là kết quả của lượt gửi mới.
- **Điều kiện chuyển:** `BannerLoadsTest.staleResultBeforeStart_isIgnored` hay `BannerSlotHostTest.revokedAfterAd_keepsHeight_hidesAd_pauses_reloadsWhenGranted` vẫn thất bại vì lượt gửi không nhận được kết quả, hoặc nhận kết quả cũ, sau 2 lần sửa.
- **Cách làm thay:** mỗi lượt `send()` trả một số thứ tự; `BannerLoadResult` mang số thứ tự của lượt gửi gần nhất lúc callback đến (`SdkBannerAdView` giữ số này); `results` là `Channel(UNLIMITED)` và vòng bỏ mọi kết quả có số thứ tự nhỏ hơn lượt nó vừa gửi. Cùng file `BannerLoads.kt`, `BannerAdView.kt`, `FakeBannerAdView.kt`; test và tiêu chí ở mục 4 giữ nguyên.

## 4. Kiểm thử

Mọi test ở `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/`. `BannerLoadsTest` theo cách của `NativeAdRetryTest`: `runTest`, `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))`, hàm `at(millis)` dời đồng hồ ảo, `FakeBannerAdView` ghi thời điểm mỗi lượt gửi (`sendTimes`), `visible` và `retryResets` là `MutableStateFlow`, `mayRequest` là biến. `BannerSlotHostTest` dùng `createComposeRule()`, `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi`, `heightDp = 62`, `testTag` trên `modifier` của khe, bản giả `FakeBannerAdView` (thẻ `fake_banner` khi `showAd`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Định dạng nào vẽ bằng `AdSlot` | Unit test | `BannerLoadsTest.refreshOf_bannerByAdUnit_collapsibleManual_othersThrow`: `BANNER` ra `BY_AD_UNIT`, `BANNER_COLLAPSIBLE` ra `MANUAL`, mọi định dạng khác của `AdFormat.entries` ném `IllegalArgumentException` |
| KB-18, tiêu chí 1 (app không tự tải lại) | Unit test | `BannerLoadsTest.byAdUnit_afterLoaded_appNeverSendsAgain`: `BY_AD_UNIT`, gửi ở 0, `Loaded` ở 500, bản giả phát thêm `Loaded` (lượt tự làm mới của SDK) ở 60 000 và 120 000; tới 600 000 `sendTimes == [0]`. Đối chứng (L-055): cùng kịch bản với `MANUAL` thì `sendTimes` có 30 500 |
| KB-18, tiêu chí 2 (nền: không thử lại), B-02, AR-03 | Unit test | `BannerLoadsTest.retryDueWhileHidden_waitsForNextVisible`: gửi ở 0, `Failed(3)` ở 0, `visible = false` ở 1 000; tới 9 000 không có lượt thứ hai (hạn là 2 000); `visible = true` ở 9 000 thì `sendTimes == [0, 9000]` |
| KB-18, tiêu chí 2 (nền: không tải lại), B-05 | Unit test | `BannerLoadsTest.manualReloadDueWhileHidden_waitsForNextVisible`: `MANUAL`, `Loaded` ở 0, `visible = false` ở 10 000; tới 50 000 `sendTimes == [0]`; `visible = true` ở 50 000 thì `[0, 50000]` |
| KB-18, tiêu chí 2 (lượt đầu chờ màn hiện) | Unit test | `BannerLoadsTest.firstSend_waitsUntilVisible`: `visible = false` lúc đầu; tới 60 000 không gửi; `visible = true` ở 60 000 thì `sendTimes == [60000]` |
| KB-18, tiêu chí 2 (SDK không tự làm mới ở nền) | Compose UI test (Robolectric) | `BannerSlotHostTest.hiddenScreen_pausesView_visibleAgain_resumes`: khung `ACTIVE`, `visible` từ `true` sang `false` thì `pauseCount == 1`, về `true` thì `resumeCount == 1`; không có `pause` hay `resume` nào lúc dựng khi `visible` đã `true` (đối chứng) |
| KB-18, tiêu chí 2 (`RESUMED` của Activity) | Compose UI test (Robolectric) | `BannerSlotHostTest.resumedFlow_followsActivityPauseAndResume`: `Robolectric.buildActivity(ComponentActivity::class.java).setup()`; `lifecycle.resumedFlow()` thu được `true`, sau `pause()` là `false`, sau `resume()` là `true` |
| KB-18, tiêu chí 3 (mỗi lần làm mới một banner bung mới) | Unit test (Robolectric) | `SdkBannerAdViewTest.collapsibleSends_haveBottomAnchorAndNewRequestIdEachTime`: `AdView(activity)` thật, `load` bắt `AdRequest`; hai lần `send()` với `MANUAL` thì cả hai `getNetworkExtrasBundle(AdMobAdapter::class.java)` có `collapsible == "bottom"`, hai `collapsible_request_id` khác nhau và không rỗng |
| KB-18, tiêu chí 3 (banner cố định không thu gọn) | Unit test (Robolectric) | `SdkBannerAdViewTest.fixedBannerRequest_hasNoCollapsibleExtras`: `BY_AD_UNIT`, `getNetworkExtrasBundle(AdMobAdapter::class.java)` là `null` hay không có khóa `collapsible` |
| KB-18, tiêu chí 3 và 6 (mỗi lượt nạp lại tay là một lượt gửi mới) | Unit test | `BannerLoadsTest.manual_reloadsEvery30sAfterLoaded`: `MANUAL`, `Loaded` 300 ms sau mỗi lượt gửi; tới 95 000 `sendTimes == [0, 30300, 60600, 90900]` |
| KB-18, tiêu chí 4 (đường lượt đầu) | Unit test | `BannerLoadsTest.firstSend_gateClosed_sendsNothingAndNeverRetries`: `mayRequest = false`; tới 120 000 `sendTimes` rỗng và có log `request blocked`; đối chứng: `mayRequest = true` thì `[0]` |
| KB-18, tiêu chí 4 (đường thử lại) | Unit test | `BannerLoadsTest.retry_gateClosedAtSend_sendsNothingAndStops`: `Failed` ở 0, `mayRequest = false` ở 1 000, `mayRequest = true` lại ở 5 000; tới 60 000 `sendTimes == [0]` (vòng đã kết thúc, không hẹn lại) |
| KB-18, tiêu chí 4 (đường nạp lại tay) | Unit test | `BannerLoadsTest.manualReload_gateClosedAtSend_sendsNothingAndStops`: `MANUAL`, `Loaded` ở 0, `mayRequest = false` ở 10 000; tới 120 000 `sendTimes == [0]` |
| KB-18, tiêu chí 4 (đường đồng ý mở lại) và tiêu chí 5 | Compose UI test (Robolectric) | `BannerSlotHostTest.revokedAfterAd_keepsHeight_hidesAd_pauses_reloadsWhenGranted`: cổng mở, `emitLoaded()`; đối chứng: khe cao 62 dp, có `fake_banner`, `sendCount == 1`. Đóng cổng: khe vẫn 62 dp, không còn `fake_banner`, `pauseCount == 1`, `destroyCount == 0`, `sendCount == 1`. Mở lại cổng: có `fake_banner`, `resumeCount == 1`, `sendCount == 2`, cùng một đối tượng giả (`createdCount == 1`) |
| KB-18, tiêu chí 4 (cổng đóng thì không dựng gì) | Compose UI test (Robolectric) | `BannerSlotHostTest.gateClosed_noFrame_noViewCreated`: cổng đóng lúc đầu; khe cao 0 dp, `createdCount == 0`. Đối chứng: mở cổng thì 62 dp, `createdCount == 1`, `sendCount == 1` |
| KB-18, tiêu chí 5 (cổng đóng khi chưa có quảng cáo) | Compose UI test (Robolectric) | `BannerSlotHostTest.revokedBeforeAnyAd_givesSpaceBack_andDestroys`: cổng mở, chưa có `Loaded`, khe 62 dp (đối chứng); đóng cổng thì 0 dp, `destroyCount == 1` |
| KB-18, tiêu chí 5 (bảng khung) | Unit test | `BannerFrameTest.frame_table`: chín dòng `(gateOpen, enabled, unitId, hadAd) -> frame`: `(F,T,"u",F) NONE`, `(T,T,"u",F) ACTIVE`, `(F,T,"u",T) KEPT_EMPTY`, `(T,T,"u",T) ACTIVE`, `(T,F,"u",T) NONE`, `(F,F,"u",T) NONE`, `(T,T,null,F) NONE`, `(T,T,"",F) NONE`, `(T,T,"  ",F) NONE` |
| KB-18, tiêu chí 5 (lúc chạy) | Unit test | `BannerFrameTest.shouldRun_onlyWhenVisibleAndActive`: chỉ `(true, ACTIVE)` ra `true`; `(true, KEPT_EMPTY)`, `(false, ACTIVE)`, `(true, NONE)` ra `false` |
| KB-18, tiêu chí 6 (30 giây, không dùng tự làm mới) | Unit test | `manual_reloadsEvery30sAfterLoaded` (trên) và `BannerLoadsTest.manual_failure_retriesThenBackTo30s`: `MANUAL`, gửi ở 0 lỗi, thử lại ở 2 000 thành công (`Loaded` ở 2 000), lượt sau ở 32 000 |
| B-02 (thử lại tăng dần, không bỏ cuộc) | Unit test | `BannerLoadsTest.failures_retryAt2_4_6_8_10_10s`: mỗi lượt gửi lỗi ngay; `sendTimes == [0, 2000, 6000, 12000, 20000, 30000, 40000]` |
| B-02 (nạp được thì đếm lại) | Unit test | `BannerLoadsTest.loaded_resetsFailureCount`: lỗi ở 0, thử lại ở 2 000 thành công; lượt tự làm mới của SDK lỗi ở 50 000 thì gửi lại ở 52 000 (không phải 54 000) |
| B-04 (mã rỗng) | Compose UI test (Robolectric) | `BannerSlotHostTest.blankUnit_noFrame_nothingSent`: `unitId = ""` rồi `"  "`: khe 0 dp, `createdCount == 0`; đối chứng `"u"` thì 62 dp |
| KB-24, tiêu chí 3 (mạng về thì thử lại ngay) | Unit test | `BannerLoadsTest.networkBack_cutsRetryWaitShort`: năm lần lỗi liên tiếp (lượt gửi ở 0, 2 000, 6 000, 12 000, 20 000), đang chờ 10 000; `retryResets` tăng ở 21 000 thì gửi ở 21 000 |
| Câu hỏi 5 (mạng về không rút ngắn 30 giây) | Unit test | `BannerLoadsTest.manual_networkBack_doesNotShortenReload`: `MANUAL`, `Loaded` ở 0, `retryResets` tăng ở 5 000; tới 29 999 `sendTimes == [0]`, tới 30 000 là `[0, 30000]` |
| L-068 (hai khe cùng vị trí) | Unit test | `BannerLoadsTest.twoSlotsSamePlacement_countFailuresApart`: hai vòng với hai bản giả cùng `placementKey`; khe A lỗi ở 0, 2 000, 6 000; khe B gửi lần đầu ở 6 000 và lỗi thì thử lại ở 8 000 (n = 1), không ở 14 000 |
| Kết quả cũ không bị đọc nhầm (mục 3.4) | Unit test | `BannerLoadsTest.staleResultBeforeStart_isIgnored`: bản giả phát `Failed(3)` trước khi vòng chạy; vòng gửi ở 0 rồi `Loaded` ở 100 ms; `MANUAL`: lượt sau ở 30 100, không có lượt thử lại ở 2 000 |
| Rời màn thì dừng | Unit test và Compose UI test | `BannerLoadsTest.cancelled_sendsNothingMore`: hủy vòng ở 1 000 khi đang chờ thử lại; tới 60 000 `sendTimes == [0]`. `BannerSlotHostTest.leavingComposition_destroysView`: gỡ khe khỏi cây giao diện thì `destroyCount == 1` |
| L-061 (khóa từ xa tắt giữa chừng, giữ tài nguyên theo khóa) | Compose UI test (Robolectric) | `BannerSlotHostTest.placementSwitchedOffMidway_givesSpaceBack_destroysOnce_sendsNoMore`: cổng mở, `emitLoaded()`, khe 62 dp (đối chứng); `enabled = false` (khóa vị trí hay `is_show_ads`, cùng một giá trị `isEnabled`) thì khe 0 dp, `destroyCount == 1`, `sendCount` không tăng khi `retryResets` đổi sau đó; bật lại thì `createdCount == 2`, `sendCount == 2` |
| Lượt gửi ghi số liệu, kết quả tới vòng gửi | Unit test (Robolectric) | `SdkBannerAdViewTest.listenerResults_reachResultsAndMetrics`: `telemetry` với `RecordingAdMetrics`, `is_log_ad_slot_events` bật; `send()` rồi gọi `adView.adListener.onAdFailedToLoad(LoadAdError(3, ...))`: `results` phát `Failed(3)`, có `AdMetricEvent.LoadFail`; `send()` rồi `onAdLoaded()`: phát `Loaded`, có `AdMetricEvent.LoadSuccess` với `load_attempt` 2 |
| Banner thật trên máy: tự làm mới, bung lại mỗi 30 giây, giữ khung khi rút đồng ý, tạm dừng ở nền | Kiểm tra thủ công (máy thật: quảng cáo thật; chỉ làm được khi có vị trí banner trên một màn hình, T-030 hay T-036, câu hỏi 1) | Bản staging, log `adb logcat -v threadtime \| grep ' VGL\.'`: (1) banner cố định: có quảng cáo, không có dòng `Banner ... reload`; (2) banner thu gọn: bung ra, sau 30 giây có `Banner <mã> reload in 30000 ms` và bung ra lại; xuống nền 60 giây rồi lên thì không có lượt gửi nào trong lúc ở nền; (3) chế độ máy bay khi đang hiện: `Banner <mã> retry in ...` tăng tới 10 000 ms, tắt máy bay thì có quảng cáo; (4) mở nút quyền riêng tư, rút đồng ý: khung giữ chiều cao, quảng cáo biến mất, không có lượt gửi; đồng ý lại thì quảng cáo về trong khung |

Mỗi lượt gửi ở mọi đường (L-072): lượt đầu, thử lại, nạp lại tay đi qua `sendWhenAllowed()` của `runBannerLoads` và có test riêng cho cả màn không hiện lẫn cổng đóng ở bảng trên; lượt khi đồng ý mở lại là lượt đầu của một vòng mới (`revokedAfterAd_...`); lượt tự làm mới của SDK không do app gửi và bị chặn bằng `pause()` (`hiddenScreen_...`, `revokedAfterAd_...`).

**Ngoại lệ viết test trước:** phần nối trong `AdSlot` (đọc `canRequestAds`, `isEnabled`, `unitId`, tính `AdSize`, lấy `LocalLifecycleOwner`, tạo `AdView` bằng `LocalContext` và `SdkBannerAdView`, truyền `ads::mayRequestNow`, `ads.retryResets`): cần `AdsController` thật, không dựng được trên JVM (như `NativeAdSlot` của T-010, banner của T-023). Mọi quyết định (khung, lúc chạy, cổng ngay trước khi gửi, màn hiện) nằm trong hàm và composable có test ở trên, nên phần nối chỉ truyền giá trị. KDoc và `docs/ops/ads-guide.md`: tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-25, trên nhánh chính `bf7a671`, một test tạm không commit `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/TmpBannerDryRunTest.kt` (đã xóa) gồm 5 test, gọi đúng các lớp và API mới của plan: (1) dựng `AdRequest` với `addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle)` rồi đọc lại bằng `getNetworkExtrasBundle`: `collapsible == "bottom"`, hai yêu cầu có hai `collapsible_request_id` khác nhau; yêu cầu thường trả `null`; (2) tạo `AdView(activity)` thật trong Robolectric, `setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 411))`, `adUnitId` là mã thử của Google, gọi `isCollapsible`, `pause()`, `resume()`, `destroy()`: không lỗi (cỡ đo được 411 x 128 dp trên Robolectric, khác máy thật; test của plan không dựa vào cỡ này); (3) `ComponentActivity.lifecycle.currentStateFlow` đổi đúng theo `pause()`, `resume()` của Robolectric; (4) `AndroidView` có `update` đặt `View.INVISIBLE`: khung giữ 62 dp trước và sau khi ẩn; (5) vòng `delay` cộng `StateFlow.first { it }` trên `runTest`: lượt tới hạn khi ẩn được gửi đúng lúc hiện lại (5 000).
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TmpBannerDryRunTest*'` (5 test qua, 4,4 giây), rồi `scripts/check.sh` với test tạm trong cây (JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`).
- **Kết quả:** `scripts/check.sh` qua, "Cổng chất lượng: QUA": 661 test JVM, 0 lỗi (`:core:ads` 380, tức 375 có sẵn cộng 5 test tạm; kết quả `TEST-...TmpBannerDryRunTest.xml` 5 test, 0 lỗi, ghi lúc 08:35 UTC trong lần chạy này); 42 ảnh screenshot, không ảnh nào đổi; lint qua. `AdMobAdapter` có trên classpath biên dịch của test `:core:ads`, nên cũng có ở `src/main` (cùng phụ thuộc `implementation(libs.play.services.ads)`). Không thêm thư viện hay module nên không chạy `:app:assembleStagingRelease` (L-083); luật R8 giữ `AdMobAdapter` đã có trong `proguard.txt` của SDK (mục 3.3). Phương án dự phòng 3.4 chưa thử: điểm đó chỉ lộ ra khi cài vòng gửi thật.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi cách vẽ và nhịp gửi yêu cầu của định dạng banner; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì task đổi số lượt gửi yêu cầu banner.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Ba file code mới, ba file sửa, năm file test trong một module; `AdSlot` công khai nhận thêm một định dạng |
| B. Độ rõ của yêu cầu | 1 | KB-18 và B-01 tới B-06 rõ, câu hỏi 25 đã trả lời; AI tự quyết vài chi tiết (ẩn quảng cáo khi giữ khung, mốc tính 30 giây, lỗi của lượt tự làm mới, mạng về), ghi ở câu hỏi 2 tới 5 |
| C. Khả năng kiểm chứng tự động | 2 | Mọi luật có test JVM với thời gian ảo và bản giả; phần SDK thật (tự làm mới của AdMob, banner bung ra, ẩn lớp bung khi rút đồng ý) chỉ kiểm được trên máy thật, và hôm nay chưa có màn hình nào có vị trí banner (câu hỏi 1) |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `AdPlacement.kt`, `docs/ops/ads-guide.md` cũng bị T-024 và T-038 sửa; T-030 sẽ dựng thanh đáy trên `AdSlot` |
| E. Độ mới của kỹ thuật | 2 | Banner thu gọn (phần thêm `AdMobAdapter`) và `Lifecycle.currentStateFlow` lần đầu dùng trong dự án; đã chạy thử bằng `scripts/check.sh` (mục 4.1). Phần còn lại lặp lại mẫu khe native và vòng thử lại của T-021 |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdSlot.kt`, `AdsController.kt`, `AdPlacement.kt` (chỉ KDoc của `AdFormat`) của `:core:ads`; `docs/ops/ads-guide.md` mục 3, 5, 6, 7. Ảnh chuẩn screenshot, `tokens.json`, bảng khóa `docs/remote-config-keys.md` không bị ảnh hưởng (không đổi giao diện có ảnh, không thêm khóa hay vị trí).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **T-024 (app open ở màn loading, plan đang viết song song, nhánh `claude/plan-T-024-app-open`):** gần như chắc sửa `AdsController.kt` (nạp và hiện app open), có thể sửa `AdPlacement.kt` (KDoc của `AdFormat.APP_OPEN` ghi "No function shows it yet (T-024, T-025)") và `docs/ops/ads-guide.md` (mục 3 dòng app open, mục 6). Không file nào cả hai plan cùng **tạo** (L-054): T-029 chỉ tạo `Banner*.kt` và `FakeBannerAdView.kt`. Chỗ sửa chung khác dòng: T-029 chỉ thêm một thuộc tính `retryResets` và một câu KDoc trong `AdsController.kt`, chỉ đổi KDoc của `BANNER`, `BANNER_COLLAPSIBLE` trong `AdPlacement.kt`, chỉ đổi dòng banner của `ads-guide.md`. Task chạy sau gộp nhánh chính rồi sửa xung đột theo chữ của task chạy trước, không đổi hành vi của nó. Nếu plan T-024 cũng cần tín hiệu mạng về cho app open, hai plan thống nhất một tên (`retryResets`) và một plan ghi "Sửa", plan kia ghi "dùng lại".
- **T-038 (xóa lượt giữ khi nạp sẵn chạy, plan `draft` trên nhánh `claude/plan-T-038-xoa-luot-giu`):** sửa `AdPreloader.kt` và `ads-guide.md` mục 6 (gạch "Lúc chạy" và gạch log của "Nạp sẵn và nạp lại"). T-029 không sửa `AdPreloader.kt` (chỉ đọc `nativeRetryResets` qua `AdsController`) và sửa gạch banner của mục 6, khác gạch.
- **T-030 (thanh đáy), T-036 (màn xem thử):** phụ thuộc T-029; dùng `AdSlot` mới, khai báo vị trí banner (câu hỏi 1).
- Đề xuất thứ tự chạy: sau T-038 nếu cùng phiên (ít dòng chung trong `ads-guide.md`); với T-024 thì thứ tự nào cũng được, task chạy sau gộp nhánh chính trước khi viết code.

## 7. Điều kiện dừng riêng

- Cần thêm một vị trí banner vào `AdPlacement` (kể cả vị trí `gallery_*`) hay sửa màn xem thử, `:feature:sample`: ngoài mục 3.2 và là điểm chặn 1; dừng, chờ câu hỏi 1.
- `AdView` thật trong `SdkBannerAdViewTest` ném lỗi khi gọi `adListener` hay `destroy()` trên Robolectric (mục 4.1 chỉ thử `pause`, `resume`, `destroy` khi chưa nạp): không đổi test sang bản giả; dừng và báo.
- Cần đổi `AdTelemetry`, `LoadAttemptCounter` hay thêm sự kiện để ghi lượt thử lại của banner: ngoài phạm vi (điểm chặn 4).
- Vòng gửi cần đọc trạng thái foreground của `AdsActivityTracker` thay cho `RESUMED` của màn hình (ví dụ để chặn gửi khi Activity khác của app che màn): đổi cách hiểu tiêu chí 2, dừng và hỏi.

## 8. Câu hỏi cho người duyệt

1. **Thấy banner trên máy ở đâu?** `AdPlacement` không có vị trí banner nào (câu hỏi 3 của spec đã bỏ `sample_bottom_banner`), nên sau task này chưa màn hình nào hiện banner; dòng kiểm tra thủ công ở mục 4 chưa làm được.
   - Đề xuất của AI: task này không thêm vị trí (thêm vị trí là điểm chặn 1 và là phạm vi của T-036: "vị trí `gallery_*` cho định dạng mới"). T-036 thêm `gallery_banner` (`banner`) và `gallery_banner_collapsible` (`banner_collapsible`) cùng một trang xem thử; banner cố định cũng thấy ở thanh đáy của màn mẫu khi T-030 xong và `is_show_native_bottom` tắt. Kiểm thủ công của mục 4 làm ở phiên của T-036 (ghi vào ticket T-036, phụ thuộc thêm T-029). Phương án khác: thêm hai vị trí `gallery_*` và một trang "Banner" vào màn xem thử ngay trong task này (thêm khoảng 6 file của `app/src/staging`, `app/src/testStaging`, một ảnh chuẩn, không có ảnh thiết kế), đổi lại kiểm được trên máy ngay sau phiên.
2. **Rút đồng ý khi banner đang có quảng cáo: giữ khung thì quảng cáo cũ còn hiện không?** B-06 chỉ nói giữ khung, dừng gửi, tạm dừng tự làm mới; KB-12 và S-11 nói quảng cáo đã tải không hiện nữa sau khi rút.
   - Đề xuất của AI: giữ khung, ẩn quảng cáo cũ (`INVISIBLE`, khung trống đúng chiều cao), như mục 3.1 bước 4. Đồng ý lại thì hiện lại và gửi lượt mới trong khung đó.
3. **Nhịp 30 giây của banner thu gọn tính từ đâu, và lượt tới hạn khi app ở nền thì sao?**
   - Đề xuất của AI: tính từ lúc lượt trước nạp xong (`Loaded`), không từ lúc gửi; lỗi thì thử lại theo B-02, nạp được thì lại chờ 30 giây. Tới hạn khi màn không hiện thì gửi ngay khi màn hiện lại (B-05 chỉ cấm gửi ở nền), nên quay lại app có thể thấy banner bung ra ngay. Nếu anh muốn tránh bung ngay khi quay lại, phương án khác là đếm lại đủ 30 giây sau khi màn hiện lại.
4. **Lượt tự làm mới của SDK lỗi (banner cố định đang hiện quảng cáo cũ): app có thử lại không?** B-02 ghi "lỗi thì thử lại" mà không tách lượt của app với lượt của SDK.
   - Đề xuất của AI: có, mọi `onAdFailedToLoad` đều bắt đầu thử lại `min(n x 2 giây, 10 giây)` (gọi lại `loadAd`, đặt lại nhịp tự làm mới của SDK). Lượt thử lại thành công thì SDK tự làm mới tiếp theo nhịp của đơn vị.
5. **Mạng về có rút ngắn chờ của banner không?** KB-24 tiêu chí 3: "mọi loại quảng cáo tải lại"; banner chờ thử lại tối đa 10 giây.
   - Đề xuất của AI: có cho lượt thử lại (dùng chung tín hiệu mạng về của khe native, S-24); không cho nhịp 30 giây của banner thu gọn (banner đang có quảng cáo, không cần tải lại).
6. **Lượt nạp đầu lỗi (chưa từng có quảng cáo): khung giữ chiều cao hay co về 0?** Hôm nay khung giữ chiều cao từ lúc dựng tới khi rời màn. B-04 chỉ nói mã rỗng thì không chiếm chỗ; D-02 nói banner trong thanh đáy "cao theo nội dung" (T-030).
   - Đề xuất của AI: giữ như hôm nay ở task này (không nhảy giao diện khi thử lại có quảng cáo); T-030 quyết cho thanh đáy theo D-02, D-04.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
