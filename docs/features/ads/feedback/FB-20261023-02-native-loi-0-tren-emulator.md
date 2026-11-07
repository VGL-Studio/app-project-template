---
id: FB-20261023-02
title: Native sample_native_small tải thất bại với mã 0 trên emulator của CI
reporter: AI (RUN-20261023-01, phiên thử TS-20261023-01)
source: staging              # staging | nghiem-thu | store | crash
build: 0.1.0-staging (commit 95f8673)
feature: ads
ticket: T-010
severity: nhe                # chan | nang | nhe | gop-y
status: triaged              # new | triaged | planned | fixed | wontfix
created: 2026-10-23
---

# FB-20261023-02: NATIVE SAMPLE_NATIVE_SMALL TẢI THẤT BẠI VỚI MÃ 0 TRÊN EMULATOR CỦA CI

## Điều đã xảy ra

Trong lúc monkey dùng màn hình mẫu (12:58:53 tới 12:59:37), native small ở đáy tải thất bại 7 lần, mỗi lần với mã 0:

```text
10-23 12:58:53.147 I VGL.Ads : Native sample_native_small failed: 0
10-23 12:59:37.464 I VGL.Ads : Native sample_native_small failed: 0
```

Kết luận: vị trí `sample_native_small` được bật trên staging nhờ giá trị cục bộ của T-008, và lỗi tải được bắt, app không dừng (không có crash hay ANR; vùng quảng cáo thu lại như KB của T-010). Giả thuyết (chưa kiểm): mã 0 là lỗi nội bộ của SDK quảng cáo, thường gặp trên emulator không có Google Play đầy đủ hay khi chưa có đồng ý; không kết luận từ emulator. Log không cho biết lý do chi tiết (chỉ có mã).

## Các bước làm lại

1. Cài bản staging dựng từ commit `95f8673` lên máy thật có mạng.
2. Chấp nhận hộp thoại đồng ý, mở màn hình mẫu.
3. `adb logcat -v threadtime | grep ' VGL\.Ads'`: có dòng tải thành công hay `failed: <mã>`.

## Mong đợi

Kiểm thủ công của plan T-010 mục 4: trên staging với mã thử, khối chờ rồi native small hiện. Nếu trên máy thật vẫn `failed: 0`, nên ghi thêm thông điệp lỗi (`LoadAdError.message`) vào dòng log để biết lý do.

## Ảnh chụp, video

Không có. Log ở artifact `device-session` của lần chạy 39955967527.

## Thiết bị

- Máy: Google sdk_gphone64_x86_64 (emulator của CI)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** góp ý mới (AI đề xuất, anh Nhật đồng ý trong phiên ngày 2026-11-07)
- **Xử lý:** đưa vào backlog
- **Lý do:** không trái spec: lỗi tải được bắt, vùng quảng cáo thu lại, app không dừng. Mã 0 là lỗi nội bộ của SDK; RUN-20261025-01 (dòng Ghi chú của phần kiểm thử trên máy ảo) thấy SDK tự ghi "Incorrect native ad response. Click actions were not properly specified" cho native toàn màn hình `gallery_native_full_screen`, một định dạng khác nhưng cùng lỗi mã 0, tức native thử của Google không chạy được trên emulator, và các plan sau ghi kiểm native trên máy thật. Phần đáng làm là ý của phản hồi: dòng log lỗi tải chỉ có mã, không có lý do. Spec thêm tiêu chí ở KB-28 cho bản staging, có che mã đơn vị, vì thông điệp lỗi của SDK có thể chứa mã đơn vị (câu hỏi 58).
- **Liên kết:** T-010; T-093 (thông điệp lỗi trong log lỗi tải)
