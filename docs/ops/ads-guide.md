# HƯỚNG DẪN QUẢNG CÁO

Tài liệu này dành cho người phụ trách sản phẩm, dev và AI. Nó nói cách đặt, thử, phát hành và đo quảng cáo trong app của nhóm. Hành vi của từng định dạng (khi nào nạp, khi nào hiện, chờ bao lâu, ghi số liệu gì) nằm ở [ads-business-rules.md](ads-business-rules.md): bộ PARTNER của AdsKit, viết lại cho dự án mẫu (ADR-0026). Quyết định khác: ADR-0004, ADR-0013 (đồng ý, mã quảng cáo), ADR-0027 (AppsFlyer); cấu hình từ xa ở [remote-config.md](remote-config.md).

Các phần ghi **(đợt 3)** cần code của ticket T-019 tới T-036 (`docs/features/ads/tickets/`), chưa có trong `:core:ads`. Tới lúc đó, dùng những gì đợt 2 đã làm.

## 1. Nguyên tắc

1. **Hành vi theo một bộ luật.** Mọi app của nhóm dùng cùng luật PARTNER ở `ads-business-rules.md`. Không tự chế hành vi khác cho một app; muốn khác thì đề xuất sửa luật, kèm số liệu.
2. **Bật từ xa, mặc định tắt.** Mọi định dạng và vị trí có khóa Remote Config, mặc định tắt. Bật dần theo [remote-config.md](remote-config.md), mục 5.
3. **Vị trí là quyết định kinh doanh.** Chỉ thêm vị trí có trong spec đã duyệt. AI không tự thêm vị trí, định dạng hay sự kiện.
4. **Theo dõi chính sách.** Bộ PARTNER có các hành vi Google có thể coi là gây khó chịu (ADR-0026, Hệ quả). Người vận hành theo dõi trung tâm chính sách của AdMob và Play, tắt khóa của định dạng bị cảnh báo trước, tìm nguyên nhân sau.

## 2. Luôn giữ

Các quy tắc này vẫn giữ dưới bộ PARTNER (ADR-0026, mục 10). Không có ngoại lệ vì doanh thu. Gặp spec hay yêu cầu ngược với bảng này: không làm, ghi thành câu hỏi cho người phụ trách sản phẩm.

| Không làm | Vì sao |
| --- | --- |
| Quảng cáo giả làm nút hay mục của app; CTA của quảng cáo giống nút chính của app (cùng màu, cùng hình), hay nút của app giống CTA | Gây nhầm lẫn giữa nội dung và quảng cáo: chính sách về vị trí đặt quảng cáo của AdMob |
| Che, ẩn hay làm mờ nhãn "Ad" | Chính sách native của AdMob |
| Tự vẽ, che, thu nhỏ hay đè lên biểu tượng AdChoices | Chính sách native của AdMob |
| Kéo giãn hay cắt ảnh, video của quảng cáo | Chính sách native của AdMob |
| Lớp quảng cáo nổi chiếm cả màn, đẩy thanh điều hướng của app ra ngoài hay nhận chạm thay app | Gây bấm nhầm (luật D-03) |
| Hai quảng cáo toàn màn hình chồng nhau | Luật F-01 |
| Bấm vào quảng cáo của chính mình, nhờ người khác bấm | Lưu lượng không hợp lệ |
| Phát hành lại app bị đình chỉ bằng tên gói hay khóa ký khác | Play coi là lách chế tài, có thể khóa cả tài khoản |
| Dùng mã quảng cáo thật trên bản staging hay máy của dev; mã thật, khóa SDK (AppLovin...), khóa dev AppsFlyer trong repo | Hiển thị không hợp lệ; lộ bí mật (ADR-0013, ADR-0027) |

## 3. Định dạng

Chi tiết hành vi và kích thước ở `ads-business-rules.md`, mục 5. Khóa bật tắt ở mục 10 của tài liệu đó.

| Định dạng | Dùng ở đâu | Khóa bật | Có trong `:core:ads` |
| --- | --- | --- | --- |
| App open lúc mở app | Màn loading, lần đầu sau khi cài và lần thường | `show_open_ads_first_open`, `show_open_ads`, `open_ad_type` | (đợt 3) |
| Quảng cáo khi quay lại app | Mọi lần app lên foreground, trừ lần đầu của process | `show_resum_ads`, `resume_ad_type` | (đợt 3) |
| Interstitial | Hành động của người dùng (sang màn sau, xong một việc) | `is_show_<vị trí>`, cách nhau `ads_interval` | Có (đợt 2), luật mới ở đợt 3 |
| Native toàn màn hình sau inter (combo) | Ngay sau inter, đếm ngược 3 giây | `is_show_native_after_inter` | (đợt 3) |
| Thanh đáy: native nhỏ 56 dp và native thu gọn 250 dp, hoặc banner | Đáy màn hình nội dung | `is_show_native_bottom`, `time_expand_collap_ad` | (đợt 3) |
| Banner cố định, banner thu gọn | Đáy màn hình | `is_show_<vị trí>` | Banner cố định có (đợt 2); làm mới theo đơn vị ở đợt 3 |
| Native medium | Trong nội dung | `is_show_<vị trí>` | Có (đợt 2), kích thước mới ở đợt 3 |
| Native ô lưới, trang xem trước | Trong lưới hay pager, mỗi ô một quảng cáo riêng | `is_show_<vị trí>` | (đợt 3) |
| Native giới thiệu | Luồng giới thiệu (onboarding) | `is_show_native_onboarding` | (đợt 3) |
| Rewarded với hộp thoại | Hành động mở khóa hay xuất do người dùng chọn | `is_show_rw_get_item` | (đợt 3) |

## 4. Native: kích thước và hình thức

- Kích thước theo layout của AdsKit (`ads-business-rules.md`, mục 5.7): khe đáy cao 56 dp, native thu gọn 250 dp với nút thu gọn 32 dp, native giới thiệu 250 dp, nút đóng native sau inter 36 dp ở góc trên phải.
- Màu, chữ, bo góc lấy từ token của `:core:designsystem` (ADR-0008), không lấy màu của AdsKit. Màu chữ native theo nền của vùng quảng cáo, không đổi theo chế độ tối của máy khi nền không đổi.
- Thiết kế native trên Figma đang theo cỡ cũ của đợt 2 (`docs/features/ads/design/`); vẽ lại theo cỡ AdsKit ở T-035. Tới lúc đó, khi thiết kế và `ads-business-rules.md` khác nhau về kích thước, theo `ads-business-rules.md`.
- Mỗi lượt hiện một quảng cáo riêng; rời màn thì hủy (luật O-01, O-03).

## 5. Thêm một vị trí quảng cáo

Vị trí là quyết định kinh doanh: chỉ thêm khi spec đã duyệt có vị trí đó (ADR-0004).

1. **Spec:** mục Quảng cáo ghi vị trí, định dạng, màn hình, thời điểm, khóa bật tắt. Thiết kế đánh dấu vùng quảng cáo trên ảnh màn hình.
2. **Khai báo:** thêm một dòng vào `AdPlacement` (`core/ads`), đúng dạng `TEN_VI_TRI("ten_vi_tri", AdFormat.X),` trên một dòng: `scripts/release.sh` đọc danh sách vị trí bằng mẫu này và đòi mã production cho mọi vị trí, trừ vị trí có khóa bắt đầu bằng `gallery_` (chỉ dùng cho màn hình xem thử ở staging, không bao giờ phát hành). Mã đơn vị theo vị trí hay theo khe như AdsKit là câu hỏi mở của spec ads; đổi cách này cần sửa `release.sh` (việc của người).
3. **Khóa bật tắt:** vị trí dùng khe chung đã có khóa AdsKit (thanh đáy, combo, resume, app open, rewarded, giới thiệu) thì dùng khóa đó. Vị trí khác: `is_show_<ten_vi_tri>`, mặc định tắt (đợt 3; tới lúc đó là `ads_<ten_vi_tri>_enabled` của đợt 2). Khóa riêng khác theo [remote-config.md](remote-config.md).
4. **Màn hình:** đặt composable của `:core:ads` (banner, native, thanh đáy), hoặc gọi `rememberShowInterstitial(placement)` ở hành động của người dùng. Feature không gọi SDK quảng cáo. Màn hình mở màn khác của hệ thống (chọn ảnh, camera, chia sẻ) dùng cách chặn quảng cáo tự hiện của `:core:ads` (`ads-business-rules.md`, mục 6).
5. **Test:** screenshot test của màn hình không có quảng cáo (`LocalAdsController` mặc định rỗng); test ViewModel cho điểm gọi interstitial nếu có logic.
6. **Mã quảng cáo production:** tạo đơn vị quảng cáo trên AdMob, ghi vào `release.properties` lúc phát hành ([release.md](release.md)); không vào repo.
7. **Bật:** sau khi bản production có vị trí đã lên Play, người phụ trách bật khóa trên Firebase theo [remote-config.md](remote-config.md), mục 5.

## 6. Nhịp hiển thị

Luật đầy đủ ở `ads-business-rules.md`, mục 3 và 4. Tóm tắt cho người vận hành:

- `ads_interval` (mặc định 45 giây): khoảng tối thiểu từ lúc inter gần nhất rời màn (hoặc cả cụm inter và native) tới inter sau. Chỉ tính trong một lần mở app; mở lại app thì bắt đầu lại. App open và rewarded không tính.
- App open lúc mở app và inter lúc mở app (`open_ad_type = inter`) không bị `ads_interval` chặn; inter lúc mở app vẫn đẩy mốc.
- Quảng cáo khi quay lại app không có khoảng chờ thời gian; bị bỏ khi người dùng vừa bấm quảng cáo, vừa mở màn của hệ thống hay rời app có chủ đích.
- Banner làm mới theo cài đặt tự làm mới của đơn vị banner trên AdMob; native ở đáy và native thu gọn thay nội dung mỗi 60 giây khi app ở foreground.

## 7. Mediation: AdMob và chín mạng (đợt 3)

- AdMob điều phối. Mạng con qua adapter chính thức của AdMob: AppLovin, Meta Audience Network, DT Exchange (Fyber), InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads (ADR-0026). Đợt 2 có AppLovin (T-015); tám mạng còn lại ở T-034.
- Thêm adapter là thay đổi thư viện: plan rủi ro cao, so quyền trong manifest gộp trước và sau.
- Khóa SDK AppLovin là cấu hình của app, đưa vào lúc build như mã quảng cáo. Không đặt trong resource hay trong repo.
- Khai báo mạng trong nhóm mediation của từng đơn vị quảng cáo trên AdMob (việc của người).
- Kiểm bằng Ad Inspector của AdMob trên bản staging (mã thử): mỗi adapter phải báo đã khởi tạo.
- Hướng dẫn chính thức: https://developers.google.com/admob/android/mediation (mỗi mạng một trang con).

## 8. Thử trên staging

- Staging luôn dùng mã thử của Google theo định dạng (inter, banner, rewarded, app open, native; ADR-0013).
- Giá trị Remote Config cục bộ của staging (T-008) bật mọi khóa quảng cáo để thấy mọi định dạng mà không cần project Firebase thật.
- Hộp thoại đồng ý: staging giả lập vùng EEA để thấy hộp thoại. Thử thêm: tắt mạng khi mở app (quyền tạm `ERROR_ALLOWED`), mở lại app sau khi đã đồng ý (`CACHED_EARLY`), rút đồng ý bằng nút quyền riêng tư.
- Màn hình xem thử quảng cáo (chỉ staging, T-014; định dạng mới ở T-036) gọi từng định dạng và mở Ad Inspector.
- Kiểm trên máy thật theo mục 2: nhãn "Ad", AdChoices, nút đóng và đếm ngược, thu gọn và bung lại, thanh đáy không che điều hướng của app.
- Xem log `adb logcat -v threadtime | grep ' VGL\.'`: mỗi quyết định cổng có một dòng (ADR-0019).

## 9. Trước khi phát hành

- [ ] Mỗi vị trí trong `AdPlacement` có mã production trong `release.properties` (`release.sh` kiểm).
- [ ] Khóa SDK AppLovin và khóa dev AppsFlyer có trong trình quản lý mật khẩu, đưa vào lúc build (ADR-0027).
- [ ] `app-ads.txt` trên website của nhà phát triển có dòng của AdMob và của cả chín mạng con; lấy dòng từ bảng điều khiển của từng mạng. Thiếu dòng thì mạng đó có thể không trả quảng cáo.
- [ ] Hộp thoại đồng ý đã cấu hình trên AdMob (Privacy & messaging) cho các vùng cần, có danh sách đối tác quảng cáo gồm chín mạng.
- [ ] Liên kết AdMob với Firebase (để có `ad_impression` tự động); AppsFlyer đã nhận sự kiện từ bản phát hành thử.
- [ ] **Data safety** trên Play Console khai đủ SDK đang dùng: Google Mobile Ads, chín mạng con, AppsFlyer, Firebase. Các mạng quảng cáo thường thu thập: mã quảng cáo và mã thiết bị khác, tương tác với quảng cáo và app, chẩn đoán, vị trí gần đúng suy từ IP. Tra trang công bố dữ liệu của từng mạng lúc làm T-034 và ghi kết quả vào plan của ticket đó; khai đúng mục đích (quảng cáo, phân tích, chống gian lận).
- [ ] Rà từng vị trí theo mục 2 trên bản phát hành.
- [ ] Mọi khóa quảng cáo vẫn tắt trên Remote Config tới khi bản phát hành lên Play; bật dần theo [remote-config.md](remote-config.md).

## 10. Đo lường

- **Firebase:** liên kết AdMob với Firebase thì `ad_impression` và doanh thu được ghi tự động. App tự ghi `ad_impression_placement` (doanh thu theo vị trí, không đếm đôi với `ad_impression`) và, khi bật khóa, số liệu vòng đời `ad_load_*`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, `ad_reward_earned`, `reward_dialog` (`ads-business-rules.md`, mục 8.2). Kiểm bằng DebugView.
- **AppsFlyer:** `af_ad_revenue` cho mọi lượt hiển thị, phễu `af_inters_*`, `af_rewarded_*`, `af_native_*`, `af_inters_displayed_{N}_times`. Không có `af_revenue` (ADR-0027).
- **Chỉ số:** ARPDAU; doanh thu theo vị trí và định dạng; số lượt hiển thị mỗi người dùng mỗi ngày; tỷ lệ lấp đầy và eCPM theo định dạng, quốc gia, mạng; tỷ lệ `shown` trên cơ hội (`ad_opportunity`). Luôn xem cùng giữ chân D1, D7 và điểm Play: tăng ARPDAU mà giữ chân hay điểm giảm là dấu hiệu quảng cáo quá dày.
- **Báo cáo vận hành** (`docs/templates/ops-report.md`) ghi các chỉ số này theo kỳ, kèm mọi lần đổi cấu hình quảng cáo trong kỳ.

## Nguồn

Tra ngày 2026-10-21 và 2026-10-23; chính sách có thể đổi, đối chiếu lại trước khi dựa vào một chi tiết.

- Luật nghiệp vụ và dẫn chứng AdsKit: [ads-business-rules.md](ads-business-rules.md)
- Native advanced (Android): https://developers.google.com/admob/android/native/advanced
- Native toàn màn hình: https://developers.google.com/admob/android/native/full-screen
- App open: https://developers.google.com/admob/android/app-open
- Banner thu gọn: https://developers.google.com/admob/android/banner/collapsible
- Đo doanh thu quảng cáo bằng Firebase: https://firebase.google.com/docs/analytics/measure-ad-revenue
- Mediation của AdMob: https://developers.google.com/admob/android/mediation
- Chính sách chương trình AdMob: https://support.google.com/admob/answer/6128543
- Interstitial không được phép (lúc mở, lúc thoát app, lặp lại): https://support.google.com/admob/answer/6201362
- Chính sách quảng cáo của Google Play: https://support.google.com/googleplay/android-developer/answer/9857753
