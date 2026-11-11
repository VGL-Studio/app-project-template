---
id: T-102
title: Lượt native combo không treo khi không có FullScreenAdHost nào đang vẽ
feature: ads
spec: features/ads/spec.md
scenarios: [KB-16]
owner: Nhật
status: approved              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quảng cáo và :core:ads (điểm chặn 1, 7)
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-102-combo-khong-treo-khi-thieu-host.md
auto_run:
created: 2026-11-11
---

# T-102: LƯỢT NATIVE COMBO KHÔNG TREO KHI KHÔNG CÓ FULLSCREENADHOST NÀO ĐANG VẼ

## Mô tả

Nguồn: issue #445 phần (a) (camera-pose, PR #313 của app, phiên thử TS-20261102-01 của app), bài học L-167. Khi khóa `is_show_native_after_inter` bật mà vỏ app chưa đặt `FullScreenAdHost()`, khung vẫn mở lượt combo sau interstitial: không có gì được vẽ, lượt không đóng, việc chạy sau interstitial không chạy và chặn quảng cáo khi quay lại app bị giữ. Phần (b) của issue đã có ở mẫu (luật A-12, KB-14, KB-16), không thuộc ticket này.

## Phạm vi

- `:core:ads`: khung biết có `FullScreenAdHost` nào đang vẽ; không có thì không mở lượt combo, hoặc tự đóng lượt sau một thời hạn (plan chọn, nêu lý do). Lượt kết thúc: việc chạy sau interstitial chạy, chặn quảng cáo khi quay lại app được thả, cờ đang hiện quảng cáo toàn màn hình (F-01) được hạ, có log cho người phân tích. Plan nêu rõ cho cách đã chọn: có ghi mốc `ads_interval` của combo (K-05, I-05) không, có ghi `ad_opportunity` (K-07) không (đổi thì thêm điểm chặn 4), có nạp lại không. "Thiếu host thì không có native" trái KB-16 tiêu chí 1: plan đề xuất tiêu chí và đặt câu hỏi cho người phụ trách sản phẩm.
- Plan xét cùng cách cho native toàn màn hình độc lập đi qua host (`rememberShowFullScreenAd`), và trường hợp host rời màn khi lượt đang mở (đổi vỏ, dựng lại Activity).
- Luật mới hay đổi ghi vào `docs/ops/ads-business-rules.md` (kể cả cột hậu quả của HD-05, hiện ghi "Combo không hiện được" nhẹ hơn thực tế), `docs/ops/ads-guide.md` (vỏ app phải có host; thiếu host thì mất combo).
- Test JVM với `AdsController` thật: không có host, khe đã có quảng cáo sẵn (và một ca quảng cáo về giữa lúc chờ), thì lượt kết thúc và việc sau interstitial chạy (ca không có quảng cáo tự đóng khi hết đếm ngược nên qua ngay trên code cũ, không dùng làm test đỏ); sau đó interstitial kế tiếp không bị coi là bận; có host thì giữ hành vi cũ; host rời rồi quay lại.

**Không bao gồm:**

- Công tắc cho `resume_inter` nối combo (issue #445 phần (b)): đã có ở mẫu.

## Rủi ro sơ bộ

Đổi điều kiện hiện quảng cáo toàn màn hình (điểm chặn 1) và module core (điểm chặn 7): rủi ro cao, duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
