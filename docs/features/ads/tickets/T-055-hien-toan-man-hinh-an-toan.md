---
id: T-055
title: Kiểm màn hình trước khi hiện quảng cáo toàn màn hình, xoay máy
feature: ads
spec: features/ads/spec.md
scenarios: [KB-26]
owner: Nhật
status: planned
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-055-hien-toan-man-hinh-an-toan.md
auto_run: 
created: 2026-10-29
---

# T-055: KIỂM MÀN HÌNH TRƯỚC KHI HIỆN QUẢNG CÁO TOÀN MÀN HÌNH, XOAY MÁY

## Mô tả

Quảng cáo toàn màn hình chỉ được gọi hiện khi màn hình của app còn dùng được; xoay máy không làm hành động sau quảng cáo chạy trước khi quảng cáo đóng.

## Phạm vi

- Kiểm Activity (không đang đóng, chưa hủy, đang ở foreground) trước `ad.show` ở mọi đường hiện: interstitial (`AdsController.kt:894`), app open, rewarded; không đạt thì giữ quảng cáo trong khe, gọi hàm hoàn tất, ghi `ad_show_fail` lý do `app_not_foreground` (đã có mã ở `AdTelemetryRules.kt:82`).
- Hoàn tất cưỡng bức (`onAppActivityDestroyed`, `AdsController.kt:763`, N-04, HD-03): plan viết test tái hiện lần Activity bị tạo lại vì xoay máy lúc quảng cáo còn hiện, chạy trên code hiện tại trước (L-029). Tái hiện được thì bỏ qua lần hủy có `isChangingConfigurations`, như dòng 237 đã làm; không tái hiện được thì ghi số đo vào plan và không đổi phần này.

**Không bao gồm:**

- Hạn dùng interstitial: bỏ theo chuẩn JBase (spec `ads`, câu hỏi 35).

## Rủi ro sơ bộ

Module core, quảng cáo, vòng đời ứng dụng (tiêu chí C cao). Phần xoay máy chưa chắc có lỗi thật.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
