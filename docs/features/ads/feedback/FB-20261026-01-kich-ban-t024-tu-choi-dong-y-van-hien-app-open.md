---
id: FB-20261026-01
title: Kịch bản emulator T-024 lại lỗi ở bước 3: chạm "Do not consent" mà cổng vẫn mở, app open thử che màn chính
reporter: AI (RUN-20261026-05)
source: staging
build: 0.1.0-staging (main 04e80c0, trước khi phát bản của RUN-20261026-05)
feature: ads
ticket: T-024
severity: nang               # mức "nên sửa" của auto-run.md mục 4 (kịch bản theo plan lỗi)
status: fixed
created: 2026-10-26
---

# FB-20261026-01: KỊCH BẢN EMULATOR T-024 LẠI LỖI Ở BƯỚC 3: CHẠM "DO NOT CONSENT" MÀ CỔNG VẪN MỞ, APP OPEN THỬ CHE MÀN CHÍNH

## Điều đã xảy ra

Kịch bản `T-024-mo-app-tu-choi-dong-y.txt` (phiên `TS-20261026-02`, lần chạy `device-session` 40377991748) lỗi ở bước 3 (`wait id=sample_screen 60`, 75 giây). Bước 2 (`consent decline 30`) báo `ok` (đã chạm từ chối ở 160,486), nhưng app ghi `Consent path: GRANTED` và hiện quảng cáo mở app thử. Lúc kịch bản chờ màn chính, quảng cáo vẫn che màn (`fail.xml` có `ad_iframe`, "Test Ad", "Continue to app"), nên không thấy `sample_screen`. Cùng nguyên nhân với FB-20261025-01; ở RUN-20261026-04 kịch bản này đạt (17 giây), nên lỗi không ổn định.

```text
10-26 18:47:40.343 I VGL.Session: scenario=plan file=T-024-mo-app-tu-choi-dong-y.txt
10-26 18:47:42.267 I VGL.Ads : Consent update sent
10-26 18:47:45.156 I VGL.Ads : Consent update done
10-26 18:47:50.511 I VGL.Ads : Consent path: GRANTED
10-26 18:47:53.651 I VGL.Ads : startup ad: ready (open_app) after 11652 ms
10-26 18:47:53.667 I VGL.Ads : App open shown: open_app
```

Kết luận: kịch bản lỗi vì lần chạm "Do not consent" trên form UMP thử của emulator không thành từ chối (đường đồng ý ra `GRANTED`), không phải vì task của phiên (T-043, T-042, T-044 không đổi đồng ý hay quảng cáo mở app). Giả thuyết, chưa kiểm: form UMP thử trên emulator có hai trang và lần chạm đầu chỉ mở trang tùy chọn, hoặc tọa độ chạm lệch nút; xem `fail.png` và `notes.md` của phiên.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính (`reason` bất kỳ).
2. Xem bảng "Kịch bản theo plan" của `summary.md` và `scenarios/T-024-mo-app-tu-choi-dong-y/fail.xml`.
3. Tìm `Consent path:` trong `app-log.txt` sau dòng `scenario=plan file=T-024-mo-app-tu-choi-dong-y.txt`.

## Mong đợi

Từ chối đồng ý thì cổng đóng, màn loading không chờ quảng cáo mở app, app vào màn chính (T-024, KB của spec về đồng ý quảng cáo).

## Ảnh chụp, video

`scenarios/T-024-mo-app-tu-choi-dong-y/fail.png` trong artifact `device-session` của lần chạy 40377991748.

## Thiết bị

- Máy: emulator Google sdk_gphone64_x86_64
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** lỗi của kịch bản emulator (cùng FB-20261025-01), không phải lỗi so với spec
- **Xử lý:** cùng phần sửa của FB-20261025-01
- **Lý do:** lần chạm "Do not consent" thật sự là từ chối; UMP vẫn cho quảng cáo không cá nhân hóa, nên app đi đường `GRANTED` đúng luật. Kịch bản lúc đạt lúc lỗi tùy app open thử nạp xong trước hay sau khi qua màn loading
- **Liên kết:** FB-20261025-01, FB-20261025-03
