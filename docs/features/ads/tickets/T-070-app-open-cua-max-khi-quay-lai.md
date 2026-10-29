---
id: T-070
title: App open của MAX khi quay lại app
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31, KB-14]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-069]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-070: APP OPEN CỦA MAX KHI QUAY LẠI APP

## Mô tả

Quảng cáo khi quay lại app dùng app open của MAX theo thứ tự `ads_mediation_order`; lúc mở app vẫn chỉ app open của AdMob (câu hỏi 41).

## Phạm vi

- Khe app open của MAX; nạp lại 2 giây sau khi đóng như JBase (ADR-0038, quyết định 5); khóa 1 giây sau app open trước áp cho cả hai nền tảng (A-08).
- Cùng thứ tự cổng của A-11; `resume_ad_type = inter` dùng interstitial theo thứ tự nền tảng (T-069).

**Không bao gồm:**

- App open lúc mở app (giữ AdMob).

## Rủi ro sơ bộ

Module core, quảng cáo, vòng đời ứng dụng.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
