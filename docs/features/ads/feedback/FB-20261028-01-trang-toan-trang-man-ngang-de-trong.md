---
id: FB-20261028-01
title: Trang toàn trang của luồng để trống khi xoay ngang trên điện thoại thật
reporter: Nhật (AI thử trên máy của anh Nhật)
source: staging
build: 0.1.0-staging.17
feature: ads
ticket: T-049
severity: nang
status: fixed
created: 2026-10-28
---

# FB-20261028-01: TRANG TOÀN TRANG CỦA LUỒNG ĐỂ TRỐNG KHI XOAY NGANG TRÊN ĐIỆN THOẠI THẬT

## Điều đã xảy ra

Ở màn xem thử, mục "Flow steps", bước có trang toàn trang hiện quảng cáo đúng khi máy để dọc. Xoay ngang thì vùng trang để trống hoàn toàn suốt 25 giây: không có khung chờ, không có quảng cáo. Xoay về dọc thì quảng cáo mới hiện ngay.

Log (`VGL.Ads`):

```
07:34:53.601 Flow step full page left: gallery_native_step_full
07:34:53.606 Reload gallery_native_step_full in 0 ms
(không có dòng "shown" nào trong lúc màn ngang)
07:35:01.049 Flow step full page shown: gallery_native_step_full   <- sau khi xoay về dọc
```

Cây giao diện ở màn ngang: vùng trang `[94,292][2400,805]`, cao 513 px. Máy có mật độ 440 dpi, nên vùng trang cao khoảng 187 dp.

Kết luận: code chạy đúng thiết kế của T-041. Trang thấp hơn `FlowStepMetrics.minHeight` (khoảng 200 dp ở cỡ chữ 1.0) thì không lấy quảng cáo (`NativeFullScreenStepGate`), và cũng không vẽ khung chờ. Plan T-041 đo ở khung Robolectric `w640dp-h360dp` (204 dp) và `w891dp-h411dp` (255 dp). Các khung này không có thanh trạng thái, thanh điều hướng hay phần khoét camera. Trên máy thật (khoảng 873 x 393 dp ở màn ngang) các phần đó chiếm thêm chỗ, nên trang thấp hơn ngưỡng.

Hệ quả: trên điện thoại thường, xoay ngang ở bước có trang toàn trang chỉ thấy một vùng trắng. Người dùng không có quảng cáo, không có khung chờ, cũng không biết vì sao trang trống.

## Các bước làm lại

1. Cài bản `0.1.0-staging.17` lên một điện thoại thường (đã thử trên Redmi Note 12, 2400 x 1080, 440 dpi), để cỡ chữ hệ thống 1.0.
2. Mở app, vào màn xem thử, cuộn tới "Flow steps", bấm Next tới trang toàn trang.
3. Xoay máy sang ngang, chờ 25 giây.

## Mong đợi

Tiêu chí kiểm tra thủ công của T-041 (báo cáo RUN-20261026-06): xoay ngang rồi dọc thì thấy khung chờ rồi quảng cáo mới, CTA bấm được. Nếu màn ngang thật sự quá thấp thì không nên để một vùng trắng không lời giải thích.

## Ảnh chụp, video

Chưa đính kèm. Ảnh chụp lúc thử nằm trong máy của AI, không đưa vào git.

## Thiết bị

- Máy: Xiaomi Redmi Note 12 (`23021RAAEG`), 2400 x 1080, 440 dpi
- Phiên bản Android: chưa ghi (máy đã rút trước khi đọc)
- Ngôn ngữ, chế độ tối: tiếng Việt, chế độ sáng

## Phân loại

- **Loại:** lỗi so với spec (tiêu chí kiểm thủ công của T-041); test của plan không tính các thanh hệ thống
- **Xử lý:** đưa vào backlog, ticket T-049; đã sửa ở PR #244 (RUN-20261028-02, bản `0.1.0-staging.18`), Nhật kiểm trên máy thật 2026-10-28
- **Lý do:** đây không phải lỗi dừng app. Màn ngang ở bước của luồng ít gặp, nhưng vẫn mất quảng cáo và người dùng thấy giao diện trống.
- **Liên kết:** `features/ads/tickets/T-049-trang-toan-trang-man-ngang.md`

## Ghi chú thêm từ cùng lần thử

- ~~Ở cỡ chữ 200%, nút "Native full screen" của màn xem thử (chỉ có ở staging) xuống ba dòng và bị cắt chữ "screen". Gộp vào T-049.~~ **Sửa 2026-10-28:** quan sát này nhầm. Lúc thử, `font_scale` đúng là 2.0, cỡ hiển thị không đổi. Xem lại ảnh chụp: nút nằm sát mép dưới màn hình, phần dưới của nút chỉ nằm ngoài vùng đang xem (chưa cuộn tới), chữ không bị cắt. Chạy thử trên Robolectric lúc soạn plan T-049 cũng thấy nút cao theo chữ, ba dòng, không tràn. Bỏ khỏi T-049.
- Ở cỡ chữ 200%, T-045 đạt: CTA và nhãn "Ad" đọc trọn ở native small, native medium và trang toàn trang. Ở màn dọc, T-041 đạt.
