---
id: T-019
title: Khóa Remote Config quảng cáo theo tên AdsKit và danh sách vị trí theo mô hình chung
feature: ads
spec: features/ads/spec.md
scenarios: [KB-7, KB-10, KB-25]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-007, T-008, T-009]
sprint:
plan: features/ads/plans/T-019-khoa-remote-config-theo-adskit.md
created: 2026-10-23
---

# T-019: KHÓA REMOTE CONFIG QUẢNG CÁO THEO TÊN ADSKIT VÀ DANH SÁCH VỊ TRÍ THEO MÔ HÌNH CHUNG

## Mô tả

Khóa quảng cáo trong `:core:ads` đổi sang đúng tên của AdsKit và các khóa mới cùng kiểu, theo bảng ở `docs/ops/ads-business-rules.md`, mục 10. Có một bộ cờ quảng cáo đọc từ Remote Config dạng `Flow` cho `:core:ads` và vỏ app. Giá trị cục bộ của staging (T-008) bật đủ khóa mới. Bảng khóa sinh tự động (T-007) liệt kê tên mới.

## Phạm vi

- Khai báo các khóa ở mục 10 bằng `RemoteKey` (T-007), mặc định theo bảng; `is_show_<vị trí>` thay `ads_<vị trí>_enabled`.
- Gỡ khóa và luật của T-009 mà PARTNER không có (ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên), theo câu hỏi 10 của spec; công tắc tổng theo câu hỏi 7.
- Bộ cờ quảng cáo dạng `Flow`; test mặc định và đọc lại sau khi đổi giá trị.
- Cập nhật giá trị cục bộ của staging và sinh lại `docs/remote-config-keys.md`.
- Danh sách vị trí theo mô hình chung (MC-01 tới MC-03 của `ads-business-rules.md`): `AdPlacement` mang mã, định dạng (danh sách đóng MC-02), nhóm giãn cách và thuộc tính của định dạng; khóa suy ra theo mẫu `is_show_<mã vị trí>`, `ads_interval_<nhóm>`, `<tham số>_<mã vị trí>` (mẫu theo câu hỏi 28), có test: khai báo một vị trí giả thì khóa có trong bảng khóa với mặc định tắt (KB-25).
- Khóa mới của mục 10.1: `is_show_native_collap` (mặc định bật), `is_enable_consent_shortcut` mặc định **tắt** (câu hỏi 21), 11 công tắc `is_log_af_*` (mặc định bật). Bảng khóa sinh tự động có cột hay ghi chú "tên do ta đặt" cho khóa không có ở `DG:171-181`.
- Không khai báo `time_reload_collap_ad`, `resume_ads_interval`, `time_reload_bottom_ad` (mục 10).

**Không bao gồm:**

- Dùng khóa để đổi hành vi định dạng (các ticket sau).

## Rủi ro sơ bộ

Module core `:core:config` và `:core:ads` (điểm chặn của risk-scoring.md). Gỡ luật đã merge ở T-009 cần test chứng minh không còn đường đọc khóa cũ. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
