---
id: T-008
title: Giá trị Remote Config cục bộ cho bản staging
feature: ads
spec: features/ads/spec.md
scenarios: [KB-8]
owner: Nhật
status: approved
risk: high
size: S
depends_on: [T-007]
sprint:
plan: features/ads/plans/T-008-gia-tri-cuc-bo-cho-staging.md
created: 2026-10-21
---

# T-008: GIÁ TRỊ REMOTE CONFIG CỤC BỘ CHO BẢN STAGING

## Mô tả

Bản staging dùng Firebase giả nên không nhận giá trị Remote Config nào, quảng cáo không bao giờ hiện. Ticket này cho `RemoteSettings` đọc thêm một bộ giá trị khai báo trong `app/src/staging`, xếp sau giá trị từ Firebase và trước mặc định, như `StagingFeatureFlagsModule` làm với cờ (ADR-0023). Bộ giá trị staging bật mọi vị trí quảng cáo; production không có bộ này.

## Phạm vi

- Nguồn giá trị cục bộ trong `:core:config`, rỗng ở production.
- Module staging khai báo giá trị, bật mọi vị trí quảng cáo của `AdPlacement`.
- Test thứ tự ưu tiên: Firebase, cục bộ, mặc định.

**Không bao gồm:**

- Màn hình xem thử quảng cáo (T-014).

## Rủi ro sơ bộ

Module core `:core:config` và cấu hình DI của `app`. Sai thứ tự ưu tiên có thể làm production đọc giá trị staging: test phải chặn.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
