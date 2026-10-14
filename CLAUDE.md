# HƯỚNG DẪN AI LÀM VIỆC VỚI DỰ ÁN

Đây là dự án mẫu cho các app Android của nhóm. Mọi app mới được tạo từ dự án này. Quy trình làm việc với tài liệu (spec, ticket, plan, phiên tự chạy) nằm ở [docs/CLAUDE.md](docs/CLAUDE.md); đọc file đó trước khi làm bất kỳ task nào.

## Công nghệ

- Kotlin, Jetpack Compose, một Activity, Navigation Compose với đích điều hướng kiểu `@Serializable`.
- ViewModel với `StateFlow`, luồng dữ liệu một chiều. Hilt cho tiêm phụ thuộc. DataStore cho lưu trữ cục bộ.
- minSdk 26, compileSdk và targetSdk 36. Phiên bản thư viện nằm ở `gradle/libs.versions.toml`.

## Cấu trúc module

| Module | Vai trò |
| --- | --- |
| `:app` | Vỏ ứng dụng, điều hướng, ghép các feature, Crashlytics. Hai biến thể `staging` và `production` (ADR-0007, ADR-0012) |
| `:core:designsystem` | Theme, màu, chữ, khoảng cách, thành phần giao diện dùng chung. Bản gốc của hệ thống thiết kế (ADR-0008) |
| `:core:data` | DataStore dùng chung |
| `:core:config` | Cờ tính năng, giá trị từ Firebase Remote Config (ADR-0003, ADR-0012) |
| `:core:analytics` | Ghi sự kiện phân tích vào Firebase Analytics (ADR-0012) |
| `:core:ads` | Vị trí quảng cáo (ADR-0004) |
| `:feature:<ten>` | Mỗi feature một module |
| `build-logic` | Convention plugin dùng chung cho mọi module |

Quy tắc phụ thuộc (ADR-0001, kiểm tra tự động bằng `checkModuleDependencies` cho mọi module): feature chỉ phụ thuộc core, không bao giờ phụ thuộc feature khác; core chỉ phụ thuộc core; không module nào phụ thuộc `:app`. Cần dùng chung giữa các feature thì đề xuất đưa vào một module core.

## Lệnh

- Kiểm tra toàn bộ, giống hệt CI: `scripts/check.sh`.
- Ghi lại ảnh chuẩn screenshot khi plan cho phép: `./gradlew recordRoborazziDebug`.
- Môi trường cloud chưa có `ANDROID_HOME` thì đặt `export ANDROID_HOME=$HOME/android-sdk` (SDK do `scripts/cloud-setup.sh` cài).
- `app/src/staging/google-services.json` là file cấu hình Firebase giả, chỉ để build qua được; bản build từ file này không gửi số liệu nào (ADR-0012). Không sửa, không thay bằng file thật.
- Môi trường cloud không có emulator. Test trên emulator (`app/src/androidTest`) chỉ chạy trong CI.

## Viết một feature mới

Làm theo đúng mẫu `:feature:sample`:

1. `build.gradle.kts` chỉ áp dụng plugin `template.android.feature`, khai báo `namespace`.
2. `data/`: interface repository và lớp cài đặt `internal`, gắn bằng một Hilt module trong `di/`.
3. `<Ten>UiState`: sealed interface mô tả mọi trạng thái màn hình.
4. `<Ten>ViewModel`: nhận repository, `Analytics`, `FeatureFlags`; trả `StateFlow<UiState>`; hành động của người dùng là hàm thường.
   - `FeatureFlags.enabled()` là một `Flow`: theo dõi nó, không đọc một lần (ADR-0003).
   - Ghi dữ liệu có thể ném `IOException`: bắt lỗi trong ViewModel, đưa vào `UiState` để màn hình báo cho người dùng, không để app dừng đột ngột. Luồng đọc DataStore tự thử lại khi gặp `IOException`.
5. `<Ten>Screen.kt`: một composable `<Ten>Route` nối với ViewModel, một composable `<Ten>Screen` không trạng thái nhận `uiState` và các callback.
6. `navigation/`: một đích `@Serializable` và hàm mở rộng `NavGraphBuilder.<ten>Screen()`. Đăng ký trong `AppNavHost` của `:app`.
7. Cờ tính năng mới khai báo trong `FeatureFlag` của `:core:config`, luôn mặc định tắt. Muốn thấy feature trên bản staging thì thêm cờ vào `StagingFeatureFlagsModule` trong `app/src/staging`.
8. Test, viết trước code (docs/auto-run.md, bước 3):
   - Unit test cho ViewModel, dùng repository giả và `MainDispatcherRule`.
   - Test giao diện cho `<Ten>Screen` chạy bằng Robolectric.
   - Screenshot test bằng Roborazzi. Mỗi lệnh `captureRoboImage` ghi rõ đường dẫn, dạng `"src/test/screenshots/<Ten>.png"`; `scripts/check.sh` báo lỗi nếu thiếu.

## Quy ước code

- Tên lớp, hàm, biến và chú thích trong code viết bằng tiếng Anh, kể cả chú thích trong script và workflow. Thông báo mà script in ra cho nhóm và tài liệu trong `docs/` viết bằng tiếng Việt.
- Chữ hiển thị cho người dùng đặt trong `res/values/strings.xml`, không viết cứng trong code.
- Màu, chữ, khoảng cách lấy từ `:core:designsystem` (`AppTheme`, `MaterialTheme`, `Spacing`). Không viết cứng mã màu hay cỡ chữ.
- Feature không gọi trực tiếp SDK quảng cáo, phân tích hay Remote Config; luôn đi qua `:core:ads`, `:core:analytics`, `:core:config`.
- Định dạng code theo ktlint (kiểu Android Studio, xem `.editorconfig`).

## Không được làm

- Không sửa `.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/` hay `gradle/libs.versions.toml`, trừ khi đó chính là nội dung của một plan rủi ro cao đã được duyệt chéo.
- Không thêm quyền truy cập vào `AndroidManifest.xml`, không thêm SDK, vị trí quảng cáo hay sự kiện phân tích ngoài spec đã duyệt.
- Không merge bằng cách nào khác ngoài `scripts/merge-if-green.sh`. Không đóng, sửa hay bình luận để gỡ issue mang nhãn `main-broken`.
- Không đọc, in ra hay chuyển đi bí mật của repo. Không tạo hay sửa cấu hình production (ADR-0007, ADR-0009).
- Không làm cho test qua bằng cách sửa test, xóa test, cập nhật ảnh chuẩn screenshot ngoài plan, hay tắt quy tắc lint.
