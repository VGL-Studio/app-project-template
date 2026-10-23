---
id: T-031
title: Rewarded và hộp thoại xem quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-022, T-023]
sprint:
plan:
created: 2026-10-23
---

# T-031: REWARDED VÀ HỘP THOẠI XEM QUẢNG CÁO

## Mô tả

Khe rewarded và hộp thoại theo R-01 tới R-06 của `docs/ops/ads-business-rules.md`: thưởng trong callback, chặn khi chưa sẵn sàng, chỉ báo khi lỗi mạng, không qua cổng `ads_interval`, chống bấm hai lần. Chữ hộp thoại trong `strings.xml`.

## Phạm vi

- Hàm và composable cho feature; vị trí theo `is_show_rw_get_item`.
- Sự kiện `ad_reward_earned`, `reward_dialog` theo T-023.

**Không bao gồm:**

- Vị trí rewarded trong `:feature:sample` (câu hỏi 13 của spec).

## Rủi ro sơ bộ

Module core, định dạng quảng cáo mới. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
