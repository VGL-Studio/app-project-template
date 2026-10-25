---
ticket: T-038
title: Xóa lượt giữ khi lượt xin nạp sẵn mới được chạy
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 5M           # cỡ S, số gợi ý 5.0M (scripts/token-usage.py estimate, 2026-10-25); một file code, một file test, một đoạn tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-038: XÓA LƯỢT GIỮ KHI LƯỢT XIN NẠP SẴN MỚI ĐƯỢC CHẠY

## 1. Tóm tắt

Rà soát độc lập T-037 (`docs/runs/RUN-20261025-02.md`, mục Cần người kiểm tra, ghi chú 1) thấy: khi đang có lượt nạp sẵn bị giữ vì cổng đóng, cổng mở và một lượt xin mới tới `AdPreloader.request` trước collector cổng thì ra `RUN` nhưng không xóa `held`, nên khi lượt đó xong, `onGateChanged` cuối `launch` chạy cả kế hoạch thêm một lần. Task này thêm `held = null` (kèm một dòng log) ở nhánh `RUN` của `request`. Lỗi đã tái hiện trên code hiện tại bằng test tạm: ba lượt chạy kế hoạch thay vì hai (mục 4). Kiểm chứng bằng unit test với thời gian ảo, cùng cách của `AdPreloaderTest`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed`), luật chi tiết trong `docs/ops/ads-business-rules.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-24 | Khi mạng về sau khi mất thì mọi loại quảng cáo tải lại. | Không đổi: lượt xin vẫn chạy cả kế hoạch. Task chỉ bỏ lượt chạy thừa sau đó; lượt `RUN` đã xin mọi khe sau thời điểm lượt giữ được tạo, nên mọi loại quảng cáo vẫn tải lại. |
| KB-24 (S-23) | Luật trong phạm vi ticket, không có tiêu chí riêng trong spec. | S-23: "Lượt nạp sẵn bị chặn vì cổng đóng được giữ lại, chạy đúng một lần khi cổng mở; không chạy chồng lên kế hoạch đang chạy." Cách hiểu: lượt giữ là một yêu cầu "chạy cả kế hoạch một lần nữa"; một lượt cả kế hoạch bắt đầu sau khi lượt giữ được tạo đã đáp ứng yêu cầu đó, nên lượt giữ coi như đã chạy ("đúng một lần"). Chạy thêm một lượt nữa là thừa. |

Khi nào `request` ra `RUN` mà `held` khác `null` (đã đọc `AdPreloader.kt`, `PreloadDecision.kt`, `AdsController.kt` trên nhánh chính, commit `bf7a671`):

| Lượt giữ được tạo vì | `request` ra `RUN` được không | Đường thật |
| --- | --- | --- |
| Cổng đóng (`!sdkReady` hay `!mayRequest`) | Có: cổng đã mở mà `onGateChanged` chưa chạy | `ConsentCoordinator` gọi `onGateOpened(false)` ngay khi cổng mở, `AdsController.onGateOpened` gọi `request(GATE_REOPENED)` đồng bộ; collector cổng đọc `canRequestAds` (một `stateIn`), nên chạy sau. Cũng có thể: `PLACEMENT_ENABLED` hay `NETWORK_BACK` tới đúng lúc cổng vừa mở (ví dụ app về nền trước, `foregroundState` đổi) |
| Lượt khác đang chạy (T-037) | Không, khi `running` còn true: `decidePreload` ra `HOLD` hay `IGNORE_*`, không bao giờ `RUN` (dòng `running` đứng trước dòng `RUN`, `PreloadDecisionTest.running_holdsEveryWholePlanReason_ignoresNativeRefill`). Lượt chạy xong thì `launch` gọi `onGateChanged()` ngay sau `running = false`, cùng coroutine, không có điểm treo, nên không lượt `request` nào chen vào giữa. Lượt giữ chỉ còn sau đó nếu cổng đóng lúc chạy xong (`HOLD`), và khi đó nó trở về hàng trên | `AdPreloaderTest.heldDuringRun_gateClosedAtEnd_runsWhenGateOpens` (T-037) là tình huống này khi `onGateChanged` tới trước |
| `SDK_READY` | `request(SDK_READY)` chỉ gọi một lần (`AdsController.startSdk`); trước nó mọi lý do khác ra `IGNORE_NEVER_REQUESTED` nên không có lượt giữ | Không gặp |

Lý do có thể ra `RUN` trong `request` khi đang có lượt giữ: `GATE_REOPENED`, `NETWORK_BACK` (qua `onNetworkReturned`), `PLACEMENT_ENABLED` (L-072: mục 4 có test cho cả ba). `NATIVE_REFILL` không đi qua `request` (`onActivityResumed` gọi `launch` riêng, chỉ nhóm native, không đụng `held`) và không thuộc task này (câu hỏi 3).

Thứ tự ngược lại (collector cổng chạy trước: lượt giữ chạy, rồi lượt xin mới tới khi đang chạy, ra `HOLD` theo T-037 và chạy thêm một lượt sau) là hành vi T-037 đã duyệt: lượt xin mới là yêu cầu mới tới sau khi lượt kia bắt đầu. Task không đổi (câu hỏi 2).

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0004, ADR-0019 (log), ADR-0026 (hành vi theo AdsKit PARTNER).

## 3. Giải pháp

### 3.1. Cách làm

**1. Bộ điều phối** (`AdPreloader.kt`, hàm `request`), nhánh `RUN`:

```kotlin
PreloadDecision.RUN -> {
    held?.let { logger.info(TAG, "Held preload $it: covered by $reason run") }
    held = null
    launch(plan, wholePlan = true)
}
```

- Xóa lượt giữ trước `launch`, để `onGateChanged` cuối lượt (và collector cổng tới sau) thấy `held == null` và không làm gì.
- Log mức `info`, thẻ `Ads`, chỉ khi có lượt giữ (câu hỏi 1). Không ghi dữ liệu cá nhân.
- Nhánh `HOLD` và `else` không đổi. `onGateChanged`, `launch`, `onActivityResumed`, `decidePreload` không đổi.
- KDoc của `request`: thêm "A RUN takes the held preload with it (S-23: it ran once)".

**2. Tài liệu** (`docs/ops/ads-guide.md`, mục 6, gạch "Lúc chạy"): thêm câu "Lượt xin mới được chạy khi đang có lượt giữ thì lượt giữ tính là đã chạy cùng lượt đó, không chạy thêm (T-038)." Gạch log thêm `Held preload NETWORK_BACK: covered by GATE_REOPENED run`.

**3. Không đổi:** `AdsController.kt`, `PreloadDecision.kt`, `PreloadPlanRunner.kt`, `AdPreloadPlan.kt`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPreloader.kt` | Nhánh `RUN` của `request` xóa `held`, log; KDoc (mục 3.1 bước 1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPreloaderTest.kt` | Năm test mới (mục 4); không sửa test có sẵn |
| Sửa | `docs/ops/ads-guide.md` | Mục 6, gạch "Lúc chạy" và gạch log (bước 2) |
| Sửa | `docs/features/ads/tickets/T-038-xoa-luot-giu-khi-nap-san-chay.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-038-xoa-luot-giu-khi-nap-san-chay.md` | Nhật ký thực hiện |

Nơi đọc các file bị sửa (L-020, đã `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, `src/androidTest`):

- `AdPreloader.kt`: đọc bởi `AdsController.kt` (chữ ký công khai trong module không đổi) và `AdPreloaderTest.kt` (có trong bảng).
- Chuỗi log mới: không test hay script nào đọc chuỗi log của `AdPreloader` ngoài `AdPreloaderTest.kt`.
- `docs/ops/ads-guide.md`: không script, workflow hay test nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng. Đổi số lượt chạy kế hoạch nạp sẵn (điểm chặn 1): bớt một lượt thừa trong tình huống ở mục 2. Không có lượt nào trước đây chạy mà nay khe thiếu yêu cầu: lượt `RUN` đã xin mọi khe trong kế hoạch.
- Sự kiện phân tích: Không chạm. Nạp sẵn không ghi `AdTelemetry.opportunity` (A-04, HD-06).
- Dữ liệu: không lưu gì trên máy; lượt giữ chỉ trong bộ nhớ.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng `kotlinx-coroutines-test` (`libs.kotlinx.coroutines.test`) đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Thay đổi là hai dòng trong một nhánh `when`, đã chạy thử trên nhánh tạm (mục 4).

## 4. Kiểm thử

Unit test JVM, cùng cách của `AdPreloaderTest` hiện có: `runTest`, `backgroundScope`, `PreloadPlanRunner` thật với `MonotonicClock { testScheduler.currentTime }`, `FakePreloadSlot` (`settleAt = null`: không xong, lượt chạy hết ở trần 6 giây), hàm phụ `at(millis)`, `logged(message)`, cổng giả `gate` (biến, `OPEN` mặc định), `RecordingAppLogger`. Không thêm hàm phụ mới vào file test (L-025 không áp dụng).

Test viết trước chạy trên **code hiện tại của nhánh chính** (chỉ đổi hành vi của một lớp đã có). Thứ tự `request` trước `onGateChanged()` trong test là thứ tự thật của `GATE_REOPENED` (mục 2).

**Số đo thật trên code hiện tại** (L-029; test tạm `TmpT038Test.kt` không commit, `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT038Test*'`, commit `bf7a671`, 2026-10-25). Cả hai thất bại vì lượt thừa ở cuối lượt `RUN`, đúng lỗi rà soát đã nêu; sau đó không còn lượt nào (không vòng lặp, kiểm tới 30 000):

- Giữ vì cổng đóng: khe `INTERSTITIAL` xong ở lượt `SDK_READY` (0), rồi `settleAt = null`; `gate = mayRequest false`, `request(NETWORK_BACK)` ở 1 000 (`HOLD`); ở 2 000 `gate = OPEN`, `request(GATE_REOPENED)` rồi `onGateChanged()`. Kết quả `expected:<[0, 2000]> but was:<[0, 2000, 8000]>`; 3 dòng `Preload plan finished` (đúng ra 2); log: `Preload NETWORK_BACK: HOLD`, `Preload GATE_REOPENED: RUN`, `Preload group 1/1 moved on: timed out`, `Preload plan finished`, `Held preload NETWORK_BACK: run`, thêm một lượt tới 14 000.
- Giữ vì đang chạy, cổng đóng lúc chạy xong: khe không xong; `request(SDK_READY)` ở 0; `request(PLACEMENT_ENABLED)` ở 1 000 (`HOLD`); `gate = mayRequest false` ở 3 000; lượt hết ở 6 000, lượt giữ ở lại; ở 8 000 `gate = OPEN`, `request(GATE_REOPENED)` rồi `onGateChanged()`. Kết quả `expected:<[0, 8000]> but was:<[0, 8000, 14000]>`; 3 dòng `Preload plan finished` (đúng ra 2); có `Held preload PLACEMENT_ENABLED: run` sau lượt `GATE_REOPENED`.
- Chạy thử bản sửa (nhánh tạm, không commit): thêm `held = null` ở nhánh `RUN` thì hai test tạm qua, `AdPreloaderTest` 23 test và `PreloadDecisionTest` 4 test qua (`--tests '*AdPreloaderTest*' --tests '*PreloadDecisionTest*'`). Đã hoàn tác, test tạm đã xóa.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| S-23 "chạy đúng một lần", giữ vì cổng đóng | Unit test, thời gian ảo | `AdPreloaderTest.requestRun_whileHeldForClosedGate_clearsHeld`: các bước của số đo thứ nhất ở trên; kỳ vọng `loadCalls == [0, 2000]` ở 30 000, log `Held preload NETWORK_BACK: covered by GATE_REOPENED run`, không có log `Held preload NETWORK_BACK: run`. Code hiện tại thất bại: `[0, 2000, 8000]` |
| S-23, giữ vì đang chạy rồi cổng đóng lúc xong (T-037) | Unit test, thời gian ảo | `AdPreloaderTest.requestRun_whileHeldFromRunAndGateClosed_clearsHeld`: các bước của số đo thứ hai; kỳ vọng `loadCalls == [0, 8000]` ở 30 000, log `Held preload PLACEMENT_ENABLED: covered by GATE_REOPENED run`. Code hiện tại thất bại: `[0, 8000, 14000]` |
| Mọi lý do có thể ra `RUN` khi đang giữ (L-072) | Unit test, thời gian ảo | `AdPreloaderTest.everyRunReason_whileHeld_clearsHeld`: lặp `GATE_REOPENED`, `NETWORK_BACK` (gọi `onNetworkReturned()`), `PLACEMENT_ENABLED`; mỗi trường hợp một `AdPreloader` và khe mới, `gate = OPEN` đầu mỗi vòng; lượt giữ là `NETWORK_BACK` (hay `PLACEMENT_ENABLED` khi lý do thử là `NETWORK_BACK`) tạo khi cổng đóng; mở cổng, gọi lý do thử trước `onGateChanged()`: khe có đúng một lần gọi thêm, ở thời điểm xin, không lần nào tới 20 giây sau; thông báo lỗi ghi tên lý do. Code hiện tại thất bại ở cả ba |
| Lượt giữ không bị xóa khi chỉ cổng mở (đối chứng, L-055) | Unit test, thời gian ảo | `AdPreloaderTest.gateOpens_withoutRequest_heldRunsOnce`: như test thứ nhất nhưng ở 2 000 chỉ `gate = OPEN`, `onGateChanged()` (không `request`): `loadCalls == [0, 2000]`, log `Held preload NETWORK_BACK: run`, không có log `covered by`. Qua ngay trên code hiện tại; là đối chứng cho test thứ nhất: cùng số lần gọi cuối, khác đường, cho thấy test thứ nhất phân biệt được lượt giữ chạy qua `onGateChanged` với lượt giữ bị xóa |
| Sau khi xóa, lượt xin tới khi đang chạy vẫn được giữ (T-037 không lùi) | Unit test, thời gian ảo | `AdPreloaderTest.requestRun_clearsHeld_thenRequestDuringRunIsHeld`: như test thứ nhất tới 2 000; `request(PLACEMENT_ENABLED)` ở 3 000 (`HOLD`, log `Preload PLACEMENT_ENABLED: HOLD`); ở 8 000 `loadCalls == [0, 2000, 8000]`, log `Held preload PLACEMENT_ENABLED: run`; ở 30 000 vẫn ba lần. Phần đếm lần gọi qua ngay trên code hiện tại (lượt thừa trùng thời điểm); dòng log `covered by` ở 2 000 thất bại trên code hiện tại. Xác nhận `held = null` ở nhánh `RUN` không chặn lượt giữ mới sau đó |
| `RUN` không xảy ra khi đang chạy (nên lượt giữ vì đang chạy chỉ bị xóa qua hàng trên) | Unit test có sẵn | `PreloadDecisionTest.running_holdsEveryWholePlanReason_ignoresNativeRefill`, `AdPreloaderTest.wholePlanRequests_duringAnyRun_areHeldAndRunOnceAfter`, `twoRequestsDuringRun_runOnceAfterIt` không sửa và vẫn qua |
| Hành vi T-021, T-037 không lùi | Unit test có sẵn | Toàn bộ `AdPreloaderTest` (23 test), `PreloadDecisionTest`, `PreloadPlanRunnerTest` không sửa và vẫn qua; `scripts/check.sh` |
| Trên máy (staging) | Kiểm tra thủ công (máy thật: khác, cần đổi quyền đồng ý UMP qua form quyền riêng tư trong lúc lượt giữ đang chờ; thời điểm không lặp lại được trên emulator), không bắt buộc | Mở form quyền riêng tư, từ chối rồi đồng ý lại khi mạng vừa về: log `VGL.Ads` nếu có `Held preload <lý do>: covered by GATE_REOPENED run` thì sau `Preload plan finished` kế tiếp không có `Held preload <lý do>: run`. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'` |

**Ngoại lệ viết test trước:** đoạn tài liệu `docs/ops/ads-guide.md` và KDoc (chỉ chữ). Không có phần code nào ngoài test.

### 4.1. Chạy thử

- **Đã chạy thử:** Không cần (E dưới 2). Cùng kỹ thuật của `AdPreloaderTest` hiện có; bản sửa đã chạy thử trên nhánh tạm (mục 4).
- **Lệnh:** `scripts/check.sh` chạy trong phiên ở bước kiểm tra như mọi task.
- **Kết quả:** chưa có.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi số lượt gửi yêu cầu nạp sẵn; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước (task chỉ bớt một lượt nạp sẵn thừa).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Một nhánh `when` trong một file code, cộng test và một câu tài liệu; không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 1 | Rà soát đã nêu cách sửa; AI tự quyết có log hay không và cách hiểu "đúng một lần" (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tình huống có unit test với thời gian ảo; lỗi đã tái hiện bằng số đo |
| D. Ảnh hưởng chéo | 1 | Đổi lúc nạp sẵn của mọi vị trí toàn màn hình, nhưng chỉ bớt lượt thừa; không plan đang mở nào sửa `AdPreloader.kt` |
| E. Độ mới của kỹ thuật | 0 | Lặp lại mẫu `held = null` có sẵn trong `onGateChanged` |
| **Tổng** | 2 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPreloader.kt` (`:core:ads`), `docs/ops/ads-guide.md` mục 6. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- Task khác có thể xung đột: không plan nào đang mở sửa `AdPreloader.kt` (`git grep AdPreloader docs/features/*/plans/`, 2026-10-25: chỉ T-021, T-037, đã `done`). T-024, T-027, T-028 chưa có plan; khi có, có thể sửa `AdPreloader.kt` hay `ads-guide.md` mục 6: khác dòng, merge theo thứ tự chạy.
- Đề xuất thứ tự chạy: độc lập, chạy được ở bất kỳ vị trí nào trong phiên; phụ thuộc T-037 (đã merge).

## 7. Điều kiện dừng riêng

- Cần sửa `AdsController.kt`, `PreloadDecision.kt`, `onGateChanged` hay `launch` để qua test.
- Test có sẵn của `AdPreloaderTest` hay `PreloadDecisionTest` đỏ sau khi đổi code.
- Test viết trước thứ nhất hay thứ hai qua ngay trên code hiện tại (số đo ở mục 4 không còn đúng: code đã đổi từ commit `bf7a671`).

## 8. Câu hỏi cho người duyệt

1. **Có ghi log khi lượt giữ bị xóa không?**
   - Đề xuất của AI: có, một dòng `info` `Held preload <lý do giữ>: covered by <lý do mới> run`, chỉ khi có lượt giữ. Mọi đổi trạng thái khác của lượt giữ đã có log (`HOLD`, `Held preload ...: run`, `... dropped, ...`), thiếu dòng này thì đọc log trên máy thấy lượt giữ biến mất không rõ lý do (ADR-0019). Phương án khác: không log (ít dòng hơn, nhưng mất dấu khi phân tích phiên thử).

2. **"Chạy đúng một lần" của S-23: lượt `RUN` mới có tính là lượt giữ đã chạy không?**
   - Đề xuất của AI: có, khi lượt `RUN` bắt đầu sau khi lượt giữ được tạo (luôn đúng ở nhánh `RUN` của `request`). Lượt giữ chỉ cần "cả kế hoạch chạy một lần nữa", không mang dữ liệu riêng; lý do giữ chỉ ảnh hưởng `IGNORE_NEVER_REQUESTED`, mà `everRequested` đã true. Không sửa spec. Thứ tự ngược lại (lượt giữ chạy trước qua `onGateChanged`, lượt xin mới tới khi đang chạy, ra `HOLD` và chạy thêm một lượt) giữ như T-037 đã duyệt: lượt xin đó tới sau khi lượt kia bắt đầu, có thể vì khe mới bật hay mạng mới về. Phương án khác: gộp cả thứ tự ngược (lượt xin trong vòng vài trăm ms sau khi lượt giữ chạy thì bỏ), cần đồng hồ và lệch T-037; không đề xuất.

3. **Lượt nạp bù native (`onActivityResumed`) có xóa lượt giữ không?**
   - Đề xuất của AI: không. Nạp bù chỉ chạy nhóm native, không xin khe toàn màn hình mà lượt giữ cần; lượt giữ vẫn chạy cả kế hoạch sau nó (T-037, câu hỏi 3). Code hiện tại đã đúng như vậy, không đổi.

4. **Duyệt chéo khi nhóm chỉ có một dev:** theo `risk-scoring.md` mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.
   - Đề xuất của AI: như T-021, T-037.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
