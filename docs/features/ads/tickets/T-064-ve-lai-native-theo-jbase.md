---
id: T-064
title: Vẽ lại native nhỏ, thu gọn và medium trên Figma theo kích thước JBase
feature: ads
spec: features/ads/spec.md
scenarios: [KB-32]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: false
created: 2026-10-29
---

# T-064: VẼ LẠI NATIVE NHỎ, THU GỌN VÀ MEDIUM TRÊN FIGMA THEO KÍCH THƯỚC JBASE

## Mô tả

Component native nhỏ ở đáy, native thu gọn và native medium trong file thiết kế của dự án mẫu vẽ lại theo layout của JBase (`res/layout/gnt_*.xml` của gói JBase; spec `ads`, câu hỏi 48), như T-035 đã làm theo AdsKit. Màu và chữ theo token.

## Phạm vi

- Dựng theo `docs/ops/design-workflow.md`; người duyệt.
- Khe đáy theo bề rộng màn hình (90 dp ở 320 dp, 200 dp ở 600 dp): vẽ ở ba bề rộng khung chuẩn của ADR-0020.
- Cập nhật `docs/features/ads/design/` (ảnh và `context/`), ghi ảnh nào thay ảnh của T-035.

**Không bao gồm:**

- Code (T-065).

## Rủi ro sơ bộ

Không chạm code. Phiên có người theo dõi (thiết kế trên Figma), không chạy trong phiên tự chạy: `auto_run: false`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
