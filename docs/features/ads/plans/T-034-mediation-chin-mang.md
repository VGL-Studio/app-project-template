---
ticket: T-034
title: Mediation chín mạng
status: stopped                 # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M, số tạm của scripts/token-usage.py estimate (chưa đủ 5 task đã đo)
blocking_points: [ads, sdk, init-order, core, build-config]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-034: MEDIATION CHÍN MẠNG

## 1. Tóm tắt

**Phiên bản 2 (2026-10-24, phiên RUN-20261024-02 soạn, `draft`).** Bản 1 dừng ở bước 4 vì môi trường cloud của phiên không tải được từ hai kho Maven mới: proxy của môi trường từ chối kết nối tới `artifact.bytedance.com` và `dl-maven-android.mintegral.com` (403, `connect_rejected`), đúng điều kiện chuyển của mục 3.4. Vì sao plan chưa đủ tốt: lần chạy thử ở mục 4.1 làm trên máy dev, không làm trong môi trường cloud của phiên, nên không thấy proxy chặn hai kho này (cùng loại với R2 ở L-050). Cách làm, danh sách file, test và chấm rủi ro không đổi. Thay đổi: mục 3.1 thêm cách tiếp tục trên nhánh cũ; điều kiện trước khi chạy lại là người cho phép hai tên miền trên trong chính sách mạng của môi trường cloud (hoặc chạy task trên máy dev). Mục 9 chờ duyệt lại.

Thêm adapter chính thức của AdMob cho tám mạng còn lại (Meta Audience Network, DT Exchange, InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads) và SDK Unity Ads vào `gradle/libs.versions.toml` và `:core:ads`, cạnh AppLovin của T-015. Hai kho Maven mới (Pangle, Mintegral) vào `settings.gradle.kts`, mỗi kho chỉ cho đúng nhóm thư viện của nó. Không có code khởi tạo riêng: `MobileAds.initialize` khởi tạo mọi adapter và dòng log trạng thái của T-015 ghi từng adapter.

Đo thật ngày 2026-10-24: manifest gộp `stagingDebug` **không thêm quyền nào** (mọi quyền tám SDK khai báo đã có từ Google Mobile Ads). SDK kéo vào 5 provider, 1 bộ khởi tạo App Startup, 1 receiver xuất ra ngoài, 39 activity, 2 service và 19 mục `<queries>`. Plan bỏ hai phần tử bằng `tools:node="remove"` trong `core/ads/src/main/AndroidManifest.xml`: provider của Meta (khởi động SDK Meta lúc mở app, trước khi có đồng ý) và receiver của Mintegral (xuất ra ngoài, không bao giờ nhận được broadcast hệ thống từ API 24). Giữ việc bỏ `AppLovinInitProvider` của T-015.

Khóa SDK AppLovin: đọc bytecode adapter `13.6.4.2` cho thấy adapter chỉ lấy khóa từ cấu hình mediation mà AdMob gửi xuống (`MediationConfiguration.getServerParameters().getString("sdkKey")`), không đọc từ app. Plan đề xuất app không mang khóa nào; khóa chỉ nằm trong hộp thoại ánh xạ của AdMob; `scripts/secret-scan.py` thêm mẫu chặn khóa AppLovin trong repo (câu hỏi 2). Chạy thử cho thấy lint báo lỗi `NotificationPermission` từ Picasso (phụ thuộc của InMobi); plan bỏ qua đúng phát hiện đó bằng `app/lint.xml`, cần người duyệt đồng ý (câu hỏi 7). Kiểm chứng tự động: test classpath cho chín adapter và SDK Unity Ads, test manifest gộp (danh sách quyền đúng, các phần tử bỏ đi không còn, không có khóa AppLovin), test của `secret-scan.py`, `scripts/check.sh` qua (đã chạy thử, mục 4.1).

Plan thêm tám SDK bên thứ ba, sửa file được bảo vệ (`gradle/libs.versions.toml`) và cấu hình build dùng chung (`settings.gradle.kts`): rủi ro cao, bắt buộc duyệt chéo.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-9 | Khi đơn vị quảng cáo có một trong chín mạng trong nhóm mediation trên AdMob thì app nhận được quảng cáo của mạng đó mà không đổi code. | App chỉ cần có adapter và SDK của mạng trong bản build. Nhóm mediation, mã của từng mạng (khóa SDK AppLovin, app key ironSource, game ID Unity, app ID Pangle...) và `app-ads.txt` là cấu hình của người trên AdMob và trang của từng mạng. Chỉ kiểm hết được trên app có mã AdMob thật (câu hỏi 1). Hai mạng (Unity, ironSource) yêu cầu nạp banner, interstitial, rewarded bằng `Activity`; `:core:ads` hiện nạp interstitial bằng `ApplicationContext`, nên hai mạng này có thể không thắng đấu giá cho interstitial tới khi T-021, T-026 đổi cách nạp (câu hỏi 4) |
| KB-9, tiêu chí 2, vế 1 (sửa 2026-10-24 theo câu hỏi 1) | Khi mở staging thì log trạng thái (và Ad Inspector khi mở được) liệt kê đủ chín adapter; mã app thử của Google không có cấu hình mediation nên trên staging adapter báo `NOT_READY`. | Đọc bytecode (mục 2, Tra cứu): **mọi** adapter lấy mã của mạng từ cấu hình mediation mà AdMob gửi xuống, nên trên staging cả chín báo `NOT_READY` kèm lỗi "thiếu mã", như AppLovin ở FB-20261023-01. Đạt khi log trạng thái của một lần `MobileAds.initialize` có đủ chín adapter, chứng minh chúng có trong bản build và được khởi tạo. Kiểm: test classpath (adapter có trong bản build) và kiểm thủ công 1 (mục 4) |
| KB-9, tiêu chí 2, vế 2 (spec ghi thành dòng riêng "Thêm 2026-10-24") | Khi một app có mã AdMob thật và nhóm mediation đủ chín mạng thì Ad Inspector thấy cả chín adapter đã khởi tạo. | Không kiểm được trong dự án mẫu (không có mã thật). Kiểm ở app đầu tiên có mã AdMob thật: kiểm thủ công 4 (mục 4), ghi vào báo cáo phiên thử của app đó (câu hỏi 1) |
| KB-9 | Khóa SDK của AppLovin không nằm trong repo; bản staging chạy được khi không có khóa đó. | Adapter không đọc khóa từ app (mục 2). Plan: app không mang khóa; `secret-scan.py` chặn khóa trong repo; test manifest gộp kiểm staging không có `applovin.sdk.key`. Staging chạy được khi không có khóa: đã đúng hôm nay (FB-20261023-01: app chạy, adapter `NOT_READY`) (câu hỏi 2) |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0004 (AdMob điều phối, `:core:ads` là lớp trung gian, staging chỉ dùng mã thử), ADR-0013 (mã production lúc build; thư viện quảng cáo chỉ trong `:core:ads`), ADR-0026 mục 3 và 8 (khởi động theo PARTNER; chín mạng; khóa SDK AppLovin là cấu hình của app, không vào repo), ADR-0027 (cách đưa khóa lúc build của AppsFlyer, mẫu cho phương án B của câu hỏi 2), ADR-0005 (không nhắm trẻ em), ADR-0011 (lint). Luật: `ads-business-rules.md` M-01, M-02, M-03, M-07, AR-04. Spec ads câu hỏi 18 (gỡ quyền không cần, đưa danh sách vào mục 6).

### Tra cứu tài liệu chính thức

Tra ngày 2026-10-24. Phiên bản lấy từ `maven-metadata.xml` của Google Maven (bản mới nhất ổn định, M-02) và đối chiếu mục Changelog của trang hướng dẫn mediation của từng mạng.

| Mạng | Adapter (Google Maven) | SDK của mạng, kho | Changelog: built and tested with | Kho Maven mới | Nguồn |
| --- | --- | --- | --- | --- | --- |
| Meta Audience Network | `com.google.ads.mediation:facebook:6.22.0.1` | `com.facebook.android:audience-network-sdk:6.22.0`, Maven Central | GMA 25.4.0, Meta SDK 6.22.0 | Không | https://developers.google.com/admob/android/mediation/meta |
| DT Exchange | `com.google.ads.mediation:fyber:8.5.0.0` | `com.fyber:marketplace-sdk:8.5.0`, Maven Central | GMA 25.5.0, DT SDK 8.5.0 | Không | https://developers.google.com/admob/android/mediation/dt-exchange |
| InMobi | `com.google.ads.mediation:inmobi:11.5.0.0` | `com.inmobi.monetization:inmobi-ads-kotlin:11.5.0`, Maven Central | GMA 25.5.0, InMobi Kotlin SDK 11.5.0 | Không | https://developers.google.com/admob/android/mediation/inmobi |
| ironSource | `com.google.ads.mediation:ironsource:9.6.1.0` | `com.unity3d.ads-mediation:mediation-sdk:9.6.1`, Maven Central | GMA 25.5.0, ironSource SDK 9.6.1 | Không (xem dưới) | https://developers.google.com/admob/android/mediation/ironsource |
| Mintegral | `com.google.ads.mediation:mintegral:17.1.81.1` | `com.mbridge.msdk.oversea:mbridge_android_sdk:17.1.81`, kho riêng của Mintegral | GMA 25.4.0, Mintegral SDK 17.1.81 | `https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea` | https://developers.google.com/admob/android/mediation/mintegral |
| Moloco | `com.google.ads.mediation:moloco:4.13.0.0` | `com.moloco.sdk:moloco-sdk:4.13.0`, Maven Central | GMA 25.5.0, Moloco SDK 4.13.0 | Không | https://developers.google.com/admob/android/mediation/moloco |
| Pangle | `com.google.ads.mediation:pangle:8.3.0.4.0` | `com.pangle.global:pag-sdk:8.3.0.4`, kho riêng của Pangle | GMA 25.5.0, Pangle SDK 8.3.0.4 | `https://artifact.bytedance.com/repository/pangle/` | https://developers.google.com/admob/android/mediation/pangle |
| Unity Ads | `com.google.ads.mediation:unity:4.21.0.0` | `com.unity3d.ads:unity-ads:4.21.0`, Maven Central, **khai báo riêng** (POM của adapter không kéo SDK) | GMA 25.5.0, Unity Ads 4.21.0 | Không | https://developers.google.com/admob/android/mediation/unity |

- Tương thích: dự án dùng `play-services-ads` 25.5.0. Ba adapter build với 25.4.0 (Meta, Mintegral, AppLovin của T-015), năm với 25.5.0; cùng bản chính 25, Gradle chọn 25.5.0 (đã đo, mục 4.1).
- Bản mới hơn có trên Google Maven nhưng chưa ghi là phát hành trong Changelog: InMobi `11.5.0.1` (trang ghi "in progress"). Plan chọn `11.5.0.0`, bản trang ghi đã phát hành.
- minSdk: mọi trang ghi API 24 trở lên (vài trang còn ghi 23 ở chỗ khác); dự án minSdk 26, đạt.
- Kho ironSource: trang hướng dẫn bảo thêm `https://android-sdk.is.com/`. Chạy thử: `mediation-sdk:9.6.1` và phụ thuộc của nó tải được từ Maven Central, không cần kho này. Plan không thêm (giảm nguồn tải không cần); phiên gặp lỗi tải từ kho đó thì dừng (mục 7, điều kiện 1).
- Kho Pangle và Mintegral: bắt buộc. `artifact.bytedance.com` là nơi duy nhất có `pag-sdk:8.3.0.4`, `dl-maven-android.mintegral.com` là nơi duy nhất có `mbridge_android_sdk:17.1.81` (đã kiểm từng kho bằng HTTP, chỉ kho đó trả 200).
- Không cần code khởi tạo: cả tám trang ghi `MobileAds.initialize` khởi tạo adapter. Đồng ý GDPR từ UMP được chuyển tự động (TCF hay Additional Consent) cho cả tám. Trang của mỗi mạng có thêm bước tùy chọn cho luật các bang Mỹ (`setDoNotSell` và tương tự) và cho trẻ em; plan không làm, như T-015 câu hỏi 4.
- R8: InMobi có `proguard.txt` trong AAR; trang ironSource và Pangle bảo theo tài liệu của mạng khi làm rối mã. M-07 bảo kiểm bằng build release của staging (mục 4).
- Việc của người trên AdMob (mỗi trang ghi): thêm từng mạng vào danh sách đối tác quảng cáo của thông điệp GDPR và các bang Mỹ; ánh xạ mã của từng mạng; bật chế độ thử của mạng khi thử và tắt trước khi phát hành.

### Khóa SDK AppLovin và trạng thái adapter trên staging

Đọc bytecode bằng `javap` ngày 2026-10-24 (không lưu vào repo):

- `AppLovinMediationAdapter.initialize` (adapter `13.6.4.2`) đọc khóa bằng `MediationConfiguration.getServerParameters().getString("sdkKey")`, rồi gọi `AppLovinInitializer.initialize(context, sdkKey, ...)`, hàm này dựng `AppLovinSdkInitializationConfiguration.builder(sdkKey)`. Không có khóa thì báo lỗi `110`, "Missing or invalid SDK Key.", đúng dòng log của FB-20261023-01. Không có chuỗi `applovin.sdk.key` nào trong adapter; Changelog bản 12.1.0.0: "Adapter no longer checks for the AppLovin SDK key from the AndroidManifest.xml file". Kết luận: đưa khóa vào app lúc build (meta-data, resource, `BuildConfig`) không làm adapter `READY`; chỉ cấu hình ánh xạ trên AdMob làm được.
- Bảy adapter khác cũng đọc mã từ cấu hình AdMob gửi xuống, theo chuỗi trong hàm `initialize`: DT Exchange `applicationId`, InMobi `accountid`, ironSource `appKey`, Mintegral `app_id` và `app_key`, Moloco `app_key`, Pangle `appid`, Unity `gameId`; Meta đọc `placement_id` (chưa đọc hết thông báo lỗi của Meta). Với mã app thử của Google, AdMob không gửi cấu hình nào, nên trên staging cả chín adapter sẽ `NOT_READY` (câu hỏi 1).

### Manifest gộp: tám SDK kéo vào gì

Đo ngày 2026-10-24, `./gradlew :app:processStagingDebugMainManifest` trên `main` `adf9450` trước và sau khi thêm thư viện (mục 4.1), so bằng một script tạm:

- **`uses-permission`: không thêm, không bớt.** Quyền các SDK khai báo trong AAR: `INTERNET`, `ACCESS_NETWORK_STATE` (cả tám), `com.google.android.gms.permission.AD_ID` (Meta, InMobi, Mintegral, Unity), `android.permission.ACCESS_ADSERVICES_ATTRIBUTION` (Unity, ironSource), `android.permission.ACCESS_ADSERVICES_TOPICS` (Unity). Tất cả đã có trong manifest gộp từ Google Mobile Ads. Không SDK nào khai báo `ACCESS_WIFI_STATE`, `CHECK_LICENSE` hay quyền vị trí trong AAR. Trang InMobi gợi ý thêm tùy chọn `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`: không thêm (không cần cho quảng cáo, spec câu hỏi 18).
- **Provider chạy lúc tiến trình khởi động** (đọc `onCreate` bằng `javap`):
  - `com.facebook.ads.AudienceNetworkContentProvider` (Meta): gọi `DynamicLoaderFactory.initialize(...)`, tức **khởi động SDK Meta** lúc mở app, trước khi có đồng ý. **Bỏ**, cùng lý do với `AppLovinInitProvider` của T-015.
  - `com.ironsource.lifecycle.IronsourceLifecycleProvider`, `com.ironsource.lifecycle.LevelPlayActivityLifecycleProvider` (ironSource): đăng ký theo dõi vòng đời activity (`registerActivityLifecycleCallbacks`). Giữ: SDK cần biết activity đang mở để hiện quảng cáo; không thấy đọc mã định danh.
  - `com.mbridge.msdk.config.component.status.MBComponentLifecycleProvider` (Mintegral): tạo một đối tượng theo dõi trạng thái thành phần, lời gọi nằm trong khối bắt lỗi. Giữ; chưa đọc sâu lớp bên trong (mã bị làm rối), kiểm thủ công 2.
  - `com.squareup.picasso.PicassoProvider` (Picasso, phụ thuộc của InMobi): chỉ giữ `Context`. Giữ.
- **Bộ khởi tạo App Startup** `com.unity3d.services.core.configuration.AdsSdkInitializer` (Unity, meta-data trong `androidx.startup.InitializationProvider`): giữ `Context`, ghi giờ khởi động app, bắt đầu theo dõi activity và hướng màn hình, gọi `CronetProviderInstaller.installProvider`. Không khởi động Unity Ads, không thấy đọc mã định danh. Giữ (bỏ thì Unity mất theo dõi activity, cần cho hiện quảng cáo); kiểm thủ công 2.
- **Receiver** `com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver` (Mintegral), `exported="true"`, lọc `android.net.conn.CONNECTIVITY_CHANGE`. Từ API 24, broadcast này không gửi tới receiver khai báo trong manifest, nên nó không bao giờ chạy do hệ thống; vẫn là một thành phần mà app khác gọi được. **Bỏ.**
- 39 activity (Pangle 18, Moloco 4, Fyber 4, ironSource 4 cùng `InlineStoreActivity` của Unity, Mintegral 4, InMobi 2, Meta 1, Unity 1), 2 service không xuất ra ngoài, meta-data `com.bytedance.sdk.pangle.version`. Giữ.
- `<queries>`: thêm 19 mục: Custom Tabs, `VIEW`, `MAIN`, `market://`, kho ứng dụng (`com.android.vending`, `com.huawei.appmarket`, `com.xiaomi.market`, `com.oppo.market`, `com.bbk.appstore`, `com.sec.android.app.samsungapps`, `com.amazon.venezia`, `com.google.market`), `com.facebook.katana`, dịch vụ của Digital Turbine, và **năm gói của công cụ root máy** (`com.topjohnwu.magisk`, `eu.chainfire.supersu`, `com.koushikdutta.superuser`, `com.noshufou.android.su`, `com.thirdparty.superuser`). Không phải `QUERY_ALL_PACKAGES`, không phải quyền. Giữ, ghi vào `ads-guide.md` cho Data safety (câu hỏi 3).

### Trang công bố dữ liệu của từng mạng (cho Data safety)

Tra ngày 2026-10-24. Ghi vào `ads-guide.md` mục 9 để người khai Data safety. Trang nào chưa tìm thấy thì ghi rõ.

| Mạng | Trang | Ghi chú |
| --- | --- | --- |
| Google Mobile Ads | https://developers.google.com/admob/android/privacy/play-data-disclosure | IP, tương tác, chẩn đoán, mã thiết bị |
| AppLovin | Theo T-015, `ads-guide.md` mục 7 | |
| Unity Ads | https://docs.unity.com/grow/ads/privacy/google-data-safety | Mã thiết bị, chẩn đoán, thời gian dùng app (khi bật Acquire Optimization) |
| ironSource | https://docs.unity.com/grow/levelplay/platform/legal-resources (mục Google Data Safety Questionnaire) | Có báo cáo Google đòi khai "ứng dụng đã cài" |
| Pangle | https://pangleglobal.com/integration/app-privacy-details-gp | Vị trí gần đúng suy từ IP, hoạt động trong app |
| DT Exchange | https://docs.digitalturbine.com/dt-exchange/publishers/getting-started-with-dt-exchange/privacy/google-data-safety-form | Vị trí gần đúng, mã quảng cáo |
| Moloco | Chưa thấy trang Data safety; chính sách riêng tư: https://www.moloco.com/privacy-policy | Mã quảng cáo, thông tin máy, vị trí gần đúng |
| Meta Audience Network | Chưa thấy trang Data safety của Meta | Người khai theo chính sách của Meta |
| InMobi | Chưa thấy trang Data safety của InMobi | |
| Mintegral | Chưa thấy trang Data safety của Mintegral | |

## 3. Giải pháp

### 3.1. Cách làm

**Tiếp tục (phiên bản 2):** trên nhánh cũ `claude/T-034-mediation-chin-mang` (chưa có PR). Nhánh đã có commit test `c2b821a` (bước 3 xong, 9 test đã thất bại đúng lý do) và commit `1a105a9` khai báo thư viện ở bước 1 đến 3 dưới đây (chưa tải được). Phiên cập nhật nhánh theo nhánh chính, chạy lại để thấy hai test `hasNoMetaAudienceNetworkProvider`, `hasNoMintegralNetworkReceiver` thất bại khi đã có thư viện, rồi làm tiếp từ bước 4. Trước khi chạy: người kiểm `curl -sS -o /dev/null -w '%{http_code}' https://artifact.bytedance.com/repository/pangle/` và kho Mintegral trong môi trường cloud không còn trả lỗi kết nối 403 của proxy; còn 403 thì không đưa plan vào danh sách chạy.

Làm theo mẫu T-015 (thư viện trong catalog, chỉ `:core:ads` phụ thuộc, dòng bỏ trong manifest của `:core:ads`, test classpath và test manifest gộp).

1. `gradle/libs.versions.toml`, nhóm `# Ads`, ngay dưới `admobMediationApplovin`: thêm chín phiên bản `admobMediationMeta = "6.22.0.1"`, `admobMediationDtExchange = "8.5.0.0"`, `admobMediationInmobi = "11.5.0.0"`, `admobMediationIronsource = "9.6.1.0"`, `admobMediationMintegral = "17.1.81.1"`, `admobMediationMoloco = "4.13.0.0"`, `admobMediationPangle = "8.3.0.4.0"`, `admobMediationUnity = "4.21.0.0"`, `unityAds = "4.21.0"`. Ngay dưới thư viện `admob-mediation-applovin`: `admob-mediation-meta` (`com.google.ads.mediation:facebook`), `admob-mediation-dtexchange` (`com.google.ads.mediation:fyber`), `admob-mediation-inmobi`, `admob-mediation-ironsource`, `admob-mediation-mintegral`, `admob-mediation-moloco`, `admob-mediation-pangle`, `admob-mediation-unity` (`com.google.ads.mediation:<tên>`), `unity-ads` (`com.unity3d.ads:unity-ads`), mỗi dòng `version.ref` tới phiên bản tương ứng. Không đổi phiên bản nào khác.
2. `core/ads/build.gradle.kts`: chín dòng `implementation(libs.admob.mediation.meta)`, `...dtexchange`, `...inmobi`, `...ironsource`, `...mintegral`, `...moloco`, `...pangle`, `...unity`, `implementation(libs.unity.ads)`, ngay dưới dòng AppLovin. Chỉ `:core:ads` thấy các thư viện này (ADR-0013).
3. `settings.gradle.kts`, khối `dependencyResolutionManagement.repositories`, sau `mavenCentral()`:

   ```kotlin
   // Mediation SDKs hosted outside Google Maven and Maven Central (plan T-034). Each repository serves only its group.
   maven("https://artifact.bytedance.com/repository/pangle/") {
       content { includeGroup("com.pangle.global") }
   }
   maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") {
       content { includeGroupByRegex("com\\.mbridge.*") }
   }
   ```

   Không đổi `pluginManagement`. `content` giữ cho mọi thư viện khác chỉ tải từ `google()` và `mavenCentral()`, để một kho ngoài không thể trả thay một thư viện cùng tên.
4. `core/ads/src/main/AndroidManifest.xml`: giữ ba dòng bỏ của T-015. Trong `<application>`, thêm:
   - `<provider android:name="com.facebook.ads.AudienceNetworkContentProvider" android:authorities="${applicationId}.AudienceNetworkContentProvider" tools:node="remove" />`, chú thích: provider khởi động SDK Meta lúc mở app, trước khi có đồng ý; bỏ để Meta chỉ khởi động qua `MobileAds.initialize` (ADR-0004, ADR-0026 mục 3).
   - `<receiver android:name="com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver" tools:node="remove" />`, chú thích: receiver xuất ra ngoài, lọc `CONNECTIVITY_CHANGE` mà hệ thống không gửi tới receiver trong manifest từ API 24.
   - Sửa chú thích đầu file: danh sách phần tử bị bỏ theo plan T-015 và T-034; nâng SDK mediation thì kiểm lại tên trong AAR và chạy `MergedManifestTest`.
   - Không thêm quyền, không thêm thành phần. Đã chạy thử: lint không báo `MissingClass` vì các lớp có trên classpath của `:core:ads` (mục 4.1).
4a. Tạo `app/lint.xml` (chạy thử ở mục 4.1 cho thấy lint báo lỗi `NotificationPermission` từ Picasso, phụ thuộc của InMobi):

   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <lint>
       <!--
           Picasso, required by the InMobi SDK (plan T-034), has code that posts notifications
           (RemoteViewsAction.NotificationAction). Neither InMobi nor the app calls it, and the app does not ask for
           POST_NOTIFICATIONS. Only findings that name Picasso are ignored; the check still covers every other class.
       -->
       <issue id="NotificationPermission">
           <ignore regexp="com\.squareup\.picasso\." />
       </issue>
   </lint>
   ```

   Không thêm `POST_NOTIFICATIONS`, không tắt quy tắc cho cả app, không thêm baseline, không sửa `build-logic/`. Đây là bỏ qua một phát hiện của lint, nên chỉ làm khi người duyệt đồng ý (câu hỏi 7); không đồng ý thì plan không chạy được (mục 7, điều kiện 3).
5. Xem lại việc bỏ `AppLovinInitProvider` (ads-guide.md mục 7 yêu cầu): **giữ**. ADR-0026 cho khởi tạo SDK sớm, nhưng chỉ khi UMP còn giữ đồng ý dùng được của phiên trước (`CACHED_EARLY`, S-03) hay khi UMP lỗi mạng ở foreground (`ERROR_ALLOWED`), và nhánh rút gọn mặc định tắt (S-12). Provider thì chạy ở mọi lần mở tiến trình, kể cả lần đầu sau khi cài, khi chưa có đồng ý nào, và đọc Advertising ID cùng App Set ID (T-015, mục 2). Nên lý do bỏ vẫn đúng dưới ADR-0026; cùng lý do áp cho provider của Meta. Ghi vào `ads-guide.md`.
6. Không thêm code khởi tạo, không gọi API của mạng nào. `MobileAds.initialize` (vị trí gọi theo T-020 khi T-020 xong) khởi tạo cả chín adapter; `adapterStatusLines` của T-015 ghi một dòng mỗi adapter vào thẻ `Ads`. Không đổi `AdsController.kt`.
7. `scripts/secret-scan.py`: thêm mẫu "khóa SDK AppLovin": một chuỗi đúng 86 ký tự `[A-Za-z0-9_-]` trên dòng có chữ `applovin` (không phân biệt hoa thường), ví dụ `applovin.sdk.key`, `applovin_sdk_key`. Chuỗi có `FAKE` thì cho qua, như khóa Google giả. Cập nhật docstring (danh sách những gì bị chặn). Đây là cách kiểm tự động cho KB-9 tiêu chí 3 và luật AR-04 (`ads-business-rules.md` ghi `secret-scan.py` chặn, nhưng script hiện chưa có mẫu này).
8. `scripts/tests/run.sh`, cạnh các test `secret-scan` hiện có: hai test mới (mục 4). Khóa giả dựng từ nhiều mảnh bằng `printf` như test mã AdMob, để chính file test không chứa chuỗi giống khóa.
9. Tài liệu:
   - `docs/ops/ads-guide.md` mục 7: bỏ nhãn "(đợt 3)"; bảng tám adapter và SDK Unity Ads (tọa độ, khóa catalog), hai kho Maven mới và vì sao có `content`; kết quả manifest (không quyền mới; provider và receiver bị bỏ, lý do; provider và bộ khởi tạo được giữ, lý do; `<queries>`); khóa SDK AppLovin theo câu trả lời câu hỏi 2 (thay câu "T-034 chốt cách đưa khóa vào lúc build" và gạch đầu dòng "Với ADR-0026"); trên staging mọi adapter `NOT_READY` vì thiếu mã của mạng, cách kiểm `READY` trên app có mã thật bằng Ad Inspector; Unity và ironSource cần `Activity` khi nạp (câu hỏi 4); nâng adapter là plan rủi ro cao, kiểm lại tên provider, receiver trong AAR.
   - `docs/ops/ads-guide.md` mục 9: dòng Data safety thay câu "Tra trang công bố dữ liệu... lúc làm T-034" bằng bảng trang của từng mạng (mục 2); dòng khóa SDK AppLovin theo câu hỏi 2.
   - `docs/ops/release.md` mục 1.4: dòng `app-ads.txt` của tám mạng mới (lấy từ bảng điều khiển của từng mạng), cạnh AdMob và AppLovin. Mục 1.2: không thêm giá trị nào (theo phương án A của câu hỏi 2).
   - `docs/ops/ads-business-rules.md` M-03 và AR-04: **chỉ khi** người duyệt chọn phương án A ở câu hỏi 2, sửa chữ "đưa vào lúc build như mã quảng cáo" thành "nhập trong ánh xạ nguồn quảng cáo trên AdMob, app không mang khóa", kèm dẫn chứng bytecode. Chọn B thì không sửa file này và plan cần phiên bản 2.
   - Không sửa spec (spec `confirmed`; đề xuất sửa mục 6 ở câu hỏi 5) và không sửa ADR.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. Chín phiên bản, chín thư viện (mục 3.1, bước 1) |
| Sửa | `core/ads/build.gradle.kts` | Chín dòng `implementation` (bước 2) |
| Sửa | `settings.gradle.kts` | Hai kho Maven có `content` (bước 3) |
| Sửa | `core/ads/src/main/AndroidManifest.xml` | Bỏ provider của Meta và receiver của Mintegral, sửa chú thích (bước 4) |
| Tạo | `app/lint.xml` | Bỏ qua `NotificationPermission` chỉ cho thông báo có `com.squareup.picasso.` (bước 4a), chỉ khi câu hỏi 7 được đồng ý |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MediationAdapterClasspathTest.kt` | Thêm test cho tám adapter và SDK Unity Ads (mục 4) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/MergedManifestTest.kt` | Thêm test danh sách quyền, provider Meta, receiver Mintegral, activity của tám mạng, không có khóa AppLovin; sửa chú thích lớp (mục 4) |
| Sửa | `scripts/secret-scan.py` | Mẫu khóa SDK AppLovin, docstring (bước 7) |
| Sửa | `scripts/tests/run.sh` | Hai test `secret-scan` cho khóa AppLovin (bước 8) |
| Sửa | `docs/ops/ads-guide.md` | Mục 7, mục 9 (bước 9) |
| Sửa | `docs/ops/release.md` | Mục 1.4 (bước 9) |
| Sửa | `docs/ops/ads-business-rules.md` | M-03, AR-04, chỉ khi câu hỏi 2 chọn A (bước 9) |
| Sửa | `docs/features/ads/plans/T-034-mediation-chin-mang.md` | Nhật ký thực hiện (mục 10) |
| Sửa | `docs/features/ads/tickets/T-034-mediation-chin-mang.md` | Trạng thái ticket |

Nơi đọc các file bị đổi (L-020), tìm bằng `grep -rn <tên file> scripts/ .github/ */src/test */*/src/test` và `scripts/plan-check.py`:

- `gradle/libs.versions.toml`: `build-logic/settings.gradle.kts` nạp file này làm catalog cho convention plugin, plugin chỉ đọc khóa đã có, không đọc khóa mới; `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` chỉ nhắc tên file trong chú thích về lint (khóa `disable`), không đọc nội dung; `scripts/tests/run.sh` chỉ ghi và đọc một bản giả trong repo tạm của test (`$repo/gradle/libs.versions.toml`), không đọc bản thật; `scripts/plan-check.py` chỉ có tên file trong danh sách đường dẫn rủi ro cao. Không bị ảnh hưởng.
- `settings.gradle.kts`: `scripts/new-app.sh` thay dòng `rootProject.name = "app-project-template"` khi tạo app mới; plan không đổi dòng đó, hai kho mới nằm trong khối `repositories`. Test của `new-app.sh` trong `scripts/tests/run.sh` dựng repo tạm riêng, không đọc file thật. Không bị ảnh hưởng. `.github/workflows/` không nhắc file này.
- `core/ads/src/main/AndroidManifest.xml`: `grep -rn AndroidManifest scripts/ .github/` chỉ thấy `scripts/app-explore/apk-screens.py` (đọc manifest của APK app mẫu khi khảo sát) và `scripts/tests/run.sh` dòng 645 (xóa `app/src/main/AndroidManifest.xml` trong repo tạm của test `new-app.sh`). Không bị ảnh hưởng. `MergedManifestTest` đọc manifest gộp, đã ở trong danh sách.
- `scripts/secret-scan.py`: chỉ `scripts/tests/run.sh` gọi (dòng 933 đến 946, đã trong danh sách); `scripts/check.sh` chạy `scripts/tests/run.sh` nên quét cả repo, kể cả plan này. Plan không chứa chuỗi 86 ký tự nào.
- `scripts/tests/run.sh`: `scripts/check.sh` chạy nó, không đọc nội dung. Không bị ảnh hưởng.
- `app/lint.xml` (file mới): `grep -rn "lint.xml" scripts/ .github/ build-logic/` không thấy nơi nào đọc; lint của AGP tự đọc file ở thư mục module. `build-logic` không đặt `lintConfig`, nên không có cấu hình nào bị thay.
- `docs/ops/release.md`: `scripts/release.sh` chỉ nhắc tên file trong thông báo lỗi; plan không chạm, không chạy script đó.
- `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`: không script, workflow hay test nào đọc.
- `docs/STATUS.md`: không đưa vào PR (L-059).
- Ảnh chuẩn screenshot không đổi: test và preview không có `AdsController`, không có quảng cáo trong ảnh.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng, tần suất. Thêm tám mạng con mediation theo ADR-0026 mục 8 và M-01. Mã quảng cáo không đổi; staging vẫn dùng mã thử của Google.
- Sự kiện phân tích: Không chạm. Tám SDK tự gửi dữ liệu về mạng của nó khi phục vụ quảng cáo, sau khi được khởi tạo qua `MobileAds.initialize`; provider của Meta (khởi động SDK lúc mở app) bị bỏ. Data safety của từng app phải khai tám mạng (mục 2, bảng trang công bố).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Đã đo manifest gộp `stagingDebug` trước và sau (mục 4.1): danh sách `uses-permission` không đổi. Không thêm: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` mà trang InMobi gợi ý là tùy chọn (spec câu hỏi 18: không cần cho quảng cáo); `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`, `android.permission.ACCESS_ADSERVICES_TOPICS` mà các SDK khai báo đã có từ Google Mobile Ads; `VIBRATE` đã có từ T-015; `android.permission.ACCESS_ADSERVICES_AD_ID`, `FOREGROUND_SERVICE`, `WAKE_LOCK` đã có trong manifest gộp từ Google Mobile Ads, Firebase và WorkManager, không đổi; `com.android.vending.CHECK_LICENSE` vẫn bỏ như T-015; `POST_NOTIFICATIONS` mà lint của Picasso nhắc tới (câu hỏi 7) và `QUERY_ALL_PACKAGES` không thêm.
- **Thư viện và SDK:** thêm, chỉ ở `:core:ads` (khóa catalog trong `gradle/libs.versions.toml`, mục 3.1 bước 1):
  - `com.google.ads.mediation:facebook:6.22.0.1` (Apache-2.0), kéo `com.facebook.android:audience-network-sdk:6.22.0` (giấy phép riêng của Meta, Audience Network Terms).
  - `com.google.ads.mediation:fyber:8.5.0.0` (Apache-2.0), kéo `com.fyber:marketplace-sdk:8.5.0` (giấy phép của Digital Turbine).
  - `com.google.ads.mediation:inmobi:11.5.0.0` (Apache-2.0), kéo `com.inmobi.monetization:inmobi-ads-kotlin:11.5.0` (giấy phép của InMobi) và `com.squareup.picasso:picasso:2.8` (Apache-2.0).
  - `com.google.ads.mediation:ironsource:9.6.1.0` (Apache-2.0), kéo `com.unity3d.ads-mediation:mediation-sdk:9.6.1` (giấy phép của Unity, ironSource).
  - `com.google.ads.mediation:mintegral:17.1.81.1` (Apache-2.0), kéo `com.mbridge.msdk.oversea:mbridge_android_sdk:17.1.81` (giấy phép của Mintegral), từ kho Mintegral.
  - `com.google.ads.mediation:moloco:4.13.0.0` (Apache-2.0), kéo `com.moloco.sdk:moloco-sdk:4.13.0` (giấy phép của Moloco) cùng Ktor 3.2.3, protobuf của Moloco.
  - `com.google.ads.mediation:pangle:8.3.0.4.0` (Apache-2.0), kéo `com.pangle.global:pag-sdk:8.3.0.4` (giấy phép của Pangle) cùng `pag-apm`, `pag-gecko` và `tiktok-business-android-sdk-comp:1.6.1` (SDK đo lường của TikTok), từ kho Pangle.
  - `com.google.ads.mediation:unity:4.21.0.0` (Apache-2.0) và `com.unity3d.ads:unity-ads:4.21.0` (giấy phép của Unity), khai báo riêng.
  - Giấy phép adapter lấy từ POM; giấy phép SDK của mạng là điều khoản riêng của từng hãng, người duyệt xác nhận khi ký hợp đồng với mạng.
  - Quyền mà các thư viện kéo vào manifest gộp: không có quyền mới (dòng trên). Thành phần kéo vào và phần bị bỏ: mục 2.
  - Thư viện đã có giữ nguyên phiên bản: `play-services-ads` 25.5.0, `user-messaging-platform` 4.0.0, Kotlin 2.2.10 (đo ở mục 4.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** kho Maven của Pangle hay Mintegral không tải được trong môi trường của phiên (proxy của `claude --cloud` chặn máy chủ ngoài, như R2 ở L-050), dù trên máy dev đã tải được.
- **Điều kiện chuyển:** lần build đầu báo `Could not resolve com.pangle.global:pag-sdk:8.3.0.4` hay `com.mbridge.msdk.oversea:...` với lỗi kết nối hay 403 tới đúng kho đó, trong khi `google()` và `mavenCentral()` tải được.
- **Cách làm thay:** không có cách làm thay giữ nguyên tiêu chí trong phiên (bỏ một mạng là hạ tiêu chí KB-9). Dừng task theo mục 7 điều kiện 1, ghi lỗi tải; người kiểm môi trường hay chạy task trên máy dev.
- Ghi chú: `MergedManifestTest` đọc manifest gộp theo đường dẫn trung gian của AGP; phương án dự phòng của T-015 (mục 3.4: tìm file `AndroidManifest.xml` duy nhất dưới `build/intermediates/merged_manifest/stagingDebug/`) vẫn áp dụng, cùng file `MergedManifestTest.kt`.

## 4. Kiểm thử

Test JVM, chạy trong `scripts/check.sh`. Tên lớp viết thẳng trong test (không qua hằng số của code).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-9, tiêu chí 1: adapter của tám mạng có trong bản build | Unit test `:core:ads` | `MediationAdapterClasspathTest.networkAdapters_areOnClasspath`: với từng tên lớp `com.google.ads.mediation.facebook.FacebookMediationAdapter`, `com.google.ads.mediation.fyber.FyberMediationAdapter`, `com.google.ads.mediation.inmobi.InMobiMediationAdapter`, `com.google.ads.mediation.ironsource.IronSourceMediationAdapter`, `com.google.ads.mediation.mintegral.MintegralMediationAdapter`, `com.google.ads.mediation.moloco.MolocoMediationAdapter`, `com.google.ads.mediation.pangle.PangleMediationAdapter`, `com.google.ads.mediation.unity.UnityMediationAdapter`: `Class.forName(name, false, javaClass.classLoader)` không ném lỗi và lớp gán được cho `com.google.android.gms.ads.mediation.Adapter`; thông báo lỗi ghi tên lớp. Trước khi thêm thư viện: thất bại `ClassNotFoundException: com.google.ads.mediation.facebook.FacebookMediationAdapter` |
| Như trên, SDK Unity Ads khai báo riêng | Unit test `:core:ads` | `MediationAdapterClasspathTest.unityAdsSdk_isOnClasspath`: `Class.forName("com.unity3d.ads.UnityAds", false, ...)` không ném lỗi. Adapter Unity không kéo SDK theo (POM), nên test này bắt lỗi quên dòng `libs.unity.ads`. Trước khi thêm: `ClassNotFoundException` |
| KB-9, tiêu chí 1: SDK của tám mạng vào manifest gộp | Unit test `:app` | `MergedManifestTest.mergedManifest_containsActivityOfEveryNetwork`: với mỗi tiền tố `com.facebook.ads.`, `com.fyber.inneractive.`, `com.inmobi.ads.`, `com.ironsource.`, `com.mbridge.msdk.`, `com.moloco.sdk.`, `com.bytedance.sdk.openadsdk.`, `com.unity3d.ads.`, có ít nhất một `activity` tên bắt đầu bằng tiền tố đó; thông báo lỗi ghi tiền tố thiếu. Tiền tố của Unity đã kiểm ngày 2026-10-24 (rà soát trước phiên) trên manifest trong AAR `com.unity3d.ads:unity-ads:4.21.0` của bộ nhớ đệm Gradle: activity duy nhất là `com.unity3d.ads.adplayer.FullScreenWebViewDisplay`, khớp "Unity 1" ở mục 2; `InlineStoreActivity` thuộc SDK ironSource có tên `com.unity3d.ironsourceads.internal.services.InlineStoreActivity`, không khớp `com.unity3d.ads.`, nên test vẫn bắt được khi quên `libs.unity.ads`. Test này cũng chặn ba test "không có" bên dưới qua chỉ vì SDK chưa vào bản build. Trước khi thêm thư viện: thất bại ở `com.facebook.ads.` |
| Spec câu hỏi 18: không quyền mới | Unit test `:app` | `MergedManifestTest.mergedManifest_permissionsAreExactlyTheAllowedSet`: danh sách `uses-permission` đã sắp xếp bằng đúng danh sách đo ở mục 4.1 (11 quyền: `android.permission.ACCESS_ADSERVICES_AD_ID`, `android.permission.ACCESS_ADSERVICES_ATTRIBUTION`, `android.permission.ACCESS_ADSERVICES_TOPICS`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `INTERNET`, `VIBRATE`, `WAKE_LOCK`, `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`, `AD_ID`, `com.vglstudio.template.staging.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), viết đủ tên trong test. Quyền mới sau này (nâng SDK) làm test đỏ, buộc plan quyết. Test này qua ngay trước khi thêm thư viện (danh sách không đổi): bước đối chứng (L-055) là chạy test với một tên quyền bị bỏ khỏi danh sách mong đợi, thấy test thất bại, rồi đặt lại; AI ghi cả hai lần vào nhật ký |
| ADR-0004, ADR-0026 mục 3: SDK Meta không khởi động trước đồng ý | Unit test `:app` | `MergedManifestTest.mergedManifest_hasNoMetaAudienceNetworkProvider`: không `provider` nào tên `com.facebook.ads.AudienceNetworkContentProvider`. Sau khi thêm thư viện, trước khi sửa manifest: thất bại, thông báo "AudienceNetworkContentProvider starts the Meta SDK before consent" |
| Receiver xuất ra ngoài không cần | Unit test `:app` | `MergedManifestTest.mergedManifest_hasNoMintegralNetworkReceiver`: không `receiver` nào tên `com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver`. Sau khi thêm thư viện, trước khi sửa manifest: thất bại |
| KB-9, tiêu chí 3: staging không mang khóa AppLovin | Unit test `:app` | `MergedManifestTest.mergedManifest_hasNoAppLovinSdkKey`: không `meta-data` nào tên `applovin.sdk.key` ở bất kỳ đâu trong manifest gộp. Qua ngay (chưa ai thêm); bước đối chứng: tạm thêm một `meta-data` tên đó, giá trị `FAKE`, vào `core/ads/src/main/AndroidManifest.xml`, chạy lại `:app:processStagingDebugMainManifest` và test, thấy thất bại, rồi gỡ; không commit phần tạm |
| Test của T-015 vẫn đúng | Unit test `:app`, `:core:ads` | Năm test cũ của `MergedManifestTest` và `appLovinAdapter_isOnClasspath` không sửa, vẫn qua |
| KB-9, tiêu chí 3: khóa AppLovin không vào repo | Test script trong `scripts/tests/run.sh` | `secret-scan: an AppLovin SDK key fails`: thư mục quét tạm có file với dòng `applovin.sdk.key=` cộng một chuỗi 86 ký tự dựng bằng `printf` từ các mảnh: `secret-scan.py` trả 1, và dòng báo đã che (không in đủ chuỗi). `secret-scan: a fake AppLovin key passes`: cùng dòng nhưng chuỗi 86 ký tự có `FAKE`: trả 0. Test "this repository has no real ad IDs or keys" có sẵn kiểm cả repo. Trước khi sửa script: test đầu thất bại (trả 0) |
| Phiên bản và kho đúng | Lệnh, AI ghi vào nhật ký | `./gradlew :app:dependencies --configuration stagingReleaseRuntimeClasspath`: có chín tọa độ ở mục 3.3 đúng phiên bản; `play-services-ads` vẫn 25.5.0, `user-messaging-platform` vẫn 4.0.0, `kotlin-stdlib` vẫn 2.2.10. Lệch thì dừng (mục 7) |
| Manifest gộp chỉ đổi như mục 2 | Lệnh, AI ghi vào nhật ký | `./gradlew :app:processStagingDebugMainManifest` trước và sau; so `uses-permission`, `provider`, `receiver`, meta-data của `androidx.startup.InitializationProvider`: chỉ khác như mục 2 (thêm bốn provider giữ lại, `AdsSdkInitializer`, activity, service; không thêm quyền). Báo cáo `app/build/outputs/logs/manifest-merger-staging-debug-report.txt` có `REJECTED` cho đúng hai phần tử mới (Meta, Mintegral) cùng ba phần tử của T-015 |
| M-07: R8 của bản release không lỗi vì adapter | Lệnh, AI ghi vào nhật ký | `./gradlew :app:minifyStagingReleaseWithR8`: qua, không có lỗi "Missing class" từ gói của tám mạng. Lỗi thì dừng (mục 7, điều kiện 4) |
| Build, lint, ktlint, quy tắc phụ thuộc, test và ảnh chuẩn cũ | `scripts/check.sh` | Qua toàn bộ (đã chạy thử, mục 4.1). Trước khi tạo `app/lint.xml`, `:app:lintStagingDebug` thất bại với đúng một lỗi `NotificationPermission` từ `com.squareup.picasso`; AI ghi lần thất bại đó vào nhật ký |
| Kiểm thủ công 1, KB-9 tiêu chí 2 vế 1 (người, bản staging của `staging.yml`) | Kiểm tra thủ công | Cài bản staging, đồng ý, `adb logcat -v threadtime \| grep ' VGL\.Ads'`: có 10 dòng `Adapter ...` (chín adapter cùng `com.google.android.gms.ads.MobileAds`) trong cùng một lần `MobileAds.initialize` báo xong. Chín adapter dự kiến `NOT_READY` với thông báo thiếu mã của mạng (câu hỏi 1); dòng nào có lỗi khác (lớp thiếu, `SecurityException`, ngoại lệ) thì tạo phản hồi cho T-034. Khi mở được Ad Inspector (T-014): mục adapter liệt kê đủ chín mạng |
| Kiểm thủ công 2, ADR-0004 (người, cùng phiên thử) | Kiểm tra thủ công | Xóa dữ liệu app, mở app, chưa trả lời hộp thoại đồng ý: logcat không có dòng của các thẻ `AudienceNetwork`, `FBAudienceNetwork`, `UnityAds`, `IronSource`, `LevelPlay`, `Mintegral`, `MBridge`, `PAGSdk`, `Moloco`, `InMobi`, `Fyber`, `AppLovinSdk` báo khởi tạo hay gửi yêu cầu. Có thì báo người duyệt (mục 7, điều kiện 8) |
| Kiểm thủ công 3, app chạy lâu (người, cùng phiên thử) | Kiểm tra thủ công | Dùng app ít nhất 30 phút sau khi đồng ý, qua lại các màn có quảng cáo, nền rồi mở lại, xoay máy: không `FATAL EXCEPTION`, không ngoại lệ có `com.facebook.ads`, `com.mbridge`, `com.unity3d`, `com.ironsource`, `com.bytedance`, `com.moloco`, `com.inmobi`, `com.fyber` trong stack trace |
| Kiểm thủ công 4, KB-9 tiêu chí 1 và tiêu chí 2 vế 2 (người, app đầu tiên có mã AdMob thật) | Kiểm tra thủ công | Nhóm mediation có đủ chín mạng, máy thử đã đăng ký: log có 10 dòng `READY`; Ad Inspector, single ad source testing với từng mạng tải được quảng cáo thử. Ghi vào báo cáo phiên thử của app đó |

**Ngoại lệ viết test trước:** `app/lint.xml` là cấu hình lint, kiểm bằng `scripts/check.sh` (dòng trên), không có test riêng. Tài liệu (`ads-guide.md`, `release.md`, `ads-business-rules.md`) không có test. Việc mạng nhận được quảng cáo thật và khởi tạo `READY` chỉ kiểm được trên app có mã thật (kiểm thủ công 4). Việc các SDK không chạy trước đồng ý sau khi bỏ provider chỉ kiểm được trên máy (kiểm thủ công 2). Người duyệt đồng ý ngoại lệ này.

Thứ tự commit (L-025): commit test của bước 3 gồm mọi test ở trên và hai test script; khi đó chưa có thư viện, nên test classpath, `containsActivityOfEveryNetwork` và test khóa AppLovin của script thất bại đúng lý do; `hasNoMetaAudienceNetworkProvider`, `hasNoMintegralNetworkReceiver` qua (chưa có SDK). Commit code của bước 4 thêm thư viện trước, chạy lại để thấy hai test đó thất bại, rồi mới sửa manifest. AI ghi cả ba lần chạy vào nhật ký.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-24, trong worktree của nhánh chính (`main` `adf9450`), không commit, không push; đã hoàn lại mọi file bằng `git checkout` và xóa file tạm sau khi chạy. Code thử: chín phiên bản và chín thư viện trong `gradle/libs.versions.toml` như mục 3.1 bước 1; chín dòng `implementation` trong `core/ads/build.gradle.kts`; hai kho có `content` trong `settings.gradle.kts`; dòng bỏ provider Meta và receiver Mintegral trong `core/ads/src/main/AndroidManifest.xml`; test tạm `MediationAdaptersTrialTest` (`:core:ads`, tám adapter và `com.unity3d.ads.UnityAds` bằng `Class.forName`) và `MergedManifestTrialTest` (`:app`, danh sách 11 quyền, không còn provider Meta và receiver Mintegral). Chưa thử phần `secret-scan.py` (script Python thuần, mẫu đã có của mã AdMob). Máy dùng JDK của Android Studio (`JAVA_HOME` trỏ tới `jbr`) và `ANDROID_HOME` trỏ tới SDK của máy; worktree không có `local.properties`.
- **Lệnh:** `./gradlew :app:processStagingDebugMainManifest` trước và sau khi thêm thư viện, so manifest gộp bằng một script Python tạm; `./gradlew :app:dependencies --configuration stagingReleaseRuntimeClasspath` trước và sau; `javap` trên lớp adapter và provider trong cache Gradle; `scripts/check.sh`; `./gradlew :app:minifyStagingReleaseWithR8`.
- **Kết quả:**
  - Tải thư viện: lần đầu tải được hết từ `google()`, `mavenCentral()` và hai kho mới; không cần kho ironSource.
  - Manifest gộp: như mục 2. `uses-permission` trước và sau giống hệt (11 quyền). Thêm 39 activity, 2 service, 1 receiver, 5 provider, 1 meta-data, 1 bộ khởi tạo App Startup, 19 mục `<queries>`. Với hai dòng bỏ: không còn provider Meta và receiver Mintegral.
  - Phụ thuộc (`:app:dependencies --configuration stagingReleaseRuntimeClasspath` trước và sau, so bằng script tạm): `play-services-ads` vẫn 25.5.0, `user-messaging-platform` vẫn 4.0.0, `kotlin-stdlib` vẫn 2.2.10, `kotlinx-coroutines` vẫn 1.10.2. Thêm 88 thư viện chạy (ngoài chín tọa độ ở mục 3.3: Ktor 3.2.3, Coil 2.2.2, ExoPlayer 2.18.2 và Media3 1.4.1, Picasso 2.8, OkHttp 4.10.0, protobuf-lite 3.25.6, Cronet, `androidx.compose.material` 1.9.1, `com.pangle.global:tiktok-business-android-sdk-comp:1.6.1`, `com.unity3d.ads-mediation:adquality-sdk:9.10.0`...). Thư viện chạy đã có bị nâng: `androidx.browser` 1.8.0 lên 1.9.0, `androidx.work:work-runtime` 2.7.0 lên 2.8.1, `androidx.room` 2.2.5 lên 2.5.2, `androidx.sqlite` 2.1.0 lên 2.3.1, `guava` 31.1 lên 33.0.0, `okio` 3.4.0 lên 3.7.0, `play-services-ads-identifier` 18.0.0 lên 18.0.1, `play-services-appset` 16.0.1 lên 16.0.2, `kotlin-stdlib-jdk7`, `-jdk8` 1.8.22 lên 2.1.21 cùng vài thư viện nhỏ. Không thư viện nào khai báo trong catalog bị đổi phiên bản. Các thư viện này chỉ nằm trên classpath chạy (`implementation` của `:core:ads`), feature không biên dịch được với chúng.
  - Kích thước: APK `stagingDebug` (chưa thu gọn) 23,4 MB trước, 47,4 MB sau (thêm khoảng 24 MB).
  - `scripts/check.sh` lần 1: dừng ngay ở Gradle vì worktree không có `local.properties` ("SDK location not found"); không liên quan tới plan. Lần 2 (thêm `ANDROID_HOME`): **thất bại** ở `:app:lintStagingDebug`, một lỗi: `NotificationPermission`, "When targeting Android 13 or higher, posting a permission requires holding the POST_NOTIFICATIONS permission (usage from com.squareup.picasso.RemoteViewsAction.NotificationAction)". Picasso 2.8 là phụ thuộc bắt buộc của InMobi SDK (POM của `inmobi-ads-kotlin:11.5.0`); lớp `RemoteViewsAction.NotificationAction` chỉ chạy khi app gọi Picasso nạp ảnh vào thông báo, điều InMobi và dự án không làm. Lint quét cả thư viện (`checkDependencies = true`). Ktlint, unit test (`MediationAdaptersTrialTest` 2 test, `MergedManifestTrialTest` 2 test, 5 test cũ của `MergedManifestTest` đều qua), screenshot và `assembleStagingDebug` đều qua.
  - Thử cách xử lý lỗi lint: `tools:ignore="NotificationPermission"` trên thẻ `manifest` của `core/ads/src/main/AndroidManifest.xml`: **không có tác dụng** (lint báo lỗi ở manifest của `:app`). `app/lint.xml` với `<issue id="NotificationPermission"><ignore regexp="com\.squareup\.picasso\." /></issue>` (lint tự đọc `lint.xml` ở thư mục module): lint qua.
  - `scripts/check.sh` lần 3 (có `app/lint.xml` trên): **qua**, "Cổng chất lượng: QUA", exit 0; 458 test JVM, 0 lỗi (`:app` 73, `:core:ads` 200), 42 ảnh screenshot, mọi test script qua. Plan lấy cách này, cần người duyệt đồng ý (câu hỏi 7).
  - R8: `./gradlew :app:minifyStagingReleaseWithR8` **qua** (exit 0), không cần quy tắc giữ lớp. 22 cảnh báo "Missing class", không phải lỗi: 20 lớp `com.android.billingclient.api.*` do mã `com.tiktok.iap.billing` (SDK TikTok Business đi kèm Pangle, đo doanh thu mua trong app) gọi tới, 2 lớp `com.bytedance.sdk` do chính Pangle gọi. Dự án không có Play Billing nên phần đó của TikTok không chạy.
  - Chưa thử trên máy: adapter khởi tạo (dự kiến `NOT_READY` trên staging), SDK Meta còn khởi tạo được qua adapter khi không có provider, không có mã mạng nào chạy trước đồng ý (kiểm thủ công 1 đến 3).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

1. Quảng cáo: thêm tám adapter mediation (điểm 1).
2. SDK bên thứ ba mới: tám SDK quảng cáo, cùng Picasso, Ktor và protobuf kéo theo (điểm 2).
3. Thứ tự khởi tạo: bỏ provider khởi động SDK Meta lúc mở app; giữ bộ khởi tạo App Startup của Unity và provider vòng đời của ironSource, Mintegral chạy lúc mở app (điểm 5).
4. Module core dùng chung `:core:ads`, cấu hình build dùng chung (`gradle/libs.versions.toml` được bảo vệ, `settings.gradle.kts` thêm kho Maven ngoài) và script dùng chung `scripts/secret-scan.py` (điểm 7).

Không chạm điểm 3 (quyền mới): manifest gộp không thêm quyền nào (đã đo).

Mức: **Cao**, không tính tổng điểm. Bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt vì chạm quảng cáo và dữ liệu gửi ra khỏi máy (risk-scoring.md, mục 3). Chấm năm tiêu chí để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Cấu hình build gốc (`settings.gradle.kts`, catalog), `:core:ads`, test của `:app`, script dùng chung |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, nhưng còn câu hỏi mở ảnh hưởng cách làm: KB-9 tiêu chí 2 không đạt nguyên văn trên staging (câu hỏi 1), cách giữ khóa AppLovin khác chữ của M-03 (câu hỏi 2), danh sách mục 6 của spec chờ xác nhận (câu hỏi 5) |
| C. Khả năng kiểm chứng tự động | 2 | Có test classpath, manifest gộp, script và R8; nhưng adapter khởi tạo, nhận quảng cáo thật và việc không chạy trước đồng ý chỉ kiểm thủ công trên máy, phần `READY` chỉ trên app có mã thật |
| D. Ảnh hưởng chéo | 3 | Mọi app tạo từ dự án mẫu nhận tám SDK, kho Maven ngoài và kích thước app tăng; T-033 (AppsFlyer) cũng sửa catalog; cách nạp quảng cáo của T-021, T-026 quyết định Unity, ironSource có thắng được không (câu hỏi 4) |
| E. Độ mới của kỹ thuật | 2 | Mediation và cách bỏ phần tử bằng manifest merger đã có từ T-015; mới là kho Maven ngoài có `content`, bỏ receiver, và tám SDK chưa từng dùng |
| **Tổng** | 12 | **Mức: Cao** (đã cao vì điểm chặn) |

Rủi ro riêng: SDK Meta có thể dựa vào provider đã bỏ và chạy sai mà không báo lỗi ngay (giảm bằng kiểm thủ công 1 đến 3, và kiểm thủ công 4 trên app có mã thật). Mã nguồn đóng của tám mạng chạy trong app với mọi quyền của app; Mintegral từng bị báo cáo bảo mật (SourMint, 2020); người phụ trách sản phẩm quyết giữ mạng này hay không theo ADR-0026 (câu hỏi 3).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `gradle/libs.versions.toml`, `settings.gradle.kts`, `core/ads/build.gradle.kts`, `core/ads/src/main/AndroidManifest.xml`, manifest gộp của `:app`, `scripts/secret-scan.py`, `scripts/tests/run.sh`, ba tài liệu vận hành dùng cho mọi app. Mọi app tạo từ dự án mẫu nhận tám SDK và hai kho Maven ngoài.
- Task khác trong sprint có thể xung đột:
  - T-033 (AppsFlyer, `:core:analytics`) sửa `gradle/libs.versions.toml` và có thể `MergedManifestTest` (AppsFlyer kéo quyền vào manifest gộp): test `mergedManifest_permissionsAreExactlyTheAllowedSet` của T-034 sẽ đỏ khi T-033 thêm quyền, đúng ý đồ; plan T-033 phải sửa danh sách mong đợi trong test đó (L-054). Không chạy cùng lúc.
  - T-020 (đồng ý và khởi động theo PARTNER) đổi chỗ gọi `MobileAds.initialize`; T-034 không đổi `AdsController.kt`, nên không xung đột file. Việc bỏ provider của T-015 và T-034 là điều kiện cho T-020 giữ đúng "không chạy mã mạng con trước đồng ý".
  - T-021, T-026 viết lại cách nạp quảng cáo toàn màn hình: nên nạp bằng `Activity` khi có, để Unity và ironSource tham gia đấu giá (câu hỏi 4). Ghi vào `ads-guide.md` mục 7 để plan của hai ticket đó đọc.
  - T-036 (màn xem thử thêm định dạng) không chạm file của T-034.
- Đề xuất thứ tự chạy: độc lập với T-019 tới T-032 về file; chạy riêng trước T-033 để T-033 dựng danh sách quyền trên manifest đã có tám mạng. Không chạy cùng phiên với task khác sửa `gradle/libs.versions.toml` hay `MergedManifestTest.kt`.

## 7. Điều kiện dừng riêng

1. Một tọa độ ở mục 3.3 không tải được (kể cả từ hai kho mới), cần kho ironSource hay một kho khác ngoài hai kho ở mục 3.1 bước 3, hoặc Gradle chọn `play-services-ads` khác 25.5.0, `user-messaging-platform` khác 4.0.0, hay đổi phiên bản của bất kỳ thư viện nào khai báo trong catalog. Các thư viện chạy bị nâng theo ghi ở mục 4.1 (ví dụ `guava` 31.1 lên 33.0.0, `androidx.room` 2.2.5 lên 2.5.2) là dự kiến; nâng thêm thư viện khác thì dừng.
2. Manifest gộp có quyền khác danh sách 11 quyền ở mục 4, có provider, receiver hay bộ khởi tạo App Startup khác mục 2, có xung đột manifest, hoặc cần ghi vào `core/ads/src/main/AndroidManifest.xml` gì ngoài hai dòng bỏ và chú thích ở mục 3.1 bước 4.
3. Lint báo lỗi từ thư viện mới ngoài lỗi `NotificationPermission` của Picasso đã biết (mục 4.1), hoặc lỗi đó vẫn còn với `app/lint.xml` ở mục 3.1 bước 4a, hoặc câu hỏi 7 chưa được đồng ý: dừng. Không thêm quy tắc bỏ qua nào khác, không baseline, không sửa `build-logic/`.
4. Build hay R8 cần quy tắc giữ lớp (`proguard-rules.pro`, `consumer-rules.pro`) hay `minSdk` khác.
5. Tài liệu chính thức lúc chạy khác mục 2 (phiên bản, kho, yêu cầu code khởi tạo hay khóa trong manifest): ghi chỗ khác, không tự đổi cách làm.
6. Cần khóa SDK AppLovin, mã của bất kỳ mạng nào, mã quảng cáo hay app id thật ở bất kỳ đâu trong repo.
7. Tên provider, receiver trong AAR lúc chạy khác mục 2, hoặc `tools:node="remove"` không xóa được phần tử (test vẫn thất bại sau khi sửa manifest): dừng, không thử cách bỏ khác.
8. Dấu hiệu SDK Meta không khởi tạo được khi thiếu provider (tài liệu chính thức nói provider là bắt buộc, test hay build báo lỗi liên quan): dừng, báo người duyệt. Phát hiện trên máy sau khi merge thì người tạo phản hồi; plan sửa chọn giữa sửa tiếp và giữ provider, cần người duyệt lại.
9. Câu hỏi 2 chưa được trả lời, hoặc được trả lời bằng phương án B: không chạy plan này (phương án B cần plan phiên bản 2 và người sửa `scripts/release.sh`).

## 8. Câu hỏi cho người duyệt

1. **KB-9 tiêu chí 2 ("Ad Inspector trên staging thấy cả chín adapter đã khởi tạo") không đạt nguyên văn trên staging.** Cả chín adapter đọc mã của mạng từ cấu hình mediation mà AdMob gửi xuống (mục 2); mã app thử của Google không có cấu hình đó, nên trên staging cả chín báo `NOT_READY` (như AppLovin ở FB-20261023-01). Đưa khóa vào app lúc build không đổi được điều này.
   - Đề xuất của AI: sửa tiêu chí thành hai vế: "Khi mở staging thì log trạng thái (và Ad Inspector khi mở được) liệt kê đủ chín adapter" và "Khi một app có mã AdMob thật và nhóm mediation đủ chín mạng thì Ad Inspector thấy cả chín adapter đã khởi tạo". Đóng FB-20261023-01 với phân loại "góp ý về cách kiểm", xử lý bằng câu trả lời này. Người sửa spec (spec `confirmed`).
   - Cách khác: một app AdMob riêng cho staging có nhóm mediation thật dùng đơn vị thử của từng mạng. Không đề xuất: cần tài khoản ở chín mạng cho dự án mẫu, và app id thật trong `app/build.gradle.kts` của staging trái ADR-0004.
   - Trả lời: theo đề xuất của AI: tiêu chí 2 của KB-9 tách thành hai vế, FB-20261023-01 đóng theo câu trả lời này. Spec sửa theo lời anh Nhật (AI ghi, 2026-10-24). (anh Nhật, 2026-10-24)

2. **Khóa SDK AppLovin.** ADR-0026 mục 8 và M-03 ghi "đưa vào lúc build như mã quảng cáo". Bytecode adapter `13.6.4.2` và Changelog 12.1.0.0 cho thấy adapter chỉ lấy khóa từ ánh xạ trên AdMob, không đọc khóa trong app; AdsKit để khóa trong resource là theo cách cũ của adapter.
   - Phương án A (đề xuất của AI): app không mang khóa. Khóa chỉ nhập trong hộp thoại ánh xạ "AppLovin (Bidding)" trên AdMob của từng app; không vào repo, không vào APK, không cần sửa `scripts/release.sh` hay cấu hình production. `secret-scan.py` chặn khóa trong repo; `MergedManifestTest` kiểm staging không có `applovin.sdk.key`. Đúng ý của M-03 (khóa là cấu hình của từng app, không trong resource hay repo), khác chữ "lúc build". Plan sửa chữ M-03 và AR-04 trong `ads-business-rules.md`; ADR-0026 không sửa (ý chính vẫn đúng), người quyết có thêm ghi chú vào ADR không.
   - Phương án B: tham số build `-PapplovinSdkKey` (giống `-PappsFlyerDevKey` của ADR-0027), đưa vào meta-data `applovin.sdk.key` chỉ ở production qua `manifestPlaceholders`; staging rỗng; thiếu tham số thì build production hỏng. Cần sửa `app/build.gradle.kts`, `scripts/release.sh` và mục 2.2 của `release.md`: sửa `release.sh` là việc của người (CLAUDE.md, mục Không được làm). Không đề xuất: theo bytecode, adapter không đọc giá trị này, nên khóa vào APK (ai giải nén APK cũng đọc được) mà không có tác dụng; chỉ có ích khi một app gọi thẳng AppLovin SDK, điều ADR-0004 không cho.
   - Trả lời: phương án A: app không mang khóa, khóa chỉ nhập trong ánh xạ trên AdMob. (anh Nhật, 2026-10-24)

3. **Mintegral.** Hãng bảo mật Snyk báo cáo năm 2020 rằng SDK của Mintegral (khi đó gọi là SourMint) theo dõi lượt bấm và URL trong app; Mintegral bác bỏ và đưa ra kết quả kiểm toán. Mintegral cũng thêm `<queries>` dò năm gói của công cụ root máy (chưa rõ SDK nào trong tám SDK thêm), và có receiver xuất ra ngoài (plan bỏ).
   - Đề xuất của AI: giữ theo ADR-0026 (đã chọn chín mạng của AdsKit), vì báo cáo đã cũ và adapter là bản chính thức của Google; ghi chú vào `ads-guide.md` để từng app quyết khi khai Data safety. Người phụ trách sản phẩm quyết có giữ không.
   - Trả lời: giữ Mintegral theo đề xuất của AI, ghi chú cho Data safety. (anh Nhật, 2026-10-24)

4. **Unity và ironSource cần `Activity` khi nạp banner, interstitial, rewarded** (trang hướng dẫn của Google). `:core:ads` hiện nạp interstitial bằng `ApplicationContext` (`AdsController.preloadInterstitial`); banner và native dùng context của composable.
   - Đề xuất của AI: T-034 không đổi cách nạp (ngoài phạm vi, file của T-021, T-026); ghi yêu cầu vào `ads-guide.md` mục 7 để plan T-021, T-026 nạp quảng cáo toàn màn hình bằng `Activity` đang mở khi có. Doanh thu của hai mạng cho interstitial sẽ thấp tới khi hai ticket đó xong.
   - Trả lời: theo đề xuất của AI. (anh Nhật, 2026-10-24)

5. **Đề xuất sửa spec ads mục 6 (câu hỏi 18).** Spec ghi "Quyền mà từng SDK kéo vào manifest gộp đo ở plan T-033 và T-034, ghi vào mục này trước khi duyệt plan". AI không sửa spec `confirmed`; đề xuất người thêm vào mục 6, Quyền của app và dịch vụ bên ngoài, đoạn sau:

   > **Thêm 2026-10-24 (đo ở plan T-034):** tám adapter (Meta Audience Network `6.22.0.1`, DT Exchange `8.5.0.0`, InMobi `11.5.0.0`, ironSource `9.6.1.0`, Mintegral `17.1.81.1`, Moloco `4.13.0.0`, Pangle `8.3.0.4.0`, Unity Ads `4.21.0.0` kèm SDK Unity Ads `4.21.0`) không thêm quyền nào vào manifest gộp: các quyền chúng khai báo (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`) đã có từ Google Mobile Ads. Không thêm các quyền tùy chọn InMobi gợi ý (vị trí chính xác, trạng thái và đổi Wi-Fi). Mạng nào khởi động SDK lúc mở app thì bỏ phần đó, để mạng chỉ khởi động qua AdMob sau khi có đồng ý: bỏ thành phần khởi động của Meta (cùng cách với AppLovin). Bỏ thành phần nhận sự kiện đổi mạng của Mintegral (app khác gọi được, hệ thống không còn gửi sự kiện này). Giữ các thành phần chỉ theo dõi màn hình đang mở của ironSource, Mintegral, Unity. Khóa SDK AppLovin không nằm trong app: nhập trong ánh xạ nguồn quảng cáo trên AdMob (theo câu trả lời câu hỏi 2 của plan T-034).

   - Plan chờ người xác nhận đoạn này (spec chuyển `changed` rồi `confirmed` lại) trước khi duyệt.
   - Trả lời: đồng ý đoạn đề xuất; AI thêm vào spec mục 6 theo lời anh Nhật, 2026-10-24. (anh Nhật, 2026-10-24)

6. **Kích thước app.** Tám SDK làm APK `stagingDebug` (chưa thu gọn) tăng từ 23,4 MB lên 47,4 MB (mục 4.1); bản release thu gọn bằng R8 sẽ nhỏ hơn nhưng chưa đo. Có cần app nào bỏ bớt mạng không? Đề xuất của AI: dự án mẫu giữ đủ chín mạng theo ADR-0026; app muốn bỏ mạng thì đề xuất ADR của app.
   - Trả lời: giữ đủ chín mạng theo đề xuất của AI. (anh Nhật, 2026-10-24)

7. **Lỗi lint `NotificationPermission` từ Picasso (phụ thuộc bắt buộc của InMobi).** Chạy thử `scripts/check.sh` thất bại vì lint thấy mã gửi thông báo trong Picasso, mà app không có `POST_NOTIFICATIONS`. CLAUDE.md cấm làm cho kiểm tra qua bằng cách tắt quy tắc lint.
   - Đề xuất của AI: tạo `app/lint.xml` bỏ qua quy tắc này **chỉ** cho phát hiện có tên `com.squareup.picasso.` (mục 3.1 bước 4a, đã chạy thử: lint qua). Quy tắc vẫn kiểm mọi lớp khác, kể cả code của app. Lý do: InMobi và app không gọi phần gửi thông báo của Picasso; thêm `POST_NOTIFICATIONS` là xin một quyền không dùng.
   - Cách khác: bỏ InMobi khỏi danh sách (trái ADR-0026, M-01); hay loại Picasso khỏi phụ thuộc của InMobi (`exclude`), không đề xuất: InMobi dùng Picasso để tải ảnh của native, thiếu thì lỗi lúc chạy.
   - Người duyệt quyết; mỗi lần nâng InMobi hay Picasso thì xem lại dòng này.
   - Trả lời: đồng ý tạo `app/lint.xml` chỉ bỏ qua phát hiện của `com.squareup.picasso.`. (anh Nhật, 2026-10-24)

8. **Duyệt chéo khi nhóm chỉ có một dev:** theo risk-scoring.md mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.
   - Trả lời: anh Nhật vừa duyệt vừa duyệt chéo. (anh Nhật, 2026-10-24)

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-24, sau khi anh trả lời các câu ở mục 8 và sau rà soát độc lập trước phiên của RUN-20261024-02 (các điểm nên sửa đã sửa trong plan trước khi duyệt).

Phiên bản 2: chờ người duyệt.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261024-02, phiên bản plan 1

- **Kết quả:** dừng ở bước 4 (mục 3.4, điều kiện dừng 1 ở mục 7)
- **Nhánh, PR:** `claude/T-034-mediation-chin-mang`, chưa mở PR. Commit `c2b821a` (test), `1a105a9` (khai báo thư viện, chưa tải được). Bước 3 đến 5 do phiên con làm trong worktree riêng.
- **Đã làm:** viết 9 test của mục 4; khai báo chín thư viện, chín dòng `implementation` và hai kho có `content` theo bước 1 đến 3. Chưa làm bước 4 đến 9 (manifest, `secret-scan.py`, `app/lint.xml`, tài liệu).
- **Test viết trước:** 9 test (2 classpath, 5 `MergedManifestTest`, 2 test `secret-scan` trong `scripts/tests/run.sh`), commit `c2b821a`. Lần 1 (chưa có thư viện): `networkAdapters_areOnClasspath` thất bại `ClassNotFoundException: com.google.ads.mediation.facebook.FacebookMediationAdapter`; `unityAdsSdk_isOnClasspath` thất bại `ClassNotFoundException: com.unity3d.ads.UnityAds`; `containsActivityOfEveryNetwork` thất bại "No com.facebook.ads. activity"; `secret-scan: an AppLovin SDK key fails` thất bại (script trả 0). `hasNoMetaAudienceNetworkProvider`, `hasNoMintegralNetworkReceiver`, `secret-scan: a fake AppLovin key passes` qua, đúng mục 4. 2 test có bước đối chứng (L-055): `permissionsAreExactlyTheAllowedSet` (bỏ `WAKE_LOCK` khỏi danh sách mong đợi thì thất bại, rồi đặt lại) và `hasNoAppLovinSdkKey` (thêm tạm `meta-data` `applovin.sdk.key` giá trị `FAKE` thì thất bại "applovin.sdk.key must not be in the app", rồi gỡ). Manifest gộp trước khi thêm thư viện có đúng 11 quyền của mục 4. Năm test cũ của T-015 không sửa, vẫn qua.
- **Lý do dừng:** lần 2 (sau khi thêm thư viện) Gradle chỉ không tải được `com.pangle.global:pag-sdk:8.3.0.4` và `com.mbridge.msdk.oversea:mbridge_android_sdk:17.1.81`: "Could not GET 'https://artifact.bytedance.com/repository/pangle/...pag-sdk-8.3.0.4.pom'. Received status code 403 from server: Forbidden", tương tự với `dl-maven-android.mintegral.com`. Mọi thư viện khác tải được từ `google()`, `mavenCentral()`. Proxy của môi trường báo `connect_rejected` cho hai máy chủ đó; `curl` từ phiên chính cũng không kết nối được, trong khi `dl.google.com` trả 302. Đây là chặn theo chính sách mạng, không phải lỗi tạm (429, 5xx), nên không thử lại theo lịch ở bước 5. Không thêm init script, không đổi kho hay biến môi trường.
- **Rà soát độc lập:** chưa làm (dừng trước bước 6).
- **Kết quả CI:** chưa đẩy PR.
- **Lệch so với plan:** chú thích một dòng ở bước 3 dài 124 ký tự ở đúng thụt lề, tách thành hai dòng (giới hạn 120 của ktlint). Test `an AppLovin SDK key fails` gộp kiểm che khóa vào cùng một `expect` (giá trị thực: "<mã trả về> <số lần khóa đầy đủ xuất hiện>", mong đợi "1 0"), giữ số test là hai. Không đổi điều test kiểm.
- **Việc còn lại:** bước 4 đến 10 (xem mục 3.1, Tiếp tục).
- **Cần người xem:** cho phép `artifact.bytedance.com` và `dl-maven-android.mintegral.com` trong chính sách mạng của môi trường cloud (`docs/ops/staging-and-runs.md`), hoặc chạy task trên máy dev; duyệt plan phiên bản 2.
