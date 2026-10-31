---
id: T-072
title: Doanh thu và phễu của MAX
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31, KB-22]
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-069]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-072: DOANH THU VÀ PHỄU CỦA MAX

## Mô tả

Doanh thu và phễu của quảng cáo MAX ghi cùng sự kiện với AdMob: `ad_impression_placement`, `af_ad_revenue` với `ad_platform = applovin`, `mediation_network = applovinmax`, giá trị theo đô la; phễu `af_inters_*`, `af_rewarded_*` (câu hỏi 46).

## Phạm vi

- Ánh xạ doanh thu của MAX vào `AdTelemetry` (định dạng, mạng, mã đơn vị, vị trí đọc lúc sự kiện về); không ghi `ad_impression` từ app.
- Sửa `ads-business-rules.md` mục 8 kèm ADR-0037; báo nhóm UA trước khi phát hành (HD-07).
- Thêm 2026-10-31: tham số `ad_platform` (`admob`, `applovin`) cho `ad_load_success`, `ad_load_fail`, `ad_clicked` (`:core:analytics`), kèm sửa spec `ads` mục 6 và luật mục 8.2 (plan T-071, câu hỏi 8). T-072 merge trước bản phát hành đầu có mã MAX.

**Không bao gồm:**

- Doanh thu APS.

## Rủi ro sơ bộ

Module core, sự kiện phân tích (điểm chặn 4, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
