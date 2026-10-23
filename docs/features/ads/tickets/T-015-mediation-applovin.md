---
id: T-015
title: Mediation AdMob và AppLovin
feature: ads
spec: features/ads/spec.md
scenarios: [KB-9]
owner: Nhật
status: approved
risk: high
size: S
depends_on: [T-009]
sprint:
plan: features/ads/plans/T-015-mediation-applovin.md
created: 2026-10-21
---

# T-015: MEDIATION ADMOB VÀ APPLOVIN

## Mô tả

Thêm adapter AppLovin chính thức của AdMob (bidding) để đơn vị quảng cáo có AppLovin trong nhóm mediation nhận được quảng cáo AppLovin mà không đổi code (ADR-0022). Cập nhật `docs/ops/ads-guide.md` mục 7 và `docs/ops/release.md` (`app-ads.txt`).

## Phạm vi

- Thêm thư viện adapter vào `gradle/libs.versions.toml` và `:core:ads`.
- Kiểm khởi tạo adapter qua Ad Inspector trên staging (thủ công, ghi vào báo cáo phiên).

**Không bao gồm:**

- Cấu hình nhóm mediation trên AdMob (việc của người).

## Rủi ro sơ bộ

Thêm SDK và sửa `gradle/libs.versions.toml` (file được bảo vệ): plan rủi ro cao, duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
