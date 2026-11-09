---
id: T-096
title: Hộp thoại mất mạng hiện ngay khi mở lại app đã bị hệ thống đóng và khi activity dựng lại
feature: network
spec: features/network/spec.md
scenarios: [KB-2]
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa :core:network và vỏ app (điểm chặn 7)
size: M
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-09
---

# T-096: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI MỞ LẠI APP ĐÃ BỊ HỆ THỐNG ĐÓNG VÀ KHI ACTIVITY DỰNG LẠI

## Mô tả

Nguồn: issue #368 (camera-pose), bài học L-160. KB-2 có trường hợp đặc biệt "Khi app ở nền rồi quay lại mà không có mạng thì popup hiện"; mẫu xử lý lần quay lại thường bằng `NetworkDialogController.onAppForeground()`. Hai đường tới cùng tình huống chưa có: hệ thống đóng app ở nền rồi người dùng mở lại từ màn đa nhiệm khi không có mạng (hộp thoại chờ khoảng chờ của KB-2), và activity dựng lại trong cùng tiến trình (xoay, gập máy, đổi sáng tối, đổi ngôn ngữ) làm hộp thoại đang hiện bị ẩn rồi mới hiện lại.

## Phạm vi

- `:core:network`, `NetworkDialogController`: phân biệt lần mở đầu, quay lại app (kể cả sau khi hệ thống đóng app) và dựng lại activity; quay lại ở màn sau loading mà không có mạng thì kiểm lại ngắn rồi hiện, không chờ khoảng chờ của KB-2; dựng lại activity không ẩn hộp thoại đang hiện.
- `:app`, `AppNavHost`: chỉ báo "đã qua loading" khi back stack đã có màn (khung hình đầu của activity mới chưa có).
- Gộp với `onAppForeground()` có sẵn, không thêm đường thứ hai cho cùng việc.
- Test JVM cho từng đường, test dựng lại activity (`ActivityScenario.recreate()`), test đưa activity ra nền rồi trở lại.
- Plan đối chiếu KB-1, KB-2 của spec: khoảng thời gian "hiện ngay" chưa ghi trong spec thì đặt câu hỏi cho người phụ trách sản phẩm.

**Không bao gồm:**

- Đổi chữ, giao diện của hộp thoại.

## Rủi ro sơ bộ

Sửa module core `:core:network` và vỏ app (`AppNavHost`): điểm chặn 7, rủi ro cao, duyệt chéo. Sai thì hộp thoại hiện ở màn loading hay không hiện khi mất mạng.

## Định nghĩa hoàn thành

- [ ] Mọi điểm của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
