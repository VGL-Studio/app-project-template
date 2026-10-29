---
id: T-067
title: Banner cố định dùng cỡ thích ứng neo đáy thường
feature: ads
spec: features/ads/spec.md
scenarios: [KB-18, KB-32]
owner: Nhật
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-067-banner-thich-ung-thuong.md
auto_run: 
created: 2026-10-29
---

# T-067: BANNER CỐ ĐỊNH DÙNG CỠ THÍCH ỨNG NEO ĐÁY THƯỜNG

## Mô tả

Banner cố định đổi từ `getLargeAnchoredAdaptiveBannerAdSize` sang cỡ thích ứng neo đáy thường như JBase (`getCurrentOrientationAnchoredAdaptiveBannerAdSize`) (ADR-0038, quyết định 3).

## Phạm vi

- Đổi `AdSlot.kt` (dòng 92-94); khung chừa chỗ theo chiều cao mới.
- Ghi lại ảnh chuẩn có banner (liệt kê trong plan); test so chiều cao khung với cỡ mới.
- Sửa `ads-business-rules.md` B-01 kèm ADR-0038.

**Không bao gồm:**

- Banner thu gọn (giữ B-03) và banner MREC (T-062).

## Rủi ro sơ bộ

Module core, định dạng quảng cáo (điểm chặn 1, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
