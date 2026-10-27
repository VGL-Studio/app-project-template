---
id: T-044
title: Bổ sung test quảng cáo còn yếu theo rà soát RUN-20261026-03 và RUN-20261026-04
feature: ads
spec: features/ads/spec.md
scenarios: [KB-2, KB-5, KB-8, KB-16]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: [T-026, T-027, T-036]
sprint:
plan: features/ads/plans/T-044-bo-sung-test-quang-cao.md
created: 2026-10-26
---

# T-044: BỔ SUNG TEST QUẢNG CÁO CÒN YẾU THEO RÀ SOÁT RUN-20261026-03 VÀ RUN-20261026-04

## Mô tả

Các điểm nên sửa về test mà rà soát độc lập nêu nhưng phiên không sửa vì không có test đỏ trước. Task chỉ thêm hay làm chặt test; nếu test mới đỏ vì lỗi thật thì sửa code theo ADR-0029 và ghi vào báo cáo.

## Phạm vi

- T-026 (KB-5, KB-16): test đi qua `skipTurn(NOT_READY)` cho các trường hợp `standalone_noAd_*`; test cho nhánh `nativeTurns.busy ||` của combo inter rồi native.
- T-027 (KB-2): `full_placeholderHeight_equalsCardHeight_shortAndLongTexts` không đỏ được vì thẻ `FULL` bị ép chiều cao; thay bằng test đo đáy CTA và hàng sao nằm trong thẻ `FULL` 418 dp, cả với chữ dài và cỡ chữ hệ thống lớn.
- T-036 (KB-8): `buttons_haveTouchTargetOf48dp` đo cả 12 nút, không chỉ 5 nút cũ.
- Mỗi test mới có bước đối chứng (L-055).

**Không bao gồm:**

- Test của resume (T-043) và rewarded bấm đúp (T-042).

## Rủi ro sơ bộ

Chỉ test của module core quảng cáo và `:app`; điểm chặn ads, core vì test có thể lộ lỗi thật phải sửa code.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
