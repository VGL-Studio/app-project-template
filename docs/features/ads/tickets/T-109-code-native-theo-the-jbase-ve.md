---
id: T-109
title: Code kích thước và bố cục native theo thẻ JBase vẽ ra
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-17, KB-32]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-108]
sprint:
plan:
auto_run:
created: 2026-11-17
---

# T-109: CODE KÍCH THƯỚC VÀ BỐ CỤC NATIVE THEO THẺ JBASE VẼ RA

## Mô tả

Đổi native nhỏ ở đáy, lớp thu gọn và native medium trong `:core:ads` theo thiết kế T-108 (spec `ads`, KB-32 tiêu chí 3, câu hỏi 66). Sau task, khe đáy thấp hơn và cùng chiều cao ở mọi bề rộng (khoảng 106 dp ở cỡ chữ mặc định), lớp thu gọn có ảnh lớn hơn, medium thấp hơn rõ.

## Phạm vi

- Bố cục hai tầng của native nhỏ (trong khe đáy, đứng riêng và trong lớp thu gọn); chiều cao theo nội dung thay `NativeAdDimens.bottomSlotHeight`; một chiều cao cho vẽ, khung chờ và chừa chỗ (D-04, câu hỏi 49). Chiều cao chừa lúc chưa có quảng cáo phải biết trước khi tải, kể cả ở cỡ chữ lớn: plan chọn cách tính (đo khối nội dung hay tính từ cỡ chữ).
- Lớp thu gọn: tổng theo câu hỏi 66, ảnh bằng tổng trừ chiều cao khối nội dung; giữ cạnh ngắn, trần nửa cửa sổ, ảnh không dưới 120 dp (câu hỏi 65).
- Medium 250 dp, cao thêm khi ảnh dưới 120 dp; xem lại khung thấp của medium (`MediumCompactBelow`) có còn cần không.
- Viết lại câu khẳng định cũ về chiều cao (126 dp, 320 tới 400 dp) theo ADR-0031; ghi lại ảnh chuẩn bị đổi (liệt kê trong plan), kể cả ảnh của feature network và `:app` có khe đáy.
- Giới hạn bề rộng 120 dp của nút ở native nhỏ không còn hợp với nút ngang hết bề rộng: plan quyết bỏ và viết lại các test liên quan theo ADR-0031, vẫn giữ kiểm tiêu đề và nhãn "Ad" hiện đủ ở cỡ chữ lớn (KB-1).
- `ads-business-rules.md` mục 5.7, D-02, D-04 và `ads-guide.md`: đổi kích thước theo câu hỏi 66.

**Không bao gồm:**

- Thiết kế (T-108).
- Biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ, native toàn màn hình).

## Rủi ro sơ bộ

Module core, giao diện quảng cáo, nhiều ảnh chuẩn screenshot. Plan chỉ viết khi spec `ads` đã `confirmed` lại và T-108 đã được duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
