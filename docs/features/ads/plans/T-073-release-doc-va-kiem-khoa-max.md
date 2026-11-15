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

`scripts/release.sh` đọc khóa SDK MAX và mã đơn vị MAX từ `release.properties`, kiểm ký tự, truyền cho Gradle và in dòng "MAX:" ở phần tóm tắt. Khóa **tùy chọn**: app chưa có tài khoản AppLovin thì không ghi khóa, bản phát hành bỏ qua MAX, chỉ chạy AdMob, và script báo CHÚ Ý (quyết định của anh Nhật ngày 2026-11-15, đổi câu hỏi 43 của spec). Khóa hay mã sai ký tự thì cả `release.sh` lẫn `preProduction*Build` từ chối thay vì để giá trị lọt thẳng vào `BuildConfig`. Hướng dẫn phát hành ghi cấu hình MAX và việc bỏ AppLovin khỏi nhóm mediation AdMob (câu hỏi 45). AI sửa cả `scripts/release.sh` trong task này theo lời cho phép riêng của anh Nhật (mục 3.1).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-31, tiêu chí 8 | Khóa SDK MAX và mã APS không nằm trong repo; bản staging chạy được khi không có chúng. | Giữ nguyên: khóa và mã MAX chỉ ở thư mục phát hành ngoài repo, vào build qua biến môi trường; staging không đổi (đọc `local.properties`, CI không khóa) |
| KB-31, câu hỏi 43 (đổi 2026-11-15, PR này) | Khóa và mã đơn vị MAX vào app bằng tham số build lúc phát hành; production không có khóa thì MAX không khởi tạo, bản phát hành chỉ chạy AdMob, `release.sh` báo CHÚ Ý; khóa, mã sai ký tự bị từ chối | `release.sh` đọc `maxSdkKey`, `maxAdUnits`, truyền bằng `export ORG_GRADLE_PROJECT_...` như các giá trị khác (ADR-0049, mục 11). Không bắt buộc khóa ở `preProduction*Build` (bỏ vế "báo lỗi khi thiếu" của câu trả lời cũ) |
| KB-31, câu hỏi 45 | App dùng MAX thì nhóm mediation AdMob không có AppLovin; ghi vào hướng dẫn phát hành | Thêm mục vào `docs/ops/release.md`; "app dùng MAX" nay là app có khóa MAX ở production; code không đổi |

Khác ticket: ticket ghi "`preProduction*Build` bắt buộc `maxSdkKey` khi nhóm `com.applovin.mediation` có trên classpath" (chuyển từ T-068). Theo quyết định ngày 2026-11-15, plan không bắt buộc khóa, chỉ kiểm ký tự. Ticket sửa dòng đó trong PR này.

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện).

ADR liên quan: ADR-0049 (mục 10, 11; "không chạm vào `scripts/release.sh`": ngoại lệ cho task này, mục 3.1; không đọc, tạo hay sửa thư mục phát hành: giữ nguyên), ADR-0045 (mục 2.5: AppLovin hai vai; mục 5.5: khóa và mã MAX là cấu hình lúc build, không vào repo, không nói bắt buộc), ADR-0007 (production không build trên CI), ADR-0031, ADR-0029.

Bài học: L-125 (hợp đồng giữa `release.sh` và build: mọi thuộc tính truyền thì build đọc, mọi thuộc tính build bắt buộc thì script truyền).

## 3. Giải pháp

### 3.1. Cách làm

**Lời cho phép sửa `scripts/release.sh`.** Anh Nhật, trong phiên ngày 2026-11-15, nguyên văn: "Tôi cho phép bạn sửa file scripts/release.sh lần này, bỏ qua lệnh chặn ở CLAUDE.md". Phạm vi: việc thực hiện T-073 theo plan này, ở bất kỳ phiên nào, chỉ phần mô tả ở bước 3 dưới (câu hỏi 3). Phần còn lại của ADR-0049 giữ nguyên: không đọc, tạo hay sửa thư mục phát hành (`~/.vgl-release/`), không in khóa. File không vào bảng mục 3.2 vì `scripts/plan-check.py` không cho plan nào ghi nó (`NEVER`); đây là ngoại lệ có lời cho phép, không phải lệch phạm vi. Task khác: quy tắc chặn áp lại.

Làm theo mẫu khóa AppsFlyer ở `release.sh` (T-033, issue #261: biết app có SDK qua khai báo trong `*.gradle.kts`, không in khóa, CHÚ Ý khi khóa không dùng) và mẫu lọc của flavor staging (T-068, T-071).

**Thứ tự commit trong một PR** (nhánh `claude/T-073-release-max`):

1. Commit test (bước 3 của auto-run.md): test của mục 4 trong `ProductionPropertiesTest.kt` và `scripts/tests/run.sh`. Đỏ đúng lý do.
2. Commit code: `app/build.gradle.kts`, `scripts/release.sh`, tài liệu.
3. `scripts/check.sh`, rà soát độc lập (agent rà soát được đọc cả `scripts/release.sh` theo lời cho phép trên), mở PR. Merge bằng `scripts/merge-if-green.sh` khi anh Nhật nói rõ trong phiên.

**Bước 1. `app/build.gradle.kts`.**

Trong `doFirst` của khối `preProduction*Build`, tính danh sách tham số sai **trước** `if (missing.isNotEmpty())` (test đọc khối từ `val missingProductionProperties = mapOf(` tới lần `throw GradleException(` đầu tiên), rồi `throw` sau. Viết sẵn theo giới hạn 120 ký tự của ktlint:

```kotlin
        val missing = ... // unchanged, no MAX entry
        // MAX values are optional (no key: the release runs AdMob only) but checked, never printed:
        // a quote would break the Java string of BuildConfig.
        val malformed = listOfNotNull(
            "maxSdkKey".takeIf {
                providers.gradleProperty("maxSdkKey").orNull
                    ?.let { value -> value.isNotEmpty() && !value.matches(Regex("[A-Za-z0-9_-]+")) } == true
            },
            "maxAdUnits".takeIf {
                providers.gradleProperty("maxAdUnits").orNull
                    ?.let { value -> !value.matches(Regex("[A-Za-z0-9_=;-]*")) } == true
            },
        )
        if (missing.isNotEmpty()) { ... } // unchanged
        if (malformed.isNotEmpty()) {
            throw GradleException(
                "Build production có tham số sai dạng: " +
                    malformed.joinToString { "-P$it" } + " (ADR-0045).",
            )
        }
```

Các chuỗi mà test tìm: `"maxSdkKey".takeIf`, `"maxAdUnits".takeIf`, `Regex("[A-Za-z0-9_-]+")`, `Regex("[A-Za-z0-9_=;-]*")`, `if (malformed.isNotEmpty())`, `"Build production có tham số sai dạng: "`, `malformed.joinToString`, `(ADR-0045).` Mã `ADR-0045` (mục 5.5, ADR còn hiệu lực); không dùng `ADR-0037` đã `superseded`, vì `scripts/new-app.sh` bỏ ADR `superseded` khỏi app mới. Production từ chối thay vì lọc như staging: lọc lặng lẽ là phát hành một bản thiếu MAX mà không ai biết.

Chú thích: đầu `productFlavors` bỏ "(optional until T-073)" ở hai dòng `-PmaxSdkKey`, `-PmaxAdUnits`, ghi "(optional; characters checked)"; flavor `production` thay hai chú thích "Optional until T-073 ... (ADR-0037)" và "Optional until T-073 passes it ..." bằng "Optional: without a key MAX never starts and the release runs AdMob only (ADR-0045, section 5.5); characters are checked by the production pre-build task and scripts/release.sh" và "Optional: a slot without a MAX unit loads from AdMob only"; chú thích trên `val missingProductionProperties` thêm một câu về hai giá trị MAX. Không đổi dòng `buildConfigField` nào.

**Bước 2. Tài liệu.**

- `docs/ops/release.md`:
  - Mục 1.2: hai dòng bảng: "Khóa SDK AppLovin MAX (chỉ khi app dùng MAX)" lấy ở AppLovin, **Account > Keys > SDK Key**; "Mã đơn vị MAX của từng khe (`banner`, `banner_mrec`; thêm `interstitial`, `rewarded`, `app_open` khi app dùng các khe đó)" lấy ở AppLovin, **MAX > Mediation > Manage > Ad Units**. App chưa có tài khoản AppLovin thì bỏ qua hai dòng này.
  - Mục mới 1.5 "Nhóm mediation AdMob khi app dùng MAX" (câu hỏi 45, chuẩn JBase): app có khóa MAX thì mọi nhóm mediation AdMob của app bỏ "AppLovin (Bidding)" và "AppLovin" (waterfall), để AppLovin không đấu giá hai lần với chính nó; MAX gọi Google qua adapter Google của MAX. App chưa dùng MAX thì giữ AppLovin trong nhóm mediation AdMob. Adapter AppLovin của AdMob vẫn có trong build; bỏ adapter là việc riêng. Chữ trong `docs/ops/` không ghi mã ticket hay plan (`scripts/history-refs.py`). Kiểm lại khi thêm nhóm mediation mới hay khi bắt đầu dùng MAX.
  - Mục 2.2: thêm `maxSdkKey=<khóa SDK MAX>` và `maxAdUnits=banner=<mã MAX>;banner_mrec=<mã MAX>` vào khối `release.properties`, đánh dấu tùy chọn; đoạn giải thích: không có khóa thì bản phát hành không chạy MAX, script in CHÚ Ý và dòng "MAX: không dùng"; khóa chỉ chữ, số, `_`, `-`; `maxAdUnits` dạng `<khe>=<mã>;...`, khe thiếu mã chỉ chạy AdMob; script không in khóa hay mã, chỉ in số khe có mã; app không có adapter MAX mà có khóa thì script in CHÚ Ý và không dùng khóa.
  - Mục 2.3, bước 1: thêm "khóa và mã đơn vị MAX (nếu có) đúng ký tự".
- `docs/ops/ads-business-rules.md`: M-09 thêm vế "ở production khóa tùy chọn: không có khóa thì bản phát hành chỉ chạy AdMob (`scripts/release.sh` báo CHÚ Ý); khóa sai ký tự bị từ chối (`preProduction*Build`, `scripts/release.sh`)"; M-10 thêm vế "mã sai ký tự bị từ chối ở production".
- `docs/ops/ads-guide.md`, mục 7: dòng "Khóa SDK MAX" thay "(tùy chọn; `release.sh` chưa đọc và kiểm khóa này)" bằng "(tùy chọn; `scripts/release.sh` đọc từ `release.properties`, không có thì bản phát hành chỉ chạy AdMob, `docs/ops/release.md`)"; dòng "Banner và MREC của MAX" thay "(tùy chọn; `release.sh` chưa đọc và kiểm mã này)" bằng "(tùy chọn; `scripts/release.sh` đọc `maxAdUnits` từ `release.properties` và kiểm ký tự)"; dòng "AppLovin hai vai" đổi "app dùng MAX thì bỏ \"AppLovin (Bidding)\"" thành "app có khóa MAX ở production thì bỏ \"AppLovin (Bidding)\" và \"AppLovin\" (waterfall)" cho khớp `release.md` mục 1.5.
- `docs/features/ads/spec.md`: câu hỏi 43 thêm dòng "**Đổi 2026-11-15** (anh Nhật, plan T-073): ..." theo mục 2; spec chuyển `changed`, mục Lịch sử; chờ anh Nhật xác nhận chữ đầy đủ cùng lời duyệt plan.
- `ProductionPropertiesTest.kt`: KDoc đầu lớp thêm câu về hai giá trị MAX ở production; chú thích "optional until T-073" của các test đổi theo hành vi mới (cùng commit test).
- Ticket T-073: sửa dòng "bắt buộc `maxSdkKey`" theo mục 2.

**Bước 3. `scripts/release.sh`** (lời cho phép ở đầu mục). Chữ trong ngoặc kép là chữ test tìm. Trong script hôm nay, bước 1 kiểm theo thứ tự: `admobAppId`, `adUnits`, `assetsBaseUrl` (https), `uploadKeyAlias`, rồi khóa AppsFlyer (đọc bằng hàm `property`, lỗi bằng `die`).

1. Đọc `max_sdk_key="$(property maxSdkKey)"` và `max_ad_units="$(property maxAdUnits)"` cạnh các dòng `property` có sẵn (hàm có sẵn đọc như chữ, không `source`). Dòng thiếu và giá trị rỗng coi như nhau.
2. Khối MAX đặt **cuối bước 1, ngay sau khối khóa AppsFlyer**, trước `step "Kiểm tra commit phát hành"`:
   - Biết app có MAX như khối AppsFlyer: `grep -rqsE --include='*.gradle.kts' --exclude-dir=build --exclude-dir=.gradle 'libs\.max\.mediation\.|"com\.applovin\.mediation:' .`
   - `maxSdkKey` không rỗng mà không khớp `^[A-Za-z0-9_-]+$`: `die`, dòng lỗi chứa "maxSdkKey sai dạng" (mọi app).
   - `maxAdUnits` không rỗng mà không khớp `^[a-z_]+=[A-Za-z0-9_-]+(;[a-z_]+=[A-Za-z0-9_-]+)*$`: `die`, dòng lỗi chứa "maxAdUnits sai dạng" (mọi app).
   - Hai dòng lỗi trên không in giá trị (khác dòng lỗi `adUnits` có sẵn in lại mục sai).
   - App có MAX, `maxSdkKey` rỗng: `echo` một dòng bắt đầu bằng "CHÚ Ý: " chứa "không có maxSdkKey" (không chứa "appsFlyerDevKey"), chạy tiếp.
   - App không có MAX, `maxSdkKey` không rỗng: dòng "CHÚ Ý: " chứa "maxSdkKey không được dùng" (không chứa "appsFlyerDevKey"); đặt `max_sdk_key=""`.
   - Sau hai ý trên, `max_sdk_key` rỗng mà `maxAdUnits` không rỗng: dòng "CHÚ Ý: " chứa "maxAdUnits không được dùng"; đặt `max_ad_units=""`.
3. Truyền bằng đúng dạng `export ORG_GRADLE_PROJECT_maxSdkKey="$max_sdk_key"` và `export ORG_GRADLE_PROJECT_maxAdUnits="$max_ad_units"` cạnh dòng `export ORG_GRADLE_PROJECT_appsFlyerDevKey` (test hợp đồng đọc dòng `export ORG_GRADLE_PROJECT_<tên>`); `unset` sau build thêm `max_sdk_key max_ad_units`.
4. Phần tóm tắt (cả `--check-only`), dưới dòng `   App:`: một dòng bắt đầu bằng "   MAX: ": "   MAX: có khóa, <n> khe có mã đơn vị" khi có khóa, "   MAX: không dùng (không có maxSdkKey)" khi app có MAX mà không khóa, "   MAX: không có trong app" khi không có adapter. Không in giá trị.
5. Chú thích đầu file: danh sách khóa của `release.properties` thêm hai dòng `maxSdkKey=<AppLovin MAX SDK key>, optional: without it the release runs AdMob only` và `maxAdUnits=<slot>=<MAX ad unit>;..., optional`; danh sách các lần người phát hành cho phép sửa file thêm "2026-11-15: AppLovin MAX key and ad units". Chú thích viết tiếng Anh, không ghi mã ticket hay đường dẫn tài liệu, như các chú thích có sẵn.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/build.gradle.kts` | Bước 1: kiểm ký tự `maxSdkKey`, `maxAdUnits` ở `preProduction*Build`; chú thích |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` | Mục 4: viết lại hai test (mục 3.5), thêm test kiểm ký tự; KDoc, chú thích |
| Sửa | `scripts/tests/run.sh` | Mục 4: `write_release_properties` nhận khóa và mã MAX; test hợp đồng (10 thuộc tính); test `release.sh` với MAX; bản sao `max_copy`; bản sao `--check-only` chép thêm `core/ads/build.gradle.kts` |
| Sửa | `docs/ops/release.md` | Bước 2: mục 1.2, mục mới 1.5, mục 2.2, mục 2.3 |
| Sửa | `docs/ops/ads-business-rules.md` | M-09, M-10 |
| Sửa | `docs/ops/ads-guide.md` | Ba dòng của mục 7 |
| Sửa | `docs/features/ads/spec.md` | Câu hỏi 43 đổi, `changed`, Lịch sử (sửa trong PR của plan, trước khi duyệt) |
| Sửa | `docs/features/ads/tickets/T-073-release-sh-ma-max.md` | Dòng phạm vi về bắt buộc khóa; trạng thái, `plan:` (làm có người, một PR) |
| Sửa | `docs/features/ads/plans/T-073-release-doc-va-kiem-khoa-max.md` | Plan này: trạng thái, nhật ký (làm có người, một PR) |

**Ngoài bảng, có lời cho phép:** `scripts/release.sh`, bước 3 của mục 3.1.

Nơi đọc các file bị sửa (`grep -rn` trong `scripts/ .github/ */src/test`):

- `scripts/tests/run.sh` được gọi từ `scripts/check.sh`, `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/local-setup.sh` (gợi ý chạy), `scripts/main-guard-verified.sh` và `scripts/merge-if-green.sh` (phân loại file là code hay tài liệu): plan chỉ thêm test trong file, không đổi tên, đối số hay mã thoát, nên các nơi gọi không đổi.
- `docs/ops/ads-business-rules.md` và `docs/ops/ads-guide.md` được nhắc trong chú thích của các test `:core:ads`, `:core:analytics` (`core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`): chỉ dẫn mã luật trong chú thích (`grep` mọi dòng nhắc hai file đều là chú thích), không đọc file; plan sửa M-09, M-10 và ba dòng của mục 7, không đổi mã luật nào.
- `scripts/release.sh` được nhắc trong `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementSourceTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdPlacementTest.kt` (chép cách đọc vị trí quảng cáo), `scripts/status.py`, `scripts/run-check.py`, `scripts/plan-queue.py` (chú thích): bước 3 không đổi cách đọc `AdPlacement.kt`.
- `docs/ops/release.md` được `scripts/status.py` dẫn "mục 1.2": plan thêm dòng vào bảng của mục 1.2, không đổi số mục.
- `app/build.gradle.kts` được `scripts/ui-test-verdict.sh`, `scripts/setup-check.sh`, `scripts/new-app.sh` đọc (`applicationId`, `ASSETS_BASE_URL`, địa chỉ R2 staging) và `ProductionPropertiesTest`, `scripts/tests/run.sh` đọc (trong bảng): plan không đổi các dòng mà script đọc. `pr-check.yml` build bản release khi file build đổi: flavor staging không đổi.
- `docs/features/ads/spec.md`: `scripts/run-check.py`, `scripts/plan-queue.py`, `scripts/status.py` đọc trạng thái spec; spec `changed` tới khi anh Nhật xác nhận (cùng lời duyệt plan).
- Tài liệu quy tắc: `scripts/history-refs.py` quét (không ghi mã ticket, phiên); `scripts/ads-plan.py` không đọc ba tài liệu ở `docs/ops/` này.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: không thêm, bớt hay đổi vị trí, định dạng; đổi cách cấu hình MAX của bản production (khóa, mã đơn vị)
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm (`max-mediation-google`, `applovin-sdk` đã có ở `:core:ads` từ T-068)

### 3.4. Phương án dự phòng

Không có. Bước 1 đọc tham số như các dòng có sẵn; bước 3 theo mẫu khóa AppsFlyer của cùng script.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` (2026-11-15) báo "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)": plan chỉ sửa `app/build.gradle.kts` (Kotlin script). Test có sẵn đọc file build là `ProductionPropertiesTest`; test của `release.sh` nằm trong `scripts/tests/run.sh`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Production đọc `-PmaxSdkKey`, không bắt buộc, không kiểm; khối kiểm không nhắc MAX | Vẫn không bắt buộc; sai ký tự thì từ chối (bước 1) | `ProductionPropertiesTest.maxSdkKey_isOptionalUntilReleaseShPassesIt`: `listOf("maxSdkKey", "com.applovin", "ADR-0037").forEach { assertFalse("$it in:\n$check", it in check) }` | Viết lại (ADR-0031), đổi tên `maxSdkKey_isOptional_charactersChecked`. Câu cũ: ba câu `assertFalse` trên. Câu của plan nói điều mới: "kiểm ký tự `maxSdkKey`, `maxAdUnits` ở `preProduction*Build`" (mục 3.1, bước 1). Câu ý định: khóa vẫn tùy chọn, production đọc khóa từ `-PmaxSdkKey`. Câu mới: `listOf("\"maxSdkKey\" to", "com.applovin", "ADR-0037").forEach { assertFalse("$it in:\n$check", it in check) }` (chỉ phần tử đầu đổi chữ: từ `maxSdkKey` thành `"maxSdkKey" to`; `com.applovin`, `ADR-0037` giữ nguyên) và thêm `assertTrue(check, "\"maxSdkKey\".takeIf" in check)`; hai câu về khối production giữ nguyên. Không nới: mọi cách làm khóa thành bắt buộc theo mẫu của file đều viết `"maxSdkKey" to`. Đối chứng: lệnh 1 ở mục 4 |
| Production nhận `-PmaxAdUnits`, không kiểm; khối kiểm không nhắc `maxAdUnits` | Vẫn tùy chọn; sai ký tự thì từ chối (bước 1) | `ProductionPropertiesTest.maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional`: `assertFalse(check, "maxAdUnits" in check)` | Viết lại câu đó (ADR-0031), giữ tên test và mọi câu khác. Câu của plan nói điều mới: như trên. Câu ý định: `maxAdUnits` tùy chọn. Câu mới: `assertFalse(check, "\"maxAdUnits\" to" in check)` và `assertTrue(check, "\"maxAdUnits\".takeIf" in check)`. Đối chứng: lệnh 2 ở mục 4 |
| Test hợp đồng: `release.sh` truyền 8 thuộc tính | 10 thuộc tính (thêm `maxSdkKey`, `maxAdUnits`); số bắt buộc giữ 4 (3 khi không có AppsFlyer) | `scripts/tests/run.sh`: `expect "contract: ... every Gradle property that release.sh exports" "8 none"` | Cập nhật dữ liệu đếm (ADR-0029): `"10 none"`, và chú thích "release.sh exports 8 properties today" thành 10. Chiều thứ hai (bắt buộc thì truyền) không đổi |
| `write_release_properties` ghi 5 dòng | Thêm `maxSdkKey=${5-FAKE_MAX_KEY_0123456789}` và `maxAdUnits=${6-}` | Mọi test `release.sh` dùng hàm này | Cập nhật dữ liệu (ADR-0029). Khóa giả hợp lệ, nên test cũ đi tới bước sau như trước. Ở bản sao không có MAX (`no_af`, `own_af`), khóa mặc định làm script in thêm một dòng "CHÚ Ý: ... maxSdkKey không được dùng"; câu có sẵn đếm `^CHÚ Ý: .*appsFlyerDevKey` vẫn 1 vì dòng đó không chứa chữ ấy |

Số câu viết lại: hai (phần tử `maxSdkKey` của `forEach` và câu `maxAdUnits`), dưới trần 3 của ADR-0031; không bớt câu nào.

## 4. Kiểm thử

Mọi test dưới đây viết ở commit 1, trước bước 1 tới 3. Test của `release.sh` chạy trên bản sao của script như các test có sẵn. Khóa và mã giả có chữ `FAKE` viết hoa (chỗ cho phép của `scripts/secret-scan.py`), không 86 ký tự: `FAKE_MAX_KEY_0123456789`, `FAKE_MAX_SECRET_0123456789`, mã `FAKE_UNIT_A`. `write_release_properties` nhận đối số theo vị trí: muốn đặt đối số 5 hay 6 thì truyền rõ đối số 4 bằng khóa AppsFlyer đúng dạng `AbCdEf0123456789XyZ` (rỗng thì repo có AppsFlyer dừng ở "thiếu appsFlyerDevKey" trước khối MAX), và đối số 5 bằng `FAKE_MAX_KEY_0123456789` khi chỉ thử đối số 6.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu hỏi 43: khóa tùy chọn ở production, sai ký tự bị từ chối | Unit test `:app` | `ProductionPropertiesTest.maxSdkKey_isOptional_charactersChecked` (mục 3.5). Thất bại trước bước 1 vì `check` chưa có `"maxSdkKey".takeIf` |
| Mã đơn vị tùy chọn, sai ký tự bị từ chối | Unit test `:app` | `ProductionPropertiesTest.maxAdUnits_stagingReadsLocalPropertiesFiltered_productionOptional`, câu viết lại. Thất bại trước bước 1 vì `check` chưa có `"maxAdUnits".takeIf` |
| Production từ chối mà không in giá trị | Unit test `:app` | `ProductionPropertiesTest.productionMaxValues_wrongCharactersRefused_valueNotPrinted`: (1) `check` chứa `Regex("[A-Za-z0-9_-]+")` và `Regex("[A-Za-z0-9_=;-]*")`; (2) hai dấu mốc có thật: `assertTrue("if (malformed.isNotEmpty())" in build)` và `val afterRefusal = build.substringAfter("if (malformed.isNotEmpty())")`, `assertTrue("(ADR-0045)." in afterRefusal)`; (3) `val refusal = afterRefusal.substringBefore("(ADR-0045).")` chứa `"Build production có tham số sai dạng: "` và `malformed.joinToString`; (4) mọi chỗ nội suy trong `refusal` (`Regex("\\$\\{?([A-Za-z_]+)")`) chỉ là `it`, và `refusal` không chứa `gradleProperty` (không in giá trị theo cách khác); (5) đối chứng trong test: `check` có `if (missing.isNotEmpty())`. Thất bại trước bước 1 từ (1): `check` chưa có hai `Regex` |
| Staging không đổi (KB-31, tiêu chí 8) | Unit test `:app` | Giữ nguyên `stagingMaxKey_readsLocalPropertiesOnly`, `stagingMaxKey_isFilteredBeforeBuildConfig`, `stagingKey_isFixedEmpty`, `appsFlyerDevKey_isRequiredForProductionWithTheSdk`; qua ở mọi commit |
| Hợp đồng L-125 | Test script | `"10 none"` (mục 3.5); đỏ tới khi `release.sh` có cả hai `export` |
| App có MAX, không khóa: chạy tiếp, CHÚ Ý | Test script | Khối MAX ở cuối bước 1 nên lần chạy phải qua `assetsBaseUrl` (dùng `https://cdn.example.vn/`) và đi tới bước commit; vì vậy chạy trên bản sao không có origin, như `no_af`: bản sao `max_copy` dựng từ `git ls-files '*.gradle.kts'` và `scripts/release.sh` như `own_af`, `git init`. Biến `repo_has_max` tính như `repo_has_appsflyer`, mẫu `libs\.max\.mediation\.|"com\.applovin\.mediation:`; repo không có MAX (app bỏ MAX) thì thêm vào bản sao module probe `implementation(libs.max.mediation.google)`. Trên `max_copy`: `write_release_properties "<mã app>" "" "https://cdn.example.vn/" "AbCdEf0123456789XyZ" ""`: `expect ... "1 1"`: một dòng `^CHÚ Ý: .*không có maxSdkKey`, một dòng `^== Kiểm tra commit phát hành`. Nhánh `repo_has_max=no` còn chạy trên bản sao **không** probe: không dòng CHÚ Ý "không có maxSdkKey" (tương ứng câu "0 1" của AppsFlyer). Thất bại trước bước 3 vì script cũ không in dòng CHÚ Ý |
| Khóa sai dạng bị từ chối, không lộ giá trị | Test script | Trên repo thật, `https://cdn.example.vn/` (dừng ở khối MAX, trước bước commit): `release_fails_with "MAX SDK key with odd characters" "maxSdkKey sai dạng"` với đối số 5 `"FAKE_MAX_SECRET_0123456789 x"`; cùng lần chạy đếm `FAKE_MAX_SECRET_0123456789` trong đầu ra: 0 |
| Mã đơn vị sai dạng bị từ chối, không lộ giá trị | Test script | Như trên: `release_fails_with "MAX ad units malformed" "maxAdUnits sai dạng"` với đối số 5 `FAKE_MAX_KEY_0123456789`, đối số 6 `"banner"` (thiếu `=`), và một lần với `"banner=FAKE_UNIT_SECRET\"X"`; lần sau đếm `FAKE_UNIT_SECRET` trong đầu ra: 0 |
| Không in khóa, mã MAX khi hợp lệ | Test script | Trên `max_copy`, `https://`: khóa `FAKE_MAX_SECRET_0123456789`, `maxAdUnits=banner=FAKE_UNIT_A`: `expect "release.sh never prints the MAX SDK key or units" "1 0 0"`: một dòng `^== Kiểm tra commit phát hành` (lần chạy đã qua khối MAX) và hai chuỗi giả |
| Mã đơn vị khi không có khóa | Test script | Trên `max_copy`: đối số 5 `""`, đối số 6 `banner=FAKE_UNIT_A`: một dòng `^CHÚ Ý: .*maxAdUnits không được dùng`, tới bước commit |
| App không có MAX: khóa không dùng | Test script | Bản sao `no_af` (không module nào khai báo MAX): đối số 5 `""` thì `"1 0"` (tới `^== Kiểm tra commit phát hành`, không dòng CHÚ Ý `maxSdkKey`); đối số 5 `FAKE_MAX_SECRET_0123456789` thì `"1 1 0"` (tới bước commit, một dòng `^CHÚ Ý: .*maxSdkKey không được dùng`, không in khóa) |
| `--check-only` có dòng MAX | Test script | Bản sao `--check-only` chép thêm `core/ads/build.gradle.kts`: với khóa giả mặc định, `expect "release.sh --check-only: prints the MAX line without values" "1 0"` (dòng `^   MAX: có khóa, 0 khe có mã đơn vị$`, chuỗi khóa giả); đối số 5 `""` thì một dòng `^   MAX: không dùng \(không có maxSdkKey\)$`. Câu chính có sẵn ("1 1 0 0") giữ nguyên |
| Câu hỏi 45: hướng dẫn nhóm mediation | Kiểm tra thủ công (người: đọc tài liệu) | Anh Nhật đọc mục 1.5 mới của `docs/ops/release.md` khi duyệt PR |
| Bản production thật | Kiểm tra thủ công (máy thật: khác, cần thư mục phát hành và khóa thật, chỉ người phát hành có) | Anh Nhật chạy `scripts/release.sh --check-only` với thư mục phát hành thật: không có `maxSdkKey` thì có CHÚ Ý và dòng "MAX: không dùng"; có khóa thật thì dòng "MAX: có khóa, n khe". Lần phát hành đầu có khóa: log thẻ `Ads` có `MAX started` rồi `MAX ready`; không khóa: `MAX skipped: no SDK key` |

Đối chứng cho hai câu viết lại (sau khi test đã xanh ở bước 1; dùng `perl`, trả về bằng `git checkout`):

```sh
# 1. maxSdkKey vẫn tùy chọn
perl -pi -e 's/\Q"assetsBaseUrl" to "ADR-0015",\E/"assetsBaseUrl" to "ADR-0015",\n    "maxSdkKey" to "ADR-0045",/' app/build.gradle.kts
git diff --stat -- app/build.gradle.kts   # phải có thay đổi
./gradlew :app:testStagingDebugUnitTest --tests '*ProductionPropertiesTest*' --rerun   # maxSdkKey_isOptional_charactersChecked đỏ
git checkout -- app/build.gradle.kts
# 2. maxAdUnits vẫn tùy chọn
perl -pi -e 's/\Q"assetsBaseUrl" to "ADR-0015",\E/"assetsBaseUrl" to "ADR-0015",\n    "maxAdUnits" to "ADR-0045",/' app/build.gradle.kts
git diff --stat -- app/build.gradle.kts
./gradlew :app:testStagingDebugUnitTest --tests '*ProductionPropertiesTest*' --rerun   # maxAdUnits_..._productionOptional đỏ
git checkout -- app/build.gradle.kts
```

**Ngoại lệ viết test trước:**

- Tài liệu (`release.md`, M-09, M-10, `ads-guide.md`, spec) và chú thích: chỉ chữ.
- Phần Gradle chỉ được kiểm bằng test đọc file: production không build trên CI (ADR-0007), như T-033, T-068. Hành vi thật kiểm ở kiểm thủ công cuối bảng.
- Qua ngay trên script cũ (script cũ không đọc dòng MAX): "không in khóa, mã MAX khi hợp lệ" (`1 0 0`), lần `no_af` với khóa rỗng (`1 0`), và lần chạy bản sao không probe ở nhánh `repo_has_max=no`. Chúng giữ hành vi sau khi sửa, không thất bại trước được. Lần `no_af` có khóa (`1 1 0`) đỏ trước nhờ dòng CHÚ Ý; hai test "sai dạng" đỏ trước vì script cũ không từ chối.

### 4.1. Chạy thử

Không cần (E dưới 2): lặp lại mẫu đọc tham số, mẫu khóa AppsFlyer của `release.sh` và mẫu test có sẵn.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (cách bản production cấu hình nền tảng quảng cáo MAX: khóa, mã đơn vị), 7 (cấu hình build `app/build.gradle.kts`, test của CI `scripts/tests/run.sh`, script phát hành). Mức Cao, duyệt chéo bắt buộc.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | `:app` (file build, test), script phát hành, test script, ba tài liệu, spec |
| B. Độ rõ của yêu cầu | 2 | Câu hỏi 45 đã chốt; dòng đổi của câu hỏi 43 còn chờ xác nhận (spec `changed`); chữ thông báo do plan đặt |
| C. Khả năng kiểm chứng tự động | 2 | `release.sh` và hợp đồng có test tự động; phần Gradle chỉ kiểm bằng đọc file, hành vi thật chỉ thấy khi người phát hành chạy với cấu hình thật |
| D. Ảnh hưởng chéo | 2 | Mọi app tạo từ dự án mẫu nhận script mới khi đồng bộ; không app nào phải đổi thư mục phát hành (khóa tùy chọn). T-069, T-070, T-072 chờ ticket này |
| E. Độ mới của kỹ thuật | 0 | Đúng mẫu có sẵn |
| **Tổng** | 8 | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `app/build.gradle.kts` (khối kiểm production), `scripts/release.sh`, `scripts/tests/run.sh`, `docs/ops/release.md`.
- App đã tạo từ dự án mẫu (camera-pose, Photune): lần đồng bộ sau đưa thay đổi về; thư mục phát hành không có khóa MAX vẫn phát hành được (chỉ thêm CHÚ Ý). PR ghi điều này cho người đồng bộ.
- Task khác: T-069, T-070 thêm khe `interstitial`, `rewarded`, `app_open` vào `maxAdUnits`: regex của bước 1 và 3 đã nhận tên khe chữ thường có `_`. T-085 (backlog) ghi "Lọc ký tự của `-PmaxAdUnits` ở production (T-073)": plan này làm phần đó. Issue #498 không chạm file của plan.
- Đề xuất thứ tự chạy: trước T-069. Không cùng lúc với plan khác sửa `app/build.gradle.kts` hay `scripts/tests/run.sh`.

## 7. Điều kiện dừng riêng

- Cần đổi `buildConfigField` của flavor staging, hay cách staging đọc `local.properties` (KB-31 tiêu chí 8).
- Cần sửa phần nào của `scripts/release.sh` ngoài bước 3 của mục 3.1, hay cần đọc thư mục phát hành.

## 8. Câu hỏi cho người duyệt

1. **App chưa có tài khoản AppLovin.** Trả lời (anh Nhật, 2026-11-15): "bỏ qua, không dùng MAX". Plan làm khóa tùy chọn ở production; không khóa thì bản phát hành chỉ chạy AdMob, `release.sh` báo CHÚ Ý và dòng "MAX: không dùng". Spec câu hỏi 43 đổi theo, chờ anh Nhật xác nhận cùng lời duyệt plan.
2. **Chữ thông báo của `release.sh`.** Test tìm "maxSdkKey sai dạng", "maxAdUnits sai dạng", "CHÚ Ý: ...không có maxSdkKey", "CHÚ Ý: ...maxSdkKey không được dùng" và các dòng "   MAX: ..." của mục 3.1 bước 3. Anh Nhật muốn chữ khác thì nói khi duyệt.

3. **Phạm vi lời cho phép sửa `scripts/release.sh`.** Lời gốc "lần này" nói trong phiên soạn plan; plan hiểu là áp cho việc thực hiện T-073 theo plan này ở bất kỳ phiên nào, chỉ phần bước 3 của mục 3.1, và không áp cho task khác. Anh Nhật xác nhận cách hiểu này cùng lời duyệt.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
