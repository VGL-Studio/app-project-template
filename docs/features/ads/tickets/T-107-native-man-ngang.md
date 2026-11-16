---
id: T-107
title: Khe đáy và lớp thu gọn ở màn ngang, chia đôi màn hình
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-4, KB-32, KB-33]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-065]
sprint:
plan:
auto_run:
created: 2026-11-16
---

# T-107: KHE ĐÁY VÀ LỚP THU GỌN Ở MÀN NGANG, CHIA ĐÔI MÀN HÌNH

## Mô tả

Từ T-065, khe native nhỏ ở đáy và lớp thu gọn nội suy theo bề rộng màn hình. Ở màn ngang (điện thoại 411 x 891 dp xoay ngang) lớp thu gọn khoảng 400 dp, cao hơn màn, phủ hết nội dung (rà soát độc lập PR #512, tính trên giấy). Làm theo spec `ads`, câu hỏi 65 (chỉ soạn plan khi spec đã `confirmed` lại): nội suy theo cạnh ngắn của cửa sổ, lớp thu gọn có trần theo chiều cao cửa sổ.

## Phạm vi

- `bottomSlotHeight`, `collapsibleMediaAreaHeight`, `nativeReservation`, `bottomBarReservation` (`:core:ads`) đọc cạnh ngắn của cửa sổ thay bề rộng; một hàm cho vẽ, khung chờ và khoảng chừa.
- Trần của lớp thu gọn theo chiều cao cửa sổ: vùng ảnh co trước, không dưới 120 dp (như KB-2 tiêu chí 1); không vừa thì không mở lớp, chỉ có khe native nhỏ (sự kiện và nhật ký như khi lớp không mở; plan nêu rõ).
- Test ở khung ngang `w891dp-h411dp-xxhdpi` và khung chia đôi (ví dụ `w411dp-h445dp`); điện thoại dọc giữ nguyên số (126 dp, 299 dp ở 411 dp), ảnh chuẩn dọc không đổi.
- Plan trả lời: xoay màn khi lớp đang mở (dọc sang ngang đóng hay giữ; ngang sang dọc mở lại không); bộ đếm tự bung lại (CL-03) khi cửa sổ không đủ chỗ; kho nạp sẵn `native_collap` (KB-33) có nạp khi lớp không mở được không (phí yêu cầu, tỷ lệ hiện).
- Cập nhật `ads-business-rules.md` (5.7), README thiết kế, `ads-guide.md`.
- Ghi chú thêm (rà soát PR #512): vùng chạm thật của nút thu gọn khoảng 37,5 dp vì mép lớp cắt phía trái và trên; plan nêu cho người duyệt, không đổi khi chưa có thiết kế mới.

**Không bao gồm:**

- Medium (đã theo chiều cao cửa sổ), máy tính bảng.

## Rủi ro sơ bộ

Module core, giao diện quảng cáo; điểm chặn `ads`, `core`.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
