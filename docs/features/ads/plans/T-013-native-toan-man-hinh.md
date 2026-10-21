---
ticket: T-013
title: Native toàn màn hình thay interstitial
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 25M          # cỡ M, số gợi ý 20M của scripts/token-usage.py estimate (2026-10-21), cộng 5M vì có màn hình theo thiết kế và ảnh so sánh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-013: NATIVE TOÀN MÀN HÌNH THAY INTERSTITIAL

## 1. Tóm tắt

Thêm định dạng `AdFormat.NATIVE_FULL_SCREEN` vào `:core:ads`, một hàm gọi chung `rememberShowFullScreenAd(placement)` cho cả interstitial và native toàn màn hình, và màn hình `NativeFullScreenAdScreen` theo thiết kế đã duyệt. Native toàn màn hình được tải sẵn khi vị trí bật, hiện trong một hộp thoại toàn màn hình do `FullScreenAdHost` (đặt một lần ở `:app`) vẽ, đi qua cùng cổng tần suất của T-009 (`awaitFullScreenTurn`, `onFullScreenShown`, `FullScreenAdPacer`). `AdsController` có thêm `fullScreenAdShowing` (hợp đồng chung với T-017). Kiểm chứng bằng unit test cho phần logic, test giao diện Robolectric cho nút đóng và bố cục, screenshot test trùng tên ảnh thiết kế kèm ảnh khung thấp.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-5 | Khi đủ điều kiện tần suất thì quảng cáo hiện toàn màn hình với nút đóng thấy và bấm được ngay, ít nhất 48 dp, ở góc khác AdChoices. | Nút đóng tròn 48 dp ở góc trên trái, hiện ngay từ khung hình đầu, không đếm ngược, không trễ. AdChoices do SDK vẽ ở góc trên phải (`ADCHOICES_TOP_RIGHT`), plan không vẽ lại. "Đủ điều kiện tần suất" là kết quả `true` của `awaitFullScreenTurn(placement)` (T-009). |
| KB-5 | Khi bấm nút đóng hoặc nút quay lại của máy thì quảng cáo đóng và người dùng về đúng chỗ đang làm. | Quảng cáo là một hộp thoại phủ lên màn hình đang mở, không đổi điều hướng, nên màn hình bên dưới giữ nguyên trạng thái. Nút đóng và nút quay lại cùng gọi một hàm đóng; chạm ra ngoài không có nghĩa (hộp thoại phủ kín). |
| KB-5 | Khi vừa có một quảng cáo toàn màn hình (interstitial hay native) thì không hiện thêm cái nào cho tới hết khoảng cách tối thiểu. | Cả hai định dạng đi qua `awaitFullScreenTurn` và `onFullScreenShown` của T-009, nên dùng chung khoảng cách, ân hạn, giới hạn mỗi phiên. Thêm: khi một quảng cáo toàn màn hình đang hiện (`fullScreenAdShowing`), lần gọi khác bị bỏ qua ngay. |
| KB-5 | Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng. | Hàm gọi trả về ngay khi chưa có native tải sẵn còn hạn: không chờ tải, không hiện màn hình chờ; đồng thời bắt đầu tải cho lần sau. Native tải sẵn quá 1 giờ bị bỏ (Google khuyên không hiện native cũ hơn 1 giờ). |

Thiết kế tham chiếu: `features/ads/design/README.md` (đã duyệt 2026-10-21), ảnh `design/screens/NativeFullScreenAdScreen_default_light.png`, `NativeFullScreenAdScreen_default_dark.png`, số liệu `design/context/` cùng tên. Các ảnh khác trong `screens/` thuộc T-010, T-011, T-012, T-014.

ADR liên quan: ADR-0013 (rào chắn, chỉ ở điểm dừng tự nhiên), ADR-0014 (vị trí mặc định tắt, không đổi mặc định), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot test, ảnh so sánh), ADR-0022 (quyết định 1, 3, 4, 5, 9), ADR-0023 (khóa khai báo một chỗ; task không thêm khóa), ADR-0024 (popup mất mạng không chen vào quảng cáo toàn màn hình, qua `fullScreenAdShowing`).

Giả định về các task chạy trước (hợp đồng chung, giữ đúng tên):

- T-007: `RemoteKey<T>`, `RemoteSettings.value(key)`, `AdsSettings` public, `AdsSettings.placementEnabled(p)`, `AppRemoteKeys` ở `:app`; test sinh `docs/remote-config-keys.md` và `docs/remote-config-template.json` từ `AdPlacement.entries`.
- T-009: `AdsController.awaitFullScreenTurn(placement): Boolean` và `onFullScreenShown()` (nội bộ), `FullScreenAdPacer`, công tắc tổng trong `isEnabled(placement)`, `:core:ads` dùng `AppLogger`, cờ `fullScreenInProgress` chống gọi chồng.
- T-010: `AdFormat.NATIVE_SMALL`, `NATIVE_MEDIUM`; `NativeAdAssets` (headline, advertiser, body, callToAction, icon, có media hay không); phần dựng Compose bọc `NativeAdView` (đăng ký từng view asset), nhãn "Ad" dùng chung, hằng số khoảng cách riêng (2, 6, 12 dp) của composable native; mã thử native trong `GoogleTestAdUnitIds`; `:core:ads` phụ thuộc `:core:designsystem` và có Roborazzi với ảnh chuẩn ở `core/ads/src/test/screenshots/`.

## 3. Giải pháp

### 3.1. Cách làm

**Định dạng.** `AdPlacement.kt`: thêm `AdFormat.NATIVE_FULL_SCREEN` với KDoc "Covers the whole screen at a natural break, shown with [rememberShowFullScreenAd]". Thêm thuộc tính nội bộ `internal val AdFormat.isFullScreen: Boolean` (đúng cho `INTERSTITIAL`, `NATIVE_FULL_SCREEN`). Không thêm vị trí vào `AdPlacement`: spec không có vị trí native toàn màn hình nào ngoài màn hình xem thử, và vị trí đó thuộc T-014 (câu hỏi 1). Vì không thêm vị trí, bảng khóa `docs/remote-config-keys.md` và `docs/remote-config-template.json` không đổi.

**Mã thử.** `AdUnitIds.kt`: nhánh `AdFormat.NATIVE_FULL_SCREEN` trả mã thử native video công khai của Google `ca-app-pub-3940256099942544/1044960115`, để thử được media dạng video trên màn hình lớn (câu hỏi 3). Nếu T-010 chưa tách hàm theo định dạng, tách `internal fun testUnitFor(format: AdFormat): String` trong `GoogleTestAdUnitIds` để test được mà không cần vị trí; `of(placement)` gọi hàm này, không đổi hành vi.

**Hàm gọi cho feature** (file mới `FullScreenAds.kt`):

```kotlin
@Composable
fun rememberShowFullScreenAd(placement: AdPlacement): () -> Unit
```

- `require(placement.format.isFullScreen)`, thông báo "<key> is not a full-screen placement".
- Giống `rememberShowInterstitial`: không có `LocalAdsController` hay `LocalActivity` (test, preview) thì không làm gì; có thì `scope.launch { ads.showFullScreenAd(activity, placement) }`.
- `rememberShowInterstitial` giữ nguyên chữ ký và hành vi (feature cũ vẫn chạy); KDoc trỏ sang hàm mới. Không gắn `@Deprecated` để không sinh cảnh báo ở app đang dùng.

**Trạng thái quảng cáo toàn màn hình đang hiện** (file mới `FullScreenAdTracker.kt`, lớp thuần, không phụ thuộc Android):

```kotlin
internal class FullScreenAdTracker {
    val showing: StateFlow<Boolean>
    fun tryStart(): Boolean   // false when a full-screen ad is already showing
    fun finish()              // idempotent
}
```

`AdsController` thêm `val fullScreenAdShowing: StateFlow<Boolean> = tracker.showing` (công khai, cho T-017), **chỉ thêm nếu chưa có**. Nếu T-017 đã thêm trước, task dùng phần sẵn có và chỉ nối native toàn màn hình vào đó.

**Bộ nhớ quảng cáo tải sẵn có hạn dùng** (file mới `FreshAdCache.kt`, lớp thuần, kiểu tham số để test không cần SDK):

```kotlin
internal class FreshAdCache<T>(private val maxAgeMillis: Long = MAX_AGE_MILLIS, private val onDiscard: (T) -> Unit) {
    fun put(key: AdPlacement, ad: T, nowMillis: Long)   // replaces and discards an older ad of the same key
    fun hasFresh(key: AdPlacement, nowMillis: Long): Boolean
    fun take(key: AdPlacement, nowMillis: Long): T?     // null and discard when older than maxAgeMillis
    fun contains(key: AdPlacement): Boolean
    companion object { const val MAX_AGE_MILLIS = 60 * 60 * 1_000L }
}
```

Đồng hồ là `SystemClock.elapsedRealtime()` (đơn điệu). `onDiscard` của native gọi `NativeAd.destroy()`.

**Nối vào `AdsController`:**

- `internal suspend fun showFullScreenAd(activity: Activity, placement: AdPlacement): Boolean`: `INTERSTITIAL` gọi `showInterstitial` hiện có; `NATIVE_FULL_SCREEN` gọi `showNativeFullScreen(placement)`.
- `showNativeFullScreen`, trên luồng chính, theo thứ tự:
  1. `tracker.showing.value` đang `true`: trả `false`, log `info("Ads", "Native full-screen skipped: ANOTHER_SHOWING")`.
  2. Chưa có native còn hạn (`cache.hasFresh`): gọi tải sẵn, trả `false` ngay, log `NOT_READY`. Không chờ (KB-5, tiêu chí 4).
  3. `awaitFullScreenTurn(placement)` (T-009, gồm đồng ý, công tắc tổng, vị trí bật, `FullScreenAdPacer`, chờ người dùng ngừng chạm tối đa 3 giây) trả `false`: trả `false` (lý do đã do T-009 ghi log).
  4. `cache.take` ra `null` (vừa hết hạn trong lúc chờ): tải sẵn, trả `false`.
  5. `tracker.tryStart()`, `onFullScreenShown()` (T-009 đếm và lưu thời điểm), đặt `_nativeFullScreenAd.value = ShownNativeFullScreenAd(placement, ad)`, log `info("Ads", "Native full-screen shown: <placement key>")`, trả `true`.
- `internal val nativeFullScreenAd: StateFlow<ShownNativeFullScreenAd?>`; `internal fun closeNativeFullScreenAd()`: hủy quảng cáo (`destroy`), đặt `null`, `tracker.finish()`, log `"Native full-screen closed"`, tải sẵn cho lần sau.
- Interstitial nối vào cùng `tracker`: `onAdShowedFullScreenContent` gọi `tryStart()`; `onAdDismissedFullScreenContent` và `onAdFailedToShowFullScreenContent` gọi `finish()`. Không đổi phần còn lại của `showInterstitial`.
- Tải sẵn: `onConsentUpdated` hiện tải sẵn mọi vị trí `INTERSTITIAL`; đổi điều kiện lọc thành `it.format.isFullScreen` và `preloadWhenEnabled` chọn hàm tải theo định dạng. Native toàn màn hình tải bằng `AdLoader` với `NativeAdOptions`: `setAdChoicesPlacement(ADCHOICES_TOP_RIGHT)`, `setMediaAspectRatio(NativeMediaAspectRatio.ANY)`, `setVideoOptions(startMuted = true)`. Nếu T-010 đã có hàm tải native nhận `NativeAdOptions`, dùng lại hàm đó; nếu không, thêm hàm riêng `preloadNativeFullScreen(placement)` trong `AdsController`. Đã có trong bộ nhớ hay vị trí tắt thì không tải. Tải lỗi chỉ ghi `warn` (mã lỗi, không có nội dung quảng cáo).
- Log chỉ có lý do và khóa vị trí, không có chữ của quảng cáo hay dữ liệu cá nhân (ADR-0019). Không ghi sự kiện phân tích (ADR-0022, quyết định 9).

**Màn hình** (file mới `NativeFullScreenAdScreen.kt`), composable không trạng thái, công khai trong module để test và màn hình xem thử (T-014) dùng:

```kotlin
@Composable
fun NativeFullScreenAdScreen(
    assets: NativeAdAssets,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
)
```

Bố cục theo `design/context/NativeFullScreenAdScreen_default_light.md`, mục 3 (tọa độ trên khung 411 x 891):

- Cột phủ màn hình, nền `colorScheme.background`, lề trên `Spacing.lg`, phải và trái `Spacing.md`, dưới `Spacing.xl`, các khối cách nhau `Spacing.md`; cộng thêm `WindowInsets.safeDrawing` để không nằm dưới thanh hệ thống (trên Robolectric phần này bằng 0, ảnh khớp thiết kế).
- Hàng trên cao 48 dp, hai đầu: nút đóng (trái) và chỗ của AdChoices (phải, slot `adChoices`).
- Nút đóng: `IconButton` 48 x 48 dp, `CircleShape`, nền `surfaceContainerHigh`, icon `ic_ads_close` 24 dp màu `onSurface`, `contentDescription` = `R.string.ads_close_full_screen` ("Close ad"), `testTag("nativeFullScreenClose")`. Không có trạng thái tắt, không có đếm ngược.
- Media: hộp chiếm phần còn lại (`weight(1f)`), ít nhất 120 dp chiều cao (`heightIn(min = 120.dp)`), nền `surfaceContainer`, bo `MaterialTheme.shapes.large` (16 dp), nội dung là slot `media`. `testTag("nativeFullScreenMedia")`.
- Đầu quảng cáo: hàng cao 48 dp, icon 48 dp bo `shapes.small` (8 dp; không có icon thì khối nền `secondaryContainer`), cách 12 dp (hằng số native của T-010), cột tiêu đề `titleMedium` `onSurface` một dòng, rồi hàng nhãn "Ad" (thành phần nhãn của T-010, nền `tertiary`, chữ `onTertiary`, `labelSmall`) cách 6 dp với tên nhà quảng cáo `bodySmall` `onSurfaceVariant`.
- Mô tả: `bodyMedium` `onSurfaceVariant`, tối đa 3 dòng, cắt bằng dấu ba chấm.
- CTA: rộng hết hàng, cao 48 dp, nền `tertiaryContainer`, chữ `labelLarge` `onTertiaryContainer`, bo `shapes.small` (8 dp), lề ngang `Spacing.md`. Khác `PrimaryButton` của app (README thiết kế, mục 5). `testTag("nativeFullScreenCta")`.
- Không có giá trị màu, cỡ chữ viết cứng; 2, 6, 12 dp dùng hằng số của T-010 (README thiết kế, mục 3).

Ở khung thấp, media co lại trước (tối thiểu 120 dp); nút đóng và CTA luôn nằm trong màn hình. Với khung 360 x 640, phần còn lại cho media khoảng 316 dp khi mô tả đủ 3 dòng, nên không cần cuộn.

**Phần có SDK** (file mới `NativeFullScreenAdHost.kt`):

- `@Composable fun FullScreenAdHost()` công khai: lấy `LocalAdsController`, không có thì không vẽ gì; theo dõi `nativeFullScreenAd`; khác `null` thì vẽ `NativeFullScreenAdDialog(onClose = ads::closeNativeFullScreenAd)` chứa nội dung có quảng cáo thật.
- `NativeFullScreenAdDialog` (nội bộ): `Dialog` của Compose với `nativeFullScreenDialogProperties()` = `DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)`; `onDismissRequest = onClose`, nên nút quay lại của máy đóng quảng cáo. Kích thước phủ kín cửa sổ.
- Nội dung thật: bọc `NativeAdView` bằng phần dựng của T-010, đăng ký headline, advertiser, body, call to action, icon, `MediaView` (`ImageView.ScaleType.FIT_CENTER`, không kéo giãn hay cắt), rồi `setNativeAd(ad)`; truyền vào `NativeFullScreenAdScreen` qua slot `media` và qua asset. AdChoices do SDK đặt ở góc trên phải, slot `adChoices` để trống.
- Hộp thoại nằm trong `AppTheme` của `:app` nên tự theo chế độ sáng, tối. Xoay màn hình: `AdsController` là singleton nên quảng cáo vẫn còn, hộp thoại được vẽ lại.

**`:app`:** `MainActivity.kt` đặt `FullScreenAdHost()` một lần trong `AppTheme`, sau `Surface { AppNavHost() }`. Không thêm quyền, không thêm thư viện.

**Tài liệu:** `CLAUDE.md` gốc, bước 7: nêu `rememberShowFullScreenAd(placement)` cho interstitial và native toàn màn hình (chỉ ở điểm dừng tự nhiên). `docs/ops/ads-guide.md`: mục 3 bỏ "(đợt 2)" ở dòng native toàn màn hình; mục 5 bước 4 thêm hàm mới.

**Icon đóng:** vector `core/ads/src/main/res/drawable/ic_ads_close.xml` (24 dp, hai nét chéo 12 dp, nét 2 dp theo context), màu do `Icon(tint)` lấy từ `onSurface`. Không dùng `material-icons` (chưa có trong `gradle/libs.versions.toml`).

**Commit test trước (bước 3, L-025):** commit test gồm mọi file test mới (mục 3.2), phần thêm vào `AdUnitIdsTest.kt`, và khung rỗng để test biên dịch: `AdFormat.NATIVE_FULL_SCREEN` (và nhánh mã thử trả chuỗi rỗng), `isFullScreen` trả `false`, `FullScreenAdTracker.tryStart` luôn `true` và `showing` luôn `false`, `FreshAdCache.take` trả `null`, `NativeFullScreenAdScreen` vẽ `Box` rỗng, `nativeFullScreenDialogProperties()` trả `DialogProperties()` mặc định, chuỗi `ads_close_full_screen`. Test thất bại ở phần kiểm tra, không ở biên dịch. Screenshot test thất bại vì chưa có ảnh chuẩn. Commit code ở bước 4 viết phần thật, nối `AdsController`, `MainActivity`, rồi ghi ảnh chuẩn (mục 3.2).

**Ảnh so sánh với thiết kế (ADR-0020):** ảnh chuẩn của `:core:ads` nằm ở `core/ads/src/test/screenshots/`, không ở `feature/ads/`, nên chạy thẳng công cụ so ảnh với thư mục của module core: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens core/ads/src/test/screenshots docs/features/ads/design/review`. Kết quả ghi vào `docs/features/ads/design/review/`. Nếu T-010 đã cho script của ADR-0020 nhận module core, dùng cách T-010 chọn (câu hỏi 5).

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `AdFormat.NATIVE_FULL_SCREEN`, `isFullScreen`; không thêm vị trí |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | Mã thử native video cho `NATIVE_FULL_SCREEN`; `testUnitFor(format)` nếu T-010 chưa tách |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `showFullScreenAd`, `showNativeFullScreen`, `nativeFullScreenAd`, `closeNativeFullScreenAd`, tải sẵn native toàn màn hình, `fullScreenAdShowing` (thêm nếu chưa có), interstitial báo `tracker` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | Chỉ KDoc của `rememberShowInterstitial` trỏ sang `rememberShowFullScreenAd`; không đổi code |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAds.kt` | `rememberShowFullScreenAd(placement)` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdTracker.kt` | Trạng thái quảng cáo toàn màn hình đang hiện, một cái mỗi lúc |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FreshAdCache.kt` | Bộ nhớ quảng cáo tải sẵn có hạn 1 giờ |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | Composable không trạng thái theo thiết kế |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdHost.kt` | `FullScreenAdHost`, `NativeFullScreenAdDialog`, `nativeFullScreenDialogProperties`, phần bọc `NativeAdView` |
| Tạo | `core/ads/src/main/res/drawable/ic_ads_close.xml` | Icon đóng 24 dp |
| Sửa | `core/ads/src/main/res/values/strings.xml` | `ads_close_full_screen` = "Close ad" |
| Sửa | `core/ads/build.gradle.kts` | Chỉ khi T-010 chưa thêm: `implementation(project(":core:designsystem"))`, plugin Roborazzi và `outputDir` `src/test/screenshots` như `core/designsystem/build.gradle.kts`. Không thêm thư viện mới |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | Đặt `FullScreenAdHost()` trong `AppTheme` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Thêm `googleTestUnit_existsForEveryFormat`; không sửa test sẵn có |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` | `isFullScreen` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdTrackerTest.kt` | Một quảng cáo toàn màn hình mỗi lúc |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FreshAdCacheTest.kt` | Hạn dùng, thay thế, hủy |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdsTest.kt` | `rememberShowFullScreenAd` nhận đúng định dạng, không làm gì khi không có controller |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Test giao diện Robolectric: nút đóng, vị trí, chữ, kích thước |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdDialogTest.kt` | Thuộc tính hộp thoại, nút quay lại đóng quảng cáo |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenScreenshotTest.kt` | Ba screenshot test (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdFakes.kt` | `NativeAdAssets` giả (chữ minh họa của thiết kế, chữ dài nhất), media và AdChoices giả cho ảnh |
| Tạo | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_light.png` | Ảnh chuẩn, trùng tên ảnh thiết kế |
| Tạo | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` | Ảnh chuẩn, trùng tên ảnh thiết kế |
| Tạo | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` | Ảnh chuẩn khung thấp (L-028) |
| Ghi lại | `docs/features/ads/design/review/` | Ảnh so sánh và `summary.md`, sinh bằng công cụ so ảnh (mục 3.1) |
| Sửa | `CLAUDE.md` | Bước 7: `rememberShowFullScreenAd` |
| Sửa | `docs/ops/ads-guide.md` | Mục 3 bỏ "(đợt 2)" của native toàn màn hình; mục 5 bước 4 |
| Sửa | `docs/features/ads/tickets/T-013-native-toan-man-hinh.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-013-native-toan-man-hinh.md` | Nhật ký thực hiện |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Không sửa: `docs/remote-config-keys.md`, `docs/remote-config-template.json` (không thêm vị trí hay khóa; test của T-007 xác nhận hai file vẫn khớp), `AdsSettings.kt`, `FullScreenAdPacer.kt`, `FullScreenAdGuard.kt` và test của chúng, `docs/features/ads/design/README.md` (thiết kế đã duyệt), `gradle/libs.versions.toml`, `AndroidManifest.xml`.

Nơi khác đọc các file trên (đã tìm bằng `grep -rn` trong `scripts/`, `.github/`, `build-logic/`, `*/src/test`):

- `CLAUDE.md`: `scripts/new-app.sh` và `scripts/tests/run.sh` đọc và thay tên app trong file này; task chỉ thêm một câu ở bước 7, không đổi tên dự án hay dòng mà script thay, nên không ảnh hưởng. `scripts/check.sh` không đọc file này.
- `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`: chỉ có chú thích trỏ tới "CLAUDE.md, step 10"; task không thêm, bớt hay đánh số lại các bước của mục "Viết một feature mới" (chỉ thêm câu trong bước 7), nên bước 10 vẫn đúng chỗ. Không sửa.
- `scripts/device-session.sh`: chỉ in tên `docs/CLAUDE.md` (mục Khi phân tích một phiên thử), không phải `CLAUDE.md` gốc mà task sửa. Không ảnh hưởng.
- `.github/ISSUE_TEMPLATE/from-app.yml`: chỉ nêu "CLAUDE.md" như một lựa chọn trong danh sách loại đề xuất, không đọc nội dung. Không ảnh hưởng, không chạm `.github/`.
- `core/ads/src/main/res/values/strings.xml`: `scripts/new-app.sh` chỉ sửa `strings.xml` của `:app`, không đọc file của `:core:ads`.
- `docs/STATUS.md`: `scripts/tests/run.sh` chỉ đọc bản của repo giả trong thư mục tạm.
- Ảnh chuẩn mới của `:core:ads`: `scripts/check.sh` tự tìm qua `captureRoboImage("src/test/screenshots/...")`, không cần sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có. Không thêm khóa Remote Config: vị trí native toàn màn hình (khi có) dùng `ads_<vị trí>_enabled` mặc định tắt (ADR-0014) và chung các khóa tần suất của interstitial (spec mục 8).
- Quảng cáo: thêm định dạng native toàn màn hình theo KB-5 (điểm chặn 1). Không thêm, bớt hay di chuyển vị trí; vị trí của màn hình xem thử thuộc T-014.
- Sự kiện phân tích: Không chạm (ADR-0022, quyết định 9).
- Dữ liệu trên máy: không thêm; số đếm và thời điểm do T-009 lưu.
- File R2: Không chạm.

## 4. Kiểm thử

Unit test chạy trên JVM, không cần SDK. Test giao diện và screenshot chạy bằng Robolectric, `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")` trừ khi ghi khác, bọc `AppTheme(darkTheme = ...)`. Quảng cáo giả `FakeNativeAdAssets.Design` dùng đúng chữ minh họa của thiết kế: "Headline of the ad", "Advertiser", "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.", "Install", không có icon (khối `secondaryContainer`). `FakeNativeAdAssets.Longest`: tiêu đề 80 ký tự, mô tả 300 ký tự.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Định dạng toàn màn hình | Unit test | `AdFormatTest.fullScreenFormats_areInterstitialAndNativeFullScreen`: `isFullScreen` đúng với đúng hai định dạng, sai với mọi định dạng còn lại của `AdFormat.entries` |
| KB-8 (phần của task): có mã thử | Unit test | `AdUnitIdsTest.googleTestUnit_existsForEveryFormat`: mọi `AdFormat.entries` có `testUnitFor` bắt đầu bằng `ca-app-pub-3940256099942544/`; `NATIVE_FULL_SCREEN` ra `ca-app-pub-3940256099942544/1044960115` |
| KB-5, tiêu chí 3: một cái mỗi lúc | Unit test | `FullScreenAdTrackerTest.secondStart_whileShowing_isRefused`: `tryStart()` ra `true`, lần hai ra `false`; `showing.value == true` |
| KB-5, tiêu chí 3 | Unit test | `FullScreenAdTrackerTest.finish_allowsNextStart`: `tryStart`, `finish`, `showing.value == false`, `tryStart` ra `true` |
| Đóng hai lần không lỗi | Unit test | `FullScreenAdTrackerTest.finishTwice_isHarmless`: `finish` khi chưa bắt đầu và gọi hai lần, `showing` vẫn `false` |
| Popup mất mạng biết khi quảng cáo đóng (ADR-0024) | Unit test, Turbine | `FullScreenAdTrackerTest.showingFlow_emitsStartAndFinish`: phát `false`, `true`, `false` |
| KB-5, tiêu chí 4: chưa có thì không hiện | Unit test | `FreshAdCacheTest.emptyCache_hasNothingFresh`: `hasFresh` ra `false`, `take` ra `null` |
| Quảng cáo còn hạn được hiện | Unit test | `FreshAdCacheTest.adWithinOneHour_isTaken`: `put` lúc 0, `take` lúc 3_599_999 ra quảng cáo đó; lần `take` sau ra `null` |
| Quảng cáo quá 1 giờ bị bỏ | Unit test | `FreshAdCacheTest.adOlderThanOneHour_isDiscarded`: `put` lúc 0, `hasFresh` lúc 3_600_000 ra `false`, `take` ra `null`, `onDiscard` được gọi đúng một lần với quảng cáo đó |
| Tải mới thay quảng cáo cũ | Unit test | `FreshAdCacheTest.put_replacesAndDiscardsOlderAd`: hai lần `put` cùng vị trí, quảng cáo đầu được hủy, `take` ra quảng cáo sau |
| Chỉ nhận vị trí toàn màn hình | Compose UI test | `FullScreenAdsTest.bannerPlacement_isRejected`: gọi `rememberShowFullScreenAd(AdPlacement.SAMPLE_BOTTOM_BANNER)` (hay vị trí native small sau T-010) ném `IllegalArgumentException` |
| Test và preview không có quảng cáo | Compose UI test | `FullScreenAdsTest.withoutController_actionDoesNothing`: không cấp `LocalAdsController`, gọi hành động trả về không ném lỗi; `FullScreenAdHost()` không tạo nút nào (`onAllNodes(isRoot().not())` rỗng) |
| KB-5, tiêu chí 1: nút đóng thấy ngay | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_isDisplayedAtFirstFrame`: `mainClock.autoAdvance = false`, không tiến đồng hồ, nút `nativeFullScreenClose` hiện, `assertIsEnabled`, có `contentDescription` "Close ad" |
| KB-5, tiêu chí 1: ít nhất 48 dp | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_isAtLeast48dp`: `assertWidthIsAtLeast(48.dp)`, `assertHeightIsAtLeast(48.dp)` |
| KB-5, tiêu chí 1: khác góc AdChoices | Compose UI test | `NativeFullScreenAdScreenTest.closeButton_isTopLeft_awayFromAdChoices`: slot `adChoices` giả có `testTag("adChoices")`; nút đóng `left == 16.dp`, `top == 24.dp`; cạnh phải nút đóng nhỏ hơn cạnh trái AdChoices ít nhất 200 dp; cạnh dưới nút đóng cách cạnh trên CTA hơn 600 dp |
| KB-5, tiêu chí 2: bấm nút đóng | Compose UI test | `NativeFullScreenAdScreenTest.tapClose_callsOnCloseOnce`: `performClick`, bộ đếm `onClose == 1` |
| KB-5, tiêu chí 2: nút quay lại | Unit test | `NativeFullScreenAdDialogTest.dialogProperties_closeOnBackOnly`: `dismissOnBackPress == true`, `dismissOnClickOutside == false`, `usePlatformDefaultWidth == false` |
| KB-5, tiêu chí 2: nút quay lại | Compose UI test | `NativeFullScreenAdDialogTest.backPress_callsOnClose`: dựng `NativeFullScreenAdDialog(onClose)` với nội dung giả, `Espresso.pressBack()`, `onClose` được gọi một lần |
| Đúng thiết kế: chữ và kiểu chữ | Compose UI test | `NativeFullScreenAdScreenTest.texts_followDesign`: thấy "Headline of the ad", "Advertiser", mô tả, "Install" và nhãn "Ad"; CTA cao 48 dp (`nativeFullScreenCta`), media cao ít nhất 120 dp |
| Đúng thiết kế, sáng | Screenshot test | `NativeFullScreenAdScreenScreenshotTest.default_light` ghi `src/test/screenshots/NativeFullScreenAdScreen_default_light.png`, `FakeNativeAdAssets.Design`, media giả nền `surfaceContainer` với nhãn "Media", AdChoices giả 16 dp |
| Đúng thiết kế, tối | Screenshot test | `NativeFullScreenAdScreenScreenshotTest.default_dark` ghi `src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, `FakeNativeAdAssets.Longest` | `NativeFullScreenAdScreenScreenshotTest.maxContent_small` ghi `src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` |
| Màn hình thấp: nút không bị che, cắt | Compose UI test khung `w360dp-h640dp-xxhdpi` | `NativeFullScreenAdScreenTest.smallScreen_closeAndCtaInside`: với `FakeNativeAdAssets.Longest`, nút đóng và CTA nằm trọn trong 360 x 640, media cao ít nhất 120 dp, mô tả không quá 3 dòng |
| Đối chiếu ảnh với thiết kế | Ảnh so sánh, rà soát độc lập theo mục 7 của README thiết kế | Chạy công cụ so ảnh (mục 3.1); `docs/features/ads/design/review/summary.md` có dòng cho hai ảnh `NativeFullScreenAdScreen_default_*`; người rà soát xem ảnh cạnh nhau, lệch không quá 4 dp |
| KB-5, tiêu chí 3 và 4, nút quay lại, xoay màn hình trên máy | Kiểm tra thủ công, sau T-014 | Bản staging, màn hình xem thử: bấm "Native full screen" khi chưa tải xong thì không có gì và màn hình vẫn dùng được; bấm lại sau khi tải thì quảng cáo hiện, nút đóng bấm được ngay, nút quay lại đóng, về đúng màn hình xem thử; bấm "Interstitial" ngay sau đó thì không hiện (khoảng cách 45 giây); xoay máy khi quảng cáo đang hiện thì quảng cáo vẫn còn. `adb logcat -v threadtime \| grep ' VGL\.Ads'` thấy `Native full-screen shown`, `closed`, `skipped: NOT_READY` |

`AdsController` không có unit test (gắn với SDK, như T-009 đã nêu). Phần nối (`showNativeFullScreen`, thứ tự kiểm tra, chỗ gọi `onFullScreenShown`, `tracker` của interstitial) được người rà soát độc lập đọc kỹ.

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt`, `NativeFullScreenAdHost.kt` (phần bọc `NativeAdView` và `MediaView`), `MainActivity.kt`, `AdSlot.kt` (chỉ KDoc), icon vector, `CLAUDE.md` và `ads-guide.md`: không có unit test vì cần SDK thật hay chỉ là tài liệu; kiểm qua build, test trên emulator có sẵn (`AppLaunchTest`: app vẫn khởi động khi có `FullScreenAdHost`) và kiểm tra thủ công sau T-014. Người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (định dạng mới native toàn màn hình; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí hay sự kiện. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là định dạng quảng cáo toàn màn hình (risk-scoring.md, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều file của `:core:ads` (đổi giao diện công khai: `AdFormat`, `rememberShowFullScreenAd`, `NativeFullScreenAdScreen`, `FullScreenAdHost`, `fullScreenAdShowing`) và `:app` (`MainActivity`) |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, thiết kế đã duyệt; AI tự quyết cách hiện (hộp thoại), hạn 1 giờ, mã thử video, không thêm vị trí (câu hỏi 1 đến 4) |
| C. Khả năng kiểm chứng tự động | 2 | Logic, nút đóng, bố cục, ảnh có test tự động; quảng cáo thật, nút quay lại trên máy và tần suất chung chỉ kiểm được bằng tay sau T-014 |
| D. Ảnh hưởng chéo | 3 | `AdsController.kt` cũng do T-009, T-010, T-012, T-017 sửa; `MainActivity.kt` do T-009, T-017 sửa; T-017 dựa vào `fullScreenAdShowing`; T-014 dựa vào hàm mới |
| E. Độ mới của kỹ thuật | 2 | Hộp thoại Compose toàn màn hình bọc `NativeAdView` chưa có trong dự án; phổ biến, có tài liệu của Google |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thêm hàm, đổi điều kiện tải sẵn, interstitial báo trạng thái đang hiện), `AdPlacement.kt` (`AdFormat` thêm giá trị: mọi `when` trên `AdFormat` phải xét thêm nhánh; hiện chỉ có `GoogleTestAdUnitIds`), `AdUnitIds.kt`, `MainActivity.kt`, `core/ads/build.gradle.kts` (nếu T-010 chưa sửa), `CLAUDE.md`, `strings.xml` của `:core:ads`. `docs/remote-config-keys.md` và `docs/remote-config-template.json` không đổi vì `AdPlacement` không đổi. Ảnh chuẩn của `:feature:sample` và `:core:designsystem` không đổi (`LocalAdsController` rỗng trong test).
- Task khác trong sprint có thể xung đột: T-009 (tạo `awaitFullScreenTurn`, `onFullScreenShown`, sửa `MainActivity.kt`), T-010 (thêm `AdFormat`, nhánh mã thử, phần dựng native, `core/ads/build.gradle.kts`, `CLAUDE.md` bước 7), T-012 (thêm `AdFormat.NATIVE_COLLAPSIBLE`, sửa `AdsController.kt`), T-015 (sửa `core/ads/build.gradle.kts`), T-017 (`fullScreenAdShowing`, `MainActivity.kt`), T-014 (dùng `rememberShowFullScreenAd`, `NativeFullScreenAdScreen`, thêm vị trí).
- Đề xuất thứ tự chạy: sau T-007, T-009, T-010 (bắt buộc: `depends_on`), sau T-012 nếu cùng phiên (cùng thêm nhánh vào `AdFormat` và `GoogleTestAdUnitIds`), trước T-014. Với T-017: chạy trước hay sau đều được nhưng không cùng lúc; ticket nào chạy trước thì thêm `fullScreenAdShowing`. Không chạy song song với T-015 (cùng `core/ads/build.gradle.kts`).

## 7. Điều kiện dừng riêng

- Hợp đồng của T-009 hay T-010 khác giả định ở mục 2 (tên `awaitFullScreenTurn`, `onFullScreenShown`, `NativeAdAssets`, phần dựng `NativeAdView`): dừng, không tự viết lại phần của task khác.
- Cần thêm thư viện (ví dụ `material-icons`, `lifecycle-process`) hay sửa `gradle/libs.versions.toml`, `build-logic/`.
- Cần thêm vị trí vào `AdPlacement` để test hay để thử (thuộc T-014, đổi bảng khóa).
- `NativeAdView` trong `Dialog` của Compose không nhận được lượt hiển thị hay không bấm được CTA trên emulator với mã thử, sau 3 lần sửa: dừng, ghi lại để chọn cách khác (câu hỏi 2).
- Ảnh so sánh lệch thiết kế quá một bước lưới 4 dp ở bố cục hay vị trí nút đóng mà không sửa được bằng token và hằng số đã có.
- Phải hiện quảng cáo khi chưa tải sẵn (chờ tải) để qua test: trái KB-5, tiêu chí 4.

## 8. Câu hỏi cho người duyệt

1. **Không thêm vị trí native toàn màn hình trong task này.** Spec mục 5 chỉ có vị trí của màn hình xem thử (T-014). AI đề xuất: T-013 chỉ thêm định dạng, hàm và màn hình; T-014 thêm vị trí (ví dụ `gallery_native_full_screen`) và sinh lại bảng khóa. Hệ quả: tới khi T-014 xong, native toàn màn hình chưa thử được trên bản staging; mục "Có trong bản staging" của ticket hoàn thành cùng T-014. Phương án khác: thêm một vị trí mẫu ở `:feature:sample`, nhưng spec không có điểm dừng tự nhiên nào ở màn hình mẫu.
2. **Hiện bằng hộp thoại Compose hay bằng một Activity riêng.** AI đề xuất hộp thoại toàn màn hình do `FullScreenAdHost` (đặt một lần ở `:app`) vẽ: không phải khai báo Activity mới trong manifest, theo được theme sáng tối, màn hình bên dưới giữ nguyên. Phương án khác: Activity riêng trong `:core:ads` (tách hẳn khỏi giao diện app, nhưng thêm khai báo manifest và chuyển quảng cáo qua giữa hai Activity).
3. **Mã thử cho native toàn màn hình.** AI đề xuất mã thử native video (`.../1044960115`) để thử media video trên màn hình lớn; các cỡ native khác dùng mã native thường của T-010. Hay dùng chung mã native thường?
4. **Hạn dùng của native tải sẵn: 1 giờ, hằng số trong code.** Theo khuyến nghị của Google; không phải khóa Remote Config vì spec mục 8 không có. Đồng ý không?
5. **Ảnh so sánh của module core.** Script của ADR-0020 chỉ tìm ảnh ở `feature/<feature>/`, còn native nằm ở `core/ads/`. Plan chạy thẳng công cụ Java với thư mục của `:core:ads` và không sửa script. Nếu T-010 đã sửa script (thêm tham số thư mục ảnh), task dùng cách đó. Có muốn đề xuất sửa script ở một ticket riêng không?
6. **Một lần gọi khi interstitial đang hiện.** AI đề xuất bỏ qua ngay (không xếp hàng chờ) để không có hai quảng cáo toàn màn hình liền nhau (`ads-guide.md`, bảng "không bao giờ làm").

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
