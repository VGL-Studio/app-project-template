---
ticket: T-098
title: Thẻ hộp thoại quảng cáo có thưởng công khai cho screenshot test, cách dịch chữ của khung ở :app
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 8.6M         # cỡ S, số gợi ý 8.6M của scripts/token-usage.py estimate (2026-11-10, trung vị 22 task S); không chỉnh
blocking_points: [core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-11-10
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-098: THẺ HỘP THOẠI QUẢNG CÁO CÓ THƯỞNG CÔNG KHAI CHO SCREENSHOT TEST, CÁCH DỊCH CHỮ CỦA KHUNG Ở :APP

## 1. Tóm tắt

`RewardedAdDialogCard` của `:core:ads` đổi từ `internal` sang công khai, có KDoc nói thẻ dùng cho screenshot test và preview của feature, màn hình thật vẫn gọi `RewardedAdDialog`. Một test Robolectric mới trong `:feature:sample` dựng thẻ từ một module feature (không biên dịch được khi thẻ còn `internal`), kiểm hai nút và callback; hai test mới trong `RewardedActionTest` của `:core:ads` khóa khoảng 48 dp mà nút "No, thanks" (vẽ 40 dp) chiếm trong thẻ. `docs/ops/ads-guide.md` sửa câu dịch chữ của khung (mọi chuỗi `ads_*` của `:core:ads`) theo kết quả chạy thử `:app:lintStagingDebug` (mục 4.1), `docs/ops/design-workflow.md` ghi nút Material cao dưới 48 dp chiếm 48 dp trên frame. Bài học L-162 chuyển `applied` trong PR của task.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-19 | Khi người vận hành bật vị trí rewarded của hành động đó thì hộp thoại hỏi xem quảng cáo hiện, có nút "Xem quảng cáo" và "Không, cảm ơn" | Ticket không đổi hành vi hay chữ của hộp thoại (phần "Không bao gồm" của ticket). Ticket giúp app làm đúng tiêu chí này trên giao diện của mình: feature chụp được thẻ trong screenshot test, và app hiện được hai nút theo ngôn ngữ của app mà lint không báo. Chữ mặc định của `:core:ads` là tiếng Anh ("Watch ad", "No, thanks"); chữ tiếng Việt trong tiêu chí là chữ app dịch. Các tiêu chí khác của KB-19 không đổi; test có sẵn của `RewardedActionTest` giữ |

Ticket không có tiêu chí mới trong spec: nguồn là bài học L-162 (issue #380, camera-pose). Các ý của phạm vi ticket và cách kiểm ở mục 4.

Thiết kế tham chiếu: Không có thiết kế riêng. Hộp thoại có thưởng chưa có ảnh thiết kế (`RewardedAdDialogScreenshotTest` là ảnh chuẩn, plan T-031 câu hỏi 6); docs/ops/design-workflow.md chỉ thêm một dòng hướng dẫn dựng frame.

ADR liên quan: ADR-0045 (quảng cáo qua `:core:ads`, feature không gọi SDK; mục 4.8 rewarded là hành động có điều kiện), ADR-0001 (feature chỉ phụ thuộc core: test ở `:feature:sample` dùng `:core:ads` đã có trong plugin `template.android.feature`), ADR-0020 (screenshot test của feature), ADR-0044 (tài liệu quy tắc không ghi mã bài học hay ticket), ADR-0031.

Phản hồi: không có phản hồi `triaged` hay `planned` nào của feature `ads` liên kết tới ticket này.

Plan trước: T-031 (tạo `RewardedAction.kt`, thẻ để `internal` vì chỉ `RewardedAdDialog` dùng; nút "Watch ad" 56 dp, "No, thanks" là `TextButton` có `colors` rõ).

## 3. Giải pháp

### 3.1. Cách làm

1. **`RewardedAction.kt`:** `internal fun RewardedAdDialogCard(...)` thành `fun RewardedAdDialogCard(...)` (công khai mặc định của Kotlin, như `RewardedAdDialog` cạnh nó; module không bật explicit API). Chữ ký giữ nguyên: `title: String`, `onWatch: () -> Unit`, `onNoThanks: () -> Unit`, `modifier: Modifier = Modifier`, `content: @Composable () -> Unit = {}`. KDoc thay `/** The card of [RewardedAdDialog], without state. */` bằng:

   ```kotlin
   /**
    * The card of [RewardedAdDialog], without state: the title, the feature's [content] (scrolls when long), then
    * "Watch ad" and "No, thanks". Public so a feature can draw its rewarded dialog in screenshot tests and previews,
    * where no [RewardedAction] exists. A real screen calls [RewardedAdDialog], which shows this card in a `Dialog`
    * only while the placement allows it and wires the buttons to the ad.
    */
   ```

   Thân hàm không đổi; `RewardedAdDialogTestTags` đã công khai. Không đổi `RewardedAdDialog`, chữ, kích thước hay màu.
2. **Test ở module feature (`:feature:sample`, mục 4):** chứng minh feature dựng được thẻ. `internal` của Kotlin chỉ chặn ngoài module, nên test phải nằm ngoài `:core:ads`; `:feature:sample` là feature mẫu mà mọi app chép theo, đã phụ thuộc `:core:ads` qua plugin `template.android.feature`. Test chỉ dựng thẻ và bấm hai nút, không đo bố cục nội bộ của thẻ (phần đó ở `:core:ads`, ý dưới), để app xóa hay sửa `:feature:sample` không mất test đo và không phải gộp tay khi đồng bộ. Test không thêm ảnh chuẩn mới (ảnh của thẻ đã có ở `:core:ads`) và không đổi màn `SampleScreen`.
   - **Test đo ở `:core:ads` (`RewardedActionTest`, cạnh `watchButton_is56dpHigh_fullCardWidth`):** hai test khóa số 40 dp và 48 dp mà `docs/ops/design-workflow.md` ghi (mục 4). Chúng đo qua `Spacing` và thứ tự con của thẻ, là chi tiết nội bộ của `:core:ads`, nên nằm cùng module với thẻ.
3. **`docs/ops/ads-guide.md`, mục 5, bước 4, ý "Rewarded":** câu "Chữ hộp thoại tiếng Anh trong `strings.xml` của `:core:ads`; app dịch trong `values-<ngôn ngữ>` của mình." thay bằng cách đã chạy thử (mục 4.1). Câu mới (tài liệu quy tắc, không ghi mã bài học hay ticket):

   > Chữ hiển thị của khung là các chuỗi `ads_*` tiếng Anh trong `strings.xml` của `:core:ads` (với rewarded: `ads_reward_watch` "Watch ad", `ads_reward_no_thanks` "No, thanks", `ads_reward_check_connection` "Check your connection and try again"; các chuỗi khác là nhãn quảng cáo, nút quyền riêng tư, nút thu gọn, nút đóng và đếm ngược), không sửa ở module mẫu. App hiện ngôn ngữ khác thì dịch ở `:app`: chép mọi chuỗi `ads_*` muốn dịch vào một tệp riêng trong `app/src/main/res/values/` (ví dụ `ads_strings.xml`), giá trị theo ngôn ngữ mặc định của `values/` của `:app`, và đặt bản dịch cùng tên trong mỗi `values-<ngôn ngữ>` mà `:app` có. Chuỗi có tham số (`%1$d` của `ads_native_star_rating`, `ads_close_countdown`) giữ nguyên tham số trong mọi bản. Khi đồng bộ từ dự án mẫu có thêm chuỗi `ads_*` mới mà app đã dịch khung, chép và dịch chuỗi đó cùng cách. Chỉ thêm bản dịch vào `values-<ngôn ngữ>` mà không chép chuỗi gốc vào `values/` của `:app` thì lint (`checkDependencies = true`) báo lỗi `ExtraTranslation` ("translated here but not found in default locale") và `:app:lintStagingDebug` dừng. Chuỗi gốc đặt trong tệp riêng, không trong `strings.xml` của `:app`, vì tệp đó mang `tools:ignore="MissingTranslation"` cho cả tệp: chuỗi ở đó thiếu bản dịch của một ngôn ngữ thì lint không báo; ở tệp riêng, lint báo `MissingTranslation` cho từng ngôn ngữ còn thiếu. Screenshot test vẽ thẻ bằng `RewardedAdDialogCard(title, onWatch = {}, onNoThanks = {}) { ... }` (không trạng thái, không hiện quảng cáo); màn hình thật luôn dùng `RewardedAdDialog`.

   Nếu T-082 đã merge trước khi phiên chạy task này (`values/` của `:app` là tiếng Anh, `values-vi/` có bản tiếng Việt), câu giữ nguyên vì không nêu ngôn ngữ cụ thể; ví dụ nào có nêu ngôn ngữ thì theo `values/` và `values-<ngôn ngữ>` đang có của `:app` lúc đó (câu hỏi 1).
4. **`docs/ops/design-workflow.md`, mục 2 "Dựng màn hình":** thêm một gạch đầu dòng sau "Chỉ dùng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế...":

   > - Nút Material vẽ cao dưới 48 dp (ví dụ nút chữ 40 dp như "No, thanks" của hộp thoại) chiếm 48 dp chiều cao trên frame: nút vẽ ở giữa một khoảng 48 dp. Thành phần Material luôn giữ vùng chạm tối thiểu 48 dp, nên frame vẽ khoảng của nút bằng cỡ nút thì giao diện thật cao hơn frame (8 dp với mỗi nút chữ 40 dp), và số đo lấy từ `context/` lệch với screenshot test.

   Số 40 dp và 48 dp đã đo trên code hiện tại (mục 4, Đo trên code hiện tại). Gạch đầu dòng ở mục 8.5 ("vùng chạm tối thiểu 48 dp") giữ.
5. **Không làm:** không thêm bản dịch nào cho `:core:ads` hay `:app` của dự án mẫu (ticket không đổi chữ; dự án mẫu đổi ngôn ngữ mặc định ở T-082); không thêm test kiểm đủ bản dịch ở `:app` (lint đã kiểm khi chuỗi gốc ở tệp riêng, mục 4.1; câu hỏi 3).

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedAction.kt` | `RewardedAdDialogCard` bỏ `internal`, KDoc mới (bước 1) |
| Tạo | `feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/RewardedAdDialogCardFromFeatureTest.kt` | Test Robolectric dựng thẻ và bấm hai nút từ module feature (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | Thêm hai test đo khoảng của nút "No, thanks" (mục 4); test có sẵn không đổi |
| Sửa | `docs/ops/ads-guide.md` | Mục 5, bước 4, ý "Rewarded": cách dịch mọi chuỗi `ads_*` của khung ở `:app`, cách vẽ thẻ trong screenshot test (bước 3) |
| Sửa | `docs/ops/design-workflow.md` | Mục 2: nút Material cao dưới 48 dp chiếm 48 dp trên frame (bước 4) |
| Sửa | `docs/lessons.md` | L-162: `applied`, dòng Áp dụng ghi số PR. Ghi trong PR của task, như T-100 |

Không có ảnh chuẩn screenshot nào đổi hay thêm: thân thẻ không đổi, test mới không chụp ảnh. Nơi đọc các file bị sửa:

- `RewardedAction.kt`: `grep -rn "RewardedAdDialogCard" scripts/ .github/ */src/test */*/src/test` chỉ có `core/ads/src/test/.../RewardedAdDialogScreenshotTest.kt` (gọi thẻ, vẫn biên dịch khi thẻ công khai). Không script nào đọc file này.
- `docs/ops/ads-guide.md`: `grep -rln "ads-guide.md" scripts/ .github/` chỉ cho `scripts/ads-plan.py`, nhắc file trong một chú thích (mục 5, bước 2) và trong chữ dẫn nguồn của bảng Excel (mục 3, mục 5), không mở file; plan chỉ thay một câu trong ý "Rewarded" của mục 5, bước 4, không đổi số mục, nên `scripts/ads-plan.py` không bị ảnh hưởng. `scripts/history-refs.py` quét mọi tài liệu quy tắc: câu mới không có mã bài học, ticket hay phiên.
- `docs/ops/design-workflow.md`: `grep -rn "design-workflow" scripts/ .github/` chỉ cho `scripts/figma-export.py` dòng 263, chữ của thông báo lỗi dẫn tới "mục 4" khi thiếu `FIGMA_TOKEN`; script không mở file. Plan thêm một gạch đầu dòng ở mục 2, không đổi tiêu đề hay số mục, nên `scripts/figma-export.py` không bị ảnh hưởng. `scripts/history-refs.py` quét như trên.
- `docs/lessons.md` (chỉ đổi dòng Trạng thái và Áp dụng của L-162): `.github/ISSUE_TEMPLATE/from-app.yml` chỉ dẫn tên file trong chữ mô tả; `scripts/new-app.sh` tìm dòng đánh dấu `<!-- Các bài học ghi dưới dòng này. -->`, plan không chạm dòng đó; `scripts/status.py` đọc dòng Trạng thái, `applied` là trạng thái nó đã biết; `scripts/tests/run.sh` chỉ dựng `lessons.md` giả trong thư mục tạm hay dùng tên file trong dữ liệu giả, không đọc file thật. Không file nào bị ảnh hưởng.
- `RewardedActionTest.kt`: chỉ thêm hai test, không đổi hàm phụ dựng hộp thoại mà test khác của lớp dùng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm vị trí, định dạng, tần suất hay hành vi. Chỉ đổi phạm vi nhìn thấy của một composable không trạng thái.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test mới dùng thư viện đã có trên classpath test của `:feature:sample` qua plugin `template.android.feature` (Compose UI test `ui-test-junit4`, Robolectric, `androidx.test.ext.junit`), như `SampleScreenTest`; `:core:ads` và `:core:designsystem` (`AppTheme`) đã là phụ thuộc `implementation` của feature. Hai test mới của `RewardedActionTest` dùng `LocalMinimumInteractiveComponentSize`, `CompositionLocalProvider`, `Dp` (Material 3 và Compose, đã trên classpath test của `:core:ads`; đã biên dịch trong bản nháp, mục 4).

### 3.4. Phương án dự phòng

Không có. Đổi một từ khóa phạm vi và thêm một test theo mẫu `SampleScreenTest` (E = 0).

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-098-the-hop-thoai-co-thuong.md` (2026-11-10, nhánh plan từ `88b899a`): 134 test dùng tên hay tag của `RewardedAction.kt`; 4 test "Cần xem trước", 5 test "Nên xem", 125 test chỉ dùng tên plan không nêu. AI đã đọc từng test của hai nhóm đầu:

- "Cần xem trước": `RewardedActionTest.longContent_small_buttonsInsideWindow` (hai nút nằm trong cửa sổ 360 x 640 khi nội dung dài), `noThanksText_usesOnSurfaceVariant_light_and_dark` (màu chữ "No, thanks"), `realController_adFreeWhileDialogOpen_watchRunsAction` (luồng `RewardedAdDialog` với `AdsController` thật), `watchButton_is56dpHigh_fullCardWidth` (nút "Watch ad" 56 dp, rộng hết thẻ). Cả bốn đo hay kiểm thẻ qua `RewardedAdDialog`; plan không đổi thân thẻ, kích thước, màu hay luồng: câu khẳng định giữ.
- "Nên xem": `noController_start_runsActionAtOnce`, `noThanks_closesDialog_runsNothing_logsShowThenDismiss`, `policyOverride_galleryPlacement_isAccepted`, `switchOn_start_showsDialogWithBothButtonsAndAppContent`, `twoActionsSamePlacement_bothDialogsClose`: kiểm hành vi của `RewardedAction` và `RewardedAdDialog`, không đổi: giữ. Lần chạy lại sau khi sửa plan theo rà soát (cùng ngày) thêm `RewardedTurnsTest.syncShowedAndReward_joinedTap_latestGranted`: chỉ dùng tên `RewardedAction` (lượt bấm và lượt thưởng), plan không đổi lớp đó: giữ.
- Ảnh chuẩn `RewardedAdDialog_default_light.png`, `RewardedAdDialog_default_dark.png`, `RewardedAdDialog_content_small.png` (`RewardedAdDialogScreenshotTest`, vẽ thẻ trong cùng module): thân thẻ không đổi, ảnh giữ.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `RewardedAdDialogCard` chỉ gọi được trong `:core:ads` | Module khác (feature, `:app`) gọi được thẻ (bước 1) | Không test nào khẳng định thẻ là `internal` | Không có câu cũ phải đổi; test mới ở mục 4 |

Không hành vi nào khác đổi: thân hàm, chữ ký, chữ, kích thước và màu giữ nguyên.

## 4. Kiểm thử

Hai nhóm test, mỗi nhóm ở module chứa điều nó kiểm:

- **`:feature:sample`, lớp mới `RewardedAdDialogCardFromFeatureTest`** (`feature/sample/src/test/kotlin/com/vglstudio/template/feature/sample/`): `@RunWith(AndroidJUnit4::class)`, `createComposeRule()`, không chụp ảnh (không `captureRoboImage`), không đo kích thước. Mỗi test dựng `AppTheme { RewardedAdDialogCard(title = "Unlock export", onWatch = { watch++ }, onNoThanks = { noThanks++ }) { Text("Feature content") } }` (import `com.vglstudio.template.core.ads.RewardedAdDialogCard`, `RewardedAdDialogTestTags`). Chữ nút đọc bằng `getString(com.vglstudio.template.core.ads.R.string.ads_reward_watch)` và `ads_reward_no_thanks`, không viết cứng. KDoc của lớp: feature dựng được thẻ hộp thoại có thưởng cho screenshot test và preview.
- **`:core:ads`, `RewardedActionTest`** (đã có `@GraphicsMode(NATIVE)`, khung `w411dp-h891dp-xxhdpi`, `createAndroidComposeRule<ComponentActivity>()`): hai test mới đặt ngay sau `watchButton_is56dpHigh_fullCardWidth`, dựng thẳng `AdsTestTheme { RewardedAdDialogCard(title = "Unlock export", onWatch = {}, onNoThanks = {}) { Text("Feature content") } }` trong `setContent` (cùng module nên gọi được thẻ cả trước lẫn sau thay đổi), không qua `Subject()` hay `Dialog`. Test có sẵn của lớp không đổi.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Feature dựng được thẻ (ticket, ý 1) và thẻ có hai nút của KB-19 cùng nội dung của feature | Compose UI test (Robolectric) | `RewardedAdDialogCardFromFeatureTest.featureBuildsCard_showsTitleContentAndBothButtons`: `onNodeWithText("Unlock export")`, `onNodeWithText("Feature content")` `assertIsDisplayed()`; `onNodeWithTag(RewardedAdDialogTestTags.WATCH).assertTextEquals(getString(ads_reward_watch))` và `onNodeWithTag(RewardedAdDialogTestTags.NO_THANKS).assertTextEquals(getString(ads_reward_no_thanks))`, cả hai `assertIsDisplayed()` |
| Hai nút gọi đúng callback | Compose UI test (Robolectric) | `RewardedAdDialogCardFromFeatureTest.featureBuildsCard_buttonsCallTheirCallbacks`: bấm `WATCH` thì `watch == 1`, `noThanks == 0`; bấm `NO_THANKS` thì `noThanks == 1`, `watch == 1` |
| Nút chữ chiếm 48 dp, vẽ 40 dp (ticket, ý 3; số của câu mới ở `design-workflow.md`) | Compose UI test (Robolectric, `NATIVE`) | `RewardedActionTest.noThanksButton_drawn40dp_takes48dpOfCard`: với `w`, `nt`, `card` là `getUnclippedBoundsInRoot()` của `WATCH`, `NO_THANKS`, `CARD`: khoảng của nút `card.bottom - Spacing.lg - (w.bottom + Spacing.sm)` bằng 48 dp (sai số 0,5); `nt.height` bằng 40 dp (sai số 0,5). Đối chứng `RewardedActionTest.noThanksSlot_withoutMinimumTouchSize_is40dp`: bọc thẻ trong `CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified)`, khoảng của nút bằng 40 dp, để thấy phép đo bắt đúng vùng chạm tối thiểu chứ không cộng sai khoảng cách |
| Hướng dẫn dịch chữ của khung đúng với lint (ticket, ý 2 và 4) | Kiểm tra thủ công (người: đọc tài liệu; phần lint đã chạy thử ở mục 4.1, dự án mẫu không có bản dịch nào để test tự động) | Người duyệt đọc câu mới của `docs/ops/ads-guide.md` (mục 5, bước 4, ý "Rewarded") đối chiếu với kết quả ở mục 4.1 |
| Hướng dẫn thiết kế ghi nút Material cao dưới 48 dp chiếm 48 dp (ticket, ý 3) | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc gạch đầu dòng mới ở mục 2 của `docs/ops/design-workflow.md`; số đo khớp test `noThanksButton_drawn40dp_takes48dpOfCard` |
| Hộp thoại không đổi (phần "Không bao gồm") | Screenshot test và Compose UI test có sẵn | `RewardedAdDialogScreenshotTest` (ba ảnh chuẩn giữ, `verifyRoborazziDebug` của `scripts/check.sh` qua), các test có sẵn của `RewardedActionTest` qua không sửa |

**Thất bại đúng lý do (đã chạy trên code hiện tại):** 2026-11-10, nhánh từ `88b899a`, bản nháp của lớp test ở `:feature:sample` (không commit), `./gradlew :feature:sample:testDebugUnitTest --tests '*RewardedAdDialogCardFromFeatureTest*'`: biên dịch thất bại với `Cannot access 'fun RewardedAdDialogCard(...)': it is internal in file.` ở dòng import và dòng gọi. Đây là lý do mong đợi: commit test của bước 3 làm `:feature:sample` không biên dịch được test tới khi bỏ `internal` (như T-007, T-010); ghi vào nhật ký. Không cần khung rỗng vì hàm đã có.

**Đo trên code hiện tại:**

- Trong `:feature:sample` (`AppTheme`), tạm bỏ `internal` (không commit): test qua; `WATCH` từ 112 tới 168 dp (56 dp), `NO_THANKS` từ 180 tới 220 dp (40 dp, nằm giữa khoảng 176 tới 224), `CARD` cao 248 dp, rộng 312 dp; khoảng của nút chữ `248 - 24 - (168 + 8) = 48` dp; đối chứng với `LocalMinimumInteractiveComponentSize provides Dp.Unspecified`: khoảng 40 dp.
- Trong `:core:ads` (`AdsTestTheme`, `createAndroidComposeRule<ComponentActivity>()`, khung và chế độ đồ họa như `RewardedActionTest`), bản nháp hai test đo trên code hiện tại, không đổi `RewardedAction.kt`: khoảng 48 dp, `NO_THANKS` 40 dp, thẻ 248 dp; đối chứng: khoảng 40 dp, thẻ 240 dp. `LocalMinimumInteractiveComponentSize` biên dịch không cần `@OptIn` với Material 3 của `gradle/libs.versions.toml`.
- Đã trả lại `RewardedAction.kt` và xóa mọi test tạm.

Hai test đo của `:core:ads` qua ngay trên code hiện tại (thẻ gọi được trong cùng module, kích thước không đổi): chúng khóa số mà tài liệu thiết kế ghi, không kiểm hành vi mới, nên không "thất bại trước" được; đối chứng của chúng nằm ngay trong lớp (khoảng 40 dp khi tắt vùng chạm tối thiểu). Người duyệt đồng ý ở ngoại lệ dưới.

**Ngoại lệ viết test trước:** (1) `docs/ops/ads-guide.md`, `docs/ops/design-workflow.md`, `docs/lessons.md` (L-162 `applied`), nhật ký plan, ticket và KDoc của thẻ: chỉ là tài liệu; cách dịch đã chạy thử bằng lint (mục 4.1). (2) Hai test đo trong `RewardedActionTest` qua ngay trên code hiện tại, vì kích thước thẻ không đổi; chúng khóa số 40 dp và 48 dp của tài liệu thiết kế, có đối chứng trong lớp.

### 4.1. Chạy thử

Không bắt buộc (E dưới 2). Ticket đòi chạy thử cách dịch bằng `:app:lintStagingDebug`, nên ghi ở đây.

- **Đã chạy thử:** 2026-11-10, máy dev (macOS, JDK từ `scripts/local-setup.sh`), nhánh plan từ `88b899a`. Mỗi lượt thêm tệp tạm vào `:app`, không commit; xong thì xóa tệp tạm và `git checkout -- app/src/main/res/values/strings.xml`, `git status --short` sạch. `:app` của dự án mẫu hôm nay: `values/strings.xml` tiếng Việt (`tools:locale="vi"`, `tools:ignore="MissingTranslation"` cho cả tệp), `values-en/strings.xml` tiếng Anh; chuỗi của khung chỉ có ở `core/ads/src/main/res/values/strings.xml` (tiếng Anh).
- **Lệnh:** `source ~/.local/share/app-project-template/env.sh && ./gradlew :app:lintStagingDebug` cho mỗi lượt.
- **Kết quả:**

  | Lượt | Tệp tạm | Kết quả |
  | --- | --- | --- |
  | A, cách cũ của hướng dẫn | `app/src/main/res/values-fr/strings.xml`: bốn chuỗi `network_lost_*` và ba chuỗi của khung dịch tiếng Pháp; không có chuỗi gốc của khung ở `:app` | Thất bại, 3 lỗi: `values-fr/strings.xml:7: Error: "ads_reward_watch" is translated here but not found in default locale [ExtraTranslation]`, cùng lỗi cho `ads_reward_no_thanks` (dòng 8), `ads_reward_check_connection` (dòng 9) |
  | B1, chuỗi gốc ở tệp riêng, thiếu một ngôn ngữ | A cộng `app/src/main/res/values/ads_strings.xml` (ba chuỗi, giá trị tiếng Việt, không `tools:ignore`) | Thất bại, 3 lỗi: `values/ads_strings.xml:3: Error: "ads_reward_watch" is not translated in "en" (English) [MissingTranslation]`, cùng lỗi cho hai chuỗi kia: lint báo ngôn ngữ còn thiếu (`values-en` của `:app`) |
  | B2, cách mới | B1 cộng `app/src/main/res/values-en/ads_strings.xml` (ba chuỗi tiếng Anh) | `BUILD SUCCESSFUL`, không lỗi; số cảnh báo (11) bằng lượt A |
  | D, chuỗi gốc trong `strings.xml` của `:app` | Chỉ thêm `ads_reward_watch` vào `app/src/main/res/values/strings.xml`, không bản dịch nào | `BUILD SUCCESSFUL`: thiếu bản dịch `en` mà lint không báo, vì `tools:ignore="MissingTranslation"` của cả tệp; nên hướng dẫn đặt chuỗi gốc ở tệp riêng |

  Phương án dự phòng: không có (mục 3.4). Lỗi lượt A khớp mô tả của issue #380 (`ExtraTranslation`); `MissingTranslation` chỉ thấy khi chuỗi gốc đã ở `:app` mà thiếu một ngôn ngữ (B1). Lượt chạy thử chỉ dùng ba chuỗi rewarded; sáu chuỗi `ads_*` còn lại của `:core:ads` (`ads_privacy_options`, `ads_native_badge`, `ads_native_star_rating`, `ads_collapse`, `ads_close_full_screen`, `ads_close_countdown`) cùng là chuỗi mặc định của thư viện không có bản dịch, nên lint xét chúng theo cùng quy tắc; câu hướng dẫn viết cho mọi chuỗi `ads_*`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core `:core:ads`: đổi giao diện công khai của module). Không chạm 1 (không đổi vị trí, định dạng, tần suất, SDK hay adapter), 2 tới 6, 8, 9.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một file mã chính của `:core:ads`, nhưng đổi giao diện công khai của module (thêm một composable công khai); một lớp test mới ở `:feature:sample`, hai test thêm vào `RewardedActionTest`, ba tài liệu |
| B. Độ rõ của yêu cầu | 2 | Bài học đã nhận, ticket nêu rõ các ý; còn câu hỏi mở ảnh hưởng tới cách làm: cách dịch có chờ T-082 không (câu hỏi 1), nơi đặt test (câu hỏi 2); câu hỏi 3 tới 6 là chi tiết |
| C. Khả năng kiểm chứng tự động | 1 | Phần code có test Robolectric; cách dịch đã chạy thử bằng lint trên máy nhưng không có test tự động trong repo (dự án mẫu không có bản dịch nào của chữ khung); tài liệu do người đọc |
| D. Ảnh hưởng chéo | 2 | `docs/ops/ads-guide.md` cũng do T-097 sửa (cùng danh sách thì chạy nối tiếp, `depends_on`); `RewardedAction.kt` không plan nào khác đang chờ sửa |
| E. Độ mới của kỹ thuật | 0 | Đổi phạm vi nhìn thấy và test theo đúng mẫu `SampleScreenTest`, `RewardedActionTest` |
| **Tổng** | 7 | **Mức: Cao** (chạm điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `RewardedAction.kt` của `:core:ads` (chỉ phạm vi nhìn thấy của thẻ), `docs/ops/ads-guide.md`, `docs/ops/design-workflow.md`. Ứng xử của hộp thoại ở app không đổi; app đã chép `RewardedAdDialogCard` công khai (như camera-pose) nhận đúng chữ ký đó qua `scripts/template-sync.sh`.
- Task khác trong sprint có thể xung đột: T-097 (ô native trong lưới, plan đang viết song song) sửa `docs/ops/ads-guide.md` (mục 4 hay mục 5, phần native trong danh sách); T-098 chỉ sửa câu cuối của ý "Rewarded" ở mục 5, bước 4. T-099 (thẻ native của bước) có thể cũng sửa `ads-guide.md`, ở phần native của bước, không chồng dòng với ý "Rewarded"; chạy cùng danh sách thì xếp nối tiếp như với T-097. T-082 (chữ mặc định tiếng Anh, chưa có plan, `auto_run: false`) đổi ngôn ngữ của `values/` ở `:app` (câu hỏi 1).
- Đề xuất thứ tự chạy: sau T-097 theo `depends_on` của ticket (cùng sửa `ads-guide.md`, để `run-check.py` không báo hai plan cùng sửa một file trong cùng lượt). Plan này không viết phần nào của T-097. Nếu T-097 chưa duyệt, người duyệt có thể bỏ phụ thuộc này (câu hỏi 4).

## 7. Điều kiện dừng riêng

- Cần đổi chữ, kích thước, màu hay hành vi của hộp thoại, hay đổi chữ ký của `RewardedAdDialogCard`, để test qua: dừng (ngoài phạm vi ticket).
- Ảnh chuẩn `RewardedAdDialog_*.png` của `:core:ads` lệch: dừng (thân thẻ không được đổi).
- Không phải điều kiện dừng, ghi để không quên: sau khi merge, đóng issue #380 kèm số PR khi dev đồng ý (định nghĩa hoàn thành của ticket); phiên ghi vào "Việc còn lại" của nhật ký, không tự đóng issue.
- Câu dịch của `ads-guide.md` lúc phiên chạy không còn đúng vì T-082 đã đổi ngôn ngữ mặc định của `:app` theo cách khác plan giả định (ví dụ `:app` không còn `values/strings.xml` hay chuỗi gốc của khung đã được T-082 đặt sẵn ở `:app`): dừng, để người chọn câu chữ.

## 8. Câu hỏi cho người duyệt

1. **Cách dịch có phải chờ T-082 không.** T-082 (chưa có plan, `auto_run: false`) đổi `values/` của `:app` sang tiếng Anh, bỏ `values-en/`, thêm `values-vi/`; ticket T-098 ghi "T-082 làm trước thì cách dịch theo kết quả của nó". Lượt chạy thử ở mục 4.1 cho thấy quy tắc của lint không phụ thuộc ngôn ngữ mặc định: chuỗi gốc phải có ở `values/` của `:app`, và mỗi `values-<ngôn ngữ>` của `:app` phải có bản dịch. Đề xuất: viết câu hướng dẫn không nêu ngôn ngữ cụ thể (mục 3.1 bước 3), nên đúng cả trước lẫn sau T-082, không chờ T-082. Plan của T-082, khi viết, đối chiếu đoạn này: nếu T-082 bỏ `tools:ignore="MissingTranslation"` khỏi `strings.xml` của `:app` thì vế lý do "tệp riêng" của câu hướng dẫn phải sửa theo. Liên quan: tiêu chí 1 của KB-19 ghi nút "Xem quảng cáo", "Không, cảm ơn", trong khi `:core:ads` (tiếng Anh, ngoài phạm vi T-082) và `:app` của dự án mẫu (mặc định tiếng Việt) hiện "Watch ad", "No, thanks"; ticket không đổi chữ, nên plan không sửa chỗ lệch có sẵn này (câu hỏi 5).
2. **Nơi đặt test "feature dựng được thẻ".** `internal` chỉ chặn ngoài module, nên test trong `:core:ads` không chứng minh được. Đề xuất: lớp test mới trong `:feature:sample` (feature mẫu mà app chép theo, đã phụ thuộc `:core:ads`), chỉ dựng thẻ và bấm hai nút, không chụp ảnh, không đổi `SampleScreen`; hai test đo khoảng 48 dp nằm ở `RewardedActionTest` của `:core:ads`, vì chúng dựa vào bố cục nội bộ của thẻ (`Spacing`, thứ tự con) và app có thể xóa hay sửa `:feature:sample` (phải gộp tay khi đồng bộ). Phương án khác: test phản chiếu trong `:core:ads` kiểm `KVisibility.PUBLIC` (cần `kotlin-reflect`, thư viện mới cho test); hay thêm screenshot test của thẻ ở `:feature:sample` (thêm ảnh chuẩn trùng ảnh đã có ở `:core:ads`).
3. **Test kiểm đủ bản dịch ở `:app`.** App nguồn có `AdsStringsTranslationTest`. Đề xuất: dự án mẫu không thêm, vì dự án mẫu không có bản dịch nào của chữ khung, và lint đã báo `MissingTranslation` khi chuỗi gốc ở tệp riêng (lượt B1); hướng dẫn chỉ cách đặt tệp. Phương án khác: hướng dẫn thêm câu gợi ý app nhiều ngôn ngữ viết test như vậy.
4. **Phụ thuộc T-097.** Ticket ghi `depends_on: [T-097]` vì cùng sửa `docs/ops/ads-guide.md`; T-098 chỉ thay câu cuối của ý "Rewarded" (mục 5, bước 4), T-097 sửa phần native trong danh sách. Đề xuất: giữ phụ thuộc, xếp T-098 sau T-097 trong cùng danh sách chạy. Nếu T-097 tách nhỏ hay chưa duyệt ở cùng buổi họp, người duyệt có thể bỏ phụ thuộc (sửa ticket) để T-098 chạy riêng; khi đó phiên sau của T-097 dựng trên câu đã sửa.
5. **Dự án mẫu có tự dịch chữ khung sang tiếng Việt không.** `:app` của dự án mẫu mặc định tiếng Việt, nên hộp thoại hiện nút tiếng Anh giữa giao diện tiếng Việt. Đề xuất: không làm trong T-098 (ticket không đổi chữ của hộp thoại); để T-082 hay một ticket sau quyết, theo đúng cách của hướng dẫn mới. Nếu người duyệt muốn làm ngay, plan thêm `app/src/main/res/values/ads_strings.xml` (tiếng Việt) và `app/src/main/res/values-en/ads_strings.xml` (tiếng Anh) như lượt B2, và kiểm lại các ảnh chuẩn của `:app` có hộp thoại (hiện không có).
6. **Thẻ công khai bị dùng nhầm ở màn hình thật.** Feature có thể gọi `RewardedAdDialogCard` thay `RewardedAdDialog` và bỏ qua quảng cáo. Đề xuất: chấp nhận, KDoc và hướng dẫn ghi rõ màn hình thật dùng `RewardedAdDialog`; rà soát độc lập của task app bắt chỗ dùng sai. Phương án khác: test quét mã nguồn từ chối `RewardedAdDialogCard` trong `src/main` của feature (thêm việc, ít giá trị khi chỉ một hàm).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev: người duyệt cũng là người duyệt chéo, `risk-scoring.md` mục 3)
- **Ghi chú:**
  - AI ghi theo lời anh Nhật ngày 2026-11-10, trong phiên có người: "Duyệt theo đề xuất của AI".
  - Lời duyệt trả lời bản plan ở commit ab54565 (PR #441), sau rà soát độc lập. Mục 8 theo đề xuất AI đã trình ngay trước lời duyệt:
    - câu 1: không chờ T-082, câu hướng dẫn không nêu ngôn ngữ cụ thể;
    - câu 2: test dựng thẻ ở `:feature:sample`, hai test đo 48 dp ở `RewardedActionTest` của `:core:ads`;
    - câu 3: không thêm test kiểm đủ bản dịch ở `:app`;
    - câu 4: giữ phụ thuộc T-097, T-098 chạy sau T-097;
    - câu 5: không dịch chữ khung sang tiếng Việt trong T-098, để T-082 quyết (kể cả chỗ lệch chữ nút của KB-19);
    - câu 6: chấp nhận rủi ro dùng nhầm thẻ công khai, không thêm test quét.
  - Phiên tự chạy làm được (`lane` để trống).

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
