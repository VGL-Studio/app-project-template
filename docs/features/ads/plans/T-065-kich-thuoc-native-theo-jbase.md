---
ticket: T-065
title: Code kích thước native theo JBase
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20.2M        # cỡ M, số gợi ý 20.2M (scripts/token-usage.py estimate, 2026-11-15), không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-065: CODE KÍCH THƯỚC NATIVE THEO JBASE

## 1. Tóm tắt

Đổi chiều cao và bố cục của ba biến thể native trong `:core:ads` theo thiết kế T-064: khe native nhỏ cao theo bề rộng màn hình (90 dp ở 320 dp tới 200 dp ở 600 dp), lớp thu gọn bằng vùng ảnh nội suy (160 tới 200 dp) cộng phần nội dung của khe đáy, native medium cao theo chiều cao màn hình (320 dp ở 640 dp tới 400 dp ở 891 dp). Ba chiều cao tính bằng hàm thuần trong `NativeAdDimens`, cùng một hàm cho thẻ, khung chờ và khoảng chừa của thanh đáy (câu hỏi 22, 49 của spec). Kiểm bằng unit test cho ba hàm, test Robolectric đo kích thước ở các khung 360, 411, 673 dp, test cỡ chữ 2.0, và ghi lại các ảnh chuẩn liệt kê ở mục 3.2 so với ảnh thiết kế.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` ngày 2026-11-15, gồm câu hỏi 64):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Khi có quảng cáo thì thấy icon, tiêu đề, CTA, nhãn "Ad", AdChoices; có nền hoặc viền; CTA khác nút chính | Giữ nguyên; bố cục mới của native nhỏ (icon 44 dp, "Ad" trước tiêu đề, mô tả, CTA 44 dp) vẫn đủ các phần, test có sẵn kiểm |
| KB-1, tiêu chí 2 | Native nhỏ ở đáy cao theo KB-32; đang tải thì giữ đúng chiều cao với hiệu ứng chờ | Khung chờ và thẻ cùng gọi `NativeAdDimens.bottomSlotHeight(bề rộng màn hình)`; khung chờ theo thiết kế (khối icon 44 dp, hai vạch, khối CTA 96 x 44 dp) |
| KB-1, tiêu chí cỡ chữ 200% | Tiêu đề, nhãn "Ad", CTA của native nhỏ không bị cắt mất chữ (có thể "…"), khe giữ chiều cao theo KB-32 | Đo lại ở cỡ chữ 2.0 trên bố cục mới, khung 320, 360, 411 dp; mô tả bớt dòng trước, tiêu đề không mất |
| KB-2, tiêu chí 1 | Medium có media ít nhất 120 x 120 dp | Medium cao theo chiều cao màn hình; media lấp phần còn lại (182 dp ở 891 dp, 170 dp với `cta_large`), ở khung 640 dp và cỡ chữ 2.0 vẫn không dưới 120 dp |
| KB-2, tiêu chí 2 | Giữ chỗ, không có quảng cáo, chế độ tối như KB-1 | Khung chờ medium cùng hàm chiều cao với thẻ |
| KB-4, tiêu chí 1 | Lớp thu gọn mở ở đáy, nổi trên thanh đáy, nút thu gọn ít nhất 48 dp vùng chạm, không cùng góc với AdChoices | Lớp cao = vùng ảnh nội suy theo bề rộng + chiều cao khe đáy (299 dp ở 411 dp); nút vẽ 27 dp tại (4, 4) góc trên trái, vùng chạm 48 dp; AdChoices góc trên phải (thiết kế T-064, câu hỏi 48; xem câu hỏi 1 ở mục 8) |
| KB-4, tiêu chí 2 | Chạm nút thu gọn thì lớp ẩn, lộ khe đáy bên dưới, khe cao theo KB-32 | Khe dưới lớp dùng cùng hàm chiều cao; không đổi luồng thu gọn |
| KB-17, tiêu chí 1 | Thanh đáy có native nhỏ và lớp thu gọn, chiều cao theo KB-32 | Như KB-1, KB-4 |
| KB-17, tiêu chí 5 | Nội dung chừa đúng chiều cao khe đáy từ lúc khe đang tải | `bottomBarReservation` và `BottomAdBarDefaults.nativeReservation()` = `bottomSlotHeight(bề rộng màn hình)` + lề 16 dp (D-04) |
| KB-32, tiêu chí 3 | Ba biến thể nội suy tuyến tính giữa hai mốc, ngoài khoảng mốc giữ sàn và trần; chữ, icon, CTA, nút thu gọn, AdChoices theo câu hỏi 48; medium giữ hàng sao; biến thể khác giữ kích thước | Ba hàm thuần, làm tròn tới dp nguyên gần nhất như số của thiết kế (106, 126, 173, 384 dp; câu hỏi 3 ở mục 8). `STEP`, `CARD`, `FULL` và native toàn màn hình không đổi |

Thiết kế tham chiếu: `features/ads/design/README.md`, mục 1, đoạn "Vẽ lại theo bề rộng màn hình (T-064)", số liệu trong `design/context/`; các ảnh trong `design/screens/`: `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`. Ảnh `NativeFullScreenAdScreen_*` không đổi ở ticket này (code đã khớp từ T-053).

ADR liên quan: ADR-0045 (mục 7.7, mục 7.2), ADR-0031, ADR-0020, ADR-0029, ADR-0038.

## 3. Giải pháp

### 3.1. Cách làm

1. **Ba hàm chiều cao (một nguồn cho vẽ, khung chờ và chừa chỗ).** Trong `NativeAdDimens` (`NativeAdCard.kt`) bỏ `BottomSlotHeight`, `MediumHeight`, `MediumLargeHeight`; thêm hàm thuần:
   - `bottomSlotHeight(screenWidth: Dp): Dp`: 90 dp ở 320 dp, 200 dp ở 600 dp, nội suy tuyến tính, kẹp ngoài khoảng.
   - `collapsibleMediaAreaHeight(screenWidth: Dp): Dp`: 160 dp ở 320 dp, 200 dp ở 600 dp, kẹp ngoài khoảng.
   - `mediumHeight(screenHeight: Dp): Dp`: 320 dp ở 640 dp, 400 dp ở 891 dp, kẹp ngoài khoảng; `cta_large` cùng chiều cao (media 170 thay 182 dp).
   - Mỗi hàm làm tròn kết quả tới dp nguyên gần nhất (`roundToInt`), để khớp số của thiết kế và để ảnh chuẩn không lệch nửa điểm ảnh.
   - Một hàm `@Composable @ReadOnlyComposable internal fun nativeScreenSize(): DpSize` đọc `LocalConfiguration.current.screenWidthDp` và `screenHeightDp` (cùng nguồn với `AdSlot.kt` hiện dùng cho banner). `heightOf(size, ctaLarge)` nhận thêm `screen: DpSize`; mọi chỗ gọi (`NativeAdCard.kt`, `NativeAdSlot.kt`) truyền `nativeScreenSize()`.
   - Chiều cao theo bề rộng **màn hình**, không theo bề rộng thẻ (thẻ 379 dp trong màn 411 dp vẫn cao 126 dp), như D-02 và thiết kế.
2. **Native nhỏ (`SmallLayout`, `NativeAdSize.SMALL`, mọi nơi dùng: khe đáy, khe đứng riêng, hàng trong danh sách; câu hỏi 2 ở mục 8).** Một hàng căn giữa theo chiều dọc: lề trái 12 dp, phải 24 dp, các phần cách nhau 12 dp; icon 44 dp; cột chữ gồm hàng tiêu đề (nhãn "Ad" 13 sp đứng trước tiêu đề 16 sp, một dòng, cắt cuối) và mô tả 13 sp tối đa hai dòng (không có mô tả thì không có dòng đó); CTA cao 44 dp, chữ 18 sp, rộng theo chữ, tối thiểu 80 dp, tối đa 120 dp (câu hỏi 4 ở mục 8). Cỡ chữ 13, 16, 18 sp ngoài bảng kiểu chữ: hằng riêng trong file, như đợt T-035. Chữ lớn: cột chữ có chiều cao giới hạn bằng chiều cao khe trừ lề; mô tả bớt dòng trước (đặt `maxLines` của mô tả theo chỗ còn lại sau tiêu đề, đo bằng `Layout`/`SubcomposeLayout` như `MediaShrinksFirstLayout`), tiêu đề và "Ad" không bị đẩy ra ngoài. Khung chờ: cùng chiều cao, khối icon 44 dp, hai vạch (vạch trên rộng hết cột chữ, vạch dưới 120 dp), khối CTA 96 x 44 dp.
3. **Medium (`MediumLayout`).** Chiều cao `mediumHeight(screenHeight)`; icon 44 dp; nhãn "Ad" 13 sp đứng trước tiêu đề 16 sp, nhà quảng cáo 13 sp ở dòng dưới; giữ hàng sao và nền mờ sau media; CTA 44 dp chữ 18 sp (`cta_large` giữ 56 dp, 17 sp); media lấp phần còn lại qua `MediaShrinksFirstLayout` như hiện nay, không dưới 120 dp; thẻ chỉ cao thêm khi media đã chạm 120 dp (giữ cách của plan T-044, T-045).
4. **Lớp thu gọn (`CollapsibleNativeAd.kt`).** Bỏ `LayerHeight` 250 dp; lớp = vùng ảnh `collapsibleMediaAreaHeight(screenWidth)` (lề 12 dp, MediaView lấp vùng, 161 dp ở 411 dp) đặt trên phần nội dung dùng lại đúng bố cục của native nhỏ (chiều cao `bottomSlotHeight`), tổng 299 dp ở 411 dp. Nút thu gọn vẽ 27 dp tròn tại (4, 4) góc trên trái, biểu tượng 17 dp, nền đen 40%, vùng chạm 48 dp (Compose tự nới như nút hiện có); `AdChoicesMarker` sang góc trên phải; `collapsibleNativeRequestOptions()` (`NativeRequestOptions.kt`) đổi `ADCHOICES_TOP_LEFT` thành `ADCHOICES_TOP_RIGHT` để SDK ghim AdChoices đúng góc mới (câu hỏi 1 ở mục 8). Cỡ chữ lớn: phần nội dung theo quy tắc của native nhỏ, vùng ảnh co trước nhưng không dưới 120 dp, rồi lớp cao thêm (giữ cách của plan T-045).
5. **Thanh đáy (`BottomAdBar.kt`).** `bottomBarReservation(slot, slotHasAd, bannerHeight, screenWidth)` trả `bottomSlotHeight(screenWidth) + SlotMargin` cho khe native. `BottomAdBarDefaults.nativeReservation` đổi từ `val` thành `@Composable @ReadOnlyComposable fun nativeReservation(): Dp` (cùng hàm). Đổi chỗ gọi: `SampleScreen.kt` và các test, `PreviewAdGallerySlots.kt`, đoạn mẫu trong `ads-guide.md`.
6. **Tài liệu.** Đối chiếu `ads-business-rules.md` mục 5.7, D-02, D-04, mục 11 và `ads-guide.md` với code khi xong: hôm nay các mục đó đã ghi kích thước đích; còn lệch (giới hạn CTA 120 dp ở dòng CTA của `ads-guide.md`, `nativeReservation` thành hàm, nút thu gọn) thì sửa. KDoc của các hằng có số cũ (80, 250, 410 dp) viết lại.
7. Thứ tự với T-060: T-060 đã `done`; plan giữ các câu kiểm của T-060 (tiêu đề còn ít nhất 4 ký tự trước "…", "Ad" hiện đủ, CTA không quá 120 dp) trên bố cục mới và thêm khung 320 dp mà T-060 để lại cho ticket này (câu hỏi 4 của plan T-060).

Mẫu có sẵn làm theo: `MediaShrinksFirstLayout` (plan T-044, T-045) cho phần co theo chữ lớn; `AdSlot.kt` đọc `LocalConfiguration.current.screenWidthDp`; cách đo và chụp của `NativeAdCardTest`, `NativeAdFontScaleTest`, `CollapsibleNativeAdTest`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | Ba hàm chiều cao, `nativeScreenSize()`, `heightOf` nhận `screen`; bố cục và khung chờ native nhỏ, medium theo mục 3.1; hằng cỡ chữ 13, 16, 18 sp; bỏ `BottomSlotHeight`, `MediumHeight`, `MediumLargeHeight`, `SmallIconSize` 36, `SmallCallToActionHeight` 34 |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | Khung chờ và chỗ giữ gọi `heightOf(size, ctaLarge, nativeScreenSize())` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | Lớp = vùng ảnh nội suy + nội dung của native nhỏ; nút 27 dp góc trên trái, biểu tượng 17 dp; AdChoices góc trên phải; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeRequestOptions.kt` | Lớp thu gọn xin AdChoices góc trên phải |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BottomAdBar.kt` | `bottomBarReservation` nhận bề rộng màn hình; `BottomAdBarDefaults.nativeReservation()` thành hàm composable; KDoc |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdDimensTest.kt` | Unit test ba hàm chiều cao (mốc, giữa mốc, ngoài mốc, làm tròn) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Test viết trước của mục 4 cho native nhỏ, medium; viết lại câu cũ theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Khung chờ và thẻ cùng chiều cao theo bề rộng; câu cũ 80 dp theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Câu cũ 80 dp và CTA ngắn 80 dp theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | Bảng chiều cao theo khung; thêm khung 320 dp cho native nhỏ cỡ chữ 2.0; medium ở 891 và 640 dp |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | Khoảng chừa theo bề rộng (142 dp ở 411 dp, 122 dp ở 360 dp) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Lớp 299 dp ở 411 dp, 272 dp ở 360 dp; vùng ảnh; nút 27 dp tại (4, 4) góc trên trái; góc AdChoices |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRequestOptionsTest.kt` | `collapsible_adChoicesTopLeft_otherwiseLikeMedium` viết lại theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarScreenshotTest.kt` | Gọi `BottomAdBarDefaults.nativeReservation()` |
| Sửa | `feature/sample/src/main/kotlin/com/vglstudio/template/feature/sample/SampleScreen.kt` | Gọi `BottomAdBarDefaults.nativeReservation()` |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenTest.kt` | Gọi `nativeReservation()` |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenScreenshotTest.kt` | Gọi `nativeReservation()`; thêm `SampleScreen_ready_light_unfolded` ở khung `w673dp-h841dp-xxhdpi` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PreviewAdGallerySlots.kt` | Gọi `nativeReservation()` |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | Native nhỏ mới (126 dp) |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_medium_light.png` | Medium 400 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_medium_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_medium-cta-large_light.png` | Medium `cta_large` 400 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_light.png` | Cỡ chữ 2.0 trên bố cục mới |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_small.png` | Như trên, khung 360 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdCard_long-texts_light.png` | Chữ dài trên bố cục mới |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdPlaceholder_small_light.png` | Khung chờ native nhỏ mới |
| Ghi lại | `core/ads/src/test/screenshots/NativeAdPlaceholder_medium_light.png` | Khung chờ medium 400 dp |
| Ghi lại | `core/ads/src/test/screenshots/BottomAdBar_collapsed_light.png` | Khe đáy 126 dp |
| Ghi lại | `core/ads/src/test/screenshots/BottomAdBar_expanded_light.png` | Lớp 299 dp, nút góc trên trái |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_light.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_small.png` | Lớp 272 dp ở khung 360 dp |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` | Khe đáy 126 dp |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_dark.png` | Như trên |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_small.png` | Khe đáy 106 dp |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png` | Khung chờ 126 dp |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_bar-expanded_small.png` | Lớp 272 dp ở khung 360 dp |
| Tạo | `feature/sample/src/test/screenshots/SampleScreen_ready_light_unfolded.png` | Ảnh mới, khe đáy 200 dp ở 673 dp |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_light.png` | Native nhỏ và medium mới |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_dark.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default_small.png` | Như trên, khung 360 x 640 |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_default-more_light.png` | Trang mặc định cuộn xuống, có native nhỏ và medium; chỉ giữ khi đổi |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_inlist_light.png` | Hàng native nhỏ và medium trong danh sách |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_inlist_small.png` | Như trên |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` | Lớp 299 dp |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png` | Lớp 272 dp |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Khe đáy 126 dp |
| Ghi lại | `docs/features/ads/design/review/*_compare.png` (các ảnh của mục 2 có trong `review/`) và `docs/features/ads/design/review/summary.md` | Sinh lại bằng `scripts/design-compare.sh ads` |
| Sửa | `docs/ops/ads-guide.md` | Đối chiếu sau khi code xong: `nativeReservation()`, dòng CTA, nút thu gọn (mục 3.1, bước 6) |
| Sửa | `docs/ops/ads-business-rules.md` | Chỉ khi đối chiếu còn lệch ở mục 5.7, D-02, D-04, mục 11 (mục 3.1, bước 6) |

Ảnh không đổi, đã kiểm nơi đọc (`grep -rn "NativeAdSize\.\|nativeReservation" */src/test app/src/testStaging`): `NativeAdCard_card_*`, `_full_*`, `_step_*`, `NativeAdPlaceholder_card_light`, `_step_light`, `NativeFlowStep_*`, `NativeFullScreen*`, `SampleScreen_disabled_light`, các trang `AdGalleryScreen_banners_*`, `_grid_*`, `_pager_*`, `_flow-*`, `_rewarded_*`, `_status_*` (không vẽ native nhỏ, medium hay thanh đáy native), `NetworkLostScreen_*` (test của màn mất mạng không vẽ thanh đáy, dù ảnh thiết kế của feature network có khe đáy). Ảnh nào ngoài bảng lệch khi ghi lại thì trả về bằng `git checkout -- <ảnh>` nếu chỉ là nhiễu vẽ; lệch thật thì dừng (risk-scoring.md, mục 4).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; đổi kích thước hiển thị của định dạng `native_small`, `native_collap`, `native_medium` theo KB-32 (điểm chặn `ads`), và góc AdChoices xin cho lớp thu gọn (`NativeAdOptions.ADCHOICES_TOP_RIGHT`).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `LocalConfiguration` thuộc `androidx.compose.ui:ui`, `DpSize` thuộc `androidx.compose.ui:ui-unit`, đã có trên classpath biên dịch của `:core:ads` (plugin `template.android.library` và Compose; `AdSlot.kt` đã dùng `LocalConfiguration`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** mô tả của native nhỏ bớt dòng theo chỗ còn lại ở cỡ chữ 2.0 (mục 3.1, bước 2) bằng `Layout` đo lại.
- **Điều kiện chuyển:** test `NativeAdFontScaleTest.small_fontScale2_headlineBadgeCta_notCut_allWidths` (mục 4) vẫn đỏ vì tiêu đề bị đẩy ra ngoài khe sau 2 lần sửa phần đo.
- **Cách làm thay:** cột chữ là `Column` có `Modifier.heightIn(max = chiều cao khe - lề)`, tiêu đề đặt trước, mô tả `Modifier.weight(1f, fill = false)` với `maxLines = 2` và `TextOverflow.Ellipsis`; ở cỡ chữ từ 1.5 mô tả `maxLines = 1` (đọc `LocalDensity.current.fontScale`). Tiêu chí và test của mục 4 giữ nguyên. Không thêm file.

### 3.5. Hành vi đã có bị đổi

<!-- Danh sách bắt đầu từ `scripts/existing-tests.py` (xem cuối bảng). Các số mới đo ở khung `w411dp-h891dp-xxhdpi` của các lớp test, trừ khi ghi khác. -->

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Native nhỏ cao 80 dp | Cao `bottomSlotHeight(bề rộng màn hình)`: 126 dp ở 411 dp | `NativeAdCardTest` (các câu `assertHeightIsEqualTo(80.dp)` ở dòng 148, 398 và bảng dòng 313) | Câu mới: thẻ native nhỏ cao 126 dp ở khung 411 dp |
| Medium cao 410 dp, `cta_large` 422 dp | Cao `mediumHeight(chiều cao màn hình)`: 400 dp ở 891 dp, cả `cta_large` | `NativeAdCardTest.medium_is410dpHigh`, `medium_ctaLarge_56dp_17sp_cardIs422dp`, các câu 410 dp ở dòng 188, 210, 292, 293, 403 và bảng dòng 314 | Câu mới: medium và khung chờ medium cao 400 dp; `cta_large` cũng 400 dp, CTA 56 dp, chữ 17 sp. Đổi tên test thành `medium_is400dpHigh`, `medium_ctaLarge_56dp_17sp_cardIs400dp` |
| Media của medium 196 dp, đỉnh media cách đỉnh thẻ 93 dp | Media lấp phần còn lại: 182 dp (170 dp với `cta_large`); đỉnh media theo đầu thẻ mới | `NativeAdCardTest.medium_hasMediaOfAtLeast120dp` | Giữ "media ít nhất 120 x 120 dp, rộng ít nhất 355 dp"; câu mới: media cao 182 dp; đỉnh media = 1 + 24 + chiều cao đầu thẻ đo được + 12 dp (phiên đo đầu thẻ mới, ghi số vào chú thích) |
| CTA ngắn ("Install") của native nhỏ rộng 80 dp | CTA rộng theo chữ 18 sp, tối thiểu 80, tối đa 120 dp | `NativeAdCardTest.small_ctaAtMost120dpWide_shortCtaKeepsWidth`, `NativeAdViewBindingTest.sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible` | Giữ "CTA dài dừng ở 120 dp, tiêu đề và nhãn Ad còn chỗ". Câu mới cho CTA ngắn: CTA "Install" không bị cắt (chữ hiện đủ, không có "…"), rộng từ 80 tới 120 dp. Câu 80 dp của chiều cao thẻ ở test thứ hai thành 126 dp (câu hỏi 4 ở mục 8) |
| Khung chờ và thẻ native nhỏ cùng 80 dp; hằng `BottomSlotHeight` = 80 | Cùng `bottomSlotHeight(411 dp)` = 126 dp | `NativeAdSlotTest` (dòng 64, 82, 103, 265, 276, 277) | Câu mới: khung chờ, thẻ và khe cùng 126 dp; câu dòng 265 (hằng = 80) thành `bottomSlotHeight(411.dp) == 126.dp` |
| Khoảng chừa của khe native = 80 + 16 = 96 dp | `bottomSlotHeight(bề rộng) + 16`: 142 dp ở 411 dp | `BottomAdBarTest` (dòng 285 `assertEquals(96.dp, …nativeReservation)`, dòng 310) | Câu mới: `nativeReservation()` ở khung 411 dp = 142 dp, bằng `bottomBarReservation(NATIVE, true, 0.dp, 411.dp)` |
| Lớp thu gọn cao 250 dp (272 dp ở cỡ chữ 2.0), media 134 dp | Lớp = vùng ảnh 173 + nội dung 126 = 299 dp ở 411 dp; 166 + 106 = 272 dp ở 360 dp | `CollapsibleNativeAdTest.firstShown_isExpanded250dp`, các câu 250 dp ở dòng 280, 388, 592 | Câu mới: lớp cao 299 dp ở 411 dp (272 dp ở khung 360 dp của dòng 589), MediaView cao 161 dp; đổi tên `firstShown_isExpanded299dp` |
| Nút thu gọn 32 dp ở góc trên phải, AdChoices góc trên trái | Nút 27 dp tại (4, 4) góc trên trái, vùng chạm 48 dp; AdChoices góc trên phải | `CollapsibleNativeAdTest` (câu về góc ở dòng 243 và các test đo nút) | Câu mới: nút vẽ 27 dp, mép trái và mép trên cách lớp 4 dp, vùng chạm ít nhất 48 dp; góc trên phải 24 dp không có phần chạm được |
| Lớp thu gọn xin `ADCHOICES_TOP_LEFT` | Xin `ADCHOICES_TOP_RIGHT`, như mọi native khác | `NativeRequestOptionsTest.collapsible_adChoicesTopLeft_otherwiseLikeMedium`, `toNativeAdOptions_copiesAdChoicesCorner` | Câu mới: `collapsibleNativeRequestOptions() == nativeRequestOptions(MEDIUM)`, góc `ADCHOICES_TOP_RIGHT`; đổi tên `collapsible_adChoicesTopRight_likeMedium`. Test thứ hai: giữ ý "góc được chép sang `NativeAdOptions`", dùng `nativeRequestOptions(MEDIUM).copy(adChoicesCorner = ADCHOICES_TOP_LEFT)` làm dữ liệu để vẫn có góc khác mặc định |
| Bảng cỡ chữ: small 80 dp, CTA 34 dp; medium 410/422 dp | Small 126 dp (106 dp ở 360 dp), CTA 44 dp; medium 400 dp | `NativeAdFontScaleTest` (dòng 128 tới 130, 245, 257, 288, 321) | Câu mới theo số mới; giữ mọi câu "không cắt mất chữ", "media không dưới 120 dp"; câu dòng 321 ("cao hơn 410 dp") thành "cao hơn 400 dp" |
| `BottomAdBarDefaults.nativeReservation` là `val` | Hàm composable `nativeReservation()` | Mọi chỗ gọi trong test (`SampleScreenTest`, `SampleScreenScreenshotTest`, `CollapsibleNativeAdTest`, `BottomAdBarScreenshotTest`, `PreviewAdGallerySlots`) | Giữ ý; chỉ đổi cú pháp gọi |

Đọc từ `scripts/existing-tests.py` ngày 2026-11-15; danh sách đầy đủ ở mục 4.1. Các câu ở bảng trên là câu của plan đã duyệt, phiên viết lại như test viết trước của bước 3, không tính vào giới hạn 3 câu của ADR-0031.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-32 tiêu chí 3: khe đáy nội suy, kẹp | Unit test | `NativeAdDimensTest.bottomSlotHeight_interpolatesAndClamps`: 300 dp → 90, 320 → 90, 360 → 106, 411 → 126, 600 → 200, 673 → 200 dp |
| KB-32 tiêu chí 3: vùng ảnh lớp thu gọn | Unit test | `NativeAdDimensTest.collapsibleMediaAreaHeight_interpolatesAndClamps`: 320 → 160, 360 → 166, 411 → 173, 600 → 200, 673 → 200 dp |
| KB-32 tiêu chí 3: medium theo chiều cao | Unit test | `NativeAdDimensTest.mediumHeight_interpolatesAndClamps`: 600 → 320, 640 → 320, 841 → 384, 891 → 400, 1000 → 400 dp |
| KB-32: làm tròn dp nguyên | Unit test | `NativeAdDimensTest.heights_areWholeDp`: với mọi bề rộng 300 tới 700 dp bước 1 dp, ba hàm trả số nguyên dp; đối chứng: 411 dp trả 126 chứ không phải 125,75 |
| KB-1 tiêu chí 2, KB-32: thẻ và khung chờ native nhỏ cùng chiều cao theo bề rộng màn hình | Compose UI test | `NativeAdCardTest.small_heightFollowsScreenWidth` (khung 411: 126 dp, thẻ rộng 379 dp vẫn 126) và hai lớp con `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` (106 dp), `"w673dp-h841dp-xxhdpi"` (200 dp); mỗi khung đo cả `NativeAdPlaceholder(SMALL)` |
| KB-1 tiêu chí 1, thiết kế T-064: bố cục native nhỏ | Compose UI test | `NativeAdCardTest.small_jbaseLayout`: icon 44 dp, mép trái icon cách thẻ 12 dp; CTA cao 44 dp, chữ 18 sp, mép phải cách thẻ 24 dp; nhãn "Ad" bên trái tiêu đề cùng hàng (`badge.right <= headline.left`, chênh tâm dọc dưới 2 dp); tiêu đề một dòng 16 sp; mô tả 13 sp tối đa 2 dòng; icon, cột chữ, CTA căn giữa theo chiều dọc của thẻ (chênh tâm dưới 1 dp) |
| KB-1: khung chờ native nhỏ | Compose UI test | `NativeAdCardTest.smallPlaceholder_jbaseBlocks`: khối icon 44 dp, khối CTA (`PLACEHOLDER_CTA`) 96 x 44 dp |
| KB-2, KB-32: medium theo chiều cao màn hình | Compose UI test | `NativeAdCardTest.medium_heightFollowsScreenHeight`: 400 dp ở 891, cả `cta_large`; lớp con khung 640 dp: 320 dp, media vẫn ít nhất 120 dp; khung 841 dp: 384 dp |
| KB-2: bố cục medium | Compose UI test | `NativeAdCardTest.medium_jbaseLayout`: icon 44 dp, "Ad" trước tiêu đề cùng hàng, CTA 44 dp chữ 18 sp; media 182 dp (170 dp với `cta_large`); hàng sao còn |
| KB-1 cỡ chữ 200% | Compose UI test (`@GraphicsMode(NATIVE)`) | `NativeAdFontScaleTest.small_fontScale2_headlineBadgeCta_notCut_allWidths`: khung 320, 360, 411 dp, cỡ chữ 2.0, chữ dài và chữ ngắn: tiêu đề còn ít nhất 4 ký tự trước "…", nhãn "Ad" hiện đủ, CTA "Install" hiện đủ, CTA dài dừng ở 120 dp, thẻ giữ chiều cao của khung (90, 106, 126 dp), mọi phần nằm trong thẻ |
| KB-2 cỡ chữ 2.0 | Compose UI test | `NativeAdFontScaleTest` (câu có sẵn đổi số theo mục 3.5): medium giữ 400 dp tới cỡ chữ 2.0, media không dưới 120 dp |
| KB-17 tiêu chí 5, D-04: khoảng chừa | Compose UI test | `BottomAdBarTest.nativeReservation_followsScreenWidth`: 142 dp ở 411 dp; lớp con khung 360 dp: 122 dp; bằng chiều cao khe đo được + 16 dp; đối chứng: banner vẫn chừa chiều cao khung |
| KB-4 tiêu chí 1, KB-32: lớp thu gọn | Compose UI test | `CollapsibleNativeAdTest.expanded_heightIsMediaAreaPlusSlot`: 299 dp ở 411 dp, MediaView 161 dp, phần nội dung cao 126 dp; lớp con khung 360 dp: 272 dp |
| KB-4 tiêu chí 1: nút thu gọn | Compose UI test | `CollapsibleNativeAdTest.collapseButton_27dpTopStart_touch48dp`: nút vẽ 27 dp, cách mép trái và mép trên 4 dp, vùng chạm ít nhất 48 dp; góc trên phải 24 dp không có node chạm được |
| KB-4: AdChoices xin góc trên phải | Unit test | `NativeRequestOptionsTest.collapsible_adChoicesTopRight_likeMedium` (mục 3.5) |
| KB-4 tiêu chí 2, KB-17: thu gọn lộ khe đáy theo bề rộng | Compose UI test | `CollapsibleNativeAdTest` (test có sẵn của luồng thu gọn) thêm câu: khe dưới cao 126 dp |
| Giao diện native nhỏ, medium, khung chờ, lớp thu gọn, thanh đáy | Screenshot test | Ghi lại các ảnh `core/ads` của mục 3.2 |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi` | `SampleScreen_ready_small.png`, `SampleScreen_bar-expanded_small.png`, `CollapsibleNativeAd_expanded_small.png`, `NativeAdCard_font-scale-2_small.png`, `AdGalleryScreen_default_small.png` (ghi lại); nút "Tap" của `SampleScreen` không bị khe đáy che |
| Gập mở ra | Screenshot test khung `w673dp-h841dp-xxhdpi` | `SampleScreenScreenshotTest.ready_light_unfolded` → `SampleScreen_ready_light_unfolded.png` (mới, khe 200 dp) |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, `scripts/design-compare.sh ads`, rà soát độc lập theo mục 7 của mẫu thiết kế | `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-013, T-053): `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Không đổi: chữ của app trên quảng cáo chỉ là nhãn "Ad" (`labelSmall` trong bảng chữ; cỡ 13 sp ghi đè theo mục 1 của thiết kế), test có sẵn `NativeAdCardTest` kiểm nhãn |
| KB-1, KB-4 với quảng cáo thật (MediaView, AdChoices của SDK ở góc trên phải lớp thu gọn) | Kiểm tra thủ công (máy thật: quảng cáo thật) | Trên bản staging, màn `SampleScreen` có thanh đáy: khe đáy cao theo bề rộng máy (ví dụ khoảng 126 dp ở máy 411 dp), lớp thu gọn có AdChoices góc trên phải, nút thu gọn góc trên trái, chạm nút thì lớp ẩn |
| KB-32 trên điện thoại gập | Kiểm tra thủ công (máy thật: gập có bản lề) | Mở máy gập: activity dựng lại, khe đáy 200 dp ở màn trong, nội dung chừa đúng |

Lệnh ghi ảnh (sau khi test viết trước đã đỏ đúng lý do và code đã xong):

```
./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun
./gradlew :feature:sample:recordRoborazziDebug :feature:sample:testDebugUnitTest --rerun
./gradlew :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun
git status --short -- '*/src/test/screenshots/*'
```

Chỉ giữ các ảnh của mục 3.2; ảnh khác đổi vì nhiễu vẽ thì `git checkout -- <ảnh>`. Rồi `scripts/design-compare.sh ads` và mở các ảnh `_compare.png`.

**Ngoại lệ viết test trước:** ghi lại ảnh chuẩn screenshot (ảnh chỉ có sau khi code xong); sửa `ads-guide.md`, `ads-business-rules.md` và KDoc (tài liệu); đổi cú pháp gọi `nativeReservation()` ở chỗ gọi (không đổi hành vi, trình biên dịch kiểm).

### 4.1. Chạy thử

Không cần (E dưới 2): nội suy và đọc `LocalConfiguration` lặp lại mẫu có sẵn (`AdSlot.kt`, `MediaShrinksFirstLayout`). Số ở mục 3.5 lấy từ hằng và câu khẳng định trong code hiện tại, không cần đo thêm.

Kết quả `scripts/existing-tests.py` (2026-11-15): xem dòng cuối của mục này sau khi chạy.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi kích thước hiển thị của ba định dạng native và góc AdChoices xin cho lớp thu gọn, mục 1 điều 1 của risk-scoring.md); module core dùng chung `:core:ads` (điều 7).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module (`:core:ads`, `:feature:sample`, `:app`), đổi giao diện công khai `BottomAdBarDefaults.nativeReservation` |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, thiết kế T-064 đã duyệt có số chi tiết; vài chi tiết AI phải chọn: giới hạn CTA, native nhỏ đứng riêng, làm tròn, góc AdChoices (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi kích thước có test Robolectric; quảng cáo thật (MediaView, AdChoices của SDK) và máy gập kiểm tay |
| D. Ảnh hưởng chéo | 3 | Khoảng chừa của thanh đáy đổi trên mọi màn có thanh đáy (feature dựa vào), nhiều ảnh chuẩn của `:app` và `:feature:sample` |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn (đọc kích thước màn hình như `AdSlot.kt`, `MediaShrinksFirstLayout`) |
| **Tổng** | 9 | **Mức: Cao** (cả do điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `BottomAdBar.kt`, `NativeAdSlot.kt`, `NativeRequestOptions.kt` của `:core:ads`; `SampleScreen.kt`; ảnh chuẩn của `:app` (trang xem thử).
- Task khác có thể xung đột: plan khác của feature `ads` chưa chạy mà sửa `NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `BottomAdBar.kt` hay các ảnh chuẩn trên (ví dụ T-086, T-087, T-097, T-099, T-103 nếu còn chưa merge): khi lập danh sách chạy, `run-check.py` báo file trùng; app nào đã đọc `BottomAdBarDefaults.nativeReservation` phải đổi sang hàm khi đồng bộ dự án mẫu.
- Đề xuất thứ tự chạy: sau mọi task đang chờ cùng sửa các file trên trong cùng danh sách, để ảnh chuẩn ghi một lần sau cùng; trước task nào dựa vào số 80 hay 250 dp.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn của `NativeAdCard_card_*`, `_full_*`, `_step_*`, `NativeFlowStep_*`, `NativeFullScreen*` hay trang `AdGalleryScreen` ngoài mục 3.2 lệch thật (không phải nhiễu vẽ): dừng, vì `STEP`, `CARD`, `FULL` phải giữ kích thước (KB-32).
- `scripts/design-compare.sh ads` cho thấy khe đáy hay lớp thu gọn lệch thiết kế quá 4 dp theo chiều cao sau khi code theo số của mục 3.1: dừng, vì spec, thiết kế và plan khác nhau.

## 8. Câu hỏi cho người duyệt

1. **Nút thu gọn 27 dp góc trên trái và AdChoices góc trên phải.** Ticket chỉ ghi kích thước, nhưng thiết kế T-064 và câu hỏi 48 đã chốt vị trí này, còn code vẫn vẽ nút 32 dp góc trên phải và xin `ADCHOICES_TOP_LEFT`; chưa ticket nào làm. Đề xuất: làm trong T-065 (cùng lần ghi lại ảnh chuẩn), gồm đổi góc AdChoices xin từ SDK. Phương án khác: để một ticket riêng, plan bỏ bước đó và ảnh `_collapsible-expanded_` lệch thiết kế ở góc tới khi ticket kia xong.
2. **Native nhỏ ở khe đứng riêng và hàng trong danh sách.** Thiết kế đặt số "khe đáy theo bề rộng màn hình"; `ads-guide.md` ghi native nhỏ nói chung "cao theo bề rộng màn hình, cùng một hàm". Đề xuất: mọi `NativeAdSize.SMALL` dùng cùng hàm và bố cục (một component `Native/Small`). Phương án khác: khe đứng riêng giữ 80 dp và bố cục cũ, thêm biến thể riêng.
3. **Làm tròn.** Nội suy cho số lẻ (125,75 dp ở 411 dp); thiết kế ghi 126. Đề xuất: làm tròn tới dp nguyên gần nhất. Phương án khác: giữ số lẻ (ảnh chuẩn có thể lệch nửa điểm ảnh giữa các máy).
4. **Giới hạn 120 dp của CTA native nhỏ (T-060).** Thiết kế ghi CTA "rộng theo chữ", không ghi trần. CTA 18 sp dài không có trần thì chiếm hết chỗ của tiêu đề (lỗi T-060 sửa). Đề xuất: giữ trần 120 dp, giữ hai test 120 dp của T-060; chỉ viết lại câu "CTA ngắn rộng đúng 80 dp" thành "CTA ngắn hiện đủ chữ, rộng 80 tới 120 dp" (chữ 18 sp làm "Install" rộng hơn 80 dp) và câu chiều cao 80 dp thành 126 dp. Phương án khác: bỏ trần, viết lại hai test theo ADR-0031 với điều kiện tiêu đề còn ít nhất 4 ký tự và "Ad" hiện đủ.
5. **Nguồn chiều cao màn hình của medium.** Plan đọc `Configuration.screenHeightDp` (Robolectric khung h891 cho 891). Trên máy Android 14 trở xuống số này trừ thanh hệ thống, nên medium thấp hơn vài dp so với máy Android 15 cùng màn. Đề xuất: chấp nhận, như banner đang đọc `screenWidthDp`. Phương án khác: đọc `LocalWindowInfo.current.containerSize`.
6. **Chạy thử.** Plan không chạy `scripts/check.sh` vì E là 1; số đo của mục 3.5 lấy từ code. Đề xuất: như vậy.
