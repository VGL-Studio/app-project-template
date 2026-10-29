---
ticket: T-053
title: Đếm ngược native sau inter và bung lại native thu gọn theo JBase
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 23.9M        # cỡ M (ticket đổi từ S sang M theo rà soát độc lập, 2026-10-29: 36 file, 9 ảnh chuẩn), số gợi ý 23.9M (scripts/token-usage.py estimate, 2026-10-29)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-30
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-053: ĐẾM NGƯỢC NATIVE SAU INTER VÀ BUNG LẠI NATIVE THU GỌN THEO JBASE

## 1. Tóm tắt

Native toàn màn hình (sau inter và lượt độc lập) đếm ngược theo khóa mới `time_close_native`: mặc định 5 giây, nhận 1 tới 30, giá trị khác dùng 5; số giây đọc lúc màn native mở, như JBase. Vòng đếm ngược và nút đóng vẽ 48 dp, vẫn ở góc trên trái tại (16, 24) dp; phần chờ inter rời màn tối đa 2 giây của K-02 giữ nguyên. `time_expand_collap_ad` mặc định 10 giây thay 30, 0 vẫn là không bung lại. Kiểm chứng bằng unit test của hàm thuần, của bộ điều phối lượt và của khóa, test Compose đo kích thước, và ghi lại 9 ảnh chuẩn của màn native toàn màn hình.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-32, tiêu chí 1 | Khi native toàn màn hình sau inter hiện thì đếm ngược mặc định 5 giây trước khi đóng được; người vận hành đổi bằng `time_close_native` (1 tới 30 giây, giá trị khác dùng 5). Vòng đếm ngược và nút đóng 48 dp, ở góc trên trái (K-02, K-03) | Một hàm thuần đổi giá trị từ xa thành số giây: 1..30 giữ nguyên, mọi giá trị khác (0, âm, 31 trở lên) ra 5, như JBase (`NativeFullscreenTemplate.java:101-106`). Số giây đọc một lần lúc màn native mở; đổi khóa giữa lúc đếm không đổi mốc của lượt đang mở, lượt sau dùng giá trị mới |
| KB-32, tiêu chí 2 | Khi người dùng thu gọn native thu gọn thì nó tự bung lại sau 10 giây nếu người vận hành không đổi `time_expand_collap_ad`; 0 vẫn là không bung lại (CL-03) | Chỉ đổi giá trị mặc định của khóa; luật bung lại (`CollapsibleReexpand.kt`) giữ nguyên, kể cả 0 hay âm là không bung (JBase coi 0 là bung ngay, đó là lỗi, ADR-0038) |
| KB-16, tiêu chí 2 | Màn native có đếm ngược trong vòng ở góc trên trái; hết đếm ngược thì vòng đó thành nút đóng. Đổi 2026-10-29: mặc định 5 giây, `time_close_native`, vòng 48 dp | Như KB-32. Vòng đếm ngược và nút đóng cùng khung 48 x 48 dp tại (16, 24) dp; hàng trên của màn sau inter cao 48 dp, nên media bắt đầu ở y = 88 thay 76 |
| KB-16, tiêu chí 3 | Khi native không tải được thì nút đóng dùng được ngay và màn tự đóng khi hết đếm ngược | Giữ nguyên (K-04); "hết đếm ngược" nay là hết số giây của lượt đó |
| KB-16, tiêu chí 4 tới 7 | Đi tiếp đúng màn, mốc `ads_interval`, khóa tắt, inter bị chặn, inter không lên màn | Không đổi. Phần chờ inter rời màn tối đa 2 giây (kiểm mỗi 200 ms) giữ nguyên (ADR-0038, quyết định 3; L-A10) |
| KB-5, tiêu chí 1 và 2 | Đổi 2026-10-29: đếm ngược và nút đóng theo KB-32 (5 giây, 48 dp); nút quay lại theo thời gian đếm ngược của KB-32 | Lượt native toàn màn hình độc lập dùng cùng khóa `time_close_native` và cùng nút 48 dp (KB-5, dòng 108 của spec: "đếm ngược và nút đóng theo KB-32 (5 giây, 48 dp)"). Nút quay lại của máy không làm gì trong lúc đếm, sau đó đóng như nút đóng (K-08, câu hỏi 17 của spec); chỉ độ dài đổi |
| KB-4, tiêu chí 3 | Đổi 2026-10-29: mặc định 10 giây như JBase | Như KB-32, tiêu chí 2 |

Thiết kế tham chiếu: `features/ads/design/README.md`, các ảnh trong `design/screens/`: `NativeFullScreenAdScreen_default_light`, `NativeFullScreenAdScreen_default_dark` (vẽ nút đóng 36 dp, trước ADR-0038). Spec đã xác nhận ngày 2026-10-29 đứng trên ảnh thiết kế cũ (ADR-0038, quyết định 1: quyết định của người phụ trách sản phẩm trước). Task không viết cứng giá trị nào để giảm tỷ lệ khác biệt (ADR-0020); ảnh so sánh mới và tỷ lệ khác ghi vào `design/review/`, README thiết kế ghi rõ ảnh còn vẽ 36 dp. Vẽ lại trên Figma là câu hỏi 1.

ADR liên quan: ADR-0038 (quyết định 3, hai gạch đầu), ADR-0026 (luật PARTNER còn giữ), ADR-0023 (khóa khai báo một chỗ, có bảng sinh tự động), ADR-0020 (ảnh thiết kế và ảnh chuẩn), ADR-0029, ADR-0031 (test cũ).

Nguồn JBase (đường dẫn trong gói `AndroidBuild_Template/unityLibrary/src/main/java/game/`, chỉ đọc để dẫn chứng, không chép code):

- `NativeFullscreenTemplate.java:53`: đếm ngược mặc định 5 giây.
- `NativeFullscreenTemplate.java:101-106`: đọc `time_close_native` lúc hiện; nhận giá trị lớn hơn 0 và không quá 30, khác thì dùng 5.
- `NativeFullscreenTemplate.java:325`: vòng đếm ngược 48 dp, góc trên trái (dòng 315).
- `NativeBannerTemplate.java:34`: tự bung lại sau 10 000 ms.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn: khóa chung trong `AdsSettings` đọc qua `AdsFlags` (plan T-019), hàm thuần trong `NativeAfterInterRules.kt` và bộ điều phối `NativeFullScreenTurns` với port giả trên JVM (plan T-026), mốc tuyệt đối trên đồng hồ đơn điệu (K-02).

1. **`AdsSettings.kt`: khóa mới và mặc định mới.**
   - `val TIME_CLOSE_NATIVE = longKey(name = "time_close_native", default = 5L, description = "Countdown before the full-screen native ad can be closed, in seconds; 1 to 30, any other value uses 5.")`.
   - Tên này của JBase, không phải của AdsKit, nên mô tả không được kết thúc bằng câu "Name set by the template". Thêm `internal val JBASE_KEY_NAMES: Set<String> = setOf("time_close_native")`; `nameNote` trả `"$description $JBASE_NAME"` với `JBASE_NAME = "Name from JBase, not an AdsKit key."` khi tên thuộc `JBASE_KEY_NAMES`, giữ nhánh AdsKit và nhánh "Name set by the template" như cũ. Câu mới vẫn chứa "not an AdsKit key", nên `AdsSettingsTest.keysNamedByTemplate_areMarked` giữ nguyên.
   - `TIME_EXPAND_COLLAP_AD.default` từ `30L` thành `10L`; mô tả không đổi.
   - `GLOBAL_KEYS`: thêm `TIME_CLOSE_NATIVE` ngay sau `IS_SHOW_NATIVE_AFTER_INTER`, cùng thứ tự với dòng mới của bảng mục 10.1 (bước 7).
2. **`NativeAfterInterRules.kt`: hàm thuần của số giây.**
   - Bỏ `NATIVE_AFTER_INTER_COUNTDOWN_MILLIS` và `MAX_SECONDS_LEFT`.
   - Thêm `internal const val NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS = 5L`, `internal val NATIVE_CLOSE_COUNTDOWN_SECONDS = 1L..30L`, và `internal fun nativeCloseCountdownSeconds(remote: Long): Long = if (remote in NATIVE_CLOSE_COUNTDOWN_SECONDS) remote else NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS` (K-02).
   - `countdownSecondsLeft(deadline: Long, now: Long, countdownSeconds: Long): Int`: làm tròn lên như cũ, 0 khi đã qua mốc, giới hạn trên là `countdownSeconds` thay số 3 cố định.
   - `INTER_GONE_WAIT_MILLIS = 2_000L`, `INTER_GONE_POLL_MILLIS = 200L`, `awaitInterGone`: không đổi.
3. **`AdsFlags.kt`: giá trị đã chuẩn hóa.**
   - `AdsGlobalFlags` thêm trường cuối `val closeNativeCountdownSeconds: Long` (KDoc: `time_close_native`, 1 to 30, any other value read as 5).
   - `DefaultAdsFlags.global`: thêm `settings.value(AdsSettings.TIME_CLOSE_NATIVE)` ở **cuối** danh sách của `combine` (chỉ số 14, các chỉ số cũ giữ nguyên), `closeNativeCountdownSeconds = nativeCloseCountdownSeconds(values[14] as Long)`.
4. **`NativeFullScreenTurns.kt`: độ dài của từng lượt.**
   - `NativeFullScreenPort` thêm `fun closeCountdownSeconds(): Long` (KDoc: `time_close_native` read now, already in 1..30, K-02).
   - `Turn` thêm `var countdownSeconds = 0L`. `open()` đọc `port.closeCountdownSeconds()` một lần, rồi `deadline = clock.nowMillis() + countdownSeconds * 1_000`. Áp cho cả combo và lượt độc lập. Lượt đang mở không đọc lại khóa.
   - `countDown()`: các mốc `(countdownSeconds - 1 downTo 0).map { deadline - it * TICK_MILLIS }` của lượt đó, thay `MAX_TICKS` cố định (bỏ hằng số này). `publish()` gọi `countdownSecondsLeft(deadline, now, countdownSeconds)`.
   - Phần chờ inter rời màn (`awaitInterGone`) và mọi luật đóng (K-04 tới K-08) giữ nguyên.
5. **`AdsController.kt`: nối port.** Port của `nativeTurns` thêm `override fun closeCountdownSeconds(): Long = globalFlags.value.closeNativeCountdownSeconds` (giá trị mới nhất của Remote Config, mặc định khi chưa có giá trị nào, như thanh đáy đọc `time_expand_collap_ad`). `DEFAULT_GLOBAL_FLAGS` thêm `closeNativeCountdownSeconds = nativeCloseCountdownSeconds(AdsSettings.TIME_CLOSE_NATIVE.default)`. Sửa KDoc dòng 96 và 1042 ("countdown of 3 s" thành "countdown of `time_close_native`, 5 s by default").
6. **`NativeFullScreenAdScreen.kt`: nút và vòng 48 dp.**
   - `NativeFullScreenAdDimens.CloseButtonSize = 48.dp`; thêm `CloseBarHeight = 48.dp` cho hàng trên của màn sau inter và khung chờ. `TopBarHeight = 36.dp` giữ nguyên: đó là hàng trên của trang ở bước của luồng (`NativeFlowStepLayout.kt` dùng nó ở dòng 79, 111, 271, 293), task này không đổi trang đó (câu hỏi 2). `CloseIconSize = 24.dp` giữ nguyên.
   - `NativeFullScreenTopBar` dùng `CloseBarHeight`. `CloseButton` vẫn là `Box` tròn `clickable`, vẽ và chiếm 48 dp, nên vùng chạm bằng đúng khung vẽ (không còn phần nới của Compose). `CountdownBadge` cùng khung 48 dp, chữ và màu như cũ, không nhận chạm. Không thêm vòng tiến độ như JBase (vẫn số giây trong hình tròn, K-03).
   - Sửa KDoc dòng 45, 148, 162, 186 theo số mới. `NativeFullScreenOverlay.kt` không đổi: khung chờ gọi lại `NativeFullScreenTopBar`.
   - Hệ quả trên màn sau inter (khung 411 x 891): nút ở (16, 24, 64, 72), media bắt đầu ở y = 24 + 48 + 16 = 88, media ở cỡ chữ 1.0 thấp hơn 12 dp (583 dp thay 595 dp). Trang ở bước của luồng: media vẫn ở y = 76.
7. **Tài liệu.**
   - `docs/ops/ads-business-rules.md`: K-02 (đếm ngược `time_close_native`, mặc định 5, 1 tới 30, đọc lúc màn mở; chữ cũ "3 giây" gạch ngang, ghi "Đổi 2026-10-29 (ADR-0038)" và nguồn `NativeFullscreenTemplate.java:53,101-106`; giữ vế chờ inter rời màn 2 giây kèm câu "JBase không có, giữ để chống kẹt (L-A10)"); K-03 (vòng và nút 48 dp tại (16, 24), nguồn `NativeFullscreenTemplate.java:315,325`); K-04 ("vẫn đóng được sau 3 giây" thành "sau khi hết đếm ngược"); CL-03 (mặc định 10, nguồn `NativeBannerTemplate.java:34`; giữ "0 hay âm là không bung lại" kèm lý do của ADR-0038); mục 10.1 (dòng `time_close_native` sau `is_show_native_after_inter`, nguồn tên "JBase"; `time_expand_collap_ad` mặc định 10); mục 11 (dòng "Đếm ngược native sau inter; chờ inter rời màn" thành "`time_close_native` (mặc định 5 giây, không còn là hằng số); 2 giây", dòng "Nút đóng native sau inter" thành 48 dp); mục 12, dòng L-A10 (ghi thêm: JBase không chờ, ta giữ, ADR-0038).
   - `docs/ops/ads-guide.md`: dòng 44 (đếm ngược `time_close_native`, mặc định 5 giây), dòng 55 (nút đóng 48 dp ở góc trên trái, thay "36 dp ở góc trên phải"), dòng 127 (mục Thanh đáy: "mặc định 30, 0 là không mở lại" thành "mặc định 10, 0 là không mở lại"), dòng 141 (ví dụ log `Collapsible <mã> re-expanded after 30 s` thành `after 10 s`). Thêm một câu ở mục phát hành: app đã đặt `time_expand_collap_ad` trên Firebase thì giữ giá trị đó; chỉ app chưa đặt mới đổi từ 30 sang 10 giây. Chỉ khi người duyệt nhận câu hỏi 3: thêm câu khuyến cáo chính sách của câu đó vào cùng mục phát hành.
   - `docs/ops/remote-config.md`, mục 2 và mục 3: khóa mang tên JBase (ADR-0038) ghi nguồn "JBase" trong bảng khóa, mô tả kết thúc bằng `Name from JBase, not an AdsKit key.`.
   - `docs/features/ads/design/README.md`: dòng `Native/FullScreen` (biến thể `After interstitial`), dòng "Nút đóng toàn màn hình" của bảng ánh xạ (48 dp, vùng chạm bằng khung vẽ) dòng 84 (trang ở bước của luồng: "media bắt đầu ở y = 76 như bản sau inter" thành "media bắt đầu ở y = 76; bản sau inter ở y = 88 vì hàng nút 48 dp", theo câu hỏi 2), dòng 93 ("mặc định 30" thành "mặc định 10") và một ghi chú: ảnh `NativeFullScreenAdScreen_default_*` còn vẽ 36 dp, ADR-0038 đứng trên ảnh, chờ vẽ lại (câu hỏi 1). Thêm đoạn kết quả so sánh của T-053.
   - `docs/features/ads/design/review/`: ghi lại `NativeFullScreenAdScreen_default_light_compare.png`, `_dark_compare.png` bằng `scripts/design-compare/DesignCompare.java` cho `core/ads` như T-027 (script so sánh của feature tìm ảnh chuẩn ở `feature/ads/`, không thấy ảnh của `:core:ads`, nhật ký T-026). `review/summary.md` không đổi, như T-027; tỷ lệ khác và nhận xét ghi vào đoạn kết quả so sánh của README thiết kế.
   - `docs/remote-config-keys.md`, `docs/remote-config-template.json`: sinh lại bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` (`RemoteConfigKeysTest`), không sửa tay. Chỉ hai dòng đổi: thêm `time_close_native`, `time_expand_collap_ad` mặc định `10`.
   - `scripts/ads-plan.py`, `KNOWN_KEYS`: thêm `time_close_native` (nhóm "Interstitial", giá trị phát hành đề xuất `5`, "giây, 1 tới 30; giá trị khác dùng 5", nguồn "JBase"); `time_expand_collap_ad` giá trị phát hành đề xuất `10`; câu của `is_show_native_after_inter` "đếm ngược 3 giây" thành "đếm ngược theo time_close_native".
8. **Ghi lại ảnh chuẩn** của mục 3.2 bằng lệnh ghi ảnh kèm `--rerun` như `CLAUDE.md`, mục Lệnh, rồi `git status --short -- '*/src/test/screenshots/'` chỉ được đổi 9 ảnh của mục 3.2 (L-114).

Thứ tự commit (L-025): commit test của bước 3 gồm mọi test ở mục 4, `FakeNativeFullScreenPort` có trường `var countdownSeconds = 3L` và hàm `closeCountdownSeconds()`, cùng khung rỗng ở code chính để test biên dịch được: `NativeFullScreenPort.closeCountdownSeconds()`, `AdsGlobalFlags.closeNativeCountdownSeconds`, `AdsSettings.TIME_CLOSE_NATIVE` với mặc định `0L`, `nativeCloseCountdownSeconds` trả `0`, `countdownSecondsLeft` nhận tham số mới mà vẫn giới hạn ở 3. Commit code của bước 4 làm phần còn lại.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | `TIME_CLOSE_NATIVE`, `JBASE_KEY_NAMES`, câu `Name from JBase`, `TIME_EXPAND_COLLAP_AD` mặc định 10, `GLOBAL_KEYS` (mục 3.1, bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsFlags.kt` | `AdsGlobalFlags.closeNativeCountdownSeconds`, đọc khóa ở cuối `combine` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeAfterInterRules.kt` | Bỏ hằng số 3 giây; `nativeCloseCountdownSeconds`, hằng số mặc định và khoảng; `countdownSecondsLeft` nhận số giây (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurns.kt` | `NativeFullScreenPort.closeCountdownSeconds()`, độ dài theo lượt, bỏ `MAX_TICKS` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Port đọc `globalFlags`, `DEFAULT_GLOBAL_FLAGS`, KDoc dòng 96 và 1042 (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreen.kt` | `CloseButtonSize = 48.dp`, `CloseBarHeight`, KDoc (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdHost.kt` | Chỉ KDoc dòng 12: "countdown of 3 s" thành "countdown of `time_close_native`" |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAfterInterRulesTest.kt` | Viết lại `countdownSecondsLeft_roundsUp_andStopsAtZero` theo tham số mới; test mới của `nativeCloseCountdownSeconds` và mặc định (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenTurnsTest.kt` | `FakeNativeFullScreenPort.countdownSeconds` (mặc định 3, dữ liệu test), `closeCountdownSeconds()`; test mới (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerNativeComboTest.kt` | Câu khẳng định 3 giây thành 5 giây (mục 3.5); test mới đọc khóa từ xa |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Bảng 15 khóa chung (thêm `time_close_native` 5, `time_expand_collap_ad` 10); test câu `Name from JBase` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | Mặc định 5 và 10; giá trị từ xa của `time_close_native`; trường mới trong hai đối tượng mong đợi |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Nút 48 dp, media từ y = 88, trang bước của luồng vẫn 76 (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayTest.kt` | Khung vòng và nút (16, 24, 64, 72); test ra nền rồi trở lại lúc đếm (L-091) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | Nút 48 dp, media 583 dp ở cỡ chữ 1.0 (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Test mới: bung lại sau mặc định 10 giây |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/template/adgallery/AdGalleryViewModelTest.kt` | Dữ liệu test: `FakeAdsFlags` thêm `closeNativeCountdownSeconds = 5` (ADR-0029, trường mới của `AdsGlobalFlags`) |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_light.png` | Nút 48 dp, media từ y = 88 |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_default_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_font-scale-2_light.png` | Như trên, cỡ chữ 2.0 |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png` | Như trên, khung `w360dp-h640dp-xxhdpi` |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenOverlay_countdown_light.png` | Vòng đếm ngược 48 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenOverlay_countdown_dark.png` | Như trên |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenOverlay_countdown_small.png` | Như trên, khung thấp |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenOverlay_loading_light.png` | Khung chờ với vòng 48 dp |
| Ghi lại | `core/ads/src/test/screenshots/NativeFullScreenOverlay_failed_light.png` | Khung chờ với nút đóng 48 dp |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `RemoteConfigKeysTest` (bước 7) |
| Sửa | `docs/remote-config-template.json` | Sinh lại bằng `RemoteConfigKeysTest` (bước 7) |
| Sửa | `docs/ops/ads-business-rules.md` | K-02, K-03, K-04, CL-03, mục 10.1, 11, dòng L-A10 của mục 12 (bước 7) |
| Sửa | `docs/ops/ads-guide.md` | Dòng 44, 55, 127, 141, câu về giá trị đã đặt trên Firebase, câu khuyến cáo chính sách nếu câu hỏi 3 được nhận (bước 7) |
| Sửa | `docs/ops/remote-config.md` | Khóa mang tên JBase (bước 7) |
| Sửa | `docs/features/ads/design/README.md` | Dòng `Native/FullScreen`, nút đóng 48 dp, dòng 84, 93, ghi chú ảnh thiết kế cũ, kết quả so sánh (bước 7) |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_light_compare.png` | Ảnh so sánh mới |
| Ghi lại | `docs/features/ads/design/review/NativeFullScreenAdScreen_default_dark_compare.png` | Ảnh so sánh mới |
| Sửa | `scripts/ads-plan.py` | `KNOWN_KEYS`: `time_close_native`, `time_expand_collap_ad` 10, chữ của `is_show_native_after_inter` (bước 7) |

Nơi khác đọc các file trên (L-020), đã tìm bằng `grep -rn` trong `scripts/`, `.github/`, `*/src/test`:

- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` so hai file với code; file được sinh lại nên test không đổi. `scripts/ads-plan.py` đọc `remote-config-keys.md` theo cột, không theo dòng cụ thể; `scripts/tests/run.sh` dựng repo giả với bảng khóa riêng (dòng 4487), không đọc bảng thật. `scripts/template-sync.sh` chỉ chép hai file khi đồng bộ app, không đọc nội dung.
- `scripts/ads-plan.py`: `scripts/tests/run.sh` (dòng 4454 tới 4560) kiểm vị trí, nhóm giãn cách và tên app trên repo giả, không kiểm chữ của `KNOWN_KEYS`.
- Ảnh chuẩn của mục 3.2: chỉ test screenshot của `:core:ads` ghi và so (`NativeFullScreenAdScreenScreenshotTest.kt`, `NativeFullScreenOverlayScreenshotTest.kt`); trang ở bước của luồng (`NativeFlowStep_*`, `AdGalleryScreen_flow-full-page_*`) dùng `TopBarHeight` không đổi nên ảnh không đổi.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json` còn được `app/build.gradle.kts` (dòng 120, 121) truyền đường dẫn cho `RemoteConfigKeysTest`; file build không đổi.
- `docs/ops/ads-business-rules.md`: các test sau chỉ nhắc tới luật trong chú thích (ví dụ "copied by hand" của MC-02, mục 8, S-xx), không đọc file và không chép mục 5.2, 5.8, 10.1, 11 hay 12 mà plan đổi, nên không bị ảnh hưởng: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`. Bảng khóa chung chép tay của mục 10.1 nằm ở `AdsSettingsTest.globalKeys_matchAdsKitTable`, đã có trong bảng trên.
- `AdsGlobalFlags` được dựng ở `AdsFlagsTest.kt`, `AdsController.kt` và `AdGalleryViewModelTest.kt` (đã có trong bảng); nơi khác dùng `copy(...)` nên không phải sửa.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, không bớt, không di chuyển vị trí. Đổi hành vi của vị trí toàn app `native_combo` và lượt native toàn màn hình độc lập (ví dụ `gallery_native_full_screen`): đếm ngược theo khóa mới `time_close_native`, nút 48 dp; lớp native thu gọn của thanh đáy: bung lại sau 10 giây theo mặc định. Khóa Remote Config mới `time_close_native` (long, giây, mặc định 5) đã có trong spec, mục 8 và KB-32.
- Sự kiện phân tích: Không chạm. Cơ hội `ad_opportunity` của combo (K-07) và mốc `ads_interval` (K-05) ghi lúc đóng như cũ.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code mới chỉ dùng Compose, coroutines và `RemoteKey` của `:core:config` đã có trên classpath của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Mọi phần lặp lại mẫu đã chạy (khóa của T-019, bộ điều phối của T-026, kích thước của T-027).

### 3.5. Hành vi đã có bị đổi

Từ `python3 -I scripts/existing-tests.py docs/features/ads/plans/T-053-dem-nguoc-va-bung-lai-theo-jbase.md` (2026-10-29). Test của nhóm "Cần xem trước" và "Nên xem" đã đọc; test không nêu dưới đây không khẳng định hành vi plan đổi (các test của `NativeFullScreenTurnsTest`, `NativeFullScreenOverlayTest` dùng mốc 3 000 ms chạy với port giả `countdownSeconds = 3L`, dữ liệu test theo tham số mới của port, ADR-0029, nên giữ nguyên câu).

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Số giây hiện tối đa là 3, mốc 3 000 ms | `countdownSecondsLeft(deadline, now, countdownSeconds)` giới hạn trên là `countdownSeconds` (bước 2) | `NativeAfterInterRulesTest.countdownSecondsLeft_roundsUp_andStopsAtZero` | Câu mới, `countdownSeconds = 5`, mốc 5 000: `now` 0 và 1 ra 5, 1 000 ra 4, 4 001 ra 1, 4 999 ra 1, 5 000 ra 0, 9 000 ra 0; giới hạn trên: `now = -500` ra 5. Thêm ca 30 giây: mốc 30 000, `now = 0` ra 30, 29 001 ra 1 |
| Khóa chung có 14 khóa, `time_expand_collap_ad` mặc định 30 | 15 khóa, thêm `time_close_native` 5; `time_expand_collap_ad` 10 (bước 1) | `AdsSettingsTest.globalKeys_matchAdsKitTable` | Câu mới: bảng mong đợi thêm `Triple("time_close_native", RemoteKey.Type.LONG, 5L)`, `Triple("time_expand_collap_ad", RemoteKey.Type.LONG, 10L)`, `assertEquals(15, expected.size)` |
| Mặc định của cờ chung: `expandCollapAfterSeconds = 30` | 10, cộng trường mới `closeNativeCountdownSeconds = 5` (bước 1, 3) | `AdsFlagsTest.global_usesTableDefaultsWhenNothingSet` | Câu mới: đối tượng mong đợi `expandCollapAfterSeconds = 10`, `closeNativeCountdownSeconds = 5` |
| Mọi giá trị từ xa được đọc | Thêm `time_close_native` | `AdsFlagsTest.global_followsEveryRemoteValue` | Câu mới: đặt thêm `settings.set("time_close_native", 12L)`, đối tượng mong đợi `closeNativeCountdownSeconds = 12` |
| Đối chứng mặc định trước khi đặt số âm: 30 | 10 | `AdsFlagsTest.negativeSeconds_areReadAsZero` | Câu mới: `assertEquals(10L, flags.global.first().expandCollapAfterSeconds)`; phần số âm ra 0 giữ nguyên |
| Lượt độc lập qua controller mở với 3 giây | 5 giây theo mặc định của khóa (bước 4, 5) | `AdsControllerNativeComboTest.standalone_noCurrentTurn_presenceUntilClose` | Câu mới: `assertEquals(5, controller.nativeFullScreenView.value!!.secondsLeft)` |
| Nút đóng của lượt độc lập dùng được sau 3 giây | Sau 5 giây | `AdsControllerNativeComboTest.standalone_withAd_throughShow_opensTurn` | Câu mới: `idleFor(4_999)` rồi `assertFalse(...closeEnabled)` (đối chứng), `idleFor(1)` rồi `assertTrue(...closeEnabled)`; chú thích "countdown of 5 s by default (K-03, `time_close_native`)" |
| Nút đóng vẽ 36 dp, vùng chạm nới tới 48 dp | Vẽ và chạm đúng 48 dp (bước 6) | `NativeFullScreenAdScreenTest.closeButton_drawn36dp_touchAtLeast48dp` | Đổi tên `closeButton_drawnAndTouch48dp`. Câu mới: rộng và cao 48 dp; chạm cách mép phải và mép dưới 2 dp ở phía trong thì gọi `onClose`; đối chứng: chạm cách mép phải 2 dp ở phía ngoài thì không gọi |
| Media của màn sau inter bắt đầu ở y = 76 | y = 24 + 48 + 16 = 88 | `NativeFullScreenAdScreenTest.media_startsAt76dp` | Đổi tên `media_startsAt88dp`; câu mới `assertEquals(88f, media.top.value, TOLERANCE)`, chú thích "top margin 24, close button bar 48, gap 16" |
| Media của trang ở bước của luồng ngang với màn sau inter | Trang bước của luồng vẫn 76 (hàng 36 dp), màn sau inter 88 (câu hỏi 2) | `NativeFullScreenAdScreenTest.flowStep_mediaStartsLikeAfterInter` | Ý định: trang của luồng giữ bố cục của màn sau inter, chỉ khác chiều cao hàng trên. Câu mới: `stepMedia.top` 76; `afterInterMedia.top` bằng `stepMedia.top + 12` (48 trừ 36); `left` bằng nhau |
| Đối chứng khoảng an toàn: màn sau inter ở 76 + thanh trạng thái | 88 + thanh trạng thái | `NativeFullScreenAdScreenTest.flowStep_noWindowInsetPadding` | Câu mới của phần đối chứng: `assertEquals(88f + STATUS_BAR.value, ...)`; phần trang của luồng (76) giữ nguyên |
| Vòng đếm ngược ở (16, 24, 52, 60) | (16, 24, 64, 72) | `NativeFullScreenOverlayTest.countdown_isCircle36dp_atTopLeft`, `closeEnabled_showsCloseButton_sameBounds_tapClosesOnce`, `loading_showsPlaceholderWithCountdown` | Đổi tên test đầu thành `countdown_isCircle48dp_atTopLeft`; ba câu `DpRect(16.dp, 24.dp, 52.dp, 60.dp)` thành `DpRect(16.dp, 24.dp, 64.dp, 72.dp)`. `countdown_leavesTopRightFree` giữ |
| Nút đóng 36 dp và media 595 dp ở mọi cỡ chữ | 48 dp; media 583 dp ở cỡ chữ 1.0 | `NativeAdFontScaleTest.fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales` | Câu mới: `close button width`, `height` là 48f; `media at the design size` là 583f; `left` 16, `top` 24 giữ |
| Ảnh chuẩn của màn native toàn màn hình | Nút và vòng 48 dp | 9 ảnh của mục 3.2 | Ghi lại, plan cho phép (mục 3.2) |
| Khóa chung không phải AdsKit kết thúc bằng câu "Name set by the template" | `time_close_native` kết thúc bằng `Name from JBase, not an AdsKit key.` | `AdsSettingsTest.keysNamedByTemplate_areMarked` | Giữ: câu mới vẫn chứa "not an AdsKit key" |
| CTA vừa chữ ở màn sau inter và trang của luồng | Media màn sau inter thấp hơn 12 dp, CTA giữ chỗ | `NativeAdFontScaleTest.fullScreen_ctaTextFits_fontScales` (nhóm Cần xem trước) | Giữ: test đo CTA trong khung, không đo media hay hàng trên; media co trước (O-08) |
| Rewarded ra nền lúc chờ quyết định | Không đổi | `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` (nhóm Cần xem trước, dùng `AdsController`, `AdsFlags`, `AdsSettings`) | Giữ: không đọc khóa nào plan đổi, không chạm native toàn màn hình |
| Chỉ đọc khóa đã khai báo | Thêm một khóa đọc | `AdsFlagsTest.readsOnlyDeclaredKeys` | Giữ: `time_close_native` có trong `AdsSettings.all()` |
| Bảng khóa sinh tự động khớp code | Hai dòng mới hay đổi | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` | Giữ: sinh lại hai file |

Những hành vi không đổi mà test có sẵn vẫn kiểm: chờ inter rời màn tối đa 2 giây (`NativeFullScreenTurnsTest.interNeverGone_opensAfter2s`, `NativeAfterInterRulesTest` phần `awaitInterGone`); nút quay lại (`NativeFullScreenOverlayTest.backPress_*`); mốc giữ qua lúc tạo lại Activity (`recreate_keepsDeadline_notRestarted`); luật bung lại với số giây cho sẵn (`CollapsibleReexpandTest`, `CollapsibleNativeAdTest` dùng 30 và 0 viết thẳng). Các test "không có combo" của `AdsControllerNativeComboTest` (`showStartupAd_blocked_neverOpensCombo`, `blockedAtGate_*`) chờ 5 000 ms rồi khẳng định không có màn native: với đếm ngược 5 giây, một combo mở nhầm ở 2 000 ms vẫn còn mở lúc đó, nên câu `assertNull(controller.nativeFullScreenView.value)` vẫn bắt được; giữ nguyên.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-32, tiêu chí 1 (nhận 1 tới 30, khác dùng 5) | Unit test | `NativeAfterInterRulesTest.nativeCloseCountdownSeconds_acceptsOneToThirty_elseFive`: 1 ra 1, 5 ra 5, 12 ra 12, 30 ra 30; 0, -1, 31, `Long.MAX_VALUE`, `Long.MIN_VALUE` ra 5 |
| KB-32, tiêu chí 1 (mặc định 5) | Unit test | `NativeAfterInterRulesTest.defaultCountdown_isFive_matchesKeyDefault`: `NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS == 5L` và `== AdsSettings.TIME_CLOSE_NATIVE.default`. Đối chứng L-055: khung rỗng có mặc định `0L`, test đỏ |
| KB-32, tiêu chí 1 (số giây hiện) | Unit test | `NativeAfterInterRulesTest.countdownSecondsLeft_roundsUp_andStopsAtZero` (câu mới ở mục 3.5) |
| KB-32, tiêu chí 1 (khóa) | Unit test | `AdsSettingsTest.globalKeys_matchAdsKitTable`: bảng 15 khóa chung (mục 3.5); `AdsSettingsTest.timeCloseNative_isJBaseName`: tên `time_close_native`, kiểu long, mặc định 5, mô tả kết thúc bằng `Name from JBase, not an AdsKit key.`; đối chứng: mô tả của `time_expand_collap_ad` vẫn kết thúc bằng `Name set by the template, not an AdsKit key.` |
| KB-32, tiêu chí 1 (đọc từ xa) | Unit test | `AdsFlagsTest.timeCloseNative_outsideOneToThirty_readsFive`: chưa đặt gì ra 5; đặt 12 ra 12 (đối chứng ghi rồi đọc lại, L-055); đặt 1 ra 1, 30 ra 30; đặt 0, 31, -3 ra 5. Cộng hai test cũ ở mục 3.5 |
| KB-32, tiêu chí 1 (độ dài của lượt combo) | Unit test | `NativeFullScreenTurnsTest.combo_countdownFromPort_fiveSeconds`: `port.countdownSeconds = 5`, inter rời màn ở 0; mở với `secondsLeft` 5; ở 4 999 `close()` không đóng, `secondsLeft` 1; ở 5 000 `closeEnabled`, `close()` đóng một lần |
| KB-32, tiêu chí 1 (30 giây) | Unit test | `NativeFullScreenTurnsTest.combo_countdownThirtySeconds`: `countdownSeconds = 30`; mở với 30; ở 29 999 còn 1 và chưa đóng được; ở 30 000 đóng được |
| Khóa đổi giữa lúc đếm (L-061) | Unit test | `NativeFullScreenTurnsTest.countdownChange_whileOpen_keepsDeadline_nextTurnUsesNew`: mở với 5; ở 1 000 đặt `port.countdownSeconds = 30`; ở 5 000 đóng được (mốc không đổi); đóng; lượt sau mở ở `t` với 30 và chưa đóng được ở `t + 5 000` |
| K-02, chờ inter rời màn 2 giây giữ nguyên | Unit test | `NativeFullScreenTurnsTest.interNeverGone_opensAfter2s_thenFullCountdown`: `countdownSeconds = 5`, inter không rời màn; ở 1 999 chưa có màn; ở 2 000 màn mở với 5; ở 6 999 chưa đóng được; ở 7 000 đóng được |
| KB-16, tiêu chí 3 (nạp lỗi tự đóng khi hết đếm ngược) | Unit test | `NativeFullScreenTurnsTest.loadFailed_autoClosesAtEndOfFiveSeconds`: `countdownSeconds = 5`, `ensureLoadingResult = false`; nút đóng dùng được ngay; ở 4 999 màn còn mở; ở 5 000 tự đóng, `onFinished` một lần |
| KB-5 (lượt độc lập cùng khóa) | Unit test | `NativeFullScreenTurnsTest.standalone_usesPortCountdown`: `countdownSeconds = 5`; mở ngay với 5; ở 4 999 chưa đóng được; ở 5 000 đóng được, mốc `ads_interval` của vị trí độc lập ở 5 000 |
| KB-32, tiêu chí 1 (nối controller với Remote Config) | Unit test (Robolectric) | `AdsControllerNativeComboTest.standalone_timeCloseNative_fromRemote`: `settings.set("time_close_native", 12L)` trước lượt độc lập: `secondsLeft` 12; đóng; đặt `0L`, lượt sau: 5 (đối chứng: giá trị ngoài khoảng). Cộng hai câu mới ở mục 3.5 (mặc định 5) |
| KB-32, tiêu chí 1 (48 dp, góc trên trái) | Compose UI test | `NativeFullScreenOverlayTest.countdown_isCircle48dp_atTopLeft`, `closeEnabled_showsCloseButton_sameBounds_tapClosesOnce`, `loading_showsPlaceholderWithCountdown` (khung (16, 24, 64, 72)); `NativeFullScreenAdScreenTest.closeButton_drawnAndTouch48dp`, `closeButton_isTopLeft_awayFromAdChoices` (giữ), `media_startsAt88dp`; `NativeAdFontScaleTest.fullScreenAfterInter_adBadgeKeepsTextHeight_fontScales` (48 dp ở mọi cỡ chữ) |
| Ra nền giữa lúc đếm ngược (L-091) | Compose UI test | `NativeFullScreenOverlayTest.background_duringCountdown_resumedPastDeadline_showsClose`: mở combo (port 3 giây), ở 1 000 đưa activity sang `CREATED`, tới 4 000 rồi `RESUMED`: có nút đóng, không còn vòng đếm, `finishes` vẫn 0 (màn không tự đóng khi có quảng cáo); bấm đóng thì `finishes` 1. Khi app ở nền lúc đếm, mốc tuyệt đối vẫn chạy trên đồng hồ đơn điệu, như khi tạo lại Activity |
| Tạo lại Activity giữa lúc đếm | Compose UI test | `NativeFullScreenOverlayTest.recreate_keepsDeadline_notRestarted` (giữ) |
| Trang ở bước của luồng không đổi | Compose UI test | `NativeFullScreenAdScreenTest.flowStep_mediaStartsLikeAfterInter`, `flowStep_noWindowInsetPadding` (câu mới ở mục 3.5); ảnh `NativeFlowStep_*` và `AdGalleryScreen_flow-full-page_*` không đổi |
| KB-32, tiêu chí 2, KB-4 tiêu chí 3 (mặc định 10) | Unit test | `AdsSettingsTest.globalKeys_matchAdsKitTable` (`time_expand_collap_ad` 10), `AdsFlagsTest.global_usesTableDefaultsWhenNothingSet` (10), đối chứng `global_followsEveryRemoteValue` (12) |
| KB-32, tiêu chí 2 (bung lại sau 10 giây trên màn) | Compose UI test | `CollapsibleNativeAdTest.collapsed_reexpandsAfterDefaultSeconds`: `setTimedHost(expandAfterSeconds = AdsSettings.TIME_EXPAND_COLLAP_AD.default)`, thu gọn ở `mark`; ở `mark + 9_999` còn thu gọn; ở `mark + 10_000` mở lại |
| KB-32, tiêu chí 2 (0 là không bung lại) | Compose UI test, unit test | `CollapsibleNativeAdTest.collapsed_zeroSeconds_neverReexpands`, `CollapsibleReexpandTest` (giữ) |
| Bảng khóa sinh tự động | Unit test | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` (giữ, hai file sinh lại) |
| Giao diện màn native sau inter | Screenshot test | `NativeFullScreenAdScreenScreenshotTest` và `NativeFullScreenOverlayScreenshotTest`: ghi lại 9 ảnh của mục 3.2, cùng tên |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `NativeFullScreenAdScreen_max-content_small.png`, `NativeFullScreenOverlay_countdown_small.png`; `NativeFullScreenAdScreenTest.smallScreen_closeAndCtaInside`, `NativeFullScreenOverlayTest.smallScreen_countdownAndCtaInside` (giữ) |
| Đúng thiết kế (khi có `design/screens/`) | Screenshot test cùng tên với ảnh thiết kế, ảnh so sánh bằng `scripts/design-compare/DesignCompare.java` cho `core/ads` (script so sánh của feature không tìm được ảnh của `:core:ads`, nhật ký T-026) | `NativeFullScreenAdScreen_default_light.png`, `NativeFullScreenAdScreen_default_dark.png`. Ảnh thiết kế còn vẽ 36 dp: phần lệch ở hàng trên và media (12 dp) là chủ đích theo ADR-0038, ghi trong README thiết kế |
| | | Ảnh thiết kế của ticket khác (T-014, T-030, T-036): `AdGalleryScreen_default_light`, `AdGalleryScreen_default_dark`, `AdGalleryScreen_inlist_light`, `AdGalleryScreen_collapsible-expanded_light`, `AdGalleryScreen_collapsible-collapsed_light` |
| | | Ảnh thiết kế của ticket khác (T-027, T-030): `SampleScreen_ready_light`, `SampleScreen_ready_dark`, `SampleScreen_ad-loading_light` |
| Combo với quảng cáo thật | Kiểm tra thủ công (máy thật: quảng cáo thật) | Bản staging (combo bật sẵn), đồng ý quảng cáo, màn xem thử, bấm Interstitial: đóng inter thì có native toàn màn hình, vòng 5, 4, 3, 2, 1 ở góc trên trái, to hơn bản cũ, AdChoices góc trên phải; hết đếm thì X; nút quay lại không làm gì trong lúc đếm |
| `time_close_native` đổi từ xa | Kiểm tra thủ công (máy thật: Remote Config) | Trên Firebase của staging đặt `time_close_native` = 10, mở lại app: vòng bắt đầu ở 10. Đặt 0 hay 45: vòng bắt đầu ở 5. Log `adb logcat -v threadtime \| grep ' VGL\.'` có `Native combo opened` |
| Bung lại 10 giây | Kiểm tra thủ công (máy thật: quảng cáo thật) | Màn mẫu có thanh đáy, `is_show_native_bottom` bật, chưa đặt `time_expand_collap_ad`: thu gọn lớp native, khoảng 10 giây sau lớp mở lại |
| Ảnh so sánh với thiết kế | Kiểm tra thủ công (người: xem ảnh so sánh và quyết câu hỏi 1) | Mở `design/review/NativeFullScreenAdScreen_default_*_compare.png`: chỉ hàng trên (nút 48 dp) và media (thấp hơn 12 dp) khác thiết kế |

**Ngoại lệ viết test trước:** tài liệu (`ads-business-rules.md`, `ads-guide.md`, `remote-config.md`, README thiết kế, `review/`), KDoc ở `AdsController.kt`, `NativeFullScreenAdHost.kt`, `NativeFullScreenAdScreen.kt`, chữ trong `KNOWN_KEYS` của `scripts/ads-plan.py` (văn bản cho người vận hành, không có test kiểm chữ), và hai file sinh tự động (sinh lại bằng chính test của chúng). Ảnh chuẩn ghi sau khi code xong, như mọi task giao diện.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi hành vi của native toàn màn hình và lớp thu gọn, thêm khóa Remote Config quảng cáo), 7 (module core `:core:ads`). Mức Cao, không tính tổng. Nhóm hiện có một dev: anh Nhật vừa duyệt vừa duyệt chéo theo ngoại lệ của `risk-scoring.md`, mục 3; người duyệt ghi lý do ở mục 9. Anh Nhật cũng là người phụ trách sản phẩm và đã chọn thay đổi này (ADR-0038), nên điều kiện "người phụ trách sản phẩm biết trước khi duyệt" đã có.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một module code (`:core:ads`, 7 file), đổi giao diện công khai (`AdsGlobalFlags` thêm trường, `AdsSettings.TIME_CLOSE_NATIVE`); thêm một test dữ liệu ở `:app`, hai file sinh, 9 ảnh chuẩn, năm tài liệu và một script |
| B. Độ rõ của yêu cầu | 2 | Spec đã xác nhận, số đo rõ (5 giây, 1 tới 30, 48 dp, 10 giây). Còn câu hỏi ảnh hưởng tới cách làm: ảnh thiết kế còn 36 dp (câu 1), hàng trên của trang ở bước của luồng (câu 2) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có test JVM với đồng hồ giả và test Compose đo kích thước; còn lại kiểm bằng mắt trên máy thật với quảng cáo thật và Remote Config |
| D. Ảnh hưởng chéo | 2 | Sửa `AdsController.kt`, `AdsSettings.kt`, `AdsFlags.kt`, `ads-business-rules.md` mà T-054, T-065, T-066, T-068 cũng sửa (mục 6) |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu đã có: khóa của T-019, bộ điều phối và đồng hồ của T-026, kích thước của T-027 |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn 1 và 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsSettings` và `AdsGlobalFlags` (public, `:app` đọc qua `AppRemoteKeys`), `AdsController` (port của `NativeFullScreenTurns`), `NativeFullScreenAdScreen` (màn sau inter và khung chờ; trang ở bước của luồng không đổi), bảng khóa sinh tự động, `ads-business-rules.md` mục 10, 11.
- Task khác trong sprint có thể xung đột:
  - T-066 (`CACHED_EARLY` luôn bật): sửa mô tả `IS_ENABLE_CONSENT_SHORTCUT` trong `AdsSettings.kt`, mục 10.1 của `ads-business-rules.md` và hai file sinh. Cùng file, khác dòng; task sau rebase và sinh lại hai file.
  - T-068 (SDK MAX, plan trên nhánh `claude/plan-T-068-sdk-max`, bước 8): cũng thêm một khóa chung vào cuối `combine` của `DefaultAdsFlags` (chỉ số 14) và đổi `AdsSettingsTest.globalKeys_matchAdsKitTable` thành `assertEquals(15, ...)`. Luật: task chạy sau lấy chỉ số kế tiếp (15, rồi 16) và cộng thêm một vào số khóa của test (16, rồi 17), thêm `Triple` của mình vào bảng mong đợi; đó là dữ liệu test theo khóa mà plan kia thêm (ADR-0029), không phải viết lại câu khẳng định. Các câu viết sẵn của plan này ("chỉ số 14", `assertEquals(15, expected.size)`) đúng khi T-053 chạy trước T-068; chạy sau thì theo luật trên.
  - T-065 (kích thước native theo JBase): sửa mục 11 của `ads-business-rules.md` (dòng chiều cao) và có thể `NativeAdCard.kt`; không chạm `NativeFullScreenAdScreen.kt` nếu giữ phạm vi ticket.
  - T-054 (dừng quảng cáo khi bị che): sửa `AdsController.kt` và mục 12 của luật; khác hàm.
  - T-055 (hiện toàn màn hình an toàn): sửa `AdsController.kt` ở đường hiện inter, app open, rewarded; không chạm `NativeFullScreenTurns`.
  - T-064 (vẽ lại trên Figma, `auto_run: false`): nếu người duyệt nhận câu hỏi 1, frame `Native/FullScreen` biến thể `After interstitial` vẽ lại ở T-064 và ảnh `NativeFullScreenAdScreen_default_*` trong `design/screens/` thay sau task này.
- Đề xuất thứ tự chạy: không phụ thuộc task nào. Thứ tự dự định: T-053, rồi T-066, rồi T-068; T-065 cũng sau T-053. Task này thêm dòng vào bảng khóa, `combine` và mục 11; các task sau rebase và lấy chỉ số kế tiếp như trên.

## 7. Điều kiện dừng riêng

- Bảng khóa sinh tự động (`docs/remote-config-keys.md`, `docs/remote-config-template.json`) đổi ở dòng khác ngoài `time_close_native` và `time_expand_collap_ad`: dừng.
- Muốn đổi phần chờ inter rời màn (2 giây, kiểm mỗi 200 ms) hay ý nghĩa "0 là không bung lại" để test qua: dừng (ADR-0038 giữ hai luật này).
- Đổi 48 dp làm đổi ảnh chuẩn của trang ở bước của luồng (`NativeFlowStep_*`, `AdGalleryScreen_flow-full-page_*`): dừng, vì câu hỏi 2 chưa được trả lời theo hướng đó.

- Điều kiện dừng 6 (phần việc lớn hơn rõ rệt so với ước lượng) đo theo mục 3.2 của plan này và cỡ M của ticket (đổi từ S theo rà soát độc lập, 2026-10-29), không theo cỡ S cũ.

## 8. Câu hỏi cho người duyệt

1. **Ảnh thiết kế `NativeFullScreenAdScreen_default_light`, `_dark` còn vẽ nút đóng 36 dp tại (16, 24), media từ y = 76.** Spec đã xác nhận (KB-32) và ADR-0038 chọn 48 dp, nên theo ADR-0038, quyết định 1, quyết định của người phụ trách sản phẩm đứng trên ảnh. ADR-0020 không cho viết cứng để giảm khác biệt, và `design-workflow.md` không có luật cho ảnh thiết kế cũ hơn spec.
   - Đề xuất của AI: task này code 48 dp, ghi ảnh so sánh và ghi chú ở README thiết kế; thêm biến thể `After interstitial` của `Native/FullScreen` (48 dp, media từ y = 88, hai ảnh sáng tối) vào phạm vi của T-064, phiên Figma có người theo dõi; ảnh mới thay vào `design/screens/` khi T-064 xong, không cần đổi code.
   - Phương án khác: dừng plan này tới khi T-064 vẽ lại.
2. **Hàng trên của trang ở bước của luồng.** Hôm nay trang này giữ hàng trên 36 dp chỉ để media bắt đầu ngang màn sau inter (y = 76; test `flowStep_mediaStartsLikeAfterInter`, README thiết kế dòng 84). Với nút 48 dp, hai trang không còn ngang nhau.
   - Đề xuất của AI: giữ trang ở bước của luồng như cũ (36 dp, y = 76): ticket không nói tới nó, trang không có nút đóng, và giữ được 7 ảnh chuẩn cùng phép tính của T-049. Màn sau inter có hàng 48 dp, media từ y = 88.
   - Phương án khác: cả hai trang dùng 48 dp; thêm ảnh chuẩn `NativeFlowStep_full-page_*`, `AdGalleryScreen_flow-full-page_*` vào mục 3.2 và kiểm lại phép tính hàng gọn của T-049.
3. **Rủi ro chính sách.** ADR-0038 ghi đếm ngược dài hơn 2 giây làm tăng rủi ro chính sách của quảng cáo toàn màn hình khó đóng; khóa cho phép tới 30 giây. Code không chặn thêm. Đề xuất của AI: ghi vào `ads-guide.md`, mục phát hành, rằng người vận hành chỉ nên đặt quá 5 giây khi đã kiểm chính sách của AdMob; không thêm giới hạn nào khác ngoài 1 tới 30 của JBase. Câu này đã có trong mục 3.1, bước 7 và mục 3.2 (`ads-guide.md`), chỉ làm khi người duyệt nhận.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30 (lời anh Nhật: "Duyệt các plan."), cùng 11 plan khác của đợt 4 quảng cáo, sau khi xem bảng tóm tắt plan và câu hỏi AI gửi trong phiên. Mọi câu ở mục 8 theo đề xuất của AI. Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Trả lời: (1) code 48 dp, ghi ảnh so sánh và ghi chú ở README thiết kế; biến thể `After interstitial` của `Native/FullScreen` vào phạm vi T-064 (ticket T-064 đã ghi, PR #313). (2) trang ở bước của luồng giữ hàng trên 36 dp, media từ y = 76. (3) nhận câu ghi `ads-guide.md`: chỉ đặt `time_close_native` quá 5 giây khi đã kiểm chính sách của AdMob.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
