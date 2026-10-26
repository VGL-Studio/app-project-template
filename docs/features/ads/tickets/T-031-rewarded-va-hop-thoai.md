---
id: T-031
title: Rewarded là hành động có điều kiện, kèm hộp thoại xem quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: [T-022, T-023]
sprint:
plan: features/ads/plans/T-031-rewarded-va-hop-thoai.md
created: 2026-10-23
---

# T-031: REWARDED LÀ HÀNH ĐỘNG CÓ ĐIỀU KIỆN, KÈM HỘP THOẠI XEM QUẢNG CÁO

## Mô tả

Khe rewarded và hộp thoại theo R-01 tới R-06 của `docs/ops/ads-business-rules.md`: thưởng trong callback, chính sách khi chưa sẵn sàng theo vị trí, chỉ báo khi lỗi mạng, không qua cổng `ads_interval`, bấm hai lần ghi `busy`. Chữ hộp thoại trong `strings.xml`.

## Phạm vi

- Hàm và composable chung cho feature; mỗi vị trí rewarded bật bằng `is_show_<mã vị trí>`.
- Sự kiện `ad_reward_earned`, `reward_dialog` theo T-023.
- Rewarded là hành động có điều kiện (MC-05): vị trí khai báo hành động nó giữ và chính sách khi không có quảng cáo (`block` mặc định, `allow`, `allow_after_<n>`, câu hỏi 29); đóng trước khi được thưởng thì luôn chặn. Vị trí `rw_get_item` (khóa `is_show_rw_get_item`) và `rw_export` (khóa `is_show_rw_export`, câu hỏi 34) là hai ví dụ trong test, không phải code riêng.
- Bấm "Xem" lần hai không bị chặn, gọi hiện như AdsKit và ghi cơ hội `busy` (R-06, câu hỏi 24). Lỗi mạng gồm mọi mã lỗi lúc máy không có mạng; cờ xóa đầu mỗi lượt nạp (R-03).
- Test nghiệm thu gợi ý: `AT-RW-*`.

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
