---
id: T-090
title: Khe đáy chờ khi lớp native thu gọn đang mở
feature: ads
spec: features/ads/spec.md
scenarios: [KB-35]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: M                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-NNN]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run:                    # để trống; ghi false khi ticket do người làm (ví dụ chạm scripts/release.sh): plan-queue.py không soạn plan, run-check.py từ chối đưa vào danh sách chạy
created: 2026-11-06
---

# T-090: KHE ĐÁY CHỜ KHI LỚP NATIVE THU GỌN ĐANG MỞ

## Mô tả

Thanh đáy không còn hai quảng cáo chồng nhau: khi lớp native thu gọn đang mở hay đang chờ quảng cáo đầu tiên, khe đáy native không vẽ, không nạp, không làm mới; người dùng thu gọn lớp nổi thì khe đáy mới nạp và hiện. Lớp nổi mở lại thì khe đáy hủy quảng cáo đang có (spec `ads`, KB-35, tiêu chí 1 tới 3; luật D-07 của `docs/ops/ads-business-rules.md`).

## Phạm vi

- `:core:ads`: `BottomAdBar.kt` (thanh đáy, nhịp làm mới đầu của khe đáy đang tính theo lớp nổi) và `CollapsibleNativeAd.kt` (trạng thái mở, thu gọn, bung lại) báo cho khe đáy lúc nào được chạy; khe đáy native lấy quảng cáo (từ kho nạp sẵn nếu có, KB-33) khi lớp nổi thu gọn hay không có quảng cáo, và hủy quảng cáo khi lớp nổi mở lại.
- Khoảng chừa của nội dung giữ như lúc khe đang tải (D-04) trong lúc khe đáy chờ.
- Test viết trước: khe đáy không gửi yêu cầu khi lớp nổi mở hay đang chờ quảng cáo đầu; thu gọn thì gửi; lớp nổi hết thời gian chờ hay nạp lỗi thì khe đáy chạy; bung lại thì quảng cáo của khe đáy bị hủy; khoảng chừa không đổi. Test cũ khẳng định khe đáy chạy dưới lớp nổi (nếu có) viết lại theo ADR-0031.
- Plan xem lại số liệu: lượt hủy quảng cáo khe đáy khi lớp nổi mở lại có ghi gì không (cơ hội, `ad_show_fail`), theo mục 8 của `ads-business-rules.md`.
- `docs/ops/ads-guide.md`: phần thanh đáy, phần thử trên staging của trang "Collapsible ad"; `scripts/ads-plan.py` nếu mô tả thanh đáy trong file Excel nói hai lớp cùng chạy.

**Không bao gồm:**

- Giới hạn theo màn (một quảng cáo neo đáy, quảng cáo trong nội dung thắng lớp nổi): T-091.
- Khe đáy là banner: không có lớp nổi (D-01), không đổi.

## Rủi ro sơ bộ

Đổi tần suất và cách hiện quảng cáo (điểm chặn 1 của risk-scoring.md), chạm `:core:ads` (điểm chặn 7): plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. Ít lượt hiển thị hơn ở khe đáy; có thể đổi ảnh chuẩn screenshot của trang xem thử thanh đáy.

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
