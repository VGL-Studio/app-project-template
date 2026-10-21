---
id: T-013
title: Native toàn màn hình thay interstitial
feature: ads
spec: features/ads/spec.md
scenarios: [KB-5]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-010, T-009]
sprint:
plan: features/ads/plans/T-013-native-toan-man-hinh.md
created: 2026-10-21
---

# T-013: NATIVE TOÀN MÀN HÌNH THAY INTERSTITIAL

## Mô tả

Một điểm dừng tự nhiên gọi native toàn màn hình như gọi interstitial: nút đóng dùng được ngay, nút quay lại của máy cũng đóng, tính chung giới hạn tần suất (T-009), không liền sau quảng cáo toàn màn hình khác, không chặn người dùng khi chưa có quảng cáo sẵn.

## Phạm vi

- Hàm gọi tương tự `rememberShowInterstitial`; màn hình native toàn màn hình theo thiết kế.
- Tải sẵn khi vị trí bật; dùng chung bộ đếm tần suất.

**Không bao gồm:**

- Màn hình xem thử (T-014).

## Rủi ro sơ bộ

Quảng cáo toàn màn hình trong module core (điểm chặn); chính sách Play về quảng cáo gây khó chịu.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
