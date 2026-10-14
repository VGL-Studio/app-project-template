---
id: RUN-YYYYMMDD-NN
status: draft                # draft | approved | running | finished
sprint: <tên hoặc số sprint>
approved_by: <dev duyệt danh sách và kích hoạt phiên>
max_tasks: <số task tối đa>
staging_needed_by:           # tùy chọn: thời điểm muốn có bản staging, ví dụ 2026-10-21 08:00
started_at:
finished_at:
staging_build:               # phiên bản bản staging được phát cuối phiên
---

# PHIÊN TỰ CHẠY RUN-YYYYMMDD-NN

## 1. Danh sách chạy

<!-- AI lập sau họp đánh giá rủi ro. Dev xem lại và chuyển trạng thái sang approved. -->

| Thứ tự | Ticket | Feature | Plan (phiên bản) | Rủi ro | Phụ thuộc | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | T-NNN | <feature> | `features/<feature>/plans/T-NNN-....md` (1) | Thấp | | |
| 2 | T-NNN | <feature> | `...` (2) | Cao | T-NNN | Đã duyệt chéo |

**Lý do của thứ tự chạy:** <vì sao task này chạy trước task kia: phụ thuộc, ảnh hưởng chéo, rủi ro.>

## 2. Kiểm tra đầu phiên

<!-- AI điền khi bắt đầu phiên. -->

- [ ] Nhánh chính đang xanh, không có issue hay PR đang mở mang nhãn `main-broken`.
- [ ] Mọi plan và ticket trong danh sách có trạng thái `approved`.
- [ ] Các ticket phụ thuộc đã sẵn sàng.

Task bị bỏ qua: <mã ticket và lý do, hoặc "Không có">

## 3. Báo cáo phiên

<!-- AI viết khi kết thúc phiên. Đây là thứ dev đọc đầu tiên khi bắt đầu làm việc lại. -->

### Tóm tắt

<Hai đến ba câu: bao nhiêu task merge, bao nhiêu task dừng, điều quan trọng nhất cần người xem.>

### Kết quả từng task

| Ticket | Kết quả | PR | Test viết trước | Rà soát độc lập | CI | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| T-NNN | Đã merge | <liên kết> | 8 test, đã thất bại đúng lý do | 0 chặn, 1 ghi chú | Xanh | |
| T-NNN | Dừng ở bước 8 | <liên kết> | | | Đỏ sau 3 lần sửa | Xem nhật ký plan |

### Cần người kiểm tra

- [ ] Ảnh chuẩn screenshot mới: <màn hình, đường dẫn ảnh>
- [ ] Tiêu chí kiểm tra thủ công: <ticket, tiêu chí, các bước>
- [ ] Điểm rà soát mức ghi chú: <ticket, nội dung>

### Task bị dừng

| Ticket | Dừng ở bước | Lý do | Đề xuất hướng xử lý |
| --- | --- | --- | --- |
| | | | |

### Bản staging

- Phiên bản: <...>
- Kênh phát: Firebase App Distribution / Internal testing
- Gồm các ticket: <...>

### Thời gian và mức sử dụng

- Bắt đầu: <...>, kết thúc: <...>, lý do kết thúc: <hết danh sách / đến giờ / hết hạn mức / nhánh chính đỏ>
- Mức sử dụng: <nếu đo được>

### Đề xuất cho buổi họp tiếp theo

1. <...>
