---
ticket: T-031
title: Rewarded là hành động có điều kiện, kèm hộp thoại xem quảng cáo
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M, số gợi ý 9.5M (scripts/token-usage.py estimate, 2026-10-25); nâng vì định dạng quảng cáo mới, API công khai mới, hộp thoại có giao diện và hai module
blocking_points: [ads, core, analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-031: REWARDED LÀ HÀNH ĐỘNG CÓ ĐIỀU KIỆN, KÈM HỘP THOẠI XEM QUẢNG CÁO

## 1. Tóm tắt

`:core:ads` thêm khe rewarded trong `AdsController` (nạp sẵn, thử lại, nạp lại như inter) và một hàm composable chung `rememberRewardedAction(placement, onGranted, ...)`: feature gọi hàm này ở hành động có điều kiện; vị trí tắt thì hành động làm ngay, vị trí bật thì hiện hộp thoại "Xem quảng cáo" / "Không, cảm ơn", thưởng trao ngay trong callback thưởng, không có quảng cáo thì theo chính sách khai báo trong `AdPlacement` (`block` mặc định, `allow`, `allow_after_<n>`). Phần quyết định nằm trong hàm thuần và một lớp lượt (`RewardedTurn`) không phụ thuộc SDK, kiểm bằng unit test; hộp thoại kiểm bằng test Robolectric và screenshot test; hai sự kiện `ad_reward_earned`, `reward_dialog` thêm vào `:core:analytics`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | Khi người vận hành bật vị trí rewarded của hành động đó thì hộp thoại hỏi xem quảng cáo hiện, có nút "Xem quảng cáo" và "Không, cảm ơn". | "Bật" là `is_show_ads` và `is_show_<mã vị trí>` cùng bật (`placementSwitch`, KB-7). Hộp thoại do `:core:ads` vẽ (R-01), có tiêu đề, chỗ để app vẽ nội dung riêng, hai nút. Mở hộp thoại ghi `reward_dialog` với `dialog_action=show`. |
| KB-19 | Khi vị trí tắt thì hành động làm ngay, không có hộp thoại. | `onGranted` chạy ngay, không ghi `reward_dialog`, không gọi SDK. Không có `AdsController` (test, preview) cũng vậy, như `rememberShowFullScreenAd`. Cổng đồng ý đóng: xem câu hỏi 3. |
| KB-19 | Khi người dùng xem đủ thì nhận thưởng (hành động được làm) ngay lúc quảng cáo báo thưởng, không chờ đóng. | `onGranted` chạy trong `OnUserEarnedRewardListener` (R-02), ghi `ad_reward_earned`. Lúc quảng cáo đóng sau đó không gọi gì thêm. |
| KB-19 | Khi quảng cáo chưa sẵn sàng thì hành động theo chính sách của vị trí: mặc định bị chặn và người dùng thử lại được; nếu lỗi mạng thì app báo kiểm tra kết nối; hộp thoại đóng. | Bấm "Xem" khi khe rỗng: ghi `ad_opportunity` `not_ready`, gọi nạp (F-07), hộp thoại đóng; chính sách `block` gọi `onBlocked`, `allow` gọi `onGranted`, `allow_after_<n>` gọi `onGranted` ở lần thứ n liên tiếp không có quảng cáo, các lần trước gọi `onBlocked`. Lỗi mạng (R-03): máy không có mạng, hoặc lượt nạp gần nhất lỗi mã 2, hoặc lỗi mã bất kỳ lúc máy không có mạng; cờ xóa ở đầu mỗi lượt nạp. Khi đó `:core:ads` báo "kiểm tra kết nối" một lần (câu hỏi 4); app chỉ nhận `onBlocked`. Lỗi mạng không đổi chính sách: `allow` vẫn cho làm (câu hỏi 5). |
| KB-19 | Khi người dùng đóng quảng cáo trước khi đủ thì không có thưởng, hành động bị chặn. | Đóng mà chưa có callback thưởng: gọi `onBlocked`, không phụ thuộc chính sách (MC-05). R-04 ghi "đóng mà chưa được thưởng thì không gọi gì": hiểu là không gọi `onGranted`; `onBlocked` là callback tùy chọn để feature biết hành động bị chặn (câu hỏi 6). Hiện lỗi (`onAdFailedToShowFullScreenContent`) cũng gọi `onBlocked`, ghi `ad_show_fail`. |
| KB-19 | Khi bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì app làm như AdsKit (không chặn) và số liệu ghi lần bấm đó là "bận" (`busy`). | Hộp thoại giữ mở sau lần bấm đầu tới khi quảng cáo báo đã lên màn (`onAdShowedFullScreenContent`) hay có kết quả. Lần bấm thứ hai trong khoảng đó gọi `show` lần nữa trên cùng quảng cáo (R-06) và ghi `ad_opportunity` `busy`. Mỗi lượt chỉ có một kết quả (`onGranted` hay `onBlocked`), kết quả đầu thắng; callback lỗi của lần `show` thứ hai đến sau khi quảng cáo đã lên màn thì bỏ qua (mục 3.4, câu hỏi 7). |
| KB-19 | Rewarded không bị chặn bởi khoảng cách `ads_interval` và không làm dời khoảng cách đó. | Rewarded không qua `decideFullScreenTurn`, không đọc, không ghi mốc của `FullScreenAdGuard` (R-05, I-02). Rewarded có đặt cờ "đang có quảng cáo toàn màn hình" (`FullScreenAdPresence`) để inter, app open không hiện chồng lên (F-01), nhưng chính nó không kiểm cờ này (R-06; câu hỏi 7). |
| KB-19 | Hai app khác nhau gắn rewarded vào hai hành động khác nhau (nhận vật phẩm, xuất file) mà không sửa module core. | App chỉ thêm một dòng `AdPlacement` (ví dụ `RW_GET_ITEM("rw_get_item", AdFormat.REWARDED)`, `RW_EXPORT("rw_export", AdFormat.REWARDED, rewardedNoAd = RewardedNoAdPolicy.Block)`) và gọi `rememberRewardedAction` ở màn của mình. Theo ticket, `rw_get_item` và `rw_export` là hai ví dụ trong test (khai báo bằng `AdPlacementDeclaration`), không phải dòng của `AdPlacement`. |

Thiết kế tham chiếu: Không có thiết kế riêng cho hộp thoại rewarded (`features/ads/design/screens/` không có ảnh của nó; docs/ops/design-workflow.md). Hộp thoại dựng bằng token và thành phần của `:core:designsystem` (`PrimaryButton`, `MaterialTheme.shapes.extraLarge`, `surfaceContainerHigh` như `NetworkDialog`); câu hỏi 2.

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0004 (vị trí quảng cáo), ADR-0008 (hệ thống thiết kế), ADR-0012, ADR-0013 (mã thử ở staging, đồng ý), ADR-0019 (log), ADR-0024 (mạng), ADR-0026 (bộ PARTNER), ADR-0029 (phần ngoài mục 3.2).

Luật liên quan trong `docs/ops/ads-business-rules.md`: MC-01, MC-02, MC-05, R-01 tới R-06, F-01, F-05, F-06, F-07, I-02, S-11, S-15, mục 8.2 (`ad_opportunity`, `ad_reward_earned`, `reward_dialog`), PL-03 (doanh thu theo vị trí).

## 3. Giải pháp

### 3.1. Cách làm

**Khai báo (MC-05).** `AdPlacement` và `AdPlacementDeclaration` thêm thuộc tính `rewardedNoAd: RewardedNoAdPolicy = RewardedNoAdPolicy.Block`. `RewardedNoAdPolicy` là sealed interface công khai: `Block`, `Allow`, `AllowAfter(n)` (n từ 1), mỗi giá trị có `code` (`block`, `allow`, `allow_after_<n>`) và hàm `parse(code)` trả `null` khi sai, để sau này một app đọc được khóa `rw_no_ad_policy_<mã vị trí>` (câu hỏi 28 của spec). T-031 không tạo khóa này: spec chưa có app nào cần chỉnh từ xa (câu hỏi 8). `init` của khai báo từ chối chính sách khác `Block` ở vị trí không phải `REWARDED`. Hành động mà vị trí giữ là tên vị trí và chú thích của dòng, không phải thuộc tính code.

Vị trí `GALLERY_RW_ACTION("gallery_rw_action", AdFormat.REWARDED)` (chỉ staging, câu hỏi 13 của spec) thêm vào `AdPlacement` để hàm công khai có một vị trí rewarded thật cho test và để staging nạp sẵn khe rewarded; nút gọi trên màn xem thử là việc của T-036 (câu hỏi 1).

**Quyết định thuần (`RewardedRules.kt`, internal).**

- `decideRewardedStart(enabled: Boolean, consentOpen: Boolean): RewardedStart` trả `RUN_ACTION` (không hộp thoại) hay `ASK` (hiện hộp thoại).
- `decideRewardedWatch(ready: Boolean, consentOpen: Boolean, enabled: Boolean): RewardedWatch` lúc bấm "Xem": `SHOW`, `NOT_READY`, hay `RUN_ACTION` khi vị trí vừa bị tắt trong lúc hộp thoại mở (L-061).
- `decideNoAd(policy: RewardedNoAdPolicy, missesBefore: Int): NoAdOutcome` trả `GRANT` hay `BLOCK`; `missesBefore` là số lần liên tiếp không có quảng cáo trước lần này. `AllowAfter(n)`: lần thứ n (missesBefore = n - 1) trả `GRANT`.
- `class RewardedMisses` (bộ đếm lần liên tiếp không có quảng cáo theo vị trí, chỉ sống trong process như mốc I-03): tăng khi `BLOCK` vì không có quảng cáo, về 0 khi `GRANT` vì chính sách, khi quảng cáo lên màn, hay khi có thưởng.
- `isRewardedNetworkError(online: Boolean, lastLoadNetworkError: Boolean): Boolean` (R-03). `class RewardedNetworkFlags`: `onLoadStarted(placement)` xóa cờ, `onLoadFailed(placement, code, online)` đặt cờ khi `code == 2` hay `!online`, `isSet(placement)`.

**Một lượt bấm (`RewardedTurn.kt`, internal).** Lớp không phụ thuộc SDK, nhận `show: () -> Unit` và hai callback `onGranted`, `onBlocked`. Sự kiện vào: `watchTapped()`, `showed()`, `rewarded()`, `dismissed()`, `showFailed()`. Quy tắc: lần `watchTapped` đầu gọi `show`; lần sau trước `showed` gọi `show` lần nữa và trả `busy` để ghi `ad_opportunity` (R-06); `rewarded` gọi `onGranted` một lần (R-02); `dismissed` chưa có thưởng gọi `onBlocked`; `showFailed` trước `showed` gọi `onBlocked` (R-04), sau `showed` thì bỏ qua (lần `show` thứ hai bị SDK từ chối trong lúc lần đầu đang hiện); mọi sự kiện sau kết quả đầu chỉ ghi log. Biến `finished` cho hộp thoại biết lúc đóng.

**`AdsController` (khe rewarded).** Làm theo đúng mẫu inter của T-021, T-022, T-023:

- `loadedRewardeds: MutableMap<AdPlacement, RewardedAd>`, `rewardedCells` (PL-03: doanh thu ghi vị trí lúc hiện), `fullScreenLoadState` có nhánh `REWARDED`, `preloadNow` có nhánh `preloadRewarded`. `AdFormat.hasFullScreenSlot` thêm `REWARDED` để kế hoạch nạp sẵn, lượt bật khóa từ xa (S-20) và lượt có mạng lại (S-24) cùng gọi `loadFullScreenAdIfNeeded`; `AdFormat.isFullScreen` giữ nguyên (rewarded không đi qua `rememberShowFullScreenAd`, không đẩy mốc).
- `preloadRewarded(placement)`: như `preloadInterstitial` (kiểm `isEnabled`, `mayRequestNow` ngay trước khi gửi, S-15; `RewardedAd.load(fullScreenLoadContext(...), unitId, request, RewardedAdLoadCallback)`, nên Unity, ironSource có `Activity` khi có; `setOnPaidEventListener` ghi doanh thu với `AdFormat.REWARDED`). Đầu mỗi lượt gửi gọi `networkFlags.onLoadStarted(placement)` (R-03); lỗi gọi `networkFlags.onLoadFailed(placement, code, online.value)` rồi `onFullScreenLoadFailed` (F-05). Thử lại và nạp lại sau khi đóng hay hiện lỗi dùng `FullScreenReloads` sẵn có (2 giây, `AdRetryPolicy` đã có `REWARDED`).
- `internal fun rewardedEnabled(placement): Flow<Boolean>` = `isEnabled(placement)` kết hợp cổng đồng ý (cho `decideRewardedStart`).
- `internal suspend fun watchRewarded(activity, placement, turn: RewardedTurn, onNetworkError: () -> Unit)`: đọc khóa và cổng (`decideRewardedWatch`); không có quảng cáo: ghi `ad_opportunity` `not_ready` với `load_state` lúc quyết định, gọi `loadFullScreenAdIfNeeded` (F-07), áp `decideNoAd`, gọi `onNetworkError` khi `isRewardedNetworkError`. Có quảng cáo: ghi `shown`, đặt `presence.tryBegin` (để inter khác thấy `busy`) nhưng không đọc kết quả của nó, gắn `FullScreenContentCallback` nối vào `RewardedTurn`, `ad.show(activity) { turn.rewarded() }`. Quảng cáo rời khe ở `onAdShowedFullScreenContent` (không ở lần bấm đầu), để lần bấm thứ hai còn gọi được `show` trên cùng quảng cáo (R-06). Đóng hay hiện lỗi: `presence.onClosed()`, `fullScreenReloads.onClosedOrShowFailed(placement)`. Không gọi `recordIntervalMark`.
- Log bằng `AppLogger` thẻ `Ads`: quyết định của mỗi lần bấm, thưởng, đóng, lỗi hiện (mã lỗi, không có chữ của SDK).

**Không có bước chờ trước khi hiện (L-091).** Bấm "Xem" chỉ hiện quảng cáo đã nạp sẵn; khe rỗng thì kết quả có ngay (R-03), không chờ nạp. Người dùng rời app lúc hộp thoại đang mở: hộp thoại giữ nguyên, không có gì tự hiện khi quay lại. Rời app sau khi `show` đã gọi mà quảng cáo chưa lên màn: SDK quyết; callback thưởng hay đóng đến sau vẫn đi qua `RewardedTurn` như thường, kết quả chỉ một lần. Mục 4 có test `pause().stop()` rồi `resume()` khi hộp thoại mở.

**Hàm công khai (`RewardedAds.kt`).**

```kotlin
@Composable
fun rememberRewardedAction(
    placement: AdPlacement,
    onBlocked: () -> Unit = {},
    dialogContent: (@Composable () -> Unit)? = null,
    onGranted: () -> Unit,
): () -> Unit
```

`require(placement.format == AdFormat.REWARDED)`. Hành động trả về: không có `AdsController` hay activity thì gọi `onGranted` ngay; có thì đọc `rewardedEnabled` một lần lúc bấm (`first()`), `RUN_ACTION` gọi `onGranted`, `ASK` mở hộp thoại (trạng thái mở giữ bằng `rememberSaveable`, nên dựng lại activity không mở lần hai và không ghi `reward_dialog` lần hai). Hộp thoại gọi `watchRewarded` ở nút "Xem", đóng khi lượt có kết quả; nút "Không, cảm ơn", nút quay lại và chạm ngoài hộp thoại đóng hộp thoại và ghi `reward_dialog` `dismiss`, không gọi callback nào. Báo "kiểm tra kết nối" bằng `Toast` với chuỗi `ads_rewarded_check_connection` (câu hỏi 4). Phần nối ra composable dùng một interface nội bộ `RewardedHost` mà `AdsController` cài đặt, để test Robolectric dùng bản giả (như `StartupAds` của T-024).

`RewardedAdDialog` (internal, không trạng thái): `Dialog` của Compose với `dismissOnBackPress = true`, `dismissOnClickOutside = true`; thẻ rộng tối đa 312 dp, nền `surfaceContainerHigh`, bo `MaterialTheme.shapes.extraLarge`, tiêu đề `titleLarge`, chỗ `dialogContent`, nút chính `PrimaryButton` "Watch ad" rộng hết thẻ, nút phụ `TextButton` "No, thanks" có `colors` rõ (L-053). Khoảng cách theo `Spacing`. Thẻ cuộn được khi nội dung dài (khung thấp, L-028). Test tag trong `RewardedDialogTestTags`.

**`:core:analytics`.** `AdMetricEvent` thêm `RewardEarned(adFormat, adPlacement)` và `RewardDialog(adFormat, adPlacement, dialogAction)`; `FirebaseAdMetrics` đặt tên `ad_reward_earned` (tham số `ad_placement`) và `reward_dialog` (tham số `ad_placement`, `dialog_action`: `show`, `dismiss`), theo mục 8.2. `AdTelemetry` thêm `rewardEarned(placement)` và `rewardDialog(placement, action)`, đi qua công tắc nhóm toàn màn hình như `opportunity` (`lifecycleOn`). Phễu AppsFlyer `af_rewarded_*` không thuộc ticket này: thuộc T-033 (ticket ghi "theo T-023").

**Tài liệu.** `docs/ops/ads-guide.md`: bảng định dạng (dòng rewarded bỏ "(đợt 3)", ghi T-031), mục 5 thêm ví dụ khai báo vị trí rewarded và gọi `rememberRewardedAction`, mục mạng con sửa câu "rewarded (T-031) theo cách của plan đó" thành nạp bằng activity đang mở như inter. Chú thích của `AdFormat.REWARDED` đổi theo.

Mẫu làm theo: `showInterstitial`, `preloadInterstitial` và `skipTurn` của `AdsController`; `rememberFullScreenAdAction` của `FullScreenAds.kt`; `NativeFullScreenAdDialog` (T-013) cho hộp thoại; `NetworkDialog` của `:core:designsystem` cho thẻ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Thuộc tính `rewardedNoAd` ở `AdPlacement` và `AdPlacementDeclaration` (kiểm trong `init`); dòng `GALLERY_RW_ACTION`; `hasFullScreenSlot` có `REWARDED`; chú thích `REWARDED` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedNoAdPolicy.kt` | Sealed interface `RewardedNoAdPolicy`, `code`, `parse` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedRules.kt` | `decideRewardedStart`, `decideRewardedWatch`, `decideNoAd`, `RewardedMisses`, `isRewardedNetworkError`, `RewardedNetworkFlags` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurn.kt` | Lượt bấm, một kết quả, `busy` ở lần bấm thứ hai |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedAds.kt` | `rememberRewardedAction`, `RewardedHost`, `RewardedAdDialog`, `RewardedDialogTestTags` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Khe rewarded: nạp, thử lại, trạng thái nạp, `watchRewarded`, `rewardedEnabled`, cờ lỗi mạng, bộ đếm, cài đặt `RewardedHost` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` | `rewardEarned`, `rewardDialog` |
| Sửa | `core/ads/src/main/res/values/strings.xml` | `ads_rewarded_title`, `ads_rewarded_watch`, `ads_rewarded_no_thanks`, `ads_rewarded_check_connection` |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/AdMetrics.kt` | `AdMetricEvent.RewardEarned`, `AdMetricEvent.RewardDialog` |
| Sửa | `core/analytics/src/main/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetrics.kt` | Tên và tham số của hai sự kiện |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` | Test tên và tham số hai sự kiện |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedNoAdPolicyTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedRulesTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnTest.kt` | Mục 4 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | Robolectric, `rememberRewardedAction` với `RewardedHost` giả |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedAdDialogTest.kt` | Robolectric, nút, chữ, màu chữ nút phụ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedAdDialogScreenshotTest.kt` | Ba ảnh chuẩn dưới đây |
| Tạo | `core/ads/src/test/screenshots/RewardedAdDialog_default_light.png` | Ảnh chuẩn |
| Tạo | `core/ads/src/test/screenshots/RewardedAdDialog_default_dark.png` | Ảnh chuẩn |
| Tạo | `core/ads/src/test/screenshots/RewardedAdDialog_long_small.png` | Ảnh chuẩn khung `w360dp-h640dp-xxhdpi`, nội dung riêng dài |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Test thuộc tính `rewardedNoAd`; dữ liệu theo dòng mới (ADR-0029): `galleryPlacements_coverEveryFormatOnce` đếm 8 vị trí và tập định dạng có thêm `AdFormat.REWARDED` (đỏ khi chạy thử, mục 4.1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | Test hai sự kiện và công tắc nhóm |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` | `hasFullScreenSlot` có `REWARDED`, `isFullScreen` không có |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Dữ liệu theo dòng mới (ADR-0029): `googleTestIds_coverGalleryPlacements` đếm 8 vị trí `gallery_` thay vì 7 (đỏ khi chạy thử, mục 4.1) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: thêm dòng `is_show_gallery_rw_action` (`./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`) |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh: thêm khóa `is_show_gallery_rw_action` |
| Sửa | `docs/ops/ads-guide.md` | Mục 3.1 cuối |

Phần L-020, L-089 (test lặp `AdPlacement.entries` và file đọc danh sách vị trí): đã chạy thật với dòng `GALLERY_RW_ACTION` tạm bằng `scripts/check.sh` (mục 4.1). Đỏ đúng bốn test: `AdPlacementTest.galleryPlacements_coverEveryFormatOnce`, `AdUnitIdsTest.googleTestIds_coverGalleryPlacements` (`:core:ads`), `RemoteConfigKeysTest.keysFile_matchesCode`, `RemoteConfigKeysTest.templateFile_matchesCode` (`:app`, hai file sinh tự động); cả bốn đã có trong bảng trên. Nơi đọc hai file khóa sinh tự động: `app/build.gradle.kts` (chỉ truyền đường dẫn cho test qua `systemProperty`) và `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` (so file với code): không sửa, vì task sinh lại hai file bằng lệnh của T-007 nên test qua; đã thử lệnh sinh lại ở mục 4.1. Qua mà không sửa (đã chạy, không chỉ đọc): `AdsFlagsTest`, `AdsSettingsTest`, `AdPreloadPlanTest`, `AdPlacementSourceTest`, `StagingRemoteValuesModuleTest`. Các file sau đọc `AdPlacement.entries`: `AdPlacementTest`, `AdPreloadPlanTest`, `AdsFlagsTest`, `AdsSettingsTest`, `AdUnitIdsTest`, `AdPlacementSourceTest` (`:core:ads`), `RemoteConfigKeysTest` (`:app`), `StagingRemoteValuesModuleTest` và `StagingRemoteValuesModule` (`:app`, staging), `AdsController`, `AdPreloadPlan`, `AdUnitIds`, `AdsSettings`. `scripts/release.sh` bỏ qua tiền tố `gallery_`; `scripts/status.py` (mục 6) cũng bỏ khóa `gallery_`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Vị trí bật bằng `is_show_<mã vị trí>` suy ra từ khai báo (mặc định tắt).
- Quảng cáo: định dạng `rewarded` có trong MC-02 và spec (KB-19); thêm một vị trí chỉ ở staging `gallery_rw_action` (câu hỏi 13 của spec, câu hỏi 1 của plan), khóa `is_show_gallery_rw_action`, mã thử của Google `ca-app-pub-3940256099942544/5224354917` (đã có trong `GoogleTestAdUnitIds`). Không thêm vị trí production.
- Sự kiện phân tích: `ad_reward_earned`, `reward_dialog` (spec mục 8, `ads-business-rules.md` mục 8.2); `ad_opportunity`, `ad_load_*`, `ad_show_fail`, `ad_clicked`, `ad_impression_placement` với `ad_format=REWARDED` (đã có trong spec). Không thêm sự kiện AppsFlyer (T-033).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `RewardedAd` nằm trong `play-services-ads` đã có, không kéo quyền mới; `Toast` không cần quyền.
- **Thư viện và SDK:** Không thêm. `RewardedAd`, `RewardedAdLoadCallback`, `OnUserEarnedRewardListener` thuộc `play-services-ads` (`libs.play.services.ads`, đã là `implementation` của `:core:ads`); `Dialog` của `androidx.compose.ui.window` và `material3` đã có trên classpath biên dịch của `:core:ads` qua plugin `template.android.compose` (đã kiểm ở mục 4.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** gọi `RewardedAd.show` lần hai trên cùng quảng cáo trước khi nó lên màn (R-06). Trên JVM không kiểm được SDK làm gì: có thể bỏ qua, có thể gọi `onAdFailedToShowFullScreenContent` (mã 1, quảng cáo đã dùng) cho lần thứ hai trong khi lần đầu đang hiện.
- **Điều kiện chuyển:** kiểm thủ công trên staging (mục 4) thấy lần bấm thứ hai làm lượt có kết quả sai (bị chặn dù xem đủ, hay hai kết quả), hoặc app dừng.
- **Cách làm thay:** lần bấm thứ hai vẫn ghi `busy` nhưng không gọi `show` lần nữa. Đây là lệch khỏi R-06 nên không tự chuyển: ghi vào báo cáo để người quyết (câu hỏi 7). Test của `RewardedTurn` giữ nguyên, trừ câu khẳng định "gọi `show` hai lần" (người quyết).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| MC-05, khai báo chính sách | Unit test | `RewardedNoAdPolicyTest.code_roundTrips` (`block`, `allow`, `allow_after_3` qua `parse` rồi `code`), `parse_rejectsBadCodes` (`allow_after_0`, `allow_after_x`, `ALLOW`, chuỗi rỗng trả `null`), `allowAfter_requiresOneOrMore` |
| MC-05, chính sách chỉ cho rewarded | Unit test | `AdPlacementTest.rewardedNoAd_defaultsToBlock`, `rewardedNoAd_otherThanBlockOnNonRewarded_throws` (khai báo `AdPlacementDeclaration("x", AdFormat.INTERSTITIAL, rewardedNoAd = Allow)` ném `IllegalArgumentException`) |
| KB-19, tiêu chí 1 và 2 | Unit test | `RewardedRulesTest.placementOff_runsActionWithoutDialog`, `placementOn_asks`, `placementTurnedOffWhileDialogOpen_watchRunsAction` (L-061) |
| KB-19, tiêu chí 4, chính sách | Unit test | `RewardedRulesTest.noAd_block_blocks`, `noAd_allow_grants`, `noAd_allowAfter3_blocksTwiceThenGrants` (đối chứng L-055: lần 1, 2 `BLOCK`, lần 3 `GRANT`, lần 4 sau khi về 0 lại `BLOCK`), `misses_resetWhenAdShows`, `misses_arePerPlacement` (hai vị trí `rw_get_item`, `rw_export` xen nhau, L-068) |
| KB-19, tiêu chí 4, lỗi mạng (R-03) | Unit test | `RewardedRulesTest.networkError_offlineAnyCode`, `networkError_code2Online`, `noNetworkError_code3Online` (đối chứng), `networkFlag_clearedWhenLoadStarts` (lỗi mã 2, rồi `onLoadStarted`, cờ tắt), `networkError_doesNotChangeAllowPolicy` |
| KB-19, tiêu chí 3 (thưởng ngay lúc báo) | Unit test | `RewardedTurnTest.reward_grantsBeforeDismiss` (`showed`, `rewarded`: `onGranted` đã chạy một lần trước `dismissed`; `dismissed` sau đó không gọi gì) |
| KB-19, tiêu chí 5 | Unit test | `RewardedTurnTest.dismissWithoutReward_blocks`, `dismissWithoutReward_blocksEvenWithAllowPolicy` |
| R-04 | Unit test | `RewardedTurnTest.showFailedBeforeShowed_blocksOnce`, `showFailedAfterShowed_isIgnored` |
| KB-19, tiêu chí 6 (R-06) | Unit test | `RewardedTurnTest.secondTapBeforeShowed_callsShowAgainAndReportsBusy` (show gọi 2 lần, lần 2 trả `busy`), `tapAfterResult_doesNothing`, `oneResultPerTurn` (thưởng rồi `showFailed`: chỉ `onGranted`) |
| KB-19, tiêu chí 7 (R-05) | Unit test | `RewardedRulesTest.rewarded_neitherReadsNorPushesInterval` (`declaration.readsInterval()` và `pushesIntervalMark()` cùng `false` cho vị trí rewarded); `AdFormatTest.rewarded_hasSlotButIsNotFullScreenGate` |
| KB-19, tiêu chí 8 | Unit test | `RewardedRulesTest.twoAppsTwoActions_noCoreChange`: hai khai báo `rw_get_item` (`Block`) và `rw_export` (`AllowAfter(2)`) cho khóa `is_show_rw_get_item`, `is_show_rw_export` (`AdsSettings.placementEnabled(declaration)`) và quyết định riêng của từng vị trí |
| KB-19, tiêu chí 1, 2 (nối composable) | Compose UI test (Robolectric) | `RewardedActionTest.noController_grantsAtOnce`, `placementOff_grantsWithoutDialog` (không có node `RewardedDialogTestTags.DIALOG`, không có `reward_dialog`), `placementOn_showsDialog_logsShowOnce`, `noThanks_closesWithoutCallback_logsDismiss`, `backPress_closesLikeNoThanks` |
| KB-19, tiêu chí 4 (nối composable) | Compose UI test (Robolectric) | `RewardedActionTest.notReady_block_closesDialogAndCallsOnBlocked`, `notReady_networkError_showsToastOnce` (`ShadowToast.getTextOfLatestToast()` là chuỗi `ads_rewarded_check_connection`, `ShadowToast.shownToastCount() == 1`), `notReady_notNetwork_noToast` |
| KB-19, tiêu chí 3, 6 (nối composable) | Compose UI test (Robolectric) | `RewardedActionTest.reward_callsOnGrantedAndClosesDialog`, `doubleTap_callsWatchTwice` (host giả ghi hai lần gọi) |
| Dựng lại activity, rời foreground (L-061, L-091) | Compose UI test (Robolectric) | `RewardedActionTest.recreateWhileDialogOpen_keepsDialog_logsShowOnce` (`ActivityScenario.recreate()`), `backgroundWhileDialogOpen_nothingShowsOnReturn` (`pause().stop()` rồi `resume()`: hộp thoại còn, host không nhận lần gọi hiện nào) |
| R-01, chữ và nút của hộp thoại | Compose UI test (Robolectric) | `RewardedAdDialogTest.showsTitleContentAndButtons` (chữ từ `strings.xml`), `watchButton_isAtLeast48dpHigh`, `watchTwice_callsTwice`, `noThanksText_usesOnSurfaceVariantColor` (vẽ `decorView` vào `Bitmap`, đọc điểm ảnh giữa chữ, so với màu token, L-053) |
| Giao diện hộp thoại | Screenshot test | `RewardedAdDialogScreenshotTest`: `captureRoboImage("src/test/screenshots/RewardedAdDialog_default_light.png")`, `"src/test/screenshots/RewardedAdDialog_default_dark.png"`, khung `@Config(qualifiers = "w411dp-h891dp-xxhdpi")` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, nội dung riêng dài | `captureRoboImage("src/test/screenshots/RewardedAdDialog_long_small.png")`; cùng lớp có test `longContent_watchButtonReachableByScroll` (cuộn tới nút chính, nút hiện đủ) |
| Sự kiện `ad_reward_earned`, `reward_dialog` | Unit test | `AdTelemetryTest.rewardEarned_logsPlacement`, `rewardDialog_logsShowAndDismiss`, `rewardEvents_offWhenFullScreenSwitchOff` (đối chứng: bật thì có, tắt thì không); `FirebaseAdMetricsTest.rewardEarned_nameAndParams`, `rewardDialog_nameAndParams` |
| Vị trí mới không làm hỏng test lặp `AdPlacement.entries` | Unit test sẵn có | Toàn bộ test JVM của `:core:ads`, `:app` qua `scripts/check.sh`; bốn test đỏ khi chạy thử (mục 4.1) qua sau khi cập nhật dữ liệu và sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` (`RemoteConfigKeysTest`) |
| Thưởng thật, đóng sớm, bấm hai lần, mất mạng với quảng cáo thử của Google | Kiểm tra thủ công (máy thật: quảng cáo thật) | Chưa có nút gọi tới T-036. Sau T-036: bật `is_show_ads`, `is_show_gallery_rw_action` trên staging; bấm nút rewarded của màn xem thử: (1) xem hết, thấy hành động chạy lúc quảng cáo báo thưởng; (2) đóng sớm, hành động không chạy; (3) bấm "Xem" hai lần thật nhanh, một lượt, một kết quả, `ad_opportunity` `busy` trong DebugView; (4) tắt mạng rồi bấm, thấy thông báo kiểm tra kết nối |
| Đọc lại ví dụ khai báo trong `ads-guide.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc mục 5 của `ads-guide.md` |

**Ngoại lệ viết test trước:** (1) Phần gọi SDK trong `AdsController` (`RewardedAd.load`, `show`, gắn `FullScreenContentCallback` và `OnUserEarnedRewardListener` vào `RewardedTurn`): lớp của SDK không dựng được trên JVM, như phần nối inter của T-021, T-022. Mọi quyết định của đường này nằm trong hàm thuần và `RewardedTurn` đã có test; mọi đường nạp (nạp sẵn, thử lại, nạp lại sau khi đóng, bật khóa từ xa, có mạng lại) đi qua `preloadRewarded`, nơi duy nhất gọi `RewardedNetworkFlags.onLoadStarted` (L-072). (2) Chuỗi trong `strings.xml` và đoạn tài liệu `ads-guide.md`. Người duyệt cần đồng ý.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-25, worktree `/home/user/plan-T-031` trên nhánh chính `a76b3cd`, ba thay đổi tạm không commit: (1) `core/ads/src/main/.../TrialRewarded.kt` gọi `RewardedAd.load(context, unitId, AdRequest, RewardedAdLoadCallback)`, `RewardedAd.show(activity, OnUserEarnedRewardListener { ... })` và một hộp thoại Compose (`AlertDialog`, `Button`, `TextButton` của `material3`, `testTag`); (2) `core/ads/src/test/.../TrialRewardedTest.kt` (Robolectric, `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi`): bấm nút "Watch" hai lần, nút "No" một lần, đếm callback; và lớp `RewardedAd` có trên classpath test; (3) dòng `GALLERY_RW_ACTION("gallery_rw_action", AdFormat.REWARDED)` trong `AdPlacement`. Gỡ hết sau khi chạy (`git status --short` sạch).
- **Lệnh:** `export ANDROID_HOME=$HOME/android-sdk; scripts/check.sh` (3 phút 37 giây, `BUILD FAILED`, thoát 1); rồi `./gradlew :app:testStagingDebugUnitTest --tests '*RemoteConfigKeysTest' -PremoteConfigKeys.update=true` (thoát 0) để xem hai file khóa sinh lại.
- **Kết quả:** biên dịch `:core:ads` qua với `RewardedAd`, `RewardedAdLoadCallback`, `OnUserEarnedRewardListener` và `material3`: thư viện đã có trên classpath biên dịch, không thêm thư viện. ktlint, Android Lint và build bản staging qua. `TrialRewardedTest`: 2 test, 0 lỗi (hộp thoại Compose bấm được hai lần liền trên Robolectric, mỗi lần một callback). Đỏ đúng bốn test do dòng vị trí mới (mục 3.2): `:core:ads` 446 test, 2 lỗi (`AdPlacementTest.galleryPlacements_coverEveryFormatOnce` dòng 16, `AdUnitIdsTest.googleTestIds_coverGalleryPlacements` dòng 40, cùng đếm 7 vị trí `gallery_`); `:app` staging 85 test, 2 lỗi (`RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`). Sinh lại hai file khóa chỉ thêm `is_show_gallery_rw_action` (1 dòng ở `docs/remote-config-keys.md`, 5 dòng ở `docs/remote-config-template.json`). Không chạy `:app:assembleStagingRelease` vì plan không thêm thư viện hay module. Chưa chạy thử: hành vi thật của SDK khi gọi `show` hai lần (mục 3.4, câu hỏi 7), vì môi trường cloud không có emulator.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: định dạng rewarded lần đầu có code hiện, thêm vị trí `gallery_rw_action`), 4 (sự kiện phân tích `ad_reward_earned`, `reward_dialog`), 7 (module core `:core:ads`, `:core:analytics`). Mức Cao, cần duyệt chéo. Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module core, API công khai mới (`rememberRewardedAction`, `RewardedNoAdPolicy`, thuộc tính của `AdPlacement`) |
| B. Độ rõ của yêu cầu | 2 | Spec đã chốt và R-01 tới R-06 rõ, nhưng còn câu hỏi ảnh hưởng tới cách làm: vị trí `gallery_rw_action` ở ticket nào, cổng đồng ý đóng, cách báo lỗi mạng, hộp thoại chưa có thiết kế |
| C. Khả năng kiểm chứng tự động | 2 | Quyết định và giao diện có test; hành vi thật của SDK (thưởng, bấm hai lần) chỉ kiểm được trên máy, và chưa có nút gọi tới T-036 |
| D. Ảnh hưởng chéo | 2 | Sửa `AdsController.kt`, `AdPlacement.kt` mà T-025 tới T-036 cũng sửa; `hasFullScreenSlot` đổi làm kế hoạch nạp sẵn có thêm khe rewarded ở staging |
| E. Độ mới của kỹ thuật | 2 | `RewardedAd`, callback thưởng và `Dialog` của Compose lần đầu dùng trong `:core:ads` (hộp thoại native toàn màn hình của T-013 là `Dialog` nhưng không có nút) |
| **Tổng** | 11 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt`, `AdPlacement.kt`, `AdTelemetry.kt`, `strings.xml` của `:core:ads`; `AdMetrics.kt`, `FirebaseAdMetrics.kt` của `:core:analytics`; `docs/ops/ads-guide.md`. `hasFullScreenSlot` đổi: `fullScreenPlacements()` có thêm vị trí rewarded, nên staging nạp sẵn rewarded khi bật `is_show_gallery_rw_action`.
- Task khác trong sprint có thể xung đột: T-025 (resume), T-026, T-027, T-028, T-030, T-032 cùng sửa `AdsController.kt`, `AdPlacement.kt`; T-033 sửa `:core:analytics` (AppsFlyer, gồm `af_rewarded_*`); T-036 thêm nút gọi `gallery_rw_action` (dùng lại dòng của plan này, câu hỏi 1).
- Đề xuất thứ tự chạy: sau T-022, T-023 (đã merge). Trước T-036. Với T-033: chạy trước để T-033 nối phễu `af_rewarded_*` vào `watchRewarded`; chạy sau thì T-033 ghi rõ phần đó cho T-031.

## 7. Điều kiện dừng riêng

- Phải đổi hành vi của inter, app open hay native toàn màn hình (cổng `decideFullScreenTurn`, mốc `ads_interval`, `isFullScreen`) để rewarded chạy được: dừng.
- Bảng khóa Remote Config sinh tự động có khóa khác `is_show_gallery_rw_action` thay đổi: dừng.
- Cần gửi `af_rewarded_*` hay sự kiện nào ngoài mục 3.3: dừng.

## 8. Câu hỏi cho người duyệt

1. **Vị trí `gallery_rw_action` thêm ở T-031 hay T-036?** Ticket T-036 ghi vị trí này trong phạm vi của nó; nhưng `rememberRewardedAction` nhận một `AdPlacement`, nên không có dòng rewarded nào thì không test được hàm công khai và khe rewarded không bao giờ nạp. Đề xuất của AI: T-031 thêm dòng (chỉ staging, mã thử), T-036 chỉ thêm nút gọi và ghi Sửa thay vì Tạo (L-054).
2. **Hộp thoại chưa có thiết kế; nút "Xem" theo R-01 (cao 56 dp, bo 28 dp, như AdsKit) hay `PrimaryButton` của hệ thống thiết kế (52 dp, bo 16 dp)?** `ads-guide.md` ghi màu, chữ, bo góc lấy từ token của `:core:designsystem`, không lấy của AdsKit (ADR-0008). Đề xuất của AI: dùng `PrimaryButton` và thẻ như `NetworkDialog`; vẽ thiết kế trên Figma sau (như T-035) và ghi lại ảnh chuẩn theo plan của ticket đó.
3. **Cổng đồng ý đóng (người dùng chưa có hay không cho đồng ý) mà khóa vị trí bật: hành động làm ngay hay hiện hộp thoại rồi "chưa sẵn sàng"?** Spec và R-01 tới R-06 không nói. Với chính sách `block`, hiện hộp thoại nghĩa là người dùng không bao giờ làm được hành động. Đề xuất của AI: coi như vị trí tắt (làm ngay, không hộp thoại, không ghi cơ hội), giống quy tắc "không ghi `ad_opportunity` khi cổng đồng ý đóng" ở mục 8.2.
4. **"App báo kiểm tra kết nối" bằng gì?** R-03 để `:core:ads` báo, app chỉ nhận thất bại. ADR-0024 có popup mất mạng của `:app` nhưng popup đó chỉ hiện khi máy mất mạng, không hiện khi lượt nạp gần nhất lỗi mã 2 lúc máy có mạng. Đề xuất của AI: `Toast` của `:core:ads` với chuỗi `ads_rewarded_check_connection`, không cần chỗ chứa ở `:app`.
5. **Lỗi mạng mà chính sách là `allow` hay `allow_after_<n>` đã tới lượt cho: có báo kiểm tra kết nối không?** Đề xuất của AI: chỉ báo khi hành động bị chặn; cho làm thì không báo.
6. **Đóng quảng cáo trước khi được thưởng: R-04 ghi "không gọi gì".** Plan thêm callback tùy chọn `onBlocked` (mặc định rỗng) cho cả ca này và ca không có quảng cáo bị chặn, để feature biết hành động không làm (ví dụ bỏ trạng thái chờ). Đề xuất của AI: giữ `onBlocked` tùy chọn; không gọi `onGranted` là đúng R-04.
7. **Bấm "Xem" lần hai (R-06):** plan gọi `show` lần nữa trên cùng quảng cáo như AdsKit, và bỏ qua callback lỗi đến sau khi quảng cáo đã lên màn để lượt không bị chặn nhầm. Rewarded đặt cờ "đang có quảng cáo toàn màn hình" để inter, app open không hiện chồng (F-01) nhưng chính nó không kiểm cờ. Đề xuất của AI: duyệt như vậy; nếu kiểm trên máy thấy SDK làm hỏng lượt thì làm theo mục 3.4 sau khi người quyết.
8. **Khóa `rw_no_ad_policy_<mã vị trí>`:** T-031 chỉ làm chính sách cố định trong `AdPlacement` và hàm `parse` sẵn, chưa tạo khóa vì chưa có spec của app nào cần chỉnh từ xa (câu hỏi 28 của spec). Đề xuất của AI: đồng ý; app cần thì thêm khóa trong plan của app.
9. **Chữ của hộp thoại:** `strings.xml` của `:core:ads` đang viết tiếng Anh ("Privacy settings", "Ad"). Đề xuất của AI: "Watch an ad to continue?", "Watch ad", "No, thanks", "Check your internet connection and try again."; app dịch trong `values-<ngôn ngữ>` của mình.
10. **Bộ đếm `allow_after_<n>`:** đếm theo vị trí, chỉ trong process (mở lại app thì về 0), về 0 khi cho làm vì chính sách, khi quảng cáo lên màn hay khi có thưởng. Đóng sớm (bị chặn) không tăng bộ đếm vì đó không phải "không có quảng cáo". Đề xuất của AI: như vậy.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
