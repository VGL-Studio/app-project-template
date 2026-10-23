---
ticket: T-009
title: Công tắc tổng, ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core)
risk_level: high             # low | medium | high
token_estimate: 20M          # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-21)
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-009: CÔNG TẮC TỔNG, ÂN HẠN PHIÊN ĐẦU, GIỚI HẠN MỖI PHIÊN, KHOẢNG CÁCH LƯU QUA PHIÊN

## 1. Tóm tắt

Thêm vào `:core:ads` một hàm quyết định thuần cho quảng cáo toàn màn hình (công tắc tổng, ân hạn phiên đầu, tối đa mỗi phiên, khoảng cách tối thiểu), một bộ đếm phiên thuần và một kho lưu hai thời điểm trên DataStore dùng chung của `:core:data`. `AdsController` gom ba phần này thành hai hàm nội bộ mà interstitial hôm nay và native toàn màn hình (T-013) cùng gọi; công tắc tổng `ads_enabled` được ghép vào `isEnabled(placement)`, điểm chung của `AdSlot`, việc tải sẵn interstitial và mọi composable native về sau. Kiểm chứng bằng unit test cho mọi nhánh với đồng hồ giả, và kiểm tra công tắc tổng trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-6 | Khi người dùng đóng app rồi mở lại ngay thì khoảng cách tối thiểu giữa hai quảng cáo toàn màn hình vẫn được tính từ lần hiện trước. | Thời điểm hiện quảng cáo toàn màn hình gần nhất lưu trên máy theo giờ thực (giờ của hệ thống, tính bằng mili giây). Lần quyết định sau, kể cả ở tiến trình mới, so với thời điểm này và `ads_interstitial_min_interval_seconds` (mặc định 45). Rào chắn hiện có trong bộ nhớ (`FullScreenAdGuard`, đồng hồ đơn điệu) giữ nguyên, làm lớp chặn thứ hai trong cùng tiến trình khi người dùng đổi giờ máy. |
| KB-6 | Khi người dùng mở app lần đầu thì không có quảng cáo toàn màn hình trong thời gian ân hạn (mặc định 180 giây). | Lần đầu app vào tiền cảnh mà chưa có giá trị thì ghi thời điểm mở app lần đầu; không ghi đè về sau. Quảng cáo toàn màn hình bị chặn khi chưa qua `ads_first_session_grace_seconds` kể từ thời điểm đó. Chưa đọc được giá trị (chưa ghi hay đọc lỗi) thì coi như còn trong ân hạn, nghiêng về phía an toàn. |
| KB-6 | Khi đã đủ số quảng cáo toàn màn hình tối đa của phiên (mặc định 4) thì không hiện thêm cho tới phiên sau. | Đếm trong bộ nhớ số quảng cáo toàn màn hình đã hiện trong phiên. "Phiên" do plan định nghĩa (câu hỏi 1): bắt đầu khi tiến trình khởi động, và bắt đầu lại khi app trở lại tiền cảnh sau ít nhất 30 phút ở nền (giống định nghĩa phiên của Firebase Analytics). |
| KB-7 | Khi người vận hành tắt `ads_enabled` trên Firebase thì ở lần cập nhật cấu hình tiếp theo app không còn tải hay hiện quảng cáo nào, ở mọi vị trí. | `isEnabled(placement)` trả `ads_enabled && ads_<vị trí>_enabled` dưới dạng `Flow`. `AdSlot` đang theo dõi flow này nên banner gỡ ngay (AdView bị hủy); interstitial không tải mới và không hiện. Composable native của T-010 đến T-013 dùng cùng hàm này. Việc khởi động SDK sau hộp thoại đồng ý giữ nguyên (câu hỏi 3). |
| KB-7 | Khi bật lại thì các vị trí đang bật hoạt động lại mà không cần cập nhật app. | Flow phát lại `true`: `AdSlot` tải lại banner; `preloadWhenEnabled` thấy flow đổi sang `true` nên tải sẵn interstitial. |

Thiết kế tham chiếu: Không có thiết kế riêng (task không có giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0003, ADR-0004, ADR-0013, ADR-0014, ADR-0019, ADR-0022 (quyết định 6 và 7), ADR-0023.

Giả định về T-007 (đã merge trước task này, giữ đúng tên):

- `RemoteKey<T>` và `RemoteSettings.value(key: RemoteKey<T>): Flow<T>` trong `:core:config`.
- `AdsSettings` có sẵn `ENABLED` (`ads_enabled`, mặc định true), `INTERSTITIAL_MIN_INTERVAL_SECONDS` (45), `FIRST_SESSION_GRACE_SECONDS` (180), `INTERSTITIAL_MAX_PER_SESSION_COUNT` (4) và `placementEnabled(placement)`.

## 3. Giải pháp

### 3.1. Cách làm

**Hàm quyết định thuần** (`FullScreenAdPolicy.kt`), không phụ thuộc Android, cùng kiểu với `FullScreenAdGuard`:

```kotlin
internal data class FullScreenAdLimits(
    val adsEnabled: Boolean,
    val minIntervalMillis: Long,
    val firstSessionGraceMillis: Long,
    val maxPerSession: Int,
)

/** Wall-clock times stored on the device; null when unknown. */
internal data class FullScreenAdHistory(val firstOpenAtMillis: Long?, val lastShownAtMillis: Long?)

internal sealed interface FullScreenAdVerdict {
    data object Allowed : FullScreenAdVerdict
    data class Blocked(val reason: BlockReason) : FullScreenAdVerdict
}

internal enum class BlockReason { MASTER_SWITCH_OFF, HISTORY_UNAVAILABLE, FIRST_SESSION_GRACE, SESSION_CAP_REACHED, TOO_SOON }

internal fun decideFullScreenAd(
    nowMillis: Long,
    history: FullScreenAdHistory?,   // null: storage could not be read
    shownThisSession: Int,
    limits: FullScreenAdLimits,
): FullScreenAdVerdict
```

Thứ tự kiểm tra, dừng ở điều kiện đầu tiên chặn:

1. `adsEnabled` tắt: `MASTER_SWITCH_OFF`.
2. `history == null` (đọc lỗi): `HISTORY_UNAVAILABLE`.
3. `firstOpenAtMillis == null`, hoặc `0 <= now - firstOpenAt < grace`: `FIRST_SESSION_GRACE`.
4. `shownThisSession >= maxPerSession`: `SESSION_CAP_REACHED`.
5. `0 <= now - lastShownAt < minInterval`: `TOO_SOON`.
6. Còn lại: `Allowed`.

Thời điểm lưu nằm sau `now` (người dùng lùi giờ máy) thì bỏ qua giá trị đó, để quảng cáo không bị chặn mãi; trong cùng tiến trình, `FullScreenAdGuard` vẫn chặn theo đồng hồ đơn điệu. Giá trị âm từ Remote Config được đưa về 0 khi dựng `FullScreenAdLimits` (`maxPerSession = 0` nghĩa là không quảng cáo toàn màn hình nào).

**Bộ đếm phiên thuần** (`FullScreenAdSession.kt`), đồng hồ đơn điệu (`SystemClock.elapsedRealtime`):

```kotlin
internal class FullScreenAdSession(private val timeoutMillis: Long = SESSION_TIMEOUT_MILLIS) {
    val shownCount: Int
    fun onBackground(now: Long)
    fun onForeground(now: Long)   // resets shownCount when the app was in background for >= timeoutMillis
    fun onShown()
    companion object { const val SESSION_TIMEOUT_MILLIS = 30 * 60 * 1_000L }
}
```

Hằng số trong code, không phải khóa Remote Config mới, vì spec mục 8 không có khóa này (câu hỏi 1).

**Kho lưu trên máy** (`FullScreenAdHistoryStore.kt`): interface và lớp cài đặt `internal` trên `DataStore<Preferences>` dùng chung của `:core:data`, theo mẫu `DataStoreCounterRepository` của `:feature:sample`.

- Hai khóa mới, tiền tố `ads_` để không trùng khóa của feature trong file `app_preferences`: `longPreferencesKey("ads_first_open_at_millis")`, `longPreferencesKey("ads_last_full_screen_shown_at_millis")`. Khóa mới, không có dữ liệu cũ cần chuyển đổi.
- `suspend fun read(): FullScreenAdHistory?`: đọc một lần (`data.first()`), trả `null` khi gặp `IOException`.
- `suspend fun recordFirstOpenIfAbsent(nowMillis: Long)`: ghi trong một lần `edit`, không ghi đè.
- `suspend fun recordShown(nowMillis: Long)`.
- Hai hàm ghi ném `IOException`; nơi gọi bắt lỗi.

**Bộ điều phối** (`FullScreenAdPacer.kt`): lớp thường, nhận `FullScreenAdHistoryStore`, `AdClock` (giờ thực và đồng hồ đơn điệu, có bản giả cho test) và `AppLogger`. Đây là API mà interstitial và native toàn màn hình (T-013) dùng chung:

- `suspend fun check(limits: FullScreenAdLimits): FullScreenAdVerdict`: đọc kho, gọi `decideFullScreenAd`; bị chặn thì ghi log `info("Ads", "Full-screen ad blocked: <REASON>")`.
- `suspend fun onShown()`: tăng bộ đếm phiên, ghi `lastShownAt`; lỗi ghi thì `warn` và không ném ra.
- `fun onForeground()` / `fun onBackground()`: chuyển cho `FullScreenAdSession`; `onForeground` lần đầu của tiến trình ghi thời điểm mở app lần đầu nếu chưa có (lỗi ghi thì `warn`).

Log chỉ có lý do và số đếm, không có thời điểm hay dữ liệu cá nhân (ADR-0019). Code mới dùng `AppLogger`; các lệnh `android.util.Log` sẵn có trong `AdsController` giữ nguyên, ngoài phạm vi task.

**Quy tắc đọc từ Remote Config** (`AdsRemoteRules.kt`), hàm thuần trên `RemoteSettings` để test được bằng bản giả:

- `internal fun placementActive(settings: RemoteSettings, placement: AdPlacement): Flow<Boolean>` = `combine(value(AdsSettings.ENABLED), value(AdsSettings.placementEnabled(placement))) { a, b -> a && b }.distinctUntilChanged()`.
- `internal fun fullScreenAdLimits(settings: RemoteSettings): Flow<FullScreenAdLimits>`: ghép bốn khóa, đổi giây sang mili giây, đưa giá trị âm về 0.

**Nối vào `AdsController`:**

- Constructor nhận thêm `FullScreenAdHistoryStore` và `AppLogger`; tạo `FullScreenAdPacer(store, SystemAdClock, logger)`.
- `isEnabled(placement)` trả `placementActive(settings, placement)`. `AdSlot`, `preloadWhenEnabled`, `preloadInterstitial` và `showInterstitial` đều đi qua hàm này nên công tắc tổng áp dụng ở mọi chỗ mà không sửa `AdSlot.kt`. KDoc của `isEnabled` ghi rõ: mọi định dạng, kể cả native về sau, phải kiểm tra qua hàm này.
- Hai hàm nội bộ dùng chung cho mọi quảng cáo toàn màn hình, T-013 gọi y như `showInterstitial`:
  - `internal suspend fun awaitFullScreenTurn(placement: AdPlacement): Boolean`: kiểm tra `canRequestAds`, `isEnabled`, `pacer.check(fullScreenAdLimits(settings).first())`, rồi vòng chờ của `FullScreenAdGuard` như hiện nay (khoảng cách trong bộ nhớ lấy từ cùng `limits.minIntervalMillis`).
  - `internal fun onFullScreenShown()`: `guard.onShown(elapsedRealtime)` và `scope.launch { pacer.onShown() }`.
- `showInterstitial` viết lại bằng hai hàm trên; một cờ `fullScreenInProgress` (luồng chính) chặn hai lần gọi chồng nhau trong lúc đang đọc kho hay đang chờ.
- `fun onAppForeground()` / `fun onAppBackground()` công khai, `MainActivity` gọi trong `onStart` / `onStop`. Xoay màn hình gây `onStop`/`onStart` cách nhau rất ngắn nên không đổi phiên. `AdsController` đếm số activity đang ở `onStart` (luồng chính): chỉ lần chuyển từ 0 lên 1 mới gọi `pacer.onForeground()`, chỉ lần chuyển từ 1 về 0 mới gọi `pacer.onBackground()`. Lý do (người rà soát thêm ngày 2026-10-23): T-014 thêm `AdGalleryActivity` thứ hai ở staging; khi chuyển giữa hai activity, `onStart` của activity mới chạy trước `onStop` của activity cũ, nên gọi thẳng sẽ làm bộ đếm phiên tưởng app đang ở nền. Không dùng `ProcessLifecycleOwner` vì `lifecycle-process` chưa có trong `gradle/libs.versions.toml` (file được bảo vệ).
- Đếm một lần hiện ngay trước `ad.show(activity)`, cùng chỗ với `guard.onShown` hiện nay. Quảng cáo báo lỗi khi hiện thì vẫn tính (nghiêng về ít quảng cáo hơn).

**Hilt:** module mới `core/ads/.../di/AdsDataModule.kt` gắn `FullScreenAdHistoryStore` với lớp cài đặt (`@Binds`). `DataStore<Preferences>` lấy từ `DataStoreModule` của `:core:data`, `AppLogger` từ `:core:logging`; `:app` đã phụ thuộc cả hai nên đồ thị Hilt đủ.

**Phụ thuộc module:** `core/ads/build.gradle.kts` thêm `implementation(project(":core:data"))` và `implementation(project(":core:logging"))`. Core phụ thuộc core, đúng ADR-0001; `checkModuleDependencies` trong `build.gradle.kts` gốc kiểm tra theo quy tắc chung, không có danh sách module cần sửa. Không đụng `gradle/libs.versions.toml`.

**Commit test trước (bước 3, L-025):** commit test chứa toàn bộ file test và bản giả mới (mục 3.2), cùng khung rỗng của các lớp chính mới để test biên dịch được: `decideFullScreenAd` trả `Allowed`, `FullScreenAdSession` không đếm, kho trả `FullScreenAdHistory(null, null)` và không ghi, `FullScreenAdPacer` trả `Allowed`, `placementActive` trả giá trị của vị trí (chưa ghép công tắc tổng), `fullScreenAdLimits` trả số 0. Test thất bại ở phần kiểm tra. Commit code ở bước 4 viết phần thật và nối vào `AdsController`, `MainActivity`.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPolicy.kt` | `FullScreenAdLimits`, `FullScreenAdHistory`, `FullScreenAdVerdict`, `BlockReason`, hàm thuần `decideFullScreenAd` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdSession.kt` | Bộ đếm phiên thuần, hằng số 30 phút |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdHistoryStore.kt` | Interface và `DataStoreFullScreenAdHistoryStore` (`internal`), hai khóa `ads_first_open_at_millis`, `ads_last_full_screen_shown_at_millis` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/FullScreenAdPacer.kt` | Bộ điều phối dùng chung cho mọi quảng cáo toàn màn hình; `AdClock` và `SystemAdClock` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsRemoteRules.kt` | `placementActive` (công tắc tổng ghép khóa của vị trí), `fullScreenAdLimits` |
| Tạo | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/di/AdsDataModule.kt` | Hilt module gắn `FullScreenAdHistoryStore` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Nhận kho và `AppLogger`; `isEnabled` qua `placementActive`; `awaitFullScreenTurn`, `onFullScreenShown`, `onAppForeground`, `onAppBackground`; `showInterstitial` dùng các hàm này; cờ chống gọi chồng |
| Sửa | `core/ads/build.gradle.kts` | Thêm `implementation(project(":core:data"))`, `implementation(project(":core:logging"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/template/MainActivity.kt` | `onStart` gọi `ads.onAppForeground()`, `onStop` gọi `ads.onAppBackground()` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Thêm test giữ mặc định `ENABLED` true, ân hạn 180, tối đa 4, nếu T-007 chưa có; không sửa test sẵn có |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPolicyTest.kt` | Mọi nhánh của `decideFullScreenAd` |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdSessionTest.kt` | Bộ đếm phiên |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdHistoryStoreTest.kt` | Kho trên DataStore trong bộ nhớ, đọc lỗi, lưu qua lần mở app |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdPacerTest.kt` | Ba tiêu chí KB-6 qua bộ điều phối với đồng hồ giả và kho giả |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsRemoteRulesTest.kt` | Hai tiêu chí KB-7, giới hạn lấy từ Remote Config |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeRemoteSettings.kt` | `RemoteSettings` giả theo hợp đồng T-007: giá trị đặt được theo tên khóa, mặc định của `RemoteKey` khi chưa đặt |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FakeAdTestDoubles.kt` | `FakeAdClock`, `InMemoryFullScreenAdHistoryStore` (đặt được lỗi đọc và lỗi ghi), `InMemoryDataStore`, `UnreadableDataStore` |
| Sửa | `docs/ops/ads-guide.md` | Mục 6: bỏ ghi chú "(đợt 2)" của ba dòng ân hạn, tối đa mỗi phiên, công tắc tổng; thêm một câu định nghĩa phiên |
| Sửa | `docs/features/ads/tickets/T-009-tan-suat-va-cong-tac-tong.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/ads/plans/T-009-tan-suat-va-cong-tac-tong.md` | Nhật ký thực hiện |
| Sửa | `docs/STATUS.md` | Sinh lại bằng `scripts/status.py` |

Không sửa: `AdSlot.kt` (đi qua `isEnabled`), `FullScreenAdGuard.kt` và `FullScreenAdGuardTest.kt` (giữ nguyên rào chắn trong bộ nhớ), `AdsSettings.kt` (khóa đã khai báo ở T-007), `core/data/` (dùng nguyên `DataStoreModule`), `docs/remote-config-keys.md` (không thêm khóa, không đổi mặc định).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có. Dùng các khóa Remote Config mà T-007 đã khai báo: `ads_enabled`, `ads_interstitial_min_interval_seconds`, `ads_first_session_grace_seconds`, `ads_interstitial_max_per_session_count`. Không đổi giá trị mặc định nào (ADR-0014).
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Đổi tần suất quảng cáo toàn màn hình theo KB-6, thêm công tắc tổng theo KB-7.
- Sự kiện phân tích: Không chạm.
- Dữ liệu trên máy: hai khóa DataStore mới theo spec mục 6; số đếm của phiên chỉ trong bộ nhớ. Không gửi ra khỏi máy. `allowBackup="false"` nên dữ liệu không được sao lưu sang máy khác.
- File R2: Không chạm.

## 4. Kiểm thử

Unit test chạy trên JVM, đồng hồ giả, không cần Android. Thời gian tính bằng mili giây; `limits` mặc định trong test là `FullScreenAdLimits(adsEnabled = true, minIntervalMillis = 45_000, firstSessionGraceMillis = 180_000, maxPerSession = 4)`; `first = 1_000_000`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-7: công tắc tổng thắng mọi điều kiện khác | Unit test | `FullScreenAdPolicyTest.masterSwitchOff_blocksEvenWhenEverythingElseAllows`: `adsEnabled = false`, lịch sử cũ, 0 lần trong phiên, ra `Blocked(MASTER_SWITCH_OFF)` |
| Đọc kho lỗi thì không hiện | Unit test | `FullScreenAdPolicyTest.historyUnavailable_blocks`: `history = null`, ra `Blocked(HISTORY_UNAVAILABLE)` |
| KB-6, tiêu chí 2 | Unit test | `FullScreenAdPolicyTest.firstOpenUnknown_blocksAsGrace`: `firstOpenAtMillis = null`, ra `Blocked(FIRST_SESSION_GRACE)` |
| KB-6, tiêu chí 2 | Unit test | `FullScreenAdPolicyTest.withinGrace_blocks`: now = first + 179_999, ra `Blocked(FIRST_SESSION_GRACE)` |
| KB-6, tiêu chí 2 | Unit test | `FullScreenAdPolicyTest.graceOver_allows`: now = first + 180_000, không có lần hiện trước, ra `Allowed` |
| Ân hạn bằng 0 | Unit test | `FullScreenAdPolicyTest.zeroGrace_allowsRightAfterFirstOpen`: grace 0, now = first, ra `Allowed` |
| Giờ máy bị lùi | Unit test | `FullScreenAdPolicyTest.firstOpenInFuture_isIgnored`: first = now + 60_000, ra `Allowed` |
| KB-6, tiêu chí 3 | Unit test | `FullScreenAdPolicyTest.sessionCapReached_blocks`: 4 lần trong phiên, ra `Blocked(SESSION_CAP_REACHED)`; `belowSessionCap_allows`: 3 lần, ra `Allowed` |
| Tối đa bằng 0 | Unit test | `FullScreenAdPolicyTest.zeroCap_blocksEveryFullScreenAd`: max 0, 0 lần, ra `Blocked(SESSION_CAP_REACHED)` |
| KB-6, tiêu chí 1 | Unit test | `FullScreenAdPolicyTest.lastShownWithinInterval_blocksTooSoon`: lastShown = now - 44_999, ra `Blocked(TOO_SOON)`; `lastShownAtInterval_allows`: now - 45_000, ra `Allowed` |
| Giờ máy bị lùi | Unit test | `FullScreenAdPolicyTest.lastShownInFuture_isIgnored`: lastShown = now + 10_000, ra `Allowed` |
| Thứ tự lý do | Unit test | `FullScreenAdPolicyTest.graceIsReportedBeforeCapAndInterval`: còn ân hạn, đủ 4 lần, vừa hiện, ra `FIRST_SESSION_GRACE` |
| Phiên mới bắt đầu từ 0 | Unit test | `FullScreenAdSessionTest.newSession_hasNoAdShown`: `shownCount == 0` |
| Đếm | Unit test | `FullScreenAdSessionTest.onShown_incrementsCount`: hai lần `onShown`, `shownCount == 2` |
| Nền ngắn giữ phiên | Unit test | `FullScreenAdSessionTest.shortBackground_keepsCount`: `onBackground(0)`, `onForeground(1_799_999)`, đếm giữ nguyên |
| Nền 30 phút là phiên mới | Unit test | `FullScreenAdSessionTest.backgroundOfTimeout_startsNewSession`: `onBackground(0)`, `onForeground(1_800_000)`, `shownCount == 0` |
| Vào tiền cảnh lần đầu không đổi đếm | Unit test | `FullScreenAdSessionTest.foregroundWithoutBackground_keepsCount` |
| Kho rỗng | Unit test | `FullScreenAdHistoryStoreTest.emptyStore_readsUnknownTimes`: `FullScreenAdHistory(null, null)` |
| Thời điểm mở đầu không bị ghi đè | Unit test | `FullScreenAdHistoryStoreTest.firstOpen_isWrittenOnce`: ghi 1_000 rồi 2_000, đọc ra 1_000 |
| KB-6, tiêu chí 1 (lưu qua lần mở app) | Unit test | `FullScreenAdHistoryStoreTest.lastShown_survivesNewStoreInstance`: kho A ghi 5_000; kho B mới trên cùng `InMemoryDataStore` đọc ra 5_000 |
| Đọc lỗi không làm dừng app | Unit test | `FullScreenAdHistoryStoreTest.unreadableStorage_readsNull`: `UnreadableDataStore`, `read()` trả `null`, không ném |
| Ghi lỗi ném `IOException` | Unit test | `FullScreenAdHistoryStoreTest.unwritableStorage_throwsIOException`: `assertTrue(error is IOException)` |
| KB-6, tiêu chí 1 qua bộ điều phối | Unit test | `FullScreenAdPacerTest.intervalCountsAcrossAppRestarts`: pacer A (qua ân hạn) `onShown` lúc T; pacer B mới (phiên mới) cùng kho, `check` lúc T + 10_000 ra `TOO_SOON`, lúc T + 45_000 ra `Allowed` |
| KB-6, tiêu chí 2 qua bộ điều phối | Unit test | `FullScreenAdPacerTest.firstForeground_startsGrace`: kho rỗng, `onForeground` lúc giờ thực T; `check` lúc T + 100_000 ra `FIRST_SESSION_GRACE`, lúc T + 180_000 ra `Allowed`; `onForeground` lần hai lúc T + 500_000 không đổi thời điểm đã lưu |
| KB-6, tiêu chí 3 qua bộ điều phối | Unit test | `FullScreenAdPacerTest.fifthAdInSession_isBlockedUntilNextSession`: 4 lần `onShown` cách nhau 60_000, lần `check` thứ 5 ra `SESSION_CAP_REACHED`; `onBackground`, đồng hồ đơn điệu tiến 30 phút, `onForeground`, `check` ra `Allowed` |
| Đọc lỗi thì không hiện, có log | Unit test | `FullScreenAdPacerTest.unreadableHistory_blocksAndLogs`: kho giả lỗi đọc, ra `HISTORY_UNAVAILABLE`, `RecordingAppLogger` có một dòng `INFO` thẻ `Ads` chứa `HISTORY_UNAVAILABLE` |
| Ghi lỗi không làm dừng app | Unit test | `FullScreenAdPacerTest.writeFailureOnShown_isLoggedAndStillCounted`: kho giả lỗi ghi, `onShown` không ném, `shownCount` tăng, có một dòng `WARN` thẻ `Ads` |
| Log không chứa thời điểm | Unit test | `FullScreenAdPacerTest.logsHoldNoTimestamps`: không dòng log nào chứa chuỗi số của giờ thực đã dùng |
| KB-7, tiêu chí 1 | Unit test, Turbine | `AdsRemoteRulesTest.masterSwitchOff_turnsEveryPlacementOff`: vị trí bật, `ads_enabled` từ true sang false, flow phát `true` rồi `false` |
| KB-7, tiêu chí 2 | Unit test, Turbine | `AdsRemoteRulesTest.masterSwitchBackOn_restoresEnabledPlacements`: tắt rồi bật lại `ads_enabled`, flow phát `false` rồi `true`; vị trí đang tắt vẫn `false` |
| Vị trí vẫn mặc định tắt (ADR-0014) | Unit test | `AdsRemoteRulesTest.placementOffByDefault_evenWithMasterSwitchOn`: chưa đặt giá trị nào, flow phát `false` |
| Giới hạn đúng mặc định | Unit test | `AdsRemoteRulesTest.limitsUseDefaults`: chưa đặt giá trị, ra `FullScreenAdLimits(true, 45_000, 180_000, 4)` |
| Giới hạn đổi theo Remote Config, giá trị âm về 0 | Unit test | `AdsRemoteRulesTest.limitsFollowRemoteValues`: đặt 60, 0, 2 ra `(true, 60_000, 0, 2)`; `negativeRemoteValues_areClampedToZero` |
| Mặc định không đổi (ADR-0014) | Unit test | `AdsSettingsTest.adsEnabledIsOnByDefault`, `firstSessionGraceIs180SecondsByDefault`, `maxPerSessionIs4ByDefault` (chỉ thêm nếu T-007 chưa có test tương đương) |
| Rào chắn cũ giữ nguyên | Unit test có sẵn | `FullScreenAdGuardTest` chạy qua, không sửa |
| KB-7 trên máy | Kiểm tra thủ công | Bản staging, giá trị cục bộ của T-008 (nếu đã có) hay Remote Config staging: bật `ads_sample_bottom_banner_enabled`, thấy banner; tắt `ads_enabled`, lần cập nhật cấu hình sau banner biến mất; bật lại thì banner trở lại. `adb logcat -v threadtime \| grep ' VGL\.Ads'` |
| KB-6 trên máy | Kiểm tra thủ công, sau T-014 | App mẫu chưa có vị trí toàn màn hình nên chưa thử được trên máy; kiểm lại ở màn hình xem thử (T-014): mở lần đầu không có interstitial trong 180 giây, sau đó tối đa 4 lần mỗi phiên, đóng mở lại app trong 45 giây không có interstitial |

`AdsController` và `MainActivity` không có unit test: lớp này gắn với SDK quảng cáo, hiện không có test, và phần nối chỉ gọi các hàm đã test ở trên. Người rà soát độc lập đọc kỹ phần nối (thứ tự kiểm tra, chỗ gọi `onFullScreenShown`, cờ chống gọi chồng).

**Ngoại lệ viết test trước:** phần nối trong `AdsController.kt`, `MainActivity.kt`, `AdsDataModule.kt`, `core/ads/build.gradle.kts` và mục 6 của `ads-guide.md`: không có unit test (lý do ở trên), kiểm qua build, test trên emulator có sẵn (app vẫn khởi động, Hilt đủ đồ thị) và kiểm tra thủ công. Người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** quảng cáo (đổi tần suất quảng cáo toàn màn hình, công tắc tổng; điểm 1), module core dùng chung `:core:ads` (điểm 7). Điểm 9 không chạm: hai khóa DataStore mới, không có dữ liệu cũ cần chuyển đổi. Mức Cao, cần duyệt chéo; người phụ trách sản phẩm biết trước vì đây là tần suất quảng cáo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:ads` (nhiều file, đổi constructor và hàm nội bộ của `AdsController`), `:app` (`MainActivity`), file build của `:core:ads` |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, đủ tiêu chí; AI tự định nghĩa "phiên" và cách xử lý khi đọc lỗi hay giờ máy bị lùi (câu hỏi 1, 2) |
| C. Khả năng kiểm chứng tự động | 2 | Mọi nhánh quyết định có unit test với đồng hồ giả; nhưng phần nối trong `AdsController` (SDK thật) không test tự động, và KB-6 chưa thử được trên máy cho tới khi có vị trí toàn màn hình (T-014) |
| D. Ảnh hưởng chéo | 3 | Đổi hành vi `isEnabled` mà `AdSlot` và việc tải sẵn đang dựa vào; T-010 đến T-013 dựa vào API mới; T-007 và T-008 cũng sửa `AdsController.kt` |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: hàm thuần như `FullScreenAdGuard`, kho DataStore như `DataStoreCounterRepository` |
| **Tổng** | 10 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdsController.kt` (constructor, `isEnabled`, `showInterstitial`), `core/ads/build.gradle.kts`, `MainActivity.kt`, file DataStore dùng chung `app_preferences` (thêm hai khóa tiền tố `ads_`). `tokens.json`, ảnh chuẩn screenshot và `docs/remote-config-keys.md` không bị ảnh hưởng: task không đổi giao diện, không thêm khóa Remote Config; test và preview không cấp `AdsController` nên ảnh không có quảng cáo (ADR-0013).
- `docs/STATUS.md`: `scripts/tests/run.sh` chỉ đọc `STATUS.md` của app mới và repo giả do test tạo trong thư mục tạm, không đọc file của repo. Không bị ảnh hưởng; `scripts/status.py --check` xác nhận trang khớp. `AdsController.kt`, `MainActivity.kt` và `core/ads/build.gradle.kts` không được script hay test nào đọc theo tên.
- Task khác trong sprint có thể xung đột: T-007 (sửa `AdsController.kt`, `AdsSettings.kt`, `AdsSettingsTest.kt`), T-008 (giá trị cục bộ cho staging, có thể chạm phần cung cấp `RemoteSettings`), T-010 (native small, thêm composable gọi `isEnabled`), T-015 (AppLovin, sửa `core/ads/build.gradle.kts`).
- T-014 (`AdGalleryActivity` ở staging) gọi `gatherConsent` và `onUserInteraction` nhưng plan T-014 chưa gọi `onAppForeground` / `onAppBackground`. Nếu người thử chỉ mở "Ad gallery" sau khi cài mới, thời điểm mở app lần đầu không được ghi, nên quảng cáo toàn màn hình bị chặn mãi vì ân hạn (câu hỏi 7). T-013 (`FullScreenAdHost` trong `MainActivity`) và T-017 (`onAppForeground` lần hai gọi `checkNow()`) dùng đúng tên hàm của plan này.
- Đề xuất thứ tự chạy: sau T-007 (bắt buộc) và sau T-008 nếu chạy cùng phiên, để kiểm tra thủ công KB-7 bằng giá trị cục bộ; trước T-010 đến T-013 để các composable native dùng ngay `isEnabled` có công tắc tổng. T-015 chạy trước hay sau đều được, nhưng không cùng lúc (cùng sửa `core/ads/build.gradle.kts`).

## 7. Điều kiện dừng riêng

- Hợp đồng T-007 khác giả định ở mục 2 (tên `RemoteKey`, `RemoteSettings.value`, tên khóa trong `AdsSettings` hay giá trị mặc định): dừng, không tự đổi tên hay tự khai báo khóa.
- Cần thêm thư viện (ví dụ `lifecycle-process`) hay sửa `gradle/libs.versions.toml`.
- Cần sửa `FullScreenAdGuard.kt` hay `FullScreenAdGuardTest.kt` để test qua.
- Hilt không cấp được `DataStore<Preferences>` hay `AppLogger` cho `:core:ads` mà không sửa `:core:data`, `:core:logging` hay `build-logic/`.
- Test trên emulator có sẵn thất bại vì app không khởi động sau khi đổi constructor của `AdsController`, sau 3 lần sửa.
- Phải đổi thứ tự khởi động SDK hay luồng hộp thoại đồng ý (điểm chặn 5, ngoài plan).

## 8. Câu hỏi cho người duyệt

1. **Định nghĩa phiên.** AI đề xuất: phiên bắt đầu khi tiến trình khởi động, và bắt đầu lại khi app trở lại tiền cảnh sau ít nhất 30 phút ở nền (giống Firebase Analytics, để số liệu quảng cáo và số liệu phiên khớp nhau). 30 phút là hằng số trong code. Phương án khác: chỉ theo tiến trình (đơn giản hơn, nhưng Android có thể giữ tiến trình nhiều giờ, nên một "phiên" có thể kéo dài cả ngày); hoặc thêm khóa Remote Config `ads_session_timeout_seconds` (phải thêm vào spec mục 8 trước).
2. **Khi không đọc được dữ liệu trên máy, hay thời điểm lưu nằm sau giờ hiện tại.** AI đề xuất: đọc lỗi thì không hiện quảng cáo toàn màn hình lần đó; thời điểm nằm sau giờ hiện tại (giờ máy bị lùi) thì bỏ qua giá trị đó, trong cùng tiến trình vẫn có rào chắn theo đồng hồ đơn điệu. Chưa có thời điểm mở đầu (ghi lỗi) thì coi như còn ân hạn.
3. **Công tắc tổng có chặn cả việc khởi động SDK quảng cáo không?** AI đề xuất: không. Tắt `ads_enabled` thì không tải và không hiện quảng cáo nào, nhưng SDK vẫn khởi động sau hộp thoại đồng ý như hiện nay; đổi thứ tự khởi động là điểm chặn 5 và làm việc bật lại phức tạp hơn. Interstitial đã tải sẵn trong bộ nhớ không bị xóa, chỉ không được hiện.
4. **Người dùng đang có app trước bản này.** Lần đầu mở bản mới, app chưa có thời điểm mở đầu nên ghi thời điểm đó và người dùng cũ có 180 giây ân hạn một lần. AI đề xuất chấp nhận. Phương án khác: dùng thời điểm cài app của hệ thống (`firstInstallTime`) cho người dùng cũ, thêm một nhánh và một test.
5. **Một lần hiện bị lỗi có tính vào giới hạn không?** AI đề xuất: có tính (đếm ngay trước khi gọi hiện, như rào chắn hiện nay), nghiêng về ít quảng cáo hơn.
6. **Ghi log bằng `AppLogger` trong `:core:ads`.** Code mới dùng `AppLogger` (ADR-0019), các lệnh `android.util.Log` cũ trong `AdsController` giữ nguyên để không mở rộng phạm vi. Có muốn chuyển nốt trong task này không (thêm vài dòng sửa trong cùng file)?
7. **Activity thứ hai của T-014.** Người rà soát (2026-10-23) đề xuất: sửa plan T-014 để `AdGalleryActivity` gọi `ads.onAppForeground()` trong `onStart` và `ads.onAppBackground()` trong `onStop`, như `MainActivity`; bộ đếm activity ở mục 3.1 làm việc chuyển qua lại giữa hai activity không đổi phiên. Phương án khác: T-009 ghi thời điểm mở app lần đầu ngay trong `gatherConsent` (mọi activity đều gọi), không phụ thuộc T-014; thêm một dòng và một nhánh không có unit test.
8. **Ân hạn phiên đầu trên staging** (plan T-008, câu hỏi 4, để T-009 quyết). Với 180 giây, người thử staging phải chờ 3 phút sau lần mở đầu mới thấy quảng cáo toàn màn hình. AI đề xuất giữ 180 trên staging, để staging thử đúng hành vi production; người thử có thể đặt `ads_first_session_grace_seconds` trên project Firebase staging khi cần. Nếu người duyệt muốn 0 trên staging thì thêm `app/src/staging/.../StagingRemoteValuesModule.kt` (của T-008) và test của nó vào danh sách file.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
