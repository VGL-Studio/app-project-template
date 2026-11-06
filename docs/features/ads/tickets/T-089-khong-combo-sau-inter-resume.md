---
id: T-089
title: Inter khi quay lại app không nối native toàn màn hình
feature: ads
spec: features/ads/spec.md
scenarios: [KB-13, KB-14, KB-16]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-089: INTER KHI QUAY LẠI APP KHÔNG NỐI NATIVE TOÀN MÀN HÌNH

## Mô tả

Khi `resume_ad_type = inter`, interstitial hiện lúc người dùng quay lại app đóng xong thì người dùng đi tiếp ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật (spec `ads`, KB-14, KB-16; luật K-01, A-12 của `docs/ops/ads-business-rules.md`). Interstitial lúc mở app đã không nối native; ticket này giữ nguyên hành vi đó và thêm test khóa nó lại nếu còn thiếu (KB-13).

## Phạm vi

- `:core:ads`: lượt resume loại inter gọi interstitial không kèm combo (`ResumeAdTurns.kt`, chỗ gọi `showInterstitial(..., combo = true)`); sửa chú thích KDoc của `AdsController` đang nói resume inter nối combo.
- Test viết trước: lượt resume loại inter không mở combo khi `is_show_native_after_inter` bật; câu khẳng định cũ của `ResumeAdTurnsTest` (combo `true` cho `RESUME_INTER`) viết lại theo luật mới, theo ADR-0031. Có test cho inter lúc mở app không mở combo khi khóa bật, nếu chưa có.
- `docs/ops/ads-guide.md`: dòng "Quảng cáo khi quay lại app" và dòng combo của bảng định dạng, và phần thử trên staging (`resume_ad_type = inter`: quay lại thì có inter, không có combo).

**Không bao gồm:**

- Đổi khóa Remote Config hay thêm khóa mới.
- Inter của vị trí trong app: vẫn nối combo như hiện nay.

## Rủi ro sơ bộ

Đổi tần suất quảng cáo toàn màn hình (điểm chặn 1 của risk-scoring.md) và chạm `:core:ads` (module core, điểm chặn 7): plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. Thay đổi nhỏ, bớt một quảng cáo toàn màn hình, không thêm vị trí, SDK hay quyền.

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
