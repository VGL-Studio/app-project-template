---
feature: ads
status: changed                # draft | confirmed | changed
owner: Nhật
product_owner: Nhật
requests:
  - requests/2026-10-21-native-va-remote-config.md
design: features/ads/design/README.md
created: 2026-10-21
confirmed: 2026-10-23
---

# SPEC: QUẢNG CÁO CỦA DỰ ÁN MẪU (NATIVE, REMOTE CONFIG, HÀNH VI THEO ADSKIT)

<!--
Đây là phần nền của dự án mẫu (module core), không phải một màn hình cho người dùng cuối. "Người dùng" trong kịch bản
là người dùng app tạo từ dự án mẫu, hoặc người vận hành app (bật tắt quảng cáo trên Firebase).
Quyết định: ADR-0026 (hành vi theo AdsKit PARTNER, thay phần lớn ADR-0022), ADR-0027 (AppsFlyer), ADR-0023 (Remote Config).
Luật chi tiết: docs/ops/ads-business-rules.md. Hướng dẫn: docs/ops/ads-guide.md, docs/ops/remote-config.md.
Thay đổi 2026-10-23: tiêu chí bị thay giữ nguyên chữ cũ, gạch ngang, kèm dòng "Đổi 2026-10-23". Kịch bản mới từ KB-11.
-->

## 1. Mục tiêu

- **Người dùng được gì:** ~~quảng cáo trong app giống giao diện app, không đặt ở chỗ dễ bấm nhầm, không xuất hiện dày đặc hay lúc vừa mở app; người vận hành bật tắt và chỉnh tần suất từ xa, thử được quảng cáo trên bản staging trước khi phát hành.~~ **Đổi 2026-10-23:** quảng cáo trong mọi app của nhóm chạy theo cùng một cách đã có doanh thu tốt ở các app dùng AdsKit; quảng cáo vẫn có nhãn rõ, không giả làm nút của app; người vận hành bật tắt từng loại quảng cáo từ xa bằng cùng bộ khóa với các app AdsKit, thấy doanh thu theo từng vị trí, thử được quảng cáo trên bản staging trước khi phát hành.
- **Vì sao làm bây giờ:** app Camera Pose cần native và mức quảng cáo vừa; làm ở dự án mẫu để mọi app dùng chung, không lệch nhau. **Thêm 2026-10-23:** người phụ trách sản phẩm quyết định hành vi theo bộ PARTNER của AdsKit (ADR-0026) để mọi app có doanh thu như các app đang chạy.
- **Đo kết quả bằng gì:** mỗi app bật quảng cáo theo hướng dẫn mà không phải sửa module core; ~~không có cảnh báo chính sách AdMob hay Play về vị trí đặt quảng cáo;~~ ARPDAU và doanh thu theo vị trí ngang các app AdsKit cùng loại; không bị giới hạn phân phát quảng cáo; ARPDAU theo dõi cùng giữ chân D7 và điểm Play trong báo cáo vận hành.

## 2. Phạm vi

**Trong phạm vi:**

- Năm loại native: small, medium, trong danh sách, thu gọn được, toàn màn hình. **Đổi 2026-10-23:** kích thước và hành vi theo AdsKit: thanh đáy (native nhỏ hoặc banner, lớp native thu gọn), native sau inter, native ô lưới và trang xem trước, native giới thiệu, native medium.
- ~~Giới hạn tần suất lưu qua các phiên, ân hạn phiên đầu, giới hạn số quảng cáo toàn màn hình mỗi phiên, công tắc tổng.~~ **Đổi 2026-10-23:** khoảng cách giữa hai interstitial theo AdsKit (trong một lần mở app); công tắc tổng chờ câu hỏi 7.
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
- [ ] ~~Khi đang tải thì vùng quảng cáo giữ đúng chiều cao với hiệu ứng chờ, nội dung bên dưới không nhảy.~~ **Đổi 2026-10-23:** khi native nhỏ ở đáy thì vùng quảng cáo cao 56 dp; khi đang tải thì giữ đúng chiều cao đó với hiệu ứng chờ.
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

- [ ] Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục (N chỉnh từ xa, mặc định 8), không bao giờ ở vị trí đầu tiên. **Đổi 2026-10-23:** tên khóa của N chờ câu hỏi 19.
- [ ] Khi danh sách ít hơn N mục thì không có quảng cáo.
- [ ] Mỗi ô quảng cáo có nhãn "Ad" rõ, không thể nhầm với một mục của danh sách.
- [ ] **Thêm 2026-10-23:** mỗi ô quảng cáo có một quảng cáo riêng; khi không còn quảng cáo nào để lấy thì ô không hiện, không để khung chờ mãi.

### KB-4. Native thu gọn được

Tiêu chí chấp nhận:

- [ ] ~~Khi vào màn hình thì quảng cáo ở dạng mở (như medium) ở đáy màn hình, có nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices.~~ **Đổi 2026-10-23:** khi vào màn hình thì quảng cáo mở ở đáy, cao 250 dp, nổi trên thanh đáy, có nút thu gọn 32 dp ở góc trên phải; chỉ lấp chiều rộng, không che thanh điều hướng của app.
- [ ] ~~Khi người dùng chạm nút thu gọn thì quảng cáo chuyển sang dạng gọn (như small).~~ **Đổi 2026-10-23:** khi người dùng chạm nút thu gọn thì lớp quảng cáo ẩn, lộ thanh đáy (native nhỏ 56 dp) bên dưới.
- [ ] ~~Khi đã thu gọn thì quảng cáo không tự mở lại trong lúc người dùng còn ở màn hình đó, kể cả khi tải quảng cáo mới.~~ **Đổi 2026-10-23:** khi đã thu gọn thì quảng cáo tự mở lại sau số giây đặt từ xa (mặc định 30 giây, 0 là không mở lại), đếm từ lúc người dùng thu gọn; nội dung được thay mỗi 60 giây mà không đổi trạng thái thu gọn.

### KB-5. Native toàn màn hình thay interstitial

Một điểm dừng tự nhiên trong app (ví dụ sau khi lưu xong một ảnh) gọi quảng cáo toàn màn hình.

**Đổi 2026-10-23:** native toàn màn hình nay hiện liền sau interstitial (KB-16), không thay interstitial. Các tiêu chí dưới thay bằng KB-16.

Tiêu chí chấp nhận:

- [ ] ~~Khi đủ điều kiện tần suất thì quảng cáo hiện toàn màn hình với nút đóng thấy và bấm được ngay, ít nhất 48 dp, ở góc khác AdChoices.~~ **Đổi 2026-10-23:** xem KB-16 (đếm ngược 3 giây, nút đóng 36 dp).
- [ ] Khi bấm nút đóng hoặc nút quay lại của máy thì quảng cáo đóng và người dùng về đúng chỗ đang làm. **Đổi 2026-10-23:** nút quay lại trong lúc đếm ngược chờ câu hỏi 17.
- [ ] ~~Khi vừa có một quảng cáo toàn màn hình (interstitial hay native) thì không hiện thêm cái nào cho tới hết khoảng cách tối thiểu.~~ **Đổi 2026-10-23:** xem KB-15.
- [ ] Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng.

### KB-6. Tần suất qua các phiên

**Đổi 2026-10-23:** bộ PARTNER không có các luật này (ADR-0026, câu hỏi 10). Thay bằng KB-15.

Tiêu chí chấp nhận:

- [ ] ~~Khi người dùng đóng app rồi mở lại ngay thì khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình vẫn được tính từ lần hiện trước.~~ **Đổi 2026-10-23:** khoảng cách chỉ tính trong một lần mở app (KB-15).
- [ ] ~~Khi người dùng mở app lần đầu thì không có quảng cáo toàn màn hình trong thời gian ân hạn (mặc định 180 giây).~~ **Đổi 2026-10-23:** không có ân hạn; lần mở đầu có app open khi bật `show_open_ads_first_open` (KB-13).
- [ ] ~~Khi đã đủ số quảng cáo toàn màn hình tối đa của phiên (mặc định 4) thì không hiện thêm cho tới phiên sau.~~ **Đổi 2026-10-23:** không có giới hạn mỗi phiên.

### KB-7. Công tắc tổng

**Đổi 2026-10-23:** giữ công tắc tổng hay không và tên khóa chờ câu hỏi 7.

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
- [ ] Khi mở Ad Inspector trên staging thì thấy adapter AppLovin đã khởi tạo. **Đổi 2026-10-23:** thấy cả chín adapter đã khởi tạo.
- [ ] **Thêm 2026-10-23:** khóa SDK của AppLovin không nằm trong repo; bản staging chạy được khi không có khóa đó.

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
- [ ] Khi người vận hành tắt quyền tạm từ xa thì quyền tạm khi lỗi mạng bị thu hồi ngay.

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
- [ ] Màn native có đếm ngược 3 giây trong vòng 36 dp ở góc trên phải; hết đếm ngược thì vòng đó thành nút đóng.
- [ ] Khi native không tải được thì nút đóng dùng được ngay và màn tự đóng khi hết đếm ngược.
- [ ] Khi đóng màn native thì người dùng đi tiếp tới màn đã chọn, và khoảng cách `ads_interval` tính từ lúc này.
- [ ] Khi khóa tắt thì interstitial đóng xong người dùng đi tiếp ngay.
- [ ] Khi interstitial không hiện (chưa có, chưa qua khoảng cách) thì người dùng đi tiếp ngay, không có native.

### KB-17. Thanh quảng cáo ở đáy màn hình

Tiêu chí chấp nhận:

- [ ] Khi bật native ở đáy thì đáy màn hình có native nhỏ 56 dp và lớp native thu gọn 250 dp nổi trên (KB-4).
- [ ] Khi tắt native ở đáy thì đáy màn hình có banner.
- [ ] Nội dung của màn hình chừa đúng chiều cao của thanh đáy khi thanh có quảng cáo, không chừa thừa hay thiếu.
- [ ] Doanh thu và lượt bấm của thanh đáy tính cho màn hình đang hiện, không cho màn hình đầu tiên.

### KB-18. Banner

Tiêu chí chấp nhận:

- [ ] Khi đơn vị banner bật tự làm mới trên AdMob thì banner tự đổi theo nhịp của AdMob, app không tự tải lại.
- [ ] Khi app ở nền thì banner không tải lại hay thử lại.
- [ ] Khi dùng banner thu gọn thì mỗi lần làm mới là một banner bung ra mới.
- [ ] Khi chưa có đồng ý thì banner không gửi yêu cầu.

### KB-19. Quảng cáo có thưởng

Người dùng chọn một hành động mở khóa hay xuất có thưởng.

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật rewarded thì hộp thoại hỏi xem quảng cáo hiện, có nút "Xem quảng cáo" và "Không, cảm ơn".
- [ ] Khi người dùng xem đủ thì nhận thưởng ngay lúc quảng cáo báo thưởng, không chờ đóng.
- [ ] Khi quảng cáo chưa sẵn sàng thì người dùng không nhận thưởng; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng.
- [ ] Khi người dùng đóng quảng cáo trước khi đủ thì không có thưởng.
- [ ] Khi bấm "Xem quảng cáo" hai lần liền thì chỉ một quảng cáo được gọi.
- [ ] Rewarded không bị chặn bởi khoảng cách `ads_interval` và không làm dời khoảng cách đó.

### KB-20. Native trên luồng giới thiệu

Một app có luồng giới thiệu (onboarding) nhiều trang.

Tiêu chí chấp nhận:

- [ ] Khi người vận hành bật native giới thiệu thì trang mà spec của app chọn có native cao 250 dp hoặc native toàn trang.
- [ ] Hai trang kề nhau không hiện cùng một quảng cáo.
- [ ] Native toàn trang không có nút điều khiển riêng; nút Tiếp của app vẫn ở chỗ quen thuộc và không bị quảng cáo che.
- [ ] Khi khóa tắt hay không có quảng cáo thì trang hiện bình thường, không có khoảng trống.

### KB-21. Native trong lưới và trang xem trước

Tiêu chí chấp nhận:

- [ ] Mỗi ô quảng cáo trong lưới hay trang xem trước có một quảng cáo riêng, dạng thẻ dọc.
- [ ] Khi không còn quảng cáo thì ô không hiện; quảng cáo về muộn vẫn hiện vào ô.
- [ ] Ô chỉ tính là đã được xem khi trang của nó đứng yên trên màn hình.

### KB-22. Ghi doanh thu theo vị trí

Người vận hành xem doanh thu quảng cáo.

Tiêu chí chấp nhận:

- [ ] Mỗi lượt hiển thị quảng cáo ghi một sự kiện doanh thu theo vị trí ở Firebase (`ad_impression_placement`) và ở AppsFlyer (`af_ad_revenue`), với định dạng, mạng, giá trị, vị trí.
- [ ] Doanh thu không bị tính hai lần: app không ghi `ad_impression` và không gửi `af_revenue`.
- [ ] Native hiện mà mạng không báo doanh thu trong 2 giây thì vẫn có một lượt ghi giá trị 0.
- [ ] Khi người vận hành bật số liệu chi tiết thì có thêm sự kiện tải, cơ hội hiển thị, lỗi hiển thị, lượt bấm.
- [ ] Bản staging không gửi gì sang AppsFlyer.

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
| **Thêm 2026-10-23:** quảng cáo mở app (chờ câu hỏi 11) | app open hoặc interstitial | Màn loading | Lúc khởi động mới | Không bị `ads_interval` chặn | `show_open_ads_first_open`, `show_open_ads`, `open_ad_type` |
| **Thêm 2026-10-23:** `resume` (chờ câu hỏi 11) | app open hoặc interstitial (có thể nối native) | Mọi màn | Khi quay lại app | Theo KB-14 | `show_resum_ads`, `resume_ad_type`, `is_show_native_after_inter` |

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
- **Thêm 2026-10-23:** SDK bên ngoài mới: adapter của tám mạng còn lại (Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads, kèm SDK Unity Ads) và SDK AppsFlyer (ADR-0026, ADR-0027). Quyền mà từng SDK kéo vào manifest gộp đo ở plan T-033 và T-034, ghi vào mục này trước khi duyệt plan (câu hỏi 18). Khóa SDK AppLovin và khóa dev AppsFlyer đưa vào lúc build, không vào repo.
- **Thêm 2026-10-23:** Play Console: An toàn dữ liệu khai đủ chín mạng và AppsFlyer (mã quảng cáo và mã thiết bị, tương tác với app và quảng cáo, chẩn đoán) trước khi phát hành bản có các SDK đó.

## 7. File tĩnh trên R2

Không có.

## 8. Cờ tính năng

Không có cờ tính năng mới. Khóa Remote Config mới (mặc định trong ngoặc):

| Khóa | Kiểu | Mặc định | Ý nghĩa |
| --- | --- | --- | --- |
| `ads_enabled` | boolean | bật | Công tắc tổng. **Đổi 2026-10-23:** chờ câu hỏi 7 |
| `ads_first_session_grace_seconds` | long | 180 | Ân hạn phiên đầu. **Đổi 2026-10-23:** bỏ (câu hỏi 10) |
| `ads_interstitial_max_per_session_count` | long | 4 | Tối đa quảng cáo toàn màn hình mỗi phiên. **Đổi 2026-10-23:** bỏ (câu hỏi 10) |
| `ads_inlist_every_count` | long | 8 | Native trong danh sách cách nhau. **Đổi 2026-10-23:** tên chờ câu hỏi 19 |
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
| `time_expand_collap_ad` | long (giây) | 30 | Native thu gọn tự mở lại sau khi người dùng thu gọn; 0 là không |
| `is_show_rw_get_item` | boolean | tắt | Rewarded ở hành động mở khóa hay xuất |
| `is_show_native_onboarding` | boolean | tắt | Native trên luồng giới thiệu |
| `is_show_<vị trí>` | boolean | tắt | Bật một vị trí của feature, thay `ads_<vị trí>_enabled` |
| `is_enable_consent_shortcut` | boolean | bật (câu hỏi 21) | Quyền tạm khi đồng ý chưa trả lời xong |
| `is_log_ad_fullscreen_events` | boolean | tắt | Số liệu chi tiết của quảng cáo toàn màn hình |
| `is_log_ad_slot_events` | boolean | tắt | Số liệu chi tiết của quảng cáo trong màn hình |

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
   - Trả lời:
8. **Khóa quảng cáo cá nhân hóa ở vùng có luật (`ads_personalized_regulated_regions_enabled`, ADR-0014) có giữ không? AdsKit không có.**
   - Đề xuất của AI: bỏ khóa, để chuỗi đồng ý của UMP quyết như AdsKit: người đồng ý nhận quảng cáo cá nhân hóa, người từ chối thì Google đọc lựa chọn từ chuỗi đồng ý.
   - Trả lời:
9. **Rào chắn "chờ người dùng ngừng chạm 1 giây" trước interstitial (ADR-0013) có giữ không? AdsKit không có.**
   - Đề xuất của AI: bỏ. Interstitial theo AdsKit gọi ngay ở cú bấm của người dùng, nên rào chắn này làm mọi interstitial trễ 1 giây.
   - Trả lời:
10. **Ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên (KB-6, T-009) bỏ theo PARTNER?**
    - Đề xuất của AI: bỏ như đã ghi ở KB-6; code đợt 2 của T-009 được gỡ ở T-019 và T-022.
    - Trả lời:
11. **Vỏ app của dự án mẫu làm những luồng nào?**
    - Đề xuất của AI: màn loading làm quảng cáo mở app (lần đầu và lần thường, KB-13); `:core:ads` làm quảng cáo khi quay lại (KB-14). Dự án mẫu không có luồng giới thiệu: chỉ cung cấp native giới thiệu trong `:core:ads` và trên màn xem thử; app nào có luồng giới thiệu thì spec của app đặt vị trí.
    - Trả lời:
12. **Thời gian chờ ở màn loading theo PARTNER (đồng ý 10 giây, SDK 35 giây, quảng cáo mở app 40 giây) dài hơn giới hạn chờ Remote Config của ADR-0018. Theo PARTNER?**
    - Đề xuất của AI: theo PARTNER khi khóa quảng cáo mở app bật; khi khóa tắt thì màn loading giữ giới hạn của ADR-0018. Ghi chú vào ADR-0018 khi làm T-024.
    - Trả lời:
13. **Có đặt vị trí rewarded trong `:feature:sample` không?**
    - Đề xuất của AI: không. Rewarded chỉ có trên màn xem thử ở staging; cách dùng ghi trong `ads-guide.md`. Màn mẫu giữ đơn giản cho người viết feature.
    - Trả lời:
14. **Mã đơn vị quảng cáo theo khe (như AdsKit) hay theo vị trí (như hiện tại)?**
    - Đề xuất của AI: theo khe như AdsKit (11 khe, `ads-business-rules.md` M-05); vị trí chỉ để bật tắt và ghi doanh thu. Ít mã phải tạo hơn, và pool, nạp sẵn dùng chung một khe. Cần sửa `scripts/release.sh` và `release.properties` (việc của người hoặc plan rủi ro cao được duyệt chéo).
    - Trả lời:
15. **Khóa dev AppsFlyer xử lý thế nào?**
    - Đề xuất của AI: như ADR-0027: production qua tham số build lúc phát hành; staging và CI không có khóa nên không gửi. Chưa cần app AppsFlyer riêng cho staging; khi cần thì thêm bí mật vào `staging.yml` bằng plan rủi ro cao.
    - Trả lời:
16. **Vẽ lại thiết kế native trên Figma theo kích thước AdsKit lúc nào?**
    - Đề xuất của AI: ticket T-035, làm trước các ticket code native của đợt 3 (T-027, T-030, T-032). AI dựng component theo kích thước AdsKit với token hiện có, người duyệt; ảnh chuẩn screenshot của T-010 tới T-013 ghi lại theo plan của ticket sửa.
    - Trả lời:
17. **Nút quay lại của máy trên màn native sau inter làm gì? AdsKit không xử lý.**
    - Đề xuất của AI: trong 3 giây đếm ngược thì không làm gì; sau đó đóng như nút đóng. Nạp lỗi thì đóng ngay.
    - Trả lời:
18. **Tám adapter mới kéo thêm quyền vào manifest. Gỡ quyền không cần như đã làm với AppLovin (`CHECK_LICENSE`, `ACCESS_WIFI_STATE`) không? AdsKit không gỡ quyền nào.**
    - Đề xuất của AI: giữ cách của T-015: plan T-034 liệt kê quyền từng adapter kéo vào, giữ quyền thường mà quảng cáo cần, gỡ quyền không cần cho quảng cáo; đưa danh sách vào mục 6 để anh xác nhận trước khi duyệt plan.
    - Trả lời:
19. **Native trong danh sách chèn mỗi N mục (KB-3): AdsKit không có khóa này (ô lưới do app đặt). Giữ không, và tên khóa là gì?**
    - Đề xuất của AI: giữ, tên `native_inlist_every` (số mục, mặc định 8) theo kiểu AdsKit; thay `ads_inlist_every_count`.
    - Trả lời:
20. **Khóa `ads_native_load_timeout_seconds` có giữ không? AdsKit co khe về 0 ngay khi nạp lỗi; native thu gọn chờ cố định 10 giây.**
    - Đề xuất của AI: bỏ, theo AdsKit.
    - Trả lời:
21. **Công tắc quyền tạm `is_enable_consent_shortcut` mặc định bật hay tắt? AdsKit mặc định tắt, app bật từ xa.**
    - Đề xuất của AI: mặc định bật, vì anh đã chọn khởi động sớm theo PARTNER; tắt từ xa khi có vấn đề về đồng ý.
    - Trả lời:

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
