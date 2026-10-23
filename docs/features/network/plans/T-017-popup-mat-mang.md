---
ticket: T-017
title: Popup mất mạng chung và chặn vào app khi không có mạng
status: approved            # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (analytics, core, build)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-21)
blocking_points: [analytics, core, build]
run_order: 10               # AI điền sau họp đánh giá rủi ro
reviewer: Nhật              # người duyệt
cross_reviewer: Nhật        # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-017: POPUP MẤT MẠNG CHUNG VÀ CHẶN VÀO APP KHI KHÔNG CÓ MẠNG

## 1. Tóm tắt

`:app` thêm một ViewModel cấp app ghép ba nguồn: trạng thái mạng của `NetworkMonitor` (T-016), `AdsController.fullScreenAdShowing` và màn hình hiện tại của `NavController`. Khi mất mạng, không ở màn hình loading và không có quảng cáo toàn màn hình, một `Dialog` theo thiết kế `NetworkLostScreen` hiện đè lên app, không đóng được bằng tay, có nút "Thử lại", tự đóng khi có mạng; hai sự kiện `network_lost_shown` và `network_restored` ghi qua `Analytics`. Kiểm chứng bằng unit test cho mọi nhánh của ViewModel (nguồn giả, thời gian ảo), test giao diện và screenshot test trên JVM trong `:app` (tên trùng ảnh thiết kế), và kiểm tra thủ công trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi màn hình loading bật và không có mạng thì màn hình loading báo lỗi và có nút "Thử lại"; popup mất mạng không hiện chồng lên. | Popup không hiện khi đích hiện tại là `LoadingDestination`. ViewModel bắt đầu ở trạng thái "đang ở loading" (đích đầu tiên của `AppNavHost`), nên trước khi biết đích thì không hiện. |
| KB-1 | Khi màn hình loading tắt và không có mạng thì popup mất mạng hiện ngay trên màn hình đầu tiên và chặn thao tác. | Loading tắt thì `LoadingViewModel` chuyển ngay sang `Finished` và app đi tới `SampleDestination`. Lần đầu rời loading, ViewModel gọi `NetworkMonitor.checkNow()` một lần; kết quả `false` hiện popup ngay, không chờ khoảng `network_lost_delay_seconds` (khoảng chờ chỉ để tránh mạng chập chờn lúc đang dùng; câu hỏi 2). |
| KB-1 | Khi Wi-Fi bật nhưng không vào được Internet thì app coi là không có mạng. | Do T-016 (`NET_CAPABILITY_VALIDATED`). T-017 chỉ dùng `isOnline` và `checkNow()`, không đọc trạng thái mạng của hệ thống. |
| KB-2 | Khi đang dùng mà mất Internet thì sau khoảng chờ ngắn (mặc định 2 giây) popup hiện: tiêu đề, lời giải thích, nút "Thử lại". | Khoảng chờ nằm trong `NetworkMonitor.isOnline` (T-016). Khi flow phát `false`, popup hiện ngay. Chữ theo bảng chữ của thiết kế. |
| KB-2 | Khi popup đang hiện thì không chạm được vào màn hình bên dưới; chạm ra ngoài hay bấm nút quay lại không đóng popup. | `Dialog` là cửa sổ riêng, có lớp phủ, chặn chạm xuống cửa sổ app. `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`, `onDismissRequest` không làm gì. |
| KB-2 | Khi bấm "Thử lại" mà vẫn không có mạng thì popup vẫn hiện và cho biết vẫn chưa có mạng. | `onRetry()` gọi `checkNow()`; trong lúc kiểm tra nút bị tắt; kết quả `false` thì popup thêm một dòng "vẫn chưa có mạng" (chữ chưa có trong thiết kế, câu hỏi 1). |
| KB-2 | Khi có mạng trở lại thì popup tự đóng và người dùng ở đúng màn hình, đúng trạng thái trước đó. | Popup không chạm vào điều hướng hay trạng thái của feature; chỉ thôi vẽ `Dialog`. `checkNow()` trả `true` cũng đóng popup ngay, không chờ flow. |
| KB-2, đặc biệt | Khi một quảng cáo toàn màn hình đang hiện thì popup không chen vào; nếu vẫn mất mạng khi quảng cáo đóng thì popup hiện ngay. | Điều kiện hiện có thêm `!fullScreenAdShowing`. Flow về `false` khi quảng cáo đóng, popup hiện ngay nếu vẫn mất mạng. |
| KB-2, đặc biệt | Khi app ở nền rồi quay lại mà không có mạng thì popup hiện. | ViewModel sống theo Activity, vẫn theo dõi `isOnline` khi app ở nền; quay lại thì `Dialog` đang trong cây giao diện hiện ra. Thêm: mỗi lần Activity vào `onStart` (trừ lần đầu), ViewModel gọi `checkNow()` để không phải chờ khoảng trễ. |
| KB-3 | Khi vẫn có mạng nhưng máy chủ lỗi hay quá thời gian thì màn hình của feature báo lỗi như trước; popup không hiện. | Popup chỉ dựa vào `NetworkMonitor`, không dựa vào lỗi tải. Có mạng thì popup không hiện, dù feature đang báo lỗi. |

Sự kiện phân tích (spec mục 6):

- `network_lost_shown`, tham số `screen`: ghi mỗi lần popup chuyển từ ẩn sang hiện. `screen` là tên đích hiện tại rút từ `route` của Navigation: phần sau dấu chấm cuối, bỏ hậu tố `Destination`, đổi sang chữ thường có gạch dưới (ví dụ `SampleDestination` thành `sample`); không rõ thì `unknown`. Không có dữ liệu cá nhân.
- `network_restored`, tham số `duration_ms` (thời gian popup hiện, đồng hồ đơn điệu) và `retry_count` (số lần bấm "Thử lại" trong lần hiện đó): ghi khi popup đóng vì có mạng (flow phát `true` hoặc `checkNow()` trả `true`). Popup bị ẩn vì quảng cáo toàn màn hình hay vì quay về loading thì không ghi (không phải "tự đóng vì có mạng").

Thiết kế tham chiếu: `features/network/design/README.md`, các ảnh trong `design/screens/`: `NetworkLostScreen_default_light.png`, `NetworkLostScreen_default_dark.png`; số liệu trong `design/context/` cùng tên.

ADR liên quan: ADR-0024 (quyết định 3, 4), ADR-0013 (quảng cáo toàn màn hình, không gọi SDK ngoài `:core:ads`), ADR-0019 (log), ADR-0020 (tên screenshot test, đối chiếu thiết kế), ADR-0022, ADR-0023 (khóa `network_lost_delay_seconds` do T-016 khai báo), ADR-0001 (`:app` ghép feature và core), ADR-0017 (`scripts/new-app.sh` sửa `app_name` trong `strings.xml`).

Giả định về các task chạy trước (giữ đúng tên của hợp đồng chung):

- T-016 đã merge: `interface NetworkMonitor { val isOnline: Flow<Boolean>; suspend fun checkNow(): Boolean }` có binding Hilt, ở `:core:data` hoặc module mới `:core:network` theo plan T-016. `isOnline` đã gồm khoảng chờ `network_lost_delay_seconds`.
- T-009 đã merge: `AdsController` có `awaitFullScreenTurn`, `onFullScreenShown`, `onAppForeground`, `onAppBackground`; `MainActivity` gọi hai hàm sau trong `onStart`, `onStop`.
- `AdsController.fullScreenAdShowing: StateFlow<Boolean>` (từ `FullScreenAdPresence.showing`, cùng tên lớp và hàm với plan T-013) thêm ở T-013 hoặc T-017, task nào chạy trước. Plan này **thêm nếu chưa có**.
- PR #81 đã merge: `scripts/check.sh` duyệt cả `app/`, so ảnh chuẩn của `:app` bằng `verifyRoborazziStagingDebug` (đọc `src/test` và `src/testStaging`).

## 3. Giải pháp

### 3.1. Cách làm

**Trạng thái quảng cáo toàn màn hình trong `:core:ads`** (thêm nếu T-013 chưa thêm):

- Dùng đúng lớp mà plan T-013 mô tả, để task nào chạy trước cũng tạo ra cùng một file: lớp thuần `internal class FullScreenAdPresence` trong `FullScreenAdPresence.kt` với `val showing: StateFlow<Boolean>`, `fun onShowing()` (đặt `true`), `fun onClosed()` (đặt `false`, gọi nhiều lần không sao). Không phụ thuộc Android, test bằng unit test. Việc chống hai quảng cáo toàn màn hình chồng nhau do cờ `fullScreenInProgress` của T-009 lo, không phải lớp này.
- `AdsController` giữ một `FullScreenAdPresence` (`presence`) và công khai `val fullScreenAdShowing: StateFlow<Boolean> = presence.showing` (KDoc: true khi interstitial hay native toàn màn hình đang hiện). Nối interstitial như plan T-013: ngay trước `show()` gọi `presence.onShowing()`; `onAdDismissedFullScreenContent` và `onAdFailedToShowFullScreenContent` gọi `presence.onClosed()`. Không đổi gì khác trong `AdsController` (native toàn màn hình do T-013 nối vào sau).
- Nếu T-013 đã thêm `FullScreenAdPresence` và `fullScreenAdShowing` đúng tên và kiểu hợp đồng, bỏ phần này, không tạo lại file và test; ghi vào nhật ký.

**Cầu nối trong `:app`** (gói `com.vglstudio.template.network`):

- `fun interface FullScreenAdStatus { fun showing(): Flow<Boolean> }`, cài bằng `AdsController.fullScreenAdShowing`. ViewModel nhận interface này thay vì `AdsController` (lớp gắn SDK, không dựng được trong unit test).
- `fun interface NetworkClock { fun elapsedRealtimeMillis(): Long }`, cài bằng `SystemClock.elapsedRealtime()`, theo mẫu `LoadingClock`.
- Hilt module `app/.../di/NetworkLostModule.kt` (`@InstallIn(SingletonComponent::class)`) cung cấp hai interface trên.
- `fun screenNameOf(route: String?): String` (hàm thuần trong `ScreenName.kt`): bỏ phần sau `?` và `/`, lấy phần sau dấu chấm cuối, bỏ hậu tố `Destination`, đổi CamelCase sang snake_case, cắt tối đa 100 ký tự; `null` hay rỗng thì `unknown`.

**`NetworkLostUiState`** (sealed interface):

- `Hidden`.
- `Shown(checking: Boolean = false, stillOffline: Boolean = false)`: `checking` khi đang chạy `checkNow()` (nút tắt), `stillOffline` sau một lần thử lại thất bại.

**`NetworkLostViewModel`** (`@HiltViewModel`, theo mẫu `LoadingViewModel`), nhận `NetworkMonitor`, `FullScreenAdStatus`, `Analytics`, `AppLogger`, `NetworkClock`:

- Trạng thái trong: `online: MutableStateFlow<Boolean?>` (chưa biết là `null`, coi như có mạng), `onLoadingScreen: MutableStateFlow<Boolean>` (bắt đầu `true`), `screen: String`, `retryCount`, `shownAt`.
- `init`: thu `networkMonitor.isOnline` vào `online`; `combine(online, onLoadingScreen, fullScreenAdStatus.showing())` ra `shouldShow = online == false && !onLoading && !adShowing`.
- Chuyển ẩn sang hiện: `state = Shown()`, `shownAt = clock`, `retryCount = 0`, ghi `network_lost_shown(screen)`, log `info("Network", "Popup shown on <screen>")`.
- Chuyển hiện sang ẩn: nếu lý do là `online == true` thì ghi `network_restored(duration_ms, retry_count)` và log `info("Network", "Popup closed: online after <ms> ms, <n> retries")`; nếu lý do là quảng cáo hay loading thì chỉ log `info("Network", "Popup paused: full-screen ad" | "loading screen")`, không ghi sự kiện.
- `onDestinationChanged(route: String?, isLoading: Boolean)`: cập nhật `screen` và `onLoadingScreen`. Lần đầu `isLoading` thành `false`, gọi `checkNow()` một lần và đưa kết quả vào `online` (KB-1, tiêu chí 2).
- `onRetry()`: chỉ chạy khi `Shown` và không `checking`. `retryCount++`, `state = Shown(checking = true)`, `checkNow()`. `true`: `online = true` (popup đóng, ghi `network_restored`). `false`: `Shown(stillOffline = true)`, log `info("Network", "Retry <n>: still offline")`. Ngoại lệ từ `checkNow()` (trừ `CancellationException`) coi như `false` và log `warn`.
- `onAppForeground()`: từ lần thứ hai trở đi gọi `checkNow()` (KB-2, quay lại từ nền).
- Kết quả của `checkNow()` chỉ ghi vào `online` khi flow chưa phát giá trị mới hơn trong lúc chờ (giữ số thứ tự), để flow luôn thắng kết quả cũ.
- Hằng số: `EVENT_LOST_SHOWN = "network_lost_shown"`, `EVENT_RESTORED = "network_restored"`, `LOG_TAG = "Network"`. Log không chứa tên mạng, địa chỉ hay dữ liệu cá nhân (ADR-0019).

**Giao diện** (`NetworkLostDialog.kt`):

- `@Composable fun NetworkLostDialog(uiState: NetworkLostUiState.Shown, onRetry: () -> Unit)`: `Dialog(onDismissRequest = {}, properties = NetworkLostDialogProperties)`, với `NetworkLostDialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)`. Lớp phủ là lớp phủ mặc định của `Dialog` (thiết kế mục 6 cho phép khác).
- Nội dung `NetworkLostDialogContent(uiState, onRetry, modifier)` (tách ra để test và screenshot): `Surface` màu `colorScheme.surfaceContainerHigh`, `shapes.extraLarge` (28 dp), rộng `DialogWidth = 312.dp` nhưng không vượt màn hình trừ lề `Spacing.lg` mỗi bên (`widthIn(max = ...)`); `Column` lề `Spacing.lg`, cách nhau `Spacing.md`, căn giữa, `verticalScroll` để không bị cắt khi cỡ chữ lớn. Hằng số `DialogWidth` là giá trị "ngoài hệ thống" duy nhất (thiết kế mục 3 cho plan quyết), đặt `private val` trong file, có chú thích nguồn Material 3.
- Các phần tử theo thứ tự: icon `R.drawable.ic_network_off` 24 dp, màu `colorScheme.secondary`, `contentDescription = null`; tiêu đề `headlineSmall`, `onSurface`, căn giữa; nội dung `bodyMedium`, `onSurfaceVariant`, căn giữa; khi `stillOffline`: dòng `network_lost_still_offline`, `bodyMedium`, `colorScheme.error`, căn giữa, `liveRegion = Polite`; nút `PrimaryButton(text = network_lost_retry, enabled = !checking, modifier = fillMaxWidth())`.
- Test tag: `network_lost_dialog`, `network_lost_retry`, `network_lost_still_offline`.
- Icon: vector drawable `ic_network_off.xml` vẽ theo icon "wifi-off" của thiết kế (24 x 24, nét 2), màu do `tint` quyết.

**Ghép vào app:**

- `AppNavHost(navController: NavHostController, modifier)`: nhận `navController` thay vì tự tạo, để `MainActivity` dùng chung với popup. Không đổi đích nào.
- `NetworkLostHost(navController, viewModel)` trong `NetworkLostHost.kt`: theo dõi `navController.currentBackStackEntryAsState()`, gọi `viewModel.onDestinationChanged(route, destination.hasRoute<LoadingDestination>())`; `uiState` qua `collectAsStateWithLifecycle()`; `Shown` thì vẽ `NetworkLostDialog`.
- `MainActivity`: `private val networkLost: NetworkLostViewModel by viewModels()`; trong `setContent`: `val navController = rememberNavController()`, `AppNavHost(navController)`, `NetworkLostHost(navController, networkLost)`; `onStart` gọi thêm `networkLost.onAppForeground()`. Giữ nguyên `gatherConsent`, `onUserInteraction` và các lệnh T-009 thêm.
- Chữ: `app/src/main/res/values/strings.xml` thêm bốn chuỗi tiếng Việt (mặc định, như `:feature:loading`): `network_lost_title` "Không có kết nối mạng", `network_lost_message` "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại.", `network_lost_retry` "Thử lại", `network_lost_still_offline` (câu hỏi 1). Dòng `app_name` giữ nguyên từng ký tự vì `scripts/new-app.sh` thay đúng dòng đó (ADR-0017); test của script ở `scripts/tests/run.sh` vì vậy không bị ảnh hưởng. `app/src/main/res/values-en/strings.xml` mới có bốn chuỗi tiếng Anh: "No internet connection", "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.", "Try again", và bản tiếng Anh của câu hỏi 1. Không đưa `app_name` vào `values-en` (new-app.sh không sửa file đó); xem câu hỏi 3 về lint.

**Hạ tầng test JVM của `:app`** (lần đầu `:app` có test trên JVM):

- `app/build.gradle.kts`: thêm plugin `alias(libs.plugins.roborazzi)` và khối `roborazzi { outputDir.set(layout.projectDirectory.dir("src/test/screenshots")) }` như convention của feature; thêm `implementation(libs.androidx.lifecycle.runtime.compose)`, `testImplementation(libs.roborazzi)`, `testImplementation(libs.roborazzi.compose)`, `testImplementation(libs.roborazzi.junit.rule)`; thêm `implementation(project(":core:network"))` (plan T-016 chọn module mới `:core:network`, nếu `:app` chưa phụ thuộc). Mọi alias đã có trong `gradle/libs.versions.toml`; không sửa file đó hay `build-logic/`. Robolectric, Compose test, coroutines test và Turbine đã có qua convention `template.android.compose` và `KotlinAndroid.kt`.
- `app/src/test/resources/robolectric.properties`: `sdk=35` và `application=android.app.Application`, để Robolectric không dựng `TemplateApplication` (Hilt, Firebase) trong test giao diện.
- `MainDispatcherRule` chép sang `app/src/test` (mỗi module một bản, như `:feature:loading`).
- Screenshot test dùng `composeRule.onRoot().captureRoboImage("src/test/screenshots/<Tên>.png")`, không dùng `captureScreenRoboImage`: `scripts/check.sh` chỉ nhận lệnh `captureRoboImage("src/test/screenshots/...")` để biết module có ảnh chuẩn và chạy `verifyRoborazziStagingDebug`; gọi `captureScreenRoboImage` thì CI không so ảnh nào của `:app`. `onRoot()` chỉ chụp cửa sổ chính, nên screenshot test không mở cửa sổ `Dialog` mà dựng cùng bố cục trong một cửa sổ: `setContent { AppTheme { Box(Modifier.fillMaxSize()) { SampleScreen(SampleUiState.Ready(count = 3), onIncrement = {}); Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))); NetworkLostDialogContent(uiState, onRetry = {}, Modifier.align(Alignment.Center)) } } }` (lớp phủ đen 32% theo thiết kế mục 3; hàm dựng này nằm trong file test). Cửa sổ `Dialog` thật (không đóng được, chặn quay lại, kích thước) kiểm ở `NetworkLostDialogTest`. `SampleScreen` của `:feature:sample` đổi từ `internal` sang công khai (thiết kế README mục 1: dựng popup đè lên `SampleScreen`); không đổi nội dung màn hình. `:app` đã phụ thuộc `:feature:sample` nên không đổi quy tắc module.

**Ảnh chuẩn và đối chiếu thiết kế:**

- Sau PR #81, `scripts/check.sh` duyệt cả `app/`: thấy `captureRoboImage("src/test/screenshots/...")` trong `app/src/test` thì chạy `:app:verifyRoborazziStagingDebug` trong CI, và trên máy tự ghi ảnh còn thiếu bằng `:app:recordRoborazziStagingDebug` mà không ghi đè ảnh đã có. Plan không sửa `scripts/check.sh`. T-017 cần PR #81 merge trước (mục 6, mục 7).
- Script đối chiếu thiết kế của repo đọc ảnh ở `feature/<feature>/src/test/screenshots/`, không có thư mục `feature/network`. Phiên tự chạy gọi thẳng chương trình so ảnh với thư mục của `:app`, không sửa script: `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/network/design/screens app/src/test/screenshots docs/features/network/design/review`. Commit `docs/features/network/design/review/`, ghi bảng `summary.md` và nhận xét của rà soát độc lập vào `design/README.md` mục 8 và mô tả PR. Phần dưới popup có thể khác thiết kế ở vùng quảng cáo native (test không có `AdsController`, ADR-0013); ghi rõ trong mục 8.

**Commit test trước (bước 3, L-025):** commit test chứa mọi file test và bản giả trong `app/src/test` và `core/ads/src/test`, `robolectric.properties`, phần test của `app/build.gradle.kts` (plugin Roborazzi, `testImplementation`), cùng khung rỗng để test biên dịch được: `NetworkLostUiState` đầy đủ, `NetworkLostViewModel` luôn `Hidden` và không ghi gì, `screenNameOf` trả `unknown`, `NetworkLostDialog` và `NetworkLostDialogContent` rỗng, `FullScreenAdPresence` với `showing` luôn `false`, `FullScreenAdStatus`, `NetworkClock`, bốn chuỗi trong `strings.xml` (cả hai ngôn ngữ, để test chữ thất bại ở phần kiểm tra chứ không ở biên dịch, vì chữ là tài nguyên), và `SampleScreen` công khai. Test thất bại ở phần kiểm tra. Commit code ở bước 4 viết phần thật, icon, Hilt module, nối vào `AdsController`, `AppNavHost`, `MainActivity`, rồi ghi ảnh chuẩn.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresence.kt` | Lớp thuần giữ trạng thái quảng cáo toàn màn hình đang hiện, cùng API với plan T-013 (bỏ nếu T-013 đã tạo) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `fullScreenAdShowing: StateFlow<Boolean>`; interstitial gọi `presence.onShowing()` ngay trước `show()`, `presence.onClosed()` khi đóng hay lỗi hiện (bỏ nếu T-013 đã có) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresenceTest.kt` | Test của `FullScreenAdPresence`, cùng tên test với plan T-013 (bỏ nếu T-013 đã tạo) |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/NetworkLostUiState.kt` | `Hidden`, `Shown(checking, stillOffline)` |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/NetworkLostViewModel.kt` | Ghép mạng, quảng cáo, màn hình; thử lại; hai sự kiện; log; `NetworkClock` |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/FullScreenAdStatus.kt` | `fun interface FullScreenAdStatus` |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/ScreenName.kt` | Hàm thuần `screenNameOf(route)` |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/NetworkLostDialog.kt` | `NetworkLostDialog`, `NetworkLostDialogContent`, `NetworkLostDialogProperties`, test tag |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/NetworkLostHost.kt` | Nối `NavController` và ViewModel với `NetworkLostDialog` |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/di/NetworkLostModule.kt` | Hilt: `FullScreenAdStatus` từ `AdsController`, `NetworkClock` từ `SystemClock` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | ViewModel popup, `rememberNavController` dùng chung, `NetworkLostHost`, `onStart` gọi `onAppForeground` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/navigation/AppNavHost.kt` | Nhận `navController` làm tham số |
| Tạo | `app/src/main/res/drawable/ic_network_off.xml` | Icon mất wifi 24 dp |
| Sửa | `app/src/main/res/values/strings.xml` | Bốn chuỗi tiếng Việt; dòng `app_name` giữ nguyên |
| Tạo | `app/src/main/res/values-en/strings.xml` | Bốn chuỗi tiếng Anh |
| Sửa | `app/build.gradle.kts` | Plugin và cấu hình Roborazzi, `lifecycle-runtime-compose`, thư viện test Roborazzi, `implementation(project(":core:network"))` nếu T-016 chưa thêm |
| Sửa | `feature/sample/src/main/kotlin/com/vglstudio/template/feature/sample/SampleScreen.kt` | `SampleScreen` từ `internal` sang công khai, KDoc ghi lý do; không đổi nội dung |
| Tạo | `app/src/test/resources/robolectric.properties` | `sdk=35`, `application=android.app.Application` |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/MainDispatcherRule.kt` | Chép từ `:feature:loading` |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkTestDoubles.kt` | `FakeNetworkMonitor` riêng của test `:app` (flow chưa phát hay đặt được, `checkNow` trả kết quả đặt trước, treo tới khi thả, hay ném lỗi; plan T-016 không cung cấp bản giả công khai), `FakeFullScreenAdStatus`, `FakeAnalytics` |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostViewModelTest.kt` | Unit test ViewModel |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/ScreenNameTest.kt` | Unit test `screenNameOf` |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostDialogTest.kt` | Test giao diện Robolectric |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostScreenScreenshotTest.kt` | Screenshot test Roborazzi |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_default_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_default_dark.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_stillOffline_light.png` | Ảnh chuẩn trạng thái thử lại thất bại |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_stillOffline_small.png` | Ảnh chuẩn khung thấp, trạng thái nhiều nội dung nhất (L-028) |
| Tạo | `docs/features/network/design/review/` | Ảnh ghép `*_compare.png` và `summary.md` sinh bởi `DesignCompare.java` |
| Sửa | `docs/features/network/design/README.md` | Mục 8 Kết quả đối chiếu; hai ô còn trống của mục 7 sau khi kiểm |
| Sửa | `docs/features/network/tickets/T-017-popup-mat-mang.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/network/plans/T-017-popup-mat-mang.md` | Nhật ký thực hiện |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Không sửa: `scripts/check.sh` (đã sửa ở PR #81) và script đối chiếu thiết kế, `gradle/libs.versions.toml`, `build-logic/`, `AndroidManifest.xml` (không thêm quyền; quyền mạng nếu cần thuộc T-016), `feature/loading/` (popup dựa vào đích điều hướng, không cần loading báo gì thêm), `app/src/androidTest/` (emulator CI có mạng nên `AppLaunchTest` không đổi; nếu popup hiện thì test vẫn qua vì tìm `sample_screen` trong cây giao diện), `docs/remote-config-keys.md` và `docs/remote-config-template.json` (T-017 không thêm khóa; `network_lost_delay_seconds` do T-016 khai báo và sinh lại), `AdSlot.kt`, `AdPlacement.kt` (không vị trí mới).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có (spec mục 8, ADR-0024: áp dụng cho mọi app). Khóa `network_lost_delay_seconds` thuộc T-016, T-017 không đọc trực tiếp.
- Quảng cáo: không thêm, bớt, di chuyển vị trí hay đổi tần suất. Chỉ thêm trạng thái chỉ đọc `fullScreenAdShowing` trong `:core:ads` (thêm nếu chưa có). Popup không có quảng cáo.
- Sự kiện phân tích: `network_lost_shown` (`screen`) và `network_restored` (`duration_ms`, `retry_count`), đúng spec mục 6 (câu hỏi 2 của spec đã duyệt). Ghi qua `Analytics` của `:core:analytics`.
- Dữ liệu trên máy: không có.
- File R2: Không chạm.

## 4. Kiểm thử

Unit test chạy trên JVM với `MainDispatcherRule`, `runTest` và thời gian ảo (`clock = { testScheduler.currentTime }`). Mặc định trong test: `FakeNetworkMonitor` chưa phát gì, `checkNow()` trả `false`; `FakeFullScreenAdStatus` phát `false`; ViewModel đã nhận `onDestinationChanged("com.vglstudio.template.feature.sample.navigation.SampleDestination", isLoading = false)` trừ khi test ghi khác. Sự kiện so bằng `AnalyticsEvent(name, params)` đầy đủ.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Unit test | `NetworkLostViewModelTest.onLoadingScreen_offline_staysHidden`: `onDestinationChanged(loading route, isLoading = true)`, flow phát `false`, trạng thái `Hidden`, không có sự kiện |
| KB-1, tiêu chí 1 | Unit test | `NetworkLostViewModelTest.beforeAnyDestination_staysHidden`: chưa gọi `onDestinationChanged`, flow phát `false`, `Hidden` |
| KB-1, tiêu chí 2 | Unit test | `NetworkLostViewModelTest.leavingLoadingOffline_showsAtOnceWithoutWaitingForFlow`: flow chưa phát, `checkNow` trả `false`, `onDestinationChanged(sample, false)`, ngay sau `runCurrent()` là `Shown()`; `checkNow` được gọi đúng một lần |
| KB-1, tiêu chí 2 | Unit test | `NetworkLostViewModelTest.leavingLoadingOnline_staysHidden`: `checkNow` trả `true`, `Hidden` |
| KB-1, tiêu chí 2 | Unit test | `NetworkLostViewModelTest.checkNowOnlyOnFirstLeaveOfLoading`: đổi đích ba lần, `checkNow` gọi một lần |
| KB-1, tiêu chí 3 | Do T-016 | Test của T-016 (Wi-Fi chưa xác nhận là mất mạng); T-017 chỉ dùng `isOnline` |
| KB-2, tiêu chí 1 | Unit test | `NetworkLostViewModelTest.flowReportsOffline_showsPopup`: flow phát `true` rồi `false`, `Shown()` |
| KB-2, tiêu chí 1 (khoảng chờ) | Do T-016 | Khoảng chờ nằm trong `isOnline`; T-017 không chờ thêm: `NetworkLostViewModelTest.noExtraDelayAfterFlow`: flow phát `false`, `Shown()` sau `runCurrent()`, không `advanceTimeBy` |
| KB-2, tiêu chí 1 (chữ) | Compose UI test | `NetworkLostDialogTest.default_showsTitleMessageAndRetry_vi`: có đúng ba chữ của bảng chữ tiếng Việt; không có `network_lost_still_offline` |
| KB-2, tiêu chí 1 (chữ tiếng Anh) | Compose UI test | `NetworkLostDialogTest.default_showsEnglishText`: `@Config(qualifiers = "en-w411dp-h891dp-xxhdpi")`, có "No internet connection", câu nội dung tiếng Anh, "Try again" |
| KB-2, tiêu chí 2 | Compose UI test | `NetworkLostDialogTest.dialog_cannotBeDismissedByUser`: `NetworkLostDialogProperties.dismissOnBackPress == false`, `dismissOnClickOutside == false`; dựng `NetworkLostDialog` trên nội dung có `BackHandler` đếm số lần, `Espresso.pressBackUnconditionally()`, dialog vẫn còn (`isDialog()` tồn tại) và bộ đếm của nội dung dưới vẫn 0 |
| KB-2, tiêu chí 2 (chặn chạm) | Kiểm tra thủ công | Test Compose trên JVM bơm chạm thẳng vào nút, bỏ qua cửa sổ, nên không kiểm được việc `Dialog` chặn chạm. Bản staging: tắt Wi-Fi và dữ liệu di động ở màn hình mẫu, chờ popup; chạm nút "Tap" phía sau, chạm vùng tối, bấm quay lại: số đếm không đổi, popup còn |
| KB-2, tiêu chí 3 | Unit test | `NetworkLostViewModelTest.retryStillOffline_keepsPopupAndSaysSo`: `Shown()`, `onRetry()` với `checkNow` treo: `Shown(checking = true)`; thả với `false`: `Shown(stillOffline = true)`; có log `INFO` thẻ `Network` chứa `still offline` |
| KB-2, tiêu chí 3 | Unit test | `NetworkLostViewModelTest.retryWhileChecking_isIgnored`: hai lần `onRetry()` khi đang kiểm tra, `checkNow` gọi một lần, `retry_count` cuối là 1 |
| KB-2, tiêu chí 3 | Unit test | `NetworkLostViewModelTest.retryThrows_isTreatedAsOffline`: `checkNow` ném `IllegalStateException`, `Shown(stillOffline = true)`, có log `WARN`, không ném ra ngoài |
| KB-2, tiêu chí 3 (giao diện) | Compose UI test | `NetworkLostDialogTest.stillOffline_showsMessage`: `Shown(stillOffline = true)` có chữ `network_lost_still_offline`; `NetworkLostDialogTest.checking_disablesRetry`: `Shown(checking = true)`, nút `network_lost_retry` `assertIsNotEnabled()`; `NetworkLostDialogTest.retryClick_callsOnRetry`: bấm nút, callback chạy một lần |
| KB-2, tiêu chí 4 | Unit test | `NetworkLostViewModelTest.flowReportsOnline_closesPopup`: `Shown`, flow phát `true`, `Hidden` |
| KB-2, tiêu chí 4 | Unit test | `NetworkLostViewModelTest.retrySucceeds_closesPopupWithoutWaitingForFlow`: `onRetry()`, `checkNow` trả `true`, `Hidden` dù flow vẫn là `false` |
| KB-2, tiêu chí 4 | Unit test | `NetworkLostViewModelTest.staleCheckResult_doesNotOverrideNewerFlowValue`: `onRetry()` treo; flow phát `true` rồi `false`; thả `checkNow` với `true`; trạng thái `Shown` (flow mới hơn thắng) |
| KB-2, tiêu chí 4 (đúng trạng thái trước) | Compose UI test | `NetworkLostDialogTest.closingPopup_keepsUnderlyingState`: nội dung dưới có bộ đếm `rememberSaveable` tăng lên 3, hiện rồi ẩn popup bằng biến trạng thái, bộ đếm vẫn 3 và nội dung không bị dựng lại từ đầu |
| KB-2, đặc biệt: quảng cáo | Unit test | `NetworkLostViewModelTest.fullScreenAdShowing_holdsPopup`: quảng cáo `true`, flow phát `false`, `Hidden`, không có sự kiện; quảng cáo về `false`, `Shown()` ngay, một sự kiện `network_lost_shown` |
| KB-2, đặc biệt: quảng cáo | Unit test | `NetworkLostViewModelTest.adStartsWhileShown_hidesWithoutRestoredEvent`: `Shown`, quảng cáo `true`, `Hidden`, không có `network_restored` |
| KB-2, đặc biệt: quảng cáo (nguồn) | Unit test, Turbine | `FullScreenAdPresenceTest.startsHidden`, `onShowing_setsTrue`, `onClosed_setsFalse`, `closedTwice_staysFalse`, `showingFlow_emitsShowAndClose` (phát `false`, `true`, `false`; cùng tên với plan T-013)
| KB-2, đặc biệt: về từ nền | Unit test | `NetworkLostViewModelTest.appForeground_checksNowExceptFirstStart`: `onAppForeground()` lần đầu không gọi `checkNow`; lần hai gọi, trả `false`, `Shown()` |
| KB-2, đặc biệt: về từ nền (máy) | Kiểm tra thủ công | Bản staging: ở màn hình mẫu, về màn hình chính, bật chế độ máy bay, mở lại app: popup hiện |
| KB-3 | Unit test | `NetworkLostViewModelTest.online_neverShowsPopup`: flow phát `true`, `checkNow` trả `true`, đổi đích, `onAppForeground()`: luôn `Hidden`, không có sự kiện |
| Sự kiện `network_lost_shown` | Unit test | `NetworkLostViewModelTest.shown_logsLostEventWithScreen`: một sự kiện `AnalyticsEvent("network_lost_shown", mapOf("screen" to "sample"))`; hiện lại lần hai sau khi đóng ghi thêm một sự kiện |
| Sự kiện `network_restored` | Unit test | `NetworkLostViewModelTest.restored_logsDurationAndRetries`: hiện lúc t, hai lần thử lại thất bại, flow phát `true` lúc t + 12_000: một sự kiện `AnalyticsEvent("network_restored", mapOf("duration_ms" to "12000", "retry_count" to "2"))` |
| Sự kiện không ghi khi ẩn vì loading | Unit test | `NetworkLostViewModelTest.backToLoading_hidesWithoutRestoredEvent` |
| Log không có dữ liệu cá nhân | Unit test | `NetworkLostViewModelTest.logsUseNetworkTag`: mọi dòng của `RecordingAppLogger` có thẻ `Network`; dòng hiện popup chứa `sample` |
| Tên màn hình | Unit test | `ScreenNameTest`: `...navigation.SampleDestination` ra `sample`; `...LoadingDestination` ra `loading`; `com.x.AdGalleryDestination` ra `ad_gallery`; route có tham số `com.x.DetailDestination/{id}?tab={tab}` ra `detail`; `null` và `""` ra `unknown`; chuỗi 300 ký tự cắt còn 100 |
| Đúng thiết kế, sáng | Screenshot test | `NetworkLostScreenScreenshotTest.default_light`, khung `w411dp-h891dp-xxhdpi`, `@GraphicsMode(NATIVE)`: `SampleScreen(Ready(3))`, lớp phủ 32% và `NetworkLostDialogContent(Shown())` (mục 3.1), `onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_default_light.png")` |
| Đúng thiết kế, tối | Screenshot test | `NetworkLostScreenScreenshotTest.default_dark`: như trên, `AppTheme(darkTheme = true)`, `NetworkLostScreen_default_dark.png` |
| Thử lại thất bại | Screenshot test | `NetworkLostScreenScreenshotTest.stillOffline_light`: `Shown(stillOffline = true)`, `NetworkLostScreen_stillOffline_light.png` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `NetworkLostScreenScreenshotTest.stillOffline_small`: `Shown(stillOffline = true)`, `NetworkLostScreen_stillOffline_small.png`; nút "Thử lại" thấy trọn, không cắt |
| Màn hình thấp (đo) | Compose UI test | `NetworkLostDialogTest.smallScreen_retryFullyVisible`: `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`, `Shown(stillOffline = true)`: nút `network_lost_retry` `assertIsDisplayed()`, đáy nút nằm trong đáy hộp thoại; rộng hộp thoại `network_lost_dialog` là 312 dp ở khung 411 dp và không quá 312 dp ở khung 360 dp |
| Kích thước theo thiết kế | Compose UI test | `NetworkLostDialogTest.layout_matchesDesignNumbers`: `@GraphicsMode(NATIVE)`, khung chuẩn: rộng hộp thoại 312 dp, nút cao ít nhất 52 dp và rộng 264 dp (312 trừ lề 24 x 2) |
| Đối chiếu ảnh thiết kế | Ảnh so sánh, rà soát độc lập | Chạy `DesignCompare.java` với `app/src/test/screenshots` (mục 3.1); rà soát độc lập xem hai ảnh `*_compare.png` theo mục 7 của `design/README.md`, ghi nhận xét vào mục 8 |
| Ảnh chuẩn đúng | `scripts/check.sh` (sau PR #81) | `:app:verifyRoborazziStagingDebug` có trong danh sách tác vụ của `check.sh` và qua, cả trên máy lẫn CI |
| Hilt và khởi động app | Build và test emulator có sẵn | `:app:assembleStagingDebug` qua (đồ thị Hilt đủ); `AppLaunchTest` trong CI qua |
| Toàn bộ trên máy | Kiểm tra thủ công | Bản staging: (1) loading bật, chế độ máy bay, mở app: loading báo lỗi, không popup; (2) loading tắt (cờ cục bộ hay Remote Config staging), chế độ máy bay, mở app: popup ngay trên màn hình mẫu; (3) đang dùng, tắt mạng: popup sau khoảng 2 giây; bấm "Thử lại": dòng "vẫn chưa có mạng"; bật mạng: popup tự đóng, số đếm giữ nguyên; (4) Wi-Fi nối vào điểm phát không có Internet: popup hiện; (5) `adb logcat -v threadtime \| grep ' VGL\.Network'` thấy log hiện, đóng, thử lại; (6) Firebase DebugView không áp dụng cho bản staging với cấu hình giả (ADR-0012), sự kiện kiểm qua unit test |

`MainActivity`, `NetworkLostHost`, `NetworkLostModule` và phần nối trong `AdsController` không có unit test riêng: chúng chỉ nối các phần đã test. Rà soát độc lập đọc kỹ: `navController` dùng chung, `hasRoute<LoadingDestination>()`, chỗ gọi `onShowing`/`onClosed`.

**Ngoại lệ viết test trước:** (1) phần nối nêu trên, kiểm qua build, `AppLaunchTest` và kiểm tra thủ công; (2) icon `ic_network_off.xml` và cấu hình build, kiểm qua screenshot test; (3) chữ trong `strings.xml` vào commit test cùng khung rỗng vì test chữ cần tài nguyên để biên dịch; (4) ảnh chuẩn screenshot ghi sau khi code xong, như mọi screenshot test của dự án. Người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** thu thập dữ liệu (thêm hai sự kiện phân tích, điểm 4), module core dùng chung `:core:ads` (thêm `fullScreenAdShowing`, điểm 7), cấu hình build (`app/build.gradle.kts`: plugin Roborazzi và thư viện, điểm 7). Không chạm vị trí, định dạng hay tần suất quảng cáo, quyền, SDK, hộp thoại đồng ý. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì có sự kiện phân tích mới (đã duyệt trong spec, câu hỏi 2).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba module: `:app` (nhiều file mới, `MainActivity`, `AppNavHost`, build), `:core:ads` (`AdsController`), `:feature:sample` (đổi phạm vi truy cập `SampleScreen`) |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, có thiết kế đã duyệt; AI tự quyết chữ "vẫn chưa có mạng" (không có trong thiết kế), cách hiện ngay khi rời loading, cách đặt tên `screen` (câu hỏi 1, 2, 5) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh logic và chữ có test, ảnh chuẩn của `:app` được CI so (sau PR #81); nhưng việc `Dialog` chặn chạm, quay lại từ nền và tương tác với quảng cáo thật chỉ kiểm thủ công, và screenshot dựng lại lớp phủ thay cho cửa sổ `Dialog` thật |
| D. Ảnh hưởng chéo | 3 | Lớp popup nằm trên mọi màn hình của mọi feature; `AdsController.kt` cũng do T-007, T-009, T-013 sửa; `MainActivity.kt` do T-009 sửa; `SampleScreen.kt` do T-010 sửa |
| E. Độ mới của kỹ thuật | 2 | `Dialog` không đóng được, test trên cửa sổ dialog bằng Robolectric, test JVM và ảnh chuẩn đầu tiên của `:app`: phổ biến nhưng chưa có trong dự án |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (thêm một `StateFlow` và hai lệnh gọi), `MainActivity.kt`, `AppNavHost.kt` (chữ ký đổi, chỉ `MainActivity` gọi), `app/build.gradle.kts`, `app/src/main/res/values/strings.xml`, `SampleScreen.kt`.
- File mà nơi khác đọc theo tên (L-020): `scripts/new-app.sh` thay dòng `app_name` trong `app/src/main/res/values/strings.xml` và sửa `app/build.gradle.kts` (đổi `applicationId`, `namespace`); plan giữ nguyên dòng `app_name` và không đụng các dòng `applicationId`, `namespace`, nên script và test của nó trong `scripts/tests/run.sh` không bị ảnh hưởng. `scripts/release.sh` nhắc tới `app/build.gradle.kts` vì truyền các tham số `-P` của bản production (khóa upload, `admobAppId`, `adUnits`, `assetsBaseUrl`) mà file build đọc; plan không đụng các khối `signingConfigs`, `productFlavors` hay phần kiểm tham số production, chỉ thêm plugin, khối `roborazzi` và dòng phụ thuộc, nên script đó không bị ảnh hưởng (plan không mở hay sửa script này, ADR-0017). `scripts/check.sh` (sau PR #81) đọc `app/src/test` để tìm lệnh `captureRoboImage("src/test/screenshots/...")`: screenshot test của plan theo đúng dạng đó. `docs/STATUS.md` do `scripts/status.py` sinh, `scripts/tests/run.sh` chỉ đọc bản trong thư mục tạm. Ảnh chuẩn mới nằm ở `app/src/test/screenshots/`, không thay ảnh chuẩn nào đã có.
- Task khác có thể xung đột:
  - T-016 (bắt buộc trước): cung cấp `NetworkMonitor` ở module `:core:network` và đã thêm `implementation(project(":core:network"))` vào `app/build.gradle.kts`.
  - PR #81 (bắt buộc trước, không phải ticket): `scripts/check.sh` so ảnh chuẩn của `:app`.
  - T-009: sửa `AdsController.kt` và `MainActivity.kt` (`onStart`, `onStop`). T-017 thêm vào cùng `onStart`.
  - T-013: cũng thêm `fullScreenAdShowing` qua cùng lớp `FullScreenAdPresence` (cùng file, cùng tên test, hợp đồng "thêm nếu chưa có"); task chạy sau bỏ phần này và dùng phần sẵn có. T-013 còn đặt `FullScreenAdHost` ở `:app`, có thể sửa `MainActivity.kt`.
  - T-010: đổi đáy `SampleScreen` sang native; T-017 chỉ đổi `internal` thành công khai. Chạy sau T-010 thì ảnh chuẩn có phần dưới mới nhất.
  - T-014: màn hình xem thử là `AdGalleryActivity` riêng ở `app/src/staging`, không sửa `MainActivity.kt` hay `AppNavHost.kt`; popup mất mạng không có trên màn hình đó (plan T-014, câu hỏi 6). Không xung đột file.
  - T-007, T-015: sửa `AdsController.kt` hay file build của `:core:ads`.
- Đề xuất thứ tự chạy: sau T-016 và PR #81 (bắt buộc), sau T-009 và T-010 nếu cùng phiên; trước hay sau T-013 đều được, không cùng lúc. Nên chạy cuối chuỗi quảng cáo của phiên vì chạm nhiều file chung nhất.

## 7. Điều kiện dừng riêng

- `NetworkMonitor` của T-016 khác hợp đồng (tên, kiểu, `checkNow` không có), hay `isOnline` không gồm khoảng chờ: dừng, không tự viết bộ theo dõi hay khoảng chờ trong `:app`.
- `fullScreenAdShowing` đã có nhưng khác tên hay kiểu hợp đồng.
- Lint `:app:lintStagingDebug` báo `MissingTranslation` hay lỗi khác về chuỗi nằm ngoài các file của plan (ví dụ chuỗi của `:feature:loading`, `:feature:sample` vì `:app` có thư mục `values-en`): dừng, không thêm bản dịch cho module khác, không tắt quy tắc (câu hỏi 3).
- PR #81 chưa merge (`scripts/check.sh` chưa duyệt `app/`): dừng, không tự sửa `check.sh`.
- Ảnh chụp bằng `onRoot().captureRoboImage` không có popup hay lớp phủ sau 3 lần sửa.
- Robolectric không chạy được test giao diện của `:app` mà không sửa `build-logic/` hay `gradle/libs.versions.toml`.
- Cần sửa `scripts/check.sh` hay script đối chiếu thiết kế để task qua.
- Popup cần biết điều gì từ `:feature:loading` ngoài đích điều hướng.

## 8. Câu hỏi cho người duyệt

1. **Chữ khi bấm "Thử lại" mà vẫn chưa có mạng.** Spec KB-2 yêu cầu popup "cho biết vẫn chưa có mạng" nhưng bảng chữ của thiết kế không có dòng này. AI đề xuất: thêm một dòng dưới nội dung, `bodyMedium`, màu `error`, căn giữa: "Vẫn chưa có kết nối. Hãy thử lại sau ít phút." / "Still no connection. Please try again in a moment."; có ảnh chuẩn riêng `NetworkLostScreen_stillOffline_light.png`. Phương án khác: không thêm dòng, chỉ rung nhẹ nút; hoặc bổ sung trạng thái này vào Figma trước.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
2. **Hiện ngay khi rời loading.** `isOnline` của T-016 chỉ báo mất mạng sau 2 giây; khi loading tắt, người dùng có thể thao tác trong 2 giây đầu. AI đề xuất: lần đầu rời màn hình loading và mỗi lần app quay lại từ nền, ViewModel gọi `checkNow()` và hiện popup ngay nếu không có mạng; khoảng chờ chỉ áp dụng khi mạng rớt lúc đang dùng.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
3. **Ngôn ngữ mặc định và lint.** Chuỗi của `:app` hiện tiếng Việt ở `values/` (như `:feature:loading`), tiếng Anh ở `values-en/`. Có `values-en` thì lint có thể đòi bản tiếng Anh cho `app_name` và cho chuỗi của module khác. AI đề xuất: giữ tiếng Việt mặc định; nếu lint chỉ báo `app_name` thì đánh dấu riêng dòng đó là không dịch bằng thuộc tính trên thẻ `<resources>` mà không đổi dòng `app_name` (new-app.sh thay đúng dòng đó); nếu lint báo chuỗi của module khác thì dừng (mục 7) để nhóm chọn chiến lược ngôn ngữ chung cho dự án mẫu. Người duyệt xác nhận ngôn ngữ mặc định của dự án mẫu là tiếng Việt.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
4. **Ảnh chuẩn của `:app` không được CI so.** `scripts/check.sh` chỉ duyệt `core/` và `feature/`. AI đề xuất: chấp nhận cho T-017 (phiên tự chạy so cục bộ bằng `:app:verifyRoborazziStagingDebug` và ghi vào nhật ký), và mở một ticket riêng, rủi ro cao, để `check.sh` duyệt cả `app/` (tên task có biến thể `StagingDebug`) và để script đối chiếu thiết kế nhận thư mục ảnh của `:app`. Phương án khác: gộp sửa `check.sh` vào T-017 (thêm file được bảo vệ vào plan này).
   - Trả lời (anh Nhật, 2026-10-23): `scripts/check.sh` đã sửa ở PR #81, so ảnh chuẩn của `:app` (`verifyRoborazziStagingDebug`, đọc cả `src/testStaging`). Bỏ phần so ảnh trên máy thay cho CI; T-017 cần PR #81 merge trước. Rà soát độc lập ghi thêm: vì `check.sh` chỉ nhận `captureRoboImage("src/test/screenshots/...")`, screenshot test đổi từ `captureScreenRoboImage` sang `onRoot().captureRoboImage` trên bố cục dựng lại (mục 3.1). Script đối chiếu thiết kế vẫn chỉ đọc `feature/<feature>/`, nên phiên tự chạy gọi thẳng `DesignCompare.java` như mục 3.1.
5. **Tham số `screen`.** AI đề xuất rút từ `route` của đích: `sample`, `loading`, `ad_gallery`... Không cần mỗi feature khai báo tên. Đích có tham số chỉ lấy tên đích, không lấy giá trị tham số (tránh dữ liệu cá nhân).
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
6. **`SampleScreen` thành công khai** chỉ để screenshot test của `:app` dựng đúng ảnh thiết kế. Phương án khác: dựng popup trên nền trống (ảnh khác thiết kế ở phần dưới, không phải sửa `:feature:sample`). AI đề xuất giữ cách trong plan vì README thiết kế ghi rõ dựng trên `SampleScreen`.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện một dev đảm nhận cả hai vai)
- **Ghi chú:** anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), sau rà soát độc lập của AI; AI ghi mục này theo lời anh. Câu hỏi ở mục 8 đã có trả lời. Cần PR #81 (`release.sh`, `check.sh`) merge trước phiên chạy.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
