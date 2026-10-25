---
id: T-028
title: Pool native và native trong tập hợp (lưới, danh sách, pager), medium
feature: ads
spec: features/ads/spec.md
scenarios: [KB-3, KB-21]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-011, T-027, T-040]
sprint:
plan: features/ads/plans/T-028-pool-native.md
created: 2026-10-23
---

# T-028: POOL NATIVE VÀ NATIVE TRONG TẬP HỢP (LƯỚI, DANH SÁCH, PAGER), MEDIUM

## Mô tả

Pool native theo P-01 tới P-07 của `docs/ops/ads-business-rules.md`: nạp tuần tự từng quảng cáo, vòng tròn, không trả quảng cáo đang dùng, cạn thì ẩn ô nhưng vẫn thử lấy, ô trong pager chỉ tính khi trang đứng yên. Native trong danh sách (T-011) và native medium lấy quảng cáo từ pool.

## Phạm vi

- Pool là lớp thuần có test vòng tròn, cạn, hết hạn.
- Ô lưới thẻ dọc và trang xem trước; khóa chèn mỗi N mục theo câu hỏi 19 của spec.
- Native trong tập hợp theo mô hình chung (MC-07): định dạng `native_in_collection` và `native_medium`, vị trí theo màn; thuộc tính giữ chỗ khi pool cạn (mặc định ẩn, câu hỏi 33), cờ "đang được xem" trong pager, `cta_large` cho medium; chèn mỗi N mục là `native_every_<mã vị trí>` (câu hỏi 19, 28). Không có composable mang tên màn ("gallery", "preview").
- Quảng cáo đã ghi lượt hiển thị thì hủy khi ô trả về, chưa ghi thì trả lại vòng xoay (P-01a, câu hỏi 23); hủy toàn pool đưa pool về "không đang nạp" (P-08 theo A 05:234).
- Test nghiệm thu gợi ý: `AT-PL-*`.

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
