---
id: FB-20261025-01
title: Kịch bản emulator T-024 lỗi: chạm "Do not consent" mà cổng vẫn mở, app open thử hiện và chặn màn chính
reporter: AI (RUN-20261026-02)
source: staging
build: 0.1.0-staging (main 3d4f165)
feature: ads
ticket: T-024
severity: nang               # mức "nên sửa" của auto-run.md mục 4 (kịch bản theo plan lỗi)
status: fixed
created: 2026-10-25
---

# FB-20261025-01: KỊCH BẢN EMULATOR T-024 LỖI: CHẠM "DO NOT CONSENT" MÀ CỔNG VẪN MỞ, APP OPEN THỬ HIỆN VÀ CHẶN MÀN CHÍNH

## Điều đã xảy ra

Kịch bản `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt` lỗi ở bước 3, `wait id=sample_screen 60`, sau 72 giây (phiên thử TS-20261025-03 của `device-session.yml`, lần chạy 40209787112). Kịch bản và plan T-024 (mục 4) giả định rằng từ chối đồng ý thì cổng quảng cáo đóng, nên màn loading không chờ quảng cáo. Log trên emulator cho thấy giả định này sai:

- Bước `consent decline` chạm đúng nút từ chối: UMP ghi `IABTCF_PurposeConsents` toàn số 0 (16:18:20.672).
- Nhưng UMP đóng hộp thoại với `CONSENT_SIGNAL_SUFFICIENT` (16:18:20.685), nên `canRequestAds` vẫn đúng. App ghi `Consent path: GRANTED` (16:18:20.716), tức cổng mở với quảng cáo không cá nhân hóa.
- Đây là lần mở đầu (`First open since install`, 16:18:12.920), và staging bật `show_open_ads_first_open`. SDK sẵn sàng lúc 16:18:21.665, app open thử nạp xong, và màn loading hiện nó: `startup ad: ready (open_app) after 12041 ms`, `App open shown: open_app` (16:18:23.564, 16:18:23.571).
- Kịch bản không đóng quảng cáo, nên màn chính không hiện. `fail.xml` có `Test Ad`, `Continue to app`, `close-button` của app open thử.

App làm đúng luật cổng hiện có (UMP cho phép gửi yêu cầu thì cổng mở) và đúng KB-13. Kịch bản và câu trong plan "từ chối đồng ý thì cổng đóng" sai với hành vi của UMP. Không có crash hay ANR.

## Các bước làm lại

1. Cài bản staging mới (hay xóa dữ liệu app) trên emulator API 34, có mạng.
2. Mở app; khi hộp thoại đồng ý hiện, chạm "Do not consent".
3. Chờ: khoảng 12 giây sau khi mở app, app open thử hiện trên màn loading và ở đó tới khi người dùng đóng.

## Mong đợi

Kịch bản theo plan phải kiểm được tiêu chí "màn loading không treo". Có hai cách sửa kịch bản. Cách 1: thêm bước đóng quảng cáo (chạm `Continue to app` hay nút đóng) trước `wait id=sample_screen`; khi đó kịch bản chứng minh quảng cáo đóng thì vào màn chính (KB-13). Cách 2: đổi điều kiện để cổng thật sự đóng, nếu spec có trường hợp như vậy. Người cũng cần xác nhận: từ chối đồng ý ở EEA mà UMP vẫn cho quảng cáo không cá nhân hóa, thì có hiện quảng cáo mở app không. Theo code và spec hiện tại câu trả lời là có.

Hệ quả phụ: `AppLaunchTest` (emulator, `ui-test`) chờ `sample_screen` tối đa 30 giây. Lần chạy của PR #170 xanh. Nhưng nếu app open thử nạp xong trước khi test qua màn loading thì nó che màn chính, và test có thể chập chờn. Nên xem trong plan sửa.

## Ảnh chụp, video

`fail.png`, `fail.xml` trong artifact `device-session` của lần chạy 40209787112 (thư mục `TS-20261025-03/scenarios/T-024-mo-app-tu-choi-dong-y/`); không đưa vào repo.

## Thiết bị

- Máy: emulator Google `sdk_gphone64_x86_64` (CI)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** lỗi của kịch bản và của giả định trong plan T-024, không phải lỗi của app so với spec
- **Xử lý:** sửa `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt`: sau khi từ chối, chạm "Continue to app" khi app open thử hiện, rồi chờ màn chính; câu tiêu chí đổi theo
- **Lý do:** anh Nhật xác nhận 2026-10-27: từ chối đồng ý mà UMP vẫn cho quảng cáo không cá nhân hóa (`canRequestAds`) thì cổng mở, app open lần đầu vẫn hiện (spec KB-13, KB-11 không đổi). `AppLaunchTest` tìm `sample_screen` trong cây ngữ nghĩa của `MainActivity`, nên quảng cáo che màn không làm nó lỗi
- **Liên kết:** FB-20261026-01, FB-20261025-03, plan T-024 (nhật ký RUN-20261026-02), PR #170
