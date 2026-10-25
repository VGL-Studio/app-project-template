---
id: FB-20261025-02
title: ANR trong AdActivity của SDK quảng cáo khi chạy monkey trên emulator
reporter: AI (RUN-20261026-03)
source: staging
build: 0.1.0-staging (main 934695d, chưa phát)
feature: ads
ticket:
severity: chan               # auto-run.md mục 4: crash hay ANR thì không phát staging
status: new
created: 2026-10-25
---

# FB-20261025-02: ANR TRONG ADACTIVITY CỦA SDK QUẢNG CÁO KHI CHẠY MONKEY TRÊN EMULATOR

## Điều đã xảy ra

Kịch bản chuẩn trên emulator (`device-session.yml`, lần chạy 40228765790, phiên `TS-20261025-04`, nhánh chính `934695d`) báo 2 ANR và monkey báo treo, mức chặn. Không có crash. Phiên RUN-20261026-02 (nhánh chính `3d4f165`, cùng hạt giống monkey 20261017) báo 0 ANR.

Dòng log làm bằng chứng (giờ UTC trên emulator):

```text
10-25 20:19:14.705 I VGL.Session: scenario=monkey events=1500 seed=20261017
10-25 20:20:14.455 I WindowManager: ANR in Window{... com.vglstudio.template.staging/com.google.android.gms.ads.AdActivity}. Reason:Input dispatching timed out (... AdActivity (server) is not responding. Waited 5001ms for MotionEvent(... source=TRACKBALL ...))
10-25 20:20:15.408 I WindowManager: ANR in ActivityRecord{... com.vglstudio.template.MainActivity t11}.  Reason: Input dispatching timed out (Application does not have a focused window)
10-25 20:20:15.705 D VGL.Ads : Native refill skipped: full-screen ad showing
10-25 20:20:18.952 E ActivityManager: ANR in com.vglstudio.template.staging (com.vglstudio.template.staging/com.google.android.gms.ads.AdActivity)
```

Monkey dừng sau 958 trên 1500 sự kiện. `crash-anr.txt` (dropbox) không có mục `data_app_anr`.

Kết luận: ANR xảy ra khi một quảng cáo toàn màn hình của SDK (`AdActivity`, quảng cáo thử) đang hiện, trong lúc monkey gửi sự kiện trackball. Giả thuyết, chưa kiểm chứng: `AdActivity` (WebView của quảng cáo thử trên emulator không có GPU) không xử lý kịp sự kiện; hai task merge trong phiên (T-027 `feb516e`, T-040 `934695d`) chỉ đổi native, không đổi interstitial hay `AdActivity`, nên chưa thấy commit nào của phiên gây ra. Log không đủ để biết quảng cáo đang hiện thuộc vị trí nào (không có dòng `VGL.Ads` hiện quảng cáo toàn màn hình trong 10 giây trước ANR).

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính `934695d` (`reason` tùy ý).
2. Chờ bước monkey (`seed=20261017`, 1500 sự kiện).
3. Xem `summary.md` và `logcat.txt` của artifact `device-session`.

## Mong đợi

Không có ANR; phiên trước cùng hạt giống không có.

## Ảnh chụp, video

Không có ảnh lúc ANR. Artifact `device-session` của lần chạy 40228765790 có `logcat.txt`, `monkey.txt`.

## Thiết bị

- Máy: emulator của `device-session.yml` (GitHub Actions, không GPU)
- Phiên bản Android: theo workflow
- Ngôn ngữ, chế độ tối: mặc định

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI: chạy lại `device-session.yml` một lần trên `934695d` để biết ANR có lặp lại không. Lặp lại thì so với `3d4f165` để tìm commit; không lặp thì xếp vào lỗi môi trường của emulator và phát staging.
