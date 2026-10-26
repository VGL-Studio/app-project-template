---
id: T-045
title: Native giữ trọn CTA, nhãn "Ad" và media 120 dp khi cỡ chữ hệ thống lớn
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-16, KB-20, KB-21]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-044, T-041]
sprint:
plan: features/ads/plans/T-045-cta-native-khi-chu-lon.md
created: 2026-10-26
---

# T-045: NATIVE GIỮ TRỌN CTA, NHÃN "AD" VÀ MEDIA 120 DP KHI CỠ CHỮ HỆ THỐNG LỚN

## Mô tả

Chạy thử của plan T-044 (mục 8, câu 2) thấy ở cỡ chữ hệ thống 2.0 chữ của CTA cao hơn khung CTA và bị cắt mép ở mọi native có CTA thấp (small 34 dp, thẻ dọc, thẻ ở bước, lớp thu gọn, đầy đủ 36 dp). Native medium nặng hơn: thẻ cố định 410 dp ép CTA còn 32 dp ở cỡ chữ 1.3 và 11 dp ở 2.0. Chạy thử của plan T-045 thấy thêm: nhãn "Ad" của native toàn màn hình sau inter bị ép còn 2 dp ở 2.0 (O-08), media của thẻ ở bước và lớp thu gọn còn khoảng 100 dp (dưới 120 dp). Anh Nhật chọn làm chung trong ticket này, chạy sau T-041. Sau task, người dùng đặt chữ lớn vẫn đọc trọn CTA và nhãn "Ad", media không dưới 120 dp; ở cỡ chữ 1.0 giao diện không đổi.

## Phạm vi

- CTA của native (small, medium, đầy đủ, thẻ dọc, thẻ ở bước, lớp thu gọn) cao ít nhất số thiết kế, cao thêm vừa một dòng chữ khi chữ lớn.
- Native medium giữ CTA, hàng sao và hai dòng mô tả khi chữ lớn theo luật "media co trước, không dưới 120 dp" (luật do T-044 ghi ở README thiết kế).
- Native toàn màn hình sau inter: đầu quảng cáo cao theo cỡ chữ như T-041 làm cho trang toàn trang, nhãn "Ad" giữ trọn chiều cao chữ.
- Thẻ ở bước và lớp thu gọn: media không dưới 120 dp; thẻ cao thêm khi media chạm sàn; khung chờ và khung của app theo cùng chiều cao.
- Test đo ở cỡ chữ 1.0, 1.3, 2.0 cho mọi dạng native.

**Không bao gồm:**

- Bố cục trang toàn trang của luồng (T-041), thẻ `FULL` (T-044).
- Khung chờ của native toàn màn hình sau inter (đầu chờ 48 dp, không có chữ).

## Rủi ro sơ bộ

Đổi bố cục native trong module core quảng cáo khi chữ lớn, thêm API công khai cho chiều cao thẻ ở bước (điểm chặn 1 và 7): rủi ro cao. Ở cỡ chữ 1.0 không đổi gì, ảnh chuẩn giữ nguyên.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
