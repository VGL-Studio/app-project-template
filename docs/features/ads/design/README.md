---
feature: ads
status: approved             # draft | approved
spec: features/ads/spec.md
tool: figma
link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ (App Template: Design System, section "Ads" trên trang "Screens / Ads")
designer: AI (Figma MCP), Nhật duyệt
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: QUẢNG CÁO NATIVE

<!--
Quy trình: docs/ops/design-workflow.md (mục 8: AI dựng, người duyệt). Quyết định: ADR-0008, ADR-0020, ADR-0022.
Ảnh và số liệu sinh bằng scripts/figma-export.py từ section "Ads" (node 7:239). Component nằm ở trang
"Components / Native ads" của cùng file; nền móng (biến màu Light/Dark, khoảng cách, bo góc, kiểu chữ) dựng từ
docs/design-system/tokens.json.
-->

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản | Ticket |
| --- | --- | --- | --- | --- |
| `SampleScreen` | Sẵn sàng, native small ở đáy | `SampleScreen_ready_light` | KB-1 | T-010 |
| `SampleScreen` | Sẵn sàng, chế độ tối | `SampleScreen_ready_dark` | KB-1 | T-010 |
| `SampleScreen` | Quảng cáo đang tải | `SampleScreen_ad-loading_light` | KB-1 | T-010 |
| `AdGalleryScreen` (chỉ staging) | Small, medium, nút gọi quảng cáo toàn màn hình, Ad Inspector | `AdGalleryScreen_default_light` | KB-1, KB-2, KB-8 | T-014 |
| `AdGalleryScreen` | Như trên, chế độ tối | `AdGalleryScreen_default_dark` | KB-1, KB-2, KB-8 | T-014 |
| `AdGalleryScreen` | Native trong danh sách (khung minh họa với N = 3) | `AdGalleryScreen_inlist_light` | KB-3 | T-014 (T-011 làm phần chèn, có test riêng) |
| `AdGalleryScreen` | Native thu gọn được, đang mở | `AdGalleryScreen_collapsible-expanded_light` | KB-4 | T-030 |
| `AdGalleryScreen` | Native thu gọn được, đã thu gọn | `AdGalleryScreen_collapsible-collapsed_light` | KB-4 | T-030 |
| `NativeFullScreenAdScreen` | Native toàn màn hình | `NativeFullScreenAdScreen_default_light` | KB-5 | T-013 |
| `NativeFullScreenAdScreen` | Như trên, chế độ tối | `NativeFullScreenAdScreen_default_dark` | KB-5 | T-013 |

Ảnh `SampleScreen_disabled_light` (đã có) không đổi: trạng thái tắt không có quảng cáo.

**Vẽ lại theo kích thước AdsKit (T-035, 2026-10-25, anh Nhật duyệt).** Component trên trang "Components / Native ads" theo `docs/ops/ads-business-rules.md`, mục 5.7:

- `Native/Small` (khe đáy): cao 80 dp, icon 36 dp, CTA cao 34 dp chữ 13 sp, nhãn "Ad" 10 sp.
- `Native/Collapsible`: lớp mở cao 250 dp, media lấp phần còn lại, icon 32 dp, tiêu đề 13 sp, CTA 36 dp, nút thu gọn 32 dp (biểu tượng 20 dp) cách góc trên phải 4 dp, nền đen 40%. Thu gọn thì khe đáy còn native nhỏ 80 dp. AdChoices đặt ở góc trên trái vì nút thu gọn giữ góc trên phải.
- `Native/Medium`: icon 40 dp, nền mờ sau media, hàng sao, CTA 44 dp; biến thể `CTA=Large` (thuộc tính `cta_large`): CTA 56 dp, chữ 17 sp, không viết hoa toàn bộ.
- `Native/FullScreen`: CTA 52 dp; hai biến thể `After interstitial` (nút đóng 36 dp) và `Flow step` (không có nút điều khiển, app tự đặt nút Tiếp).
- Mới: `Native/Step` (thẻ ở bước của luồng, 250 dp, icon 32 dp, CTA 36 dp) và `Native/Card` (thẻ dọc trong tập hợp; 180 x 300 dp là ví dụ, ô do lưới quyết định), mỗi component có trạng thái chờ.
- Cỡ chữ 13, 10 và 17 sp không có trong bảng kiểu chữ: ghi đè trên component, code đặt cỡ chữ riêng trong composable quảng cáo của `:core:ads`.

Ảnh đã đổi theo đợt này, thay ảnh của đợt 2: mọi ảnh trong bảng trên (khe đáy 80 dp đặt cách đáy 16 dp, lớp thu gọn cách đáy 16 dp, cùng mép dưới với khe, medium và toàn màn hình theo số mới), và `NetworkLostScreen_default_light`, `NetworkLostScreen_default_dark` của feature network (khe đáy). Ticket code ghi lại ảnh chuẩn screenshot theo các ảnh này: T-027 (khe đáy và lớp thu gọn), T-030 (thanh đáy), T-032 (native trong tập hợp và bước của luồng). `Native/Step` và `Native/Card` chưa có màn hình riêng; ticket dùng chúng thêm ảnh màn hình khi cần.

## 2. Bảng chữ

Chữ của quảng cáo (tiêu đề, nhà quảng cáo, mô tả, CTA, ảnh) do SDK trả về; trên thiết kế là chữ minh họa, không vào `strings.xml`. Chữ của app:

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Mọi native | Nhãn quảng cáo | Ad | `labelSmall` |
| `SampleScreen` | Tiêu đề, nhãn, số, nút | Sample feature, Times tapped, 3, Tap | như hiện có |
| `AdGalleryScreen` | Tiêu đề | Ad gallery (staging) | `titleLarge` |
| `AdGalleryScreen` | Nhãn khu vực | Native small, Native medium | `titleSmall` |
| `AdGalleryScreen` | Nút | Interstitial, Native full screen, Open Ad Inspector | `labelLarge` đậm 600 (`PrimaryButton`) |
| `AdGalleryScreen`, trong danh sách | Tiêu đề, mục | In-list ads (every 3 items), List item 1 đến 7 | `titleLarge`, `bodyLarge` |
| `AdGalleryScreen`, thu gọn | Tiêu đề, mục | Collapsible ad, List item 1 đến 6 | `titleLarge`, `bodyLarge` |

Chữ của màn hình xem thử chỉ ở staging, tiếng Anh, vẫn đặt trong `strings.xml` của source set staging.

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách |
| --- | --- | --- | --- | --- |
| Khung native small, medium, thu gọn | composable native của `:core:ads` (T-010) | nền `surfaceContainerLow`, viền 1 dp `outlineVariant` | | bo `MaterialTheme.shapes.medium` (12 dp); lề 12 dp |
| Nhãn "Ad" | trong composable native, đầu dòng mô tả dưới tiêu đề | nền `tertiary`, chữ `onTertiary` | `labelSmall` (small: cỡ 10 sp) | bo 4 dp, lề ngang 4 dp |
| Tiêu đề quảng cáo | asset headline | `onSurface` | `titleSmall` (small, medium, đầy đủ), `titleMedium` (toàn màn hình) | |
| Nhà quảng cáo, mô tả | asset advertiser, body | `onSurfaceVariant` | `bodySmall`, `bodyMedium` | |
| CTA quảng cáo | asset call to action | nền `tertiaryContainer`, chữ `onTertiaryContainer` | `labelLarge` (small: cỡ 13 sp; medium `cta_large`: 17 sp) | cao 34 dp ở small, 44 dp ở medium (56 dp khi `cta_large`), 36 dp ở đầy đủ và thẻ dọc, 52 dp ở toàn màn hình; bo 8 dp |
| Icon quảng cáo | asset icon | | | 36 dp ở small, 40 dp ở medium và đầy đủ, 48 dp ở toàn màn hình; bo 8 dp |
| Media | `MediaView` | nền chờ `surfaceContainer` | | ít nhất 120 x 120 dp; medium cao 196 dp, đầy đủ 200 dp, lớp thu gọn lấp phần còn lại (134 dp) |
| AdChoices | do SDK vẽ, góc trên phải (lớp thu gọn: góc trên trái) | | | không đè, không vẽ lại |
| Nút thu gọn | trong lớp thu gọn của thanh đáy | nền `scrim` độ mờ 40%, icon trắng (ngoại lệ có tên `CollapseIconTint`, plan T-030, câu hỏi 8) | | vẽ 32 dp tròn cách góc trên phải 4 dp, icon 20 dp; vùng chạm 48 dp do Compose tự nới |
| Nút đóng toàn màn hình | trong `NativeFullScreenAdScreen` | nền `surfaceContainerHigh`, icon `onSurface` | | vẽ 36 dp tròn tại (16, 24), vùng chạm 48 dp do Compose tự nới, góc trên trái |
| Trạng thái đang tải | trong composable native | khối `surfaceContainerHighest` | | giữ đúng chiều cao của quảng cáo (small 80 dp, medium 410 dp, đầy đủ 418 dp, thẻ dọc 300 dp hay cỡ ô) |
| Nút của app | `PrimaryButton` | `primary`, `onPrimary` | `labelLarge` đậm 600 | |

- **Giá trị "ngoài hệ thống" trong `context/`:** khoảng cách 2, 6 và 12 dp bên trong quảng cáo (giữa tiêu đề và dòng nhà quảng cáo, giữa nhãn "Ad" và chữ, lề trong khung). Plan T-010 quyết: giữ làm hằng số riêng của composable native trong `:core:ads` (kích thước của một component, như `MinHeight` của `PrimaryButton`), không thêm token.
- **Thành phần mới cần thêm vào hệ thống thiết kế:** các composable native nằm ở `:core:ads` (ADR-0022), dùng token của `:core:designsystem`; không thêm thành phần vào `:core:designsystem`.

## 4. Luồng thao tác

- `SampleScreen`: vào màn hình, native small ở đáy hiện trạng thái đang tải rồi tới quảng cáo.
- `AdGalleryScreen` (staging): bấm "Interstitial" hay "Native full screen" thì gọi quảng cáo toàn màn hình; bấm "Open Ad Inspector" thì mở Ad Inspector của AdMob.
- Thu gọn: vào màn hình thì lớp thu gọn 250 dp mở trên khe đáy; chạm nút thu gọn thì lớp ẩn, lộ khe native nhỏ 80 dp bên dưới; lớp tự mở lại sau `time_expand_collap_ad` giây (mặc định 30, 0 là không mở lại), đếm từ lúc chạm (CL-03, T-030).
- `NativeFullScreenAdScreen`: chạm nút đóng hay nút quay lại của máy thì về màn hình trước.

## 5. Vị trí quảng cáo

| Vị trí | Định dạng | Màn hình | Ghi chú |
| --- | --- | --- | --- |
| `sample_native_small` (thay `sample_bottom_banner`) | Native small | `SampleScreen` | Ghim ở đáy, cách mép dưới 16 dp, không nằm ngay dưới nút "Tap" |
| Các vị trí của màn hình xem thử | Mọi định dạng | `AdGalleryScreen` | Chỉ staging |

Mọi vùng quảng cáo có nền và viền tách khỏi nội dung; cách nút, mục danh sách của app ít nhất 16 dp; CTA khác `PrimaryButton` (màu tertiary nhạt, cao 36 dp, bo 8 dp so với primary, 52 dp, bo 16 dp).

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục và thứ tự thành phần của từng cỡ native, vị trí AdChoices, nút thu gọn, nút đóng, chiều cao giữ chỗ khi tải, vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** chữ và ảnh của quảng cáo (do SDK), cách vẽ chữ của hệ thống, chênh lệch nhỏ theo lưới 4 dp. Screenshot test dùng quảng cáo giả trong test để có chữ và ảnh cố định.

## 7. Tiêu chí khi duyệt

- [x] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [x] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [x] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp (kiểm khi code).
- [x] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640) (kiểm khi code, ảnh `_small`).
- [x] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [x] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Màn hình xem thử (T-014, 2026-10-23). Ảnh chuẩn ở `app/src/test/screenshots/` (`:app`, staging); script so sánh bọc sẵn chưa nhận `:app` nên chạy thẳng `scripts/design-compare/DesignCompare.java` (plan T-014, mục 3.2). Ảnh so sánh trong `review/`; kết quả chỉ ghi ở mục này, `review/summary.md` không đổi.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_default_light` | 1.9% | Khác chính: hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế (plan T-014, câu hỏi 7). Quảng cáo giả: ô AdChoices không có chữ "i", media không có nhãn "Media", medium hiện "Advertiser or short body" thay "Advertiser". Vị trí tiêu đề, nhãn, quảng cáo, nút trùng thiết kế |
| `AdGalleryScreen_default_dark` | 1.9% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 1.6% | Thứ tự và khoảng cách trùng (small sau mục 3, medium sau mục 6, cách 16 dp); viền mục chỉ ở cạnh dưới; khác ở chữ quảng cáo giả như trên |
| `AdGalleryScreen_collapsible-expanded_light` | 0.7% | Ghim đáy, cách mép 16 dp. Dạng mở không vẽ ô AdChoices giả (dạng thu gọn có), đã biết từ T-012; chữ quảng cáo giả như trên |
| `AdGalleryScreen_collapsible-collapsed_light` | 0.7% | Trùng thiết kế, khác ở chữ "i" của ô AdChoices giả |

Nhận xét của rà soát độc lập (RUN-20261024-01): chữ, thứ tự và vai trò màu (nút toàn màn hình `primary`, nút chuyển trang viền, bản tối) trùng thiết kế; chỉ khác ở hàng nút chuyển trang đã duyệt và quảng cáo giả; vị trí lệch nhiều nhất khoảng 1 px. Không có điểm chặn.

Native theo kích thước AdsKit (T-027, 2026-10-26), so với thiết kế vẽ lại (T-035). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho từng module (`core/ads`, `feature/sample`, `app`), chép ảnh so sánh vào `review/`; `review/summary.md` không đổi.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `SampleScreen_ready_light` | 1.3% | Khe đáy 80 dp, icon 36, CTA 34 dp, nhãn "Ad" đầu dòng mô tả trùng thiết kế. Khác: nút "Tap" thấp hơn khoảng 2 dp (không thuộc task này), ô AdChoices giả không có chữ "i" |
| `SampleScreen_ready_dark` | 1.3% | Như bản sáng |
| `SampleScreen_ad-loading_light` | 0.9% | Khung chờ 80 dp, khối CTA 80 x 34 dp trùng thiết kế |
| `NativeFullScreenAdScreen_default_light` | 0.5% | Nút đóng 36 dp tại (16, 24), media từ y = 76, icon 48, CTA 52 dp trùng thiết kế; khác ở chữ "i" của AdChoices giả |
| `NativeFullScreenAdScreen_default_dark` | 0.5% | Như bản sáng |
| `AdGalleryScreen_default_light` | 14.7% | Native small trùng thiết kế. Lệch dự kiến: thiết kế vẽ medium mới (cao 410 dp, hàng sao, nền mờ sau media, CTA 44 dp), code giữ medium 386 dp (plan T-027, Q3; T-028 đổi), nên mọi thứ dưới thẻ medium lệch khoảng 24 dp. Hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế như T-014 |
| `AdGalleryScreen_default_dark` | 14.5% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 3.1% | Dòng native small 80 dp (cả dòng 112 dp) trùng thiết kế; phần lệch ở thẻ medium phía dưới như trên |

`AdGalleryScreen_collapsible-collapsed_light` cũng đổi (dạng thu gọn 80 dp) nhưng là ảnh thiết kế của T-030: ảnh so sánh và dòng ở bảng trên chưa ghi lại ở task này.

Native medium theo thiết kế vẽ lại (T-028, 2026-10-26): medium cao 410 dp (hàng đầu 56 dp có hàng sao, CTA 44 dp, lớp nền sau media). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `app`, chép ba ảnh so sánh vào `review/`; `review/summary.md` không đổi. Phần lệch 24 dp của medium mà T-027 ghi ở bảng trên không còn.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_default_light` | 2.0% | Thẻ medium 410 dp, vị trí tiêu đề, media, mô tả, CTA và các nút bên dưới trùng thiết kế. Khác: quảng cáo giả của màn xem thử không có điểm sao nên không vẽ sao (hàng sao vẫn giữ 16 dp), media giả che lớp nền sau media và không có nhãn "Media", "Advertiser or short body" thay "Advertiser", ô AdChoices giả không có chữ "i"; hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế như T-014 |
| `AdGalleryScreen_default_dark` | 2.0% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 1.7% | Dòng small 112 dp và dòng medium 442 dp (410 + 16 + 16) đúng vị trí; khác ở chữ quảng cáo giả như trên |

Thanh đáy và lớp native thu gọn 250 dp (T-030, 2026-10-26). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `app`, chép hai ảnh `AdGalleryScreen_collapsible-*_compare.png` vào `review/`; `review/summary.md` không đổi. Hai dòng `AdGalleryScreen_collapsible-*` ở bảng của T-014 phía trên là kết quả cũ (dạng 342 dp).

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_collapsible-expanded_light` | 0.7% | Lớp mở 250 dp ở y 625 tới 875, cách mép 16 dp; media 134 dp, icon 32 dp, tiêu đề 13 sp, CTA 36 dp, nút thu gọn 32 dp nền `scrim` 40% cách góc trên phải 4 dp trùng thiết kế. Khác: ô AdChoices giả (góc trên trái) không có chữ "i", media giả không có nhãn "Media", hai mép CTA lệch khoảng 1 dp (lề 12 dp tính từ mép ngoài, như các native khác) |
| `AdGalleryScreen_collapsible-collapsed_light` | 0.7% | Lớp ẩn, khe đáy 80 dp ở y 795 tới 875 trùng thiết kế; khác ở chữ "i" của ô AdChoices giả |
