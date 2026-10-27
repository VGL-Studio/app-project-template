---
ticket: T-046
title: Dọn quảng cáo theo ghi chú rà soát RUN-20261026-05 và RUN-20261026-06
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 4.6M         # cỡ S, số gợi ý 4.6M (scripts/token-usage.py estimate, 2026-10-27); việc nhỏ, không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-27
---

# PLAN T-046: DỌN QUẢNG CÁO THEO GHI CHÚ RÀ SOÁT RUN-20261026-05 VÀ RUN-20261026-06

## 1. Tóm tắt

Gom các điểm mức ghi chú mà rà soát độc lập của T-041 đến T-045 nêu nhưng phiên không sửa. Bỏ hai tham số không còn nơi gọi (`safeArea` của `NativeFullScreenFrame`, `growWithText` của `NativeFullScreenAdHeader`), sửa KDoc, sửa một ô của bảng kích thước trong `ads-guide.md`. Làm chặt hai đối chứng test: đối chứng của `RewardedTurnsTest.doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy` hôm nay đi qua nhánh R-06 chứ không qua nhánh nhập vào (đã đo, mục 4.1), và đối chứng của `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing` kiểm thêm `adFormat`. Ô AdChoices giả có chữ "i" bị cắt ở cỡ chữ 2.0 chỉ có trong test, plan giữ nguyên và ghi lý do. Người dùng không thấy gì đổi; mọi ảnh chuẩn giữ nguyên từng byte.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed`). Ticket không thêm tiêu chí mới: mỗi phần giữ nguyên hành vi mà tiêu chí đã có, chỉ dọn code, chú thích, tài liệu và làm chặt test. Nguồn: báo cáo `RUN-20261026-05` (mục Cần người kiểm tra, dòng "Điểm rà soát mức ghi chú") và `RUN-20261026-06` (cùng mục).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2 | Vùng ảnh của native ít nhất 120 x 120 dp, đúng tỷ lệ | Bảng kích thước của `ads-guide.md`, cột `FULL`: tài liệu ghi đúng điều code làm (mô tả đúng 2 dòng, cao ít nhất 40 dp). Không đổi code |
| KB-5 (thay bằng KB-16) | Native toàn màn hình sau inter | `NativeFullScreenFrame` luôn đệm vùng an toàn của cửa sổ (mọi nơi gọi hôm nay đều vậy); đầu quảng cáo luôn cao ít nhất 48 dp, cao thêm theo chữ (mọi nơi gọi hôm nay đều vậy). Bỏ tham số, không đổi bố cục |
| KB-14, tiêu chí 1 | Bật quảng cáo khi quay lại thì mỗi lần quay lại có quảng cáo mở app | Đối chứng của test "khóa tắt thì không ghi gì" kiểm thêm cơ hội được ghi thuộc định dạng `APP_OPEN`, không chỉ đúng mã vị trí |
| KB-19, tiêu chí 6 | Bấm "Xem quảng cáo" lần hai trước khi quảng cáo lên màn thì ghi `busy` | Đối chứng của test bấm đúp khi hiện lỗi ngay trong lời gọi phải đi qua nhánh "lần bấm nhập vào lần quyết định đang chạy" (T-042), như tên test nêu; chú thích về hàng cổng của `doubleTapAwaitingSwitch` nói đúng cổng nào được lấy |
| KB-20, tiêu chí 1, 3 | Native toàn trang ở bước của luồng, không nút điều khiển | Trang của bước dùng khung riêng (`FlowStepPageFrame`, T-041), không dùng `NativeFullScreenFrame`; KDoc thôi nhắc trang của bước ở khung đó. Đầu quảng cáo của trang bỏ đối số `growWithText = true`, hành vi giữ nguyên |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0013 (quảng cáo chỉ qua `:core:ads`), ADR-0026 (nghiệp vụ AdsKit), ADR-0029 (phạm vi), ADR-0031 (hành vi đã có bị đổi).

## 3. Giải pháp

### 3.1. Cách làm

Năm phần, mỗi phần nhỏ và độc lập.

**A. Bỏ `safeArea` của `NativeFullScreenFrame` (T-041).** `NativeFullScreenAdScreen.kt`: hai nơi gọi (`NativeFullScreenAdLayout` nhánh sau inter, `NativeFullScreenOverlay.kt` khung chờ) đều dùng giá trị mặc định `true`. Bỏ tham số, giữ `.windowInsetsPadding(WindowInsets.safeDrawing)` không điều kiện (bỏ `.then(if (safeArea) … else Modifier)`). KDoc mới: "The page of the native full-screen ad after an interstitial and of its placeholder: background, the safe area of the window, margins, 16 dp gaps. The page of a flow step has its own frame ([FlowStepPageFrame], plan T-041)." `NativeFullScreenOverlay.kt` không phải sửa (gọi theo vị trí, chỉ truyền `modifier` và `content`).

**B. Bỏ `growWithText` của `NativeFullScreenAdHeader` (T-045).** Hai nơi gọi (`NativeFullScreenAdLayout` nhánh sau inter, `FlowStepPageLayout` trong `NativeFlowStepLayout.kt`) đều truyền `true`. Bỏ tham số, giữ `Modifier.heightIn(min = NativeFullScreenAdDimens.HeaderHeight)` không điều kiện; bỏ nhánh `height(...)` cố định. Hai nơi gọi bỏ đối số `growWithText = true`; bỏ dòng chú thích `// The head grows with the text…` ở `NativeFullScreenAdScreen.kt:123` (nhánh sau inter), vì KDoc mới của hàm đã nói ý đó. KDoc mới: "Icon, headline, then the "Ad" label with the advertiser: 48 dp at least, taller with the text, so the headline and the "Ad" label are never cut when the system text is large (O-08; plans T-041, T-045)."

**C. `docs/ops/ads-guide.md`, bảng kích thước (T-044).** Ô "Khung" của cột "Đầy đủ (`FULL`)": "cao 418 dp như khung chờ (mô tả ít nhất 2 dòng), như small" thành "cao 418 dp như khung chờ (mô tả đúng 2 dòng, cao ít nhất 40 dp; không có mô tả thì chỗ trống 40 dp), như small". Code: `FULL_BODY_LINES = 2` cho cả `minLines` và `maxLines`, khung `heightIn(min = NativeAdDimens.BodyHeight)` (40 dp) (`NativeAdCard.kt`, `FullLayout`). Không script, workflow hay test nào đọc `ads-guide.md` (`grep -rn ads-guide.md scripts/ .github/ */src/test core/*/src/test`: không có), nên không file nào khác bị ảnh hưởng (L-020).

**D. `RewardedTurnsTest` (T-042).**

1. `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy`, phần đối chứng: phần đầu để lại 2 cổng đã mở trong `port.switchGates` (cổng của lần bấm hai, không dùng vì lần bấm hai nhập vào, và cổng thừa cho lượt nạp, không dùng vì có quảng cáo). Lần gọi `doubleTapAwaitingSwitch` thứ hai thêm 3 cổng sau 2 cổng này, nên lần bấm đầu của đối chứng lấy ngay cổng cũ đã mở, quyết định và tạo lượt trong cùng một nhịp, rồi lần bấm hai đi nhánh R-06 (`turns[declaration]` có) chứ không nhập vào lần quyết định. Kết quả `a`, `b` giống nhau ở hai nhánh nên đối chứng vẫn qua (đã đo, mục 4.1). Sửa: thêm `port.switchGates.clear()` cùng nhóm với `a.clear()`, `b.clear()`, `port.opportunities.clear()` trước lần gọi thứ hai, và thêm hai câu khẳng định đường đi (mục 4, dòng 1).
2. Chú thích trong `doubleTapAwaitingSwitch` (hôm nay: "With no ad, the load round of the first tap reads the switch too (F-07): it answers from its own gate, so a gate left in the queue afterwards means the second tap never waited for the switch."). Câu "answers from its own gate" sai: hàng cổng lấy theo thứ tự, lượt nạp lấy cổng thứ hai (cổng dành cho lần bấm hai), cổng thứ ba mới còn lại. Câu mới (tiếng Anh, trong code): "Three gates, taken in order by whatever reads the switch: the first tap, then a second tap that reads it itself (none when it joins the decision, plan T-042), then the load round of a decision with no ad (F-07). So after a decision with no ad one gate is left when the second tap joined, none when it read the switch; with an ad two are left." Không đổi code của hàm.

**E. `AdsControllerResumeTest` (T-043).** `resumeOff_gateHasNoPlacement_foregroundRecordsNothing`, phần đối chứng: sau `assertEquals("idle", opportunities.single().loadState)` thêm `assertEquals("APP_OPEN", opportunities.single().adFormat)` (`AdFormat.APP_OPEN.metricCode()`, `AdTelemetryRules.kt`; `AdPlacement.RESUME` khai báo `AdFormat.APP_OPEN`). Ghi chuỗi `"APP_OPEN"` thẳng như `AdTelemetryTest` (`"INTER"`, `"NATIVE_MEDIUM"`), để test không đi cùng một hàm với code.

**F. Ô AdChoices giả ở cỡ chữ 2.0: giữ nguyên.** Đã xem code và hai ảnh:

- `NativeFullScreenAdScreen_font-scale-2_light.png`: ô là `FakeAdChoices()` trong `core/ads/src/test/.../NativeFullScreenAdFakes.kt` (ô 16 dp, chữ "i" `labelSmall`), chỉ có trong test. Ở cỡ 2.0 chữ 22 sp không vừa ô 16 dp nên bị cắt. Màn xem thử (`NativeFullScreenAdScreen` của `main`) không vẽ ô AdChoices nào (`adChoices` mặc định rỗng); quảng cáo thật do SDK vẽ.
- `NativeAdCard_font-scale-2_light.png`: ô là `AdChoicesMarker` trong `NativeAdCard.kt` (`main`, chỉ vẽ cho quảng cáo giả của màn xem thử), một ô màu 16 dp **không có chữ**. Ảnh không có chữ "i" nào bị cắt; ghi chú rà soát nhầm ảnh này.
- Lý do giữ: ô giả chỉ đánh dấu chỗ SDK vẽ AdChoices; cho chữ vừa ô (cỡ chữ cố định theo dp, hay bỏ chữ) đổi điểm ảnh của mọi ảnh chuẩn có `FakeAdChoices` ở cỡ 1.0 (`NativeFullScreenAdScreen_default_*`, các ảnh `NativeFlowStep_*`), trái điều ticket giữ ("ảnh chuẩn giữ nguyên từng byte"). Ghi lý do vào KDoc của `FakeAdChoices` (một câu, không đổi code): "At font scale 2.0 the "i" is cut (plan T-046): kept, as the SDK draws the real AdChoices and a fitted "i" would move every screenshot that draws this box."

Không phần nào đổi chữ ký công khai (`NativeFullScreenFrame`, `NativeFullScreenAdHeader` là `internal`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | Bỏ `safeArea` của `NativeFullScreenFrame`, bỏ `growWithText` của `NativeFullScreenAdHeader` và đối số ở nhánh sau inter; KDoc của hai hàm (mục 3.1 A, B) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayout.kt` | `FlowStepPageLayout` gọi `NativeFullScreenAdHeader(assets, binder)` (bỏ `growWithText = true`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedTurnsTest.kt` | Đối chứng của `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy`: `port.switchGates.clear()` và hai câu khẳng định đường đi; chú thích của `doubleTapAwaitingSwitch` (mục 3.1 D) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Đối chứng của `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` kiểm `adFormat == "APP_OPEN"` (mục 3.1 E) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdFakes.kt` | KDoc của `FakeAdChoices`: lý do giữ chữ "i" bị cắt ở cỡ 2.0 (mục 3.1 F); không đổi code |
| Sửa | `docs/ops/ads-guide.md` | Bảng kích thước, ô "Khung" của cột `FULL`: "mô tả đúng 2 dòng, cao ít nhất 40 dp" (mục 3.1 C) |

Không file nào khác. `NativeFullScreenOverlay.kt` gọi `NativeFullScreenFrame(Modifier.testTag(...)) { … }`, không truyền `safeArea`, nên không phải sửa. Không ảnh chuẩn nào được ghi lại.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng, tần suất hay cách vẽ. Chỉ bỏ hai tham số `internal` không còn nơi gọi với giá trị khác, và làm chặt test.
- Sự kiện phân tích: Không chạm (test chỉ đọc thêm trường `adFormat` của `ad_opportunity` đã có).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code chỉ bớt; test dùng lớp đã có (`CompletableDeferred`, `RecordingAppLogger`, `AdMetricEvent.Opportunity`).

### 3.4. Phương án dự phòng

Không có. Mọi phần đã chạy thử hay chỉ là chú thích và tài liệu (mục 4.1).

### 3.5. Hành vi đã có bị đổi

Không có: plan không đổi hành vi nào. Code `main` chỉ bỏ hai tham số mà mọi nơi gọi đều dùng một giá trị (`safeArea` luôn `true`, `growWithText` luôn `true`); nhánh còn lại (`safeArea = false`, chiều cao đầu cố định 48 dp) không nơi nào gọi tới. Bố cục, kích thước và điểm ảnh giữ nguyên.

`scripts/existing-tests.py docs/features/ads/plans/T-046-don-sau-ra-soat-quang-cao.md` (2026-10-27) liệt kê các test chạm hai file `main`; plan đã đọc từng test ở nhóm "Cần xem trước" và "Nên xem". Các test khẳng định hành vi giữ nguyên, câu khẳng định giữ nguyên:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Đầu của native toàn màn hình sau inter cao ít nhất 48 dp, cao thêm theo chữ; nhãn "Ad" giữ chiều cao chữ (O-08) | Như cũ, không còn tham số (mục 3.1 B) | `NativeAdFontScaleTest.fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales`, `NativeAdFontScaleTest.fullScreen_ctaTextFits_fontScales`, ảnh `NativeFullScreenAdScreen_font-scale-2_light.png` | Giữ |
| Đầu của trang toàn trang ở bước của luồng cao theo chữ | Như cũ, không còn đối số (mục 3.1 B) | `NativeFlowStepLayoutTest` (các test đo đầu, media, CTA theo cỡ chữ và chiều cao trang), ảnh `NativeFlowStep_*` | Giữ |
| Native toàn màn hình sau inter đệm vùng an toàn của cửa sổ; trang của bước thì không | Như cũ, `NativeFullScreenFrame` đệm không điều kiện; trang của bước dùng `FlowStepPageFrame` (mục 3.1 A) | `NativeFullScreenAdScreenTest.flowStep_noWindowInsetPadding` (trang của bước: media cách đỉnh vùng 76 dp khi có thanh trạng thái 40 dp; đối chứng: trang sau inter cách 76 + 40 dp, tức đúng nhánh `safeArea = true` hôm nay) | Giữ |
| Media của trang ở bước bắt đầu cùng độ cao với trang sau inter (76 dp) | Như cũ (mục 3.1 A, B) | `NativeFullScreenAdScreenTest.flowStep_mediaStartsLikeAfterInter` | Giữ |
| Trang của bước không có nút đóng, không đếm ngược; trang sau inter có nút đóng | Như cũ (không chạm nhánh `onClose == null` của `NativeFullScreenAdLayout`) | `NativeFullScreenAdScreenTest.flowStep_hasNoCloseButton_noCountdown` | Giữ |
| Khung chờ của native toàn màn hình dùng `NativeFullScreenFrame` | Như cũ, gọi không đổi | `NativeFullScreenOverlayTest` | Giữ |

## 4. Kiểm thử

Code `main` của plan chỉ bớt tham số, nên không có test mới đỏ được vì phần đó; hành vi được canh bằng test và ảnh có sẵn (bảng dưới, dòng KB-5, KB-16, KB-20). Hai test sửa ở `RewardedTurnsTest` và `AdsControllerResumeTest` là chính phần việc của ticket: viết câu khẳng định mới trước, xem nó đỏ đúng lý do trên code hôm nay, rồi mới thêm phần làm nó đúng.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-19 tiêu chí 6: đối chứng đi đúng nhánh nhập vào | Unit test | `RewardedTurnsTest.doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy`, phần đối chứng. **Bước 3 (đỏ trước):** chưa thêm `clear()`; ghi số dòng log `"tap joins the one being decided"` trước lần gọi `doubleTapAwaitingSwitch` thứ hai (`val joinsBefore = logger.entries.count { it.message.endsWith("tap joins the one being decided") }`), sau lần gọi khẳng định `assertEquals(joinsBefore + 1, …count…)` (lần bấm hai của đối chứng nhập vào) và `assertEquals(2, port.switchGates.size)` (lần bấm hai không đọc khóa; có quảng cáo nên không có lượt nạp lấy cổng). Đỏ đúng lý do, đã đo trên code hôm nay: lần bấm hai đi nhánh R-06, số dòng nhập vào của đối chứng 0, hàng còn 4 cổng (mục 4.1). **Bước 4:** thêm `port.switchGates.clear()` trước lần gọi thứ hai: số dòng nhập vào 1, hàng còn 2 cổng, `a` rỗng, `b == [Granted]` (đã đo). Câu khẳng định cũ (`a.isEmpty()`, `b == [Granted]`) giữ nguyên |
| KB-19: chú thích của `doubleTapAwaitingSwitch` đúng | Unit test có sẵn | Không đổi code của hàm; ba test dùng nó khẳng định số cổng còn lại khớp câu mới: `doubleTapNoAd_allow_grantsOnce_secondTapBusy`, `doubleTapNoAd_allowAfter2_countsOneTry`, `doubleTapNoAd_block_oneOutcome_userMayTryAgain` (không có quảng cáo, còn 1 cổng); dòng 1 ở trên (có quảng cáo, còn 2 cổng) |
| KB-14 tiêu chí 1: cơ hội của quảng cáo khi quay lại là `APP_OPEN` | Unit test (Robolectric) | `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing`, phần đối chứng, thêm `assertEquals("APP_OPEN", opportunities.single().adFormat)`. Câu này qua ngay trên code hôm nay (đã đo: `adFormat=APP_OPEN`, mục 4.1). **Đối chứng (L-055):** ở bước 3, phiên viết tạm `"INTER"` làm giá trị mong đợi, chạy, thấy đỏ với thông báo `expected:<[INTER]> but was:<[APP_OPEN]>`, rồi đổi về `"APP_OPEN"`; ghi kết quả vào nhật ký |
| KB-5, KB-16, KB-20: bỏ tham số không đổi bố cục | Unit test, Compose UI test, screenshot có sẵn | Các test ở mục 3.5 qua không sửa; `scripts/check.sh` với `verifyRoborazziDebug` của `:core:ads` và `verifyRoborazziStagingDebug` của `:app`: không ảnh chuẩn nào lệch (ảnh giữ nguyên từng byte) |
| KB-2: bảng kích thước đúng code | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc ô "Khung" của cột `FULL` trong `docs/ops/ads-guide.md`, so với `FULL_BODY_LINES`, `NativeAdDimens.BodyHeight` trong `NativeAdCard.kt` |
| Ô AdChoices giả: lý do giữ | Kiểm tra thủ công (người: đọc KDoc và xem ảnh) | Người duyệt đọc KDoc của `FakeAdChoices` và mục 3.1 F; xem `NativeFullScreenAdScreen_font-scale-2_light.png` (ảnh không đổi) |

**Ngoại lệ viết test trước:** (1) bỏ hai tham số (mục 3.1 A, B): không có hành vi mới để một test đỏ; hành vi giữ được canh bằng test và ảnh có sẵn (mục 3.5). (2) KDoc của `NativeFullScreenFrame`, `NativeFullScreenAdHeader`, `FakeAdChoices`, chú thích của `doubleTapAwaitingSwitch`, ô của `ads-guide.md`: chỉ chữ. (3) Câu `adFormat` của T-043 qua ngay vì code đã đúng; bước đối chứng ở dòng 3 thay cho lần đỏ trước.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, worktree `claude/plan-T-046-don-ra-soat` từ nhánh chính `6547211`, bằng sửa tạm trong hai file test (đã `git checkout`, worktree sạch). Đo đường đi của đối chứng T-042 bằng `println` số dòng log "tap joins the one being decided", số cổng còn lại, `port.showAgains`, `port.opportunities`, `a`, `b`; lần 1 không `clear()`, lần 2 có `port.switchGates.clear()` trước lần gọi thứ hai. T-043: thêm tạm câu `adFormat` và in giá trị.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*RewardedTurnsTest.doubleTap_showFailsInsideTheCall*' -i`; `./gradlew :core:ads:testDebugUnitTest --tests '*AdsControllerResumeTest.resumeOff_gateHasNoPlacement*' -i` (JDK của Android Studio). Không chạy `scripts/check.sh`: E = 0 (mục 5), không kỹ thuật mới (ADR-0029: không dựng thử cả bộ lúc soạn plan).
- **Kết quả, T-042:**

  | | Cổng còn trước đối chứng | Dòng "tap joins" của phần đầu / đối chứng | Cổng còn sau đối chứng | `showAgains` | Cơ hội | `a` / `b` |
  | --- | --- | --- | --- | --- | --- | --- |
  | Code hôm nay (không `clear()`) | 2, đều đã mở | 1 / **0** (nhánh R-06) | 4 | `[rw_allow]` | `[(shown, ready), (busy, idle)]` | `[]` / `[Granted]` |
  | Có `clear()` | 0 | 1 / **1** (nhánh nhập vào) | 2 | `[rw_allow]` | `[(shown, ready), (busy, idle)]` | `[]` / `[Granted]` |

  Kết quả và số liệu của hai nhánh giống hệt nhau, nên câu khẳng định cũ không phân biệt được; hai câu mới (số dòng nhập vào, số cổng còn) phân biệt được. Xác nhận ghi chú rà soát.
- **Kết quả, T-043:** test qua, `adFormat` của cơ hội được ghi là `APP_OPEN`.
- **Chưa chạy thử:** bỏ hai tham số (chỉ bớt code; phiên chạy `check.sh` ở bước 5).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** module core dùng chung `:core:ads` (điểm 7); code của module quảng cáo (điểm 1, ghi như T-044 dù không đổi vị trí, định dạng hay tần suất). Không thêm vị trí, quyền, SDK, sự kiện hay dữ liệu. Mức Cao theo luật, cần duyệt chéo; việc nhỏ, người dùng không thấy gì đổi.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một module (`:core:ads`, hai file `main`, ba file test) và một tài liệu; không đổi giao diện công khai |
| B. Độ rõ của yêu cầu | 0 | Ticket nêu từng điểm và cách sửa; điểm AdChoices đã quyết theo nhánh ticket cho sẵn |
| C. Khả năng kiểm chứng tự động | 0 | Test và ảnh có sẵn canh việc bỏ tham số; hai test sửa đã đo đỏ và xanh |
| D. Ảnh hưởng chéo | 1 | Sửa file dùng chung của `:core:ads` nhưng không đổi hành vi; T-047 sửa `RewardedTurns.kt` và có thể `RewardedTurnsTest.kt` (mục 6) |
| E. Độ mới của kỹ thuật | 0 | Lặp lại mẫu có sẵn |
| **Tổng** | 2 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `NativeFullScreenAdScreen.kt`, `NativeFlowStepLayout.kt`, `RewardedTurnsTest.kt`, `AdsControllerResumeTest.kt`, `NativeFullScreenAdFakes.kt`, `docs/ops/ads-guide.md` mục 4. Ảnh chuẩn, `tokens.json`, bảng khóa Remote Config không bị ảnh hưởng.
- `docs/STATUS.md`: không đưa vào PR.
- **T-047** (lượt quyết định treo của rewarded, `onStuck`): sửa `RewardedTurns.kt` và nhiều khả năng thêm test vào `RewardedTurnsTest.kt`. T-046 chỉ sửa một test và một chú thích trong file test, không chạm `RewardedTurns.kt`. Chạy T-046 trước để T-047 gộp nhánh chính có đối chứng đã sửa; chạy sau cũng được, xung đột nếu có chỉ ở chú thích của `doubleTapAwaitingSwitch`.
- Không file nào hai plan cùng **tạo** (L-054).
- Đề xuất thứ tự chạy: độc lập, đặt trước T-047 nếu cùng phiên. `depends_on` của ticket (T-041 đến T-045) đều đã merge.

## 7. Điều kiện dừng riêng

- Sau khi bỏ tham số, một ảnh chuẩn sẵn có lệch (`verifyRoborazzi*` báo lỗi): dừng, không ghi lại ảnh; báo ảnh và chỗ lệch. Plan không cho ghi lại ảnh nào.
- Ở bước 3, câu khẳng định mới của đối chứng T-042 qua ngay khi chưa có `clear()` (khác số đo mục 4.1): dừng, báo số dòng nhập vào và số cổng đo được; không đổi câu cho tới khi đỏ.
- Sau khi thêm `clear()`, câu số dòng nhập vào hay số cổng của đối chứng T-042 vẫn đỏ: dừng, báo số đo, không đổi số mong đợi.
- Sau khi thêm `clear()`, đối chứng T-042 đỏ ở câu cũ (`a.isEmpty()`, `b == [Granted]`): dừng, không sửa câu cũ; báo kết quả thật (có thể là lỗi của nhánh nhập vào mà đối chứng cũ che).

## 8. Câu hỏi cho người duyệt

1. **Ô AdChoices giả ở cỡ chữ 2.0: giữ nguyên hay cho chữ vừa ô?** Ô có chữ "i" chỉ nằm trong test (`FakeAdChoices`); ô của màn xem thử (`AdChoicesMarker`, `main`) không có chữ, nên ảnh `NativeAdCard_font-scale-2_light.png` không có chữ bị cắt.
   - Đề xuất của AI: giữ nguyên, ghi lý do vào KDoc của `FakeAdChoices` (mục 3.1 F). Cho chữ vừa ô đổi mọi ảnh có ô này ở cỡ 1.0, trái điều ticket giữ ảnh từng byte, mà không giúp gì quảng cáo thật. Nếu anh muốn sửa: cho chữ "i" cỡ cố định theo dp (không theo cỡ chữ hệ thống), ghi lại `NativeFullScreenAdScreen_font-scale-2_light.png` và kiểm các ảnh ở cỡ 1.0 vẫn giữ byte; khi đó plan lên phiên bản 2.
   - **Trả lời (anh Nhật, 2026-10-27):** giữ nguyên ô giả, ghi lý do vào KDoc, theo đề xuất.
2. **Điểm chặn.** Ticket ghi điểm chặn 1 (quảng cáo); plan không đổi vị trí, định dạng hay tần suất nên chỉ điểm 7 (core) là chắc chắn.
   - Đề xuất của AI: giữ `blocking_points: [ads, core]` như T-044, T-045 cho nhất quán; mức Cao không đổi vì điểm 7.
   - **Trả lời (anh Nhật, 2026-10-27):** giữ `[ads, core]`, theo đề xuất.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, `risk-scoring.md` mục 3)
- **Ghi chú:** Anh Nhật duyệt 2026-10-27 trong phiên giám sát cùng AI. Câu hỏi ở mục 8: anh Nhật đã trả lời, ghi ở mục 8. Đã qua rà soát độc lập (0 chặn, 2 nên sửa đã sửa). Chạy trước T-047 nếu cùng phiên.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
