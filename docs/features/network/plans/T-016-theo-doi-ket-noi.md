---
ticket: T-016
title: Theo dõi có Internet dùng được, dùng chung cho cả app
status: approved
version: 2
risk_score:                  # để trống: chạm điểm chặn (core, permission)
risk_level: high
token_estimate: 20M          # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-23)
blocking_points: [core, permission]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-23
---

# PLAN T-016: THEO DÕI CÓ INTERNET DÙNG ĐƯỢC, DÙNG CHUNG CHO CẢ APP

**Phiên bản 2 (2026-10-23), đổi so với bản 1 (đã duyệt 2026-10-23, nội dung ở commit `4cb757a`):**

- Lấy code `:core:network` của app camera-pose làm nền (issue #72, PR VGL-Studio/camera-pose#23, commit merge `e23066751fd93e3f945137558287843b506620b3`). Bản 1 tự thiết kế `NetworkMonitor` (`isOnline`, `checkNow()`) và `DefaultNetworkMonitor`; bản 2 bỏ hai lớp đó, dùng `ConnectivitySource` của app (sửa định nghĩa có mạng) và tách phần chờ trước khi báo mất mạng thành một hàm riêng để `NetworkDialogController` của T-017 (cũng lấy từ app) dùng. Bảng đổi so với app ở mục 3.1.
- Rà soát RUN-20261023-01: thêm `core/network/src/test/resources/robolectric.properties` (`sdk=35`).
- Rà soát trước khi chạy: thêm test Robolectric `isConnected_readsActiveNetworkCapabilities` cho `AndroidConnectivitySource.isConnected()` (mục 4).
- Mẫu plan mới: mục 3.3 có dòng "Quyền truy cập:" và "Thư viện và SDK:" (dẫn tới spec network mục 6), mục 3.4, mục 4.1 với lần chạy thử thật bằng `scripts/check.sh`.
- Câu hỏi đã trả lời ở bản 1 (quyền ở module, module mới, không đặt trần thời gian chờ) giữ nguyên quyết định; câu hỏi 3, 5, 6 của bản 1 chuyển sang T-017 vì phần đó nay nằm trong bộ điều khiển popup.

## 1. Tóm tắt

Tạo module mới `:core:network` với nguồn kết nối `ConnectivitySource` (chép từ app camera-pose, đổi định nghĩa: mạng mặc định phải có cả `NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`, ADR-0024), hàm `lostAfterDelay` phát một lần khi mất mạng liên tục đủ `network_lost_delay_seconds` (khóa mới khai báo bằng `RemoteKey` của T-007, mặc định 2), và Hilt module cấp nguồn kết nối cho T-017. Mọi tiêu chí về thời gian kiểm bằng unit test với thời gian ảo, ánh xạ capability và callback hệ thống kiểm bằng Robolectric; chưa có giao diện (popup là T-017).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md` (phần bộ theo dõi; phần popup là T-017):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi Wi-Fi bật nhưng không vào được Internet thì app coi là không có mạng | Mạng mặc định có `INTERNET` mà thiếu `VALIDATED` (Wi-Fi có cổng đăng nhập, Wi-Fi không ra Internet) là không có mạng. App camera-pose chỉ đòi `INTERNET` (plan T-011 của app, câu hỏi 1: có máy xác nhận chậm); dự án mẫu theo spec và ADR-0024, đòi cả hai. Không xét loại kết nối |
| KB-1 | Khi màn hình loading tắt và không có mạng thì popup hiện ngay | Phần của T-016: `isConnected()` đọc trạng thái hệ thống ngay, không chờ. T-017 gọi hàm này khi rời màn hình loading để hiện popup ngay |
| KB-2 | Khi đang dùng mà mất Internet thì sau một khoảng chờ ngắn (mặc định 2 giây) popup hiện | `lostAfterDelay` phát một lần khi nguồn báo mất liên tục đủ `network_lost_delay_seconds` giây; có mạng lại trong lúc chờ thì không phát. Thời gian chờ đọc lại ở mỗi lần mất, nên đổi trên Firebase có tác dụng từ lần mất sau |
| KB-2 | Khi bấm "Thử lại" mà vẫn không có mạng / có mạng trở lại thì popup tự đóng | Phần của T-016: `isConnected()` cho nút "Thử lại", `connectivity()` báo có mạng ngay (không chờ). Quyết định đóng popup là T-017 |
| KB-3 | Khi vẫn có mạng nhưng máy chủ lỗi hay quá thời gian thì popup không hiện | Nguồn kết nối chỉ dựa vào trạng thái mạng của hệ thống, không dựa vào kết quả tải. Một test giữ điều này |

Thiết kế tham chiếu: Không có thiết kế riêng cho ticket này. Thiết kế popup (`features/network/design/`) thuộc T-017; T-016 không có màn hình nên không có screenshot test.

ADR liên quan: ADR-0024 (định nghĩa có mạng, module core dùng chung), ADR-0023 (khóa khai báo một chỗ, sinh bảng khóa), ADR-0003 (Remote Config cho tham số), ADR-0001 (quy tắc phụ thuộc module). ADR-0013, ADR-0014, ADR-0020, ADR-0022: đã đọc, ticket không chạm quảng cáo hay thiết kế.

## 3. Giải pháp

### 3.1. Cách làm

**Nguồn code.** Mọi file của module mới lấy từ PR VGL-Studio/camera-pose#23 (T-011 của app, merge `e23066751fd93e3f945137558287843b506620b3`, đọc bằng `gh pr diff 23 -R VGL-Studio/camera-pose`), đổi tên gói `com.vglstudio.camera.pose.line.guide` thành `com.vglstudio.template`:

| File ở dự án mẫu | Nguồn ở app | Cách lấy |
| --- | --- | --- |
| `core/network/build.gradle.kts` | `core/network/build.gradle.kts` | Chép, đổi `namespace`; bỏ `:core:logging` (T-017 thêm khi cần); thêm ba dòng `testImplementation` cho Robolectric |
| `core/network/consumer-rules.pro` | cùng tên | Chép (rỗng) |
| `core/network/src/main/AndroidManifest.xml` | cùng tên | Chép, sửa chú thích (dẫn spec network mục 6) |
| `.../core/network/ConnectivitySource.kt` | `core/network/.../ConnectivitySource.kt` | Chép, sửa định nghĩa có mạng (bảng dưới), tách `hasUsableInternet` ra hàm `internal` để test |
| `.../core/network/LostAfterDelay.kt` | phần `collectLatest { if (!connected) showAfterDelay() }` và `showAfterDelay()` của `NetworkDialogController.kt` | Tách thành hàm thuần trên `Flow` |
| `.../core/network/NetworkSettings.kt` | `core/network/.../NetworkSettings.kt` | Viết lại theo `RemoteKey` (T-007); hai hằng thời gian của nút "Thử lại" để T-017 thêm |
| `.../core/network/di/NetworkModule.kt` | `core/network/.../di/NetworkModule.kt` | Chép phần `connectivitySource` và `@NetworkScope`; phần `@Binds` cho bộ điều khiển để T-017 thêm |
| `core/network/src/test/.../Fakes.kt` | `core/network/src/test/.../Fakes.kt` | Chép `FakeConnectivitySource`; `FakeRemoteSettings` viết lại theo `RemoteSettings.value(key)` của T-007; bỏ `FakeFeatureFlags` |
| `core/network/src/test/.../NetworkSettingsTest.kt` | cùng tên | Viết lại cho `RemoteKey`, mặc định 2 |
| `core/network/src/test/.../LostAfterDelayTest.kt`, `AndroidConnectivitySourceTest.kt` | không có ở app | Mới; các trường hợp thời gian lấy từ `NetworkDialogControllerTest` của app (`deviceOffline_shownOnlyAfterDelay`, `delay_followsRemoteSetting`, `flapping_underDelay_showsNothing`) |

**Phần đổi so với app:**

| Điểm | App camera-pose (PR #23) | Dự án mẫu (plan này) | Lý do |
| --- | --- | --- | --- |
| Định nghĩa có mạng | Chỉ `NET_CAPABILITY_INTERNET` | `INTERNET` và `VALIDATED` | Spec KB-1 tiêu chí 3, ADR-0024 |
| `onAvailable` | Gửi `true` | Bỏ: lúc `onAvailable` hệ thống chưa xác nhận Internet; `onCapabilitiesChanged` báo đủ | Theo định nghĩa trên |
| Khóa thời gian chờ | Chuỗi `"network_lost_delay_seconds"`, hằng mặc định 3, đọc bằng `settings.long(key, default)` | `RemoteKey.long("network_lost_delay_seconds", 2, ...)`, đọc bằng `settings.value(key)`, có trong `AppRemoteKeys` và bảng khóa sinh ra | Spec mục 8 (mặc định 2), ADR-0023, T-007 |
| Thời gian chờ | Nằm trong `NetworkDialogController` | Hàm `lostAfterDelay` riêng trong T-016, T-017 dùng | Ticket T-016 giao phần theo dõi; test thời gian tách khỏi popup |
| Giá trị chờ âm | Không xử lý (`delay` âm coi như 0) | `coerceAtLeast(0)` rõ ràng, có test | Quyết định bản 1, câu hỏi 4 |
| Quyền `ACCESS_NETWORK_STATE` | Khai báo ở manifest của module và của `:app` | Chỉ ở manifest của module, không sửa `app/src/main/AndroidManifest.xml` | Spec mục 6 ghi khai báo ở module theo dõi kết nối |
| Cờ tính năng | `NETWORK_DIALOG` (mặc định tắt) | Không có | ADR-0024, spec mục 8: luôn bật. Cờ nằm ở bộ điều khiển của app, T-016 không có |
| `NetworkErrors.kt` (`Throwable.isNetworkFailure()`) | Có | Không lấy | Phân loại lỗi tải do `AssetFailureReason` của T-018 làm; giữ một chỗ |
| Tên gói | `com.vglstudio.camera.pose.line.guide` | `com.vglstudio.template` | Dự án mẫu |

**Chọn module: `:core:network`** (giữ quyết định bản 1, câu hỏi 2). Phụ thuộc `implementation(project(":core:config"))`, chỉ core phụ thuộc core (ADR-0001). Thêm `include` trong `settings.gradle.kts`, một dòng phụ thuộc trong `app/build.gradle.kts` (để Hilt gom module), một dòng trong bảng module của `CLAUDE.md`. Convention plugin có sẵn (`template.android.library`, `template.hilt`), không sửa `build-logic/` hay `gradle/libs.versions.toml`. `scripts/check.sh` tự lặp `core/*/`, `checkModuleDependencies` tự xét mọi module.

**`ConnectivitySource.kt`** (`internal`):

```kotlin
internal interface ConnectivitySource {
    /** Usable Internet right now: the default network has INTERNET and VALIDATED (ADR-0024). */
    fun isConnected(): Boolean

    /** Current value at once, then every change. The system callback is registered only while collected. */
    fun connectivity(): Flow<Boolean>
}

internal fun isUsableInternet(hasInternet: Boolean, isValidated: Boolean): Boolean = hasInternet && isValidated

internal fun NetworkCapabilities?.hasUsableInternet(): Boolean
```

`AndroidConnectivitySource(context)`: `isConnected()` đọc `getNetworkCapabilities(activeNetwork).hasUsableInternet()`; `connectivity()` là `callbackFlow`: gửi `isConnected()`, `registerDefaultNetworkCallback` (có từ API 24, minSdk 26), `onCapabilitiesChanged` gửi `hasUsableInternet()`, `onLost` gửi `false`, `awaitClose { unregisterNetworkCallback }`, rồi `distinctUntilChanged()`. Không đọc tên Wi-Fi, địa chỉ IP hay định danh mạng.

**`LostAfterDelay.kt`** (`internal`):

```kotlin
/** Emits once each time the source stays offline for delaySeconds(); a shorter loss emits nothing. */
internal fun ConnectivitySource.lostAfterDelay(delaySeconds: suspend () -> Long): Flow<Unit> =
    connectivity().transformLatest { connected ->
        if (!connected) {
            delay(delaySeconds().coerceAtLeast(0) * 1_000)
            emit(Unit)
        }
    }
```

`transformLatest` hủy lần chờ cũ khi có giá trị mới: mạng chập chờn ngắn hơn thời gian chờ không bao giờ phát. Giá trị đầu là mất mạng thì cũng chờ (T-017 tự hiện ngay bằng `isConnected()` khi rời loading).

**`NetworkSettings.kt`** (`public object`, theo mẫu `AdsSettings` của T-007):

```kotlin
object NetworkSettings {
    val LOST_DELAY_SECONDS: RemoteKey<Long> = RemoteKey.long(
        name = "network_lost_delay_seconds",
        default = 2,
        description = "Seconds without usable Internet before the network dialog shows (ADR-0024).",
    )

    fun all(): List<RemoteKey<*>> = listOf(LOST_DELAY_SECONDS)
}
```

**Hilt (`di/NetworkModule.kt`, `internal`).** `@Provides @Singleton ConnectivitySource` từ `@ApplicationContext`; qualifier `internal annotation class NetworkScope` và `@Provides @Singleton @NetworkScope CoroutineScope(SupervisorJob() + Dispatchers.Default)` cho T-017. Chưa ai tiêm hai thứ này cho tới T-017, nên hành vi app không đổi.

**Gom khóa ở `:app`.** `AppRemoteKeys.byModule` (T-007) thêm `":core:network" to NetworkSettings.all()` sau `":core:ads"`, rồi `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json`. Không sửa tay hai file này.

**Nơi khác đọc file plan đổi (L-020).** Đã `grep -rn` trên `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/androidTest`:

- `settings.gradle.kts`, `app/build.gradle.kts`: `scripts/new-app.sh` thay đúng dòng `rootProject.name` và chuỗi địa chỉ R2; plan không đổi hai dòng đó. `scripts/release.sh` nhắc tới `app/build.gradle.kts` vì truyền tham số `-P` của bản production; plan chỉ thêm một dòng `implementation(project(":core:network"))`, không đổi biến thể hay tham số, không mở hay sửa `release.sh`. `scripts/new-app.sh` dời mọi thư mục `*/src/*/kotlin/com/vglstudio/template` và thay tên gói trong mọi file, nên module mới cũng được đổi gói (test `new-app.sh replaces the package outside ADRs` của `scripts/tests/run.sh`). `.github/workflows/pr-check.yml` lọc `core/**`, module mới tự kích hoạt CI.
- `AppRemoteKeys.kt`, `RemoteConfigKeysTest.kt`: do T-007 tạo, có trong danh sách file.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: đọc bởi `RemoteConfigKeysTest` qua system property của T-007; sinh lại trong cùng PR.
- `CLAUDE.md`: chỉ thêm một dòng vào bảng module; các nơi nhắc tên file (`.github/ISSUE_TEMPLATE/from-app.yml`, chú thích "CLAUDE.md, step 10" trong `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `scripts/device-session.sh`, `scripts/status.py`) chỉ trích dẫn, không đọc nội dung.

**Thứ tự commit (L-025).**

1. Khung module: `settings.gradle.kts`, `core/network/build.gradle.kts`, `consumer-rules.pro`, manifest, `robolectric.properties`, dòng phụ thuộc ở `app/build.gradle.kts`.
2. Test (bước 3): mọi file trong `core/network/src/test/kotlin/`, phần thêm vào `RemoteConfigKeysTest.kt`. Không biên dịch được vì `ConnectivitySource`, `lostAfterDelay`, `NetworkSettings` chưa có: đúng lý do mong đợi.
3. Code (bước 4): mọi file `core/network/src/main/kotlin/`, `AppRemoteKeys.kt`.
4. Sinh file: xác nhận `keysFile_matchesCode`, `templateFile_matchesCode` thất bại vì thiếu khóa, rồi chạy lệnh ghi lại.
5. Tài liệu: `CLAUDE.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | Thêm `include(":core:network")` sau `:core:logging` |
| Tạo | `core/network/build.gradle.kts` | Plugin `template.android.library`, `template.hilt`; `namespace = "com.vglstudio.template.core.network"`; `implementation(project(":core:config"))`; `testImplementation(libs.robolectric)`, `testImplementation(libs.androidx.test.core)`, `testImplementation(libs.androidx.test.ext.junit)` |
| Tạo | `core/network/consumer-rules.pro` | Rỗng, như các module core khác |
| Tạo | `core/network/src/main/AndroidManifest.xml` | `uses-permission android.permission.ACCESS_NETWORK_STATE`, chú thích dẫn spec network mục 6; không có thuộc tính `package` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/ConnectivitySource.kt` | `ConnectivitySource`, `isUsableInternet`, `hasUsableInternet`, `AndroidConnectivitySource` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/LostAfterDelay.kt` | `ConnectivitySource.lostAfterDelay` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkSettings.kt` | Khóa `network_lost_delay_seconds` (long, 2), `all()` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/di/NetworkModule.kt` | Hilt: `ConnectivitySource`, `NetworkScope` |
| Tạo | `core/network/src/test/resources/robolectric.properties` | `sdk=35` (rà soát RUN-20261023-01) |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/Fakes.kt` | `FakeConnectivitySource`, `FakeRemoteSettings` |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/LostAfterDelayTest.kt` | Thời gian chờ, thời gian ảo |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/AndroidConnectivitySourceTest.kt` | Robolectric: ánh xạ capability, callback, hủy đăng ký |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/NetworkSettingsTest.kt` | Tên, kiểu, mặc định của khóa |
| Sửa | `app/build.gradle.kts` | Thêm `implementation(project(":core:network"))` sau `:core:logging` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/config/AppRemoteKeys.kt` | Thêm `":core:network" to NetworkSettings.all()` (file do T-007 tạo) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` | Thêm `networkKeys_areListed` (file do T-007 tạo) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng lệnh ghi lại |
| Sửa | `docs/remote-config-template.json` | Sinh lại bằng lệnh ghi lại |
| Sửa | `CLAUDE.md` | Bảng "Cấu trúc module": thêm dòng `:core:network` (theo dõi có Internet dùng được, popup mất mạng; ADR-0024) |
| Sửa | `docs/features/network/tickets/T-016-theo-doi-ket-noi.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/network/plans/T-016-theo-doi-ket-noi.md` | Nhật ký thực hiện |

Không có ảnh chuẩn screenshot. File được bảo vệ (`.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/`, `gradle/libs.versions.toml`, `scripts/release.sh`): không chạm. `app/src/main/AndroidManifest.xml`: không chạm.

- `app/build.gradle.kts` (L-020): `scripts/setup-check.sh` và `scripts/tests/run.sh` chỉ đọc dòng `ASSETS_BASE_URL` và `applicationId` của file này; plan không đổi hai dòng đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có (spec mục 8, ADR-0024). Khóa Remote Config mới `network_lost_delay_seconds` (long, mặc định 2), đúng spec mục 8.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm (hai sự kiện thuộc T-017).
- File R2: Không chạm.
- **Quyền truy cập:** `ACCESS_NETWORK_STATE` trong manifest của `:core:network` (spec network mục 6: quyền thường, khai báo ở module theo dõi kết nối; cần cho `registerDefaultNetworkCallback`, `getNetworkCapabilities`, `activeNetwork` trên mọi API từ 26 tới 36, không có `maxSdkVersion`). Quyền đã có trong manifest gộp qua Google Mobile Ads và Firebase, nên danh sách quyền của app không đổi (kiểm ở mục 4.1). Không thêm: `ACCESS_WIFI_STATE` (không đọc thông tin Wi-Fi), `INTERNET` (đã có, module không mở kết nối).
- **Thư viện và SDK:** Không thêm thư viện mới. `ConnectivityManager`, `NetworkCapabilities` thuộc Android SDK. `kotlinx-coroutines` có sẵn qua convention. Test dùng `libs.robolectric`, `libs.androidx.test.core`, `libs.androidx.test.ext.junit` (đã có trong `gradle/libs.versions.toml`), khai báo `testImplementation` trong `core/network/build.gradle.kts` vì convention `template.android.library` không tự thêm; Turbine, coroutines-test có sẵn qua `KotlinAndroid.kt`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** shadow mạng của Robolectric 4.15.1 (`ShadowNetworkCapabilities.newInstance`, `ShadowConnectivityManager.getNetworkCallbacks`, `ShadowConnectivityManager.setNetworkCapabilities`) với `sdk=35`. Hai hàm đầu đã chạy thử qua (mục 4.1); `setNetworkCapabilities` (test `isConnected_readsActiveNetworkCapabilities`, thêm sau chạy thử) chưa chạy.
- **Điều kiện chuyển:** test Robolectric của `AndroidConnectivitySourceTest` thất bại vì shadow thiếu hay đổi hành vi sau 2 lần sửa.
- **Cách làm thay:** kiểm ánh xạ capability bằng hàm thuần `isUsableInternet` (test JVM thường, giữ tên test), và kiểm đăng ký, hủy callback, `isConnected()` bằng `mockk` của `ConnectivityManager` (`activeNetwork`, `getNetworkCapabilities`) (`mockk` có sẵn qua `KotlinAndroid.kt`). Tiêu chí và tên test ở mục 4 giữ nguyên. Không thêm file mới.

## 4. Kiểm thử

Test thời gian dùng `runTest`, thời gian ảo (`advanceTimeBy`, `currentTime`) và Turbine. Thời gian chờ mặc định 2 giây trừ khi ghi khác.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2, tiêu chí 1: chờ trước khi báo mất | Unit test | `LostAfterDelayTest.lossLongerThanDelay_emitsAfterDelay`: nguồn `true`, đổi `false` lúc 0; ở 1 999 ms chưa phát, ở 2 001 ms nhận một `Unit` |
| KB-2, tiêu chí 1: mạng chập chờn không báo | Unit test | `LostAfterDelayTest.flapping_emitsNothing`: ba lần mất 1 500 ms rồi có lại; tới 10 000 ms không phát gì |
| Thời gian chờ đọc lại mỗi lần mất | Unit test | `LostAfterDelayTest.delayIsReadAtEachLoss`: hàm chờ trả 2 rồi 5; lần mất thứ nhất phát sau 2 000 ms, lần thứ hai sau 5 000 ms |
| Giá trị chờ không hợp lệ | Unit test | `LostAfterDelayTest.zeroOrNegativeDelay_emitsAtOnce`: hàm chờ trả 0 rồi -3; cả hai lần phát ở cùng `currentTime` lúc mất |
| Mất mạng ngay từ đầu | Unit test | `LostAfterDelayTest.offlineAtStart_emitsAfterDelay`: nguồn bắt đầu `false`; phát ở 2 000 ms, không trước |
| KB-2, tiêu chí 4 (phần T-016): có mạng lại thì không phát | Unit test | `LostAfterDelayTest.backOnlineDuringDelay_cancelsLoss`: `false` lúc 0, `true` lúc 1 000; tới 10 000 ms không phát |
| KB-1, tiêu chí 3: Wi-Fi bật nhưng chưa xác nhận | Unit test (Robolectric) | `AndroidConnectivitySourceTest.wifiWithInternetNotValidated_isNotUsable`: `ShadowNetworkCapabilities.newInstance()`, thêm `TRANSPORT_WIFI`, `INTERNET`, không `VALIDATED`: `false`; thêm cả hai: `true`; `null`: `false`. `isUsableInternet_needsBoth`: bốn tổ hợp của hàm thuần |
| Callback hệ thống được nối đúng | Unit test (Robolectric) | `AndroidConnectivitySourceTest.callback_reportsCapabilitiesAndLoss_thenUnregisters`: thu thập `connectivity()`; giá trị đầu `false` (mạng mặc định của Robolectric chưa xác nhận, đã thấy khi chạy thử); `shadowOf(manager).networkCallbacks` có đúng 1; gọi `onCapabilitiesChanged` chưa xác nhận, rồi đã xác nhận, rồi `onLost`: nhận `true`, `false`; hủy thu thập thì `networkCallbacks` rỗng |
| `isConnected()` đọc đúng mạng đang dùng | Unit test (Robolectric) | `AndroidConnectivitySourceTest.isConnected_readsActiveNetworkCapabilities`: mạng mặc định của Robolectric (chưa xác nhận): `false`; `shadowOf(manager).setNetworkCapabilities(manager.activeNetwork, caps)` với `caps` có `INTERNET` và `VALIDATED`: `true`; đổi sang `caps` chỉ có `INTERNET`: `false`. Gọi thẳng `isConnected()`, không qua `connectivity()` |
| KB-3: chỉ dựa vào hệ thống | Unit test | `LostAfterDelayTest.onlineSource_neverEmits`: nguồn giữ `true` 60 000 ms ảo, không phát gì (không có đầu vào nào khác) |
| Đăng ký callback chỉ khi có nơi thu thập | Unit test | `LostAfterDelayTest.collectsSourceOnlyWhileCollected`: `FakeConnectivitySource.collectors` là 1 khi đang thu thập, 0 sau khi hủy |
| Khóa đúng spec mục 8 và ADR-0023 | Unit test | `NetworkSettingsTest.lostDelay_isLong2Seconds`: tên `network_lost_delay_seconds`, `type == LONG`, `default == 2L`, mô tả không rỗng; `all_containsEveryDeclaredKey` |
| Khóa có trong bảng khóa | Unit test | `RemoteConfigKeysTest.networkKeys_areListed`: `AppRemoteKeys.byModule[":core:network"] == NetworkSettings.all()`. Test của T-007 (`keysFile_matchesCode`, `templateFile_matchesCode`, `everyKeyName_isUniqueAcrossModules`, `everyNumberKey_endsWithUnit`) qua sau khi sinh lại |
| Không có quyền mới với app | Kiểm tra trong phiên | Trước và sau khi sửa, chạy `./gradlew :app:processStagingDebugMainManifest` và so danh sách `uses-permission` của `app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml`: giống hệt (mục 4.1 đã thấy giống) |
| Quy tắc module (ADR-0001) | Bộ kiểm tra hiện có | `checkModuleDependencies` trong `scripts/check.sh` qua |
| Hành vi trên máy thật | Kiểm tra thủ công (dời sang T-017) | T-016 chưa có nơi thu thập, bản staging không đổi gì nhìn thấy được. Kiểm cùng T-017 |

**Ngoại lệ viết test trước:** khung module (`settings.gradle.kts`, `build.gradle.kts`, `consumer-rules.pro`, manifest, `robolectric.properties`, dòng phụ thuộc ở `app/build.gradle.kts`) commit trước test vì test cần nó để chạy. Hai file bảng khóa sinh từ code. Dòng trong `CLAUDE.md` là tài liệu.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, worktree tạm ngoài repo (`git worktree add --detach <scratchpad>/trial-t016 origin/main`, commit `80cc3e2`), không commit, đã xóa sau khi chạy. Code thử: `:core:network` chép từ app (PR #23) đổi gói sang `com.vglstudio.template`, với `ConnectivitySource` đòi `VALIDATED`, `lostAfterDelay`, `NetworkModule`, manifest có `ACCESS_NETWORK_STATE`, `robolectric.properties` (`sdk=35`); test tạm `LostAfterDelayTest` (3 test: chờ đủ, chập chờn, đọc lại thời gian chờ) và `AndroidConnectivitySourceTest` (2 test: capability chưa xác nhận, callback rồi hủy đăng ký). `NetworkSettings` dùng hằng chuỗi vì T-007 (`RemoteKey`) chưa merge trên nhánh chính; phần `RemoteKey` theo hợp đồng plan T-007, kiểm khi chạy task.
- **Lệnh:** `./gradlew :core:network:testDebugUnitTest` rồi `scripts/check.sh` (với `ANDROID_HOME=$HOME/Library/Android/sdk` và `JAVA_HOME` của Android Studio: JDK 26 mặc định của máy làm Gradle báo lỗi "26").
- **Kết quả:** `scripts/check.sh` qua: "Test JVM: 110 test, 0 lỗi ... ./core/network: 5 test, 0 lỗi", "Module dependency rule OK", lint, ktlint, build staging qua, "Cổng chất lượng: QUA" (1 phút 24 giây). Kết quả đo được: `ShadowNetworkCapabilities.newInstance()` và `shadowOf(connectivityManager).networkCallbacks` có trong Robolectric 4.15.1 với `sdk=35`; mạng mặc định của Robolectric không có `VALIDATED` nên giá trị đầu của `connectivity()` là `false`, sau `onCapabilitiesChanged` đã xác nhận và `onLost` nhận `true`, `false`; hủy thu thập thì danh sách callback rỗng. Test thời gian cần `@OptIn(ExperimentalCoroutinesApi::class)` (không có thì chỉ là cảnh báo, không lỗi). Danh sách `uses-permission` của manifest gộp `stagingDebug` có module mới giống hệt bản build của nhánh chính (10 quyền, có sẵn `ACCESS_NETWORK_STATE`). Không cần phương án dự phòng ở mục 3.4.
- **Thay đổi sau chạy thử (rà soát trước khi chạy):** thêm test `isConnected_readsActiveNetworkCapabilities`. Test này dùng `ShadowConnectivityManager.setNetworkCapabilities`, chưa gọi trong lần chạy thử (lần chạy thử chỉ dùng `ShadowNetworkCapabilities.newInstance` và `networkCallbacks` của cùng shadow); chưa chạy lại. Kết quả trên không đổi. Shadow này thiếu hay khác hành vi thì áp dụng điều kiện chuyển ở mục 3.4 như các test Robolectric khác của lớp.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- Mục 7 (module core dùng chung, cấu hình build): tạo `:core:network`, sửa `settings.gradle.kts`, `app/build.gradle.kts`.
- Mục 3 (quyền trong `AndroidManifest.xml`): dòng `ACCESS_NETWORK_STATE` trong manifest của module mới. Quyền không mới với app và nay có trong spec mục 6 (spec `changed`, chờ xác nhận lại); plan chỉ duyệt được sau khi spec được xác nhận lại.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới, sửa `:app` (`build.gradle.kts`, `AppRemoteKeys`), `settings.gradle.kts`, hai file docs sinh lại |
| B. Độ rõ của yêu cầu | 1 | Spec và ADR-0024 rõ; có code app đã chạy làm nền; AI tự quyết tách `lostAfterDelay` và bỏ `onAvailable` |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí của T-016 có unit test; hành vi mạng thật chỉ kiểm tay, dời sang T-017 |
| D. Ảnh hưởng chéo | 2 | Sửa file do T-007 tạo, bảng khóa mà T-010 đến T-014 cũng sinh lại; T-017 dựa vào API này |
| E. Độ mới của kỹ thuật | 2 | `ConnectivityManager` qua `callbackFlow`, `transformLatest`, shadow mạng của Robolectric lần đầu trong dự án (đã chạy thử, mục 4.1) |
| **Tổng** | **9** | **Mức: Cao (điểm chặn 7 và 3)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `settings.gradle.kts`, `app/build.gradle.kts`, `AppRemoteKeys.kt`, `RemoteConfigKeysTest.kt`, hai file bảng khóa, `CLAUDE.md`. Dùng nhưng không sửa: `RemoteSettings`, `RemoteKey` (`:core:config`).
- Phụ thuộc: **T-007** (bắt buộc): `RemoteKey`, `RemoteSettings.value(key)`, `AppRemoteKeys`, `RemoteConfigKeysTest`, hai file bảng khóa phải đã merge. T-008 không bắt buộc.
- Task khác có thể xung đột: T-010 đến T-014 sinh lại hai file bảng khóa (xung đột thì lấy bản của nhánh chính rồi chạy lại lệnh ghi lại, không gộp tay). T-017 thêm file vào `:core:network` và sửa `NetworkModule.kt`, `NetworkSettings.kt`, `core/network/build.gradle.kts`. T-018 không chạm file của T-016.
- Đề xuất thứ tự chạy: sau T-007, trước T-017. Độc lập với T-018 và T-009 đến T-015.

## 7. Điều kiện dừng riêng

- Danh sách `uses-permission` của manifest gộp `stagingDebug` thay đổi theo bất kỳ cách nào.
- Cần sửa `build-logic/`, `gradle/libs.versions.toml` hay `scripts/check.sh` để module mới build, chạy test hay được lint.
- `checkModuleDependencies` báo lỗi, hoặc cần cho feature phụ thuộc `:core:network`.
- T-007 chưa merge, hoặc API thật khác hợp đồng (`RemoteKey.long`, `RemoteSettings.value`, `AppRemoteKeys.byModule`).
- Phương án dự phòng ở mục 3.4 cũng không qua.

## 8. Câu hỏi cho người duyệt

1. **Đòi `VALIDATED` khác với app camera-pose.** App chỉ đòi `INTERNET` vì có máy xác nhận chậm hay không bao giờ xác nhận (plan T-011 của app, câu hỏi 1). Spec KB-1 tiêu chí 3 và ADR-0024 đòi Wi-Fi không ra Internet cũng là mất mạng, nên dự án mẫu đòi cả hai. Đề xuất: giữ theo spec; nếu sau này thấy popup hiện nhầm trên máy xác nhận chậm thì sửa ADR-0024 ở ticket sau.
   - Trả lời (anh Nhật, 2026-10-23): INTERNET và VALIDATED, theo spec.
2. **Spec network đang `changed`** (thêm mục 6 về quyền, 2026-10-23). Đề xuất: xác nhận lại spec trước hoặc cùng lúc duyệt plan này; plan không thêm quyền nào ngoài mục 6.
   - Trả lời (anh Nhật, 2026-10-23): đã xác nhận lại spec network cùng lúc duyệt plan.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 2:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua hai lần rà soát độc lập trước khi chạy, ghi ở RUN-20261023-01)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-23; câu hỏi ở mục 8 đã trả lời ngay tại đó.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
