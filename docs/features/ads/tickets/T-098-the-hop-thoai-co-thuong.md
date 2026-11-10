---
id: T-098
title: Thẻ hộp thoại quảng cáo có thưởng công khai cho screenshot test, cách dịch chữ của khung ở :app
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: on-staging             # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đổi API công khai của :core:ads (điểm chặn 7)
size: S
depends_on: []               # anh Nhật bỏ phụ thuộc T-097 ngày 2026-11-10 (RUN-20261110-01): chỉ cùng sửa ads-guide.md ở đoạn khác
sprint:
plan: features/ads/plans/T-098-the-hop-thoai-co-thuong.md  # đường dẫn plan khi đã có
auto_run:
created: 2026-11-09
---

# T-098: THẺ HỘP THOẠI QUẢNG CÁO CÓ THƯỞNG CÔNG KHAI CHO SCREENSHOT TEST, CÁCH DỊCH CHỮ CỦA KHUNG Ở :APP

## Mô tả

Nguồn: issue #380 (camera-pose, PR #295 của app), bài học L-162. Feature không vẽ được thẻ hộp thoại mở khóa bằng quảng cáo có thưởng trong screenshot test vì `RewardedAdDialogCard` là `internal`; chữ hai nút của khung chỉ có tiếng Anh, và app dịch ở `:app` thì lint báo nếu không đặt chuỗi gốc đúng chỗ.

## Phạm vi

- `RewardedAdDialogCard` công khai, có KDoc nói dùng cho screenshot test và preview; test cho thấy feature dựng được thẻ.
- `docs/ops/ads-guide.md`: cách dịch chữ của khung ở `:app` (chuỗi gốc ở `values/` của `:app`, bản dịch ở `values-<ngôn ngữ>`), đã chạy thử `:app:lintStagingDebug`.
- `docs/ops/design-workflow.md`: nút chữ của hộp thoại khung cao 48 dp (vùng chạm tối thiểu của Material), không phải 40 dp.
- `docs/ops/ads-guide.md` hiện viết app dịch chữ của khung trong `values-<ngôn ngữ>` của mình (mục 4, ý Rewarded), đúng cách làm lint báo lỗi: sửa câu đó. Đối chiếu với T-082 (chữ mặc định tiếng Anh, tiếng Việt ở `values-vi/`): T-082 làm trước thì cách dịch theo kết quả của nó.

**Không bao gồm:**

- Đổi chữ hay hành vi của hộp thoại.

## Rủi ro sơ bộ

Đổi API công khai của `:core:ads` (điểm chặn 7): rủi ro cao, duyệt chéo. Thay đổi nhỏ, không đổi hành vi quảng cáo.

## Định nghĩa hoàn thành

- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
