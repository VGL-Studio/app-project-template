---
id: T-014
title: Màn hình xem thử quảng cáo chỉ ở bản staging
feature: ads
spec: features/ads/spec.md
scenarios: [KB-8]
owner: Nhật
status: backlog
risk: medium
size: M
depends_on: [T-008, T-010, T-011, T-012, T-013]
sprint:
plan:
created: 2026-10-21
---

# T-014: MÀN HÌNH XEM THỬ QUẢNG CÁO CHỈ Ở BẢN STAGING

## Mô tả

Bản staging có một màn hình liệt kê mọi định dạng quảng cáo (native small, medium, trong danh sách, thu gọn, toàn màn hình, interstitial) dùng mã thử, và nút mở Ad Inspector. Bản production không có màn hình này.

## Phạm vi

- Màn hình trong `app/src/staging`, lối vào chỉ có ở staging.
- Vị trí quảng cáo riêng của màn hình xem thử, bật bằng giá trị cục bộ (T-008).

**Không bao gồm:**

- Định dạng mới (các ticket trước).

## Rủi ro sơ bộ

Code chỉ ở biến thể staging; rủi ro lọt vào production nếu đặt sai source set: test kiểm production không có màn hình.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
