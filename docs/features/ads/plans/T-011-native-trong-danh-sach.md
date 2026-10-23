---
ticket: T-011
title: Native trong danh sách
status: approved            # draft | approved | stopped | done
version: 1
risk_score:
risk_level: high
token_estimate: 20M
blocking_points: [ads, core]
run_order: 7
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-23
---

# PLAN T-011: NATIVE TRONG DANH SÁCH

## 1. Tóm tắt

Thêm vào `:core:ads` hàm thuần `inListAdPositions(itemCount, every)` tính chỗ đặt native trong danh sách, và hai hàm mở rộng cho `LazyListScope` và `LazyGridScope` chèn ô quảng cáo sau mỗi `ads_inlist_every_count` mục, không ở vị trí đầu, không chèn khi danh sách ít hơn N mục. Mỗi ô quảng cáo giữ quảng cáo đã tải khi người dùng cuộn qua lại (không tải lại), hủy hết khi rời màn hình. Kiểm chứng bằng unit test cho hàm tính vị trí và phần giữ quảng cáo, và test giao diện Robolectric cho thứ tự, khoảng cách, nhãn "Ad" của ô chèn; ảnh `AdGalleryScreen_inlist_light` thuộc T-014.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục (N chỉnh từ xa, mặc định 8), không bao giờ ở vị trí đầu tiên | N đọc từ `AdsSettings.INLIST_EVERY_COUNT` (T-007), theo dõi như `Flow`. Quảng cáo thứ k (k từ 1) nằm ngay sau mục thứ k x N của app. Quảng cáo chỉ nằm giữa hai mục của app, không ở cuối danh sách (câu hỏi 1). Ví dụ thiết kế, N = 3, 7 mục: quảng cáo sau mục 3 và mục 6 |
| KB-3 | Khi danh sách ít hơn N mục thì không có quảng cáo | Hệ quả của quy tắc trên. Theo câu hỏi 1, danh sách đúng N mục cũng không có quảng cáo (quảng cáo duy nhất sẽ nằm ở cuối) |
| KB-3 | Mỗi ô quảng cáo có nhãn "Ad" rõ, không thể nhầm với một mục của danh sách | Ô chèn dùng phần dựng native của T-010 (nhãn "Ad", nền `surfaceContainerLow`, viền, bo 12 dp), cách mục trên và dưới 16 dp (`Spacing.md`), đúng khung "In-list ad slot" của thiết kế. Cỡ small hay medium theo `placement.format` (spec, câu hỏi 4) |
| KB-1 (đặc biệt), áp dụng cho ô chèn | Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ | Khi chưa được phép (không có `AdsController`, chưa đồng ý, vị trí hay công tắc tổng tắt) thì không chèn hàng nào vào danh sách, kể cả hàng rỗng. Ô đã chèn mà hết thời gian chờ không có quảng cáo thì thu về 0 dp, bỏ cả khoảng 16 dp |
| KB-1 (đặc biệt), áp dụng cho ô chèn | Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ | Quảng cáo của mọi ô được giữ ở mức màn hình (không ở mức hàng của danh sách) và hủy khi màn hình rời khỏi cây giao diện. Cuộn một ô ra khỏi màn hình không hủy, cuộn lại không tải lại (ads-guide.md, mục 6: native không tự làm mới) |

Thiết kế tham chiếu: `features/ads/design/README.md`, ảnh `design/screens/AdGalleryScreen_inlist_light.png` và số liệu `design/context/AdGalleryScreen_inlist_light.md` (N = 3, small sau mục 3, medium sau mục 6, khung "In-list ad slot" lề trên dưới 16 dp). Ảnh này là màn hình xem thử của T-014; screenshot test cùng tên do T-014 viết. T-011 chỉ có unit test và test giao diện của hàm chèn.

ADR liên quan: ADR-0013 (đồng ý trước khi tải quảng cáo), ADR-0014 (vị trí mặc định tắt), ADR-0019 (log), ADR-0020 (thiết kế, tên screenshot), ADR-0022 (native, quy chuẩn), ADR-0023 (khóa Remote Config). ADR-0024 (luôn cần mạng) không đổi cách làm: không có mạng thì ô hết thời gian chờ và thu lại như mọi native.

## 3. Giải pháp

### 3.1. Cách làm

Giả định T-007, T-009, T-010 đã merge, dùng đúng tên trong hợp đồng chung: `AdsSettings.INLIST_EVERY_COUNT`, `RemoteSettings.value(key)`, `AdsController.isEnabled(placement)` (gồm công tắc tổng, T-009), `AdFormat.NATIVE_SMALL`, `AdFormat.NATIVE_MEDIUM`, `NativeAdState` (`Loading`, `Loaded(ad: LoadedNativeAd)`, `Unavailable`), `LoadedNativeAd` (`assets`, `destroy()`), `NativeAdCard`, `NativeAdAssets`, `NativeAdSize`, `NativeAdPlaceholder`, `NativeAdSlot`, phần dựng nội bộ `BoundNativeAd(ad, size, modifier)`, hàm tải một quảng cáo `AdsController.loadNativeAd(placement): NativeAdState`, chuỗi `R.string.ads_native_badge`, file test `FakeNativeAds.kt` (`FakeLoadedNativeAd` đếm `destroy`, `sampleNativeAdAssets`) (T-010), `AppLogger` trong `:core:ads` (T-009). `core/ads/build.gradle.kts` đã có `:core:designsystem`, Roborazzi (T-010) và `:core:logging` (T-009).

**Hàm thuần (`InListAdPositions.kt`, public).**

```kotlin
/**
 * Positions, in the combined list of items and ads, where an in-list ad goes (ADR-0022, ads-guide.md section 4).
 * The k-th ad follows the (k * every)-th item. An ad always sits between two items: never first, never last,
 * so a list of `every` items or fewer has none. A non-positive `every` means no ads.
 */
fun inListAdPositions(itemCount: Int, every: Int): List<Int>

/** `every` rounded up to whole grid rows, so an ad row never leaves a half-filled row above it. */
fun gridAdEvery(every: Int, columns: Int): Int

/** N to use, or null when no ad row may be added (ads not allowed, placement off, bad remote value). */
internal fun inListEvery(adsAllowed: Boolean, everyCount: Long): Int?
```

- Số quảng cáo là `(itemCount - 1) / every` khi `every >= 1`, vị trí của quảng cáo thứ k (từ 1) là `k * every + (k - 1)`. Ví dụ `inListAdPositions(7, 3) == listOf(3, 7)`, `inListAdPositions(20, 8) == listOf(8, 17)`.
- `gridAdEvery(3, 2) == 4`, `gridAdEvery(8, 3) == 9`; `columns < 1` trả `every` không đổi.
- `inListEvery` trả null khi `adsAllowed` là false, hoặc `everyCount` ngoài khoảng `1..Int.MAX_VALUE` (giá trị từ xa sai thì an toàn là không chèn).

**Giữ quảng cáo của từng ô (`InListAds.kt`).**

```kotlin
/** Ads of one in-list placement on one screen. Create it with [rememberInListAds], outside the lazy list. */
@Stable
class InListAds internal constructor(
    val placement: AdPlacement,
    every: State<Int?>,
    private val loader: InListAdLoader?,
    private val scope: CoroutineScope,
    private val logger: AppLogger?,
) {
    /** Current N, or null while no ad row may be added. */
    val every: Int?
    internal fun slot(index: Int): State<NativeAdState>   // loads once on first call, then reuses
    internal fun destroyAll()
}

/** Named apart from T-010's callback-based NativeAdLoader in the same package. */
internal fun interface InListAdLoader {
    suspend fun load(placement: AdPlacement): NativeAdState   // Loaded or Unavailable, after the load timeout
}

@Composable
fun rememberInListAds(placement: AdPlacement): InListAds
```

- `rememberInListAds` kiểm `placement.format` là `NATIVE_SMALL` hoặc `NATIVE_MEDIUM` bằng `require` (như `AdSlot` kiểm banner), qua hàm nội bộ `requireInListFormat(format)` để test được. Không có `LocalAdsController` (test, preview) thì `every` là null, `loader` là null: không chèn gì.
- `every` lấy từ `AdsController.inListEvery(placement): Flow<Int?>` (hàm nội bộ mới): ghép `canRequestAds`, `isEnabled(placement)` và `settings.value(AdsSettings.INLIST_EVERY_COUNT)` rồi gọi `inListEvery(...)`. Thu bằng `collectAsStateWithLifecycle(initialValue = null)`: chưa biết thì chưa chèn, tránh danh sách nhảy khi giá trị đến.
- `slot(index)` dùng một `mutableMapOf<Int, MutableState<NativeAdState>>`: lần đầu tạo trạng thái `Loading` và chạy `loader.load(placement)` trong `scope` (của `rememberCoroutineScope` ở mức màn hình); lần sau trả trạng thái đã có. Ô `Unavailable` không tải lại khi cuộn qua lại; vào lại màn hình thì tải mới.
- `rememberInListAds` có `DisposableEffect` gọi `destroyAll()` khi rời màn hình: gọi `destroy()` cho mọi `LoadedNativeAd` trong trạng thái `Loaded`, hủy việc tải đang chạy, xóa map.
- Log (ADR-0019, thẻ `Ads`, như T-009): một dòng `INFO` cho kết quả tải mỗi ô, dạng `in-list <placement.key> slot <index>: LOADED` hoặc `UNAVAILABLE`. Không ghi nội dung quảng cáo. Không log mỗi lần cuộn.
- Tải một quảng cáo riêng cho mỗi ô: `AdsController.loadNativeAd(placement)` của T-010 (chờ tối đa `NATIVE_LOAD_TIMEOUT_SECONDS`, quảng cáo tới muộn hay tới sau khi hủy do T-010 hủy; quảng cáo `Loaded` thuộc về người gọi). `InListAdLoader` của màn hình thật là `InListAdLoader(ads::loadNativeAd)`. T-011 không thêm hàm tải nào. Lý do không dùng luồng tải theo vị trí của `NativeAdSlot`: mọi ô của một vị trí trong danh sách phải có quảng cáo riêng (một quảng cáo không hiện hai chỗ), và phải sống lâu hơn hàng của danh sách.

**Hàm chèn vào danh sách và lưới (`InListAds.kt`, public).**

```kotlin
fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
)

fun <T> LazyGridScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    columns: Int,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
)
```

- Hai hàm trên gọi hai hàm nội bộ cùng tên gốc nhận sẵn `positions: List<Int>` và `adContent: @Composable (slot: Int) -> Unit`. Test dùng hàm nội bộ với quảng cáo giả (`NativeAdCard` với `sampleNativeAdAssets` trong `FakeNativeAds.kt` của T-010), không cần SDK.
- Duyệt mục theo thứ tự, gặp vị trí trong `positions` thì thêm một hàng quảng cáo. Khóa của hàng quảng cáo là một `private data class InListAdKey(placementKey: String, slot: Int)` để không trùng khóa của mục; `contentType` là một đối tượng riêng `InListAdContentType` để Compose không dùng lại khung của mục cho quảng cáo.
- Lưới: N thực dùng là `gridAdEvery(every, columns)`; hàng quảng cáo `span = { GridItemSpan(maxLineSpan) }`, rộng hết lưới. Lưới không biết số cột khi dựng danh sách nên người gọi truyền `columns` (câu hỏi 2).
- Nội dung một hàng quảng cáo (`InListAdRow(ads, slot)`, nội bộ): đọc `ads.slot(slot)`; `Loading` thì `NativeAdPlaceholder(size)`, `Loaded` thì `BoundNativeAd(state.ad, size)` của T-010, `Unavailable` thì không vẽ gì. Hai trạng thái đầu bọc `Modifier.padding(vertical = Spacing.md)` (khung "In-list ad slot", lề trên dưới 16); `Unavailable` không có lề nên hàng cao 0 dp. `size` theo `placement.format`.
- Phần dựng quảng cáo đã tải: `BoundNativeAd(ad, size, modifier)` nội bộ của T-010 (`NativeAdViewBinding.kt`), vẽ quảng cáo của SDK trong `NativeAdView`, quảng cáo giả thì vẽ `NativeAdCard`. T-011 chỉ đọc file của T-010, không sửa. Tên hay chữ ký khác thì dừng (mục 7).

**Ví dụ dùng (đưa vào `ads-guide.md`):**

```kotlin
val ads = rememberInListAds(AdPlacement.MY_LIST_NATIVE)
LazyColumn { itemsWithInListAds(photos, ads, key = { it.id }) { PhotoRow(it) } }
```

**Tài liệu:** `docs/ops/ads-guide.md` mục 3 đổi "(đợt 2)" của dòng Native in-list thành "Có", mục 5 bước 4 thêm `rememberInListAds` và `itemsWithInListAds` kèm ví dụ trên và quy tắc chọn N là bội số cột ở lưới. `CLAUDE.md` không sửa: bước 7 đã dẫn tới `ads-guide.md` cho quy chuẩn native (T-010 cập nhật bước này), nên cách dùng in-list chỉ ghi ở `ads-guide.md`, tránh hai nơi lệch nhau và tránh xung đột với T-010.

**Nơi khác đọc file plan đổi:** `grep -rln` trên `scripts/`, `.github/`, `build-logic/` và mọi `src/test`: không nơi nào đọc `AdsController.kt`, `ads-guide.md` hay `core/ads/build.gradle.kts` theo tên. Không đổi khóa Remote Config hay `AdPlacement`, nên `docs/remote-config-keys.md` và `docs/remote-config-template.json` (test `RemoteConfigKeysTest` của T-007) không đổi, không phải sinh lại. Không thêm vị trí nên `scripts/release.sh` (đọc dòng `AdPlacement`) không bị ảnh hưởng; plan không đọc hay sửa file này.

**Thứ tự commit (L-025):**

1. Commit test (bước 3): `InListAdPositionsTest.kt`, `InListAdsStateTest.kt`, `InListAdsTest.kt`. Không biên dịch được vì `inListAdPositions`, `InListAds`, `itemsWithInListAds` chưa có: đó là lý do thất bại mong đợi, ghi vào nhật ký.
2. Commit code (bước 4): `InListAdPositions.kt`, `InListAds.kt`, `AdsController.kt`.
3. Commit tài liệu: `docs/ops/ads-guide.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAdPositions.kt` | `inListAdPositions`, `gridAdEvery` (public), `inListEvery` (internal) |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | `InListAds`, `InListAdLoader`, `rememberInListAds`, `requireInListFormat`, `itemsWithInListAds` cho `LazyListScope` và `LazyGridScope` (public và bản nội bộ nhận `positions`, `adContent`), `InListAdRow`, `InListAdKey`, `InListAdContentType` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `internal fun inListEvery(placement): Flow<Int?>` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdPositionsTest.kt` | Unit test hàm thuần |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsStateTest.kt` | Unit test giữ, tải một lần, hủy quảng cáo của từng ô, log |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Test giao diện Robolectric của hàm chèn cho `LazyColumn` và `LazyVerticalGrid` |
| Sửa | `docs/ops/ads-guide.md` | Mục 3: in-list có trong `:core:ads`; mục 5 bước 4: cách dùng, ví dụ, N ở lưới |

Không có ảnh chuẩn screenshot (ảnh `AdGalleryScreen_inlist_light` thuộc T-014; T-010 đã thêm Roborazzi cho `:core:ads` nhưng T-011 không ghi ảnh). Không sửa `core/ads/build.gradle.kts`: phụ thuộc cần đã có từ T-009, T-010, `LazyGrid` có trong Compose foundation. Không sửa file của T-010. Không sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (không đổi khóa, không đổi vị trí). Không sửa `docs/STATUS.md` trong task (phiên tự chạy sinh lại trong PR báo cáo).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có.
- Quảng cáo: thêm cách đặt native trong danh sách (định dạng của spec KB-3). Không thêm vị trí vào `AdPlacement`; vị trí in-list đầu tiên là của màn hình xem thử (T-014), app khai báo vị trí của mình theo spec của app. Đọc khóa đã có `ads_inlist_every_count` (T-007), không thêm khóa.
- Sự kiện phân tích: Không chạm (ADR-0022: hiển thị và bấm lấy từ liên kết AdMob và Firebase).
- File R2: Không chạm.

## 4. Kiểm thử

Mọi test chạy trên JVM trong `:core:ads:testDebugUnitTest` (đã nằm trong `scripts/check.sh`). Test giao diện dùng `createComposeRule()`, `@RunWith(AndroidJUnit4::class)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`; test đo kích thước thêm `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. Mục dùng chữ `List item 1` đến `List item n` (chữ minh họa của test, không phải chữ hiển thị của app) và `testTag` `item_<n>`; hàng quảng cáo giả có `testTag` `inlist_ad_<slot>` và vẽ `NativeAdCard(fakeAssets, NativeAdSize.SMALL)`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3, sau mỗi N mục, theo thiết kế (N = 3, 7 mục) | Unit test | `InListAdPositionsTest.designExample_sevenItemsEveryThree_adsAfterItems3And6`: `inListAdPositions(7, 3) == listOf(3, 7)` |
| KB-3, mặc định N = 8 | Unit test | `InListAdPositionsTest.defaultEvery_twentyItems_adsAfterItems8And16`: `inListAdPositions(20, 8) == listOf(8, 17)` |
| KB-3, không bao giờ ở vị trí đầu | Unit test | `InListAdPositionsTest.neverFirst_neverLast_neverAdjacent`: với mọi `itemCount` 0..60 và `every` 1..10: không có 0, không có vị trí cuối (`itemCount + size - 1`), hai vị trí liền nhau cách nhau đúng `every + 1` |
| KB-3, ít hơn N mục thì không có | Unit test | `InListAdPositionsTest.fewerItemsThanEvery_noAds`: `inListAdPositions(7, 8)` rỗng; `zeroItems_noAds`: `inListAdPositions(0, 3)` rỗng |
| KB-3, đúng N mục (câu hỏi 1) | Unit test | `InListAdPositionsTest.exactlyEveryItems_noTrailingAd`: `inListAdPositions(8, 8)` rỗng; `inListAdPositions(9, 8) == listOf(8)` |
| Giá trị từ xa sai | Unit test | `InListAdPositionsTest.nonPositiveEvery_noAds`: `every` 0 và -1 cho rỗng; `everyOne_adBetweenEveryTwoItems`: `inListAdPositions(3, 1) == listOf(1, 3)` |
| Lưới: hàng quảng cáo không để hàng trên thiếu ô | Unit test | `InListAdPositionsTest.gridAdEvery_roundsUpToWholeRows`: `(3, 2) -> 4`, `(8, 2) -> 8`, `(8, 3) -> 9`, `(5, 1) -> 5`, `(5, 0) -> 5` |
| KB-1 đặc biệt: chưa đồng ý hay vị trí tắt thì không chiếm chỗ | Unit test | `InListAdPositionsTest.inListEvery_nullWhenAdsNotAllowedOrBadValue`: `(false, 8) -> null`, `(true, 0) -> null`, `(true, -3) -> null`, `(true, Int.MAX_VALUE + 1L) -> null`, `(true, 8) -> 8` |
| Chỉ native small hay medium | Unit test | `InListAdsStateTest.requireInListFormat_rejectsOtherFormats`: `BANNER`, `INTERSTITIAL` ném `IllegalArgumentException`; `NATIVE_SMALL`, `NATIVE_MEDIUM` qua |
| Cuộn qua lại không tải lại (ads-guide.md, mục 6) | Unit test | `InListAdsStateTest.slot_loadsOnce_reusedOnSecondCall`: `InListAdLoader` giả trả `Loaded(FakeLoadedNativeAd(sampleNativeAdAssets))`, đếm số lần gọi; `slot(0)` hai lần thì loader được gọi một lần, trạng thái từ `Loading` sang `Loaded` (dùng `StandardTestDispatcher`, `advanceUntilIdle`) |
| Mỗi ô một quảng cáo riêng | Unit test | `InListAdsStateTest.twoSlots_twoSeparateLoads`: `slot(0)`, `slot(1)` thì loader được gọi hai lần, hai trạng thái giữ hai `FakeLoadedNativeAd` khác nhau |
| Ô không có quảng cáo không tải lại trên cùng màn hình | Unit test | `InListAdsStateTest.unavailableSlot_notRetriedOnSameScreen`: loader trả `Unavailable`, gọi `slot(0)` lần nữa thì không gọi loader thêm |
| KB-1 đặc biệt: rời màn hình thì hủy | Unit test | `InListAdsStateTest.destroyAll_destroysEveryLoadedAd_andCancelsPendingLoads`: hai ô `Loaded` thì `destroyCount == 1` cho từng `FakeLoadedNativeAd`; một ô đang tải (loader treo bằng `CompletableDeferred`) thì việc tải bị hủy, quảng cáo về sau (nếu có) không được giữ |
| Log kết quả tải (ADR-0019) | Unit test | `InListAdsStateTest.slotResult_isLoggedOnceWithoutAdContent`: `RecordingAppLogger` có đúng một dòng `INFO` thẻ `Ads` chứa `slot 0` và `LOADED`, không chứa headline của quảng cáo giả |
| KB-3, thứ tự trong `LazyColumn` (N = 3, 7 mục) | Compose UI test | `InListAdsTest.lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6`: hàm nội bộ với `positions = inListAdPositions(7, 3)`; `inlist_ad_0` nằm dưới `item_3` và trên `item_4` (so `getBoundsInRoot`); cuộn tới `inlist_ad_1` (`performScrollToNode`) thì nó nằm dưới `item_6` và trên `item_7`; tổng số hàng quảng cáo là 2 |
| KB-3, ít hơn N mục thì không có, trong danh sách | Compose UI test | `InListAdsTest.lazyColumn_fewerItemsThanEvery_noAdRow`: 7 mục, N = 8: `onAllNodesWithTag("inlist_ad_0")` rỗng, `onAllNodesWithText("Ad")` rỗng |
| KB-3, nhãn "Ad" rõ trong ô chèn | Compose UI test | `InListAdsTest.adRow_showsAdLabel`: `onNode(hasText("Ad") and hasAnyAncestor(hasTestTag("inlist_ad_0"))).assertExists()`; chữ "Ad" lấy từ `R.string.ads_native_badge` của T-010, không viết cứng ngoài test |
| KB-3, không nhầm với mục: cách mục 16 dp | Compose UI test, `NATIVE` | `InListAdsTest.adRow_isSixteenDpFromItemsAboveAndBelow`: khoảng từ đáy `item_3` tới đỉnh `NativeAdCard` trong `inlist_ad_0`, và từ đáy thẻ tới đỉnh `item_4`, đều bằng 16 dp (sai số 0.5 dp) |
| Ô hết thời gian chờ thì thu lại, không để khoảng trống | Compose UI test | `InListAdsTest.unavailableSlot_takesNoHeight`: `InListAdRow` với `InListAds` có loader giả trả `Unavailable`: đỉnh `item_4` sát đáy `item_3` (khoảng 0 dp) |
| Đang tải giữ chỗ | Compose UI test | `InListAdsTest.loadingSlot_showsPlaceholderOfSmallHeight`: loader treo, hàng quảng cáo cao 72 dp + 2 x 16 dp (chiều cao giữ chỗ small của thiết kế) |
| Lưới: rộng hết hàng, sau hàng đủ ô | Compose UI test | `InListAdsTest.lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4`: `LazyVerticalGrid(GridCells.Fixed(2))`, 7 mục, `every = 3`: hàng quảng cáo nằm dưới `item_3` và `item_4`, trên `item_5`, rộng bằng lưới |
| KB-1 đặc biệt: không có `AdsController` thì không chèn | Compose UI test | `InListAdsTest.publicApi_withoutAdsController_addsNoAdRow`: `rememberInListAds(AdPlacement.SAMPLE_NATIVE_SMALL)` và `itemsWithInListAds` với 20 mục, không cung cấp `LocalAdsController`: `ads.every` là null, danh sách có đúng 20 hàng, không có chữ "Ad" |
| Khóa hàng quảng cáo không trùng khóa mục | Compose UI test | `InListAdsTest.adRowKeys_doNotClashWithItemKeys`: mục có `key = { it }` là số nguyên 0..19, N = 3: dựng không ném lỗi trùng khóa |
| KB-3 với quảng cáo thật, N đổi từ xa | Kiểm tra thủ công (sau T-014) | Trên bản staging, màn hình xem thử, phần in-list: thấy quảng cáo thử của Google sau mỗi N mục, có nhãn "Ad"; cuộn xuống rồi lên, `adb logcat -v threadtime \| grep ' VGL\.Ads'` không có dòng tải mới cho ô đã tải; rời màn hình rồi vào lại thì tải mới. Đổi `ads_inlist_every_count` (giá trị cục bộ T-008 hoặc Remote Config staging) thì lần cập nhật sau khoảng cách đổi theo |
| Đúng thiết kế `AdGalleryScreen_inlist_light` | Screenshot test (thuộc T-014) | T-014 ghi `AdGalleryScreen_inlist_light.png` và ảnh `_small`, chạy so sánh với ảnh thiết kế. T-011 không ghi ảnh |

**Ngoại lệ viết test trước:** sửa `docs/ops/ads-guide.md` chỉ là tài liệu, không có test. Phần bọc `NativeAdView` (`BoundNativeAd`) và `AdsController.loadNativeAd` là của T-010, không test lại ở đây; hai phần đó với quảng cáo thật kiểm thủ công sau T-014.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thêm cách đặt native trong danh sách), 7 (module core dùng chung `:core:ads`). Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước (risk-scoring.md, mục 3). Bảng dưới để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`), thêm API public mới (`inListAdPositions`, `rememberInListAds`, `itemsWithInListAds`), sửa `AdsController`; sửa `ads-guide.md` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, thiết kế đã duyệt; AI phải tự chọn quảng cáo có ở cuối danh sách hay không và cách làm tròn N ở lưới (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Vị trí, nhãn, khoảng cách, giữ và hủy quảng cáo có test JVM; tải quảng cáo thật và cuộn trên máy chỉ kiểm thủ công sau T-014 |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt` cũng do T-007, T-009, T-010, T-012, T-013 sửa; dựa vào phần dựng của T-010 |
| E. Độ mới của kỹ thuật | 2 | Giữ quảng cáo native theo ô ngoài vòng đời hàng của `LazyColumn` chưa có trong dự án, nhưng là cách phổ biến |
| **Tổng** | **8** | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thêm một hàm nội bộ, không đổi hàm có sẵn); `docs/ops/ads-guide.md` (T-010 cũng sửa). Đọc, không sửa: file native của T-010 (`BoundNativeAd`, `loadNativeAd`, `FakeNativeAds.kt`).
- Task khác trong sprint có thể xung đột: T-010 (tạo `NativeAdSlot`, `NativeAdState`, sửa `AdsController.kt`, `ads-guide.md`), T-012 và T-013 (sửa `AdsController.kt`, `ads-guide.md`), T-014 (dùng `rememberInListAds`, `itemsWithInListAds` cho phần in-list, ghi ảnh `AdGalleryScreen_inlist_light`), T-017 (có thể thêm `fullScreenAdShowing` vào `AdsController.kt`).
- Đề xuất thứ tự chạy: sau T-007, T-009, T-010 (phụ thuộc trực tiếp); trước T-014. Có thể chạy trước hay sau T-012, T-013; chạy liền nhau thì rebase `AdsController.kt` (các phần thêm không chồng nhau).

## 7. Điều kiện dừng riêng

- T-010 đã merge mà không có `BoundNativeAd(ad: LoadedNativeAd, size, modifier)` nội bộ hay `AdsController.loadNativeAd(placement): NativeAdState` đúng như mục 3.1: dừng, không tự sửa file của T-010 vì không có trong danh sách.
- `NativeAdState`, `NativeAdCard`, `NativeAdPlaceholder` hay `AdsSettings.INLIST_EVERY_COUNT` có tên hoặc dạng khác hợp đồng chung đến mức phải đổi API công khai của plan này.
- Cần thêm thư viện ngoài Compose foundation đã có, hay cần sửa `core/ads/build.gradle.kts` hoặc `build-logic/` để `:core:ads` chạy được test Robolectric.
- Thiết kế hay spec đọc ra cách đặt khác mục 2 (ví dụ quảng cáo đầu tiên ngay sau mục 1).

## 8. Câu hỏi cho người duyệt

1. **Danh sách đúng N mục (hoặc 2N, 3N...) có đặt quảng cáo ở cuối không?** Spec chỉ nói ít hơn N mục thì không có.
   - Đề xuất của AI: không. Quảng cáo chỉ nằm giữa hai mục của app, nên luôn có nội dung app bên dưới, không bị hiểu là "mục cuối" hay nút tải thêm. Hệ quả: đúng N mục cũng không có quảng cáo, N + 1 mục mới có một quảng cáo.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
2. **Ở lưới, N không chia hết cho số cột thì làm sao?**
   - Đề xuất của AI: làm tròn N lên bội số cột gần nhất (`gridAdEvery`), người gọi truyền số cột. Ví dụ lưới 2 cột, N = 3 thành 4. Cách khác: giữ đúng N, chấp nhận hàng trên quảng cáo thiếu ô.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
3. **Giữ quảng cáo đã tải của mọi ô tới khi rời màn hình, không giới hạn số ô?**
   - Đề xuất của AI: có. Chỉ ô người dùng đã cuộn tới mới tải, nên số quảng cáo giữ bằng số ô đã thấy; danh sách rất dài mới cần giới hạn, khi đó thêm ở ticket sau.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
4. **T-010 cần có một hàm nội bộ vẽ một `NativeAd` đã tải (tách khỏi `NativeAdSlot`) để T-011 dùng lại.**
   - Đề xuất của AI: khi duyệt plan T-010, xác nhận plan đó có hàm này. Nếu không, plan T-011 thêm file native của T-010 vào danh sách sửa trước khi duyệt.
   - Rà soát độc lập (2026-10-23): đã khớp. Plan T-010 có `BoundNativeAd(ad, size, modifier)` nội bộ và `AdsController.loadNativeAd(placement)` (dựa trên `loadOneNativeAd`) cho T-011; T-011 đổi tên giao diện tải của mình thành `InListAdLoader` để không trùng `NativeAdLoader` của T-010.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **Có đặt mức N tối thiểu không (ví dụ không dưới 3)?** Hiện N = 1 trên Firebase nghĩa là quảng cáo xen giữa mọi mục.
   - Đề xuất của AI: không chặn trong code (giá trị là quyết định của người vận hành, ADR-0023); ghi khuyến nghị N ít nhất 5 trong `ads-guide.md`. Nếu muốn chặn cứng thì `inListEvery` trả null khi N nhỏ hơn mức tối thiểu.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện một dev đảm nhận cả hai vai)
- **Ghi chú:** anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), sau rà soát độc lập của AI; AI ghi mục này theo lời anh. Câu hỏi ở mục 8 đã có trả lời. Cần PR #81 (`release.sh`, `check.sh`) merge trước phiên chạy.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
