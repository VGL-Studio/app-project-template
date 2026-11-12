---
id: T-103
title: Giãn dần thời gian thử lại native khi khe chưa có quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-24]
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quảng cáo và :core:ads (điểm chặn 1, 7)
size: M
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-11
---

# T-103: GIÃN DẦN THỜI GIAN THỬ LẠI NATIVE KHI KHE CHƯA CÓ QUẢNG CÁO

## Mô tả

Nguồn: [yêu cầu 2026-11-11](../requests/2026-11-11-gian-thu-lai-native-khi-chua-co-quang-cao.md). Hôm nay khe native chưa giữ quảng cáo nào thử lại cố định mỗi 5 giây, mọi mã lỗi, không giới hạn (O-05, `AdRetryPolicy.nativeDelayMillis`), áp cho mọi định dạng native kể cả native toàn màn hình. Khi no fill kéo dài, mỗi vị trí gửi khoảng 120 yêu cầu trong 10 phút ở tiền cảnh (phiên thử trên emulator: 75 lần `Retry ... in 5000 ms` trong khoảng 8 phút). Sau ticket này, khe chưa có quảng cáo giãn dần 5, 10, 20, 40 rồi 60 giây, và native toàn màn hình không còn chờ cố định 5 giây.

## Phạm vi

- `AdRetryPolicy.nativeDelayMillis`: khe chưa giữ quảng cáo chờ 5, 10, 20, 40 giây rồi giữ 60 giây với lần lỗi liên tiếp thứ 1, 2, 3, 4, từ 5 trở đi; mọi mã lỗi, không bỏ cuộc, không tràn số với n rất lớn. Luật khi đang giữ quảng cáo (2 giây nhân đôi tới 60 giây) giữ nguyên.
- Bộ đếm lỗi của chỗ chưa có quảng cáo về 0 khi: nạp thành công, app quay lại tiền cảnh, người dùng mở một màn mới có chỗ đó, mạng về. Hiện trạng: mở màn mới tự về 0 với khe và hàng (bộ đếm là biến của từng luồng); pool và native toàn màn hình không đặt lại khi mở màn (bộ đếm của pool chỉ về 0 khi nạp thành công hay `reopen()`); dịp mở màn không áp cho native toàn màn hình, còn với pool (sống qua nhiều màn, dùng chung cho nhiều vị trí) plan chọn và nêu lý do: một màn có chỗ dùng pool mở ra thì đặt lại bộ đếm của pool đó hay không; app quay lại tiền cảnh chưa đặt lại ở đường nào; mạng về (S-24) chỉ cắt ngắn lượt chờ qua `nativeRetryResets`, không đưa bộ đếm về 0, và native toàn màn hình (`FullScreenReloads`, bộ đếm `LoadAttemptCounter`) không nghe `nativeRetryResets`. Plan liệt kê bộ đếm cho từng đường: khe và hàng (`retryingNativeAdStates`), pool (`NativeAdPool`: medium, ô lưới, giới thiệu, `native_step`), native toàn màn hình; nói rõ ở pool "chưa giữ quảng cáo" nghĩa là pool trống (`holdsAd = free.size + inUse.size > 0`), không phải ô trống. Lượt tới hạn khi app ở nền vẫn dời tới lúc hiện lại như hôm nay.
- Native toàn màn hình (cả lượt sau interstitial và lượt độc lập): bỏ luật cố định 5 giây, dùng cùng chuỗi giãn dần ở mục đầu (spec, câu hỏi 62: không dùng `min(n x 2 giây, 10 giây)` của F-05, vì luật đó vẫn gửi khoảng 60 yêu cầu trong 10 phút).
- Tài liệu: luật O-05 và dòng "Thử lại native" của bảng hằng số (mục 11) trong `docs/ops/ads-business-rules.md`, theo KB-24 đã đổi (tiêu chí 2 và tiêu chí thêm ngày 2026-11-11).
- Test, viết trước code:
  - Unit test `AdRetryPolicyTest` cho từng trường hợp: chưa giữ quảng cáo n = 1..6 và n rất lớn ra 5, 10, 20, 40, 60, 60, 60 giây; đang giữ quảng cáo giữ chuỗi cũ; native toàn màn hình theo luật đã chốt.
  - Test JVM với `AdsController` thật và đồng hồ ảo: mỗi đường (khe hay hàng, pool, native toàn màn hình) luôn lỗi trong 10 phút ở tiền cảnh, không có dịp đặt lại, gửi không quá 15 yêu cầu; sau một lần thành công, sau khi app ra nền rồi quay lại, hay sau khi mạng về, lần lỗi kế tiếp chờ lại 5 giây, mỗi dịp có test cho từng đường (khe hay hàng, pool, native toàn màn hình); dịp mở màn mới có test cho khe hay hàng, và cho pool theo nghĩa plan đã chọn; native toàn màn hình lỗi liên tiếp không còn chờ 5 giây cố định.
  - Plan chạy `scripts/existing-tests.py` để tìm test hiện khẳng định chờ 5 giây (ví dụ `AdRetryPolicyTest`, `NativeAdRetryTest`, `NativeAdPoolTest`, `FullScreenReloadsTest`, `NativeFullScreenTurnsTest`, `NetworkReturnsTest`, `AdsControllerInListRowTest`); câu khẳng định cũ trái luật mới thì viết lại theo ADR-0031.

**Không bao gồm:**

- Khóa Remote Config cho mốc chờ tối đa (mục 3, tùy chọn, của yêu cầu): không làm lần này (spec, câu hỏi 62); cần thì tách ticket khác, khai báo theo `docs/ops/remote-config.md`.
- Thử lại của interstitial, app open, rewarded (F-05) và banner (B-02): không đổi.
- Nhịp thử của ô pool (`PoolCellPolicy`, 500 ms và 2 giây) khi chờ quảng cáo của pool: không phải yêu cầu gửi lên mạng quảng cáo; plan kiểm lại điều này và ghi rõ.

## Rủi ro sơ bộ

Đổi nhịp gửi yêu cầu quảng cáo (điểm chặn 1) trong module core (điểm chặn 7): rủi ro cao, duyệt chéo. Doanh thu: no fill thoáng qua thì khe được lấp chậm hơn tối đa vài chục giây. Không đổi giao diện, không thêm khóa Remote Config.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí của KB-24 đạt, kể cả tiêu chí đổi và thêm ngày 2026-11-11.
- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging; một phiên thử trên emulator không có native cho thấy các dòng `Native <vị trí> retry in` (khe, hàng), `Pool <mã pool> failed, retry in` (pool trống) và `Retry <vị trí> in` (native toàn màn hình) giãn 5, 10, 20, 40, 60 giây.
