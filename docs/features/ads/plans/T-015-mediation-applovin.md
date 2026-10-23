---
ticket: T-015
title: Mediation AdMob và AppLovin
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 10M          # cỡ S, số tạm của scripts/token-usage.py estimate (chưa đủ 5 task đã đo)
blocking_points: [ads, sdk, permissions, init-order, core, build-config]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-015: MEDIATION ADMOB VÀ APPLOVIN

## 1. Tóm tắt

Thêm adapter AppLovin chính thức của Google Mobile Ads (`com.google.ads.mediation:applovin:13.6.4.2`, kéo theo AppLovin SDK 13.6.4) vào `gradle/libs.versions.toml` và `:core:ads`, để đơn vị quảng cáo có AppLovin (bidding) trong nhóm mediation trên AdMob nhận được quảng cáo AppLovin mà không đổi code. Không cần code khởi tạo riêng: `MobileAds.initialize` (đã gọi sau khi có đồng ý, ADR-0013) khởi tạo mọi adapter; plan chỉ thêm một dòng log trạng thái từng adapter sau khi khởi tạo, để kiểm được trên staging. Kiểm chứng tự động: test JVM thấy lớp adapter trong classpath của `:core:ads`, test định dạng dòng log, `scripts/check.sh` qua (build, lint, quy tắc phụ thuộc); kiểm thủ công qua log và Ad Inspector trên bản staging là việc của người.

Đây là thay đổi file được bảo vệ (`gradle/libs.versions.toml`), thêm SDK bên thứ ba và quyền mới qua manifest gộp: plan rủi ro cao, bắt buộc duyệt chéo (CLAUDE.md, mục Không được làm; risk-scoring.md, mục 1).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-9 | Khi đơn vị quảng cáo có AppLovin trong nhóm mediation trên AdMob thì app nhận được quảng cáo của AppLovin mà không đổi code. | App chỉ cần có adapter trong bản build; nhóm mediation, khóa SDK AppLovin (nhập trong hộp thoại ánh xạ của AdMob) và `app-ads.txt` là cấu hình của người trên AdMob, AppLovin và website. Bản staging dùng mã thử của Google, đơn vị thử không có nhóm mediation, nên tiêu chí này chỉ kiểm hết được trên bản có mã thật (khi phát hành) hoặc bằng chế độ thử một nguồn của Ad Inspector (câu hỏi 3). |
| KB-9 | Khi mở Ad Inspector trên staging thì thấy adapter AppLovin đã khởi tạo. | Ad Inspector có trên màn hình xem thử của T-014 (chưa làm). Trong T-015, cùng thông tin đó (trạng thái khởi tạo từng adapter, lấy từ `MobileAds.getInitializationStatus`) được ghi vào logcat; người kiểm bằng log trên staging, và bằng Ad Inspector khi T-014 xong. |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0004 (AdMob điều phối, không khởi tạo SDK quảng cáo trước khi có đồng ý), ADR-0011 (cổng chất lượng, lint), ADR-0013 (khởi tạo sau UMP, mã thật chỉ lúc build phát hành), ADR-0022 mục 8 (AdMob và AppLovin, bidding, `app-ads.txt`), ADR-0005 (không nhắm trẻ em: AppLovin không hỗ trợ đối tượng trẻ em).

### Tra cứu tài liệu chính thức

Tra ngày 2026-10-21.

| Nội dung | Kết quả | Nguồn |
| --- | --- | --- |
| Tọa độ Maven | `com.google.ads.mediation:applovin:13.6.4.2`, bản mới nhất trên Google Maven (metadata cập nhật 2026-09-18) | https://developers.google.com/admob/android/mediation/applovin, https://dl.google.com/android/maven2/com/google/ads/mediation/applovin/maven-metadata.xml |
| Phụ thuộc của adapter (file POM) | `com.applovin:applovin-sdk:13.6.4` (Maven Central), `play-services-ads:25.4.0`, `com.google.ads.mediation:common:1.1.0`, `androidx.annotation:1.5.0`, `kotlin-stdlib:1.9.0`. Dự án đang dùng `play-services-ads` 25.5.0 và Kotlin 2.2.10, Gradle chọn bản cao hơn, không hạ phiên bản nào | POM của adapter trên Google Maven, POM của `applovin-sdk` trên Maven Central |
| Tương thích GMA | Ghi chú phát hành 13.6.4.2: build và thử với Google Mobile Ads SDK 25.4.0; mức tối thiểu ghi gần nhất là 24.0.0 (bản 13.1.0.1). 25.5.0 cùng bản chính, tương thích | Mục Changelog của trang hướng dẫn |
| minSdk | Trang hướng dẫn ghi API 24 trở lên; manifest của cả adapter và SDK ghi `minSdkVersion="24"`. Dự án minSdk 26, đạt | Trang hướng dẫn, `AndroidManifest.xml` trong hai file AAR |
| Khởi tạo | "No additional code is required": `MobileAds.initialize` khởi tạo adapter. Tùy chọn quyền riêng tư phải đặt trước khi khởi tạo GMA | Trang hướng dẫn, bước 5 và mục Privacy |
| Khóa SDK AppLovin | Không đặt trong manifest: từ adapter 12.1.0.0, adapter không đọc khóa trong `AndroidManifest.xml`; với bidding, khóa nhập trong hộp thoại ánh xạ nguồn quảng cáo trên AdMob. Không có khóa hay mã nào vào repo | Changelog 12.1.0.0, trang hướng dẫn bước 3 |
| Đồng ý GDPR | Từ adapter 12.0.0.0, đồng ý từ UMP và CMP theo IAB Additional Consent được chuyển sang AppLovin, không cần code. Người phải thêm "AppLovin Corp." vào danh sách đối tác quảng cáo của thông điệp GDPR và luật các bang Mỹ trên AdMob | Trang hướng dẫn, mục EU consent and GDPR |
| Luật các bang Mỹ | Muốn chọn không bán dữ liệu thì gọi `AppLovinPrivacySettings.setDoNotSell(true)` trước khi khởi tạo GMA; plan này không làm (câu hỏi 4) | Trang hướng dẫn, mục US states privacy laws |
| Trẻ em | AppLovin không hỗ trợ đối tượng trẻ em; từ 13.0.0.1 adapter tự tắt khi request đánh dấu trẻ em. Khớp ADR-0005 | Trang hướng dẫn |
| Ad Inspector | Đăng ký máy thử trên AdMob, bật chế độ thử trên AppLovin, dùng "single ad source testing" với nguồn "AppLovin (Bidding)". Tắt chế độ thử trước khi phát hành | Trang hướng dẫn, mục Test your implementation; https://developers.google.com/admob/android/ad-inspector |
| `app-ads.txt` | Bắt buộc có, thêm dòng của AppLovin theo hướng dẫn của AppLovin; thiếu thì doanh thu giảm đáng kể | Trang hướng dẫn, mục app-ads.txt |
| R8 | SDK AppLovin có sẵn `proguard.txt` trong AAR; trang hướng dẫn không yêu cầu quy tắc riêng | File AAR |

### Manifest mà SDK AppLovin gộp vào app

Đọc từ `AndroidManifest.xml` trong `applovin-sdk-13.6.4.aar` ngày 2026-10-21, so với manifest gộp hiện tại của `stagingDebug`:

- **Quyền mới** (chưa có trong manifest gộp hiện tại): `android.permission.VIBRATE`, `android.permission.ACCESS_WIFI_STATE` (cho Open Measurement SDK), `com.android.vending.CHECK_LICENSE`. Cả ba là quyền thường, không hiện hộp thoại xin quyền. Đây là điểm chặn 3 (quyền mới), dù `app/src/main/AndroidManifest.xml` không bị sửa (câu hỏi 1). Người rà soát đọc bytecode (2026-10-23) để biết SDK dùng các quyền này thế nào:
  - `VIBRATE`: hàm `com.applovin.impl.u7.a(float, long, ...)` gọi `Vibrator.vibrate` (rung khi quảng cáo yêu cầu), **không kiểm quyền trước và không bắt lỗi** (không có bảng bắt ngoại lệ). Bỏ quyền này thì lần quảng cáo yêu cầu rung sẽ ném `SecurityException`, có thể làm app dừng.
  - `CHECK_LICENSE`: lớp `com.applovin.impl.s2` (`LicenseVerificationManager`) gắn vào dịch vụ cấp phép của Play (`com.android.vending.licensing.ILicensingService`), lời gọi nằm trong khối bắt lỗi. Bỏ quyền thì việc kiểm cấp phép thất bại và được ghi log, không làm app dừng (theo bytecode, chưa thử trên máy).
  - `ACCESS_WIFI_STATE`: không tìm thấy lời gọi trong `classes.jar` của SDK; có thể dùng trong phần Open Measurement. Không đánh giá được ảnh hưởng khi bỏ.
- Quyền đã có: `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID`.
- Thẻ `<queries>` cho Custom Tabs, các activity toàn màn hình và activity gỡ lỗi của AppLovin (`MaxDebugger...`), hai service không xuất ra ngoài.
- **`com.applovin.sdk.AppLovinInitProvider`** với `initOrder="101"` (chú thích trong manifest của SDK: "Init order is 101 so we're before Firebase/Google which uses 100"), chạy lúc tiến trình khởi động, trước `MobileAdsInitProvider` và Firebase. Đây là điểm chặn 5 (thứ tự khởi tạo SDK) và cần đối chiếu với ADR-0004 ("không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý").
  - Người rà soát đọc bytecode của provider (`javap` trên `classes.jar` của SDK 13.6.4, ngày 2026-10-23). `onCreate` gọi ba hàm: `com.applovin.impl.i1.a(context)` mở một luồng nền đọc **Google Advertising ID** (`AdvertisingIdClient.getAdvertisingIdInfo`, kèm cờ giới hạn theo dõi) và **App Set ID** (`AppSet.getClient(context).getAppSetIdInfo()`); `com.applovin.impl.sdk.l.a(context)` tạo một đối tượng nội bộ dùng chung; `l.b(context)` giữ `ApplicationContext` và bật một cờ. Không thấy gọi mạng trực tiếp trong ba hàm này, nhưng không loại trừ được ở tầng sâu hơn vì mã bị làm rối.
  - Kết luận: bản đầu của plan hiểu sai ("provider chỉ giữ `Context`"). Provider **đọc mã định danh quảng cáo trên máy trước khi có đồng ý**. Chưa thấy gửi đi trước đồng ý, nhưng đọc định danh trên máy trước đồng ý đã là điểm cần người quyết theo ADR-0004 và quy định ePrivacy của EU (câu hỏi 2). Lưu ý: `MobileAdsInitProvider` của Google cũng chạy lúc khởi động từ trước tới nay, nhưng tài liệu của Google nói nó không thu thập dữ liệu trước `MobileAds.initialize`; AppLovin không có tài liệu tương tự.

### Phương án bỏ provider và quyền bằng manifest merger (để người duyệt chọn)

Tra ngày 2026-10-23. Tài liệu của Google (hướng dẫn mediation AppLovin) và của AppLovin (trang tích hợp MAX Android, trang quyền riêng tư) **không nhắc tới `AppLovinInitProvider`, các quyền trên hay việc bỏ chúng**. AppLovin khuyên khởi tạo SDK "as early as possible" và yêu cầu đặt cờ đồng ý trước khi khởi tạo. Vì vậy mọi cách bỏ dưới đây là cách làm không có trong tài liệu của nhà cung cấp, chỉ dựa vào cơ chế chung của Android.

| Phương án | Cách làm | Ưu | Nhược, rủi ro |
| --- | --- | --- | --- |
| A. Giữ nguyên (bản đầu của plan) | Không sửa manifest | Đúng cách AppLovin phát hành, ít rủi ro chạy sai | Đọc Advertising ID và App Set ID trước đồng ý; ba quyền mới vào mọi app |
| B. Bỏ provider | `<provider android:name="com.applovin.sdk.AppLovinInitProvider" android:authorities="${applicationId}.applovininitprovider" tools:node="remove" />` | Không có mã AppLovin nào chạy trước `MobileAds.initialize` (sau đồng ý); hợp ADR-0004 | AppLovin không hỗ trợ chính thức; `l.b` bật một cờ mà phần khác của SDK có thể dựa vào. Adapter truyền `Context` khi GMA khởi tạo nên nhiều khả năng vẫn chạy, nhưng phải kiểm trên máy: adapter `READY`, Ad Inspector tải được quảng cáo thử AppLovin, không lỗi trong logcat. Mỗi lần nâng SDK phải kiểm lại |
| C. Bỏ `CHECK_LICENSE` | `<uses-permission android:name="com.android.vending.CHECK_LICENSE" tools:node="remove" />` | Bớt một quyền; lời gọi đã được bắt lỗi | AppLovin có thể dùng kết quả kiểm cấp phép cho chống gian lận, ảnh hưởng doanh thu chưa rõ |
| D. Bỏ `VIBRATE` | Như C, với `android.permission.VIBRATE` | Bớt một quyền | **Không khuyên dùng**: lời gọi rung không bắt lỗi, có thể làm app dừng khi quảng cáo yêu cầu rung |
| E. Bỏ `ACCESS_WIFI_STATE` | Như C | Bớt một quyền | Chưa biết phần nào dùng; có thể làm sai đo lường hiển thị (Open Measurement), ảnh hưởng doanh thu |

Nơi đặt dòng `tools:node="remove"`: Android gộp manifest theo thứ tự ưu tiên biến thể, rồi manifest chính của app, rồi thư viện; `tools:node="remove"` xóa phần tử đến từ manifest có ưu tiên thấp hơn. Đặt trong `app/src/main/AndroidManifest.xml` là chắc chắn có tác dụng (ưu tiên cao hơn mọi thư viện). Đặt trong manifest của `:core:ads` (chưa có file) thì giữ việc quảng cáo trong một module, nhưng tài liệu không nói rõ dấu `tools` của thư viện có tác dụng lên phụ thuộc của chính nó không; nếu chọn cách này phải kiểm bằng manifest gộp của `stagingDebug`. Cả hai cách đều thêm file vào danh sách mục 3.2 (plan phiên bản 2). Mỗi app có thể chọn khác bằng manifest của mình.

Nguồn:

- Google, Integrate AppLovin with mediation: https://developers.google.com/admob/android/mediation/applovin
- AppLovin, MAX Android integration và Privacy: https://support.applovin.com/en/max/android/overview/integration, https://support.applovin.com/en/max/android/overview/privacy
- Android, Merge multiple manifest files (`tools:node="remove"`, thứ tự ưu tiên): https://developer.android.com/build/manage-manifests
- Android, App Startup (mẫu chính thức bỏ một provider khởi tạo bằng `tools:node="remove"`): https://developer.android.com/topic/libraries/app-startup
- Bytecode của `applovin-sdk-13.6.4.aar` (đọc bằng `javap`), không lưu vào repo.

## 3. Giải pháp

### 3.1. Cách làm

1. `gradle/libs.versions.toml`: thêm phiên bản `admobMediationApplovin = "13.6.4.2"` ở nhóm `# Ads`, và thư viện `admob-mediation-applovin = { group = "com.google.ads.mediation", name = "applovin", version.ref = "admobMediationApplovin" }` cạnh `play-services-ads`. Không đổi phiên bản nào khác. Không cần thêm kho Maven: `settings.gradle.kts` đã có `google()` và `mavenCentral()`.
2. `core/ads/build.gradle.kts`: `implementation(libs.admob.mediation.applovin)`. Adapter chỉ nằm trong `:core:ads` (ADR-0004); `:app` nhận nó qua phụ thuộc chạy của `:core:ads`, feature không thấy nó. Không thêm phụ thuộc module nào, quy tắc ADR-0001 không đổi.
3. Không thêm code khởi tạo AppLovin, không gọi API của AppLovin. Thứ tự khởi tạo của app giữ nguyên: UMP, rồi `MobileAds.initialize` khi `canRequestAds` (ADR-0013).
4. Ghi log trạng thái adapter sau khi khởi tạo, để kiểm KB-9 trên staging mà chưa cần T-014:
   - Tạo `MediationStatus.kt` trong `:core:ads` với hàm `internal fun adapterStatusLines(statuses: Map<String, AdapterStatus>): List<String>`: mỗi adapter một dòng, sắp theo tên lớp, dạng `Adapter <tên lớp>: <READY|NOT_READY>, <độ trễ> ms, <mô tả>`. Mô tả do SDK trả, không chứa dữ liệu người dùng.
   - Trong `AdsController.onConsentUpdated`, sau `MobileAds.initialize(context)`, lấy `MobileAds.getInitializationStatus()` và ghi từng dòng bằng `AppLogger` (xem dòng dưới). Không đổi luồng, không đổi giao diện công khai.
   - Ghi bằng `AppLogger` (ADR-0019), thẻ `Ads`: T-009 đã thêm phụ thuộc `:core:logging` cho `:core:ads`, nên T-015 chạy sau T-009 (người rà soát sửa ngày 2026-10-21, thay cho `android.util.Log` ở bản đầu).
5. Tài liệu:
   - `docs/ops/ads-guide.md` mục 7: bỏ nhãn "(đợt 2)" của mục; ghi tọa độ và phiên bản adapter, không cần code khởi tạo, khóa SDK nhập trên AdMob chứ không vào app, việc của người trên AdMob (nhóm mediation, ánh xạ AppLovin bidding, thêm "AppLovin Corp." vào đối tác quảng cáo của thông điệp GDPR và các bang Mỹ), quyền và provider mà SDK gộp vào, cách kiểm bằng log `Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: READY` và Ad Inspector (single ad source testing), nâng cấp adapter là plan rủi ro cao. Câu mở đầu về "(đợt 2)" bỏ AppLovin khỏi danh sách phần chưa có. Mục 9: thêm "AppLovin Corp." trong danh sách đối tác quảng cáo của thông điệp đồng ý, tắt chế độ thử của AppLovin và AdMob. Mục Nguồn: thêm trang Ad Inspector.
   - `docs/ops/release.md`: mục 1 thêm 1.4 `app-ads.txt` (một lần mỗi nhà phát triển, trên tên miền ghi ở trang Play của app: dòng của AdMob lấy từ AdMob, mục Apps > app-ads.txt; dòng của AppLovin lấy từ tài khoản AppLovin; file nằm trên website, không vào repo); mục 2.4 thêm bước kiểm `app-ads.txt` trên AdMob báo đã xác minh trước khi chuyển sang Production.
   - Không sửa ADR-0013 (đã `accepted`); danh sách quyền mới ghi ở `ads-guide.md` và câu hỏi 1.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. Thêm phiên bản `admobMediationApplovin` và thư viện `admob-mediation-applovin` (mục 3.1, bước 1) |
| Sửa | `core/ads/build.gradle.kts` | `implementation(libs.admob.mediation.applovin)` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MediationStatus.kt` | `adapterStatusLines` (mục 3.1, bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Ghi log trạng thái adapter sau `MobileAds.initialize`; cập nhật chú thích lớp: adapter mediation khởi tạo cùng SDK |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MediationAdapterClasspathTest.kt` | Test lớp adapter AppLovin có trong classpath (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MediationStatusTest.kt` | Test `adapterStatusLines` (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Mục mở đầu, mục 7, mục 9, mục Nguồn (mục 3.1, bước 5) |
| Sửa | `docs/ops/release.md` | Mục 1.4 `app-ads.txt`, bước kiểm ở mục 2.4 |
| Sửa | `docs/features/ads/plans/T-015-mediation-applovin.md` | Nhật ký thực hiện (mục 10) |
| Sửa | `docs/features/ads/tickets/T-015-mediation-applovin.md` | Trạng thái ticket |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Nơi đọc các file bị đổi (L-020), tìm bằng `scripts/plan-check.py` và `grep -rn`:

- `gradle/libs.versions.toml`: `build-logic/settings.gradle.kts` nạp file này làm catalog `libs` cho convention plugin, và plugin chỉ đọc các khóa đã có (`compileSdk`, `minSdk`, `junit4`...), không đọc khóa mới; `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` chỉ nhắc tên file trong chú thích về lint; `scripts/plan-check.py` chỉ có tên file trong danh sách đường dẫn rủi ro cao. Không file nào bị ảnh hưởng, không sửa `build-logic/` hay script.
- `docs/ops/release.md`: `scripts/release.sh` chỉ nhắc tên file trong thông báo lỗi ("Xem docs/ops/release.md"), không đọc nội dung; plan không chạm và không chạy script này (CLAUDE.md, mục Không được làm). Không bị ảnh hưởng.
- `docs/STATUS.md`: `scripts/tests/run.sh` chỉ đọc `STATUS.md` của app mới và repo giả do test tạo trong thư mục tạm, không đọc file của repo. Không bị ảnh hưởng; `scripts/status.py --check` xác nhận trang khớp. `core/ads/build.gradle.kts` và `AdsController.kt` không được script hay test nào đọc theo tên. Ảnh chuẩn screenshot không đổi: test và preview không cấp `AdsController`, nên không có quảng cáo trong ảnh (ADR-0013).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng, tần suất. Thêm một mạng con mediation (AppLovin, bidding) theo ADR-0022 mục 8. Mã quảng cáo không đổi; staging vẫn dùng mã thử của Google.
- Sự kiện phân tích: Không chạm. SDK AppLovin tự gửi dữ liệu về AppLovin khi phục vụ quảng cáo (định danh quảng cáo, thông tin máy), theo tài liệu chỉ sau khi được khởi tạo qua GMA, tức sau khi có đồng ý; riêng provider đọc Advertising ID và App Set ID trên máy lúc khởi động (mục 2, câu hỏi 2); khai báo Data safety của từng app phải tính tới AppLovin (câu hỏi 1).
- File R2: Không chạm.

## 4. Kiểm thử

Test JVM, chạy trong `:core:ads:testDebugUnitTest` (một phần của `scripts/check.sh`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-9, tiêu chí 1: adapter có trong bản build | Unit test | `MediationAdapterClasspathTest.appLovinAdapter_isOnClasspath`: `Class.forName("com.google.ads.mediation.applovin.AppLovinMediationAdapter", false, javaClass.classLoader)` không ném lỗi (không chạy khối khởi tạo tĩnh, để tránh gọi API Android trên JVM) và lớp đó gán được cho `com.google.android.gms.ads.mediation.Adapter`. Tên lớp viết thẳng trong test (không qua hằng số của code), nên test biên dịch được và thất bại với `ClassNotFoundException` trước khi thêm thư viện |
| KB-9, tiêu chí 2: log có trạng thái adapter | Unit test | `MediationStatusTest.statusLines_listOneLinePerAdapterSortedByName`: hai `AdapterStatus` giả (READY 120 ms, NOT_READY 0 ms "Timeout") cho đúng hai dòng theo thứ tự tên lớp, dạng ở mục 3.1 |
| Như trên | Unit test | `MediationStatusTest.statusLines_emptyWhenNoAdapter`: map rỗng cho danh sách rỗng |
| Build, lint, quy tắc phụ thuộc, ktlint, test cũ và ảnh chuẩn cũ không đổi | `scripts/check.sh` | Qua toàn bộ, gồm `checkModuleDependencies`, `:app:lintStagingDebug` (lint cả thư viện, `checkDependencies = true`) và `:app:assembleStagingDebug` |
| Adapter nằm trong classpath chạy của `:core:ads` và app, đúng phiên bản | Lệnh, AI ghi kết quả vào nhật ký | `./gradlew :core:ads:dependencies --configuration releaseRuntimeClasspath` và `./gradlew :app:dependencies --configuration stagingReleaseRuntimeClasspath`: có `com.google.ads.mediation:applovin:13.6.4.2`, `com.applovin:applovin-sdk:13.6.4`; `play-services-ads` vẫn 25.5.0, `user-messaging-platform` vẫn 4.0.0 |
| Manifest gộp chỉ thêm đúng các quyền đã nêu ở mục 2 | Lệnh, AI ghi kết quả vào nhật ký | `./gradlew :app:processStagingDebugMainManifest`, rồi so danh sách `uses-permission` trong `app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml` với danh sách hiện tại: chỉ thêm `VIBRATE`, `ACCESS_WIFI_STATE`, `com.android.vending.CHECK_LICENSE`. Có quyền khác thì dừng (mục 7) |
| Kiểm thủ công 1, KB-9 tiêu chí 2 (người, trên bản staging từ `staging.yml`, bản release có R8) | Kiểm tra thủ công | Cài bản staging mới, chấp nhận hộp thoại đồng ý, ghi phiên thử bằng `scripts/device-session.sh`; trong logcat có dòng `Adapter com.google.ads.mediation.applovin.AppLovinMediationAdapter: READY`. Không có dòng này hay trạng thái `NOT_READY` thì tạo phản hồi cho T-015. Khi T-014 xong: mở Ad Inspector từ màn hình xem thử, thấy AppLovin trong danh sách adapter đã khởi tạo |
| Kiểm thủ công 2, ADR-0004 (người, cùng phiên thử) | Kiểm tra thủ công | Xóa dữ liệu app, mở app, chưa trả lời hộp thoại đồng ý: logcat không có dòng của thẻ `AppLovinSdk` báo khởi tạo hay gửi yêu cầu, và không có dòng `Adapter ...` nào. Sau khi đồng ý mới có. Có thì báo ngay cho người duyệt (câu hỏi 2). Lưu ý: logcat không cho thấy việc provider đọc Advertising ID (mục 2), nên kiểm này chỉ chứng minh không có yêu cầu mạng có log; muốn chắc hơn thì xem lưu lượng mạng trước đồng ý (ví dụ proxy trên máy thử), việc của người. Nếu người duyệt chọn phương án B (mục 2): kiểm thêm manifest gộp không còn `AppLovinInitProvider`, và kiểm thủ công 1 vẫn đạt |
| Kiểm thủ công 3, KB-9 tiêu chí 1 (người, khi một app có đơn vị quảng cáo thật) | Kiểm tra thủ công | Trên AdMob đã có nhóm mediation có AppLovin (bidding) và máy thử đã đăng ký: Ad Inspector, single ad source testing với "AppLovin (Bidding)" tải được quảng cáo thử của AppLovin. Không làm trên bản staging của dự án mẫu vì app id thử của Google không có nhóm mediation (câu hỏi 3) |

**Ngoại lệ viết test trước:** sửa `ads-guide.md`, `release.md` và thêm dòng thư viện là tài liệu và cấu hình, không viết test trước được ngoài test classpath ở trên. Dòng log trong `AdsController` không có test riêng (lớp này không có unit test vì gọi thẳng SDK); phần định dạng nằm trong `adapterStatusLines` đã có test. Người duyệt đồng ý ngoại lệ này.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

1. Quảng cáo: thêm adapter mediation (điểm 1).
2. SDK bên thứ ba mới: AppLovin SDK 13.6.4 (điểm 2).
3. Quyền mới trong manifest gộp: `VIBRATE`, `ACCESS_WIFI_STATE`, `com.android.vending.CHECK_LICENSE` (điểm 3).
4. Thứ tự khởi tạo: `AppLovinInitProvider` chạy lúc tiến trình khởi động (điểm 5).
5. Module core dùng chung `:core:ads` và cấu hình build dùng chung `gradle/libs.versions.toml`, file được bảo vệ theo `CLAUDE.md` (điểm 7).

Mức: **Cao**, không tính tổng điểm. Bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt vì chạm quảng cáo và dữ liệu gửi ra khỏi máy (risk-scoring.md, mục 3). Chấm năm tiêu chí để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Cấu hình build dùng chung và một module core, không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận; nhưng provider đọc định danh quảng cáo trước đồng ý (mục 2) và cách xử lý (giữ hay bỏ provider, quyền) chưa chốt, có thể đổi danh sách file |
| C. Khả năng kiểm chứng tự động | 2 | Có test classpath và build, nhưng nhận quảng cáo AppLovin thật và Ad Inspector chỉ kiểm thủ công trên máy |
| D. Ảnh hưởng chéo | 2 | Mọi app tạo từ dự án mẫu nhận SDK, quyền và kích thước app tăng (AAR của SDK khoảng 3,8 MB); `AdsController.kt` và `core/ads/build.gradle.kts` cũng là file T-009, T-010 sẽ sửa |
| E. Độ mới của kỹ thuật | 2 | Mediation chưa có trong dự án nhưng là cách làm phổ biến, có tài liệu chính thức |
| **Tổng** | 10 | **Mức: Cao** (đã cao vì điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `gradle/libs.versions.toml`, `core/ads/build.gradle.kts`, `AdsController.kt`, manifest gộp của `:app` (quyền, provider, activity của AppLovin), kích thước app. Hai tài liệu vận hành dùng cho mọi app.
- Task khác trong sprint có thể xung đột: T-009 (tần suất, công tắc tổng) và T-010 (native small, medium) sẽ sửa `AdsController.kt` và có thể `core/ads/build.gradle.kts`; T-007 không chạm `:core:ads`. Phần T-015 sửa trong `AdsController.kt` chỉ là vài dòng trong `onConsentUpdated`, xung đột nếu có thì nhỏ. T-014 (màn hình xem thử, nút Ad Inspector) hoàn thiện cách kiểm KB-9 tiêu chí 2, nhưng T-015 không phụ thuộc nó.
- Đề xuất thứ tự chạy: sau T-009 (bắt buộc, ticket ghi `depends_on: [T-009]`: T-009 thêm `:core:logging` và `AppLogger` vào `:core:ads`), trước T-010 nếu cùng phiên để T-010 dựng trên `core/ads/build.gradle.kts` đã có adapter, và trước T-014 để kiểm KB-9 bằng Ad Inspector ngay. Không chạy cùng lúc với T-009, T-010 (cùng sửa `AdsController.kt`, `core/ads/build.gradle.kts`). (Người rà soát sửa ngày 2026-10-23: bản trước ghi "không phụ thuộc ticket nào, chạy trước T-009", mâu thuẫn với mục 3.1 bước 4 và ticket.)

## 7. Điều kiện dừng riêng

1. Phiên bản adapter `13.6.4.2` không tải được, hoặc Gradle chọn `play-services-ads` khác 25.5.0, `user-messaging-platform` khác 4.0.0, hay bất kỳ thư viện nào của dự án bị đổi phiên bản chính.
2. Manifest gộp có quyền mới ngoài ba quyền ở mục 2, hoặc có xung đột manifest cần sửa `app/src/main/AndroidManifest.xml`.
3. Lint báo lỗi từ thư viện mới mà chỉ sửa được bằng cách tắt quy tắc, thêm baseline hay sửa `build-logic/`.
4. Build cần quy tắc R8 (`proguard-rules.pro`, `consumer-rules.pro`) hay kho Maven mới.
5. Tài liệu chính thức lúc chạy khác với mục 2 (phiên bản tương thích, yêu cầu khóa SDK trong manifest, yêu cầu code khởi tạo): ghi chỗ khác, không tự đổi cách làm.
6. Cần khóa SDK AppLovin, mã quảng cáo hay app id thật ở bất kỳ đâu trong repo.

## 8. Câu hỏi cho người duyệt

1. **SDK AppLovin thêm ba quyền thường vào mọi app: `VIBRATE`, `ACCESS_WIFI_STATE`, `com.android.vending.CHECK_LICENSE`.** Không quyền nào hiện hộp thoại xin quyền, nhưng là quyền mới (điểm chặn 3) và Data safety của từng app phải tính tới AppLovin như một bên nhận dữ liệu.
   - Đề xuất của AI: giữ cả ba như SDK khai báo; ghi danh sách vào `ads-guide.md` mục 7 để mỗi app cập nhật Data safety (như ADR-0013 đã làm với quyền của Google Mobile Ads). Phương án bỏ từng quyền và rủi ro ở mục 2 (bảng phương án C, D, E): không bỏ `VIBRATE` (lời gọi rung không bắt lỗi, có thể làm app dừng); bỏ `CHECK_LICENSE` an toàn về kỹ thuật theo bytecode nhưng ảnh hưởng doanh thu chưa rõ; `ACCESS_WIFI_STATE` chưa đánh giá được. Bỏ quyền nào thì cần plan phiên bản 2 (thêm manifest vào danh sách file).
   - Người duyệt quyết: giữ cả ba, hay bỏ `CHECK_LICENSE`?
2. **`AppLovinInitProvider` chạy lúc tiến trình khởi động, trước khi có đồng ý, và đọc Advertising ID cùng App Set ID trên máy** (người rà soát đọc bytecode, mục 2; bản đầu của plan hiểu sai là provider chỉ giữ `Context`). Chưa thấy gửi đi trước đồng ý. Tài liệu của Google và AppLovin không nói gì về provider này hay cách tắt nó.
   - Phương án A (giữ, bản đầu của plan): chấp nhận như SDK phát hành, kiểm thủ công 2 trong phiên thử staging đầu tiên. Rủi ro: đọc định danh trên máy trước đồng ý, khó bảo vệ là đúng tinh thần ADR-0004 ("không khởi tạo SDK quảng cáo trước khi xử lý xong sự đồng ý").
   - Phương án B: bỏ provider bằng `tools:node="remove"` (mục 2), để không có mã AppLovin nào chạy trước `MobileAds.initialize`. Cách này dùng cơ chế chính thức của Android (giống mẫu App Startup) nhưng AppLovin không ghi là hỗ trợ; phải kiểm trên máy adapter vẫn `READY` và tải được quảng cáo thử AppLovin qua Ad Inspector.
   - Đề xuất của người rà soát: B, đặt dòng bỏ trong `app/src/main/AndroidManifest.xml` (chắc chắn có tác dụng; chỉ xóa, không thêm quyền), làm plan phiên bản 2 thêm file này vào mục 3.2, thêm bước kiểm manifest gộp không còn provider và thêm vào điều kiện dừng: adapter `NOT_READY` sau khi bỏ provider thì dừng, quay về phương án A cần người duyệt lại. Nếu người duyệt chọn A thì ghi rõ trong mục 9 rằng việc đọc định danh trước đồng ý được chấp nhận và vì sao.
3. **KB-9 tiêu chí 1 không kiểm được trên staging của dự án mẫu** vì app id và đơn vị thử của Google không có nhóm mediation.
   - Đề xuất của AI: T-015 kiểm phần app (adapter có, khởi tạo READY); tiêu chí 1 kiểm ở app đầu tiên có đơn vị quảng cáo thật (Camera Pose), bằng Ad Inspector single ad source testing, ghi vào báo cáo phiên thử của app đó. Ticket T-015 được coi là xong khi kiểm thủ công 1 và 2 đạt.
4. **Luật quyền riêng tư các bang Mỹ** (`AppLovinPrivacySettings.setDoNotSell`): plan không làm.
   - Đề xuất của AI: không làm lúc này; GDPR đã được chuyển tự động từ UMP. Khi một app nhắm thị trường Mỹ và bật thông điệp các bang Mỹ trên AdMob, làm ticket riêng (cần đặt trước `MobileAds.initialize`, chạm thứ tự khởi tạo).
5. **Log trạng thái adapter.** Đã chốt khi rà bốn plan (2026-10-21): dùng `AppLogger`, T-015 phụ thuộc T-009 (T-009 thêm `:core:logging` vào `:core:ads`). Không còn câu hỏi.
6. **Nâng cấp adapter về sau:** mỗi lần nâng là plan rủi ro cao (risk-scoring.md, điểm 1). Có muốn ghi lịch kiểm phiên bản adapter (ví dụ mỗi lần nâng `play-services-ads`) vào `ads-guide.md` không?
   - Đề xuất của AI: có, một dòng trong mục 7: nâng `play-services-ads` thì nâng adapter cùng plan, chọn bản adapter có ghi chú "Built and tested with" khớp bản GMA mới.
7. **Duyệt chéo khi nhóm chỉ có một dev:** theo risk-scoring.md mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
