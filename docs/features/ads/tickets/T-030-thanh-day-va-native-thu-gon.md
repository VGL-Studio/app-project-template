---
id: T-030
title: Thanh đáy: native nhỏ hoặc banner, lớp native thu gọn 250 dp tự mở lại
feature: ads
spec: features/ads/spec.md
scenarios: [KB-4, KB-17]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-012, T-027, T-029]
sprint:
plan:
created: 2026-10-23
---

# T-030: THANH ĐÁY: NATIVE NHỎ HOẶC BANNER, LỚP NATIVE THU GỌN 250 DP TỰ MỞ LẠI

## Mô tả

Thanh đáy hai lớp theo D-01 tới D-05 và native thu gọn theo CL-01 tới CL-04 của `docs/ops/ads-business-rules.md`: khe đáy 56 dp (hoặc banner), lớp thu gọn 250 dp chỉ lấp chiều rộng, nút thu gọn 32 dp, tự mở lại sau `time_expand_collap_ad`, làm mới mỗi 60 giây không đổi trạng thái. Thay hành vi "không tự mở lại" của T-012. `:feature:sample` dùng thanh đáy.

## Phạm vi

- Khoảng chừa dưới nội dung bằng đúng chiều cao khe (AR-02).
- Hẹn giờ mở lại tách khỏi làm mới; test đọc trạng thái hiện tại (CL-04).
- Ghi lại ảnh chuẩn screenshot của màn mẫu theo plan.

**Không bao gồm:**

- Màn xem thử (T-036).

## Rủi ro sơ bộ

Module core và màn mẫu; tự mở lại là rủi ro chính sách (ADR-0026). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
