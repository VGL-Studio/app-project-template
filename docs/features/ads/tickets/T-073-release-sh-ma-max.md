---
id: T-073
title: release.sh đọc và kiểm khóa, mã đơn vị MAX
feature: ads
spec: features/ads/spec.md
scenarios: [KB-31]
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-068]
sprint:
plan:
auto_run: false
created: 2026-10-29
---

# T-073: RELEASE.SH ĐỌC VÀ KIỂM KHÓA, MÃ ĐƠN VỊ MAX

## Mô tả

`scripts/release.sh` đọc, kiểm khóa SDK MAX và mã đơn vị MAX theo khe; hướng dẫn phát hành ghi việc bỏ AppLovin khỏi nhóm mediation AdMob khi app dùng MAX (câu hỏi 45). Ticket do người làm vì phiên tự chạy không được chạm `scripts/release.sh`.

## Phạm vi

- `release.properties`: khóa SDK MAX, mã đơn vị MAX; `--check-only` báo thiếu.
- Chuyển từ T-068 (rà soát plan T-068, 2026-10-29): `preProduction*Build` bắt buộc `maxSdkKey` khi nhóm `com.applovin.mediation` có trên classpath; `release.sh` truyền `maxSdkKey` vào Gradle; mở rộng test hợp đồng L-125 trong `scripts/tests/run.sh` (hôm nay chỉ đọc mục `"ADR-0027"`) để có khóa của ADR-0037; viết lại câu khẳng định `ProductionPropertiesTest.maxSdkKey_isOptionalUntilReleaseShPassesIt` của T-068 theo ADR-0031.
- ~~Thêm mẫu mã đơn vị MAX vào `scripts/secret-scan.py` nếu T-068 chưa làm (plan T-068, câu hỏi 5).~~ Đổi 2026-10-31: T-071 làm mẫu chặn (plan T-071, câu hỏi 3, 9). T-073 còn kiểm ký tự của `-PmaxAdUnits` ở production (plan T-071, mục 6, ghi chú G4).
- `docs/ops/release.md`: cấu hình MAX và nhóm mediation AdMob.

**Không bao gồm:**

- Code khởi tạo và hiện quảng cáo MAX (T-068 tới T-072); ticket này chỉ nhận phần build phát hành và test hợp đồng nêu ở trên.

## Rủi ro sơ bộ

Chạm `scripts/release.sh` và cấu hình phát hành: chỉ người làm, `auto_run: false`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
