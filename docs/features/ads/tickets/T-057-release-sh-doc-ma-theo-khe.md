---
id: T-057
title: release.sh đọc và kiểm mã theo khe
feature: ads
spec: features/ads/spec.md
scenarios: [KB-17, KB-29]
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-056]
sprint:
plan:
auto_run: false
created: 2026-10-29
---

# T-057: RELEASE.SH ĐỌC VÀ KIỂM MÃ THEO KHE

## Mô tả

Phần phát hành của câu hỏi 14: `scripts/release.sh` đọc, kiểm và điền mã thử theo khe mà T-056 đọc được, gồm cả định dạng `banner_mrec` (T-062). Ticket do người làm vì phiên tự chạy không được chạm `scripts/release.sh` (CLAUDE.md, mục Không được làm).

## Phạm vi

- `--list-placements` liệt kê cả hai khóa theo khe của thanh đáy (`banner`, `native_collap`) khi app có vị trí `native_small` phát hành, cùng luật với `missingUnitKeys` của T-056; dòng "Vị trí quảng cáo" của `status.py` đổi cùng lúc (rà soát plan T-056).
- `--fill-test-units` điền mã thử cho khóa theo khe còn thiếu, và có mã thử cho định dạng `banner_mrec`. `banner_mrec` là định dạng của chính vị trí, dùng mục `<mã vị trí>=<mã>`, không phải khóa theo khe (quyết định khi rà soát plan T-056, T-062).
- Tùy chọn: báo khóa lạ trong `adUnits`; test của `scripts/tests/run.sh` và dữ liệu mẫu đi cùng.
- T-057 là điều kiện trước bản production đầu tiên cần lớp của thanh đáy; trước đó app chỉ ghi cảnh báo `missing` trong log (plan T-056).
- Hướng dẫn điền `release.properties` ở `docs/ops/release.md`.
- Luật M-05 của `docs/ops/ads-business-rules.md`: lớp của thanh đáy lấy mã theo khe `banner`, `native_collap`, định dạng chính theo vị trí (câu hỏi 3 của plan T-056).

**Không bao gồm:**

- Phần code (T-056).

## Rủi ro sơ bộ

Chạm `scripts/release.sh` và cấu hình phát hành: chỉ người làm, `auto_run: false`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
