---
id: T-091
title: Giới hạn quảng cáo theo màn: một quảng cáo neo đáy, không lớp nổi khi có quảng cáo trong nội dung
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

# T-091: GIỚI HẠN QUẢNG CÁO THEO MÀN: MỘT QUẢNG CÁO NEO ĐÁY, KHÔNG LỚP NỔI KHI CÓ QUẢNG CÁO TRONG NỘI DUNG

## Mô tả

`:core:ads` biết các quảng cáo của từng màn (một đích điều hướng) và tự áp hai giới hạn của MC-12: mỗi màn tối đa một quảng cáo neo đáy (thanh đáy, banner cố định, banner thu gọn), cái đăng ký trước chạy, cái sau không nạp, không chiếm chỗ, ghi cảnh báo; màn có vị trí quảng cáo trong nội dung đang bật thì không có lớp nổi (thanh đáy chỉ có khe đáy, banner thu gọn không nạp). Spec `ads`, KB-35, tiêu chí 4 tới 6.

## Phạm vi

- `:core:ads`: một chỗ ghi các quảng cáo đang đặt trên màn (theo đích điều hướng), dùng bởi `BottomAdBarScaffold`, `AdSlot` (banner cố định, banner thu gọn, MREC), `NativeAdSlot`, native trong tập hợp và ở bước của luồng. Plan chọn cách gắn với màn (ví dụ theo `NavBackStackEntry` hay một `CompositionLocal` do vỏ app cấp) và nói rõ màn không có chỗ ghi (test, preview) thì làm gì.
- "Đang bật" đọc theo khóa `is_show_<mã vị trí>` lúc chạy: tắt hết vị trí trong nội dung thì lớp nổi được hiện lại; đổi khóa lúc màn đang mở thì áp theo plan quyết.
- Log cảnh báo thẻ `VGL.Ads` khi một quảng cáo neo đáy thứ hai bị bỏ.
- Test viết trước: hai quảng cáo neo đáy trên một màn (thanh đáy và banner thu gọn; hai banner) thì cái sau không gửi yêu cầu, không chiếm chỗ, có dòng cảnh báo; màn có native medium (hay MREC, native trong danh sách) đang bật thì thanh đáy không có lớp nổi và banner thu gọn không nạp; tắt vị trí trong nội dung thì lớp nổi có lại.
- Màn xem thử trên staging: trang nào đang đặt quảng cáo trái MC-12 (ví dụ "Collapsible ad" có danh sách xen native, "Banners" có banner cố định dưới tiêu đề, banner MREC trong nội dung và banner thu gọn ở đáy) thì plan sửa trang hay ghi rõ hành vi mới để người thử thấy được.
- `docs/ops/ads-guide.md`: luật đặt quảng cáo theo màn ở mục các bước thêm vị trí và mục kiểm trên máy thật; `scripts/ads-plan.py` nếu cần cột hay ghi chú cho giới hạn theo màn.

**Không bao gồm:**

- Khe đáy chờ lớp nổi: T-090.
- Quảng cáo toàn màn hình: đã có cổng không chồng nhau (F-01).

## Rủi ro sơ bộ

Đổi cách hiện quảng cáo (điểm chặn 1 của risk-scoring.md), chạm `:core:ads` (điểm chặn 7) và có thể chạm màn xem thử: plan rủi ro cao, người phụ trách sản phẩm biết trước khi duyệt. App đang đặt hai quảng cáo neo đáy hay lớp nổi cạnh quảng cáo trong nội dung sẽ mất bớt quảng cáo sau khi đồng bộ từ dự án mẫu.

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
