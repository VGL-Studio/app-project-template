---
ticket: T-071
title: Banner và MREC của MAX
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 22.2M        # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-30, trung vị 20 task M)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-071: BANNER VÀ MREC CỦA MAX

## 1. Tóm tắt

Banner cố định (`AdFormat.BANNER`, cả lớp banner của thanh đáy) và banner MREC (`AdFormat.BANNER_MREC`) lấy quảng cáo từ AppLovin MAX và AdMob theo thứ tự `AdsController.mediationOrder` (T-068): mỗi khung banner giữ một `MaxAdView` và một `AdView`, mỗi lượt gửi hỏi nền tảng thứ nhất, không có quảng cáo thì hỏi nền tảng thứ hai ngay trong cùng lượt, cùng khung, không đổi chiều cao. Mọi lượt gửi của MAX đi qua cùng bước `sendWhenAllowed` của `runBannerLoads` (màn hiện, không bị che, cổng đồng ý mở), nên luật B-01 tới B-08 và KB-27 áp như nhau; tạm dừng là `stopAutoRefresh()` của MAX, như `pauseAutoRefresh` của JBase. Banner thu gọn giữ AdMob. Mã đơn vị MAX theo khe (`banner`, `banner_mrec`) vào lúc build như khóa SDK MAX: `-PmaxAdUnits` ở production, `ads.maxAdUnits` của `local.properties` ở staging. Kiểm bằng unit test của luật chọn nền tảng và của khung hai nền tảng trên bản giả, test Robolectric của `BannerSlotHost` (chiều cao không nhảy, MREC không co giữa hai nền tảng, bị che thì dừng tự làm mới của MAX), test của bọc `MaxAdView` trên bản giả, và kiểm thủ công trên máy thật có khóa MAX.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31 | Khi nền tảng thứ nhất theo `ads_mediation_order` (mặc định MAX) chưa có quảng cáo cho một lượt hiện và nền tảng thứ hai có thì người dùng thấy quảng cáo của nền tảng thứ hai; một lượt chỉ ghi một cơ hội. | Với banner, "một lượt hiện" là một lượt gửi của khung (lượt đầu, thử lại sau lỗi, nạp lại khi cổng mở lại). Nền tảng thứ nhất báo lỗi nạp thì khung gửi ngay cho nền tảng thứ hai (không chờ, không tính là một lần lỗi của B-02). Cả hai lỗi thì là một lần lỗi, thử lại theo B-02 bắt đầu lại từ nền tảng thứ nhất. Banner không ghi `ad_opportunity` (mục 8.2 của luật chỉ ghi cho toàn màn hình và pool), nên vế "một cơ hội" không có sự kiện nào để đếm; giữ đúng một lượt `requestSent` của AdMob mỗi lần AdView gửi như hôm nay. |
| KB-31 | Khi `ads_mediation_order` là `admob` hay `max` thì chỉ nền tảng đó được nạp và hiện. | `max`: khung không dựng `AdView`, không gửi cho AdMob; không có mã MAX của khe thì không có khung (như B-04). `admob`: không dựng `MaxAdView`. Build không có khóa SDK MAX: thứ tự có hiệu lực là `admob` (`MediationOrder.effective`, T-068), banner chạy như hôm nay. |
| KB-31 | MAX chỉ khởi động sau cổng đồng ý; không bao giờ gửi yêu cầu khi chưa có đồng ý. | Mỗi lượt `loadAd()` của MAX đi qua `sendWhenAllowed` (hỏi `mayRequestNow()`); MAX chưa sẵn sàng (`AdsController.maxReady` còn `false`) thì lượt đó coi như MAX không có quảng cáo, chuyển sang nền tảng kia, hay là một lần lỗi khi thứ tự chỉ có MAX. |
| KB-31 | Doanh thu của MAX ghi ở Firebase và AppsFlyer với vị trí, không tính hai lần. | Không thuộc T-071: doanh thu và số liệu nạp của MAX là T-072 (câu hỏi 2). T-071 không gắn `setRevenueListener`, không gọi `AdTelemetry` cho lượt của MAX. |
| KB-31 | Khóa SDK MAX và mã APS không nằm trong repo; bản staging chạy được khi không có chúng. | Mã đơn vị MAX cũng không vào repo: tham số build ở production, `local.properties` ở staging. CI không có khóa nên bản staging của CI chạy như hôm nay. |
| KB-18 | Banner tự làm mới theo đơn vị; ở nền không tải lại; chưa đồng ý không gửi; rút đồng ý giữ khung, đồng ý lại tải lại trong khung; banner thu gọn 30 giây; cỡ neo đáy thường. | Áp cho MAX bằng cùng `BannerSlotHost`, `runBannerLoads`: MAX tự làm mới theo cài đặt của đơn vị MAX; `pause()` là `stopAutoRefresh()`, `resume()` là `startAutoRefresh()` của nền tảng đang hiện. Cỡ: banner thích ứng của MAX (`adaptive_banner`) cùng bề rộng khung. Banner thu gọn chỉ AdMob (MAX không có banner thu gọn). |
| KB-29 | MREC 300 x 250 dp căn giữa; cùng luật banner cố định; đang tải giữ 250 dp, nạp lỗi thì co về 0; số liệu `BANNER_MREC`; staging có MREC trên màn xem thử dùng mã thử của Google. | MAX dùng `MaxAdFormat.MREC` (300 x 250). Khung chỉ co về 0 khi **mọi** nền tảng của lượt đều lỗi trước khi có quảng cáo (không co giữa hai nền tảng). Staging của CI không có khóa MAX nên màn xem thử vẫn chỉ dùng mã thử của Google; dev có khóa và `ads.maxAdUnits` thì màn xem thử thử được MAX. |
| KB-27 | Native toàn màn hình đang hiện thì banner bên dưới không gửi, không làm mới, không thử lại; quảng cáo đang có giữ nguyên; đóng thì chạy lại, lượt tới hạn không dồn. | `covered` của `BannerSlotHost` (T-054) đã dẫn tới `pause()` và chặn mọi lượt gửi; với MAX, `pause()` gọi `stopAutoRefresh()` (kèm tham số `allow_pause_auto_refresh_immediately`), `resume()` gọi `startAutoRefresh()`. Câu hỏi 36 của spec: JBase `pauseAutoRefresh: stopped (fullscreen showing)`. |

Thiết kế tham chiếu: không có thiết kế riêng (docs/ops/design-workflow.md). Khung và kích thước giữ như T-062, T-067.

ADR liên quan: ADR-0037 (MAX, thứ tự, khóa, đồng ý), ADR-0036 (MREC, dừng khi bị che), ADR-0038 (cỡ banner), ADR-0026 (luật PARTNER), ADR-0013 (vị trí và mã), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo các mẫu đã có: giao diện `BannerAdView` và bản thật `SdkBannerAdView` (T-029, T-062) cho phần SDK; luật thuần tách hàm, test bằng bản giả (`FakeBannerAdView`, ADR-0026); cấu hình build cấp qua Hilt từ `:app` như `MaxConfig` (T-068); mã theo khe dạng `<khóa>=<mã>;...` như `ConfiguredAdUnitIds` (T-056).

1. **Mã đơn vị MAX** (`MaxSdk.kt`, sửa):
   - `MaxConfig` thêm tham số thứ hai `adUnits: String = ""` (có mặc định, để `MaxConfig(sdkKey)` của test T-068 vẫn biên dịch). Dạng `<mã khe>=<mã đơn vị>;...`, mã khe là `AdFormat.code` (`banner`, `banner_mrec`): một mã MAX cho mỗi định dạng của cả app, như JBase (`applovin_mrec_ads_key`) và "mã đơn vị MAX theo khe" của ADR-0037. Hàm `fun unitFor(format: AdFormat): String?`: chỉ trả mã cho `AdFormat.BANNER` và `AdFormat.BANNER_MREC` (định dạng khác `null`, T-069 và T-070 mở rộng); không có khóa SDK (`!hasKey`) thì `null`; mã rỗng là `null`. Mục sai dạng (thiếu `=`, khóa hay mã rỗng) bỏ qua, không ném lỗi (khác `ConfiguredAdUnitIds`: thiếu MAX chỉ làm khung chạy AdMob). `toString()` giữ che khóa và ghi số khe có mã, không ghi mã (`MaxConfig(sdkKey=abcd…, units=2)`).
2. **Bọc `MaxAdView`** (`MaxBannerAdView.kt`, tạo):
   - `internal interface MaxBannerSdkView`: `fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit)`, `loadAd()`, `startAutoRefresh()`, `stopAutoRefresh()`, `destroy()`, `val view: View`.
   - `internal class AppLovinMaxBannerView(unitId: String, kind: BannerSizeKind, widthDp: Int, context: Context) : MaxBannerSdkView`: `BannerSizeKind.ANCHORED` dựng `MaxAdView(unitId, MaxAdFormat.BANNER, context)` kèm `setExtraParameter("adaptive_banner", "true")` và `setExtraParameter("adaptive_banner_width", widthDp.toString())`; `MEDIUM_RECTANGLE` dựng `MaxAdView(unitId, MaxAdFormat.MREC, context)`; `LARGE_ANCHORED` ném `IllegalArgumentException` (banner thu gọn không qua MAX). **Không** dùng hàm dựng `MaxAdView(String, MaxAdFormat, MaxAdViewConfiguration)`: nó không nhận `Context` và ném `NullPointerException` khi SDK MAX chưa khởi tạo (chạy thử, mục 4.1). Gọi thêm `setExtraParameter("allow_pause_auto_refresh_immediately", "true")` để `stopAutoRefresh()` dừng ngay, không chờ hết lượt làm mới đang đếm. Listener `MaxAdViewAdListener`: `onAdLoaded` gọi `onLoaded`, `onAdLoadFailed` gọi `onFailed(error.code)`; các hàm khác không làm gì. Chữ ký đã đọc bằng `javap` trên `applovin-sdk-13.6.4` và gọi thử trong JVM (mục 4.1). Không gắn `setRevenueListener` (T-072).
   - `internal fun maxBannerHeightDp(context: Context, kind: BannerSizeKind, widthDp: Int): Int`: `ANCHORED` là `MaxAdFormat.BANNER.getAdaptiveSize(widthDp, context).height`, `MEDIUM_RECTANGLE` là `MEDIUM_RECTANGLE_HEIGHT_DP`.
   - `internal class MaxBannerAdView(sdk: MaxBannerSdkView, logger: AppLogger, placementKey: String) : BannerAdView`: `results` từ callback (bộ đệm như `SdkBannerAdView`); `send()` gọi `loadAd()`; `pause()` gọi `stopAutoRefresh()`; `resume()` gọi `startAutoRefresh()`; `destroy()`; `Content` vẽ `sdk.view` bằng `AndroidView`, `INVISIBLE` khi `showAd` là `false` (B-06). Log `"Banner <placementKey> MAX loaded"`, `"Banner <placementKey> MAX failed (code <n>)"`; không bao giờ ghi mã đơn vị.
3. **Khung hai nền tảng** (`PlatformBanners.kt`, tạo):
   - `internal fun bannerPlatforms(format: AdFormat, order: MediationOrder, admobUnit: String?, maxUnit: String?): List<AdPlatform>`: `BANNER_COLLAPSIBLE` chỉ `[ADMOB]` khi có mã AdMob; `BANNER`, `BANNER_MREC`: `order.platforms` lọc nền tảng có mã không rỗng, giữ thứ tự. Rỗng thì khung không có (như B-04). Không đọc `maxReady`: danh sách đổi khi MAX vừa sẵn sàng sẽ dựng lại khung, mất quảng cáo AdMob đang hiện.
   - `internal fun bannerFrameHeightDp(platforms: List<AdPlatform>, admobHeightDp: Int, maxHeightDp: Int): Int`: lớn nhất trong chiều cao của các nền tảng có trong danh sách, để khung không nhảy khi đổi nền tảng (ticket: "không nhảy chiều cao"). MREC: 250 cả hai.
   - `internal fun bannerSlotUnitKey(platforms, admobUnit, maxUnit): String?`: khóa của khung cho `BannerSlotHost` (tham số `unitId`): `null` khi danh sách rỗng, khác thì chuỗi ghép nền tảng và mã (chỉ dùng làm khóa `key(...)` trong bộ nhớ, không log). Đổi thứ tự hay mã thì khung dựng lại, như đổi mã hôm nay.
   - `internal class OrderedBannerAdView(children: List<Pair<AdPlatform, BannerAdView>>, maxReady: () -> Boolean, logger: AppLogger, placementKey: String) : BannerAdView`:
     - `send()` gửi cho nền tảng ở vị trí `next` (bắt đầu 0). Nền tảng là MAX mà `maxReady()` là `false` thì không gọi `send()` của con, coi như con đó báo lỗi ngay (mã `MAX_NOT_READY = -1`), log `"Banner <key> MAX not ready"`.
     - Trạng thái: `next` (con sẽ được hỏi), `showing` (con đang hiện, chưa có thì `null`), `running` (khung đang chạy: `pause()` đặt `false`, `resume()` đặt `true`, mặc định `true`), và mỗi con một cờ `paused` (con đó đã bị `pause()` mà chưa `resume()`).
     - Kết quả của con đang được hỏi: `Loaded` thì đặt `showing` là con đó, `next = 0`, `pause()` mọi con khác chưa dừng; nếu `running` và con vừa nạp đang `paused` thì `resume()` nó (MAX: `startAutoRefresh()`), để con vừa thành con hiện sau khi từng bị dừng vẫn tự làm mới (KB-18; rà soát độc lập, điểm chặn C1); rồi phát `Loaded`. `Failed(code)` thì `pause()` con đó (không để SDK tự làm mới hay tự gửi ngoài `sendWhenAllowed`, L-072), rồi: còn nền tảng sau thì `next++`, log `"Banner <key> <platform> no ad, trying <platform sau>"`, phát `BannerLoadResult.TryNext`; nền tảng cuối thì `next = 0`, phát `Failed(code)`. Con lỗi mà đang là `showing` thì vẫn là `showing` (quảng cáo cũ còn hiện) tới khi con khác nạp được.
     - Kết quả của con đang hiện mà không phải con đang hỏi (làm mới của SDK): `Loaded` phát `Loaded`; `Failed` phát `Failed` (thử lại B-02 bắt đầu từ nền tảng thứ nhất, quảng cáo đang có vẫn hiện tới khi lượt mới có quảng cáo). Kết quả của con khác bỏ qua.
     - `pause()`: `running = false`, `next = 0` (vòng gửi dừng giữa chừng sau `TryNext`, ví dụ cổng đóng hay khung giữ trống, thì vòng mới hỏi lại từ nền tảng thứ nhất; nên sửa N1), `pause()` mọi con chưa dừng. `resume()`: `running = true`, chỉ `resume()` con đang hiện nếu nó `paused` (con khác giữ dừng, không tự làm mới ngầm). `destroy()`: `next = 0`, hủy mọi con. `Content` chồng các con trong một `Box` cùng kích thước, chỉ con `showing` được `showAd = true`; chưa có con nào hiện thì không con nào hiện.
     - Đường gửi của MAX (L-072): (a) `loadAd()` chỉ qua `send()` của khung, tức qua `sendWhenAllowed` của `runBannerLoads`; (b) tự làm mới của SDK chỉ khi con MAX là `showing` và khung `running` (con không hiện, con vừa lỗi, khung dừng đều đã `stopAutoRefresh()`). Không đường nào khác gọi `loadAd()`.
     - Một con duy nhất (thứ tự `admob`, `max`, hay thiếu mã của một nền tảng) vẫn đi qua lớp này để một đường gửi duy nhất (L-072).
4. **Vòng gửi** (`BannerLoads.kt`, sửa): `BannerLoadResult` thêm `data object TryNext`. `runBannerLoads`: nhận `TryNext` thì `sending = sendWhenAllowed()` ngay, không tăng `failures`, không gọi `onFailed`, không chờ. Nhờ vậy lượt gửi cho nền tảng thứ hai cũng đợi màn hiện và hỏi `mayRequestNow()` (S-15, KB-27, L-072); khung MREC không co giữa hai nền tảng (`onFailed` chỉ chạy khi mọi nền tảng lỗi). KDoc của hàm thêm một gạch đầu dòng về `TryNext`.
5. **`AdSlot.kt`, `BannerSlot`** (sửa): đọc `ads.mediationOrder` (`collectAsStateWithLifecycle`), `admobUnit = ads.unitId(placement, format)`, `maxUnit = ads.maxUnitId(format)`; `platforms = bannerPlatforms(...)`; `heightDp = bannerFrameHeightDp(platforms, adSize.height, maxHeight)` với `maxHeight = maxBannerHeightDp(...)` chỉ tính khi danh sách có MAX; `unitId = bannerSlotUnitKey(...)`. Báo chiều cao cho thanh đáy theo khung: `LaunchedEffect(adSize)` cũ đổi thành `LaunchedEffect(heightDp) { currentOnHeight(heightDp.dp) }`, để `bottomBarReservation` chừa đúng chiều cao khung hai nền tảng (nên sửa N3). `newView` dựng `OrderedBannerAdView` với `SdkBannerAdView` (như hôm nay) cho `ADMOB` và `MaxBannerAdView(AppLovinMaxBannerView(...))` cho `MAX`, `maxReady = { ads.maxReady.value }`. `BannerSlotHost`, `BannerFrameContent` không đổi chữ ký. KDoc của `AdSlot` thêm một gạch đầu dòng: banner cố định và MREC lấy quảng cáo của MAX và AdMob theo `ads_mediation_order`, banner thu gọn chỉ AdMob.
6. **`AdsController.kt`** (sửa): `internal fun maxUnitId(format: AdFormat): String? = maxConfig.unitFor(format)`. Khi `start()`, nếu `maxConfig.hasKey` mà không có mã MAX cho `banner` hay `banner_mrec` thì log một lần `"MAX ad units missing, these use AdMob only: banner, banner_mrec"` (không ghi mã). Không đổi `mediationOrder`, `maxReady`, `mayRequestNow`.
7. **`:app`**:
   - `app/build.gradle.kts`: flavor `staging` đọc `localProperties.getProperty("ads.maxAdUnits")`, `trim()`, giữ nguyên chuỗi khi khớp `Regex("[A-Za-z0-9_=;-]*")`, khác thì rỗng; `buildConfigField("String", "MAX_AD_UNITS", "\"$maxAdUnits\"")`. **Không** thêm dòng `logger.warn(` nào trong khối `staging` (test `ProductionPropertiesTest.stagingMaxKey_isFilteredBeforeBuildConfig` đòi đúng một dòng `logger.warn(`, mục 3.5); giá trị sai dạng thì bỏ, `AdsController` log thiếu mã lúc chạy (bước 6). Flavor `production`: `buildConfigField("String", "MAX_AD_UNITS", "\"${providers.gradleProperty("maxAdUnits").getOrElse("")}\"")`, tùy chọn như `maxSdkKey` tới T-073. Chú thích tham số ở đầu `productFlavors` thêm `-PmaxAdUnits="banner=<MAX ad unit>;banner_mrec=<MAX ad unit>" (optional until T-073)`. Không đổi `missingProductionProperties`.
   - `app/src/main/kotlin/com/vglstudio/template/di/MaxConfigModule.kt`: `MaxConfig(BuildConfig.MAX_SDK_KEY, BuildConfig.MAX_AD_UNITS)`; KDoc thêm mã đơn vị.
8. **Tài liệu**:
   - `docs/ops/ads-business-rules.md`: mục 5.5 thêm dòng B-09 "Banner cố định và MREC của MAX (T-071, ADR-0037): mỗi khung hỏi nền tảng theo `ads_mediation_order`; nền tảng thứ nhất lỗi thì gửi ngay cho nền tảng thứ hai trong cùng khung, qua cùng bước kiểm màn hiện và cổng đồng ý; cả hai lỗi là một lần lỗi của B-02; khung cao bằng nền tảng cao nhất; MREC chỉ co về 0 khi mọi nền tảng lỗi; tạm dừng là `stopAutoRefresh()` của MAX (B-06, B-07); banner thu gọn chỉ AdMob" (nguồn: JBase `pauseAutoRefresh`, `applovin_mrec_ads_key`, theo ADR-0037). Mục 9 thêm M-10 "Mã đơn vị MAX theo khe (`banner`, `banner_mrec`), một mã mỗi định dạng của app, vào lúc build bằng `-PmaxAdUnits`; staging đọc `ads.maxAdUnits` của `local.properties`; thiếu mã thì khe đó chỉ chạy AdMob".
   - `docs/ops/ads-guide.md`, tiểu mục "AppLovin MAX (T-068)": thêm đoạn banner và MREC của MAX (khe, `ads.maxAdUnits` để thử ở staging, cách thấy trong log `VGL.Ads`, banner thu gọn chỉ AdMob, doanh thu MAX chờ T-072); sửa câu "MAX chưa nạp hay hiện quảng cáo nào (T-069 tới T-071)" thành "chưa nạp quảng cáo toàn màn hình (T-069, T-070)".
   - Không sửa spec, ADR, `docs/ops/release.md`, `scripts/release.sh` (T-073), `scripts/secret-scan.py` (mẫu mã đơn vị MAX: T-073, câu hỏi 3).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxSdk.kt` | `MaxConfig.adUnits`, `unitFor`, `toString` (bước 1) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxBannerAdView.kt` | `MaxBannerSdkView`, `AppLovinMaxBannerView`, `maxBannerHeightDp`, `MaxBannerAdView` (bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PlatformBanners.kt` | `bannerPlatforms`, `bannerFrameHeightDp`, `bannerSlotUnitKey`, `OrderedBannerAdView` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerLoads.kt` | `BannerLoadResult.TryNext`, nhánh trong `runBannerLoads` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerSlot` dựng khung hai nền tảng; KDoc của `AdSlot` (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `maxUnitId`, log thiếu mã MAX lúc `start()` (bước 6) |
| Sửa | `app/build.gradle.kts` | `MAX_AD_UNITS` cho hai flavor, chú thích tham số (bước 7) |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/di/MaxConfigModule.kt` | Truyền `MAX_AD_UNITS` (bước 7) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxSdkStarterTest.kt` | Test `MaxConfig.unitFor`, `toString` (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxBannerAdViewTest.kt` | Test bọc MAX trên `FakeMaxBannerSdkView` (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeMaxBannerSdkView.kt` | Bản giả của `MaxBannerSdkView`: đếm `loadAd`, `start/stopAutoRefresh`, `destroy`, phát callback theo lệnh |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PlatformBannersTest.kt` | Test `bannerPlatforms`, `bannerFrameHeightDp`, `bannerSlotUnitKey`, `OrderedBannerAdView` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeBannerAdView.kt` | `emitTryNext()` và tham số `tag` (mặc định `FAKE_BANNER_TAG`) để phân biệt con đang hiện; vào commit test của bước 3 (L-025) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerAdSizeTest.kt` | Test nối chiều cao thanh đáy và thứ tự `admob` có khóa MAX (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerLoadsTest.kt` | Test `TryNext` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerSlotHostTest.kt` | Test khung hai nền tảng qua `BannerSlotHost` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerMaxTest.kt` | Test `maxUnitId`, log thiếu mã (mục 4) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` | Test `MAX_AD_UNITS`: staging chỉ đọc `local.properties` và lọc, production đọc `-PmaxAdUnits`, không bắt buộc (mục 4) |
| Sửa | `docs/ops/ads-business-rules.md` | B-09, M-10 (bước 8) |
| Sửa | `docs/ops/ads-guide.md` | Banner và MREC của MAX (bước 8) |

Nơi đọc các file bị sửa (L-020; `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, 2026-10-30):

- `MaxSdk.kt`: `MaxConfig(` chỉ được gọi ở `MaxConfigModule.kt` và test của `:core:ads`; tham số mới có giá trị mặc định nên không phải sửa chỗ gọi.
- `BannerLoads.kt`: `BannerLoadResult` được đọc ở `BannerAdView.kt`, `FakeBannerAdView.kt`, `SdkBannerAdViewTest.kt`; không có `when` nào trên `BannerLoadResult` ngoài `runBannerLoads`, nên thêm `TryNext` không làm hỏng biên dịch chỗ khác.
- `AdSlot.kt`, `AdsController.kt`: chỉ code và test của `:core:ads`, `:app` gọi; chữ ký công khai (`AdSlot`, `BannerSlotHost`) không đổi.
- `app/build.gradle.kts`: `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` đọc khối flavor (có trong bảng); `scripts/tests/run.sh` (test hợp đồng L-125) chỉ đọc `missingProductionProperties` và mục `"ADR-0027"`, plan không đổi hai chỗ đó; `scripts/new-app.sh` chỉ thay địa chỉ R2 staging, `scripts/setup-check.sh` đọc `ASSETS_BASE_URL` và `applicationId`, `scripts/ui-test-verdict.sh` đọc `applicationId`: plan không đổi các dòng đó, không sửa ba script.
- `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`: không script hay test nào đọc nội dung hai file này. `scripts/ads-plan.py` chỉ nhắc tên hai file trong chú thích và chữ của bảng Excel (đọc `remote-config-keys.md`, `AdPlacement`, hằng `EVENT_`, không mở hai file). `AdFormatTest.kt` chép tay danh sách MC-02: plan không đổi MC-02 (không thêm định dạng). Các test khác chỉ nhắc `ads-business-rules.md` trong chú thích dẫn luật, không mở file, và plan không đổi các mục chúng dẫn (S, 4, 5.7, 8, 10): `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`; `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` (MC-02 không đổi); `scripts/ads-plan.py` (không mở hai file); `scripts/new-app.sh`, `scripts/setup-check.sh`, `scripts/ui-test-verdict.sh` (đọc dòng của `app/build.gradle.kts` mà plan không đổi).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Bật tắt theo `is_show_<mã vị trí>` và `ads_mediation_order` đã có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí; thêm nguồn MAX cho định dạng `banner`, `banner_mrec` đã có (KB-31, ADR-0037, câu hỏi 41 của spec). Điểm chặn 1.
- Sự kiện phân tích: Không chạm. Lượt của MAX không ghi số liệu nạp hay doanh thu trong T-071 (T-072, câu hỏi 2); số liệu của AdMob giữ như hôm nay.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `MaxAdView` thuộc SDK đã có từ T-068, manifest gộp đã đo ở T-068 (spec mục 6).
- **Thư viện và SDK:** Không thêm. Dùng `applovin-sdk` (`libs.applovin.sdk`, 13.6.4) đã có trên classpath biên dịch của `:core:ads` từ T-068; `MaxAdView`, `MaxAdFormat`, `MaxAdViewAdListener`, `MaxAd`, `MaxError` đều trong thư viện đó (đọc bằng `javap`, mục 4.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** cỡ banner thích ứng của MAX bằng tham số phụ `adaptive_banner`, `adaptive_banner_width` trên máy thật (JVM không nạp quảng cáo thật), và việc gọi `MaxAdView` khi SDK MAX đã khởi tạo nhưng mạng con trả cỡ khác.
- **Điều kiện chuyển:** kiểm thủ công trên máy thật (mục 4) cho thấy banner MAX không theo bề rộng khung, hay test dùng `AppLovinMaxBannerView` đỏ vì cùng một lỗi của SDK sau 2 lần sửa.
- **Cách làm thay:** banner MAX dùng cỡ cố định của `MaxAdFormat.BANNER` (`MaxAdFormat.BANNER.size`, 320 x 50 trên điện thoại), bỏ hai tham số phụ; `maxBannerHeightDp(ANCHORED)` trả chiều cao đó; khung vẫn cao bằng nền tảng cao nhất (`bannerFrameHeightDp`). Chỉ đổi `MaxBannerAdView.kt`; test và tiêu chí ở mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` chạy trên plan này (2026-10-30, nhánh chính `0bd30d9`); kết quả đầy đủ ở dòng cuối bảng.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Khung banner có một `AdView`, `newView` trả `SdkBannerAdView` | `newView` trả `OrderedBannerAdView` bọc `SdkBannerAdView` (bước 5) | `BannerSlotHostTest` (dựng `BannerSlotHost` với `FakeBannerAdViews`, không qua `BannerSlot`) | Giữ: `BannerSlotHost` không đổi chữ ký; test cũ không đi qua `BannerSlot` |
| `runBannerLoads` chỉ có `Loaded`, `Failed` | Thêm `TryNext`: gửi lại ngay, không tính lỗi (bước 4) | `BannerLoadsTest` (mọi test hiện có) | Giữ: các test cũ không phát `TryNext`, hành vi của `Loaded`, `Failed` không đổi |
| `MaxConfig(sdkKey)` một tham số, `toString` chỉ che khóa | Thêm `adUnits` có mặc định, `toString` ghi số khe (bước 1) | `MaxSdkStarterTest` (dựng `MaxConfig(FAKE_KEY)`, `MaxConfig.NONE`, `MaxConfig("   ")`; kiểm log không chứa khóa) | Giữ: không test nào so nguyên chuỗi `toString`; `MaxConfig(sdkKey)` vẫn biên dịch nhờ giá trị mặc định |
| Staging có đúng một dòng `logger.warn(` (khóa MAX sai dạng) | Không thêm dòng `logger.warn(` (bước 7) | `ProductionPropertiesTest.stagingMaxKey_isFilteredBeforeBuildConfig` | Giữ: plan không thêm `logger.warn(` trong khối `staging` |
| `BottomBannerSlot` báo `onHeight` bằng chiều cao neo của AdMob | Báo `bannerFrameHeightDp` (bước 5); không khóa MAX thì danh sách chỉ `[ADMOB]`, số không đổi | `BannerAdSizeTest.bottomBannerSlot_reportsAnchoredHeight` (dựng `testAdsController()` không khóa MAX) | Giữ: không khóa thì chiều cao khung bằng chiều cao neo của AdMob |
| Production không bắt buộc tham số MAX | Không đổi (`maxAdUnits` tùy chọn) | `ProductionPropertiesTest.maxSdkKey_isOptionalUntilReleaseShPassesIt` | Giữ: `missingProductionProperties` không có `maxAdUnits` |
| Banner cố định, MREC luôn của AdMob | Theo `ads_mediation_order` khi build có khóa và mã MAX | Không test nào dựng `AdsController` có khóa MAX rồi vẽ `AdSlot`; build không khóa: thứ tự `admob`, như cũ | Giữ |

Kết quả `scripts/existing-tests.py` (2026-10-30): 320 test dùng tên hay tag của các file plan sửa. Nhóm "Cần xem trước" có 15 test, đều trùng tên chung chứ không đi qua code plan đổi: `Loaded`, `Failed` của trạng thái native (`InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`, `NativeFlowStepTest`) và của màn loading (`LoadingScreenTest`), `unitId` của native (`NativeAdSlotTest.slotAllowed_trueWhenAllGiven`), `AdsController`, `toString` của `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction` (dựng `AdsController` không khóa MAX: thứ tự `admob`, không đổi). Nhóm "Nên xem" (130): đã đọc các test của `SdkBannerAdViewTest`, `MaxSdkStarterTest`, `AdsControllerMaxTest`, `BannerLoadsTest`, `BannerSlotHostTest`; các test còn lại chỉ trùng tên chung (`Loaded`, `Failed`, `unitId`, `toString`). Không câu khẳng định nào phải viết lại.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-31: thứ tự quyết nền tảng; `admob`, `max` chỉ một nền tảng; banner thu gọn chỉ AdMob; thiếu mã thì bỏ nền tảng đó | Unit test | `PlatformBannersTest.platforms_followOrder_dropMissingUnits_collapsibleAdmobOnly`: bảng mọi tổ hợp 4 thứ tự x 3 định dạng banner x có hay không mã từng nền tảng; `max` thiếu mã MAX là danh sách rỗng; `max_admob` thiếu mã MAX là `[ADMOB]`; `BANNER_COLLAPSIBLE` với `max` là `[ADMOB]`. Đối chứng: `MAX_ADMOB` đủ mã là `[MAX, ADMOB]` đúng thứ tự, `ADMOB_MAX` là `[ADMOB, MAX]` |
| KB-31: nền tảng thứ nhất không có thì nền tảng thứ hai, cùng khung | Unit test | `PlatformBannersTest.firstFails_sendsSecond_showsSecond`: hai `FakeBannerAdView` (MAX, AdMob); `send()`, MAX `emitFailed(204)` thì kết quả là `TryNext` và MAX `pauseCount` 1 (con lỗi bị dừng, N2); `send()` lần hai gửi cho AdMob (`sendCount` MAX 1, AdMob 1); AdMob `emitLoaded` thì `Loaded`. Phần vẽ (chỉ con hiện có thẻ) kiểm ở `BannerSlotHostTest.banner_twoPlatforms_firstFails_heightUnchanged_secondShown` bằng compose rule (N5) |
| KB-18: con vừa thành con hiện sau khi bị dừng được chạy lại (điểm chặn C1) | Unit test | `PlatformBannersTest.shownAfterPaused_isResumed`: AdMob hiện, MAX `pauseCount` 1; AdMob `Failed`, `send()`, MAX `Loaded`: MAX `resumeCount` 1. Đối chứng: `pause()` khung trước khi MAX nạp thì MAX `resumeCount` 0 tới khi `resume()` khung, lúc đó 1. `PlatformBannersTest.singleChild_pausedWhileLoading_resumedThenLoaded_runs`: thứ tự `admob`, `send()`, `pause()`, `resume()`, AdMob `Loaded`: AdMob đang chạy (lần `resume()` cuối sau lần `pause()` cuối) |
| N1: vòng dừng giữa chừng thì vòng mới hỏi lại nền tảng thứ nhất | Unit test | `PlatformBannersTest.pauseAfterTryNext_resetsToFirst`: MAX lỗi (`TryNext`), `pause()` (cổng đóng trước lượt gửi AdMob), `resume()`, `send()` đầu gửi cho MAX. Đối chứng không `pause()`: `send()` gửi cho AdMob. Cùng ý với `destroy()` |
| KB-31: cả hai lỗi là một lần lỗi, thử lại từ nền tảng thứ nhất | Unit test | `PlatformBannersTest.bothFail_oneFailure_retryStartsFromFirst`: MAX lỗi (`TryNext`), AdMob lỗi (`Failed(3)`, một kết quả); `send()` kế tiếp gửi cho MAX |
| KB-31: MAX chưa sẵn sàng thì lượt đó sang AdMob, không gọi `loadAd` | Unit test | `PlatformBannersTest.maxNotReady_skipsMaxWithoutSending`: `maxReady = { false }`, `send()` phát `TryNext` và MAX `sendCount` 0; đối chứng `maxReady = { true }` thì MAX `sendCount` 1. Thứ tự chỉ `max` và chưa sẵn sàng: `Failed(-1)` |
| KB-31: làm mới của nền tảng đang hiện | Unit test | `PlatformBannersTest.refreshOfShown_passesThrough_otherIgnored`: sau khi AdMob hiện, AdMob `emitLoaded` phát `Loaded`, `emitFailed` phát `Failed`; MAX phát gì cũng bỏ qua |
| KB-18, KB-27: chỉ nền tảng đang hiện chạy, nền tảng kia dừng | Unit test | `PlatformBannersTest.pauseAll_resumeOnlyShown`: sau khi AdMob hiện, MAX `pauseCount` 1 (dừng khi AdMob có quảng cáo); `pause()` thì cả hai thêm 1; `resume()` thì chỉ AdMob `resumeCount` 1, MAX 0. `destroy()` hủy cả hai |
| Không nhảy chiều cao | Unit test | `PlatformBannersTest.frameHeight_isTallestOfPlatforms`: `[MAX, ADMOB]` với 50 và 62 là 62; `[MAX]` là 50; `[ADMOB]` là 62; MREC 250 và 250 là 250 |
| Khóa của khung | Unit test | `PlatformBannersTest.unitKey_nullWithoutPlatforms_changesWithOrderOrUnit` |
| B-02: `TryNext` gửi ngay qua `sendWhenAllowed` | Unit test | `BannerLoadsTest.tryNext_sendsAtOnce_noFailureCounted_noOnFailed`: giả phát `TryNext` sau lượt gửi đầu thì lượt gửi thứ hai cùng thời điểm ảo, `onFailed` 0 lần; sau đó `Failed` thì chờ 2 000 ms (lần lỗi thứ nhất, không phải thứ hai) |
| S-15, KB-27, L-072: lượt gửi của nền tảng thứ hai cũng đợi màn hiện và cổng | Unit test | `BannerLoadsTest.tryNext_waitsVisible_andAsksGate`: `TryNext` khi `visible` là `false` thì không gửi tới khi `true`; `mayRequestNow` trả `false` thì vòng dừng, không gửi, log `request blocked` |
| KB-29: MREC không co giữa hai nền tảng; co về 0 khi cả hai lỗi | Compose UI test (Robolectric) | `BannerSlotHostTest.mrec_twoPlatforms_firstFails_keeps250_bothFail_collapses`: `BannerSlotHost(kind = MEDIUM_RECTANGLE, newView = { OrderedBannerAdView(MAX giả, AdMob giả) })`; MAX lỗi: khung vẫn 250 dp; AdMob lỗi: khung 0 dp; AdMob nạp được ở lượt thử lại: 250 dp |
| Banner cố định hai nền tảng: khung giữ chiều cao, đổi nền tảng không đổi chiều cao | Compose UI test (Robolectric) | `BannerSlotHostTest.banner_twoPlatforms_firstFails_heightUnchanged_secondShown`: chiều cao khung đo trước khi gửi, sau MAX lỗi, sau AdMob nạp: cùng một số dp |
| L-061: đổi `ads_mediation_order` giữa chừng (nên sửa N6) | Compose UI test (Robolectric) | `BannerSlotHostTest.unitKeyChangesMidway_destroysBothChildren_newFrameAsksFirst`: MAX đang hiện, đổi khóa ghép (`unitId`) sang thứ tự `admob_max`: hai con cũ `destroyCount` 1, không gửi thêm; khung mới gửi đúng một lượt cho AdMob (nền tảng thứ nhất của thứ tự mới) |
| Thanh đáy chừa đúng chiều cao khung (nên sửa N3) | Compose UI test (Robolectric) | `BannerAdSizeTest.bottomBannerSlot_withMaxBannerUnit_reportsTallestHeight`: `testAdsController(maxConfig = MaxConfig("key", "banner=u1"))`, `BottomBannerSlot` ở 360 dp báo `max(chiều cao neo AdMob, maxBannerHeightDp(ANCHORED, 360))`. Đối chứng: `MaxConfig.NONE` báo chiều cao neo AdMob |
| KB-31: có khóa MAX mà thứ tự `admob` thì không dựng `MaxAdView` (nên sửa N4) | Compose UI test (Robolectric) | `BannerAdSizeTest.admobOrder_withMaxKey_noMaxView_heightIsAdmob`: `testAdsController(maxConfig = MaxConfig("key", "banner=u1"))`, khóa `ads_mediation_order = admob` của `FakeRemoteSettings`, cổng mở: cây view không có `MaxAdView`, `onHeight` bằng chiều cao neo AdMob; đối chứng `max_admob` thì `onHeight` là chiều cao lớn nhất |
| KB-27 với MAX: bị che thì dừng tự làm mới của MAX, đóng thì chạy lại | Compose UI test (Robolectric) | `BannerSlotHostTest.covered_maxShown_stopsAutoRefresh_uncovered_starts`: `OrderedBannerAdView(MaxBannerAdView(FakeMaxBannerSdkView), AdMob giả)`, MAX nạp được; `covered` bật: `stopAutoRefresh` thêm 1, không `loadAd` mới; tắt: `startAutoRefresh` 1 |
| B-06 với MAX: rút đồng ý giữ khung, ẩn, dừng; đồng ý lại tải lại trong khung | Compose UI test (Robolectric) | `BannerSlotHostTest.maxShown_revoked_keepsHeight_hides_reloadsFromFirstWhenGranted`: sau rút: khung giữ chiều cao, thẻ quảng cáo không còn, `stopAutoRefresh` thêm 1; cổng mở lại: một lượt `loadAd` mới của MAX |
| B-05: màn không hiện thì MAX không gửi | Compose UI test (Robolectric) | `BannerSlotHostTest.hiddenScreen_maxFirst_noLoadUntilVisible` |
| Bọc `MaxAdView`: callback thành kết quả, dừng là `stopAutoRefresh` | Unit test (Robolectric) | `MaxBannerAdViewTest.callbacks_becomeResults`, `pauseResume_mapToAutoRefresh`, `send_callsLoadAd`, `destroy_destroysSdkView`, `logs_neverContainUnitId` (mã `"unit-secret"` không có trong mọi dòng của `RecordingAppLogger`; đối chứng: dòng log có `placementKey`) |
| Bọc `MaxAdView` thật dựng được trong JVM | Unit test (Robolectric) | `MaxBannerAdViewTest.realView_buildsForBannerAndMrec_rejectsCollapsible`: `AppLovinMaxBannerView` cho `ANCHORED`, `MEDIUM_RECTANGLE` không ném lỗi, `LARGE_ANCHORED` ném `IllegalArgumentException`; `maxBannerHeightDp(ANCHORED, 360)` lớn hơn 0, `MEDIUM_RECTANGLE` là 250. Không gọi `loadAd` (không mạng) |
| Mã đơn vị MAX theo khe | Unit test | `MaxSdkStarterTest.maxConfig_unitFor_bannerAndMrecOnly_noKeyNoUnit`: `"banner=a1;banner_mrec=b2;interstitial=c3"` cho `BANNER` là `a1`, `BANNER_MREC` là `b2`, `INTERSTITIAL` `null`, `BANNER_COLLAPSIBLE` `null`; mục sai dạng (`"banner"`, `"=x"`, `"banner_mrec="`) bỏ qua; khóa SDK rỗng thì mọi định dạng `null` (đối chứng: cùng chuỗi với khóa là `a1`); `toString` không chứa `a1`, `b2` |
| `AdsController` đọc mã MAX; log thiếu mã | Unit test | `AdsControllerMaxTest.maxUnitId_readsConfig`, `start_keyWithoutBannerUnits_logsMissingOnce_noUnitInLog`; đối chứng: không khóa thì không log dòng đó |
| Mã MAX không vào repo, staging chỉ đọc `local.properties`, production tùy chọn | Unit test (đọc `app/build.gradle.kts`) | `ProductionPropertiesTest.maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional`: khối `staging` có `getProperty("ads.maxAdUnits")`, `.trim()`, `Regex("[A-Za-z0-9_=;-]*")`, `"MAX_AD_UNITS"`, không có `gradleProperty("maxAdUnits")`; khối `production` có `gradleProperty("maxAdUnits")`, không có `ads.maxAdUnits`; `missingProductionProperties` không có `maxAdUnits` |
| Banner, MREC của MAX trên máy có khóa và mã MAX | Kiểm tra thủ công (máy thật: quảng cáo thật; cần khóa SDK MAX và mã đơn vị MAX thật trong `local.properties`, CI và emulator không có) | Dev đặt `ads.maxSdkKey`, `ads.maxAdUnits=banner=...;banner_mrec=...` trong `local.properties`, cài staging, mở màn xem thử: log `VGL.Ads` có `Banner ... MAX loaded`; bật chế độ máy bay giữa chừng rồi tắt; mở native toàn màn hình: log không có lượt nạp MAX trong lúc che; đặt `ads_mediation_order = admob_max` bằng giá trị cục bộ của staging: banner của AdMob hiện trước |
| Ảnh chuẩn không đổi | Screenshot test (có sẵn) | Không ghi lại ảnh nào: test và màn xem thử không có `AdsController` nên `AdSlot` không vẽ gì; `git status --short -- '*/src/test/screenshots/'` phải rỗng |

**Ngoại lệ viết test trước:** chỉ lượt nạp thật của `AppLovinMaxBannerView` trong `BannerSlot` (gọi `loadAd()` của SDK thật, cần mạng và khóa thật). Phần nối còn lại của `BannerSlot` có test viết trước qua `testAdsController` (chiều cao báo cho thanh đáy, thứ tự `admob` không dựng `MaxAdView`, `BannerAdSizeTest`); mọi lượt gửi đi qua `runBannerLoads` (L-072). Tài liệu (`ads-business-rules.md`, `ads-guide.md`) và chú thích tham số của `app/build.gradle.kts`.

### 4.1. Chạy thử

- **Đã chạy thử:** test Robolectric tạm `MaxAdViewTrialTest` (không commit) trong `:core:ads`, worktree `../try-T-071` từ nhánh chính `0bd30d9`, 2026-10-30, SDK MAX chưa khởi tạo (như mọi test JVM): đọc `MaxAdFormat.BANNER.getAdaptiveSize(360, context)` và `MaxAdFormat.MREC.size`; dựng `MaxAdView` cho banner và MREC, gắn `MaxAdViewAdListener`, `setExtraParameter`, `stopAutoRefresh`, `startAutoRefresh`, `destroy`. Không gọi `loadAd` (không mạng). Hai lần chạy:
  1. Banner bằng `MaxAdView("trial-unit", MaxAdFormat.BANNER, MaxAdViewConfiguration(ANCHORED, 360))`.
  2. Banner bằng `MaxAdView("trial-unit", MaxAdFormat.BANNER, activity)` kèm `adaptive_banner=true`, `adaptive_banner_width=360`; MREC bằng `MaxAdView("trial-unit-mrec", MaxAdFormat.MREC, activity)`.
- **Lệnh:** `export ANDROID_HOME=$HOME/android-sdk; scripts/check.sh` (mỗi lần chạy trọn).
- **Kết quả:**
  - Lần 1: `EXIT=1`, `1290 tests completed, 1 failed`: chỉ test tạm đỏ, `NullPointerException: Cannot invoke "android.content.Context.getResources()" because "context" is null` trong `View.<init>` từ `RelativeLayout.<init>`: hàm dựng có `MaxAdViewConfiguration` không nhận `Context`, lấy context từ SDK MAX, nên ném lỗi khi SDK chưa khởi tạo. Số đọc được trước lỗi: banner thích ứng của MAX ở 360 dp là 360 x 56, MREC 300 x 250.
  - Lần 2: `EXIT=0`, `BUILD SUCCESSFUL`, `scripts/check.sh` qua toàn bộ (5 phút 39 giây); test tạm in `TRIAL views built: MaxAdFormat{label='BANNER'} MaxAdFormat{label='MREC'}`, cùng số đo 360 x 56 và 300 x 250. Vì vậy mục 3.1 bước 2 dùng hàm dựng có `Context` và tham số phụ `adaptive_banner`; hàm dựng có `MaxAdViewConfiguration` bị loại (mục 7). Cỡ thích ứng trên máy thật và lượt nạp thật chưa kiểm được (phương án dự phòng, mục 3.4; kiểm thủ công, mục 4).
  - Không chạy `./gradlew :app:assembleStagingRelease`: plan không thêm thư viện hay module (L-083); `MaxAdView` thuộc `applovin-sdk` mà T-068 đã build release qua R8.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thêm nguồn MAX cho định dạng banner, MREC), 7 (module core `:core:ads`, cấu hình build `app/build.gradle.kts`). Không chạm 2 (SDK MAX đã có từ T-068, không nâng), 3 (không quyền), 4 (không sự kiện mới; số liệu MAX để T-072), 5 (không đổi thứ tự khởi tạo hay đồng ý).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads` và `:app` (cấu hình build, Hilt), cùng tài liệu luật |
| B. Độ rõ của yêu cầu | 1 | KB-31, KB-18, KB-29, KB-27 đã chốt; AI tự quyết dạng mã đơn vị MAX, chiều cao khung và MAX chưa sẵn sàng (câu hỏi 1, 4, 5) |
| C. Khả năng kiểm chứng tự động | 2 | Luật có test trên bản giả; quảng cáo MAX thật chỉ kiểm được trên máy có khóa và mã thật |
| D. Ảnh hưởng chéo | 3 | Mọi màn có banner cố định, MREC, thanh đáy đi qua khung mới; T-069, T-070 sẽ dùng cùng `MaxConfig.unitFor` |
| E. Độ mới của kỹ thuật | 2 | `MaxAdView` lần đầu dùng trong dự án; chạy thử ở mục 4.1 |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdSlot.kt` (`BannerSlot` của mọi banner và lớp banner của thanh đáy), `BannerLoads.kt`, `MaxSdk.kt`, `AdsController.kt`, `app/build.gradle.kts`, `ads-business-rules.md`, `ads-guide.md`.
- Task khác có thể xung đột:
  - T-069 (inter, rewarded của MAX), T-070 (app open của MAX): chưa có plan; sẽ mở rộng `MaxConfig.unitFor` cho `interstitial`, `rewarded`, `app_open` theo cùng `-PmaxAdUnits` (plan này tạo, plan kia ghi Sửa, L-054).
  - T-072 (doanh thu, phễu của MAX): gắn `setRevenueListener` và số liệu nạp vào `MaxBannerAdView` (plan này tạo). Trước khi phát hành bản có mã MAX, T-072 phải merge, không thì banner MAX không có doanh thu trong Firebase và AppsFlyer (câu hỏi 2).
  - T-073 (người làm): `release.sh` truyền và kiểm `maxAdUnits`, mẫu mã đơn vị MAX trong `secret-scan.py`, viết lại `ProductionPropertiesTest` khi bắt buộc (câu hỏi 3).
- Đổi `ads_mediation_order` khi Remote Config tải xong (giá trị khác mặc định) dựng lại khung đang hiện: quảng cáo đang có bị hủy, khung gửi lại một yêu cầu, có thể thấy nháy (rà soát, G4; câu hỏi 7).
- Đề xuất thứ tự chạy: sau T-062, T-067, T-054, T-068 (đã merge). Trước T-072. Độc lập với T-069 về file code ngoài `MaxSdk.kt` và `AdsController.kt`: cùng danh sách chạy thì T-071 trước, T-069 dùng lại `MaxConfig.adUnits`.

## 7. Điều kiện dừng riêng

- Hàm dựng `MaxAdView(unitId, format, context)` ném lỗi trong JVM hay trên máy: dừng, không bỏ test `MaxBannerAdViewTest.realView_buildsForBannerAndMrec_rejectsCollapsible`, không chuyển sang hàm dựng có `MaxAdViewConfiguration` (ném lỗi khi SDK chưa khởi tạo, mục 4.1).
- Cần thêm sự kiện phân tích, tham số số liệu hay `setRevenueListener` để test qua: dừng (điểm chặn 4, việc của T-072).
- Ảnh chuẩn nào đổi: dừng (plan không ghi lại ảnh).
- Cần đổi chữ ký `BannerSlotHost` hay hành vi của banner AdMob khi thứ tự là `admob`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Dạng mã đơn vị MAX.** Ticket và ADR-0037 nói "mã đơn vị MAX theo khe" nhưng chưa có dạng tham số; T-073 (người làm) nhận phần `release.sh`. Đề xuất: plan này đặt `-PmaxAdUnits="banner=<mã>;banner_mrec=<mã>"` (một mã mỗi định dạng của app, như JBase) và `ads.maxAdUnits` trong `local.properties` ở staging, tùy chọn ở production như `maxSdkKey`; T-069, T-070 thêm khe `interstitial`, `rewarded`, `app_open` vào cùng tham số; T-073 cho `release.sh` truyền và kiểm. Phương án khác: mã MAX theo vị trí như `adUnits` của AdMob (mỗi vị trí một mã MAX).
2. **Số liệu của MAX trong T-071.** Đề xuất: T-071 không ghi số liệu nạp hay doanh thu cho lượt của MAX (không chạm điểm chặn 4); T-072 gắn sau, và T-072 merge trước bản phát hành đầu có mã MAX. Phương án khác: T-071 ghi luôn `LoadSuccess`/`LoadFail` của MAX bằng sự kiện có sẵn, nhưng mã lỗi của MAX khác AdMob (`loadStateAfterError` đọc mã AdMob), nên cần quyết cách quy đổi.
3. **Mẫu mã đơn vị MAX trong `secret-scan.py`.** Plan này đưa mã MAX vào `local.properties` và tham số build nhưng không thêm mẫu chặn (T-073 nhận, theo ticket T-073). Đề xuất giữ ở T-073; người duyệt muốn chặn ngay thì thêm `scripts/secret-scan.py` và `scripts/tests/run.sh` vào mục 3.2.
4. **Khung hai nền tảng cao bằng nền tảng cao nhất.** Cỡ neo đáy thường của AdMob và cỡ `ANCHORED` của MAX có thể khác vài dp; plan giữ khung cao bằng số lớn hơn để không nhảy, quảng cáo thấp hơn nằm trên cùng khung (căn trên như hôm nay), có thể chừa một dải trống nhỏ. Đề xuất chấp nhận; phương án khác: căn giữa theo chiều dọc.
5. **MAX chưa sẵn sàng.** MAX khởi tạo sau AdMob (T-068), nên lượt đầu của banner có thể tới trước `maxReady`. Đề xuất: lượt đó bỏ qua MAX và hỏi AdMob ngay (thứ tự `max` thì là một lần lỗi, thử lại theo B-02); khung có quảng cáo AdMob giữ AdMob tới lượt gửi kế tiếp (lỗi, thu hồi đồng ý, màn mới), không tự chuyển sang MAX. Phương án khác: chờ `maxReady` có giới hạn trước lượt đầu. Lưu ý (rà soát, G3): ADR-0037 ghi "nền tảng có trong thứ tự thì được nạp sẵn", còn plan hỏi lần lượt với banner (không nạp sẵn banner); hệ quả: với thứ tự mặc định `max_admob`, khung mở trước `maxReady` giữ AdMob tới lần lỗi hay lần dựng khung kế tiếp. Cần người xác nhận cách hiểu này với ADR-0037.
6. **Rà soát độc lập trước khi duyệt (2026-10-30):** 1 chặn, 6 nên sửa, 6 ghi chú. Đã sửa: C1 (con vừa thành con hiện sau khi bị dừng được `resume()`, cờ `running`, test `shownAfterPaused_isResumed`, `singleChild_pausedWhileLoading_resumedThenLoaded_runs`), N1 (`pause()`, `destroy()` đặt `next = 0`, test `pauseAfterTryNext_resetsToFirst`), N2 (con lỗi bị `pause()`, câu khẳng định `pauseCount` 1, liệt kê đường gửi của MAX ở bước 3), N3 (`onHeight` theo `bannerFrameHeightDp`, `BannerAdSizeTest.kt` vào mục 3.2 và 3.5), N4 (thu hẹp ngoại lệ viết test trước, thêm test thứ tự `admob` có khóa MAX), N5 (`FakeBannerAdView.kt` vào mục 3.2, phần vẽ chuyển sang `BannerSlotHostTest`), N6 (test đổi khóa ghép giữa chừng). Ghi chú G3 ở câu hỏi 5, G4 ở mục 6 và câu hỏi 7. G5, G6: không cần làm gì. Ghi chú còn để người xem:
   - G1: `MaxBannerAdViewTest.logs_neverContainUnitId` sẽ qua ngay vì `MaxBannerAdView` không nhận mã; câu kiểm mã không lọt log thật nằm ở `AdsControllerMaxTest.start_keyWithoutBannerUnits_logsMissingOnce_noUnitInLog` và `toString` của `MaxConfig` (`MaxSdkStarterTest.maxConfig_unitFor_bannerAndMrecOnly_noKeyNoUnit`).
   - G2: `MaxAdView` được dựng khi khung mở, có thể trước `maxReady`; mục 4.1 chỉ chứng minh việc dựng trong JVM (SDK chưa khởi tạo), chưa chứng minh trên máy. Phương án cân nhắc: dựng con MAX lười ở lượt gửi MAX đầu khi `maxReady`. Chưa chạy thử trên máy; kiểm thủ công thêm: gọi `loadAd()` khi MAX đang tự làm mới. Người duyệt chọn giữ cách của mục 3.1 hay đổi sang dựng lười.
7. **Đổi `ads_mediation_order` giữa phiên (G4):** khi Remote Config tải xong với thứ tự khác mặc định, khung banner đang hiện bị dựng lại (thêm một yêu cầu, có thể nháy). Đề xuất chấp nhận (chỉ xảy ra một lần mỗi lần mở app khi giá trị đổi); phương án khác: chỉ áp thứ tự mới cho khung dựng sau đó.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
