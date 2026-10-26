---
id: T-041
title: Native toàn trang của luồng giữ CTA trong màn thấp và giữ quảng cáo khi xoay, gập máy
feature: ads
spec: features/ads/spec.md
scenarios: [KB-20]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-032, T-036]
sprint:
plan: features/ads/plans/T-041-trang-toan-trang-cua-luong.md
created: 2026-10-26
---

# T-041: NATIVE TOÀN TRANG CỦA LUỒNG GIỮ CTA TRONG MÀN THẤP VÀ GIỮ QUẢNG CÁO KHI XOAY, GẬP MÁY

## Mô tả

Hai lỗi của native toàn trang ở một bước của luồng (T-032, T-036), thấy sau RUN-20261026-04. Ảnh chuẩn `app/src/test/screenshots/AdGalleryScreen_flow-full-page_small.png`: ở khung thấp `w360dp-h640dp`, trang toàn trang còn khoảng 200 dp vì màn xem thử giữ khung thẻ 250 dp cả khi thẻ bị ẩn, nên nút CTA của quảng cáo bị cắt còn một vạch. Rà soát độc lập của T-032: xoay máy hay gập, mở máy thì trang toàn trang mất quảng cáo. Sau task, CTA của native toàn trang luôn thấy được và bấm được, và quảng cáo giữ qua thay đổi cấu hình.

## Phạm vi

- Bố cục native toàn trang (`NativeFlowStep`, `:core:ads`) co phần media trước, giữ tiêu đề, nhãn "Ad", AdChoices và CTA trong khung khi chiều cao nhỏ; có chiều cao tối thiểu rõ ràng và test đo đáy CTA nằm trong trang ở khung thấp.
- Trang "Flow steps" của màn xem thử (`:app`, T-036) không giữ khung thẻ 250 dp ở bước toàn trang; nút Next vẫn ở đáy, cùng chỗ ở mọi bước (OB-01).
- Quảng cáo của trang toàn trang giữ qua xoay máy, gập và mở máy (dựng lại activity), không nạp lượt mới, không ghi lượt hiển thị hai lần.
- Ghi lại `AdGalleryScreen_flow-full-page_light.png`, `_small.png` và ảnh của `NativeFlowStep` bị ảnh hưởng.

**Không bao gồm:**

- Thẻ 250 dp ngoài pager (không lỗi).
- Pool và native trong tập hợp (T-028).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn ads, core); ghi lại ảnh chuẩn. Phần giữ quảng cáo qua dựng lại activity chạm vòng đời của khe native (T-040).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
