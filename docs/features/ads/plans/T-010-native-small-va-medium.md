---
ticket: T-010
title: Native small và medium; màn hình mẫu dùng native small thay banner
status: approved               # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 40M          # cỡ L, số gợi ý của scripts/token-usage.py estimate (2026-10-21, chưa có task đã đo)
blocking_points: [ads, core]
run_order:                  # AI điền sau họp đánh giá rủi ro
reviewer: Nhật            # người duyệt
cross_reviewer: Nhật            # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-010: NATIVE SMALL VÀ MEDIUM; MÀN HÌNH MẪU DÙNG NATIVE SMALL THAY BANNER

## 1. Tóm tắt

`:core:ads` có thêm hai định dạng `NATIVE_SMALL`, `NATIVE_MEDIUM`; phần hiển thị không trạng thái `NativeAdCard` và `NativeAdPlaceholder` dựng bằng Compose theo token của `:core:designsystem` và số liệu trong `docs/features/ads/design/context/`; composable có trạng thái `NativeAdSlot(placement)` bọc `NativeAdView` qua `AndroidView`, giữ chỗ khi tải, thu chỗ sau `ads_native_load_timeout_seconds`, hủy quảng cáo khi rời màn hình. `AdsController` có luồng tải native trả `NativeAdState`. Màn hình mẫu đổi `SAMPLE_BOTTOM_BANNER` thành `SAMPLE_NATIVE_SMALL` ghim ở đáy. Kiểm chứng bằng unit test cho luồng tải và thời gian chờ (đồng hồ ảo), test giao diện Robolectric cho kích thước và vị trí, screenshot test trùng tên ba ảnh thiết kế của `SampleScreen`, ảnh khung thấp, và kiểm tra thủ công trên bản staging với mã thử của Google.

**Phiên bản 2 (2026-10-23).** Bản 1 đã duyệt ngày 2026-10-23 (anh Nhật). Rà soát danh sách chạy RUN-20261023-01 thấy T-012, T-013 cần gắn `NativeAdView` cho bố cục riêng, mà `BoundNativeAd(ad, size)` của bản 1 chỉ nhận hai cỡ. Thay đổi:

- Hàm gắn SDK dùng chung `internal` nhận bố cục bất kỳ: `BoundNativeAd(ad, modifier, content: @Composable (NativeAdBinder) -> Unit)`, giao diện `NativeAdBinder` để bố cục đăng ký view của từng asset và `MediaView`; `BoundNativeAd(ad, size, modifier)` của bản 1 giữ nguyên chữ ký (T-011 dùng), nay gọi hàm chung. Các phần dựng con của `NativeAdLayout` và `NativeAdPlaceholderLayout(size, height)` là `internal` để T-012, T-013 ghép bố cục riêng mà không sửa file của T-010 (mục 3.1).
- Chạy thử thật kỹ thuật bọc `NativeAdView` qua `AndroidView` (mục 4.1): gọi `NativeAdView.setNativeAd` với quảng cáo giả bằng mockk ném `ClassCastException` trên JVM, nên phần gắn tách lời gọi `setNativeAd` thành tham số `bind` của `NativeAdViewHost` nội bộ; nhờ vậy việc đăng ký view, bỏ đăng ký khi bố cục đổi, gọi lại `setNativeAd` và chiều cao trong `NativeAdView` có test JVM mới `NativeAdViewBindingTest.kt` (mục 4), không còn là ngoại lệ viết test trước.
- Theo mẫu plan mới: mục 3.3 thêm dòng quyền truy cập và thư viện (đều không thêm), mục 3.4 phương án dự phòng, mục 4.1 chạy thử. Mục 9 chờ duyệt lại.
- Theo rà soát trước khi chạy: `requireNativeFormat` dùng `when` có `else`; `when` đủ nhánh duy nhất về cỡ tải là `AdFormat.nativeLoadSize()` trong `AdsController.kt`, để T-012, T-013 thêm định dạng mà không sửa file của T-010, T-011 (mục 3.1).

Không đổi: phạm vi, định dạng, vị trí, luồng tải, màn hình mẫu, ảnh chuẩn, chấm rủi ro.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` ngày 2026-10-21):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices; vùng quảng cáo có nền hoặc viền tách khỏi nội dung; CTA khác nút chính của app. | Native small cao 72 dp: icon 40 dp, tiêu đề, dòng "Ad" cùng nhà quảng cáo (hoặc mô tả ngắn), CTA cao 36 dp nền `tertiaryContainer`, bo 8 dp; khung nền `surfaceContainerLow`, viền 1 dp `outlineVariant`, bo 12 dp. AdChoices do SDK vẽ ở góc trên phải; lề phải 24 dp để chữ và CTA không đè lên góc đó. `PrimaryButton` cao 52 dp, bo 16 dp, màu `primary`: khác CTA. |
| KB-1 | Khi đang tải thì vùng quảng cáo giữ đúng chiều cao với hiệu ứng chờ, nội dung bên dưới không nhảy. | `NativeAdPlaceholder` cùng khung, cùng chiều cao (small 72 dp, medium 386 dp), các khối `surfaceContainerHighest` như ảnh `SampleScreen_ad-loading_light`, độ mờ nhấp nháy nhẹ. Card và placeholder có chiều cao cố định, nên khi quảng cáo tới bố cục không đổi. |
| KB-1 | Khi không có quảng cáo sau thời gian chờ thì vùng quảng cáo thu lại; nếu vị trí được khai báo là cạnh nút của app thì giữ khoảng trống để nút không dịch. | Không nhận được quảng cáo trong `ads_native_load_timeout_seconds` (mặc định 10) hoặc SDK báo lỗi: trạng thái `Unavailable`, vùng quảng cáo cao 0. "Khai báo cạnh nút" là tham số `keepSpaceWhenEmpty` của `NativeAdSlot` (mặc định `false`): khi `true` giữ một khoảng trống đúng chiều cao, không vẽ gì. Màn hình mẫu không cạnh nút (thiết kế: ghim ở đáy, không ngay dưới nút "Tap") nên dùng mặc định. Xem câu hỏi 3. |
| KB-1 | Khi app ở chế độ tối thì quảng cáo dùng màu tối của hệ thống thiết kế. | Mọi màu lấy từ `MaterialTheme.colorScheme`; ảnh `SampleScreen_ready_dark`. |
| KB-1, trường hợp đặc biệt | Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ. | Như `AdSlot`: không có `AdsController` (test, preview), `canRequestAds` là `false`, `isEnabled(placement)` là `false` (gồm công tắc tổng của T-009) hoặc không có mã quảng cáo: `NativeAdSlot` chỉ vẽ `Box(modifier)` rỗng, cao 0, kể cả khi `keepSpaceWhenEmpty`. |
| KB-1, trường hợp đặc biệt | Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ. | Luồng tải gắn với composition (không theo vòng đời `STOP`/`START`, để không tải lại mỗi lần app về tiền cảnh; spec: native chỉ tải mới khi vào lại màn hình). Composable rời composition, hoặc vị trí bị tắt giữa chừng: luồng bị hủy, `NativeAd.destroy()` được gọi; quảng cáo tới sau khi đã hủy hay sau thời gian chờ cũng bị `destroy()` ngay. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt. | Native medium cao 386 dp: đầu (icon, tiêu đề, "Ad", nhà quảng cáo), `MediaView` rộng hết khung trừ lề, cao 196 dp, mô tả tối đa 2 dòng, CTA rộng hết, cao 36 dp. `MediaView` dùng `ImageView.ScaleType.FIT_CENTER` cho ảnh; video do SDK giữ tỷ lệ. Yêu cầu tỷ lệ ảnh `MediaAspectRatio.ANY`. |
| KB-2 | Các tiêu chí giữ chỗ, không có quảng cáo, chế độ tối như KB-1. | Dùng chung `NativeAdSlot`, `NativeAdPlaceholder`, cùng luồng tải. Chưa có vị trí medium nào trong app (màn hình xem thử là T-014), nên medium được kiểm bằng test và screenshot của component trong `:core:ads`. |

Ngoài ra, theo ticket và spec câu hỏi 3: bỏ banner ở màn hình mẫu, giữ định dạng `BANNER` và `AdSlot` trong `:core:ads` cho app cần; cập nhật `CLAUDE.md` bước 7 và `docs/ops/ads-guide.md`.

Thiết kế tham chiếu: `features/ads/design/README.md` (`approved` ngày 2026-10-21), các ảnh trong `design/screens/` thuộc T-010: `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light`. Số liệu của native medium lấy từ `design/context/AdGalleryScreen_default_light.md` (khung "Native/Medium"); ảnh của màn hình xem thử thuộc T-014. `SampleScreen_disabled_light` không đổi (README mục 1).

ADR liên quan: ADR-0004, ADR-0008, ADR-0013, ADR-0014, ADR-0019, ADR-0020, ADR-0022 (quyết định 4 và 5), ADR-0023, ADR-0024 (không có mạng thì native thu chỗ sau thời gian chờ như mọi lần tải thất bại; feature không đọc trạng thái mạng).

**Giả định về plan trước** (hợp đồng chung, giữ đúng tên):

- T-007 đã merge: `RemoteKey<T>`, `RemoteSettings.value(key)`, `AdsSettings` public có `NATIVE_LOAD_TIMEOUT_SECONDS` (`ads_native_load_timeout_seconds`, long, 10) và `placementEnabled(placement)`; `AppRemoteKeys` và test sinh `docs/remote-config-keys.md`, `docs/remote-config-template.json` lấy vị trí từ `AdPlacement.entries`.
- T-008 đã merge: staging bật mọi vị trí của `AdPlacement`, nên `sample_native_small` tự bật trên staging.
- T-009 đã merge (ticket nay ghi `depends_on: [T-007, T-009]`): `AdsController` nhận `AppLogger`, `isEnabled(placement)` đã gồm công tắc tổng. Xem câu hỏi 5.
- PR #81 đã merge (cần PR #81 merge trước): `scripts/release.sh` đọc được `AdFormat.NATIVE_SMALL`.

T-009 hay PR #81 chưa merge, hoặc API thật khác các tên trên ở điểm làm đổi danh sách file: dừng (mục 7).

## 3. Giải pháp

### 3.1. Cách làm

**Định dạng và mã thử** (`AdPlacement.kt`, `AdUnitIds.kt`):

- `AdFormat` thêm `NATIVE_SMALL` ("shown inside a screen with `NativeAdSlot`, no media"), `NATIVE_MEDIUM` ("with media, replaces MREC").
- `AdPlacement`: `SAMPLE_BOTTOM_BANNER("sample_bottom_banner", AdFormat.BANNER)` đổi thành `SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL),` trên một dòng như mẫu của `ads-guide.md`, mục 5. Khóa bật tắt đổi từ `ads_sample_bottom_banner_enabled` sang `ads_sample_native_small_enabled`, vẫn mặc định tắt (ADR-0014).
- `GoogleTestAdUnitIds.of`: `NATIVE_SMALL` dùng mã thử native advanced `ca-app-pub-3940256099942544/2247696110`; `NATIVE_MEDIUM` dùng mã thử native video `ca-app-pub-3940256099942544/1044960115` để thử được `MediaView` với video. Đối chiếu lại hai mã trên trang mã thử của Google khi làm; khác thì dùng mã trên trang và ghi nhật ký.

**Phần hiển thị không trạng thái** (file mới `NativeAdCard.kt`, public):

```kotlin
enum class NativeAdSize { SMALL, MEDIUM }

/** What a native ad shows; the SDK fills it in the app, tests and previews use fixed values. */
data class NativeAdAssets(
    val headline: String,
    val advertiser: String?,
    val body: String?,
    val callToAction: String?,
    val icon: ImageBitmap?,
    val hasMedia: Boolean,
)

@Composable fun NativeAdCard(assets: NativeAdAssets, size: NativeAdSize, modifier: Modifier = Modifier)
@Composable fun NativeAdPlaceholder(size: NativeAdSize, modifier: Modifier = Modifier, animated: Boolean = true)
```

- Cả hai gọi một bố cục chung `internal NativeAdLayout(assets, size, binder, showAdChoicesMarker, modifier)`. `binder` là điểm nối của từng thành phần (tiêu đề, nhà quảng cáo, mô tả, CTA, icon, media): `NativeAdCard` dùng bộ nối rỗng, `NativeAdSlot` dùng bộ nối đăng ký view với `NativeAdView` (bên dưới). Nhờ vậy screenshot của quảng cáo giả và quảng cáo thật có cùng bố cục.
- `NativeAdCard` vẽ thêm một ô 16 dp ở góc trên phải đánh dấu chỗ AdChoices (`showAdChoicesMarker = true`), như component "Ad/AdChoices (SDK)" của thiết kế, để ảnh chụp giống thiết kế. Quảng cáo thật không vẽ ô này: AdChoices do SDK vẽ (ads-guide.md, mục 2: không tự vẽ AdChoices). Xem câu hỏi 4.
- `icon` là `ImageBitmap?`: SDK trả `Drawable`, đổi bằng `toBitmap().asImageBitmap()` của `core-ktx` có sẵn, không thêm thư viện. Không có icon thì vẽ ô 40 dp nền `secondaryContainer`, bo 8 dp, như thiết kế.
- `hasMedia` chỉ dùng ở medium: quảng cáo giả vẽ khối `surfaceContainer` bo 8 dp ở chỗ media; quảng cáo thật đặt `MediaView`. Small không có media.
- `animated = false` để screenshot test không phụ thuộc thời gian; mặc định nhấp nháy độ mờ bằng `rememberInfiniteTransition`. Tham số thêm này có mặc định, không đổi chữ ký trong hợp đồng chung.

**Số liệu, theo `design/context/` và README mục 3:**

| Phần | Small | Medium |
| --- | --- | --- |
| Khung | cao 72 dp, rộng hết vùng nội dung; nền `surfaceContainerLow`, viền 1 dp `outlineVariant`, bo `MaterialTheme.shapes.medium` (12 dp) | cao 386 dp; như small |
| Lề trong (trên/phải/dưới/trái) | 0 / 24 / 0 / 12, căn giữa theo chiều dọc | 24 / 12 / 12 / 12 |
| Khoảng giữa các phần | 12 dp ngang | 12 dp dọc |
| Icon | 40 dp, bo 8 dp | 40 dp trong hàng đầu |
| Tiêu đề | `titleSmall`, `onSurface`, 1 dòng, cắt bằng dấu ba chấm | như small |
| Dòng dưới tiêu đề (cách 2 dp) | nhãn "Ad" + nhà quảng cáo, hoặc mô tả khi không có nhà quảng cáo; `bodySmall`, `onSurfaceVariant`, 1 dòng | nhãn "Ad" + nhà quảng cáo |
| Nhãn "Ad" | nền `tertiary`, chữ `onTertiary`, `labelSmall`, bo 4 dp, lề ngang `Spacing.xs`, cách chữ bên cạnh 6 dp | như small |
| Media | không có | rộng hết, cao 196 dp, nền chờ `surfaceContainer`, bo 8 dp |
| Mô tả | trong dòng dưới tiêu đề | `bodyMedium`, `onSurfaceVariant`, tối đa 2 dòng, cao 40 dp |
| CTA | cao 36 dp, lề ngang `Spacing.md`, nền `tertiaryContainer`, chữ `onTertiaryContainer` `labelLarge`, bo 8 dp | rộng hết, cao 36 dp |
| Đang tải | khối icon 40 dp, hai vạch 180 x 10 và 120 x 10 cách nhau `Spacing.sm`, khối CTA 80 x 36; nền `surfaceContainerHighest` | cùng khung, khối cho đầu, media, mô tả, CTA, cùng chiều cao 386 dp |

Giá trị "ngoài hệ thống" 2, 6, 12 dp (README mục 3 để plan quyết): giữ làm hằng số `private` của `NativeAdCard.kt` có chú thích, như `MinHeight` của `PrimaryButton`; không thêm token, không sửa `:core:designsystem` hay `docs/design-system/tokens.json`. Bo 4 dp và 8 dp cũng là hằng số của component. Khoảng 16 dp, 24 dp, 4 dp, 8 dp dùng `Spacing`.

Chữ "Ad" là `R.string.ads_native_badge` trong `core/ads/src/main/res/values/strings.xml`. Chữ của quảng cáo do SDK trả, không vào `strings.xml` (README mục 2).

**Luồng tải** (file mới `NativeAdLoading.kt`, `internal`, không phụ thuộc SDK để test trên JVM):

```kotlin
internal interface LoadedNativeAd { val assets: NativeAdAssets; fun destroy() }

internal sealed interface NativeAdState {
    data object Loading : NativeAdState
    data class Loaded(val ad: LoadedNativeAd) : NativeAdState
    data object Unavailable : NativeAdState
}

/** Starts one native request; calls exactly one of the callbacks. */
internal fun interface NativeAdLoader {
    fun load(unitId: String, size: NativeAdSize, onLoaded: (LoadedNativeAd) -> Unit, onFailed: (code: Int) -> Unit)
}

/** Loads one ad: Loaded or Unavailable once the request ends or times out. The caller owns a Loaded ad and destroys it. */
internal suspend fun loadOneNativeAd(
    loader: NativeAdLoader, unitId: String, size: NativeAdSize, timeoutMillis: Long,
    logger: AppLogger, placementKey: String,
): NativeAdState

internal fun nativeAdStates(
    loader: NativeAdLoader, unitId: String, size: NativeAdSize, timeoutMillis: Long,
    logger: AppLogger, placementKey: String,
): Flow<NativeAdState>
```

- `loadOneNativeAd` là phần tải dùng chung: chờ tối đa `timeoutMillis`, trả `Loaded` hoặc `Unavailable`; quảng cáo tới sau thời gian chờ, hay tới sau khi coroutine gọi bị hủy, bị `destroy()` ngay. Hàm không hủy quảng cáo đã trả về: người gọi giữ và hủy. `nativeAdStates` phát `Loading` rồi kết quả của `loadOneNativeAd`, và hủy quảng cáo khi luồng bị hủy. T-011 dùng `loadOneNativeAd` (qua `AdsController.loadNativeAd`) để mỗi ô trong danh sách có một quảng cáo riêng sống lâu hơn hàng của danh sách.
- Phát `Loading` ngay. Quảng cáo tới trước thời gian chờ: `Loaded`. SDK báo lỗi: `Unavailable` ngay, không chờ hết thời gian. Hết thời gian chưa có gì: `Unavailable`; quảng cáo tới sau đó bị `destroy()` ngay, không hiện (bố cục đã thu lại, hiện ra sẽ làm nhảy màn hình).
- Luồng bị hủy (rời màn hình, vị trí bị tắt): `awaitClose` gọi `destroy()` cho quảng cáo đã nhận.
- Thời gian chờ âm hoặc 0 từ Remote Config: coi là 0, tức `Unavailable` ngay nếu chưa có quảng cáo (cùng cách T-009 kẹp giá trị âm về 0).
- Log bằng `AppLogger`, thẻ `Ads` (ADR-0019): `Native <key> loaded`, `Native <key> failed: <code>`, `Native <key> timed out after <n> ms`, `Native <key> destroyed`. Chỉ khóa vị trí, mã lỗi, số mili giây; không ghi chữ của quảng cáo hay mã quảng cáo.

**Cài đặt bằng SDK** (file mới `SdkNativeAdLoader.kt`, `internal`): `AdLoader.Builder(context, unitId).forNativeAd { ... }.withAdListener(onAdFailedToLoad)`, `NativeAdOptions` với `setAdChoicesPlacement(ADCHOICES_TOP_RIGHT)`, medium thêm `setMediaAspectRatio(MediaAspectRatio.ANY)`, small `setRequestMultipleImages(false)`; tải bằng `ads.adRequest()` hiện có (giữ quy tắc quảng cáo không cá nhân hóa). `SdkLoadedNativeAd(nativeAd)` dựng `NativeAdAssets` từ `headline`, `advertiser`, `body`, `callToAction`, `icon?.drawable`, `mediaContent != null`, và giữ `nativeAd` để gắn vào view.

**`AdsController`** (sửa): thêm `internal fun nativeAd(placement: AdPlacement): Flow<NativeAdState>`: lấy cỡ tải bằng `requireNotNull(placement.format.nativeLoadSize())` (định dạng không tải như native thì ném `IllegalArgumentException`); đọc `settings.value(AdsSettings.NATIVE_LOAD_TIMEOUT_SECONDS).first()`; lấy `unitId(placement)` (null thì phát `Unavailable`); dựng `adRequest()` một lần rồi gọi `nativeAdStates(SdkNativeAdLoader(context, request), unitId, size, timeoutMillis, logger, placement.key)`. Thêm `internal suspend fun loadNativeAd(placement: AdPlacement): NativeAdState`: cùng các bước đó nhưng gọi `loadOneNativeAd`, trả `Loaded` hoặc `Unavailable` (không có mã thì `Unavailable`); người gọi giữ và hủy quảng cáo. Hàm này dành cho T-011 (một quảng cáo riêng cho mỗi ô trong danh sách); không kiểm cổng đồng ý hay bật tắt, người gọi kiểm trước như `NativeAdSlot`. `SdkNativeAdLoader` tạo trong `AdsController`, không qua Hilt, để không sửa module Hilt của T-009. Không đổi `showInterstitial`, `isEnabled`, việc khởi động SDK hay hộp thoại đồng ý. Cập nhật KDoc của lớp: feature dùng thêm `NativeAdSlot`.

**Bảng định dạng sang cỡ tải** (trong `AdsController.kt`): `internal fun AdFormat.nativeLoadSize(): NativeAdSize?` là biểu thức `when` đủ nhánh trên `AdFormat`, không có `else`: `BANNER`, `INTERSTITIAL` ra `null`, `NATIVE_SMALL` ra `SMALL`, `NATIVE_MEDIUM` ra `MEDIUM`. Đây là `when` duy nhất trên `AdFormat` buộc phải đủ nhánh ngoài `GoogleTestAdUnitIds.of`: ticket sau thêm giá trị vào `AdFormat` (T-012 thêm `NATIVE_COLLAPSIBLE`, T-013 thêm `NATIVE_FULL_SCREEN`) thì trình biên dịch buộc xét thêm nhánh ở đây, trong file mà ticket đó vốn sửa. Các hàm kiểm định dạng ở file khác (`requireNativeFormat` bên dưới, `requireInListFormat` của T-011) không dùng `when` đủ nhánh, để giá trị mới không làm hỏng biên dịch file của T-010, T-011.

**Composable có trạng thái** (file mới `NativeAdSlot.kt`, public):

```kotlin
@Composable
fun NativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier, keepSpaceWhenEmpty: Boolean = false)
```

- `require` định dạng là `NATIVE_SMALL` hoặc `NATIVE_MEDIUM`; cỡ lấy theo `placement.format`.
- Cổng như `AdSlot`: `LocalAdsController` rỗng, chưa `canRequestAds`, `isEnabled` là `false` hoặc không có mã: `Box(modifier)` rỗng.
- Cỡ lấy bằng hàm thuần `internal fun requireNativeFormat(format: AdFormat): NativeAdSize` (ném `IllegalArgumentException` với định dạng khác `NATIVE_SMALL`, `NATIVE_MEDIUM`). Thân hàm là `when (format) { NATIVE_SMALL -> SMALL; NATIVE_MEDIUM -> MEDIUM; else -> throw IllegalArgumentException(...) }`, có nhánh `else`, không bao giờ là `when` đủ nhánh không có `else`: T-012, T-013 thêm giá trị vào `AdFormat` mà không phải sửa `NativeAdSlot.kt`, và `NativeAdSlot` vẫn chỉ nhận small và medium.
- Có cổng: gọi `internal NativeAdSlotHost(states = remember(placement) { ads.nativeAd(placement) }, size, keepSpaceWhenEmpty, modifier, adContent = { BoundNativeAd(it, size) })`. `NativeAdSlotHost` thu luồng bằng `collectAsState(NativeAdState.Loading)` (gắn với composition) rồi gọi `internal NativeAdSlotContent(state, size, keepSpaceWhenEmpty, modifier, adContent)`:
  - `Loading`: `NativeAdPlaceholder(size)`.
  - `Loaded`: `adContent(ad)`; bản thật là `BoundNativeAd`.
  - `Unavailable`: cao 0, hoặc `Spacer` đúng chiều cao khi `keepSpaceWhenEmpty`.
- `BoundNativeAd(ad, size, modifier)`: phần dựng một quảng cáo đã tải, tách khỏi `NativeAdSlot` để T-011 dùng lại cho ô trong danh sách; gọi hàm gắn chung bên dưới với `NativeAdLayout(ad.assets, size, binder, showAdChoicesMarker = false)`. Feature không thấy `NativeAdView` (ADR-0022, quyết định 5).

**Hàm gắn SDK dùng chung** (file mới `NativeAdViewBinding.kt`, mọi khai báo `internal`; T-012, T-013 dùng lại, không sửa file này):

```kotlin
internal enum class NativeAdAsset { HEADLINE, ADVERTISER, BODY, CALL_TO_ACTION, ICON }

/** Lets a native ad layout of any shape mark its parts for the SDK. */
internal interface NativeAdBinder {
    @Composable fun Asset(asset: NativeAdAsset, modifier: Modifier, content: @Composable () -> Unit)
    @Composable fun Media(modifier: Modifier)
}

/** Fake ads, tests and previews: draws the content as is; Media draws the surfaceContainer block. */
internal object PreviewNativeAdBinder : NativeAdBinder

@Composable
internal fun BoundNativeAd(ad: LoadedNativeAd, modifier: Modifier = Modifier, content: @Composable (NativeAdBinder) -> Unit)

@Composable
internal fun BoundNativeAd(ad: LoadedNativeAd, size: NativeAdSize, modifier: Modifier = Modifier)

/** One NativeAdView around [content]; [bind] runs after the asset views change, at most once per frame. */
@Composable
internal fun NativeAdViewHost(bind: (NativeAdView) -> Unit, modifier: Modifier = Modifier, content: @Composable (NativeAdBinder) -> Unit)
```

- `BoundNativeAd(ad, modifier, content)`: `ad` không phải `SdkLoadedNativeAd` (quảng cáo giả) thì `Box(modifier) { content(PreviewNativeAdBinder) }`, không có `NativeAdView`. Với `SdkLoadedNativeAd`: `NativeAdViewHost(bind = { it.setNativeAd(ad.nativeAd) }, modifier, content)`.
- `NativeAdViewHost`: `remember` một `NativeAdView`; `AndroidView(factory = { adView.apply { addView(ComposeView(it)) } }, update = { composeView.setContent { content(binder) } })`. Bộ nối SDK (lớp `private`) vẽ mỗi `Asset` trong một `AndroidView` con chứa `ComposeView`: `update` gọi `setContent(content)` rồi gán view vào `headlineView`, `advertiserView`, `bodyView`, `callToActionView` hay `iconView`; `onRelease` gán `null`. `Media` là `AndroidView` tạo `MediaView` (`setImageScaleType(ImageView.ScaleType.FIT_CENTER)`), gán `mediaView`, `onRelease` gán `null`. Mỗi lần gán hay bỏ gán, bộ nối hẹn một lần `bind(adView)` bằng `adView.post` (gộp nhiều lần gán trong cùng khung hình), nên `setNativeAd` luôn chạy sau khi mọi view đã gán (yêu cầu của SDK), và chạy lại khi bố cục đổi (T-012 bỏ `MediaView` khi thu gọn). `DisposableEffect` gọi `adView.destroy()` khi rời cây; hủy `NativeAd` vẫn do người giữ quảng cáo (`NativeAdSlotHost`, T-011).
- Asset là view thật trong `NativeAdView`, nên SDK nhận lượt chạm trên view đó: phần vẽ trong `Asset` không dùng `Modifier.clickable` hay `Button` (CTA là `Box` có nền và chữ), để chạm tới được `callToActionView`.
- Chạy thử (mục 4.1) xác nhận trên JVM: `CompositionLocal` (theme) đi xuyên `ComposeView` lồng; `onNodeWithTag` thấy node trong `ComposeView` lồng; kích thước đo đúng; bỏ `Media` khỏi bố cục thì `mediaView` về `null` và `bind` chạy lại.

**Phần dựng con dùng chung** (`NativeAdCard.kt`, `internal`, để T-012, T-013 ghép bố cục riêng theo cùng số liệu): `NativeAdFrame(modifier, content)` (nền, viền, bo góc), `NativeAdIcon(icon, binder, modifier)`, `NativeAdHeadline(text, binder, modifier)`, `NativeAdBadgeLine(assets, binder, modifier)` (nhãn "Ad" và nhà quảng cáo, hoặc mô tả), `NativeAdBody(text, binder, maxLines, modifier)`, `NativeAdCallToAction(text, binder, modifier)`, `NativeAdMediaArea(binder, modifier)` (gọi `binder.Media`, bo 8 dp), hằng số kích thước trong `internal object NativeAdDimens`, và `NativeAdPlaceholderLayout(size, height, modifier, animated)`: khối chờ của `size` với chiều cao cho trước, khối media co theo chiều cao còn lại (`weight(1f)`). `NativeAdLayout` và `NativeAdPlaceholder` (public, chiều cao theo cỡ) chỉ ghép các phần này. Tên khác trong lúc làm thì ghi nhật ký; T-012, T-013 dẫn tới mục này.

**Màn hình mẫu** (`SampleScreen.kt`):

- `SampleScreen(uiState, onIncrement, modifier, bottomAd: @Composable (Modifier) -> Unit = { NativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL, it) })`. `SampleRoute` không đổi (dùng mặc định). Test truyền `NativeAdCard` hay `NativeAdPlaceholder` với quảng cáo giả, vì test không có `AdsController` nên `NativeAdSlot` không vẽ gì (ADR-0013). Xem câu hỏi 2.
- Bố cục: `Column(fillMaxSize)`: phần nội dung hiện có trong `Column(weight(1f).verticalScroll(...))`, giữ nguyên lề `Spacing.md`, khoảng `Spacing.md`, căn giữa ngang; bên dưới, quảng cáo `fillMaxWidth()`, lề ngang và dưới `Spacing.md` (thiết kế: khung 379 x 72 tại 16,803, cạnh dưới cách đáy 16 dp). Không thêm `navigationBarsPadding()`: `AppNavHost` đã có `safeDrawingPadding()` nên thanh điều hướng đã được chừa. Quảng cáo luôn ở đáy, cách nội dung cuộn được; trên màn hình thấp nội dung cuộn, nút không bị quảng cáo che.
- `bottomAd` chỉ gọi khi `uiState` là `Ready`: thiết kế ghi trạng thái tắt không có quảng cáo, nên `SampleScreen_disabled_light` giữ nguyên ảnh; `Loading` của màn hình mẫu cũng không có quảng cáo (ngắn, tránh hai hiệu ứng chờ cùng lúc).
- `PrivacyOptionsButton` giữ trong phần nội dung, ngay sau nút và thông báo lỗi, không đặt cạnh quảng cáo.
- Preview thêm một bản với `NativeAdCard` giả.

**Build** (`core/ads/build.gradle.kts`): thêm `alias(libs.plugins.roborazzi)`, khối `RoborazziExtension` trỏ `src/test/screenshots`, `testImplementation` ba thư viện Roborazzi, và `implementation(project(":core:designsystem"))` cho `Spacing` và `AppTheme` của screenshot; giống hệt `core/designsystem/build.gradle.kts`. Plugin và thư viện đã có trong `gradle/libs.versions.toml`; không sửa catalog hay `build-logic/`. Lõi phụ thuộc lõi, đúng ADR-0001.

**Tài liệu:**

- `CLAUDE.md`, bước 7: thêm `NativeAdSlot(placement)` (native small, medium; `keepSpaceWhenEmpty` khi vị trí cạnh nút của app), và cách screenshot màn hình có quảng cáo: truyền `NativeAdCard` giả qua tham số của màn hình như `:feature:sample`.
- `docs/ops/ads-guide.md`: mục 3 native small, medium "Có"; mục 4 bỏ "(đợt 2)" ở tiêu đề phần small, medium, thêm số đo đã chốt (72 dp, 386 dp, media 196 dp, CTA 36 dp, lề phải 24 dp cho AdChoices), cách dùng `keepSpaceWhenEmpty`; mục 5 bước 4 và 5 thay "(đợt 2)" bằng `NativeAdSlot` và screenshot với `NativeAdCard` giả; câu mở đầu về "(đợt 2)" chỉ còn các phần chưa làm.
- `docs/ops/new-app.md`, `docs/ops/release.md`: ví dụ `SAMPLE_BOTTOM_BANNER`, `sample_bottom_banner` đổi thành `SAMPLE_NATIVE_SMALL`, `sample_native_small`.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: sinh lại bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` (lệnh của T-007), vì khóa `ads_sample_bottom_banner_enabled` thành `ads_sample_native_small_enabled`. Không sửa tay.

**Nơi đọc các file bị đổi** (L-020; `grep -rn` trên `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/androidTest`, và `scripts/plan-check.py`):

- `AdPlacement.kt`: `scripts/release.sh` đọc danh sách vị trí từ file này bằng biểu thức `AdFormat\.[A-Z]+`, không khớp tên có dấu gạch dưới như `AdFormat.NATIVE_SMALL`. Khi đó bước kiểm "mỗi vị trí có mã production" của script không thấy `sample_native_small` và không còn kiểm vị trí native nào. Plan không được chạm `release.sh` (CLAUDE.md, mục Không được làm). Đã sửa ở PR #81 (câu hỏi 1): cần PR #81 merge trước T-010. Bản staging và CI không bị ảnh hưởng. `scripts/tests/run.sh` có chuỗi `sample_bottom_banner` trong dữ liệu thử của `release.sh`, nhưng các ca đó bị từ chối vì lý do khác (mã thử của Google, mục sai dạng) trước bước so vị trí, nên không cần sửa.
- `AdsController.kt`, `AdUnitIds.kt`, `SampleScreen.kt`, `core/ads/build.gradle.kts`: không script hay workflow nào đọc theo tên. Test đọc chúng là các test có trong danh sách.
- `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`: `scripts/check.sh` và `.github/workflows/pr-check.yml` xử lý mọi ảnh trong `src/test/screenshots` theo mẫu chung, không theo tên; `check.sh` giữ nguyên ảnh đã có, nên hai ảnh này ghi lại bằng `./gradlew :feature:sample:recordRoborazziDebug` (mục 4).
- `CLAUDE.md`: `scripts/new-app.sh` đổi đoạn mở đầu của file khi tạo app mới; plan chỉ sửa bước 7, không chạm đoạn mở đầu, nên script không bị ảnh hưởng. Ba nơi khác chỉ nhắc tên file, không đọc nội dung: `.github/ISSUE_TEMPLATE/from-app.yml` (một lựa chọn "docs/, CLAUDE.md" trong mẫu issue), `scripts/device-session.sh` (in lời nhắc tới `docs/CLAUDE.md`, file khác), `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt` (chú thích dẫn bước 10, plan không đổi bước 10).
- `docs/ops/ads-guide.md`, `docs/ops/new-app.md`, `docs/ops/release.md`: script chỉ nhắc tên các file này trong thông báo hay danh sách tài liệu (`new-app.sh` chép `docs/ops/`, `release.sh` in "Xem docs/ops/release.md"), không đọc nội dung.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: chỉ `RemoteConfigKeysTest` của T-007 đọc; task sinh lại đúng hai file.

**Thứ tự commit (L-025):**

1. Commit test (bước 3): mọi file test mới và sửa ở mục 3.2, gồm phần đổi `AdPlacement.SAMPLE_BOTTOM_BANNER` thành `SAMPLE_NATIVE_SMALL` trong `AdUnitIdsTest.kt`. Commit này không biên dịch được vì `NativeAdCard`, `NativeAdState`, `SAMPLE_NATIVE_SMALL`, tham số `bottomAd` chưa có: đó là lý do thất bại đúng mong đợi; ghi vào nhật ký.
2. Commit code (bước 4): mọi file `src/main`, `core/ads/build.gradle.kts`, `strings.xml`.
3. Commit ảnh chuẩn: chạy `./gradlew :core:ads:recordRoborazziDebug :feature:sample:recordRoborazziDebug`; xem từng ảnh trước khi commit.
4. Commit sinh file: lệnh ghi lại của T-007; trước khi chạy, xác nhận `RemoteConfigKeysTest` thất bại vì bảng còn khóa cũ.
5. Commit tài liệu và ảnh đối chiếu: `CLAUDE.md`, `ads-guide.md`, `new-app.md`, `release.md`, `docs/features/ads/design/review/`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `AdFormat.NATIVE_SMALL`, `NATIVE_MEDIUM`; `SAMPLE_BOTTOM_BANNER` thành `SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL)`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | Mã thử native cho hai định dạng mới trong `GoogleTestAdUnitIds` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `nativeAd(placement): Flow<NativeAdState>`, `loadNativeAd(placement): NativeAdState` (cho T-011), `AdFormat.nativeLoadSize()` (`when` đủ nhánh); KDoc lớp |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `NativeAdSize`, `NativeAdAssets`, `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdLayout` và phần dựng con nội bộ, `NativeAdPlaceholderLayout`, `NativeAdDimens`, test tag |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdLoading.kt` | `LoadedNativeAd`, `NativeAdState`, `NativeAdLoader`, `loadOneNativeAd`, `nativeAdStates` (thời gian chờ, hủy, log) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/SdkNativeAdLoader.kt` | Tải bằng `AdLoader`, `NativeAdOptions`, `SdkLoadedNativeAd` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | Hàm gắn dùng chung (mục 3.1): `NativeAdAsset`, `NativeAdBinder`, `PreviewNativeAdBinder`, `BoundNativeAd(ad, modifier, content)`, `BoundNativeAd(ad, size, modifier)`, `NativeAdViewHost(bind, modifier, content)`, bộ nối SDK `private` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | `NativeAdSlot(placement, modifier, keepSpaceWhenEmpty)`, `requireNativeFormat`, `NativeAdSlotHost` và `NativeAdSlotContent` nội bộ |
| Sửa | `core/ads/src/main/res/values/strings.xml` | `ads_native_badge` = "Ad" |
| Sửa | `core/ads/build.gradle.kts` | Plugin Roborazzi, thư mục ảnh, thư viện test Roborazzi, `implementation(project(":core:designsystem"))` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Đổi tên vị trí theo `AdPlacement` mới (bắt buộc vì đổi tên, cùng kiểm tra như cũ); thêm test mã thử native |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdLoadingTest.kt` | Luồng tải, thời gian chờ, hủy, log (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Test giao diện Robolectric: thành phần, kích thước, vị trí AdChoices, chữ dài |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Test giao diện: cổng không có controller, `NativeAdSlotContent` theo trạng thái, `BoundNativeAd` với quảng cáo giả |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Test giao diện Robolectric của hàm gắn qua `NativeAdViewHost` với `bind` đếm số lần (không gọi SDK): đăng ký view, bỏ đăng ký, gọi lại `bind`, chiều cao, `CompositionLocal` (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Screenshot component small, medium, sáng, tối, đang tải |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | `FakeNativeAdLoader` (điều khiển được callback), `FakeLoadedNativeAd` (đếm `destroy`), `sampleNativeAdAssets`; T-011 dùng lại file này |
| Tạo | `core/ads/src/test/resources/robolectric.properties` | `sdk=35`, như `:feature:sample` |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_small_light.png` | Ảnh chuẩn mới |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_small_dark.png` | Ảnh chuẩn mới |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_medium_light.png` | Ảnh chuẩn mới |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_medium_dark.png` | Ảnh chuẩn mới |
| Tạo | `core/ads/src/test/screenshots/NativeAdPlaceholder_medium_light.png` | Ảnh chuẩn mới |
| Sửa | `feature/sample/src/main/kotlin/com/vglstudio/template/feature/sample/SampleScreen.kt` | Tham số `bottomAd`, ghim native small ở đáy, nội dung cuộn được, chỉ ở `Ready`; preview |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenTest.kt` | Thêm test vị trí quảng cáo, không có quảng cáo khi tắt, màn hình thấp; test cũ giữ nguyên |
| Sửa | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/SampleScreenScreenshotTest.kt` | `ready_light`, `ready_dark` truyền `NativeAdCard` giả; thêm `ad_loading_light`, `ready_small` |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` | Có native small ở đáy (thiết kế mới) |
| Ghi lại | `feature/sample/src/test/screenshots/SampleScreen_ready_dark.png` | Như trên, chế độ tối |
| Tạo | `feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png` | Ảnh chuẩn mới, trùng tên ảnh thiết kế |
| Tạo | `feature/sample/src/test/screenshots/SampleScreen_ready_small.png` | Khung `w360dp-h640dp-xxhdpi`, `Ready(count = 3, saveFailed = true)`, quảng cáo giả (L-028) |
| Tạo | `docs/features/ads/design/review/SampleScreen_ready_light_compare.png` | Sinh bằng `DesignCompare.java` (mục 4) |
| Tạo | `docs/features/ads/design/review/SampleScreen_ready_dark_compare.png` | Như trên |
| Tạo | `docs/features/ads/design/review/SampleScreen_ad-loading_light_compare.png` | Như trên |
| Tạo | `docs/features/ads/design/review/summary.md` | Như trên |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng lệnh của T-007: `ads_sample_native_small_enabled` thay khóa cũ |
| Sửa | `docs/remote-config-template.json` | Sinh lại như trên |
| Sửa | `CLAUDE.md` | Bước 7: `NativeAdSlot`, screenshot với `NativeAdCard` giả |
| Sửa | `docs/ops/ads-guide.md` | Mục mở đầu, mục 3, mục 4, mục 5 (bước 4, 5) |
| Sửa | `docs/ops/new-app.md` | Tên vị trí mẫu `SAMPLE_NATIVE_SMALL` |
| Sửa | `docs/ops/release.md` | Ví dụ khóa vị trí `sample_native_small` |
| Sửa | `docs/features/ads/tickets/T-010-native-small-va-medium.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-010-native-small-va-medium.md` | Nhật ký thực hiện |

Không sửa: `AdSlot.kt` (banner giữ cho app cần), `AdsSettings.kt` (khóa vị trí sinh theo `AdPlacement`, `NATIVE_LOAD_TIMEOUT_SECONDS` đã có từ T-007), `FullScreenAdGuard.kt`, module Hilt của `:core:ads`, `app/` (staging tự bật vị trí mới nhờ T-008; `StagingAdsModule` dùng `GoogleTestAdUnitIds` đã có mã native), `AppLaunchTest.kt` (chờ thẻ `sample_screen`, không đổi), `SampleScreen_disabled_light.png` (không đổi; nếu ảnh lệch thì dừng, mục 7), `docs/design-system/tokens.json`, `:core:designsystem`. File được bảo vệ (`scripts/check.sh`, `.github/`, `build-logic/`, `gradle/libs.versions.toml`, `scripts/release.sh`): không chạm.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: thêm hai định dạng native; đổi vị trí `sample_bottom_banner` (banner) thành `sample_native_small` (native small) trên màn hình mẫu, theo spec mục 5 và câu hỏi 3; khóa bật tắt đổi tên tương ứng, vẫn mặc định tắt (ADR-0014). Không thêm vị trí nào khác. Staging chỉ dùng mã thử của Google (ADR-0013). App đã đặt `ads_sample_bottom_banner_enabled` trên Firebase phải đặt khóa mới (câu hỏi 6).
- Sự kiện phân tích: Không chạm. `ad_impression` lấy từ liên kết AdMob và Firebase (ADR-0022, quyết định 9).
- Dữ liệu trên máy: không có.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `AdLoader`, `NativeAdView`, `MediaView` chỉ cần `INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` mà manifest gộp đã có từ `play-services-ads` (spec ads mục 6); không sửa manifest nào.
- **Thư viện và SDK:** Không thêm. `NativeAdView`, `MediaView`, `NativeAd`, `AdLoader`, `NativeAdOptions` thuộc `play-services-ads` (`libs.play.services.ads`, đã là `implementation` của `:core:ads`); `AndroidView`, `ComposeView` thuộc Compose UI (plugin `template.android.compose`); `toBitmap` thuộc `androidx-core-ktx`, có trên classpath biên dịch qua Compose UI; mockk có sẵn ở test của mọi module (`build-logic`). `:core:ads` thêm `implementation(project(":core:designsystem"))` và ba thư viện Roborazzi đã có trong `gradle/libs.versions.toml` (mục 3.1). Chạy thử ở mục 4.1 dùng đúng các lớp này và biên dịch được.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** gộp các lần gọi `bind` (`setNativeAd`) bằng `adView.post` trong hàm gắn chung: chạy thử thấy đúng trên Robolectric, nhưng thứ tự giữa `onRelease` của `AndroidView` con và khung hình kế tiếp có thể khác khi bố cục lớn hơn.
- **Điều kiện chuyển:** `NativeAdViewBindingTest.removingMedia_unregistersAndBindsAgain` hay `registersEveryAssetView_*` vẫn thất bại vì `bind` không chạy, hoặc chạy trước khi view được gán, sau 2 lần sửa.
- **Cách làm thay:** bộ nối giữ một `mutableIntStateOf` đếm số lần gán và bỏ gán; `NativeAdViewHost` đọc số đếm trong `LaunchedEffect(count) { withFrameNanos { }; bind(adView) }`. Cùng file `NativeAdViewBinding.kt` (có trong mục 3.2), cùng chữ ký công khai, cùng test.

## 4. Kiểm thử

Unit test và test giao diện chạy trên JVM (Robolectric). Test có kiểm tra kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`; kích thước đo trên phần tử mang `testTag` (`NativeAdTestTags.CARD`, `MEDIA`, `CTA`, `PLACEHOLDER`, `AD_CHOICES_AREA`). Quảng cáo giả trong mọi test: `headline = "Headline of the ad"`, `advertiser = "Advertiser or short body"`, `body = "Body text of the ad, two lines at most so the call to action stays in view."`, `callToAction = "Install"`, `icon = null`, `hasMedia = true` (chữ minh họa của thiết kế). Thời gian chờ trong test luồng tải: `timeoutMillis = 10_000`, đồng hồ ảo của `runTest`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1: đủ thành phần | Compose UI test | `NativeAdCardTest.small_showsHeadlineAdBadgeAdvertiserAndCta`: có chữ tiêu đề, "Ad" (lấy từ `R.string.ads_native_badge`), nhà quảng cáo, "Install"; `small_withoutAdvertiser_showsBody`: `advertiser = null` thì dòng dưới là mô tả |
| KB-1, tiêu chí 1: chỗ cho AdChoices | Compose UI test | `NativeAdCardTest.small_leavesAdChoicesCornerFree`: card rộng 379 dp, cạnh phải của CTA cách cạnh phải của card ít nhất 24 dp; `medium_leavesAdChoicesCornerFree`: cạnh trên của icon cách cạnh trên của card ít nhất 24 dp |
| KB-1, tiêu chí 1: CTA khác nút của app | Compose UI test | `NativeAdCardTest.cta_is36dpHigh`: CTA cao 36 dp (nút chính 52 dp); màu và bo góc xem trên screenshot |
| KB-1, tiêu chí 1: có ranh giới | Screenshot test | `NativeAdCardScreenshotTest.small_light` (`NativeAdCard_small_light.png`): nền và viền khác nền màn hình |
| KB-1, tiêu chí 2: giữ đúng chiều cao | Compose UI test | `NativeAdCardTest.small_is72dpHigh`, `medium_is386dpHigh`; `placeholder_matchesCardHeight_forEachSize`: với mỗi `NativeAdSize`, chiều cao `NativeAdPlaceholder` bằng chiều cao `NativeAdCard` |
| KB-1, tiêu chí 2: chữ dài không làm đổi chiều cao | Compose UI test | `NativeAdCardTest.longTexts_keepHeight`: tiêu đề 80 ký tự, mô tả 300 ký tự: small vẫn 72 dp, medium vẫn 386 dp, tiêu đề một dòng (cao 20 dp) |
| KB-1, tiêu chí 2: đang tải trên màn hình | Compose UI test | `NativeAdSlotTest.loading_showsPlaceholder`: `NativeAdSlotContent(Loading, SMALL)` có `PLACEHOLDER` cao 72 dp |
| KB-1, tiêu chí 3: hết thời gian thì thu lại | Unit test | `NativeAdLoadingTest.noAdBeforeTimeout_becomesUnavailable`: phát `Loading`; tại 9_999 ms vẫn `Loading`; tại 10_000 ms phát `Unavailable` |
| KB-1, tiêu chí 3: lỗi SDK | Unit test | `NativeAdLoadingTest.loadFailure_becomesUnavailableAtOnce`: loader gọi `onFailed(3)` tại 1_000 ms, phát `Unavailable` ngay, có một dòng log `INFO` thẻ `Ads` chứa `failed: 3` |
| KB-1, tiêu chí 3: có quảng cáo | Unit test | `NativeAdLoadingTest.adBeforeTimeout_isLoaded`: loader gọi `onLoaded` tại 2_000 ms, phát `Loaded` với đúng quảng cáo, sau 10_000 ms không phát `Unavailable` |
| KB-1, tiêu chí 3: quảng cáo tới muộn | Unit test | `NativeAdLoadingTest.adAfterTimeout_isDestroyedAndNotShown`: `onLoaded` tại 12_000 ms; trạng thái giữ `Unavailable`, `FakeLoadedNativeAd.destroyCount == 1` |
| KB-1, tiêu chí 3: thời gian chờ không hợp lệ | Unit test | `NativeAdLoadingTest.nonPositiveTimeout_isUnavailableWithoutWaiting`: `timeoutMillis = -5`, phát `Loading` rồi `Unavailable` tại 0 ms |
| KB-1, tiêu chí 3: thu chỗ trên màn hình | Compose UI test | `NativeAdSlotTest.unavailable_takesNoSpace`: `NativeAdSlotContent(Unavailable, SMALL, keepSpaceWhenEmpty = false)` cao 0 dp |
| KB-1, tiêu chí 3: vị trí cạnh nút giữ chỗ | Compose UI test | `NativeAdSlotTest.unavailable_keepsSpace_whenDeclaredNextToButton`: `keepSpaceWhenEmpty = true`, cao 72 dp (medium: 386 dp), không có chữ nào trong vùng |
| KB-1, tiêu chí 3: nút không dịch | Compose UI test | `NativeAdSlotTest.buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace`: `Column` gồm vùng quảng cáo `keepSpaceWhenEmpty = true` rồi một nút; đổi trạng thái `Loading` sang `Unavailable`, vị trí nút không đổi |
| KB-1, tiêu chí 4: chế độ tối | Screenshot test | `NativeAdCardScreenshotTest.small_dark`, `medium_dark`; `SampleScreenScreenshotTest.ready_dark` |
| KB-1, đặc biệt: chưa đồng ý hay vị trí tắt | Compose UI test | `NativeAdSlotTest.withoutAdsController_takesNoSpace`: `NativeAdSlot(SAMPLE_NATIVE_SMALL)` không có `LocalAdsController`, cao 0 dp. Hai nhánh `canRequestAds`, `isEnabled` dùng chung cổng với `AdSlot`, không tạo được `AdsController` thật trên JVM: kiểm bằng rà soát và thủ công (bên dưới) |
| Vị trí sai định dạng bị chặn, cỡ theo định dạng | Unit test | `NativeAdSlotTest.nonNativeFormat_isRejected`: `requireNativeFormat(AdFormat.BANNER)` và `INTERSTITIAL` ném `IllegalArgumentException`; `nativeSize_followsPlacementFormat`: `NATIVE_SMALL` ra `SMALL`, `NATIVE_MEDIUM` ra `MEDIUM`; `nativeLoadSize_mapsFormats`: `AdFormat.BANNER.nativeLoadSize()` và `INTERSTITIAL` ra `null`, `NATIVE_SMALL` ra `SMALL`, `NATIVE_MEDIUM` ra `MEDIUM` |
| KB-1, đặc biệt: hủy khi rời màn hình | Unit test | `NativeAdLoadingTest.cancelCollection_destroysLoadedAd`: nhận `Loaded`, hủy job thu thập, `destroyCount == 1`, có log `destroyed`; `cancelWhileLoading_destroysLateAd`: hủy khi đang tải, loader gọi `onLoaded` sau đó, `destroyCount == 1` |
| KB-1, đặc biệt: hủy khi rời composition | Compose UI test | `NativeAdSlotTest.leavingComposition_destroysAd`: `NativeAdSlotHost` nhận `nativeAdStates(FakeNativeAdLoader, ...)`, loader trả `FakeLoadedNativeAd`, `adContent` vẽ `NativeAdCard` của `assets`; bỏ composable khỏi cây (`var shown by mutableStateOf(true)`, đặt `false`), `destroyCount == 1` |
| Tải một quảng cáo cho một ô (dùng cho T-011) | Unit test | `NativeAdLoadingTest.loadOne_adBeforeTimeout_returnsLoadedAndKeepsAd`: loader gọi `onLoaded` tại 2_000 ms, trả `Loaded`, `destroyCount == 0`; `loadOne_noAd_returnsUnavailableAtTimeout`: trả `Unavailable` tại 10_000 ms, quảng cáo tới tại 12_000 ms có `destroyCount == 1`; `loadOne_cancelledWhileLoading_destroysLateAd`: hủy coroutine gọi rồi loader gọi `onLoaded`, `destroyCount == 1` |
| Phần dựng quảng cáo đã tải với quảng cáo giả | Compose UI test | `NativeAdSlotTest.boundNativeAd_withFakeAd_drawsCard`: `BoundNativeAd(FakeLoadedNativeAd(sampleNativeAdAssets), SMALL)` có chữ tiêu đề và `CARD` cao 72 dp |
| Hàm gắn chung nhận bố cục bất kỳ, quảng cáo giả | Compose UI test | `NativeAdViewBindingTest.customLayout_withFakeAd_drawsContentWithPreviewBinder`: `BoundNativeAd(FakeLoadedNativeAd(sampleNativeAdAssets)) { binder -> Column { binder.Asset(HEADLINE, Modifier.testTag("h")) { Text(...) }; binder.Media(Modifier.size(200.dp, 120.dp).testTag("m")) } }`: có chữ, node `m` cao 120 dp |
| Gắn SDK: đăng ký đủ view (small) | Compose UI test, `NATIVE` | `NativeAdViewBindingTest.registersEveryAssetView_small`: `NativeAdViewHost(bind = { binds++; adView = it }) { NativeAdLayout(sampleNativeAdAssets, SMALL, it, showAdChoicesMarker = false) }`: `headlineView`, `advertiserView`, `callToActionView`, `iconView` khác null, `mediaView` null, `binds >= 1` |
| Gắn SDK: đăng ký media (medium) | Compose UI test, `NATIVE` | `NativeAdViewBindingTest.registersEveryAssetView_medium`: như trên với `MEDIUM`, `bodyView` khác null, `mediaView is MediaView` |
| Gắn SDK: chiều cao không đổi trong `NativeAdView` | Compose UI test, `NATIVE` | `NativeAdViewBindingTest.sdkLayout_keepsCardHeights`: `CARD` cao 72 dp (small), 386 dp (medium), `CTA` cao 36 dp, `MEDIA` cao 196 dp |
| Gắn SDK: bố cục đổi thì đăng ký lại | Compose UI test, `NATIVE` | `NativeAdViewBindingTest.removingMedia_unregistersAndBindsAgain`: bố cục tự viết có `binder.Media` trong `if (!collapsed)`; đặt `collapsed = true`, `waitForIdle`: `mediaView` null, `headlineView` vẫn khác null, `binds` tăng |
| Gắn SDK: theme đi xuyên view lồng | Compose UI test | `NativeAdViewBindingTest.assetContent_seesCompositionLocals`: `CompositionLocalProvider` bên ngoài `NativeAdViewHost`, chữ trong `Asset` đọc giá trị đó và hiện đúng |
| Log không có dữ liệu quảng cáo | Unit test | `NativeAdLoadingTest.logsHoldNoAdContent`: không dòng log nào chứa tiêu đề, nhà quảng cáo hay mã quảng cáo |
| KB-2, tiêu chí 1: có media, ít nhất 120 x 120 dp | Compose UI test | `NativeAdCardTest.medium_hasMediaOfAtLeast120dp`: card rộng 379 dp, `MEDIA` rộng 355 dp trở lên và cao 196 dp (cả hai chiều ≥ 120 dp); `small_hasNoMedia`: `MEDIA` không có |
| KB-2, tiêu chí 1: đúng tỷ lệ, không kéo giãn | Kiểm tra thủ công | Bản staging, mã thử native video và native ảnh (bên dưới); `FIT_CENTER` của `MediaView` có trong rà soát code |
| KB-2, tiêu chí 2 | Như KB-1 | `placeholder_matchesCardHeight_forEachSize`, `unavailable_keepsSpace_whenDeclaredNextToButton` (medium), `NativeAdCardScreenshotTest.medium_dark`, `placeholder_medium_light` (`NativeAdPlaceholder_medium_light.png`) |
| Mã thử native trên staging | Unit test | `AdUnitIdsTest.googleTestUnits_nativeFormats`: `NATIVE_SMALL` ra `.../2247696110`, `NATIVE_MEDIUM` ra `.../1044960115`; `googleTestUnits_existForEveryPlacement` (có sẵn) vẫn qua |
| Vị trí mẫu đúng spec | Unit test | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` và `placementWithoutUnit_isReportedMissingAndShowsNoAd` dùng `sample_native_small`, `SAMPLE_NATIVE_SMALL`; `everyPlacementKey_isUniqueAndUsableInRemoteConfig` (có sẵn) vẫn qua. `AdsSettingsTest.placementsAreOffByDefault` của T-007 vẫn qua mà không sửa |
| Bảng khóa khớp code | Unit test có sẵn | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` (T-007): thất bại trước khi sinh lại, qua sau khi sinh lại |
| Màn hình mẫu: quảng cáo ở đáy | Compose UI test | `SampleScreenTest.whenReady_thenAdIsPinnedToBottom`: khung `w411dp-h891dp`, `bottomAd` là một `Box` cao 72 dp có tag; cạnh dưới cách cạnh dưới màn hình 16 dp, rộng 379 dp |
| Màn hình mẫu: cách nút ít nhất 16 dp | Compose UI test | `SampleScreenTest.whenReady_thenAdIsAtLeast16dpBelowButton`: cạnh trên của quảng cáo trừ cạnh dưới nút "Tap" ≥ 16 dp |
| Màn hình mẫu: trạng thái tắt không có quảng cáo | Compose UI test | `SampleScreenTest.whenDisabled_thenNoAd`: `bottomAd` không được gọi (biến đếm bằng 0); `whenLoading_thenNoAd` tương tự |
| Màn hình thấp (L-028) | Compose UI test | `SampleScreenTest.whenSmallScreen_thenButtonAndAdDoNotOverlap` với `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`, `Ready(count = 3, saveFailed = true)`: nút "Tap" hiện đủ, thông báo lỗi tới được bằng cuộn (`performScrollTo`), cạnh dưới nút cách cạnh trên quảng cáo ≥ 16 dp |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `SampleScreenScreenshotTest.ready_small` (`SampleScreen_ready_small.png`): `Ready(count = 3, saveFailed = true)`, `NativeAdCard` giả |
| Đúng thiết kế | Screenshot test cùng tên với từng ảnh thiết kế, khung `w411dp-h891dp-xxhdpi` | `SampleScreenScreenshotTest.ready_light` (`SampleScreen_ready_light.png`, `NativeAdCard` giả), `ready_dark` (`SampleScreen_ready_dark.png`), `ad_loading_light` (`SampleScreen_ad-loading_light.png`, `NativeAdPlaceholder(SMALL, animated = false)`); `disabled_light` giữ nguyên ảnh cũ |
| Đúng thiết kế: ảnh so sánh | Đối chiếu, rà soát độc lập theo mục 7 của mẫu thiết kế | Script so ảnh của ADR-0020 chỉ tìm ảnh ở `feature/ads/`, mà ảnh của T-010 nằm ở `feature/sample/`. Chạy thẳng chương trình so ảnh, chỉ với ba ảnh của T-010: chép `SampleScreen_ready_light.png`, `SampleScreen_ready_dark.png`, `SampleScreen_ad-loading_light.png` của `docs/features/ads/design/screens/` vào một thư mục tạm, rồi `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java <thư mục tạm> feature/sample/src/test/screenshots docs/features/ads/design/review`. Dán `summary.md` vào mô tả PR. Rà soát độc lập xem ba ảnh ghép: bố cục, thứ tự thành phần, vai trò màu, kiểu chữ, lệch không quá 4 dp |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `NativeAdCardTest.badgeText_isAd`: `stringResource(R.string.ads_native_badge) == "Ad"`, kiểu `labelSmall`; chữ của màn hình mẫu không đổi (test có sẵn của `SampleScreenTest` qua) |
| Màn hình mẫu vẫn khởi động | Test trên emulator có sẵn | `AppLaunchTest.appStarts_andReachesStartScreen` qua mà không sửa |
| KB-1, KB-2 với quảng cáo thật | Kiểm tra thủ công | Bản staging (mã thử, T-008 bật vị trí): đồng ý ở hộp thoại, mở màn hình mẫu: thấy khối chờ rồi native small ở đáy với icon, tiêu đề, "Ad", CTA, AdChoices ở góc trên phải không bị đè; chạm "Tap" nhiều lần không chạm nhầm quảng cáo; chế độ tối. Bật chế độ máy bay rồi mở lại màn hình: khối chờ, sau 10 giây vùng quảng cáo thu lại, nội dung không nhảy. Rời màn hình: `adb logcat -v threadtime \| grep ' VGL\.Ads'` có dòng `destroyed`. Native medium và tỷ lệ media: kiểm ở màn hình xem thử của T-014; trước đó dev có thể tạm đổi `sample_native_small` sang `NATIVE_MEDIUM` trên máy (không commit) để xem. Bật lớp kiểm tra native của SDK (Native Ad Validator, hiện trên quảng cáo thử): ghi mọi cảnh báo |
| Không chạm điểm vào ngoài phạm vi | Kiểm tra toàn bộ | `scripts/check.sh` qua (module, ktlint, unit test, screenshot của `:core:ads` và `:feature:sample`, lint, build staging) |

**Ngoại lệ viết test trước:**

- `SdkNativeAdLoader.kt`, lời gọi `setNativeAd` thật (tham số `bind` mà `BoundNativeAd` truyền cho `NativeAdViewHost`) và phần gắn của `NativeAdSlot` với `AdsController` gọi thẳng SDK quảng cáo, không chạy được trên JVM (chạy thử: `setNativeAd` với quảng cáo giả ném `ClassCastException`); `AdsController` hiện cũng chưa có unit test. Kiểm bằng build, rà soát độc lập và kiểm tra thủ công trên staging. Phần logic (thời gian chờ, hủy, chọn trạng thái hiển thị, đăng ký view) đã tách ra `nativeAdStates`, `NativeAdSlotContent` và `NativeAdViewHost` để test trước.
- Ảnh chuẩn screenshot ghi sau khi code xong (Roborazzi cần code để chụp); test screenshot viết trước, thất bại vì không biên dịch.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json` sinh từ code; tài liệu (`CLAUDE.md`, `ads-guide.md`, `new-app.md`, `release.md`) không có test.

Người duyệt cần đồng ý với các ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** bọc `NativeAdView` qua `AndroidView`, mỗi asset là `AndroidView` con chứa `ComposeView` đăng ký vào `headlineView`, `callToActionView`, `MediaView` đăng ký vào `mediaView`, gọi lại `bind` gộp bằng `View.post`, bỏ đăng ký trong `onRelease`. Ngày 2026-10-23, worktree tạm ngoài repo từ `origin/main` (commit `39bbc70`, chưa có T-007 đến T-010), code thử `TrialNativeAdBinding.kt` trong `:core:ads` (bản rút gọn của mục 3.1) và test tạm `TrialNativeAdBindingTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi`, `robolectric.properties` `sdk=35`), không cần quảng cáo thật. Không commit, đã xóa worktree.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TrialNativeAdBindingTest*'`, rồi `scripts/check.sh` (JDK 21 của Android Studio; JDK 26 của máy làm Gradle lỗi ngay khi khởi động).
- **Kết quả:** lần đầu thất bại: `NativeAdView.setNativeAd` với `mockk<NativeAd>(relaxed = true)` ném `ClassCastException` (`Object` không ép được sang `IObjectWrapper`) trên JVM. Xử lý: tách `setNativeAd` thành tham số `bind` (nay là `NativeAdViewHost`, mục 3.1). Lần hai thất bại: bỏ `Media` khỏi bố cục không gọi lại `bind`; xử lý: `onRelease` cũng hẹn `bind`. Lần ba qua: `headlineView`, `callToActionView` khác null, `mediaView is MediaView`; CTA trong view lồng cao 36 dp, media 160 dp (sai số 0.5 dp); `CompositionLocal` đi xuyên `ComposeView` lồng; sau khi bỏ media, `mediaView` null, `headlineView` còn, `bind` chạy thêm. Test thứ hai đo khoảng cách 16 dp của một hàng có lề dọc `Spacing.md` trong `LazyColumn` (dùng cho T-011): đúng 16 dp trên và dưới. `scripts/check.sh` qua: `Cổng chất lượng: QUA`, `:core:ads` 17 test 0 lỗi (gồm 2 test thử), ktlint và lint qua, 2 phút 57 giây. Phương án dự phòng ở mục 3.4 chưa cần thử.
- **Thay đổi sau chạy thử:** cách viết `requireNativeFormat` (`when` có `else`), bảng `AdFormat.nativeLoadSize()` trong `AdsController.kt` và test `nativeLoadSize_mapsFormats` có sau lần chạy thử (rà soát trước khi chạy). Lần chạy thử chỉ thử kỹ thuật bọc `NativeAdView` qua `AndroidView` và khoảng cách hàng, không đụng phần chọn cỡ theo định dạng, nên kết quả trên không đổi. Câu về thư mục `review/` ở mục 6 chỉ là tài liệu.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 1 (quảng cáo: thêm hai định dạng, đổi định dạng của vị trí mẫu từ banner sang native small) và điểm 7 (module core dùng chung `:core:ads`, thêm phụ thuộc `:core:designsystem`). Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt (risk-scoring.md, mục 3).

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module code (`:core:ads`, `:feature:sample`), đổi API công khai của `:core:ads` (`AdFormat`, `AdPlacement`, 3 composable mới) và của `SampleScreen`; tài liệu chung `CLAUDE.md` |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, thiết kế `approved` có số liệu; AI tự quyết vài điểm nhỏ (cách khai báo "cạnh nút", ô AdChoices trong ảnh giả, chỉ hiện quảng cáo ở `Ready`), nêu ở mục 8 |
| C. Khả năng kiểm chứng tự động | 2 | Logic tải, thời gian chờ, kích thước, bố cục có test; phần gắn `NativeAdView`, quảng cáo thật, tỷ lệ media và lớp kiểm tra native của SDK chỉ kiểm được thủ công trên máy |
| D. Ảnh hưởng chéo | 2 | Sửa `AdsController.kt`, `core/ads/build.gradle.kts`, `AdPlacement.kt`, `ads-guide.md` mà T-009, T-011 đến T-015 cũng sửa; đổi tên vị trí làm đổi bảng khóa sinh ra |
| E. Độ mới của kỹ thuật | 2 | Native ads và `NativeAdView` bọc Compose chưa có trong dự án; có mẫu chính thức của Google và đã chạy thử (mục 4.1); Roborazzi cho `:core:ads` lặp lại cách của `:core:designsystem` |
| **Tổng** | 10 | **Mức: Cao (điểm chặn 1 và 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:ads`: `AdFormat`, `AdPlacement` (đổi tên vị trí duy nhất), `GoogleTestAdUnitIds`, `AdsController` (thêm hàm, không đổi hàm cũ), `build.gradle.kts`, `strings.xml`. Mọi `when` trên `AdFormat` ngoài `AdUnitIds.kt`: hiện không có (đã `grep`). Sau T-010 có hai `when` đủ nhánh: `GoogleTestAdUnitIds.of` và `AdFormat.nativeLoadSize()` trong `AdsController.kt`; ticket thêm giá trị `AdFormat` sửa cả hai file này.
  - Khóa Remote Config `ads_sample_bottom_banner_enabled` không còn; `ads_sample_native_small_enabled` thay thế. App đã tạo từ dự án mẫu mà vẫn dùng vị trí mẫu thì phải đổi khóa trên Firebase khi kéo thay đổi về.
  - `scripts/release.sh` cần bản sửa của PR #81 để thấy vị trí native (mục 3.1, câu hỏi 1); T-010 chạy sau khi PR #81 merge.
  - `CLAUDE.md` bước 7: mọi AI làm feature về sau đọc.
- Task khác trong sprint có thể xung đột:
  - T-009: sửa `AdsController.kt`, `core/ads/build.gradle.kts`, `ads-guide.md`; T-010 chạy sau T-009 (giả định ở mục 2).
  - T-011, T-012, T-013 (`depends_on: [T-010]`): dùng `NativeAdLayout`, `NativeAdSlot`, `NativeAdState`; T-012, T-013 dùng hàm gắn chung `BoundNativeAd(ad, modifier, content)`, `NativeAdBinder` và phần dựng con nội bộ (mục 3.1), không sửa file của T-010; T-011 dùng thêm `BoundNativeAd(ad, size, modifier)`, `AdsController.loadNativeAd`, `LoadedNativeAd` và `FakeNativeAds.kt` (giữ đúng tên này); cùng sửa `AdPlacement.kt`, `AdsController.kt`, `ads-guide.md`. T-014 thêm vị trí riêng và các ảnh của `AdGalleryScreen`; T-014 chỉ chép 5 ảnh so sánh `AdGalleryScreen_*_compare.png` của mình vào `docs/features/ads/design/review/` và ghi dòng của chúng vào `design/README.md`, không sinh lại cả thư mục `review/`, nên ảnh so sánh và `summary.md` của T-010 còn nguyên.
  - T-015: sửa `core/ads/build.gradle.kts`, `AdsController.kt`, `ads-guide.md`, `docs/ops/release.md`; không chạy cùng lúc.
  - T-017: dùng `AdsController` (`fullScreenAdShowing`); không đụng phần native.
- Đề xuất thứ tự chạy: sau T-007, T-008, T-009 (và T-015 nếu cùng phiên); trước T-011, T-012, T-013, T-014. Đây là đầu của chuỗi native, nên xếp sau các task độc lập trong phiên.

## 7. Điều kiện dừng riêng

1. T-009 hay PR #81 chưa merge, hoặc sau T-007, T-009 `AdsController` không có `AppLogger`, `RemoteSettings.value`, `AdsSettings.NATIVE_LOAD_TIMEOUT_SECONDS` như mục 2: dừng, ghi API thật, sửa plan.
2. Gắn `NativeAdView` với Compose cần thư viện mới (ví dụ thư viện Compose riêng của Google cho native, hay accompanist để vẽ `Drawable`) hoặc nâng phiên bản `play-services-ads`: dừng (cần sửa `gradle/libs.versions.toml`, plan khác).
3. Lớp kiểm tra native của SDK báo lỗi chính sách cho native small vì không có `MediaView`: dừng, không tự thêm media; hỏi người phụ trách sản phẩm (câu hỏi 7).
4. `SampleScreen_disabled_light.png` khác ảnh cũ sau khi đổi bố cục: dừng, không ghi lại ảnh này.
5. Screenshot với `NativeAdPlaceholder(animated = false)` hay `NativeAdCard` vẫn không ổn định giữa hai lần chạy (ví dụ vì `AndroidView` hay animation): dừng.
6. Đo trên test, bố cục small hay medium lệch thiết kế quá 4 dp mà chỉ sửa được bằng cách thêm token hay đổi `:core:designsystem`: dừng.
7. `checkModuleDependencies` hay lint không cho `:core:ads` phụ thuộc `:core:designsystem`: dừng.

## 8. Câu hỏi cho người duyệt

1. **`scripts/release.sh` không đọc được vị trí native.** Biểu thức đọc `AdPlacement.kt` là `AdFormat\.[A-Z]+`, không khớp `AdFormat.NATIVE_SMALL`. Sau T-010, script không kiểm mã production của vị trí native nào (gồm `sample_native_small` và mọi vị trí native của app), nên bản phát hành có thể thiếu mã mà không bị chặn (app chỉ ghi log lỗi và không hiện quảng cáo). AI không được chạm `release.sh`. Đề xuất: dev sửa tay thành `AdFormat\.[A-Z_]+` trước lần phát hành production đầu tiên có native, kèm một ca thử trong `scripts/tests/run.sh`, trước hoặc cùng lúc T-010. Phương án khác: đặt tên định dạng không có gạch dưới (`NATIVESMALL`), lệch hợp đồng chung và khó đọc; AI không đề xuất.
   - Trả lời (anh Nhật, 2026-10-23): `release.sh` đã sửa ở PR #81 (đọc định dạng có gạch dưới, bỏ qua vị trí có khóa bắt đầu bằng `gallery_`). Cần PR #81 merge trước T-010.
2. **Cách màn hình mẫu nhận quảng cáo giả cho screenshot.** Plan thêm tham số `bottomAd` cho `SampleScreen`, mặc định là `NativeAdSlot`, test truyền `NativeAdCard` giả. Feature sau làm theo mẫu này. Phương án khác: một `CompositionLocal` của `:core:ads` để `NativeAdSlot` tự vẽ quảng cáo giả trong test, màn hình không cần tham số, nhưng thêm một API chỉ dành cho test vào module core. AI đề xuất tham số.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
3. **Khai báo "vị trí cạnh nút của app".** Spec KB-1 nói vị trí được khai báo là cạnh nút thì giữ chỗ khi không có quảng cáo. Plan dùng tham số `keepSpaceWhenEmpty` của `NativeAdSlot` (người viết màn hình biết vị trí đặt). Phương án khác: thêm thuộc tính vào `AdPlacement` (khai báo một chỗ, nhưng đổi hàm tạo của enum mà T-011 đến T-014 cũng dùng). AI đề xuất tham số.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
4. **Ô đánh dấu AdChoices trong ảnh giả.** `NativeAdCard` vẽ một ô 16 dp ở góc trên phải để screenshot giống thiết kế; quảng cáo thật không vẽ (SDK vẽ AdChoices). Đồng ý không? Nếu không, ảnh chụp sẽ thiếu ô này so với thiết kế, chấp nhận lệch ở ảnh so sánh.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **Phụ thuộc T-009.** Ticket ghi `depends_on: [T-007]`, nhưng plan dùng `AppLogger` trong `AdsController` và công tắc tổng trong `isEnabled` của T-009. Đề xuất: thêm T-009 vào `depends_on` của ticket khi duyệt (người sửa), và xếp T-010 sau T-009.
   - Rà soát độc lập (2026-10-23): đã thêm T-009 vào `depends_on` của ticket T-010; người duyệt xác nhận.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
6. **Đổi tên khóa bật tắt của vị trí mẫu.** `ads_sample_bottom_banner_enabled` thành `ads_sample_native_small_enabled`. Project Firebase staging đã đặt khóa cũ thì phải đặt khóa mới (T-008 vẫn bật sẵn trên bản build từ file giả). Đề xuất: ghi vào mô tả PR để dev cập nhật project staging; không giữ khóa cũ.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
7. **Native small không có `MediaView`.** Thiết kế và ads-guide mục 4 chọn small không có media. Chính sách native của AdMob yêu cầu hiện media chính khi quảng cáo có video hay ảnh chính; lớp kiểm tra native của SDK có thể cảnh báo. Đề xuất: làm đúng thiết kế, kiểm bằng lớp kiểm tra trên staging; có cảnh báo thì dừng (mục 7) để người phụ trách sản phẩm chọn: thêm media nhỏ vào small (đổi thiết kế) hay chỉ dùng medium.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
8. **Quảng cáo chỉ ở trạng thái `Ready` của màn hình mẫu.** Thiết kế ghi trạng thái tắt không có quảng cáo; plan cũng bỏ quảng cáo ở `Loading` (ngắn, tránh hai hiệu ứng chờ). Đồng ý không?
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
9. **Mã thử cho native medium.** Plan dùng mã thử native video (`.../1044960115`) để thử `MediaView` với video; small dùng mã native thường (`.../2247696110`). Đề xuất: đồng ý; nếu muốn medium luôn là ảnh tĩnh để dễ so thiết kế thì dùng chung mã thường.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 2 (AI ghi theo lời anh Nhật, 2026-10-23):

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua hai lần rà soát độc lập trước khi chạy, ghi ở RUN-20261023-01)
- **Ghi chú:** câu hỏi ở mục 8 đã trả lời ngay tại đó.

Phiên bản 1 đã được anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), duyệt chéo: Nhật; câu hỏi 1 đến 9 ở mục 8 đã có trả lời và giữ nguyên ở phiên bản 2.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
