---
ticket: T-109
title: Code native nhỏ và lớp thu gọn theo thẻ JBase vẽ ra
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20.2M        # cỡ M sau khi tách medium sang T-110, số gợi ý 20.2M (scripts/token-usage.py estimate, 2026-11-17), không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật
cross_reviewer: Nhật         # nhóm một dev (risk-scoring.md, mục 3)
approved_at: 2026-11-17
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-109: CODE NATIVE NHỎ VÀ LỚP THU GỌN THEO THẺ JBASE VẼ RA

## 1. Tóm tắt

Đổi mọi native nhỏ của `:core:ads` (khe đáy, khe đứng riêng, hàng trong danh sách, khối nội dung của lớp thu gọn) sang bố cục hai tầng của thiết kế T-108: 106 dp ở cỡ chữ 1.0 ở mọi bề rộng, cao thêm theo chữ; một hàm tính chiều cao từ cỡ chữ (đo dòng chữ như thẻ của bước, `stepCardHeight`) dùng cho thẻ, khung chờ và chỗ chừa của thanh đáy. Lớp thu gọn có tổng nội suy theo cạnh ngắn (250 tới 400 dp, 299 dp ở 411 dp), vùng ảnh lấy phần còn lại, cao thêm khi ảnh dưới 120 dp, vẫn chịu trần nửa cửa sổ. Native medium tách sang ticket mới T-110 (mục 8, câu hỏi 1). Kiểm bằng test Robolectric về kích thước ở nhiều khung và cỡ chữ, ảnh chuẩn screenshot cùng tên ảnh thiết kế và `scripts/design-compare/DesignCompare.java`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-17, câu hỏi 66):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Khi có quảng cáo thì thấy icon, tiêu đề, CTA, nhãn "Ad" và AdChoices; vùng quảng cáo có nền hay viền; CTA khác nút chính | Giữ: bố cục hai tầng vẫn có đủ các phần, góc trên phải trống cho AdChoices (hàng trên chừa 16 dp bên phải) |
| KB-1, tiêu chí 2 (đổi 2026-11-17) | Khe cao theo nội dung như JBase vẽ, 106 dp ở cỡ chữ mặc định ở mọi bề rộng; đang tải thì giữ đúng chiều cao đó | Thẻ và khung chờ cùng `smallNativeHeight`: 6 + 44 + 6 + 44 + 6 = 106 dp ở cỡ chữ 1.0, ở khung 360, 411, 673 dp và màn ngang |
| KB-1, tiêu chí 6 (đổi 2026-11-17) | Cỡ chữ 200% thì tiêu đề, nhãn "Ad" và CTA không bị cắt mất chữ (ba chấm được); thẻ cao thêm theo chữ, khung chờ và chỗ chừa theo cùng chiều cao | Chiều cao tính trước khi có quảng cáo, từ cỡ chữ (đo một dòng của từng kiểu chữ), không đo quảng cáo thật; test đo các phần nằm trong thẻ, không chồng nhau, ở cỡ chữ 1.0 tới 2.0 |
| KB-3 | Ô quảng cáo có nhãn "Ad" rõ; mỗi ô một quảng cáo | Hàng native nhỏ trong danh sách dùng cùng bố cục, dòng trong danh sách 138 dp (106 cộng 16 trên dưới) thay 158 dp |
| KB-4, tiêu chí 1, 2 | Lớp mở ở đáy, nút thu gọn; thu gọn lộ khe đáy | Giữ hành vi; số: lớp 299 dp ở 411 x 891 (vùng ảnh 193 dp, MediaView 181 dp, khối nội dung 106 dp), khe lộ ra 106 dp |
| KB-4, tiêu chí 4 | Cửa sổ không đủ chỗ (lớp kể cả lề quá nửa chiều cao cửa sổ dù vùng ảnh đã 120 dp) thì không mở lớp | Ngưỡng nay theo khối nội dung ở cỡ chữ hiện tại: cỡ chữ 1.0, cửa sổ 411 dp rộng có lớp từ chiều cao 508 dp (trước 548); chữ lớn thì cần cửa sổ cao hơn |
| KB-17, tiêu chí 1, 3, 5 | Thanh đáy có native nhỏ và lớp nổi; khe khung chờ khi chờ; nội dung chừa đúng chiều cao khe | Chỗ chừa = `smallNativeHeight` + 16 dp (122 dp ở cỡ chữ 1.0, mọi bề rộng), cùng hàm với thẻ vẽ ra (D-04) |
| KB-32, tiêu chí 3 (đổi 2026-11-17) | Mọi native nhỏ hai tầng; 106 dp; chữ lớn cao thêm; chừa = thẻ + lề; lớp thu gọn tổng hai phần nội suy theo cạnh ngắn 320 tới 600 dp, vùng ảnh phần còn lại; chữ lớn làm ảnh dưới 120 dp thì lớp cao thêm; trần nửa cửa sổ; medium 250 dp | Phần native nhỏ và lớp thu gọn làm ở đây; medium ở T-110. Tổng của lớp nội suy thẳng 250 tới 400 dp rồi làm tròn (271 dp ở 360, 299 ở 411, 400 từ 600), khớp số thiết kế ở cả ba mốc (cộng hai số đã làm tròn cho 272 ở 360) |

Thiết kế tham chiếu: `features/ads/design/README.md`, mục 1 (đợt T-108) và mục 3; ảnh trong `design/screens/`: `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`; số liệu trong `design/context/` cùng tên. Feature network: `features/network/design/README.md` (khe đáy của màn mẫu sau popup).

ADR liên quan: ADR-0045 (mục 1.2: JBase là chuẩn thứ hai; mục 7.7), ADR-0038, ADR-0020 (khung screenshot, ảnh cùng tên thiết kế), ADR-0031 (viết lại câu khẳng định cũ), ADR-0029, ADR-0008.

## 3. Giải pháp

### 3.1. Cách làm

**Chiều cao native nhỏ (một hàm cho thẻ, khung chờ, chỗ chừa).** Theo mẫu `stepCardHeight` của `NativeFlowStep.kt` (plan T-045): hàm thuần `internal fun smallNativeHeight(measurer: TextMeasurer, typography: Typography, density: Density): Dp` trong `NativeAdCard.kt`, đo một dòng của từng kiểu chữ bằng `TextMeasurer` (cùng cách `Text` đo, nên có cỡ chữ phi tuyến của Android 14):

- hàng tiêu đề = max(dòng tiêu đề 16/22 sp trên `titleSmall`, dòng nhãn "Ad" 13 sp trên `labelSmall`);
- cột chữ = hàng tiêu đề + 4 dp + dòng dưới 13/18 sp trên `bodySmall`;
- hàng trên = max(44 dp, cột chữ); CTA = max(44 dp, một dòng `labelLarge` 18 sp);
- thẻ = 6 + hàng trên + 6 + CTA + 6, cộng theo px rồi đổi ra dp, làm tròn lên dp nguyên; không dưới 106 dp. Ở mật độ mà dòng chữ ra px lẻ (420 dpi: 2.625), mỗi dòng bị làm tròn lên px nên thẻ có thể 107 dp; chiều cao vẫn bằng bố cục thật vì cùng cách đo. Test kiểm đúng 106 dp ở các khung xxhdpi (mật độ 3) của mục 4. Viền 1 dp vẽ phía trong (`Modifier.border`), không cộng.

Composable `@Composable internal fun rememberSmallNativeHeight(): Dp` nhớ kết quả theo `measurer`, `typography`, `density` (như `NativeStepAdDefaults.height()`). Ở cỡ chữ 1.0: 22 + 4 + 18 = 44, nên thẻ 106 dp.

**Bố cục `SmallLayout` (thiết kế T-108, `Native/Small`).** Bỏ tham số `slotHeight`. Một `Column` lề trái phải 12 dp, đệm trên dưới 6 dp, cách nhau 6 dp:

- hàng trên (`Row`, cao ít nhất 44 dp, căn giữa dọc, lề phải 16 dp chừa AdChoices): icon 44 dp, cách 12 dp, `SmallTexts` (`weight(1f)`);
- `SmallTexts`: `Column` cách 4 dp: hàng nhãn "Ad" 13 sp trước tiêu đề 16/22 sp một dòng; dòng dưới là nhà quảng cáo, không có thì mô tả, 13/18 sp, **một dòng** ở mọi cỡ chữ, cắt cuối; không có cả hai thì không có dòng dưới;
- CTA `fillMaxWidth()`, cao ít nhất 44 dp, chữ 18 sp.

Bỏ hai quy tắc chữ lớn của plan T-065 (mục 8, câu hỏi 2): `SMALL_TIGHT_FONT_SCALE` (CTA không còn nằm cạnh tiêu đề) và `SMALL_BADGE_ON_BODY_LINE_FONT_SCALE` (nhãn "Ad" luôn đứng trước tiêu đề, dòng dưới luôn một dòng); bỏ `SmallCallToActionMaxWidth` (CTA ngang hết thẻ). Bỏ `SmallInnerEnd` 24 dp của bố cục cũ và `SmallPlaceholderCallToActionWidth` 96 dp của khung chờ cũ.

**Khung native nhỏ.** `NativeAdLayout` đặt thẻ SMALL `Modifier.height(rememberSmallNativeHeight())`. Khung chờ SMALL (`NativeAdPlaceholderLayout`) cùng bố cục hai tầng: khối icon 44 dp, hai vạch (14 dp rộng hết cột, 12 dp rộng 120 dp, cách 8 dp), khối CTA ngang hết thẻ cao 44 dp; cùng chiều cao. `NativeAdDimens.heightOf` bỏ nhánh SMALL: thay bằng composable `internal fun nativeAdHeight(size: NativeAdSize, ctaLarge: Boolean): Dp` (SMALL: `rememberSmallNativeHeight()`; STEP: `NativeStepAdDefaults.height()`; MEDIUM, FULL, CARD: như `heightOf` hôm nay), dùng ở `NativeAdPlaceholder`, `NativeAdLayout` và chỗ giữ khi trống của `NativeAdSlotContent` (`keepSpaceWhenEmpty`). `heightOf` còn lại cho MEDIUM, FULL, CARD, STEP; nhánh SMALL ném `IllegalArgumentException` với lời nhắn dùng `nativeAdHeight` (không còn ai gọi). Bỏ `NativeAdDimens.bottomSlotHeight`.

**Thanh đáy.** `bottomBarReservation(slot, nativeTakesSpace, bannerHeight, nativeSlotHeight: Dp)`: tham số thứ tư đổi từ `windowShortEdge` sang chiều cao khe native; khe native chiếm chỗ thì chừa `nativeSlotHeight + 16 dp`. `BottomAdBarHost` truyền `rememberSmallNativeHeight()`. `BottomAdBarDefaults` giữ tên và chữ ký công khai, chỉ bỏ `@ReadOnlyComposable` (chúng gọi `remember`): `nativeReservation()` = `rememberSmallNativeHeight()` + 16 dp (122 dp ở cỡ chữ 1.0), `nativeSlotHeight()` = `rememberSmallNativeHeight()`, `collapsibleLayerHeight()` = `rememberCollapsibleLayer()?.height`. Feature gọi như hôm nay, trong composition, dưới `AppTheme` (cỡ chữ đọc từ `LocalDensity`, kiểu chữ từ `MaterialTheme`); không cần sửa `SampleScreen.kt`. KDoc nói rõ chiều cao nay theo cỡ chữ và không đổi theo bề rộng.

**Lớp thu gọn.** Trong `NativeAdDimens`:

- `collapsibleTotalHeight(shortEdge: Dp): Dp = interpolated(shortEdge, 320.dp, 600.dp, 250.dp, 400.dp)` (thay `collapsibleMediaAreaHeight`; 271 dp ở 360, 299 ở 411, 400 từ 600);
- `collapsibleLayer(window: DpSize, content: Dp): CollapsibleLayerSize?`: vùng ảnh = max(tổng − `content`, 132 dp) (MediaView 120 dp cộng lề trên 12 dp: lớp cao thêm khi chữ lớn); phòng = floor(nửa chiều cao cửa sổ − `content` − 16 dp); vùng ảnh = min(vùng ảnh, phòng); dưới 132 dp thì `null`. `CollapsibleLayerSize(mediaArea, slot = content)`.
- composable `rememberCollapsibleLayer(): CollapsibleLayerSize?` = `collapsibleLayer(nativeScreenSize(), rememberSmallNativeHeight())`, dùng ở `CollapsibleNativeAdLayout`, giá trị mặc định `hasRoom` của `CollapsibleNativeAdLayerHost`, `CollapsibleNativeAdLayer` (cổng `hasRoom`, `ReportNativeEligible`, nhật ký không đủ chỗ) và `BottomAdBarDefaults.collapsibleLayerHeight()`. Đổi cỡ chữ là đổi cấu hình, màn dựng lại, nên `hasRoom` được tính lại như khi xoay.

Số: 411 x 891: tổng 299, vùng ảnh 193 (MediaView 181), nội dung 106. 360 x 640: 271, vùng ảnh 165; phòng = floor(320 − 106 − 16) = 198, không chạm trần. 673 x 841: 400, vùng ảnh 294, phòng 298. 411 x 600: tổng 299, phòng 178, lớp 284 dp. 411 x 508: phòng 132, lớp 238 dp; 411 x 507: `null`. 891 x 411: phòng 83, `null`. Nội dung 180 dp (chữ rất lớn, kiểm bằng hàm thuần) ở 411 x 891: vùng ảnh 132, lớp 312 dp.

**Không đổi:** medium (T-110), thẻ đầy đủ, thẻ dọc, thẻ ở bước, toàn màn hình; luồng thu gọn, nạp, làm mới, sự kiện. `AdsController.onCollapsibleNoRoom` giữ nguyên (chỉ ghi nhật ký với kích thước cửa sổ).

**Tài liệu.** `ads-business-rules.md` mục 5.7 (dòng native nhỏ ở khe đáy và dòng native thu gọn), D-02, D-04, O-08, CL-05 (bỏ ví dụ máy gập xoay ngang, mục 8 câu hỏi 5) và mục 11 Hằng số cứng (dòng chiều cao khe đáy và native thu gọn) viết lại theo câu hỏi 66; D-07 chỉ dẫn D-02, không nhắc số, giữ. `ads-guide.md` mục 3 (bảng định dạng, dòng thanh đáy), mục 4 (đoạn kích thước và cột Small của bảng; thêm câu chỗ chừa theo cỡ chữ, đọc qua `BottomAdBarDefaults.nativeReservation()`, mục 8 câu hỏi 4) và mục 5 bước 4 (thanh đáy) viết lại. README thiết kế: mục 3 ghi chú "Hai quy tắc ngoài thiết kế" thành đã bỏ ở T-109 (phần native nhỏ), mục 8 thêm bảng đối chiếu của T-109, chép ảnh `*_compare.png` của các ảnh đổi vào `review/`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `smallNativeHeight`, `rememberSmallNativeHeight`, `nativeAdHeight`; `SmallLayout` hai tầng, `SmallTexts` một dòng dưới; khung chờ SMALL hai tầng; `collapsibleTotalHeight`, `collapsibleLayer(window, content)`, `rememberCollapsibleLayer`; bỏ `bottomSlotHeight`, `collapsibleMediaAreaHeight`, `SmallCallToActionMaxWidth`, `SMALL_TIGHT_FONT_SCALE`, `SmallTightPartGap`, `SmallTightInnerEnd`, `SMALL_BADGE_ON_BODY_LINE_FONT_SCALE`, `SmallInnerEnd`, `SmallPlaceholderCallToActionWidth`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BottomAdBar.kt` | `bottomBarReservation` nhận chiều cao khe; `BottomAdBarDefaults` theo cỡ chữ; KDoc của `BottomAdBarScaffold` (106 dp, 299 dp) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | dùng `rememberCollapsibleLayer()`; `SmallLayout` không còn tham số chiều cao; KDoc số mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | chỗ giữ khi trống dùng `nativeAdHeight`; KDoc `BottomNativeSlot` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdDimensTest.kt` | test mới của `collapsibleTotalHeight`, `collapsibleLayer(window, content)`; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | test mới bố cục hai tầng, chiều cao theo khung; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | test mới cỡ chữ của native nhỏ, lớp thu gọn; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | chỗ chừa theo chiều cao khe và cỡ chữ; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | vùng ảnh phần còn lại, khối nội dung hai tầng; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | chiều cao khe = thẻ = khung chờ; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | `sdkLayout_keepsCardHeights`: thẻ SMALL 106 dp (phần MEDIUM giữ, T-110 đổi) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | dòng native nhỏ 106 + 32 dp; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeStockTest.kt` | `configurationChange_layerGoes_adDestroyed_logged`: khe 106 dp; viết lại theo mục 3.5; hai test mới ở chữ lớn (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarScreenshotTest.kt` | chú thích số (126 thành 106 dp, 142 thành 122 dp, MediaView 146 thành 166 dp); không đổi lệnh chụp |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | chú thích của `fontScale2_small` (bỏ câu CTA dừng ở 120 dp); không đổi lệnh chụp |
| Sửa | `core/ads/src/test/screenshots/BottomAdBar_collapsed_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/BottomAdBar_expanded_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/BottomAdBar_landscape_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/BottomAdBar_short-window_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/BottomAdBar_split_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_dark.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_small.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdPlaceholder_small_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_light.png` | ghi lại (phần native nhỏ; medium trong ảnh đổi ở T-110) |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_small.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_long-texts_light.png` | ghi lại |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenTest.kt` | chiều cao khe 106 dp; viết lại theo mục 3.5 |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenScreenshotTest.kt` | chú thích số (200 dp ở 673 dp và 126 dp thành 106 dp; lớp 272 thành 271 dp); không đổi lệnh chụp |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_ready_dark.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_ready_small.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_ready_light_unfolded.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_bar-expanded_landscape.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_bar-expanded_small.png` | ghi lại |
| Sửa | `feature/sample/src/test/screenshots/SampleScreen_bar-expanded_split.png` | ghi lại |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | dòng small 138 dp, khe 106 dp; viết lại theo mục 3.5 (dòng medium 432 dp giữ, T-110 đổi) |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_light.png` | ghi lại (phần native nhỏ; medium đổi ở T-110) |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_dark.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_small.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default-more_light.png` | ghi lại (trang cuộn xuống, các phần dưới native nhỏ lên 20 dp) |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_inlist_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_inlist_small.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png` | ghi lại |
| Sửa | `docs/ops/ads-business-rules.md` | mục 5.7 (native nhỏ, native thu gọn), D-02, D-04, O-08, CL-05, mục 11 (hằng số cứng) theo câu hỏi 66 |
| Sửa | `docs/ops/ads-guide.md` | mục 3 (dòng thanh đáy của bảng định dạng), mục 4 (đoạn kích thước, cột Small: khung, chỗ cho AdChoices, chữ, CTA; câu chỗ chừa theo cỡ chữ), mục 5 bước 4 (thanh đáy) |
| Sửa | `docs/features/ads/design/README.md` | mục 3 ghi chú quy tắc chữ lớn đã bỏ (native nhỏ); hai câu về medium ghi "plan T-109" (dòng Media của mục 3, ghi chú khung thấp) đổi thành T-110; mục 8 bảng đối chiếu T-109 |
| Sửa | `docs/features/ads/design/review/SampleScreen_ready_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/SampleScreen_ready_dark_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/SampleScreen_ready_light_unfolded_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/SampleScreen_ad-loading_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_default_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_default_dark_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_inlist_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_collapsible-expanded_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_collapsible-collapsed_light_compare.png` | ảnh so sánh mới |

Nơi đọc các file dùng chung của bảng (`scripts/plan-check.py`, L-020): `scripts/tests/run.sh` chỉ chép `SampleScreen_ready_light.png` làm dữ liệu mẫu cho test của công cụ so ảnh (không so nội dung), nên không bị ảnh hưởng. `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md`: không test, script hay workflow nào mở hai file này; `scripts/ads-plan.py` chép tay bảng định dạng và chỉ nhắc tên file trong chữ dẫn nguồn; các test sau chỉ nhắc tên file trong chú thích hay KDoc để dẫn nguồn quy tắc khác (MC-02, mục S, 4, 8, 10), không chép chữ của các dòng plan sửa (5.7 native nhỏ và thu gọn, D-02, D-04, O-08, CL-05; `ads-guide.md` mục 4), nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Ảnh chuẩn chỉ do test cùng tên và công cụ so ảnh đọc. Không đổi: `SampleScreen.kt` (chỉ preview gọi `nativeReservation()`, chữ ký giữ), `PreviewAdGallerySlots.kt` (gọi như cũ), `NetworkLostScreen_*` của `:app` (test không có `AdsController` nên khe đáy trống từ T-017, mục 8 của README thiết kế network; đợt T-108 đã ghi lệch này là dự kiến), `SampleScreen_disabled_light`, các ảnh `AdGalleryScreen_*` khác (`banners`, `grid`, `pager`, `flow-*`, `rewarded`, `status`: không có native nhỏ hay thanh đáy), `NativeAdCard_card_*`, `_full_*`, `_step_*`, `_medium*`, `NativeAdPlaceholder_card_light`, `_medium_light`, `_step_light`, `NativeFlowStep_*`, `NativeFullScreen*`. Ảnh nào ngoài bảng lệch khi ghi lại thì trả về bằng `git checkout -- <ảnh>` nếu chỉ là nhiễu vẽ; lệch thật thì dừng (mục 7).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm vị trí hay định dạng; đổi kích thước và bố cục native nhỏ và lớp thu gọn theo spec `ads`, KB-32 tiêu chí 3 (câu hỏi 66). Cổng `hasRoom` của lớp thu gọn (CL-05) nay theo cả cỡ chữ.
- Sự kiện phân tích: Không chạm (`af_native_ad_eligible` của lớp vẫn theo `hasRoom`, chỉ số ngưỡng đổi).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp dùng trong code: `rememberTextMeasurer`, `TextMeasurer` (gói `androidx.compose.ui.text` của Compose UI, đã dùng ở `NativeFlowStep.kt` của `:core:ads`), `LocalDensity`, `MaterialTheme.typography`; đều trên classpath biên dịch `main` của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** chiều cao tính bằng `TextMeasurer` có thể lệch chiều cao `Row`, `Column` thật đo ở một cỡ chữ (làm tròn px của từng dòng, phần đệm trên dưới của font khi `lineHeight` nhỏ hơn cỡ chữ ở cỡ chữ lớn), làm khối nội dung tràn khỏi thẻ 1 tới 2 px; test "các phần nằm trong thẻ" ở mục 4 sẽ bắt.
- **Điều kiện chuyển:** test `small_partsInsideCard_fontScales` (hay `collapsibleLayer_contentFits_fontScales`) vẫn thất bại vì phần dưới cùng tràn khỏi thẻ sau 2 lần sửa hàm đo.
- **Cách làm thay:** đo khối nội dung thật một lần bằng `SubcomposeLayout` với `NativeAdAssets` mẫu một dòng (cùng `SmallLayout`, không gắn binder) trong `rememberSmallNativeHeight`, lấy chiều cao đo được làm chiều cao chung; cùng file `NativeAdCard.kt`, cùng test. Không hạ tiêu chí, không đổi test viết trước.

### 3.5. Hành vi đã có bị đổi

Từ `scripts/existing-tests.py` (28 test "Cần xem trước", 36 "Nên xem") và `grep` các số 126, 142, 161, 173, 272, 548 trong test của ba module. Test chỉ dùng tên mà không kiểm kích thước (ví dụ `AdPlacementTest.collapsibleLayerOff_onlyForNativeSmallBottomBar`, `AdsControllerNativeStockTest.layer_entersAfterPreload_showsStockAd`, `NativeFlowStepLayoutTest.fontScale_requiredPartsKeepSize`, `NativeAdViewBindingTest.registersStarRatingView_*`) giữ nguyên. Câu khẳng định của medium (400 dp, 182 dp, 432 dp, 97 dp, khung thấp) giữ nguyên ở plan này, T-110 đổi. Hằng số và chú thích có số cũ trong cùng test đổi theo câu mới (ví dụ `BottomAdBarTest.NATIVE_RESERVATION_DP` 142 thành 122). Mọi câu mới viết vào commit test của bước 3, trước code.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Hàm `bottomSlotHeight` nội suy 90 tới 200 dp theo cạnh ngắn (300→90, 411→126, 600→200), dp nguyên | Bỏ hàm; chiều cao native nhỏ là `smallNativeHeight(measurer, typography, density)`, 106 dp ở cỡ chữ 1.0, dp nguyên | `NativeAdDimensTest.bottomSlotHeight_interpolatesAndClamps` | Thay bằng `NativeAdFontScaleTest.smallNativeHeight_is106dp_atFontScale1_wholeDp` (test mới, cùng ý định "một hàm, số dp nguyên, đúng số thiết kế"): gọi hàm thuần với `TextMeasurer` và kiểu chữ của `AdsTestTheme` ở `Density(density = 2f, fontScale = 1f)` và `Density(density = 3f, fontScale = 1f)` (mọi dòng ra px chẵn): cả hai đúng 106 dp; ở `Density(density = 2.625f, fontScale = 1f)` (máy 420 dpi: dòng 22 sp = 57.75 px làm tròn lên 58, nên cột chữ hơn icon 1 px): dp nguyên, từ 106 tới 108 dp; đối chứng cùng test ở `fontScale = 2f`, mật độ 3: lớn hơn 106 dp và là dp nguyên. Test cũ bị thay vì hàm nó kiểm không còn |
| Native nhỏ cao theo cạnh ngắn của cửa sổ (126 dp ở 411, 106 ở 360, 200 ở 673, 126 ở 891 x 411, 106 ở 640 x 360) | 106 dp ở cỡ chữ 1.0 ở mọi khung | `NativeAdCardTest.small_heightFollowsScreenWidth`, `_360`, `_673`, `small_landscape_followsShortEdge`, `small_landscape360_followsShortEdge`, `small_is126dpHigh`, `placeholder_matchesCardHeight_forEachSize` (nhánh SMALL), `longTexts_keepHeight` (phần SMALL) | Câu mới: thẻ và khung chờ SMALL cao 106 dp ở cả năm khung (đổi tên thành `small_heightIs106dp_everyWindow` và bốn biến thể khung); bề rộng thẻ 859 dp ở 891 x 411 giữ; `small_is126dpHigh` thành `small_is106dpHigh`; nhánh SMALL của `placeholder_matchesCardHeight_forEachSize` và `longTexts_keepHeight` thành 106 dp, tiêu đề giữ 22 dp |
| Bố cục một hàng: icon, cột chữ, CTA bên phải rộng 80 tới 120 dp, cách mép phải 24 dp, ba phần căn giữa dọc; dòng dưới tối đa 2 dòng | Hai tầng: hàng trên (icon, cột chữ, lề phải 16 dp), CTA ngang hết thẻ ở dưới; dòng dưới 1 dòng | `NativeAdCardTest.small_jbaseLayout`, `small_leavesAdChoicesCornerFree`, `small_ctaAtMost120dpWide_shortCtaKeepsWidth`; `NativeAdViewBindingTest.sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible`; `CollapsibleNativeAdTest.layer_contentIsSmallNative` | Thay `small_jbaseLayout` bằng `small_twoTierLayout` (mục 4). `small_leavesAdChoicesCornerFree`: câu mới "hàng trên (tiêu đề) kết thúc cách mép phải thẻ ít nhất 28 dp (lề 12 cộng chỗ AdChoices 16), CTA bắt đầu dưới góc AdChoices (CTA top − thẻ top ≥ 16 + 4 dp)". `small_ctaAtMost120dpWide_shortCtaKeepsWidth` thành `small_ctaFullWidth_longCtaEllipsized`: CTA rộng 355 dp (379 − 24) với chữ ngắn và chữ dài; "Install" hiện đủ không ba chấm; chữ dài bị ba chấm, hiện ít nhất 1 ký tự; tiêu đề rộng hơn 0. `sdkLayout_small_longCta_...` thành `sdkLayout_small_longCta_ctaFullWidth_headlineVisible`: CTA 355 dp, tiêu đề và nhãn "Ad" rộng hơn 0, thẻ 106 dp. `layer_contentIsSmallNative`: icon 44 dp, CTA 44 dp cao giữ; câu CTA 80 tới 120 dp và cách mép 24 dp thành "CTA rộng bằng lớp trừ 24 dp, cách hai mép 12 dp"; cỡ chữ tiêu đề 16 sp giữ |
| Từ cỡ chữ 1.5 nhãn "Ad" xuống đầu dòng dưới, dòng dưới 1 dòng; dưới 1.5 dòng dưới tối đa 2 dòng | Nhãn "Ad" luôn trước tiêu đề, dòng dưới luôn 1 dòng (mục 8, câu hỏi 2) | `NativeAdFontScaleTest.small_badgeMovesToBodyLine_fromFontScale1_5` | Viết lại thành `small_badgeBeforeHeadline_oneSecondLine_fontScales`: ở mọi cỡ chữ của `SMALL_SCALES`, nhãn "Ad" bên trái tiêu đề cùng hàng (mép phải nhãn ≤ mép trái tiêu đề, tâm dọc lệch dưới 2 dp), dòng dưới 1 dòng; không có cả nhà quảng cáo lẫn mô tả thì không có dòng dưới (`SECOND_LINE` không tồn tại), nhãn vẫn trước tiêu đề |
| Thẻ nhỏ giữ chiều cao khe ở mọi cỡ chữ (126 / 106 / 90 dp theo khung), CTA tối đa 120 dp, "Install" rộng 83.33 / 100.33 / 117.33 dp | Thẻ cao thêm theo chữ, bằng `nativeSlotHeight()` ở cùng cỡ chữ; CTA ngang hết thẻ | `NativeAdFontScaleTest.small_fontScales_headlineBadgeCta_notCut_allWidths`, `_320`, `_360`; `smallAndCard_headlineBadgeCta_notCut_fontScales`, `_small` (tham số chiều cao `bottomSlotHeight`); `cardSizes_ctaTextFits_fontScales` (ca "small" `{ 126.dp }`) | `assertSmallTexts(slotHeight, minHeadlineChars)` thành `assertSmallTexts(minHeadlineChars)` (chiều cao kỳ vọng đọc trong `setContent` vào một biến state ở mỗi cỡ chữ, vì `nativeSlotHeight()` là composable; `TextCase.frameHeight` và `CardCase.cardHeight` của ca small đọc cùng cách): chiều cao thẻ bằng `BottomAdBarDefaults.nativeSlotHeight()` đọc trong cùng composition, bằng 106 dp ở cỡ chữ 1.0; câu chữ không bị cắt chiều cao và nằm trong thẻ áp cho tiêu đề, nhãn "Ad", CTA và thêm dòng dưới (`SECOND_LINE`, spec câu hỏi 66: mô tả không bị cắt mất chữ); "Ad" đủ hai chữ, tiêu đề ít nhất `minHeadlineChars` ký tự (4 ở 411 và 360 dp, 1 ở 320 dp) giữ; "Install" hiện đủ, cao ≥ 44 dp; câu bề rộng CTA (83.33 dp..., 120 dp) thành "CTA rộng bằng thẻ trừ 24 dp". `smallAndCard_...`: chiều cao của ca small lấy `nativeSlotHeight()` thay `bottomSlotHeight(...)`, phần thẻ dọc giữ. `cardSizes_...`: ca "small" lấy `{ scale -> chiều cao đọc từ nativeSlotHeight() ở scale }` (106 dp ở 1.0), CTA 44 dp giữ |
| Lớp thu gọn giữ 299 dp ở mọi cỡ chữ, MediaView 161 dp | Khối nội dung cao theo chữ; lớp = max(299, nội dung + 132); MediaView = lớp − nội dung − 12 | `NativeAdFontScaleTest.collapsibleLayer_keepsHeight_fontScales` | Viết lại thành `collapsibleLayer_contentFollowsFontScale`: ở 1.0, 1.3, 1.5, 2.0, chữ ngắn và dài: nội dung (`CONTENT`) cao bằng `nativeSlotHeight()`; lớp cao max(299, nội dung + 132) dp; MediaView = lớp − nội dung − 12, ≥ 120 dp; ở 1.0 MediaView 181 dp, lớp 299 dp; CTA vừa chữ (`assertCtaFits`, `frameHeight` = lớp đo được); nút thu gọn cách góc 4, 4 dp giữ |
| Vùng ảnh của lớp nội suy 160 tới 200 dp (173 ở 411), khe 126 dp, lớp 299 dp; 360 dp: 166 + 106 = 272 | Tổng 250 tới 400 dp (271 ở 360, 299 ở 411), vùng ảnh = tổng − nội dung (193 ở 411, 165 ở 360), nội dung 106 dp | `CollapsibleNativeAdTest.media_fillsRemainingHeight`, `expanded_heightIsMediaAreaPlusSlot`, `expanded_heightIsMediaAreaPlusSlot_360`, `layer_smallScreen_fitsOnScreen`; `NativeAdDimensTest.collapsibleMediaAreaHeight_interpolatesAndClamps`, `collapsibleLayer_keepsDesign_whenRoom` | `media_fillsRemainingHeight`: MediaView 181 dp, rộng ≥ 120 dp. `expanded_heightIsMediaAreaPlusSlot`: lớp 299, MediaView 181, nội dung 106. `_360`: lớp 271, nội dung 106. `layer_smallScreen_fitsOnScreen`: lớp 271 dp, nút thu gọn và CTA trong màn 360 x 640 giữ. `collapsibleMediaAreaHeight_interpolatesAndClamps` thành `collapsibleTotalHeight_interpolatesAndClamps` (300→250, 320→250, 360→271, 411→299, 600→400, 673→400). `collapsibleLayer_keepsDesign_whenRoom` thành `collapsibleLayer_contentLeftForMedia` với `content = 106.dp`: 411 x 891, 411 x 700, 411 x 630 (299 + 16 = 315, đúng nửa 630) → 193 / 106 / 299; 360 x 640 → 165 / 106 / 271; 673 x 841 → 294 / 106 / 400; đối chứng 411 x 507 → `null` |
| Trần nửa cửa sổ với khe 126 dp: 411 x 600 → 158 / 126 / 284; 411 x 629 → 172 / 126 / 298; ngưỡng 548 dp; 411 x 524 không có lớp; 841 x 673 không có lớp | Cùng công thức với nội dung 106 dp: 411 x 600 → 178 / 106 / 284; 411 x 629 → 192 / 106 / 298; ngưỡng 508 dp; 411 x 524 có lớp (140 / 106 / 246); 841 x 673 có lớp (214 / 106 / 320, lớp cộng lề 336 dp ≤ 336.5) | `NativeAdDimensTest.collapsibleLayer_mediaShrinks_toHalfWindow`, `collapsibleLayer_null_whenNoRoom`, `collapsibleLayer_neverPastHalf_mediaAtLeast120`, `heights_areWholeDp`; `BottomAdBarTest.shortWindow_layerMediaShrinks` | `collapsibleLayer_mediaShrinks_toHalfWindow` (`content = 106.dp`): 411 x 600 → 178 / 106 / 284; 411 x 629 → 192 / 106 / 298; 411 x 508 và 411 x 509 → 132 / 106 / 238. `collapsibleLayer_null_whenNoRoom`: đối chứng 411 x 508 có lớp; `null` ở 411 x 507, 411 x 445, 411 x 411, 891 x 411, 640 x 360, 320 x 470; bỏ 411 x 524 và 841 x 673 khỏi danh sách, thêm câu riêng 841 x 673 → 214 / 106 / 320 (mục 8, câu hỏi 5). `collapsibleLayer_neverPastHalf_mediaAtLeast120`: lặp thêm `content` ở 106, 140, 180 dp; giữ "lớp + 16 ≤ nửa", "vùng ảnh ≥ 132", "dp nguyên"; "vùng ảnh ≤ thiết kế" thành "vùng ảnh ≤ max(tổng − content, 132)"; "khe = bottomSlotHeight" thành "slot = content"; đối chứng có cửa sổ không có lớp và có cửa sổ vùng ảnh bị co giữ. `heights_areWholeDp`: kiểm `collapsibleTotalHeight` và `mediumHeight` (bỏ `bottomSlotHeight`, `collapsibleMediaAreaHeight`), đối chứng 411 → 299 (298.75 làm tròn). `shortWindow_layerMediaShrinks`: lớp 284, đỉnh 300, MediaView 166, nội dung 106, `collapsibleLayerHeight()` 284 |
| Chỗ chừa = `bottomSlotHeight(cạnh ngắn)` + 16 (142 dp ở 411 và 891 x 411, 122 ở 360) | `nativeSlotHeight` + 16: 122 dp ở cỡ chữ 1.0 ở mọi khung; `bottomBarReservation` nhận chiều cao khe | `BottomAdBarTest.reservation_nativeWithAd_isSlotPlusMargin`, `reservation_bannerWithAd_isBannerHeight`, `reservation_noAd_isZero`, `reservation_notAllowed_isZero`, `reservedSlot_equalsDrawnSlotHeight`, `nativeReservation_followsScreenWidth`, `_360`, `nativeReservation_landscape_followsShortEdge`, `availabilityRepeatedTrue_reservationStable` (142), `NATIVE_RESERVATION_DP` | Mọi lời gọi `bottomBarReservation(..., windowShortEdge = X)` thành `nativeSlotHeight = 106.dp`; 142 dp thành 122 dp. `reservation_noAd_isZero`: đối chứng "khe có quảng cáo giữ chỗ" thành `106.dp + 16.dp` và 122 dp. `reservedSlot_equalsDrawnSlotHeight`: chỗ chừa trừ 16 bằng 106 dp và bằng khe vẽ ra. `nativeReservation_followsScreenWidth`, `_360`, `_landscape_followsShortEdge` thành `nativeReservation_sameEveryWindow` ba khung: 122 dp, khe 106 dp; câu "khe vẽ + 16 = chỗ chừa", "khe = nativeSlotHeight()" và đối chứng banner 62 dp giữ |
| Khe đáy 126 dp trong thanh đáy ở màn ngang và chia đôi (đỉnh 269, 303 dp) | 106 dp (đỉnh 289 ở 891 x 411, 323 ở 411 x 445) | `BottomAdBarTest.landscape_slotOnly_noLayer`, `split_slotOnly_noLayer`, `slotLoading_reservesPlaceholderSpace` (chú thích); `AdsControllerNativeStockTest.configurationChange_layerGoes_adDestroyed_logged`; `SampleScreenTest.whenLandscape_thenNoLayer_buttonReachable` | `landscape_slotOnly_noLayer`: khe 106, đỉnh 289, đáy 395, nội dung kết thúc ở 289, `reservations` = [0, 122], không có lớp, không thu thập giữ. `split_slotOnly_noLayer`: khe 106, đỉnh 323. `configurationChange_...`: khe 106 dp (dung sai 1 giữ), phần còn lại giữ. `whenLandscape_...`: khe 106, đỉnh 289, nội dung kết thúc ở 289, nút cuộn tới được giữ |
| Khe native nhỏ (`NativeAdSlot`, khe đáy, chỗ giữ khi trống) cao 126 dp ở 411 và ở 891 x 411 | 106 dp | `NativeAdSlotTest.loading_showsPlaceholder`, `unavailable_takesNoSpace` (đối chứng), `unavailable_keepsSpace_whenDeclaredNextToButton`, `keptSpace_landscape_followsShortEdge`, `buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace` (chú thích), `boundNativeAd_withFakeAd_drawsCard` (dòng 268), `bottomSlotHeight_equalsDrawnCardAndPlaceholderHeight`, `bottomNativeSlot_loading_showsPlaceholderOfSlotHeight`, `bottomNativeSlot_unavailable_takesNoSpace_reportsFalse` | 126 dp thành 106 dp ở từng câu; `keptSpace_landscape_followsShortEdge` thành `keptSpace_landscape_is106dp`; `bottomSlotHeight_equalsDrawnCardAndPlaceholderHeight` thành `smallHeight_equalsDrawnCardAndPlaceholderHeight`: thẻ và khung chờ cao bằng `BottomAdBarDefaults.nativeSlotHeight()` và bằng 106 dp; co về 0 khi lỗi giữ |
| Hàng native nhỏ trong danh sách 126 + 32 = 158 dp | 106 + 32 = 138 dp | `InListAdsTest` (dòng 234 và 426); `AdGalleryScreenTest.inList_insertsAdsAfterEveryNItems` (dòng small) | `126.dp + 16.dp * 2` thành `106.dp + 16.dp * 2`; `AdGalleryScreenTest`: dòng small 138 dp, dòng medium 432 dp giữ |
| Thẻ SMALL trong khung SDK 126 dp | 106 dp | `NativeAdViewBindingTest.sdkLayout_keepsCardHeights` | Thẻ SMALL 106 dp, CTA 44 dp; phần MEDIUM giữ |
| Thanh đáy của màn mẫu và màn xem thử: khe 126 dp, lớp 272 dp ở 360 dp (đỉnh 352), khe thu gọn đỉnh 749 | Khe 106 dp; lớp 271 dp ở 360 dp (đỉnh 353); khe đỉnh 769 | `SampleScreenTest.whenReady_thenAdIsPinnedToBottom`, `whenSmallScreen_thenOpenLayerDoesNotCoverButton`; `AdGalleryScreenTest.collapsible_pinsAdAtBottom` (chú thích), `collapsibleCollapsed_slotAt749` | `whenReady_...`: cách đáy 16, rộng 379 giữ, cao 106. `whenSmallScreen_...`: đỉnh lớp 353 dp (640 − 16 − 271), lớp dưới nút giữ. `collapsible_pinsAdAtBottom`: lớp 299 dp, đỉnh 576 giữ, chú thích 106 dp. `collapsibleCollapsed_slotAt749` thành `collapsibleCollapsed_slotAt769`: đỉnh khe 769, cao 106, lề 16 giữ, danh sách kết thúc ở 769 |
| Nội dung màn mẫu kết thúc ở đỉnh khe 749 dp (411 x 891) | Đỉnh khe 769 dp | `SampleScreenTest.whenReady_thenBarContentEndsAtSlotTop` | Câu "nội dung kết thúc ở đỉnh khe" giữ; `749f` thành `769f` |
| Thu gọn lớp thì khe lộ ra cao 126 dp | Khe 106 dp | `CollapsibleNativeAdTest.loading_drawsNothing_slotVisible` | `126f` thành `106f`, chú thích "106 dp ở mọi bề rộng"; các câu khác giữ |
| Chỗ chừa của khe native trong thanh đáy thật bằng `NATIVE_RESERVATION_DP` (142) lúc tải, lớp mở, có quảng cáo, lỗi rồi có lại | 122 dp | `BottomAdBarTest.nativeSlot_reservationEqualsSlot_loadingLayerOpenAd`, `nativeSlot_reservationEqualsSlot_failedBeforeAd_layerOpen_adAgain`, `availabilityRepeatedTrue_reservationStable` | Câu giữ, hằng `NATIVE_RESERVATION_DP` đổi 142 thành 122 (chú thích "khe 106 dp và lề 16 dp") |
| Khung chờ SMALL: khối CTA 96 x 44 dp bên phải | Khối CTA ngang hết thẻ, cao 44 dp, dưới hàng trên | `NativeAdCardTest.smallPlaceholder_jbaseBlocks` | Thành `smallPlaceholder_twoTierBlocks`: khối icon 44 x 44 cách mép trái 12, mép trên 6; khối CTA rộng 355 dp (thẻ 379), cao 44, cách mép dưới 6; hai vạch 14 dp và 12 x 120 dp nằm giữa khối icon và mép phải trừ 28 dp |

## 4. Kiểm thử

Test viết trước ở bước 3 gồm các test mới dưới đây và mọi câu khẳng định viết lại của mục 3.5; cả hai thất bại trên code hiện tại vì chiều cao (126 thay 106 dp), bố cục một hàng hay hàm chưa có (lỗi biên dịch của `collapsibleTotalHeight`, `collapsibleLayer(window, content)`, `bottomBarReservation(..., nativeSlotHeight = ...)`: phiên ghi lỗi biên dịch làm lý do thất bại, như plan T-065). Test kích thước chạy `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi` trừ khi ghi khác, dung sai 0.5 dp (1 dp ở cỡ chữ khác 1.0, do làm tròn px của từng dòng). Cỡ chữ đặt bằng `WithFontScale` có sẵn trong `FontScaleTestSupport.kt`; `SMALL_SCALES` là hằng có sẵn của `NativeAdFontScaleTest`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1 tiêu chí 1, KB-32 tiêu chí 3: bố cục hai tầng (design/context/SampleScreen_ready_light.md, "Native small") | Compose UI test | `NativeAdCardTest.small_twoTierLayout` (thẻ 379 dp): thẻ 106 dp; icon 44 x 44, cách mép trái 12, mép trên 6; mép trái cột chữ cách icon 12; hàng tiêu đề cao 22 dp, nhãn "Ad" bên trái tiêu đề, tâm dọc lệch dưới 2 dp; tiêu đề 16 sp một dòng; dòng dưới 13 sp, `lineHeight` 18 sp, cao 18 dp, cách hàng tiêu đề 4 dp; mép phải tiêu đề cách mép phải thẻ ít nhất 28 dp; CTA cách mép trái 12, mép phải 12 (rộng 355), cao 44, chữ 18 sp, đỉnh CTA cách đáy hàng trên 6 dp (đáy icon), đáy CTA cách đáy thẻ 6 dp; không phần nào chồng lên phần khác (hộp icon, cột chữ, CTA rời nhau). Đổi sang không có nhà quảng cáo, mô tả dài: dòng dưới là mô tả, 1 dòng, bị ba chấm, thẻ vẫn 106 dp |
| KB-1 tiêu chí 1: góc AdChoices trống | Compose UI test | `NativeAdCardTest.small_leavesAdChoicesCornerFree` viết lại (mục 3.5) |
| KB-1 tiêu chí 2, KB-32: 106 dp ở mọi bề rộng, khung chờ cùng chiều cao | Compose UI test | `NativeAdCardTest.small_heightIs106dp_everyWindow` và các biến thể `@Config` `w360dp-h640dp`, `w673dp-h841dp`, `w891dp-h411dp`, `w640dp-h360dp` (mục 3.5); `NativeAdSlotTest.smallHeight_equalsDrawnCardAndPlaceholderHeight` |
| KB-1 tiêu chí 2: khung chờ hai tầng | Compose UI test | `NativeAdCardTest.smallPlaceholder_twoTierBlocks` (mục 3.5) |
| KB-1 tiêu chí 6, KB-32: chữ lớn thì thẻ cao thêm, các phần không bị cắt, chiều cao tính trước bằng chiều cao thật | Compose UI test | `NativeAdFontScaleTest.small_partsInsideCard_fontScales` (mới), khung 411, 360 (`w360dp-h640dp`), 320 dp (`w320dp-h640dp`), `SMALL_SCALES`, chữ mẫu và chữ dài: thẻ cao bằng `nativeSlotHeight()` đọc trong cùng composition, 106 dp ở 1.0, lớn hơn 106 dp ở 2.0, không giảm khi cỡ chữ tăng; khung chờ (`NativeAdPlaceholder` đặt cạnh, cùng cỡ chữ) cao bằng thẻ; đỉnh hàng trên cách đỉnh thẻ 6 dp, đỉnh CTA cách đáy hàng trên 6 dp, đáy CTA cách đáy thẻ 6 dp (dung sai 1 dp: đây là câu so hàm tính trước với bố cục thật, mục 3.4); đáy cột chữ không quá đáy hàng trên; ba phần rời nhau. Cùng với `small_fontScales_headlineBadgeCta_notCut_allWidths*` viết lại (chữ không bị cắt chiều cao, "Ad" đủ, tiêu đề ít nhất 4 / 4 / 1 ký tự, "Install" hiện đủ) |
| KB-1 tiêu chí 6: nhãn "Ad" luôn thấy, dòng dưới một dòng ở chữ lớn | Compose UI test | `NativeAdFontScaleTest.small_badgeBeforeHeadline_oneSecondLine_fontScales` (mục 3.5) |
| KB-17 tiêu chí 5, D-04: chỗ chừa = khe vẽ + 16 dp, mọi khung | Unit test, Compose UI test | `BottomAdBarTest.reservation_*`, `reservedSlot_equalsDrawnSlotHeight`, `nativeReservation_sameEveryWindow` (mục 3.5) |
| KB-17 tiêu chí 5, KB-32: chỗ chừa theo cỡ chữ | Compose UI test | `BottomAdBarTest.nativeReservation_followsFontScale` (mới): `WithFontScale(2f)`, `AdsTestTheme`, thanh đáy giả như `assertNativeReservation`: chỗ chừa = khe vẽ ra + 16 dp (dung sai 0.5), lớn hơn 122 dp; đối chứng ở 1.0 cùng test: 122 dp. Và `BottomAdBarTest.slotLoading_largeText_reservesPlaceholderSpace` (mới): thanh đáy thật (`setBar` có sẵn) ở cỡ chữ 2.0, khe đang tải: khung chờ cao bằng khe, nội dung kết thúc ở đỉnh khung chờ (như `slotLoading_reservesPlaceholderSpace`) |
| KB-3: hàng native nhỏ trong danh sách | Compose UI test | `InListAdsTest` dòng 234 và 426 viết lại (138 dp); `AdGalleryScreenTest.inList_insertsAdsAfterEveryNItems` (dòng small 138 dp) |
| KB-4 tiêu chí 1, KB-32: lớp mở = tổng nội suy, vùng ảnh phần còn lại | Unit test, Compose UI test | `NativeAdDimensTest.collapsibleTotalHeight_interpolatesAndClamps`, `collapsibleLayer_contentLeftForMedia`; `CollapsibleNativeAdTest.expanded_heightIsMediaAreaPlusSlot`, `_360`, `media_fillsRemainingHeight`, `layer_contentIsSmallNative` (mục 3.5) |
| KB-32 tiêu chí 3: chữ lớn làm ảnh dưới 120 dp thì lớp cao thêm | Unit test | `NativeAdDimensTest.collapsibleLayer_growsWhenContentLeavesMediaBelow120` (mới): 411 x 891, `content` 167 dp → 132 / 167 / 299 (đúng ranh); 168 → 132 / 168 / 300; 180 → 132 / 180 / 312; 411 x 700, 180 → 132 / 180 / 312 (phòng 154); 411 x 600, 180 → `null` (phòng 104) |
| KB-32 tiêu chí 3: lớp thu gọn ở chữ lớn | Compose UI test | `NativeAdFontScaleTest.collapsibleLayer_contentFollowsFontScale` (mục 3.5) |
| KB-4 tiêu chí 4, CL-05: trần nửa cửa sổ, không đủ chỗ thì không có lớp | Unit test, Compose UI test | `NativeAdDimensTest.collapsibleLayer_mediaShrinks_toHalfWindow`, `collapsibleLayer_null_whenNoRoom`, `collapsibleLayer_neverPastHalf_mediaAtLeast120`; `BottomAdBarTest.landscape_slotOnly_noLayer`, `split_slotOnly_noLayer`, `shortWindow_layerMediaShrinks`, `collapsibleLayerHeight_nullWhenNoRoom*` (mục 3.5) |
| KB-4 tiêu chí 4, CL-05: chữ lớn làm cửa sổ hết chỗ thì không lấy quảng cáo, không vẽ | Compose UI test | `BottomAdBarTest.collapsibleLayerHeight_largeTextLeavesNoRoom` (mới, `@Config(qualifiers = "w411dp-h520dp-xxhdpi")`): ở 1.0 `collapsibleLayerHeight()` = 244 dp (vùng ảnh 138, phòng floor(260 − 122)); ở 2.0, đối chứng trước `nativeSlotHeight()` > 112 dp, rồi `collapsibleLayerHeight()` = `null`. `CollapsibleNativeAdTest.largeText_noRoom_collectsNothing` (mới, cùng khung): `CollapsibleNativeAdLayerHost` với giá trị mặc định `hasRoom` trong `WithFontScale(2f)`: không thu thập luồng (`collections` = 0), không vẽ `AD`; đối chứng cùng test ở 1.0: thu thập 1 lần, vẽ lớp khi có quảng cáo |
| KB-4 tiêu chí 4, CL-05, MC-14: lớp thật ở chữ lớn hết chỗ thì không lấy kho, không ghi `af_native_ad_eligible`, ghi một dòng nhật ký | Compose UI test | `AdsControllerNativeStockTest.largeText_layerNoRoom_stockKept_noEligible_logged` (mới, theo mẫu `landscape_layerNoRoom_stockKept_noEligible_logged`, khung `w411dp-h520dp-xxhdpi`, `BottomAdBarScaffold` bọc trong `WithFontScale(2f)`): lớp không vẽ, kho `native_collap` vẫn giữ quảng cáo, không có sự kiện eligible của lớp, đúng một dòng `Collapsible <mã> not shown: window 411x520 dp has no room`; đối chứng `largeText_layerNoRoom_fontScale1_layerShown` (mới, cùng khung, cỡ chữ 1.0): lớp vẽ, có một sự kiện eligible của lớp, không có dòng nhật ký đó |
| D-04: khe đáy của thanh đáy thật vẽ đúng chiều cao chỗ chừa | Compose UI test | Các test có sẵn `contentBottom_meetsSlotTop_whenSlotHasAd` (so tương đối) và `nativeSlot_reservationEqualsSlot_*` (so với `NATIVE_RESERVATION_DP`, nay 122; mục 3.5) |
| Hành vi không đổi: medium, thẻ đầy đủ, thẻ dọc, thẻ ở bước | Compose UI test | Các test có sẵn của MEDIUM, FULL, CARD, STEP trong `NativeAdCardTest`, `NativeAdFontScaleTest`, `NativeAdViewBindingTest` giữ nguyên, phải qua |
| Giao diện: native nhỏ, khung chờ, chữ lớn | Screenshot test | `NativeAdCardScreenshotTest`: `NativeAdCard_small_light.png`, `NativeAdCard_small_dark.png`, `NativeAdPlaceholder_small_light.png`, `NativeAdCard_font-scale-2_light.png`, `NativeAdCard_long-texts_light.png` (ghi lại) |
| Giao diện: thanh đáy, lớp thu gọn | Screenshot test | `BottomAdBarScreenshotTest` (5 ảnh `BottomAdBar_*`), `CollapsibleNativeAdScreenshotTest` (`CollapsibleNativeAd_expanded_light.png`, `_dark`, `_small`) (ghi lại) |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `NativeAdCard_font-scale-2_small.png`, `CollapsibleNativeAd_expanded_small.png`, `SampleScreen_ready_small.png`, `SampleScreen_bar-expanded_small.png`, `AdGalleryScreen_default_small.png`, `AdGalleryScreen_inlist_small.png`, `AdGalleryScreen_collapsible-expanded_small.png` (ghi lại) |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, ảnh so sánh bằng `scripts/design-compare/DesignCompare.java` (cách chạy dưới bảng), rà soát độc lập theo mục 7 của mẫu thiết kế; bảng kết quả ghi vào mục 8 của README thiết kế | `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light` (`:feature:sample`); `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` (`:app`). Phần native nhỏ và lớp thu gọn lệch thiết kế dưới 4 dp |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-110): `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light` (ảnh chuẩn cùng tên vẫn ghi lại ở plan này và so với thiết kế cho phần native nhỏ; medium trong ảnh khớp ở T-110) |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-013, T-053): `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Bảng chữ không đổi (chữ quảng cáo do SDK trả, nhãn "Ad" có sẵn): `NativeAdCardTest.badgeText_isAd` giữ |
| Ghi lại ảnh chuẩn | Lệnh | `./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun`, `./gradlew :feature:sample:recordRoborazziDebug :feature:sample:testDebugUnitTest --rerun`, `./gradlew :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun`; rồi `git status --short -- '*/src/test/screenshots/*'` chỉ được thấy đúng 31 ảnh của bảng 3.2 (14 của `core/ads`, 8 của `feature/sample`, 9 của `app`); ảnh khác đổi vì nhiễu thì `git checkout -- <ảnh>` |
| Quảng cáo thật trên máy: khe đáy 106 dp, CTA ngang hết thẻ, AdChoices không đè chữ; lớp thu gọn MediaView đúng phần còn lại | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, màn mẫu: xem khe đáy và lớp thu gọn ở cỡ chữ mặc định và cỡ chữ lớn nhất của máy; xoay ngang thì chỉ còn khe 106 dp |
| Máy gập mở ra (673 x 841) và xoay ngang (841 x 673) | Kiểm tra thủ công (máy thật: gập có bản lề) | Khe 106 dp; lớp 400 dp khi dọc, 320 dp khi ngang (mục 8, câu hỏi 5) |
| Tài liệu luật và hướng dẫn khớp code | Kiểm tra thủ công (người: đọc tài liệu) | Đọc `ads-business-rules.md` mục 5.7, D-02, D-04, O-08, CL-05, mục 11 và `ads-guide.md` mục 3, 4, 5 trong diff |

Ảnh so sánh với thiết kế (ADR-0020): ảnh chuẩn của feature ads nằm ở `feature/sample` và `app`, không ở `feature/ads/`, nên như T-065 chạy thẳng công cụ so ảnh cho từng thư mục, ra thư mục tạm, rồi chép các ảnh `*_compare.png` của mục 3.2 vào `docs/features/ads/design/review/`:

```
java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens feature/sample/src/test/screenshots ${TMPDIR:-/tmp}/t109-compare-sample
java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens app/src/test/screenshots ${TMPDIR:-/tmp}/t109-compare-app
```

**Ngoại lệ viết test trước:** tài liệu (`ads-business-rules.md`, `ads-guide.md`, README thiết kế, ảnh `review/`), chú thích trong `BottomAdBarScreenshotTest.kt`, `NativeAdCardScreenshotTest.kt`, `SampleScreenScreenshotTest.kt`, và các ảnh chuẩn ghi lại (ảnh chuẩn chỉ có sau khi code đổi; câu kích thước của chúng đã có test UI viết trước ở trên).

### 4.1. Chạy thử

Không cần (E dưới 2): `TextMeasurer` đo dòng chữ cho chiều cao thẻ đã dùng ở `stepCardHeight` (plan T-045), `SubcomposeLayout` của phương án dự phòng là API Compose có sẵn.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi kích thước hiển thị của native nhỏ và lớp thu gọn, mục 1 điều 1 của risk-scoring.md); module core dùng chung `:core:ads` (điều 7).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:sample`, `:app`), đổi hành vi công khai của `BottomAdBarDefaults` (theo cỡ chữ), 31 ảnh chuẩn |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed` với câu hỏi 66, thiết kế T-108 đã duyệt có số chi tiết ở cỡ chữ 1.0; hành vi ở chữ lớn chỉ có quy tắc chữ (cao thêm, không cắt), số do code tính; hai quyết định chờ người duyệt (tách medium, bỏ quy tắc chữ lớn của T-065: mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi kích thước, vị trí tương đối và ngưỡng có test Robolectric ở nhiều khung và cỡ chữ; quảng cáo thật (AdChoices, MediaView của SDK), máy gập và cỡ chữ hệ thống thật kiểm tay |
| D. Ảnh hưởng chéo | 3 | Chỗ chừa của thanh đáy đổi trên mọi màn có thanh đáy (feature dựa vào), ảnh chuẩn của ba module |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn (`stepCardHeight`, `collapsibleLayer`) |
| **Tổng** | 9 | **Mức: Cao** (cả do điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt`, `BottomAdBar.kt`, `CollapsibleNativeAd.kt`, `NativeAdSlot.kt` của `:core:ads`; `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`; app tạo từ mẫu nhận chỗ chừa mới qua `template-sync.sh`.
- Task khác trong sprint có thể xung đột: T-110 (medium) cùng sửa `NativeAdCard.kt`, `NativeAdCardTest.kt`, `NativeAdFontScaleTest.kt`, `NativeAdViewBindingTest.kt`, `AdGalleryScreenTest.kt`, ảnh `AdGalleryScreen_default_*`, `AdGalleryScreen_inlist_*`, `NativeAdCard_font-scale-2_light`, hai tài liệu luật.
- Đề xuất thứ tự chạy: T-109 trước, T-110 sau (`depends_on: [T-109]`), cùng một danh sách chạy để bản staging không ở lâu với native nhỏ mới và medium cũ.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn có khe đáy hay lớp thu gọn mà không nằm trong mục 3.2 đổi (ví dụ `NetworkLostScreen_*`, `AdGalleryScreen_banners_*`): dừng, vì nghĩa là một màn khác dựa vào chiều cao cũ mà plan chưa thấy.
- Chiều cao native nhỏ ở cỡ chữ 1.0 khác 106 dp ở bất kỳ khung xxhdpi (mật độ 3) nào của mục 4: dừng, không chỉnh hằng số cho khớp. Mật độ 2.625 chỉ kiểm khoảng 106 tới 108 dp (mục 3.5).

## 8. Câu hỏi cho người duyệt

1. **Tách medium sang T-110.** Ticket T-109 cỡ L và gợi ý cân nhắc tách. Plan này chỉ làm native nhỏ và lớp thu gọn; medium (250 dp, ảnh sát mép trên, bỏ đoạn mô tả, bỏ khung thấp `MediumCompactBelow`) thành ticket T-110 (tạo cùng PR này, `depends_on: [T-109]`). Hệ quả: giữa hai lượt, ảnh `AdGalleryScreen_default_*`, `inlist_*`, `NativeAdCard_font-scale-2_light` có native nhỏ mới và medium cũ, ghi lại hai lần. Đề xuất: tách, chạy hai ticket liền nhau trong một danh sách.
2. **Bỏ hai quy tắc chữ lớn của plan T-065** (cách nhau 8 dp, lề phải 16 dp từ cỡ chữ 1.3; nhãn "Ad" xuống dòng dưới từ cỡ chữ 1.5). Cả hai viết cho bố cục một hàng có CTA bên phải; nay CTA ở tầng dưới, cột chữ rộng hơn khoảng 130 dp. Đề xuất: bỏ cả hai; nhãn "Ad" luôn trước tiêu đề, dòng dưới luôn một dòng như thiết kế T-108; test giữ ý định "tiêu đề còn chỗ" với cùng ngưỡng hôm nay (ít nhất 4 ký tự trước dấu ba chấm ở khung 411 và 360 dp, 1 ở 320 dp, cỡ chữ tới 2.0). O-08 của luật viết lại theo đó.
3. **Tổng của lớp thu gọn nội suy thẳng 250 tới 400 dp** (271, 299, 400) thay vì cộng hai số đã làm tròn (272, 299, 400). Cả hai khớp thiết kế trong 4 dp; cách đầu khớp đúng số thiết kế ghi ở 360 dp (khoảng 271). Đề xuất: nội suy thẳng.
4. **Khe đáy cao thêm khi chữ lớn** làm chỗ chừa của mọi màn có thanh đáy đổi theo cỡ chữ (122 dp ở cỡ chữ 1.0, nhiều hơn ở chữ lớn). Feature đọc qua `BottomAdBarDefaults.nativeReservation()` như cũ; app nào đặt chỗ chừa bằng hằng số (không qua hàm) sẽ lệch. Mẫu không có chỗ như vậy; ghi vào `ads-guide.md`. Đồng ý?
5. **Máy gập mở ra xoay ngang (841 x 673 dp) nay có lớp thu gọn** (320 dp: vùng ảnh co còn 214 dp; lớp cộng lề 336 dp, dưới nửa 336.5 dp). Trước đây khe 200 dp làm cửa sổ này không đủ chỗ; CL-05 của luật đang lấy nó làm ví dụ "không đủ chỗ". Đây là hệ quả thẳng của câu hỏi 66 (khe 106 dp) và trần nửa cửa sổ của câu hỏi 65, spec không cần đổi (KB-4 tiêu chí 4 chỉ nêu điện thoại xoay ngang và chia đôi). Đề xuất: giữ theo công thức, bỏ ví dụ máy gập xoay ngang khỏi CL-05; ngưỡng mới ở cửa sổ rộng 411 dp là chiều cao 508 dp.

## 9. Quyết định duyệt

- **Quyết định:** duyệt, 2026-11-17, trong phiên có người theo dõi (ADR-0048, phần C). Lời đồng ý nguyên văn của anh Nhật: "Duyệt. Chấp nhận câu trả lời theo đề xuất của AI."
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** năm câu hỏi ở mục 8 theo đề xuất: (1) tách medium sang T-110, chạy liền sau T-109 trong cùng danh sách; (2) bỏ hai quy tắc chữ lớn của plan T-065, nhãn "Ad" luôn trước tiêu đề; (3) tổng lớp thu gọn nội suy thẳng 250 tới 400 dp (271 dp ở 360 dp); (4) chỗ chừa của thanh đáy đổi theo cỡ chữ, ghi vào `ads-guide.md`; (5) giữ công thức, bỏ ví dụ máy gập xoay ngang khỏi CL-05.

## 10. Nhật ký thực hiện
