---
ticket: T-084
title: Test của :core:ads vẽ trong theme cố định của test, ảnh chuẩn không đổi theo theme của app
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 22.3M
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-084: TEST CỦA :CORE:ADS VẼ TRONG THEME CỐ ĐỊNH CỦA TEST

## 1. Tóm tắt

Mọi test của `:core:ads` thôi gọi `AppTheme` của `:core:designsystem` và vẽ trong `AdsTestTheme`, một theme khai báo trong mã test của module: bảng màu sáng và tối, thang chữ và phông Inter chép đúng giá trị hiện tại của dự án mẫu. Hai test của `NativeFlowStep` thay `PrimaryButton` bằng một nút dựng trong test. Lần chạy thử (mục 4.1) cho kết quả: ghi lại cả 43 ảnh chuẩn của `:core:ads` bằng theme của test thì giống bản trong repo từng byte, 1338 test qua; đổi màu và cỡ chữ của `AppTheme` thì ảnh và test của `:core:ads` vẫn qua, chỉ test so theme (mới, chỉ chạy ở repo mẫu) đỏ.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-143 (issue #321 từ Photune, kèm bình luận bằng chứng của Tuấn).

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | Không test nào của `:core:ads` gọi `AppTheme`; mọi test dùng theme của test | 21 tệp test gọi `AppTheme` (117 chỗ gọi) chuyển sang `AdsTestTheme`. Chú thích KDoc của `FontScaleTestSupport.kt` nhắc `AppTheme` cũng đổi. Lệnh kiểm: lệnh 1 của mục 4 (chỉ `AdsTestThemeMatchTest.kt` còn gọi `AppTheme`, để so) |
| Định nghĩa hoàn thành 2 | Ảnh chuẩn không đổi, hoặc plan liệt kê ảnh đổi | Lần thử: 0 ảnh đổi ở mức byte (mục 4.1). Plan không ghi lại ảnh nào |
| Định nghĩa hoàn thành 3 | Đổi màu của `AppTheme` trong bản thử thì ảnh của `:core:ads` vẫn khớp | Lần thử đã làm (mục 4.1, kết quả 4); phiên làm lại bằng lệnh 3 của mục 4 và ghi vào nhật ký |
| Định nghĩa hoàn thành 4 | Có test báo khi theme của test lệch `AppTheme` của mẫu, chỉ chạy ở repo mẫu | `AdsTestThemeMatchTest`, bỏ qua (JUnit `assume`) khi repo không có `scripts/new-app.sh`. `new-app.sh` tự xóa tệp đó trong app (dòng `rm -f scripts/new-app.sh ...`), cùng cách `scripts/tests/run.sh` nhận ra repo mẫu |
| Định nghĩa hoàn thành 5 | `docs/ops/to-app.md` cập nhật | Mục 2: ảnh chuẩn và test của `:core:ads` không đổi theo theme của app; điều còn phụ thuộc (`Spacing`) |
| Phạm vi: `Spacing`, `PrimaryButton`, phông | Plan quyết, có lý do | Mục 3.1, quyết định 2, 3, 4 |
| Bình luận của Tuấn trên #321 | 13 test JVM không chụp ảnh của `:core:ads` đỏ khi app đổi thang chữ (cỡ, chiều cao dòng) | Các test đó nằm trong 21 tệp trên (`NativeAdCardTest`, `NativeAdFontScaleTest`, ...), nên cũng vẽ trong theme của test. `AdGalleryScreenTest` của `:app` nằm ngoài phạm vi ticket (mục 8, câu 1) |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0008 (`:core:designsystem` là bản gốc; quy tắc "không viết cứng mã màu" áp cho mã của feature, không áp cho theme cố định trong mã test), ADR-0011 (ảnh chuẩn không cập nhật ngoài plan), ADR-0020 (screenshot test), ADR-0030 (đồng bộ mẫu về app, `TEMPLATE_OWNED`).

## 3. Giải pháp

### 3.1. Cách làm

**Quyết định 1. Theme của test.** Tệp mới `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestTheme.kt`:

- `@Composable internal fun AdsTestTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)`: gọi `MaterialTheme(colorScheme = if (darkTheme) AdsTestDarkColors else AdsTestLightColors, typography = AdsTestTypography, content = content)`. Mặc định của `darkTheme` và việc không truyền `shapes` giống hệt `AppTheme`, nên 96 chỗ gọi `AppTheme { }` đổi tên mà không đổi nghĩa.
- `AdsTestLightColors`, `AdsTestDarkColors` (`private`): chép nguyên `lightColorScheme(...)` và `darkColorScheme(...)` của `core/designsystem/.../theme/Color.kt` ở commit gốc của nhánh, đủ 35 vai màu mỗi bảng, thay `BrandPrimary` bằng `Color(0xFF147D68)` và `BrandPrimaryDark` bằng `Color(0xFF84DCC1)`.
- `AdsTestTypography` (`private`): chép `AppTypography` của `Type.kt` (15 kiểu chữ của Material 3, `letterSpacing = 0.sp`, `titleMedium` đậm `SemiBold`), thay `InterFamily` bằng `AdsTestFontFamily`.
- `AdsTestFontFamily` (`private`): ba `Font(File, FontWeight)` (Normal, Medium, SemiBold) từ ba tệp `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` đặt ở `core/ads/src/test/resources/ads-test-fonts/`, đọc qua `classLoader.getResource("ads-test-fonts/<tên>")` rồi `File(url.toURI())`.
- KDoc của tệp nói: theme cố định của test, giá trị của dự án mẫu ở thời điểm T-084; app không sửa (`:core:ads` thuộc phần của mẫu); dự án mẫu đổi `AppTheme` thì đổi theo, `AdsTestThemeMatchTest` báo khi lệch.

**Quyết định 2. Phông chữ: chép Inter vào tài nguyên test của `:core:ads`.** Lý do:

- Dùng `R.font.inter_*` của `:core:designsystem` thì theme của test vẫn phụ thuộc app: app xóa hay thay phông Inter (`docs/ops/new-app.md`, mục 3, bước 2) làm test của `:core:ads` không biên dịch được hoặc đổi ảnh.
- Phông mặc định của Robolectric thì chữ của mọi ảnh có chữ đổi (43 ảnh đều có chữ), plan phải ghi lại toàn bộ ảnh chuẩn, và ảnh không còn giống preview của app mẫu. Theo bình luận của Tuấn, test đo kích thước không đổi theo phông, nên cách này chỉ tốn ở ảnh.
- Phông đặt ở tài nguyên Java của test (`src/test/resources`), không vào APK nào. Ba tệp nặng tổng 1,2 MB trong git. Giấy phép SIL OFL 1.1 cho phép chép kèm giấy phép: `core/ads/src/test/resources/ads-test-fonts/LICENSE.txt` chép từ `core/designsystem/third_party/inter/LICENSE.txt`.
- Thư mục phải tên khác `fonts/`: lần thử đầu dùng `src/test/resources/fonts/` làm 237 trên 1338 test của `:core:ads` đỏ, kể cả test không dùng theme (`BannerSlotHostTest`). Thư mục `fonts/` trên classpath test che bộ phông hệ thống của Robolectric: Robolectric nạp bảng phông hệ thống lúc dựng môi trường (`Typeface.loadPreinstalledSystemFontMap`) và gặp `NullPointerException ... "family" is null` (mục 4.1, kết quả 2). Tên `ads-test-fonts/` không gặp lỗi này.

**Quyết định 3. `PrimaryButton`: thay bằng nút dựng trong test.** `NativeFlowStepScreenshotTest` (8 ảnh `NativeFlowStep_*`) và hai test `nextButton_samePlace_notCovered_withFullPage`, `nextButton_samePlace_notCovered_withCard` của `NativeFlowStepTest` dùng `PrimaryButton` làm nút "Next" của app. `PrimaryButton` thuộc `:core:designsystem`, app sửa được (bo góc, chiều cao, chữ), nên ảnh và câu `assertEquals(52f, ...)` đổi theo app. Nút trong test đại diện "nút của một app", không cần giống nút của app nào. Thêm vào `AdsTestTheme.kt`: `@Composable internal fun AdsTestNextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)`, dựng như `PrimaryButton` hiện tại ở trạng thái thường: `Button(onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.heightIn(min = 52.dp)) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) }`. Lần thử: 8 ảnh `NativeFlowStep_*` giống từng byte, hai test qua với câu khẳng định giữ nguyên (52 dp).

**Quyết định 4. `Spacing`: giữ, ghi rõ trong `to-app.md`.** 9 tệp mã chính của `:core:ads` dùng `Spacing` (37 chỗ: `xs` 7, `sm` 14, `md` 15, `lg` 5, `xl` 2), và 7 tệp test dùng nó để dựng khung hay so vị trí với mã chính (ví dụ `withAd.top - Spacing.md` trong `NativeFlowStepTest`). Theme của test không chặn được phần của mã chính. Lý do giữ:

- `Spacing` là lưới 4 dp. `docs/ops/new-app.md` mục 3 (thương hiệu) bảo app thay màu, logo, khẩu hiệu, phông; không bảo thay `Spacing`. Photune (issue #321) đỏ vì màu và thang chữ, không vì `Spacing`.
- Thay bằng hằng riêng trong mã chính của `:core:ads` là sửa 9 tệp mã chính, trái `CLAUDE.md` ("khoảng cách lấy từ `:core:designsystem`") và đổi hành vi của quảng cáo thật trong app, trong khi ticket chỉ đòi test không phụ thuộc theme.
- Test giữ `Spacing` để so vị trí với mã chính: app đổi `Spacing` thì mã chính đổi theo, test vẫn đúng ý.
- `to-app.md` mục 2 ghi: app đổi giá trị `Spacing` thì quảng cáo trong app cũng đổi khoảng cách, và ảnh của `:core:ads` phải ghi lại như một ngoại lệ riêng của app (ADR của app). Người duyệt muốn khác thì trả lời mục 8, câu 2.

**Quyết định 5. Test so theme, chỉ ở repo mẫu.** Tệp mới `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestThemeMatchTest.kt` (Robolectric, `createComposeRule()`; lớp này không chụp ảnh nên quy tắc cửa sổ của L-048 không áp):

- `@Before`: tìm gốc repo bằng cách đi lên từ thư mục làm việc tới thư mục có `settings.gradle.kts`; `assumeTrue(File(root, "scripts/new-app.sh").isFile)`. Ở app, cả lớp được báo là bỏ qua, không đỏ.
- Mỗi test dựng một lần `setContent { AppTheme(darkTheme = d) { lấy MaterialTheme.colorScheme, typography, shapes }; AdsTestTheme(darkTheme = d) { lấy như trên } }`. `AppTheme` và các bảng màu của nó là `internal` ở `:core:designsystem`, nên test đọc giá trị qua `MaterialTheme`, không cần sửa mã chính của module đó.
- So bảng màu bằng `ColorScheme.toString()` (in đủ mọi vai màu); so 15 kiểu chữ sau khi bỏ `fontFamily` (`style.copy(fontFamily = null)`), vì hai họ phông là hai đối tượng khác nhau; so `shapes`; so phông bằng nội dung tệp: ba tệp `ads-test-fonts/inter_*.ttf` giống từng byte ba tệp `core/designsystem/src/main/res/font/inter_*.ttf`.

**Quyết định 6. Đổi tệp test.** Trong 21 tệp: bỏ dòng `import com.vglstudio.template.core.designsystem.theme.AppTheme`, thay `AppTheme {` và `AppTheme(` bằng `AdsTestTheme {` và `AdsTestTheme(`; trong hai tệp `NativeFlowStep*` bỏ thêm `import ...component.PrimaryButton` và thay `PrimaryButton(` bằng `AdsTestNextButton(`. Không đổi câu khẳng định, khung, đường dẫn ảnh hay dữ liệu nào. Lần thử làm đúng việc này bằng `sed` rồi chạy ktlint qua `scripts/check.sh` (mục 4.1): tên mới dài hơn 4 ký tự, nên dòng `AdsTestTheme { NativeAdCard(... ctaLarge = ctaLarge) }` của `NativeAdFontScaleTest.kt` (khoảng dòng 271) dài 123 ký tự và ktlint báo lỗi; tách khối lambda thành ba dòng thì qua. Các dòng khác có tên mới đều dưới 121 ký tự.

**Quyết định 7. Không đổi:** hàm `@Preview` trong mã chính (`NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `NativeFullScreenAdScreen.kt`) vẫn dùng `AppTheme`; ảnh và test của `:app` (`AdGalleryScreen*`); `:feature:loading`, `:feature:sample`; không ghi lại ảnh chuẩn nào.

**`to-app.md`, mục 2:** thêm một đoạn dưới bảng: "Test và ảnh chuẩn của `:core:ads` vẽ trong theme cố định của test (`AdsTestTheme`, chép giá trị của dự án mẫu, phông Inter riêng trong `src/test/resources/ads-test-fonts/`), không theo `AppTheme` của app (T-084, L-143). App đổi màu, chữ hay phông trong `:core:designsystem` không cần ghi lại ảnh của `:core:ads`; xung đột ở các ảnh đó lấy bản của mẫu như phần còn lại của module. Còn phụ thuộc: `Spacing` (mã chính của `:core:ads` dùng). App đổi giá trị `Spacing` thì ghi lại ảnh của `:core:ads` và ghi ngoại lệ trong ADR của app. Ảnh của `:app` có quảng cáo (`AdGalleryScreen_*`) vẫn theo theme của app."

**Ticket:** phiên điền trạng thái theo plan (`scripts/status.py --sync`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestTheme.kt` | `AdsTestTheme`, bảng màu, thang chữ, họ phông Inter của test, `AdsTestNextButton` (quyết định 1, 2, 3) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestThemeMatchTest.kt` | Test so theme của test với `AppTheme`, chỉ chạy ở repo mẫu (quyết định 5) |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_regular.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_regular.ttf` |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_medium.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_medium.ttf` |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_semibold.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_semibold.ttf` |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/LICENSE.txt` | Chép từ `core/designsystem/third_party/inter/LICENSE.txt` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarScreenshotTest.kt` | `AppTheme` thành `AdsTestTheme` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdScreenshotTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/CollapsibleNativeAdTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/InListAdsTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCellTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdFontScaleTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdSlotTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdViewBindingTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepLayoutTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepScreenshotTest.kt` | Như trên; `PrimaryButton` thành `AdsTestNextButton` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFlowStepTest.kt` | Như trên; `PrimaryButton` thành `AdsTestNextButton` (hai test) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenScreenshotTest.kt` | `AppTheme` thành `AdsTestTheme` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenAdScreenTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenStepRecreateTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedActionTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/RewardedAdDialogScreenshotTest.kt` | Như trên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FontScaleTestSupport.kt` | Chỉ KDoc: "Put it outside `AdsTestTheme`" thay cho `AppTheme` |
| Sửa | `docs/ops/to-app.md` | Mục 2: đoạn mới ở mục 3.1 |
| Sửa | `docs/features/quy-trinh/tickets/T-084-theme-co-dinh-cho-test-core-ads.md` | Trạng thái theo plan (`scripts/status.py --sync`) |

Không ghi lại ảnh chuẩn nào: 43 ảnh trong `core/ads/src/test/screenshots/` giữ nguyên từng byte (mục 4.1). Không sửa mã Kotlin ngoài thư mục test.

Nơi đọc các file plan đổi (L-020; `grep -rn` trong `scripts/`, `.github/`, `*/src/test`, 2026-11-01):
- Tệp test của `:core:ads`: chỉ Gradle biên dịch; `scripts/check.sh` đọc lệnh `captureRoboImage` trong `src/test` (đường dẫn ảnh không đổi). `scripts/existing-tests.py` và `scripts/ads-plan.py` không đọc tệp test.
- `docs/ops/to-app.md`: `scripts/template-sync.sh` và `scripts/tests/run.sh` không đọc nội dung mục 2 (chỉ dẫn tên tệp trong chú thích và thông báo). Bảng mục 2 không đổi, nên `TEMPLATE_OWNED` không cần đổi (mục 7 của `to-app.md`).
- `core/designsystem/src/main/res/font/*.ttf` và `third_party/inter/LICENSE.txt`: chỉ được đọc, không sửa. Tệp mới `ads-test-fonts/` không trùng tên thư mục `fonts/` mà Robolectric dùng (quyết định 2).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm hành vi quảng cáo; chỉ đổi theme mà test của `:core:ads` vẽ
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `Font(File, FontWeight)` (gói `androidx.compose.ui.text.font`) đến từ `androidx-compose-ui` của catalog, đã có trên classpath của `:core:ads` qua plugin `template.android.compose`; `Type.kt` của `:core:designsystem` dùng cùng gói; `createComposeRule`, `assumeTrue` đã có (JUnit 4, `ui-test-junit4`)

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** CI Linux nạp phông từ tệp (`Font(File)`) và vẽ ra ảnh khác máy dev macOS (lần thử chỉ chạy trên macOS). Ảnh hiện có được ghi bằng `R.font` (cùng tệp phông), nên khác biệt nếu có là do đường nạp, không do phông.
- **Điều kiện chuyển:** `pr-check` đỏ ở ảnh của `:core:ads` mà `scripts/check.sh` trên máy dev qua, và ảnh `_compare.png` trong artifact `build-and-test-reports` cho thấy lệch ở chữ.
- **Cách làm thay:** theme của test dùng `Font(R.font.inter_*)` của `:core:designsystem` cho tới khi có cách nạp phông riêng chạy được trên CI, chỉ thay phần `AdsTestFontFamily` trong `AdsTestTheme.kt`; màu, thang chữ, nút và test so theme giữ nguyên. Phần phông khi đó còn phụ thuộc app: ghi vào `to-app.md` cùng chỗ với `Spacing`, và ghi vào nhật ký để người quyết ticket tiếp theo. Tiêu chí 1, 2, 4, 5 vẫn đạt; tiêu chí 3 (đổi màu) vẫn đạt.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa mã Kotlin ngoài thư mục test (`scripts/existing-tests.py` in "Không có tên nào để tìm"; mục 4.1, kết quả 7). Các test có sẵn của `:core:ads` giữ nguyên mọi câu khẳng định; chỉ đổi composable bọc ngoài (`AppTheme` thành `AdsTestTheme`, `PrimaryButton` thành `AdsTestNextButton`) có cùng giá trị.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Định nghĩa hoàn thành 4: theme của test khớp `AppTheme` (sáng) | Compose UI test (Robolectric) | `AdsTestThemeMatchTest.lightTheme_matchesAppTheme`: `ColorScheme.toString()` bằng nhau, 15 kiểu chữ bằng nhau sau khi bỏ `fontFamily`, `shapes` bằng nhau |
| Định nghĩa hoàn thành 4: theme của test khớp `AppTheme` (tối) | Compose UI test (Robolectric) | `AdsTestThemeMatchTest.darkTheme_matchesAppTheme`: như trên với `darkTheme = true` |
| Định nghĩa hoàn thành 4: phông của test là phông của mẫu | Unit test (Robolectric) | `AdsTestThemeMatchTest.fonts_matchDesignSystemFonts`: ba tệp `ads-test-fonts/inter_*.ttf` giống từng byte ba tệp `core/designsystem/src/main/res/font/inter_*.ttf` |
| Định nghĩa hoàn thành 4: chỉ chạy ở repo mẫu | Lệnh, không commit | Lệnh 4 của khối dưới bảng: đổi tên tạm `scripts/new-app.sh`, chạy `AdsTestThemeMatchTest`, báo cáo XML có 3 test `skipped`, 0 lỗi; trả tên tệp |
| Định nghĩa hoàn thành 4: test so theme bắt được lệch | Đối chứng, không commit | Lệnh 3: đổi màu `BrandPrimary`, `background` sáng và cỡ `labelLarge` của `AppTheme`; `lightTheme_matchesAppTheme` đỏ |
| Định nghĩa hoàn thành 1 | Lệnh | Lệnh 1: ngoài `AdsTestThemeMatchTest.kt` (phải gọi `AppTheme` để so), không tệp test nào của `:core:ads` nhắc `AppTheme` hay `PrimaryButton` |
| Định nghĩa hoàn thành 2: ảnh chuẩn không đổi | Lệnh đo, không commit | Lệnh 2: ghi lại ảnh của `:core:ads` bằng theme của test; `git status --short -- '*/src/test/screenshots/*'` in rỗng (0 ảnh đổi ở mức byte) |
| Định nghĩa hoàn thành 3: đổi theme của app, ảnh của `:core:ads` vẫn khớp | Lệnh đo, không commit | Lệnh 3: sau khi đổi `AppTheme` như trên, `:core:ads:verifyRoborazziDebug` chỉ có test của `AdsTestThemeMatchTest` đỏ; 43 ảnh và mọi test khác qua; trả `Color.kt`, `Type.kt` về |
| Mọi test qua, không ảnh nào đổi | Cổng chất lượng | `scripts/check.sh`: `== Cổng chất lượng: QUA`; số test JVM không ít hơn trước cộng 3 (test mới); `git status --short -- '*/src/test/screenshots/*'` in rỗng |
| `to-app.md` đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc đoạn mới ở mục 2 |
| CI qua | Kiểm tra thủ công (người: GitHub Actions) | `pr-check` của PR xanh, gồm so ảnh của `:core:ads` trên Linux (mục 3.4) |

Lệnh của mục 4 (chạy ở gốc repo; pathspec ảnh luôn có `*` ở cuối, L-136):

```sh
# 1. No test of :core:ads names AppTheme or PrimaryButton, except the match test (prints nothing).
grep -rln 'AppTheme\|PrimaryButton' core/ads/src/test | grep -v '/AdsTestThemeMatchTest\.kt$'
# 2. Record the images of :core:ads with the test theme; no image may change. Put them back afterwards.
./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun
git status --short -- '*/src/test/screenshots/*'
git checkout -- '*/src/test/screenshots/*'
# 3. Change the theme of the app (not committed): images and tests of :core:ads still pass, only the match test fails.
sed -i.bak 's/private val BrandPrimary = Color(0xFF147D68)/private val BrandPrimary = Color(0xFFC0392B)/; s/    background = Color(0xFFF7FAF8),/    background = Color(0xFFFFF3E0),/' \
  core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/Color.kt
sed -i.bak 's/labelLarge = labelLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),/labelLarge = labelLarge.copy(fontFamily = InterFamily, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),/' \
  core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/Type.kt
./gradlew :core:ads:verifyRoborazziDebug --continue; echo "exit $? (must be 1)"
grep -l '<failure' core/ads/build/test-results/testDebugUnitTest/*.xml   # only TEST-...AdsTestThemeMatchTest.xml
git checkout -- core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/
rm -f core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/*.bak
# 4. Outside the template the match test is skipped.
mv scripts/new-app.sh "${TMPDIR:-/tmp}/new-app.sh.t084"
./gradlew :core:ads:testDebugUnitTest --tests com.vglstudio.template.core.ads.AdsTestThemeMatchTest --rerun
grep -o 'skipped="[0-9]*"' core/ads/build/test-results/testDebugUnitTest/TEST-com.vglstudio.template.core.ads.AdsTestThemeMatchTest.xml   # skipped="3"
mv "${TMPDIR:-/tmp}/new-app.sh.t084" scripts/new-app.sh
git status --short   # only the files of section 3.2
```

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** việc chuyển 21 tệp test sang `AdsTestTheme` và `AdsTestNextButton` là thay composable bọc ngoài có cùng giá trị, không có hành vi mới để test đỏ trước; bằng chứng là lệnh 1, 2, 3 và cổng chất lượng. Ba test của `AdsTestThemeMatchTest` viết trước được: commit test có `AdsTestThemeMatchTest` và một `AdsTestTheme.kt` khung (bảng màu, chữ mặc định của Material 3) thì `lightTheme_matchesAppTheme`, `darkTheme_matchesAppTheme` đỏ vì màu khác, `fonts_matchDesignSystemFonts` đỏ vì chưa có tệp phông; commit sau điền giá trị và chép phông. Tệp `to-app.md` là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-01, máy dev (macOS, JDK 21.0.13 của `scripts/local-setup.sh`), nhánh `claude/plan-T-084-theme-test-core-ads` trên `origin/main` `151b4c2b`, thay đổi thử không commit và đã hoàn tác. Bản thử: `AdsTestTheme.kt` như mục 3.1 (tên nút trong bản thử là `TestNextButton`), ba tệp Inter chép vào tài nguyên test, 21 tệp đổi bằng `sed` (quyết định 6), `AdsTestThemeMatchTest.kt` như mục 3.1.
- **Lệnh:** `./gradlew :core:ads:verifyRoborazziDebug`; `./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun` rồi `git status --short -- '*/src/test/screenshots/*'`; lệnh 3 của mục 4; `scripts/check.sh`; `python3 scripts/existing-tests.py docs/features/quy-trinh/plans/T-084-theme-co-dinh-cho-test-core-ads.md`.
- **Kết quả:**
  1. Trước khi đổi gì: `:core:ads:verifyRoborazziDebug` qua (đo chuẩn).
  2. Lần thử đầu, phông ở `src/test/resources/fonts/`: đỏ, "1338 tests completed, 237 failed" ở 22 lớp test. 177 lỗi `IllegalStateException: Could not load font`, 59 lỗi `NullPointerException: null cannot be cast to non-null type android.graphics.Typeface`, và `BannerSlotHostTest` (không dùng theme) lỗi `NullPointerException: Cannot read field "mStyle" because "family" is null` ở `Typeface.loadPreinstalledSystemFontMap` lúc Robolectric dựng môi trường. Một test thăm dò tạm (`Typeface.createFromFile`, `Typeface.Builder(File)`, `android.graphics.fonts.Font.Builder(File)`) đỏ cùng lỗi đó trước khi chạy dòng nào. Đổi tên thư mục thành `ads-test-fonts/`: cả ba cách nạp đều trả `Typeface` (`Success`), không lỗi.
  3. Với `ads-test-fonts/`: `:core:ads:verifyRoborazziDebug` qua, 1338 test, 0 lỗi. Ghi lại ảnh (`recordRoborazziDebug ... --rerun`) rồi `git status --short -- '*/src/test/screenshots/*'`: in rỗng, 0 trên 43 ảnh đổi ở mức byte, kể cả 8 ảnh `NativeFlowStep_*` dùng nút của test và 4 ảnh cỡ chữ 2 lần.
  4. Đổi theme của app (lệnh 3 của mục 4, không commit: `BrandPrimary` thành `0xFFC0392B`, `background` sáng thành `0xFFFFF3E0`, `labelLarge` 18/26 sp thay cho 14/20): `:core:ads:verifyRoborazziDebug` chạy lại toàn bộ, "1341 tests completed, 2 failed"; hai test đỏ là `AdsTestThemeMatchTest.lightTheme_matchesAppTheme` và `darkTheme_matchesAppTheme` (`ComparisonFailure` ở `primary`, `inversePrimary`), `fonts_matchDesignSystemFonts` qua. 43 ảnh và 1338 test cũ của `:core:ads` qua, kể cả các test đo kích thước theo cỡ chữ mà Tuấn thấy đỏ ở Photune. Đã trả `Color.kt`, `Type.kt` về.
  5. `scripts/check.sh` với bản thử đủ (theme, nút, phông, test so theme, 21 tệp đổi): lần đầu đỏ ở ktlint (`NativeAdFontScaleTest.kt:271`, dòng 123 ký tự, quyết định 6); tách dòng rồi chạy lại: `== Cổng chất lượng: QUA`, `Test JVM: 1742 test, 0 lỗi. Ảnh screenshot: 91`, `./core/ads: 1341 test, 0 lỗi` (1338 cũ cộng 3 test mới). Sau đó `git status --short -- '*/src/test/screenshots/*'` in rỗng.
  6. Ngoài repo mẫu (lệnh 4 của mục 4): chuyển tạm `scripts/new-app.sh` ra ngoài, chạy `AdsTestThemeMatchTest`: báo cáo XML `tests="3" skipped="3" failures="0" errors="0"`; đã trả tệp về.
  7. `python3 scripts/existing-tests.py <plan>`: "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)".
  8. Mọi thay đổi thử đã hoàn tác; `git status --short` chỉ còn plan và ticket.
- **Chưa thử:** CI Linux (mục 3.4; kiểm ở `pr-check`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung: test và tài nguyên test của `:core:ads`, tài liệu quy trình `docs/ops/to-app.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | 28 tệp trong thư mục test của một module (`:core:ads`: 6 tạo, 22 sửa) cộng `to-app.md` và ticket; không đổi giao diện công khai nào của mã chính |
| B. Độ rõ của yêu cầu | 1 | Ticket và L-143 rõ, ba điều phải quyết đã quyết có lý do; còn câu hỏi về `AdGalleryScreenTest` và `Spacing` (mục 8) không đổi cách làm |
| C. Khả năng kiểm chứng tự động | 1 | So ảnh, test so theme và lệnh đo kiểm hết; CI Linux nạp phông từ tệp chỉ thấy ở `pr-check` |
| D. Ảnh hưởng chéo | 2 | Mọi plan sau thêm test cho `:core:ads` phải dùng `AdsTestTheme`; ticket quảng cáo còn ở backlog (T-069, T-070, T-072, chưa có plan) sẽ thêm test vào `:core:ads` và có thể sửa cùng tệp test (mục 6) |
| E. Độ mới của kỹ thuật | 2 | Nạp phông từ tài nguyên Java trong Robolectric (`Font(File)`) và test chỉ chạy ở repo mẫu bằng `assume` chưa có trong dự án; lần thử gặp lỗi che phông hệ thống của Robolectric |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: 21 tệp test của `:core:ads`, `docs/ops/to-app.md`.
- Plan khác chạm test của `:core:ads` (task quảng cáo còn mở) có thể xung đột ở dòng `import` và chỗ gọi theme. Sau T-084, test mới của `:core:ads` gọi `AdsTestTheme`; test viết theo bản cũ (`AppTheme`) vẫn biên dịch được nhưng phụ thuộc theme của app. Rà soát độc lập của task sau nên bắt điều này; đề xuất ghi vào `docs/ops/ads-guide.md` ở một PR sau nếu người duyệt muốn (mục 8, câu 3).
- Các app nhận thay đổi qua `scripts/template-sync.sh`: `core/ads/` thuộc `TEMPLATE_OWNED`, ảnh của `:core:ads` trở về đúng bản của mẫu. App đã ghi lại ảnh theo theme riêng (Photune) lấy bản của mẫu và bỏ ngoại lệ trong ADR của app.
- Đề xuất thứ tự: chạy khi không có task nào khác của cùng danh sách sửa test của `:core:ads`, hay chạy trước các task đó.

## 7. Điều kiện dừng riêng

- Lệnh 2 của mục 4 in ra ảnh đổi: dừng. Theme của test không còn cùng giá trị với theme lúc viết plan; cần plan mới liệt kê ảnh và mức lệch.
- Lệnh 3: có test hay ảnh của `:core:ads` đỏ ngoài `AdsTestThemeMatchTest` sau khi đổi `AppTheme`: dừng, vì còn chỗ phụ thuộc theme của app mà plan chưa biết.

## 8. Câu hỏi cho người duyệt

1. `AdGalleryScreenTest` của `app/src/testStaging` (bình luận của Tuấn: 1 test đỏ với thang chữ của Photune, thẻ cao 272,33 dp thay vì 270). Ticket để ảnh và test của `:app` theo theme của app, và `:app` thuộc loại "cả hai cùng sửa", nên app tự sửa được. Đề xuất: giữ ngoài plan này; nếu anh muốn, một ticket riêng cho `AdGalleryScreenTest` dùng thang chữ cố định ở các test đo kích thước.
2. `Spacing`: đề xuất giữ (mục 3.1, quyết định 4), ghi rõ trong `to-app.md`. Phương án khác: `:core:ads` có hằng khoảng cách riêng trong mã chính, sửa 9 tệp mã chính, cần sửa thêm `CLAUDE.md` (quy tắc khoảng cách) và là một plan khác.
3. Ghi quy tắc "test mới của `:core:ads` dùng `AdsTestTheme`" vào `docs/ops/ads-guide.md` hay `CLAUDE.md`? Đề xuất: thêm một dòng vào `docs/ops/ads-guide.md` trong plan này nếu anh đồng ý (thêm tệp vào mục 3.2), vì phiên viết test quảng cáo đọc tệp đó.
4. Phông Inter chép vào tài nguyên test (1,2 MB thêm vào git). Đề xuất: chấp nhận; phương án tránh chép (dùng `R.font` của `:core:designsystem`) để lại phụ thuộc phông của app, là phương án dự phòng ở mục 3.4.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
