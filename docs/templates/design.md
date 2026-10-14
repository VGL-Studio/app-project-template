---
feature: <ten-feature>
status: draft                # draft | approved
spec: features/<ten-feature>/spec.md
claude_design: <đường dẫn tới dự án trên Claude Design>
designer: <chủ feature>
approved_by: <người phụ trách sản phẩm>
approved_at: YYYY-MM-DD
---

# THIẾT KẾ: <TÊN FEATURE>

<!--
Thư mục features/<ten-feature>/design/ chứa:
- README.md này.
- Gói chuyển giao xuất từ Claude Design, giữ nguyên như khi xuất.
- Ảnh chụp từng màn hình đã duyệt, đặt tên theo mã màn hình, ví dụ MH-1-default.png.
Bản mẫu Claude Design dựng bằng công nghệ web. AI dùng nó làm ý đồ thiết kế để viết lại bằng Compose, không chép code.
-->

## 1. Các màn hình

| Mã | Tên màn hình | Kịch bản | Ảnh |
| --- | --- | --- | --- |
| MH-1 | <tên> | KB-1 | `MH-1-default.png` |

## 2. Trạng thái của từng màn hình

| Màn hình | Trạng thái | Ảnh | Ghi chú |
| --- | --- | --- | --- |
| MH-1 | Đang tải | `MH-1-loading.png` | |
| MH-1 | Chưa có dữ liệu | `MH-1-empty.png` | |
| MH-1 | Lỗi, không có mạng | `MH-1-error.png` | |
| MH-1 | Chế độ tối | `MH-1-dark.png` | |

## 3. Thành phần dùng

- **Có sẵn trong `:core:designsystem`:** <tên thành phần>
- **Thành phần mới cần thêm vào hệ thống thiết kế:** <tên, mô tả>. Thêm thành phần mới là thay đổi module core, plan sẽ được xếp rủi ro cao.

## 4. Luồng thao tác

<Từ màn hình nào sang màn hình nào, bằng thao tác gì. Có thể dùng danh sách: MH-1, bấm "Bắt đầu", sang MH-2.>

## 5. Vị trí quảng cáo

<Trùng với mục Quảng cáo trong spec. Đánh dấu vị trí trên ảnh màn hình. Vùng quảng cáo phải tách bạch với nội dung và không sát nút bấm. Ghi "Không có" nếu không có.>

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** <bố cục, thứ tự thành phần, nội dung chữ, màu theo hệ thống thiết kế>
- **Được phép khác:** <hiệu ứng chuyển động, khoảng cách nhỏ theo lưới của hệ thống thiết kế, cách hiển thị riêng của Android>

## 7. Tiêu chí khi duyệt

- [ ] Dùng đúng màu, chữ, thành phần của hệ thống thiết kế.
- [ ] Đủ các trạng thái: đang tải, chưa có dữ liệu, lỗi, chế độ tối.
- [ ] Vùng chạm đủ lớn, chữ dễ đọc.
- [ ] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [ ] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.
