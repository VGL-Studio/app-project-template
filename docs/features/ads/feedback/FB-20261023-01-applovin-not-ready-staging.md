---
id: FB-20261023-01
title: Adapter AppLovin báo NOT_READY trên bản staging (thiếu SDK Key)
reporter: AI (RUN-20261023-01, phiên thử TS-20261023-01)
source: staging              # staging | nghiem-thu | store | crash
build: 0.1.0-staging (commit 95f8673)
feature: ads
ticket: T-015
severity: nhe                # chan | nang | nhe | gop-y
status: new                  # new | triaged | planned | fixed | wontfix
created: 2026-10-23
---

# FB-20261023-01: ADAPTER APPLOVIN BÁO NOT_READY TRÊN BẢN STAGING (THIẾU SDK KEY)

## Điều đã xảy ra

Trong kịch bản chuẩn trên emulator của `device-session.yml` (RUN-20261023-01), sau khi `MobileAds.initialize` xong, dòng log trạng thái adapter mà T-015 thêm vào báo AppLovin chưa sẵn sàng:

```text
10-23 12:58:51.915 I VGL.Ads : Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: NOT_READY, 50 ms, Missing or invalid SDK Key.
10-23 12:58:51.915 I VGL.Ads : Adapter com.google.android.gms.ads.MobileAds: READY, 2363 ms,
```

Kết luận: dòng log của T-015 chạy đúng (một dòng mỗi adapter, xếp theo tên). Giả thuyết (chưa kiểm): bản staging dùng mã app thử của Google, không có cấu hình mediation AppLovin trên AdMob, nên adapter không nhận được SDK Key. Tiêu chí kiểm thủ công (1) của plan T-015 ("logcat có `...AppLovinMediationAdapter: READY` sau khi đồng ý") vì vậy có thể không đạt được trên staging.

## Các bước làm lại

1. Cài bản staging dựng từ commit `95f8673`, mở app có mạng.
2. Chấp nhận hộp thoại đồng ý (nếu hiện).
3. `adb logcat -v threadtime | grep ' VGL\.Ads'`.

## Mong đợi

Plan T-015 mục 4, kiểm thủ công (1): adapter AppLovin `READY` sau khi đồng ý. Cần người quyết: tiêu chí này chỉ kiểm được trên app có mã AdMob thật và cấu hình mediation (khi đó ghi lại tiêu chí), hay staging cần cấu hình riêng.

## Ảnh chụp, video

Không có. Log ở artifact `device-session` của lần chạy 39955967527.

## Thiết bị

- Máy: Google sdk_gphone64_x86_64 (emulator của CI)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất: góp ý mới (cách kiểm tiêu chí), không phải lỗi so với spec KB-9
- **Xử lý:**
- **Lý do:**
- **Liên kết:** T-015
