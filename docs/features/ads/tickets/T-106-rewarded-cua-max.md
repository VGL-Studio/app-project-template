---
id: T-106
title: Rewarded của MAX qua lượt quyết định rewarded chung
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-069]
sprint:
plan: features/ads/plans/T-106-rewarded-cua-max.md
auto_run:
created: 2026-11-15
---

# T-106: REWARDED CỦA MAX QUA LƯỢT QUYẾT ĐỊNH REWARDED CHUNG

## Mô tả

Rewarded có thêm nền tảng AppLovin MAX: mỗi lượt hỏi nền tảng theo `ads_mediation_order`, chưa có thì nền tảng kia, trong cùng lượt quyết định của `RewardedTurns` (R-06, chờ tối đa 5 giây). Tách từ T-069 ngày 2026-11-15 (anh Nhật chọn khi soạn plan T-069).

## Phạm vi

- Một khe rewarded của MAX (một mã đơn vị mỗi định dạng, M-10), nạp sẵn khi MAX có trong thứ tự; thử lại theo F-05, nạp lại 2 giây sau khi đóng (F-06). Dùng lại cách chọn nền tảng và mẫu khe của T-069 (`FullScreenPlatforms.kt`, `MaxInterstitialSlot`).
- Thưởng trong callback thưởng của MAX (R-02); chính sách khi không có quảng cáo (MC-05, R-03) xét sau khi đã hỏi cả hai nền tảng; "lỗi mạng" của R-03 hiểu cả mã lỗi mạng của MAX.
- `MaxRewardedAd` dùng chung một đối tượng cho mỗi mã đơn vị (`getInstance`): hai vị trí cùng mã dùng chung khe; kiểm hai yêu cầu chồng nhau (L-068).
- Test bằng cổng giả như `RewardedPort`.

**Không bao gồm:**

- Interstitial (T-069), app open khi quay lại (T-070), doanh thu (T-072).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn 1, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
