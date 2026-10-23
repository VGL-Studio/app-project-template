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
| `AdGalleryScreen` | Native thu gọn được, đang mở | `AdGalleryScreen_collapsible-expanded_light` | KB-4 | T-012 hay T-014 (câu hỏi ở plan hai ticket) |
| `AdGalleryScreen` | Native thu gọn được, đã thu gọn | `AdGalleryScreen_collapsible-collapsed_light` | KB-4 | T-012 hay T-014 (câu hỏi ở plan hai ticket) |
| `NativeFullScreenAdScreen` | Native toàn màn hình | `NativeFullScreenAdScreen_default_light` | KB-5 | T-013 |
| `NativeFullScreenAdScreen` | Như trên, chế độ tối | `NativeFullScreenAdScreen_default_dark` | KB-5 | T-013 |

Ảnh `SampleScreen_disabled_light` (đã có) không đổi: trạng thái tắt không có quảng cáo.

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
| Nhãn "Ad" | trong composable native | nền `tertiary`, chữ `onTertiary` | `labelSmall` | bo 4 dp, lề ngang 4 dp |
| Tiêu đề quảng cáo | asset headline | `onSurface` | `titleSmall` (small, medium), `titleMedium` (toàn màn hình) | |
| Nhà quảng cáo, mô tả | asset advertiser, body | `onSurfaceVariant` | `bodySmall`, `bodyMedium` | |
| CTA quảng cáo | asset call to action | nền `tertiaryContainer`, chữ `onTertiaryContainer` | `labelLarge` | cao 36 dp (48 dp ở toàn màn hình), bo 8 dp |
| Icon quảng cáo | asset icon | | | 40 dp (48 dp ở toàn màn hình), bo 8 dp |
| Media | `MediaView` | nền chờ `surfaceContainer` | | ít nhất 120 x 120 dp; medium cao 196 dp, thu gọn mở 160 dp |
| AdChoices | do SDK vẽ, góc trên phải | | | không đè, không vẽ lại |
| Nút thu gọn | trong composable thu gọn | icon `onSurface` | | 48 dp, góc trên trái |
| Nút đóng toàn màn hình | trong `NativeFullScreenAdScreen` | nền `surfaceContainerHigh`, icon `onSurface` | | 48 dp tròn, góc trên trái |
| Trạng thái đang tải | trong composable native | khối `surfaceContainerHighest` | | giữ đúng chiều cao của quảng cáo (small 72 dp, medium 386 dp) |
| Nút của app | `PrimaryButton` | `primary`, `onPrimary` | `labelLarge` đậm 600 | |

- **Giá trị "ngoài hệ thống" trong `context/`:** khoảng cách 2, 6 và 12 dp bên trong quảng cáo (giữa tiêu đề và dòng nhà quảng cáo, giữa nhãn "Ad" và chữ, lề trong khung). Plan T-010 quyết: giữ làm hằng số riêng của composable native trong `:core:ads` (kích thước của một component, như `MinHeight` của `PrimaryButton`), không thêm token.
- **Thành phần mới cần thêm vào hệ thống thiết kế:** các composable native nằm ở `:core:ads` (ADR-0022), dùng token của `:core:designsystem`; không thêm thành phần vào `:core:designsystem`.

## 4. Luồng thao tác

- `SampleScreen`: vào màn hình, native small ở đáy hiện trạng thái đang tải rồi tới quảng cáo.
- `AdGalleryScreen` (staging): bấm "Interstitial" hay "Native full screen" thì gọi quảng cáo toàn màn hình; bấm "Open Ad Inspector" thì mở Ad Inspector của AdMob.
- Thu gọn: vào màn hình thì quảng cáo ở dạng mở; chạm nút thu gọn thì sang dạng thu gọn; không tự mở lại.
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

Chưa có: chạy `scripts/design-compare.sh ads` khi có ảnh chuẩn screenshot.
