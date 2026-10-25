---
ticket: T-027
title: Native một quảng cáo theo kích thước AdsKit: khe đáy, đầy đủ, toàn màn hình
status: approved                # draft | approved | stopped | done
version: 2                   # 2: anh Nhật trả lời 15 câu (2026-10-25), tách phần B sang T-040; plan chỉ còn phần A
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M sau khi tách, số gợi ý 15.0M (scripts/token-usage.py estimate, 2026-10-25, tạm vì chưa đủ 5 task M)
blocking_points: [ads, core]
run_order: 1                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Nhật              # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-027: NATIVE MỘT QUẢNG CÁO THEO KÍCH THƯỚC ADSKIT: KHE ĐÁY, ĐẦY ĐỦ, TOÀN MÀN HÌNH

## 1. Tóm tắt

**Phiên bản 2 (2026-10-25, chờ duyệt).** Anh Nhật trả lời 15 câu hỏi của phiên bản 1, cả 15 câu theo đề xuất của AI. Câu 1: tách ticket. T-027 giữ phần A (giao diện, kích thước, ảnh chuẩn; O-06 tới O-08). Phần B (nguồn một quảng cáo, hủy, hết hạn 55 phút, làm mới 60 giây, lỗi mã 0, số liệu `REFRESH`; O-01 tới O-05, O-09) chuyển sang ticket mới T-040, có plan riêng. Thay đổi so với phiên bản 1: bỏ mọi file, test, quyết định của phần B (danh sách chuyển ở mục 6); câu hỏi đã trả lời chuyển thành quyết định ở mục 2, 3; chạy thử lại trên code hiện tại, thêm chạy thử vùng chạm nút đóng 36 dp (mục 4.1); thêm đường gửi native toàn màn hình vào O-06 (mục 3.1, A4; câu hỏi 1 mới); `token_estimate` 30M thành 15M; chấm B từ 2 xuống 1.

Đổi native nhỏ của `:core:ads` sang khe đáy 80 dp của AdsKit cho mọi native nhỏ (một hằng số cho chiều cao vẽ và chiều cao chừa chỗ), thêm composable native đầy đủ `NativeAdSize.FULL` (chưa có vị trí), đổi số đo native toàn màn hình theo thiết kế vẽ lại (T-035, nút đóng 36 dp góc trên trái, vùng chạm 48 dp không đổi bố cục); tùy chọn yêu cầu, cách co giãn ảnh, nhãn "Ad" và hàng sao theo O-06 tới O-08. Kiểm chứng bằng unit test hàm thuần, test giao diện Robolectric đo kích thước và màu, và ghi lại ảnh chuẩn screenshot của `:core:ads`, `:feature:sample`, `:app` có native nhỏ hay native toàn màn hình.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (luật chi tiết: `docs/ops/ads-business-rules.md`, mục 5.6, 5.7, 11):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices; vùng quảng cáo có nền hoặc viền tách khỏi nội dung; CTA khác nút chính của app. | Giữ như T-010; đổi số đo theo 5.7: icon 36 dp, tiêu đề 14 sp, mô tả 12 sp, CTA cao 34 dp chữ 13 sp, nhãn "Ad" 10 sp, không có media. AdChoices góc trên phải (O-08). Nhãn "Ad" đặt theo thiết kế vẽ lại: đầu dòng mô tả, dưới tiêu đề (quyết định Q6). |
| KB-1 | Khi native nhỏ ở đáy thì vùng quảng cáo cao 80 dp; khi đang tải thì giữ đúng chiều cao đó với hiệu ứng chờ. | `NativeAdDimens.BottomSlotHeight = 80.dp` dùng cho thẻ, khung chờ và khoảng giữ chỗ (D-02, D-04, câu hỏi 22 của spec). Một cỡ 80 dp cho mọi native nhỏ, kể cả native nhỏ trong danh sách và dạng thu gọn của native thu gọn (quyết định Q4). |
| KB-1 | Khi nạp lỗi thì vùng quảng cáo co về 0 dp, nội dung không chừa khoảng trống cho nó. | Đã có từ T-021 (`Unavailable` không vẽ gì, trừ `keepSpaceWhenEmpty`, giữ tham số này: quyết định Q11). Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit` để báo app "có"/"không có" quảng cáo (D-02); T-030 dùng để chừa chỗ (D-04). |
| KB-1 | Màu lấy từ hệ thống thiết kế; chữ quảng cáo luôn đọc được trên nền của vùng quảng cáo, kể cả khi máy ở chế độ tối. | Màu theo token như T-010 (`docs/features/ads/design/README.md`, mục 3). Test đo màu chữ tiêu đề và CTA trên nền thẻ ở cả hai chế độ (vẽ `decorView` ra `Bitmap`, CLAUDE.md bước 10). |
| KB-1 | Khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ. | Không thuộc task này: T-040 (làm mới 60 giây, CL-02, O-05). |
| KB-1, trường hợp đặc biệt | Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ. | Đã có (T-010, T-020); giữ, tách điều kiện thành hàm thuần `nativeSlotAllowed` có test. |
| KB-1, trường hợp đặc biệt | Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ. | Quảng cáo đang hiện đã được hủy khi rời composition (T-021, `NativeAdSlotTest.leavingComposition_destroysAd`); giữ làm test hồi quy. Quảng cáo chờ và quảng cáo về muộn (O-01, O-03) là việc của T-040. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt. | Áp cho native đầy đủ (media cao 200 dp, icon 40 dp, tiêu đề 14 sp) và toàn màn hình. Cách co giãn ảnh đặt trước khi gắn quảng cáo (O-07). Native medium giữ số đo hiện tại; số đo mới của medium đi với pool ở T-028 (quyết định Q3). |
| KB-2 | Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1. | Như các dòng KB-1 ở trên, cho native đầy đủ: khung chờ giữ chiều cao của thẻ, lỗi thì co về 0 (đường `Unavailable` chung của `NativeAdSlot`). |
| KB-2 | Khi hai ô medium cùng hiện trên màn thì mỗi ô có một quảng cáo khác nhau. | Không thuộc task này: T-028 (pool; KB-2 đã thêm vào ticket T-028, quyết định Q15). |

Luật của ticket, ngoài tiêu chí chấp nhận (mỗi luật có test ở mục 4):

- O-06: tùy chọn yêu cầu: video tắt tiếng, không có điều khiển riêng, một ảnh, media ngang. Áp cho mọi đường gửi yêu cầu native: `SdkNativeAdLoader.load` (nhỏ, medium, đầy đủ, thu gọn, trong danh sách) và nạp sẵn native toàn màn hình trong `AdsController` (câu hỏi 1).
- O-07: đặt cách co giãn ảnh trước khi gắn quảng cáo vào khung.
- O-08: nhãn "Ad" luôn thấy được (vị trí theo thiết kế, Q6); AdChoices do SDK ghim ở góc trên phải, không bị đè; hàng sao chỉ hiện khi điểm lớn hơn 0, sao đặc làm tròn theo điểm, tối đa 5, không vẽ nửa sao.
- D-02, D-04, AR-02: 80 dp cho cả khe và chừa chỗ, một hằng số; test so hai giá trị.

Test nghiệm thu gợi ý `AT-NT-*` thuộc bản đặc tả độc lập của AdsKit, không có trong repo (như T-021 đã ghi); plan dùng các luật trên làm danh sách test.

**Quyết định đã chốt (anh Nhật, 2026-10-25, theo đề xuất của AI ở phiên bản 1):**

- Q1: tách; phần B thành T-040.
- Q2: native đầy đủ chỉ là composable `NativeAdCard(assets, NativeAdSize.FULL)` và khung chờ, có screenshot test; không thêm `AdFormat.NATIVE_FULL` hay vị trí (MC-02 là danh sách đóng). Muốn có vị trí thì sửa MC-02 trước, một task sau thêm.
- Q3: native medium giữ số đo và ảnh chuẩn `NativeAdCard_medium_*`; T-028 đổi số đo medium.
- Q4: một cỡ 80 dp cho mọi native nhỏ; ghi lại các ảnh đổi theo chiều cao (`CollapsibleNativeAd_collapsed_*`, `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_inlist_*`), không đổi bố cục lớp thu gọn 250 dp (T-030).
- Q5: nút đóng toàn màn hình vẽ 36 dp, bố cục 36 dp, vùng nhận chạm 48 dp do Compose tự nới (`minimumTouchTargetSize`), không dùng `minimumInteractiveComponentSize()`. Đã chạy thử trên Robolectric: qua (mục 4.1).
- Q6: nhãn "Ad" theo thiết kế vẽ lại (đầu dòng mô tả). Ticket ghi "góc trên trái" theo chữ của O-08 trước khi có câu trả lời; plan theo câu trả lời.
- Q7: không đăng ký khe native vào kế hoạch nạp sẵn (HD-02); việc đó đi với pool ở T-028.
- Q10: nút đóng toàn màn hình ở góc trên trái (16, 24) theo thiết kế, vì góc trên phải là của AdChoices; chữ KB-16 sửa ở lượt sửa spec sau; T-026 (đếm ngược) theo cùng góc.
- Q11: giữ `keepSpaceWhenEmpty`.
- Q8, Q9 (chạy thử, rà soát độc lập) đã xử lý trong plan này; Q12, Q13, Q14 thuộc phần B, chuyển sang T-040; Q15: KB-2 đã thêm vào ticket T-028.

Thiết kế tham chiếu: `features/ads/design/README.md` (vẽ lại T-035, 2026-10-25), các ảnh trong `design/screens/`: `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light` (khe đáy 80 dp), `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` (toàn màn hình), `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light` (có native nhỏ). Số liệu lấy từ `design/context/` cùng tên. Native đầy đủ chưa có ảnh màn hình (Q2): số đo lấy từ mục 5.7 của luật.

ADR liên quan: ADR-0004, ADR-0008, ADR-0013, ADR-0019, ADR-0020, ADR-0022, ADR-0026, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn của `:core:ads`: composable không trạng thái nhận `NativeAdAssets` (`NativeAdCard`, T-010), luồng trạng thái `Flow<NativeAdState>` gom trong composition (`NativeAdSlotHost`), hàm thuần có unit test cho mọi luật (`AdRetryPolicy`, T-021).

1. `NativeAdCard.kt`:
   - `enum class NativeAdSize { SMALL, MEDIUM, FULL }`: thêm `FULL` (native đầy đủ). Mọi `when` trên `NativeAdSize` thêm nhánh `FULL` (`NativeAdDimens.heightOf`, `NativeAdLayout`, `NativeAdPlaceholderLayout`; `SdkNativeAdLoader` chuyển sang hàm thuần ở A4). Chạy thử L-089 ở mục 4.1: không có `when` nào ngoài `:core:ads`.
   - `NativeAdDimens`: `BottomSlotHeight = 80.dp` (một hằng số; `SmallHeight` đổi tên thành `BottomSlotHeight`), `SmallIconSize = 36.dp`, `SmallCallToActionHeight = 34.dp`, `FullMediaHeight = 200.dp`, `FullIconSize = 40.dp`. Cỡ chữ ngoài bảng kiểu chữ (13 sp CTA nhỏ, 10 sp nhãn "Ad") là hằng số riêng của composable quảng cáo, dựng bằng `MaterialTheme.typography.<kiểu>.copy(fontSize = ...)` (README thiết kế, đoạn T-035). Màu giữ theo token (README, mục 3).
   - `SmallLayout`: theo `design/context/SampleScreen_ready_light.md`: hàng ngang, lề trái 12, lề phải 24, icon 36, khối chữ (tiêu đề `titleSmall` 14 sp, dòng nhãn "Ad" 10 sp và mô tả `bodySmall` 12 sp cách 2 dp), CTA 34 dp chữ 13 sp, AdChoices góc trên phải.
   - `FullLayout` (mới): cột: đầu (icon 40, tiêu đề 14 sp, dòng nhãn), media cao 200 dp (`NativeAdMediaArea`), mô tả, CTA, hàng sao theo O-08.
   - `NativeAdPlaceholderLayout`: khung chờ của `SMALL` cao 80 dp, khối CTA giả cao `SmallCallToActionHeight` (34 dp) như ảnh thiết kế `SampleScreen_ad-loading_light`; của `FULL` cao cố định `FullPlaceholderHeight`. Để thẻ `FULL` cũng cao cố định, tiêu đề một dòng, dòng nhãn một dòng, mô tả cố định 2 dòng (`minLines = maxLines = 2`), CTA một dòng; "cao theo nội dung" của 5.7 nghĩa là theo bố cục, không theo độ dài chữ (rà soát độc lập phiên bản 1, điểm 14). Có test chữ ngắn và chữ dài.
   - Hàm thuần `nativeStarCount(rating: Double?): Int` (O-08): `null`, `<= 0` hay NaN trả 0; còn lại `round(rating)` kẹp trong 0..5. `NativeAdAssets` thêm trường `starRating: Double? = null` (giá trị mặc định, nên nơi gọi cũ không đổi); `SdkLoadedNativeAd` đọc `nativeAd.starRating`.
2. `NativeAdSlot.kt`: tách điều kiện hiện khe thành hàm thuần `nativeSlotAllowed(canRequestAds: Boolean, enabled: Boolean, unitId: String?): Boolean` mà `NativeAdSlot` gọi (KB-1, trường hợp đặc biệt 1). `requireNativeFormat` giữ `NATIVE_SMALL -> SMALL`, `NATIVE_MEDIUM -> MEDIUM`; không có định dạng nào ra `FULL` (Q2). `Unavailable` với `keepSpaceWhenEmpty` giữ `BottomSlotHeight`. Thêm tham số `onAdAvailabilityChange: (Boolean) -> Unit = {}` (D-02): gọi `true` khi trạng thái chuyển sang `Loaded`, `false` khi chuyển sang `Unavailable`, không gọi ở `Loading`; và gọi `false` khi `nativeSlotAllowed` đổi từ true sang false sau một lần đã báo `true` (rút đồng ý hay vị trí tắt giữa chừng, L-061). Logic nằm trong `NativeAdSlot.kt`: `NativeAdSlot` đọc `canRequestAds`, `enabled`, `unitId` rồi gọi composable internal mới `NativeAdSlotGate(allowed: Boolean, states, size, keepSpaceWhenEmpty, onAdAvailabilityChange, adContent)`, nơi giữ cờ "đã báo true" (`remember`) và báo `false` khi `allowed` thành false; `NativeAdSlotHost` nhận thêm tham số internal `onStateChange: (NativeAdState) -> Unit` để báo `Loaded`/`Unavailable` lên `NativeAdSlotGate`. Test dựng `NativeAdSlotGate` với `allowed` là `mutableStateOf`, không cần `AdsController`.
3. `NativeFullScreenAdScreen.kt`: số đo theo `design/context/NativeFullScreenAdScreen_default_light.md`: nút đóng 36 dp tròn ở góc trên trái, tại (16, 24) (biểu tượng 24 dp; README mục 3 cũ ghi 48 dp), `TopBarHeight` 36 dp nên media bắt đầu ở y = 76, icon 48 dp, tiêu đề `titleMedium` 16 sp, CTA cao 52 dp, AdChoices góc trên phải. Đếm ngược 3 giây là của T-026. Nút đóng: thay `IconButton` (tự thêm `minimumInteractiveComponentSize()`, nới bố cục lên 48 dp) bằng `Box(Modifier.size(36.dp).clip(CircleShape).background(surfaceContainerHigh).clickable(onClick = onClose))` chứa `Icon`; giữ `testTag`, `contentDescription`. Compose tự nới vùng nhận chạm của nút có `pointerInput` nhỏ hơn `ViewConfiguration.minimumTouchTargetSize` (48 dp) khi không có thành phần chạm khác chồng lên, không đổi bố cục (Q5, đã chạy thử).
4. `NativeRequestOptions.kt` (mới, internal, O-06): `data class NativeRequestOptions(startMuted, customControls, multipleImages, mediaAspect)`; `nativeRequestOptions(size: NativeAdSize)` (`startMuted = true`, `customControls = false`, `multipleImages = false`; `mediaAspect = LANDSCAPE` cho `MEDIUM`, `FULL`; `SMALL` không xin media nên `ANY`); `fullScreenNativeRequestOptions()` (như trên, `mediaAspect = ANY`, câu hỏi 1); `NativeRequestOptions.toNativeAdOptions(): NativeAdOptions` chép vào `NativeAdOptions.Builder` (AdChoices `ADCHOICES_TOP_RIGHT`) và `VideoOptions.Builder().setStartMuted(...).setCustomControlsRequested(...)`. Đây là nơi duy nhất trong `src/main` dựng `NativeAdOptions` (L-072: mọi đường gửi native gọi chung một hàm).
   - `SdkNativeAdLoader.kt`: `load` dùng `nativeRequestOptions(size).toNativeAdOptions()` thay cho khối `when (size)`.
   - `AdsController.kt`: khối nạp sẵn native toàn màn hình (hôm nay dựng `NativeAdOptions.Builder` riêng, `ANY`, chỉ `setStartMuted(true)`) dùng `fullScreenNativeRequestOptions().toNativeAdOptions()`; bỏ import không còn dùng. Chỉ đổi khối này, không đổi luồng nạp.
5. `NativeAdViewBinding.kt` (O-07): hôm nay `MediaView` được tạo ngay trong `factory` và đặt `FIT_CENTER` ở đó, nên test không chen được. Thêm tham số internal `newMediaView: (Context) -> MediaView = ::MediaView` cho composable gắn media, và tham số `bind` (đã có ở `NativeAdViewHost`) cho test truyền bộ ghi; test truyền một `MediaView` con ghi lời gọi `setImageScaleType` và một `bind` ghi lời gọi `setNativeAd`, rồi so thứ tự. `FIT_CENTER` vẫn đặt sau khi tạo, trước khi gắn.
6. `res/values/strings.xml` của `:core:ads`: chuỗi mô tả hàng sao cho trợ năng (`ads_native_star_rating`, "%1$d of 5 stars"); nhãn "Ad" đã có.
7. Ảnh chuẩn: ghi lại ảnh có native nhỏ, native toàn màn hình (danh sách đủ ở mục 3.2, đúng các ảnh đỏ ở chạy thử, mục 4.1).
8. `docs/ops/ads-guide.md`, mục native: khe đáy 80 dp, một hằng số `BottomSlotHeight`; tham số `onAdAvailabilityChange`; `NativeAdSize.FULL` chưa có định dạng; tùy chọn yêu cầu chỉ dựng ở `NativeRequestOptions.kt`. `docs/features/ads/design/README.md`: mục 3 sửa số cũ, mục 8 ghi kết quả đối chiếu.

Ghi log: không thêm dòng log; các composable và hàm thuần của task này không có hành động hay lỗi cần ghi (ADR-0019), đường nạp đã có log từ T-021.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | A1: `NativeAdSize.FULL`, `NativeAdDimens` theo 5.7, `SmallLayout`, `FullLayout`, khung chờ, `nativeStarCount`, `NativeAdAssets.starRating` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | A2: `nativeSlotAllowed`, `NativeAdSlotGate`, `onAdAvailabilityChange` (cả khi vị trí tắt sau `true`), `NativeAdSlotHost.onStateChange`, giữ chỗ bằng `BottomSlotHeight`; dùng chung với T-040 (mục 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | A3: số đo theo thiết kế vẽ lại, nút đóng 36 dp bằng `Box` + `clickable` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeRequestOptions.kt` | A4: `NativeRequestOptions`, `nativeRequestOptions`, `fullScreenNativeRequestOptions`, `toNativeAdOptions` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | A4: `load` dùng `nativeRequestOptions(size).toNativeAdOptions()`; `starRating` vào `NativeAdAssets` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | A4: chỉ khối `NativeAdOptions` của nạp sẵn native toàn màn hình gọi `fullScreenNativeRequestOptions().toNativeAdOptions()`, bỏ import thừa (T-024 đã merge, khối ở dòng 1050; mục 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | A5: tham số `newMediaView` để test chen bộ ghi lời gọi (O-07) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | KDoc `CollapsibleNativeAdContent` (dòng 189) "72 dp collapsed" thành 80 dp; dạng thu gọn vẽ bằng `NativeAdSize.SMALL` nên chiều cao đổi theo, không đổi hành vi thu gọn (T-030) |
| Sửa | `core/ads/src/main/res/values/strings.xml` | A6: `ads_native_star_rating` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Bước 3: `small_is72dpHigh` thành `small_is80dpHigh`, `longTexts_keepHeight` 80 dp, `cta_is36dpHigh` thành `small_ctaIs34dpHighWith13spText`, `placeholder_matchesCardHeight_forEachSize` có chiều cao riêng cho `FULL` (test lặp `NativeAdSize.entries`, L-089); số đo icon, cỡ chữ; test `FULL`; hàng sao; màu chữ hai chế độ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Bước 3: 72 dp thành 80 dp (`loading_showsPlaceholder`, `unavailable_takesNoSpace`, `unavailable_keepsSpace_whenDeclaredNextToButton`, `buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace`, `boundNativeAd_withFakeAd_drawsCard`); `nativeSlotAllowed`; `onAdAvailabilityChange`; hằng số khe bằng chiều cao vẽ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Bước 3: `sdkLayout_keepsCardHeights` thẻ nhỏ 72 thành 80 dp (dòng 114) và CTA nhỏ 36 thành 34 dp (dòng 115; CTA medium dòng 120 giữ 36 dp, Q3); thứ tự O-07 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Bước 3: dạng thu gọn 72 thành 80 (năm test đỏ ở chạy thử: `collapsed_reloading_keepsSmallPlaceholderThenStaysCollapsed`, `tapCollapse_showsSmallLayout`, `collapsed_survivesStateRestoration`, `collapsed_hasNoExpandControl`, `collapsed_newAdLoaded_staysCollapsed`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Bước 3: `loadingSlot_showsPlaceholderOfSmallHeight` 72 thành 80 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Bước 3: `texts_followDesign` (CTA 48 thành 52 dp, icon 48 dp, tiêu đề 16 sp), `closeButton_isAtLeast48dp` thành `closeButton_drawn36dp_touchAtLeast48dp`; `closeButton_isTopLeft_awayFromAdChoices` giữ (16, 24); thêm `media_startsAt76dp`; KDoc lớp "48 dp" thành "36 dp" |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Thêm ảnh `NativeAdCard_full_light`, `NativeAdCard_full_dark`, `NativeAdPlaceholder_small_light` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | Quảng cáo giả có `starRating`; một bộ chữ dài cho `FULL` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRequestOptionsTest.kt` | Test O-06, hai đường gửi |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStarCountTest.kt` | Test O-08, hàng sao |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Bước 3: `inList_insertsAdsAfterEveryNItems` dòng quảng cáo nhỏ 104 thành 112 dp (80 + 16 + 16), chú thích dòng 127 (đỏ ở chạy thử, mục 4.1) |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenTest.kt` | Ô giả 72 dp (dòng 83 tới 85) thành 80 dp cho đúng số; test không đỏ (ghi chú rà soát 17 của phiên bản 1) |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | Khe đáy 80 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_light.png` | Native đầy đủ |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_full_dark.png` | Như trên |
| Tạo | `core/ads/src/test/screenshots/NativeAdPlaceholder_small_light.png` | Khung chờ 80 dp, chụp với `animated = false` |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_light.png` | Số đo mới (đỏ ở chạy thử) |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_collapsed_light.png` | Dạng thu gọn 80 dp (đỏ ở chạy thử) |
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
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Dạng thu gọn 80 dp (đỏ ở chạy thử). Ảnh này cũng là ảnh thiết kế: `docs/features/ads/design/review/AdGalleryScreen_collapsible-collapsed_light_compare.png` và dòng của nó trong `summary.md`, mục 8 README sẽ cũ; T-030 sở hữu và ghi lại (ảnh thiết kế của ticket khác, mục 4), task này không sửa |
| Sửa | `docs/ops/ads-guide.md` | Mục 3 (Định dạng: `NativeAdSize.FULL` chưa có định dạng; tùy chọn yêu cầu chỉ dựng ở `NativeRequestOptions.kt`) và mục 4 (Native: bảng small 72 thành 80 dp ở dòng 62, icon 36, CTA 34; `BottomSlotHeight`; `onAdAvailabilityChange`); 3.1, bước 8 |
| Sửa | `docs/features/ads/design/README.md` | Mục 3: số cũ (CTA 36 dp, icon 40 dp, khung chờ 72 dp, nút đóng 48 dp) theo số vẽ lại; mục 8: kết quả đối chiếu của `SampleScreen_*`, `NativeFullScreenAdScreen_*`, `AdGalleryScreen_default_*`, `AdGalleryScreen_inlist_light`, kèm phần lệch dự kiến của medium |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ready_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ready_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/SampleScreen_ad-loading_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_default_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_default_dark_compare.png` | Ảnh so sánh với thiết kế vẽ lại |
| Ghi lại | `docs/features/ads/design/review/AdGalleryScreen_inlist_light_compare.png` | Ảnh so sánh với thiết kế vẽ lại |

Không sửa: `InListAds.kt` (không có hằng số 72 dp riêng; khoảng giữ chỗ dùng `NativeAdPlaceholder(NativeAdSize.SMALL)`, `grep -rn "72" core/ads/src/main/kotlin` 2026-10-25 chỉ thấy `SmallHeight` và KDoc ở `CollapsibleNativeAd.kt:189`). `feature/sample/src/main/.../SampleScreen.kt` và `feature/sample/src/test/.../SampleScreenScreenshotTest.kt` dùng `NativeAdSize.SMALL`, `NativeAdPlaceholder(NativeAdSize.SMALL, ...)` qua tên, không `when` trên enum, nên thêm `FULL` không làm hỏng biên dịch (chạy thử ở mục 4.1); `app/src/testStaging/.../PreviewAdGallerySlots.kt` cũng vậy (`if`, không `when`). `scripts/tests/fixtures/release/AdPlacement.kt` là bản sao cho test của `release.sh`, không đọc `NativeAdSize`; plan không thêm dòng vào `AdPlacement` hay `AdFormat`. Người đọc ảnh chuẩn (L-020): `scripts/check.sh` đọc mọi ảnh trong `src/test/screenshots/` theo module (không theo tên), công cụ so ảnh đọc ảnh cùng tên ảnh thiết kế; `scripts/tests/run.sh` chép `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png` làm dữ liệu mẫu cho test của công cụ so ảnh (cặp ảnh giống nhau, cặp ảnh khác nhau), không so với nội dung cố định, nên ghi lại hai ảnh này không làm đỏ test của script; không script nào khác đọc tên các ảnh này (`grep -rn "NativeAdCard_small\|SampleScreen_ready\|AdGalleryScreen_default\|NativeFullScreenAdScreen_" scripts/ .github/`, 2026-10-25: chỉ `scripts/tests/run.sh` như trên).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; không đổi tần suất. Đổi kích thước native nhỏ, native toàn màn hình, thêm composable native đầy đủ (chưa có vị trí), đổi tùy chọn yêu cầu native (O-06): điểm chặn 1 (đổi định dạng). Vị trí dùng: `SAMPLE_NATIVE_SMALL`, `GALLERY_NATIVE_SMALL`, `GALLERY_NATIVE_FULL_SCREEN` (đã có).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `NativeAdOptions`, `VideoOptions`, `NativeAd.getStarRating`, `MediaView.setImageScaleType` không cần quyền ngoài `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có từ Google Mobile Ads.
- **Thư viện và SDK:** Không thêm. `VideoOptions`, `NativeAdOptions`, `MediaAspectRatio`, `NativeAd` thuộc `play-services-ads` (`libs.play.services.ads`, đã có trên classpath biên dịch của `:core:ads`; getter `getMediaAspectRatio`, `shouldRequestMultipleImages`, `getAdChoicesPlacement`, `getVideoOptions`, `VideoOptions.getStartMuted`, `getCustomControlsRequested` có trong `play-services-ads-api-25.5.0`, tra bằng `javap` 2026-10-25); Compose, Robolectric, Roborazzi đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Điểm chưa chắc của phiên bản 1 (vùng chạm 48 dp của nút đóng 36 dp mà không đổi bố cục) đã chạy thử trên Robolectric và qua (mục 4.1).

## 4. Kiểm thử

Test viết trước code (auto-run.md, bước 3). Test có kiểm kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Test kiểm màu vẽ `decorView` ra `Bitmap` (CLAUDE.md, bước 10).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 (icon, tiêu đề, CTA, "Ad", AdChoices) | Compose UI test | `NativeAdCardTest.small_showsHeadlineAdBadgeAdvertiserAndCta`, `small_leavesAdChoicesCornerFree` (đã có, giữ); `small_iconIs36dp`, `small_ctaIs34dpHighWith13spText` (thay `cta_is36dpHigh`), `small_badgeTextIs10sp_belowHeadline` (nhãn đầu dòng mô tả, dưới tiêu đề, Q6); `small_hasNoMedia` (đã có, giữ) |
| KB-1, tiêu chí 2 (80 dp, khung chờ cùng chiều cao) | Compose UI test | `NativeAdCardTest.small_is80dpHigh` (thay `small_is72dpHigh`); `NativeAdSlotTest.loading_showsPlaceholder` (72 thành 80); `NativeAdSlotTest.bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight` (AR-02, phạm vi: chỉ so hằng số `NativeAdDimens.BottomSlotHeight` với chiều cao đo được của thẻ và khung chờ; chừa chỗ thật dưới nội dung, D-04, là test của T-030) |
| KB-1, tiêu chí 3 (lỗi thì 0 dp, báo app) | Compose UI test | `NativeAdSlotTest.unavailable_takesNoSpace` (đã có, đối chứng 80 dp khi đang tải); `availability_reportsTrueOnLoadedFalseOnUnavailable_notOnLoading`; `availability_falseThenTrue_whenAdComesAfterFailure`; `availability_reportsFalse_whenPlacementTurnsOffAfterLoaded` (`NativeAdSlotGate`, `allowed` true rồi `Loaded`, sau đó `allowed` thành false: chuỗi báo đúng `[true, false]`; đối chứng trong cùng test: trước khi đổi `allowed` chuỗi là `[true]`) |
| KB-1, tiêu chí 4 (màu, chữ đọc được ở chế độ tối) | Compose UI test | `NativeAdCardTest.small_headlineAndCtaColors_lightAndDark`: ở mỗi chế độ, đọc điểm ảnh giữa nền thẻ và giữa nền CTA, so với `surfaceContainerLow`, `tertiaryContainer`; đọc điểm ảnh trên nét chữ tiêu đề và nét chữ nhãn CTA (quảng cáo giả có chữ đậm một ký tự lớn như "█" hay đo điểm tối nhất trong vùng chữ), so với `onSurface` và `onTertiaryContainer` của chế độ đó (sai số mỗi kênh 8), và độ tương phản chữ với nền ít nhất 4.5:1 (qua ngay, ngoại lệ kèm đối chứng) |
| KB-1, tiêu chí 5 (làm mới 60 giây) | Không thuộc task này | T-040 |
| KB-1, trường hợp đặc biệt 1 (chưa đồng ý, vị trí tắt) | Unit test + Compose UI test | `NativeAdSlotTest.slotAllowed_falseWithoutConsent`, `slotAllowed_falseWhenPlacementOff`, `slotAllowed_falseWithoutUnitId`, `slotAllowed_trueWhenAllGiven` (đối chứng) cho `nativeSlotAllowed`; `withoutAdsController_takesNoSpace` (đã có, giữ) |
| KB-1, trường hợp đặc biệt 2 (rời màn thì hủy) | Compose UI test | `NativeAdSlotTest.leavingComposition_destroysAd` (đã có, giữ; quảng cáo chờ và về muộn: T-040) |
| O-06 (tùy chọn yêu cầu, mọi đường gửi native, L-072) | Unit test | `NativeRequestOptionsTest.everySize_startMuted_noCustomControls_singleImage`; `mediumAndFull_landscapeMedia`; `small_anyAspect`; `fullScreen_startMuted_noCustomControls_singleImage_anyAspect`; `toNativeAdOptions_copiesEveryField` (dựng `NativeAdOptions` thật rồi đọc getter: AdChoices `ADCHOICES_TOP_RIGHT`, tỷ lệ media, một ảnh, `getVideoOptions().startMuted`, `customControlsRequested`); `onlyNativeRequestOptions_buildsNativeAdOptions`: quét `src/main/kotlin` của `:core:ads` theo ký hiệu (import `com.google.android.gms.ads.nativead.NativeAdOptions` hay lời gọi `NativeAdOptions.Builder(` ngoài dòng chú thích), chỉ `NativeRequestOptions.kt` được có, nên `SdkNativeAdLoader.load` và nạp sẵn toàn màn hình trong `AdsController` đều đi qua hàm chung (đỏ trước code: hôm nay hai file khác dựng nó) |
| O-07 (co giãn ảnh trước khi gắn) | Compose UI test | `NativeAdViewBindingTest.mediaScaleType_setBeforeSetNativeAd` (bộ ghi qua `newMediaView` và `bind`) |
| O-08 (nhãn, AdChoices, sao) | Unit test + Compose UI test | `NativeStarCountTest`: `null`, 0, -1, NaN thì 0; 0.4 thì 0 (làm tròn), 0.5 thì 1, 3.6 thì 4, 4.5 thì 5, 7.0 thì 5; `NativeAdCardTest.full_starRow_hiddenWhenZero_shownFilledWhenFour` (cùng test có hai quảng cáo, điểm 0 và điểm 4, L-055); AdChoices: `small_leavesAdChoicesCornerFree` (đã có, giữ); nhãn: `badgeText_isAd` (đã có, giữ) |
| KB-2, tiêu chí 1 (media đầy đủ, ít nhất 120 x 120, không cắt) | Compose UI test | `NativeAdCardTest.full_showsHeadlineAdBadgeCta_leavesAdChoicesCornerFree` (như `small_*` cho `FULL`: icon, tiêu đề, nhãn "Ad", CTA hiện; không phần nào đè góc trên phải 24 x 24 dp); `full_mediaIs200dpHigh_atLeast120dpWide`; `full_iconIs40dp_headline14sp` |
| KB-2, tiêu chí 2 (giữ chỗ, lỗi, tối) | Compose UI test | `NativeAdCardTest.full_placeholderHeight_equalsCardHeight_shortAndLongTexts` (hai quảng cáo giả: chữ ngắn, chữ dài nhiều dòng); `placeholder_matchesCardHeight_forEachSize` (lặp `NativeAdSize.entries`, có `FULL`); màu như KB-1 tiêu chí 4 cho `FULL` (`full_headlineAndCtaColors_lightAndDark`: nền thẻ, nền CTA, chữ tiêu đề so với `onSurface`, chữ CTA so với `onTertiaryContainer`, tương phản ít nhất 4.5:1, mỗi chế độ) |
| KB-2, tiêu chí 3 (hai ô medium khác nhau) | Không thuộc task này | T-028 |
| Toàn màn hình theo thiết kế vẽ lại | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_drawn36dp_touchAtLeast48dp` (bố cục 36 x 36 dp; chạm cách mép phải 5 dp và cách mép dưới 5 dp đều gọi `onClose`; đối chứng: chạm cách mép 8 dp không gọi; thay `closeButton_isAtLeast48dp`); `closeButton_isTopLeft_awayFromAdChoices` (đã có, vẫn 16, 24); `media_startsAt76dp`; `texts_followDesign` (CTA 52 dp, icon 48 dp, tiêu đề 16 sp) |
| Giao diện native nhỏ, đầy đủ | Screenshot test | `NativeAdCardScreenshotTest`: `NativeAdCard_small_light.png`, `NativeAdCard_small_dark.png` (ghi lại), `NativeAdCard_full_light.png`, `NativeAdCard_full_dark.png`, `NativeAdPlaceholder_small_light.png` (mới, `animated = false`) |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `NativeFullScreenAdScreen_max-content_small.png`, `SampleScreen_ready_small.png`, `AdGalleryScreen_default_small.png`, `AdGalleryScreen_inlist_small.png` (ghi lại) |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh bằng công cụ so ảnh chạy thẳng theo từng module (dưới bảng), rà soát độc lập theo mục 7 của mẫu thiết kế | `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_ad-loading_light.png` (`:feature:sample`), `NativeFullScreenAdScreen_default_light.png`, `NativeFullScreenAdScreen_default_dark.png` (`:core:ads`), `AdGalleryScreen_default_light.png`, `AdGalleryScreen_default_dark.png`, `AdGalleryScreen_inlist_light.png` (`:app`), đều ghi lại |
| | | Ảnh thiết kế của ticket khác (T-030): `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `NativeAdCardTest.badgeText_isAd` (đã có, giữ) |
| Tùy chọn yêu cầu với quảng cáo thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, mở màn xem thử quảng cáo: native toàn màn hình có video thì video bắt đầu tắt tiếng, không có thanh điều khiển riêng; native nhỏ ở đáy cao 80 dp, AdChoices ở góc trên phải không bị đè |

**Ảnh so sánh với thiết kế (ADR-0020):** ảnh chuẩn của feature ads nằm ở ba module (`core/ads`, `feature/sample`, `app`), không ở `feature/ads/`, nên không dùng script so sánh bọc sẵn; chạy thẳng công cụ so ảnh cho từng thư mục, ra thư mục tạm, như T-013 và T-014: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens <module>/src/test/screenshots ${TMPDIR:-/tmp}/t027-compare-<module>`. Phần lệch dự kiến: thiết kế vẽ lại medium cao 410 dp, plan giữ 386 dp (Q3), nên ở `AdGalleryScreen_default_*` và `AdGalleryScreen_inlist_light` mọi thứ dưới thẻ medium lệch khoảng 24 dp (quá một bước lưới 4 dp); ghi vào mục 8 của README thiết kế, không tính là lệch của task này. Công cụ ghi đè `summary.md` của thư mục đích, nên chỉ chép ảnh `_compare.png` của các ảnh trên vào `docs/features/ads/design/review/` và ghi kết quả vào mục 8 của README thiết kế.

**Ngoại lệ viết test trước:**

- Ghi lại ảnh chuẩn screenshot (mục 3.2): ảnh là kết quả của code, ghi sau code ở bước 4 theo plan.
- `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`: tài liệu.
- `strings.xml`: tài nguyên.
- Phần nối `nativeSlotAllowed` vào `NativeAdSlot` (thay điều kiện `!canRequestAds || !enabled || unitId == null` bằng lời gọi hàm, L-072): hàm có test riêng; đường hiện khe chỉ có một chỗ gọi, hành vi giữ bằng `withoutAdsController_takesNoSpace` và `availability_reportsFalse_whenPlacementTurnsOffAfterLoaded`.
- KDoc `CollapsibleNativeAd.kt:189`, ô giả 80 dp của `SampleScreenTest.kt`: chú thích và dữ liệu test cho đúng số, không đổi hành vi.
- Các test đã có giữ nguyên ("đã có, giữ") là test hồi quy, qua ngay.
- Test qua ngay trên code cũ (L-055), mỗi test có bước đối chứng chạy ở bước 3, không commit: `small_headlineAndCtaColors_lightAndDark`, `full_headlineAndCtaColors_lightAndDark` (đối chứng: tạm đổi màu chữ tiêu đề sang `surfaceContainerLow` và nền thẻ sang `surface` thì test đỏ); `small_hasNoMedia` (đối chứng: tạm vẽ `NativeAdMediaArea` trong `SmallLayout` thì đỏ); `slotAllowed_trueWhenAllGiven` (đặt cạnh ba test false); `NativeAdViewBindingTest.mediaScaleType_setBeforeSetNativeAd` (thứ tự hôm nay đã đúng, test chỉ đỏ vì chưa có `newMediaView`; đối chứng: tạm chuyển `setImageScaleType` vào trong hay sau lời gọi `bind` thì test đỏ). Kiểm AdChoices dùng `small_leavesAdChoicesCornerFree` đã có, không thêm test gần trùng.

### 4.1. Chạy thử

- **Đã chạy thử (phiên bản 2):** trên worktree của nhánh `claude/plan-T-027-native-adskit`, commit `aa975c7` (mã Kotlin, ảnh chuẩn và file build giống `a1cec25` của lần chạy phiên bản 1: `git diff --stat a1cec25 aa975c7 -- '*.kt' '*.png' '*.kts'` rỗng), 2026-10-25, không commit. Thay đổi tạm, gộp một lần: (1) L-089: `enum class NativeAdSize { SMALL, MEDIUM, FULL }`, nhánh `FULL` gộp với `MEDIUM` ở ba `when` của `NativeAdCard.kt` và một của `SdkNativeAdLoader.kt`; (2) `SmallHeight = 80.dp`; (3) nút đóng toàn màn hình theo A3: `CloseButtonSize`, `TopBarHeight` 36 dp, `IconButton` thay bằng `Box(...size(36.dp).clip(CircleShape).background(...).clickable(onClick = onClose))`; (4) test tạm `TmpCloseTouchTest` (bốn test). Đã hoàn tác bằng `git checkout -- core/` và xóa test tạm; `git status --short` sạch.
- **Lệnh:** `./gradlew --no-daemon --console=plain --continue :core:ads:testDebugUnitTest :core:ads:verifyRoborazziDebug :feature:sample:testDebugUnitTest :feature:sample:verifyRoborazziDebug :app:testStagingDebugUnitTest :app:verifyRoborazziStagingDebug`. Chưa chạy `scripts/check.sh` (E dưới 2, không bắt buộc).
- **Kết quả, vùng chạm nút đóng (Q5):** nút đo được `DpRect(16, 24, 52, 60)` (36 x 36 dp tại 16, 24), media bắt đầu ở y = 76 dp; chạm cách mép phải 5 dp và cách mép dưới 5 dp đều gọi `onClose` một lần; chạm cách mép phải 8 dp không gọi (vùng nới tới 48 dp, tức 6 dp mỗi phía). Cả bốn test tạm qua. `closeButton_isTopLeft_awayFromAdChoices`, `tapClose_callsOnCloseOnce`, `smallScreen_closeAndCtaInside`, `closeButton_isDisplayedAtFirstFrame`, `texts_followDesign` (CTA còn 48 dp ở lần chạy này) vẫn qua; chỉ `closeButton_isAtLeast48dp` đỏ (đúng dự kiến, test được thay ở mục 3.2).
- **Kết quả, 80 dp và `FULL`:** không có lỗi biên dịch nào (thêm `FULL` không làm hỏng `:feature:sample` hay `:app`; mọi `when` trên enum nằm ở `:core:ads`). `:core:ads`: 415 test (411 cũ và 4 test tạm), 23 đỏ: `NativeAdCardTest` (`small_is72dpHigh`, `longTexts_keepHeight`, `placeholder_matchesCardHeight_forEachSize`: test này lặp `NativeAdSize.entries` và giả định mọi cỡ khác `SMALL` cao 386 dp, nên `FULL` cần chiều cao riêng trong test, L-089), `NativeAdSlotTest` (5: `loading_showsPlaceholder`, `unavailable_takesNoSpace`, `unavailable_keepsSpace_whenDeclaredNextToButton`, `buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace`, `boundNativeAd_withFakeAd_drawsCard`), `NativeAdViewBindingTest.sdkLayout_keepsCardHeights`, `CollapsibleNativeAdTest` (5 test của dạng thu gọn), `InListAdsTest.loadingSlot_showsPlaceholderOfSmallHeight`, `NativeFullScreenAdScreenTest.closeButton_isAtLeast48dp`, ảnh `NativeAdCard_small_light`, `_small_dark`, `CollapsibleNativeAd_collapsed_light`, `_collapsed_dark`, `NativeFullScreenAdScreen_default_light`, `_default_dark`, `_max-content_small`. `:feature:sample`: 24 test, 4 đỏ, đều là ảnh `SampleScreen_ready_light`, `_ready_dark`, `_ready_small`, `_ad-loading_light`; `SampleScreen_disabled_light` qua. `:app` (staging): 84 test, 7 đỏ: ảnh `AdGalleryScreen_default_light`, `_default_dark`, `_default_small`, `_inlist_light`, `_inlist_small`, `_collapsible-collapsed_light` và `AdGalleryScreenTest.inList_insertsAdsAfterEveryNItems` (đo chiều cao dòng quảng cáo nhỏ 104 dp, `AdGalleryScreenTest.kt:128`). `NetworkLostScreen_*`, `AdGalleryScreen_collapsible-expanded_*`, `LoadingScreen_*` qua: không vẽ native nhỏ. Kết quả trùng lần chạy phiên bản 1 (19 đỏ), cộng 4 đỏ do nút đóng mới. Mọi test và ảnh đỏ có trong mục 3.2.
- **Chưa chạy thử:** các test đỏ vì số đo mới ngoài 72/80 và nút đóng (CTA 34 dp, icon 36 dp của native nhỏ; CTA 52 dp, icon 48 dp, tiêu đề 16 sp của toàn màn hình: `cta_is36dpHigh`, `texts_followDesign`) chỉ suy ra từ số đo, đã ghi tên ở mục 3.2; `NativeRequestOptions` (API của SDK đã dùng, getter tra bằng `javap`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi định dạng hiển thị và tùy chọn yêu cầu native), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Code ở `:core:ads`; ảnh chuẩn ở `:core:ads`, `:feature:sample`, `:app` (khoảng 50 dòng ở mục 3.2) |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, luật và thiết kế rõ số đo; 15 câu của phiên bản 1 đã trả lời; còn câu 1 (tỷ lệ media của native toàn màn hình), không đổi cách làm |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có unit test hay test đo; tùy chọn yêu cầu với quảng cáo thật chỉ thấy trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi chiều cao native nhỏ mà `:feature:sample`, màn xem thử, native thu gọn và native trong danh sách dựa vào; T-026, T-028, T-030, T-032, T-040 phụ thuộc task này |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu đã có (`NativeAdCard`, hàm thuần); `VideoOptions` là API của SDK đã dùng; vùng chạm đã chạy thử |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn; cần duyệt chéo) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdSize`, `NativeAdDimens`, `NativeAdSlot` (API công khai của `:core:ads` cho feature), `AdsController.kt` (chỉ khối tùy chọn của native toàn màn hình), ảnh chuẩn của `:feature:sample` và `:app`.
- **Ranh giới với T-040 (nhánh `claude/plan-T-040-nguon-mot-quang-cao`, phụ thuộc T-027, chạy sau).** Chuyển sang T-040 từ phiên bản 1 của plan này: cổng gửi chung `NativeSendGate.kt` (B0); `NativeAdSource.kt`, `NativeAdSources.kt` (nguồn một quảng cáo, đếm khe, đóng khi vị trí tắt hay rút đồng ý; O-01 tới O-04, L-061); `NativeAdRefresh.kt` (làm mới 60 giây, bỏ nhịp ở nền, đợi 10 giây, `retryResets`; CL-02, O-05, O-09); `NativeAdRetry.kt` (gọi cổng chung, nối sang làm mới); `NativeAdLoading.kt` (quảng cáo về muộn); `AdTelemetry.kt` (`NativeLoadUse.REFRESH`); `FreshAdCache.kt` (`nativeFullScreenCache` 55 phút); phần `nativeAd` của `AdsController.kt`; các test `NativeAdSourceTest`, `NativeSendGateTest`, `NativeAdSourcesTest`, `NativeAdRefreshTest`, `NativeAdRetryTest`, `FreshAdCacheTest`, `AdTelemetryTest`; dòng kiểm thủ công làm mới 60 giây; câu hỏi 12, 13, 14 và phương án dự phòng nhịp 60 giây của phiên bản 1. T-027 không chạm các file đó.
- File cả hai plan cùng sửa (L-054): `AdsController.kt` (T-027: khối `NativeAdOptions` của nạp sẵn toàn màn hình; T-040: `nativeAd` và `FreshAdCache`), `FakeNativeAds.kt` (T-027: `starRating`, bộ chữ dài; T-040: nguồn giả), `docs/ops/ads-guide.md` (T-027: kích thước, `onAdAvailabilityChange`, `NativeRequestOptions`; T-040: làm mới 60 giây). `NativeRequestOptions.kt` do T-027 tạo; T-040 không dựng `NativeAdOptions` ở chỗ khác, vì `NativeRequestOptionsTest.onlyNativeRequestOptions_buildsNativeAdOptions` quét `src/main` (L-062): plan nào cùng danh sách chạy thêm `NativeAdOptions.Builder(` ngoài file đó sẽ làm test đỏ.
- `NativeAdSlot.kt` dùng chung với T-040 (L-054): T-027 sửa trước (`nativeSlotAllowed`, `NativeAdSlotGate`, `onAdAvailabilityChange`) và merge trước; T-040 sở hữu phần bọc `key(state.ad)` quanh nội dung quảng cáo để quảng cáo thay tại chỗ được gắn lại, sửa trên bản của T-027.
- Ghi cho T-030: sau khi T-040 merge, mỗi lần làm mới thay quảng cáo là một lần chuyển sang `Loaded`, nên `onAdAvailabilityChange(true)` được gọi lại mỗi lần thay; T-030 xử lý callback theo kiểu lặp lại không đổi kết quả (chỉ chuyển trạng thái chừa chỗ khi giá trị khác lần trước).
- Task khác có thể xung đột: T-024 (đã merge vào nhánh chính, commit `3d4f165`, đã gộp vào nhánh plan này): `AdsController.kt` (khe app open, `StartupAds`; không chạm khối native toàn màn hình, nay ở dòng 1050) và `docs/ops/ads-guide.md` (mục 3, 6, 8, 9; T-027 sửa mục 3 và 4, nên phần chung là mục 3: sửa trên bản đã có T-024); T-030 (thanh đáy và thu gọn: dùng `BottomSlotHeight`, `onAdAvailabilityChange`; ảnh `AdGalleryScreen_collapsible-*`, `CollapsibleNativeAd_*`); T-028 (pool: `NativeAdSize`, số đo medium); T-026 (đếm ngược trên `NativeFullScreenAdScreen`, góc trên trái theo Q10); T-032; T-031 (rewarded, nhánh `claude/plan-T-031-rewarded`: không dùng native, có thể cùng sửa `AdsController.kt` ở chỗ khác). T-035 đã xong (thiết kế).
- Đề xuất thứ tự chạy: T-024 đã merge; T-027 trước T-040 (cùng sửa `NativeAdSlot.kt`, `AdsController.kt`), T-026, T-028, T-030, T-032.

## 7. Điều kiện dừng riêng

- Đo trên thiết kế khác số của mục 5.7 quá một bước lưới 4 dp (ví dụ khe đáy không còn 80 dp): dừng, mâu thuẫn thiết kế và luật.
- Đổi chiều cao native nhỏ làm nút hay nội dung chính của một màn hình bị che hoặc cắt ở khung thấp `w360dp-h640dp-xxhdpi` (L-028), mà không sửa được bằng bố cục của chính màn đó: dừng, cần người quyết lại cỡ 80 dp cho mọi native nhỏ (Q4). Test đỏ ở file ngoài mục 3.2 thì sửa tiếp và ghi vào báo cáo (ADR-0029), không dừng.
- Cần thêm định dạng vào `AdFormat` hay vị trí vào `AdPlacement` để hiện native đầy đủ: dừng (Q2).

## 8. Câu hỏi cho người duyệt

1. **Tùy chọn yêu cầu của native toàn màn hình (O-06).** Phiên bản 1 chỉ áp O-06 cho `SdkNativeAdLoader`; rà lại thấy nạp sẵn native toàn màn hình trong `AdsController` dựng `NativeAdOptions` riêng (media `ANY`, chỉ tắt tiếng, không tắt điều khiển riêng, không giới hạn một ảnh). O-06 ghi "media ngang (ô lưới xin media dọc)", không nói native toàn màn hình, mà vùng media toàn màn hình là khung dọc (379 x 595 dp).
   - Đề xuất của AI: áp O-06 cho cả đường này qua hàm chung (tắt tiếng, không điều khiển riêng, một ảnh), giữ media `ANY` cho toàn màn hình để không thu hẹp nguồn quảng cáo; plan viết theo đề xuất này (mục 3.1, A4). Phương án khác: `LANDSCAPE` như O-06, hay `PORTRAIT` theo khung dọc; chỉ đổi một giá trị trong `fullScreenNativeRequestOptions()` và test `fullScreen_*`.

**Câu hỏi của phiên bản 1:** 15 câu, anh Nhật trả lời 2026-10-25, cả 15 câu theo đề xuất của AI; quyết định ghi ở mục 2 (Quyết định đã chốt), câu 12, 13, 14 chuyển sang T-040.

**Rà soát độc lập của phiên bản 1 (2026-10-25), phần còn áp cho phần A:** chặn 1 (tên test thật, hàm thuần `nativeSlotAllowed` và test); nên sửa 7 (vùng chạm không đổi bố cục, test vị trí media; nay đã chạy thử), 10 (ngoại lệ L-055 kèm đối chứng, bỏ test gần trùng), 11 (`newMediaView`), 13 (phần lệch medium, sửa mục 3 README), 14 (thẻ `FULL` số dòng cố định); ghi chú 16 (test đỏ ngoài 72/80 ghi tên), 17 (KDoc `CollapsibleNativeAd.kt:189`, ô giả của `SampleScreenTest.kt`), 21 (`bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight`, phạm vi). Các điểm chặn 2, 3, 4, nên sửa 5, 6, 9, 12, 15, ghi chú 18 thuộc phần B, đi cùng T-040. Phiên bản 2 đã qua rà soát độc lập 2026-10-25 (0 chặn, 7 nên sửa, 4 ghi chú; đã sửa ở c6f6b36).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** AI duyệt thay anh Nhật theo ủy quyền ngày 2026-10-25 ("thay thế tôi duyệt tất cả các plan và tickets"). Câu hỏi ở mục 8: theo đề xuất của AI, trừ chỗ ghi khác dưới đây. Đã qua rà soát độc lập và sửa theo điểm chặn, điểm nên sửa. Chạy thứ 1 trong RUN-20261026-03. 15 câu của phiên bản 1 đã được anh Nhật trả lời 2026-10-25 (mục 8).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
