---
id: T-089
title: Inter khi quay lại app không nối native toàn màn hình
feature: ads
spec: features/ads/spec.md
scenarios: [KB-13, KB-14, KB-16]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: planned              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan: features/ads/plans/T-089-khong-combo-sau-inter-resume.md  # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-089: INTER KHI QUAY LẠI APP KHÔNG NỐI NATIVE TOÀN MÀN HÌNH

## Mô tả

Khi `resume_ad_type = inter`, interstitial hiện lúc người dùng quay lại app đóng xong thì người dùng đi tiếp ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật (spec `ads`, KB-14, KB-16; luật K-01, A-12 của `docs/ops/ads-business-rules.md`). Interstitial lúc mở app đã không nối native; ticket này giữ nguyên hành vi đó và thêm test khóa nó lại (KB-13): test hiện có không đi qua đường mở app với inter qua được cổng.

## Phạm vi

- `:core:ads`: lượt resume loại inter gọi interstitial không kèm combo (`ResumeAdTurns.kt`, chỗ gọi `showInterstitial(..., combo = true)`); sửa chú thích KDoc của `AdsController` đang nói resume inter nối combo.
- Test viết trước, ở hai tầng:
  - `ResumeAdTurnsTest`: câu khẳng định cũ (combo `true` cho `RESUME_INTER`) viết lại theo luật mới, theo ADR-0031. Plan chọn giữ tham số `combo` của `ResumeAdPort.showInterstitial` (luôn `false`) hay bỏ hẳn.
  - Test với `AdsController` thật, trong `AdsControllerResumeTest`, khóa `is_show_native_after_inter` bật: lượt resume loại inter đi qua cổng resume của controller rồi kết thúc, không mở native toàn màn hình.
  - Test mới với `AdsController` thật, khóa bật: `showStartupAd(OPEN_APP_INTER)` với inter qua được cổng, hai trường hợp: inter đóng bình thường, và inter hiện lỗi (K-01: inter không lên được màn); cả hai không mở native toàn màn hình. Hai test hiện có không khóa được điều này: một test tự truyền `combo = false`, một test dùng inter bị chặn ở cổng.
- `docs/ops/ads-guide.md`: dòng "Quảng cáo khi quay lại app" và dòng combo của bảng định dạng, và phần thử trên staging (`resume_ad_type = inter`: quay lại thì có inter, không có combo).
- `scripts/ads-plan.py`: các mô tả trong file Excel đang ghi `resume_inter` nối combo (vị trí `resume_inter`, khóa `resume_ad_type`, khóa `is_show_native_after_inter`). Mô tả khóa `is_show_native_after_inter` trong code (`AdsSettings.kt`): plan quyết có đổi không; nếu đổi thì sinh lại `docs/remote-config-keys.md` trong cùng PR.

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
