---
ticket: T-059
title: Native small trong danh sách thử lại khi lỗi
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5.4M         # cỡ S, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29); hai file code, hai file test, một tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-059: NATIVE SMALL TRONG DANH SÁCH THỬ LẠI KHI LỖI

## 1. Tóm tắt

Hàng native small trong danh sách hôm nay tải đúng một lần (`AdsController.loadNativeAd`, `AdsController.kt:1553-1568`): lỗi thì hàng co về 0 suốt màn, không thử lại, không chờ foreground. Plan cho mỗi hàng một luồng trạng thái đi qua `retryingNativeAdStates` (đường thử lại mà mọi khe native trong màn đang dùng), không có làm mới, nên hàng thử lại sau 5 giây (O-05), mỗi lượt gửi chờ foreground và hỏi cổng đồng ý ngay trước khi gửi, lỗi chuẩn bị yêu cầu tính mã 0 (O-09), rời màn khi yêu cầu đang bay thì quảng cáo về muộn bị hủy (O-03). Mỗi hàng vẫn giữ một quảng cáo riêng suốt màn, hủy khi rời màn. Kiểm chứng bằng unit test trên đồng hồ ảo cho từng đường gửi (lượt đầu, thử lại 5 giây, thử lại sớm khi mạng về), hai hàng tải cùng lúc, rời màn giữa chừng, và test Robolectric của danh sách giữ nguyên câu khẳng định.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | **Thêm 2026-10-29:** khi một ô native small trong danh sách tải lỗi thì ô thử lại theo cùng nhịp của native (O-05), chỉ khi app ở foreground và cổng đồng ý mở; có quảng cáo thì ô hiện. | Hàng chưa giữ quảng cáo nào, nên theo nhánh "chưa giữ quảng cáo" của O-05: thử lại cố định sau 5 giây, không bỏ cuộc, mạng về thì thử ngay (S-24). Mọi lượt gửi (cả lượt đầu) chờ foreground rồi hỏi `mayRequestNow` ngay trước khi gửi (`awaitSendAllowed`, L-072). Cổng đóng đúng lúc gửi: không gửi, không đếm lỗi, không hẹn lượt sau (S-15, như mọi khe native); hàng được dựng lại khi `every` đổi về `null` rồi trở lại (đồng ý đổi, vị trí bật lại). Quảng cáo về thì hàng hiện, từ trạng thái co 0 lên cao của native small. |
| KB-3 | Mỗi ô quảng cáo có một quảng cáo riêng; khi không còn quảng cáo nào để lấy thì ô không hiện, không để khung chờ mãi. | Giữ: mỗi hàng một luồng riêng, một lượt nạp riêng (`LIST_ROW`), không chung nguồn với khe khác. Lỗi lượt đầu thì hàng co 0 ngay (`Unavailable`), thử lại ở nền của màn; luồng không phát `Loading` lần nữa nên hàng không hiện lại khung chờ khi đang thử lại. |
| KB-3 | Khi danh sách có nhiều mục thì quảng cáo xuất hiện sau mỗi N mục...; ít hơn N mục thì không có quảng cáo; nhãn "Ad". | Không đổi (`inListAdPositions`, `inListEvery`). Test có sẵn giữ nguyên. |
| KB-24 | Khi tải native lỗi thì app thử lại...; chưa có native nào thì thử lại sau 5 giây. Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại. Khi app ở nền thì native không gửi yêu cầu. | Hàng trong danh sách là một chỗ của "native" trong tiêu chí này; hôm nay lệch cả ba ý (`ads-guide.md`, mục 6 ghi rõ "Hàng native small trong danh sách chưa theo luật này"). Sau plan: 5 giây, mạng về cắt ngắn lượt chờ (`retryResets`), ở nền không gửi. |
| KB-21 | Mỗi ô ... có một quảng cáo riêng; quảng cáo về muộn vẫn hiện vào ô. | KB-21 là ô medium và thẻ dọc lấy từ pool; ticket ghi "Không bao gồm" phần này. Đọc để giữ: hàng medium và thẻ dọc của `InListAds` vẫn đi `pooledCell`, không đổi. |

Câu hỏi đã trả lời của spec liên quan: câu 4 (danh sách có cả small và medium: small giữ đường riêng, medium từ pool), câu 19 (N là `native_every_<mã vị trí>`, không đổi), câu 33 (ô hết quảng cáo mặc định ẩn: hàng small `Unavailable` cao 0 như hôm nay).

Luật nghiệp vụ (`docs/ops/ads-business-rules.md`): O-01 (mỗi lượt hiện sở hữu một quảng cáo, rời màn thì hủy), O-03 (rời màn khi yêu cầu đang bay: quảng cáo về sau thì hủy, lỗi về sau không hẹn thử lại), O-05 (thử lại native, chờ foreground, kiểm cổng ngay trước khi gửi), O-09 (lỗi chuẩn bị yêu cầu là lỗi mã 0, thử lại theo O-05), S-24 (mạng về đặt lại thời gian chờ thử lại của native), mục 11 (thử lại native 5 giây khi chưa có quảng cáo). MC-07 và P-01 tới P-07 là của pool, không đổi. O-02 (hai chỗ cùng chờ một lượt nạp) không áp: mỗi hàng có lượt nạp riêng, không có chỗ nào chờ chung. O-04 (55 phút) không áp: hàng không có quảng cáo chờ; quảng cáo đã hiện giữ suốt màn như hôm nay (`ads-guide.md`, mục 5 "giữ khi cuộn qua lại").

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md). Giao diện của hàng (khung chờ, cao 0, 16 dp trên dưới) không đổi.

ADR liên quan: ADR-0026 (hành vi theo `ads-business-rules.md`; luật thuần tách hàm có test với đồng hồ giả), ADR-0038 (JBase không có hàng native trong danh sách thử lại riêng; quyết định của người phụ trách sản phẩm trong spec, tiêu chí thêm 2026-10-29 của KB-3, đứng trước), ADR-0029, ADR-0031 (test cũ viết lại ở mục 3.5).

## 3. Giải pháp

### 3.1. Cách làm

**Chọn đường nào.** Có hai đường thử lại sẵn có:

- (a) `retryingNativeAdStates(load, foreground, mayRequestNow, retryResets, logger, placementKey, refresh = null)` (`NativeAdRetry.kt:29-84`), mỗi hàng một luồng, `load` là `nativeLoadOnce` của chính hàng đó. Đây là đường của khe native medium cũ và lớp thu gọn trước T-030, của `nativeAd` với định dạng không có nguồn.
- (b) Nguồn một quảng cáo `NativeAdSource` qua `singleAdSlotStates` (`NativeAdSources.kt:81-119`), đường của khe native small (T-040).

Plan chọn (a), vì (b) trái ba điều mà hàng trong danh sách phải giữ:

1. (b) làm mới tại chỗ mỗi 60 giây (`refreshingNativeAdStates`, CL-02); hàng trong danh sách giữ một quảng cáo suốt màn và "Native medium và native trong danh sách không làm mới" (`ads-guide.md`, mục 6; KB-3 "mỗi ô một quảng cáo riêng").
2. (b) dùng chung một nguồn cho mọi khe của cùng (vị trí, định dạng), và mỗi lần lấy thì nạp tiếp một quảng cáo chờ (O-01, `REFRESH`): N hàng gửi N + 1 yêu cầu, quảng cáo chờ cuối bị hủy khi rời màn, và hai hàng lên màn cùng lúc rơi vào luật chỗ thua của O-02 thay vì mỗi hàng một lượt riêng.
3. (b) ghi `SINGLE_AD` (bộ đếm `load_attempt` theo vị trí); hàng trong danh sách ghi `LIST_ROW` (mỗi yêu cầu một báo cáo, câu hỏi 6 của plan T-023, `AdTelemetryTest` dòng 315, 359), để các hàng tải cùng lúc không trộn số liệu (L-068).

(a) cho đúng điều cần: mỗi lượt gửi qua `awaitSendAllowed` (lượt đầu và mọi lượt thử lại, `NativeAdRetry.kt:42`), 5 giây khi chưa giữ quảng cáo (`AdRetryPolicy.nativeDelayMillis(failures, holdsAd = false)`), `retryResets` cắt ngắn lượt chờ (S-24), lỗi ném tính mã 0 (O-09, `nativeLoadOnce`), quảng cáo đã hiện bị hủy khi luồng bị hủy (`finally`, dòng 66-72), quảng cáo về sau khi hủy bị hủy trong `loadOneNativeAd` (O-03). Không viết đường thử lại thứ hai.

**Các bước:**

1. **`InListAds.kt`:**
   - Thêm hàm thuần `internal fun inListRowStates(load: suspend () -> NativeAdState, foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean, retryResets: StateFlow<Int>, logger: AppLogger, placementKey: String): Flow<NativeAdState>` trả `retryingNativeAdStates(load, foreground, mayRequestNow, retryResets, logger, placementKey, refresh = null)`. KDoc: một hàng native small của danh sách, không làm mới, quảng cáo giữ tới khi luồng bị hủy (L-072: mọi đường gửi của hàng đi qua hàm này, hàm này đi qua `awaitSendAllowed`). Đây là chỗ duy nhất `AdsController` và test cùng gọi.
   - Thay `fun interface InListAdLoader { suspend fun load(placement): NativeAdState }` bằng `internal fun interface InListRowSource { fun states(placement: AdPlacement, slot: Int): Flow<NativeAdState> }`; tham số hàm tạo `loader: InListAdLoader?` thành `rowSource: InListRowSource?` (cùng vị trí, sau `every`).
   - `slot(index)`: giữ map `slots`, `loads`, cờ `destroyed`, điều kiện trả `Unavailable` khi `destroyed || rowSource == null`. Lần gọi đầu tạo `mutableStateOf(Loading)` và `loads[index] = scope.launch { rowSource.states(placement, index).collect { s -> if (slots[index] !== state) return@collect; state.value = s; log } }`. Log giữ dạng `in-list <mã> slot <i>: LOADED` hay `UNAVAILABLE`, ghi mỗi lần trạng thái đổi sang một trong hai (một hàng lỗi rồi có quảng cáo ghi hai dòng; luồng không phát `Unavailable` hai lần liền). Không in nội dung quảng cáo (ADR-0019).
   - `release()`: hủy mọi `loads` (luồng của hàng hủy quảng cáo đang hiện trong `finally` của `retryingNativeAdStates`, và quảng cáo về muộn trong `loadOneNativeAd`); đặt `state.value = Unavailable` cho mọi ô đã thả để hàng còn trong composition không vẽ quảng cáo đã hủy; xóa `slots`. **Bỏ** dòng `slots.values.forEach { (it.value as? Loaded)?.ad?.destroy() }`: chủ của quảng cáo là luồng, không hủy hai lần. `destroyAll()` giữ (đặt `destroyed`, gọi `release`).
   - `rememberInListAds`: `rowSource = controller?.let { ads -> InListRowSource { p, slot -> ads.inListRowStates(p, slot) } }`. Định dạng có pool giữ `pooledCell` như cũ (không gọi `slot`).
   - KDoc lớp `InListAds` thêm: hàng native small lỗi thì thử lại sau 5 giây trong lúc màn còn mở, chỉ ở foreground, cổng đồng ý mở ngay lúc gửi (O-05, T-059).
2. **`AdsController.kt`:** thay `internal suspend fun loadNativeAd(placement)` (dòng 1548-1568) bằng `internal fun inListRowStates(placement: AdPlacement, slot: Int): Flow<NativeAdState> = flow { ... }`:
   - `size = requireNotNull(placement.format.nativeLoadSize())`, `unitId(placement)` null thì `emit(Unavailable)` rồi kết thúc (như hôm nay).
   - `emitAll(inListRowStates(load = nativeLoadOnce(newEvents = { nativeAdMetrics(placement, unitId, NativeLoadUse.LIST_ROW) }, newLoader = { events -> SdkNativeAdLoader(context, adRequest(), events) }, unitId, size, NATIVE_LOAD_TIMEOUT_MILLIS, logger, placementKey = key), foreground = foregroundState, mayRequestNow = ::mayRequestNow, retryResets = preloader.nativeRetryResets, logger, placementKey = key))` với `key = "${placement.key}#$slot"`.
   - Khóa log `<mã vị trí>#<số ô>` (như `Pool native_medium#1` của T-028) để log của hai hàng tải cùng lúc không lẫn: `Native gallery_inlist_small#0 retry in 5000 ms (failures 1)`. Khóa này chỉ là log; số liệu vẫn qua `nativeAdMetrics(placement, ...)` với `placement.key` (câu hỏi 7 của plan T-023 giữ). Câu hỏi 1.
   - Bỏ log riêng `Native <mã> not requested: consent gate closed` của `loadNativeAd`: cổng đóng lúc gửi nay ghi `Native <khóa> request blocked` của `retryingNativeAdStates` như mọi khe native.
   - KDoc của `nativeAd` không đổi; KDoc hàm mới nói: một hàng small của danh sách, lượt nạp `LIST_ROW`, thử lại như khe native nhưng không làm mới, không qua nguồn một quảng cáo (plan T-059, mục 3.1).
3. **`NativeAdRetry.kt`, `NativeSendGate.kt`:** chỉ KDoc. `retryingNativeAdStates`: dòng "Without it (medium, collapsible)" thêm "an in-list small row ([inListRowStates])". `awaitSendAllowed`: danh sách đường gửi thêm "the first load and the 5 s retry of an in-list small row". Không đổi code.
4. **`docs/ops/ads-guide.md`:** mục 5 dòng 117 ("ô không có quảng cáo sau thời gian chờ thì cao 0") thêm: hàng lỗi thì thử lại sau 5 giây khi màn còn mở, chỉ ở foreground, mạng về thì thử ngay; có quảng cáo thì hàng hiện; lượt thử lại ghi `ad_load_success`, `ad_load_fail` không có `load_attempt` như lượt đầu. Mục 6 dòng 156 bỏ câu "Hàng native small trong danh sách chưa theo luật này", thay bằng "Hàng native small trong danh sách cũng vậy (T-059)". Thêm dòng log `Native gallery_inlist_small#0 retry in 5000 ms (failures 1)` vào dòng log mẫu của mục 6.

Không đổi: `NativeLoadUse` (giữ `LIST_ROW`), `nativeLoadReport`, `inListEvery`, vị trí chèn, giao diện hàng, đường pool của medium và thẻ dọc, `usesSingleAdSource`, `NativeAdSource`, vị trí quảng cáo nào.

**Hai hàng tải cùng lúc (L-068).** Cách đối tượng "hàng" được dùng hôm nay: (1) một hàng một lượt nạp; (2) nhiều hàng của cùng một vị trí lên màn cùng lúc (danh sách dài, mỗi hàng một `slot`); (3) hai vị trí xen kẽ (`itemsWithInListAds(items, listOf(small, medium))`, màn "In-list ads" của staging: hàng small là `InListAds` riêng, hàng medium từ pool). Không có kho dùng chung giữa các hàng small. Cách tính sau plan: mỗi hàng có luồng riêng, bộ đếm lỗi riêng (biến `failures` trong luồng), lượt chờ riêng; `retryResets` chung cho cả app nên mạng về thì mỗi hàng đang chờ gửi đúng một lượt; mỗi lượt có `NativeAdEvents` mới và báo cáo `LIST_ROW` mới (`nativeLoadOnce`, `perRequestLoad`), không có `load_attempt`, nên số liệu của hai hàng không trộn. Mục 4 có test cho (2) và (3).

**Rời màn và tắt giữa chừng (L-061, O-03).** `DisposableEffect` của `rememberInListAds` gọi `destroyAll()`; `every` về `null` (rút đồng ý, vị trí hay công tắc tổng tắt) gọi `release()`. Cả hai hủy luồng: đang chờ thử lại thì không gửi gì nữa; đang bay thì quảng cáo về sau bị hủy, lỗi về sau không hẹn thử lại; đã có quảng cáo thì quảng cáo bị hủy đúng một lần.

**Ra nền giữa lúc chờ (L-091).** Lượt thử lại tới hạn khi app ở nền thì chờ `foreground` (`awaitSendAllowed`), về foreground thì gửi ngay một lượt rồi hỏi cổng. Hàng không có bước chờ trước khi hiện (quảng cáo về là hiện), nên không có lượt hiện nào bị giữ khi ra nền.

**Mẫu làm theo:** `nativeAd` nhánh không nguồn (`AdsController.kt:1484-1502`) và cách `NativeAdSlot` gom luồng trạng thái trong composition; test theo `NativeAdRetryTest` (đồng hồ ảo, `FakeNativeAdLoader`, `loadOneNativeAd` thật).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/InListAds.kt` | Bước 1: `inListRowStates`, `InListRowSource` thay `InListAdLoader`, `slot` gom luồng, `release` không tự hủy quảng cáo, nối `rememberInListAds`, KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 2: `inListRowStates(placement, slot)` thay `loadNativeAd` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | Bước 3: chỉ KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeSendGate.kt` | Bước 3: chỉ KDoc |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsStateTest.kt` | Hàm dựng theo `InListRowSource`; test mới của mục 4; viết lại `unavailableSlot_notRetriedOnSameScreen` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Hàm dựng `inListAds`, `setPublicListContent`, `loadedLoader` và ba test dùng `InListAdLoader` chuyển sang `InListRowSource` (cập nhật dữ liệu, câu khẳng định giữ); test mới `failedRow_showsAdAfterRetry` |
| Sửa | `docs/ops/ads-guide.md` | Bước 4: mục 5 (dòng 117), mục 6 (dòng 156, log) |

Người đọc các ký hiệu bị đổi hay bỏ (L-020): `grep -rn "InListAdLoader\|loadNativeAd\|InListAds("` trên mọi file `.kt`, `scripts/`, `.github/`, `docs/ops/` (2026-10-29): chỉ `InListAds.kt`, `AdsController.kt` (và chú thích dòng 2041 "never through nativeAd or loadNativeAd", sửa thành "or inListRowStates"), `InListAdsTest.kt`, `InListAdsStateTest.kt`. `app/src/staging/.../AdGalleryScreen.kt` chỉ gọi `rememberInListAds` (chữ ký công khai không đổi). `scripts/tests/fixtures/release/AdPlacement.kt` chỉ chép enum vị trí, không đọc các ký hiệu này.

Người đọc `docs/ops/ads-guide.md` (L-020), `grep -rn "ads-guide" scripts/ .github/` (2026-10-29): chỉ `scripts/ads-plan.py` (dòng 373, 540, 545, 560), nhắc tên tài liệu trong chú thích và chữ in ra ("mục 3", "mục 5"), không đọc hay phân tích nội dung tệp. Plan chỉ sửa câu trong mục 5 và 6, không đổi số hay tên mục, nên `scripts/ads-plan.py` không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Công tắc `is_show_ads`, `is_show_<mã vị trí>`, `native_every_<mã vị trí>` giữ như cũ.
- Quảng cáo: vị trí đã có (`gallery_inlist_small` của staging và mọi vị trí native small trong danh sách của app); không thêm, bớt hay dời vị trí; đổi tần suất gửi yêu cầu của hàng lỗi (thêm lượt thử lại 5 giây theo O-05), đúng luật đã có, không đổi luật.
- Sự kiện phân tích: không thêm sự kiện hay tham số. Lượt thử lại ghi `ad_load_success`, `ad_load_fail` (khi bật số liệu chi tiết) như lượt đầu của hàng (`LIST_ROW`, không `load_attempt`); số sự kiện tăng theo số lượt thử lại.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Dùng `kotlinx-coroutines` (`Flow`, `StateFlow`) và Google Mobile Ads đã có trên classpath biên dịch của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Mọi phần đều là đường đã chạy ở khe native (`retryingNativeAdStates`, `nativeLoadOnce`, hủy trong `finally` khi luồng gom trong composition bị hủy, như `NativeAdSlot`); không có kỹ thuật chưa chắc.

### 3.5. Hành vi đã có bị đổi

`python3 -I scripts/existing-tests.py docs/features/ads/plans/T-059-native-trong-danh-sach-thu-lai.md` (2026-10-29): xem kết quả ở cuối mục. Đã đọc từng test của `InListAdsStateTest` và `InListAdsTest` dùng `InListAds`, `slot`, `destroyAll`, `rememberInListAds`, `InListAdRow`, `itemsWithInListAds`; `NativeAdRetryTest` (mọi test, `retryingNativeAdStates` chỉ đổi KDoc); `NativeSendGateTest` (chỉ đổi KDoc).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Hàng `Unavailable` không bao giờ tải lại trong lúc màn còn mở | Hàng lỗi thử lại sau 5 giây; gọi `slot` lần nữa (cuộn đi rồi về) không tạo luồng thứ hai | `InListAdsStateTest.unavailableSlot_notRetriedOnSameScreen` | Câu mới (ADR-0031, câu 1/3), đổi tên thành `unavailableSlot_retriedAfter5s_notOnScrollBack`: ý định "một hàng không có lượt nạp nào ngoài luật"; lượt nạp đếm ở 0 là 1, ở 4 999 ms vẫn 1, ở 5 000 ms là 2; gọi `ads.slot(0)` lần nữa ở 5 000 ms trả cùng `State` (`assertSame`) và lượt nạp vẫn 2 sau `runCurrent()`. Đối chứng: không còn câu `assertEquals(1, loader.calls)` sau lần gọi thứ hai mà thay bằng mốc thời gian, không nới |
| `InListAds.release` tự hủy quảng cáo đã tải | Luồng của hàng hủy quảng cáo khi bị hủy; `release` chỉ hủy luồng và đặt ô về `Unavailable` | `InListAdsStateTest.destroyAll_destroysEveryLoadedAd_andCancelsPendingLoads`; `InListAdsTest.loadedSlots_destroyedWhenEveryBecomesNull_andLoadAgainWhenBack` | Giữ mọi câu (mỗi quảng cáo `destroyCount` 1, không 2; lượt đang chờ bị hủy; quảng cáo về muộn không thành `Loaded`). Dữ liệu: `InListAdLoader { ... }` thành `InListRowSource` dựng bằng `inListRowStates` thật với `load` cũ, foreground `true`, cổng mở (ADR-0029, quyết định 2) |
| `InListAds` nhận `loader: InListAdLoader?` | Nhận `rowSource: InListRowSource?` | `InListAdsStateTest.slot_loadsOnce_reusedOnSecondCall`, `twoSlots_twoSeparateLoads`, `slotResult_isLoggedOnceWithoutAdContent`; `InListAdsTest`: mọi test dựng `InListAds` qua `inListAds(...)`, `setPublicListContent`, `loadedLoader` hay `InListAds(loader = ...)` (`lazyColumn_fewerItemsThanEvery_noAdRow`, `adRow_showsAdLabel`, `adRow_isSixteenDpFromItemsAboveAndBelow`, `unavailableSlot_takesNoHeight`, `loadingSlot_showsPlaceholderOfSmallHeight`, `lazyGrid_twoColumnsEveryThree_adSpansFullWidthAfterItem4`, `publicApi_withoutAdsController_addsNoAdRow`, `adRowKeys_doNotClashWithItemKeys`, `alternatingPlacements_takeTurns`, các test hàng có pool qua hàm dựng dòng 394-412 (`pooledRow_*`, `pooledRows_everyBecomesNull_releaseToPool`), `grid_inCollection_adTakesOneCell_everyNItems`, `positions_followEvery`). `lazyColumn_sevenItemsEveryThree_adRowsAfterItems3And6` không dùng `InListAds`, không đổi | Giữ mọi câu; chỉ đổi dữ liệu: `loadedLoader` thành `loadedRows = InListRowSource { _, _ -> inListRowStates({ Loaded(FakeLoadedNativeAd(sampleNativeAdAssets)) }, MutableStateFlow(true), { true }, MutableStateFlow(0), RecordingAppLogger(), "test") }`; hàng có pool không gọi `rowSource` nên chỉ đổi tên tham số. Robolectric: hàng `Unavailable` dùng `InListRowSource { _, _ -> flowOf(Unavailable) }`, hàng đang tải dùng `flow { emit(Loading); awaitCancellation() }`, để test bố cục không có vòng thử lại 5 giây chạy theo đồng hồ của Compose; hàng `Loaded` dùng `inListRowStates` thật (để `destroyCount` là của luồng). `slotResult_isLoggedOnceWithoutAdContent`: một dòng INFO vẫn đúng vì `retryingNativeAdStates` không ghi log ở lượt thành công với `load` giả |
| Cổng đóng lúc gọi: `loadNativeAd` ghi `Native <mã> not requested: consent gate closed`, trả `Unavailable` | Cổng đóng ngay trước khi gửi: `Native <mã>#<ô> request blocked`, `Unavailable`, luồng kết thúc | Không có test nào khẳng định dòng log cũ (`grep -rn "not requested"`: chỉ `RewardedTurnsTest`, không liên quan) | Không có câu nào phải đổi |
| Lượt đầu của hàng gửi ngay cả khi app ở nền | Chờ foreground | Không có test nào khẳng định | Không có câu nào phải đổi; test mới ở mục 4 |

Kết quả của `existing-tests.py` (2026-10-29): 335 test dùng tên hay tag của bốn file; "Cần xem trước" 19, "Nên xem" 194, 112 chỉ dùng tên plan không nêu. Đã đọc cả hai nhóm đầu: gần hết khớp vì tên chung `slot`, `load`, `start`, `AdsController`, `mayRequestNow` (khe native và banner của `NativeAdSlot`, `BottomAdBar`, `CollapsibleNativeAd`, pool, nạp sẵn, toàn màn hình, rewarded, `:core:assets`) chứ không gọi `InListAds.slot`, `loadNativeAd` hay đường thử lại mà plan đổi; các test đó giữ, không câu nào phải đổi. Nhóm thật sự chạm là `InListAdsStateTest`, `InListAdsTest` (bảng trên); `AdTelemetryTest.inListRows_haveNoLoadAttempt_andDoNotCount`, `inListRows_overlappingRequests_eachHasOwnLatency` (giữ: `LIST_ROW` không đổi); `NativeAdRetryTest.*`, `NativeSendGateTest.*` (giữ: chỉ KDoc đổi); `NativeEligibleCoverageTest.everyFileDrawingANativeAd_reportsEligible` (giữ: `InListAdRow` vẫn gọi `ReportNativeEligible`); `InListAdPositionsTest.inListEvery_nullWhenAdsNotAllowedOrBadValue` (giữ: `inListEvery` không đổi).

## 4. Kiểm thử

Test viết trước code (auto-run.md, bước 3). Unit test dùng `runTest` (đồng hồ ảo), luồng gom trong `CoroutineScope(StandardTestDispatcher(testScheduler))` như `InListAdsStateTest.loadScope()`; `load` của hàng là `loadOneNativeAd(FakeNativeAdLoader, ...)` thật như `NativeAdRetryTest.loadOnce()` (ghi `testScheduler.currentTime` của mỗi lượt gửi), `foreground` là `MutableStateFlow`, cổng là biến `mayRequest`, `retryResets` là `MutableStateFlow(0)`; quảng cáo là `FakeLoadedNativeAd` (`destroyCount`). Mỗi test dựng `InListAds` với `rowSource = InListRowSource { p, slot -> inListRowStates(loadOf(slot), foreground, { mayRequest }, retryResets, logger, "${p.key}#$slot") }`, tức đi qua đúng hàm mà `AdsController` gọi.

**Tái hiện lỗi (L-029).** Ở bước 3, phiên dựng khung rỗng `inListRowStates` giữ đúng hành vi hôm nay của `loadNativeAd`: `flow { if (!mayRequestNow()) { emit(Unavailable); return@flow }; emit(Loading); val s = load(); emit(s); if (s is Loaded) try { awaitCancellation() } finally { s.ad.destroy() } }` (một lượt, kiểm cổng lúc gọi, không chờ foreground, không thử lại). Test `unavailableSlot_retriedAfter5s_notOnScrollBack` là test tái hiện: kỳ vọng thất bại ở mốc 5 000 ms với `expected:<2> but was:<1>`; `firstSend_inBackground_waitsForForeground` kỳ vọng `expected:<0> but was:<1>`. **Số đo trên code hiện tại (2026-10-29, L-029):** chạy trên máy dev, nhánh chính `origin/main` (`9cb0cad`), bằng test tạm không commit `TrialInListRetryTest` trong `core/ads/src/test` (đã xóa sau khi đo). Test dựng `InListAds` thật của hôm nay với `InListAdLoader` gọi `loadOneNativeAd(FakeNativeAdLoader, ...)` thật (như `AdsController.loadNativeAd` làm, chỉ thay `SdkNativeAdLoader` bằng bản giả), `runTest` với `StandardTestDispatcher(testScheduler)` như `InListAdsStateTest.loadScope()`, đồng hồ ảo (`advanceTimeBy`, `runCurrent`); không dùng Robolectric hay `runBlocking`. Lệnh `./gradlew :core:ads:testDebugUnitTest --tests 'com.vglstudio.template.core.ads.TrialInListRetryTest'`: 11 giây, `2 tests completed, 2 failed`, đúng như kỳ vọng:
  - `unavailableSlot_retriedAfter5s` (hàng native small, lượt đầu lỗi mã 3): ô `Unavailable` ở 0 ms; mốc gửi `[0]` ở 0, 4 999, 5 000 và 60 000 ms, kể cả sau khi gọi `slot(0)` lần nữa ở 60 000 ms; log chỉ có `Native sample_native_small#0 failed: 3` và `in-list sample_native_small slot 0: UNAVAILABLE`. Thất bại: `java.lang.AssertionError: expected:<2> but was:<1>` ở câu khẳng định số lượt gửi tại 5 000 ms.
  - `firstSend_inBackground_waitsForForeground`: `InListAds` hôm nay không nhận trạng thái foreground nào, hàng gửi ngay khi được gọi; sau 60 000 ms ảo mốc gửi là `[0]`. Thất bại: `java.lang.AssertionError: expected:<0> but was:<1>`.

  Hai số đo khớp kỳ vọng của plan, nên hai test này là test tái hiện của bước 3. Nếu số đo trên khung rỗng ở bước 3 khác các số trên thì theo L4 của ADR-0032 (test viết trước khác kỳ vọng của plan), không đổi test.

Đường gửi của một hàng (L-072): (a) lượt đầu khi hàng vào danh sách; (b) lượt thử lại 5 giây sau lỗi (cả lỗi ném, O-09); (c) lượt thử lại sớm khi mạng về (`retryResets`); (d) lượt đầu của hàng mới sau khi `every` về `null` rồi trở lại. Cuộn hàng đi rồi về không phải đường gửi (cùng `State`, không luồng mới), có test riêng. Mọi đường đi qua `awaitSendAllowed` trong `retryingNativeAdStates`; mỗi đường có test ở nền và test cổng đóng.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3 thêm 2026-10-29, thử lại 5 giây (O-05, mục 11) | Unit test | `InListAdsStateTest.unavailableSlot_retriedAfter5s_notOnScrollBack` (mục 3.5); `failedRow_keepsRetryingEvery5s_neverGivesUp` (lỗi ở 0, 5 000, 10 000, 15 000 ms: bốn lượt gửi, mốc gửi đúng `[0, 5000, 10000, 15000]`, trạng thái chỉ `[Loading, Unavailable]`, không `Loading` lần hai); `failedRow_thenAd_showsLoaded_noMoreSends` (lượt hai có quảng cáo: ô thành `Loaded(ad)`, 120 giây ảo sau không lượt gửi nào, `destroyCount` 0) |
| KB-3, có quảng cáo thì ô hiện | Compose UI test (Robolectric) | `InListAdsTest.failedRow_showsAdAfterRetry`: `InListRowSource` phát `Loading`, `Unavailable`, rồi `Loaded(ad)` khi test mở một `CompletableDeferred` (không dùng đồng hồ); trước khi mở: `inlist_ad_0` cao 0 dp (đối chứng); sau khi mở: có nhãn "Ad" trong `inlist_ad_0`, cao 80 dp + 16 dp x 2 như `adRow_isSixteenDpFromItemsAboveAndBelow` |
| Đường (a) lượt đầu: ở nền | Unit test | `InListAdsStateTest.firstSend_inBackground_waitsForForeground` (`foreground = false` khi gọi `slot(0)`: 0 lượt gửi sau 60 giây ảo; `foreground = true`: 1 lượt ngay) |
| Đường (a) lượt đầu: cổng đóng | Unit test | `firstSend_gateClosed_sendsNothing_noRetry` (cổng đóng: ô `Unavailable`, 0 lượt gửi sau 60 giây ảo, log có `request blocked`); đối chứng `firstSend_gateOpen_sends` |
| Đường (b) thử lại 5 giây: ở nền | Unit test | `retry_dueInBackground_waitsForForeground` (lỗi ở 0, vào nền ở 1 000 ms: không lượt gửi nào ở 5 000 hay 30 000 ms; về foreground ở 30 000 ms: một lượt gửi ở 30 000 ms); đối chứng `retry_inForeground_sendsAt5s` |
| Đường (b) thử lại 5 giây: cổng đóng | Unit test | `retry_gateClosedAtSend_sendsNothing_endsRetries` (lỗi ở 0, đóng cổng ở 4 000 ms: không lượt gửi ở 5 000 ms và trong 60 giây sau, không log `retry in` thứ hai); đối chứng là `retry_inForeground_sendsAt5s` (cổng mở) |
| Đường (b), lỗi ném (O-09) | Unit test | `loadThrows_countsAsFailure_retriesAt5s` (`load` ném `IllegalStateException` ở lượt đầu: ô `Unavailable`, lượt hai ở 5 000 ms, `runTest` không báo lỗi chưa bắt; log có `failed: 0 (request not built)`) |
| Đường (c) mạng về (S-24) | Unit test | `networkBack_cutsRetryWaitShort` (lỗi ở 0, `retryResets.value++` ở 2 000 ms: lượt hai ở 2 000 ms, không chờ tới 5 000 ms); `networkBack_inBackground_waitsForForeground` (mạng về khi app ở nền: không gửi tới khi về foreground) |
| Đường (d) hàng mới sau khi `every` trở lại; tắt giữa chừng (L-061) | Unit test | `everyNull_duringRetryWait_sendsNothingMore` (lỗi ở 0, `every = null` ở 3 000 ms: 0 lượt gửi trong 60 giây sau); `everyBack_newRowLoads_throughGate` (`every` về 3: `slot(0)` gửi một lượt mới; cùng test với `foreground = false` thì 0 lượt tới khi về foreground) |
| Cuộn đi rồi về không gửi lại | Unit test | `slot_loadsOnce_reusedOnSecondCall` (đã có, giữ, dữ liệu mới); `loadedRow_scrollBack_sameAd_noSend` (ô `Loaded`, gọi `slot(0)` lần hai sau 120 giây ảo: `assertSame` cùng `State`, cùng `ad`, lượt gửi vẫn 1) |
| KB-3, mỗi ô một quảng cáo riêng; hai hàng cùng lúc (L-068) | Unit test | `twoSlots_twoSeparateLoads` (đã có, giữ); `twoRowsFailTogether_retryIndependently` (hàng 0 và 1 cùng lỗi ở 0: hai lượt gửi ở 5 000 ms; hàng 0 có quảng cáo, hàng 1 lỗi lần nữa: ở 10 000 ms chỉ hàng 1 gửi, tổng 5 lượt; quảng cáo của hàng 0 không sang hàng 1); `twoRows_networkBack_eachSendsOnce` (cả hai đang chờ, `retryResets` đổi: đúng 2 lượt gửi, không 4); `twoRows_logKeysDiffer` (log `retry in` của hai hàng chứa `#0` và `#1`) |
| Hai vị trí xen kẽ (L-068, cách dùng 3) | Compose UI test (Robolectric) | `InListAdsTest.alternatingPlacements_takeTurns` (đã có, giữ, dữ liệu mới) |
| Rời màn khi yêu cầu đang bay (O-03) | Unit test | `destroyAll_whileLoading_lateAdDestroyed_noRetry` (yêu cầu chưa trả lời, `destroyAll()`, rồi `loader.deliver(ad)`: `ad.destroyCount` 1, ô không `Loaded`; 60 giây sau không lượt gửi); `destroyAll_whileLoading_lateFailure_noRetry` (cùng, `loader.fail(3)`: không lượt gửi nào sau đó) |
| Rời màn thì hủy, đúng một lần (O-01) | Unit test, Compose UI test | `InListAdsStateTest.destroyAll_destroysEveryLoadedAd_andCancelsPendingLoads` (đã có, giữ: `destroyCount` là 1, không 2); `destroyAll_duringRetryWait_sendsNothing` (ô đang chờ 5 giây: 0 lượt gửi sau `destroyAll`); `InListAdsTest.loadedSlots_destroyedWhenEveryBecomesNull_andLoadAgainWhenBack` (đã có, giữ) |
| Số liệu mỗi lượt riêng (`LIST_ROW`) | Unit test | `AdTelemetryTest` dòng 315 và 359 (đã có, giữ: mỗi lượt `LIST_ROW` một báo cáo, không `load_attempt`); `NativeAdRetryTest.everyNativeLoad_attachesFreshTelemetryEvents` (đã có, giữ: `nativeLoadOnce` tạo `NativeAdEvents` mới mỗi lượt, gồm lượt thử lại) |
| Log không có nội dung quảng cáo (ADR-0019) | Unit test | `slotResult_isLoggedOnceWithoutAdContent` (đã có, giữ); `retryLogs_holdNoUnitIdOrAdText` (log của một hàng lỗi rồi có quảng cáo: có `retry in 5000 ms`, `slot 0: UNAVAILABLE`, `slot 0: LOADED`; không chứa mã đơn vị hay `headline` của quảng cáo giả) |
| Hàng thử lại trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, bật `is_show_gallery_inlist_small`; bật chế độ máy bay, mở "In-list ads": `adb logcat -v threadtime \| grep ' VGL\.Ads'` thấy `Native gallery_inlist_small#0 retry in 5000 ms (failures 1)` mỗi 5 giây, hàng small cao 0; tắt chế độ máy bay: trong vài giây có `Network back`, hàng small hiện quảng cáo có nhãn "Ad"; vào nền 30 giây trong lúc hàng đang lỗi: không có dòng gửi nào trong lúc ở nền; rời màn: `Native gallery_inlist_small#0 destroyed` cho mỗi hàng đã có quảng cáo |
| Tài liệu cho app | Kiểm tra thủ công (người: đọc tài liệu) | Đọc mục 5 dòng 117 và mục 6 của `docs/ops/ads-guide.md` khớp mục 3.1 |

**Ngoại lệ viết test trước:**

- `docs/ops/ads-guide.md`, KDoc của `NativeAdRetry.kt`, `NativeSendGate.kt`, chú thích dòng 2041 của `AdsController.kt`: tài liệu.
- Phần nối trong `AdsController.inListRowStates` (dựng `nativeLoadOnce` với `SdkNativeAdLoader`, truyền `foregroundState`, `::mayRequestNow`, `preloader.nativeRetryResets`): lớp này dựng SDK thật, không có test JVM cho nạp native. Mọi điều kiện nằm ở `inListRowStates` và `retryingNativeAdStates`, có test cho từng đường ở trên qua đúng hàm đó; phần nối chỉ truyền tham số, giống phần nối của `nativeAd` (plan T-040, mục 4). Kiểm trên máy ở dòng kiểm thủ công.
- Test đã có giữ nguyên ("đã có, giữ"): test hồi quy, qua ngay.
- Test qua ngay trên khung rỗng (L-055), mỗi test có đối chứng chạy ở bước 3, không commit riêng: `firstSend_gateClosed_sendsNothing_noRetry` (khung rỗng cũng kiểm cổng lúc gọi; đối chứng `firstSend_gateOpen_sends`); `retry_gateClosedAtSend_sendsNothing_endsRetries`, `everyNull_duringRetryWait_sendsNothingMore`, `destroyAll_duringRetryWait_sendsNothing`, `destroyAll_whileLoading_lateFailure_noRetry` (khung rỗng không thử lại nên "không gửi" qua ngay; đối chứng `retry_inForeground_sendsAt5s` đỏ trên khung rỗng); `loadedRow_scrollBack_sameAd_noSend`, `failedRow_thenAd_showsLoaded_noMoreSends` (phần "không gửi sau khi có quảng cáo" qua ngay; phần lượt hai ở 5 000 ms đỏ trên khung rỗng).
- Phần hàm phụ trợ trong file test (L-025): hàm dựng `InListAds` theo `InListRowSource` và `loadOf(slot)` vào commit test của bước 3 cùng khung rỗng `inListRowStates`, `InListRowSource`; code thật vào commit của bước 4.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi tần suất gửi yêu cầu của hàng native small trong danh sách, thêm lượt thử lại), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`), hai file code (cộng hai KDoc), đổi giao diện nội bộ `InListAds` (`InListRowSource` thay `InListAdLoader`) và bỏ `AdsController.loadNativeAd`; API công khai cho feature (`rememberInListAds`, `itemsWithInListAds`) không đổi |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, tiêu chí thêm 2026-10-29 của KB-3 và luật O-05, O-03, O-09, S-24 rõ; AI tự quyết khóa log `<mã>#<ô>` và việc cổng đóng lúc gửi kết thúc thử lại của hàng (câu hỏi 1, 3) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường gửi có unit test trên đồng hồ ảo; quảng cáo thật về sau khi mất mạng chỉ thấy trên máy |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt` mà nhiều plan đợt 2026-10-29 cũng sửa (mục 6); hành vi của hàng small trong danh sách mà màn "In-list ads" và app dùng `rememberInListAds` dựa vào, nhưng theo hướng luật đã ghi trong `ads-guide.md` |
| E. Độ mới của kỹ thuật | 0 | Lặp đúng đường `retryingNativeAdStates` và `nativeLoadOnce` đã có, không có phần mới |
| **Tổng** | 6 | **Mức: Cao** (chạm điểm chặn 1 và 7; cần duyệt chéo; người phụ trách sản phẩm biết trước vì đổi tần suất gửi yêu cầu quảng cáo) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thay `loadNativeAd`), `InListAds.kt` (giao diện nội bộ). Dùng mà không sửa: `retryingNativeAdStates`, `nativeLoadOnce`, `awaitSendAllowed`, `AdRetryPolicy`, `NativeLoadUse.LIST_ROW`.
- Task khác có thể xung đột (ticket đợt 2026-10-29, chưa có plan lúc viết; các plan đang được soạn song song):
  - T-054 (quảng cáo trong màn dừng khi native toàn màn hình che): có thể thêm điều kiện "bị che" vào cổng gửi hay luồng của khe native (`awaitSendAllowed`, `retryingNativeAdStates`). Nếu T-054 đổi chữ ký `retryingNativeAdStates` hay `awaitSendAllowed`, hàng trong danh sách đi theo vì gọi qua `inListRowStates`; ai merge sau nối chỗ gọi (ADR-0029). Nên thống nhất: hàng trong danh sách có dừng khi bị che không thì do plan T-054 quyết.
  - T-056 (mã đơn vị theo khe): có thể đổi `unitId(placement)`; hàm mới gọi `unitId(placement)` như `loadNativeAd` cũ.
  - T-063 (cổng không hiện quảng cáo cho người đã mua gói): có thể sửa `inListEvery` và `mayRequestNow`; task này không đổi hai hàm đó, chỉ gọi `::mayRequestNow`.
  - T-060, T-065 (dọn native, kích thước native theo JBase): sửa vẽ native small; task này không chạm giao diện hàng.
- Đề xuất thứ tự chạy: không phụ thuộc task nào; chạy trước T-054 để T-054 thấy sẵn hàng trong danh sách là một chỗ gọi của `retryingNativeAdStates`.

## 7. Điều kiện dừng riêng

- Để test qua mà phải cho hàng native small trong danh sách đi qua `NativeAdSource` hay làm mới 60 giây, hay đổi hàng medium, thẻ dọc (pool): dừng (ngoài phạm vi, mục 3.1).
- Phải đổi `retryingNativeAdStates`, `awaitSendAllowed` hay `AdRetryPolicy` (ngoài KDoc) để hàng thử lại đúng: dừng, vì các khe native khác dựa vào hành vi đó.

## 8. Câu hỏi cho người duyệt

1. **Khóa log của hàng:** AI đề xuất `<mã vị trí>#<số ô>` (ví dụ `Native gallery_inlist_small#0 retry in 5000 ms (failures 1)`), như `Pool native_medium#1` của T-028, để log của hai hàng không lẫn. Số liệu vẫn ghi `placement.key`. Đồng ý không?
2. **Số đo tái hiện lỗi:** đã đo trên code hiện tại 2026-10-29 (mục 4, đoạn "Số đo trên code hiện tại"): lượt gửi vẫn là 1 ở mốc 5 000 ms (`expected:<2> but was:<1>`), hàng gửi khi chưa có foreground (`expected:<0> but was:<1>`), đúng như kỳ vọng. Không còn câu hỏi ở đây; hai test đó là test tái hiện của bước 3.
3. **Cổng đóng ngay lúc gửi:** theo S-15 và như mọi khe native, hàng không gửi, ở `Unavailable` và ngừng thử lại tới khi `every` về `null` rồi trở lại. Trường hợp `mayRequestNow` false mà `inListEvery` vẫn khác `null` (SDK chưa xong khởi tạo, `sdkStarted` false lúc hàng gửi lần đầu) thì hàng không có quảng cáo suốt màn. Hôm nay `loadNativeAd` cũng vậy (trả `Unavailable`, không thử lại). AI đề xuất giữ như khe native khác (không tự thêm luật); muốn hàng chờ cổng mở lại thì thành ticket riêng áp cho mọi khe native.
4. **Lượt thử lại tăng số sự kiện `ad_load_fail`:** khi bật số liệu chi tiết, một hàng lỗi suốt màn ghi một `ad_load_fail` mỗi 5 giây (như khe native small và pool hôm nay). Người phụ trách sản phẩm biết trước điều này (điểm chặn 1)?

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
