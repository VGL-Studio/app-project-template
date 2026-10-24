---
ticket: T-023
title: Doanh thu theo vị trí ad_impression_placement và số liệu vòng đời Firebase
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (analytics, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý 15M (scripts/token-usage.py estimate, 2026-10-24); nâng vì hai module và năm chỗ nối của SDK
blocking_points: [analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                # người duyệt
cross_reviewer: Nhật          # bắt buộc khi risk_level là high
approved_at: 2026-10-24
---

# PLAN T-023: DOANH THU THEO VỊ TRÍ AD_IMPRESSION_PLACEMENT VÀ SỐ LIỆU VÒNG ĐỜI FIREBASE

## 1. Tóm tắt

Thêm vào `:core:analytics` một interface ghi số liệu quảng cáo `AdMetrics` (bản Firebase, bản ghi lại cho test) và vào `:core:ads` một bộ ghi `AdTelemetry` cùng các hàm thuần ánh xạ (mã định dạng, giá trị micro sang tiền, mã lỗi sang `load_state`, lý do hiện lỗi, vị trí của lượt nạp, `opportunity_result`), theo `docs/ops/ads-business-rules.md`, mục 8.1 và 8.2. Mọi định dạng đang có (interstitial, native toàn màn hình, native small, medium, thu gọn, trong danh sách, banner) nghe sự kiện doanh thu của SDK và ghi `ad_impression_placement` với vị trí đọc lúc sự kiện về; native có dự phòng giá trị 0 sau 2 giây; số liệu vòng đời (`ad_load_success`, `ad_load_fail`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`) chỉ gửi khi khóa của nhóm bật và đang được phục vụ quảng cáo. Kiểm chứng bằng unit test cho từng hàm ánh xạ, từng cổng gửi, dự phòng native với đồng hồ giả, tên và kiểu tham số của Firebase, và kiểm DebugView trên staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23; mục 6 liệt kê đủ các sự kiện, plan không đổi tham số nên không cần sửa spec):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-22 | Mỗi lượt hiển thị quảng cáo ghi một sự kiện doanh thu theo vị trí ở Firebase (`ad_impression_placement`) và ở AppsFlyer (`af_ad_revenue`), với định dạng, mạng, giá trị, vị trí. | Phần Firebase: task này. Mỗi sự kiện doanh thu của SDK (`OnPaidEventListener`, gắn lúc nạp, T-01) ghi `ad_impression_placement` với `ad_format` (T-04), `ad_platform` = `admob`, `ad_source` (tên nguồn của adapter đã trả quảng cáo, không biết thì `admob`, T-03), `ad_unit_name` (mã đơn vị), `currency`, `value` (số thực, micro chia 1 000 000), `ad_placement` (mã vị trí đọc lúc sự kiện về, PL-03, D-05; rỗng thì bỏ tham số). Luôn gửi, không phụ thuộc khóa vòng đời hay cổng đồng ý (8.2). Phần AppsFlyer là T-033: `AdMetricEvent.ImpressionRevenue` mang đủ dữ liệu (kể cả lớp adapter) để T-033 thêm bản gửi AppsFlyer mà không sửa chỗ nối của `:core:ads`. |
| KB-22 | Doanh thu không bị tính hai lần: app không ghi `ad_impression` và không gửi `af_revenue`. | Không sự kiện nào của `AdMetrics` có tên `ad_impression` (test duyệt mọi loại sự kiện). `af_revenue` thuộc T-033 (task này không có AppsFlyer). |
| KB-22 | Native hiện mà mạng không báo doanh thu trong 2 giây thì vẫn có một lượt ghi giá trị 0. | T-02: lúc native ghi lượt hiển thị (`AdListener.onAdImpression`), hẹn 2 000 ms; sự kiện doanh thu về trước thì hủy hẹn; tới hạn mà chưa có thì ghi một lượt `USD`, `0.0`. Mỗi quảng cáo chỉ ghi một lần. Banner không có dự phòng. Áp cho native small, medium, thu gọn, trong danh sách, toàn màn hình. |
| KB-22 | Khi người vận hành bật số liệu chi tiết thì có thêm sự kiện tải, cơ hội hiển thị, lỗi hiển thị, lượt bấm. | Khóa `is_log_ad_fullscreen_events` (nhóm toàn màn hình: `INTER`, `REWARDED`, `APP_OPEN`, `NATIVE_FULL`) và `is_log_ad_slot_events` (nhóm khe: còn lại), đọc từ `AdsFlags.global` của T-019 ở mỗi sự kiện. "Nhóm bật" = khóa của nhóm bật và đang được phục vụ (cổng đồng ý mở và SDK đã khởi tạo, `canRequestAds`). Sự kiện: `ad_load_success`, `ad_load_fail`, `ad_opportunity`, `ad_show_fail`, `ad_clicked` với tham số của mục 8.2. `ad_reward_earned`, `reward_dialog` chờ rewarded (T-031). |
| KB-22 | Bản staging không gửi gì sang AppsFlyer. | T-033 (task này không thêm AppsFlyer). |
| KB-22 | Khi người vận hành tắt một sự kiện AppsFlyer từ xa thì sự kiện đó ngừng gửi từ lượt kế tiếp, các sự kiện khác vẫn gửi. | T-033. |

Luật của mục 8.2 mà ticket nêu, mỗi luật một test (mục 4): Firebase bỏ `ad_placement` khi rỗng; `latency_ms` tính từ lúc gửi chính yêu cầu đó tới callback đầu tiên, không có khi kết quả không do `:core:ads` gửi (lượt tự làm mới của banner); `load_attempt` chỉ ở khe có bộ đếm lỗi liên tiếp; không có vị trí thì không ghi cơ hội; `load_state` chụp lúc quyết định (`ready`, `loading`, nhãn lỗi gần nhất, `idle`); bảng vị trí của lượt nạp; lượt dò trạng thái (`isFullScreenAdReady`, `loadFullScreenAdIfNeeded` của T-022) không ghi gì; ánh xạ mã lỗi sang `load_state`; không gửi thông báo lỗi của SDK; bộ ghi ném lỗi thì nuốt, luồng quảng cáo chạy tiếp; giá trị `ad_placement` là mã vị trí trong `AdPlacement` (MC-01).

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0012 (Firebase, staging dùng cấu hình giả không gửi), ADR-0013 (Analytics đọc chuỗi đồng ý TCF), ADR-0019, ADR-0026 (quyết định 7), ADR-0027 (T-033 dùng cùng interface).

Dựa trên các plan chưa merge khi viết plan này, dùng đúng tên:

- T-019: `AdsFlags.global: Flow<AdsGlobalFlags>` với `logFullScreenEvents`, `logSlotEvents`; `AdFormat` 11 giá trị (`APP_OPEN`, `REWARDED`, `BANNER_COLLAPSIBLE`, `NATIVE_STEP`, `NATIVE_IN_COLLECTION` cùng 6 giá trị cũ), `AdPlacement.key` là mã vị trí.
- T-020: `canRequestAds` là cổng đồng ý cộng SDK đã khởi tạo; `InternetConnectivity.connected` của `:core:network` (máy không có Internet thì `load_state` là `network`).
- T-022: `FullScreenShowDecision` (`SHOW`, `CONSENT_CLOSED`, `DISABLED`, `BUSY`, `NOT_READY`, `THROTTLED`, đúng thứ tự xét `busy`, `not_ready`, `throttled`), `FullScreenLoadState` (`Idle`, `Loading`, `Ready`, `WaitingRetry`), `decideFullScreenTurn`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded`.

## 3. Giải pháp

### 3.1. Cách làm

**1. `:core:analytics`** (ADR-0027 quyết định 1: `:core:ads` gửi số liệu quảng cáo qua interface của module này):

```kotlin
// AdMetrics.kt (public)
interface AdMetrics { fun log(event: AdMetricEvent) }

sealed interface AdMetricEvent {
    val adFormat: String
    data class ImpressionRevenue(
        override val adFormat: String, val adSource: String, val adUnitName: String, val currency: String,
        val value: Double, val adPlacement: String?, val adapterClassName: String?,  // adapter: for T-033
    ) : AdMetricEvent
    data class LoadSuccess(override val adFormat: String, val adPlacement: String?, val loadAttempt: Int?, val latencyMs: Long?) : AdMetricEvent
    data class LoadFail(
        override val adFormat: String, val adPlacement: String?, val loadAttempt: Int?, val latencyMs: Long?,
        val loadErrorCode: String, val loadState: String,
    ) : AdMetricEvent
    data class Opportunity(override val adFormat: String, val adPlacement: String, val result: String, val loadState: String) : AdMetricEvent
    data class ShowFail(override val adFormat: String, val adPlacement: String?, val showErrorCode: String, val showFailReason: String) : AdMetricEvent
    data class Clicked(override val adFormat: String, val adPlacement: String?) : AdMetricEvent
}
```

- `FirebaseAdMetrics.kt` (nội bộ): hai hàm thuần `AdMetricEvent.firebaseName(): String` (`ad_impression_placement`, `ad_load_success`, `ad_load_fail`, `ad_opportunity`, `ad_show_fail`, `ad_clicked`; `when` không có `else`) và `AdMetricEvent.firebaseParams(): Map<String, Any>` (tên tham số của mục 8.2; `value` là `Double`, `latency_ms`, `load_attempt` là `Long`, còn lại `String`; giá trị `null` hay chuỗi rỗng thì bỏ hẳn tham số). Lớp `FirebaseAdMetrics` dựng `Bundle` (`putDouble`, `putLong`, `putString`) rồi `FirebaseAnalytics.logEvent`, như `FirebaseAnalyticsLogger` có sẵn. Chạy thử đã gọi `Bundle.putDouble`, `putLong` và `logEvent` trong `:core:analytics` (mục 4.1).
- `RecordingAdMetrics.kt` (công khai, trong `main`, như `RecordingAppLogger` của `:core:logging`): giữ các sự kiện đã ghi cho test của `:core:ads` (ADR-0027 quyết định 5).
- `di/AnalyticsModule.kt`: `@Binds` `FirebaseAdMetrics` thành `AdMetrics`.
- `Analytics`, `AnalyticsEvent`, `FirebaseAnalyticsLogger` không đổi (feature vẫn dùng như cũ).

**2. Hàm thuần ở `:core:ads`** (`AdTelemetryRules.kt`, mới):

- `AdFormat.metricCode(): String` (T-04, `when` đủ 11 giá trị, không `else`): `INTERSTITIAL` ra `INTER`, `REWARDED` ra `REWARDED`, `APP_OPEN` ra `APP_OPEN`, `BANNER` và `BANNER_COLLAPSIBLE` ra `BANNER`, `NATIVE_FULL_SCREEN` ra `NATIVE_FULL`, `NATIVE_COLLAPSIBLE` ra `NATIVE_COLLAP`, `NATIVE_SMALL` ra `NATIVE_SMALL`, `NATIVE_STEP` ra `NATIVE_ONBOARDING`, `NATIVE_IN_COLLECTION` ra `NATIVE_CONTENT`, `NATIVE_MEDIUM` ra `NATIVE_MEDIUM`.
- `AdFormat.metricGroup(): MetricGroup` (`FULL_SCREEN` cho `INTERSTITIAL`, `REWARDED`, `APP_OPEN`, `NATIVE_FULL_SCREEN`; `SLOT` cho còn lại).
- `revenueValue(valueMicros: Long): Double` = `valueMicros / 1_000_000.0`; `adSourceOrAdmob(name: String?)`: rỗng hay `null` ra `admob`.
- `loadStateAfterError(code: Int, offline: Boolean, sdkThrottled: Boolean): String`: offline luôn `network`; 3 và 9 `no_fill`; 1 `sdk_throttled` nếu `sdkThrottled`, không thì `invalid`; 8, 10, 11 `invalid`; 0 `internal`; còn lại `unknown`. `sdkThrottled` do chỗ nối đọc từ thông báo lỗi của SDK lúc nhận lỗi (chỉ đọc trên máy, không gửi đi; câu hỏi 3).
- `showFailReasonOf(code: Int): String` (mã của `FullScreenContentCallback`): 0 `internal`, 1 `ad_reused`, 2 `not_ready`, 3 `app_not_foreground`, 4 `mediation_show_error`, còn lại `unknown`.
- `loadPlacementOf(format: AdFormat, screenPlacement: AdPlacement?): String?` (bảng của mục 8.2): `INTERSTITIAL`, `APP_OPEN`, `REWARDED`, `NATIVE_FULL_SCREEN` luôn `preload`; native một quảng cáo và thu gọn là mã của màn đang dùng khe, chưa có thì `preload`; banner là mã của màn, chưa có thì `null` (bỏ tham số); pool (T-028) là `pool`.
- `FullScreenShowDecision.opportunityResult(): String?`: `SHOW` ra `shown`, `BUSY` ra `busy`, `NOT_READY` ra `not_ready`, `THROTTLED` ra `throttled`, `CONSENT_CLOSED` và `DISABLED` ra `null` (không phải cơ hội).
- `loadStateAtDecision(state: FullScreenLoadState, lastErrorLabel: String?): String`: `Ready` ra `ready`, `Loading` ra `loading`, có nhãn lỗi gần nhất ra nhãn đó, còn lại `idle`. `shown` luôn đi với `ready`.

**3. Bộ đếm lượt nạp** (`LoadAttemptCounter.kt`, mới): theo khóa khe (mã vị trí hay `preload` của định dạng): `onRequestSent(slot, now)`, `onLoaded(slot, now): LoadOutcome(attempt, latencyMs)`, `onFailed(slot, now): LoadOutcome`. `attempt` = số lỗi liên tiếp trước đó + 1, về 0 khi thành công; `latencyMs` chỉ có khi callback ứng với một lượt gửi đã ghi (lượt tự làm mới của banner không có). `failuresInARow(slot)` cho T-021 tính thời gian chờ thử lại (F-05, O-05). Bộ đếm là trạng thái của đường nạp, không phải của số liệu: được cập nhật ở **mọi** lượt gửi, thành công và lỗi, không phụ thuộc khóa `is_log_ad_*` hay việc đang được phục vụ quảng cáo (`serving`); tắt số liệu không được làm thời gian chờ thử lại của T-021 đứng ở 2 giây (sửa theo rà soát trước phiên, 2026-10-24).

**4. Dự phòng doanh thu native** (`NativeRevenueFallback.kt`, mới, T-02): mỗi quảng cáo native một đối tượng; `onImpression(now): Long` trả thời điểm tới hạn (`now + 2_000`); `onPaidEvent(): Boolean` true nếu sự kiện này được ghi (lần đầu), và hủy dự phòng; `onDue(now): Boolean` true nếu phải ghi lượt `USD`, `0.0` (chưa ghi lần nào, đã tới hạn). Chỗ nối hẹn bằng `delay` trong scope của `AdsController`, hủy khi quảng cáo bị hủy.

**5. Bộ ghi** (`AdTelemetry.kt`, mới, nội bộ):

```kotlin
internal class AdTelemetry(
    private val metrics: AdMetrics,
    private val lifecycleSwitches: () -> AdsGlobalFlags,   // latest value of AdsFlags.global, read at each event
    private val serving: () -> Boolean,                    // canRequestAds.value
    private val offline: () -> Boolean,                    // latest InternetConnectivity value, negated
    private val counter: LoadAttemptCounter,               // updated on every send and result, switches or not
    private val logger: AppLogger,
)
```

- `revenue(format, unitId, placement: () -> AdPlacement?, valueMicros, currency, adSource, adapterClassName)`: luôn ghi. `placement()` ném lỗi thì coi như không có vị trí (PL-04).
- `AdTelemetry` giữ `LoadAttemptCounter` (truyền vào constructor). `requestSent(slot)`, `loadSucceeded`, `loadFailed` cập nhật bộ đếm (`onRequestSent`, `onLoaded`, `onFailed`) ở dòng đầu, luôn luôn, trước và ngoài cổng nhóm và ngoài `try`/`catch` của `metrics.log`; chỗ nối của mục 6 gọi chúng ở mọi lượt gửi và mọi kết quả, dù số liệu đang tắt.
- `loadSucceeded`, `loadFailed`, `opportunity`, `showFailed`, `clicked`: chỉ **ghi sự kiện** khi khóa của nhóm (`logFullScreenEvents` hay `logSlotEvents`) bật và `serving()`. `opportunity` bỏ qua khi kết quả là `null` hay không có vị trí (A 06:197).
- Mọi lời gọi `metrics.log` bọc `try`/`catch`: lỗi thì `logger.warn` thẻ `Ads` với tên lớp lỗi, luồng quảng cáo chạy tiếp. Không đưa thông báo lỗi của SDK vào tham số nào (`load_error_code` là mã số dạng chuỗi).
- Log (ADR-0019): mỗi lượt ghi doanh thu một dòng `debug` (định dạng, vị trí, có phải dự phòng), không ghi mã đơn vị.

**6. Nối với SDK** (các chỗ dưới chỉ đọc dữ liệu của SDK và gọi `AdTelemetry`; không có quyết định nào ở đây):

- `AdsController`: constructor thêm `metrics: AdMetrics`; dựng `AdTelemetry` với `flags.global` thu vào một `StateFlow` (giá trị mặc định của T-019 trước lần phát đầu), `canRequestAds`, `InternetConnectivity.connected` thu vào một `StateFlow`.
  - Interstitial: lúc gửi `requestSent("preload:INTER")` (cập nhật bộ đếm); `onAdLoaded`: `loadSucceeded`, `ad.setOnPaidEventListener` (vị trí đọc từ một ô giữ vị trí của lượt hiện, đặt lại ở mỗi lượt hiện, PL-03); `onAdFailedToLoad`: `loadFailed` với `loadStateAfterError`. Lượt hiện: `decideFullScreenTurn` (T-022) ra quyết định, `opportunity(placement, decision.opportunityResult(), loadStateAtDecision(...))` chụp trước khi kích nạp (8.2); `FullScreenContentCallback.onAdClicked`: `clicked`; `onAdFailedToShowFullScreenContent`: `showFailed(code, showFailReasonOf(code))`.
  - Native toàn màn hình: như interstitial (vị trí lượt nạp `preload`), cộng `AdListener.onAdImpression` để hẹn dự phòng.
  - `nativeAd`, `loadNativeAd`: truyền vào `SdkNativeAdLoader` một `NativeAdEvents` (interface nội bộ mới trong `SdkNativeAdLoader.kt`) gắn với vị trí của màn.
- `SdkNativeAdLoader.kt`: nhận `events: NativeAdEvents?` (mặc định `null`); trong `forNativeAd` gắn `setOnPaidEventListener`; `AdListener` thêm `onAdImpression` (dự phòng), `onAdClicked`, và gọi `loadSucceeded`, `loadFailed`. Interface `NativeAdLoader` và `loadOneNativeAd` không đổi, nên `FakeNativeAdLoader` và test hiện có giữ nguyên.
- `AdSlot.kt` (`BannerAd`): nhận `placement`; `AdView.setOnPaidEventListener` (không dự phòng), `adListener` có `onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`; `latency_ms` chỉ cho callback đầu tiên sau `loadAd` (lượt tự làm mới về sau không có, A 06:157).
- `isFullScreenAdReady`, `loadFullScreenAdIfNeeded` (T-022) không gọi `AdTelemetry.opportunity` (lượt dò không đo, A-04, HD-06).
- Tên nguồn của adapter: `responseInfo.loadedAdapterResponseInfo?.adSourceName`; lớp adapter: `loadedAdapterResponseInfo?.adapterClassName` (native: `NativeAd.responseInfo`); `AdValue.valueMicros`, `AdValue.currencyCode`. Chạy thử đã biên dịch các lời gọi này cho cả ba loại (mục 4.1).

**7. Phụ thuộc module:** `core/ads/build.gradle.kts` thêm `implementation(project(":core:analytics"))` (core phụ thuộc core, ADR-0001). `:core:analytics` (sau T-019 phụ thuộc `:core:config`) không phụ thuộc `:core:ads`, nên không có vòng; chạy thử: "Module dependency rule OK".

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/AdMetrics.kt` | `AdMetrics`, `AdMetricEvent` |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetrics.kt` | `firebaseName`, `firebaseParams`, `FirebaseAdMetrics` |
| Tạo | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/RecordingAdMetrics.kt` | Bản ghi lại cho test |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/di/AnalyticsModule.kt` | `@Binds` `AdMetrics` |
| Tạo | `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` | Tên, tham số, kiểu, bỏ tham số rỗng, không có `ad_impression` |
| Sửa | `core/ads/build.gradle.kts` | `implementation(project(":core:analytics"))` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetryRules.kt` | Hàm ánh xạ thuần (mục 3.1 bước 2) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounter.kt` | `load_attempt`, `latency_ms` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeRevenueFallback.kt` | Dự phòng 2 giây |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` | Bộ ghi: cổng nhóm, nuốt lỗi, vị trí đọc lúc sự kiện |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `metrics`, `AdTelemetry`, nối interstitial, native toàn màn hình, native một quảng cáo; ghi cơ hội ở lượt hiện |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | `NativeAdEvents`, doanh thu, lượt hiển thị, lượt bấm, kết quả nạp |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerAd` nhận vị trí; doanh thu, kết quả nạp, lượt bấm của banner |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt` | Mỗi hàm ánh xạ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt` | Bộ đếm lỗi liên tiếp, độ trễ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRevenueFallbackTest.kt` | T-02 với đồng hồ giả |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | Cổng gửi, vị trí lúc sự kiện, nuốt lỗi |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdsTest.kt` | Dựng `AdsController` với `metrics = RecordingAdMetrics()` |
| Sửa | `docs/features/ads/tickets/T-023-doanh-thu-theo-vi-tri.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-023-doanh-thu-theo-vi-tri.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã tìm `git grep` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/testStaging`, `*/src/androidTest`, mọi `build.gradle.kts`):

- `AdsController.kt`: `FullScreenAdsTest.kt` dựng thẳng `AdsController` (có trong danh sách; chạy thử của T-020 cho thấy test này lỗi biên dịch khi constructor thêm tham số).
- `SdkNativeAdLoader.kt`: không test nào dựng nó; `FakeNativeAdLoader` cài `NativeAdLoader`, interface không đổi.
- `AdSlot.kt`: `CollapsibleNativeAdTest`, `NativeAdSlotTest` gọi `CollapsibleNativeAdSlot`, `NativeAdSlot` (tên chứa chuỗi `AdSlot(`), không gọi `AdSlot` hay `BannerAd`; `PrivacyOptionsButton`, `rememberShowInterstitial` trong file này không đổi.
- `AnalyticsModule.kt`: không test hay script nào nhắc.
- `core/ads/build.gradle.kts`: `checkModuleDependencies` của `build-logic` xét theo tên module; core phụ thuộc core được phép.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt, di chuyển vị trí; không đổi định dạng, tần suất hay cổng. Chỉ nghe thêm callback của SDK.
- Sự kiện phân tích: thêm `ad_impression_placement`, `ad_load_success`, `ad_load_fail`, `ad_opportunity`, `ad_show_fail`, `ad_clicked` (Firebase), đúng tên và tham số của spec mục 6 và `ads-business-rules.md` mục 8.2 (điểm chặn 4). Không có dữ liệu cá nhân: mã đơn vị, mã vị trí, mã định dạng, tên mạng, tiền tệ, giá trị, mã lỗi số. Không gửi thông báo lỗi của SDK. Staging dùng Firebase giả (ADR-0012) nên không gửi gì; bản build staging có Firebase thật thì gửi về project staging.
- Dữ liệu trên máy: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Firebase Analytics và SDK quảng cáo đã có trong manifest gộp; không gọi API Android mới cần quyền.
- **Thư viện và SDK:** Không thêm. `firebase-analytics` (`libs.firebase.analytics`, BOM `libs.firebase.bom`) đã có trong `:core:analytics`; `OnPaidEventListener`, `AdValue`, `ResponseInfo`, `AdapterResponseInfo` thuộc `play-services-ads` (`libs.play.services.ads`) đã có trong `:core:ads` (biên dịch qua trong chạy thử). `:core:ads` thêm phụ thuộc module `:core:analytics` của dự án, không phải thư viện mới.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `sdkThrottled` cho mã lỗi 1 đọc từ thông báo lỗi của SDK (câu chữ "Too many recently failed requests" chưa được kiểm trên máy; chạy thử không gọi SDK thật).
- **Điều kiện chuyển:** kiểm thủ công trên staging không thấy được lỗi tự chặn để so câu chữ, hoặc người duyệt không muốn đọc thông báo lỗi (câu hỏi 3).
- **Cách làm thay:** mã 1 luôn ra `invalid` (`sdkThrottled = false` ở chỗ nối), hàm thuần và test của nó giữ nguyên (nhánh `sdk_throttled` vẫn có test, chờ T-021 hay task sau có cách đo).

## 4. Kiểm thử

Unit test JVM. `RecordingAdMetrics` (công khai, từ `:core:analytics`), `RecordingAppLogger`, đồng hồ giả là số mili giây truyền vào. `AdTelemetry` dựng với lambda: `switches` trả `AdsGlobalFlags` có `logFullScreenEvents`, `logSlotEvents` do test đặt (giá trị khác giữ mặc định của T-019), `serving` và `offline` do test đặt. Vị trí là `AdPlacement.SAMPLE_NATIVE_SMALL` và `GALLERY_INTERSTITIAL`.

Bước 3 (test viết trước) có khung rỗng để biên dịch: `AdMetrics`, `AdMetricEvent` đủ lớp; `firebaseName` trả `""`, `firebaseParams` trả map rỗng; `RecordingAdMetrics` có danh sách; hàm của `AdTelemetryRules.kt` trả giá trị cố định (`""`, `0.0`, `null`); `LoadAttemptCounter` trả `LoadOutcome(1, null)`; `NativeRevenueFallback` trả false; `AdTelemetry` không ghi gì. Commit test của bước 3: mọi file test mới. Sửa `FullScreenAdsTest` vào commit code của bước 4 (chỉ biên dịch được khi constructor đổi) (L-025).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-22, tiêu chí 1 (tên và tham số Firebase) | Unit test | `FirebaseAdMetricsTest.impressionRevenue_paramsAndTypes`: `ImpressionRevenue("INTER", "AppLovin", "unit", "USD", 0.0123, "gallery_interstitial", "com.x.Adapter")` ra tên `ad_impression_placement`, tham số đúng 7 khóa `ad_format`, `ad_platform` (`admob`), `ad_source`, `ad_unit_name`, `currency`, `value` (kiểu `Double`, bằng 0.0123), `ad_placement`; không có khóa của lớp adapter. Khung rỗng thất bại vì tên rỗng |
| KB-22, tiêu chí 1 (vị trí rỗng, A 06:87) | Unit test | `FirebaseAdMetricsTest.emptyOrMissingParams_areDropped`: `adPlacement = null` và `""` thì không có khóa `ad_placement`; `LoadSuccess` không có `loadAttempt`, `latencyMs` thì không có hai khóa đó. Đối chứng (L-055): cùng sự kiện có vị trí thì có khóa |
| KB-22, tiêu chí 1 (giá trị, nguồn, T-03) | Unit test | `AdTelemetryRulesTest.revenueValue_microsToUnits`: 12 300 ra 0.0123, 0 ra 0.0. `adSourceOrAdmob`: `null`, `""` ra `admob`, `"Pangle"` giữ nguyên |
| KB-22, tiêu chí 1 (mã định dạng, T-04) | Unit test | `AdTelemetryRulesTest.metricCode_coversEveryFormat`: bảng 11 dòng của mục 3.1; tập kết quả bằng đúng 10 mã của T-04 (`BANNER` hai lần). Khung rỗng thất bại vì trả `""` |
| KB-22, tiêu chí 1 (vị trí đọc lúc sự kiện, PL-03, D-05, PL-04) | Unit test | `AdTelemetryTest.revenue_readsPlacementAtEventTime`: ô vị trí đổi từ A sang B sau khi gắn bộ nghe; sự kiện về ra `adPlacement == B.key`. `placementThrows_logsWithoutPlacement`: hàm vị trí ném lỗi: vẫn ghi, `adPlacement` null, luồng không ném |
| KB-22, tiêu chí 1 (luôn gửi) | Unit test | `AdTelemetryTest.revenue_ignoresLifecycleSwitchesAndServing`: cả hai khóa tắt, `serving = false`: `revenue` vẫn ghi 1 sự kiện. Đối chứng: cùng điều kiện, `loadSucceeded` không ghi |
| KB-22, tiêu chí 2 | Unit test | `FirebaseAdMetricsTest.noEventIsNamedAdImpression`: dựng một sự kiện của mỗi lớp con của `AdMetricEvent` (duyệt `AdMetricEvent::class.sealedSubclasses`, kiểm đủ 6), không tên nào là `ad_impression`, mọi tên không rỗng. Phần `af_revenue`: T-033 |
| KB-22, tiêu chí 3 (T-02) | Unit test | `NativeRevenueFallbackTest.noPaidEventIn2s_logsZeroOnce`: `onImpression(1_000)` ra 3 000; `onDue(2_999)` false; `onDue(3_000)` true; `onDue(4_000)` false (một lần). `paidEventFirst_cancelsFallback`: `onPaidEvent()` true rồi `onDue(3_000)` false. `paidEventAfterFallback_isNotLoggedAgain`: sau `onDue` true, `onPaidEvent()` false. `AdTelemetryTest.fallback_logsUsdZero`: lượt dự phòng ra `currency == "USD"`, `value == 0.0` |
| KB-22, tiêu chí 4 (cổng nhóm) | Unit test | `AdTelemetryTest.lifecycleEvents_needGroupSwitchAndServing`: `logFullScreenEvents = true`, `serving = true`: `loadSucceeded(INTERSTITIAL, ...)` ghi; `serving = false`: không; `logFullScreenEvents = false`: không; `NATIVE_SMALL` theo `logSlotEvents`, không theo khóa toàn màn hình. `switchChangedMidSession_appliesToNextEvent`: đổi giá trị lambda giữa hai lần gọi, lần sau theo giá trị mới. Khung rỗng thất bại ở vế ghi |
| KB-22, tiêu chí 4 (`ad_opportunity`) | Unit test | `AdTelemetryRulesTest.opportunityResult_ofDecision`: `SHOW` `shown`, `BUSY` `busy`, `NOT_READY` `not_ready`, `THROTTLED` `throttled`, `CONSENT_CLOSED` và `DISABLED` `null`. `AdTelemetryTest.opportunity_withoutPlacementOrResult_isSkipped`: vị trí `null` hay kết quả `null`: không ghi (A 06:197). `FirebaseAdMetricsTest.opportunity_params`: `ad_format`, `ad_placement`, `opportunity_result`, `load_state` |
| KB-22, tiêu chí 4 (`load_state`) | Unit test | `AdTelemetryRulesTest.loadStateAfterError_table`: offline với mọi mã ra `network`; 3, 9 `no_fill`; 1 với `sdkThrottled` `sdk_throttled`, không thì `invalid`; 8, 10, 11 `invalid`; 0 `internal`; 2, 7 `unknown`. `loadStateAtDecision_snapshot`: `Ready` `ready`, `Loading` `loading`, `Idle` với nhãn `no_fill` ra `no_fill`, `Idle` không nhãn `idle`, `WaitingRetry` với nhãn `network` ra `network` |
| KB-22, tiêu chí 4 (`ad_show_fail`) | Unit test | `AdTelemetryRulesTest.showFailReason_table`: 0 tới 4 ra năm lý do của mục 8.2, 9 ra `unknown`. `FirebaseAdMetricsTest.showFail_params`: `show_error_code` là chuỗi `"3"`, `show_fail_reason` |
| KB-22, tiêu chí 4 (`ad_clicked`, `ad_load_*`) | Unit test | `FirebaseAdMetricsTest.loadFail_params`: `ad_format`, `ad_placement`, `load_attempt` (`Long`), `latency_ms` (`Long`), `load_error_code` (chuỗi), `load_state`. `clicked_params`: `ad_format`, `ad_placement` |
| Bộ đếm không theo công tắc số liệu | Unit test | `AdTelemetryTest.loadCounter_updatesWhenLoggingOff`: cả hai khóa tắt và `serving = false`; `requestSent`, `loadFailed` hai lần cho cùng khe: `counter.failuresInARow(slot)` là 1 rồi 2, `RecordingAdMetrics` không có sự kiện nào; `requestSent`, `loadSucceeded`: về 0. Đối chứng (L-055): cùng chuỗi với khóa bật và `serving = true` cho cùng số đếm và có sự kiện. Khung rỗng thất bại vì `AdTelemetry` không đếm. T-021 dựa vào test này |
| `load_attempt`, `latency_ms` | Unit test | `LoadAttemptCounterTest.attemptCountsFailuresInARow`: gửi, lỗi, gửi, lỗi, gửi, thành công: `attempt` 1, 2, 3; sau thành công, lượt tiếp `attempt` 1; `failuresInARow` 2 trước thành công, 0 sau. `latencyFromThisRequest`: gửi ở 1 000, kết quả ở 1 250: 250. `callbackWithoutRequest_hasNoLatency`: `onLoaded` không có `onRequestSent` trước (banner tự làm mới): `latencyMs == null`. Khung rỗng thất bại ở attempt thứ hai |
| Vị trí của lượt nạp (8.2) | Unit test | `AdTelemetryRulesTest.loadPlacement_table`: `INTERSTITIAL`, `NATIVE_FULL_SCREEN`, `APP_OPEN`, `REWARDED` với màn bất kỳ ra `preload`; `NATIVE_SMALL` với màn ra mã màn, không màn ra `preload`; `BANNER` không màn ra `null` |
| Nhóm định dạng | Unit test | `AdTelemetryRulesTest.metricGroup_fullScreenFour`: đúng 4 định dạng ra `FULL_SCREEN` |
| Bộ ghi lỗi không làm hỏng luồng (8.2) | Unit test | `AdTelemetryTest.throwingMetrics_isSwallowedAndLogged`: `AdMetrics` ném `IllegalStateException`: `revenue`, `loadFailed` không ném; `RecordingAppLogger` có một dòng `warn` thẻ `Ads` chứa `IllegalStateException`, không chứa thông báo của lỗi |
| Không gửi thông báo lỗi của SDK | Unit test | `AdTelemetryTest.sdkMessage_neverInParams`: `loadFailed` với thông báo giả `"unit ca-app-pub-x"`: không giá trị tham số nào chứa chuỗi đó |
| Lượt dò không đo (A-04, HD-06) | Kiểm tra mã và thủ công | `isFullScreenAdReady`, `loadFullScreenAdIfNeeded` (T-022) không gọi `AdTelemetry`; rà soát độc lập kiểm. T-024 có test của màn loading |
| Doanh thu thật trên máy | Kiểm tra thủ công | Bản staging có Firebase staging thật, bật DebugView (`adb shell setprop debug.firebase.analytics.app <tên gói staging>`): mở màn mẫu, thấy `ad_impression_placement` với `ad_placement = sample_native_small`, `ad_format = NATIVE_SMALL`; màn xem thử: interstitial ra `INTER`, `gallery_interstitial`; không có sự kiện vòng đời khi hai khóa tắt; bật `is_log_ad_slot_events` trên Firebase staging thì có `ad_load_success`. Log `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt`, `SdkNativeAdLoader.kt`, `AdSlot.kt` (gắn `OnPaidEventListener`, `AdListener`, đọc `AdValue`, `ResponseInfo`), lớp `FirebaseAdMetrics` (dựng `Bundle`, gọi `FirebaseAnalytics`), `AnalyticsModule.kt`, `core/ads/build.gradle.kts`. `AdValue` không dựng được trên JVM (không có hàm tạo công khai) và SDK không chạy trên JVM, như T-010 với `setNativeAd`; mọi quyết định nằm trong hàm thuần và `AdTelemetry` đã có test; `firebaseParams` có test cho đúng dữ liệu mà `FirebaseAdMetrics` đưa vào `Bundle`. Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-24, chung một lần chạy với plan T-020 và T-022 (mục 4.1 của plan T-020), trong worktree của người viết plan trên commit `adf9450`, code thử không commit, đã gỡ hết. Phần của task này:
  - `:core:ads` thêm `implementation(project(":core:analytics"))` và `AdsController` nhận `Analytics` qua Hilt.
  - Hàm thử trong `:core:ads` gắn `setOnPaidEventListener` cho `InterstitialAd`, `AdView`, `NativeAd`, đọc `AdValue.valueMicros`, `currencyCode`, `precisionType`, `InterstitialAd.responseInfo.loadedAdapterResponseInfo?.adSourceName`, `AdView.responseInfo?.loadedAdapterResponseInfo?.adapterClassName`, `NativeAd.responseInfo?.mediationAdapterClassName`.
  - Lớp thử trong `:core:analytics` dựng `Bundle` bằng `putDouble`, `putLong`, `putString` và gọi `FirebaseAnalytics.getInstance(context).logEvent("ad_impression_placement", bundle)`, tiêm qua Hilt.
- **Lệnh:** `scripts/check.sh` (`JAVA_HOME` của Android Studio, `ANDROID_HOME=/Users/khacnhat/Library/Android/sdk`), ba lần như plan T-020.
- **Kết quả:** mọi lời gọi trên biên dịch qua với `play-services-ads` 25.5.0 và Firebase BOM 34.19.0 hiện có; "Module dependency rule OK." với `:core:ads` phụ thuộc `:core:analytics`; lint `:app:lintStagingDebug` không báo gì về lớp thử; lần 3 "Cổng chất lượng: QUA". Lần 1 lộ `FullScreenAdsTest` phải sửa khi constructor đổi (đã đưa vào mục 3.2). Chưa chạy thử: sự kiện doanh thu thật (cần SDK trên máy, kiểm thủ công), câu chữ lỗi tự chặn của SDK (mục 3.4), `Bundle` trên JVM (không dùng: test kiểm `firebaseParams`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** thu thập dữ liệu (điểm 4: sáu sự kiện phân tích mới, đã có trong spec mục 6), module core dùng chung `:core:ads`, `:core:analytics` và file build `core/ads/build.gradle.kts` (điểm 7). Không thêm quyền, SDK, vị trí; không đổi hành vi quảng cáo. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là dữ liệu gửi ra khỏi máy và đổi cách đọc doanh thu (risk-scoring.md, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module core (`:core:ads`, `:core:analytics`), API công khai mới `AdMetrics`, `AdMetricEvent`, `RecordingAdMetrics`, constructor `AdsController` |
| B. Độ rõ của yêu cầu | 1 | Mục 8.2 liệt kê đủ tham số và luật; AI tự quyết cách đo lỗi tự chặn, ranh giới phần AppsFlyer với T-033 (mục 8) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi ánh xạ và cổng có unit test; sự kiện doanh thu thật của SDK và hiện trên DebugView chỉ kiểm bằng tay |
| D. Ảnh hưởng chéo | 3 | `AdsController.kt`, `core/ads/build.gradle.kts` cũng do T-019 tới T-022 sửa; T-024 tới T-033 gọi cùng bộ ghi; ghi sai làm đếm đôi doanh thu ở mọi app |
| E. Độ mới của kỹ thuật | 2 | `OnPaidEventListener`, `ResponseInfo` và tham số kiểu số của Firebase chưa dùng trong dự án; phổ biến, có tài liệu của Google; đã chạy thử biên dịch (mục 4.1) |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `SdkNativeAdLoader.kt`, `AdSlot.kt`, `core/ads/build.gradle.kts`, `AnalyticsModule.kt`. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện; test không có `AdsController` nên không có quảng cáo).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **Phụ thuộc T-019** (chưa merge): `AdsFlags.global` (`logFullScreenEvents`, `logSlotEvents`), 11 giá trị `AdFormat`. T-019 thêm `AppsFlyerSettings.kt` và phụ thuộc `:core:config` vào `:core:analytics`; task này không đụng hai chỗ đó.
- **Phụ thuộc T-020 và T-022** (chưa merge; ticket T-023 chỉ ghi T-019, T-010; câu hỏi 1): `canRequestAds`, `InternetConnectivity` (T-020); `FullScreenShowDecision`, `FullScreenLoadState`, `decideFullScreenTurn`, `isFullScreenAdReady`, `loadFullScreenAdIfNeeded` (T-022). Các file này do T-020, T-022 **tạo**; plan này dùng, không sửa, nên không có trong mục 3.2 (L-054).
- **File chung với các plan đợt 3 (L-054):**

| File | T-019 | T-020 | T-022 | T-023 (plan này) |
| --- | --- | --- | --- | --- |
| `core/ads/.../AdsController.kt` | Sửa | Sửa | Sửa | Sửa |
| `core/ads/build.gradle.kts` | | Sửa (`:core:network`) | | Sửa (`:core:analytics`) |
| `core/ads/src/test/.../FullScreenAdsTest.kt` | Sửa | Sửa | | Sửa |
| `core/ads/.../FullScreenShowDecision.kt` | | | Tạo | Dùng |
| `core/ads/.../FullScreenLoadState.kt` | | | Tạo | Dùng |
| `core/network/.../InternetConnectivity.kt` | | Tạo | | Dùng |
| `core/analytics/build.gradle.kts` | Sửa (`:core:config`) | | | không sửa |
| `core/analytics/.../AdMetrics.kt` | | | | **Tạo** (T-033 thêm bản AppsFlyer) |
| `core/ads/.../LoadAttemptCounter.kt` | | | | **Tạo** (T-021 dùng `failuresInARow`) |

- Task khác có thể xung đột: T-021 dùng `LoadAttemptCounter.failuresInARow` cho thời gian chờ thử lại (F-05, O-05), không viết bộ đếm thứ hai; T-024 ghi cơ hội `not_ready` khi vỏ app tự bỏ app open (A-06) qua `AdTelemetry.opportunity`; T-025 ghi cơ hội của vị trí `resume`; T-026, T-028 ghi cơ hội `shown`, `not_ready` của combo và ô pool; T-031 thêm `ad_reward_earned`, `reward_dialog` vào `AdMetricEvent`; T-033 thêm bản AppsFlyer của `AdMetrics` (gửi `ad_placement` là `""` khi chưa có, mục 8.3) và công tắc `is_log_af_*`.
- Đề xuất thứ tự chạy: T-019, T-020, T-022, rồi task này, rồi T-021.

## 7. Điều kiện dừng riêng

- Tên của T-019, T-020, T-022 khác khi task chạy: không có `AdsGlobalFlags.logFullScreenEvents`, `logSlotEvents`, `AdFormat` đủ 11 giá trị, `InternetConnectivity`, `FullScreenShowDecision` hay `FullScreenLoadState`. Dừng, không tự tạo bản thay.
- `checkModuleDependencies` từ chối `:core:ads` phụ thuộc `:core:analytics`, hay có vòng phụ thuộc qua `:core:config`.
- Cần sửa `NativeAdLoader`, `loadOneNativeAd`, `FakeNativeAdLoader` hay test có sẵn của native để nối sự kiện.
- Kiểm thủ công thấy hai sự kiện doanh thu cho một lượt hiển thị (ví dụ cả `onPaidEvent` và dự phòng), hay thấy `ad_impression` do app gửi.
- Cần thêm tham số hay sự kiện ngoài mục 8.2 (ví dụ lớp adapter vào Firebase).

## 8. Câu hỏi cho người duyệt

1. **Ticket T-023 chỉ phụ thuộc T-019, T-010, nhưng plan dùng tên của T-020 và T-022** (cổng đồng ý, Internet chung, thứ tự quyết định của cổng toàn màn hình cho `ad_opportunity`, trạng thái nạp cho `load_state`).
   - Đề xuất của AI: thêm T-022 vào `depends_on` của ticket T-023 (T-022 đã sau T-020 theo câu hỏi 1 của plan T-020, T-022). Phương án khác: T-023 chạy trước T-022 và tự ghi cơ hội theo cổng cũ; khi đó T-022 phải giữ các lời gọi ghi cơ hội và plan T-022 phải thêm việc này.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

2. **Ranh giới với T-033 cho KB-22.** Tiêu chí 1 có phần AppsFlyer, tiêu chí 2 có `af_revenue`, tiêu chí 5 và 6 chỉ về AppsFlyer.
   - Đề xuất của AI: task này làm phần Firebase và interface chung; T-033 làm phần AppsFlyer trên cùng `AdMetricEvent.ImpressionRevenue` (có sẵn lớp adapter và giá trị), nên KB-22 chỉ đạt đủ sau T-033.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

3. **Mã lỗi 1 là `sdk_throttled` "nếu SDK báo tự chặn".** SDK không có mã riêng; cách khả dĩ là đọc câu chữ của thông báo lỗi trên máy (không gửi đi).
   - Đề xuất của AI: đọc thông báo, chứa "Too many recently failed requests" thì là tự chặn; kiểm câu chữ trên staging. Không chắc thì dùng phương án dự phòng ở mục 3.4 (mã 1 luôn `invalid`).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

4. **Banner chưa có vị trí nào dùng** (dự án mẫu bỏ banner ở màn mẫu, câu hỏi 3 của spec).
   - Đề xuất của AI: vẫn nối banner trong `AdSlot.kt` để thanh đáy (T-029, T-030) có sẵn; kiểm thủ công banner chờ T-029.
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

5. **Native toàn màn hình đứng riêng ghi cơ hội như interstitial.** Mục 8.2 chỉ nói cổng inter và native sau inter.
   - Đề xuất của AI: ghi `ad_opportunity` cho mỗi lượt gọi hiện của vị trí native toàn màn hình (định dạng `NATIVE_FULL`), cùng thứ tự `busy`, `not_ready`, `throttled`, vì nó đi qua cùng cổng (T-022, câu hỏi 2).
   - Trả lời: theo đề xuất của AI (anh Nhật, 2026-10-24).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-24, sau khi anh trả lời các câu ở mục 8 và sau rà soát độc lập trước phiên của RUN-20261024-02 (các điểm nên sửa đã sửa trong plan trước khi duyệt).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
