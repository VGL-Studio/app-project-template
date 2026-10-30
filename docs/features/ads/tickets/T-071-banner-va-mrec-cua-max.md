---
id: T-071
title: Banner và MREC của MAX
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31, KB-18, KB-29]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-068, T-062, T-054]
sprint:
plan: features/ads/plans/T-071-banner-va-mrec-cua-max.md
auto_run: 
created: 2026-10-29
---

# T-071: BANNER VÀ MREC CỦA MAX

## Mô tả

Banner cố định và banner MREC lấy quảng cáo của MAX theo thứ tự `ads_mediation_order`, cùng luật banner B-01 tới B-06 và dừng khi bị che (KB-27); MAX tự làm mới và tạm dừng tự làm mới khi có quảng cáo toàn màn hình như JBase.

## Phạm vi

- View banner, MREC của MAX trong composable của `:core:ads`; khung chừa chỗ như banner của AdMob.
- Nền tảng thứ nhất không có quảng cáo thì khung thử nền tảng thứ hai, không nhảy chiều cao.
- Banner thu gọn giữ AdMob (MAX không có banner thu gọn).

**Không bao gồm:**

- Native (không qua MAX).

## Rủi ro sơ bộ

Module core, định dạng quảng cáo (điểm chặn 1, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
