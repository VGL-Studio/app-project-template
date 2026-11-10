---
ticket: T-099
title: Feature biết thẻ native của một bước có được phép không; vị trí chỉ hiện ở lần mở app đầu tiên
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 22.0M        # cỡ M, số gợi ý 22.0M của scripts/token-usage.py estimate (2026-11-10, trung vị 27 task M); không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-11-10
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-099: FEATURE BIẾT THẺ NATIVE CỦA MỘT BƯỚC CÓ ĐƯỢC PHÉP KHÔNG; VỊ TRÍ CHỈ HIỆN Ở LẦN MỞ APP ĐẦU TIÊN

> **Plan chỉ được duyệt sau khi người phụ trách sản phẩm (anh Nhật) xác nhận spec đã sửa** theo câu hỏi 1 ở mục 8. Đã đạt: spec `ads` sửa ở PR #446 (KB-20 tiêu chí 6 tới 9, câu hỏi 59), anh Nhật xác nhận ngày 2026-11-10 ("Xác nhận spec ads").

## 1. Tóm tắt

Hai phần nhỏ của `:core:ads`, theo issue #381 (camera-pose, PR #296 của app) và bài học L-163:

- Hàm công khai mới `rememberNativeStepAdAllowed(placement)`: thẻ native của bước (`NativeStepAd`) có được lấy quảng cáo lúc này không. Hàm đi qua đúng một cổng chung với `NativeStepAd` (đồng ý, công tắc tổng cùng gói bỏ quảng cáo, `is_show_<mã vị trí>`, mã đơn vị), để app có luồng giới thiệu chỉ giữ nút Tiếp khi thẻ được phép.
- Thuộc tính mới của khai báo vị trí `firstOpenOnly`, chỉ nhận ở hai định dạng của bước trong luồng (`native_step`, `native_fullscreen`) và không cho vị trí toàn app: công tắc của vị trí là `is_show_<mã vị trí>` cùng dấu "lần mở đầu sau khi cài" (dấu A-01 đã có, đọc một lần mỗi tiến trình). Từ lần mở thứ hai, mọi đường đọc công tắc (`AdsController.isEnabled`) thấy vị trí tắt: không nạp, không chiếm chỗ.

Kiểm chứng bằng test JVM: hàm thuần `placementSwitch`, `AdsController` thật với dấu lần mở đầu giả, pool native khi công tắc chưa biết, yêu cầu nạp khi dấu đọc xong, thẻ của bước, trang toàn trang và hàm mới khi từng cổng mở rồi đóng lại. Tài liệu `ads-guide.md`, `ads-business-rules.md` và Excel kế hoạch quảng cáo (`scripts/ads-plan.py`) ghi luật mới.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-10, PR #446). Bốn tiêu chí đề xuất ở mục 8, câu hỏi 1 ("đề xuất 1" tới "đề xuất 4") là KB-20 tiêu chí 6 tới 9 của spec; bảng này và mục 4 dùng số tiêu chí thật.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-20, tiêu chí 1 | Khi người vận hành bật vị trí native của luồng thì bước mà spec của app chọn có native cao 250 dp hoặc native toàn trang | Không đổi. `rememberNativeStepAdAllowed` trả `true` đúng khi thẻ được lấy quảng cáo |
| KB-20, tiêu chí 4 | Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống | Vị trí `firstOpenOnly` ở lần mở thứ hai coi như khóa tắt: thẻ không chiếm chỗ, hàm mới trả `false` |
| KB-20, tiêu chí 5 | Luồng giới thiệu dùng được khóa `is_show_native_onboarding` như các app AdsKit; luồng khác dùng khóa theo mã vị trí của nó | Không đổi tên khóa. Vị trí `firstOpenOnly` vẫn bật bằng `is_show_<mã vị trí>`; không có khóa mới |
| KB-20 tiêu chí 6 | Vị trí native của luồng khai báo chỉ ở lần mở đầu: chỉ tải và hiện trong lần mở đầu sau khi cài hay xóa dữ liệu; từ lần mở thứ hai không tải, không khoảng trống, dù khóa vẫn bật | `firstOpenOnly` trên khai báo, công tắc là khóa vị trí và dấu lần mở đầu (mục 3.1 B) |
| KB-20 tiêu chí 7 | Lần mở đầu kéo dài tới khi tiến trình của app kết thúc (người dùng đóng hẳn, hoặc hệ thống đóng app khi ở nền) | Dấu đọc một lần mỗi tiến trình (`FirstOpenMark.consume()`, có sẵn); mọi lần đọc công tắc trong tiến trình thấy cùng giá trị |
| KB-20 tiêu chí 8 | Trong lần mở đầu, rút đồng ý, mua gói bỏ quảng cáo hay tắt khóa thì thẻ biến mất ngay như vị trí thường | Công tắc vẫn gồm công tắc tổng, gói bỏ quảng cáo; cổng đồng ý đọc riêng như mọi thẻ |
| KB-20 tiêu chí 9 | App chờ thẻ trước khi cho đi tiếp thì chỉ chờ lúc thẻ được phép; không được phép hay thôi được phép giữa lúc chờ thì đi tiếp ngay | Ở dự án mẫu kiểm bằng giá trị của `rememberNativeStepAdAllowed` (test 8, 11). Phần chờ, nút Tiếp, thời gian chờ thuộc spec và code của app |
| KB-7, tiêu chí 1 | Khi người vận hành tắt công tắc tổng thì không còn tải hay hiện quảng cáo nào | Vị trí `firstOpenOnly` cũng tắt theo `is_show_ads`, giữa chừng cũng vậy |
| KB-12, tiêu chí 1 | Khi người dùng rút đồng ý thì không còn yêu cầu mới và quảng cáo đã tải không hiện | Hàm mới về `false` ngay khi rút đồng ý; đồng ý lại thì về `true` |
| KB-30, tiêu chí 1, 3 | Đã mua gói bỏ quảng cáo thì không có quảng cáo; đổi giữa lúc dùng thì quảng cáo biến mất ngay | Hàm mới và công tắc `firstOpenOnly` về `false` ngay khi `AdFreeStatus.adFree` bật |
| KB-13, tiêu chí 1, 2 | Lần mở đầu sau khi cài dùng khóa lần đầu; lần mở thứ hai trở đi dùng khóa thường | Dấu lần mở đầu (A-01) không đổi cách đọc, dùng chung cho app open lúc mở app và vị trí `firstOpenOnly`. Test của T-024 giữ nguyên |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; `docs/ops/design-workflow.md`).

ADR liên quan: ADR-0045 (mục 4.6 tới 4.11: vị trí theo mô hình chung, không thêm hàm hay cờ mang tên tính năng của một app; mọi thay đổi `:core:ads` là rủi ro cao), ADR-0046 (khóa không đổi; mô tả khóa suy ra sinh từ code), ADR-0019 (log), ADR-0031 (mục 3.5), ADR-0044 (tài liệu quy tắc không ghi mã ticket hay bài học).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo bản đã chạy ở app (PR #296 của camera-pose), chỉnh cho dự án mẫu: phạm vi định dạng hẹp lại (phương án B của câu hỏi 3), một nguồn cho thuộc tính (`declaration.firstOpenOnly`), và dự án mẫu không có vị trí nào `firstOpenOnly` (không thêm vị trí), nên test của controller dùng vị trí xem thử và một điểm nối test.

**A. Cổng chung của thẻ bước.** Trong `NativeFlowStep.kt`:

- Thêm hàm `@Composable internal fun AdsController.rememberStepAllowed(placement: AdPlacement): Boolean = rememberStepAllowed(placement, canRequestAds, ::isEnabled) { unitId(it) }`. Ba chỗ đang gọi `rememberStepAllowed(placement, ads.canRequestAds, ads::isEnabled) { ads.unitId(it) }` (`NativeStepAd`, `NativeFullScreenStep`, `rememberNativeFullScreenStepReady`) đổi sang `ads.rememberStepAllowed(placement)`. Hàm bốn tham số `rememberStepAllowed(placement, canRequestAds, enabled, unitId)` có sẵn (cổng KB-1, MC-03: `nativeSlotAllowed(canRequest, on, unit)`) giữ nguyên, các test của nó giữ nguyên.
- Thêm hàm công khai, ngay sau `NativeStepAd`:

  ```kotlin
  @Composable
  fun rememberNativeStepAdAllowed(placement: AdPlacement): Boolean {
      requireStepFormat(placement.format)
      val ads = LocalAdsController.current ?: return false
      return ads.rememberStepAllowed(placement)
  }
  ```

  KDoc (tiếng Anh): trả `true` khi thẻ của `placement` được lấy quảng cáo lúc này, cùng cổng mà `NativeStepAd` đọc trước khi chiếm chỗ (đồng ý, `is_show_<placement>` cùng công tắc tổng và gói bỏ quảng cáo, mã đơn vị, và dấu lần mở đầu với vị trí `firstOpenOnly`); `false` khi không có `AdsController` (test, preview), ở các khung hình đầu khi công tắc chưa biết, và ngay khi quảng cáo thôi được phép. "Đã có quảng cáo" vẫn lấy từ `onAdAvailabilityChange` của `NativeStepAd`. Vị trí khác định dạng `native_step` ném `IllegalArgumentException`. Không ghi số liệu, không lấy quảng cáo.
- Không thêm hàm tương tự cho trang toàn trang: `rememberNativeFullScreenStepReady` đã có, và nay có test với controller thật (test 15).

**B. Thuộc tính `firstOpenOnly`, một nguồn: `declaration.firstOpenOnly`.**

- `AdPlacement.kt`: tham số cuối mới của enum `firstOpenOnly: Boolean = false`, **không có `val`** (chỉ là tham số hàm dựng, truyền vào `declaration`; enum không có thuộc tính `firstOpenOnly`, nên code chỉ đọc được `placement.declaration.firstOpenOnly`) (KDoc: chỉ bật ở lần mở đầu sau khi cài hay sau khi xóa dữ liệu, A-01, OB-03; chỉ cho `native_step`, `native_fullscreen`, không cho vị trí `appWide`). `AdPlacementDeclaration` có trường `val firstOpenOnly: Boolean = false` và trong `init` hai lệnh:
  - `require(!firstOpenOnly || format == AdFormat.NATIVE_STEP || format == AdFormat.NATIVE_FULL_SCREEN) { "Placement $code: firstOpenOnly is for flow step formats only" }`
  - `require(!firstOpenOnly || !appWide) { "Placement $code: firstOpenOnly is not for an app-wide placement" }` (`native_combo` là `native_fullscreen` toàn app).

  Không thêm dòng vị trí nào.
- `StartupAdChoice.kt`, `placementSwitch(placement, flags, firstOpen, firstOpenOnly: Boolean = placement.declaration.firstOpenOnly)`: tham số để test hàm thuần khai báo được mà không cần vị trí thật; mặc định đọc khai báo. Nhánh mới đặt ngay trong `if (!placement.appWide)`:

  ```kotlin
  if (!placement.appWide) {
      if (!firstOpenOnly) return flags.placementEnabled(placement)
      return combine(flags.placementEnabled(placement), firstOpen) { on, first -> on && first }.distinctUntilChanged()
  }
  ```

  Vị trí `firstOpenOnly` chờ dấu lần mở đầu như vị trí toàn app (chưa đọc xong thì chưa có giá trị); vị trí khác không chờ, như trước. KDoc của hàm thêm một vế cho nhánh này.
- `AdsController.kt`, cạnh `isEnabled`:
  - Điểm nối test `@VisibleForTesting internal var firstOpenOnlyOf: (AdPlacement) -> Boolean = { it.declaration.firstOpenOnly }`. KDoc: code chính không bao giờ đặt biến này; test đặt nó (ngay sau `testAdsController(...)`, trước khi dựng giao diện hay gọi `start`) để coi một vị trí xem thử như khai báo `firstOpenOnly`, vì dự án mẫu không có vị trí nào như vậy.
  - `isEnabled(placement) = placementSwitch(placement, flags, firstOpen, firstOpenOnly = firstOpenOnlyOf(placement))`. Biến đọc lúc gọi `isEnabled`; `nativePools`, `nativeSources` là `by lazy`, `collectPreloadTriggers` chạy trong `start()`, nên đặt biến trước các lần đó là đủ (đã kiểm trên nhánh chính: hàm dựng `AdsController` không gọi `isEnabled`; phiên kiểm lại bằng đối chứng của test 4b).
  - Chú thích của `firstOpenResult` đổi vế "the first time a switch of an app-wide placement is read" thành "...of an app-wide or first-open-only placement is read"; chú thích của `isEnabled` thêm vế `firstOpenOnly`. Dấu vẫn đọc một lần mỗi tiến trình (`by lazy` + `async`), không đổi.
- `AdsSettings.kt`, `placementEnabled(declaration)`: mô tả khóa thêm `" First open since install only."` khi `declaration.firstOpenOnly`, cùng kiểu với `" Staging ad gallery only."`. Dự án mẫu không có vị trí `firstOpenOnly`, nên `docs/remote-config-keys.md` và `docs/remote-config-template.json` không đổi (bước kiểm ở mục 4); app nào khai báo thì bảng khóa của app sinh lại có câu này.
- `scripts/ads-plan.py`, hàm đọc `AdPlacement.kt`: thêm `"firstOpenOnly"` vào danh sách `extras` (cạnh `ignoreInterval`, `ctaLarge`, `keepSpaceWhenPoolEmpty`), để cột ghi chú của sheet vị trí trong Excel kế hoạch cho nhóm kinh doanh thấy vị trí chỉ ở lần mở đầu (giới hạn của `--keep`: câu hỏi 6).

**C. Mọi đường dẫn tới lời gọi đều qua một cổng** (`docs/CLAUDE.md`, Khi viết plan: cổng mở rồi đóng lại). Mọi đường lấy hay nạp quảng cáo của một vị trí đọc công tắc qua `AdsController.isEnabled` (L-072, đã có), nên `firstOpenOnly` chỉ cần ở `placementSwitch`. Với phạm vi B, các đường của hai định dạng được khai báo:

| Đường | Đọc cổng ở đâu | Test |
| --- | --- | --- |
| `NativeStepAd` lấy quảng cáo từ pool `native_step` | `ads.rememberStepAllowed` rồi `NativeAdSlotGate(allowed)`: gate đóng thì hủy thu thập, trả quảng cáo (L-061) | Test 9, 10, 11 (controller thật, đếm `af_native_ad_eligible` mà gate ghi khi cổng mở) |
| `rememberNativeStepAdAllowed` | `ads.rememberStepAllowed` | Test 7, 8, 11 |
| `rememberNativeFullScreenStepReady` | `ads.rememberStepAllowed` rồi `rememberStepReady` | Test 15 (controller thật; có trường hợp `native_fullscreen` khai báo `firstOpenOnly` ở lần mở sau và lần mở đầu, mới) |
| `NativeFullScreenStep` lấy quảng cáo nạp sẵn | `ads.rememberStepAllowed` rồi `NativeFullScreenStepGate(allowed)` | Có sẵn, controller thật: `NativeFullScreenStepRecreateTest.recreate_placementOff_noPlaceholder_noTake`, `recreate_consentClosed_notTaken` |
| Pool `native_step`: đóng, mở lại (`NativeAdPools.update`) | `enabled = ::isEnabled`, `combine` mọi công tắc vị trí có pool | Test 6 |
| Pool `native_step`: nạp sẵn (`NativePoolPreloadSlot`, chỉ vị trí `enabledNow == true`) | như trên | Test 5 (cơ chế chung của `NativeAdPools`, thử bằng pool `native_medium`) |
| Slot nạp sẵn `native_fullscreen` (`enabled = { enabledNow[placement] == true }` của kế hoạch nạp) và yêu cầu nạp khi vị trí bật (`collectPreloadTriggers`) | `combine(isEnabled(...))` của vị trí toàn màn hình, có pool và kho | Test 14 (controller thật, `start()`): 14a cho vị trí `native_step`, 14b cho vị trí `native_fullscreen` |
| Hiện, lấy quảng cáo khi đã nạp (`takePreloadedNativeFullScreen` qua `stepFullScreenTake`, đồng ý) | Cổng của trang ở trên | Như dòng `NativeFullScreenStep`; vị trí `native_fullscreen` khai báo `firstOpenOnly`: test 15c, 15d |

Định dạng khác không khai báo được `firstOpenOnly` (test 1), nên đường của chúng không đổi.

Cổng có thể mở rồi đóng lại: đồng ý (rút rồi cho lại), `is_show_<mã vị trí>`, `is_show_ads`, `AdFreeStatus.adFree`. Dấu lần mở đầu không đổi trong một tiến trình (chỉ từ "chưa biết" sang "biết"), nên không có đường "mở rồi đóng" riêng cho nó. Bước đóng lại của từng cổng: test 2 (khóa, công tắc tổng), 3b (gói bỏ quảng cáo), 8 (cả bốn), 9 và 15 (đồng ý), 10 (đồng ý), 11 (đồng ý trong lần mở đầu).

**D. Lúc công tắc chưa biết.** Công tắc của vị trí `firstOpenOnly` chưa có giá trị tới khi dấu lần mở đầu đọc xong. Dấu được đọc trong `start()` (luồng `combine(flags.global, firstOpen)` của quảng cáo lúc mở app), đọc DataStore mất vài mili giây, thường xong trước khi SDK sẵn sàng (SDK cần cổng đồng ý và khởi tạo). Trong lúc đó:

- `collectPreloadTriggers` hôm nay đã chờ dấu, vì `combine` của nó có vị trí toàn app (`open_app`, `open_app_inter`). Không đổi.
- `NativeAdPools` hôm nay không chờ dấu (không có vị trí toàn app nào có pool). Khi app có một vị trí `firstOpenOnly` định dạng `native_step`, `combine` công tắc của mọi vị trí có pool chờ cả dấu, nên **mọi pool** (`native_step`, `native_medium`, `native_in_collection`) chưa vào lượt nạp sẵn tới khi dấu đọc xong: khi đóng mở pool, vị trí chưa biết tính là bật (`enabledNow[it] != false`), nên pool vẫn mở cho ô đang trên màn; khi nạp sẵn, vị trí chưa biết tính là tắt (`enabledNow[it] == true`). Dấu đọc xong thì `collectPreloadTriggers` xin nạp `PLACEMENT_ENABLED` nếu có vị trí vừa thành bật, và pool nạp theo kế hoạch.
- Vị trí `firstOpenOnly` định dạng `native_fullscreen` không có pool: slot nạp sẵn của nó đọc `enabledNow` của `collectPreloadTriggers`, nên chỉ nạp khi dấu đã đọc và là lần mở đầu.

Test 5, 6 khóa hai cách tính "chưa biết" của `NativeAdPools`; test 14 khóa yêu cầu nạp sau khi dấu đọc xong. Hàm mới và thẻ trả `false` ở các khung hình đầu (`collectAsStateWithLifecycle(initialValue = false)`, có sẵn): `ads-guide.md` ghi để app không đọc giá trị một lần ở khung hình đầu.

**E. Tài liệu** (tài liệu quy tắc, không ghi mã ticket hay bài học, ADR-0044; `scripts/history-refs.py` kiểm):

- `docs/ops/ads-business-rules.md`:
  - MC-06: thêm câu "Vị trí của luồng khai báo được là chỉ ở lần mở đầu (OB-03)".
  - A-01: thêm vế "dấu này dùng chung cho vị trí chỉ ở lần mở đầu (OB-03)".
  - Bảng 5.11, dòng mới OB-03 (bốn cột `#`, Luật, Chủ, AdsKit): "**Vị trí chỉ ở lần mở đầu.** Spec của app khai báo được vị trí native của luồng (`native_step`, `native_fullscreen`, không toàn app) là chỉ ở lần mở đầu sau khi cài (hay sau khi xóa dữ liệu), thuộc tính `firstOpenOnly`; định dạng khác không khai báo được. Công tắc của vị trí là `is_show_<mã vị trí>` (cùng công tắc tổng và cổng không hiện quảng cáo) và dấu lần mở đầu của A-01, đọc một lần mỗi tiến trình: lần mở đầu kéo dài tới khi tiến trình của app kết thúc (người dùng đóng hẳn, hoặc hệ thống đóng app khi ở nền). Từ lần mở thứ hai, vị trí coi như tắt ở mọi đường: không nạp, không chiếm chỗ, không ghi cơ hội. Đọc dấu lỗi thì coi là lần sau; ghi dấu lỗi thì lần mở sau có thể lại là lần đầu, như A-01. Không có khóa riêng. Khi app có vị trí này định dạng `native_step`, pool native nạp sẵn sau khi dấu đọc xong | app khai báo, ads làm | Không có (luật của ta)". Nội dung cuối theo trả lời câu hỏi 2 tới 4.
  - Bảng 5.11, dòng mới OB-04: "App biết thẻ của bước có được lấy quảng cáo lúc này không qua một hàm của `:core:ads`, cùng cổng mà thẻ đọc (đồng ý, công tắc tổng và cổng không hiện quảng cáo, `is_show_<mã vị trí>`, mã đơn vị, OB-03). App chờ thẻ trước khi cho đi tiếp thì chỉ chờ khi hàm này cho phép, và cho đi tiếp ngay khi nó thôi cho phép | app, ads | Không có (luật của ta)".
- `docs/ops/ads-guide.md`:
  - Mục 3, bảng định dạng, dòng "Native ở một bước của luồng" (dòng 48): thêm vế "app biết thẻ có được phép không bằng `rememberNativeStepAdAllowed(placement)`; vị trí chỉ ở lần mở đầu khai báo `firstOpenOnly = true`".
  - Mục 4, gạch "Thẻ ở bước của luồng" (dòng 77): thêm câu về `rememberNativeStepAdAllowed(placement)` (cùng cổng với thẻ; `false` khi không có `AdsController`, ở khung hình đầu khi công tắc chưa biết, và ngay khi quảng cáo thôi được phép; "đã có quảng cáo" lấy từ `onAdAvailabilityChange`; ví dụ giữ nút Tiếp của bước đầu chỉ khi hàm trả `true` và chưa có quảng cáo, có giới hạn thời gian do spec của app đặt).
  - Mục 5, bước 1, gạch "Thuộc tính vị trí" (dòng 124): thêm `firstOpenOnly = true` (chỉ `native_step`, `native_fullscreen`, không cho vị trí `appWide`): chỉ bật ở lần mở đầu sau khi cài, công tắc là `is_show_<mã vị trí>` cùng dấu lần mở đầu, lần mở sau đọc là tắt nên không nạp, không chiếm chỗ; ví dụ thẻ luồng giới thiệu `native_onboarding`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Tham số `firstOpenOnly` của enum và trường của `AdPlacementDeclaration`, hai `require` (định dạng của luồng, không `appWide`), truyền vào `declaration` (mục 3.1 B). Không thêm vị trí |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/StartupAdChoice.kt` | `placementSwitch` có tham số `firstOpenOnly = placement.declaration.firstOpenOnly` và nhánh `combine` với dấu lần mở đầu; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Điểm nối `firstOpenOnlyOf` cạnh `isEnabled`; `isEnabled` truyền `firstOpenOnlyOf(placement)`; chú thích của `firstOpenResult`, `isEnabled` (dự phòng: `isEnabled` bọc `flow { emitAll(...) }`, mục 3.4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `AdsController.rememberStepAllowed(placement)` dùng chung cho ba chỗ gọi cũ; hàm công khai `rememberNativeStepAdAllowed(placement)`; KDoc của `NativeStepAd` nhắc hàm mới |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Mô tả `is_show_<code>` thêm " First open since install only." khi khai báo `firstOpenOnly` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` | Test 1 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/StartupAdChoiceTest.kt` | Test 2, 3 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Test 12 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolsTest.kt` | Hàm `newPools` thêm tham số `enabled: (AdPlacement) -> Flow<Boolean> = { switchOf(it) }` (commit test, bước 3); test 5, 6 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerFirstOpenOnlyTest.kt` | Test 4, 14a, 14b (Robolectric, `AdsController` thật qua `testAdsController`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStepAdAllowedTest.kt` | Test 7 tới 11 và 15 (gồm 15c, 15d; Robolectric, `createComposeRule`, khung `w411dp-h891dp-xxhdpi`, không có ảnh chụp) |
| Sửa | `scripts/ads-plan.py` | `extras` thêm `firstOpenOnly` |
| Sửa | `scripts/tests/run.sh` | Khối `ads-plan`: dòng `HOME_GRID` của `AdPlacement.kt` giả thêm `, firstOpenOnly = true`; test 13 |
| Sửa | `docs/ops/ads-business-rules.md` | MC-06, A-01, OB-03 và OB-04 mới (mục 3.1 E) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3 dòng 48, mục 4 dòng 77, mục 5 dòng 124 (mục 3.1 E) |

Nơi đọc các file bị đổi (`grep -rn` trong `scripts/`, `.github/`, `*/src/test`):

- `AdPlacement.kt`: `scripts/release.sh` đọc dòng khai báo vị trí, không đổi vì không thêm dòng nào và thuộc tính mới nằm sau định dạng trên cùng dòng (`AdPlacementSourceTest` kiểm); `scripts/ads-plan.py` (có trong bảng; dòng `HOME_GRID` giả của test là định dạng `NATIVE_IN_COLLECTION`, script không kiểm định dạng nên không vướng `require`); `scripts/status.py` mục 6 đọc khóa vị trí theo luật của `release.sh`, không đổi.
- `AdsSettings.kt`: bảng khóa sinh `docs/remote-config-keys.md`, `docs/remote-config-template.json` từ mọi vị trí của `AdPlacement`; không vị trí nào `firstOpenOnly` nên hai file không đổi, không đưa vào bảng. Lệnh kiểm ở mục 4; đổi thì dừng (mục 7).
- `ads-business-rules.md`, `ads-guide.md`: `scripts/history-refs.py` quét (không có tệp nào trong danh sách cho phép, nên chữ mới không được ghi mã ticket, bài học hay phiên); không script nào khác đọc nội dung.
- `docs/ops/ads-business-rules.md` được nhắc trong chú thích của các test sau, chỉ để dẫn mã luật đã có (mục, mã như S-14, P-06, N-05); plan chỉ thêm OB-03, OB-04 và vế mới của MC-06, A-01, không đổi số mục hay mã luật nào, nên không test nào bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.
- `scripts/tests/run.sh` được chạy hay nhắc tới bởi `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml` (chạy cả tệp), `scripts/main-guard-verified.sh` (liệt kê nó trong danh sách tệp coi là code), `scripts/merge-if-green.sh` (nhắc trong chú thích và thông báo) và `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` (nhắc trong chú thích). Plan chỉ thêm một dòng vào khối `ads-plan` và một thuộc tính vào dòng giả `HOME_GRID`, không đổi cách chạy tệp, tên tệp hay đầu ra mà các nơi đó đọc, nên không nơi nào bị ảnh hưởng.
- Ảnh chuẩn screenshot: không đổi; test mới không chụp ảnh. `NativeFlowStepScreenshotTest` không gọi đường nào bị đổi ngoài cách gọi cổng.

Không đổi trong PR của task: `docs/features/ads/spec.md` (sửa riêng trước khi duyệt, câu hỏi 1), `docs/ops/ads-plan.md` (câu hỏi 6), ticket T-099 (đổi trạng thái theo quy trình của phiên).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Thêm một thuộc tính khai báo vị trí (`firstOpenOnly`, chỉ hai định dạng của luồng) đổi điều kiện hiện của vị trí khai báo nó (điểm chặn 1); dự án mẫu không có vị trí nào khai báo, nên hành vi quảng cáo của dự án mẫu không đổi.
- Sự kiện phân tích: Không chạm. Test 9, 10, 11 chỉ đếm `af_native_ad_eligible` đã có.
- File R2: Không chạm.
- Dữ liệu cục bộ: dùng lại khóa DataStore `ads_first_open_done` của `FirstOpenMark`, không thêm khóa, không chuyển đổi dữ liệu.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng thư viện đã có trên classpath test của `:core:ads`: Compose UI test (`createComposeRule`), Robolectric, Turbine (`app.cash.turbine.test`, đã dùng trong `StartupAdChoiceTest`), DataStore Preferences (`preferencesOf`, đã dùng trong `FirstOpenMarkTest`), `kotlinx.coroutines.CompletableDeferred`, `RecordingAdFunnel` của `:core:analytics`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test 4, 11, 14, 15c dùng điểm nối mới `firstOpenOnlyOf`. Biến đọc lúc gọi `isEnabled`; nếu một đường gọi `isEnabled` của vị trí trước khi test đặt biến (ví dụ một luồng thu thập bắt đầu trong hàm dựng `AdsController`, hay một lần sửa sau làm `nativePools` không còn `by lazy`), đường đó thấy vị trí như không khai báo.
- **Điều kiện chuyển:** test 4, 11, 14 hay 15 vẫn đỏ sau bước viết code (bước 4), sau 2 lần sửa, và log hay debug cho thấy `isEnabled` của vị trí được gọi trước khi đặt biến.
- **Cách làm thay:** vẫn đi đường thật qua `AdsController.isEnabled`, chỉ dời lúc đọc biến từ lúc gọi sang lúc thu thập: `isEnabled(placement) = flow { emitAll(placementSwitch(placement, flags, firstOpen, firstOpenOnlyOf(placement))) }`. Test, câu khẳng định và điểm nối giữ nguyên; không file mới. Một luồng đã bắt đầu thu thập trước khi test đặt biến mà vẫn đỏ thì **dừng task** (không có cách nào khác không qua Hilt: tham số mới của hàm dựng cần binding cho mọi app).

### 3.5. Hành vi đã có bị đổi

Chạy `python3 scripts/existing-tests.py docs/features/ads/plans/T-099-the-native-cua-buoc-va-lan-mo-dau.md` (2026-11-10, sau khi sửa theo rà soát): 724 test dùng tên của năm tệp nguồn; 12 ở nhóm "Cần xem trước", 419 ở nhóm "Nên xem" (tên plan nêu nhiều hơn lần chạy đầu); sau khi ghi các test đó vào mục này, nhóm "Cần xem trước" còn 0. Mười hai test "Cần xem trước" của lần chạy này (`NativeAdSlotTest.slotAllowed_trueWhenAllGiven`; `NativeEligibleReportTest.fullScreenStep_belowMinHeight_noEligible`, `fullScreenStep_phoneLandscape_eligible`; `NativeFlowStepTest.fullScreenStep_belowMinHeight_takesNothing`, `fullScreenStep_phoneLandscape_takesAd`, `fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse`, `nextButton_samePlace_notCovered_withCard`, `nextButton_samePlace_notCovered_withFullPage`, `stepAd_loading_keeps250dp`, `stepAd_stepChange_showsAnotherAdOfThePool`, `stepAd_unavailable_takesNoSpace`, `stepCard_doesNotSuppress`) khớp vì tên `unitId`, `NativeStepAdHost`, `NativeFullScreenStepGate`: chúng truyền `allowed` bằng tay vào phần sau cổng (bố cục, chiều cao, pool, chặn quảng cáo tự hiện) hay kiểm hàm thuần `nativeSlotAllowed`; plan không đổi phần sau cổng, chữ ký hay hàm thuần đó: giữ. Mười bốn test "Cần xem trước" của lần chạy đầu (`AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`; `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `bottomBannerSlot_reportsAnchoredHeight`; `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`; `InListAdsTest.lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6`; `NativeAdCellTest.mediumSlot_recordsShownOnce_notReadyOnDispose`, `otherFormat_isRejected`; `NativeAdSlotTest.withoutAdsController_takesNoSpace`; `NativeFullScreenOverlayTest.smallScreen_countdownAndCtaInside`; `RewardedActionTest.longContent_small_buttonsInsideWindow`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `realController_adFreeWhileDialogOpen_watchRunsAction`, `watchButton_is56dpHigh_fullCardWidth`) khớp chỉ vì tên chung (`start`, `AdPlacement`, `AdsController`); chúng đo bố cục của banner, lớp thu gọn, danh sách, rewarded và lớp phủ toàn màn hình, không đường nào plan đổi (không thêm vị trí, không đổi bố cục, công tắc của mọi vị trí hiện có giữ nguyên): giữ. Nhóm "Nên xem" gồm các test liệt kê mọi vị trí hay đọc `isEnabled` của mọi vị trí; các test liên quan nhất ở bảng dưới. Plan không đổi hành vi nào mà test có sẵn khẳng định; mọi câu khẳng định giữ nguyên:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Vị trí không toàn app theo `is_show_ads && is_show_<code>`, không chờ dấu lần mở đầu | Giữ đúng như vậy khi `firstOpenOnly` là `false` (mặc định, mọi vị trí của dự án mẫu) | `StartupAdChoiceTest.placementSwitch_otherPlacements_unchanged`, `placementSwitch_adFreeMidSession_emitsFalse` | Giữ |
| Vị trí toàn app đọc khóa của lần mở (A-01, A-02, A-12, combo) | Không đổi; `firstOpenOnly` cùng `appWide` bị từ chối khi khai báo | `StartupAdChoiceTest.placementSwitch_followsKeysAndFirstOpen`, `placementSwitch_waitsForFirstOpen`, `placementSwitch_resume_followsResumeKeys`, `placementSwitch_nativeCombo_followsNativeAfterInterKey`, `placementSwitch_galleryAppOpen_followsOwnKey` | Giữ |
| Dấu lần mở đầu đọc và ghi một lần mỗi tiến trình | Không đổi (vị trí `firstOpenOnly` dùng chung `firstOpen`) | `FirstOpenMarkTest` (cả bốn test), các test app open của `AdsControllerStartupInterComboTest` | Giữ |
| Hành vi của thẻ bước và trang toàn trang khi `allowed` đổi | Không đổi. Các test này truyền `allowed` bằng tay vào `NativeStepAdHost`, `NativeFullScreenStepGate`, `rememberStepReady`, không qua cổng; chúng khóa phần sau cổng, không khóa cách đọc cổng | `NativeFlowStepTest.stepAd_drawsOnlyWhenAllowed`, `stepAd_allowedTurnsFalse_hidesAndReportsFalse`, `fullScreenStep_drawsOnlyWhenAllowed`, `fullScreenStep_allowedTurnsFalse_releasesAndUnsuppresses`, `stepReady_falseWhenDisallowed`, `stepReady_allowedTurnsFalse_midFlow` | Giữ |
| Cổng bốn tham số của bước (khóa của đúng vị trí, mã đơn vị) | Không đổi; hàm mới `ads.rememberStepAllowed(placement)` gọi đúng hàm này với `canRequestAds`, `isEnabled`, `unitId` của controller | `NativeFlowStepTest.stepAllowed_readsOnlyItsPlacementSwitch`, `stepAllowed_falseWithoutUnitId`, `fullScreenStep_withoutAdsController_drawsNothing_notReady` | Giữ |
| Trang toàn trang qua controller thật: khóa tắt, cổng đồng ý đóng thì không lấy quảng cáo | Đọc cổng qua `ads.rememberStepAllowed(placement)`, cùng đối số như cũ | `NativeFullScreenStepRecreateTest.recreate_placementOff_noPlaceholder_noTake`, `recreate_consentClosed_notTaken` (và các test dựng lại khác của lớp) | Giữ |
| Khai báo vị trí hợp lệ với các thuộc tính hiện có | Thêm trường `firstOpenOnly` mặc định `false`; khai báo cũ không đổi | `AdPlacementTest` (mọi test khai báo), `AdPlacementSourceTest` (dòng đọc được bằng luật của `release.sh`) | Giữ |
| Mô tả khóa `is_show_<code>` | Chỉ thêm câu khi `firstOpenOnly`; mọi khóa của dự án mẫu giữ nguyên chữ | `AdsSettingsTest` (mô tả khóa), `RemoteConfigKeysTest` của `:app` (so với `docs/remote-config-keys.md`) | Giữ |
| Pool native theo công tắc của từng vị trí | Không đổi code; test mới khóa hành vi khi một công tắc chưa có giá trị | `NativeAdPoolsTest` (mọi test có sẵn; `newPools` thêm tham số có mặc định, gọi cũ không đổi) | Giữ |
| Sheet vị trí của Excel kế hoạch | Cột ghi chú thêm `firstOpenOnly` khi vị trí khai báo | Khối `ads-plan` của `scripts/tests/run.sh`: "the KDoc of a placement becomes its note" (dòng `HOME_BOTTOM`, không đổi), "placements of the code, in order" (không thêm dòng vị trí giả nào) | Giữ |
| Công tắc của mọi vị trí tắt theo gói bỏ quảng cáo; mọi vị trí có khóa `is_show_<code>` trong bảng khóa và bật ở staging | Không đổi (không vị trí nào của dự án mẫu khai báo `firstOpenOnly`) | `AdFreeGateTest.adFreeMidSession_everyPlacementSwitchOff`, `adFree_consentSdkAndAppsFlyerUnchanged`; `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed`, `everyPlacementShowKey_isInTableOff`; `StagingRemoteValuesModuleTest.everyAdPlacement_isShown`; `AdPreloadPlanTest.fromPlacements_*` | Giữ |

## 4. Kiểm thử

Mỗi test viết trước code (bước 3), chạy và thất bại đúng lý do (hàm, thuộc tính hay tham số chưa có thì lỗi biên dịch; với câu khẳng định đã biên dịch được, thất bại ở đúng câu ghi trong dòng), rồi mới viết code (bước 4). Các test tầng Robolectric dùng `testAdsController(...)` của `AdsControllerFixtures.kt` và các điểm nối test đã có `openConsentGateForTest()`, `closeConsentGateForTest()`, `reopenConsentGateForTest()`, `putPreloadedNativeFullScreenForTest(...)`: điểm nối mở và đóng cổng đồng ý thật (`gate.onPrivacyFormClosed`), bỏ hộp thoại UMP và SDK, hay đặt quảng cáo vào slot như một lần nạp; câu khẳng định đọc giá trị trả về của hàm Compose, sự kiện trong `RecordingAdFunnel` hay dòng log trong `RecordingAppLogger`. AI đã chạy thử các điểm nối này bằng test tạm không commit (mục 4.1).

**Giới hạn khi không có vị trí thật:** dự án mẫu không có vị trí nào khai báo `firstOpenOnly`. Test của controller (4, 11, 14, 15c) dùng điểm nối `firstOpenOnlyOf` thay cho lần đọc `declaration.firstOpenOnly` trong `isEnabled`; test 1 và 12 kiểm `require` và mô tả khóa trên `AdPlacementDeclaration`. Phần không test tự động được: tham số của enum được truyền đúng vào `declaration`, và mặc định của `firstOpenOnlyOf` đọc đúng `declaration.firstOpenOnly` (một dòng mỗi chỗ; người duyệt PR của task đọc). App đầu tiên khai báo vị trí như vậy có test của app (như `AdPlacementAppTest` ở camera-pose).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Khai báo `firstOpenOnly` chỉ ở định dạng của luồng, không toàn app (OB-03) | Unit test | **1.** `AdPlacementTest.firstOpenOnly_flowStepFormatsOnly_notAppWide`: `AdPlacementDeclaration("x", AdFormat.NATIVE_STEP, firstOpenOnly = true)` và `AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, firstOpenOnly = true)` được nhận, `firstOpenOnly` là `true`; mọi định dạng khác của `AdFormat.entries` với `firstOpenOnly = true` ném `IllegalArgumentException`; `AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, appWide = true, firstOpenOnly = true)` ném. Đối chứng có nghĩa: `AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, appWide = true)` (không `firstOpenOnly`) được nhận, nên lần ném trên là do `firstOpenOnly` cùng `appWide`, không do định dạng; và mọi định dạng khác không `firstOpenOnly` được nhận (khi định dạng đó không cần thuộc tính khác) |
| KB-20 tiêu chí 6: chỉ lần mở đầu, theo khóa của vị trí; cổng đóng lại | Unit test | **2.** `StartupAdChoiceTest.placementSwitch_firstOpenOnly_followsKeyAndFirstOpen` (vị trí `GALLERY_NATIVE_STEP`, `firstOpenOnly = true`): khóa `is_show_gallery_native_step` bật, `flowOf(true)` thì `true`; `flowOf(false)` thì `false`. Khóa tắt, `flowOf(true)` thì `false`. Đối chứng (L-055): cùng vị trí `firstOpenOnly = false`, `flowOf(false)` thì `true` (vị trí thường vẫn bật ở lần mở sau). Lần mở đầu, cổng mở rồi đóng lại rồi mở (Turbine): khóa bật thì `true`, tắt khóa thì `false`, bật lại thì `true`; tắt `is_show_ads` thì `false` (KB-7) |
| Chờ dấu lần mở đầu; tiêu chí 8 (gói bỏ quảng cáo giữa chừng, KB-30) | Unit test | **3a.** `placementSwitch_firstOpenOnly_waitsForFirstOpen`: khóa bật, `firstOpen = neverRead`, `firstOpenOnly = true`: không có giá trị sau `advanceTimeBy(1_000)` (`expectNoEvents`). Đối chứng: cùng vị trí `firstOpenOnly = false` với `neverRead` có giá trị `true` ngay. **3b.** `placementSwitch_firstOpenOnly_adFree_turnsOff`: `DefaultAdsFlags(settings, adFreeStatus(adFree))`, khóa bật, `flowOf(true)`: `true`, rồi `adFree.value = true` thì `false`, rồi `false` thì `true` lại. Đối chứng: `flowOf(false)` thì `false` dù `adFree` tắt |
| KB-20 tiêu chí 6 qua controller thật; tiêu chí 7 (dấu đọc một lần mỗi tiến trình) | Unit test (Robolectric) | **4.** `AdsControllerFirstOpenOnlyTest` (lớp `@RunWith(AndroidJUnit4::class)`, đọc bằng `runBlocking { controller.isEnabled(...).first() }` như lần chạy thử ở mục 4.1; `FakeRemoteSettings` bật `is_show_gallery_native_step` và `is_show_gallery_native_medium`; kho DataStore giả đếm số lần đọc `data`, như `MemoryDataStore` của `FirstOpenMarkTest`): **4a.** `laterOpen_firstOpenOnlyPlacementOff_otherPlacementOn`: kho có sẵn `ads_first_open_done = true`, `controller.firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP }` ngay sau `testAdsController(firstOpenMark = FirstOpenMark(kho, logger), flags = ...)`: `isEnabled(GALLERY_NATIVE_STEP).first()` là `false`; `isEnabled(GALLERY_NATIVE_MEDIUM).first()` là `true` (đối chứng: vị trí thường). **4b.** `firstOpen_firstOpenOnlyPlacementOn`: kho rỗng, cùng điểm nối: `isEnabled(GALLERY_NATIVE_STEP).first()` là `true`. Đối chứng của điểm nối: không đặt `firstOpenOnlyOf`, kho có dấu: `true`. **4c.** `firstOpenLastsForProcess_nextProcessIsLater` (tiêu chí 7): kho rỗng, ghi được (giữ dữ liệu trong bộ nhớ như `MemoryDataStore` của `FirstOpenMarkTest`) và đếm số lần đọc `data`. Controller A (`firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP }`) đọc `isEnabled(GALLERY_NATIVE_STEP).first()` hai lần: cả hai `true` (lần đọc thứ hai không biến thành "lần sau" dù dấu đã ghi); kho chỉ bị đọc đúng 1 lần, và sau đó kho đã có `ads_first_open_done = true`. "Tiến trình kết thúc" là dựng controller B trên cùng kho, cùng điểm nối: `isEnabled(GALLERY_NATIVE_STEP).first()` là `false`, kho bị đọc thêm đúng 1 lần (tổng 2). Đã đo trên nhánh chính phần đếm: hai lượt đọc của một controller chỉ đọc kho 1 lần |
| Pool khi công tắc chưa biết (mục 3.1 D) | Unit test | **5.** `NativeAdPoolsTest.switchNotKnownYet_preloadWaits_thenFollowsSwitches`: `units[GALLERY_INLIST_MEDIUM] = UNIT_B`; `enabled` của `GALLERY_NATIVE_MEDIUM` là một `MutableSharedFlow<Boolean>()` chưa phát; `pools.slot().loadIfNeeded()` (hàm phụ có sẵn của lớp test), `runCurrent()`: không có lượt gửi nào. Phát `false`, gọi lại: có đúng một lượt gửi ở thời điểm 0 (pool của `GALLERY_INLIST_MEDIUM`, công tắc bật). **6.** `switchNotKnownYet_poolTakenBySlot_stillOpen`: cùng dựng; `pools.pool(GALLERY_INLIST_MEDIUM)!!.refill()`, `runCurrent()`: có một lượt gửi (pool không bị đóng vì một công tắc chưa biết). Hai test khóa hành vi có sẵn mà `firstOpenOnly` dựa vào nên có thể qua ngay; mỗi test có bước đối chứng, đổi tạm rồi trả về: **đối chứng của test 5:** `perl -pi -e 's/\Q.format == format && enabledNow[it] == true\E/.format == format \&\& enabledNow[it] != false/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt`, `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt` thấy tệp đổi, chạy thì test 5 đỏ (có lượt gửi khi công tắc chưa biết), rồi `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt`. **Đối chứng của test 6:** `perl -pi -e 's/\Qit] != false\E/it] == true/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt`, `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt` thấy đổi, test 6 đỏ, rồi `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt` |
| App biết thẻ có được phép không: không có `AdsController`, sai định dạng | Compose UI test | **7.** `NativeStepAdAllowedTest.noController_false` (đặt nội dung không có `LocalAdsController`: `false`); `otherFormat_throws` (`rememberNativeStepAdAllowed(GALLERY_NATIVE_MEDIUM)` ném `IllegalArgumentException`) |
| KB-20 tiêu chí 9 ở dự án mẫu: hàm mới theo mọi cổng, mở rồi đóng lại (KB-12, KB-7, KB-30, khóa vị trí) | Compose UI test | **8.** `NativeStepAdAllowedTest.followsEveryGate_openThenClosed` (`testAdsController(flags = DefaultAdsFlags(settings, adFreeStatus))`, `CompositionLocalProvider(LocalAdsController provides controller)`, đọc `rememberNativeStepAdAllowed(GALLERY_NATIVE_STEP)` vào một biến): khóa bật, chưa đồng ý: `false`; `openConsentGateForTest()`: `true`; `closeConsentGateForTest()`: `false`; `reopenConsentGateForTest()`: `true`; `is_show_ads` tắt: `false`, bật: `true`; `is_show_gallery_native_step` tắt: `false`, bật: `true`; `adFree` bật: `false`, tắt: `true`. Mỗi bước sau `composeRule.waitForIdle()`. Đã chạy thử chuỗi này trên nhánh chính với hàm nội bộ `rememberStepAllowed(...)` (mục 4.1) |
| Thẻ và hàm mới đi qua cùng một cổng | Compose UI test | **9.** `NativeStepAdAllowedTest.stepAdAndAllowed_sameGate`: một nội dung có cả `NativeStepAd(GALLERY_NATIVE_STEP, step = 0)` và `rememberNativeStepAdAllowed(GALLERY_NATIVE_STEP)`, `testAdsController(funnel = RecordingAdFunnel(), ...)`. Số sự kiện `AdFunnelEvent.Eligible` (thẻ ghi một lần mỗi khi cổng của nó mở, `ReportNativeEligible(allowed)` của `NativeAdSlotGate`) tăng đúng 1 ở mỗi bước mà hàm mới đổi từ `false` sang `true`, không tăng ở bước hàm mới là `false`: chưa đồng ý 0 và `false`; đồng ý 1 và `true`; rút đồng ý 1 và `false`; đồng ý lại 2 và `true`; `adFree` bật rồi tắt 3 và `true` (đã đo trên nhánh chính chỉ với `NativeStepAd`: 0, 1, 1, 2, 3). **10.** `stepAd_gateClosed_takesNoSpace`: cùng dựng, `NativeStepAd` mang `Modifier.testTag(SLOT)`; đồng ý rồi rút đồng ý: nút `SLOT` cao 0 dp, không có nút `NativeAdTestTags.CARD`, và hàm mới `false` |
| KB-20 tiêu chí 6 và 8 ở thẻ: lần mở sau thẻ không chiếm chỗ, hàm mới `false` (KB-20 tiêu chí 4); lần mở đầu cổng đóng lại vẫn áp; tiêu chí 9 | Compose UI test | **11.** `NativeStepAdAllowedTest.firstOpenOnly_laterOpen_falseAndNoEligible`: kho DataStore có `ads_first_open_done = true`, `firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP }`, khóa bật, `openConsentGateForTest()`: hàm mới `false`, không có sự kiện `Eligible`, nút `SLOT` cao 0 dp. Đối chứng: cùng dựng với kho rỗng (lần mở đầu): hàm mới `true` và có 1 sự kiện `Eligible`. Rồi lần mở đầu, rút đồng ý: `false` |
| Mô tả khóa cho người nhập Firebase | Unit test | **12.** `AdsSettingsTest.placementEnabled_firstOpenOnly_saysSo`: `AdsSettings.placementEnabled(AdPlacementDeclaration("x_step", AdFormat.NATIVE_STEP, firstOpenOnly = true)).description` chứa `"First open since install only."`; đối chứng: cùng khai báo không `firstOpenOnly` thì không chứa; mọi `AdPlacement.entries` của dự án mẫu không chứa (bảng khóa không đổi) |
| Excel kế hoạch ghi vị trí chỉ ở lần mở đầu | Test script | **13.** Khối `ads-plan` của `scripts/tests/run.sh`: `expect "ads-plan: a first-open-only placement says so in its note" firstOpenOnly "$(ap_get 'P["home_grid"]["note"]')"` (dòng `HOME_GRID` giả có `firstOpenOnly = true`, không có KDoc). Đỏ trước code: ghi chú rỗng |
| Yêu cầu nạp sau khi dấu đọc xong (mục 3.1 D) | Unit test (Robolectric) | **14a.** `AdsControllerFirstOpenOnlyTest.markRead_asksPreloadOnlyWhenOn` (vị trí `native_step`): kho DataStore giả giữ lần đọc tới khi test cho qua (`CompletableDeferred`), chỉ `is_show_gallery_native_step` bật (mọi khóa khác mặc định tắt), `firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP }`, `controller.start(application, AppActivityFilter { true })`, `shadowOf(Looper.getMainLooper()).idle()`: chưa có dòng log bắt đầu bằng `"Preload PLACEMENT_ENABLED"`. Cho kho trả rỗng (lần mở đầu), chờ có giới hạn: có đúng một dòng như vậy. Đối chứng: kho trả dấu có sẵn (lần sau): không có dòng nào; không đặt điểm nối, kho có dấu: có một dòng (vị trí thường vẫn xin nạp). Đã chạy thử cơ chế trên nhánh chính với `show_open_ads_first_open` bật: trước khi cho kho qua không có dòng, sau đó có `Preload PLACEMENT_ENABLED: IGNORE_SDK_NOT_STARTED` (mục 4.1); câu khẳng định chỉ so phần đầu, vì quyết định phụ thuộc SDK. **14b.** `markRead_fullScreenStep_asksPreloadOnlyWhenOn` (vị trí `native_fullscreen`): như 14a nhưng chỉ bật `is_show_gallery_native_step_full` và `firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP_FULL }`: kho có dấu (lần sau) thì không có dòng `Preload PLACEMENT_ENABLED`; kho rỗng (lần đầu) thì có đúng một dòng |
| Trang toàn trang qua controller thật theo cổng | Compose UI test | **15.** `NativeStepAdAllowedTest.fullScreenStepReady_followsGate_realController`: `GALLERY_NATIVE_STEP_FULL`, khóa bật; `openConsentGateForTest()` và `putPreloadedNativeFullScreenForTest(GALLERY_NATIVE_STEP_FULL, FakeLoadedNativeAd())`; `rememberNativeFullScreenStepReady` thành `true` (chờ có giới hạn 3 giây, vì đọc lại mỗi 500 ms trên đồng hồ thật); `closeConsentGateForTest()`: `false` ngay sau `waitForIdle()`; `reopenConsentGateForTest()`: `true` lại (chờ có giới hạn). Đã chạy thử trên nhánh chính: `true`, `false`, `true` (mục 4.1). **15c.** `fullScreenStepReady_firstOpenOnly_laterOpen_false`: kho DataStore có `ads_first_open_done = true`, `firstOpenOnlyOf = { it == GALLERY_NATIVE_STEP_FULL }`, khóa bật, đã đồng ý, quảng cáo giả trong slot: `rememberNativeFullScreenStepReady` vẫn `false` sau một khoảng cố định 1 500 ms đồng hồ thật (ba lần đọc lại 500 ms), kèm `waitForIdle()`. **15d.** Đối chứng: cùng dựng với kho rỗng (lần mở đầu): `true` trong 3 giây |
| Bảng khóa không đổi | Kiểm trong `scripts/check.sh` | Test bảng khóa của `:app` so `docs/remote-config-keys.md` và `docs/remote-config-template.json` với khóa sinh từ code; qua mà không sinh lại. Thêm: `git status --short -- docs/remote-config-keys.md docs/remote-config-template.json` rỗng sau `scripts/check.sh` |
| Tài liệu đúng luật mới và không ghi mã lịch sử | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR của task đọc OB-03, OB-04, MC-06, A-01 của `ads-business-rules.md` và ba chỗ của `ads-guide.md`, so với spec đã sửa (câu hỏi 1 tới 4), và đọc hai dòng nối của mục "Giới hạn" ở trên. `python3 scripts/history-refs.py` qua (cũng chạy trong `scripts/check.sh`) |

**Ngoại lệ viết test trước:** phần chỉ là tài liệu (`ads-business-rules.md`, `ads-guide.md`) và chú thích, KDoc. Test 5, 6 khóa hành vi đã có nên có thể qua ngay; bước đối chứng ở dòng của chúng. Không có kiểm tra trên emulator hay máy thật: dự án mẫu không có vị trí `firstOpenOnly` nào để thử trên bản staging, và thẻ của màn xem thử không đổi hành vi.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-10, trên nhánh chính `88b899a`, bằng test tạm `TmpT099ProbeTest` trong `core/ads/src/test` (đã xóa, không commit):
  - Hàm nội bộ `rememberStepAllowed(GALLERY_NATIVE_STEP, controller.canRequestAds, controller::isEnabled) { controller.unitId(it) }` với `testAdsController(flags = DefaultAdsFlags(settings, adFreeStatus))`, khóa bật: chưa đồng ý `false`; `openConsentGateForTest()` `true`; `closeConsentGateForTest()` `false`; `reopenConsentGateForTest()` `true`; `is_show_ads` tắt `false`, bật `true`; khóa vị trí tắt `false`, bật `true`; `adFree` bật `false`. `unitId(GALLERY_NATIVE_STEP)` khác `null` với mã thử của Google.
  - `NativeStepAd(GALLERY_NATIVE_STEP, step = 0)` với `testAdsController(funnel = RecordingAdFunnel())`: số `AdFunnelEvent.Eligible` lần lượt 0 (chưa đồng ý), 1 (đồng ý), 1 (rút), 2 (đồng ý lại), 3 (sau `adFree` bật rồi tắt).
  - `FirstOpenMark` trên kho có dấu sẵn: `isEnabled(OPEN_APP).first()` hai lần đều `false`, kho bị đọc 1 lần.
  - `rememberNativeFullScreenStepReady(GALLERY_NATIVE_STEP_FULL)` với quảng cáo giả trong slot: đồng ý `true` (trong 3 giây), rút `false`, đồng ý lại `true`.
  - `controller.start(...)` với kho DataStore giữ lần đọc và `show_open_ads_first_open` bật: trước khi cho kho qua không có dòng `Preload PLACEMENT_ENABLED`; sau đó có `Preload PLACEMENT_ENABLED: IGNORE_SDK_NOT_STARTED`.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT099ProbeTest*' -i` (JDK của `scripts/local-setup.sh`). Không cần `scripts/check.sh`: tiêu chí E dưới 2, không có kỹ thuật hay thư viện mới.
- **Kết quả:** các điểm nối, cách đếm sự kiện và dòng log cho đúng các giá trị mà test 4c, 8, 9, 14, 15 khẳng định. Kết quả của `scripts/existing-tests.py` ở mục 3.5.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thuộc tính khai báo mới đổi điều kiện hiện của vị trí khai báo nó; dự án mẫu không có vị trí nào khai báo), 7 (module core `:core:ads`, API công khai mới). Không chạm 2, 3, 4 (không thêm hay sửa sự kiện; test chỉ đếm sự kiện có sẵn), 5 (không đổi hộp thoại đồng ý hay thứ tự khởi tạo SDK; dấu lần mở đầu đọc như trước), 6, 8, 9 (dùng lại khóa DataStore có sẵn, không chuyển đổi dữ liệu). Người phụ trách sản phẩm (anh Nhật) cần biết trước khi duyệt và xác nhận spec đã sửa (câu hỏi 1).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Năm tệp nguồn của `:core:ads`, có đổi giao diện công khai (`AdPlacement` thêm tham số, hàm `rememberNativeStepAdAllowed`); một script và test của nó; hai tài liệu hướng dẫn. Một module |
| B. Độ rõ của yêu cầu | 2 | Spec chưa có tiêu chí cho vị trí chỉ ở lần mở đầu; còn câu hỏi mở ảnh hưởng tới cách làm (lần mở đầu tính tới đâu, định dạng nào được khai báo: câu hỏi 2, 3). Có bản đã chạy ở app làm mẫu |
| C. Khả năng kiểm chứng tự động | 1 | Mọi điểm code có test JVM với `AdsController` thật, trừ hai dòng nối ghi ở mục 4 (người đọc); dự án mẫu không có vị trí `firstOpenOnly` để thử trên staging |
| D. Ảnh hưởng chéo | 2 | `ads-guide.md`, `ads-business-rules.md`, `AdsController.kt`, `NativeFlowStep.kt` (gọi `NativeAdSlotGate`) cũng nằm trong phạm vi của T-097 hay T-098 (chưa có plan); `AdPlacement.kt` là tệp dùng chung của mọi định dạng |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn (thuộc tính khai báo như `keepSpaceWhenPoolEmpty`, nhánh của `placementSwitch` như vị trí toàn app, điểm nối `...ForTest`); bản ở app đã chạy |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` (khai báo của mọi vị trí), `StartupAdChoice.kt` (`placementSwitch`, công tắc của mọi vị trí), `AdsController.kt` (`isEnabled`), `NativeFlowStep.kt`, `AdsSettings.kt`, `scripts/ads-plan.py`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`.
- Task khác có thể xung đột:
  - T-097 (plan đang được viết song song): `AdsController.kt` (thêm `poolExhausted`), chữ ký của `NativeAdSlotGate` thêm `cardHeight` (mà `NativeStepAdHost` trong `NativeFlowStep.kt` gọi), `docs/ops/ads-guide.md` (dòng "Thuộc tính vị trí" ở mục 5, đoạn native trong lưới ở mục 4), `docs/ops/ads-business-rules.md` (luật của ô trong lưới, P-06, MC-07); có thể `NativeAdSlot.kt`, `NativeAdCell.kt`, `InListAds.kt`, `NativeAdCard.kt` mà plan này không chạm. Phiên áp T-099 lên chữ ký mới của T-097: chỗ T-099 sửa trong `NativeFlowStep.kt` chỉ là cách đọc cổng, không đổi lời gọi `NativeAdSlotGate`; trong `AdsController.kt` chỉ là `isEnabled` và điểm nối cạnh nó. Dòng "Thuộc tính vị trí" của `ads-guide.md`: plan này chỉ thêm một vế ở cuối danh sách, áp lên bản T-097 để lại.
  - T-098 (đang được viết song song): `docs/ops/ads-guide.md` (đoạn rewarded và cách dịch chữ của khung ở `:app`), có thể `docs/ops/ads-business-rules.md`. Không trùng đoạn với plan này.
  - Chỗ T-099 sửa là các dòng riêng (OB-03, OB-04 mới, một vế của MC-06, A-01 và ba chỗ của `ads-guide.md`), nên chỉ có thể xung đột khi merge, không xung đột ý. Plan này không viết phần của T-097, T-098.
  - T-086, T-087, T-088 và các task ads khác đã xong; `AdsController.kt` thay đổi nhiều, phiên áp plan lên nhánh chính mới nhất.
- Đề xuất thứ tự chạy: sau T-097 và T-098 (`depends_on` của ticket), trong cùng danh sách hay phiên sau. `run-check.py` sẽ báo cùng sửa `ads-guide.md`, `ads-business-rules.md`, `AdsController.kt`; xếp T-099 sau cùng của chuỗi.

## 7. Điều kiện dừng riêng

- Một test có sẵn của `StartupAdChoiceTest`, `FirstOpenMarkTest`, `AdsControllerStartupInterComboTest` (app open lúc mở app), `NativeFlowStepTest` hay `NativeFullScreenStepRecreateTest` đỏ sau bước 4: dừng, vì plan không đổi cách đọc dấu lần mở đầu, công tắc của vị trí thường hay hành vi của thẻ và trang toàn trang.
- `docs/remote-config-keys.md` hay `docs/remote-config-template.json` đổi sau `scripts/check.sh` (hay test bảng khóa đỏ): dừng, vì plan không đổi khóa hay mô tả của vị trí nào của dự án mẫu.
- Phương án dự phòng ở mục 3.4 vẫn đỏ vì một luồng đã thu thập trước khi test đặt điểm nối: dừng.
- Spec chưa có tiêu chí cho vị trí chỉ ở lần mở đầu lúc phiên bắt đầu task (câu hỏi 1 chưa được xác nhận): dừng (plan không được duyệt khi chưa có, điều kiện này chỉ để chắc).

## 8. Câu hỏi cho người duyệt

1. **(Người phụ trách sản phẩm) Thêm tiêu chí cho vị trí chỉ ở lần mở đầu vào spec `ads`.** Spec `confirmed` chưa có tiêu chí nào; chỉ có `show_open_ads_first_open` của app open (KB-13). Plan chờ spec sửa xong và được xác nhận lại rồi mới duyệt; sau đó mục 2 và mục 4 đổi "đề xuất 1 tới 4" sang số tiêu chí thật.
   - AI đề xuất: một PR tài liệu riêng (trước khi duyệt plan, ngoài phiên tự chạy) sửa `docs/features/ads/spec.md`: chuyển sang `changed`, thêm vào KB-20 bốn tiêu chí "**Thêm 2026-11-10:**" dưới đây, câu hỏi 59 (đã trả lời theo câu hỏi 2 tới 4 của plan này), một dòng lịch sử; anh Nhật xác nhận thì về `confirmed`.
     - Đề xuất 1: "Khi spec của app khai báo vị trí native của luồng là chỉ ở lần mở đầu và người vận hành bật vị trí đó, thì thẻ hay trang native chỉ có trong lần mở đầu sau khi cài (hay sau khi xóa dữ liệu app); từ lần mở thứ hai, bước hiện bình thường, không có khoảng trống, app không tải quảng cáo cho vị trí đó, dù khóa vẫn bật."
     - Đề xuất 2: "Lần mở đầu kéo dài tới khi tiến trình của app kết thúc (người dùng đóng hẳn, hoặc hệ thống đóng app khi ở nền); ra nền rồi quay lại khi app chưa bị đóng vẫn là lần mở đầu." (theo câu hỏi 2)
     - Đề xuất 3: "Trong lần mở đầu, khi người dùng rút đồng ý, mua gói bỏ quảng cáo hay người vận hành tắt khóa, thì quảng cáo của vị trí chỉ ở lần mở đầu biến mất ngay như vị trí thường."
     - Đề xuất 4: "Khi app chờ thẻ native của bước trước khi cho đi tiếp, thì app chỉ chờ lúc thẻ được phép hiện (đã đồng ý, khóa bật, chưa mua gói bỏ quảng cáo, đúng lần mở với vị trí chỉ ở lần mở đầu); thẻ không được phép, hay thôi được phép giữa lúc chờ, thì người dùng đi tiếp ngay." Ở dự án mẫu, tiêu chí này kiểm bằng giá trị của hàm cho biết thẻ có được phép không; phần chờ thuộc spec của app.
   - Câu hỏi 59 đề xuất: "**Vị trí chỉ ở lần mở app đầu tiên khai báo thế nào?** (KB-20, issue #381) Trả lời: thuộc tính của khai báo vị trí native của luồng (`native_step`, `native_fullscreen`), không có khóa riêng; công tắc là `is_show_<mã vị trí>` cùng dấu lần mở đầu dùng chung với quảng cáo mở app (A-01); định dạng khác và vị trí toàn app không khai báo được; đọc dấu lỗi thì coi là lần sau."
   - Nếu anh chọn khác ở câu 2 tới 4: tiêu chí và câu 59 đổi theo; plan lên phiên bản 2 khi cách làm đổi (câu 2 phương án B, câu 3 phương án A).
2. **(Người phụ trách sản phẩm) "Lần mở đầu" tính tới đâu?**
   - Phương án A (AI đề xuất): tới khi tiến trình của app kết thúc (người dùng đóng hẳn, hoặc hệ thống đóng app khi ở nền), như dấu của app open lúc mở app (A-01): ra nền rồi quay lại khi app chưa bị đóng vẫn là lần mở đầu. Ưu: dùng lại đúng một dấu, đọc một lần, không thêm trạng thái; người dùng xem dở luồng giới thiệu, ra nền rồi quay lại vẫn thấy thẻ như trước. Đây là cách app camera-pose đang chạy.
   - Phương án B: chỉ tới lần ra nền đầu tiên. Cần thêm trạng thái theo vòng đời app và test ra nền rồi trở lại; plan lên phiên bản 2.
3. **(Người phụ trách sản phẩm) `firstOpenOnly` cho định dạng nào?**
   - Phương án B (AI đề xuất, plan viết theo): chỉ hai định dạng của bước trong luồng (`native_step`, `native_fullscreen`), không vị trí toàn app; định dạng khác bị từ chối khi khai báo. Ưu: khớp tiêu chí đề xuất của KB-20 và mục đích của ticket; không phải quyết các trường hợp kinh doanh của định dạng khác (ví dụ rewarded ở lần mở sau làm hành động thẳng, tức người dùng được thưởng mà không xem quảng cáo).
   - Phương án A: mọi định dạng trừ vị trí toàn app. Khi chọn A cần thêm vào spec một tiêu chí chung cho mọi định dạng (ngoài KB-20) và một tiêu chí riêng cho rewarded (hành động ở lần mở sau làm thẳng hay chặn), test 1 và `require` của mục 3.1 B đổi, mục 3.1 C thêm đường của các định dạng khác; plan lên phiên bản 2.
4. **(Người phụ trách sản phẩm) Khi đọc hay ghi dấu lần mở đầu lỗi.**
   - AI đề xuất: giữ đúng như app open lúc mở app (A-01, `FirstOpenMark`): đọc lỗi thì coi là lần sau (vị trí tắt, an toàn cho người dùng), ghi lỗi thì lần này vẫn là lần đầu và lần sau có thể lại là lần đầu (thẻ có thể hiện thêm một lần). Không đổi code của `FirstOpenMark`.
5. **(Người duyệt) Thử vị trí `firstOpenOnly` bằng điểm nối test hay bằng một vị trí xem thử mới?** Dự án mẫu không có vị trí nào khai báo `firstOpenOnly`.
   - Phương án A (AI đề xuất): điểm nối `AdsController.firstOpenOnlyOf` (mục 3.1 B, mặc định đọc `declaration.firstOpenOnly`, code chính không đặt) cho test 4, 11, 14, 15c; hàm thuần `placementSwitch` nhận `firstOpenOnly` làm tham số cho test 2, 3. Ưu: không thêm vị trí (không đổi bảng khóa, giá trị cục bộ của staging, màn xem thử, test liệt kê vị trí). Nhược: một nhánh chỉ cho test trong code chính (đã có tiền lệ `nativeLoaderForTest`, `maxBannerHeightForTest`); hai dòng nối chỉ người đọc (mục 4, Giới hạn); không thử được trên bản staging.
   - Phương án B: thêm vị trí xem thử `gallery_native_step_first_open` (`firstOpenOnly = true`) và một thẻ trên màn xem thử, để dev thử trên máy: lần đầu sau khi xóa dữ liệu có thẻ, mở lại thì không. Thêm `AdPlacement.kt` một dòng, bảng khóa sinh lại, `AdGalleryScreen.kt`, ảnh chuẩn của màn xem thử nếu có, các test liệt kê vị trí; cỡ ticket lên L; plan lên phiên bản 2.
6. **(Người duyệt) Ghi `firstOpenOnly` vào Excel kế hoạch quảng cáo và vào mô tả khóa?** AI đề xuất có, cả hai (mục 3.1 B): nhóm kinh doanh và người nhập Firebase thấy khóa `is_show_<mã>` của vị trí đó chỉ có tác dụng ở lần mở đầu. Giới hạn: `scripts/ads-plan.py --keep <file cũ>` giữ cột Ghi chú mà nhóm đã điền ở file cũ, nên khi file cũ đã có ghi chú cho vị trí đó, dấu `firstOpenOnly` từ code bị ghi đè trong file mới. T-099 không đổi cách `--keep` gộp ghi chú (không đơn giản: phải tách phần của nhóm và phần của code trong một ô) và không đổi `docs/ops/ads-plan.md`; mô tả khóa trong sheet Remote Config vẫn có câu "First open since install only.". Nếu anh muốn sửa `--keep`: ticket riêng. Không chọn ghi gì thì bỏ `scripts/ads-plan.py`, `scripts/tests/run.sh`, `AdsSettings.kt`, `AdsSettingsTest.kt` khỏi mục 3.2 và test 12, 13.
7. **(Người duyệt) Có cần ADR không?** AI đề xuất không: ADR-0045 đã để luật nghiệp vụ ở `ads-business-rules.md` và cho thuộc tính khai báo của vị trí (mục 4.7); thuộc tính mới không theo tính năng của một app (mục 4.9). Luật ghi ở OB-03, OB-04.
8. **(Người duyệt) Thêm log cho vị trí `firstOpenOnly` bị tắt ở lần mở sau?** `FirstOpenMark` đã ghi "First open since install" ở lần đầu; lần sau không có dòng nào cho biết vị trí bị tắt vì dấu. AI đề xuất không thêm trong ticket này: `isEnabled` được gọi nhiều lần nên log phải đặt ở `start()` kèm test riêng; log tạo pool hiện có (`Pool ... created for ...`) không xuất hiện ở lần sau, đủ để phân tích phiên thử. Nếu anh muốn: một dòng `info` ở `start()` sau khi dấu đọc xong, liệt kê mã các vị trí `firstOpenOnly` bị tắt, thêm một test với `RecordingAppLogger`; plan lên phiên bản 2.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev: người duyệt cũng là người duyệt chéo, `risk-scoring.md` mục 3)
- **Ghi chú:**
  - AI ghi theo lời anh Nhật ngày 2026-11-10, trong phiên có người: "Duyệt theo đề xuất của AI" (trả lời bản plan ở commit 2c1bcdd, PR #442, sau rà soát độc lập và một vòng rà soát lại). Anh Nhật trả lời cả với vai người phụ trách sản phẩm (câu 1 tới 4).
  - Điều kiện duyệt đã đạt: spec sửa ở PR #446, anh Nhật xác nhận chữ đầy đủ ngày 2026-11-10 ("Xác nhận spec ads"). PR #446 merge trước PR này.
  - Mục 8 theo đề xuất AI đã trình ngay trước lời duyệt:
    - câu 1: bốn tiêu chí vào KB-20 (tiêu chí 6 tới 9) và câu hỏi 59;
    - câu 2 (A): lần mở đầu kéo dài tới khi tiến trình kết thúc;
    - câu 3 (B): `firstOpenOnly` chỉ ở `native_step`, `native_fullscreen`, không cho vị trí toàn app;
    - câu 4: đọc dấu lỗi thì coi là lần sau, như quảng cáo mở app;
    - câu 5 (A): điểm nối `firstOpenOnlyOf`, không thêm vị trí xem thử;
    - câu 6: ghi `firstOpenOnly` vào Excel kế hoạch và mô tả khóa, chấp nhận giới hạn của `--keep`;
    - câu 7: không cần ADR;
    - câu 8: không thêm log.
  - Phiên tự chạy làm được (`lane` để trống). Chạy sau T-097 và T-098 (mục 6).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261110-01, phiên bản plan 1

- **Kết quả:** đã merge (PR #451, commit `34219e4`, 2026-11-10 18:49 UTC).
- **Nhánh, PR:** `claude/T-099-the-native-cua-buoc`, https://github.com/VGL-Studio/app-project-template/pull/451. Commit test `a52dd2c` (kèm khung rỗng), commit code `3a3a399`, commit tài liệu `6822a68`.
- **Đã làm:** mục 3.1 A, B, E: `rememberNativeStepAdAllowed`, `AdsController.rememberStepAllowed` (ba chỗ gọi cũ đổi sang), `firstOpenOnly` của khai báo vị trí với hai `require`, `placementSwitch` gộp dấu lần mở đầu, `isEnabled` đọc `firstOpenOnlyOf`, mô tả khóa, `scripts/ads-plan.py`; MC-06, A-01, OB-03, OB-04 và ba chỗ của `ads-guide.md` (tìm theo tên mục và gạch; vế mới nối cuối gạch "Thuộc tính vị trí", sau phần của T-097). Bước 3 đến 5 giao cho một phiên con.
- **Test viết trước:** 19 trường hợp (18 test JVM, test script 13). 17 test JVM và test 13 đỏ đúng lý do trên khung rỗng. Test 5, 6, 15 qua ngay như plan nêu; hai lệnh `perl` đối chứng của plan, mỗi lệnh đổi đúng một dòng, làm test 5 và test 6 đỏ. 3 test viết chặt bằng bước đối chứng (`noController_false`, `stepAd_gateClosed_takesNoSpace`, `firstOpen_firstOpenOnlyPlacementOn`). Test JVM mọi module trên khung rỗng: chỉ 17 test mới đỏ.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 3 ghi chú: hai dòng KDoc dài hơn 120 ký tự; KDoc của `firstOpenResult` nói lần đọc dấu đầu ở `start` (có thể từ pool hay thẻ của bước khi app khai báo vị trí như vậy; hành vi vẫn đúng); hai dòng nối không có test tự động (đã đọc, đúng; app đầu tiên khai báo vị trí `firstOpenOnly` nên thêm test ở app).
- **Kết quả CI:** `pr-check` xanh (lần chạy 42877198131). `scripts/check.sh` cục bộ: 1919 test JVM, 0 lỗi; 91 ảnh screenshot, không ảnh nào đổi.
- **Lệch so với plan:** L4 (ADR-0032), bố cục test, câu khẳng định giữ: đối chứng 14a trong cùng phương thức; 15c và 15d gộp, đổi controller bằng `mutableStateOf`; test 11 đổi controller như vậy; test 9 thêm bước trung gian; `CountingPreferencesStore`, `FIRST_OPEN_KEY` dùng chung giữa hai lớp test. Test 13 giữ dòng giả `HOME_GRID` của plan. L-163 `applied` ghi trong PR báo cáo (ghi chú của danh sách chạy). Không có file ngoài mục 3.2, không có thay đổi file test sau commit test, không có câu khẳng định cũ viết lại.
- **Việc còn lại:** đóng issue #381 (phần `:core:ads`) kèm PR #451 khi dev đồng ý; phiên không tự đóng.
- **Cần người xem:** hai dòng nối của mục 4 ("Giới hạn"): tham số cuối của `AdPlacementDeclaration(...)` trong `AdPlacement.kt` và mặc định `firstOpenOnlyOf = { it.declaration.firstOpenOnly }` của `AdsController`.
