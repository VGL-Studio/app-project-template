---
id: T-093
title: Thông điệp lỗi của SDK trong log lỗi tải quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-28]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: on-staging              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan: features/ads/plans/T-093-thong-diep-loi-trong-log-tai.md # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-07
---

# T-093: THÔNG ĐIỆP LỖI CỦA SDK TRONG LOG LỖI TẢI QUẢNG CÁO

## Mô tả

Ở bản staging, khi SDK trả lỗi cho một lượt tải quảng cáo, dòng log lỗi tải có thêm thông điệp lỗi của SDK cạnh mã lỗi, đã che chuỗi dạng mã đơn vị, để người đọc log biết lý do mà không phải tìm trong log riêng của SDK. Spec `ads`, KB-28, tiêu chí thêm ngày 2026-11-07 (câu hỏi 58). Từ phản hồi FB-20261023-02: native thử lỗi mã 0 trên emulator lặp ở nhiều phiên mà log chỉ có mã.

## Phạm vi

- `:core:ads`, chỉ ở biến thể staging: các dòng log lỗi tải mà SDK trả lỗi ghi thêm thông điệp lỗi, qua một hàm che chuỗi dạng mã đơn vị (`ca-app-pub-…`, mã đơn vị MAX). Các dòng đã thấy trên nhánh chính ngày 2026-11-07 (plan đối chiếu lại):
  - native: `NativeAdLoading.kt` (`Native <vị trí> failed: <mã>`; `onFailed` hôm nay chỉ nhận mã, nên sửa cả đường từ `SdkNativeAdLoader`), dòng thử lại ở `NativeAdRefresh.kt`; kho `NativeAdPool.kt` (dòng lỗi hôm nay không có mã);
  - quảng cáo toàn màn hình (interstitial, native toàn màn hình, app open, có thưởng): các dòng lỗi tải ở `AdsController.kt` và `FullScreenReloads.kt`;
  - banner AdMob: dòng thử lại có mã ở `BannerLoads.kt`, `PlatformBanners.kt` (`BannerLoadResult.Failed` hôm nay chỉ mang mã, `BannerAdView.kt`); banner và MREC của MAX: `MaxBannerAdView.kt`.
- Bản production giữ dòng log chỉ có mã. Thông điệp không vào sự kiện phân tích (`ad_load_fail` giữ `load_error_code`, luật mục 8 của `docs/ops/ads-business-rules.md`).
- Test viết trước: ở staging, lượt tải lỗi với mã và thông điệp giả thì `RecordingAppLogger` có dòng mang cả hai, cho từng định dạng; thông điệp có mã đơn vị mẫu thì dòng log không có `ca-app-pub`; ở production dòng log không có thông điệp; sự kiện `ad_load_fail` không có thông điệp.
- Giữ các test của log từng mạng (`AdNetworkResponsesTest`: không đọc thông điệp, dòng `Network …` không có chữ của thông điệp); thông điệp chỉ ghi ở dòng lỗi tải, không qua `AdNetworkLog`.

**Không bao gồm:**

- Dòng `failed: 0 (request not built)`, dòng chỉ ghi tên lớp ngoại lệ (ADR-0019), dòng lỗi lúc hiện (`failed to show`): giữ nguyên.
- Kiểm adapter có trong bản build sau R8 (KB-9 tiêu chí 2, câu hỏi 57 của spec): chờ trả lời câu hỏi đó.

## Rủi ro sơ bộ

Không đổi cách nạp hay hiện quảng cáo, nhưng chạm `:core:ads` (điểm chặn 7 của `risk-scoring.md`) và dữ liệu lỗi của SDK có thể chứa mã đơn vị (gần điểm chặn 4): rủi ro cao. Nối thông điệp qua nhiều đường tải nên cỡ M. Chờ spec `ads` được xác nhận lại (câu hỏi 58) trước khi viết plan.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
