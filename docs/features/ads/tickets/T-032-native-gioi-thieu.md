---
id: T-032
title: Native ở một bước của luồng (giới thiệu và luồng khác)
feature: ads
spec: features/ads/spec.md
scenarios: [KB-20]
owner: Nhật
status: approved
risk: medium
size: S
depends_on: [T-027, T-028]
sprint:
plan: features/ads/plans/T-032-native-gioi-thieu.md
created: 2026-10-23
---

# T-032: NATIVE Ở MỘT BƯỚC CỦA LUỒNG (GIỚI THIỆU VÀ LUỒNG KHÁC)

## Mô tả

Native giới thiệu cao 250 dp từ pool 3 quảng cáo (đặt ngoài pager) và native toàn trang không có nút điều khiển, bật bằng `is_show_native_onboarding` (OB-01, OB-02 của `docs/ops/ads-business-rules.md`). Dự án mẫu không có luồng giới thiệu (câu hỏi 11 của spec): ticket chỉ làm composable và ví dụ trên màn xem thử.

## Phạm vi

- Composable native giới thiệu và native toàn trang; chặn quảng cáo tự hiện khi trang native đang hiện.
- Screenshot test theo thiết kế vẽ lại (T-035).
- Native ở bước của luồng theo mô hình chung (MC-06): định dạng `native_step` (thẻ 250 dp, pool) và `native_fullscreen` (cả trang); một vị trí cho cả luồng, bước có quảng cáo do spec của app chọn (câu hỏi 32); luồng giới thiệu dùng vị trí `native_onboarding` (khóa `is_show_native_onboarding`). Không có composable mang tên "onboarding" hay số trang.
- Khe của bước đầu xếp ở nhóm nạp sẵn đầu (HD-02).
- Test nghiệm thu gợi ý: `AT-NT-*` phần pool và bước của luồng.

**Không bao gồm:**

- Luồng giới thiệu của app (spec của từng app).

## Rủi ro sơ bộ

Quảng cáo trong module core; quảng cáo trên luồng giới thiệu là rủi ro chính sách (ADR-0026). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
