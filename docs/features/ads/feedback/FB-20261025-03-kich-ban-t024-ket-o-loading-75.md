---
id: FB-20261025-03
title: Kịch bản emulator T-024 lỗi: đồng ý vẫn thành GRANTED, Remote Config hết giờ, màn loading dừng ở 75%
reporter: AI (RUN-20261026-03)
source: staging
build: 0.1.0-staging (main 934695d, chưa phát)
feature: ads
ticket: T-024
severity: nang               # mức "nên sửa" của auto-run.md mục 4 (kịch bản theo plan lỗi)
status: new
created: 2026-10-25
---

# FB-20261025-03: KỊCH BẢN EMULATOR T-024 LỖI: ĐỒNG Ý VẪN THÀNH GRANTED, REMOTE CONFIG HẾT GIỜ, MÀN LOADING DỪNG Ở 75%

## Điều đã xảy ra

Kịch bản `T-024-mo-app-tu-choi-dong-y.txt` (phiên `TS-20261025-04`, lần chạy 40228765790) lỗi ở bước 3 (`wait id=sample_screen 60`). Bước 2 (`consent decline 30`) báo `ok`, nhưng app ghi `Consent path: GRANTED`, như FB-20261025-01. Lần này app không tới bước quảng cáo mở app: Remote Config hết giờ, màn loading dừng ở 75% với chữ "Tiến độ đã dừng" và nút thử lại (`fail.xml`: `loading_error`, `loading_retry`, `loading_percent` "75%").

```text
10-25 20:20:31.976 I VGL.Session: scenario=plan file=T-024-mo-app-tu-choi-dong-y.txt
10-25 20:20:45.210 W VGL.RemoteConfig: Refresh timed out after 10000 ms
10-25 20:20:45.210 W VGL.Loading: Task REMOTE_CONFIG failed
10-25 20:20:45.210 W VGL.Loading: 1 task(s) not done, waiting for retry
10-25 20:20:51.929 I VGL.Ads : Consent path: GRANTED
```

Kết luận: kịch bản lỗi vì hai nguyên nhân không thuộc task của phiên: chạm "Do not consent" trên emulator vẫn cho `GRANTED` (đã ghi ở FB-20261025-01), và Remote Config của staging (Firebase giả) hết giờ lần đầu nên màn loading chờ người bấm thử lại. Giả thuyết: kịch bản cần bước bấm `loading_retry` khi thấy `loading_error`, hoặc đợi lâu hơn.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính `934695d`.
2. Xem bảng "Kịch bản theo plan" của `summary.md` và `scenarios/T-024-mo-app-tu-choi-dong-y/fail.xml`.

## Mong đợi

Từ chối đồng ý thì màn loading không chờ quảng cáo mở app, app vào màn chính (KB-13, T-024).

## Ảnh chụp, video

`scenarios/T-024-mo-app-tu-choi-dong-y/fail.png` trong artifact `device-session` của lần chạy 40228765790.

## Thiết bị

- Máy: emulator của `device-session.yml`
- Phiên bản Android: theo workflow
- Ngôn ngữ, chế độ tối: mặc định

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI: xử lý cùng FB-20261025-01 (sửa kịch bản hay cách từ chối đồng ý trên emulator), thêm bước thử lại khi màn loading báo lỗi.
