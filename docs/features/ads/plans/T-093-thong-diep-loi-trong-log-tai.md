---
ticket: T-093
title: Thông điệp lỗi của SDK trong log lỗi tải quảng cáo (chỉ staging)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 24.1M        # cỡ M, số gợi ý 24.1M của scripts/token-usage.py estimate (2026-11-07, trung vị 24 task M); không chỉnh
blocking_points: [core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-093: THÔNG ĐIỆP LỖI CỦA SDK TRONG LOG LỖI TẢI QUẢNG CÁO (CHỈ STAGING)

## 1. Tóm tắt

Ở bản staging, mỗi lượt tải quảng cáo mà SDK trả lỗi có một dòng log lỗi tải mang cả mã lỗi và thông điệp lỗi của SDK, dạng `<mã> "<thông điệp>"`, sau khi che chuỗi dạng mã đơn vị (AdMob, MAX), gộp thành một dòng và cắt ở 300 ký tự. Một lớp mới `LoadErrorLog` của `:core:ads` làm việc này, bật theo cờ mới `AdsDebugOptions.logLoadErrorMessages` (staging bật, production tắt, như log từng mạng); bản production giữ đúng dòng log chỉ có mã như hôm nay, thông điệp không vào sự kiện phân tích hay log từng mạng. Kiểm bằng test JVM và Robolectric với `RecordingAppLogger` cho từng định dạng (native mọi đường, native toàn màn hình, interstitial, app open, rewarded, banner AdMob mọi loại, banner và MREC của MAX), một test quét mã nguồn cho các đường nằm trong callback của SDK, và một lượt đọc logcat trên máy.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-28 | **Thêm 2026-11-07:** khi SDK trả lỗi cho một lượt tải quảng cáo ở bản staging thì dòng log lỗi tải có cả mã lỗi và thông điệp lỗi của SDK, ở mọi định dạng | "Lượt SDK trả lỗi" là lượt có callback lỗi tải của SDK: `onAdFailedToLoad(LoadAdError)` của AdMob (native, native toàn màn hình, interstitial, app open, rewarded, banner kể cả lượt SDK tự làm mới) và `onAdLoadFailed(String, MaxError)` của MAX (banner, MREC). "Dòng log lỗi tải" là dòng đầu tiên ghi mã lỗi của lượt đó; mỗi lượt lỗi có đúng một dòng như vậy mang thông điệp (bảng ở mục 3.1). Lượt không do SDK trả lỗi giữ nguyên (câu trả lời 58): `failed: 0 (request not built)`, dòng chỉ ghi tên lớp ngoại lệ, hết giờ chờ, dòng lỗi lúc hiện (`failed to show`) |
| KB-28 | chuỗi có dạng mã đơn vị trong thông điệp được thay bằng dấu che trước khi ghi | Che theo dạng, không theo mã cụ thể: mã AdMob (`ca-app-pub-<số>`, kèm `/<số>` hay `~<số>`, và `pub-<số>`) và mã đơn vị MAX (chuỗi hex từ 16 ký tự, đứng riêng). Dấu che là `<ad unit>`. Thông điệp cũng được gộp về một dòng và cắt ở 300 ký tự, để một thông điệp dài không làm vỡ logcat; câu hỏi 1 |
| KB-28 | Bản production giữ dòng log chỉ có mã | Production (`AdsDebugOptions.NONE`) không đọc thông điệp vào log: mọi dòng giống từng ký tự với hôm nay. Mọi bộ tải dựng không có `LoadErrorLog` cũng chỉ có mã (mặc định `CODE_ONLY`) |
| KB-28 | Thông điệp không vào sự kiện phân tích | `ad_load_fail` giữ `load_error_code`, `load_state` như hôm nay (`docs/ops/ads-business-rules.md`, mục 8.2). `AdTelemetry.loadFailed` vẫn nhận thông điệp gốc chỉ để nhận ra lỗi tự chặn của SDK, không đổi. Log từng mạng (`AdNetworkLog`) vẫn không đọc thông điệp: thông điệp chỉ ghi ở dòng lỗi tải |
| KB-28 (tiêu chí có sẵn) | Mỗi lượt tải quảng cáo ở staging ghi log từng mạng đã thử, không ghi mã đơn vị | Giữ nguyên; test có sẵn `AdNetworkResponsesTest`, `AdNetworkLogWiringTest` không đổi câu khẳng định |

KB-9 (vừa đổi cùng ngày, câu hỏi 57) không thuộc ticket này: kiểm adapter sau R8 không có việc tự động (câu trả lời 57). Plan không thêm hay bỏ mạng mediation, không nâng SDK, nên không phải kiểm lại danh sách lớp trong APK.

Thiết kế tham chiếu: Không có thiết kế riêng (thay đổi chỉ ở log, docs/ops/design-workflow.md).

ADR liên quan: ADR-0019 (log của app: thẻ, mức, không ghi dữ liệu cá nhân; Crashlytics chỉ nhận tên loại lỗi), ADR-0045 (quảng cáo theo AdsKit PARTNER; mã thử của Google trên staging, mục 5.1), ADR-0012 (staging dùng Firebase giả, không gửi số liệu), ADR-0029, ADR-0031.

Phản hồi: FB-20261023-02 (native thử lỗi mã 0 trên emulator, log chỉ có mã). Đối chiếu đã làm trên code hiện tại ở mục 4 (chạy thử).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu của log từng mạng (`AdNetworkLog`, plan T-058): một lớp có cờ bật, dựng một lần trong `AdsController` từ `AdsDebugOptions`, truyền vào các bộ tải; mặc định tắt, nên mọi bộ tải dựng thiếu nó ghi như production. Phần thuần (định dạng, che, gộp dòng, cắt) nằm trong một file, có test JVM.

1. **`LoadErrorLog.kt` (mới):**
   - `internal class LoadErrorLog(private val withMessage: Boolean)` với `companion object { val CODE_ONLY = LoadErrorLog(false) }`.
   - `fun messageOf(sdkMessage: String?): String?`: `null` khi `withMessage` tắt, hay thông điệp `null` hoặc trống (chỉ khoảng trắng); còn lại `logSafeMessage(sdkMessage)`.
   - `fun text(code: Int, sdkMessage: String?): String = loadErrorText(code, messageOf(sdkMessage))`.
   - `internal fun loadErrorText(code: Int, logMessage: String?): String`: `"$code"` khi `logMessage` là `null`, không thì `"$code \"$logMessage\""`.
   - `internal fun logSafeMessage(message: String): String`, theo thứ tự: (a) che: mọi khớp của `(?<![0-9A-Za-z])(?:ca-app-)?pub-[0-9]{10,}(?:[~/][0-9]+)?` và của `(?<![0-9A-Za-z])[0-9A-Fa-f]{16,}(?![0-9A-Za-z])` thay bằng `AD_UNIT_MASK = "<ad unit>"`; (b) mọi dãy khoảng trắng (kể cả xuống dòng, tab) thành một dấu cách, bỏ khoảng trắng hai đầu; (c) dài hơn `MAX_MESSAGE_CHARS = 300` thì giữ 300 ký tự đầu và thêm `...`. Che trước khi cắt, nên phần cắt không để lại nửa mã.
   - KDoc nói rõ: chỉ dùng cho dòng lỗi tải (KB-28, câu trả lời 58); không dùng cho log từng mạng, số liệu hay Crashlytics của production.
2. **`AdsDebugOptions.kt`:** thêm trường `val logLoadErrorMessages: Boolean = false` (cuối danh sách, có mặc định, nên `NONE` và chỗ dựng khác không đổi); `staging(raw)` đặt `logLoadErrorMessages = true`. KDoc của lớp thêm một câu. `:app` không đổi: staging đã dựng bằng `AdsDebugOptions.staging(...)`, production bằng `NONE`.
3. **`AdsController.kt`:**
   - `internal val loadErrorLog = LoadErrorLog(debugOptions.logLoadErrorMessages)` cạnh `networkLog`.
   - Bốn dòng lỗi tải trong `onAdFailedToLoad(error: LoadAdError)`: `Native full-screen ${placement.key} failed to load: ${loadErrorLog.text(error.code, error.message)}`, tương tự `Interstitial ...`, `App open ...`, `Rewarded ...`. Dòng `failed to load: 0 (request not built)` giữ nguyên.
   - `newNativeLoader`: `SdkNativeAdLoader(..., networkLog = networkLog, logKey = logKey, loadErrorLog = loadErrorLog)`.
4. **Native trong màn hình (`NativeAdLoading.kt`, `SdkNativeAdLoader.kt`):**
   - `NativeAdLoader.load(..., onFailed: (code: Int, logMessage: String?) -> Unit)`; KDoc: `logMessage` đã an toàn để ghi log (đã che, hay `null`).
   - `SdkNativeAdLoader` thêm tham số `loadErrorLog: LoadErrorLog = LoadErrorLog.CODE_ONLY`, truyền vào `NativeAdLoadListener`. `NativeAdLoadListener(events, onLoadFailedResponse, loadErrorLog = CODE_ONLY, onFailed: (Int, String?) -> Unit)` (giữ `onFailed` cuối để test có sẵn dùng lambda cuối); `onAdFailedToLoad`: `onLoadFailedResponse(error)`, `events?.onFailed(error.code, error.message)` (giữ: số liệu nhận thông điệp gốc như hôm nay), rồi `onFailed(error.code, loadErrorLog.messageOf(error.message))`.
   - `NativeAdRequest.onFailed(code, logMessage)` ghi `Native $placementKey failed: ${loadErrorText(code, logMessage)}`.
   - Mọi lượt tải native trong màn hình đi qua đúng hai chỗ này: `loadOneNativeAd` (gọi từ `nativeLoadOnce`) và `SdkNativeAdLoader` dựng duy nhất trong `newNativeLoader` (test có sẵn `AdNetworkLogWiringTest.everyLoaderInMain_getsTheNetworkLog` giữ điều này). Nên lượt đầu của khe, thử lại (`NativeAdRetry`), làm mới (`NativeAdRefresh` qua kho), kho của định dạng (`NativeAdPool`, khóa `pool`), kho nạp sẵn (`NativeAdSources`), trong danh sách, lớp thu gọn, thẻ của luồng bước đều có dòng `Native <khóa> failed: <mã> "<thông điệp>"`. Dòng `Pool ... failed, retry in ...` và `Native ... refresh: failed, retry in ...` không có mã hôm nay và đứng sau dòng `Native <khóa> failed` của cùng lượt: không đổi.
5. **Banner AdMob (`BannerLoads.kt`, `BannerAdView.kt`):**
   - `BannerLoadResult.Failed(val code: Int, val logMessage: String? = null)`.
   - `SdkBannerAdView` thêm tham số `loadErrorLog: LoadErrorLog = LoadErrorLog.CODE_ONLY`; `onAdFailedToLoad` phát `Failed(error.code, loadErrorLog.messageOf(error.message))`. Cùng listener nhận lượt gửi của `:core:ads` và lượt SDK tự làm mới.
   - `runBannerLoads`: dòng thử lại (dòng duy nhất có mã của lỗi banner AdMob) thành `Banner $placementKey retry in $wait ms (failures $failures, code ${loadErrorText(result.code, result.logMessage)})`.
   - `PlatformBanners.kt` (`OrderedBannerAdView`) không sửa: kết quả `Failed` của nền tảng cuối đi qua nguyên đối tượng (`answer` trả `result`), nên thông điệp tới `runBannerLoads`; lượt bỏ qua MAX chưa sẵn sàng (`Failed(MAX_NOT_READY)`) không phải lỗi của SDK, giữ không có thông điệp. Dòng `Banner <khóa> ADMOB no ad, trying MAX` không có mã hôm nay: giữ (câu hỏi 3).
6. **Banner và MREC của MAX (`MaxBannerAdView.kt`):**
   - `MaxBannerSdkView.setCallbacks(onLoaded, onFailed: (code: Int, message: String?) -> Unit, onClicked)`; `AppLovinMaxBannerView`: `onAdLoadFailed(adUnitId: String, error: MaxError) = onFailed(error.code, error.message)` (thông điệp gốc, chưa che, chỉ đi tới `MaxBannerAdView`).
   - `MaxBannerAdView` thêm tham số `loadErrorLog: LoadErrorLog = LoadErrorLog.CODE_ONLY`; `onFailed(code, message)` ghi `Banner $placementKey MAX failed (code ${loadErrorLog.text(code, message)})`; `telemetry.maxLoadFailed(...)` không đổi; vẫn phát `Failed(code)` không có thông điệp, để dòng thử lại của `runBannerLoads` không lặp lại thông điệp đã ghi ở dòng MAX (câu hỏi 4). KDoc "no log line can hold it" thêm vế: thông điệp lỗi của MAX chỉ vào log qua `LoadErrorLog` (đã che).
7. **`AdSlot.kt`:** hai chỗ dựng trong khung banner truyền `loadErrorLog = ads.loadErrorLog` cho `SdkBannerAdView` và `MaxBannerAdView`.
8. **Không sửa (đối chiếu với danh sách của ticket):**
   - `FullScreenReloads.kt`: dòng `Retry <khóa> in <ms> ms (failures <n>, code <mã>)` đứng ngay sau dòng `failed to load` của cùng lượt trong `AdsController`, nên thông điệp đã có ở dòng lỗi tải; thêm nữa phải truyền thông điệp qua `onLoadFailed` của cả bốn đường mà không thêm thông tin (câu hỏi 3).
   - `NativeAdRefresh.kt`, `NativeAdPool.kt`, `NativeAdRetry.kt`: dòng của chúng không có mã lỗi của SDK (xem bước 4); dòng `failed: 0 (request not built)` và `failed: 0 (<tên lớp>)` giữ theo phạm vi của ticket.
   - `AdNetworkResponses.kt`, `AdTelemetry.kt`, `AdTelemetryRules.kt`: không đổi; thông điệp không qua `AdNetworkLog`, không vào sự kiện.
9. **Tài liệu:** `docs/ops/ads-guide.md`, mục 7 "Thử mediation trên máy thật", thêm một gạch đầu dòng sau "Log từng mạng": "Thông điệp lỗi" (ở staging, dòng lỗi tải của mọi định dạng có `<mã> "<thông điệp>"`, ví dụ `Native sample_native_small failed: 0 "Incorrect native ad response. Click actions were not properly specified"`; mã đơn vị trong thông điệp thành `<ad unit>`; gộp một dòng, tối đa 300 ký tự; dòng `request not built` và dòng ngoại lệ không có; bản production chỉ có mã; thông điệp không vào số liệu hay log từng mạng). Không đổi `ads-business-rules.md` (mục 8.2 và 13 vẫn đúng), không đổi ADR-0019 (câu trả lời 58).

Bảng các dòng lỗi tải sau thay đổi (staging; production bỏ phần `"<thông điệp>"`):

| Định dạng, đường | Dòng mang thông điệp | Nơi ghi |
| --- | --- | --- |
| Native trong màn hình: lượt đầu, thử lại, làm mới, kho định dạng, kho nạp sẵn, trong danh sách, lớp thu gọn, thẻ luồng bước | `Native <khóa> failed: <mã> "<thông điệp>"` | `NativeAdRequest.onFailed` (`loadOneNativeAd`) |
| Native toàn màn hình: nạp sẵn, thử lại, nạp lại | `Native full-screen <vị trí> failed to load: <mã> "<thông điệp>"` | `AdsController`, callback của `AdLoader` |
| Interstitial, app open, rewarded: nạp sẵn, thử lại, nạp lại | `<Định dạng> <vị trí> failed to load: <mã> "<thông điệp>"` | `AdsController`, callback `onAdFailedToLoad` |
| Banner AdMob (cố định, MREC, thu gọn, lớp banner của thanh đáy): lượt gửi, thử lại, nạp lại, lượt SDK tự làm mới | `Banner <khóa> retry in <ms> ms (failures <n>, code <mã> "<thông điệp>")` | `runBannerLoads`, từ `Failed` của `SdkBannerAdView` |
| Banner, MREC của MAX | `Banner <khóa> MAX failed (code <mã> "<thông điệp>")` | `MaxBannerAdView.onFailed` |

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Gốc mã chính: `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/`; gốc test: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/`. Đường dẫn trong bảng ghi đầy đủ.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/LoadErrorLog.kt` | `LoadErrorLog`, `loadErrorText`, `logSafeMessage`, hằng `AD_UNIT_MASK`, `MAX_MESSAGE_CHARS` (bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsDebugOptions.kt` | Trường `logLoadErrorMessages`, `staging()` bật (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `loadErrorLog`; bốn dòng `failed to load`; `newNativeLoader` truyền `loadErrorLog` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdLoading.kt` | Chữ ký `onFailed` của `NativeAdLoader`; dòng `Native <khóa> failed` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | Tham số `loadErrorLog` của `SdkNativeAdLoader` và `NativeAdLoadListener`; `onFailed(code, logMessage)` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerLoads.kt` | `Failed.logMessage`; dòng thử lại (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerAdView.kt` | Tham số `loadErrorLog` của `SdkBannerAdView`; `Failed(code, logMessage)` (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxBannerAdView.kt` | Chữ ký `onFailed` của `MaxBannerSdkView`; `AppLovinMaxBannerView` truyền `error.message`; `MaxBannerAdView` có `loadErrorLog`, dòng `MAX failed`; KDoc (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | Truyền `loadErrorLog = ads.loadErrorLog` cho `SdkBannerAdView`, `MaxBannerAdView` (bước 7) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadErrorLogTest.kt` | Test JVM của phần thuần (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerLoadErrorLogTest.kt` | Robolectric: `AdsController` dựng `loadErrorLog` theo `AdsDebugOptions` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsDebugOptionsTest.kt` | Thêm `loadErrorMessages_stagingOnly` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdLoadingTest.kt` | Thêm hai test dòng `Native <khóa> failed` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdLoadListenerTest.kt` | Thêm hai test; phần dựng của ba test có sẵn theo chữ ký `onFailed` hai tham số (`{ _, _ -> }`, `{ code, _ -> order += "failed:$code" }`), câu khẳng định giữ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/SdkBannerAdViewTest.kt` | Thêm `loadFailed_staging_resultCarriesMaskedMessage_networkLinesDoNot` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerLoadsTest.kt` | Thêm `failedWithMessage_retryLineHasCodeAndMessage` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PlatformBannersTest.kt` | Thêm `lastPlatformFailed_keepsMessage` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxBannerAdViewTest.kt` | Hàm dựng `banner(...)` thêm tham số `loadErrorLog` (mặc định `CODE_ONLY`); phần dựng của `realView_buildsForBannerAndMrec_rejectsCollapsible` (dòng 268) theo chữ ký mới; thêm hai test dòng `MAX failed` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/di/StagingAdsModuleTest.kt` | Thêm `adsDebugOptions_logLoadErrorMessages`: `StagingAdsModule.adsDebugOptions().logLoadErrorMessages == true` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | Thêm `loadFailed_sdkMessage_neverInEvents` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkLogWiringTest.kt` | Thêm ba test quét: mọi callback lỗi tải của AdMob đi qua `loadErrorLog`; mọi chỗ dựng bộ tải mang `loadErrorLog =`; callback lỗi của MAX truyền `error.message`. KDoc của lớp thêm một câu; test và hàm phụ có sẵn không đổi |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | `FakeNativeAdLoader.Request.onFailed: (Int, String?) -> Unit`, `fail(code: Int, logMessage: String? = null)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeBannerAdView.kt` | `emitFailed(code: Int, logMessage: String? = null)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeMaxBannerSdkView.kt` | Kiểu `onFailed: (Int, String?) -> Unit`; `emitFailed(code: Int, message: String? = null)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsStateTest.kt` | Phần dựng: `NativeAdLoader { _, _, _, onFailed -> onFailed(3, null) }` (dòng 56), câu khẳng định giữ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourcesTest.kt` | Phần dựng của bộ tải bọc (dòng 266 tới 279): lambda lỗi nhận `(code, message)`, gọi `report.failed(code, null, ...)`, `onFailed(code, message)`; câu khẳng định giữ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInListRowTest.kt` | Phần dựng: `ThrowingNativeAdLoader.load` theo chữ ký mới (dòng 168) |
| Sửa | `docs/ops/ads-guide.md` | Mục 7, "Thử mediation trên máy thật": gạch đầu dòng "Thông điệp lỗi" (bước 9) |

Không có ảnh chuẩn screenshot nào đổi: plan không đổi giao diện. Nơi đọc các file bị sửa: chỉ mã và test của `:core:ads` (lớp `internal`); `:app` chỉ dùng `AdsDebugOptions.staging(...)` và `AdsDebugOptions.NONE` (`app/src/staging/.../StagingAdsModule.kt`, `app/src/production/.../ProductionAdsModule.kt`, test `StagingAdsModuleTest`, `AdsDebugOptionsSourceSetTest`): không đổi vì trường mới có mặc định và hai hàm dựng giữ tên. `grep -rn "AdsDebugOptions\|NativeAdLoader\|BannerLoadResult\|MaxBannerSdkView" scripts/ .github/` không có kết quả. `scripts/ads-plan.py` nhắc tới `docs/ops/ads-guide.md` chỉ trong chú thích và chữ dẫn nguồn (mục 3, mục 5), không đọc nội dung file; plan chỉ thêm một gạch đầu dòng ở mục 7, không đổi số mục, nên script không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Bật tắt theo biến thể qua `AdsDebugOptions` (đã có, plan T-058), không qua Remote Config.
- Quảng cáo: Không đổi vị trí, định dạng, tần suất, cách nạp hay hiện. Chỉ đổi chữ của dòng log lỗi tải ở staging.
- Sự kiện phân tích: Không chạm. `ad_load_fail` giữ tham số như hôm nay; test giữ ở mục 4.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp dùng: `LoadAdError.getMessage()` (`play-services-ads`, đã là phụ thuộc của `:core:ads`, trên classpath biên dịch, đã đọc ở `AdsController` và `SdkNativeAdLoader` hôm nay), `MaxError.getMessage()` (SDK AppLovin MAX `applovin-sdk`, đã là phụ thuộc của `:core:ads`, `MaxError` đã import ở `MaxBannerAdView.kt`), `kotlin.text.Regex` (thư viện chuẩn).

### 3.4. Phương án dự phòng

Không có. Không có kỹ thuật mới (E = 1); mọi lớp và API đã dùng trong module, đã chạy thử đọc `LoadAdError.message` trong test JVM (mục 4).

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-093-thong-diep-loi-trong-log-tai.md` (chạy trên nhánh plan, 2026-11-08) liệt kê 558 test dùng tên của file plan sửa; 9 test "Cần xem trước", 66 test "Nên xem". AI đã đọc từng test của hai nhóm:

- "Cần xem trước": `LoadingScreenTest.*` (sáu test) chỉ trùng tên chung `Failed` (trạng thái của màn tải, không phải `BannerLoadResult.Failed`); `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `NativeAdSlotTest.leavingComposition_destroysAd`, `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction` đo chiều cao hay vị trí, chỉ trùng tên `AdsController`, `nativeAdStates`. Plan không đổi giao diện: câu khẳng định giữ.
- "Nên xem": `PlatformBannersTest.*`, `BannerLoadsTest.*`, `SdkBannerAdViewTest.*`, `NativeAdLoadingTest.loadOne_*`, `NativeAdLoadListenerTest.*`, `NativeAdRetryTest.loaderNotBuilt_reportsCode0ToEventsAndRetriesAt5s`, `NativeAdRefreshTest.loaderThrows_countsAsCode0_keepsAd`, `NativeAdSourcesTest.twoSlotsOverlap_loadAttemptAndLatencyPerRequest`, `AdsControllerInListRowTest.inListRow_failedThenRetriesAt5s_listRowMetrics`, `AdsDebugOptionsTest.*`, `StagingAdsModuleTest.adsDebugOptions_readsBuildConfig_andLogsNetworks`, `AdsDebugOptionsSourceSetTest.production_providesNone`, `MaxBannerAdViewTest.realView_buildsForBannerAndMrec_rejectsCollapsible` đọc ở bảng dưới. Các test còn lại (`FullScreenReloadsTest`, `LoadAttemptCounterTest` dùng `onFailed` của bộ đếm, khác hàm; `AdInspectorTest`, `AdGalleryViewModelTest` dùng `AdInspectorResult.Failed`; các test `AdsController*`, `RewardedActionTest`, `FullScreenAdsTest`, `NativeEligibleCoverageTest`, `RemovedPacingRulesTest` chỉ dựng `AdsController`; `NativeRequestOptionsTest` chỉ tìm tên file `SdkNativeAdLoader.kt`; `BannerSlotHostTest.mrec_*` dựng `MaxBannerAdView` qua `AdSlot` với bản giả không có thông điệp; `LoadingScreenScreenshotTest` trùng tên `Failed`) không kiểm điều plan đổi: giữ.

Hành vi plan đổi chỉ có ở staging (dòng log có thêm thông điệp) và ở chữ ký nội bộ (`NativeAdLoader.onFailed`, `BannerLoadResult.Failed`, `MaxBannerSdkView.setCallbacks`). Mọi test có sẵn dựng bộ tải không có `LoadErrorLog` hay với `AdsDebugOptions.NONE`, tức chạy như production: câu khẳng định về chữ log của chúng giữ đúng từng ký tự. Phần dựng đổi theo chữ ký mới (ADR-0029, đã liệt kê ở mục 3.2): `FakeNativeAds.kt`, `FakeBannerAdView.kt`, `FakeMaxBannerSdkView.kt`, `InListAdsStateTest`, `NativeAdSourcesTest`, `AdsControllerInListRowTest`, `NativeAdLoadListenerTest`, `MaxBannerAdViewTest` (hàm dựng). `Failed(3)` vẫn bằng `Failed(3, null)` (lớp `data`, mặc định `null`), nên mọi câu `assertEquals(listOf(Failed(3)), ...)` có sẵn giữ.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Dòng `Native <khóa> failed: <mã>` | Giữ ở production và bộ tải không có `LoadErrorLog`; staging thêm `"<thông điệp>"` (bước 4) | `NativeAdLoadingTest.loadFailure_becomesUnavailableAtOnce` (chứa `failed: 3`), `logsHoldNoAdContent`; `NativeAdRetryTest`, `NativeAdSourceTest`, `NativeAdSourcesTest`, `AdsControllerInListRowTest` (dòng `failed: 0 (request not built)`, `retry in ...`) | Giữ: bản giả `fail(3)` truyền `null`, dòng giữ từng ký tự |
| `NativeAdLoadListener` gọi `onFailed(code)` | Gọi `onFailed(code, loadErrorLog.messageOf(message))`; `events.onFailed(code, message gốc)` giữ (bước 4) | `NativeAdLoadListenerTest.onAdFailedToLoad_handsErrorToResponseLog_beforeOnFailed` (thứ tự `response:3`, `failed:3`; `events.onFailed(3, any())`) | Giữ câu khẳng định; lambda của phần dựng thành `{ code, _ -> order += "failed:$code" }` |
| `BannerLoadResult.Failed(code)` | `Failed(code, logMessage = null)`; AdMob ở staging mang thông điệp đã che (bước 5) | `SdkBannerAdViewTest.listenerResults_reachResultsAndMetrics` (`[Failed(3)]`), `PlatformBannersTest.*` (`Failed(3)`, `Failed(204)`), `MaxBannerAdViewTest.callbacks_becomeResults` (`Failed(204)`) | Giữ: các test này dựng không có `LoadErrorLog` (hay qua bản giả không có thông điệp), `Failed(3) == Failed(3, null)` |
| Dòng thử lại banner `... (failures <n>, code <mã>)` | Giữ khi `logMessage` là `null`; có thông điệp thì `code <mã> "<thông điệp>"` (bước 5) | `BannerLoadsTest.failures_retryAt2_4_6_8_10_10s` (`Banner $KEY retry in 10000 ms (failures 5, code 3)`) | Giữ |
| Dòng `Banner <khóa> MAX failed (code <mã>)` | Giữ với `CODE_ONLY`; staging thêm thông điệp (bước 6) | `MaxBannerAdViewTest.logs_neverContainUnitId` (`"Banner gallery_banner MAX failed (code 204)" in messages`, không dòng nào có mã đơn vị) | Giữ: hàm dựng mặc định `CODE_ONLY` |
| Bốn dòng `... failed to load: <mã>` của `AdsController` | Giữ với `AdsDebugOptions.NONE`; staging thêm thông điệp (bước 3) | Không test nào chạy tới callback này (SDK tải thật); `AdNetworkLogWiringTest.everySdkLoadCallback_logsNetworks` đếm callback 4/1/1 | Giữ; test quét mới ở mục 4 kiểm từng callback |
| `AdsDebugOptions(testDeviceIds, logNetworkResponses)` | Thêm trường thứ ba, mặc định `false`; `staging()` bật (bước 2) | `AdsDebugOptionsTest.staging_keepsIdsAndLogsNetworks` (các câu về `testDeviceIds`, `logNetworkResponses` của `staging`, `NONE`); `:app` `StagingAdsModuleTest.adsDebugOptions_readsBuildConfig_andLogsNetworks` (`assertEquals(AdsDebugOptions.staging(BuildConfig.ADS_TEST_DEVICE_IDS), options)`); `AdsDebugOptionsSourceSetTest.production_providesNone` (production dùng `AdsDebugOptions.NONE`) | Giữ: hai bên của phép so cùng dựng bằng `staging(...)`; production vẫn `NONE` |
| `MaxBannerSdkView.setCallbacks(onLoaded, onFailed: (Int) -> Unit, onClicked)` | `onFailed: (Int, String?) -> Unit` (bước 6) | `MaxBannerAdViewTest.realView_buildsForBannerAndMrec_rejectsCollapsible` (gọi `banner.setCallbacks({}, {}, {})` trên `MaxAdView` thật, dòng 268) | Giữ câu khẳng định; phần dựng thành `banner.setCallbacks({}, { _, _ -> }, {})` |
| Bộ tải ném lỗi khi dựng yêu cầu: `events.onFailed(0, null)`, dòng `request not built` | Giữ (không phải lỗi do SDK trả) | `NativeAdRetryTest.loaderNotBuilt_reportsCode0ToEventsAndRetriesAt5s`, `NativeAdRefreshTest.loaderThrows_countsAsCode0_keepsAd`, `AdsControllerInListRowTest.inListRow_failedThenRetriesAt5s_listRowMetrics` | Giữ; `ThrowingNativeAdLoader` chỉ đổi chữ ký |

## 4. Kiểm thử

Test viết trước code, thất bại đúng lý do trước khi viết code. Commit test của bước 3 có khung rỗng để test biên dịch được (auto-run.md, bước 3): file `LoadErrorLog.kt` với `messageOf` trả `null`, `text` trả `"$code"`, `loadErrorText` trả `"$code"`, `logSafeMessage` trả nguyên chuỗi; các chữ ký mới của mục 3.1 (tham số `loadErrorLog` có mặc định `CODE_ONLY`, `onFailed` hai tham số mà code chính truyền `null`, `Failed.logMessage`, trường `logLoadErrorMessages` mà `staging()` chưa bật, `AdsController.loadErrorLog = LoadErrorLog.CODE_ONLY`); các bản giả và phần dựng của mục 3.2. Commit code của bước 4 điền phần thật. Thời gian ảo của `runTest`; logger là `RecordingAppLogger` (ADR-0019).

Thông điệp mẫu dùng trong test (không có mã thật: `3940256099942544` là mã thử của Google mà `scripts/secret-scan.py` cho phép):

- `NATIVE_MESSAGE = "Incorrect native ad response. Click actions were not properly specified"` (thông điệp thật đã thấy, RUN-20261025-01).
- `ADMOB_UNIT_MESSAGE = "No ad for ca-app-pub-3940256099942544/2247696110."` (che thành `No ad for <ad unit>.`).
- `MAX_UNIT_MESSAGE = "No fill for 2d0ec3c0d7d4b5ad"` (che thành `No fill for <ad unit>`).

**Mọi đường tới một dòng lỗi tải** (docs/CLAUDE.md, Khi viết plan):

| Đường | Cách có test |
| --- | --- |
| Native trong màn hình: lượt đầu, thử lại, làm mới, kho định dạng (`pool`), kho nạp sẵn, trong danh sách, lớp thu gọn, thẻ luồng bước | Một hàm chung có test: dòng ghi ở `loadOneNativeAd` (`NativeAdLoadingTest`), thông điệp đi qua `NativeAdLoadListener` (`NativeAdLoadListenerTest`); mọi `SdkNativeAdLoader` chỉ dựng trong `newNativeLoader` (test có sẵn `everyLoaderInMain_getsTheNetworkLog`) và chỗ dựng đó mang `loadErrorLog =` (test quét mới); `AdsController.loadErrorLog` theo biến thể (`AdsControllerLoadErrorLogTest`) |
| Native toàn màn hình, interstitial, app open, rewarded: nạp sẵn, thử lại, nạp lại (mỗi định dạng một hàm tải, một callback lỗi) | Callback của SDK, không chạy được trên JVM: test quét mới `everyAdMobLoadFailure_logsThroughLoadErrorLog` (bốn callback của `AdsController` gọi `loadErrorLog.text(error.code, error.message)`); `LoadErrorLog.text` có test JVM; `AdsController.loadErrorLog` theo biến thể |
| Banner AdMob (cố định, MREC, thu gọn, lớp banner của thanh đáy): lượt gửi, thử lại, nạp lại, lượt SDK tự làm mới | Một listener (`SdkBannerAdViewTest`), một dòng (`BannerLoadsTest`), đi qua khung hai nền tảng (`PlatformBannersTest`); chỗ dựng duy nhất ở `AdSlot.kt` mang `loadErrorLog =` (test quét mới) |
| Banner, MREC của MAX | `MaxBannerAdViewTest` (dòng); callback của `AppLovinMaxBannerView` truyền `error.message` (test quét mới); chỗ dựng ở `AdSlot.kt` (test quét mới) |

Không có trạng thái theo vị trí hay tài nguyên giữ theo cờ: plan không thêm test hai yêu cầu chồng nhau hay test cờ tắt giữa chừng (mỗi lượt lỗi tự ghi dòng của nó, `LoadErrorLog` không giữ gì).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-28 (mới): mã và thông điệp, dạng dòng | Unit test | `LoadErrorLogTest.staging_codeAndMessage`: `LoadErrorLog(true).text(3, "No fill.") == "3 \"No fill.\""`; `loadErrorText(3, "No fill.") == "3 \"No fill.\""`, `loadErrorText(3, null) == "3"` |
| KB-28: lượt không có thông điệp | Unit test | `LoadErrorLogTest.staging_noMessage_isJustTheCode`: `LoadErrorLog(true).text(0, null) == "0"`, `.text(0, "   ") == "0"`, `.messageOf("   ") == null`; đối chứng cùng test: `.text(0, "x") == "0 \"x\""` (đỏ trên khung rỗng) |
| KB-28: production chỉ có mã | Unit test | `LoadErrorLogTest.codeOnly_isJustTheCode`: `LoadErrorLog.CODE_ONLY.text(3, NATIVE_MESSAGE) == "3"`, `LoadErrorLog(false).messageOf(NATIVE_MESSAGE) == null`; đối chứng cùng test: `LoadErrorLog(true).messageOf(NATIVE_MESSAGE) == NATIVE_MESSAGE` (đỏ trên khung rỗng) |
| KB-28: che mã AdMob | Unit test | `LoadErrorLogTest.logSafeMessage_masksAdMobIds`: `logSafeMessage(ADMOB_UNIT_MESSAGE) == "No ad for <ad unit>."`; `logSafeMessage("App ca-app-pub-3940256099942544~3347511713 mismatch") == "App <ad unit> mismatch"`; `logSafeMessage("Publisher pub-3940256099942544 blocked") == "Publisher <ad unit> blocked"`; mỗi kết quả không chứa `ca-app-pub` hay `3940256099942544` |
| KB-28: che mã đơn vị MAX | Unit test | `LoadErrorLogTest.logSafeMessage_masksMaxUnitIds`: `logSafeMessage(MAX_UNIT_MESSAGE) == "No fill for <ad unit>"`; `logSafeMessage("unit 2D0EC3C0D7D4B5AD0011 failed") == "unit <ad unit> failed"` (hoa, 20 ký tự) |
| KB-28: không che chữ khác | Unit test | `LoadErrorLogTest.logSafeMessage_keepsOtherText`: `"Error code 204, see https://support.google.com/admob/answer/9905175"` giữ nguyên; `"token abcdef012345678 ok"` (15 ký tự hex) giữ nguyên; `"id abcdef0123456789xyz"` (hex dính chữ) giữ nguyên; `NATIVE_MESSAGE` giữ nguyên |
| KB-28: một dòng | Unit test | `LoadErrorLogTest.logSafeMessage_oneLine`: `"Line one\n  line two\t"` thành `"Line one line two"` |
| KB-28: cắt sau khi che | Unit test | `LoadErrorLogTest.logSafeMessage_capped_afterMasking`: `"x".repeat(400)` thành `"x".repeat(300) + "..."`; `"x".repeat(290) + " ca-app-pub-3940256099942544/2247696110"`: kết quả không chứa `ca-app`, `3940256099942544`, dài đúng 303 ký tự; đối chứng `"x".repeat(300)` giữ nguyên, không có `...` |
| KB-28: chỉ staging bật | Unit test | `AdsDebugOptionsTest.loadErrorMessages_stagingOnly`: `AdsDebugOptions.staging("").logLoadErrorMessages == true`, `staging("A,B").logLoadErrorMessages == true`, `AdsDebugOptions.NONE.logLoadErrorMessages == false` |
| KB-28: bản staging của `:app` bật | Unit test (`:app`, `testStagingDebugUnitTest`) | `StagingAdsModuleTest.adsDebugOptions_logLoadErrorMessages`: `StagingAdsModule.adsDebugOptions().logLoadErrorMessages == true`. Production: test có sẵn `AdsDebugOptionsSourceSetTest.production_providesNone` (nguồn production chỉ có `AdsDebugOptions.NONE`) cộng `AdsDebugOptionsTest.loadErrorMessages_stagingOnly` (`NONE` tắt) |
| KB-28: `AdsController` theo biến thể | Robolectric | `AdsControllerLoadErrorLogTest.staging_logsMessage`: `testAdsController(debugOptions = AdsDebugOptions.staging("")).loadErrorLog.text(3, ADMOB_UNIT_MESSAGE) == "3 \"No ad for <ad unit>.\""`; `production_codeOnly`: `testAdsController().loadErrorLog.text(3, ADMOB_UNIT_MESSAGE) == "3"` (qua ngay trên khung rỗng, cặp với test trên) |
| KB-28: native, dòng có thông điệp | Unit test | `NativeAdLoadingTest.loadFailure_withMessage_lineHasCodeAndMessage`: `nativeAdStates` với `FakeNativeAdLoader`, lúc 1 000 `loader.fail(0, NATIVE_MESSAGE)`: đúng một mục thẻ `Ads`, mức `INFO`, `message == "Native sample_native_small failed: 0 \"$NATIVE_MESSAGE\""`; trạng thái `[Loading, Unavailable]` |
| KB-28: native, production | Unit test | `NativeAdLoadingTest.loadFailure_noMessage_lineIsCodeOnly`: `loader.fail(3)`: mục duy nhất có `message == "Native sample_native_small failed: 3"` (qua ngay trên khung rỗng, cặp với test trên) |
| KB-28: native, thông điệp đã che tới bộ tải; số liệu nhận thông điệp gốc | Robolectric | `NativeAdLoadListenerTest.onAdFailedToLoad_staging_handsMaskedMessage`: `NativeAdLoadListener(events, loadErrorLog = LoadErrorLog(true)) { code, message -> got += code to message }`, `onAdFailedToLoad(LoadAdError(3, ADMOB_UNIT_MESSAGE, "com.google.android.gms.ads", null, null))`: `got == [3 to "No ad for <ad unit>."]`, `verify { events.onFailed(3, ADMOB_UNIT_MESSAGE) }`. `onAdFailedToLoad_default_handsNoMessage`: không truyền `loadErrorLog`: `got == [3 to null]` (qua ngay, cặp với test trên) |
| KB-28: banner AdMob, kết quả mang thông điệp đã che; log từng mạng không có thông điệp | Robolectric | `SdkBannerAdViewTest.loadFailed_staging_resultCarriesMaskedMessage_networkLinesDoNot`: `SdkBannerAdView(..., networkLog = AdNetworkLog(true, logger), loadErrorLog = LoadErrorLog(true))`, `send()`, `onAdFailedToLoad(LoadAdError(3, ADMOB_UNIT_MESSAGE, ...))`: kết quả `[Failed(3, "No ad for <ad unit>.")]`; có ít nhất một dòng bắt đầu bằng `Network ` (đối chứng) và không dòng nào của logger chứa `No ad for` hay `ca-app-pub`. Test có sẵn `listenerResults_reachResultsAndMetrics` giữ `Failed(3)` cho bộ tải mặc định |
| KB-28: banner AdMob, dòng thử lại | Unit test | `BannerLoadsTest.failedWithMessage_retryLineHasCodeAndMessage`: `view.onSend = { if (view.sendCount == 1) view.emitFailed(3, "No fill.") else view.emitLoaded() }`, `reloadMillis = MutableStateFlow(0L)`, chạy tới 3 000: logger có đúng một mục `message == "Banner $KEY retry in 2000 ms (failures 1, code 3 \"No fill.\")"`. Test có sẵn `failures_retryAt2_4_6_8_10_10s` giữ dòng chỉ có mã |
| KB-28: thông điệp đi qua khung hai nền tảng | Unit test | `PlatformBannersTest.lastPlatformFailed_keepsMessage`: `ordered(ADMOB to admob)`, `send()`, `admob.emitFailed(3, "No fill.")`: `got == [Failed(3, "No fill.")]`; đối chứng trong cùng test: `ordered(MAX to max2, ADMOB to admob2)`, MAX `emitFailed(204, "x")` rồi AdMob `emitFailed(3, "No fill.")`: `got2 == [Failed(3, "No fill.")]` (kết quả của nền tảng cuối) |
| KB-28: MAX, dòng có thông điệp đã che | Robolectric | `MaxBannerAdViewTest.failed_staging_lineHasCodeAndMaskedMessage`: `banner(loadErrorLog = LoadErrorLog(true))`, `send()`, `sdk.emitFailed(204, MAX_UNIT_MESSAGE)`: logger có `"Banner gallery_banner MAX failed (code 204 \"No fill for <ad unit>\")"`, không dòng nào chứa `2d0ec3c0d7d4b5ad`; kết quả `[Failed(204)]` (không lặp thông điệp ở dòng thử lại) |
| KB-28: MAX, production | Robolectric | `MaxBannerAdViewTest.failed_default_lineCodeOnly`: `banner()`, `sdk.emitFailed(204, "No Fill")`: có mục `message == "Banner gallery_banner MAX failed (code 204)"`, không dòng nào chứa `No Fill` (qua ngay trên khung rỗng, cặp với test trên) |
| KB-28: mọi callback lỗi tải của AdMob đi qua `LoadErrorLog` | Unit test (quét mã nguồn) | `AdNetworkLogWiringTest.everyAdMobLoadFailure_logsThroughLoadErrorLog`: với mỗi thân hàm `override fun onAdFailedToLoad(<tên>: LoadAdError)` trong `src/main/kotlin` (tìm bằng mẫu có sẵn của `callbackCounts`, lấy thân bằng `closingBracket`, bỏ dòng chú thích bằng `code(...)`), thân có `loadErrorLog.text(<tên>.code, <tên>.message)` hay `loadErrorLog.messageOf(<tên>.message)`. Đối chứng: số thân là 4 ở `AdsController.kt`, 1 ở `BannerAdView.kt`, 1 ở `SdkNativeAdLoader.kt`; một đoạn mẫu `override fun onAdFailedToLoad(error: LoadAdError) { logger.warn(TAG, "x ${error.code}") }` bị báo |
| KB-28: mọi bộ tải mang `LoadErrorLog` | Unit test (quét mã nguồn) | `AdNetworkLogWiringTest.everyLoaderInMain_getsTheLoadErrorLog`: mọi lời gọi `SdkNativeAdLoader(` trong `src/main` (dùng `callsOf` có sẵn) có tham số bắt đầu bằng `loadErrorLog =`; mọi lời gọi `SdkBannerAdView(` và `MaxBannerAdView(` trong `AdSlot.kt` có tham số `loadErrorLog = ads.loadErrorLog`; mỗi danh sách không rỗng (đối chứng); đoạn mẫu `SdkNativeAdLoader(context, adRequest(), events, networkLog = networkLog, logKey = logKey)` bị báo |
| KB-28: callback lỗi của MAX truyền thông điệp | Unit test (quét mã nguồn) | `AdNetworkLogWiringTest.maxLoadFailure_passesMessage`: `MaxBannerAdView.kt` có đúng một `onAdLoadFailed(`, và nó khớp `onAdLoadFailed\(\s*\w+\s*:\s*String\s*,\s*(\w+)\s*:\s*MaxError\s*\)\s*=\s*onFailed\(\1\.code,\s*\1\.message\)`; đối chứng: đoạn `onAdLoadFailed(adUnitId: String, error: MaxError) = onFailed(error.code, null)` không khớp |
| KB-28: thông điệp không vào sự kiện phân tích | Unit test | `AdTelemetryTest.loadFailed_sdkMessage_neverInEvents`: `telemetry().loadFailed(AdFormat.INTERSTITIAL, SLOT, "preload", 3, "No fill. $ADMOB_UNIT_MESSAGE", 0)`: đúng một `AdMetricEvent.LoadFail` với `loadErrorCode == "3"`, `loadState == "no_fill"` (đối chứng sự kiện có thật); `toString()` của mọi sự kiện đã ghi không chứa `No fill`, `No ad for`, `ca-app-pub`. Qua ngay (plan không đổi `AdTelemetry`), là test giữ |
| KB-28 (có sẵn): log từng mạng không đọc thông điệp | Unit test có sẵn | `AdNetworkResponsesTest` giữ nguyên; thêm vế trong `SdkBannerAdViewTest.loadFailed_staging_...` ở trên |
| Dòng không do SDK trả lỗi giữ nguyên | Unit test có sẵn | `NativeAdRetryTest` (`failed: 0 (request not built)`), `AdsControllerInListRowTest`, `NativeAdPoolTest` (dòng ngoại lệ) giữ câu khẳng định |
| KB-28 trên máy: dòng thật có thông điệp, không có mã đơn vị | Kiểm tra thủ công (máy thật: khác: kịch bản emulator chỉ đọc giao diện, không đọc logcat; cần lượt tải lỗi thật của SDK) | Cài bản staging dựng từ nhánh chính sau merge, đồng ý quảng cáo, bật chế độ máy bay rồi mở màn xem thử (hay mở trên emulator, nơi native thử của Google lỗi mã 0): `adb logcat -v threadtime \| grep ' VGL\.Ads'` có dòng `Native ... failed: <mã> "<thông điệp>"` hay `... failed to load: <mã> "<thông điệp>"`, không dòng nào có `ca-app-pub`. Dòng ghi lúc callback lỗi tải của SDK chạy (mục 3.1, bảng), điều kiện: bản staging (`AdsDebugOptions.staging`) và SDK trả lỗi; chế độ máy bay chỉ cho lỗi khi app đã qua popup mất mạng, nên emulator là cách chắc hơn. Log `device-session` cuối phiên (emulator của CI) cũng cho thấy dòng native lỗi mã 0, người đọc báo cáo xác nhận |
| Bản production chỉ có mã | Unit test | `LoadErrorLogTest.codeOnly_isJustTheCode`, `AdsControllerLoadErrorLogTest.production_codeOnly`, `NativeAdLoadingTest.loadFailure_noMessage_lineIsCodeOnly`, `MaxBannerAdViewTest.failed_default_lineCodeOnly`, `NativeAdLoadListenerTest.onAdFailedToLoad_default_handsNoMessage`; `ProductionAdsModule` dùng `AdsDebugOptions.NONE` (test có sẵn `AdsDebugOptionsSourceSetTest`) |
| Tài liệu `ads-guide.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc gạch đầu dòng "Thông điệp lỗi", đối chiếu mục 3.1 bước 9 và bảng dòng lỗi tải; `scripts/history-refs.py` qua |

**Chạy thử trên code hiện tại** (test tạm `TmpT093BaselineTest`, không commit, 2026-11-07, nhánh plan từ `2fd2701`, lệnh `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT093BaselineTest*' -i`, BUILD SUCCESSFUL trong 2 phút 5 giây): `nativeAdStates` với `FakeNativeAdLoader`, `fail(0)` ghi đúng một dòng `Native sample_native_small failed: 0` (chỉ mã); `NativeAdLoadListener.onAdFailedToLoad(LoadAdError(0, "Incorrect native ad response. Click actions were not properly specified", ...))` chỉ chuyển mã `0` cho bộ tải, thông điệp đọc được trong test JVM (`error.message` trả đúng chuỗi trên). Tức trên code hiện tại, dòng lỗi tải không có thông điệp; test viết trước ở trên đỏ vì thiếu thông điệp, không vì không đọc được `LoadAdError` trên Robolectric.

**Ngoại lệ viết test trước:**

- Sửa tài liệu `docs/ops/ads-guide.md` và KDoc.
- Qua ngay trên khung rỗng vì kiểm phần production giữ như hôm nay, mỗi test đứng cặp với một test staging đỏ trên khung rỗng (bước đối chứng của docs/CLAUDE.md): `AdsControllerLoadErrorLogTest.production_codeOnly`, `NativeAdLoadingTest.loadFailure_noMessage_lineIsCodeOnly`, `NativeAdLoadListenerTest.onAdFailedToLoad_default_handsNoMessage`, `MaxBannerAdViewTest.failed_default_lineCodeOnly`. `LoadErrorLogTest.codeOnly_isJustTheCode` có vế đối chứng đỏ trong chính nó.
- Qua ngay vì là test giữ của hành vi không đổi: `AdTelemetryTest.loadFailed_sdkMessage_neverInEvents`. Đột biến để thấy đỏ: `perl -pi -e 's/\QrecordLoadFailure(format, slot, placement, code, now) { offline ->\E/recordLoadFailure(format, slot, "\$placement \$sdkMessage", code, now) { offline ->/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt`, rồi `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` thấy file đổi; chạy test thì đỏ (`adPlacement` của sự kiện chứa thông điệp); trả về bằng `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt`. Phiên ghi kết quả vào nhật ký.
- `StagingAdsModuleTest.adsDebugOptions_logLoadErrorMessages` (`:app`, mục 3.2) đỏ trên khung rỗng vì khung chưa bật trường ở `staging()`.
- Test quét `everyLoaderInMain_getsTheLoadErrorLog` và `maxLoadFailure_passesMessage` đỏ trên khung rỗng chỉ khi khung rỗng chưa nối `loadErrorLog =` và `error.message`: khung rỗng của bước 3 không nối chúng (chỗ nối thuộc commit code), nên chúng đỏ đúng lý do.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core `:core:ads`). Không chạm 1 (không đổi vị trí, định dạng, tần suất, SDK hay adapter), 2, 3, 5, 6, 8, 9. Điểm 4 (dữ liệu gửi ra khỏi máy): AI coi là không chạm, vì thông điệp chỉ vào log của bản staging, bản này dựng với cấu hình Firebase giả nên log chỉ ở logcat (ADR-0012), sự kiện phân tích không đổi, và production giữ dòng chỉ có mã; người duyệt xác nhận ở câu hỏi 5.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Chín file mã chính trong một module `:core:ads`, đổi giao diện nội bộ (`NativeAdLoader.onFailed`, `BannerLoadResult.Failed`, `MaxBannerSdkView.setCallbacks`, `AdsDebugOptions` công khai thêm trường có mặc định) |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, câu hỏi 58 đã trả lời; còn chi tiết AI tự quyết: dạng dòng, dấu che, giới hạn 300 ký tự, mẫu mã MAX, dòng nào mang thông điệp (câu hỏi 1 tới 4) |
| C. Khả năng kiểm chứng tự động | 1 | Phần thuần, native, banner, MAX có test JVM và Robolectric; bốn callback toàn màn hình và callback của MAX chỉ kiểm bằng test quét mã nguồn (SDK thật không chạy trên JVM); dòng thật trên máy là một lượt đọc logcat |
| D. Ảnh hưởng chéo | 1 | Sửa `AdsController.kt`, `AdSlot.kt`, `BannerLoads.kt`, `MaxBannerAdView.kt` dùng chung của `:core:ads`, nhưng không plan nào khác đang chờ chạy sửa chúng; T-072 (doanh thu MAX, chưa có plan) sau này sẽ sửa `MaxBannerAdView.kt`. Hành vi mà feature khác dựa vào không đổi |
| E. Độ mới của kỹ thuật | 1 | Biến thể của log từng mạng (`AdNetworkLog`, cờ trong `AdsDebugOptions`) và của test quét có sẵn (`AdNetworkLogWiringTest`); biểu thức chính quy che mã là điểm mới nhỏ |
| **Tổng** | 6 | **Mức: Cao** (chạm điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `AdSlot.kt`, `BannerLoads.kt`, `BannerAdView.kt`, `MaxBannerAdView.kt`, `NativeAdLoading.kt`, `SdkNativeAdLoader.kt`, `AdsDebugOptions.kt`, các bản giả dùng chung của test `:core:ads` (`FakeNativeAds.kt`, `FakeBannerAdView.kt`, `FakeMaxBannerSdkView.kt`). Chữ log của production không đổi, nên phân tích log và kịch bản thiết bị có sẵn không bị ảnh hưởng.
- Task khác trong sprint có thể xung đột: không plan `ads` nào đang `draft` hay `approved` (2026-11-07). T-072 (doanh thu MAX) sẽ sửa `MaxBannerAdView.kt` khi có plan: plan đó dựng trên chữ ký `onFailed(code, message)` mới. Plan khác thêm test quét có chuỗi `loadErrorLog` trong chú thích không làm test quét đỏ, vì `code(...)` bỏ dòng chú thích.
- Đề xuất thứ tự chạy: không phụ thuộc task nào; chạy trước T-072 nếu cùng danh sách.

## 7. Điều kiện dừng riêng

- Cần đọc thông điệp lỗi trong log từng mạng (`AdNetworkLog`, dòng `Network ...`), đưa thông điệp vào sự kiện phân tích (`AdMetricEvent`, `AdFunnelEvent`) hay vào dòng log của bản production, để tiêu chí qua: dừng (trái câu trả lời 58).
- Cần đổi dòng `failed: 0 (request not built)`, dòng ngoại lệ hay dòng `failed to show`: dừng (ngoài phạm vi ticket).
- Thấy một callback lỗi tải của SDK ngoài bảng của mục 3.1 (ví dụ định dạng MAX mới): nối nó qua `LoadErrorLog` như các đường khác (ADR-0029) và ghi nhật ký; không dừng.

## 8. Câu hỏi cho người duyệt

1. **Dạng dòng, dấu che và giới hạn.** Đề xuất: `<mã> "<thông điệp>"` (ví dụ `failed: 0 "Incorrect native ad response..."`, trong dòng banner `code 3 "No fill."`), dấu che `<ad unit>`, gộp một dòng, cắt ở 300 ký tự rồi thêm `...`. Phương án khác: dấu che `***`; không cắt (thông điệp của SDK thường dưới 200 ký tự, nhưng không có bảo đảm).
2. **Mẫu mã đơn vị MAX.** AI chưa thấy mã MAX thật trong repo (chỉ nằm trong `local.properties` của dev). Đề xuất: che mọi chuỗi hex từ 16 ký tự đứng riêng (mã đơn vị MAX là 16 ký tự hex), chấp nhận che nhầm chuỗi hex dài khác (an toàn hơn che thiếu). Phương án khác: chỉ che đúng 16 ký tự; hay che thêm đúng mã đơn vị đang tải (truyền vào `messageOf`), đổi thêm chữ ký. Người duyệt có mã MAX thật xin xác nhận độ dài.
3. **Dòng nào mang thông điệp.** Đề xuất: mỗi lượt lỗi một dòng, là dòng đầu tiên ghi mã (bảng mục 3.1); dòng `Retry ... (failures n, code m)` của `FullScreenReloads.kt` và dòng `Banner <khóa> ADMOB no ad, trying MAX` của `PlatformBanners.kt` giữ nguyên. Hệ quả: ở khung có AdMob rồi MAX, lỗi AdMob chuyển sang MAX không có dòng nào mang thông điệp của AdMob (hôm nay dòng đó cũng không có mã); chỉ gặp ở app có mã MAX. Phương án khác: thêm `(<mã> "<thông điệp>")` vào dòng `no ad, trying` ở staging (production thêm mã), sửa `PlatformBanners.kt` và test của nó.
4. **MAX không lặp thông điệp.** Đề xuất: dòng `MAX failed` mang thông điệp, `Failed` của MAX không mang, nên dòng thử lại sau đó chỉ có mã. Phương án khác: `Failed` của MAX cũng mang, dòng thử lại lặp thông điệp (giống banner AdMob hơn, nhưng hai dòng cùng chữ).
5. **Điểm chặn 4.** Đề xuất: không tính là chạm (log staging chỉ ở logcat, Firebase staging giả, sự kiện không đổi, production không đổi), nên `blocking_points: [core]`. Nếu người duyệt coi log staging là dữ liệu ra khỏi máy (ví dụ bản staging dựng với Firebase thật ở một app), thêm `data` và báo người phụ trách sản phẩm theo `risk-scoring.md`, mục 3.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
