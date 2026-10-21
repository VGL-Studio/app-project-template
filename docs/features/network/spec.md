---
feature: network
status: draft            # draft | confirmed | changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-app-luon-can-mang.md
design:                  # popup dựng trong hệ thống thiết kế, xem câu hỏi 1
created: 2026-10-21
confirmed:
---

# SPEC: APP LUÔN CẦN MẠNG

<!--
Phần nền của dự án mẫu (module core và :app), không phải một feature riêng của người dùng. Quyết định: ADR-0024.
Liên quan: spec loading (màn hình loading tự báo lỗi khi không có mạng lúc mở app), ADR-0018.
-->

## 1. Mục tiêu

- **Người dùng được gì:** biết ngay khi app không dùng được vì mất mạng, và tiếp tục ngay khi có mạng trở lại, không phải mở lại app.
- **Vì sao làm bây giờ:** app kiếm tiền bằng quảng cáo; không có mạng thì không có quảng cáo và không tối ưu được doanh thu (ADR-0024).
- **Đo kết quả bằng gì:** không còn phiên dùng app nào khi không có Internet; tỷ lệ phiên có quảng cáo được yêu cầu.

## 2. Phạm vi

**Trong phạm vi:**

- Theo dõi có Internet dùng được hay không, dùng chung cho cả app.
- Popup mất mạng chung trên mọi màn hình, có nút "Thử lại", tự đóng khi có mạng.
- Chặn vào app khi không có mạng, kể cả khi màn hình loading bị tắt.

**Ngoài phạm vi lần này:**

- Đổi cách màn hình loading báo lỗi (giữ như spec `loading`).
- Chế độ dùng khi không có mạng (không làm, ADR-0024).

## 3. Kịch bản sử dụng

### KB-1. Mở app khi không có mạng

Tiêu chí chấp nhận:

- [ ] Khi màn hình loading bật và không có mạng thì màn hình loading báo lỗi và có nút "Thử lại" như spec `loading`; popup mất mạng không hiện chồng lên.
- [ ] Khi màn hình loading tắt và không có mạng thì popup mất mạng hiện ngay trên màn hình đầu tiên và chặn thao tác.
- [ ] Khi Wi-Fi bật nhưng không vào được Internet thì app coi là không có mạng.

### KB-2. Mất mạng trong lúc dùng

Tiêu chí chấp nhận:

- [ ] Khi đang dùng mà mất Internet thì sau một khoảng chờ ngắn (mặc định 2 giây, tránh hiện khi mạng chỉ chập chờn) popup mất mạng hiện: tiêu đề, lời giải thích ngắn, nút "Thử lại".
- [ ] Khi popup đang hiện thì không chạm được vào màn hình bên dưới; chạm ra ngoài hay bấm nút quay lại của máy không đóng popup.
- [ ] Khi bấm "Thử lại" mà vẫn không có mạng thì popup vẫn hiện và cho biết vẫn chưa có mạng.
- [ ] Khi có mạng trở lại thì popup tự đóng và người dùng ở đúng màn hình, đúng trạng thái trước đó.

Trường hợp đặc biệt:

- [ ] Khi một quảng cáo toàn màn hình đang hiện thì popup không chen vào; nếu vẫn mất mạng khi quảng cáo đóng thì popup hiện ngay.
- [ ] Khi app ở nền rồi quay lại mà không có mạng thì popup hiện.

### KB-3. Lỗi tải khi vẫn có mạng

Tiêu chí chấp nhận:

- [ ] Khi vẫn có mạng nhưng máy chủ lỗi hay quá thời gian thì màn hình của feature báo lỗi và có nút thử lại như trước; popup mất mạng không hiện.

## 4. Giao diện

- **Thiết kế:** popup dùng thành phần của hệ thống thiết kế (hộp thoại, nút chính). Xem câu hỏi 1.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Popup mất mạng | Báo mất mạng, thử lại | KB-1, KB-2 |
| Màn hình mẫu (đã có) | Màn hình bên dưới popup trong test | KB-2 |

## 5. Quảng cáo

**Màn hình chức năng chính (không có quảng cáo):** không áp dụng.

Không có vị trí quảng cáo mới. Popup không có quảng cáo.

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:** không có.

**Sự kiện phân tích:**

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `network_lost_shown` | Khi popup mất mạng hiện | `screen`: màn hình bên dưới | Người dùng mất mạng ở đâu, bao lâu một lần |
| `network_restored` | Khi popup tự đóng vì có mạng | `duration_ms`: thời gian popup hiện; `retry_count` | Mất mạng thường kéo dài bao lâu |

Sự kiện ghi khi mất mạng sẽ được Firebase gửi khi có mạng trở lại. Xem câu hỏi 2.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ: quy tắc áp dụng cho mọi app (ADR-0024). Khóa Remote Config:

| Khóa | Kiểu | Mặc định | Ý nghĩa |
| --- | --- | --- | --- |
| `network_lost_delay_seconds` | long | 2 | Chờ bao lâu sau khi mất mạng mới hiện popup |

## 9. Câu hỏi còn mở

1. **Có dựng popup trên Figma không?**
   - Đề xuất của AI: có, cùng file "App Template: Design System", một component hộp thoại dùng chung (popup mất mạng là trường hợp đầu tiên), sáng và tối.
   - Trả lời: Có.
2. **Có ghi hai sự kiện phân tích ở mục 6 không?**
   - Đề xuất của AI: có; giúp biết người dùng mất mạng nhiều ở đâu, nhất là ở các thị trường mạng yếu.
   - Trả lời: Có, ghi cả hai sự kiện.
3. **Chữ trên popup.**
   - Đề xuất của AI: tiêu đề "Không có kết nối mạng"; nội dung "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại."; nút "Thử lại". Bản tiếng Anh tương ứng.
   - Trả lời: Duyệt theo đề xuất.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu và đề xuất đã duyệt (ADR-0024) |
| 2026-10-21 | Nhật | Trả lời 3 câu hỏi |
