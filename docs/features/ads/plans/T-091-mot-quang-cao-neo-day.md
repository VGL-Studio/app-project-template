---
ticket: T-091
title: Mỗi màn tối đa một quảng cáo neo đáy
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 22.2M        # cỡ M, số gợi ý 22.2M của scripts/token-usage.py estimate (2026-11-17, trung vị 38 task M); không chỉnh
blocking_points: [ads, analytics, core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-091: MỖI MÀN TỐI ĐA MỘT QUẢNG CÁO NEO ĐÁY

## 1. Tóm tắt

`:core:ads` thêm một phạm vi màn ghi các quảng cáo neo đáy đang đặt (thanh đáy `BottomAdBarScaffold`, banner cố định và banner thu gọn của `AdSlot`) theo thứ tự vào giao diện, và một luật lượt thuần: cái đầu tiên được phép chạy giữ lượt tới khi rời giao diện; cái khác không dựng khe (không nạp, không chiếm chỗ, không ghi số liệu) và ghi một dòng cảnh báo thẻ `VGL.Ads`. Phạm vi mặc định theo `LocalLifecycleOwner` (mỗi đích của `NavHost` là một `NavBackStackEntry` riêng), nên vỏ app không đổi; màn xem thử staging cấp một phạm vi cho từng trang bằng composable mới `AnchoredAdScreen`, và trang "Banners" tách banner thu gọn sang một trang riêng. Kiểm bằng test JVM của luật lượt và test nối với `AdsController` thật đếm `AdView`, yêu cầu native và sự kiện.

Chạy thử trên commit `37ef825` (mục 4.1): hai `AdSlot` neo đáy trên một màn hôm nay cùng dựng `AdView` (2 cái) và cùng chiếm chỗ (64 dp và 128 dp).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-35 | Tiêu chí 5: một màn đặt hai quảng cáo neo đáy thì cái vào giao diện trước trong lần vào màn đó chạy; cái sau không nạp, không chiếm chỗ, có một dòng log cảnh báo | "Màn" là một `LifecycleOwner` của Compose (đích điều hướng; tab và trang của pager trong đích dùng chung owner nên tính chung), hoặc một `AnchoredAdScreen` khi app cấp rõ. "Vào giao diện trước" là thứ tự composable được dựng (số thứ tự cấp lúc `remember`), nên thanh đáy bọc nội dung đi trước `AdSlot` nằm trong nội dung. Một thanh đáy là một quảng cáo neo đáy dù khe là native hay banner; lớp native thu gọn thuộc thanh đáy, không tính riêng. MREC và native trong nội dung không phải neo đáy |
| KB-35 | Tiêu chí 5: cái đang chạy rời giao diện thì cái đang chờ kế tiếp được chạy | Lượt chỉ đổi khi người giữ rời phạm vi (rời cây giao diện): người giữ mới là cái đầu tiên theo thứ tự còn trong phạm vi và đang được phép |
| KB-35 | Tiêu chí 5: cái không được phép chạy (vị trí tắt, không có mã đơn vị, banner thu gọn bị chặn theo tiêu chí 4) không giữ lượt | "Được phép" của thanh đáy: `bottomAdBarLayers(...)` khác `null` (đồng ý, `is_show_<mã>`, B-06). Của `AdSlot` neo đáy: cổng đồng ý mở, vị trí bật, có mã đơn vị của định dạng. Thành phần chưa biết giá trị đầu của khóa thì chưa tính (chờ), để cái sau không giành lượt trước khi khóa của cái trước về. Chặn theo tiêu chí 4 (MC-13) là T-092: plan để sẵn tham số "được phép" để T-092 nối vào |
| KB-35 | Tiêu chí 5: khóa đổi lúc màn đang mở không đổi lượt tới lần vào màn sau; cái đang chạy bị tắt thì thôi hiện, cái đang chờ chưa được chạy | Khi đã có người giữ, đổi "được phép" của ai cũng không đổi người giữ. Người giữ bị tắt thì tự thôi hiện theo luật sẵn có của nó (thanh đáy tắt, banner không còn khung) mà vẫn giữ lượt. Khi chưa ai giữ (mọi cái đều chưa được phép lúc vào màn, ví dụ chưa có đồng ý), cái đầu tiên trở thành được phép thì nhận lượt: giữ hành vi của KB-7 tiêu chí 2 cho màn chỉ có một quảng cáo neo đáy (câu hỏi 1, mục 8) |
| KB-35 | Tiêu chí 6: lượt bị chặn không ghi số liệu | Cái đang chờ không dựng khe nào (không `AdView`, không luồng native, không `af_native_ad_eligible`, không `ad_load_*`). Cái chờ được lượt sau đó là một cơ hội mới, ghi như khi vào màn |

Luật: `docs/ops/ads-business-rules.md` MC-08, MC-12, MC-14, B-01, B-03, B-04, B-06, D-01, D-04.

Thiết kế tham chiếu: không có thiết kế riêng (docs/ops/design-workflow.md). Trang "Banners" và trang mới của màn xem thử thuộc nhóm "More formats" của T-036, không có khung thiết kế; ảnh thiết kế `AdGalleryScreen_default_*`, `AdGalleryScreen_collapsible-*`, `AdGalleryScreen_inlist_light` không đổi.

ADR liên quan: ADR-0045 (mục 1.2: chỗ khác PARTNER, PARTNER không có MC-12), ADR-0019 (log), ADR-0001 (`:core:ads` không phụ thuộc `:app`), ADR-0031, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

**Luật lượt (hàm thuần, L-072).** File mới `AnchoredAds.kt` trong `:core:ads`:

- `internal data class AnchoredAdMember(val seq: Int, val placement: AdPlacement, val allowed: Boolean?)`: `allowed` là `null` khi chưa biết giá trị đầu.
- `internal fun anchoredAdHolder(members: List<AnchoredAdMember>, holder: Int?): Int?`: `holder` còn trong `members` thì giữ nguyên; không thì duyệt `members` theo `seq` tăng: gặp `allowed == null` trả `null` (chờ, không cho cái sau giành lượt), gặp `true` trả `seq` của nó, gặp `false` đi tiếp; hết danh sách trả `null`.
- `internal fun anchoredAdWaitLog(waiting: AdPlacement, holder: AdPlacement): String` = `"Anchored ad ${waiting.key} waits: ${holder.key} holds the anchored ad of this screen"` và `anchoredAdTurnLog(placement)` = `"Anchored ad ${placement.key} takes the turn"`.

**Phạm vi màn.**

- `internal class AnchoredAdScope(logger: AppLogger)`: danh sách thành viên, số thứ tự tăng dần (`join(placement): Int`), `report(seq, allowed: Boolean?)`, `leave(seq)`, và `holder` là `mutableStateOf<Int?>` (Compose đọc được), tính lại bằng `anchoredAdHolder` sau mỗi lần `join`, `report`, `leave`. Mỗi lần tính lại: người giữ đổi thì ghi `info` dòng `anchoredAdTurnLog`; thành viên `allowed == true` mà không giữ lượt trong khi có người giữ thì ghi `warn` dòng `anchoredAdWaitLog` một lần cho mỗi lần chờ (cờ trên thành viên, xóa khi nó nhận lượt). Chỉ luồng chính.
- `internal class AnchoredAdScopes(logger)` trong `AdsController` (trường `internal val anchoredScopes`): `scopeFor(owner: LifecycleOwner): AnchoredAdScope`, giữ trong một map theo owner, bỏ owner khi phạm vi không còn thành viên nào. Trong `NavHost`, `LocalLifecycleOwner` của mỗi đích là `NavBackStackEntry` của nó, nên hai đích (kể cả lúc chuyển cảnh, hai đích cùng dựng) không chung phạm vi; tab và trang của pager trong một đích chung owner nên tính chung (đúng định nghĩa "màn" của KB-35).
- `internal val LocalAnchoredAdScope = staticCompositionLocalOf<AnchoredAdScope?> { null }` và composable công khai `fun AnchoredAdScreen(content: @Composable () -> Unit)`: cấp một phạm vi mới (`remember { AnchoredAdScope(logger) }`, logger của `LocalAdsController`, không có controller thì chỉ vẽ `content`). KDoc: dùng cho màn không phải một đích của `NavHost` (trang của màn xem thử, màn đổi nội dung trong cùng một đích mà muốn tính là màn khác).
- `@Composable internal fun rememberAnchoredAdTurn(ads: AdsController, placement: AdPlacement, allowed: Boolean?): Boolean`: phạm vi là `LocalAnchoredAdScope.current ?: ads.anchoredScopes.scopeFor(LocalLifecycleOwner.current)`; thành viên là một `RememberObserver` trong `remember(scope)`: `onRemembered` gọi `join` (Compose gọi `onForgotten` của phần bị bỏ trước `onRemembered` của phần mới, và `onRemembered` theo thứ tự composition, nên số thứ tự theo thứ tự vào giao diện, và màn A đổi sang màn B trong cùng một phạm vi thì A rời trước khi B vào, B không phải chờ); `onForgotten` gọi `leave`; `onAbandoned` không làm gì (chưa vào). `SideEffect { scope.report(seq, allowed) }`; trả `scope.holder == seq`, nên khung hình đầu của mỗi thành viên chưa có lượt (không vẽ gì), khung hình kế mới dựng khe: trễ một khung hình, như hôm nay khe đã chờ giá trị đầu của `enabled`. `AnchoredAdScreen` chỉ là `CompositionLocalProvider`, không thêm nút bố cục nào.
- Lần vào màn: khi mọi thành viên rời (rời đích, chuyển trang, dựng lại activity khi xoay), phạm vi rỗng, lần dựng sau là lần vào màn mới, thứ tự và lượt tính lại từ đầu. Không giữ gì qua `ViewModel` hay `SavedState`, nên vỏ app không có trạng thái mới.
- Không có `LocalAdsController` (test, preview): thanh đáy và `AdSlot` đã không vẽ gì từ trước, không gọi `rememberAnchoredAdTurn`.

**Thanh đáy.** `rememberBottomAdBarState` (trong `BottomAdBarScaffold`): `allowed` là `null` tới khi `enabled` có giá trị đầu (`collectAsStateWithLifecycle(initialValue = null)` thay cho `false`), sau đó là `layers != null` của `rememberBottomAdBarLayers`. `turn = rememberAnchoredAdTurn(ads, placement, allowed)`. `!turn` thì `BottomAdBarChoice(layers = null, ...)`: thanh không dựng khe, không dựng lớp, khoảng chừa 0 (`bottomBarReservation` của `kind == null`), nội dung không dựng lại (giữ luật "Turning the bar on or off never rebuilds content"). Lớp native thu gọn và D-07 của T-090 nằm trong thanh nên theo luôn.

**`AdSlot`.** `BannerSlot` với `!inBottomBar` và `format` là `BANNER` hay `BANNER_COLLAPSIBLE` (hàm thuần `isAnchoredFormat(format)`; `BANNER_MREC` không): `enabled` lấy với giá trị đầu `null`; `allowed = enabled?.let { canRequestAds && it && hasUnit }`, `hasUnit` là có mã của nền tảng đầu trong `platforms` (AdMob: `admobUnit != null`; MAX: `maxUnit != null`), cùng điều kiện B-04 mà `BannerSlotHost` dùng. `!rememberAnchoredAdTurn(...)` thì vẽ `Box(modifier)` rỗng và `return` trước `BoxWithConstraints`, nên không có `BannerSlotHost`, `AdView`, khung hay khoảng chiếm. Banner của thanh đáy (`inBottomBar`) đi theo lượt của thanh.

**Màn xem thử staging.** `AdGalleryScreen` bọc trang đang hiện trong `key(uiState::class) { AnchoredAdScreen { ... } }`: mỗi trang một màn. Trang "Banners" giữ banner cố định, MREC và danh sách; banner thu gọn chuyển sang trang mới `AdGalleryUiState.CollapsibleBanner(itemCount)` (tiêu đề "Collapsible banner", danh sách 30 dòng, banner thu gọn ở đáy như hôm nay), mở bằng nút mới ngay sau nút "Banners" trong nhóm "More formats" (`AdGalleryTestTags.COLLAPSIBLE_BANNER_BUTTON`, `COLLAPSIBLE_BANNER`, `COLLAPSIBLE_BANNER_LIST`), `AdGalleryViewModel.openCollapsibleBanner()` ghi `page=collapsible_banner`. Trang "Collapsible" (thanh đáy) không đổi.

**Tài liệu.** `docs/ops/ads-guide.md`: mục 5 (thêm vị trí) thêm luật "mỗi màn tối đa một quảng cáo neo đáy" (MC-12), cái sau bị bỏ, `AnchoredAdScreen` cho màn không phải đích điều hướng, và app đang đặt hai cái sẽ mất một cái sau khi đồng bộ; mục 8 (thử trên staging) thêm bước thử trang "Banners", "Collapsible banner" và dòng log chờ.

Không đổi `AdPlacement`, khóa Remote Config, `docs/ops/ads-business-rules.md` (MC-12 đã có), `scripts/ads-plan.py`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AnchoredAds.kt` | `AnchoredAdMember`, `anchoredAdHolder`, hai hàm log, `AnchoredAdScope`, `AnchoredAdScopes`, `LocalAnchoredAdScope`, `AnchoredAdScreen`, `rememberAnchoredAdTurn`, `isAnchoredFormat` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BottomAdBar.kt` | `rememberBottomAdBarState`: `enabled` giá trị đầu `null`, gọi `rememberAnchoredAdTurn`, không có lượt thì `layers = null`; KDoc của `BottomAdBarScaffold` thêm MC-12 |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerSlot`: lượt cho banner cố định và banner thu gọn ngoài thanh đáy; KDoc của `AdSlot` thêm MC-12 |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Trường `internal val anchoredScopes = AnchoredAdScopes(logger)` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AnchoredAdTurnTest.kt` | Test thuần của `anchoredAdHolder`, `AnchoredAdScope` (log), `isAnchoredFormat` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AnchoredAdWiringTest.kt` | Test nối với `AdsController` thật: hai `AdSlot`, thanh đáy và `AdSlot`, rời giao diện, khóa đổi, hai màn, số liệu |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `AnchoredAdScreen` cho mỗi trang; trang "Banners" bỏ banner thu gọn; trang mới `CollapsibleBannerPage`; nút mới; tag mới; nối `onOpenCollapsibleBanner` trong route |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryUiState.kt` | `data class CollapsibleBanner(val itemCount: Int)`; KDoc của `Banners` bỏ banner thu gọn |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModel.kt` | `openCollapsibleBanner()` |
| Sửa | `app/src/staging/res/values/strings.xml` | `ad_gallery_open_collapsible_banner`, `ad_gallery_collapsible_banner_title` (`translatable="false"`, như các chuỗi của T-036) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Viết lại ba test banners theo mục 3.5; thêm test trang mới, nút mới, phạm vi từng trang |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenScreenshotTest.kt` | Hai ảnh mới của trang "Collapsible banner" |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModelTest.kt` | Thêm `openCollapsibleBanner` vào bảng mở trang |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_collapsible-banner_light.png` | Ảnh chuẩn mới |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_collapsible-banner_small.png` | Ảnh chuẩn mới, khung `w360dp-h640dp-xxhdpi` |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_banners_light.png` | Ghi lại: không còn banner thu gọn ở đáy |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_banners_small.png` | Ghi lại, như trên |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_default-more_light.png` | Ghi lại: thêm nút "Collapsible banner" trong nhóm "More formats" |
| Sửa | `docs/ops/ads-guide.md` | Mục 5 và mục 8 như mục 3.1 |

Nơi đọc ảnh chuẩn: `grep -rn "AdGalleryScreen_banners\|AdGalleryScreen_default-more" scripts/ .github/ */src/test` chỉ thấy `AdGalleryScreenScreenshotTest.kt`; các ảnh này không có ảnh thiết kế cùng tên nên `scripts/design-compare.sh` không đọc. `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` chỉ nhắc `ads-guide.md` trong một chú thích (vị trí `firstOpenOnly`), không đọc file, nên không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm vị trí. Đổi cách hiện của mọi thanh đáy (`BottomAdBarScaffold`) và banner cố định, banner thu gọn (`AdSlot`) khi một màn có hai cái; màn chỉ có một cái giữ nguyên. Màn xem thử dùng lại `GALLERY_BANNER`, `GALLERY_BANNER_COLLAPSIBLE`, `GALLERY_BANNER_MREC`, `GALLERY_NATIVE_SMALL`.
- Sự kiện phân tích: không thêm sự kiện. Cái bị chặn không ghi sự kiện nào (MC-14).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `LocalLifecycleOwner` từ `androidx.lifecycle.compose` (`libs.androidx.lifecycle.runtime.compose`, đã có ở `:core:ads`, `AdSlot.kt` đang dùng); `RememberObserver`, `staticCompositionLocalOf` của Compose runtime đã có.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** khóa phạm vi theo `LocalLifecycleOwner`: một app đặt quảng cáo neo đáy ngoài `NavHost` (vỏ app) thì owner là activity, chung phạm vi với mọi đích; hay thứ tự `remember` không đúng thứ tự vào giao diện ở một bố cục nào đó (`SubcomposeLayout` dựng con sau cha).
- **Điều kiện chuyển:** test `navHost_twoDestinations_eachOwnScope` hay `bar_thenBannerInContent_barRuns` của mục 4 vẫn thất bại vì phạm vi hay thứ tự sau 2 lần sửa.
- **Cách làm thay:** số thứ tự cấp lúc thành viên báo `allowed` lần đầu trong `SideEffect` (thứ tự áp dụng composition) thay vì lúc `remember`; phạm vi lấy từ `LocalViewModelStoreOwner` thay `LocalLifecycleOwner` (cùng là `NavBackStackEntry` trong `NavHost`; `androidx.lifecycle.viewmodel.compose` đã đi theo `activity-compose`, kiểm có trên classpath biên dịch của `:core:ads` trước khi dùng, không có thì dừng). Cùng file `AnchoredAds.kt` (dự phòng); tiêu chí và test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (chạy 2026-11-17 trên `37ef825`): 8 test "Cần xem trước", 62 test "Nên xem". Giữ nguyên, lý do theo nhóm (đã đọc tên và chỗ dựng, chưa đọc hết thân từng test, câu hỏi 5):

- Dựng một quảng cáo neo đáy mỗi màn, nên luôn giữ lượt: `AdsControllerNativeStockTest` (`configurationChange_layerGoes_adDestroyed_logged`, `bottomSlot_collapOff_entersAfterPreload_showsStockAd`, `collapse_slotTakesStock_eligibleOnce`, `landscape_layerNoRoom_stockKept_noEligible_logged`, `largeText_layerNoRoom_fontScale1_layerShown`, `largeText_layerNoRoom_stockKept_noEligible_logged`, `layer_entersAfterPreload_showsStockAd`), `AdsControllerReloadIntervalTest.collapsibleLayer_refreshesAtCollapKey_staysCollapsed`, `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob` (gọi thẳng `BottomBannerSlot`, không qua lượt), `BottomBarSlotWiringTest`, `BottomSlotYieldWiringTest`. Các test này chờ `idle` sau khi dựng, nên trễ một khung hình của lượt không đổi câu khẳng định.
- `AdsControllerNativeStockTest.leaveScreen_stockKept_nextScreenGetsIt` đổi `SCREEN_A` (thanh `SAMPLE_NATIVE_SMALL`) sang `SCREEN_B` (thanh `GALLERY_NATIVE_SMALL`) trong một `when` cùng owner: A rời trước khi B vào (mục 3.1), B nhận lượt, câu khẳng định giữ. Test mới `switchInSameScope_noWaitLog` (mục 4) khóa điều này.
- Dựng `BottomAdBarHost`, `BottomAdBarLayout` hay hàm thuần (`bottomBarReservation`), không qua `BottomAdBarScaffold`: `BottomAdBarTest` (cả `nativeReservation_followsFontScale`, `reservedSlot_equalsDrawnSlotHeight`, `reservation_*`), `NativeAdSlotTest.bottomNativeSlot_unavailable_takesNoSpace_reportsFalse`. `BottomAdBarTest.withoutAdsController_drawsContentOnly_noPadding`, `scaffold_rejectsNonNativeSmallPlacement`: không có controller hay ném lỗi trước lượt, giữ.
- Luật nạp banner và log, dưới `BannerSlotHost` (không đổi): `BannerLoadsTest`, `BannerSlotHostTest`, `PlatformBannersTest`, `MaxBannerAdViewTest`. Quét mã nguồn: `AdNetworkLogWiringTest` (năm test), `NativeEligibleCoverageTest.everyFileDrawingANativeAd_reportsEligible` (`AnchoredAds.kt` không vẽ native), `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags` (trường mới không đọc Remote Config).
- Chỉ dùng tên `AdsController` cho phần toàn màn hình, rewarded, combo, pool, bước của luồng: `AdsControllerNativeComboTest`, `AdsControllerResumeTest`, `AdsSdkStartTest`, `FullScreenAdGuardTest`, `FullScreenAdsTest`, `FullScreenPreloadSlotTest`, `InListAdsTest.positions_followEvery`, `NativeStepAdAllowedTest`, `NativeStepNextAdReadyTest`, `RewardedActionTest`, `RewardedAdRequiredTest`, `RewardedTurnsTest`.
- `AdGalleryScreenTest.flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard` và các test trang khác của màn xem thử: `AnchoredAdScreen` không thêm nút bố cục, không có controller thì chỉ vẽ nội dung, nên vị trí không đổi.

`rememberBottomAdBarState` đổi giá trị đầu của `enabled` từ `false` sang `null` (lượt chưa tính); thanh vẫn không dựng gì trước giá trị đầu như hôm nay. Hành vi đổi có test khẳng định:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Trang "Banners" có banner cố định trên, banner thu gọn dưới | Banner thu gọn chuyển sang trang "Collapsible banner" | `AdGalleryScreenTest.banners_fixedOnTop_collapsibleAtBottom` | Viết lại tại chỗ thành `banners_fixedOnTop_noCollapsible`: banner dưới tiêu đề 24 dp, rộng hết màn; không có node `PreviewAdTags.COLLAPSIBLE_BANNER`; danh sách dưới banner, đáy danh sách ở 891 dp (đáy màn); cuộn tới "List item 30" thì đáy dòng đó không quá 891 dp. Phần banner thu gọn chuyển sang test mới `collapsibleBanner_atBottom_lastItemNotCovered` (mục 4) với các câu cũ: banner thu gọn ở đáy 891 dp, danh sách trên nó, dòng 30 không bị che |
| Như trên, khung thấp | Như trên | `AdGalleryScreenTest.banners_small_listStillReachable` | Giữ tên; câu "last item not covered by the collapsible banner" đổi thành đáy "List item 30" sau khi cuộn không quá 640 dp (đáy khung); câu MREC giữ. Câu cũ về banner thu gọn chuyển sang `collapsibleBanner_small_listStillReachable` (mục 4) |
| Như trên | MREC giữ chỗ | `AdGalleryScreenTest.banners_mrecCenteredBetweenBannerAndList` | Giữ nguyên |
| Ảnh trang "Banners" có banner thu gọn | Không còn | `AdGalleryScreenScreenshotTest.banners_light`, `banners_small` | Giữ test, ghi lại ảnh (mục 3.2) |
| Nhóm "More formats" có 7 nút | Có 8 nút, "Collapsible banner" sau "Banners" | `AdGalleryScreenTest` (danh sách nút ở dòng 348, bảng chữ ở dòng 400), `AdGalleryScreenScreenshotTest.default_more_light` (ảnh `AdGalleryScreen_default-more_light.png`) | Thêm dữ liệu cho nút mới vào danh sách và bảng chữ (ADR-0029, dòng mới của plan); ghi lại ảnh |
| Bảng mở trang của ViewModel | Thêm trang mới | `AdGalleryViewModelTest` (bảng ở dòng 121, 139) | Thêm dòng `Triple(viewModel::openCollapsibleBanner, AdGalleryUiState.CollapsibleBanner(30), "page=collapsible_banner")` (dữ liệu mới, ADR-0029) |

## 4. Kiểm thử

Điểm nối test dùng (đã có trên nhánh chính): `testAdsController(flags = DefaultAdsFlags(settings), logger = RecordingAppLogger(), metrics = RecordingAdMetrics(), funnel = RecordingAdFunnel())`, `openConsentGateForTest()`, `admobReadyForTest()`, `nativeLoaderForTest = FakeNativeAdLoader()`. Banner đếm bằng `AdView` trong `decorView` của activity (cách của `BottomBarSlotWiringTest.adViews`), chiếm chỗ đo bằng `getBoundsInRoot` của node bọc `AdSlot` có `testTag`; native đếm bằng `loader.requests`; số liệu đọc từ `RecordingAdMetrics`, `RecordingAdFunnel` theo `placement.key`; log từ `RecordingAppLogger` mức `warn`, thẻ `Ads`. Màn chuyển bằng một `mutableStateOf` trong `setContent` (bỏ `AdSlot` ra khỏi cây). Placement dùng: `GALLERY_BANNER`, `GALLERY_BANNER_COLLAPSIBLE`, `SAMPLE_NATIVE_SMALL` (thanh đáy), khóa `is_show_<mã>` bật qua `FakeRemoteSettings`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-35 tiêu chí 5: luật lượt | Unit test | `AnchoredAdTurnTest.holder_firstAllowedInOrder` (`[1:true, 2:true]` → 1); `holder_skipsNotAllowed` (`[1:false, 2:true]` → 2); `holder_waitsForUnknownBefore` (`[1:null, 2:true]` → `null`); `holder_keptWhileInMembers_evenWhenOff` (holder 1, `[1:false, 2:true]` → 1); `holder_gone_nextAllowedTakes` (holder 1, `[2:true, 3:true]` → 2); `holder_none_firstThatTurnsOnTakes` (`[1:false, 2:false]` → `null`, rồi `[1:false, 2:true]` → 2); `isAnchoredFormat`: `BANNER`, `BANNER_COLLAPSIBLE` đúng, `BANNER_MREC`, `NATIVE_SMALL` sai |
| KB-35 tiêu chí 5: dòng cảnh báo | Unit test | `AnchoredAdTurnTest.scope_waitingLogsOnce`: `join` hai thành viên, `report` cả hai `true`, `report` lại lần nữa: đúng một dòng `warn` thẻ `Ads` bằng `anchoredAdWaitLog(GALLERY_BANNER_COLLAPSIBLE, GALLERY_BANNER)`; `leave` người giữ: một dòng `info` `anchoredAdTurnLog(GALLERY_BANNER_COLLAPSIBLE)` |
| KB-35 tiêu chí 5: banner cố định rồi banner thu gọn | Unit test (Robolectric, `AdsController` thật) | `AnchoredAdWiringTest.twoBanners_secondNoViewNoSpace_warns`: `Column { AdSlot(GALLERY_BANNER, tag "fixed"); AdSlot(GALLERY_BANNER_COLLAPSIBLE, tag "collapsible") }`, khung `w411dp-h891dp-xxhdpi`: 1 `AdView` (hôm nay 2, mục 4.1), "fixed" cao 64 dp, "collapsible" cao 0 dp (hôm nay 128 dp), một dòng `warn` như trên. Đối chứng thứ tự: đảo hai dòng thì banner thu gọn cao 128 dp, banner cố định 0 dp |
| KB-35 tiêu chí 5: thanh đáy rồi banner thu gọn | Unit test (Robolectric) | `AnchoredAdWiringTest.bar_thenBannerInContent_barRuns`: `BottomAdBarScaffold(SAMPLE_NATIVE_SMALL) { AdSlot(GALLERY_BANNER_COLLAPSIBLE, tag) }`, native loader giả: thanh gửi yêu cầu (`loader.requests` khác rỗng), khe chờ hiện (`NativeAdTestTags.PLACEHOLDER` 1 node), 0 `AdView`, banner cao 0 dp, một dòng `warn` |
| KB-35 tiêu chí 5: cái trước rời giao diện thì cái sau chạy | Unit test (Robolectric) | `AnchoredAdWiringTest.holderLeaves_waitingRuns`: hai banner như trên, `showFixed = false`: banner thu gọn có 1 `AdView`, cao 128 dp, dòng `info` "takes the turn". `AnchoredAdWiringTest.barLeaves_bannerRuns`: thanh đáy bỏ khỏi cây (nội dung giữ `AdSlot`), banner thu gọn dựng `AdView` |
| KB-35 tiêu chí 5: cái vào trước tắt hay không có mã thì cái sau chạy | Unit test (Robolectric) | `AnchoredAdWiringTest.firstOff_secondRuns`: `is_show_gallery_banner` tắt: banner thu gọn có `AdView`, không dòng `warn`. `AnchoredAdWiringTest.firstNoUnit_secondRuns`: `unitIds` giả không có mã của `GALLERY_BANNER` (cách của test B-04 có sẵn), banner thu gọn có `AdView`. `AnchoredAdWiringTest.barOff_bannerRuns`: thanh đáy với `is_show_sample_native_small` tắt, banner thu gọn chạy |
| KB-35 tiêu chí 5: khóa đổi lúc màn đang mở | Unit test (Robolectric) | `AnchoredAdWiringTest.holderTurnedOff_staysHolder_waitingStillWaits`: hai banner, tắt `is_show_gallery_banner` lúc màn mở: 0 `AdView` của banner cố định, banner thu gọn vẫn 0 `AdView`, cao 0. `AnchoredAdWiringTest.firstTurnedOnLater_noTurnChange`: `is_show_gallery_banner` tắt lúc vào màn, banner thu gọn chạy; bật lại: banner cố định cao 0, không có `AdView` mới (vẫn 1). Lần vào màn sau (bỏ cả hai khỏi cây rồi dựng lại): banner cố định chạy, banner thu gọn chờ |
| KB-35 tiêu chí 5: chưa ai giữ lượt (chưa đồng ý lúc vào màn) | Unit test (Robolectric) | `AnchoredAdWiringTest.consentLater_firstInOrderRuns`: cổng đồng ý đóng lúc vào màn, hai banner: 0 `AdView`; `openConsentGateForTest()`: chỉ banner cố định có `AdView` (câu hỏi 1) |
| KB-35 tiêu chí 5: hai màn khác nhau | Unit test (Robolectric) | `AnchoredAdWiringTest.twoOwners_eachOwnTurn`: hai `Box` mỗi cái `CompositionLocalProvider(LocalLifecycleOwner provides <owner riêng, LifecycleRegistry RESUMED>)`, mỗi cái một `AdSlot` neo đáy: 2 `AdView`, không dòng `warn`. `AnchoredAdWiringTest.anchoredAdScreen_ownScope`: cùng owner, hai `AnchoredAdScreen` mỗi cái một banner: 2 `AdView`. Đối chứng: bỏ hai `AnchoredAdScreen` thì 1 `AdView` |
| KB-35 tiêu chí 5: đích của `NavHost` | Unit test (Robolectric, `:app`) | `AdGalleryScreenTest` không có `NavHost`; phần đích kiểm bằng `twoOwners_eachOwnTurn` (mỗi đích có `LocalLifecycleOwner` riêng là hành vi của `NavHost`) và bước máy thật dưới. `:core:ads` không có thư viện navigation, không thêm (câu hỏi 2) |
| KB-35 tiêu chí 6: không ghi số liệu cho lượt bị chặn | Unit test (Robolectric) | `AnchoredAdWiringTest.waiting_recordsNothing`: hai banner, `admobReadyForTest()`: `RecordingAdMetrics` và `RecordingAdFunnel` không có sự kiện nào mang `gallery_banner_collapsible`. `AnchoredAdWiringTest.waitingBar_noEligibleNoRequest`: `AdSlot(GALLERY_BANNER)` rồi thanh đáy trong nội dung: `loader.requests` rỗng, không `af_native_ad_eligible` của `sample_native_small` |
| Đổi màn trong cùng một phạm vi | Unit test (Robolectric) | `AnchoredAdWiringTest.switchInSameScope_noWaitLog`: `when (screen) { A -> AdSlot(GALLERY_BANNER); B -> AdSlot(GALLERY_BANNER_COLLAPSIBLE) }` cùng owner, đổi A sang B: B có `AdView`, không có dòng `warn` nào |
| Thứ tự trong một lần vào màn | Unit test (Robolectric) | Như `bar_thenBannerInContent_barRuns`, và `bannerAboveBar_bannerRuns`: `Column { AdSlot(GALLERY_BANNER); BottomAdBarScaffold(...) {} }`: banner chạy, thanh không gửi yêu cầu, không khe chờ |
| Dựng lại activity | Unit test (Robolectric) | `AnchoredAdWiringTest.recreate_newEntry_firstRunsAgain`: `ActivityScenario` của `ComponentActivity`, hai banner, `recreate()`: vẫn 1 `AdView` của banner cố định, banner thu gọn cao 0 (phạm vi cũ đã rỗng, không giữ người giữ cũ) |
| Màn xem thử: mỗi trang một phạm vi | Compose UI test | `AdGalleryScreenTest.pages_eachOwnAnchoredScope`: `AdGallerySlots` giả, `Banner` của nó đọc `LocalAnchoredAdScope.current` (tham chiếu nội bộ không thấy từ `:app`, nên test so `currentCompositeKeyHash` hay đếm lần dựng của nội dung trang): đổi `Banners` sang `CollapsibleBanner` thì nội dung trang dựng mới. Nếu không viết được từ `:app`, thay bằng `AnchoredAdWiringTest.anchoredAdScreen_ownScope` và bước máy thật (câu hỏi 3) |
| Trang "Collapsible banner" | Compose UI test | `AdGalleryScreenTest.collapsibleBanner_atBottom_lastItemNotCovered` (câu của test cũ, mục 3.5); `AdGalleryScreenTest.collapsibleBanner_small_listStillReachable` (`w360dp-h640dp-xxhdpi`); nút mới có chữ "Collapsible banner", bấm gọi `onOpenCollapsibleBanner` một lần |
| Trang "Banners" | Compose UI test | `banners_fixedOnTop_noCollapsible`, `banners_small_listStillReachable` (mục 3.5) |
| Giao diện trang mới | Screenshot test | `AdGalleryScreenScreenshotTest.collapsibleBanner_light` → `"src/test/screenshots/AdGalleryScreen_collapsible-banner_light.png"` |
| Màn hình thấp | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `AdGalleryScreenScreenshotTest.collapsibleBanner_small` → `"src/test/screenshots/AdGalleryScreen_collapsible-banner_small.png"` |
| Ảnh ghi lại | Screenshot test | `./gradlew :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/*'`: chỉ đổi hay thêm năm ảnh của mục 3.2; ảnh khác trả bằng `git checkout -- <ảnh>` |
| Ảnh thiết kế không đổi ở ticket này | Không chạy | Ảnh thiết kế của ticket khác (T-109, T-110): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light_unfolded`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`. Ảnh thiết kế của ticket khác (T-013, T-053): `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |
| Hai quảng cáo neo đáy trên một màn, quảng cáo thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging: trang "Banners" có banner cố định và MREC; trang "Collapsible banner" có banner thu gọn bung từ đáy; trang "Collapsible" có thanh đáy và lớp thu gọn như trước. `adb logcat -v threadtime \| grep ' VGL\.Ads'` không có dòng "waits" trên ba trang (mỗi trang một cái). Màn mẫu (`feature:sample`) giữ thanh đáy |
| Tài liệu | Kiểm tra thủ công (người: đọc tài liệu) | Đọc diff của `docs/ops/ads-guide.md` mục 5 và 8 |

**Ngoại lệ viết test trước:** chuỗi mới trong `strings.xml` và đoạn của `docs/ops/ads-guide.md` (tài nguyên, tài liệu). Ảnh chuẩn ghi ở bước code (ảnh chưa có thì test ảnh chưa có gì để so).

### 4.1. Chạy thử

- **Đã chạy thử:** test tạm `TrialAnchoredTwoBannersTest` (không commit, worktree riêng đã xóa), 2026-11-17, commit `37ef825` (nhánh chính `b676dff` cộng khung plan): `testAdsController(flags = DefaultAdsFlags(settings))` với `is_show_gallery_banner`, `is_show_gallery_banner_collapsible` bật, `openConsentGateForTest()`, `Column { AdSlot(GALLERY_BANNER); AdSlot(GALLERY_BANNER_COLLAPSIBLE) }` khung `w411dp-h891dp-xxhdpi`, đếm `AdView` trong `decorView`, đo node bằng `getBoundsInRoot`.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TrialAnchoredTwoBannersTest*'`. Không chạy `scripts/check.sh`: tiêu chí E là 1 (không có kỹ thuật lần đầu dùng), lần chạy này chỉ để kiểm điểm nối và đo trạng thái hiện tại.
- **Kết quả:** qua (test chỉ in số). `adViews=2`, banner cố định cao 64.0 dp, banner thu gọn cao 128.0 dp: hôm nay cả hai dựng và chiếm chỗ, nên `twoBanners_secondNoViewNoSpace_warns` sẽ thất bại đúng lý do (2 thay vì 1, 128 dp thay vì 0). Điểm nối `adViews` và đo cao dùng được không cần `admobReadyForTest()`. Chưa chạy thử thanh đáy cùng `AdSlot` và `ActivityScenario.recreate()`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (đổi cách hiện quảng cáo: cái neo đáy thứ hai của một màn không còn hiện), 4 (lượt bị chặn không ghi sự kiện phân tích), 7 (`:core:ads`, dùng bởi mọi app). Không tính điểm; người phụ trách sản phẩm biết trước khi duyệt: app đang đặt hai quảng cáo neo đáy trên một màn mất một cái sau khi đồng bộ.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một file mới và ba file sửa trong `:core:ads`, bốn file của màn xem thử, ba file test có sẵn, năm ảnh chuẩn, một tài liệu |
| B. Độ rõ của yêu cầu | 1 | KB-35 và MC-12 rõ; còn điểm cần người quyết: lượt khi chưa ai giữ (câu hỏi 1), cách kiểm đích của `NavHost` (câu hỏi 2) |
| C. Khả năng kiểm chứng tự động | 1 | Luật lượt thuần và nối với controller thật đều có test JVM; quảng cáo thật trên đích thật chỉ kiểm trên máy |
| D. Ảnh hưởng chéo | 2 | Mọi màn có thanh đáy hay banner neo đáy của mọi app; T-092 dùng lại phạm vi và cùng sửa `BottomAdBar.kt`, `AdSlot.kt` |
| E. Độ mới của kỹ thuật | 1 | `CompositionLocal` và luật thuần là mẫu có sẵn (`LocalAdsController`, `LocalResumeSuppression`, L-072); khóa theo `LocalLifecycleOwner` là mới trong dự án nhưng là API sẵn có, có phương án dự phòng (mục 3.4) |
| **Tổng** | 7 | **Mức: Cao (chạm điểm chặn)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `BottomAdBarScaffold` (mọi màn có thanh đáy: `feature:sample`, màn xem thử), `AdSlot` (mọi banner), `AdsController.kt` (một trường mới). Màn chỉ có một quảng cáo neo đáy không đổi hành vi.
- Task khác có thể xung đột: T-092 (quảng cáo trong nội dung thắng lớp nổi) dùng lại `AnchoredAdScope` và tham số "được phép" của banner thu gọn (MC-13), cùng sửa `BottomAdBar.kt`, `AdSlot.kt`; chưa có plan. T-110 sửa `NativeAdSlot.kt`, không chạm file của plan này.
- Đề xuất thứ tự chạy: sau T-090 (đã merge); trước T-092.

## 7. Điều kiện dừng riêng

- Phải giữ trạng thái lượt ở vỏ app (`MainActivity`, `AppNavHost`) hay thêm thư viện navigation vào `:core:ads` thì dừng: plan dựa vào phạm vi theo `LocalLifecycleOwner` và `AnchoredAdScreen`.
- Ảnh chuẩn ngoài năm ảnh của mục 3.2 đổi (nhất là ảnh thiết kế `AdGalleryScreen_default_*`, `AdGalleryScreen_collapsible-*`, `SampleScreen_*`) thì dừng.
- Phải đổi luật của `BannerSlotHost` (khung, B-04, B-06) hay của `BottomAdBarHost` (D-07) để cái chờ không chiếm chỗ thì dừng: plan chỉ không dựng chúng.

## 8. Câu hỏi cho người duyệt

1. **Lượt khi chưa ai giữ (người phụ trách sản phẩm).** MC-12 nói khóa đổi lúc màn đang mở không đổi lượt. Khi vào màn mà chưa cái nào được phép (thường gặp nhất: lần mở đầu, đồng ý quảng cáo về sau khi màn đã dựng; hay vị trí đang tắt rồi được bật), AI đề xuất: cái đầu tiên theo thứ tự trở thành được phép thì nhận lượt, vì lúc đó chưa có lượt nào để đổi, và màn chỉ có một quảng cáo neo đáy vẫn hiện khi bật lại (KB-7 tiêu chí 2). Cách khác: không ai chạy tới lần vào màn sau; khi đó màn đầu tiên sau lúc đồng ý không có quảng cáo neo đáy. Chọn cách đề xuất?
2. **Kiểm đích của `NavHost`.** `:core:ads` không có thư viện navigation; plan kiểm "hai màn" bằng hai `LifecycleOwner` riêng (đúng cách `NavHost` cấp cho mỗi đích) và bước máy thật, không thêm test dựng `AppNavHost` trong `:app` (chỉ có một đích có quảng cáo neo đáy là màn mẫu). Đồng ý, hay cần thêm một test Robolectric trong `:app` dựng `NavHost` hai đích giả (thêm file test vào mục 3.2)?
3. **Test phạm vi từng trang của màn xem thử.** `LocalAnchoredAdScope` là `internal` của `:core:ads`, `:app` không đọc được. Plan đề xuất kiểm từ `:app` rằng nội dung trang dựng mới khi đổi trang, phần "mỗi `AnchoredAdScreen` một phạm vi" kiểm trong `:core:ads`. Nếu người duyệt muốn kiểm trực tiếp, cách khác là để `AnchoredAdScreen` nhận một tham số `onScope` chỉ cho test, AI không đề xuất vì thêm API công khai.
4. **Tên và vị trí nút mới của màn xem thử.** "Collapsible banner" ngay sau "Banners" (ảnh `AdGalleryScreen_default-more_light.png` ghi lại). Hay đặt cuối nhóm để ít đổi ảnh hơn?
5. **Ghi chú, không cần trả lời:** chạy thử mục 4.1 chỉ gồm hai `AdSlot`; chưa chạy thử thanh đáy cùng `AdSlot` và `recreate()`, phiên sẽ thấy ở bước test viết trước. Plan soạn trong thời gian có giới hạn của bước cuối phiên: test của mục 3.5 đã xét theo tên, chỗ dựng và nhóm, chưa đọc hết thân từng test; người duyệt muốn thì yêu cầu bản 2 đọc hết trước khi duyệt.

## 9. Quyết định duyệt

- **Quyết định:** 
- **Người duyệt:** , **duyệt chéo:** 
- **Ghi chú:** 

## 10. Nhật ký thực hiện
