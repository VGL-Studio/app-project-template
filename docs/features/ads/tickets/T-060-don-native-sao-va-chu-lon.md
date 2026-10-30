---
id: T-060
title: Dọn native: hàng sao, chữ lớn ở native nhỏ, chú thích cũ
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-060-don-native-sao-va-chu-lon.md
auto_run: 
created: 2026-10-29
---

# T-060: DỌN NATIVE: HÀNG SAO, CHỮ LỚN Ở NATIVE NHỎ, CHÚ THÍCH CŨ

## Mô tả

Ba việc nhỏ của native: hàng sao được đăng ký với SDK như các phần khác của quảng cáo; native nhỏ 80 dp không cắt mất chữ ở cỡ chữ 200%; xóa chú thích cũ nhắc khóa đã bỏ.

## Phạm vi

- Đăng ký `starRatingView` trong `NativeAdViewBinding` (hôm nay chỉ vẽ, chưa đăng ký).
- Chụp ảnh native SMALL và CARD ở cỡ chữ 2.0 trên code hiện tại trước (L-029); thấy chữ bị cắt thì sửa cách xếp chữ trong khung 80 dp, không đổi chiều cao (D-02); không thấy thì ghi số đo và chỉ thêm ảnh chuẩn.
- Sửa KDoc ở `NativeAdSlot.kt:22` (còn nhắc `ads_native_load_timeout_seconds`, đã bỏ ở câu hỏi 20).

**Không bao gồm:**

- Khóa `time_reload_collap_ad`: cố ý không khai báo (`ads-business-rules.md`, mục 10), không phải việc của ticket này.

## Rủi ro sơ bộ

Module core, giao diện quảng cáo; có thể đổi ảnh chuẩn screenshot (ghi trong plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
