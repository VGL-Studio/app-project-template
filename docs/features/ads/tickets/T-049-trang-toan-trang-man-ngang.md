---
id: T-049
title: Trang toàn trang của luồng không để trống khi xoay ngang trên điện thoại thật
feature: ads
spec: features/ads/spec.md
scenarios: [KB-20]
owner: Nhật
status: approved
risk: high
size: S
depends_on: [T-041]
sprint:
plan: features/ads/plans/T-049-trang-toan-trang-man-ngang.md
created: 2026-10-28
---

# T-049: TRANG TOÀN TRANG CỦA LUỒNG KHÔNG ĐỂ TRỐNG KHI XOAY NGANG TRÊN ĐIỆN THOẠI THẬT

## Mô tả

FB-20261028-01: trên điện thoại thật, khi xoay ngang, vùng trang toàn trang chỉ cao khoảng 187 dp. Mức này dưới ngưỡng khoảng 200 dp của T-041, nên trang không lấy quảng cáo và để trống. Test của T-041 đo trên khung Robolectric không có thanh hệ thống, nên không thấy lỗi. Sau task, xoay ngang trên điện thoại thường không còn vùng trắng không lời giải thích.

## Phạm vi

- **Plan quyết cách xử lý** khi trang thấp hơn ngưỡng, có đề xuất và câu hỏi cho người. Các phương án:
  - bố cục gọn hơn cho màn ngang, để chạm được ngưỡng;
  - ở màn ngang, thay trang bằng thẻ nhỏ của bước;
  - ẩn hẳn vùng trang và nới phần nội dung của bước;
  - giữ khung chờ thay cho vùng trắng.
- **Test** đo vùng trang ở khung ngang có thanh trạng thái, thanh điều hướng và phần khoét camera, ví dụ khung `873x393 dp` có các phần lề hệ thống tương ứng. Bước đối chứng tái hiện được 187 dp như máy thật (L-029).

**Không bao gồm:**

- Thử trên máy gập (cần máy thật).

## Rủi ro sơ bộ

Đổi bố cục native trong module core quảng cáo (điểm chặn 1 và 7). Nếu chọn thẻ nhỏ thay trang thì đổi cách dùng vị trí quảng cáo ở màn ngang, cần người duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
