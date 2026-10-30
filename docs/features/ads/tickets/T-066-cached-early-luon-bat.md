---
id: T-066
title: Khởi động sớm bằng đồng ý của lần mở trước luôn bật
feature: ads
spec: features/ads/spec.md
scenarios: [KB-11, KB-32]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-066-cached-early-luon-bat.md
auto_run: 
created: 2026-10-29
---

# T-066: KHỞI ĐỘNG SỚM BẰNG ĐỒNG Ý CỦA LẦN MỞ TRƯỚC LUÔN BẬT

## Mô tả

`CACHED_EARLY` (đồng ý thật từ lần mở trước còn dùng được thì khởi tạo SDK ngay) luôn bật như JBase, không cần cấu hình build hay `is_enable_consent_shortcut`; công tắc chỉ còn điều khiển `ERROR_ALLOWED` (ADR-0038, quyết định 3).

## Phạm vi

- Tách điều kiện của nhánh rút gọn trong `ConsentGate` thành hai: `CACHED_EARLY` luôn bật, `ERROR_ALLOWED` theo cấu hình build và công tắc từ xa như S-12.
- Test: tắt công tắc giữa phiên chỉ thu hồi `ERROR_ALLOWED` (đã có), `CACHED_EARLY` vẫn mở cổng ở lần mở sau.
- Sửa `ads-business-rules.md` S-03, S-12, mục 10 (mô tả khóa) kèm ADR-0038; cập nhật mô tả khóa trong `AdsSettings`.

**Không bao gồm:**

- Đồng ý của MAX (ADR-0037).

## Rủi ro sơ bộ

Module core, hộp thoại đồng ý và thứ tự khởi tạo SDK (điểm chặn 5, 7). Plan rủi ro cao chạm đồng ý cần người phụ trách sản phẩm biết trước khi duyệt (`risk-scoring.md`, mục 3).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
