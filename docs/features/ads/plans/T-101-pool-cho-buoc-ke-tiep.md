---
ticket: T-101
title: Feature hỏi được pool native_step đã có quảng cáo cho bước kế tiếp của luồng chưa
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 21.1M        # cỡ M, số gợi ý 21.1M của scripts/token-usage.py estimate (2026-11-10, trung vị 28 task M); không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-101: FEATURE HỎI ĐƯỢC POOL NATIVE_STEP ĐÃ CÓ QUẢNG CÁO CHO BƯỚC KẾ TIẾP CỦA LUỒNG CHƯA

> **Plan chỉ được duyệt sau khi người phụ trách sản phẩm (anh Nhật) xác nhận spec đã sửa** theo câu hỏi 1 ở mục 8 (tiêu chí mới cho KB-20). Spec `ads` hôm nay chưa có tiêu chí nào về "bước kế tiếp đã có quảng cáo".

## 1. Tóm tắt

Theo issue #444 (camera-pose, PR #312 của app, đã đọc diff) và bài học L-166, `:core:ads` thêm bốn phần chỉ đọc, không nạp và không hủy quảng cáo:

- `NativeAdPool.claimableAdCount()`: số quảng cáo mà `claim` cấp được ngay (cổng hiện `canShowNow()` mở, chưa ghi lượt hiển thị, chưa đủ 55 phút).
- `AdsController.poolClaimableAdCount(placement)` (`internal`): đọc số đó trên pool đã có của vị trí, không tạo pool.
- `nextStepAdReady(claimable, currentStepHasAd)` công khai: luật chừa một quảng cáo cho thẻ hiện tại (thẻ chưa có quảng cáo thì cần ít nhất 2, có rồi thì ít nhất 1).
- `@Composable rememberNativeStepNextAdReady(placement, step, currentStepHasAd)`: đi qua cùng cổng với `NativeStepAd` (`rememberNativeStepAdAllowed`, OB-04), đọc ở khung hình đầu của bước và đọc lại mỗi 200 ms; đã đúng thì giữ đúng cho bước đó, trừ khi cổng đóng.

Kiểm chứng bằng test JVM: pool thật với đồng hồ giả, hàm thuần, hàm Compose với số đếm tiêm vào (đồng hồ khung hình của test), `AdsController` thật với bộ nạp giả, và đột biến đối chứng cho luật chừa quảng cáo, cờ mới nhất, `canShowNow`, hạn dùng, vòng đọc lại và cổng. Luật mới OB-05 vào `ads-business-rules.md`, cách dùng vào `ads-guide.md`.

## 2. Hiểu yêu cầu

Ticket xử lý KB-20 trong `features/ads/spec.md` (spec `confirmed`). Tiêu chí "đề xuất 10" chưa có trong spec: nó là tiêu chí AI đề xuất ở mục 8, câu hỏi 1; khi spec được sửa và xác nhận, mục 2 và mục 4 đổi "đề xuất 10" thành số tiêu chí thật.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-20, tiêu chí 2 | Hai bước kề nhau không hiện cùng một quảng cáo | Không đổi. Hàm mới chỉ đọc; thẻ vẫn lấy quảng cáo mới trước rồi mới trả quảng cáo cũ (OB-02). Luật chừa của hàm mới dựa vào đúng cách lấy này: thẻ của bước kế tiếp sẽ lấy một quảng cáo rảnh khác với quảng cáo thẻ hiện tại đang giữ |
| KB-20, tiêu chí 4 | Khi khóa tắt hay không có quảng cáo thì bước hiện bình thường, không có khoảng trống | Không đổi. Hàm mới trả `false` khi khóa tắt (cổng đóng); app không chờ khi đó (tiêu chí 9) |
| KB-20, tiêu chí 9 | App chờ thẻ trước khi cho đi tiếp thì chỉ chờ lúc thẻ được phép; không được phép hay thôi được phép giữa lúc chờ thì đi tiếp ngay | Hàm mới đi qua đúng cổng của `rememberNativeStepAdAllowed` (cùng `ads.rememberStepAllowed(placement)`): cổng đóng thì trả `false` ngay, kể cả khi đã đúng trước đó. App chờ bước kế tiếp chỉ khi `rememberNativeStepAdAllowed(placement)` là `true` và hàm mới là `false`; cổng đóng thì đi tiếp ngay (mục 3.1 D) |
| KB-20, đề xuất 10 (câu hỏi 1) | Khi app giữ nút đi tiếp cho tới khi bước kế tiếp có quảng cáo, thì quảng cáo vừa về lúc thẻ của bước hiện tại còn chờ là của thẻ hiện tại; bước kế tiếp chỉ tính là đã có khi còn một quảng cáo rảnh ngoài quảng cáo của thẻ hiện tại | `nextStepAdReady`: thẻ hiện tại chưa có quảng cáo thì cần 2, có rồi thì cần 1. Đếm theo đúng điều kiện của `claim` (cổng hiện, lượt hiển thị, hạn dùng), không tính yêu cầu đang chạy |
| KB-12, tiêu chí 1 | Khi người dùng rút đồng ý thì không còn yêu cầu mới và quảng cáo đã tải không hiện | Rút đồng ý: cổng của hàm mới đóng (`false` ngay) và pool trả 0 (`canShowNow()` tắt, pool đóng). Hàm mới không gửi yêu cầu nào |
| KB-7, tiêu chí 1; KB-30, tiêu chí 1, 3 | Công tắc tổng tắt, hay đã mua gói bỏ quảng cáo, thì không có quảng cáo; đổi giữa chừng thì biến mất ngay | Cùng cổng (`isEnabled` gồm công tắc tổng và gói bỏ quảng cáo): hàm mới về `false` ngay |
| KB-24, tiêu chí về nạp | Nạp và thử lại theo nhịp của khung (O-05, S-24) | Không đổi: hàm mới không gọi `refill`, không đi qua cổng gửi. Khác `poolHasFreeAd` (có `refill`) mà lưới dùng |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; `docs/ops/design-workflow.md`).

ADR liên quan: ADR-0045 (mục 4: quảng cáo chỉ qua `:core:ads`, luật nghiệp vụ ở `ads-business-rules.md`, không đặt tên hàm theo tính năng của một app, mọi thay đổi `:core:ads` là rủi ro cao), ADR-0001 (feature chỉ phụ thuộc core), ADR-0019 (log), ADR-0031 (mục 3.5), ADR-0044 (tài liệu quy tắc không ghi mã ticket, bài học hay phiên).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo bản đã chạy ở app (PR #312 của camera-pose, T-092 của app; diff đọc được bằng `gh api repos/VGL-Studio/camera-pose/pulls/312`), chỉnh cho quy tắc của dự án mẫu ở bốn chỗ (bảng đối chiếu ở mục E).

**A. Pool: số quảng cáo cấp được ngay.** `NativeAdPool.kt`, ngay sau `refill()`:

```kotlin
/**
 * The free ads a claim could hand out now: none while consent is withdrawn (S-11, like [claim]), else the free ads
 * with no impression and under 55 minutes old (P-01a, O-04). A request in flight is not an ad yet. Never loads or
 * drops anything (no [pruneExpired], no [refill], no log): the next-step read of a flow calls it every 200 ms.
 */
fun claimableAdCount(): Int {
    if (!canShowNow()) return 0
    val now = clock.nowMillis()
    return free.count { !it.ad.impressionRecorded && now - it.loadedAtMillis < NativeAdPolicy.MAX_AGE_MILLIS }
}
```

Pool đóng (`close()`) thì `free` rỗng nên trả 0. Điều kiện khớp `claim`: `claim` bỏ quảng cáo đã ghi lượt hiển thị hay đủ 55 phút (`pruneExpired`) rồi mới lấy, nên số này là số lần `claim` liên tiếp trả được quảng cáo.

**B. Controller.** `AdsController.kt`, ngay sau `poolHasFreeAd`:

```kotlin
/**
 * The ads the pool of [placement] could hand out now ([NativeAdPool.claimableAdCount]); 0 while it has no pool.
 * Reads the pool a card or the preload made: never makes one, never looks up an ad unit, never refills.
 */
internal fun poolClaimableAdCount(placement: AdPlacement): Int =
    nativePools.existingPool(placement)?.claimableAdCount() ?: 0
```

**C. Luật và hàm Compose.** `NativeFlowStep.kt`, ngay sau `rememberNativeStepAdAllowed`:

```kotlin
/**
 * Whether [claimable] ads of the pool leave one for the next step (OB-05): while the card of the current step has no
 * ad yet ([currentStepHasAd] false) the first one is for it, so two are needed; once it has its own, one. Public so
 * an app can test its wiring with this rule on a fake count.
 */
fun nextStepAdReady(claimable: Int, currentStepHasAd: Boolean): Boolean =
    claimable >= if (currentStepHasAd) 1 else 2

/**
 * Whether the pool of [placement] already holds an ad for the step after [step] (OB-05) ... (KDoc đầy đủ: mục dưới)
 */
@Composable
fun rememberNativeStepNextAdReady(placement: AdPlacement, step: Any, currentStepHasAd: Boolean): Boolean {
    requireStepFormat(placement.format)
    val ads = LocalAdsController.current ?: return false
    val stepAllowed = ads.rememberStepAllowed(placement)
    val ready = rememberNextStepAdReady(stepAllowed, step, currentStepHasAd) { ads.poolClaimableAdCount(placement) }
    LaunchedEffect(step, ready) {
        if (ready) ads.logger.info(TAG, "Step ${placement.key}: next step ad ready")
    }
    return ready
}

/** How often [rememberNativeStepNextAdReady] reads the pool while the next step has no ad yet. */
internal const val NEXT_STEP_AD_READ_MILLIS = 200L

/** [rememberNativeStepNextAdReady] without an ads controller: [allowed] and [claimable] come from the caller. */
@Composable
internal fun rememberNextStepAdReady(
    allowed: Boolean,
    step: Any,
    currentStepHasAd: Boolean,
    claimable: () -> Int,
): Boolean {
    val hasAd by rememberUpdatedState(currentStepHasAd)
    val count by rememberUpdatedState(claimable)
    // Read in the composition itself, so the first frame of the step already carries the answer; nothing is read
    // while the gate is closed. Keyed on the gate too: a gate that closes starts a new false state, and one that
    // opens again reads again instead of keeping the true of before.
    var ready by remember(step, allowed) { mutableStateOf(allowed && nextStepAdReady(claimable(), currentStepHasAd)) }
    LaunchedEffect(step, allowed) {
        if (!allowed) return@LaunchedEffect
        while (!ready) {
            delay(NEXT_STEP_AD_READ_MILLIS)
            // The latest count and the latest flag together.
            ready = nextStepAdReady(count(), hasAd)
        }
    }
    return ready
}
```

Không có `allowed &&` ở giá trị trả về: cổng đóng thì khóa `allowed` của `remember` tạo trạng thái mới với giá trị đầu `false` (không đọc pool) và hiệu ứng trả về ngay, nên trạng thái đó luôn `false` khi cổng đóng; `allowed &&` ở đó sẽ thừa và đột biến bỏ nó không đổi gì.

Thêm import `mutableStateOf`, `setValue` (`LaunchedEffect`, `delay`, `rememberUpdatedState`, `remember` đã có). Log không chứa khóa bước (có thể là dữ liệu của app), chỉ mã vị trí, như các dòng `Step ...` của `stepAdStates`; một dòng mỗi lần giá trị thành `true` (`LaunchedEffect(step, ready)`: lần đầu của bước, hay lại thành `true` sau khi cổng đóng rồi mở), không ghi mỗi 200 ms.

KDoc của `rememberNativeStepNextAdReady` (tiếng Anh) nói đủ:

- Trả `true` khi pool của `placement` đã có quảng cáo cho bước sau `step`, chừa một quảng cáo cho thẻ của `step` khi `currentStepHasAd` là `false` (`nextStepAdReady`).
- Đọc ở khung hình đầu của bước (nút do app giữ không sáng một khung rồi mới mờ) và mỗi 200 ms sau đó; mỗi lần đọc lấy số mới nhất cùng cờ mới nhất. Đã `true` thì giữ `true` cho bước đó: giữa lúc thẻ vừa lấy quảng cáo và lúc thẻ báo về, số đã giảm mà cờ chưa đổi.
- Trả `false` khi không có `AdsController`, và ngay khi thẻ thôi được phép (cùng cổng với `NativeStepAd` và `rememberNativeStepAdAllowed`: đồng ý, công tắc tổng và gói bỏ quảng cáo, `is_show_<placement>`, mã đơn vị, lần mở đầu với vị trí `firstOpenOnly`); cổng mở lại thì đọc lại từ đầu. `false` khi cổng đóng không có nghĩa là phải chờ: app chỉ chờ khi `rememberNativeStepAdAllowed(placement)` là `true`.
- `currentStepHasAd` là việc thẻ của **chính bước này** đã có quảng cáo: thẻ không báo gì khi đang chờ quảng cáo mới, nên cờ `true` của bước trước còn nguyên khi sang bước này; app đặt cờ theo bước (ví dụ nhớ bước mà `onAdAvailabilityChange` báo `true`, so với `step`). Bước hiện tại không có thẻ (`NativeStepAd` không có trong cây ở bước đó) thì không có gì để chừa: truyền `currentStepHasAd = true`, một quảng cáo là đủ. Truyền cùng `step` như cho `NativeStepAd`.
- Chỉ đọc: không nạp, không hủy, không lấy quảng cáo, không ghi số liệu. Vị trí khác định dạng `native_step` ném `IllegalArgumentException`.

KDoc của `NativeStepAd` thêm một câu: app muốn biết bước kế tiếp đã có quảng cáo thì đọc `rememberNativeStepNextAdReady`.

**D. Kết hợp với `rememberNativeStepAdAllowed` (OB-04, KB-20 tiêu chí 9).** Hai hàm cùng đọc `ads.rememberStepAllowed(placement)`, nên luôn cùng một cổng. App giữ nút đi tiếp ở bước `n` (bước kế tiếp có thẻ) theo:

```kotlin
val allowed = rememberNativeStepAdAllowed(placement)
val nextReady = rememberNativeStepNextAdReady(placement, step, currentStepHasAd = cardAdStep == step)
val hold = allowed && !nextReady && /* giới hạn thời gian, lần đầu, ở tiền cảnh: phần của app */
```

Thẻ không được phép (chưa đồng ý, khóa tắt, gói bỏ quảng cáo, lần mở sau với vị trí `firstOpenOnly`) thì `allowed` là `false`: không chờ. Thôi được phép giữa lúc chờ: `allowed` về `false` ở khung hình kế tiếp, hết chờ. Ví dụ này vào `ads-guide.md`, mục 4 (phần chờ, giới hạn thời gian, ra nền là của app, không thuộc ticket).

Mọi đường tới giá trị trả về đều qua cổng (cổng mở rồi đóng lại):

| Đường | Kiểm cổng ở đâu | Test |
| --- | --- | --- |
| Lần đọc ở khung hình đầu của bước (`remember(step, allowed)`) | `allowed && ...`: cổng đóng thì không gọi `claimable()` | Test 14a |
| Các lần đọc lại mỗi 200 ms | `LaunchedEffect(step, allowed)` không chạy khi cổng đóng; cổng đóng giữa chừng thì hiệu ứng bị hủy | Test 14a, 14b |
| Giá trị đã `true` của bước (giữ đúng) | Khóa `allowed` của `remember(step, allowed)`: cổng đóng thì trạng thái mới có giá trị đầu `false` và hiệu ứng không chạy, nên trả `false` ngay; cổng mở lại thì lại một trạng thái mới, đọc lại từ đầu | Test 14b, 18, 20 |
| Cổng gồm cả công tắc tổng, gói bỏ quảng cáo, khóa vị trí (không chỉ đồng ý) | `ads.rememberStepAllowed(placement)`, cùng hàm với `rememberNativeStepAdAllowed` | Test 20 |
| Pool trả 0 khi rút đồng ý (S-11) hay pool đóng | `claimableAdCount()`: `canShowNow()`; `close()` xóa `free` | Test 3, 5 |
| Qua controller thật: rút đồng ý sau khi đã `true` | `ads.rememberStepAllowed` (cổng đồng ý thật) | Test 18 |

**Ra nền rồi quay lại.** Hàm mới không chờ gì và không hiện gì: nó chỉ trả một giá trị. Vòng đọc lại vẫn chạy khi app ở nền (lời gọi `delay` của hiệu ứng không dừng theo vòng đời), mỗi lần đọc là đếm một danh sách trong bộ nhớ, không gửi yêu cầu (ở nền pool cũng không nạp được, P-05). Quay lại thì giá trị theo pool trong lần đọc kế tiếp (tối đa 200 ms). Việc giữ nút khi ra nền là của app (ví dụ ở app nguồn: hết chờ khi ra nền); `ads-guide.md` ghi điều này. Test 15 (ra nền rồi trở lại với activity thật) và test 17 (ở nền, đọc lại 2 giây không gửi yêu cầu nào).

**E. Đối chiếu bản của app với quy tắc của mẫu.**

| Điểm | Bản của app (PR #312) | Bản của mẫu (plan này) | Lý do |
| --- | --- | --- | --- |
| Tìm pool | `nativePools.pool(placement)`: tạo pool nếu chưa có (tra mã đơn vị) | `nativePools.existingPool(placement)`: chỉ đọc pool đã có | Như `poolExhausted`, `poolHasFreeAd` của mẫu (đọc không tạo, không tra mã đơn vị). Pool được tạo bởi thẻ (`stepAd`) hay lượt nạp sẵn; chưa có pool thì chưa có quảng cáo nào, trả 0 là đúng (câu hỏi 5) |
| Cổng | Không đọc cổng; giữ `true` cả khi rút đồng ý | Qua `ads.rememberStepAllowed(placement)`; cổng đóng thì `false` ngay, mở lại thì đọc lại | L-061, OB-04, KB-20 tiêu chí 9: mọi giá trị về quảng cáo thôi đúng ngay khi quảng cáo thôi được phép (câu hỏi 2) |
| Log | Không có | Một dòng mỗi bước khi thành `true` | ADR-0019: đổi trạng thái chính (câu hỏi 6) |
| Hằng số | `private const val NEXT_AD_POLL_MILLIS = 200L` | `internal const val NEXT_STEP_AD_READ_MILLIS = 200L`, ghi vào bảng hằng số (mục 11) của `ads-business-rules.md` | Test đọc chung một hằng số; mọi hằng số cứng có trong bảng (câu hỏi 4) |
| `claimableAdCount` | `canShowNow`, lượt hiển thị, 55 phút, không prune | Giống | Đã đúng P-01a, P-03, S-11 |
| `nextStepAdReady`, giữ `true` cho bước, đọc khung hình đầu, cờ mới nhất | Có | Giống | Đã đúng; đã qua rà soát và chạy ở app |

Quy tắc của mẫu được giữ: P-01a (không tính quảng cáo đã ghi lượt hiển thị, vì `claim` sẽ hủy nó), P-02 (cỡ 3, đếm cả quảng cáo đang dùng: lúc thẻ giữ 1 thì pool có tối đa 2 rảnh, nên luật "1 khi thẻ có rồi" đạt được), P-03 (không tính quảng cáo đủ 55 phút; không đổi nhịp nạp bù vì không gọi `refill`), P-06 (thẻ vẫn lấy theo vòng 500 ms; hàm mới chỉ đọc nên không tranh quảng cáo với thẻ), S-11 (cổng hiện).

**F. Tài liệu** (tài liệu quy tắc: không ghi mã ticket, bài học hay phiên; `scripts/history-refs.py` kiểm):

- `docs/ops/ads-business-rules.md`:
  - Bảng 5.11, dòng mới OB-05 ngay sau OB-04 (bốn cột): "**Bước kế tiếp đã có quảng cáo.** App biết pool của vị trí `native_step` đã có quảng cáo cho bước kế tiếp chưa qua một hàm chỉ đọc của `:core:ads` (không nạp, không hủy, không lấy quảng cáo). Chỉ tính quảng cáo mà thẻ lấy được ngay (cổng hiện mở, chưa ghi lượt hiển thị, chưa đủ 55 phút; yêu cầu đang chạy không tính). Chừa một quảng cáo cho thẻ của bước hiện tại: thẻ chưa có quảng cáo thì cần ít nhất 2, có rồi (hay bước hiện tại không có thẻ) thì ít nhất 1. Đọc ở khung hình đầu của bước và mỗi 200 ms; đã có thì giữ là có cho bước đó, trừ khi thẻ thôi được phép (OB-04). Hàm đi qua cùng cổng với thẻ: cổng đóng thì báo chưa có, và app không chờ (OB-04); thời gian chờ và bước nào chờ do spec của app đặt | app, ads | Không có (luật của ta)". Câu chữ cuối theo trả lời câu hỏi 1, 2.
  - Mục 11, bảng hằng số, dòng mới cạnh dòng native thu gọn: "Đọc lại pool cho bước kế tiếp (OB-05) | 200 ms; chỉ đọc, không gửi yêu cầu | Không có".
- `docs/ops/ads-guide.md`:
  - Mục 3, bảng định dạng, dòng "Native ở một bước của luồng" (dòng 48): thêm vế "app biết bước kế tiếp đã có quảng cáo bằng `rememberNativeStepNextAdReady(placement, step, currentStepHasAd)`".
  - Mục 4, gạch "Thẻ ở bước của luồng" (dòng 77): thêm đoạn về `rememberNativeStepNextAdReady`: luật chừa một quảng cáo; `currentStepHasAd` đặt theo bước (cờ của bước trước còn nguyên khi thẻ đang chờ), và là `true` khi bước hiện tại không có thẻ; kết hợp với `rememberNativeStepAdAllowed` như mục D (chỉ chờ khi thẻ được phép); hàm chỉ đọc, giữ `true` cho bước; app hết chờ khi ra nền, có giới hạn thời gian do spec của app đặt; chỉ gọi ở bước mà bước kế tiếp có thẻ, để không đọc pool ở bước khác.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt` | `claimableAdCount()` (mục 3.1 A; khung rỗng ở bước 3, mục 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `poolClaimableAdCount(placement)` ngay sau `poolHasFreeAd` (mục 3.1 B) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt` | `nextStepAdReady`, `rememberNativeStepNextAdReady`, `NEXT_STEP_AD_READ_MILLIS`, `rememberNextStepAdReady`, hai import; một câu trong KDoc của `NativeStepAd` (mục 3.1 C) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdPoolTest.kt` | Test 1 tới 5 |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeStepNextAdReadyTest.kt` | Test 6 tới 20 (Robolectric, `createAndroidComposeRule<ComponentActivity>()`, khung `w411dp-h891dp-xxhdpi`, không chụp ảnh) |
| Sửa | `docs/ops/ads-business-rules.md` | OB-05 mới, một dòng ở bảng hằng số mục 11 (mục 3.1 F) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3 dòng 48, mục 4 dòng 77 (mục 3.1 F) |

Nơi đọc các file bị đổi (`grep -rn` trong `scripts/`, `.github/`, `*/src/test`):

- `NativeAdPool.kt`, `AdsController.kt`, `NativeFlowStep.kt`: không script hay workflow nào đọc nội dung; chỉ test của `:core:ads` dùng các lớp này. Plan chỉ thêm hàm, không đổi chữ ký hay hành vi có sẵn (mục 3.5).
- `ads-business-rules.md`, `ads-guide.md`: `scripts/history-refs.py` quét (không có trong danh sách cho phép, nên chữ mới không ghi mã ticket, bài học hay phiên).
- `scripts/ads-plan.py` nhắc `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md` trong chú thích và trong chữ của sheet (dẫn mục 5, mục 11, mục 3), và có bảng `CONSTANTS` chép tay một phần bảng hằng số mục 11 cho nhóm kinh doanh (không phải mọi dòng: ví dụ dòng chờ native thu gọn 10 giây không có), không đọc tệp lúc chạy, không có test so hai bảng. Plan không đổi số mục nào và nhịp đọc 200 ms không phải thông số kinh doanh, nên không sửa `scripts/ads-plan.py`, Excel không đổi.
- Các test sau nhắc `docs/ops/ads-business-rules.md` trong chú thích, chỉ để dẫn mã luật đã có (mục, mã như S-14, P-06, N-05); plan chỉ thêm OB-05 và một dòng hằng số, không đổi số mục hay mã luật nào, nên không test nào bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`.
- Ảnh chuẩn screenshot: không đổi; test mới không chụp ảnh, không có lớp nào gọi đường bị đổi.
- `NativeFlowStepTest.kt` có hằng số riêng và test của `rememberStepReady`; plan không sửa tệp này (test mới ở lớp riêng, để không thêm rule thứ hai vào lớp có ảnh chụp và giữ tệp 789 dòng không dài thêm).

Không đổi trong PR của task: `docs/features/ads/spec.md` (sửa riêng trước khi duyệt, câu hỏi 1), `docs/lessons.md`, ticket T-101 và plan này (phiên ghi bài học `applied`, nhật ký và trạng thái qua PR báo cáo).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay nhịp hiện. Thêm một hàm chỉ đọc mà app dùng để giữ nút đi tiếp chờ quảng cáo của bước kế tiếp (luật OB-05 mới, điểm chặn 1 theo ticket). Dự án mẫu không có luồng nào gọi hàm mới, nên hành vi quảng cáo của dự án mẫu không đổi.
- Sự kiện phân tích: Không chạm. Không ghi `ad_opportunity` (P-07) hay `af_native_ad_eligible`.
- File R2: Không chạm.
- Dữ liệu cục bộ: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code dùng Compose runtime (`mutableStateOf`, `setValue`, `LaunchedEffect`, `rememberUpdatedState`) và `kotlinx.coroutines.delay`, đã có trên classpath của `:core:ads` (`NativeFlowStep.kt` đã dùng). Test dùng thư viện đã có trên classpath test của `:core:ads`: Compose UI test (`createAndroidComposeRule`, đã dùng trong `NativeFullScreenOverlayTest`), Robolectric (`shadowOf(Looper.getMainLooper())`), `androidx.lifecycle.Lifecycle` (`moveToState`, đã dùng trong `InListAdsTest`), `RecordingAppLogger` của `:core:logging`, `FakeNativeAdLoader`, `FakeLoadedNativeAd`, `FakeRemoteSettings`, `testAdsController`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** test 15 (ra nền rồi trở lại) với `mainClock.autoAdvance = false`: khi activity ở `CREATED`, đồng hồ của test có còn chạy `delay` của hiệu ứng không.
- **Điều kiện chuyển:** test 15 đỏ ở câu khẳng định sau `moveToState(RESUMED)` vì giá trị chưa đổi sau `advanceTimeBy(NEXT_STEP_AD_READ_MILLIS + FRAME_MILLIS)`, hai lần sửa phần dựng không qua, trong khi test 9 (đọc lại không qua vòng đời) qua.
- **Cách làm thay:** để `mainClock.autoAdvance = true` riêng cho test 15 và chờ có giới hạn `composeRule.waitUntil(timeoutMillis = 2_000) { ready == true }` sau `RESUMED`. Câu khẳng định giữ nguyên (sau khi quay lại, giá trị theo số mới nhất), không đổi code chính.

### 3.5. Hành vi đã có bị đổi

Chạy `python3 scripts/existing-tests.py docs/features/ads/plans/T-101-pool-cho-buoc-ke-tiep.md` (2026-11-10): kết quả ghi ở dòng dưới bảng.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `NativeAdPool`: `claim`, `release`, `refill`, `hasFreeAd`, `exhausted`, `close`, hạn dùng | Không đổi; chỉ thêm `claimableAdCount()` chỉ đọc | `NativeAdPoolTest` (mọi test, ví dụ `hasFreeAd_falseWhileRequestInFlight_trueWithFreeAd`, các test hạn dùng 55 phút) | Giữ |
| `AdsController.poolHasFreeAd`, `poolExhausted`, `stepAd` | Không đổi; thêm `poolClaimableAdCount` cạnh đó | `InListAdsTest` (lưới hỏi pool), `NativeStepAdStatesTest`, `NativeFlowStepTest.stepAd_*` | Giữ |
| `NativeStepAd`, `rememberNativeStepAdAllowed`, `rememberStepReady`, `rememberNativeFullScreenStepReady` | Không đổi; chỉ thêm một câu KDoc cho `NativeStepAd` | `NativeStepAdAllowedTest` (mọi test), `NativeFlowStepTest.stepReady_*`, `NativeFlowStepLayoutTest`, `NativeFlowStepScreenshotTest` | Giữ |

Plan không sửa hay xóa dòng code nào đang chạy: chỉ thêm hàm mới, hằng số mới, import và một câu KDoc.

Kết quả của `existing-tests.py` (2026-11-10, trên `origin/main` `119f91b`): 525 test dùng tên của ba tệp nguồn; 17 ở nhóm "Cần xem trước", 161 ở nhóm "Nên xem". Mười bảy test "Cần xem trước" khớp vì tên chung mà plan nhắc khi mô tả (`AdsController`, `poolExhausted`, `poolHasFreeAd`, `refill`, `release`, `close`, `NativeStepAdHost`), không vì hành vi plan đổi: `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`; `InListAdsTest.lazyColumn_pooledRow_poolExhausted_keepsZeroHeightRow`, `positions_followEvery`; `NativeAdCellTest.twoMediumCells_showDifferentAds`; `NativeAdFontScaleTest.fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales`; `NativeFlowStepTest.nextButton_samePlace_notCovered_withCard`, `stepAd_allowedTurnsFalse_hidesAndReportsFalse`, `stepAd_drawsOnlyWhenAllowed`, `stepAd_loading_keeps250dp`, `stepAd_stepChange_showsAnotherAdOfThePool`, `stepAd_unavailable_takesNoSpace`, `stepCard_doesNotSuppress`; `NativeFullScreenAdScreenTest.closeButton_drawnAndTouch48dp`, `closeButton_isTopLeft_awayFromAdChoices`, `media_startsAt88dp`; `NativeFullScreenOverlayTest.smallScreen_countdownAndCtaInside`; `RewardedActionTest.realController_adFreeWhileDialogOpen_watchRunsAction`; lần chạy sau khi viết mục này còn sáu test khớp vì tên `start` mà mục 3.4 nhắc: `AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`; `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`; `RewardedActionTest.longContent_small_buttonsInsideWindow`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `watchButton_is56dpHigh_fullCardWidth` (bố cục của màn xem thử, lớp thu gọn, hộp thoại rewarded). Chúng kiểm bố cục, chiều cao, pool của lưới và thẻ, nút đóng của quảng cáo toàn màn hình; plan không đổi `poolExhausted`, `poolHasFreeAd`, `refill`, `release`, `close`, `NativeStepAdHost` hay bố cục nào: giữ. Lần chạy cuối (sau khi sửa theo rà soát) có 284 test "Nên xem", khớp cùng các tên chung đó (`isEnabled`, `release`, `refill`, `close`, `start`, `pool`, `openConsentGateForTest`) và kiểm hành vi plan không đổi: giữ.

## 4. Kiểm thử

Mỗi test viết trước code (bước 3), chạy và thất bại đúng lý do ở một câu khẳng định, không vì không biên dịch được (`auto-run.md`, bước 3), rồi mới viết code (bước 4). Sau bước 4, chạy các đột biến đối chứng ở cuối mục này: mỗi đột biến làm đúng test ghi kèm đỏ, rồi trả tệp về.

**Khung rỗng (commit test của bước 3):** để test biên dịch được mà vẫn đỏ, bước 3 thêm khung rỗng cùng chữ ký của mục 3.1, không có logic:

- `NativeAdPool.claimableAdCount(): Int = 0`.
- `AdsController.poolClaimableAdCount(placement: AdPlacement): Int = 0`.
- `fun nextStepAdReady(claimable: Int, currentStepHasAd: Boolean): Boolean = false`.
- `@Composable fun rememberNativeStepNextAdReady(placement, step, currentStepHasAd): Boolean = false` (chưa có `requireStepFormat`, chưa đọc cổng, chưa log).
- `internal const val NEXT_STEP_AD_READ_MILLIS = 200L` (test đọc hằng số này).
- `@Composable internal fun rememberNextStepAdReady(allowed, step, currentStepHasAd, claimable): Boolean = false` (không gọi `claimable`).

Bước 4 thay thân các hàm bằng mục 3.1. Lý do đỏ trên khung rỗng, đã đối chiếu từng câu khẳng định:

| Test | Câu khẳng định đỏ trên khung rỗng |
| --- | --- |
| 1, 2, 4, 5 | Lần đọc đầu mong `1`, `3`, `3`, `2` mà khung trả `0` |
| 3 | Câu đầu (`0` khi `canShow = false`) qua; đối chứng mong `1` khi `canShow = true`, khung trả `0` |
| 6 | `nextStepAdReady(2, false)` mong `true`, khung trả `false` |
| 7 | Câu đầu (`false` không có controller) qua; đối chứng `rememberNextStepAdReady(allowed = true, ...) { 1 }` mong `true`. **7b**: khung không ném `IllegalArgumentException` |
| 8, 9, 10, 11, 12, 13, 15 | Câu mong `true` (khung luôn `false`); 10: câu đầu `false` qua, đối chứng `true` đỏ |
| 14a | Câu đầu (`false`, không gọi `claimable`) qua; đối chứng `allowed = true` mong `true` và có lần gọi. **14b**: câu đầu mong `true` |
| 16, 18, 19, 20 | Cần hàm thành `true` trước (16: câu cuối; 18, 20: bước dựng; 19: dòng log) |
| 17 | Qua ngay trên khung (khung không chạm pool): test canh giữ, xem ngoại lệ viết test trước và đột biến 9 |

Dựng chung:

- Test 1 tới 5 trong `NativeAdPoolTest` dùng hàm phụ có sẵn của lớp: `newPool(capacity)`, `fill(pool, n)`, `deliver()`, `at(millis)`, biến `canShow`, `loads.times`, `logger.entries`, hằng `MAX_AGE`.
- Test 6 tới 20 trong lớp mới `NativeStepNextAdReadyTest` (`@RunWith(AndroidJUnit4::class)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, `createAndroidComposeRule<ComponentActivity>()`). Hai hàm phụ, như ở app: `frame()` = `Snapshot.sendApplyNotifications()` rồi `composeRule.mainClock.advanceTimeByFrame()`; `poll()` = `Snapshot.sendApplyNotifications()` rồi `composeRule.mainClock.advanceTimeBy(NEXT_STEP_AD_READ_MILLIS + FRAME_MILLIS)` (`FRAME_MILLIS = 32L`). Test với số đếm tiêm vào gọi `rememberNextStepAdReady(allowed, step, currentStepHasAd) { count }` với `mainClock.autoAdvance = false`.
- Test 16 tới 20 dùng `mainClock.autoAdvance = false`; mỗi bước của thẻ chạy vòng lặp chính trước (`looper.idle()`, `looper.idleFor(Duration.ofMillis(PoolCellPolicy.RETRY_MILLIS))` để thẻ lấy quảng cáo), rồi mới `poll()` cho hàm mới đọc; job thu `stepAd` chạy trong `try` và bị hủy trong `finally` (`cardScope.cancel()`, `looper.idle()`), như test của app.
- Test 16 tới 20 dùng `AdsController` thật qua `testAdsController(flags = DefaultAdsFlags(settings, adFreeStatus(adFree)), logger = logger)` và các điểm nối có sẵn: `nativeLoaderForTest = FakeNativeAdLoader()` (bộ nạp giả giữ yêu cầu tới khi test gọi `deliver`), `openConsentGateForTest()`, `closeConsentGateForTest()`, `reopenConsentGateForTest()` (cổng đồng ý thật), `setAppForegroundForTest(...)`, `markSdkReadyForTest()`; thẻ của bước là `controller.stepAd(GALLERY_NATIVE_STEP, MutableStateFlow<Any>(1))` thu thập trong `CoroutineScope(Dispatchers.Main)` (luồng các bước không kết thúc, như `NativeStepAdHost`), `shadowOf(Looper.getMainLooper()).idle()`. Câu khẳng định đọc giá trị trả về của hàm Compose, `loader.requests.size` và dòng log trong `RecordingAppLogger`. Đã chạy thử các điểm nối này trên nhánh (mục 4.1): thẻ lấy quảng cáo đầu, pool nạp bù yêu cầu thứ hai, quảng cáo thứ hai rảnh, ở nền không có yêu cầu mới.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đề xuất 10: chỉ tính quảng cáo cấp được ngay, theo nạp và lấy | Unit test | **1.** `NativeAdPoolTest.claimableAdCount_followsLoadAndClaim`: pool mới `0`; `refill()`, `runCurrent()`: vẫn `0` (yêu cầu đang chạy không tính); `deliver()` ba lần: `1`, rồi `3`; `claim` ba lần: `2`, `1`, `0`, rồi `claim` trả `null` (khớp `claim`); `release(a)` (chưa ghi lượt hiển thị): `1`, `claim` trả lại đúng `a`, `0` |
| P-01a, P-03 (hạn dùng) | Unit test | **2.** `claimableAdCount_skipsImpressedAndExpired`: `fill(pool, 3)`: `3`; `a.impressionRecorded = true`: `2`; đối chứng: `at(MAX_AGE - 1)` vẫn `2`; `at(MAX_AGE)`: `0`, và `claim` trả `null` (khớp `claim`) |
| S-11, KB-12 | Unit test | **3.** `claimableAdCount_zeroWhenCannotShow`: `newPool(capacity = 1)`, `fill(pool, 1)`, `canShow = false`: `0`, `claim` trả `null`, quảng cáo không bị hủy (`destroyCount == 0`). Đối chứng: `canShow = true`: `1`, `claim` trả đúng quảng cáo đó |
| Chỉ đọc: không nạp, không hủy, không log | Unit test | **4.** `claimableAdCount_hasNoSideEffect`: `fill(pool, 3)`, đối chứng đọc `3` trước khi cũ; `ads[0].impressionRecorded = true`, `at(MAX_AGE)`, ghi số dòng log; đọc ba lần đều `0`, `runCurrent()`: `destroyCount` của cả ba là `0`, `loads.times.size` vẫn `3`, số dòng log không đổi. Đối chứng: `claim` trên cùng pool trả `null`, `runCurrent()`: cả ba bị hủy (`1, 1, 1`) và có yêu cầu thứ tư |
| Pool đóng rồi mở lại (rút đồng ý, tắt mọi vị trí) | Unit test | **5.** `claimableAdCount_zeroWhenClosed_countsAfterReopen`: `fill(pool, 2)`: `2`; `close()`: `0`; `reopen()`, `refill()`, `runCurrent()`, `deliver()`: `1` |
| Đề xuất 10: luật chừa một quảng cáo | Unit test | **6.** `NativeStepNextAdReadyTest.nextStepAdReady_keepsOneForCurrentCard`: `(0, false)` `false`; `(1, false)` `false`; `(2, false)` `true`; `(0, true)` `false`; `(1, true)` `true`; `(3, true)` `true`; bước hiện tại không có thẻ (app truyền `currentStepHasAd = true`, chú thích trong test): `nextStepAdReady(claimable = 1, currentStepHasAd = true)` là `true` |
| Không có `AdsController`; sai định dạng | Compose UI test | **7.** `noController_false`: `rememberNativeStepNextAdReady(GALLERY_NATIVE_STEP, step = 1, currentStepHasAd = true)` không có `LocalAdsController`: `false` sau `frame()`; đối chứng trong cùng nội dung: `rememberNextStepAdReady(allowed = true, step = 1, currentStepHasAd = true) { 1 }` là `true`. **7b.** `wrongFormat_throws`: `rememberNativeStepNextAdReady(GALLERY_NATIVE_MEDIUM, ...)` ném `IllegalArgumentException` |
| Đọc ở khung hình đầu của bước | Compose UI test | **8.** `firstFrame_readsPool`: số đếm `2`, cờ `false`, lưu mọi giá trị trả về vào danh sách: giá trị đầu tiên (lần hợp thành đầu) là `true`, số lần gọi `claimable` ít nhất 1 trước `frame()` đầu |
| Đọc lại mỗi 200 ms | Compose UI test | **9.** `followsPool`: số `0`, cờ `false`: `false` sau `frame()` và sau một `poll()`; số `2`: `true` sau đúng một `poll()` |
| Đề xuất 10: quảng cáo vừa về là của thẻ đang chờ | Compose UI test | **10.** `cardWaiting_oneAdArrives_staysFalse`: số `0`, cờ `false`; số `1`: `false` sau ba `poll()`; đối chứng: số `2`: `true` sau một `poll()` |
| Giữ `true` giữa lúc thẻ lấy và lúc thẻ báo | Compose UI test | **11.** `cardClaims_countFallsBeforeReport_staysTrue`: số `2`, cờ `false` (trạng thái Compose): `true` sau `frame()`; số `1`, `poll()`: `true`; số `0`, `poll()`: `true`; cờ `true`, `poll()`: `true` |
| Cờ mới nhất đi cùng số mới nhất | Compose UI test | **12.** `cardReportsAd_latestFlagWithLatestCount`: số `1`, cờ `false`: `false` sau `frame()` và một `poll()`; cờ thành `true` (số vẫn `1`), `poll()`: `true` |
| Bước mới đọc lại | Compose UI test | **13.** `newStep_readsAgain`: số `2`, bước `1`: `true`; số `0`, bước `2`, `frame()`: `false` (giá trị `true` của bước trước không mang sang); số `2`, `poll()`: `true` |
| KB-20 tiêu chí 9: cổng đóng thì không đọc và trả `false`; cổng mở rồi đóng lại rồi mở | Compose UI test | **14a.** `gateClosed_falseAndNoRead`: `allowed = false`, số `2`, đếm số lần gọi `claimable`: `false` sau `frame()` và ba `poll()`, số lần gọi `0`. Đối chứng: `allowed = true` (trạng thái Compose): `true` sau một `frame()`, số lần gọi ít nhất 1. **14b.** `gateOpenThenClosed_falseAtOnce_thenReadsAgain`: `allowed = true`, số `2`: `true`; `allowed = false`, `frame()`: `false` ngay; số `0`, `allowed = true`, `frame()`: `false` (đọc lại, không giữ `true` của lần mở trước); số `2`, `poll()`: `true` |
| Ra nền rồi quay lại | Compose UI test (activity thật) | **15.** `background_thenBack_followsLatestCount`: số `0`, cờ `false`: `false` sau `frame()`; `composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)`; số `2`; `moveToState(Lifecycle.State.RESUMED)`; sau một `poll()`: `true`. Không khẳng định giá trị lúc ở nền (hàm không hiện gì; phần chờ khi ra nền là của app). Phương án dự phòng: mục 3.4 |
| Qua controller thật: đọc đúng pool của vị trí, luật chừa trên pool thật | Unit test (Robolectric) | **16.** `withController_readsPoolOfPlacement` (như test của app): `GALLERY_NATIVE_STEP` bật, đồng ý, tiền cảnh, SDK sẵn sàng; nội dung đọc `rememberNativeStepNextAdReady(GALLERY_NATIVE_STEP, step = 1, currentStepHasAd = hasAd)`: pool rỗng `false`. Thẻ thu thập `stepAd`: `loader.requests.size == 1`; `deliver`, `idleFor(RETRY_MILLIS)`: thẻ có `Loaded`, `loader.requests.size == 2`, `poll()`: `false` (một quảng cáo đang dùng, không rảnh); `deliver` thứ hai, `poll()`: `false` (một rảnh, cờ còn `false`: hàm chừa nó cho thẻ); `hasAd = true`, `poll()`: `true` |
| Không tạo pool, không gửi yêu cầu (canh giữ) | Unit test (Robolectric) | **17.** `withController_noCard_makesNoPool`: cùng dựng, chỉ có hàm mới, không có thẻ: `false` sau `frame()` và mười `poll()` (2 giây); `logger.entries` không có dòng `"Pool native_step created for gallery_native_step"` (dòng mà `NativeAdPools.pool` ghi khi tạo pool; `existingPool` không tạo, đột biến 9 đổi sang `pool` thì có dòng này và test đỏ); `loader.requests` rỗng. Đối chứng: thêm thẻ thu thập `stepAd`: có đúng một dòng `"Pool native_step created for gallery_native_step"` và `loader.requests.size == 1` (pool được tạo bởi thẻ). Phần ở nền chỉ là đối chứng (cổng gửi đã chặn mọi yêu cầu ở nền, nên không phân biệt được hai cách tìm pool): `setAppForegroundForTest(false)`, ghi `loader.requests.size`, mười `poll()`: không đổi |
| KB-12, KB-20 tiêu chí 9 qua cổng đồng ý thật | Unit test (Robolectric) | **18.** `withController_consentClosed_falseAtOnce_reopenReadsAgain`: dựng như 16 tới khi hàm mới `true`; `closeConsentGateForTest()`, `idle()`, `frame()`: `false`; `reopenConsentGateForTest()`, `idle()`: pool vừa mở lại chưa có quảng cáo (đã đóng thì hủy quảng cáo rảnh), `poll()`: `false` (không giữ `true` cũ) |
| Log mỗi bước một dòng | Unit test (Robolectric) | **19.** `withController_logsOncePerStep`: dựng như 16 tới khi `true`; thêm năm `poll()`: đúng một dòng `"Step gallery_native_step: next step ad ready"` trong `logger.entries` (không ghi mỗi 200 ms); dòng không chứa khóa bước |
| KB-7, KB-30, khóa vị trí, KB-20 tiêu chí 9: mọi cổng (không chỉ đồng ý) qua controller thật | Unit test (Robolectric) | Theo mẫu `NativeStepAdAllowedTest.stepAdAndAllowed_sameGate`: nội dung đọc cả `rememberNativeStepAdAllowed(GALLERY_NATIVE_STEP)` vào `allowed` và hàm mới vào `ready`; mỗi test dựng mới như 16 tới khi `ready` `true` (và `allowed` `true`). **20a.** `withController_adFreeOn_falseAtOnce`: `adFree.value = true`, `looper.idle()`, `frame()`: `allowed` `false` và `ready` `false` trong cùng khung. `adFree.value = false`, `idle()`, `frame()`: `allowed` `true`; pool vừa đóng (mọi vị trí của nó tắt) đã hủy quảng cáo rảnh, bộ nạp giả chưa trả quảng cáo nào, `poll()`: `ready` `false` (đọc lại, không giữ `true` cũ). **20b.** `withController_placementKeyOff_falseAtOnce`: `settings.set(AdsSettings.placementEnabled(GALLERY_NATIVE_STEP), false)`, `idle()`, `frame()`: `allowed` `false`, `ready` `false`; bật lại, `idle()`, `poll()`: `allowed` `true`, `ready` `false`. Đột biến 10 (hàm chỉ đọc cổng đồng ý) làm cả hai đỏ (`ready` giữ `true`) |
| Bản dự án mẫu không có luồng gọi hàm mới | Kiểm trong `scripts/check.sh` | Mọi test có sẵn của `:core:ads`, `:feature:sample`, `:app` qua (không đổi hành vi) |
| Tài liệu đúng luật mới và không ghi mã lịch sử | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR của task đọc OB-05, dòng hằng số ở mục 11 của `ads-business-rules.md` và hai chỗ của `ads-guide.md`, so với spec đã sửa (câu hỏi 1). `python3 scripts/history-refs.py` qua (cũng chạy trong `scripts/check.sh`) |

Ghi chú test 16: câu khẳng định giữa là "một quảng cáo rảnh, thẻ chưa báo: quảng cáo đó được tính cho thẻ" (thẻ đã có quảng cáo thật nhưng cờ app truyền còn `false`, nên hàm chừa một quảng cáo; đây là cách hiểu an toàn: cờ chậm thì chờ thêm, không mở sớm).

**Đột biến đối chứng** (sau bước 4, mỗi đột biến đổi tạm bằng `perl -pi -e`, chạy `git diff --stat -- <tệp>` thấy tệp đổi, chạy `./gradlew :core:ads:testDebugUnitTest --tests '*NativeStepNextAdReadyTest*' --tests '*NativeAdPoolTest*'`, thấy đúng test ghi kèm đỏ, rồi `git checkout -- <tệp>`; lệnh dùng đường dẫn đầy đủ). Mỗi mẫu khớp đúng một chỗ của code ở mục 3.1:

1. Bỏ luật chừa: `perl -pi -e 's/\Qif (currentStepHasAd) 1 else 2\E/if (currentStepHasAd) 1 else 1/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 6, 10, 16 đỏ.
2. Bỏ đọc cờ mới nhất: `perl -pi -e 's/\QnextStepAdReady(count(), hasAd)\E/nextStepAdReady(count(), currentStepHasAd)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 12 đỏ.
3. Bỏ `canShowNow`: `perl -pi -e 's/\Qif (!canShowNow()) return 0\E//' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt`: test 3 đỏ.
4. Bỏ hạn dùng: `perl -pi -e 's/\Q && now - it.loadedAtMillis < NativeAdPolicy.MAX_AGE_MILLIS\E//' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt`: test 2 đỏ (có thể kèm cảnh báo biến `now` không dùng; chỉ là lần chạy tạm).
5. Đọc mà dọn pool: `perl -pi -e 's/\Qif (!canShowNow()) return 0\E/if (!canShowNow()) return 0; pruneExpired()/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAdPool.kt`: test 4 đỏ.
6. Bỏ vòng đọc lại: `perl -pi -e 's/\Qwhile (!ready)\E/while (false && !ready)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 9 đỏ.
7. Giá trị đầu không đọc pool: `perl -pi -e 's/\QmutableStateOf(allowed && nextStepAdReady(claimable(), currentStepHasAd))\E/mutableStateOf(false)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 8 đỏ (giá trị đầu `false`).
8. Không đọc lại khi cổng đổi (bỏ khóa `allowed` của `remember`): `perl -pi -e 's/\Qremember(step, allowed) { mutableStateOf\E/remember(step) { mutableStateOf/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 14b, 18, 20a, 20b đỏ (cổng đóng mà vẫn trả `true` đã giữ).
9. Tạo pool khi đọc: `perl -pi -e 's/\QnativePools.existingPool(placement)?.claimableAdCount()\E/nativePools.pool(placement)?.claimableAdCount()/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt`: test 17 đỏ (có dòng `Pool native_step created for gallery_native_step` trước khi có thẻ).
10. Chỉ đọc cổng đồng ý: `perl -pi -e 's/\Qval stepAllowed = ads.rememberStepAllowed(placement)\E/val stepAllowed = ads.canRequestAds.collectAsState().value/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 20a, 20b đỏ (`ready` giữ `true` khi `adFree` bật hay khóa vị trí tắt); `collectAsState` đã được import trong tệp.
11. Đọc lại liên tục (không giữ `true`): `perl -pi -e 's/\Qwhile (!ready)\E/while (true)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 11 đỏ (số giảm trước khi thẻ báo thì về `false`).
12. Không đọc lại ở bước mới (bỏ khóa `step` của `remember`): `perl -pi -e 's/\Qremember(step, allowed) { mutableStateOf\E/remember(allowed) { mutableStateOf/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 13 đỏ (giá trị `true` của bước trước mang sang).
13. Đọc pool khi cổng đóng: `perl -pi -e 's/\QmutableStateOf(allowed && nextStepAdReady\E/mutableStateOf(nextStepAdReady/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStep.kt`: test 14a đỏ (có lần gọi `claimable` và giá trị `true` khi `allowed = false`).

Kết quả từng đột biến ghi vào nhật ký của plan.

**Ngoại lệ viết test trước:** phần chỉ là tài liệu (`ads-business-rules.md`, `ads-guide.md`) và KDoc. Test canh giữ: test 17 qua ngay trên khung rỗng (khung không chạm pool, nên không tạo pool, không gửi gì); nó khóa việc code thật vẫn đọc bằng `existingPool`, đối chứng là đột biến 9 (đổi sang `pool`) làm nó đỏ, và đối chứng trong chính test (có thẻ thì có dòng tạo pool và một yêu cầu) cho thấy câu khẳng định đọc được dòng log đó. Không có kiểm tra trên emulator hay máy thật: dự án mẫu không có luồng nào gọi hàm mới, và màn xem thử không đổi hành vi.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-10, trên nhánh `claude/plan-T-101-pool-buoc-ke-tiep` (từ `origin/claude/from-app-444-445` `f108aa3`, code `:core:ads` như nhánh chính `e078e7a`), bằng test tạm `T101TempSeamTest` trong `core/ads/src/test` (đã xóa, không commit): `testAdsController` với `nativeLoaderForTest = FakeNativeAdLoader()`, `GALLERY_NATIVE_STEP` bật, `openConsentGateForTest()`, `setAppForegroundForTest(true)`, `markSdkReadyForTest()`: trước khi có thẻ `poolHasFreeAd` là `false` (chưa có pool); thẻ thu thập `controller.stepAd(GALLERY_NATIVE_STEP, MutableStateFlow<Any>(1))`: 1 yêu cầu; `deliver`, `idleFor(500 ms)`: trạng thái `[Loading, Loaded]`, 2 yêu cầu; `poolHasFreeAd` `false` (một quảng cáo đang dùng); `deliver` thứ hai: `poolHasFreeAd` `true`; `setAppForegroundForTest(false)` rồi `idleFor(3 000 ms)`: số yêu cầu giữ nguyên 3.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*T101TempSeamTest*' -i` (JDK của `scripts/local-setup.sh`), qua trong 1 phút 12 giây. Không cần `scripts/check.sh`: tiêu chí E dưới 2, không có kỹ thuật hay thư viện mới.
- **Kết quả:** các điểm nối cho đúng số yêu cầu và trạng thái mà test 16, 17 khẳng định (`poolClaimableAdCount` chưa có nên lần chạy thử dùng `poolHasFreeAd` thay; cùng `existingPool`). Test của app với cùng cách dựng (14 test) đã qua ở app.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: luật mới OB-05 cho app chờ quảng cáo của bước kế tiếp, theo ticket), 7 (module core `:core:ads`, API công khai mới). Không chạm 2, 3, 4 (không thêm hay sửa sự kiện), 5, 6, 8, 9. Người phụ trách sản phẩm (anh Nhật) xác nhận spec đã sửa trước khi duyệt (câu hỏi 1).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Ba tệp nguồn của `:core:ads`, có giao diện công khai mới (`nextStepAdReady`, `rememberNativeStepNextAdReady`); hai tài liệu hướng dẫn. Một module |
| B. Độ rõ của yêu cầu | 2 | Spec chưa có tiêu chí về bước kế tiếp (câu hỏi 1); còn câu hỏi ảnh hưởng tới cách làm (giá trị khi cổng đóng, cách app báo cờ: câu hỏi 2, 3). Có bản đã chạy ở app |
| C. Khả năng kiểm chứng tự động | 2 | Mọi điểm code có test JVM, kể cả `AdsController` thật và 13 đột biến đối chứng; nhưng hành vi phụ thuộc thời gian (đọc lại 200 ms, đồng hồ khung hình của test), vòng đời (ra nền, mục 3.4 có dự phòng), và đúng hay sai ở người dùng còn tùy app truyền cờ đúng bước (câu hỏi 3); phần chờ thật chỉ kiểm được ở app |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `ads-business-rules.md`, `ads-guide.md` cũng nằm trong phạm vi của T-102 (plan đang viết song song); không đổi hành vi mà feature khác dựa vào |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn (`rememberStepReady`, `poolHasFreeAd`); bản ở app đã chạy |
| **Tổng** | 9 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeAdPool.kt`, `AdsController.kt`, `NativeFlowStep.kt`, `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`.
- Task khác có thể xung đột:
  - T-102 (plan `draft` ở PR #456, cũng `:core:ads`): sửa `AdsController.kt` (bộ đếm host, `fullScreenHostAttached`, `attachFullScreenHost()`, kiểm host trong `showNativeFullScreen`, `skipNoOverlayHost`, KDoc của lớp), `NativeFullScreenTurns.kt`, `NativeAfterInterRules.kt`, `NativeFullScreenAdHost.kt` và các test combo; `docs/ops/ads-business-rules.md`: K-01, K-02, K-09 mới, F-08, HD-05 (việc và cột hậu quả), mục 8.2 (dòng `ad_show_fail` mã 3), và có thể thêm dòng vào bảng hằng số mục 11 (1 giây chờ host, 2 giây khi mất host); `docs/ops/ads-guide.md`: mục 5 (vỏ app đặt host), mục 9 (dòng kiểm trước khi phát hành). Chỗ T-101 sửa: một hàm cạnh `poolHasFreeAd` trong `AdsController.kt` (vùng pool, xa vùng combo và host), OB-05 ở bảng 5.11, một dòng ở bảng hằng số mục 11, mục 3 và mục 4 của `ads-guide.md`. Chỗ có thể xung đột khi merge: bảng hằng số mục 11 (cả hai thêm dòng; T-101 đặt dòng cạnh dòng native thu gọn, task sau giữ cả hai dòng), và `AdsController.kt` nếu hai hàm mới rơi gần nhau. Không xung đột ý; T-101 không chạm tệp nào khác của T-102. Plan này không viết phần của T-102.
  - Spec `ads`: câu hỏi 60 và tiêu chí 10 của KB-20 thuộc T-101 (câu hỏi 1). T-102 đề xuất tiêu chí cho KB-16; câu hỏi spec mới của T-102 (nếu có) lấy số sau 60.
  - T-097, T-099 đã xong; `NativeFlowStep.kt` của T-099 (`rememberNativeStepAdAllowed`) là chỗ T-101 thêm hàm ngay sau.
- Đề xuất thứ tự chạy: độc lập với T-102 (`depends_on` rỗng); chạy cùng danh sách được, `run-check.py` sẽ báo cùng sửa `AdsController.kt`, `ads-business-rules.md`, `ads-guide.md`. Đề xuất T-101 trước T-102 (thêm thuần, ít dòng), T-102 áp lên nhánh chính mới.

## 7. Điều kiện dừng riêng

- Một test có sẵn của `NativeAdPoolTest`, `NativeStepAdAllowedTest`, `NativeFlowStepTest`, `NativeStepAdStatesTest` hay `InListAdsTest` đỏ sau bước 4: dừng, vì plan không đổi hành vi có sẵn nào của pool, thẻ hay lưới.
- Một đột biến đối chứng ở mục 4 không làm test ghi kèm đỏ, sau khi kiểm `git diff --stat` thấy tệp đã đổi: dừng (test không bắt được điều plan cần khóa).
- Spec chưa có tiêu chí về bước kế tiếp lúc phiên bắt đầu task (câu hỏi 1 chưa được xác nhận): dừng (plan không được duyệt khi chưa có, điều kiện này chỉ để chắc).

## 8. Câu hỏi cho người duyệt

1. **(Người phụ trách sản phẩm) Thêm tiêu chí "bước kế tiếp đã có quảng cáo" vào KB-20 của spec `ads`.** Spec `confirmed` chưa có; plan chờ spec sửa xong và được xác nhận lại rồi mới duyệt; sau đó mục 2 và mục 4 đổi "đề xuất 10" thành số tiêu chí thật.
   - AI đề xuất: một PR tài liệu riêng (trước khi duyệt plan, ngoài phiên tự chạy) sửa `docs/features/ads/spec.md`: thêm vào KB-20 tiêu chí "**Thêm <ngày>:**" dưới đây, câu hỏi 60 đã trả lời, một dòng lịch sử; anh Nhật xác nhận chữ đầy đủ trên PR.
     - Đề xuất 10: "Khi app giữ nút đi tiếp ở một bước cho tới khi bước kế tiếp có quảng cáo, thì một quảng cáo vừa về lúc thẻ của bước hiện tại còn chờ được tính cho thẻ hiện tại, không cho bước kế tiếp; bước kế tiếp chỉ tính là đã có quảng cáo khi còn một quảng cáo dùng được ngay ngoài quảng cáo của thẻ hiện tại. App chỉ chờ lúc thẻ được phép (tiêu chí 9); bước nào chờ và chờ bao lâu do spec của app đặt. Ở dự án mẫu, tiêu chí này kiểm bằng giá trị của hàm cho biết bước kế tiếp đã có quảng cáo."
   - Câu hỏi 60 đề xuất: "**App biết bước kế tiếp của luồng đã có quảng cáo thế nào?** (KB-20, issue #444) Trả lời: qua một hàm chỉ đọc của khung, cùng cổng với thẻ; chừa một quảng cáo cho thẻ của bước hiện tại khi thẻ chưa có quảng cáo; thẻ không được phép thì hàm báo chưa có và app không chờ (OB-05)."
2. **(Người phụ trách sản phẩm) Khi thẻ không được phép, hàm trả gì?**
   - Phương án A (AI đề xuất, plan viết theo): trả `false` ("chưa có quảng cáo"), đúng nghĩa của hàm và giống `rememberNativeFullScreenStepReady`; app kết hợp với `rememberNativeStepAdAllowed` và chỉ chờ khi thẻ được phép (mục 3.1 D, `ads-guide.md`). Nhược: app quên kết hợp thì giữ nút vô ích tới hết giới hạn thời gian ở người dùng chưa đồng ý hay đã mua gói bỏ quảng cáo.
   - Phương án B: trả `true` khi không được phép (nghĩa thành "không cần chờ"), app chỉ cần đọc một hàm. Nhược: tên và nghĩa lệch ("đã có quảng cáo" mà không có), khác các hàm cùng họ; test 14, 18 đổi; plan lên phiên bản 2.
3. **(Người duyệt) Cờ "thẻ của bước này đã có quảng cáo" do app truyền, hay khung tự biết?** Thẻ không báo gì khi đang chờ quảng cáo mới, nên cờ `true` của bước trước còn nguyên khi sang bước sau; app nguồn phải nhớ bước mà thẻ báo (`cardAdPage == page`).
   - Phương án A (AI đề xuất, plan viết theo): giữ chữ ký của app (`currentStepHasAd` do app truyền), KDoc và `ads-guide.md` ghi rõ cạm bẫy và cách đặt cờ theo bước. Ưu: đúng bản đã chạy và qua rà soát ở app, không đổi `stepAdStates` hay `NativeAdSlotGate` đã có test. Nhược: app có thể truyền cờ sai; khung không kiểm được.
   - Phương án B: khung tự biết (thẻ ghi vào controller bước nào đã có quảng cáo, hàm mới bỏ tham số cờ). Ưu: hết cạm bẫy. Nhược: đổi `stepAdStates` (hành vi có sẵn, nhiều test), phải xử lý hai thẻ cùng vị trí; cỡ lên L; plan lên phiên bản 2.
4. **(Người duyệt) Nhịp đọc lại 200 ms hay 500 ms như P-06?** AI đề xuất 200 ms như app: lần đọc chỉ đếm danh sách trong bộ nhớ, không gửi yêu cầu, và nút mở nhanh hơn khi quảng cáo về. Ghi vào bảng hằng số của `ads-business-rules.md`.
5. **(Người duyệt) Đọc pool đã có (`existingPool`) hay tạo pool khi chưa có (`pool`, như app)?** AI đề xuất `existingPool`, như `poolExhausted`, `poolHasFreeAd` của mẫu: hàm chỉ đọc không tạo pool, không tra mã đơn vị. Chưa có pool (thẻ chưa từng vẽ, chưa nạp sẵn) thì chưa có quảng cáo nào, trả 0 là đúng; test 17 khóa điều này.
6. **(Người duyệt) Ghi một dòng log khi bước kế tiếp đã có quảng cáo?** AI đề xuất có, một dòng `Step <mã vị trí>: next step ad ready` mỗi lần giá trị thành `true` (lần đầu của bước, hay lại sau khi cổng đóng rồi mở; ADR-0019; app nguồn không có). Không cần thì bỏ `LaunchedEffect` log và test 19.
7. **(Người duyệt) `nextStepAdReady` công khai?** AI đề xuất có, như app: app test phần nối của mình với luật này trên số đếm giả, không phải chép luật. Tên theo mô hình chung (bước của luồng), không theo tính năng của một app (ADR-0045).
8. **(Người duyệt) Có cần ADR không?** AI đề xuất không: ADR-0045 để luật nghiệp vụ ở `ads-business-rules.md`; luật mới là OB-05.
9. **(Người duyệt) Giữ `true` cho cả bước dù quảng cáo rảnh sau đó bị lấy mất?** Pool dùng chung theo (định dạng, mã đơn vị): một thẻ khác cùng pool có thể lấy quảng cáo rảnh sau khi hàm đã `true`, hay quảng cáo đủ 55 phút nếu người dùng đứng ở bước rất lâu. AI đề xuất giữ như app: hàm chỉ để quyết lúc mở nút; đọc lại liên tục sẽ làm nút sáng rồi mờ. Bước kế tiếp khi đó hiện khung chờ như hôm nay (P-06).
10. **(Người duyệt) Dừng vòng đọc 200 ms khi vòng đời dưới `STARTED` (app ở nền)?** AI đề xuất không, như vòng lấy 500 ms của `stepAdStates` (cũng chạy tiếp ở nền): mỗi lần đọc chỉ đếm một danh sách trong bộ nhớ, không gửi yêu cầu (test 17 đối chứng ở nền), và hàm không hiện gì; dừng theo vòng đời cần thêm `LocalLifecycleOwner` và test riêng. Nếu anh muốn dừng: bọc vòng lặp bằng `repeatOnLifecycle(STARTED)`, test 15 thêm câu "ở nền không gọi `claimable`"; plan lên phiên bản 2.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
