---
id: T-109
title: Code native nhỏ và lớp thu gọn theo thẻ JBase vẽ ra
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-3, KB-4, KB-17, KB-32]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: [T-108]
sprint:
plan: features/ads/plans/T-109-native-nho-theo-the-jbase.md
auto_run:
created: 2026-11-17
---

# T-109: CODE NATIVE NHỎ VÀ LỚP THU GỌN THEO THẺ JBASE VẼ RA

## Mô tả

Đổi mọi native nhỏ (khe đáy, đứng riêng, hàng trong danh sách) và lớp thu gọn trong `:core:ads` theo thiết kế T-108 (spec `ads`, KB-32 tiêu chí 3, câu hỏi 66). Sau task, khe đáy thấp hơn và cùng chiều cao ở mọi bề rộng (khoảng 106 dp ở cỡ chữ mặc định), lớp thu gọn có ảnh lớn hơn. Medium tách sang T-110 khi viết plan (2026-11-17).

## Phạm vi

- Bố cục hai tầng của native nhỏ (trong khe đáy, đứng riêng, hàng trong danh sách và trong lớp thu gọn); chiều cao theo nội dung thay `NativeAdDimens.bottomSlotHeight`; một chiều cao cho vẽ, khung chờ và chừa chỗ (D-04, câu hỏi 49). Chiều cao chừa lúc chưa có quảng cáo phải biết trước khi tải, kể cả ở cỡ chữ lớn: plan chọn cách tính (đo khối nội dung hay tính từ cỡ chữ).
- Lớp thu gọn: tổng theo câu hỏi 66, ảnh bằng tổng trừ chiều cao khối nội dung; giữ cạnh ngắn, trần nửa cửa sổ, ảnh không dưới 120 dp (câu hỏi 65); chữ lớn làm phần còn lại cho ảnh dưới 120 dp thì lớp cao thêm; chạm trần thì vùng ảnh co trước, không dưới 120 dp, vẫn vượt trần thì không mở lớp.
- Viết lại câu khẳng định cũ về chiều cao của native nhỏ và lớp thu gọn (126 dp, 142 dp, 173 dp) theo ADR-0031; ghi lại ảnh chuẩn bị đổi (liệt kê trong plan), kể cả `:feature:sample` (`SampleScreen_*`, `SampleScreenTest` dùng `nativeReservation()`, `nativeSlotHeight()`, `collapsibleLayerHeight()`), feature network và `:app` (`AdGalleryScreenTest`, `PreviewAdGallerySlots`). `BottomAdBarDefaults.nativeReservation()` nay phụ thuộc cỡ chữ: plan nói cách feature đọc nó.
- Giới hạn bề rộng 120 dp của nút ở native nhỏ không còn hợp với nút ngang hết bề rộng: plan quyết bỏ và viết lại các test liên quan theo ADR-0031, vẫn giữ kiểm tiêu đề và nhãn "Ad" hiện đủ ở cỡ chữ lớn (KB-1).
- `ads-business-rules.md` mục 5.7, D-02, D-04, D-07, CL-05 và `ads-guide.md`: đổi kích thước theo câu hỏi 66.

**Không bao gồm:**

- Thiết kế (T-108).
- Native medium (T-110).
- Biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ, native toàn màn hình).

## Rủi ro sơ bộ

Module core, giao diện quảng cáo, nhiều ảnh chuẩn screenshot. Plan chỉ viết khi spec `ads` đã `confirmed` lại và T-108 đã được duyệt. Ban đầu cỡ L; khi viết plan đã tách medium sang T-110 để vừa một lượt của phiên tự chạy, còn cỡ M.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
