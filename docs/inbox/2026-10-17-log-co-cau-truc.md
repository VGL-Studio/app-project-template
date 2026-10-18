---
date: 2026-10-17
author: Nhật (AI soạn theo trao đổi)
feature:
status: new
---

# YÊU CẦU: LOG CÓ CẤU TRÚC VÀ GHI HÀNH TRÌNH, ĐỂ AI PHÂN TÍCH PHIÊN THỬ

## Mình muốn gì

Khi dev, người kiểm thử hoặc emulator trong CI dùng thử bản staging, log của app phải đủ để AI dựng lại người dùng đã làm gì, theo thứ tự nào, mỗi bước mất bao lâu, và chỗ nào hỏng, mà không cần hỏi lại người thử. Đây là giai đoạn 3 của việc phân tích phiên thử (`docs/ops/test-sessions.md`); giai đoạn 1 (ghi phiên trên máy thật) và giai đoạn 2 (kịch bản trên emulator trước khi phát staging) đã có.

## Vì sao

Log hiện tại (ADR-0019) là chữ tự do do từng feature tự viết, ví dụ một câu báo trạng thái. Khi phân tích:

- Khó đếm và so: không có tên sự kiện hay trường cố định, AI phải đoán nghĩa từng câu.
- Thiếu hành trình: không ghi màn hình nào được mở, app vào nền hay trở lại lúc nào.
- Thiếu thời gian của thao tác: không biết bước nào chậm.
- Thiếu bối cảnh: log không ghi phiên bản, commit hay mã phiên, nên khó nối log với đúng bản build.

## Người dùng sẽ trải nghiệm thế nào

Người dùng app không thấy gì khác. Người thử chạy `scripts/device-session.sh` như hiện nay; báo cáo phân tích của AI có diễn biến chính xác hơn, có số liệu thời gian, ít mục "Log còn thiếu" hơn.

Ví dụ một dòng log mong muốn: `event=retry_pressed screen=loading attempt=2 elapsed_ms=10234`.

## Những gì chưa cần làm lần này

- Gửi log lên dịch vụ nào ngoài thiết bị (Crashlytics vẫn như ADR-0019).
- Màn hình xuất log cho người kiểm thử không có máy tính: làm sau, khi có người thử thật sự không dùng được `adb`.

## Quảng cáo, dữ liệu, file tĩnh

- Không đổi quảng cáo, sự kiện phân tích hay file R2.
- Không ghi thêm dữ liệu cá nhân. Quy tắc của ADR-0019 giữ nguyên; có thể cần một ADR mới hoặc sửa ADR-0019 cho định dạng log.

## Gợi ý cho spec và plan

- Đổi `:core:logging`: tên sự kiện và trường khóa-giá trị; dòng mở đầu mỗi lần mở app (phiên bản, commit, mã phiên); tự ghi khi mở màn hình và khi app vào nền hay trở lại; thời gian của thao tác chính. Module core nên rủi ro cao.
- Quy ước đặt tên sự kiện và trường ghi vào `CLAUDE.md`; rà soát độc lập kiểm tra.
- Feature loading và sample chuyển sang log mới, kèm test bằng `RecordingAppLogger`.
- Test hành trình trên emulator cho từng kịch bản của spec loading (`app/src/androidTest`), để phân tích trên emulator có mốc so sánh.
- Nên làm sau một hai phiên thử thật, để biết AI thật sự cần trường nào (mục Log còn thiếu của các báo cáo `docs/test-sessions/`).
