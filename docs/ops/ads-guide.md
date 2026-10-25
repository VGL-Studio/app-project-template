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

Tùy chọn yêu cầu native (O-06: video tắt tiếng, không điều khiển riêng, một ảnh, media ngang; toàn màn hình nhận mọi tỷ lệ) chỉ dựng ở `NativeRequestOptions.kt`; mọi đường gửi native gọi `nativeRequestOptions(size)` hay `fullScreenNativeRequestOptions()`. `NativeRequestOptionsTest` đỏ khi một file khác dựng `NativeAdOptions`.

| Định dạng | Dùng ở đâu | Khóa bật | Có trong `:core:ads` |
| --- | --- | --- | --- |
| App open lúc mở app | Màn loading, lần đầu sau khi cài và lần thường | `show_open_ads_first_open`, `show_open_ads`, `open_ad_type` | Có (T-024): vị trí toàn app `open_app` (app open) và `open_app_inter` (inter khi `open_ad_type = inter`), không có `is_show_<mã vị trí>`; màn loading gọi `StartupAds`, feature không gọi |
| Quảng cáo khi quay lại app | Mọi lần app lên foreground, trừ lần đầu của process | `show_resum_ads`, `resume_ad_type` | (đợt 3) |
| Interstitial | Hành động của người dùng (sang màn sau, xong một việc) | `is_show_<mã vị trí>`, cách nhau `ads_interval` hay `ads_interval_<nhóm>` | Có (đợt 2, `rememberShowFullScreenAd`), luật mới ở đợt 3 |
| Native toàn màn hình sau inter (combo) | Ngay sau inter, đếm ngược 3 giây | `is_show_native_after_inter` | Native toàn màn hình đứng riêng có (đợt 2, `rememberShowFullScreenAd`); combo ở đợt 3 |
| Thanh đáy: native nhỏ 80 dp và native thu gọn 250 dp, hoặc banner | Đáy màn hình nội dung, mỗi màn một vị trí | `is_show_<mã vị trí>`, `is_show_native_bottom`, `is_show_native_collap`, `time_expand_collap_ad` | Native small (`NativeAdSlot`) và native thu gọn (`CollapsibleNativeAdSlot`) có ở cỡ đợt 2; 80 dp, 250 dp và làm mới ở đợt 3 |
| Banner cố định, banner thu gọn | Đáy màn hình | `is_show_<mã vị trí>` | Có (T-029, `AdSlot`): banner cố định làm mới theo đơn vị, banner thu gọn nạp lại mỗi 30 giây |
| Native medium | Trong nội dung | `is_show_<mã vị trí>` | Có (đợt 2), kích thước mới ở đợt 3 (T-028) |
| Native trong tập hợp (ô lưới, danh sách, trang của pager) | Mỗi ô một quảng cáo riêng | `is_show_<mã vị trí>` | Danh sách và lưới có (đợt 2, `itemsWithInListAds`); pager ở đợt 3 |
| Native ở một bước của luồng (giới thiệu, kết quả, hướng dẫn) | Bước do spec của app chọn | `is_show_<mã vị trí>` (luồng giới thiệu: `is_show_native_onboarding`) | (đợt 3) |
| Native đầy đủ (`NativeAdSize.FULL`) | Chưa có vị trí: MC-02 là danh sách đóng, muốn có thì sửa MC-02 trước | | Có composable `NativeAdCard(assets, NativeAdSize.FULL)` và khung chờ (T-027), chưa có `AdFormat` nào vẽ nó |
| Rewarded với hộp thoại (hành động có điều kiện) | Trước một hành động người dùng chọn: nhận vật phẩm, xuất file, mở khóa | `is_show_<mã vị trí>` (ví dụ `is_show_rw_get_item`) | (đợt 3) |

## 4. Native: kích thước và hình thức

- Kích thước theo layout của AdsKit và bản đặc tả độc lập (`ads-business-rules.md`, mục 5.7): khe đáy cao 80 dp (cả vẽ và chừa chỗ), native thu gọn 250 dp với nút thu gọn 32 dp, thẻ native ở bước của luồng 250 dp, nút đóng native sau inter 36 dp ở góc trên phải.
- Màu, chữ, bo góc lấy từ token của `:core:designsystem` (ADR-0008), không lấy màu của AdsKit. Màu chữ native theo nền của vùng quảng cáo, không đổi theo chế độ tối của máy khi nền không đổi.
- Thiết kế native trên Figma đang theo cỡ cũ của đợt 2 (`docs/features/ads/design/`); vẽ lại theo cỡ AdsKit ở T-035. Tới lúc đó, khi thiết kế và `ads-business-rules.md` khác nhau về kích thước, theo `ads-business-rules.md`.
- Mỗi lượt hiện một quảng cáo riêng; rời màn thì hủy (luật O-01, O-03).
- Hiện có: small theo cỡ AdsKit (T-027), medium giữ cỡ đợt 2 tới T-028; native đầy đủ chưa có vị trí.

Small, medium và đầy đủ trong `:core:ads` (plan T-010, T-027, số liệu từ `docs/features/ads/design/` và mục 5.7 của luật):

| Phần | Small | Medium | Đầy đủ (`FULL`) |
| --- | --- | --- | --- |
| Khung | cao 80 dp (`NativeAdDimens.BottomSlotHeight`, cùng hằng số cho thẻ, khung chờ và chỗ giữ), rộng hết vùng nội dung; nền `surfaceContainerLow`, viền 1 dp `outlineVariant`, bo 12 dp | cao 386 dp, như small | cao cố định 418 dp (mô tả luôn 2 dòng), như small |
| Chỗ cho AdChoices | lề phải 24 dp, chữ và CTA không đè góc trên phải | lề trên 24 dp | lề trên 24 dp |
| Icon | 36 dp, bo 8 dp | 40 dp, trong hàng đầu | 40 dp, trong hàng đầu |
| Chữ | tiêu đề 14 sp, mô tả 12 sp, nhãn "Ad" 10 sp đầu dòng mô tả | tiêu đề 14 sp | tiêu đề 14 sp |
| Media | không có | rộng hết, cao 196 dp, `MediaView` giữ tỷ lệ | rộng hết, cao 200 dp |
| CTA | cao 34 dp, chữ 13 sp, nền `tertiaryContainer`, bo 8 dp (nút chính của app: 52 dp, `primary`, bo 16 dp) | rộng hết, cao 36 dp | rộng hết, cao 36 dp |
| Hàng sao | không có | không có (T-028) | sao đặc làm tròn theo điểm, tối đa 5; không hiện khi không có điểm lớn hơn 0 (O-08) |

- Màn hình dùng `NativeAdSlot(placement)`: giữ chỗ bằng khối chờ khi tải, thu chỗ (cao 0) khi không có quảng cáo sau 10 giây, hủy quảng cáo khi rời màn hình. Vị trí nằm cạnh nút của app thì truyền `keepSpaceWhenEmpty = true` để giữ khoảng trống đúng chiều cao, nút không dịch.
- `onAdAvailabilityChange: (Boolean) -> Unit` của `NativeAdSlot` báo app khe có quảng cáo hay không (D-02): `true` khi có quảng cáo, `false` khi nạp lỗi, không gọi khi đang tải; và `false` khi vị trí tắt hay rút đồng ý sau khi đã báo `true`. App dùng nó để chừa chỗ dưới nội dung (D-04, T-030).
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
2. **Khai báo:** thêm một dòng vào `AdPlacement` (`core/ads`) với mã, định dạng và thuộc tính trên. Mỗi vị trí khai báo trên một dòng mà `scripts/release.sh` đọc được, `TEN_VI_TRI("ten_vi_tri", AdFormat.X),` hoặc có thuộc tính sau định dạng (`TEN_VI_TRI("ten_vi_tri", AdFormat.INTERSTITIAL, intervalGroup = ...),`): script đòi mã production cho mọi vị trí, trừ vị trí bắt đầu bằng `gallery_` (chỉ dùng cho màn xem thử ở staging, không bao giờ phát hành). `AdPlacement.stagingOnly` suy từ tiền tố này; `ConfiguredAdUnitIds` không đòi và không trả mã production cho các vị trí đó. Mã đơn vị theo vị trí hay theo khe như AdsKit là câu hỏi 14 của spec ads; đổi cách này cần sửa `release.sh` (việc của người).
   - **Một dòng cho mỗi vị trí:** `release.sh` đọc cả dòng có thuộc tính (nhóm giãn cách riêng, `nativeEvery`, `ignoreInterval`...), nhưng từ chối phát hành khi một vị trí khai báo trên nhiều dòng; `AdPlacementSourceTest` của `:core:ads` báo lỗi khi script không đọc được một vị trí. Dòng dài quá 120 ký tự thì rút gọn thuộc tính (ví dụ đặt nhóm giãn cách thành hằng), không xuống dòng.
3. **Khóa:** không đặt tên tay. Khóa suy ra từ khai báo: `is_show_<mã vị trí>` (mặc định tắt), `ads_interval_<nhóm>` nếu có nhóm riêng, `<tham số>_<mã vị trí>` cho tham số riêng (câu hỏi 28 của spec ads). Muốn giữ đúng chuỗi khóa của AdsKit thì chọn mã vị trí trùng (ví dụ vị trí `rw_get_item` cho `is_show_rw_get_item`). Bảng khóa sinh lại tự động.
4. **Màn hình:** gọi composable hay hàm của `:core:ads` với vị trí: banner, native, thanh đáy, ô trong tập hợp, `rememberShowInterstitial(placement)` ở hành động của người dùng, hộp thoại rewarded trước hành động có điều kiện. Feature không gọi SDK quảng cáo, không đọc khóa quảng cáo. Màn hình mở màn khác của hệ thống (chọn ảnh, camera, chia sẻ) dùng cách chặn quảng cáo tự hiện của `:core:ads` (`ads-business-rules.md`, mục 6).
   - Hiện có (đợt 2): `AdSlot(placement)` (banner cố định hay banner thu gọn, theo định dạng của vị trí, T-029; banner thu gọn đặt ở đáy màn hình), `NativeAdSlot(placement)` (native small, medium; `keepSpaceWhenEmpty = true` khi vị trí cạnh nút của app), `rememberShowFullScreenAd(placement)` (interstitial và native toàn màn hình; hàm cũ `rememberShowInterstitial` vẫn chạy cho interstitial). Native toàn màn hình hiện trong hộp thoại do `FullScreenAdHost()` của `:app` vẽ, dùng chung giới hạn tần suất với interstitial, và không hiện gì khi chưa tải sẵn xong. Màn hình nhận phần quảng cáo qua một tham số composable, mặc định là `NativeAdSlot`, như `bottomAd` của `SampleScreen`.
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
6. **Mã quảng cáo production:** tạo đơn vị quảng cáo trên AdMob, ghi vào `release.properties` lúc phát hành ([release.md](release.md)); không vào repo. Đơn vị của vị trí `banner` bật tự làm mới trên AdMob (app không tự tải lại); đơn vị của vị trí `banner_collapsible` tắt tự làm mới (`:core:ads` nạp lại mỗi 30 giây, T-029).
7. **Bật:** sau khi bản production có vị trí đã lên Play, người phụ trách bật khóa trên Firebase theo [remote-config.md](remote-config.md), mục 5.

Cần một định dạng mới hay một cơ chế mới (không phải một vị trí mới) thì không làm trong app: đề xuất về dự án mẫu (`docs/ops/from-app.md`).

## 6. Nhịp hiển thị

Luật đầy đủ ở `ads-business-rules.md`, mục 3 và 4. Tóm tắt cho người vận hành:

- `ads_interval` (mặc định 45 giây): khoảng tối thiểu từ lúc inter gần nhất của nhóm mặc định rời màn (hoặc cả cụm inter và native) tới inter sau. Vị trí có nhóm giãn cách riêng dùng `ads_interval_<nhóm>` và mốc riêng của nhóm. Chỉ tính trong một lần mở app; mở lại app thì bắt đầu lại. App open và rewarded không tính.
- App open lúc mở app và inter lúc mở app (`open_ad_type = inter`) không bị `ads_interval` chặn; inter lúc mở app vẫn đẩy mốc.
- Quảng cáo khi quay lại app không có khoảng chờ thời gian; bị bỏ khi người dùng vừa bấm quảng cáo, vừa mở màn của hệ thống hay rời app có chủ đích.
- Banner cố định làm mới theo cài đặt tự làm mới của đơn vị banner trên AdMob; banner thu gọn do `:core:ads` nạp lại 30 giây sau mỗi lần nạp được, mỗi lần là một banner bung ra mới (T-029). Banner lỗi nạp thì thử lại sau 2, 4, 6, 8 rồi 10 giây, không bỏ cuộc, gửi ngay khi mạng về. Banner chỉ gửi yêu cầu khi màn hình chứa nó đang hiện và cổng đồng ý cho phép ngay lúc gửi; lượt tới hạn khi màn không hiện thì chờ màn hiện lại, và banner tạm dừng khi màn bị che hay app xuống nền. Rút đồng ý khi banner đã có quảng cáo thì khung giữ chiều cao, quảng cáo ẩn; đồng ý lại thì nạp quảng cáo mới trong khung đó. Log thẻ `Ads`: `Banner <mã> retry in 2000 ms (failures 1, code 3)`, `Banner <mã> reload in 30000 ms`, `Banner <mã> request blocked`. Native ở đáy và native thu gọn thay nội dung mỗi 60 giây khi app ở foreground.

Khóa, kiểu và mặc định của mọi khóa quảng cáo: `ads-business-rules.md`, mục 10; bảng sinh từ code ở `docs/remote-config-keys.md`.

### Nạp sẵn và nạp lại (T-021)

Luật đầy đủ ở `ads-business-rules.md`, S-20 tới S-25, F-04 tới F-06, O-05, O-09.

- **Không khe nào tự nạp khi được tạo.** Mọi lượt nạp sẵn đi qua kế hoạch nạp sẵn (`AdPreloadPlan`): danh sách nhóm định dạng chạy lần lượt; nhóm sau bắt đầu khi mọi khe của nhóm trước xong (có quảng cáo, lỗi hay đang chờ thử lại) hoặc sau tối đa 6 giây. Định dạng không có trong kế hoạch thì không được nạp sẵn.
- **Kế hoạch mặc định** suy ra từ danh sách vị trí (`AdPreloadPlan.fromPlacements()`): nhóm đầu là app open và native của bước đầu luồng (khe người dùng thấy đầu tiên), nhóm sau là mọi định dạng còn lại. Dự án mẫu có nhóm đầu là `APP_OPEN` (vị trí `open_app`, T-024).
- **App đặt kế hoạch riêng** bằng `ads.setPreloadPlan(AdPreloadPlan(listOf(setOf(AdFormat.APP_OPEN), setOf(AdFormat.INTERSTITIAL, ...))))` trong `Application.onCreate`, ngay sau `ads.start(...)`, trước khi activity đầu tiên hỏi đồng ý. Một định dạng chỉ ở một nhóm.
- **Lúc chạy:** lần đầu khi SDK sẵn sàng (callback của `MobileAds.initialize`, hoặc 35 giây sau khi gọi khởi tạo nếu callback không về); khi cổng đồng ý mở lại; khi một vị trí toàn màn hình được bật từ xa; khi mạng về. Lượt bị chặn vì cổng đóng được giữ một lượt và chạy một lần khi cổng mở; khi quyền thật của UMP là "không" thì không giữ. Lượt xin tới khi kế hoạch hay lượt nạp bù native đang chạy cũng được giữ (chung chỗ giữ một lượt) và chạy cả kế hoạch một lần khi lượt đang chạy xong; nạp bù native khi đang chạy thì bỏ. Lượt xin mới được chạy khi đang có lượt giữ thì lượt giữ tính là đã chạy cùng lượt đó, không chạy thêm (T-038). Cách hiểu "không chạy chồng" của S-23 do ta đặt (T-037), chưa đối chiếu AdsKit.
- **Thử lại sau lỗi nạp:** interstitial chờ 2, 4, 6, 8 rồi 10 giây mỗi lần, không bỏ cuộc; native (khe trong màn và native toàn màn hình) thử lại sau 5 giây khi chưa có quảng cáo. Bộ đếm lỗi về 0 khi nạp được, đếm riêng từng vị trí. Interstitial đóng hay hiện lỗi thì nạp lại sau 2 giây; native toàn màn hình nạp lại ngay.
- **Mạng về:** Internet dùng được ổn định 3 giây sau khi mất thì kế hoạch nạp lại và khe native đang chờ thử lại gửi ngay. Mở app khi đang ở chế độ máy bay rồi tắt cũng tính là mạng về.
- **Native không nạp ở nền:** mọi yêu cầu native của khe trong màn và native toàn màn hình (cả lượt đầu, lượt nạp sẵn và lượt thử lại) chờ app lên foreground. Hàng native trong danh sách chưa theo luật này (chờ pool của T-028). Activity của app hiện lại thì các khe native trong kế hoạch được nạp bù theo thứ tự kế hoạch, khi kế hoạch đã chạy xong ít nhất một lần và không có quảng cáo toàn màn hình.
- Log thẻ `Ads` để kiểm trên máy: `SDK ready: callback` hay `SDK ready: 35 s cap`, `Preload SDK_READY: RUN`, `Preload group 1/1 started: ...`, `Preload PLACEMENT_ENABLED: HOLD`, `Held preload PLACEMENT_ENABLED: run`, `Held preload NETWORK_BACK: covered by GATE_REOPENED run`, `Retry gallery_interstitial in 2000 ms (failures 1, code 2)`, `Native sample_native_small retry in 5000 ms (failures 1)`, `Network back`.

## 7. Mediation: AdMob và chín mạng

- AdMob điều phối. Mạng con qua adapter chính thức của AdMob: AppLovin, Meta Audience Network, DT Exchange (Fyber), InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads (ADR-0026). AppLovin có từ T-015, tám mạng còn lại từ T-034 (mục dưới).
- Thêm hay nâng adapter là thay đổi thư viện: plan rủi ro cao, so quyền và thành phần trong manifest gộp trước và sau, kiểm lại tên provider, receiver trong manifest của AAR, chạy `MergedManifestTest` của `:app`.
- **Khóa SDK AppLovin không nằm trong app** (plan T-034, câu hỏi 2, phương án A): khóa chỉ nhập trong hộp thoại ánh xạ nguồn quảng cáo "AppLovin (Bidding)" trên AdMob của từng app; không vào repo, không vào APK. Lý do: đọc bytecode adapter `13.6.4.2`, adapter chỉ lấy khóa từ cấu hình mediation mà AdMob gửi xuống (`MediationConfiguration.getServerParameters().getString("sdkKey")`), không đọc meta-data `applovin.sdk.key` hay resource nào của app (Changelog adapter 12.1.0.0: "Adapter no longer checks for the AppLovin SDK key from the AndroidManifest.xml file"). Đưa khóa vào app lúc build không làm adapter `READY`, chỉ làm lộ khóa trong APK. `scripts/secret-scan.py` chặn khóa AppLovin trong repo; `MergedManifestTest` kiểm manifest gộp không có `applovin.sdk.key`.
- **Trên staging cả chín adapter báo `NOT_READY`:** mọi adapter lấy mã của mạng (khóa SDK AppLovin, app key ironSource, game ID Unity, app ID Pangle...) từ cấu hình mediation trên AdMob; mã app thử của Google không có cấu hình đó (FB-20261023-01). Trên staging chỉ kiểm được log trạng thái liệt kê đủ chín adapter (thẻ `Ads`, một dòng `Adapter ...` mỗi adapter khi `MobileAds.initialize` báo xong). Kiểm `READY` trên app có mã AdMob thật và nhóm mediation đủ chín mạng: log có dòng `READY` cho từng adapter; Ad Inspector, "single ad source testing" với từng mạng tải được quảng cáo thử, sau khi đăng ký máy thử.
- Khai báo mạng trong nhóm mediation của từng đơn vị quảng cáo trên AdMob (việc của người).
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
- **Với ADR-0026 (xem lại ở T-034):** khóa SDK vẫn không vào app (mục trên). Việc bỏ `AppLovinInitProvider` **giữ nguyên**: ADR-0026 cho khởi tạo SDK sớm chỉ khi UMP còn giữ đồng ý dùng được của phiên trước (`CACHED_EARLY`, S-03) hay khi UMP lỗi mạng ở foreground (`ERROR_ALLOWED`), và nhánh rút gọn mặc định tắt (S-12). Provider thì chạy ở mọi lần mở tiến trình, kể cả lần đầu sau khi cài khi chưa có đồng ý nào, và đọc Advertising ID cùng App Set ID. Cùng lý do áp cho provider của Meta (mục dưới).

### Tám mạng còn lại (T-034)

- **Thư viện** (chỉ `:core:ads` phụ thuộc, khóa trong `gradle/libs.versions.toml`, bản mới nhất ổn định ngày 2026-10-24):

  | Mạng | Adapter | Khóa catalog | SDK của mạng (kéo theo) |
  | --- | --- | --- | --- |
  | Meta Audience Network | `com.google.ads.mediation:facebook:6.22.0.1` | `admob-mediation-meta` | `com.facebook.android:audience-network-sdk:6.22.0` |
  | DT Exchange | `com.google.ads.mediation:fyber:8.5.0.0` | `admob-mediation-dtexchange` | `com.fyber:marketplace-sdk:8.5.0` |
  | InMobi | `com.google.ads.mediation:inmobi:11.5.0.0` | `admob-mediation-inmobi` | `com.inmobi.monetization:inmobi-ads-kotlin:11.5.0`, kèm Picasso 2.8 |
  | ironSource | `com.google.ads.mediation:ironsource:9.6.1.0` | `admob-mediation-ironsource` | `com.unity3d.ads-mediation:mediation-sdk:9.6.1` |
  | Mintegral | `com.google.ads.mediation:mintegral:17.1.81.1` | `admob-mediation-mintegral` | `com.mbridge.msdk.oversea:mbridge_android_sdk:17.1.81` |
  | Moloco | `com.google.ads.mediation:moloco:4.13.0.0` | `admob-mediation-moloco` | `com.moloco.sdk:moloco-sdk:4.13.0` |
  | Pangle | `com.google.ads.mediation:pangle:8.3.0.4.0` | `admob-mediation-pangle` | `com.pangle.global:pag-sdk:8.3.0.4` |
  | Unity Ads | `com.google.ads.mediation:unity:4.21.0.0` | `admob-mediation-unity` | `com.unity3d.ads:unity-ads:4.21.0`, **khai báo riêng** (khóa `unity-ads`): POM của adapter không kéo SDK |

- **Hai kho Maven ngoài** trong `settings.gradle.kts`: `https://artifact.bytedance.com/repository/pangle/` (chỉ nhóm `com.pangle.global`) và `https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea` (chỉ nhóm `com.mbridge.*`). Đây là nơi duy nhất có SDK của Pangle và Mintegral. Khối `content` giữ cho mọi thư viện khác chỉ tải từ `google()` và `mavenCentral()`, để kho ngoài không trả thay được một thư viện cùng tên. Không cần kho ironSource (`android-sdk.is.com`): SDK ironSource có trên Maven Central. Môi trường cloud phải cho phép hai tên miền này (RUN-20261024-02 dừng vì proxy chặn).
- **Không có code khởi tạo:** `MobileAds.initialize` khởi tạo cả chín adapter; đồng ý GDPR từ UMP được chuyển tự động (TCF hay Additional Consent). Không làm bước tùy chọn của từng mạng cho luật các bang Mỹ hay trẻ em.
- **Manifest gộp** (đo ở plan T-034):
  - **Không thêm quyền nào:** các quyền tám SDK khai báo (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`) đã có từ Google Mobile Ads. Không thêm các quyền tùy chọn InMobi gợi ý (vị trí chính xác, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`). `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet` giữ danh sách 11 quyền; quyền mới khi nâng SDK làm test đỏ, plan phải quyết.
  - **Bỏ** trong `core/ads/src/main/AndroidManifest.xml` bằng `tools:node="remove"`: provider `com.facebook.ads.AudienceNetworkContentProvider` (khởi động SDK Meta lúc mở app, trước khi có đồng ý; bỏ để Meta chỉ khởi động qua `MobileAds.initialize`, ADR-0004, ADR-0026 mục 3); receiver `com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver` của Mintegral (xuất ra ngoài, lọc `CONNECTIVITY_CHANGE` mà hệ thống không gửi tới receiver trong manifest từ API 24, nên chỉ app khác gọi được nó).
  - **Giữ:** provider `IronsourceLifecycleProvider`, `LevelPlayActivityLifecycleProvider` (ironSource) và `MBComponentLifecycleProvider` (Mintegral) chỉ theo dõi vòng đời activity, SDK cần để hiện quảng cáo; `PicassoProvider` chỉ giữ `Context`; bộ khởi tạo App Startup `com.unity3d.services.core.configuration.AdsSdkInitializer` (Unity) theo dõi activity và hướng màn hình, không khởi động Unity Ads. Cùng 39 activity, 2 service không xuất ra ngoài, meta-data phiên bản của Pangle.
  - **`<queries>`:** thêm 19 mục: Custom Tabs, `VIEW`, `MAIN`, `market://`, các kho ứng dụng, `com.facebook.katana`, dịch vụ của Digital Turbine và năm gói của công cụ root máy (`com.topjohnwu.magisk`, `eu.chainfire.supersu`, `com.koushikdutta.superuser`, `com.noshufou.android.su`, `com.thirdparty.superuser`). Không phải `QUERY_ALL_PACKAGES`, không phải quyền; người khai Data safety cần biết (mục 9).
- **Lint:** Picasso (phụ thuộc bắt buộc của InMobi) có mã gửi thông báo, lint báo `NotificationPermission`. `app/lint.xml` bỏ qua quy tắc này chỉ cho phát hiện có `com.squareup.picasso.` (plan T-034, câu hỏi 7); không thêm `POST_NOTIFICATIONS`. Nâng InMobi hay Picasso thì xem lại dòng này.
- **R8:** bản release của staging thu gọn được, không cần quy tắc giữ lớp. Cảnh báo "Missing class" (không phải lỗi) cho `com.android.billingclient.api.*` từ SDK TikTok Business đi kèm Pangle; dự án không có Play Billing nên phần đó không chạy.
- **Unity và ironSource cần `Activity` khi nạp** banner, interstitial, rewarded (trang hướng dẫn của Google). Từ T-021, `:core:ads` nạp interstitial bằng activity đang mở của app khi có (không bao giờ bằng activity của SDK), không có thì bằng `ApplicationContext`. Banner nạp bằng activity của màn hình chứa nó (T-029). Native toàn màn hình vẫn nạp bằng `ApplicationContext`; rewarded (T-031) theo cách của plan đó.
- **Mintegral:** năm 2020 Snyk báo cáo SDK của Mintegral (khi đó là SourMint) theo dõi lượt bấm và URL trong app; Mintegral bác bỏ và đưa kết quả kiểm toán. Dự án mẫu giữ Mintegral theo ADR-0026 (plan T-034, câu hỏi 3); từng app cân nhắc khi khai Data safety.
- **Kích thước:** APK `stagingDebug` chưa thu gọn tăng khoảng 24 MB (23,4 MB lên 47,4 MB). App muốn bỏ bớt mạng thì đề xuất ADR của app.
- **Việc của người trên AdMob** (trang của từng mạng): thêm từng mạng vào danh sách đối tác quảng cáo của thông điệp GDPR và các bang Mỹ; ánh xạ mã của từng mạng trong nhóm mediation; bật chế độ thử của mạng khi thử và tắt trước khi phát hành; `app-ads.txt` có dòng của từng mạng ([release.md](release.md), mục 1.4).
- **Nâng adapter** là plan rủi ro cao: chọn bản có "Built and tested with" khớp bản Google Mobile Ads; kiểm lại tên provider, receiver trong manifest của AAR. `MergedManifestTest` chỉ kiểm theo tên cũ: SDK đổi tên phần tử thì dòng `tools:node="remove"` không còn khớp, phần tử tên mới vào app mà test vẫn qua, nên phải đọc manifest của AAR bằng tay.

## 8. Thử trên staging

- Staging luôn dùng mã thử của Google theo định dạng (inter, banner, rewarded, app open, native; ADR-0013).
- Bản staging dùng Firebase giả nên Remote Config không trả gì: staging bật mọi vị trí của `AdPlacement` bằng giá trị cục bộ (`StagingRemoteValuesModule`), trừ hai vị trí toàn app `open_app`, `open_app_inter` (bật bằng `show_open_ads`, `show_open_ads_first_open`, cũng bật sẵn trên staging); Firebase staging vẫn tắt được từng vị trí.
- Hộp thoại đồng ý: staging giả lập vùng EEA để thấy hộp thoại. Thử thêm: tắt mạng khi mở app (quyền tạm `ERROR_ALLOWED`), mở lại app sau khi đã đồng ý (`CACHED_EARLY`), rút đồng ý bằng nút quyền riêng tư.
- Màn hình xem thử quảng cáo (chỉ staging, plan T-014): mở từ biểu tượng "Ad gallery" thứ hai trên máy. Ba trang: mặc định (native small, medium, nút "Interstitial", "Native full screen", "Open Ad Inspector"), "In-list ads" (xen small và medium sau mỗi N mục, N từ `native_every_<mã vị trí>`, mặc định 8), "Collapsible ad"; nút quay lại của máy về trang mặc định. Dùng 7 vị trí `gallery_*`, khoảng cách quảng cáo toàn màn hình như production (bấm lại trong 45 giây (`ads_interval`) sau khi inter đóng thì không hiện, log `THROTTLED`; khoảng cách tính từ lúc đóng, không từ lúc hiện, và chỉ trong process, mở lại app thì hiện được ngay). Ad Inspector chỉ mở trên emulator hoặc máy đã đăng ký thiết bị thử trên AdMob (https://support.google.com/admob/answer/9691433); máy chưa đăng ký thì màn hình báo lỗi kèm mã. Log ở thẻ `VGL.AdGallery` và `VGL.Ads`. Định dạng mới của đợt 3 thêm vào màn hình này ở T-036.
- Kiểm trên máy thật theo mục 2: nhãn "Ad", AdChoices, nút đóng và đếm ngược, thu gọn và bung lại, thanh đáy không che điều hướng của app.
- Xem log `adb logcat -v threadtime | grep ' VGL\.'`: mỗi quyết định cổng có một dòng (ADR-0019).
- App open lúc mở app (T-024): staging bật sẵn `show_open_ads`, `show_open_ads_first_open`. Đồng ý quảng cáo, rồi thử: lần đầu sau khi xóa dữ liệu app (khóa `show_open_ads_first_open`); mở lại app (lần thường, `show_open_ads`); đặt `open_ad_type = inter` trên Firebase staging để thấy inter thay app open; tắt khóa trên Firebase staging thì màn loading vào màn chính sau khoảng 2 giây. Màn loading đứng ở 100% trong lúc chờ, tối đa 40 giây từ lúc mở app (đồng ý 10 giây, SDK 35 giây, quảng cáo 40 giây). Log thẻ `VGL.Ads` có một dòng `startup ad: <lý do>` (`key off`, `consent timeout`, `consent closed`, `sdk timeout`, `ad timeout`, `ready`) kèm số mili giây từ lúc mở app.

## 9. Trước khi phát hành

- [ ] Mỗi vị trí trong `AdPlacement` có mã production trong `release.properties` (`release.sh` kiểm), kể cả hai vị trí toàn app `open_app` và `open_app_inter` (T-024).
- [ ] App có quảng cáo mở app: bật cả `feature_loading_enabled` (Remote Config hay mặc định của app) lẫn `show_open_ads`, `show_open_ads_first_open`. Cờ loading tắt thì app vào thẳng màn chính, không có quảng cáo mở app.
- [ ] Khóa dev AppsFlyer có trong trình quản lý mật khẩu, đưa vào lúc build (ADR-0027). Khóa SDK AppLovin nhập trong ánh xạ nguồn quảng cáo "AppLovin (Bidding)" trên AdMob, không đưa vào app (mục 7).
- [ ] `app-ads.txt` trên website của nhà phát triển có dòng của AdMob và của cả chín mạng con; lấy dòng từ bảng điều khiển của từng mạng. Thiếu dòng thì mạng đó có thể không trả quảng cáo.
- [ ] Hộp thoại đồng ý đã cấu hình trên AdMob (Privacy & messaging) cho các vùng cần, có danh sách đối tác quảng cáo gồm chín mạng.
- [ ] Liên kết AdMob với Firebase (để có `ad_impression` tự động); AppsFlyer đã nhận sự kiện từ bản phát hành thử.
- [ ] Bản đầu tiên gửi `ad_placement` sang AppsFlyer hay gửi sự kiện đếm inter: đã báo nhóm thu hút người dùng (UA) và ghi ngày đổi lên dashboard (`ads-business-rules.md`, HD-07). App chuyển từ `ad_impression` sang `ad_impression_placement` cũng ghi ngày đổi và báo nhóm.
- [ ] **Data safety** trên Play Console khai đủ SDK đang dùng: Google Mobile Ads, chín mạng con, AppsFlyer, Firebase. Các mạng quảng cáo thường thu thập: mã quảng cáo và mã thiết bị khác, tương tác với quảng cáo và app, chẩn đoán, vị trí gần đúng suy từ IP. Khai đúng mục đích (quảng cáo, phân tích, chống gian lận). Trang công bố dữ liệu của từng mạng (tra ngày 2026-10-24, plan T-034):

  | Mạng | Trang | Ghi chú |
  | --- | --- | --- |
  | Google Mobile Ads | https://developers.google.com/admob/android/privacy/play-data-disclosure | IP, tương tác, chẩn đoán, mã thiết bị |
  | AppLovin | Theo mục 7 (AppLovin) | Định danh quảng cáo, thông tin máy |
  | Unity Ads | https://docs.unity.com/grow/ads/privacy/google-data-safety | Mã thiết bị, chẩn đoán, thời gian dùng app (khi bật Acquire Optimization) |
  | ironSource | https://docs.unity.com/grow/levelplay/platform/legal-resources (mục Google Data Safety Questionnaire) | Có báo cáo Google đòi khai "ứng dụng đã cài" |
  | Pangle | https://pangleglobal.com/integration/app-privacy-details-gp | Vị trí gần đúng suy từ IP, hoạt động trong app |
  | DT Exchange | https://docs.digitalturbine.com/dt-exchange/publishers/getting-started-with-dt-exchange/privacy/google-data-safety-form | Vị trí gần đúng, mã quảng cáo |
  | Moloco | Chưa thấy trang Data safety; chính sách riêng tư: https://www.moloco.com/privacy-policy | Mã quảng cáo, thông tin máy, vị trí gần đúng |
  | Meta Audience Network | Chưa thấy trang Data safety của Meta | Khai theo chính sách của Meta |
  | InMobi | Chưa thấy trang Data safety của InMobi | |
  | Mintegral | Chưa thấy trang Data safety của Mintegral | Xem ghi chú về Mintegral ở mục 7 |

  Manifest gộp có `<queries>` dò kho ứng dụng và năm gói của công cụ root máy (mục 7); không phải `QUERY_ALL_PACKAGES`.
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
