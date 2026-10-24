# HƯỚNG DẪN QUẢNG CÁO

Tài liệu này dành cho người phụ trách sản phẩm, dev và AI. Nó nói cách đặt, thử, phát hành và đo quảng cáo trong app của nhóm. Hành vi của từng định dạng (khi nào nạp, khi nào hiện, chờ bao lâu, ghi số liệu gì) nằm ở [ads-business-rules.md](ads-business-rules.md): bộ PARTNER của AdsKit, viết lại cho dự án mẫu (ADR-0026). Quyết định khác: ADR-0004, ADR-0013 (đồng ý, mã quảng cáo), ADR-0027 (AppsFlyer); cấu hình từ xa ở [remote-config.md](remote-config.md).

Các phần ghi **(đợt 3)** cần code của ticket T-019 tới T-036 (`docs/features/ads/tickets/`), chưa có trong `:core:ads`. Tới lúc đó, dùng những gì đợt 2 đã làm.

Đợt 2 đã có trong `:core:ads` và dùng được ngay: banner, interstitial, native small, native medium, native trong danh sách và lưới, native thu gọn, native toàn màn hình, màn hình xem thử của staging. Các phần ghi "Hiện có (đợt 2)" nói cách dùng chúng cho tới khi ticket đợt 3 thay.

## 1. Nguyên tắc

1. **Hành vi theo một bộ luật.** Mọi app của nhóm dùng cùng luật PARTNER ở `ads-business-rules.md`. Không tự chế hành vi khác cho một app; muốn khác thì đề xuất sửa luật, kèm số liệu.
2. **Cơ chế chung, vị trí riêng.** `:core:ads` chỉ có cơ chế (định dạng, cổng, nhóm giãn cách, hành động có điều kiện). Tính năng của từng app (nhận vật phẩm, xuất file, giới thiệu, lưới ảnh) là vị trí do app khai báo, không phải code trong `:core:ads` (`ads-business-rules.md`, mục Mô hình chung).
3. **Bật từ xa, mặc định tắt.** Mọi định dạng và vị trí có khóa Remote Config, mặc định tắt. Bật dần theo [remote-config.md](remote-config.md), mục 5.
4. **Vị trí là quyết định kinh doanh.** Chỉ thêm vị trí có trong spec đã duyệt. AI không tự thêm vị trí, định dạng hay sự kiện.
5. **Theo dõi chính sách.** Bộ PARTNER có các hành vi Google có thể coi là gây khó chịu (ADR-0026, Hệ quả). Người vận hành theo dõi trung tâm chính sách của AdMob và Play, tắt khóa của định dạng bị cảnh báo trước, tìm nguyên nhân sau.

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
| Interstitial | Hành động của người dùng (sang màn sau, xong một việc) | `is_show_<mã vị trí>`, cách nhau `ads_interval` hay `ads_interval_<nhóm>` | Có (đợt 2, `rememberShowFullScreenAd`), luật mới ở đợt 3 |
| Native toàn màn hình sau inter (combo) | Ngay sau inter, đếm ngược 3 giây | `is_show_native_after_inter` | Native toàn màn hình đứng riêng có (đợt 2, `rememberShowFullScreenAd`); combo ở đợt 3 |
| Thanh đáy: native nhỏ 80 dp và native thu gọn 250 dp, hoặc banner | Đáy màn hình nội dung, mỗi màn một vị trí | `is_show_<mã vị trí>`, `is_show_native_bottom`, `is_show_native_collap`, `time_expand_collap_ad` | Native small (`NativeAdSlot`) và native thu gọn (`CollapsibleNativeAdSlot`) có ở cỡ đợt 2; 80 dp, 250 dp và làm mới ở đợt 3 |
| Banner cố định, banner thu gọn | Đáy màn hình | `is_show_<mã vị trí>` | Banner cố định có (đợt 2); làm mới theo đơn vị ở đợt 3 |
| Native medium | Trong nội dung | `is_show_<mã vị trí>` | Có (đợt 2), kích thước mới ở đợt 3 |
| Native trong tập hợp (ô lưới, danh sách, trang của pager) | Mỗi ô một quảng cáo riêng | `is_show_<mã vị trí>` | Danh sách và lưới có (đợt 2, `itemsWithInListAds`); pager ở đợt 3 |
| Native ở một bước của luồng (giới thiệu, kết quả, hướng dẫn) | Bước do spec của app chọn | `is_show_<mã vị trí>` (luồng giới thiệu: `is_show_native_onboarding`) | (đợt 3) |
| Rewarded với hộp thoại (hành động có điều kiện) | Trước một hành động người dùng chọn: nhận vật phẩm, xuất file, mở khóa | `is_show_<mã vị trí>` (ví dụ `is_show_rw_get_item`) | (đợt 3) |

## 4. Native: kích thước và hình thức

- Kích thước theo layout của AdsKit và bản đặc tả độc lập (`ads-business-rules.md`, mục 5.7): khe đáy cao 80 dp (cả vẽ và chừa chỗ), native thu gọn 250 dp với nút thu gọn 32 dp, thẻ native ở bước của luồng 250 dp, nút đóng native sau inter 36 dp ở góc trên phải.
- Màu, chữ, bo góc lấy từ token của `:core:designsystem` (ADR-0008), không lấy màu của AdsKit. Màu chữ native theo nền của vùng quảng cáo, không đổi theo chế độ tối của máy khi nền không đổi.
- Thiết kế native trên Figma đang theo cỡ cũ của đợt 2 (`docs/features/ads/design/`); vẽ lại theo cỡ AdsKit ở T-035. Tới lúc đó, khi thiết kế và `ads-business-rules.md` khác nhau về kích thước, theo `ads-business-rules.md`.
- Mỗi lượt hiện một quảng cáo riêng; rời màn thì hủy (luật O-01, O-03).
- Hiện có (đợt 2): cỡ small và medium ở bảng dưới, tới khi T-035 vẽ lại và ticket đợt 3 đổi code.

Small và medium trong `:core:ads` (plan T-010, số liệu từ `docs/features/ads/design/`):

| Phần | Small | Medium |
| --- | --- | --- |
| Khung | cao 72 dp, rộng hết vùng nội dung; nền `surfaceContainerLow`, viền 1 dp `outlineVariant`, bo 12 dp | cao 386 dp, như small |
| Chỗ cho AdChoices | lề phải 24 dp, chữ và CTA không đè góc trên phải | lề trên 24 dp |
| Icon | 40 dp, bo 8 dp | 40 dp, trong hàng đầu |
| Media | không có | rộng hết, cao 196 dp, `MediaView` giữ tỷ lệ |
| CTA | cao 36 dp, nền `tertiaryContainer`, bo 8 dp (nút chính của app: 52 dp, `primary`, bo 16 dp) | rộng hết, cao 36 dp |

- Màn hình dùng `NativeAdSlot(placement)`: giữ chỗ bằng khối chờ khi tải, thu chỗ (cao 0) khi không có quảng cáo sau 10 giây, hủy quảng cáo khi rời màn hình. Vị trí nằm cạnh nút của app thì truyền `keepSpaceWhenEmpty = true` để giữ khoảng trống đúng chiều cao, nút không dịch.
- AdChoices do SDK vẽ ở góc trên phải; app không tự vẽ.

## 5. Thêm một vị trí quảng cáo

Vị trí là quyết định kinh doanh: chỉ thêm khi spec đã duyệt có vị trí đó (ADR-0004). Vị trí theo mô hình chung (`ads-business-rules.md`, mục Mô hình chung): thêm vị trí cho một tính năng mới **không** thêm code vào `:core:ads`.

1. **Spec của app:** mục Quảng cáo ghi cho mỗi vị trí:
   - Mã vị trí (chữ thường, gạch dưới), cũng là giá trị `ad_placement` trong số liệu.
   - Định dạng, chọn trong danh sách của `:core:ads`: `app_open`, `interstitial`, `rewarded`, `banner`, `banner_collapsible`, `native_small`, `native_collap`, `native_fullscreen`, `native_step`, `native_medium`, `native_in_collection`.
   - Màn hình, thời điểm, thuộc tính của định dạng:
     - Inter: nhóm giãn cách (mặc định dùng `ads_interval`; nhóm riêng dùng `ads_interval_<nhóm>`), bỏ cổng khi đọc hay không, ghi mốc hay không.
     - Rewarded: hành động nó giữ (nhận vật phẩm, xuất file...), chính sách khi không có quảng cáo (`block` mặc định, `allow`, `allow_after_<n>`).
     - Native ở bước của luồng: luồng nào, bước nào.
     - Native trong tập hợp: ẩn hay giữ chỗ khi hết quảng cáo, chèn mỗi N mục.
     - Thanh đáy: màn nào có.
   - Thiết kế đánh dấu vùng quảng cáo trên ảnh màn hình.
2. **Khai báo:** thêm một dòng vào `AdPlacement` (`core/ads`) với mã, định dạng và thuộc tính trên. Dòng giữ dạng một dòng mà `scripts/release.sh` đọc được, `TEN_VI_TRI("ten_vi_tri", AdFormat.X),`: script đòi mã production cho mọi vị trí, trừ vị trí bắt đầu bằng `gallery_` (chỉ dùng cho màn xem thử ở staging, không bao giờ phát hành). `AdPlacement.stagingOnly` suy từ tiền tố này; `ConfiguredAdUnitIds` không đòi và không trả mã production cho các vị trí đó. Mã đơn vị theo vị trí hay theo khe như AdsKit là câu hỏi 14 của spec ads; đổi cách này cần sửa `release.sh` (việc của người).
   - **Giới hạn hiện tại:** dòng của vị trí không bắt đầu bằng `gallery_` phải là một dòng và không có thuộc tính nào sau định dạng (nhóm giãn cách riêng, `nativeEvery`, `ignoreInterval`...), cho tới khi người sửa biểu thức ở dòng 42 của `release.sh`. Script bỏ qua dòng có thuộc tính, nên bản phát hành sẽ không đòi mã cho vị trí đó và vị trí im lặng không có quảng cáo; `AdPlacementSourceTest` của `:core:ads` báo lỗi với dòng như vậy. Vị trí cần thuộc tính thì hỏi người sửa script trước khi khai báo.
3. **Khóa:** không đặt tên tay. Khóa suy ra từ khai báo: `is_show_<mã vị trí>` (mặc định tắt), `ads_interval_<nhóm>` nếu có nhóm riêng, `<tham số>_<mã vị trí>` cho tham số riêng (câu hỏi 28 của spec ads). Muốn giữ đúng chuỗi khóa của AdsKit thì chọn mã vị trí trùng (ví dụ vị trí `rw_get_item` cho `is_show_rw_get_item`). Bảng khóa sinh lại tự động.
4. **Màn hình:** gọi composable hay hàm của `:core:ads` với vị trí: banner, native, thanh đáy, ô trong tập hợp, `rememberShowInterstitial(placement)` ở hành động của người dùng, hộp thoại rewarded trước hành động có điều kiện. Feature không gọi SDK quảng cáo, không đọc khóa quảng cáo. Màn hình mở màn khác của hệ thống (chọn ảnh, camera, chia sẻ) dùng cách chặn quảng cáo tự hiện của `:core:ads` (`ads-business-rules.md`, mục 6).
   - Hiện có (đợt 2): `AdSlot(placement)` (banner), `NativeAdSlot(placement)` (native small, medium; `keepSpaceWhenEmpty = true` khi vị trí cạnh nút của app), `rememberShowFullScreenAd(placement)` (interstitial và native toàn màn hình; hàm cũ `rememberShowInterstitial` vẫn chạy cho interstitial). Native toàn màn hình hiện trong hộp thoại do `FullScreenAdHost()` của `:app` vẽ, dùng chung giới hạn tần suất với interstitial, và không hiện gì khi chưa tải sẵn xong. Màn hình nhận phần quảng cáo qua một tham số composable, mặc định là `NativeAdSlot`, như `bottomAd` của `SampleScreen`.
   - **Mở màn của hệ thống hay rời app** (`ads-business-rules.md`, mục 6, G-03): để người dùng quay lại không gặp quảng cáo khi quay lại app.
     - Màn chọn ảnh, camera, hộp thoại quyền: `val adSuppression = rememberAdSuppression()`, gọi `adSuppression.hold()` ngay trước khi mở, `adSuppression.release()` ở dòng đầu của callback kết quả, kể cả khi mở thất bại. Composable rời màn thì tự nhả.
     - Màn của app chiếm toàn màn hình (trang native toàn trang, native sau inter): đặt `SuppressResumeAds(active = <trang đang hiện>)` trong màn đó.
     - Chia sẻ, mở link, mở Play: `val markLeavingApp = rememberMarkLeavingApp()`, gọi `markLeavingApp()` ngay sau `startActivity` không ném lỗi. Dấu tự nhả ở lần activity của app hiện lại kế tiếp.
     - Ba cách này chỉ chạy khi `:app` đặt `AdsHost(ads) { ... }` ở gốc giao diện (`MainActivity`, thay cho `CompositionLocalProvider(LocalAdsController provides ads)`). Thiếu `AdsHost` thì chặn không bao giờ hạ và quảng cáo khi quay lại app tắt tới hết process (G-04).
   - **Sau quảng cáo toàn màn hình:** `rememberShowFullScreenAd(placement, onFinished = { ... })` chạy `onFinished` đúng một lần: ngay khi không có quảng cáo nào hiện (bị chặn, chưa tải xong), hoặc sau khi quảng cáo đóng. Điều hướng sang màn sau đặt trong `onFinished`.
   - **Native trong danh sách** (vị trí native small hay medium): gọi `rememberInListAds(placement)` ở mức màn hình, ngoài `LazyColumn` hay `LazyVerticalGrid`, rồi thay `items(...)` bằng `itemsWithInListAds(...)`:

     ```kotlin
     val ads = rememberInListAds(AdPlacement.MY_LIST_NATIVE)
     LazyColumn { itemsWithInListAds(photos, ads, key = { it.id }) { PhotoRow(it) } }
     ```

     Quảng cáo thứ k nằm ngay sau mục thứ k x N (N là `native_every_<mã vị trí>`, mặc định là `nativeEvery` của vị trí; vị trí không khai báo `nativeEvery` thì không chèn hàng nào), chỉ nằm giữa hai mục: không ở đầu, không ở cuối, danh sách có N mục trở xuống thì không có quảng cáo. Chưa đồng ý, vị trí hay công tắc tổng tắt thì không chèn hàng nào. Mỗi ô một quảng cáo riêng, tải khi ô hiện lần đầu, giữ khi cuộn qua lại (không tải lại), hủy hết khi rời màn hình; ô không có quảng cáo sau thời gian chờ thì cao 0. Hàng quảng cáo có khóa và `contentType` riêng, không trùng khóa của mục.
   - **Lưới:** truyền số cột, `itemsWithInListAds(items, ads, columns = 2) { ... }`. N được làm tròn lên bội số cột (N = 3, 2 cột thành 4) để hàng trên quảng cáo luôn đủ ô; hàng quảng cáo rộng hết lưới. Khi chọn N cho màn hình lưới, nên chọn sẵn bội số cột.
   - N trên Remote Config nên từ 5 trở lên; N = 1 nghĩa là quảng cáo xen giữa mọi mục. Code không chặn, giá trị là quyết định của người vận hành (ADR-0023).
   - **Native thu gọn** (vị trí định dạng `NATIVE_COLLAPSIBLE`): đặt `CollapsibleNativeAdSlot(placement)`. Màn hình gọi tự đặt nó ở đáy; slot chỉ chiếm chiều rộng được cho, không tự ghim. Quảng cáo mở dạng medium có nút thu gọn 48 dp ở góc trên trái, chạm thì thành dạng small và không mở lại trong lần vào màn hình đó, kể cả khi xoay máy, quay lại từ ngăn xếp hay tải quảng cáo mới. Screenshot test truyền `CollapsibleNativeAdCard(assets, collapsed, onCollapse = {})` giả.
5. **Test:** test không có `LocalAdsController` nên slot quảng cáo không vẽ gì; screenshot test truyền `NativeAdCard` giả (hay `NativeAdPlaceholder(size, animated = false)` cho trạng thái đang tải) qua tham số quảng cáo của màn hình, để ảnh có quảng cáo như thiết kế, như `SampleScreenScreenshotTest`; test ViewModel cho hành động có điều kiện (xem xong thì làm, không xem được thì theo chính sách) và điểm gọi interstitial nếu có logic.
6. **Mã quảng cáo production:** tạo đơn vị quảng cáo trên AdMob, ghi vào `release.properties` lúc phát hành ([release.md](release.md)); không vào repo.
7. **Bật:** sau khi bản production có vị trí đã lên Play, người phụ trách bật khóa trên Firebase theo [remote-config.md](remote-config.md), mục 5.

Cần một định dạng mới hay một cơ chế mới (không phải một vị trí mới) thì không làm trong app: đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## 6. Nhịp hiển thị

Luật đầy đủ ở `ads-business-rules.md`, mục 3 và 4. Tóm tắt cho người vận hành:

- `ads_interval` (mặc định 45 giây): khoảng tối thiểu từ lúc inter gần nhất của nhóm mặc định rời màn (hoặc cả cụm inter và native) tới inter sau. Vị trí có nhóm giãn cách riêng dùng `ads_interval_<nhóm>` và mốc riêng của nhóm. Chỉ tính trong một lần mở app; mở lại app thì bắt đầu lại. App open và rewarded không tính.
- App open lúc mở app và inter lúc mở app (`open_ad_type = inter`) không bị `ads_interval` chặn; inter lúc mở app vẫn đẩy mốc.
- Quảng cáo khi quay lại app không có khoảng chờ thời gian; bị bỏ khi người dùng vừa bấm quảng cáo, vừa mở màn của hệ thống hay rời app có chủ đích.
- Banner làm mới theo cài đặt tự làm mới của đơn vị banner trên AdMob; native ở đáy và native thu gọn thay nội dung mỗi 60 giây khi app ở foreground.

Khóa, kiểu và mặc định của mọi khóa quảng cáo: `ads-business-rules.md`, mục 10; bảng sinh từ code ở `docs/remote-config-keys.md`.

## 7. Mediation: AdMob và chín mạng (đợt 3)

- AdMob điều phối. Mạng con qua adapter chính thức của AdMob: AppLovin, Meta Audience Network, DT Exchange (Fyber), InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads (ADR-0026). Đợt 2 có AppLovin (T-015); tám mạng còn lại ở T-034.
- Thêm adapter là thay đổi thư viện: plan rủi ro cao, so quyền trong manifest gộp trước và sau.
- Khóa SDK AppLovin là cấu hình của từng app, đưa vào lúc build, không vào repo (ADR-0026). Đợt 2 (T-015, mục dưới) không đưa khóa vào app vì với bidding khóa nhập trên AdMob; phản hồi FB-20261023-01 cho thấy adapter báo `NOT_READY` trên staging khi app không có khóa. T-034 chốt cách đưa khóa vào lúc build.
- Khai báo mạng trong nhóm mediation của từng đơn vị quảng cáo trên AdMob (việc của người).
- Kiểm bằng Ad Inspector của AdMob trên bản staging (mã thử): mỗi adapter phải báo đã khởi tạo.
- Hướng dẫn chính thức: https://developers.google.com/admob/android/mediation (mỗi mạng một trang con).

### AppLovin (đợt 2, T-015)

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
- **Với ADR-0026:** khóa SDK đưa vào lúc build (mục trên), thay dòng "Khóa SDK AppLovin không vào app". Bộ PARTNER khởi tạo SDK sớm, trước khi có đồng ý. Việc bỏ `AppLovinInitProvider` ở trên dựa trên ADR-0004 (không chạy mã mạng con trước đồng ý); T-034 xem lại quyết định này cùng tám mạng còn lại.

## 8. Thử trên staging

- Staging luôn dùng mã thử của Google theo định dạng (inter, banner, rewarded, app open, native; ADR-0013).
- Bản staging dùng Firebase giả nên Remote Config không trả gì: staging bật mọi vị trí của `AdPlacement` bằng giá trị cục bộ (`StagingRemoteValuesModule`); Firebase staging vẫn tắt được từng vị trí.
- Hộp thoại đồng ý: staging giả lập vùng EEA để thấy hộp thoại. Thử thêm: tắt mạng khi mở app (quyền tạm `ERROR_ALLOWED`), mở lại app sau khi đã đồng ý (`CACHED_EARLY`), rút đồng ý bằng nút quyền riêng tư.
- Màn hình xem thử quảng cáo (chỉ staging, plan T-014): mở từ biểu tượng "Ad gallery" thứ hai trên máy. Ba trang: mặc định (native small, medium, nút "Interstitial", "Native full screen", "Open Ad Inspector"), "In-list ads" (xen small và medium sau mỗi N mục, N từ `native_every_<mã vị trí>`, mặc định 8), "Collapsible ad"; nút quay lại của máy về trang mặc định. Dùng 7 vị trí `gallery_*`, khoảng cách quảng cáo toàn màn hình như production (bấm lại trong 45 giây (`ads_interval`) sau khi inter đóng thì không hiện, log `THROTTLED`; khoảng cách tính từ lúc đóng, không từ lúc hiện, và chỉ trong process, mở lại app thì hiện được ngay). Ad Inspector chỉ mở trên emulator hoặc máy đã đăng ký thiết bị thử trên AdMob (https://support.google.com/admob/answer/9691433); máy chưa đăng ký thì màn hình báo lỗi kèm mã. Log ở thẻ `VGL.AdGallery` và `VGL.Ads`. Định dạng mới của đợt 3 thêm vào màn hình này ở T-036.
- Kiểm trên máy thật theo mục 2: nhãn "Ad", AdChoices, nút đóng và đếm ngược, thu gọn và bung lại, thanh đáy không che điều hướng của app.
- Xem log `adb logcat -v threadtime | grep ' VGL\.'`: mỗi quyết định cổng có một dòng (ADR-0019).

## 9. Trước khi phát hành

- [ ] Mỗi vị trí trong `AdPlacement` có mã production trong `release.properties` (`release.sh` kiểm).
- [ ] Khóa SDK AppLovin và khóa dev AppsFlyer có trong trình quản lý mật khẩu, đưa vào lúc build (ADR-0027).
- [ ] `app-ads.txt` trên website của nhà phát triển có dòng của AdMob và của cả chín mạng con; lấy dòng từ bảng điều khiển của từng mạng. Thiếu dòng thì mạng đó có thể không trả quảng cáo.
- [ ] Hộp thoại đồng ý đã cấu hình trên AdMob (Privacy & messaging) cho các vùng cần, có danh sách đối tác quảng cáo gồm chín mạng.
- [ ] Liên kết AdMob với Firebase (để có `ad_impression` tự động); AppsFlyer đã nhận sự kiện từ bản phát hành thử.
- [ ] Bản đầu tiên gửi `ad_placement` sang AppsFlyer hay gửi sự kiện đếm inter: đã báo nhóm thu hút người dùng (UA) và ghi ngày đổi lên dashboard (`ads-business-rules.md`, HD-07). App chuyển từ `ad_impression` sang `ad_impression_placement` cũng ghi ngày đổi và báo nhóm.
- [ ] **Data safety** trên Play Console khai đủ SDK đang dùng: Google Mobile Ads, chín mạng con, AppsFlyer, Firebase. Các mạng quảng cáo thường thu thập: mã quảng cáo và mã thiết bị khác, tương tác với quảng cáo và app, chẩn đoán, vị trí gần đúng suy từ IP. Tra trang công bố dữ liệu của từng mạng lúc làm T-034 và ghi kết quả vào plan của ticket đó; khai đúng mục đích (quảng cáo, phân tích, chống gian lận).
- [ ] Tắt chế độ thử trên AdMob và các mạng con (máy thử, single ad source testing) trước khi phát hành.
- [ ] Rà từng vị trí theo mục 2 trên bản phát hành.
- [ ] Mọi khóa quảng cáo vẫn tắt trên Remote Config tới khi bản phát hành lên Play; bật dần theo [remote-config.md](remote-config.md).

## 10. Đo lường

- **Firebase:** liên kết AdMob với Firebase thì `ad_impression` và doanh thu được ghi tự động. App tự ghi `ad_impression_placement` (doanh thu theo vị trí, không đếm đôi với `ad_impression`) và, khi bật khóa, số liệu vòng đời `ad_load_*`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`, `ad_reward_earned`, `reward_dialog` (`ads-business-rules.md`, mục 8.2). Kiểm bằng DebugView.
- **AppsFlyer:** `af_ad_revenue` cho mọi lượt hiển thị, phễu `af_inters_*`, `af_rewarded_*`, `af_native_*`, `af_inters_displayed_{N}_times`. Không có `af_revenue` (ADR-0027). Mỗi sự kiện có công tắc từ xa `is_log_af_*`, mặc định bật; tắt khẩn khi một sự kiện làm hỏng số liệu.
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
- Hiệu quả quảng cáo AdMob trong Firebase: https://support.google.com/firebase/answer/7378163
- Mediation AppLovin: https://developers.google.com/admob/android/mediation/applovin
- Ad Inspector: https://developers.google.com/admob/android/ad-inspector
- Chính sách chương trình AdMob: https://support.google.com/admob/answer/6128543
- Interstitial không được phép (lúc mở, lúc thoát app, lặp lại): https://support.google.com/admob/answer/6201362
- Chính sách quảng cáo của Google Play: https://support.google.com/googleplay/android-developer/answer/9857753
