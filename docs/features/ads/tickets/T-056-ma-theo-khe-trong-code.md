---
id: T-056
title: Mã đơn vị theo khe trong code: mọi lớp của thanh đáy có mã ở production
feature: ads
spec: features/ads/spec.md
scenarios: [KB-17]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-056-ma-theo-khe-trong-code.md
auto_run: 
created: 2026-10-29
---

# T-056: MÃ ĐƠN VỊ THEO KHE TRONG CODE: MỌI LỚP CỦA THANH ĐÁY CÓ MÃ Ở PRODUCTION

## Mô tả

Làm phần code của câu hỏi 14 (đã chốt: mã theo khe). Hôm nay `ConfiguredAdUnitIds` chỉ có mã của định dạng chính của vị trí (`AdUnitIds.kt:15`), nên ở production khe banner và lớp native thu gọn của thanh đáy không gửi yêu cầu nào; mặc định `is_show_native_bottom` tắt chọn đúng khe banner, nên thanh đáy trống.

## Phạm vi

- `ConfiguredAdUnitIds` đọc thêm mã theo khe, dạng `<mã vị trí>/<định dạng>=<mã>` (hay `<khe>=<mã>`, plan chốt theo câu hỏi 14 và M-05), và vẫn đọc được dạng cũ `<mã vị trí>=<mã>` để bản production không hỏng khi `release.sh` chưa đổi.
- `missing` liệt kê cả lớp của thanh đáy thiếu mã.
- Cập nhật `docs/ops/ads-guide.md` mục 5 và 6 (dòng "production: mã lớp banner và thu gọn chưa có").

**Không bao gồm:**

- Sửa `scripts/release.sh` và `release.properties` (T-057, việc của người).

## Rủi ro sơ bộ

Module core, cấu hình build phát hành (điểm chặn 7). Không chạm mã thật; test dùng chuỗi mã giả.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
