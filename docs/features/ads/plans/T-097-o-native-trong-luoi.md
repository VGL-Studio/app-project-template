---
ticket: T-097
title: Ô native trong lưới bỏ ô khi hết quảng cáo, nhận chiều cao ô và thẻ, vẽ được ở màn không trạng thái
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 30.0M        # cỡ L, số gợi ý 30.0M của scripts/token-usage.py estimate (2026-11-10, tạm, mới 2 task L); không chỉnh
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-097: Ô NATIVE TRONG LƯỚI BỎ Ô KHI HẾT QUẢNG CÁO, NHẬN CHIỀU CAO Ô VÀ THẺ, VẼ ĐƯỢC Ở MÀN KHÔNG TRẠNG THÁI

## 1. Tóm tắt

Ô thẻ native một ô trong lưới (`native_in_collection` qua `itemsWithInListAds` của lưới) có ba chỗ chưa làm được, đo trên nhánh chính (mục 4): pool cạn thì ô vẫn giữ một ô trống trong lưới; ô cao 332 dp (thẻ 300 dp cộng lề 16 dp trên dưới), lệch 16 dp so với thẻ của app cùng hàng và không nhận chiều cao của app; màn không trạng thái phải tự tính chỗ đặt ô.

Plan dựa trên bản đã chạy ở app camera-pose (PR #287 của app, issue #379), có bốn chỗ khác nêu ở mục 3.1; chỗ quan trọng nhất là điều kiện để ô quay lại:

- Ô một ô tự bỏ khỏi lưới khi pool cạn. Ô chỉ quay lại khi pool **có quảng cáo rảnh**, hay khi vị trí bật lại; pool chỉ "hết cạn" (đang có lượt nạp) thì ô không quay lại. Vị trí khai báo giữ chỗ khi cạn thì ô giữ khung chờ tĩnh. Hàng rộng hết lưới và hàng của danh sách giữ hành vi cũ.
- Ô một ô không có lề riêng; `adCellHeight` cho ô lưới, `cardHeight` cho `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdCell`; mặc định `null` giữ 300 dp và mọi ảnh chuẩn cũ.
- `itemsWithFixedAds` đặt quảng cáo cố định đúng chỗ của lưới thật, dùng ngay cho trang lưới của màn xem thử trong test staging.

Kiểm chứng bằng test JVM Robolectric của `:core:ads` (đo vị trí, chiều cao, số lần lấy từ pool, số liệu phễu, pool có lượt nạp mà không có quảng cáo), test có sẵn của màn xem thử và ảnh chuẩn `AdGalleryScreen_grid_*` không đổi.

**Không tách ticket.** Lý do:

- Ba phần nằm trên cùng một đường code: hàm lưới công khai, `InListAdRow`, `NativeAdCellHost`, `NativeAdSlotContent`, `BoundNativeAd`, `NativeAdLayout`. Tách thì hai plan nối tiếp sửa cùng các chữ ký đó; plan sau phải dựng trên plan trước. T-098, T-099 cũng chạy sau vì cùng sửa `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`: thành ba phiên nối tiếp trên cùng tài liệu thay vì hai.
- Bản cài đã chạy ở app (PR đã merge, CI, `main-guard`, `ui-test` xanh, đột biến từng phần đều làm test đỏ). Code chính khoảng 200 dòng, phần lớn là truyền một tham số; phần có logic mới chỉ là bỏ ô khi cạn.
- Luật hiển thị đổi chỉ ở phần bỏ ô. Người phụ trách sản phẩm duyệt phần này qua câu hỏi 1, 2, 3 và 6 ở mục 8 mà không cần ticket riêng. Muốn tách thì xem câu hỏi 4.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-07):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3, tiêu chí 4 | Mỗi ô quảng cáo có một quảng cáo riêng; khi không còn quảng cáo nào để lấy thì ô không hiện, không để khung chờ mãi | Trong lưới, "ô không hiện" nghĩa là lưới không còn ô trống: ô một ô không giữ quảng cáo bị bỏ khỏi lưới, mục sau dồn lên. Hiện nay ô cao 0 nhưng vẫn chiếm một ô của lưới: lỗi so với spec |
| KB-21, tiêu chí 2 | Khi không còn quảng cáo thì ô không hiện (hoặc giữ khung chờ nếu vị trí khai báo giữ chỗ); quảng cáo về muộn vẫn hiện vào ô | Như trên. Ô đã bị bỏ thì không tự hỏi pool nữa; `InListAds` hỏi pool mỗi 2 giây (P-06) và đặt lại các ô khi pool có quảng cáo rảnh, nên quảng cáo về muộn vẫn vào ô. "Không còn quảng cáo" đọc theo quảng cáo rảnh, không theo cờ cạn của P-05: pool đang có lượt nạp thì không cạn mà vẫn chưa có quảng cáo; máy không có fill thì pool đổi qua lại giữa cạn (chờ thử lại 5 giây) và không cạn (đang gửi), nên đặt ô lại theo cờ cạn làm ô quay lại thành khung chờ rồi bị bỏ, lưới dịch mỗi vài giây (mục 3.1, bước 1). Vị trí khai báo `keepSpaceWhenPoolEmpty` thì ô một ô giữ khung chờ tĩnh, không bị bỏ (câu hỏi 33 của spec: "vị trí trong lưới có khung cố định khai báo giữ chỗ khi cạn"). Hiện nay hàng trong danh sách và lưới bỏ qua khai báo này (`InListAdRow` truyền `keepSpaceWhenEmpty = false`); plan chỉ sửa cho ô một ô của lưới (câu hỏi 2) |
| KB-21, tiêu chí 1, 3, 4 | Mỗi ô một quảng cáo riêng; ô chỉ tính là đã xem khi trang đứng yên; quảng cáo đã thấy không hiện lại ở ô khác | Không đổi. Ô quay lại là một ô mới của pool (lấy quảng cáo mới, P-01, P-01a); test giữ nguyên |
| KB-3, tiêu chí 1, 2 | Quảng cáo sau mỗi N mục, không ở đầu; ít hơn N mục thì không có quảng cáo | Không đổi vị trí. `itemsWithFixedAds` dùng đúng `gridInListPositions` của hàm thật |

Phần còn lại của ticket không có tiêu chí riêng trong spec, AI hiểu như sau:

- **Chiều cao ô và thẻ:** thiết kế ghi thẻ dọc "180 x 300 dp là ví dụ, ô do lưới quyết định" (`features/ads/design/README.md`, component `Native/Card`). App truyền chiều cao thẻ của mình theo chiều rộng ô; khung chờ cùng chiều cao để không nhảy khi quảng cáo về (KB-1).
- **Màn không trạng thái:** screenshot test và preview không có `AdsController`, nên không có hàng quảng cáo nào. `itemsWithFixedAds` vẽ quảng cáo cố định (thường là `NativeAdCard`) ở đúng chỗ hàm thật đặt.

Luật: `docs/ops/ads-business-rules.md` MC-07, P-05, P-06, P-07; plan thêm vào P-06 cách làm trong lưới (mục 3.1, bước 8).

Thiết kế tham chiếu: `features/ads/design/README.md`, component `Native/Card` và trạng thái chờ ("giữ đúng chiều cao của quảng cáo ... thẻ dọc 300 dp hay cỡ ô"). Không có ảnh thiết kế màn hình cho ô trong lưới; không chạy `design-compare`.

ADR liên quan: ADR-0045 (nghiệp vụ quảng cáo theo AdsKit PARTNER; mục 4: quảng cáo chỉ qua composable của `:core:ads`), ADR-0001 (phụ thuộc module), ADR-0019 (log), ADR-0029, ADR-0031 (test cũ), ADR-0044 (tài liệu quy tắc không ghi mã ticket).

## 3. Giải pháp

### 3.1. Cách làm

Bản tham khảo là diff của PR #287 ở app camera-pose (commit merge `510388877aeff6a9f4e2f904323d2316467a688d`; các file `core/ads/.../InListAds.kt`, `NativeAdCard.kt`, `NativeAdCell.kt`, `NativeAdSlot.kt`, `NativeAdViewBinding.kt`, `AdsController.kt`). Plan này đủ để làm, phiên không cần đọc repo của app; muốn đối chiếu thì đọc bằng `gh api` (không dùng `gh pr diff`, `docs/ops/from-app.md` mục 3):

- `gh api -H "Accept: application/vnd.github.diff" repos/VGL-Studio/camera-pose/pulls/287 > "${TMPDIR:-/tmp}/pr287.diff"`
- Một file theo commit merge: `gh api "repos/VGL-Studio/camera-pose/contents/core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAds.kt?ref=510388877aeff6a9f4e2f904323d2316467a688d" -q .content | base64 -d`
- Phiên ở cloud có thể không có quyền đọc repo của app; khi đó bỏ qua, không dừng.

Code của dự án mẫu ở các chỗ đó giống bản của app trước PR. Bốn chỗ khác bản của app:

- Điều kiện quay lại là pool có quảng cáo rảnh, không phải pool hết cạn (bước 1, 6). `NativeAdPool.exhausted` của app giống hệt dự án mẫu, nên đọc code thì bản của app cũng có hiện tượng lưới dịch trên máy không có fill (câu hỏi 6).
- Giữ chỗ khi cạn cho ô lưới (bước 2).
- Tham số công khai `cardHeight` của `NativeAdCell` (bước 4).
- Hàm của controller đọc pool đã có, không tạo pool, không tra mã đơn vị (bước 6); test ở file khác.

1. **`InListAds.kt`, lớp `InListAds`: bỏ ô khi pool cạn.**
   - Thêm ba tham số `internal` vào constructor: `poolExhausted: (() -> Boolean)? = null` (pool của vị trí đang cạn theo P-05; dùng để bỏ ô), `poolHasFreeAd: (() -> Boolean)? = null` (pool có quảng cáo rảnh; dùng để đặt ô lại; `null` thì không đặt lại trước khi vị trí bật lại) và `keepSpaceWhenPoolEmpty: Boolean = placement.keepSpaceWhenPoolEmpty` (tham số riêng để test khai báo được, vì mọi vị trí của `AdPlacement` đều để `false`).
   - Trạng thái mới, khai báo trước `init` (vì `init` có thể gọi `release()` ngay): `poolEmpty = mutableStateOf(false)`, `holding = mutableStateMapOf<Int, Boolean>()`, `poolWatch: Job?`.
   - `internal fun cellShown(slot: Int): Boolean = !poolEmpty.value || holding[slot] == true`.
   - `internal fun onCellState(slot: Int, state: NativeAdState)`: `Loaded` thì `holding[slot] = true`, trạng thái khác thì bỏ `slot` khỏi `holding`. Gặp `Unavailable` mà `poolExhausted?.invoke() == true` và vị trí không giữ chỗ (`!keepSpaceWhenPoolEmpty`) thì `poolEmpty.value = true` và, nếu `poolWatch` chưa chạy, mở một vòng trong `scope`: mỗi `PoolCellPolicy.EXHAUSTED_RETRY_MILLIS` (2 giây, P-06) gọi `poolHasFreeAd()`; trả `true` thì `poolEmpty.value = false` và vòng dừng. Pool chỉ hết cạn (lượt nạp đang bay, hay vừa hết chờ thử lại) mà chưa có quảng cáo thì ô vẫn ở ngoài lưới.
   - `internal fun onCellLeft(slot: Int)`: bỏ `slot` khỏi `holding`.
   - `release()` (vị trí tắt, rút đồng ý, rời màn): hủy `poolWatch`, `poolEmpty.value = false`, xóa `holding`. Vị trí bật lại thì các ô có mặt ngay, không chờ nhịp 2 giây.
   - Vòng 2 giây không lấy quảng cáo. Ô quay lại là một `NativeAdCellHost` mới, lấy quảng cáo qua `pooledCellStates` như mọi ô (cổng S-11 trong `claim`).
   - Pool vẫn tự nạp khi không có ô nào (đã đọc code): lượt nạp lỗi thì `sendNext` chờ `AdRetryPolicy.nativeDelayMillis` (5 giây khi pool không giữ quảng cáo) rồi tự gọi `refill()`; nạp được thì `refill()` tiếp tới đủ cỡ. Chỉ khi cổng chặn lượt gửi (`sendGate` trả `false`) thì `sendNext` dừng mà không hẹn lại; trước đây ô chờ gọi `claim()` mỗi 2 giây, và `claim()` gọi `refill()`. Để giữ đúng nhịp đó khi ô đã rời lưới, `poolHasFreeAd` của controller gọi `refill()` của pool trước khi đọc (bước 6). `refill()` gửi qua `sendNext` và `sendGate` như mọi lượt (L-072); plan không thêm đường gửi nào.
   - App ra nền giữa lúc chờ: pool cạn khi ở nền (P-05, `canLoadNow` sai) trừ khi pool còn quảng cáo rảnh. Pool còn quảng cáo rảnh thì vòng có thể đặt `poolEmpty = false` lúc app ở nền, nhưng Compose không dựng lại khi activity đã dừng (đo ở mục 4). Ô chỉ quay lại và lấy quảng cáo sau khi người dùng trở lại app, như mọi ô vào màn; không có quảng cáo toàn màn hình nào đi theo đường này. Test `grid_inCollection_outInBackground_returnsOnlyAfterResume` kiểm điều này.
2. **`InListAds.kt`, hàm lưới công khai `LazyGridScope.itemsWithInListAds(items, ads, columns, key, contentType, itemContent)`.**
   - Thêm tham số `adCellHeight: (@Composable (cellWidth: Dp) -> Dp)? = null`, đặt trước `itemContent` để lambda cuối vẫn là nội dung mục.
   - `oneCell = gridAdSpansOneCell(format)`. Ô một ô: nội dung là `BoxWithConstraints { InListAdRow(ads, slot, contentPadding = PaddingValues(), cardHeight = adCellHeight?.invoke(maxWidth), keepSpaceWhenEmpty = ads.keepSpaceWhenPoolEmpty) }`. Hàng rộng hết lưới: `InListAdRow(ads, slot)` như cũ.
   - `adShown`: ô một ô của vị trí không giữ chỗ dùng `ads::cellShown`; mọi trường hợp khác `{ true }`. Dòng code đúng nguyên văn: `adShown = if (oneCell && !ads.keepSpaceWhenPoolEmpty) ads::cellShown else { _ -> true },` (lệnh đột biến ở mục 4 khớp dòng này). Khác bản của app: app không áp `keepSpaceWhenPoolEmpty` cho ô một ô (câu hỏi 2).
   - KDoc: thẻ không có lề trên dưới riêng (lưới tự cách hàng), cao `adCellHeight` theo chiều rộng ô (300 dp khi không truyền); pool cạn thì ô không giữ quảng cáo rời lưới, mục sau dồn lên, quay lại khi pool có quảng cáo rảnh; vị trí giữ chỗ thì ô giữ khung chờ tĩnh; hàng rộng hết lưới ở lại, cao 0.
3. **`InListAds.kt`, hàm lưới nội bộ, `itemsWithFixedAds`, `InListAdRow`.**
   - Hàm lưới nội bộ `itemsWithInListAds(items, positions, placementKey, ...)` thêm `adShown: (slot: Int) -> Boolean = { true }`; `InListRun.Ad` chỉ thêm `item(...)` khi `adShown(run.slot)`. Thứ tự mục giữ nguyên.
   - Hàm công khai mới `fun <T> LazyGridScope.itemsWithFixedAds(items, format: AdFormat, every: Int, columns: Int, key, contentType, adContent: @Composable (slot: Int) -> Unit, itemContent)`: gọi hàm nội bộ với `gridInListPositions(format, items.size, every, columns)`, `placementKey = "fixed"` (hằng `private`), `adSpansOneCell = gridAdSpansOneCell(format)`. KDoc: cho screenshot và preview; app hiện quảng cáo thật bằng `itemsWithInListAds`.
   - Không thêm bản cho `LazyListScope`: ticket chỉ nêu lưới. Danh sách có thể dùng `inListAdPositions` (công khai) như hiện nay.
   - `InListAdRow(ads, slot, modifier, contentPadding: PaddingValues = PaddingValues(vertical = Spacing.md), cardHeight: Dp? = null, keepSpaceWhenEmpty: Boolean = false)`. Nhánh pool: truyền `keepSpaceWhenEmpty`, `contentModifier = Modifier.padding(contentPadding)`, `onStateChange = { ads.onCellState(slot, it) }`, `cardHeight`, `adContent = { BoundNativeAd(it, size, ctaLarge = ctaLarge, cardHeight = cardHeight) }`; thêm `DisposableEffect(ads, slot) { onDispose { ads.onCellLeft(slot) } }`. Nhánh native small (không pool): lề và chiều cao như tham số, giá trị mặc định giữ hành vi cũ.
4. **`NativeAdCell.kt`.**
   - Công khai `NativeAdCell(placement, modifier, viewed, onAdAvailabilityChange, onScreen, cardHeight: Dp? = null)`: truyền `cardHeight` vào `NativeAdSlotGate` và `BoundNativeAd`. Bản của app không thêm vào `NativeAdCell` công khai; ticket nêu `NativeAdCell` và ô tự đặt hay trang pager cạnh thẻ của app cũng cần (câu hỏi 5).
   - `NativeAdCellHost`, `NativeAdCellOnScreen` thêm `cardHeight: Dp? = null`, truyền cho khung chờ tĩnh khi giữ chỗ (`NativeAdPlaceholder(size, animated = false, ctaLarge = ctaLarge, cardHeight = cardHeight)`) và cho `NativeAdSlotContent`.
5. **`NativeAdSlot.kt`, `NativeAdCard.kt`, `NativeAdViewBinding.kt`.**
   - `NativeAdSlotGate` (nội bộ) thêm `cardHeight: Dp? = null`, truyền cho `NativeAdCellHost` (nhánh pool) và `NativeAdSlotHost` không đổi.
   - `NativeAdSlotContent` thêm `cardHeight: Dp? = null`, chỉ cho khung chờ lúc tải: `NativeAdPlaceholder(size, ctaLarge = ctaLarge, cardHeight = cardHeight)`. Nhánh `Unavailable` không đổi: ô thẻ giữ chỗ đi qua khung chờ tĩnh của `NativeAdCellOnScreen` (`placeholderWhenEmpty` mặc định `true`), không qua `Spacer` của nhánh này.
   - `NativeAdSlot` công khai không đổi: nó chỉ vẽ small và medium (`requireNativeFormat`), không bao giờ vẽ thẻ (câu hỏi 5).
   - `NativeAdCard(assets, size, modifier, ctaLarge, cardHeight: Dp? = null)`, `NativeAdPlaceholder(size, modifier, animated, ctaLarge, cardHeight: Dp? = null)`, `NativeAdLayout(..., cardHeight: Dp? = null)`: nhánh mới `NativeAdSize.CARD -> Modifier.height(cardHeight ?: NativeAdDimens.CardHeight)` (khung chờ: `NativeAdSize.CARD -> cardHeight ?: NativeAdDimens.CardHeight`). Cỡ khác bỏ qua `cardHeight`. KDoc của `NativeAdLayout` sửa câu "A card takes the height a cell gives it; its own height is the default" thêm "or [cardHeight] when set".
   - `BoundNativeAd(ad, size, modifier, ctaLarge, cardHeight: Dp? = null)` truyền cho `NativeAdLayout`.
6. **Pool và controller: tín hiệu đọc pool đã có.**
   - `NativeAdPool.kt`: `val hasFreeAd: Boolean get() = free.isNotEmpty()`.
   - `NativeAdPools.kt`: map `byPlacement: MutableMap<AdPlacement, NativeAdPool>` ghi trong `pool(placement)` khi trả pool; `fun existingPool(placement: AdPlacement): NativeAdPool? = byPlacement[placement]`. Không tạo pool, không gọi `unitId` (thiếu mã đơn vị thì `AdsController.unitId` ghi `Log.e` mỗi lần gọi; vòng 2 giây không được gây dòng log đó).
   - `AdsController.kt`:
     - `internal fun poolExhausted(placement: AdPlacement): Boolean = nativePools.existingPool(placement)?.exhausted ?: true` (chưa có pool, hay không có mã đơn vị, thì như `pooledCell`: không có quảng cáo).
     - `internal fun poolHasFreeAd(placement: AdPlacement): Boolean`: `existingPool(placement)` là `null` thì `false`; có thì gọi `refill()` (bỏ quảng cáo hết hạn, gửi tiếp nếu pool rảnh và chưa đầy, như `claim()` của ô chờ) rồi trả `hasFreeAd`.
   - `rememberInListAds` truyền cho vị trí có pool (qua `controller?.takeIf { placement.format.usesPool }`): `poolExhausted = { ads.poolExhausted(placement) }`, `poolHasFreeAd = { ads.poolHasFreeAd(placement) }`.
7. **Test staging của màn xem thử, `PreviewAdGallerySlots.gridWithAds`:** thay vòng tự tính vị trí (`inListAdPositions` và `item(key = "ad_$slot")`) bằng `itemsWithFixedAds(items, AdFormat.NATIVE_IN_COLLECTION, every = gridEvery, columns = 2, key = { it }, adContent = { slot -> NativeAdCard(assets, NativeAdSize.CARD, Modifier.testTag(PreviewAdTags.grid(slot))) }, itemContent = itemContent)`; `gridEvery == null` thì `items(...)` như cũ. `columns = 2` kèm chú thích "same as GRID_COLUMNS of AdGalleryScreen" (hằng đó là `private` của `AdGalleryScreen.kt`; plan không đổi file này). Vị trí như cũ (thẻ một ô, N đếm theo mục), nên `AdGalleryScreenTest.grid_*` và ảnh chuẩn `AdGalleryScreen_grid_light.png`, `AdGalleryScreen_grid_small.png` không đổi.
8. **Tài liệu quy tắc** (không ghi mã ticket, ADR-0044):
   - `docs/ops/ads-business-rules.md`, P-06, cột luật, thêm sau câu cuối: "Trong lưới, ô một ô (thẻ trong tập hợp) cạn mà không giữ quảng cáo nào thì bị bỏ khỏi lưới, các mục sau dồn lên, lưới không có ô trống; ô đang có quảng cáo ở lại. Khi đó lưới hỏi pool mỗi 2 giây và đặt lại các ô khi pool có quảng cáo rảnh, hay khi vị trí được bật lại; pool đang nạp mà chưa có quảng cáo thì ô chưa quay lại. Vị trí khai báo giữ chỗ khi cạn thì ô giữ khung chờ tĩnh, không bị bỏ. Hàng rộng hết lưới và hàng của danh sách cạn thì cao 0, không bị bỏ." Cột nguồn thêm "; cách làm trong lưới là của dự án mẫu".
   - `docs/ops/ads-guide.md`:
     - Đoạn pool (dòng "Medium và thẻ dọc lấy quảng cáo từ pool"): sau câu "ô vẫn hỏi lại (500 ms, 2 giây khi cạn) nên quảng cáo về muộn vẫn hiện." thêm câu ngoại lệ của ô một ô trong lưới, ý như P-06.
     - Đoạn "Native trong danh sách", câu "pool cạn thì hàng cao 0" thêm ", trừ ô thẻ một ô của lưới (mục Lưới dưới)".
     - Mục **Lưới**: thêm ô thẻ không có lề trên dưới riêng (lưới đã cách hàng), cao 300 dp hay theo thẻ của app khi truyền `adCellHeight = { chiềuRộngÔ -> chiềuCao }`; pool cạn thì ô không giữ quảng cáo rời lưới, mục sau dồn lên (vị trí giữ chỗ thì giữ khung chờ); screenshot và preview của lưới dùng `itemsWithFixedAds(items, format, every, columns, adContent = { slot -> NativeAdCard(...) }) { item -> ... }` (lambda cuối là nội dung mục).
     - Mục **Ô tự đặt và trang của pager**: chữ ký thêm `cardHeight` (thẻ cao bằng thẻ của app bên cạnh; mặc định 300 dp).
     - Mục **Thuộc tính vị trí**: `keepSpaceWhenPoolEmpty` áp cho `NativeAdCell` và ô thẻ một ô của lưới; hàng của danh sách và hàng rộng hết lưới không giữ chỗ.
     - Bước 5 (**Test**): lưới có ô quảng cáo dùng `itemsWithFixedAds` với `NativeAdCard(assets, NativeAdSize.CARD, modifier, cardHeight = ...)`.

Mẫu theo:

- `InListAdsTest.setPooledList`, `FakePooledCells`, `pooledRow_*`: dựng `InListAds` với `pooledCell` giả, không cần controller.
- `InListAdsTest.pooledRow_impressedAd_notShownInAnotherRow`: dừng đồng hồ (`mainClock.autoAdvance = false`), tiến thời gian cố định.
- `NativeEligibleReportTest`, `AdsControllerFunnelTest`: `testAdsController(funnel = RecordingAdFunnel())` để đếm `AdFunnelEvent.Eligible`.
- `AdsControllerInScreenWindowTest.realController_poolRefillWhileCovered_waits`, `poolCanLoadNow_followsWindow`: controller thật, `cover()`, `uncover()`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Bước 1, 2, 3: bỏ ô khi cạn, `adCellHeight`, `itemsWithFixedAds`, `InListAdRow` nhận lề, chiều cao thẻ, giữ chỗ; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCell.kt` | Bước 4: `cardHeight` ở `NativeAdCell`, `NativeAdCellHost`, `NativeAdCellOnScreen`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSlot.kt` | Bước 5: `cardHeight` ở `NativeAdSlotGate`, `NativeAdSlotContent` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdCard.kt` | Bước 5: `cardHeight` ở `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdLayout`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdViewBinding.kt` | Bước 5: `cardHeight` ở `BoundNativeAd(ad, size, ...)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 6: `poolExhausted(placement)`, `poolHasFreeAd(placement)` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt` | Bước 6: `hasFreeAd` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPools.kt` | Bước 6: `byPlacement`, `existingPool(placement)` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Test mới của mục 4; tham số `poolExhausted` cho `setPooledList`; hàm dựng `ScriptedCells`, `loadedCell`, `loadingCell`, `setPooledGrid` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Test mới `card_cardHeight_setsCardAndPlaceholderHeight` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCellTest.kt` | Test mới `gate_card_cardHeight_placeholderAdAndKeptSpace` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInScreenWindowTest.kt` | Test mới `poolExhausted_noPool_true_followsWindow`, `poolHasFreeAd_onlyWithFreeAd_neverCreatesPool` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolTest.kt` | Test mới `hasFreeAd_falseWhileRequestInFlight_trueWithFreeAd` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolsTest.kt` | Test mới `existingPool_neverCreates_noUnitLookup` |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/PreviewAdGallerySlots.kt` | Bước 7: `gridWithAds` dùng `itemsWithFixedAds` |
| Sửa | `docs/ops/ads-business-rules.md` | Bước 8: P-06 |
| Sửa | `docs/ops/ads-guide.md` | Bước 8: sáu chỗ |
| Sửa | `docs/lessons.md` | L-161: trạng thái `applied`, dòng Áp dụng có số PR (cuối phiên) |
| Sửa | `docs/features/ads/plans/T-097-o-native-trong-luoi.md` | Nhật ký thực hiện |
| Sửa | `docs/features/ads/tickets/T-097-o-native-trong-luoi.md` | Trạng thái ticket |

Ảnh chuẩn: không thêm, không ghi lại ảnh nào. Mọi tham số mới mặc định giữ cách vẽ cũ; ảnh có thể bị chạm là `app/src/test/screenshots/AdGalleryScreen_grid_light.png`, `AdGalleryScreen_grid_small.png` (bước 7, vị trí và cỡ thẻ như cũ) và các ảnh `NativeAdCard_card_*` của `core/ads/src/test/screenshots/` (nhánh `CARD` mới của `NativeAdLayout` cho cùng 300 dp). Lệch thì dừng (mục 7).

Nơi đọc file bị đổi (`grep -rn` tên từng file trong `scripts/`, `.github/`, `*/src/test`):

- Tám file Kotlin: không script hay workflow nào đọc theo tên; chỉ có test dùng lớp, hàm của chúng (mục 3.5).
- `docs/ops/ads-business-rules.md`: các file sau chỉ nhắc tên trong chú thích hay KDoc, không mở file, nên không bị ảnh hưởng. Plan chỉ thêm câu vào P-06, không đổi mục hay bảng mà các chú thích đó dẫn tới (MC-02, mục 4, 5.7, 8, 10, S):
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`
  - `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`
  - `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`
- `scripts/ads-plan.py`: nhắc `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md` trong chú thích và trong chữ của trang "Nguồn", "Vào code" của file Excel; không đọc nội dung hai file. Không bị ảnh hưởng.
- `docs/lessons.md`: `scripts/status.py` đếm dòng "Trạng thái" của từng bài; dòng của L-161 vẫn đúng dạng (`applied (...)`). `scripts/new-app.sh` chỉ tìm dòng đánh dấu "Các bài học ghi dưới dòng này", plan không chạm dòng đó. `scripts/tests/run.sh` dùng tệp `lessons.md` giả trong thư mục tạm. `.github/ISSUE_TEMPLATE/from-app.yml` chỉ nhắc tên trong mô tả. Không file nào bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay nhịp. Đổi cách hiện ô thẻ một ô trong lưới (`native_in_collection`): rời lưới khi pool cạn, không lề 16 dp, chiều cao theo app; vị trí có sẵn duy nhất dùng nó là `gallery_in_collection` (chỉ staging). Không khóa Remote Config mới.
- Sự kiện phân tích: không đổi định nghĩa, tên hay tham số sự kiện nào. Thời điểm ghi đổi, theo định nghĩa có sẵn ("mỗi lần ô vào màn"): ô đã xem mà rời lưới khi cạn ghi `ad_opportunity` `not_ready` ngay lúc đó (trước đây khi cuộn đi); ô quay lại (pool có quảng cáo rảnh, hay vị trí bật lại) là một lần vào màn mới, thêm một `af_native_ad_eligible` (`ReportNativeEligible` trong `InListAdRow`) và thường một `shown`. Pool cạn kéo dài, kể cả khi pool đổi qua lại giữa cạn và đang nạp trên máy không có fill, thì ô ở ngoài lưới, không ghi thêm gì (câu hỏi 3; test `grid_inCollection_poolNotExhaustedWithoutAd_cellStaysOut`).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `BoxWithConstraints`, `PaddingValues`, `mutableStateMapOf` thuộc Compose foundation và runtime, đã có trên classpath của `:core:ads`; `Lifecycle` và `AndroidComposeTestRule` (test) đã có trên classpath test.

### 3.4. Phương án dự phòng

Không có. Điểm từng nghi là trạng thái đọc trong khối dựng `LazyGridScope` (`adShown` đọc `poolEmpty`, `holding`) có làm lưới dựng lại không: Compose chạy khối dựng của lưới trong `derivedStateOf` (`rememberLazyGridItemProviderLambda`), nên mọi trạng thái đọc ở đó được theo dõi; bản ở app chạy đúng cách này và các test tương ứng qua. Hàm lưới là extension của `LazyGridScope`, không có mức composable trước lưới, nên không có phương án thay trong cùng chữ ký.

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-097-o-native-trong-luoi.md`, chạy lại trên bản sửa theo rà soát (2026-11-10, nhánh từ `origin/main` tại `88b899a`): 784 test dùng tên hay tag của tám file code; "Cần xem trước" 58, "Nên xem" 331, còn lại plan đã nhắc. Bản đầu (sáu file) ra 652, 80, 253; các test của bản đầu nay plan đã nêu tên ở dưới.

"Cần xem trước" (đo vị trí, kích thước), đã đọc và xếp theo nhóm:

- Ô và hàng trong danh sách, lưới: `InListAdsTest.adRow_isSixteenDpFromItemsAboveAndBelow`, `loadingSlot_showsPlaceholderOfSmallHeight`, `lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6`, `alternatingPlacements_takeTurns`, `failedRow_showsAdAfterRetry` (hàng native small của `LazyColumn`, lề 16 dp giữ nguyên vì `contentPadding` mặc định là `PaddingValues(vertical = Spacing.md)`); `lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4` (small rộng hết lưới, không qua `cellShown`); `grid_inCollection_adTakesOneCell_everyNItems` (hàm lưới nội bộ với `adContent` riêng, `adShown` mặc định `{ true }`). Giữ.
- Thẻ, khung chờ, cỡ chữ: mọi test `NativeAdCardTest.*`, `NativeAdFontScaleTest.*`, `NativeAdViewBindingTest.sdkLayout_*`, `registersStarRatingView_*`, `NativeAdSlotTest.boundNativeAd_withFakeAd_drawsCard`, `swappedAd_recreatesBoundView`. Nhánh `CARD` mới của `NativeAdLayout` cho `Modifier.height(NativeAdDimens.CardHeight)`, đúng giá trị của nhánh `else` cũ (`heightOf(CARD)` là `CardHeight`); khung chờ thẻ cũng vậy. `card_inFixedCell_takesCellSize` (thẻ trong Box 160 x 240 dp lấy cỡ Box) giữ vì `Modifier.height(300.dp)` vẫn bị ràng buộc của cha thu về 240 dp như hôm nay (đo ở mục 4: modifier nhỏ hơn 300 dp thắng). Giữ.
- Ô của pool và cổng: `NativeAdCellTest.exhausted_takesNoSpace`, `exhausted_keepSpaceDeclared_showsStaticPlaceholder`, `inCollection_drawsCard_medium_drawsMedium`, `twoMediumCells_showDifferentAds`, `mediumSlot_recordsShownOnce_notReadyOnDispose`, `otherFormat_isRejected`; `NativeAdSlotTest.unavailable_*`, `buttonDoesNotMove_*`, `loading_showsPlaceholder`, `leavingComposition_destroysAd`, `slotAllowed_trueWhenAllGiven`, `withoutAdsController_takesNoSpace`, `availability_*`, `bottomNativeSlot_*`, `bottomSlotHeightConstant_*`. `cardHeight` mặc định `null`; `NativeAdSlotHost`, `NativeAdSlot` không đổi. Giữ.
- Bước của luồng, thanh đáy, lớp thu gọn, banner, hộp thoại rewarded, màn mẫu, màn xem thử ngoài trang lưới: `NativeFlowStepTest.*`, `BottomAdBarTest.*`, `CollapsibleNativeAdTest.*`, `BannerSlotHostTest.*`, `BannerAdSizeTest.*`, `RewardedActionTest.*`, `SampleScreenTest.*`, `AdGalleryScreenTest.collapsible*`. Chỉ trùng tên chung (`slot`, `AdsController`, `NativeAdTestTags.CARD`, `PLACEHOLDER`); không đi qua hàm lưới, `InListAdRow` hay cỡ `CARD`. `NativeFlowStep` dùng `NativeAdSlotGate` với cỡ `STEP`, `cardHeight` mặc định. Giữ.

"Nên xem": đã lọc theo "grid", "pool", "InListAds", "eligible", "opportunity", "keepSpace" và đọc các test khớp:

- `InListAdsStateTest.*`, `InListAdsTest.adRowKeys_*`, `adRow_scrolledAway_*`, `loadedSlots_destroyedWhenEveryBecomesNull_*`, `positions_followEvery`, `publicApi_withoutAdsController_addsNoAdRow`, `emptyAds_isRejected`, `lazyColumn_fewerItemsThanEvery_noAdRow`: hàng native small hay hàm danh sách; constructor `InListAds` chỉ thêm tham số có mặc định. Giữ.
- `InListAdsTest.pooledRow_*`, `pooledRows_everyBecomesNull_releaseToPool`: hàng pool của `LazyColumn`, không qua `cellShown`; `setPooledList` thêm một tham số có mặc định `null`. Giữ.
- `NativeAdPoolTest.*`, `NativeAdPoolsTest.*`, `NativePoolCellTest.*`, `AdsControllerInScreenWindowTest.*`, `AdsControllerNativeStockTest.*`: `NativeAdPool` chỉ thêm thuộc tính đọc `hasFreeAd`; `NativeAdPools.pool(placement)` chỉ ghi thêm pool vào `byPlacement`, cách tạo, đóng, mở và kế hoạch nạp sẵn không đổi; `pooledCellStates` không đổi. Giữ.
- `NativeAdSourceTest.*`, `NativeAdSourcesTest.*`, `AdPreloaderTest.*`, `PreloadPlanRunnerTest.*`, `NativeFullScreenTurnsTest.*`, `FullScreenPreloadSlotTest.*`, `NativeAdRefreshTest.*`, `NativeAdRetryTest.*`, `AdUnitIdsTest.*`, `AdTelemetryTest.*`: chỉ trùng tên chung với hai file pool (`refill`, `slot`, `free`, `pool`); không dùng `hasFreeAd`, `existingPool` hay hàm lưới. Giữ.
- `NativeEligibleReportTest.*`, `AdsControllerFunnelTest.*`: định nghĩa `eligible` không đổi; chỉ số lần ô vào màn trong lưới đổi khi pool cạn, có test mới. Giữ.
- `AdGalleryScreenTest.grid_insertsCardsAfterEveryNItems`, `grid_withoutN_showsOffTitle_noCards` (plan đã nêu ở mục 4): vị trí của `itemsWithFixedAds` bằng `inListAdPositions` cũ với thẻ một ô. Giữ.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Ô thẻ một ô của lưới cạn vẫn chiếm một ô (cỡ 0), để lỗ trong lưới | Bước 1, 2: ô không giữ quảng cáo rời lưới, mục sau dồn lên | Không có (không test nào dựng lưới có ô của pool) | Test mới ở mục 4 |
| Ô thẻ một ô của lưới có lề 16 dp trên dưới (ô 332 dp) | Bước 2: không lề riêng, ô cao bằng thẻ | Không có (`adRow_isSixteenDpFromItemsAboveAndBelow` là hàng danh sách, giữ 16 dp) | Test mới `grid_inCollection_noHeight_keeps300_noOwnPadding` |
| Hàng trong lưới bỏ qua `keepSpaceWhenPoolEmpty` | Bước 2: ô thẻ một ô của vị trí giữ chỗ giữ khung chờ tĩnh | Không có | Test mới `grid_inCollection_keepSpace_keepsStaticPlaceholder` |
| Ô cạn của lưới ở lại trong màn, chỉ ghi `eligible` một lần, `not_ready` khi cuộn đi | Bước 1: `not_ready` khi rời lưới; quay lại (có quảng cáo rảnh, hay vị trí bật lại) là lần vào màn mới, thêm `eligible` | Không có cho lưới; `NativeEligibleReportTest` kiểm cổng, giữ | Test mới `grid_inCollection_poolCycle_reportsOncePerEntry`, `grid_inCollection_poolNotExhaustedWithoutAd_cellStaysOut` |
| Thẻ, khung chờ thẻ luôn 300 dp | Bước 5: `cardHeight` khi truyền; mặc định 300 dp | `NativeAdCardTest.placeholder_matchesCardHeight_forEachSize`, `card_inFixedCell_takesCellSize`, `NativeAdViewBindingTest.sdkLayout_keepsCardHeights` | Giữ |
| Trang lưới của màn xem thử tự tính chỗ đặt thẻ | Bước 7: `itemsWithFixedAds` | `AdGalleryScreenTest.grid_*`, ảnh `AdGalleryScreen_grid_light.png`, `AdGalleryScreen_grid_small.png` | Giữ |
| Hàng pool của danh sách, hàng rộng hết lưới cạn thì cao 0 | Không đổi | Không có cho hàng pool cạn | Test mới `lazyColumn_pooledRow_*`, `grid_fullWidthRow_*` khóa hành vi cũ |

Không có câu khẳng định cũ nào phải viết lại (ADR-0031).

## 4. Kiểm thử

Test JVM Robolectric của `:core:ads` (`@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi` như `InListAdsTest`), không có `AdsController` trừ khi ghi rõ. Không có ảnh chuẩn mới.

**Số đo trên nhánh chính** (`88b899a`, 2026-11-10, test tạm `TmpT097MeasureTest` không commit, đã xóa; lưới 2 cột, 7 mục cao 120 dp, `GALLERY_IN_COLLECTION`, N = 3, `InListAds` với `pooledCell` giả):

- Pool cạn (ô nhận `Unavailable`): nút `inlist_ad_0` vẫn có (1 nút, cỡ 0); mục 3 ở cột trái, đỉnh 120 dp; mục 4 xuống hàng sau (cột trái, đỉnh 240 dp), mục 5 cột phải đỉnh 240 dp. Ô bên phải mục 3 (205,7 x 120 dp) bỏ trống: đây là lỗ trong lưới.
- Ô có quảng cáo: ô cao 332 dp, thẻ 300 dp, đỉnh thẻ 136 dp trong khi mục 3 cùng hàng ở 120 dp (lề 16 dp trên dưới).
- Ô đang tải: ô cao 332 dp, khung chờ 300 dp.
- `NativeAdCard(CARD, Modifier.height(240.dp))` đặt thẳng trong `setContent`: thẻ 240 dp (modifier ngoài đặt trước thắng `height(300)` bên trong, trừ khi cha giới hạn). Ô trong lưới thì khác: `InListAdRow` không nhận modifier của app, khung chờ và lề tính chiều cao riêng, nên cần tham số.
- Dựng lại khi activity ở nền: `moveToState(CREATED)` rồi ghi một trạng thái và `advanceTimeBy(3_000)`: không dựng lại; `moveToState(RESUMED)`: dựng lại. Test nền dưới dùng cơ chế này.

**Khung rỗng của bước 3** (cùng commit test, để test biên dịch được và đỏ ở câu khẳng định): các tham số mới có mặt nhưng bị bỏ qua (`poolExhausted`, `poolHasFreeAd`, `keepSpaceWhenPoolEmpty` của `InListAds`; `adCellHeight`; `cardHeight` ở mọi hàm của bước 4, 5); `onCellState`, `onCellLeft` rỗng, `cellShown` trả `true`; `itemsWithFixedAds` chỉ thêm mục, không thêm quảng cáo; `AdsController.poolExhausted` trả `false`, `poolHasFreeAd` trả `true`; `NativeAdPool.hasFreeAd` trả `true`; `NativeAdPools.existingPool` trả `null`; `rememberInListAds` chưa nối hai tín hiệu. `PreviewAdGallerySlots` (bước 7) đổi ở commit code.

**Hàm dựng mới trong `InListAdsTest`** (commit test):

- `ScriptedCells(script: (call: Int) -> Flow<NativeAdState>)`: lần gọi thứ k của `cell()` trả `script(k)`, đếm `calls`.
- `loadedCell(call, released)`: một quảng cáo giả tên "Pool ad <call + 1>", trả vào `released` khi ô rời đi. `loadingCell()`: `Loading` mãi.
- `setPooledGrid(itemCount, every, cell, exhausted, hasFreeAd: () -> Boolean = { false }, placement = GALLERY_IN_COLLECTION, columns = 2, adCellHeight = null, keepSpace = false, onOpportunity = {}, controller: AdsController? = null)`: `LazyVerticalGrid` dùng hàm lưới công khai, `InListAds(..., pooledCell = cell, poolExhausted = exhausted, poolHasFreeAd = hasFreeAd, keepSpaceWhenPoolEmpty = keepSpace)`; `controller` khác `null` thì bọc trong `CompositionLocalProvider(LocalAdsController provides controller)`.
- `setPooledList` thêm `poolExhausted: (() -> Boolean)? = null`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3 tiêu chí 4, KB-21 tiêu chí 2: pool cạn thì lưới không có ô trống | Compose UI test | `InListAdsTest.grid_inCollection_poolExhausted_noEmptyCell`: 7 mục, N = 3, mọi ô `flowOf(Unavailable)`, `exhausted = { true }`; `waitForIdle`. Đối chứng: `pool.calls >= 1` (ô đã được dựng và hỏi pool). `inlist_ad_0` không tồn tại; đỉnh mục 4 bằng đỉnh mục 3 (mục 4 dồn vào ô bên phải mục 3). Trên nhánh chính: `inlist_ad_0` còn, mục 4 ở 240 dp thay vì 120 dp |
| KB-21 tiêu chí 1, 4: ô đang có quảng cáo ở lại khi pool cạn | Compose UI test | `grid_inCollection_cellHoldingAd_staysWhenPoolExhausted`: lần gọi 0 là `loadedCell`, các lần sau `Unavailable`, `exhausted = { true }`. `inlist_ad_0` có 1 nút chứa "Pool ad 1"; `inlist_ad_1` không tồn tại; `released` rỗng (quảng cáo không bị trả) |
| KB-21 tiêu chí 2: quảng cáo về muộn vẫn vào ô (sau nhịp 2 giây) | Compose UI test | `grid_inCollection_poolHasAdsAgain_cellReturns`: lần gọi 0, 1 `Unavailable`, từ lần 2 `loadedCell`; `exhausted = { true }`, `hasFreeAd = { free }` với `free` là biến; đồng hồ dừng; `advanceTimeBy(500)`: `inlist_ad_0` không có. `free = true` (pool có quảng cáo rảnh); `advanceTimeBy(1_000)`: vẫn không có (đối chứng: chưa tới nhịp 2 giây). `advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100 - 1_000)`: `inlist_ad_0` có, chứa "Pool ad 3"; không nút nào chứa "Pool ad 1" hay "Pool ad 2" (ô quay lại là ô mới của pool) |
| KB-3 tiêu chí 4, KB-21 tiêu chí 2: pool hết cạn mà chưa có quảng cáo (máy không có fill) thì ô không quay lại, lưới không dịch, không ghi thêm số liệu | Compose UI test | `grid_inCollection_poolNotExhaustedWithoutAd_cellStaysOut`: `controller = testAdsController(funnel = RecordingAdFunnel())`, 5 mục, N = 3; mọi ô `Unavailable`; `hasFreeAd = { false }` suốt test; `exhausted = { cạn }` với `cạn` là biến, ban đầu `true`; ghi `onOpportunity`; đồng hồ dừng, `advanceTimeBy(500)`: ô không có, 1 `Eligible`, `opportunities == ["not_ready"]`, ghi `calls = pool.calls`. Lặp ba vòng: `cạn = false` (lượt nạp đang bay), `advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100)`, `cạn = true` (lượt nạp lỗi), `advanceTimeBy(5_000)`; sau mỗi bước: `inlist_ad_0` không có, đỉnh mục 4 bằng đỉnh mục 3, vẫn 1 `Eligible`, `opportunities` vẫn `["not_ready"]`, `pool.calls == calls`. Đối chứng: `hasFreeAd` đổi sang trả `true`, `advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100)`: ô có, 2 `Eligible`. Trên khung rỗng đỏ ở câu đầu (ô không rời lưới); với cách của app (đặt lại theo `!exhausted`) đỏ ở vòng đầu (ô quay lại) |
| Cổng mở rồi đóng (vị trí tắt rồi bật, rút đồng ý): ô quay lại ngay, quảng cáo đang giữ trả về pool | Compose UI test | `grid_inCollection_gateClosedAndOpened_cellsReturnAtOnce`: lần gọi 0 `loadedCell`, lần 1 `Unavailable`, từ lần 2 `loadingCell`; `exhausted` true; đồng hồ dừng, `advanceTimeBy(500)`: `inlist_ad_0` có, `inlist_ad_1` không. `every = null`, `Snapshot.sendApplyNotifications()`, hai khung: không còn hàng quảng cáo; `released` có đúng quảng cáo của lần gọi 0 ("Pool ad 1"). `every = 3` (pool vẫn cạn, `hasFreeAd` vẫn `false`), gửi thông báo, hai khung: `inlist_ad_0` và `inlist_ad_1` có ngay (không chờ 2 giây) |
| App ra nền giữa lúc chờ: ô không lấy quảng cáo khi app ở nền, chỉ quay lại sau khi trở lại | Compose UI test | `grid_inCollection_outInBackground_returnsOnlyAfterResume`: mọi lần gọi trước khi ra nền `Unavailable`, sau đó `loadedCell`; `exhausted` true, `free` false (`hasFreeAd = { free }`); `waitForIdle`: `inlist_ad_0` không có, ghi `callsBefore = pool.calls`. `(composeRule as AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>).activityRule.scenario.moveToState(CREATED)`; `free = true`; `mainClock.advanceTimeBy(EXHAUSTED_RETRY_MILLIS * 2)`: `pool.calls == callsBefore` (không ô nào được dựng, không lấy quảng cáo). `moveToState(RESUMED)`, `waitForIdle`: `inlist_ad_0` có, `pool.calls > callsBefore` |
| Ticket: hàng rộng hết lưới giữ hành vi cũ | Compose UI test | `grid_fullWidthRow_poolExhausted_keepsZeroHeightRow`: `placement = GALLERY_INLIST_MEDIUM`, `Unavailable`, `exhausted = { true }`: `inlist_ad_0` có 1 nút, cao 0 dp |
| Ticket: hàng của danh sách giữ hành vi cũ | Compose UI test | `lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`: `setPooledList(7, { 3 }, Unavailable, poolExhausted = { true })`: `inlist_ad_0` có 1 nút, cao 0 dp |
| KB-21 tiêu chí 2, phần "giữ khung chờ nếu vị trí khai báo giữ chỗ" | Compose UI test | `grid_inCollection_keepSpace_keepsStaticPlaceholder`: `keepSpace = true`, `Unavailable`, `exhausted = { true }`, `adCellHeight = { it * 1.5f }`. `inlist_ad_0` có, đỉnh bằng đỉnh mục 3; trong ô có đúng 1 nút `NativeAdTestTags.PLACEHOLDER` cao `chiều rộng ô x 1,5`; mục 4 xuống hàng sau. Đối chứng: `pool.calls >= 1`. Trên nhánh chính: ô cỡ 0, không có khung chờ |
| Ticket: ô cao theo thẻ của app, không lề riêng; khung chờ cùng cao | Compose UI test | `grid_inCollection_cellTakesAppHeight`: lần gọi 0 `loadedCell`, sau đó `loadingCell`; `adCellHeight = { it * 1.5f }`. Thẻ (`NativeAdTestTags.CARD` trong `inlist_ad_0`) cao `chiều rộng ô x 1,5`; ô cao bằng thẻ; đỉnh ô bằng đỉnh mục 3. Khung chờ trong `inlist_ad_1` cao `chiều rộng ô x 1,5`, ô đó cao bằng khung chờ |
| Ticket: không truyền chiều cao thì giữ 300 dp, ô thẳng hàng với mục bên cạnh | Compose UI test | `grid_inCollection_noHeight_keeps300_noOwnPadding`: mọi ô `loadedCell`. Thẻ cao 300 dp; ô cao 300 dp; đỉnh thẻ bằng đỉnh mục 3. Trên nhánh chính: ô 332 dp, thẻ ở 136 dp so với 120 dp |
| KB-3 tiêu chí 1, 2: `itemsWithFixedAds` đặt quảng cáo đúng chỗ của lưới thật | Compose UI test | `fixedAds_gridPositions_oneCellAndFullWidth`: 12 mục, `NATIVE_IN_COLLECTION`, N = 8, `columns` trong `mutableStateOf(2)`, `adContent = { Text(..., testTag("fixed_ad_$it")) }`. 2 cột: 1 quảng cáo, đỉnh bằng mục 9, cột trái, rộng nửa lưới. Đổi `columns = 3`: quảng cáo cùng hàng mục 7, 8, ở cột thứ ba; mục 9 mở hàng sau ở cột trái. Phần hai: `NATIVE_MEDIUM`, N = 3, 2 cột, 7 mục: quảng cáo sau mục 4, rộng hết lưới (như `gridInListPositions`) |
| P-07, phễu: mỗi lần ô vào lưới ghi một `eligible`; ô rời lưới khi cạn ghi `not_ready` một lần; cạn kéo dài không ghi thêm | Compose UI test | `grid_inCollection_poolCycle_reportsOncePerEntry`: `controller = testAdsController(funnel = RecordingAdFunnel())`, 5 mục, N = 3; ô đầu là `MutableStateFlow(Loading)`; đồng hồ dừng, `advanceTimeBy(500)`: ô có, 1 `Eligible`. `exhausted = true`, ô đầu `Unavailable`, `advanceTimeBy(500)`: ô không có, `opportunities == ["not_ready"]`. `advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100)`: ô vẫn không có, vẫn 1 `Eligible`. `free = true` (`hasFreeAd = { free }`), `advanceTimeBy(EXHAUSTED_RETRY_MILLIS + 100)`: ô có, 2 `Eligible`, `opportunities` vẫn `["not_ready"]` |
| `poolExhausted` của controller theo P-05 | Unit test | `AdsControllerInScreenWindowTest.poolExhausted_noPool_true_followsWindow`: `SAMPLE_NATIVE_SMALL` (định dạng không có pool) trả `true`. `GALLERY_NATIVE_MEDIUM` bật; `controller.markSdkReadyForTest()`, `openAndForeground()`, `cover()`: `true` (không gửi được, không quảng cáo rảnh, không lượt đang bay). `uncover()`, `idle()`: `false` (pool đang nạp hay gửi được). Đối chứng như `poolCanLoadNow_followsWindow`: `poolCanLoadNow()` cùng lúc lần lượt là `false`, `true`. Trước câu của pool, pool phải đã có: `collect(controller.pooledCell(GALLERY_NATIVE_MEDIUM))` (như `realController_poolRefillWhileCovered_waits`), vì `poolExhausted` không tạo pool |
| Tín hiệu đặt ô lại: chỉ khi pool có quảng cáo rảnh, không tạo pool | Unit test | `AdsControllerInScreenWindowTest.poolHasFreeAd_onlyWithFreeAd_neverCreatesPool`: `GALLERY_NATIVE_MEDIUM` bật, `markSdkReadyForTest()`, `openAndForeground()`; chưa có pool: `false`, và log không có dòng bắt đầu bằng `Pool ` chứa `created` (không tạo pool). `collect(controller.pooledCell(GALLERY_NATIVE_MEDIUM))` (pool tạo và gửi): `loader.requests.size == 1`, `poolExhausted == false` mà `poolHasFreeAd == false` (đang nạp, chưa có quảng cáo). Hủy job thu của ô, `loader.deliver(FakeLoadedNativeAd())`, `idle()`: `poolHasFreeAd == true` |
| Pool: `hasFreeAd` theo quảng cáo rảnh, không theo cờ cạn | Unit test | `NativeAdPoolTest.hasFreeAd_falseWhileRequestInFlight_trueWithFreeAd`: pool với lượt nạp treo (mẫu `FakePoolLoads` của `NativeAdPoolsTest`): `refill()`, lượt đang bay: `exhausted == false`, `hasFreeAd == false`; giao một quảng cáo: `hasFreeAd == true`; `claim()` lấy đi (lượt nạp kế tiếp treo): `hasFreeAd == false` |
| `existingPool` không tạo pool, không tra mã đơn vị | Unit test | `NativeAdPoolsTest.existingPool_neverCreates_noUnitLookup`: `unitId` đếm số lần gọi; `existingPool(GALLERY_NATIVE_MEDIUM)` là `null`, số lần gọi `unitId` không tăng, không pool nào được tạo (`loads` rỗng); sau `pool(GALLERY_NATIVE_MEDIUM)`: `existingPool` trả đúng pool đó (`assertSame`) |
| `rememberInListAds` nối hai tín hiệu của controller cho vị trí có pool | Compose UI test | `InListAdsTest.rememberInListAds_pooledPlacement_wiresControllerSignals`: `CompositionLocalProvider(LocalAdsController provides testAdsController())`, `rememberInListAds(GALLERY_IN_COLLECTION)` và `rememberInListAds(SAMPLE_NATIVE_SMALL)` giữ ra ngoài. Vị trí có pool: `poolExhausted`, `poolHasFreeAd` khác `null`, và gọi chúng trả đúng `controller.poolExhausted(GALLERY_IN_COLLECTION)`, `controller.poolHasFreeAd(GALLERY_IN_COLLECTION)` (chưa có pool: `true`, `false`). Đối chứng: `SAMPLE_NATIVE_SMALL` cả hai là `null`. Mẫu: `publicApi_withoutAdsController_addsNoAdRow` |
| `cardHeight` của thẻ và khung chờ; cỡ khác bỏ qua | Compose UI test | `NativeAdCardTest.card_cardHeight_setsCardAndPlaceholderHeight`: `Column { NativeAdCard(CARD, Modifier.width(CARD_WIDTH), cardHeight = 420.dp); NativeAdPlaceholder(CARD, Modifier.width(CARD_WIDTH), animated = false, cardHeight = 420.dp) }`: thẻ 420 dp, khung chờ 420 dp. Đối chứng: `MEDIUM` với `cardHeight = 420.dp` vẫn 410 dp |
| `cardHeight` đi qua cổng tới ô của pool (`NativeAdCell` dùng đường này) | Compose UI test | `NativeAdCellTest.gate_card_cardHeight_placeholderAdAndKeptSpace`: `NativeAdSlotGate(allowed = true, states = <MutableStateFlow(Loading)>, size = CARD, keepSpaceWhenEmpty = true, onAdAvailabilityChange = {}, modifier = Modifier.width(CARD_WIDTH).testTag(CELL), onPoolOpportunity = {}, cardHeight = 420.dp, adContent = { BoundNativeAd(it, CARD, cardHeight = 420.dp) })`: khung chờ 420 dp; `Loaded(FakeLoadedNativeAd())`: thẻ 420 dp; `Unavailable`: khung chờ tĩnh 420 dp (khung của `NativeAdCellOnScreen`) |
| Mặc định giữ ảnh chuẩn cũ | Screenshot test có sẵn | `NativeAdCardScreenshotTest`, `AdGalleryScreenScreenshotTest` (`grid_light`, `grid_small`) qua `verifyRoborazzi` không đổi ảnh nào |
| Màn xem thử dùng `itemsWithFixedAds` mà vị trí như cũ | Compose UI test có sẵn | `AdGalleryScreenTest.grid_insertsCardsAfterEveryNItems`, `grid_withoutN_showsOffTitle_noCards` giữ nguyên và phải qua |
| Tài liệu luật và hướng dẫn nói đúng cách làm mới | Kiểm tra thủ công (người: đọc tài liệu) | Đọc P-06 của `docs/ops/ads-business-rules.md` và sáu chỗ của `docs/ops/ads-guide.md` (bước 8); `python3 scripts/history-refs.py` qua |
| Trên máy: trang "Grid ads" của màn xem thử, ô thẻ thẳng hàng với mục bên cạnh và không để ô trống | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, đồng ý quảng cáo, mở trang "Grid ads": ô thẻ cùng đỉnh với mục cùng hàng, không lề 16 dp. Ô chưa có quảng cáo mà logcat có dòng chứa `cell: pool exhausted` (`pooledCellStates`, thẻ `VGL.Ads`) thì lưới không có ô trống; có quảng cáo lại thì ô xuất hiện trong khoảng 2 giây |

Thứ tự viết test của phiên (bước 3 của `auto-run.md`):

- Đỏ trên khung rỗng, đúng lý do: `grid_inCollection_poolExhausted_noEmptyCell`, `cellHoldingAd_staysWhenPoolExhausted`, `poolHasAdsAgain_cellReturns`, `gateClosedAndOpened_cellsReturnAtOnce`, `outInBackground_returnsOnlyAfterResume`, `poolNotExhaustedWithoutAd_cellStaysOut` (ô không bao giờ rời lưới, câu `assertDoesNotExist` đầu đỏ); `keepSpace_keepsStaticPlaceholder` (không có khung chờ); `cellTakesAppHeight`, `noHeight_keeps300_noOwnPadding` (ô 332 dp); `fixedAds_gridPositions_oneCellAndFullWidth` (không có quảng cáo); `poolCycle_reportsOncePerEntry` (ô không rời lưới); `poolExhausted_noPool_true_followsWindow` (khung trả `false`); `poolHasFreeAd_onlyWithFreeAd_neverCreatesPool`, `hasFreeAd_falseWhileRequestInFlight_trueWithFreeAd` (khung trả `true`); `existingPool_neverCreates_noUnitLookup` (khung trả `null` cả sau `pool()`); `rememberInListAds_pooledPlacement_wiresControllerSignals` (tín hiệu chưa nối, `null`); `card_cardHeight_*`, `gate_card_cardHeight_*` (300 dp).
- Qua ngay, khóa hành vi cũ: `grid_fullWidthRow_poolExhausted_keepsZeroHeightRow`, `lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`. Sau khi viết code (bước 4 của `auto-run.md`), phiên chạy đột biến tạm, không commit, để thấy test thất bại đúng lý do:
  - `perl -pi -e 's/\Qif (oneCell && !ads.keepSpaceWhenPoolEmpty) ads::cellShown\E/if (!ads.keepSpaceWhenPoolEmpty) ads::cellShown/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` (đột biến: hàng rộng hết lưới cũng rời lưới khi cạn), rồi `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` phải thấy 1 dòng đổi; không đổi thì mẫu không khớp, lần thử không có giá trị.
  - `./gradlew :core:ads:testDebugUnitTest --tests '*InListAdsTest.grid_fullWidthRow*'`: đỏ (`inlist_ad_0` không còn).
  - `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt`. Ghi lần chạy vào nhật ký.
  - Hàm lưới của danh sách không đổi trong plan, nên `lazyColumn_pooledRow_*` không có đột biến trong phạm vi plan: nó chặn việc đem cách bỏ ô sang danh sách về sau. Người duyệt đồng ý ngoại lệ này (dưới).
- Chạy test JVM mọi module sau bước 3: chỉ các test đỏ ở trên đỏ.

**Ngoại lệ viết test trước:** (1) `lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow` qua ngay và không có đột biến trong phạm vi plan (lý do ở trên). (2) Nối tham số công khai `cardHeight` của `NativeAdCell` vào `NativeAdSlotGate` (một dòng) cần controller có cổng mở; đoạn sau của đường đó có test `gate_card_cardHeight_*`. (3) Bước 7 đổi test staging: kiểm bằng test và ảnh chuẩn có sẵn của màn xem thử. (4) KDoc và tài liệu (bước 8) là chữ.

### 4.1. Chạy thử

Không cần (E dưới 2). Số đo ở mục 4 lấy bằng test tạm chạy `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT097MeasureTest*'` trên máy dev (JDK của `scripts/local-setup.sh`), không phải `scripts/check.sh`; test tạm đã xóa, không commit.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi cách hiện ô native trong lưới khi pool cạn, lề và chiều cao của ô), 4 (thu thập dữ liệu: không đổi tên hay tham số sự kiện, nhưng đổi thời điểm ghi `ad_opportunity` `not_ready` và thêm `af_native_ad_eligible` khi ô quay lại; mục 3.3, câu hỏi 3), 7 (module core `:core:ads`). Không chạm 2, 3, 5, 6, 8, 9. Người phụ trách sản phẩm cần biết trước khi duyệt (`risk-scoring.md`, mục 3).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Tám file code của `:core:ads`, có đổi giao diện công khai (hàm lưới, `itemsWithFixedAds` mới, `NativeAdCard`, `NativeAdPlaceholder`, `NativeAdCell`); một file test của `:app` (staging); hai tài liệu luật |
| B. Độ rõ của yêu cầu | 2 | Spec rõ ở KB-3, KB-21 và luật P-06; còn câu hỏi ảnh hưởng tới cách làm: giữ chỗ cho ô lưới (câu hỏi 2), bỏ lề 16 dp (câu hỏi 1), lưới nhảy khi ô quay lại (câu hỏi 1), số liệu (câu hỏi 3) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM (vị trí, chiều cao, thời gian, ra nền, phễu); quảng cáo thật và pool cạn thật chỉ kiểm trên máy (một bước) |
| D. Ảnh hưởng chéo | 2 | `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md` cũng do T-098, T-099 sửa; `AdsController.kt`, `NativeAdCard.kt` có thể bị T-099 chạm. Không feature nào của dự án mẫu dùng ô lưới (chỉ màn xem thử) |
| E. Độ mới của kỹ thuật | 1 | Chép bản đã chạy ở app; mẫu test có sẵn. Điểm mới nhỏ: test đưa activity ra nền trong `InListAdsTest` (đã chạy thử, mục 4) |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: hàm lưới công khai `itemsWithInListAds`, `InListAdRow`, `NativeAdCellHost`, `NativeAdSlotGate`, `NativeAdSlotContent`, `NativeAdLayout`, `BoundNativeAd` của `:core:ads` (mọi app có danh sách hay ô native); `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`.
- Task khác có thể xung đột:
  - Chỗ T-097 sửa trong `docs/ops/ads-guide.md`: đoạn pool (dòng "Medium và thẻ dọc lấy quảng cáo từ pool", khoảng dòng 59), đoạn "Native trong danh sách" (khoảng dòng 115), mục **Lưới** (117), **Ô tự đặt và trang của pager** (118), **Thuộc tính vị trí** (124), bước 5 **Test** (127). Trong `docs/ops/ads-business-rules.md`: P-06.
  - T-099 (thẻ native của bước, lần mở đầu; plan viết song song): cũng sửa mục **Thuộc tính vị trí** (khoảng dòng 124) của hướng dẫn và `AdsController.kt`, có thể cả `docs/ops/ads-business-rules.md`, `docs/lessons.md`. Chồng dòng thật ở dòng 124; ở `AdsController.kt` là chỗ khác nhau của file.
  - T-098 (thẻ hộp thoại có thưởng; plan viết song song): chỉ sửa `RewardedAction.kt` và bước 4 của mục 5 trong hướng dẫn (đoạn rewarded); không chồng dòng với T-097, chỉ cùng file `docs/ops/ads-guide.md` và `docs/lessons.md`.
  - Ticket ads khác chưa có plan: không biết.
- Đề xuất thứ tự chạy: T-097 trước, rồi T-098, T-099 nối tiếp trong cùng danh sách chạy, vì cùng sửa hai tài liệu luật; task sau dựng trên nhánh chính đã có T-097. `run-check.py` sẽ báo cùng sửa file.
- App đã có bản riêng (camera-pose): khi đưa thay đổi này về app bằng `scripts/template-sync.sh`, app xem lại bốn chỗ khác ở mục 3.1 (điều kiện quay lại theo quảng cáo rảnh, giữ chỗ khi cạn, `NativeAdCell` công khai, tín hiệu đọc pool đã có).

## 7. Điều kiện dừng riêng

- Một ảnh chuẩn có sẵn lệch sau khi viết code (`NativeAdCard_card_*`, `AdGalleryScreen_grid_*` hay ảnh khác): dừng. Plan không cho ghi lại ảnh nào; mọi mặc định phải giữ cách vẽ cũ.
- Một test có sẵn về hàng của danh sách (`InListAdsTest` với `LazyColumn`, `InListAdsStateTest`, `AdsControllerInListRowTest`) hay về pool (`NativeAdPoolTest`, `NativePoolCellTest`, `NativeAdCellTest`) đỏ sau khi viết code: dừng, vì plan không đổi danh sách hay pool.
- Cần thêm đường gửi yêu cầu quảng cáo mới (vòng 2 giây làm gì khác ngoài gọi `refill()` của pool và đọc `hasFreeAd`, hay lấy quảng cáo ngoài `pooledCellStates`): dừng.

## 8. Câu hỏi cho người duyệt

Câu 1, 2, 3 là luật hiển thị và số liệu, dành cho người phụ trách sản phẩm (anh Nhật).

1. **Ô thẻ trong lưới bỏ ô khi pool cạn và không còn lề 16 dp trên dưới: đồng ý?**
   - Khi pool cạn, ô không có quảng cáo rời lưới, các mục sau dồn lên một ô. Ô chỉ quay lại khi pool đã có quảng cáo rảnh (thấy trong khoảng 2 giây), hay khi vị trí bật lại; lúc đó các mục sau lùi một ô, người dùng đang xem có thể thấy lưới dịch một ô. Máy không có fill thì ô ở ngoài lưới suốt, lưới không dịch qua lại. Ô đang có quảng cáo không bao giờ bị bỏ.
   - Quay lại khi vị trí bật lại (sau khi tắt hay rút đồng ý rồi cho lại) có thể thành khung chờ rồi bị bỏ lần nữa nếu pool vẫn cạn: một lần dịch mỗi lần bật lại, không lặp.
   - Ô thẻ một ô không còn lề 16 dp trên dưới (lưới tự cách hàng), để thẻ thẳng hàng và cao bằng thẻ của app cùng hàng. Hàng rộng hết lưới và hàng của danh sách giữ 16 dp.
   - AI đề xuất: đồng ý cả hai, như app camera-pose đã chạy. App muốn lưới không dịch thì khai báo giữ chỗ (câu hỏi 2).
2. **Vị trí khai báo `keepSpaceWhenPoolEmpty` thì ô thẻ một ô trong lưới giữ khung chờ tĩnh thay vì rời lưới?** Câu hỏi 33 của spec đã chốt "vị trí trong lưới có khung cố định khai báo giữ chỗ khi cạn"; hiện nay hàng trong lưới bỏ qua khai báo này. App camera-pose không áp khai báo cho ô lưới.
   - AI đề xuất: áp (plan viết theo đề xuất này, test `grid_inCollection_keepSpace_keepsStaticPlaceholder`). Hàng của danh sách và hàng rộng hết lưới vẫn bỏ qua khai báo như hiện nay (ngoài phạm vi ticket).
   - Nếu chọn không áp: bỏ tham số `keepSpaceWhenPoolEmpty` của `InListAds`, bỏ test đó, sửa câu của P-06 và mục Thuộc tính vị trí; plan lên version 2.
3. **Số liệu của ô lưới đổi thời điểm ghi: đồng ý?** Không đổi tên hay tham số sự kiện. Ô đã xem mà rời lưới khi cạn ghi `ad_opportunity` `not_ready` ngay lúc rời (trước đây khi cuộn đi). Ô quay lại (pool có quảng cáo rảnh, hay vị trí bật lại) là một lần vào màn mới: thêm một `af_native_ad_eligible`, thường thêm một `shown`. Pool cạn kéo dài, kể cả khi pool đổi qua lại giữa cạn và đang nạp, thì không ghi thêm gì.
   - AI đề xuất: đồng ý, theo định nghĩa có sẵn "mỗi lần hàng vào danh sách"; test `grid_inCollection_poolCycle_reportsOncePerEntry` và `grid_inCollection_poolNotExhaustedWithoutAd_cellStaysOut` khóa đúng một lần mỗi lần vào.
4. **Không tách ticket: đồng ý?** (người duyệt)
   - AI đề xuất: không tách, lý do ở mục 1.
   - Nếu muốn tách: T-097 giữ phần bỏ ô khi cạn và giữ chỗ (bước 1, 2 phần `adShown`, bước 6, P-06, các test pool cạn); một ticket mới nhận phần chiều cao và `itemsWithFixedAds` (bước 2 phần `adCellHeight` và lề, 3 phần `itemsWithFixedAds`, 4, 5, 7, các test chiều cao và `fixedAds_*`). Phần chiều cao chạy sau phần bỏ ô vì cùng sửa `InListAdRow` và hàm lưới. Chọn tách thì plan lên version 2.
5. **`cardHeight` thêm vào `NativeAdCell` công khai, không thêm vào `NativeAdSlot` công khai: đồng ý?** (người duyệt) Ticket nêu cả ba. `NativeAdSlot` chỉ vẽ small và medium, không bao giờ vẽ thẻ; tham số ở đó không có tác dụng. `NativeAdCell` vẽ thẻ ở ô tự đặt và trang pager, nơi thẻ của app bên cạnh cũng cần chiều cao riêng.
   - AI đề xuất: đồng ý; `NativeAdSlotContent` (nội bộ) vẫn nhận `cardHeight` vì `NativeAdCellHost` dùng nó.

6. **Bản ở camera-pose có cùng hiện tượng không, và có báo lại cho app?** (người phụ trách sản phẩm, người duyệt) Bản của app đặt ô lại khi pool hết cạn (`!poolExhausted()`). Đọc code của app ở commit merge `5103888`: `NativeAdPool.exhausted` giống hệt dự án mẫu (`false` khi có lượt nạp đang bay), nên trên máy không có fill ô của app sẽ quay lại thành khung chờ rồi bị bỏ, lưới dịch mỗi 5 tới 7 giây, mỗi lần thêm `af_native_ad_eligible` và `not_ready`. AI chưa chạy thử trên app; đây là suy ra từ code. Plan khác bản của app ở điểm này.
   - AI đề xuất: giữ điều kiện "có quảng cáo rảnh" như plan; app nhận bản sửa khi lấy thay đổi về bằng `scripts/template-sync.sh` sau khi T-097 merge. Dev quyết có cần báo app sớm hơn (một issue ở repo app) hay không; AI không mở issue.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
