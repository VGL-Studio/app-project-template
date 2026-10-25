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
| KB-1 | Khi nạp lỗi thì vùng quảng cáo co về 0 dp, nội dung không chừa khoảng trống cho nó. | Đã có từ T-021 (`Unavailable` không vẽ gì, trừ `keepSpaceWhenEmpty`). Spec đã gạch tiêu chí cũ "giữ khoảng trống cạnh nút"; plan chưa gỡ `keepSpaceWhenEmpty` (CLAUDE.md bước 7 còn nhắc) và hỏi ở mục 8, câu 11. Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit` để báo app "có"/"không có" quảng cáo (D-02); T-030 dùng để chừa chỗ (D-04). |
| KB-1 | Màu lấy từ hệ thống thiết kế; chữ quảng cáo luôn đọc được trên nền của vùng quảng cáo, kể cả khi máy ở chế độ tối. | Màu theo token như T-010 (`docs/features/ads/design/README.md`, mục 3). Test đo màu chữ tiêu đề và CTA trên nền thẻ ở cả hai chế độ (vẽ `decorView` ra `Bitmap`, CLAUDE.md bước 10). |
| KB-1 | Khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ. | Phần B: sau mỗi lần hiện quảng cáo, mỗi 60 giây có một nhịp; nhịp rơi lúc app ở nền thì bỏ (CL-02, L-A19); nhịp ở foreground qua cổng gửi (B0) thì lấy quảng cáo mới từ nguồn (B2); có thì thay và hủy cái cũ; lỗi thì giữ cái cũ và thử lại theo nhánh "còn giữ quảng cáo" của O-05 (2, 4, 8, 16, 32 rồi 60 giây). Áp cho native nhỏ ở khe một quảng cáo (`NativeAdSlot` với `NATIVE_SMALL`); medium không làm mới (5.7). Trễ 500 ms sau lớp thu gọn là việc của T-030 (hằng số đặt ở task này). |
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
   - `NativeAdPlaceholderLayout`: khung chờ của `SMALL` cao 80 dp; của `FULL` cao cố định `FullPlaceholderHeight`. Để thẻ `FULL` cũng cao cố định, tiêu đề một dòng, dòng nhãn một dòng, mô tả cố định 2 dòng (`minLines = maxLines = 2`), CTA một dòng; "cao theo nội dung" của 5.7 nghĩa là theo bố cục, không theo độ dài chữ. Phạm vi: chiều cao bằng nhau với mọi độ dài chữ, có test chữ ngắn và chữ dài.
   - Hàm thuần `nativeStarCount(rating: Double?): Int` (O-08): `null`, `<= 0` hay NaN trả 0; còn lại `round(rating)` kẹp trong 1..5. `NativeAdAssets` thêm trường `starRating: Double? = null` (giá trị mặc định, nên nơi gọi cũ không đổi); `SdkLoadedNativeAd` đọc `nativeAd.starRating`.
2. `NativeAdSlot.kt`: tách điều kiện hiện khe thành hàm thuần `nativeSlotAllowed(canRequestAds: Boolean, enabled: Boolean, unitId: String?): Boolean` mà `NativeAdSlot` gọi (KB-1, trường hợp đặc biệt 1). `requireNativeFormat` giữ `NATIVE_SMALL -> SMALL`, `NATIVE_MEDIUM -> MEDIUM`. Native đầy đủ chưa có định dạng trong `AdFormat` (MC-02 là danh sách đóng, không có `native_full`; mục 8, câu 2): task này chỉ thêm composable `NativeAdCard(assets, NativeAdSize.FULL)` và khung chờ, chưa có vị trí nào hiện nó. `Unavailable` với `keepSpaceWhenEmpty` giữ `BottomSlotHeight`. Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit = {}` gọi khi trạng thái chuyển sang `Loaded` (true) hay `Unavailable` (false), không gọi ở `Loading` (D-02).
3. `NativeFullScreenAdScreen.kt`: số đo theo `design/context/NativeFullScreenAdScreen_default_light.md`: nút đóng 36 dp tròn ở góc trên trái, tại (16, 24) (biểu tượng 24 dp; README mục 3 cũ ghi 48 dp), media bắt đầu ở y = 76, icon 48 dp, tiêu đề `titleMedium` 16 sp, CTA cao 52 dp, AdChoices góc trên phải. KB-16 ghi vòng 36 dp "ở góc trên phải", trùng góc AdChoices (O-08), còn thiết kế đặt trên trái: plan theo thiết kế, hỏi ở mục 8, câu 10. Đếm ngược 3 giây là của T-026. Vùng chạm: không dùng `minimumInteractiveComponentSize()` (nới bố cục lên 48 dp, đẩy media xuống khoảng 12 dp và làm đỏ `closeButton_isTopLeft_awayFromAdChoices`). Nút giữ bố cục 36 dp (`Modifier.size(36.dp).clickable`); Compose tự nới vùng nhận chạm của nút có `pointerInput` nhỏ hơn `ViewConfiguration.minimumTouchTargetSize` (48 dp) khi không có thành phần chạm khác chồng lên, không đổi bố cục. Chưa chạy thử trên Robolectric (mục 4.1, mục 8 câu 5).
4. `SdkNativeAdLoader.kt` (O-06): hàm thuần `nativeRequestOptions(size: NativeAdSize): NativeRequestOptions` (data class: `startMuted = true`, `customControls = false`, `multipleImages = false`, `mediaAspect = LANDSCAPE` cho `MEDIUM`, `FULL`; `SMALL` không xin media nên `ANY`), rồi `load` chép vào `NativeAdOptions.Builder` và `VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(false)`. AdChoices giữ `ADCHOICES_TOP_RIGHT`.
5. `NativeAdViewBinding.kt` (O-07): hôm nay `MediaView` được tạo ngay trong `factory` và đặt `FIT_CENTER` ở đó, nên test không chen được. Thêm tham số internal `newMediaView: (Context) -> MediaView = ::MediaView` cho composable gắn media, và tham số `bind` (đã có ở `NativeAdViewHost`) cho test truyền bộ ghi; test truyền một `MediaView` con ghi lời gọi `setImageScaleType` và một `bind` ghi lời gọi `setNativeAd`, rồi so thứ tự. `FIT_CENTER` vẫn đặt sau khi tạo, trước khi gắn.
6. `res/values/strings.xml` của `:core:ads`: chuỗi mô tả hàng sao cho trợ năng (`ads_native_star_rating`, "%1$d of 5 stars"); nhãn "Ad" đã có.
7. Ảnh chuẩn: ghi lại ảnh có native nhỏ, native toàn màn hình (danh sách đủ ở mục 3.2, theo kết quả chạy thử ở mục 4.1).

**Phần B. Nguồn một quảng cáo và làm mới tại chỗ**

Chỉ áp cho `NATIVE_SMALL`. `NATIVE_MEDIUM` và `NATIVE_COLLAPSIBLE` (`CollapsibleNativeAd.kt:247` gọi `ads.nativeAd(placement)`) giữ đường hiện tại (`retryingNativeAdStates` không làm mới), vì thu gọn là của T-030; có test hồi quy (mục 4).

0. **Cổng gửi chung (L-072).** `NativeSendGate.kt` (mới): `suspend fun awaitSendAllowed(foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean): Boolean`: chờ `foreground` là true, rồi trả `mayRequestNow()` (cổng đồng ý, S-15). Mọi đường gửi yêu cầu native nhỏ đều gọi hàm này ngay trước khi gửi: (a) lượt đầu khi khe lên màn, (b) lượt nạp tiếp sau khi lấy (O-01), (c) lượt thử lại của chỗ thua (O-02), (d) lượt thử lại 5 giây khi chưa giữ quảng cáo, (e) nhịp làm mới 60 giây, (f) lượt thử lại sau làm mới lỗi (2, 4, 8... giây). Trả false thì không gửi, không tính lỗi, không hẹn thử lại; đường đó kết thúc và chờ nhịp sau (e, f) hay lần khe được dựng lại (a tới d). `retryingNativeAdStates` chuyển sang gọi hàm này (hành vi không đổi).
1. `NativeAdSource.kt` (mới, internal): nguồn một quảng cáo của một vị trí, luồng chính, nhận `load: suspend (NativeLoadUse) -> NativeAdState`, `sendGate: suspend () -> Boolean` (bọc B0), `clock: MonotonicClock`, `logger`.
   - `suspend fun claim(use): NativeAdState`: có quảng cáo chờ còn hạn (dưới 55 phút) thì trả và bắt đầu nạp cái tiếp theo qua cổng (O-01); quảng cáo chờ hết hạn thì hủy, không trả, nạp lại (O-04); đang nạp thì chờ lượt đó; hai chỗ cùng chờ một lượt: chỗ thắng lấy quảng cáo, chỗ thua chờ đúng một lượt nạp nữa (qua cổng) rồi mới nhận `Unavailable` (O-02).
   - Chỗ chờ bị hủy khi yêu cầu đang bay: chỗ đó nhận `Unavailable` ngay qua hủy coroutine; quảng cáo về sau khi nguồn đã đóng thì hủy ngay; lỗi về sau đó không hẹn thử lại; nguồn không còn ở trạng thái "đang nạp" (O-03).
   - Đếm số khe đang hiện của vị trí: khe lên màn thì `attach()`, rời màn thì `detach()`; về 0 thì `close()`: hủy quảng cáo chờ, bỏ lượt đang bay. Nhờ vậy rời màn không còn giữ quảng cáo nào (KB-1). Vào lại màn thì nạp từ đầu (khung chờ). Hệ quả: lượt nạp tiếp sau khi lấy (O-01) chỉ giữ quảng cáo khi màn còn khe; hỏi ở mục 8, câu 12.
   - Hằng số `NativeAdPolicy.MAX_AGE_MILLIS = 55 * 60 * 1_000L`, `REFRESH_INTERVAL_MILLIS = 60_000L`, `REFRESH_WAIT_MILLIS = 10_000L` (CL-02: đợi quảng cáo chờ tối đa 10 giây). Trễ 500 ms sau lớp thu gọn để T-030.
2. `NativeAdSources.kt` (mới, internal): quản lý các nguồn, để test được không cần `AdsController` (lớp này tự tạo `SdkNativeAdLoader` và dùng `SystemMonotonicClock`). Nhận qua hàm tạo: `enabled: (AdPlacement) -> Flow<Boolean>`, `canRequestAds: StateFlow<Boolean>`, `newLoad: (AdPlacement, NativeLoadUse) -> suspend () -> NativeAdState`, `sendGate`, `clock`, `scope`, `logger`. `source(placement)` tạo hay trả nguồn; theo dõi `enabled` và `canRequestAds`: một trong hai thành false thì đóng nguồn (hủy quảng cáo chờ, lượt đang bay) và báo các khe đang hiện kết thúc, khe hủy quảng cáo đang hiện và dừng làm mới (L-061). `AdsController` chỉ dựng `NativeAdSources` với các phụ thuộc thật.
3. `NativeAdRefresh.kt` (mới): `refreshingNativeAdStates(first: NativeAdState.Loaded, source: NativeAdSource, foreground, sendGate, retryResets, clock, logger, placementKey): Flow<NativeAdState>`:
   - Phát quảng cáo đầu. Mỗi `REFRESH_INTERVAL_MILLIS` có một nhịp. Nhịp đến khi app ở nền thì bỏ, không dồn lại, nhịp sau 60 giây nữa (CL-02 "ở nền thì bỏ nhịp", L-A19); hỏi ở mục 8, câu 9.
   - `load` của làm mới: `source.claim(NativeLoadUse.REFRESH)` đợi tối đa `REFRESH_WAIT_MILLIS` (CL-02: nạp quảng cáo chờ, đợi tối đa 10 giây, rồi đổi vào). Không gửi yêu cầu riêng ngoài nguồn. Có quảng cáo thì phát quảng cáo mới rồi hủy cái cũ.
   - Lỗi hay hết 10 giây (cả lỗi mã 0, O-09): giữ quảng cáo cũ, thử lại sau `AdRetryPolicy.nativeDelayMillis(failures, holdsAd = true)`; `retryResets` đổi (Internet về, S-24) thì cắt ngắn lần chờ thử lại như nhánh 5 giây; thành công thì bộ đếm về 0. Lượt thử lại cũng qua cổng gửi (B0).
   - Hủy collection thì hủy quảng cáo đang hiện, và nguồn bỏ quảng cáo đang về.
4. `NativeAdRetry.kt`: `retryingNativeAdStates` gọi `awaitSendAllowed` (B0); thêm `refresh: (NativeAdState.Loaded) -> Flow<NativeAdState>?`: khác null thì nối sang làm mới thay vì `awaitCancellation()`.
5. `AdTelemetry.kt`: `enum class NativeLoadUse { SINGLE_AD, LIST_ROW, REFRESH }`; `REFRESH` dùng `perRequestLoad(format, loadPlacement)`: mỗi lượt một bộ sự kiện mới, không có `load_attempt` (luật mục 8.2: "pool và lượt làm mới native nhỏ không có"), vị trí là vị trí hiện tại của màn đang dùng khe; bộ ghi doanh thu gắn vào mọi lượt như lượt đầu (L-A07). Lượt đầu và lượt thử lại 5 giây giữ `SINGLE_AD` (có `load_attempt`). Lượt nạp tiếp sau khi lấy (O-01) là quảng cáo cho nhịp làm mới, nên dùng `REFRESH` (mục 8, câu 13).
6. `AdsController.kt`: dựng `NativeAdSources`; `nativeAd(placement)` với `NATIVE_SMALL` đi qua nguồn và làm mới; `NATIVE_MEDIUM`, `NATIVE_COLLAPSIBLE` giữ nguyên. Native toàn màn hình nạp sẵn dùng `nativeFullScreenCache(onDiscard)` (B7). `loadNativeAd` của danh sách giữ nguyên tới T-028.
7. `FreshAdCache.kt`: thêm `internal fun <T> nativeFullScreenCache(onDiscard: (T) -> Unit) = FreshAdCache(NativeAdPolicy.MAX_AGE_MILLIS, onDiscard)` (O-04); `AdsController` gọi hàm này thay vì hàm tạo, để test kiểm đúng giá trị thật dùng. Mặc định 60 phút giữ cho nơi khác.
8. Ghi log bằng `AppLogger`, thẻ `Ads`: `Native <key> claimed`, `Native <key> refresh: loaded|failed <code>|blocked|skipped (background)`, `Native <key> expired`, `Native <key> late ad destroyed`, `Native <key> claim lost, retry once`, `Native <key> source closed`. Không ghi mã đơn vị.
9. `docs/ops/ads-guide.md`: mục native: khe đáy 80 dp, một hằng số; làm mới 60 giây; tham số `onAdAvailabilityChange`; native đầy đủ chưa có định dạng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | A1: `NativeAdSize.FULL`, `NativeAdDimens` theo 5.7, `SmallLayout`, `FullLayout`, khung chờ, `nativeStarCount`, `NativeAdAssets.starRating` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | A2: `nativeSlotAllowed`, `onAdAvailabilityChange`, giữ chỗ bằng `BottomSlotHeight` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | A3: số đo theo thiết kế vẽ lại |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | A4: `nativeRequestOptions`, `VideoOptions`; nhánh `FULL`; `starRating` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | A5: tham số `newMediaView` để test chen bộ ghi lời gọi (O-07) |
| Sửa | `core/ads/src/main/res/values/strings.xml` | A6: `ads_native_star_rating` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSource.kt` | B1: nguồn một quảng cáo (đếm khe, đóng khi về 0), `NativeAdPolicy` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeSendGate.kt` | B0: `awaitSendAllowed`, cổng gửi chung của mọi đường |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSources.kt` | B2: quản lý các nguồn, đóng khi vị trí tắt hay rút đồng ý |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRefresh.kt` | B3: `refreshingNativeAdStates` (bỏ nhịp ở nền, đợi 10 giây, thử lại có `retryResets`) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | B4: gọi `awaitSendAllowed`, nối sang làm mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | B6: dựng `NativeAdSources`, `nativeAd` qua nguồn chỉ cho `NATIVE_SMALL`, `nativeFullScreenCache` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` | B5: `NativeLoadUse.REFRESH` dùng `perRequestLoad` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FreshAdCache.kt` | B7: `nativeFullScreenCache` (55 phút) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdLoading.kt` | B1: `loadOneNativeAd` trả quảng cáo về muộn cho nguồn hủy (O-03), nếu cần tách khỏi `NativeAdRequest` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | KDoc `CollapsibleNativeAdContent` (dòng 189) "72 dp collapsed" thành 80 dp; dạng thu gọn vẽ bằng `NativeAdSize.SMALL` nên chiều cao đổi theo, không đổi hành vi thu gọn (T-030) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Chỉ khi khoảng giữ chỗ của dòng quảng cáo dùng hằng số 72 dp riêng |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Bước 3: 72 dp thành 80 dp (`small_is72dpHigh` đổi tên `small_is80dpHigh`, `longTexts_keepHeight`), `cta_is36dpHigh` thành `small_ctaIs34dpHighWith13spText`, `placeholder_matchesCardHeight_forEachSize` có chiều cao riêng cho `FULL` (test lặp `NativeAdSize.entries`); số đo icon, cỡ chữ; test `FULL`; màu chữ hai chế độ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Bước 3: 72 dp thành 80 dp (`loading_showsPlaceholder`, `unavailable_takesNoSpace`, `unavailable_keepsSpace_whenDeclaredNextToButton`, `buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace`, `boundNativeAd_withFakeAd_drawsCard`); `nativeSlotAllowed`; `onAdAvailabilityChange`; hằng số khe bằng chiều cao vẽ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Bước 3: 72 dp thành 80 dp; thứ tự O-07 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Bước 3: dạng thu gọn 72 thành 80 (sáu chỗ `assertHeight(AD, 72f)`) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Bước 3: `inList_insertsAdsAfterEveryNItems` đo dòng quảng cáo 72 thành 80 dp (đỏ ở chạy thử, mục 4.1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Bước 3: `loadingSlot_showsPlaceholderOfSmallHeight` 72 thành 80 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Bước 3: `texts_followDesign` (CTA 48 thành 52 dp, icon 48 dp, tiêu đề 16 sp), `closeButton_isAtLeast48dp` thành `closeButton_drawn36dp_touchAtLeast48dp`; `closeButton_isTopLeft_awayFromAdChoices` giữ (16, 24); thêm `media_startsAt76dp` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Thêm ảnh `NativeAdCard_full_light`, `NativeAdCard_full_dark`, `NativeAdPlaceholder_small_light` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | Quảng cáo giả có `starRating`, nguồn giả đếm lượt hủy |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourceTest.kt` | Test O-01 tới O-04 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeSendGateTest.kt` | Test cổng gửi chung |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourcesTest.kt` | Test cờ tắt, rút đồng ý giữa chừng (L-061), thu gọn và medium không qua nguồn |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRefreshTest.kt` | Test làm mới 60 giây, O-05 nhánh giữ quảng cáo, O-09 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRequestOptionsTest.kt` | Test O-06 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStarCountTest.kt` | Test O-08, hàng sao |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | Nối sang làm mới; đường 5 giây gọi cổng gửi chung |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FreshAdCacheTest.kt` | `nativeFullScreenCache` 55 phút |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | `NativeLoadUse.REFRESH`: không `load_attempt`, vị trí hiện tại |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenTest.kt` | Ô giả 72 dp (dòng 83 tới 99) thành 80 dp cho đúng số; test không đỏ (ghi chú rà soát 17) |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | Khe đáy 80 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_light.png` | Native đầy đủ |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdPlaceholder_small_light.png` | Khung chờ 80 dp, chụp với `animated = false` |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_light.png` | Số đo mới |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_collapsed_light.png` | Dạng thu gọn 80 dp (đỏ ở chạy thử, mục 4.1) |
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
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Dạng thu gọn 80 dp (đỏ ở chạy thử, mục 4.1) |
| Sửa | `docs/ops/ads-guide.md` | B9 |
| Sửa | `docs/features/ads/design/README.md` | Mục 3: số cũ (CTA 36 dp, icon 40 dp, khung chờ 72 dp, nút đóng 48 dp) theo số vẽ lại; mục 8: kết quả đối chiếu của `SampleScreen_*`, `NativeFullScreenAdScreen_*`, `AdGalleryScreen_default_*`, `AdGalleryScreen_inlist_light`, kèm phần lệch dự kiến của medium |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ready_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ready_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ad-loading_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_default_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_default_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_inlist_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |

Không sửa: `feature/sample/src/main/.../SampleScreen.kt` và `feature/sample/src/test/.../SampleScreenScreenshotTest.kt` dùng `NativeAdSize.SMALL`, `NativeAdPlaceholder(NativeAdSize.SMALL, ...)` qua tên, không `when` trên enum, nên thêm `FULL` không làm hỏng biên dịch (chạy thử ở mục 4.1); `app/src/testStaging/.../PreviewAdGallerySlots.kt` cũng vậy (`if`, không `when`). `scripts/tests/fixtures/release/AdPlacement.kt` là bản sao cho test của `release.sh`, không đọc `NativeAdSize`; plan không thêm dòng vào `AdPlacement` hay `AdFormat`. Người đọc ảnh chuẩn: `scripts/check.sh` đọc mọi ảnh trong `src/test/screenshots/` theo module (không theo tên), công cụ so ảnh đọc ảnh cùng tên ảnh thiết kế; `scripts/tests/run.sh` chép `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png` làm dữ liệu mẫu cho test của công cụ so ảnh (cặp ảnh giống nhau, cặp ảnh khác nhau), không so với nội dung cố định, nên ghi lại hai ảnh này không làm đỏ test của script; không script nào khác đọc tên các ảnh này (`grep -rn "NativeAdCard_small\|SampleScreen_ready\|AdGalleryScreen_default" scripts/ .github/`, 2026-10-25: không có).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi kích thước native nhỏ, native toàn màn hình, thêm biến thể native đầy đủ, thêm làm mới tại chỗ mỗi 60 giây cho native nhỏ (đổi tần suất: điểm chặn 1). Vị trí dùng: `SAMPLE_NATIVE_SMALL`, `GALLERY_NATIVE_SMALL`, `GALLERY_NATIVE_FULL_SCREEN` (đã có).
- Sự kiện phân tích: không thêm sự kiện hay tham số. Lượt làm mới native nhỏ dùng cách ghi riêng `NativeLoadUse.REFRESH` (`perRequestLoad`): mỗi lượt một bộ sự kiện mới, không `load_attempt`, vị trí là vị trí hiện tại, luôn có bộ ghi doanh thu (luật mục 8.2, L-A07). Đổi cách ghi số liệu là điểm chặn 4, đã nằm trong mức cao của plan.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `AdLoader`, `NativeAdOptions`, `VideoOptions`, `NativeAd.getStarRating`, `MediaView.setImageScaleType` không cần quyền ngoài `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có từ Google Mobile Ads.
- **Thư viện và SDK:** Không thêm. `VideoOptions`, `NativeAdOptions`, `MediaAspectRatio`, `NativeAd` thuộc `play-services-ads` (`libs.play.services.ads`, đã có trên classpath biên dịch của `:core:ads`); Compose, `kotlinx-coroutines-test`, Robolectric, Roborazzi đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** nhịp 60 giây bằng `delay` trong `refreshingNativeAdStates`, kiểm `foreground.value` lúc nhịp đến, cùng lúc đợi `claim` tối đa 10 giây bằng `withTimeoutOrNull`, có thể khó test trên đồng hồ ảo của `runTest` (thời gian ảo và `StateFlow` cùng chạy).
- **Điều kiện chuyển:** test `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s` hay `claimWaitsAtMost10s_thenKeepsOld` vẫn thất bại vì cùng một lỗi sau 2 lần sửa, hay test chạy quá 60 giây thật.
- **Cách làm thay:** tách lịch nhịp thành hàm thuần `nextRefreshTickAt(lastTickMillis)` trên `MonotonicClock` giả, vòng kiểm mỗi 1 giây (`delay(1_000)`) so đồng hồ với mốc nhịp. Cùng file `NativeAdRefresh.kt`; test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test viết trước code (auto-run.md, bước 3). Test có kiểm kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Test kiểm màu vẽ `decorView` ra `Bitmap` (CLAUDE.md, bước 10). Thời gian dùng `runTest` và `MonotonicClock` giả.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 (icon, tiêu đề, CTA, "Ad", AdChoices) | Compose UI test | `NativeAdCardTest.small_showsHeadlineAdBadgeAdvertiserAndCta`, `small_leavesAdChoicesCornerFree` (đã có, giữ); `small_iconIs36dp`, `small_ctaIs34dpHighWith13spText` (thay `cta_is36dpHigh`), `small_badgeTextIs10sp`; `small_hasNoMedia` (đã có, giữ) |
| KB-1, tiêu chí 2 (80 dp, khung chờ cùng chiều cao) | Compose UI test | `NativeAdCardTest.small_is80dpHigh` (thay `small_is72dpHigh`); `NativeAdSlotTest.loading_showsPlaceholder` (72 thành 80); `NativeAdSlotTest.bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight` (AR-02, phạm vi: chỉ so hằng số `NativeAdDimens.BottomSlotHeight` với chiều cao đo được của thẻ và khung chờ; chừa chỗ thật dưới nội dung, D-04, là test của T-030) |
| KB-1, tiêu chí 3 (lỗi thì 0 dp, báo app) | Compose UI test | `NativeAdSlotTest.unavailable_takesNoSpace` (đã có, đối chứng 80 dp khi đang tải); `availability_reportsTrueOnLoadedFalseOnUnavailable_notOnLoading`; `availability_falseThenTrue_whenAdComesAfterFailure` |
| KB-1, tiêu chí 4 (màu, chữ đọc được ở chế độ tối) | Compose UI test | `NativeAdCardTest.small_headlineAndCtaColors_lightAndDark`: đọc điểm ảnh giữa nền thẻ, giữa nền CTA, so với `surfaceContainerLow`, `tertiaryContainer` của mỗi chế độ; điểm chữ tiêu đề khác nền thẻ (qua ngay, ngoại lệ kèm đối chứng) |
| KB-1, tiêu chí 5 (mỗi 60 giây ở foreground; lỗi giữ cũ) | Unit test | `NativeAdRefreshTest.tickAt60s_claimsNewAd_emitsIt_thenDestroysOld`; `at59s_noClaim` (đối chứng); `tickInBackground_isSkipped_nextTickAt120s` (CL-02); `claimWaitsAtMost10s_thenKeepsOld`; `refreshFails_keepsOldAd_retriesAt2s4s8s` (O-05); `refreshRetry_cutShortByRetryReset` (S-24); `refreshSucceeds_afterFailures_resetsCounter`; `loaderThrows_countsAsCode0_keepsAd` (O-09); `cancelled_destroysShownAd_sourceDropsIncoming` |
| Cổng gửi chung, mọi đường (L-072) | Unit test | `NativeSendGateTest.waitsForForeground_thenReadsGate`, `gateClosed_returnsFalse`. Mỗi đường một test ở nền (không gửi tới khi về foreground) và một test cổng đóng (không gửi, không tính lỗi): (a) lượt đầu `NativeAdRetryTest.firstLoad_inBackground_waits`, `firstLoad_gateClosed_sendsNothing`; (b) nạp tiếp sau khi lấy `NativeAdSourceTest.nextLoadAfterClaim_inBackground_waits`, `_gateClosed_sendsNothing`; (c) thử lại của chỗ thua `loserRetry_inBackground_waits`, `loserRetry_gateClosed_getsUnavailable_sendsNothing`; (d) thử lại 5 giây `NativeAdRetryTest.retry5s_inBackground_waits`, `retry5s_gateClosed_sendsNothing` (đã có dạng tương tự từ T-021, giữ và đổi sang cổng chung); (e) nhịp làm mới `NativeAdRefreshTest.tick_gateClosed_sendsNothing_keepsAd` (ở nền: `tickInBackground_isSkipped_nextTickAt120s`); (f) thử lại sau làm mới lỗi `refreshRetry_inBackground_waits`, `refreshRetry_gateClosed_sendsNothing_keepsAd` |
| Số liệu của lượt làm mới (luật 8.2, L-A07) | Unit test | `NativeAdRefreshTest.everyRefresh_attachesFreshEvents_withRevenue` (như `everyNativeLoad_attachesFreshTelemetryEvents`); `AdTelemetryTest.refreshUse_hasNoLoadAttempt_placementIsCurrent`; `singleAdUse_keepsLoadAttempt` (đối chứng) |
| Chỉ native nhỏ qua nguồn và làm mới | Unit test | `NativeAdRetryTest.refreshNull_neverRequestsAgainAfterLoaded` (qua ngay, ngoại lệ kèm đối chứng); `refreshGiven_handsOverToRefresh`; `NativeAdSourcesTest.mediumAndCollapsible_notServedBySources`; `CollapsibleNativeAdTest` (đã có, hồi quy cho khe thu gọn) |
| KB-1, trường hợp đặc biệt 1 (chưa đồng ý, vị trí tắt) | Unit test + Compose UI test | `NativeAdSlotTest.slotAllowed_falseWithoutConsent`, `slotAllowed_falseWhenPlacementOff`, `slotAllowed_falseWithoutUnitId`, `slotAllowed_trueWhenAllGiven` (đối chứng) cho `nativeSlotAllowed`; `withoutAdsController_takesNoSpace` (đã có, giữ) |
| KB-1, trường hợp đặc biệt 2 (rời màn thì hủy) | Unit test + Compose UI test | `NativeAdSourceTest.claimerCancelled_whileLoading_lateAdDestroyed_sourceNotLoading` (O-03); `lastSlotDetached_closesSource_destroysPendingAd` (không còn giữ quảng cáo chờ); `NativeAdSlotTest.leavingComposition_destroysAd` (đã có, giữ) |
| O-01 (mỗi lượt hiện một quảng cáo riêng; lấy thì nạp tiếp) | Unit test | `NativeAdSourceTest.claim_returnsPendingAd_andStartsNextLoad`; `twoSlots_samePlacement_getDifferentAds` (hai lượt chồng nhau, L-068) |
| O-02 (chỗ thua thử lại một lần) | Unit test | `NativeAdSourceTest.twoClaimersOneLoad_loserRetriesOnce_getsSecondAd`; `loserRetryFails_getsUnavailable_notTwice` |
| O-03 (lỗi về sau khi đóng không hẹn thử lại) | Unit test | `NativeAdSourceTest.failureAfterClose_schedulesNoRetry`; `close_destroysPendingAd` |
| O-04 (55 phút) | Unit test | `NativeAdSourceTest.pendingAdAt54m59s_isClaimed`, `pendingAdAt55m_isDestroyed_notClaimed` (biên đúng, ghi rồi đọc lại); `FreshAdCacheTest.nativeFullScreenCache_keepsAt54m59s_dropsAt55m` (hàm `nativeFullScreenCache` mà `AdsController` gọi) |
| Cờ tắt, rút đồng ý giữa chừng (L-061) | Unit test | `NativeAdSourcesTest.placementSwitchedOff_closesSource_destroysPending`; `consentWithdrawn_closesSource`; `placementSwitchedOff_whileRefreshing_destroysShownAd_noMoreRefresh` (đồng hồ giả, 120 giây sau không có lượt nạp nào) |
| O-06 (tùy chọn yêu cầu) | Unit test | `NativeRequestOptionsTest.everySize_startMuted_noCustomControls_singleImage`; `mediumAndFull_landscapeMedia`; `small_anyAspect` |
| O-07 (co giãn ảnh trước khi gắn) | Compose UI test | `NativeAdViewBindingTest.mediaScaleType_setBeforeSetNativeAd` (bộ ghi qua `newMediaView` và `bind`) |
| O-08 (nhãn, AdChoices, sao) | Unit test + Compose UI test | `NativeStarCountTest`: `null`, 0, -1, NaN thì 0; 0.4 thì 0 (làm tròn), 0.5 thì 1, 3.6 thì 4, 4.5 thì 5, 7.0 thì 5; `NativeAdCardTest.full_starRow_hiddenWhenZero_shownFilledWhenFour`; AdChoices: `small_leavesAdChoicesCornerFree` (đã có, giữ) |
| KB-2, tiêu chí 1 (media đầy đủ, ít nhất 120 x 120, không cắt) | Compose UI test | `NativeAdCardTest.full_mediaIs200dpHigh_atLeast120dpWide`; `full_iconIs40dp_headline14sp` |
| KB-2, tiêu chí 2 (giữ chỗ, lỗi, tối) | Compose UI test | `NativeAdCardTest.full_placeholderHeight_equalsCardHeight_shortAndLongTexts` (hai quảng cáo giả: chữ ngắn, chữ dài nhiều dòng); màu như KB-1 tiêu chí 4 cho `FULL` |
| Toàn màn hình theo thiết kế vẽ lại | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_drawn36dp_touchAtLeast48dp` (chạm cách mép nút 5 dp vẫn gọi `onClose`; thay `closeButton_isAtLeast48dp`); `closeButton_isTopLeft_awayFromAdChoices` (đã có, vẫn 16, 24); `media_startsAt76dp`; `texts_followDesign` (CTA 52 dp, icon 48 dp, tiêu đề 16 sp) |
| Giao diện native nhỏ, đầy đủ | Screenshot test | `NativeAdCardScreenshotTest`: `NativeAdCard_small_light.png`, `NativeAdCard_small_dark.png` (ghi lại), `NativeAdCard_full_light.png`, `NativeAdCard_full_dark.png`, `NativeAdPlaceholder_small_light.png` (mới, `animated = false`) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `NativeFullScreenAdScreen_max-content_small.png`, `SampleScreen_ready_small.png`, `AdGalleryScreen_default_small.png` (ghi lại) |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh bằng công cụ so ảnh chạy thẳng theo từng module (dưới bảng), rà soát độc lập theo mục 7 của mẫu thiết kế | `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_ad-loading_light.png` (`:feature:sample`), `NativeFullScreenAdScreen_default_light.png`, `NativeFullScreenAdScreen_default_dark.png` (`:core:ads`), `AdGalleryScreen_default_light.png`, `AdGalleryScreen_default_dark.png`, `AdGalleryScreen_inlist_light.png` (`:app`), đều ghi lại |
| | | Ảnh thiết kế của ticket khác (T-030): `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `NativeAdCardTest.badgeText_isAd` (đã có, giữ) |
| Làm mới 60 giây trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Mở màn mẫu trên bản staging, chờ 70 giây ở foreground, `adb logcat -v threadtime \| grep ' VGL\.'` thấy `Native sample_native_small refresh: loaded`; vào nền 2 phút rồi quay lại, không có lượt làm mới trong lúc ở nền |

**Ảnh so sánh với thiết kế (ADR-0020):** ảnh chuẩn của feature ads nằm ở ba module (`core/ads`, `feature/sample`, `app`), không ở `feature/ads/`, nên không dùng script so sánh bọc sẵn; chạy thẳng công cụ so ảnh cho từng thư mục, ra thư mục tạm, như T-013 và T-014: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens <module>/src/test/screenshots ${TMPDIR:-/tmp}/t027-compare-<module>`. Phần lệch dự kiến: thiết kế vẽ lại medium cao 410 dp, plan giữ 386 dp (câu 3), nên ở `AdGalleryScreen_default_*` và `AdGalleryScreen_inlist_light` mọi thứ dưới thẻ medium lệch khoảng 24 dp (quá một bước lưới 4 dp); ghi vào mục 8 của README thiết kế, không tính là lệch của task này. Công cụ ghi đè `summary.md` của thư mục đích, nên chỉ chép ảnh `_compare.png` của các ảnh trên vào `docs/features/ads/design/review/` và ghi kết quả vào mục 8 của README thiết kế.

**Ngoại lệ viết test trước:**

- Ghi lại ảnh chuẩn screenshot (mục 3.2): ảnh là kết quả của code, ghi sau code ở bước 4 theo plan.
- `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`: tài liệu.
- `strings.xml`: tài nguyên.
- Các test đã có giữ nguyên ("đã có, giữ") là test hồi quy, qua ngay.
- Test qua ngay trên code cũ (L-055), mỗi test có bước đối chứng chạy ở bước 3, không commit: `small_headlineAndCtaColors_lightAndDark` (đối chứng: tạm đổi nền thẻ sang `surface` thì test đỏ); `small_hasNoMedia` (đối chứng: tạm vẽ `NativeAdMediaArea` trong `SmallLayout` thì đỏ); `refreshNull_neverRequestsAgainAfterLoaded` (đối chứng: cùng test với `refresh` khác null thì thấy lượt nạp thứ hai ở 60 giây); `slotAllowed_trueWhenAllGiven` (đặt cạnh ba test false). Kiểm AdChoices dùng `small_leavesAdChoicesCornerFree` đã có, không thêm test gần trùng.

### 4.1. Chạy thử

- **Đã chạy thử:** L-089 (thêm dòng vào enum dùng chung `NativeAdSize`) và đổi `SmallHeight` 72 thành 80 dp, trên worktree tạm `${TMPDIR:-/tmp}/t027-try` từ commit `a1cec25`, 2026-10-25, không commit. Thay đổi tạm: `enum class NativeAdSize { SMALL, MEDIUM, FULL }`, nhánh `FULL` gộp với `MEDIUM` ở bốn `when`, `SmallHeight = 80.dp`.
- **Chưa chạy:** nới vùng chạm nút đóng 36 dp lên 48 dp mà không đổi bố cục (mục 3.1, A3) trên Robolectric: chưa chạy (không chạy Gradle lại trong lượt sửa theo rà soát); hỏi ở mục 8, câu 5. Các test đỏ vì số đo mới ngoài 72/80 (CTA 34 dp, icon 36 dp, số đo toàn màn hình: `cta_is36dpHigh`, `texts_followDesign`, `closeButton_isAtLeast48dp`) chưa chạy thử, chỉ suy ra từ số đo; đã ghi tên ở mục 3.2.
- **Lệnh:** `./gradlew --no-daemon --console=plain --continue :core:ads:testDebugUnitTest :core:ads:verifyRoborazziDebug :feature:sample:testDebugUnitTest :feature:sample:verifyRoborazziDebug :app:testStagingDebugUnitTest :app:verifyRoborazziStagingDebug`. Chưa chạy `scripts/check.sh` (E dưới 2, không bắt buộc).
- **Kết quả:** Thất bại đúng như dự kiến, không có lỗi biên dịch nào (thêm `FULL` không làm hỏng `:feature:sample` hay `:app`; mọi `when` trên enum nằm ở `:core:ads`). `:core:ads`: 411 test, 19 đỏ: `NativeAdCardTest` (`small_is72dpHigh`, `longTexts_keepHeight`, `placeholder_matchesCardHeight_forEachSize`: test này lặp `NativeAdSize.entries` và giả định mọi cỡ khác `SMALL` cao 386 dp, nên `FULL` cần chiều cao riêng trong test, L-089), `NativeAdSlotTest` (5: `loading_showsPlaceholder`, `unavailable_takesNoSpace`, `unavailable_keepsSpace_whenDeclaredNextToButton`, `buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace`, `boundNativeAd_withFakeAd_drawsCard`), `NativeAdViewBindingTest.sdkLayout_keepsCardHeights`, `CollapsibleNativeAdTest` (5 test của dạng thu gọn), `InListAdsTest.loadingSlot_showsPlaceholderOfSmallHeight`, ảnh `NativeAdCard_small_light`, `NativeAdCard_small_dark`, `CollapsibleNativeAd_collapsed_light`, `CollapsibleNativeAd_collapsed_dark`. `:feature:sample`: 24 test, 4 đỏ, đều là ảnh `SampleScreen_ready_light`, `_ready_dark`, `_ready_small`, `_ad-loading_light`; `SampleScreen_disabled_light` qua. `:app` (staging): 84 test, 7 đỏ: ảnh `AdGalleryScreen_default_light`, `_default_dark`, `_default_small`, `_inlist_light`, `_inlist_small`, `_collapsible-collapsed_light` và `AdGalleryScreenTest.inList_insertsAdsAfterEveryNItems` (đo chiều cao dòng quảng cáo, `AdGalleryScreenTest.kt:78`). `NetworkLostScreen_*`, `AdGalleryScreen_collapsible-expanded_*`, ảnh `LoadingScreen_*` qua: không vẽ native nhỏ. Mọi test và ảnh đỏ đã có trong mục 3.2 (thêm `AdGalleryScreenTest.kt` sau lần chạy này). Worktree tạm đã xóa.

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
- Thêm: `AdTelemetry.kt` (T-023 tạo; T-028 pool cũng dùng `perRequestLoad`), `CollapsibleNativeAdSlot` dùng `AdsController.nativeAd` (không đổi đường ở task này).
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
   - Đề xuất của AI: vẽ 36 dp như thiết kế và KB-16, giữ bố cục 36 dp và dựa vào việc Compose tự nới vùng nhận chạm tới `minimumTouchTargetSize` 48 dp (không dùng `minimumInteractiveComponentSize()`, vì nó nới bố cục và đẩy media xuống). Chưa chạy thử trên Robolectric: nếu test chạm cách mép 5 dp không qua thì phương án thay là bọc nút trong vùng chạm 48 dp đặt lệch âm 6 dp (`Modifier.layout`, báo cỡ 36 dp) để bố cục không đổi. Người duyệt muốn chạy thử trước khi duyệt thì ghi ở mục 9.
6. **Vị trí nhãn "Ad".** O-08 ghi "đặt ở góc trên trái", thiết kế vẽ lại (T-035) đặt nhãn ở đầu dòng mô tả, dưới tiêu đề.
   - Đề xuất của AI: theo thiết kế đã duyệt (nhãn luôn thấy, không đè, cùng chỗ với AdsKit `adskit_native_small.xml` nếu dev xác nhận); nếu luật đúng thì T-035 vẽ lại trước khi chạy task này.
7. **Đăng ký khe native vào kế hoạch nạp sẵn (HD-02, S-22, T-021 câu 1).** T-021 ghi "T-027 đăng ký khe native vào kế hoạch"; ticket không nêu.
   - Đề xuất của AI: không làm ở task này (khe nạp khi lên màn như hôm nay, khung chờ lúc chờ lượt đầu); nạp sẵn native đi với pool ở T-028, vì `AdPreloader.kt` vừa đổi ở T-037, T-038.
8. **Chạy thử:** đã chạy thử L-089 (mục 4.1), mọi test đỏ đã vào mục 3.2. Chưa chạy `scripts/check.sh` đầy đủ, chưa thử `VideoOptions` và vùng chạm nút đóng trên Robolectric (E là 1); người duyệt muốn chạy thêm thì ghi ở mục 9.
9. **Rà soát độc lập:** đã rà soát 2026-10-25: 4 chặn, 11 nên sửa, 6 ghi chú. Đã sửa: chặn 1 (tên test thật, hàm thuần `nativeSlotAllowed` và test), chặn 2 (`NativeLoadUse.REFRESH`, `AdTelemetry.kt` và test vào 3.2), chặn 3 (cổng gửi chung `NativeSendGate.kt`, sáu đường, mỗi đường test ở nền và cổng đóng), chặn 4 (`NativeAdSources` nhận phụ thuộc qua tham số, test cờ tắt khi đang làm mới); nên sửa 5 (chỉ `NATIVE_SMALL` qua nguồn, hồi quy thu gọn), 6 (nguồn đóng khi khe cuối rời màn; làm mới lấy từ nguồn, đợi 10 giây), 7 (vùng chạm không đổi bố cục, test vị trí media), 9 (theo CL-02: bỏ nhịp ở nền), 10 (ngoại lệ L-055 kèm đối chứng, bỏ test gần trùng), 11 (`newMediaView`), 12 (`nativeFullScreenCache`), 13 (phần lệch medium, sửa mục 3 README), 14 (thẻ `FULL` số dòng cố định), 15 (`retryResets` cho thử lại của làm mới). Chuyển thành câu hỏi: 7 (câu 10), 8 (câu 11), 6 (câu 12), 9 (câu 9 vẫn cần người xác nhận, xem câu 14), cách ghi số liệu lượt nạp tiếp (câu 13).
10. **Góc nút đóng toàn màn hình.** KB-16 ghi vòng đếm ngược 36 dp "ở góc trên phải", trùng góc AdChoices mà O-08 dành cho SDK; thiết kế vẽ lại đặt nút trên trái (16, 24).
    - Đề xuất của AI: theo thiết kế (trên trái), vì góc trên phải là của AdChoices; sửa chữ KB-16 ở lượt sửa spec sau. Đếm ngược là của T-026, nên T-026 cũng theo câu trả lời này.
11. **`keepSpaceWhenEmpty` còn giữ không?** Spec đã gạch tiêu chí "vị trí cạnh nút thì giữ khoảng trống", tiêu chí mới là "co về 0 dp"; CLAUDE.md bước 7 và `:feature:sample` vẫn nhắc tham số này.
    - Đề xuất của AI: giữ ở task này (không vị trí nào của dự án mẫu bật nó; gỡ là đổi API công khai mà CLAUDE.md hướng dẫn, nên cần người quyết). Nếu muốn gỡ, làm một task riêng sửa CLAUDE.md và `ads-guide.md`.
12. **Nguồn giữ quảng cáo chờ tới đâu?** O-01 nói "lấy ra thì khe tự nạp cái mới", KB-1 nói rời màn thì "không còn giữ bộ nhớ".
    - Đề xuất của AI: nguồn chỉ giữ quảng cáo chờ khi còn ít nhất một khe của vị trí trên màn; khe cuối rời màn thì hủy quảng cáo chờ và lượt đang bay. Quảng cáo chờ là quảng cáo cho nhịp làm mới kế tiếp (CL-02: "nạp quảng cáo chờ, đợi tối đa 10 giây, rồi đổi vào"). Phương án khác: giữ quảng cáo chờ qua các màn (vào màn sau có quảng cáo ngay), chấp nhận giữ bộ nhớ, trái chữ của KB-1.
13. **Số liệu của lượt nạp tiếp sau khi lấy.** Luật 8.2 chỉ nói lượt làm mới native nhỏ không có `load_attempt`.
    - Đề xuất của AI: lượt nạp tiếp (cho nhịp làm mới) ghi như lượt làm mới (`REFRESH`, không `load_attempt`, vị trí hiện tại); lượt đầu và thử lại 5 giây ghi `SINGLE_AD` có `load_attempt`.
14. **Nhịp làm mới ở nền.** Bản trước của plan dừng đếm và giữ thời gian đã đếm; CL-02 nói "ở nền thì bỏ nhịp". Plan nay theo CL-02.
    - Đề xuất của AI: theo CL-02 (nhịp rơi lúc ở nền thì bỏ, không dồn, nhịp sau 60 giây nữa). Về foreground không có lượt làm mới ngay.
15. **KB-2 tiêu chí 3 (hai ô medium khác nhau) đẩy sang T-028**, nhưng ticket T-028 chỉ ghi `scenarios: [KB-3, KB-21]`.
    - Đề xuất của AI: người thêm KB-2 vào ticket T-028 (AI không sửa ticket khác).

**Ghi chú của rà soát độc lập**

- (16) Chạy thử chỉ đổi 72 thành 80; test cũ đỏ vì CTA 34, icon 36 và số đo toàn màn hình (`cta_is36dpHigh`, `texts_followDesign`, `closeButton_isAtLeast48dp`) chưa chạy, đã ghi tên ở mục 3.2 và mục 4.1.
- (17) KDoc `CollapsibleNativeAd.kt:189` ghi "72 dp collapsed": dòng 3.2 đã sửa cho đúng; `SampleScreenTest.kt:83-99` ô giả 72 dp không đỏ nhưng sai số: đã thêm vào 3.2.
- (18) `BOTTOM_REFRESH_DELAY_AFTER_COLLAPSIBLE_MILLIS` không dùng ở task này: đã bỏ, để T-030.
- (19) Cỡ khoảng 30M, khoảng 60 dòng ở 3.2; người chọn tách (câu 1) thì cần plan phiên bản 2: quyết câu 1 trước khi duyệt.
- (20) KB-2 tiêu chí 3 đẩy sang T-028: ticket T-028 chưa ghi (câu 15).
- (21) `bottomSlotReserve_equalsDrawnHeight` đổi tên `bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight`, ghi phạm vi: chừa chỗ thật (D-04) là của T-030.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
