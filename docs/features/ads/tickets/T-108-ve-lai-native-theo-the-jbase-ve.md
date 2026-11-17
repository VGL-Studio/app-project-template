---
id: T-108
title: Vẽ lại native nhỏ, thu gọn và medium trên Figma theo thẻ JBase vẽ ra
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-32]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: false
created: 2026-11-17
---

# T-108: VẼ LẠI NATIVE NHỎ, THU GỌN VÀ MEDIUM TRÊN FIGMA THEO THẺ JBASE VẼ RA

## Mô tả

Component `Native/Small`, `Native/Collapsible` và `Native/Medium` trong file thiết kế của dự án mẫu vẽ lại theo bố cục và chiều cao mà JBase vẽ thẻ (spec `ads`, KB-32 tiêu chí 3, câu hỏi 66; mã JBase ở yêu cầu 2026-11-17). Native nhỏ xếp hai tầng, cao theo nội dung; lớp thu gọn có ảnh lấy phần còn lại của tổng nội suy; medium 250 dp. Màu và chữ theo token.

## Phạm vi

- Dựng theo `docs/ops/design-workflow.md`; người duyệt.
- `Native/Small`: hàng trên icon 44 dp, nhãn "Ad" 13 sp trước tiêu đề 16 sp (một dòng), mô tả 13 sp (một dòng); hàng dưới nút ngang hết bề rộng, cao 44 dp, chữ 18 sp; đệm 6 dp, hàng cách nút 6 dp; trạng thái chờ cùng chiều cao. Cùng một chiều cao ở ba bề rộng khung của ADR-0020.
- `Native/Collapsible`: tổng chiều cao theo câu hỏi 66 ở ba khung, ảnh lấy phần còn lại trên khối nội dung của `Native/Small`; nút thu gọn và AdChoices giữ như hôm nay.
- `Native/Medium`: cao 250 dp, ảnh trên, khối nội dung hai tầng và hàng sao dưới; biến thể `CTA=Large` cao thêm để ảnh đủ 120 dp.
- Cập nhật `docs/features/ads/design/` (ảnh, `context/`, README mục 1), ghi ảnh nào thay ảnh của T-064; ảnh `NetworkLostScreen_*` của feature network nếu khe đáy có trong ảnh.

**Không bao gồm:**

- Code (T-109).
- Biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ, native toàn màn hình).

## Rủi ro sơ bộ

Không chạm code. Phiên có người theo dõi (thiết kế trên Figma), không chạy trong phiên tự chạy: `auto_run: false`. Chỉ bắt đầu khi spec `ads` đã `confirmed` lại.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
