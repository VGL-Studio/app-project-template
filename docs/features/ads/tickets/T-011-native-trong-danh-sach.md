---
id: T-011
title: Native trong danh sách
feature: ads
spec: features/ads/spec.md
scenarios: [KB-3]
owner: Nhật
status: backlog
risk: medium
size: M
depends_on: [T-010]
sprint:
plan:
created: 2026-10-21
---

# T-011: NATIVE TRONG DANH SÁCH

## Mô tả

Một hàm của `:core:ads` chèn native small hay medium vào danh sách hay lưới Compose sau mỗi `ads_inlist_every_count` mục, không ở vị trí đầu, không chèn khi danh sách ngắn hơn N mục.

## Phạm vi

- Hàm tính vị trí chèn (hàm thuần, unit test).
- Mở rộng cho `LazyColumn` và lưới; định dạng theo vị trí khai báo.
- Ví dụ trong màn hình xem thử (T-014).

**Không bao gồm:**

- Màn hình xem thử (T-014).

## Rủi ro sơ bộ

Quảng cáo trong module core; chèn sai vị trí có thể đặt quảng cáo như một mục của danh sách.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
