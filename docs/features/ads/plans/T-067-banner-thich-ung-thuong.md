---
ticket: T-067
title: Banner cố định dùng cỡ thích ứng neo đáy thường
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5.4M         # cỡ S theo ticket, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29); một hàm mới, một chỗ gọi, không ghi lại ảnh chuẩn, nên giữ đúng số gợi ý
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-067: BANNER CỐ ĐỊNH DÙNG CỠ THÍCH ỨNG NEO ĐÁY THƯỜNG

## 1. Tóm tắt

Banner cố định (định dạng `banner`, cả khi đứng riêng qua `AdSlot` lẫn khi là lớp banner của thanh đáy) đổi từ `AdSize.getLargeAnchoredAdaptiveBannerAdSize` sang `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`, đúng hàm JBase gọi (ADR-0038, quyết định 3; KB-18, KB-32). Banner thu gọn giữ cỡ lớn như hôm nay (ticket để ngoài phạm vi, câu hỏi 2). Cách chọn cỡ tách thành một hàm thuần theo định dạng và một hàm gọi SDK, có test cho cả hai, và một test Compose kiểm chiều cao mà khe banner của thanh đáy báo lên để chừa chỗ bằng đúng cỡ mới.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` ngày 2026-10-29):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-18 | **Thêm 2026-10-29 (ADR-0038):** banner cố định dùng cỡ thích ứng neo đáy thường như JBase (KB-32) | Mọi banner định dạng `AdFormat.BANNER` lấy cỡ bằng `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)`, với `widthDp` là bề rộng của khe như hôm nay. Gồm banner riêng (`AdSlot` của vị trí `banner`) và lớp banner của thanh đáy (`BottomBannerSlot`, luôn dùng `AdFormat.BANNER`) |
| KB-18 | Khi người dùng rút đồng ý lúc banner đang hiện thì khung banner giữ nguyên chiều cao (B-06) | Không đổi cách giữ khung; khung giữ chiều cao mới. Test có sẵn `BannerSlotHostTest` và `BottomAdBarTest.consentWithdrawn_bannerWithAd_barAndReservationKept` truyền chiều cao vào, không đọc cỡ của SDK, nên giữ nguyên |
| KB-18 | Banner thu gọn được tải lại sau mỗi 30 giây (B-03) | Không đổi. Banner thu gọn giữ `getLargeAnchoredAdaptiveBannerAdSize` (ticket: "Không bao gồm: banner thu gọn"); câu hỏi 2 |
| KB-32 | Banner cố định dùng cỡ thích ứng neo đáy thường, như JBase, thay cỡ thích ứng neo đáy lớn (B-01) | Như dòng đầu. Sửa B-01 của `ads-business-rules.md` kèm ADR-0038 và nguồn JBase |
| KB-17 | Nội dung chừa đúng chiều cao của khe đáy (câu hỏi 37 của spec: banner bằng chiều cao khung adaptive đã biết trước khi tải) | Khe banner của thanh đáy báo chiều cao của cỡ mới qua `onHeight` (đã có, `AdSlot.kt` dòng 95); `bottomBarReservation` dùng số đó. Test mới kiểm số báo lên bằng chiều cao của cỡ thường |

Nguồn JBase (đọc bytecode của `Assets/Plugins/Android/admobadshelper-release.aar` trong gói JBase Unity 1.0.0, bằng `javap -c`, ngày 2026-10-29):
- `com.jb.admobadshelper.internal.AdLayoutUtils.getAdaptiveBannerSize(Activity)`: bề rộng là `DisplayMetrics.widthPixels / density` của màn hình, rồi `invokestatic AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(Context, int)` (offset 48), với `Activity.getApplicationContext()`.
- `com.jb.admobadshelper.controllers.BannerAdController.lambda$init$1(Activity)`: gọi `AdLayoutUtils.getAdaptiveBannerSize` (offset 18) và lưu `AdSize.getHeight()` vào `bannerHeightDp`. Cùng một `AdView` cho banner thường và banner thu gọn (cờ `isCollapsible` chỉ đổi yêu cầu), nên JBase dùng cỡ thường cho cả banner thu gọn (câu hỏi 2).
- Khác JBase: ta lấy bề rộng của khe (`BoxWithConstraints`, bằng bề rộng màn hình khi khe rộng hết màn) và `LocalContext` của màn (Activity, cần cho Unity và ironSource, `ads-guide.md` mục 7). Giữ như hôm nay, không đổi.

**SDK `play-services-ads` 25.5.0** (`gradle/libs.versions.toml`, `playServicesAds`), đọc `AdSize.class` của `play-services-ads-api-25.5.0.aar` trong bộ nhớ đệm Gradle bằng `javap -v`, ngày 2026-10-29:
- `getCurrentOrientationAnchoredAdaptiveBannerAdSize`, `getPortraitAnchoredAdaptiveBannerAdSize`, `getLandscapeAnchoredAdaptiveBannerAdSize` đều có `Deprecated: true` và `@java.lang.Deprecated`. Ba hàm gọi cùng một hàm nội bộ với chiều cao tối thiểu 50 dp.
- Không bị đánh dấu: `getLargeAnchoredAdaptiveBannerAdSize` (và bản `Portrait`, `Landscape`), các hàm inline adaptive (`getCurrentOrientationInlineAdaptiveBannerAdSize`, `getInlineAdaptiveBannerAdSize(width, maxHeight)`).
- Kết luận: SDK 25.5.0 **không có hàm thay không bị đánh dấu** cho cỡ neo đáy thường. Hàm Google đưa ra thay là chính cỡ lớn, tức cỡ ticket muốn bỏ. Cỡ inline adaptive là loại khác (cho banner nằm trong nội dung cuộn, chiều cao do quảng cáo quyết tới mức tối đa), không tương đương. Vì vậy plan gọi hàm `getCurrentOrientationAnchoredAdaptiveBannerAdSize` như JBase, bọc trong `@Suppress("DEPRECATION")` ở đúng một hàm, KDoc ghi lý do (câu hỏi 1). Build không coi cảnh báo là lỗi (`build-logic`: lint `warningsAsErrors = false`, không có `allWarningsAsErrors` cho Kotlin), nên cảnh báo không làm đỏ `check.sh`.

Thiết kế tham chiếu: không có thiết kế riêng cho banner. Ảnh trong `design/screens/` không có banner cố định thật (xem mục 3.2, ảnh chuẩn).

ADR liên quan: ADR-0038 (quyết định 3, Việc AI phải tuân thủ: ghi luật đổi vào `ads-business-rules.md` kèm mã ADR và nguồn JBase), ADR-0026, ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

1. **Tạo `BannerAdSize.kt`** trong `:core:ads`:
   - `internal enum class BannerSizeKind { ANCHORED, LARGE_ANCHORED }`.
   - `internal fun bannerSizeKind(format: AdFormat): BannerSizeKind`: `AdFormat.BANNER` trả `ANCHORED` (B-01, ADR-0038); `AdFormat.BANNER_COLLAPSIBLE` trả `LARGE_ANCHORED` (B-03, giữ như hôm nay); định dạng khác ném `IllegalArgumentException("$format is not a banner format")`, cùng câu với `bannerRefreshOf`. Hàm thuần, theo mẫu `bannerRefreshOf` ở `BannerLoads.kt`.
   - `internal fun bannerAdSize(context: Context, format: AdFormat, widthDp: Int): AdSize`: theo `bannerSizeKind(format)`, `ANCHORED` gọi `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)`, `LARGE_ANCHORED` gọi `AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)`. Hàm có `@Suppress("DEPRECATION")`; KDoc ghi: cỡ thường theo JBase (`AdLayoutUtils.getAdaptiveBannerSize`), SDK 25.5.0 đánh dấu bỏ hàm này mà không có hàm thay cho cỡ thường, hàm thay của Google là cỡ lớn; khi nâng SDK mà hàm bị xóa thì cần quyết lại B-01.
2. **Sửa `AdSlot.kt`, `BannerSlot`** (dòng 94): `val adSize = remember(widthDp, format) { bannerAdSize(context, format, widthDp) }`. Phần còn lại giữ nguyên: `LaunchedEffect(adSize) { currentOnHeight(adSize.height.dp) }` báo chiều cao mới cho thanh đáy, `key(adSize)` dựng lại khung khi cỡ đổi, `heightDp = adSize.height` đặt chiều cao khung, `setAdSize(adSize)` cho `AdView`. Sửa KDoc của `AdSlot` và `BottomBannerSlot`: banner cố định cỡ thích ứng neo đáy thường (B-01), banner thu gọn cỡ lớn. Bỏ import `AdSize` nếu không còn dùng trong file (ktlint).
3. **Khung chừa chỗ theo chiều cao mới:** không cần code mới. `BottomAdBarScaffold` nhận chiều cao qua `onHeight` của `BottomBannerSlot` rồi `bottomBarReservation(BANNER, slotHasAd, bannerHeight)`; banner riêng dùng `heightDp` cho `Modifier.height` của khung. Cả hai lấy từ `adSize` của bước 2.
4. **Sửa `docs/ops/ads-business-rules.md`, B-01:** thêm vào cuối luật: "**Đổi 2026-10-29 (ADR-0038):** cỡ thích ứng neo đáy thường theo hướng màn hình hiện tại (`AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`), như JBase, thay cỡ neo đáy lớn (`getLargeAnchoredAdaptiveBannerAdSize`). Banner thu gọn (B-03) giữ cỡ lớn. SDK 25.5.0 đánh dấu bỏ hàm cỡ thường và không có hàm thay." Cột nguồn thêm `JBase admobadshelper-release.aar: AdLayoutUtils.getAdaptiveBannerSize (offset 48)`. Giữ chữ cũ của luật.
5. **Sửa `docs/ops/ads-guide.md`** dòng 46 (bảng định dạng) và câu "banner cố định neo đáy" của mục thanh đáy (dòng 127): thêm "cỡ thích ứng neo đáy thường (ADR-0038)"; banner thu gọn ghi "cỡ neo đáy lớn".

Không đổi: `BannerFrame.kt`, `BannerAdView.kt`, `BannerLoads.kt`, `BottomAdBar.kt`, cách nạp, làm mới, thử lại, cổng đồng ý, số liệu.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerAdSize.kt` | `BannerSizeKind`, `bannerSizeKind(format)`, `bannerAdSize(context, format, widthDp)` (mục 3.1 bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerSlot` gọi `bannerAdSize`, khóa `remember` thêm `format`; KDoc của `AdSlot`, `BottomBannerSlot` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerAdSizeTest.kt` | Test của mục 4 |
| Sửa | `docs/ops/ads-business-rules.md` | B-01: ghi chú `Đổi 2026-10-29 (ADR-0038)` và nguồn JBase |
| Sửa | `docs/ops/ads-guide.md` | Dòng 46 và mục thanh đáy (dòng 127): cỡ của banner cố định và banner thu gọn |

**Ảnh chuẩn: không ghi lại ảnh nào.** Ticket ghi "ghi lại ảnh chuẩn có banner (liệt kê trong plan)"; đã tìm và không ảnh nào vẽ cỡ banner của SDK:
- Mọi screenshot test chạy không có `LocalAdsController`, nên `BannerSlot` chỉ vẽ `Box(modifier)` rỗng (`AdSlot.kt` dòng 79 tới 82) và không gọi `AdSize`.
- `app/src/test/screenshots/AdGalleryScreen_banners_light.png`, `AdGalleryScreen_banners_small.png`, `AdGalleryScreen_collapsible-*.png` vẽ banner giả `PreviewBanner` cao cố định 60 dp (`app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PreviewAdGallerySlots.kt` dòng 66), không đọc cỡ của SDK.
- `core/ads/src/test/screenshots/BottomAdBar_collapsed_light.png`, `BottomAdBar_expanded_light.png` vẽ khe native giả, không có banner.
- `docs/features/ads/design/screens/`: không ảnh nào có banner cố định thật.

Nên mọi ảnh chuẩn giữ nguyên; ảnh nào đổi là dấu hiệu sai (mục 7). Có nên đổi chiều cao 60 dp của banner giả không: câu hỏi 3.

**Nơi khác đọc file plan đổi (L-020):** `grep -rln "ads-business-rules\|ads-guide" scripts/ .github/ */src/test` chỉ ra `scripts/ads-plan.py`. Script chỉ ghi tên hai tài liệu trong chữ hướng dẫn và có bảng định dạng chép tay; dòng `banner` ghi "Thích ứng theo chiều rộng màn hình, neo ở đáy, cao theo nội dung" (dòng 122), vẫn đúng với cỡ thường, nên không sửa. `AdSlot.kt` không được script hay workflow nào đọc. Các test sau nhắc `ads-business-rules.md` chỉ trong chú thích, chép tay mục khác của tài liệu (MC-02, mục S, 4, 5.7, 8.1 tới 8.3, 10), không đọc file lúc chạy và không chạm B-01, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Không thêm khóa Remote Config (JBase không có khóa cho cỡ banner).
- Quảng cáo: đổi kích thước của định dạng `banner` đã có (điểm chặn 1). Không thêm, bớt hay dời vị trí; không đổi tần suất, nhịp làm mới, mã đơn vị. Banner thu gọn `banner_collapsible` không đổi.
- Sự kiện phân tích: Không chạm. Sự kiện của banner không có tham số kích thước.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize` thuộc `play-services-ads` (khóa `play-services-ads` của `gradle/libs.versions.toml`, phiên bản `playServicesAds = "25.5.0"`), đã có trên classpath biên dịch của `:core:ads` (`AdSlot.kt` đã import `com.google.android.gms.ads.AdSize`). Hàm bị SDK đánh dấu bỏ (mục 2).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** cảnh báo hàm bị bỏ có làm đỏ một bước của `scripts/check.sh` không. Đã kiểm cấu hình: Kotlin không bật `allWarningsAsErrors`, lint `warningsAsErrors = false`; chưa chạy `check.sh` (máy không có JDK lúc viết plan, và E dưới 2). Ngoài ra chưa biết Robolectric có tính hai cỡ khác nhau không (mục 4, câu kiểm tiền đề).
- **Điều kiện chuyển:** `check.sh` đỏ ở biên dịch, ktlint hay lint với lỗi chỉ vào lời gọi hàm bị bỏ hay vào `@Suppress("DEPRECATION")` (ví dụ ktlint đòi đặt chú thích ở chỗ khác).
- **Cách làm thay:** đặt `@Suppress("DEPRECATION")` ở mức file (`@file:Suppress("DEPRECATION")` đầu `BannerAdSize.kt`, file chỉ chứa ba khai báo của mục 3.1) thay vì ở hàm, hoặc ngược lại, theo điều công cụ đòi. Không đổi sang hàm khác: không có hàm không bị bỏ cho cỡ thường (mục 2), và cỡ inline hay cỡ tự tính bằng `AdSize(width, height)` đổi B-01. Nếu không cách đặt chú thích nào qua thì dừng theo điều kiện 3 (B-01 mới không làm được với công cụ hiện tại). Test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

`python3 -I scripts/existing-tests.py docs/features/ads/plans/T-067-banner-thich-ung-thuong.md` (2026-10-29) liệt kê file `AdSlot.kt` và 0 test có sẵn dùng tên hay tag của nó; nhóm "Cần xem trước" và "Nên xem" đều 0 (`BannerAdSize.kt` là file mới). Đã đọc thêm theo tay các test của banner và thanh đáy truyền chiều cao vào `BannerSlotHost` và `BottomAdBarLayout`, ghi ở bảng dưới.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Banner cố định (riêng và lớp của thanh đáy) cỡ `getLargeAnchoredAdaptiveBannerAdSize` | Cỡ `getCurrentOrientationAnchoredAdaptiveBannerAdSize` (bước 1, 2) | Không có: không test nào gọi `BannerSlot` với `AdsController`, và không test nào đọc `AdSize` (`grep -rn "AdSize\." core/ads/src/test app/src/test*`: chỉ `NativeAdSize`) | Test mới ở mục 4 |
| Chiều cao khung và khoảng chừa của banner bằng chiều cao do `BannerSlot` truyền vào | Không đổi cách truyền; chỉ số truyền vào đổi | `BannerSlotHostTest` (`heightDp = HEIGHT_DP`, `assertSlotHeight`), `BottomAdBarTest` (`BANNER_HEIGHT_DP`, `bottomBarReservation(..., bannerHeight = 62.dp)`, `consentWithdrawn_bannerWithAd_barAndReservationKept`, `bannerUnitIdNull_noLoad_noReservation`) | Giữ: các test truyền số cố định, không qua `bannerAdSize` |
| Banner thu gọn cỡ lớn, nạp lại 30 giây | Không đổi | `BannerLoadsTest`, `SdkBannerAdViewTest` (yêu cầu có `collapsible`) | Giữ |
| Định dạng không phải banner thì `AdSlot` ném lỗi | Không đổi (`bannerRefreshOf` vẫn ném trước) | Test của `bannerRefreshOf` ở `BannerLoadsTest` | Giữ |

## 4. Kiểm thử

Test viết trước. Bước 3 tạo khung rỗng của `BannerAdSize.kt`: `bannerSizeKind` trả `LARGE_ANCHORED` cho mọi định dạng banner (đúng hành vi hôm nay) và `bannerAdSize` gọi cỡ lớn, `BannerSlot` chưa gọi `bannerAdSize`. Test của dòng KB-18 đỏ vì cỡ, không vì biên dịch.

`BannerAdSizeTest` (`:core:ads`, `@RunWith(AndroidJUnit4::class)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")` cho cả lớp, `createAndroidComposeRule<ComponentActivity>()`; `activity` là `composeRule.activity`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-18, KB-32: banner cố định cỡ neo đáy thường | Unit test | `fixedBanner_kindIsAnchored`: `assertEquals(BannerSizeKind.ANCHORED, bannerSizeKind(AdFormat.BANNER))`. Hôm nay (khung rỗng) đỏ: `LARGE_ANCHORED` |
| Banner thu gọn giữ cỡ lớn (ngoài phạm vi, B-03) | Unit test | `collapsibleBanner_kindStaysLarge`: `assertEquals(BannerSizeKind.LARGE_ANCHORED, bannerSizeKind(AdFormat.BANNER_COLLAPSIBLE))`. Đối chứng là test dòng trên: hai định dạng khác cỡ |
| Định dạng không phải banner | Unit test | `nonBannerFormat_throws`: với mọi `AdFormat` khác `BANNER`, `BANNER_COLLAPSIBLE`, `assertThrows(IllegalArgumentException::class.java) { bannerSizeKind(it) }`. Khi T-062 thêm `BANNER_MREC`, task đó thêm nhánh và loại định dạng đó khỏi vòng (mục 6) |
| Tiền đề của test cỡ: Robolectric tính hai cỡ khác nhau | Unit test (Robolectric) | `sdkSizes_anchoredDiffersFromLarge`: ở bề rộng 360 và 411 dp, `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, w).height != AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, w).height`, và cỡ thường cao ít nhất 50 dp. Qua cả trên khung rỗng (kiểm SDK, không kiểm code). Đỏ thì các test so cỡ dưới đây không phân biệt được hai cỡ: phiên ghi số đo vào nhật ký và báo người, vẫn dựa vào `fixedBanner_kindIsAnchored` |
| KB-18: `bannerAdSize` gọi đúng hàm của SDK | Unit test (Robolectric) | `bannerAdSize_fixed_matchesAnchored`: với `w` là 320, 360, 411, 600, `bannerAdSize(activity, AdFormat.BANNER, w)` có `width`, `height` bằng `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, w)`; đối chứng `height` khác cỡ lớn ở 411 dp. `bannerAdSize_collapsible_matchesLarge`: `AdFormat.BANNER_COLLAPSIBLE` bằng `getLargeAnchoredAdaptiveBannerAdSize`. Hôm nay đỏ ở câu đầu |
| KB-17, KB-18: khung chừa chỗ theo chiều cao mới | Compose UI test | `bottomBannerSlot_reportsAnchoredHeight`: `setContent { CompositionLocalProvider(LocalAdsController provides testAdsController()) { Box(Modifier.width(411.dp)) { BottomBannerSlot(AdPlacement.SAMPLE_NATIVE_SMALL, onAdAvailabilityChange = {}, onHeight = { reported += it }) } } }`, `waitForIdle()`. Kiểm: `reported.last()` bằng `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, 411).height.dp`; đối chứng: khác `getLargeAnchoredAdaptiveBannerAdSize(activity, 411).height.dp`. Chiều cao được báo trước cổng đồng ý (`LaunchedEffect(adSize)` ở `BannerSlot`), nên không cần đồng ý hay quảng cáo; `testAdsController` không có SDK nên không gửi yêu cầu. Hôm nay đỏ: số báo lên là chiều cao cỡ lớn |
| Ảnh chuẩn không đổi | Screenshot test | `scripts/check.sh` (bước verify của Roborazzi) qua mà không ghi lại ảnh nào; `git status --short -- '*/src/test/screenshots/'` trống |
| Cỡ banner thật trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging trên điện thoại dọc, đồng ý quảng cáo, mở màn xem thử, trang "Banners": `adb shell uiautomator dump`, đọc `bounds` của `AdView` của banner cố định (dưới tiêu đề); chiều cao (px chia mật độ) nằm trong 50 tới 90 dp, thấp hơn banner thu gọn ở đáy khi chưa bung (cỡ lớn). Tắt `is_show_native_bottom` trên Firebase staging, trang "Collapsible ad": lớp banner của thanh đáy cùng chiều cao đó, nội dung danh sách không bị che (khoảng chừa bằng khung). Ghi hai số vào nhật ký |
| Tài liệu B-01, `ads-guide.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc ghi chú B-01 và hai dòng của `ads-guide.md` |

Banner riêng (`AdSlot` của vị trí `banner`) không có test đo chiều cao riêng: khi cổng đồng ý đóng, khung là `NONE`, cao 0, nên không đo được cỡ. Banner riêng và lớp banner của thanh đáy đi qua cùng hàm `BannerSlot`; `bottomBannerSlot_reportsAnchoredHeight` kiểm hàm đó với `AdFormat.BANNER`, các test `bannerAdSize_*` kiểm phần theo định dạng.

Không có tệp kịch bản emulator: `device-session` từ chối đồng ý nên không có banner để đo.

**Ngoại lệ viết test trước:** tài liệu (`ads-business-rules.md`, `ads-guide.md`) và KDoc. `sdkSizes_anchoredDiffersFromLarge` qua ngay trên khung rỗng vì nó kiểm tiền đề của SDK trên Robolectric, không kiểm code của plan.

### 4.1. Chạy thử

Không cần (E dưới 2). Đổi một lời gọi `AdSize` sang hàm cùng họ, theo mẫu `bannerRefreshOf`; không thư viện, kỹ thuật hay module mới. Đã đọc bytecode của SDK 25.5.0 (mục 2) để chọn hàm; chưa chạy Gradle.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**
- quảng cáo, điểm 1: đổi kích thước của định dạng `banner` đã có;
- module core dùng chung `:core:ads`, điểm 7.

Không thêm vị trí, quyền, SDK hay dữ liệu. Mức Cao, cần duyệt chéo. Người phụ trách sản phẩm đã chọn đổi (ADR-0038, 2026-10-29); cần biết thêm: banner cố định thấp hơn thì doanh thu mỗi lượt hiển thị có thể đổi (cỡ lớn có nhiều chỗ cho quảng cáo hơn), và hàm SDK bị đánh dấu bỏ (câu hỏi 1).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một module, một file mới, một dòng gọi; không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 1 | Spec, ADR rõ; còn hai chi tiết cần người chốt: dùng hàm bị bỏ (câu hỏi 1), banner thu gọn (câu hỏi 2) |
| C. Khả năng kiểm chứng tự động | 1 | Cách chọn cỡ, lời gọi SDK, chiều cao báo lên đều có test; cỡ thật của quảng cáo chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `AdSlot.kt` dùng cho mọi màn có banner và thanh đáy; T-062 (MREC), T-054, T-071 cũng sửa phần banner; `ads-business-rules.md` nhiều ticket của đợt 4 cùng sửa |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn; lần đầu trong dự án dùng `@Suppress("DEPRECATION")` |
| **Tổng** | 6 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdSlot.kt` (`BannerSlot`, dùng bởi `AdSlot` và `BottomBannerSlot` của `BottomAdBarScaffold`), `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`.
- Không đổi: `BannerFrame.kt`, `BannerAdView.kt`, `BannerLoads.kt`, `BottomAdBar.kt`, `AdsController.kt`, ảnh chuẩn.
- App dùng banner hôm nay: màn xem thử staging (`gallery_banner`, `gallery_banner_collapsible`) và lớp banner của thanh đáy (`SampleScreen`, `gallery_native_small`) khi `is_show_native_bottom` tắt.
- Ticket của đợt 4 cùng vùng: T-062 (banner MREC, thêm `AdFormat.BANNER_MREC`; nên thêm nhánh `MEDIUM_RECTANGLE` vào `bannerSizeKind` hay đi đường riêng, và sửa `nonBannerFormat_throws`), T-054 (banner dừng khi bị che, sửa vòng chạy của banner trong `AdSlot.kt`), T-071 (banner của MAX). T-053, T-064 tới T-066 sửa các dòng khác của `ads-business-rules.md` (K-02, CL-03, D-02, S-12) và `ads-guide.md`. Không file nào hai plan cùng **tạo** (L-054); `BannerAdSize.kt` chỉ plan này tạo.
- `docs/STATUS.md`: không đưa vào PR.
- Đề xuất thứ tự chạy: trước T-062 và T-071, để hai task đó dùng `bannerSizeKind` sẵn có; độc lập với T-053, T-064 tới T-066 (chỉ chung tài liệu, khác dòng).

## 7. Điều kiện dừng riêng

- Ảnh chuẩn nào đổi: dừng, không ghi lại (mục 3.2: không ảnh nào vẽ cỡ của SDK, nên đổi là dấu hiệu sai).
- Phần sửa cần đổi `BannerFrame.kt`, `BannerLoads.kt`, `BottomAdBar.kt` hay cách giữ khung khi rút đồng ý (B-06): dừng và hỏi (ngoài phạm vi).
- Cần đổi cỡ của banner thu gọn để test qua: dừng (câu hỏi 2 chưa trả lời thì giữ cỡ lớn).

## 8. Câu hỏi cho người duyệt

1. **Dùng hàm SDK bị đánh dấu bỏ.** Trong `play-services-ads` 25.5.0, `getCurrentOrientationAnchoredAdaptiveBannerAdSize` (và bản `Portrait`, `Landscape`) bị đánh dấu bỏ; hàm Google đưa ra thay là `getLargeAnchoredAdaptiveBannerAdSize`, chính cỡ ADR-0038 muốn bỏ. Không có hàm không bị bỏ cho cỡ neo đáy thường.
   - Đề xuất của AI: vẫn dùng hàm này như JBase, `@Suppress("DEPRECATION")` ở một hàm, KDoc và B-01 ghi lý do. Khi nâng SDK mà hàm bị xóa thì biên dịch đỏ, lộ ra ngay; lúc đó quyết lại B-01.
   - Phương án khác: (a) giữ cỡ lớn và sửa ADR-0038 (không theo JBase ở điểm này vì SDK đã bỏ); (b) cỡ inline adaptive với chiều cao tối đa bằng cỡ thường: loại quảng cáo khác, không đề xuất.
2. **Banner thu gọn có đổi cỡ không?** JBase dùng cùng một `AdView` và cùng cỡ thường cho banner thường và banner thu gọn (`BannerAdController.lambda$init$1`). ADR-0038, quyết định 3 chỉ nói banner cố định; ticket để banner thu gọn ngoài phạm vi.
   - Đề xuất của AI: giữ cỡ lớn cho banner thu gọn trong task này, đúng phạm vi ticket. Cỡ truyền vào là cỡ lúc thu gọn; phần bung ra do quảng cáo quyết, nên ảnh hưởng nhỏ hơn. Nếu anh muốn theo JBase cả ở đây thì ghi vào ADR-0038 và tách ticket nhỏ: một nhánh của `bannerSizeKind` và một câu test.
3. **Banner giả 60 dp của màn xem thử trong test.** `PreviewBanner` cao 60 dp, gần cỡ thường trên điện thoại dọc, nên không cần đổi.
   - Đề xuất của AI: giữ, không ghi lại ảnh. Ảnh chuẩn không vẽ cỡ thật của SDK.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
