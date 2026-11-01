---
id: T-085
title: Dọn test và luật biên của khung banner hai nền tảng
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31, KB-18]
owner: Nhật
status: on-staging
risk: medium
size: S
depends_on: [T-071]
sprint:
plan: features/ads/plans/T-085-don-khung-hai-nen-tang.md
auto_run: 
created: 2026-11-01
---

# T-085: DỌN TEST VÀ LUẬT BIÊN CỦA KHUNG BANNER HAI NỀN TẢNG

## Mô tả

Ba ghi chú của rà soát độc lập T-071 (PR #364, báo cáo RUN-20261031-01) về `OrderedBannerAdView` (`PlatformBanners.kt`): hai nhánh code có test không bắt được khi đột biến, và một ca biên làm mất kết quả nạp của MAX. Người dùng không thấy gì mới; khung banner hai nền tảng có test giữ đủ các luật đã có và xử lý đúng ca biên.

## Phạm vi

- Test bắt đột biến của nhánh bỏ kết quả giả cũ (`sendNo` khác lượt gửi hiện tại): kết quả giả `MAX_NOT_READY` còn trong hộp thư khi lượt gửi mới chạy không bị coi là kết quả của lượt mới (ghi chú 1).
- Xem lại `resumeChild` ở nhánh `Loaded` khi khung đang chạy: đột biến qua vì đường `send()` đã `resume()` con trước. Thêm test cho đường mà nhánh này cần, hoặc bỏ nhánh nếu không có đường nào cần nó; hành vi KB-18 giữ nguyên (ghi chú 2).
- Thứ tự `[MAX, ADMOB]`, AdMob đang hiện mà làm mới lỗi hai lần trong lúc một lượt nạp MAX còn chờ: kết quả của MAX bị bước xả hộp thư của `sendWhenAllowed` bỏ (theo chữ mục 3.1 của plan T-071). Quyết cách xử lý (giữ kết quả của con `pending`, hay chấp nhận và ghi vào luật B-09), có test (ghi chú 3).
- Chú thích ở `PlatformBanners.kt` (khoảng dòng 174) nói hẹp hơn code: sửa cho khớp KDoc (ghi chú 6).

**Không bao gồm:**

- Nhật ký mạng con và `ad_platform` của MAX (T-072).
- Lọc ký tự của `-PmaxAdUnits` ở production (T-073).

## Rủi ro sơ bộ

Module core `:core:ads` (điểm chặn 7), chỉ trong khung banner hai nền tảng; không đổi sự kiện, quyền hay vị trí quảng cáo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
