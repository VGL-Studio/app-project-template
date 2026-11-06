---
date: 2026-11-06
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: NATIVE TOÀN MÀN HÌNH CHỈ SAU INTERSTITIAL CỦA VỊ TRÍ TRONG APP

Ghi lại từ trao đổi với AI ngày 2026-11-06.

## Mình muốn gì

"Sau interstitial ads thì vẫn có thể hiển thị native fullscreen (khi bật is_show_native_after_inter), tuy nhiên, có ngoại lệ là khi interstitial được sử dụng là aoa hoặc resume."

- Interstitial của một vị trí trong app (hành động của người dùng): đóng xong vẫn nối native toàn màn hình khi `is_show_native_after_inter` bật, như hiện nay.
- Interstitial dùng thay quảng cáo mở app (`open_ad_type = inter`, vị trí `open_app_inter`): không nối native toàn màn hình.
- Interstitial khi quay lại app (`resume_ad_type = inter`, vị trí `resume_inter`): không nối native toàn màn hình.

## Hiện trạng lúc ghi

- Interstitial lúc mở app đã không nối native (code của `:core:ads`), nhưng tài liệu quy tắc chưa ghi thành luật.
- Interstitial khi quay lại app đang nối native khi khóa bật (luật A-12 của `docs/ops/ads-business-rules.md`, theo AdsKit PARTNER).

## Những gì chưa cần làm lần này

Không đổi khóa Remote Config, không thêm khóa mới.
