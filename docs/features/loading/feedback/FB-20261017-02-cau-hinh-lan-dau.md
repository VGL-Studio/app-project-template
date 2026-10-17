---
id: FB-20261017-02
title: Lần mở đầu sau khi cài, tải cấu hình hết 10 giây rồi báo lỗi dù có mạng
reporter: AI (phiên thử TS-20261017-01)
source: staging
build: 0.1.0-staging, commit caf0746
feature: loading
ticket: T-001
severity: nang
status: new
created: 2026-10-17
---

# FB-20261017-02: LẦN MỞ ĐẦU SAU KHI CÀI, TẢI CẤU HÌNH HẾT 10 GIÂY RỒI BÁO LỖI DÙ CÓ MẠNG

## Điều đã xảy ra

Trên emulator có mạng, ba lần mở app đầu tiên sau khi cài đều hết 10 giây ở bước tải cấu hình (`Refresh timed out after 10000 ms`) và hiện màn hình lỗi; ba file R2 vẫn tải xong trong 2 đến 3 giây. Sau khi app đã chạy một lúc, mọi lần mở sau tải cấu hình chỉ mất 0,7 đến 2 giây. Ở lần mở đầu, hộp thoại đồng ý quảng cáo hiện đè lên màn hình lỗi. Chi tiết và log: phiên thử [TS-20261017-01](../../../test-sessions/TS-20261017-01.md), mục P-2.

## Các bước làm lại

1. Cài bản staging mới (hoặc xóa dữ liệu app), có mạng.
2. Mở app, chờ 15 giây.
3. Xem log: `scripts/device-session.sh start`, mở app, `scripts/device-session.sh stop`.

## Mong đợi

Có mạng thì lần mở đầu chuẩn bị xong và vào màn hình chính (KB-1), không báo lỗi.

## Ảnh chụp, video

Ảnh `after-start.png` trong artifact `device-session` của lần chạy 39044089126 (giữ 7 ngày).

## Thiết bị

- Máy: emulator Google sdk_gphone64_x86_64
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: sáng

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** lỗi so với spec / thay đổi yêu cầu / góp ý mới
- **Xử lý:** sửa trong sprint này / đưa vào backlog / không xử lý
- **Lý do:** <...>
- **Liên kết:** <plan hoặc ticket xử lý>

AI đề xuất: **chưa phân loại, xác nhận trước trên máy thật**. Cài bản staging mới trên một điện thoại, ghi một phiên thử với vài lần mở đầu. Nếu lặp lại: lỗi so với spec (KB-1). Giả thuyết: lần đầu Firebase phải đăng ký bản cài trước khi tải cấu hình, và việc này chậm hơn 10 giây; log hiện không ghi `RemoteConfigRefresher` đang chờ bước nào, nên trước hết thêm log đó. Hướng sửa có thể: thời gian chờ riêng cho lần mở đầu, hoặc thử lại tự động một lần trước khi báo lỗi; cả hai đổi spec (thời gian chờ là câu hỏi đã chốt ở mục 9 của spec), cần người phụ trách sản phẩm quyết định. Thứ tự hộp thoại đồng ý thuộc điểm chặn (ADR-0013), chỉ cân nhắc, không đổi trong lần sửa này.
