---
ticket: T-016
title: Theo dõi có Internet dùng được, dùng chung cho cả app
status: approved
version: 1
risk_score:
risk_level: high
token_estimate: 20M
blocking_points: [core, permission]
run_order: 6
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-23
---

# PLAN T-016: THEO DÕI CÓ INTERNET DÙNG ĐƯỢC, DÙNG CHUNG CHO CẢ APP

## 1. Tóm tắt

Tạo module mới `:core:network` với interface công khai `NetworkMonitor { val isOnline: Flow<Boolean>; suspend fun checkNow(): Boolean }`, cài bằng `ConnectivityManager.registerDefaultNetworkCallback`, coi là có mạng khi mạng mặc định có `NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED` (ADR-0024). Có mạng thì báo ngay; mất mạng chỉ báo sau `network_lost_delay_seconds` (khóa mới, kiểu long, mặc định 2, khai báo bằng `RemoteKey` của T-007). Phần quyết định nằm sau một nguồn kết nối nội bộ thay được bằng nguồn giả, nên mọi tiêu chí về thời gian được kiểm bằng unit test với thời gian ảo; chưa có giao diện (popup là T-017).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md` (phần bộ theo dõi; phần popup là T-017):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-1 | Khi Wi-Fi bật nhưng không vào được Internet thì app coi là không có mạng | Mạng mặc định có `INTERNET` mà thiếu `VALIDATED` (Wi-Fi có cổng đăng nhập, Wi-Fi không ra Internet) là không có mạng. Không xét loại kết nối (Wi-Fi, di động) |
| KB-1 | Khi màn hình loading tắt và không có mạng thì popup hiện ngay trên màn hình đầu tiên | Phần của T-016: giá trị đầu tiên của `isOnline` phát ngay, không chờ `network_lost_delay_seconds`, vì lúc mở app chưa có "mất mạng" nào để lọc chập chờn. Popup và màn hình đầu là T-017. Xem câu hỏi 3 |
| KB-2 | Khi đang dùng mà mất Internet thì sau một khoảng chờ ngắn (mặc định 2 giây) popup hiện | `isOnline` đang `true`, nguồn báo mất: chỉ phát `false` khi mất liên tục đủ `network_lost_delay_seconds` giây. Có mạng lại trong lúc chờ thì không phát gì. Thời gian chờ đọc lại ở mỗi lần mất, nên đổi trên Firebase có tác dụng từ lần mất sau |
| KB-2 | Khi bấm "Thử lại" mà vẫn không có mạng thì popup vẫn hiện | `checkNow()` đọc trạng thái hệ thống ngay lúc gọi, trả `false`; `isOnline` giữ (hoặc phát ngay) `false` |
| KB-2 | Khi có mạng trở lại thì popup tự đóng | Nguồn báo có mạng thì `isOnline` phát `true` ngay, không chờ. `checkNow()` thấy có mạng cũng phát `true` ngay |
| KB-3 | Khi vẫn có mạng nhưng máy chủ lỗi hay quá thời gian thì popup không hiện | Bộ theo dõi chỉ dựa vào trạng thái mạng của hệ thống, không dựa vào kết quả tải của feature: lỗi máy chủ không làm `isOnline` đổi. Không cần code riêng, ghi một test để giữ |

Thiết kế tham chiếu: Không có thiết kế riêng cho ticket này. Thiết kế popup (`features/network/design/`, ảnh `NetworkLostScreen_default_light`, `NetworkLostScreen_default_dark`) thuộc T-017; T-016 không có màn hình nên không có screenshot test.

ADR liên quan: ADR-0024 (định nghĩa có mạng, bộ theo dõi dùng chung ở module core), ADR-0023 (quy ước khóa, khai báo một chỗ, sinh bảng khóa), ADR-0003 (Remote Config cho tham số kinh doanh), ADR-0019 (log qua `AppLogger`), ADR-0001 (quy tắc phụ thuộc module). ADR-0013, ADR-0014, ADR-0020, ADR-0022: đã đọc, ticket không chạm quảng cáo, thiết kế hay định dạng quảng cáo.

## 3. Giải pháp

### 3.1. Cách làm

**Chọn module: tạo `:core:network`, không đặt vào `:core:data`.**

- `:core:data` hiện chỉ có DataStore (lưu trên máy). Bộ theo dõi mạng là một mối quan tâm khác, cần `:core:config` (đọc khóa) và `:core:logging`; đặt vào `:core:data` thì mọi nơi dùng DataStore kéo theo phụ thuộc này và tên module không còn nói đúng vai trò.
- Module riêng giống cách `:core:assets`, `:core:logging` đã làm (ADR-0015, ADR-0019): một vai trò, một module. Sau này các phần mạng khác (nếu có) có chỗ rõ ràng.
- Chi phí: thêm một dòng `include` trong `settings.gradle.kts`, một dòng phụ thuộc trong `app/build.gradle.kts`, một dòng trong bảng module của `CLAUDE.md`. Convention plugin có sẵn (`template.android.library`, `template.hilt`), không sửa `build-logic/` hay `gradle/libs.versions.toml`. `scripts/check.sh` tự lặp qua `core/*/`, `checkModuleDependencies` tự xét mọi module: không phải sửa.
- Phụ thuộc: `:core:network` dùng `implementation(project(":core:config"))` và `implementation(project(":core:logging"))`, chỉ core phụ thuộc core (ADR-0001). Feature không phụ thuộc `:core:network` (ADR-0024: feature không tự đọc trạng thái mạng); convention plugin của feature không đổi.

**Quyền `ACCESS_NETWORK_STATE`.** `registerDefaultNetworkCallback`, `getNetworkCapabilities` cần quyền này. Manifest gộp hiện tại của `stagingDebug` và `productionDebug` đã có `android.permission.ACCESS_NETWORK_STATE` (đến từ Google Mobile Ads và Firebase, ADR-0012), nên app không có quyền mới. Plan khai báo quyền này trong `core/network/src/main/AndroidManifest.xml` để module tự đủ: không phụ thuộc việc SDK quảng cáo vô tình mang quyền vào, và lint không báo `MissingPermission` khi xét riêng module. Đây vẫn là một dòng `uses-permission` mới trong một file manifest, nên plan ghi là chạm điểm chặn 3 để người duyệt quyết (câu hỏi 1). Không sửa `app/src/main/AndroidManifest.xml`. Kiểm chứng: danh sách `uses-permission` của manifest gộp `stagingDebug` trước và sau giống hệt (mục 4).

**Interface công khai (`NetworkMonitor.kt`).**

```kotlin
/**
 * Whether the device has usable Internet (ADR-0024): the default network has INTERNET and VALIDATED.
 * Only :app reads this; features never read the network state themselves.
 */
interface NetworkMonitor {
    /**
     * Emits the current state at once, then each change. Back online is reported at once; a loss is reported
     * only after `network_lost_delay_seconds` without usable Internet. The first value is never delayed.
     */
    val isOnline: Flow<Boolean>

    /** Reads the system state now, reports it through [isOnline] at once (no delay) and returns it. For "Try again". */
    suspend fun checkNow(): Boolean
}
```

**Nguồn kết nối nội bộ (`ConnectivitySource.kt`, `internal`).**

```kotlin
internal interface ConnectivitySource {
    /** Raw state, without delay: emits the current value at once, then every change. */
    val hasUsableInternet: Flow<Boolean>

    /** Raw state right now. */
    fun isUsableNow(): Boolean
}

internal fun isUsableInternet(hasInternet: Boolean, isValidated: Boolean): Boolean = hasInternet && isValidated

internal fun NetworkCapabilities?.hasUsableInternet(): Boolean =
    this != null && isUsableInternet(hasCapability(NET_CAPABILITY_INTERNET), hasCapability(NET_CAPABILITY_VALIDATED))
```

`AndroidConnectivitySource(connectivityManager)`:

- `hasUsableInternet` dựng bằng `callbackFlow`: gửi ngay `isUsableNow()`, rồi `registerDefaultNetworkCallback` (có từ API 24, minSdk là 26). `onCapabilitiesChanged(network, caps)` gửi `caps.hasUsableInternet()`; `onLost` gửi `false`. `awaitClose { unregisterNetworkCallback(callback) }`. Kết thúc bằng `distinctUntilChanged()` để hai báo cáo giống nhau liên tiếp không làm khởi động lại thời gian chờ.
- `isUsableNow()`: `connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork).hasUsableInternet()`.
- Không đọc hay ghi tên Wi-Fi, địa chỉ IP hay định danh mạng nào.

**Thời gian chờ trước khi báo mất mạng (`LossDelay.kt`, `internal`, hàm thuần trên `Flow`).**

```kotlin
internal data class ConnectivitySignal(val online: Boolean, val immediate: Boolean)

/** First value, back-online values and immediate signals pass at once; a loss passes after [delaySeconds]. */
internal fun Flow<ConnectivitySignal>.reportLossAfter(delaySeconds: suspend () -> Long): Flow<Boolean>
```

Cài bằng `transformLatest`: giá trị đầu tiên, `online == true` hay `immediate == true` thì `emit(online)` ngay; còn lại `delay(delaySeconds().coerceAtLeast(0) * 1_000)` rồi `emit(false)`. Tín hiệu mới đến trong lúc chờ hủy lần chờ cũ (bản chất của `transformLatest`), nên mạng chập chờn ngắn hơn thời gian chờ không bao giờ thành `false`. Giá trị âm coi như 0 (báo ngay); không đặt trần (câu hỏi 4).

**Cài đặt (`DefaultNetworkMonitor.kt`, `internal`, `@Singleton`).** Nhận `ConnectivitySource`, `RemoteSettings`, `AppLogger` và một `CoroutineScope` (qualifier `@NetworkScope` nội bộ; Hilt cấp `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, test truyền `backgroundScope`).

- `manualChecks = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)`.
- `isOnline = merge(source.hasUsableInternet.map { ConnectivitySignal(it, immediate = false) }, manualChecks.map { ConnectivitySignal(it, immediate = true) }).reportLossAfter { settings.value(NetworkSettings.LOST_DELAY_SECONDS).first() }.distinctUntilChanged().onEach { log(it) }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)`. Dùng chung một đăng ký callback cho mọi nơi thu thập; không ai thu thập thì sau 5 giây hủy đăng ký.
- `checkNow()`: `val now = source.isUsableNow()`, log, `manualChecks.emit(now)`, trả `now`. Không ai đang thu thập thì tín hiệu bị bỏ, không sao vì lần thu thập sau đọc lại trạng thái từ đầu.
- Log (ADR-0019), thẻ `Network`: `info("Network", "Online")`, `info("Network", "Offline")` khi `isOnline` đổi; `info("Network", "Check now: online|offline")` cho mỗi lần `checkNow()`; `debug` cho tín hiệu thô và thời gian chờ đang dùng. Không ghi tên mạng, loại mạng hay địa chỉ.

**Khóa Remote Config (`NetworkSettings.kt`, `public object`).** Theo mẫu `AdsSettings` của T-007:

```kotlin
object NetworkSettings {
    val LOST_DELAY_SECONDS: RemoteKey<Long> = RemoteKey.long(
        name = "network_lost_delay_seconds",
        default = 2,
        description = "Seconds without usable Internet before the app reports the connection as lost (ADR-0024).",
    )

    fun all(): List<RemoteKey<*>> = listOf(LOST_DELAY_SECONDS)
}
```

Tên đúng ADR-0023 (`<module>_<tham số>`, đơn vị `_seconds` ở cuối, qua test `everyNumberKey_endsWithUnit` của T-007). Public để `:app` gom vào `AppRemoteKeys`.

**Gom khóa ở `:app`.** `AppRemoteKeys.byModule` (T-007 tạo) thêm `":core:network" to NetworkSettings.all()` sau `":core:ads"`. Rồi chạy `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` để sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` (một dòng mới, một tham số mới `NUMBER` giá trị `"2"`). Không sửa tay hai file này.

**Nguồn giả cho test.**

- `core/network/src/test/.../FakeConnectivitySource.kt` (`internal`, chỉ cho test của module): `MutableStateFlow<Boolean>` làm `hasUsableInternet`, `var now: Boolean` cho `isUsableNow()`, đếm số lần có người thu thập (`subscriptionCount`).
- Không có bản giả công khai của `NetworkMonitor` trong `src/main`: test của T-017 cần những hành vi riêng (flow chưa phát gì, `checkNow` treo tới khi thả hay ném lỗi), nên plan T-017 tự viết bản giả trong `app/src/test` (câu hỏi 6).
- `core/network/src/test/.../FakeRemoteSettings.kt`: `RemoteSettings` giả trả `MutableStateFlow` theo tên khóa, chưa có thì trả `key.default`. Cài theo interface sau T-007 (`value(key)`); T-008 không đổi interface (plan T-008 mục 3).

**Hilt (`di/NetworkModule.kt`, `internal`).** `@Binds NetworkMonitor <- DefaultNetworkMonitor`, `@Binds ConnectivitySource <- AndroidConnectivitySource`, `@Provides ConnectivityManager` từ `@ApplicationContext` (`context.getSystemService(ConnectivityManager::class.java)`), `@Provides @Singleton @NetworkScope CoroutineScope`. `:app` phụ thuộc `:core:network` để Hilt gom module. Chưa ai tiêm `NetworkMonitor` cho tới T-017, nên hành vi app không đổi.

**Nơi khác đọc file plan đổi (L-020).** Đã `grep -rn` trên `scripts/`, `.github/`, `build-logic/`, `*/src/test`, `*/src/androidTest`:

- `settings.gradle.kts`, `app/build.gradle.kts`: `scripts/new-app.sh` thay đúng dòng `rootProject.name = "app-project-template"` của `settings.gradle.kts` và một chuỗi địa chỉ R2 trong `app/build.gradle.kts`; plan không đổi hai dòng đó. Không script nào khác đọc nội dung hai file theo dòng. `scripts/check.sh` lặp `core/*/` nên tự thấy module mới (chưa có `captureRoboImage` nên không có screenshot task). `scripts/release.sh` nhắc tới `app/build.gradle.kts`; plan chỉ thêm một dòng `implementation(project(":core:network"))`, không đổi biến thể, tham số `-P` hay task; không đọc hay sửa `release.sh`. `scripts/new-app.sh` dời mọi thư mục `*/src/*/kotlin/com/vglstudio/template` và thay tên gói trong mọi file (trừ ADR), nên module mới cũng được đổi gói mà không phải sửa script; test `new-app.sh replaces the package outside ADRs` của `scripts/tests/run.sh` kiểm điều này. Manifest của module không có thuộc tính `package` (namespace đặt trong `build.gradle.kts`), nên không có gì để đổi. `.github/workflows/pr-check.yml` lọc theo `core/**`, nên module mới tự kích hoạt CI. `build-logic/`: không sửa; `template.android.library` và `template.hilt` đủ (Robolectric khai báo trong `build.gradle.kts` của module vì chỉ convention Compose tự thêm).
- `AppRemoteKeys.kt`: đọc bởi `RemoteConfigKeysTest` (T-007), có trong danh sách file; T-008 đọc qua tên khóa, không bị ảnh hưởng vì không có khóa trùng tên.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: đọc bởi `RemoteConfigKeysTest` qua `app/build.gradle.kts` (system property của T-007); hai file sinh lại trong cùng PR.
- `CLAUDE.md`: chỉ thêm một dòng vào bảng module. Bốn nơi nhắc tới tên file này chỉ trích dẫn trong chữ, không đọc nội dung: `.github/ISSUE_TEMPLATE/from-app.yml` (nhãn lựa chọn "docs/, CLAUDE.md"), `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt` (chú thích "CLAUDE.md, step 10"), `scripts/device-session.sh` và `scripts/status.py` (in đường dẫn `docs/CLAUDE.md`, file khác). Không file nào bị ảnh hưởng.

**Thứ tự commit (L-025).**

1. Commit khung module: `settings.gradle.kts`, `core/network/build.gradle.kts`, `core/network/consumer-rules.pro`, `core/network/src/main/AndroidManifest.xml`, `app/build.gradle.kts`. Build qua (module rỗng).
2. Commit test (bước 3): mọi file trong `core/network/src/test/`, phần thêm vào `RemoteConfigKeysTest.kt`. Không biên dịch được vì `NetworkMonitor`, `NetworkSettings`, `reportLossAfter`... chưa có: đó là lý do thất bại đúng mong đợi, ghi vào nhật ký.
3. Commit code (bước 4): mọi file `core/network/src/main/kotlin/`, `AppRemoteKeys.kt`.
4. Commit sinh file: xác nhận `RemoteConfigKeysTest.keysFile_matchesCode` và `templateFile_matchesCode` thất bại vì thiếu khóa `network_lost_delay_seconds`, rồi chạy lệnh ghi lại, thêm hai file docs.
5. Commit tài liệu: `CLAUDE.md`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | Thêm `include(":core:network")` sau `:core:logging` |
| Tạo | `core/network/build.gradle.kts` | Plugin `template.android.library`, `template.hilt`; `namespace = "com.vglstudio.template.core.network"`; `implementation` `:core:config`, `:core:logging`; `testImplementation(libs.robolectric)`, `testImplementation(libs.androidx.test.core)` (đã có trong `libs.versions.toml`) |
| Tạo | `core/network/consumer-rules.pro` | Rỗng, như các module core khác (convention plugin trỏ tới file này) |
| Tạo | `core/network/src/main/AndroidManifest.xml` | `uses-permission android.permission.ACCESS_NETWORK_STATE` (quyết định của anh Nhật, câu hỏi 1), kèm chú thích: quyền đã có trong manifest gộp qua SDK, khai báo để module tự đủ. Đây là manifest đầu tiên trong một module core; không có thuộc tính `package` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkMonitor.kt` | Interface công khai `NetworkMonitor` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/ConnectivitySource.kt` | `ConnectivitySource`, `isUsableInternet`, `NetworkCapabilities?.hasUsableInternet()`, `AndroidConnectivitySource` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/LossDelay.kt` | `ConnectivitySignal`, `Flow<ConnectivitySignal>.reportLossAfter` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/DefaultNetworkMonitor.kt` | Cài đặt, chia sẻ một callback, `checkNow`, log thẻ `Network`, qualifier `NetworkScope` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/NetworkSettings.kt` | Khóa `network_lost_delay_seconds` (long, 2), `all()` |
| Tạo | `core/network/src/main/kotlin/com/vglstudio/template/core/network/di/NetworkModule.kt` | Hilt: hai `@Binds`, `ConnectivityManager`, scope |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/FakeConnectivitySource.kt` | Nguồn giả của module |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/FakeRemoteSettings.kt` | `RemoteSettings` giả theo tên khóa |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/LossDelayTest.kt` | Test hàm `reportLossAfter` với thời gian ảo |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/DefaultNetworkMonitorTest.kt` | Test bộ theo dõi với nguồn giả, khóa giả, `RecordingAppLogger` |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/AndroidConnectivitySourceTest.kt` | Test Robolectric: ánh xạ capability, đăng ký và hủy callback |
| Tạo | `core/network/src/test/kotlin/com/vglstudio/template/core/network/NetworkSettingsTest.kt` | Tên, kiểu, mặc định của khóa |
| Sửa | `app/build.gradle.kts` | Thêm `implementation(project(":core:network"))` sau `:core:logging` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/config/AppRemoteKeys.kt` | Thêm `":core:network" to NetworkSettings.all()` (file do T-007 tạo) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` | Thêm test `networkKeys_areListed` (file do T-007 tạo) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng lệnh ghi lại, không sửa tay: thêm dòng `network_lost_delay_seconds` |
| Sửa | `docs/remote-config-template.json` | Sinh lại bằng lệnh ghi lại, không sửa tay: thêm tham số `network_lost_delay_seconds` |
| Sửa | `CLAUDE.md` | Bảng "Cấu trúc module": thêm dòng `:core:network` (theo dõi có Internet dùng được, ADR-0024) |

Không có ảnh chuẩn screenshot nào được ghi: ticket không có màn hình. File được bảo vệ (`.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/`, `gradle/libs.versions.toml`, `scripts/release.sh`): không chạm. `app/src/main/AndroidManifest.xml`: không chạm.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có (spec mục 8: quy tắc áp dụng cho mọi app). Một khóa Remote Config mới `network_lost_delay_seconds` (long, mặc định 2), đúng spec mục 8.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm. Hai sự kiện `network_lost_shown`, `network_restored` thuộc T-017.
- File R2: Không chạm.
- Dữ liệu trên máy: không lưu gì.

## 4. Kiểm thử

Mọi test thời gian dùng `runTest` và thời gian ảo (`advanceTimeBy`, `currentTime`), Turbine để đọc `Flow`. Thời gian chờ mặc định 2 giây trừ khi ghi khác.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-1, tiêu chí 2 (phần T-016): giá trị đầu không chờ | Unit test | `LossDelayTest.firstValueOffline_isEmittedAtOnce`: luồng bắt đầu bằng `(false, immediate=false)`, nhận `false` ở `currentTime == 0`. `firstValueOnline_isEmittedAtOnce`: tương tự với `true` |
| KB-2, tiêu chí 1: chờ trước khi báo mất | Unit test | `LossDelayTest.lossLongerThanDelay_isReportedAfterDelay`: `true` lúc 0, `false` lúc 1 000 ms; ở 2 999 ms chưa có `false`, ở 3 000 ms nhận `false` |
| KB-2, tiêu chí 1: mạng chập chờn không báo | Unit test | `LossDelayTest.lossShorterThanDelay_isNotReported`: `true` lúc 0, `false` lúc 1 000, `true` lúc 2 500; chạy tới 10 000 ms, chỉ nhận một giá trị `true` (sau `distinctUntilChanged` ở `DefaultNetworkMonitor`; ở mức hàm thuần nhận `true`, `true`) |
| KB-2, tiêu chí 4: có mạng lại báo ngay | Unit test | `LossDelayTest.backOnline_isReportedAtOnce`: sau khi đã báo `false`, `true` lúc 5 000 ms được nhận ở đúng 5 000 ms |
| KB-2, tiêu chí 3 và 4: "Thử lại" không chờ | Unit test | `LossDelayTest.immediateSignal_skipsDelay`: `true` lúc 0, `(false, immediate=true)` lúc 1 000, nhận `false` ở 1 000 |
| Thời gian chờ đọc lại mỗi lần mất | Unit test | `LossDelayTest.delayIsReadAtEachLoss`: hàm chờ trả 2 rồi 5; lần mất thứ nhất báo sau 2 000 ms, lần thứ hai sau 5 000 ms |
| Giá trị chờ không hợp lệ | Unit test | `LossDelayTest.zeroOrNegativeDelay_reportsAtOnce`: hàm chờ trả `0`, rồi `-3`; cả hai lần `false` được báo ngay |
| KB-2, tiêu chí 1: mặc định 2 giây khi chưa có giá trị từ Firebase | Unit test | `DefaultNetworkMonitorTest.noRemoteValue_reportsLossAfter2Seconds`: `FakeRemoteSettings` không có giá trị, nguồn `true` rồi `false` lúc 0; `false` ở 2 000 ms, không trước |
| Khóa từ Firebase được dùng | Unit test | `DefaultNetworkMonitorTest.remoteDelay_isUsed`: `network_lost_delay_seconds = 5`; `false` ở 5 000 ms |
| KB-1, tiêu chí 3: Wi-Fi bật nhưng chưa xác nhận Internet | Unit test (Robolectric) | `AndroidConnectivitySourceTest.wifiWithInternetNotValidated_isNotUsable`: `NetworkCapabilities` tạo bằng `ShadowNetworkCapabilities.newInstance()`, thêm `TRANSPORT_WIFI` và `NET_CAPABILITY_INTERNET`, không có `VALIDATED`: `hasUsableInternet()` là `false`. `internetAndValidated_isUsable`: thêm cả hai: `true`. `noCapabilities_isNotUsable`: `null` là `false`. `ConnectivitySourceTest` thuần trên JVM: `isUsableInternet(true, false) == false`, `isUsableInternet(false, true) == false`, `isUsableInternet(true, true) == true` (viết trong cùng file) |
| Callback hệ thống được nối đúng | Unit test (Robolectric) | `AndroidConnectivitySourceTest.callback_reportsCapabilitiesAndLoss`: thu thập `hasUsableInternet`, lấy callback từ `shadowOf(connectivityManager).networkCallbacks`, gọi `onCapabilitiesChanged` với capability đã xác nhận rồi `onLost`: nhận `true`, `false`. `cancel_unregistersCallback`: hủy thu thập thì `networkCallbacks` rỗng |
| KB-1, tiêu chí 3 qua bộ theo dõi | Unit test | `DefaultNetworkMonitorTest.sourceNotUsable_isOffline`: nguồn giả bắt đầu `false` (Wi-Fi bật mà chưa xác nhận): `isOnline` phát `false` ngay ở 0 ms |
| KB-2, tiêu chí 3: thử lại vẫn mất mạng | Unit test | `DefaultNetworkMonitorTest.checkNow_stillOffline_returnsFalseAndStaysOffline`: đã báo `false`, `now = false`; `checkNow()` trả `false`, không có giá trị mới |
| KB-2, tiêu chí 4: thử lại có mạng | Unit test | `DefaultNetworkMonitorTest.checkNow_online_reportsOnlineAtOnce`: đã báo `false`, `now = true` (nguồn chưa báo); `checkNow()` trả `true`, `isOnline` phát `true` ở cùng `currentTime` |
| Thử lại trong lúc đang chờ báo mất | Unit test | `DefaultNetworkMonitorTest.checkNow_duringPendingLoss_reportsOfflineAtOnce`: `true`, nguồn `false` lúc 0, `now = false`, `checkNow()` lúc 500 ms: nhận `false` ở 500 ms |
| KB-3: lỗi máy chủ không làm đổi trạng thái | Unit test | `DefaultNetworkMonitorTest.onlineSource_staysOnline`: nguồn giữ `true` 60 000 ms ảo, chỉ có một giá trị `true` (bộ theo dõi không có đầu vào nào khác ngoài nguồn hệ thống) |
| Dùng chung một callback | Unit test | `DefaultNetworkMonitorTest.twoCollectors_shareOneSubscription`: hai nơi thu thập, `FakeConnectivitySource.subscriptionCount == 1`; cả hai nhận cùng giá trị |
| Log đúng ADR-0019 | Unit test | `DefaultNetworkMonitorTest.logsStateChangesAndChecks`: `RecordingAppLogger` có `INFO` thẻ `Network` "Offline", "Online", "Check now: online"; không entry nào chứa chữ khác ngoài các mẫu đã nêu (không tên mạng) |
| Khóa đúng spec mục 8 và ADR-0023 | Unit test | `NetworkSettingsTest.lostDelay_isLong2Seconds`: tên `network_lost_delay_seconds`, `type == LONG`, `default == 2L`, mô tả không rỗng; `all_containsEveryDeclaredKey`: mọi field kiểu `RemoteKey` của `NetworkSettings::class.java.declaredFields` có trong `all()` |
| Khóa có trong bảng khóa | Unit test | `RemoteConfigKeysTest.networkKeys_areListed`: `AppRemoteKeys.byModule[":core:network"] == NetworkSettings.all()`. Các test của T-007 (`keysFile_matchesCode`, `templateFile_matchesCode`, `everyKeyName_isUniqueAcrossModules`, `everyNumberKey_endsWithUnit`) qua sau khi sinh lại hai file |
| Không có quyền mới với app | Kiểm tra trong phiên | Trước khi sửa, chạy `./gradlew :app:processStagingDebugManifest` và lưu danh sách `uses-permission` của `app/build/intermediates/merged_manifests/stagingDebug/processStagingDebugManifest/AndroidManifest.xml`; sau khi xong chạy lại và so: hai danh sách giống hệt. Ghi kết quả vào nhật ký và mô tả PR |
| Quy tắc module (ADR-0001) | Bộ kiểm tra hiện có | `checkModuleDependencies` trong `scripts/check.sh` qua |
| Hành vi trên máy thật | Kiểm tra thủ công (dời sang T-017) | T-016 chưa có nơi nào thu thập `isOnline`, nên bản staging không đổi gì nhìn thấy được. Khi T-017 xong: bật Wi-Fi không ra Internet, tắt mạng, bật lại, xem `adb logcat -v threadtime \| grep ' VGL\.Network'` có "Offline" sau khoảng 2 giây và "Online" ngay khi có mạng |

**Ngoại lệ viết test trước:** khung module (`settings.gradle.kts`, `build.gradle.kts`, `consumer-rules.pro`, manifest, dòng phụ thuộc ở `app/build.gradle.kts`) là cấu hình cần có để test chạy được, commit trước test. `docs/remote-config-keys.md`, `docs/remote-config-template.json` sinh từ code; test so file của T-007 đã có và thất bại vì thiếu khóa trước khi sinh lại. Dòng trong `CLAUDE.md` là tài liệu, không có test.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- Mục 7 (module core dùng chung, cấu hình build): tạo `:core:network`, sửa `settings.gradle.kts`, `app/build.gradle.kts`.
- Mục 3 (quyền trong `AndroidManifest.xml`): thêm dòng `ACCESS_NETWORK_STATE` vào manifest của module mới. Quyền không mới với app (manifest gộp đã có), nhưng spec không nêu quyền này và `CLAUDE.md` cấm thêm quyền ngoài spec, nên plan ghi là chạm để người duyệt quyết (câu hỏi 1).
- Không chạm mục 1 (quảng cáo), mục 4 (sự kiện là việc của T-017), mục 9 (không lưu gì trên máy).

Điểm 5 tiêu chí, chấm để người duyệt thấy mức độ dù plan đã xếp cao vì điểm chặn:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Module mới với interface công khai, sửa `:app` (`build.gradle.kts`, `AppRemoteKeys`), `settings.gradle.kts` và hai file docs sinh lại |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, ADR-0024 nêu rõ định nghĩa có mạng; AI tự quyết chọn module, giá trị đầu không chờ, cách `checkNow` bỏ qua thời gian chờ, không đặt trần thời gian chờ (câu hỏi 2 đến 4) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí của T-016 có unit test với thời gian ảo; hành vi với mạng thật trên máy chỉ kiểm được bằng tay, và dời sang T-017 vì chưa có nơi dùng |
| D. Ảnh hưởng chéo | 2 | Sửa `app/build.gradle.kts`, `AppRemoteKeys.kt`, `RemoteConfigKeysTest.kt` và hai file bảng khóa mà T-007 tạo và các ticket native (T-010 đến T-013) cũng sinh lại; T-017 dựa vào API này |
| E. Độ mới của kỹ thuật | 2 | `ConnectivityManager` với `callbackFlow`, `transformLatest` và shadow mạng của Robolectric chưa có trong dự án, nhưng là cách làm phổ biến |
| **Tổng** | **9** | **Mức: Cao (điểm chặn 7 và 3; tổng điểm cũng đạt mức cao)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `settings.gradle.kts`, `app/build.gradle.kts`, `AppRemoteKeys.kt`, `RemoteConfigKeysTest.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `CLAUDE.md`. Dùng nhưng không sửa: `RemoteSettings`, `RemoteKey` (`:core:config`), `AppLogger`, `RecordingAppLogger` (`:core:logging`).
- Phụ thuộc plan khác: **T-007** (bắt buộc, `depends_on`): `RemoteKey`, `RemoteSettings.value(key)`, `AppRemoteKeys`, `RemoteConfigKeysTest` và hai file bảng khóa phải đã merge. **T-008**: không bắt buộc; nếu đã merge thì `RemoteSettings` có thêm giá trị cục bộ của staging nhưng interface không đổi, khóa mạng không cần giá trị cục bộ (mặc định 2 dùng được trên staging).
- Task khác trong sprint có thể xung đột: T-010 đến T-013 thêm vị trí vào `AdPlacement` và sinh lại hai file bảng khóa; T-007, T-015 sửa `app/build.gradle.kts` hoặc file build khác. Xung đột ở hai file sinh ra thì không gộp tay: lấy bản của nhánh chính rồi chạy lại lệnh ghi lại. T-017 dùng `NetworkMonitor` (bản giả của T-017 nằm trong `app/src/test`) và sửa `app/build.gradle.kts`.
- Đề xuất thứ tự chạy: sau T-007, trước T-017. Không phụ thuộc T-009 đến T-015; có thể chạy song song với chúng nếu xử lý bảng khóa như trên.

## 7. Điều kiện dừng riêng

- Robolectric không có shadow cần thiết (`ShadowNetworkCapabilities.newInstance`, `ShadowConnectivityManager.getNetworkCallbacks`) ở phiên bản trong `libs.versions.toml`: dừng, không nâng phiên bản và không bỏ test; báo người chọn giữa kiểm ánh xạ chỉ bằng hàm thuần `isUsableInternet` hay hướng khác.
- Danh sách `uses-permission` của manifest gộp `stagingDebug` thay đổi theo bất kỳ cách nào.
- Cần sửa `build-logic/`, `gradle/libs.versions.toml` hay `scripts/check.sh` để module mới build, chạy test hay được lint (ví dụ thư viện test chưa khai báo trong `libs.versions.toml`).
- `checkModuleDependencies` báo lỗi, hoặc cần cho feature phụ thuộc `:core:network`.
- T-007 chưa merge, hoặc API thật khác hợp đồng (`RemoteKey.long`, `RemoteSettings.value`, `AppRemoteKeys.byModule`).

## 8. Câu hỏi cho người duyệt

1. **Khai báo `ACCESS_NETWORK_STATE` trong manifest của `:core:network`?** Quyền đã có trong manifest gộp (Google Mobile Ads, Firebase), app không có quyền mới và khai báo Data safety không đổi. Đề xuất: khai báo, để module không phụ thuộc vào việc SDK khác vô tình mang quyền vào; plan kiểm danh sách quyền gộp không đổi. Phương án khác: không khai báo, dựa vào SDK (nếu sau này một app bỏ SDK quảng cáo thì bộ theo dõi ném `SecurityException`). Spec không nêu quyền này; nếu duyệt, đề nghị ghi một dòng vào spec mục 2 hay ADR-0024 ở PR tài liệu, ngoài T-016.
   - Trả lời (anh Nhật, 2026-10-23): có khai báo `ACCESS_NETWORK_STATE` trong manifest của `:core:network`.
2. **Module mới `:core:network` thay vì `:core:data`.** Đề xuất: module mới (lý do ở mục 3.1). Chi phí là một dòng ở `settings.gradle.kts`, `app/build.gradle.kts`, `CLAUDE.md`.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
3. **Giá trị đầu tiên không chờ.** Mở app khi đã mất mạng thì `isOnline` phát `false` ngay (spec KB-1: popup "hiện ngay"). Mặt trái: ngay sau khi máy vừa bật hay vừa đổi mạng, hệ thống có thể chưa xác nhận xong Internet, popup có thể hiện chốc lát rồi tự đóng. Đề xuất: giữ như vậy; nếu muốn, áp cùng thời gian chờ cho giá trị đầu (popup đầu tiên trễ 2 giây).
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
4. **Giới hạn của `network_lost_delay_seconds`.** Đề xuất: giá trị âm coi là 0, không đặt trần; đặt rất lớn trên Firebase thì thực tế tắt popup, đó là quyết định của người vận hành. Có muốn đặt trần (ví dụ 30 giây) không?
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **"Thử lại" chỉ đọc lại trạng thái hệ thống**, không tự gửi yêu cầu thử ra Internet và không gọi `reportNetworkConnectivity` để hệ thống xác nhận lại. Đề xuất: giữ đơn giản như vậy; hệ thống tự xác nhận lại khi mạng đổi, và nguồn báo có mạng thì popup tự đóng. Nếu thấy trên máy thật "Thử lại" ít tác dụng thì làm ticket sau.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
6. **Không có `FakeNetworkMonitor` công khai.** Bản đầu của plan đặt một bản giả công khai trong `src/main` cho T-017. Rà soát độc lập (2026-10-23) bỏ đi: plan T-017 cần bản giả có `checkNow` treo hay ném lỗi và flow chưa phát gì, nên đã tự viết bản giả trong `app/src/test`; giữ cả hai là hai bản giả trùng tên, một bản không ai dùng trong `src/main`. Nếu muốn một bản giả dùng chung cho các app sau, thêm lại ở ticket sau với đủ các hành vi đó.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện một dev đảm nhận cả hai vai)
- **Ghi chú:** anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), sau rà soát độc lập của AI; AI ghi mục này theo lời anh. Câu hỏi ở mục 8 đã có trả lời. Cần PR #81 (`release.sh`, `check.sh`) merge trước phiên chạy.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
