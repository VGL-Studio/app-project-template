---
id: T-034
title: Mediation chín mạng
feature: ads
spec: features/ads/spec.md
scenarios: [KB-9]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-015]
sprint:
plan: features/ads/plans/T-034-mediation-chin-mang.md
created: 2026-10-23
---

# T-034: MEDIATION CHÍN MẠNG

## Mô tả

Thêm adapter chính thức của AdMob cho tám mạng còn lại (Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads kèm SDK Unity Ads), cạnh AppLovin của T-015 (ADR-0026). Khóa SDK AppLovin đưa vào lúc build, không trong repo.

## Phạm vi

- Thư viện trong `gradle/libs.versions.toml`; liệt kê quyền từng adapter kéo vào manifest gộp, đề xuất giữ hay gỡ (câu hỏi 18 của spec).
- Kiểm khởi tạo adapter bằng Ad Inspector trên staging (thủ công, ghi vào báo cáo phiên).
- Tra trang công bố dữ liệu của từng mạng, ghi vào plan cho Data safety; cập nhật `docs/ops/ads-guide.md` mục 7 và 9.

**Không bao gồm:**

- Cấu hình nhóm mediation trên AdMob và `app-ads.txt` (việc của người).

## Rủi ro sơ bộ

Thêm tám SDK và sửa file được bảo vệ: plan rủi ro cao, duyệt chéo. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
