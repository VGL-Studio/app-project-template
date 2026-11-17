---
ticket: T-110
title: Code native medium theo thẻ JBase vẽ ra
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20.2M        # cỡ M, số gợi ý 20.2M (scripts/token-usage.py estimate, 2026-11-17), không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật
cross_reviewer: Nhật         # nhóm một dev (risk-scoring.md, mục 3)
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-110: CODE NATIVE MEDIUM THEO THẺ JBASE VẼ RA

## 1. Tóm tắt

Đổi native medium của `:core:ads` theo thiết kế T-108 (`Native/Medium`): thẻ 250 dp ở mọi khung ở cỡ chữ 1.0, ảnh ở trên sát mép thẻ lấp phần còn lại (144 dp; 132 dp với CTA lớn), dưới là khối nội dung hai tầng của native nhỏ (plan T-109) với hàng sao ở cuối dòng dưới tiêu đề; bỏ đoạn mô tả hai dòng, chiều cao theo chiều cao màn hình (`mediumHeight`) và khung thấp (`MediumCompactBelow`). Chữ lớn làm ảnh dưới 120 dp thì thẻ cao thêm. Chiều cao tính trước từ cỡ chữ (cùng cách đo của `smallNativeHeight`, plan T-109), dùng chung cho thẻ, khung chờ và chỗ giữ khi trống. Chạy sau T-109, trên code của T-109.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-17, câu hỏi 66):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2, tiêu chí 1 | Có quảng cáo thì thấy thêm ảnh hoặc video, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ | Ảnh 379 x 144 dp ở khung 411 dp (132 dp với CTA lớn), không dưới 120 dp ở mọi cỡ chữ: thẻ cao thêm thay vì co ảnh dưới 120 dp. `MediaView` của SDK tự giữ tỷ lệ trong khung như hôm nay |
| KB-2, tiêu chí 2 | Giữ chỗ, không có quảng cáo, chế độ tối như KB-1 | Khung chờ và chỗ giữ khi trống (`keepSpaceWhenEmpty`, pool cạn có `keepSpaceWhenPoolEmpty`) cùng chiều cao với thẻ ở mọi cỡ chữ; màu theo token như hôm nay, có ảnh tối |
| KB-2, tiêu chí 3 | Hai ô medium cùng màn có hai quảng cáo khác nhau | Không đổi (pool); test `NativeAdCellTest.twoMediumCells_showDifferentAds` giữ |
| KB-2, tiêu chí 4 | Chạm hàng sao là chạm quảng cáo | Hàng sao chuyển sang cuối dòng dưới tiêu đề, vẫn đăng ký `starRatingView` khi có sao; test `NativeAdViewBindingTest.registersStarRatingView_medium_whenRated` giữ |
| KB-3, tiêu chí 3 | Ô quảng cáo trong danh sách có nhãn "Ad" rõ | Nhãn "Ad" trước tiêu đề như native nhỏ; dòng medium trong danh sách cao 282 dp (250 + 16 + 16) thay 432 dp |
| KB-32, tiêu chí 3 (đổi 2026-11-17, câu hỏi 66) | Medium 250 dp, ảnh ở trên lấp phần còn lại, cao thêm khi ảnh dưới 120 dp | Một hàm `mediumNativeHeight`: max(250 dp, khối nội dung + 120 dp); ở cỡ chữ 1.0 luôn 250 dp (nội dung 106 hay 118 dp), không còn theo chiều cao màn hình |

Giả định: plan T-109 đã chạy xong trước (ticket `depends_on: [T-109]`), nên code có `smallNativeHeight`, `rememberSmallNativeHeight`, `nativeAdHeight(size, ctaLarge)`, `SmallLayout` hai tầng, `SmallTexts` và khung chờ SMALL hai tầng như mục 3.1 của plan T-109. Tên hàm dưới đây dẫn theo plan đó; phiên đọc code thật trước khi sửa.

Thiết kế: `docs/features/ads/design/README.md` mục 1 (đợt T-108, dòng `Native/Medium`), mục 3 (khung, media, nhà quảng cáo, CTA); số liệu ở `design/context/AdGalleryScreen_default_light.md`, frame "Native/Medium": thẻ 379 x 250, không lề, cách nhau 0; "Media" 379 x 144 ở góc trên; "Content (as small)" 379 x 106, lề 6/12/6/12, cách nhau 6; "Top row" 355 x 44, cách nhau 12, **không lề phải** (AdChoices nằm trên ảnh, 375,270: cách mép trên và mép phải thẻ 4 dp); "Texts" 299 x 44; "Advertiser row" cách nhau 6: chữ 223 x 18, "Rating" 70 x 16 (bốn sao 16 dp cách 2 dp); "Ad/CTA" 355 x 44, chữ 18/24.

## 3. Giải pháp

### 3.1. Cách làm

**Chiều cao (một hàm cho thẻ, khung chờ, chỗ giữ).** Trong `NativeAdCard.kt`, tổng quát hóa hàm của plan T-109:

- `internal fun nativeContentHeight(measurer: TextMeasurer, typography: Typography, density: Density, ctaLarge: Boolean): Dp`: khối nội dung hai tầng; hàng trên như `smallNativeHeight` (max(44 dp, hàng tiêu đề + 4 dp + dòng dưới 13/18 sp)); CTA = max(44 dp, dòng `labelLarge` 18 sp), hay max(56 dp, dòng 17 sp) khi `ctaLarge`; 6 + hàng trên + 6 + CTA + 6, cộng theo px, đổi ra dp, làm tròn lên dp nguyên. `smallNativeHeight(...)` = `nativeContentHeight(..., ctaLarge = false)`, không đổi kết quả (test của T-109 giữ).
- `internal fun mediumNativeHeight(measurer, typography, density, ctaLarge): Dp` = max(250 dp, `nativeContentHeight` + 120 dp). Ở cỡ chữ 1.0: nội dung 106 dp (118 dp với CTA lớn, 6 + 44 + 6 + 56 + 6), nên thẻ 250 dp ở mọi mật độ (cả 2.625, nơi nội dung có thể 107 tới 108 dp); ảnh 144 dp (132 dp). Chữ lớn: khi nội dung quá 130 dp thì thẻ = nội dung + 120 dp.
- `@Composable internal fun rememberMediumNativeHeight(ctaLarge: Boolean): Dp` nhớ theo `measurer`, `typography`, `density`, `ctaLarge`.
- `nativeAdHeight(size, ctaLarge)` (plan T-109) nhánh MEDIUM trả `rememberMediumNativeHeight(ctaLarge)`. `NativeAdDimens.heightOf` bỏ nhánh MEDIUM như nhánh SMALL của T-109 (ném `IllegalArgumentException`, lời nhắn dùng `nativeAdHeight`); KDoc bỏ câu "medium as the screen height says". Tham số `screen` của `heightOf` không ai dùng nữa thì bỏ cùng `@Suppress`.
- Bỏ `mediumHeight`, `mediumIsCompact`, `MediumCompactBelow`, `CompactBodyHeight`, `CompactPartGap`, `LowScreen`, `HighScreen` (không còn ai đọc), `BodyHeight` nếu chỉ medium dùng (khung chờ FULL dùng thì giữ).

**Khối nội dung dùng chung.** Tách phần thân `SmallLayout` của T-109 thành `@Composable private fun NativeContentBlock(assets, binder, topRowEnd: Dp, starsOnSecondLine: Boolean, ctaLarge: Boolean, modifier)`: `Column` lề trái phải 12 dp, đệm trên dưới 6 dp, cách nhau 6 dp; hàng trên (icon 44 dp, cách 12 dp, `SmallTexts`, lề phải `topRowEnd`); CTA ngang hết, cao ít nhất 44 dp chữ 18 sp, hay 56 dp chữ 17 sp khi `ctaLarge` (chữ của SDK giữ nguyên, không viết hoa). `SmallLayout` = `NativeContentBlock(topRowEnd = 16.dp, starsOnSecondLine = false, ctaLarge = false)`, không đổi hình (ảnh chuẩn của native nhỏ không đổi).

`SmallTexts` thêm tham số `starRating: Double?` (null: như T-109). Với medium, dòng dưới tiêu đề là `Row` cách nhau 6 dp, căn giữa dọc: chữ (nhà quảng cáo, không có thì mô tả; 13/18 sp, một dòng, cắt cuối, `weight(1f)`) rồi `NativeAdStarRow` (sao 16 dp cách 2 dp, chỉ vẽ khi điểm lớn hơn 0, đăng ký `STAR_RATING` như hôm nay). Không có điểm thì không có sao, chữ lấy cả dòng; dòng giữ 18 dp nên không gì dịch chuyển (L-055). Dòng có mặt khi có nhà quảng cáo, mô tả hay sao; không có cả ba thì không có dòng dưới, như native nhỏ (thẻ vẫn cao theo hàm, hàng trên căn giữa trong 44 dp).

**Bố cục `MediumLayout` (thiết kế T-108).** Thẻ `Modifier.height(nativeAdHeight(MEDIUM, ctaLarge))` thay `heightIn(min = heightOf(...))`. Bên trong một `Column` không lề:

- `NativeAdMediaArea(binder, Modifier.fillMaxWidth().weight(1f), backdrop = true, shape = ...)`: ảnh sát mép trên và hai bên (thẻ cắt hai góc trên theo bo 12 dp của khung), hai góc dưới bo 8 dp; `NativeAdMediaArea` thêm tham số `shape` (mặc định bo 8 dp bốn góc như hôm nay, thẻ đầy đủ, thẻ ở bước, lớp thu gọn không đổi). Viền 1 dp của khung vẽ đè lên ảnh như vẽ đè nền.
- `NativeContentBlock(topRowEnd = 0.dp, starsOnSecondLine = true, ctaLarge)`.

Ảnh lấy phần thẻ còn lại sau khối nội dung đo thật. Bỏ `MediaShrinksFirstLayout` khỏi medium (thẻ đầy đủ, thẻ ở bước vẫn dùng), bỏ `MediumHeader` (hàng sao riêng 16 dp dưới dòng nhà quảng cáo) và đoạn mô tả hai dòng (`NativeAdBody` với tag `BODY` không còn trong medium; asset body chỉ hiện ở dòng dưới khi không có nhà quảng cáo, tag `SECOND_LINE`). AdChoices ở góc trên phải của thẻ, nằm trên ảnh (`AdChoicesMarker` của ảnh giả giữ lề 4 dp); không cần chừa góc ở hàng trên.

Số ở khung 411 x 891, thẻ 379 dp: ảnh 0 tới 144, hàng trên 150 tới 194 (icon 12,150), dòng nhà quảng cáo cao 18 dp ở 176, chữ 223 dp và bốn sao 70 dp ở cuối (mép phải sao cách mép phải thẻ 12 dp), CTA 200 tới 244 rộng 355 dp, thẻ 250. CTA lớn: ảnh 132, CTA 188 tới 244. Khung 360 x 640 và 673 x 841: cùng 250 dp (không còn 320, 384 dp). Gallery: dòng medium trong danh sách 282 dp; trang mặc định, mọi thứ dưới thẻ medium lên 150 dp ở khung 411 x 891 (400 → 250), lên 70 dp ở 360 x 640 (320 → 250).

**Khung chờ MEDIUM.** `NativeAdPlaceholderLayout` nhánh MEDIUM: `Column` không lề: khối ảnh `fillMaxWidth().weight(1f)` cùng hình dạng với ảnh (góc dưới 8 dp), rồi khối nội dung chờ hai tầng của T-109 (khối icon 44 dp, hai vạch, khối CTA ngang hết thẻ) trong cùng lề 12/6 dp, khối CTA cao 44 dp hay 56 dp theo `ctaLarge`. Tham số `mediumCallToActionHeight: Dp` đổi thành `ctaLarge: Boolean` (chỉ `NativeAdPlaceholder` gọi). `NativeAdPlaceholder` lấy chiều cao qua `nativeAdHeight` (T-109 đã đổi), nên khung chờ cao bằng thẻ ở mọi cỡ chữ (hôm nay khung chờ medium giữ chiều cao thiết kế, không theo chữ lớn). Bỏ `MediumPlaceholderBlocks` cũ.

**Nơi khác đọc chiều cao medium.** `NativeAdSlotContent` (chỗ giữ khi trống), `NativeAdCell` (ô medium, khung chờ tĩnh khi pool cạn) và hàng medium của `InListAds` đã đi qua `nativeAdHeight`/`NativeAdPlaceholder` sau T-109; phiên `grep` `heightOf(` và `NativeAdSize.MEDIUM` trong `core/ads/src/main` để chắc không còn chỗ gọi `heightOf(MEDIUM, ...)`. Chỗ nào còn thì đổi sang `nativeAdHeight` trong cùng file (`NativeAdSlot.kt`, `NativeAdCell.kt`, `InListAds.kt` có trong mục 3.2).

**Không đổi:** native nhỏ, lớp thu gọn (T-109), thẻ đầy đủ, thẻ dọc, thẻ ở bước, toàn màn hình; pool, nạp, làm mới, sự kiện; tỷ lệ ảnh yêu cầu (`NativeRequestOptions`: medium vẫn `LANDSCAPE`).

**Tài liệu.** `ads-business-rules.md` mục 5.7: dòng Native medium viết lại (250 dp ở cỡ chữ 1.0 mọi khung, ảnh ở trên sát mép 144 dp, 132 dp với CTA lớn; khối nội dung như native nhỏ, nhà quảng cáo hay mô tả một dòng với hàng sao ở cuối; chữ lớn làm ảnh dưới 120 dp thì thẻ cao thêm; bỏ câu khung thấp và "media co trước"); đoạn mở đầu mục 5.7 bỏ medium khỏi "chiều cao nội suy giữa hai mốc". `ads-guide.md` mục 3 (dòng Native medium của bảng định dạng: "cao 250 dp, cao thêm khi chữ lớn") và mục 4 (cột Medium của bảng: khung, ảnh, chữ, hàng sao ở cuối dòng nhà quảng cáo, CTA ngang hết; câu "chiều cao nội suy" của đoạn đầu mục bỏ medium). README thiết kế: dòng Media của mục 3 thay "(medium 250 dp: số cụ thể ở cỡ chữ lớn do plan T-110 đo)" bằng quy tắc thẻ = khối nội dung + 120 dp khi chữ lớn; ghi chú khung thấp của medium ghi đã bỏ ở T-110; mục 8 thêm bảng đối chiếu T-110, chép ảnh `*_compare.png` vào `review/`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `nativeContentHeight`, `mediumNativeHeight`, `rememberMediumNativeHeight`; `nativeAdHeight` nhánh MEDIUM; `heightOf` bỏ nhánh MEDIUM; `NativeContentBlock` tách từ `SmallLayout`; `SmallTexts` có hàng sao; `MediumLayout` ảnh sát mép trên và khối nội dung; `NativeAdMediaArea` tham số `shape`; khung chờ MEDIUM; bỏ `mediumHeight`, `mediumIsCompact`, `MediumCompactBelow`, `CompactBodyHeight`, `CompactPartGap`, `LowScreen`, `HighScreen`, `MediumHeader`, `MediumPlaceholderBlocks`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | chỉ khi còn gọi `heightOf(MEDIUM, ...)` (mục 3.1); KDoc nhắc 400 dp nếu có |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCell.kt` | chỉ khi còn gọi `heightOf(MEDIUM, ...)`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | chỉ khi còn gọi `heightOf(MEDIUM, ...)`; KDoc |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdDimensTest.kt` | viết lại theo mục 3.5 (bỏ test của `mediumHeight`, `mediumIsCompact`; `heights_areWholeDp` bỏ dòng medium) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | test mới bố cục medium; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | test mới của `mediumNativeHeight`, cỡ chữ của medium; viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCellTest.kt` | 400 → 250 dp (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | 400 → 250 dp (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | 400/182 → 250/144 dp (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | chú thích của ảnh font-scale-2 (medium ảnh 120 dp, thẻ cao thêm) nếu nhắc số cũ |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_medium_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_medium_dark.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_medium-cta-large_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdPlaceholder_medium_light.png` | ghi lại |
| Sửa | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_light.png` | ghi lại (phần medium) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | dòng medium 432 → 282 dp (mục 3.5) |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_dark.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default_small.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default-more_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_inlist_light.png` | ghi lại |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_inlist_small.png` | ghi lại |
| Sửa | `docs/ops/ads-business-rules.md` | mục 5.7: đoạn mở đầu, dòng Native medium |
| Sửa | `docs/ops/ads-guide.md` | mục 3 (dòng Native medium), mục 4 (đoạn đầu, cột Medium) |
| Sửa | `docs/features/ads/design/README.md` | mục 3 dòng Media và ghi chú khung thấp; mục 8 bảng đối chiếu T-110 |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_default_light_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_default_dark_compare.png` | ảnh so sánh mới |
| Sửa | `docs/features/ads/design/review/AdGalleryScreen_inlist_light_compare.png` | ảnh so sánh mới |

Nơi đọc các file dùng chung của bảng (`scripts/plan-check.py`, L-020): `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md`: không test, script hay workflow nào mở hai file này; `scripts/ads-plan.py` chép tay bảng định dạng và chỉ nhắc tên file trong chữ dẫn nguồn; các test sau chỉ nhắc tên file trong chú thích hay KDoc để dẫn nguồn quy tắc khác, không chép chữ của dòng medium hay đoạn mở đầu mục 5.7, mục 3, mục 4 mà plan sửa, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` (`AdsSettingsTest.kt` nhắc cả `ads-guide.md`). Ảnh trong `review/` chỉ người xem; `scripts/tests/run.sh` không đọc ảnh của ads. `NativeAdCard.kt` là file chung của mọi native: phần native nhỏ chỉ được tách hàm, không đổi hình (ảnh chuẩn của native nhỏ, lớp thu gọn phải giữ nguyên, mục 7).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm vị trí hay định dạng; đổi kích thước và bố cục native medium theo spec `ads`, KB-32 tiêu chí 3 (câu hỏi 66). Asset vẫn đăng ký đủ với SDK (headline, body hay advertiser, icon, CTA, media, star rating).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp dùng: `TextMeasurer`, `rememberTextMeasurer` (gói `androidx.compose.ui.text`, đã dùng ở `NativeFlowStep.kt` và, sau T-109, ở `NativeAdCard.kt`), `RoundedCornerShape` bốn góc riêng của Compose Foundation; đều trên classpath của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** khối nội dung đo thật lệch `nativeContentHeight` 1 tới 2 px ở cỡ chữ lớn, làm ảnh thấp hơn 120 dp một chút hay khối nội dung tràn khỏi thẻ.
- **Điều kiện chuyển:** test `medium_partsInsideCard_mediaAtLeast120_fontScales` vẫn thất bại vì ảnh dưới 120 dp hay phần dưới cùng tràn sau 2 lần sửa hàm đo.
- **Cách làm thay:** dùng lại phương án dự phòng của plan T-109 nếu T-109 đã dùng (`SubcomposeLayout` đo khối nội dung mẫu); không thì `MediumLayout` đặt khối nội dung trước bằng `Layout` tự viết: đo khối nội dung, ảnh = max(thẻ − nội dung, 120 dp), thẻ = ảnh + nội dung (như `MediaShrinksFirstLayout` hôm nay với một phần), và `nativeAdHeight(MEDIUM)` chỉ dùng cho khung chờ và chỗ giữ. Cùng file, cùng test; không hạ tiêu chí.

### 3.5. Hành vi đã có bị đổi

Từ `grep` các số 400, 406, 418, 182, 170, 432, 320, 384, 134, 122, 97 và tên `medium_*` trong test của `:core:ads` và `:app` (phiên chạy thêm `scripts/existing-tests.py` trên plan này ở bước 1 và so). Test chỉ dùng tên medium mà không kiểm kích thước (pool, sự kiện, `NativeAdCellTest.twoMediumCells_showDifferentAds`, `mediumSlot_recordsShownOnce_notReadyOnDispose`, `NativeAdViewBindingTest.registersStarRatingView_medium_whenRated`, `registersEveryAssetView_medium`, `NativeAdCardTest.medium_headlineAndCtaColors_lightAndDark`, `medium_ctaIs44dp_18sp`, `mediumPlaceholder_ctaIs44dp`, `AdGalleryScreenTest` khoảng cách 16 dp dưới medium) giữ nguyên. Test của native nhỏ, lớp thu gọn (T-109) giữ nguyên.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `mediumHeight` nội suy 320 tới 400 dp theo chiều cao màn hình, dp nguyên | Bỏ hàm; `mediumNativeHeight` = max(250, nội dung + 120), 250 dp ở cỡ chữ 1.0 | `NativeAdDimensTest.mediumHeight_interpolatesAndClamps`; dòng "medium" của `heights_areWholeDp` | Thay bằng `NativeAdFontScaleTest.mediumNativeHeight_is250dp_atFontScale1_wholeDp` (cùng ý định "một hàm, dp nguyên, đúng số thiết kế"): đúng 250 dp ở `Density(2f, 1f)`, `Density(3f, 1f)`, `Density(2.625f, 1f)`, cả `ctaLarge`; ở cỡ chữ 2.0 bằng `nativeContentHeight` + 120 khi lớn hơn 250, luôn dp nguyên. `heights_areWholeDp` bỏ dòng medium, giữ các dòng khác |
| Khung thấp: thẻ dưới 350 dp có mô tả 1 dòng, cách 8 dp | Bỏ (không còn đoạn mô tả, thẻ cố định 250 dp) | `NativeAdDimensTest.mediumIsCompact_belowCard350dp`; `NativeAdCardTest.medium_compact_lowScreen` (360 x 640); `NativeAdFontScaleTest.medium_compact_lowScreen_fontScales` | Ý định "màn thấp vẫn giữ ảnh ít nhất 120 dp, khung chờ cùng chiều cao" giữ bằng câu mới: `medium_compact_lowScreen` thành `medium_is250dp_lowScreen` (360 x 640: thẻ 250, ảnh 144 (132 với `ctaLarge`), khung chờ 250); `medium_compact_lowScreen_fontScales` thành `medium_lowScreen_sameAsStandard_fontScales` (cỡ chữ 1.0, 1.5, 2.0 ở 360 x 640: thẻ bằng `mediumNativeHeight`, ảnh ≥ 120 dp, khung chờ bằng thẻ); `mediumIsCompact_belowCard350dp` xóa cùng hàm (ADR-0031: hành vi bị bỏ theo câu hỏi 66, ý định nằm ở hai test trên) |
| Medium 400 dp ở 891 dp, 384 dp ở 841 dp, cả `ctaLarge` | 250 dp mọi khung | `NativeAdCardTest.medium_is400dpHigh`, `medium_heightFollowsScreenHeight`, `medium_heightFollowsScreenHeight_841` | Đổi tên `medium_is250dpHigh`, `medium_is250dp_everyWindow` (cả `ctaLarge`), `medium_is250dp_841`: số 250 |
| Bố cục T-064: đầu thẻ 60 dp (icon, tiêu đề, nhà quảng cáo, hàng sao riêng), ảnh 182 dp từ y = 97, mô tả 2 dòng, CTA; không có nhà quảng cáo thì giữ chỗ 18 dp | Ảnh ở trên 144 dp từ y = 0; khối nội dung hai tầng; dòng dưới nhà quảng cáo hay mô tả, sao ở cuối | `NativeAdCardTest.medium_jbaseLayout`, `medium_hasMediaOfAtLeast120dp` (182), `medium_leavesAdChoicesCornerFree` (icon cách mép trên ≥ 24 dp) | `medium_jbaseLayout` thay bằng `medium_twoTierLayout` (mục 4). `medium_hasMediaOfAtLeast120dp`: ảnh 144 dp. `medium_leavesAdChoicesCornerFree`: câu mới "góc AdChoices (16 x 16 dp, cách mép trên, mép phải thẻ 4 dp) nằm trong vùng ảnh, không đè chữ, sao hay CTA" |
| Hàng sao 16 dp dưới dòng nhà quảng cáo; điểm 0 thì không vẽ, giữ chiều cao, ảnh không dịch | Sao ở cuối dòng dưới tiêu đề; điểm 0 thì không vẽ, dòng giữ 18 dp, không gì dịch | `NativeAdCardTest.medium_starRow16dp_belowBadgeLine_hiddenStarsKeepHeight` | Đổi tên `medium_starsEndOfSecondLine_noRatingKeepsPlace`: 4 sao, hàng 16 dp, cùng hàng (tâm dọc lệch dưới 1 dp) và bên phải dòng chữ, cách 6 dp, mép phải cách mép phải thẻ 12 dp; điểm 0: không sao, thẻ 250, đỉnh ảnh và đỉnh CTA không đổi (giữ ý định L-055) |
| CTA lớn 56 dp chữ 17 sp, thẻ 400 dp | Thẻ 250 dp, ảnh 132 dp | `NativeAdCardTest.medium_ctaLarge_56dp_17sp_cardIs400dp` | Đổi tên `..._cardIs250dp`: 400 → 250, thêm ảnh 132 (144 khi tắt) |
| Khung chờ và thẻ medium 400 dp; chữ dài giữ 400 dp | 250 dp | `NativeAdCardTest.placeholder_matchesCardHeight_forEachSize` (nhánh MEDIUM, dòng 303-306 và 327), `longTexts_keepHeight` (phần MEDIUM) | 400 → 250; giữ ý định "khung chờ bằng thẻ", "chữ dài không đổi chiều cao" |
| Cỡ chữ: thẻ 400 dp tới cỡ 2.0 rồi 406 (418 với `ctaLarge`); ảnh co trước, mô tả 2 dòng; ảnh 182/170 ở cỡ chữ 1.0 | Thẻ = `mediumNativeHeight`; ảnh = thẻ − nội dung, ≥ 120 dp; dòng dưới 1 dòng | `NativeAdFontScaleTest`: bảng cỡ chữ (dòng "medium", "medium ctaLarge", mong đợi `if (it < 2f) 400 else 406/418`), `medium_partsKeepSize_mediaShrinksFirst_fontScales`, `medium_mediaFloor120dp_cardGrows_atFontScale3` | Bảng: mong đợi thành `mediumNativeHeight` đo cùng `TextMeasurer` của test (250 ở 1.0, lớn hơn 250 ở 2.0). `medium_partsKeepSize_mediaShrinksFirst_fontScales` thành `medium_partsInsideCard_mediaAtLeast120_fontScales` (mục 4); câu "mô tả 2 dòng" thay bằng "dòng dưới 1 dòng"; câu "ảnh 182/170 ở 1.0" thành 144/132. `medium_mediaFloor120dp_cardGrows_atFontScale3`: giữ ảnh 120 ± 0.5 dp và CTA ≥ 44 dp; "thẻ > 406" thành "thẻ = `nativeContentHeight` + 120 và > 250" |
| Ô medium, khe medium 400 dp | 250 dp | `NativeAdCellTest` (dòng 128, 155 của `exhausted_keepSpaceDeclared_showsStaticPlaceholder`, 231 của `inCollection_drawsCard_medium_drawsMedium`), `NativeAdSlotTest.unavailable_keepsSpace_whenDeclaredNextToButton` (dòng 108, phần MEDIUM; phần SMALL do T-109 viết lại) | 400 → 250 |
| SDK layout medium: thẻ 400, `MediaView` 182 | 250, 144 | `NativeAdViewBindingTest` (dòng 206-210) | 250, 144 |
| Gallery: dòng medium 432 dp (400 + 16 + 16) | 282 dp | `AdGalleryScreenTest` (dòng 184-185) | 432 → 282; chú thích "400 dp at 891 dp" thành "250 dp" |

## 4. Kiểm thử

Test viết trước ở bước 3 gồm các test mới dưới đây và mọi câu khẳng định viết lại của mục 3.5; chúng thất bại trên code sau T-109 vì chiều cao (400 thay 250 dp), bố cục cũ hay hàm chưa có (lỗi biên dịch của `mediumNativeHeight`, `nativeContentHeight`: phiên ghi lỗi biên dịch làm lý do thất bại). Test kích thước chạy `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi` trừ khi ghi khác, thẻ 379 dp, dung sai 0.5 dp (1 dp ở cỡ chữ lớn, như T-109).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-32 tiêu chí 3, KB-2 tiêu chí 1: bố cục `Native/Medium` (design/context/AdGalleryScreen_default_light.md) | Compose UI test | `NativeAdCardTest.medium_twoTierLayout`: thẻ 250; ảnh từ mép trên thẻ, rộng bằng thẻ, cao 144; icon 44 x 44, cách mép trái 12, cách đáy ảnh 6; hàng tiêu đề 22 dp, nhãn "Ad" trước tiêu đề, tâm lệch dưới 2 dp; dòng nhà quảng cáo 18 dp cách hàng tiêu đề 4 dp, chữ 13 sp một dòng; mép phải cột chữ cách mép phải thẻ 12 dp (không chừa 16 dp); CTA cách hàng trên 6, rộng 355, cao 44, chữ 18 sp, đáy cách đáy thẻ 6; không có node `BODY`; `ctaLarge`: ảnh 132, CTA 56 dp chữ 17 sp, thẻ 250; không có nhà quảng cáo: dòng dưới là mô tả một dòng (`isLineEllipsized` với mô tả dài), ảnh vẫn 144 |
| KB-2 tiêu chí 4, O-08: hàng sao | Compose UI test | `NativeAdCardTest.medium_starsEndOfSecondLine_noRatingKeepsPlace` (mục 3.5); `NativeAdViewBindingTest.registersStarRatingView_medium_whenRated` giữ |
| KB-1 tiêu chí 1: góc AdChoices trống | Compose UI test | `NativeAdCardTest.medium_leavesAdChoicesCornerFree` viết lại (mục 3.5) |
| KB-32 tiêu chí 3: 250 dp mọi khung, khung chờ cùng chiều cao | Compose UI test | `medium_is250dp_everyWindow`, `medium_is250dp_841`, `medium_is250dp_lowScreen` (mục 3.5); `NativeAdSlotTest.mediumHeight_equalsDrawnCardAndPlaceholderHeight` (mới: khe medium có quảng cáo, đang tải và giữ chỗ khi trống cùng chiều cao, ở cỡ chữ 1.0 và 2.0) |
| KB-32 tiêu chí 3: một hàm, dp nguyên | Unit test (Robolectric) | `NativeAdFontScaleTest.mediumNativeHeight_is250dp_atFontScale1_wholeDp` (mục 3.5); `smallNativeHeight` của T-109 không đổi (test của T-109 chạy lại) |
| KB-2 tiêu chí 1, KB-32: chữ lớn làm thẻ cao thêm, ảnh ≥ 120 dp, các phần trong thẻ | Compose UI test | `NativeAdFontScaleTest.medium_partsInsideCard_mediaAtLeast120_fontScales` (mới, thay test cũ ở mục 3.5): cỡ chữ 1.0, 1.3, 1.5, 2.0, cả `ctaLarge`, `longFullNativeAdAssets`: thẻ = `mediumNativeHeight` (dung sai 1 dp); ảnh ≥ 120 dp; ảnh + khối nội dung = thẻ; CTA nằm trong thẻ, đáy cách đáy thẻ 6 ± 1 dp; tiêu đề, nhãn "Ad", chữ CTA không bị cắt (`isLineEllipsized` của CTA false, tiêu đề còn ít nhất 4 ký tự trước dấu ba chấm); khung chờ cùng chiều cao |
| KB-2 tiêu chí 2: giữ chỗ ở chữ lớn | Compose UI test | `NativeAdCardTest.mediumPlaceholder_twoTier_matchesCard` (mới): khối ảnh 144 (132 với `ctaLarge`) từ mép trên, khối icon 44 dp, khối CTA rộng 355 cao 44 (56); ở cỡ chữ 2.0 khung chờ cao bằng thẻ |
| KB-3: dòng medium trong danh sách | Compose UI test | `AdGalleryScreenTest` dòng medium 282 dp (mục 3.5) |
| Giao diện medium | Screenshot test | `NativeAdCard_medium_light`, `_dark`, `NativeAdCard_medium-cta-large_light`, `NativeAdPlaceholder_medium_light`, `NativeAdCard_font-scale-2_light` (ghi lại) |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `AdGalleryScreen_default_small`, `AdGalleryScreen_inlist_small` (ghi lại) |
| Đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, ảnh so sánh, rà soát độc lập theo mục 7 của mẫu thiết kế | `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light` (ghi lại); `AdGalleryScreen_default-more_light` ghi lại (không có ảnh thiết kế) |
| Ảnh thiết kế không đổi ở ticket này | Không chạy | Ảnh thiết kế của ticket khác (T-109): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`. Ảnh thiết kế của ticket khác (T-013, T-053): `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |
| Quảng cáo thật | Kiểm tra thủ công (máy thật: `MediaView` và AdChoices của SDK không có trên JVM) | Trên bản staging, màn xem thử quảng cáo: medium có ảnh thật lấp 144 dp không kéo giãn, AdChoices ở góc trên phải trên ảnh, chạm sao mở quảng cáo; đặt cỡ chữ 200%: ảnh không dưới 120 dp, thẻ cao thêm, chữ CTA đủ |

Lệnh so ảnh thiết kế (ảnh của ads nằm ở `:app`, không ở `feature/ads`):

```
java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens app/src/test/screenshots ${TMPDIR:-/tmp}/t110-compare-app
```

Chỉ chép ba ảnh `AdGalleryScreen_default_light_compare.png`, `AdGalleryScreen_default_dark_compare.png`, `AdGalleryScreen_inlist_light_compare.png` vào `docs/features/ads/design/review/`.

**Ngoại lệ viết test trước:** tài liệu (`ads-business-rules.md`, `ads-guide.md`, README thiết kế, ảnh `review/`), chú thích trong `NativeAdCardScreenshotTest.kt`, và ảnh chuẩn ghi lại (ảnh chuẩn chỉ có sau khi code đổi; câu kích thước của chúng có test UI viết trước ở trên).

### 4.1. Chạy thử

Không cần (E dưới 2): đo dòng chữ cho chiều cao thẻ là cách của `stepCardHeight` (plan T-045) và `smallNativeHeight` (plan T-109); bo góc riêng từng góc là API Compose có sẵn.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi kích thước hiển thị của native medium, mục 1 điều 1 của risk-scoring.md); module core dùng chung `:core:ads` (điều 7).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai module (`:core:ads`, `:app`), 11 ảnh chuẩn; API công khai giữ chữ ký (`NativeAdCard`, `NativeAdSlot`, `NativeAdPlaceholder`) |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed` (câu hỏi 66), thiết kế T-108 có số chi tiết ở cỡ chữ 1.0; chữ lớn chỉ có quy tắc (cao thêm khi ảnh dưới 120 dp); hai điểm chờ người duyệt (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Kích thước, vị trí, cỡ chữ có test Robolectric; ảnh thật và AdChoices của SDK kiểm tay |
| D. Ảnh hưởng chéo | 2 | Medium dùng ở khe, ô, hàng trong danh sách của mọi app; `NativeAdCard.kt` chung với native nhỏ (tách hàm dùng chung) |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn (`smallNativeHeight`, `stepCardHeight`) |
| **Tổng** | 7 | **Mức: Cao** (do điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt` (cả native nhỏ qua `NativeContentBlock`, `SmallTexts`), có thể `NativeAdSlot.kt`, `NativeAdCell.kt`, `InListAds.kt` của `:core:ads`; `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`; app tạo từ mẫu nhận medium mới qua `template-sync.sh`, màn nào đặt medium cạnh nội dung sẽ thấy nội dung bên dưới dịch lên 70 tới 150 dp.
- Task khác trong sprint có thể xung đột: T-109 cùng sửa `NativeAdCard.kt`, `NativeAdCardTest.kt`, `NativeAdFontScaleTest.kt`, `NativeAdViewBindingTest.kt`, `NativeAdSlotTest.kt`, `NativeAdSlot.kt`, `NativeAdCardScreenshotTest.kt`, `AdGalleryScreenTest.kt`, ảnh `AdGalleryScreen_default_*`, `AdGalleryScreen_default-more_light`, `AdGalleryScreen_inlist_*`, `NativeAdCard_font-scale-2_light`, ba ảnh `review/` của gallery, hai tài liệu luật và README thiết kế.
- Đề xuất thứ tự chạy: T-109 trước, T-110 ngay sau trong cùng danh sách chạy (`depends_on: [T-109]`).

## 7. Điều kiện dừng riêng

- Code trên nhánh chính chưa có phần của T-109 (`smallNativeHeight`, `nativeAdHeight`, `SmallLayout` hai tầng): dừng, không tự làm phần của T-109.
- Ảnh chuẩn ngoài mục 3.2 đổi khi ghi lại (đặc biệt `NativeAdCard_small_*`, `NativeAdPlaceholder_small_light`, `BottomAdBar_*`, `CollapsibleNativeAd_*`, `SampleScreen_*`, `AdGalleryScreen_collapsible-*`, `AdGalleryScreen_grid_*`, `AdGalleryScreen_pager_*`): dừng, vì việc tách `NativeContentBlock` đã đổi hình native nhỏ hay một màn khác dựa vào medium mà plan chưa thấy.
- Medium ở cỡ chữ 1.0 khác 250 dp ở bất kỳ khung nào của mục 4: dừng, không chỉnh hằng số cho khớp.

## 8. Câu hỏi cho người duyệt

1. **Bỏ đoạn mô tả hai dòng của medium.** Thiết kế T-108 (như JBase) chỉ có một dòng dưới tiêu đề: nhà quảng cáo, không có thì mô tả. Quảng cáo có cả nhà quảng cáo và mô tả thì mô tả không hiện ở đâu (asset body vẫn đăng ký với SDK khi nó là dòng hiện ra; khi không hiện thì không đăng ký, như native nhỏ hôm nay). AdMob không bắt buộc hiện body. Đề xuất: theo thiết kế.
2. **Không có điểm sao thì chữ lấy cả dòng** (223 → 299 dp), thay vì chừa 70 dp trống ở cuối dòng. Chiều cao không đổi, chỉ chiều ngang của dòng chữ. Đề xuất: chữ lấy cả dòng.
3. **Khung chờ medium cao theo cỡ chữ** (hôm nay giữ chiều cao thiết kế, chỉ thẻ thật cao thêm, nên nội dung nhảy khi quảng cáo về ở cỡ chữ lớn). Đây là hệ quả của câu hỏi 66 (D-04 cho mọi native); nêu để anh biết. Đề xuất: theo cỡ chữ.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:**

## 10. Nhật ký thực hiện
