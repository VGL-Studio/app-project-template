# HƯỚNG DẪN QUẢNG CÁO

Tài liệu này dành cho người phụ trách sản phẩm, dev và AI. Nó nói cách chọn, đặt, thử và đo quảng cáo trong app của nhóm. Quyết định nằm ở ADR-0004, ADR-0013, ADR-0014 và ADR-0022; cấu hình từ xa ở [remote-config.md](remote-config.md) (ADR-0023). Repo `vgl-docs` và file "Quy chuẩn Native" cũ chỉ để tham khảo; khi khác tài liệu này thì theo tài liệu này.

Các phần ghi **(đợt 2)** cần code chưa có trong `:core:ads` (native, tần suất lưu qua phiên, công tắc tổng); tới lúc đó chỉ dùng banner và interstitial.

## 1. Nguyên tắc

1. **Chính sách trước doanh thu.** Tài khoản AdMob hay Play bị hạn chế thì mất doanh thu của mọi app. Không có ngoại lệ cho bảng ở mục 2.
2. **Người dùng mới được bảo vệ.** Luồng mở đầu lần đầu và thời gian ân hạn của phiên đầu không có quảng cáo toàn màn hình. Người dùng thấy giá trị của app trước, rồi mới thấy quảng cáo.
3. **Màn hình chức năng chính không có quảng cáo.** Spec ghi màn hình nào là chức năng chính (ví dụ camera của app chụp ảnh).
4. **Quảng cáo toàn màn hình chỉ ở điểm dừng tự nhiên:** sau khi người dùng xong một việc và trước việc tiếp theo; không khi mở hay đóng app, không giữa một thao tác.
5. **Mọi thứ chỉnh được từ xa và mặc định tắt** (ADR-0014): từng vị trí, tần suất, công tắc tổng.

## 2. Không bao giờ làm

| Không làm | Vì sao |
| --- | --- |
| Đặt quảng cáo sát nút của app, ở chỗ người dùng sắp chạm, hay hiện lên đúng lúc người dùng đang chạm | Gây bấm nhầm: chính sách về vị trí đặt quảng cáo của AdMob |
| CTA của quảng cáo giống nút của app (cùng màu, cùng hình), hay nút của app giống CTA | Gây nhầm lẫn giữa nội dung và quảng cáo |
| Native, banner nằm ngay đầu danh sách lựa chọn (ngôn ngữ, cài đặt) như một mục của danh sách | Gây nhầm lẫn và bấm nhầm |
| Quảng cáo toàn màn hình lúc mở app, lúc thoát app, hay trước khi người dùng làm xong hành động chính lần đầu | Quảng cáo bất ngờ, gây khó chịu: chính sách quảng cáo của Play |
| Ép chờ trước khi đóng được quảng cáo, nút đóng nhỏ hay khó thấy | Như trên |
| Hai quảng cáo toàn màn hình liền nhau (ví dụ native toàn màn hình ngay sau interstitial) | Như trên |
| Tự vẽ, che, thu nhỏ hay đè lên biểu tượng AdChoices và nhãn quảng cáo | Chính sách native của AdMob |
| Kéo giãn, cắt ảnh hay video của quảng cáo | Chính sách native của AdMob |
| Native thu gọn tự bung lại, banner tự làm mới nhanh hơn mức của AdMob | Gây bấm nhầm, hiển thị không hợp lệ |
| Đặt quảng cáo trong màn hình không có nội dung của app (màn hình chỉ có quảng cáo), trừ định dạng toàn màn hình hợp lệ | Chính sách AdMob |
| Bấm vào quảng cáo của chính mình, nhờ người khác bấm | Lưu lượng không hợp lệ |
| Phát hành lại app bị đình chỉ bằng tên gói hay khóa ký khác | Play coi là lách chế tài, có thể khóa cả tài khoản |
| Dùng mã quảng cáo thật trên bản staging hay máy của dev | Hiển thị không hợp lệ (ADR-0013) |

Gặp spec hay yêu cầu ngược với bảng này: không làm, ghi thành câu hỏi cho người phụ trách sản phẩm.

## 3. Định dạng

| Định dạng | Thay cho | Dùng ở đâu | Có trong `:core:ads` |
| --- | --- | --- | --- |
| Banner thích ứng | | Đáy màn hình danh sách, chi tiết | Có |
| Interstitial | | Điểm dừng tự nhiên | Có |
| Native small | Banner | Đáy hay giữa màn hình nội dung | (đợt 2) |
| Native medium | MREC | Trong nội dung, giữa các khối | (đợt 2) |
| Native in-list | | Trong danh sách hay lưới, cách nhau N mục | (đợt 2) |
| Native collapsible | Banner thu gọn | Đáy màn hình; thu gọn khi người dùng chạm | (đợt 2) |
| Native toàn màn hình | Interstitial | Điểm dừng tự nhiên, như interstitial | (đợt 2) |
| App open, rewarded | | Chưa làm; cần ADR mới khi một app cần | Không |

Chọn định dạng theo màn hình, không theo doanh thu từng định dạng: màn hình người dùng ở lâu (danh sách, chi tiết) hợp native small hay medium; điểm dừng giữa hai việc hợp interstitial.

## 4. Quy chuẩn native (đợt 2)

Tính bằng dp. Mỗi cỡ là một component trong hệ thống thiết kế (Figma và `:core:ads`), dùng màu, chữ, bo góc của `:core:designsystem` để giống giao diện app (ADR-0008, ADR-0022). Số đo cụ thể chốt khi dựng component; bảng dưới là quy tắc phải giữ.

| Cỡ | Thành phần | Quy tắc |
| --- | --- | --- |
| Small | Icon, tiêu đề, CTA, nhãn "Ad", AdChoices; mô tả hay điểm sao nếu có chỗ | Rộng hết vùng nội dung; không có media |
| Medium | Như small, thêm media và mô tả | MediaView ít nhất 120 x 120 dp; giữ tỷ lệ ảnh, video của quảng cáo |
| In-list | Small hay medium, cùng bề ngoài với mục của danh sách nhưng có nhãn "Ad" rõ | Không ở mục đầu; cách nhau ít nhất N mục (khóa Remote Config) |
| Collapsible | Medium khi mở, small khi thu gọn | Chỉ thu gọn khi người dùng chạm nút thu gọn; nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices; không tự bung lại |
| Toàn màn hình | Media lớn, icon, tiêu đề, mô tả, CTA, nhãn "Ad", AdChoices, nút đóng | Nút đóng thấy và bấm được ngay từ đầu, ít nhất 48 dp, ở góc khác AdChoices và xa CTA; nút quay lại của máy cũng đóng; không đếm ngược bắt chờ; tính chung giới hạn tần suất với interstitial; không liền sau một quảng cáo toàn màn hình khác |

Quy tắc chung:

- Nhãn "Ad" và AdChoices luôn thấy được, không bị đè.
- CTA khác hẳn nút chính của app (màu, hình dạng); vùng quảng cáo có ranh giới nhìn thấy được (nền, viền).
- Cách mọi phần tử bấm được của app ít nhất 16 dp.
- Đang tải: giữ chỗ đúng chiều cao, có hiệu ứng chờ, để bố cục không nhảy.
- Không có quảng cáo: thu chỗ lại sau thời gian chờ; nếu vị trí nằm cạnh nút của app thì giữ chỗ trống để nút không dịch dưới tay người dùng.
- Chế độ tối dùng màu tối của hệ thống thiết kế.
- Hủy (`destroy`) quảng cáo cũ khi thay hay khi rời màn hình.

## 5. Thêm một vị trí quảng cáo

Vị trí là quyết định kinh doanh: chỉ thêm khi spec đã duyệt có vị trí đó (ADR-0004).

1. **Spec:** mục Quảng cáo ghi vị trí, định dạng, màn hình, thời điểm, giới hạn tần suất, khóa bật tắt. Thiết kế đánh dấu vùng quảng cáo trên ảnh màn hình.
2. **Khai báo:** thêm một dòng vào `AdPlacement` (`core/ads`), đúng dạng `TEN_VI_TRI("ten_vi_tri", AdFormat.X),` trên một dòng: `scripts/release.sh` đọc danh sách vị trí bằng mẫu này và đòi mã production cho mọi vị trí, trừ vị trí có khóa bắt đầu bằng `gallery_` (chỉ dùng cho màn hình xem thử ở staging, không bao giờ phát hành).
3. **Khóa bật tắt:** `ads_<ten_vi_tri>_enabled`, mặc định tắt (ADR-0014), tự có qua `AdsSettings.placementEnabled` và tự vào bảng khóa qua `AdsSettings.all()`. Sinh lại bảng khóa bằng lệnh ở [remote-config.md](remote-config.md), mục 3, trong cùng PR. Tham số riêng của vị trí theo [remote-config.md](remote-config.md).
4. **Màn hình:** đặt `AdSlot(placement)` (banner) hay composable native của `:core:ads` (đợt 2), hoặc `rememberShowInterstitial(placement)` gọi ở điểm dừng tự nhiên. Feature không gọi SDK quảng cáo.
5. **Test:** screenshot test của màn hình không có quảng cáo (`LocalAdsController` mặc định rỗng); test ViewModel cho điểm gọi interstitial nếu có logic.
6. **Mã quảng cáo production:** tạo đơn vị quảng cáo trên AdMob, ghi vào `release.properties` lúc phát hành ([release.md](release.md)); không vào repo.
7. **Bật:** sau khi bản production có vị trí đã lên Play, người phụ trách bật `ads_<ten_vi_tri>_enabled` trên Firebase theo [remote-config.md](remote-config.md), mục 5.

## 6. Tần suất và thời điểm

Giá trị đề xuất ban đầu; chỉnh bằng Remote Config theo số liệu.

| Quy tắc | Khóa | Đề xuất |
| --- | --- | --- |
| Khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình | `ads_interstitial_min_interval_seconds` (đã có) | 45 giây, nâng lên nếu giữ chân giảm |
| Ân hạn phiên đầu: không quảng cáo toàn màn hình trong khoảng này sau lần mở đầu tiên | `ads_first_session_grace_seconds` | 180 giây |
| Tối đa quảng cáo toàn màn hình mỗi phiên | `ads_interstitial_max_per_session_count` | 4 |
| Native in-list cách nhau (đợt 2) | `ads_inlist_every_count` | 8 mục |
| Công tắc tổng | `ads_enabled` | bật |

Khoảng cách và ân hạn lưu trên máy, tính qua các phiên (ADR-0022). Một phiên bắt đầu khi app khởi động, và bắt đầu lại khi app trở lại tiền cảnh sau ít nhất 30 phút ở nền (giống phiên của Firebase Analytics); số quảng cáo toàn màn hình của phiên chỉ đếm trong bộ nhớ. Banner làm mới theo cài đặt của đơn vị quảng cáo trên AdMob, không đặt dưới 60 giây; native không tự làm mới, chỉ tải mới khi người dùng vào lại màn hình.

## 7. Mediation: AdMob và AppLovin

- AdMob điều phối, AppLovin là mạng con qua adapter chính thức của AdMob, bidding (ADR-0022, thay phần mạng con của ADR-0004).
- **Thư viện:** `com.google.ads.mediation:applovin:13.6.4.2` (khóa `admob-mediation-applovin` trong `gradle/libs.versions.toml`), kéo theo `com.applovin:applovin-sdk:13.6.4`. Chỉ `:core:ads` phụ thuộc adapter; feature không thấy nó.
- **Không cần code khởi tạo:** `MobileAds.initialize`, gọi sau khi có đồng ý (ADR-0013), khởi tạo mọi adapter. Đồng ý GDPR từ UMP được chuyển sang AppLovin tự động.
- **Khóa SDK AppLovin không vào app:** với bidding, khóa nhập trong hộp thoại ánh xạ nguồn quảng cáo trên AdMob. Không đặt khóa trong `AndroidManifest.xml` hay bất kỳ file nào của repo.
- **Việc của người trên AdMob:** tạo nhóm mediation cho từng đơn vị quảng cáo, thêm nguồn "AppLovin (Bidding)" và ánh xạ với tài khoản AppLovin; thêm "AppLovin Corp." vào danh sách đối tác quảng cáo của thông điệp GDPR và thông điệp luật các bang Mỹ (Privacy & messaging); `app-ads.txt` trên website của nhà phát triển có dòng của AdMob và AppLovin ([release.md](release.md), mục 1.4).
- **Manifest:** SDK AppLovin gộp vào app ba quyền thường (`VIBRATE`, `ACCESS_WIFI_STATE`, `com.android.vending.CHECK_LICENSE`), các activity và service của nó, và `com.applovin.sdk.AppLovinInitProvider` chạy lúc tiến trình khởi động. Dự án mẫu bỏ ba phần tử trong `core/ads/src/main/AndroidManifest.xml` bằng `tools:node="remove"` (plan T-015):
  - `AppLovinInitProvider`: provider đọc Advertising ID và App Set ID lúc khởi động, trước khi có đồng ý. Bỏ đi thì không có mã AppLovin nào chạy trước `MobileAds.initialize` (ADR-0004). AppLovin không ghi cách này trong tài liệu; mỗi lần nâng SDK phải kiểm lại trên máy.
  - `CHECK_LICENSE` và `ACCESS_WIFI_STATE`: bớt quyền; ảnh hưởng tới chống gian lận và đo lường hiển thị của AppLovin chưa rõ, theo dõi doanh thu khi có số liệu thật.
  - `VIBRATE` giữ lại: SDK gọi rung không kiểm quyền và không bắt lỗi, bỏ quyền có thể làm app dừng.
  - Một app muốn chọn khác thì thêm dòng trong manifest của chính app (ưu tiên cao hơn thư viện). `MergedManifestTest` của `:app` kiểm manifest gộp của dự án mẫu.
- **Data safety:** khai báo AppLovin là bên nhận dữ liệu (định danh quảng cáo, thông tin máy). Quyền mà mediation thêm vào manifest gộp: `VIBRATE`.
- **Kiểm tra trên staging:** sau khi đồng ý, logcat (`adb logcat -v threadtime | grep ' VGL\.'`, thẻ `Ads`) có dòng `Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: READY`, ghi khi `MobileAds.initialize` báo xong. Trước khi đồng ý không có dòng `Adapter ...` nào và không có log khởi tạo của thẻ `AppLovinSdk`. Trên app có đơn vị quảng cáo thật: Ad Inspector, chế độ "single ad source testing" với "AppLovin (Bidding)", sau khi đăng ký máy thử trên AdMob và bật chế độ thử trên AppLovin.
- **Nâng cấp adapter** là thay đổi thư viện: plan rủi ro cao. Nâng `play-services-ads` thì nâng adapter trong cùng plan, chọn bản adapter có ghi chú "Built and tested with" khớp bản Google Mobile Ads mới; kiểm lại tên provider và quyền trong manifest của AAR.
- Hướng dẫn chính thức: https://developers.google.com/admob/android/mediation/applovin

## 8. Thử trên staging

- Staging luôn dùng mã thử của Google (ADR-0013).
- Bản staging dùng Firebase giả nên Remote Config không trả gì: staging bật mọi vị trí của `AdPlacement` bằng giá trị cục bộ (`StagingRemoteValuesModule`); Firebase staging vẫn tắt được từng vị trí.
- Hộp thoại đồng ý: staging giả lập vùng EEA để thấy hộp thoại.
- Kiểm theo mục 4 và mục 2 trên máy thật: nhãn, AdChoices, khoảng cách tới nút, giữ chỗ khi tải, khi không có quảng cáo, chế độ tối.

## 9. Trước khi phát hành

- [ ] Mỗi vị trí trong `AdPlacement` có mã production trong `release.properties` (`release.sh` kiểm).
- [ ] `app-ads.txt` đủ dòng cho mọi mạng đang dùng.
- [ ] Hộp thoại đồng ý đã cấu hình trên AdMob (Privacy & messaging) cho các vùng cần, có "AppLovin Corp." trong danh sách đối tác quảng cáo (mục 7).
- [ ] Tắt chế độ thử trên AppLovin và AdMob (máy thử, single ad source testing) trước khi phát hành.
- [ ] Liên kết AdMob với Firebase (để có `ad_impression` và doanh thu, mục 10).
- [ ] Rà từng vị trí theo mục 2 trên bản phát hành.
- [ ] Mọi vị trí vẫn tắt trên Remote Config tới khi bản phát hành lên Play; bật dần theo [remote-config.md](remote-config.md).

## 10. Đo lường

- **Sự kiện:** liên kết AdMob với Firebase thì `ad_impression`, `ad_click` và doanh thu được ghi tự động. App không tự ghi sự kiện hiển thị quảng cáo (ADR-0022). Kiểm bằng DebugView của Firebase.
- **Chỉ số:** ARPDAU (doanh thu quảng cáo mỗi người dùng hoạt động mỗi ngày); số lượt hiển thị mỗi người dùng mỗi ngày theo định dạng; tỷ lệ lấp đầy và eCPM theo định dạng, quốc gia; tỷ lệ hiển thị so với yêu cầu. Luôn xem cùng giữ chân D1, D7 và điểm Play: tăng ARPDAU mà giữ chân hay điểm giảm là dấu hiệu quảng cáo quá dày.
- **Báo cáo vận hành** (`docs/templates/ops-report.md`) ghi các chỉ số này theo kỳ, kèm mọi lần đổi cấu hình quảng cáo trong kỳ.

## Nguồn

Tra ngày 2026-10-21; chính sách có thể đổi, đối chiếu lại trước khi dựa vào một chi tiết.

- Native advanced (Android): https://developers.google.com/admob/android/native/advanced
- Native toàn màn hình: https://developers.google.com/admob/android/native/full-screen
- MediaView tối thiểu 120 x 120 dp cho video: https://support.google.com/admanager/answer/7031536
- Đo doanh thu quảng cáo bằng Firebase: https://firebase.google.com/docs/analytics/measure-ad-revenue
- Hiệu quả quảng cáo AdMob trong Firebase: https://support.google.com/firebase/answer/7378163
- Mediation AppLovin: https://developers.google.com/admob/android/mediation/applovin
- Ad Inspector: https://developers.google.com/admob/android/ad-inspector
- Chính sách chương trình AdMob: https://support.google.com/admob/answer/6128543
- Interstitial không được phép (lúc mở, lúc thoát app, lặp lại): https://support.google.com/admob/answer/6201362
- Chính sách quảng cáo của Google Play: https://support.google.com/googleplay/android-developer/answer/9857753
