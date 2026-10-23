---
id: T-010
title: Native small và medium; màn hình mẫu dùng native small thay banner
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: approved
risk: high
size: L
depends_on: [T-007, T-009]
sprint:
plan: features/ads/plans/T-010-native-small-va-medium.md
created: 2026-10-21
---

# T-010: NATIVE SMALL VÀ MEDIUM; MÀN HÌNH MẪU DÙNG NATIVE SMALL THAY BANNER

## Mô tả

`:core:ads` có composable cho native small và native medium, dựng bằng Compose bọc `NativeAdView`, theo component đã duyệt trong `docs/features/ads/design/` và quy chuẩn ở `docs/ops/ads-guide.md`, mục 4: giữ chỗ khi tải, thu chỗ khi không có quảng cáo sau `ads_native_load_timeout_seconds`, chế độ tối, hủy khi rời màn hình. `:feature:sample` đổi vị trí `sample_bottom_banner` thành native small và bỏ banner (spec, câu hỏi 3). Định dạng banner vẫn giữ trong `:core:ads` cho app cần.

## Phạm vi

- `AdFormat` thêm NATIVE_SMALL, NATIVE_MEDIUM; mã thử native của Google.
- Composable native và tải quảng cáo native trong `AdsController`.
- `:feature:sample` dùng native small; ảnh chuẩn screenshot theo thiết kế.
- Cập nhật `CLAUDE.md` (bước 7) và `ads-guide.md`.

**Không bao gồm:**

- Native trong danh sách (T-011), thu gọn (T-012), toàn màn hình (T-013).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn). Thiết kế đã duyệt ngày 2026-10-21 (`docs/features/ads/design/`). Cần ghi lại ảnh chuẩn screenshot của màn hình mẫu theo plan.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
