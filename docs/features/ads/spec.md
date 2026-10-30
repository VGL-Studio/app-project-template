---
feature: ads
status: confirmed                # draft | confirmed | changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-native-va-remote-config.md
  - requests/2026-10-29-hoc-tu-jbase.md
design: features/ads/design/README.md
created: 2026-10-21
confirmed: 2026-10-30
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
- **Thêm 2026-10-29 (yêu cầu `requests/2026-10-29-hoc-tu-jbase.md`, ADR-0036):** quảng cáo trong màn hình dừng khi bị quảng cáo toàn màn hình che; quảng cáo toàn màn hình chỉ hiện khi màn hình của app còn dùng được; mã quảng cáo cho mọi lớp của thanh đáy ở production; thử mediation trên máy thật của staging; native trong danh sách thử lại khi lỗi; thanh đáy chừa chỗ lúc đang tải; banner MREC; cổng không hiện quảng cáo cho người đã mua gói bỏ quảng cáo (KB-26 tới KB-30). AppLovin MAX và Amazon APS (ADR-0037, KB-31), tiêu chí chốt sau câu hỏi 41 tới 47.

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
- [ ] ~~Khi đang tải thì vùng quảng cáo giữ đúng chiều cao với hiệu ứng chờ, nội dung bên dưới không nhảy.~~ **Đổi 2026-10-23:** khi native nhỏ ở đáy thì vùng quảng cáo cao 80 dp; khi đang tải thì giữ đúng chiều cao đó với hiệu ứng chờ. **Đổi 2026-10-29 (ADR-0038, KB-32):** khe native nhỏ ở đáy cao theo bề rộng màn hình như JBase, từ 90 dp tới 200 dp.
- [ ] ~~Khi không có quảng cáo sau thời gian chờ thì vùng quảng cáo thu lại; nếu vị trí được khai báo là cạnh nút của app thì giữ khoảng trống để nút không dịch.~~ **Đổi 2026-10-23:** khi nạp lỗi thì vùng quảng cáo co về 0 dp, nội dung không chừa khoảng trống cho nó.
- [ ] ~~Khi app ở chế độ tối thì quảng cáo dùng màu tối của hệ thống thiết kế.~~ **Đổi 2026-10-23:** màu lấy từ hệ thống thiết kế; chữ quảng cáo luôn đọc được trên nền của vùng quảng cáo, kể cả khi máy ở chế độ tối.
- [ ] **Thêm 2026-10-23:** khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ.
- [ ] **Thêm 2026-10-29:** khi máy đặt cỡ chữ 200% thì tiêu đề, nhãn "Ad" và CTA của native nhỏ không bị cắt mất chữ (có thể rút gọn bằng dấu ba chấm), khe vẫn cao 80 dp (D-02). **Đổi 2026-10-29 (ADR-0038):** chiều cao khe theo KB-32.

Trường hợp đặc biệt:

- [ ] Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ.
- [ ] Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ.

### KB-2. Native medium thay MREC

Tiêu chí chấp nhận:

- [ ] Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt.
- [ ] Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1.
- [ ] **Thêm 2026-10-23:** khi hai ô medium cùng hiện trên màn thì mỗi ô có một quảng cáo khác nhau.
- [ ] **Thêm 2026-10-29:** khi quảng cáo có điểm đánh giá thì chạm vào hàng sao được tính là chạm vào quảng cáo, như các phần khác của quảng cáo.
- **Thêm 2026-10-29 (ADR-0036):** native medium vẫn là lựa chọn mặc định thay MREC; app nào cần banner MREC thật thì dùng định dạng `banner_mrec` (KB-29).

### KB-3. Native trong danh sách

Một danh sách hay lưới có vị trí native trong danh sách.

Tiêu chí chấp nhận:

- [ ] Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục (N chỉnh từ xa, mặc định 8), không bao giờ ở vị trí đầu tiên. **Đổi 2026-10-23:** N là tham số của vị trí, khóa `native_every_<mã vị trí>` (câu hỏi 19, 28).
- [ ] Khi danh sách ít hơn N mục thì không có quảng cáo.
- [ ] Mỗi ô quảng cáo có nhãn "Ad" rõ, không thể nhầm với một mục của danh sách.
- [ ] **Thêm 2026-10-23:** mỗi ô quảng cáo có một quảng cáo riêng; khi không còn quảng cáo nào để lấy thì ô không hiện, không để khung chờ mãi.
- [ ] **Thêm 2026-10-29:** khi một ô native small trong danh sách tải lỗi thì ô thử lại theo cùng nhịp của native (`ads-business-rules.md`, O-05), chỉ khi app ở foreground và cổng đồng ý mở; có quảng cáo thì ô hiện.

### KB-4. Native thu gọn được

Tiêu chí chấp nhận:

- [ ] ~~Khi vào màn hình thì quảng cáo ở dạng mở (như medium) ở đáy màn hình, có nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices.~~ **Đổi 2026-10-23:** khi vào màn hình thì quảng cáo mở ở đáy, cao 250 dp, nổi trên thanh đáy, có nút thu gọn 32 dp ở góc trên phải; chỉ lấp chiều rộng, không che thanh điều hướng của app.
- [ ] ~~Khi người dùng chạm nút thu gọn thì quảng cáo chuyển sang dạng gọn (như small).~~ **Đổi 2026-10-23:** khi người dùng chạm nút thu gọn thì lớp quảng cáo ẩn, lộ thanh đáy (native nhỏ 80 dp) bên dưới. **Đổi 2026-10-29 (ADR-0038):** chiều cao native nhỏ theo KB-32.
- [ ] ~~Khi đã thu gọn thì quảng cáo không tự mở lại trong lúc người dùng còn ở màn hình đó, kể cả khi tải quảng cáo mới.~~ **Đổi 2026-10-23:** khi đã thu gọn thì quảng cáo tự mở lại sau số giây đặt từ xa (mặc định 30 giây, 0 là không mở lại), đếm từ lúc người dùng thu gọn; nội dung được thay mỗi 60 giây mà không đổi trạng thái thu gọn. **Đổi 2026-10-29 (ADR-0038, KB-32):** mặc định 10 giây như JBase.

### KB-5. Native toàn màn hình thay interstitial

Một điểm dừng tự nhiên trong app (ví dụ sau khi lưu xong một ảnh) gọi quảng cáo toàn màn hình.

**Đổi 2026-10-23:** native toàn màn hình nay hiện liền sau interstitial (KB-16), không thay interstitial. Các tiêu chí dưới thay bằng KB-16.

Tiêu chí chấp nhận:

- [ ] ~~Khi đủ điều kiện tần suất thì quảng cáo hiện toàn màn hình với nút đóng thấy và bấm được ngay, ít nhất 48 dp, ở góc khác AdChoices.~~ **Đổi 2026-10-23:** xem KB-16 (đếm ngược 3 giây, nút đóng 36 dp). **Đổi 2026-10-29 (ADR-0038):** đếm ngược và nút đóng theo KB-32 (5 giây, 48 dp).
- [ ] Khi bấm nút đóng hoặc nút quay lại của máy thì quảng cáo đóng và người dùng về đúng chỗ đang làm. **Đổi 2026-10-23:** trong 3 giây đếm ngược nút quay lại không làm gì, sau đó đóng như nút đóng (câu hỏi 17). **Đổi 2026-10-29 (ADR-0038):** thời gian đếm ngược theo KB-32.
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
- **Thêm 2026-10-29:** AppLovin MAX và Amazon APS là nguồn quảng cáo thứ hai bên cạnh AdMob (ADR-0037, KB-31). Khi ADR-0037 được chấp nhận, vế "khóa không nằm trong app" ở trên chỉ còn đúng cho adapter AppLovin của AdMob; khóa SDK MAX đi vào app lúc build phát hành (câu hỏi 43).

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
- [ ] Khi app chưa bật quyền tạm (mặc định tắt, cả cấu hình của app lẫn công tắc từ xa) thì quảng cáo chỉ tải khi hộp thoại đồng ý cho phép; lỗi mạng của việc hỏi đồng ý không cho tải. **Đổi 2026-10-29 (ADR-0038, KB-32):** khởi động sớm bằng đồng ý thật của lần mở trước (`CACHED_EARLY`) luôn bật như JBase, không cần cấu hình hay công tắc; phần quyền tạm khi lỗi mạng (`ERROR_ALLOWED`) giữ đúng tiêu chí này.
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
- [ ] Màn native có đếm ngược 3 giây trong vòng 36 dp ở ~~góc trên phải~~ **đổi 2026-10-26:** góc trên trái, theo thiết kế T-035 (góc trên phải dành cho AdChoices; câu hỏi 10 của plan T-027); hết đếm ngược thì vòng đó thành nút đóng. **Đổi 2026-10-29 (ADR-0038, KB-32):** đếm ngược mặc định 5 giây, chỉnh từ xa bằng `time_close_native` (1 tới 30), trong vòng 48 dp, vẫn ở góc trên trái.
- [ ] Khi native không tải được thì nút đóng dùng được ngay và màn tự đóng khi hết đếm ngược.
- [ ] Khi đóng màn native thì người dùng đi tiếp tới màn đã chọn, và khoảng cách `ads_interval` tính từ lúc này.
- [ ] Khi khóa tắt thì interstitial đóng xong người dùng đi tiếp ngay.
- [ ] Khi interstitial bị chặn ngay từ đầu (chưa có, chưa qua khoảng cách, đang có quảng cáo khác) thì người dùng đi tiếp ngay, không có native.
- [ ] Khi interstitial đã được phép hiện nhưng không lên được màn (mất quảng cáo, lỗi hiển thị) và native sau inter đang bật thì vẫn có màn native toàn màn hình. **Đổi 2026-10-30:** trừ khi interstitial không được gọi hiện vì màn hình của app không còn dùng được (KB-26 tiêu chí 3): khi đó không có native, người dùng đi tiếp ngay (câu hỏi 2 của plan T-055).

### KB-17. Thanh quảng cáo ở đáy màn hình

Tiêu chí chấp nhận:

- [ ] Khi bật native ở đáy thì đáy màn hình có native nhỏ 80 dp và lớp native thu gọn 250 dp nổi trên (KB-4). **Đổi 2026-10-29 (ADR-0038):** chiều cao theo KB-32.
- [ ] Khi người vận hành tắt riêng lớp thu gọn thì native nhỏ ở đáy vẫn còn.
- [ ] Khi đang chờ quảng cáo đầu tiên thì khe native nhỏ hiện khung chờ; khi tải lỗi thì khe co về 0.
- [ ] Khi tắt native ở đáy thì đáy màn hình có banner.
- [ ] ~~Nội dung của màn hình chừa đúng chiều cao của thanh đáy khi thanh có quảng cáo, không chừa thừa hay thiếu.~~ **Đổi 2026-10-29 (ADR-0036, câu hỏi 37):** nội dung của màn hình chừa đúng chiều cao của khe đáy từ lúc khe đang tải và khi khe có quảng cáo, nên nội dung không bị đẩy khi quảng cáo về; khi khe nạp lỗi thì không chừa gì. Không chừa thừa hay thiếu.
- [ ] **Thêm 2026-10-29:** khi app phát hành bản production có thanh đáy thì khe banner và lớp native thu gọn đều có mã quảng cáo và gửi được yêu cầu, theo mã theo khe (câu hỏi 14).
- [ ] Doanh thu và lượt bấm của thanh đáy tính cho màn hình đang hiện, không cho màn hình đầu tiên.

### KB-18. Banner

Tiêu chí chấp nhận:

- [ ] Khi đơn vị banner bật tự làm mới trên AdMob thì banner tự đổi theo nhịp của AdMob, app không tự tải lại.
- [ ] Khi app ở nền thì banner không tải lại hay thử lại.
- [ ] Khi dùng banner thu gọn thì mỗi lần làm mới là một banner bung ra mới.
- [ ] Khi chưa có đồng ý thì banner không gửi yêu cầu.
- [ ] Khi người dùng rút đồng ý lúc banner đang hiện thì khung banner giữ nguyên chiều cao, giao diện không nhảy; đồng ý lại thì banner tải lại trong khung đó.
- [ ] Banner thu gọn được tải lại sau mỗi 30 giây, không dùng tự làm mới của AdMob.
- [ ] **Thêm 2026-10-29 (ADR-0038):** banner cố định dùng cỡ thích ứng neo đáy thường như JBase (KB-32).

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

### KB-26. Quảng cáo toàn màn hình không cũ và không hiện sai lúc

**Thêm 2026-10-29.** Người dùng bấm một nút có interstitial, hoặc quay lại app.

Tiêu chí chấp nhận:

- [ ] ~~Khi interstitial đã tải từ đủ 1 giờ trở lên thì nó không được hiện; app coi như chưa có quảng cáo (người dùng đi tiếp ngay, ghi cơ hội `not_ready`) và tải quảng cáo mới (câu hỏi 35).~~ **Đổi 2026-10-29:** bỏ, theo chuẩn JBase (câu hỏi 35).
- [ ] ~~Khi interstitial tải chưa tới 1 giờ thì hiện như trước.~~ **Đổi 2026-10-29:** bỏ cùng tiêu chí trên.
- [ ] Khi màn hình của app không còn dùng được lúc gọi hiện (đang đóng, đã hủy, không ở foreground) thì quảng cáo không được gọi hiện, vẫn giữ trong khe cho lần sau; người dùng đi tiếp ngay và có một sự kiện lỗi hiển thị lý do `app_not_foreground`.
- [ ] Khi người dùng xoay máy lúc quảng cáo toàn màn hình đang hiện thì hành động sau quảng cáo (sang màn sau, lưu) chỉ chạy khi quảng cáo đóng, đúng một lần.

### KB-27. Quảng cáo trong màn hình dừng khi bị quảng cáo toàn màn hình che

**Thêm 2026-10-29** (ADR-0036, điểm 1). Màn hình có banner hay native làm mới tại chỗ; một native toàn màn hình (sau inter hay độc lập) hiện đè lên.

Tiêu chí chấp nhận:

- [ ] Khi native toàn màn hình đang hiện thì banner và native ở màn bên dưới không gửi yêu cầu mới, không làm mới, không thử lại (câu hỏi 36).
- [ ] Quảng cáo đang có ở màn bên dưới giữ nguyên, không bị hủy.
- [ ] Khi native toàn màn hình đóng thì banner chạy lại và native làm mới lại theo nhịp của chúng; lượt làm mới tới hạn trong lúc bị che thì bỏ, không dồn.

### KB-28. Thử mediation trên máy thật của bản staging

**Thêm 2026-10-29.** Dev cài bản staging lên máy thật để kiểm mediation.

Tiêu chí chấp nhận:

- [ ] Khi dev khai báo máy của mình là máy thử trong cấu hình trên máy dev (không trong repo) thì bản staging gửi yêu cầu như máy thử và Ad Inspector mở được trên máy đó (câu hỏi 40).
- [ ] Khi không khai báo máy thử thì bản staging chạy như hiện tại; bản production không bao giờ có máy thử.
- [ ] Mỗi lượt tải quảng cáo ở staging ghi log từng mạng đã thử (tên mạng, thời gian, lỗi), không ghi mã đơn vị.

### KB-29. Banner MREC

**Thêm 2026-10-29** (ADR-0036, điểm 3). Spec của một app khai báo vị trí `banner_mrec` trong màn hình.

Tiêu chí chấp nhận:

- [ ] Khi vị trí bật và có quảng cáo thì màn hình có một banner 300 x 250 dp, căn giữa, có nhãn và AdChoices của SDK.
- [ ] Banner MREC theo cùng luật với banner cố định: chờ cổng đồng ý, làm mới theo cài đặt của đơn vị trên AdMob, tạm dừng khi màn hình không hiện, thử lại khi lỗi, giữ khung khi rút đồng ý (KB-18), dừng khi bị quảng cáo toàn màn hình che (KB-27) (câu hỏi 38).
- [ ] Khi đang tải thì vùng giữ đúng 250 dp; khi nạp lỗi thì co về 0.
- [ ] Doanh thu ghi với định dạng riêng của MREC (`ad_format`), vị trí là mã vị trí.
- [ ] Bản staging có một vị trí MREC trên màn xem thử, dùng mã thử của Google.

### KB-30. Người dùng đã mua gói bỏ quảng cáo

**Thêm 2026-10-29** (ADR-0036, điểm 4). Một app tạo từ dự án mẫu có gói bỏ quảng cáo; app báo cho `:core:ads` người dùng đã mua.

Tiêu chí chấp nhận:

- [ ] Khi app báo người dùng đã mua gói bỏ quảng cáo thì không có quảng cáo nào được tải hay hiện, kể cả quảng cáo mở app và khi quay lại app; thanh đáy không chiếm chỗ (câu hỏi 39).
- [ ] Khi người dùng làm một hành động có điều kiện (KB-19) thì hành động làm thẳng, không có hộp thoại.
- [ ] Khi trạng thái đổi giữa lúc dùng app (vừa mua xong) thì quảng cáo đang hiện trong màn hình biến mất ngay, không cần mở lại app.
- [ ] Khi app không nối tín hiệu này (dự án mẫu hôm nay) thì quảng cáo chạy như trước.
- [ ] **Thêm 2026-10-30:** Khi người dùng đã mua gói bỏ quảng cáo thì hộp thoại đồng ý, SDK quảng cáo và AppsFlyer vẫn như khi chưa mua; chỉ không tải, không hiện quảng cáo (câu hỏi 50).

### KB-31. AppLovin MAX và Amazon APS

**Thêm 2026-10-29** (ADR-0037). Chi tiết ở câu hỏi 41 tới 47: MAX cho banner, MREC, interstitial, rewarded, app open; native chỉ qua AdMob; thứ tự đặt bằng `ads_mediation_order`, mặc định MAX trước; APS làm sau khi có tài khoản.

Tiêu chí chấp nhận:

- [ ] Khi nền tảng thứ nhất theo `ads_mediation_order` (mặc định MAX) chưa có quảng cáo cho một lượt hiện và nền tảng thứ hai có thì người dùng thấy quảng cáo của nền tảng thứ hai; một lượt chỉ ghi một cơ hội.
- [ ] Khi `ads_mediation_order` là `admob` hay `max` thì chỉ nền tảng đó được nạp và hiện.
- [ ] Quảng cáo mở app lúc khởi động chỉ là app open của AdMob; app open của MAX dùng khi quay lại app.
- [ ] Native luôn qua AdMob, kể cả khi thứ tự là `max`.
- [ ] MAX và APS chỉ khởi động sau khi cổng đồng ý của app mở; không bao giờ gửi yêu cầu khi chưa có đồng ý.
- [ ] Mọi luật toàn màn hình (không chồng nhau, khoảng cách `ads_interval`, gỡ kẹt, chặn quảng cáo tự hiện) áp như nhau cho quảng cáo của MAX.
- [ ] Doanh thu của MAX ghi ở Firebase và AppsFlyer với vị trí, không tính hai lần.
- [ ] Khóa SDK MAX và mã APS không nằm trong repo; bản staging chạy được khi không có chúng.
- [ ] Khi app có tài khoản Amazon Publisher Services (chưa có ngày 2026-10-29) thì APS trả giá cho banner, MREC, interstitial, rewarded của MAX; chế độ thử của APS chỉ ở staging.

### KB-32. Nghiệp vụ theo chuẩn JBase

**Thêm 2026-10-29** (ADR-0038). Anh Nhật chọn lấy nghiệp vụ của JBase làm chuẩn, kể cả cho luật đã chốt; các luật dưới là những chỗ JBase làm khác PARTNER mà không phải lỗi, anh Nhật chọn đổi theo. Chỗ JBase không có thì giữ luật hiện tại.

Tiêu chí chấp nhận:

- [ ] Khi native toàn màn hình sau inter hiện thì đếm ngược mặc định 5 giây trước khi đóng được; người vận hành đổi bằng `time_close_native` (1 tới 30 giây, giá trị khác dùng 5). Vòng đếm ngược và nút đóng 48 dp, ở góc trên trái (K-02, K-03).
- [ ] Khi người dùng thu gọn native thu gọn thì nó tự bung lại sau 10 giây nếu người vận hành không đổi `time_expand_collap_ad`; 0 vẫn là không bung lại (CL-03).
- [ ] Khe native nhỏ ở đáy cao theo bề rộng màn hình như JBase: 90 dp ở bề rộng 320 dp trở xuống, 200 dp ở 600 dp trở lên, tăng đều ở giữa; chữ, icon, CTA theo layout của JBase (câu hỏi 48). Native medium và native thu gọn theo kích thước của JBase (câu hỏi 48); các biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ) giữ kích thước hiện tại.
- [ ] Khi người dùng đã đồng ý ở lần mở app trước thì SDK quảng cáo khởi động ngay lúc mở app, không chờ hộp thoại đồng ý trả lời, không cần cấu hình hay công tắc nào (`CACHED_EARLY`, S-03, S-12).
- [ ] Banner cố định dùng cỡ thích ứng neo đáy thường, như JBase, thay cỡ thích ứng neo đáy lớn (B-01).

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
- ~~**Thêm 2026-10-29 (ADR-0037, chờ câu hỏi 41 tới 47):** SDK bên ngoài mới dự kiến: AppLovin MAX và Amazon APS. Quyền chúng kéo vào manifest gộp đo ở plan, ghi vào mục này trước khi duyệt plan, như câu hỏi 18. Play Console: An toàn dữ liệu khai thêm Amazon trước khi phát hành bản có APS.~~
- **Thêm 2026-10-29 (đo ở plan T-068):** SDK AppLovin MAX `13.6.4` (cùng thư viện mà adapter AppLovin của AdMob đã kéo vào từ T-015) và adapter Google của MAX `25.5.0.0` không thêm quyền hay thành phần nào vào manifest gộp; vẫn bỏ `AppLovinInitProvider`, `CHECK_LICENSE`, `ACCESS_WIFI_STATE` như T-015, giữ `VIBRATE`. MAX chỉ khởi động sau khi cổng đồng ý của app mở và sau Google Mobile Ads, đọc đồng ý từ chuỗi TCF của UMP; app không viết cứng đồng ý. Khóa SDK MAX đưa vào lúc build phát hành, không vào repo; bản staging không có khóa thì MAX không khởi động. Play Console: An toàn dữ liệu đã khai AppLovin (T-015); APS chưa làm.
- **Thêm 2026-10-29:** cổng không hiện quảng cáo (KB-30) và banner MREC (KB-29) không thêm SDK hay quyền. Không thêm sự kiện phân tích mới; MREC dùng các sự kiện đã có với một mã định dạng mới (câu hỏi 38).

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
| `time_expand_collap_ad` | long (giây) | ~~30~~ 10 (đổi 2026-10-29, ADR-0038, theo JBase) | Native thu gọn tự mở lại sau khi người dùng thu gọn; 0 là không (tên do ta đặt) |
| `is_show_rw_get_item` | boolean | tắt | Khóa suy ra của vị trí rewarded `rw_get_item` (nhận vật phẩm), trùng chuỗi AdsKit. Vị trí rewarded khác dùng `is_show_<mã vị trí>`, ví dụ `is_show_rw_export` |
| `is_show_native_onboarding` | boolean | tắt | Khóa suy ra của vị trí `native_onboarding` (native trên luồng giới thiệu), trùng chuỗi AdsKit |
| `is_show_<mã vị trí>` | boolean | tắt | Bật một vị trí, thay `ads_<vị trí>_enabled` (suy ra theo mẫu) |
| `ads_interval_<nhóm>` | long (giây) | theo spec | Khoảng cách của nhóm giãn cách riêng (suy ra theo mẫu) |
| `<tham số>_<mã vị trí>` | theo tham số | theo spec | Tham số riêng của vị trí (câu hỏi 28) |
| `is_enable_consent_shortcut` | boolean | tắt (câu hỏi 21) | Quyền tạm khi đồng ý chưa trả lời xong; cần thêm cấu hình của app, cũng mặc định tắt (tên do ta đặt). **Đổi 2026-10-29 (ADR-0038):** chỉ còn điều khiển `ERROR_ALLOWED`; `CACHED_EARLY` luôn bật |
| **Thêm 2026-10-29 (ADR-0038):** `time_close_native` | long (giây) | 5 | Đếm ngược trước khi đóng được native toàn màn hình sau inter; nhận 1 tới 30, giá trị khác dùng 5 (tên của JBase) |
| **Thêm 2026-10-29 (ADR-0037):** `ads_mediation_order` | string | `max_admob` | Thứ tự hai nền tảng cho banner, MREC, interstitial, rewarded, app open khi quay lại: `admob`, `max`, `admob_max`, `max_admob`; giá trị khác dùng `max_admob`; bản không có khóa SDK MAX dùng `admob` (tên do ta đặt) |
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
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-23). **Đổi 2026-10-30:** phần làm hôm nay chỉ áp mã theo khe cho hai lớp của thanh đáy (`banner`, `native_collap`); định dạng chính vẫn lấy mã theo vị trí, vì `release.sh` điền mã thử cho mọi khóa vị trí thiếu. Đổi hẳn sang theo khe cho mọi định dạng là ticket riêng sau T-057 (anh Nhật, câu hỏi 1 của plan T-056).
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

**Thêm 2026-10-29** (yêu cầu `requests/2026-10-29-hoc-tu-jbase.md`). Câu 35 tới 40 chốt chi tiết của ADR-0036 và các ticket T-054 tới T-063; câu 41 tới 47 cho MAX và APS (ADR-0037), ticket tách sau khi trả lời.

35. **Interstitial hết hạn sau bao lâu, và hết hạn thì làm gì?** AdsKit và JBase không có hạn dùng cho interstitial; app open và rewarded của ta hết hạn sau 1 giờ (A-07).
    - Đề xuất của AI: 1 giờ, cùng hằng số với app open, đọc lúc quyết định hiện. Hết hạn thì bỏ quảng cáo, coi như chưa có: ghi cơ hội `not_ready` với `load_state` lúc đó, người dùng đi tiếp ngay, tải lại ngay (không chờ 2 giây như sau khi đóng). Áp cho mọi vị trí interstitial, kể cả `open_app_inter`, `resume_inter`.
    - Trả lời: không làm. Anh Nhật chọn lấy nghiệp vụ JBase làm chuẩn (2026-10-29); interstitial của JBase không có hạn dùng (bộ điều khiển interstitial của `admobadshelper` không giữ thời điểm tải), chỉ app open có. Bỏ điểm 1 của ADR-0036 và ticket hạn dùng interstitial (mã T-053 dùng lại cho KB-32, vì ticket cũ chưa vào nhánh chính). Tiêu chí 1 và 2 của KB-26 không áp dụng.
36. **Dừng quảng cáo trong màn hình khi bị quảng cáo toàn màn hình che: áp cho những gì?**
    - Đề xuất của AI: khi native toàn màn hình của `:core:ads` đang hiện (cờ "đang hiện" của cổng chung, F-01), banner tạm dừng như khi màn hình không hiện, native small và lớp thu gọn bỏ nhịp làm mới, mọi lượt tải và thử lại của khe trong màn hình chờ tới khi màn che đóng. Không hủy quảng cáo đang có. Trang native toàn trang ở bước của luồng (`native_step` toàn trang) không tính, vì nó không phải lớp phủ và chiếm chỗ của bước, không đè lên quảng cáo khác.
    - Trả lời: theo đề xuất của AI, khớp chuẩn JBase: MAX của JBase dừng tự làm mới banner và MREC khi có quảng cáo toàn màn hình (`pauseAutoRefresh: stopped (fullscreen showing)`) và chạy lại khi nó đóng; native banner, MREC bị chặn khi native toàn màn hình đang hiện (anh Nhật, 2026-10-29).
37. **Thanh đáy chừa chỗ lúc đang tải: bao nhiêu, tới khi nào?** Anh Nhật đã chọn chừa chỗ lúc tải (đổi D-04).
    - Đề xuất của AI: chừa đúng chiều cao vẽ của khe đáy từ lúc khe bắt đầu chờ quảng cáo đầu tiên: native bằng chiều cao khe cộng lề 16 dp (hôm nay 96 dp; đổi theo KB-32: chiều cao khe tính theo bề rộng màn hình), banner bằng chiều cao của khung adaptive đã biết trước khi tải. Nạp lỗi thì co về 0 như cũ; lần nạp lại sau đó không chừa lại cho tới khi có quảng cáo (tránh nhảy lên xuống mỗi lần thử lại). Lớp thu gọn vẫn không chừa (D-03).
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29). JBase không hiện gì khi chưa có quảng cáo (`NativeAdsManager.java:888`); anh Nhật giữ lựa chọn chừa chỗ lúc tải, đứng trên chuẩn JBase.
38. **Banner MREC dùng khe nào, luật nào?**
    - Đề xuất của AI: khe mới `banner_mrec`, kích thước `MEDIUM_RECTANGLE` 300 x 250, mã thử MREC của Google ở staging, làm mới theo cài đặt của đơn vị trên AdMob như banner cố định (B-01, B-02, B-04, B-05, B-06). Đặt trong nội dung của màn (như native medium), không vào thanh đáy, không làm ô trong danh sách ở đợt này. Mã định dạng số liệu `BANNER_MREC`; định dạng `banner_mrec` thêm vào bảng định dạng của `ads-plan.py` và `release.sh` (mã thử). Thêm vị trí `gallery_banner_mrec` cho màn xem thử.
    - Trả lời: theo đề xuất của AI, khớp chuẩn JBase: JBase có MREC cho cả AdMob và MAX (`mrec_admob`, `applovin_mrec_ads_key`), MAX tự làm mới (anh Nhật, 2026-10-29). JBase đặt MREC nổi ở dưới, giữa hay trên màn; dự án mẫu đặt trong nội dung của màn như native medium.
39. **Cổng không hiện quảng cáo: nhận tín hiệu từ đâu, và tắt những gì?**
    - Đề xuất của AI: `:core:ads` nhận một tín hiệu đúng sai (luồng dữ liệu) mà `:app` cung cấp qua Hilt, mặc định luôn `false`; dự án mẫu không có mua hàng nên không có nguồn thật. Bật thì như `is_show_ads` tắt: không khởi tạo thêm lượt tải nào, mọi khe trong màn hình biến mất ngay, không quảng cáo mở app hay khi quay lại, hành động có điều kiện làm thẳng, không ghi cơ hội. Không tự lưu trạng thái mua; app sở hữu việc đó. Không thêm khóa Remote Config.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29). JBase không có cổng này; anh Nhật giữ lựa chọn, đứng trên chuẩn JBase.
40. **Khai báo máy thử ở đâu, và log từng mạng ghi gì?**
    - Đề xuất của AI: khóa `ads.testDeviceIds` trong `local.properties` của máy dev (không vào git), chỉ đọc ở biến thể staging; CI không có khóa nên không có máy thử. Bản production không có đường đọc khóa này. Mỗi lượt tải xong ở staging ghi một dòng log thẻ `Ads` cho từng mạng trong `ResponseInfo` (tên lớp adapter, thời gian, mã lỗi), không ghi mã đơn vị hay mã máy.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29). Đây là công cụ cho dev, không phải nghiệp vụ; JBase có log waterfall của MAX ở mỗi lượt doanh thu, không có máy thử.
41. **MAX dùng cho những định dạng nào?** JBase dùng MAX cho mọi định dạng; phần native của ta viết trên `NativeAdView` của AdMob.
    - Đề xuất của AI: interstitial, rewarded, app open (cả lúc mở app và khi quay lại). Banner và MREC của MAX để đợt sau nếu số liệu cho thấy thiếu quảng cáo; native giữ AdMob. Lý do: định dạng toàn màn hình có doanh thu cao nhất và không phải viết lại phần vẽ.
    - Trả lời: theo chuẩn JBase (anh Nhật, 2026-10-29): MAX cho banner, MREC, interstitial, rewarded, app open; native chỉ qua AdMob (cầu nối Unity của JBase không gọi native của MAX). Quảng cáo mở app lúc khởi động chỉ dùng app open của AdMob, như màn loading của JBase; app open của MAX dùng khi quay lại app theo thứ tự của câu 42.
42. **Thứ tự giữa AdMob và MAX đặt thế nào?** JBase có bốn chế độ: chỉ AdMob, chỉ MAX, AdMob trước, MAX trước.
    - Đề xuất của AI: khóa Remote Config `ads_mediation_order` (string, tên do ta đặt): `admob` (mặc định, như hôm nay), `max`, `admob_max`, `max_admob`. Mỗi lượt hiện hỏi nền tảng thứ nhất, chưa có thì nền tảng thứ hai, cùng một lượt quyết định và một cơ hội; cả hai đều nạp sẵn khi có trong thứ tự.
    - Trả lời: theo chuẩn JBase (anh Nhật, 2026-10-29): bốn chế độ như `AdsConfig.Network` của JBase, mặc định MAX trước (`max_admob`, JBase `MaxPrimary`). Đặt bằng khóa `ads_mediation_order` theo cách của dự án mẫu (JBase đặt trong code). Mỗi lượt hiện hỏi nền tảng thứ nhất, chưa có thì nền tảng thứ hai.
43. **Khóa SDK MAX và mã APS đưa vào app thế nào?** MAX cần khóa SDK trong app, khác adapter AppLovin của AdMob (M-03).
    - Đề xuất của AI: tham số build lúc phát hành như `-PadUnits` (`-PmaxSdkKey`, `-PapsAppId`, mã đơn vị MAX và slot APS theo khe), `preProduction*Build` báo lỗi khi thiếu lúc thứ tự có `max`; `secret-scan.py` thêm mẫu khóa MAX. Staging không có khóa thì MAX và APS không khởi tạo và thứ tự coi như `admob`; muốn thử MAX ở staging thì dev đặt khóa trong `local.properties` như câu 40.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29). Không theo JBase ở điểm này: JBase để khóa thật trong `strings.xml`, trái quy tắc không đưa khóa vào repo (ADR-0026, mục 10).
44. **Đồng ý của MAX và APS theo cổng nào?**
    - Đề xuất của AI: theo cổng đồng ý của `:core:ads` (S-01 tới S-19): MAX và APS chỉ khởi tạo khi cổng mở lần đầu, đọc chuỗi đồng ý của UMP; không bật luồng đồng ý riêng của MAX. Rút đồng ý thì cùng đóng như AdMob (S-15).
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29). Không theo JBase ở điểm này: JBase khởi tạo MAX khi Remote Config tải xong, không chờ đồng ý, và viết cứng `setHasUserConsent(true)`, `setDoNotSell(false)`; đó là lỗi tuân thủ GDPR, không phải nghiệp vụ.
45. **AppLovin vừa là mạng con của AdMob vừa là nền tảng MAX: xử lý thế nào để không tự đấu giá với chính mình?**
    - Đề xuất của AI: ghi vào hướng dẫn phát hành: một app dùng MAX thì nhóm mediation AdMob của các khe toàn màn hình không có AppLovin (Bidding), hoặc người vận hành chấp nhận và theo dõi; code không tự xử lý. Cần anh Nhật hoặc nhóm UA quyết.
    - Trả lời: theo chuẩn JBase (anh Nhật, 2026-10-29): app dùng MAX thì nhóm mediation AdMob không có AppLovin, còn MAX gọi Google qua adapter Google của MAX (JBase: AdMob không có adapter AppLovin, MAX có `google-adapter` và `google-ad-manager-adapter`). Ghi vào hướng dẫn phát hành.
46. **Doanh thu của MAX ghi thế nào?**
    - Đề xuất của AI: cùng sự kiện như AdMob (`ad_impression_placement`, `af_ad_revenue`, mục 8 của luật), `ad_platform` và `mediation_network` là `applovin_max`, `ad_source` là mạng MAX báo; doanh thu MAX đơn vị là đô la, không chia một triệu. Phễu `af_inters_*`, `af_rewarded_*` gửi cho cả hai nền tảng. Báo nhóm UA trước khi phát hành (HD-07).
    - Trả lời: theo chuẩn JBase cho giá trị (anh Nhật, 2026-10-29): `ad_platform` là `applovin`, `mediation_network` là `applovinmax` (JBase `MediationNetwork.applovinmax`), `ad_source` là mạng MAX báo, giá trị theo đô la. Tên sự kiện giữ của dự án mẫu (`ad_impression_placement`, `af_ad_revenue`): JBase tự ghi `ad_impression` nên doanh thu bị đếm đôi khi AdMob đã liên kết Firebase. Báo nhóm UA trước khi phát hành (HD-07).
47. **APS cần gì trước khi làm?**
    - Đề xuất của AI: APS chỉ đi kèm MAX, như JBase, cho các định dạng của câu 41. AI chưa kiểm APS có cách đấu giá qua AdMob không; plan APS tra tài liệu của Amazon và ghi kết quả trước khi duyệt. Cần tài khoản Amazon Publisher Services, mã ứng dụng và slot theo khe; anh Nhật xác nhận nhóm có tài khoản trước khi viết plan APS. Nếu chưa có thì làm MAX trước, APS là ticket sau.
    - Trả lời: theo chuẩn JBase cho phạm vi (anh Nhật, 2026-10-29): APS chỉ đi với MAX, cho banner, MREC, interstitial, rewarded. Không theo JBase ở chế độ thử: JBase bật `AdRegistration.enableTesting(true)` ở bản phát hành; dự án mẫu chỉ bật ở staging. Nhóm chưa có tài khoản Amazon Publisher Services (anh Nhật, 2026-10-29): làm MAX trước, APS là ticket sau khi có tài khoản, mã ứng dụng và slot.
48. **Kích thước native theo JBase áp tới đâu?** Layout của JBase: native nhỏ cao 90 tới 200 dp theo bề rộng màn hình, icon 44 dp, tiêu đề 16 sp, mô tả 13 sp, nhãn "Ad" 13 sp trước tiêu đề, CTA cao 44 dp chữ 18 sp; native thu gọn có ảnh 180 dp trên phần nội dung của native nhỏ, vùng thêm 160 tới 200 dp, nút thu gọn 27 dp ở góc trên trái, AdChoices góc trên phải; native medium (MREC native) cao 320 tới 400 dp theo chiều cao màn hình, icon 44 dp, CTA 44 dp chữ 18 sp, không có hàng sao.
    - Đề xuất của AI: theo JBase cho kích thước của ba biến thể đó; vị trí nút thu gọn và AdChoices của lớp thu gọn cũng theo JBase (nút trái, AdChoices phải, ngược thiết kế T-035 hôm nay); màu vẫn lấy từ token của dự án mẫu (JBase viết cứng màu); giữ hàng sao của native medium (JBase không có, không phải lỗi bỏ được). Vẽ lại trên Figma trước khi code (T-064, như T-035), rồi ghi lại ảnh chuẩn. Kiểm lại câu hỏi 22 (80 dp) và luật D-02, D-04 theo chiều cao mới.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29): kích thước ba biến thể theo JBase; lớp thu gọn đặt nút thu gọn ở góc trên trái, AdChoices ở góc trên phải như JBase, nút vẽ 27 dp với vùng chạm 48 dp; giữ hàng sao của native medium; màu theo token.
49. **Chừa chỗ lúc tải (câu 37) với khe cao theo bề rộng:** chiều cao chừa tính thế nào khi chưa có quảng cáo?
    - Đề xuất của AI: dùng cùng hàm tính chiều cao theo bề rộng màn hình của KB-32, nên biết trước khi tải; một hằng số cho cả vẽ và chừa chỗ như câu 22.
    - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-29): cùng một hàm tính chiều cao theo bề rộng màn hình cho cả vẽ và chừa chỗ.
50. **Người đã mua gói bỏ quảng cáo có còn thấy hộp thoại đồng ý của UMP, và SDK quảng cáo, AppsFlyer có còn khởi động không?** (câu hỏi 6 của plan T-063)
    - Đề xuất của AI: T-063 giữ như `is_show_ads` tắt: hộp thoại đồng ý, `MobileAds.initialize` và AppsFlyer như hôm nay, chỉ không tải, không hiện (ít rủi ro nhất cho thứ tự khởi tạo; người đó rút mua thì quảng cáo chạy lại ngay). Bỏ hộp thoại và không khởi động SDK khi đã mua là một ticket sau, khi có app đầu tiên làm mua hàng.
    - Trả lời: theo đề xuất của AI (anh Nhật duyệt plan T-063, 2026-10-30).

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
| 2026-10-29 | AI | Theo yêu cầu `requests/2026-10-29-hoc-tu-jbase.md` và bốn lựa chọn của anh Nhật trong phiên (thêm MAX và APS, thêm `banner_mrec`, chỉ làm cổng không hiện quảng cáo, chừa chỗ lúc đang tải): thêm phạm vi ở mục 2, tiêu chí mới ở KB-1, KB-2, KB-3, KB-9, đổi một tiêu chí của KB-17 (giữ chữ cũ, gạch ngang), thêm KB-26 tới KB-31, hai dòng ở mục 6, câu hỏi 35 tới 47; ADR-0036, ADR-0037 ở trạng thái `proposed`; tách ticket T-053 tới T-063 (MAX và APS tách sau câu 41 tới 47). Chuyển sang `changed`, chờ xác nhận lại |
| 2026-10-29 | AI | Theo hướng dẫn của anh Nhật "lấy nghiệp vụ của JBase làm chuẩn để ra quyết định", kể cả luật đã chốt: ghi trả lời câu 35 tới 47 theo chuẩn JBase (bỏ hạn dùng interstitial, câu 35, ticket T-053 cũ); hai agent đối chiếu toàn bộ `ads-business-rules.md` với JBase; anh Nhật chọn đổi theo JBase đếm ngược native sau inter, bung lại native thu gọn, kích thước native, `CACHED_EARLY` luôn bật, cỡ banner (ADR-0038, KB-32, ghi chú `Đổi 2026-10-29` ở KB-1, KB-4, KB-5, KB-11, KB-16, KB-17, KB-18, mục 8), giữ banner thu gọn nạp lại 30 giây và cách ghi doanh thu AppsFlyer; thêm câu 48, 49; tách ticket T-053 (mới), T-064 tới T-067. Vẫn `changed`, chờ xác nhận |
| 2026-10-29 | Nhật | Trả lời câu 47 (chưa có tài khoản APS), 48 và 49 theo đề xuất của AI (AI ghi theo lời anh Nhật) |
| 2026-10-29 | AI | Chốt KB-31 theo câu 41 tới 47; tách ticket MAX T-068 tới T-073 (APS chưa tách: chưa có tài khoản) |
| 2026-10-29 | Nhật | Xác nhận spec; chấp nhận ADR-0036, ADR-0037, ADR-0038 (AI ghi theo lời anh Nhật) |
| 2026-10-29 | AI | Sửa số điểm khi dẫn ADR-0036 ở KB-27, KB-29, KB-30 cho khớp ADR sau khi bỏ điểm hạn dùng interstitial (điểm 2, 4, 5 thành 1, 3, 4); không đổi nội dung (rà soát plan T-054, T-062) |
| 2026-10-30 | Nhật | Duyệt 12 plan đợt 4 (T-053 tới T-060, T-062, T-063, T-066 tới T-068) theo đề xuất của AI: thêm kết quả đo quyền của MAX vào mục 6 và khóa `ads_mediation_order` vào mục 8 (câu hỏi 1 của plan T-068); thêm câu hỏi 50 và một tiêu chí của KB-30 (câu hỏi 6 của plan T-063). Xác nhận lại (AI ghi theo lời anh Nhật) |
| 2026-10-30 | Nhật | Theo rà soát độc lập danh sách chạy RUN-20261030-01: thêm ngoại lệ vào KB-16 tiêu chí 7 (câu hỏi 2 của plan T-055) và ghi phạm vi hôm nay của câu 14 (câu hỏi 1 của plan T-056). Xác nhận lại (AI ghi theo lời anh Nhật) |
