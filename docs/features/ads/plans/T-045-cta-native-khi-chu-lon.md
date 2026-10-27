---
ticket: T-045
title: Native giữ trọn CTA, nhãn "Ad" và media 120 dp khi cỡ chữ hệ thống lớn
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 22M          # cỡ M, số gợi ý 19.6M (scripts/token-usage.py estimate, 2026-10-26); cộng ma trận cỡ chữ cho chín dạng native, chiều cao thẻ ở bước dùng chung cho thẻ, khung chờ và màn xem thử, sửa hai test của T-044
blocking_points: [ads, core]
run_order: 2                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Nhật              # bắt buộc khi risk_level là high
approved_at: 2026-10-26
---

# PLAN T-045: NATIVE GIỮ TRỌN CTA, NHÃN "AD" VÀ MEDIA 120 DP KHI CỠ CHỮ HỆ THỐNG LỚN

## 1. Tóm tắt

Ở cỡ chữ hệ thống 2.0, chữ `labelLarge` của CTA cần một dòng cao 37,3 dp (small: 38,7 dp), cao hơn khung 34 hay 36 dp, nên bị cắt ở small, thẻ dọc, thẻ ở bước, lớp thu gọn và thẻ đầy đủ. Medium bị ép: CTA còn 32 dp ở 1.3 và 10,7 dp ở 2.0. Nhãn "Ad" của native toàn màn hình sau inter còn 2 dp ở 2.0 (O-08); media của thẻ ở bước và lớp thu gọn còn 101 và 99 dp (dưới 120 dp). Plan: khung CTA cao **ít nhất** số thiết kế, cao thêm vừa chữ; medium, thẻ ở bước và lớp thu gọn đo theo "media co trước, không dưới 120 dp" (luật và cách đo của T-044), thẻ cao thêm khi media chạm sàn; đầu quảng cáo sau inter cao theo cỡ chữ như T-041 làm cho trang toàn trang. Chạy thử trên nhánh chính hiện tại (trước T-044, T-041) với phần sửa áp tạm: ở 1.0 mọi kích thước giữ số thiết kế, ở 2.0 chữ vừa khung ở mọi dạng, nhãn "Ad" đủ 32 dp, media ít nhất 120 dp, cả bộ test JVM qua và không ảnh chuẩn nào đổi. Sau T-044 và T-041 đây là kết quả dự kiến, phiên kiểm lại (mục 4.1, mục 7).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-26), bảng kích thước mục 5.7 và luật O-08 của `docs/ops/ads-business-rules.md`. Lỗi gốc: plan T-044, mục 4.1 ghi chú cuối và mục 8 câu 2; phần nhãn "Ad" và media do chạy thử của plan này tìm ra (mục 4.1), anh Nhật gộp vào ticket (mục 8, câu 2, 3).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices… | "Thấy nút CTA" gồm thấy trọn chữ của nó. Ở mọi cỡ chữ máy cho đặt (1.0 tới 2.0), chữ CTA của small không bị cắt: `TextLayoutResult.didOverflowHeight` là `false`, chữ nằm trong khung CTA. Khung small vẫn 80 dp. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp… Các tiêu chí giữ chỗ… như KB-1. | Medium: CTA đủ 44 dp (56 dp với `ctaLarge`), hàng sao 16 dp, mô tả 2 dòng tới cỡ 2.0; media co trước, không dưới 120 dp (`NativeAdDimens.MinMediaSize`, do T-044 tạo); thẻ vẫn 410 dp (422 dp) tới 2.0 nên chỗ giữ và khung chờ vẫn bằng thẻ. Sàn 120 dp áp cho mọi native có media trong task: medium, thẻ ở bước, lớp thu gọn (thẻ dọc đã trên 159 dp; `FULL` do T-044). |
| KB-4 | Khi vào màn hình thì quảng cáo mở ở đáy, cao 250 dp… | Lớp thu gọn 250 dp ở cỡ chữ tới 1.3 (media 122 dp). Từ khoảng 1.4, media chạm 120 dp thì lớp cao thêm: 255 dp ở 1.5, 264,7 dp ở 1.8, 272 dp ở 2.0 (anh Nhật duyệt, mục 8 câu 3). Chữ CTA không bị cắt. Lớp nổi trên khe đáy, không chừa chỗ (D-04), nên lớp cao thêm không đẩy nội dung, chỉ che thêm 22 dp ở 2.0. |
| KB-16 | Interstitial đóng xong có ngay một native toàn màn hình… | Nhãn "Ad" (O-08) giữ trọn chiều cao chữ ở mọi cỡ: đầu `heightIn(min = 48.dp)` như T-041; media lấp phần còn lại (493 dp ở 2.0 trên khung `w411dp-h891dp`). CTA 52 dp đã đủ chỗ (chữ 37,3 dp ở 2.0). Đếm ngược, nút đóng không đổi. |
| KB-20 | Bước mà spec của app chọn có native cao 250 dp hoặc native toàn trang. … nút Tiếp của app vẫn ở chỗ quen thuộc… | Thẻ ở bước 250 dp tới cỡ 1.3; lớn hơn thì cao thêm khi media chạm 120 dp (253 dp ở 1.5, 262,7 ở 1.8, 270 ở 2.0). Khung chờ và khung của app đặt cùng chiều cao qua `NativeStepAdDefaults.height()`, nên nút Tiếp vẫn đứng một chỗ trên mọi bước (OB-01) ở mỗi cỡ chữ. Trang toàn trang (CTA 52 dp, cả bố cục cột và bố cục hàng gọn của T-041): test canh. |
| KB-21 | Mỗi ô… dạng thẻ dọc hoặc medium… | Thẻ dọc (`CARD`, 180 x 300 dp) giữ kích thước ô, chữ CTA không bị cắt (media 159 dp ở 2.0); medium trong ô theo dòng KB-2. |

**Đối chiếu thiết kế và luật.**

- Mục 5.7 và README thiết kế (mục 3) ghi CTA 34, 36, 44, 56, 52 dp; thẻ ở bước và lớp thu gọn 250 dp; đầu toàn màn hình 48 dp. Đó là số ở cỡ chữ 1.0. Plan giữ đúng các số này ở 1.0 và 1.3, chỉ cho lớn thêm khi chữ cần. Spec KB-4, KB-20 ghi "cao 250 dp": plan hiểu là ở cỡ chữ mặc định (mục 8, câu 5).
- O-08 (nhãn "Ad" luôn thấy được): sau phần sửa nhãn đủ cao ở mọi dạng (16, 21, 32 dp ở 1.0, 1.3, 2.0).
- Vùng chạm 44/48 dp: quy tắc 48 dp (`docs/ops/design-workflow.md` mục 8.5) áp cho nút của app. CTA của native là tài sản của quảng cáo, SDK nhận chạm qua view của nó, cỡ theo bảng 5.7 đã duyệt. Task không làm khung nào nhỏ đi (mục 8, câu 4: giữ theo thiết kế).

Thiết kế tham chiếu: `features/ads/design/README.md` (mục 3: dòng CTA, dòng Media có luật "media co trước, không dưới 120 dp" do T-044 ghi, dòng "Khung native"), `design/context/AdGalleryScreen_default_light.md` (medium), `design/context/AdGalleryScreen_collapsible-expanded_light.md` (lớp 250 dp, media 134 dp), `design/context/NativeFullScreenAdScreen_default_light.md` (đầu 48 dp). Thiết kế chỉ vẽ cỡ chữ 1.0; bố cục khi chữ lớn không có trên Figma. Task không vẽ màn hình mới.

ADR liên quan: ADR-0008, ADR-0013, ADR-0026, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

Plan dựa trên code sau khi T-044 và T-041 đã merge. "Do T-044 tạo": `FontScaleTestSupport.kt` (`WithFontScale`), `NativeAdDimens.MinMediaSize` (120 dp), hàm thuần `fullMediaHeight(inner, others, floor)`, `FullLayout` bằng `Layout` riêng, các test `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales`, `full_mediaFloor120dp_cardGrows_atFontScale3`, `FullMediaHeightTest`. "Do T-041 tạo": `FlowStepPageLayout` (trang toàn trang qua `NativeFullScreenStepContent`), đầu quảng cáo dùng chung nhận chiều cao tối thiểu trong `NativeFullScreenAdScreen.kt`, `FlowStepsPage` mới của màn xem thử (khung thẻ đặt đè ở đáy vùng bước, trang thường đệm đáy 250 dp).

**A. Khung CTA cao vừa chữ.**

1. `NativeAdCallToAction` (`NativeAdCard.kt`): khung trong `.height(height)` thành `.heightIn(min = height)`. Chữ vẫn một dòng, cắt "…" theo chiều ngang. KDoc: "cao ít nhất [height]; cỡ chữ lớn thì cao vừa một dòng chữ (plan T-045)".
2. Nơi bọc CTA trong chiều cao cố định đổi sang `heightIn(min = …)`: `ctaModifier` của `CardLayout`, `StepLayout`, `FullLayout` (do T-044 tạo). `SmallLayout`, `MediumLayout`, `CollapsibleNativeAdLayout` truyền `height` vào `NativeAdCallToAction`, không bọc thêm.
3. Khối chờ không có chữ: khối CTA của khung chờ giữ số thiết kế.

**B. Hàm đo chung "media co trước".**

1. Tách phần đo và xếp của `FullLayout` (do T-044 tạo) thành `@Composable internal fun MediaShrinksFirstLayout(innerHeight: Dp, gap: Dp, mediaIndex: Int, modifier: Modifier, content: @Composable () -> Unit)` trong `NativeAdCard.kt`: đo mọi phần trừ media với chiều cao không giới hạn (rộng cố định), media = `fullMediaHeight(innerHeight, others, NativeAdDimens.MinMediaSize)` (others = tổng các phần kia cộng các khoảng `gap`), đo media với chiều cao cố định đó, xếp từ trên xuống; chiều cao của `Layout` = `others + media`. Mỗi phần là một nút con (bọc `Box` khi cần). `FullLayout` gọi hàm này và giữ đúng hành vi của T-044. `fullMediaHeight` giữ tên, KDoc thêm "dùng cho mọi native có media co trước".
2. **Medium** (`MediumLayout`): bốn phần đầu (`NativeAdHeader(starRow = true)`), media (nền mờ), mô tả (`heightIn(min = BodyHeight)`, `maxLines = 2`), CTA; `mediaIndex = 1`, `gap = PartGap` (12 dp), `innerHeight = heightOf(MEDIUM, ctaLarge) - MediumInnerTop - MediumInnerBottom` (372 dp, 384 dp với `ctaLarge`). Khung thẻ của `MEDIUM` trong `NativeAdLayout`: `heightIn(min = heightOf(size, ctaLarge))`.
3. **Thẻ ở bước** (`StepLayout`): media, hàng đầu (icon 32 dp, tiêu đề 13 sp, dòng nhãn "Ad"), CTA; `mediaIndex = 0`, `gap = Spacing.sm` (8 dp), `innerHeight = StepHeight - 2 x InnerPadding` (226 dp). Khung thẻ của `STEP`: `height(NativeStepAdDefaults.height())` (bước C).
4. **Lớp thu gọn** (`CollapsibleNativeAdLayout`, `CollapsibleNativeAd.kt`): media (`Box` mang tag `CollapsibleNativeAdTestTags.MEDIA`), `LayerHeader`, CTA; `mediaIndex = 0`, `gap = LayerGap` (8 dp), `innerHeight = LayerHeight - 2 x LayerInnerVertical` (224 dp). Khung lớp `height(LayerHeight)` thành `heightIn(min = LayerHeight)`. Nút thu gọn, chỗ AdChoices giữ ở góc như cũ.

**C. Chiều cao thẻ ở bước dùng chung cho thẻ, khung chờ và khung của app (OB-01).**

1. API công khai mới trong `NativeFlowStep.kt`: `object NativeStepAdDefaults { @Composable fun height(): Dp }`. Hàm đo một dòng của mỗi kiểu chữ trong thẻ bằng `rememberTextMeasurer()` với `LocalDensity` hiện tại (cùng bộ đo chữ mà `Text` dùng, nên tính cả bảng đổi phi tuyến của cỡ chữ): tiêu đề (`titleSmall`, 13 sp), dòng nhãn "Ad" (cao hơn trong `labelSmall` của nhãn và `bodySmall` của nhà quảng cáo), CTA (`labelLarge`). Kết quả = `max(250 dp, 12 + 120 + 8 + max(32 dp, tiêu đề + 2 dp + dòng nhãn) + 8 + max(36 dp, dòng CTA) + 12)`. Ở cỡ 1.0 và 1.3 bằng 250 dp. KDoc ghi: app đặt khung của thẻ (khung cố định để nút Tiếp không nhảy) bằng hàm này, không viết cứng 250 dp.
2. Thẻ (`NativeAdLayout`, cỡ `STEP`), khung chờ của thẻ (`NativeAdPlaceholder(STEP)` và `Spacer` giữ chỗ của `NativeAdSlot`) đều dùng `NativeStepAdDefaults.height()`, nên khung chờ bằng thẻ ở mọi cỡ chữ (KB-1, giữ chỗ). `NativeAdDimens.heightOf(STEP)` vẫn trả 250 dp (số thiết kế) cho nơi không phải composable; KDoc ghi "ở cỡ chữ mặc định".
3. Màn xem thử (`AdGalleryScreen.kt`, `FlowStepsPage` theo bản của T-041): khung thẻ `FLOW_CARD_FRAME` và phần đệm đáy 250 dp của trang thường đổi sang `NativeStepAdDefaults.height()`; hằng `StepCardFrame` bỏ. Ở 1.0 không đổi gì.
4. **Ảnh hưởng tới nơi đặt thẻ, lớp:**
   - Thẻ ở bước: app nào đặt thẻ trong khung cố định 250 dp thì ở cỡ chữ trên 1.3 khung ép thẻ lại (media lại dưới 120 dp). `ads-guide.md` ghi cách đặt khung bằng `NativeStepAdDefaults.height()`. Trong dự án mẫu chỉ màn xem thử đặt thẻ (bước 3).
   - Lớp thu gọn: nổi trên khe đáy (`BottomAdBarLayout`, `layer(Modifier.fillMaxWidth())`), không ai đặt chiều cao, không chừa chỗ (D-04), nên lớp cao thêm không đổi bố cục của màn; nút thu gọn vẫn ở góc trên phải của lớp.

**D. Nhãn "Ad" của native toàn màn hình sau inter (O-08, KB-16).**

1. `NativeFullScreenAdScreen.kt`: đầu quảng cáo dùng chung (bản T-041 nhận chiều cao tối thiểu) dùng `heightIn(min = HeaderHeight)` cho cả nhánh sau inter, như trang toàn trang. Media (`weight(1f)`, `heightIn(min = 120.dp)` sẵn có) nhường chỗ: 595 dp ở 1.0 (không đổi), 493 dp ở 2.0 trên `w411dp-h891dp`.
2. Khung chờ của màn sau inter (`NativeFullScreenOverlay.kt`, đầu chờ 48 dp, không có chữ) không đổi.

**E. Tài liệu.**

- `docs/features/ads/design/README.md`, mục 3: dòng "CTA quảng cáo" thêm "số trên là ở cỡ chữ 1.0; chữ lớn thì CTA cao vừa một dòng chữ"; dòng "Icon quảng cáo" hay dòng tương ứng của đầu toàn màn hình: "đầu 48 dp ở cỡ 1.0, cao theo chữ khi chữ lớn"; dòng "Khung native…": "thẻ ở bước và lớp thu gọn 250 dp ở cỡ 1.0, cao thêm khi media chạm 120 dp". Dòng Media (của T-044) không đổi.
- `docs/ops/ads-guide.md`, mục 4: bảng số đo (dòng CTA "ít nhất", ô Khung của medium); đoạn "Thẻ ở bước của luồng" (250 dp ở cỡ chữ mặc định, khung của app đặt bằng `NativeStepAdDefaults.height()`); đoạn thanh đáy (lớp cao thêm khi chữ lớn).
- `docs/ops/ads-business-rules.md` không đổi.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `NativeAdCallToAction` `heightIn`; `ctaModifier` của `CardLayout`, `StepLayout`, `FullLayout` dùng `heightIn`; `MediaShrinksFirstLayout` (tách từ `FullLayout`); `MediumLayout`, `StepLayout` dùng nó; khung `MEDIUM` `heightIn`, khung và khung chờ `STEP` theo `NativeStepAdDefaults.height()`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | `CollapsibleNativeAdLayout` dùng `MediaShrinksFirstLayout`; khung lớp `heightIn(min = LayerHeight)`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `NativeStepAdDefaults.height()` công khai; KDoc của `NativeStepAd` (250 dp ở cỡ mặc định) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | `Spacer` giữ chỗ của cỡ `STEP` theo `NativeStepAdDefaults.height()` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | Đầu quảng cáo `heightIn(min = HeaderHeight)` cả ở nhánh sau inter; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BottomAdBar.kt` | Chỉ KDoc: lớp 250 dp ở cỡ chữ mặc định, cao thêm khi chữ lớn |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | Ma trận cỡ chữ: CTA của chín dạng, medium, thẻ ở bước, lớp thu gọn, nhãn "Ad" sau inter (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Hai test do T-044 tạo theo luật CTA mới: `full_partsKeepSize_insideCard_textsAndFontScales`, `full_mediaFloor120dp_cardGrows_atFontScale3` (mục 4, dòng 9, 10) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Ảnh mới `fontScale2_light` (small, medium, thẻ ở bước ở cỡ 2.0) |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_light.png` | Ảnh chuẩn mới, khung `w411dp-h891dp-xxhdpi` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenScreenshotTest.kt` | Ảnh mới `fontScale2_light` của màn sau inter ở cỡ 2.0 |
| Tạo | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_font-scale-2_light.png` | Ảnh chuẩn mới, khung `w411dp-h891dp-xxhdpi` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `FlowStepsPage`: khung thẻ và đệm đáy theo `NativeStepAdDefaults.height()`, bỏ `StepCardFrame`; KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Test mới `flowSteps_stepCardFrame_followsCardHeight_atFontScale2` |
| Sửa | `docs/features/ads/design/README.md` | Mục 3: dòng CTA, đầu toàn màn hình, khung thẻ ở bước và lớp thu gọn khi chữ lớn |
| Sửa | `docs/ops/ads-guide.md` | Mục 4: bảng số đo, đoạn thẻ ở bước, đoạn thanh đáy |

**Dùng, do T-044 tạo (không tạo lại):** `FontScaleTestSupport.kt` (`WithFontScale`), `NativeAdDimens.MinMediaSize`, `fullMediaHeight`, `Layout` của `FullLayout`, `FullMediaHeightTest` (không sửa). **Dùng, do T-041 tạo:** `FlowStepPageLayout`, `NativeFlowStepLayout.kt` (không sửa), đầu dùng chung có chiều cao tối thiểu, `FlowStepsPage` mới.

Ảnh chuẩn: không ghi lại ảnh nào ở cỡ chữ 1.0. Hai ảnh mới ở cỡ 2.0. Ảnh mới là ảnh thành phần, như các ảnh `NativeAdCard_*`, `NativeFullScreenAdScreen_*` sẵn có; không cần ảnh `_small` (L-028 áp cho màn hình; màn sau inter đã có `NativeFullScreenAdScreen_max-content_small`).

**Khung rỗng vào commit test của bước 3 (L-025):** `object NativeStepAdDefaults { @Composable fun height(): Dp = 250.dp }` (khung rỗng trả số của cỡ 1.0, để test biên dịch và đỏ đúng lý do ở cỡ 2.0). Mọi API khác test dùng đã có. Hai câu sửa trong test của T-044 vào commit test.

**Nơi đọc file bị đổi (L-020).**

- `NativeAdCallToAction`: `SmallLayout`, `MediumLayout`, `CardLayout`, `StepLayout`, `FullLayout`, `CollapsibleNativeAdLayout`; tất cả trong ma trận test.
- `NativeAdLayout` cỡ `MEDIUM`, `STEP`: `NativeAdSlot`, `NativeAdCell`, `NativeStepAd`, `BoundNativeAd`; khung chờ qua `NativeAdPlaceholder`. Tới cỡ 2.0 medium không đổi chiều cao; thẻ ở bước theo `NativeStepAdDefaults.height()` ở cả thẻ, khung chờ và chỗ giữ.
- `CollapsibleNativeAdLayout`: `CollapsibleNativeAdCard`, lớp của `BottomAdBar` (không đặt chiều cao).
- Đầu toàn màn hình: `NativeFullScreenAdScreen`, `NativeFullScreenOverlay` (màn sau inter), `NativeFullScreenStepContent` (trang toàn trang, đã `heightIn` từ T-041).
- Ảnh chuẩn: `scripts/check.sh` (`verifyRoborazzi*`, đòi ảnh của mỗi `captureRoboImage`), `scripts/test-summary.sh` (đếm). `scripts/design-compare.sh` chỉ đọc `feature/<tên>`. Không test hay workflow nào đọc tên ảnh mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay tần suất. Đổi cách vẽ native small, medium, đầy đủ, thẻ dọc, thẻ ở bước, lớp thu gọn và toàn màn hình sau inter khi cỡ chữ hệ thống lớn (điểm chặn 1); ở cỡ 1.0 không đổi. API công khai mới `NativeStepAdDefaults.height()`.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp dùng trong code: `heightIn` (Compose foundation), `Layout`, `Constraints`, `Placeable` (Compose UI), `rememberTextMeasurer`, `TextMeasurer` (`androidx.compose.ui.text`, Compose UI), `LocalDensity`; đều trên classpath biên dịch `main` của `:core:ads` (chạy thử ở mục 4.1 biên dịch `Layout`, `heightIn`; `rememberTextMeasurer` nằm trong `ui-text`, phụ thuộc của `ui` đã có).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:**
  1. tách `MediaShrinksFirstLayout` từ `FullLayout` (code của T-044 chưa có trên nhánh chính) có thể đổi hành vi của thẻ `FULL`;
  2. khung CTA `heightIn` có thể làm lệch một ảnh chuẩn ở cỡ 1.0 trên máy CI dù máy dev không lệch;
  3. `NativeStepAdDefaults.height()` tính bằng bộ đo chữ có thể lệch chiều cao thật của thẻ quá 0,5 dp.
- **Điều kiện chuyển:**
  1. test của T-044 (`full_*`, `FullMediaHeightTest`) đỏ sau khi tách, hai lần sửa không xanh;
  2. `verifyRoborazzi*` báo lệch một ảnh có sẵn ở cỡ 1.0;
  3. `step_mediaFloor120dp_cardGrows_fontScales` đỏ ở câu "thẻ bằng `NativeStepAdDefaults.height()`" sau hai lần sửa.
- **Cách làm thay:**
  1. không tách: mỗi bố cục (`MediumLayout`, `StepLayout`, `CollapsibleNativeAdLayout`) có `Layout` riêng cùng cách đo (bản riêng cho medium đã chạy thử);
  2. giữ khung CTA cố định `height(height)` và chỉ khi dòng chữ cao hơn khung (`labelLarge.lineHeight` đổi sang dp theo `LocalDensity` lớn hơn `height`) thì vẽ chữ với `lineHeight = TextUnit.Unspecified` (một dòng cao bằng cỡ chữ của font, khoảng 31,5 dp ở 2.0), nên ở cỡ 1.0 chữ vẽ y như hôm nay. Khi đó khung CTA giữ đúng số thiết kế ở mọi cỡ, nên hai test của T-044 **giữ nguyên, không sửa** (bỏ hai dòng sửa ở mục 4); test mới của mục 4 giữ nguyên (chữ không tràn tới 2.0, khung không thấp hơn số thiết kế). Thử phương án này một lần; ảnh vẫn lệch thì dừng (mục 7);
  3. `NativeStepAdDefaults.height()` đo bằng chính bố cục: `SubcomposeLayout` dựng thẻ với chữ mẫu một dòng, ẩn, lấy chiều cao; khung chờ và khung của app vẫn gọi cùng hàm.
- Phương án dự phòng không hạ tiêu chí hay đổi test viết trước (trừ hai dòng sửa test T-044, không cần nữa ở cách 2).

## 4. Kiểm thử

Test viết trước code, đỏ đúng lý do trên code trước phần sửa (số đo ở mục 4.1, đo trên nhánh chính trước T-044, T-041; phiên đo lại sau khi gộp nhánh chính). Khung `w411dp-h891dp-xxhdpi`, thẻ rộng 379 dp như `NativeAdCardTest`, thẻ dọc 180 x 300 dp, cỡ chữ bằng `WithFontScale(scale) { AppTheme { … } }` (do T-044 tạo), đổi `scale` và bộ chữ qua `mutableStateOf` trong một lần `setContent`.

**`NativeAdFontScaleTest`** (`:core:ads`, Robolectric, `@GraphicsMode(NATIVE)`). Hàm phụ `assertCtaFits(label, ctaTag, frameTag, designHeight, frameHeight)`: đọc `getUnclippedBoundsInRoot()` (`useUnmergedTree = true`) và `TextLayoutResult` của chữ CTA. Khẳng định, dung sai 0,5 dp, thông báo ghi dạng, cỡ chữ, bộ chữ: (a) `didOverflowHeight == false`; (b) chữ nằm trong khung CTA; (c) khung CTA cao ít nhất `designHeight`, ở 1.0 và 1.3 đúng `designHeight`; (d) khung thẻ cao đúng `frameHeight`, khung CTA nằm trong thẻ. Bộ chữ: `sampleNativeAdAssets`, `longFullNativeAdAssets` cho thẻ; `FakeNativeAdAssets.Design`, `Longest` cho toàn màn hình.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, KB-2, KB-21: chữ CTA trọn trong khung ở các thẻ | Compose UI test | `cardSizes_ctaTextFits_fontScales`: `SMALL` (34 dp, thẻ 80), `MEDIUM` (44, thẻ 410), `MEDIUM` `ctaLarge` (56, 422), `FULL` (36, 418), `CARD` trong `Modifier.size(180.dp, 300.dp)` (36, 300); cỡ 1.0, 1.3, 2.0; hai bộ chữ. **Lý do thất bại (mục 4.1):** ở 2.0 chữ tràn ở `SMALL` (38,7 trong 34), `CARD`, `FULL` (37,3 trong 36); `MEDIUM` bị ép 32 dp ở 1.3 và 10,7 dp ở 2.0; `ctaLarge` 22,7 dp ở 2.0. **Đối chứng (L-055):** 1.0 qua trên code hiện tại; sau phần sửa, tạm đổi lại `heightIn(min = height)` của `NativeAdCallToAction` thành `height(height)`: đỏ ở 2.0; hoàn lại |
| KB-2: medium giữ CTA, hàng sao, mô tả; media co trước | Compose UI test | `medium_partsKeepSize_mediaShrinksFirst_fontScales`: `MEDIUM` và `ctaLarge`, hai bộ chữ (`starRating = 4.0`), 1.0, 1.3, 2.0. Thẻ 410 (422) dp; CTA 44 (56) dp; `STAR_ROW` 16 dp; đáy CTA cách đáy thẻ 13 dp; mô tả đúng 2 dòng; media ít nhất 120 dp, đỉnh media không trên đáy hàng sao; ở 1.0 media đúng 196 dp. **Lý do thất bại:** 1.3 CTA 32 dp, mô tả 1 dòng; 2.0 CTA 10,7 dp. **Đối chứng:** 1.0 qua; sau phần sửa, tạm đo media bằng `Constraints.fixed(width, NativeAdDimens.MediaHeight)`: đỏ ở 1.3 (thẻ cao hơn 410); hoàn lại |
| KB-2: sàn 120 dp của medium giữ bằng code | Compose UI test | `medium_mediaFloor120dp_cardGrows_atFontScale3`: `WithFontScale(3f)`, chữ dài. Media đúng 120 dp; CTA cao ít nhất 44 dp; thẻ cao hơn 410 dp (chạy thử: 502). Không khẳng định chữ CTA ở 3.0 (cỡ máy không cho đặt). **Lý do thất bại:** thẻ ép 410, CTA bị ép. **Đối chứng:** tạm bỏ sàn (media = `inner - others`): đỏ; hoàn lại |
| KB-20: thẻ ở bước giữ media 120 dp, CTA trọn, cao thêm khi cần; khung chờ bằng thẻ | Compose UI test | `step_mediaFloor120dp_cardGrows_fontScales`: `STEP` 379 dp, hai bộ chữ, cỡ 1.0, 1.3, 1.5, 2.0. Media ít nhất 120 dp; `assertCtaFits` (36 dp); thẻ cao bằng `NativeStepAdDefaults.height()` đọc trong cùng composition; khung chờ `NativeAdPlaceholder(STEP, animated = false)` cùng cỡ chữ cao bằng thẻ; số tuyệt đối: 250 dp ở 1.0 và 1.3, 270 dp ở 2.0; nhãn "Ad" (`BADGE`) cao 16, 21, 32 dp. **Lý do thất bại:** media 101,3 dp ở 2.0, chữ CTA tràn; khung rỗng của `height()` trả 250. **Đối chứng:** 1.0 qua; sau phần sửa, tạm cho khung chờ dùng `heightOf(STEP)`: đỏ ở 2.0 (250 khác 270); hoàn lại |
| KB-4: lớp thu gọn giữ media 120 dp, CTA trọn | Compose UI test | `collapsibleLayer_mediaFloor120dp_layerGrows_fontScales`: `CollapsibleNativeAdCard(assets, onCollapse = {}, Modifier.width(379.dp))`, cỡ 1.0, 1.3, 1.5, 2.0. Media (`CollapsibleNativeAdTestTags.MEDIA`) ít nhất 120 dp; `assertCtaFits` (36 dp); lớp 250 dp ở 1.0 và 1.3, 272 dp ở 2.0; nút thu gọn (`COLLAPSE`) vẫn cách góc trên phải 4 dp. **Lý do thất bại:** media 99,3 dp ở 2.0, chữ CTA tràn. **Đối chứng:** 1.0 qua; sau phần sửa, tạm bỏ sàn: đỏ; hoàn lại |
| KB-16, O-08: nhãn "Ad" sau inter giữ trọn chữ | Compose UI test | `fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales`: `NativeFullScreenAdScreen(assets, onClose = {}, media = { FakeMedia() }, adChoices = { FakeAdChoices() })`, cỡ 1.0, 1.3, 2.0, hai bộ chữ. `BADGE` cao 16, 21, 32 dp (bằng dòng `labelSmall` đo bằng `TextLayoutResult`, chữ "Ad" không tràn); tiêu đề không tràn; media ít nhất 120 dp; nút đóng vẫn 36 dp tại (16, 24); ở 1.0 đầu 48 dp, media 595 dp (không đổi). **Lý do thất bại:** nhãn 13,7 dp ở 1.3, 2 dp ở 2.0. **Đối chứng:** 1.0 qua; sau phần sửa, tạm trả `height(HeaderHeight)` cho nhánh sau inter: đỏ ở 1.3; hoàn lại |
| KB-20, KB-16: CTA 52 dp của toàn màn hình không bị cắt (test canh) | Compose UI test | `fullScreen_ctaTextFits_fontScales`: màn sau inter, và `NativeFullScreenStepContent` trong `Box` 484 dp (bố cục cột của T-041) và 255 dp (bố cục hàng gọn của T-041); `assertCtaFits` với 52 dp, ba cỡ chữ. Qua trên code hiện tại (37,3 dp trong 52 dp). **Đối chứng:** tạm đặt `NativeFullScreenAdDimens.CallToActionHeight = 36.dp`: đỏ ở 2.0; hoàn lại |
| KB-20, OB-01: màn xem thử đặt khung thẻ theo chiều cao thẻ | Compose UI test (`:app` staging) | `AdGalleryScreenTest.flowSteps_stepCardFrame_followsCardHeight_atFontScale2`: màn xem thử trong `WithFontScale(2f)` (bản sao trong nguồn test của `:app`, hay dùng `LocalDensity` như hàm của T-044), trang "Flow steps" với thẻ giả. `FLOW_CARD_FRAME` cao bằng thẻ (270 dp); media của thẻ ít nhất 120 dp; nút Next cùng chỗ ở bước 1 và bước 2. **Lý do thất bại:** khung cố định 250 dp. **Đối chứng:** cùng test ở 1.0 thì khung 250 dp |
| KB-2 (thẻ `FULL`) ở 1.0 tới 2.0: test của T-044 theo luật CTA mới | Compose UI test (sửa) | `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales` (do T-044 tạo): câu "CTA cao 36 dp" thành: ở 1.0 và 1.3 đúng 36 dp; ở 2.0 ít nhất 36 dp và chữ CTA `didOverflowHeight == false`. Câu khác giữ nguyên (thẻ 418 dp, hàng sao 16 dp cách đáy thẻ 13 dp, mô tả 2 dòng, media ít nhất 120 dp, 200 dp ở 1.0). Sửa có trong plan, không phải nới (câu hỏi 1) |
| KB-2 (thẻ `FULL`) ở 3.0: test của T-044 theo luật CTA mới | Compose UI test (sửa) | `NativeAdCardTest.full_mediaFloor120dp_cardGrows_atFontScale3` (do T-044 tạo): câu "CTA 36 dp" thành "CTA ít nhất 36 dp và chữ CTA `didOverflowHeight == false`"; giữ: media 120 dp, hàng sao 16 dp cách đáy thẻ 13 dp, thẻ cao hơn 418 dp. Số thẻ ở 3.0: plan T-044 đo 488 dp với CTA 36 dp; CTA vừa chữ ở 3.0 cao 60 dp (đo ở mục 4.1), nên dự kiến 512 dp. Test chỉ khẳng định "hơn 418 dp", không khẳng định 488 |
| KB-1, KB-2: khung chờ bằng thẻ ở 1.0 | Compose UI test (đã có) | `placeholder_matchesCardHeight_forEachSize`, `full_placeholderHeight_equalsCardHeight_shortAndLongTexts`, `medium_is410dpHigh`, `small_is80dpHigh`, `step_is250dpHigh_icon32_cta36_headline13sp`, `CollapsibleNativeAdTest.firstShown_isExpanded250dp`: giữ nguyên, phải xanh |
| Cỡ chữ 1.0 không đổi (thiết kế) | Screenshot test (đã có) | Mọi ảnh của `core/ads`, `app`, `feature/sample`, `feature/loading` không ghi lại; `verifyRoborazzi*` của `scripts/check.sh` phải qua |
| Giao diện thẻ ở cỡ 2.0 | Screenshot test (mới) | `NativeAdCardScreenshotTest.fontScale2_light`: `Column` với `SMALL`, `MEDIUM`, `STEP` (379 dp, cách nhau 16 dp), bọc `WithFontScale(2f)`: `captureRoboImage("src/test/screenshots/NativeAdCard_font-scale-2_light.png")`. Ảnh tĩnh (L-048 không áp dụng) |
| Giao diện sau inter ở cỡ 2.0 | Screenshot test (mới) | `NativeFullScreenAdScreenScreenshotTest.fontScale2_light`, bọc `WithFontScale(2f)`, ở trạng thái đã hết đếm ngược như ảnh `default` sẵn có: `captureRoboImage("src/test/screenshots/NativeFullScreenAdScreen_font-scale-2_light.png")` |
| Đúng thiết kế | Không có ảnh thiết kế riêng (thiết kế chỉ vẽ cỡ 1.0, ảnh ở 1.0 không đổi). Ảnh thiết kế của ticket khác (T-014, T-027, T-030, T-036): `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_default_light`, `AdGalleryScreen_inlist_light`, `NativeFullScreenAdScreen_default_dark`, `NativeFullScreenAdScreen_default_light`, `SampleScreen_ad-loading_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light` | Không ghi lại ảnh nào |
| Native ở cỡ chữ 2.0 trên máy thật, quảng cáo thật | Kiểm tra thủ công (máy thật: khác, cần đổi cỡ chữ trong cài đặt máy; `device-session` và `ax.sh` không đổi cài đặt hệ thống) | Bản staging, cỡ chữ lớn nhất (Android 14 trở lên: 200%). Màn xem thử: small, medium, "Grid ads", "Flow steps" (thẻ 250 dp thành khoảng 270 dp, Next không nhảy giữa các bước), thanh đáy với lớp thu gọn, nút "Interstitial" rồi native sau inter. Chữ CTA và nhãn "Ad" đọc trọn; bấm CTA mở quảng cáo; video (nếu có) hiện trong media |

**Ngoại lệ viết test trước:** sửa `docs/features/ads/design/README.md`, `docs/ops/ads-guide.md`, KDoc của `BottomAdBar.kt` (chỉ tài liệu). Hai ảnh chuẩn mới ghi sau phần sửa (chỉ có nghĩa với code đã sửa); test của ảnh vào commit test, ảnh vào commit code.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, worktree `claude/plan-T-045` từ nhánh chính `04ae69a` (T-041 đến T-044 **chưa** merge), bằng hai test tạm `TempT045CtaTest.kt`, `TempT045bTest.kt`, không commit (đã xóa, worktree sạch). Cỡ chữ đặt bằng `CompositionLocalProvider(LocalDensity provides Density(density, fontScale))`, cùng cách của `WithFontScale` mà plan T-044 ghi. Lần 1 đo code hiện tại; lần 2 áp tạm phần sửa A (khung CTA), B (`MediaShrinksFirstLayout` cho medium, thẻ ở bước, lớp thu gọn; khung `heightIn`), D (đầu sau inter `heightIn`), rồi đo lại và chạy cả bộ test và so ảnh. Phần C (`NativeStepAdDefaults.height()` bằng bộ đo chữ) chưa viết; test tạm chỉ so chiều cao thẻ thật với một công thức thử theo `lineHeight` (lệch 6 tới 10 dp ở 1.8 và 2.0, vì `lineHeight` đổi theo bảng phi tuyến theo cỡ chữ của từng kiểu), nên plan chọn đo bằng `rememberTextMeasurer` (mục 3.1, C) và có phương án dự phòng 3.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TempT045*'`; `./gradlew --continue testDebugUnitTest testStagingDebugUnitTest :core:ads:verifyRoborazziDebug :feature:sample:verifyRoborazziDebug :feature:loading:verifyRoborazziDebug :app:verifyRoborazziStagingDebug` (JDK của Android Studio). Không chạy `scripts/check.sh`: E = 1 (mục 5), không thêm thư viện.
- **Kết quả, code hiện tại (L-029).** Chữ `labelLarge` cao 20, 27, 37,3 dp ở 1.0, 1.3, 2.0; chữ 13 sp của small 20; 26,7; 38,7 dp.

  | Dạng | Khung CTA | 1.0 | 1.3 | 2.0 |
  | --- | --- | --- | --- | --- |
  | Small (thẻ 80) | 34 | 34 | 34 | 34, chữ tràn (38,7) |
  | Medium (410) | 44 | 44 | **32** (ép), mô tả 1 dòng | **10,7** (ép), chữ tràn |
  | Medium `ctaLarge` (422) | 56 | 56 | 44 (ép) | **22,7**, chữ tràn |
  | Đầy đủ (418, trước T-044) | 36 | 36 | 36, hàng sao 6 dp | 32,7, chữ tràn |
  | Thẻ dọc (300) | 36 | 36 | 36 | 36, chữ tràn (37,3) |
  | Thẻ ở bước (250) | 36 | 36 | 36 | 36, chữ tràn (37,3) |
  | Lớp thu gọn (250) | 36 | 36 | 36 | 36, chữ tràn (37,3) |
  | Toàn màn hình sau inter | 52 | 52 | 52 | 52, chữ vừa |
  | Trang toàn trang 484 dp | 52 | 52 | 52 | 52, chữ vừa |

  Media của thẻ ở bước 136; 124,3; 101,3 dp và của lớp thu gọn 134; 122,3; 99,3 dp ở 1.0, 1.3, 2.0 (thẻ dọc 194; 182,3; 159,3). Nhãn "Ad" của màn sau inter và trang toàn trang (đầu cố định 48 dp) cao 16; 13,7; 2 dp. Nhãn "Ad" của các thẻ trong `NativeAdCard.kt` đủ cao (16, 21, 32 dp).
- **Kết quả, phần sửa áp tạm.**

  | Dạng | 1.0 | 1.3 | 1.5 | 1.8 | 2.0 | 3.0 |
  | --- | --- | --- | --- | --- | --- | --- |
  | Medium: thẻ / media | 410 / 196 | 410 / 170 | 410 / 153 | 410 / 138 | 410 / 128 | 502 / 120 |
  | Thẻ ở bước: thẻ / media | 250 / 136 | 250 / 124,3 | 253 / 120 | 262,7 / 120 | 270 / 120 | 330 / 120 |
  | Lớp thu gọn: lớp / media | 250 / 134 | 250 / 122,3 | 255 / 120 | 264,7 / 120 | 272 / 120 | 332 / 120 |
  | Sau inter: nhãn "Ad" / media | 16 / 595 | 21 / 573,7 | 24 / 525,3 | 29 / 506 | 32 / 493 | 48 / 379 |

  CTA ở 1.0 và 1.3 đúng số thiết kế ở mọi dạng; ở 2.0 small 38,7 dp (thẻ 80), thẻ dọc, thẻ ở bước, lớp thu gọn 37,3 dp, medium 44, `ctaLarge` 56 dp; chữ không tràn ở mọi dạng tới 2.0 (ở 3.0 các thẻ vẫn vừa, khung 60 dp; toàn màn hình 52 dp tràn ở 3.0, cỡ máy không cho đặt). Medium mô tả 2 dòng ở 2.0 ("…" ở dòng 2). Khung chờ của thẻ ở bước vẫn 250 dp ở mọi cỡ (phần C chưa áp): đây là chỗ test `step_mediaFloor120dp_cardGrows_fontScales` kiểm.
- **Kết quả, bộ test và ảnh với phần sửa áp tạm (trên nhánh chính trước T-044, T-041):** 1371 test JVM (1364 sẵn có cộng 7 test tạm), 0 lỗi; `verifyRoborazzi` của `:core:ads`, `:app` (staging), `:feature:sample`, `:feature:loading` qua, không ảnh nào đổi ở cỡ 1.0. Sau khi T-044, T-041 merge đây là kết quả **dự kiến**: phiên chạy lại bộ test sau khi gộp nhánh chính, test của T-044, T-041 ngoài mục 3.2 đỏ thì dừng (mục 7).
- **Chưa chạy thử:** tách `MediaShrinksFirstLayout` từ `FullLayout` của T-044 (chưa có trên nhánh chính; bản thử dùng một hàm mới cho ba bố cục, không đụng `FullLayout`); `NativeStepAdDefaults.height()`; màn xem thử ở cỡ 2.0; các bước đối chứng (theo số đo ở trên).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi cách vẽ native khi cỡ chữ lớn; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm vị trí, quyền, SDK, sự kiện hay dữ liệu. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước (thẻ ở bước và lớp thu gọn cao hơn 250 dp khi chữ lớn).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads` code và test, `:app` staging code và test); API công khai mới `NativeStepAdDefaults.height()`; sáu file code |
| B. Độ rõ của yêu cầu | 1 | Lỗi đo được, các câu hỏi đã trả lời (mục 8); spec ghi 250 dp ở KB-4, KB-20, plan hiểu là cỡ chữ mặc định (câu 5) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi dạng có test đo ở nhiều cỡ chữ và ảnh ở 2.0; quảng cáo thật ở cỡ chữ lớn xem trên máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi chiều cao thẻ ở bước mà app đặt khung dựa vào (cần `NativeStepAdDefaults.height()`); sửa file mà T-044, T-041 cũng sửa (mục 6) |
| E. Độ mới của kỹ thuật | 1 | Biến thể của `Layout` riêng và sàn media của T-044; `rememberTextMeasurer` là API Compose phổ biến, lần đầu dùng để tính chiều cao trước |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `NativeFlowStep.kt`, `NativeAdSlot.kt`, `NativeFullScreenAdScreen.kt`, `BottomAdBar.kt` (KDoc), `AdGalleryScreen.kt`, `NativeAdCardTest.kt`, `AdGalleryScreenTest.kt`, hai test screenshot, README thiết kế mục 3, `ads-guide.md` mục 4. `tokens.json`, bảng khóa Remote Config, ảnh ở cỡ 1.0 không bị ảnh hưởng.
- `docs/STATUS.md`: không đưa vào PR.
- **T-044 (chạy trước):** tạo `FontScaleTestSupport.kt`, `MinMediaSize`, `fullMediaHeight`, `Layout` của `FullLayout`, hai test `full_partsKeepSize_…`, `full_mediaFloor120dp_…`; sửa dòng Media của README mục 3, cột "Đầy đủ" của bảng `ads-guide.md` mục 4, `AdGalleryScreenTest.buttons_haveTouchTargetOf48dp`. T-045 dùng (không tạo lại, L-054), tách phần đo của `FullLayout` thành `MediaShrinksFirstLayout`, sửa một câu ở mỗi test trong hai test kể trên, sửa dòng khác của README và ô khác của bảng.
- **T-041 (chạy trước):** sửa `NativeFlowStep.kt` (cổng, khung chờ, `NativeFullScreenStepDefaults`), `NativeFullScreenAdScreen.kt` (đầu dùng chung, nhánh trang), `AdGalleryScreen.kt` (`FlowStepsPage`), `AdGalleryScreenTest.kt` (nhóm `flowSteps_*`), README mục 8, `ads-guide.md` mục 3. T-045 sửa cùng file ở chỗ khác: thêm `NativeStepAdDefaults` cạnh `NativeFullScreenStepDefaults`; đầu sau inter dùng chiều cao tối thiểu mà T-041 thêm; khung thẻ và đệm đáy của `FlowStepsPage` theo bản T-041; một test mới trong `AdGalleryScreenTest`. CTA 52 dp của trang toàn trang giữ được ở cả hai bố cục của T-041 (cột và hàng gọn), có test canh.
- **T-042, T-043:** không chung file.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: thứ 5 trong RUN-20261026-05, sau T-041 (yêu cầu của anh Nhật), `depends_on: [T-044, T-041]`. Phiên gộp nhánh chính sau T-041 trước khi viết test. T-044 hay T-041 dừng thì T-045 bị bỏ qua.

## 7. Điều kiện dừng riêng

- Khi bắt đầu task, T-044 chưa merge (thiếu `FontScaleTestSupport.kt`, `NativeAdDimens.MinMediaSize`, `fullMediaHeight`, `Layout` của `FullLayout`) hay T-041 chưa merge (thiếu `FlowStepPageLayout`, đầu dùng chung có chiều cao tối thiểu, `FlowStepsPage` mới): dừng và báo, không tạo bản riêng.
- Một test của T-044 hay T-041 không có trong mục 3.2 đỏ sau phần sửa: dừng, không sửa test đó; báo test, số đo và lý do.
- Một ảnh chuẩn sẵn có (cỡ 1.0) lệch sau phần sửa: chuyển sang phương án dự phòng 2 của mục 3.4 một lần; vẫn lệch thì dừng, không ghi lại ảnh. Plan chỉ cho ghi hai ảnh mới `NativeAdCard_font-scale-2_light.png`, `NativeFullScreenAdScreen_font-scale-2_light.png`.
- Phần sửa cần đổi `NativeFlowStepLayout.kt`, bố cục của trang toàn trang hay khung chờ `NativeFullScreenOverlay.kt`: ngoài phạm vi, dừng và hỏi.
- Thẻ ở bước hay lớp thu gọn cao khác 250 dp ở cỡ chữ 1.0: dừng (thiết kế ở 1.0 phải giữ).

## 8. Câu hỏi cho người duyệt

1. **Khung CTA cao vừa chữ hay giữ cố định?** Ở cỡ 2.0 dòng chữ CTA cao 37,3 dp (small 38,7 dp), khung thiết kế 34 hay 36 dp.
   - Đề xuất của AI: khung cao **ít nhất** số thiết kế, cao thêm vừa một dòng chữ khi cần; sửa một câu ở mỗi test trong hai test của T-044 thành "ít nhất 36 dp và chữ không tràn". Phương án khác: (b) khung cố định, chữ không có khoảng dòng thêm khi tràn (dự phòng 2); (c) khóa cỡ chữ của CTA ở 1.3 (không đề xuất).
   - **Trả lời (anh Nhật, 2026-10-26):** khung CTA có chiều cao tối thiểu, cao thêm theo chữ: đồng ý.
2. **Nhãn "Ad" của native toàn màn hình sau inter bị ép khi chữ lớn** (2 dp ở 2.0, O-08).
   - Đề xuất của AI (bản đầu): ticket sau, dùng đầu `heightIn` của T-041 cho nhánh sau inter.
   - **Trả lời (anh Nhật, 2026-10-26):** không để sau, gộp vào T-045: nhãn "Ad" giữ trọn chiều cao chữ, đầu cao theo cỡ chữ, dùng lại cách T-041 làm cho trang toàn trang. Plan: mục 3.1 D, mục 4 dòng 6, số đo ở mục 4.1.
3. **Media dưới 120 dp ở thẻ ở bước và lớp thu gọn khi chữ lớn** (101 và 99 dp ở 2.0).
   - Đề xuất của AI (bản đầu): ticket sau.
   - **Trả lời (anh Nhật, 2026-10-26):** không để sau, gộp vào T-045: media ít nhất 120 dp ở cỡ 2.0, dùng sàn và cách "media co trước" của T-044, thẻ cao thêm khi cần; nêu rõ chiều cao thẻ đổi thế nào và nơi đặt thẻ vẫn ổn. Plan: mục 3.1 B, C (thẻ 250 dp tới 1.3, 270 dp ở 2.0; lớp 272 dp; khung chờ và khung của app theo `NativeStepAdDefaults.height()`; lớp nổi không chừa chỗ), mục 4 dòng 4, 5, 8.
4. **Vùng chạm của CTA dưới 44 và 48 dp** (34 dp ở small, 36 dp ở thẻ).
   - Đề xuất của AI: giữ như thiết kế; quy tắc 48 dp áp cho nút của app, CTA là view của quảng cáo.
   - **Trả lời (anh Nhật, 2026-10-26):** giữ vùng chạm của CTA theo thiết kế.
5. **Spec KB-4, KB-20 ghi "cao 250 dp"**, trong khi theo câu 3 thẻ ở bước và lớp thu gọn cao thêm từ khoảng cỡ chữ 1.4.
   - Đề xuất của AI: hiểu "250 dp" là ở cỡ chữ mặc định (như mọi số của thiết kế), không sửa spec trong task này; lần cập nhật spec sau thêm một câu "khi cỡ chữ hệ thống lớn, thẻ cao thêm để media không dưới 120 dp". Nếu anh muốn sửa spec trước thì plan chờ spec được xác nhận lại.
   - **Trả lời (anh Nhật, 2026-10-26):** đồng ý, "250 dp" là ở cỡ chữ mặc định; không sửa spec trong task này.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-26 trong phiên giám sát cùng AI. Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua rà soát độc lập, sửa theo điểm chặn và điểm nên sửa. Chạy thứ 5 trong RUN-20261026-05. 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
