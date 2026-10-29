---
ticket: T-062
title: Định dạng banner MREC
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, analytics, core)
risk_level: high             # low | medium | high
token_estimate: 24M          # ticket cỡ M, số gợi ý 23.9M (scripts/token-usage.py estimate, 2026-10-29); không chỉnh
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-062: ĐỊNH DẠNG BANNER MREC

## 1. Tóm tắt

Thêm định dạng `banner_mrec` (banner 300 x 250 dp, `AdSize.MEDIUM_RECTANGLE`) vào danh sách đóng của `:core:ads` (MC-02, ADR-0036 điểm 3, câu hỏi 38): feature đặt nó trong nội dung của màn bằng `AdSlot(placement)` như banner cố định, đi cùng đường gửi, tạm dừng, thử lại, giữ khung khi rút đồng ý (B-01, B-02, B-04, B-05, B-06) và cùng cờ dừng khi bị quảng cáo toàn màn hình che mà T-054 tạo (KB-27). Khác banner cố định ở ba điểm: khung rộng 300 dp căn giữa, giữ đúng 250 dp lúc đang tải, co về 0 khi nạp lỗi trước khi có quảng cáo. Số liệu dùng mã định dạng mới `BANNER_MREC` ở mọi sự kiện có `ad_format`; staging có vị trí `gallery_banner_mrec` trên trang "Banners" của màn xem thử với mã thử banner cỡ cố định của Google; `scripts/ads-plan.py` có dòng `banner_mrec` trong bảng định dạng. Kiểm chứng bằng test JVM của hàm thuần, test Robolectric của khung banner và của trang "Banners", ghi lại hai ảnh chuẩn của trang đó, test `scripts/tests/run.sh` cho bảng định dạng, và kiểm trên máy thật với quảng cáo thử.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-29; câu hỏi 36 và 38 đã trả lời):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-29 | Khi vị trí bật và có quảng cáo thì màn hình có một banner 300 x 250 dp, căn giữa, có nhãn và AdChoices của SDK | `AdSlot(placement)` của vị trí `banner_mrec` dựng `AdView` cỡ `AdSize.MEDIUM_RECTANGLE` trong khung rộng 300 dp, cao 250 dp, căn giữa theo chiều ngang của chỗ nơi gọi cho. Nhãn và AdChoices do quảng cáo của SDK tự vẽ trong `AdView`, ta không vẽ gì thêm. Chỗ nơi gọi cho hẹp hơn 300 dp thì không dựng khung, không gửi yêu cầu, không chiếm chỗ (giả định, câu hỏi 2 ở mục 8) |
| KB-29 | Banner MREC theo cùng luật với banner cố định: chờ cổng đồng ý, làm mới theo cài đặt của đơn vị trên AdMob, tạm dừng khi màn hình không hiện, thử lại khi lỗi, giữ khung khi rút đồng ý (KB-18), dừng khi bị quảng cáo toàn màn hình che (KB-27) | `bannerRefreshOf(BANNER_MREC)` là `BY_AD_UNIT`; MREC đi qua đúng `BannerSlotHost`, `bannerFrame`, `bannerShouldRun`, `runBannerLoads` của banner cố định, nên mọi đường gửi (lượt đầu, thử lại sau lỗi, gửi lại khi cổng mở lại) qua cùng bước "chờ hiện, hỏi cổng" (L-072); lượt làm mới do SDK tự chạy và dừng bằng `pause()`. Cờ "đang hiện quảng cáo toàn màn hình" và chỗ nó đi vào điều kiện chạy, điều kiện gửi là phần T-054 tạo; MREC không cần code riêng, chỉ cần test riêng |
| KB-29 | Khi đang tải thì vùng giữ đúng 250 dp; khi nạp lỗi thì co về 0 | Khung `ACTIVE` chưa có quảng cáo cao 250 dp. Một lượt nạp lỗi khi khung chưa từng có quảng cáo thì khung cao 0 dp, `AdView` vẫn sống (vẫn thử lại theo B-02, vẫn qua cổng); lượt sau có quảng cáo thì khung về 250 dp. Đã có quảng cáo rồi mà lượt làm mới hay thử lại lỗi thì giữ 250 dp, vì `AdView` vẫn hiện quảng cáo cũ (câu hỏi 3). Banner cố định giữ hành vi hôm nay (khung giữ chiều cao cả khi lỗi) |
| KB-29 | Doanh thu ghi với định dạng riêng của MREC (`ad_format`), vị trí là mã vị trí | `AdFormat.BANNER_MREC.metricCode()` là `BANNER_MREC` (câu hỏi 38): `ad_impression_placement` và các sự kiện vòng đời của Firebase ghi `ad_format = BANNER_MREC`, `af_ad_revenue` ghi `ad_type = BANNER_MREC`; `ad_placement` là mã vị trí, đọc lúc có sự kiện như banner. Nhóm số liệu là nhóm khe; không có phễu AppsFlyer, như banner (mục 8.3) |
| KB-29 | Bản staging có một vị trí MREC trên màn xem thử, dùng mã thử của Google | Vị trí `gallery_banner_mrec` (chỉ staging, `release.sh` bỏ qua tiền tố `gallery_`), đặt trên trang "Banners" giữa banner cố định và danh sách; mã thử banner cỡ cố định của Google `ca-app-pub-3940256099942544/6300978111` (câu hỏi 1). Staging tự bật mọi vị trí (`StagingRemoteValuesModule.everyAdPlacementEnabled()`) |
| KB-1 (qua KB-29, B-01, B-04) | Khi chưa đồng ý hoặc vị trí bị tắt thì không có vùng quảng cáo, không chiếm chỗ; rời màn thì hủy | `bannerFrame` trả `NONE` như banner cố định; test riêng cho khung MREC ở mục 4. Hủy khi rời màn đã có test chung (`leavingComposition_destroysView`) |
| KB-2 (ghi chú 2026-10-29) | Native medium vẫn là lựa chọn mặc định thay MREC; app nào cần banner MREC thật thì dùng `banner_mrec` | Chỉ là tài liệu: `ads-guide.md` mục 3 và `ads-business-rules.md` 5.7 ghi rõ khi nào chọn định dạng nào |

Thiết kế tham chiếu: không có thiết kế riêng. Trang "Banners" của màn xem thử không có ảnh thiết kế (plan T-036, câu hỏi 1); ảnh chuẩn của trang là tham chiếu. Banner MREC do SDK vẽ, ta chỉ đặt khung.

ADR liên quan: ADR-0026 (hành vi theo PARTNER, mục 10 giữ nguyên), ADR-0036 (điểm 3: thêm `banner_mrec`; ghi điểm lệch vào mục 12), ADR-0038 (chuẩn JBase: JBase có MREC của AdMob, đặt nổi; dự án mẫu đặt trong nội dung, câu hỏi 38), ADR-0029, ADR-0031 (phần ngoài mục 3.2, test cũ).

## 3. Giải pháp

### 3.1. Cách làm

Theo mẫu banner của T-029 (`AdSlot`, `BannerSlotHost`, `runBannerLoads`, `SdkBannerAdView`) và mẫu thêm định dạng của T-036 (dòng `gallery_*`, mã thử, bảng khóa sinh lại). Không thêm composable công khai mới: MREC dùng `AdSlot(placement)`.

1. **Định dạng và vị trí** (`AdPlacement.kt`): thêm `AdFormat.BANNER_MREC("banner_mrec")` với KDoc ("banner 300 x 250 dp đặt trong nội dung của màn bằng [AdSlot], làm mới theo đơn vị trên AdMob như [BANNER] (B-07); không vào thanh đáy, không làm ô trong danh sách"). Thêm dòng `GALLERY_BANNER_MREC("gallery_banner_mrec", AdFormat.BANNER_MREC),` ngay sau `GALLERY_BANNER_COLLAPSIBLE`, trên một dòng (`AdPlacementSourceTest`). Không thêm `BANNER_MREC` vào `NATIVE_EVERY_FORMATS` hay `APP_WIDE_FORMATS`.
2. **Các `when` không có `else`** (trình biên dịch bắt): thêm `AdFormat.BANNER_MREC` vào nhánh "không phải native, không có pool, không có khe toàn màn hình" ở `RewardedRules.kt` (`fullScreenLoaderOf`, ném lỗi), `NativeAdPool.kt` (`poolCapacity`, `null`), `AdsController.kt` (`nativeLoadSize`, `null`), `AdPreloadPlan.kt` (`isNative`, `false`). Banner không có khe nạp sẵn (`preloadSlots` chỉ có vị trí toàn màn hình và pool), nên `AdPreloadPlan.fromPlacements()` có thêm `BANNER_MREC` ở nhóm sau mà không làm gì; `templatePlan_hasAppOpenFirst` chỉ kiểm nhóm đầu.
3. **Số liệu** (`AdTelemetryRules.kt`): `metricCode()` thêm `AdFormat.BANNER_MREC -> "BANNER_MREC"`; `funnelKind()` thêm vào nhánh `null` cùng `BANNER`; `loadPlacementOf` thêm vào nhánh của banner (`screenPlacement?.key`); `metricGroup()` không đổi (nhánh `else` là nhóm khe). `:core:analytics` truyền nguyên chuỗi `adFormat` (`AppsFlyerEvents.kt`, `ad_type`; Firebase `ad_format`), không có bảng mã nào phải sửa ở code chính.
4. **Mã thử** (`AdUnitIds.kt`): `GoogleTestAdUnitIds.unitFor` thêm `AdFormat.BANNER_MREC -> "ca-app-pub-3940256099942544/6300978111"` (đơn vị banner cỡ cố định của Google, dùng cho MREC; chú thích một dòng). `ConfiguredAdUnitIds` không đổi: vị trí `gallery_` không có mã production; vị trí MREC của app cần mã thật trong `release.properties`.
5. **Cỡ, khung và chiều cao: hàm thuần** (`BannerFrame.kt`):
   - `internal enum class BannerSize { ANCHORED_ADAPTIVE, MEDIUM_RECTANGLE }`.
   - `internal fun bannerSizeOf(format: AdFormat): BannerSize`: `BANNER`, `BANNER_COLLAPSIBLE` là `ANCHORED_ADAPTIVE`, `BANNER_MREC` là `MEDIUM_RECTANGLE`, định dạng khác ném `IllegalArgumentException` (như `bannerRefreshOf`).
   - `internal const val MEDIUM_RECTANGLE_WIDTH_DP = 300`, `MEDIUM_RECTANGLE_HEIGHT_DP = 250` (bằng `AdSize.MEDIUM_RECTANGLE`; test so hai giá trị).
   - `internal fun bannerFitsWidth(size: BannerSize, availableWidthDp: Int): Boolean`: `ANCHORED_ADAPTIVE` luôn `true`; `MEDIUM_RECTANGLE` khi `availableWidthDp >= 300`.
   - `internal fun bannerFrameWidthDp(size: BannerSize): Int?`: `null` (lấp chiều rộng) cho `ANCHORED_ADAPTIVE`, `300` cho `MEDIUM_RECTANGLE`.
   - `internal fun bannerSlotHeightDp(frame: BannerFrame, size: BannerSize, frameHeightDp: Int, failedBeforeAd: Boolean): Int`: `NONE` là 0; `KEPT_EMPTY` là `frameHeightDp` (B-06); `ACTIVE` là 0 khi `size == MEDIUM_RECTANGLE && failedBeforeAd`, còn lại `frameHeightDp`.
6. **Vòng gửi** (`BannerLoads.kt`): `bannerRefreshOf` thêm `AdFormat.BANNER_MREC -> BannerRefresh.BY_AD_UNIT` (KDoc: "banner và MREC làm mới theo đơn vị"). `runBannerLoads` thêm tham số `onFailed: () -> Unit = {}`, gọi ở nhánh `Failed` ngay sau khi tăng `failures`, trước khi chờ thử lại; không đổi thứ tự hay thời gian gửi nào. Phần T-054 thêm vào vòng này (cờ toàn màn hình trong bước "được gửi lúc này") giữ nguyên.
7. **Khung** (`AdSlot.kt`):
   - `BannerSlotHost` thêm tham số `size: BannerSize = BannerSize.ANCHORED_ADAPTIVE`. Giữ trạng thái `failedBeforeAd` (`remember`, cùng chỗ với `hadAd`): `onFailed` đặt `true` khi `!hadAd`; `onLoaded` đặt `false`; khung về `NONE` thì đặt lại `false` cùng `hadAd`. Chiều cao của `Box` khung là `bannerSlotHeightDp(frame, size, heightDp, failedBeforeAd)`; `Box` có `contentAlignment = Alignment.TopCenter`; `BannerFrameContent` vẽ `view.Content` với `Modifier.width(300.dp)` khi `bannerFrameWidthDp(size)` khác `null`, không thì `fillMaxWidth()`. `onAdAvailabilityChange` không đổi (thanh đáy chỉ dùng `ANCHORED_ADAPTIVE`).
   - `BannerSlot` (riêng tư): `val size = bannerSizeOf(format)`; trong `BoxWithConstraints`, `adSize` là `AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)` khi `ANCHORED_ADAPTIVE`, `AdSize.MEDIUM_RECTANGLE` khi `MEDIUM_RECTANGLE` (T-067 đổi dòng của cỡ thích ứng, xem mục 6); khi `!bannerFitsWidth(size, widthDp)` thì ghi log một lần `Banner <khe> too narrow (<w> dp)` qua `ads.logger` (không có mã đơn vị) và không dựng `BannerSlotHost` (chỗ cao 0). Truyền `size` cho `BannerSlotHost`.
   - KDoc của `AdSlot`: thêm gạch đầu dòng `[AdFormat.BANNER_MREC]` (300 x 250 dp căn giữa, trong nội dung của màn, giữ 250 dp lúc tải, co về 0 khi nạp lỗi trước khi có quảng cáo, chỗ hẹp hơn 300 dp thì không hiện). `BottomBannerSlot` không đổi.
8. **Màn xem thử** (`AdGalleryScreen.kt`, chỉ staging): `AdGallerySlots` thêm `@Composable fun MediumRectangleBanner(modifier: Modifier)` (KDoc: "banner MREC của trang Banners, KB-29"); `SdkAdGallerySlots` gọi `AdSlot(AdPlacement.GALLERY_BANNER_MREC, modifier)`. `BannersPage`: sau `slots.Banner(...)` thêm `slots.MediumRectangleBanner(Modifier.fillMaxWidth().padding(vertical = Spacing.md))`, trước `LazyColumn` (không đặt trong danh sách: ô cuộn khỏi màn bị hủy, `AdView` sẽ bị dựng lại mỗi lần cuộn). Không thêm chuỗi: trang không có nhãn cho từng banner. KDoc của `BannersPage` và của `SdkAdGallerySlots` ghi thêm MREC.
9. **Bản giả cho test** (`PreviewAdGallerySlots.kt`): `PreviewAdTags.BANNER_MREC = "preview_ad_banner_mrec"`; `MediumRectangleBanner` vẽ `Box(modifier) { hộp 300 x 250 dp, căn giữa trên, nền surfaceContainer, chữ "MREC banner", tag BANNER_MREC }` theo cùng kiểu với `PreviewBanner`.
10. **Bảng khóa:** sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` (khóa mới `is_show_gallery_banner_mrec`) bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`; `RemoteConfigKeysTest` đỏ trước khi sinh lại.
11. **`scripts/ads-plan.py`:** thêm vào `FORMATS`, sau dòng `banner_collapsible`: `("banner_mrec", "Banner MREC", "Trong nội dung của màn, không ở đáy, không trong danh sách", "AdSlot(placement)", "300 x 250 dp căn giữa; giữ 250 dp lúc tải, nạp lỗi thì co về 0; chỗ hẹp hơn 300 dp thì không hiện", "Tự làm mới theo đơn vị trên AdMob", "BANNER_MREC", "Khe", "Không có", "is_show_<mã>")`. `PLACEMENT_FORMATS` tự có `banner_mrec` (ô chọn định dạng của trang "Vị trí QC"). `LIST_FORMATS` không đổi.
12. **`scripts/release.sh` không sửa** (đã đọc): `placement_formats()` bỏ mọi dòng có mã bắt đầu bằng `gallery_` trước khi gọi `test_unit_for`, nên `gallery_banner_mrec` không bao giờ tới nhánh `die "không có mã quảng cáo thử cho định dạng AdFormat.$1..."`; `--list-placements` và `fill_test_units` cũng không thấy dòng này. Vị trí `banner_mrec` của một app (không phải `gallery_`) mà chưa có mã trong `adUnits` thì `release.sh` dừng ở nhánh đó tới khi T-057 thêm mã thử MREC; `ads-guide.md` ghi điều này (bước 13). Test có sẵn chứng minh: `AdPlacementSourceTest.readByReleaseScript` cùng quy tắc bỏ `gallery_` (mục 3.5).
13. **Tài liệu:**
    - `docs/ops/ads-business-rules.md`: MC-02 thêm `banner_mrec` ("banner 300 x 250 trong nội dung, ADR-0036"); mục 5.5 thêm dòng B-07 (khe `banner_mrec`, đơn vị bật tự làm mới; cùng B-01, B-02, B-04, B-05, B-06 và luật dừng khi bị che của T-054; khung 300 dp căn giữa trong nội dung; giữ 250 dp lúc tải; nạp lỗi trước khi có quảng cáo thì co về 0 và vẫn thử lại, có quảng cáo thì về 250 dp; lỗi sau khi đã có quảng cáo thì giữ khung; chỗ hẹp hơn 300 dp thì không hiện; không vào thanh đáy, không làm ô trong danh sách; cột AdsKit ghi "Không có (ADR-0036, JBase `mrec_admob`)"); mục 5.7 thêm một câu dưới bảng: "MREC thật là banner `banner_mrec` (B-07), không phải biến thể native; native medium vẫn là lựa chọn mặc định (KB-2)"; M-05 thêm khe `banner_mrec`; M-06 thêm MREC (mã banner cỡ cố định); T-04 thêm `BANNER_MREC`; mục 8.2, gạch "Vị trí của lượt nạp": "banner (cả MREC)"; mục 12, đoạn "Chỗ ta lệch có chủ đích": thêm "định dạng `banner_mrec` (ADR-0036, B-07)".
    - `docs/ops/ads-guide.md`: mục 3, thêm dòng bảng "Banner MREC" (trong nội dung, `is_show_<mã vị trí>`, "Có (T-062, `AdSlot`): 300 x 250 dp căn giữa, làm mới theo đơn vị; native medium vẫn là mặc định, chỉ dùng khi spec cần MREC thật"); mục 5, bước 1, danh sách định dạng thêm `banner_mrec`; bước 6, câu về đơn vị: "đơn vị của vị trí `banner_mrec` bật tự làm mới; trước T-057, `release.sh` chưa có mã thử MREC nên vị trí `banner_mrec` thiếu mã thật thì script dừng"; mục 8, đoạn màn xem thử: trang "Banners" có thêm MREC `gallery_banner_mrec` dưới banner cố định, "Dùng 14 vị trí `gallery_*`".

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | `AdFormat.BANNER_MREC`, dòng `GALLERY_BANNER_MREC` (3.1 bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedRules.kt` | `fullScreenLoaderOf`: nhánh ném lỗi thêm `BANNER_MREC` (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt` | `poolCapacity`: nhánh `null` thêm `BANNER_MREC` (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `nativeLoadSize`: `BANNER_MREC -> null` (bước 2); không đổi gì khác |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPreloadPlan.kt` | `isNative`: nhánh `false` thêm `BANNER_MREC` (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetryRules.kt` | `metricCode`, `funnelKind`, `loadPlacementOf` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | Mã thử MREC (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerFrame.kt` | `BannerSize`, `bannerSizeOf`, hằng 300 và 250, `bannerFitsWidth`, `bannerFrameWidthDp`, `bannerSlotHeightDp` (bước 5); T-054 cũng sửa file này |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerLoads.kt` | `bannerRefreshOf`, tham số `onFailed` (bước 6); T-054 cũng sửa file này |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | `BannerSlotHost` có `size`, `failedBeforeAd`, khung căn giữa; `BannerSlot` chọn `AdSize`, kiểm bề rộng; KDoc `AdSlot` (bước 7); T-054 cũng sửa file này |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt` | `codes_areTheClosedListOfMc02` thêm `banner_mrec` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Dữ liệu `galleryPlacements_coverEveryFormatOnce` (14); test mới `galleryBannerMrec_isStagingOnlyMrec` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Dữ liệu `googleTestIds_coverGalleryPlacements` (14); test mới mã thử MREC (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt` | Dữ liệu `funnelKind_coversEveryFormat`, `metricCode_coversEveryFormat`; test mới nhóm và vị trí lượt nạp của MREC (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | Dữ liệu `funnel_appOpenAndBanner_sendNothing` thêm `BANNER_MREC`; test mới doanh thu MREC (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/SdkBannerAdViewTest.kt` | Test mới `mrecBanner_reportsBannerMrecFormatAndPlacement` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerFrameTest.kt` | Test mới của các hàm thuần bước 5 (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerLoadsTest.kt` | Dữ liệu `refreshOf_bannerByAdUnit_collapsibleManual_othersThrow`; test mới `onFailed` (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerSlotHostTest.kt` | Test mới của khung MREC (mục 4); hàm `setHost` nhận `size` và `heightDp` |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt` | Dữ liệu `FORMAT_CODES` thêm `BANNER_MREC` (mã của T-04) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `MediumRectangleBanner` trong `AdGallerySlots`, `SdkAdGallerySlots`, `BannersPage` (bước 8) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PreviewAdGallerySlots.kt` | Bản giả MREC 300 x 250 dp, tag (bước 9) |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Test mới vị trí của MREC trên trang "Banners" (mục 4) |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_banners_light.png` | Trang "Banners" có thêm hộp MREC giả 300 x 250 dp dưới banner cố định, khung `w411dp-h891dp-xxhdpi` (`AdGalleryScreenScreenshotTest.banners_light`, không đổi test) |
| Ghi lại | `app/src/test/screenshots/AdGalleryScreen_banners_small.png` | Như trên, khung `w360dp-h640dp-xxhdpi` (L-028; `banners_small`, không đổi test) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: `is_show_gallery_banner_mrec` (bước 10) |
| Sửa | `docs/remote-config-template.json` | Sinh lại, như trên |
| Sửa | `scripts/ads-plan.py` | Dòng `banner_mrec` của `FORMATS` (bước 11) |
| Sửa | `scripts/tests/run.sh` | Hai `expect` của bảng định dạng (mục 4), trong khối `ads-plan` |
| Sửa | `docs/ops/ads-business-rules.md` | MC-02, B-07, 5.7, M-05, M-06, T-04, 8.2, mục 12 (bước 13) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3, 5, 8 (bước 13) |
| Sửa | `docs/features/ads/tickets/T-062-dinh-dang-banner-mrec.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-062-dinh-dang-banner-mrec.md` | Nhật ký thực hiện |

Ảnh chuẩn thêm hay đổi: chỉ ghi lại hai ảnh `AdGalleryScreen_banners_light.png`, `AdGalleryScreen_banners_small.png`; không thêm ảnh mới (khung MREC của `:core:ads` không có ảnh chuẩn: test và bản xem trước không có `AdsController` nên `AdSlot` không vẽ gì). Lệnh ghi lại ở mục 4.

Nơi đọc các file bị sửa (L-020; `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, `src/testStaging`, 2026-10-29):

- `AdPlacement.kt`: `scripts/release.sh` (`placement_formats`, `--list-placements`, `--fill-test-units`), `scripts/status.py` và `AdPlacementSourceTest.kt` đọc dòng một dòng và bỏ mã `gallery_`, nên dòng mới không đòi mã production (3.1 bước 12); `scripts/ads-plan.py` đọc mã `AdFormat` và dòng vị trí (`--with-staging` mới liệt kê `gallery_`), bảng định dạng của nó được sửa cùng plan này. `scripts/tests/run.sh` dùng bản sao `scripts/tests/fixtures/release/AdPlacement.kt` và tệp tạm, không đọc file thật; test mới của plan đọc file thật chỉ để so mã định dạng. Test lặp `AdFormat.entries` hay `AdPlacement.entries` (`AdFormatPoolTest`, `NativeAdSourcesTest`, `RewardedRulesTest`, `NativeFlowStepTest`, `InListAdsStateTest`, `StagingRemoteValuesModuleTest`, `AdsSettingsTest`) đọc theo thuộc tính, không theo số lượng; đã xem, không đổi (mục 3.5).
- `BannerFrame.kt`, `BannerLoads.kt`, `AdSlot.kt`: chỉ test của `:core:ads` gọi (`BannerFrameTest`, `BannerLoadsTest`, `BannerSlotHostTest`, `BottomAdBarTest`); tham số mới có giá trị mặc định nên `BottomAdBarTest` dựng `BannerSlotHost` như cũ.
- `AdUnitIds.kt`: chú thích của `scripts/release.sh` (`test_unit_for`) nói hai bảng giống nhau; `release.sh` không sửa ở plan này (CLAUDE.md, "Không được làm"; T-057 thêm mã thử MREC), nên tới T-057 hai bảng khác nhau ở một dòng; `release.sh` không bao giờ cần mã thử cho vị trí `gallery_` (3.1 bước 12).
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` so với code (sinh lại thì qua, không sửa test), `app/build.gradle.kts` chỉ truyền đường dẫn hai file và cờ `-PremoteConfigKeys.update` cho test đó (không sửa); `scripts/ads-plan.py` đọc `remote-config-keys.md` (khóa `gallery_` chỉ có với `--with-staging`, không đổi test `ads-plan`); `scripts/template-sync.sh` chép nguyên hai file sang app, không đọc nội dung.
- `scripts/ads-plan.py`: `scripts/tests/run.sh` (khối `ads-plan`), sửa cùng plan; `docs/ops/ads-plan.md` chỉ mô tả bảng định dạng chung, không liệt kê dòng, không sửa.
- `scripts/tests/run.sh`: `scripts/check.sh` và `.github/workflows/pr-check.yml` chạy nguyên script, `scripts/local-setup.sh` chỉ nhắc tên trong chú thích về chỗ nối test; thêm hai `expect` không đổi cách chạy hay công cụ cần (vẫn `python3 -I`, không cần `openpyxl`), nên không sửa ba file đó (`check.sh` và `.github/` cũng thuộc mục "Không được làm").
- Ảnh chuẩn `AdGalleryScreen_banners_*`: không script hay workflow nào đọc theo tên (`git grep "AdGalleryScreen_" scripts .github` rỗng).
- `AdGalleryScreen.kt`, `PreviewAdGallerySlots.kt`: `AdGallerySourceSetTest` bỏ qua source set `staging`; `app/src/androidTest` không mở màn xem thử.
- `ads-business-rules.md`, `ads-guide.md`: không script hay test nào đọc nội dung; `AdFormatTest` chép tay danh sách MC-02 (sửa cùng plan). Các test khác nhắc `docs/ops/ads-business-rules.md` chỉ trong chú thích dẫn luật, không mở file: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`; luật họ dẫn (mục 1, 5.7, 8, 10, 11) không đổi chữ, nên không sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Khóa mới `is_show_gallery_banner_mrec` suy ra từ dòng vị trí (MC-01), mặc định tắt, staging tự bật.
- Quảng cáo: thêm định dạng `banner_mrec` vào MC-02 (spec KB-29, câu hỏi 38; ADR-0036 điểm 3) và vị trí `gallery_banner_mrec` (spec mục 5, dòng "Các vị trí của màn hình xem thử", chỉ staging). Không thêm vị trí production nào.
- Sự kiện phân tích: không thêm sự kiện hay tham số; giá trị mới `BANNER_MREC` của `ad_format` (Firebase: `ad_impression_placement`, `ad_load_success`, `ad_load_fail`, `ad_clicked`) và `ad_type` (AppsFlyer `af_ad_revenue`) (spec mục 6, dòng "Thêm 2026-10-29", câu hỏi 38). Nhóm UA và người đọc báo cáo doanh thu cần biết mã mới trước khi một app phát hành vị trí MREC.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `AdSize.MEDIUM_RECTANGLE` và `AdView` của `play-services-ads` đã có, không cần quyền mới (spec mục 6: "banner MREC (KB-29) không thêm SDK hay quyền").
- **Thư viện và SDK:** Không thêm. `AdSize`, `AdView` thuộc `play-services-ads` (khai báo `libs.play.services.ads` trong `gradle/libs.versions.toml`, `:core:ads` đã phụ thuộc, `AdSlot.kt` đã import cả hai); `Alignment`, `Modifier.width` thuộc Compose UI và foundation đã có trên classpath của `:core:ads` và `:app`.

### 3.4. Phương án dự phòng

Không có. Mọi phần là biến thể của mẫu banner đã chạy (T-029, T-036), không có kỹ thuật mới. Điểm chỉ kiểm được trên máy thật (`AdView` trong khung cao 0 dp vẫn tải quảng cáo khi thử lại) đặt ở câu hỏi 3 và ở dòng kiểm tra trên máy thật của mục 4, không phải điều kiện chuyển của phiên.

### 3.5. Hành vi đã có bị đổi

Từ `python3 -I scripts/existing-tests.py docs/features/ads/plans/T-062-dinh-dang-banner-mrec.md` (2026-10-29), đã đọc từng test của nhóm "Cần xem trước" và "Nên xem". Mọi dòng dưới là dữ liệu của test cũ phải có dòng enum hay vị trí plan thêm (ADR-0029, quyết định 2) hoặc câu khẳng định giữ nguyên; không có câu nào trái điều plan nói phải viết lại theo ADR-0031.

Nhóm "Cần xem trước" (16 test) chỉ trùng tên với plan, không kiểm hành vi plan đổi: `LoadingScreenTest` (6 test, `Failed` của màn loading), `NativeAdCellTest`, `NativeAdSlotTest`, `NativeFlowStepTest` (`Loaded` của native), `InListAdsTest.grid_inCollection_adTakesOneCell_everyNItems`, `NativeAdCellTest.otherFormat_isRejected` (định dạng native, `BANNER_MREC` không vào `IN_LIST_FORMATS` hay ô), `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` (`AdsController`, plan chỉ thêm một nhánh `nativeLoadSize`), `AdGalleryScreenTest.flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard` (trang "Flow steps", không đổi; `AdGallerySlots` thêm một hàm, chỉ `PreviewAdGallerySlots` và `SdkAdGallerySlots` cài giao diện này). Nhóm "Nên xem" (164 test) lặp `AdFormat.entries`, `AdPlacement.entries` hay dựng `AdPreloadPlan` theo thuộc tính: đã xem các test lặp theo số lượng hay danh sách cố định; những test cần sửa dữ liệu có trong bảng dưới, còn lại (`AdPreloadPlanTest.fromPlacements_hasRewardedInSecondGroup`, `NativeAdSourcesTest.singleAdSourceFormats_areSmallAndCollapsible`, `AdFormatPoolTest.poolCapacity_followsP02`, `AdPreloaderTest`, `PreloadPlanRunnerTest`) giữ nguyên vì `BANNER_MREC` không có pool, không có khe nạp sẵn, không phải native.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| MC-02 có 11 mã định dạng | Có thêm `banner_mrec` (bước 1) | `AdFormatTest.codes_areTheClosedListOfMc02` | Dữ liệu: tập `mc02` chép tay thêm `"banner_mrec"`, cùng lúc sửa MC-02 trong `ads-business-rules.md` |
| Màn xem thử có 13 vị trí, mọi định dạng trừ lớp thu gọn | 14 vị trí, vẫn mọi định dạng trừ lớp thu gọn (bước 1) | `AdPlacementTest.galleryPlacements_coverEveryFormatOnce`, `AdUnitIdsTest.googleTestIds_coverGalleryPlacements` | Dữ liệu: `13` thành `14`; câu tập định dạng (`AdFormat.entries - NATIVE_COLLAPSIBLE`) giữ nguyên và tự có `BANNER_MREC` |
| Chỉ `BANNER`, `BANNER_COLLAPSIBLE` có cách làm mới; định dạng khác ném lỗi | `BANNER_MREC` là `BY_AD_UNIT` (bước 6) | `BannerLoadsTest.refreshOf_bannerByAdUnit_collapsibleManual_othersThrow` | Dữ liệu: thêm `assertEquals(BannerRefresh.BY_AD_UNIT, bannerRefreshOf(AdFormat.BANNER_MREC))`, `others` bỏ thêm `BANNER_MREC`; câu "định dạng khác ném lỗi" giữ |
| Bảng mã định dạng T-04 có 10 mã | Có thêm `BANNER_MREC` (bước 3) | `AdTelemetryRulesTest.metricCode_coversEveryFormat`, `AppsFlyerTrackerTest` (`FORMAT_CODES`) | Dữ liệu: bản đồ thêm `AdFormat.BANNER_MREC to "BANNER_MREC"`, tập mã thêm `"BANNER_MREC"`; `FORMAT_CODES` thêm `"BANNER_MREC"` (mã đi nguyên vào `ad_type`) |
| Banner không có phễu AppsFlyer | MREC cũng không (bước 3) | `AdTelemetryRulesTest.funnelKind_coversEveryFormat`, `AdTelemetryTest.funnel_appOpenAndBanner_sendNothing` | Dữ liệu: thêm `AdFormat.BANNER_MREC to null`; danh sách định dạng không có phễu thêm `BANNER_MREC` |
| Mọi định dạng có mã thử | MREC có mã thử riêng (bước 4) | `AdUnitIdsTest.googleTestUnit_existsForEveryFormat`, `googleTestUnits_existForEveryPlacement` | Giữ: tự kiểm `BANNER_MREC` và `GALLERY_BANNER_MREC` |
| `release.sh` đọc mọi vị trí không phải `gallery_` | Không đổi: dòng mới là `gallery_` (bước 12) | `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement` | Giữ: tập đọc được và tập `!stagingOnly` cùng không có `gallery_banner_mrec` |
| Khung banner `ACTIVE` luôn cao `heightDp`, lấp chiều rộng | `ANCHORED_ADAPTIVE` (mặc định) như cũ; chỉ `MEDIUM_RECTANGLE` rộng 300 dp và co về 0 khi lỗi trước quảng cáo (bước 5, 7) | `BannerSlotHostTest` (mọi test, `revokedAfterAd_keepsHeight_hidesAd_pauses_reloadsWhenGranted`, `revokedBeforeAnyAd_givesSpaceBack_andDestroys`, `gateClosed_noFrame_noViewCreated`), `BannerFrameTest.frame_table`, `shouldRun_onlyWhenVisibleAndActive`, `BottomAdBarTest` | Giữ: các test dựng host không truyền `size`, nên chạy `ANCHORED_ADAPTIVE`; `bannerFrame`, `bannerShouldRun` không đổi chữ ký ở plan này (T-054 đổi `bannerShouldRun`, xem mục 6) |
| Vòng gửi của banner không báo lỗi ra ngoài | Gọi thêm `onFailed` ở mỗi lỗi, không đổi thời điểm gửi (bước 6) | `BannerLoadsTest` (mọi test thời gian: `failures_retryAt2_4_6_8_10_10s`, `retryDueWhileHidden_waitsForNextVisible`, `networkBack_cutsRetryWaitShort`...) | Giữ: tham số mặc định `{}`; thời điểm gửi không đổi |
| Trang "Banners": tiêu đề, banner cố định, danh sách, banner thu gọn ở đáy | Thêm MREC giữa banner cố định và danh sách (bước 8) | `AdGalleryScreenTest.banners_fixedOnTop_collapsibleAtBottom`; ảnh chuẩn `AdGalleryScreen_banners_light`, `_small` | Test giữ: banner cố định vẫn ngay dưới tiêu đề 24 dp, lấp chiều rộng; danh sách vẫn nằm dưới banner (dưới cả MREC), trên banner thu gọn; mục 30 cuộn tới vẫn không bị che. Hai ảnh chuẩn ghi lại (mục 3.2) |
| Bảng khóa Remote Config | Thêm `is_show_gallery_banner_mrec` (bước 10) | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` | Dữ liệu: sinh lại hai file |
| Nhóm nạp sẵn đầu `{APP_OPEN, NATIVE_STEP}` | Không đổi; `BANNER_MREC` ở nhóm sau, không có khe nạp sẵn (bước 2) | `AdPreloadPlanTest.templatePlan_hasAppOpenFirst` | Giữ |

## 4. Kiểm thử

Thứ tự commit (L-025): commit test (bước 3 của phiên) có khung rỗng để biên dịch được: dòng `AdFormat.BANNER_MREC`, dòng `GALLERY_BANNER_MREC`, nhánh `BANNER_MREC` của các `when` ở bước 2, 3, 4 trả giá trị sai có chủ đích (`metricCode` trả `"BANNER"`, `unitFor` trả mã banner thích ứng, `bannerRefreshOf` ném lỗi), các hàm của bước 5 trả giá trị của banner thường, `size` và `onFailed` có mặt nhưng không dùng, `MediumRectangleBanner` của `SdkAdGallerySlots` và bản giả vẽ `Box(modifier)` rỗng; dữ liệu test cũ của mục 3.5 sửa cùng commit test. Code thật ở commit sau.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| MC-02 có `banner_mrec` | Unit test | `AdFormatTest.codes_areTheClosedListOfMc02` (dữ liệu, mục 3.5); `AdFormatTest.bannerMrec_codeAndNotFullScreen`: `AdFormat.BANNER_MREC.code == "banner_mrec"`, `!isFullScreen`, `!hasFullScreenSlot`, `poolCapacity() == null`, `nativeLoadSize() == null`; đối chứng: `NATIVE_MEDIUM.poolCapacity() == 3` |
| KB-29, vị trí xem thử | Unit test | `AdPlacementTest.galleryBannerMrec_isStagingOnlyMrec`: `GALLERY_BANNER_MREC.key == "gallery_banner_mrec"`, `format == BANNER_MREC`, `stagingOnly`, `!appWide`, `AdsSettings.placementEnabled(GALLERY_BANNER_MREC).name == "is_show_gallery_banner_mrec"`; `galleryPlacements_coverEveryFormatOnce` (14) |
| KB-29, mã thử của Google | Unit test | `AdUnitIdsTest.googleTestUnit_bannerMrec_isFixedSizeBannerUnit`: `unitFor(BANNER_MREC) == "ca-app-pub-3940256099942544/6300978111"` và `GoogleTestAdUnitIds.of(GALLERY_BANNER_MREC)` bằng giá trị đó; đối chứng: khác `unitFor(BANNER)` (`.../9214589741`). `ConfiguredAdUnitIds("gallery_banner_mrec=x").of(GALLERY_BANNER_MREC) == null` và `missing` không có nó |
| KB-29, cỡ và bề rộng | Unit test | `BannerFrameTest.size_ofBannerFormats`: `bannerSizeOf(BANNER)`, `(BANNER_COLLAPSIBLE)` là `ANCHORED_ADAPTIVE`, `(BANNER_MREC)` là `MEDIUM_RECTANGLE`, mọi định dạng khác ném `IllegalArgumentException`. `BannerFrameTest.mediumRectangle_matchesSdkSize`: `AdSize.MEDIUM_RECTANGLE.width == MEDIUM_RECTANGLE_WIDTH_DP` (300), `.height == MEDIUM_RECTANGLE_HEIGHT_DP` (250). `BannerFrameTest.fitsWidth_mrecNeeds300`: MREC 299 là `false`, 300 và 411 là `true`; đối chứng: `ANCHORED_ADAPTIVE` 200 là `true`. `bannerFrameWidthDp`: 300 và `null` |
| KB-29, 250 dp lúc tải, 0 khi lỗi (hàm thuần) | Unit test | `BannerFrameTest.slotHeight_table`: bảng (`frame`, `size`, `failedBeforeAd`) ra chiều cao với `frameHeightDp = 250`: `NONE` mọi trường hợp 0; `ACTIVE, MEDIUM_RECTANGLE, false` 250; `ACTIVE, MEDIUM_RECTANGLE, true` 0; `KEPT_EMPTY, MEDIUM_RECTANGLE, true` 250 (B-06); đối chứng (L-055): `ACTIVE, ANCHORED_ADAPTIVE, true` 250 (banner cố định không co) |
| KB-29, làm mới theo đơn vị | Unit test | `BannerLoadsTest.refreshOf_bannerByAdUnit_collapsibleManual_othersThrow` (dữ liệu, mục 3.5): `BANNER_MREC` là `BY_AD_UNIT` |
| KB-29, thử lại khi lỗi; báo lỗi cho khung | Unit test | `BannerLoadsTest.failed_callsOnFailed_onceEach_beforeRetry`: với `BY_AD_UNIT`, lượt đầu lỗi thì `onFailed` chạy 1 lần lúc 0 ms, lượt thử lại gửi lúc 2 000 ms, lỗi tiếp thì `onFailed` lần 2; `Loaded` không gọi `onFailed`; thời điểm gửi bằng `failures_retryAt2_4_6_8_10_10s` (đối chứng: cùng chuỗi gửi khi không truyền `onFailed`) |
| KB-29, khung 300 x 250 căn giữa lúc tải | Compose UI test (Robolectric, khung `w411dp-h891dp-xxhdpi`) | `BannerSlotHostTest.mrec_loading_keeps250_width300_centered`: host `size = MEDIUM_RECTANGLE`, `heightDp = 250`, slot `fillMaxWidth`; trước kết quả nào: slot cao 250 dp, nút `FAKE_BANNER_TAG` rộng 300 dp, cách mép trái (411 - 300) / 2 = 55,5 dp (sai số 1 dp), 1 lượt gửi. Đối chứng: host mặc định cùng chỗ thì quảng cáo giả rộng 411 dp |
| KB-29, nạp lỗi thì co về 0, có quảng cáo thì về 250 | Compose UI test | `BannerSlotHostTest.mrec_failedBeforeAd_collapsesTo0_retries_loadedBackTo250`: `emitFailed(3)` thì slot cao 0 dp, `destroyCount == 0`; tiến đồng hồ 2 giây (`composeRule.mainClock.advanceTimeBy(2_000)`) thì `sendCount == 2`; `emitLoaded()` thì slot cao 250 dp, quảng cáo hiện. Đối chứng (L-055): host mặc định (`ANCHORED_ADAPTIVE`) cùng `emitFailed(3)` vẫn cao `HEIGHT_DP` |
| KB-29, lỗi sau khi đã có quảng cáo thì giữ khung | Compose UI test | `BannerSlotHostTest.mrec_failedAfterAd_keeps250`: `emitLoaded()` rồi `emitFailed(3)` thì slot vẫn 250 dp, quảng cáo giả vẫn hiện |
| KB-29 (KB-18, B-06), rút đồng ý thì giữ khung | Compose UI test | `BannerSlotHostTest.mrec_revokedAfterAd_keeps250_reloadsWhenGranted`: như `revokedAfterAd_keepsHeight_hidesAd_pauses_reloadsWhenGranted` với `size = MEDIUM_RECTANGLE`: rút thì 250 dp, ẩn, `pauseCount == 1`; cho lại thì gửi lần 2 trong cùng `AdView` |
| KB-29 (KB-1, B-01, B-04), chưa đồng ý, vị trí tắt, mã rỗng | Compose UI test | `BannerSlotHostTest.mrec_gateClosedOrOffOrBlank_noSpace_noView`: ba trường hợp, mỗi trường hợp slot cao 0 dp, `createdCount == 0`; đối chứng: mở cổng thì 250 dp và 1 lượt gửi |
| KB-29, lỗi trước quảng cáo rồi rút đồng ý | Compose UI test | `BannerSlotHostTest.mrec_failedThenRevoked_givesSpaceBack_freshFrameAt250`: lỗi (0 dp), rút đồng ý (khung `NONE`, `AdView` hủy), mở lại thì khung mới cao 250 dp (trạng thái `failedBeforeAd` đặt lại cùng `hadAd`) |
| Hai khe MREC cùng vị trí (L-068) | Compose UI test | `BannerSlotHostTest.mrec_twoSlotsSamePlacement_collapseApart`: hai host cùng `placementKey`, mỗi host một `FakeBannerAdView`; khe đầu `emitFailed(3)` thì khe đầu 0 dp, khe hai vẫn 250 dp; khe hai `emitLoaded()` không làm khe đầu về 250 |
| KB-29 (KB-18), tạm dừng khi màn không hiện | Compose UI test | `BannerSlotHostTest.mrec_hiddenScreen_pauses_noRetryUntilVisible`: `size = MEDIUM_RECTANGLE`; `visible = false` thì `pauseCount == 1`; `emitFailed(3)` rồi tiến 10 giây thì không gửi thêm; `visible = true` thì `resumeCount == 1` và gửi lại 1 lần (B-05) |
| KB-29 (KB-27), dừng khi bị quảng cáo toàn màn hình che | Compose UI test | `BannerSlotHostTest.mrec_fullScreenShowing_pauses_sendsNothing_resumesAfter`: host `size = MEDIUM_RECTANGLE` với cờ "đang hiện quảng cáo toàn màn hình" mà T-054 tạo (tên trong ticket T-054: `fullScreenAdShowing`, đi vào `bannerShouldRun` và bước gửi; phiên dùng đúng tên và chỗ truyền của T-054 đã merge). Cờ bật thì `pauseCount == 1`, `destroyCount == 0`, quảng cáo giả vẫn hiện; `emitFailed(3)` rồi tiến 10 giây thì không gửi thêm; cờ tắt thì `resumeCount == 1` và gửi lại đúng 1 lần (lượt tới hạn trong lúc che không dồn). Đối chứng: cùng khung, cờ tắt từ đầu, lỗi thì gửi lại sau 2 giây |
| KB-29, `ad_format` của doanh thu, nạp, bấm | Unit test | `AdTelemetryRulesTest.metricCode_coversEveryFormat` (dữ liệu); `AdTelemetryRulesTest.bannerMrec_slotGroup_loadPlacementOfScreen`: `metricGroup() == SLOT`, `funnelKind() == null`, `loadPlacementOf(BANNER_MREC, GALLERY_BANNER_MREC) == "gallery_banner_mrec"`, `loadPlacementOf(BANNER_MREC, null) == null`; đối chứng: `loadPlacementOf(NATIVE_MEDIUM, ...) == "pool"` |
| KB-29, doanh thu ghi `BANNER_MREC` với mã vị trí | Unit test | `AdTelemetryTest.revenue_bannerMrec_formatCodeAndPlacement`: `telemetry.revenue(BANNER_MREC, "unit", { GALLERY_BANNER_MREC }, 1_000, "USD", "Pangle", null)` thì `ImpressionRevenue.adFormat == "BANNER_MREC"`, `adPlacement == "gallery_banner_mrec"`, `value == 0.001`; không có sự kiện phễu. Đối chứng: cùng lượt với `BANNER` ra `"BANNER"` |
| KB-29, `SdkBannerAdView` của MREC báo đúng định dạng | Unit test (Robolectric) | `SdkBannerAdViewTest.mrecBanner_reportsBannerMrecFormatAndPlacement`: như `barBanner_reportsBannerFormatAndBarPlacement` với `placement = GALLERY_BANNER_MREC`, `format = BANNER_MREC`: `LoadSuccess.adFormat == "BANNER_MREC"`, `adPlacement == "gallery_banner_mrec"`, `Clicked` cũng vậy; lượt gửi không có extras thu gọn (`bannerAdRequest(BY_AD_UNIT)`) |
| KB-29, AppsFlyer nhận mã mới | Unit test | `AppsFlyerTrackerTest` (dữ liệu `FORMAT_CODES`): `ad_type` của `af_ad_revenue` có `"BANNER_MREC"` |
| KB-29, màn xem thử: MREC trên trang "Banners" | Compose UI test (khung `w411dp-h891dp-xxhdpi`) | `AdGalleryScreenTest.banners_mrecCenteredBetweenBannerAndList`: nút `PreviewAdTags.BANNER_MREC` rộng 300 dp, cao 250 dp, tâm ngang ở 205,5 dp (sai số 1 dp), `top >= banner.bottom + 16 dp - 1`, danh sách `top >= mrec.bottom`; `banners_fixedOnTop_collapsibleAtBottom` giữ nguyên |
| Màn thấp (L-028): trang "Banners" vẫn dùng được | Compose UI test (khung `w360dp-h640dp-xxhdpi`) | `AdGalleryScreenTest.banners_small_listStillReachable`: MREC, banner thu gọn và danh sách cùng hiện; danh sách cao hơn 0; cuộn tới "List item 30" thì mục đó nằm trên banner thu gọn |
| Giao diện trang "Banners" | Screenshot test | Ghi lại `AdGalleryScreen_banners_light.png`, `AdGalleryScreen_banners_small.png` (test có sẵn `banners_light`, `banners_small`, không đổi code test; lệnh dưới bảng) |
| KB-29, `ads-plan.py` có định dạng mới | Test script (`scripts/tests/run.sh`, khối `ads-plan`) | `expect "ads-plan: the format table lists every AdFormat code of :core:ads" True ...`: `runpy.run_path("scripts/ads-plan.py")` rồi so tập mã `FORMATS` với tập mã `AdFormat` đọc từ `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` (cùng regex của `read_placements`); `expect "ads-plan: banner_mrec is a placement format with metric code BANNER_MREC" "True BANNER_MREC" ...`: `"banner_mrec" in PLACEMENT_FORMATS` và cột mã số liệu của dòng đó. Trên khung rỗng câu đầu đỏ (`AdFormat` có `banner_mrec`, bảng chưa có) |
| Bảng khóa Remote Config | Unit test | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` (đỏ trước, qua sau khi sinh lại; bước 10) |
| KB-29, banner MREC thật trên máy | Kiểm tra thủ công (máy thật: khác: quảng cáo thử của Google cần SDK thật và mạng; emulator của phiên từng trả lỗi 0 cho quảng cáo thử, FB-20261023-02) | Cài bản staging, mở "Ad gallery" > "Banners": có banner 300 x 250 dp căn giữa dưới banner cố định, có nhãn "Test Ad" và AdChoices của SDK; log `VGL.Ads` có `Banner gallery_banner_mrec...` không có mã đơn vị. Bật chế độ máy bay, xóa dữ liệu app, mở lại trang: khung giữ 250 dp lúc tải rồi co về 0 khi lỗi; tắt chế độ máy bay: MREC hiện lại trong vòng 10 giây (câu hỏi 3). Ra nền rồi vào lại: không có lượt gửi khi ở nền |
| Doanh thu MREC trên Firebase DebugView | Kiểm tra thủ công (máy thật: khác: cần Firebase DebugView của project staging, phiên không có) | Bật DebugView cho máy, xem trang "Banners": sự kiện `ad_impression_placement` có `ad_format = BANNER_MREC`, `ad_placement = gallery_banner_mrec` |
| Tài liệu MC-02, B-07, M-05, M-06, T-04, mục 12, ads-guide | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc phần sửa của `ads-business-rules.md` và `ads-guide.md` khớp mục 3.1 bước 13 |

Lệnh ghi lại hai ảnh chuẩn (L-114): `./gradlew :app:recordRoborazziStagingDebug --rerun --tests '*AdGalleryScreenScreenshotTest.banners_*'` (task test như `CLAUDE.md`, mục Lệnh), rồi `git status --short -- '*/src/test/screenshots/'` chỉ được có `AdGalleryScreen_banners_light.png` và `AdGalleryScreen_banners_small.png`.

**Ngoại lệ viết test trước:**

- Nối trong `BannerSlot` (chọn `AdSize.MEDIUM_RECTANGLE` theo `bannerSizeOf`, gọi `bannerFitsWidth` và bỏ khung khi chỗ hẹp, truyền `size` cho `BannerSlotHost`): cần `AdsController` thật và SDK, test JVM không dựng được (test và bản xem trước không có `LocalAdsController`, `AdSlot` không vẽ gì). Phần quyết định nằm ở hàm thuần đã test (`bannerSizeOf`, `bannerFitsWidth`, `bannerSlotHeightDp`), mọi lượt gửi đi qua `runBannerLoads` đã test (L-072); kiểm tra trên máy thật ở dòng cuối mục 4.
- `SdkAdGallerySlots.MediumRectangleBanner` (một dòng gọi `AdSlot`): như các ô thật khác của màn xem thử, kiểm trên máy thật.
- Tài liệu (`ads-business-rules.md`, `ads-guide.md`) và dòng `FORMATS` của `ads-plan.py` ngoài phần test script kiểm: chỉ là chữ.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thêm định dạng và một vị trí staging), 4 (thu thập dữ liệu: giá trị mới `BANNER_MREC` của `ad_format` và `ad_type`), 7 (module core dùng chung `:core:ads`, test của `:core:analytics`). Mức Cao, không tính tổng.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module: `:core:ads` (mười file code), test của `:core:analytics`, `:app` staging, `scripts/ads-plan.py` và test script, hai tài liệu nghiệp vụ |
| B. Độ rõ của yêu cầu | 1 | KB-29 và câu hỏi 38 đã chốt; AI tự quyết vài chi tiết nhỏ: chỗ hẹp hơn 300 dp, mã thử cỡ cố định, lỗi sau khi đã có quảng cáo thì giữ khung, vị trí trên trang "Banners" (câu hỏi 1 tới 4) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có test JVM hay Robolectric; phần nối với SDK và hiển thị quảng cáo thật kiểm bằng mắt trên máy |
| D. Ảnh hưởng chéo | 2 | `BannerFrame.kt`, `BannerLoads.kt`, `AdSlot.kt` cũng do T-054 sửa (phụ thuộc), `AdSlot.kt` do T-067, `AdUnitIds.kt` do T-056; hành vi banner cố định và thanh đáy giữ nguyên |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của banner T-029 (một `AdSize` khác, một trạng thái co khi lỗi) |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn 1, 4, 7) |

Plan chạm quảng cáo và dữ liệu: người phụ trách sản phẩm (anh Nhật) cần biết trước khi duyệt (risk-scoring.md, mục 3).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdFormat` (danh sách đóng mọi `when` không có `else` đọc), `BannerSlotHost`, `runBannerLoads`, `AdTelemetryRules`, `GoogleTestAdUnitIds`, màn xem thử staging, bảng khóa Remote Config, bảng định dạng của `ads-plan.py`.
- Task khác trong sprint có thể xung đột:
  - T-054 (phụ thuộc, plan đang soạn song song trên nhánh `claude/plan-T-054-dung-khi-bi-che`): T-054 tạo cờ `fullScreenAdShowing` trong điều kiện chạy của banner (`bannerShouldRun`, `BannerFrame.kt`) và hàm thuần "được gửi lúc này" mà mọi đường gửi cùng gọi (bước gửi của `runBannerLoads`, `BannerLoads.kt`), và nối cờ vào `BannerSlotHost` (`AdSlot.kt`). Plan này ghi Sửa cho ba file đó sau T-054 và không tạo lại cờ hay hàm đó (L-054); test KB-27 của MREC (mục 4) dùng đúng tên T-054 đã merge. T-054 đổi chữ ký mà test của plan này không còn dựng được thì phiên sửa phần dựng của test theo code đã merge, ý định của test giữ nguyên, ghi vào nhật ký.
  - T-057 (`release.sh` đọc mã theo khe): thêm `BANNER_MREC` vào `test_unit_for` của `release.sh` cùng mã `.../6300978111`; plan T-057 nên đọc bảng `GoogleTestAdUnitIds.unitFor` sau khi plan này merge.
  - T-056 (mã theo khe trong code): sửa `AdUnitIds.kt`; khe `banner_mrec` là một khe mới của M-05 mà T-056 phải có.
  - T-067 (banner thích ứng thường): đổi dòng `AdSize.getLargeAnchoredAdaptiveBannerAdSize` trong `BannerSlot`; plan này đặt dòng đó trong nhánh `ANCHORED_ADAPTIVE`, xung đột chữ nếu chạy song song, không xung đột ý.
  - T-061 (thanh đáy chừa chỗ lúc tải): dùng `BannerSlotHost` qua `BottomBannerSlot` (`onHeight`, `onAdAvailabilityChange`); plan này không đổi hai tham số đó và để thanh đáy ở `ANCHORED_ADAPTIVE`.
  - T-063 (cổng không hiện quảng cáo): đóng cổng thì MREC như banner (`bannerFrame` `NONE` hay `KEPT_EMPTY`); không cần sửa riêng.
- Đề xuất thứ tự chạy: sau T-054 (`depends_on`); trước T-057 để T-057 thêm mã thử MREC cùng lúc; T-056, T-067 trước hay sau đều được, chạy sau thì phiên của task sau gộp chữ.

## 7. Điều kiện dừng riêng

- Phải đổi hành vi của banner cố định, banner thu gọn hay khe banner của thanh đáy (chiều cao, lúc gửi, giữ khung) để MREC chạy đúng: dừng, vì KB-18 và D-02 không đổi ở ticket này.
- Ảnh chuẩn nào ngoài `AdGalleryScreen_banners_light.png`, `AdGalleryScreen_banners_small.png` lệch: dừng (risk-scoring.md, mục 4).
- T-054 chưa merge khi tới task này: không làm (phụ thuộc), không tự thêm cờ toàn màn hình.

## 8. Câu hỏi cho người duyệt

1. **Mã thử của MREC:** Google không có đơn vị thử riêng tên "MREC"; tài liệu của Google dùng đơn vị banner cỡ cố định `ca-app-pub-3940256099942544/6300978111` cho mọi cỡ cố định, kể cả 300 x 250. AI đề xuất dùng mã này (`unitFor(BANNER_MREC)`), và T-057 dùng cùng mã trong `release.sh`. Đồng ý không?
2. **Chỗ hẹp hơn 300 dp:** AdMob không vẽ MREC khi không đủ 300 dp. AI đề xuất không dựng khung, không gửi yêu cầu, không chiếm chỗ, ghi một dòng log (như B-04 khi không có mã). Điện thoại thường rộng từ 320 dp nên ít gặp, trừ khi nơi gọi đặt MREC trong cột hẹp. Hay muốn gửi yêu cầu và để SDK báo lỗi (khung co về 0 theo KB-29)?
3. **Lỗi sau khi đã có quảng cáo:** KB-29 nói "nạp lỗi thì co về 0". AI hiểu là lỗi trước khi khung có quảng cáo nào; khi đã có quảng cáo mà lượt làm mới hay thử lại lỗi, `AdView` vẫn hiện quảng cáo cũ nên khung giữ 250 dp (như khe đáy native giữ quảng cáo cũ, mục 5.7). Khi khung đã co về 0, `AdView` vẫn sống trong khung cao 0 dp và vẫn thử lại theo B-02; điều này chỉ kiểm được trên máy thật (mục 4). Đồng ý cách hiểu này không?
4. **Chỗ đặt trên màn xem thử:** AI đặt MREC trên trang "Banners" có sẵn, giữa banner cố định và danh sách, để không đổi trang mặc định (có ảnh thiết kế) và chỉ ghi lại hai ảnh chuẩn của trang không có thiết kế. Trên khung 360 x 640 dp danh sách còn ít chỗ hơn hôm nay nhưng vẫn cuộn được. Hay muốn một trang "MREC" riêng (thêm một nút ở khu "More formats", ghi lại ảnh `AdGalleryScreen_default-more_light`)?
5. **Số điểm của ADR-0036 lệch nhau:** spec KB-29 và ticket T-062 ghi "ADR-0036, điểm 4" cho `banner_mrec`, nhưng ADR-0036 hiện đánh số `banner_mrec` là điểm 3 (điểm hạn dùng interstitial cũ đã bỏ, câu hỏi 35, nên các điểm sau dời lên; cổng không hiện quảng cáo giờ là điểm 4). Nội dung không mâu thuẫn, chỉ lệch số. Plan này dẫn "ADR-0036 điểm 3" theo ADR. AI đề xuất người sửa số trong spec (KB-27 tới KB-30) và các ticket T-054 tới T-063 ở một PR tài liệu riêng; plan không sửa spec đã `confirmed`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
