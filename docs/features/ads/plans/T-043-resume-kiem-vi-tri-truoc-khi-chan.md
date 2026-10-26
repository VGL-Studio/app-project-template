---
ticket: T-043
title: Quảng cáo khi quay lại app: kiểm vị trí tắt trước cổng chặn (L-A05)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 4.1M         # cỡ S, số gợi ý 4.1M (scripts/token-usage.py estimate, 2026-10-26); đổi thứ tự hai khối trong một hàm, hai test mới, sửa một test, một câu tài liệu
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-043: QUẢNG CÁO KHI QUAY LẠI APP: KIỂM VỊ TRÍ TẮT TRƯỚC CỔNG CHẶN (L-A05)

## 1. Tóm tắt

Rà soát độc lập T-025 (RUN-20261026-04, plan T-025 mục 10) thấy: ở bước hiện, `ResumeAdTurns.onAppActivityResumed` kiểm `suppressedNow()` trước `placementNow()`, nên khi khóa resume vừa tắt (hay đổi loại) giữa lúc quyết và lúc hiện mà app đang bị chặn (form UMP vừa mở) thì vẫn ghi một cơ hội `suppressed`, lệch L-A05 ("resume tắt không phải cơ hội"). Task đổi thứ tự hai khối kiểm, theo lựa chọn của anh Nhật (2026-10-26), và sửa test `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing` để test thật sự tới nhánh `OFF`. Lỗi đã tái hiện trên code hiện tại bằng test tạm (mục 4); bản sửa đã chạy thử với toàn bộ test JVM: 1368 test, 0 lỗi.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed`), luật chi tiết trong `docs/ops/ads-business-rules.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-14 | Khi người dùng quay lại từ màn chọn ảnh, camera, chia sẻ hay hộp thoại đồng ý mà app tự mở thì không có quảng cáo. | Không đổi: lượt bị chặn ở bước hiện vẫn không hiện gì. Task chỉ đổi lượt đó có ghi `ad_opportunity` hay không. |
| KB-14 (mục 8.2, L-A05) | Luật trong phạm vi ticket, không có tiêu chí riêng trong spec. | Mục 8.2: "resume bị chặn (vị trí `resume`; lần đầu của process và resume tắt **không** phải cơ hội)". L-A05: "Resume tắt không phải cơ hội". Cách hiểu: ngay trước khi hiện, nếu vị trí đọc lại (`placementNow()`) khác vị trí đã quyết lúc lên foreground (tắt: `null`; đổi loại: `resume` thành `resume_inter` hay ngược lại) thì lượt bỏ, không ghi gì, kể cả khi đang bị chặn. Chỉ khi vị trí vẫn đúng mà đang bị chặn thì ghi `suppressed` đúng một lần. |
| KB-14 (A-11) | Thứ tự cổng lúc lên foreground: lần đầu, bị chặn, tắt, ... | Không đổi. Ở bước quyết (`decideResumeAd`), bị chặn đứng trước tắt, nhưng `ResumeAdTurns.onProcessForegroundChanged` đã không ghi gì khi `placement == null` (test có sẵn `ResumeAdTurnsTest.suppressedWhileOff_recordsNothing`). Task chỉ đổi bước hiện, nơi hai lần đọc lại là kiểm tra lần hai (L-061), không phải cổng của A-11. |

Mọi đường tới lượt ghi cơ hội `suppressed` của resume (L-072), đã đọc `ResumeAdTurns.kt`, `ResumeAdRules.kt`, `AdsController.kt` trên commit `6077815`:

| Đường | Có ghi khi resume tắt không (trước task) | Sau task |
| --- | --- | --- |
| Lúc lên foreground, `decideResumeAd` ra `SUPPRESSED` | Không: `if (placement == null) return` trước khi ghi | Không đổi |
| Lúc Activity của app resume, `suppressedNow()` true | **Có**: kiểm chặn trước kiểm vị trí | Không: kiểm vị trí trước |
| Lúc Activity của app resume, vị trí đổi loại mà đang bị chặn | **Có**: ghi `suppressed` cho vị trí cũ | Không: bỏ lượt, log `Resume ad dropped: switched` |

Thiết kế tham chiếu: Không có thiết kế riêng (task không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0004, ADR-0019 (log), ADR-0026 (hành vi theo AdsKit PARTNER), ADR-0029 (lệch phạm vi).

## 3. Giải pháp

### 3.1. Cách làm

**1. Bước hiện** (`ResumeAdTurns.onAppActivityResumed`): đổi chỗ hai khối, khối kiểm vị trí lên trước:

```kotlin
fun onAppActivityResumed(activity: Activity) {
    val placement = pending ?: return
    pending = null
    // A placement switched off or changed since the decision is no opportunity, even while blocked (L-A05).
    if (port.placementNow() != placement) {
        logger.info(TAG, "Resume ad dropped: switched")
        return
    }
    if (port.suppressedNow()) {
        logger.info(TAG, "Resume ad ${ResumeDecision.SUPPRESSED} on resume: ${placement.key}")
        val result = ResumeDecision.SUPPRESSED.opportunityResult() ?: return
        port.recordOpportunity(placement, result, port.loadStateOf(placement))
        return
    }
    ...
}
```

- Nội dung hai khối và hai dòng log giữ nguyên (câu hỏi 1). Không đổi `onProcessForegroundChanged`, `decideResumeAd`, `opportunityResult`, `ResumeAdPort`, phần nối trong `AdsController.kt`.
- Lúc hiện không đổi: cả hai nhánh trước và sau task đều không hiện quảng cáo; chỉ bớt một `ad_opportunity` ở trường hợp tắt hay đổi loại trong lúc bị chặn.

**2. Sửa test `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing`** (điểm nên sửa của rà soát T-025). Hai lý do test cũ qua cả khi nhánh `OFF` hỏng, cả hai đã thấy khi chạy thử (mục 4):

- Bộ điều khiển chưa mở cổng đồng ý và lượt quảng cáo lúc mở app chưa xong, nên lượt quay lại ra `STARTUP` (rồi `NOT_SERVING`), không tới `OFF`.
- `is_log_ad_fullscreen_events` mặc định tắt, nên `AdTelemetry.opportunity` không ghi gì dù có cơ hội; dòng `metrics.events.none { it is AdMetricEvent.Opportunity }` luôn đúng.

Test mới (giữ tên): bật `AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS`; `startedController()`; `controller.openConsentGateForTest()` (hàm có sẵn, `AdsControllerNativeComboTest` dùng); `idleFor(45_100)` cho hết trần 45 giây của lượt mở app (cách của `readResumeGate_mapsControllerState`); khẳng định cổng đọc được `serving == true`, `startupFinished == true`, `placement == null`; lên foreground, xuống nền, lên lại; khẳng định log resume đúng bằng `["Resume ad FIRST_FOREGROUND: none", "Resume ad OFF: none"]` và không có `Opportunity`. Đối chứng (L-055) trong cùng test: bật `SHOW_RESUM_ADS`, xuống nền rồi lên lại thì có đúng một `Opportunity(APP_OPEN, resume, not_ready, idle)`; giữ phần đối chứng cũ (`RESUME_AD_TYPE = inter` thì cổng đọc `RESUME_INTER`). Hàm phụ `startedController()`, `idleFor()` đã có trong file, không thêm hàm phụ (L-025 không áp dụng).

**3. Tài liệu** (`docs/ops/ads-guide.md`, gạch "Quảng cáo khi quay lại app không có khoảng chờ" ở mục quy tắc, dòng 138): thêm câu "Giữa lúc quyết và lúc hiện, khóa resume bị tắt (`show_resum_ads`) hay loại resume bị đổi (`resume_ad_type`, `resume` thành `resume_inter` hay ngược lại) thì lượt bỏ và không ghi cơ hội nào, kể cả khi đang bị chặn; cả hai trường hợp đều không phải cơ hội (L-A05 mở rộng cho đổi loại, T-043)."

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdTurns.kt` | Đổi thứ tự hai khối kiểm ở `onAppActivityResumed`, thêm một dòng chú thích (mục 3.1 bước 1) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdTurnsTest.kt` | Hai test mới (mục 4); không sửa test có sẵn |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Sửa `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` (mục 3.1 bước 2) |
| Sửa | `docs/ops/ads-guide.md` | Một câu ở gạch quy tắc của resume (mục 3.1 bước 3) |
| Sửa | `docs/features/ads/tickets/T-043-resume-kiem-vi-tri-truoc-khi-chan.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-043-resume-kiem-vi-tri-truoc-khi-chan.md` | Nhật ký thực hiện |

Test đỏ khi áp bản sửa của mục 3.1 bước 1 lên code hiện tại (chạy thử ở mục 4.1, `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue`): **không có**. Không test có sẵn nào cần sửa dữ liệu (ADR-0029).

Nơi đọc các file bị sửa (L-020, `git grep` trong `scripts/`, `.github/`, `build-logic/`, mọi `src/test`, `src/androidTest`, `src/deviceScenarios`, `docs/ops/`):

- `ResumeAdTurns.kt`: đọc bởi `AdsController.kt` (không đổi chữ ký), `ResumeAdTurnsTest.kt` (có trong bảng), `ResumeAdLifecycleTest.kt` (đi qua `onAppActivityResumed` với `FakeResumeAdPort(liveSuppressed = ...)`, `placementNow` luôn đúng vị trí đã quyết, nên thứ tự mới không đổi kết quả; đã chạy, qua).
- Hai dòng log `Resume ad dropped: switched`, `Resume ad SUPPRESSED on resume: ...`: chỉ `ResumeAdTurnsTest.kt` và `docs/ops/ads-guide.md` dòng 217 (danh sách log, không đổi chữ). Không script hay kịch bản thiết bị nào đọc.
- `docs/ops/ads-guide.md`: không script, workflow hay test nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi lúc hiện. Đổi số liệu `ad_opportunity` của `resume`, `resume_inter` (điểm chặn 1): bớt lượt `suppressed` khi vị trí đã tắt hay đổi loại.
- Sự kiện phân tích: không thêm sự kiện; `ad_opportunity` (mục 8.2) ghi ít hơn ở một trường hợp, theo L-A05.
- Dữ liệu: không lưu gì trên máy.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng Robolectric, `RecordingAdMetrics`, `RecordingAppLogger`, `FakeRemoteSettings` đã có trong test của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Bản sửa và cả ba test đã chạy thử trên code hiện tại (mục 4, 4.1).

## 4. Kiểm thử

Unit test JVM chạy bằng Robolectric, cùng cách của `ResumeAdTurnsTest` (`FakeResumeAdPort`, `ALL_PASS`, hàm phụ `launched()`, `returnToApp()`) và `AdsControllerResumeTest` (bộ điều khiển thật, `FakeRemoteSettings`, `RecordingAdMetrics`, `RecordingAppLogger`, `idle()`, `idleFor()`). Không thêm hàm phụ.

**Số đo thật trên code hiện tại** (L-029; test tạm `TmpT043Test.kt` không commit, `./gradlew :core:ads:testDebugUnitTest --tests '*TmpT043Test*'`, commit `6077815`, 2026-10-26):

- Tắt giữa lượt khi bị chặn (`launched()`, `returnToApp()`, rồi `suppressedNow = true`, `placementNow = null`, `onAppActivityResumed`): ghi `[ResumeOpportunity(placement=RESUME, result=suppressed, loadState=idle)]`; log `Resume ad FIRST_FOREGROUND: resume`, `Resume ad SHOW: resume`, `Resume ad SUPPRESSED on resume: resume`. Thất bại đúng lỗi rà soát nêu.
- Đổi loại giữa lượt khi bị chặn (`placementNow = RESUME_INTER`): ghi đúng cùng cơ hội `suppressed` của `RESUME`. Thất bại.
- Đang bật mà bị chặn, hai lần resume: một cơ hội `suppressed`, 0 lần hiện. Qua (đúng như test có sẵn `suppressedAtResume_recordsSuppressedOnce_showsNothing`).
- Bộ điều khiển thật, bản test mới của mục 3.1 bước 2: cổng đọc được `ResumeGate(firstForeground=false, startupFinished=true, serving=true, suppressed=false, placement=null, busy=false, appOpenLockPassed=true, ready=false)`; log `[Resume ad FIRST_FOREGROUND: none, Resume ad OFF: none]`; đối chứng ghi `Opportunity(adFormat=APP_OPEN, adPlacement=resume, result=not_ready, loadState=idle)`. Lần chạy đầu chưa bật `IS_LOG_AD_FULLSCREEN_EVENTS` thì đối chứng ra 0 cơ hội: đó là lý do thứ hai test cũ không bắt được gì.
- Đột biến kiểm sức bắt của test (đổi tạm `gate.placement == null -> ResumeDecision.OFF` thành `NOT_SERVING` trong `ResumeAdRules.kt`): test mới đỏ (`expected:<[... OFF: none]> but was:<[... NOT_SERVING: none]>`), test cũ vẫn qua. Đột biến thứ hai (cổng luôn đọc `placement = RESUME`): cả hai đỏ ở dòng `assertNull`. Đã hoàn tác.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| L-A05, mục 8.2: vị trí tắt giữa lượt, đang bị chặn, không ghi cơ hội | Unit test (Robolectric) | `ResumeAdTurnsTest.switchedOffWhileSuppressed_atResume_recordsNothing`: `launched()`, `returnToApp()`; `port.suppressedNow = true`, `port.placementNow = null`; `onAppActivityResumed`: `opportunities` rỗng, `shows == 0`, có log `Resume ad dropped: switched`, không có log `Resume ad SUPPRESSED on resume: resume`. Đối chứng (L-055): `placementNow = RESUME`, `returnToApp()`, resume (vẫn bị chặn) thì `opportunities == [ResumeOpportunity(RESUME, "suppressed", "idle")]`. Code hiện tại thất bại: ghi một `suppressed` ở bước đầu (số đo trên) |
| L-A05: vị trí đổi loại giữa lượt, đang bị chặn, không ghi cơ hội | Unit test (Robolectric) | `ResumeAdTurnsTest.typeChangedWhileSuppressed_atResume_recordsNothing`: như trên với `placementNow = RESUME_INTER`: `opportunities` rỗng, `shows == 0`, log `Resume ad dropped: switched`. Đối chứng: `gate = ALL_PASS.copy(placement = RESUME_INTER)`, `returnToApp()`, resume (vẫn bị chặn) thì `opportunities == [ResumeOpportunity(RESUME_INTER, "suppressed", "idle")]`. Code hiện tại thất bại |
| KB-14 tiêu chí 3: vị trí đang bật mà bị chặn ở bước hiện ghi `suppressed` đúng một lần, không hiện | Unit test có sẵn | `ResumeAdTurnsTest.suppressedAtResume_recordsSuppressedOnce_showsNothing` không sửa và vẫn qua (đã chạy với bản sửa); đối chứng của hai test trên cũng kiểm điều này |
| L-A05 lúc lên foreground (bị chặn mà tắt) không đổi | Unit test có sẵn | `ResumeAdTurnsTest.suppressedWhileOff_recordsNothing`, `off_recordsNothing`, `switchedOffBeforeResume_showsNothing_recordsNothing`, `typeChangedBeforeResume_showsNothing` không sửa và vẫn qua |
| Nhánh `OFF` của bộ điều khiển thật không ghi cơ hội (điểm nên sửa của rà soát T-025) | Unit test (Robolectric) | `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing`, bản sửa ở mục 3.1 bước 2. Test sửa, không "thất bại trước" được (code đúng); sức bắt đã kiểm bằng đột biến ở trên |
| KB-14, hành vi T-025 không lùi | Unit test có sẵn | Toàn bộ `ResumeAdTurnsTest`, `ResumeAdLifecycleTest`, `ResumeAdRulesTest`, `ResumeSuppressionTest`, `AdsControllerResumeTest` (trừ test sửa) không sửa và vẫn qua; `scripts/check.sh` |
| Trên máy (staging) | Kiểm tra thủ công (máy thật: Remote Config; cần tắt `show_resum_ads` trên Firebase staging đúng lúc form đồng ý đang mở giữa lúc quay lại và lúc Activity hiện lại, không lặp lại được theo thời gian trên emulator), không bắt buộc | Lệnh `adb logcat -v threadtime \| grep ' VGL\.Ads'`: sau `Resume ad SHOW: resume`, nếu khóa đã tắt thì có `Resume ad dropped: switched`, không có `Resume ad SUPPRESSED on resume` |

**Ngoại lệ viết test trước:** (1) bản sửa `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` là sửa test cho code đã đúng, không thất bại trước được; sức bắt đã kiểm bằng đột biến lúc lập plan (mục 4). (2) Câu tài liệu trong `ads-guide.md` và dòng chú thích code (chỉ chữ). (3) Phần nối `placementNow()`, `suppressedNow()` trong `AdsController.kt` không đổi; đường qua bộ điều khiển thật tới `onAppActivityResumed` cần một quảng cáo đã nạp (`ready`), Robolectric không có, nên chỉ kiểm bằng `FakeResumeAdPort` như T-025.

### 4.1. Chạy thử

- **Đã chạy thử:** Không bắt buộc (E dưới 2). Đã chạy theo brief của lượt soạn plan: áp bản sửa của mục 3.1 bước 1 cùng test tạm `TmpT043Test.kt` (bốn test của mục 4) lên commit `6077815`, 2026-10-26; rồi hoàn tác, không commit.
- **Lệnh:** `./gradlew testDebugUnitTest testStagingDebugUnitTest --continue` (`JAVA_HOME` của Android Studio). `scripts/check.sh` chạy trong phiên ở bước kiểm tra như mọi task.
- **Kết quả:** `BUILD SUCCESSFUL`; 1368 test JVM (gồm bốn test tạm), 0 lỗi; không test có sẵn nào đỏ. Bốn test tạm qua với bản sửa, hai test tắt và đổi loại đã đỏ trên code chưa sửa (mục 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi số liệu `ad_opportunity` của resume; điểm 1), module core dùng chung `:core:ads` (điểm 7). Không thêm quyền, SDK, vị trí, sự kiện hay dữ liệu lưu trên máy; không đổi lúc hiện quảng cáo. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Đổi chỗ hai khối trong một hàm, hai test mới, sửa một test, một câu tài liệu |
| B. Độ rõ của yêu cầu | 0 | Ticket nêu cách sửa, anh Nhật đã chọn đổi thứ tự (2026-10-26); L-A05 và mục 8.2 rõ |
| C. Khả năng kiểm chứng tự động | 0 | Mọi trường hợp có unit test; lỗi đã tái hiện bằng số đo |
| D. Ảnh hưởng chéo | 1 | Đổi số liệu cơ hội của hai vị trí toàn app; không plan đang mở nào sửa `ResumeAdTurns.kt` |
| E. Độ mới của kỹ thuật | 0 | Cùng kỹ thuật test của T-025 |
| **Tổng** | 1 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `ResumeAdTurns.kt` (`:core:ads`), `docs/ops/ads-guide.md`. Ảnh chuẩn screenshot và `tokens.json` không bị ảnh hưởng (không đổi giao diện).
- `docs/STATUS.md`: không đưa vào PR (`docs/CLAUDE.md`, mục Trang tiến độ).
- Task khác có thể xung đột: ba plan soạn song song T-041 (native toàn trang, `NativeFlowStep`), T-042 (rewarded bấm đúp, `RewardedTurns`), T-044 (bổ sung test T-026, T-027, T-036) không chạm `ResumeAdTurns.kt`, `ResumeAdTurnsTest.kt`, `AdsControllerResumeTest.kt` theo phạm vi ticket (T-044 ghi rõ không gồm test của resume). Có thể cùng sửa `docs/ops/ads-guide.md`: khác đoạn; T-043 chạy trước nên các plan sau cập nhật nhánh theo `scripts/merge-if-green.sh --update`.
- Đề xuất thứ tự chạy: chạy đầu tiên (thứ tự T-043, T-042, T-044, T-041); phụ thuộc T-025 (đã merge).

## 7. Điều kiện dừng riêng

- Cần sửa `AdsController.kt`, `ResumeAdRules.kt` hay `ResumeAdPort` để qua test.
- Bản sửa của `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing` (mục 3.1 bước 2) thất bại trước khi đổi `ResumeAdTurns.kt`: nhánh `OFF` thật của bộ điều khiển sai, ngoài phạm vi task.
- Hai test viết trước của `ResumeAdTurnsTest` qua ngay trên code hiện tại (số đo ở mục 4 không còn đúng: code đã đổi từ commit `6077815`).
- Đổi thứ tự làm đổi lúc hiện quảng cáo (một test có sẵn đếm `shows` đỏ).

## 8. Câu hỏi cho người duyệt

1. **Log khi vị trí đổi giữa lượt mà đang bị chặn:** giữ dòng có sẵn `Resume ad dropped: switched` hay thêm dòng riêng nói rõ lượt cũng đang bị chặn?
   - Đề xuất của AI: giữ dòng có sẵn, không đọc `suppressedNow()` khi vị trí đã đổi. Lượt đó không còn là cơ hội nên trạng thái chặn không cần ghi; `ads-guide.md` dòng 217 đã liệt kê dòng log này nên không phải đổi tài liệu log.

2. **Có thêm một câu vào `docs/ops/ads-guide.md` không?**
   - Đề xuất của AI: có, một câu ở gạch quy tắc resume (mục 3.1 bước 3), để người đọc số liệu biết `suppressed` của resume chỉ đếm lượt của vị trí đang bật. Phương án khác: không đổi tài liệu, vì L-A05 đã nằm ở `ads-business-rules.md`.

3. **Duyệt chéo khi nhóm chỉ có một dev:** theo `risk-scoring.md` mục 3, anh Nhật vừa duyệt vừa duyệt chéo, ghi lý do ở mục 9.
   - Đề xuất của AI: như T-025, T-038.

**Trả lời của anh Nhật (2026-10-26):** câu 1 đến 3: theo đề xuất của AI (giữ log `Resume ad dropped: switched`; thêm câu vào `ads-guide.md`, nói rõ cả tắt lẫn đổi loại; anh Nhật vừa duyệt vừa duyệt chéo).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
