---
ticket: T-045
title: CTA của native giữ trọn chữ khi cỡ chữ hệ thống lớn
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 8M           # cỡ S, số gợi ý 4.1M (scripts/token-usage.py estimate, 2026-10-26); cộng bố cục medium bằng cách đo của T-044, ma trận cỡ chữ cho tám dạng native, một ảnh chuẩn mới
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-045: CTA CỦA NATIVE GIỮ TRỌN CHỮ KHI CỠ CHỮ HỆ THỐNG LỚN

## 1. Tóm tắt

Ở cỡ chữ hệ thống 2.0, chữ `labelLarge` của CTA cần một dòng cao 37,3 dp (small: 38,7 dp), cao hơn khung 34 hay 36 dp, nên bị cắt ở small, thẻ dọc, thẻ ở bước, lớp thu gọn và thẻ đầy đủ. Medium nặng hơn: thẻ cố định 410 dp ép CTA còn 32 dp ở cỡ 1.3 và 10,7 dp ở 2.0 (mục 4.1). Plan cho khung CTA cao **ít nhất** số của thiết kế (cao thêm vừa một dòng chữ khi cần), và đổi medium sang cách đo "media co trước, không dưới 120 dp" mà T-044 làm cho thẻ `FULL`. Chạy thử phần sửa: ở cỡ chữ 1.0 và 1.3 mọi khung CTA giữ đúng số thiết kế, ở 2.0 chữ vừa khung ở cả tám dạng native, medium vẫn 410 dp, 1364 test JVM qua và không ảnh chuẩn nào đổi.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-26), cùng bảng kích thước mục 5.7 và luật O-08 của `docs/ops/ads-business-rules.md`. Lỗi gốc: plan T-044, mục 4.1 ghi chú cuối và mục 8 câu 2 ("chữ của CTA ở cỡ 2.0 cao hơn khung 36 dp, ở mọi cỡ native có CTA cố định"; trả lời: để một ticket theo sau).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices… | "Thấy nút CTA" gồm thấy trọn chữ của nó. Ở mọi cỡ chữ hệ thống mà máy cho đặt (1.0 tới 2.0), chữ CTA của native small không bị cắt: `TextLayoutResult.didOverflowHeight` là `false` và chữ nằm trong khung CTA. Khung small vẫn 80 dp. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp… Các tiêu chí giữ chỗ… như KB-1. | Medium: CTA đủ 44 dp (56 dp với `ctaLarge`), hàng sao 16 dp, mô tả 2 dòng ở mọi cỡ chữ tới 2.0; media co trước, không dưới 120 dp (`NativeAdDimens.MinMediaSize`, do T-044 tạo); thẻ vẫn 410 dp (422 dp) tới cỡ 2.0 nên khung chờ vẫn bằng thẻ. Chỉ khi media chạm sàn (cỡ chữ giả lập 3.0) thì thẻ cao thêm, như luật của thẻ `FULL` ở T-044. |
| KB-4 | Khi vào màn hình thì quảng cáo mở ở đáy, cao 250 dp… | Lớp thu gọn giữ 250 dp; chữ CTA không bị cắt ở mọi cỡ chữ tới 2.0. |
| KB-20 | Bước mà spec của app chọn có native cao 250 dp hoặc native toàn trang. | Thẻ ở bước (`STEP`) giữ 250 dp, chữ CTA không bị cắt. Trang toàn trang (CTA 52 dp, bố cục của T-041) và native toàn màn hình sau inter (KB-16, cùng CTA 52 dp) đã đủ chỗ ở 2.0 (chữ 37,3 dp trong 52 dp): task không sửa, chỉ thêm test canh. |
| KB-21 | Mỗi ô… có một quảng cáo riêng, dạng thẻ dọc hoặc medium theo khai báo của vị trí. | Thẻ dọc (`CARD`, 180 x 300 dp như ví dụ README) giữ kích thước ô, chữ CTA không bị cắt; medium trong ô theo dòng KB-2. |

**Đối chiếu thiết kế và luật.**

- Mục 5.7 và README thiết kế (mục 3, dòng CTA) ghi CTA cao 34, 36, 44, 56, 52 dp. Đó là số ở cỡ chữ 1.0, nơi chữ cao 20 dp: plan giữ đúng các số này ở 1.0 và 1.3 (mục 4.1), chỉ cho khung cao thêm khi chữ cần, nên không mâu thuẫn. Plan thêm một câu vào README dòng CTA.
- O-08 (nhãn "Ad" luôn thấy được): plan không làm nhãn "Ad" của các thẻ nhỏ đi; số đo ở mục 4.1 thấy nhãn đủ cao ở mọi thẻ của `NativeAdCard.kt` sau phần sửa. Ở native toàn màn hình sau inter, đầu 48 dp ép nhãn "Ad" còn 13,7 dp ở 1.3 và 2 dp ở 2.0. Đây là lỗi khác, ngoài phạm vi ticket: câu hỏi 2.
- KB-2 "vùng ảnh ít nhất 120 x 120 dp": thẻ ở bước và lớp thu gọn (media lấp phần còn lại của 250 dp) có media 101 dp và 99 dp ở cỡ 2.0 ngay trên code hiện tại; phần sửa của task bớt thêm 1,3 dp. Câu hỏi 3.
- Vùng chạm 44/48 dp: quy tắc vùng chạm tối thiểu 48 dp (`docs/ops/design-workflow.md` mục 8.5) áp cho nút của app. CTA của native là tài sản của quảng cáo, SDK nhận chạm qua view của nó, cỡ theo bảng 5.7 đã duyệt (34 dp ở small, 36 dp ở thẻ, đều dưới 44 và 48 dp). Task không đổi số này ở cỡ 1.0, chỉ làm khung cao thêm khi chữ lớn, nên vùng chạm không nhỏ đi ở đâu. Câu hỏi 4.

Thiết kế tham chiếu: `features/ads/design/README.md` (mục 3: dòng CTA quảng cáo, dòng Media có luật "media co trước, không dưới 120 dp" do T-044 ghi), `design/context/AdGalleryScreen_default_light.md` (medium: đầu 56, media 196, mô tả 40, CTA 44, ba khoảng 12 dp). Thiết kế chỉ vẽ cỡ chữ 1.0; bố cục khi chữ lớn không có trên Figma. Task không vẽ màn hình mới.

ADR liên quan: ADR-0008 (hệ thống thiết kế), ADR-0013 (quảng cáo qua `:core:ads`), ADR-0026 (nghiệp vụ PARTNER), ADR-0029 (phần ngoài mục 3.2, test cũ).

## 3. Giải pháp

### 3.1. Cách làm

Plan dựa trên code sau khi T-044 và T-041 đã merge. Các tên "do T-044 tạo": `FontScaleTestSupport.kt` (`WithFontScale`), `NativeAdDimens.MinMediaSize` (120 dp), hàm thuần `fullMediaHeight(inner, others, floor)`, `FullLayout` bằng `Layout` riêng, `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales`, `FullMediaHeightTest`. "Do T-041 tạo": `FlowStepPageLayout` (trang toàn trang của luồng đi qua `NativeFullScreenStepContent`).

**A. Khung CTA cao vừa chữ (mọi native của `NativeAdCard.kt` và lớp thu gọn).**

1. `NativeAdCallToAction` (`NativeAdCard.kt`): khung trong đổi `.height(height)` thành `.heightIn(min = height)`. Chữ vẫn một dòng, `labelLarge`, cắt bằng "…" khi quá dài theo chiều ngang. Khi dòng chữ cao hơn `height` (chỉ từ cỡ chữ khoảng 1.8, mục 4.1), khung cao bằng dòng chữ. KDoc: "cao ít nhất [height]; cỡ chữ lớn thì cao vừa một dòng chữ (plan T-045)".
2. Nơi gọi bọc CTA trong một chiều cao cố định thì đổi sang `heightIn(min = …)`, để khung trong được cao thêm:
   - `CardLayout`: `ctaModifier` `height(NativeAdDimens.CallToActionHeight)` thành `heightIn(min = …)`;
   - `StepLayout`: `height(NativeAdDimens.StepCallToActionHeight)` thành `heightIn(min = …)`;
   - `FullLayout` (do T-044 tạo): `ctaModifier` của CTA thành `heightIn(min = NativeAdDimens.CallToActionHeight)`. `Layout` của T-044 đo CTA với chiều cao không giới hạn, nên media co thêm 1,3 dp ở cỡ 2.0 (134 thành 132,7 dp), thẻ vẫn 418 dp;
   - `SmallLayout`, `MediumLayout`, `CollapsibleNativeAdLayout` truyền `height` vào `NativeAdCallToAction`, không bọc thêm: không cần sửa. `Spacer(ctaModifier)` khi quảng cáo không có CTA vẫn giữ đúng số thiết kế.
3. Khung chờ (`NativeAdPlaceholderLayout`, `CardPlaceholderBlocks`, `MediumPlaceholderBlocks`) không đổi: khối chờ không có chữ.
4. Các thẻ cố định (small 80 dp, thẻ dọc theo ô, thẻ ở bước 250 dp, lớp thu gọn 250 dp) giữ chiều cao: CTA cao thêm tối đa 4,7 dp ở 2.0 (small, căn giữa hàng 80 dp), hay 1,3 dp (thẻ 250 dp và thẻ dọc, media `weight(1f)` nhường chỗ).

**B. Medium giữ đủ các phần khi chữ lớn (KB-2, KB-21).**

1. Trên code hiện tại `MediumLayout` là `Column` 410 dp với năm phần cao cố định (mục 4.1). Đầu (56 dp ở 1.0) cao thêm theo chữ (68 dp ở 1.3, 89,3 dp ở 2.0), nên `Column` ép phần cuối: CTA còn 32 dp ở 1.3, 10,7 dp ở 2.0; mô tả còn 1 dòng.
2. Đổi `MediumLayout` sang cùng cách đo của `FullLayout` (do T-044 tạo):
   - đo đầu (`NativeAdHeader(starRow = true)`), mô tả, CTA với chiều cao không giới hạn, rộng cố định;
   - chiều cao media = `fullMediaHeight(inner, others, NativeAdDimens.MinMediaSize)`, với `inner` = `heightOf(MEDIUM, ctaLarge)` trừ lề trên `MediumInnerTop` (25 dp) và lề dưới `MediumInnerBottom` (13 dp), tức 372 dp (384 dp với `ctaLarge`), `others` = tổng ba phần kia cộng ba khoảng 12 dp;
   - media đo với chiều cao cố định đó (`NativeAdMediaArea` có nền mờ như hôm nay), xếp bốn phần từ trên xuống cách nhau 12 dp; chiều cao của `Layout` = `others + media`.
   - Phiên tách phần đo và xếp chung của `FullLayout` và `MediumLayout` thành một hàm `@Composable private` trong `NativeAdCard.kt` (tên gợi ý `MediaFirstColumn(innerHeight, header, media, below)`), để không chép hai lần. Tách thì `FullLayout` phải giữ đúng hành vi: test của T-044 và ảnh `NativeAdCard_full_*` không đổi. Không tách được gọn thì giữ hai `Layout` riêng (mục 3.4).
   - `fullMediaHeight` giữ tên (do T-044 tạo, có test riêng), KDoc thêm "dùng cho cả medium".
3. Khung thẻ: trong `NativeAdLayout`, cỡ `MEDIUM` đổi `height(heightOf(size, ctaLarge))` thành `heightIn(min = heightOf(size, ctaLarge))`, như T-044 làm cho `FULL`. Các cỡ khác giữ `height`.
4. Mô tả của medium: `height(NativeAdDimens.BodyHeight)` thành `heightIn(min = NativeAdDimens.BodyHeight)`, giữ `maxLines = 2`.
5. Số đo của phần sửa khi chạy thử (mục 4.1, bản thử viết `Layout` riêng cho medium): medium 410 dp ở 1.0, 1.3, 2.0; media 196 dp (1.0), 170 dp (1.3), 128 dp (2.0); CTA 44 dp; hàng sao 16 dp; mô tả 2 dòng. Ở 3.0 media 120 dp, thẻ 502 dp (504 dp với `ctaLarge`). `ctaLarge` cho cùng số media, CTA 56 dp.
6. `NativeAdSlot` giữ chỗ bằng `Spacer(height(heightOf(size, ctaLarge)))`: tới cỡ 2.0 thẻ medium vẫn bằng số này, nên chỗ giữ vẫn bằng thẻ (KB-1, giữ chỗ). Không sửa.

**C. Tài liệu.**

- `docs/features/ads/design/README.md`, mục 3, dòng "CTA quảng cáo": thêm "số trên là ở cỡ chữ 1.0; chữ lớn thì CTA cao vừa một dòng chữ, không thấp hơn số trên". Dòng Media (luật của T-044) không đổi; dòng "Khung native…" không đổi.
- `docs/ops/ads-guide.md`, mục 4, bảng số đo: dòng CTA ghi "cao ít nhất …; chữ lớn thì cao vừa chữ"; ô Khung của medium ghi "410 dp tới cỡ chữ 2.0, media co trước, không dưới 120 dp (README thiết kế, dòng Media)". Cột "Đầy đủ (`FULL`)" do T-044 sửa: chỉ thêm chữ "ít nhất" vào ô CTA.
- `docs/ops/ads-business-rules.md` không đổi: bảng 5.7 là số của AdsKit ở cỡ chữ mặc định.

Mẫu làm theo: `Layout` riêng và sàn media của `FullLayout` (T-044); test cỡ chữ bằng `WithFontScale` của `full_partsKeepSize_insideCard_textsAndFontScales` (T-044); đọc `TextLayoutResult` qua `SemanticsActions.GetTextLayoutResult` như hàm `textStyle` của `NativeAdCardTest`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `NativeAdCallToAction` `heightIn(min = height)`; `ctaModifier` của `CardLayout`, `StepLayout`, `FullLayout` dùng `heightIn`; `MediumLayout` đo theo media co trước, sàn `MinMediaSize`, mô tả `heightIn`; khung thẻ `MEDIUM` `heightIn(min = …)`; hàm đo chung với `FullLayout` (nếu tách); KDoc của `NativeAdCallToAction`, `MediumLayout`, `NativeAdDimens.MediumHeight`, `fullMediaHeight` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCtaFontScaleTest.kt` | Ma trận cỡ chữ của CTA cho tám dạng native, bố cục medium khi chữ lớn (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | `full_partsKeepSize_insideCard_textsAndFontScales` (do T-044 tạo): câu "CTA cao 36 dp" thành "CTA cao 36 dp ở 1.0 và 1.3; ở 2.0 cao ít nhất 36 dp và chữ CTA không tràn" (mục 4, dòng 7; câu hỏi 1). Các câu khác của test giữ nguyên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Ảnh mới `fontScale2_light`: small, medium, thẻ ở bước xếp dọc, bọc `WithFontScale(2f)` |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_light.png` | Ảnh chuẩn mới, khung `w411dp-h891dp-xxhdpi` (ghi bằng `scripts/check.sh`) |
| Sửa | `docs/features/ads/design/README.md` | Mục 3, dòng "CTA quảng cáo": số ở cỡ chữ 1.0, chữ lớn thì CTA cao vừa chữ |
| Sửa | `docs/ops/ads-guide.md` | Mục 4, bảng số đo: dòng CTA "ít nhất", ô Khung của medium khi chữ lớn |

**Dùng, do T-044 tạo (không tạo lại):** `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FontScaleTestSupport.kt` (`WithFontScale`), `NativeAdDimens.MinMediaSize`, `fullMediaHeight`, `Layout` riêng của `FullLayout`, `FullMediaHeightTest` (không sửa: hàm không đổi). **Dùng, do T-041 tạo:** bố cục trang toàn trang qua `NativeFullScreenStepContent` (test canh ở mục 4, dòng 3; không sửa `NativeFlowStepLayout.kt`, `NativeFullScreenAdScreen.kt`).

Ảnh chuẩn: không ghi lại ảnh nào ở cỡ chữ 1.0. Chạy thử với phần sửa áp tạm: `:core:ads:verifyRoborazziDebug`, `:app:verifyRoborazziStagingDebug`, `:feature:sample:verifyRoborazziDebug`, `:feature:loading:verifyRoborazziDebug` đều qua (mục 4.1). Một ảnh mới ở cỡ chữ 2.0 (dòng trên). Không có ảnh `_small` cho ảnh mới: đây là ảnh thành phần như các ảnh `NativeAdCard_*` sẵn có, không phải màn hình (L-028 áp cho màn hình).

**Khung rỗng vào commit test của bước 3 (L-025):** không cần. Test mới chỉ dùng API đã có (`NativeAdCard`, `CollapsibleNativeAdCard`, `NativeFullScreenAdScreen`, `NativeFullScreenStepContent`, `WithFontScale`, tag sẵn có), nên biên dịch được và đỏ đúng lý do trên code trước phần sửa. Câu sửa trong `full_partsKeepSize_insideCard_textsAndFontScales` vào commit test (nó qua cả trước và sau phần sửa ở 1.0 và 1.3; ở 2.0 đỏ trước phần sửa vì chữ tràn, như các test mới).

**Nơi đọc file bị đổi (L-020).**

- `NativeAdCallToAction` được gọi ở `SmallLayout`, `MediumLayout`, `CardLayout`, `StepLayout`, `FullLayout` (`NativeAdCard.kt`) và `CollapsibleNativeAdLayout` (`CollapsibleNativeAd.kt`). Mọi nơi gọi đều nằm trong ma trận test của mục 4. `CollapsibleNativeAd.kt` không cần sửa (truyền `height`, không bọc thêm).
- `MediumLayout` được vẽ qua `NativeAdSlot` (medium), `NativeAdCell` (ô medium), `BottomAdBar` không dùng medium; mọi nơi đi qua `NativeAdLayout`, nên khung `heightIn` áp chung. Ô cố định kích thước của lưới vẫn ép thẻ như hôm nay; tới cỡ 2.0 thẻ không vượt 410 dp nên không khác.
- Ảnh chuẩn đọc bởi `scripts/check.sh` (`verifyRoborazzi*`, đòi ảnh của mỗi `captureRoboImage` có trong cây) và `scripts/test-summary.sh` (đếm ảnh). `scripts/design-compare.sh` chỉ đọc ảnh của `feature/<tên>`. Không test hay workflow nào đọc tên ảnh mới.
- `NativeFullScreenAdScreen.kt` (CTA 52 dp riêng, `AdCallToAction`) không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay tần suất. Đổi cách vẽ native small, medium, đầy đủ, thẻ dọc, thẻ ở bước và lớp thu gọn khi cỡ chữ hệ thống lớn (điểm chặn 1); ở cỡ chữ 1.0 không đổi.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp dùng trong code: `androidx.compose.foundation.layout.heightIn` (Compose foundation), `androidx.compose.ui.layout.Layout`, `androidx.compose.ui.unit.Constraints` (Compose UI), đều đã có trên classpath biên dịch `main` của `:core:ads` (T-044 dùng; chạy thử ở mục 4.1 biên dịch qua). Test dùng `TextLayoutResult`, `SemanticsActions.GetTextLayoutResult`, `WithFontScale` (Compose UI test, đã có ở `:core:ads`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** (1) tách phần đo chung của `FullLayout` (code của T-044 mà plan này chưa thấy trên nhánh chính) có thể đổi hành vi của thẻ `FULL`; (2) khung CTA `heightIn` có thể làm lệch một ảnh chuẩn trên máy CI dù máy dev không lệch.
- **Điều kiện chuyển:** (1) test của T-044 (`full_*`, `FullMediaHeightTest`) đỏ sau khi tách, hai lần sửa không xanh; (2) `verifyRoborazzi*` trên CI báo lệch ảnh có CTA ở cỡ chữ 1.0.
- **Cách làm thay:** (1) không tách: `MediumLayout` có `Layout` riêng của nó, cùng cách đo (bản này đã chạy thử, mục 4.1); (2) giữ khung CTA cố định `height(height)` và chỉ khi dòng chữ cao hơn khung (`labelLarge.lineHeight` đổi sang dp theo `LocalDensity` lớn hơn `height`) thì vẽ chữ với `lineHeight = TextUnit.Unspecified` (một dòng cao bằng cỡ chữ của font, khoảng 31,5 dp ở 2.0), nên ở cỡ 1.0 chữ vẽ y như hôm nay. Test ở mục 4 giữ nguyên: cách này cũng cho `didOverflowHeight == false` và khung không thấp hơn số thiết kế. Bố cục medium (phần B) vẫn cần.

## 4. Kiểm thử

Test viết trước code, đỏ đúng lý do trên code hiện tại (số đo ở mục 4.1). Khung test tái hiện lỗi gốc: `w411dp-h891dp-xxhdpi`, thẻ rộng `CARD_WIDTH` 379 dp như `NativeAdCardTest`, thẻ dọc 180 x 300 dp như README, cỡ chữ đặt bằng `WithFontScale(scale) { AppTheme { … } }` (do T-044 tạo), đổi `scale` và bộ chữ qua `mutableStateOf` trong một lần `setContent`.

**`NativeAdCtaFontScaleTest`** (`:core:ads`, Robolectric, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`). Hàm phụ trong file `assertCtaFits(label, ctaTag, frameTag, designHeight, frameHeight)`: đọc `getUnclippedBoundsInRoot()` (`useUnmergedTree = true`) của khung CTA và khung thẻ, và `TextLayoutResult` của nút chữ CTA (`onNodeWithText(callToAction)`). Khẳng định, dung sai 0,5 dp, thông báo ghi dạng native, cỡ chữ, bộ chữ:
- (a) `didOverflowHeight == false`;
- (b) nút chữ nằm trong khung CTA;
- (c) khung CTA cao ít nhất `designHeight`; ở cỡ 1.0 và 1.3 cao đúng `designHeight`;
- (d) khung thẻ cao đúng `frameHeight`; khung CTA nằm trong khung thẻ.

Bộ chữ: `sampleNativeAdAssets` và `longFullNativeAdAssets` (CTA "Install now and get the full version", dài hơn mọi khung) cho các thẻ; `FakeNativeAdAssets.Design` và `Longest` cho toàn màn hình.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, KB-2, KB-20, KB-21: chữ CTA trọn trong khung ở các thẻ của `NativeAdCard` | Compose UI test | `cardSizes_ctaTextFits_fontScales`: với `SMALL` (34 dp, thẻ 80), `MEDIUM` (44, thẻ 410), `MEDIUM` `ctaLarge` (56, thẻ 422), `FULL` (36, thẻ 418), `CARD` trong `Modifier.size(180.dp, 300.dp)` (36, thẻ 300), `STEP` (36, thẻ 250); cỡ chữ 1.0, 1.3, 2.0; hai bộ chữ; gọi `assertCtaFits`. **Lý do thất bại trên code hiện tại (mục 4.1):** ở 2.0 chữ tràn khung ở `SMALL` (dòng 38,7 dp trong 34 dp), `CARD`, `STEP` (37,3 trong 36), `FULL` (khung 36 dp của T-044, dòng 37,3); `MEDIUM` khung bị ép 32 dp ở 1.3 và 10,7 dp ở 2.0; `ctaLarge` 22,7 dp ở 2.0. **Đối chứng (L-055):** ở 1.0 mọi dạng qua trên code hiện tại (cùng test, chỉ cỡ chữ đổi); sau phần sửa, tạm đổi lại `heightIn(min = height)` của `NativeAdCallToAction` thành `height(height)`: test đỏ ở 2.0 (`SMALL`, `CARD`, `STEP`); hoàn lại |
| KB-4: lớp thu gọn | Compose UI test | `collapsibleLayer_ctaTextFits_fontScales`: `CollapsibleNativeAdCard(assets, onCollapse = {}, Modifier.width(379.dp))`, khung `CollapsibleNativeAdTestTags.AD` 250 dp, CTA 36 dp, cùng ba cỡ chữ và hai bộ chữ. **Lý do thất bại:** ở 2.0 dòng 37,3 dp trong khung 36 dp. **Đối chứng:** 1.0 qua trên code hiện tại |
| KB-20, KB-16: native toàn màn hình (CTA 52 dp) không bị cắt (test canh) | Compose UI test | `fullScreen_ctaTextFits_fontScales`: `NativeFullScreenAdScreen(assets, onClose = {}, media = { FakeMedia() }, adChoices = { FakeAdChoices() })` và `NativeFullScreenStepContent(assets, …)` trong `Box(fillMaxWidth().height(484.dp))` (trang toàn trang theo bố cục cột của T-041), tag `NativeFullScreenAdTestTags.CTA`, CTA 52 dp, ba cỡ chữ, hai bộ chữ; khẳng định (a), (b), (c). Qua trên code hiện tại (chữ 37,3 dp trong 52 dp ở 2.0, mục 4.1): test canh, không có phần sửa. **Đối chứng (L-055):** tạm đặt `NativeFullScreenAdDimens.CallToActionHeight = 36.dp`: test đỏ ở 2.0 (dòng 37,3 dp); hoàn lại |
| KB-2: medium giữ CTA, hàng sao, mô tả; media co trước, không dưới 120 dp | Compose UI test | `medium_partsKeepSize_mediaShrinksFirst_fontScales`: `MEDIUM` và `MEDIUM` `ctaLarge`, hai bộ chữ (cả hai `starRating = 4.0`), cỡ 1.0, 1.3, 2.0. Khẳng định: thẻ 410 dp (422 dp); CTA 44 dp (56 dp); `STAR_ROW` 16 dp; đáy CTA cách đáy thẻ 13 dp (12 dp lề cộng 1 dp viền); mô tả đúng 2 dòng (`lineCount == 2`); media (`MEDIA`) cao ít nhất 120 dp, đỉnh media không trên đáy hàng sao; ở 1.0 media đúng 196 dp (số thiết kế). **Lý do thất bại trên code hiện tại:** ở 1.3 CTA 32 dp, mô tả 1 dòng; ở 2.0 CTA 10,7 dp, đáy CTA vẫn trong thẻ vì `Column` ép. **Đối chứng:** 1.0 qua trên code hiện tại; sau phần sửa, tạm đo media bằng `Constraints.fixed(width, NativeAdDimens.MediaHeight)` thay `fullMediaHeight(...)`: test đỏ ở 1.3 (thẻ cao hơn 410 dp); hoàn lại |
| KB-2: sàn 120 dp của media medium giữ bằng code | Compose UI test | `medium_mediaFloor120dp_cardGrows_atFontScale3`: `WithFontScale(3f)`, chữ dài, `MEDIUM`. Khẳng định: media đúng 120 dp; CTA cao ít nhất 44 dp và chữ không tràn; thẻ cao hơn 410 dp (chạy thử: 502 dp). **Lý do thất bại:** thẻ ép 410 dp, CTA bị ép. **Đối chứng:** sau phần sửa, tạm bỏ sàn (media = `inner - others`): media dưới 120 dp, test đỏ; hoàn lại |
| KB-1, KB-2: khung chờ vẫn bằng thẻ ở cỡ chữ 1.0 | Compose UI test (đã có) | `NativeAdCardTest.placeholder_matchesCardHeight_forEachSize`, `full_placeholderHeight_equalsCardHeight_shortAndLongTexts`, `medium_is410dpHigh`, `small_is80dpHigh`: giữ nguyên, phải xanh |
| KB-2 (thẻ `FULL`) ở cỡ chữ lớn: test của T-044 theo luật CTA mới | Compose UI test (sửa) | `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales` (do T-044 tạo): câu "CTA cao 36 dp" thành: ở 1.0 và 1.3 cao 36 dp; ở 2.0 cao ít nhất 36 dp và `didOverflowHeight == false` của chữ CTA. Câu về thẻ 418 dp, hàng sao 16 dp, đáy hàng sao cách đáy thẻ 13 dp, mô tả 2 dòng, media ít nhất 120 dp và 200 dp ở 1.0 giữ nguyên. Đây là sửa test có trong plan (đổi luật CTA cố định thành "ít nhất"), không phải nới để qua (câu hỏi 1) |
| Cỡ chữ 1.0 không đổi (thiết kế) | Screenshot test (đã có) | Mọi ảnh `NativeAdCard_*`, `NativeAdPlaceholder_*`, `CollapsibleNativeAd_*`, `BottomAdBar_*`, `NativeFlowStep_*`, `NativeFullScreenAdScreen_*` (`core/ads`), `AdGalleryScreen_*` (`app`), `SampleScreen_*` (`feature/sample`) không ghi lại; `verifyRoborazzi*` của `scripts/check.sh` phải qua (chạy thử với phần sửa: qua) |
| Giao diện ở cỡ chữ 2.0 | Screenshot test (mới) | `NativeAdCardScreenshotTest.fontScale2_light`: `Column` với `SMALL`, `MEDIUM`, `STEP` (379 dp, cách nhau 16 dp, `sampleNativeAdAssets`), bọc `WithFontScale(2f) { OnScreen(darkTheme = false) { … } }`: `captureRoboImage("src/test/screenshots/NativeAdCard_font-scale-2_light.png")`. Ảnh tĩnh (thẻ có quảng cáo, không có khung chờ chạy hiệu ứng), nên chụp như các ảnh sẵn có (L-048 không áp dụng). Người duyệt PR xem ảnh: chữ CTA trọn, nhãn "Ad" đọc được |
| Đúng thiết kế | Không có ảnh thiết kế riêng cho task (thiết kế chỉ vẽ cỡ chữ 1.0; ảnh ở 1.0 không đổi). Ảnh thiết kế của ticket khác (T-014, T-027, T-030, T-036): `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_default_light`, `AdGalleryScreen_inlist_light`, `NativeFullScreenAdScreen_default_dark`, `NativeFullScreenAdScreen_default_light`, `SampleScreen_ad-loading_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light` | Không ghi lại ảnh nào |
| Native ở cỡ chữ 2.0 trên máy thật, quảng cáo thật | Kiểm tra thủ công (máy thật: khác, cần đổi cỡ chữ trong cài đặt máy; `device-session` và `ax.sh` không đổi cài đặt hệ thống) | Bản staging, Cài đặt máy: cỡ chữ lớn nhất (Android 14 trở lên: 200%). Màn xem thử: native small, medium, "Grid ads", "Flow steps" (thẻ 250 dp), thanh đáy với lớp thu gọn. Chữ CTA đọc trọn, bấm CTA mở quảng cáo; medium vẫn đủ CTA, hàng sao |

**Ngoại lệ viết test trước:** sửa `docs/features/ads/design/README.md` và `docs/ops/ads-guide.md` (chỉ tài liệu). Ảnh chuẩn mới `NativeAdCard_font-scale-2_light.png` được ghi sau phần sửa (ảnh chỉ có nghĩa với code đã sửa); test của ảnh vào commit test, ảnh vào commit code. Người duyệt đồng ý với ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, worktree `claude/plan-T-045` từ nhánh chính `04ae69a` (T-041 đến T-044 chưa merge), bằng test tạm `TempT045CtaTest.kt` không commit (đã xóa, worktree sạch). Test tạm đặt cỡ chữ bằng `CompositionLocalProvider(LocalDensity provides Density(density, fontScale))` (cùng cách của `WithFontScale` mà T-044 sẽ tạo), đo khung CTA, nút chữ CTA (`TextLayoutResult`), khung thẻ, media, nhãn "Ad", tiêu đề, hàng sao, mô tả, cho chín dạng (small, medium, medium `ctaLarge`, đầy đủ, thẻ dọc 180 x 300, thẻ ở bước, lớp thu gọn, toàn màn hình sau inter, trang toàn trang 484 dp), cỡ chữ 1.0, 1.3, 2.0 (và 3.0 sau phần sửa), hai bộ chữ. Lần hai áp tạm phần sửa (khung CTA `heightIn`, `ctaModifier` `heightIn`, `MediumLayout` bằng `Layout` riêng với sàn 120 dp, khung medium `heightIn`), đo lại, rồi chạy cả bộ test JVM và so ảnh. Đã hoàn lại.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TempT045*'`; `./gradlew --continue testDebugUnitTest testStagingDebugUnitTest :core:ads:verifyRoborazziDebug :feature:sample:verifyRoborazziDebug :feature:loading:verifyRoborazziDebug :app:verifyRoborazziStagingDebug` (JDK của Android Studio). Không chạy `scripts/check.sh`: E = 1 (mục 5), không thêm thư viện.
- **Kết quả, code hiện tại (L-029).** Chữ `labelLarge` (14 sp) cao 20 dp ở 1.0, 27 dp ở 1.3, 37,3 dp ở 2.0 (cỡ 26 dp, bảng đổi phi tuyến của Android 14); chữ 13 sp của small: 20; 26,7; 38,7 dp. Hai bộ chữ cho cùng số khung.

  | Dạng | Khung CTA thiết kế | 1.0 | 1.3 | 2.0 |
  | --- | --- | --- | --- | --- |
  | Small (thẻ 80) | 34 dp | 34, chữ 20 | 34, chữ 26,7 | 34, chữ tràn (38,7) |
  | Medium (thẻ 410) | 44 dp | 44 | **32** (bị ép), mô tả 1 dòng | **10,7** (bị ép), chữ tràn |
  | Medium `ctaLarge` (422) | 56 dp | 56 | 44 (bị ép) | **22,7**, chữ tràn |
  | Đầy đủ (418, code trước T-044) | 36 dp | 36 | 36, hàng sao 6 dp | 32,7, chữ tràn (T-044 sửa phần ép, chữ vẫn 37,3 trong 36) |
  | Thẻ dọc (300) | 36 dp | 36 | 36 | 36, chữ tràn (37,3) |
  | Thẻ ở bước (250) | 36 dp | 36 | 36 | 36, chữ tràn (37,3) |
  | Lớp thu gọn (250) | 36 dp | 36 | 36 | 36, chữ tràn (37,3) |
  | Toàn màn hình sau inter | 52 dp | 52 | 52 | 52, chữ 37,3 vừa |
  | Trang toàn trang 484 dp | 52 dp | 52 | 52 | 52, chữ 37,3 vừa |

  Phần khác (không thuộc CTA, ghi cho câu hỏi 2, 3): nhãn "Ad" của toàn màn hình sau inter và trang toàn trang cao 16 dp ở 1.0, 13,7 dp ở 1.3, 2 dp ở 2.0 (đầu cố định 48 dp; T-041 sửa riêng trang toàn trang); media của thẻ ở bước 136, 124,3, 101,3 dp và của lớp thu gọn 134, 122,3, 99,3 dp ở 1.0, 1.3, 2.0; media thẻ dọc 194, 182,3, 159,3 dp. Nhãn "Ad" của các thẻ trong `NativeAdCard.kt` đủ cao ở mọi cỡ (16, 21, 32 dp).
- **Kết quả, phần sửa áp tạm.** Khung CTA ở 1.0 và 1.3 đúng số thiết kế ở mọi dạng; ở 2.0: small 38,7 dp (thẻ 80), thẻ dọc, thẻ ở bước, lớp thu gọn 37,3 dp (thẻ giữ 300, 250, 250 dp; media bớt 1,3 dp), medium 44 dp, `ctaLarge` 56 dp; `didOverflowHeight` `false` ở mọi dạng và cỡ tới 2.0 (ở 3.0 các thẻ vẫn vừa chữ: khung 60 dp; toàn màn hình 52 dp thì tràn ở 3.0, cỡ máy không cho đặt). Medium: thẻ 410 dp ở 1.0, 1.3, 2.0; media 196, 170, 128 dp; hàng sao 16 dp; mô tả 2 dòng (ở 2.0 có "…" ở dòng 2); 3.0: media 120 dp, thẻ 502 dp (`ctaLarge` 504 dp). Thẻ `FULL` chưa có phần sửa của T-044 nên vẫn bị ép ở 2.0 (đúng như T-044 ghi); sau T-044 media 134 dp còn 132,7 dp.
- **Kết quả, bộ test và ảnh với phần sửa áp tạm:** 1373 test JVM (1364 test sẵn có cộng 9 test tạm), 0 lỗi: không test cũ nào đỏ. `verifyRoborazzi` của `:core:ads`, `:app` (staging), `:feature:sample`, `:feature:loading`: qua, không ảnh nào đổi ở cỡ chữ 1.0.
- **Chưa chạy thử:** phần tách hàm đo chung với `FullLayout`, vì `Layout` của T-044 chưa có trên nhánh chính (mục 3.4, điểm 1); bước đối chứng của `fullScreen_ctaTextFits_fontScales` (theo số đo, dòng 37,3 dp sẽ tràn khung 36 dp); `WithFontScale` thật của T-044 (test tạm dùng đúng cách đặt cỡ chữ mà plan T-044 ghi).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi cách vẽ native khi cỡ chữ lớn; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm vị trí, quyền, SDK, sự kiện hay dữ liệu. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước (đổi giao diện quảng cáo ở cỡ chữ lớn).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`): một file code (`NativeAdCard.kt`: khung CTA, ba `ctaModifier`, bố cục medium, khung medium), một file test mới, hai file test sửa, một ảnh mới, hai tài liệu; không đổi API công khai |
| B. Độ rõ của yêu cầu | 1 | Lỗi đo được, T-044 câu 2 đã trả lời; luật media của medium lấy từ luật đã duyệt của T-044; ba câu hỏi ở mục 8 không đổi cách làm chính |
| C. Khả năng kiểm chứng tự động | 1 | Mọi dạng native có test đo ở ba cỡ chữ và ảnh ở 2.0; quảng cáo thật ở cỡ chữ lớn chỉ xem trên máy thật |
| D. Ảnh hưởng chéo | 2 | `NativeAdCard.kt`, `NativeAdCardTest.kt`, README thiết kế, `ads-guide.md` cũng bị T-044 sửa; T-041 sửa README, `ads-guide.md` (mục 6) |
| E. Độ mới của kỹ thuật | 1 | Biến thể của `Layout` riêng và sàn media của T-044; `heightIn` đã dùng nhiều nơi |
| **Tổng** | 7 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt` (mọi native dùng `NativeAdCallToAction`; bố cục medium), `NativeAdCardTest.kt`, `NativeAdCardScreenshotTest.kt`, `docs/features/ads/design/README.md` mục 3, `docs/ops/ads-guide.md` mục 4. `tokens.json`, bảng khóa Remote Config, ảnh chuẩn ở cỡ chữ 1.0 không bị ảnh hưởng.
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **T-044 (chạy trước, thứ 3 trong RUN-20261026-05):** tạo `FontScaleTestSupport.kt`, `MinMediaSize`, `fullMediaHeight`, `Layout` riêng của `FullLayout`, test `full_partsKeepSize_insideCard_textsAndFontScales`; sửa dòng Media của README mục 3 và cột "Đầy đủ" của bảng `ads-guide.md` mục 4. T-045 dùng các thứ này (không tạo lại, L-054), sửa `ctaModifier` của `FullLayout`, có thể tách phần đo của nó thành hàm chung, sửa một câu của test `full_partsKeepSize_…` (câu hỏi 1), sửa dòng CTA (khác dòng Media) của README và ô CTA, ô Khung của medium trong bảng `ads-guide.md`.
- **T-041 (chạy trước, thứ 4):** sửa `NativeFullScreenAdScreen.kt`, tạo `NativeFlowStepLayout.kt`, sửa README mục 8, `ads-guide.md` mục 3 và dòng màn xem thử. T-045 không sửa các file code đó, chỉ dùng `NativeFullScreenStepContent` trong test canh; README, `ads-guide.md` sửa đoạn khác.
- **T-042, T-043:** không chung file.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: thứ 5 trong RUN-20261026-05, sau T-041 (theo yêu cầu của anh Nhật), `depends_on: [T-044, T-041]`. T-045 gộp nhánh chính sau T-041 trước khi viết test. T-044 dừng thì T-045 bị bỏ qua (thiếu `WithFontScale`, `MinMediaSize`, `Layout` của `FULL`).

## 7. Điều kiện dừng riêng

- Một ảnh chuẩn sẵn có (cỡ chữ 1.0) đổi sau phần sửa: dừng, không ghi lại ảnh; plan chỉ cho ghi ảnh mới `NativeAdCard_font-scale-2_light.png`.
- Khi bắt đầu task, `FontScaleTestSupport.kt`, `NativeAdDimens.MinMediaSize`, `fullMediaHeight` hay `Layout` riêng của `FullLayout` chưa có trên nhánh chính (T-044 chưa merge): dừng và báo, không tạo bản riêng.
- Phần sửa cần đổi `NativeFullScreenAdScreen.kt`, `NativeFlowStepLayout.kt` hay `NativeFullScreenOverlay.kt` (đầu 48 dp, nhãn "Ad" của toàn màn hình): ngoài phạm vi (câu hỏi 2), dừng và hỏi.
- Giữ sàn 120 dp cho media của thẻ ở bước, lớp thu gọn hay thẻ dọc: ngoài phạm vi (câu hỏi 3), không làm trong task này.

## 8. Câu hỏi cho người duyệt

1. **Khung CTA cao vừa chữ hay giữ cố định?** Ở cỡ 2.0 dòng chữ CTA cao 37,3 dp (small 38,7 dp), khung thiết kế 34 hay 36 dp.
   - Đề xuất của AI: khung cao **ít nhất** số thiết kế, cao thêm vừa một dòng chữ khi cần (mục 3.1, A). Ở 1.0 và 1.3 không đổi gì; ở 2.0 khung cao thêm 1,3 tới 4,7 dp, thẻ giữ chiều cao, media của thẻ 250 dp và thẻ dọc bớt 1,3 dp. Cách này buộc sửa một câu của test `full_partsKeepSize_insideCard_textsAndFontScales` (T-044 khẳng định CTA đúng 36 dp ở 2.0) thành "ít nhất 36 dp và chữ không tràn"; plan ghi rõ sửa này (mục 3.2, mục 4 dòng 7), anh Nhật duyệt thì phiên không coi là nới test. Phương án khác: (b) giữ khung cố định, chỉ khi dòng chữ cao hơn khung thì vẽ chữ không có khoảng dòng thêm (cao bằng cỡ chữ của font, khoảng 31,5 dp ở 2.0): không đổi kích thước nào, không sửa test của T-044, nhưng phụ thuộc số đo của font và hỏng lại ở cỡ chữ trên 2.0 (plan để (b) làm phương án dự phòng, mục 3.4); (c) khóa cỡ chữ của CTA ở 1.3: chữ nhỏ hơn cài đặt trợ năng của người dùng, không đề xuất.
2. **Nhãn "Ad" của native toàn màn hình sau inter bị ép khi chữ lớn** (đầu cố định 48 dp: nhãn 13,7 dp ở 1.3, 2 dp ở 2.0; O-08 đòi nhãn luôn thấy được). T-041 sửa đầu của trang toàn trang (`heightIn(min = 48.dp)`), giữ nhánh sau inter 48 dp cố định.
   - Đề xuất của AI: một ticket sau, nhỏ, sau T-041: dùng đầu `heightIn` của T-041 cho cả nhánh sau inter, kèm test nhãn "Ad" ở 1.0, 1.3, 2.0 và ảnh `NativeFullScreenAdScreen_*` không đổi ở 1.0. Không gộp vào T-045 để giữ T-045 trong `NativeAdCard.kt` và tránh sửa chồng `NativeFullScreenAdScreen.kt` ngay sau T-041.
3. **Media dưới 120 dp ở thẻ 250 dp khi chữ lớn.** Thẻ ở bước và lớp thu gọn (media lấp phần còn lại) có media 101 và 99 dp ở cỡ 2.0 trên code hiện tại (124 và 122 dp ở 1.3); KB-2 và README đòi ít nhất 120 x 120 dp, AdMob cần 120 x 120 dp để hiện video.
   - Đề xuất của AI: ticket sau, chọn một trong hai: thẻ 250 dp cao thêm khi media chạm sàn 120 dp (như luật media co trước của T-044, nhưng nút Tiếp của luồng sẽ dịch xuống), hay chữ của đầu thẻ khóa cỡ ở 1.3. T-045 chỉ ghi số đo và để nguyên (phần sửa bớt thêm 1,3 dp).
4. **Vùng chạm của CTA dưới 44 và 48 dp** (34 dp ở small, 36 dp ở thẻ; bảng 5.7 đã duyệt).
   - Đề xuất của AI: giữ như thiết kế. CTA là view của quảng cáo, SDK nhận chạm; quy tắc 48 dp của `design-workflow.md` mục 8.5 áp cho nút của app. Task chỉ làm khung cao thêm khi chữ lớn, không làm nhỏ đi.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
