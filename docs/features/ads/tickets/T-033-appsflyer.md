---
id: T-033
title: AppsFlyer trong :core:analytics: doanh thu và phễu quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-22]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-023, T-026, T-031]
sprint:
plan: features/ads/plans/T-033-appsflyer.md
created: 2026-10-23
---

# T-033: APPSFLYER TRONG :CORE:ANALYTICS: DOANH THU VÀ PHỄU QUẢNG CÁO

## Mô tả

Thêm SDK AppsFlyer vào `:core:analytics` theo ADR-0027: khóa dev qua tham số build ở production, staging và CI không có khóa thì không gửi, đọc chuỗi đồng ý TCF, khởi động cùng lúc với SDK quảng cáo. `:core:ads` gửi `af_ad_revenue` và phễu `af_*` theo `docs/ops/ads-business-rules.md`, mục 8.3.

## Phạm vi

- Thư viện mới trong `gradle/libs.versions.toml`; so quyền manifest gộp trước và sau, ghi vào spec mục 6.
- Bản không gửi khi thiếu khóa; test không gọi SDK.
- Không bao giờ gửi `af_revenue`; test chặn.
- Công tắc từ xa cho từng sự kiện (`is_log_af_*`, mặc định bật, hiệu lực từ sự kiện kế tiếp; câu hỏi 27); `monetization_network` rỗng khi SDK không báo; `ad_placement` là `""` khi chưa có.
- Ghi vào `ads-guide.md` mục 9 việc báo nhóm UA trước khi phát hành (HD-07).
- Test nghiệm thu gợi ý: `AT-DL-*` phần AppsFlyer.

**Không bao gồm:**

- Thêm bước khóa dev vào `scripts/release.sh` (việc của người, câu hỏi 15 của spec).

## Rủi ro sơ bộ

Thêm SDK, sửa file được bảo vệ, dữ liệu gửi ra ngoài: plan rủi ro cao, duyệt chéo. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
