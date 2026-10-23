---
feature: network
status: approved             # draft | approved
spec: features/network/spec.md
tool: figma
link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ (App Template: Design System, section "Network" trên trang "Screens / Ads")
designer: AI (Figma MCP), Nhật duyệt
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: POPUP MẤT MẠNG

<!--
Quy trình: docs/ops/design-workflow.md. Quyết định: ADR-0020, ADR-0024.
Ảnh và số liệu sinh bằng scripts/figma-export.py từ section "Network" (node 7:240). Component `Dialog/NetworkLost` và
`Button/Primary` ở trang "Components / Dialogs" của cùng file.
-->

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản |
| --- | --- | --- | --- |
| `NetworkLostScreen` (popup đè lên màn hình mẫu) | Mất mạng | `NetworkLostScreen_default_light` | KB-1, KB-2 |
| `NetworkLostScreen` | Mất mạng, chế độ tối | `NetworkLostScreen_default_dark` | KB-1, KB-2 |

Tên frame có hậu tố `Screen` theo quy tắc của `figma-export.py` (ADR-0020). Screenshot test dựng popup đè lên `SampleScreen` để có cùng ảnh; tên composable của popup do plan quyết.

## 2. Bảng chữ

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Popup | Tiêu đề | Không có kết nối mạng | `headlineSmall` |
| Popup | Nội dung | Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại. | `bodyMedium` |
| Popup | Nút | Thử lại | `labelLarge` đậm 600 (`PrimaryButton`) |

Bản tiếng Anh tương ứng (spec, câu hỏi 3): "No internet connection", "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.", "Try again".

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách |
| --- | --- | --- | --- | --- |
| Lớp phủ | lớp phủ mặc định của `Dialog` | đen 32% (scrim của Material) | | |
| Hộp thoại | `Dialog` với bề mặt riêng | `surfaceContainerHigh` | | rộng 312 dp, lề 24 dp, cách nhau 16 dp, bo 28 dp (`shapes.extraLarge`) |
| Icon mất wifi | icon vector, Material Symbols Rounded "wifi_off" (Apache 2.0) | nền `secondary` | | 24 dp |
| Tiêu đề, nội dung | `Text` | `onSurface`, `onSurfaceVariant` | `headlineSmall`, `bodyMedium` | căn giữa |
| Nút "Thử lại" | `PrimaryButton` | `primary`, `onPrimary` | `labelLarge` đậm 600 | rộng hết hộp thoại |

- **Giá trị "ngoài hệ thống" trong `context/`:** lề 24 dp và rộng 312 dp của hộp thoại (theo Material 3), màu lớp phủ. Plan quyết: hằng số của composable popup.
- **Thành phần mới cần thêm vào hệ thống thiết kế:** không có; popup nằm ở `:app` hoặc module core do plan chọn (ADR-0024).

## 4. Luồng thao tác

Mất Internet thì popup hiện đè lên màn hình hiện tại, chặn thao tác. Bấm "Thử lại" thì kiểm lại kết nối. Có mạng thì popup tự đóng, màn hình bên dưới giữ nguyên trạng thái.

## 5. Vị trí quảng cáo

Không có.

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục hộp thoại, chữ, vai trò màu và kiểu chữ.
- **Được phép khác:** lớp phủ và bóng theo mặc định của `Dialog` trong Compose, chênh lệch nhỏ theo lưới 4 dp.

## 7. Tiêu chí khi duyệt

- [x] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [x] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [x] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục giống bản mẫu (kiểm khi code).
- [x] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640) (kiểm khi code).
- [x] Không có quảng cáo.
- [x] Không thiết kế để thu hút chủ yếu trẻ em.

## 7a. Thay đổi sau khi duyệt

- 2026-10-23: icon mất wifi đổi sang Material Symbols Rounded "wifi_off" theo quyết định của anh Nhật ở câu hỏi 3 của plan T-017 (code dùng cùng icon); ảnh `screens/` và `context/` xuất lại bằng `scripts/figma-export.py`.

## 8. Kết quả đối chiếu

Chưa có.
