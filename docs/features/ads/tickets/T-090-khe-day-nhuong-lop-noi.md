---
id: T-090
title: Khe đáy nhường lớp native thu gọn
feature: ads
spec: features/ads/spec.md
scenarios: [KB-35]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: [T-061, T-087]          # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-090: KHE ĐÁY NHƯỜNG LỚP NATIVE THU GỌN

## Mô tả

Thanh đáy không còn hai quảng cáo chồng nhau. Vào màn thì khe đáy native chạy ngay; mỗi lần lớp native thu gọn mở có quảng cáo (lần đầu, mở muộn, hay bung lại), khe đáy hủy quảng cáo đang có và không vẽ gì, kể cả khung chờ, trong khi nội dung vẫn chừa chỗ; người dùng thu gọn thì khe đáy hiện khung chờ và nạp quảng cáo mới. Spec `ads`, KB-35, tiêu chí 1 tới 3 và 6; luật D-07, MC-14 của `docs/ops/ads-business-rules.md`.

## Phạm vi

- `:core:ads`: `BottomAdBar.kt` và `CollapsibleNativeAd.kt` (trạng thái mở, thu gọn, bung lại; lớp nổi chưa có quảng cáo, nạp lỗi hay không có mã đơn vị thì không tính là mở) báo cho khe đáy lúc nào được chạy. Khe đáy hủy quảng cáo khi lớp nổi mở (yêu cầu đang bay theo O-03), lấy quảng cáo mới khi thu gọn (từ kho nạp sẵn nếu có, KB-33). Bỏ trễ 500 ms của lần làm mới đầu của khe đáy (`bottomSlotFirstRefreshDelayMillis`), vì hai lớp không còn chạy cùng lúc.
- Khoảng chừa của nội dung giữ như lúc khe đang tải (D-04) trong lúc lớp nổi mở; luật chừa lúc đang tải do T-061 làm, nên ticket này làm sau T-061. Nhịp làm mới đầu do T-087 sửa, nên làm sau T-087.
- Số liệu (MC-14): lượt khe đáy bị hủy hay không chạy vì lớp nổi mở không ghi `ad_opportunity`, `af_native_ad_eligible` hay số liệu nào khác. Plan liệt kê các chỗ đang ghi số liệu của khe đáy và cách giữ chúng im lặng.
- Test viết trước: vào màn thì khe đáy gửi yêu cầu ngay; lớp nổi mở (lần đầu, mở muộn khi khe đáy đã có quảng cáo, bung lại) thì quảng cáo của khe đáy bị hủy, khe không vẽ khung chờ, không gửi yêu cầu, không làm mới; khoảng chừa không đổi; thu gọn thì khe hiện khung chờ và gửi yêu cầu; lớp nổi nạp lỗi hay không có mã thì khe đáy chạy như thường; lượt bị chặn không ghi số liệu. Test cũ khẳng định khe đáy chạy dưới lớp nổi hay trễ 500 ms (nếu có) viết lại theo ADR-0031.
- Màn xem thử staging, trang "Collapsible ad": vẫn thử được lớp native thu gọn và khe đáy; ảnh chuẩn screenshot đổi theo plan nếu cần.
- `docs/ops/ads-guide.md`: phần thanh đáy và phần thử trên staging. `scripts/ads-plan.py`: các mô tả lớp nổi trên khe đáy (dòng thanh đáy và khóa `is_show_native_collap`).

**Không bao gồm:**

- Giới hạn theo màn: T-091 (một quảng cáo neo đáy), T-092 (quảng cáo trong nội dung thắng lớp nổi).
- Khe đáy là banner: không có lớp nổi (D-01), không đổi.

## Rủi ro sơ bộ

Đổi cách hiện quảng cáo (điểm chặn 1 của risk-scoring.md), đổi lúc ghi sự kiện phân tích của khe đáy (điểm chặn 4), chạm `:core:ads` (điểm chặn 7): plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. Ít lượt hiển thị hơn ở khe đáy.

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
