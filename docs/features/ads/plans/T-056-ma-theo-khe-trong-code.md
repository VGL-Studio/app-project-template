---
ticket: T-056
title: Mã đơn vị theo khe trong code: mọi lớp của thanh đáy có mã ở production
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 23.9M        # số gợi ý cỡ M (scripts/token-usage.py estimate, 2026-10-29); giữ nguyên dù phần code nhỏ, vì có test giao diện của thanh đáy và ba tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-056: MÃ ĐƠN VỊ THEO KHE TRONG CODE: MỌI LỚP CỦA THANH ĐÁY CÓ MÃ Ở PRODUCTION

## 1. Tóm tắt

`ConfiguredAdUnitIds` (bản production) đọc thêm mã theo khe, dạng `<tên khe>=<mã>` với tên khe là mã định dạng của M-05 (`banner`, `native_collap`), cho các lớp của thanh đáy; dạng cũ `<mã vị trí>=<mã>` vẫn đọc như hôm nay và vẫn là nguồn duy nhất của định dạng chính của vị trí. `missing` đổi thành danh sách khóa cần có trong `adUnits` (khóa vị trí, rồi khóa khe của lớp thanh đáy). Kiểm chứng bằng unit test của `AdUnitIdsTest` và test giao diện của `BottomAdBarTest` dùng `ConfiguredAdUnitIds` thật với chuỗi mã giả; không chạm `scripts/release.sh` (T-057).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-17 | Thêm 2026-10-29: khi app phát hành bản production có thanh đáy thì khe banner và lớp native thu gọn đều có mã quảng cáo và gửi được yêu cầu, theo mã theo khe (câu hỏi 14) | Phần code: `ConfiguredAdUnitIds.of(placement, BANNER)` và `of(placement, NATIVE_COLLAPSIBLE)` trả mã của khe `banner`, `native_collap` khi `adUnits` có hai khóa đó; thanh đáy dùng mã đó để tạo `AdView` và gửi lượt nạp của lớp. Phần đưa mã vào build (đọc, kiểm, điền mã thử trong `release.sh`) là T-057. Tiêu chí chỉ đạt hẳn khi cả hai ticket xong |
| KB-17 | Khi tắt native ở đáy thì đáy màn hình có banner | Ở production, hôm nay tiêu chí này không đạt (banner không có mã, B-04). Sau task, đạt khi có khóa `banner=` |
| KB-17 | Khi bật native ở đáy thì có native nhỏ và lớp native thu gọn nổi trên | Như trên, với khóa `native_collap=` |

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); task không đổi giao diện.

ADR liên quan: ADR-0004 (một lớp trung gian `:core:ads`), ADR-0009 (không chạm `release.sh`), ADR-0013 (mã production đưa vào lúc build, không vào repo), ADR-0026 và ADR-0038 (nghiệp vụ: M-05, B-04, D-01 tới D-05), ADR-0029, ADR-0031 (test cũ).

**Chọn dạng khóa (câu hỏi 14, M-05).** Câu hỏi 14 đã chốt "theo khe như AdsKit: vị trí chỉ để bật tắt và ghi doanh thu, ít mã phải tạo hơn". M-05 đặt tên khe theo bản đặc tả: `banner` (tự làm mới), `native_collap`..., trùng `AdFormat.code` của `BANNER` và `NATIVE_COLLAPSIBLE`. Plan chọn khóa `<tên khe>=<mã>` một mã cho cả app, không chọn `<mã vị trí>/<định dạng>=<mã>`, vì:

- đúng ý câu 14 (một mã mỗi khe, mọi thanh đáy dùng chung), không thêm một mã mỗi màn;
- `release.sh` hôm nay đã chấp nhận dòng này: mỗi mục `adUnits` chỉ phải khớp `^[a-z0-9_]+=ca-app-pub-[0-9]+/[0-9]+$` (`scripts/release.sh`, dòng 149 tới 157), không đòi khóa là vị trí. Dạng có `/` thì `release.sh` cũ từ chối cả bản phát hành;
- doanh thu và lượt bấm vẫn ghi theo vị trí (D-05, `ad_placement` là mã vị trí của thanh), không đổi.

Mã theo khe chỉ áp cho lớp (định dạng khác định dạng chính của vị trí). Định dạng chính vẫn chỉ đọc khóa vị trí, xem mục 3.1, ý 2 và câu hỏi 1.

## 3. Giải pháp

### 3.1. Cách làm

1. **Đọc khóa (`AdUnitIds.kt`, `ConfiguredAdUnitIds`).** Bộ đọc chuỗi giữ nguyên: tách `;`, mỗi mục `<khóa>=<mã>`, mục hỏng ném `IllegalArgumentException` như hôm nay. Khóa vị trí và khóa khe nằm chung một bảng: không có cú pháp mới, chuỗi cũ đọc ra đúng như cũ.
2. **`of(placement, format)`** ghi đè phương thức mặc định của `AdUnitIds`:
   - vị trí `stagingOnly` thì `null` (như `of(placement)`);
   - `format == placement.format` thì `of(placement)`, tức chỉ khóa vị trí, như hôm nay. Không dùng khóa khe làm mã dự phòng cho định dạng chính: `release.sh` cũ điền mã thử của Google cho mọi khóa vị trí chưa có (`fill_test_units`), nên mã thử đó sẽ luôn thắng mã khe thật, gây nhầm (câu hỏi 1);
   - định dạng khác (lớp của thanh đáy) thì `units[format.code]`, ví dụ `banner`, `native_collap`. Không có thì `null`: lớp không gửi, không chiếm chỗ (B-04), như hôm nay.
   - `of(placement)` không đổi.
3. **Lớp nào cần mã theo khe.** Thêm trong `AdUnitIds.kt`: `internal val BOTTOM_BAR_LAYER_FORMATS = listOf(AdFormat.BANNER, AdFormat.NATIVE_COLLAPSIBLE)` và `internal val AdPlacementDeclaration.layerFormats: List<AdFormat>` trả danh sách đó khi `format == AdFormat.NATIVE_SMALL`, rỗng khi khác. Lý do: thanh đáy là vị trí định dạng `native_small` (MC-08, `BottomAdBarScaffold` từ chối định dạng khác, `BottomAdBar.kt:167`), banner và lớp thu gọn là hai lớp của nó (D-01). Một vị trí `native_small` đặt trong màn (`NativeAdSlot`) cũng bị tính là có lớp (câu hỏi 2).
4. **`missing`** đổi từ `List<AdPlacement>` sang `List<String>`: khóa còn thiếu trong `adUnits`, đúng chữ người phát hành ghi vào `release.properties`. Thứ tự: khóa vị trí không phải staging chưa có mã (thứ tự khai báo, như hôm nay), rồi khóa khe của `layerFormats` của các vị trí đó, mỗi khóa một lần, theo thứ tự `BOTTOM_BAR_LAYER_FORMATS`. Tính bằng hàm thuần `internal fun missingUnitKeys(placements: List<AdPlacementDeclaration>, configured: Set<String>): List<String>` để test được với danh sách vị trí riêng; `missing = missingUnitKeys(AdPlacement.entries.map { it.declaration }, units.keys)`. Khóa vị trí có mã nhưng lớp thiếu mã thì chỉ khóa khe nằm trong `missing`.
   - Ai đọc `missing`: không code nào ngoài test (`grep -rn '\.missing'`: chỉ `AdUnitIdsTest`). `ProductionAdsModule` chỉ dựng `ConfiguredAdUnitIds(BuildConfig.AD_UNITS)`, không đọc `missing`; `preProduction*Build` của `app/build.gradle.kts` chỉ kiểm tham số `adUnits` có mặt, không kiểm nội dung. `release.sh` chỉ nhắc `ConfiguredAdUnitIds.missing` trong chú thích (dòng 215) và tự đọc `AdPlacement.kt`. `status.py` đọc `AdPlacement.kt`, không đọc Kotlin. Nên đổi `missing` không làm bản phát hành nào thất bại.
5. **Bản phát hành khi `release.sh` chưa đổi (trước T-057).** `release.properties` cũ chỉ có khóa vị trí: mã định dạng chính như hôm nay, lớp `null` như hôm nay, không gì đổi. Người phát hành muốn có lớp trước T-057 thì thêm `banner=ca-app-pub-.../...;native_collap=ca-app-pub-.../...` vào `adUnits`: `release.sh` cũ chấp nhận (mục 2) và truyền nguyên vào `-PadUnits`; `fill_test_units` chỉ thêm khóa vị trí nên không đụng hai khóa này. Thiếu khóa khe thì lớp không có quảng cáo, không có mã thử (mã thử cho khe là việc của T-057). Ghi điều này ở `ads-guide.md`.
6. **Không trùng tên.** Khóa vị trí và khóa khe chung một bảng, nên một vị trí tên `banner` sẽ đọc nhầm mã của khe. Thêm test `AdUnitIdsTest.placementKeys_neverSlotNames`: không mã vị trí nào trùng `AdFormat.code` nào (app thêm vị trí như vậy thì CI đỏ). Không thêm `require` vào `AdPlacement.kt`, để không đổi file mà `release.sh` và `status.py` đọc.
7. **Chú thích.** KDoc của `AdUnitIds.of(placement, format)`, `ConfiguredAdUnitIds` (dạng chuỗi, hai loại khóa), `missing`; KDoc của `BottomBannerSlot` trong `AdSlot.kt` ("no unit (production today) sends nothing" thành "no unit (production without the `banner` slot key) sends nothing"). Không đổi code của `AdSlot.kt`. KDoc của `CollapsibleNativeAdLayer` (`CollapsibleNativeAd.kt:326-330`) không nhắc production, không sửa.
8. **Tài liệu.** `docs/ops/ads-guide.md` mục 5, ý 2 (câu "Mã đơn vị theo vị trí hay theo khe ... câu hỏi 14" thành: định dạng chính theo vị trí, lớp thanh đáy theo khe `banner`, `native_collap`) và ý 6 (bỏ "production hôm nay chỉ có mã của định dạng chính"; ghi hai khóa khe, cách thêm trước T-057, thiếu thì lớp không có quảng cáo); mục 9, dòng kiểm "Mỗi vị trí trong `AdPlacement` có mã production" thêm hai khóa khe khi app có thanh đáy. Không sửa `docs/ops/release.md` (hướng dẫn điền `release.properties` là phạm vi của T-057), xem câu hỏi 3.

Mẫu làm theo: `GoogleTestAdUnitIds.of(placement, format)` (T-030, ý 6) đã ghi đè cùng phương thức; test theo `AdUnitIdsTest.production_otherFormat_null_ownFormat_configured` và `BottomAdBarTest.bannerUnitIdNull_noLoad_noReservation`.

Ở bước 3 (test viết trước), khung rỗng: `missing` đổi kiểu sang `List<String>` nhưng vẫn chỉ trả khóa vị trí; `missingUnitKeys`, `layerFormats`, `BOTTOM_BAR_LAYER_FORMATS` có chữ ký, thân trả rỗng hay hành vi cũ; `of(placement, format)` chưa ghi đè. Test mới biên dịch được và thất bại vì khẳng định.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdUnitIds.kt` | Ý 1 tới 4, 7: `ConfiguredAdUnitIds.of(placement, format)`, `missing: List<String>`, `missingUnitKeys`, `BOTTOM_BAR_LAYER_FORMATS`, `layerFormats`, KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | Ý 7: chỉ KDoc của `BottomBannerSlot` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` | Test mới của mục 4; ba câu khẳng định `missing` đổi sang khóa chuỗi (mục 3.5); `production_otherFormat_null_ownFormat_configured` giữ nguyên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | Hai test mới dùng `ConfiguredAdUnitIds` thật (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Ý 8: mục 5 ý 2 và ý 6, mục 9 |

Không sửa, đã kiểm nơi đọc (L-020):

- `scripts/release.sh`, `release.properties`: T-057, việc của người (ADR-0009). Đọc ở mục 3.1, ý 4, 5.
- `scripts/status.py` (mục 6 "Sẵn sàng phát hành"): đọc `AdPlacement.kt` theo luật của `release.sh`, không đọc `AdUnitIds.kt`; `AdPlacement.kt` không đổi nên trang không đổi. Trang sẽ chưa liệt kê hai khóa khe (câu hỏi 4).
- `scripts/tests/run.sh`: test của `release.sh` dùng `scripts/tests/fixtures/release/AdPlacement.kt` và `AdPlacement.kt` thật (dòng 1303, chỉ kiểm mã thoát); cả hai không đổi.
- `app/build.gradle.kts`: chú thích dòng 47 ghi dạng `<placement key>=<ad unit>`, vẫn đúng; không sửa file build để không kéo bản release vào `pr-check` chỉ vì chú thích. `AD_UNITS` truyền nguyên chuỗi, không cần đổi.
- `app/src/production/.../ProductionAdsModule.kt`, `app/src/staging/.../StagingAdsModule.kt`: không đổi (cùng hàm dựng, staging vẫn `GoogleTestAdUnitIds`).
- `AdsController.unitId` gọi `unitIds.of(placement, format)`: mọi chỗ dùng (`AdSlot.kt:85`, `CollapsibleNativeAd.kt:336`, `NativeAdSlot.kt`, `NativeAdCell.kt`, `NativeFlowStep.kt`, `NativeAdPools.kt`) tự nhận mã mới, không sửa. Chỉ hai lớp thanh đáy hỏi định dạng khác định dạng chính.
- Test dùng bộ `AdUnitIds` giả (`AdsControllerFixtures`, `AdsControllerFunnelTest`, `AdsControllerNativeComboTest`, `AdsControllerResumeTest`, `RewardedActionTest`, `BottomAdBarTest.AllUnits`, `NoLayerUnits`): lambda `AdUnitIds { null }` và `object` không bị ảnh hưởng vì interface không đổi.
- `scripts/ads-plan.py` nhắc `docs/ops/ads-guide.md` (dòng 373, 540, 545, 560) chỉ bằng chữ trỏ tới mục 3 và mục 5, ý 2; không đọc nội dung file. Plan không đổi tên hay số mục nào của `ads-guide.md`, nên script không bị ảnh hưởng.
- `docs/ops/ads-business-rules.md` M-05 ("mã theo khe hay theo vị trí là câu hỏi 14"): không sửa ở task này (câu hỏi 3).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Lớp vẫn theo `is_show_native_bottom`, `is_show_native_collap`, `is_show_<mã vị trí>` như hôm nay.
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng, tần suất. Ở production, lớp banner và lớp thu gọn của thanh đáy bắt đầu gửi yêu cầu khi `adUnits` có khóa khe (KB-17); không có khóa thì như hôm nay.
- Sự kiện phân tích: Không chạm. Lớp đã ghi số liệu với mã vị trí của thanh và định dạng thật của lớp (T-030).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code mới chỉ dùng Kotlin chuẩn và lớp có sẵn của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Thay đổi là một hàm tra bảng và một hàm thuần tính danh sách, theo mẫu `GoogleTestAdUnitIds` đã có.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (kết quả ở mục 4.1, dòng Lệnh): xem từng test ở nhóm "Cần xem trước" và "Nên xem".

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `missing` là `List<AdPlacement>` | `missing` là `List<String>`, khóa vị trí rồi khóa khe (ý 4) | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`: `assertTrue(ids.missing.isEmpty())` với chuỗi chỉ có khóa vị trí | Câu mới: chuỗi cấu hình thêm `banner=unit/8;native_collap=unit/9`, rồi `assertTrue(ids.missing.isEmpty())`; đối chứng mới: cùng chuỗi không có hai khóa khe thì `missing == listOf("banner", "native_collap")` |
| như trên | như trên | cùng test, đối chứng `withoutAppWide.missing == listOf(OPEN_APP, OPEN_APP_INTER, NATIVE_AFTER_INTER, RESUME, RESUME_INTER)` | Câu mới: `listOf("open_app", "open_app_inter", "native_combo", "resume", "resume_inter", "banner", "native_collap")` (ý định giữ: vị trí toàn app vẫn cần mã ở production; thêm hai khóa khe vì chuỗi không có) |
| như trên | như trên | `AdUnitIdsTest.placementWithoutUnit_isReportedMissingAndShowsNoAd`: `missing == AdPlacement.entries.filterNot { it.stagingOnly }` | Câu mới: `missing == AdPlacement.entries.filterNot { it.stagingOnly }.map { it.key } + listOf("banner", "native_collap")`; `assertNull(ids.of(SAMPLE_NATIVE_SMALL))` giữ |
| Lớp thanh đáy ở production luôn `null` | Có khóa khe thì trả mã khe (ý 2) | `AdUnitIdsTest.production_otherFormat_null_ownFormat_configured` (chuỗi chỉ có `sample_native_small=unit/1`) | Giữ nguyên: chuỗi không có khóa khe nên vẫn `null`; chú thích "until units per slot exist (spec, Q14)" sửa thành "without a slot key" |
| `stagingOnly` không có mã production | Giữ, cả cho lớp (ý 2) | `AdUnitIdsTest.stagingOnlyPlacement_hasNoUnit_evenWhenConfigured` | Giữ |
| Mục hỏng bị từ chối | Giữ (ý 1) | `AdUnitIdsTest.malformedEntry_isRejected` | Giữ |
| Staging trả mã thử theo định dạng của lớp | Không đổi | `AdUnitIdsTest.staging_unitOfEachLayerFormat`, `googleTestUnits_*` | Giữ |
| Lớp không có mã thì không nạp, không chiếm chỗ (B-04) | Không đổi | `BottomAdBarTest.layerUnitIdNull_noLoad_slotStays`, `bannerUnitIdNull_noLoad_noReservation` (bộ giả `NoLayerUnits`) | Giữ |
| Vị trí, kích thước, chạm và vòng đời của lớp thu gọn và khe đáy | Không đổi: plan không sửa code vẽ; các test này dùng bộ giả `AllUnits`, không dùng `ConfiguredAdUnitIds` | `BottomAdBarTest.collapSwitchedOffWhileShown_layerRemovedAndAdDestroyed_slotStays`, `collapse_revealsSlot`, `layer_takesOnlyWidth_touchAboveReachesContent`, `NativeAdFontScaleTest.collapsibleLayer_mediaFloor120dp_layerGrows_fontScales`, `BottomAdBarTest.consentWithdrawn_barGone_adsDestroyed`, `layer_notCountedInReservation`, `placementSwitchedOff_barGone_reservationZero_adsDestroyed` | Giữ (chỉ trùng tên `CollapsibleNativeAdTestTags.AD` mà plan nêu; mục 7, gạch 3) |
| Mã thử của Google theo vị trí và theo định dạng | Không đổi: `GoogleTestAdUnitIds` không sửa | `AdUnitIdsTest.googleTestIds_coverGalleryPlacements`, `googleTestUnit_existsForEveryFormat`, `googleTestUnits_existForEveryPlacement`, `googleTestUnits_nativeFormats`, `googleTestUnits_newFormats` | Giữ |

Ba câu đổi đều là dữ liệu và kiểu của `missing` mà plan nói rõ ở mục 3.1, ý 4 (ADR-0031: có câu của plan, giữ ý định, không nới, có đối chứng, tối đa 3 câu). Ghi vào commit test của bước 3.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-17, thêm 2026-10-29: lớp có mã theo khe | Unit test | `AdUnitIdsTest.production_layerUnits_readFromSlotKeys`: `ConfiguredAdUnitIds("sample_native_small=unit/1;banner=unit/b;native_collap=unit/c")`, `of(SAMPLE_NATIVE_SMALL, BANNER) == "unit/b"`, `of(SAMPLE_NATIVE_SMALL, NATIVE_COLLAPSIBLE) == "unit/c"`, `of(SAMPLE_NATIVE_SMALL, NATIVE_SMALL) == "unit/1"`. Đối chứng: `production_otherFormat_null_ownFormat_configured` (không có khóa khe thì `null`) |
| Dạng cũ vẫn chạy, định dạng chính chỉ theo khóa vị trí (ý 2) | Unit test | `AdUnitIdsTest.production_ownFormat_ignoresSlotKey`: `ConfiguredAdUnitIds("native_small=unit/s;interstitial=unit/i")`: `of(SAMPLE_NATIVE_SMALL)`, `of(SAMPLE_NATIVE_SMALL, NATIVE_SMALL)`, `of(OPEN_APP_INTER)` đều `null`. Đối chứng: thêm `sample_native_small=unit/1;open_app_inter=unit/4` thì trả `unit/1`, `unit/4`. Qua được trên khung rỗng (hôm nay khóa khe không được đọc): là test chặn, ghi vào nhật ký |
| Vị trí chỉ ở staging không có mã lớp | Unit test | `AdUnitIdsTest.production_stagingOnlyLayer_null`: `ConfiguredAdUnitIds("banner=unit/b;native_collap=unit/c")`: `of(GALLERY_NATIVE_SMALL, BANNER)` và `of(GALLERY_NATIVE_SMALL, NATIVE_COLLAPSIBLE)` là `null`; đối chứng: `of(SAMPLE_NATIVE_SMALL, BANNER) == "unit/b"` |
| `missing` liệt kê lớp thiếu mã | Unit test | `AdUnitIdsTest.missingUnitKeys_listsLayerSlotsOnce`: với `AdPlacementDeclaration("bar_a", NATIVE_SMALL)`, `("bar_b", NATIVE_SMALL)`, `("inter_a", INTERSTITIAL)`, `configured = setOf("bar_a", "bar_b", "inter_a")` thì `listOf("banner", "native_collap")` (mỗi khóa một lần dù hai thanh); `configured` thêm `"banner"` thì `listOf("native_collap")`; `configured` rỗng thì `listOf("bar_a", "bar_b", "inter_a", "banner", "native_collap")` |
| Không có thanh đáy thì không đòi khóa khe | Unit test | `AdUnitIdsTest.missingUnitKeys_noBar_noSlotKeys`: chỉ `("inter_a", INTERSTITIAL)` và `("gallery_x", NATIVE_SMALL, stagingOnly = true)`, `configured` rỗng: `listOf("inter_a")`. Đối chứng: bỏ `stagingOnly` của `gallery_x` thì có thêm `"gallery_x", "banner", "native_collap"` |
| `missing` của `ConfiguredAdUnitIds` dùng đúng hàm | Unit test có sẵn, sửa | `configuredUnits_areParsedPerPlacementKey`, `placementWithoutUnit_isReportedMissingAndShowsNoAd` theo mục 3.5 |
| Không trùng tên khóa (ý 6) | Unit test | `AdUnitIdsTest.placementKeys_neverSlotNames`: `AdPlacement.entries.map { it.key }` giao `AdFormat.entries.map { it.code }` là rỗng; đối chứng: tập mã định dạng có `"banner"` và `"native_collap"` (để test không qua vì tập rỗng). Qua ngay trên khung rỗng: là test chặn, ghi vào nhật ký |
| KB-17: tắt native ở đáy thì có banner, gửi yêu cầu (production) | Compose UI test | `BottomAdBarTest.configuredSlotUnits_bannerBuiltAndKeepsHeight`: `layerUnitIds = ConfiguredAdUnitIds("sample_native_small=unit/1;banner=unit/b")`, `flags = barFlags(nativeBottom = false, collap = false)`: `banners.createdCount == 1`, sau `banners.last.emitLoaded()` nội dung chừa `BANNER_HEIGHT_DP`. Đối chứng trong cùng test: trước đó `ConfiguredAdUnitIds("sample_native_small=unit/1")` thì `createdCount == 0`, nội dung tới `SCREEN_HEIGHT` |
| KB-17: bật native ở đáy thì lớp thu gọn nạp (production) | Compose UI test | `BottomAdBarTest.configuredSlotUnits_layerLoads`: `layerUnitIds = ConfiguredAdUnitIds("sample_native_small=unit/1;native_collap=unit/c")`, cờ mặc định (native và lớp bật): `layerFeed.collections == 1`, sau `loadBoth()` có `CollapsibleNativeAdTestTags.AD` và `SLOT_AD`. Đối chứng: `ConfiguredAdUnitIds("sample_native_small=unit/1")` thì `layerFeed.collections == 0`, lớp vắng, khe vẫn có quảng cáo |
| Tài liệu ads-guide đúng với code | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc `ads-guide.md` mục 5 ý 2, ý 6, mục 9: hai khóa khe, dạng `banner=`, `native_collap=`, cách thêm trước T-057, thiếu thì lớp không có quảng cáo |
| KB-17 trên bản production thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | Sau T-057, người phát hành build production có `banner=`, `native_collap=` trong `adUnits`, cài trên máy thật, bật `is_show_<mã thanh>`: có lớp thu gọn; tắt `is_show_native_bottom`: có banner ở đáy. Không thuộc phiên tự chạy |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

Mọi đường gửi yêu cầu của lớp (L-072): lượt đầu, làm mới, thử lại và nạp lại của banner và lớp thu gọn đều lấy mã qua một chỗ, `AdsController.unitId(placement, format)` gọi `unitIds.of(placement, format)` (`AdSlot.kt:85`, `CollapsibleNativeAd.kt:336`). Test hàm đó (`AdUnitIdsTest`) và test thanh đáy lấy mã qua cùng `of(placement, format)` (`BottomAdBarTest`) là đủ; plan không thêm đường gửi nào.

Đối chứng cho test giá trị mặc định (L-055): mỗi test `null` ở trên có câu đối chứng trả mã khi chuỗi có khóa đúng.

**Ngoại lệ viết test trước:** sửa KDoc ở `AdUnitIds.kt`, `AdSlot.kt` và `docs/ops/ads-guide.md`: chỉ chữ, không có hành vi để test. Người duyệt đọc (dòng "người" ở trên).

### 4.1. Chạy thử

Không cần (E dưới 2).

- **Lệnh:** `python3 -I scripts/existing-tests.py docs/features/ads/plans/T-056-ma-theo-khe-trong-code.md` (2026-10-29, cho mục 3.5): 11 test dùng tên của các file trong mục 3.2, plan đã nhắc cả 11; "Cần xem trước" 0, "Nên xem" 0. Lần chạy đầu, khi mục 3.2 còn có `CollapsibleNativeAd.kt` (đã bỏ vì KDoc không cần sửa), có thêm các test của lớp thu gọn trong `BottomAdBarTest` và `NativeAdFontScaleTest`: vẫn ghi ở mục 3.5 vì chúng là test của thanh đáy. Không chạy `./gradlew` hay `scripts/check.sh`: máy soạn plan chưa có JDK; kỹ thuật lặp lại mẫu `GoogleTestAdUnitIds.of(placement, format)` đã qua `check.sh` ở T-030.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**
- quảng cáo, điểm 1: ở production, hai lớp của thanh đáy bắt đầu gửi yêu cầu khi có mã (không thêm vị trí, định dạng hay tần suất, nhưng đổi quảng cáo nào được gửi ở bản phát hành);
- module core dùng chung `:core:ads`, điểm 7. Cấu hình build phát hành không bị sửa, nhưng cách đọc `adUnits` đổi và phải khớp với T-057.

Không thêm quyền, SDK hay dữ liệu. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì sau T-057 bản production có thêm quảng cáo ở thanh đáy.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`), một file logic, đổi kiểu công khai của `ConfiguredAdUnitIds.missing`; hai file test; một tài liệu |
| B. Độ rõ của yêu cầu | 2 | Câu 14 đã chốt "theo khe", nhưng dạng khóa, việc định dạng chính có đọc khóa khe không và cách `release.sh` sẽ đọc là plan tự chọn (mục 8, câu 1, 2, 4) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi hành vi code có unit test và Compose UI test; quảng cáo thật ở production chỉ kiểm trên máy thật sau T-057 |
| D. Ảnh hưởng chéo | 3 | Đổi mã quảng cáo mà bản production dùng; T-057 (người) phải đọc đúng cùng dạng khóa; T-062 cùng sửa `AdUnitIds.kt` |
| E. Độ mới của kỹ thuật | 0 | Lặp lại mẫu ghi đè `of(placement, format)` của `GoogleTestAdUnitIds` |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdUnitIds.kt` (API của `:core:ads`, `ConfiguredAdUnitIds.missing` đổi kiểu), KDoc của `AdSlot.kt`, `AdUnitIdsTest.kt`, `BottomAdBarTest.kt`, `docs/ops/ads-guide.md`.
- Task khác có thể xung đột:
  - **T-057** (người, `depends_on: [T-056]`): phải dùng đúng dạng khóa của plan này. Việc cần thống nhất, ghi vào T-057 khi viết plan của nó:
    - `--list-placements` (và bước in "Vị trí quảng cáo") liệt kê thêm `banner`, `native_collap` khi có vị trí `native_small` không phải `gallery_`, cùng luật với `missingUnitKeys` (mục 3.1, ý 3, 4); hoặc in riêng một dòng "Khe của thanh đáy".
    - `--fill-test-units` và `fill_test_units` điền mã thử theo khe cho khóa khe còn thiếu (`banner`: mã banner thử, `native_collap`: mã native thử), in vào dòng "không có doanh thu" như khóa vị trí.
    - Kiểm mục `adUnits`: dạng regex hôm nay giữ được (khóa khe đã khớp). Nên thêm kiểm khóa lạ (không phải vị trí, không phải khe) để bắt lỗi gõ.
    - Định dạng chính dùng khóa khe (câu hỏi 1) chỉ làm được khi `fill_test_units` không điền mã thử cho vị trí đã có khóa khe; nếu người chọn vậy thì đó là phần của T-057 cộng một ticket code mới.
    - `scripts/tests/run.sh` (test của `release.sh`) và `scripts/tests/fixtures/release/AdPlacement.kt` đi cùng T-057.
    - `docs/ops/release.md` (dòng 26, 66, 72, 91) ghi dạng khóa khe.
    - `scripts/status.py`, mục 6 "Sẵn sàng phát hành", theo luật của `release.sh` (L-086): thêm hai khóa khe cùng lúc với T-057 để trang và script khớp (câu hỏi 4).
  - **T-062** (banner MREC, `depends_on: [T-054]`): thêm `AdFormat.BANNER_MREC` và nhánh trong `GoogleTestAdUnitIds.unitFor`. Cùng file `AdUnitIds.kt`, khác hàm. `banner_mrec` là định dạng chính của vị trí của nó nên đi khóa vị trí, không vào `BOTTOM_BAR_LAYER_FORMATS`; test `placementKeys_neverSlotNames` tự gồm mã `banner_mrec`. Task chạy sau merge nhánh chính trước bước 3.
  - **T-068, T-073** (MAX): mã MAX theo khe là dạng riêng của T-073; không đọc chung bảng này ở plan này.
- Đề xuất thứ tự chạy: T-056 trước T-057 (phụ thuộc). T-056 merge một mình an toàn: bản phát hành với `release.sh` cũ và `release.properties` cũ không đổi gì (mục 3.1, ý 5). Độc lập với các task khác của danh sách chạy, chạy ở đầu được.

## 7. Điều kiện dừng riêng

- Cần sửa `scripts/release.sh`, `release.properties`, `scripts/status.py`, `docs/ops/release.md` hay `app/build.gradle.kts` để test qua: dừng (phạm vi của T-057, việc của người).
- Cần sửa `AdPlacement.kt` (thêm thuộc tính hay `require`): dừng, vì `release.sh` và `status.py` đọc file này.
- Test giao diện có sẵn của thanh đáy (`BottomAdBarTest` ngoài hai test mới) hay ảnh chuẩn nào đổi: dừng, vì plan không đổi cách vẽ.

## 8. Câu hỏi cho người duyệt

1. **Định dạng chính có đọc khóa khe không?** Câu 14 ý đầy đủ là "mọi định dạng một mã mỗi khe, vị trí chỉ bật tắt và ghi doanh thu". Plan chỉ áp khóa khe cho lớp của thanh đáy, định dạng chính vẫn theo khóa vị trí, vì `release.sh` cũ điền mã thử cho mọi khóa vị trí thiếu, nên mã thử sẽ luôn thắng mã khe thật.
   - Đề xuất của AI: giữ như plan. Muốn đổi hẳn sang theo khe cho mọi định dạng thì làm sau T-057, bằng một ticket riêng: khóa vị trí thắng, khóa khe làm mặc định, `release.sh` không điền mã thử khi đã có khóa khe.
2. **Vị trí `native_small` đặt trong màn có bị đòi khóa khe không?** Plan coi mọi vị trí `native_small` là thanh đáy (MC-08), nên một vị trí `native_small` trong màn (`NativeAdSlot`) cũng làm `missing` có `banner`, `native_collap`. Hôm nay dự án mẫu chỉ có `sample_native_small` (thanh đáy) ngoài `gallery_`.
   - Đề xuất của AI: chấp nhận đòi thừa (chỉ là danh sách thông tin, không chặn build). Nếu cần phân biệt thì thêm thuộc tính `bottomBar` vào `AdPlacement`, nhưng việc đó sửa `AdPlacement.kt` và luật đọc của `release.sh`, nên là ticket khác.
3. **Tài liệu luật và phát hành:** sửa `ads-business-rules.md` M-05 (câu "mã theo khe hay theo vị trí là câu hỏi 14") và `release.md` ở task này hay ở T-057?
   - Đề xuất của AI: `release.md` ở T-057 như ticket ghi. M-05 thêm một câu "lớp của thanh đáy: mã theo khe `banner`, `native_collap` (T-056)" ở task này nếu người đồng ý; plan hôm nay không đưa file đó vào mục 3.2.
4. **`status.py` "Sẵn sàng phát hành"** chưa liệt kê hai khóa khe sau task này. `docs/CLAUDE.md` nói trang đọc theo đúng luật của `release.sh`, nên plan không tự đổi.
   - Đề xuất của AI: đổi `status.py` cùng lúc với T-057 (ghi vào phạm vi của T-057, hay một ticket nhỏ của AI ngay sau T-057).
5. **Kiểu của `missing`:** plan đổi `List<AdPlacement>` thành `List<String>` (khóa của `adUnits`) theo đúng chữ ticket "missing liệt kê cả lớp". Phương án khác: giữ `missing: List<AdPlacement>`, thêm `missingSlots: List<AdFormat>`; ba câu khẳng định cũ giữ nguyên.
   - Đề xuất của AI: `List<String>`, vì đó đúng là chữ người phát hành phải ghi, và không code nào ngoài test đọc `missing`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
