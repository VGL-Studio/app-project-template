---
id: T-086
title: Kho nạp sẵn cho native nhỏ và native thu gọn
feature: ads
spec: features/ads/spec.md
scenarios: [KB-33]
owner: Nhật
status: approved
risk: high
size: L
depends_on: []
sprint:
plan: features/ads/plans/T-086-nap-san-native-thanh-day.md
auto_run: 
created: 2026-11-02
---

# T-086: KHO NẠP SẴN CHO NATIVE NHỎ VÀ NATIVE THU GỌN

## Mô tả

Native nhỏ một quảng cáo (thanh đáy và khe đứng riêng) và lớp native thu gọn lấy quảng cáo từ một kho nạp sẵn theo mã đơn vị, giữ tối đa một quảng cáo, sống qua các màn. Người dùng vào màn nào có các khe này cũng thấy quảng cáo ngay, không chờ yêu cầu gửi lúc màn vào (KB-33, câu hỏi 53, 55).

## Phạm vi

- Kho theo `(định dạng, mã đơn vị)` cho `NATIVE_SMALL` và `NATIVE_COLLAPSIBLE`, như khóa của pool (`NativePoolKey`), một `PreloadSlot` cho mỗi kho bên cạnh `FullScreenPreloadSlot` và `NativePoolPreloadSlot` (`AdsController.kt`, `NativeAdSources.kt`, `NativeAdSource.kt`). Kho chỉ nạp khi còn vị trí dùng nó đang bật (vị trí thanh đáy chỉ dùng kho `native_small` khi `is_show_native_bottom` bật), cổng đồng ý mở, không có cổng không hiện quảng cáo (KB-30); kho `native_collap` thêm điều kiện `is_show_native_bottom` và `is_show_native_collap`.
- Kế hoạch mặc định: `NATIVE_SMALL` đã có trong kế hoạch nhưng chưa có khe; `NATIVE_COLLAPSIBLE` phải suy ra từ các vị trí thanh đáy (`defaultPreloadPlan`, `AdPreloadPlan.kt`). Cả hai ở nhóm sau nhóm đầu.
- Khe một quảng cáo lấy từ kho trước khi gửi yêu cầu riêng; lấy ra thì kho nạp cái kế (O-01); quảng cáo trong kho không bị hủy khi rời màn, yêu cầu của kho đang bay lúc rời màn vẫn giữ kết quả (ngoại lệ của O-01 và O-03), quá 55 phút thì bỏ (O-04). Vòng làm mới (`NativeAdRefresh.kt`) và quảng cáo chờ của lớp thu gọn giữ cách hiện tại hay lấy từ kho: plan quyết, ghi lý do.
- Số liệu: lượt nạp của kho ghi `preload`, doanh thu ghi vị trí của màn lấy (PL-02, PL-03).
- Vị trí chỉ có ở staging (`gallery_*`): dùng chung kho theo mã đơn vị nên không thêm yêu cầu; plan kiểm lại.
- Sửa `docs/ops/ads-business-rules.md` (S-22, cả câu "mặc định một nhóm" đã lệch code; HD-02; O-01, O-03; mục 5.7; CL-01) và `docs/ops/ads-guide.md` (ghi: native nhỏ ở production lấy mã theo vị trí, app muốn mọi màn dùng chung một kho thì đặt cùng một mã; `docs/ops/to-app.md` nếu cần).

**Không bao gồm:**

- Nạp sẵn banner cố định, banner thu gọn, MREC (câu hỏi 53: không làm).
- Nhịp làm mới chỉnh từ xa (T-087).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn 1 và 7 của `risk-scoring.md`): đổi vòng đời nguồn native và kế hoạch nạp sẵn, dễ sinh hai chỗ cùng giữ một quảng cáo, quảng cáo nạp mà không hiện, hay rò quảng cáo khi kho không được hủy. Đổi luật đã chốt O-01, O-03. Không thêm SDK, quyền, sự kiện. Cỡ L: nếu plan thấy không xong trong một lượt thì tách (kho native nhỏ trước, lớp thu gọn sau).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
