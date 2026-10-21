---
id: T-016
title: Theo dõi có Internet dùng được, dùng chung cho cả app
feature: network
spec: features/network/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-007]
sprint:
plan: features/network/plans/T-016-theo-doi-ket-noi.md
created: 2026-10-21
---

# T-016: THEO DÕI CÓ INTERNET DÙNG ĐƯỢC, DÙNG CHUNG CHO CẢ APP

## Mô tả

Một bộ theo dõi kết nối ở module core đưa ra trạng thái có Internet dùng được hay không dưới dạng `Flow` (mạng mặc định có `NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`, ADR-0024). Trạng thái "mất mạng" chỉ báo sau `network_lost_delay_seconds` (khóa Remote Config mới, mặc định 2 giây) để không báo khi mạng chập chờn. Có hàm kiểm tra lại ngay cho nút "Thử lại".

## Phạm vi

- Bộ theo dõi trong module core (plan chọn module: `:core:data` hay module mới `:core:network`), cài bằng `ConnectivityManager`, bọc sau một interface để test bằng nguồn giả.
- Khóa `network_lost_delay_seconds` khai báo theo ADR-0023.
- Unit test: chờ trước khi báo mất mạng, báo có mạng ngay, Wi-Fi bật mà chưa xác nhận Internet là mất mạng.

**Không bao gồm:**

- Popup và chặn vào app (T-017).

## Rủi ro sơ bộ

Module core mới hoặc đổi module core (điểm chặn). Có thể cần quyền `ACCESS_NETWORK_STATE` (thường đã có qua SDK quảng cáo; plan kiểm manifest gộp).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
