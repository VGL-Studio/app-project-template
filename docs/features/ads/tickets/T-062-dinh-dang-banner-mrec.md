---
id: T-062
title: Định dạng banner MREC
feature: ads
spec: features/ads/spec.md
scenarios: [KB-29]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-054, T-067]
sprint:
plan: features/ads/plans/T-062-dinh-dang-banner-mrec.md
auto_run: 
created: 2026-10-29
---

# T-062: ĐỊNH DẠNG BANNER MREC

## Mô tả

Thêm định dạng `banner_mrec` (300 x 250) vào danh sách đóng của `:core:ads` (ADR-0036, điểm 3; anh Nhật chọn 2026-10-29), cùng luật với banner cố định và dừng khi bị che như T-054.

## Phạm vi

- `AdFormat.BANNER_MREC`, khe `banner_mrec`, mã thử MREC của Google ở staging, composable đặt trong nội dung của màn (câu hỏi 38).
- Số liệu: mã định dạng `BANNER_MREC` ở mọi sự kiện có `ad_format`.
- Vị trí `gallery_banner_mrec` trên màn xem thử; bảng định dạng của `scripts/ads-plan.py`.
- Sửa `ads-business-rules.md` MC-02, M-05, mục 5 (bảng biến thể), mục 8; `ads-guide.md`.

**Không bao gồm:**

- Mã thử MREC trong `scripts/release.sh` (T-057; vị trí `gallery_` không cần mã production).

## Rủi ro sơ bộ

Module core, thêm định dạng quảng cáo (điểm chặn 1 và 7), sự kiện có giá trị mới (điểm chặn 4).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
