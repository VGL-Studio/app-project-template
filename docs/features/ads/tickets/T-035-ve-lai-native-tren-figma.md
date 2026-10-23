---
id: T-035
title: Vẽ lại component native trên Figma theo kích thước AdsKit
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-16, KB-17, KB-20]
owner: Nhật
status: backlog
risk: low
size: M
depends_on: []
sprint:
plan:
created: 2026-10-23
---

# T-035: VẼ LẠI COMPONENT NATIVE TRÊN FIGMA THEO KÍCH THƯỚC ADSKIT

## Mô tả

Component native trong file thiết kế của dự án mẫu vẽ lại theo kích thước của AdsKit (`docs/ops/ads-business-rules.md`, mục 5.7): khe đáy 56 dp, thu gọn 250 dp với nút 32 dp, giới thiệu 250 dp, toàn màn hình, native sau inter có nút đóng 36 dp, ô lưới thẻ dọc, medium. Màu và chữ theo token. Cập nhật `docs/features/ads/design/` (ảnh và `context/`).

## Phạm vi

- Dựng theo `docs/ops/design-workflow.md`; người duyệt.
- Ghi rõ ảnh thiết kế nào thay ảnh của đợt 2 để các ticket code ghi lại ảnh chuẩn.

**Không bao gồm:**

- Code (T-027, T-030, T-032).

## Rủi ro sơ bộ

Không chạm code. Phiên có người theo dõi (thiết kế trên Figma), không chạy trong phiên tự chạy. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
