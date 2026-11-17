---
ticket: T-061
title: Thanh đáy chừa chỗ lúc đang tải
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (core)
risk_level: high             # low | medium | high
token_estimate: 7.4M         # số gợi ý của cỡ S (scripts/token-usage.py estimate, 2026-11-17): ticket ghi M, nhưng hành vi đã có trên nhánh chính, plan chỉ thêm một test
blocking_points: [core]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:
cross_reviewer:
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0048)
---

# PLAN T-061: THANH ĐÁY CHỪA CHỖ LÚC ĐANG TẢI

## 1. Tóm tắt

**Hành vi của ticket đã có trên nhánh chính.** PR #531 (from-app #527, bài học L-184, merge 2026-11-17) đã làm thanh đáy chừa đúng chỗ khe đang chiếm ở mọi trạng thái, theo D-04: khe native chừa chiều cao khe cộng lề 16 dp ngay khi khung chờ hiện, nạp lỗi thì không vẽ gì và chừa 0, các lượt thử lại không hiện khung chờ, có quảng cáo mới chừa lại; khe banner chừa chiều cao khung từ lúc thanh được phép hiện, nạp lỗi trước quảng cáo đầu thì về 0, có quảng cáo thì về chiều cao khung. PR #538 (T-109) đổi chiều cao khe mà vẫn giữ luật này (`slotLoading_largeText_reservesPlaceholderSpace`). `ads-business-rules.md` D-04 và `ads-guide.md` mục 5 đã ghi đúng luật và khớp code. Không còn code nào phải đổi.

Phần duy nhất còn thiếu là một test đi qua đường nối production cho **lượt thử lại** của khe native sau khi nạp lỗi: hôm nay `BottomAdBarTest` kiểm điều này bằng luồng giả (`slotFeed`, chú thích "it never emits Loading again"), còn `BottomBarSlotWiringTest` dưới `AdsController` thật dừng ở lúc nạp lỗi. Plan chỉ thêm một test vào `BottomBarSlotWiringTest`; test qua ngay trên code hiện tại (đã chạy thử, mục 4.1), nên đây là test canh hồi quy, không phải test viết trước một thay đổi. Đề xuất cho người duyệt (mục 8, câu 1): ghi ticket theo PR #531 và không chạy plan này, hoặc duyệt plan để có thêm test canh.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-17, tiêu chí 5 (đổi 2026-10-29, ADR-0036, câu hỏi 37) | Nội dung của màn hình chừa đúng chiều cao của khe đáy từ lúc khe đang tải và khi khe có quảng cáo, nên nội dung không bị đẩy khi quảng cáo về; khi khe nạp lỗi thì không chừa gì. Không chừa thừa hay thiếu | Đã có từ PR #531. Kiểm bởi `BottomAdBarTest.slotLoading_reservesPlaceholderSpace` (đáy nội dung ở đỉnh khe 769 dp cả lúc tải và lúc có quảng cáo), `nativeSlot_reservationEqualsSlot_loadingLayerOpenAd`, `bannerSlot_reservationEqualsFrame_noSendLoadingAdKept`, và hai test nối production của `BottomBarSlotWiringTest` |
| KB-17, tiêu chí 5, đổi 2026-11-06 (KB-35) | Lúc lớp nổi mở, khoảng chừa giữ như đang có | Phần "khe đáy nhường lớp nổi" thuộc T-090 (backlog, phụ thuộc T-061). Hôm nay lớp nổi không tính vào khoảng chừa (`layer_notCountedInReservation`); plan không đổi |
| Câu hỏi 37 (nạp lại sau lỗi không chừa lại tới khi có quảng cáo) | Lượt nạp lại sau lỗi không chừa lại cho tới khi có quảng cáo | Đã có: khe native `retryingNativeAdStates` không phát `Loading` lần nữa (KDoc "Never emits Loading again"); khe banner `bannerSlotHeightDp` về 0 tới khi có quảng cáo (`bannerSlot_reservationEqualsFrame_failedBeforeAd_retry_adAgain`). Plan thêm test của khe native qua `AdsController` thật |
| Câu hỏi 49, câu hỏi 66 (một chiều cao cho vẽ, khung chờ, chừa chỗ) | Chiều cao chừa tính trước khi có quảng cáo, cùng hàm với chiều cao vẽ | Đã có: `rememberSmallNativeHeight`, `reservedSlot_equalsDrawnSlotHeight`, `nativeReservation_followsFontScale` (T-109) |

Thiết kế tham chiếu: Không có thiết kế riêng cho khoảng chừa (docs/ops/design-workflow.md); chiều cao khe theo `design/context/` của T-109.

ADR liên quan: ADR-0036 (điểm 2, thay luật D-04 cũ), ADR-0045 (mục 7.2), ADR-0031 (viết lại câu khẳng định cũ, đã làm ở PR #531).

## 3. Giải pháp

### 3.1. Cách làm

Đối chiếu ticket với nhánh chính (commit `4297ca6`):

- Phạm vi 1 của ticket (đổi `BottomAdBar.kt` dòng 68-72): đã làm ở PR #531. `bottomBarReservation(slot, nativeTakesSpace, bannerHeight, nativeSlotHeight)` chừa theo chỗ khe đang chiếm; `NativeAdSlotGate.onTakesSpaceChange` báo khe native có chiếm chỗ (khung chờ hay quảng cáo); `BannerSlotHost` báo chiều cao khung (`onHeight`), `bannerSlotHeightDp(inBottomBar = true)` về 0 khi nạp lỗi trước quảng cáo đầu.
- Phạm vi 2 (viết lại câu khẳng định cũ theo ADR-0031, ảnh chuẩn): PR #531 đã viết lại `slotLoading_reservesNothing` (nay `slotLoading_reservesPlaceholderSpace`), `placementOnAgain_barBuiltFresh`, `restore_reservationZeroUntilAvailableAgain`, `reservation_noAd_isZero`. Không ảnh chuẩn nào đổi: screenshot test của màn mẫu và màn xem thử chạy không có `AdsController`, thanh đáy không dựng, nội dung không có lề (`withoutAdsController_drawsContentOnly_noPadding`).
- Phạm vi 3 (đối chiếu `ads-business-rules.md` D-04 và `ads-guide.md`): đã đối chiếu, khớp code; PR #531 sửa cả hai.

Việc còn lại của plan: thêm test `bottomNativeSlot_retryAfterFailure_takesNoSpaceUntilAd` vào `BottomBarSlotWiringTest`, dựng như `bottomNativeSlot_placeholderTakesSpace_failureDoesNot` (`testAdsController` với `FakeNativeAdLoader`, `start`, `openConsentGateForTest`, `admobReadyForTest`, `setAppForegroundForTest(true)`, `BottomNativeSlot` với `onTakesSpaceChange` ghi vào danh sách):

1. Lượt đầu đang bay: `reported == [true]`.
2. Mọi yêu cầu đang mở nạp lỗi (`loader.fail(3)`): `reported == [true, false]`, không có khung chờ.
3. Đẩy đồng hồ tới lượt thử lại (O-05: 5 giây sau lỗi đầu): `composeRule.mainClock.advanceTimeBy(6_000)` rồi `shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))`, `waitForIdle`. Khẳng định: `loader.requests.size` lớn hơn trước bước này (đối chứng: lượt thử lại thật sự đã gửi, nên câu sau không qua vì chưa có lượt nào), `placeholders() == 0`, `reported == [true, false]` (thử lại không chừa lại).
4. Lượt thử lại có quảng cáo (`loader.deliver(FakeLoadedNativeAd())`): `reported == [true, false, true]`.

Không đổi code ngoài thư mục test.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomBarSlotWiringTest.kt` | Thêm test `bottomNativeSlot_retryAfterFailure_takesNoSpaceUntilAd` (mục 3.1); KDoc của lớp thêm một câu về lượt thử lại; thêm import `java.time.Duration` |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm (không thêm, bớt hay di chuyển vị trí; chỉ thêm test cho `SAMPLE_NATIVE_SMALL` đã có)
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (test dùng `FakeNativeAdLoader`, `testAdsController`, Robolectric `shadowOf` đã có trong `:core:ads`)

### 3.4. Phương án dự phòng

Không có. Lần chạy thử (mục 4.1) cho thấy cách đẩy đồng hồ ở bước 3 gửi được lượt thử lại trên code hiện tại.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin ngoài thư mục test, chỉ thêm một test; mọi câu khẳng định có sẵn giữ nguyên. `scripts/existing-tests.py` in "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)"; hai dòng dưới là test có sẵn gần nhất, ghi để người duyệt thấy chúng giữ nguyên.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Khe native thử lại sau lỗi không hiện khung chờ, không chừa | Không đổi | `BottomAdBarTest.nativeSlot_reservationEqualsSlot_failedBeforeAd_layerOpen_adAgain` (luồng giả) | Giữ |
| Khe native dưới `AdsController` thật: khung chờ chiếm chỗ, nạp lỗi thì không | Không đổi | `BottomBarSlotWiringTest.bottomNativeSlot_placeholderTakesSpace_failureDoesNot` | Giữ |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-17 tiêu chí 5, câu hỏi 37: lượt thử lại sau lỗi của khe native không chừa lại tới khi có quảng cáo, qua đường nối production | Compose UI test (Robolectric) | Mới: `BottomBarSlotWiringTest.bottomNativeSlot_retryAfterFailure_takesNoSpaceUntilAd` (mục 3.1, bốn bước) |
| KB-17 tiêu chí 5: chừa từ lúc đang tải, nội dung không bị đẩy khi quảng cáo về (native) | Compose UI test | Có sẵn: `BottomAdBarTest.slotLoading_reservesPlaceholderSpace`, `slotLoading_largeText_reservesPlaceholderSpace`, `nativeSlot_reservationEqualsSlot_loadingLayerOpenAd` |
| KB-17 tiêu chí 5: như trên (banner) | Compose UI test | Có sẵn: `BottomAdBarTest.bannerSlot_reservationEqualsFrame_noSendLoadingAdKept`, `bannerSlot_reservationEqualsFrame_failedBeforeAd_retry_adAgain`, `BottomBarSlotWiringTest.bottomBannerSlot_failedBeforeAd_reportsNoHeight` |
| KB-17 tiêu chí 5: nạp lỗi thì không chừa | Compose UI test | Có sẵn: `BottomAdBarTest.nativeSlot_reservationEqualsSlot_failedBeforeAd_layerOpen_adAgain`, `BottomBarSlotWiringTest.bottomNativeSlot_placeholderTakesSpace_failureDoesNot` |
| Câu hỏi 49, 66: cùng một chiều cao cho vẽ, khung chờ, chừa chỗ | Unit test, Compose UI test | Có sẵn: `BottomAdBarTest.reservedSlot_equalsDrawnSlotHeight`, `nativeReservation_followsFontScale` |
| Ảnh chuẩn của màn mẫu và màn xem thử không đổi | Screenshot test | Có sẵn, chạy trong `scripts/check.sh`; plan không ghi lại ảnh nào |
| Nội dung không bị đẩy khi quảng cáo thật về trên bản staging | Kiểm tra thủ công (máy thật: quảng cáo thật) | Mở màn mẫu có thanh đáy trên bản staging, bật `is_show_native_bottom`: lúc khung chờ hiện, dòng cuối của danh sách nằm ngay trên khung chờ; quảng cáo về thì dòng đó không nhảy. Đã đúng từ PR #531, kiểm lại khi nghiệm thu |

**Ngoại lệ viết test trước:** test mới qua ngay trên code hiện tại vì hành vi đã có từ PR #531 (đã chạy thử, mục 4.1); plan không có code để viết sau test. Bước "test thất bại đúng lý do" của auto-run.md, bước 3, thay bằng đối chứng trong chính test: bước 3 khẳng định lượt thử lại đã gửi (`loader.requests` tăng) trước khi khẳng định không chừa lại, nên test không qua nhờ chưa có lượt thử lại nào. Người duyệt phải đồng ý với ngoại lệ này.

### 4.1. Chạy thử

- **Đã chạy thử:** test tạm `tmp_bottomNativeSlot_retryAfterFailure_noSpaceUntilAd` (đúng bốn bước của mục 3.1) trong `BottomBarSlotWiringTest`, ở worktree tạm từ commit `4297ca6`, không commit, đã xóa worktree; 2026-11-17.
- **Lệnh:** `./gradlew :core:ads:testDebugUnitTest --tests '*BottomBarSlotWiringTest*' -i`. Không chạy `scripts/check.sh`: không cần (E dưới 2), lần chạy thử chỉ để thấy cách đẩy đồng hồ gửi được lượt thử lại.
- **Kết quả:** `BUILD SUCCESSFUL`, test tạm qua cùng hai test có sẵn của lớp. Số thấy (in từ test tạm): sau khi nạp lỗi `requests=1`, `reported=[true, false]`, 0 khung chờ; sau lần đẩy đồng hồ đầu (6 giây) `requests=2` (lượt thử lại đã gửi), `reported=[true, false]`, 0 khung chờ; đẩy thêm ba lần 6 giây, tới `requests=3`, vẫn `[true, false]` và 0 khung chờ; trả quảng cáo cho yêu cầu cuối thì `reported=[true, false, true]`. Một lần đẩy 6 giây là đủ cho bước 3; test chính thức dùng một lần, không lặp. Dòng `NullPointerException` của luồng `AdWorker` ở stderr là của test banner có sẵn (SDK AdMob trên Robolectric), không làm test nào thất bại.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** module core dùng chung (`:core:*`, risk-scoring.md mục 1, điểm 7): plan chỉ thêm test trong `core/ads/src/test`, không đổi code chạy của module, nhưng vẫn chấm theo điểm chặn, không hạ.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 0 | Một file test, không đổi giao diện công khai nào |
| B. Độ rõ của yêu cầu | 0 | D-04, câu hỏi 37, 49, 66 đã trả lời; hành vi đã có và đã có test |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tiêu chí có test tự động; bước máy thật chỉ để nghiệm thu |
| D. Ảnh hưởng chéo | 1 | Dùng `AdsController`, `FakeNativeAdLoader` dùng chung mà không sửa; T-090 sau này sửa cùng vùng nhưng không sửa file test này |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng mẫu `bottomNativeSlot_placeholderTakesSpace_failureDoesNot` |
| **Tổng** | 1 | **Mức: Cao** (chạm điểm chặn core; tổng điểm nếu không chạm là 1, Thấp) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: chỉ `BottomBarSlotWiringTest.kt` (test của `:core:ads`); dùng `AdsController`, `FakeNativeAdLoader`, `testAdsController` mà không sửa.
- Task khác trong sprint có thể xung đột: T-090 (`depends_on: [T-061, T-087]`). T-090 (`depends_on: [T-061, T-087]`). `scripts/plan-queue.py` chỉ nhận T-061 làm phụ thuộc xong khi ticket ở `on-staging`, `accepted`, `released` hay có plan; ticket `cancelled` thì T-090 bị chặn (DEAD). Vì vậy câu 1 ở mục 8 nêu cả cách gỡ chặn T-090.
- Đề xuất thứ tự chạy: không có plan approved nào sửa `BottomBarSlotWiringTest.kt`; chạy lúc nào cũng được, trước T-090.

## 7. Điều kiện dừng riêng

Không có ngoài điều kiện chung của auto-run.md. Nếu test mới thất bại trên nhánh chính khi phiên chạy (hành vi đã đổi sau ngày soạn plan), phiên dừng task và báo, không sửa code để test qua: đó là hồi quy của D-04 cần người xem.

## 8. Câu hỏi cho người duyệt

1. **Ticket đã được PR #531 làm xong. Anh chọn cách nào?** (a) Không chạy plan: ghi ticket `on-staging` (hành vi đã có trên nhánh chính từ PR #531, bản staging sau merge có nó), plan này `stopped` hay xóa trong PR duyệt; T-090 được gỡ chặn ngay. (b) Duyệt plan để thêm một test canh hồi quy qua đường nối production (mục 3.1), rồi ticket đi đường thường. (c) Hủy ticket (`cancelled`): khi đó phải bỏ T-061 khỏi `depends_on` của T-090, vì `plan-queue.py` coi phụ thuộc bị hủy là chặn. Đề xuất của AI: (a), vì test thêm chỉ phủ một đường mà `retryingNativeAdStates` đã ghi rõ và `BottomAdBarTest` đã kiểm bằng luồng giả.
2. Ticket ghi cỡ M; plan ghi `token_estimate` theo cỡ S (7.4M) vì chỉ còn một test. Anh có muốn sửa cỡ của ticket thành S cho số liệu đo token không?
3. Ngoại lệ viết test trước (mục 4): test mới qua ngay trên code hiện tại. Anh đồng ý thay bước "thất bại đúng lý do" bằng câu đối chứng trong test không?

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
