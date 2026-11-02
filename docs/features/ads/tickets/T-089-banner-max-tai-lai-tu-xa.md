---
id: T-089
title: Banner cố định và MREC của MAX tải lại theo Remote Config
feature: ads
spec: features/ads/spec.md
scenarios: [KB-34, KB-31]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-088]
sprint:
plan:
auto_run: 
created: 2026-11-02
---

# T-089: BANNER CỐ ĐỊNH VÀ MREC CỦA MAX TẢI LẠI THEO REMOTE CONFIG

## Mô tả

Khung banner hai nền tảng (B-09) dùng một nhịp cho cả AdMob và MAX: view MAX tắt tự làm mới (`stopAutoRefresh()` ngay khi tạo) và được `:core:ads` tải lại theo `time_reload_banner`, `time_reload_banner_mrec` như AdMob sau T-088 (KB-34, câu hỏi 52).

## Phạm vi

- `MaxBannerAdView.kt`, khung hai nền tảng (`PlatformBanners.kt`, `BannerFrame.kt`): MAX không tự làm mới; mỗi lượt tải lại đi theo `ads_mediation_order` như lượt đầu (B-09), qua cùng bước kiểm màn hiện, cổng đồng ý, bị che.
- Giữ luật lỗi và thử lại của B-09 (kể cả phần của T-085).
- Sửa `ads-business-rules.md` (B-09) và hướng dẫn phát hành: tắt tự làm mới của đơn vị banner, MREC trên MAX.

**Không bao gồm:**

- Banner thu gọn (chỉ AdMob, T-087).

## Rủi ro sơ bộ

Module core, quảng cáo, SDK MAX (điểm chặn 1 và 7). Khung hai nền tảng vừa được dọn ở T-085, logic chờ và bỏ kết quả của lượt thử lại dễ vỡ khi thêm vòng tải lại tay.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
