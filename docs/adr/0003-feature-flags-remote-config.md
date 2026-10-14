---
id: ADR-0003
title: Dùng cờ tính năng qua Firebase Remote Config
status: accepted
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0003: DÙNG CỜ TÍNH NĂNG QUA FIREBASE REMOTE CONFIG

## Bối cảnh

Mọi thứ AI làm ra trong mỗi phiên tự chạy đều vào nhánh chính, trong khi chỉ những tính năng được người phụ trách sản phẩm duyệt mới được phát hành tới người dùng. App tự kinh doanh nên cần bật dần tính năng, thử nghiệm và tắt nhanh khi có sự cố.

## Quyết định

- Tính năng chưa được duyệt phát hành nằm sau cờ tính năng, mặc định tắt trong bản production.
- Cờ được quản lý tập trung trong module `:core:config`, giá trị lấy từ Firebase Remote Config.
- Các tham số kinh doanh có thể điều chỉnh (vị trí và tần suất quảng cáo, quảng cáo cá nhân hóa ở châu Âu) cũng đi qua Remote Config.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Cờ tính năng | Hợp với merge liên tục, bật tắt không cần phát hành lại | Code thêm điều kiện, cần dọn cờ cũ |
| Cắt nhánh phát hành và gỡ phần chưa duyệt | Code đơn giản | Gỡ thủ công tốn công, dễ sót |

## Hệ quả

- **Tích cực:** nhánh chính luôn phát hành được; người phụ trách sản phẩm tự điều chỉnh mà không cần dev.
- **Phải chấp nhận:** cờ đã bật cho toàn bộ người dùng một thời gian phải được gỡ khỏi code để tránh tích tụ.
- **Việc AI phải tuân thủ:** mỗi feature mới có cờ riêng khai báo trong `:core:config`, tên theo dạng `feature_<ten>_enabled`; không đọc Remote Config trực tiếp từ module feature.
