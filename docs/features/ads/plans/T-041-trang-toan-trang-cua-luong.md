---
ticket: T-041
title: Native toàn trang của luồng giữ CTA trong màn thấp và giữ quảng cáo khi xoay, gập máy
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 25M          # cỡ M, số gợi ý 19.6M (scripts/token-usage.py estimate, 2026-10-26); thêm cho hai phần độc lập (bố cục và giữ quảng cáo), ba file test mới, test Robolectric dựng lại activity, ghi lại ảnh chuẩn ở hai module
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-041: NATIVE TOÀN TRANG CỦA LUỒNG GIỮ CTA TRONG MÀN THẤP VÀ GIỮ QUẢNG CÁO KHI XOAY, GẬP MÁY

## 1. Tóm tắt

Hai phần. (1) Bố cục: trang native toàn trang của một bước (`NativeFullScreenStep`, `:core:ads`) chia theo chiều cao app cho: từ 360 dp trở lên là cột như thiết kế, media co trước (không dưới 120 dp), rồi tới phần mô tả bớt dòng; từ 200 dp tới dưới 360 dp là hàng ngang gọn (media bên trái, chữ và CTA bên phải); dưới 200 dp trang không lấy quảng cáo. Trang "Flow steps" của màn xem thử không giữ khung thẻ 250 dp ở bước toàn trang: thẻ nằm đè lên đáy vùng bước, trang toàn trang dùng cả vùng tới nút Next, nút Next không đổi chỗ. (2) Giữ quảng cáo: khi activity dựng lại vì xoay hay gập, mở máy, quảng cáo đang hiện được gửi vào một chỗ giữ của `AdsController` (tối đa 10 giây) thay vì bị hủy; trang dựng lại lấy đúng quảng cáo đó, không lấy từ khe, không nạp mới. Kiểm chứng bằng test Robolectric đo CTA trong trang ở các chiều cao thật của màn xem thử (234 dp hôm nay, 484, 435, 255, 204 dp), test `ActivityScenario.recreate()` đếm lượt lấy, lượt hủy và quảng cáo được vẽ, unit test thuần cho chỗ giữ với đồng hồ giả.

## 2. Hiểu yêu cầu

Ticket xử lý KB-20 trong `features/ads/spec.md` (spec `confirmed` 2026-10-26), luật MC-06, OB-01, OB-02, O-01, O-08, G-03, S-11 trong `docs/ops/ads-business-rules.md`. Lỗi gốc: ảnh chuẩn `app/src/test/screenshots/AdGalleryScreen_flow-full-page_small.png` (ghi chú rà soát của T-036) và ghi chú rà soát của T-032 (xoay, gập máy thì trang mất quảng cáo).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-20 | Native toàn trang không có nút điều khiển riêng; nút Tiếp của app vẫn ở chỗ quen thuộc và không bị quảng cáo che. | Giữ nguyên (T-032). Thêm: mọi phần bắt buộc của quảng cáo (tiêu đề, nhãn "Ad", AdChoices, CTA) nằm trọn trong trang và CTA cao đủ 52 dp ở mọi chiều cao trang lấy quảng cáo; trang thấp hơn mức tối thiểu thì không lấy quảng cáo (như không có quảng cáo, không chừa khoảng trống). Nút Next của màn xem thử ở cùng chỗ trên mọi bước, kể cả khi thẻ 250 dp không còn giữ khung ở bước toàn trang (OB-01). |
| KB-20 | Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống. | Giữ nguyên. Áp thêm cho trường hợp mới: trang quá thấp, và quảng cáo đã giữ mà sau khi dựng lại vị trí tắt hay cổng đồng ý đóng (L-061, S-11): quảng cáo giữ không được lấy lại, bị hủy khi hết 10 giây, khe nạp lại như sau khi rời trang. |
| KB-20 (O-01) | Luật trong phạm vi ticket: mỗi lượt hiện sở hữu một quảng cáo, rời màn thì hủy, một quảng cáo chỉ ghi một lượt hiển thị. | Activity dựng lại vì đổi cấu hình (`isChangingConfigurations`) không phải "rời màn": cùng lượt hiện, cùng quảng cáo. Rời trang thật (sang bước khác, quay lại, đóng activity) vẫn hủy ngay như hôm nay. SDK chỉ ghi lượt hiển thị một lần cho một quảng cáo (O-01), nên vẽ lại cùng quảng cáo sau khi dựng lại không ghi lượt thứ hai; phía app, không có lượt lấy thứ hai từ khe, không gọi `ensureLoading`, không có log "Flow step full page shown" thứ hai (câu hỏi 2 về `af_native_ad_eligible`). |
| KB-20 (G-03) | Trang native toàn trang giữ chặn quảng cáo tự hiện khi đang hiện. | Sau khi dựng lại, trang vẽ lại quảng cáo giữ nên chặn lại ngay (`SuppressResumeAds`), như hôm nay. |

Thiết kế tham chiếu: `features/ads/design/README.md`, dòng "Trang native ở bước của luồng" (biến thể `Flow step` của `Native/FullScreen`, T-032) và `design/context/NativeFullScreenAdScreen_default_light.md` (lề 24/16/32/16 dp, hàng trên 36 dp, cách nhau 16 dp, media ít nhất 120 x 120 dp, đầu quảng cáo 48 dp, mô tả `bodyMedium` tối đa 3 dòng, CTA 52 dp). Biến thể `Flow step` không có ảnh thiết kế riêng (README mục 8, câu hỏi 2 của plan T-032). Bố cục hàng ngang gọn dưới 360 dp không có trong Figma (câu hỏi 1). Màn xem thử không có ảnh thiết kế; ảnh chuẩn `AdGalleryScreen_*` là bản gốc (README dòng 38).

ADR liên quan: ADR-0013, ADR-0020, ADR-0026, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

**Phần 1. Bố cục trang toàn trang (KB-20, O-08).**

Số đo trên code hiện tại (mục 4.1): `heightIn(min = 120.dp)` của media không có tác dụng trong `Column` có `weight`, nên khi thiếu chỗ media co về 0 rồi CTA bị ép: trang 360 dp thì media 64 dp; trang 255 dp thì media 0, CTA 27 dp; trang 200 dp thì CTA 0 dp. Trên màn xem thử `w360dp-h640dp` trang chỉ còn 234 dp (72 tới 306), chữ "Install" của CTA còn 6 dp.

1. File mới `NativeFlowStepLayout.kt` (`:core:ads`): `FlowStepPageLayout(assets, binder, media, adChoices)` thay nhánh `onClose == null` của `NativeFullScreenAdLayout` (nhánh sau inter giữ nguyên, ảnh `NativeFullScreenAdScreen_*` không đổi). Dùng `BoxWithConstraints` đọc chiều cao trang:
   - `maxHeight >= NativeFullScreenStepDefaults.ColumnMinHeight` (360 dp = lề trên 24 + hàng trên 36 + 16 + media 120 + 16 + đầu quảng cáo 48 + 16 + CTA 52 + lề dưới 32): cột như hôm nay. Số dòng mô tả `n = min(3, floor((maxHeight - 360 dp - 16 dp) / lineHeight))`, `lineHeight` là `bodyMedium.lineHeight` đổi sang dp theo `LocalDensity` (theo cả cỡ chữ hệ thống); `n <= 0` thì không vẽ mô tả. Media `weight(1f)` lấy phần còn lại, nên không bao giờ dưới 120 dp. Thứ tự co: media trước (tới 120 dp), rồi mô tả (3, 2, 1, 0 dòng).
   - `NativeFullScreenStepDefaults.MinHeight <= maxHeight < 360 dp` (`MinHeight` 200 dp = lề 16 + 16 và cột phải 36 + 16 + 48 + 16 + 52): hàng ngang gọn. Lề 16 dp bốn phía, cách nhau 16 dp. Trái: media cao hết hàng, rộng `max(120 dp, min(cao hàng, 40% chiều rộng))`. Phải (`weight(1f)`): hàng trên 36 dp chỉ có AdChoices (góc trên phải, O-08), đầu quảng cáo 48 dp (icon, tiêu đề, nhãn "Ad"), mô tả theo cùng cách tính dòng nếu còn chỗ, khoảng trống `weight`, CTA 52 dp rộng hết cột phải ở đáy.
   - Không có nhánh dưới 200 dp trong bố cục: cổng không vẽ trang khi đó (bước 2).
   - Phần quảng cáo vẫn đi qua `binder` như hôm nay (`HEADLINE`, `ICON`, `CALL_TO_ACTION`, media của SDK), nên quảng cáo thật và giả dùng chung bố cục.
2. `NativeFlowStep.kt`: `NativeFullScreenStepGate` bọc nội dung trong `BoxWithConstraints`; `maxHeight < MinHeight` thì coi như trang không `shown`: không lấy quảng cáo, không chặn quảng cáo tự hiện, báo `false` một lần nếu trước đó đã báo `true` (như nhánh `!allowed`). Chiều cao không giới hạn (trang đặt trong vùng cuộn) thì coi như đủ. Thêm `public object NativeFullScreenStepDefaults { val MinHeight = 200.dp }` và hằng `internal val ColumnMinHeight = 360.dp`, ghi vào KDoc của `NativeFullScreenStep` và `ads-guide.md`: app cho trang ít nhất `MinHeight`, nên cho từ 360 dp để có bố cục như thiết kế.
3. Màn xem thử, `FlowStepsPage` (`app/src/staging`): vùng bước là một `Box(weight(1f))` chứa `HorizontalPager(fillMaxSize)` và khung thẻ 250 dp đặt đè ở đáy (`Alignment.BottomCenter`, vẫn ngoài pager theo MC-06). Trang thường của pager đệm đáy 250 dp nên chữ "Step n of 3" ở giữa phần phía trên thẻ như hôm nay; trang toàn trang dùng cả vùng. Khung thẻ (tag `FLOW_CARD_FRAME`) chỉ dựng khi trang đứng yên không phải trang toàn trang. Chiều cao pager không đổi giữa các bước nên không nhảy khi chuyển trang; nút Next ở dưới vùng bước, cùng chỗ ở mọi bước (OB-01). Trên `w360dp-h640dp` trang toàn trang thành 484 dp (72 tới 556); trên `w891dp-h411dp` 255 dp, `w640dp-h360dp` 204 dp (hôm nay 5 dp và 0 dp).

**Phần 2. Giữ quảng cáo qua dựng lại activity (O-01, L-061, S-11).**

Hôm nay: dựng lại activity hủy composition, `fullScreenStepStates` trả quảng cáo trong `finally`, `releaseStepFullScreenAd` hủy nó và hẹn khe nạp lại; trang dựng lại thấy khe trống (`ensureLoading`), đợi quảng cáo mới, ghi lượt hiển thị mới.

4. File mới `NativeStepKeeper.kt`: lớp thuần `KeptStepAds(now: () -> Long, schedule: (delayMillis: Long, action: () -> Unit) -> Unit, release: (AdPlacement, LoadedNativeAd) -> Unit, logger: AppLogger)`, hằng `STEP_KEEP_MILLIS = 10_000`.
   - `keep(placement, ad)`: giữ `ad` cho `placement` kèm mốc giờ, hẹn `expire()` sau 10 giây. Đã giữ một quảng cáo khác cho cùng vị trí thì trả quảng cáo cũ qua `release` trước (một vị trí một khe một quảng cáo).
   - `reclaim(placement): LoadedNativeAd?`: lấy ra quảng cáo giữ còn trong 10 giây; quá hạn thì `release` nó và trả `null`.
   - `isKept(ad)`, `hasKept(placement)`.
   - `expire()`: `release` mọi quảng cáo giữ đã đủ 10 giây.
   - Log thẻ `Ads`: `Flow step full page kept across recreation: <mã vị trí>`, `... reclaimed: ...`, `... kept ad expired: ...`. Không ghi mã đơn vị.
5. `NativeFlowStep.kt`:
   - `fullScreenStepStates(take, ensureLoading, release, isKept: (LoadedNativeAd) -> Boolean = { false })`: trong `finally`, `if (!isKept(ad)) release(ad)`.
   - `NativeFullScreenStepGate` thêm tham số `keepOnRecreate: (LoadedNativeAd) -> Unit = {}`. Trong `FullPageOnScreen`, khi đang vẽ một quảng cáo: `DisposableEffect(ad) { onDispose { if (activity?.isChangingConfigurations == true) keepOnRecreate(ad) } }`, `activity = LocalActivity.current` (đã dùng ở `AdSlot.kt`, `FullScreenAds.kt`). `DisposableEffect` đặt sau `collectAsState`, nên khi composition bị hủy nó chạy trước `finally` của lượt thu thập (thứ tự ngược với thứ tự nhớ; mục 4.1 đo được `dispose` rồi `finally`, cả hai trước khi activity mới dựng).
   - Hàm nối `keptFullScreenStepStates(keeper, placement, take, ensureLoading, release)`: `take = { keeper.reclaim(placement) ?: take() }`, `isKept = keeper::isKept`. `AdsController` và test dựng lại dùng chung hàm này (L-072: một chỗ cho mọi đường lấy).
   - `NativeFullScreenStep` truyền `keepOnRecreate = { ads.keepStepFullScreenAd(placement, it) }`.
6. `AdsController.kt`: trường `keptStepAds = KeptStepAds(clock::nowMillis, schedule = { d, a -> scope.launch { delay(d); a() } }, release = ::releaseStepFullScreenAd, logger)`; `fullScreenStep(placement)` gọi `keptFullScreenStepStates`; lượt `reclaim` cũng đi qua `stepFullScreenTake(canRequestAds.value)` (S-11: cổng đóng thì không lấy lại, quảng cáo giữ hết hạn và bị hủy); `keepStepFullScreenAd(placement, ad)` gọi `keptStepAds.keep`; `stepFullScreenReady(placement) = keptStepAds.hasKept(placement) || isFullScreenAdReady(placement)`.
   - Vị trí tắt sau khi dựng lại (L-061): cổng không mở nên không ai `reclaim`; hết 10 giây `expire` hủy quảng cáo và khe nạp lại. Trang dựng lại không ở bước toàn trang (`shown = false`) hay quá thấp: như vậy.
   - Không có lượt hiển thị thứ hai: không có lượt lấy thứ hai từ khe, không `ensureLoading`, không log "Flow step full page shown" thứ hai; SDK không ghi lượt thứ hai cho cùng quảng cáo (O-01). `ReportNativeEligible` giữ như hôm nay (câu hỏi 2).
7. Tài liệu: `docs/ops/ads-guide.md` (dòng trang toàn trang: chiều cao tối thiểu, hai bố cục, giữ qua dựng lại 10 giây; dòng màn xem thử "Flow steps"); `docs/ops/ads-business-rules.md` thêm luật OB-03 (câu hỏi 3); `docs/features/ads/design/README.md` mục 8 thêm dòng ảnh chuẩn mới (bố cục gọn), cột "So với Figma" ghi "không có trong Figma".

Mẫu làm theo: cổng của `NativeFullScreenStepGate` và test của T-032; đo bố cục như `NativeFlowStepTest.nextButton_samePlace_notCovered_withFullPage`; dựng lại activity như `ResumeAdLifecycleTest.recreate_showsNothing` và `AdsActivityTrackerTest` (`ActivityScenario<ComponentActivity>.recreate()`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayout.kt` | `FlowStepPageLayout`: cột từ 360 dp (media co trước, tới 120 dp; mô tả bớt dòng), hàng ngang gọn từ 200 tới dưới 360 dp; tính số dòng mô tả theo chiều cao |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeStepKeeper.kt` | `KeptStepAds`, `STEP_KEEP_MILLIS` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `NativeFullScreenStepDefaults.MinHeight`; cổng đo chiều cao; `keepOnRecreate` và `DisposableEffect` trong `FullPageOnScreen`; `isKept` của `fullScreenStepStates`; `keptFullScreenStepStates`; `FlowStepFullPage` dùng `FlowStepPageLayout`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | Nhánh `onClose == null` của `NativeFullScreenAdLayout` gọi `FlowStepPageLayout` (`NativeFullScreenStepContent` đi qua đây); bỏ `FlowStepTopBar` nếu chuyển sang file mới; hằng `ColumnMinHeight` nếu đặt ở `NativeFullScreenAdDimens`; KDoc của `MediaMinHeight` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `keptStepAds`, `fullScreenStep` qua `keptFullScreenStepStates`, `keepStepFullScreenAd`, `stepFullScreenReady` tính cả quảng cáo giữ |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayoutTest.kt` | Đo bố cục ở các chiều cao (mục 4) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/KeptStepAdsTest.kt` | Unit test thuần của chỗ giữ với đồng hồ và bộ hẹn giả |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenStepRecreateTest.kt` | `ActivityScenario<ComponentActivity>.recreate()` với cổng thật, `KeptStepAds` thật, khe giả |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenStepStatesTest.kt` | Thêm test `isKept` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepTest.kt` | Thêm test cổng quá thấp |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepScreenshotTest.kt` | Thêm ảnh bố cục gọn |
| Tạo | `core/ads/src/test/screenshots/NativeFlowStep_full-page-compact_light.png` | Ảnh chuẩn mới, khung `w640dp-h360dp-xxhdpi` |
| Sửa | `core/ads/src/test/screenshots/NativeFlowStep_full-page_light.png`, `NativeFlowStep_full-page_dark.png`, `NativeFlowStep_full-page_small.png` | Chỉ ghi lại nếu đổi (trang 807 dp và 556 dp, đủ 360 dp; dự kiến không đổi) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreen.kt` | `FlowStepsPage`: khung thẻ đặt đè ở đáy vùng bước, chỉ dựng ở bước có thẻ; KDoc |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | Sửa `flowSteps_smallScreen_nextNotCovered` (bước toàn trang không còn khung 250 dp, theo phạm vi ticket); thêm test mục 4 |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenScreenshotTest.kt` | Thêm ảnh khung ngang |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_light.png`, `AdGalleryScreen_flow-full-page_small.png` | Ghi lại (trang lớn hơn, CTA đủ) |
| Tạo | `app/src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` | Ảnh chuẩn mới, khung `w640dp-h360dp-xxhdpi` |
| Sửa | `app/src/test/screenshots/AdGalleryScreen_flow-card_light.png`, `AdGalleryScreen_flow-card_small.png` | Chỉ ghi lại nếu đổi (thẻ đặt đè thay vì nằm dưới pager; vị trí dự kiến không đổi) |
| Sửa | `docs/ops/ads-guide.md` | Dòng trang toàn trang và màn xem thử (mục 3.1 bước 7) |
| Sửa | `docs/ops/ads-business-rules.md` | Thêm OB-03 (câu hỏi 3) |
| Sửa | `docs/features/ads/design/README.md` | Mục 8: dòng ảnh `NativeFlowStep_full-page-compact_light`; dòng "Trang native ở bước của luồng" ghi hai bố cục |

L-020, nơi đọc ảnh chuẩn: `scripts/check.sh` (so ảnh qua `verifyRoborazzi*`, đòi ảnh của mỗi `captureRoboImage` có trong cây), `scripts/test-summary.sh` (đếm số ảnh), `docs/features/ads/design/README.md` mục 8 (đã có trong bảng). `scripts/design-compare.sh` chỉ đọc ảnh của `feature/<tên>`, không đọc `core/ads` hay `app`. Không có test hay workflow nào đọc tên các ảnh này. Nơi nhắc `docs/ops/ads-business-rules.md` trong test (`core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`) chỉ nhắc mã luật trong chú thích, không đọc file; task chỉ thêm dòng OB-03, không đổi mã luật cũ, nên các test này không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay đổi định dạng vị trí. Đổi cách vẽ và vòng đời quảng cáo của `native_fullscreen` dùng làm trang của luồng (`gallery_native_step_full` trên staging; vị trí của app sau này). Bớt lượt nạp: xoay, gập máy không còn hủy rồi nạp lại.
- Sự kiện phân tích: Không chạm (câu hỏi 2: `af_native_ad_eligible` giữ như hôm nay).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `LocalActivity` từ `libs.androidx.activity.compose`, `BoxWithConstraints` từ Compose foundation, đã có ở `:core:ads` (`implementation(libs.androidx.activity.compose)`; `LocalActivity` đã dùng ở `AdSlot.kt`). Test dùng `ActivityScenario` (androidx.test.core) như `ResumeAdLifecycleTest`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên máy thật, thứ tự giữa `onDispose` của composition cũ và lần `reclaim` của composition mới (mục 4.1 chỉ đo trên Robolectric; phương án chính không phụ thuộc vào thứ tự `finally` vì `keep` chạy đồng bộ trong `onDispose`). Chỗ chưa chắc thứ hai: `BoxWithConstraints` trong cổng làm trang bên cạnh trong pager bị đo lại khiến test `nextButton_*` của T-032 đổi.
- **Điều kiện chuyển:** test `NativeFullScreenStepRecreateTest` vẫn đỏ sau 2 lần sửa vì quảng cáo bị trả về (`destroyCount` 1) trước khi `keep` chạy, hoặc kiểm thủ công trên máy thật (mục 4) thấy lượt nạp mới sau khi xoay. Với điểm thứ hai: test cũ của `NativeFlowStepTest` đỏ vì đo lại.
- **Cách làm thay:** (a) giữ quảng cáo bằng một `ViewModel` nhỏ của `:core:ads` gắn vào `ViewModelStoreOwner` của activity (`LocalActivity.current as? ViewModelStoreOwner`, lớp `ViewModelProvider` đã có trên classpath của `:core:ads` qua `libs.androidx.activity.compose`), `onCleared()` trả quảng cáo; `onDispose` khi không đổi cấu hình vẫn trả ngay. Không thêm thư viện. (b) Đo chiều cao bằng `Modifier.onSizeChanged` của khung trang thay cho `BoxWithConstraints` (khung đầu chưa lấy quảng cáo cho tới khi có kích thước). Test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test viết trước, thất bại đúng lý do trên code hiện tại. Số đo hiện tại ghi ở mục 4.1. Khung đo tái hiện lỗi gốc: `w360dp-h640dp-xxhdpi` như ảnh `AdGalleryScreen_flow-full-page_small`, và các khung ngang mà xoay máy tạo ra.

**`NativeFlowStepLayoutTest`** (`:core:ads`, Robolectric, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`). Trang `NativeFullScreenStepContent(assets, media = { FakeMedia() }, adChoices = { FakeAdChoices() })` trong một `Box(fillMaxWidth().height(h))` có tag; đo bằng `getUnclippedBoundsInRoot` (`useUnmergedTree = true`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20 tiêu chí 3, O-08: phần bắt buộc nằm trong trang ở mọi chiều cao lấy quảng cáo | Compose UI test | `requiredParts_insidePage_atEveryHeight`: với `h` trong 484, 435, 400, 360, 359, 255, 204, 200 dp và cả `FakeNativeAdAssets.Design`, `Longest`: CTA (`NativeFullScreenAdTestTags.CTA`) cao 52 dp (sai số 0,5), `top >= page.top`, `bottom <= page.bottom`; tiêu đề (chữ `headline`), nhãn "Ad", `FAKE_AD_CHOICES_TAG` nằm trong trang; AdChoices không đè CTA hay tiêu đề. Hôm nay đỏ: 255 dp CTA cao 27 dp, 200 dp cao 0 dp |
| Media không dưới mức của SDK (design README, Media) | Compose UI test | `media_neverBelow120dp`: cùng các chiều cao, media (`MEDIA`) cao và rộng ít nhất 120 dp (sai số 0,5). Hôm nay đỏ: 360 dp media 64 dp, 255 và 200 dp media 0 dp |
| Thứ tự co: media trước, rồi mô tả | Compose UI test | `shrinkOrder_mediaFirst_thenBody` với `Longest`: 484 dp thì mô tả 3 dòng (cao 60 dp) và media trên 120 dp; 436 dp thì mô tả 3 dòng và media 120 dp; 400 dp thì mô tả không quá 1 dòng (cao tối đa 20,5 dp) và media 120 dp; 360 dp thì không có chữ mô tả. Hôm nay đỏ ở 400 dp (media dưới 120 dp, mô tả 3 dòng) |
| Bố cục gọn dưới 360 dp | Compose UI test | `compactRow_below360dp`: 255 dp: media nằm bên trái tiêu đề và CTA (`media.right <= cta.left`), CTA ở đáy (`page.bottom - cta.bottom` bằng 16 dp, sai số 0,5); đối chứng 360 dp: media ở trên đầu quảng cáo (`media.bottom <= cta.top`), cột như thiết kế, media bắt đầu ở y = 76 dp như T-032 (`flowStep_mediaStartsLikeAfterInter` vẫn xanh) |
| Cỡ chữ hệ thống lớn | Compose UI test | `requiredParts_insidePage_largeFont`: `@Config(fontScale = 1.3f)`, `Longest`, 484 và 255 dp: CTA trong trang, cao 52 dp |

**`NativeFlowStepTest`** (sửa, thêm):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-20 tiêu chí 4: trang quá thấp thì như không có quảng cáo | Compose UI test | `fullScreenStep_belowMinHeight_takesNothing`: cổng `allowed = true` trong `Box(height(190.dp))` với `fullPageStates(ad, takes)`: `takes` rỗng, không có `CTA`, `suppressor.suppressed` là `false`. Đối chứng (L-055): cùng test với 200 dp thì `takes` có 1 và CTA có. Hôm nay đỏ (190 dp vẫn lấy quảng cáo) |
| Trang co lại dưới mức khi đang hiện | Compose UI test | `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`: chiều cao `mutableStateOf(484.dp)` đổi sang 190 dp: `released` có đúng `ad`, báo cáo `[true, false]`, không còn chặn |

**`KeptStepAdsTest`** (JVM thuần, đồng hồ giả, bộ hẹn giả ghi lại `(delay, action)`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| O-01: dựng lại là cùng lượt hiện | Unit test | `keep_thenReclaim_returnsSameAd_noRelease`: `keep(P, a)`, giờ + 1 s, `reclaim(P)` trả `a` (cùng đối tượng), `released` rỗng, `isKept(a)` thành `false` sau `reclaim` |
| Hết 10 giây thì hủy | Unit test | `expire_after10s_releasesOnce`: `keep`, chạy hành động đã hẹn ở giờ + 10 000 ms: `released == [(P, a)]`, `reclaim(P)` trả `null`. Đối chứng: ở 9 999 ms gọi `expire()` thì chưa trả |
| Lấy lại quá hạn | Unit test | `reclaim_afterWindow_releasesAndReturnsNull` |
| Một vị trí một quảng cáo | Unit test | `keepTwice_samePlacement_releasesOlder`: `keep(P, a)`, `keep(P, b)`: `released == [(P, a)]`, `reclaim(P) === b` |
| Hai vị trí riêng (L-068) | Unit test | `twoPlacements_keptSeparately`: `keep(P, a)`, `keep(Q, b)`: `reclaim(Q) === b`, `reclaim(P) === a`; hai lượt chồng nhau, `expire` chỉ trả cái quá hạn |
| `hasKept` cho `stepFullScreenReady` | Unit test | `hasKept_trueUntilReclaimedOrExpired` |
| Log không có mã đơn vị | Unit test | `logs_keepReclaimExpire_withPlacementKeyOnly` (`RecordingAppLogger`) |

**`FullScreenStepStatesTest`** (sửa, thêm):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Quảng cáo giữ không bị trả khi lượt thu thập dừng | Unit test | `cancel_keptAd_notReleased`: `isKept = { true }`, hủy lượt thu thập: `released` rỗng. Đối chứng `cancel_notKept_released` (`isKept = { false }`): `released == [ad]` (hành vi T-032) |

**`NativeFullScreenStepRecreateTest`** (`:core:ads`, Robolectric, `ActivityScenario.launch(ComponentActivity::class.java)`; mỗi activity mới gọi lại `setContent` trong `onActivity`, như app làm trong `onCreate`). Nội dung: `AdsHostContent(suppression, suppressor) { NativeFullScreenStepGate(allowed, states = keptFullScreenStepStates(keeper, P, take = { slot.removeFirstOrNull().also { takes++ } }, ensureLoading = { loads++ }, release = { released += it }), keepOnRecreate = { keeper.keep(P, it) }, adContent = { drawn += it; ... }) }`, `keeper` là `KeptStepAds` thật với đồng hồ giả và bộ hẹn giả; `allowed`, `shown`, cổng đồng ý là biến ngoài activity (sống qua dựng lại như `AdsController`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Xoay, gập máy: cùng quảng cáo, không nạp mới, không hủy | Compose UI test | `recreate_keepsSameAd_noNewTake_noRelease`: sau `recreate()`: `takes == 1`, `loads == 0`, `released` rỗng, `ad.destroyCount == 0`, `drawn == [ad, ad]` (cùng đối tượng), CTA có trong cây. Hôm nay đỏ: `released == [ad]`, `takes == 2` |
| Không ghi lượt hiển thị thứ hai phía app | Compose UI test | `recreate_noSecondTurn`: qua hai lần `recreate()`, `takes == 1` (lượt lấy từ khe, nơi `AdsController` ghi log "Flow step full page shown", chỉ chạy khi `reclaim` trả `null`), `RecordingAppLogger` của `KeptStepAds` có đúng hai dòng "kept across recreation" và hai dòng "reclaimed", không có "kept ad expired" |
| Hai lần dựng lại liên tiếp | Compose UI test | `recreateTwice_stillOneAd` |
| G-03 sau dựng lại | Compose UI test | `recreate_resumeAdsStillSuppressed`: `suppressor.suppressed.value` là `true` sau `recreate()` |
| Rời trang thật vẫn hủy (O-01, đối chứng L-055) | Compose UI test | `replaceContent_withoutRecreate_releases`: `onActivity { it.setContent { } }`: `released == [ad]`, `keeper.hasKept(P)` là `false` |
| Đóng activity vẫn hủy | Compose UI test | `close_releases`: `scenario.close()`: `released == [ad]` |
| Sau dựng lại sang bước khác | Compose UI test | `recreate_thenNotShown_releasesKeptAd`: sau `recreate()`, `shown = false`: `released == [ad]` một lần |
| L-061: vị trí tắt giữa chừng | Compose UI test | `recreate_placementOff_releasedAfterWindow_neverDrawnAgain`: tắt `allowed` trước `recreate()`; sau `recreate()`: `drawn == [ad]`, chạy hành động hẹn 10 s: `released == [ad]` một lần, bật lại `allowed` thì lấy từ khe (`takes == 2`), không lấy lại `ad` |
| S-11: cổng đồng ý đóng sau dựng lại | Compose UI test | `recreate_consentClosed_notReclaimed`: lượt lấy đi qua `stepFullScreenTake(canRequest, ...)` như `AdsController`; đóng cổng trước `recreate()`: không vẽ lại, hết 10 s thì `released == [ad]` |
| Trang dựng lại quá thấp (khung ngang của app) | Compose UI test | `recreate_belowMinHeight_notReclaimed_releasedAfterWindow`: sau `recreate()` chiều cao trang 190 dp |

**Màn xem thử** (`AdGalleryScreenTest`, `:app` staging; CTA tìm bằng chữ `Install` của `previewAdAssets`, `useUnmergedTree = true`):

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Bước toàn trang không giữ khung 250 dp (lỗi gốc) | Compose UI test | `flowSteps_fullPage_fillsStepArea_small` (`w360dp-h640dp-xxhdpi`): sau Next, trang (`STEP_FULL_PAGE`) từ 72 dp tới `next.top - 16 dp` (cao 484 dp, sai số 0,5), không có `FLOW_CARD_FRAME`, chữ `Install` nằm trọn trong trang và cao 20 dp. Hôm nay đỏ: trang 234 dp, chữ CTA 6 dp |
| Khung ngang sau khi xoay | Compose UI test | `flowSteps_fullPage_landscape_showsCta` (`w640dp-h360dp-xxhdpi` và `w891dp-h411dp-xxhdpi`, hai test): trang cao ít nhất 200 dp, chữ `Install` trong trang. Hôm nay đỏ: trang 0 dp và 5 dp |
| OB-01: Next cùng chỗ mọi bước | Compose UI test | `flowSteps_nextBounds_sameOnCardFullPageAndEmptyCard` (có sẵn, giữ xanh) |
| Next không bị che, thẻ 250 dp ở bước thường | Compose UI test | `flowSteps_smallScreen_nextNotCovered` (sửa): bước thường khung 250 dp, Next dưới khung; bước toàn trang không có khung, trang dừng trên Next, Next trong màn |
| Không nhảy giao diện khi chuyển bước | Compose UI test | `flowSteps_pagerHeight_sameOnCardAndFullPage`: `FLOW_PAGER` cùng bounds ở bước 1 và bước toàn trang. Đối chứng: khi chạy thử thay khung bằng `height(if (fullPage) 0 else 250)` (mục 4.1) thì test này đỏ |
| Trang toàn trang chỉ một native mỗi bước | Compose UI test | `flowSteps_fullPage_hidesCard` (có sẵn, giữ xanh) |

**Screenshot test:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn xem thử, bước toàn trang | Screenshot test | Ghi lại `src/test/screenshots/AdGalleryScreen_flow-full-page_light.png` |
| Màn thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | Ghi lại `src/test/screenshots/AdGalleryScreen_flow-full-page_small.png` (CTA đủ 52 dp) |
| Khung ngang | Screenshot test khung `w640dp-h360dp-xxhdpi` | `flowFullPage_landscape`: `src/test/screenshots/AdGalleryScreen_flow-full-page_landscape.png` |
| Bố cục gọn của trang | Screenshot test khung `w640dp-h360dp-xxhdpi` | `fullPage_compact_light` trong `NativeFlowStepScreenshotTest`: `src/test/screenshots/NativeFlowStep_full-page-compact_light.png` (bước có nút Next như ảnh `full-page` có sẵn) |
| Đúng thiết kế (khi có `design/screens/`) | Không chạy so sánh thiết kế: task này không có ảnh thiết kế, biến thể của trang toàn trang ở bước của luồng không có ảnh riêng (README mục 8). Ảnh cột của trang (`NativeFlowStep_full-page_light`, `_dark`, `_small`) không đổi ở 807 dp và 556 dp; người duyệt PR so ảnh gọn với component toàn màn hình trên Figma (câu hỏi 1) | Xem dòng dưới |
| | | Ảnh thiết kế của ticket khác (T-027, T-028, T-030): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light`, `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light`, `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` |

**Kiểm tra thủ công:**

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Xoay máy giữ quảng cáo thật, không nạp mới | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, màn xem thử, "Flow steps", Next tới trang toàn trang; xoay ngang rồi dọc: cùng quảng cáo, CTA thấy và bấm được ở cả hai hướng; log `VGL.Ads` có "kept across recreation", "reclaimed", không có "Flow step full page left" hay "Reload gallery_native_step_full" giữa hai lần xoay; Ad Inspector không có lượt yêu cầu mới |
| Gập, mở máy | Kiểm tra thủ công (máy thật: gập có bản lề) | Như trên trên máy gập; trên emulator thường dùng `device unfold` của `device-session` nếu có máy gập ảo |
| Không có lượt hiển thị thứ hai | Kiểm tra thủ công (máy thật: quảng cáo thật) | Ad Inspector hay log `ad_impression` của Firebase DebugView: một lượt cho quảng cáo đó qua hai lần xoay |

Không có tệp kịch bản emulator: lệnh `fresh` mở `MainActivity`, không mở được màn xem thử; quảng cáo trên emulator của `device-session` bị từ chối đồng ý (`consent decline`) nên trang toàn trang không có quảng cáo để kiểm.

**Ngoại lệ viết test trước:** tài liệu (`ads-guide.md`, `ads-business-rules.md`, design README); dòng nối trong `AdsController.kt` (`keptStepAds`, `keepStepFullScreenAd`, `stepFullScreenReady`) và `NativeFullScreenStep` truyền `keepOnRecreate`: đường đi của chúng được test qua `keptFullScreenStepStates` và cổng thật (L-072), phần còn lại là gọi hàm, như phần nối của T-032; ảnh chuẩn ghi ở bước 5.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, worktree của nhánh `claude/plan-T-041` (từ nhánh chính `6077815`), hai file test tạm không commit (đã xóa): `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/DryRunT041Test.kt` (đo bố cục và thứ tự hủy khi dựng lại) và `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/DryRunT041GalleryTest.kt` (đo màn xem thử ở năm khung); cùng một khung API tạm: tham số `isKept` của `fullScreenStepStates` và khung thẻ cao 0 dp ở bước toàn trang của màn xem thử.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*DryRunT041*' :app:testStagingDebugUnitTest --tests '*DryRunT041*'`, rồi `scripts/check.sh` với test tạm và khung API tạm trong cây (JDK của Android Studio, `ANDROID_HOME=$HOME/Library/Android/sdk`).
- **Kết quả, bố cục (L-029)** (`NativeFullScreenStepContent` trong khung `w360dp`, chiều cao cho trước): 484 dp: media 168 dp, CTA 400 tới 452 dp; 436 dp (`Longest`): media 120 dp; 436 dp (`Design`): media 140 dp; 360 dp: media 64 dp; 255 dp: media 0 dp, CTA 196 tới 223 dp (27 dp); 200 dp: media 0 dp, CTA 0 dp. `heightIn(min = 120.dp)` của media không giữ được 120 dp.
- **Kết quả, màn xem thử (L-029)** (bước toàn trang sau một lần Next): `w360dp-h640dp`: pager và trang 72 tới 306 dp (234 dp), khung thẻ 306 tới 556 dp, Next 572 tới 624 dp, chữ CTA 268 tới 274 dp (6 dp); `w411dp-h891dp`: trang 485 dp; `w673dp-h841dp` (mở máy gập): 435 dp; `w891dp-h411dp`: 5 dp; `w640dp-h360dp`: 0 dp. Bỏ khung 250 dp ở bước toàn trang thì trang thành 484, 735, 685, 255, 204 dp.
- **Kết quả, dựng lại (E = 2)** (`ActivityScenario<ComponentActivity>.recreate()`, `LocalActivity`, một `DisposableEffect` và một `LaunchedEffect` chờ `awaitCancellation`): thứ tự `take`, `recreate`, `dispose changing=true`, `finally changing=true`, rồi activity mới `take`; đóng scenario: `dispose changing=false`, `finally changing=false`. `isChangingConfigurations` của activity cũ đọc được trong cả `onDispose` lẫn `finally`, và cả hai chạy trước composition mới.
- **Kết quả, `scripts/check.sh`** (test tạm và khung API tạm trong cây): 1376 test JVM chạy, 3 đỏ, đều thuộc mục 3.2: `AdGalleryScreenTest.flowSteps_smallScreen_nextNotCovered` ("card frame height expected 250.0 but was 0.0", test sửa theo phạm vi ticket), `AdGalleryScreenScreenshotTest.flowFullPage_light` và `flowFullPage_small` (ảnh chuẩn `AdGalleryScreen_flow-full-page_light`, `_small` đổi, ghi lại theo plan). Test tạm qua. `:core:ads:verifyRoborazziDebug` qua (ảnh `NativeFlowStep_*` không đổi với khung API tạm); `AdGalleryScreen_flow-card_*` không đổi. `ktlint` đỏ chỉ ở hai file test tạm (thứ tự import, xuống dòng); `:app:lintStagingDebug`, `:app:assembleStagingDebug` chạy xong. Không có test đỏ ngoài mục 3.2. Đã hoàn lại khung API tạm và xóa test tạm. Không thêm thư viện nên không chạy `:app:assembleStagingRelease` (L-083).
- Phương án dự phòng 3.4 chưa thử: phương án chính chạy trên Robolectric; điểm chưa chắc chỉ lộ ra trên máy thật.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi vòng đời và cách vẽ quảng cáo của định dạng `native_fullscreen` khi làm trang của luồng; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm vị trí, quyền, SDK, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì task đổi lượt nạp của trang toàn trang khi xoay máy và thêm luật OB-03.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads`, `:app` staging); API công khai mới `NativeFullScreenStepDefaults`; hai file code mới, ba file sửa |
| B. Độ rõ của yêu cầu | 1 | Ticket và KB-20 rõ; AI tự quyết bố cục gọn dưới 360 dp (không có trong Figma), mức 200 dp, cửa sổ giữ 10 giây, sự kiện eligible (câu hỏi 1 tới 4) |
| C. Khả năng kiểm chứng tự động | 2 | Bố cục, cổng và dựng lại có test Robolectric; quảng cáo thật qua xoay, gập máy, lượt hiển thị của SDK chỉ kiểm được trên máy thật |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `ads-guide.md`, `AdGalleryScreenTest.kt` cũng có thể bị T-042, T-044 sửa (mục 6) |
| E. Độ mới của kỹ thuật | 2 | Giữ tài nguyên qua dựng lại activity bằng `isChangingConfigurations` trong `onDispose` lần đầu dùng trong dự án; đã chạy thử (mục 4.1). `BoxWithConstraints` và `recreate()` đã có mẫu |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeFlowStep.kt`, `NativeFullScreenAdScreen.kt`, `AdsController.kt` của `:core:ads`; `AdGalleryScreen.kt` (staging); `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`, `docs/features/ads/design/README.md`; ảnh chuẩn ở mục 3.2. Bố cục sau inter (`NativeFullScreenAdScreen`, `NativeFullScreenOverlay`) không đổi: chỉ nhánh `onClose == null` đổi.
- App dùng `NativeFullScreenStep` hôm nay: chỉ màn xem thử staging (`gallery_native_step_full`); chưa có vị trí của app.
- **T-044 (bổ sung test quảng cáo, chạy trước T-041):** sửa `AdGalleryScreenTest.kt` (`buttons_haveTouchTargetOf48dp` đo cả 12 nút) và có thể `NativeAdCardTest.kt`. T-041 sửa test khác trong cùng file `AdGalleryScreenTest.kt` (nhóm `flowSteps_*`), khác chỗ; task chạy sau gộp nhánh chính rồi sửa xung đột theo chữ của T-044.
- **T-042 (rewarded bấm đúp, chạy trước):** sửa `RewardedAction.kt`, `RewardedTurns.kt`, có thể `AdsController.kt` và `ads-guide.md` (dòng rewarded). T-041 chỉ thêm trường và hàm quanh `fullScreenStep` trong `AdsController.kt` và dòng trang toàn trang của `ads-guide.md`; khác chỗ.
- **T-043 (resume kiểm vị trí, chạy trước):** sửa `ResumeAdTurns.kt`; không chung file với T-041. T-041 không đổi `SuppressResumeAds` hay `ResumeSuppression`.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: cuối, như danh sách T-043, T-042, T-044, T-041; T-041 gộp nhánh chính sau T-044 trước khi viết test của màn xem thử.

## 7. Điều kiện dừng riêng

- Ảnh chuẩn `NativeFullScreenAdScreen_*` hay `NativeFullScreenOverlay_*` (bố cục sau inter) đổi: bố cục sau inter không thuộc phạm vi; dừng và báo.
- Giữ quảng cáo cần đổi `AdPreloader`, `FreshAdCache` hay cách khe `native_fullscreen` nạp sẵn (ngoài `releaseStepFullScreenAd`): ngoài phạm vi, dừng và hỏi.
- Muốn đổi `ReportNativeEligible` hay số lần ghi `af_native_ad_eligible` (điểm chặn 4) khi câu hỏi 2 chưa có câu trả lời khác đề xuất: không làm, ghi vào báo cáo.

## 8. Câu hỏi cho người duyệt

1. **Bố cục gọn dưới 360 dp không có trong Figma.** Khung ngang của điện thoại (255 dp và 204 dp ở màn xem thử) không đủ cho cột của thiết kế (cần 360 dp với media 120 dp).
   - Đề xuất của AI: hàng ngang gọn như mục 3.1 bước 1 (media trái ít nhất 120 dp, chữ và CTA phải, AdChoices góc trên phải), trang tối thiểu 200 dp; dưới 200 dp không lấy quảng cáo. Người duyệt PR so ảnh `NativeFlowStep_full-page-compact_light` với component `Native/FullScreen`; muốn có trong Figma thì thêm biến thể `Flow step / Compact` sau. Phương án khác, nhỏ hơn: chỉ có cột, mức tối thiểu 360 dp; khi xoay ngang trên điện thoại trang không lấy quảng cáo (quảng cáo giữ hết hạn và bị hủy), tức phần 2 chỉ có ích khi gập, mở máy.
2. **`af_native_ad_eligible` sau khi dựng lại.** `ReportNativeEligible` ghi một lần mỗi lần cổng vào composition; hôm nay mọi khe native ghi lại sau khi xoay máy. Trang giữ quảng cáo vẫn ghi thêm một lần.
   - Đề xuất của AI: giữ như mọi khe native (đổi là điểm chặn 4, nên tách ticket nếu muốn đếm "một lượt hiện một lần eligible" cho mọi khe). Ghi vào `ads-guide.md` rằng eligible tính theo lần vào màn, kể cả dựng lại.
3. **Thêm luật OB-03 vào `ads-business-rules.md`?** O-01 ghi "rời màn thì hủy"; task coi dựng lại activity không phải rời màn.
   - Đề xuất của AI: thêm OB-03: "Trang native toàn trang của luồng giữ quảng cáo qua dựng lại activity (xoay, gập, mở máy): tối đa 10 giây, cùng quảng cáo, không nạp mới; không lấy lại khi vị trí tắt hay cổng đồng ý đóng; quá hạn thì hủy và khe nạp lại. Nguồn: rà soát T-032". Thẻ 250 dp (pool) và các khe native khác giữ như hôm nay (ticket không bao gồm).
4. **Cửa sổ giữ 10 giây.** Dựng lại thường xong dưới 1 giây; khung đầu của trang mới chưa mở cổng (khóa `is_show_*` đọc từ `Flow`).
   - Đề xuất của AI: 10 giây, hằng số cứng (`STEP_KEEP_MILLIS`), không thành khóa Remote Config. Ngắn hơn có thể mất quảng cáo trên máy chậm; dài hơn chỉ giữ thêm một quảng cáo không ai xem.
5. **Tách phần 2 thành ticket riêng?** Phần 1 (bố cục, màn xem thử) và phần 2 (giữ quảng cáo) độc lập; phần 2 chạm vòng đời quảng cáo trong `AdsController`.
   - Đề xuất của AI: không tách. Chạy thử đã cho thấy cách giữ chạy trên Robolectric (mục 4.1), phần 2 thêm khoảng 150 dòng code và ba file test, và phần 1 cần có trước để quảng cáo giữ hiện được ở khung ngang. Nếu anh muốn tách: T-041 giữ phần 1 (bước 1 tới 3, `NativeFlowStepLayoutTest`, test màn xem thử, ảnh chuẩn); ticket mới nhận bước 4 tới 6, `NativeStepKeeper.kt`, `KeptStepAdsTest`, `NativeFullScreenStepRecreateTest`, dòng `FullScreenStepStatesTest`, OB-03, và phụ thuộc T-041.
6. **Thẻ 250 dp ở khung ngang của màn xem thử.** Ở `w891dp-h411dp` vùng bước thường chỉ còn 5 dp phía trên thẻ (hôm nay và sau task, vì thẻ 250 dp ở ngoài pager). Ticket ghi "Thẻ 250 dp ngoài pager (không lỗi)".
   - Đề xuất của AI: không sửa trong task này; màn xem thử là công cụ của nhóm. App thật đặt thẻ theo spec của app (có thể cuộn nội dung bước).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
