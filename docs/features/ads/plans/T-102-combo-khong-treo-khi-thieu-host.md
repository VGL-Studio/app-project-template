---
ticket: T-102
title: Lượt native combo không treo khi không có FullScreenAdHost nào đang vẽ
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 21.1M        # cỡ M (ticket ghi S, plan đề xuất M ở câu hỏi 6), số gợi ý 21.1M của scripts/token-usage.py estimate (2026-11-11, trung vị 28 task M); không chỉnh
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-102: LƯỢT NATIVE COMBO KHÔNG TREO KHI KHÔNG CÓ FULLSCREENADHOST NÀO ĐANG VẼ

> **Plan chỉ được duyệt sau khi người phụ trách sản phẩm (anh Nhật) trả lời câu hỏi 1 tới 3 ở mục 8**: luật hiển thị đổi (thiếu chỗ vẽ thì không có native, trái KB-16 tiêu chí 1; native độc lập bị bỏ có sự kiện), nên spec `ads` cần thêm tiêu chí và được xác nhận lại trước.

## 1. Tóm tắt

`AdsController` đếm số `FullScreenAdHost` đang có trong cây giao diện (host tự đăng ký khi vào cây, tự gỡ khi rời cây). Lượt combo chỉ mở khi có host: sau khi interstitial rời màn, khung chờ host tối đa 1 giây (Activity vừa dựng lại kịp vẽ); không có thì đi tiếp như khi khóa combo tắt. Native toàn màn hình độc lập (`rememberShowFullScreenAd`) không có host thì bị bỏ như kiểm màn hình F-08. Lượt đã mở mà host rời cây trong lúc app ở trước quá 2 giây thì tự đóng theo đường đóng bình thường; app ở nền không tính, nên dựng lại Activity hay ra nền không làm mất lượt. Kiểm bằng test JVM với `AdsController` thật và bộ giả của `NativeFullScreenTurns`, cùng một test Compose dựng lại Activity và ra nền với host thật.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-16, tiêu chí 1 | Khi người vận hành bật native sau inter thì interstitial đóng xong có ngay một native toàn màn hình | Giữ khi vỏ app có host. Thiếu host thì hiện nay lượt treo (mục 4, số đo); plan đổi thành "không có native, đi tiếp ngay": trái chữ của tiêu chí, nên đề xuất tiêu chí mới ở câu hỏi 1 |
| KB-16, tiêu chí 4 | Khi native không tải được thì nút đóng dùng được ngay và màn tự đóng khi hết đếm ngược | Giữ nguyên. Ca này đã tự đóng trên code hiện tại kể cả khi thiếu host (mục 4, ca đối chứng), nên không dùng làm test đỏ |
| KB-16, tiêu chí 5 | Khi đóng màn native thì người dùng đi tiếp tới màn đã chọn, và khoảng cách `ads_interval` tính từ lúc này | Lượt đóng vì host rời đi đường đóng bình thường: đẩy mốc lúc đóng (K-05). Combo bị bỏ vì thiếu host thì không mở, mốc giữ ở lúc interstitial đóng (I-02), như khi khóa tắt |
| KB-16, tiêu chí 6 | Khi khóa tắt thì interstitial đóng xong người dùng đi tiếp ngay | Thiếu host đi đúng đường này: hàm hoàn tất chạy, chặn quảng cáo khi quay lại app được thả, cờ đang hiện (F-01) được hạ |
| KB-5, tiêu chí 4 | Khi chưa có quảng cáo sẵn thì không hiện gì và không chặn người dùng | Mở rộng cho ca thiếu host (câu hỏi 2): không hiện gì, không chặn người dùng, quảng cáo nằm lại trong khe |
| KB-15, tiêu chí 4 | Không bao giờ có hai quảng cáo toàn màn hình chồng nhau; lượt bị chặn thì bỏ | Lượt treo hiện nay giữ cờ đang hiện tới trần 10 phút (F-02), nên interstitial sau bị coi là bận (mục 4, số đo). Sau khi sửa, interstitial kế tiếp không bị coi là bận |
| KB-26, tiêu chí 3 | Khi màn hình của app không còn dùng được lúc gọi hiện thì quảng cáo không được gọi hiện, vẫn giữ trong khe; người dùng đi tiếp ngay và có một sự kiện lỗi hiển thị `app_not_foreground` | Native độc lập thiếu host coi như màn hình không dùng được (câu hỏi 2): cùng kết quả và cùng sự kiện |
| KB-26, tiêu chí 4 | Khi người dùng xoay máy lúc quảng cáo toàn màn hình đang hiện thì hành động sau quảng cáo chỉ chạy khi quảng cáo đóng, đúng một lần | Host rời cây rồi vào lại khi dựng lại Activity: lượt giữ nguyên, hàm hoàn tất chạy một lần khi đóng |
| KB-27 | Banner và native bên dưới dừng khi native toàn màn hình hiện, chạy lại khi nó đóng | Lượt treo giữ cờ nên quảng cáo trong màn dừng mãi. Sau khi sửa, cờ hạ ở mọi đường kết thúc mới; không đổi luật |

Phần (b) của issue #445 (công tắc cho `resume_inter` nối combo) không làm: mẫu đã không nối combo sau interstitial của resume (A-12, KB-14, KB-16 tiêu chí 3).

Nguồn ở app: PR #313 của camera-pose (đọc được bằng `gh api`). App sửa bằng cách đặt `FullScreenAdHost()` vào `MainActivity` và thêm test đọc mã nguồn chỗ đặt; khung của app vẫn mở lượt khi thiếu host. Plan này sửa ở khung. Mẫu test `FullScreenAdHostComboTest` của app (dựng `AdsHost` cùng `FullScreenAdHost()` công khai với `AdsController` thật) được dùng lại cho test Compose ở mục 4.

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; docs/ops/design-workflow.md)

ADR liên quan: ADR-0045 (bộ PARTNER, mục 7.5 đếm ngược native), ADR-0029, ADR-0031 (cập nhật dữ liệu test cũ, mục 3.5), ADR-0019 (log).

## 3. Giải pháp

### 3.1. Cách làm

Quyết định: **cả hai** cách của ticket, mỗi cách cho một thời điểm.

- Lúc mở lượt: **không mở** khi không có host. Lý do: chưa lấy quảng cáo ra khỏi khe, chưa ghi cơ hội, chưa ghi `eligible`, nên thiếu host không tốn quảng cáo và không sinh số liệu giả. Tự đóng sau một thời hạn thì lượt đã lấy và hủy một quảng cáo không ai thấy, và ghi `shown` sai.
- Lúc lượt đang mở: **tự đóng** khi host rời cây quá 2 giây trong lúc app ở trước. Lý do: lượt đã hợp lệ (có host lúc mở), người dùng đã thấy nó; đường đóng bình thường giữ đúng K-05, K-07. Đếm chỉ khi app ở trước, nên dựng lại Activity (host rời rồi vào lại trong cùng một khung hình) hay ra nền (Activity có thể bị hủy rồi tạo lại khi quay về) không làm mất lượt.

**Biết có host (`AdsController`, `NativeFullScreenAdHost.kt`):**

- `AdsController`: thêm `private var fullScreenHostCount = 0` và `private val _fullScreenHostAttached = MutableStateFlow(false)`, lộ ra `internal val fullScreenHostAttached: StateFlow<Boolean>`. Thêm `internal fun attachFullScreenHost(): () -> Unit`: tăng bộ đếm, đặt `true`, ghi log `Full-screen host attached (count: N)`; trả về hàm gỡ chỉ chạy một lần (gọi lần hai không làm gì), giảm bộ đếm, đặt `false` khi về 0, ghi log `Full-screen host detached (count: N)`. Luồng chính, như mọi trạng thái khác của controller.
- `FullScreenAdHost()`: sau dòng `val ads = LocalAdsController.current ?: return`, thêm `DisposableEffect(ads) { val detach = ads.attachFullScreenHost(); onDispose { detach() } }`, rồi `NativeFullScreenOverlayHost(...)` như cũ. KDoc ghi: host đăng ký khi vào cây; không có host nào thì combo không mở và native độc lập bị bỏ.
- "Có host" là có ít nhất một host trong một cây giao diện còn sống, không theo Activity nào (test của controller không có Activity resumed). **Giới hạn đã biết (câu hỏi 7):** app nhiều Activity, Activity phía trên không có host còn Activity bên dưới có: khung thấy có host, nên lượt mở và `watchHost` không đóng nó; lớp phủ chỉ vẽ khi Activity có host lên lại (thu theo vòng đời). Trong lúc đó lượt giữ cờ đang hiện (tới trần 10 phút của F-02), chặn quảng cáo khi quay lại app, và hàm hoàn tất chưa chạy: lượt treo tới khi người dùng quay về Activity có host. Plan không sửa ca này (cần biết host thuộc Activity nào và Activity nào đang resumed, mà test của controller không có); HD-05 và `ads-guide.md` ghi: mọi Activity gọi quảng cáo toàn màn hình phải có host.

**`NativeFullScreenPort` thêm hai thành phần:**

```kotlin
/** True while at least one FullScreenAdHost is in a composition (HD-05). */
val hostAttached: StateFlow<Boolean>

/** True while an activity of the app is started ([AdsActivityTracker.foreground]). */
val foreground: StateFlow<Boolean>
```

Ở `AdsController`: `hostAttached = fullScreenHostAttached`, `foreground = foregroundState`.

**`NativeAfterInterRules.kt`:** thêm `internal const val HOST_WAIT_MILLIS = 1_000L`, `internal const val HOST_GONE_GRACE_MILLIS = 2_000L` và

```kotlin
/** Waits at most [HOST_WAIT_MILLIS] for a FullScreenAdHost; true when one is there. Uses the time of the coroutine. */
internal suspend fun awaitHost(attached: StateFlow<Boolean>): Boolean =
    attached.value || withTimeoutOrNull(HOST_WAIT_MILLIS) { attached.first { it } } != null
```

**`NativeFullScreenTurns`:**

- `afterInterstitial`: sau `val gone = awaitInterGone(port::interGone)` và trước `open(current)`, thêm `if (!awaitHost(port.hostAttached)) { skip(current, "no host"); return@launch }`. Gộp nhánh "off" hiện có vào cùng hàm riêng `skip(current: Turn, reason: String)`: `turn = null`, `port.releaseResumeAds(current.hold)`, `port.endPresence()`, log `Native combo skipped: <reason>`, `current.onFinished()`. Nhánh off giữ đúng log cũ `Native combo skipped: off`. Không ghi mốc `ads_interval`, không ghi cơ hội, không lấy quảng cáo, không nạp, không gọi `loadStateAtOpen` (nên không có `eligible` của native). Log mở giữ `Native combo opened (inter gone: $gone)`. Ra nền giữa lúc chờ host: chờ vẫn giới hạn 1 giây theo thời gian của coroutine (không dừng khi ở nền); hết 1 giây mà chưa có host thì bỏ ngay lúc đó, nên người dùng quay lại sau nhiều phút không gặp native hiện muộn. Có host lúc ở nền (cây giao diện còn sống khi Activity chỉ dừng) thì lượt mở như hiện nay: đếm ngược theo đồng hồ tuyệt đối (K-02), quay lại thấy nút đóng.
- `open(current)`: thêm `current.jobs += scope.launch { watchHost(current) }`, cho cả combo và lượt độc lập:

```kotlin
private suspend fun watchHost(current: Turn) {
    combine(port.hostAttached, port.foreground) { attached, foreground -> !attached && foreground }
        .distinctUntilChanged()
        .collectLatest { lost ->
            if (!lost) return@collectLatest
            delay(HOST_GONE_GRACE_MILLIS)
            logger.info(TAG, "Native full-screen closed: host gone")
            closeTurn(current)
        }
}
```

  `closeTurn` là đường đóng có sẵn: mốc trước tiên (I-05), hủy quảng cáo, nhả chặn, hạ cờ, cơ hội của combo (`shown` nếu đã có quảng cáo, không thì `not_ready`, K-07), nạp lại, hàm hoàn tất. Không đi qua `close()` vì nút đóng có thể chưa dùng được (đang đếm ngược): không ai thấy màn, không có gì để chờ. `closeTurn` hủy mọi job của lượt, kể cả chính `watchHost`, sau khi đã ghi xong (lời gọi hủy không dừng phần đang chạy đồng bộ).
- `showStandalone`: không đổi điều kiện; kiểm host nằm ở controller (dưới), `watchHost` chạy qua `open`.

**Native độc lập (`AdsController.showNativeFullScreen`):** sau khi cổng trả `SHOW` và trước `beginStandaloneNativeTurn` (trước khi bật cờ và lấy quảng cáo), thêm theo đúng thứ tự: (1) `if (nativeTurns.busy) return@withContext skipTurn(placement, BUSY, loadState, onFinished)` (một lượt native còn mở quá trần F-02 vẫn là `busy`, như hiện nay, kể cả khi thiếu host); (2) `if (!fullScreenHostAttached.value) return@withContext skipNoOverlayHost(placement, onFinished)`. Nhánh `busy` hiện có sau `beginStandaloneNativeTurn` giữ nguyên cho cờ F-01. Hàm mới `skipNoOverlayHost` ghi log `Full-screen <key> skipped: no full-screen host`, rồi làm đúng như `skipHostGone` (F-08): cơ hội `shown`, `ad_show_fail` mã 3 `app_not_foreground`, hàm hoàn tất chạy ngay một lần; không cờ, không mốc, không nạp, quảng cáo nằm lại trong khe. `beginStandaloneNativeTurn` giữ nguyên (test khác gọi thẳng nó).

**Ghi gì cho từng đường (câu hỏi 3 của rà soát ticket):**

| Đường | Mốc `ads_interval` | `ad_opportunity` | `ad_show_fail` | Nạp lại | Hàm hoàn tất | Cờ F-01, chặn resume |
| --- | --- | --- | --- | --- | --- | --- |
| Combo, có host (giữ) | Lúc đóng combo (K-05) | Một lần (K-07) | Không | `native_combo` | Lúc đóng | Giữ tới lúc đóng |
| Combo, thiếu host (mới) | Không thêm; mốc ở lúc interstitial đóng (I-02), như khóa tắt | Không (như khóa tắt) | Không | Không (quảng cáo còn trong khe) | Sau tối đa 1 giây chờ host | Hạ, nhả ngay |
| Lượt đang mở, host rời quá 2 giây khi app ở trước (mới) | Lúc đóng | Combo: một lần; độc lập: không (như cũ) | Không | Như đóng bình thường | Lúc đóng | Hạ, nhả |
| Native độc lập, thiếu host (mới) | Không | `shown` (như F-08) | Mã 3 `app_not_foreground` | Không | Ngay | Không bật |

Sự kiện là tên và giá trị đã có; chỉ dòng cuối gửi sự kiện ở một ca mới (điểm chặn 4, câu hỏi 2).

**Tài liệu:**

- `docs/ops/ads-business-rules.md`: (1) K-09 mới, sau K-08: combo chỉ mở khi có bộ chứa lớp phủ (HD-05) trong cây giao diện; sau khi inter rời màn chờ bộ chứa tối đa 1 giây; không có thì đi tiếp như khóa tắt (không native, không cơ hội, không thêm mốc, không nạp), có log. Lượt native toàn màn hình đang mở (combo hay độc lập) mà không còn bộ chứa nào trong lúc app ở trước đủ 2 giây thì đóng như bấm đóng; thời gian app ở nền không tính, dựng lại Activity không mất lượt. Cột nguồn: "Không có (AdsKit không có; issue from-app #445)". (2) F-08: thay vế "Native toàn màn hình vẽ trong cây Compose, không có kiểm này" bằng: native toàn màn hình độc lập kiểm có bộ chứa lớp phủ thay cho kiểm Activity, không có thì kết quả như trên (`shown`, `ad_show_fail` mã 3). (3) HD-05, cột hậu quả: thay "Combo không hiện được" bằng "Combo và native toàn màn hình độc lập không bao giờ hiện: combo bị bỏ như khi khóa tắt, lượt native độc lập bị bỏ với `ad_show_fail` `app_not_foreground`; mỗi Activity gọi quảng cáo toàn màn hình phải có bộ chứa". Cột việc thêm: đặt trong mọi Activity của app có thể gọi quảng cáo toàn màn hình; Activity phía trên không có bộ chứa trong khi Activity bên dưới có thì combo mở và chờ tới khi người dùng quay về Activity có bộ chứa (câu hỏi 7). (4) Mục 8.2, dòng về `ad_show_fail` mã 3: thêm ca native độc lập thiếu bộ chứa. (5) K-01: sau câu "Inter đã qua cổng thì khi hoàn tất mở màn native toàn màn hình, kể cả khi inter không lên được màn", thêm "trừ khi không có bộ chứa lớp phủ (K-09)". (6) K-02: "Bắt đầu khi inter đã rời màn hẳn" thành "Bắt đầu khi inter đã rời màn hẳn và có bộ chứa lớp phủ (K-09)". (7) Mục 11, dòng "Đếm ngược native sau inter; chờ inter rời màn": thêm hai giá trị "chờ bộ chứa lớp phủ 1 giây; mất bộ chứa khi app ở trước 2 giây thì đóng (K-09)", cột nguồn thêm "Không có". Câu chữ cuối cùng theo câu trả lời của mục 8.
- `docs/ops/ads-guide.md`, mục 5 ý gọi hàm có sẵn (dòng "Native toàn màn hình hiện trong hộp thoại do `FullScreenAdHost()` của `:app` vẽ"): thêm "`:app` đặt `FullScreenAdHost()` một lần ở gốc giao diện của mọi Activity gọi quảng cáo toàn màn hình; thiếu thì mất combo và native toàn màn hình độc lập, luồng vẫn đi tiếp (luật K-09, HD-05)". Mục 9 thêm một dòng kiểm: app bật `is_show_native_after_inter` hay có vị trí native toàn màn hình thì mọi Activity gọi quảng cáo toàn màn hình có `FullScreenAdHost()`; log khi mở app có dòng `Full-screen host attached`.

Không sửa spec (spec `confirmed`): tiêu chí mới đề xuất ở mục 8, người phụ trách sản phẩm sửa hay cho AI sửa ở PR riêng.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurns.kt` | Port thêm `hostAttached`, `foreground`; `afterInterstitial` chờ host, `skip(current, reason)` gộp nhánh off và nhánh thiếu host; `open` chạy `watchHost`; KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAfterInterRules.kt` | `HOST_WAIT_MILLIS`, `HOST_GONE_GRACE_MILLIS`, `awaitHost` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bộ đếm host, `fullScreenHostAttached`, `attachFullScreenHost()`, hai thành phần mới của port, kiểm host trong `showNativeFullScreen`, `skipNoOverlayHost`; KDoc của lớp (đoạn combo) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdHost.kt` | `FullScreenAdHost()` đăng ký và gỡ host bằng `DisposableEffect`; KDoc |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurnsTest.kt` | `FakeNativeFullScreenPort` thêm `hostAttached`, `foreground` (mặc định có host, ở trước); test mới của mục 4 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeComboTest.kt` | Dựng: đăng ký host khi dựng controller của lớp (trường `detachHost`, mục 3.5); test mới của mục 4 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerFunnelTest.kt` | Dựng: đăng ký host trong hàm dựng `controller(...)` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInScreenWindowTest.kt` | Dựng: đăng ký host khi dựng controller của lớp (mục 3.5; app ở trước, `cover()` mở lượt độc lập) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInListRowTest.kt` | Dựng: đăng ký host khi dựng controller của lớp (mục 3.5; app ở trước, `cover()` mở lượt độc lập) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Dựng: đăng ký host trong hàm dựng `controller()` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerStartupInterComboTest.kt` | Dựng: đăng ký host khi dựng controller của lớp (mục 3.5) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdHostAttachTest.kt` | Test Compose với `AdsHost` cùng `FullScreenAdHost()` thật: đăng ký và gỡ, đổi vỏ, dựng lại Activity, ra nền rồi trở lại (mục 4) |
| Sửa | `docs/ops/ads-business-rules.md` | K-09 mới; K-01 (dẫn ngoại lệ K-09); K-02 (đếm ngược bắt đầu khi có bộ chứa); F-08; HD-05 (việc và cột hậu quả); mục 8.2 dòng `ad_show_fail` mã 3; mục 11 dòng hằng số của native sau inter (1 giây chờ bộ chứa, 2 giây khi mất bộ chứa) |
| Sửa | `docs/ops/ads-guide.md` | Mục 5 (vỏ app đặt host ở mọi Activity gọi quảng cáo toàn màn hình), mục 9 (dòng kiểm trước khi phát hành) |

Không ghi `docs/lessons.md`, file plan này hay ticket: plan cho phiên tự chạy, phiên ghi bài học `applied`, nhật ký và trạng thái qua PR báo cáo. Không ảnh chuẩn screenshot nào đổi: lớp phủ vẽ như cũ, chỉ thêm `DisposableEffect`. Nơi đọc các file bị sửa: `NativeEligibleCoverageTest` liệt kê `NativeFullScreenAdHost.kt` là ngoại lệ theo tên file (tên giữ nguyên); `scripts/ads-plan.py` đọc `AdPlacement.kt` và `remote-config-keys.md`, không đọc các file này; `docs/ops/ads-business-rules.md` được `scripts/history-refs.py` kiểm (câu mới chỉ ghi mã luật và số issue, không ghi mã bài học, ticket hay phiên).

`docs/ops/ads-business-rules.md` được nhắc trong chú thích của các test sau, chỉ để dẫn mục hay mã luật đã có (MC-02, mục S, 4, 5.7, 8, 8.1, 8.2, 8.3, 10, 10.1); plan thêm K-09, sửa câu của F-08, HD-05 và một dòng của mục 8.2, không đổi số mục, mã luật, tên hay giá trị sự kiện và khóa nào, nên không test nào bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Khóa `is_show_native_after_inter`, `time_close_native` đọc như cũ.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi điều kiện hiện của `native_combo` và mọi vị trí native toàn màn hình độc lập (thiếu host thì không hiện), và thêm đường đóng khi host rời (điểm chặn 1).
- Sự kiện phân tích: không thêm tên hay tham số. Ca mới gửi sự kiện có sẵn: native độc lập thiếu host gửi `ad_opportunity` `shown` và `ad_show_fail` mã 3 `app_not_foreground` (điểm chặn 4, câu hỏi 2). Combo thiếu host không gửi gì (như khóa tắt).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `DisposableEffect` có trong `androidx.compose.runtime` (đã là phụ thuộc của `:core:ads`); `combine`, `collectLatest`, `withTimeoutOrNull` có trong `kotlinx-coroutines-core` (đã dùng trong module).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `watchHost` dùng `combine` với `collectLatest` trên `StateFlow` trong `scope` của controller; trên Robolectric (looper chính, `idleFor`) và trong `runTest` (thời gian ảo) thứ tự chạy có thể khác nhau, ví dụ `collectLatest` chưa nhận giá trị đầu trước khi test đổi `hostAttached`.
- **Điều kiện chuyển:** test `open_hostGoneInForeground_closesAfterGrace` hay `hostDetached_whileComboOpen_foreground_closesAfterGrace_nextInterNotBusy` vẫn đỏ vì lượt không đóng (hoặc đóng sớm hơn 2 giây) sau 2 lần sửa.
- **Cách làm thay:** vòng hỏi có giới hạn như `awaitInterGone`: mỗi 200 ms đọc `port.hostAttached.value` và `port.foreground.value`, cộng dồn thời gian "mất host khi ở trước", về 0 khi host có lại hay app ra nền, đóng khi đủ 2 000 ms. Cùng file (`NativeFullScreenTurns.kt`, `NativeAfterInterRules.kt`), cùng test, cùng tiêu chí.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` liệt kê nhiều test ở "Cần xem trước" và "Nên xem" (theo tên file nguồn). Đã đọc mọi test dựng lượt combo hay native độc lập qua `AdsController` thật (`grep` `combo = true`, `beginStandaloneNativeTurn`, `nativeFullScreenView`, `showFullScreenAd` trong `core/ads/src/test`: `AdsControllerNativeComboTest`, `AdsControllerFunnelTest`, `AdsControllerInScreenWindowTest`, `AdsControllerInListRowTest`, `AdsControllerResumeTest`, `AdsControllerStartupInterComboTest`, `AdFreeGateTest`), và mọi test đặt app ở trước (`setAppForegroundForTest(true)`, hay `start` với Activity resumed sau đó). `watchHost` chạy cho mọi lượt mở qua `open`, cả lượt độc lập mở thẳng bằng `beginStandaloneNativeTurn`: lượt không host trong lúc app ở trước tự đóng sau 2 giây. `AdsControllerInScreenWindowTest` (`openAndForeground()`, `standaloneNativeTurn_closesWindow_closeOpensIt`, các test đặt `setAppForegroundForTest(true)`) và `AdsControllerInListRowTest.inListRow_waitsWhileCovered_thenSends` mở lượt độc lập không host bằng `cover()` khi app ở trước và chờ tới 60 giây: không dựng host thì lượt đóng ở 2 giây và các câu "bị che thì không gửi" đỏ vì hành vi đổi, không vì lỗi. `AdFreeGateTest.adFree_fullScreenTurn_disabled_noOpportunity` dừng ở cổng (`DISABLED`, `NOT_READY`) trước kiểm host, không đổi.

**Cách dựng host duy nhất cho test cũ (ADR-0029, quyết định 2):** đăng ký host ngay khi dựng controller của lớp, giữ hàm gỡ trong một trường, để mọi test của lớp chạy có host như trên máy có vỏ app; các test "không có combo" vẫn chạy có host nên vẫn phân biệt được (combo không mở vì luật K-01, A-12, không vì thiếu host). Từng file:

- `AdsControllerNativeComboTest`, `AdsControllerInScreenWindowTest`, `AdsControllerInListRowTest`, `AdsControllerStartupInterComboTest`: controller là trường `private val controller = testAdsController(...)`; thêm ngay sau nó `private val detachHost = controller.attachFullScreenHost()`. Test mới "không host" của `AdsControllerNativeComboTest` dựng controller riêng không đăng ký host (hàm `noHostController()`, mục 4), không gọi `detachHost()`, để không phụ thuộc hàm gỡ mà chính test kiểm.
- `AdsControllerFunnelTest`, `AdsControllerResumeTest`: controller dựng bằng hàm `controller(...)`; hàm thêm `.also { detachHost = it.attachFullScreenHost() }` với trường `private var detachHost: () -> Unit = {}`. `AdsControllerResumeTest.adFreeMidSession_resumeOff_noOpportunity` dựng controller riêng, không mở combo hay native, không cần host.
- `NativeFullScreenTurnsTest`, `NativeFullScreenOverlayTest`: bộ giả `FakeNativeFullScreenPort` mặc định `hostFlow = true`, `foregroundFlow = true`, nên mọi test có sẵn chạy như trước.

Phần dựng này vào commit test của bước 3, cùng các test mới.

Bốn test "Cần xem trước" của `scripts/existing-tests.py`: `NativeFullScreenOverlayTest.smallScreen_countdownAndCtaInside` dùng `FakeNativeFullScreenPort` (mặc định có host, ở trước) và tự vẽ lớp phủ, nên không đổi, kể cả các test dựng lại Activity của lớp đó; `BannerAdSizeTest`, `InListAdsTest.positions_followEvery`, `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction` không mở lượt native toàn màn hình, không đổi. Sau khi sửa theo rà soát, script thêm bảy test khớp chữ `start` (`AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`, `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`, `RewardedActionTest.longContent_small_buttonsInsideWindow`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `watchButton_is56dpHigh_fullCardWidth`): không test nào mở lượt native toàn màn hình, không đổi. "Nên xem": `AdsControllerHostTest.rotation_*`, `handover_*` mở interstitial với `combo = false`, không đổi; `FullScreenAdsTest.withoutController_actionDoesNothing` dựng `FullScreenAdHost()` không có controller, host trả về trước `DisposableEffect`, không đổi; `NativeFullScreenTurnsTest.afterInterstitial_doesNotReadInterShown` đọc tên hàm của port bằng phản chiếu, tên mới (`getHostAttached`, `getForeground`) không chứa "shown", giữ; `close_order_markFirst_finishLast`, `holdsResumeAds_fromStartUntilClose` giữ (đường đóng không đổi); `AdsControllerFunnelTest.comboOff_recordsInterEligibleOnly` giữ (khóa tắt không chờ host).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Combo mở sau interstitial dù không có host | Combo chỉ mở khi có host; thiếu host thì bỏ như khóa tắt | `AdsControllerNativeComboTest`: `closed_opensCombo`, `showFailed_opensCombo`, `adLost_opensCombo`, `stuckRecovered_opensCombo`, `hostDestroyed_opensCombo`, `capExpired_reachesCombo_busyFinishesAtOnce`, `onFinished_runsOnMainThread`; `AdsControllerFunnelTest.combo_recordsNativeEligibleAfterInter`; `AdsControllerInScreenWindowTest.comboTurn_noOpenWindowAcrossInterClose`; `AdsControllerResumeTest` (hàm `assertInAppInterOpensCombo` của các test resume inter); `AdsControllerStartupInterComboTest` (hàm `assertInAppInterOpensCombo`) | Giữ mọi câu khẳng định; dựng host như trên. Có host thì combo mở ngay sau khi chờ interstitial, nên mốc 2 100 ms của các test giữ đúng. Các test "không có combo" cùng lớp (`AdsControllerResumeTest.resumeInter_closed_comboOn_noNativeFullScreen`, `resumeInter_showFailed_comboOn_noNativeFullScreen`; `AdsControllerStartupInterComboTest.startupInter_closed_comboOn_noNativeFullScreen`, `startupInter_showFailed_comboOn_noNativeFullScreen` (hàm `assertNoCombo`); `AdsControllerNativeComboTest.startupTurn_neverOpensCombo`, `showStartupAd_blocked_neverOpensCombo`, `blockedAtGate_*`) chạy có host, giữ nguyên |
| Native độc lập qua `showFullScreenAd` mở dù không có host | Thiếu host thì bỏ như F-08 | `AdsControllerNativeComboTest.standalone_withAd_throughShow_opensTurn`, `standalone_turnOpenPastCap_throughShow_skipsBusy_keepsAdInSlot` | Giữ câu khẳng định; dựng host như trên. Lượt mở quá trần F-02 vẫn ra `BUSY` vì `nativeTurns.busy` được kiểm trước host. `standalone_noAd_throughShow_skipsNotReady_finishesOnce` giữ: cổng trả `NOT_READY` trước khi kiểm host |
| Lượt native độc lập không host, app ở trước, mở mãi tới khi bấm đóng | Tự đóng sau 2 giây (`watchHost`) | `AdsControllerInScreenWindowTest`: `standaloneNativeTurn_closesWindow_closeOpensIt`, `realController_smallSlot_refreshTickWhileCovered_isSkipped`, `realController_smallSlot_nextLoadAfterClaimWhileCovered_waits`, `realController_retryingSlot_firstLoadWaitsWhileCovered`, `realController_poolRefillWhileCovered_waits`, `inScreenSendGate_waitsWhileCovered_thenSends`, `poolCanLoadNow_followsWindow`, `poolExhausted_noPool_true_followsWindow`, `flowStepFullPage_keepsWindowOpen`, `background_closesWindow_withoutCover` và mọi test khác của lớp gọi `cover()`; `AdsControllerInListRowTest.inListRow_waitsWhileCovered_thenSends` | Giữ mọi câu khẳng định: lớp dựng host (trên), nên lượt của `cover()` chỉ đóng bằng `uncover()` như trước. Không viết lại câu nào |
| Nhánh khóa tắt ghi log `Native combo skipped: off` trong khối riêng | Gộp vào `skip(current, "off")`, log giữ nguyên chữ | `NativeFullScreenTurnsTest.comboOff_finishesAtOnce_noView_noLoad` (thứ tự `releaseResumeAds`, `endPresence` trước `onFinished`) | Giữ: `skip` giữ đúng thứ tự đó |

## 4. Kiểm thử

**Tái hiện lỗi trên code hiện tại** (2026-11-11, giờ Việt Nam, trên commit `f108aa3` của nhánh `claude/from-app-444-445`; phần code của `:core:ads` giống hệt nhánh chính `119f91b`, nơi PR #455 được merge và chỉ đổi tài liệu). Hai test tạm không commit trong `core/ads/src/test/.../`, chạy bằng `./gradlew :core:ads:testDebugUnitTest --tests '*T102*' -i`, đã xóa sau khi đo. Khung giống `AdsControllerNativeComboTest`: `AdsController` thật, khóa combo bật, không host, không tracker.

| Ca | Số đo | Log thật |
| --- | --- | --- |
| Combo, khe `native_combo` có quảng cáo sẵn, cổng đồng ý mở | t = 2,1 s: lượt mở, có quảng cáo, hàm hoàn tất 0 lần, cờ đang hiện `true`. t = 62,1 s và t = 362,1 s: y nguyên (nút đóng đã dùng được nhưng không ai bấm được), hàm hoàn tất 0 lần. Interstitial mới lúc 362 s: `beginInterstitialTurn` trả `null` (bận, F-01). Sau trần 10 phút: lượt vẫn mở, hàm hoàn tất vẫn 0 lần | `Full-screen turn finished: closed`, `Resume ads held: full_screen_page`, `Reload gallery_interstitial in 2000 ms`, `Native combo opened (inter gone: false)`, rồi không còn dòng nào: không có `Resume ads released` |
| Combo, quảng cáo về giữa lúc chờ (bộ giả `FakeNativeFullScreenPort`, `deliver` lúc 1,5 s) | t = 61,5 s: lượt mở, có quảng cáo, `busy` `true`, hàm hoàn tất 0 lần, cờ đang hiện `true`, chặn resume `true` | `Native combo opened (inter gone: true)`, `Native combo ad arrived`, rồi không còn dòng nào |
| Native độc lập qua `showFullScreenAd`, có quảng cáo, không host | Kết quả `true`; t = 60 s: lượt mở, hàm hoàn tất 0 lần, cờ đang hiện `true` | `Resume ads held: full_screen_page`, `Native full-screen standalone opened`, rồi không còn dòng nào |
| Đối chứng: combo không có quảng cáo (cổng đồng ý đóng trên JVM) | t = 2,1 s: lượt mở, nút đóng dùng được; t = 7,1 s: tự đóng, hàm hoàn tất 1 lần, cờ `false` | `... Native combo opened (inter gone: false)`, `Resume ads released: full_screen_page`, `Native combo closed (shown: false)` |

Ca đối chứng qua ngay trên code cũ (K-04 tự đóng khi hết đếm ngược), nên không dùng làm test đỏ. Ghi chú cho phiên: trong test của controller không có vỏ app, `resumeAdsSuppressed` luôn đọc `true` (nó theo báo cáo của vỏ, không theo bộ đếm), kể cả sau khi lượt nhả chặn; test kiểm chặn resume bằng `controller.foregroundAdSuppressor.suppressed.value` hay dòng log `Resume ads released: full_screen_page`.

**Khung rỗng của bước 3** (auto-run.md, bước 3: test biên dịch được và đỏ ở phần kiểm). Commit test kèm khung rỗng sau, không có hành vi mới:

- `NativeFullScreenPort` có hai thành phần `hostAttached`, `foreground`; port của `AdsController` trả `hostAttached = MutableStateFlow(true)` (giá trị cố định "luôn có host") và `foreground = foregroundState`. `NativeFullScreenTurns` chưa đọc hai thành phần này (không chờ host, không `watchHost`).
- `AdsController.fullScreenHostAttached` là `MutableStateFlow(true).asStateFlow()` cố định; `attachFullScreenHost()` trả `{}` và không đổi gì.
- `NativeAfterInterRules.kt` có `HOST_WAIT_MILLIS`, `HOST_GONE_GRACE_MILLIS` (test đọc chung), chưa có `awaitHost`.
- `FullScreenAdHost()` chưa đổi; `showNativeFullScreen` chưa kiểm host.
- `FakeNativeFullScreenPort` thêm `val hostFlow = MutableStateFlow(true)`, `val foregroundFlow = MutableStateFlow(true)`, `override val hostAttached get() = hostFlow`, `override val foreground get() = foregroundFlow`.

Chọn "luôn có host" để mọi test có sẵn qua nguyên trên khung, như code cũ. Mỗi test dưới ghi lý do đỏ trên khung đó (trùng với code cũ, vì khung không đổi hành vi). Test qua ngay trên khung nằm ở nhóm canh giữ của ngoại lệ viết test trước, mỗi test có đối chứng đỏ.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-16 tiêu chí 6 (thiếu host như khóa tắt), quảng cáo có sẵn | Unit test (bộ giả, thời gian ảo) | `NativeFullScreenTurnsTest.combo_noHost_slotHasAd_skipsAfterHostWait_releases`: `slot` có quảng cáo, `hostFlow = false`, `interGoneAt = 0`. Lúc 999 ms: `finishes == 0`, `view` null. Lúc 1 000 ms: `finishes == 1`, `view` null, `takeCount == 0`, `ensureCount == 0`, `marks`, `opportunities`, `reloads` rỗng, `presence.showing` `false`, `suppressor.suppressed` `false`, `releaseResumeAds` và `endPresence` đứng trước `onFinished` trong `calls`, log `Native combo skipped: no host`. Lúc 60 000 ms: `finishes` vẫn 1. Đỏ trên khung: lúc 999 ms `view` đã có quảng cáo (lượt mở lúc 0, không chờ host) |
| KB-16 tiêu chí 6, quảng cáo về giữa lúc chờ | Unit test | `NativeFullScreenTurnsTest.combo_noHost_adWouldArriveLater_noLoad_finishes`: `slot` rỗng, `hostFlow = false`; `deliver(NATIVE_AFTER_INTER, ad)` lúc 1 500 ms. Lúc 61 500 ms: `finishes == 1`, `view` null, `busy` `false`, `ensureCount == 0`, `presence.showing` `false`. Đỏ trên khung: `finishes == 0`, lượt mở có quảng cáo (số đo ở trên) |
| Ra nền giữa lúc chờ host (bước chờ có giới hạn, docs/CLAUDE.md) | Unit test | `NativeFullScreenTurnsTest.combo_backgroundDuringHostWait_skipsAtWaitEnd_noLateShow`: `slot` có quảng cáo, `hostFlow = false`, `foregroundFlow = false` lúc 0 (người dùng ra nền khi interstitial vừa đóng). Mong đợi: chờ vẫn giới hạn 1 giây kể cả ở nền; lúc 1 000 ms `finishes == 1`, `view` null, `takeCount == 0`. Lúc 30 000 ms người dùng quay lại: `hostFlow = true`, `foregroundFlow = true`; lúc 60 000 ms `view` vẫn null, `finishes` vẫn 1 (native không hiện muộn khi quay lại). Đỏ trên khung: lượt mở lúc 0 có quảng cáo, `finishes == 0` |
| Dựng lại Activity giữa lúc inter rời màn: host vào lại trong 1 giây | Unit test | `NativeFullScreenTurnsTest.combo_hostAttachesWithinWait_opensThen_deadlineFromThen`: `slot` có quảng cáo, `hostFlow = false`, đặt `true` lúc 300 ms, đếm ngược 3 giây. Lúc 299 ms: `view` null; lúc 300 ms: `view` có quảng cáo. Lúc 3 299 ms: `closeEnabled` `false`; lúc 3 300 ms: `true`. Đỏ trên khung: lúc 299 ms `view` đã có (mở lúc 0) |
| Host tới sau thời hạn chờ | Unit test | `NativeFullScreenTurnsTest.combo_hostAttachesAfterWait_skips`: `hostFlow` đặt `true` lúc 1 200 ms; lúc 1 000 ms `finishes == 1`, lúc 1 200 ms và 10 000 ms `view` null, `takeCount == 0`. Đỏ trên khung: lượt mở lúc 0, `finishes == 0` |
| Host rời khi lượt đang mở, app ở trước: tự đóng sau 2 giây (combo) | Unit test | `NativeFullScreenTurnsTest.open_hostGoneInForeground_closesAfterGrace`: `slot` có quảng cáo, mở lúc 0; `hostFlow = false` lúc 1 000 ms. Lúc 2 999 ms: `view` còn, `finishes == 0`. Lúc 3 000 ms: `view` null, `finishes == 1`, `marks == [(GALLERY_INTERSTITIAL, 3000)]`, `opportunities == [("shown","ready")]`, `reloads == [NATIVE_AFTER_INTER]`, quảng cáo `destroyCount == 1`, `presence.showing` `false`, `suppressor.suppressed` `false`, log `Native full-screen closed: host gone`. Đỏ trên khung: lúc 3 000 ms lượt còn, `finishes == 0` |
| Host rời khi đang đếm ngược (nút đóng chưa dùng được) | Unit test | `NativeFullScreenTurnsTest.open_hostGoneDuringCountdown_closesAnyway`: đếm ngược 30 giây, `hostFlow = false` lúc 500 ms; lúc 2 500 ms `finishes == 1`. Đối chứng trong cùng test: lúc 1 000 ms `close()` không đóng (nút chưa dùng được). Đỏ trên khung: lúc 2 500 ms `finishes == 0` |
| Ra nền rồi trở lại khi lượt đang mở, Activity tạo lại khi về: không đóng | Unit test | `NativeFullScreenTurnsTest.open_hostGoneInBackground_notCounted_hostBackOnReturn_keepsTurn`: `foregroundFlow = false` lúc 500 ms, `hostFlow = false` lúc 1 000 ms; lúc 60 000 ms `view` còn, `finishes == 0`; `hostFlow = true` lúc 60 000 ms, `foregroundFlow = true` lúc 60 010 ms; lúc 70 000 ms `view` còn. Canh giữ (qua trên khung); đối chứng đỏ là test kế tiếp |
| Ra nền rồi về trước mà không có host: đóng sau 2 giây tính từ lúc về | Unit test | `NativeFullScreenTurnsTest.open_hostGoneInBackground_foregroundWithoutHost_closes`: như trên tới 60 000 ms, rồi `foregroundFlow = true` lúc 60 000 ms khi host vẫn `false`; lúc 61 999 ms còn, lúc 62 000 ms đóng, `finishes == 1`. Đỏ trên khung: lúc 62 000 ms lượt còn |
| Host rời rồi vào lại trong 2 giây (dựng lại Activity): giữ lượt | Unit test | `NativeFullScreenTurnsTest.open_hostGoneAndBackWithinGrace_keepsTurn`: `hostFlow = false` lúc 1 000 ms, `true` lúc 1 050 ms; lúc 20 000 ms `view` còn, `finishes == 0`; `close()` đóng, `finishes == 1`. Canh giữ; đối chứng đỏ: `open_hostGoneInForeground_closesAfterGrace` |
| Native độc lập: host rời khi đang mở | Unit test | `NativeFullScreenTurnsTest.standalone_hostGone_closesAfterGrace_marksOwnPlacement`: `showStandalone(GALLERY_NATIVE_FULL_SCREEN, ad)`, `hostFlow = false` lúc 1 000 ms; lúc 3 000 ms `finishes == 1`, `marks == [(GALLERY_NATIVE_FULL_SCREEN, 3000)]`, `opportunities` rỗng, `reloads == [GALLERY_NATIVE_FULL_SCREEN]`. Đỏ trên khung: lúc 3 000 ms `finishes == 0` |
| Khóa tắt giữ như cũ (đường `skip` gộp) | Unit test có sẵn | `NativeFullScreenTurnsTest.comboOff_finishesAtOnce_noView_noLoad` qua không đổi (đã có `assertEquals(0L, currentTime)`: khóa tắt không chờ host) |
| Combo thiếu host qua `AdsController` thật, quảng cáo có sẵn; interstitial sau không bận (F-01) | Unit test (Robolectric) | `AdsControllerNativeComboTest.noHost_adInSlot_comboSkipped_finishes_nextInterNotBusy`: dựng controller riêng không đăng ký host (hàm `noHostController()` cùng tham số với trường `controller`, thêm `RecordingAdMetrics`); `openConsentGateForTest()`, `putPreloadedNativeFullScreenForTest(NATIVE_AFTER_INTER, ad)`, mở lượt interstitial có combo, `onInterstitialClosed`. `idleFor(2_999)`: `finishes == 0`. `idleFor(2)` (tổng 3 001 ms: 2 giây chờ inter rời màn vì không có tracker, rồi 1 giây chờ host): `finishes == 1`, `nativeFullScreenView` null, `fullScreenAdShowing` `false`, `foregroundAdSuppressor.suppressed` `false`, `isFullScreenAdReady(NATIVE_AFTER_INTER)` `true`, `ad.destroyCount == 0`, không có `AdMetricEvent.Opportunity` nào của `native_combo`, log `Native combo skipped: no host`. Rồi `beginInterstitialTurn(GALLERY_INTERSTITIAL, hostId, combo = false) {}` khác `null`. Đỏ trên khung: lúc 3 001 ms `finishes == 0`, lượt mở có quảng cáo (số đo ở trên) |
| Host gỡ hết thì về không có host (hàm gỡ chạy một lần) | Unit test (Robolectric) | `AdsControllerNativeComboTest.attachTwice_detachOne_stillAttached_detachTwiceIgnored`: trên controller riêng không host: `fullScreenHostAttached` `false` (đối chứng của phần dựng); hai lần `attachFullScreenHost()`: `true`; gỡ cái đầu hai lần: vẫn `true`; gỡ cái thứ hai: `false`. Đỏ trên khung: câu đầu (`false`) đọc `true` cố định |
| KB-5 tiêu chí 4, KB-26 tiêu chí 3: native độc lập thiếu host | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_noHost_throughShow_skipped_keepsAdInSlot_showFail`: controller riêng không host, cổng đồng ý mở, vị trí bật, `putPreloadedNativeFullScreenForTest(GALLERY_NATIVE_FULL_SCREEN, ad)`; `showFullScreenAd`: kết quả `false`, `finishes == 1`, `nativeFullScreenView` null, `fullScreenAdShowing` `false`, `isFullScreenAdReady(GALLERY_NATIVE_FULL_SCREEN)` `true`, `ad.destroyCount == 0`, metrics có đúng một `Opportunity` `shown` và một `ShowFail` mã 3 của `gallery_native_full_screen`, log `Full-screen gallery_native_full_screen skipped: no full-screen host`; `idleFor(60_000)`: `finishes` vẫn 1. Đỏ trên khung: kết quả `true`, `finishes == 0` |
| Native độc lập thiếu host mà một lượt còn mở quá trần F-02: vẫn `BUSY` | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_noHost_turnOpenPastCap_skipsBusy_notHostCheck`: controller riêng; lượt độc lập thứ nhất mở bằng `beginStandaloneNativeTurn`, `idleFor(SHOWING_CAP_MILLIS + 1)`; lượt thứ hai qua `showFullScreenAd`: log `skipped: BUSY`, không có `ShowFail` mã 3, quảng cáo thứ hai còn trong khe. Canh giữ cho thứ tự kiểm (qua trên khung vì khung chưa kiểm host); đối chứng đỏ là test trên |
| Host rời qua controller, app ở trước: tự đóng, interstitial sau không bận | Unit test (Robolectric) | `AdsControllerNativeComboTest.hostDetached_whileComboOpen_foreground_closesAfterGrace_nextInterNotBusy`: controller riêng không host, `setAppForegroundForTest(true)`, `val detach = attachFullScreenHost()`, quảng cáo có sẵn, combo mở (`idleFor(2_100)`), `detach()`; `idleFor(1_999)`: còn mở; `idleFor(1)`: `finishes == 1`, `fullScreenAdShowing` `false`, `foregroundAdSuppressor.suppressed` `false`; `beginInterstitialTurn(...)` khác `null`. Đỏ trên khung: lượt còn mở sau 2 000 ms |
| Host thật vào cây và rời cây; đổi vỏ app (bỏ host) | Compose UI test (Robolectric) | `FullScreenAdHostAttachTest.hostInComposition_attached_removed_detached`: `createAndroidComposeRule<ComponentActivity>()`, `setContent { CompositionLocalProvider(LocalAdsController provides controller) { AdsTestTheme { if (showHost) FullScreenAdHost() } } }` với `showHost` là `mutableStateOf(true)`; sau `waitForIdle`: `controller.fullScreenHostAttached.value` `true`; `showHost = false`: `false`. Đối chứng: không có `LocalAdsController` thì `FullScreenAdHost()` không đăng ký và không lỗi. Đỏ trên khung: sau `showHost = false` vẫn đọc `true` |
| Dựng lại Activity khi combo đang mở: lượt giữ, hàm hoàn tất một lần (KB-26 tiêu chí 4) | Compose UI test (Robolectric) | `FullScreenAdHostAttachTest.recreate_whileComboOpen_turnKept_finishesOnceOnClose`: `controller.setAppForegroundForTest(true)` trước khi mở lượt (không dùng `start`: rule đã mở Activity trước thân test, nên tracker không thấy Activity và `foregroundState` luôn `false`). Nội dung như mẫu của app (`AdsHost(controller) { ... }` rồi `FullScreenAdHost()`). Khóa combo bật, cổng đồng ý mở, quảng cáo có sẵn; mở combo bằng `beginInterstitialTurn` rồi `onInterstitialClosed`; `idleFor(2_100)` (looper chính của Robolectric): `nativeFullScreenView` có quảng cáo. `activityRule.scenario.recreate()` rồi `onActivity { it.setContent { Shell() } }`; `idleFor(3_000)`: `nativeFullScreenView` còn, `finishes == 0`, `fullScreenHostAttached` `true`; hết đếm ngược thì `closeNativeFullScreen()`: `finishes == 1`. Canh giữ (qua trên khung); đối chứng đỏ: test JVM `open_hostGoneInForeground_closesAfterGrace` và test kế tiếp |
| Ra nền, Activity bị hủy và dựng lại lúc ở nền (host thật rời cây), rồi trở lại: lượt giữ | Compose UI test (Robolectric) | `FullScreenAdHostAttachTest.background_hostLeavesWhileStopped_returns_turnKept`: như trên tới lúc combo mở. `setAppForegroundForTest(false)`, `scenario.moveToState(Lifecycle.State.CREATED)`, `scenario.recreate()` (Activity mới ở CREATED, chưa `setContent`, nên host thật đã rời cây). Câu trung gian bắt buộc: `fullScreenHostAttached` `false` (chắc ca có thật, không qua vô điều kiện). `idleFor(5_000)`: lượt còn, `finishes == 0`. Về trước: `moveToState(RESUMED)`, `onActivity { it.setContent { Shell() } }`, `setAppForegroundForTest(true)`; `idleFor(3_000)`: lượt còn, `fullScreenHostAttached` `true`. Canh giữ cho phần lượt; câu trung gian đỏ trên khung (đọc `true` cố định) |
| Như trên nhưng về trước mà vỏ không có host: đóng sau 2 giây | Compose UI test (Robolectric) | `FullScreenAdHostAttachTest.background_returnWithoutHost_closesAfterGrace`: như trên, nhưng khi về chỉ `moveToState(RESUMED)` và `setAppForegroundForTest(true)`, không `setContent`; `idleFor(1_999)`: lượt còn; `idleFor(1)`: `nativeFullScreenView` null, `finishes == 1`, `fullScreenAdShowing` `false`. Đỏ trên khung: câu trung gian, rồi lượt không đóng |
| Tài liệu: K-01, K-02, K-09, F-08, HD-05, mục 8.2, mục 11, `ads-guide.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc các đoạn đã sửa khớp mục 3.1 và câu trả lời mục 8; `scripts/history-refs.py` qua |
| Combo trên máy thật vẫn hiện khi có host (`MainActivity`, màn xem thử) | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging, `is_show_native_after_inter` bật trên Firebase staging (`StagingRemoteValuesModule` cho Firebase ghi đè): màn xem thử, bấm interstitial của gallery, đóng; native toàn màn hình hiện với đếm ngược; xoay máy khi đang đếm, native còn; đóng thì đi tiếp. Log: `Full-screen host attached` lúc mở app (dòng ghi trong `attachFullScreenHost`, chạy khi `FullScreenAdHost()` vào cây của `MainActivity` và `AdGalleryActivity`), `Native combo opened`, `Native combo closed` |

**Ngoại lệ viết test trước:** sửa `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md` (chỉ tài liệu). Phần nối hai thành phần port trong `AdsController` (`hostAttached = fullScreenHostAttached`, `foreground = foregroundState`) được các test của controller ở trên kiểm qua hành vi. Nhóm canh giữ (qua ngay trên khung rỗng, giữ hành vi đúng sau khi sửa), mỗi test kèm đối chứng đỏ: `AdsControllerNativeComboTest.withHost_adInSlot_opensCombo_asBefore` (controller của lớp có host; quảng cáo có sẵn; `idleFor(2_100)`: `nativeFullScreenView.value!!.ad === ad`, `finishes == 0`; `idleFor(5_000)`, `closeNativeFullScreen()`: `finishes == 1`; đối chứng: `noHost_adInSlot_comboSkipped_...`), `standalone_noHost_turnOpenPastCap_skipsBusy_notHostCheck` (đối chứng: `standalone_noHost_throughShow_...`), `NativeFullScreenTurnsTest.open_hostGoneAndBackWithinGrace_keepsTurn`, `open_hostGoneInBackground_notCounted_hostBackOnReturn_keepsTurn` (đối chứng: hai test "đóng" tương ứng), `FullScreenAdHostAttachTest.recreate_whileComboOpen_...` và phần lượt của `background_hostLeavesWhileStopped_returns_turnKept` (đối chứng: `background_returnWithoutHost_closesAfterGrace`).

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi điều kiện hiện của combo và native toàn màn hình độc lập, thêm đường đóng lượt), 4 (dữ liệu: ca native độc lập thiếu host gửi `ad_opportunity` `shown` và `ad_show_fail` mã 3, tên và giá trị có sẵn), 7 (module core `:core:ads`). Không chạm 2, 3, 5 (không đổi hộp thoại đồng ý hay thứ tự khởi tạo), 6, 8, 9. Người phụ trách sản phẩm cần biết trước khi duyệt và trả lời câu hỏi 1 tới 3.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Bốn file nguồn của `:core:ads`, có đổi giao diện nội bộ (`NativeFullScreenPort`) và hành vi của composable công khai `FullScreenAdHost`; tám file test (bảy có sẵn, một mới); hai tài liệu. Một module |
| B. Độ rõ của yêu cầu | 2 | Spec chưa có tiêu chí cho ca thiếu host; luật đổi (câu hỏi 1 tới 3) ảnh hưởng tới cách làm của native độc lập và thời hạn |
| C. Khả năng kiểm chứng tự động | 2 | Mọi điểm code có test JVM với thời gian điều khiển được và `AdsController` thật; nhưng phần dựng lại Activity và ra nền phụ thuộc cách Robolectric dựng lại Activity ở CREATED và gỡ cây giao diện (vòng đời, tiêu chí C mức 3 một phần), và ca app nhiều Activity không có test (câu hỏi 7); combo thật trên máy chỉ kiểm thủ công |
| D. Ảnh hưởng chéo | 2 | T-101 (PR #457) cũng sửa `AdsController.kt`, `ads-business-rules.md` (cả bảng mục 11) và `ads-guide.md`; bảy file test có sẵn đổi phần dựng |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: `DisposableEffect` đăng ký với controller, chờ có giới hạn như `awaitInterGone`, `StateFlow` của controller vào port |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (lượt toàn màn hình, port của native), `NativeFullScreenTurns.kt`, `NativeFullScreenAdHost.kt` (`FullScreenAdHost` mà `MainActivity` và `AdGalleryActivity` của staging đặt; không đổi chữ ký nên `:app` không sửa), `docs/ops/ads-business-rules.md` (K, F-08, HD-05, mục 8.2), `docs/ops/ads-guide.md` (mục 5, mục 9).
- Task khác có thể xung đột:
  - T-101 (pool `native_step` cho bước kế tiếp; plan ở PR #457, nhánh `claude/plan-T-101-pool-buoc-ke-tiep`): sửa `AdsController.kt` (thêm `poolClaimableAdCount` ngay sau `poolHasFreeAd`), `docs/ops/ads-business-rules.md` (OB-05 mới và một dòng `NEXT_STEP_AD_READ_MILLIS` ở bảng hằng số mục 11), `docs/ops/ads-guide.md` (dòng 48, 77). Chỗ chung: `AdsController.kt` khác vùng (T-102: port của `nativeTurns`, `showNativeFullScreen`, hàm host mới), ghép tự động được; `ads-guide.md` khác dòng (T-102: mục 5 dòng 101, mục 9); `ads-business-rules.md` mục OB khác mục K, F-08, HD-05, 8.2, nhưng **bảng mục 11 có thể xung đột**: cả hai plan sửa hay thêm dòng ở bảng hằng số (T-102 sửa dòng "Đếm ngược native sau inter; chờ inter rời màn", T-101 thêm một dòng). Task chạy sau giữ cả hai dòng khi rebase. Không có file mà hai plan cùng tạo.
  - Không thấy plan `approved` nào khác đang chờ chạy sửa `NativeFullScreenTurns.kt` hay `NativeFullScreenAdHost.kt`.
- Đề xuất thứ tự chạy: độc lập với T-101; chạy trước hay sau đều được. Task sau rebase lên nhánh chính và giải xung đột chữ ở bảng mục 11 của `ads-business-rules.md` nếu có (giữ cả hai dòng).

## 7. Điều kiện dừng riêng

- Phải đổi chữ ký công khai của `FullScreenAdHost()` (thêm tham số) hay sửa `MainActivity`, `AdGalleryActivity` để combo hiện: dừng, vì plan giữ vỏ app không đổi.
- Một ảnh chuẩn screenshot của lớp phủ native đổi: dừng (plan không cho ghi lại ảnh).
- Test có sẵn của dựng lại Activity hay ra nền với native toàn màn hình (`NativeFullScreenOverlayTest`, `NativeFullScreenStepRecreateTest`, `AdsControllerHostTest.rotation_*`) đỏ vì lượt bị đóng: dừng, vì đó là mất lượt hợp lệ mà plan cấm.
- Một test có sẵn mở lượt native độc lập hay combo khi app ở trước (`AdsControllerInScreenWindowTest`, `AdsControllerInListRowTest` qua `cover()`, hay lớp khác có `setAppForegroundForTest(true)`) vẫn đỏ vì lượt tự đóng sau khi lớp đã dựng host theo mục 3.5: dừng, vì `watchHost` đóng cả lượt có host (lỗi của code, không phải dữ liệu test). Lớp test có lượt như vậy mà mục 3.5 chưa nêu: dựng host theo cùng cách (ADR-0029) và ghi vào nhật ký, không dừng.

## 8. Câu hỏi cho người duyệt

1. **(Người phụ trách sản phẩm) Combo khi vỏ app thiếu chỗ vẽ.** KB-16 tiêu chí 1 nói khóa bật thì có native; thiếu chỗ vẽ thì không thể có, và hiện nay luồng treo. Đề xuất thêm vào KB-16: "**Thêm 2026-11-11:** Khi app không có chỗ vẽ native toàn màn hình (thiếu bộ chứa lớp phủ) thì interstitial đóng xong người dùng đi tiếp ngay, không có native, như khi khóa tắt; khoảng cách `ads_interval` tính từ lúc interstitial đóng." và thêm vào tiêu chí 1 "(khi app có chỗ vẽ native, tiêu chí 9)". Đề xuất: đồng ý. Câu duyệt sẵn: "Đồng ý tiêu chí combo thiếu chỗ vẽ như câu hỏi 1 của plan T-102."
2. **(Người phụ trách sản phẩm) Native toàn màn hình độc lập khi thiếu chỗ vẽ, và sự kiện.** Đề xuất thêm vào KB-5: "Khi app không có chỗ vẽ native toàn màn hình thì không hiện gì, không chặn người dùng, quảng cáo giữ trong khe cho lần sau", và coi đây là ca của KB-26 tiêu chí 3: ghi cơ hội `shown` rồi `ad_show_fail` lý do `app_not_foreground` (mã 3), như mọi lần gọi hiện mà màn hình không dùng được, để mục 8.2 "mỗi lượt gọi hiện đúng một kết quả" vẫn đúng. Phương án khác: chỉ ghi cơ hội `not_ready`, không có `ad_show_fail` (dễ lẫn với "chưa tải xong"). Đề xuất: phương án F-08. Câu duyệt sẵn: "Đồng ý native độc lập thiếu chỗ vẽ theo F-08 như câu hỏi 2 của plan T-102."
3. **(Người phụ trách sản phẩm) Chỗ vẽ biến mất khi native đang mở.** Đề xuất thêm vào KB-16 (và áp cho KB-5): "Khi chỗ vẽ native biến mất lúc native đang mở và không quay lại trong 2 giây khi app đang ở trước (không phải xoay máy hay dựng lại màn), native đóng như bấm đóng: người dùng đi tiếp, khoảng cách tính từ lúc này. Thời gian app ở nền không tính." Đóng cả khi đang đếm ngược, vì không ai thấy màn. Đề xuất: đồng ý, 2 giây. Câu duyệt sẵn: "Đồng ý tự đóng sau 2 giây như câu hỏi 3 của plan T-102."
4. **Thời gian chờ chỗ vẽ sau khi interstitial rời màn: 1 giây.** Đủ cho Activity vừa dựng lại (máy bật "không giữ hoạt động", hay hệ thống thu hồi Activity khi quảng cáo che) vẽ khung đầu; không làm chậm ca thường vì có host thì không chờ. Đề xuất: 1 giây. Câu duyệt sẵn: "Đồng ý chờ 1 giây."
5. **Combo bị bỏ vì thiếu chỗ vẽ không ghi `ad_opportunity`.** Như khi khóa tắt: không phải một lần hiện (K-07), và log `Native combo skipped: no host` đủ để người phân tích thấy. Ghi `not_ready` thì đổi số liệu của `native_combo` và lẫn với "chưa tải xong". Đề xuất: không ghi. Câu duyệt sẵn: "Đồng ý không ghi cơ hội cho combo bị bỏ."
6. **Cỡ ticket.** Ticket ghi S; plan sửa bốn file nguồn, tám file test, hai tài liệu, nên plan đề xuất M (`token_estimate` theo cỡ M) để phiên không gặp điều kiện dừng 6 (việc lớn hơn rõ rệt so với ước lượng). Đề xuất: đổi ticket sang M khi duyệt plan. Câu duyệt sẵn: "Đổi T-102 sang cỡ M."
7. **App nhiều Activity: Activity phía trên không có host còn Activity bên dưới có.** Khung thấy có host nên lượt mở, `watchHost` không đóng; lượt treo (cờ đang hiện tới trần 10 phút, chặn quảng cáo khi quay lại app, hàm hoàn tất chưa chạy) tới khi người dùng quay về Activity có host. Sửa triệt để cần gắn host với Activity và so với Activity đang resumed (thêm tham số Activity cho đăng ký, đổi điều kiện của `watchHost`), làm plan lớn hơn và test của controller phải có tracker. Đề xuất: không làm trong T-102; ghi giới hạn ở HD-05 (mọi Activity gọi quảng cáo toàn màn hình phải có host, thiếu thì combo có thể treo tới khi về Activity có host) và ở `ads-guide.md`; mở ticket riêng khi có app nhiều Activity cần. Câu duyệt sẵn: "Đồng ý để ca nhiều Activity ngoài T-102, ghi ở HD-05."

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
