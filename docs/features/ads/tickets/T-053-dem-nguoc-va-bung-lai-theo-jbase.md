---
id: T-053
title: Đếm ngược native sau inter và bung lại native thu gọn theo JBase
feature: ads
spec: features/ads/spec.md
scenarios: [KB-16, KB-5, KB-4, KB-32]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-053-dem-nguoc-va-bung-lai-theo-jbase.md
auto_run: 
created: 2026-10-29
---

# T-053: ĐẾM NGƯỢC NATIVE SAU INTER VÀ BUNG LẠI NATIVE THU GỌN THEO JBASE

## Mô tả

Native toàn màn hình sau inter đếm ngược mặc định 5 giây, chỉnh từ xa bằng `time_close_native` (1 tới 30), vòng đếm ngược và nút đóng 48 dp; native thu gọn tự bung lại sau 10 giây theo mặc định (ADR-0038, quyết định 3). Mã này dùng lại số T-053 đã bỏ (hạn dùng interstitial, câu hỏi 35) vì ticket cũ chưa từng vào nhánh chính.

## Phạm vi

- Khóa mới `time_close_native` trong `AdsSettings` (long, mặc định 5, ngoài 1..30 dùng 5), vào bảng khóa sinh tự động và `remote-config-template.json`.
- `NATIVE_AFTER_INTER_COUNTDOWN_MILLIS` đọc từ khóa; giữ phần chờ inter rời màn 2 giây (K-02).
- Vòng đếm ngược và nút đóng 48 dp (K-03); ghi lại ảnh chuẩn của màn native sau inter (liệt kê trong plan).
- `time_expand_collap_ad` mặc định 10; 0 vẫn là không bung lại.
- Sửa `ads-business-rules.md` K-02, K-03, CL-03, mục 10, 11 kèm ADR-0038.

**Không bao gồm:**

- Kích thước native (T-064, T-065).

## Rủi ro sơ bộ

Module core, quảng cáo, khóa Remote Config mới (điểm chặn 1, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
