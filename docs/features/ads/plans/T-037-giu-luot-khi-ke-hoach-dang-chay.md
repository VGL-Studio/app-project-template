---
ticket: T-037
title: Giữ lượt nạp sẵn tới khi kế hoạch đang chạy xong
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5M           # cỡ S, số gợi ý 5.0M (scripts/token-usage.py estimate, 2026-10-25); hai file code, hai file test, một đoạn tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-037: GIỮ LƯỢT NẠP SẴN TỚI KHI KẾ HOẠCH ĐANG CHẠY XONG

## 1. Tóm tắt

Sau T-021, lượt xin nạp sẵn tới khi kế hoạch hay lượt nạp bù native đang chạy ra `IGNORE_RUNNING` và bị bỏ (rà soát độc lập T-021, mức nên sửa, `docs/runs/RUN-20261025-01.md`). Task này đổi hàm thuần `decidePreload` để lượt xin cả kế hoạch khi đang chạy ra `HOLD`, và để `AdPreloader` chạy lượt giữ đúng một lần khi lượt đang chạy xong, quyết lại theo bảng S-23 lúc chạy (quyền "không" hay SDK chưa khởi tạo thì bỏ). Nạp bù native khi đang chạy vẫn bỏ (S-25). `AdsController` không đổi: mọi đường xin nạp sẵn đã đi qua `AdPreloader.request`, nên test ở `PreloadDecisionTest` và `AdPreloaderTest` phủ mọi đường (L-072). Kiểm chứng bằng unit test với thời gian ảo.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-23), luật chi tiết trong `docs/ops/ads-business-rules.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-24 | Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại. | Hôm nay, mạng về (`NETWORK_BACK`, S-24) trong lúc kế hoạch hay nạp bù native đang chạy thì kế hoạch không chạy lại, nên khe toàn màn hình đã về `Idle` (ví dụ interstitial vừa đóng, hay vừa bị chặn lúc gửi khi mất mạng) không tải lại tới lần xin sau. Sau task: lượt đó được giữ và chạy một lần khi lượt đang chạy xong, nên tiêu chí đạt cả khi mạng về giữa lượt chạy. |
| KB-24 (S-20, S-23) | Luật trong phạm vi ticket, không có tiêu chí riêng trong spec. | S-23: "lượt nạp sẵn bị chặn vì cổng đóng được giữ lại, chạy đúng một lần khi cổng mở; không chạy chồng lên kế hoạch đang chạy". Cách hiểu: "không chạy chồng" là không chạy hai lượt cùng lúc, không phải bỏ lượt tới sau. Giữ lượt rồi chạy khi lượt đang chạy xong vẫn không chồng, nên luật hiện có cho phép, không cần sửa spec (câu hỏi 1). Lượt giữ: tối đa một lượt (cùng chỗ giữ với lượt bị cổng chặn), quyết lại bằng `decidePreload` lúc chạy. |
| KB-24 (S-25) | Luật trong phạm vi ticket, không có tiêu chí riêng trong spec. | S-25: nạp bù native "chỉ khi ... không có kế hoạch đang chạy". Đây là điều kiện, không phải hàng chờ: nạp bù khi đang chạy vẫn bỏ, không giữ (câu hỏi 2). |

Mọi đường dẫn tới lượt bị bỏ hôm nay (L-072; đã đọc `AdsController.kt` và `AdPreloader.kt` trên nhánh chính, commit `2f357da`):

| Đường xin | Chỗ gọi | Lý do | Gặp lượt đang chạy được không |
| --- | --- | --- | --- |
| SDK sẵn sàng | `AdsController.startSdk`, `preloader.request(SDK_READY)` | `SDK_READY` | Không trong thực tế (lượt đầu; trước nó mọi lý do khác ra `IGNORE_NEVER_REQUESTED`, nạp bù cần `finishedOnce`). Vẫn có dòng trong bảng quyết định |
| Cổng mở lại | `AdsController.onGateOpened(false)`, `preloader.request(GATE_REOPENED)` | `GATE_REOPENED` | Có |
| Vị trí bật từ xa | `AdsController.collectPreloadTriggers`, `preloader.request(PLACEMENT_ENABLED)` | `PLACEMENT_ENABLED` | Có (Remote Config về giữa lượt `SDK_READY` đầu là trường hợp hay gặp) |
| Mạng về | `AdsController.collectPreloadTriggers`, `preloader.onNetworkReturned()` gọi `request(NETWORK_BACK)` | `NETWORK_BACK` | Có |
| Lượt đang giữ khi cổng đổi | `preloader.onGateChanged()` (collector cổng; và cuối mỗi lượt chạy) | lý do đã giữ | Có: hôm nay `IGNORE_RUNNING` ở đây giữ nguyên lượt (`else -> Unit`) |
| Activity hiện lại | `preloader.onActivityResumed(...)` | `NATIVE_REFILL` | Có; bỏ trước khi gọi `decidePreload` (`running -> "plan running"`), giữ nguyên |

Kiểu lượt đang chạy (`AdPreloader.launch`): lượt `SDK_READY` đầu, lượt cả kế hoạch sau đó (từ `request` ra `RUN`), lượt giữ được chạy (từ `onGateChanged`), lượt nạp bù chỉ native (từ `onActivityResumed`). Mục 4 có test cho mỗi lý do có thể gặp lượt đang chạy với mỗi kiểu lượt.

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0004, ADR-0019 (log), ADR-0024 (theo dõi Internet chung), ADR-0026 (hành vi theo AdsKit PARTNER).

## 3. Giải pháp

### 3.1. Cách làm

**1. Bảng quyết định** (`PreloadDecision.kt`): đổi một dòng của `decidePreload`, giữ thứ tự (SDK chưa khởi tạo, quyền "không", chưa từng xin, đang chạy, rồi `HOLD` hay `RUN`):

```kotlin
running -> if (reason == PreloadReason.NATIVE_REFILL) PreloadDecision.IGNORE_RUNNING else PreloadDecision.HOLD
```

KDoc của `decidePreload` và của `IGNORE_RUNNING` ghi: đang chạy thì lượt cả kế hoạch được giữ (S-23, không chồng), nạp bù native bỏ (S-25). `IGNORE_RUNNING` giữ trong enum, chỉ còn cho `NATIVE_REFILL`. `PreloadGate`, `sdkReadyAt` không đổi.

**2. Bộ điều phối** (`AdPreloader.kt`):

- `request`: không đổi code. `HOLD` (nay gồm cả lượt tới khi đang chạy) vẫn `if (held == null) held = reason`: tối đa một lượt giữ; lượt thứ hai không thêm gì (lý do chỉ ảnh hưởng tới `IGNORE_NEVER_REQUESTED`, mà lượt đã giữ được thì `everRequested` đã true, nên giữ lý do đầu là đủ). Log `Preload PLACEMENT_ENABLED: HOLD` như mọi lượt `HOLD`.
- `onGateChanged`: quyết lại lượt giữ. `RUN` thì chạy cả kế hoạch như hôm nay; `HOLD` thì giữ tiếp (cổng còn đóng, SDK chưa sẵn sàng, hay lượt khác đang chạy); **mọi `IGNORE_*` thì bỏ lượt giữ** và log `Held preload <lý do>: dropped, <quyết định>` (thay log riêng `dropped, consent denied` hôm nay; không test hay tài liệu nào đọc chuỗi cũ, đã `git grep`). Hôm nay chỉ `IGNORE_CONSENT_DENIED` bỏ, các `IGNORE_*` khác giữ (`else -> Unit`): `IGNORE_RUNNING` không còn ra cho lượt giữ (lượt giữ không bao giờ là `NATIVE_REFILL`), còn `IGNORE_SDK_NOT_STARTED`, `IGNORE_NEVER_REQUESTED` không xảy ra với cờ hiện có (`sdkStarted`, `everRequested` chỉ đổi từ false sang true), nhưng bỏ cho đúng yêu cầu "lượt giữ quyết lại khi chạy".
- `launch`: không đổi. Cuối mỗi lượt chạy (cả kế hoạch hay nạp bù), sau `running = false`, đã gọi `onGateChanged()`; nay lượt giữ vì đang chạy chạy ở đây.
- Lượt giữ sau một lượt **nạp bù chỉ native** chạy **cả kế hoạch** (`launch(plan, wholePlan = true)`, như hôm nay trong `onGateChanged`), không chỉ phần còn thiếu (câu hỏi 3): lý do được giữ (`PLACEMENT_ENABLED`, `NETWORK_BACK`, `GATE_REOPENED`) xin cả khe toàn màn hình mà lượt nạp bù không chạm; khe native vừa nạp bù có quảng cáo, đang nạp hay đang chờ thử lại thì `loadIfNeeded` không gửi (F-04), nên chạy lại không thêm yêu cầu thừa. Lượt đó chạy xong thì `finishedOnce` true (đã true từ trước, vì nạp bù cần `finishedOnce`).
- KDoc của lớp và của `onGateChanged`: lượt giữ gồm lượt bị cổng chặn và lượt tới khi đang chạy.

**3. Không đổi:** `AdsController.kt` (mọi đường ở bảng mục 2 đã gọi `preloader.request` hay `onNetworkReturned`; không chỗ nào khác gọi `decidePreload` hay `loadIfNeeded` của khe trong kế hoạch, đã `git grep`), `PreloadPlanRunner.kt`, `AdPreloadPlan.kt`, `onActivityResumed`, `onNetworkReturned` (vẫn tăng `nativeRetryResets` ngay, kể cả khi lượt kế hoạch bị giữ).

**4. Tài liệu** (`docs/ops/ads-guide.md`, mục 6, gạch "Lúc chạy"): thêm câu "Lượt xin tới khi kế hoạch hay lượt nạp bù native đang chạy cũng được giữ (chung chỗ giữ một lượt) và chạy cả kế hoạch một lần khi lượt đang chạy xong; nạp bù native khi đang chạy thì bỏ." Gạch log thêm `Preload PLACEMENT_ENABLED: HOLD`, `Held preload PLACEMENT_ENABLED: run`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PreloadDecision.kt` | Dòng `running` của `decidePreload` (mục 3.1 bước 1), KDoc |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPreloader.kt` | `onGateChanged` bỏ lượt giữ ở mọi `IGNORE_*`, log `dropped, <quyết định>`; KDoc (bước 2) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PreloadDecisionTest.kt` | Dòng `running` của `order` đổi kỳ vọng sang `HOLD`; hai test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPreloaderTest.kt` | `noOverlap_whileRunning` đổi dòng log kỳ vọng sang `HOLD` và kiểm lượt chạy sau; test mới cho mọi đường (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Mục 6, gạch "Lúc chạy" và gạch log (bước 4) |
| Sửa | `docs/features/ads/tickets/T-037-giu-luot-khi-ke-hoach-dang-chay.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-037-giu-luot-khi-ke-hoach-dang-chay.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, `src/androidTest`, `build.gradle.kts`):

- `PreloadDecision.kt`: đọc bởi `AdPreloader.kt`, `AdsController.kt` (`PreloadGate`, `PreloadReason`, `SDK_READY_CAP_MILLIS`, `sdkReadyAt`: không đổi) và `PreloadDecisionTest.kt` (có trong bảng).
- `AdPreloader.kt`: đọc bởi `AdsController.kt` (chữ ký công khai trong module không đổi: `request`, `onGateChanged`, `onNetworkReturned`, `onActivityResumed`, `setPlan`, `nativeRetryResets`, `running`, `finishedOnce`) và `AdPreloaderTest.kt` (có trong bảng).
- `docs/ops/ads-guide.md`: không script, workflow hay test nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng. Đổi lúc gửi yêu cầu nạp sẵn (điểm chặn 1): lượt xin tới khi đang chạy, trước đây bị bỏ, nay chạy cả kế hoạch một lần sau lượt đang chạy. Mỗi lượt chạy cộng tối đa một lượt chạy sau nó (một chỗ giữ); khe có quảng cáo, đang nạp hay đang chờ thử lại không gửi (F-04), nên không thêm yêu cầu cho khe đó.
- Sự kiện phân tích: Không chạm. Lượt giữ không ghi `AdTelemetry.opportunity` (như mọi lượt nạp sẵn, A-04, HD-06).
- Dữ liệu: không lưu gì trên máy; lượt giữ chỉ trong bộ nhớ.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng `kotlinx-coroutines-test` (`libs.kotlinx.coroutines.test`) đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Thay đổi là một dòng của hàm thuần và một nhánh `when`, lặp lại mẫu `HOLD` có sẵn của T-021.

## 4. Kiểm thử

Unit test JVM, cùng cách của `AdPreloaderTest` hiện có: `runTest`, `backgroundScope`, `PreloadPlanRunner` thật với `MonotonicClock { testScheduler.currentTime }`, `FakePreloadSlot` (`settleAt` là `var`: `null` không bao giờ xong, nên lượt chạy hết ở trần 6 giây), hàm phụ `at(millis)`, `logged(message)`, cổng giả `gate` (biến, `OPEN` mặc định), `RecordingAppLogger`. Một nhóm một khe không xong: lượt bắt đầu ở t chạy tới t + 6 000; lượt giữ chạy ở chính thời điểm đó.

Test viết trước chạy trên **code hiện tại của nhánh chính** (không cần khung rỗng: chỉ đổi hành vi của hai lớp đã có). Mỗi test mới thất bại vì lượt tới khi đang chạy ra `IGNORE_RUNNING` và bị bỏ. Test giữ hành vi (qua ngay trên code hiện tại) có ghi bước đối chứng (L-055).

Số đo thật trên code hiện tại (L-029; test tạm không commit, `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT037Test*'`, commit `2f357da`, 2026-10-25): khe `INTERSTITIAL` không xong, `request(SDK_READY)` ở 0, `request(PLACEMENT_ENABLED)` ở 2 000, đọc ở 6 000: `expected:<[0, 6000]> but was:<[0]>`; log `Preload SDK_READY: RUN`, `Preload group 1/1 started: ...`, `Preload PLACEMENT_ENABLED: IGNORE_RUNNING`, `Preload group 1/1 moved on: timed out`, `Preload plan finished`, không có lượt chạy sau. Đúng lỗi rà soát đã nêu.

Sửa test có sẵn (người duyệt đồng ý, câu hỏi 4): `PreloadDecisionTest.order` dòng `running` và `AdPreloaderTest.noOverlap_whileRunning` mã hóa đúng hành vi mà task đổi theo quyết định của anh Nhật; không test nào khác đổi. Hai chỗ sửa nằm trong commit test của bước 3, cùng các test mới (L-025); commit code của bước 4 không chạm file test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| S-23, bảng quyết định (dòng sửa) | Unit test | `PreloadDecisionTest.order`: dòng `running` (`NETWORK_BACK`, cổng mở, `everRequested`, `running`) đổi kỳ vọng từ `IGNORE_RUNNING` sang `HOLD`; bảy dòng khác giữ nguyên. Code hiện tại thất bại ở dòng này (trả `IGNORE_RUNNING`) |
| S-23, S-25, mọi lý do khi đang chạy (L-072) | Unit test | `PreloadDecisionTest.running_holdsEveryWholePlanReason_ignoresNativeRefill`: với mỗi `PreloadReason.entries`, cổng mở, `everRequested = true`, `running = true`: `SDK_READY`, `GATE_REOPENED`, `NETWORK_BACK`, `PLACEMENT_ENABLED` ra `HOLD`; `NATIVE_REFILL` ra `IGNORE_RUNNING`. Lặp trên `entries` để lý do thêm sau phải có dòng. Code hiện tại thất bại ở bốn lý do đầu; dòng `NATIVE_REFILL` qua ngay, đối chứng là bốn dòng kia trong cùng test |
| S-23, thứ tự bảng khi đang chạy | Unit test | `PreloadDecisionTest.running_losesToSdkConsentAndNeverRequested`: `running = true` cùng `sdkStarted = false` ra `IGNORE_SDK_NOT_STARTED`; cùng `consentDenied = true` ra `IGNORE_CONSENT_DENIED`; `everRequested = false` với `PLACEMENT_ENABLED` ra `IGNORE_NEVER_REQUESTED`; cùng `mayRequest = false` ra `HOLD`. Ba dòng đầu qua ngay trên code hiện tại (thứ tự giữ); dòng cuối là đối chứng, thất bại vì `IGNORE_RUNNING` |
| Không chồng, lượt sau chạy (test sửa) | Unit test, thời gian ảo | `AdPreloaderTest.noOverlap_whileRunning`: giữ phần cũ (khe không xong, `request(NETWORK_BACK)` ở 1 000: `loadCalls == [0]`), đổi log kỳ vọng thành `Preload NETWORK_BACK: HOLD`; thêm: ở 5 999 vẫn `[0]`, ở 6 000 là `[0, 6000]`. Code hiện tại thất bại ở dòng log |
| Đường `PLACEMENT_ENABLED`, lượt `SDK_READY` đầu | Unit test, thời gian ảo | `AdPreloaderTest.placementEnabled_duringFirstRun_runsOnceAfterIt`: khe `INTERSTITIAL` không xong; `request(SDK_READY)` ở 0; `request(PLACEMENT_ENABLED)` ở 2 000: không gọi thêm, log `Preload PLACEMENT_ENABLED: HOLD`; ở 6 000: `loadCalls == [0, 6000]`, log `Held preload PLACEMENT_ENABLED: run`; ở 20 000 vẫn hai lần (chạy đúng một lần). Code hiện tại thất bại: `[0]` |
| Đường `GATE_REOPENED`, lượt cả kế hoạch sau đó | Unit test, thời gian ảo | `AdPreloaderTest.gateReopened_duringLaterRun_runsOnceAfterIt`: khe xong ngay ở lượt `SDK_READY` (0), rồi `settleAt = null`; `request(NETWORK_BACK)` ở 1 000 (`RUN`, chạy tới 7 000); `request(GATE_REOPENED)` ở 3 000: `HOLD`; `loadCalls == [0, 1000, 7000]` ở 7 000 và ở 20 000. Code hiện tại thất bại: `[0, 1000]` |
| Đường `NETWORK_BACK` (qua `onNetworkReturned`), lượt giữ được chạy | Unit test, thời gian ảo | `AdPreloaderTest.networkReturned_duringHeldRun_runsOnceAfterIt`: sau lượt `SDK_READY` (khe xong ở 0, rồi `settleAt = null`), `gate = mayRequest false`, `request(PLACEMENT_ENABLED)` ở 500 (`HOLD` vì cổng); cổng mở và `onGateChanged()` ở 1 000: lượt giữ chạy tới 7 000; `onNetworkReturned()` ở 2 000: `nativeRetryResets` tăng 1 ngay (không chờ), log `Preload NETWORK_BACK: HOLD`; `loadCalls == [0, 1000, 7000]`. Code hiện tại thất bại: `[0, 1000]` |
| Lượt tới khi đang nạp bù native, chạy cả kế hoạch sau (câu hỏi 3) | Unit test, thời gian ảo | `AdPreloaderTest.requestDuringNativeRefill_runsWholePlanAfterIt`: kế hoạch `[{INTERSTITIAL, NATIVE_SMALL}]`, lượt `SDK_READY` xong ở 0; `NATIVE_SMALL.settleAt = null`; `onActivityResumed(false)` ở 100 (nạp bù chạy tới 6 100); `request(PLACEMENT_ENABLED)` ở 1 000: `HOLD`; ở 6 100: `INTERSTITIAL.loadCalls == [0, 6100]`, `NATIVE_SMALL.loadCalls == [0, 100, 6100]`. Code hiện tại thất bại: interstitial chỉ `[0]` |
| Mọi lý do có thể gặp lượt đang chạy, với mọi kiểu lượt (L-072) | Unit test, thời gian ảo | `AdPreloaderTest.wholePlanRequests_duringAnyRun_areHeldAndRunOnceAfter`: lặp 3 lý do (`GATE_REOPENED`, `NETWORK_BACK` qua `onNetworkReturned()`, `PLACEMENT_ENABLED`) nhân 4 kiểu lượt đang chạy (`SDK_READY` đầu, lượt `RUN` sau đó, lượt giữ được chạy, nạp bù native); mỗi trường hợp một `AdPreloader` và khe giả mới, kế hoạch `[{INTERSTITIAL, NATIVE_SMALL}]`, lượt đang chạy không xong; xin ở 1 giây sau khi lượt bắt đầu: khe `INTERSTITIAL` có thêm đúng một lần gọi, ở lúc lượt đang chạy hết (6 giây sau khi bắt đầu), không có lần nào giữa hai thời điểm; thông báo lỗi ghi tên trường hợp. Code hiện tại thất bại ở cả 12 trường hợp |
| Tối đa một lượt giữ | Unit test, thời gian ảo | `AdPreloaderTest.twoRequestsDuringRun_runOnceAfterIt`: lượt `SDK_READY` không xong; `request(PLACEMENT_ENABLED)` ở 1 000, `onNetworkReturned()` ở 2 000, `request(GATE_REOPENED)` ở 3 000: ở 6 000 `loadCalls == [0, 6000]`; lượt đó chạy tới 12 000, ở 30 000 vẫn `[0, 6000]` (không có lượt thứ ba). Code hiện tại thất bại: `[0]` |
| Lượt giữ khi đang chạy, cổng đóng lúc chạy xong | Unit test, thời gian ảo | `AdPreloaderTest.heldDuringRun_gateClosedAtEnd_runsWhenGateOpens`: lượt `SDK_READY` không xong; `request(NETWORK_BACK)` ở 1 000; `gate = mayRequest false` ở 3 000; ở 6 000 không chạy (`[0]`); `gate = OPEN`, `onGateChanged()` ở 8 000: `[0, 8000]`. Code hiện tại thất bại ở 8 000 |
| Lượt giữ quyết lại: quyền "không" thì bỏ (S-23) | Unit test, thời gian ảo | `AdPreloaderTest.heldDuringRun_consentDeniedAtEnd_isDropped`: lượt `SDK_READY` không xong; `request(PLACEMENT_ENABLED)` ở 1 000; `gate = consentDenied true` ở 3 000 (không gọi `onGateChanged`); ở 6 000: không chạy, log `Held preload PLACEMENT_ENABLED: dropped, IGNORE_CONSENT_DENIED`; `gate = OPEN`, `onGateChanged()` ở 8 000: vẫn `[0]`. Phần đếm lần gọi qua ngay trên code hiện tại (lượt bị bỏ từ đầu); đối chứng (L-055) trong cùng test: dòng log, và một preloader thứ hai cùng các bước nhưng không đổi quyền thì gọi ở 6 000; cả hai thất bại trên code hiện tại |
| Lượt giữ quyết lại: SDK chưa khởi tạo thì bỏ | Unit test, thời gian ảo | `AdPreloaderTest.heldDuringRun_sdkNotStartedAtEnd_isDropped`: như trên với `gate = sdkStarted false` ở 3 000: log `Held preload PLACEMENT_ENABLED: dropped, IGNORE_SDK_NOT_STARTED`, `gate = OPEN`, `onGateChanged()` ở 8 000: vẫn `[0]`. Đối chứng như trên (log thất bại trên code hiện tại, vì lượt không được giữ; với code chỉ đổi `decidePreload` mà chưa đổi `onGateChanged` thì lần gọi ở 8 000 làm test thất bại) |
| Nạp bù native khi đang chạy vẫn bỏ (S-25) | Unit test, thời gian ảo | `AdPreloaderTest.nativeRefill_duringRun_isNotHeld`: kế hoạch `[{NATIVE_SMALL}]`, lượt `SDK_READY` xong ở 0, `settleAt = null`; `request(NETWORK_BACK)` ở 1 000 (chạy tới 7 000); `onActivityResumed(false)` ở 2 000: bỏ; ở 20 000 `NATIVE_SMALL.loadCalls == [0, 1000]` (không có lần ở 7 000). Giữ hành vi, qua ngay trên code hiện tại; đối chứng (L-055): `onActivityResumed(false)` ở 21 000 gọi ở 21 000, cho thấy test thấy được một lượt nạp bù |
| Hành vi T-021 còn lại không lùi | Unit test có sẵn | `AdPreloaderTest` (`heldWhileGateClosed_runsOnceWhenOpen`, `consentDenied_dropsHeldRequest`, `noHold_beforeSdkStarted`, `noHold_beforeFirstPreload`, `planSetWhileRunning_appliesNextRun`, `activityResumed_*`, `networkReturn_preloadsAndResetsNativeWait`, `everyRequest_asksConsentAgain`), `PreloadDecisionTest.sdkReady_atCallbackOr35s`, `PreloadPlanRunnerTest` không sửa và vẫn qua |
| Đường gọi trong `AdsController` (L-072) | Hàm dùng chung | Không có ngoại lệ nối: `AdsController.kt` không đổi; mọi đường xin ở bảng mục 2 gọi `AdPreloader.request` hay `onNetworkReturned`, nơi duy nhất gọi `decidePreload` (cùng `onGateChanged`, `onActivityResumed`). Hai hàm dùng chung có test cho từng lý do ở trên. `scripts/check.sh` biên dịch và chạy toàn bộ test |
| Trên máy (staging) | Kiểm tra thủ công, không bắt buộc | Mở app lần đầu sau khi cài (Remote Config về sau SDK sẵn sàng): log `VGL.Ads` nếu có `Preload PLACEMENT_ENABLED: HOLD` thì ngay sau `Preload plan finished` có `Held preload PLACEMENT_ENABLED: run` và một lượt `Preload group 1/1 started`. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'`. Thời điểm phụ thuộc mạng, nên không thấy dòng `HOLD` thì không phải lỗi |

**Ngoại lệ viết test trước:** đoạn tài liệu `docs/ops/ads-guide.md` và KDoc (chỉ chữ). Không có phần code nào ngoài test.

### 4.1. Chạy thử

- **Đã chạy thử:** Không cần (E dưới 2). Cùng kỹ thuật của `AdPreloaderTest` hiện có (thời gian ảo, `FakePreloadSlot`, `PreloadPlanRunner` thật).
- **Lệnh:** `scripts/check.sh` chạy trong phiên ở bước kiểm tra như mọi task.
- **Kết quả:** chưa có.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi lúc gửi yêu cầu nạp sẵn; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì task đổi số lượt nạp sẵn.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai file code và hai file test trong một module, cộng một đoạn tài liệu; không đổi giao diện công khai của lớp nào |
| B. Độ rõ của yêu cầu | 1 | Anh Nhật đã quyết hướng (giữ như `HOLD`, một lượt, quyết lại khi chạy); AI tự quyết hai chi tiết: nạp bù không giữ, lượt giữ sau nạp bù chạy cả kế hoạch (câu hỏi 2, 3) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi đường và mọi kiểu lượt có unit test với thời gian ảo; trên máy chỉ thấy được khi thời điểm trùng (kiểm thủ công không bắt buộc) |
| D. Ảnh hưởng chéo | 2 | Đổi lúc nạp sẵn mà mọi vị trí toàn màn hình dựa vào; T-024, T-027, T-028 (chưa có plan) sẽ dùng `AdPreloader` |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng mẫu `HOLD` và test thời gian ảo của T-021 |
| **Tổng** | 5 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `PreloadDecision.kt`, `AdPreloader.kt` (`:core:ads`), `docs/ops/ads-guide.md` mục 6. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- Task khác có thể xung đột: không plan nào đang mở sửa hai file code này (`git grep` trong `docs/features/*/plans/`, 2026-10-25). T-024 (app open), T-027, T-028 (khe native trong kế hoạch) chưa có plan; plan của chúng đọc hành vi mới ở mục 3.1. `docs/ops/ads-guide.md` mục 6 có thể bị plan khác sửa cùng phiên: khác gạch, merge theo thứ tự chạy.
- Đề xuất thứ tự chạy: độc lập, chạy được ở bất kỳ vị trí nào trong phiên; nên trước T-024 (app open là khe toàn màn hình, hay được bật từ xa giữa lượt chạy đầu).

## 7. Điều kiện dừng riêng

- Cần sửa `AdsController.kt`, `PreloadPlanRunner.kt` hay `AdPreloadPlan.kt` để qua test.
- Test có sẵn của `AdPreloaderTest` hay `PreloadDecisionTest` ngoài hai chỗ sửa ở mục 4 đỏ sau khi đổi code.
- Lượt giữ sau một lượt chạy tạo vòng chạy liên tiếp không có lượt xin mới (một test thấy lượt thứ ba ở `twoRequestsDuringRun_runOnceAfterIt`).

## 8. Câu hỏi cho người duyệt

1. **S-23 có cho phép giữ lượt tới khi đang chạy không?** Chữ của S-23 (`ads-business-rules.md`): "Lượt nạp sẵn bị chặn vì cổng đóng được giữ lại, chạy đúng một lần khi cổng mở; không chạy chồng lên kế hoạch đang chạy."
   - Đề xuất của AI: có. "Không chạy chồng" cấm hai lượt cùng lúc, không bắt bỏ lượt tới sau; giữ rồi chạy sau lượt đang chạy vẫn không chồng. Spec và `ads-business-rules.md` không phải sửa. Nếu anh muốn ghi rõ, đề xuất câu cho S-23 (người sửa, không thuộc task này): "Lượt xin tới khi kế hoạch hay lượt nạp bù đang chạy cũng được giữ (chung chỗ giữ một lượt) và chạy một lần khi lượt đang chạy xong." Nguồn AdsKit (`M/consent/AdsStartupState.kt:159-193`) không có trong repo, AI không đối chiếu được AdsKit giữ hay bỏ lượt này; đổi này lệch AdsKit nếu AdsKit bỏ (ADR-0026 theo PARTNER). Phương án khác: giữ nguyên chữ, không thêm.

2. **Nạp bù native (`NATIVE_REFILL`) tới khi đang chạy có giữ không?**
   - Đề xuất của AI: không giữ, như hôm nay. S-25 đặt "không có kế hoạch đang chạy" làm điều kiện của lượt nạp bù, không phải hàng chờ; lượt cả kế hoạch đang chạy đã gồm khe native theo đúng thứ tự; lần activity hiện lại sau xin lại. Phương án khác: giữ như các lý do khác (thêm một lượt nạp bù sau mỗi lượt chạy trùng lúc resume, lệch chữ S-25).

3. **Lượt giữ sau một lượt nạp bù chỉ native chạy cả kế hoạch hay chỉ phần còn thiếu?**
   - Đề xuất của AI: cả kế hoạch. Lý do được giữ xin khe toàn màn hình mà lượt nạp bù không chạm; khe native vừa nạp không gửi lại (F-04); code không cần nhớ lượt nào đã chạy gì. Phương án khác: chỉ các định dạng không phải native (thêm một hàm lọc kế hoạch và một nhánh, nhóm của native bị bỏ thứ tự).

4. **Sửa hai test có sẵn** (`PreloadDecisionTest.order` dòng `running`, `AdPreloaderTest.noOverlap_whileRunning` dòng log): hai chỗ này kiểm đúng hành vi mà task đổi. `CLAUDE.md` cấm sửa test để qua, trừ khi plan duyệt.
   - Đề xuất của AI: duyệt hai chỗ sửa này cùng plan; không test nào khác được sửa (mục 7).

5. **Duyệt chéo khi nhóm chỉ có một dev:** theo `risk-scoring.md` mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.
   - Đề xuất của AI: như các plan quảng cáo trước (T-021).

**Trả lời của anh Nhật (2026-10-25):** câu 1: có, giữ chữ spec, không sửa S-23; câu 2: không giữ lượt nạp bù native; câu 3: lượt giữ chạy cả kế hoạch; câu 4: duyệt sửa hai test nêu trên; câu 5: anh Nhật vừa duyệt vừa duyệt chéo. Plan chờ rà soát độc lập trước khi duyệt.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
