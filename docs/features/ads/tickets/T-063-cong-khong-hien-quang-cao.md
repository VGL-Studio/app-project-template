---
id: T-063
title: Cổng không hiện quảng cáo cho người đã mua gói bỏ quảng cáo
feature: ads
spec: features/ads/spec.md
scenarios: [KB-30]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-063-cong-khong-hien-quang-cao.md
auto_run: 
created: 2026-10-29
---

# T-063: CỔNG KHÔNG HIỆN QUẢNG CÁO CHO NGƯỜI ĐÃ MUA GÓI BỎ QUẢNG CÁO

## Mô tả

`:core:ads` nhận tín hiệu "người dùng đã mua gói bỏ quảng cáo" từ app; bật thì mọi định dạng như công tắc tổng tắt, hành động có điều kiện làm thẳng (ADR-0036, điểm 5; anh Nhật chọn chỉ làm cổng, chưa làm mua hàng).

## Phạm vi

- Một tín hiệu mà `:app` cấp qua Hilt, mặc định luôn `false` (câu hỏi 39); kết hợp với `is_show_ads` trong `AdsFlags`.
- Đổi giữa phiên: khe trong màn hình biến mất ngay, không tải thêm, quảng cáo mở app và khi quay lại không hiện; test cờ đổi giữa chừng (L-061).
- Hướng dẫn nối tín hiệu cho app có mua hàng ở `ads-guide.md`.

**Không bao gồm:**

- Mua hàng, Play Billing, doanh thu mua hàng (ngoài phạm vi, yêu cầu 2026-10-29).

## Rủi ro sơ bộ

Module core, quảng cáo, thứ tự khởi tạo (điểm chặn 1, 5, 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
