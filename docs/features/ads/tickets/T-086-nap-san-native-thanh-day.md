---
id: T-086
title: Nạp sẵn native nhỏ ở đáy và native thu gọn
feature: ads
spec: features/ads/spec.md
scenarios: [KB-33]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: 
created: 2026-11-02
---

# T-086: NẠP SẴN NATIVE NHỎ Ở ĐÁY VÀ NATIVE THU GỌN

## Mô tả

Native nhỏ ở khe đáy và lớp native thu gọn vào kế hoạch nạp sẵn như interstitial, app open và các pool native. Người dùng vào màn đầu tiên có thanh đáy thấy quảng cáo ngay, không phải chờ một yêu cầu gửi lúc màn vào (KB-33, câu hỏi 53).

## Phạm vi

- Thêm một `PreloadSlot` cho mỗi khe một quảng cáo (`NATIVE_SMALL`, `NATIVE_COLLAPSIBLE`) bên cạnh `FullScreenPreloadSlot` và `NativePoolPreloadSlot` (`AdsController.kt`, `PreloadPlanRunner.kt`); khe chỉ nạp khi vị trí thanh đáy đang bật, khe đáy là native (`is_show_native_bottom`), lớp thu gọn bật (`is_show_native_collap`), cổng đồng ý mở và không có cổng không hiện quảng cáo (KB-30).
- Kế hoạch mặc định (`defaultPreloadPlan`) có hai khe này ở nhóm sau nhóm đầu; `nativeOnly()` (nạp bù khi Activity quay lại, S-25) có chúng.
- Thanh đáy (`BottomAdBar.kt`, `CollapsibleNativeAd.kt`, `NativeAdSource.kt`) lấy quảng cáo đã nạp sẵn khi màn vào; lấy ra thì khe nạp cái tiếp theo (O-01); quảng cáo quá 55 phút thì bỏ (O-04). Vị trí của số liệu theo PL-02 và mục 8 (Ghi số liệu) của `ads-business-rules.md`.
- Sửa `docs/ops/ads-business-rules.md` (S-22, HD-02, mục 5.7, CL-01) và `docs/ops/ads-guide.md` cho khớp.

**Không bao gồm:**

- Nạp sẵn banner cố định, banner thu gọn, MREC (câu hỏi 53: không làm).
- Nhịp làm mới chỉnh từ xa (T-087).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn 1 và 7 của `risk-scoring.md`): đổi kế hoạch nạp sẵn và đường lấy quảng cáo của thanh đáy, dễ sinh hai chỗ cùng giữ một quảng cáo hay quảng cáo nạp mà không hiện. Không thêm SDK, quyền, sự kiện.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
