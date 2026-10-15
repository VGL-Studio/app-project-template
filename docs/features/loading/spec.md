---
feature: loading
status: confirmed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-15-man-hinh-loading.md
design:
created: 2026-10-15
confirmed: 2026-10-15
---

# SPEC: MÀN HÌNH LOADING

## 1. Mục tiêu

- **Người dùng được gì:** mỗi lần mở app, người dùng thấy app đang chuẩn bị và tiến độ tới đâu. Khi vào màn hình chính, app đã có cấu hình mới nhất và đủ file cần dùng.
- **Vì sao làm bây giờ:** các app sau này cần chuẩn bị cấu hình và dữ liệu trước khi vào màn hình chính. Dự án mẫu cần một màn hình loading làm mẫu cho việc này. Đây cũng là task của phiên tự chạy thật đầu tiên.
- **Đo kết quả bằng gì:** sự kiện `loading_completed` cho biết bao nhiêu lần mở app chuẩn bị xong và mất bao lâu.

## 2. Phạm vi

**Trong phạm vi:**

- Màn hình loading hiện mỗi lần mở app, có thanh tiến trình ngang và phần trăm từ 0% đến 100%.
- Bốn phần việc chuẩn bị, mỗi phần chiếm 25% thanh tiến trình:
  1. Tải cấu hình mới nhất của app (Firebase Remote Config).
  2. Tải file `hello.txt` từ R2.
  3. Tải file `content.txt` từ R2.
  4. Tải file `goodbye.txt` từ R2.
- Màn hình hiện ít nhất 2 giây.
- Chuẩn bị xong thì tự chuyển sang màn hình chính hiện tại (màn hình mẫu).
- Không chuẩn bị được thì báo lỗi, có nút "Thử lại", không vào màn hình chính.

**Ngoài phạm vi lần này:**

- Màn hình chào của hệ thống Android (biểu tượng app hiện trong lúc app khởi động) giữ nguyên.
- Dùng nội dung của ba file ở màn hình khác. Lần này chỉ tải về và giữ trên máy.
- Loading trong từng màn hình khác, hình ảnh hay hoạt ảnh riêng cho màn hình loading.

## 3. Kịch bản sử dụng

### KB-1. Mở app, chuẩn bị thành công

Người dùng mở app khi có mạng.

Tiêu chí chấp nhận:

- [ ] Khi mở app thì màn hình loading hiện đầu tiên, có thanh tiến trình ngang, phần trăm và dòng chữ cho biết app đang chuẩn bị.
- [ ] Khi một phần việc xong thì thanh tiến trình tăng thêm 25%, không bao giờ lùi.
- [ ] Khi cả bốn phần việc xong và màn hình đã hiện đủ 2 giây thì app tự chuyển sang màn hình chính, không cần bấm gì.
- [ ] Khi cả bốn phần việc xong trước 2 giây thì thanh tiến trình đứng ở 100% cho tới đủ 2 giây rồi mới chuyển.
- [ ] Khi chuyển sang màn hình chính thì app ghi sự kiện `loading_completed` một lần.
- [ ] Khi đã vào màn hình chính và người dùng bấm nút quay lại thì app đóng, không quay lại màn hình loading.

Trường hợp đặc biệt:

- [ ] Khi người dùng xoay máy trong lúc loading thì tiến trình không bắt đầu lại từ đầu và thời gian 2 giây không tính lại.

### KB-2. Mở app lần đầu khi không có mạng

Người dùng cài app và mở lần đầu khi không có mạng.

Tiêu chí chấp nhận:

- [ ] Khi một phần việc không xong được thì màn hình loading báo lỗi bằng lời dễ hiểu và có nút "Thử lại". App không vào màn hình chính.
- [ ] Khi bấm "Thử lại" thì app chỉ làm lại những phần việc chưa xong, thanh tiến trình tiếp tục từ chỗ đã đạt.
- [ ] Khi có mạng trở lại và bấm "Thử lại" thì app chuẩn bị xong và vào màn hình chính.
- [ ] Khi tải cấu hình không có phản hồi sau 10 giây, hoặc tải một file không có phản hồi sau 15 giây, thì app coi phần việc đó là không xong được và báo lỗi như trên.

### KB-3. Mở lại app khi không có mạng

Người dùng đã từng mở app thành công, nay mở lại khi không có mạng.

Tiêu chí chấp nhận:

- [ ] Khi ba file đã có trên máy từ lần trước thì ba phần việc tải file xong ngay, không cần mạng.
- [ ] Khi không tải được cấu hình thì app báo lỗi và có nút "Thử lại" như KB-2, không vào màn hình chính, kể cả khi cấu hình đã từng tải thành công ở lần trước.

### KB-4. Tắt màn hình loading

Người phụ trách tắt màn hình loading từ xa khi có sự cố.

Tiêu chí chấp nhận:

- [ ] Khi cờ `feature_loading_enabled` tắt thì app mở thẳng vào màn hình chính như hiện nay, không ghi `loading_completed`.

## 4. Giao diện

- **Thiết kế:** Dùng thành phần có sẵn, không cần thiết kế mới: thanh tiến trình ngang của hệ thống thiết kế, phần trăm, một dòng chữ; khi lỗi thì thêm thông báo và nút "Thử lại".
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Loading | Cho người dùng thấy app đang chuẩn bị và tiến độ | KB-1, KB-2, KB-3 |
| Màn hình mẫu (đã có) | Màn hình chính sau khi chuẩn bị xong | KB-1, KB-4 |

## 5. Quảng cáo

Không có. Màn hình loading không hiện quảng cáo.

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Ba file tải từ R2 | Dùng khi không có mạng ở lần mở sau | Đến khi người dùng xóa dữ liệu app |

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `loading_completed` | Khi màn hình loading chuyển sang màn hình chính | `duration_ms`: thời gian từ lúc hiện màn hình loading tới lúc chuyển, tính cả 2 giây tối thiểu; `retry_count`: số lần bấm "Thử lại" | Bao nhiêu lần mở app chuẩn bị xong, mất bao lâu, có phải thử lại không |

## 7. File tĩnh trên R2

| Nhóm file | Dung lượng ước tính | Khi nào tải | Khi không tải được |
| --- | --- | --- | --- |
| `test/v1/hello.txt`, `test/v1/content.txt`, `test/v1/goodbye.txt` | Vài KB | Lần đầu mở app; lần sau dùng bản trên máy | Báo lỗi, cho thử lại (KB-2) |

## 8. Cờ tính năng

| Tên cờ | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `feature_loading_enabled` | tắt | Bật để hiện màn hình loading khi mở app. Bản staging bật sẵn |

## 9. Câu hỏi còn mở

1. **Mở lại app khi không có mạng, mà cấu hình đã từng tải thành công, có cho vào màn hình chính không?**
   - Đề xuất của AI: có.
   - Trả lời: không cho vào màn hình chính (KB-3).
2. **Tham số của `loading_completed`:**
   - Đề xuất của AI: `duration_ms` và `retry_count`.
   - Trả lời: `duration_ms` và `retry_count`.
3. **Thời gian chờ tối đa cho mỗi phần việc:**
   - Đề xuất của AI: 10 giây cho tải cấu hình; ba file dùng mức 15 giây sẵn có của việc tải file.
   - Trả lời: đồng ý.
4. **Bản build trên máy dev và trong CI dùng cấu hình Firebase giả, không bao giờ tải được cấu hình:**
   - Đề xuất của AI: với cấu hình giả, phần việc tải cấu hình được tính là xong ngay. Bản staging phát cho nhóm và bản production luôn tải thật.
   - Trả lời: đồng ý.
5. **Hai file `content.txt` và `goodbye.txt` chưa có trên R2 staging:**
   - Trả lời: đã đưa lên. AI kiểm tra lại ngày 2026-10-15: cả ba file trong `test/v1/` đều tải được.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-15 | AI | Tạo bản đầu từ yêu cầu |
| 2026-10-15 | AI | Cập nhật theo câu trả lời của anh Nhật: đo Remote Config và ba file R2, thanh ngang có phần trăm, báo lỗi và thử lại khi mất mạng lần đầu, hiện mỗi lần mở app, tối thiểu 2 giây, sự kiện `loading_completed`, tên feature `loading` |
| 2026-10-15 | AI | Cập nhật theo câu trả lời mục 9: mở lại khi không có mạng cũng báo lỗi, tham số của `loading_completed`, thời gian chờ, cấu hình giả trên máy dev và CI, ba file đã có trên R2 |
