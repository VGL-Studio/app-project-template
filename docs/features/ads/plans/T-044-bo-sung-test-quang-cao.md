---
ticket: T-044
title: Bổ sung test quảng cáo còn yếu theo rà soát RUN-20261026-03 và RUN-20261026-04
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 7M           # cỡ S, số gợi ý 4.1M (scripts/token-usage.py estimate, 2026-10-26); cộng bố cục FULL bằng Layout riêng (mục 4.1), lối vào cho test, hàm phụ cỡ chữ dùng chung
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-044: BỔ SUNG TEST QUẢNG CÁO CÒN YẾU THEO RÀ SOÁT RUN-20261026-03 VÀ RUN-20261026-04

## 1. Tóm tắt

Thêm hay làm chặt test cho ba điểm nên sửa mà rà soát độc lập của T-026, T-027, T-036 nêu nhưng phiên chưa sửa vì không có test đỏ trước: lượt native toàn màn hình độc lập đi qua `showFullScreenAd` tới `skipTurn` (`NOT_READY` khi không có quảng cáo, `BUSY` qua nhánh `nativeTurns.busy ||`); thẻ native `FULL` 418 dp giữ đủ cỡ CTA, hàng sao và hai dòng mô tả với chữ dài và cỡ chữ hệ thống lớn; `buttons_haveTouchTargetOf48dp` đo cả 12 nút của màn xem thử. Chạy thử (mục 4.1) cho thấy test của thẻ `FULL` đỏ trên code hiện tại vì một lỗi thật: ở cỡ chữ 1.3 hàng sao bị ép còn 6 dp, mô tả còn 1 dòng; ở 2.0 hàng sao mất hẳn, CTA còn 32,7 dp. Plan sửa `FullLayout` theo luật "media co trước, không dưới 120 dp" bằng một `Layout` riêng (ở cỡ chữ 1.0 không đổi gì, ảnh chuẩn giữ nguyên). Thêm test cho nhánh `turn != null` của combo (lượt combo gặp lượt native khác đang mở). Mỗi test mới có bước đối chứng (L-055), bước đối chứng của hai test nhánh bận đã chạy thử.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed`) và luật O-08, mục 5.7, F-01, F-02, F-04, F-07, N-02 của `docs/ops/ads-business-rules.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-5 | Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng. | Lượt native toàn màn hình độc lập gọi qua `showFullScreenAd` (đường mà `rememberShowFullScreenAd` dùng) khi khe trống: không hiện gì, `onFinished` chạy đúng một lần ngay, log `skipped: NOT_READY`, cờ "đang hiện" không bật. Test cũ `standalone_noAd_showsNothing_finishesAtOnce` chỉ gọi `beginStandaloneNativeTurn` và khẳng định `finishes == 0` ("nơi gọi sẽ bỏ lượt"), không đi qua `showNativeFullScreen` tới `skipTurn` (điểm nên sửa vòng 1 của rà soát T-026). |
| KB-16 (và F-02) | Khi interstitial bị chặn ngay từ đầu (… đang có quảng cáo khác) thì người dùng đi tiếp ngay, không có native. | Áp cho lượt độc lập: một lượt native còn mở quá trần 10 phút (F-02) thì cổng chung không còn thấy "đang hiện" (`presence.isBusy` trả `false`), cổng trả `SHOW`, `beginStandaloneNativeTurn` từ chối; nhánh `nativeTurns.busy ||` của `showNativeFullScreen` (`AdsController.kt:1054`) phải ghi lượt bỏ là `BUSY`, không phải `NOT_READY`, không lấy quảng cáo ra khỏi khe. Nhánh này chưa có test (điểm nên sửa vòng 2 của rà soát T-026). Ticket ghi "nhánh `nativeTurns.busy ||` của combo inter rồi native" là ghi nhầm: điểm rà soát (`docs/runs/RUN-20261026-04.md:100`, nhật ký plan T-026) là dòng của lượt độc lập trong `showNativeFullScreen`. Nhánh bận của combo (`NativeFullScreenTurns.afterInterstitial`, `NativeFullScreenTurns.kt:106`, `turn != null || !port.beginPresence()`) có test cho vế `beginPresence()` (`secondTurn_whilePresenceBusy_finishesAtOnce`, `capExpired_reachesCombo_busyFinishesAtOnce`) nhưng chưa có test cho vế `turn != null` (lượt native khác đang mở mà cờ "đang hiện" trống, ví dụ quá trần F-02): task thêm test cho vế này (mục 4, dòng 4); chạy thử ở mục 4.1 cho thấy bỏ vế này thì chỉ test mới đỏ. |
| KB-2 | Khi có quảng cáo thì thấy thêm ảnh hoặc video của quảng cáo, vùng ảnh ít nhất 120 x 120 dp, đúng tỷ lệ, không kéo giãn hay cắt. Các tiêu chí giữ chỗ… như KB-1. | Thẻ `NativeAdSize.FULL` (T-027) cao cố định 418 dp bằng khung chờ. `full_placeholderHeight_equalsCardHeight_shortAndLongTexts` không đỏ được vì thẻ bị ép `height(heightOf(size))`. Thay bằng kiểm từng phần: CTA đủ 36 dp, hàng sao đủ 16 dp và đáy hai phần này nằm trong vùng nội dung của thẻ (cách đáy 13 dp), mô tả đủ 2 dòng, media còn ít nhất 120 dp, với chữ ngắn, chữ dài và cỡ chữ hệ thống 1.0, 1.3, 2.0. Cỡ 1.3 là nấc lớn nhất của Android tới bản 13; 2.0 là nấc lớn nhất của Android 14 trở lên. Luật của thẻ (trả lời câu hỏi 1): media co trước, không dưới 120 dp; quá mức đó (chỉ có ở cỡ chữ giả lập 3.0) thì thẻ cao thêm, CTA và hàng sao vẫn đủ cỡ. |
| KB-8 | Khi mở màn hình xem thử quảng cáo… thấy từng loại native, nút gọi… (thêm 2026-10-23: app open, rewarded, combo, thanh đáy, native giới thiệu, ô lưới). | Mọi nút của trang mặc định có vùng chạm ít nhất 48 x 48 dp. `buttons_haveTouchTargetOf48dp` đếm 12 nút nhưng chỉ đo 5 nút của T-014; 7 nút "More formats" nằm dưới màn, đo khi chưa cuộn thì ra 0 dp (mục 4.1). Test đo cả 12 nút, cuộn tới từng nút trước khi đo, đo cả cao và rộng (điểm nên sửa vòng 3 của rà soát T-036). |

Thiết kế tham chiếu: `features/ads/design/README.md` (bảng thành phần: CTA 36 dp ở đầy đủ, media đầy đủ 200 dp, ít nhất 120 x 120 dp, khung chờ đầy đủ 418 dp). Task không vẽ màn hình mới, không có ảnh thiết kế riêng (docs/ops/design-workflow.md).

ADR liên quan: ADR-0013 (quảng cáo qua `:core:ads`), ADR-0026 (nghiệp vụ PARTNER), ADR-0029 (phần ngoài mục 3.2, test cũ).

## 3. Giải pháp

### 3.1. Cách làm

Ba phần độc lập, cùng một nhánh. Thứ tự commit theo `docs/auto-run.md` bước 3, 4: test (kèm lối vào cho test của phần A), rồi code sửa của phần B.

**A. Lượt native toàn màn hình độc lập tới `skipTurn` (T-026; KB-5, KB-16).**

1. Trên JVM không có cách nào đặt một quảng cáo vào khe nạp sẵn `preloadedNativeFullScreen` (chỉ lệnh gọi lại của `AdLoader` ghi vào, `AdsController.kt:1715`), nên cổng `decideFullScreenTurn` luôn trả `NOT_READY` và nhánh `nativeTurns.busy ||` (chỉ tới được khi cổng trả `SHOW`) không có test nào chạm. Thêm vào `AdsController` một lối vào chỉ cho test, cạnh `openConsentGateForTest` (cùng mẫu của T-026):

   ```kotlin
   /** Tests only: puts [ad] in the preloaded native full-screen slot of [placement], as a load would (plan T-044). */
   @VisibleForTesting
   internal fun putPreloadedNativeFullScreenForTest(placement: AdPlacement, ad: LoadedNativeAd) {
       preloadedNativeFullScreen.put(placement, ad, clock.nowMillis())
   }
   ```

   Không đổi hành vi nào của production. Lối vào này vào commit test của bước 3 (test cần nó để biên dịch; L-025), vì nó chỉ là phần dựng của test, không phải phần sửa.

2. Ba test mới trong `AdsControllerNativeComboTest` (mục 4, dòng 1 tới 3), đều bật `is_show_gallery_native_full_screen` và mở cổng đồng ý bằng `openConsentGateForTest()`, gọi `controller.showFullScreenAd(activity, GALLERY_NATIVE_FULL_SCREEN, onFinished)` trong `CoroutineScope(Dispatchers.Main.immediate).launch` như `blockedAtGate_noAd_finishesOnce_noView`.
3. Một test mới trong `NativeFullScreenTurnsTest` cho vế `turn != null` của nhánh bận của combo (mục 4, dòng 4), với cổng giả sẵn có của file.
4. Không sửa code: chạy thử cho thấy các test này qua trên code hiện tại, và bỏ vế được kiểm thì chỉ test mới đỏ (mục 4.1). Test cũ `standalone_noAd_showsNothing_finishesAtOnce` giữ nguyên (kiểm riêng `beginStandaloneNativeTurn`).
5. Nhánh `else NOT_READY` của `AdsController.kt:1057` (cổng trả `SHOW` mà `takeAd` trả `null`) chỉ tới được khi quảng cáo hết hạn giữa `hasFresh` và `take` trong cùng một lời gọi, không dựng được trên JVM với đồng hồ đứng yên. Bước đối chứng của test `BUSY` đi qua đúng nhánh này (bỏ `nativeTurns.busy ||` thì lượt ghi `NOT_READY` từ nhánh `else`), nên hai nhánh của biểu thức đều có lần chạy.

**B. Thẻ native `FULL` giữ đủ các phần trong 418 dp (T-027; KB-2).**

1. Test mới `full_partsKeepSize_insideCard_textsAndFontScales` và `full_mediaFloor120dp_cardGrows_atFontScale3` trong `NativeAdCardTest` (mục 4, dòng 5, 6). Cỡ chữ hệ thống đặt bằng một hàm phụ dùng chung mới trong nguồn test của `:core:ads`, file `FontScaleTestSupport.kt`: `@Composable internal fun WithFontScale(fontScale: Float, content: @Composable () -> Unit)` = `CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale), content)`, KDoc ghi "đặt ngoài `AppTheme`" và lý do dùng `Density(density, fontScale)`. T-041 dùng lại hàm này cho bố cục toàn trang. Test bao `WithFontScale(scale) { AppTheme { NativeAdCard(...) } }`, đổi `scale` qua `mutableStateOf` trong cùng một lần `setContent`. `Density(density, fontScale)` của Compose dùng bảng đổi cỡ chữ phi tuyến của Android 14 (`FontScaleConverterFactory`), nên số đo giống máy thật hơn nhân thẳng (mục 4.1: dòng mô tả ở 1.3 cao 54 dp, không phải 52 dp).
2. Test cũ `full_placeholderHeight_equalsCardHeight_shortAndLongTexts` giữ nguyên, không xóa: nó vẫn kiểm khung chờ bằng thẻ (418 dp), điều test mới không kiểm. Test mới mới là test bắt được thẻ bị ép.
3. **Phần sửa code (test mới đỏ trên code hiện tại, mục 4.1).** `FullLayout` hôm nay xếp năm phần cao cố định trong một `Column` 418 dp: đầu (icon 40 dp, nhưng cột tiêu đề và nhãn "Ad" lớn theo cỡ chữ), media 200 dp, mô tả 40 dp, CTA 36 dp, hàng sao 16 dp. Khi đầu cao thêm, `Column` ép các phần cuối: hàng sao còn 6 dp ở 1.3 và 0 dp từ 1.5; CTA còn 32,7 dp ở 2.0; mô tả bị cắt còn 1 dòng có dấu "…" từ 1.3. Sửa trong `NativeAdCard.kt`, chỉ phần của cỡ `FULL` (`FullLayout` và khung thẻ của cỡ này):
   - Luật (trả lời câu hỏi 1): **media co trước, không dưới 120 dp**. `heightIn(min = 120.dp)` không giữ được sàn này khi media nằm dưới `weight(1f)` (`weight` cho ràng buộc cố định, `heightIn` bị kẹp vào đó), nên `FullLayout` đổi từ `Column` sang một `Layout` riêng (`androidx.compose.ui.layout.Layout`, lần đầu trong dự án):
     1. đo đầu, mô tả, CTA, hàng sao với chiều cao không giới hạn (rộng cố định);
     2. chiều cao media = `fullMediaHeight(inner, others, floor)` = `max(inner - others, floor)`, với `inner` = 418 dp trừ lề trên 13 dp và lề dưới 13 dp, `others` = tổng chiều cao bốn phần kia cộng bốn khoảng 12 dp, `floor` = hằng số mới `NativeAdDimens.MinMediaSize = 120.dp`; `fullMediaHeight` là hàm thuần `internal` trong `NativeAdCard.kt`, có unit test riêng (mục 4, dòng 7);
     3. đo media với chiều cao cố định đó, xếp năm phần từ trên xuống cách nhau 12 dp; chiều cao của `Layout` = `others + media`.
   - Khung thẻ của cỡ `FULL` (trong `NativeAdLayout`) đổi `height(heightOf(FULL))` thành `heightIn(min = NativeAdDimens.FullHeight)`: tới cỡ chữ 2.0 thẻ vẫn đúng 418 dp; chỉ khi media đã chạm sàn 120 dp thì thẻ cao thêm thay vì ép CTA và hàng sao. Các cỡ khác giữ `height(heightOf(size))`. Khung chờ `FULL` giữ 418 dp.
   - mô tả: `heightIn(min = NativeAdDimens.BodyHeight)` thay `height(NativeAdDimens.BodyHeight)`, để đủ 2 dòng ở cỡ chữ lớn; CTA (36 dp), hàng sao (16 dp), khoảng cách 12 dp giữ nguyên. Sửa KDoc của `FullLayout` và `NativeAdDimens.FullHeight`. `FullMediaHeight` vẫn dùng cho khung chờ (`NativeAdCard.kt:822`).
   - Kết quả chạy thử với phần sửa (mục 4.1): media 200 dp ở 1.0, 176 dp ở 1.3, 134 dp ở 2.0, thẻ 418 dp; ở 3.0 media 120 dp và thẻ 488 dp; CTA 36 dp và hàng sao 16 dp ở mọi cỡ; mô tả 2 dòng; 33 test của `NativeAdCardTest` qua; `:core:ads:verifyRoborazziDebug` qua (ảnh `NativeAdCard_full_light`, `_dark` không đổi).
   - **Rủi ro:** (1) media co lại ở cỡ chữ lớn, nhỏ nhất 134 dp ở 2.0 (cỡ lớn nhất Android cho đặt), vẫn đạt 120 x 120 dp của AdMob và KB-2; (2) ở cỡ chữ trên 2.0 (không đặt được trên máy thật) thẻ cao hơn khung chờ; (3) `Layout` riêng: media đi qua `binder.Media` (khung `MediaView` của SDK) như trước, chỉ đổi cách đo; (4) `NativeAdSize.FULL` chưa có vị trí nào vẽ (`ads-guide.md` mục 3), nên không đổi gì người dùng thấy hôm nay.
4. Ghi luật một lần ở dòng Media của bảng thành phần trong `docs/features/ads/design/README.md` (mục 3): "khi chữ lớn, media co trước, không dưới 120 dp" (đầy đủ 200 dp ở cỡ chữ 1.0); T-041 dùng lại luật này cho trang toàn trang. Dòng "Đầy đủ (`FULL`)" của bảng số đo ở `docs/ops/ads-guide.md` mục 4 dẫn về dòng đó, không chép lại luật.

**C. Vùng chạm của cả 12 nút màn xem thử (T-036; KB-8).**

1. Sửa `AdGalleryScreenTest.buttons_haveTouchTargetOf48dp`: giữ câu khẳng định tập 12 tag (`planT014Buttons + moreFormatButtons`) và `assertEquals(12, allTagged.size)`; thay phần đo: với mỗi tag của `allTagged` (lấy từ cây ngữ nghĩa, không từ danh sách viết tay), `onNodeWithTag(tag).performScrollTo()` rồi `bounds(tag)`, khẳng định cao và rộng đều ít nhất 48 dp (dung sai `TOLERANCE_DP`). Bỏ dòng lọc `in planT014Buttons`, `assertEquals(5, buttons.size)` và chú thích "The seven … lie below the screen": câu đếm 5 được thay bằng câu đo cả 12, chặt hơn (ADR-0029 quyết định 2 cấm nới; đây là làm chặt theo plan).
2. `moreFormats_buttonsHaveTouchTargetOf48dp` giữ nguyên (trùng một phần, không xóa test).
3. Không sửa code: cả 12 nút đạt (mục 4.1).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Thêm `putPreloadedNativeFullScreenForTest` (`@VisibleForTesting internal`), cạnh `openConsentGateForTest`; commit test của bước 3 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeComboTest.kt` | Ba test mới của phần A (mục 4, dòng 1 tới 3); sửa KDoc đầu lớp (thêm lượt độc lập qua `showFullScreenAd`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurnsTest.kt` | Test mới `comboWhileStandaloneOpen_presenceFree_skipsBusy` (vế `turn != null`) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `FullLayout` thành `Layout` riêng theo luật media co trước, sàn 120 dp; hàm thuần `fullMediaHeight`; hằng số `NativeAdDimens.MinMediaSize`; khung thẻ `FULL` `heightIn(min = FullHeight)`; mô tả `heightIn(min = BodyHeight)`; KDoc của `FullLayout`, `NativeAdDimens.FullHeight` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FontScaleTestSupport.kt` | Hàm phụ dùng chung `WithFontScale(fontScale) { ... }` cho test giao diện của `:core:ads` (T-041 dùng lại) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullMediaHeightTest.kt` | Unit test của `fullMediaHeight` (mục 4, dòng 7) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Test mới `full_partsKeepSize_insideCard_textsAndFontScales`, `full_mediaFloor120dp_cardGrows_atFontScale3` (dùng `WithFontScale`); test cũ giữ nguyên |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryScreenTest.kt` | `buttons_haveTouchTargetOf48dp` cuộn tới và đo cao, rộng của cả 12 nút |
| Sửa | `docs/ops/ads-guide.md` | Mục 4, bảng số đo, cột "Đầy đủ (`FULL`)": media 200 dp ở cỡ chữ 1.0, chữ lớn thì theo luật ở README thiết kế (dẫn về, không chép); mô tả ít nhất 2 dòng |
| Sửa | `docs/features/ads/design/README.md` | Mục 3, bảng thành phần, dòng Media: luật "khi chữ lớn, media co trước, không dưới 120 dp" (đầy đủ 200 dp ở cỡ chữ 1.0) |

Ảnh chuẩn: không ghi lại ảnh nào. Chạy thử với phần sửa của phần B, `:core:ads:verifyRoborazziDebug` qua; không ảnh nào của `:app` vẽ `NativeAdSize.FULL` (`grep -rn "NativeAdSize.FULL"` chỉ ra `NativeAdCard.kt` và `NativeRequestOptions.kt`).

Nơi đọc file bị đổi (L-020): `NativeAdCard.kt` được đọc bởi mọi khe native, nhưng phần sửa chỉ ở `FullLayout` (riêng nhánh `NativeAdSize.FULL` của `NativeAdLayout`); `NativeAdDimens.FullMediaHeight` vẫn do khung chờ dùng. `AdsController.kt` chỉ thêm một hàm `internal` mới, không ai ngoài test gọi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi bố cục của thẻ native `FULL` khi cỡ chữ hệ thống lớn (điểm chặn 1, định dạng native); thẻ này chưa có vị trí nào vẽ. Lượt độc lập và màn xem thử: chỉ test.
- Sự kiện phân tích: Không chạm (test đọc log, không thêm sự kiện).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Lớp mới dùng trong test: `androidx.compose.ui.unit.Density`, `androidx.compose.ui.platform.LocalDensity`, `androidx.compose.runtime.CompositionLocalProvider` (Compose UI và runtime, đã có trên classpath biên dịch test của `:core:ads`). Lớp mới trong code: `androidx.compose.ui.layout.Layout`, `androidx.compose.ui.unit.Constraints` (Compose UI), `androidx.compose.foundation.layout.heightIn` (Compose foundation), đều đã có trên classpath biên dịch `main` của `:core:ads`; chạy thử ở mục 4.1 biên dịch qua.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `Layout` riêng của `FullLayout` (lần đầu trong dự án) làm lệch ảnh chuẩn trên CI (máy CI vẽ khác máy dev), hay đổi cách `binder.Media` gắn `MediaView` trên máy thật.
- **Điều kiện chuyển:** `verifyRoborazziDebug` trên CI báo lệch ảnh `NativeAdCard_full_*` sau phần sửa; hoặc một test sẵn có của `NativeAdCardTest`, `SdkNativeAdBinder` đỏ vì cách đo mới sau 2 lần sửa.
- **Cách làm thay:** không có phương án thay được kiểm trước: `Layout` riêng đã qua `scripts/check.sh` khi chạy thử (mục 4.1). Lệch ảnh chuẩn thì dừng theo ADR-0029 quyết định 2 (plan không cho ghi ảnh); test sẵn có đỏ vì cách đo sau 2 lần sửa thì dừng theo điều kiện dừng 4 và báo, không tự đổi cách giữ sàn 120 dp.

## 4. Kiểm thử

Mọi test viết trước code. Test của phần A và C qua ngay trên code hiện tại (chỉ là test thiếu, không có lỗi), nên mỗi test có bước đối chứng làm nó đỏ đúng lý do rồi hoàn lại (L-055); test của phần B đỏ trên code hiện tại vì lỗi thật (số đo ở mục 4.1).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-5, tiêu chí 4: không có quảng cáo thì không hiện gì, không chặn | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_noAd_throughShow_skipsNotReady_finishesOnce`: bật `placementEnabled(GALLERY_NATIVE_FULL_SCREEN)`, `openConsentGateForTest()`, `idle()`; gọi `showFullScreenAd(activity, GALLERY_NATIVE_FULL_SCREEN) { finishes++ }`, `idle()`. Khẳng định: kết quả `false`; `finishes == 1`; log có đúng dòng `Full-screen gallery_native_full_screen skipped: NOT_READY`; `fullScreenAdShowing` `false`; `nativeFullScreenView` `null`; `idleFor(5_000)` rồi `finishes` vẫn 1. **Đối chứng:** (1) dữ liệu đúng cạnh dữ liệu sai: dòng 2 dưới đây, cùng cách dựng nhưng có quảng cáo, thì lượt mở và `finishes == 0`; (2) tạm thay `return@withContext skipTurn(placement, decision, loadState, onFinished)` (nhánh cổng, `AdsController.kt:1050`) bằng `return@withContext false`: test đỏ ở `finishes == 1`; hoàn lại. |
| KB-5: có quảng cáo thì lượt mở (đối chứng của dòng trên) | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_withAd_throughShow_opensTurn`: như dòng trên, thêm `putPreloadedNativeFullScreenForTest(GALLERY_NATIVE_FULL_SCREEN, ad)` trước khi gọi. Khẳng định: kết quả `true`; `finishes == 0`; `nativeFullScreenView.value!!.ad === ad`; không có dòng log `skipped`; `closeNativeFullScreen()` thì `finishes == 1`. **Đối chứng:** bỏ dòng `putPreloaded…` thì test đỏ ở kết quả `true` (chính là dòng trên). |
| KB-16, F-02: lượt còn mở quá trần thì lượt sau bị bỏ là `BUSY`, không lấy quảng cáo | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_turnOpenPastCap_throughShow_skipsBusy_keepsAdInSlot`: dựng như trên; lượt 1 với quảng cáo `first` qua `showFullScreenAd` (kết quả `true`); `idleFor(FullScreenAdPresence.SHOWING_CAP_MILLIS + 1)`; đặt quảng cáo `second` vào khe; gọi lần 2 `{ secondFinishes++ }`. Khẳng định: kết quả `false`; `secondFinishes == 1`; log có `Full-screen gallery_native_full_screen skipped: BUSY` và không có `skipped: NOT_READY`; `isFullScreenAdReady(GALLERY_NATIVE_FULL_SCREEN)` vẫn `true` và `second.destroyCount == 0` (không lấy ra khỏi khe); `nativeFullScreenView.value!!.ad === first`; `finishes == 0`; `closeNativeFullScreen()` thì `finishes == 1`. **Đối chứng (đã chạy thử, mục 4.1):** tạm bỏ `nativeTurns.busy ||` ở `AdsController.kt:1054`: lượt 2 đi nhánh `else` và log `skipped: NOT_READY`, test đỏ ở câu `BUSY`; hoàn lại. |
| KB-16, F-02: lượt combo gặp một lượt native khác đang mở (cờ "đang hiện" trống) thì bỏ ngay, không mở combo | Unit test (`runTest`, cổng giả) | `NativeFullScreenTurnsTest.comboWhileStandaloneOpen_presenceFree_skipsBusy`: cổng giả có quảng cáo trong khe combo, không ai giữ `presence`; `turns.showStandalone(GALLERY_NATIVE_FULL_SCREEN, standalone) { standaloneFinishes++ }`; rồi `turns.afterInter(port)`. Khẳng định ngay: `finishes == 1`; log có `Native combo skipped: busy`; sau `runCurrent()`: `turns.view.value!!.ad === standalone`; `port.presence.showing.value` vẫn `false` (combo không lấy cờ); `port.calls` không có `beginPresence`; `standaloneFinishes == 0`. **Đối chứng (đã chạy thử, mục 4.1):** tạm đổi `if (turn != null || !port.beginPresence())` thành `if (!port.beginPresence())` ở `NativeFullScreenTurns.kt:106`: test mới đỏ (`finishes` 0 thay 1), 32 test cũ của file vẫn qua; hoàn lại. |
| KB-2: thẻ `FULL` giữ đủ CTA, hàng sao, mô tả, media trong 418 dp với chữ dài và cỡ chữ lớn | Compose UI test (Robolectric `NATIVE`, khung `w411dp-h891dp-xxhdpi`, thẻ rộng `CARD_WIDTH` 379 dp như các test `full_*`) | `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales` (bao bằng `WithFontScale`): với mỗi cỡ chữ 1.0, 1.3, 2.0 và mỗi bộ chữ `sampleNativeAdAssets`, `longFullNativeAdAssets` (cả hai `starRating = 4.0`), đọc `getUnclippedBoundsInRoot()` của `CARD`, `CTA`, `STAR_ROW`, `MEDIA`, `BADGE`. Khẳng định (dung sai 0,5 dp, thông báo ghi cỡ chữ và bộ chữ): thẻ cao 418 dp; CTA cao 36 dp; hàng sao cao 16 dp; đáy hàng sao không dưới đáy thẻ trừ 13 dp (12 dp lề trong cộng 1 dp viền); đỉnh hàng sao cách đáy CTA 12 dp; media cao ít nhất 120 dp và đỉnh media không trên đáy nhãn "Ad"; mô tả có đúng 2 dòng (`TextLayoutResult.lineCount == 2`, đọc qua `GetTextLayoutResult` như hàm `textStyle` của file). Ở cỡ chữ 1.0 thêm: media cao đúng 200 dp (giữ số của thiết kế). **Lý do thất bại đã thấy trên code hiện tại (mục 4.1):** ở 1.3 hàng sao cao 6 dp (399..405), mô tả 1 dòng; ở 2.0 hàng sao 0 dp, CTA 32,7 dp. Ở 1.0 mọi câu qua, nên câu đỏ đúng là câu của cỡ chữ lớn. **Đối chứng:** vòng lặp 1.0 qua trên code hiện tại (dữ liệu đúng cạnh dữ liệu sai: cùng test, chỉ cỡ chữ đổi); sau phần sửa, tạm đo media bằng `Constraints.fixed(width, FullMediaHeight)` thay `fullMediaHeight(...)` thì test đỏ ở 1.3 (thẻ cao hơn 418 dp); hoàn lại. |
| KB-2: sàn 120 dp của media giữ bằng code, thẻ cao thêm khi chạm sàn | Compose UI test (như dòng trên) | `NativeAdCardTest.full_mediaFloor120dp_cardGrows_atFontScale3`: `WithFontScale(3f)`, chữ dài. Khẳng định: media cao đúng 120 dp; CTA 36 dp; hàng sao 16 dp, đáy cách đáy thẻ 13 dp; thẻ cao hơn 418 dp (chạy thử: 488 dp). **Lý do thất bại trên code hiện tại:** thẻ ép 418 dp, hàng sao 0 dp. **Đối chứng:** sau phần sửa, tạm bỏ `max(..., floor)` (media = `inner - others`): media dưới 120 dp, test đỏ; hoàn lại. |
| Luật sàn của media | Unit test (JVM thuần) | `FullMediaHeightTest`: `fullMediaHeight(inner = 392, others = 192, floor = 120) == 200` (cỡ 1.0); `(392, 258, 120) == 134`; `(392, 300, 120) == 120` (chạm sàn); `(392, 392, 120) == 120`. **Đối chứng:** ca 1 trả 200, khác sàn, nên hàm trả `floor` cố định không qua được. |
| KB-2: khung chờ vẫn bằng thẻ | Compose UI test | `NativeAdCardTest.full_placeholderHeight_equalsCardHeight_shortAndLongTexts` (đã có, giữ nguyên) và `placeholder_matchesCardHeight_forEachSize` (đã có) |
| Giao diện thẻ `FULL` ở cỡ chữ 1.0 không đổi | Screenshot test (đã có) | `NativeAdCard_full_light.png`, `NativeAdCard_full_dark.png` không ghi lại; `verifyRoborazziDebug` của `scripts/check.sh` phải qua (chạy thử ở mục 4.1 đã qua) |
| KB-8: mọi nút của màn xem thử có vùng chạm ít nhất 48 dp | Compose UI test (Robolectric `NATIVE`, `w411dp-h891dp-xxhdpi`) | `AdGalleryScreenTest.buttons_haveTouchTargetOf48dp` (sửa): tập tag bằng `planT014Buttons + moreFormatButtons`, đếm 12; với mỗi tag trong `allTagged`: `performScrollTo()`, `bounds(tag)`, cao và rộng ít nhất 48 dp. **Số đo trên code hiện tại (mục 4.1):** cả 12 đạt (cao 48 hoặc 52 dp, rộng 183,3 tới 379 dp); đo khi chưa cuộn thì 4 nút "More formats" ra 0 x 0 dp, nên phải cuộn. **Đối chứng:** tạm thêm `.height(40.dp)` vào modifier của riêng nút `STATUS_BUTTON` (trong `PageButtonRow` của `AdGalleryScreen.kt`): test mới đỏ với `ad_gallery_status_button is 40.0.dp high`, bản cũ của test (chỉ đo 5 nút) vẫn qua; hoàn lại. |
| Đúng thiết kế | Không có ảnh thiết kế riêng cho task (chỉ test và bố cục `FULL` ở cỡ chữ lớn, thiết kế không vẽ). Ảnh thiết kế của ticket khác (T-014, T-027, T-030, T-036): `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_default_light`, `AdGalleryScreen_inlist_light`, `NativeFullScreenAdScreen_default_dark`, `NativeFullScreenAdScreen_default_light`, `SampleScreen_ad-loading_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light` | Không ghi lại ảnh nào |
| Thẻ `FULL` ở cỡ chữ lớn trên máy thật | Kiểm tra thủ công (máy thật: khác, `NativeAdSize.FULL` chưa có vị trí nào vẽ nên chưa có gì để xem trên máy) | Không làm trong task này; khi có vị trí dùng `FULL` (sửa MC-02) thì kiểm cỡ chữ lớn nhất trên máy thật |

**Ngoại lệ viết test trước:** sửa `docs/ops/ads-guide.md` và `docs/features/ads/design/README.md` (chỉ tài liệu). `FontScaleTestSupport.kt` là phần dựng của test, vào commit test. Hàm `fullMediaHeight` và hằng số `MinMediaSize` vào commit test dưới dạng khung rỗng (`= floor`) để test biên dịch được và đỏ đúng lý do (L-025). Lối vào `putPreloadedNativeFullScreenForTest` vào cùng commit test (phần dựng của test, không phải phần sửa).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, trên `claude/plan-T-044` từ nhánh chính `6077815`, bằng test tạm không commit (đã xóa, worktree sạch):
  - `TempT044ComboTest`: ba test của phần A với lối vào `putPreloadedNativeFullScreenForTest` thêm tạm.
  - `TempT044FullTest`, `TempT044BodyTest`: đo thẻ `FULL` ở cỡ chữ 1.0, 1.15, 1.3, 1.5, 1.8, 2.0 với hai bộ chữ, trước và sau phần sửa của phần B.
  - Một hàm tạm trong `AdGalleryScreenTest`: đo 12 nút trước và sau khi cuộn.
  - Bộ test JVM đầy đủ với lối vào và phần sửa của phần B áp tạm.
  - Phiên bản 1 sau rà soát (cùng ngày): `Layout` riêng của `FullLayout` với sàn 120 dp và khung thẻ `heightIn(min = 418 dp)`; đo lại ở 1.0, 1.3, 2.0, 3.0; test tạm `TempT044Combo2Test` (lượt `BUSY`) và một test tạm trong `NativeFullScreenTurnsTest` (vế `turn != null`), mỗi test với bước đối chứng của nó.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TempT044*'`; `./gradlew :app:testStagingDebugUnitTest --tests '*AdGalleryScreenTest.tempT044*'`; `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue`; `./gradlew :core:ads:verifyRoborazziDebug`. `scripts/check.sh` (E = 2) với lối vào và `Layout` riêng áp tạm, không có test mới.
- **Kết quả:**
  - Phần A: 3 test qua trên code hiện tại. Log của lượt `BUSY`: `Resume ads held: full_screen_page`, `Native full-screen standalone opened`, `Full-screen gallery_native_full_screen skipped: BUSY`, `Full-screen turn finished: busy`; quảng cáo thứ hai vẫn trong khe (`isFullScreenAdReady` `true`). Lượt không quảng cáo: `skipped: NOT_READY`, `Full-screen turn finished: not_ready`, `finishes == 1`. Bước đối chứng đã chạy: bỏ `nativeTurns.busy ||` thì log `Full-screen gallery_native_full_screen skipped: NOT_READY`, `Full-screen turn finished: not_ready`, test đỏ ở câu `BUSY`. Vế `turn != null` của combo: test tạm qua trên code hiện tại; đổi thành `if (!port.beginPresence())` thì test tạm đỏ (`expected:<1> but was:<0>`), 32 test cũ của `NativeFullScreenTurnsTest` vẫn qua (vế này chưa có test nào). Hoàn lại cả hai.
  - Phần B, code hiện tại (dp, từ đỉnh thẻ; thẻ 0..418 ở mọi cỡ, hai bộ chữ cho cùng số):

    | Cỡ chữ | Nhãn "Ad" | Media | Mô tả | CTA | Hàng sao |
    | --- | --- | --- | --- | --- | --- |
    | 1.0 | 48..64 | 77..277 | 2 dòng | 341..377 | 389..405 |
    | 1.3 | 54..75 | 87..287 | 1 dòng, "…" | 351..387 | 399..405 (6 dp) |
    | 1.5 | 58,7..82,7 | 94,7..294,7 | | 358,7..394,7 | 0 dp |
    | 2.0 | 64,3..96,3 | 108,3..308,3 | 1 dòng, "…" | 372,3..405 (32,7 dp) | 0 dp |

    Đáy CTA và hàng sao luôn nằm trong thẻ vì `Column` ép các phần cuối; nên test chỉ kiểm "đáy nằm trong thẻ" vẫn qua, test phải kiểm cả cỡ của từng phần.
  - Phần B, sau phần sửa: CTA 341..377, hàng sao 389..405 ở 1.0, 1.3, 2.0, thẻ 418 dp; media 200 dp (1.0), 176 dp (1.3), 134 dp (2.0); mô tả 2 dòng (ở 2.0 mô tả ngắn cũng có "…" ở dòng 2). Ở 3.0: media 147..267 (120 dp, chạm sàn), CTA 411..447, hàng sao 459..475, thẻ 488 dp. `NativeAdCardTest` 33 test qua. Bản đầu dùng `weight(1f)` cho cùng số ở 1.0 tới 2.0 nhưng không giữ được sàn, nên bị thay.
  - Phần C: đã ghi ở mục 4, dòng KB-8.
  - Bộ test JVM đầy đủ với lối vào và phần sửa: 1364 test, 0 lỗi (không test cũ nào đỏ, không có gì ngoài mục 3.2). `:core:ads:verifyRoborazziDebug`: qua.
  - `scripts/check.sh` với `Layout` riêng: lần 1 đỏ ở `ktlintMainSourceSetCheck` (lời gọi `NativeAdBody` dài 123 ký tự sau khi lùi vào trong `Layout`; phiên viết mỗi đối số một dòng); lần 2 sau khi xuống dòng: 1364 test JVM, 0 lỗi, 82 ảnh screenshot, Cổng chất lượng: QUA.
  - Ghi chú: ở 2.0, chữ của CTA (`labelLarge`) cao hơn khung 36 dp (`didOverflowHeight`), cả trước và sau phần sửa; câu hỏi 2.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi bố cục của thẻ native `FULL` khi cỡ chữ lớn; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm vị trí, quyền, SDK, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module (`:core:ads` code và test, `:app` chỉ test staging); code: một hàm cho test, `FullLayout` đổi sang `Layout` riêng, một hàm thuần, khung thẻ `FULL` |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu rõ ba test (một chỗ ghi nhầm "combo", mục 2); luật media co trước, sàn 120 dp đã được trả lời (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi phần có test JVM; bố cục `FULL` trên máy thật chỉ xem được khi có vị trí |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `NativeAdCard.kt`, `AdGalleryScreenTest.kt` có thể bị T-041, T-042, T-043 cùng sửa (mục 6) |
| E. Độ mới của kỹ thuật | 2 | `Layout` riêng (đo rồi xếp tay) lần đầu trong code của dự án; đặt cỡ chữ bằng `LocalDensity` lần đầu trong test; đã chạy thử bằng `scripts/check.sh` (mục 4.1) |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thêm một hàm cho test), `NativeAdCard.kt` (`FullLayout`, khung thẻ của riêng cỡ `FULL`, hàm `fullMediaHeight`, hằng số `MinMediaSize`), `FontScaleTestSupport.kt` mới, `docs/ops/ads-guide.md` mục 4, `docs/features/ads/design/README.md` (dòng Media). Ảnh chuẩn, `tokens.json`, bảng khóa Remote Config không bị ảnh hưởng.
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- **T-041 (native toàn trang co media, Flow steps; chạy sau T-044):** có thể sửa `NativeAdCard.kt` (khung chờ, `NativeAdDimens`) hay `NativeFlowStep.kt`, và gần như chắc sửa `AdGalleryScreenTest.kt` (trang Flow steps, ảnh `AdGalleryScreen_flow-full-page_*`). T-044 chỉ sửa `FullLayout` và một test `buttons_haveTouchTargetOf48dp`, khác chỗ. Cùng luật "media co trước, không dưới 120 dp": T-044 tạo luật ở README thiết kế, hằng số `NativeAdDimens.MinMediaSize`, hàm `fullMediaHeight` và hàm phụ test `WithFontScale`; T-041 ghi "dùng lại" (Sửa hay dùng, không Tạo) cho các thứ này (L-054). T-041 cũng dùng `putPreloadedNativeFullScreenForTest` của T-044 (nạp sẵn trang toàn trang trên JVM): T-044 ghi Tạo, T-041 ghi dùng lại, nên T-041 phụ thuộc T-044 về thứ tự.
- **T-042 (rewarded bấm đúp), T-043 (resume): chạy trước T-044.** Có thể sửa `AdsController.kt` (đường rewarded, resume). T-044 chỉ thêm một hàm cạnh `openConsentGateForTest`, khác chỗ; nếu T-042 hay T-043 cũng thêm lối vào cho test thì đặt cạnh nhau, không đổi tên của nhau.
- Đề xuất thứ tự chạy: theo thứ tự đã định T-043, T-042, T-044, T-041; T-044 gộp nhánh chính trước khi viết test. T-044 dừng thì T-041 cần tự tạo lối vào và hàm phụ (ghi trong plan T-041).

## 7. Điều kiện dừng riêng

- Phần sửa của `FullLayout` làm đổi một ảnh chuẩn screenshot nào (kể cả `NativeAdCard_full_*`): dừng, không ghi lại ảnh (plan không cho ghi ảnh nào).
- Phần sửa cần đổi bố cục của cỡ khác `FULL` (small, medium, thẻ, bước) hay đổi `NativeAdCallToAction` dùng chung: ngoài phạm vi đã chọn ở câu hỏi 1 và 2, dừng và hỏi.
- Test của phần A cần thêm một lối vào cho test khác `putPreloadedNativeFullScreenForTest` hay đổi hành vi của `showNativeFullScreen`: dừng và báo (task chỉ thêm test cho phần này).

## 8. Câu hỏi cho người duyệt

1. **Thẻ native `FULL` ở cỡ chữ hệ thống lớn: sửa thế nào?** Code hiện tại ép hàng sao và CTA khi chữ lớn (mục 4.1). `FULL` chưa có vị trí nào vẽ, nhưng test mới của ticket đỏ vì lỗi này.
   - Đề xuất của AI: media lấp phần còn lại, mô tả cao ít nhất 40 dp; thẻ vẫn 418 dp bằng khung chờ, ở cỡ chữ 1.0 không đổi gì (ảnh chuẩn giữ nguyên), media nhỏ nhất 134 dp ở cỡ 2.0. Phương án khác: (b) thẻ cao theo nội dung (`heightIn(min = 418.dp)`, như AdsKit ghi "cao theo nội dung"), giữ media 200 dp nhưng khung chờ không còn bằng thẻ khi chữ lớn, nội dung nhảy khi quảng cáo về; (c) khóa cỡ chữ trong thẻ ở 1.0 (không theo cài đặt trợ năng của người dùng); (d) không sửa, đổi test chỉ kiểm cỡ chữ 1.0 và ghi lỗi cho task có vị trí `FULL`.
   - **Trả lời (anh Nhật, 2026-10-26):** media co lại, theo đề xuất. Rà soát độc lập thêm: sàn 120 dp phải do code giữ, không chỉ test (mục 3.1, phần B: `Layout` riêng, `fullMediaHeight`); luật ghi một lần ở README thiết kế, T-041 dùng lại.
2. **Chữ của CTA ở cỡ 2.0 cao hơn khung 36 dp** (bị cắt mép), ở mọi cỡ native có CTA cố định, không riêng `FULL`.
   - Đề xuất của AI: không sửa trong task này (ngoài phạm vi ticket, chạm `NativeAdCallToAction` dùng chung cho mọi cỡ); ghi một ticket sau cho mọi cỡ native ở cỡ chữ lớn (small 80 dp, medium, thẻ, bước), cùng kiểu test của task này.
   - **Trả lời (anh Nhật, 2026-10-26):** để một ticket theo sau, không làm trong task này.
3. **Lối vào cho test `putPreloadedNativeFullScreenForTest` trong code production.** Cần để nhánh `nativeTurns.busy ||` có test qua `showFullScreenAd` (mục 3.1, phần A, bước 1).
   - Đề xuất của AI: thêm, cùng mẫu `openConsentGateForTest` của T-026 (`@VisibleForTesting internal`). Phương án khác: tách phần chọn `BUSY` hay `NOT_READY` thành một hàm thuần `standaloneSkipDecision(turnOpen, presenceBusy)`, test hàm đó, không cần lối vào; nhưng khi đó không test nào đi qua `showFullScreenAd` tới `skipTurn(BUSY)` như rà soát yêu cầu.
   - **Trả lời (anh Nhật, 2026-10-26):** đồng ý thêm lối vào cho test.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
