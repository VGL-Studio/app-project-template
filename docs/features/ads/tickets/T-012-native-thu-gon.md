---
id: T-012
title: Native thu gọn được
feature: ads
spec: features/ads/spec.md
scenarios: [KB-4]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-010]
sprint:
plan:
created: 2026-10-21
---

# T-012: NATIVE THU GỌN ĐƯỢC

## Mô tả

Native ở đáy màn hình, mở dạng medium, thu gọn thành small khi người dùng chạm nút thu gọn (ít nhất 48 dp, không cùng góc với AdChoices), không tự mở lại khi còn ở màn hình đó.

## Phạm vi

- Composable thu gọn được, trạng thái thu gọn giữ trong màn hình.
- Test giao diện: chạm nút thì thu gọn; quảng cáo mới không mở lại.

**Không bao gồm:**

- Màn hình xem thử (T-014).

## Rủi ro sơ bộ

Quảng cáo trong module core; dễ vi phạm chính sách nếu nút thu gọn sát AdChoices hay tự mở lại.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
