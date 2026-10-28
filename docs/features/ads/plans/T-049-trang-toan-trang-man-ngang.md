---
ticket: T-049
title: Trang toàn trang của luồng không để trống khi xoay ngang trên điện thoại thật
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 10M          # cỡ S theo ticket, số gợi ý 4.9M (scripts/token-usage.py estimate, 2026-10-28); plan thêm dòng giải thích của màn xem thử khi trang quá thấp, hàm công khai mới, ghi lại 2 ảnh và tạo 2 ảnh ở hai module, nên lấy khoảng 2 lần số của cỡ S
blocking_points: [ads, core, analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-28
---

# PLAN T-049: TRANG TOÀN TRANG CỦA LUỒNG KHÔNG ĐỂ TRỐNG KHI XOAY NGANG TRÊN ĐIỆN THOẠI THẬT

## 1. Tóm tắt

Trên Redmi Note 12 xoay ngang, vùng trang toàn trang của màn xem thử chỉ cao 186,5 dp, dưới mức tối thiểu 200 dp của T-041, nên trang không lấy quảng cáo và để trắng. Test Robolectric giả các phần lề hệ thống của máy (thanh trạng thái 94 px, khoét camera 94 px bên trái, thanh điều hướng 44 px ở đáy, khung `w873dp-h393dp-440dpi`) tái hiện đúng từng điểm ảnh vùng trang của máy thật. Đề xuất chính: bố cục hàng ngang gọn bỏ hàng trên 36 dp riêng, chừa ô 36 dp ngay bên phải đầu quảng cáo (AdChoices thật vẫn do SDK ghim ở góc trên phải của trang, không phần nào của quảng cáo cắt vào góc đó); mức tối thiểu giảm từ 200 dp xuống 152 dp ở cỡ chữ 1.0, nên trang lấy quảng cáo ở màn ngang của máy này từ cỡ chữ 1.0 tới 1.8 (vùng 173,8 dp, mức 172,4 dp ở 1.8). Trang vẫn thấp hơn mức (cỡ chữ 2.0 ở màn ngang) thì màn xem thử ghi một dòng giải thích thay cho vùng trắng, nhờ hàm công khai mới `NativeFullScreenStepDefaults.minHeight()`.

## 2. Hiểu yêu cầu

Ticket xử lý KB-20 trong `features/ads/spec.md` (spec `confirmed`), luật MC-06, OB-01, O-08 trong `docs/ops/ads-business-rules.md`. Lỗi gốc: FB-20261028-01 (`features/ads/feedback/FB-20261028-01-trang-toan-trang-man-ngang-de-trong.md`).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-20 | Khi người vận hành bật vị trí native của luồng thì bước mà spec của app chọn có native cao 250 dp hoặc native toàn trang. | Ở màn ngang của điện thoại thường (khoảng 873 x 393 dp, có thanh trạng thái, thanh điều hướng, khoét camera), trang toàn trang lấy quảng cáo và hiện đủ phần bắt buộc ở cỡ chữ 1.0. Đo: vùng trang 186,5 dp; mức tối thiểu mới 152 dp. |
| KB-20 | Native toàn trang không có nút điều khiển riêng; nút Tiếp của app vẫn ở chỗ quen thuộc và không bị quảng cáo che. | Giữ nguyên (T-032, T-041). Bố cục mới không đổi vùng của trang, nút Next không đổi chỗ. Phần bắt buộc (tiêu đề, nhãn "Ad", AdChoices, CTA 52 dp) nằm trọn trong trang ở mọi chiều cao trang lấy quảng cáo. AdChoices vẫn ở góc trên phải (O-08), không đè tiêu đề hay CTA. |
| KB-20 | Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống. | Phản hồi gốc: "không nên để một vùng trắng không lời giải thích". Trang thấp hơn mức tối thiểu không lấy quảng cáo như hôm nay. App biết mức đó qua `NativeFullScreenStepDefaults.minHeight()` và tự hiện nội dung của mình. Màn xem thử hiện một dòng giải thích. |

Thiết kế tham chiếu: `features/ads/design/README.md`, dòng "Trang native ở bước của luồng" (mục 3, biến thể `Flow step` của `Native/FullScreen`) và dòng `NativeFlowStep_full-page-compact_light` của mục 8. Bố cục hàng ngang gọn không có trong Figma (anh Nhật duyệt 2026-10-26, T-041); plan đổi vị trí ô chừa chỗ AdChoices trong bố cục này (mục 8, câu 2). Màn xem thử không có ảnh thiết kế cho trang "Flow steps"; trang mặc định có `AdGalleryScreen_default_light`, `_dark` (task không chạm).

ADR liên quan: ADR-0013, ADR-0020, ADR-0026, ADR-0029, ADR-0031, ADR-0032.

## 3. Giải pháp

### 3.1. Cách làm

**Vì sao vùng trang chỉ còn 186,5 dp** (chạy thử, mục 4.1). `AdGalleryActivity` gọi `enableEdgeToEdge()` và đặt `Modifier.safeDrawingPadding()` cho `AdGalleryRoute`. Trên máy ở màn ngang, phần lề an toàn là: trên 94 px (34,2 dp, thanh trạng thái), trái 94 px (khoét camera), đáy 44 px (16 dp, thanh điều hướng cử chỉ). Màn 393 dp trừ 50,2 dp lề, trừ tiêu đề trang (72 dp) và nút Next với lề (84 dp) còn 186,5 dp. Khung `w891dp-h411dp` của T-041 không có lề nên còn 255 dp. Mức tối thiểu của bố cục hàng gọn hôm nay: 16 + (36 hàng trên + 16 + 48 đầu + 16 + 52 CTA) + 16 = 200 dp.

**Phần 1. Bố cục hàng gọn thấp hơn (`:core:ads`).**

1. `NativeFlowStepLayout.kt`, nhánh hàng gọn của `FlowStepPageFrame`: cột bên phải không còn hàng trên 36 dp riêng. Dòng đầu của cột là một `Row`: đầu quảng cáo (`weight(1f)`) rồi ô chừa chỗ AdChoices rộng cố định 36 dp. Dòng này cao `max(headerHeight, 36 dp)`.
   - `FlowStepTopBar` (`NativeFlowStepLayout.kt`, dòng 223) và thanh của khung chờ (dòng 171) hôm nay dùng `fillMaxWidth()`; đặt trong `Row` cạnh phần `weight(1f)` thì chúng chiếm hết ngang. Vì vậy `topBar` nhận modifier từ `FlowStepPageFrame`, như `media`: trong hàng là `Modifier.size(36.dp)`, trong cột là `Modifier.fillMaxWidth().height(36.dp)` như hôm nay.
   - AdChoices thật do SDK ghim ở góc trên phải của `NativeAdView`, tức của cả trang (`ADCHOICES_TOP_RIGHT`, `NativeFlowStep.kt` dòng 353; README thiết kế dòng 79; O-08), không nằm trong ô 36 dp. Ô 36 dp chỉ chừa chỗ: nhờ nó và lề 16 dp, đầu quảng cáo, mô tả, CTA, media không cắt vào góc trên phải của trang. Ô `adChoices` của bản xem thử và test (`FakeAdChoices`) vẽ trong ô này. Dưới đó: mô tả nếu còn chỗ, khoảng trống `weight`, CTA 52 dp ở đáy. Lề 16 dp bốn phía, các phần cách nhau 16 dp, media bên trái như cũ.
2. `rowTextColumnMin(header)` thành `max(TopBarHeight, header) + PartGap + CallToActionHeight`. Từ đó:
   - `FlowStepMetrics.minHeight = 16 + max(120, max(36, header) + 16 + 52) + 16`: 152 dp ở cỡ chữ 1.0; 155,6 dp ở 1.3; 178,2 dp ở 2.0 (số ở 440 dpi, mục 4.1).
   - `columnMinHeight` không đổi (360 dp ở 1.0). Bố cục cột không đổi.
   - Số dòng mô tả trong hàng tính theo chỗ còn lại như cũ: ở 186,5 dp còn 1 dòng.
3. Khung chờ (`FlowStepPagePlaceholder`) dùng cùng `FlowStepPageFrame`, nên theo cùng bố cục: ô trống 36 dp nằm bên phải khối đầu; khối đầu rộng phần còn lại, không bị ô trống ép về 0. Khối đầu (`Row` của icon và hai vạch chữ) và khối CTA của khung chờ mang tag nội bộ mới `NativeFlowStepTestTags.PLACEHOLDER_HEADER`, `NativeFlowStepTestTags.PLACEHOLDER_CTA` (hôm nay chỉ gốc `FULL_PAGE_PLACEHOLDER` có tag), để test đo được.
4. `NativeFlowStep.kt`:
   - `NativeFullScreenStepDefaults.MinHeight` thành 152 dp; KDoc.
   - Hàm công khai mới: hàm thành viên `@Composable fun minHeight(): Dp` trong `object NativeFullScreenStepDefaults`, như `NativeStepAdDefaults.height()` của T-045. Thân: `val density = LocalDensity.current; val typography = MaterialTheme.typography; return remember(density, typography) { flowStepMetrics(density, typography).minHeight }`. KDoc phân biệt: `MinHeight` là hằng, mức ở cỡ chữ 1.0 (152 dp); `minHeight()` là mức ở cỡ chữ hiện tại, cao hơn khi chữ lớn; app chọn nội dung theo `minHeight()`, không theo hằng. App dùng nó để biết vùng của mình có đủ cho trang không và tự hiện nội dung khi không đủ.
   - Cổng (`NativeFullScreenStepGate`) không đổi logic: vẫn `fits = !hasBoundedHeight || maxHeight >= metrics.minHeight`. Chỉ số của mức đổi.
   - KDoc của `NativeFullScreenStep`: mức 152 dp ở cỡ chữ 1.0; app đọc `minHeight()` để hiện nội dung của mình khi vùng thấp hơn.

**Phần 2. Màn xem thử (`app/src/staging`).**

5. `FlowStepsPage`, trang toàn trang: bọc trong `BoxWithConstraints(Modifier.fillMaxSize())`.
   - `maxHeight >= NativeFullScreenStepDefaults.minHeight()`: gọi `slots.StepFullPage(...)` như hôm nay.
   - Thấp hơn: không gọi `StepFullPage`; hiện một dòng chữ giữa vùng, `bodyMedium`, màu `onSurfaceVariant`, tag mới `AdGalleryTestTags.FLOW_FULL_PAGE_TOO_SHORT`, chuỗi mới `ad_gallery_flow_full_page_too_short` ("Not enough room for the full-page ad", `translatable="false"` như các chuỗi khác của màn xem thử).
   - Pager, khung thẻ, nút Next không đổi (OB-01).

**Phần 3. Tài liệu.**

6. `docs/ops/ads-guide.md`: dòng trang toàn trang (mục 3, dòng 79) và đoạn "Trang toàn trang" (dòng 120): mức 152 dp ở cỡ chữ 1.0, hàm `minHeight()`, app tự hiện nội dung khi vùng thấp hơn; nhắc màn ngang của điện thoại có thanh hệ thống (vùng khoảng 187 dp ở 873 x 393 dp). Đoạn màn xem thử (dòng 219): dòng giải thích khi trang quá thấp.
7. `docs/features/ads/design/README.md`: dòng "Trang native ở bước của luồng" (mục 3): hàng gọn từ 152 dp, AdChoices cạnh đầu quảng cáo. Mục 8: sửa dòng `NativeFlowStep_full-page-compact_light`, thêm dòng hai ảnh mới của màn xem thử.

Mẫu làm theo:
- giả lề hệ thống bằng `ViewCompat.dispatchApplyWindowInsets` như `NativeFullScreenAdScreenTest` (dòng 269 tới 280);
- lấy activity của `createComposeRule()` bằng `(composeRule as AndroidComposeTestRule<*, *>).activity` (`CLAUDE.md`, bước 10);
- hàm công khai đo theo cỡ chữ như `NativeStepAdDefaults.height()` (T-045);
- cách đo của `NativeFlowStepLayoutTest`, `NativeFlowStepTest.fullScreenStep_belowMinHeight_takesNothing`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayout.kt` | Hàng gọn: ô chừa chỗ AdChoices 36 dp cạnh đầu quảng cáo, bỏ hàng trên riêng; `topBar` nhận modifier; `rowTextColumnMin`; tag `PLACEHOLDER_HEADER`, `PLACEHOLDER_CTA` trên khối đầu và khối CTA của `FlowStepPagePlaceholder`; KDoc của `FlowStepPageLayout`, `FlowStepMetrics` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `NativeFullScreenStepDefaults.MinHeight` 152 dp, hàm thành viên `minHeight()`; hằng tag `PLACEHOLDER_HEADER`, `PLACEHOLDER_CTA` trong `NativeFlowStepTestTags`; KDoc của `NativeFullScreenStep`, `NativeFullScreenStepGate` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayoutTest.kt` | Test mới mục 4; viết lại `fontScale_requiredPartsKeepSize` (mục 3.5); thêm 186,5 và 152 dp vào `HEIGHTS` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepTest.kt` | Test mới mục 4; viết lại số của hai test (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeEligibleReportTest.kt` | Viết lại số của `fullScreenStep_belowMinHeight_noEligible` (mục 3.5) |
| Sửa | `core/ads/src/test/screenshots/NativeFlowStep_full-page-compact_light.png` | Ghi lại: ô chừa chỗ AdChoices cạnh đầu quảng cáo (chạy thử thấy ảnh này đổi) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `FlowStepsPage`: dòng giải thích khi vùng thấp hơn `minHeight()`; tag `FLOW_FULL_PAGE_TOO_SHORT`; KDoc |
| Sửa | `app/src/staging/res/values/strings.xml` | Chuỗi `ad_gallery_flow_full_page_too_short` |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PhoneInsets.kt` | Hằng lề của máy (94, 94, 44 px; khung `w873dp-h393dp-440dpi`) và hàm phụ `dispatchPhoneLandscapeInsets(rule)`: phát lề một lần tới `decorView`; dùng chung cho hai lớp test |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Test mới mục 4; hàm phụ `Screen`, `setScreen` nhận `modifier` (để đặt `safeDrawingPadding()` như `AdGalleryActivity`) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenScreenshotTest.kt` | Hai ảnh mới (mục 4); `setScreen` nhận `modifier` |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` | Ghi lại: trang 204 dp ở `w640dp-h360dp` dùng hàng gọn mới (chạy thử thấy ảnh này đổi) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_landscape-phone.png` | Ảnh chuẩn mới, khung `w873dp-h393dp-440dpi` có lề của máy, cỡ chữ 1.0: trang 186,5 dp có quảng cáo |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_too-short.png` | Ảnh chuẩn mới, cùng khung và lề, `fontScale = 2.0f`: dòng giải thích |
| Sửa | `docs/ops/ads-guide.md` | Mục 3.1, bước 6 |
| Sửa | `docs/features/ads/design/README.md` | Mục 3.1, bước 7 |

Không ghi lại ảnh nào khác. Ảnh dự kiến giữ nguyên từng byte (chạy thử với phần sửa của Phần 1 áp tạm, mục 4.1): `NativeFlowStep_full-page_light`, `_dark`, `_small`, `_full-page-placeholder_light` (bố cục cột), `AdGalleryScreen_flow-full-page_light`, `_small` (cột), mọi ảnh `NativeFullScreenAdScreen_*`, `AdGalleryScreen_default_*` (cỡ chữ 1.0).

**Khung rỗng vào commit test của bước 3 (L-025)**, để test viết trước biên dịch được và đỏ đúng lý do:
- hàm thành viên `@Composable fun minHeight(): Dp` trong `object NativeFullScreenStepDefaults`, có `remember(density, typography)`, trả `flowStepMetrics(...).minHeight` của code hiện tại (200 dp ở 1.0): hàm thật, chỉ chưa đổi số; test số 152 dp đỏ;
- hằng tag `NativeFlowStepTestTags.PLACEHOLDER_HEADER`, `PLACEHOLDER_CTA` gắn sẵn vào khối đầu và khối CTA của khung chờ (chỉ thêm tag, bố cục chưa đổi), để test khung chờ đỏ vì bố cục, không vì thiếu tag;
- hằng `AdGalleryTestTags.FLOW_FULL_PAGE_TOO_SHORT` (chưa gắn vào giao diện) và chuỗi `ad_gallery_flow_full_page_too_short`;
- `PhoneInsets.kt` đủ (chỉ là phần của test).

Phần sửa thật (bố cục, mức, dòng giải thích) và hai ảnh ghi lại vào commit code của bước 4. Câu viết lại của mục 3.5 vào commit test của bước 3 (chúng đỏ trên khung rỗng vì số mới).

**Nơi đọc file bị đổi (L-020):**
- `FlowStepMetrics.minHeight`: cổng `NativeFullScreenStepGate`, `FlowStepPageFrame`; hàm mới `minHeight()`; test ở mục 3.5.
- `NativeFullScreenStepDefaults.MinHeight`: chỉ KDoc và `ads-guide.md` (không code nào đọc).
- `FlowStepPageFrame` (nhánh hàng): `FlowStepPageLayout` (trang có quảng cáo, `NativeFullScreenStepContent`), `FlowStepPagePlaceholder`. Màn sau inter (`NativeFullScreenAdScreen`, `NativeFullScreenOverlay`) không đi qua đây.
- Ảnh chuẩn: `scripts/check.sh` (`verifyRoborazzi*`, đòi ảnh của mỗi `captureRoboImage`), `scripts/test-summary.sh` (đếm). `scripts/design-compare.sh` chỉ đọc `feature/<tên>`. Không test hay workflow nào đọc tên ảnh.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay đổi định dạng vị trí. Đổi cách vẽ của `native_fullscreen` khi làm trang của luồng ở bố cục hàng gọn (ô chừa chỗ AdChoices cạnh đầu quảng cáo; AdChoices thật vẫn do SDK ghim ở góc trên phải của trang) và mức tối thiểu lấy quảng cáo (200 xuống 152 dp ở cỡ chữ 1.0; điểm chặn 1). API công khai mới: hàm thành viên `NativeFullScreenStepDefaults.minHeight()`.
- Sự kiện phân tích: không thêm. `af_native_ad_eligible` vẫn ghi theo `allowed && shown && fits` (T-041), nhưng `fits` đúng ở nhiều chiều cao hơn (từ 152 dp), nên sự kiện được ghi ở trang 152 tới 200 dp, nơi hôm nay không ghi. Đây là đổi lúc ghi một sự kiện đã có (điểm chặn 4).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code dùng `BoxWithConstraints`, `Row`, `LocalDensity` (Compose, đã có ở `:core:ads` và `:app`). Test dùng `ViewCompat`, `WindowInsetsCompat`, `Insets` (androidx.core, đã có trên classpath test của `:core:ads` qua `NativeFullScreenAdScreenTest`; trên classpath test của `:app` qua chạy thử ở mục 4.1), `AndroidComposeTestRule` (ui-test-junit4, đã có).

### 3.4. Phương án dự phòng

Không có. Điểm chưa chắc của bản trước (giả lề với `createComposeRule()` ép kiểu, chụp ảnh sau khi giả lề) đã được chạy thử ở rà soát độc lập (mục 4.1): phát lề một lần tới `decorView` cho đúng 186,55 dp, và `captureRoboImage` chụp được. Phần chưa chạy thử (dòng giải thích của màn xem thử) là code Compose thường, không cần cách làm thay.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py <plan>` liệt kê 149 test dùng tên của ba file; 6 test "Cần xem trước" và 16 test "Nên xem" mà bảng dưới chưa nhắc, xử lý ở dòng cuối của bảng. Đã đọc các test đo trang toàn trang, cổng, eligible. Chạy thử Phần 1 áp tạm (mục 4.1): chỉ bốn test dưới đây đỏ, đúng như dự đoán, cộng hai ảnh chuẩn ghi lại theo mục 3.2. Phần 2 chưa áp tạm; nó chỉ đổi trang toàn trang của "Flow steps" khi vùng dưới mức.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mức tối thiểu 200 dp ở cỡ chữ 1.0 | 152 dp (bước 2) | `NativeFlowStepLayoutTest.fontScale_requiredPartsKeepSize`: `assertDp("least height at 1.0", 200f, atOne.minHeight)` | Câu mới: `assertDp("least height at 1.0", 152f, atOne.minHeight)`. Vòng lặp `listOf(360.dp, 200.dp)` thành `listOf(360.dp, 154.dp)`: với mức mới, 200 dp ở trên mức ở cả 1.0, 1.3, 2.0, nên nhánh `else` (qua cổng, không lấy quảng cáo) không chạy nữa và test lỏng đi. Ở 154 dp: cỡ 1.0 trên mức (152 dp) đi nhánh bố cục; cỡ 1.3 (155,6 dp) và 2.0 (178,2 dp) dưới mức đi nhánh cổng. Thêm hai bộ đếm `laidOut`, `gated` và câu cuối `assertEquals(4, laidOut)` (360 dp ở ba cỡ, 154 dp ở 1.0), `assertEquals(2, gated)` (154 dp ở 1.3 và 2.0): cả hai nhánh phải chạy. Số đã đo ở khung `xxhdpi` của test (người rà, 2026-10-28), cỡ 1.0 / 1.3 / 2.0: `minHeight` 152 / 155,33 / 178 dp; `columnMinHeight` 360 / 367,33 / 390 dp. 154 dp nằm giữa mức của 1.0 và 1.3 |
| Trang 190 dp không lấy quảng cáo; 200 dp lấy | Mức 152 dp | `NativeFlowStepTest.fullScreenStep_belowMinHeight_takesNothing`: `mutableStateOf(190.dp)`, `"nothing taken in 190 dp"`, đối chứng `height = 200.dp` | Câu mới: chiều cao đầu 151 dp (ngay dưới mức), `"nothing taken in 151 dp"`; đối chứng `height = 152.dp` lấy đúng 1 quảng cáo và có CTA. Giữ ý định: ngay dưới mức không lấy, đúng mức thì lấy |
| Trang co từ 484 xuống 190 dp thì trả quảng cáo, báo `false` | Mức 152 dp | `NativeFlowStepTest.fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`: `height = 190.dp` | Câu mới: `height = 151.dp`; các câu kiểm `released`, `[true, false]`, không còn chặn giữ nguyên. Đối chứng: co sang 152 dp (đúng mức) thì không trả quảng cáo, báo vẫn `[true]` |
| Không ghi eligible ở 190 dp; ghi ở 200 dp | Mức 152 dp | `NativeEligibleReportTest.fullScreenStep_belowMinHeight_noEligible`: `mutableStateOf(190.dp)`, đối chứng `200.dp` | Câu mới: 151 dp không có `eligible`; đối chứng 152 dp đúng một `eligible` |
| Hàng gọn: hàng trên 36 dp riêng, đầu quảng cáo dưới nó | Ô chừa chỗ AdChoices cạnh đầu quảng cáo (bước 1) | `NativeFlowStepLayoutTest.requiredParts_insidePage_atEveryHeight` (AdChoices không đè CTA, tiêu đề), `compactRow_belowColumnMin` (media bên trái CTA, CTA cách đáy 16 dp), `NativeAdFontScaleTest.fullScreen_ctaTextFits_fontScales` (hàng ở 255 dp) | Giữ: chạy thử với Phần 1 áp tạm, ba test qua |
| Ảnh hàng gọn | Ô chừa chỗ AdChoices cạnh đầu | `NativeFlowStepScreenshotTest.fullPage_compact_light`, `AdGalleryScreenScreenshotTest.flowFullPage_landscape` | Ghi lại hai ảnh (mục 3.2); test giữ nguyên |
| Trang toàn trang của màn xem thử luôn gọi `StepFullPage` | Dưới `minHeight()` thì hiện dòng giải thích (bước 5) | `AdGalleryScreenTest.flowSteps_fullPage_landscape_showsCta` (204 dp), `_landscapeWide_showsCta` (255 dp), `flowSteps_fullPage_fillsStepArea_small` (484 dp), `flowSteps_fullPage_hidesCard`, `flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard`, `flowSteps_pagerHeight_sameOnCardAndFullPage` | Giữ: mọi khung của các test này cho vùng trên 152 dp ở cỡ chữ 1.0 |
| Trang ở vùng cao (cột), cổng, khung chờ, dựng lại, eligible qua `AdsController` | Không đổi | Cần xem trước: `NativeFlowStepLayoutTest.unboundedHeight_media120`, `NativeFlowStepTest.nextButton_samePlace_notCovered_withFullPage`, `NativeFlowStepTest.stepCard_doesNotSuppress`, `NativeFullScreenAdScreenTest.flowStep_mediaStartsLikeAfterInter`, `NativeFullScreenAdScreenTest.flowStep_noWindowInsetPadding`, `NativeFullScreenAdScreenTest.flowStep_publicContent_hasNoCloseButton`. Nên xem: `AdGalleryScreenTest.texts_matchDesignTable`, `NativeEligibleReportTest.fullPageStepGate_withController_recordsOnlyWhenShown`, `NativeFlowStepScreenshotTest.fullPage_placeholder_light`, `NativeFlowStepTest.fullPage_notShown_neitherTakesNorSuppresses`, `NativeFlowStepTest.fullPage_suppressesResumeAds_whileShown`, `NativeFlowStepTest.fullScreenStep_allowedTurnsFalse_releasesAndUnsuppresses`, `NativeFlowStepTest.fullScreenStep_drawsOnlyWhenAllowed`, `NativeFlowStepTest.fullScreenStep_hasNoControls`, `NativeFlowStepTest.fullScreenStep_reportsTrueOnAd`, `NativeFlowStepTest.fullScreenStep_unavailable_drawsNothing`, `NativeFlowStepTest.fullScreenStep_waiting_drawsStaticPlaceholder_noSuppress`, `NativeFlowStepTest.fullScreenStep_withoutAdsController_drawsNothing_notReady`, `NativeFullScreenStepRecreateTest.recreate_consentClosed_notTaken`, `NativeFullScreenStepRecreateTest.recreate_newAd_suppressesAgain`, `NativeFullScreenStepRecreateTest.recreate_placementOff_noPlaceholder_noTake`, `NativeFullScreenStepRecreateTest.recreate_thenNewAdArrives_showsNewAd` | Giữ: các test này đặt trang ở vùng cao (`fillMaxSize` ở `w411dp-h891dp`, bố cục cột) hay không đo bố cục; chạy thử với Phần 1 áp tạm, tất cả qua |

Bốn câu trên là câu viết sẵn theo quyết định 1 của ADR-0031: plan nêu trước câu mới; phiên đưa chúng vào commit test của bước 3 và dùng như test viết trước (đỏ trên khung rỗng vì số mới, xanh sau bước 4). Đây không phải câu phiên tự viết lại giữa chừng, nên giới hạn ba câu của điều 6 không áp dụng. Đối chứng của từng câu:
- `fontScale_requiredPartsKeepSize`: hai bộ đếm nhánh (4 và 2) đỏ khi một nhánh không chạy;
- `fullScreenStep_belowMinHeight_takesNothing`: 151 dp không lấy, 152 dp lấy đúng 1;
- `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`: co sang 152 dp không trả, co sang 151 dp thì trả;
- `fullScreenStep_belowMinHeight_noEligible`: 151 dp không có, 152 dp đúng một `eligible`.

## 4. Kiểm thử

Test viết trước, thất bại đúng lý do trên code hiện tại. Số đo thật ở mục 4.1. Khung tái hiện lỗi gốc (L-029): `w873dp-h393dp-440dpi` (2400 x 1080 px, 440 dpi như Redmi Note 12 ở màn ngang) có lề hệ thống giả, cho vùng trang đúng `[94,292][2400,805]` px như cây giao diện của máy.

**Cách giả lề của máy** (`PhoneInsets.kt`): lấy activity bằng `(composeRule as AndroidComposeTestRule<*, *>).activity`; sau `setContent`, trên luồng giao diện, gọi một lần `ViewCompat.dispatchApplyWindowInsets(activity.window.decorView, insets)`, với `insets = WindowInsetsCompat.Builder().setInsets(statusBars(), Insets.of(0, 94, 0, 0)).setInsets(navigationBars(), Insets.of(0, 0, 0, 44)).setInsets(displayCutout(), Insets.of(94, 0, 0, 0)).build()`, rồi `waitForIdle()`. Màn xem thử đặt trong `Surface(Modifier.fillMaxSize()) { AdGalleryScreen(..., modifier = Modifier.safeDrawingPadding()) }` như `AdGalleryActivity`. Đây là cách tái hiện được lỗi (mục 4.1): khung nhỏ hơn không lề `w839dp-h343dp` cho cùng chiều cao (186,9 dp) nhưng không có lề trái 34 dp; khung của T-041 không có lề nào.

**`NativeFlowStepLayoutTest`** (`:core:ads`, khung có sẵn `w360dp-h640dp-xxhdpi`, trừ khi ghi khác):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20: trang 186,5 dp ở màn ngang của máy có đủ phần bắt buộc | Compose UI test | `compactRow_phoneLandscape_requiredPartsInside`, `@Config(qualifiers = "w873dp-h393dp-440dpi")`, `h = 186.5.dp`, bộ chữ `Longest`. Kiểm: CTA cao 52 dp, cách đáy trang 16 dp, nằm trọn trong trang; media cao 154,5 dp, từ y = 16 dp; tiêu đề, nhãn "Ad" trong trang; ô AdChoices giả nằm bên phải tiêu đề (`adChoices.left >= headline.right`), không đè tiêu đề hay CTA; mô tả 1 dòng (cao 20 dp). Góc của AdChoices thật (O-08): không phần nào trong media, tiêu đề, nhãn "Ad", mô tả, CTA cắt vào hình vuông 24 x 24 dp ở góc trên phải của trang (`[page.right - 24, page.top, page.right, page.top + 24]`); cùng câu này kiểm ở 152 dp và ở bố cục cột 360 dp. Đột biến kiểm: bỏ ô chừa chỗ (đầu quảng cáo rộng hết cột) thì tiêu đề cắt vào góc, test đỏ. Hôm nay đỏ: hàng trên 36 dp chiếm chỗ, CTA bị ép còn khoảng 38 dp (người rà đo) |
| Khung chờ ở bố cục hàng | Compose UI test | `compactRow_placeholder_phoneLandscape`, cùng khung: `FlowStepPagePlaceholder` trong `Box(fillMaxWidth().height(186.5.dp))`. Đo bằng tag `NativeFlowStepTestTags.PLACEHOLDER_HEADER` và `PLACEHOLDER_CTA` (`useUnmergedTree = true`), trang là gốc `FULL_PAGE_PLACEHOLDER`. Kiểm: khối đầu rộng hơn 0 và `header.right <= page.right - 16 - 36 dp` (bên trái ô trống 36 dp); khối CTA cao 52 dp, nằm trọn trong trang. Đột biến kiểm: để thanh của khung chờ `fillMaxWidth()` trong hàng thì khối đầu rộng 0, test đỏ. Hôm nay đỏ: khối CTA bị ép ra ngoài 186,5 dp |
| Mức tối thiểu mới | Compose UI test | `fontScale_requiredPartsKeepSize` (câu viết lại ở mục 3.5): `minHeight` 152 dp ở 1.0 |
| Phần bắt buộc ở mọi chiều cao lấy quảng cáo | Compose UI test | `requiredParts_insidePage_atEveryHeight`, `media_neverBelow120dp`: thêm 186,5 dp và 152 dp vào `HEIGHTS` (vào commit test). Hôm nay đỏ ở cả hai chiều cao (CTA bị ép ra ngoài) |
| App đọc được mức theo cỡ chữ | Compose UI test | `defaults_minHeight_followsFontScale`: trong `WithFontScale(s) { AppTheme { ... } }` với `s` là 1.0, 1.3, 2.0, `NativeFullScreenStepDefaults.minHeight()` bằng `flowStepMetrics(LocalDensity.current, MaterialTheme.typography).minHeight` (sai số 0,01 dp); ở 1.0 bằng 152 dp; tăng dần theo cỡ chữ. Đối chứng: `NativeFullScreenStepDefaults.MinHeight == 152.dp`. Hôm nay đỏ ở số 152 |

**`NativeFlowStepTest`** (`:core:ads`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20: cổng lấy quảng cáo ở 186,5 dp | Compose UI test | `fullScreenStep_phoneLandscape_takesAd`, `@Config(qualifiers = "w873dp-h393dp-440dpi")`: `NativeFullScreenStepGate(allowed = true, states = fullPageStates(ad, takes))` trong `Box(fillMaxWidth().height(186.5.dp))`, bọc trong `AdsHostContent(suppression, suppressor)`. Kiểm: `takes` có 1 phần tử, có `CTA`, không có `FULL_PAGE_PLACEHOLDER`, báo `[true]`, `suppressor.suppressed` là `true`. Đối chứng: cùng test ở 151 dp thì `takes` rỗng, không CTA, không khung chờ. Hôm nay đỏ: `takes` rỗng, báo `[]`, không CTA, không khung chờ (đo ở mục 4.1) |
| Dưới mức thì như không có quảng cáo | Compose UI test | `fullScreenStep_belowMinHeight_takesNothing`, `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse` (số viết lại ở mục 3.5) |

**`NativeEligibleReportTest`**: `fullScreenStep_belowMinHeight_noEligible` (số viết lại ở mục 3.5); thêm đối chứng ở 186,5 dp: đúng một `eligible`. Hôm nay đỏ ở 186,5 dp.

**Màn xem thử** (`AdGalleryScreenTest`, `:app` staging; khung `w873dp-h393dp-440dpi`, lề giả của máy, màn đặt trong `safeDrawingPadding()`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Tái hiện vùng trang của máy (L-029) | Compose UI test | `flowSteps_phoneLandscape_insets_reproducePageArea`: sau Next, `STEP_FULL_PAGE` (`useUnmergedTree = true`) có `left` 34,18 dp, `top` 106,18 dp, `bottom` 292,73 dp, cao 186,5 dp (sai số 0,5). Đối chứng: không giả lề thì trang cao 236,7 dp, `left` 0. Qua ngay trên code hiện tại (đây là bước tái hiện, có đối chứng đỏ khi bỏ lề) |
| KB-20: trang ở màn ngang của máy đủ cao để lấy quảng cáo | Compose UI test | `flowSteps_phoneLandscape_pageFitsAd`: đọc `NativeFullScreenStepDefaults.minHeight()` trong composition; kiểm vùng trang 186,5 dp `>=` mức đó; chữ `Install` nằm trọn trong trang; không có `FLOW_FULL_PAGE_TOO_SHORT`. Hôm nay đỏ: mức 200 dp |
| Trang quá thấp thì có dòng giải thích, không vùng trắng | Compose UI test | `flowSteps_phoneLandscape_fontScale2_showsTooShortNote`, `@Config(qualifiers = "w873dp-h393dp-440dpi", fontScale = 2.0f)`: vùng trang 168,4 dp, dưới mức 178,2 dp. Kiểm: có `FLOW_FULL_PAGE_TOO_SHORT` với chữ của chuỗi mới, nằm trong vùng giữa tiêu đề và nút Next; không có `STEP_FULL_PAGE`; nút Next cùng chỗ như ở bước 1 (OB-01). Đối chứng: cỡ chữ 1.0 ở cùng khung thì không có dòng giải thích và có `STEP_FULL_PAGE`. Hôm nay đỏ: không có tag |
| Thẻ, nút Next không đổi | Compose UI test | `flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard`, `flowSteps_pagerHeight_sameOnCardAndFullPage` (có sẵn, giữ xanh) |

**Screenshot test:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn ngang của máy | Screenshot test khung `w873dp-h393dp-440dpi`, lề giả | `flowFullPage_landscapePhone`: `src/test/screenshots/AdGalleryScreen_flow-full-page_landscape-phone.png` (trang 186,5 dp có quảng cáo xem thử, lề trái 34 dp) |
| Trang quá thấp | Screenshot test cùng khung, `fontScale = 2.0f` | `flowFullPage_tooShort`: `src/test/screenshots/AdGalleryScreen_flow-full-page_too-short.png` |
| Hàng gọn mới | Screenshot test | Ghi lại `src/test/screenshots/NativeFlowStep_full-page-compact_light.png` (`core/ads`) và `src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` (`app`) |
| Đúng thiết kế (khi có `design/screens/`) | Không chạy so sánh thiết kế: bố cục hàng gọn và trang "Flow steps" không có ảnh thiết kế (README mục 8). Người duyệt PR xem ảnh hàng gọn mới | Xem dòng dưới |
| | | Ảnh thiết kế của ticket khác (T-027, T-028, T-030): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`, `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |

Mọi lệnh `captureRoboImage` ghi thẳng đường dẫn `"src/test/screenshots/<Tên>.png"`. Hai ảnh mới không có hiệu ứng chạy mãi (L-048 không áp dụng).

**Kiểm tra thủ công:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn ngang trên máy thật có quảng cáo | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging trên Redmi Note 12 (hay điện thoại thường khác), cỡ chữ 1.0: màn xem thử, "Flow steps", Next tới trang toàn trang, xoay ngang. Thấy khung chờ rồi quảng cáo trong bố cục hàng gọn: media bên trái, CTA thấy trọn và bấm được, AdChoices góc trên phải không đè chữ. Log `VGL.Ads` có `Flow step full page shown: gallery_native_step_full` khi đang ngang. Ở cỡ chữ 200%, khi trang quá thấp, màn xem thử không dựng `NativeFullScreenStep` (mục 3.1 bước 5): trang rời composition nên quảng cáo cũ được trả qua dispose (log `Flow step full page left`), không qua nhánh `fits` của cổng; nhật ký kiểm thủ công lúc xoay ghi điều này |
| Cỡ chữ lớn ở màn ngang | Kiểm tra thủ công (máy thật: khác, thanh hệ thống và khoét camera thật của máy, cỡ chữ của hãng) | Cỡ chữ 200% ở màn ngang: thấy dòng "Not enough room for the full-page ad", không vùng trắng. Ghi giá trị `font_scale` của máy (`adb shell settings get system font_scale`) vào báo cáo |

Không có tệp kịch bản emulator: lệnh `fresh` của `device-session` mở `MainActivity`, không mở được màn xem thử (như T-041). Ngoài ra `device-session` từ chối đồng ý, nên trang không có quảng cáo để kiểm.

**Ngoại lệ viết test trước:**
- Tài liệu: `ads-guide.md`, design README.
- `flowSteps_phoneLandscape_insets_reproducePageArea` qua ngay trên code hiện tại vì nó kiểm cách tái hiện, không kiểm phần sửa; đối chứng bỏ lề làm nó đỏ.

### 4.1. Chạy thử

Không bắt buộc theo L-042 (E dưới 2: cách giả lề đã có ở `NativeFullScreenAdScreenTest`), nhưng plan sửa lỗi đo được nên đã chạy thử theo L-029.

- **Đã chạy thử:** 2026-10-28, worktree của nhánh `claude/plan-T-049-man-ngang`, từ nhánh chính `8d9bc55`. Hai test tạm không commit, đã xóa: `app/src/testStaging/.../DryRunT049Test.kt` (màn xem thử, `createAndroidComposeRule<ComponentActivity>()`), `core/ads/src/test/.../DryRunT049Test.kt` (cổng, số đo, bố cục). Phần sửa của Phần 1 (bước 1, 2) áp tạm vào `NativeFlowStepLayout.kt` cho một lần đo, đã hoàn lại.
- **Lệnh:** `./gradlew :app:testStagingDebugUnitTest --tests '*DryRunT049*'`; `./gradlew :core:ads:testDebugUnitTest --tests '*DryRunT049*' --tests '*NativeFlowStep*' --tests '*NativeEligibleReport*' --tests '*NativeFullScreen*' --tests '*NativeAdFontScaleTest*'`; `./gradlew :app:verifyRoborazziStagingDebug --continue`. JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`. Không chạy `scripts/check.sh`.
- **Kết quả, tái hiện vùng trang (L-029)**, màn xem thử, trang toàn trang sau một lần Next:

  | Khung | Vùng trang |
  | --- | --- |
  | Máy thật (cây giao diện của FB-20261028-01) | `[94,292][2400,805]` px, 186,5 dp |
  | `w873dp-h393dp-440dpi`, lề giả 94/94/44 px, `safeDrawingPadding()` | `[94,292][2400,805]` px, 186,55 dp: trùng từng điểm ảnh |
  | `w873dp-h393dp-440dpi`, không lề | 236,7 dp |
  | `w839dp-h343dp-440dpi`, không lề (khung tương đương) | 186,9 dp, `left` 0 |
  | `w891dp-h411dp-xxhdpi` (T-041) | 255 dp |
  | `w393dp-h873dp-440dpi` (màn dọc) | 716,7 dp |

  Số lề suy từ cây giao diện: trang bắt đầu ở x = 94 px (khoét camera bên trái), y = 292 px = 72 dp của tiêu đề cộng 94 px (thanh trạng thái); dưới trang còn 275 px = 84 dp của nút Next cộng 44 px (thanh điều hướng).
- **Kết quả, cổng ở 186,5 dp** (`NativeFullScreenStepGate`, `Box(height(513 px / 2,75))`, khung `w873dp-h393dp-440dpi`): `takes` 0, báo `[]`, CTA 0, khung chờ 0, chỉ có nút gốc `FULL_PAGE`. Xác nhận: trang không lấy quảng cáo, không có khung chờ, để trắng.
- **Kết quả, theo cỡ chữ ở màn ngang của máy** (cỡ chữ đặt bằng `@Config(fontScale = ...)`, lề giả):

  | Cỡ chữ | Vùng trang | Mức hôm nay | Mức đề xuất |
  | --- | --- | --- | --- |
  | 1.0 | 186,5 dp | 200 dp | 152 dp |
  | 1.15 | 184,4 dp | 201,8 dp | 152 dp |
  | 1.3 | 182,5 dp | 207,6 dp | 155,6 dp |
  | 1.5 | 180,0 dp | 214,5 dp | 162,5 dp |
  | 1.7 | 176,0 dp | (không đo) | (không đo) |
  | 1.8 | 173,8 dp | 224,4 dp | 172,4 dp |
  | 2.0 | 168,4 dp | 230,2 dp | 178,2 dp |
- **Kết quả, bố cục đề xuất áp tạm** (`NativeFullScreenStepContent`, bộ chữ `Longest`, rộng 872,7 dp):
  - 186,5 dp, cỡ 1.0: media 16 tới 170,5 dp (154,5 dp vuông); CTA 118,5 tới 170,5 dp (cách đáy 16 dp); mô tả 1 dòng 80 tới 100 dp; ô AdChoices giả x 840,7 tới 856,7 dp, y 26,2 tới 42,2 dp, bên phải đầu quảng cáo.
  - 152 dp, cỡ 1.0: media 120 dp, CTA 84 tới 136 dp, không mô tả.
  - 178,2 dp, cỡ 2.0: media 146,2 dp, CTA 110,2 tới 162,2 dp, nhãn "Ad" cao 32 dp.
  - Cổng ở 186,5 dp: `takes` 1, báo `[true]`, có CTA.
- **Kết quả, bộ test với Phần 1 áp tạm:** đỏ đúng bốn test của mục 3.5 (`fontScale_requiredPartsKeepSize` "least height at 1.0 expected 200.0 but was 152.0"; `fullScreenStep_belowMinHeight_takesNothing` "nothing taken in 190 dp"; `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`; `fullScreenStep_belowMinHeight_noEligible`) và ảnh `NativeFlowStep_full-page-compact_light`. `NativeAdFontScaleTest`, các test còn lại của `NativeFlowStep*`, `NativeFullScreen*` qua. `:app:verifyRoborazziStagingDebug`: 158 test, chỉ `flowFullPage_landscape` đổi ảnh.
- **Kết quả, giả lề với `createComposeRule()`** (người rà độc lập chạy thử, 2026-10-28): ép kiểu `AndroidComposeTestRule` lấy activity, phát lề một lần tới `decorView` bằng `ViewCompat.dispatchApplyWindowInsets`: trang 186,55 dp, đúng máy; không lề 236,7 dp; `captureRoboImage` chụp được sau khi giả lề. Trên code hiện tại, trang 186,5 dp có CTA bị ép còn khoảng 38 dp, nên câu "CTA 52 dp" đỏ đúng lý do.
- **Chưa chạy thử:** Phần 2 (dòng giải thích).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**
- quảng cáo, điểm 1: đổi cách vẽ của `native_fullscreen` ở bố cục hàng gọn và mức chiều cao trang lấy quảng cáo;
- thu thập dữ liệu, điểm 4: `af_native_ad_eligible` được ghi ở trang 152 tới 200 dp, nơi hôm nay không ghi;
- module core dùng chung `:core:ads`, điểm 7.

Không thêm vị trí, quyền, SDK hay dữ liệu. Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm cần biết trước: trang lấy quảng cáo ở vùng thấp hơn trước.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module (`:core:ads`, `:app` staging); bốn file code, một chuỗi, một hàm công khai mới; hai ảnh ghi lại, hai ảnh mới |
| B. Độ rõ của yêu cầu | 2 | Lỗi đo được và tái hiện được; cách xử lý cần người chọn (mục 8, câu 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Khung có lề giả tái hiện đúng vùng trang của máy; bố cục, cổng, eligible, màn xem thử có test; quảng cáo thật và cỡ chữ của hãng chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `NativeFlowStepLayout.kt`, `NativeFlowStep.kt` dùng chung cho mọi app; app nào đặt trang toàn trang ở vùng 152 tới 200 dp sẽ bắt đầu có quảng cáo; `ads-guide.md`, design README |
| E. Độ mới của kỹ thuật | 1 | Giả lề hệ thống theo mẫu `NativeFullScreenAdScreenTest`, lần đầu cho ba loại lề và ở `:app` |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeFlowStepLayout.kt`, `NativeFlowStep.kt` (`:core:ads`); `AdGalleryScreen.kt`, `strings.xml` (staging); `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`; các ảnh chuẩn ở mục 3.2.
- Không đổi: bố cục cột của trang, màn sau inter (`NativeFullScreenAdScreen`, `NativeFullScreenOverlay`), thẻ ở bước, vòng đời quảng cáo (`fullScreenStepStates`, `releaseStepFullScreenAd`), `AdsController.kt`.
- App dùng `NativeFullScreenStep` hôm nay: chỉ màn xem thử staging (`gallery_native_step_full`). Chưa có vị trí của app.
- Ticket đang mở của feature: T-048 (rewarded, `backlog`, chưa có plan) sửa test và KDoc của rewarded, có thể sửa dòng Rewarded của `ads-guide.md`. T-049 sửa dòng khác của `ads-guide.md`; không chung file code.
- `docs/STATUS.md`: không đưa vào PR.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: độc lập; chạy trước T-048 hay sau đều được.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn ngoài hai ảnh ghi lại của mục 3.2 đổi: dừng, không ghi lại. Nhất là `NativeFlowStep_full-page_*`, `NativeFlowStep_full-page-placeholder_light`, `AdGalleryScreen_flow-full-page_light`, `_small` (bố cục cột), `NativeFullScreenAdScreen_*` (màn sau inter), `AdGalleryScreen_default_*`, `_default-more_light` (cỡ chữ 1.0).
- `columnMinHeight` ở cỡ chữ 1.0 khác 360 dp, hay bố cục cột đổi: dừng (ngoài phạm vi).
- Phần sửa cần đổi `fullScreenStepStates`, `releaseStepFullScreenAd`, `AdsController.kt` hay cách cổng tính `fits` (ngoài số của mức): dừng và hỏi.

## 8. Câu hỏi cho người duyệt

1. **Chọn cách xử lý khi màn ngang thấp hơn mức tối thiểu.**
   - Đề xuất của AI (kết hợp): (a) bố cục hàng gọn thấp hơn, mức 152 dp ở cỡ chữ 1.0, và (d') trang vẫn quá thấp thì app tự hiện nội dung qua `NativeFullScreenStepDefaults.minHeight()`; màn xem thử hiện dòng giải thích. Lý do: không đổi luật dùng vị trí (vẫn `native_fullscreen`, một quảng cáo một trang); người dùng điện thoại thường có quảng cáo ở màn ngang từ cỡ chữ 1.0 tới khoảng 1.8; phần bắt buộc vẫn đủ (CTA 52 dp, media ít nhất 120 dp); sửa nhỏ, trong một file bố cục.
   - (b) Ở màn ngang thay trang bằng thẻ nhỏ của bước (`native_step`, 250 dp): đổi định dạng của vị trí theo hướng màn hình, trang dùng quảng cáo của pool `native_step`; đổi luật MC-06, OB-01 và cách đếm lượt của hai vị trí. Cần người duyệt luật; không đề xuất.
   - (c) Ẩn hẳn vùng trang ở màn ngang và nới nội dung của bước: luồng mất một trang khi xoay, số trang đổi trong lúc luồng chạy (trái T-036); không đề xuất.
   - (d) Giữ khung chờ thay vùng trắng: không có quảng cáo nào tới, khung chờ còn mãi, người dùng tưởng đang tải. Không đề xuất làm cách duy nhất.
   - **Trả lời (anh Nhật, 2026-10-28):** (a), bố cục hàng gọn, `minHeight()` và dòng giải thích ở màn xem thử, theo đề xuất.
2. **Ô chừa chỗ AdChoices cạnh đầu quảng cáo ở bố cục hàng gọn.** Hôm nay hàng gọn có hàng trên 36 dp chỉ để chừa chỗ cho AdChoices (anh Nhật duyệt 2026-10-26, T-041). Plan thay hàng đó bằng ô 36 dp bên phải đầu quảng cáo. AdChoices thật vẫn do SDK ghim ở góc trên phải của trang; ô và lề 16 dp giữ góc đó trống, không phần nào của quảng cáo cắt vào (O-08, có test). Bố cục cột giữ hàng trên 36 dp.
   - Đề xuất của AI: đồng ý đổi; ảnh `NativeFlowStep_full-page-compact_light` ghi lại.
   - Phương án khác: giữ hàng trên, chỉ giảm lề hàng gọn từ 16 xuống 8 dp (mức 184 dp ở 1.0; vẫn dưới 186,5 dp nhưng chỉ dư 2,5 dp, và thua ở cỡ chữ 1.15 trở lên).
   - **Trả lời (anh Nhật, 2026-10-28):** đồng ý đổi bố cục đã duyệt ở T-041: ô chừa chỗ AdChoices cạnh đầu quảng cáo.
3. **Cỡ chữ 2.0 ở màn ngang của điện thoại vẫn không có quảng cáo** (vùng 168,4 dp, mức 178,2 dp).
   - Đề xuất của AI: chấp nhận, màn xem thử ghi dòng giải thích; `ads-guide.md` khuyên app đọc `minHeight()` để hiện nội dung của mình.
   - Phương án khác: thu tiếp hàng gọn ở cỡ chữ lớn (lề 12 dp và khoảng cách đầu tới CTA 8 dp: mức 162 dp ở 2.0). Lệch thêm khỏi số của thiết kế; không đề xuất trong task này.
   - **Trả lời (anh Nhật, 2026-10-28):** chấp nhận cỡ chữ 2.0 ở màn ngang không có quảng cáo, có dòng giải thích.
4. **Ghi `af_native_ad_eligible` ở trang 152 tới 200 dp.** Mức thấp hơn nên trang lấy quảng cáo ở vùng thấp hơn và ghi eligible ở đó (cùng điều kiện `allowed && shown && fits` của T-041).
   - Đề xuất của AI: đồng ý, giữ `analytics` trong `blocking_points`.
   - **Trả lời (anh Nhật, 2026-10-28):** đồng ý ghi `af_native_ad_eligible` ở trang 152 tới 200 dp.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-28 trong phiên giám sát cùng AI, gồm việc đổi bố cục hàng gọn đã duyệt ở T-041 (ô chừa chỗ AdChoices cạnh đầu quảng cáo). Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua hai vòng rà soát độc lập, sửa theo điểm chặn và điểm nên sửa.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261028-02, phiên bản plan 1

- **Kết quả:** đã merge (2026-10-28 08:49 UTC, commit `b7aee1d` trên nhánh chính).
- **Nhánh, PR:** `claude/T-049-trang-toan-trang-man-ngang`, #244. Bước 3 tới 5 do phiên con làm trong worktree `../task-T-049`.
- **Đã làm:** mục 3.1 bước 1 tới 7: hàng gọn có ô chừa chỗ AdChoices 36 dp bên phải đầu quảng cáo, `topBar` nhận modifier, `rowTextColumnMin = max(36, header) + 16 + 52`; tag `PLACEHOLDER_HEADER`, `PLACEHOLDER_CTA`; `MinHeight` 152 dp, hàm `minHeight()`; màn xem thử hiện dòng "Not enough room for the full-page ad" khi vùng thấp hơn `minHeight()`; `PhoneInsets.kt`; `ads-guide.md`, design README. Bố cục cột, cổng, `fullScreenStepStates`, `releaseStepFullScreenAd`, `AdsController` không đổi.
- **Test viết trước:** commit `5dbfda4` (gồm khung rỗng của mục 3.2, L-025) trước commit code `b556246`. 13 test mới hay viết lại (9 test mới của mục 4, 4 câu viết sẵn của mục 3.5), thêm 186,5 và 152 dp vào `HEIGHTS`. Trên khung rỗng 12 test đỏ đúng lý do (ví dụ "CTA height expected 52.0 but was 38.545456", "least height at 1.0 expected 152.0 but was 200.0", "one ad taken in 186.5 dp: [] expected 1 but was 0", không có dòng giải thích). Qua ngay: `flowSteps_phoneLandscape_insets_reproducePageArea` (bước tái hiện, plan cho phép; 34,18 / 106,18 / 292,73 dp, cao 186,5 dp; đối chứng không lề 236,7 dp, `left` 0) và `media_neverBelow120dp` ở hai chiều cao mới (media đã từ 120 dp trên code cũ; mục 4 ghi đỏ là quá, phần đỏ trước ở hai chiều cao này do `requiredParts_insidePage_atEveryHeight` mang). 3 test có bước đối chứng (L-055): `fullScreenStep_phoneLandscape_takesAd` (151 dp trước), `compactRow_phoneLandscape_requiredPartsInside` (152 dp và cột 360 dp), `defaults_minHeight_followsFontScale` (hằng `MinHeight`). Test JVM mọi module trên khung rỗng: chỉ 12 test trên đỏ, đều trong file của mục 3.2. Không đổi file test sau commit test.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, ghi chú. Nên sửa: một dòng KDoc của `NativeFullScreenStep` (`NativeFlowStep.kt:258`) dài 163 ký tự, quá `max_line_length = 120` (ktlint không báo, `check.sh` qua). Không sửa: chú thích không có test thất bại trước được (auto-run.md bước 6, ADR-0029 quyết định 3), để người quyết. Ghi chú: dòng 79 của `ads-guide.md` chỉ nói ô AdChoices, phần 152 dp và `minHeight()` nằm ở đoạn dòng 120; screenshot test đặt `safeDrawingPadding()` không có `Surface` bọc ngoài như test giao diện (ảnh vẫn đúng); test cỡ chữ 2.0 dùng `flowSteps_phoneLandscape_pageFitsAd` làm đối chứng cỡ 1.0.
- **Kết quả CI:** `scripts/check.sh` trong worktree QUA (1455 test JVM, 0 lỗi, 89 ảnh); `pr-check` xanh một vòng (lần chạy 40658111639).
- **Lệch so với plan:** cách dựng test, không đổi điều test kiểm (rà soát xác nhận): (1) đối chứng eligible ở 186,5 dp là test riêng `fullScreenStep_phoneLandscape_eligible` (khung máy), vì sau khi 152 dp đã báo eligible, đổi sang 186,5 dp không báo lại; (2) `fullScreenStep_phoneLandscape_takesAd` chạy đối chứng 151 dp trước rồi đổi sang 186,5 dp (không gọi `setContent` hai lần được); (3) test cỡ chữ 2.0 không có đối chứng cỡ 1.0 trong cùng test (cỡ chữ đặt theo test bằng `@Config`), `flowSteps_phoneLandscape_pageFitsAd` là đối chứng đó; (4) `PhoneInsets.kt` giữ thêm hằng khung `PHONE_LANDSCAPE`; (5) lần `recordRoborazzi*` đầu không ghi vì task test đã cập nhật, phiên con chạy lại kèm `--rerun`. Bốn câu khẳng định viết lại là câu viết sẵn trong plan, quyết định 1 của ADR-0031 (rà soát xác nhận khớp chữ, không nới). Không có phần ngoài mục 3.2.
- **Ảnh chuẩn:** chỉ 4 ảnh của mục 3.2 đổi (2 ghi lại, 2 mới), không phải `git checkout` ảnh nào.
- **Việc còn lại:** dòng KDoc dài (nên sửa của rà soát).
- **Cần người xem:** 4 ảnh chuẩn; kiểm tra thủ công trên máy thật (mục 4, hai dòng); dòng KDoc dài.
