---
id: T-023
title: Doanh thu theo vị trí ad_impression_placement và số liệu vòng đời Firebase
feature: ads
spec: features/ads/spec.md
scenarios: [KB-22]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-019, T-010]
sprint:
plan:
created: 2026-10-23
---

# T-023: DOANH THU THEO VỊ TRÍ AD_IMPRESSION_PLACEMENT VÀ SỐ LIỆU VÒNG ĐỜI FIREBASE

## Mô tả

`:core:ads` nghe sự kiện doanh thu của mọi định dạng đang có và ghi `ad_impression_placement` qua `:core:analytics`, với dự phòng 2 giây cho native; số liệu vòng đời (`ad_load_*`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, rewarded) có hai khóa bật, mặc định tắt. Theo `docs/ops/ads-business-rules.md`, mục 8.1, 8.2. Các ticket định dạng sau gọi cùng bộ ghi này.

## Phạm vi

- Interface ghi số liệu quảng cáo trong `:core:analytics`, bản Firebase và bản giả cho test.
- Ánh xạ mã lỗi sang `load_state`, giá trị micro sang tiền: hàm thuần có test.
- Vị trí đọc lúc có sự kiện (PL-03, D-05).
- Cập nhật mục 6 của spec nếu tham số đổi khi viết plan.

**Không bao gồm:**

- AppsFlyer (T-033).

## Rủi ro sơ bộ

Module core, thêm sự kiện phân tích: điểm chặn (sự kiện phải có trong spec đã duyệt, mục 6). Ghi sai làm đếm đôi doanh thu. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
