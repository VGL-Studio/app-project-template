---
id: T-045
title: CTA của native giữ trọn chữ khi cỡ chữ hệ thống lớn
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-20, KB-21]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-044, T-041]
sprint:
plan: features/ads/plans/T-045-cta-native-khi-chu-lon.md
created: 2026-10-26
---

# T-045: CTA CỦA NATIVE GIỮ TRỌN CHỮ KHI CỠ CHỮ HỆ THỐNG LỚN

## Mô tả

Chạy thử của plan T-044 (mục 8, câu 2) thấy ở cỡ chữ hệ thống 2.0 chữ của CTA cao hơn khung CTA và bị cắt mép, ở mọi cỡ native có CTA thấp (small 34 dp, thẻ dọc, thẻ ở bước của luồng, lớp thu gọn, đầy đủ 36 dp). Native medium nặng hơn: thẻ cố định 410 dp ép CTA còn 32 dp ở cỡ chữ 1.3 và 11 dp ở 2.0. Anh Nhật chọn làm ticket riêng, chạy sau T-041. Sau task, người dùng đặt chữ lớn vẫn đọc trọn chữ của CTA ở mọi native; ở cỡ chữ 1.0 giao diện không đổi.

## Phạm vi

- CTA của native (small, medium, đầy đủ, thẻ dọc, thẻ ở bước, lớp thu gọn) cao vừa một dòng chữ khi chữ lớn, không nhỏ hơn số của thiết kế.
- Native medium giữ CTA, hàng sao và hai dòng mô tả khi chữ lớn theo luật "media co trước, không dưới 120 dp" (luật do T-044 ghi ở README thiết kế).
- Test đo ở cỡ chữ 1.0, 1.3, 2.0 cho mọi cỡ native, kể cả native toàn màn hình (sau inter và trang của luồng, CTA 52 dp, đã đủ chỗ: test canh).

**Không bao gồm:**

- Đầu quảng cáo 48 dp của native toàn màn hình sau inter ép nhãn "Ad" khi chữ lớn, và media dưới 120 dp của thẻ 250 dp ở cỡ chữ 2.0: ghi ở câu hỏi của plan, đề xuất ticket sau.
- Bố cục trang toàn trang của luồng (T-041), thẻ `FULL` (T-044).

## Rủi ro sơ bộ

Đổi bố cục native trong module core quảng cáo khi chữ lớn (điểm chặn 1 và 7): rủi ro cao. Ở cỡ chữ 1.0 không đổi gì, ảnh chuẩn giữ nguyên.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
