---
ticket: T-027
title: Native một quảng cáo theo kích thước AdsKit: khe đáy, đầy đủ, toàn màn hình
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 30M          # cỡ L, số gợi ý 30.0M (scripts/token-usage.py estimate, 2026-10-25, tạm vì chưa đo task L nào); tách thành hai task thì mỗi phần khoảng 15M (mục 8, câu 1)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-027: NATIVE MỘT QUẢNG CÁO THEO KÍCH THƯỚC ADSKIT: KHE ĐÁY, ĐẦY ĐỦ, TOÀN MÀN HÌNH

## 1. Tóm tắt

Đổi native nhỏ của `:core:ads` sang khe đáy 80 dp của AdsKit (một hằng số cho chiều cao vẽ và chiều cao chừa chỗ), thêm biến thể native đầy đủ, đổi số đo native toàn màn hình theo thiết kế vẽ lại (T-035); thêm nguồn một quảng cáo cho mỗi vị trí (lấy, hủy khi rời màn, hủy quảng cáo về muộn, hết hạn 55 phút, thử lại khi hai chỗ cùng chờ) và thay nội dung tại chỗ mỗi 60 giây ở foreground; tùy chọn yêu cầu và nhãn theo O-06 tới O-09. Kiểm chứng bằng unit test các hàm thuần với đồng hồ giả, test giao diện Robolectric đo kích thước, và ghi lại ảnh chuẩn screenshot của T-010, T-013 và các màn hình có native nhỏ.

Ticket cỡ L: đề xuất tách thành hai task (mục 8, câu 1): phần A giao diện (mục 3.1, phần A), phần B nguồn một quảng cáo và làm mới (mục 3.1, phần B). Plan viết cả hai phần để người duyệt chọn.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (luật chi tiết: `docs/ops/ads-business-rules.md`, mục 5.6, 5.7, 11):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices; vùng quảng cáo có nền hoặc viền tách khỏi nội dung; CTA khác nút chính của app. | Giữ như T-010; đổi số đo theo 5.7: icon 36 dp, tiêu đề 14 sp, mô tả 12 sp, CTA cao 34 dp chữ 13 sp, nhãn "Ad" 10 sp, không có media. AdChoices góc trên phải (O-08). |
| KB-1 | Khi native nhỏ ở đáy thì vùng quảng cáo cao 80 dp; khi đang tải thì giữ đúng chiều cao đó với hiệu ứng chờ. | `NativeAdDimens.BottomSlotHeight = 80.dp` dùng cho thẻ, khung chờ và khoảng giữ chỗ (D-02, D-04, câu hỏi 22 của spec). Native nhỏ chỉ có một cỡ trong dự án mẫu, nên cả native nhỏ trong danh sách và dạng thu gọn của native thu gọn cũng thành 80 dp (mục 8, câu 4). |
| KB-1 | Khi nạp lỗi thì vùng quảng cáo co về 0 dp, nội dung không chừa khoảng trống cho nó. | Đã có từ T-021 (`Unavailable` không vẽ gì, trừ `keepSpaceWhenEmpty`). Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit` để báo app "có"/"không có" quảng cáo (D-02); T-030 dùng để chừa chỗ (D-04). |
| KB-1 | Màu lấy từ hệ thống thiết kế; chữ quảng cáo luôn đọc được trên nền của vùng quảng cáo, kể cả khi máy ở chế độ tối. | Màu theo token như T-010 (`docs/features/ads/design/README.md`, mục 3). Test đo màu chữ tiêu đề và CTA trên nền thẻ ở cả hai chế độ (vẽ `decorView` ra `Bitmap`, CLAUDE.md bước 10). |
| KB-1 | Khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ. | Phần B: sau mỗi lần hiện quảng cáo, chờ 60 giây (chỉ đếm khi app ở foreground), qua cổng đồng ý thì nạp quảng cáo mới; có thì thay và hủy cái cũ; lỗi thì giữ cái cũ và thử lại theo nhánh "còn giữ quảng cáo" của O-05 (2, 4, 8, 16, 32 rồi 60 giây). Áp cho native nhỏ ở khe một quảng cáo (`NativeAdSlot` với `NATIVE_SMALL`); medium không làm mới (5.7). Trễ 500 ms sau lớp thu gọn là việc của T-030 (hằng số đặt ở task này). |
| KB-1, trường hợp đặc biệt | Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ. | Đã có (T-010, T-020); giữ, có test hồi quy. Lượt làm mới cũng qua cổng đồng ý ngay trước khi gửi (O-05, S-15). |
| KB-1, trường hợp đặc biệt | Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ. | O-01, O-03: lượt hiện sở hữu quảng cáo; rời màn thì hủy cả quảng cáo đang hiện, quảng cáo mới đang về (về muộn thì hủy ngay) và không hẹn thử lại. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt. | Áp cho native đầy đủ (media cao 200 dp, icon 40 dp, tiêu đề 14 sp, cao theo nội dung) và toàn màn hình. Cách co giãn ảnh đặt trước khi gắn quảng cáo (O-07). Native medium giữ số đo hiện tại; số đo mới của medium (CTA 44 dp, hàng sao, `cta_large`) đi với pool ở T-028 (mục 8, câu 3). |
| KB-2 | Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1. | Như các dòng KB-1 ở trên, cho native đầy đủ: khung chờ giữ chiều cao của thẻ, lỗi thì co về 0. |
| KB-2 | Khi hai ô medium cùng hiện trên màn thì mỗi ô có một quảng cáo khác nhau. | Không thuộc task này: medium lấy từ pool (T-028). Khe một quảng cáo của task này bảo đảm mỗi lượt hiện một quảng cáo riêng (O-01), nên hai khe cùng vị trí cũng không dùng chung một quảng cáo. |

Luật của ticket, ngoài tiêu chí chấp nhận (mỗi luật có test ở mục 4):

- O-01: mỗi lượt hiện sở hữu riêng một quảng cáo; lấy ra thì nguồn tự nạp cái mới; rời màn thì hủy.
- O-02: hai chỗ cùng chờ một lượt nạp: chỗ thua thử lại một lần, không coi là hết quảng cáo.
- O-03: nơi hiện rời màn khi yêu cầu đang bay: quảng cáo về sau đó thì hủy ngay; lỗi về sau đó không hẹn thử lại; chỗ đang chờ nhận "không có quảng cáo" ngay; nguồn không còn ở trạng thái "đang nạp".
- O-04: native hết hạn từ đủ 55 phút sau khi nạp; quảng cáo chờ đã hết hạn thì bỏ, không đổi vào. Áp cả cho native toàn màn hình nạp sẵn (`FreshAdCache` của `AdsController`, nay 60 phút).
- O-05: nhánh "còn giữ quảng cáo" của `AdRetryPolicy.nativeDelayMillis(n, holdsAd = true)` (T-021 đã có hàm thuần) dùng cho lượt làm mới lỗi; bộ đếm về 0 khi thành công.
- O-06: tùy chọn yêu cầu: video tắt tiếng, không có điều khiển riêng, một ảnh, media ngang.
- O-07: đặt cách co giãn ảnh trước khi gắn quảng cáo vào khung.
- O-08: nhãn "Ad" luôn thấy được; AdChoices do SDK ghim ở góc trên phải, không bị đè; hàng sao chỉ hiện khi điểm lớn hơn 0, sao đặc làm tròn theo điểm, tối đa 5, không vẽ nửa sao.
- O-09: chuẩn bị yêu cầu lỗi tính là lỗi mã 0 (đã có ở `nativeLoadOnce`, T-021); giữ, thêm test cho lượt làm mới.
- D-02, D-04, AR-02: 80 dp cho cả khe và chừa chỗ, một hằng số; test so hai giá trị.

Test nghiệm thu gợi ý `AT-NT-*` thuộc bản đặc tả độc lập của AdsKit, không có trong repo (như T-021 đã ghi); plan dùng các luật trên làm danh sách test.

Thiết kế tham chiếu: `features/ads/design/README.md` (vẽ lại T-035, 2026-10-25), các ảnh trong `design/screens/`: `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light` (khe đáy 80 dp), `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` (toàn màn hình), `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark` (có native nhỏ). Số liệu lấy từ `design/context/` cùng tên. Native đầy đủ chưa có ảnh màn hình (mục 8, câu 2).

ADR liên quan: ADR-0004, ADR-0008, ADR-0013, ADR-0019, ADR-0020, ADR-0022, ADR-0026.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn của `:core:ads`: composable không trạng thái nhận `NativeAdAssets` (`NativeAdCard`, T-010), luồng trạng thái `Flow<NativeAdState>` gom trong composition (`NativeAdSlotHost`), hàm thuần cho mọi luật thời gian với đồng hồ giả (`AdRetryPolicy`, `retryingNativeAdStates`, T-021).

**Phần A. Giao diện theo kích thước AdsKit**

1. `NativeAdCard.kt`:
   - `enum class NativeAdSize { SMALL, MEDIUM, FULL }`: thêm `FULL` (native đầy đủ). Mọi `when` trên `NativeAdSize` thêm nhánh `FULL` (`NativeAdDimens.heightOf`, `NativeAdLayout`, `NativeAdPlaceholderLayout`, `SdkNativeAdLoader`). Chạy thử L-089 ở mục 4.1.
   - `NativeAdDimens`: `BottomSlotHeight = 80.dp` (một hằng số; `SmallHeight` đổi tên thành `BottomSlotHeight` hoặc trỏ vào nó), `SmallIconSize = 36.dp`, `SmallCallToActionHeight = 34.dp`, `FullMediaHeight = 200.dp`, `FullIconSize = 40.dp`. Cỡ chữ ngoài bảng kiểu chữ (13 sp CTA nhỏ, 10 sp nhãn "Ad") là hằng số riêng của composable quảng cáo, dựng bằng `MaterialTheme.typography.<kiểu>.copy(fontSize = ...)` (README thiết kế, đoạn T-035). Màu giữ theo token (README, mục 3).
   - `SmallLayout`: theo `design/context/SampleScreen_ready_light.md`: hàng ngang, lề trái 12, lề phải 24, icon 36, khối chữ (tiêu đề `titleSmall` 14 sp, dòng nhãn "Ad" 10 sp và mô tả `bodySmall` 12 sp cách 2 dp), CTA 34 dp chữ 13 sp, AdChoices góc trên phải.
   - `FullLayout` (mới): cột: đầu (icon 40, tiêu đề 14 sp, dòng nhãn), media cao 200 dp (`NativeAdMediaArea`), mô tả, CTA; cao theo nội dung; hàng sao theo O-08.
   - `NativeAdPlaceholderLayout`: khung chờ của `SMALL` cao 80 dp; của `FULL` theo cùng bố cục của thẻ (chiều cao cố định `FullPlaceholderHeight`, đo từ thẻ với quảng cáo giả trong test).
   - Hàm thuần `nativeStarCount(rating: Double?): Int` (O-08): `null`, `<= 0` hay NaN trả 0; còn lại `round(rating)` kẹp trong 1..5. `NativeAdAssets` thêm trường `starRating: Double? = null` (giá trị mặc định, nên nơi gọi cũ không đổi); `SdkLoadedNativeAd` đọc `nativeAd.starRating`.
2. `NativeAdSlot.kt`: `requireNativeFormat` giữ `NATIVE_SMALL -> SMALL`, `NATIVE_MEDIUM -> MEDIUM`. Native đầy đủ chưa có định dạng trong `AdFormat` (MC-02 là danh sách đóng, không có `native_full`; mục 8, câu 2): task này chỉ thêm composable `NativeAdCard(assets, NativeAdSize.FULL)` và khung chờ, chưa có vị trí nào hiện nó. `Unavailable` với `keepSpaceWhenEmpty` giữ `BottomSlotHeight`. Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit = {}` gọi khi trạng thái chuyển sang `Loaded` (true) hay `Unavailable` (false), không gọi ở `Loading` (D-02).
3. `NativeFullScreenAdScreen.kt`: số đo theo `design/context/NativeFullScreenAdScreen_default_light.md`: nút đóng 36 dp tròn ở góc trên trái (biểu tượng 24 dp; README mục 3 cũ ghi 48 dp, ảnh vẽ lại ghi 36 dp, KB-16 ghi 36 dp), icon 48 dp, tiêu đề `titleMedium` 16 sp, CTA cao 52 dp, AdChoices góc trên phải. Đếm ngược 3 giây và nút đóng hiện sau đếm ngược là của T-026, không làm ở đây. Vùng chạm của nút đóng: 36 dp nhỏ hơn 48 dp của quy tắc chung; plan giữ vùng vẽ 36 dp và nới vùng chạm lên 48 dp bằng `minimumInteractiveComponentSize()` (mục 8, câu 5).
4. `SdkNativeAdLoader.kt` (O-06): hàm thuần `nativeRequestOptions(size: NativeAdSize): NativeRequestOptions` (data class: `startMuted = true`, `customControls = false`, `multipleImages = false`, `mediaAspect = LANDSCAPE` cho `MEDIUM`, `FULL`; `SMALL` không xin media nên `ANY`), rồi `load` chép vào `NativeAdOptions.Builder` và `VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(false)`. AdChoices giữ `ADCHOICES_TOP_RIGHT`.
5. `NativeAdViewBinding.kt` (O-07): `MediaView` đặt `setImageScaleType(FIT_CENTER)` trong `factory` trước khi `setNativeAd` chạy (hôm nay đã như vậy); thêm test thứ tự bằng bộ ghi lời gọi để giữ luật, không đổi code nếu test qua ngay (ghi ở ngoại lệ viết test trước).
6. `res/values/strings.xml` của `:core:ads`: chuỗi mô tả hàng sao cho trợ năng (`ads_native_star_rating`, "%1$d of 5 stars"); nhãn "Ad" đã có.
7. Ảnh chuẩn: ghi lại ảnh có native nhỏ, native toàn màn hình (danh sách đủ ở mục 3.2, theo kết quả chạy thử ở mục 4.1).

**Phần B. Nguồn một quảng cáo và làm mới tại chỗ**

1. `NativeAdSource.kt` (mới, internal): nguồn một quảng cáo của một vị trí, chạy trên luồng chính, nhận `load: suspend () -> NativeAdState`, `clock: MonotonicClock`, `logger`.
   - `suspend fun claim(): NativeAdState`: có quảng cáo chờ còn hạn (dưới 55 phút) thì trả và bắt đầu nạp cái mới (O-01); quảng cáo chờ hết hạn thì hủy, không trả (O-04); đang nạp thì chờ lượt đó; hai chỗ cùng chờ một lượt: chỗ thắng lấy quảng cáo, chỗ thua chờ đúng một lượt nạp nữa rồi mới nhận `Unavailable` (O-02).
   - Chỗ chờ bị hủy (rời màn) khi yêu cầu đang bay: chỗ đó nhận `Unavailable` ngay qua hủy coroutine; quảng cáo về sau đó nếu không còn chỗ chờ và nguồn đã đóng thì hủy ngay; lỗi về sau đó không hẹn thử lại; nguồn không còn ở trạng thái "đang nạp" (O-03).
   - `close()`: hủy quảng cáo chờ, bỏ lượt đang bay (quảng cáo về muộn bị hủy).
   - Một nguồn mỗi vị trí, giữ trong `AdsController` (`nativeSources: Map<AdPlacement, NativeAdSource>`), tạo khi khe đầu tiên của vị trí lên màn; đóng khi vị trí bị tắt hay đồng ý bị rút (theo `isEnabled`, `canRequestAds`), để quảng cáo không bị giữ khi cờ tắt giữa chừng (L-061).
   - Hằng số `NativeAdPolicy.MAX_AGE_MILLIS = 55 * 60 * 1_000L`, `REFRESH_INTERVAL_MILLIS = 60_000L`, `BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS = 500L` (mục 11 của luật).
2. `NativeAdRefresh.kt` (mới): `refreshingNativeAdStates(first: NativeAdState.Loaded, load, foreground, mayRequestNow, retryResets, clock, logger, placementKey): Flow<NativeAdState>`: phát quảng cáo đầu; đếm 60 giây chỉ khi `foreground` là true (vào nền thì dừng đếm, giữ thời gian đã đếm); hết 60 giây thì kiểm `mayRequestNow` ngay trước khi gửi (đóng thì không gửi, chờ chu kỳ sau); nạp được thì phát quảng cáo mới rồi hủy cái cũ; lỗi (cả lỗi mã 0, O-09) thì giữ quảng cáo cũ, thử lại sau `AdRetryPolicy.nativeDelayMillis(failures, holdsAd = true)`; thành công thì bộ đếm về 0. Hủy collection thì hủy quảng cáo đang hiện và quảng cáo đang về.
3. `NativeAdRetry.kt`: `retryingNativeAdStates` nhận thêm `refresh: Boolean`; khi `Loaded` và `refresh` thì nối sang `refreshingNativeAdStates` thay vì `awaitCancellation()`. Không đổi nhánh 5 giây khi chưa giữ quảng cáo.
4. `AdsController.kt`: `nativeAd(placement)` lấy quảng cáo qua `NativeAdSource.claim()`, bật `refresh` cho `NATIVE_SMALL`; `FreshAdCache` của native toàn màn hình đặt `maxAgeMillis = NativeAdPolicy.MAX_AGE_MILLIS` (O-04). `loadNativeAd` của danh sách (`rememberInListAds`) giữ nguyên tới T-028.
5. `FreshAdCache.kt`: chỉ đổi chú thích (giá trị mặc định 60 phút giữ cho nơi khác; native truyền 55 phút).
6. Ghi log bằng `AppLogger`, thẻ `Ads`: `Native <key> claimed`, `Native <key> refresh: loaded|failed <code>|blocked`, `Native <key> expired`, `Native <key> late ad destroyed`, `Native <key> claim lost, retry once`. Không ghi mã đơn vị.
7. `docs/ops/ads-guide.md`: mục native: khe đáy 80 dp, một hằng số; làm mới 60 giây; tham số `onAdAvailabilityChange`; native đầy đủ chưa có định dạng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | A1: `NativeAdSize.FULL`, `NativeAdDimens` theo 5.7, `SmallLayout`, `FullLayout`, khung chờ, `nativeStarCount`, `NativeAdAssets.starRating` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | A2: `onAdAvailabilityChange`, giữ chỗ bằng `BottomSlotHeight` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | A3: số đo theo thiết kế vẽ lại |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | A4: `nativeRequestOptions`, `VideoOptions`; nhánh `FULL`; `starRating` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | A5: chỉ khi test thứ tự O-07 thất bại; hàng sao gắn vào `starRatingView` nếu SDK cần |
| Sửa | `core/ads/src/main/res/values/strings.xml` | A6: `ads_native_star_rating` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSource.kt` | B1: nguồn một quảng cáo, `NativeAdPolicy` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRefresh.kt` | B2: `refreshingNativeAdStates` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | B3: nối sang làm mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | B4: `nativeSources`, `nativeAd` qua nguồn, 55 phút cho native toàn màn hình |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FreshAdCache.kt` | B5: chú thích |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdLoading.kt` | B1: `loadOneNativeAd` trả quảng cáo về muộn cho nguồn hủy (O-03), nếu cần tách khỏi `NativeAdRequest` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | Chỉ khi dạng thu gọn vẽ bằng hằng số riêng thay vì `NativeAdDimens.BottomSlotHeight` (mục 8, câu 4); không đổi hành vi thu gọn (T-030) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Chỉ khi khoảng giữ chỗ của dòng quảng cáo dùng hằng số 72 dp riêng |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Bước 3: 72 dp thành 80 dp (`small_is72dpHigh` đổi tên `small_is80dpHigh`), số đo icon, CTA, cỡ chữ; test `FULL`; màu chữ hai chế độ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Bước 3: 72 dp thành 80 dp; `onAdAvailabilityChange`; hằng số chừa chỗ bằng chiều cao vẽ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Bước 3: 72 dp thành 80 dp; thứ tự O-07 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Bước 3: dạng thu gọn 72 thành 80 (sáu chỗ `assertHeight(AD, 72f)`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Bước 3: `loadingSlot_showsPlaceholderOfSmallHeight` 72 thành 80 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Bước 3: CTA 48 thành 52 dp, nút đóng vẽ 36 dp, vùng chạm ít nhất 48 dp, icon 48 dp |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Thêm ảnh `NativeAdCard_full_light`, `NativeAdCard_full_dark`, `NativeAdPlaceholder_small_light` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | Quảng cáo giả có `starRating`, nguồn giả đếm lượt hủy |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourceTest.kt` | Test O-01 tới O-04 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRefreshTest.kt` | Test làm mới 60 giây, O-05 nhánh giữ quảng cáo, O-09 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRequestOptionsTest.kt` | Test O-06 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStarCountTest.kt` | Test O-08, hàng sao |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | Nối sang làm mới khi `refresh` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FreshAdCacheTest.kt` | 55 phút cho native |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | Khe đáy 80 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_light.png` | Native đầy đủ |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdPlaceholder_small_light.png` | Khung chờ 80 dp, chụp với `animated = false` |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_light.png` | Số đo mới |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_collapsed_light.png` | Dạng thu gọn 80 dp (theo mục 4.1) |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_collapsed_dark.png` | Như trên |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` | Khe đáy 80 dp |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_dark.png` | Như trên |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png` | Như trên |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_small.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_light.png` | Native nhỏ 80 dp |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_dark.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_small.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_inlist_light.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_inlist_small.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Dạng thu gọn 80 dp (theo mục 4.1) |
| Sửa | `docs/ops/ads-guide.md` | B7 |
| Sửa | `docs/features/ads/design/README.md` | Mục 8: kết quả đối chiếu của `SampleScreen_*`, `NativeFullScreenAdScreen_*` |

Không sửa: `feature/sample/src/main/.../SampleScreen.kt` và `feature/sample/src/test/.../SampleScreenScreenshotTest.kt` dùng `NativeAdSize.SMALL`, `NativeAdPlaceholder(NativeAdSize.SMALL, ...)` qua tên, không `when` trên enum, nên thêm `FULL` không làm hỏng biên dịch (chạy thử ở mục 4.1); `app/src/testStaging/.../PreviewAdGallerySlots.kt` cũng vậy (`if`, không `when`). `scripts/tests/fixtures/release/AdPlacement.kt` là bản sao cho test của `release.sh`, không đọc `NativeAdSize`; plan không thêm dòng vào `AdPlacement` hay `AdFormat`. Người đọc ảnh chuẩn: `scripts/check.sh` đọc mọi ảnh trong `src/test/screenshots/` theo module (không theo tên), `scripts/design-compare.sh ads` đọc ảnh cùng tên ảnh thiết kế; không script nào khác đọc tên các ảnh này (`grep -rn "NativeAdCard_small\|SampleScreen_ready\|AdGalleryScreen_default" scripts/ .github/`, 2026-10-25: không có).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi kích thước native nhỏ, native toàn màn hình, thêm biến thể native đầy đủ, thêm làm mới tại chỗ mỗi 60 giây cho native nhỏ (đổi tần suất: điểm chặn 1). Vị trí dùng: `SAMPLE_NATIVE_SMALL`, `GALLERY_NATIVE_SMALL`, `GALLERY_NATIVE_FULL_SCREEN` (đã có).
- Sự kiện phân tích: Không chạm. Lượt làm mới đi qua `nativeAdMetrics` như lượt đầu (T-023), mỗi lượt một bộ sự kiện riêng.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `AdLoader`, `NativeAdOptions`, `VideoOptions`, `NativeAd.getStarRating`, `MediaView.setImageScaleType` không cần quyền ngoài `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có từ Google Mobile Ads.
- **Thư viện và SDK:** Không thêm. `VideoOptions`, `NativeAdOptions`, `MediaAspectRatio`, `NativeAd` thuộc `play-services-ads` (`libs.play.services.ads`, đã có trên classpath biên dịch của `:core:ads`); Compose, `kotlinx-coroutines-test`, Robolectric, Roborazzi đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** đếm 60 giây chỉ ở foreground trong `refreshingNativeAdStates` bằng `withTimeoutOrNull` lồng với `foreground.first { it }` có thể khó test trên đồng hồ ảo của `runTest` (thời gian ảo và `StateFlow` cùng chạy).
- **Điều kiện chuyển:** test `NativeAdRefreshTest` của nhánh nền (dòng "vào nền giữa chừng" ở mục 4) vẫn thất bại vì cùng một lỗi sau 2 lần sửa, hay test chạy quá 60 giây thật.
- **Cách làm thay:** tách việc đếm thành hàm thuần `refreshDueAt(elapsedForegroundMillis)` trên `MonotonicClock` giả, vòng kiểm mỗi 1 giây (`delay(1_000)`) cộng dồn thời gian ở foreground. Cùng file `NativeAdRefresh.kt`; test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test viết trước code (auto-run.md, bước 3). Test có kiểm kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Test kiểm màu vẽ `decorView` ra `Bitmap` (CLAUDE.md, bước 10). Thời gian dùng `runTest` và `MonotonicClock` giả.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 (icon, tiêu đề, CTA, "Ad", AdChoices) | Compose UI test | `NativeAdCardTest.small_showsIconHeadlineCtaBadgeAndAdChoices` (đã có, giữ); `small_iconIs36dp`, `small_ctaIs34dpHighWith13spText`, `small_badgeTextIs10sp`, `small_hasNoMedia` |
| KB-1, tiêu chí 2 (80 dp, khung chờ cùng chiều cao) | Compose UI test | `NativeAdCardTest.small_is80dpHigh` (thay `small_is72dpHigh`); `NativeAdSlotTest.loading_placeholderIs80dp`; `NativeAdSlotTest.bottomSlotReserve_equalsDrawnHeight` (D-04, AR-02: `NativeAdDimens.BottomSlotHeight` bằng chiều cao đo được của thẻ và của khung chờ) |
| KB-1, tiêu chí 3 (lỗi thì 0 dp, báo app) | Compose UI test | `NativeAdSlotTest.unavailable_takesZeroHeight` (đã có, giữ, đối chứng 80 dp khi đang tải); `NativeAdSlotTest.availability_reportsTrueOnLoadedFalseOnUnavailable_notOnLoading`; `availability_falseThenTrue_whenAdComesAfterFailure` |
| KB-1, tiêu chí 4 (màu, chữ đọc được ở chế độ tối) | Compose UI test | `NativeAdCardTest.small_headlineAndCtaColors_lightAndDark`: đọc điểm ảnh giữa nền thẻ, giữa nền CTA, so với `surfaceContainerLow`, `tertiaryContainer` của mỗi chế độ; điểm chữ tiêu đề khác nền thẻ |
| KB-1, tiêu chí 5 (thay mỗi 60 giây ở foreground; lỗi giữ cũ) | Unit test | `NativeAdRefreshTest.after60sForeground_loadsNewAd_emitsIt_thenDestroysOld`; `at59s_noRequest` (đối chứng); `background_pausesCount_resumesWithElapsedKept` (30 giây foreground, 120 giây nền, 30 giây foreground thì gửi đúng lúc đó); `refreshFails_keepsOldAd_retriesAt2s4s8s` (O-05); `refreshSucceeds_afterFailures_resetsCounter`; `consentClosed_atRefresh_sendsNothing_keepsAd` (S-15); `loaderThrows_countsAsCode0_keepsAd` (O-09); `cancelled_destroysShownAndInFlightAd` |
| KB-1, chỉ native nhỏ làm mới | Unit test | `NativeAdRetryTest.refreshFalse_neverRequestsAgainAfterLoaded` (medium); `refreshTrue_handsOverToRefresh` |
| KB-1, trường hợp đặc biệt 1 (chưa đồng ý, vị trí tắt) | Compose UI test | `NativeAdSlotTest.noConsent_noSpace`, `placementOff_noSpace` (đã có, giữ) |
| KB-1, trường hợp đặc biệt 2 (rời màn thì hủy) | Unit test + Compose UI test | `NativeAdSourceTest.claimerCancelled_whileLoading_lateAdDestroyed_sourceNotLoading` (O-03); `NativeAdSlotTest.leavingComposition_destroysAd` (đã có, giữ) |
| O-01 (mỗi lượt hiện một quảng cáo riêng; lấy thì nạp mới) | Unit test | `NativeAdSourceTest.claim_returnsPendingAd_andStartsNextLoad`; `twoSlots_samePlacement_getDifferentAds` (hai lượt chồng nhau, L-068) |
| O-02 (hai chỗ cùng chờ: chỗ thua thử lại một lần) | Unit test | `NativeAdSourceTest.twoClaimersOneLoad_loserRetriesOnce_getsSecondAd`; `loserRetryFails_getsUnavailable_notTwice` |
| O-03 (lỗi về sau khi rời màn không hẹn thử lại) | Unit test | `NativeAdSourceTest.failureAfterClose_schedulesNoRetry`; `close_destroysPendingAd` |
| O-04 (55 phút) | Unit test | `NativeAdSourceTest.pendingAdAt54m59s_isClaimed`, `pendingAdAt55m_isDestroyed_notClaimed` (biên đúng, ghi rồi đọc lại); `FreshAdCacheTest.nativeFullScreenMaxAge_is55Minutes` |
| Cờ tắt giữa chừng (L-061) | Unit test | `NativeAdSourceTest.placementSwitchedOff_closesSource_destroysPending` qua `AdsController` với `FakeRemoteSettings` |
| O-06 (tùy chọn yêu cầu) | Unit test | `NativeRequestOptionsTest.everySize_startMuted_noCustomControls_singleImage`; `mediumAndFull_landscapeMedia`; `small_anyAspect` |
| O-07 (co giãn ảnh trước khi gắn) | Compose UI test | `NativeAdViewBindingTest.mediaScaleType_setBeforeSetNativeAd` (bộ ghi lời gọi) |
| O-08 (nhãn, AdChoices, sao) | Unit test + Compose UI test | `NativeStarCountTest`: `null`, 0, -1, NaN thì 0; 0.4 thì 0... cụ thể: 0.4 thì 0 (làm tròn), 0.5 thì 1, 3.6 thì 4, 4.5 thì 5, 7.0 thì 5; `NativeAdCardTest.full_starRow_hiddenWhenZero_shownFilledWhenFour`; `small_adChoicesTopRight_notOverlapped` (bounds AdChoices không giao bounds tiêu đề, CTA) |
| KB-2, tiêu chí 1 (media đầy đủ, ít nhất 120 x 120, không cắt) | Compose UI test | `NativeAdCardTest.full_mediaIs200dpHigh_atLeast120dpWide`; `full_iconIs40dp_headline14sp` |
| KB-2, tiêu chí 2 (giữ chỗ, lỗi, tối) | Compose UI test | `NativeAdCardTest.full_placeholderHeight_equalsCardHeight`; màu như KB-1 tiêu chí 4 cho `FULL` |
| Toàn màn hình theo thiết kế vẽ lại | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_drawn36dp_touchAtLeast48dp`; `cta_is52dpHigh` (thay 48 dp); `icon_is48dp`; `headline_is16sp` |
| Giao diện native nhỏ, đầy đủ | Screenshot test | `NativeAdCardScreenshotTest`: `NativeAdCard_small_light.png`, `NativeAdCard_small_dark.png` (ghi lại), `NativeAdCard_full_light.png`, `NativeAdCard_full_dark.png`, `NativeAdPlaceholder_small_light.png` (mới, `animated = false`) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `NativeFullScreenAdScreen_max-content_small.png`, `SampleScreen_ready_small.png`, `AdGalleryScreen_default_small.png` (ghi lại) |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, `scripts/design-compare.sh ads`, rà soát độc lập theo mục 7 của mẫu thiết kế | `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_ad-loading_light.png`, `NativeFullScreenAdScreen_default_light.png`, `NativeFullScreenAdScreen_default_dark.png`, `AdGalleryScreen_default_light.png`, `AdGalleryScreen_default_dark.png`, `AdGalleryScreen_inlist_light.png` (ghi lại); `AdGalleryScreen_*` so bằng `scripts/design-compare/DesignCompare.java` như T-014 |
| | | Ảnh thiết kế của ticket khác (T-030): `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `NativeAdCardTest.badgeText_isAd` (đã có, giữ) |
| Làm mới 60 giây trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Mở màn mẫu trên bản staging, chờ 70 giây ở foreground, `adb logcat -v threadtime \| grep ' VGL\.'` thấy `Native sample_native_small refresh: loaded`; vào nền 2 phút rồi quay lại, không có lượt làm mới trong lúc ở nền |

**Ngoại lệ viết test trước:**

- Ghi lại ảnh chuẩn screenshot (mục 3.2): ảnh là kết quả của code, ghi sau code ở bước 4 theo plan.
- `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`: tài liệu.
- `strings.xml`: tài nguyên.
- O-07: hôm nay `MediaView` đã đặt `FIT_CENTER` trong `factory`; test thứ tự có thể qua ngay trên code cũ. Test giữ luật, không thất bại trước; đối chứng: test tạm đặt cách co giãn sau `setNativeAd` thì test phải đỏ (chạy ở bước 3, không commit).
- Các test đã có giữ nguyên ("đã có, giữ") là test hồi quy, qua ngay.

### 4.1. Chạy thử

- **Đã chạy thử:** L-089 (thêm dòng vào enum dùng chung `NativeAdSize`) và đổi `SmallHeight` 72 thành 80 dp, trên worktree tạm `${TMPDIR:-/tmp}/t027-try` từ commit `a1cec25`, 2026-10-25, không commit. Thay đổi tạm: `enum class NativeAdSize { SMALL, MEDIUM, FULL }`, nhánh `FULL` gộp với `MEDIUM` ở bốn `when`, `SmallHeight = 80.dp`.
- **Lệnh:** `./gradlew --no-daemon --console=plain --continue :core:ads:testDebugUnitTest :core:ads:verifyRoborazziDebug :feature:sample:testDebugUnitTest :feature:sample:verifyRoborazziDebug :app:testStagingDebugUnitTest :app:verifyRoborazziStagingDebug`. Chưa chạy `scripts/check.sh` (E dưới 2, không bắt buộc).
- **Kết quả:** KẾT_QUẢ_CHẠY_THỬ

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi định dạng và tần suất, thêm làm mới tại chỗ), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Code ở `:core:ads`; ảnh chuẩn ở `:core:ads`, `:feature:sample`, `:app` |
| B. Độ rõ của yêu cầu | 2 | Spec `confirmed`, luật và thiết kế rõ số đo; còn câu hỏi ảnh hưởng cách làm: native đầy đủ chưa có định dạng (câu 2), vị trí nhãn "Ad" (câu 6), thu gọn thuộc T-027 hay T-030 (câu 4) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có unit test với đồng hồ giả, giao diện có test đo; làm mới với quảng cáo thật chỉ thấy trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi chiều cao native nhỏ mà `:feature:sample`, màn xem thử, native thu gọn và native trong danh sách dựa vào; T-026, T-028, T-030, T-032 phụ thuộc task này |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu đã có (`retryingNativeAdStates`, `FreshAdCache`, `NativeAdCard`); `VideoOptions` là API của SDK đã dùng |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn; cần duyệt chéo) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdSize`, `NativeAdDimens`, `NativeAdSlot` (API công khai của `:core:ads` cho feature), `AdsController.nativeAd`, `FreshAdCache`, ảnh chuẩn của `:feature:sample` và `:app`.
- Task khác có thể xung đột: T-024 (`AdsController.kt`, đang chạy ở nhánh `claude/T-024-app-open-luc-mo-app`, khác hàm); T-030 (thanh đáy và thu gọn: dùng `BottomSlotHeight`, `onAdAvailabilityChange`, hằng số trễ 500 ms; ảnh `AdGalleryScreen_collapsible-*`, `CollapsibleNativeAd_*`); T-028 (pool: dùng `NativeAdSource` hay viết pool riêng, `NativeAdSize`); T-026 (đếm ngược trên `NativeFullScreenAdScreen`); T-032. T-035 đã xong (thiết kế).
- Đề xuất thứ tự chạy: sau T-024 (cùng sửa `AdsController.kt`); trước T-026, T-028, T-030, T-032. Nếu tách (câu 1): phần A trước, phần B sau.

## 7. Điều kiện dừng riêng

- Đo trên thiết kế khác số của mục 5.7 quá một bước lưới 4 dp (ví dụ khe đáy không còn 80 dp): dừng, mâu thuẫn thiết kế và luật.
- Đổi `NativeAdSize.SMALL` làm đỏ test của module ngoài `:core:ads`, `:feature:sample`, `:app` (ví dụ `:core:network`): dừng, plan chưa liệt kê.
- Cần thêm định dạng vào `AdFormat` hay vị trí vào `AdPlacement` để hiện native đầy đủ: dừng (câu 2).

## 8. Câu hỏi cho người duyệt

1. **Tách ticket cỡ L?** Phần A (giao diện, ảnh chuẩn, O-06 tới O-08) và phần B (nguồn một quảng cáo, làm mới, O-01 tới O-05) độc lập về file trừ `NativeAdRetry.kt`, `AdsController.kt`.
   - Đề xuất của AI: tách. T-027 giữ phần A (khoảng 15M token, ảnh chuẩn đổi một lần); ticket mới cho phần B (khoảng 15M), phụ thuộc T-027. AI không tự tạo ticket; người duyệt tạo ticket rồi AI viết plan phiên bản 2 cho từng phần.
2. **Native đầy đủ chưa có định dạng.** Mục 5.7 có "Native đầy đủ, khe `native_full`", M-05 có khe `native_full`, nhưng MC-02 (danh sách đóng) và `AdFormat` không có `native_full`. Mâu thuẫn trong luật.
   - Đề xuất của AI: task này chỉ làm composable `NativeAdSize.FULL` và screenshot test, không thêm định dạng hay vị trí. Muốn có vị trí thì sửa MC-02 trước (thêm `native_full`), rồi một task sau thêm `AdFormat.NATIVE_FULL` và vị trí xem thử `gallery_native_full` (chạy thử L-089 cho `AdFormat`, `AdPlacement` lúc đó). Phương án khác: bỏ native đầy đủ khỏi T-027.
3. **Native medium:** thiết kế vẽ lại medium (CTA 44 dp, hàng sao, `cta_large`), nhưng ticket chỉ nêu small, full, toàn màn hình; medium lấy từ pool (T-028).
   - Đề xuất của AI: T-028 đổi số đo medium; task này giữ `NativeAdCard_medium_*`.
4. **Native nhỏ một cỡ cho mọi chỗ, và ảnh thu gọn.** README thiết kế ghi T-027 ghi lại ảnh "khe đáy và lớp thu gọn", ticket ghi "Không bao gồm: thanh đáy và thu gọn (T-030)". Đổi `NativeAdSize.SMALL` lên 80 dp cũng đổi native nhỏ trong danh sách và dạng thu gọn của native thu gọn.
   - Đề xuất của AI: một cỡ 80 dp cho mọi native nhỏ (D-02 nói "một hằng số"); task này ghi lại các ảnh bị đổi theo chiều cao (`CollapsibleNativeAd_collapsed_*`, `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_inlist_*`) nhưng không đổi bố cục lớp thu gọn 250 dp; T-030 làm lớp nổi và so với ảnh thiết kế `AdGalleryScreen_collapsible-*`. Phương án khác: giữ 72 dp cho trong danh sách và thu gọn, thêm `NativeAdSize.BOTTOM` 80 dp (thêm một dòng enum nữa).
5. **Nút đóng toàn màn hình 36 dp** nhỏ hơn vùng chạm 48 dp của quy tắc chung (README thiết kế, mục 7) và của test hiện có (`assertWidthIsAtLeast(48.dp)`).
   - Đề xuất của AI: vẽ 36 dp như thiết kế và KB-16, vùng chạm 48 dp bằng `minimumInteractiveComponentSize()`; test đổi thành "vẽ 36 dp, chạm ít nhất 48 dp".
6. **Vị trí nhãn "Ad".** O-08 ghi "đặt ở góc trên trái", thiết kế vẽ lại (T-035) đặt nhãn ở đầu dòng mô tả, dưới tiêu đề.
   - Đề xuất của AI: theo thiết kế đã duyệt (nhãn luôn thấy, không đè, cùng chỗ với AdsKit `adskit_native_small.xml` nếu dev xác nhận); nếu luật đúng thì T-035 vẽ lại trước khi chạy task này.
7. **Đăng ký khe native vào kế hoạch nạp sẵn (HD-02, S-22, T-021 câu 1).** T-021 ghi "T-027 đăng ký khe native vào kế hoạch"; ticket không nêu.
   - Đề xuất của AI: không làm ở task này (khe nạp khi lên màn như hôm nay, khung chờ lúc chờ lượt đầu); nạp sẵn native đi với pool ở T-028, vì `AdPreloader.kt` vừa đổi ở T-037, T-038.
8. **Chạy thử:** ghi ở mục 4.1. Chưa chạy `scripts/check.sh` đầy đủ và chưa thử `VideoOptions` trên Robolectric (E là 1); người duyệt muốn chạy thêm thì ghi ở mục 9.
9. Plan chưa qua rà soát độc lập (bước 6 của mục 4, `auto-run.md`); phiên chính giao sau.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
