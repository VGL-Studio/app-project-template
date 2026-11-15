---
id: T-069
title: Interstitial của MAX qua cổng toàn màn hình chung
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-068, T-073]
sprint:
plan: features/ads/plans/T-069-inter-cua-max.md
auto_run: 
created: 2026-10-29
---

# T-069: INTERSTITIAL CỦA MAX QUA CỔNG TOÀN MÀN HÌNH CHUNG

**Đổi 2026-11-15** (anh Nhật, khi soạn plan): ticket chỉ còn interstitial; rewarded của MAX tách sang T-106. Inter lúc khởi động (`open_app_inter`) chỉ AdMob, inter khi quay lại app (`resume_inter`) theo thứ tự (spec câu hỏi 41).

## Mô tả

Interstitial của MAX đi qua cùng cổng toàn màn hình, mốc `ads_interval`, gỡ kẹt, chặn quảng cáo tự hiện như AdMob; mỗi lượt hiện hỏi nền tảng theo `ads_mediation_order`, chưa có thì nền tảng kia.

## Phạm vi

- Một khe MAX cho mỗi định dạng; nạp sẵn qua kế hoạch nạp sẵn khi MAX có trong thứ tự; thử lại theo F-05, nạp lại 2 giây sau khi đóng (F-06).
- Lượt quyết định chọn nền tảng: một cơ hội, một hàm hoàn tất chạy đúng một lần; combo native sau inter chạy như với inter của AdMob.
- ~~Rewarded: thưởng trong callback thưởng của MAX; chính sách khi không có quảng cáo xét sau khi đã hỏi cả hai nền tảng.~~ Chuyển sang T-106.
- Test bằng cổng giả cho MAX như `RewardedPort`, `FakePreloadSlot`; kiểm hai yêu cầu chồng nhau (L-068).

**Không bao gồm:**

- Banner, MREC (T-071), app open khi quay lại (T-070), rewarded (T-106).

## Rủi ro sơ bộ

Phụ thuộc T-073 (người làm): quảng cáo MAX đầu tiên chỉ vào bản phát hành khi `release.sh` đã truyền và kiểm khóa MAX; không thì production lặng lẽ chỉ chạy AdMob (rà soát plan T-068).

Module core, quảng cáo, hành vi toàn màn hình (điểm chặn 1, 7). Cỡ M sau khi tách rewarded sang T-106.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
