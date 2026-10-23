---
ticket: T-017
title: Popup mất mạng chung và chặn vào app khi không có mạng
status: done                    # draft | approved | stopped | done
version: 3                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (analytics, core, build)
risk_level: high             # low | medium | high
token_estimate: 30M          # cỡ M (gợi ý 20M của scripts/token-usage.py estimate, 2026-10-23), nâng lên: bốn module (:core:network, :core:designsystem, :core:assets, :app), 4 ảnh chuẩn
blocking_points: [analytics, core, build]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-23
---

# PLAN T-017: POPUP MẤT MẠNG CHUNG VÀ CHẶN VÀO APP KHI KHÔNG CÓ MẠNG

**Phiên bản 3 (2026-10-23), đổi so với bản 2 (đã duyệt 2026-10-23, nội dung ở commit `4cb757a`):**

- Theo trả lời của anh Nhật ở mục 8 (2026-10-23): lỗi tải vì mạng hiện popup ngay như app, kể cả khi máy vẫn báo có mạng (spec KB-3 sửa cùng ngày); dùng icon Material Symbols "wifi_off", icon trên Figma đổi theo.

- Lấy code của app camera-pose làm nền (issue #72, PR VGL-Studio/camera-pose#23, merge `e23066751fd93e3f945137558287843b506620b3`): bộ điều khiển popup `NetworkDialogController` nằm ở `:core:network` (thay `NetworkLostViewModel` ở `:app` của bản 2), popup `NetworkDialog` ở `:core:designsystem`, trạng thái chờ của `PrimaryButton`, `NetworkDialogHost` vẽ popup trong cửa sổ `Dialog` riêng, `:core:assets` báo khi tải file lỗi vì mạng. Bảng nguồn và bảng đổi so với app ở mục 3.1.
- Phụ thuộc thêm T-018 (`AssetFailureReason`, issue #73): `:core:assets` chỉ báo lỗi mạng khi lý do là `NETWORK`.
- Rà soát RUN-20261023-01 (chặn): test nút quay lại không dùng Espresso (`pressBackUnconditionally` không có trên classpath biên dịch test). Nay gọi `onBackPressedDispatcher` của chính cửa sổ dialog (cách đã chạy thử, mục 4.1) và kiểm activity không bị kết thúc. Ghi chú của rà soát: `setContent` của `MainActivity` giữ `FullScreenAdHost()` mà T-013 đặt.
- Theo kết quả chạy thử của T-013 (plan T-013 bản 2, mục 4.1): gọi `onBackPressedDispatcher` của activity không tới được Dialog Compose; test dùng dispatcher của dialog.
- Mẫu plan mới: mục 3.3 có "Quyền truy cập:" và "Thư viện và SDK:", mục 3.4, mục 4.1 chạy thử thật bằng `scripts/check.sh`.
- Rà soát trước khi chạy: `AppNavHost` báo `null` khi chưa có mục trên back stack (khung hình đầu), qua hàm thuần `reportedScreenOf(route)` có unit test; `app/src/test/resources/robolectric.properties` có hai dòng `sdk=35` và `application=android.app.Application`, dùng chung với T-014.
- Quyết định đã duyệt ở bản 2 giữ nguyên: chữ "vẫn chưa có mạng" (câu hỏi 1), hiện ngay khi rời loading và khi quay lại từ nền (câu hỏi 2), tiếng Việt mặc định và lint (câu hỏi 3), `scripts/check.sh` so ảnh của `:app` (câu hỏi 4), tham số `screen` rút từ route (câu hỏi 5), `SampleScreen` công khai (câu hỏi 6). Câu hỏi 3, 5 của plan T-016 bản 1 (giá trị đầu không chờ, "Thử lại" chỉ đọc trạng thái hệ thống) chuyển về đây vì phần đó nay nằm trong bộ điều khiển.

## 1. Tóm tắt

`NetworkDialogController` (một đối tượng cho cả app, ở `:core:network`) quyết định khi nào popup mất mạng hiện: sau `network_lost_delay_seconds` mất mạng liên tục (T-016), ngay khi rời màn hình loading hay quay lại từ nền mà không có mạng, hoặc ngay khi một lần tải file thất bại vì mạng (như app, kể cả khi máy vẫn báo có mạng); không hiện ở màn hình loading và khi quảng cáo toàn màn hình đang hiện; tự đóng khi có mạng; nút "Thử lại" kiểm lại trong tối đa 2 giây; ghi `network_lost_shown` và `network_restored`. `:app` vẽ popup (`NetworkDialogScreen` của `:core:designsystem`, theo thiết kế `NetworkLostScreen`) trong cửa sổ `Dialog` riêng nên chặn chạm và nút quay lại. Kiểm chứng bằng unit test cho bộ điều khiển (thời gian ảo), test giao diện Robolectric, screenshot test trùng tên ảnh thiết kế và kiểm tra thủ công trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi màn hình loading bật và không có mạng thì màn hình loading báo lỗi; popup không hiện chồng lên. | `AppNavHost` báo màn hình hiện tại; ở `LoadingDestination` thì báo `null`, bộ điều khiển không hoạt động (không theo dõi, không hiện, bỏ mọi báo lỗi tải). Lúc mở app bộ điều khiển chưa hoạt động. |
| KB-1 | Khi màn hình loading tắt và không có mạng thì popup hiện ngay trên màn hình đầu tiên và chặn thao tác. | Khi chuyển sang hoạt động, bộ điều khiển gọi `ConnectivitySource.isConnected()` (T-016); không có mạng thì hiện ngay, không chờ khoảng `network_lost_delay_seconds` (khoảng chờ chỉ lọc mạng chập chờn lúc đang dùng). |
| KB-1 | Khi Wi-Fi bật nhưng không vào được Internet thì app coi là không có mạng. | Do T-016 (`VALIDATED`). |
| KB-2 | Khi đang dùng mà mất Internet thì sau khoảng chờ (mặc định 2 giây) popup hiện: tiêu đề, lời giải thích, nút "Thử lại". | Bộ điều khiển thu `lostAfterDelay` của T-016 (thời gian chờ đọc `NetworkSettings.LOST_DELAY_SECONDS`). Chữ theo bảng chữ của thiết kế. |
| KB-2 | Khi popup đang hiện thì không chạm được vào màn hình bên dưới; chạm ra ngoài hay nút quay lại không đóng popup. | `Dialog` là cửa sổ riêng ở trên cùng: hệ thống chặn chạm và TalkBack xuống cửa sổ app (L-004 của app). `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`; lớp phủ của `NetworkDialogScreen` nhận mọi chạm; `BackHandler` trong cửa sổ dialog nhận nút quay lại và gọi `onBack` (đưa app về nền, như app camera-pose; câu hỏi 1), không đóng popup, không điều hướng. |
| KB-2 | Khi bấm "Thử lại" mà vẫn không có mạng thì popup vẫn hiện và cho biết vẫn chưa có mạng. | Như app: kiểm `isConnected()` ngay và mỗi 250 ms, tối đa 2 000 ms; trong lúc kiểm nút hiện vòng xoay và bỏ qua lần bấm; hết 2 giây vẫn mất thì hiện dòng "vẫn chưa có mạng". Chỉ đọc trạng thái hệ thống, không tự gửi yêu cầu ra Internet (plan T-016 bản 1, câu hỏi 5). |
| KB-2 | Khi có mạng trở lại thì popup tự đóng và người dùng ở đúng màn hình, đúng trạng thái trước đó. | Khác app (app chỉ đóng qua "Thử lại"): nguồn kết nối báo có mạng thì đóng ngay. Popup không chạm điều hướng hay trạng thái feature; nội dung dưới nằm trong cửa sổ khác nên không bị dựng lại. |
| KB-2, đặc biệt | Khi quảng cáo toàn màn hình đang hiện thì popup không chen vào; nếu vẫn mất mạng khi quảng cáo đóng thì popup hiện ngay. | `MainActivity` chuyển `AdsController.fullScreenAdShowing` vào `setFullScreenAdShowing`. Đang có quảng cáo: bộ điều khiển vẫn ghi nhận mất mạng nhưng trạng thái là `Hidden`; quảng cáo đóng mà vẫn mất mạng thì `Shown` ngay. |
| KB-2, đặc biệt | Khi app ở nền rồi quay lại mà không có mạng thì popup hiện. | Bộ điều khiển sống theo app nên vẫn theo dõi khi app ở nền. Thêm: `MainActivity.onStart` gọi `onAppForeground()`, từ lần thứ hai kiểm `isConnected()` và hiện ngay nếu mất mạng. |
| KB-3 | Khi tải nội dung thất bại vì lỗi mạng (không kết nối được, quá thời gian) thì popup hiện ngay, kể cả khi máy vẫn báo có mạng; máy chủ trả lỗi thì màn hình của feature báo lỗi, popup không hiện (spec sửa 2026-10-23, câu hỏi 5 của spec). | Như app: `:core:assets` chỉ báo khi `AssetFailureReason.NETWORK` (T-018, gồm quá thời gian); bộ điều khiển nhận báo thì hiện ngay. Máy vẫn báo có mạng thì không có lần đổi trạng thái nào để tự đóng: popup giữ tới khi bấm "Thử lại" thấy có mạng. Lỗi HTTP không báo. |

Sự kiện phân tích (spec mục 6), ghi trong bộ điều khiển qua `Analytics`:

- `network_lost_shown`, tham số `screen`: mỗi lần trạng thái chuyển `Hidden` sang `Shown`. `screen` là tên rút từ route của đích hiện tại (`screenNameOf`, giữ quyết định bản 2): `SampleDestination` thành `sample`; không rõ thì `unknown`.
- `network_restored`, tham số `duration_ms` (thời gian popup hiện, đồng hồ đơn điệu) và `retry_count` (số lần bấm "Thử lại" trong lần hiện đó): khi popup đóng vì có mạng (nguồn báo có mạng, hoặc "Thử lại" thấy có mạng). Ẩn vì quảng cáo toàn màn hình hay vì về loading thì không ghi.

Thiết kế tham chiếu: `features/network/design/README.md`, ảnh `design/screens/NetworkLostScreen_default_light.png`, `NetworkLostScreen_default_dark.png`, số liệu trong `design/context/` cùng tên.

ADR liên quan: ADR-0024 (quyết định 3, 4), ADR-0013 (quảng cáo toàn màn hình, không gọi SDK ngoài `:core:ads`), ADR-0019 (log), ADR-0020 (tên screenshot test, đối chiếu thiết kế), ADR-0023 (khóa do T-016 khai báo), ADR-0001 (core phụ thuộc core: `:core:assets` phụ thuộc `:core:network`, `:core:network` phụ thuộc `:core:analytics`, `:core:logging`), ADR-0017 (dòng `app_name` của `strings.xml`).

Giả định về task chạy trước (đúng tên hợp đồng):

- T-016: `internal interface ConnectivitySource { fun isConnected(): Boolean; fun connectivity(): Flow<Boolean> }`, `internal fun ConnectivitySource.lostAfterDelay(delaySeconds: suspend () -> Long): Flow<Unit>`, `NetworkSettings.LOST_DELAY_SECONDS: RemoteKey<Long>`, `di/NetworkModule.kt` với `@NetworkScope CoroutineScope`, `FakeConnectivitySource` và `FakeRemoteSettings` trong `core/network/src/test`.
- T-018: `enum class AssetFailureReason { NETWORK, NOT_FOUND, OTHER }`, `AssetResult.Unavailable(reason)`, `DefaultRemoteAssets` tính `e.toFailureReason()`, `HttpStatusException`.
- T-009: `MainActivity` có `onStart`, `onStop` gọi `ads.onAppForeground()`, `ads.onAppBackground()`.
- `AdsController.fullScreenAdShowing: StateFlow<Boolean>` (từ `FullScreenAdPresence.showing`, hợp đồng chung với T-013): plan này thêm nếu chưa có.
- PR #81 đã merge: `scripts/check.sh` so ảnh chuẩn của `:app` bằng `verifyRoborazziStagingDebug` (đã thấy chạy trong lần chạy thử, mục 4.1).

## 3. Giải pháp

### 3.1. Cách làm

**Nguồn code.** PR VGL-Studio/camera-pose#23 (merge `e23066751fd93e3f945137558287843b506620b3`), đổi gói `com.vglstudio.camera.pose.line.guide` thành `com.vglstudio.template`:

| File ở dự án mẫu | Nguồn ở app | Cách lấy |
| --- | --- | --- |
| `core/network/.../NetworkDialogState.kt` | `core/network/.../NetworkDialogState.kt` | Chép |
| `core/network/.../NetworkStatus.kt` | `core/network/.../NetworkStatus.kt` | Chép, sửa KDoc (`recovered` phát cả khi tự đóng vì có mạng) |
| `core/network/.../NetworkFailureReporter.kt` | `core/network/.../NetworkFailureReporter.kt` | Chép |
| `core/network/.../NetworkDialogController.kt` | `core/network/.../NetworkDialogController.kt` | Chép khung (`state`, `dialogShown`, `recovered`, `retry` với `compareAndSet`, `show`), sửa theo bảng dưới |
| `core/network/.../NetworkClock.kt` | không có ở app | Mới, theo mẫu `LoadingClock` của `:feature:loading`, cho `duration_ms` |
| `core/network/.../NetworkSettings.kt` (sửa) | `RETRY_MAX_MILLIS`, `RETRY_POLL_MILLIS` của `NetworkSettings.kt` | Thêm hai hằng `internal` |
| `core/network/.../di/NetworkModule.kt` (sửa) | `NetworkBindingsModule` của `di/NetworkModule.kt` | Chép hai `@Binds`; thêm `NetworkClock` |
| `core/network/src/test/.../NetworkDialogControllerTest.kt` | cùng tên | Chép các test hợp với spec, sửa và thêm theo bảng dưới |
| `core/designsystem/.../component/NetworkDialog.kt` | cùng tên | Chép khung (`NetworkDialog`, `NetworkDialogScreen` nhận mọi chạm, cuộn, `paneTitle`, test tag), đổi bố cục theo thiết kế đã duyệt |
| `core/designsystem/.../component/PrimaryButton.kt` (sửa) | phần `loading` của `PrimaryButton.kt` | Chép tham số `loading`, vòng xoay, `PrimaryButtonTestTags` |
| `core/designsystem/src/main/res/drawable/ic_wifi_off.xml`, `core/designsystem/third_party/material-symbols/LICENSE.txt` | cùng tên | Chép (Material Symbols Rounded, Apache 2.0) |
| `core/designsystem/src/test/.../NetworkDialogTest.kt`, `PrimaryButtonLoadingTest.kt` | cùng tên | Chép, sửa số đo và chữ theo thiết kế |
| `core/assets/.../DefaultRemoteAssets.kt`, `di/AssetsModule.kt` (sửa), `DefaultRemoteAssetsNetworkFailureTest.kt` | cùng tên | Chép phần `networkFailures` |
| `app/.../network/NetworkDialogHost.kt` | cùng tên | Chép, bỏ tham số `darkTheme`, truyền chữ từ `strings.xml` của `:app` |
| `app/.../navigation/AppNavHost.kt`, `MainActivity.kt` (sửa) | cùng tên | Chép cách nối, đổi `onPastLoadingChanged(Boolean)` thành `onScreenChanged(String?)` |
| `app/src/test/.../network/NetworkDialogHostTest.kt`, `app/src/test/resources/robolectric.properties` | cùng tên | Chép (đã chạy thử), thêm kiểm activity không kết thúc; `robolectric.properties` thêm dòng `application=android.app.Application` |
| `app/.../network/ScreenName.kt`, `ScreenNameTest.kt`, `NetworkLostScreenScreenshotTest.kt` | không có ở app | Từ plan bản 2 |

**Phần đổi so với app:**

| Điểm | App camera-pose (PR #23) | Dự án mẫu (plan này) | Lý do |
| --- | --- | --- | --- |
| Cờ tính năng | `FeatureFlag.NETWORK_DIALOG` (mặc định tắt), bật ở `StagingFeatureFlagsModule` | Bỏ: không có cờ, không sửa `FeatureFlag.kt` hay `StagingFeatureFlagsModule.kt` | ADR-0024, spec mục 8: luôn bật |
| Khóa thời gian chờ | Chuỗi, mặc định 3, `settings.long(...)` | `NetworkSettings.LOST_DELAY_SECONDS` (`RemoteKey`, mặc định 2) của T-016, `settings.value(key)` | Spec mục 8, ADR-0023 |
| Có mạng trở lại | Popup giữ tới khi bấm "Thử lại" | Tự đóng ngay, phát `recovered`, ghi `network_restored` | Spec KB-2 tiêu chí 4 |
| Báo lỗi tải vì mạng | Hiện ngay | Giữ như app: hiện ngay | Câu hỏi 2 (anh Nhật chọn theo app), spec KB-3 sửa 2026-10-23 |
| Khi mở popup lần đầu sau loading | Chờ đủ khoảng chờ | Kiểm `isConnected()` ngay khi rời loading và khi quay lại từ nền | Spec KB-1 tiêu chí 2, KB-2 (quay lại từ nền); plan bản 2, câu hỏi 2 |
| Quảng cáo toàn màn hình | Không xét | `setFullScreenAdShowing(Boolean)` giữ popup ẩn khi quảng cáo đang hiện | Spec KB-2, trường hợp đặc biệt; hợp đồng `fullScreenAdShowing` với T-013 |
| Đầu vào màn hình | `setActive(Boolean)` | `setScreen(screen: String?)` (`null` là loading) | Cần tên màn hình cho `network_lost_shown` |
| Sự kiện phân tích | Không có | `network_lost_shown`, `network_restored` qua `:core:analytics` | Spec mục 6, câu hỏi 2 đã duyệt |
| Chữ | "Mất kết nối mạng", "App cần mạng...", "Vẫn chưa có mạng" trong `strings.xml` của `:core:designsystem` | Chữ của bảng chữ thiết kế, trong `strings.xml` của `:app` (tiếng Việt) và `values-en`; `NetworkDialog` nhận chữ qua tham số | Thiết kế mục 2; chữ của app không nằm trong hệ thống thiết kế (giữ `:core:designsystem` không có `values-en`, không đổi `brand_tagline`) |
| Giao diện hộp thoại | Kiểu Sticker: rộng tối đa 331 dp, nền `surface`, viền, icon 32 dp trong vòng tròn 64 dp, tiêu đề `titleLarge`, nút có icon làm mới, dòng "vẫn chưa có mạng" dưới nút `bodySmall`, lớp phủ 50% | Theo thiết kế đã duyệt: rộng 312 dp (không quá màn hình trừ `Spacing.lg` mỗi bên), nền `surfaceContainerHigh`, bo `shapes.extraLarge` (28 dp), lề `Spacing.lg`, cách nhau `Spacing.md`, icon wifi-off 24 dp màu `secondary`, tiêu đề `headlineSmall` `onSurface`, nội dung `bodyMedium` `onSurfaceVariant`, nút `PrimaryButton` hết chiều rộng không icon, dòng "vẫn chưa có mạng" dưới nội dung `bodyMedium` màu `error`, lớp phủ `colorScheme.scrim` 32% | Thiết kế mục 2, 3; plan bản 2, câu hỏi 1 |
| Icon làm mới `ic_refresh.xml` | Có | Không lấy | Nút trong thiết kế không có icon |
| Nút quay lại | `moveTaskToBack(true)` | Giữ như app (câu hỏi 1) | Spec chỉ đòi không đóng popup |
| Chủ đề tối của popup | Tham số `darkTheme` cho màn hình camera | Bỏ: popup theo `AppTheme` của app | Dự án mẫu không có màn hình tối riêng |
| Quyền ở `app/src/main/AndroidManifest.xml` | Khai báo `ACCESS_NETWORK_STATE` | Không sửa | Quyền ở manifest của `:core:network` (T-016, spec mục 6) |
| Tên ảnh chuẩn | `NetworkDialogScreen_*.png` trong `:core:designsystem`, nền trống | `NetworkLostScreen_*.png` trong `:app`, popup đè lên `SampleScreen` | Tên frame của thiết kế (ADR-0020); thiết kế README mục 1 |

**`:core:network` (bộ điều khiển).**

- `core/network/build.gradle.kts`: thêm `implementation(project(":core:analytics"))`, `implementation(project(":core:logging"))`.
- `NetworkDialogState`: `Hidden`, `Shown(checking: Boolean, stillOffline: Boolean)`.
- `NetworkStatus` (public): `dialogShown: StateFlow<Boolean>`, `recovered: Flow<Unit>` (phát mỗi lần popup đóng vì có mạng). Chưa module nào của dự án mẫu dùng; dành cho feature của app (app camera-pose dừng đếm ngược camera, tải lại màn hình lỗi).
- `NetworkFailureReporter` (public `fun interface`): `reportNetworkFailure(source: String)`, `source` là nhãn ngắn, chỉ để log.
- `NetworkClock` (`internal fun interface`): `elapsedRealtimeMillis()`; Hilt cấp `SystemClock.elapsedRealtime()`.
- `NetworkDialogController @Singleton @Inject internal constructor(connectivity: ConnectivitySource, settings: RemoteSettings, analytics: Analytics, logger: AppLogger, clock: NetworkClock, @NetworkScope scope: CoroutineScope) : NetworkStatus, NetworkFailureReporter`:
  - Trạng thái trong: `screen: MutableStateFlow<String?>` (bắt đầu `null`), `adShowing: MutableStateFlow<Boolean>`, `lost: MutableStateFlow<Boolean>`, `retryJob`, `retryCount`, `shownAt`, `foregroundCount`.
  - `state: StateFlow<NetworkDialogState>`: `Shown(checking, stillOffline)` khi `lost && screen != null && !adShowing`, còn lại `Hidden`. Chuyển `Hidden` sang `Shown`: `shownAt = clock`, `retryCount = 0`, ghi `network_lost_shown(screen)`, log `info("Network", "Dialog shown: <lý do> on <screen>")`. Lý do: `device_offline`, `offline_on_start`, `offline_on_foreground`, `load_failed`.
  - `setScreen(screen: String?)`: `null` thì ngừng theo dõi, `lost = false`, hủy `retryJob`, log `Dialog hidden: loading` nếu đang hiện, không ghi sự kiện. Từ `null` sang tên màn hình: kiểm `isConnected()`, mất thì `lost = true` (lý do `offline_on_start`), rồi thu `connectivity.lostAfterDelay { settings.value(NetworkSettings.LOST_DELAY_SECONDS).first() }` (mất: `lost = true`, lý do `device_offline`) và `connectivity.connectivity()` (có mạng khi `lost`: đóng vì có mạng). Hai luồng cùng một nguồn nên chỉ thu trong một `collectLatest` theo `screen != null`, như app.
  - `setFullScreenAdShowing(showing: Boolean)`: cập nhật; quảng cáo bắt đầu khi đang hiện thì ẩn, log `Dialog paused: full_screen_ad`, không ghi `network_restored`; quảng cáo đóng mà `lost` thì hiện ngay (ghi `network_lost_shown` lần nữa).
  - Đóng vì có mạng: `lost = false`; nếu đang `Shown` thì ghi `network_restored(duration_ms, retry_count)`, log `Dialog closed: online after <ms> ms, <n> retries`, phát `recovered`.
  - `retry()`: như app (bỏ qua khi không `Shown` hay đang `checking`; `compareAndSet` sang `checking`; kiểm ngay rồi mỗi `RETRY_POLL_MILLIS` tới `RETRY_MAX_MILLIS`), cộng `retryCount`. Có mạng: đóng vì có mạng. Hết giờ: `Shown(stillOffline = true)`, log `Retry: still_offline`.
  - `reportNetworkFailure(source)`: chỉ khi `screen != null`. `lost = true` ngay, như app (lý do `load_failed source=<source>`, log ghi thêm `online=<isConnected()>`). Máy vẫn báo có mạng thì popup đóng khi bấm "Thử lại" (kiểm `isConnected()` thấy có mạng) hay khi nguồn kết nối phát lại trạng thái có mạng (KB-3). Gọi được từ mọi luồng.
  - `onAppForeground()`: lần đầu bỏ qua (đã kiểm khi rời loading); từ lần hai, nếu `screen != null` và `isConnected()` là `false` thì `lost = true` (lý do `offline_on_foreground`).
  - Log thẻ `Network`, không có tên mạng, địa chỉ, đường dẫn file (ADR-0019).
- `NetworkModule.kt`: thêm `NetworkBindingsModule` (`@Binds NetworkStatus`, `@Binds NetworkFailureReporter` tới `NetworkDialogController`) và `@Provides NetworkClock`. Phải có trong cùng task với phần sửa `:core:assets`: chạy thử cho thấy thiếu binding `NetworkFailureReporter` thì Hilt của `:app` báo lỗi biên dịch (mục 4.1).

**`:core:assets`.** `build.gradle.kts` thêm `implementation(project(":core:network"))`. `DefaultRemoteAssets` nhận thêm `networkFailures: NetworkFailureReporter? = null`; trong `catch`, sau khi tính `reason` (T-018), `if (reason == AssetFailureReason.NETWORK) networkFailures?.reportNetworkFailure("assets")`. `AssetsModule` tiêm `NetworkFailureReporter`. File đã có trên máy thì không tải, không báo.

**`:core:designsystem`.**

- `PrimaryButton(text, onClick, modifier, enabled, icon, loading: Boolean = false)`: khi `loading`, `CircularProgressIndicator` cỡ `IconSize` (18 dp), màu `LocalContentColor.current`, nét 2 dp, thay chỗ icon, test tag `PrimaryButtonTestTags.SPINNER`; nút giữ dáng `enabled`, `onClick` bị bỏ qua. Không `loading` thì không đổi gì (ảnh chuẩn của danh mục thiết kế không đổi).
- `NetworkDialog(title: String, message: String, retryLabel: String, stillOfflineMessage: String?, checking: Boolean, onRetry: () -> Unit, modifier)`: `Column` rộng `DialogWidth = 312.dp` (`widthIn(max = 312.dp).fillMaxWidth()`, hằng ngoài hệ thống duy nhất, có chú thích nguồn Material 3 và thiết kế mục 3), nền `surfaceContainerHigh`, `shapes.extraLarge`, lề `Spacing.lg`, cách nhau `Spacing.md`, căn giữa, `semantics { paneTitle = title }`, test tag `NetworkDialogTestTags.CARD`. Thứ tự: icon `ic_wifi_off` 24 dp màu `secondary`, tiêu đề `headlineSmall` `onSurface`, nội dung `bodyMedium` `onSurfaceVariant`, `stillOfflineMessage` (khác `null`) `bodyMedium` màu `error` có `liveRegion = Polite` và tag `STILL_OFFLINE`, rồi `PrimaryButton(retryLabel, loading = checking)` hết chiều rộng, tag `RETRY`.
- `NetworkDialogScreen(...cùng tham số)`: `Box` toàn màn hình, nền `colorScheme.scrim` độ mờ 0.32 (thiết kế mục 3), nhận và tiêu mọi chạm (`pointerInput` như app), tag `SCREEN`; trong đó `Column` `safeDrawingPadding`, `verticalScroll`, lề `Spacing.md`, căn giữa, chứa `NetworkDialog`.
- `ic_wifi_off.xml` và `third_party/material-symbols/LICENSE.txt` chép từ app.

**`:app`.**

- `strings.xml` (tiếng Việt, dòng `app_name` giữ nguyên từng ký tự vì `scripts/new-app.sh` thay đúng dòng đó): `network_lost_title` "Không có kết nối mạng", `network_lost_message` "Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại.", `network_lost_retry` "Thử lại", `network_lost_still_offline` "Vẫn chưa có kết nối. Hãy thử lại sau ít phút.". `values-en/strings.xml` mới: "No internet connection", "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.", "Try again", "Still no connection. Please try again in a moment.". Không đưa `app_name` vào `values-en` (xử lý lint theo câu hỏi 3 của bản 2).
- `NetworkDialogHost(state, onRetry, onBack, modifier, content)`: vẽ `content`; khi `Shown` mở `Dialog(onDismissRequest = {}, DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false))`, tắt lớp mờ của hệ thống (`setDimAmount(0f)`, popup tự vẽ lớp phủ), vẽ `NetworkDialogScreen` với chữ từ `stringResource`, `BackHandler(onBack = onBack)`.
- `ScreenName.kt`: `screenNameOf(route: String?): String` như bản 2, và hàm thuần mới `internal fun reportedScreenOf(route: String?): String?`: `route` là `null` (chưa có mục nào trên back stack, khung hình đầu tiên khi `currentBackStackEntryAsState()` còn `null`) thì `null`; route của `LoadingDestination` (`screenNameOf(route) == "loading"`) thì `null`; còn lại `screenNameOf(route)`. Hàm chỉ nhận chuỗi route nên unit test JVM gọi thẳng được, không cần `NavController`.
- `AppNavHost(modifier, onScreenChanged: (String?) -> Unit = {})`: `val entry by navController.currentBackStackEntryAsState()`, `val screen = reportedScreenOf(entry?.destination?.route)`; `LaunchedEffect(screen) { onScreenChanged(screen) }` (như app, đúng cả khi activity dựng lại). Khung hình đầu chưa có mục trên back stack thì báo `null`, giống `LoadingDestination`: bộ điều khiển chưa hoạt động, popup không thể chớp lên trước màn hình loading.
- `MainActivity`: `@Inject lateinit var networkDialog: NetworkDialogController`. Trong `AppTheme`: `Surface { val state by networkDialog.state.collectAsState(); NetworkDialogHost(state, networkDialog::retry, onBack = { moveTaskToBack(true) }) { AppNavHost(onScreenChanged = networkDialog::setScreen) } }`; `val adShowing by ads.fullScreenAdShowing.collectAsState()`, `LaunchedEffect(adShowing) { networkDialog.setFullScreenAdShowing(adShowing) }`; giữ nguyên `FullScreenAdHost()` nếu T-013 đã đặt, `gatherConsent`, `onUserInteraction`. `onStart` thêm `networkDialog.onAppForeground()` cạnh lệnh của T-009.
- `app/build.gradle.kts`: plugin `alias(libs.plugins.roborazzi)`, khối `roborazzi { outputDir.set(layout.projectDirectory.dir("src/test/screenshots")) }`, `testImplementation(libs.roborazzi)`, `testImplementation(libs.roborazzi.compose)`, `testImplementation(libs.roborazzi.junit.rule)` (thêm nếu T-014 chưa thêm). `implementation(project(":core:network"))` đã có từ T-016.
- `app/src/test/resources/robolectric.properties`: hai dòng `sdk=35` và `application=android.app.Application`, để mọi test Robolectric của `:app` không dựng `TemplateApplication` (Hilt, Firebase). Lớp test của `:app` không cần ghi `@Config(application = Application::class)`; lớp chép từ app còn dòng đó thì để nguyên, vô hại vì trùng giá trị với file. T-014 dùng chung file này (chạy trước T-017 thì T-014 tạo với đúng hai dòng này).
- `SampleScreen` của `:feature:sample` từ `internal` sang công khai (bản 2, câu hỏi 6).

**`:core:ads`** (thêm nếu T-013 chưa thêm, giữ hợp đồng bản 2): `internal class FullScreenAdPresence` (`showing`, `onShowing()`, `onClosed()`), `AdsController.fullScreenAdShowing`, interstitial gọi `onShowing()` ngay trước `show()`, `onClosed()` khi đóng hay lỗi hiện. Đã có đúng tên và kiểu thì bỏ phần này, ghi nhật ký.

**Screenshot và đối chiếu thiết kế.** `NetworkLostScreenScreenshotTest` ở `:app` dựng trong một cửa sổ `Box { SampleScreen(SampleUiState.Ready(count = 3), onIncrement = {}); NetworkDialogScreen(...) }` trong `AppTheme`, rồi `composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_default_light.png")` (tương tự cho ba ảnh còn lại ở mục 4). `onRoot()` chỉ chụp cửa sổ chính, nên screenshot không mở `Dialog`; cửa sổ dialog thật kiểm ở `NetworkDialogHostTest`. Đối chiếu: gọi thẳng `java -Djava.awt.headless=true scripts/design-compare/DesignCompare.java docs/features/network/design/screens app/src/test/screenshots docs/features/network/design/review` (script bọc ngoài chỉ đọc `feature/<feature>/`), commit `design/review/`, ghi nhận xét vào `design/README.md` mục 8. Icon Material Symbols có thể khác nét icon trên Figma (câu hỏi 3); vùng quảng cáo native dưới `SampleScreen` trống vì test không có `AdsController`.

**Thứ tự commit (L-025).** Commit test (bước 3): mọi file test mới và sửa trong `core/network/src/test`, `core/designsystem/src/test`, `core/assets/src/test`, `core/ads/src/test`, `app/src/test`, `robolectric.properties`, phần test của `app/build.gradle.kts`, và khung để biên dịch: `NetworkDialogState`, `NetworkStatus`, `NetworkFailureReporter`, `NetworkClock`, `NetworkDialogController` luôn `Hidden` không ghi gì, tham số `loading` của `PrimaryButton` chưa làm gì, `NetworkDialog`/`NetworkDialogScreen` rỗng có test tag, `NetworkDialogHost` chỉ vẽ `content`, `screenNameOf` trả `unknown`, `reportedScreenOf` trả `unknown`, tham số `networkFailures` chưa gọi, `FullScreenAdPresence` luôn `false`, bốn chuỗi (hai ngôn ngữ), `SampleScreen` công khai. Test thất bại ở phần kiểm tra. Commit code (bước 4) viết phần thật, icon, Hilt, nối `AdsController`, `AppNavHost`, `MainActivity`, rồi ghi ảnh chuẩn.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/network/build.gradle.kts` | Thêm `:core:analytics`, `:core:logging` (file do T-016 tạo) |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkDialogState.kt` | Trạng thái popup |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkStatus.kt` | `dialogShown`, `recovered` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkFailureReporter.kt` | Báo lỗi tải vì mạng |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkClock.kt` | Đồng hồ đơn điệu |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkDialogController.kt` | Bộ điều khiển popup, hai sự kiện, log |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkSettings.kt` | `RETRY_MAX_MILLIS = 2_000`, `RETRY_POLL_MILLIS = 250` (`internal`) |
| Sửa | `core/network/src/main/kotlin/com/vglstudio/template/core/network/di/NetworkModule.kt` | `NetworkBindingsModule`, `NetworkClock` |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/template/core/network/Fakes.kt` | Thêm `FakeAnalytics`, `FakeNetworkClock` |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/NetworkDialogControllerTest.kt` | Unit test bộ điều khiển |
| Sửa | `core/network/src/test/kotlin/com/vglstudio/template/core/network/NetworkSettingsTest.kt` | Thêm `retryWaitsAtMost2Seconds` |
| Sửa | `core/assets/build.gradle.kts` | `implementation(project(":core:network"))` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/DefaultRemoteAssets.kt` | `networkFailures`, báo khi `NETWORK` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/di/AssetsModule.kt` | Tiêm `NetworkFailureReporter` |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/template/core/assets/DefaultRemoteAssetsNetworkFailureTest.kt` | Báo đúng một lần khi lỗi mạng, không báo khi 404, lỗi khác, file có sẵn, tải được |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/component/PrimaryButton.kt` | Tham số `loading`, `PrimaryButtonTestTags` |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/template/core/designsystem/component/NetworkDialog.kt` | `NetworkDialog`, `NetworkDialogScreen`, `NetworkDialogTestTags` |
| Tạo | `core/designsystem/src/main/res/drawable/ic_wifi_off.xml` | Icon Material Symbols Rounded "wifi_off" |
| Tạo | `core/designsystem/third_party/material-symbols/LICENSE.txt` | Giấy phép Apache 2.0 của icon |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/template/core/designsystem/NetworkDialogTest.kt` | Test giao diện popup |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/template/core/designsystem/PrimaryButtonLoadingTest.kt` | Trạng thái chờ của nút |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresence.kt` | Thêm nếu T-013 chưa tạo |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `fullScreenAdShowing`, interstitial báo `presence` (bỏ nếu T-013 đã có) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPresenceTest.kt` | Bỏ nếu T-013 đã tạo |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/NetworkDialogHost.kt` | Cửa sổ `Dialog` riêng, nút quay lại |
| Tạo | `app/src/main/kotlin/com/vglstudio/template/network/ScreenName.kt` | `screenNameOf(route)`, `reportedScreenOf(route)` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/navigation/AppNavHost.kt` | `onScreenChanged`, gọi `reportedScreenOf(entry?.destination?.route)` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | `NetworkDialogHost`, quảng cáo toàn màn hình, `onAppForeground`; giữ `FullScreenAdHost()` |
| Sửa | `app/src/main/res/values/strings.xml` | Bốn chuỗi tiếng Việt; `app_name` giữ nguyên |
| Tạo | `app/src/main/res/values-en/strings.xml` | Bốn chuỗi tiếng Anh |
| Sửa | `app/build.gradle.kts` | Plugin, khối và thư viện test Roborazzi (nếu T-014 chưa thêm) |
| Sửa | `feature/sample/src/main/kotlin/com/vglstudio/template/feature/sample/SampleScreen.kt` | `SampleScreen` công khai, KDoc ghi lý do |
| Tạo | `app/src/test/resources/robolectric.properties` | `sdk=35`, `application=android.app.Application` (T-014 chạy trước thì đã có, chỉ kiểm hai dòng) |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkDialogHostTest.kt` | Cửa sổ riêng, không lớp mờ, nút quay lại |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/ScreenNameTest.kt` | Tên màn hình, màn hình được báo |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostScreenScreenshotTest.kt` | Screenshot test |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_default_light.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_default_dark.png` | Ảnh chuẩn, cùng tên ảnh thiết kế |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_stillOffline_light.png` | Ảnh chuẩn sau lần thử lại thất bại |
| Tạo | `app/src/test/screenshots/NetworkLostScreen_stillOffline_small.png` | Ảnh chuẩn khung thấp (L-028) |
| Tạo | `docs/features/network/design/review/` | Ảnh ghép và `summary.md` của `DesignCompare.java` |
| Sửa | `docs/features/network/design/README.md` | Mục 8, hai ô còn trống của mục 7 |
| Sửa | `CLAUDE.md` | Dòng `:core:network` của bảng module (do T-016 thêm): thêm "popup mất mạng" nếu T-016 chưa ghi |
| Sửa | `docs/features/network/tickets/T-017-popup-mat-mang.md` | Trạng thái ticket |
| Sửa | `docs/features/network/plans/T-017-popup-mat-mang.md` | Nhật ký thực hiện |

Không sửa: `scripts/check.sh`, script đối chiếu thiết kế, `gradle/libs.versions.toml`, `build-logic/`, mọi `AndroidManifest.xml`, `FeatureFlag.kt`, `StagingFeatureFlagsModule.kt`, `feature/loading/`, `app/src/androidTest/` (emulator CI có mạng; popup nếu hiện vẫn để `sample_screen` trong cây giao diện), hai file bảng khóa (T-017 không thêm khóa), `AdSlot.kt`, `AdPlacement.kt`.

- `app/build.gradle.kts` (L-020): `scripts/setup-check.sh` và `scripts/tests/run.sh` chỉ đọc dòng `ASSETS_BASE_URL` và `applicationId` của file này; plan không đổi hai dòng đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có (spec mục 8, ADR-0024). Bỏ cờ `NETWORK_DIALOG` của app. Khóa `network_lost_delay_seconds` thuộc T-016.
- Quảng cáo: không thêm, bớt, di chuyển vị trí hay đổi tần suất. Chỉ thêm trạng thái chỉ đọc `fullScreenAdShowing` (nếu chưa có). Popup không có quảng cáo.
- Sự kiện phân tích: `network_lost_shown` (`screen`), `network_restored` (`duration_ms`, `retry_count`), đúng spec mục 6 (câu hỏi 2 của spec đã duyệt), qua `Analytics` của `:core:analytics`.
- File R2: không thêm file; `:core:assets` chỉ báo lỗi mạng cho bộ điều khiển, không đổi đường dẫn hay cách tải.
- Dữ liệu trên máy: không có.
- **Quyền truy cập:** Không thêm. `ACCESS_NETWORK_STATE` do T-016 khai báo ở manifest của `:core:network` (spec network mục 6); T-017 không sửa manifest nào. `Dialog`, `moveTaskToBack`, `SystemClock` không cần quyền.
- **Thư viện và SDK:** Không thêm thư viện mới. Đã có: `CircularProgressIndicator` trong `libs.androidx.compose.material3` (convention Compose của `:core:designsystem`); `BackHandler`, `LocalOnBackPressedDispatcherOwner` trong `libs.androidx.activity.compose` (`:app`); `Dialog`, `DialogWindowProvider` trong `libs.androidx.compose.ui`; `libs.roborazzi`, `libs.roborazzi.compose`, `libs.roborazzi.junit.rule`, plugin `libs.plugins.roborazzi` (đã có trong `gradle/libs.versions.toml`, `:app` thêm `testImplementation`); Robolectric, Compose test, Turbine qua convention. Icon `ic_wifi_off.xml` là một file vector chép từ Material Symbols (Apache 2.0), không phải thư viện; giấy phép đi kèm ở `core/designsystem/third_party/material-symbols/LICENSE.txt`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** kiểm nút quay lại trên cửa sổ dialog bằng Robolectric (`ShadowDialog.getLatestDialog()` ép sang `ComponentDialog`).
- **Điều kiện chuyển:** `ShadowDialog.getLatestDialog()` trả `null` hay không phải `ComponentDialog` (ví dụ sau khi nâng Compose), hoặc `back_onDialogWindow_*` thất bại vì cùng một lỗi sau 2 lần sửa.
- **Cách làm thay:** trong test, lấy dispatcher từ bên trong nội dung dialog qua `LocalOnBackPressedDispatcherOwner.current` (cách T-013 đã chạy thử; chạy thử của plan này cho thấy đó cùng là dispatcher của `ComponentDialog`, test `dialogDispatcher_isTheOneComposeSeesInsideTheDialog`). Để test đọc được, `NetworkDialogHost` nhận thêm tham số `internal` `onDialogBackDispatcher: (OnBackPressedDispatcher) -> Unit = {}` gọi một lần trong nội dung dialog. Tiêu chí và tên test ở mục 4 giữ nguyên. File thay đổi đã có trong mục 3.2 (`NetworkDialogHost.kt`, `NetworkDialogHostTest.kt`).

## 4. Kiểm thử

Unit test bộ điều khiển: `runTest`, thời gian ảo, `backgroundScope` làm `@NetworkScope`, `FakeConnectivitySource`, `FakeRemoteSettings`, `FakeAnalytics`, `RecordingAppLogger`, `FakeNetworkClock` đọc `testScheduler.currentTime`. Mặc định: có mạng, không có quảng cáo, đã `setScreen("sample")` trừ khi ghi khác. Sự kiện so bằng `AnalyticsEvent(name, params)` đầy đủ.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 1 | Unit test | `NetworkDialogControllerTest.onLoading_ignoresOfflineAndFailures`: `setScreen(null)`, mất mạng 10 000 ms, `reportNetworkFailure("assets")`: `Hidden`, `collectors == 0`, không sự kiện; chuyển sang `sample` khi đã có mạng: vẫn `Hidden` (báo lỗi cũ không giữ lại) |
| KB-1, tiêu chí 1 | Unit test | `NetworkDialogControllerTest.backToLoading_hidesWithoutRestoredEvent`: đang `Shown`, `setScreen(null)`: `Hidden`, `collectors == 0`, không có `network_restored` |
| KB-1, tiêu chí 2 | Unit test | `NetworkDialogControllerTest.leavingLoadingOffline_showsAtOnce`: nguồn `false`, `setScreen("sample")`, sau `runCurrent()` ở 0 ms là `Shown`, một `network_lost_shown(screen=sample)` |
| KB-1, tiêu chí 3 | Do T-016 | `AndroidConnectivitySourceTest.wifiWithInternetNotValidated_isNotUsable` |
| KB-2, tiêu chí 1 | Unit test | `NetworkDialogControllerTest.deviceOffline_shownOnlyAfterDelay` (từ app, mặc định 2 s): ở 1 999 ms `Hidden`, ở 2 001 ms `Shown`; `delay_followsRemoteSetting`: khóa 5, ở 4 999 ms `Hidden`, 5 001 ms `Shown`; `flapping_underDelay_showsNothing` (từ app) |
| KB-2, tiêu chí 1 (chữ) | Compose UI test | `NetworkDialogTest.default_showsTextAndIcon`: `NetworkDialogScreen` nhận ba chữ của bảng chữ, có đúng ba chữ, không có tag `STILL_OFFLINE`. `NetworkDialogHostTest.shown_usesAppStrings_vi` và `shown_usesAppStrings_en` (`@Config(qualifiers = "en")`): chữ tiếng Việt và tiếng Anh của mục 3.1 |
| KB-2, tiêu chí 2 (cửa sổ riêng) | Compose UI test | `NetworkDialogHostTest.shown_drawsDialogInSeparateWindow` (từ app): hai gốc, gốc 0 chứa nội dung, gốc 1 chứa tag `SCREEN`; nội dung vẫn trong cây ngữ nghĩa. `shown_dialogWindowAddsNoSystemDim` (từ app) |
| KB-2, tiêu chí 2 (nút quay lại) | Compose UI test | `NetworkDialogHostTest.back_onDialogWindow_keepsDialogAndDestination`: `NavHost` hai đích đã ở đích hai, `Shown`; `(ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()` trong `runOnUiThread`: `onBack` gọi một lần, `composeRule.activity.isFinishing == false`, đích hai và popup còn. `dialogDispatcher_isTheOneComposeSeesInsideTheDialog`: dispatcher đọc bằng `LocalOnBackPressedDispatcherOwner` trong nội dung dialog cũng là dispatcher của `ComponentDialog`, khác dispatcher của activity. `back_whileHidden_navigatesBack` (từ app): `Hidden`, dispatcher của activity đưa về đích một, `onBack` không gọi. Không dùng Espresso |
| KB-2, tiêu chí 2 (chạm) | Compose UI test | `NetworkDialogTest.screen_consumesTouches` (từ app): nút phía sau không nhận lần bấm, chạm lớp phủ không gọi `onRetry`, hộp thoại còn |
| KB-2, tiêu chí 2 (chạm thật) | Kiểm tra thủ công | Robolectric không mô phỏng cửa sổ chồng nhau. Bản staging: tắt mạng ở màn hình mẫu, chờ popup; chạm nút "Tap" phía sau, chạm vùng tối, bấm quay lại (app về nền), mở lại: số đếm không đổi, popup còn; TalkBack chỉ đọc popup |
| KB-2, tiêu chí 3 | Unit test | `NetworkDialogControllerTest.retry_offline_spinsAtMost2s_thenStillOffline` (từ app): `checking` tới 1 999 ms, `stillOffline` ở 2 001 ms, không `recovered`; `retry_whileChecking_isIgnored`, `retryAgain_clearsStillOffline` (từ app) |
| KB-2, tiêu chí 3 (giao diện) | Compose UI test | `NetworkDialogTest.stillOffline_showsMessageAboveButton`: dòng `STILL_OFFLINE` có, đáy dòng không thấp hơn đỉnh nút; `checking_showsSpinner_andIgnoresClicks`, `retry_click_callsOnRetry` (từ app). `PrimaryButtonLoadingTest.loading_showsSpinner_keepsText_ignoresClick`, `notLoading_hasNoSpinner` (từ app) |
| KB-2, tiêu chí 4 | Unit test | `NetworkDialogControllerTest.networkBack_closesAtOnce_andEmitsRecovered`: `Shown`, nguồn `true`: `Hidden` ở cùng `currentTime`, một `recovered`. `retry_online_closes_andEmitsRecovered`, `retry_networkReturnsWhileChecking_closes` (từ app) |
| KB-2, tiêu chí 4 (đúng trạng thái) | Compose UI test | `NetworkDialogHostTest.hidingDialog_keepsContentState`: nội dung có bộ đếm `rememberSaveable` lên 3, đổi `Shown` rồi `Hidden`: bộ đếm vẫn 3 |
| KB-2, đặc biệt: quảng cáo | Unit test | `NetworkDialogControllerTest.fullScreenAd_holdsDialog_thenShowsWhenClosed`: `setFullScreenAdShowing(true)`, mất mạng 3 000 ms: `Hidden`, không sự kiện; quảng cáo đóng: `Shown` ngay, một `network_lost_shown`. `adStartsWhileShown_hidesWithoutRestoredEvent` |
| KB-2, đặc biệt: quảng cáo (nguồn) | Unit test, Turbine | `FullScreenAdPresenceTest.startsHidden`, `onShowing_setsTrue`, `onClosed_setsFalse`, `closedTwice_staysFalse`, `showingFlow_emitsShowAndClose` (cùng tên với plan T-013; bỏ nếu T-013 đã tạo) |
| KB-2, đặc biệt: về từ nền | Unit test | `NetworkDialogControllerTest.appForeground_checksAtOnceExceptFirst`: lần đầu không đọc `isConnected()`; lần hai khi mất mạng: `Shown` ngay |
| KB-3 | Unit test | `NetworkDialogControllerTest.networkFailure_whileDeviceOnline_showsAtOnce_andRetryCloses`: nguồn `true`, `reportNetworkFailure("assets")`: `Shown` ngay, log `INFO` chứa `load_failed` và `online=true`; `retry()`: `Hidden`, ghi `network_restored`. Trước khi viết code, khung trả `Hidden` nên test thất bại đúng lý do. `networkFailure_whileDeviceOffline_showsAtOnce`: nguồn `false` chưa đủ khoảng chờ, báo lỗi: `Shown` ngay. `manyFailures_singleDialog` (từ app) |
| KB-3 (nguồn báo) | Unit test | `DefaultRemoteAssetsNetworkFailureTest.networkError_returnsUnavailable_andReportsOnce`, `http404_isNotReported`, `otherError_isNotReported`, `keptFile_isNotDownloaded_andNotReported`, `success_isNotReported` (từ app) |
| Sự kiện `network_lost_shown` | Unit test | `NetworkDialogControllerTest.shown_logsLostEventWithScreen`: `AnalyticsEvent("network_lost_shown", mapOf("screen" to "sample"))`; hiện lại lần hai ghi thêm một |
| Sự kiện `network_restored` | Unit test | `NetworkDialogControllerTest.restored_logsDurationAndRetries`: hiện lúc t, hai lần thử lại thất bại, nguồn `true` lúc t + 12 000: `AnalyticsEvent("network_restored", mapOf("duration_ms" to "12000", "retry_count" to "2"))` |
| `dialogShown` | Unit test | `NetworkDialogControllerTest.dialogShown_followsState` (từ app) |
| Log không có dữ liệu cá nhân | Unit test | `NetworkDialogControllerTest.logsShowAndRetryOutcome` (từ app): có `device_offline`, `still_offline`, `online`, `load_failed`; không dòng nào chứa `/` |
| Tên màn hình | Unit test | `ScreenNameTest` như bản 2: `sample`, `loading`, `ad_gallery`, route có tham số ra `detail`, `null` và `""` ra `unknown`, cắt còn 100 ký tự |
| KB-1: không hiện ở loading, không chớp lúc mở app | Unit test JVM | `ScreenNameTest.reportedScreen_nullBeforeFirstEntryAndOnLoading`: `reportedScreenOf(null)` là `null` (chưa có mục trên back stack); route của `LoadingDestination` ra `null`; route của `SampleDestination` ra `"sample"`. Cùng chuỗi route như các ca `screenNameOf` ở trên |
| Đúng thiết kế, sáng | Screenshot test | `NetworkLostScreenScreenshotTest.default_light`, khung `w411dp-h891dp-xxhdpi`, `@GraphicsMode(NATIVE)`: `composeRule.onRoot().captureRoboImage("src/test/screenshots/NetworkLostScreen_default_light.png")` |
| Đúng thiết kế, tối | Screenshot test | `default_dark`: `AppTheme(darkTheme = true)`, `captureRoboImage("src/test/screenshots/NetworkLostScreen_default_dark.png")` |
| Thử lại thất bại | Screenshot test | `stillOffline_light`: `captureRoboImage("src/test/screenshots/NetworkLostScreen_stillOffline_light.png")` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi`, trạng thái nhiều nội dung nhất | `stillOffline_small`: `captureRoboImage("src/test/screenshots/NetworkLostScreen_stillOffline_small.png")`; nút "Thử lại" thấy trọn |
| Màn hình thấp (đo) | Compose UI test | `NetworkDialogTest.smallScreen_cardFitsAndRetryVisible` (từ app, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")`, `stillOffline`): hộp thoại nằm trong màn hình, nút `assertIsDisplayed()` |
| Kích thước theo thiết kế | Compose UI test | `NetworkDialogTest.card_is312dpWide_andRetry264dp`: `@GraphicsMode(NATIVE)`, khung chuẩn: hộp thoại rộng 312 dp, nút cao ít nhất 52 dp, rộng 264 dp (312 trừ 24 x 2), sai số 0.5 dp |
| Đối chiếu ảnh thiết kế | Ảnh so sánh, rà soát độc lập | `DesignCompare.java` (mục 3.1); rà soát độc lập xem hai ảnh `*_compare.png` theo mục 7 của `design/README.md`, ghi mục 8 |
| Ảnh chuẩn đúng | `scripts/check.sh` | `:app:verifyRoborazziStagingDebug` có trong tác vụ của `check.sh` và qua |
| Hilt và khởi động app | Build, test emulator có sẵn | `:app:assembleStagingDebug` qua (đồ thị Hilt có `NetworkFailureReporter`); `AppLaunchTest` trong CI qua |
| Toàn bộ trên máy | Kiểm tra thủ công | Bản staging: (1) loading bật, chế độ máy bay, mở app: loading báo lỗi, không popup; (2) loading tắt, chế độ máy bay, mở app: popup ngay trên màn hình mẫu; (3) đang dùng, tắt mạng: popup sau khoảng 2 giây; "Thử lại": vòng xoay rồi dòng "vẫn chưa có kết nối"; bật mạng: popup tự đóng, số đếm giữ nguyên; (4) Wi-Fi không ra Internet: popup hiện; (5) `adb logcat -v threadtime \| grep ' VGL\.Network'` thấy hiện, đóng, thử lại; (6) sự kiện kiểm qua unit test (bản staging dùng Firebase giả, ADR-0012) |

`MainActivity`, `NetworkModule`, phần nối trong `AdsController` và `AssetsModule` không có unit test riêng: chỉ nối các phần đã test. Rà soát độc lập đọc kỹ chỗ gọi `setFullScreenAdShowing`, `onAppForeground`, `onShowing`/`onClosed`.

**Ngoại lệ viết test trước:** (1) phần nối nêu trên, kiểm qua build, `AppLaunchTest`, kiểm tra thủ công; (2) icon, giấy phép và cấu hình build, kiểm qua screenshot test; (3) chữ trong `strings.xml` vào commit test cùng khung rỗng vì test chữ cần tài nguyên để biên dịch; (4) ảnh chuẩn ghi sau khi code xong. Người duyệt cần đồng ý.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, cùng worktree tạm ngoài repo với T-016 (`origin/main`, commit `80cc3e2`), không commit, đã xóa. Code thử, chép từ app (PR #23) đổi gói: `NetworkDialogState`, `NetworkFailureReporter`, `NetworkDialogHost` của `:app` (nội dung dialog là một `Box` có tag thay cho `NetworkDialogScreen`), `app/src/test/resources/robolectric.properties` (`sdk=35`), `NetworkDialogHostTest` (5 test: cửa sổ riêng và không lớp mờ; nút quay lại trên dispatcher của `ComponentDialog`, kiểm `onBack` một lần, `activity.isFinishing == false`, đích và popup còn; dispatcher đọc bằng `LocalOnBackPressedDispatcherOwner` trong dialog trùng dispatcher của `ComponentDialog`; dispatcher của activity khi popup hiện; dispatcher của activity khi ẩn thì điều hướng lùi); `:core:assets` báo lỗi mạng qua `NetworkFailureReporter` (phần của T-018 cũng chép vào), `DefaultRemoteAssetsNetworkFailureTest`; plugin Roborazzi cho `:app` và một screenshot test tạm chụp bằng `onRoot()` ảnh `NetworkTrial_light.png` trong `app/src/test/screenshots/` (nội dung và lớp phủ 32% trong một cửa sổ).
- **Lệnh:** `./gradlew :core:network:testDebugUnitTest :app:testStagingDebugUnitTest`, rồi `scripts/check.sh` ba lần (`ANDROID_HOME=$HOME/Library/Android/sdk`, `JAVA_HOME` của Android Studio).
- **Kết quả:**
  - Lần 1 của phần này: `check.sh` lỗi biên dịch Hilt: `NetworkFailureReporter cannot be provided` (do `AssetsModule` tiêm mà chưa có binding). Kết luận ghi vào mục 3.1: binding của bộ điều khiển phải vào cùng task với phần sửa `:core:assets`. Thêm binding tạm thì qua bước này.
  - Lần 2: ktlint báo `argument-list-wrapping` ở test tạm (lỗi định dạng của code thử); `ktlintFormat` sửa.
  - Lần 3: "Test JVM: 118 test, 0 lỗi ... ./app: 6 test, 0 lỗi; ./core/assets: 14 test, 0 lỗi; ./core/network: 5 test, 0 lỗi", lint, build staging qua, "Cổng chất lượng: QUA". `check.sh` ghi ảnh còn thiếu bằng `:app:recordRoborazziStagingDebug` ở lần 2 và so bằng `:app:verifyRoborazziStagingDebug` ở lần 3: đường so ảnh chuẩn của `:app` chạy được.
  - Nút quay lại: gọi dispatcher của dialog thì `BackHandler` trong dialog nhận (`onBack` = 1), activity không kết thúc, đích dưới giữ nguyên. Gọi dispatcher của activity khi popup hiện: `onBack` = 0, activity không kết thúc (Robolectric không định tuyến phím của activity vào cửa sổ dialog; trên máy thật phím quay lại đi vào cửa sổ trên cùng, là dialog). Vì vậy test dùng dispatcher của dialog, không dùng dispatcher của activity (cùng kết luận với chạy thử của T-013). Phương án dự phòng ở mục 3.4 chưa cần.
  - Thay đổi sau chạy thử (rà soát trước khi chạy): `robolectric.properties` thêm dòng `application=android.app.Application`, và hàm thuần `reportedScreenOf`. Lần chạy thử dùng `sdk=35` cộng `@Config(application = Application::class)` ở từng lớp, tức cùng lớp `Application` khi chạy test, nên kết quả trên không đổi; chưa chạy lại. `reportedScreenOf` là hàm thuần trên chuỗi, không cần chạy thử.
  - Chưa chạy thử: `NetworkDialogController` bản sửa (logic thuần, thời gian ảo, cách test giống app đã qua CI của app), `NetworkDialog` theo thiết kế mới và bốn ảnh chuẩn thật.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** thu thập dữ liệu (hai sự kiện phân tích, điểm 4), module core dùng chung (`:core:network`, `:core:designsystem`, `:core:assets`, `:core:ads`, điểm 7), cấu hình build (`app/build.gradle.kts`, `core/assets/build.gradle.kts`, `core/network/build.gradle.kts`, điểm 7). Không chạm vị trí, định dạng hay tần suất quảng cáo, quyền, SDK, hộp thoại đồng ý. Mức Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Năm module (`:core:network`, `:core:designsystem`, `:core:assets`, `:core:ads`, `:app`) và `:feature:sample` |
| B. Độ rõ của yêu cầu | 1 | Spec, thiết kế đã duyệt, code app đã chạy; AI tự quyết các điểm khác app (bảng mục 3.1) và nút quay lại (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh logic và chữ có test, ảnh chuẩn của `:app` được so; chặn chạm thật, TalkBack, quay lại từ nền và quảng cáo thật chỉ kiểm tay |
| D. Ảnh hưởng chéo | 3 | Popup nằm trên mọi màn hình; `PrimaryButton` dùng chung; `AdsController.kt`, `MainActivity.kt`, `app/build.gradle.kts`, `SampleScreen.kt` cũng do T-009, T-010, T-013, T-014 sửa; `:core:assets` đổi phụ thuộc |
| E. Độ mới của kỹ thuật | 2 | Cửa sổ `Dialog` riêng, test dispatcher của dialog bằng Robolectric, ảnh chuẩn đầu tiên của `:app` (đã chạy thử, mục 4.1) |
| **Tổng** | 11 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `PrimaryButton.kt` (tham số mới có mặc định, ảnh danh mục thiết kế không đổi), `AdsController.kt`, `MainActivity.kt`, `AppNavHost.kt`, `DefaultRemoteAssets.kt`, `AssetsModule.kt`, ba file `build.gradle.kts`, `strings.xml` của `:app`, `SampleScreen.kt`, `CLAUDE.md`.
- File mà nơi khác đọc theo tên (L-020): `scripts/new-app.sh` thay dòng `app_name` trong `app/src/main/res/values/strings.xml` và sửa `app/build.gradle.kts` (`applicationId`, `namespace`): plan giữ nguyên dòng `app_name` và không đụng các dòng đó. Test của script ở `scripts/tests/run.sh` (đọc `app/src/main/res/values/strings.xml` trong bản chép tạm) vì vậy không bị ảnh hưởng. `scripts/release.sh` nhắc `app/build.gradle.kts` vì tham số `-P` của bản production: plan không đụng `signingConfigs`, `productFlavors` hay phần kiểm tham số, không mở hay sửa `release.sh`. `scripts/check.sh` đọc `app/src/test` để tìm `captureRoboImage("src/test/screenshots/...")`: test của plan đúng dạng. `docs/STATUS.md` do `scripts/status.py` sinh. `CLAUDE.md`: các nơi nhắc tên (`.github/ISSUE_TEMPLATE/from-app.yml`, chú thích "CLAUDE.md, step 10" trong `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `scripts/device-session.sh`, `scripts/status.py`) chỉ trích dẫn.
- Task khác:
  - T-016 (bắt buộc trước): `ConnectivitySource`, `lostAfterDelay`, `NetworkSettings`, `NetworkModule`, `Fakes.kt`.
  - T-018 (bắt buộc trước): `AssetFailureReason`, `AssetResult.Unavailable(reason)`.
  - T-009 (bắt buộc trước): `onStart`, `onStop` của `MainActivity`, sửa `AdsController.kt`.
  - T-013: cùng hợp đồng `FullScreenAdPresence`; đặt `FullScreenAdHost()` trong `MainActivity`, plan này giữ dòng đó.
  - T-010: đổi đáy `SampleScreen`; chạy sau T-010 thì ảnh chuẩn có phần dưới mới nhất.
  - T-014: có thể thêm Roborazzi vào `app/build.gradle.kts` trước; task sau không thêm lại.
- Đề xuất thứ tự chạy: sau T-016, T-018, T-009 và PR #81; sau T-010, T-013 nếu cùng phiên. Cuối chuỗi vì chạm nhiều file chung nhất.

## 7. Điều kiện dừng riêng

- API của T-016 hay T-018 khác hợp đồng ở mục 2.
- `fullScreenAdShowing` đã có nhưng khác tên hay kiểu hợp đồng.
- Lint `:app:lintStagingDebug` báo `MissingTranslation` hay lỗi chuỗi nằm ngoài các file của plan: dừng, không thêm bản dịch cho module khác, không tắt quy tắc.
- Phương án dự phòng ở mục 3.4 cũng không qua.
- Ảnh chụp bằng `onRoot().captureRoboImage` không có popup hay lớp phủ sau 3 lần sửa.
- Cần sửa `scripts/check.sh`, script đối chiếu thiết kế, `build-logic/` hay `gradle/libs.versions.toml`.
- `checkModuleDependencies` báo vòng phụ thuộc giữa `:core:assets` và `:core:network`.

## 8. Câu hỏi cho người duyệt

1. **Nút quay lại khi popup hiện.** App camera-pose đưa app về nền (`moveTaskToBack(true)`); bản 2 của plan này không làm gì. Cả hai đều không đóng popup (spec KB-2). Đề xuất: theo app, vì người dùng có cách thoát quen thuộc mà popup vẫn còn khi mở lại.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI (đưa app về nền).
2. **Báo lỗi tải khi máy vẫn có mạng.** App hiện popup ngay khi tải file lỗi vì mạng (kể cả quá thời gian). Spec KB-3 nói quá thời gian khi vẫn có mạng thì không hiện popup. Đề xuất: chỉ hiện khi máy cũng báo không có mạng; còn mạng thì màn hình của feature tự báo lỗi (như hiện nay) và chỉ ghi log.
   - Trả lời (anh Nhật, 2026-10-23): hiện ngay như app, kể cả khi máy vẫn báo có mạng; spec KB-3 sửa theo (câu hỏi 5 của spec). Plan đã đổi ở mục 2, 3.1, 4.
3. **Icon wifi-off.** Lấy icon Material Symbols Rounded "wifi_off" của app (Apache 2.0, kèm giấy phép) thay vì vẽ lại icon nét của Figma. Ảnh so sánh có thể lệch nhẹ ở icon. Đề xuất: chấp nhận; hoặc thay icon trên Figma bằng Material Symbols cho khớp.
   - Trả lời (anh Nhật, 2026-10-23): dùng Material Symbols "wifi_off"; AI đổi icon trên Figma sang Material Symbols cho khớp, xuất lại ảnh thiết kế trước phiên.
4. **`NetworkStatus` chưa ai dùng trong dự án mẫu.** Giữ để app dùng (dừng đếm ngược, tải lại màn hình lỗi khi `recovered`), như app camera-pose. Đề xuất: giữ.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **Spec network đang `changed`** (mục 6 về quyền). Plan này không thêm quyền; đề xuất duyệt sau khi spec được xác nhận lại, cùng T-016.
   - Trả lời (anh Nhật, 2026-10-23): spec network đã xác nhận lại cùng lúc duyệt plan.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 3:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua hai lần rà soát độc lập trước khi chạy, ghi ở RUN-20261023-01)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-23; câu hỏi ở mục 8 đã trả lời ngay tại đó.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-20261023-01, phiên bản plan 3

- **Kết quả:** đã merge
- **Nhánh, PR:** `claude/T-017-popup-mat-mang`, PR #113, merge commit `95f8673` lúc 12:22 UTC
- **Đã làm:** `NetworkDialogController` trong `:core:network` (theo dõi kết nối sau màn hình loading, chờ `network_lost_delay_seconds`, "Thử lại" kiểm tối đa 2 giây, tự đóng khi có mạng, lỗi tải file `NETWORK` hiện popup ngay kể cả khi máy báo có mạng, quảng cáo toàn màn hình giữ popup ẩn); `:core:assets` báo lỗi mạng; `NetworkDialog`, `NetworkDialogHost`, nút chính có trạng thái đang tải; nối trong `MainActivity`, `AppNavHost`; `reportedScreenOf`, `reportsScreen`; `app/src/test/resources/robolectric.properties`; 4 ảnh chuẩn đầu tiên của `:app`; ảnh so sánh thiết kế, README thiết kế mục 7 và 8. Đọc mã nguồn PR #23 của camera-pose qua GitHub API (clone bị từ chối). Bước 3 đến 5 do phiên con làm.
- **Test viết trước:** 55 test (controller 22, NetworkSettings 1, assets 5, NetworkDialog 7, PrimaryButtonLoading 2, NetworkDialogHost 8, ScreenName 7, screenshot 4), commit `6849b6e` trước code, kèm khung rỗng. 51 test không phải screenshot thất bại ở phần khẳng định (`expected:<Shown(...)> but was:<Hidden>`, `expected:<2000> but was:<0>`, `Actual width is 0.0.dp, expected 312.0.dp`, `no dialog window was opened`, `expected:<[sample]> but was:<[unknown]>`...). 4 screenshot qua trước khi có code vì chưa có ảnh chuẩn, đúng ngoại lệ 4 của plan. 11 test có bước đối chứng (L-055), trong đó `back_whileHidden_navigatesBack` qua ngay với khung rỗng, thêm đối chứng thì thất bại đúng lý do.
- **Rà soát độc lập:** 0 chặn, 1 nên sửa, 3 ghi chú. Nên sửa: khi activity dựng lại (xoay máy, đổi sáng tối, đổi ngôn ngữ), khung hình đầu không có mục trên back stack nên `setScreen(null)` chạy, popup chớp tắt, ghi thêm một `network_lost_shown`, mất dòng "vẫn chưa có kết nối" và số lần thử lại; ca KB-3 thì popup biến mất. Đã sửa ở `ee4ba98`: `reportsScreen(route)` không báo màn hình khi back stack chưa có mục (sửa ở `AppNavHost`, không ở controller, để giữ test `backToLoading_hidesWithoutRestoredEvent`). 5 test mới ở `5821ed3`: `activityRecreated_firstFrameWithoutEntry_isNotReported` (`expected:<[sample]> but was:<[null, sample]>`) và `coldStart_reportsLoadingThenSample` thất bại trước khi sửa; `backToLoading_isStillReported`, `sameScreenReportedAgain_keepsDialogAndRecordsNothing`, `networkFailureWhileOnline_survivesSameScreenReportedAgain` là test chặn hồi quy, qua ngay. Ghi chú: `tools:ignore="MissingTranslation"` trên cả `<resources>` của `app/src/main/res/values/strings.xml` (câu hỏi 3 đã duyệt; không gắn được lên riêng dòng `app_name` vì `scripts/new-app.sh` tìm đúng dòng đó), phạm vi rộng: chuỗi mới thiếu bản tiếng Anh sẽ không bị lint báo; đã sửa chú thích cho đúng phạm vi. `duration_ms` đặt lại khi popup hiện lại sau quảng cáo toàn màn hình (spec định nghĩa là thời gian popup hiện). Ảnh `stillOffline` không có thẻ quảng cáo ở đáy còn ảnh `default` có.
- **Kết quả CI:** `scripts/check.sh` cục bộ qua (410 test JVM, 0 lỗi, 34 ảnh, có `:app:verifyRoborazziStagingDebug`); `pr-check.yml` xanh lần đầu (39951626820).
- **Lệch so với plan:** (1) không sửa `:core:ads`: `FullScreenAdPresence.kt`, test của nó và `AdsController.fullScreenAdShowing` đã có từ T-013 cùng API; (2) lint chỉ báo `MissingTranslation` cho `app_name`, xử lý theo câu hỏi 3; (3) test `shown_usesAppStrings_vi` và khung ảnh chuẩn ghim `vi` (`vi-w411dp-h891dp-xxhdpi`, `vi-w360dp-h640dp-xxhdpi`) vì Robolectric mặc định tiếng Anh; (4) thêm `NetworkDialogTestTags.ICON`, `FakeConnectivitySource.isConnectedCalls`; test dispatcher của hộp thoại kiểm callback thay vì thêm tham số chỉ cho test; (5) thêm `reportsScreen` khi sửa sau rà soát. Không dùng phương án dự phòng (`ShadowDialog.getLatestDialog()` ép kiểu `ComponentDialog` chạy được).
- **Việc còn lại:** không có.
- **Cần người xem:** ảnh chuẩn mới `app/src/test/screenshots/NetworkLostScreen_default_{light,dark}.png`, `NetworkLostScreen_stillOffline_light.png`, `NetworkLostScreen_stillOffline_small.png`; ảnh so sánh trong `docs/features/network/design/review/` (lệch 1,8% và 2,5%), ghi nhận xét vào README thiết kế mục 8. Kiểm thủ công trên staging: chặn chạm thật, TalkBack, nút quay lại đưa app xuống nền; loading bật và chế độ máy bay thì không có popup; loading tắt và chế độ máy bay thì popup hiện ngay; mất mạng giữa chừng thì popup sau khoảng 2 giây, "Thử lại" có vòng chờ rồi "vẫn chưa có kết nối", có mạng lại thì tự đóng và giữ số đếm; Wi-Fi không có Internet thì có popup; log `VGL.Network`.
