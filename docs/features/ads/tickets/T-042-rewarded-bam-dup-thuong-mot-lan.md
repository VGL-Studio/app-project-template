---
id: T-042
title: Rewarded: bấm "Xem quảng cáo" hai lần khi chưa có quảng cáo chỉ làm hành động một lần
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: [T-031]
sprint:
plan: features/ads/plans/T-042-rewarded-bam-dup-thuong-mot-lan.md
created: 2026-10-26
---

# T-042: REWARDED: BẤM "XEM QUẢNG CÁO" HAI LẦN KHI CHƯA CÓ QUẢNG CÁO CHỈ LÀM HÀNH ĐỘNG MỘT LẦN

## Mô tả

Rà soát độc lập của T-031 (RUN-20261026-04, điểm nên sửa chưa sửa): vị trí rewarded có chính sách `allow` (hay `allow_after_<n>` đã tới ngưỡng), quảng cáo chưa sẵn sàng, người dùng bấm "Xem quảng cáo" hai lần nhanh thì `onGranted` chạy hai lần, tức hành động có điều kiện (nhận vật phẩm, xuất file) làm hai lần. Sau task, mỗi lần mở hộp thoại cho làm hành động nhiều nhất một lần.

## Phạm vi

- `rememberRewardedAction` và lượt rewarded (`RewardedTurns`, `:core:ads`): lần bấm thứ hai trong cùng một lượt khi chưa có quảng cáo không gọi `onGranted` lần nữa, không đếm thêm một lần thử của `allow_after_<n>`.
- Giữ R-06: lần bấm thứ hai trước khi quảng cáo lên màn vẫn gọi hiện như AdsKit và ghi cơ hội `busy`; thưởng vẫn trao một lần theo R-02.
- Test viết trước: bấm hai lần với `allow`, `allow_after_<n>`, `block`; có quảng cáo và không có.

**Không bao gồm:**

- Đổi chính sách hay hộp thoại (T-031).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn ads, core); chỉ logic, không đổi giao diện.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
