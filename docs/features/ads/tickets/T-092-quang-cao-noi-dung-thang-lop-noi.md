---
id: T-092
title: Quảng cáo trong nội dung thắng lớp nổi
feature: ads
spec: features/ads/spec.md
scenarios: [KB-35]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: [T-091]          # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-092: QUẢNG CÁO TRONG NỘI DUNG THẮNG LỚP NỔI

## Mô tả

Màn khai báo trước các vị trí quảng cáo trong nội dung của nó (native nhỏ đứng riêng, native medium, banner MREC, native trong tập hợp, thẻ native và trang native toàn trang ở bước của luồng). Khi ít nhất một vị trí đó đang bật thì màn không có lớp nổi: thanh đáy chỉ có khe đáy, banner thu gọn không nạp, không chiếm chỗ; tắt hết thì lớp nổi có lại. Spec `ads`, KB-35, tiêu chí 4 và 6; luật MC-13, MC-14.

## Phạm vi

- `:core:ads`: cách màn khai báo vị trí trong nội dung, gắn với phạm vi màn của T-091, biết trước khi vẽ lớp nổi; `BottomAdBarScaffold` và `AdSlot` (banner thu gọn) đọc khai báo cùng khóa `is_show_<mã vị trí>` để quyết có lớp nổi không. Quyết định có lớp nổi đọc một lần lúc vào màn; khóa đổi lúc màn đang mở thì áp ở lần vào màn sau (MC-13).
- Màn có quảng cáo trong nội dung khai báo vị trí theo cách mới: hôm nay chỉ màn xem thử (`:feature:sample` chỉ có thanh đáy, không cần khai báo); `docs/ops/ads-guide.md` (các bước thêm vị trí) ghi bước khai báo.
- Số liệu (MC-14): lớp nổi không có vì luật này không ghi số liệu nào.
- Test viết trước: màn khai báo native medium (hay MREC, native trong danh sách, thẻ ở bước của luồng) đang bật thì thanh đáy không có lớp nổi và banner thu gọn không gửi yêu cầu, không chiếm chỗ, không ghi số liệu; mọi vị trí trong nội dung tắt thì lớp nổi có lại ở lần vào màn sau, không đổi giữa lúc màn đang mở; màn không khai báo gì thì lớp nổi như thường.
- Màn xem thử staging: trang có quảng cáo trong nội dung và lớp nổi trên cùng trang (ví dụ "Banners" có MREC và banner thu gọn) được sắp lại; vẫn còn trang thử banner thu gọn và trang thử lớp native thu gọn.
- `scripts/ads-plan.py`: dòng khóa `is_show_native_collap` ghi ngoại lệ của MC-13.

**Không bao gồm:**

- Khe đáy nhường lớp nổi: T-090. Một quảng cáo neo đáy: T-091.

## Rủi ro sơ bộ

Đổi cách hiện quảng cáo (điểm chặn 1 của risk-scoring.md), đổi lúc ghi sự kiện phân tích của lớp nổi bị bỏ (điểm chặn 4), chạm `:core:ads` và feature có quảng cáo (điểm chặn 7): plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. App có lớp nổi cạnh quảng cáo trong nội dung sẽ mất lớp nổi ở màn đó sau khi đồng bộ từ dự án mẫu.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
