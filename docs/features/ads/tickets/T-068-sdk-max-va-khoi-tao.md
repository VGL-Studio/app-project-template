---
id: T-068
title: SDK AppLovin MAX và khởi tạo sau cổng đồng ý
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-068: SDK APPLOVIN MAX VÀ KHỞI TẠO SAU CỔNG ĐỒNG Ý

## Mô tả

Thêm SDK AppLovin MAX vào `:core:ads`, khởi tạo khi cổng đồng ý mở lần đầu và đọc chuỗi đồng ý của UMP; khóa SDK MAX đưa vào lúc build phát hành, staging không có khóa thì MAX không khởi tạo (ADR-0037).

## Phạm vi

- SDK MAX và adapter Google của MAX trong `gradle/libs.versions.toml`, phiên bản cụ thể (không `+`); kho Maven nếu cần (`settings.gradle.kts`, chạy thử tải trong môi trường cloud, L-067).
- Khởi tạo trong cùng chỗ với `startSdk` của `AdsController`, sau cổng đồng ý; rút đồng ý thì đóng như AdMob; không viết cứng trạng thái đồng ý.
- Tham số build `-PmaxSdkKey`; `preProduction*Build` báo lỗi khi thiếu mà thứ tự mặc định có MAX; đọc `local.properties` ở staging; thêm mẫu khóa MAX vào `scripts/secret-scan.py`.
- Đo quyền mà SDK kéo vào manifest gộp, ghi vào spec mục 6 trước khi duyệt plan; chạy `:app:assembleStagingRelease` (R8, L-083).
- Khóa `ads_mediation_order` (mặc định `max_admob`) trong `AdsSettings`, bảng khóa, `remote-config-template.json`.

**Không bao gồm:**

- Hiện quảng cáo của MAX (T-069 tới T-071), doanh thu (T-072), `release.sh` (T-073), APS (chưa có tài khoản).

## Rủi ro sơ bộ

SDK mới, thứ tự khởi tạo SDK, đồng ý, cấu hình build (điểm chặn 2, 5, 7). Plan chạm đồng ý cần người phụ trách sản phẩm biết trước khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
