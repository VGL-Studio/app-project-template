---
ticket: T-014
title: Màn hình xem thử quảng cáo chỉ ở bản staging
status: approved               # draft | approved | stopped | done
version: 3                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, build)
risk_level: high             # low | medium | high
token_estimate: 30M          # cỡ M (gợi ý 20M của scripts/token-usage.py estimate, 2026-10-21), nâng lên: 3 trang, 7 ảnh chuẩn, sửa cả :core:ads và :app
blocking_points: [ads, core, build]
run_order:                  # AI điền sau họp đánh giá rủi ro
reviewer: Nhật            # người duyệt
cross_reviewer: Nhật            # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-014: MÀN HÌNH XEM THỬ QUẢNG CÁO CHỈ Ở BẢN STAGING

**Phiên bản 3 (2026-10-23)**, sau rà soát trước phiên của RUN-20261023-01 (mục Kiểm tra trước khi duyệt). Thay đổi so với phiên bản 2 (đã duyệt 2026-10-23):

- `AdPlacementTest.otherPlacements_areNotStagingOnly` dùng `SAMPLE_NATIVE_SMALL` thay `SAMPLE_BOTTOM_BANNER`: T-010 đổi vị trí mẫu, nên tên cũ không còn khi T-014 chạy (test không biên dịch được).
- `docs/ops/ads-guide.md` mục 8: dòng "quảng cáo không hiện trên staging" đã được T-008 thay bằng câu về giá trị cục bộ của staging. T-014 giữ câu của T-008, chỉ thêm phần màn hình xem thử.
- Mục 3.3 có hai dòng "Quyền truy cập:", "Thư viện và SDK:" (dẫn tới spec mục 6); thêm mục 3.4 (phương án dự phòng) và mục 4.1 (chạy thử bằng `scripts/check.sh`: activity và source set chỉ ở staging, biểu tượng thứ hai, test ở `src/testStaging`, Roborazzi ở `:app`).
- Mục 9 để trống cho phiên bản 3; quyết định của phiên bản 2 giữ nguyên.
- Theo rà soát trước khi chạy: phần đo và cuộn ở khung thấp chuyển từ `AdGalleryScreenScreenshotTest` sang `AdGalleryScreenTest` (`@Config` khung thấp trên từng hàm), lớp screenshot chỉ chụp; `lifecycle-runtime-compose` luôn thêm nếu `:app` chưa có; `robolectric.properties` đã có từ T-017 với hai dòng; ghi chú `AdGalleryActivity` không tính phiên (mục 3.1, mục 4).

## 1. Tóm tắt

Thêm `AdGalleryScreen` vào source set `app/src/staging`, mở từ một biểu tượng riêng trên máy ("Ad gallery") do `AdGalleryActivity` khai báo trong manifest của staging; bản production không có lớp, chuỗi hay khai báo nào của màn hình này. Màn hình có ba trang theo thiết kế (mặc định: native small, medium, hai nút quảng cáo toàn màn hình, nút mở Ad Inspector; trong danh sách; thu gọn), dùng 7 vị trí riêng `gallery_*` trong `AdPlacement` (chỉ staging, nhận ra bằng tiền tố khóa `gallery_` mà `scripts/release.sh` bỏ qua sau PR #81), và mở Ad Inspector qua một hàm mới của `:core:ads`. Kiểm chứng bằng unit test ViewModel và vị trí, test giao diện Robolectric, screenshot test trùng tên 5 ảnh thiết kế kèm ảnh khung thấp, và test quét mã nguồn chứng minh ngoài `src/staging` không có gì của màn hình xem thử.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-8 | Khi mở bản staging thì mọi loại quảng cáo hiện được bằng mã thử của Google, không cần project Firebase thật. | Phần của T-014: mỗi định dạng có một vị trí `gallery_*` (7 vị trí, mục 3.1). Mã thử có sẵn qua `GoogleTestAdUnitIds` (T-010, T-012, T-013 đã thêm nhánh cho mọi định dạng). Khóa `ads_gallery_*_enabled` tự bật trên staging nhờ `StagingRemoteValuesModule` của T-008 (lặp qua `AdPlacement.entries`), không cần Firebase. Tần suất của interstitial và native toàn màn hình giữ như production (spec mục 5): bấm lại trong 45 giây, hay trong 180 giây ân hạn phiên đầu, thì không hiện. |
| KB-8 | Khi mở màn hình xem thử quảng cáo (chỉ có ở staging) thì thấy từng loại native, nút gọi interstitial và native toàn màn hình, và mở được Ad Inspector của AdMob. | Trang mặc định: small, medium, nút "Interstitial", "Native full screen", "Open Ad Inspector" (ảnh `AdGalleryScreen_default_*`). Trang "trong danh sách" và "thu gọn" (ảnh `_inlist_light`, `_collapsible-*`). Native toàn màn hình hiện qua `rememberShowFullScreenAd` của T-013. Ad Inspector mở bằng hàm mới của `:core:ads`; `:app` không gọi SDK quảng cáo. Ad Inspector của Google chỉ mở trên máy thử (emulator, hoặc máy đã đăng ký thiết bị thử trên AdMob); máy chưa đăng ký thì màn hình báo lỗi ngắn và ghi log (câu hỏi 4). |
| KB-8 | Bản production không có màn hình xem thử và không có giá trị cục bộ. | Phần "không có màn hình xem thử": mọi code, chuỗi, khai báo activity nằm trong `app/src/staging`; `app/src/main` và `app/src/production` không nhắc tới. Vị trí `gallery_*` nằm trong `AdPlacement` (dùng chung) nhưng `stagingOnly` (suy từ tiền tố khóa `gallery_`): production không cần và không dùng mã quảng cáo cho chúng, script phát hành bỏ qua chúng (PR #81). Phần "không có giá trị cục bộ" thuộc T-008. |

Gián tiếp: màn hình này là nơi kiểm thủ công KB-1 đến KB-5, KB-9 (Ad Inspector thấy adapter AppLovin của T-015), theo các dòng "kiểm tra thủ công sau T-014" trong plan T-010 đến T-013.

Thiết kế tham chiếu: `features/ads/design/README.md` (`approved` 2026-10-21), ảnh trong `design/screens/`: `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`; số liệu trong `design/context/` cùng tên. README mục 1 gán `_inlist_light` cho T-011 và hai ảnh `_collapsible-*` cho T-012; plan T-011 đã ghi ảnh `_inlist_light` thuộc T-014; theo quyết định của anh Nhật (2026-10-23, câu hỏi 2), T-014 ghi cả 5 ảnh ở màn hình thật (`:app`, staging); T-012 chỉ ghi ảnh component tên `CollapsibleNativeAd_*`, không ghi ảnh mang tên thiết kế và không tạo ảnh so sánh cho hai ảnh `_collapsible-*`.

ADR liên quan: ADR-0001 (`:app` ghép module), ADR-0007 (tách staging, production), ADR-0013 (mã thử ở staging, quảng cáo chỉ sau đồng ý), ADR-0014 (vị trí mặc định tắt; ở staging bật bằng giá trị cục bộ), ADR-0019 (log qua `AppLogger`), ADR-0020 (tên screenshot trùng tên ảnh thiết kế, ảnh so sánh), ADR-0022 (native chỉ qua composable của `:core:ads`), ADR-0023 (khóa vị trí khai báo một chỗ, bảng khóa sinh từ code). ADR-0024 (luôn cần mạng) không đổi cách làm: popup mất mạng của T-017 nằm ở `MainActivity`, không phủ lên `AdGalleryActivity`; không có mạng thì native hết thời gian chờ và thu lại như mọi chỗ khác.

## 3. Giải pháp

### 3.1. Cách làm

Giả định PR #81 (`scripts/release.sh` bỏ qua vị trí `gallery_*` và đọc định dạng có gạch dưới; `scripts/check.sh` so ảnh chuẩn của `:app`) và T-007 đến T-013 đã merge, với đúng tên trong hợp đồng chung: `AdsSettings.placementEnabled`, `AdsSettings.all()`, `AppRemoteKeys`, `RemoteConfigKeysTest` (T-007); `StagingRemoteValuesModule` (T-008); `AdsController` có `AppLogger` (T-009); `AdFormat.NATIVE_SMALL`, `NATIVE_MEDIUM`, `NativeAdAssets`, `NativeAdSize`, `NativeAdCard`, `NativeAdSlot` (T-010); `rememberInListAds`, `InListAds.every`, `itemsWithInListAds`, `inListAdPositions` (T-011); `AdFormat.NATIVE_COLLAPSIBLE`, `CollapsibleNativeAdSlot`, `CollapsibleNativeAdCard` (T-012); `AdFormat.NATIVE_FULL_SCREEN`, `rememberShowFullScreenAd`, `FullScreenAdHost` (T-013).

**Vị trí riêng, chỉ staging (`:core:ads`, `AdPlacement.kt`).** `AdPlacement` thêm thuộc tính suy ra `val stagingOnly: Boolean get() = key.startsWith(STAGING_ONLY_KEY_PREFIX)` với `STAGING_ONLY_KEY_PREFIX = "gallery_"` (KDoc: "Exists only for the staging ad gallery: production has no ad unit for it and never shows it. scripts/release.sh skips the same prefix"). Không thêm tham số vào constructor, để mọi dòng khai báo giữ đúng mẫu `TEN("khoa", AdFormat.X),` mà `scripts/release.sh` đọc (ads-guide mục 5); một nguồn sự thật duy nhất là tiền tố, trùng quy tắc của script sau PR #81. Các vị trí đã có giữ nguyên dòng khai báo. Thêm 7 vị trí, mỗi vị trí một dòng theo đúng mẫu đó:

| Hằng | Khóa | Định dạng | Dùng ở |
| --- | --- | --- | --- |
| `GALLERY_NATIVE_SMALL` | `gallery_native_small` | `NATIVE_SMALL` | Trang mặc định |
| `GALLERY_NATIVE_MEDIUM` | `gallery_native_medium` | `NATIVE_MEDIUM` | Trang mặc định |
| `GALLERY_INLIST_SMALL` | `gallery_inlist_small` | `NATIVE_SMALL` | Trang trong danh sách, quảng cáo thứ 1, 3, 5... |
| `GALLERY_INLIST_MEDIUM` | `gallery_inlist_medium` | `NATIVE_MEDIUM` | Trang trong danh sách, quảng cáo thứ 2, 4, 6... |
| `GALLERY_NATIVE_COLLAPSIBLE` | `gallery_native_collapsible` | `NATIVE_COLLAPSIBLE` | Trang thu gọn |
| `GALLERY_INTERSTITIAL` | `gallery_interstitial` | `INTERSTITIAL` | Nút "Interstitial" |
| `GALLERY_NATIVE_FULL_SCREEN` | `gallery_native_full_screen` | `NATIVE_FULL_SCREEN` | Nút "Native full screen" |

Hai vị trí in-list riêng vì một vị trí chỉ có một định dạng, mà thiết kế xen small (sau mục 3) và medium (sau mục 6) trong cùng danh sách.

**Production không cần mã quảng cáo cho vị trí chỉ staging (`AdUnitIds.kt`).** `ConfiguredAdUnitIds.missing` bỏ qua vị trí `stagingOnly`; `ConfiguredAdUnitIds.of` trả `null` cho chúng kể cả khi cấu hình có khóa (production không bao giờ hiện). `GoogleTestAdUnitIds` không đổi (trả mã thử theo định dạng). Kiểm tra trước khi phát hành của script phát hành đọc danh sách vị trí theo mẫu dòng trong `AdPlacement` và, sau PR #81, bỏ qua khóa bắt đầu bằng `gallery_` (ads-guide.md, mục 5; câu hỏi 1 đã trả lời). AI không mở, không chạy, không sửa script đó (ADR-0017); test `AdPlacementTest` bảo đảm `stagingOnly` đúng bằng tiền tố `gallery_` nên hai bên không lệch. T-014 cần PR #81 merge trước (điều kiện dừng 1).

**Bảng khóa.** `AdsSettings.all()` lặp qua `AdPlacement.entries` nên tự có 7 khóa `ads_gallery_*_enabled` (boolean, mặc định `false`). `AdsSettings.placementEnabled` thêm vào mô tả câu "Staging ad gallery only." khi `placement.stagingOnly`, để người vận hành không bật nhầm trên production. Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng lệnh của T-007: `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Phương án bỏ hẳn các khóa này khỏi bảng: câu hỏi 3.

**Ad Inspector qua `:core:ads` (file mới `AdInspector.kt`).**

```kotlin
/** Result of opening AdMob's Ad Inspector. */
sealed interface AdInspectorResult {
    /** The inspector was shown and the user closed it. */
    data object Closed : AdInspectorResult
    /** The SDK refused, for example on a device that is not a test device. [code] is the SDK error code. */
    data class Failed(val code: Int) : AdInspectorResult
    /** Ads are not started yet (no consent, or no ads controller in tests and previews). */
    data object NotReady : AdInspectorResult
}

/** Opens AdMob's Ad Inspector, for the staging ad gallery only. Never call it from a feature. */
@Composable
fun rememberOpenAdInspector(onResult: (AdInspectorResult) -> Unit): () -> Unit

internal fun adInspectorResult(errorCode: Int?): AdInspectorResult   // null -> Closed, else Failed(code)
```

- `rememberOpenAdInspector` lấy `LocalAdsController` và `LocalActivity` (như `AdSlot.kt`); không có một trong hai thì hành động trả `NotReady`.
- `AdsController` thêm `internal fun openAdInspector(activity: Activity, onResult: (AdInspectorResult) -> Unit)`: chưa `canRequestAds` thì `NotReady`; có thì gọi `MobileAds.openAdInspector(activity) { error -> onResult(adInspectorResult(error?.code)) }`. Log thẻ `Ads` (ADR-0019): `Ad Inspector opened`, `Ad Inspector closed`, `Ad Inspector failed: <code>`, `Ad Inspector skipped: ads not started`. Không ghi gì khác.
- Không thêm thư viện: `MobileAds.openAdInspector` có trong `play-services-ads` đang dùng.

**Xen hai vị trí trong một danh sách (`InListAds.kt` của T-011).** Thêm một hàm công khai:

```kotlin
/** Like [itemsWithInListAds], with ads taken in turn from [ads]: slot k uses ads[k % ads.size]. N is ads.first().every. */
fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    ads: List<InListAds>,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
)
```

Gọi lại hàm nội bộ nhận `positions` và `adContent` của T-011; `require(ads.isNotEmpty())`; khóa của hàng quảng cáo vẫn `InListAdKey(placementKey, slot)` của vị trí được dùng. Không đổi hàm có sẵn. Nếu người duyệt muốn tránh đổi API này, trang trong danh sách chỉ dùng `GALLERY_INLIST_SMALL` và ảnh `_inlist_light` khác thiết kế ở quảng cáo thứ hai (câu hỏi 5).

**Lối vào chỉ ở staging.** File mới `app/src/staging/AndroidManifest.xml` khai báo `AdGalleryActivity` (`exported="true"`, `intent-filter` `MAIN` và `LAUNCHER`, nhãn `@string/ad_gallery_launcher_label` = "Ad gallery", cùng icon và theme với app). Manifest được gộp chỉ vào biến thể staging. Không thêm quyền. Lý do chọn activity riêng thay vì một đích trong `AppNavHost`: không phải sửa `MainActivity.kt`, `AppNavHost.kt` (T-013, T-017 cùng sửa), không phải đặt nút lối vào trong giao diện của app, và production không có một dòng nào về màn hình này. Đánh đổi: bản staging có hai biểu tượng trên máy (câu hỏi 6).

**`AdGalleryActivity`** (`app/src/staging/.../adgallery/AdGalleryActivity.kt`, `@AndroidEntryPoint`): nhận `AdsController`, gọi `ads.gatherConsent(this)` trong `onCreate` và `ads.onUserInteraction()` như `MainActivity` (người thử có thể mở thẳng màn hình xem thử từ biểu tượng). `AdGalleryActivity` không gọi `onAppForeground`/`onAppBackground` của T-009 (không sửa phần đếm phiên): phiên và giới hạn mỗi phiên chỉ được đếm theo `MainActivity`; `setContent { CompositionLocalProvider(LocalAdsController provides ads) { AppTheme { Surface(fillMaxSize) { AdGalleryRoute(onExit = ::finish) }; FullScreenAdHost() } } }`, đúng thứ tự T-013 đặt ở `MainActivity`. `enableEdgeToEdge()` và `safeDrawingPadding()` như app.

**Trạng thái và ViewModel** (`AdGalleryUiState.kt`, `AdGalleryViewModel.kt`, theo mẫu `:feature:sample`, đặt ở `:app` staging vì màn hình chỉ có ở đây):

```kotlin
sealed interface AdGalleryUiState {
    data object Default : AdGalleryUiState
    data class InList(val itemCount: Int) : AdGalleryUiState
    data class Collapsible(val itemCount: Int) : AdGalleryUiState
}
```

- `@HiltViewModel class AdGalleryViewModel @Inject constructor(private val logger: AppLogger)`: `uiState: StateFlow<AdGalleryUiState>` bắt đầu `Default`; `openInList()` sang `InList(LIST_ITEM_COUNT)`, `openCollapsible()` sang `Collapsible(LIST_ITEM_COUNT)`, `LIST_ITEM_COUNT = 30`; `back(): Boolean` về `Default` và trả `true`, đang ở `Default` thì trả `false` (activity đóng); `onFullScreenRequested(kind: GalleryFullScreen)` (`INTERSTITIAL`, `NATIVE_FULL_SCREEN`) chỉ ghi log; `onAdInspectorResult(result)` ghi log.
- Log thẻ `AdGallery`: `page=in_list`, `page=collapsible`, `page=default`, `full_screen_requested kind=interstitial|native_full_screen`, `ad_inspector result=closed|failed code=<n>|not_ready`. Không ghi nội dung quảng cáo. Không có sự kiện phân tích, cờ, DataStore. Trang hiện tại giữ qua xoay màn hình nhờ ViewModel.

**Giao diện** (`AdGalleryScreen.kt`):

- `AdGalleryRoute(viewModel, onExit)`: activity lấy ViewModel bằng `by viewModels()` (Hilt có sẵn qua `@AndroidEntryPoint`, không cần thư viện `hilt-navigation-compose`) và truyền vào; `BackHandler` gọi `back()`, trả `false` thì `onExit()`; `rememberShowFullScreenAd(GALLERY_INTERSTITIAL)`, `rememberShowFullScreenAd(GALLERY_NATIVE_FULL_SCREEN)`, `rememberOpenAdInspector`; kết quả `Failed(code)` hiện `Toast` với chuỗi `ad_gallery_inspector_failed` ("Ad Inspector unavailable (code %1$d). Register this phone as a test device in AdMob."), `NotReady` hiện `ad_gallery_inspector_not_ready` ("Ads are not started yet (consent)."). Toast là API của Android trong `:app` staging, không phải SDK quảng cáo.
- `AdGalleryScreen(uiState, slots: AdGallerySlots, onOpenInList, onOpenCollapsible, onShowInterstitial, onShowNativeFullScreen, onOpenAdInspector, modifier)` không trạng thái. `AdGallerySlots` là interface gom phần quảng cáo, để screenshot dùng quảng cáo giả và màn hình thật dùng composable của `:core:ads`:

```kotlin
interface AdGallerySlots {
    @Composable fun Small(modifier: Modifier)
    @Composable fun Medium(modifier: Modifier)
    @Composable fun Collapsible(modifier: Modifier)
    /** N of the in-list page, or null while in-list ads are off. */
    @Composable fun inListEvery(): Int?
    fun LazyListScope.listWithAds(items: List<Int>, itemContent: @Composable LazyItemScope.(Int) -> Unit)
}
```

  Bản thật `SdkAdGallerySlots` (cùng file): `NativeAdSlot(GALLERY_NATIVE_SMALL)`, `NativeAdSlot(GALLERY_NATIVE_MEDIUM)`, `CollapsibleNativeAdSlot(GALLERY_NATIVE_COLLAPSIBLE)`, `rememberInListAds` cho hai vị trí in-list (tạo ở mức trang, ngoài `LazyColumn`) và `itemsWithInListAds(items, listOf(small, medium))`. Bản giả `PreviewAdGallerySlots(assets, collapsed, every)` nằm trong `app/src/testStaging`, vẽ `NativeAdCard`, `CollapsibleNativeAdCard` và chèn ô theo `inListAdPositions(items.size, every)` với lề dọc 16 dp (`Spacing.md`), small và medium xen nhau.
- **Trang mặc định** theo `context/AdGalleryScreen_default_light.md`: tiêu đề "Ad gallery (staging)" `titleLarge` (lề trái 16, trên 20); nội dung bắt đầu y = 72, các khối cách nhau 16 (`Spacing.md`), lề ngang 16: nhãn "Native small" (`titleSmall`, `onSurfaceVariant`), ô small; nhãn "Native medium", ô medium; hàng hai `PrimaryButton` "Interstitial" và "Native full screen" chia đều chiều rộng, cách nhau 12 dp (giá trị ngoài hệ thống của thiết kế, giữ làm hằng số riêng của màn hình, như T-010 làm với 12 dp trong quảng cáo); `PrimaryButton` "Open Ad Inspector" rộng hết. Dưới cùng, ngoài thiết kế (câu hỏi 7): hàng hai `OutlinedButton` của Material 3 "In-list ads", "Collapsible ad", cao ít nhất 48 dp, cách khối trên 16 dp. Cả trang là một `Column` cuộn dọc (`verticalScroll`) để khung thấp cuộn được.
- **Trang trong danh sách** theo `context/AdGalleryScreen_inlist_light.md`: tiêu đề "In-list ads (every %1$d items)" với N từ `inListEvery()`, null thì "In-list ads (off)"; `LazyColumn` lề ngang 16, mục "List item %1$d" `bodyLarge`, lề dọc 16, viền dưới 1 dp `outlineVariant`; quảng cáo chèn bằng `listWithAds`. Màn hình thật dùng N từ `ads_inlist_every_count` (mặc định 8, đổi được từ xa) để thử đúng điều T-011 làm; ảnh chuẩn dùng N = 3 và 7 mục như thiết kế.
- **Trang thu gọn** theo `context/AdGalleryScreen_collapsible-*_light.md`: tiêu đề "Collapsible ad"; `LazyColumn` các mục chiếm phần còn lại (`weight(1f)`); quảng cáo thu gọn ghim ở đáy, cách mép dưới và hai bên 16 dp.
- Mọi màu, chữ, khoảng cách lấy từ `MaterialTheme` và `Spacing`; mọi chữ của app nằm trong `app/src/staging/res/values/strings.xml` (tiếng Anh, README thiết kế mục 2). Chữ "List item N" là chuỗi có tham số. Test tag: `ad_gallery_screen`, `ad_gallery_default`, `ad_gallery_inlist`, `ad_gallery_collapsible`, `ad_gallery_inspector_button`.

**Bằng chứng production không có màn hình xem thử, hai lớp** (không build production trên máy: thiếu `google-services.json` của production, câu hỏi 3 của T-008 đã trả lời 2026-10-23):

1. Test quét mã nguồn trong `app/src/test` (chạy trong `testStagingDebugUnitTest` mà CI chạy), như T-008 làm với `LocalRemoteValues`: ngoài `src/staging` và `src/testStaging`, không file nào của `:app` (`.kt`, `.xml`) chứa `AdGallery` hay `ad_gallery`; `src/staging` có `AdGalleryActivity.kt` và manifest staging khai báo nó (để test không qua vì đường dẫn sai); `src/main/AndroidManifest.xml` có đúng một activity `LAUNCHER`.
2. Unit test `:core:ads`: `ConfiguredAdUnitIds` không đòi và không trả mã cho vị trí `stagingOnly`.

**Tài liệu.** `docs/ops/ads-guide.md`: mục 5 đã có quy tắc `gallery_` từ PR #81, chỉ thêm một câu "`AdPlacement.stagingOnly` suy từ tiền tố này; `ConfiguredAdUnitIds` không đòi và không trả mã production cho các vị trí đó"; mục 8 giữ dòng T-008 đã viết ("staging bật mọi vị trí của `AdPlacement` bằng giá trị cục bộ (`StagingRemoteValuesModule`); Firebase staging vẫn tắt được từng vị trí"; dòng cũ "quảng cáo không hiện trên staging" không còn sau T-008), thêm sau dòng đó một gạch đầu dòng về màn hình xem thử (biểu tượng "Ad gallery", ba trang, tần suất như production, Ad Inspector chỉ mở trên emulator hay máy đã đăng ký thiết bị thử trên AdMob, đường dẫn hướng dẫn chính thức). Nếu khi task chạy mục 8 vẫn còn dòng "quảng cáo không hiện trên staging" thì T-008 chưa merge: dừng (mục 7). `docs/features/ads/design/README.md` mục 8: kết quả đối chiếu 5 ảnh `AdGalleryScreen_*`.

**Nơi khác đọc file plan đổi (L-020).** `grep -rln` trên `scripts/`, `.github/`, `build-logic/`, mọi `src/test`:

- `AdPlacement.kt`: script phát hành đọc theo mẫu dòng `TEN("khoa", AdFormat.X),` và bỏ khóa `gallery_*` (PR #81, câu hỏi 1); 7 dòng mới giữ đúng mẫu nên script thấy và bỏ qua chúng, vị trí khác không đổi. `scripts/tests/fixtures/release/AdPlacement.kt` là bản mẫu riêng của test script, không đọc file thật, không sửa; `AdUnitIdsTest.kt` so `missing` với `AdPlacement.entries` (sửa, mục 3.2); test sinh bảng khóa của T-007 đọc `AdPlacement.entries` qua `AdsSettings.all()` (hai file sinh lại, mục 3.2); `StagingRemoteValuesModuleTest` của T-008 lặp qua `AdPlacement.entries` nên tự gồm 7 vị trí mới, không sửa.
- `app/build.gradle.kts`: `scripts/new-app.sh` sửa dòng `applicationId`, `namespace`; `scripts/release.sh` truyền các tham số `-P` của bản production mà file build đọc. Plan chỉ thêm plugin, khối `roborazzi` và dòng phụ thuộc, không đụng các khối `signingConfigs`, `productFlavors` hay phần kiểm tham số production, nên hai script này không bị ảnh hưởng (plan không mở hay sửa `scripts/release.sh`, ADR-0017).
- Bảng tóm tắt `review/summary.md` của thiết kế `ads`: plan không sửa (T-010, T-012 có thể tạo); 5 dòng của `AdGalleryScreen_*` ghi vào README mục 8, tránh xung đột với hai task đó.
- `scripts/check.sh` (sau PR #81) duyệt cả `app/`, đọc mọi `src/test*` (gồm `src/testStaging`), đòi mỗi `captureRoboImage` ghi rõ `"src/test/screenshots/<Tên>.png"` và chạy `:app:verifyRoborazziStagingDebug`: 8 ảnh chuẩn ở mục 3.2 phải có trong commit, thiếu ảnh thì CI báo lỗi (câu hỏi 8). Script so sánh thiết kế bọc sẵn không đổi ở PR #81 nên vẫn chạy thẳng chương trình so sánh (cuối mục 3.2).
- Ảnh chuẩn mới nằm ở `app/src/test/screenshots/`, không thay ảnh chuẩn có sẵn nào. Screenshot của `:feature:sample` không đổi (`LocalAdsController` rỗng trong test).

**Thứ tự commit (L-025):**

1. Commit test (bước 3): mọi file test mới ở mục 3.2, phần thêm và phần đổi kỳ vọng trong `AdUnitIdsTest.kt`, `InListAdsTest.kt`, `app/src/test/resources/robolectric.properties`, và phần test của `app/build.gradle.kts` (plugin Roborazzi, `testImplementation`). Test không biên dịch được vì `stagingOnly`, các vị trí `GALLERY_*`, `AdInspectorResult`, `adInspectorResult`, `itemsWithInListAds(items, List<InListAds>)`, `AdGalleryUiState`, `AdGalleryViewModel`, `AdGalleryScreen`, `AdGallerySlots` chưa có: lý do thất bại mong đợi, ghi vào nhật ký. `AdGallerySourceSetTest` qua ngay (xem ngoại lệ).
2. Commit code (bước 4): phần `:core:ads`, phần `app/src/staging`, phần còn lại của `app/build.gradle.kts`, hai file bảng khóa sinh lại.
3. Commit ảnh chuẩn: `./gradlew :app:recordRoborazziStagingDebug`, xem từng ảnh, rồi `scripts/check.sh` qua (gồm `:app:verifyRoborazziStagingDebug`, như CI).
4. Commit tài liệu: `ads-guide.md`, kết quả đối chiếu thiết kế.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Thuộc tính `stagingOnly` suy từ tiền tố `gallery_`, 7 vị trí `GALLERY_*` đúng mẫu dòng script phát hành đọc, KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | `ConfiguredAdUnitIds`: `missing` bỏ qua, `of` trả `null` cho vị trí `stagingOnly` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Mô tả khóa của vị trí `stagingOnly` thêm "Staging ad gallery only." Không đổi tên khóa hay mặc định |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `internal fun openAdInspector(activity, onResult)`, log |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdInspector.kt` | `AdInspectorResult`, `rememberOpenAdInspector`, `adInspectorResult` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Hàm công khai `itemsWithInListAds(items, ads: List<InListAds>, ...)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Kỳ vọng mới của `missing` (bỏ vị trí `stagingOnly`), test `of` trả `null` cho vị trí `stagingOnly`, mã thử cho mọi vị trí `gallery_*` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Vị trí `gallery_*`: đúng 7, đều `stagingOnly`, khóa không trùng, đủ định dạng; vị trí khác không `stagingOnly` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdInspectorTest.kt` | `adInspectorResult`, `rememberOpenAdInspector` không có controller |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Test xen hai vị trí |
| Sửa | `app/build.gradle.kts` | Luôn thêm `implementation(libs.androidx.lifecycle.runtime.compose)` nếu file chưa có (T-017 không thêm dòng này; `:app` hiện chưa có). Phần Roborazzi chỉ thêm nếu T-017 chưa thêm (giống hệt dòng của plan T-017; chạy sau thì giữ phần đã có, không thêm lần hai): plugin `alias(libs.plugins.roborazzi)`, khối `roborazzi { outputDir.set(layout.projectDirectory.dir("src/test/screenshots")) }`, `testImplementation(libs.roborazzi)`, `testImplementation(libs.roborazzi.compose)`, `testImplementation(libs.roborazzi.junit.rule)`. Mọi alias đã có trong `gradle/libs.versions.toml` |
| Tạo | `app/src/staging/AndroidManifest.xml` | Khai báo `AdGalleryActivity` với `LAUNCHER`, nhãn "Ad gallery". Không quyền |
| Tạo | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryActivity.kt` | Activity, đồng ý quảng cáo, `LocalAdsController`, `FullScreenAdHost` |
| Tạo | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryUiState.kt` | `AdGalleryUiState`, `GalleryFullScreen` |
| Tạo | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModel.kt` | ViewModel, log |
| Tạo | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `AdGalleryRoute`, `AdGalleryScreen`, ba trang, `AdGallerySlots`, `SdkAdGallerySlots` |
| Tạo | `app/src/staging/res/values/strings.xml` | Chữ của màn hình (tiếng Anh), nhãn biểu tượng, hai thông báo Ad Inspector |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/AdGallerySourceSetTest.kt` | Test quét mã nguồn (mục 4) |
| Tạo | `app/src/test/resources/robolectric.properties` | Đã có từ T-017 (hai dòng `sdk=35` và `application=android.app.Application`): không sửa, bỏ dòng này khỏi commit. Chạy trước T-017 thì tạo với đúng hai dòng đó (`application` để Robolectric không dựng `TemplateApplication` có Hilt, Firebase) |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModelTest.kt` | Unit test ViewModel |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Test giao diện Robolectric |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenScreenshotTest.kt` | Screenshot test Roborazzi |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PreviewAdGallerySlots.kt` | Quảng cáo giả cho test: `NativeAdAssets` cố định (chữ của thiết kế: "Headline of the ad", "Advertiser or short body", "Install", icon `null`), `PreviewAdGallerySlots` |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_default_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_default_dark.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_inlist_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế (câu hỏi 2) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế (câu hỏi 2) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_default_small.png` | Khung thấp, trang mặc định (nhiều khối nhất) (L-028) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_inlist_small.png` | Khung thấp, trang trong danh sách |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png` | Khung thấp, trang thu gọn dạng mở (quảng cáo ghim đáy chiếm chỗ nhiều nhất) |
| Ghi lại | `docs/remote-config-keys.md` | File do T-007 tạo (T-014 phụ thuộc T-007 qua T-008); task sinh lại, ghi đè bằng lệnh của T-007, không sửa tay: thêm 7 khóa `ads_gallery_*_enabled` |
| Ghi lại | `docs/remote-config-template.json` | Như trên: sinh lại, ghi đè, thêm 7 tham số |
| Sửa | `docs/ops/ads-guide.md` | Mục 5 (một câu về `stagingOnly`, sau phần PR #81 đã viết), mục 8 (giữ dòng giá trị cục bộ của T-008, thêm màn hình xem thử, Ad Inspector) |
| Sửa | `docs/features/ads/design/README.md` | Mục 8: bảng tỷ lệ khác biệt của 5 ảnh `AdGalleryScreen_*` (lấy từ bảng tóm tắt trong thư mục tạm) và nhận xét của rà soát độc lập |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_default_light_compare.png` | Ảnh so sánh |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_default_dark_compare.png` | Ảnh so sánh |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_inlist_light_compare.png` | Ảnh so sánh |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_collapsible-expanded_light_compare.png` | Ảnh so sánh |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_collapsible-collapsed_light_compare.png` | Ảnh so sánh |

Không sửa: `scripts/check.sh` (đã sửa ở PR #81), script so sánh thiết kế bọc sẵn, script phát hành (ADR-0017; đã sửa ở PR #81), `gradle/libs.versions.toml`, `build-logic/`, `.github/`, `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `AppNavHost.kt`, `app/src/androidTest/` (`AppLaunchTest` mở `MainActivity`, không đổi), `StagingRemoteValuesModule.kt` (tự gồm vị trí mới), `docs/ops/release.md` (PR #81 không đổi cách phát hành với người), `docs/STATUS.md` (phiên tự chạy sinh lại trong PR báo cáo).

Ảnh so sánh thiết kế: không chạy script so sánh bọc sẵn của repo cho `ads` (script đó chỉ đọc `feature/ads/src/test/screenshots`, không có thư mục này, và xóa cả thư mục `review/`). Chạy thẳng chương trình so sánh vào thư mục tạm, rồi chỉ chép 5 ảnh `AdGalleryScreen_*_compare.png` và dòng của chúng: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens app/src/test/screenshots <thư mục tạm>`.

- `app/build.gradle.kts` (L-020): `scripts/setup-check.sh` và `scripts/tests/run.sh` chỉ đọc dòng `ASSETS_BASE_URL` và `applicationId` của file này; plan không đổi hai dòng đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Màn hình chỉ có trong source set staging, không cần cờ.
- Quảng cáo: thêm 7 vị trí `gallery_*` của "các vị trí của màn hình xem thử" trong spec mục 5 (mọi định dạng, chỉ staging, bật sẵn ở staging, tần suất như production). `stagingOnly` theo tiền tố `gallery_`, production không có mã và không hiện. Thêm hàm mở Ad Inspector (công cụ của AdMob, không phải định dạng hay vị trí). Không đổi vị trí, định dạng, tần suất nào đã có.
- Sự kiện phân tích: Không chạm. Chỉ có log qua `AppLogger`.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Manifest của staging chỉ khai báo `AdGalleryActivity`, không có `uses-permission`; `MobileAds.openAdInspector` và quảng cáo thử dùng các quyền đã có của Google Mobile Ads ghi ở spec mục 6 (Quyền của app và dịch vụ bên ngoài). `Toast` không cần quyền.
- **Thư viện và SDK:** Không thêm thư viện mới. `:app` thêm phụ thuộc đã có trong `gradle/libs.versions.toml`: `libs.androidx.lifecycle.runtime.compose` (implementation, luôn thêm nếu chưa có, vì T-017 không thêm); và nếu T-017 chưa thêm: plugin `libs.plugins.roborazzi`, `libs.roborazzi`, `libs.roborazzi.compose`, `libs.roborazzi.junit.rule` (testImplementation). `:core:ads` dùng `libs.play.services.ads` đã có (`MobileAds.openAdInspector`). Test Compose và Robolectric của `:app` có sẵn qua plugin `template.android.compose`; không dùng Espresso hay thư viện test của Hilt. Spec mục 6 không có SDK mới cho task này.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trang trong danh sách cần hàm xen hai vị trí (`itemsWithInListAds(items, ads: List<InListAds>)`) dựa trên hàm nội bộ nhận `positions` và `adContent` của T-011; nếu T-011 đã merge mà hàm nội bộ đó không có hay khác chữ ký, phải viết lại phần của T-011. Phần source set staging, activity thứ hai, test `src/testStaging` và Roborazzi của `:app` đã chạy thử qua `scripts/check.sh` (mục 4.1), không cần dự phòng.
- **Điều kiện chuyển:** `InListAds.kt` sau T-011 không có hàm nội bộ nhận `positions` và `adContent` (kiểm khi bắt đầu task, trước commit test).
- **Cách làm thay:** viết hàm xen hai vị trí trong `InListAds.kt` chỉ dựa trên phần công khai của T-011: `inListAdPositions(itemCount, every)` cho chỗ chèn, rồi với mỗi chỗ chèn thứ k gọi phần dựng ô quảng cáo của `ads[k % ads.size]` như `itemsWithInListAds` có sẵn làm; khóa hàng quảng cáo vẫn `InListAdKey(placementKey, slot)`. Không đổi hàm có sẵn của T-011. Chữ ký công khai và test ở mục 4 (`InListAdsTest.alternatingPlacements_takeTurns`, `emptyAds_isRejected`) giữ nguyên; test gọi hàm công khai thay vì hàm nội bộ. Phần T-011 cần mà không công khai (phần dựng ô) vẫn không gọi được thì dừng (mục 7, điều kiện 2).

## 4. Kiểm thử

Test viết trước code, thất bại trước khi viết code (thứ tự commit ở mục 3.1). Test giao diện và screenshot ở `app/src/testStaging` chạy Robolectric với `robolectric.properties` dùng `android.app.Application`. Test có kiểm kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Khung chuẩn `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, khung thấp `w360dp-h640dp-xxhdpi`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-8, 1: mỗi định dạng có một vị trí của màn hình xem thử | Unit test | `AdPlacementTest.galleryPlacements_coverEveryFormatOnce`: vị trí có khóa bắt đầu `gallery_` đúng 7; tập định dạng gồm `NATIVE_SMALL`, `NATIVE_MEDIUM`, `NATIVE_COLLAPSIBLE`, `INTERSTITIAL`, `NATIVE_FULL_SCREEN`; mọi khóa `AdPlacement` không trùng |
| KB-8, 1 và 3: vị trí xem thử chỉ staging, khớp quy tắc của script phát hành | Unit test | `AdPlacementTest.stagingOnly_isExactlyGalleryPrefix`: với mọi `p` trong `AdPlacement.entries`, `p.stagingOnly == p.key.startsWith("gallery_")` (chuỗi viết thẳng trong test, cùng chuỗi `scripts/release.sh` bỏ qua); `AdPlacementTest.otherPlacements_areNotStagingOnly`: `SAMPLE_NATIVE_SMALL` (vị trí mẫu sau T-010, thay `SAMPLE_BOTTOM_BANNER`) và các vị trí khác không có tiền tố `gallery_` của T-010 đến T-013 không `stagingOnly` |
| KB-8, 1: mã thử của Google cho mọi vị trí xem thử | Unit test | `AdUnitIdsTest.googleTestIds_coverGalleryPlacements`: `GoogleTestAdUnitIds.of(p)` bắt đầu bằng `ca-app-pub-3940256099942544/` với mọi `p.stagingOnly` |
| KB-8, 1: bật sẵn ở staging không cần Firebase | Unit test có sẵn | `StagingRemoteValuesModuleTest.everyAdPlacement_isEnabled` (T-008) tự gồm 7 khóa mới; chạy lại, không sửa |
| KB-8, 3: production không đòi, không dùng mã cho vị trí xem thử | Unit test | `AdUnitIdsTest` sửa kỳ vọng: `ConfiguredAdUnitIds("").missing == AdPlacement.entries.filterNot { it.stagingOnly }`; thêm `stagingOnlyPlacement_hasNoUnit_evenWhenConfigured`: `ConfiguredAdUnitIds("gallery_native_small=x").of(GALLERY_NATIVE_SMALL) == null` |
| Bảng khóa có 7 khóa mới và đúng mô tả | Unit test có sẵn | `RemoteConfigKeysTest` (T-007) so hai file sinh lại; thêm không cần. Thử trước khi sinh lại: test thất bại đúng lý do "đã cũ so với code" |
| KB-8, 2: Ad Inspector, kết quả của SDK | Unit test | `AdInspectorTest.nullError_isClosed`, `AdInspectorTest.errorCode_isFailedWithCode` (`adInspectorResult(3) == Failed(3)`) |
| KB-8, 2: không có controller thì không gọi SDK | Compose UI test | `AdInspectorTest.withoutController_reportsNotReady`: không cấp `LocalAdsController`, gọi hành động của `rememberOpenAdInspector`, `onResult` nhận `NotReady` đúng một lần |
| Xen hai vị trí trong một danh sách | Compose UI test | `InListAdsTest.alternatingPlacements_takeTurns`: hàm nội bộ với `positions = inListAdPositions(7, 3)` và hai nội dung giả có tag `ad_a`, `ad_b`: thứ tự nút là mục 1..3, `ad_a`, mục 4..6, `ad_b`, mục 7; `emptyAds_isRejected` ném `IllegalArgumentException` |
| Chuyển trang và quay lại | Unit test | `AdGalleryViewModelTest.startsOnDefault`, `openInList_showsInListWith30Items`, `openCollapsible_showsCollapsibleWith30Items`, `backFromSubPage_returnsToDefault_andReturnsTrue`, `backOnDefault_returnsFalse` (dùng `MainDispatcherRule` nếu `:app` có, không thì `Dispatchers.setMain` trong test, Turbine) |
| Log đúng, không có dữ liệu riêng (ADR-0019) | Unit test | `AdGalleryViewModelTest.logsPageChanges`, `logsFullScreenRequests`, `logsInspectorResults` với `RecordingAppLogger`: thẻ `AdGallery`, thông điệp đúng mục 3.1 |
| KB-8, 2: trang mặc định có small, medium, ba nút | Compose UI test | `AdGalleryScreenTest.default_showsSmallMediumAndButtons`: `PreviewAdGallerySlots`, có chữ "Native small", "Native medium", hai ô quảng cáo giả, nút "Interstitial", "Native full screen", "Open Ad Inspector" |
| KB-8, 2: bấm nút gọi đúng hành động | Compose UI test | `AdGalleryScreenTest.buttons_callTheirActions`: bấm từng nút trong năm nút, đếm lần gọi của từng callback bằng 1 |
| KB-8, 2: trang trong danh sách | Compose UI test | `AdGalleryScreenTest.inList_insertsAdsAfterEveryNItems`: `InList(7)`, N = 3: tiêu đề "In-list ads (every 3 items)", quảng cáo giả small sau "List item 3", medium sau "List item 6", không ở đầu; `inList_withoutN_showsOffTitle`: N null thì "In-list ads (off)" và không có ô quảng cáo |
| KB-8, 2 và KB-4: trang thu gọn, quảng cáo ghim đáy | Compose UI test | `AdGalleryScreenTest.collapsible_pinsAdAtBottom` (`@GraphicsMode(NATIVE)`, khung 411 x 891): đáy ô quảng cáo cách đáy màn hình 16 dp (sai số 1 dp), hai bên 16 dp |
| Thiết kế: khoảng cách giữa các khối và nút | Compose UI test | `AdGalleryScreenTest.default_layoutMatchesDesign` (`@GraphicsMode(NATIVE)`): tiêu đề ở x = 16, y = 20; nhãn "Native small" ở y = 72; hai nút toàn màn hình cao 52, cách nhau 12, cách ô medium 16; nút Ad Inspector rộng 379 (sai số 1 dp) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | `AdGalleryScreenTest.texts_matchDesignTable`: "Ad gallery (staging)", "Native small", "Native medium", "Interstitial", "Native full screen", "Open Ad Inspector", "In-list ads (every 3 items)", "List item 1", "Collapsible ad" đọc từ `strings.xml` staging khớp đúng từng chữ |
| Vùng chạm đủ lớn | Compose UI test | `AdGalleryScreenTest.buttons_haveTouchTargetOf48dp`: mọi nút cao ít nhất 48 dp |
| Đúng thiết kế, sáng, trang mặc định | Screenshot test | `AdGalleryScreenScreenshotTest.default_light` ghi `src/test/screenshots/AdGalleryScreen_default_light.png` |
| Đúng thiết kế, tối | Screenshot test | `AdGalleryScreenScreenshotTest.default_dark` (`AppTheme(darkTheme = true)`) ghi `src/test/screenshots/AdGalleryScreen_default_dark.png` |
| Đúng thiết kế, trong danh sách | Screenshot test | `AdGalleryScreenScreenshotTest.inlist_light`: `InList(7)`, N = 3, ghi `src/test/screenshots/AdGalleryScreen_inlist_light.png` |
| Đúng thiết kế, thu gọn đang mở | Screenshot test | `AdGalleryScreenScreenshotTest.collapsibleExpanded_light`: `Collapsible(6)`, `collapsed = false`, ghi `src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` |
| Đúng thiết kế, đã thu gọn | Screenshot test | `AdGalleryScreenScreenshotTest.collapsibleCollapsed_light`: `Collapsible(6)`, `collapsed = true`, ghi `src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` |
| Màn hình thấp (L-028), trang mặc định | Screenshot test và Compose UI test khung `w360dp-h640dp-xxhdpi` | `AdGalleryScreenScreenshotTest.default_small` chỉ chụp, ghi `src/test/screenshots/AdGalleryScreen_default_small.png`. Phần đo ở lớp test giao diện (lớp screenshot chụp bằng `captureRoboImage` kèm khối nội dung không được có `createComposeRule()`, CLAUDE.md bước 10): `AdGalleryScreenTest.default_smallScreen_scrollsToEveryButton`, gắn `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` trên hàm test, cuộn tới "Open Ad Inspector" và hàng "In-list ads" (`performScrollTo`), khẳng định nằm trọn trong 0..640 dp |
| Màn hình thấp, trong danh sách | Screenshot test và Compose UI test | `AdGalleryScreenScreenshotTest.inlist_small` chỉ chụp, ghi `src/test/screenshots/AdGalleryScreen_inlist_small.png`. `AdGalleryScreenTest.inList_smallScreen_scrollsToLastItem` (`@Config(qualifiers = "w360dp-h640dp-xxhdpi")` trên hàm test): cuộn tới "List item 7" được |
| Màn hình thấp, thu gọn đang mở | Screenshot test và Compose UI test | `AdGalleryScreenScreenshotTest.collapsibleExpanded_small` chỉ chụp, ghi `src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png`. `AdGalleryScreenTest.collapsibleExpanded_smallScreen_fitsAndScrolls` (`@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` trên hàm test): nút thu gọn và CTA nằm trọn trong 0..640 dp, danh sách cuộn được tới "List item 6" |
| Đối chiếu thiết kế (ADR-0020) | Ảnh so sánh và rà soát độc lập | Chạy chương trình so sánh (mục 3.2), chép 5 ảnh `_compare.png`, ghi tỷ lệ khác biệt và nhận xét vào README mục 8. Khác biệt dự kiến: hàng nút "In-list ads", "Collapsible ad" ở trang mặc định (câu hỏi 7), icon quảng cáo giả không có ảnh |
| Ảnh chuẩn khớp | CI | `scripts/check.sh` (sau PR #81) chạy `:app:verifyRoborazziStagingDebug` với 8 ảnh chuẩn ở mục 3.2 (câu hỏi 8) |
| KB-8, 3: production không có màn hình (mã nguồn) | Unit test | `AdGallerySourceSetTest.onlyStagingSourceSets_mentionAdGallery`: duyệt mọi `.kt`, `.xml` dưới `src/` của `:app`, bỏ `src/staging`, `src/testStaging`, `src/test`; không file nào chứa `AdGallery` hay `ad_gallery`. `stagingDeclaresGallery`: `src/staging/AndroidManifest.xml` chứa `AdGalleryActivity` và có `src/staging/.../AdGalleryActivity.kt` (để test không qua vì đường dẫn sai). `mainManifest_hasOneLauncher`: `src/main/AndroidManifest.xml` có đúng một `android.intent.category.LAUNCHER` |
| Staging build và lint qua với activity mới | Build | `:app:assembleStagingDebug`, `:app:lintStagingDebug` trong `scripts/check.sh` |
| KB-8, 1 và 2 trên máy thật | Kiểm tra thủ công | Cài bản staging (file Firebase giả), thấy biểu tượng "Ad gallery"; mở, đồng ý ở hộp thoại; trang mặc định có small và medium quảng cáo thử (nhãn "Test Ad"); bấm "Interstitial" sau khi hết 180 giây ân hạn phiên đầu thì hiện, bấm lại ngay thì không (log `skipped`); "Native full screen" tương tự; "Open Ad Inspector" trên emulator mở được và thấy adapter AppLovin (KB-9, sau T-015); trên máy chưa đăng ký thì Toast báo lỗi; trang trong danh sách có quảng cáo sau mỗi 8 mục, xen small và medium; trang thu gọn: chạm nút thì gọn, không tự mở lại. Lưu ý khi thử tần suất: `AdGalleryActivity` không gọi `onAppForeground`/`onAppBackground`, nên mở và đóng màn hình xem thử không bắt đầu phiên mới; giới hạn mỗi phiên chỉ đếm lại khi `MainActivity` vào nền rồi quay lại. `adb logcat -v threadtime \| grep ' VGL\.'` có các dòng `AdGallery` và `Ads` ở mục 3.1 |

**Ngoại lệ viết test trước:**

- `AdGallerySourceSetTest` qua ngay trên code hiện tại vì chưa có file nào nhắc tới `AdGallery`, trừ `stagingDeclaresGallery` (thất bại vì chưa có activity). Để chứng minh test bắt được lỗi, AI tạm đặt một file nhắc tới `AdGallery` trong `app/src/main` (không commit), thấy test thất bại, rồi xóa; ghi vào nhật ký.
- Phần gọi SDK trong `AdsController.openAdInspector`, `SdkAdGallerySlots`, `AdGalleryActivity`, manifest staging, `strings.xml` và tài liệu: không chạy được trên JVM hay là tài nguyên, tài liệu; kiểm bằng build, lint, test chữ và kiểm tra thủ công.
- Ảnh chuẩn screenshot ghi sau khi code xong, không có trước.

Người duyệt cần đồng ý các ngoại lệ trên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, worktree tạm ngoài repo (`git worktree add --detach <scratchpad>/trial-t013-t014 origin/main`, commit `39bbc70`), code thử không commit, đã xóa worktree sau khi chạy. Cùng một lần chạy với phần thử của plan T-014. Code thử tối thiểu cho phần dễ hỏng nhất:
  - `app/build.gradle.kts`: plugin `libs.plugins.roborazzi`, khối `roborazzi { outputDir = src/test/screenshots }`, `libs.androidx.lifecycle.runtime.compose`, ba dòng `testImplementation` Roborazzi (đúng dòng của mục 3.2).
  - `app/src/staging/AndroidManifest.xml` khai báo `.adgallery.AdGalleryActivity` (`exported`, `MAIN`, `LAUNCHER`, nhãn `@string/ad_gallery_launcher_label`, theme `Theme.App`); `app/src/staging/res/values/strings.xml`; `app/src/staging/kotlin/.../adgallery/AdGalleryActivity.kt` (`@AndroidEntryPoint`, `@Inject AdsController`, `gatherConsent`, `LocalAdsController`, `AppTheme`, `safeDrawingPadding`) và một composable đọc chuỗi của staging.
  - `app/src/test/resources/robolectric.properties` (`sdk=35`, `application=android.app.Application`); test Robolectric ở `app/src/testStaging` dựng composable của staging, thấy chữ "Ad gallery (staging)" và chụp một ảnh Roborazzi tạm vào thư mục ảnh chuẩn của `:app` (đường dẫn ghi thẳng như quy tắc của `check.sh`); test quét mã nguồn ở `app/src/test` (đường dẫn tương đối `src/`, chạy từ thư mục `app/`).
- **Lệnh:** `scripts/check.sh` (JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`), hai lần.
- **Kết quả:** lần đầu thất bại chỉ vì ktlint của code thử (`import-ordering`, `argument-list-wrapping`), không vì kỹ thuật; `check.sh` tự ghi ảnh mới của `:app` (`recordRoborazziStagingDebug`). Sau `ktlintFormat`, `scripts/check.sh` qua: `:app` 3 test 0 lỗi (test ở `src/testStaging` chạy trong `testStagingDebugUnitTest`), `:app:verifyRoborazziStagingDebug` qua, `:app:lintStagingDebug` không báo lỗi về activity `LAUNCHER` thứ hai (điều kiện dừng 5 không xảy ra), `:app:assembleStagingDebug` qua, "Cổng chất lượng: QUA". Manifest gộp của staging có 2 `LAUNCHER`, danh sách `uses-permission` không đổi so với trước (manifest staging không thêm quyền). Robolectric dựng được composable của `:app` với `android.app.Application`, không cần Hilt trong test. Không chạy thử: `MobileAds.openAdInspector` và quảng cáo thật (kiểm thủ công), hàm xen hai vị trí (phụ thuộc T-011, mục 3.4).
- **Thay đổi sau chạy thử (rà soát trước khi chạy):** chuyển phần đo khung thấp sang `AdGalleryScreenTest` với `@Config` trên từng hàm, làm rõ việc thêm `lifecycle-runtime-compose`, ghi chú về phiên của `AdGalleryActivity`. Lần chạy thử đã dùng đúng `robolectric.properties` hai dòng và đúng dòng `lifecycle-runtime-compose`. Việc tách phần đo khỏi lớp screenshot chỉ đổi lớp chứa test, không đổi kỹ thuật đã thử (Robolectric ở `src/testStaging`, Roborazzi ở `:app`), nên kết quả trên không đổi; chưa chạy lại. Ghi chú về phiên chỉ là tài liệu.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 1 (thêm 7 vị trí quảng cáo, dù chỉ staging), điểm 7 (module core dùng chung `:core:ads`: `AdPlacement`, `AdUnitIds`, `AdsController`, `InListAds`; cấu hình build `app/build.gradle.kts`). Không chạm quyền, SDK mới, sự kiện phân tích, hộp thoại đồng ý (activity mới gọi lại đúng hàm đồng ý có sẵn, không đổi thứ tự khởi tạo SDK), R2. Mức Cao, bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước vì có vị trí quảng cáo mới (chỉ staging; script phát hành bỏ qua sau PR #81).

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app`), đổi giao diện công khai của `AdPlacement` (thuộc tính mới), thêm API công khai (`rememberOpenAdInspector`, hàm xen vị trí), source set staging mới có manifest |
| B. Độ rõ của yêu cầu | 2 | Spec xác nhận, thiết kế đã duyệt; kiểm phát hành, ảnh `_collapsible-*`, so ảnh trong CI đã có quyết định (2026-10-23); còn câu hỏi ảnh hưởng cách làm: lối vào, cách chuyển trang không có trong thiết kế, hàm xen hai vị trí |
| C. Khả năng kiểm chứng tự động | 2 | Vị trí, ViewModel, giao diện, ranh giới source set có test JVM; ảnh chuẩn của `:app` được CI so (PR #81); quảng cáo thật, Ad Inspector, tần suất chỉ kiểm thủ công |
| D. Ảnh hưởng chéo | 3 | `AdPlacement.kt`, `AdUnitIds.kt`, `AdsController.kt`, `InListAds.kt`, `app/build.gradle.kts` cũng do T-010 đến T-013, T-015, T-017 sửa; thêm vị trí ảnh hưởng kiểm tra phát hành và bảng khóa mà mọi app dùng |
| E. Độ mới của kỹ thuật | 2 | Source set staging có manifest và activity riêng, Ad Inspector, screenshot ở `:app` chưa có trong dự án (T-017 cũng thêm); phần còn lại theo mẫu `:feature:sample`. Phần source set, activity, test `src/testStaging`, Roborazzi ở `:app` đã chạy thử (mục 4.1) |
| **Tổng** | 12 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `AdPlacement.kt`: thuộc tính suy ra, constructor không đổi, mọi dòng khai báo hiện có không đổi. Mọi `when` trên `AdPlacement` (nếu có) phải xét thêm 7 hằng; hiện không có (`grep`).
  - `AdUnitIds.kt`: hành vi của `ConfiguredAdUnitIds` đổi chỉ với vị trí `stagingOnly`.
  - `AdsController.kt`: thêm một hàm nội bộ, không đổi hàm có sẵn.
  - `InListAds.kt`: thêm một hàm công khai, không đổi hàm có sẵn.
  - `app/build.gradle.kts`: chỉ thêm plugin, khối `roborazzi`, phụ thuộc.
  - Bảng khóa và file mẫu Remote Config: thêm 7 dòng; ai đã nhập file mẫu vào Firebase thì 7 khóa mới mặc định `false`, không ảnh hưởng.
- Task khác trong sprint có thể xung đột:
  - T-010, T-011, T-012, T-013 (bắt buộc trước, `depends_on`): T-014 dùng API của cả bốn và sửa `InListAds.kt` của T-011, `AdPlacement.kt` và `AdUnitIds.kt` mà cả bốn sửa.
  - T-012: theo câu hỏi 2, T-012 chỉ ghi ảnh `CollapsibleNativeAd_*` ở `core/ads`; hai ảnh `AdGalleryScreen_collapsible-*` và ảnh so sánh của chúng thuộc T-014, không trùng tên.
  - PR #81 (bắt buộc merge trước, ngoài `depends_on` vì không phải ticket): `scripts/release.sh` bỏ qua `gallery_*`, `scripts/check.sh` so ảnh của `:app`.
  - T-007, T-008: bảng khóa, giá trị cục bộ staging (đã merge trước T-010).
  - T-015: Ad Inspector là nơi kiểm adapter AppLovin; không cùng file trừ có thể `core/ads/build.gradle.kts` (T-014 không sửa file này).
  - T-017: cùng sửa `app/build.gradle.kts` (cùng plugin, khối `roborazzi`, cùng ba dòng `testImplementation`; `lifecycle-runtime-compose` chỉ T-014 thêm) và cùng tạo `app/src/test/resources/robolectric.properties` (cùng nội dung `sdk=35`, `application=android.app.Application`); ai chạy sau thì dùng phần đã có, không thêm lần hai. T-017 đặt test ở `app/src/test`, T-014 ở `app/src/testStaging`: `robolectric.properties` trong `src/test/resources` áp cho cả hai.
  - T-016: không chung file (T-016 sinh lại hai file bảng khóa: xung đột thì lấy bản của nhánh chính rồi chạy lại lệnh sinh, không gộp tay).
- Đề xuất thứ tự chạy: cuối đợt quảng cáo, sau T-010, T-011, T-012, T-013 (và sau T-015 nếu cùng phiên, để kiểm KB-9 ngay). Không chạy cùng lúc với T-017.

## 7. Điều kiện dừng riêng

1. PR #81 chưa merge vào nhánh chính khi task bắt đầu (`scripts/release.sh` chưa bỏ qua `gallery_`, hoặc `scripts/check.sh` chưa duyệt `app/`): dừng trước khi làm (thêm vị trí làm hỏng lần phát hành sau; ảnh của `:app` không được so).
2. Sau T-010 đến T-013, tên trong hợp đồng chung khác (`NativeAdCard`, `NativeAdAssets`, `CollapsibleNativeAdCard`, `CollapsibleNativeAdSlot`, `rememberInListAds`, `itemsWithInListAds` và bản nội bộ nhận `positions`, `rememberShowFullScreenAd`, `FullScreenAdHost`): dừng, ghi API thật, sửa plan.
3. T-012 đã merge mà vẫn có ảnh chuẩn tên `AdGalleryScreen_collapsible-*` trong `core/ads/src/test/screenshots/` hay ảnh so sánh của chúng trong `design/review/` (trái câu hỏi 2): dừng (không tự đổi tên hay xóa ảnh của task khác).
4. Hilt hay Robolectric không dựng được `AdGalleryScreen` trong `app/src/testStaging` sau 3 lần sửa (ví dụ cần thư viện test của Hilt): dừng, không thêm thư viện.
7. `scripts/check.sh` báo lỗi ở phần ảnh chuẩn của `:app` mà nguyên nhân là script (không phải ảnh hay test của T-014): dừng, không sửa script.
5. Lint của staging báo lỗi về activity thứ hai có `LAUNCHER` mà chỉ tắt được bằng cách bỏ qua quy tắc: dừng.
6. Lần thử tạm đặt `AdGallery` trong `app/src/main` mà `AdGallerySourceSetTest` vẫn qua: dừng.
8. `docs/ops/ads-guide.md` mục 8 vẫn còn dòng "quảng cáo không hiện trên staging" (T-008 chưa merge), hoặc `AdPlacement` vẫn có `SAMPLE_BOTTOM_BANNER` mà không có `SAMPLE_NATIVE_SMALL` (T-010 chưa merge): dừng.

## 8. Câu hỏi cho người duyệt

1. **Kiểm tra trước khi phát hành với vị trí chỉ staging.** Script phát hành đòi mã production cho mọi dòng trong `AdPlacement` (ADR-0017, ads-guide mục 9); AI không được đọc hay sửa script này. Thêm 7 vị trí `gallery_*` thì lần phát hành sau sẽ bị từ chối, hoặc phải nhập 7 mã production vô dụng. AI đề xuất: người sửa script bỏ qua dòng có `stagingOnly = true` (và cập nhật `docs/ops/release.md`) trước khi T-014 merge, bằng một thay đổi riêng của dự án mẫu. Phương án khác: (a) không thêm vị trí vào `AdPlacement`, đổi các composable native nhận một kiểu vị trí chung (giao diện), là thay đổi lớn ở T-010 đến T-013; (b) màn hình xem thử dùng lại vị trí `sample_native_small` và không có vị trí riêng, nhưng khi đó không thử được medium, in-list, thu gọn, toàn màn hình (trái spec mục 5).

   Trả lời (anh Nhật, 2026-10-23): `scripts/release.sh` đã sửa ở PR #81: đọc định dạng có gạch dưới và bỏ qua vị trí có khóa bắt đầu bằng `gallery_`. T-014 cần PR #81 merge trước. Plan đổi theo: `stagingOnly` suy từ tiền tố `gallery_` thay vì tham số mới, để dòng khai báo giữ đúng mẫu script đọc; `docs/ops/release.md` không đổi.
2. **Ai ghi ảnh `AdGalleryScreen_collapsible-*`.** Plan T-012 (câu hỏi 1) đề xuất T-012 ghi hai ảnh này ở `:core:ads` bằng khung giả. AI đề xuất: T-014 ghi, vì đó là ảnh của màn hình thật; T-012 chỉ ghi ảnh component (`CollapsibleNativeAd_*`). Nên trả lời chung cho hai plan ở họp đánh giá. Nếu giữ phương án của T-012, bỏ hai ảnh `_collapsible-*_light` và ảnh `_collapsible-expanded_small` khỏi mục 3.2 của plan này.

   Trả lời (anh Nhật, 2026-10-23): T-014 ghi `AdGalleryScreen_collapsible-*` ở màn hình thật (`:app`, staging), cùng `AdGalleryScreen_inlist_light` và ảnh so sánh của chúng. T-012 chỉ ghi ảnh component tên riêng (`CollapsibleNativeAd_*`). Mục 3.2 giữ nguyên.
3. **Bảy khóa `ads_gallery_*_enabled` có vào bảng khóa và file mẫu không.** AI đề xuất có, kèm mô tả "Staging ad gallery only.": ít trường hợp riêng, và project Firebase staging tắt được từng vị trí xem thử. Phương án khác: `AdsSettings.all()` bỏ vị trí `stagingOnly`, bảng khóa không đổi; nhưng khi đó có khóa được đọc mà không nằm trong bảng, trái ADR-0023 mục 4.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
4. **Ad Inspector trên máy thật.** Google chỉ cho mở Ad Inspector trên emulator hoặc máy đã đăng ký thiết bị thử (AdMob, mục thiết bị thử). AI đề xuất: không thêm mã thiết bị vào repo; hướng dẫn trong ads-guide mục 8 cách đăng ký trên AdMob; màn hình báo lỗi bằng Toast. Phương án khác: khai báo danh sách mã thiết bị thử trong `StagingAdsModule` (mã thiết bị là dữ liệu của máy người trong nhóm, không nên vào repo).
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **Hàm xen hai vị trí trong một danh sách (`InListAds.kt`).** Cần để trang trong danh sách giống thiết kế (small rồi medium). AI đề xuất thêm, vì app cũng có thể muốn xen hai cỡ. Phương án khác: không đổi API của T-011, trang chỉ dùng `GALLERY_INLIST_SMALL`, bỏ vị trí `GALLERY_INLIST_MEDIUM`, ảnh `_inlist_light` khác thiết kế ở quảng cáo thứ hai.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
6. **Lối vào bằng biểu tượng thứ hai trên máy.** AI đề xuất `AdGalleryActivity` có biểu tượng "Ad gallery" riêng trong bản staging: production không có dòng nào, không sửa `MainActivity`, `AppNavHost`. Đánh đổi: bản staging có hai biểu tượng; popup mất mạng (T-017) không có trong màn hình xem thử. Phương án khác: một đích trong `AppNavHost` qua hàm theo biến thể (`app/src/staging`, `app/src/production`) và một nút nhỏ chỉ staging đè lên giao diện, phải sửa `MainActivity.kt`, `AppNavHost.kt` (xung đột với T-013, T-017).
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
7. **Chuyển sang trang trong danh sách và trang thu gọn.** Thiết kế có 3 khung nhưng không có nút chuyển trang. AI đề xuất thêm hàng hai `OutlinedButton` "In-list ads", "Collapsible ad" dưới nút Ad Inspector, nút quay lại của máy về trang mặc định; ảnh `default_*` sẽ khác thiết kế ở vùng này. Nếu muốn khác (ví dụ tab ở đầu màn hình), cần bổ sung thiết kế trước khi duyệt.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
8. **CI không so ảnh chuẩn của `:app`.** `scripts/check.sh` chỉ duyệt `core/*/`, `feature/*/`. Plan T-017 có cùng câu hỏi. AI đề xuất: phiên tự chạy chạy `:app:verifyRoborazziStagingDebug` cục bộ và ghi kết quả; mở rộng `check.sh` và script so sánh thiết kế bọc sẵn cho `:app` và thư mục ảnh của module core là một plan rủi ro cao riêng, trả lời chung cho T-012, T-014, T-017.

   Trả lời (anh Nhật, 2026-10-23): `scripts/check.sh` đã sửa ở PR #81: so ảnh chuẩn của `:app` (`verifyRoborazziStagingDebug`, đọc cả `src/testStaging`). Bỏ phần so ảnh trên máy thay cho CI. Script so sánh thiết kế bọc sẵn vẫn chưa nhận `:app`: task chạy thẳng chương trình so sánh như mục 3.2.
9. **`depends_on` của ticket.** Ticket đã ghi T-007 đến T-013 (rà soát 2026-10-23). Hai file bảng khóa ở mục 3.2 vẫn ghi Tạo vì hiện chưa có trên nhánh chính (`plan-check.py` so với repo); khi T-007 đã merge, phiên tự chạy coi là Sửa (sinh lại, không sửa tay). PR #81 không phải ticket nên ghi ở mục 6 và điều kiện dừng 1, không ở `depends_on`.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện một dev đảm nhận cả hai vai)
- **Ghi chú:** anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), sau rà soát độc lập của AI; AI ghi mục này theo lời anh. Câu hỏi ở mục 8 đã có trả lời. Cần PR #81 (`release.sh`, `check.sh`) merge trước phiên chạy.

Phiên bản 3:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua hai lần rà soát độc lập trước khi chạy, ghi ở RUN-20261023-01)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-23; câu hỏi ở mục 8 đã trả lời ngay tại đó.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
