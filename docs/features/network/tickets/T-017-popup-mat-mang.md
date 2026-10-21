---
id: T-017
title: Popup mất mạng chung và chặn vào app khi không có mạng
feature: network
spec: features/network/spec.md
scenarios: [KB-1, KB-2, KB-3]
owner: Nhật
status: ready
risk: high
size: M
depends_on: [T-016]
sprint:
plan:
created: 2026-10-21
---

# T-017: POPUP MẤT MẠNG CHUNG VÀ CHẶN VÀO APP KHI KHÔNG CÓ MẠNG

## Mô tả

`:app` hiện popup mất mạng theo thiết kế (`docs/features/network/design/`) trên mọi màn hình khi bộ theo dõi (T-016) báo mất mạng: chặn thao tác, không đóng bằng chạm ra ngoài hay nút quay lại, nút "Thử lại" kiểm tra lại, tự đóng khi có mạng. Không hiện chồng lên màn hình loading và không chen vào lúc quảng cáo toàn màn hình đang hiện. Khi màn hình loading tắt và không có mạng thì popup chặn ngay từ màn hình đầu. Ghi `network_lost_shown` và `network_restored`.

## Phạm vi

- Composable popup (chữ trong `strings.xml`, tiếng Việt và tiếng Anh), gắn ở `AppNavHost` hay `MainActivity`.
- Điều kiện không hiện: đang ở màn hình loading, đang có quảng cáo toàn màn hình (`:core:ads` cung cấp trạng thái).
- Hai sự kiện phân tích qua `:core:analytics`; log qua `AppLogger`.
- Test giao diện JVM và screenshot test theo tên ảnh thiết kế.

**Không bao gồm:**

- Bộ theo dõi kết nối (T-016).

## Rủi ro sơ bộ

Đổi `:app` và cần một trạng thái từ `:core:ads` (quảng cáo toàn màn hình đang hiện); thêm sự kiện phân tích theo spec đã duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
