---
feature: ads
status: confirmed                # draft | confirmed | changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-native-va-remote-config.md
design: features/ads/design/README.md
created: 2026-10-21
confirmed: 2026-10-26
---

# SPEC: QUẢNG CÁO CỦA DỰ ÁN MẪU (NATIVE, REMOTE CONFIG, HÀNH VI THEO ADSKIT)

<!--
Đây là phần nền của dự án mẫu (module core), không phải một màn hình cho người dùng cuối. "Người dùng" trong kịch bản
là người dùng app tạo từ dự án mẫu, hoặc người vận hành app (bật tắt quảng cáo trên Firebase).
Quyết định: ADR-0026 (hành vi theo AdsKit PARTNER, thay phần lớn ADR-0022), ADR-0027 (AppsFlyer), ADR-0023 (Remote Config).
Luật chi tiết: docs/ops/ads-business-rules.md. Hướng dẫn: docs/ops/ads-guide.md, docs/ops/remote-config.md.
Thay đổi 2026-10-23: tiêu chí bị thay giữ nguyên chữ cũ, gạch ngang, kèm dòng "Đổi 2026-10-23". Kịch bản mới từ KB-11.
Cũng ngày 2026-10-23 (lượt 2): theo bản đặc tả độc lập của AdsKit và mô hình chung (ads-business-rules.md, mục Mô hình chung); sửa thẳng chữ của lượt 1 vì chưa xác nhận.
-->

## 1. Mục tiêu

- **Người dùng được gì:** ~~quảng cáo trong app giống giao diện app, không đặt ở chỗ dễ bấm nhầm, không xuất hiện dày đặc hay lúc vừa mở app; người vận hành bật tắt và chỉnh tần suất từ xa, thử được quảng cáo trên bản staging trước khi phát hành.~~ **Đổi 2026-10-23:** quảng cáo trong mọi app của nhóm chạy theo cùng một cách đã có doanh thu tốt ở các app dùng AdsKit; quảng cáo vẫn có nhãn rõ, không giả làm nút của app; người vận hành bật tắt từng loại quảng cáo từ xa bằng cùng bộ khóa với các app AdsKit, thấy doanh thu theo từng vị trí, thử được quảng cáo trên bản staging trước khi phát hành.
- **Vì sao làm bây giờ:** app Camera Pose cần native và mức quảng cáo vừa; làm ở dự án mẫu để mọi app dùng chung, không lệch nhau. **Thêm 2026-10-23:** người phụ trách sản phẩm quyết định hành vi theo bộ PARTNER của AdsKit (ADR-0026) để mọi app có doanh thu như các app đang chạy.
- **Đo kết quả bằng gì:** mỗi app bật quảng cáo theo hướng dẫn mà không phải sửa module core; ~~không có cảnh báo chính sách AdMob hay Play về vị trí đặt quảng cáo;~~ ARPDAU và doanh thu theo vị trí ngang các app AdsKit cùng loại; không bị giới hạn phân phát quảng cáo; ARPDAU theo dõi cùng giữ chân D7 và điểm Play trong báo cáo vận hành.

## 2. Phạm vi

**Trong phạm vi:**

- Năm loại native: small, medium, trong danh sách, thu gọn được, toàn màn hình. **Đổi 2026-10-23:** kích thước và hành vi theo AdsKit: thanh đáy (native nhỏ hoặc banner, lớp native thu gọn), native sau inter, native ô lưới và trang xem trước, native giới thiệu, native medium.
- ~~Giới hạn tần suất lưu qua các phiên, ân hạn phiên đầu, giới hạn số quảng cáo toàn màn hình mỗi phiên, công tắc tổng.~~ **Đổi 2026-10-23:** khoảng cách giữa hai interstitial theo AdsKit (trong một lần mở app); giữ công tắc tổng, khóa `is_show_ads` (câu hỏi 7).
- ~~Mediation AdMob và AppLovin.~~ **Đổi 2026-10-23:** mediation AdMob và chín mạng: AppLovin, Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads.
- Remote Config: kiểu số thực và chuỗi, bảng khóa sinh tự động, giá trị cục bộ cho bản staging. **Thêm 2026-10-23:** tên khóa quảng cáo giống hệt AdsKit.
- Một màn hình chỉ có ở bản staging để xem thử mọi loại quảng cáo (câu hỏi 2).
- **Thêm 2026-10-23:** đồng ý quảng cáo và khởi động sớm theo AdsKit; app open lúc mở app (lần đầu sau khi cài và lần thường) và khi quay lại app; interstitial nối native toàn màn hình; banner làm mới theo AdMob, banner thu gọn; rewarded với hộp thoại; native trên luồng giới thiệu; doanh thu theo vị trí ở Firebase và AppsFlyer; chặn quảng cáo tự hiện khi app mở màn của hệ thống; nạp lại khi lỗi và khi có mạng.

**Ngoài phạm vi lần này:**

- ~~App open, rewarded.~~ **Đổi 2026-10-23:** nay trong phạm vi.
- Vị trí quảng cáo của từng app (mỗi app tự khai báo theo spec của app).
- ~~Mạng quảng cáo khác ngoài AdMob và AppLovin.~~ **Đổi 2026-10-23:** mạng ngoài chín mạng trên.
- **Thêm 2026-10-23:** bộ hành vi `CURRENT` của AdsKit và việc đổi bộ hành vi từ xa; luồng giới thiệu của dự án mẫu (câu hỏi 11); vẽ lại thiết kế trên Figma trong đợt này (làm ở ticket riêng, câu hỏi 16).

## 3. Kịch bản sử dụng

### KB-1. Native small thay banner

Một màn hình của app có vị trí native small đã bật.

Tiêu chí chấp nhận:

- [ ] Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices; vùng quảng cáo có nền hoặc viền tách khỏi nội dung; CTA khác nút chính của app.
- [ ] ~~Khi đang tải thì vùng quảng cáo giữ đúng chiều cao với hiệu ứng chờ, nội dung bên dưới không nhảy.~~ **Đổi 2026-10-23:** khi native nhỏ ở đáy thì vùng quảng cáo cao 80 dp; khi đang tải thì giữ đúng chiều cao đó với hiệu ứng chờ.
- [ ] ~~Khi không có quảng cáo sau thời gian chờ thì vùng quảng cáo thu lại; nếu vị trí được khai báo là cạnh nút của app thì giữ khoảng trống để nút không dịch.~~ **Đổi 2026-10-23:** khi nạp lỗi thì vùng quảng cáo co về 0 dp, nội dung không chừa khoảng trống cho nó.
- [ ] ~~Khi app ở chế độ tối thì quảng cáo dùng màu tối của hệ thống thiết kế.~~ **Đổi 2026-10-23:** màu lấy từ hệ thống thiết kế; chữ quảng cáo luôn đọc được trên nền của vùng quảng cáo, kể cả khi máy ở chế độ tối.
- [ ] **Thêm 2026-10-23:** khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ.

Trường hợp đặc biệt:

- [ ] Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ.
- [ ] Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ.

### KB-2. Native medium thay MREC

Tiêu chí chấp nhận:

- [ ] Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt.
- [ ] Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1.
- [ ] **Thêm 2026-10-23:** khi hai ô medium cùng hiện trên màn thì mỗi ô có một quảng cáo khác nhau.

### KB-3. Native trong danh sách

Một danh sách hay lưới có vị trí native trong danh sách.

Tiêu chí chấp nhận:

- [ ] Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục (N chỉnh từ xa, mặc định 8), không bao giờ ở vị trí đầu tiên. **Đổi 2026-10-23:** N là tham số của vị trí, khóa `native_every_<mã vị trí>` (câu hỏi 19, 28).
- [ ] Khi danh sách ít hơn N mục thì không có quảng cáo.
- [ ] Mỗi ô quảng cáo có nhãn "Ad" rõ, không thể nhầm với một mục của danh sách.
- [ ] **Thêm 2026-10-23:** mỗi ô quảng cáo có một quảng cáo riêng; khi không còn quảng cáo nào để lấy thì ô không hiện, không để khung chờ mãi.

### KB-4. Native thu gọn được

Tiêu chí chấp nhận:

- [ ] ~~Khi vào màn hình thì quảng cáo ở dạng mở (như medium) ở đáy màn hình, có nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices.~~ **Đổi 2026-10-23:** khi vào màn hình thì quảng cáo mở ở đáy, cao 250 dp, nổi trên thanh đáy, có nút thu gọn 32 dp ở góc trên phải; chỉ lấp chiều rộng, không che thanh điều hướng của app.
- [ ] ~~Khi người dùng chạm nút thu gọn thì quảng cáo chuyển sang dạng gọn (như small).~~ **Đổi 2026-10-23:** khi người dùng chạm nút thu gọn thì lớp quảng cáo ẩn, lộ thanh đáy (native nhỏ 80 dp) bên dưới.
- [ ] ~~Khi đã thu gọn thì quảng cáo không tự mở lại trong lúc người dùng còn ở màn hình đó, kể cả khi tải quảng cáo mới.~~ **Đổi 2026-10-23:** khi đã thu gọn thì quảng cáo tự mở lại sau số giây đặt từ xa (mặc định 30 giây, 0 là không mở lại), đếm từ lúc người dùng thu gọn; nội dung được thay mỗi 60 giây mà không đổi trạng thái thu gọn.

### KB-5. Native toàn màn hình thay interstitial

Một điểm dừng tự nhiên trong app (ví dụ sau khi lưu xong một ảnh) gọi quảng cáo toàn màn hình.

**Đổi 2026-10-23:** native toàn màn hình nay hiện liền sau interstitial (KB-16), không thay interstitial. Các tiêu chí dưới thay bằng KB-16.

Tiêu chí chấp nhận:

- [ ] ~~Khi đủ điều kiện tần suất thì quảng cáo hiện toàn màn hình với nút đóng thấy và bấm được ngay, ít nhất 48 dp, ở góc khác AdChoices.~~ **Đổi 2026-10-23:** xem KB-16 (đếm ngược 3 giây, nút đóng 36 dp).
- [ ] Khi bấm nút đóng hoặc nút quay lại của máy thì quảng cáo đóng và người dùng về đúng chỗ đang làm. **Đổi 2026-10-23:** trong 3 giây đếm ngược nút quay lại không làm gì, sau đó đóng như nút đóng (câu hỏi 17).
- [ ] ~~Khi vừa có một quảng cáo toàn màn hình (interstitial hay native) thì không hiện thêm cái nào cho tới hết khoảng cách tối thiểu.~~ **Đổi 2026-10-23:** xem KB-15.
- [ ] Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng.

### KB-6. Tần suất qua các phiên

**Đổi 2026-10-23:** bộ PARTNER không có các luật này (ADR-0026, câu hỏi 10). Thay bằng KB-15.

Tiêu chí chấp nhận:

- [ ] ~~Khi người dùng đóng app rồi mở lại ngay thì khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình vẫn được tính từ lần hiện trước.~~ **Đổi 2026-10-23:** khoảng cách chỉ tính trong một lần mở app (KB-15).
- [ ] ~~Khi người dùng mở app lần đầu thì không có quảng cáo toàn màn hình trong thời gian ân hạn (mặc định 180 giây).~~ **Đổi 2026-10-23:** không có ân hạn; lần mở đầu có app open khi bật `show_open_ads_first_open` (KB-13).
- [ ] ~~Khi đã đủ số quảng cáo toàn màn hình tối đa của phiên (mặc định 4) thì không hiện thêm cho tới phiên sau.~~ **Đổi 2026-10-23:** không có giới hạn mỗi phiên.

### KB-7. Công tắc tổng

**Đổi 2026-10-23:** giữ công tắc tổng, khóa `is_show_ads` (boolean, mặc định bật), tắt cả app open và resume (câu hỏi 7).

Tiêu chí chấp nhận:

- [ ] Khi người vận hành tắt ~~`ads_enabled`~~ công tắc tổng trên Firebase thì ở lần cập nhật cấu hình tiếp theo app không còn tải hay hiện quảng cáo nào, ở mọi vị trí.
- [ ] Khi bật lại thì các vị trí đang bật hoạt động lại mà không cần cập nhật app.

### KB-8. Thử quảng cáo trên bản staging

Tiêu chí chấp nhận:

- [ ] Khi mở bản staging thì mọi loại quảng cáo hiện được bằng mã thử của Google, không cần project Firebase thật.
- [ ] Khi mở màn hình xem thử quảng cáo (chỉ có ở staging) thì thấy từng loại native, nút gọi interstitial và native toàn màn hình, và mở được Ad Inspector của AdMob. **Thêm 2026-10-23:** có thêm nút gọi app open, rewarded với hộp thoại, combo inter rồi native, thanh đáy, native giới thiệu, ô lưới.
- [ ] Bản production không có màn hình xem thử và không có giá trị cục bộ.

### KB-9. Mediation ~~AdMob và AppLovin~~ AdMob và chín mạng

Tiêu chí chấp nhận:

- [ ] Khi đơn vị quảng cáo có AppLovin trong nhóm mediation trên AdMob thì app nhận được quảng cáo của AppLovin mà không đổi code. **Đổi 2026-10-23:** đúng với cả chín mạng: AppLovin, Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads.
- [ ] Khi mở Ad Inspector trên staging thì thấy adapter AppLovin đã khởi tạo. ~~**Đổi 2026-10-23:** thấy cả chín adapter đã khởi tạo.~~ **Đổi 2026-10-24 (plan T-034, câu hỏi 1):** khi mở staging thì log trạng thái (và Ad Inspector khi mở được) liệt kê đủ chín adapter; mã app thử của Google không có cấu hình mediation nên trên staging adapter báo `NOT_READY`.
- [ ] **Thêm 2026-10-24:** khi một app có mã AdMob thật và nhóm mediation đủ chín mạng thì Ad Inspector thấy cả chín adapter đã khởi tạo.
- [ ] **Thêm 2026-10-23:** khóa SDK của AppLovin không nằm trong repo; bản staging chạy được khi không có khóa đó. **Đổi 2026-10-24 (plan T-034, câu hỏi 2):** khóa cũng không nằm trong app; chỉ nhập trong ánh xạ nguồn quảng cáo "AppLovin (Bidding)" trên AdMob.

### KB-10. Bảng khóa Remote Config

Tiêu chí chấp nhận:

- [ ] Khi thêm một khóa trong code mà chưa sinh lại bảng khóa thì kiểm tra tự động báo lỗi.
- [ ] Bảng khóa liệt kê mọi khóa với kiểu, giá trị mặc định, module và mô tả; có file mẫu nhập được vào Firebase.
- [ ] Module core đọc được khóa kiểu số thực và chuỗi.
- [ ] **Thêm 2026-10-23:** mọi khóa quảng cáo có tên giống hệt bảng khóa ở mục 8 (tên AdsKit, kể cả `show_resum_ads`).

### KB-11. Đồng ý quảng cáo và khởi động sớm

Người dùng mở app. Ở vùng có luật bảo vệ dữ liệu, app phải hỏi đồng ý trước khi có quảng cáo.

Tiêu chí chấp nhận:

- [ ] Khi người dùng đã đồng ý ở lần mở trước thì quảng cáo bắt đầu tải ngay lúc mở app, không chờ hộp thoại đồng ý cập nhật xong.
- [ ] Khi người dùng ở ngoài vùng có luật thì không có hộp thoại, quảng cáo bắt đầu tải gần như ngay.
- [ ] Khi việc hỏi đồng ý lỗi vì mạng và app đang mở trên màn hình thì quảng cáo vẫn được tải; khi mạng về hay app được mở lại thì app hỏi lại đồng ý.
- [ ] Khi app ở nền thì không có yêu cầu quảng cáo nào dựa trên quyền tạm khi lỗi mạng.
- [ ] Khi việc hỏi lại cho biết cần hỏi người dùng thì app dừng tải và không hiện quảng cáo đã tải bằng quyền tạm, cho tới khi người dùng trả lời.
- [ ] Khi app chưa bật quyền tạm (mặc định tắt, cả cấu hình của app lẫn công tắc từ xa) thì quảng cáo chỉ tải khi hộp thoại đồng ý cho phép; lỗi mạng của việc hỏi đồng ý không cho tải.
- [ ] Khi người vận hành tắt quyền tạm từ xa thì quyền tạm khi lỗi mạng bị thu hồi ngay; quyền người dùng đã cho vẫn giữ.
- [ ] Khi quyền tạm tắt mà hộp thoại đồng ý lỗi sau khi người dùng đã cho phép thì kết quả được giữ cho cả lần mở app, không hỏi lại.

Trường hợp đặc biệt:

- [ ] Khi hộp thoại đồng ý đang mở thì không có quảng cáo khi quay lại app.
- [ ] Khi hộp thoại bị ngắt (xoay máy, đóng màn) thì lần sau app hỏi lại, không coi là người dùng đã trả lời.

### KB-12. Rút đồng ý

Tiêu chí chấp nhận:

- [ ] Khi người dùng rút đồng ý qua nút quyền riêng tư thì không còn yêu cầu quảng cáo mới nào và quảng cáo đã tải không hiện nữa.
- [ ] Khi người dùng đồng ý lại thì quảng cáo tải lại mà không cần mở lại app.

### KB-13. Quảng cáo khi mở app

Người dùng mở app (khởi động mới).

Tiêu chí chấp nhận:

- [ ] Khi là lần mở đầu sau khi cài và người vận hành bật quảng cáo lần đầu thì màn loading hiện quảng cáo mở app nếu quảng cáo về kịp.
- [ ] Khi là lần mở thứ hai trở đi và người vận hành bật quảng cáo mở app thì màn loading hiện quảng cáo mở app nếu quảng cáo về kịp.
- [ ] Khi người vận hành chọn loại "inter" thì lúc mở app hiện interstitial thay cho quảng cáo mở app.
- [ ] Khi quảng cáo không về trong thời gian chờ (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây) thì app vào màn chính, không hiện gì.
- [ ] Khi khóa tắt thì màn loading không chờ quảng cáo.

### KB-14. Quảng cáo khi quay lại app

Người dùng chuyển sang app khác rồi quay lại.

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật quảng cáo khi quay lại thì mỗi lần app trở lại màn hình có quảng cáo mở app (hoặc interstitial nếu chọn loại "inter", có thể nối native toàn màn hình).
- [ ] Khi người dùng vừa bấm vào một quảng cáo rồi quay lại thì lần quay lại đó không có quảng cáo.
- [ ] Khi người dùng quay lại từ màn chọn ảnh, camera, chia sẻ hay hộp thoại đồng ý mà app tự mở thì không có quảng cáo.
- [ ] Khi đang có một quảng cáo toàn màn hình, hoặc quảng cáo mở app vừa đóng chưa tới 1 giây, thì không hiện thêm.
- [ ] Khi quảng cáo chưa sẵn sàng thì lần này không hiện, app tải sẵn cho lần sau.
- [ ] Lần mở app đầu tiên (khởi động mới) không tính là quay lại.

### KB-15. Khoảng cách giữa các quảng cáo toàn màn hình

Tiêu chí chấp nhận:

- [ ] Khi một interstitial (hay cả cụm interstitial và native) vừa đóng thì interstitial sau chỉ hiện khi đã qua `ads_interval` giây (mặc định 45).
- [ ] Khi người dùng đổi giờ của máy thì khoảng cách vẫn tính đúng.
- [ ] Khi mở lại app (khởi động mới) thì khoảng cách bắt đầu lại từ đầu.
- [ ] Không bao giờ có hai quảng cáo toàn màn hình chồng nhau; lượt bị chặn thì bỏ, không xếp hàng.
- [ ] Khi quảng cáo toàn màn hình bị kẹt (không báo đóng) mà người dùng đã về lại app thì app tự gỡ kẹt và người dùng đi tiếp được.

### KB-16. Interstitial rồi native toàn màn hình

Người dùng bấm một nút có interstitial (ví dụ sang màn sau).

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật native sau inter thì interstitial đóng xong có ngay một native toàn màn hình.
- [ ] Màn native có đếm ngược 3 giây trong vòng 36 dp ở ~~góc trên phải~~ **đổi 2026-10-26:** góc trên trái, theo thiết kế T-035 (góc trên phải dành cho AdChoices; câu hỏi 10 của plan T-027); hết đếm ngược thì vòng đó thành nút đóng.
- [ ] Khi native không tải được thì nút đóng dùng được ngay và màn tự đóng khi hết đếm ngược.
- [ ] Khi đóng màn native thì người dùng đi tiếp tới màn đã chọn, và khoảng cách `ads_interval` tính từ lúc này.
- [ ] Khi khóa tắt thì interstitial đóng xong người dùng đi tiếp ngay.
- [ ] Khi interstitial bị chặn ngay từ đầu (chưa có, chưa qua khoảng cách, đang có quảng cáo khác) thì người dùng đi tiếp ngay, không có native.
- [ ] Khi interstitial đã được phép hiện nhưng không lên được màn (mất quảng cáo, lỗi hiển thị) và native sau inter đang bật thì vẫn có màn native toàn màn hình.

### KB-17. Thanh quảng cáo ở đáy màn hình

Tiêu chí chấp nhận:

- [ ] Khi bật native ở đáy thì đáy màn hình có native nhỏ 80 dp và lớp native thu gọn 250 dp nổi trên (KB-4).
- [ ] Khi người vận hành tắt riêng lớp thu gọn thì native nhỏ ở đáy vẫn còn.
- [ ] Khi đang chờ quảng cáo đầu tiên thì khe native nhỏ hiện khung chờ; khi tải lỗi thì khe co về 0.
- [ ] Khi tắt native ở đáy thì đáy màn hình có banner.
- [ ] Nội dung của màn hình chừa đúng chiều cao của thanh đáy khi thanh có quảng cáo, không chừa thừa hay thiếu.
- [ ] Doanh thu và lượt bấm của thanh đáy tính cho màn hình đang hiện, không cho màn hình đầu tiên.

### KB-18. Banner

Tiêu chí chấp nhận:

- [ ] Khi đơn vị banner bật tự làm mới trên AdMob thì banner tự đổi theo nhịp của AdMob, app không tự tải lại.
- [ ] Khi app ở nền thì banner không tải lại hay thử lại.
- [ ] Khi dùng banner thu gọn thì mỗi lần làm mới là một banner bung ra mới.
- [ ] Khi chưa có đồng ý thì banner không gửi yêu cầu.
- [ ] Khi người dùng rút đồng ý lúc banner đang hiện thì khung banner giữ nguyên chiều cao, giao diện không nhảy; đồng ý lại thì banner tải lại trong khung đó.
- [ ] Banner thu gọn được tải lại sau mỗi 30 giây, không dùng tự làm mới của AdMob.

### KB-19. Quảng cáo có thưởng trước một hành động có điều kiện

Người dùng chọn một hành động mà spec của app gắn với quảng cáo có thưởng (ví dụ nhận vật phẩm, xuất file, mở khóa).

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật vị trí rewarded của hành động đó thì hộp thoại hỏi xem quảng cáo hiện, có nút "Xem quảng cáo" và "Không, cảm ơn".
- [ ] Khi vị trí tắt thì hành động làm ngay, không có hộp thoại.
- [ ] Khi người dùng xem đủ thì nhận thưởng (hành động được làm) ngay lúc quảng cáo báo thưởng, không chờ đóng.
- [ ] Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí: mặc định bị chặn và người dùng thử lại được; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng.
- [ ] Khi người dùng đóng quảng cáo trước khi đủ thì không có thưởng, hành động bị chặn.
- [ ] ~~Khi bấm "Xem quảng cáo" hai lần liền thì chỉ một quảng cáo được gọi.~~ **Đổi 2026-10-23 (lượt 2):** khi bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì app làm như AdsKit (không chặn) và số liệu ghi lần bấm đó là "bận" (`busy`).
- [ ] Rewarded không bị chặn bởi khoảng cách `ads_interval` và không làm dời khoảng cách đó.
- [ ] Hai app khác nhau gắn rewarded vào hai hành động khác nhau (nhận vật phẩm, xuất file) mà không sửa module core.

### KB-20. Native ở một bước của luồng

Một app có luồng nhiều bước (giới thiệu, kết quả sau khi xử lý, hướng dẫn). Spec của app chọn bước nào có native.

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật vị trí native của luồng thì bước mà spec của app chọn có native cao 250 dp hoặc native toàn trang.
- [ ] Hai bước kề nhau không hiện cùng một quảng cáo.
- [ ] Native toàn trang không có nút điều khiển riêng; nút Tiếp của app vẫn ở chỗ quen thuộc và không bị quảng cáo che.
- [ ] Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống.
- [ ] Luồng giới thiệu dùng được khóa `is_show_native_onboarding` như các app AdsKit; luồng khác dùng khóa theo mã vị trí của nó.

### KB-21. Native trong tập hợp (lưới, danh sách, trang của pager)

Tiêu chí chấp nhận:

- [ ] Mỗi ô quảng cáo trong lưới, danh sách hay trang của pager có một quảng cáo riêng, dạng thẻ dọc hoặc medium theo khai báo của vị trí.
- [ ] Khi không còn quảng cáo thì ô không hiện (hoặc giữ khung chờ nếu vị trí khai báo giữ chỗ); quảng cáo về muộn vẫn hiện vào ô.
- [ ] Ô chỉ tính là đã được xem khi trang của nó đứng yên trên màn hình.
- [ ] Một quảng cáo người dùng đã thấy không hiện lại ở ô khác khi ô cũ rời màn.

### KB-22. Ghi doanh thu theo vị trí

Người vận hành xem doanh thu quảng cáo.

Tiêu chí chấp nhận:

- [ ] Mỗi lượt hiển thị quảng cáo ghi một sự kiện doanh thu theo vị trí ở Firebase (`ad_impression_placement`) và ở AppsFlyer (`af_ad_revenue`), với định dạng, mạng, giá trị, vị trí.
- [ ] Doanh thu không bị tính hai lần: app không ghi `ad_impression` và không gửi `af_revenue`.
- [ ] Native hiện mà mạng không báo doanh thu trong 2 giây thì vẫn có một lượt ghi giá trị 0.
- [ ] Khi người vận hành bật số liệu chi tiết thì có thêm sự kiện tải, cơ hội hiển thị, lỗi hiển thị, lượt bấm.
- [ ] Bản staging không gửi gì sang AppsFlyer.
- [ ] Khi người vận hành tắt một sự kiện AppsFlyer từ xa thì sự kiện đó ngừng gửi từ lượt kế tiếp, các sự kiện khác vẫn gửi.

### KB-23. Không làm phiền khi app mở màn của hệ thống

Tiêu chí chấp nhận:

- [ ] Khi app mở màn chọn ảnh, camera, hộp thoại quyền, chia sẻ hay đường link rồi người dùng quay lại thì không có quảng cáo khi quay lại.
- [ ] Khi một màn hình của app đang chiếm toàn màn (native sau inter, trang native giới thiệu) thì không có quảng cáo khi quay lại.
- [ ] Khi việc mở màn kia thất bại thì quảng cáo khi quay lại hoạt động lại từ lần sau.

### KB-24. Nạp lại khi lỗi và khi có mạng

Tiêu chí chấp nhận:

- [ ] Khi tải quảng cáo toàn màn hình lỗi thì app thử lại sau 2, 4, 6, 8 rồi 10 giây mỗi lần, không bỏ cuộc.
- [ ] Khi tải native lỗi thì app thử lại với khoảng chờ tăng dần tới 60 giây; chưa có native nào thì thử lại sau 5 giây.
- [ ] Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại.
- [ ] Khi app ở nền thì native không gửi yêu cầu.

### KB-25. Thêm vị trí quảng cáo cho một app theo mô hình chung

Dev của một app tạo từ dự án mẫu thêm quảng cáo cho một tính năng mới của app (ví dụ rewarded trước khi xuất file, inter ở màn âm thanh với khoảng cách riêng).

Tiêu chí chấp nhận:

- [ ] Khi dev khai báo vị trí trong spec của app và trong danh sách vị trí (mã, định dạng, thuộc tính) thì vị trí dùng được mà không sửa module core.
- [ ] Khi khai báo vị trí thì khóa bật tắt `is_show_<mã vị trí>` có trong bảng khóa sinh tự động, mặc định tắt.
- [ ] Khi vị trí inter khai báo nhóm giãn cách riêng thì khóa `ads_interval_<nhóm>` có trong bảng khóa, và inter của nhóm đó không làm dời khoảng cách của nhóm mặc định.
- [ ] Khi chọn mã vị trí trùng tên AdsKit (`rw_get_item`, `native_onboarding`, nhóm `sounds`) thì khóa trùng đúng chuỗi khóa mà app AdsKit đang dùng trên Firebase.
- [ ] Doanh thu của vị trí ghi với `ad_placement` bằng mã vị trí.

## 4. Giao diện

- **Thiết kế:** các cỡ native là component của hệ thống thiết kế, dựng theo token có sẵn (màu, chữ, bo góc, khoảng cách) ~~và quy chuẩn ở `docs/ops/ads-guide.md`, mục 4~~. **Đổi 2026-10-23:** kích thước theo AdsKit (`docs/ops/ads-business-rules.md`, mục 5.7), màu theo token. Thiết kế hiện có trong `design/` theo cỡ cũ; vẽ lại ở T-035 (câu hỏi 16). Xem câu hỏi 1 về việc dựng trên Figma trước.
- **Các màn hình:**

| Màn hình | Mục đích | Kịch bản liên quan |
| --- | --- | --- |
| Xem thử quảng cáo (chỉ staging) | Xem và kiểm từng loại quảng cáo trên máy thật | KB-1 đến KB-5, KB-8, KB-9, KB-13, KB-14, KB-16 đến KB-21 |
| Màn hình mẫu `:feature:sample` | Ví dụ dùng ~~native small~~ thanh đáy (đổi 2026-10-23) cho người viết feature | KB-1, KB-17 |
| Màn loading (`:feature:loading`) | Chờ đồng ý, SDK và quảng cáo mở app (câu hỏi 11, 12) | KB-11, KB-13 |
| Native sau inter | Native toàn màn hình có đếm ngược | KB-16 |
| Hộp thoại xem quảng cáo có thưởng | Hỏi người dùng có xem không | KB-19 |

## 5. Quảng cáo

**Màn hình chức năng chính (không có quảng cáo):** không áp dụng (dự án mẫu).

| Vị trí | Định dạng | Màn hình | Khi nào hiển thị | Giới hạn tần suất | Khóa bật tắt |
| --- | --- | --- | --- | --- | --- |
| `sample_bottom_banner` (đã có) | đổi sang native small (câu hỏi 3). **Đổi 2026-10-23:** thanh đáy (native nhỏ và lớp thu gọn, hoặc banner) | Màn hình mẫu | Khi vào màn hình | Không. Thay nội dung mỗi 60 giây | ~~`ads_sample_bottom_banner_enabled`~~ `is_show_sample_bottom_banner`, cùng `is_show_native_bottom`, `time_expand_collap_ad` |
| Các vị trí của màn hình xem thử | mọi định dạng | Xem thử (chỉ staging) | Theo nút bấm | Như production | Bật sẵn ở staging |
| **Thêm 2026-10-23:** quảng cáo mở app (màn loading, câu hỏi 11) | app open hoặc interstitial | Màn loading | Lúc khởi động mới | Không bị `ads_interval` chặn | `show_open_ads_first_open`, `show_open_ads`, `open_ad_type` |
| **Thêm 2026-10-23:** `resume` (`:core:ads`, câu hỏi 11). **Đổi 2026-10-26:** hai vị trí toàn app, `resume` (app open) và `resume_inter` (interstitial, không bị `ads_interval` chặn, vẫn đẩy mốc), vì mã đơn vị đi theo vị trí (plan T-025, câu hỏi 1) | app open hoặc interstitial (có thể nối native) | Mọi màn | Khi quay lại app | Theo KB-14 | `show_resum_ads`, `resume_ad_type`, `is_show_native_after_inter` |
| **Thêm 2026-10-26:** `native_combo` (`:core:ads`, vị trí toàn app, plan T-026, câu hỏi 1) | native toàn màn hình sau interstitial (combo) | Mọi màn | Sau khi interstitial đóng, khi combo bật | Theo KB-16 | `is_show_native_after_inter` (không có `is_show_<mã>`) |

## 6. Dữ liệu và phân tích

**Dữ liệu lưu trên máy:**

| Dữ liệu | Mục đích | Giữ trong bao lâu |
| --- | --- | --- |
| ~~Thời điểm lần hiện quảng cáo toàn màn hình gần nhất~~ (đổi 2026-10-23: chỉ giữ trong bộ nhớ của lần mở app, không lưu) | Khoảng cách tối thiểu ~~qua các phiên~~ | ~~Tới khi gỡ app hay xóa dữ liệu~~ Hết lần mở app |
| ~~Thời điểm mở app lần đầu~~ (đổi 2026-10-23: bỏ, không có ân hạn) | ~~Ân hạn phiên đầu~~ | |
| ~~Số quảng cáo toàn màn hình trong phiên hiện tại~~ (đổi 2026-10-23: bỏ, không có giới hạn mỗi phiên) | ~~Giới hạn mỗi phiên~~ | |
| **Thêm 2026-10-23:** dấu "đã mở app lần đầu" | Chọn khóa quảng cáo mở app lần đầu hay lần thường | Tới khi gỡ app hay xóa dữ liệu |
| **Thêm 2026-10-23:** số lần interstitial đã hiện từ lúc cài | Sự kiện `af_inters_displayed_{N}_times` | Tới khi gỡ app hay xóa dữ liệu |

**Sự kiện phân tích:** ~~không thêm. `ad_impression`, `ad_click` và doanh thu lấy từ liên kết AdMob và Firebase (ADR-0022).~~ **Đổi 2026-10-23:** `ad_impression` vẫn lấy từ liên kết AdMob và Firebase; app thêm các sự kiện dưới (tham số đầy đủ ở `docs/ops/ads-business-rules.md`, mục 8).

| Tên sự kiện | Khi nào ghi | Tham số | Dùng để trả lời câu hỏi gì |
| --- | --- | --- | --- |
| `ad_impression_placement` (Firebase) | Mỗi lượt hiển thị có doanh thu, hoặc native sau 2 giây không có doanh thu (giá trị 0) | `ad_format`, `ad_platform`, `ad_source`, `ad_unit_name`, `currency`, `value`, `ad_placement` | Doanh thu theo vị trí và định dạng |
| `ad_load_success`, `ad_load_fail`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, `ad_reward_earned`, `reward_dialog` (Firebase) | Khi khóa số liệu chi tiết của nhóm bật (mặc định tắt) | Theo `ads-business-rules.md`, mục 8.2 | Vì sao một vị trí ít hiển thị: không có quảng cáo, chưa tải xong, bị chặn |
| `af_ad_revenue` (AppsFlyer) | Mỗi lượt hiển thị, kể cả lượt giá trị 0 | `monetization_network`, `mediation_network`, `af_currency`, `af_revenue`, `country`, `ad_placement`, `ad_unit`, `ad_type` | Doanh thu trên chi phí của chiến dịch thu hút người dùng |
| `af_inters_*`, `af_rewarded_*`, `af_native_*`, `af_inters_displayed_{N}_times` (AppsFlyer) | Theo phễu: đủ điều kiện, tải xong, hiển thị; lần hiện thứ N | `ad_format`, `ad_unit_id`, `ad_source` | Phễu quảng cáo cho đối tác và chiến dịch |

**Quyền của app và dịch vụ bên ngoài:**

- Không có quyền nào phải hỏi người dùng.
- SDK bên ngoài mới: mediation AppLovin (adapter `com.google.ads.mediation:applovin` kéo theo AppLovin SDK), ADR-0022. AppLovin chỉ khởi động qua AdMob sau khi người dùng đồng ý quảng cáo: bỏ `AppLovinInitProvider` của SDK (nó đọc mã quảng cáo của máy lúc mở app, trước khi có đồng ý).
- Quyền khai báo trong manifest gộp: `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` (đã có, của Google Mobile Ads); thêm `VIBRATE` (AppLovin SDK dùng cho quảng cáo có rung, quyền thường, không hỏi người dùng). Bỏ `CHECK_LICENSE` và `ACCESS_WIFI_STATE` mà AppLovin SDK tự thêm (không cần cho quảng cáo).
- Play Console: cập nhật mục An toàn dữ liệu với mạng quảng cáo AppLovin (mã quảng cáo của máy, dữ liệu tương tác quảng cáo) trước khi phát hành bản có mediation.
- **Thêm 2026-10-23:** SDK bên ngoài mới: adapter của tám mạng còn lại (Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads, kèm SDK Unity Ads) và SDK AppsFlyer (ADR-0026, ADR-0027). Quyền mà từng SDK kéo vào manifest gộp đo ở plan T-033 và T-034, ghi vào mục này trước khi duyệt plan (câu hỏi 18). ~~Khóa SDK AppLovin và khóa dev AppsFlyer đưa vào lúc build, không vào repo.~~ **Đổi 2026-10-24:** khóa dev AppsFlyer đưa vào lúc build, không vào repo; khóa SDK AppLovin không vào app (KB-9).
- **Thêm 2026-10-26 (đo ở plan T-033, câu hỏi 18):** SDK AppsFlyer `6.18.1` khai báo `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` (đã có) và kéo vào hai quyền đọc mã giới thiệu của kho Samsung, Huawei; bỏ cả hai vì app chỉ phát hành trên Google Play. SDK kéo theo thư viện Play Integrity `com.google.android.play:integrity:1.3.0` (thêm hoạt động `com.google.android.play.core.common.PlayCoreDialogWrapperActivity`, không xuất). Manifest gộp thêm `<queries>` dò gói Instagram, Facebook Lite và intent `com.appsflyer.referrer.INSTALL_PROVIDER` (bỏ gói `com.samsung.android.mapsagent`), quy tắc sao lưu của AppsFlyer; sao lưu của app vẫn tắt. AppsFlyer chỉ khởi động khi có câu trả lời đồng ý (kể cả từ chối), hoặc sau 10 giây mà hộp thoại đồng ý không mở; hộp thoại đang mở thì chờ nó đóng (cách hiểu ADR-0027, quyết định 3; plan T-033, câu hỏi 9).
- **Thêm 2026-10-24 (đo ở plan T-034, câu hỏi 18):** tám adapter (Meta Audience Network `6.22.0.1`, DT Exchange `8.5.0.0`, InMobi `11.5.0.0`, ironSource `9.6.1.0`, Mintegral `17.1.81.1`, Moloco `4.13.0.0`, Pangle `8.3.0.4.0`, Unity Ads `4.21.0.0` kèm SDK Unity Ads `4.21.0`) không thêm quyền nào vào manifest gộp: các quyền chúng khai báo (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`) đã có từ Google Mobile Ads. Không thêm các quyền tùy chọn InMobi gợi ý (vị trí chính xác, trạng thái và đổi Wi-Fi). Mạng nào khởi động SDK lúc mở app thì bỏ phần đó, để mạng chỉ khởi động qua AdMob sau khi có đồng ý: bỏ thành phần khởi động của Meta (cùng cách với AppLovin). Bỏ thành phần nhận sự kiện đổi mạng của Mintegral (app khác gọi được, hệ thống không còn gửi sự kiện này). Giữ các thành phần chỉ theo dõi màn hình đang mở của ironSource, Mintegral, Unity.
- **Thêm 2026-10-23:** Play Console: An toàn dữ liệu khai đủ chín mạng và AppsFlyer (mã quảng cáo và mã thiết bị, tương tác với app và quảng cáo, chẩn đoán) trước khi phát hành bản có các SDK đó.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ tính năng mới. Khóa Remote Config mới (mặc định trong ngoặc):

| Khóa | Kiểu | Mặc định | Ý nghĩa |
| --- | --- | --- | --- |
| `ads_enabled` | boolean | bật | Công tắc tổng. **Đổi 2026-10-23:** đổi tên thành `is_show_ads` (câu hỏi 7, T-019) |
| `ads_first_session_grace_seconds` | long | 180 | Ân hạn phiên đầu. **Đổi 2026-10-23:** bỏ (câu hỏi 10) |
| `ads_interstitial_max_per_session_count` | long | 4 | Tối đa quảng cáo toàn màn hình mỗi phiên. **Đổi 2026-10-23:** bỏ (câu hỏi 10) |
| `ads_inlist_every_count` | long | 8 | Native trong danh sách cách nhau. **Đổi 2026-10-23:** thay bằng `native_every_<mã vị trí>` (câu hỏi 19, T-019) |
| `ads_native_load_timeout_seconds` | long | 10 | Thời gian chờ trước khi thu chỗ khi không có quảng cáo. **Đổi 2026-10-23:** bỏ (câu hỏi 20) |

~~`ads_interstitial_min_interval_seconds` (đã có, 45) áp dụng chung cho interstitial và native toàn màn hình.~~ **Đổi 2026-10-23:** thay bằng `ads_interval`.

**Thêm 2026-10-23:** khóa quảng cáo theo tên AdsKit (bảng đầy đủ, kèm dẫn chứng, ở `docs/ops/ads-business-rules.md`, mục 10):

| Khóa | Kiểu | Mặc định | Ý nghĩa |
| --- | --- | --- | --- |
| `show_open_ads` | boolean | tắt | Quảng cáo mở app từ lần mở thứ hai |
| `show_open_ads_first_open` | boolean | tắt | Quảng cáo mở app ở lần mở đầu sau khi cài |
| `open_ad_type` | string | `aoa` | `aoa` hay `inter` lúc mở app |
| `show_resum_ads` | boolean | tắt | Quảng cáo khi quay lại app |
| `resume_ad_type` | string | `aoa` | `inter` là interstitial, giá trị khác là app open |
| `ads_interval` | long (giây) | 45 | Khoảng cách giữa hai interstitial |
| `is_show_native_after_inter` | boolean | tắt | Native toàn màn hình sau interstitial |
| `is_show_native_bottom` | boolean | tắt | Thanh đáy là native (bật) hay banner (tắt) |
| `is_show_native_collap` | boolean | bật | Lớp native thu gọn trên thanh đáy khi khe đáy là native (tên do ta đặt) |
| `time_expand_collap_ad` | long (giây) | 30 | Native thu gọn tự mở lại sau khi người dùng thu gọn; 0 là không (tên do ta đặt) |
| `is_show_rw_get_item` | boolean | tắt | Khóa suy ra của vị trí rewarded `rw_get_item` (nhận vật phẩm), trùng chuỗi AdsKit. Vị trí rewarded khác dùng `is_show_<mã vị trí>`, ví dụ `is_show_rw_export` |
| `is_show_native_onboarding` | boolean | tắt | Khóa suy ra của vị trí `native_onboarding` (native trên luồng giới thiệu), trùng chuỗi AdsKit |
| `is_show_<mã vị trí>` | boolean | tắt | Bật một vị trí, thay `ads_<vị trí>_enabled` (suy ra theo mẫu) |
| `ads_interval_<nhóm>` | long (giây) | theo spec | Khoảng cách của nhóm giãn cách riêng (suy ra theo mẫu) |
| `<tham số>_<mã vị trí>` | theo tham số | theo spec | Tham số riêng của vị trí (câu hỏi 28) |
| `is_enable_consent_shortcut` | boolean | tắt (câu hỏi 21) | Quyền tạm khi đồng ý chưa trả lời xong; cần thêm cấu hình của app, cũng mặc định tắt (tên do ta đặt) |
| `is_log_ad_fullscreen_events` | boolean | tắt | Số liệu chi tiết của quảng cáo toàn màn hình |
| `is_log_ad_slot_events` | boolean | tắt | Số liệu chi tiết của quảng cáo trong màn hình |
| `is_log_af_*` (11 khóa, `ads-business-rules.md` mục 10.1) | boolean | bật | Công tắc từng sự kiện AppsFlyer (tên do ta đặt) |

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
6. **Quyền và SDK ở mục 6 (bổ sung 2026-10-23 theo rà soát danh sách chạy RUN-20261023-01).**
   - Đề xuất của AI: chấp nhận như mục 6: thêm `VIBRATE`, bỏ `CHECK_LICENSE`, `ACCESS_WIFI_STATE` và `AppLovinInitProvider`; cập nhật An toàn dữ liệu trước khi phát hành.
   - Trả lời: chấp nhận như mục 6.
7. **Có giữ công tắc tổng tắt mọi quảng cáo không? AdsKit không có.**
   - Đề xuất của AI: giữ, đặt theo kiểu AdsKit là `is_show_ads` (boolean, mặc định bật). Tắt thì không tải, không hiện mọi định dạng, kể cả app open và resume. Đây là cách nhanh nhất khi có cảnh báo chính sách; nó không đổi hành vi PARTNER khi bật.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
8. **Khóa quảng cáo cá nhân hóa ở vùng có luật (`ads_personalized_regulated_regions_enabled`, ADR-0014) có giữ không? AdsKit không có.**
   - Đề xuất của AI: bỏ khóa, để chuỗi đồng ý của UMP quyết như AdsKit: người đồng ý nhận quảng cáo cá nhân hóa, người từ chối thì Google đọc lựa chọn từ chuỗi đồng ý.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
9. **Rào chắn "chờ người dùng ngừng chạm 1 giây" trước interstitial (ADR-0013) có giữ không? AdsKit không có.**
   - Đề xuất của AI: bỏ. Interstitial theo AdsKit gọi ngay ở cú bấm của người dùng, nên rào chắn này làm mọi interstitial trễ 1 giây.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
10. **Ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên (KB-6, T-009) bỏ theo PARTNER?**
    - Đề xuất của AI: bỏ như đã ghi ở KB-6; code đợt 2 của T-009 được gỡ ở T-019 và T-022.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
11. **Vỏ app của dự án mẫu làm những luồng nào?**
    - Đề xuất của AI: màn loading làm quảng cáo mở app (lần đầu và lần thường, KB-13); `:core:ads` làm quảng cáo khi quay lại (KB-14). Dự án mẫu không có luồng giới thiệu: chỉ cung cấp native ở bước của luồng trong `:core:ads` và trên màn xem thử; app nào có luồng thì spec của app đặt vị trí. Các việc bắt buộc của `:app` theo bản đặc tả độc lập (HD-01 tới HD-07 của `ads-business-rules.md`: khởi tạo trong `Application`, đặt kế hoạch nạp sẵn trước kết quả đồng ý, hoàn tất cưỡng bức inter, công tắc nhánh rút gọn trước bước đồng ý, bộ chứa lớp phủ, lượt dò không đo, báo nhóm UA) đã có ở T-020 tới T-026 và không cần trả lời ở câu này.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
12. **Thời gian chờ ở màn loading theo PARTNER (đồng ý 10 giây, SDK 35 giây, quảng cáo mở app 40 giây) dài hơn giới hạn chờ Remote Config của ADR-0018. Theo PARTNER?**
    - Đề xuất của AI: theo PARTNER khi khóa quảng cáo mở app bật; khi khóa tắt thì màn loading giữ giới hạn của ADR-0018. Ghi chú vào ADR-0018 khi làm T-024.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
13. **Màn mẫu `:feature:sample` có minh họa một hành động có điều kiện (rewarded) không?**
    - Đề xuất của AI: không đặt trong `:feature:sample`, để màn mẫu giữ đơn giản và không bật quảng cáo nào ngoài spec. Màn xem thử ở staging có một vị trí `gallery_rw_action` minh họa đủ ba chính sách (`block`, `allow`, `allow_after_<n>`), và `ads-guide.md` mục 5 có ví dụ khai báo. Đổi 2026-10-23 (lượt 2): câu hỏi viết lại theo mô hình chung, ý cũ (rewarded trong màn mẫu) giữ nguyên.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
14. **Mã đơn vị quảng cáo theo khe (như AdsKit) hay theo vị trí (như hiện tại)?**
    - Đề xuất của AI: theo khe như AdsKit (`ads-business-rules.md` M-05, tên khe theo bản đặc tả độc lập để mỗi tên một nghĩa: `banner` là tự làm mới, `banner_collapsible` là nạp tay); vị trí chỉ để bật tắt và ghi doanh thu. Ít mã phải tạo hơn, và pool, nạp sẵn dùng chung một khe. Cần sửa `scripts/release.sh` và `release.properties` (việc của người hoặc plan rủi ro cao được duyệt chéo).
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
15. **Khóa dev AppsFlyer xử lý thế nào?**
    - Đề xuất của AI: như ADR-0027: production qua tham số build lúc phát hành; staging và CI không có khóa nên không gửi. Chưa cần app AppsFlyer riêng cho staging; khi cần thì thêm bí mật vào `staging.yml` bằng plan rủi ro cao.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
16. **Vẽ lại thiết kế native trên Figma theo kích thước AdsKit lúc nào?**
    - Đề xuất của AI: ticket T-035, làm trước các ticket code native của đợt 3 (T-027, T-030, T-032). AI dựng component theo kích thước AdsKit với token hiện có (khe đáy 80 dp theo câu 22), người duyệt; ảnh chuẩn screenshot của T-010 tới T-013 ghi lại theo plan của ticket sửa.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
17. **Nút quay lại của máy trên màn native sau inter làm gì? AdsKit không xử lý.**
    - Đề xuất của AI: trong 3 giây đếm ngược thì không làm gì; sau đó đóng như nút đóng. Nạp lỗi thì đóng ngay.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
18. **Tám adapter mới kéo thêm quyền vào manifest. Gỡ quyền không cần như đã làm với AppLovin (`CHECK_LICENSE`, `ACCESS_WIFI_STATE`) không? AdsKit không gỡ quyền nào.**
    - Đề xuất của AI: giữ cách của T-015: plan T-034 liệt kê quyền từng adapter kéo vào, giữ quyền thường mà quảng cáo cần, gỡ quyền không cần cho quảng cáo; đưa danh sách vào mục 6 để anh xác nhận trước khi duyệt plan.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
19. **Native trong danh sách chèn mỗi N mục (KB-3): AdsKit không có khóa này (ô lưới do app đặt). Giữ không, và tên khóa là gì?**
    - Đề xuất của AI: giữ, nhưng là tham số của từng vị trí trong tập hợp theo mô hình chung: `native_every_<mã vị trí>` (số mục, mặc định 8 hay theo spec của app), thay `ads_inlist_every_count`. Mẫu tên theo câu 28.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
20. **Khóa `ads_native_load_timeout_seconds` có giữ không? AdsKit co khe về 0 ngay khi nạp lỗi; native thu gọn chờ cố định 10 giây.**
    - Đề xuất của AI: bỏ, theo AdsKit.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
21. **Công tắc quyền tạm `is_enable_consent_shortcut` mặc định bật hay tắt? AdsKit mặc định tắt, app bật từ xa.**
    - Đề xuất của AI: mặc định bật, vì anh đã chọn khởi động sớm theo PARTNER; tắt từ xa khi có vấn đề về đồng ý.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Mặc định tắt, như bản đặc tả và mã nguồn; nhánh rút gọn cần cả cấu hình lúc build của `:app` và công tắc từ xa cùng bật (`ads-business-rules.md`, S-12).
22. **Khe native nhỏ ở đáy cao 56 dp (mã nguồn vẽ) hay 80 dp (mã nguồn chừa chỗ, bản đặc tả đã chốt)?**
    - Đề xuất của AI: một hằng số cho cả vẽ và chừa chỗ.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). 80 dp cho cả vẽ và chừa chỗ (A 05:68).
23. **Pool native: ô trả về quảng cáo đã ghi lượt hiển thị thì hủy (bản đặc tả) hay trả lại vòng xoay (mã nguồn)?**
    - Đề xuất của AI: theo bản đặc tả.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Hủy và nạp bù; quảng cáo chưa ghi lượt hiển thị thì trả lại vòng xoay (P-01a).
24. **Rewarded bấm "Xem" lần hai trước khi quảng cáo lên màn: chặn (bản cũ của ta, R-06) hay giữ hành vi AdsKit (bản đặc tả đã chốt)?**
    - Đề xuất của AI: theo bản đặc tả.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Không chặn, gọi hiện như AdsKit, ghi cơ hội `busy` (R-06, KB-19).
25. **Rút đồng ý giữa phiên khi banner đang hiện: giữ khung hay co về 0?**
    - Đề xuất của AI: theo bản đặc tả.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Giữ khung và chỗ chừa, tải lại trong khung khi đồng ý lại (B-06).
26. **Lớp native thu gọn có công tắc riêng không?**
    - Đề xuất của AI: có, `is_show_native_collap` (tên do ta đặt), mặc định bật để đi theo `is_show_native_bottom`.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Có công tắc riêng, mặc định đi theo `is_show_native_bottom`; tắt lớp thu gọn không mất khe đáy (D-01).
27. **AppsFlyer có công tắc từ xa cho từng sự kiện không?**
    - Đề xuất của AI: có, khóa `is_log_af_*` (tên do ta đặt), mặc định bật.
    - Trả lời: theo bản đặc tả độc lập của AdsKit (anh Nhật, 2026-10-23). Có, mỗi sự kiện một công tắc, mặc định bật, hiệu lực từ sự kiện kế tiếp (mục 8.3, ADR-0027). `af_revenue` vẫn không bao giờ gửi.
28. **Mẫu tên cho tham số riêng của một vị trí là gì?** Mô hình chung cần tham số theo vị trí (chèn mỗi N mục, chính sách rewarded khi không có quảng cáo).
    - Đề xuất của AI: `<tham số>_<mã vị trí>`, cùng thứ tự với khóa AdsKit (`is_show_<thứ>`, `ads_interval_<nhóm>`, `time_reload_<thứ>`: phần chỉ "cái gì" ở cuối). Ví dụ `native_every_<mã vị trí>`, `rw_no_ad_policy_<mã vị trí>`. Chỉ tạo khóa khi spec của app cần chỉnh từ xa; không thì là thuộc tính cố định trong `AdPlacement`.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
29. **Rewarded không có quảng cáo (chưa sẵn sàng, lỗi mạng) thì mặc định chặn hay cho làm hành động?**
    - Đề xuất của AI: mặc định `block` như bản đặc tả (A 03:188-190: xem không thành công thì chặn xuất file, người dùng thử lại được). Spec của app được chọn `allow` hay `allow_after_<n>` (cho làm sau n lần liên tiếp không có quảng cáo) cho hành động mà chặn sẽ làm người dùng bỏ app; chính sách chỉnh từ xa bằng khóa theo câu 28 khi spec cần. Người dùng tự đóng quảng cáo trước khi được thưởng thì luôn chặn, không chỉnh được.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
30. **Nhóm giãn cách riêng có liên quan tới nhóm mặc định không?** AdsKit chỉ có một mốc chung; màn âm thanh tự giữ bộ đếm riêng ở app và gọi inter với `recordInterval=false` (`M/AdsKit.kt:124-127`).
    - Đề xuất của AI: độc lập hai chiều: inter của nhóm riêng chỉ đọc và ghi mốc của nhóm mình (không bị `ads_interval` chặn, không dời mốc chung); inter của nhóm mặc định không đọc mốc của nhóm riêng. App open, resume, combo luôn ở nhóm mặc định. Cổng không chồng nhau và cổng đồng ý vẫn chung.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
31. **Màn native sau inter là đích điều hướng của `:app` hay lớp phủ của `:core:ads`?** Bản đặc tả để app chủ khai báo đường mở màn (A 07:79-84).
    - Đề xuất của AI: lớp phủ của `:core:ads`, vẽ trong một bộ chứa mà `:app` đặt một lần ở gốc giao diện (HD-05). Feature và luồng resume gọi combo mà không cần route; không đụng `AppNavHost`; `:core:ads` giữ suất chặn quảng cáo tự hiện và báo đóng cả cụm. App không dùng combo thì để `is_show_native_after_inter` tắt.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
32. **Native ở bước của luồng: một khóa cho cả luồng hay một khóa cho mỗi bước?**
    - Đề xuất của AI: một vị trí, một khóa cho cả luồng (giống `is_show_native_onboarding` của AdsKit); bước nào có quảng cáo là thông số trong spec của app. Người vận hành cần bật riêng từng bước thì spec của app khai báo thêm vị trí.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
33. **Ô native trong tập hợp khi hết quảng cáo: mặc định ẩn hay giữ khung chờ?**
    - Đề xuất của AI: mặc định ẩn (bản đặc tả, P-06); vị trí trong lưới có khung cố định khai báo "giữ chỗ khi cạn" trong spec của app.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).
34. **Tên khóa của hành động xuất file là gì?** AdsKit gán chuỗi `is_show_rw_get_item` cho trường `isShowRewardedOnExport` (`DG:180`), tức một khóa mang tên "nhận vật phẩm" nhưng dùng cho xuất file.
    - Đề xuất của AI: dự án mẫu đặt mã vị trí theo hành động: `rw_get_item` cho nhận vật phẩm, `rw_export` cho xuất file (khóa `is_show_rw_export`, tên do ta đặt). App chuyển từ AdsKit mà đang dùng `is_show_rw_get_item` cho việc xuất file trên Firebase thì đặt mã vị trí `rw_get_item` để giữ khóa cũ, ghi lý do trong spec của app.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23).

## 10. Lịch sử

| Ngày | Người | Nội dung |
| --- | --- | --- |
| 2026-10-21 | AI | Tạo bản đầu từ yêu cầu |
| 2026-10-21 | Nhật | Trả lời 5 câu hỏi, xác nhận |
| 2026-10-21 | AI | Tách ticket T-007 đến T-015. Hiểu câu 3 là màn hình mẫu bỏ banner, định dạng banner vẫn giữ trong `:core:ads` |
| 2026-10-23 | AI | Thêm quyền của app và SDK AppLovin vào mục 6 (rà soát RUN-20261023-01: `VIBRATE` của T-015 chưa có trong spec); chuyển sang `changed`, chờ xác nhận lại |
| 2026-10-23 | Nhật | Trả lời câu 6 |
| 2026-10-23 | Nhật | Xác nhận lại (AI ghi theo lời anh Nhật) |
| 2026-10-23 | AI | Theo quyết định của anh Nhật (hành vi quảng cáo theo AdsKit PARTNER, ADR-0026; AppsFlyer, ADR-0027): đổi tiêu chí của KB-1 đến KB-10 (giữ chữ cũ, gạch ngang), thêm KB-11 đến KB-24, mục 5, 6, 8 theo AdsKit, câu hỏi 7 đến 21; tách ticket T-019 đến T-036. Chuyển sang `changed`, chờ xác nhận lại |
| 2026-10-23 | AI | Theo quyết định của anh Nhật (theo bản đặc tả độc lập của AdsKit, kể cả mục "Đã chốt"): khe đáy 80 dp, combo vẫn sang native khi inter không lên màn, rewarded bấm lần hai ghi `busy`, quyền tạm mặc định tắt, giữ khung banner, công tắc lớp thu gọn và từng sự kiện AppsFlyer; ghi câu 21 đến 27 là đã trả lời. Theo yêu cầu tổng quát hóa: KB-19 thành hành động có điều kiện, KB-20 thành native ở bước của luồng, KB-21 thành native trong tập hợp, thêm KB-25; viết lại câu 13, 19; thêm câu 28 đến 34 |
| 2026-10-23 | Nhật | Trả lời câu 7 đến 20 và 28 đến 34 theo đề xuất của AI; đồng ý các khóa tên do ta đặt (`is_show_native_collap`, `is_log_af_*`, `is_show_rw_export`) và kế hoạch nạp sẵn mặc định suy từ danh sách vị trí; xác nhận (AI ghi theo lời anh Nhật) |
| 2026-10-24 | Nhật | Theo trả lời câu hỏi 1, 2, 5 của plan T-034: tách tiêu chí 2 của KB-9 thành hai vế (staging chỉ liệt kê adapter; app thật thấy chín adapter khởi tạo), khóa SDK AppLovin không vào app, thêm kết quả đo quyền của tám adapter vào mục 6. Xác nhận lại (AI ghi theo lời anh Nhật) |
| 2026-10-26 | Nhật | Theo trả lời câu hỏi 1 của plan T-026 và T-025: thêm vị trí toàn app `native_combo` và `resume_inter` (mục 5). Theo câu hỏi 10 của plan T-027: vòng đếm ngược của KB-16 ở góc trên trái. Theo câu hỏi 1 và 9 của plan T-033: quyền và manifest của AppsFlyer, lúc AppsFlyer khởi động (mục 6). Xác nhận lại (AI ghi theo lời anh Nhật) |
