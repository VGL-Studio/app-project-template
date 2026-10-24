---
id: FB-20261024-02
title: Log trạng thái adapter trên emulator chỉ có AppLovin, thiếu tám mạng của T-034
reporter: AI (RUN-20261024-03, phiên thử TS-20261024-02)
source: staging
build: 0.1.0-staging, commit 507cac5
feature: ads
ticket: T-034
severity: nang
status: new
created: 2026-10-24
---

# FB-20261024-02: LOG TRẠNG THÁI ADAPTER CHỈ CÓ APPLOVIN, THIẾU TÁM MẠNG CỦA T-034

## Điều đã xảy ra

Kịch bản chuẩn trên emulator (`device-session.yml`, lần chạy 40102285160, bản staging dựng từ `507cac5`, đã có T-034) ghi log trạng thái adapter của một lần `MobileAds.initialize`. Log chỉ có 4 dòng `Adapter ...`, không có dòng nào của tám adapter mới (Meta, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity):

```text
14:37:10.927 I VGL.Ads : Adapter com.google.ads.mediation.adcolony.AdColonyMediationAdapter: NOT_READY, 0 ms, Failed to create Adapter.
14:37:10.928 I VGL.Ads : Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: NOT_READY, 23 ms, Missing or invalid SDK Key.
14:37:10.930 I VGL.Ads : Adapter com.google.ads.mediation.vungle.VungleMediationAdapter: NOT_READY, 0 ms, Failed to create Adapter.
14:37:10.932 I VGL.Ads : Adapter com.google.android.gms.ads.MobileAds: READY, 1271 ms,
```

SDK của Unity có trong bản build và chạy: `14:35:31.641 D UnityAds: ... Wrote file: .../UnityAdsStorage-public-data.json`, trước dòng `14:35:31.922 I VGL.Ads : Consent update sent`.

Giả thuyết, chưa kiểm: danh sách trạng thái của `MobileAds` lấy theo cấu hình mediation AdMob gửi xuống cho mã app thử (AdColony, Vungle, AppLovin), không theo adapter có trong bản build. Nếu đúng thì vế 1 của KB-9 tiêu chí 2 ("log trạng thái liệt kê đủ chín adapter") không đạt được trên staging, giống vế 2. Giả thuyết khác: bản build của phiên thử đã thu gọn bằng R8 (stack trace có `r8-map-id`) và adapter bị bỏ; `:app:minifyStagingReleaseWithR8` của T-034 qua nhưng không kiểm lớp adapter còn trong APK.

## Các bước làm lại

1. Cài bản staging từ `staging.yml` của nhánh chính `507cac5`.
2. Đồng ý quảng cáo.
3. `adb logcat -v threadtime | grep ' VGL\.Ads'`, đếm dòng `Adapter ...`.

## Mong đợi

KB-9 tiêu chí 2 vế 1: log trạng thái liệt kê đủ chín adapter (kiểm thủ công 1 của plan T-034).

## Ảnh chụp, video

Không có. Log ở artifact `device-session` của lần chạy 40102285160 (`app-log.txt` dòng 224 đến 227).
