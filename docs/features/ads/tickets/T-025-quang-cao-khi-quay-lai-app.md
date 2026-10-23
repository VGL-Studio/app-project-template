---
id: T-025
title: Quảng cáo khi quay lại app, bỏ lượt sau khi bấm quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-14]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-024, T-026]
sprint:
plan:
created: 2026-10-23
---

# T-025: QUẢNG CÁO KHI QUAY LẠI APP, BỎ LƯỢT SAU KHI BẤM QUẢNG CÁO

## Mô tả

`:core:ads` theo dõi vòng đời process, hiện app open hoặc inter khi app lên foreground theo thứ tự cổng A-10 tới A-15 của `docs/ops/ads-business-rules.md`; bấm bất kỳ quảng cáo nào thì bỏ lượt resume kế tiếp.

## Phạm vi

- Thứ tự cổng resume: hàm thuần có test cho từng cổng.
- Resume là inter thì nối combo khi `is_show_native_after_inter` bật (sau T-026, dùng route của vỏ app).
- Ghi cơ hội vị trí `resume` theo mục 8.2.

**Không bao gồm:**

- Combo (T-026).

## Rủi ro sơ bộ

Module core, quảng cáo: điểm chặn; resume không có khoảng chờ là rủi ro chính sách (ADR-0026). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
