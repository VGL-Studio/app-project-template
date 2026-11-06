---
id: T-091
title: Mỗi màn tối đa một quảng cáo neo đáy
feature: ads
spec: features/ads/spec.md
scenarios: [KB-35]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: [T-090]          # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-091: MỖI MÀN TỐI ĐA MỘT QUẢNG CÁO NEO ĐÁY

## Mô tả

`:core:ads` biết các quảng cáo neo đáy (thanh đáy, banner cố định, banner thu gọn) của từng màn và chỉ cho một cái chạy: cái vào giao diện trước trong lần vào màn chạy; cái sau không nạp, không chiếm chỗ, ghi cảnh báo thẻ `VGL.Ads`; cái đang chạy rời giao diện thì cái đang chờ kế tiếp được chạy; chỉ cái được phép chạy mới giữ lượt. Spec `ads`, KB-35, tiêu chí 5 và 6; luật MC-12, MC-14.

## Phạm vi

- `:core:ads`: một phạm vi màn (một đích điều hướng; tab và trang của pager trong đích tính chung) ghi các quảng cáo neo đáy đang đặt, theo thứ tự vào giao diện, dùng bởi `BottomAdBarScaffold` và `AdSlot` (banner cố định, banner thu gọn). Plan chọn cách gắn với màn (ví dụ theo `NavBackStackEntry`, hay một `CompositionLocal` do vỏ app cấp) và nói rõ chỗ không có phạm vi (test, preview) thì làm gì. Màn xem thử là một Activity riêng, các trang là trạng thái của `AdGalleryUiState`, không có `NavHost`: plan cấp phạm vi cho từng trang. Phạm vi này dùng lại ở T-092.
- `:app` (`AppNavHost`, `MainActivity`) nếu plan chọn cách cần vỏ app cấp phạm vi; khi đó plan có test dựng lại activity (`docs/CLAUDE.md`, Khi viết plan).
- Số liệu (MC-14): quảng cáo neo đáy bị bỏ không ghi số liệu nào.
- Test viết trước: hai quảng cáo neo đáy trên một màn (thanh đáy và banner thu gọn; banner cố định và banner thu gọn) thì cái sau không gửi yêu cầu, không chiếm chỗ, có dòng cảnh báo, không ghi số liệu; cái đầu rời giao diện thì cái sau chạy; cái vào trước có vị trí đang tắt hay không có mã thì cái sau chạy; hai màn khác nhau không ảnh hưởng nhau.
- Màn xem thử staging: mỗi trang là một màn. Trang "Banners" đang có banner cố định và banner thu gọn trên một trang: plan sắp lại để vẫn thử được từng loại.
- `docs/ops/ads-guide.md`: luật đặt quảng cáo neo đáy ở mục các bước thêm vị trí và mục kiểm trên máy thật.

**Không bao gồm:**

- Quảng cáo trong nội dung thắng lớp nổi: T-092.
- Khe đáy nhường lớp nổi: T-090.

## Rủi ro sơ bộ

Đổi cách hiện quảng cáo (điểm chặn 1 của risk-scoring.md), đổi lúc ghi sự kiện phân tích của quảng cáo bị bỏ (điểm chặn 4), chạm `:core:ads` và có thể `:app` (điểm chặn 7): plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. App đang đặt hai quảng cáo neo đáy trên một màn sẽ mất một cái sau khi đồng bộ từ dự án mẫu.

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
