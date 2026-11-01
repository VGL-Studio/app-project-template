---
ticket: T-084
title: Test của :core:ads vẽ trong theme cố định của test, ảnh chuẩn không đổi theo theme của app
status: done
version: 1
risk_score:
risk_level: high
token_estimate: 22.3M
blocking_points: [core]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-11-01
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
- `AdsTestFontFiles` (`internal`, để test so theme đọc): danh sách `(tên tệp không đuôi, FontWeight, FontStyle)` = `("inter_regular", Normal, Normal)`, `("inter_medium", Medium, Normal)`, `("inter_semibold", SemiBold, Normal)`, đúng thứ tự của `InterFamily`.
- `AdsTestFontFamily` (`internal`): dựng từ `AdsTestFontFiles`, mỗi dòng một `Font(File, weight, style)` từ tệp `<tên>.ttf` đặt ở `core/ads/src/test/resources/ads-test-fonts/`, đọc qua `classLoader.getResource("ads-test-fonts/<tên>.ttf")` rồi `File(url.toURI())`.
- KDoc của tệp nói: theme cố định của test, giá trị của dự án mẫu ở thời điểm T-084; app không sửa (`:core:ads` thuộc phần của mẫu); dự án mẫu đổi `AppTheme` thì đổi theo, `AdsTestThemeMatchTest` báo khi lệch.

**Quyết định 2. Phông chữ: chép Inter vào tài nguyên test của `:core:ads`.** Lý do:

- Dùng `R.font.inter_*` của `:core:designsystem` thì theme của test vẫn phụ thuộc app: app xóa hay thay phông Inter (`docs/ops/new-app.md`, mục 3, bước 2) làm test của `:core:ads` không biên dịch được hoặc đổi ảnh.
- Phông mặc định của Robolectric thì chữ của mọi ảnh có chữ đổi (43 ảnh đều có chữ), plan phải ghi lại toàn bộ ảnh chuẩn, và ảnh không còn giống preview của app mẫu. Theo bình luận của Tuấn, test đo kích thước không đổi theo phông, nên cách này chỉ tốn ở ảnh.
- Phông đặt ở tài nguyên Java của test (`src/test/resources`), không vào APK nào. Ba tệp nặng tổng 1,2 MB trong git. Giấy phép SIL OFL 1.1 cho phép chép kèm giấy phép. Giấy phép đặt theo quy ước của repo, `third_party/<tên>/LICENSE.txt` của module (`docs/ops/new-app.md` mục 3; `core/designsystem/third_party/inter/` của T-002, T-017): `core/ads/third_party/inter/LICENSE.txt`, chép từ `core/designsystem/third_party/inter/LICENSE.txt`.
- Thư mục phải tên khác `fonts/`: lần thử đầu dùng `src/test/resources/fonts/` làm 237 trên 1338 test của `:core:ads` đỏ, kể cả test không dùng theme (`BannerSlotHostTest`). Thư mục `fonts/` trên classpath test che bộ phông hệ thống của Robolectric: Robolectric nạp bảng phông hệ thống lúc dựng môi trường (`Typeface.loadPreinstalledSystemFontMap`) và gặp `NullPointerException ... "family" is null` (mục 4.1, kết quả 2). Tên `ads-test-fonts/` không gặp lỗi này.

**Quyết định 3. `PrimaryButton`: thay bằng nút dựng trong test.** `NativeFlowStepScreenshotTest` (8 ảnh `NativeFlowStep_*`) và hai test `nextButton_samePlace_notCovered_withFullPage`, `nextButton_samePlace_notCovered_withCard` của `NativeFlowStepTest` dùng `PrimaryButton` làm nút "Next" của app. `PrimaryButton` thuộc `:core:designsystem`, app sửa được (bo góc, chiều cao, chữ), nên ảnh và câu `assertEquals(52f, ...)` đổi theo app. Nút trong test đại diện "nút của một app", không cần giống nút của app nào. Thêm vào `AdsTestTheme.kt`: `@Composable internal fun AdsTestNextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)`, dựng như `PrimaryButton` hiện tại ở trạng thái thường: `Button(onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.heightIn(min = 52.dp)) { Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) }`. Lần thử: 8 ảnh `NativeFlowStep_*` giống từng byte, hai test qua với câu khẳng định giữ nguyên (52 dp). Câu `assertEquals(52f, ...)` từ nay chỉ kiểm nút do test dựng (mục 3.5).

**Quyết định 4. `Spacing`: giữ, ghi rõ trong `to-app.md`.** 9 tệp mã chính của `:core:ads` dùng `Spacing` (37 chỗ: `xs` 7, `sm` 14, `md` 15, `lg` 5, `xl` 2), và 9 tệp test import nó để dựng khung hay so vị trí với mã chính (ví dụ `withAd.top - Spacing.md` trong `NativeFlowStepTest`). Theme của test không chặn được phần của mã chính. Lý do giữ:

- `Spacing` là lưới 4 dp. `docs/ops/new-app.md` mục 3 (thương hiệu) bảo app thay màu, logo, khẩu hiệu, phông; không bảo thay `Spacing`. Photune (issue #321) đỏ vì màu và thang chữ, không vì `Spacing`.
- Thay bằng hằng riêng trong mã chính của `:core:ads` là sửa 9 tệp mã chính, trái `CLAUDE.md` ("khoảng cách lấy từ `:core:designsystem`") và đổi hành vi của quảng cáo thật trong app, trong khi ticket chỉ đòi test không phụ thuộc theme.
- Test giữ `Spacing` để so vị trí với mã chính: app đổi `Spacing` thì mã chính đổi theo, test vẫn đúng ý.
- `to-app.md` mục 2 ghi: app đổi giá trị `Spacing` thì quảng cáo trong app cũng đổi khoảng cách, và ảnh của `:core:ads` phải ghi lại như một ngoại lệ riêng của app (ADR của app). Người duyệt muốn khác thì trả lời mục 8, câu 2.

**Quyết định 5. Test so theme, chỉ ở repo mẫu.** Tệp mới `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestThemeMatchTest.kt` (Robolectric, `createComposeRule()`; lớp này không chụp ảnh nên quy tắc cửa sổ của L-048 không áp):

- `@Before`: tìm gốc repo bằng cách đi lên từ thư mục làm việc tới thư mục có `settings.gradle.kts`; `assumeTrue(File(root, "scripts/new-app.sh").isFile)`. Ở app, cả lớp được báo là bỏ qua, không đỏ. Lớp vẫn được biên dịch ở app, nên app phải giữ tên và chữ ký `AppTheme(darkTheme, content)` (ghi vào `to-app.md` và KDoc).
- KDoc của lớp còn ghi: test đọc `scripts/new-app.sh` và thư mục `core/designsystem/src/main/res/font/` ngoài các đầu vào Gradle khai báo, nên Gradle có thể coi task test là đã cập nhật khi chỉ các tệp đó đổi; khi kiểm thì chạy với `--rerun` (lệnh 4 của mục 4).
- Mỗi test dựng một lần `setContent { AppTheme(darkTheme = d) { lấy MaterialTheme.colorScheme, typography, shapes }; AdsTestTheme(darkTheme = d) { lấy như trên } }`. `AppTheme` là `public`, còn `LightColors`, `DarkColors`, `AppTypography` là `internal` ở `:core:designsystem`, nên test đọc giá trị qua `MaterialTheme`, không cần sửa mã chính của module đó.
- So bảng màu bằng `ColorScheme.toString()`. Lần thử (mục 4.1, kết quả 9) thấy `toString()` của material3 hiện dùng (BOM `2025.09.00`) in 36 vai, gồm nhóm `surfaceContainer*` và `scrim`, không có nhóm `*Fixed`. Phiên ghi số vai in ra vào nhật ký; nếu `ColorScheme` của bản đang dùng có thuộc tính mà `toString()` không in (như `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant` và nhóm tương ứng của `secondary`, `tertiary`), test so thêm từng thuộc tính đó.
- So 15 kiểu chữ sau khi bỏ `fontFamily` (`style.copy(fontFamily = null)`), vì hai họ phông là hai đối tượng khác nhau; so `shapes`.
- So cấu trúc họ phông, để bắt khi mẫu thêm tệp phông vào `InterFamily`, đổi gán độ đậm với tệp, hay cho một kiểu chữ dùng họ khác: với từng kiểu trong 15 kiểu của `AppTheme`, ép `fontFamily` về `FontListFontFamily` (không ép được, ví dụ `FontFamily.Default`, thì đỏ), lấy danh sách `(resources.getResourceEntryName((font as ResourceFont).resId), font.weight, font.style)` và so bằng với `AdsTestFontFiles`.
- So phông bằng nội dung tệp: liệt kê thư mục `core/designsystem/src/main/res/font/` (không ghi cứng tên tệp); danh sách tên không đuôi phải bằng tập tên của `AdsTestFontFiles`, và mỗi tệp giống từng byte tệp cùng tên trong `ads-test-fonts/`.

**Quyết định 6. Đổi tệp test.** Trong 21 tệp: bỏ dòng `import com.vglstudio.template.core.designsystem.theme.AppTheme`, thay `AppTheme {` và `AppTheme(` bằng `AdsTestTheme {` và `AdsTestTheme(`; trong hai tệp `NativeFlowStep*` bỏ thêm `import ...component.PrimaryButton` và thay `PrimaryButton(` bằng `AdsTestNextButton(`. Không đổi câu khẳng định, khung, đường dẫn ảnh hay dữ liệu nào. Lần thử làm đúng việc này bằng `sed` rồi chạy ktlint qua `scripts/check.sh` (mục 4.1): tên mới dài hơn 4 ký tự, nên dòng `AdsTestTheme { NativeAdCard(... ctaLarge = ctaLarge) }` của `NativeAdFontScaleTest.kt` (khoảng dòng 271) dài 123 ký tự và ktlint báo lỗi; tách khối lambda thành ba dòng thì qua. Các dòng khác có tên mới đều dưới 121 ký tự.

**Quyết định 7. Không đổi:** hàm `@Preview` trong mã chính (`NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `NativeFullScreenAdScreen.kt`) vẫn dùng `AppTheme`; ảnh và test của `:app` (`AdGalleryScreen*`); `:feature:loading`, `:feature:sample`; không ghi lại ảnh chuẩn nào.

**`to-app.md`, mục 2:** thêm một đoạn dưới bảng: "Test và ảnh chuẩn của `:core:ads` vẽ trong theme cố định của test (`AdsTestTheme`, chép giá trị của dự án mẫu, phông Inter riêng trong `src/test/resources/ads-test-fonts/`), không theo `AppTheme` của app (T-084, L-143). App giữ tên và chữ ký `AppTheme(darkTheme, content)`: `AdsTestThemeMatchTest.kt` vẫn được biên dịch ở app dù bị bỏ qua. App đổi màu, chữ hay phông trong `:core:designsystem` không cần ghi lại ảnh của `:core:ads`; xung đột ở các ảnh đó lấy bản của mẫu như phần còn lại của module. Còn phụ thuộc: `Spacing` (mã chính của `:core:ads` dùng). App đổi giá trị `Spacing` thì ghi lại ảnh của `:core:ads` và ghi ngoại lệ trong ADR của app. Ảnh của `:app` có quảng cáo (`AdGalleryScreen_*`) vẫn theo theme của app."

**`ads-guide.md`, mục 5, ý 5 (Test):** thêm câu cuối: "Test trong `:core:ads` (module của dự án mẫu) vẽ trong `AdsTestTheme` của `core/ads/src/test`, không dùng `AppTheme`, để ảnh chuẩn không đổi theo theme của app (T-084, `docs/ops/to-app.md` mục 2); nút trong test dùng `AdsTestNextButton`, không dùng `PrimaryButton`." (câu hỏi 3, anh Nhật đồng ý 2026-11-01).

**Ticket:** phiên điền trạng thái theo plan (`scripts/status.py --sync`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestTheme.kt` | `AdsTestTheme`, bảng màu, thang chữ, họ phông Inter của test, `AdsTestNextButton` (quyết định 1, 2, 3) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsTestThemeMatchTest.kt` | Test so theme của test với `AppTheme` (màu, kiểu chữ, cấu trúc họ phông, tệp phông), chỉ chạy ở repo mẫu (quyết định 5) |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_regular.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_regular.ttf` |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_medium.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_medium.ttf` |
| Tạo | `core/ads/src/test/resources/ads-test-fonts/inter_semibold.ttf` | Chép từ `core/designsystem/src/main/res/font/inter_semibold.ttf` |
| Tạo | `core/ads/third_party/inter/LICENSE.txt` | Giấy phép SIL OFL 1.1 của Inter, chép từ `core/designsystem/third_party/inter/LICENSE.txt` (quyết định 2) |
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
| Sửa | `docs/ops/ads-guide.md` | Mục 5, ý 5: câu về `AdsTestTheme` (mục 3.1, câu hỏi 3) |
| Sửa | `docs/features/quy-trinh/tickets/T-084-theme-co-dinh-cho-test-core-ads.md` | Trạng thái theo plan (`scripts/status.py --sync`) |

Không ghi lại ảnh chuẩn nào: 43 ảnh trong `core/ads/src/test/screenshots/` giữ nguyên từng byte (mục 4.1). Không sửa mã Kotlin ngoài thư mục test.

Nơi đọc các file plan đổi (L-020; `grep -rn` trong `scripts/`, `.github/`, `*/src/test`, 2026-11-01):
- Tệp test của `:core:ads`: chỉ Gradle biên dịch; `scripts/check.sh` đọc lệnh `captureRoboImage` trong `src/test` (đường dẫn ảnh không đổi). `scripts/existing-tests.py` và `scripts/ads-plan.py` không đọc tệp test.
- `docs/ops/to-app.md`: `scripts/template-sync.sh` và `scripts/tests/run.sh` không đọc nội dung mục 2 (chỉ dẫn tên tệp trong chú thích và thông báo). Bảng mục 2 không đổi, nên `TEMPLATE_OWNED` không cần đổi (mục 7 của `to-app.md`).
- `docs/ops/ads-guide.md`: không script hay test nào đọc nội dung tệp (`scripts/ads-plan.py` chỉ nhắc tên trong chú thích và chữ của bảng Excel; các test chỉ nhắc tên trong chú thích); chỉ thêm một câu vào ý 5 của mục 5, không đổi số mục.
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
- **Điều kiện chuyển:** `verifyRoborazziDebug` của `:core:ads` đỏ trên Linux (phiên cloud hay `pr-check`) ở ảnh mà máy macOS vẽ giống từng byte (mục 4.1, kết quả 3), và ảnh `_compare.png` (thư mục `core/ads/build/outputs/roborazzi/` của phiên, hay artifact `build-and-test-reports` của `pr-check`) cho thấy chỉ lệch ở vùng chữ. Phiên Linux không ghi lại ảnh chuẩn để cho qua (mục 7).
- **Cách làm thay:** theme của test dùng `Font(R.font.inter_*)` của `:core:designsystem` cho tới khi có cách nạp phông riêng chạy được trên CI, chỉ thay phần `AdsTestFontFamily` trong `AdsTestTheme.kt`; màu, thang chữ, nút và test so theme giữ nguyên. Phần phông khi đó còn phụ thuộc app: ghi vào `to-app.md` cùng chỗ với `Spacing`, và ghi vào nhật ký để người quyết ticket tiếp theo. Tiêu chí 1, 2, 4, 5 vẫn đạt; tiêu chí 3 (đổi màu) vẫn đạt.

### 3.5. Hành vi đã có bị đổi

Plan không sửa mã Kotlin ngoài thư mục test (`scripts/existing-tests.py` in "Không có tên nào để tìm"; mục 4.1, kết quả 7). Các test có sẵn của `:core:ads` giữ nguyên mọi câu khẳng định; chỉ đổi composable bọc ngoài (`AppTheme` thành `AdsTestTheme`, `PrimaryButton` thành `AdsTestNextButton`) có cùng giá trị. Một câu đổi điều nó đo, dù chữ giữ nguyên:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Nút Next là `PrimaryButton` thật của `:core:designsystem`, cao 52 dp | Nút Next là `AdsTestNextButton` do test dựng, `heightIn(min = 52.dp)` (quyết định 3) | `NativeFlowStepTest.nextButton_samePlace_notCovered_withFullPage`, câu `assertEquals(52f, (withAd.bottom - withAd.top).value, 0.5f)` (khoảng dòng 632) | Giữ nguyên chữ. Từ nay câu chỉ kiểm khung dựng của test: nút đại diện cao 52 dp, để vị trí của trang quảng cáo được đo cạnh một nút cỡ thật. Chiều cao của `PrimaryButton` không phải việc của `:core:ads`; nó đã có test ở `:core:designsystem`. Ý định của test (Next không bị trang hay CTA che, Next không dời chỗ khi quảng cáo biến mất, chạm tới được app) vẫn do các câu còn lại của test kiểm |
| Như trên | Như trên | `NativeFlowStepTest.nextButton_samePlace_notCovered_withCard` | Giữ. Test không đo chiều cao nút; các câu so vị trí (thẻ không che Next, Next không dời chỗ, vùng bước lấy lại chỗ `withAd.top - Spacing.md`) vẫn đúng ý với nút của test |
| Như trên | Như trên | `NativeFlowStepScreenshotTest` (8 ảnh `NativeFlowStep_*`) | Giữ; ảnh giống từng byte (mục 4.1, kết quả 3) |

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Định nghĩa hoàn thành 4: theme của test khớp `AppTheme` (sáng) | Compose UI test (Robolectric) | `AdsTestThemeMatchTest.lightTheme_matchesAppTheme`: `ColorScheme.toString()` bằng nhau, 15 kiểu chữ bằng nhau sau khi bỏ `fontFamily`, `shapes` bằng nhau |
| Định nghĩa hoàn thành 4: theme của test khớp `AppTheme` (tối) | Compose UI test (Robolectric) | `AdsTestThemeMatchTest.darkTheme_matchesAppTheme`: như trên với `darkTheme = true` |
| Định nghĩa hoàn thành 4: phông của test là phông của mẫu | Unit test (Robolectric) | `AdsTestThemeMatchTest.fonts_matchDesignSystemFonts`: liệt kê `core/designsystem/src/main/res/font/`; tập tên tệp (bỏ đuôi) bằng tập tên của `AdsTestFontFiles`, mỗi tệp giống từng byte tệp cùng tên trong `ads-test-fonts/` |
| Định nghĩa hoàn thành 4: cấu trúc họ phông khớp | Compose UI test (Robolectric) | `AdsTestThemeMatchTest.fontFamilies_matchAppTheme`: với từng kiểu trong 15 kiểu chữ của `AppTheme`, `fontFamily` ép được về `FontListFontFamily`, và danh sách `(getResourceEntryName(resId), weight, style)` của nó bằng `AdsTestFontFiles` |
| Bảng màu so đủ vai | Ghi nhật ký | Phiên ghi số vai mà `ColorScheme.toString()` của material3 đang dùng in ra (lần thử: 36). `ColorScheme` có thuộc tính mà `toString()` không in (nhóm `*Fixed`) thì test so thêm từng thuộc tính đó (quyết định 5) |
| Định nghĩa hoàn thành 4: chỉ chạy ở repo mẫu | Lệnh, không commit | Lệnh 4 của khối dưới bảng: đổi tên tạm `scripts/new-app.sh`, chạy `AdsTestThemeMatchTest`, báo cáo XML có 4 test `skipped`, 0 lỗi; trả tên tệp |
| Định nghĩa hoàn thành 4: test so theme bắt được lệch | Đối chứng, không commit | Lệnh 3: đổi màu `BrandPrimary`, `background` sáng và cỡ `labelLarge` của `AppTheme`; `lightTheme_matchesAppTheme` đỏ. Lệnh 3b: thêm một `Font` vào `InterFamily` (`Font(R.font.inter_medium, FontWeight.Bold)`); `fontFamilies_matchAppTheme` đỏ |
| Định nghĩa hoàn thành 1 | Lệnh | Lệnh 1: ngoài `AdsTestThemeMatchTest.kt` (phải gọi `AppTheme` để so), không tệp test nào của `:core:ads` nhắc `AppTheme` hay `PrimaryButton` |
| Định nghĩa hoàn thành 2: ảnh chuẩn không đổi | Lệnh đo, không commit | Lệnh 2: ghi lại ảnh của `:core:ads` bằng theme của test; `git status --short -- '*/src/test/screenshots/*'` in rỗng (0 ảnh đổi ở mức byte) |
| Định nghĩa hoàn thành 3: đổi theme của app, ảnh của `:core:ads` vẫn khớp | Lệnh đo, không commit | Lệnh 3: sau khi đổi `AppTheme` như trên, `:core:ads:verifyRoborazziDebug` chỉ có test của `AdsTestThemeMatchTest` đỏ; 43 ảnh và mọi test khác qua; trả `Color.kt`, `Type.kt` về |
| Mọi test qua, không ảnh nào đổi | Cổng chất lượng | `scripts/check.sh`: `== Cổng chất lượng: QUA`; số test JVM không ít hơn trước cộng 4 (test mới); `git status --short -- '*/src/test/screenshots/*'` in rỗng |
| `to-app.md` đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc đoạn mới ở mục 2 |
| `ads-guide.md` đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt đọc câu mới ở mục 5, ý 5 |
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
# 3b. Change the font family of the app (not committed): fontFamilies_matchAppTheme must fail.
sed -i.bak 's/    Font(R.font.inter_semibold, FontWeight.SemiBold),/    Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_medium, FontWeight.Bold),/' \
  core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/Type.kt
./gradlew :core:ads:testDebugUnitTest --tests com.vglstudio.template.core.ads.AdsTestThemeMatchTest --rerun; echo "exit $? (must be 1)"
grep -A1 'name="fontFamilies_matchAppTheme"' core/ads/build/test-results/testDebugUnitTest/TEST-com.vglstudio.template.core.ads.AdsTestThemeMatchTest.xml | grep -c '<failure'   # 1
git checkout -- core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/
rm -f core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/theme/*.bak
# 4. Outside the template the match test is skipped.
mv scripts/new-app.sh "${TMPDIR:-/tmp}/new-app.sh.t084"
./gradlew :core:ads:testDebugUnitTest --tests com.vglstudio.template.core.ads.AdsTestThemeMatchTest --rerun
grep -o 'skipped="[0-9]*"' core/ads/build/test-results/testDebugUnitTest/TEST-com.vglstudio.template.core.ads.AdsTestThemeMatchTest.xml   # skipped="4"
mv "${TMPDIR:-/tmp}/new-app.sh.t084" scripts/new-app.sh
git status --short   # only the files of section 3.2
```

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** việc chuyển 21 tệp test sang `AdsTestTheme` và `AdsTestNextButton` là thay composable bọc ngoài có cùng giá trị, không có hành vi mới để test đỏ trước; bằng chứng là lệnh 1, 2, 3 và cổng chất lượng. Bốn test của `AdsTestThemeMatchTest` viết trước được: commit test có `AdsTestThemeMatchTest` và một `AdsTestTheme.kt` khung (bảng màu và chữ mặc định của Material 3, `AdsTestFontFiles` rỗng) thì `lightTheme_matchesAppTheme`, `darkTheme_matchesAppTheme` đỏ vì màu khác, `fontFamilies_matchAppTheme` và `fonts_matchDesignSystemFonts` đỏ vì danh sách phông rỗng và chưa có tệp phông; commit sau điền giá trị và chép phông. Tệp `to-app.md` là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-01, máy dev (macOS, JDK 21.0.13 của `scripts/local-setup.sh`), nhánh `claude/plan-T-084-theme-test-core-ads` trên `origin/main` `151b4c2b`, thay đổi thử không commit và đã hoàn tác. Bản thử: `AdsTestTheme.kt` như mục 3.1 (tên nút trong bản thử là `TestNextButton`), ba tệp Inter chép vào tài nguyên test, 21 tệp đổi bằng `sed` (quyết định 6), `AdsTestThemeMatchTest.kt` như mục 3.1.
- **Lệnh:** `./gradlew :core:ads:verifyRoborazziDebug`; `./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun` rồi `git status --short -- '*/src/test/screenshots/*'`; lệnh 3 của mục 4; `scripts/check.sh`; `python3 scripts/existing-tests.py docs/features/quy-trinh/plans/T-084-theme-co-dinh-cho-test-core-ads.md`.
- **Kết quả:**
  1. Trước khi đổi gì: `:core:ads:verifyRoborazziDebug` qua (đo chuẩn).
  2. Lần thử đầu, phông ở `src/test/resources/fonts/`: đỏ, "1338 tests completed, 237 failed" ở 22 lớp test. 177 lỗi `IllegalStateException: Could not load font`, 59 lỗi `NullPointerException: null cannot be cast to non-null type android.graphics.Typeface`, và `BannerSlotHostTest` (không dùng theme) lỗi `NullPointerException: Cannot read field "mStyle" because "family" is null` ở `Typeface.loadPreinstalledSystemFontMap` lúc Robolectric dựng môi trường. Một test thăm dò tạm (`Typeface.createFromFile`, `Typeface.Builder(File)`, `android.graphics.fonts.Font.Builder(File)`) đỏ cùng lỗi đó trước khi chạy dòng nào. Đổi tên thư mục thành `ads-test-fonts/`: cả ba cách nạp đều trả `Typeface` (`Success`), không lỗi.
  3. Với `ads-test-fonts/`: `:core:ads:verifyRoborazziDebug` qua, 1338 test, 0 lỗi. Ghi lại ảnh (`recordRoborazziDebug ... --rerun`) rồi `git status --short -- '*/src/test/screenshots/*'`: in rỗng, 0 trên 43 ảnh đổi ở mức byte, kể cả 8 ảnh `NativeFlowStep_*` dùng nút của test và 4 ảnh cỡ chữ 2 lần.
  4. Đổi theme của app (lệnh 3 của mục 4, không commit: `BrandPrimary` thành `0xFFC0392B`, `background` sáng thành `0xFFFFF3E0`, `labelLarge` 18/26 sp thay cho 14/20): `:core:ads:verifyRoborazziDebug` chạy lại toàn bộ, "1341 tests completed, 2 failed"; hai test đỏ là `AdsTestThemeMatchTest.lightTheme_matchesAppTheme` và `darkTheme_matchesAppTheme` (`ComparisonFailure` ở `primary`, `inversePrimary`), `fonts_matchDesignSystemFonts` qua. 43 ảnh và 1338 test cũ của `:core:ads` qua, kể cả các test đo kích thước theo cỡ chữ mà Tuấn thấy đỏ ở Photune. Đã trả `Color.kt`, `Type.kt` về.
  5. `scripts/check.sh` với bản thử đủ (theme, nút, phông, test so theme, 21 tệp đổi): lần đầu đỏ ở ktlint (`NativeAdFontScaleTest.kt:271`, dòng 123 ký tự, quyết định 6); tách dòng rồi chạy lại: `== Cổng chất lượng: QUA`, `Test JVM: 1742 test, 0 lỗi. Ảnh screenshot: 91`, `./core/ads: 1341 test, 0 lỗi` (1338 cũ cộng 3 test mới). Sau đó `git status --short -- '*/src/test/screenshots/*'` in rỗng.
  6. Ngoài repo mẫu (lệnh 4 của mục 4): chuyển tạm `scripts/new-app.sh` ra ngoài, chạy `AdsTestThemeMatchTest` (bản thử có 3 test, chưa có `fontFamilies_matchAppTheme`): báo cáo XML `tests="3" skipped="3" failures="0" errors="0"`; đã trả tệp về.
  7. `python3 scripts/existing-tests.py <plan>`: "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)".
  8. Mọi thay đổi thử đã hoàn tác; `git status --short` chỉ còn plan và ticket.
  9. Sau rà soát độc lập (2026-11-01), một test thăm dò tạm trong `:core:ads` (không commit, đã xóa) kiểm API của phần so cấu trúc: trong `AppTheme(darkTheme = false)`, `typography.titleMedium.fontFamily as FontListFontFamily` ép được; `fonts` gồm ba `ResourceFont`, `resources.getResourceEntryName(resId)` trả `(inter_regular, 400, Normal)`, `(inter_medium, 500, Normal)`, `(inter_semibold, 600, Normal)`; liệt kê `core/designsystem/src/main/res/font` từ gốc repo trả ba tệp `.ttf`. `ColorScheme.toString()` in 36 vai: 35 vai của `Color.kt` cộng `scrim`, gồm nhóm `surfaceContainer*`, không có nhóm `*Fixed`. Chưa chạy: test `fontFamilies_matchAppTheme` đủ và lệnh 3b (đối chứng đổi phông); phiên chạy ở bước 3 và bước 5.
- **Chưa thử:** CI Linux (mục 3.4; kiểm ở `pr-check`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung: test và tài nguyên test của `:core:ads`, tài liệu quy trình `docs/ops/to-app.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | 27 tệp trong thư mục test của một module (`:core:ads`: 5 tạo, 22 sửa), giấy phép phông trong `core/ads/third_party/`, cộng `to-app.md` và ticket; không đổi giao diện công khai nào của mã chính |
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
- Việc sau merge (người hay PR báo cáo, không phải phiên trong task): ghi dòng Áp dụng của L-143 trong `docs/lessons.md` (T-084, số PR) và đóng issue #321 kèm đường dẫn PR. Phiên ghi hai việc này vào mục Việc còn lại của nhật ký.

## 7. Điều kiện dừng riêng

- Lệnh 2 của mục 4 in ra ảnh đổi: dừng. Theme của test không còn cùng giá trị với theme lúc viết plan; cần plan mới liệt kê ảnh và mức lệch.
- Lệnh 3: có test hay ảnh của `:core:ads` đỏ ngoài `AdsTestThemeMatchTest` sau khi đổi `AppTheme`: dừng, vì còn chỗ phụ thuộc theme của app mà plan chưa biết.
- Ảnh của `:core:ads` đỏ trên Linux (phiên cloud hay `pr-check`): không ghi lại ảnh chuẩn để cho qua. Lệch đúng điều kiện chuyển của mục 3.4 thì chuyển phương án dự phòng; lệch khác thì dừng.

## 8. Câu hỏi cho người duyệt

1. `AdGalleryScreenTest` của `app/src/testStaging` (bình luận của Tuấn: 1 test đỏ với thang chữ của Photune, thẻ cao 272,33 dp thay vì 270). Ticket để ảnh và test của `:app` theo theme của app, và `:app` thuộc loại "cả hai cùng sửa", nên app tự sửa được. Đề xuất: giữ ngoài plan này; nếu anh muốn, một ticket riêng cho `AdGalleryScreenTest` dùng thang chữ cố định ở các test đo kích thước.
   - Trả lời (anh Nhật, 2026-11-01): theo đề xuất: giữ ngoài plan này; app gặp lỗi thì báo về bằng from-app, làm ticket riêng nếu cần.
2. `Spacing`: đề xuất giữ (mục 3.1, quyết định 4), ghi rõ trong `to-app.md`. Phương án khác: `:core:ads` có hằng khoảng cách riêng trong mã chính, sửa 9 tệp mã chính, cần sửa thêm `CLAUDE.md` (quy tắc khoảng cách) và là một plan khác.
   - Trả lời (anh Nhật, 2026-11-01): theo đề xuất: giữ phụ thuộc, ghi rõ trong `to-app.md` (quyết định 4).
3. Ghi quy tắc "test mới của `:core:ads` dùng `AdsTestTheme`" vào `docs/ops/ads-guide.md` hay `CLAUDE.md`? Đề xuất: thêm một dòng vào `docs/ops/ads-guide.md` trong plan này nếu anh đồng ý (thêm tệp vào mục 3.2), vì phiên viết test quảng cáo đọc tệp đó.
   - Trả lời (anh Nhật, 2026-11-01): có, thêm một dòng vào `docs/ops/ads-guide.md`. Đã làm: mục 3.1 (đoạn `ads-guide.md`), 3.2, nơi đọc file, mục 4.
4. Phông Inter chép vào tài nguyên test (1,2 MB thêm vào git). Đề xuất: chấp nhận; phương án tránh chép (dùng `R.font` của `:core:designsystem`) để lại phụ thuộc phông của app, là phương án dự phòng ở mục 3.4.
   - Trả lời (anh Nhật, 2026-11-01): chấp nhận chép phông Inter (giấy phép OFL 1.1 ở `core/ads/third_party/inter/LICENSE.txt`).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-11-01 (anh Nhật chọn "Chấp nhận, duyệt", lời anh Nhật: "Cùng tôi trả lời các câu hỏi đề duyệt."), sau khi trả lời câu hỏi 1 tới 4 (như ghi ở mục 8) và xem tóm tắt plan cùng kết quả rà soát độc lập (vòng 1: 0 chặn, 3 nên sửa, 6 ghi chú, đã sửa; rà soát lại: 0 chặn, 0 nên sửa, 1 ghi chú: phía test, nên kiểm mọi kiểu chữ của `AdsTestTheme` dùng `AdsTestFontFamily`, phiên làm được trong `fontFamilies_matchAppTheme`). Anh Nhật vừa duyệt vừa duyệt chéo vì nhóm chỉ có một dev (`risk-scoring.md`, mục 3). Thêm `docs/ops/ads-guide.md` vào mục 3.2 theo câu hỏi 3.

## 10. Nhật ký thực hiện

### RUN-20261101-01, phiên bản plan 1

- **Kết quả:** đã merge (PR #372, commit e58cb2b trên nhánh chính, 2026-11-01 05:40 UTC). `main-guard` xanh (lần chạy 41277856146), không có issue `main-broken`.
- **Nhánh, PR:** `claude/T-084-theme-test-core-ads`, https://github.com/VGL-Studio/app-project-template/pull/372. `merge-if-green.sh` báo không xóa được nhánh sau merge (L-092): người xóa tay.
- **Đã làm:** `AdsTestTheme.kt` (bảng màu sáng, tối 35 vai mỗi bảng, thang chữ 15 kiểu, phông Inter đọc từ `src/test/resources/ads-test-fonts/`, `AdsTestNextButton`); `AdsTestThemeMatchTest.kt` (4 test); 21 tệp test đổi `AppTheme` thành `AdsTestTheme`, hai tệp `NativeFlowStep*` đổi `PrimaryButton` thành `AdsTestNextButton`; KDoc của `FontScaleTestSupport.kt`; ba tệp phông và `core/ads/third_party/inter/LICENSE.txt`; đoạn mới ở `docs/ops/to-app.md` mục 2 và câu mới ở `docs/ops/ads-guide.md` mục 5 ý 5. Ticket chuyển `on-staging` qua PR báo cáo.
- **Test viết trước:** 4 test (`lightTheme_matchesAppTheme`, `darkTheme_matchesAppTheme`, `fontFamilies_matchAppTheme`, `fonts_matchDesignSystemFonts`), commit b93f7b7 với khung `AdsTestTheme.kt` (màu mặc định Material 3, `FontFamily.Default`, danh sách phông rỗng; không dùng `FontFamily(emptyList())`, theo ghi chú 1 của rà soát trước khi chạy): 4/4 đỏ đúng lý do (`ComparisonFailure` ở bảng màu; `fonts expected:<[]> but was:<[(inter_regular, ...)]>`; `Font files of :core:designsystem expected:<[]> but was:<[inter_medium, inter_semibold, inter_regular]>`). Thêm theo ghi chú 4 của rà soát trước khi chạy: `fontFamilies_matchAppTheme` còn khẳng định mọi kiểu chữ của `AdsTestTheme` dùng `AdsTestFontFamily` (`assertSame`). Việc chuyển 21 tệp nằm trong ngoại lệ viết test trước của mục 4.
- **Lệnh của mục 4 (phiên cloud, Linux):** lệnh 1 in rỗng (loại thêm `AdsTestTheme.kt` theo quyết định của anh Nhật ở danh sách chạy); grep chỗ gọi và import thật chỉ in `AdsTestThemeMatchTest.kt`. Lệnh 2: `recordRoborazziDebug :core:ads:testDebugUnitTest --rerun`, 1342 test, 0 lỗi; `git status --short -- '*/src/test/screenshots/*'` in rỗng (0 trên 43 ảnh đổi, nên không cần so với ảnh ghi bằng code trước T-084). Lệnh 3: `:core:ads:verifyRoborazziDebug --continue` mã 1, "1342 tests completed, 2 failed", chỉ `TEST-...AdsTestThemeMatchTest.xml` có `<failure` (`lightTheme_matchesAppTheme`, `darkTheme_matchesAppTheme`); đã trả `Color.kt`, `Type.kt` về. Lệnh 3b: `fontFamilies_matchAppTheme` đỏ (1 `<failure`), mã 1. Lệnh 4: `tests="4" skipped="4" failures="0" errors="0"`; đã trả `scripts/new-app.sh` về. Lệnh 3, 3b chạy không có `-i.bak` (lệnh `rm -f .../*.bak` của plan bị bộ kiểm an toàn của môi trường chặn vì biến đường dẫn; trả tệp bằng `git checkout`, kết quả như plan).
- **Bảng màu:** material3 1.3.2 (BOM 2025.09.00) không có thuộc tính `*Fixed` trong `ColorScheme`; test vẫn so mọi getter `*Fixed` bằng phản chiếu nếu bản sau có. Số vai `toString()` in: 36 như lần thử của plan (không đếm lại riêng; hai bảng khớp nhau qua test).
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 12 ghi chú. Nên sửa: liệt kê đủ các tệp test đổi sau commit test (L-093): commit code 7acf1a5 đổi `AdsTestTheme.kt` (điền khung), 21 tệp đổi composable bọc ngoài và import, `FontScaleTestSupport.kt` (KDoc); `AdsTestThemeMatchTest.kt` không đổi; đã ghi ở mô tả PR, nhật ký này và báo cáo phiên, không đổi code. Ghi chú đáng xem: khối KDoc của `AdsTestFontFamily` nằm trên `private object AdsTestFontLoader` nên gắn vào object đó (`AdsTestTheme.kt` khoảng dòng 114 đến 122), chỉ là chữ, không sửa. Các ghi chú khác xác nhận màu, chữ, nút, phông, giấy phép khớp; 21 tệp chỉ đổi đúng phạm vi; không xóa test.
- **Kết quả CI:** `scripts/check.sh` cục bộ: `== Cổng chất lượng: QUA`, 1743 test JVM (1739 cộng 4), 0 lỗi, 91 ảnh, không ảnh nào đổi. `pr-check` xanh (lần chạy 41277165731, gồm so ảnh trên Linux); `main-guard` xanh.
- **Lệch so với plan:** (1) `AdsTestTheme.kt` thêm `private object AdsTestFontLoader` để lấy class loader của module: lần chạy đầu của lệnh 2, `AdsTestFontFiles.javaClass.classLoader` (lớp danh sách của JDK, bootstrap loader) trả `null`, mọi test dùng theme lỗi `Missing test font`; plan chỉ ghi `classLoader.getResource(...)`. Chi tiết cài đặt trong tệp của mục 3.2, không đổi điều test kiểm. (2) Không có `-i.bak` ở lệnh 3, 3b (xem trên). (3) Ở bước 3 chỉ chạy test của `:core:ads`, không chạy `testDebugUnitTest testStagingDebugUnitTest` của mọi module: commit test chỉ thêm hai tệp test mới trong `:core:ads`; `check.sh` ở bước 5 chạy đủ mọi module. Không có phần ngoài mục 3.2; tệp ticket đi qua PR báo cáo.
- **Việc còn lại:** người ghi dòng Áp dụng của L-143 trong `docs/lessons.md` (T-084, PR #372) và đóng issue #321 kèm đường dẫn PR (mục 6 của plan). Xóa nhánh `claude/T-084-theme-test-core-ads`.
- **Cần người xem:** đoạn mới ở `docs/ops/to-app.md` mục 2 và câu ở `docs/ops/ads-guide.md` mục 5 ý 5 (kiểm tra thủ công của mục 4).
