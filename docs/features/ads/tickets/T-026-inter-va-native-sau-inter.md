---
id: T-026
title: Interstitial theo PARTNER và combo native toàn màn hình sau inter
feature: ads
spec: features/ads/spec.md
scenarios: [KB-5, KB-16]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-022, T-023, T-027]
sprint:
plan:
created: 2026-10-23
---

# T-026: INTERSTITIAL THEO PARTNER VÀ COMBO NATIVE TOÀN MÀN HÌNH SAU INTER

## Mô tả

Interstitial đi qua cổng chung (N-01 tới N-05) và, khi `is_show_native_after_inter` bật, nối màn native toàn màn hình có đếm ngược 3 giây và nút đóng 36 dp ở góc trên phải (K-01 tới K-08 của `docs/ops/ads-business-rules.md`). Thay hành vi "đóng được ngay" của T-013.

## Phạm vi

- Màn native sau inter: đếm ngược theo thời điểm tuyệt đối, chờ inter rời màn tối đa 2 giây, nút quay lại theo câu hỏi 17 của spec.
- Đóng cụm thì đẩy mốc `ads_interval`; màn chặn quảng cáo tự hiện.
- Đếm lần inter hiện từ lúc cài (lưu trên máy) cho T-033.
- Screenshot test với đồng hồ dừng (CLAUDE.md, bước 10; L-048).

**Không bao gồm:**

- Resume nối combo (T-025).

## Rủi ro sơ bộ

Module core, quảng cáo toàn màn hình: điểm chặn; đếm ngược và hai quảng cáo liền nhau là rủi ro chính sách (ADR-0026). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
