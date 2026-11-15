---
ticket: T-073
title: release.sh đọc và kiểm khóa, mã đơn vị MAX
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.4M         # cỡ S (scripts/token-usage.py estimate, 2026-11-15)
blocking_points: [ads, build, ci]
run_order:                   # AI điền khi lập danh sách chạy
reviewer: Nhật
cross_reviewer: Nhật         # nhóm một dev (risk-scoring.md, mục 3)
approved_at:
lane:                        # để trống; ticket ghi auto_run: false, làm có người, một PR
---

# PLAN T-073: RELEASE.SH ĐỌC VÀ KIỂM KHÓA, MÃ ĐƠN VỊ MAX

## 1. Tóm tắt

Bản production có adapter MAX (nhóm `com.applovin.mediation` trên classpath) không build được khi thiếu `-PmaxSdkKey`; `-PmaxSdkKey` và `-PmaxAdUnits` sai ký tự cũng bị từ chối thay vì lọt thẳng vào `BuildConfig`. `scripts/release.sh` đọc hai giá trị từ `release.properties`, kiểm, báo thiếu ở `--check-only` và truyền cho Gradle; hướng dẫn phát hành ghi cấu hình MAX và việc bỏ AppLovin khỏi nhóm mediation AdMob (câu hỏi 45). Phần `scripts/release.sh` do **anh Nhật** sửa (AI không được chạm file này, ADR-0049); AI viết test trước, phần Gradle và tài liệu, rồi cả PR qua `scripts/check.sh`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31, tiêu chí 8 | Khóa SDK MAX và mã APS không nằm trong repo; bản staging chạy được khi không có chúng. | Giữ nguyên: khóa và mã MAX chỉ ở thư mục phát hành ngoài repo, đi vào build qua biến môi trường; staging không đổi (vẫn đọc `local.properties`, CI không khóa). Phần mới: production **có MAX** thì bắt buộc khóa (câu hỏi 43: "`preProduction*Build` báo lỗi khi thiếu lúc thứ tự có `max`"; thứ tự đặt từ xa, mặc định MAX trước, nên lúc build coi như luôn có `max` khi adapter MAX có trên classpath) |
| KB-31, câu hỏi 43 | Khóa và mã đơn vị MAX vào app bằng tham số build lúc phát hành | `release.sh` đọc `maxSdkKey`, `maxAdUnits` từ `release.properties`, truyền bằng `export ORG_GRADLE_PROJECT_...` như các giá trị khác (ADR-0049, mục 11) |
| KB-31, câu hỏi 45 | App dùng MAX thì nhóm mediation AdMob không có AppLovin; ghi vào hướng dẫn phát hành | Thêm mục vào `docs/ops/release.md`; code không đổi |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0049 (mục 10, 11; AI không chạm `scripts/release.sh`, không đọc, tạo hay sửa thư mục phát hành), ADR-0045 (mục 5.5: khóa MAX lúc build; mục 1.4: không theo JBase để khóa trong `strings.xml`), ADR-0007 (production không build trên CI), ADR-0031 (viết lại câu khẳng định cũ), ADR-0029.

Bài học: L-125 (hợp đồng giữa `release.sh` và build: mọi thuộc tính truyền thì build đọc, mọi thuộc tính build bắt buộc thì script truyền).

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu khóa AppsFlyer (T-033, issue #261): build bắt buộc khóa theo classpath, `release.sh` kiểm theo khai báo trong `*.gradle.kts`, test hợp đồng L-125 đọc mã ADR của dòng bắt buộc.

**Thứ tự commit trong một PR** (nhánh `claude/T-073-release-max`):

1. AI, commit test (bước 3 của auto-run.md): test của mục 4 trong `ProductionPropertiesTest.kt` và `scripts/tests/run.sh`. Đỏ đúng lý do.
2. AI, commit code: `app/build.gradle.kts` và tài liệu. `ProductionPropertiesTest` xanh; test của `release.sh` trong `run.sh` còn đỏ.
3. **Anh Nhật**, commit `scripts/release.sh` theo bước 4 dưới, đẩy lên cùng nhánh. AI không mở, không sửa file này; nếu test vẫn đỏ, AI báo dòng test và đầu ra, anh Nhật sửa.
4. AI chạy `scripts/check.sh`, rà soát độc lập, mở PR.

Bước 2 không được merge riêng: thiếu bước 3 thì mọi bản phát hành của app có MAX dừng ở `preProduction*Build` sau khi người đã nhập mật khẩu (lý do T-068 dời phần này sang đây).

**Bước 1. `app/build.gradle.kts` (AI).**

- Khối kiểm `preProduction*Build` (sau `val missingProductionProperties = mapOf(`): trong `doFirst`, cạnh `hasAppsFlyer`, thêm `val hasMax = runtimeClasspath.get().incoming.resolutionResult.allComponents.any { (it.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier)?.group == "com.applovin.mediation" }`. Nhóm `com.applovin.mediation` là adapter của MAX (`max-mediation-google`); adapter AppLovin của AdMob là `com.google.ads.mediation:applovin`, SDK là `com.applovin:applovin-sdk`, nên chỉ app dùng MAX khớp.
- `missing` cộng thêm `if (hasMax && providers.gradleProperty("maxSdkKey").orNull.isNullOrBlank()) mapOf("maxSdkKey" to "ADR-0037") else emptyMap()` (viết đúng dạng `mapOf("maxSdkKey" to "ADR-0037")` trên một dòng, test hợp đồng đọc bằng `grep`). Mã `ADR-0037` theo ADR gốc của khóa MAX, như dòng AppsFlyer giữ `ADR-0027` (test hợp đồng tìm đúng chuỗi này).
- Sau khối `missing`, kiểm ký tự (không in giá trị): `-PmaxSdkKey` có mặt và không rỗng mà không khớp `Regex("[A-Za-z0-9_-]+")`, hay `-PmaxAdUnits` có mặt mà không khớp `Regex("[A-Za-z0-9_=;-]*")` (cùng regex với flavor staging), thì `throw GradleException("Build production có tham số sai dạng: -PmaxSdkKey (ADR-0037).")` (liệt kê tên các tham số sai, không giá trị). Production từ chối thay vì lọc như staging: lọc lặng lẽ ở production là phát hành một bản không có MAX mà không ai biết.
- Chú thích ở đầu `productFlavors` và trong flavor `production`: bỏ "(optional until T-073)", ghi "required with the MAX adapter, checked by the production pre-build task and scripts/release.sh" cho `maxSdkKey`; `maxAdUnits` "optional: a slot without a MAX unit loads from AdMob only; characters checked". Không đổi dòng `buildConfigField` nào của hai flavor.
- Chú thích trên `val missingProductionProperties`: thêm câu về khóa MAX theo classpath, cùng cách AppsFlyer.

**Bước 2. Tài liệu (AI).**

- `docs/ops/release.md`:
  - Mục 1.2: thêm hai dòng bảng: "Khóa SDK AppLovin MAX (86 ký tự)" lấy ở AppLovin, **Account > Keys > SDK Key**; "Mã đơn vị MAX của từng khe (`banner`, `banner_mrec`; thêm `interstitial`, `rewarded`, `app_open` khi app dùng các khe đó)" lấy ở AppLovin, **MAX > Mediation > Manage > Ad Units**. Ghi khi nào cần: app có adapter MAX (mọi app tạo từ dự án mẫu hôm nay, `:core:ads` khai báo `libs.max.mediation.google`).
  - Mục mới 1.5 "Nhóm mediation AdMob khi app dùng MAX" (câu hỏi 45, chuẩn JBase): trên AdMob, mọi nhóm mediation của app bỏ "AppLovin (Bidding)" và "AppLovin" (waterfall), để AppLovin không đấu giá hai lần với chính nó; MAX gọi Google qua adapter Google của MAX. Adapter AppLovin của AdMob vẫn có trong build (bỏ nó là ticket riêng, plan T-068 câu hỏi 5). Kiểm lại sau mỗi lần thêm nhóm mediation mới.
  - Mục 2.2: thêm `maxSdkKey=<khóa SDK MAX>` và `maxAdUnits=banner=<mã MAX>;banner_mrec=<mã MAX>` vào khối `release.properties`; đoạn giải thích: khóa bắt buộc khi app có adapter MAX (file `*.gradle.kts` khai báo `libs.max.mediation.google` hay tọa độ nhóm `com.applovin.mediation`), chỉ chữ, số, `_`, `-`; `maxAdUnits` tùy chọn, khe thiếu mã chỉ chạy AdMob, script in số khe có mã, không in mã; không có adapter MAX mà có dòng `maxSdkKey` thì script in CHÚ Ý và không dùng khóa.
  - Mục 2.3, bước 1: thêm "có khóa SDK MAX khi app có adapter MAX, khóa và mã đơn vị MAX đúng ký tự".
- `docs/ops/ads-business-rules.md`, M-09: thêm vế "bản production có adapter MAX không build được khi thiếu khóa; khóa và mã đơn vị MAX sai ký tự bị từ chối ở production (`preProduction*Build`, `scripts/release.sh`)".
- `docs/ops/ads-guide.md`, dòng "Khóa SDK MAX" (mục 7): thay "(tùy chọn; `release.sh` chưa đọc và kiểm khóa này)" bằng "(bắt buộc khi app có adapter MAX; `scripts/release.sh` đọc từ `release.properties`, `docs/ops/release.md`)".

**Bước 3. Test (AI), xem mục 4.**

**Bước 4. `scripts/release.sh` (anh Nhật).** AI không mở file này; đây là điều test của mục 4 kiểm, chữ trong ngoặc kép là chữ test tìm.

1. Biết app có MAX như cách đang biết app có AppsFlyer: một file `*.gradle.kts` (bỏ thư mục `build`, `.gradle`) khớp `libs\.max\.mediation\.|"com\.applovin\.mediation:`.
2. Đọc `maxSdkKey` và `maxAdUnits` từ `release.properties` (dòng có thể thiếu).
3. Ở bước 1 của script (kiểm thư mục phát hành), sau phần khóa AppsFlyer và trước phần địa chỉ R2:
   - App có MAX, `maxSdkKey` thiếu hay rỗng: dừng, dòng lỗi chứa "thiếu maxSdkKey".
   - App có MAX, `maxSdkKey` không khớp `^[A-Za-z0-9_-]+$`: dừng, dòng lỗi chứa "maxSdkKey sai dạng".
   - App không có MAX mà có `maxSdkKey`: một dòng bắt đầu bằng "CHÚ Ý: " chứa "maxSdkKey", chạy tiếp, không dùng khóa.
   - `maxAdUnits` không rỗng mà không khớp `^[a-z_]+=[A-Za-z0-9_-]+(;[a-z_]+=[A-Za-z0-9_-]+)*$`: dừng, dòng lỗi chứa "maxAdUnits sai dạng".
   - Không bao giờ in khóa hay mã đơn vị MAX (cả khi báo lỗi).
4. Truyền cho Gradle bằng đúng dạng `export ORG_GRADLE_PROJECT_maxSdkKey` và `export ORG_GRADLE_PROJECT_maxAdUnits` (test hợp đồng đọc dòng `export ORG_GRADLE_PROJECT_<tên>`); app không có MAX thì truyền rỗng.
5. Phần tóm tắt (cả `--check-only`): một dòng bắt đầu bằng "   MAX:", ví dụ "   MAX: có khóa, 2 khe có mã đơn vị" hay "   MAX: không có trong app"; không in giá trị.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/build.gradle.kts` | Bước 1: bắt buộc `maxSdkKey` theo nhóm `com.applovin.mediation`, kiểm ký tự `maxSdkKey`, `maxAdUnits` ở `preProduction*Build`; chú thích |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` | Mục 4: viết lại hai câu khẳng định (mục 3.5), thêm test kiểm ký tự |
| Sửa | `scripts/tests/run.sh` | Mục 4: hàm `write_release_properties` nhận khóa và mã MAX; test hợp đồng L-125 (10 thuộc tính, dòng `ADR-0037`); test `release.sh` với MAX; bản sao `--check-only` chép thêm `core/ads/build.gradle.kts` |
| Sửa | `docs/ops/release.md` | Bước 2: mục 1.2, mục mới 1.5, mục 2.2, mục 2.3 |
| Sửa | `docs/ops/ads-business-rules.md` | M-09: production bắt buộc khóa khi có adapter MAX, kiểm ký tự |
| Sửa | `docs/ops/ads-guide.md` | Dòng "Khóa SDK MAX" của mục 7 |
| Sửa | `docs/features/ads/tickets/T-073-release-sh-ma-max.md` | Trạng thái, dòng `plan:` (làm có người, một PR) |
| Sửa | `docs/features/ads/plans/T-073-release-doc-va-kiem-khoa-max.md` | Plan này: trạng thái, nhật ký (làm có người, một PR) |

**Ngoài bảng, người làm:** `scripts/release.sh` theo bước 4 của mục 3.1. Không plan nào được ghi file này vào bảng (`scripts/plan-check.py`, `NEVER`; `CLAUDE.md`, mục Không được làm; ADR-0049): AI không mở, không sửa nó. Anh Nhật sửa và commit trên cùng nhánh (câu hỏi 3).

Nơi đọc các file bị sửa (`grep -rn` trong `scripts/ .github/ */src/test`):

- `scripts/tests/run.sh` được gọi từ `scripts/check.sh`, `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/local-setup.sh` (gợi ý chạy), `scripts/main-guard-verified.sh` và `scripts/merge-if-green.sh` (phân loại file là code hay tài liệu): plan chỉ thêm test trong file, không đổi tên, đối số hay mã thoát của nó, nên các nơi gọi không đổi.
- `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md` được nhắc trong chú thích của các test `:core:ads`, `:core:analytics` (`core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`): chỉ dẫn mã luật trong chú thích (`grep` mọi dòng nhắc hai file đều là chú thích), không đọc file; plan chỉ sửa M-09 và một dòng của mục 7, không đổi mã luật nào.
- `scripts/release.sh` được nhắc trong `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementSourceTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` (chép cách đọc vị trí quảng cáo), `scripts/status.py`, `scripts/run-check.py`, `scripts/plan-queue.py` (chú thích): phần anh Nhật sửa không đổi cách đọc `AdPlacement.kt`.
- `docs/ops/release.md` được `scripts/status.py` dẫn "mục 1.2": plan thêm dòng vào bảng của mục 1.2, không đổi số mục.
- `app/build.gradle.kts` được `scripts/ui-test-verdict.sh` đọc `applicationId`: plan không đổi dòng đó.

- `app/build.gradle.kts`: `ProductionPropertiesTest` (trong bảng); `scripts/tests/run.sh` (test hợp đồng, bản sao `--check-only`; trong bảng); `scripts/setup-check.sh`, `scripts/new-app.sh`, `scripts/release.sh` đọc `applicationId`, `versionCode`, `versionName`, `ASSETS_BASE_URL`: plan không đổi các dòng đó. `pr-check.yml` build bản release khi file build đổi: không đổi gì ở flavor staging, nên bản staging release không đổi.
- `scripts/release.sh`: chỉ `scripts/tests/run.sh` chép và chạy nó.
- `docs/ops/release.md`, `ads-business-rules.md`, `ads-guide.md`: `scripts/history-refs.py` quét tài liệu quy tắc (không ghi mã ticket, phiên); `scripts/ads-plan.py` đọc `docs/remote-config-keys.md`, không đọc ba file này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng; đổi điều kiện build bản production có MAX (điểm chặn 1 theo nghĩa rộng: cấu hình nạp quảng cáo MAX ở production)
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (`max-mediation-google`, `applovin-sdk` đã có ở `:core:ads` từ T-068; plan chỉ đọc nhóm `com.applovin.mediation` trên classpath)

### 3.4. Phương án dự phòng

Không có. Cách đọc classpath là đúng mẫu `hasAppsFlyer` đang chạy; production không build trên CI nên phần Gradle chỉ kiểm bằng test đọc file (như T-033, T-068) và kiểm thủ công 1 ở mục 4.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-11-15) báo "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)": plan chỉ sửa `app/build.gradle.kts` (Kotlin script). Test có sẵn đọc file build là `ProductionPropertiesTest` (tìm bằng `grep -rn build.gradle.kts */src/test`); test của `release.sh` nằm trong `scripts/tests/run.sh`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Production đọc `-PmaxSdkKey` nhưng không bắt buộc; khối kiểm không nhắc MAX | Production có adapter MAX bắt buộc `-PmaxSdkKey` (bước 1) | `ProductionPropertiesTest.maxSdkKey_isOptionalUntilReleaseShPassesIt`: khối kiểm không chứa `maxSdkKey`, `com.applovin`, `ADR-0037` | Viết lại (ADR-0031), đổi tên `maxSdkKey_isRequiredForProductionWithMax`. Câu cũ: `listOf("maxSdkKey", "com.applovin", "ADR-0037").forEach { assertFalse(it in check) }`. Câu của plan nói điều mới: "Production có adapter MAX bắt buộc `-PmaxSdkKey`" (mục 1, mục 3.1 bước 1). Câu ý định giữ lại: khối production đọc khóa từ `-PmaxSdkKey`, đưa vào `MAX_SDK_KEY`, không đọc `local.properties`. Câu mới: khối kiểm chứa `?.group == "com.applovin.mediation"`, `if (hasMax && providers.gradleProperty("maxSdkKey").orNull.isNullOrBlank())`, `mapOf("maxSdkKey" to "ADR-0037")`; khối production vẫn có `"MAX_SDK_KEY"` và `gradleProperty("maxSdkKey")` (giữ nguyên hai câu đó). Đối chứng: tạm xóa dòng `mapOf("maxSdkKey" to "ADR-0037")` (lệnh ở mục 4) thì test đỏ |
| Production nhận `-PmaxAdUnits` tùy chọn, không kiểm; khối kiểm không nhắc `maxAdUnits` | `maxAdUnits` vẫn tùy chọn (không vào `missing`) nhưng sai ký tự thì bị từ chối (bước 1) | `ProductionPropertiesTest.maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional`: `assertFalse(check, "maxAdUnits" in check)` | Viết lại câu đó (ADR-0031), giữ tên test và mọi câu khác. Câu cũ: `assertFalse(check, "maxAdUnits" in check)`. Câu của plan nói điều mới: "`-PmaxAdUnits` có mặt mà không khớp ... thì throw" (mục 3.1 bước 1). Câu ý định: `maxAdUnits` vẫn tùy chọn ở production. Câu mới: `assertFalse(check, "mapOf(\"maxAdUnits\"" in check)` (không bắt buộc) và `assertTrue(check, "gradleProperty(\"maxAdUnits\")" in check)` (có kiểm). Đối chứng: tạm thêm `"maxAdUnits" to "ADR-0037",` vào `mapOf(` của `missingProductionProperties` thì test đỏ |
| Test hợp đồng: `release.sh` truyền 8 thuộc tính; build bắt buộc 4 (có SDK AppsFlyer) | 10 thuộc tính (thêm `maxSdkKey`, `maxAdUnits`); bắt buộc 5 khi repo có MAX | `scripts/tests/run.sh`: `expect "contract: ... every Gradle property that release.sh exports" "8 none"` và `"... the production build requires" "$required_count none"` (4) | Cập nhật dữ liệu đếm theo phần plan thêm (ADR-0029, ngoại lệ dữ liệu): `"10 none"`; `required_count` cộng 1 khi `repo_has_max=yes`, và dòng đọc `mapOf("…" to "ADR-0027")` có thêm một dòng cùng dạng cho `ADR-0037`. Điều test kiểm giữ nguyên (hai chiều của hợp đồng) |
| Hàm `write_release_properties` ghi 5 dòng | Ghi thêm `maxSdkKey=${5-FakeMaxSdkKey0123456789}` và `maxAdUnits=${6-}` | Mọi test `release.sh` dùng hàm này | Cập nhật dữ liệu (ADR-0029): khóa giả mặc định đúng dạng, để các test đi tới bước sau vẫn đi tới; khóa giả ngắn, không khớp mẫu 86 ký tự của `secret-scan.py` |

## 4. Kiểm thử

Mọi test dưới đây viết ở commit 1 (bước 3), trước bước 1, 2 và trước phần của anh Nhật. Test của `release.sh` chạy trên bản sao của script như các test có sẵn; khóa và mã giả không giống khóa thật (không 86 ký tự, mã không 16 ký tự hex) để `scripts/secret-scan.py` không chặn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu hỏi 43: production có adapter MAX bắt buộc khóa | Unit test `:app` | `ProductionPropertiesTest.maxSdkKey_isRequiredForProductionWithMax` (mục 3.5). Thất bại trước bước 1 vì khối kiểm chưa có `com.applovin.mediation` |
| Production từ chối khóa, mã MAX sai ký tự | Unit test `:app` | `ProductionPropertiesTest.productionMaxValues_wrongCharactersRefused_valueNotPrinted`: khối kiểm (từ `val missingProductionProperties = mapOf(` tới hết khối `tasks.matching { it.name.startsWith("preProduction")`) chứa `Regex("[A-Za-z0-9_-]+")` trên cùng dòng với `maxSdkKey` hay trong biểu thức có `gradleProperty("maxSdkKey")`, chứa `Regex("[A-Za-z0-9_=;-]*")` và `gradleProperty("maxAdUnits")`, có `"Build production có tham số sai dạng: "`; mọi dòng `throw GradleException(` và dòng nối chuỗi ngay sau không chứa `.get()`, `.orNull` hay `getOrElse` (không in giá trị). Thất bại trước bước 1 vì chưa có chuỗi thông báo |
| `maxAdUnits` vẫn tùy chọn ở production | Unit test `:app` | `ProductionPropertiesTest.maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional`, câu viết lại (mục 3.5). Thất bại trước bước 1 vì khối kiểm chưa có `gradleProperty("maxAdUnits")` |
| Staging không đổi (KB-31, tiêu chí 8) | Unit test `:app` | Giữ nguyên `stagingMaxKey_readsLocalPropertiesOnly`, `stagingMaxKey_isFilteredBeforeBuildConfig`, `stagingKey_isFixedEmpty`; chạy qua ở mọi commit |
| Hợp đồng L-125, hai chiều | Test script | `scripts/tests/run.sh`: `"10 none"` và `required_count` 5 khi `repo_has_max=yes` (mục 3.5). Biến `repo_has_max` tính như `repo_has_appsflyer`, mẫu `libs\.max\.mediation\.|"com\.applovin\.mediation:`. Chiều thứ hai thất bại sau bước 1 tới khi `release.sh` có `export ORG_GRADLE_PROJECT_maxSdkKey`; chiều thứ nhất thất bại tới khi có cả hai `export` |
| `release.sh` báo thiếu khóa khi app có MAX | Test script | `release_fails_with "missing MAX SDK key (this repo has the MAX adapter)" "thiếu maxSdkKey"` với `write_release_properties ... "https://cdn.example.vn/" "AbCdEf0123456789XyZ" ""` (khóa MAX rỗng). Nhánh `repo_has_max=no` (app tạo từ mẫu mà bỏ MAX): làm như nhánh AppsFlyer, trên bản sao có module khai báo `implementation(libs.max.mediation.google)` |
| `release.sh` từ chối khóa sai dạng | Test script | `release_fails_with "MAX SDK key with odd characters" "maxSdkKey sai dạng"` với khóa `"key with spaces"` |
| `release.sh` từ chối mã đơn vị sai dạng | Test script | `release_fails_with "MAX ad units malformed" "maxAdUnits sai dạng"` với `maxAdUnits` là `"banner"` (thiếu `=`) và một lần với `"banner=ab\"c"` |
| `release.sh` không in khóa, mã MAX | Test script | `expect "release.sh never prints the MAX SDK key or units" "0 0"`: chạy với khóa `FakeMaxSecret0123456789` và `maxAdUnits=banner=FakeUnitAbc`, địa chỉ `http://` (dừng ở R2, sau phần MAX), đếm hai chuỗi trong đầu ra |
| App không có MAX: không cần khóa, có khóa thì CHÚ Ý | Test script | Trên bản sao `no_af` (không có module khai báo MAX): không có dòng `maxSdkKey` thì đi tới `== Kiểm tra commit phát hành`; có `maxSdkKey=FakeMaxSecret0123456789` thì có dòng `^CHÚ Ý: .*maxSdkKey`, vẫn đi tới bước commit, không in khóa |
| `--check-only` có dòng MAX, đủ kiểm | Test script | Bản sao `--check-only` chép thêm `core/ads/build.gradle.kts`; `expect "release.sh --check-only: prints the MAX line without values" "1 0"`: đếm dòng `^   MAX:` và chuỗi khóa giả. Câu khẳng định chính có sẵn ("1 1 0 0") giữ nguyên, nay đi qua phần MAX |
| Câu hỏi 45: hướng dẫn nhóm mediation | Kiểm tra thủ công (người: đọc tài liệu) | Anh Nhật đọc mục 1.5 mới của `docs/ops/release.md` khi duyệt PR |
| Bản production thật có MAX | Kiểm tra thủ công (máy thật: khác, cần thư mục phát hành và khóa thật, chỉ người phát hành có) | Anh Nhật chạy `scripts/release.sh --check-only` với thư mục phát hành thật: (a) chưa thêm `maxSdkKey` thì dừng "thiếu maxSdkKey"; (b) thêm khóa thật thì qua, dòng "MAX:" đúng số khe. Lần phát hành thật đầu tiên sau PR: log thẻ `Ads` của bản production có `MAX started` rồi `MAX ready` |

Đối chứng cho hai câu viết lại (bước 3, sau khi test đã xanh ở bước 1; theo `docs/CLAUDE.md`, mục Khi viết plan, dùng `perl` và trả về bằng `git checkout`):

```sh
perl -pi -e 's/^\s*mapOf\(\Q"maxSdkKey" to "ADR-0037"\E\)\n//' app/build.gradle.kts
git diff --stat -- app/build.gradle.kts   # phải có thay đổi
./gradlew :app:testStagingDebugUnitTest --tests '*ProductionPropertiesTest*' --rerun   # maxSdkKey_isRequiredForProductionWithMax đỏ
git checkout -- app/build.gradle.kts
perl -pi -e 's/\Q"assetsBaseUrl" to "ADR-0015",\E/"assetsBaseUrl" to "ADR-0015",\n    "maxAdUnits" to "ADR-0037",/' app/build.gradle.kts
git diff --stat -- app/build.gradle.kts
./gradlew :app:testStagingDebugUnitTest --tests '*ProductionPropertiesTest*' --rerun   # maxAdUnits_..._productionOptional đỏ
git checkout -- app/build.gradle.kts
```

Dòng `mapOf("maxSdkKey" to "ADR-0037")` có thể nằm trong biểu thức `if` nhiều dòng; nếu mẫu đầu không khớp (`git diff --stat` rỗng), sửa mẫu theo dòng thật trước khi chạy Gradle.

**Ngoại lệ viết test trước:**

- Tài liệu (`release.md`, M-09, `ads-guide.md`) và chú thích trong `app/build.gradle.kts`: chỉ chữ.
- Phần Gradle chỉ được kiểm bằng test đọc file: production không build trên CI (ADR-0007), như T-033, T-068. Hành vi thật kiểm ở kiểm thủ công cuối bảng.
- Test của `release.sh` không do người viết code `release.sh` viết, nên "test viết trước" ở đây là AI viết theo hợp đồng của mục 3.1 bước 4 trước khi anh Nhật sửa script.

### 4.1. Chạy thử

Không cần (E dưới 2): lặp lại mẫu `hasAppsFlyer`, `write_release_properties`, test hợp đồng có sẵn.


## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (cấu hình nạp quảng cáo MAX ở production), 7 (cấu hình build `app/build.gradle.kts`, test của CI `scripts/tests/run.sh`, và `scripts/release.sh`). Mức Cao, duyệt chéo bắt buộc.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | `:app` (file build, test), script phát hành, test script, ba tài liệu |
| B. Độ rõ của yêu cầu | 2 | Câu hỏi 43, 45 đã chốt; còn câu 1 ở mục 8 (app chưa có tài khoản MAX phát hành thế nào) ảnh hưởng cách làm |
| C. Khả năng kiểm chứng tự động | 2 | `release.sh` và test hợp đồng có test tự động; phần Gradle chỉ kiểm bằng đọc file, hành vi thật chỉ thấy khi người phát hành chạy với khóa thật |
| D. Ảnh hưởng chéo | 3 | Mọi app tạo từ dự án mẫu: sau đồng bộ, bản phát hành dừng tới khi người phát hành thêm `maxSdkKey`; T-069, T-070, T-072 chờ ticket này |
| E. Độ mới của kỹ thuật | 0 | Đúng mẫu khóa AppsFlyer |
| **Tổng** | 9 | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `app/build.gradle.kts` (khối kiểm production), `scripts/release.sh`, `scripts/tests/run.sh`, `docs/ops/release.md`.
- App đã tạo từ dự án mẫu (camera-pose, Photune): lần đồng bộ sau (`scripts/template-sync.sh`) đưa thay đổi về; người phát hành phải thêm `maxSdkKey` vào thư mục phát hành trước lần phát hành kế, nếu không `release.sh` dừng ở bước 1 (trước mật khẩu, nên không mất gì). PR ghi điều này để người đồng bộ báo app.
- Task khác có thể xung đột: T-069, T-070 thêm khe `interstitial`, `rewarded`, `app_open` vào `maxAdUnits`: regex của bước 1 và của `release.sh` đã nhận tên khe chữ thường có `_`, không phải sửa. T-085 (backlog) ghi "Lọc ký tự của `-PmaxAdUnits` ở production (T-073)": plan này làm phần đó. Issue #498 (`collapsibleLayer`) không chạm file của plan.
- Đề xuất thứ tự chạy: làm trước T-069 (T-069 phụ thuộc T-073). Không chạy cùng lúc với plan khác sửa `app/build.gradle.kts` hay `scripts/tests/run.sh`.

## 7. Điều kiện dừng riêng

- Cần đổi `buildConfigField` của flavor staging, hay đổi cách staging đọc `local.properties` (KB-31 tiêu chí 8 phải giữ).
- Test của `release.sh` chỉ qua được nếu đổi chữ test tìm khác chữ của mục 3.1 bước 4: dừng, hỏi anh Nhật bên nào đổi, không tự nới test.
- AI cần mở hay sửa `scripts/release.sh` để làm tiếp: dừng, báo anh Nhật.

## 8. Câu hỏi cho người duyệt

1. **App chưa có tài khoản MAX phát hành thế nào?** Mọi app tạo từ dự án mẫu hôm nay có adapter MAX (`:core:ads` khai báo `libs.max.mediation.google`), nên theo câu hỏi 43 mọi bản production đều bắt buộc khóa MAX. App chưa có tài khoản AppLovin sẽ không phát hành được, và không được tự bỏ adapter khỏi `:core:ads` (file của dự án mẫu).
   - Phương án (a), plan viết theo phương án này: bắt buộc đúng như câu hỏi 43 và ticket. App muốn phát hành phải có tài khoản AppLovin và khóa SDK (tạo tài khoản miễn phí, chưa cần mã đơn vị: `maxAdUnits` tùy chọn, khe thiếu mã chỉ chạy AdMob).
   - Phương án (b): `release.properties` nhận `maxSdkKey=none` là cố ý không dùng MAX; `release.sh` in CHÚ Ý, truyền `none`; Gradle coi `none` là có mặt và đưa chuỗi rỗng vào `MAX_SDK_KEY`. Cần sửa spec câu hỏi 43 (spec `changed`, xác nhận lại) và thêm hai test.
   - Đề xuất của AI: (a) nếu mọi app sắp phát hành đều dự định dùng MAX; (b) nếu có app phát hành trước khi có tài khoản AppLovin.
2. **Chữ thông báo của `release.sh`.** Test tìm "thiếu maxSdkKey", "maxSdkKey sai dạng", "maxAdUnits sai dạng", dòng "CHÚ Ý: ...maxSdkKey" và dòng tóm tắt "   MAX:". Anh Nhật muốn chữ khác thì đổi ở plan trước khi AI viết test.
3. **Thứ tự làm.** Đề xuất: AI làm commit 1, 2 trên nhánh `claude/T-073-release-max`, đẩy lên; anh Nhật kéo nhánh, sửa `release.sh`, chạy `scripts/tests/run.sh` (hay chỉ phần `release.sh`) trên máy, đẩy commit; AI chạy `scripts/check.sh`, rà soát, mở PR. Anh Nhật đồng ý?

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
