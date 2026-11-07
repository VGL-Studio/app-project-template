---
id: T-093
title: Thông điệp lỗi của SDK trong log lỗi tải quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-28]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: low                    # đánh giá sơ bộ: low | medium | high
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-07
---

# T-093: THÔNG ĐIỆP LỖI CỦA SDK TRONG LOG LỖI TẢI QUẢNG CÁO

## Mô tả

Khi một lượt tải quảng cáo thất bại, dòng log lỗi có thêm thông điệp lỗi của SDK cạnh mã lỗi, để người đọc log biết lý do mà không phải tìm trong log riêng của SDK. Spec `ads`, KB-28, tiêu chí thêm ngày 2026-11-07 (câu hỏi 58). Từ phản hồi FB-20261023-02: native thử lỗi mã 0 trên emulator lặp ở nhiều phiên mà log chỉ có mã.

## Phạm vi

- `:core:ads`: mọi dòng log lỗi tải của từng định dạng (ví dụ `Native <vị trí> failed: <mã>` ở `NativeAdLoading.kt`, và dòng tương ứng của banner, interstitial, native toàn màn hình, app open, quảng cáo có thưởng, MAX) ghi thêm thông điệp lỗi mà SDK trả. Plan liệt kê đủ các dòng.
- Thông điệp chỉ vào log (`AppLogger`), không vào sự kiện phân tích (`ad_load_fail` giữ `load_error_code`, luật mục 8 của `docs/ops/ads-business-rules.md`). Không ghi mã đơn vị.
- Test viết trước: lượt tải thất bại với mã và thông điệp giả thì `RecordingAppLogger` có dòng mang cả hai, cho từng định dạng; sự kiện `ad_load_fail` không có thông điệp.

**Không bao gồm:**

- Kiểm adapter có trong bản build sau R8 (KB-9 tiêu chí 2, câu hỏi 57 của spec): chờ trả lời câu hỏi đó.

## Rủi ro sơ bộ

Chỉ đổi chuỗi log, không đổi cách nạp hay hiện quảng cáo. Chạm `:core:ads` (module core): plan chấm theo `risk-scoring.md`. Chờ spec `ads` được xác nhận lại (câu hỏi 58) trước khi viết plan.

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
