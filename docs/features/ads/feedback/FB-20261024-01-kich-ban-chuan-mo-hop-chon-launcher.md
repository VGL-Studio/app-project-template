---
id: FB-20261024-01
title: Kịch bản chuẩn trên emulator mở hộp chọn launcher thay vì app (staging có hai biểu tượng)
reporter: AI (RUN-20261024-02)
source: staging
build: 0.1.0-staging (main 97ba714)
feature: ads
ticket: T-014
severity: nang
status: fixed
created: 2026-10-24
---

# FB-20261024-01: KỊCH BẢN CHUẨN TRÊN EMULATOR MỞ HỘP CHỌN LAUNCHER THAY VÌ APP

## Điều đã xảy ra

Trong phiên thử `TS-20261024-01` (workflow `device-session.yml`, lần chạy 40070610334, RUN-20261024-02), các kịch bản `cold_start` (3 lần) và `offline_first_launch` không mở app. Bản staging có hai activity `LAUNCHER` từ T-014 (`MainActivity` và màn xem thử quảng cáo), nên `cmd package resolve-activity --brief -c android.intent.category.LAUNCHER` trả về hộp chọn của hệ thống, và `am start -W -n` mở hộp chọn đó:

```text
06:43:26.513 wm_create_activity: [...,android/com.android.internal.app.ResolverActivity,...]
06:43:27.254 ActivityManager: Start proc 3239:system:ui/1000 for next-top-activity {android/com.android.internal.app.ResolverActivity}
```

Cả phiên chỉ có một lần app khởi động (`06:44:55.750 Start proc 5713:com.vglstudio.template.staging ... MainActivity`), do monkey mở. Ba số khởi động nguội ở `summary.md` (3672, 250, 401 ms) là thời gian mở hộp chọn, không phải của app; kịch bản mất mạng lúc mở lần đầu không chạy qua app.

Đây là kết luận từ log. Giả thuyết: phiên thử của RUN-20261024-01 (cùng ngày, sau T-014) cũng bị như vậy; chưa kiểm.

## Các bước làm lại

1. Khởi động `device-session.yml` trên nhánh chính.
2. Mở `logcat.txt` của artifact, tìm `ResolverActivity` quanh các dòng `VGL.Session: scenario=cold_start`.

## Mong đợi

Kịch bản chuẩn mở đúng `MainActivity` của bản staging (`docs/ops/test-sessions.md`, mục 4), để số khởi động nguội và kịch bản mất mạng có nghĩa.

## Ảnh chụp, video

`after-start.png`, `offline.png` trong artifact `device-session` của lần chạy 40070610334.

## Thiết bị

- Máy: emulator `sdk_gphone64_x86_64`
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: lỗi của script (`scripts/device-session.sh` dòng 189), không phải lỗi của app.
- **Xử lý:** đề xuất: script chọn activity `LAUNCHER` thuộc gói của app có tên `MainActivity` (hoặc lấy tên từ `aapt dump badging`, `launchable-activity`), hoặc gọi `am start` với `-a MAIN -c LAUNCHER -n <gói>/com.vglstudio.template.MainActivity`; test của script trong `scripts/tests/run.sh`. Phiên tự chạy không sửa `scripts/` (auto-run.md, mục 6).
- **Lý do:** anh Nhật đồng ý sửa ngoài phiên (2026-10-24). `scripts/device-session.sh` lấy mọi activity `LAUNCHER` của gói bằng `cmd package query-activities`, chọn activity tên `MainActivity`; chỉ có một activity thì dùng nó; còn lại thì dừng với thông báo rõ. Test trong `scripts/tests/run.sh` (hai launcher, không có `MainActivity`).
- **Liên kết:** PR duyệt RUN-20261024-03 (nhánh `claude/duyet-run-20261024-03`). Số khởi động nguội của TS-20261024-01 và có thể của phiên thử RUN-20261024-01 không dùng được.
