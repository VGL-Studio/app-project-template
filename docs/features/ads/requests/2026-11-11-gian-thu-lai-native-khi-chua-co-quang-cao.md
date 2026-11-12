---
date: 2026-11-11
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: GIÃN DẦN THỜI GIAN THỬ LẠI NATIVE KHI KHE CHƯA CÓ QUẢNG CÁO

Dev dán đề xuất vào phiên làm việc với AI ngày 2026-11-11. Spec: KB-24 và câu hỏi 62 của [spec ads](../spec.md). Ticket: [T-103](../tickets/T-103-gian-thu-lai-native-khi-chua-co-quang-cao.md).

## Hiện trạng lúc ghi

Luật O-05 của `docs/ops/ads-business-rules.md`, code ở `AdRetryPolicy.nativeDelayMillis` của `:core:ads`:

- Khe đang giữ quảng cáo mà làm mới lỗi: chờ 2, 4, 8, 16, 32 giây, rồi 60 giây. Bộ đếm về 0 khi thành công.
- Khe chưa giữ quảng cáo nào: luôn chờ 5 giây, không giãn dần, không giới hạn số lần, mọi mã lỗi.
- Luật này áp cho mọi định dạng native: native nhỏ, native trong danh sách hay lưới, native ở bước của luồng, native toàn màn hình (kể cả lượt native sau interstitial).

## Vấn đề

1. Không có quảng cáo kéo dài (no fill, mạng yếu, vùng ít nhà quảng cáo): app ở tiền cảnh gửi 1 yêu cầu mỗi 5 giây, khoảng 12 yêu cầu mỗi phút cho mỗi vị trí, mãi tới khi có quảng cáo. Mỗi vị trí đang chờ tạo một luồng thử lại như vậy.
2. Tốn pin và dữ liệu di động; tỷ lệ khớp (match rate) trên AdMob có thể giảm; lượng yêu cầu lỗi lớn có thể ảnh hưởng cách mạng quảng cáo đánh giá app.
3. Native toàn màn hình chỉ hiện ở một thời điểm cụ thể (ngay sau interstitial), không có khe nào đang trống trên màn, nên lý do "thử nhanh để lấp khe trống" không đứng vững; vậy mà nó vẫn chịu luật 5 giây.
4. Không có khóa Remote Config nào để chỉnh; muốn đổi phải phát hành bản mới.

## Bằng chứng

Trên emulator (không có quảng cáo native), một phiên thử khoảng 8 phút ghi 75 cảnh báo `Native full-screen ... failed to load: 0`, mỗi dòng kèm `Retry ... in 5000 ms (failures N, code 0)`, đều đặn mỗi 5 giây.

## Mình muốn gì

1. Khe chưa có quảng cáo: vẫn bắt đầu ở 5 giây để lấp khe nhanh, nhưng giãn dần sau mỗi lần lỗi liên tiếp: 5, 10, 20, 40 giây, rồi giữ ở 60 giây. Bộ đếm về 0 khi tải thành công, khi app quay lại tiền cảnh, hoặc khi người dùng mở một màn mới có khe đó.
2. Native toàn màn hình: dùng chung luật giãn dần với interstitial và app open (`min(n x 2 giây, 10 giây)`), hoặc luật giãn dần ở mục 1. Không dùng luật cố định 5 giây.
3. Tùy chọn: đưa mốc chờ tối đa (mặc định 60 giây) thành một khóa Remote Config.
4. Giữ nguyên các luật khác: không gửi yêu cầu khi app ở nền, kiểm lại cổng đồng ý ngay trước khi gửi, luật khi đang giữ quảng cáo (2 giây nhân đôi tới 60 giây).

## Tiêu chí nghiệm thu

- Khe chưa có quảng cáo, lỗi liên tiếp: các lần chờ là 5, 10, 20, 40, 60, 60, … giây.
- Tải thành công, hoặc app quay lại tiền cảnh: lần lỗi kế tiếp chờ lại từ 5 giây.
- Native toàn màn hình lỗi liên tiếp: thời gian chờ theo luật đã chọn ở mục 2, không còn cố định 5 giây.
- Trong 10 phút ở tiền cảnh mà luôn no fill, số yêu cầu của một vị trí không quá khoảng 15, thay vì khoảng 120 như hiện tại.
- Có unit test cho hàm tính thời gian chờ ở từng trường hợp.

## Ảnh hưởng

- Doanh thu: no fill thoáng qua thì khe được lấp chậm hơn tối đa vài chục giây; no fill kéo dài thì gần như không mất gì, vì các lần thử nhanh vốn cũng thất bại.
- Không đổi giao diện, không đổi khóa Remote Config hiện có.
