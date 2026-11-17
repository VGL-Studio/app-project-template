---
id: T-110
title: Code native medium theo thẻ JBase vẽ ra
feature: ads
spec: features/ads/spec.md
scenarios: [KB-2, KB-3, KB-32]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-109]
sprint:
plan: features/ads/plans/T-110-medium-theo-the-jbase.md
auto_run:
created: 2026-11-17
---

# T-110: CODE NATIVE MEDIUM THEO THẺ JBASE VẼ RA

## Mô tả

Tách từ T-109 khi viết plan (2026-11-17), để mỗi ticket vừa một lượt của phiên tự chạy. Đổi native medium trong `:core:ads` theo thiết kế T-108 (spec `ads`, KB-32 tiêu chí 3, câu hỏi 66): thẻ 250 dp, ảnh ở trên sát mép thẻ lấp phần còn lại (144 dp, 132 dp với CTA lớn), dưới là khối nội dung hai tầng như native nhỏ của T-109; dòng dưới tiêu đề là nhà quảng cáo, không có thì mô tả, một dòng, hàng sao ở cuối dòng; bỏ đoạn mô tả hai dòng. Ảnh còn dưới 120 dp (CTA lớn, chữ lớn) thì thẻ cao thêm để ảnh đủ 120 dp.

## Phạm vi

- Bố cục và chiều cao medium (thẻ, khung chờ, chỗ giữ khi trống) theo thiết kế T-108, dùng lại khối nội dung hai tầng của T-109; bỏ chiều cao nội suy theo chiều cao màn hình (`mediumHeight`) và khung thấp (`MediumCompactBelow`, plan T-065 câu hỏi 7).
- Viết lại câu khẳng định cũ về medium (400 dp, 182 dp, 432 dp, khung thấp) theo ADR-0031; ghi lại ảnh chuẩn bị đổi (`NativeAdCard_medium_*`, `NativeAdCard_medium-cta-large_light`, `NativeAdPlaceholder_medium_light`, `AdGalleryScreen_default_light`, `_dark`, `_small`, `AdGalleryScreen_default-more_light`, `AdGalleryScreen_inlist_light`, `_small`, `NativeAdCard_font-scale-2_light`), plan liệt kê đủ.
- `ads-business-rules.md` mục 5.7 (dòng native medium) và `ads-guide.md` mục 3 (dòng Native medium) và mục 4 (cột Medium); README thiết kế: số medium ở cỡ chữ lớn (dòng Media của mục 3), bỏ ghi chú khung thấp.

**Không bao gồm:**

- Native nhỏ và lớp thu gọn (T-109).
- Thiết kế (T-108).

## Rủi ro sơ bộ

Module core, giao diện quảng cáo, nhiều ảnh chuẩn screenshot. Chạy sau T-109, nên cùng danh sách chạy để bản staging không ở lâu với native nhỏ mới và medium cũ.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
