---
ticket: T-103
title: Giãn dần thời gian thử lại native khi khe chưa có quảng cáo
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 18.6M        # cỡ M, số gợi ý 18.6M của scripts/token-usage.py estimate (2026-11-13, trung vị 31 task M); không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-103: GIÃN DẦN THỜI GIAN THỬ LẠI NATIVE KHI KHE CHƯA CÓ QUẢNG CÁO

## 1. Tóm tắt

Chỗ native chưa giữ quảng cáo (khe, hàng trong danh sách, pool trống, native toàn màn hình) thử lại sau 5, 10, 20, 40 giây rồi mỗi 60 giây thay cho cố định 5 giây; luật khi đang giữ quảng cáo giữ nguyên. Mỗi đường đếm lỗi bằng một bộ đếm chung mới (`NativeRetryCount`) về 0 khi nạp được, khi app quay lại foreground và khi mạng về; khe và hàng còn về 0 khi mở màn mới vì luồng của chúng dựng lại. Kiểm chứng bằng unit test của `AdRetryPolicy` và `NativeRetryCount`, và test JVM với `AdsController` thật, đồng hồ ảo của Robolectric, đếm yêu cầu trong 10 phút và khoảng chờ sau từng dịp đặt lại, cho từng đường.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-24 | Tiêu chí 1 (ghi chú 2026-11-11): quảng cáo toàn màn hình lỗi thì thử lại sau 2, 4, 6, 8 rồi 10 giây, không gồm native toàn màn hình | Interstitial, app open, rewarded giữ `fullScreenDelayMillis` (F-05); chỉ `NATIVE_FULL_SCREEN` đổi sang chuỗi của tiêu chí 2. Test có sẵn của interstitial giữ nguyên |
| KB-24 | Tiêu chí 2 (đổi 2026-11-11): đang có quảng cáo thì chờ tăng dần tới 60 giây; chưa có quảng cáo, kể cả native toàn màn hình, thì 5, 10, 20, 40 rồi mỗi 60 giây, không bỏ cuộc; nạp được, app quay lại foreground, mạng về, hay mở màn mới có chỗ đó thì lần lỗi kế tiếp chờ lại từ 5 giây | "Chỗ" là một đường gửi có bộ đếm riêng: một khe (native small, thu gọn, hay khe vẽ định dạng khác), một hàng của danh sách, một pool (trống nghĩa là `free.size + inUse.size == 0`, không phải ô trống), native toàn màn hình của một vị trí. "Quay lại foreground" là `foregroundState` từ false sang true (activity của app hiện lại), không phải lúc màn che toàn màn hình đóng (KB-27). Dịp mở màn mới: khe và hàng có sẵn (luồng mới, bộ đếm mới); native toàn màn hình không áp (ticket); pool: không đặt lại, câu hỏi 1 |
| KB-24 | Tiêu chí thêm 2026-11-11: foreground suốt 10 phút, không dịp đặt lại, không lần nào có quảng cáo thì một chỗ gửi không quá 15 yêu cầu | Với lỗi tức thì: gửi ở giây 0, 5, 15, 35, 75, 135, rồi mỗi 60 giây tới 555, tổng 13 yêu cầu trong 600 giây. Test khẳng định đúng 13 (chặt hơn 15) và mốc của từng yêu cầu |
| KB-24 | Tiêu chí 4: mạng về thì mọi loại quảng cáo tải lại | Khe, hàng, pool đã cắt ngắn lượt chờ qua `nativeRetryResets` (S-24), giữ nguyên. Native toàn màn hình hôm nay không nghe tín hiệu này; với chờ tới 60 giây, plan cho lượt chờ của native toàn màn hình cũng bị cắt ngắn khi mạng về (câu hỏi 2) |
| KB-24 | Tiêu chí 5: app ở nền thì native không gửi yêu cầu | Giữ nguyên: lượt tới hạn khi app ở nền vẫn chờ foreground (O-05). Test có sẵn giữ nguyên |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); ticket không đổi giao diện.

ADR liên quan: ADR-0045 (quảng cáo theo AdsKit PARTNER, `:core:ads`), ADR-0019 (log), ADR-0029, ADR-0031 (viết lại câu khẳng định cũ trái luật mới), ADR-0001.

## 3. Giải pháp

### 3.1. Cách làm

**Luật chờ (`AdRetryPolicy.nativeDelayMillis`).** `holdsAd = false`: `n = failuresInARow.coerceIn(1, 5)`, chờ `(5_000L shl (n - 1)).coerceAtMost(60_000L)`, tức 5, 10, 20, 40, 60 giây với n = 1..5 và 60 giây từ đó; n bị chặn trước khi dịch bit nên n rất lớn (`Int.MAX_VALUE`) không tràn; n dưới 1 tính là 1. `holdsAd = true` giữ nguyên (2 giây nhân đôi, n tối đa 10, trần 60 giây). Hằng `NATIVE_NO_AD_MILLIS` đổi thành `NATIVE_NO_AD_FIRST_MILLIS = 5_000L`, `NATIVE_NO_AD_MAX_FAILURES = 5`. KDoc ghi O-05 mới.

**Bộ đếm chung (`NativeRetryCount`, file mới `NativeRetryCount.kt` của `:core:ads`).** Lớp thuần, không luồng, một đối tượng cho một chỗ:

```kotlin
internal class NativeRetryCount(private val resets: StateFlow<Int>) {
    private var failures = 0
    private var since = resets.value
    /** The failures in a row, this one counted; 1 again when [resets] moved since the last count. */
    fun onFailure(): Int { if (resets.value != since) { since = resets.value; failures = 0 }; return ++failures }
    fun onLoaded() { failures = 0; since = resets.value }
    fun clear() { failures = 0; since = resets.value }
}
```

Bộ đếm không tự hủy lượt chờ đang chạy: nó chỉ quyết khoảng chờ của lần lỗi kế tiếp, đúng chữ của KB-24 ("lần lỗi kế tiếp chờ lại từ 5 giây"). Việc cắt ngắn lượt chờ khi mạng về vẫn do `nativeRetryResets` (S-24) như hôm nay.

**Tín hiệu đặt lại (`AdsController.nativeFailureResets: StateFlow<Int>`, `MutableStateFlow(0)` riêng tư).** Tăng 1 khi:
- `foregroundState` đổi từ false sang true (trong `onForegroundChanged` của listener `AdsActivityTracker`, và trong `setAppForegroundForTest` để test đi cùng đường; viết một hàm riêng `onAppForeground(foreground)` mà cả hai gọi, chỉ tăng ở cạnh lên);
- mạng về: cùng chỗ gọi `preloader.onNetworkReturned()` (`networkReturns(online).collect { ... }`), tăng trước khi gọi để lượt thử bị cắt ngắn đọc được giá trị mới.

Không tăng khi màn che toàn màn hình đóng (cửa sổ trong màn, KB-27): đó không phải quay lại foreground. Không đổi `AdPreloader` (tín hiệu cắt ngắn và tín hiệu đặt lại tách nhau, để quay lại foreground không làm lượt chờ chưa tới hạn chạy sớm).

**Từng đường.**

| Đường | Bộ đếm hôm nay | Sau ticket | Dịp đặt lại |
| --- | --- | --- | --- |
| Khe native small, thu gọn (`singleAdSlotStates` gọi `retryingNativeAdStates`, lượt `SINGLE_AD` của `NativeAdSource`); khe vẽ định dạng khác hay khe không có nguồn đơn (`retryingNativeAdStates` trực tiếp trong `AdsController`) | biến `failures` của từng luồng | `NativeRetryCount(failureResets)` tạo trong luồng; `retryingNativeAdStates` nhận thêm tham số `failureResets: StateFlow<Int>` (mặc định `MutableStateFlow(0)` cho test có sẵn), gọi `onFailure()` cho `nativeDelayMillis(n, holdsAd = false)` | nạp được: luồng chuyển sang làm mới hay giữ quảng cáo, không thử lại ở đây nữa (luật khi giữ quảng cáo của `NativeAdRefresh` giữ nguyên); foreground, mạng về: `failureResets`; mở màn mới: luồng mới, bộ đếm mới (có sẵn) |
| Hàng native small trong danh sách (`inListRowStates` gọi `retryingNativeAdStates`) | như trên | như trên; `inListRowStates` nhận và chuyển `failureResets` | như trên; hàng cuộn đi rồi về vẫn là cùng luồng nên không đặt lại (hôm nay cũng vậy) |
| Pool (`NativeAdPool`: medium, thẻ trong tập hợp/ô lưới, giới thiệu, `native_step`) | trường `failures`, về 0 khi nạp được hay `reopen()` | trường `retryCount = NativeRetryCount(failureResets)` (tham số mới của hàm dựng, mặc định `MutableStateFlow(0)`); `failures++` thay bằng `retryCount.onFailure()`; nạp được gọi `onLoaded()`, `reopen()` gọi `clear()`. Khoảng chờ vẫn chọn `holdsAd = free.size + inUse.size > 0` | nạp được, `reopen()`, foreground, mạng về. Mở màn mới: không (câu hỏi 1). Tín hiệu đặt lại áp cả khi pool đang giữ quảng cáo vì một pool có một bộ đếm (câu hỏi 3) |
| Native toàn màn hình (`FullScreenReloads`, lượt sau interstitial và lượt độc lập, cùng một lượt nạp theo vị trí) | `LoadAttemptCounter.failuresInARow(preloadCounterKey(...))`, bộ đếm của số liệu `load_attempt` | `FullScreenReloads` giữ `NativeRetryCount` riêng cho mỗi vị trí `NATIVE_FULL_SCREEN` (tham số mới `failureResets`); `onLoadFailed` của định dạng này dùng `count.onFailure()`; hàm mới `onLoaded(placement)` gọi `onLoaded()`, `AdsController` gọi nó trong `forNativeAd` của lượt nạp native toàn màn hình. `LoadAttemptCounter` không đổi, nên `load_attempt` của số liệu không đổi (câu hỏi 4). Định dạng khác giữ `failuresInARow` và `fullScreenDelayMillis` | nạp được, foreground, mạng về; mở màn mới không áp (ticket). Mạng về còn cắt ngắn lượt chờ đang chạy: `schedule` của định dạng này chờ `withTimeoutOrNull(wait) { retryResets.first { it != generation } }` thay cho `delay(wait)` (tham số mới `retryResets` = `preloader.nativeRetryResets`), rồi vẫn chờ foreground như hôm nay (câu hỏi 2) |

Nhịp của ô pool (`PoolCellPolicy`, 500 ms và 2 giây) chỉ gọi `claim()` của pool, không gửi gì: `claim()` gọi `refill()`, `refill()` trả về ngay khi `job != null` (đang chờ thử lại), nên ô không gửi thêm yêu cầu nào trong lúc pool chờ. Test pool 10 phút có ô đang thu thập để khẳng định điều này.

**Điểm nối test cho native toàn màn hình.** Lượt nạp native toàn màn hình hôm nay gọi thẳng `AdLoader.Builder` nên test JVM với `AdsController` thật không điều khiển được kết quả. Thêm `@VisibleForTesting internal var nativeFullScreenRequestForTest: ((AdPlacement) -> Boolean)? = null` trong `AdsController`: khi khác null, phần gửi (khối `try` dựng `AdLoader` và `loadAd`) được thay bằng lời gọi này, trả true thì đi đường nạp được với một `LoadedNativeAd` giả (`FakeLoadedNativeAd` có sẵn ở `FakeNativeAds.kt`), false thì đi đường lỗi `onNativeFullScreenLoadFailed(placement, events, 3, null)`. Mọi phần trước lượt gửi (chờ foreground, cổng, công tắc, `events`, `mayRequestNow`) vẫn chạy thật. Nếu nhánh nạp được khó tách khỏi `NativeAd` của SDK thì chỉ cần đường lỗi cho test đếm, còn dịp "nạp được" của native toàn màn hình kiểm ở `FullScreenReloadsTest` (mục 3.4).

**Log.** Giữ nguyên dạng dòng: `Native <khe> retry in <ms> ms (failures <n>)`, `Pool <mã> failed, retry in <ms> ms (failures <n>)`, `Retry <vị trí> in <ms> ms (failures <n>, code <c>)`; `<n>` là số của bộ đếm mới (đếm lại từ 1 sau dịp đặt lại). Thêm một dòng `Native retry counters reset: foreground` / `: network back` thẻ `Ads` mỗi lần `nativeFailureResets` tăng (ADR-0019; không dữ liệu cá nhân).

**Tài liệu.** `docs/ops/ads-business-rules.md`: O-05 (chuỗi mới, các dịp đặt lại, native toàn màn hình theo O-05 chứ không theo F-05) và dòng "Thử lại native" của bảng hằng số mục 11 ("2 giây nhân đôi tới 60 giây (n tối đa 10); chưa có quảng cáo: 5, 10, 20, 40 rồi 60 giây"), cột nguồn thêm "spec ads, câu hỏi 62 (khác AdsKit)". `docs/ops/ads-guide.md`: ba chỗ đang ghi "5 giây" (hàng danh sách, mục Thử lại sau lỗi nạp, mục Pool native, kể cả ví dụ log `retry in 5000 ms`).

Mẫu làm theo: `AdRetryPolicy.fullScreenDelayMillis` (chặn n trước khi nhân), `NativeAdPool` dùng `retryResets` (S-24), test `AdsControllerInListRowTest.inListRow_failedThenRetriesAt5s_listRowMetrics` (`AdsController` thật, `nativeLoaderForTest = ThrowingNativeAdLoader()`, `idleFor`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdRetryPolicy.kt` | `nativeDelayMillis(n, holdsAd = false)`: 5, 10, 20, 40 rồi 60 giây; KDoc O-05 |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeRetryCount.kt` | Bộ đếm lỗi liên tiếp của một chỗ native, về 0 khi nạp được, `clear()` hay khi tín hiệu đặt lại đổi |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | `retryingNativeAdStates` nhận `failureResets`, đếm bằng `NativeRetryCount`; KDoc bỏ "5 s" |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | `inListRowStates` nhận và chuyển `failureResets`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSources.kt` | `singleAdSlotStates` nhận và chuyển `failureResets`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt` | Tham số `failureResets`, bộ đếm `NativeRetryCount` thay `failures`; `reopen()` gọi `clear()`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenReloads.kt` | Bộ đếm riêng cho `NATIVE_FULL_SCREEN`, `onLoaded(placement)`, tham số `failureResets`, `retryResets`; lượt chờ của native toàn màn hình bị cắt ngắn khi mạng về |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `nativeFailureResets` (tăng khi foreground trở lại, khi mạng về), log đặt lại; truyền vào bốn đường (`singleAdSlotStates`, `retryingNativeAdStates`, `inListRowStates`, `NativeAdPool`) và `FullScreenReloads`; gọi `fullScreenReloads.onLoaded` khi native toàn màn hình nạp được; điểm nối `nativeFullScreenRequestForTest` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdRetryPolicyTest.kt` | Thay `nativeDelay_withoutAd_isAlways5s` bằng test chuỗi mới (mục 3.5, mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeRetryCountTest.kt` | Unit test bộ đếm |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | Câu khẳng định "mỗi 5 giây" viết lại theo chuỗi mới; test đặt lại bằng `failureResets` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolTest.kt` | `failures_retryDelays` viết lại theo chuỗi mới; test đặt lại (nạp được, `reopen`, `failureResets`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenReloadsTest.kt` | Hàm dựng `reloads(...)` thêm `failureResets`, `retryResets`; test native toàn màn hình theo chuỗi mới, `onLoaded`, mạng về cắt ngắn |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeRetryTest.kt` | Test JVM với `AdsController` thật và đồng hồ ảo: đếm 10 phút và từng dịp đặt lại cho khe, hàng, pool, native toàn màn hình |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInListRowTest.kt` | Chỉ khi câu khẳng định 5 giây ở lần lỗi thứ hai trở đi trái luật mới (mục 3.5); lần thử đầu vẫn 5 giây |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Chỉ khi có câu khẳng định thử lại cố định 5 giây từ lần lỗi thứ hai (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsStateTest.kt` | Như trên (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurnsTest.kt` | Như trên (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeComboTest.kt` | Như trên (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStepAdStatesTest.kt` | Như trên (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourcesTest.kt` | Như trên (mục 3.5) |
| Sửa | `docs/ops/ads-business-rules.md` | O-05 và dòng "Thử lại native" của mục 11 |
| Sửa | `docs/ops/ads-guide.md` | Ba chỗ ghi thử lại 5 giây của native (hàng danh sách, Thử lại sau lỗi nạp, Pool native) |

Không file nào khác đọc hằng số này (`grep -rn "nativeDelayMillis\|NATIVE_NO_AD" scripts/ .github/ */src` chỉ thấy các file trên). `scripts/ads-plan.py` không đọc luật thử lại.

Các test sau nhắc tới `docs/ops/ads-business-rules.md` hay `docs/ops/ads-guide.md` chỉ trong chú thích, hay chép tay mục khác (5.7, 8, 8.1 đến 8.3, 10, 10.1, MC-02, mục S, mục 4), không đọc file lúc chạy và không đọc O-05 hay dòng "Thử lại native" của mục 11, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Luật áp ngay cho mọi app, như các luật O-05 khác.
- Quảng cáo: Không thêm hay bớt vị trí; đổi nhịp gửi yêu cầu khi lỗi của mọi vị trí native (KB-24, câu hỏi 62 của spec).
- Sự kiện phân tích: Không chạm. `load_attempt` vẫn đọc `LoadAttemptCounter`, không đổi.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `StateFlow`, `withTimeoutOrNull` (kotlinx-coroutines) đã có trên classpath biên dịch của `:core:ads`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** điểm nối `nativeFullScreenRequestForTest` đi được đường nạp được của native toàn màn hình mà không cần `NativeAd` của SDK.
- **Điều kiện chuyển:** nhánh nạp được của điểm nối cần dựng `com.google.android.gms.ads.nativead.NativeAd` (lớp trừu tượng của SDK) hay đổi kiểu của `onNativeFullScreenLoaded`, hoặc test viết trước vẫn thất bại vì cùng lỗi dựng sau 2 lần sửa.
- **Cách làm thay:** điểm nối chỉ giả đường lỗi; dịp "nạp được" của native toàn màn hình kiểm ở `FullScreenReloadsTest` (`onLoaded` rồi lỗi thì chờ 5 giây) cộng một test của `AdsController` khẳng định `forNativeAd` gọi `fullScreenReloads.onLoaded` bằng log `Native full-screen <vị trí> loaded`, theo thứ tự với dòng `Retry` kế tiếp khi dùng `putPreloadedNativeFullScreenForTest`. Tiêu chí và các test khác giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Từ `scripts/existing-tests.py` (2026-11-13): 741 test dùng tên của các file plan sửa; 4 test ở nhóm "Cần xem trước", 63 ở nhóm "Nên xem". Bốn test "Cần xem trước" (`BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `InListAdsTest.failedRow_showsAdAfterRetry`, `InListAdsTest.positions_followEvery`, `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction`) đo kích thước hay vị trí của banner, hàng, hộp thoại: plan không đổi giao diện, giữ nguyên; `failedRow_showsAdAfterRetry` chỉ có một lần lỗi trước lượt có quảng cáo, lần thử đầu vẫn sau 5 giây, giữ nguyên. Nhóm "Nên xem" phần lớn chỉ dựng `AdsController`, gọi `setAppForegroundForTest` hay `mayRequestNow`, không khẳng định khoảng chờ thứ hai trở đi: giữ nguyên. Các test dưới khẳng định thử lại cố định 5 giây từ lần lỗi thứ hai, trái tiêu chí 2 mới của KB-24 (câu hỏi 62 của spec); viết lại theo ADR-0031, giữ ý định (không bỏ cuộc, đếm riêng, thứ tự), không nới.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Chưa giữ quảng cáo thì chờ 5 giây với mọi n | 5, 10, 20, 40 rồi 60 giây | `AdRetryPolicyTest.nativeDelay_withoutAd_isAlways5s` | Đổi tên `nativeDelay_withoutAd_5_10_20_40_then60s`: n = 0, 1, 2, 3, 4, 5, 6, 100, `Int.MAX_VALUE` ra 5 000, 5 000, 10 000, 20 000, 40 000, 60 000, 60 000, 60 000, 60 000 |
| Khe lỗi liên tiếp thử lại mỗi 5 giây | 5, 10, 20, 40, 60 giây | `NativeAdRetryTest.keepsFailing_every5s_neverGivesUp` | Đổi tên `keepsFailing_backsOff_neverGivesUp`: lỗi ở mỗi lượt, `requestTimes` = `[0, 5_000, 15_000, 35_000, 75_000, 135_000, 195_000]` |
| Lần thử đầu sau 5 giây | Giữ (n = 1) | `NativeAdRetryTest.failureWithoutAd_retriesAfter5s`, `background_control_foregroundRetriesAt5s`, `gateClosedAtSend_control_openGateRetriesAt5s`, `loaderNotBuilt_reportsCode0ToEventsAndRetriesAt5s`; `AdsControllerInListRowTest.inListRow_failedThenRetriesAt5s_listRowMetrics`; `InListAdsTest.failedRow_showsAdAfterRetry`; `InListAdsStateTest.unavailableSlot_retriedAfter5s_notOnScrollBack` | Giữ. Phiên đọc lại từng test: lần lỗi thứ hai trở đi mà còn khẳng định 5 giây thì viết lại theo chuỗi mới như dòng trên |
| Hàng lỗi liên tiếp thử lại mỗi 5 giây | 5, 10, 20 giây | `InListAdsStateTest.failedRow_keepsRetryingEvery5s_neverGivesUp` | Đổi tên `failedRow_keepsRetrying_backsOff_neverGivesUp`: lỗi ở 0, 5 000, 15 000, `sendTimes()` = `[0, 5_000, 15_000, 35_000]` (các mốc `at(...)` đổi theo) |
| Hai hàng lỗi cùng lúc, hàng 1 lỗi lần hai thì thử lại sau 5 giây | Lần lỗi thứ hai chờ 10 giây | `InListAdsStateTest.twoRowsFailTogether_retryIndependently` | Câu mới: `sendTimes(1)` = `[0, 5_000, 15_000]` (mốc `at(10_000)` thành `at(15_000)`, số `sends.size` theo đó); ý "mỗi hàng đếm riêng" giữ: hàng 0 có quảng cáo ở 5 000 không làm hàng 1 về 5 giây |
| Pool rỗng lỗi liên tiếp thử lại mỗi 5 giây | 5, 10, 20 giây | `NativeAdPoolTest.failures_retryDelays` (phần pool rỗng) | Câu mới: `loads.times` = `[0, 5_000, 15_000, 35_000]` (mốc `at(10_000)`, `at(15_000)` thành `at(15_000)`, `at(35_000)`); phần pool giữ quảng cáo (2, 4, 8 giây, thành công đếm lại từ 2) giữ nguyên |
| Native toàn màn hình chờ 5 giây theo `failuresInARow` của `LoadAttemptCounter` | Chờ theo bộ đếm riêng của `FullScreenReloads`, lần 1 là 5 giây | `FullScreenReloadsTest.nativeFullScreen_retryWaitsForForeground` (`failuresInARow = { 1 }`, chờ 5 000), `twoPlacements_failingTogether_retryOnTheirOwnSchedule` (native ở 5 000) | Giữ câu khẳng định (lần lỗi đầu của native toàn màn hình vẫn 5 000); chỉ đổi phần dựng (`reloads(...)` thêm `failureResets`, `retryResets` mặc định) theo ADR-0029. `failuresInARow` không còn được đọc cho định dạng này |
| Native toàn màn hình lỗi liên tiếp chờ 5 giây mỗi lần (không có test khẳng định lần thứ hai) | 5, 10, 20, 40, 60 giây | `FullScreenReloadsTest.failures_retryAt2_4_6_8_10_thenEvery10s` (interstitial) | Giữ: interstitial không đổi (KB-24, tiêu chí 1). Test mới cho native toàn màn hình ở mục 4 |
| Combo hay lượt độc lập của native toàn màn hình không có quảng cáo | Không đổi (tự đóng sau hạn chót, `nativeLoadResults`) | `NativeFullScreenTurnsTest.*`, `AdsControllerNativeComboTest.*` | Giữ: các test này không khẳng định khoảng chờ thử lại; phiên đọc lại, gặp câu khẳng định thử lại thứ hai sau 5 giây thì viết lại theo chuỗi mới |
| Khe có nguồn đơn chưa giữ quảng cáo thử lại 5 giây | Như khe | `NativeAdSourcesTest.*`, `NativeStepAdStatesTest.*` | Giữ phần một lần lỗi; câu khẳng định lần thử thứ hai trở đi sau 5 giây (nếu có) viết lại theo chuỗi mới |
| Pool về 0 bộ đếm khi `reopen()` | Giữ | `NativeAdPoolTest.reopen_sendsAgain` | Giữ |
| Mạng về cắt ngắn lượt chờ của khe và pool | Giữ, thêm: lần lỗi sau đó chờ 5 giây | `NativeAdPoolTest.networkBack_cutsRetryWaitShort`, các test `retryResets` của `NativeAdRetryTest` | Giữ |

## 4. Kiểm thử

Quy ước của mọi test JVM với `AdsController` thật (`AdsControllerNativeRetryTest`, Robolectric, đồng hồ ảo của looper chính, `idleFor(ms)` như `AdsControllerInListRowTest`): `testAdsController(unitIds = InScreenOnlyUnitIds, connectivity = fake, logger = logger)`, `nativeLoaderForTest = ThrowingNativeAdLoader()` (khe, hàng, pool: mỗi lượt gửi lỗi ngay, đếm `calls` và mốc gửi), `start(...)`, `openConsentGateForTest()`, `setAppForegroundForTest(true)`, `idle()`. Native toàn màn hình: `nativeFullScreenRequestForTest = { false }` (ghi mốc gửi), lượt đầu bằng `loadFullScreenAdIfNeeded(GALLERY_NATIVE_FULL_SCREEN)` với khóa `is_show_gallery_native_full_screen` bật trong `FakeRemoteSettings`. `fake` là `InternetConnectivity` có `connected = MutableStateFlow(true)`; "mạng về" là đặt false, rồi true, rồi `idleFor(NETWORK_SETTLE_MILLIS)`. Điểm nối `nativeLoaderForTest` thay đúng bộ nạp (lượt gửi vẫn đi qua cổng, cửa sổ trong màn và bộ đếm thật); câu khẳng định đọc số lần gọi và mốc gửi từ chính bộ nạp giả, không từ log. "Mốc" dưới đây là mili giây từ lượt gửi đầu.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-24, tiêu chí 2: chưa giữ quảng cáo chờ 5, 10, 20, 40, 60 giây, không tràn | Unit test | `AdRetryPolicyTest.nativeDelay_withoutAd_5_10_20_40_then60s` (n = 0..6, 100, `Int.MAX_VALUE`, mục 3.5) |
| KB-24, tiêu chí 2: đang giữ quảng cáo giữ chuỗi cũ | Unit test | `AdRetryPolicyTest.nativeDelay_holdingAd_doublesUpTo60s` (có sẵn, giữ) |
| KB-24, tiêu chí 1: interstitial giữ 2, 4, 6, 8, 10 giây | Unit test | `AdRetryPolicyTest.fullScreenDelay_growsBy2sUpTo10s`, `FullScreenReloadsTest.failures_retryAt2_4_6_8_10_thenEvery10s` (có sẵn, giữ) |
| Bộ đếm: đếm, về 0 khi nạp được, khi `clear()`, khi tín hiệu đổi; tín hiệu đổi hai lần giữa hai lỗi vẫn chỉ về 1 | Unit test | `NativeRetryCountTest.counts_1_2_3`, `loaded_countsFrom1Again`, `clear_countsFrom1Again`, `resetsMoved_countsFrom1Again_once` (đối chứng: tín hiệu không đổi thì lần thứ tư ra 4), `resetsMovedTwice_stillOne` |
| Khe: luôn lỗi 10 phút ở foreground, không dịp đặt lại | Test JVM, `AdsController` thật, đồng hồ ảo | `AdsControllerNativeRetryTest.slot_alwaysFailing_10min_13Requests`: thu thập `nativeAd(GALLERY_NATIVE_SMALL, ...)`, `idleFor(600_000)`; mốc gửi `[0, 5_000, 15_000, 35_000, 75_000, 135_000, 195_000, 255_000, 315_000, 375_000, 435_000, 495_000, 555_000]`, 13 lần, không quá 15 |
| Hàng: như trên | Test JVM như trên | `AdsControllerNativeRetryTest.inListRow_alwaysFailing_10min_13Requests`: `inListRowStates(GALLERY_INLIST_SMALL, 0, MutableStateFlow(true))`, cùng mốc |
| Hai hàng của một vị trí lỗi cùng lúc: đếm riêng từng hàng | Test JVM như trên | `AdsControllerNativeRetryTest.twoRows_countApart`: hàng 0 và hàng 1 cùng vị trí, hàng 1 bắt đầu ở 15 000; mỗi hàng có đúng chuỗi mốc riêng tính từ lượt đầu của nó |
| Pool: pool trống luôn lỗi 10 phút; ô của pool không gửi thêm | Test JVM như trên | `AdsControllerNativeRetryTest.pool_alwaysFailing_10min_13Requests`: thu thập hai ô `pooledCell(GALLERY_NATIVE_MEDIUM)` (nhịp 500 ms, 2 giây của `PoolCellPolicy` chạy suốt), cùng mốc 13 lần; đối chứng: số lần `claim` của ô lớn hơn 13 nhiều lần mà số lần gửi vẫn 13 |
| Native toàn màn hình: luôn lỗi 10 phút; không còn 5 giây cố định | Test JVM như trên | `AdsControllerNativeRetryTest.nativeFullScreen_alwaysFailing_10min_13Requests`: cùng mốc 13 lần (trước ticket: 120 lần, mỗi 5 giây) |
| Native toàn màn hình theo bộ đếm riêng, interstitial không đổi | Unit test (`TestScope`) | `FullScreenReloadsTest.nativeFullScreen_failures_5_10_20_40_60`: năm lần `onLoadFailed(NATIVE_FULL_SCREEN, 3)` ra `WaitingRetry` ở +5 000, +10 000, +20 000, +40 000, +60 000; `failuresInARow` truyền vào luôn trả 1 (đối chứng: kết quả không theo nó); cùng test, `INTER` vẫn 2 000 |
| Dịp nạp được, khe | Test JVM như trên | `AdsControllerNativeRetryTest.slot_successThenFailure_waits5s`: khe native small lỗi ở 0, 5 000, 15 000; lượt ở 35 000 có quảng cáo (`FakeNativeAdLoader` trả `FakeLoadedNativeAd`), slot `Loaded`; khe chuyển sang làm mới, lượt làm mới lỗi theo luật giữ quảng cáo (2 giây) không đổi. Khe chưa giữ quảng cáo sau thành công chỉ xảy ra với luồng mới, nên dịp này của khe thực chất là "mở lại" (dòng mở màn mới); câu khẳng định: không có dòng `Native gallery_native_small retry in` nào sau `Loaded` |
| Dịp nạp được, pool | Test JVM như trên | `AdsControllerNativeRetryTest.pool_successThenEmptyAgain_failureWaits5s`: pool medium lỗi ở 0, 5 000, 15 000; lượt ở 35 000 có quảng cáo; ô nhận rồi trả quảng cáo đã có lượt hiển thị (pool lại trống); lượt kế tiếp lỗi thì lượt sau cách 5 000 (không 40 000) |
| Dịp nạp được, native toàn màn hình | Unit test và test JVM | `FullScreenReloadsTest.nativeFullScreen_loadedThenFailure_waits5s` (lỗi ba lần, `onLoaded`, lỗi: `WaitingRetry` ở +5 000); `AdsControllerNativeRetryTest.nativeFullScreen_loadedThenFailure_waits5s` (điểm nối trả true ở lượt thứ tư, quảng cáo được hiện và đóng qua `beginStandaloneNativeTurn`, lượt nạp lại sau khi đóng lỗi: lượt sau cách 5 000). Dự phòng: mục 3.4 |
| Dịp app ra nền rồi quay lại, khe | Test JVM như trên | `AdsControllerNativeRetryTest.slot_backgroundAndBack_nextFailureWaits5s`: lỗi ở 0, 5 000, 15 000, 35 000 (lượt kế hạn 75 000); `setAppForegroundForTest(false)` ở 40 000, `true` ở 50 000; mốc gửi `[0, 5_000, 15_000, 35_000, 75_000, 80_000]` (lượt chưa tới hạn không chạy sớm, lần lỗi ở 75 000 chờ 5 000). Đối chứng không ra nền: `[..., 75_000, 135_000]` |
| Dịp app ra nền rồi quay lại, hàng | Test JVM như trên | `AdsControllerNativeRetryTest.inListRow_backgroundAndBack_nextFailureWaits5s`: cùng mốc như khe |
| Dịp app ra nền rồi quay lại, pool | Test JVM như trên | `AdsControllerNativeRetryTest.pool_backgroundAndBack_nextFailureWaits5s`: cùng mốc như khe |
| Dịp app ra nền rồi quay lại, native toàn màn hình | Test JVM như trên | `AdsControllerNativeRetryTest.nativeFullScreen_backgroundAndBack_nextFailureWaits5s`: cùng mốc như khe |
| Lượt tới hạn khi ở nền vẫn dời tới lúc hiện lại | Test JVM như trên | `AdsControllerNativeRetryTest.slot_retryDueInBackground_sentOnReturn`: lỗi ở 0, nền ở 1 000, quay lại ở 30 000: lượt thứ hai ở 30 000, lỗi thì lượt thứ ba ở 35 000. Cùng ý cho native toàn màn hình: `FullScreenReloadsTest.nativeFullScreen_retryWaitsForForeground` (có sẵn) |
| Màn che toàn màn hình đóng không phải quay lại foreground | Test JVM như trên | `AdsControllerNativeRetryTest.slot_coverClosed_doesNotReset`: khe lỗi ở 0, 5 000, 15 000; một lượt native toàn màn hình độc lập (`putPreloadedNativeFullScreenForTest`, `beginStandaloneNativeTurn`) che từ 16 000 tới 20 000; lượt ở 35 000 lỗi thì lượt sau ở 75 000 (không 40 000) |
| Dịp mạng về, khe | Test JVM như trên | `AdsControllerNativeRetryTest.slot_networkBack_cutsWaitAndNextFailureWaits5s`: lỗi ở 0, 5 000, 15 000, 35 000; mạng mất ở 40 000, về ở 45 000; tín hiệu ở 48 000: mốc `[0, 5_000, 15_000, 35_000, 48_000, 53_000]` |
| Dịp mạng về, hàng | Test JVM như trên | `AdsControllerNativeRetryTest.inListRow_networkBack_cutsWaitAndNextFailureWaits5s`: cùng mốc |
| Dịp mạng về, pool | Test JVM như trên | `AdsControllerNativeRetryTest.pool_networkBack_cutsWaitAndNextFailureWaits5s`: cùng mốc |
| Dịp mạng về, native toàn màn hình (cắt ngắn và chờ lại 5 giây) | Test JVM và unit test | `AdsControllerNativeRetryTest.nativeFullScreen_networkBack_cutsWaitAndNextFailureWaits5s`: cùng mốc. `FullScreenReloadsTest.nativeFullScreen_retryResetsCutWaitShort` (tín hiệu đổi ở 1 000 khi chờ 5 000: lượt nạp ở 1 000; interstitial không bị cắt ngắn: vẫn ở 2 000) |
| Dịp mở màn mới, khe | Test JVM như trên | `AdsControllerNativeRetryTest.slot_reopened_nextFailureWaits5s`: khe lỗi ở 0, 5 000, 15 000; hủy thu thập ở 20 000 (rời màn), thu thập lại ở 25 000 (màn mới): lượt gửi ở 25 000, lỗi thì lượt sau ở 30 000 |
| Dịp mở màn mới, hàng | Test JVM như trên | `AdsControllerNativeRetryTest.inListRow_reopened_nextFailureWaits5s`: cùng mốc |
| Dịp mở màn mới, pool (không đặt lại, câu hỏi 1) | Test JVM như trên | `AdsControllerNativeRetryTest.pool_newScreen_keepsBackoff`: pool lỗi ở 0, 5 000, 15 000, 35 000; ô của màn cũ hủy ở 40 000, ô của màn mới bắt đầu ở 45 000: không gửi gì trước 75 000; lượt ở 75 000 lỗi thì lượt sau ở 135 000. Nếu người duyệt chọn phương án khác ở câu hỏi 1 thì test đổi theo câu trả lời trước khi duyệt |
| Pool giữ quảng cáo: chuỗi 2, 4, 8 giây không đổi | Unit test | `NativeAdPoolTest.failures_retryDelays` (phần pool giữ quảng cáo, giữ) |
| Pool: tín hiệu đặt lại, `reopen()` | Unit test | `NativeAdPoolTest.failureResets_nextFailureWaits5s` (lỗi ở 0, 5 000, 15 000; `failureResets` đổi ở 20 000; lượt ở 35 000 lỗi: lượt sau ở 40 000), `reopen_countsFrom5sAgain` (lỗi ba lần, `close()`, `reopen()`, lỗi: 5 000) |
| Khe: tín hiệu đặt lại | Unit test | `NativeAdRetryTest.failureResets_nextFailureWaits5s` (cùng mốc như pool), đối chứng `keepsFailing_backsOff_neverGivesUp` |
| App ở nền không gửi (tiêu chí 5) | Test có sẵn | `NativeAdRetryTest.background_*`, `NativeAdPoolTest.retry_inBackground_waitsForForeground`, `AdsControllerInListRowTest.inListRow_inBackground_sendsNothing` (giữ) |
| Log dạng cũ, số mới | Test JVM như trên | Trong `slot_alwaysFailing_10min_13Requests`, `pool_alwaysFailing_10min_13Requests`, `nativeFullScreen_alwaysFailing_10min_13Requests`: `RecordingAppLogger` có đúng các dòng `retry in 5000 ms (failures 1)`, `10000 ms (failures 2)`, `20000 ms (failures 3)`, `40000 ms (failures 4)`, `60000 ms (failures 5)` theo dạng của từng đường (mục 3.1) |
| Trên bản staging, phiên thử không có native: log giãn 5, 10, 20, 40, 60 giây | Kiểm tra thủ công (người: đọc log của một phiên `scripts/device-session.sh` trên emulator khi native không về; kịch bản thiết bị không kiểm được dòng log và emulator CI thường có quảng cáo thử nên không tái hiện được no fill) | Dev ghi một phiên khoảng 3 phút ở trang "Ad gallery" và trang "Grid ads" khi không có quảng cáo (máy không có mạng ra máy chủ quảng cáo nhưng có Internet dùng được, hoặc unit lỗi), đọc `adb logcat -v threadtime \| grep ' VGL\.Ads'`: các dòng `Native <vị trí> retry in`, `Pool <mã pool> failed, retry in`, `Retry <vị trí> in` lần lượt 5000, 10000, 20000, 40000, 60000 ms; dòng `Native retry counters reset: foreground` khi ra nền rồi quay lại, sau đó lại 5000 ms |
| Tài liệu O-05, mục 11, ads-guide | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc O-05, dòng "Thử lại native" và ba chỗ của `ads-guide.md` khớp KB-24 mới |

**Ngoại lệ viết test trước:** sửa `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md` (chỉ tài liệu). Phần nối trong `AdsController` (truyền `failureResets` vào bốn đường và `FullScreenReloads`, gọi `onLoaded`) không là ngoại lệ: mỗi đường có test JVM với `AdsController` thật ở trên.

Thứ tự commit của bước 3 (test viết trước): `NativeRetryCount.kt` chỉ có khung (`onFailure()` trả 1, hai hàm kia rỗng) và điểm nối `nativeFullScreenRequestForTest` (chỉ điểm nối, chưa đổi luật) vào commit test, để test biên dịch và thất bại đúng lý do (mốc gửi sai, 120 lần thay cho 13); tham số mới có giá trị mặc định nên test có sẵn vẫn biên dịch. Mọi phần còn lại vào commit code của bước 4.

### 4.1. Chạy thử

Không cần (E dưới 2): không kỹ thuật hay thư viện mới; test JVM với `AdsController` thật, `nativeLoaderForTest` và `idleFor` đã dùng ở `AdsControllerInListRowTest`. Điểm nối mới `nativeFullScreenRequestForTest` có phương án dự phòng ở mục 3.4. Điểm nối có sẵn `nativeLoaderForTest` thay đúng bộ nạp cho khe, hàng và pool (`newNativeLoader` của `poolLoad`, `nativeLoadOnce`); chưa chạy câu khẳng định 10 phút bằng test tạm (câu hỏi 6).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi tần suất gửi yêu cầu native khi lỗi), 7 (module core dùng chung `:core:ads`). Rủi ro cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module, 8 file nguồn và khoảng 11 file test, cả bốn đường gửi native |
| B. Độ rõ của yêu cầu | 1 | KB-24 và câu hỏi 62 đã chốt chuỗi và các dịp đặt lại; còn lựa chọn ở pool (mở màn mới, bộ đếm khi giữ quảng cáo) và cắt ngắn của native toàn màn hình khi mạng về (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Đếm và mốc gửi kiểm được bằng đồng hồ ảo cho từng đường; không có no fill thật trên CI, phần log trên máy là bước người |
| D. Ảnh hưởng chéo | 2 | `AdsController`, `NativeAdPool`, `FullScreenReloads` dùng cho mọi vị trí native của mọi app; doanh thu khi no fill thoáng qua lấp chậm hơn tối đa vài chục giây |
| E. Độ mới của kỹ thuật | 1 | Mẫu có sẵn (`retryResets`, `AdsControllerInListRowTest`); một điểm nối test mới cho native toàn màn hình |
| **Tổng** | 7 (không dùng, chạm điểm chặn) | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:ads` (`AdsController`, `NativeAdPool`, `FullScreenReloads`, `NativeAdRetry`, `InListAds`, `NativeAdSources`, `AdRetryPolicy`), mọi vị trí native của mọi app tạo từ dự án mẫu (đưa về app qua `scripts/template-sync.sh`); `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`.
- Task khác trong sprint có thể xung đột: ticket khác của feature ads đang mở mà sửa `AdsController.kt` hay `NativeAdPool.kt` (xem danh sách chạy lúc lập; `scripts/run-check.py` báo plan cùng sửa một file). T-101, T-102 đã `done`.
- Đề xuất thứ tự chạy: một mình trong danh sách chạy với các plan khác chạm `:core:ads`, hoặc chạy sau chúng; `AdsController.kt` lớn và nhiều plan cùng sửa.

## 7. Điều kiện dừng riêng

- Cần đổi `LoadAttemptCounter` hay giá trị `load_attempt` của số liệu để làm được: dừng (ngoài phạm vi, câu hỏi 4).
- Cần đổi luật thử lại khi đang giữ quảng cáo (`NativeAdRefresh`, `holdsAd = true`), luật F-05 của interstitial, app open, rewarded, hay luật B-02 của banner: dừng.
- Cần làm lượt chờ chưa tới hạn chạy sớm khi app quay lại foreground để test qua: dừng (trái "lượt tới hạn khi ở nền vẫn dời tới lúc hiện lại", ticket).
- Test 10 phút của một đường đếm được hơn 15 yêu cầu sau khi sửa: dừng, không nới câu khẳng định.

## 8. Câu hỏi cho người duyệt

1. **Pool có đặt lại bộ đếm khi mở một màn có chỗ dùng pool không?** Đề xuất: không. Pool sống qua nhiều màn và dùng chung cho mọi vị trí cùng định dạng và mã quảng cáo; `:core:ads` không biết đâu là "màn mới", chỉ thấy ô bắt đầu thu thập, mà ô của lưới hay danh sách bắt đầu lại mỗi lần cuộn tới, nên đặt lại theo ô làm pool trống thử lại gần như mỗi 5 giây khi người dùng cuộn, trái ý giới hạn của KB-24. Pool vẫn về 0 khi nạp được, khi quay lại foreground, khi mạng về và khi `reopen()`. Phương án khác: đặt lại khi một ô bắt đầu thu thập lúc pool trống và không có ô nào khác của pool đang thu thập (gần đúng "mở màn mới", nhưng màn đầu tiên có ô và lưới cuộn hết ô rồi về vẫn đặt lại); test `pool_newScreen_keepsBackoff` đổi theo.
2. **Mạng về có cắt ngắn lượt chờ của native toàn màn hình không?** Đề xuất: có (mục 3.1): hôm nay `FullScreenReloads` không nghe `nativeRetryResets`, nên với chờ tới 60 giây, native toàn màn hình có thể thử lại sau mạng về tới 60 giây, xa hơn hôm nay (5 giây) và trái tiêu chí 4 của KB-24. Ticket chỉ yêu cầu bộ đếm về 0. Phương án khác: chỉ đặt lại bộ đếm, không cắt ngắn (bỏ tham số `retryResets` và test `nativeFullScreen_retryResetsCutWaitShort`).
3. **Tín hiệu đặt lại (foreground, mạng về) có áp cho bộ đếm của pool khi pool đang giữ quảng cáo không?** Đề xuất: có, vì một pool có một bộ đếm cho cả hai luật (hôm nay cũng chung); hệ quả: pool giữ quảng cáo mà lỗi sau khi quay lại foreground thì chờ 2 giây thay vì tiếp chuỗi cũ. Phương án khác: chỉ đặt lại khi pool trống lúc lỗi (thêm điều kiện ở `NativeAdPool`, thêm một test).
4. **Native toàn màn hình đếm bằng bộ đếm riêng, không bằng `LoadAttemptCounter`.** Đề xuất: bộ đếm riêng trong `FullScreenReloads`, để đặt lại khi foreground hay mạng về không làm đổi `load_attempt` của số liệu (`LoadAttemptCounter` chỉ về 0 khi nạp được, như hôm nay). Hệ quả: `load_attempt` và số `failures` trong dòng log `Retry` có thể khác nhau sau một dịp đặt lại. Phương án khác: thêm hàm đặt lại vào `LoadAttemptCounter` (`load_attempt` cũng về 1 sau dịp đặt lại; đổi định nghĩa số liệu, cần sửa `docs/ops/` của số liệu).
5. **Lần quay lại foreground đầu tiên của process** (lúc mở app, false sang true) cũng tăng tín hiệu đặt lại. Đề xuất: giữ, vô hại (chưa có lỗi nào để đặt lại).
6. **Chạy thử chưa làm.** Phiên con soạn plan không chạy Gradle trong 40 phút; mục 4.1 ghi "Không cần" vì E là 1. Nếu người duyệt muốn chắc điểm nối `nativeLoaderForTest` đếm đúng lượt gửi của pool khi có hai ô thu thập, chạy một test tạm `pool_alwaysFailing_10min_13Requests` trên code hiện tại (kỳ vọng thất bại với 120 lần gửi mỗi 5 giây) trước khi duyệt.
7. **Bước thủ công cần no fill trên emulator.** Đề xuất: dev ghi phiên khi native không về (mục 4, dòng cuối); nếu không tạo được no fill trên emulator, chấp nhận chỉ kiểm bằng test JVM và xem log trên bản staging của máy thật khi gặp no fill.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:**, **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
