---
ticket: T-012
title: Native thu gọn được
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-21)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-012: NATIVE THU GỌN ĐƯỢC

## 1. Tóm tắt

Thêm định dạng `AdFormat.NATIVE_COLLAPSIBLE` và composable công khai `CollapsibleNativeAdSlot(placement, modifier)` vào `:core:ads`: quảng cáo ở dạng mở (gần như medium, có nút thu gọn 48 dp ở góc trên trái), chạm nút thì chuyển sang dạng gọn (đúng native small của T-010), không có cách nào mở lại. Trạng thái thu gọn nằm trong một bộ giữ trạng thái chỉ có hàm `collapse()`, nhớ bằng `rememberSaveable` theo vị trí, không theo quảng cáo, nên quảng cáo mới tải về không làm nó mở lại. Kiểm chứng bằng test giao diện Robolectric (chạm thu gọn, đổi quảng cáo, khôi phục trạng thái, kích thước nút và vị trí góc) và screenshot của component ở hai trạng thái, đặt trong khung giống thiết kế, cùng tên ảnh thiết kế.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi vào màn hình thì quảng cáo ở dạng mở (như medium) ở đáy màn hình, có nút thu gọn ít nhất 48 dp, không cùng góc với AdChoices. | Dạng mở theo frame `Native/Collapsible` của `AdGalleryScreen_collapsible-expanded_light`: thanh thu gọn cao 48 dp (nút 48 x 48 dp ở góc trên trái, icon mũi tên xuống 24 dp), hàng đầu (icon 40 dp, tiêu đề, nhãn "Ad", nhà quảng cáo), media rộng hết khung cao 160 dp, CTA rộng hết khung cao 36 dp. AdChoices do SDK vẽ ở góc trên phải (mặc định của SDK, giữ nguyên). Đặt ở đáy là việc của màn hình gọi (feature, hay màn hình xem thử của T-014): composable chỉ chiếm chiều rộng được cho, không tự ghim. |
| KB-4 | Khi người dùng chạm nút thu gọn thì quảng cáo chuyển sang dạng gọn (như small). | Dạng gọn trùng số đo với `Native/Small` (379 x 72 dp, lề trái 12, lề phải 24, cách nhau 12), nên dùng lại nguyên `NativeAdCard(assets, NativeAdSize.SMALL)` của T-010. Dạng gọn không có nút mở lại. Chuyển ngay, không hiệu ứng (câu hỏi 4). |
| KB-4 | Khi đã thu gọn thì quảng cáo không tự mở lại trong lúc người dùng còn ở màn hình đó, kể cả khi tải quảng cáo mới. | `CollapsibleAdState` chỉ có `collapsed` và `collapse()`, không có hàm mở. Nó được tạo bằng `rememberSaveable` ở đầu `CollapsibleNativeAdSlot`, ngoài mọi `key` theo quảng cáo, nên quảng cáo mới, trạng thái đang tải lại, đổi cấu hình (xoay máy) hay quay lại màn hình từ ngăn xếp điều hướng đều giữ dạng gọn. Mở một lần mới của màn hình (mục mới trên ngăn xếp) thì bắt đầu ở dạng mở. Native không tự làm mới (ads-guide.md, mục 6); "quảng cáo mới" ở đây là khi luồng tải của T-010 phát lại (ví dụ công tắc tổng tắt rồi bật). |

Các tiêu chí chung của KB-1 và KB-2 áp dụng cho native thu gọn qua phần dựng của T-010: nhãn "Ad", AdChoices không bị đè, nền và viền tách khỏi nội dung, CTA khác nút của app, media ít nhất 120 x 120 dp (dạng mở 353 x 160 dp), giữ chỗ khi tải, thu chỗ khi không có quảng cáo sau `ads_native_load_timeout_seconds`, chế độ tối, hủy khi rời màn hình, không chiếm chỗ khi chưa đồng ý hay vị trí tắt.

Thiết kế tham chiếu: `features/ads/design/README.md` (đã duyệt 2026-10-21), ảnh `design/screens/AdGalleryScreen_collapsible-expanded_light.png` và `AdGalleryScreen_collapsible-collapsed_light.png`, số liệu trong `design/context/` cùng tên. README mục 1 gán hai ảnh này cho T-012.

ADR liên quan: ADR-0013 (mã thử ở staging, quảng cáo chỉ sau đồng ý), ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (tên screenshot trùng tên ảnh thiết kế, ảnh so sánh), ADR-0022 (native thu gọn không tự bung lại; native chỉ qua composable của `:core:ads`), ADR-0023 (không thêm khóa). ADR-0024 không đổi gì cho task này: popup mất mạng của `:app` (T-017) phủ lên màn hình, không liên quan tới trạng thái thu gọn.

Giả định về các plan chạy trước (hợp đồng chung, giữ đúng tên):

- T-010 đã merge: `NativeAdAssets`, `NativeAdSize` (`SMALL`, `MEDIUM`), `NativeAdCard(assets, size, modifier)`, `NativeAdPlaceholder(size)`, `NativeAdSlot(placement, modifier)`, luồng tải native của `AdsController` trả `NativeAdState` (`Loading`, `Loaded`, `Unavailable`), mã thử native trong `GoogleTestAdUnitIds`, phần dựng quảng cáo thật trong `NativeAdView` qua `AndroidView`.
- T-009 đã merge: `AdsController` có `AppLogger`, `isEnabled(placement)` gồm công tắc tổng.
- T-007: khóa bật tắt của vị trí sinh từ `AdPlacement.entries`. T-012 không thêm vị trí nên không đổi khóa nào.

## 3. Giải pháp

### 3.1. Cách làm

**Định dạng.** `AdFormat.NATIVE_COLLAPSIBLE` (KDoc: "Medium that the user collapses to small, shown with [CollapsibleNativeAdSlot]; never expands again"). `GoogleTestAdUnitIds.of` thêm nhánh `NATIVE_COLLAPSIBLE` trả mã thử native của Google mà T-010 đã thêm (cùng hằng số, không thêm mã mới). Không thêm `AdPlacement`: vị trí dùng định dạng này do T-014 (màn hình xem thử) hay spec của từng app khai báo.

**Bộ giữ trạng thái** (`CollapsibleNativeAd.kt`):

```kotlin
/** Whether a collapsible ad is collapsed. It can only collapse: there is no way to expand it again (ADR-0022). */
@Stable
class CollapsibleAdState internal constructor(collapsed: Boolean) {
    var collapsed by mutableStateOf(collapsed)
        private set

    fun collapse() { collapsed = true }

    internal companion object {
        val Saver: Saver<CollapsibleAdState, Boolean> = Saver(save = { it.collapsed }, restore = ::CollapsibleAdState)
    }
}

@Composable
fun rememberCollapsibleAdState(): CollapsibleAdState =
    rememberSaveable(saver = CollapsibleAdState.Saver) { CollapsibleAdState(collapsed = false) }
```

**Composable không trạng thái, công khai** (cho test, screenshot và màn hình xem thử của T-014, giống `NativeAdCard`):

```kotlin
@Composable
fun CollapsibleNativeAdCard(assets: NativeAdAssets, collapsed: Boolean, onCollapse: () -> Unit, modifier: Modifier = Modifier)
```

- `collapsed = true`: gọi `NativeAdCard(assets, NativeAdSize.SMALL, modifier)` của T-010, không thêm gì.
- `collapsed = false`: bố cục dạng mở dựng từ các phần con của T-010 (khung nền `surfaceContainerLow`, viền 1 dp `outlineVariant`, bo `MaterialTheme.shapes.medium`; icon; tiêu đề và dòng nhãn "Ad"; media; CTA). Số đo theo `context/AdGalleryScreen_collapsible-expanded_light.md`: lề trên 8 dp (`Spacing.sm`), lề trái, phải, dưới 12 dp, các khối cách nhau 12 dp; thanh thu gọn cao 48 dp chỉ có nút ở đầu dòng; media cao 160 dp rộng hết khung, giữ tỷ lệ ảnh (không kéo giãn, không cắt); CTA rộng hết khung, cao 36 dp. Giá trị 2, 6, 12 dp ngoài hệ thống giữ làm hằng số riêng như T-010 đã quyết (README thiết kế, mục 3); thêm `ExpandedMediaHeight = 160.dp` và `CollapseButtonSize = 48.dp` cùng chỗ.
- Nút thu gọn: `IconButton` (vùng chạm 48 x 48 dp), icon `R.drawable.ads_ic_collapse` (vector mũi tên xuống 24 dp, nét 2 dp, màu `onSurface` qua `tint`), `contentDescription = stringResource(R.string.ads_collapse)`, `testTag("collapsible_ad_collapse")`. Không thêm thư viện icon: BOM 2025.09 không kèm `material-icons-core`.
- Thẻ test: `collapsible_ad` trên khung ngoài, `collapsible_ad_media` trên vùng media. Dạng gọn dùng thẻ của `NativeAdCard` (T-010), bọc thêm `collapsible_ad` để test đo chiều cao cả khung.
- Vùng trên phải của thanh thu gọn để trống cho AdChoices; không vẽ biểu tượng AdChoices trong Compose (ảnh thiết kế có ô "i" minh họa vị trí, README mục 3 ghi "do SDK vẽ, không vẽ lại").

**Phần có trạng thái, nội bộ, test được không cần SDK:**

```kotlin
internal sealed interface CollapsibleAdContent {
    data object Loading : CollapsibleAdContent
    data class Ready(val assets: NativeAdAssets) : CollapsibleAdContent
    data object Hidden : CollapsibleAdContent
}

@Composable
internal fun CollapsibleNativeAdContent(
    content: CollapsibleAdContent,
    state: CollapsibleAdState,
    onCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
    renderReady: @Composable (assets: NativeAdAssets, collapsed: Boolean, onCollapse: () -> Unit) -> Unit =
        { assets, collapsed, onCollapse -> CollapsibleNativeAdCard(assets, collapsed, onCollapse) },
)
```

- `Loading`: `NativeAdPlaceholder` cao bằng dạng hiện tại: 342 dp khi đang mở, 72 dp (`NativeAdSize.SMALL`) khi đã thu gọn (câu hỏi 2). Cần một placeholder cao 342 dp: thêm hằng số chiều cao dạng mở và dựng placeholder bằng hàm của T-010 nhận chiều cao, hoặc bọc `NativeAdPlaceholder` trong `Modifier.height` (chọn cách T-010 cho phép, không sửa file của T-010).
- `Ready`: gọi `renderReady(assets, state.collapsed) { state.collapse(); onCollapsed() }`. `onCollapsed` chỉ được gọi ở lần chạm đầu (nút không còn sau khi thu gọn).
- `Hidden`: không vẽ gì, không chiếm chỗ.
- `renderReady` mặc định dùng `CollapsibleNativeAdCard`; slot thật truyền phần dựng có `NativeAdView`.

**Composable công khai** `CollapsibleNativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier)`:

1. `require(placement.format == AdFormat.NATIVE_COLLAPSIBLE)`.
2. `val state = rememberCollapsibleAdState()` ngay đầu hàm, trước khi đọc `LocalAdsController` và luồng tải: không nằm trong `key(...)` hay nhánh nào phụ thuộc quảng cáo.
3. Không có `AdsController` (test, preview), chưa đồng ý, vị trí tắt: `Box(modifier)` rỗng, như `AdSlot` và `NativeAdSlot`.
4. Lấy `NativeAdState` từ luồng tải native của T-010 cho `placement` (cùng cách `NativeAdSlot` làm: thời gian chờ, hủy quảng cáo khi rời màn hình hay khi thay quảng cáo), đổi sang `CollapsibleAdContent` (`Loading`, `Ready`, `Unavailable` thành `Hidden`) và gọi `CollapsibleNativeAdContent`.
5. `renderReady` của slot bọc phần dựng `NativeAdView` của T-010 quanh `CollapsibleNativeAdCard`, đăng ký lại các view tài sản (tiêu đề, icon, CTA, media) mỗi khi `collapsed` đổi: dạng gọn không có `MediaView`. Cách đăng ký theo đúng hàm T-010 dùng cho small và medium; không gọi SDK ngoài `:core:ads`.
6. `onCollapsed` gọi `ads.onNativeCollapsed(placement)`: hàm `internal` mới của `AdsController`, ghi `AppLogger` thẻ `Ads`, nội dung `native_collapsed placement=<key>`. Không ghi sự kiện phân tích (spec mục 6, ADR-0022).

Luồng tải native của T-010: nếu nó chỉ nhận `NATIVE_SMALL` và `NATIVE_MEDIUM` (`require` hay `when`), thêm `NATIVE_COLLAPSIBLE` vào tập định dạng native; dạng mở có media nên yêu cầu tải như medium (cùng `NativeAdOptions`, AdChoices góc trên phải). Không đổi gì khác trong luồng tải.

**Screenshot trong khung giống thiết kế.** `:core:ads` không có màn hình; `AdGalleryScreen` là của T-014. Để có ảnh cùng tên với hai ảnh thiết kế mà README gán cho T-012, test dựng một khung chỉ có trong test (`CollapsibleAdDesignFrame`) đúng frame thiết kế: nền `background`, tiêu đề "Collapsible ad" (`titleLarge`, cách trên 20 dp, trái 16 dp), danh sách 6 mục "List item 1" đến "List item 6" (`bodyLarge`, lề dọc 16 dp, viền dưới `outlineVariant`) bắt đầu ở y = 72 dp, quảng cáo ghim ở đáy cách mép dưới và hai bên 16 dp. Danh sách trong khung là `LazyColumn` chiếm phần còn lại (`weight(1f)`) để khung thấp 360 x 640 cuộn được. Chữ của khung là dữ liệu test (như chữ của quảng cáo giả), không phải chữ của app; khi T-014 dựng màn hình thật thì chữ nằm trong `strings.xml` staging (câu hỏi 1).

**So sánh với thiết kế.** Script bọc sẵn của dự án chỉ đọc ảnh chuẩn ở `feature/<feature>/src/test/screenshots/`, mà feature `ads` không có module `:feature:ads`. Task chạy thẳng chương trình so sánh mà script gọi: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/ads/design/screens core/ads/src/test/screenshots <thư mục tạm>`, rồi chép hai ảnh `_compare.png` của T-012 và bảng tỷ lệ khác biệt của hai ảnh đó vào `docs/features/ads/design/review/`. Không sửa script (câu hỏi 3). Các ảnh thiết kế khác của `ads` sẽ báo thiếu trong kết quả: thuộc T-010, T-011, T-013, T-014.

**Mẫu làm theo:** `AdSlot` (slot rỗng khi không có controller, `require` định dạng), `NativeAdSlot` và `NativeAdCard` của T-010, `DesignSystemCatalogScreenshotTest` (screenshot trong module core, `@GraphicsMode(NATIVE)`), `SampleScreenTest` (test giao diện Robolectric).

**Thứ tự commit (L-025):** commit test của bước 3 gồm `CollapsibleNativeAdTest.kt`, `CollapsibleNativeAdScreenshotTest.kt` và các khai báo tối thiểu để test biên dịch được (giá trị enum `NATIVE_COLLAPSIBLE`, nhánh mã thử, chữ ký của `CollapsibleAdState`, `rememberCollapsibleAdState`, `CollapsibleNativeAdCard`, `CollapsibleAdContent`, `CollapsibleNativeAdContent` với thân `TODO()` hay rỗng), để test thất bại vì hành vi chưa có, không phải vì không biên dịch. Commit code của bước 4 điền thân hàm, `CollapsibleNativeAdSlot`, `onNativeCollapsed`, drawable, chuỗi, rồi ghi ảnh chuẩn.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Thêm `AdFormat.NATIVE_COLLAPSIBLE` kèm KDoc. Không thêm, bớt hay đổi `AdPlacement` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | `GoogleTestAdUnitIds.of`: nhánh `NATIVE_COLLAPSIBLE` dùng mã thử native mà T-010 đã thêm |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `internal fun onNativeCollapsed(placement)` ghi `AppLogger`; nếu luồng tải native của T-010 giới hạn định dạng thì thêm `NATIVE_COLLAPSIBLE`, tải như medium. Không đổi gì khác |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAd.kt` | `CollapsibleAdState`, `rememberCollapsibleAdState`, `CollapsibleNativeAdCard`, `CollapsibleAdContent`, `CollapsibleNativeAdContent`, `CollapsibleNativeAdSlot`, hằng số kích thước dạng mở |
| Tạo | `core/ads/src/main/res/drawable/ads_ic_collapse.xml` | Vector mũi tên xuống 24 dp, nét 2 dp, màu do `tint` |
| Sửa | `core/ads/src/main/res/values/strings.xml` | `ads_collapse` = "Collapse ad" (mô tả cho trình đọc màn hình) |
| Sửa | `core/ads/build.gradle.kts` | Chỉ khi T-010 chưa cấu hình screenshot cho `:core:ads`: plugin `alias(libs.plugins.roborazzi)`, `outputDir` là `src/test/screenshots`, ba thư viện test Roborazzi, giống `core/designsystem/build.gradle.kts`. Không thêm thư viện nào ngoài ba thư viện đã có trong `libs.versions.toml` |
| Tạo | `core/ads/src/test/resources/robolectric.properties` | Chỉ khi T-010 chưa tạo: `sdk=35`, như `core/designsystem` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Test giao diện Robolectric ở mục 4; quảng cáo giả `collapsibleTestAd(headline)` khai báo trong file |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdScreenshotTest.kt` | 5 screenshot ở mục 4 và khung `CollapsibleAdDesignFrame` chỉ dùng trong test |
| Tạo | `core/ads/src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` | Ảnh chuẩn, khung 411 x 891, cùng tên ảnh thiết kế |
| Tạo | `core/ads/src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` | Ảnh chuẩn, khung 411 x 891, cùng tên ảnh thiết kế |
| Tạo | `core/ads/src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png` | Ảnh chuẩn khung thấp 360 x 640, dạng mở (nhiều nội dung nhất) (L-028) |
| Tạo | `core/ads/src/test/screenshots/CollapsibleNativeAd_expanded_dark.png` | Ảnh chuẩn component dạng mở, chế độ tối |
| Tạo | `core/ads/src/test/screenshots/CollapsibleNativeAd_collapsed_dark.png` | Ảnh chuẩn component dạng gọn, chế độ tối |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_collapsible-expanded_light_compare.png` | Ảnh so sánh thiết kế với screenshot |
| Tạo | `docs/features/ads/design/review/AdGalleryScreen_collapsible-collapsed_light_compare.png` | Ảnh so sánh thiết kế với screenshot |
| Tạo | `docs/features/ads/design/review/summary.md` | Tỷ lệ khác biệt của hai ảnh trên. Nếu T-010 đã tạo file này: thêm hai dòng của T-012, giữ dòng cũ (thao tác thành sửa) |
| Sửa | `docs/features/ads/design/README.md` | Mục 7: đánh dấu hai tiêu chí "kiểm khi code" cho phần thu gọn sau khi xem ảnh; mục 8: ghi kết quả đối chiếu của hai ảnh T-012. README có `status: approved`; chỉ ghi kết quả, không đổi nội dung thiết kế (câu hỏi 5) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3: dòng "Native collapsible" cột "Có trong `:core:ads`" ghi `CollapsibleNativeAdSlot`; mục 5 bước 4: thêm `CollapsibleNativeAdSlot(placement)` cho vị trí định dạng `NATIVE_COLLAPSIBLE`, màn hình gọi tự đặt ở đáy |
| Sửa | `docs/features/ads/tickets/T-012-native-thu-gon.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-012-native-thu-gon.md` | Nhật ký thực hiện |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Không sửa:

- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: không thêm khóa, không thêm vị trí (khóa bật tắt sinh từ `AdPlacement.entries`, T-007), nên test sinh bảng khóa của T-007 vẫn khớp.
- `app/` (kể cả `StagingFeatureFlagsModule`, giá trị cục bộ của T-008): không có vị trí mới để bật.
- `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt`: `googleTestUnits_existForEveryPlacement` lặp theo `AdPlacement`, không đổi; nhánh `when` mới được trình biên dịch kiểm (biểu thức `when` phải đủ nhánh).
- Các file của T-010 (`NativeAdCard`, `NativeAdSlot`, placeholder): chỉ gọi. Cần sửa thì dừng (mục 7).
- `CLAUDE.md`: T-010 cập nhật bước 7 cho native nói chung; T-012 chỉ thêm vào `ads-guide.md`.
- `scripts/check.sh`: không sửa. Script tự thấy `captureRoboImage` trong `core/ads/src/test` và kiểm ảnh của `:core:ads`, như đã làm với `core/designsystem`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: thêm định dạng native thu gọn (`AdFormat.NATIVE_COLLAPSIBLE`) và composable hiển thị nó, theo spec KB-4. Không thêm vị trí: không có `AdPlacement` mới, không có khóa `ads_<vị trí>_enabled` mới. Mã thử native của Google có sẵn từ T-010; không có mã quảng cáo thật.
- Sự kiện phân tích: Không chạm. Thu gọn chỉ ghi log hành vi qua `AppLogger` (ADR-0019), không ghi Analytics.
- File R2: Không chạm.

## 4. Kiểm thử

Test giao diện chạy bằng Robolectric, `createComposeRule()`, `@GraphicsMode(GraphicsMode.Mode.NATIVE)` (có đo kích thước), khung `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, bọc trong `AppTheme(darkTheme = false)`. Quảng cáo giả: `collapsibleTestAd(headline = "Headline of the ad")` trả `NativeAdAssets` có headline, advertiser "Advertiser", callToAction "Install", icon giả và có media. Nội dung trong test dựng bằng `CollapsibleNativeAdContent` với `state = rememberCollapsibleAdState()` và `content` lấy từ một `mutableStateOf` mà test đổi được; `renderReady` mặc định. Số đo so với dung sai 1 dp.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-4, tiêu chí 1: mở khi vào | Compose UI test | `CollapsibleNativeAdTest.firstShown_isExpanded`: `Ready(ad)`, node `collapsible_ad_media` tồn tại, node `collapsible_ad_collapse` tồn tại, chiều cao node `collapsible_ad` là 342 dp |
| KB-4, tiêu chí 1: nút ít nhất 48 dp | Compose UI test | `CollapsibleNativeAdTest.collapseButton_isAtLeast48dp`: kích thước node `collapsible_ad_collapse` (đo qua `getBoundsInRoot` của chính node, `IconButton` có vùng chạm 48 dp) rộng và cao ít nhất 48 dp |
| KB-4, tiêu chí 1: không cùng góc với AdChoices | Compose UI test | `CollapsibleNativeAdTest.collapseButton_isInTopStartCorner`: so với bounds của `collapsible_ad`, cạnh trái nút cách cạnh trái khung không quá 16 dp, cạnh phải nút nằm trước tâm ngang của khung, cạnh trên nút cách cạnh trên khung không quá 12 dp; AdChoices ở góc trên phải do SDK đặt. Cùng test kiểm không node nào bấm được (có `OnClick`) giao với ô 24 x 24 dp ở góc trên phải của khung |
| KB-2 qua dạng mở: media ít nhất 120 x 120 dp | Compose UI test | `CollapsibleNativeAdTest.expandedMedia_isAtLeast120dp`: node `collapsible_ad_media` rộng ít nhất 120 dp, cao 160 dp |
| KB-4, tiêu chí 2: chạm thì gọn | Compose UI test | `CollapsibleNativeAdTest.tapCollapse_showsSmallLayout`: `performClick` trên `collapsible_ad_collapse`; sau đó không còn node `collapsible_ad_media` và `collapsible_ad_collapse`; chiều cao `collapsible_ad` là 72 dp; vẫn thấy chữ "Headline of the ad", "Ad", "Install" |
| KB-4, tiêu chí 2: báo một lần | Compose UI test | `CollapsibleNativeAdTest.tapCollapse_reportsOnce`: đếm số lần gọi `onCollapsed` sau một lần chạm là 1 |
| KB-4, tiêu chí 3: không có cách mở lại | Compose UI test | `CollapsibleNativeAdTest.collapsed_hasNoExpandControl`: sau khi thu gọn, không node nào có `contentDescription` của `R.string.ads_collapse`; chạm vào chữ tiêu đề rồi vẫn cao 72 dp, không có `collapsible_ad_media` |
| KB-4, tiêu chí 3: quảng cáo mới không mở lại | Compose UI test | `CollapsibleNativeAdTest.collapsed_newAdLoaded_staysCollapsed`: thu gọn, đổi `content` sang `Ready(collapsibleTestAd("Second ad"))`, chờ idle; thấy "Second ad", cao 72 dp, không có `collapsible_ad_media` |
| KB-4, tiêu chí 3: tải lại không mở lại | Compose UI test | `CollapsibleNativeAdTest.collapsed_reloading_keepsSmallPlaceholderThenStaysCollapsed`: thu gọn, đổi sang `Loading` thì chiều cao `collapsible_ad` (thẻ trên placeholder) là 72 dp; đổi sang `Ready(collapsibleTestAd("Second ad"))` thì vẫn 72 dp, không có media |
| KB-4, tiêu chí 3: còn ở màn hình đó (xoay máy, quay lại từ ngăn xếp) | Compose UI test | `CollapsibleNativeAdTest.collapsed_survivesStateRestoration`: dùng `StateRestorationTester`, thu gọn, `emulateSavedInstanceStateRestore()`, vẫn cao 72 dp, không có media |
| Bộ giữ trạng thái chỉ thu gọn được | Unit test trên Robolectric (cùng lớp test) | `CollapsibleNativeAdTest.stateHolder_onlyCollapses`: `CollapsibleAdState` mới có `collapsed == false`; sau `collapse()` hai lần vẫn `true`; lớp không có hàm công khai nào đặt lại `false` (kiểm bằng biên dịch: `collapsed` có `private set`) |
| KB-1 qua slot: giữ chỗ khi tải | Compose UI test | `CollapsibleNativeAdTest.loadingExpanded_reservesExpandedHeight`: `Loading` khi chưa thu gọn, chiều cao `collapsible_ad` là 342 dp |
| KB-1 qua slot: không có quảng cáo thì không chiếm chỗ | Compose UI test | `CollapsibleNativeAdTest.hidden_takesNoSpace`: `Hidden`, không có node `collapsible_ad`; một `Box` bọc ngoài có `testTag("host")` cao 0 dp |
| KB-1: không có controller, chưa đồng ý, vị trí tắt | Compose UI test và kiểm tra thủ công | Nhánh "không vẽ gì" dùng chung với `Hidden` (test `hidden_takesNoSpace`). `CollapsibleNativeAdSlot` không có `LocalAdsController` chưa test được trong T-012 vì `AdPlacement` chưa có vị trí `NATIVE_COLLAPSIBLE` (vị trí đến ở T-014); kiểm trên staging theo dòng cuối của bảng |
| Định dạng sai bị từ chối | Compose UI test | `CollapsibleNativeAdTest.slot_rejectsNonCollapsiblePlacement`: `setContent { CollapsibleNativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL) }` ném `IllegalArgumentException` (`assertTrue(error is IllegalArgumentException)`) |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Trong `firstShown_isExpanded` và `tapCollapse_showsSmallLayout`: thấy nhãn "Ad" ở cả hai dạng; nút thu gọn có `contentDescription` "Collapse ad". Chữ còn lại là của quảng cáo giả |
| Giao diện dạng mở, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, khung `w411dp-h891dp-xxhdpi`, `CollapsibleAdDesignFrame` với `Ready(ad)` | `CollapsibleNativeAdScreenshotTest.collapsibleExpanded_light` ghi `src/test/screenshots/AdGalleryScreen_collapsible-expanded_light.png` |
| Giao diện dạng gọn, đúng thiết kế | Screenshot test cùng tên ảnh thiết kế, cùng khung; trạng thái truyền `collapsed = true` qua `CollapsibleNativeAdCard` | `CollapsibleNativeAdScreenshotTest.collapsibleCollapsed_light` ghi `src/test/screenshots/AdGalleryScreen_collapsible-collapsed_light.png` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, dạng mở (nhiều nội dung nhất) | `CollapsibleNativeAdScreenshotTest.collapsibleExpanded_small` ghi `src/test/screenshots/AdGalleryScreen_collapsible-expanded_small.png`; cùng test kiểm bằng compose rule ở khung này: nút thu gọn và CTA nằm trọn trong màn hình (bounds trong 0..640 dp), danh sách phía trên cuộn được (`performScrollToNode` tới "List item 6") |
| KB-1, KB-2: chế độ tối | Screenshot test, chỉ component, `AppTheme(darkTheme = true)`, nền `background`, lề 16 dp | `CollapsibleNativeAdScreenshotTest.expanded_dark` ghi `src/test/screenshots/CollapsibleNativeAd_expanded_dark.png`; `collapsed_dark` ghi `src/test/screenshots/CollapsibleNativeAd_collapsed_dark.png`. Không có ảnh thiết kế tối cho dạng thu gọn; người duyệt xem bằng mắt so với `AdGalleryScreen_default_dark` |
| Đối chiếu thiết kế | Ảnh so sánh bằng `scripts/design-compare/DesignCompare.java` (mục 3.1), rà soát độc lập theo mục 7 của mẫu thiết kế | Hai ảnh `_compare.png` và `summary.md` trong `docs/features/ads/design/review/`; lệch bố cục quá một bước lưới 4 dp thì sửa code theo `context/`, không viết cứng giá trị để giảm tỷ lệ khác biệt |
| Log thu gọn | Kiểm tra thủ công | Trên staging, sau khi T-014 có vị trí thu gọn: `adb logcat -v threadtime \| grep ' VGL\.'` thấy `native_collapsed placement=<key>` một lần mỗi lần chạm |
| Quảng cáo thật trong `NativeAdView` (đăng ký view, AdChoices, media) | Kiểm tra thủ công | Trên staging, màn hình xem thử của T-014, mã thử native: dạng mở có AdChoices ở góc trên phải, không đè nút thu gọn; chạm nút thì gọn ngay, CTA vẫn bấm được và mở trang của quảng cáo thử; xoay máy vẫn gọn; ra màn hình khác rồi quay lại vẫn gọn; mở lại màn hình xem thử từ đầu thì mở. Ad Inspector không báo lỗi tài sản chưa đăng ký |

**Ngoại lệ viết test trước:** `ads_ic_collapse.xml`, chuỗi `ads_collapse` và đoạn `ads-guide.md` là tài nguyên và tài liệu, kiểm gián tiếp qua test giao diện và screenshot. Phần nối với SDK trong `CollapsibleNativeAdSlot` (luồng tải, `NativeAdView`) không chạy được trên JVM khi không có `AdsController`; kiểm thủ công trên staging như bảng trên. Ảnh chuẩn screenshot ghi sau khi code xong (bước ghi ảnh mới của `scripts/check.sh`), không có trước.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (điểm 1: thêm định dạng native thu gọn), module core dùng chung `:core:ads` (điểm 7). Không thêm vị trí, SDK, quyền, sự kiện hay khóa. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là quảng cáo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module `:core:ads` (vài file nguồn, tài nguyên, test, ảnh chuẩn), thêm API công khai (`CollapsibleNativeAdSlot`, `CollapsibleNativeAdCard`, `CollapsibleAdState`, `rememberCollapsibleAdState`, giá trị enum mới); ngoài ra chỉ tài liệu |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, thiết kế đã duyệt có số đo của cả hai dạng; AI tự quyết chiều cao giữ chỗ khi tải, cách nhớ trạng thái, nơi đặt ảnh chuẩn mang tên `AdGalleryScreen`, cách so sánh thiết kế cho module core (câu hỏi 1 đến 4) |
| C. Khả năng kiểm chứng tự động | 2 | Cả ba tiêu chí KB-4 có test giao diện trên JVM; nhưng phần nối với SDK (đăng ký lại view khi đổi dạng, vị trí AdChoices thật) chỉ kiểm thủ công, và chưa kiểm trên máy được cho tới khi T-014 có vị trí thu gọn |
| D. Ảnh hưởng chéo | 2 | Sửa `AdPlacement.kt`, `AdUnitIds.kt`, `AdsController.kt`, `strings.xml`, có thể `build.gradle.kts` của `:core:ads`, mà T-010, T-011, T-013, T-014 cũng sửa; không đổi hành vi có sẵn mà feature khác dựa vào (native small, medium, banner, interstitial giữ nguyên) |
| E. Độ mới của kỹ thuật | 2 | Bố cục Compose và `rememberSaveable` là mẫu phổ biến; đổi bố cục bên trong một `NativeAdView` (bỏ `MediaView` khi thu gọn, đăng ký lại view) chưa có trong dự án |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` (enum `AdFormat`), `AdUnitIds.kt` (`GoogleTestAdUnitIds`), `AdsController.kt` (hàm log, có thể tập định dạng native), `core/ads/src/main/res/values/strings.xml`, có thể `core/ads/build.gradle.kts`, `docs/ops/ads-guide.md`, `docs/features/ads/design/README.md`, `docs/features/ads/design/review/summary.md`.
- Nơi đọc các file này (L-020): `scripts/check.sh` quét `core/*/src/test` tìm `captureRoboImage` và kiểm ảnh trong `core/ads/src/test/screenshots/`: đây là ý muốn, ảnh chuẩn mới đi cùng PR. `scripts/test-summary.sh` và `.github/workflows/pr-check.yml` chỉ đếm hay tải lên mọi ảnh `src/test/screenshots/*.png` theo mẫu, không đọc file nào theo tên. `AdPlacement.kt` được `scripts/release.sh` đọc theo mẫu dòng `TEN("key", AdFormat.X),` của `AdPlacement`; task không thêm dòng vào `AdPlacement` nên không ảnh hưởng (và không chạm `release.sh`). Test sinh bảng khóa của T-007 đọc `AdPlacement.entries`, không đổi. `docs/STATUS.md`: `scripts/tests/run.sh` chỉ đọc trang của repo giả trong thư mục tạm; `scripts/status.py --check` xác nhận trang khớp. `docs/features/ads/design/README.md` và `review/`: `scripts/figma-export.py` ghi `screens/` và `context/`, không đọc `review/`.
- Task khác trong sprint có thể xung đột: T-010 (bắt buộc trước: tạo phần dựng, luồng tải, mã thử native; cùng sửa `AdPlacement.kt`, `AdUnitIds.kt`, `AdsController.kt`, `build.gradle.kts`, `ads-guide.md`, có thể tạo `review/summary.md`); T-011 và T-013 (cùng sửa `AdPlacement.kt`, `AdUnitIds.kt`, `strings.xml`, `ads-guide.md`); T-014 (dùng `CollapsibleNativeAdSlot`, thêm vị trí thu gọn của màn hình xem thử, không được trùng tên ảnh chuẩn `AdGalleryScreen_collapsible-*`, câu hỏi 1); T-015 (sửa `core/ads/build.gradle.kts`).
- Đề xuất thứ tự chạy: sau T-007, T-009, T-010 (bắt buộc); trước T-014 để màn hình xem thử dùng được slot thu gọn. Chạy độc lập với T-011 và T-013 nhưng lần lượt (cùng file nhỏ, xung đột merge dễ gỡ). Không cùng lúc với T-015.

## 7. Điều kiện dừng riêng

- Hợp đồng T-010 khác giả định ở mục 2 (tên `NativeAdAssets`, `NativeAdSize`, `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdState`, cách nhận quảng cáo cho một vị trí), hoặc T-010 chưa merge.
- Dùng lại phần dựng của T-010 mà phải sửa file của T-010 (ví dụ đổi `private` thành `internal`, thêm tham số chiều cao cho placeholder): dừng, đề xuất sửa trong plan bản 2.
- Không bỏ hay thay được `MediaView` khi thu gọn mà không tạo lại `NativeAdView` và tải quảng cáo mới (tải mới khi thu gọn là làm mới ngoài ý người dùng).
- Cần vẽ biểu tượng AdChoices trong Compose, hay đặt `NativeAdOptions` AdChoices ở góc khác góc trên phải.
- Cần thêm `AdPlacement`, khóa Remote Config, thư viện icon hay bất kỳ thư viện nào.
- Screenshot dạng mở lệch thiết kế quá một bước lưới 4 dp ở vị trí nút thu gọn, media hay CTA sau 3 lần sửa.

## 8. Câu hỏi cho người duyệt

1. **Ảnh chuẩn mang tên `AdGalleryScreen_collapsible-*` nằm ở `:core:ads`.** README thiết kế gán hai ảnh này cho T-012, nhưng `AdGalleryScreen` là màn hình của T-014 ở `app/src/staging`. AI đề xuất: T-012 ghi hai ảnh này (và ảnh `_small`) trong `core/ads/src/test/screenshots/` bằng một khung chỉ có trong test, giống frame thiết kế; T-014 không ghi lại ảnh cùng tên, chỉ dùng `CollapsibleNativeAdSlot` trong màn hình thật. Phương án khác: T-012 chỉ ghi ảnh component (`CollapsibleNativeAd_expanded_light`, `_collapsed_light`) và để T-014 ghi hai ảnh tên `AdGalleryScreen_collapsible-*`; khi đó T-012 không có ảnh trùng tên thiết kế.
2. **Chiều cao giữ chỗ khi đang tải.** Thiết kế không có frame tải cho native thu gọn. AI đề xuất: giữ chỗ bằng dạng hiện tại, 342 dp khi chưa thu gọn, 72 dp khi đã thu gọn, để nội dung không nhảy khi quảng cáo hiện (KB-1). Phương án khác: luôn giữ 72 dp rồi bung ra 342 dp khi có quảng cáo; ít chiếm chỗ khi không có quảng cáo nhưng làm nội dung nhảy, dễ gây bấm nhầm.
3. **So sánh thiết kế cho module core.** Script bọc sẵn chỉ đọc `feature/<feature>/src/test/screenshots/`, nên với `ads` (không có `:feature:ads`) nó báo thiếu mọi ảnh. AI đề xuất: task chạy thẳng chương trình Java mà script gọi, với thư mục `core/ads/src/test/screenshots`; sau đợt quảng cáo, đề xuất một thay đổi nhỏ cho script (tham số thư mục ảnh chuẩn) bằng plan riêng. Lý do không sửa trong task này: plan T-010 đến T-014 đều cần cùng thay đổi, sửa ở một task sẽ xung đột với các task kia. Câu hỏi này có lẽ nên trả lời chung ở họp đánh giá cho cả đợt.
4. **Có hiệu ứng khi thu gọn không?** AI đề xuất: không, chuyển ngay sang dạng gọn. Hiệu ứng co lại làm CTA và chữ di chuyển dưới tay người dùng trong lúc co, và làm screenshot khó ổn định. Có thể thêm sau nếu muốn.
5. **Ghi kết quả đối chiếu vào README thiết kế đã duyệt.** README có `status: approved`; mục 7 và 8 có chỗ "kiểm khi code" và "Kết quả đối chiếu" chờ điền. AI đề xuất: chỉ đánh dấu và ghi kết quả của phần thu gọn ở hai mục này, không đổi nội dung thiết kế. Nếu muốn README chỉ do người sửa, bỏ dòng này khỏi danh sách file; kết quả vẫn có trong `review/summary.md`.
6. **Trạng thái thu gọn sống bao lâu.** AI đề xuất: nhớ bằng `rememberSaveable`, nên xoay máy và quay lại màn hình từ ngăn xếp vẫn gọn; mở một lần mới của màn hình thì bắt đầu ở dạng mở. Phương án khác: nhớ suốt phiên app cho mỗi vị trí (ít quảng cáo lớn hơn, nhưng vượt tiêu chí "còn ở màn hình đó" và cần trạng thái trong `AdsController`).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
