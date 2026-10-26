---
id: T-043
title: Quảng cáo khi quay lại app: kiểm vị trí tắt trước cổng chặn (L-A05)
feature: ads
spec: features/ads/spec.md
scenarios: [KB-14]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-025]
sprint:
plan: features/ads/plans/T-043-resume-kiem-vi-tri-truoc-khi-chan.md
created: 2026-10-26
---

# T-043: QUẢNG CÁO KHI QUAY LẠI APP: KIỂM VỊ TRÍ TẮT TRƯỚC CỔNG CHẶN (L-A05)

## Mô tả

Rà soát độc lập của T-025 (RUN-20261026-04): ngay trước khi hiện, `ResumeAdTurns` kiểm `suppressedNow()` trước `placementNow()`, nên khi khóa resume vừa tắt giữa lượt mà app đang bị chặn (form UMP vừa mở) thì vẫn ghi một cơ hội `suppressed`. Theo L-A05 (`docs/ops/ads-business-rules.md`, mục 12), vị trí tắt không phải cơ hội. Sau task, vị trí tắt hay đổi loại giữa lượt không ghi cơ hội nào, kể cả khi đang bị chặn; số liệu `suppressed` chỉ đếm lượt của vị trí đang bật. Anh Nhật chọn đổi thứ tự 2026-10-26.

## Phạm vi

- Đổi thứ tự: `placementNow() != pending` trước `suppressedNow()` ở bước hiện của `ResumeAdTurns`.
- Test viết trước: tắt khóa giữa lượt khi đang bị chặn thì không ghi gì; đang bật mà bị chặn vẫn ghi `suppressed` đúng một lần.
- Sửa test `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` cho tới nhánh `OFF` thật (điểm nên sửa của rà soát T-025).

**Không bao gồm:**

- Thay đổi khác của resume (T-025).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn ads, core); chỉ đổi số liệu cơ hội, không đổi lúc hiện quảng cáo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
