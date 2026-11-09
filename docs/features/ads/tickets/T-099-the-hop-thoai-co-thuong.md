---
id: T-099
title: Thẻ hộp thoại quảng cáo có thưởng công khai cho screenshot test, cách dịch chữ của khung ở :app
feature: ads
spec: features/ads/spec.md
scenarios: []
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đổi API công khai của :core:ads (điểm chặn 7)
size: S
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-09
---

# T-099: THẺ HỘP THOẠI QUẢNG CÁO CÓ THƯỞNG CÔNG KHAI CHO SCREENSHOT TEST, CÁCH DỊCH CHỮ CỦA KHUNG Ở :APP

## Mô tả

Nguồn: issue #380 (camera-pose, PR #295 của app), bài học L-163. Feature không vẽ được thẻ hộp thoại mở khóa bằng quảng cáo có thưởng trong screenshot test vì `RewardedAdDialogCard` là `internal`; chữ hai nút của khung chỉ có tiếng Anh, và app dịch ở `:app` thì lint báo nếu không đặt chuỗi gốc đúng chỗ.

## Phạm vi

- `RewardedAdDialogCard` công khai, có KDoc nói dùng cho screenshot test và preview; test cho thấy feature dựng được thẻ.
- `docs/ops/ads-guide.md`: cách dịch chữ của khung ở `:app` (chuỗi gốc ở `values/` của `:app`, bản dịch ở `values-<ngôn ngữ>`), đã chạy thử `:app:lintStagingDebug`.
- Hướng dẫn thiết kế (`docs/ops/design-workflow.md` hay mẫu thiết kế): nút chữ của hộp thoại khung cao 48 dp (vùng chạm tối thiểu của Material), không phải 40 dp.

**Không bao gồm:**

- Đổi chữ hay hành vi của hộp thoại.

## Rủi ro sơ bộ

Đổi API công khai của `:core:ads` (điểm chặn 7): rủi ro cao, duyệt chéo. Thay đổi nhỏ, không đổi hành vi quảng cáo.

## Định nghĩa hoàn thành

- [ ] Mọi điểm của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
