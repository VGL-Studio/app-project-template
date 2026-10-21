---
feature: ads
status: confirmed            # draft | confirmed | changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-native-va-remote-config.md
design: features/ads/design/README.md
created: 2026-10-21
confirmed: 2026-10-21
---

# SPEC: QUẢNG CÁO NATIVE VÀ REMOTE CONFIG CỦA DỰ ÁN MẪU

<!--
Đây là phần nền của dự án mẫu (module core), không phải một màn hình cho người dùng cuối. "Người dùng" trong kịch bản
là người dùng app tạo từ dự án mẫu, hoặc người vận hành app (bật tắt quảng cáo trên Firebase).
Quyết định: ADR-0022 (quảng cáo), ADR-0023 (Remote Config). Hướng dẫn: docs/ops/ads-guide.md, docs/ops/remote-config.md.
-->

## 1. Mục tiêu

- **Người dùng được gì:** quảng cáo trong app giống giao diện app, không đặt ở chỗ dễ bấm nhầm, không xuất hiện dày đặc hay lúc vừa mở app; người vận hành bật tắt và chỉnh tần suất từ xa, thử được quảng cáo trên bản staging trước khi phát hành.
- **Vì sao làm bây giờ:** app Camera Pose cần native và mức quảng cáo vừa; làm ở dự án mẫu để mọi app dùng chung, không lệch nhau.
- **Đo kết quả bằng gì:** mỗi app bật quảng cáo theo hướng dẫn mà không phải sửa module core; không có cảnh báo chính sách AdMob hay Play về vị trí đặt quảng cáo; ARPDAU theo dõi cùng giữ chân D7 trong báo cáo vận hành.

## 2. Phạm vi

**Trong phạm vi:**

- Năm loại native: small, medium, trong danh sách, thu gọn được, toàn màn hình.
- Giới hạn tần suất lưu qua các phiên, ân hạn phiên đầu, giới hạn số quảng cáo toàn màn hình mỗi phiên, công tắc tổng.
- Mediation AdMob và AppLovin.
- Remote Config: kiểu số thực và chuỗi, bảng khóa sinh tự động, giá trị cục bộ cho bản staging.
- Một màn hình chỉ có ở bản staging để xem thử mọi loại quảng cáo (câu hỏi 2).

**Ngoài phạm vi lần này:**

- App open, rewarded.
- Vị trí quảng cáo của từng app (mỗi app tự khai báo theo spec của app).
- Mạng quảng cáo khác ngoài AdMob và AppLovin.

## 3. Kịch bản sử dụng

### KB-1. Native small thay banner

Một màn hình của app có vị trí native small đã bật.

Tiêu chí chấp nhận:

- [ ] Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices; vùng quảng cáo có nền hoặc viền tách khỏi nội dung; CTA khác nút chính của app.
- [ ] Khi đang tải thì vùng quảng cáo giữ đúng chiều cao với hiệu ứng chờ, nội dung bên dưới không nhảy.
- [ ] Khi không có quảng cáo sau thời gian chờ thì vùng quảng cáo thu lại; nếu vị trí được khai báo là cạnh nút của app thì giữ khoảng trống để nút không dịch.
- [ ] Khi app ở chế độ tối thì quảng cáo dùng màu tối của hệ thống thiết kế.

Trường hợp đặc biệt:

- [ ] Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ.
- [ ] Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ.

### KB-2. Native medium thay MREC

Tiêu chí chấp nhận:

- [ ] Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt.
- [ ] Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1.

### KB-3. Native trong danh sách

Một danh sách hay lưới có vị trí native trong danh sách.

Tiêu chí chấp nhận:

- [ ] Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục (N chỉnh từ xa, mặc định 8), không bao giờ ở vị trí đầu tiên.
- [ ] Khi danh sách ít hơn N mục thì không có quảng cáo.
- [ ] Mỗi ô quảng cáo có nhãn "Ad" rõ, không thể nhầm với một mục của danh sách.

### KB-4. Native thu gọn được

Tiêu chí chấp nhận:

- [ ] Khi vào màn hình thì quảng cáo ở dạng mở (như medium) ở đáy màn hình, có nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices.
- [ ] Khi người dùng chạm nút thu gọn thì quảng cáo chuyển sang dạng gọn (như small).
- [ ] Khi đã thu gọn thì quảng cáo không tự mở lại trong lúc người dùng còn ở màn hình đó, kể cả khi tải quảng cáo mới.

### KB-5. Native toàn màn hình thay interstitial

Một điểm dừng tự nhiên trong app (ví dụ sau khi lưu xong một ảnh) gọi quảng cáo toàn màn hình.

Tiêu chí chấp nhận:

- [ ] Khi đủ điều kiện tần suất thì quảng cáo hiện toàn màn hình với nút đóng thấy và bấm được ngay, ít nhất 48 dp, ở góc khác AdChoices.
- [ ] Khi bấm nút đóng hoặc nút quay lại của máy thì quảng cáo đóng và người dùng về đúng chỗ đang làm.
- [ ] Khi vừa có một quảng cáo toàn màn hình (interstitial hay native) thì không hiện thêm cái nào cho tới hết khoảng cách tối thiểu.
- [ ] Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng.

### KB-6. Tần suất qua các phiên

Tiêu chí chấp nhận:

- [ ] Khi người dùng đóng app rồi mở lại ngay thì khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình vẫn được tính từ lần hiện trước.
- [ ] Khi người dùng mở app lần đầu thì không có quảng cáo toàn màn hình trong thời gian ân hạn (mặc định 180 giây).
- [ ] Khi đã đủ số quảng cáo toàn màn hình tối đa của phiên (mặc định 4) thì không hiện thêm cho tới phiên sau.

### KB-7. Công tắc tổng

Tiêu chí chấp nhận:

- [ ] Khi người vận hành tắt `ads_enabled` trên Firebase thì ở lần cập nhật cấu hình tiếp theo app không còn tải hay hiện quảng cáo nào, ở mọi vị trí.
- [ ] Khi bật lại thì các vị trí đang bật hoạt động lại mà không cần cập nhật app.

### KB-8. Thử quảng cáo trên bản staging

Tiêu chí chấp nhận:

- [ ] Khi mở bản staging thì mọi loại quảng cáo hiện được bằng mã thử của Google, không cần project Firebase thật.
- [ ] Khi mở màn hình xem thử quảng cáo (chỉ có ở staging) thì thấy từng loại native, nút gọi interstitial và native toàn màn hình, và mở được Ad Inspector của AdMob.
- [ ] Bản production không có màn hình xem thử và không có giá trị cục bộ.

### KB-9. Mediation AdMob và AppLovin

Tiêu chí chấp nhận:

- [ ] Khi đơn vị quảng cáo có AppLovin trong nhóm mediation trên AdMob thì app nhận được quảng cáo của AppLovin mà không đổi code.
- [ ] Khi mở Ad Inspector trên staging thì thấy adapter AppLovin đã khởi tạo.

### KB-10. Bảng khóa Remote Config

Tiêu chí chấp nhận:

- [ ] Khi thêm một khóa trong code mà chưa sinh lại bảng khóa thì kiểm tra tự động báo lỗi.
- [ ] Bảng khóa liệt kê mọi khóa với kiểu, giá trị mặc định, module và mô tả; có file mẫu nhập được vào Firebase.
- [ ] Module core đọc được khóa kiểu số thực và chuỗi.

## 4. Giao diện

- **Thiết kế:** các cỡ native là component của hệ thống thiết kế, dựng theo token có sẵn (màu, chữ, bo góc, khoảng cách) và quy chuẩn ở `docs/ops/ads-guide.md`, mục 4. Xem câu hỏi 1 về việc có dựng trên Figma trước không.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Xem thử quảng cáo (chỉ staging) | Xem và kiểm từng loại quảng cáo trên máy thật | KB-1 đến KB-5, KB-8, KB-9 |
| Màn hình mẫu `:feature:sample` | Ví dụ dùng native small cho người viết feature | KB-1 |

## 5. Quảng cáo

**Màn hình chức năng chính (không có quảng cáo):** không áp dụng (dự án mẫu).

| Vị trí | Định dạng | Màn hình | Khi nào hiển thị | Giới hạn tần suất | Khóa bật tắt |
| --- | --- | --- | --- | --- | --- |
| `sample_bottom_banner` (đã có) | đổi sang native small (câu hỏi 3) | Màn hình mẫu | Khi vào màn hình | Không | `ads_sample_bottom_banner_enabled` |
| Các vị trí của màn hình xem thử | mọi định dạng | Xem thử (chỉ staging) | Theo nút bấm | Như production | Bật sẵn ở staging |

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| Thời điểm lần hiện quảng cáo toàn màn hình gần nhất | Khoảng cách tối thiểu qua các phiên | Tới khi gỡ app hay xóa dữ liệu |
| Thời điểm mở app lần đầu | Ân hạn phiên đầu | Như trên |
| Số quảng cáo toàn màn hình trong phiên hiện tại | Giới hạn mỗi phiên | Hết phiên |

**Sự kiện phân tích:** không thêm. `ad_impression`, `ad_click` và doanh thu lấy từ liên kết AdMob và Firebase (ADR-0022).

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ tính năng mới. Khóa Remote Config mới (mặc định trong ngoặc):

| Khóa | Kiểu | Mặc định | Ý nghĩa |
| --- | --- | --- | --- |
| `ads_enabled` | boolean | bật | Công tắc tổng |
| `ads_first_session_grace_seconds` | long | 180 | Ân hạn phiên đầu |
| `ads_interstitial_max_per_session_count` | long | 4 | Tối đa quảng cáo toàn màn hình mỗi phiên |
| `ads_inlist_every_count` | long | 8 | Native trong danh sách cách nhau |
| `ads_native_load_timeout_seconds` | long | 10 | Thời gian chờ trước khi thu chỗ khi không có quảng cáo |

`ads_interstitial_min_interval_seconds` (đã có, 45) áp dụng chung cho interstitial và native toàn màn hình.

## 9. Câu hỏi còn mở

1. **Có dựng component native trên Figma trước khi code không?**
   - Đề xuất của AI: có, trong file thư viện của dự án mẫu: 5 component theo token hiện có, sáng và tối, theo `docs/ops/design-workflow.md`. Screenshot test giữ giao diện sau đó. Nếu không, AI dựng thẳng trong code theo quy chuẩn ở `ads-guide.md`, người duyệt trên ảnh screenshot.
   - Trả lời: Có.
2. **Có làm màn hình xem thử quảng cáo chỉ ở staging không?**
   - Đề xuất của AI: có. Đây là cách duy nhất kiểm đủ 5 loại native và mediation trên máy thật trước khi một app dùng; nó không vào bản production.
   - Trả lời: Có.
3. **Vị trí mẫu `sample_bottom_banner` đổi sang native small không?**
   - Đề xuất của AI: giữ banner (để có ví dụ banner) và thêm vị trí mẫu `sample_native_small`, vì `CLAUDE.md` yêu cầu feature làm theo `:feature:sample`.
   - Trả lời: Có đổi sang native small. Bỏ banner.
4. **Native trong danh sách: chỉ small, hay cả medium?**
   - Đề xuất của AI: cả hai, chọn theo vị trí khi khai báo (lưới ảnh hợp medium, danh sách chữ hợp small).
   - Trả lời: Cả 2.
5. **Thêm adapter AppLovin là đổi `gradle/libs.versions.toml` (file được bảo vệ).**
   - Đề xuất của AI: ticket riêng, plan rủi ro cao, duyệt chéo; các ticket khác không phụ thuộc nó.
   - Trả lời: Làm luôn.

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu |
| 2026-10-21 | Nhật | Trả lời 5 câu hỏi, xác nhận |
| 2026-10-21 | AI | Tách ticket T-007 đến T-015. Hiểu câu 3 là màn hình mẫu bỏ banner, định dạng banner vẫn giữ trong `:core:ads` |
