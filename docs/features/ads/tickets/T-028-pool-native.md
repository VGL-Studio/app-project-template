---
id: T-028
title: Pool native: ô lưới, trang xem trước, medium
feature: ads
spec: features/ads/spec.md
scenarios: [KB-3, KB-21]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-011, T-027]
sprint:
plan:
created: 2026-10-23
---

# T-028: POOL NATIVE: Ô LƯỚI, TRANG XEM TRƯỚC, MEDIUM

## Mô tả

Pool native theo P-01 tới P-07 của `docs/ops/ads-business-rules.md`: nạp tuần tự từng quảng cáo, vòng tròn, không trả quảng cáo đang dùng, cạn thì ẩn ô nhưng vẫn thử lấy, ô trong pager chỉ tính khi trang đứng yên. Native trong danh sách (T-011) và native medium lấy quảng cáo từ pool.

## Phạm vi

- Pool là lớp thuần có test vòng tròn, cạn, hết hạn.
- Ô lưới thẻ dọc và trang xem trước; khóa chèn mỗi N mục theo câu hỏi 19 của spec.

**Không bao gồm:**

- Native giới thiệu (T-032).

## Rủi ro sơ bộ

Module core, quảng cáo. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
