---
ticket: T-041
title: Native toàn trang của luồng giữ CTA trong màn thấp và hiện quảng cáo mới sau khi xoay, gập máy
status: stopped                 # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 22M          # cỡ M, số gợi ý 19.6M (scripts/token-usage.py estimate, 2026-10-26); bỏ phần giữ quảng cáo, thêm đo cỡ chữ, khung chờ và test qua AdsController thật, ghi lại ảnh chuẩn ở hai module
blocking_points: [ads, core, analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-041: NATIVE TOÀN TRANG CỦA LUỒNG GIỮ CTA TRONG MÀN THẤP VÀ HIỆN QUẢNG CÁO MỚI SAU KHI XOAY, GẬP MÁY

## 1. Tóm tắt

Hai phần. (1) Bố cục: trang native toàn trang của một bước (`NativeFullScreenStep`, `:core:ads`) chọn bố cục theo chiều cao app cho. Đủ chỗ thì là cột như thiết kế: media co trước, không dưới 120 dp, rồi phần mô tả bớt dòng. Thấp hơn thì là hàng ngang gọn: media bên trái, chữ và CTA bên phải. Thấp hơn mức tối thiểu (200 dp ở cỡ chữ 1.0) thì trang không lấy quảng cáo. Chiều cao không giới hạn thì media cố định 200 dp. Đầu quảng cáo cao theo cỡ chữ hệ thống, và các mức trên tính theo nó. Trang "Flow steps" của màn xem thử không giữ khung thẻ 250 dp ở bước toàn trang, nút Next không đổi chỗ. (2) Xoay, gập máy: theo quyết định của anh Nhật (2026-10-26) và T-040, dựng lại activity đóng khe như mọi khe native: quảng cáo cũ bị hủy, trang dựng lại xin và hiện quảng cáo mới. Đọc code và chạy thử qua `AdsController` thật cho thấy đường này đã chạy. Trang chỉ trống trong lúc chờ quảng cáo mới vì trang không vẽ gì khi chưa có quảng cáo. Task thêm khung chờ tĩnh cho lúc đó, và test dựng lại activity qua `AdsController.fullScreenStep` thật để giữ hành vi này.


**Phiên bản 2 (RUN-20261026-05, vì sao plan chưa đủ tốt):** phiên bản 1 dừng ở bước 5. Test có sẵn `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep` (T-032) có câu đối chứng `bounds(STEP_CARD).top >= bounds(FLOW_PAGER).bottom - 1`: thẻ nằm dưới pager về hình học. Mục 3.1 bước 3 của plan đặt pager lấp cả vùng bước và khung thẻ 250 dp đè ở đáy vùng đó, còn test viết trước `flowSteps_pagerHeight_sameOnCardAndFullPage` đòi pager cao như nhau ở mọi bước. Ba điều này không thể cùng đúng, nên câu đối chứng đỏ; sửa nó là đổi điều một test cũ kiểm về hành vi đã có (`risk-scoring.md` mục 4, điều kiện 3 và 5). Lần chạy thử ở mục 4.1 không thấy vì khi đó khung thẻ cao 0 dp ở bước toàn trang. Phần MC-06 của test (thẻ không có pager là tổ tiên) vẫn qua. Thay đổi so với phiên bản 1: chỉ thêm câu hỏi 9 ở mục 8 và dòng sửa test ở mục 3.2; cách làm không đổi.
## 2. Hiểu yêu cầu

Ticket xử lý KB-20 trong `features/ads/spec.md` (spec `confirmed` 2026-10-26), luật MC-06, OB-01, OB-02, O-01, O-08, G-03, S-11 trong `docs/ops/ads-business-rules.md`. Lỗi gốc: ảnh chuẩn `app/src/test/screenshots/AdGalleryScreen_flow-full-page_small.png` (ghi chú rà soát của T-036) và ghi chú rà soát của T-032 (xoay, gập máy thì trang mất quảng cáo).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-20 | Native toàn trang không có nút điều khiển riêng; nút Tiếp của app vẫn ở chỗ quen thuộc và không bị quảng cáo che. | Giữ nguyên (T-032). Thêm: mọi phần bắt buộc của quảng cáo (tiêu đề, nhãn "Ad" đủ chiều cao chữ, AdChoices, CTA 52 dp) nằm trọn trong trang ở mọi chiều cao và cỡ chữ mà trang lấy quảng cáo; media không dưới 120 dp (README thiết kế, dòng Media). Trang thấp hơn mức tối thiểu thì không lấy quảng cáo. Nút Next của màn xem thử ở cùng chỗ trên mọi bước (OB-01). |
| KB-20 | Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống. | Giữ nguyên với trang chưa có trong luồng (`rememberNativeFullScreenStepReady`). Trang đã có trong luồng (số trang cố định khi luồng chạy, T-036) mà đang chờ quảng cáo thì vẽ khung chờ tĩnh thay vì trang trống (mục 8, câu 4). Khóa tắt hay cổng đồng ý đóng thì vẫn không vẽ gì (L-061, S-11). |
| KB-20 (O-01, T-040) | Mỗi lượt hiện sở hữu một quảng cáo, rời màn thì hủy, một quảng cáo chỉ ghi một lượt hiển thị. | Dựng lại activity (xoay, gập, mở máy) là rời màn: quảng cáo cũ bị hủy, khe nạp lại ngay (`Reload ... in 0 ms`), trang dựng lại lấy quảng cáo mới khi về, như khe native của T-040 (`plans/T-040-nguon-mot-quang-cao-va-lam-moi.md`, dòng 58: dựng lại đóng khe rồi mở lại với một lượt nạp mới). Quảng cáo cũ chỉ có một lượt hiển thị vì nó bị hủy và không được vẽ lại. Quảng cáo mới là một lượt hiện mới, nên `af_native_ad_eligible` ghi lại như mọi khe native. |
| KB-20 (G-03) | Trang native toàn trang giữ chặn quảng cáo tự hiện khi đang hiện. | Giữ nguyên: chặn khi trang vẽ quảng cáo. Khung chờ không chặn (như khi không có quảng cáo hôm nay). |

**Vì sao trang "mất quảng cáo" sau khi xoay** (đọc code và chạy thử, mục 4.1). Khi activity dựng lại, composition cũ bị hủy. `fullScreenStepStates` trả quảng cáo trong `finally`, rồi `releaseStepFullScreenAd` hủy nó và gọi `fullScreenReloads.onClosedOrShowFailed`: lượt nạp lại hẹn sau 0 ms (`AdRetryPolicy.reloadAfterCloseMillis(NATIVE_FULL_SCREEN) = 0`) và chờ app ở foreground. Dựng lại không làm process rời foreground (`AdsActivityTracker` bỏ qua `isChangingConfigurations`). Trang mới không lấy được gì (`take()` trả `null`). Nó gọi `ensureLoading`, báo `Unavailable`, rồi thử lấy lại mỗi 500 ms. `NativeFullScreenStepGate` không vẽ gì khi chưa có quảng cáo, nên người dùng thấy trang trống trong suốt lượt nạp (vài giây, lâu hơn nếu mạng chậm hay không có quảng cáo: thử lại theo O-05, 5 giây). Khi quảng cáo về, trang hiện nó. Không có lỗi làm trang trống mãi trong `:core:ads`. Nếu trên máy thật trang vẫn trống mãi thì đó là lỗi khác, và điều kiện dừng ở mục 7 áp dụng.

Thiết kế tham chiếu: `features/ads/design/README.md`, dòng "Trang native ở bước của luồng" (biến thể `Flow step` của `Native/FullScreen`, T-032), dòng Media (T-044 ghi "media co trước, không dưới 120 dp"), và `design/context/NativeFullScreenAdScreen_default_light.md` (lề 24/16/32/16 dp, hàng trên 36 dp, cách nhau 16 dp, đầu quảng cáo 48 dp, mô tả `bodyMedium` tối đa 3 dòng, CTA 52 dp). Biến thể `Flow step` không có ảnh thiết kế riêng (README mục 8). Bố cục hàng ngang gọn không có trong Figma; anh Nhật duyệt 2026-10-26. Màn xem thử không có ảnh thiết kế; ảnh chuẩn `AdGalleryScreen_*` là bản gốc (README dòng 38).

ADR liên quan: ADR-0013, ADR-0020, ADR-0026, ADR-0027, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

**Cách tiếp tục (phiên bản 2):** tiếp tục trên nhánh cũ `claude/T-041-trang-toan-trang` (commit test `49e2fd7`, code `4e60b58`, chưa có PR). Cập nhật nhánh theo nhánh chính, sửa `flowSteps_cardOutsidePager_followsStep` theo trả lời của câu hỏi 9, chạy lại `scripts/check.sh`, rồi làm tiếp từ bước 6 (rà soát độc lập). Bước 3 chỉ áp dụng cho test đổi điều kiểm của câu hỏi 9. Phần còn lại của mục 3.1 đã làm xong trên nhánh: theo đúng phiên bản 1, lệch ghi ở mục 10.

**Phần 1. Bố cục trang toàn trang (KB-20, O-08).**

Số đo trên code hiện tại (mục 4.1): `heightIn(min = 120.dp)` của media không có tác dụng dưới `weight` (T-044 cũng thấy ở thẻ `FULL`). Khi thiếu chỗ, media co về 0, rồi CTA bị ép:

| Chiều cao trang | Media | CTA |
| --- | --- | --- |
| 360 dp | 64 dp | đủ |
| 255 dp | 0 dp | 27 dp |
| 200 dp | 0 dp | 0 dp |

Trên màn xem thử `w360dp-h640dp`, trang chỉ còn 234 dp, và chữ "Install" của CTA còn 6 dp.

1. File mới `NativeFlowStepLayout.kt` (`:core:ads`) có `FlowStepPageLayout(assets, binder, media, adChoices)`. Hàm này thay nhánh `onClose == null` của `NativeFullScreenAdLayout`; nhánh sau inter giữ nguyên, ảnh `NativeFullScreenAdScreen_*` không đổi.
   - **Số đo theo cỡ chữ** (`FlowStepMetrics`, hàm thuần `flowStepMetrics(density: Density, typography: Typography)`):
     - `headerHeight = max(48 dp, titleMedium.lineHeight + 4 dp + max(labelSmall.lineHeight, bodySmall.lineHeight))`. Mỗi `lineHeight` đổi sang dp bằng `density`, nên tính cả cỡ chữ hệ thống, kể cả bảng đổi phi tuyến của Android 14. Ở cỡ chữ 1.0 thì bằng 48 dp như thiết kế (24 + 4 + 16 = 44 dp, lấy 48 dp).
     - `columnMinHeight = 24 + 36 + 16 + 120 + 16 + headerHeight + 16 + 52 + 32` (360 dp ở 1.0).
     - `minHeight = 16 + max(120, 36 + 16 + headerHeight + 16 + 52) + 16` (200 dp ở 1.0).
     - `bodyLineHeight = bodyMedium.lineHeight`.
     - `NativeFullScreenStepDefaults.MinHeight` công khai là 200 dp (mức ở cỡ chữ 1.0), ghi trong KDoc và `ads-guide.md`.
   - Đầu quảng cáo (`AdHeader` của trang) dùng `heightIn(min = 48.dp)` thay `height(48.dp)` để tiêu đề và nhãn "Ad" không bị cắt khi chữ lớn (mục 8, câu 5).
   - Bố cục chọn bằng `BoxWithConstraints`:
     - **Chiều cao không giới hạn** (trang đặt trong vùng cuộn): cột, media cố định 200 dp, mô tả 3 dòng.
     - **`maxHeight >= columnMinHeight`**: cột như hôm nay. Số dòng mô tả là `n = min(3, floor((maxHeight - columnMinHeight - 16 dp) / bodyLineHeight))`; `n <= 0` thì không vẽ mô tả. Media lấy phần còn lại bằng cách tính cụ thể `mediaHeight = max(maxHeight - (columnMinHeight - MinMediaSize) - bodyBlock, MinMediaSize)` và đặt `height(mediaHeight)`, không dùng `weight` cộng `heightIn(min)`, theo cùng cách giữ sàn của `Layout` riêng mà T-044 làm cho thẻ `FULL`. Sàn là `NativeAdDimens.MinMediaSize` (120 dp; dùng, do T-044 tạo), không viết lại số 120. Nhờ vậy media không bao giờ dưới 120 dp. Thứ tự co: media trước (tới 120 dp), rồi mô tả (3, 2, 1, 0 dòng).
     - **`minHeight <= maxHeight < columnMinHeight`**: hàng ngang gọn, lề 16 dp bốn phía, các phần cách nhau 16 dp. Bên trái là media cao hết hàng, rộng `max(120 dp, min(cao hàng, 40% chiều rộng))`. Bên phải (`weight(1f)`), từ trên xuống:
       - hàng trên 36 dp chỉ có AdChoices ở góc trên phải (O-08);
       - đầu quảng cáo `headerHeight`;
       - mô tả, tính dòng theo cùng cách nếu còn chỗ;
       - khoảng trống `weight`;
       - CTA 52 dp rộng hết cột phải, ở đáy.
     - **Dưới `minHeight`**: không có nhánh bố cục. Cổng không vẽ trang khi đó (bước 2).
   - Phần quảng cáo vẫn đi qua `binder` như hôm nay (`HEADLINE`, `ICON`, `BODY`, `CALL_TO_ACTION`, media của SDK). Mô tả mang tag mới `NativeFullScreenAdTestTags.BODY` để test đo được.
2. `NativeFlowStep.kt`: `NativeFullScreenStepGate` bọc nội dung trong `BoxWithConstraints`, với `fits = !hasBoundedHeight || maxHeight >= flowStepMetrics(...).minHeight`. Khi `!fits`, trang được coi như không `shown`:
   - không lấy quảng cáo và không chặn quảng cáo tự hiện;
   - báo `false` một lần nếu trước đó đã báo `true`, như nhánh `!allowed`;
   - `ReportNativeEligible(allowed && shown && fits)`: trang quá thấp không phải cơ hội hiện, nên không ghi `af_native_ad_eligible` (mục 8, câu 6).
3. Màn xem thử, `FlowStepsPage` (`app/src/staging`):
   - Vùng bước là một `Box(weight(1f))` chứa `HorizontalPager(fillMaxSize)` và khung thẻ 250 dp đặt đè ở đáy (`Alignment.BottomCenter`), vẫn ngoài pager theo MC-06.
   - Trang thường của pager đệm đáy 250 dp, nên chữ "Step n of 3" vẫn ở giữa phần phía trên thẻ như hôm nay. Trang toàn trang dùng cả vùng.
   - Khung thẻ (tag `FLOW_CARD_FRAME`) chỉ dựng khi trang đứng yên không phải trang toàn trang.
   - Pager cao như nhau ở mọi bước nên không nhảy khi chuyển trang. Nút Next nằm dưới vùng bước, cùng chỗ ở mọi bước (OB-01).
   - Trang toàn trang sau thay đổi: 484 dp (72 tới 556) ở `w360dp-h640dp`; 255 dp ở `w891dp-h411dp`; 204 dp ở `w640dp-h360dp`. Hôm nay ở hai khung ngang là 5 dp và 0 dp (mục 8, câu 7).

**Phần 2. Xoay, gập máy: hủy quảng cáo cũ, hiện quảng cáo mới (O-01, T-040).**

4. Không đổi đường hủy và nạp lại: `fullScreenStepStates` (trả trong `finally`), `releaseStepFullScreenAd` (hủy, hẹn nạp lại 0 ms), lượt lấy lại mỗi 500 ms. Task thêm test giữ hành vi này qua `AdsController.fullScreenStep` thật (mục 4). Cách hiểu của task về O-01: dựng lại là rời màn.
5. Khung chờ khi trang đang chờ quảng cáo:
   - **Khi nào vẽ:** `FullPageOnScreen` vẽ khung chờ ở trạng thái `Loading` và `Unavailable`, trong lúc trang `shown`, `allowed`, `fits`.
   - **Hình dáng:** khung chờ tĩnh của trang (`FlowStepPagePlaceholder`, không chạy hiệu ứng, L-048) theo cùng bố cục cột hay hàng của bước 1. Các khối `surfaceContainer` đứng ở chỗ media, đầu quảng cáo và CTA. Không có chữ, không có nhãn "Ad", vì chưa có quảng cáo.
   - **Không chặn quảng cáo tự hiện**, và báo `false` như hôm nay.
   - **Không vẽ khung chờ khi:** `!allowed` (khóa tắt, cổng đóng), `!fits`, hay `!shown`.
   - **Tag:** `NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER`.
6. `ads-guide.md` ghi: trang của luồng đổi quảng cáo khi activity dựng lại (xoay, gập máy), như mọi khe native (T-040); trong lúc chờ quảng cáo mới, trang hiện khung chờ.
7. Tài liệu:
   - `docs/ops/ads-guide.md`: dòng trang toàn trang (chiều cao tối thiểu theo cỡ chữ, hai bố cục, chiều cao không giới hạn, khung chờ, dựng lại) và dòng màn xem thử "Flow steps".
   - `docs/features/ads/design/README.md`, mục 8: thêm dòng ảnh chuẩn mới (bố cục gọn, khung chờ), cột "So với Figma" ghi "không có trong Figma, anh Nhật duyệt 2026-10-26".
   - `docs/ops/ads-business-rules.md` không đổi: OB-03 không cần, vì hành vi theo O-01 và T-040.

Mẫu làm theo:
- cổng `NativeFullScreenStepGate` và test của T-032;
- cách đo của `NativeFlowStepTest.nextButton_samePlace_notCovered_withFullPage`;
- dựng lại activity như `ResumeAdLifecycleTest.recreate_showsNothing`;
- lối vào `putPreloadedNativeFullScreenForTest` và `openConsentGateForTest` của `AdsController` (T-044, T-026);
- đặt cỡ chữ bằng `WithFontScale` của T-044; sàn media `NativeAdDimens.MinMediaSize` và cách giữ sàn bằng chiều cao tính cụ thể như `Layout` riêng của thẻ `FULL` (T-044).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayout.kt` | `FlowStepMetrics`, `flowStepMetrics`, `FlowStepPageLayout` (cột, hàng gọn, chiều cao không giới hạn), `FlowStepPagePlaceholder` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `NativeFullScreenStepDefaults.MinHeight`; cổng đo chiều cao (`fits`); `ReportNativeEligible(allowed && shown && fits)`; khung chờ trong `FullPageOnScreen`; `FlowStepFullPage` dùng `FlowStepPageLayout`; tag `FULL_PAGE_PLACEHOLDER`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | Nhánh `onClose == null` gọi `FlowStepPageLayout` (`NativeFullScreenStepContent` đi qua đây); tag `NativeFullScreenAdTestTags.BODY` trên mô tả; đầu quảng cáo dùng chung nhận chiều cao tối thiểu (nhánh sau inter vẫn 48 dp cố định); bỏ `FlowStepTopBar` nếu chuyển sang file mới |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayoutTest.kt` | Đo bố cục ở các chiều cao và cỡ chữ (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenStepRecreateTest.kt` | Dựng lại activity qua `AdsController.fullScreenStep` thật |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepTest.kt` | Test cổng quá thấp, khung chờ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeEligibleReportTest.kt` | Test eligible không ghi khi trang quá thấp |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepScreenshotTest.kt` | Thêm ảnh bố cục gọn và khung chờ |
| Tạo | `core/ads/src/test/screenshots/NativeFlowStep_full-page-compact_light.png` | Ảnh chuẩn mới, khung `w640dp-h360dp-xxhdpi` |
| Tạo | `core/ads/src/test/screenshots/NativeFlowStep_full-page-placeholder_light.png` | Ảnh chuẩn mới, khung chờ tĩnh, khung `w411dp-h891dp-xxhdpi` |
| Sửa | `core/ads/src/test/screenshots/NativeFlowStep_full-page_light.png`, `NativeFlowStep_full-page_dark.png`, `NativeFlowStep_full-page_small.png` | Dự kiến không đổi (trang 807 dp và 556 dp, cột ở cỡ chữ 1.0; chạy thử với khung API tạm: `verifyRoborazziDebug` qua). Đổi thì dừng (mục 7) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `FlowStepsPage`: khung thẻ đặt đè ở đáy vùng bước, chỉ dựng ở bước có thẻ; KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Sửa `flowSteps_smallScreen_nextNotCovered` (bước toàn trang không còn khung 250 dp, theo phạm vi ticket); phiên bản 2: sửa câu đối chứng của `flowSteps_cardOutsidePager_followsStep` theo câu hỏi 9; thêm test mục 4 |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenScreenshotTest.kt` | Thêm ảnh khung ngang |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_light.png`, `AdGalleryScreen_flow-full-page_small.png` | Ghi lại (trang lớn hơn, CTA đủ; chạy thử thấy hai ảnh này đổi) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` | Ảnh chuẩn mới, khung `w640dp-h360dp-xxhdpi` |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_flow-card_light.png`, `AdGalleryScreen_flow-card_small.png` | Chỉ ghi lại nếu đổi (thẻ đặt đè thay vì nằm dưới pager; vị trí dự kiến không đổi) |
| Sửa | `docs/ops/ads-guide.md` | Dòng trang toàn trang và màn xem thử (mục 3.1 bước 7) |
| Sửa | `docs/features/ads/design/README.md` | Mục 8: dòng ảnh mới; dòng "Trang native ở bước của luồng" ghi hai bố cục và khung chờ |

**Dùng, do T-044 tạo (không sửa, không tạo lại):** `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FontScaleTestSupport.kt` (`WithFontScale`), hằng `NativeAdDimens.MinMediaSize` trong `NativeAdCard.kt`, lối vào `AdsController.putPreloadedNativeFullScreenForTest`, luật dòng Media của `docs/features/ads/design/README.md` mục 3 ("media co trước, không dưới 120 dp"). T-044 chạy trước; nếu T-044 chưa merge khi T-041 chạy thì dừng và báo (mục 7), không tạo bản riêng.

**Khung rỗng vào commit test của bước 3 (L-025)**, để test viết trước biên dịch được và đỏ đúng lý do, không lỗi biên dịch:
- `NativeFullScreenStepDefaults` với `MinHeight = 200.dp` (giá trị thật, chỉ là hằng);
- `internal data class FlowStepMetrics(headerHeight, columnMinHeight, minHeight, bodyLineHeight)` và `flowStepMetrics(density, typography)`, khung rỗng trả số của cỡ chữ 1.0 (48, 360, 200 dp, 20 dp) bất kể cỡ chữ: test cỡ chữ lớn đỏ;
- hằng tag `NativeFullScreenAdTestTags.BODY` và `NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER` (chưa gắn vào giao diện: test tìm chúng đỏ);
- `putPreloadedNativeFullScreenForTest`, `WithFontScale`, `MinMediaSize` đã có từ T-044, không cần khung rỗng.

Phần sửa thật (bố cục, cổng, khung chờ, màn xem thử) vào commit code của bước 4.

L-020, nơi đọc ảnh chuẩn:
- `scripts/check.sh` so ảnh qua `verifyRoborazzi*` và đòi ảnh của mỗi `captureRoboImage` có trong cây;
- `scripts/test-summary.sh` đếm số ảnh;
- `docs/features/ads/design/README.md` mục 8 đã có trong bảng.

`scripts/design-compare.sh` chỉ đọc ảnh của `feature/<tên>`, không đọc `core/ads` hay `app`. Không có test hay workflow nào đọc tên các ảnh này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay đổi định dạng vị trí. Đổi cách vẽ của `native_fullscreen` khi dùng làm trang của luồng: bố cục theo chiều cao và cỡ chữ, trang quá thấp thì không lấy quảng cáo, khung chờ khi đang chờ. Vòng đời quảng cáo không đổi.
- Sự kiện phân tích: không thêm sự kiện. `af_native_ad_eligible` không ghi cho trang quá thấp để lấy quảng cáo (mục 8, câu 6). Đây là điểm chặn 4 vì đổi lúc ghi một sự kiện đã có.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `BoxWithConstraints`, `LocalDensity` từ Compose (đã có ở `:core:ads`). Test dùng `createAndroidComposeRule<ComponentActivity>()`, `ActivityScenario.recreate()` (androidx.test.core) như `ResumeAdLifecycleTest`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:**
  - Lượt lấy lại 500 ms của `fullScreenStepStates` chạy trên đồng hồ thật trong Robolectric, vì `delay` trong composition không theo đồng hồ của looper (chạy thử mục 4.1). Test dựng lại có thể chập chờn.
  - `BoxWithConstraints` trong cổng có thể khiến test `nextButton_*` của T-032 đổi số đo.
- **Điều kiện chuyển:**
  - `NativeFullScreenStepRecreateTest` hết giờ chờ (`waitUntil` 3 giây) hai lần liên tiếp trên `scripts/check.sh`.
  - Test cũ của `NativeFlowStepTest` đỏ vì đo lại.
- **Cách làm thay:**
  - Với test dựng lại: kiểm phần lấy lại bằng `fullScreenStepStates` với `runTest` và đồng hồ ảo (test thuần, như `FullScreenStepStatesTest`). Test Robolectric chỉ kiểm phần hủy quảng cáo cũ và lượt nạp lại (log `Reload gallery_native_step_full in 0 ms`, `destroyCount == 1`). Hai test này cùng phủ tiêu chí.
  - Với số đo: đo chiều cao bằng `Modifier.onSizeChanged` của khung trang thay cho `BoxWithConstraints`. Khung đầu chưa lấy quảng cáo cho tới khi có kích thước.
  - Test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test viết trước, thất bại đúng lý do trên code hiện tại. Số đo hiện tại ghi ở mục 4.1. Khung đo tái hiện lỗi gốc: `w360dp-h640dp-xxhdpi` như ảnh `AdGalleryScreen_flow-full-page_small`, và các khung ngang mà xoay máy tạo ra.

**`NativeFlowStepLayoutTest`** (`:core:ads`, Robolectric, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`).
- Trang `NativeFullScreenStepContent(assets, media = { FakeMedia() }, adChoices = { FakeAdChoices() })` nằm trong một `Box(fillMaxWidth().height(h))` có tag.
- Đo bằng `getUnclippedBoundsInRoot` (`useUnmergedTree = true`).
- Cỡ chữ đặt bằng `WithFontScale(fontScale) { ... }` của `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FontScaleTestSupport.kt` (dùng, do T-044 tạo); không viết hàm phụ riêng.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20 tiêu chí 3, O-08: phần bắt buộc nằm trong trang ở cỡ chữ 1.0 | Compose UI test | `requiredParts_insidePage_atEveryHeight`. Với `h` trong 484, 435, 400, 360, 359, 255, 204, 200 dp, và cả `FakeNativeAdAssets.Design` lẫn `Longest`, kiểm: CTA (`NativeFullScreenAdTestTags.CTA`) cao 52 dp (sai số 0,5), `top >= page.top`, `bottom <= page.bottom`; tiêu đề, nhãn "Ad" (`NativeAdTestTags.BADGE`) và `FAKE_AD_CHOICES_TAG` nằm trong trang; AdChoices không đè CTA hay tiêu đề. Hôm nay đỏ: ở 255 dp CTA cao 27 dp, ở 200 dp cao 0 dp |
| Media không dưới 120 dp (README, dòng Media) | Compose UI test | `media_neverBelow120dp`: cùng các chiều cao, media (`MEDIA`) cao và rộng ít nhất 120 dp (sai số 0,5). Hôm nay đỏ: ở 360 dp media 64 dp; ở 255 và 200 dp media 0 dp |
| Thứ tự co: media trước, rồi mô tả | Compose UI test | `shrinkOrder_mediaFirst_thenBody` với `Longest`, mô tả tìm bằng `BODY`. 484 dp: mô tả 3 dòng (cao 60 dp), media trên 120 dp. 436 dp: mô tả 3 dòng, media 120 dp. 400 dp: mô tả tối đa 1 dòng (cao không quá 20,5 dp), media 120 dp. 360 dp: không có nút `BODY`. Hôm nay đỏ ở 400 dp (media dưới 120 dp, mô tả 3 dòng) và khi tìm `BODY` |
| Bố cục gọn dưới mức cột | Compose UI test | `compactRow_belowColumnMin`. Ở 255 dp: media bên trái CTA (`media.right <= cta.left`), CTA ở đáy (`page.bottom - cta.bottom` bằng 16 dp, sai số 0,5). Đối chứng ở 360 dp: media ở trên CTA (`media.bottom <= cta.top`), media bắt đầu ở y = 76 dp như T-032 (`flowStep_mediaStartsLikeAfterInter` vẫn xanh) |
| Chiều cao không giới hạn | Compose UI test | `unboundedHeight_media120`: trang trong `Column(Modifier.verticalScroll(...))`. Kiểm: media cao 200 dp (ít nhất 120 dp), CTA cao 52 dp, mô tả có. Hôm nay đỏ: media cao 0 dp vì `weight` không có chỗ (đo lúc viết test, ghi vào nhật ký) |
| Cỡ chữ lớn | Compose UI test | `fontScale_requiredPartsKeepSize`. Với cỡ chữ 1.0, 1.3, 2.0, ở `h` 360 và 200 dp, bộ chữ `Longest`, kiểm theo hai trường hợp. (a) `h >= flowStepMetrics(...).minHeight`: nhãn "Ad" cao đủ một dòng chữ của nó (`BADGE` cao bằng `lineHeight` của `labelSmall` đổi sang dp, sai số 0,5, không bị cắt); tiêu đề có, cao bằng một dòng `titleMedium`; media ít nhất 120 dp; CTA 52 dp trong trang. (b) `h` nhỏ hơn mức đó: cổng không lấy quảng cáo, kiểm qua `NativeFullScreenStepGate` với `fullPageStates`, `takes` rỗng. Ở 1.0 thêm số tuyệt đối: `minHeight == 200 dp`, `columnMinHeight == 360 dp`, đầu quảng cáo 48 dp. Hôm nay đỏ: ở 1.3 và 2.0 đầu cố định 48 dp cắt nhãn "Ad", và khung rỗng `flowStepMetrics` trả số của 1.0 |

**`NativeFlowStepTest`** (sửa, thêm):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20 tiêu chí 4: trang quá thấp thì như không có quảng cáo | Compose UI test | `fullScreenStep_belowMinHeight_takesNothing`: cổng `allowed = true` trong `Box(height(190.dp))`, với `fullPageStates(ad, takes)`. Kiểm: `takes` rỗng, không có `CTA` hay khung chờ, `suppressor.suppressed` là `false`. Đối chứng (L-055): cùng test ở 200 dp thì `takes` có 1 phần tử và có CTA. Hôm nay đỏ (190 dp vẫn lấy quảng cáo) |
| Trang co lại dưới mức khi đang hiện | Compose UI test | `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`: chiều cao là `mutableStateOf(484.dp)`, đổi sang 190 dp. Kiểm: `released` có đúng `ad`, báo cáo `[true, false]`, không còn chặn |
| Khung chờ khi trang đang chờ | Compose UI test | `fullScreenStep_waiting_drawsStaticPlaceholder_noSuppress`: `fullPageStates(null)` (Unavailable). Kiểm: có `FULL_PAGE_PLACEHOLDER`, không có CTA, `suppressor.suppressed` là `false`. Đối chứng: `allowed = false` thì không có khung chờ. Chạy với `mainClock.autoAdvance = false` không cần, vì khung chờ tĩnh (L-048). Hôm nay đỏ (không vẽ gì) |

**`NativeEligibleReportTest`** (sửa, thêm):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Eligible chỉ khi trang lấy được quảng cáo | Compose UI test | `fullScreenStep_belowMinHeight_noEligible`: `NativeFullScreenStepGate(allowed = true, shown = true)` trong `Box(height(190.dp))`, với `RecordingAdFunnel`. Kiểm: không có `eligible`. Đối chứng ở 200 dp: đúng một `eligible`. Hôm nay đỏ |

**`NativeFullScreenStepRecreateTest`** (`:core:ads`, Robolectric, `createAndroidComposeRule<ComponentActivity>()`), qua `AdsController` thật:
- Dựng: `testAdsController(logger, flags = DefaultAdsFlags(FakeRemoteSettings().apply { set(AdsSettings.placementEnabled(GALLERY_NATIVE_STEP_FULL), true) }))`, rồi `openConsentGateForTest()`, rồi `putPreloadedNativeFullScreenForTest(GALLERY_NATIVE_STEP_FULL, first)`.
- Nội dung: `CompositionLocalProvider(LocalAdsController provides controller) { AppTheme { NativeFullScreenStep(GALLERY_NATIVE_STEP_FULL, Modifier.fillMaxSize(), shown = true) { reports += it } } }`. Sau `activityRule.scenario.recreate()`, gọi lại `setContent` trong `onActivity`, như app làm trong `onCreate`.
- Chờ bằng `composeRule.waitUntil(3_000)`: lượt lấy lại 500 ms chạy trên đồng hồ thật (mục 4.1).
- `first` và `second` là `FakeLoadedNativeAd` có tiêu đề khác nhau. Quảng cáo đang vẽ đọc bằng chữ tiêu đề.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Xoay, gập máy: quảng cáo cũ bị hủy, khe nạp lại | Compose UI test | `recreate_destroysOldAd_requestsReload`: sau `recreate()`, `first.destroyCount == 1`, log có `Flow step full page left: gallery_native_step_full` và `Reload gallery_native_step_full in 0 ms`. Tiêu đề của `first` không còn trong cây giao diện (cũng không còn ở bất kỳ khung nào sau khi dựng lại) |
| Trang dựng lại hiện quảng cáo mới | Compose UI test | `recreate_thenNewAdArrives_showsNewAd`: sau `recreate()`, có `FULL_PAGE_PLACEHOLDER`. Sau `putPreloadedNativeFullScreenForTest(..., second)`, `waitUntil` thấy tiêu đề của `second`. Kiểm: `reports == [true, false, true]`; `second.destroyCount == 0`; log `Flow step full page shown` đúng 2 dòng, mỗi quảng cáo một dòng. Hôm nay đỏ chỉ ở khung chờ (chạy thử thấy phần hiện quảng cáo mới đã qua; câu khung chờ là câu đỏ đúng) |
| Lượt hiển thị của quảng cáo cũ chỉ một | Compose UI test | Trong cùng test: `first` được lấy một lần (log "shown" của `first` một dòng trước `recreate()`), bị hủy đúng một lần, không được vẽ lại sau `recreate()`. SDK chỉ ghi lượt hiển thị khi quảng cáo được vẽ, và quảng cáo đã hủy không vẽ lại (O-01) |
| Hai lần dựng lại liên tiếp | Compose UI test | `recreateTwice_eachAdDestroyedOnce`: mỗi quảng cáo đã hiện bị hủy đúng một lần, quảng cáo cuối còn trên màn |
| G-03 sau dựng lại | Compose UI test | `recreate_newAd_suppressesAgain`: bọc trong `AdsHostContent`. Lúc chờ, `suppressed` là `false`; sau khi `second` hiện, `suppressed` là `true` |
| L-061: vị trí tắt trong lúc dựng lại | Compose UI test | `recreate_placementOff_noPlaceholder_noTake`: tắt khóa trước `recreate()`. Kiểm: không có khung chờ, không vẽ `second` dù `second` đã có trong khe, `second.destroyCount == 0` |
| S-11: cổng đồng ý đóng | Compose UI test | `recreate_consentClosed_notTaken`: thu hồi đồng ý bằng đường có sẵn của test cổng (`ConsentGateTest`, `AdsController.gate`). Không làm được thì thay bằng một khóa vị trí tắt và ghi lệch vào nhật ký. Kiểm: không lấy `second` |

**Màn xem thử** (`AdGalleryScreenTest`, `:app` staging; CTA tìm bằng chữ `Install` của `previewAdAssets`, `useUnmergedTree = true`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Bước toàn trang không giữ khung 250 dp (lỗi gốc) | Compose UI test | `flowSteps_fullPage_fillsStepArea_small` (`w360dp-h640dp-xxhdpi`). Sau Next: trang (`STEP_FULL_PAGE`) từ 72 dp tới `next.top - 16 dp` (cao 484 dp, sai số 0,5); không có `FLOW_CARD_FRAME`; chữ `Install` nằm trọn trong trang và cao 20 dp. Hôm nay đỏ: trang 234 dp, chữ CTA 6 dp |
| Khung ngang sau khi xoay | Compose UI test | `flowSteps_fullPage_landscape_showsCta`, hai test ở `w640dp-h360dp-xxhdpi` và `w891dp-h411dp-xxhdpi`: trang cao ít nhất 200 dp, chữ `Install` nằm trong trang. Hôm nay đỏ: trang 0 dp và 5 dp |
| OB-01: Next cùng chỗ mọi bước | Compose UI test | `flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard` (có sẵn, giữ xanh) |
| Next không bị che, thẻ 250 dp ở bước thường | Compose UI test | `flowSteps_smallScreen_nextNotCovered` (sửa). Bước thường: khung 250 dp, Next dưới khung. Bước toàn trang: không có khung, trang dừng trên Next, Next trong màn |
| Không nhảy giao diện khi chuyển bước | Compose UI test | `flowSteps_pagerHeight_sameOnCardAndFullPage`: `FLOW_PAGER` có cùng bounds ở bước 1 và bước toàn trang. Đối chứng: tạm thay khung bằng `height(if (fullPage) 0 else 250)` (khung API của mục 4.1) thì test này đỏ |
| Trang toàn trang chỉ một native mỗi bước | Compose UI test | `flowSteps_fullPage_hidesCard` (có sẵn, giữ xanh) |

**Screenshot test:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn xem thử, bước toàn trang | Screenshot test | Ghi lại `src/test/screenshots/AdGalleryScreen_flow-full-page_light.png` |
| Màn thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | Ghi lại `src/test/screenshots/AdGalleryScreen_flow-full-page_small.png` (CTA đủ 52 dp) |
| Khung ngang | Screenshot test khung `w640dp-h360dp-xxhdpi` | `flowFullPage_landscape`: `src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` |
| Bố cục gọn của trang | Screenshot test khung `w640dp-h360dp-xxhdpi` | `fullPage_compact_light` trong `NativeFlowStepScreenshotTest`: `src/test/screenshots/NativeFlowStep_full-page-compact_light.png` (bước có nút Next như ảnh `full-page` có sẵn) |
| Khung chờ tĩnh | Screenshot test khung `w411dp-h891dp-xxhdpi` | `fullPage_placeholder_light`: `src/test/screenshots/NativeFlowStep_full-page-placeholder_light.png`. Khung chờ không có hiệu ứng nên chụp như các ảnh `full-page` có sẵn của file (L-048 không áp dụng) |
| Đúng thiết kế (khi có `design/screens/`) | Không chạy so sánh thiết kế. Task này không có ảnh thiết kế, vì biến thể của trang toàn trang ở bước của luồng không có ảnh riêng (README mục 8). Ảnh cột của trang (`NativeFlowStep_full-page_light`, `_dark`, `_small`) không đổi ở 807 dp và 556 dp. Người duyệt PR so ảnh gọn và ảnh khung chờ với component toàn màn hình trên Figma | Xem dòng dưới |
| | | Ảnh thiết kế của ticket khác (T-027, T-028, T-030): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`, `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |

**Kiểm tra thủ công:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Xoay máy: quảng cáo cũ hủy, quảng cáo mới hiện | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, màn xem thử, "Flow steps", bấm Next tới trang toàn trang, rồi xoay ngang. Thấy khung chờ, rồi một quảng cáo mới (thường trong vài giây), CTA thấy được và bấm được. Log `VGL.Ads` có `Flow step full page left`, `Reload gallery_native_step_full in 0 ms`, rồi `Flow step full page shown`. Xoay về dọc: như vậy |
| Gập, mở máy | Kiểm tra thủ công (máy thật: gập có bản lề) | Như trên, trên máy gập |
| Lượt hiển thị | Kiểm tra thủ công (máy thật: quảng cáo thật) | Ad Inspector hay Firebase DebugView: mỗi quảng cáo một lượt `ad_impression`, qua hai lần xoay |

Không có tệp kịch bản emulator: lệnh `fresh` mở `MainActivity`, không mở được màn xem thử. Ngoài ra `device-session` từ chối đồng ý (`consent decline`), nên trang toàn trang không có quảng cáo để kiểm.

**Ngoại lệ viết test trước:**
- Tài liệu: `ads-guide.md`, design README.
- `NativeFullScreenStep` không đổi chữ ký; đường của nó được test qua `AdsController` thật.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, worktree của nhánh `claude/plan-T-041`, từ nhánh chính `6077815`. Dùng ba file test tạm không commit, đã xóa:
  - `DryRunT041Test.kt`: đo bố cục và thứ tự hủy khi dựng lại;
  - `DryRunT041GalleryTest.kt`: màn xem thử ở năm khung;
  - `DryRunT041b.kt`: dựng lại activity qua `AdsController` thật.

  Kèm khung API tạm, đã hoàn lại:
  - khung thẻ cao 0 dp ở bước toàn trang của màn xem thử;
  - tham số `isKept` của `fullScreenStepStates` (của bản plan trước, đã bỏ);
  - `putPreloadedNativeFullScreenForTest` (T-044 thêm đúng hàm này).
- **Lệnh:**
  - `./gradlew :core:ads:testDebugUnitTest --tests '*DryRunT041*' :app:testStagingDebugUnitTest --tests '*DryRunT041*'`;
  - `./gradlew :core:ads:testDebugUnitTest --tests '*DryRunT041b*'`;
  - `scripts/check.sh` với hai test tạm đầu và khung API tạm trong cây. Mọi lệnh chạy với JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`.
- **Kết quả, bố cục (L-029)**, `NativeFullScreenStepContent` trong khung `w360dp`, chiều cao cho trước:

  | Chiều cao trang | Media | CTA |
  | --- | --- | --- |
  | 484 dp | 168 dp | 400 tới 452 dp |
  | 436 dp (`Longest`) | 120 dp | |
  | 436 dp (`Design`) | 140 dp | |
  | 360 dp | 64 dp | |
  | 255 dp | 0 dp | 196 tới 223 dp (27 dp) |
  | 200 dp | 0 dp | 0 dp |
- **Kết quả, màn xem thử (L-029)**, bước toàn trang sau một lần Next:

  | Khung | Trang toàn trang hôm nay | Bỏ khung 250 dp ở bước toàn trang |
  | --- | --- | --- |
  | `w360dp-h640dp` | 72 tới 306 dp (234 dp); khung thẻ 306 tới 556 dp; Next 572 tới 624 dp; chữ CTA 268 tới 274 dp (6 dp) | 484 dp |
  | `w411dp-h891dp` | 485 dp | 735 dp |
  | `w673dp-h841dp` (mở máy gập) | 435 dp | 685 dp |
  | `w891dp-h411dp` | 5 dp | 255 dp |
  | `w640dp-h360dp` | 0 dp | 204 dp |
- **Kết quả, dựng lại qua `AdsController` thật** (`DryRunT041b`):
  - Dựng: `openConsentGateForTest`, khóa `is_show_gallery_native_step_full` bật, `first` đặt vào khe, `NativeFullScreenStep` trong `ActivityScenario<ComponentActivity>`.
  - Trước khi dựng lại: báo `[true]`, `first` chưa bị hủy.
  - Sau `recreate()` và `setContent` lại: báo `[true, false]`, `first.destroyCount == 1`. Log có `Flow step full page left`, `Reload gallery_native_step_full in 0 ms`, `Native full-screen gallery_native_step_full waits for foreground` (test không có `AdsActivityTracker`; trong app, process vẫn ở foreground khi dựng lại).
  - Đặt `second` vào khe: báo `[true, false, true]`, log `Flow step full page shown` lần hai, `second` không bị hủy.
  - Lần chạy đầu dùng `ShadowLooper.idleFor` mà không thấy `second`, vì `delay(500)` trong composition chạy theo đồng hồ thật. Chờ thật 100 ms một lần thì thấy. Vì vậy test dùng `waitUntil` (mục 3.4).
  - Kết luận: không có lỗi làm trang trống mãi. Trang chỉ trống trong lúc nạp lại.
- **Kết quả, thứ tự hủy khi dựng lại:** khi gọi `recreate()`, `onDispose` rồi `finally` của composition cũ chạy trước composition mới. Cả hai đều thấy `isChangingConfigurations=true`.
- **Kết quả, `scripts/check.sh`** (khung API tạm và hai test tạm đầu trong cây): 1376 test JVM chạy, 3 đỏ, đều thuộc mục 3.2:
  - `AdGalleryScreenTest.flowSteps_smallScreen_nextNotCovered` ("card frame height expected 250.0 but was 0.0"), test sửa theo phạm vi ticket;
  - `AdGalleryScreenScreenshotTest.flowFullPage_light` và `flowFullPage_small`: ảnh chuẩn đổi, ghi lại theo plan.

  Phần còn lại:
  - test tạm qua;
  - `:core:ads:verifyRoborazziDebug` qua, `AdGalleryScreen_flow-card_*` không đổi;
  - `ktlint` đỏ chỉ ở hai file test tạm;
  - `:app:lintStagingDebug`, `:app:assembleStagingDebug` chạy xong;
  - không có test đỏ ngoài mục 3.2.

  Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).
- Phương án dự phòng 3.4 chưa thử.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**
- quảng cáo, điểm 1: đổi cách vẽ của định dạng `native_fullscreen` khi làm trang của luồng, và lúc trang lấy quảng cáo;
- thu thập dữ liệu, điểm 4: `af_native_ad_eligible` không ghi cho trang quá thấp;
- module core dùng chung `:core:ads`, điểm 7.

Không thêm vị trí, quyền, SDK hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm cần biết trước vì task đổi lúc trang lấy quảng cáo và lúc ghi eligible.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app` staging); API công khai mới `NativeFullScreenStepDefaults`; một file code mới, ba file sửa |
| B. Độ rõ của yêu cầu | 1 | Ticket, KB-20 và các trả lời 2026-10-26 rõ; bố cục gọn và khung chờ không có trong Figma (anh Nhật duyệt) |
| C. Khả năng kiểm chứng tự động | 2 | Bố cục, cổng, eligible và dựng lại có test Robolectric qua `AdsController` thật; quảng cáo thật qua xoay, gập máy và lượt hiển thị của SDK chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt` (lối vào test), `ads-guide.md`, design README, `AdGalleryScreenTest.kt` cũng bị T-044 sửa; T-042 có thể sửa `AdsController.kt` (mục 6) |
| E. Độ mới của kỹ thuật | 1 | Chọn bố cục theo `BoxWithConstraints` và đo cỡ chữ theo mẫu của T-044; dựng lại activity theo mẫu `ResumeAdLifecycleTest`. Không còn kỹ thuật giữ quảng cáo qua dựng lại |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeFlowStep.kt`, `NativeFullScreenAdScreen.kt` của `:core:ads`; `AdGalleryScreen.kt` (staging); `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`; các ảnh chuẩn ở mục 3.2.
- Không đổi:
  - bố cục sau inter (`NativeFullScreenAdScreen`, `NativeFullScreenOverlay`): chỉ nhánh `onClose == null` đổi;
  - `AdsController.kt`: T-041 chỉ dùng lối vào `putPreloadedNativeFullScreenForTest` của T-044.
- App dùng `NativeFullScreenStep` hôm nay: chỉ màn xem thử staging (`gallery_native_step_full`). Chưa có vị trí của app.
- **T-044** (bổ sung test quảng cáo), chạy ngay trước T-041. Sửa các file sau, và T-041 dựa vào chúng:
  - `AdsController.kt`: thêm `putPreloadedNativeFullScreenForTest` (`@VisibleForTesting internal`), cạnh `openConsentGateForTest`. T-041 dùng hàm này (dùng, do T-044 tạo), không thêm lại;
  - `docs/features/ads/design/README.md`, mục 3, bảng thành phần, dòng Media: "đầy đủ 200 dp ở cỡ chữ 1.0, co lại khi chữ lớn". T-041 dùng cùng luật "media co trước, không dưới 120 dp" và sửa mục 8 (khác chỗ);
  - `docs/ops/ads-guide.md`, mục 4, bảng số đo, cột "Đầy đủ (`FULL`)". T-041 sửa dòng trang toàn trang của mục 3 và dòng màn xem thử (khác chỗ);
  - `NativeAdCard.kt`: `FullLayout` thành `Layout` riêng giữ sàn media, hằng `NativeAdDimens.MinMediaSize`. T-041 dùng hằng này cho trang toàn trang (dùng, do T-044 tạo), không sửa `NativeAdCard.kt`;
  - tạo `FontScaleTestSupport.kt` (`WithFontScale`), hàm phụ cỡ chữ dùng chung của test `:core:ads`. T-041 dùng (dùng, do T-044 tạo), không sửa;
  - `AdGalleryScreenTest.kt`: sửa `buttons_haveTouchTargetOf48dp`. T-041 sửa nhóm `flowSteps_*`, khác chỗ. T-041 gộp nhánh chính sau T-044 trước khi viết test.
- **T-042** (rewarded bấm đúp, chạy trước): sửa `RewardedAction.kt`, `RewardedTurns.kt`, có thể sửa `AdsController.kt` và dòng rewarded của `ads-guide.md`. T-041 không sửa `AdsController.kt`, và sửa dòng khác của `ads-guide.md`.
- **T-043** (resume kiểm vị trí, chạy trước): sửa `ResumeAdTurns.kt`. Không chung file với T-041.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: cuối, như danh sách T-043, T-042, T-044, T-041.

## 7. Điều kiện dừng riêng

- Test dựng lại thấy quảng cáo cũ vẫn được vẽ sau `recreate()`, hay quảng cáo bị hủy trong lúc còn đang vẽ trên màn (`destroyCount` tăng khi tiêu đề của nó còn trong cây): dừng và báo. Đây là lỗi vòng đời, không phải lỗi bố cục.
- Ảnh chuẩn `NativeFlowStep_full-page_light`, `_dark`, `_small` (`core/ads`) đổi: dừng và giải thích vì sao bố cục cột ở cỡ chữ 1.0 đổi. Không ghi lại ảnh.
- Ảnh chuẩn `NativeFullScreenAdScreen_*` hay `NativeFullScreenOverlay_*` (bố cục sau inter) đổi: ngoài phạm vi, dừng và báo.
- Kiểm thủ công trên máy thật thấy không có quảng cáo mới sau khi xoay. Ví dụ: khung chờ còn mãi trong khi log có `Reload ... in 0 ms` mà không có lượt nạp nào, hay lượt nạp không bao giờ được gửi. Khi đó dừng, ghi log vào báo cáo. Không thêm cách giữ quảng cáo cũ.
- Khi bắt đầu task, `FontScaleTestSupport.kt`, `NativeAdDimens.MinMediaSize` hay `putPreloadedNativeFullScreenForTest` chưa có trên nhánh chính (T-044 chưa merge): dừng và báo, không tạo bản riêng.
- Phần sửa cần đổi `AdPreloader`, `FreshAdCache`, `FullScreenReloads` hay `releaseStepFullScreenAd`: ngoài phạm vi, dừng và hỏi.

## 8. Câu hỏi cho người duyệt

Đã trả lời (anh Nhật, 2026-10-26):

1. **Bố cục gọn dưới mức cột.** Trả lời: duyệt hàng ngang gọn, mức tối thiểu 200 dp (ở cỡ chữ 1.0).
2. **Tách phần 2 thành ticket riêng?** Trả lời: giữ một ticket.
3. **Giữ quảng cáo qua xoay, gập máy?** Trả lời: không giữ. Theo T-040 và O-01, dựng lại đóng khe và nạp lại. Trang dựng lại phải xin và hiện quảng cáo mới, không để trống. Plan đã bỏ chỗ giữ, cửa sổ 10 giây và luật OB-03. Không cần luật mới, vì hành vi theo O-01 và T-040.

Đã trả lời thêm (anh Nhật, 2026-10-26):

4. **Khung chờ khi trang đang chờ quảng cáo mới.** Sau khi xoay máy, trang trống trong lúc nạp lại; rà soát T-032 gọi là "mất quảng cáo".
   - Đề xuất của AI: khung chờ tĩnh (không hiệu ứng, không chặn quảng cáo tự hiện) khi trang ở trên màn, được phép, đủ cao mà chưa có quảng cáo; áp cho mọi lần chờ.
   - Trả lời: đồng ý, khung chờ tĩnh trong lúc chờ.
5. **Đầu quảng cáo 48 dp có lớn theo cỡ chữ hệ thống không?** Đầu cố định 48 dp cắt nhãn "Ad" từ cỡ chữ 1.3.
   - Đề xuất của AI: có; `heightIn(min = 48.dp)`, mức cột và mức tối thiểu tính theo chiều cao đầu thật; ở cỡ chữ 1.0 giữ số của thiết kế.
   - Trả lời: đồng ý, đầu lớn theo cỡ chữ, các mức tính từ chiều cao đầu thật.
6. **`af_native_ad_eligible` cho trang quá thấp.** `ReportNativeEligible(allowed && shown)` không xét chiều cao.
   - Đề xuất của AI: không ghi khi trang quá thấp (`allowed && shown && fits`); giữ `analytics` trong `blocking_points`.
   - Trả lời: đồng ý, không ghi; giữ `analytics` trong `blocking_points`.
7. **Màn xem thử ở khung ngang nhỏ chỉ dư 4 dp** (204 dp so với mức 200 dp; cỡ chữ lớn hơn 1.0 thì không lấy quảng cáo ở khung này).
   - Đề xuất của AI: chấp nhận cho màn xem thử; ghi vào `ads-guide.md` rằng app nên cho trang toàn trang ít nhất 360 dp.
   - Trả lời: đồng ý; `ads-guide.md` khuyên ít nhất 360 dp.
8. **Thẻ 250 dp ở khung ngang của màn xem thử** (vùng bước thường còn 5 dp ở `w891dp-h411dp`).
   - Đề xuất của AI: không sửa trong task này.
   - Trả lời: đồng ý, ngoài phạm vi.

Không còn câu hỏi mở.

Câu hỏi của phiên bản 2:

9. **Câu đối chứng "thẻ nằm dưới pager" của `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep` mâu thuẫn với khung thẻ đặt đè ở đáy vùng bước (mục 3.1 bước 3).**
   - Đề xuất của AI: đổi câu đối chứng thành "thẻ nằm ở đáy vùng bước": `bounds(STEP_CARD).bottom` bằng `bounds(FLOW_PAGER).bottom` (sai số 1 dp) và thẻ cao 250 dp; giữ phần MC-06 (không có pager là tổ tiên) như cũ. Điều test kiểm về MC-06 không đổi, chỉ đổi vị trí hình học theo bố cục mới.
   - Phương án khác: (b) bỏ test viết trước `flowSteps_pagerHeight_sameOnCardAndFullPage`, cho pager ở bước thẻ thấp hơn 250 dp, thẻ nằm dưới pager như hôm nay (giao diện nhảy khi chuyển giữa bước thẻ và bước toàn trang); (c) giữ cả hai, thêm khoảng đệm 250 dp dưới pager ở mọi bước và cho trang toàn trang tràn xuống phần đệm (trang không còn là con của pager theo chiều cao, phức tạp hơn).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-26 trong phiên giám sát cùng AI. Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua rà soát độc lập, sửa theo điểm chặn và điểm nên sửa. Chạy thứ 4 trong RUN-20261026-05. 

Phiên bản 2: <người duyệt điền>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261026-05, phiên bản plan 1

- **Kết quả:** dừng ở bước 5 (`risk-scoring.md` mục 4, điều kiện 3): test có sẵn `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep` đỏ ở câu đối chứng "thẻ nằm dưới pager" (dòng 491), mâu thuẫn với mục 3.1 bước 3 và test viết trước `flowSteps_pagerHeight_sameOnCardAndFullPage`. Không sửa test. Plan phiên bản 2 ở trên.
- **Nhánh, PR:** `claude/T-041-trang-toan-trang` (từ nhánh chính `04e80c0`). Commit test `49e2fd7`, code `4e60b58` (thông điệp ghi rõ chưa qua `check.sh`), đã đẩy. Chưa mở PR. Bước 3 đến 5 do phiên con làm trong worktree `../task-T-041` (L-052).
- **Đã làm:** mọi phần của mục 3.1: `NativeFlowStepLayout.kt` (`FlowStepMetrics`, `flowStepMetrics`, `FlowStepPageLayout` với cột giữ sàn media `MinMediaSize`, hàng ngang gọn, chiều cao không giới hạn media 200 dp; `FlowStepPagePlaceholder`); `NativeFlowStep.kt` (`MinHeight` 200 dp, cổng đo chiều cao `fits`, `ReportNativeEligible(allowed && shown && fits)`, khung chờ `FULL_PAGE_PLACEHOLDER`); `NativeFullScreenAdScreen.kt` (nhánh `onClose == null` dùng `FlowStepPageLayout`, đầu quảng cáo `heightIn(min = 48.dp)`, tag `BODY`); `AdGalleryScreen.kt` (khung thẻ đè ở đáy vùng bước); `ads-guide.md`, `design/README.md`.
- **Test viết trước:** 23 test mới, sửa `flowSteps_smallScreen_nextNotCovered` (plan cho phép). 16 đỏ đúng lý do trên khung rỗng (ví dụ CTA 27 dp ở trang 255 dp; media 0 dp khi chiều cao không giới hạn; media 44 dp ở 360 dp; 190 dp vẫn lấy quảng cáo; trang trên màn xem thử chỉ 234 dp). Qua ngay như plan nêu: bốn test dựng lại (`recreate_*`, đường hủy và nạp lại có sẵn; hai test có đối chứng bật lại khóa hay mở lại đồng ý) và `flowSteps_pagerHeight_sameOnCardAndFullPage` (đối chứng với khung tạm `height(if (fullPage) 0 else 250)` đỏ, 557 so với 807). 9 test viết chặt bằng đối chứng. Test JVM mọi module sau commit test: chỉ 16 test đó đỏ.
- **Kiểm tra cục bộ:** `scripts/check.sh` dừng ở bước ghi ảnh của `:app` vì 1 test đỏ (test nói trên). Chạy tay các task của cổng với `--continue`: `checkModuleDependencies`, `ktlintCheck`, `:app:lintStagingDebug`, `:app:assembleStagingDebug`, `verifyRoborazziDebug` của các module thư viện qua; test JVM `:core:ads` 1030 test 0 lỗi, `:app` 142 test 1 lỗi.
- **Rà soát độc lập:** chưa làm (dừng trước bước 6).
- **Kết quả CI:** chưa có.
- **Lệch so với plan:** `fontScale_requiredPartsKeepSize` tính "một dòng" bằng cỡ chữ đổi sang dp nhân `lineHeight / fontSize` (Compose đổi chiều cao dòng qua bảng phi tuyến: 21,0 dp ở cỡ 1.3, không phải 20,2 dp), điều test kiểm không đổi; `shrinkOrder` ở 400 dp ra media 124 dp (test kiểm sàn từ 120 dp); media ở bố cục cột dùng `Layout` riêng đo các phần thật như T-044, nên ảnh `NativeFlowStep_full-page_*` giữ nguyên từng byte. S-11 thu hồi đồng ý bằng `controller.resetConsent()`.
- **Ngoài mục 3.2 (ADR-0029):** không có.
- **Ảnh chuẩn:** mới `NativeFlowStep_full-page-compact_light.png`, `NativeFlowStep_full-page-placeholder_light.png` (`core/ads`), `AdGalleryScreen_flow-full-page_landscape.png` (`app`); ghi lại `AdGalleryScreen_flow-full-page_light.png`, `_small.png`. Các ảnh khác giữ nguyên từng byte. Phiên con đã mở xem từng ảnh.
- **Việc còn lại:** câu hỏi 9 ở mục 8; rồi bước 5 đến 10 trên nhánh cũ.
- **Cần người xem:** câu hỏi 9; T-045 bị bỏ qua vì phụ thuộc T-041.
