---
ticket: T-040
title: Khe và nguồn native một quảng cáo: lấy, hủy, hết hạn, làm mới 60 giây
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M, số gợi ý 15.0M (scripts/token-usage.py estimate, 2026-10-25, tạm vì chưa đủ 5 task M); bốn file code mới, bốn file test mới, nối vào AdsController.kt
blocking_points: [ads, core, analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-040: KHE VÀ NGUỒN NATIVE MỘT QUẢNG CÁO: LẤY, HỦY, HẾT HẠN, LÀM MỚI 60 GIÂY

## 1. Tóm tắt

Phần B của T-027 (tách ngày 2026-10-25). Native nhỏ ở khe một quảng cáo (`NativeAdSlot` với vị trí `NATIVE_SMALL`) lấy quảng cáo qua một nguồn theo vị trí: lấy, nạp tiếp, thử lại khi hai chỗ cùng chờ, hủy quảng cáo về muộn, hết hạn 55 phút; khi đã có quảng cáo thì thay nội dung tại chỗ mỗi 60 giây ở foreground, nhịp rơi lúc ở nền thì bỏ (CL-02). Mọi đường gửi (sáu đường) đi qua một cổng gửi chung; lượt làm mới ghi số liệu không có `load_attempt`. Kiểm chứng bằng unit test các hàm và lớp thuần trên đồng hồ ảo của `runTest`, mỗi đường gửi một test ở nền và một test cổng đóng; quảng cáo thật kiểm trên máy.

Plan này được viết từ phần B của bản plan T-027 trước khi tách (PR #167): giữ các phần đã sửa theo rà soát (chặn 1 tới 4, nên sửa 5, 6, 9, 13, 14) và câu trả lời của anh Nhật cho mục 8 của bản đó (mọi câu theo đề xuất của AI, gồm câu 12, 13, 14). Giả định T-027 phần A (giao diện) đã merge trước.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (luật chi tiết: `docs/ops/ads-business-rules.md`, O-01 tới O-05, O-09, CL-02, mục 8.2):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi người dùng ở màn hình đủ 60 giây và app ở foreground thì nội dung quảng cáo được thay tại chỗ; nạp lỗi thì giữ quảng cáo cũ. | Sau mỗi lần khe hiện hay đổi quảng cáo, 60 giây sau có một nhịp. Nhịp rơi lúc app ở nền thì bỏ, không dồn, nhịp sau 60 giây nữa (CL-02, L-A19; câu 14 của plan T-027). Nhịp ở foreground qua cổng gửi thì lấy quảng cáo từ nguồn, đợi tối đa 10 giây (CL-02); có thì phát quảng cáo mới rồi hủy cái cũ; lỗi hay hết 10 giây thì giữ cái cũ, thử lại theo nhánh "còn giữ quảng cáo" của O-05 (2, 4, 8, 16, 32 rồi 60 giây). Chỉ native nhỏ ở khe một quảng cáo; medium, thu gọn và hàng trong danh sách giữ đường hiện tại. |
| KB-1, trường hợp đặc biệt | Khi rời màn hình thì quảng cáo được hủy, không còn giữ bộ nhớ. | O-01, O-03: lượt hiện sở hữu quảng cáo của nó; rời màn thì hủy quảng cáo đang hiện. Nguồn chỉ giữ quảng cáo chờ khi còn ít nhất một khe của vị trí trên màn (câu 12 của plan T-027); khe cuối rời màn thì nguồn hủy quảng cáo chờ, bỏ lượt đang bay (về muộn thì hủy ngay), không hẹn thử lại. |
| KB-1, trường hợp đặc biệt | Khi người dùng chưa đồng ý quảng cáo, hoặc vị trí bị tắt, thì không có vùng quảng cáo, không chiếm chỗ. | Phần giao diện là của T-027 (`nativeSlotAllowed`). Task này thêm: lượt làm mới và mọi lượt gửi khác hỏi cổng đồng ý ngay trước khi gửi (S-15); vị trí tắt hay rút đồng ý giữa chừng thì nguồn đóng, khe đang làm mới hủy quảng cáo và dừng làm mới (L-061). |

Luật của ticket, ngoài tiêu chí chấp nhận (mỗi luật có test ở mục 4):

- O-01: mỗi lượt hiện sở hữu riêng một quảng cáo; lấy ra thì nguồn tự nạp cái mới (quảng cáo chờ cho nhịp làm mới kế tiếp); rời màn thì hủy.
- O-02: hai chỗ cùng chờ một lượt nạp: chỗ thắng lấy quảng cáo, chỗ thua chờ đúng một lượt nạp nữa rồi mới nhận "không có quảng cáo"; không coi là hết quảng cáo.
- O-03: nơi hiện rời màn hay nguồn đóng khi yêu cầu đang bay: quảng cáo về sau đó thì hủy ngay; lỗi về sau đó không hẹn thử lại; chỗ đang chờ nhận "không có quảng cáo" ngay; nguồn không còn ở trạng thái "đang nạp".
- O-04: native hết hạn từ đủ 55 phút sau khi nạp; quảng cáo chờ đã hết hạn thì hủy, không đổi vào. Áp cả cho native toàn màn hình nạp sẵn (`FreshAdCache` của `AdsController`, nay 60 phút).
- O-05: lượt làm mới lỗi dùng nhánh "còn giữ quảng cáo" của `AdRetryPolicy.nativeDelayMillis(n, holdsAd = true)` (đã có từ T-021); bộ đếm về 0 khi thành công; không gửi ở nền; hỏi cổng đồng ý ngay trước khi gửi.
- O-09: chuẩn bị yêu cầu lỗi tính là lỗi mã 0 (đã có ở `nativeLoadOnce`, T-021); thêm test cho lượt làm mới.
- Mục 8.2, L-A07: lượt làm mới native nhỏ không có `load_attempt`; mỗi lượt nạp, kể cả lượt làm mới, có bộ ghi doanh thu riêng. Lượt nạp tiếp sau khi lấy ghi như lượt làm mới (`REFRESH`); lượt đầu, lượt thử lại 5 giây và lượt thử lại của chỗ thua ghi `SINGLE_AD` có `load_attempt` (câu 13 của plan T-027).
- L-072: sáu đường gửi đi qua một cổng chung, mỗi đường một test ở nền và một test cổng đóng.

Mọi cách một vị trí native nhỏ được dùng hôm nay (L-068), và cách tính của mỗi cách:

| Cách dùng | Ví dụ | Đường của task này |
| --- | --- | --- |
| Một khe `NativeAdSlot` trên màn | `SAMPLE_NATIVE_SMALL`, `GALLERY_NATIVE_SMALL` | Qua nguồn và làm mới |
| Hai khe cùng vị trí cùng lúc trên màn | (chưa có màn nào; có thể có) | Cùng một nguồn; mỗi khe một quảng cáo riêng (O-01, O-02); test hai khe chồng nhau |
| Hàng trong danh sách | `GALLERY_INLIST_SMALL` (`rememberInListAds`, `loadNativeAd`, `LIST_ROW`) | Không đổi, tới T-028 |
| Khe thu gọn, khe medium | `GALLERY_NATIVE_COLLAPSIBLE`, `GALLERY_NATIVE_MEDIUM` (`AdsController.nativeAd`) | Không đổi (`retryingNativeAdStates` không làm mới), thu gọn là của T-030, medium của T-028 |

Test nghiệm thu gợi ý `AT-NT-*` thuộc bản đặc tả độc lập của AdsKit, không có trong repo (như T-021 đã ghi); plan dùng các luật trên làm danh sách test.

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md). Task không đổi giao diện: khe vẽ quảng cáo mới bằng cùng `NativeAdCard` của T-027; không ảnh chuẩn nào đổi.

ADR liên quan: ADR-0004, ADR-0013, ADR-0019, ADR-0022, ADR-0026, ADR-0027, ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn của `:core:ads`: hàm và lớp thuần cho mọi luật thời gian, nhận đồng hồ và luồng trạng thái qua tham số (`retryingNativeAdStates`, `AdRetryPolicy`, `FreshAdCache`, T-021; vòng lặp banner của T-029). `AdsController` chỉ dựng các lớp đó với phụ thuộc thật. Mọi lớp chạy trên luồng chính (`scope` của `AdsController` là `Dispatchers.Main.immediate`).

0. **Cổng gửi chung (L-072).** `NativeSendGate.kt` (mới): `internal suspend fun awaitSendAllowed(foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean): Boolean`: chờ `foreground` là true, rồi trả `mayRequestNow()` (S-15). Mọi đường gửi yêu cầu native nhỏ gọi hàm này ngay trước khi gửi:
   - (a) lượt đầu khi khe lên màn;
   - (b) lượt nạp tiếp sau khi lấy (O-01);
   - (c) lượt thử lại của chỗ thua (O-02);
   - (d) lượt thử lại 5 giây khi chưa giữ quảng cáo (O-05);
   - (e) nhịp làm mới 60 giây (CL-02);
   - (f) lượt thử lại sau làm mới lỗi (2, 4, 8... giây, O-05).

   Trả false thì không gửi, không tính lỗi, không hẹn thử lại; đường đó kết thúc và chờ nhịp sau (e, f) hay lần khe được dựng lại (a tới d). (a), (d) hỏi cổng ở `retryingNativeAdStates` như hôm nay; nguồn hỏi lại trước khi thật sự gửi (b, c, và lượt gửi của (a), (d), (e), (f) khi không có quảng cáo chờ). Hỏi hai lần không đổi hành vi: cổng luôn được hỏi ngay trước khi gửi.
1. **`NativeAdSource.kt` (mới, internal):** nguồn một quảng cáo của một vị trí. Nhận `load: suspend (NativeLoadUse) -> NativeAdState`, `sendGate: suspend () -> Boolean` (bọc B0), `clock: MonotonicClock`, `scope: CoroutineScope`, `logger`, `placementKey`.
   - `sealed interface NativeClaim { Ad(ad: LoadedNativeAd), NoAd, Blocked }`: kết quả một lần lấy. `Blocked` là cổng đóng ngay trước khi gửi (không phải lỗi).
   - `suspend fun claim(use: NativeLoadUse): NativeClaim`:
     - có quảng cáo chờ còn hạn (dưới `MAX_AGE_MILLIS` kể từ lúc nạp, theo `clock`) thì trả nó và bắt đầu lượt nạp tiếp (b) với `REFRESH` (O-01);
     - quảng cáo chờ hết hạn thì hủy, ghi log, rồi xử lý như không có quảng cáo chờ (O-04);
     - đang có lượt nạp bay thì chờ lượt đó;
     - không có gì thì bắt đầu một lượt nạp với `use` của người lấy (qua cổng) và chờ.
   - Lượt nạp chạy trong `scope` của nguồn, không trong coroutine của người lấy: người lấy bị hủy (hết 10 giây ở làm mới, hay khe rời màn khi khe khác vẫn còn) thì lượt nạp vẫn chạy và quảng cáo về thành quảng cáo chờ.
   - Hai chỗ cùng chờ một lượt (O-02): chỗ đầu hàng chờ lấy quảng cáo; mỗi chỗ còn lại được một lượt nạp nữa (c), ghi `SINGLE_AD`, qua cổng; lượt đó lỗi thì chỗ đó nhận `NoAd`, không thêm lượt nào. Lượt nạp tiếp (b) chỉ bắt đầu khi hàng chờ trống.
   - Đếm khe: `attach()` khi khe lên màn, `detach()` khi rời màn; về 0 thì `close()`. `close()`: hủy quảng cáo chờ, hủy lượt đang bay (`loadOneNativeAd` đã hủy quảng cáo về muộn khi bị hủy coroutine, T-021), mọi chỗ đang chờ nhận `NoAd` ngay, không hẹn thử lại, trạng thái về "rỗng". `attach()` lần sau mở lại nguồn từ đầu.
   - Kết quả về khi nguồn không còn khe nào (đã đóng) thì hủy ngay (O-03); lỗi về khi đã đóng không làm gì.
   - `closed: StateFlow<Long>` (số lần đóng vì vị trí tắt hay rút đồng ý, B2) để khe đang làm mới biết phải kết thúc.
   - `object NativeAdPolicy`: `MAX_AGE_MILLIS = 55 * 60 * 1_000L`, `REFRESH_INTERVAL_MILLIS = 60_000L`, `REFRESH_WAIT_MILLIS = 10_000L`. Trễ 500 ms sau lớp thu gọn để T-030.
2. **`NativeAdSources.kt` (mới, internal):** quản lý các nguồn, test được không cần `AdsController`. Nhận qua hàm tạo: `enabled: (AdPlacement) -> Flow<Boolean>`, `canRequestAds: StateFlow<Boolean>`, `newLoad: (AdPlacement, NativeLoadUse) -> suspend () -> NativeAdState`, `sendGate: suspend () -> Boolean`, `clock`, `scope`, `logger`.
   - `source(placement)` tạo hay trả nguồn của vị trí.
   - Theo dõi `enabled(placement)` và `canRequestAds` của mỗi nguồn đã tạo: một trong hai thành false thì đóng nguồn (hủy quảng cáo chờ, lượt đang bay) và tăng `closed`, các khe đang làm mới hủy quảng cáo đang hiện và dừng làm mới (L-061). `NativeAdSlot` cũng rời composition trong trường hợp này (T-027), nhưng nguồn không dựa vào đó.
   - Hàm thuần `internal fun AdFormat.usesSingleAdSource(): Boolean = this == AdFormat.NATIVE_SMALL`, mà `AdsController.nativeAd` gọi để chọn đường.
3. **`NativeAdRefresh.kt` (mới):** `internal fun refreshingNativeAdStates(first: NativeAdState.Loaded, source: NativeAdSource, foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean, retryResets: StateFlow<Int>, clock: MonotonicClock, logger: AppLogger, placementKey: String): Flow<NativeAdState>`:
   - Phát quảng cáo đầu. Nhịp đầu ở `REFRESH_INTERVAL_MILLIS` sau đó; sau mỗi lần đổi thành công, nhịp tính lại từ lúc đổi.
   - Nhịp đến khi `foreground.value` là false: bỏ, ghi log `skipped (background)`, nhịp sau 60 giây nữa; không dồn, về foreground không có lượt làm mới ngay (CL-02, câu 14 của plan T-027).
   - Nhịp ở foreground (e): `mayRequestNow()` false thì ghi `blocked`, giữ quảng cáo, chờ nhịp sau. Ngược lại `withTimeoutOrNull(REFRESH_WAIT_MILLIS) { source.claim(REFRESH) }`. `Ad` thì phát quảng cáo mới rồi hủy cái cũ, bộ đếm lỗi về 0.
   - `NoAd`, hết 10 giây hay `load` ném lỗi (lỗi mã 0, O-09): giữ quảng cáo cũ, đếm lỗi, chờ `AdRetryPolicy.nativeDelayMillis(failures, holdsAd = true)`; `retryResets` đổi (mạng về, S-24) thì cắt ngắn lần chờ như nhánh 5 giây; rồi lượt thử lại (f) gọi `awaitSendAllowed` (ở nền thì chờ foreground) và lấy lại như nhịp. `Blocked` hay cổng đóng ở (f): giữ quảng cáo, không đếm lỗi, quay về chờ nhịp 60 giây.
   - Quảng cáo về muộn sau khi hết 10 giây thành quảng cáo chờ của nguồn; lượt thử lại 2 giây sau lấy nó ngay.
   - `source.closed` đổi: hủy quảng cáo đang hiện, phát `Unavailable`, kết thúc. Hủy collection: hủy quảng cáo đang hiện.
4. **`NativeAdRetry.kt`:** `retryingNativeAdStates` gọi `awaitSendAllowed` (B0) thay cho hai dòng hiện có (hành vi không đổi). `load` đổi thành `suspend () -> NativeAdState?`: `null` là cổng đóng ở nguồn, kết thúc như nhánh "request blocked" hiện có (không đếm lỗi, không hẹn thử lại). Thêm tham số `refresh: ((NativeAdState.Loaded) -> Flow<NativeAdState>)? = null`: khác null thì `emitAll(refresh(state))` thay vì `emit(state)` và `awaitCancellation()`; quyền hủy quảng cáo chuyển cho luồng làm mới. Nơi gọi cũ (medium, thu gọn) không truyền `refresh`, hành vi giữ nguyên.
5. **`AdTelemetry.kt`:** `enum class NativeLoadUse { SINGLE_AD, LIST_ROW, REFRESH }`; trong `nativeLoadReport`, `REFRESH -> perRequestLoad(placement.format, loadPlacement)`: mỗi lượt một báo cáo mới, không `load_attempt` (mục 8.2), vị trí là vị trí của khe; không đi qua bộ đếm lỗi của `SINGLE_AD`. Bộ ghi doanh thu gắn vào mọi lượt vì `newLoad` tạo `NativeAdMetrics` mới cho mỗi lượt (`nativeLoadOnce`, L-A07).
6. **`AdsController.kt`:**
   - Dựng `NativeAdSources(enabled = ::isEnabled, canRequestAds, newLoad = { placement, use -> nativeLoadOnce(newEvents = { nativeAdMetrics(placement, unitId, use) }, ...) }, sendGate = { awaitSendAllowed(foregroundState, ::mayRequestNow) }, clock, scope, logger)`.
   - `nativeAd(placement)`: `placement.format.usesSingleAdSource()` thì `flow { source.attach(); try { emitAll(retryingNativeAdStates(load = { source.claim(SINGLE_AD).toStateOrNull() }, ..., refresh = { refreshingNativeAdStates(it, source, ...) })) } finally { source.detach() } }`; định dạng khác giữ nguyên đường hiện tại.
   - `preloadedNativeFullScreen` dựng bằng `nativeFullScreenCache(onDiscard)` (B7) thay vì hàm tạo.
   - Không đụng khối `NativeAdOptions` của nạp sẵn native toàn màn hình (T-027 đổi sang `fullScreenNativeRequestOptions().toNativeAdOptions()`). Task này không dựng `NativeAdOptions` ở đâu: mọi lượt nạp native nhỏ đi qua `SdkNativeAdLoader`, vốn lấy tùy chọn từ `NativeRequestOptions.kt` của T-027; `NativeRequestOptionsTest.onlyNativeRequestOptions_buildsNativeAdOptions` (T-027, quét `src/main`) phải vẫn qua.
   - `loadNativeAd` của danh sách giữ nguyên tới T-028.
   - Tên hàm và trường của T-024 trong cùng file (`loadedAppOpens`, `awaitStartupAd`, `showStartupAd`, `preloadAppOpen`, `placementSwitch` trong `isEnabled`) không đụng; `isEnabled` của T-024 là thứ truyền vào `NativeAdSources`.
7. **`FreshAdCache.kt`:** thêm `internal fun <T> nativeFullScreenCache(onDiscard: (T) -> Unit) = FreshAdCache(NativeAdPolicy.MAX_AGE_MILLIS, onDiscard)` (O-04), để test kiểm đúng giá trị mà `AdsController` dùng. Mặc định 60 phút giữ cho nơi khác (`loadedAppOpens` của T-024 truyền hằng riêng).
8. **Log** bằng `AppLogger`, thẻ `Ads` (ADR-0019): `Native <key> claimed`, `Native <key> next load`, `Native <key> claim lost, retry once`, `Native <key> expired`, `Native <key> late ad destroyed`, `Native <key> source closed`, `Native <key> refresh: loaded`, `refresh: failed, retry in <ms> ms (failures <n>)`, `refresh: blocked`, `refresh: skipped (background)`. Không ghi mã đơn vị hay nội dung quảng cáo.
9. **`docs/ops/ads-guide.md`:** mục native: native nhỏ ở khe một quảng cáo thay nội dung mỗi 60 giây ở foreground (dòng 43, dòng 120 sửa "ở đợt 3" và tách native thu gọn sang T-030); thử lại khi còn giữ quảng cáo 2, 4, 8, 16, 32 rồi 60 giây (dòng 132); mỗi khe một quảng cáo riêng; log mới.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeSendGate.kt` | B0: `awaitSendAllowed` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSource.kt` | B1: `NativeAdSource`, `NativeClaim`, `NativeAdPolicy` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdSources.kt` | B2: quản lý nguồn, đóng khi vị trí tắt hay rút đồng ý; `usesSingleAdSource` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRefresh.kt` | B3: `refreshingNativeAdStates` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdRetry.kt` | B4: `awaitSendAllowed`, `load` trả `null` khi bị chặn, tham số `refresh` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdTelemetry.kt` | B5: `NativeLoadUse.REFRESH` dùng `perRequestLoad` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | B6: dựng `NativeAdSources`, `nativeAd` qua nguồn cho `NATIVE_SMALL`, `preloadedNativeFullScreen` dựng bằng `nativeFullScreenCache`; KDoc của lớp (dòng 69, 70) và của `nativeAd`. Không đụng khối `NativeAdOptions` (T-027) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FreshAdCache.kt` | B7: `nativeFullScreenCache` (55 phút) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeSendGateTest.kt` | Test cổng gửi chung |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourceTest.kt` | Test O-01 tới O-04, đường (b), (c) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSourcesTest.kt` | Test vị trí tắt, rút đồng ý giữa chừng (L-061), chọn đường theo định dạng, `use` truyền tới `newLoad` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRefreshTest.kt` | Test làm mới 60 giây, CL-02, O-05 nhánh giữ quảng cáo, O-09, đường (e), (f), số liệu |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdRetryTest.kt` | Nối sang làm mới; `load` trả `null`; các test cổng của (a), (d) giữ |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FreshAdCacheTest.kt` | `nativeFullScreenCache` 55 phút |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryTest.kt` | `NativeLoadUse.REFRESH`: không `load_attempt`, không đụng bộ đếm |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeNativeAds.kt` | Thêm hàm nạp giả theo `NativeLoadUse` (ghi `use` và giờ gửi của mỗi lượt), dùng chung cho bốn file test mới; `FakeLoadedNativeAd.destroyCount` đã có. Phần `starRating` và bộ chữ dài của T-027 giữ nguyên |
| Sửa | `docs/ops/ads-guide.md` | B9 |

Không sửa, kèm lý do:

- `NativeAdLoading.kt` (plan T-027 phiên bản 2 ghi file này cho T-040 "nếu cần"; không cần): `loadOneNativeAd` đã hủy quảng cáo về muộn khi coroutine bị hủy và khi quá thời gian chờ (`NativeAdLoadingTest.loadOne_cancelledWhileLoading_destroysLateAd`, `adAfterTimeout_isDestroyedAndNotShown`); nguồn dựa vào đó.
- `NativeRequestOptions.kt`, `SdkNativeAdLoader.kt` (T-027 tạo và sửa): dùng lại qua `SdkNativeAdLoader`, không thêm cách dựng tùy chọn.
- `NativeAdSlot.kt`, `CollapsibleNativeAd.kt`, `InListAds.kt`: gọi `ads.nativeAd(placement)` hay `loadNativeAd` như cũ; chữ ký không đổi. `NativeAdSlot.kt` là file của T-027.
- `AdPreloader.kt`: chỉ đọc `nativeRetryResets`.
- Người đọc các ký hiệu bị đổi (L-020), `grep -rn "NativeLoadUse\|retryingNativeAdStates\|FreshAdCache\|\.nativeAd(\|nativeRetryResets"` trên mọi file `.kt`, `scripts/`, `.github/`, `docs/ops/` (2026-10-25): chỉ trong `:core:ads` (`AdPreloader.kt`, `AdTelemetry.kt`, `AdsController.kt`, `CollapsibleNativeAd.kt`, `FreshAdCache.kt`, `NativeAdRetry.kt`, `NativeAdSlot.kt` và bốn file test `AdPreloaderTest`, `AdTelemetryTest`, `FreshAdCacheTest`, `NativeAdRetryTest`); không script, workflow hay tài liệu ops nào. `AdTelemetryTest` dùng `NativeLoadUse.SINGLE_AD`, `LIST_ROW` theo tên, không lặp `entries`, nên thêm `REFRESH` không làm đỏ (chạy thử ở mục 4.1).
- Ảnh chuẩn: không đổi ảnh nào; test không có `AdsController` nên không đi qua nguồn.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Làm mới bật cùng vị trí (`is_show_<mã vị trí>`), không có khóa riêng (`time_reload_collap_ad` không còn hiệu lực, `ads-business-rules.md` mục 10).
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Thêm làm mới tại chỗ mỗi 60 giây cho native nhỏ ở khe một quảng cáo (đổi tần suất: điểm chặn 1). Vị trí dùng: `SAMPLE_NATIVE_SMALL`, `GALLERY_NATIVE_SMALL` (đã có). Native toàn màn hình nạp sẵn hết hạn ở 55 phút thay vì 60 (O-04).
- Sự kiện phân tích: không thêm sự kiện hay tham số. Lượt làm mới và lượt nạp tiếp ghi `ad_load_success`, `ad_load_fail` qua `perRequestLoad`: không `load_attempt`, vị trí là vị trí của khe; luôn có bộ ghi doanh thu (mục 8.2, L-A07). Đổi cách ghi số liệu là điểm chặn 4.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Chỉ dùng `AdLoader` qua `SdkNativeAdLoader` đã có (`INTERNET`, `ACCESS_NETWORK_STATE`, `AD_ID` đã có từ Google Mobile Ads).
- **Thư viện và SDK:** Không thêm. Coroutines (`kotlinx-coroutines-core`, `kotlinx-coroutines-test`), MockK, `RecordingAppLogger` đã có trên classpath của `:core:ads` và test của nó.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** nhịp 60 giây bằng `delay`, đọc `foreground.value` lúc nhịp đến, cùng lúc đợi `claim` tối đa 10 giây bằng `withTimeoutOrNull` và lượt nạp chạy trong `scope` riêng của nguồn, có thể khó điều khiển trên đồng hồ ảo của `runTest` (lượt nạp ở `backgroundScope` khác dispatcher với luồng đang thu).
- **Điều kiện chuyển:** test `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s` hay `claimWaitsAtMost10s_thenKeepsOld` vẫn thất bại vì cùng một lỗi sau 2 lần sửa, hay một test chạy quá 60 giây thật.
- **Cách làm thay:** tách lịch nhịp thành hàm thuần `nextRefreshTickAt(lastSwapMillis, nowMillis)` trên `MonotonicClock` giả, vòng kiểm mỗi 1 giây (`delay(1_000)`) so đồng hồ với mốc; nguồn nhận `CoroutineScope` của test (`backgroundScope` với `StandardTestDispatcher(testScheduler)`). Cùng file `NativeAdRefresh.kt`, `NativeAdSource.kt`; test ở mục 4 giữ nguyên.

## 4. Kiểm thử

Test viết trước code (auto-run.md, bước 3). Thời gian dùng `runTest` (đồng hồ ảo) và `MonotonicClock` đọc `testScheduler.currentTime`; nạp dùng `FakeNativeAdLoader` (`deliver`, `fail`) và `FakeLoadedNativeAd.destroyCount`; log dùng `RecordingAppLogger`. Mỗi test chạy trên nguồn thật (không giả nguồn) trừ khi ghi khác.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 5 (mỗi 60 giây ở foreground) | Unit test | `NativeAdRefreshTest.tickAt60s_claimsNewAd_emitsIt_thenDestroysOld`; `at59s_noClaim` (đối chứng); `nextTick_countsFromLastSwap` |
| KB-1, tiêu chí 5 (ở nền thì bỏ nhịp, CL-02) | Unit test | `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s` (vào nền ở 30 giây, về foreground ở 90 giây: không lượt nào ở 60 hay 90, lượt ở 120); `backToForeground_noImmediateRefresh` |
| KB-1, tiêu chí 5 (lỗi thì giữ cũ) | Unit test | `NativeAdRefreshTest.claimWaitsAtMost10s_thenKeepsOld`; `refreshFails_keepsOldAd_retriesAt2s4s8s` (O-05, `holdsAd = true`); `refreshRetry_cutShortByRetryReset` (S-24); `refreshSucceeds_afterFailures_resetsCounter` (lỗi lần sau chờ lại 2 giây); `loaderThrows_countsAsCode0_keepsAd` (O-09); `lateAdAfter10s_becomesPending_takenByRetry` |
| KB-1, trường hợp đặc biệt (rời màn thì hủy) | Unit test | `NativeAdRefreshTest.cancelled_destroysShownAd`; `NativeAdSourceTest.claimerCancelled_whileLoading_otherSlotStillThere_adBecomesPending` (đối chứng của dòng sau); `lastSlotDetached_whileLoading_lateAdDestroyed_sourceNotLoading` (O-03); `lastSlotDetached_destroysPendingAd`; `NativeAdRetryTest.loadedAd_isDestroyedWhenCollectionEnds` (đã có, giữ) |
| KB-1, trường hợp đặc biệt (chưa đồng ý, vị trí tắt), L-061 | Unit test | `NativeAdSourcesTest.placementSwitchedOff_closesSource_destroysPending`; `consentWithdrawn_closesSource_waitersGetNoAd`; `placementSwitchedOff_whileRefreshing_destroysShownAd_noMoreRefresh` (đồng hồ ảo: 180 giây sau không có lượt nạp nào); `placementOnAgain_newSlotLoadsFromScratch` (đối chứng) |
| Cổng gửi chung (L-072), hàm | Unit test | `NativeSendGateTest.foreground_openGate_returnsTrue`; `waitsForForeground_thenReadsGate` (đọc cổng lúc về foreground, không lúc gọi); `gateClosed_returnsFalse` |
| Đường (a) lượt đầu | Unit test | `NativeAdRetryTest.firstLoad_inBackground_waitsForForeground` (đã có, giữ); `gateClosedAtSend_endsWithoutRetry` (đã có, giữ); thêm `firstLoad_blockedAtSource_endsWithoutRetry` (`load` trả `null`: không lượt thứ hai sau 60 giây, không đếm lỗi) |
| Đường (b) nạp tiếp sau khi lấy | Unit test | `NativeAdSourceTest.nextLoadAfterClaim_inBackground_waitsForForeground`; `nextLoadAfterClaim_gateClosed_sendsNothing_noRetry` |
| Đường (c) thử lại của chỗ thua | Unit test | `NativeAdSourceTest.loserRetry_inBackground_waitsForForeground`; `loserRetry_gateClosed_getsBlocked_sendsNothing` |
| Đường (d) thử lại 5 giây | Unit test | `NativeAdRetryTest.background_sendsNothingUntilForeground`, `background_control_foregroundRetriesAt5s`, `gateClosedAtSend_endsWithoutRetry`, `gateClosedAtSend_control_openGateRetriesAt5s` (đã có, giữ; đi qua `awaitSendAllowed` sau khi đổi) |
| Đường (e) nhịp làm mới | Unit test | Ở nền: `NativeAdRefreshTest.tickInBackground_isSkipped_nextTickAt120s`; cổng đóng: `tick_gateClosed_sendsNothing_keepsAd_noFailureCounted` (nhịp sau ở 120 giây vẫn gửi; đối chứng: cổng mở thì gửi ở 60 giây) |
| Đường (f) thử lại sau làm mới lỗi | Unit test | `NativeAdRefreshTest.refreshRetry_inBackground_waitsForForeground`; `refreshRetry_gateClosed_sendsNothing_keepsAd_backTo60sTick` |
| Nối cổng vào `AdsController` | Unit test | `NativeAdSourcesTest.everySend_callsSendGate` (đếm lời gọi `sendGate` bằng số lượt gửi qua (a), (b), (c) trên `NativeAdSources` với `sendGate` ghi lời gọi). `AdsController` chỉ truyền `{ awaitSendAllowed(foregroundState, ::mayRequestNow) }`: phần nối, ngoại lệ dưới bảng |
| O-01 (mỗi lượt hiện một quảng cáo riêng; lấy thì nạp tiếp) | Unit test | `NativeAdSourceTest.claim_returnsPendingAd_andStartsNextLoad`; `twoSlots_samePlacement_getDifferentAds` (hai khe chồng nhau, L-068); `sameAd_neverGivenTwice` |
| O-02 (chỗ thua thử lại một lần) | Unit test | `NativeAdSourceTest.twoClaimersOneLoad_loserRetriesOnce_getsSecondAd`; `loserRetryFails_getsNoAd_noThirdLoad` |
| O-03 (lỗi về sau khi đóng) | Unit test | `NativeAdSourceTest.failureAfterClose_schedulesNoRetry`; `waitersGetNoAd_atOnce_whenClosed` |
| O-04 (55 phút) | Unit test | `NativeAdSourceTest.pendingAdAt54m59s_isClaimed`, `pendingAdAt55m_isDestroyed_newLoadSent` (biên đúng, ghi rồi đọc lại); `FreshAdCacheTest.nativeFullScreenCache_keepsAt54m59s_dropsAt55m` (hàm mà `AdsController` gọi); `defaultCache_stillKeepsAt59m` (đối chứng: hàm tạo mặc định giữ 60 phút) |
| Chỉ native nhỏ qua nguồn và làm mới | Unit test | `NativeAdSourcesTest.onlyNativeSmall_usesSingleAdSource` (mọi `AdFormat.entries`: chỉ `NATIVE_SMALL` true); `NativeAdRetryTest.refreshNull_neverRequestsAgainAfterLoaded` (qua ngay, ngoại lệ kèm đối chứng); `refreshGiven_handsOverToRefresh`; `CollapsibleNativeAdTest` (đã có, hồi quy cho khe thu gọn) |
| Số liệu của lượt làm mới (mục 8.2, L-A07) | Unit test | `AdTelemetryTest.refreshUse_hasNoLoadAttempt_doesNotTouchCounter`; `singleAdUse_keepsLoadAttempt` (đối chứng); `NativeAdRefreshTest.everyRefresh_attachesFreshEvents` (mỗi lượt làm mới một `NativeAdEvents` mới, như `everyNativeLoad_attachesFreshTelemetryEvents`); `NativeAdSourcesTest.loadUse_firstSingleAd_nextLoadRefresh_loserRetrySingleAd` (`use` mà `newLoad` nhận ở từng đường, câu 13 của plan T-027) |
| Log (ADR-0019) | Unit test | `NativeAdSourceTest.logs_holdNoUnitIdOrAdText` (log có `claimed`, `expired`, `source closed`; không chứa mã đơn vị hay tiêu đề quảng cáo giả) |
| Làm mới 60 giây trên máy | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, bật `is_show_sample_native_small`; mở màn mẫu, chờ 70 giây ở foreground: `adb logcat -v threadtime \| grep ' VGL\.'` thấy `Native sample_native_small refresh: loaded` và quảng cáo đổi; vào nền 2 phút rồi quay lại: không có dòng `refresh` nào trong lúc ở nền, có `skipped (background)`; rời màn: `source closed` |
| Tài liệu cho app | Kiểm tra thủ công (người: đọc tài liệu) | Đọc mục native của `docs/ops/ads-guide.md` khớp mục 3.1 |

**Ngoại lệ viết test trước:**

- `docs/ops/ads-guide.md`: tài liệu.
- Phần nối trong `AdsController.kt` (dựng `NativeAdSources`, chọn đường bằng `usesSingleAdSource`, `nativeFullScreenCache`, `sendGate`): lớp này dựng SDK thật, không có test JVM. Mọi điều kiện nằm ở hàm và lớp thuần có test ở trên; mỗi đường gửi có test riêng (L-072), nên phần nối chỉ truyền tham số.
- Test đã có giữ nguyên ("đã có, giữ"): test hồi quy, qua ngay.
- Test qua ngay trên code cũ (L-055), mỗi test có bước đối chứng chạy ở bước 3, không commit: `refreshNull_neverRequestsAgainAfterLoaded` (đối chứng: cùng test với `refresh` khác null thấy lượt nạp thứ hai ở 60 giây); `defaultCache_stillKeepsAt59m` (đặt cạnh `nativeFullScreenCache_keepsAt54m59s_dropsAt55m`); `singleAdUse_keepsLoadAttempt` (đặt cạnh `refreshUse_hasNoLoadAttempt_doesNotTouchCounter`).

### 4.1. Chạy thử

- **Đã chạy thử:** Không bắt buộc (E dưới 2). Vẫn chạy thử theo bản giao việc: thêm dòng `REFRESH` vào enum dùng chung `NativeLoadUse` (L-089) và khung API: `NativeSendGate.kt` (`awaitSendAllowed`), `NativeAdPolicy`, `nativeFullScreenCache` trong `FreshAdCache.kt` và `AdsController` gọi nó, `retryingNativeAdStates` gọi `awaitSendAllowed` và nhận tham số `refresh` (mặc định `null`). Trên worktree `wt-T-040` ở commit `aa975c7` (gốc `a1cec25`, chưa có code của T-024 và T-027 phần A; nhánh plan nay dựng trên `d89a196`, chỉ thêm tài liệu), 2026-10-25, không commit, đã hoàn tác.
- **Lệnh:** `./gradlew --console=plain testDebugUnitTest testStagingDebugUnitTest --continue` (cần `ANDROID_HOME`, `JAVA_HOME` của Android Studio). Chưa chạy `scripts/check.sh` (E dưới 2, không bắt buộc).
- **Kết quả:** `BUILD SUCCESSFUL`, không lỗi biên dịch, 0 test đỏ: `:core:ads` 411 test, `:app` (staging) 84, `:feature:sample` 24, `:feature:loading` 29, `:core:network` 40, `:core:config` 39, `:core:assets` 30, `:core:designsystem` 22, `:core:analytics` 9, `:core:logging` 4. Kết luận: thêm `REFRESH` (một `when` có nhánh mới trong `nativeLoadReport`) và 55 phút cho native toàn màn hình không làm đỏ test nào hôm nay; không có test cũ ngoài mục 3.2 phải sửa. Chưa chạy trên nhánh có T-024 và T-027 phần A: phiên chạy lại toàn bộ test JVM ở bước 3 (ADR-0029, quyết định 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: thêm làm mới tại chỗ, đổi tần suất; hết hạn native toàn màn hình), 4 (số liệu: cách ghi lượt làm mới), 7 (module core `:core:ads`).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module (`:core:ads`), bốn file mới, đổi chữ ký nội bộ `retryingNativeAdStates`; API công khai cho feature không đổi |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, luật O-01 tới O-05, CL-02 rõ; các câu mở của bản trước đã có trả lời (câu 12, 13, 14); còn chi tiết nhỏ AI tự quyết (mốc nhịp sau lần đổi, mục 8 câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có unit test trên đồng hồ ảo; quảng cáo thật đổi trên màn chỉ thấy trên máy |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi của native nhỏ mà `:feature:sample` và màn xem thử dựa vào; `AdsController.kt`, `AdTelemetry.kt`, `NativeAdRetry.kt` cũng được T-024, T-027, T-028, T-030 sửa hay dùng |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu đã có (`retryingNativeAdStates`, vòng lặp banner T-029, `FreshAdCache`); hàng chờ nhiều người lấy là phần mới nhất, có phương án dự phòng |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn; cần duyệt chéo; người phụ trách sản phẩm biết trước vì đổi tần suất quảng cáo và số liệu) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.nativeAd`, `retryingNativeAdStates` (khe medium và thu gọn cũng dùng, hành vi giữ), `NativeLoadUse` và `nativeLoadReport`, `FreshAdCache`.
- Task khác có thể xung đột:
  - T-024 (app open, đang chạy, merge trước): sửa `AdsController.kt` (`loadedAppOpens`, `awaitStartupAd`, `showStartupAd`, `preloadAppOpen`, `isEnabled` qua `placementSwitch`) và `docs/ops/ads-guide.md`. Khác hàm; task này dựng trên bản đã có T-024, dùng `isEnabled` của T-024.
  - T-027 phiên bản 2 (phần A, phụ thuộc, merge trước; nhánh `claude/plan-T-027-native-adskit`, commit `d89a196`, mục 6 của plan đó đã đối chiếu). T-027 tạo `NativeRequestOptions.kt` (nơi duy nhất trong `src/main` dựng `NativeAdOptions`, có test quét mã nguồn), sửa `NativeAdSlot.kt`, `NativeAdCard.kt`, `SdkNativeAdLoader.kt`, `NativeAdViewBinding.kt`, `NativeFullScreenAdScreen.kt`, ảnh chuẩn. File cả hai cùng sửa (L-054):
    - `AdsController.kt`: T-027 chỉ khối `NativeAdOptions` của nạp sẵn native toàn màn hình; T-040 hàm `nativeAd`, trường `preloadedNativeFullScreen` (`nativeFullScreenCache`) và phần dựng `NativeAdSources`.
    - `FakeNativeAds.kt`: T-027 `starRating`, bộ chữ dài; T-040 hàm nạp giả theo `NativeLoadUse`.
    - `docs/ops/ads-guide.md`: T-027 kích thước, `onAdAvailabilityChange`, `NativeRequestOptions`; T-040 làm mới 60 giây, thử lại khi giữ quảng cáo, mỗi khe một quảng cáo.
    - T-040 tạo `NativeSendGate.kt`, `NativeAdSource.kt`, `NativeAdSources.kt`, `NativeAdRefresh.kt` và là task duy nhất sửa `NativeAdRetry.kt`, `AdTelemetry.kt` (`NativeLoadUse`), `FreshAdCache.kt`; T-027 phiên bản 2 không chạm các file đó.
  - T-031 (rewarded, plan song song): có thể sửa `AdsController.kt` và `AdTelemetry.kt` (rewarded dùng `countedLoad`, không đụng `NativeLoadUse`). Khác hàm; ai merge sau gộp.
  - T-028 (pool native): dùng `NativeLoadUse.LIST_ROW`, `perRequestLoad`, có thể dùng lại `NativeAdPolicy.MAX_AGE_MILLIS` và `awaitSendAllowed`; chạy sau task này.
  - T-030 (thanh đáy, thu gọn): làm mới của khe thu gọn và trễ 500 ms dựng trên `refreshingNativeAdStates` của task này; chạy sau.
- Danh sách chạy: nếu task khác cùng danh sách thêm `NativeAdOptions.Builder(` ngoài `NativeRequestOptions.kt`, test quét của T-027 đỏ (L-062); task này không thêm.
- Đề xuất thứ tự chạy: sau T-024 và T-027 (cùng sửa `AdsController.kt`, `docs/ops/ads-guide.md`); trước T-028, T-030.

## 7. Điều kiện dừng riêng

- Để test qua mà phải cho native medium, native thu gọn hay hàng trong danh sách đi qua nguồn hay làm mới: dừng (phạm vi T-028, T-030).
- Một ảnh chuẩn screenshot đổi: dừng; task không đổi giao diện.
- Cần khóa Remote Config mới (ví dụ chu kỳ làm mới chỉnh từ xa): dừng; CL-02 cố định 60 giây.

## 8. Câu hỏi cho người duyệt

**Đã chốt, không còn hỏi:** hết hạn 55 phút của native toàn màn hình (O-04, `nativeFullScreenCache`) thuộc T-040. Plan T-027 phiên bản 2 (`d89a196`, mục 6) ghi `FreshAdCache.kt` cho T-040 và không chạm file đó.

1. **Mốc của nhịp làm mới.** CL-02 nói "làm mới mỗi 60 giây", không nói tính từ đâu sau một lần đổi hay một chuỗi lỗi.
   - Đề xuất của AI: nhịp đầu 60 giây sau khi khe hiện quảng cáo đầu; sau mỗi lần đổi thành công, tính lại 60 giây từ lúc đổi; trong chuỗi thử lại sau lỗi thì không có nhịp 60 giây chồng lên (lượt thử lại thay cho nhịp). Phương án khác: nhịp cố định 60, 120, 180 giây kể từ lúc hiện, bất kể lần đổi.
2. **Hai khe cùng vị trí trên một màn.** Dự án mẫu chưa có màn nào như vậy; O-02 vẫn được làm và test trên nguồn.
   - Đề xuất của AI: làm như mục 3.1 (rẻ, luật có sẵn). Phương án khác: bỏ O-02, mỗi khe một nguồn riêng; đơn giản hơn nhưng lệch AdsKit.
3. **Chạy thử:** đã chạy toàn bộ test JVM với khung API (mục 4.1), không test nào đỏ; chưa chạy `scripts/check.sh` và chưa chạy trên nhánh có T-024, T-027 phần A (chưa merge).
   - Đề xuất của AI: không chạy thêm trước khi duyệt (E là 1); phiên chạy toàn bộ test JVM ở bước 3 (ADR-0029). Người duyệt muốn chạy thêm thì ghi ở mục 9.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
