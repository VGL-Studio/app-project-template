---
ticket: T-060
title: "Dọn native: hàng sao, chữ lớn ở native nhỏ, chú thích cũ"
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5.4M         # cỡ S, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29), không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:
---

# PLAN T-060: DỌN NATIVE: HÀNG SAO, CHỮ LỚN Ở NATIVE NHỎ, CHÚ THÍCH CŨ

## 1. Tóm tắt

Ba việc nhỏ trong `:core:ads`. (1) Hàng sao của native medium và native đầy đủ được đăng ký với SDK (`NativeAdView.starRatingView`), như tiêu đề, icon, CTA. (2) Đo trên code hiện tại (mục 4): ở cỡ chữ 2.0 với chữ ngắn, native nhỏ 80 dp và thẻ dọc không cắt mất chữ (chỉ rút gọn bằng "…"); nhưng khi CTA dài, CTA của native nhỏ chiếm hết hàng, tiêu đề và nhãn "Ad" còn rộng 0 dp, ở khung 360 dp ngay từ cỡ chữ 1.0, ở khung 411 dp từ 1.3. Plan giới hạn CTA của native nhỏ rộng tối đa 120 dp, không đổi chiều cao 80 dp (D-02); chạy thử bằng bản sao bố cục: tiêu đề, "Ad", CTA đều còn chữ ở mọi khung và cỡ chữ, ảnh chuẩn có sẵn không đổi vì CTA "Install" chỉ rộng 80 tới 103 dp. (3) Sửa KDoc của `NativeAdSlot` còn nhắc khóa `ads_native_load_timeout_seconds` đã bỏ. Kiểm bằng Compose UI test Robolectric đo chữ và khung, test đăng ký view với SDK, và hai ảnh chuẩn mới.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Thêm 2026-10-29: khi máy đặt cỡ chữ 200% thì tiêu đề, nhãn "Ad" và CTA của native nhỏ không bị cắt mất chữ (có thể rút gọn bằng dấu ba chấm), khe vẫn cao 80 dp (D-02); đổi 2026-10-29: chiều cao khe theo KB-32 | Ticket làm trên kích thước hôm nay (80 dp). "Không cắt mất chữ" hiểu là: chữ không bị cắt theo chiều cao, nằm trong khung; theo chiều ngang được rút gọn bằng "…" nhưng còn thấy chữ: nhãn "Ad" đủ 2 ký tự, tiêu đề còn ít nhất 4 ký tự trước "…" ở khung 360 dp và 411 dp (câu hỏi 4). Áp cho native nhỏ (`NativeAdSize.SMALL`: khe đáy, native nhỏ trong danh sách) và thẻ dọc (`CARD`) như ticket ghi. Chiều cao theo bề rộng (KB-32, 90 tới 200 dp) là việc của T-065, ticket đó kiểm lại tiêu chí này |
| KB-1 | Khi có quảng cáo thì thấy icon, tiêu đề, nút CTA, nhãn "Ad" và biểu tượng AdChoices | Hôm nay CTA dài làm tiêu đề và nhãn "Ad" biến mất ở cả cỡ chữ 1.0 (mục 4). Phần sửa ở trên cũng giữ tiêu chí này và luật O-08 ("Ad" luôn thấy được) |
| KB-2 | Thêm 2026-10-29: khi quảng cáo có điểm đánh giá thì chạm vào hàng sao được tính là chạm vào quảng cáo, như các phần khác của quảng cáo | SDK chỉ tính lượt chạm trên view đã đăng ký với `NativeAdView`. Hàng sao (`NativeAdStarRow`) vẽ ở medium (đầu quảng cáo) và native đầy đủ (dưới CTA) nhưng chưa đăng ký. Plan đăng ký nó là `starRatingView` khi có sao để vẽ (điểm lớn hơn 0, O-08), bỏ đăng ký khi không có sao (câu hỏi 3). Câu trả lời 48 giữ hàng sao của medium. Trên JVM chỉ kiểm được view đã đăng ký; lượt chạm thật kiểm trên máy thật |

Thiết kế tham chiếu: `features/ads/design/README.md`, mục 3 (Native/Small: khe 80 dp, icon 36 dp, CTA 34 dp chữ 13 sp, nhãn "Ad" 10 sp; Native/Medium: hàng sao). Thiết kế không ghi chiều rộng tối đa của CTA; giới hạn 120 dp là số AI chọn (câu hỏi 1). T-064 sẽ vẽ lại native nhỏ theo JBase.

ADR liên quan: ADR-0026 (hành vi quảng cáo theo `ads-business-rules.md`), ADR-0038 (JBase làm chuẩn; kích thước JBase của native nhỏ thuộc T-064, T-065), ADR-0020 (ảnh chuẩn, không viết cứng để khớp thiết kế), ADR-0029, ADR-0031 (phần ngoài mục 3.2 và câu khẳng định cũ).

Mục Khi viết plan của `docs/CLAUDE.md` áp dụng cho plan này: mục 3.2 đủ; ảnh chuẩn và người đọc ảnh (L-020, mục 3.2); mục 3.5 (sửa Kotlin ngoài test); test viết trước đủ cụ thể; L-029 (đo trên code hiện tại bằng test tạm, khung 411 dp và 360 dp, mục 4); quyền và thư viện ở mục 3.3; screenshot test qua quy tắc `check.sh` (`captureRoboImage("src/test/screenshots/<Tên>.png")`, ảnh mới có trong 3.2, ghi ảnh có `--rerun` rồi `git status`, L-050, L-114); rủi ro không hạ điểm; `token_estimate`; câu hỏi ở mục 8; kiểm thủ công ghi nơi chạy (L-085); bước đối chứng cho test "không có thì null" (L-055). Không áp dụng: E từ 2 (mục 4.1), thư viện hay module mới, R2, Maven, trạng thái ở vỏ app, hàm `suspend`, dịch chữ, chép code từ dự án mẫu.

## 3. Giải pháp

### 3.1. Cách làm

1. **Đăng ký hàng sao** (`NativeAdViewBinding.kt`, `NativeAdCard.kt`):
   - `NativeAdAsset` thêm `STAR_RATING`. `SdkNativeAdBinder.registered` trả `adView.starRatingView`, `register` đặt `adView.starRatingView = view`, như các phần khác (cùng `requestBind()`, cùng `onRelease`).
   - `NativeAdStarRow(rating, binder, modifier)` nhận thêm `binder`. Ngoài vẫn là `Box(modifier)` cao 16 dp, giữ chỗ khi không có sao (medium giữ 56 dp đầu, T-027). Khi `nativeStarCount(rating) > 0`, hàng sao (Row có tag `STAR_ROW` và mô tả trợ năng như hôm nay) nằm trong `binder.Asset(NativeAdAsset.STAR_RATING, Modifier.height(NativeAdDimens.StarRowHeight)) { ... }`; không có sao thì không gọi `Asset`, nên view được bỏ đăng ký qua `onRelease` và lần gắn sau không có `starRatingView`.
   - `NativeAdHeader` (medium, `starRow = true`) và `FullLayout` truyền `binder` cho `NativeAdStarRow`. Với `PreviewNativeAdBinder` (thẻ giả, ảnh chuẩn) `Asset` chỉ là `Box(propagateMinConstraints = true)`, nên hình vẽ không đổi.
2. **CTA của native nhỏ không chiếm hết hàng** (`NativeAdCard.kt`, `SmallLayout`): `NativeAdDimens` thêm `SmallCallToActionMaxWidth = 120.dp` (KDoc: giữ chỗ cho tiêu đề và nhãn "Ad" khi CTA dài; KB-1, O-08; plan T-060). `SmallLayout` gọi `NativeAdCallToAction(..., modifier = Modifier.widthIn(max = NativeAdDimens.SmallCallToActionMaxWidth), ...)`. Chữ CTA vẫn một dòng, dài thì "…" (đã có). Chiều cao khe giữ `BottomSlotHeight` 80 dp; CTA vẫn cao ít nhất 34 dp và cao vừa chữ khi chữ lớn (T-045). Không đổi thẻ dọc (`CardLayout`): đo không thấy cắt chữ (mục 4). Không đổi `NativeAdCallToAction` dùng chung (medium, thẻ ở bước, lớp thu gọn, đầy đủ rộng hết khung).
   - Vì sao sửa ngay thay vì để T-065: lỗi có ở cỡ chữ 1.0 (khung 360 dp, CTA dài), làm mất nhãn "Ad" (O-08, chính sách AdMob), phần sửa một dòng, không đổi ảnh chuẩn có sẵn. T-065 thay bố cục native nhỏ theo JBase thì giữ hay bỏ giới hạn này theo layout mới, và chạy lại test của mục 4 (ticket T-065 đã ghi "Cỡ chữ 200%: kiểm lại theo tiêu chí của KB-1").
3. **KDoc `NativeAdSlot`** (`NativeAdSlot.kt`, dòng 21 tới 23): thay "with no ad after `ads_native_load_timeout_seconds`, the space is given back, or kept empty when [keepSpaceWhenEmpty] ..." bằng "when no ad could be loaded, the space shrinks to 0 dp (D-02), or is kept empty when [keepSpaceWhenEmpty] ...". Không đổi code.
4. **Tài liệu** (`docs/ops/ads-guide.md`, bảng native ở mục 4): dòng CTA của small thêm "rộng tối đa 120 dp, chữ dài thì rút gọn bằng "…" (T-060)"; dòng Hàng sao thêm "đăng ký với SDK (`starRatingView`), nên chạm vào hàng sao là chạm vào quảng cáo (KB-2)".

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | `NativeAdAsset.STAR_RATING`; `registered`, `register` với `starRatingView` (mục 3.1, bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | `NativeAdStarRow` nhận `binder`, bọc hàng sao trong `Asset(STAR_RATING)` khi có sao; `NativeAdHeader`, `FullLayout` truyền `binder`; `NativeAdDimens.SmallCallToActionMaxWidth = 120.dp`; `SmallLayout` đặt `widthIn(max = ...)` cho CTA; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | KDoc dòng 21 tới 23 (mục 3.1, bước 3) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Test đăng ký hàng sao; test CTA của small tối đa 120 dp qua binder của SDK (mục 4); `setHostedLayout` nhận `assets` (mặc định `sampleNativeAdAssets`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | Hai test chữ của native nhỏ và thẻ dọc ở khung 411 dp và 360 dp (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Test CTA của small tối đa 120 dp (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Hai screenshot test mới `fontScale2_small`, `longTexts_light` |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_font-scale-2_small.png` | Ảnh chuẩn mới: khung `w360dp-h640dp-xxhdpi`, cỡ chữ 2.0, native nhỏ chữ ngắn và chữ dài, hai thẻ dọc 156 x 300 dp chữ ngắn và chữ dài |
| Tạo | `core/ads/src/test/screenshots/NativeAdCard_long-texts_light.png` | Ảnh chuẩn mới: khung `w411dp-h891dp-xxhdpi`, cỡ chữ 1.0, native nhỏ và thẻ dọc 180 x 300 dp với `longFullNativeAdAssets` |
| Sửa | `docs/ops/ads-guide.md` | Bảng native mục 4: CTA small tối đa 120 dp; hàng sao đăng ký với SDK (mục 3.1, bước 4) |
| Sửa | `docs/features/ads/tickets/T-060-don-native-sao-va-chu-lon.md` | `plan:` trỏ tới plan này (PR của plan); trạng thái khi task xong |
| Sửa | `docs/features/ads/plans/T-060-don-native-sao-va-chu-lon.md` | Nhật ký thực hiện (mục 10) |

Ảnh chuẩn có sẵn không được đổi. Lý do (L-020): mọi ảnh chuẩn có native nhỏ dùng CTA "Install" (`FakeNativeAds.kt` `sampleNativeAdAssets`, `BottomAdBarScreenshotTest.kt:73`, `CollapsibleNativeAdScreenshotTest.kt:73`, `SampleScreenScreenshotTest.kt:124`, `PreviewAdGallerySlots.kt:39`), rộng 80 dp ở cỡ chữ 1.0 và 103 dp ở 2.0 (đo ở mục 4), dưới 120 dp; hàng sao với `PreviewNativeAdBinder` chỉ thêm một `Box` cùng kích thước. Người đọc ảnh: `scripts/check.sh` đọc mọi ảnh trong `src/test/screenshots/` theo module, không theo tên; `grep -rn "NativeAdCard_" scripts/ .github/` (2026-10-29) không thấy script nào đọc tên các ảnh này; hai ảnh mới không trùng tên ảnh trong `features/ads/design/screens/`, nên `scripts/design-compare.sh` không so chúng.

`scripts/ads-plan.py` nhắc tới `docs/ops/ads-guide.md` chỉ trong chú thích và chuỗi chỉ dẫn (mục 3 và mục 5 của hướng dẫn: vị trí, nhóm giãn cách), không đọc bảng kích thước native ở mục 4 mà plan sửa, nên không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay dời vị trí. Đổi giao diện của native nhỏ (CTA tối đa 120 dp) và cách SDK tính lượt chạm trên hàng sao của medium và native đầy đủ (KB-2). Không đổi tần suất, mã quảng cáo hay tùy chọn yêu cầu.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `NativeAdView.setStarRatingView` thuộc `com.google.android.gms:play-services-ads` (khóa `play-services-ads` trong `gradle/libs.versions.toml`), đã là phụ thuộc biên dịch của `:core:ads` (`NativeAdViewBinding.kt` đã dùng `NativeAdView`, và `NativeAdViewBindingTest` đọc các getter của nó trên JVM). `widthIn` thuộc Compose Foundation đã có.

### 3.4. Phương án dự phòng

Không có. Cả hai kỹ thuật lặp lại mẫu có sẵn (`binder.Asset` cho phần quảng cáo; `widthIn` đã dùng trong `NativeAdCallToAction`); giới hạn 120 dp đã chạy thử bằng bản sao bố cục (mục 4).

### 3.5. Hành vi đã có bị đổi

`python3 -I scripts/existing-tests.py docs/features/ads/plans/T-060-don-native-sao-va-chu-lon.md` (2026-10-29, nhánh chính `c29ce2b`): kết quả dán ở cuối mục này. Đã đọc các test của nhóm "Cần xem trước" và "Nên xem" có đo native nhỏ, CTA hay hàng sao.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| CTA của native nhỏ rộng theo chữ, không giới hạn (chữ dài thì chiếm hết hàng) | CTA rộng tối đa 120 dp (bước 2) | Không test nào khẳng định CTA rộng hơn 120 dp. `NativeAdFontScaleTest.cardSizes_ctaTextFits_fontScales` (small, `longFullNativeAdAssets`) chỉ kiểm chữ CTA không cắt theo chiều cao, nằm trong CTA, CTA cao 34 dp ở 1.0 và 1.3, ít nhất 34 dp ở 2.0, thẻ 80 dp: CTA rộng 120 dp vẫn một dòng, các câu này không đổi. `NativeAdCardTest` các test small (cao 80 dp, CTA 34 dp chữ 13 sp, góc AdChoices trống, chữ dài giữ chiều cao) dùng CTA "Install" (80 dp) hay chỉ đo chiều cao | Giữ |
| Hàng sao vẽ trong `Box`, không đăng ký với SDK | Hàng sao nằm trong `Asset(STAR_RATING)` khi có sao (bước 1) | `NativeAdCardTest.medium_starRow16dp_belowBadgeLine_hiddenStarsKeepHeight` (4 sao, hàng 16 dp dưới dòng nhãn; điểm 0 thì 0 sao, thẻ 410 dp, media không dời); `NativeAdFontScaleTest.medium_partsKeepSize_mediaShrinksFirst_fontScales` (hàng sao 16 dp, media dưới hàng sao); `NativeAdCardTest.full_starRow_hiddenWhenZero_shownFilledWhenFour`; `NativeAdViewBindingTest.registersEveryAssetView_medium` (không đọc `starRatingView`) | Giữ: tag `STAR_ROW`, `STAR` và kích thước không đổi với `PreviewNativeAdBinder` |
| `registered`, `register` có 5 nhánh | 6 nhánh, thêm `STAR_RATING` | `NativeAdViewBindingTest.registersEveryAssetView_small`, `_medium`, `removingMedia_unregistersAndBindsAgain` | Giữ |
| KDoc `NativeAdSlot` nhắc khóa đã bỏ | KDoc nói khe co về 0 dp khi nạp lỗi | Không có (chú thích) | Không áp dụng |

Các test khác của hai nhóm đã đọc: đo native nhỏ hay hàng sao theo cách plan không đổi, giữ nguyên. `NativeAdCardTest.small_leavesAdChoicesCornerFree` (CTA "Install" cách mép thẻ ít nhất 24 dp: CTA 80 dp, không chạm giới hạn), `longTexts_keepHeight` (tiêu đề dài, CTA "Install": chỉ đo chiều cao 80 dp, tiêu đề 20 dp), `small_badgeTextIs10sp_belowHeadline`, `small_is80dpHigh`, `small_ctaIs34dpHighWith13spText`, `NativeAdSlotTest.bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight`, `BottomAdBarTest.reservedSlot_equalsDrawnSlotHeight` (chiều cao 80 dp không đổi); `NativeAdCardTest.full_partsKeepSize_insideCard_textsAndFontScales`, `full_mediaFloor120dp_cardGrows_atFontScale3` (`STAR_ROW` cao 16 dp, cách đáy thẻ 13 dp: hàng sao trong `Asset` cao 16 dp, cùng chỗ). Các test của thẻ ở bước, lớp thu gọn, thẻ dọc, trang toàn trang, ô trong danh sách và khe (`NativeFlowStep*`, `CollapsibleNativeAdTest`, `InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`) không dùng `SmallLayout` với CTA dài hay hàng sao, giữ nguyên.

Kết quả `existing-tests.py` (tóm tắt; bảng đầy đủ chạy lại bằng cùng lệnh):

```
File của plan: core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt, core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt, core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt
Test có sẵn dùng tên hay tag của các file đó: 145.
Cần xem trước: đo vị trí hay kích thước, dùng tên mà plan nêu, plan chưa nhắc tới test: 59
Nên xem: dùng tên mà plan nêu, plan chưa nhắc tới test: 17
Plan đã nhắc tới 8 test trong số này.
Còn 61 test chỉ dùng tên mà plan không nêu; xem bằng --all.
```

## 4. Kiểm thử

**Số đo trên code hiện tại (L-029)**, 2026-10-29, nhánh chính `c29ce2b`. Test tạm không commit `TmpT060MeasureTest` (đã xóa) trong `:core:ads`, `@GraphicsMode(NATIVE)`, `NativeAdCard` trong `Box(fillMaxWidth().padding(16.dp))` như `NativeAdCardScreenshotTest`, cỡ chữ qua `WithFontScale` (bảng phi tuyến của Android 14), hai bộ chữ `sampleNativeAdAssets` (CTA "Install") và `longFullNativeAdAssets` (CTA "Install now and get the full version"). Lệnh: `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TmpT060MeasureTest'` (sau `source ~/.local/share/app-project-template/env.sh`), qua, kết quả ghi vào tệp trong thư mục tạm. Ảnh tạm chụp bằng `-Proborazzi.test.record=true --rerun` vào thư mục tạm (không vào `src/test/screenshots/`). "Ký tự thấy" là `TextLayoutResult.getLineEnd(0, visibleEnd = true)` trên số ký tự của chữ; "cắt theo chiều cao" là `size.height < multiParagraph.height` (cách đo của T-045).

Native nhỏ (`SMALL`, khe 80 dp ở mọi trường hợp; chữ không bị cắt theo chiều cao và nằm trong khung ở mọi trường hợp):

| Khung, cỡ chữ, chữ | Tiêu đề: rộng, ký tự thấy | Nhãn "Ad": khung, ký tự thấy | CTA: khung, ký tự thấy |
| --- | --- | --- | --- |
| 411 dp, 1.0, ngắn | 203 dp, 18/18 | 21,3 x 16 dp, 2/2 | 80 x 34 dp, 7/7 |
| 411 dp, 2.0, ngắn | 180 dp, 12/18 "…" | 34,7 x 32 dp, 2/2 | 103 x 38,7 dp, 7/7 |
| 411 dp, 1.0, dài | 40,3 dp, 3/75 | 21,3 x 16 dp, 2/2 (người quảng cáo 0/62) | 242,7 x 34 dp, 36/36 |
| 411 dp, 1.3, dài | **0 dp, 0/75** | **rộng 0 dp, 1/2** | 295 x 34 dp, 32/36 |
| 411 dp, 2.0, dài | **0 dp, 0/75** | **rộng 0 dp, 1/2** | 295 x 38,7 dp, 21/36 |
| 360 dp, 1.0, ngắn | 152 dp, 18/18 | 21,3 x 16 dp, 2/2 | 80 x 34 dp, 7/7 |
| 360 dp, 2.0, ngắn | 129 dp, 7/18 "…" | 34,7 x 32 dp, 2/2 | 103 x 38,7 dp, 7/7 |
| 360 dp, 1.0, dài | **0 dp, 0/75** | **rộng 0 dp, 1/2** | 242,7 x 34 dp, 36/36 |
| 360 dp, 1.3 và 2.0, dài | **0 dp, 0/75** | **rộng 0 dp, 1/2** | 244 dp rộng, 25/36 và 16/36 |

Thẻ dọc (`CARD`, 180 x 300 dp ở cả hai khung, thêm 156 x 300 dp ở khung 360 dp; cỡ chữ 1.0, 1.3, 2.0; hai bộ chữ): thẻ giữ 300 dp; nhãn "Ad" luôn 2/2 (34,7 tới 37,3 x 32 dp ở 2.0); tiêu đề ít nhất 6 ký tự trước "…" (156 dp, 2.0); CTA không cắt theo chiều cao (37,3 dp ở 2.0), nằm trong thẻ; chữ dài chỉ bị rút gọn bằng "…". **Thẻ dọc không có lỗi**, chỉ thêm test và ảnh chuẩn.

Kết luận: ở cỡ chữ 2.0 với chữ ngắn, native nhỏ không cắt chữ. Lỗi thật là CTA dài: `Row` đo CTA (không có trọng số) trước cột chữ `weight(1f)`, CTA lấy hết chỗ, cột chữ còn 0 dp. Ảnh tạm ở khung 360 dp cỡ chữ 2.0 cho thấy thẻ chữ dài chỉ còn icon và CTA "Install now and …", không có tiêu đề, không có nhãn "Ad".

**Chạy thử phần sửa** bằng test tạm `TmpT060CapTest` (đã xóa): bản sao `SmallLayout` trong file test, dùng các composable `internal` có sẵn (`NativeAdFrame`, `NativeAdIcon`, `NativeAdHeadline`, `NativeAdBadgeLine`, `NativeAdCallToAction`), CTA thêm `Modifier.widthIn(max = 120.dp)`; không sửa code ngoài test. Lệnh `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TmpT060CapTest'`, qua. Kết quả (tiêu đề, "Ad", CTA là ký tự thấy; nhãn "Ad" rộng 21,3 / 25,3 / 34,7 dp ở 1.0 / 1.3 / 2.0 trong mọi trường hợp; CTA không cắt theo chiều cao):

| Khung | 2.0, ngắn | 1.0, dài | 1.3, dài | 2.0, dài |
| --- | --- | --- | --- | --- |
| 411 dp | tiêu đề 12/18, "Ad" 2/2, CTA 7/7 | 22/75, 2/2, 12/36 | 16/75, 2/2, 9/36 | 10/75, 2/2, 6/36 |
| 360 dp | 7/18, 2/2, 7/7 | 14/75, 2/2, 12/36 | 9/75, 2/2, 9/36 | 6/75, 2/2, 6/36 |
| 320 dp (tham khảo, bề rộng nhỏ nhất của KB-32) | 3/18, 2/2, 7/7 | 7/75, 2/2, 12/36 | 5/75, 2/2, 9/36 | 2/75, 2/2, 6/36 |

Chữ ngắn có cùng số ký tự như code hiện tại (CTA 80 và 103 dp chưa chạm 120 dp), nên ảnh chuẩn có sẵn không đổi.

Test viết trước, thất bại đúng lý do trên code hiện tại:

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2: hàng sao đăng ký với SDK (medium) | Compose UI test | `NativeAdViewBindingTest.registersStarRatingView_medium_whenRated`: `NativeAdViewHost(bind = ::countingBind)` với `NativeAdLayout(assets, MEDIUM, binder, ...)`, `assets` là state. Điểm 4.0: `adView.starRatingView` khác `null`, khác `headlineView`, `iconView`, `callToActionView`; bound của view (`getGlobalVisibleRect` hay `height`) cao 16 dp (48 px ở xxhdpi, sai số 1 px). Đối chứng (L-055): đổi sang `starRating = 0.0`, `waitForIdle()`: `starRatingView` là `null` và `binds` tăng; đổi lại 4.0: khác `null`. Hôm nay đỏ: `starRatingView` là `null` ở điểm 4.0 |
| KB-2: hàng sao đăng ký với SDK (đầy đủ) | Compose UI test | `NativeAdViewBindingTest.registersStarRatingView_full`: `FULL`, điểm 4.0: `starRatingView` khác `null`; `starRating = null`: `null`. Hôm nay đỏ ở câu đầu |
| Native nhỏ không có hàng sao | Compose UI test | `NativeAdViewBindingTest.registersEveryAssetView_small` thêm `assertNull(view.starRatingView)` (giữ đúng ngay, canh không đăng ký thừa) |
| KB-1: CTA của native nhỏ tối đa 120 dp qua binder của SDK (cách quảng cáo thật vẽ: mỗi phần là `AndroidView(ComposeView)` của `SdkNativeAdBinder.Asset`) | Compose UI test | `NativeAdViewBindingTest.sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible`: `NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(379.dp))` với `NativeAdLayout(longFullNativeAdAssets, SMALL, binder, showAdChoicesMarker = false)`. Kiểm: `CTA` rộng 120 dp (sai số 0,5), `HEADLINE` rộng hơn 0 dp, `BADGE` rộng hơn 0 dp, thẻ cao 80 dp. Đối chứng cùng test: `sampleNativeAdAssets` thì `CTA` rộng 80 dp. Hôm nay đỏ: CTA rộng hơn 120 dp, tiêu đề hẹp (số đo với `PreviewNativeAdBinder`: CTA 242,7 dp, tiêu đề 40,3 dp; phiên ghi số đo thật dưới SDK khi test đỏ) |
| Chiều cao giữ nguyên dưới SDK | Compose UI test | `NativeAdViewBindingTest.sdkLayout_keepsCardHeights` thêm ở bước medium: `STAR_ROW` cao 16 dp, media vẫn 196 dp (qua ngay, canh) |
| KB-1: chữ native nhỏ và thẻ dọc ở cỡ chữ lớn, khung 411 dp | Compose UI test | `NativeAdFontScaleTest.smallAndCard_headlineBadgeCta_notCut_fontScales` (khung lớp `w411dp-h891dp-xxhdpi`). Trường hợp: `SMALL` trong `Box(fillMaxWidth().padding(16.dp))` (379 dp), `CARD` `Modifier.size(180.dp, 300.dp)`; cỡ chữ 1.0, 1.3, 2.0; `sampleNativeAdAssets` và `longFullNativeAdAssets`. Mỗi trường hợp: (a) khung cao `NativeAdDimens.BottomSlotHeight` (80 dp) với small, 300 dp với thẻ; (b) tiêu đề, chữ "Ad", chữ CTA không cắt theo chiều cao và nằm trong khung (trên, dưới, trái, phải; sai số 0,5 dp); (c) chữ "Ad" thấy 2/2 ký tự và khung `BADGE` rộng ít nhất bằng chiều rộng dòng chữ "Ad" (`TextLayoutResult.size.width`); (d) tiêu đề thấy ít nhất 4 ký tự (`getLineEnd(0, visibleEnd = true) >= 4`); (e) CTA thấy ít nhất 1 ký tự. Thông điệp ghi dạng, khung, cỡ chữ, bộ chữ và số đo. Hôm nay đỏ: small chữ dài ở 1.3 có tiêu đề 0/75 và "Ad" rộng 0 dp |
| KB-1: như trên, khung màn hình thấp | Compose UI test | `NativeAdFontScaleTest.smallAndCard_headlineBadgeCta_notCut_fontScales_small`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`: cùng hàm kiểm, small rộng 328 dp, thẻ 180 x 300 dp và 156 x 300 dp. Hôm nay đỏ: small chữ dài ở 1.0 có tiêu đề 0/75 và "Ad" rộng 0 dp |
| KB-1: CTA của native nhỏ không chiếm hết hàng | Compose UI test | `NativeAdCardTest.small_ctaAtMost120dpWide_shortCtaKeepsWidth`: small rộng 379 dp, cỡ chữ 1.0, `longFullNativeAdAssets`: `CTA` rộng 120 dp (sai số 0,5), tiêu đề rộng hơn 0 dp. Đối chứng cùng test: `sampleNativeAdAssets` thì `CTA` rộng 80 dp (`CallToActionMinWidth`, như hôm nay). Hôm nay đỏ: CTA rộng 242,7 dp |
| Giao diện ở cỡ chữ 2.0, màn hình thấp (L-028) | Screenshot test | `NativeAdCardScreenshotTest.fontScale2_small`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`: `captureRoboImage("src/test/screenshots/NativeAdCard_font-scale-2_small.png") { WithFontScale(2f) { OnScreen(darkTheme = false) { Column(spacedBy(Spacing.md)) { small ngắn; small dài; Row(spacedBy(Spacing.md)) { thẻ 156 x 300 dp ngắn; thẻ 156 x 300 dp dài } } } } }`. Ảnh mới, phiên và rà soát mở xem: small chữ dài có tiêu đề rút gọn, nhãn "Ad", CTA "Inst…" rộng 120 dp |
| Giao diện với chữ dài | Screenshot test | `NativeAdCardScreenshotTest.longTexts_light` (khung lớp 411 dp): `captureRoboImage("src/test/screenshots/NativeAdCard_long-texts_light.png") { OnScreen(darkTheme = false) { Column(spacedBy(Spacing.md)) { NativeAdCard(longFullNativeAdAssets, SMALL); NativeAdCard(longFullNativeAdAssets, CARD, Modifier.size(180.dp, 300.dp)) } } }`. Ảnh mới |
| Ảnh chuẩn có sẵn không đổi | Lệnh | `./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun`, `./gradlew :feature:sample:recordRoborazziDebug :feature:sample:testDebugUnitTest --rerun`, `./gradlew :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun` (L-114), rồi `git status --short -- '*/src/test/screenshots/'` chỉ có hai ảnh mới của mục 3.2 |
| Đúng thiết kế | Không có ảnh thiết kế riêng: thiết kế chỉ vẽ chữ ngắn ở cỡ 1.0, các ảnh đó không đổi. Ảnh thiết kế của ticket khác (T-014, T-027, T-030, T-036): `AdGalleryScreen_collapsible-collapsed_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_default_light`, `AdGalleryScreen_inlist_light`, `NativeFullScreenAdScreen_default_dark`, `NativeFullScreenAdScreen_default_light`, `SampleScreen_ad-loading_light`, `SampleScreen_ready_dark`, `SampleScreen_ready_light` | Không ghi lại ảnh nào trong số này |
| KB-2: chạm vào hàng sao mở quảng cáo | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, màn xem thử, native medium: khi quảng cáo có hàng sao (quảng cáo thử của AdMob không phải lúc nào cũng có điểm; thử vài lần làm mới), chạm vào hàng sao thì mở trang của người quảng cáo như chạm vào tiêu đề |
| KB-1: chữ lớn trên máy | Kiểm tra thủ công (máy thật: khác, cỡ chữ và độ dài chữ của quảng cáo thật thay đổi theo mạng quảng cáo) | Đặt cỡ chữ lớn nhất của máy, mở màn mẫu và màn xem thử: native nhỏ ở đáy và trong danh sách luôn thấy tiêu đề (có thể "…"), nhãn "Ad", CTA |
| KDoc `NativeAdSlot` | Kiểm tra thủ công (người: đọc chú thích) | Rà soát đọc KDoc mới, `grep -rn "ads_native_load_timeout" core/ads/src/main` không còn kết quả |

**Ngoại lệ viết test trước:** KDoc của `NativeAdSlot.kt` và hai dòng của `docs/ops/ads-guide.md` (chú thích, tài liệu: không có hành vi để test đỏ). Hai screenshot test mới qua ngay khi chưa có ảnh (Roborazzi ghi ảnh mới), nên không đỏ được trước code; điều chúng giữ đã có test đo ở bảng trên.

### 4.1. Chạy thử

Không cần (E dưới 2). Phần đo và chạy thử bằng test tạm đã ghi ở mục 4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi giao diện native nhỏ và phần SDK tính lượt chạm của hàng sao), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Ba file `src/main` trong một module, không đổi giao diện công khai (`NativeAdAsset`, `NativeAdStarRow`, `NativeAdDimens` đều `internal` hay `private`) |
| B. Độ rõ của yêu cầu | 1 | Spec rõ; AI tự chọn giới hạn 120 dp và ngưỡng 4 ký tự của tiêu đề (câu hỏi 1, 4) |
| C. Khả năng kiểm chứng tự động | 1 | Chữ và khung đo bằng test; lượt chạm thật trên hàng sao chỉ kiểm được trên máy thật, test JVM chỉ kiểm view đã đăng ký |
| D. Ảnh hưởng chéo | 2 | `NativeAdCard.kt` cũng do T-064, T-065 sửa (bố cục native nhỏ, medium theo JBase); native nhỏ dùng ở thanh đáy, danh sách, màn mẫu |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn: thêm một phần vào `binder.Asset`, một `widthIn` |
| **Tổng** | 6 | **Mức: Cao (chạm điểm chặn 1 và 7)** |

Plan rủi ro cao chạm quảng cáo: người phụ trách sản phẩm biết trước khi duyệt (`risk-scoring.md`, mục 3).

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdCard.kt` (mọi native trong màn hình), `NativeAdViewBinding.kt` (mọi quảng cáo native thật gắn qua `NativeAdViewHost`, kể cả native toàn màn hình: thêm một nhánh `when`, không đổi phần khác). Native nhỏ hiện ở thanh đáy (`BottomAdBar`), danh sách (`InListAds`), màn mẫu `:feature:sample`, màn xem thử `:app` staging.
- Task khác có thể xung đột: T-064 (vẽ lại native theo JBase, thiết kế), T-065 (kích thước native theo JBase: native nhỏ cao 90 tới 200 dp, icon 44 dp, tiêu đề 16 sp, CTA 44 dp) sửa `SmallLayout`, `NativeAdDimens`, `MediumLayout` của cùng file. T-065 phải giữ hai test chữ lớn của plan này (đổi chiều cao khe qua `NativeAdDimens.BottomSlotHeight` hay hàm theo bề rộng, test đọc hằng số chứ không viết cứng 80) và quyết giữ hay bỏ `SmallCallToActionMaxWidth` theo layout JBase; hàng sao của medium giữ (câu trả lời 48). Plan của T-065 phải tính tới các test của plan này: bỏ giới hạn 120 dp thì viết lại `NativeAdCardTest.small_ctaAtMost120dpWide_shortCtaKeepsWidth` và `NativeAdViewBindingTest.sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible` theo ADR-0031 (ghi ở mục 3.5 của T-065), và giữ câu kiểm tiêu đề còn ít nhất 4 ký tự, nhãn "Ad" đủ 2 ký tự của hai test chữ lớn.
- Đề xuất thứ tự chạy: chạy T-060 trước T-065 (ticket T-065 ghi "T-060 làm trên kích thước cũ"), để T-065 kế thừa test chữ lớn và đăng ký hàng sao.

## 7. Điều kiện dừng riêng

- `SmallLayout` không còn là khe 80 dp với hàng icon, cột chữ, CTA như mục 4 đo (ví dụ T-065 đã merge trước): dừng, vì số đo và cách sửa của plan không còn đúng.
- `NativeAdView` của bản `play-services-ads` đang dùng không có `starRatingView`/`setStarRatingView`: dừng (không thêm hay nâng thư viện).

## 8. Câu hỏi cho người duyệt

1. **Giới hạn CTA của native nhỏ 120 dp.** Thiết kế không có số này. 120 dp lớn hơn CTA "Install" ở cỡ chữ 2.0 (103 dp), nên ảnh chuẩn không đổi, và để lại cho cột chữ ít nhất 80 dp ở khung 320 dp. Phương án khác: tối đa 40% chiều rộng hàng (theo màn hình, nhưng đổi chỗ cắt ở mỗi máy). Đề xuất: 120 dp, T-064 vẽ lại thì thay số theo JBase.
2. **Sửa ngay hay để T-065.** Lỗi mất tiêu đề và nhãn "Ad" có ở cỡ chữ 1.0 khi CTA dài (khung 360 dp), không chỉ ở 200%. Đề xuất: sửa ngay trong T-060 (một dòng, không đổi ảnh chuẩn có sẵn), T-065 kiểm lại bằng cùng test. Nếu anh muốn để T-065, plan bỏ bước 2 và test `small_ctaAtMost120dpWide_shortCtaKeepsWidth`, hai test chữ lớn chỉ kiểm chữ ngắn, và tiêu chí chữ lớn của KB-1 với chữ dài chuyển sang T-065.
3. **Đăng ký hàng sao chỉ khi có sao.** Quảng cáo không có điểm (hay điểm 0) thì không vẽ sao (O-08); plan không đăng ký `starRatingView` khi đó, để SDK không coi một ô trống 16 dp là phần của quảng cáo. Đề xuất: như vậy.
4. **"Không cắt mất chữ" của tiêu đề.** Plan đòi tiêu đề còn ít nhất 4 ký tự trước "…" ở khung 360 dp và 411 dp (sau sửa: ít nhất 6 ký tự ở 2.0). Ở khung 320 dp, cỡ chữ 2.0, tiêu đề chỉ còn 2 tới 3 ký tự; plan không đưa khung 320 dp vào test, T-065 (tiêu đề 16 sp, khe cao hơn) xem lại. Đề xuất: chấp nhận ngưỡng này.
5. **Tài liệu thiết kế.** Plan chỉ ghi giới hạn 120 dp vào `docs/ops/ads-guide.md`, không sửa `features/ads/design/README.md` hay file Figma, vì T-064 vẽ lại native nhỏ. Đề xuất: như vậy.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
