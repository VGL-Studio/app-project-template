---
ticket: T-063
title: Cổng không hiện quảng cáo cho người đã mua gói bỏ quảng cáo
status: approved             # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, init-order, core)
risk_level: high             # low | medium | high
token_estimate: 23.9M        # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-29); không chỉnh
blocking_points: [ads, init-order, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-30
lane:
---

# PLAN T-063: CỔNG KHÔNG HIỆN QUẢNG CÁO CHO NGƯỜI ĐÃ MUA GÓI BỎ QUẢNG CÁO

## 1. Tóm tắt

`:core:ads` có thêm một giao diện `AdFreeStatus` (một `StateFlow<Boolean>`: "người dùng đã mua gói bỏ quảng cáo"); `:app` cấp nó qua Hilt, mặc định luôn `false`. `DefaultAdsFlags` gộp tín hiệu này vào công tắc tổng: `is_show_ads && !adFree`, ở đúng hai chỗ hôm nay đọc `is_show_ads` (`global` và `placementEnabled`). Mọi đường tải, hiện, nạp sẵn, quảng cáo mở app, khi quay lại và rewarded đã đọc công tắc qua `AdsFlags` (`placementSwitch`, `AdsController.isEnabled`), nên tín hiệu bật thì mọi thứ như `is_show_ads` tắt, kể cả khi đổi giữa phiên. Kiểm bằng unit test của `AdsFlags`, `placementSwitch`, controller thật (resume, mở app, toàn màn hình, rewarded) và test Compose của thanh đáy và hộp thoại rewarded với tín hiệu đổi giữa chừng (L-061); thêm một test của module Hilt ở `:app`.

**Phiên bản 2 (2026-10-30).** Bản 1 đã duyệt ngày 2026-10-30 (anh Nhật). Theo rà soát độc lập danh sách chạy RUN-20261030-01: mục 2 và mục 4 thêm tiêu chí "Thêm 2026-10-30" của KB-30 (câu hỏi 50: người đã mua vẫn có hộp thoại đồng ý, SDK và AppsFlyer như cũ) và test canh giữ `AdFreeGateTest.adFree_consentSdkAndAppsFlyerUnchanged` (cổng đồng ý mở như cũ, AppsFlyer bắt đầu đúng ở 10 giây khi `adFree = true`), nên `AdFreeGateTest` có 6 test (mục 3.2) và thêm ngoại lệ (4) ở mục 4; câu MC-11 viết sẵn ở mục 3.1 E thêm hai ý theo trả lời (3) và (6) của mục 9; mục 6 ghi rằng nếu dùng dự phòng `testAdsFlags` của mục 3.4 thì test của T-068 gọi `DefaultAdsFlags(settings)` phải cập nhật dữ liệu test theo ADR-0029. Không đổi cách làm, file code hay chấm rủi ro. Mục 9 chờ duyệt lại.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-30 | Khi app báo người dùng đã mua gói bỏ quảng cáo thì không có quảng cáo nào được tải hay hiện, kể cả quảng cáo mở app và khi quay lại app; thanh đáy không chiếm chỗ (câu hỏi 39) | Như `is_show_ads` tắt (KB-7, câu hỏi 7): mọi vị trí, kể cả vị trí toàn app (`open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo`), có công tắc `false`. Không lượt tải mới nào bắt đầu; lượt đang chạy tự dừng ở chỗ hôm nay đọc lại công tắc trước khi gửi. Quảng cáo toàn màn hình đã nạp sẵn trong bộ nhớ không bị hủy ngay, nhưng không bao giờ hiện và hết hạn theo luật cũ (A-07, như `is_show_ads` tắt; câu hỏi 2). Không ghi `ad_opportunity` (cổng ra `DISABLED`, resume ra `OFF`) |
| KB-30 | Khi người dùng làm một hành động có điều kiện (KB-19) thì hành động làm thẳng, không có hộp thoại | `rewardedSwitch` ra `false`: `RewardedAction.start` gọi `onGranted` ngay, không mở hộp thoại, không ghi `reward_dialog`. Hộp thoại đang mở lúc tín hiệu bật: bấm "Xem quảng cáo" thì lần quyết định đọc khóa tắt (`SWITCHED_OFF`), hành động được làm, không cơ hội (MC-05) |
| KB-30 | Khi trạng thái đổi giữa lúc dùng app (vừa mua xong) thì quảng cáo đang hiện trong màn hình biến mất ngay, không cần mở lại app | Mọi khe trong màn hình (`AdSlot`, `NativeAdSlot`, `BottomAdBarScaffold`, `rememberInListAds`, bước của luồng, pool) đã đọc `isEnabled` bằng `collectAsStateWithLifecycle`; công tắc ra `false` thì khe về `NONE`, quảng cáo bị hủy, thanh đáy co về 0 (hành vi đã có khi tắt vị trí, `BottomAdBarTest.placementSwitchedOff_barGone_reservationZero_adsDestroyed`). Plan chỉ thêm test nối từ tín hiệu tới khe |
| KB-30 | Khi app không nối tín hiệu này (dự án mẫu hôm nay) thì quảng cáo chạy như trước | `:app` cấp `AdFreeStatus.NONE` (luôn `false`), nên `is_show_ads && true` giống hệt hôm nay. Mọi test có sẵn qua không sửa |
| KB-30, vế "Thêm 2026-10-30" | Khi người dùng đã mua gói bỏ quảng cáo thì hộp thoại đồng ý, SDK quảng cáo và AppsFlyer vẫn như khi chưa mua; chỉ không tải, không hiện quảng cáo (câu hỏi 50) | Phương án (a) của câu hỏi 6, anh Nhật chọn (c): tín hiệu chỉ vào công tắc (`AdsFlags`), không vào `gatherConsent`, `ConsentCoordinator`, `startSdk` hay `awaitAttributionStart`. `MainActivity` vẫn gọi `ads.gatherConsent(this)` không điều kiện; cổng mở thì `MobileAds.initialize` chạy, `canRequestAds` bật như cũ; AppsFlyer khởi động theo `awaitAttributionStart` (T-033), muộn nhất 10 giây. Bỏ hộp thoại và không khởi động SDK khi đã mua là ticket sau |
| KB-7 (liên quan) | Công tắc tổng tắt thì không tải, không hiện ở mọi vị trí; bật lại thì chạy lại | Giữ nguyên; tín hiệu và khóa là hai vế AND, không vế nào ghi đè vế kia. Tín hiệu về `false` (hoàn tiền, đổi tài khoản) thì quảng cáo chạy lại như khi bật lại `is_show_ads` |

Quyết định đã có (spec, câu hỏi 39, anh Nhật 2026-10-29): tín hiệu đúng sai từ `:app` qua Hilt, mặc định `false`; bật thì như `is_show_ads` tắt; `:core:ads` không lưu trạng thái mua; không thêm khóa Remote Config. Không làm mua hàng hay Play Billing (ticket, Không bao gồm).

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; khe biến mất như khi tắt vị trí, docs/ops/design-workflow.md).

ADR liên quan: ADR-0001 (`:core:*` chỉ phụ thuộc `:core:*`; giao diện ở core, binding ở `:app`), ADR-0003 (không đọc Remote Config ngoài `:core:config` và `AdsFlags`; không thêm khóa), ADR-0026 (luật PARTNER, cổng gom một chỗ), ADR-0036 (điểm 4 của Quyết định: cổng không hiện quảng cáo; ghi điểm lệch PARTNER vào `ads-business-rules.md`), ADR-0038 (JBase không có cổng này; anh Nhật giữ lựa chọn, câu hỏi 39).

## 3. Giải pháp

### 3.1. Cách làm

**A. Giao diện ở `:core:ads` (`AdFreeStatus.kt`, mới).** Theo mẫu `AdUnitIds`, `ConsentShortcutConfig`: giao diện công khai ở core, giá trị do `:app` cấp.

```kotlin
/**
 * Whether the user bought ad removal (KB-30, question 39 of the ads spec), given by the app through Hilt. The app
 * owns the purchase and where it is kept; :core:ads only reads [adFree] and never stores it. True works like the
 * master switch `is_show_ads` off: no new load, slots on screen drop their ads at once, no startup or resume ad,
 * held actions run at once, no opportunity. A StateFlow, so :core:ads never waits for a first value (startup).
 */
interface AdFreeStatus {
    val adFree: StateFlow<Boolean>

    companion object {
        /** The template has no purchase: never ad-free. */
        val NONE: AdFreeStatus = object : AdFreeStatus {
            override val adFree: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
        }
    }
}
```

`StateFlow`, không phải `Flow`: `AdsController.awaitStartupAd` chờ `startupChoiceKnown.first { it }` không giới hạn, và `combine` chỉ phát khi mọi vế đã có giá trị. Một `Flow` của app chậm phát giá trị đầu (DataStore, Play Billing) sẽ giữ màn loading và mọi công tắc ở `false` tới khi nó phát. `StateFlow` luôn có giá trị, nên thứ tự khởi tạo giữ như hôm nay; app chọn giá trị đầu (trạng thái mua đã lưu lần trước). Câu hỏi 1.

**B. Gộp vào công tắc tổng (`AdsFlags.kt`).** `DefaultAdsFlags` nhận thêm `adFreeStatus: AdFreeStatus = AdFreeStatus.NONE` (mặc định chỉ cho test JVM gọi thẳng constructor; Hilt luôn tiêm binding của `:app`). Một luồng riêng thay cho hai chỗ đọc `settings.value(AdsSettings.IS_SHOW_ADS)`:

```kotlin
/** is_show_ads, off too while the user is ad-free (KB-30): the one master switch of every flag below. */
private val masterSwitch: Flow<Boolean> =
    combine(settings.value(AdsSettings.IS_SHOW_ADS), adFreeStatus.adFree) { on, adFree -> on && !adFree }
        .distinctUntilChanged()
```

- `global`: phần tử đầu của `combine(listOf(...))` là `masterSwitch`; `adsEnabled`, `showOpenAds`, `showOpenAdsFirstOpen`, `showResumeAds`, `showNativeAfterInter` đã AND với `adsEnabled` nên tự tắt. KDoc của `adsEnabled`: `is_show_ads && !adFree`.
- `placementEnabled(declaration)`: `combine(masterSwitch, settings.value(placementEnabled(declaration)))`. KDoc của giao diện: `is_show_ads && !adFree && is_show_<code>`.
- `showNativeBottom`, `showNativeCollap`, `expandCollapAfterSeconds`, `consentShortcutEnabled`, hai khóa log: không đổi. Chúng không tự bật quảng cáo nào: thanh đáy còn cần `isEnabled` của vị trí (`bottomAdBarAllowed`), đường đồng ý không phải quảng cáo.
- `AdsSettings`, bảng khóa, `release.sh`, `ads-plan.py`: không đổi (không khóa mới).

**C. Mọi đường đi qua `AdsFlags` (L-072).** Không sửa code controller hay composable; mỗi đường dưới đây đã đọc công tắc qua `AdsFlags` (đã xem code ngày 2026-10-29, nhánh chính `c29ce2b`):

| Đường | Đọc công tắc ở | Khi tín hiệu bật | Test của plan (mục 4) |
| --- | --- | --- | --- |
| Khe trong màn hình: banner (`AdSlot`), native (`NativeAdSlot`), thanh đáy (`BottomAdBar`), bước của luồng (`NativeFlowStep`) | `ads.isEnabled(placement)` → `placementSwitch` → `flags.placementEnabled` | `enabled = false`: `bannerFrame` ra `NONE`, `nativeSlotAllowed` ra `false`, `bottomAdBarAllowed` ra `false`; quảng cáo bị hủy | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff`, `BottomAdBarTest.adFreeMidSession_barGone_reservationZero_adsDestroyed` |
| Native trong danh sách (`rememberInListAds`) | `inListEvery` → `isEnabled` | `null`: không chèn hàng nào | `AdFreeGateTest.adFreeMidSession_inListEveryNull` |
| Pool native (`NativeAdPools`) | `enabled = ::isEnabled` | Mọi vị trí của pool tắt: pool hủy quảng cáo rảnh và lượt đang nạp (`ads-business-rules.md`, mục 5.9, như hôm nay) | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff` (vị trí pool có trong vòng lặp); hủy khi tắt đã có test ở `NativeAdPoolsTest` |
| Nạp sẵn toàn màn hình (`collectPreloadTriggers`, `FullScreenPreloadSlot`), nạp lại sau đóng (`FullScreenReloads`), nạp native toàn màn hình, rewarded (`RewardedTurns.loadIfNeeded`) | `enabledNow` lấy từ `isEnabled`; `preloadNativeFullScreen` và lượt nạp rewarded đọc lại `isEnabled` trước khi gửi | Không gửi lượt mới | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff` (công tắc); đường nạp đã có test khi vị trí tắt |
| Interstitial và native toàn màn hình (`showFullScreenAd` → `decideFullScreenTurn`) | `isEnabled(placement).first()` | `DISABLED`, `opportunityResult()` là `null`: không cơ hội, không hiện | `AdFreeGateTest.adFree_fullScreenTurn_disabled_noOpportunity` |
| Quảng cáo mở app (`awaitStartupAd`) | `startupChoice` từ `flags.global` (`startupAdPlacement`) | `null` ngay, màn loading không chờ quảng cáo (KB-13, tiêu chí 5) | `StartupAdChoiceTest.adFree_isNull`, `AdFreeGateTest.adFree_awaitStartupAd_nullAtOnce` |
| Khi quay lại app (`readResumeGate`, `ResumeAdTurns`) | `latestGlobalFlags` → `resumeAdPlacement` | `placement = null`, quyết định `OFF`, không cơ hội | `AdsControllerResumeTest.adFreeMidSession_resumeOff_noOpportunity` |
| Native sau inter (`nativeTurns.comboEnabled`) | `flags.global.first().showNativeAfterInter` | `false` | `AdsFlagsTest.adFree_turnsAppOpenResumeAndComboOff`, `StartupAdChoiceTest.placementSwitch_adFreeMidSession_emitsFalse` |
| Rewarded (`rememberRewardedAction`, `RewardedTurns.decide`) | `rewardedSwitch` → `isEnabled`; lần quyết định đọc `port.enabled` | `start` làm thẳng; lần quyết định ra `SWITCHED_OFF`, `Granted`, không cơ hội | `RewardedActionTest.realController_adFree_keyOn_runsActionAtOnce_noDialog`, `realController_adFreeWhileDialogOpen_watchRunsAction` |

Vì cổng nằm ở một chỗ (`masterSwitch`), không thêm nhánh `if (adFree)` nào trong controller hay composable (`ads-business-rules.md`, mục 13: gom mọi cổng vào một chỗ). Log: không thêm dòng mới; quyết định bị chặn đã ghi `Full-screen <mã> skipped: DISABLED`, `Resume ad OFF: none`, `Rewarded <mã> granted: switched off`.

**D. Binding ở `:app` (`app/src/main/kotlin/com/vglstudio/template/di/AdFreeModule.kt`, mới).** Một module cho cả hai biến thể (staging và production cùng mặc định), theo mẫu `AnalyticsConfigModule`:

```kotlin
/** The template sells no ad removal: never ad-free (KB-30). An app with a purchase provides its own here. */
@Module
@InstallIn(SingletonComponent::class)
object AdFreeModule {
    @Provides
    fun adFreeStatus(): AdFreeStatus = AdFreeStatus.NONE
}
```

ADR-0001: `:core:ads` không biết `:app`; `core/ads/build.gradle.kts` không đổi; `checkModuleDependencies` (trong `scripts/check.sh`) giữ xanh. `:app` thiếu binding thì Hilt báo lỗi lúc biên dịch `:app` (`:app:assembleStagingDebug` của `check.sh`), nên app tạo từ mẫu không quên được.

**E. Tài liệu.**

- `docs/ops/ads-guide.md`: mục mới "5.1. Nối gói bỏ quảng cáo" sau mục 5: app có mua hàng thay `AdFreeModule` bằng binding của mình, cấp một `StateFlow<Boolean>` có giá trị đầu là trạng thái đã lưu; binding của app phải là `@Singleton` (một `StateFlow` cho cả đồ thị, để mọi nơi đọc cùng một trạng thái; `AdFreeModule` của mẫu cấp `NONE` không scope vì `NONE` là một object, rà soát độc lập, điểm 3); `:core:ads` không lưu, không hỏi Play; bật thì như `is_show_ads` tắt (liệt kê như KB-30); đổi giữa phiên áp ngay; không có khóa Remote Config; thử trên staging bằng cách tạm cấp `MutableStateFlow(true)` trong `AdFreeModule` trên máy dev, không commit. Một dòng vào danh sách "Trước khi phát hành" (mục 9): app bán gói bỏ quảng cáo đã nối tín hiệu và thử mua, hoàn tiền trên bản staging.
- `docs/ops/ads-business-rules.md`: thêm MC-11 vào bảng Luật của mô hình: "Cổng không hiện quảng cáo (KB-30, ADR-0036): app cấp `AdFreeStatus`; bật thì công tắc tổng coi như tắt, mọi định dạng và vị trí như `is_show_ads` tắt, hành động có điều kiện làm thẳng, không ghi cơ hội; hộp thoại rewarded đang mở lúc tín hiệu bật không tự đóng, bấm nút Xem quảng cáo thì hành động làm thẳng và `af_rewarded_ad_eligible` của AppsFlyer vẫn ghi ở lần bấm đó, như khi `is_show_ads` tắt giữa chừng; hộp thoại đồng ý, SDK quảng cáo và AppsFlyer vẫn như khi chưa mua, chỉ không tải, không hiện (KB-30, câu hỏi 50; bỏ hộp thoại và không khởi động SDK khi đã mua là ticket sau); `:core:ads` không lưu trạng thái; không có khóa" (chủ: app cấp, ads làm). Câu cuối mục 12 (Chỗ ta lệch có chủ đích): thêm "cổng không hiện quảng cáo cho người đã mua gói bỏ quảng cáo, AdsKit không có (ADR-0036, MC-11)". Không sửa mục 10.1 (bảng khóa, `AdsSettingsTest` chép tay).
- `docs/ops/remote-config.md`: không đổi (không khóa mới).

**Mẫu có sẵn plan làm theo:** `AdUnitIds` và `ConsentShortcutConfig` (giao diện core, `@Provides` ở `:app`); công tắc tổng `is_show_ads` của T-019 và test `AdsFlagsTest.masterSwitchOff_*`; test controller thật của `AdsControllerResumeTest`, `RewardedActionTest.realController_*`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdFreeStatus.kt` | Giao diện `AdFreeStatus` với `adFree: StateFlow<Boolean>` và `NONE` (mục 3.1 A) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsFlags.kt` | `DefaultAdsFlags` nhận `adFreeStatus`; `masterSwitch` dùng trong `global` và `placementEnabled`; KDoc của `AdsFlags.placementEnabled`, `AdsGlobalFlags.adsEnabled` và các trường AND với nó (mục 3.1 B) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Chỉ KDoc của `isEnabled`: "the master switch `is_show_ads` (off too while ad-free, KB-30)". Không đổi code |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/di/AdFreeModule.kt` | `@Provides fun adFreeStatus(): AdFreeStatus = AdFreeStatus.NONE` (mục 3.1 D) |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/di/AdFreeModuleTest.kt` | Test của module (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | 4 test mới (mục 4); không đổi test cũ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StartupAdChoiceTest.kt` | 2 test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | 1 test mới (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFreeGateTest.kt` | Robolectric, controller thật: 6 test (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | `Bar()` nhận tham số `enabledFlow: Flow<Boolean>?` (null: dùng `enabled` và `slotAllowed` như hôm nay); khi có `enabledFlow`, cả `enabled` của `rememberBottomAdBarLayers`, `enabled` của `BannerSlotHost` và `allowed` của `NativeAdSlotGate` (`nativeSlotAllowed(canRequest, enabledFromFlow, unitId)`) cùng lấy từ luồng đó, không còn `slotAllowed` riêng; 1 test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | `realController()` nhận tham số `adFree: AdFreeStatus = AdFreeStatus.NONE`; 2 test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerFixtures.kt` | (dự phòng) hàm `testAdsFlags` (mục 3.4) |
| Sửa | `docs/ops/ads-guide.md` | Mục 5.1 mới, một dòng ở mục 9 (mục 3.1 E) |
| Sửa | `docs/ops/ads-business-rules.md` | MC-11, câu cuối mục 12 (mục 3.1 E) |
| Sửa | `docs/features/ads/tickets/T-063-cong-khong-hien-quang-cao.md` | `plan:`, `status`, nhật ký theo phiên |
| Sửa | `docs/features/ads/plans/T-063-cong-khong-hien-quang-cao.md` | `status`, mục 10 |

Nơi đọc các file này (L-020): giao diện công khai `AdsFlags` không đổi, nên các bản giả `AdsFlags` (`RewardedActionTest.HoldingFlags`, test của `AdGalleryViewModel` ở `app/src/testStaging`) không sửa. Constructor của `DefaultAdsFlags` thêm một tham số có mặc định, nên 16 chỗ gọi `DefaultAdsFlags(settings)` trong test (`grep -rn 'DefaultAdsFlags(' --include='*.kt' .`) không sửa. Hilt tạo `DefaultAdsFlags` qua `AdsFlagsModule`, cần binding `AdFreeStatus` mà `AdFreeModule` cấp; `core:ads` không có test Hilt nào (tìm `HiltAndroidTest` và `TestInstallIn` trong mọi `src/test`: không có), nên không thiếu binding ở test. `scripts/ads-plan.py` chỉ nhắc `ads-guide.md`, `ads-business-rules.md` trong chữ, không đọc file; Tìm `ads-guide.md` và `ads-business-rules.md` trong `.github/`: không có. Các test sau nhắc `ads-business-rules.md` chỉ trong chú thích (một hay hai dòng KDoc dẫn mục luật), không mở file nào (không có `File(`, `readText`, `Paths.get`), nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` (chép tay bảng khóa của mục 10.1; plan không sửa mục 10.1), `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Plan chỉ thêm dòng MC-11 và một vế câu ở mục 12, không đổi bảng khóa hay hằng số.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Không khóa Remote Config mới (câu hỏi 39); `is_show_ads` giữ tên, kiểu, mặc định.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay tần suất khi tín hiệu tắt (mặc định). Thêm một điều kiện tắt mọi vị trí (KB-30), chạm điểm chặn 1.
- Sự kiện phân tích: không thêm, không đổi sự kiện hay tham số. Khi tín hiệu bật, các sự kiện bớt đi như khi `is_show_ads` tắt (không cơ hội, không `reward_dialog`).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Plan không gọi API Android mới; không thêm `com.android.vending.BILLING` (không mua hàng, ticket, Không bao gồm).
- **Thư viện và SDK:** Không thêm. Code mới chỉ dùng `kotlinx.coroutines.flow.StateFlow`, `MutableStateFlow`, `combine` (đã có trên classpath của `:core:ads` và `:app`) và `dagger.Module`, `dagger.Provides`, `dagger.hilt.InstallIn` (plugin `template.hilt` đã áp cho `:app`, như `AnalyticsConfigModule`). Không thêm Play Billing (`com.android.billingclient`).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** Hilt với tham số có giá trị mặc định của Kotlin trong constructor `@Inject` (`adFreeStatus: AdFreeStatus = AdFreeStatus.NONE`). Kotlin sinh thêm một constructor tổng hợp không có `@Inject`, Dagger đọc constructor chính; chưa có mẫu này trong dự án.
- **Điều kiện chuyển:** `:core:ads` hay `:app` không biên dịch được vì Dagger (KSP) hay ktlint báo lỗi ở constructor đó, sau 2 lần sửa.
- **Cách làm thay:** bỏ giá trị mặc định; thêm hàm thử `internal fun testAdsFlags(settings: RemoteSettings, adFree: AdFreeStatus = AdFreeStatus.NONE) = DefaultAdsFlags(settings, adFree)` vào `AdsControllerFixtures.kt` (dự phòng, có trong mục 3.2) và đổi 16 chỗ gọi `DefaultAdsFlags(...)` trong test của `:core:ads` sang hàm này (cập nhật dữ liệu của test cũ theo tham số plan thêm, ADR-0029). Tiêu chí và test ở mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`python3 -I scripts/existing-tests.py docs/features/ads/plans/T-063-cong-khong-hien-quang-cao.md` (2026-10-29): xem đoạn dưới bảng. Plan không đổi hành vi nào khi tín hiệu tắt (mặc định của dự án mẫu): `masterSwitch` với `AdFreeStatus.NONE` phát đúng giá trị `is_show_ads`, cùng lúc, cùng số lần (`distinctUntilChanged` như hôm nay). Hành vi mới chỉ có khi tín hiệu bật, đường chưa có test nào.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `adsEnabled` = `is_show_ads` | `is_show_ads && !adFree`; với `NONE` không đổi (mục 3.1 B) | `AdsFlagsTest.masterSwitchOff_turnsAppOpenResumeAndComboOff`, `masterSwitchOff_turnsEveryPlacementOff` và các test `masterSwitch*` khác | Giữ; test mới `adFreeNone_sameFlagsAsBefore` canh giữ |
| `placementEnabled` = `is_show_ads && is_show_<code>` | `is_show_ads && !adFree && is_show_<code>` | `AdsFlagsTest.*placement*`, `StartupAdChoiceTest.placementSwitch_*` | Giữ |
| Lần phát đầu của `global` chờ mọi khóa của Remote Config | Chờ thêm `adFree`, là `StateFlow` nên có giá trị ngay; thứ tự khởi tạo không đổi | `StartupAdChoiceTest.placementSwitch_waitsForFirstOpen`, `AdsControllerResumeTest.readResumeGate_mapsControllerState` (trước khi đọc khóa) | Giữ |
| Trước lần phát đầu của `global`, `AdsController.globalFlags` dùng `DEFAULT_GLOBAL_FLAGS` (`adsEnabled = true`, `AdsController.kt:219-221`) | Không đổi; giá trị này bỏ qua `adFree`, nhưng hôm nay chỉ thanh đáy (chọn lớp native hay banner) và telemetry đọc nó, không đường tải hay hiện nào; công tắc của vị trí (`isEnabled`) vẫn `false` tới lần phát đầu, nên không có quảng cáo nào lọt (rà soát độc lập, điểm 4) | `BottomAdBarTest.*` (dùng `DEFAULT_FLAGS`) | Giữ |
| Khe, thanh đáy, rewarded, resume, toàn màn hình theo công tắc vị trí | Không đổi code; công tắc có thêm một vế | `BottomAdBarTest.placementSwitchedOff_*`, `RewardedActionTest.switchOff_*`, `realController_*`, `AdsControllerResumeTest.resumeOff_*`, `FullScreenAdsTest.*` | Giữ |

Kết quả `existing-tests.py` (2026-10-29, file của plan: `AdsFlags.kt`, `AdsController.kt`): 171 test dùng tên của hai file. Nhóm "Cần xem trước": 7. Sáu test (`AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`, `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`, `RewardedActionTest.longContent_small_buttonsInsideWindow`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `watchButton_is56dpHigh_fullCardWidth`) chỉ trùng tên chung `start`, không đọc công tắc; giữ. `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` dùng `HoldingFlags(DefaultAdsFlags(settings))`: constructor gọi với một tham số vẫn biên dịch (tham số mới có mặc định), `HoldingFlags` ủy quyền giao diện không đổi; giữ. Nhóm "Nên xem": 86, đã đọc các test có tên của plan:
- `AdsFlagsTest.masterSwitchBackOn_restoresEnabledPlacements`, `placementOffByDefault_evenWithMasterSwitchOn`, `readsOnlyDeclaredKeys`, `fakePlacement_followsDerivedKeys`: đọc khóa qua `FakeRemoteSettings`; `readsOnlyDeclaredKeys` đếm khóa đã đọc, `AdFreeStatus` không phải khóa nên số không đổi; giữ.
- `RemovedPacingRulesTest.adsController_readsRemoteConfigOnlyThroughAdsFlags`: constructor của `AdsController` không nhận `RemoteSettings`, có `AdsFlags`; plan không đổi constructor của controller; giữ.
- `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed`, `AdsSettingsTest.placementsAreOffByDefault`, `StagingRemoteValuesModuleTest.everyAdPlacement_isShown`: bảng khóa; không khóa mới; giữ.
- `InListAdPositionsTest.inListEvery_nullWhenAdsNotAllowedOrBadValue`, `NativeFullScreenStepRecreateTest.recreate_placementOff_noPlaceholder_noTake`, `AdsControllerNativeComboTest.*`: khẳng định hành vi khi vị trí tắt, mà tín hiệu bật tái dùng; giữ.
- `AdGalleryViewModelTest.logsInspectorResults`: bản giả `AdsFlags` riêng; giao diện không đổi; giữ.
- `*.DEFAULT_FLAGS = DefaultAdsFlags(FakeRemoteSettings()).global.first()` (`AdTelemetryTest`, `BottomAdBarTest`, `NativeFunnelTest`, `NativeAdSourcesTest`, `SdkBannerAdViewTest`): với `NONE` mặc định giá trị giống hôm nay; giữ.
- `LoadingViewModelTest.awaitThrows_finishesWithoutAd`, `LoadingRouteTest.finishFromBackgroundThread_navigatesOnMainThread`: bản giả `StartupAds`; giữ.
- Các test còn lại chỉ trùng `start`, `AdsController` hay `DefaultAdsFlags` trong phần dựng; giữ.

## 4. Kiểm thử

Không có số đo trên code hiện tại: plan không sửa lỗi đo được (L-029 không áp dụng). Mọi test mới dưới đây đỏ ở bước 3 vì lý do biên dịch (chưa có `AdFreeStatus`, constructor `DefaultAdsFlags` chưa nhận tham số thứ hai, `AdFreeModule` chưa có); phiên tạo `AdFreeStatus.kt` và khung `DefaultAdsFlags(settings, adFreeStatus)` **chưa** gộp vào `masterSwitch` ở bước 3 (khung rỗng: tham số bị bỏ qua), để test đỏ đúng lý do ở câu khẳng định, không chỉ ở biên dịch. Ghi thông báo đỏ vào nhật ký.

Tín hiệu trong test: `val adFree = MutableStateFlow(false)` và `AdFreeStatus` của test `object : AdFreeStatus { override val adFree = adFree }` (một hàm `adFreeStatus(flow)` riêng trong từng file test, không dùng chung).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-30 tiêu chí 1: mọi vị trí tắt, đổi giữa phiên (L-061) | Unit test | `AdsFlagsTest.adFree_turnsEveryPlacementOff`: như `masterSwitchOff_turnsEveryPlacementOff`, với `DefaultAdsFlags(settings, adFreeStatus(adFree))`: mỗi vị trí không toàn app, `is_show_ads` và `is_show_<mã>` bật, `placementEnabled(placement).test { awaitItem() == true; adFree.value = true; awaitItem() == false; adFree.value = false; awaitItem() == true }` (đối chứng: tắt rồi bật lại). Khung rỗng đỏ ở `awaitItem() == false` (hết giờ chờ của Turbine) |
| KB-30 tiêu chí 1: mở app, resume, combo tắt | Unit test | `AdsFlagsTest.adFree_turnsAppOpenResumeAndComboOff`: bật `show_open_ads`, `show_open_ads_first_open`, `show_resum_ads`, `is_show_native_after_inter`; `global.test`: lần đầu cả năm trường `true`; `adFree.value = true`: `adsEnabled`, `showOpenAds`, `showOpenAdsFirstOpen`, `showResumeAds`, `showNativeAfterInter` đều `false`; `showNativeBottom`, `consentShortcutEnabled` giữ giá trị khóa |
| KB-7 và KB-30: hai vế AND, không ghi đè nhau | Unit test | `AdsFlagsTest.adFree_andMasterSwitch_bothMustAllow`: bốn tổ hợp (`is_show_ads`, `adFree`) = (true, false) → `true`; (true, true), (false, false), (false, true) → `false`, cho `SAMPLE_NATIVE_SMALL` và `global.adsEnabled`. Khung rỗng đỏ ở (true, true) |
| KB-30 tiêu chí 4: không nối tín hiệu thì như trước (L-055) | Unit test | `AdsFlagsTest.adFreeNone_sameFlagsAsBefore`: `DefaultAdsFlags(settings)` và `DefaultAdsFlags(settings, AdFreeStatus.NONE)` cho cùng `global.first()` và cùng `placementEnabled(p).first()` với mọi `p` không toàn app, cả khi `is_show_ads` bật và tắt; `AdFreeStatus.NONE.adFree.value == false`. **Đối chứng (L-055):** cùng bộ khóa với `adFreeStatus(MutableStateFlow(true))` cho `global.first().adsEnabled == false`, khác bản `NONE`, để test không xanh vì so hai thứ luôn bằng nhau |
| KB-30 tiêu chí 1: quảng cáo mở app (KB-13 tiêu chí 5) | Unit test | `StartupAdChoiceTest.adFree_isNull`: `show_open_ads`, `show_open_ads_first_open` bật, `adFree = true`: `startupAdPlacement(flags.global.first(), firstOpen = true)` và `= false` đều `null`; đối chứng `adFree = false`: `OPEN_APP` |
| KB-30 tiêu chí 3 cho vị trí toàn app (L-061) | Unit test | `StartupAdChoiceTest.placementSwitch_adFreeMidSession_emitsFalse`: khóa của `OPEN_APP`, `RESUME`, `NATIVE_AFTER_INTER` bật; mỗi vị trí `placementSwitch(p, flags, flowOf(false)).test { true; adFree.value = true; false; adFree.value = false; true }` |
| KB-30 tiêu chí 1 và 3: mọi công tắc của controller (L-072) | Unit test (Robolectric) | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff`: `testAdsController(unitIds = AdUnitIds { null }, flags = DefaultAdsFlags(settings, adFreeStatus(adFree)))`, mọi `is_show_<mã>` của `AdPlacement.entries` không toàn app bật, cùng `show_open_ads`, `show_resum_ads`, `is_show_native_after_inter`, `open_ad_type` để mỗi vị trí toàn app được chọn một lần (hai vòng: `aoa` rồi `inter`). Với mỗi `p` trong `AdPlacement.entries`: `controller.isEnabled(p).first() == true` trước, `false` sau `adFree.value = true`. Đối chứng: số vị trí đã kiểm bằng `AdPlacement.entries.size` (vị trí mới thêm sau này cũng vào vòng) |
| KB-30 tiêu chí 2 ở controller | Unit test (Robolectric) | `AdFreeGateTest.adFreeMidSession_rewardedSwitchOff`: `openConsentGateForTest()`, `is_show_gallery_rw_action` bật: `rewardedSwitch(GALLERY_RW_ACTION).first() == true`; `adFree.value = true`: `false` |
| KB-30 tiêu chí 1: native trong danh sách | Unit test (Robolectric) | `AdFreeGateTest.adFreeMidSession_inListEveryNull`: gate mở, `is_show_gallery_inlist_small` bật: `inListEvery(GALLERY_INLIST_SMALL).test { 8; adFree.value = true; null; adFree.value = false; 8 }` |
| KB-30 tiêu chí 1: interstitial và native toàn màn hình, không cơ hội | Unit test (Robolectric) | `AdFreeGateTest.adFree_fullScreenTurn_disabled_noOpportunity`: `is_log_ad_fullscreen_events` bật (để cơ hội được ghi nếu có), gate mở, `is_show_gallery_interstitial` bật, `adFree = true`: `decideFullScreenTurn(GALLERY_INTERSTITIAL) == DISABLED`; `showFullScreenAd(activity, GALLERY_INTERSTITIAL)` trả `false`, `onFinished` chạy một lần, `metrics.events` không có `AdMetricEvent.Opportunity`, log có `Full-screen gallery_interstitial skipped: DISABLED`. **Đối chứng:** `adFree = false` thì ra `NOT_READY` (không có SDK) và có một `Opportunity` `not_ready`. Tương tự cho `GALLERY_NATIVE_FULL_SCREEN` |
| KB-30 tiêu chí 1: màn loading không chờ (thứ tự khởi tạo) | Unit test (Robolectric) | `AdFreeGateTest.adFree_awaitStartupAd_nullAtOnce`: `show_open_ads_first_open`, `show_open_ads` bật, `adFree = true`, `controller.start(application, AppActivityFilter { true })`, `idle()`; `CoroutineScope(Dispatchers.Main).launch { result = controller.awaitStartupAd(); done = true }`, `idle()`: `done == true`, `result == null`, không `Opportunity`. **Đối chứng (L-055):** `adFree = false`, cùng bước: `done == false` sau `idle()` (lần chờ theo A-03 còn chạy vì chưa có đồng ý và SDK), để test đầu không xanh vì hàm luôn trả ngay |
| KB-30 vế "Thêm 2026-10-30": đồng ý, SDK và AppsFlyer như khi chưa mua (câu hỏi 50) | Unit test (Robolectric) | `AdFreeGateTest.adFree_consentSdkAndAppsFlyerUnchanged`, theo mẫu `FullScreenAdsTest.start_startsAppsFlyerAt10s_notBefore`: `attribution = FakeAppsFlyerStartup()`, `testAdsController(attribution = attribution, flags = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(true))))`, `is_show_sample_native_small` bật; `controller.start(application, AppActivityFilter.inPackageOf(ComponentActivity::class.java))` (không activity: không mở form, cổng đóng, chỉ đường 10 giây khởi động AppsFlyer); `idleFor(9_999 ms)`: `attribution.starts == 0`; thêm 1 ms: `1`; thêm 20 giây: vẫn `1`, `attribution.prepares == 0`. Rồi `openConsentGateForTest()` (kết quả "được phép" của đường `gatherConsent`): `canRequestAds.value == true` trong khi `isEnabled(SAMPLE_NATIVE_SMALL).first() == false` (cổng đồng ý mở, chỉ quảng cáo tắt). **Đối chứng (L-055):** cùng bước với `adFreeStatus(MutableStateFlow(false))` cho cùng `starts` ở ba mốc và cùng `canRequestAds`, chỉ `isEnabled` là `true`; để test không xanh vì đọc nhầm công tắc. Kiểm diff kèm theo: `git diff --stat origin/main...HEAD -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentCoordinator.kt app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` rỗng (`gatherConsent` vẫn gọi không điều kiện) |
| KB-30 tiêu chí 1 và 3: khi quay lại app (L-061) | Unit test (Robolectric) | `AdsControllerResumeTest.adFreeMidSession_resumeOff_noOpportunity`: như `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` với `show_resum_ads` bật và `DefaultAdsFlags(settings, adFreeStatus(adFree))`: sau 45,1 giây `readResumeGate().placement == RESUME`; `adFree.value = true`, `idle()`: `placement == null`; ra nền rồi trở lại: không `Opportunity`, log `Resume ad OFF: none`. Đối chứng: `adFree.value = false`, lần trở lại sau có một `Opportunity` `resume` `not_ready` |
| KB-30 tiêu chí 3: thanh đáy biến mất ngay, không chiếm chỗ (L-061) | Compose UI test (Robolectric) | `BottomAdBarTest.adFreeMidSession_barGone_reservationZero_adsDestroyed`: `Bar(enabledFlow = controller.isEnabled(SAMPLE_NATIVE_SMALL))` với controller thật (`DefaultAdsFlags(settings, adFreeStatus(adFree))`, `is_show_sample_native_small` bật, `openConsentGateForTest()`), khe và lớp dùng bản giả như các test khác (không gửi SDK). Trong nhánh `enabledFlow`, `allowed` của khe native là `nativeSlotAllowed(canRequest, enabledFromFlow, "unit")` và `enabled` của lớp chọn và banner cũng là `enabledFromFlow`, nên tín hiệu đi qua đúng các hàm cổng của khe thật (`nativeSlotAllowed`, `bottomAdBarAllowed`, `bannerFrame`), không qua `slotAllowed` dựng riêng (rà soát độc lập, điểm 2). `loadBoth()`, `assertContentBottom("control: bar with an ad", 795f)`; `update { adFree.value = true }`: `assertAbsent(SLOT_AD)`, `assertAbsent(CollapsibleNativeAdTestTags.AD)`, `slotAd.destroyCount == 1`, `layerAd.destroyCount == 1`, `assertContentBottom("bar gone", SCREEN_HEIGHT)`. **Đối chứng:** cùng dựng nhưng chỉ đổi `is_show_sample_native_small` thì cũng biến mất (đường cũ), và với khung rỗng của bước 3 (tín hiệu bị bỏ qua) test đỏ ở `assertAbsent(SLOT_AD)` |
| KB-30 tiêu chí 2: hành động làm thẳng | Compose UI test (Robolectric) | `RewardedActionTest.realController_adFree_keyOn_runsActionAtOnce_noDialog`: `realController(adFree = adFreeStatus(MutableStateFlow(true)))`, `openConsentGateForTest()`, `setRealContent`, `start()`: `granted == 1`, `cards() == 0`, không có `reward_dialog` trong metrics. Đối chứng: `realController_consentOpen_keyOn_watchWithoutAd_closesDialogAndBlocks` có sẵn (tín hiệu tắt, hộp thoại mở) |
| KB-30 tiêu chí 2 và 3: tín hiệu bật khi hộp thoại đang mở (L-061) | Compose UI test (Robolectric) | `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction`: tín hiệu `false`, gate mở, `start()`: `cards() == 1`; `adFree.value = true`, `tapWatch()`: `granted == 1`, `cards() == 0`, không `Opportunity`, log có `Rewarded gallery_rw_action granted: switched off`. Hôm nay (tín hiệu không nối) cùng bước ra `not_ready`, `granted == 0` |
| Binding ở `:app`, mặc định `false` (câu hỏi 39) | Unit test | `AdFreeModuleTest.default_neverAdFree`: `AdFreeModule.adFreeStatus().adFree.value == false` và `=== AdFreeStatus.NONE`. Chạy ở `testStagingDebugUnitTest` (`app/src/test` dùng cho cả hai biến thể) |
| Binding Hilt có thật (thiếu thì app không build) | Build | `scripts/check.sh`: `:app:assembleStagingDebug` qua (Hilt kiểm đồ thị lúc biên dịch); ghi ở ngoại lệ dưới |
| ADR-0001: core không phụ thuộc `:app` | Build, kiểm diff | `scripts/check.sh` chạy `checkModuleDependencies` (xanh); `git diff --stat origin/main...HEAD -- core/ads/build.gradle.kts` rỗng; không file nào của `core/ads/src/main` import `com.vglstudio.template.di` hay `com.vglstudio.template.BuildConfig` (biên dịch tự chặn vì không có phụ thuộc) |
| Hành vi cũ giữ nguyên | Unit test, Robolectric | Mọi test có sẵn của `:core:ads` và `:app` qua không sửa (mục 3.5) |
| Hướng dẫn nối tín hiệu đọc được | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc mục 5.1, dòng mục 9 của `ads-guide.md` và MC-11, câu mục 12 của `ads-business-rules.md` |
| Mua thật rồi quảng cáo biến mất trên máy | Kiểm tra thủ công (máy thật: khác, dự án mẫu không có mua hàng nên không có gì để chạy; app có gói bỏ quảng cáo kiểm theo dòng mới ở mục 9 của `ads-guide.md`) | Không kiểm ở task này |

**Ngoại lệ viết test trước:** (1) `AdFreeModule.kt` và binding Hilt: phần đồ thị Hilt chỉ kiểm được bằng build `:app:assembleStagingDebug` của `check.sh`, không có test JVM đỏ trước; `AdFreeModuleTest.default_neverAdFree` đỏ ở bước 3 vì chưa có lớp. (2) KDoc và hai tài liệu là chữ. (3) Dự án chưa có test `@HiltAndroidTest`; phiên không thêm hạ tầng đó chỉ để kiểm một binding. (4) `AdFreeGateTest.adFree_consentSdkAndAppsFlyerUnchanged` là test canh giữ hành vi đã có (tiêu chí "vẫn như khi chưa mua"): đồng ý và AppsFlyer không đọc công tắc, nên test xanh cả ở khung rỗng của bước 3 (chỉ đỏ vì biên dịch trước khi có `AdFreeStatus`); câu khẳng định `isEnabled(...) == false` của nó đỏ ở khung rỗng, phần còn lại canh để thay đổi sau này (ticket bỏ hộp thoại khi đã mua) phải sửa test có chủ đích. Không có tệp kịch bản emulator: dự án mẫu cấp `NONE`, nên trên emulator không bật được tín hiệu mà không sửa code.

### 4.1. Chạy thử

Không cần (E dưới 2). Mẫu giao diện ở core và `@Provides` ở `:app` đã có (`AdUnitIds`, `AppsFlyerConfig`); chỗ chưa chắc (tham số mặc định trong constructor `@Inject`) có phương án dự phòng ở mục 3.4. Máy viết plan hôm nay không có JDK; không chạy `./gradlew` hay `scripts/check.sh`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (thêm điều kiện tắt mọi vị trí và định dạng; điểm 1), thứ tự khởi tạo (lần phát đầu của công tắc, nên màn loading và lần chọn quảng cáo mở app, chờ thêm một vế; điểm 5), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, khóa hay sự kiện. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm đã quyết ở câu hỏi 39.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app`), đổi constructor của `DefaultAdsFlags`, thêm giao diện công khai; ít dòng code nhưng chạm công tắc mà mọi định dạng đọc |
| B. Độ rõ của yêu cầu | 1 | Câu hỏi 39 đã trả lời; AI tự chọn `StateFlow` thay `Flow` và giữ quảng cáo toàn màn hình đã nạp (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường có test JVM hay Robolectric với tín hiệu đổi giữa chừng; binding Hilt chỉ kiểm bằng build; mua thật không kiểm được trong dự án mẫu |
| D. Ảnh hưởng chéo | 3 | Công tắc tổng là thứ mọi feature có quảng cáo đang dựa vào; T-054, T-061, T-062, T-068 tới T-071 cùng đợt sửa `AdsController.kt`, `ads-guide.md`, `ads-business-rules.md` (mục 6) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu `AdUnitIds` và công tắc `is_show_ads`; tham số mặc định trong constructor `@Inject` chưa có trong dự án (mục 3.4) |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn; tổng cũng từ 9) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsFlags.kt` (mọi đường quảng cáo đọc), `AdsController.kt` (chỉ KDoc của `isEnabled`), `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`; file mới `AdFreeStatus.kt`, `app/.../di/AdFreeModule.kt`.
- Task khác có thể xung đột: các ticket đợt 4 (T-053 tới T-073) cùng sửa `ads-business-rules.md` và `ads-guide.md`; T-055 (hiện toàn màn hình an toàn) sửa `AdsController.kt` và `ads-business-rules.md`; T-058 (máy thử và log từng mạng) và T-059 (native trong danh sách thử lại) chạm đường nạp của `:core:ads` và tài liệu quảng cáo; T-054 (dừng quảng cáo khi bị che), T-061 (thanh đáy chừa chỗ lúc tải) và T-062 (`banner_mrec`) sửa `BottomAdBar.kt`, `AdSlot.kt`, `AdsController.kt`, có thể cả `BottomAdBarTest.kt`; T-068 tới T-071 (MAX) thêm đường nạp và hiện mới cho toàn màn hình và banner. Đường mới của MAX phải đọc công tắc qua `AdsController.isEnabled` (KDoc đã nói "every ad format ... must check this"); nếu plan MAX đọc `is_show_ads` thẳng từ `AdsSettings` thì cổng bị vượt. Đề xuất người duyệt plan MAX kiểm điều này.
- Phương án dự phòng của mục 3.4 (bỏ giá trị mặc định của constructor, dùng `testAdsFlags`): nếu phiên dùng phương án này thì mọi chỗ gọi `DefaultAdsFlags(settings)` một tham số không biên dịch, kể cả test mà T-068 (chạy sau, cùng danh sách RUN-20261030-01) viết sẵn trong plan của nó (`AdsControllerMaxTest` dựng `testAdsController(..., flags = DefaultAdsFlags(settings))`, các test mới của `AdsFlagsTest`). Khi đó phiên T-068 đổi các chỗ gọi đó sang `testAdsFlags(settings)` hay `DefaultAdsFlags(settings, AdFreeStatus.NONE)`: cập nhật dữ liệu test theo tham số plan trước thêm (ADR-0029), không đổi câu khẳng định. Phiên T-063 ghi vào nhật ký (mục 10) nếu đã dùng dự phòng, để phiên T-068 biết.
- Không file nào hai plan cùng **tạo** (L-054): hôm nay chưa plan nào khác tạo `AdFreeStatus.kt` hay `AdFreeModule.kt`.
- `docs/STATUS.md`: không đưa vào PR.
- Đề xuất thứ tự chạy: chạy **trước** T-068 tới T-071, để đường của MAX viết trên công tắc đã có cổng và test `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff` canh luôn vị trí mới. Với T-054, T-055, T-058, T-059, T-061, T-062: độc lập về hành vi; cùng danh sách chạy thì `scripts/run-check.py` báo file chung, phiên sau rebase chỉ gặp xung đột chữ ở hai tài liệu và `BottomAdBarTest.kt`. Rà soát độc lập đồng ý: T-063 trước T-068 tới T-071, plan MAX đọc công tắc qua `isEnabled`.

## 7. Điều kiện dừng riêng

- Cách làm buộc phải thêm nhánh `if (adFree)` trong `AdsController`, composable hay `RewardedTurns` vì một đường nào đó không đọc công tắc qua `AdsFlags`: dừng, báo đường đó. Plan dựa trên việc cổng nằm ở một chỗ; đường vượt cổng là lỗi riêng cần plan riêng (có thể cũng vượt `is_show_ads`).
- Một test có sẵn đỏ khi tín hiệu là `NONE`: dừng; plan hứa không đổi hành vi khi tín hiệu tắt (KB-30 tiêu chí 4).
- Cần lưu trạng thái mua trong `:core:ads` hay `:core:data`, gọi Play Billing, hay thêm khóa Remote Config: dừng, trái câu hỏi 39.
- Cần `:core:ads` phụ thuộc `:app` hay sửa `core/ads/build.gradle.kts`: dừng (ADR-0001).

## 8. Câu hỏi cho người duyệt

1. **`StateFlow<Boolean>` hay `Flow<Boolean>`?** Câu hỏi 39 nói "luồng dữ liệu". Với `Flow`, nếu app phát giá trị đầu chậm (đọc DataStore, hỏi Play) thì công tắc của mọi vị trí chờ theo, và `awaitStartupAd` chờ không giới hạn (`startupChoiceKnown.first { it }`), nên màn loading có thể đứng. Với `StateFlow`, `:core:ads` không bao giờ chờ; app phải chọn giá trị đầu, thường là trạng thái đã lưu lần trước (người vừa mua trên máy khác có thể thấy quảng cáo tới khi app phát `true`). **Đề xuất của AI:** `StateFlow`, ghi trong KDoc và `ads-guide.md` mục 5.1.
2. **Quảng cáo toàn màn hình đã nạp sẵn lúc người dùng vừa mua: hủy ngay hay để hết hạn?** Hôm nay `is_show_ads` tắt cũng giữ chúng trong bộ nhớ (không hiện, hết hạn sau 1 giờ theo A-07, hay khi process kết thúc). Hủy ngay cần code mới trong controller (đường riêng cho tín hiệu), ngược hướng "cổng ở một chỗ". **Đề xuất của AI:** để như `is_show_ads` tắt, không hủy; không có lượt hiện nào nên không có doanh thu hay lượt hiển thị sai. Quảng cáo trong màn hình (banner, native, pool) vẫn bị hủy ngay như hôm nay khi vị trí tắt.
3. **Hộp thoại rewarded đang mở khi tín hiệu bật.** Hộp thoại không tự đóng; người dùng bấm "Xem quảng cáo" thì hành động làm thẳng, nhưng sự kiện `af_rewarded_ad_eligible` của AppsFlyer (T-033) vẫn ghi ở lần bấm đó, như khi `is_show_ads` tắt giữa chừng. Trường hợp rất hiếm (mua trong lúc hộp thoại đang mở). **Đề xuất của AI:** giữ như vậy, không thêm code; ghi một câu trong MC-11.
4. **Thử trên staging.** Dự án mẫu không có cách bật tín hiệu ngoài sửa `AdFreeModule` trên máy dev. Có muốn thêm một công tắc trên màn xem thử của staging (`AdGalleryScreen`) không? Đó là thay đổi giao diện của staging, cần thêm file và ảnh chuẩn. **Đề xuất của AI:** không làm trong ticket này; nếu cần thì một ticket nhỏ sau, cùng plan làm mua hàng của app đầu tiên.
5. **Điểm chặn.** Plan ghi `[ads, init-order, core]` theo rủi ro sơ bộ của ticket (điểm 1, 5, 7). Thứ tự khởi tạo chỉ bị chạm qua lần phát đầu của công tắc; nếu anh chọn `StateFlow` ở câu 1 thì điểm 5 gần như không còn tác động. **Đề xuất của AI:** vẫn giữ cả ba, mức Cao.
6. **Người đã mua gói bỏ quảng cáo có còn thấy hộp thoại đồng ý của UMP, và SDK quảng cáo, AppsFlyer có còn khởi động không? Đây là quyết định sản phẩm, cần anh Nhật trả lời.** "Như `is_show_ads` tắt" giữ cả ba: `MainActivity.kt:38` gọi `ads.gatherConsent(this)` không điều kiện, sau đó `MobileAds.initialize` chạy, AppsFlyer khởi động theo `awaitAttributionStart` (T-033). Nên một người đã trả tiền bỏ quảng cáo vẫn có thể thấy hộp thoại đồng ý quảng cáo GDPR ở vùng có luật. Phương án: (a) giữ như plan: chỉ không tải, không hiện; đồng ý, SDK và AppsFlyer như hôm nay (ít rủi ro nhất cho thứ tự khởi tạo; người đó rút mua thì quảng cáo chạy lại ngay); (b) tín hiệu `true` lúc `gatherConsent` thì không mở hộp thoại, không gọi `MobileAds.initialize`; AppsFlyer vẫn khởi động sau 10 giây như khi không có form (cần sửa `AdsController.start`, `ConsentCoordinator`, thêm test thứ tự khởi tạo, ticket lớn hơn); (c) như (b) nhưng làm ở ticket sau. **Đề xuất của AI:** (c): ticket này giữ (a), đúng câu trả lời của câu hỏi 39; ghi thêm câu hỏi mới vào spec ads (mục 9) để anh quyết (b) cho ticket sau. Trả lời ghi vào KB-30 (một tiêu chí mới, kèm "Thêm" và ngày) và vào câu MC-11 của `ads-business-rules.md` (đồng ý, SDK, AppsFlyer khi đã mua). Nếu anh chọn (b) ngay trong ticket này thì plan phải có phiên bản 2 (thêm file, test thứ tự khởi tạo, chấm lại rủi ro).

## 9. Quyết định duyệt

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt lại trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt."), cùng năm plan khác sửa theo rà soát độc lập danh sách chạy RUN-20261030-01, sau khi xem tóm tắt các điểm đã sửa và câu hỏi AI gửi trong phiên. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Các trả lời ở mục 9 của bản trước giữ nguyên. Chấp nhận mức kiểm của test `AdFreeGateTest.adFree_consentSdkAndAppsFlyerUnchanged`: vế AppsFlyer có test thật; vế hộp thoại đồng ý giữ bằng kiểm `ConsentCoordinator.kt`, `MainActivity.kt` không đổi, vì UMP thật không chạy trong test JVM.

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt các plan."), cùng 11 plan khác của đợt 4 quảng cáo, sau khi xem bảng tóm tắt plan và câu hỏi AI gửi trong phiên. Mọi câu ở mục 8 theo đề xuất của AI. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Trả lời: (1) `StateFlow<Boolean>`. (2) quảng cáo toàn màn hình đã nạp sẵn không hủy, như `is_show_ads` tắt. (3) giữ, ghi một câu trong MC-11. (4) không thêm công tắc trên màn xem thử trong ticket này. (5) giữ `[ads, init-order, core]`, mức Cao. (6) phương án (c): ticket này giữ (a), đồng ý, SDK và AppsFlyer như hôm nay; câu hỏi 50 và tiêu chí mới của KB-30 đã ghi vào spec (PR #313); bỏ hộp thoại và không khởi động SDK khi đã mua là ticket sau.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
