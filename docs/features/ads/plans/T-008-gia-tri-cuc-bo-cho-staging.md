---
ticket: T-008
title: Giá trị Remote Config cục bộ cho bản staging
status: approved            # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 10M          # cỡ S, số gợi ý của scripts/token-usage.py estimate (chưa đủ 5 task đã đo)
blocking_points: [core, ads]
run_order: 2                # AI điền sau họp đánh giá rủi ro
reviewer: Nhật              # người duyệt
cross_reviewer: Nhật        # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-008: GIÁ TRỊ REMOTE CONFIG CỤC BỘ CHO BẢN STAGING

## 1. Tóm tắt

`RemoteSettings` đọc thêm một bộ giá trị khai báo trong `app/src/staging`, qua một qualifier mới `@LocalRemoteValues`, theo đúng cách `@LocallyEnabledFlags` đang làm với cờ. Thứ tự: giá trị từ Firebase, rồi giá trị cục bộ, rồi mặc định của khóa (ADR-0023, mục 5). Bản staging bật mọi vị trí của `AdPlacement`; production không khai báo gì nên nhận tập rỗng từ `@Multibinds` của `ConfigModule`. Kiểm chứng bằng unit test thứ tự ưu tiên ở `:core:config`, unit test bộ giá trị staging ở `:app`, và một test quét mã nguồn chứng minh chỉ source set `staging` khai báo giá trị cục bộ.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-8 | Khi mở bản staging thì mọi loại quảng cáo hiện được bằng mã thử của Google, không cần project Firebase thật. | Phần của T-008: trên staging, khóa `ads_<vị trí>_enabled` của mọi vị trí có giá trị `true` khi Firebase không trả giá trị. Mã thử đã có (`StagingAdsModule`, `GoogleTestAdUnitIds`). Các định dạng native chưa có; chúng tự được bật khi T-010 đến T-013 thêm vị trí vào `AdPlacement`, vì module staging lặp qua `AdPlacement.entries`. |
| KB-8 | Khi mở màn hình xem thử quảng cáo (chỉ có ở staging) thì ... | Không thuộc ticket này (T-014). |
| KB-8 | Bản production không có màn hình xem thử và không có giá trị cục bộ. | Phần "không có giá trị cục bộ": chỉ `app/src/staging` khai báo phần tử cho `@LocalRemoteValues`; production nhận tập rỗng và đọc giống hệt trước đây (Firebase, rồi mặc định). Phần màn hình xem thử thuộc T-014. |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0003 (cờ, `StagingFeatureFlagsModule`), ADR-0007 (tách staging, production), ADR-0012 (file Firebase giả, staging thật trong `staging.yml`), ADR-0013 (staging chỉ dùng mã thử), ADR-0014 (vị trí mặc định tắt), ADR-0023 (mục 5: giá trị cục bộ cho staging).

**Giả định về T-007** (phụ thuộc, theo plan `T-007-khoa-remote-config-co-kieu.md`). Plan này dựa trên hợp đồng sau, giữ đúng tên:

- `:core:config` có `RemoteKey<T>`, tạo bằng `RemoteKey.boolean/long/double/string(name, default, description)`, có thuộc tính `name` và `default`.
- `RemoteSettings.value(key: RemoteKey<T>): Flow<T>`, cài đặt trong `DefaultRemoteSettings` (file `RemoteSettings.kt`), dựa trên `RemoteFlagSource` trả `null` khi Firebase không có giá trị.
- `AdsSettings` (`:core:ads`) là `public object` (T-007 chuyển từ `internal`), khai báo khóa bằng `RemoteKey`, có `placementEnabled(placement): RemoteKey<Boolean>` và `ENABLED` (`ads_enabled`, mặc định bật).
- `AppRemoteKeys.byModule` ở `app/src/main` liệt kê mọi khóa; `RemoteConfigKeysTest` ở `app/src/test` sinh `docs/remote-config-keys.md` và đảm bảo không có hai khóa trùng tên.

T-007 đã merge mà khác hợp đồng này ở điểm nào ảnh hưởng tới danh sách file thì dừng (mục 7).

## 3. Giải pháp

### 3.1. Cách làm

**Thứ tự ưu tiên: Firebase, rồi cục bộ, rồi mặc định.** Làm đúng ADR-0023, mục 5. AI đã cân nhắc để cục bộ thắng Firebase ở staging và không đề xuất, lý do:

- Bản staging phát cho nhóm qua `staging.yml` dùng project Firebase staging thật (ADR-0012). Firebase thắng thì người vận hành tập trước trên project staging được mọi thao tác sẽ làm ở production: tắt một vị trí, đổi tần suất, xem app nhận giá trị mới. Cục bộ thắng thì trên staging không tắt được vị trí nào từ xa.
- Với bản build từ file giả (CI, dev chưa có file thật), Firebase không trả giá trị nào, nên hai thứ tự cho cùng kết quả.
- Rủi ro của thứ tự này: ai đó nhập file mẫu của T-007 (mọi vị trí `false`) vào project staging thì staging mất quảng cáo. Cách tránh ghi vào `docs/ops/remote-config.md`: project staging chỉ đặt khóa muốn đổi, không nhập cả file mẫu. Xem câu hỏi 1.

Điểm lệch cần biết: cờ tính năng hiện theo thứ tự ngược lại (bật cục bộ thì luôn bật, test `locallyEnabledFlag_staysOnWhenRemoteSaysOff`), và `docs/ops/remote-config.md` mục 4 đang ghi "bản build > Firebase > mặc định". Plan không đổi hành vi cờ (ngoài phạm vi); chỉ sửa câu trong hướng dẫn cho đúng cả hai trường hợp.

**Kiểu dữ liệu, trong `:core:config`** (file mới `LocalRemoteValues.kt`):

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalRemoteValues

/** A value one build gives a remote key when Firebase has none (ADR-0023). */
class LocalRemoteValue private constructor(val key: RemoteKey<*>, val value: Any) {
    companion object {
        fun <T : Any> of(key: RemoteKey<T>, value: T): LocalRemoteValue = LocalRemoteValue(key, value)
    }
}
```

`LocalRemoteValue` là lớp `final`, không có tham số kiểu, nên Dagger thấy đúng một khóa `Set<LocalRemoteValue>` ở cả nơi khai báo và nơi đóng góp. Với lớp có tham số kiểu (`LocalRemoteValue<*>`), Kotlin sinh ký tự đại diện trong Java và phần đóng góp của staging có thể rơi vào một tập khác mà build không báo lỗi; staging sẽ lặng lẽ không có giá trị nào. Hàm `of` giữ kiểu an toàn: không tạo được giá trị `String` cho khóa boolean.

**`DefaultRemoteSettings`** nhận thêm `@LocalRemoteValues localValues: Set<@JvmSuppressWildcards LocalRemoteValue>`:

- Lúc tạo: dựng bảng `name -> value`; `require` không có hai phần tử trùng `key.name` (lỗi ngay khi khởi tạo, thông báo nêu tên khóa).
- `value(key)`: luồng của Firebase, mỗi phần tử `null` thì lấy giá trị cục bộ theo `key.name`, vẫn không có thì lấy `key.default`. Giá trị từ Firebase tới sau vẫn tới người theo dõi; Firebase bỏ giá trị thì quay về giá trị cục bộ.
- Interface `RemoteSettings` không đổi, nên `AdsController` và mọi `RemoteSettings` giả trong test của module khác không bị ảnh hưởng.

**`ConfigModule`**: thêm `@Multibinds @LocalRemoteValues abstract fun localRemoteValues(): Set<LocalRemoteValue>`, như `locallyEnabledFlags()`. Production không có module nào đóng góp nên nhận tập rỗng.

**Staging** (file mới `StagingRemoteValuesModule.kt`, cạnh `StagingFeatureFlagsModule`):

```kotlin
@Provides
@ElementsIntoSet
@LocalRemoteValues
fun everyAdPlacementEnabled(): Set<LocalRemoteValue> =
    AdPlacement.entries.map { LocalRemoteValue.of(AdsSettings.placementEnabled(it), true) }.toSet()
```

Không đặt `ads_enabled` vì mặc định đã bật. Vị trí mới thêm vào `AdPlacement` tự bật trên staging, không phải sửa module này.

**Truy cập từ `:app`**: T-007 chuyển `AdsSettings` thành `public object`, nên module staging gọi thẳng `AdsSettings.placementEnabled(it)`. Plan không sửa `AdsSettings.kt`.

**Bằng chứng production không đọc giá trị staging**, ba lớp:

1. Unit test ở `:core:config`: với tập rỗng (đúng thứ production nhận), kết quả giống hệt trước T-008.
2. Unit test quét mã nguồn trong `app/src/test`: ngoài `src/staging`, không file `.kt` nào của `:app` nhắc tới `LocalRemoteValues`. Test chạy trong `testStagingDebugUnitTest` mà CI đã chạy, nên không cần sửa `scripts/check.sh`. Lý do chọn quét mã nguồn: CI không chạy unit test của biến thể production, và repo chưa có thư viện test của Hilt (thêm thư viện là sửa `gradle/libs.versions.toml`, file bị khóa).
3. `@Multibinds` nằm trong `ConfigModule` (dùng chung mọi biến thể), nên đồ thị Hilt của production luôn có tập, kể cả rỗng. Build production không có trong CI và không chạy trên máy (câu hỏi 3); việc production không đọc giá trị staging do lớp 1 và lớp 2 chứng minh.

**Bảng khóa cột "staging"**: plan không thêm. `RemoteConfigKeysTest` của T-007 nằm ở `app/src/test`, biên dịch cho cả hai biến thể nên không được nhắc tới `StagingRemoteValuesModule` (chỉ có ở `app/src/staging`); muốn có cột này phải chuyển phần sinh bảng sang `app/src/testStaging` và sửa file của T-007. Xem câu hỏi 2.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/template/core/config/LocalRemoteValues.kt` | Qualifier `@LocalRemoteValues`, lớp `LocalRemoteValue` với hàm tạo `of(key, value)`, KDoc nêu thứ tự ưu tiên và ADR-0023 |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/RemoteSettings.kt` | `DefaultRemoteSettings` nhận tập `@LocalRemoteValues`, kiểm tên trùng, áp thứ tự Firebase, cục bộ, mặc định trong `value(key)`; cập nhật KDoc của `RemoteSettings` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/template/core/config/di/ConfigModule.kt` | `@Multibinds @LocalRemoteValues` cho `Set<LocalRemoteValue>` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/template/core/config/DefaultRemoteSettingsTest.kt` | Hàm tạo `settings(localValues = ...)` và các test ở mục 4; giữ các test có sẵn của T-007 |
| Tạo | `app/src/staging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModule.kt` | `@ElementsIntoSet @LocalRemoteValues`: bật mọi vị trí của `AdPlacement` |
| Tạo | `app/src/testStaging/kotlin/com/vglstudio/template/di/StagingRemoteValuesModuleTest.kt` | Test bộ giá trị staging (mục 4) |
| Tạo | `app/src/test/kotlin/com/vglstudio/template/LocalRemoteValuesSourceSetTest.kt` | Test quét mã nguồn: chỉ `src/staging` nhắc tới `LocalRemoteValues` |
| Sửa | `docs/ops/remote-config.md` | Bỏ ghi chú đợt 2 của phần giá trị cục bộ; mục 3 bước 5 chỉ cách khai báo trong `StagingRemoteValuesModule`; mục 4 ghi đúng hai thứ tự (cờ: bật cục bộ thắng; tham số: Firebase, cục bộ, mặc định); mục 5 thêm: project staging chỉ đặt khóa muốn đổi, không nhập cả file mẫu |
| Sửa | `docs/ops/ads-guide.md` | Mục 8, dòng "tới khi có giá trị cục bộ cho staging (ADR-0023, đợt 2), quảng cáo không hiện trên staging": thay bằng "staging bật mọi vị trí của `AdPlacement` bằng giá trị cục bộ (`StagingRemoteValuesModule`); Firebase staging vẫn tắt được từng vị trí" |

Không file nào trong `scripts/`, `.github/`, `build-logic/` nhắc tới các file trên (đã tìm bằng `grep -rln`). `AdsSettings.kt` không sửa (T-007 đã để public). `AdsController.kt` đọc `RemoteSettings` nhưng interface không đổi nên không sửa. `StagingFeatureFlagsModule.kt`, `DefaultFeatureFlags.kt`, `LocallyEnabledFlags.kt` không đổi. `app/build.gradle.kts` không đổi: `junit4` đã có trong `testImplementation` của mọi module (`build-logic`, `KotlinAndroid.kt`), và các source set `test`, `testStaging` có sẵn theo quy ước của Android Gradle Plugin.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không có. Không đổi `FeatureFlag` hay `StagingFeatureFlagsModule`.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Trên bản staging, mọi vị trí đã có trong `AdPlacement` (hiện là `sample_bottom_banner`) được bật khi Firebase không trả giá trị, vẫn chỉ dùng mã thử của Google (ADR-0013). Production không đổi: vị trí mặc định tắt (ADR-0014).
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

Test viết trước code. Các test ở `:core:config` thất bại vì `DefaultRemoteSettings` chưa nhận tham số `localValues` (lỗi biên dịch), và sau khi thêm tham số rỗng thì thất bại vì giá trị cục bộ bị bỏ qua. Test ở `:app` thất bại vì `StagingRemoteValuesModule` chưa có. Test quét mã nguồn qua ngay (chưa file nào nhắc tới qualifier); xem ngoại lệ bên dưới.

Khóa dùng trong test `:core:config`: khai báo riêng trong file test, ví dụ `RemoteKey.boolean("test_switch", default = false, description = "...")`, `RemoteKey.long("test_interval_seconds", default = 120L, ...)`, `RemoteKey.double(...)`, `RemoteKey.string(...)`. Giá trị Firebase giả qua `FakeRemoteFlagSource` (sau T-007).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Thứ tự: không có giá trị nào thì dùng mặc định | Unit test | `DefaultRemoteSettingsTest.withoutRemoteOrLocalValue_usesDefault` |
| Thứ tự: cục bộ thay mặc định khi Firebase không có giá trị, cả bốn kiểu | Unit test | `DefaultRemoteSettingsTest.localValue_replacesDefault_whenNoRemoteValue` (boolean `true` thay `false`, long, double, string) |
| Thứ tự: Firebase thắng cục bộ | Unit test | `DefaultRemoteSettingsTest.remoteValue_winsOverLocalValue` (Firebase `false`, cục bộ `true`, kết quả `false`) |
| Giá trị Firebase tới sau, rồi bị bỏ | Unit test (Turbine) | `DefaultRemoteSettingsTest.remoteValueActivatedThenRemoved_fallsBackToLocalValue`: phát `true` (cục bộ), Firebase `false` thì phát `false`, Firebase rỗng lại thì phát `true`, không phát mặc định |
| Giá trị cục bộ chỉ áp cho đúng khóa | Unit test | `DefaultRemoteSettingsTest.localValueOfOtherKey_doesNotChangeThisKey` |
| KB-8, production không có giá trị cục bộ (hành vi) | Unit test | `DefaultRemoteSettingsTest.withoutLocalValues_readsRemoteThenDefault_asBefore`: tập rỗng, không Firebase thì mặc định; Firebase có thì giá trị Firebase |
| Khai báo trùng tên khóa bị chặn | Unit test | `DefaultRemoteSettingsTest.duplicateLocalKeyNames_failAtCreation`: `assertThrows(IllegalArgumentException::class.java)`, thông báo chứa tên khóa |
| KB-8, staging bật mọi vị trí | Unit test (`testStagingDebugUnitTest`) | `StagingRemoteValuesModuleTest.everyAdPlacement_isEnabled`: tập tên khóa có giá trị `true` bằng đúng tập `ads_<vị trí>_enabled` của `AdPlacement.entries` |
| Staging chỉ đặt khóa vị trí | Unit test | `StagingRemoteValuesModuleTest.onlyPlacementKeys_areSet`: mọi phần tử có tên dạng `ads_<vị trí>_enabled`; không có `ads_enabled` |
| KB-8, production không có giá trị cục bộ (mã nguồn) | Unit test | `LocalRemoteValuesSourceSetTest.onlyStagingSourceSet_declaresLocalRemoteValues`: duyệt mọi `.kt` dưới `src/` của `:app` (thư mục làm việc của unit test là thư mục module), bỏ `src/staging`, `src/test`, `src/testStaging`; không file nào chứa `LocalRemoteValues`. Thêm một khẳng định là `src/staging` có ít nhất một file chứa chữ này, để test không qua khi đường dẫn sai |
| Đồ thị Hilt của staging có tập và nhận phần đóng góp | Build | `:app:assembleStagingDebug` trong `scripts/check.sh` |
| Ngoại lệ dừng app không có | Kiểm tra toàn bộ | `scripts/check.sh` qua |
| KB-8, quảng cáo hiện trên staging không cần Firebase thật | Kiểm tra thủ công | Cài bản staging build từ file giả (hoặc bản `staging.yml` khi project staging chưa đặt khóa vị trí), đồng ý ở hộp thoại, mở màn hình mẫu: thấy quảng cáo thử của Google ở `sample_bottom_banner`. Log `adb logcat -v threadtime \| grep ' VGL\.'` không có dòng báo vị trí tắt |

**Ngoại lệ viết test trước:** `LocalRemoteValuesSourceSetTest` qua ngay trên code hiện tại, vì chưa file nào nhắc tới `LocalRemoteValues`; nó là hàng rào cho sau này, không chứng minh code mới. Để chứng minh test bắt được lỗi, AI thử tạm đặt một file nhắc tới qualifier trong `app/src/main` (không commit), thấy test thất bại, rồi xóa; ghi lần thử vào nhật ký. Người duyệt cần đồng ý.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (module core dùng chung `:core:config`), điểm 1 (đổi khi nào quảng cáo hiện trên bản staging; production không đổi). Rủi ro cao, bắt buộc duyệt chéo; người phụ trách sản phẩm cần biết trước khi duyệt.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Hai module: `:core:config`, `:app`; đổi hàm tạo của `DefaultRemoteSettings` |
| B. Độ rõ của yêu cầu | 2 | ADR-0023 chốt thứ tự, nhưng hướng dẫn và cờ đang theo thứ tự ngược; API của T-007 lấy từ plan, chưa merge |
| C. Khả năng kiểm chứng tự động | 1 | Thứ tự ưu tiên, bộ giá trị staging, ranh giới source set có unit test; quảng cáo hiện thật trên staging kiểm tra thủ công; build production không kiểm (câu hỏi 3) |
| D. Ảnh hưởng chéo | 2 | Sửa `RemoteSettings.kt`, `DefaultRemoteSettingsTest.kt`, `remote-config.md` mà T-007 cũng sửa, và `ads-guide.md` mà T-007, T-014 cũng sửa; T-009 đến T-014 dựa vào việc vị trí bật trên staging |
| E. Độ mới của kỹ thuật | 1 | Biến thể của `@LocallyEnabledFlags`; `@ElementsIntoSet` và test quét mã nguồn trong `:app` chưa có trong dự án nhưng đơn giản |
| **Tổng** | 9 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:config`: hàm tạo của `DefaultRemoteSettings` thêm tham số; interface `RemoteSettings` không đổi. Production nhận tập rỗng, hành vi như cũ.
  - `:core:ads`: không sửa; dùng `AdsSettings.placementEnabled` công khai của T-007.
  - `:app` staging: mọi vị trí quảng cáo bật trên staging. `AppLaunchTest` (emulator, staging) đã không dùng `assertIsDisplayed` vì hộp thoại đồng ý phủ lên app; quảng cáo ở cuối màn hình mẫu không đổi màn hình đích mà test chờ, nên không sửa.
- Task khác trong sprint có thể xung đột:
  - T-007: cùng sửa `RemoteSettings.kt`, `DefaultRemoteSettingsTest.kt`, `docs/ops/remote-config.md`, `docs/ops/ads-guide.md` (mục khác). T-008 chạy sau khi T-007 đã merge.
  - T-009 (tần suất, công tắc tổng, ân hạn phiên đầu 180 giây): trên staging, ân hạn phiên đầu làm quảng cáo toàn màn hình chưa hiện trong 3 phút đầu. Nếu muốn đặt 0 cho staging thì T-009 thêm một dòng vào `StagingRemoteValuesModule` (câu hỏi 4).
  - T-014 (màn hình xem thử): vị trí của màn hình xem thử tự bật trên staging nhờ module này. T-014 cũng thay dòng về staging ở `ads-guide.md` mục 8; sau T-008 dòng đó đã đổi chữ, T-014 sửa theo nội dung mới.
- Đề xuất thứ tự chạy: ngay sau T-007, trước T-009 đến T-015. Chuỗi phụ thuộc T-007, T-008 xếp sau các task độc lập trong cùng phiên.

## 7. Điều kiện dừng riêng

1. Sau T-007, `RemoteSettings` không có `value(key: RemoteKey<T>)`, `RemoteKey` không có `name` hay `default`, hoặc `DefaultRemoteSettings` không nằm trong `RemoteSettings.kt`: dừng, ghi API thật, sửa plan.
2. Sau T-007, `AdsSettings.placementEnabled` không gọi được từ `:app` (ví dụ `AdsSettings` vẫn `internal`): dừng, không tự sửa `AdsSettings.kt`.
3. Lần thử tạm đặt qualifier trong `app/src/main` mà `LocalRemoteValuesSourceSetTest` vẫn qua: test không chặn được, dừng.

## 8. Câu hỏi cho người duyệt

1. **Thứ tự ưu tiên ở staging.** AI đề xuất giữ đúng ADR-0023: Firebase, rồi cục bộ, rồi mặc định, để project staging thật tập được việc tắt vị trí và đổi tần suất từ xa; với file giả hai thứ tự như nhau. Đánh đổi: khác cờ tính năng (cục bộ thắng), và nhập cả file mẫu vào project staging sẽ tắt quảng cáo staging; plan ghi cảnh báo vào hướng dẫn. Phương án khác: cục bộ thắng ở staging, giống cờ, staging luôn có quảng cáo nhưng không thử tắt từ xa được; khi đó phải sửa ADR-0023 mục 5 (ngoài plan này).
   Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
2. **Cột "staging" trong bảng khóa.** AI đề xuất không làm trong T-008: phần sinh bảng của T-007 không thấy module staging, làm thì phải chuyển nó sang unit test staging của `:app` và sửa file của T-007. Nếu cần, mở ticket riêng sau khi T-007 merge, khi đã biết tên file.
   Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
3. **Build production trên máy với tham số giả** (`compileProductionDebugKotlin`, chỉ biên dịch). AI đề xuất cho phép trong phiên tự chạy: đây là cách duy nhất kiểm đồ thị Hilt của production, giá trị giả, không chạm cấu hình production thật (risk-scoring.md, mục 4, điều 7). Rà soát thấy: `app/src/production` không có `google-services.json` (chỉ `app/src/staging` có file giả), mà task `processProductionDebugGoogleServices` chạy trước biên dịch, nên lệnh nhiều khả năng thất bại ở bước này; tạo file đó là cấu hình production, bị cấm. Khi đó áp điều kiện dừng 3 (ghi lại, không coi là lỗi của task). T-014 cũng dựa vào lệnh này. Nếu không đồng ý hay biết trước là không chạy được, bỏ dòng này ở mục 4 và dev chạy tay với file Firebase thật.
   Trả lời (anh Nhật, 2026-10-23): đồng ý đề xuất bỏ lệnh. `compileProductionDebugKotlin` không chạy trong task (thiếu `google-services.json` của production, không được tạo); đã bỏ khỏi mục 4 và mọi chỗ nhắc tới. Việc production không đọc giá trị staging do unit test tập rỗng ở `:core:config` và test quét mã nguồn `LocalRemoteValuesSourceSetTest` chứng minh.
4. **Ân hạn phiên đầu trên staging** (khóa của T-009, mặc định 180 giây). AI đề xuất để T-009 quyết định và thêm vào `StagingRemoteValuesModule`; T-008 chỉ bật vị trí như ticket ghi.
   Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
5. **CI không chạy unit test của biến thể production** (`scripts/check.sh` chỉ chạy `testStagingDebugUnitTest` ở `:app`). Plan dùng test quét mã nguồn chạy ở staging thay thế. Có nên đề xuất về dự án mẫu thêm `testProductionDebugUnitTest` vào `scripts/check.sh` (plan rủi ro cao riêng) không? AI đề xuất có, ngoài T-008.
   Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI ghi lại theo lời người duyệt. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm hiện một dev đảm nhận cả hai vai)
- **Ghi chú:** anh Nhật duyệt ngày 2026-10-23 ("Duyệt tất cả các plan"), sau rà soát độc lập của AI; AI ghi mục này theo lời anh. Câu hỏi ở mục 8 đã có trả lời. Cần PR #81 (`release.sh`, `check.sh`) merge trước phiên chạy.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
