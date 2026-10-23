---
id: T-019
title: Khóa Remote Config quảng cáo theo tên AdsKit
feature: ads
spec: features/ads/spec.md
scenarios: [KB-7, KB-10]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-007, T-008, T-009]
sprint:
plan:
created: 2026-10-23
---

# T-019: KHÓA REMOTE CONFIG QUẢNG CÁO THEO TÊN ADSKIT

## Mô tả

Khóa quảng cáo trong `:core:ads` đổi sang đúng tên của AdsKit và các khóa mới cùng kiểu, theo bảng ở `docs/ops/ads-business-rules.md`, mục 10. Có một bộ cờ quảng cáo đọc từ Remote Config dạng `Flow` cho `:core:ads` và vỏ app. Giá trị cục bộ của staging (T-008) bật đủ khóa mới. Bảng khóa sinh tự động (T-007) liệt kê tên mới.

## Phạm vi

- Khai báo các khóa ở mục 10 bằng `RemoteKey` (T-007), mặc định theo bảng; `is_show_<vị trí>` thay `ads_<vị trí>_enabled`.
- Gỡ khóa và luật của T-009 mà PARTNER không có (ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên), theo câu hỏi 10 của spec; công tắc tổng theo câu hỏi 7.
- Bộ cờ quảng cáo dạng `Flow`; test mặc định và đọc lại sau khi đổi giá trị.
- Cập nhật giá trị cục bộ của staging và sinh lại `docs/remote-config-keys.md`.

**Không bao gồm:**

- Dùng khóa để đổi hành vi định dạng (các ticket sau).

## Rủi ro sơ bộ

Module core `:core:config` và `:core:ads` (điểm chặn của risk-scoring.md). Gỡ luật đã merge ở T-009 cần test chứng minh không còn đường đọc khóa cũ. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
