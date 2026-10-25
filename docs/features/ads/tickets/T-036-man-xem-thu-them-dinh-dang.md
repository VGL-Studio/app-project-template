---
id: T-036
title: Màn hình xem thử quảng cáo thêm các định dạng mới
feature: ads
spec: features/ads/spec.md
scenarios: [KB-8]
owner: Nhật
status: approved
risk: high      # plan T-036: thêm, bớt vị trí gallery_* và API của :core:ads (điểm chặn 1, 7)
size: M
depends_on: [T-014, T-024, T-025, T-026, T-028, T-029, T-030, T-031, T-032]   # T-029 thêm 2026-10-25 theo câu hỏi 1 của plan T-029
sprint:
plan: features/ads/plans/T-036-man-xem-thu-them-dinh-dang.md
created: 2026-10-23
---

# T-036: MÀN HÌNH XEM THỬ QUẢNG CÁO THÊM CÁC ĐỊNH DẠNG MỚI

## Mô tả

Màn xem thử chỉ ở staging (T-014) thêm nút gọi app open, combo inter rồi native, rewarded với hộp thoại, thanh đáy, ô lưới, native giới thiệu, và hiện trạng thái đồng ý (đường đồng ý, quyền tạm) để người thử kiểm KB-11.

## Phạm vi

- Vị trí `gallery_*` cho định dạng mới.
- Screenshot test theo thiết kế.
- Một vị trí `gallery_rw_action` minh họa ba chính sách của hành động có điều kiện, nếu câu hỏi 13 chọn như đề xuất.

**Không bao gồm:**

- Định dạng mới (các ticket phụ thuộc).

## Rủi ro sơ bộ

Chỉ staging; vẫn trong module có quảng cáo. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
