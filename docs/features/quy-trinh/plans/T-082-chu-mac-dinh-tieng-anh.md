---
ticket: T-082
title: Chữ mặc định của các module mẫu là tiếng Anh, tiếng Việt ở values-vi/
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:
risk_level: high
token_estimate: 21.1M
blocking_points: [core]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-082: CHỮ MẶC ĐỊNH CỦA CÁC MODULE MẪU LÀ TIẾNG ANH, TIẾNG VIỆT Ở VALUES-VI/

## 1. Tóm tắt

Mười hai chuỗi còn tiếng Việt trong `res/values/` của module mẫu (`:feature:loading` 7, `:core:designsystem` 1, `:app` 4) chuyển sang tiếng Anh; bản tiếng Việt chuyển nguyên văn sang `res/values-vi/` của từng module; `:app` bỏ `values-en/`. Không đổi dòng Kotlin nào ngoài thư mục test. Screenshot test cùng tên ảnh thiết kế đặt ngôn ngữ `vi` (ngôn ngữ của thiết kế) nên giữ nguyên ảnh; 6 ảnh chuẩn khác đổi sang chữ tiếng Anh (đã chạy thử, mục 4.1). Test mới kiểm cả hai chiều: máy để ngôn ngữ khác tiếng Việt hiện tiếng Anh, máy để tiếng Việt hiện tiếng Việt như trước.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ riêng. Nguồn là issue #288 (Photune) và bài học L-135 (`accepted`). Anh Nhật giao viết plan ngày 2026-11-10 ("Xử lý các issue theo đề xuất của AI", trong đó có "viết plan T-082"). Ticket ghi `auto_run: false`: làm trong phiên có người, một PR cho cả task (mục 3.2 có bài học, plan, ticket). Phụ thuộc T-081 (Robolectric 4.17) đã `on-staging`, nên lần đổi ảnh chuẩn này chỉ có một nguyên nhân.

Đếm lại trên nhánh chính `e078e7a` (mọi tệp `res/values*/strings.xml`, kể cả bản staging):

| Tệp | Chuỗi | Ngôn ngữ hiện nay | Plan |
| --- | --- | --- | --- |
| `feature/loading/src/main/res/values/strings.xml` | 8: `loading_status`, `loading_guidance`, `loading_status_failed`, `loading_error_title`, `loading_percent`, `loading_error`, `loading_retry`, `loading_footer` | 7 tiếng Việt; `loading_percent` (`%1$d%%`) không theo ngôn ngữ | 7 chuỗi sang tiếng Anh; `loading_percent` thêm `translatable="false"` |
| `core/designsystem/src/main/res/values/strings.xml` | 1: `brand_tagline` | tiếng Việt | sang tiếng Anh |
| `app/src/main/res/values/strings.xml` | 5: `app_name`, `network_lost_title`, `network_lost_message`, `network_lost_retry`, `network_lost_still_offline` | `app_name` "App Template" không theo ngôn ngữ; 4 chuỗi popup tiếng Việt | 4 chuỗi lấy bản tiếng Anh của `values-en/`; `app_name` giữ nguyên dòng |
| `app/src/main/res/values-en/strings.xml` | 4 chuỗi popup, tiếng Anh | tiếng Anh | xóa tệp (nội dung đã vào `values/`) |
| `app/src/staging/res/values/strings.xml` | 58 chuỗi `ad_gallery_*`, đều `translatable="false"` | tiếng Anh | không đổi |
| `core/ads/src/main/res/values/strings.xml` | 9 chuỗi `ads_*` | tiếng Anh | không đổi (ngoài phạm vi ticket; chỗ lệch với spec `ads` KB-19 ở mục 8, câu 2) |
| `feature/sample/src/main/res/values/strings.xml` | 5 chuỗi `sample_*` | tiếng Anh | không đổi |

Tổng: 12 chuỗi tiếng Việt, khớp số của L-135 (issue #288 ghi `:core:ads` và `:feature:sample` là tiếng Việt, không đúng). Không còn chữ tiếng Việt nào khác trong `src/main` của module mẫu: tìm mọi tệp `.kt`, `.xml`, `.kts` có chữ cái riêng của tiếng Việt trong `app`, `core`, `feature`, `build-logic`, chỉ thấy ba tệp trên, hai tệp test (dưới), `app/build.gradle.kts` (thông báo lỗi cho nhóm khi build production, không hiện cho người dùng), `RemoteConfigKeysTest.kt` (sinh tài liệu tiếng Việt cho nhóm) và chú thích của một kịch bản emulator.

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| `res/values/` tiếng Anh, `values-vi/` có bản tiếng Việt, `:app` không còn `values-en/` | Mục 1 của định nghĩa hoàn thành | Bảng trên. Bản tiếng Việt chép nguyên văn (không sửa chữ); bản tiếng Anh: popup lấy nguyên `values-en/`, 8 chuỗi còn lại là chữ mới, anh Nhật xác nhận (mục 8, câu 3) |
| Máy để tiếng Việt vẫn hiện tiếng Việt | Test đặt ngôn ngữ `vi` | Test Robolectric `@Config(qualifiers = "vi")` dựng màn hình thật và so chữ tiếng Việt (mục 4). Kèm test ngôn ngữ khác (`fr`), vì ngôn ngữ mặc định của Robolectric là `en-US` và `:app` hôm nay đã có `values-en/`: chỉ ngôn ngữ không có tệp riêng mới chứng minh `values/` là tiếng Anh |
| `check.sh` qua với ảnh chuẩn mới, người xem từng ảnh | Danh sách ảnh và mức lệch trong plan | Mục 4, mục 4.1: 6 ảnh đổi, đo trên máy dev |
| Không nới câu khẳng định | ADR-0031 | Không câu khẳng định nào phải viết lại (mục 3.5): `NetworkDialogTest` nhận chữ qua tham số, không qua tài nguyên; `NetworkDialogHostTest` đã ghim `vi` và `en` |
| Spec `loading` cập nhật, Nhật xác nhận chữ | Spec `confirmed` | Plan đề xuất câu thêm vào spec (mục 3.1, bước 5); AI sửa spec trong PR của task chỉ sau khi anh Nhật xác nhận chữ (mục 8, câu 3) |
| `CLAUDE.md`, mục Quy ước code | Quy tắc mới | Mục 3.1, bước 6 |
| `SCENARIO_LANGUAGE`, `docs/ops/test-sessions.md` | Kiểm lại | Không sửa: hai chỗ đã nói `res/values/` là tiếng Anh, sau task điều đó mới đúng ở mọi module (mục 3.1, bước 8) |

Thiết kế tham chiếu: `docs/features/loading/design/README.md` (4 ảnh `LoadingScreen_*` trong `design/screens/`), `docs/features/network/design/README.md` (2 ảnh `NetworkLostScreen_default_*`). Ảnh và `context/` do `scripts/figma-export.py` sinh từ Figma, chữ tiếng Việt; plan không đổi ảnh, `context/` hay Figma (docs/ops/design-workflow.md).

ADR liên quan:
- ADR-0008: hệ thống thiết kế; `brand_tagline` là chữ thương hiệu, mỗi app thay.
- ADR-0020: screenshot test cùng tên ảnh thiết kế; test kiểm chữ theo bảng chữ của thiết kế. Thiết kế giữ tiếng Việt, nên các test này đặt `vi` (mục 3.1, bước 3).
- ADR-0029, ADR-0031: test cũ; plan không viết lại câu khẳng định nào.
- ADR-0044: `CLAUDE.md` và `docs/ops/to-app.md` là tài liệu quy tắc, không ghi mã ticket hay bài học.

## 3. Giải pháp

### 3.1. Cách làm

Bước 1, tài nguyên (commit code của bước 4, `auto-run.md`):

- `feature/loading/src/main/res/values/strings.xml`:

  | Tên | Tiếng Anh (`values/`) | Tiếng Việt (`values-vi/`, nguyên văn hiện nay) |
  | --- | --- | --- |
  | `loading_status` | Loading data | Đang tải dữ liệu |
  | `loading_guidance` | Preparing your space… | Đang chuẩn bị không gian của bạn… |
  | `loading_status_failed` | Progress stopped | Tiến độ đã dừng |
  | `loading_error_title` | Unable to load data | Không thể tải dữ liệu |
  | `loading_error` | The connection was interrupted. Check your network and try again to continue. | Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục. |
  | `loading_retry` | Try again | Thử lại |
  | `loading_footer` | A private space for your work | Không gian riêng cho việc của bạn |
  | `loading_percent` | `%1$d%%`, thêm `translatable="false"` | không có (không theo ngôn ngữ) |

  Chuỗi đứng cạnh con số (docs/CLAUDE.md, ý "Plan dịch chữ hiển thị"): `loading_status` và `loading_status_failed` là nhãn ở đầu trái của hàng có phần trăm (`loading_percent`, 0% tới 100%, 100% là chuẩn bị xong). "Loading data" cạnh "68%" nghĩa là đã tải được 68%; "Progress stopped" cạnh "68%" nghĩa là dừng ở mức đã đạt 68%, đúng nghĩa bản tiếng Việt ("Đang tải dữ liệu", "Tiến độ đã dừng"). Không chuỗi nào đổi nghĩa con số (không có từ ngược nghĩa kiểu "còn lại"), nên không thêm test chặn từ ngược nghĩa; test của mục 4 so nguyên văn từng chữ.

  `loading_guidance` giữ dấu ba chấm một ký tự `…` (U+2026) như bảng chữ của thiết kế. `loading_error_title` tránh dấu nháy đơn (`Couldn't` phải thoát `\'` trong XML). `translatable="false"` của `loading_percent` là bắt buộc: không có nó, `:app:lintStagingDebug` (`checkDependencies = true`) dừng với `"loading_percent" is not translated in "vi" (Vietnamese) [MissingTranslation]` (đã đo, mục 4.1).
- `feature/loading/src/main/res/values-vi/strings.xml` (tạo): 7 chuỗi tiếng Việt của bảng trên.
- `core/designsystem/src/main/res/values/strings.xml`: `brand_tagline` thành "Everything in rhythm." (chữ đề xuất, mục 8, câu 3), giữ dòng chú thích hiện có. `core/designsystem/src/main/res/values-vi/strings.xml` (tạo): "Mọi việc vào đúng nhịp."
- `app/src/main/res/values/strings.xml`: 4 chuỗi popup lấy nguyên văn bản tiếng Anh của `values-en/` ("No internet connection", "This app needs the Internet to work. Check Wi-Fi or mobile data, then try again.", "Try again", "Still no connection. Please try again in a moment."). Dòng `<string name="app_name">App Template</string>` giữ đúng từng ký tự, vì `scripts/new-app.sh` thay nguyên dòng đó. Thẻ `<resources>` bỏ `tools:locale="vi"`, giữ `tools:ignore="MissingTranslation"`: `app_name` không có bản tiếng Việt (không cần) mà không đặt `translatable="false"` trên dòng đó được, vì `new-app.sh` thay nguyên dòng. Chú thích đầu tệp viết lại (tiếng Anh): "English is the default language; values-vi holds Vietnamese. MissingTranslation is ignored for this whole file, because app_name has no Vietnamese entry and the attribute cannot go on its line: scripts/new-app.sh rewrites that exact line. Every other string here must still get its values-vi entry."
- `app/src/main/res/values-vi/strings.xml` (tạo): 4 chuỗi popup tiếng Việt nguyên văn hiện nay.
- `app/src/main/res/values-en/strings.xml`: xóa (`git rm`), thư mục `values-en/` không còn tệp nào.

Bước 2, test viết trước (commit test của bước 3, chạy trên tài nguyên cũ): mục 4, ba lớp test mới và một test thêm vào `NetworkDialogHostTest`. Không thêm, không xóa câu khẳng định của test có sẵn.

Bước 3, ngôn ngữ của screenshot test (cùng commit test, vì ảnh chuẩn đi theo):

- Quy tắc: ảnh có ảnh thiết kế cùng tên trong `design/screens/` chạy ngôn ngữ của thiết kế (`vi`), để `scripts/design-compare.sh` so cùng chữ và test chữ khớp bảng chữ (ADR-0020); ảnh không có ảnh thiết kế chạy ngôn ngữ mặc định của Robolectric (`en-US`, nay ra chữ tiếng Anh).
- `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenScreenshotTest.kt`: `@Config` của lớp từ `"w411dp-h891dp-xxhdpi"` thành `"vi-w411dp-h891dp-xxhdpi"` (4 ảnh thiết kế, không đổi); `SHORT_SCREEN` giữ `"w360dp-h640dp-xxhdpi"` (2 ảnh `_small`, sang tiếng Anh). KDoc thêm câu: "Tests named after a design image pin `vi`, the language of the design; the `_small` tests run in the default language (English)."
- `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostScreenScreenshotTest.kt`: `@Config` của lớp từ `"vi-w411dp-h891dp-xxhdpi"` thành `"w411dp-h891dp-xxhdpi"`; `default_light`, `default_dark` thêm `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")` (2 ảnh thiết kế, không đổi); `stillOffline_small` từ `"vi-w360dp-h640dp-xxhdpi"` thành `"w360dp-h640dp-xxhdpi"`; `stillOffline_light` theo lớp. Câu KDoc "Vietnamese is the default language of the app, so the frames pin the `vi` locale (Robolectric starts in English)." thay bằng "Frames named after a design image (`default_*`) pin `vi`, the language of the design; the other frames run in the default language (English)."
- `DesignSystemCatalogScreenshotTest` không sửa: không ảnh nào của nó có ảnh thiết kế; `DesignSystem_brand_*` đổi theo `brand_tagline`.
- Ghi ảnh (sau commit tài nguyên, bước 4 của `auto-run.md`): `./gradlew :feature:loading:recordRoborazziDebug :feature:loading:testDebugUnitTest :core:designsystem:recordRoborazziDebug :core:designsystem:testDebugUnitTest :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/*'`. Chỉ giữ 6 ảnh của mục 3.2; `DesignSystem_spacing_components_light.png` đổi vì nhiễu vẽ (lệch tối đa 1 mỗi kênh, mục 4.1) thì trả bằng `git checkout -- core/designsystem/src/test/screenshots/DesignSystem_spacing_components_light.png`, cùng mọi ảnh khác ngoài danh sách.

Bước 4, `NetworkDialogTest` của `:core:designsystem` không sửa: nó truyền chữ của bảng chữ thiết kế `network` vào `NetworkDialogScreen` qua tham số (hằng `TITLE`, `MESSAGE`, `RETRY`, `STILL_OFFLINE`), không đọc tài nguyên nào, nên không phụ thuộc ngôn ngữ. Chữ tiếng Việt viết cứng ở đó là dữ liệu của bảng chữ (thiết kế vẫn tiếng Việt), không phải chữ mặc định của app.

Bước 5, tài liệu thiết kế và spec (sau khi anh Nhật xác nhận chữ ở mục 8, câu 3; trong PR của task):

- `docs/features/loading/design/README.md`, mục 2: thêm cột "Chữ tiếng Anh (`values/`)" vào bảng chữ với chữ của bước 1 (tên app và phần trăm ghi "như cột Chữ"), và một câu dưới bảng: "Ảnh trong `screens/` và screenshot test cùng tên dùng chữ tiếng Việt (test đặt ngôn ngữ `vi`). Máy để ngôn ngữ khác tiếng Việt hiện chữ tiếng Anh." Không sửa `context/`, `screens/`, `review/` (do script sinh) hay các mục khác. Lệch với ticket: ticket ghi "bảng chữ của thiết kế `loading`, `network` (`design/context/`)", nhưng `context/` do `scripts/figma-export.py` sinh từ Figma và không sửa tay (`docs/ops/design-workflow.md`; đầu mỗi tệp `context/` ghi "Không sửa tay"); bảng chữ người đọc nằm ở mục 2 của `README.md`, nên plan ghi chữ hai ngôn ngữ ở đó.
- `docs/features/network/design/README.md`, mục 2: câu "Bản tiếng Anh tương ứng..." đã có; thêm câu "Ảnh trong `screens/` và screenshot test cùng tên dùng chữ tiếng Việt (test đặt ngôn ngữ `vi`); máy để ngôn ngữ khác tiếng Việt hiện bản tiếng Anh, và dòng báo vẫn chưa có mạng là "Still no connection. Please try again in a moment."".
- `docs/features/loading/spec.md` (`confirmed`): AI không sửa trước khi có lời xác nhận. Đề xuất thêm vào cuối mục 4 (Giao diện) một ý: "**Ngôn ngữ của chữ:** chữ trong ngoặc kép ở spec này là bản tiếng Việt, hiện khi máy để tiếng Việt. Máy để ngôn ngữ khác hiện bản tiếng Anh: "Loading data" (Đang tải dữ liệu), "Preparing your space…", "Progress stopped" (Tiến độ đã dừng), "Unable to load data" (Không thể tải dữ liệu), "Try again" (Thử lại), "A private space for your work", khẩu hiệu mẫu "Everything in rhythm."". Tiêu chí của KB-1 tới KB-4 giữ nguyên chữ. Mục 10 thêm dòng lịch sử; spec chuyển `changed`, anh Nhật xác nhận thì AI ghi lại `confirmed` kèm lời xác nhận (docs/CLAUDE.md, mục Khi sinh spec từ yêu cầu).
- `docs/features/ads/spec.md` (`confirmed`), KB-19 tiêu chí 1: theo câu trả lời của mục 8, câu 2. Với phương án đề xuất (a), đề xuất câu mới (AI sửa chỉ sau khi anh Nhật xác nhận, cùng cách như spec `loading`): "Khi người vận hành bật vị trí rewarded của hành động đó thì hộp thoại hỏi xem quảng cáo hiện, có nút xem quảng cáo và nút từ chối (chữ của khung: "Watch ad", "No, thanks"; app dịch theo hướng dẫn quảng cáo)." Câu cũ gạch đi, ghi ngày đổi như các tiêu chí đã đổi khác của spec này.
- Spec `network` không sửa: tiêu chí chỉ nêu nút "Thử lại" như một nhãn, câu hỏi 3 của spec đã ghi bản tiếng Anh tương ứng.

Bước 6, tài liệu quy tắc (không ghi mã ticket, bài học; chạy `scripts/history-refs.py`):

- `CLAUDE.md`, mục Quy ước code, sau dòng "Chữ hiển thị cho người dùng đặt trong `res/values/strings.xml`...": thêm "Chữ trong `res/values/` của module mẫu là tiếng Anh; bản tiếng Việt cùng tên ở `res/values-vi/`. Chuỗi không theo ngôn ngữ (ví dụ `%1$d%%`) ghi `translatable="false"`, vì lint dừng khi `values-vi/` thiếu nó."
- `CLAUDE.md`, mục Viết một feature mới, bước 10, ý "Feature có thiết kế": thêm "screenshot test cùng tên ảnh thiết kế đặt ngôn ngữ của chữ trên thiết kế (ví dụ `vi-w411dp-h891dp-xxhdpi`), vì Robolectric chạy `en-US`".
- `docs/ops/to-app.md`, mục 7: thêm ý (Android chọn chuỗi theo từng thư mục ngôn ngữ: chuỗi trong `values/` của app chỉ đè `values/` của module mẫu; trên máy để tiếng Việt, `values-vi/` của module mẫu vẫn thắng, đã chạy thử ở mục 4.1, ý 7):
  "Chữ của module mẫu: `values/` là tiếng Anh, `values-vi/` là tiếng Việt. Android chọn chuỗi theo từng thư mục ngôn ngữ, nên chuỗi app đặt trong `values/` không đè bản trong `values-vi/` của module mẫu: trên máy để tiếng Việt, chữ của mẫu vẫn hiện.
  - App đã đổi một chuỗi của mẫu (ví dụ `brand_tagline`, chữ của màn loading hay popup mất mạng) thì đặt giá trị của mình ở cả `values-vi/` cùng tên, hoặc xóa dòng đó trong `values-vi/` của module mẫu.
  - App chỉ một ngôn ngữ thì xóa các tệp `values-vi/` của module mẫu, hoặc đặt `androidResources { localeFilters += listOf("<ngôn ngữ>") }` trong `android { }` của `app/build.gradle.kts` để bản build chỉ giữ ngôn ngữ đó.
  - Sau mỗi đợt đồng bộ, đối chiếu từng tệp `values-vi/` mới hay đổi của module mẫu với các chuỗi app đã đổi.
  - Đợt đồng bộ đổi ảnh chuẩn có chữ của module mẫu."

Bước 7, tài liệu đi cùng PR (làm có người, như T-096): L-135 `applied` kèm số PR trong `docs/lessons.md`; nhật ký, plan `done` và ticket `on-staging` ghi trong lần đẩy cuối, sau rà soát độc lập.

Bước 8, những chỗ đã kiểm, không sửa:

- `scripts/device-scenario.py`: `SCENARIO_LANGUAGE = "en"` kèm chú thích "str= reads res/values, which is English in every module"; `string_values()` chỉ đọc thư mục `values` (`path.parent.name != "values"` thì bỏ), nên `values-vi/` không lọt vào. Hôm nay chú thích sai với `:feature:loading`, `:core:designsystem`, `:app`; sau task thì đúng. Không kịch bản nào dùng `str=` tới 12 chuỗi này: kịch bản duy nhất `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt` dùng `id=`, `text="Continue to app"`, `desc=` (chữ của hộp thoại đồng ý, không thuộc module mẫu).
- `docs/ops/test-sessions.md`: bảng lệnh (`str=` đọc `res/values/strings.xml`; `fresh` đặt ngôn ngữ của app là `en` "vì `str=` đọc `res/values/` (tiếng Anh)") đúng sau task.
- `docs/ops/ads-guide.md`, mục 5, bước 4, ý Rewarded: câu dịch chữ khung vẫn đúng sau task. (1) "giá trị theo ngôn ngữ mặc định của `values/` của `:app`" không nêu ngôn ngữ cụ thể: sau task là tiếng Anh. (2) "đặt bản dịch cùng tên trong mỗi `values-<ngôn ngữ>` mà `:app` có": sau task `:app` có `values-vi/` thay `values-en/`, nên app nào chép chuỗi `ads_*` vào tệp riêng sẽ được lint đòi bản tiếng Việt, đúng ý câu đó. (3) Vế lý do "tệp đó mang `tools:ignore="MissingTranslation"` cho cả tệp" vẫn đúng, vì plan giữ thuộc tính này (bước 1). Không sửa.
- `scripts/new-app.sh` (dòng 99) thay đúng dòng `app_name`, plan giữ dòng đó; test của nó trong `scripts/tests/run.sh` (dòng 3939) đọc `values/strings.xml` của app mới, không đổi.
- `docs/features/*/design/review/` và `scripts/design-compare.sh`: 6 ảnh thiết kế giữ nguyên ảnh chuẩn, nên không chạy lại so sánh. Bằng chứng là `git status --short` của 6 ảnh đó rỗng sau khi ghi ảnh (mục 4). Không chạy `design-compare.sh`: với `network` script không chạy được (không có module `feature/network`; ảnh `NetworkLostScreen_*` nằm ở `:app`), với `loading` script xóa rồi sinh lại `docs/features/loading/design/review/`, ngoài mục 3.2. Lỡ chạy thì trả bằng `git checkout -- docs/features/loading/design/review/`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/main/res/values/strings.xml` | 7 chuỗi tiếng Anh, `loading_percent` thêm `translatable="false"` (mục 3.1, bước 1) |
| Tạo | `feature/loading/src/main/res/values-vi/strings.xml` | 7 chuỗi tiếng Việt nguyên văn |
| Sửa | `core/designsystem/src/main/res/values/strings.xml` | `brand_tagline` tiếng Anh |
| Tạo | `core/designsystem/src/main/res/values-vi/strings.xml` | `brand_tagline` tiếng Việt nguyên văn |
| Sửa | `app/src/main/res/values/strings.xml` | 4 chuỗi popup tiếng Anh, bỏ `tools:locale="vi"`, chú thích mới, giữ dòng `app_name` và `tools:ignore` |
| Tạo | `app/src/main/res/values-vi/strings.xml` | 4 chuỗi popup tiếng Việt nguyên văn |
| Xóa | `app/src/main/res/values-en/strings.xml` | Nội dung đã vào `values/` |
| Tạo | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenLanguageTest.kt` | Test viết trước, mục 4 |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/template/core/designsystem/BrandTaglineLanguageTest.kt` | Test viết trước, mục 4 |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/network/NetworkDialogHostTest.kt` | Thêm `shown_usesAppStrings_otherLocale` và `shown_usesAppStrings_vi_stillOffline` (mục 4); test có sẵn không đổi |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenTest.kt` | Thêm `failed_onShortScreen_retryButtonFitsOnScreen_vi` (mục 4); test có sẵn không đổi |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenScreenshotTest.kt` | `@Config` của lớp đặt `vi`, KDoc (mục 3.1, bước 3) |
| Sửa | `app/src/test/kotlin/com/vglstudio/template/network/NetworkLostScreenScreenshotTest.kt` | `vi` chỉ cho `default_*`, KDoc (mục 3.1, bước 3) |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_failed_small.png` | Ảnh chuẩn ghi lại, chữ tiếng Anh |
| Sửa | `feature/loading/src/test/screenshots/LoadingScreen_progress_small.png` | Ảnh chuẩn ghi lại, chữ tiếng Anh |
| Sửa | `app/src/test/screenshots/NetworkLostScreen_stillOffline_light.png` | Ảnh chuẩn ghi lại, chữ tiếng Anh |
| Sửa | `app/src/test/screenshots/NetworkLostScreen_stillOffline_small.png` | Ảnh chuẩn ghi lại, chữ tiếng Anh |
| Sửa | `core/designsystem/src/test/screenshots/DesignSystem_brand_light.png` | Ảnh chuẩn ghi lại, khẩu hiệu tiếng Anh |
| Sửa | `core/designsystem/src/test/screenshots/DesignSystem_brand_dark.png` | Ảnh chuẩn ghi lại, khẩu hiệu tiếng Anh |
| Sửa | `docs/features/loading/design/README.md` | Mục 2: cột chữ tiếng Anh, câu về ngôn ngữ của ảnh (mục 3.1, bước 5) |
| Sửa | `docs/features/network/design/README.md` | Mục 2: câu về ngôn ngữ của ảnh |
| Sửa | `docs/features/loading/spec.md` | Mục 4: ý Ngôn ngữ của chữ; mục 10 lịch sử; trạng thái theo lời xác nhận (mục 8, câu 3) |
| Sửa | `docs/features/ads/spec.md` | KB-19 tiêu chí 1 theo mục 8, câu 2; mục lịch sử; trạng thái theo lời xác nhận |
| Sửa | `CLAUDE.md` | Mục Quy ước code và bước 10 (mục 3.1, bước 6) |
| Sửa | `docs/ops/to-app.md` | Mục 7: ý về chữ của module mẫu khi đồng bộ |
| Sửa | `docs/lessons.md` | L-135: `applied`, dòng Áp dụng ghi số PR. Trong PR của task |
| Sửa | `docs/features/quy-trinh/plans/T-082-chu-mac-dinh-tieng-anh.md` | Mục 10, nhật ký; trạng thái plan. Lần đẩy cuối của PR task |
| Sửa | `docs/features/quy-trinh/tickets/T-082-chu-mac-dinh-tieng-anh.md` | Trạng thái ticket theo kết quả. PR của task |

Nơi đọc các file bị đổi (`grep -rn` trong `scripts/`, `.github/`, `*/src/test`, `docs/`):

- Tài nguyên chuỗi: `LoadingScreen.kt`, `DesignSystemCatalog.kt`, `NetworkDialogHost.kt` đọc bằng `stringResource`, không đổi. Test đọc bằng `getString` (`LoadingScreenTest`, `LoadingRouteTest`) hay `stringResource` (`NetworkLostScreenScreenshotTest`) đi theo ngôn ngữ, không so chữ viết cứng. `scripts/device-scenario.py` chỉ đọc `values/` (mục 3.1, bước 8). `scripts/new-app.sh` thay dòng `app_name` (giữ). Không workflow nào đọc `strings.xml`.
- `values-en/`: chỉ nhắc trong ticket, L-135, plan T-017 và T-098 (lịch sử), chú thích đầu `app/src/main/res/values/strings.xml` (viết lại). `NetworkDialogHostTest.shown_usesAppStrings_en` đặt `en`; sau task `en` rơi về `values/` (tiếng Anh), test giữ nguyên và vẫn qua (đã đo).
- Ảnh chuẩn: `scripts/check.sh` và `verifyRoborazzi...` so ảnh; `scripts/design-compare.sh` chỉ so ảnh cùng tên ảnh thiết kế, 6 ảnh đó không đổi.

Các file khác mà `plan-check.py` (L-020) có thể thấy nhắc tới file của plan; không bị ảnh hưởng, không sửa:
- Plan cũ của `loading`, `network`, `ads`, `quy-trinh` (T-001 tới T-005, T-017, T-024, T-098 và các plan khác) nhắc các tệp `strings.xml`, test hay ảnh trên: tài liệu lịch sử, không sửa.
- `docs/ops/ads-guide.md`, `docs/ops/test-sessions.md`, `scripts/device-scenario.py`: đã kiểm, đúng sau task (mục 3.1, bước 8).
- `docs/lessons.md` được `scripts/status.py` đọc trạng thái (`applied` đã biết) và `scripts/history-refs.py` bỏ qua (chỗ dành cho lịch sử); `scripts/tests/run.sh` chỉ dùng tên file trong dữ liệu giả.
- `CLAUDE.md`, `docs/ops/to-app.md`: `scripts/history-refs.py` kiểm; câu mới không có mã ticket, bài học.
- Nhắc `CLAUDE.md`: `.github/ISSUE_TEMPLATE/from-app.yml` (nhắc cả file trong danh sách phần bị ảnh hưởng), `scripts/run-watchdog.py` (lời nhắc phiên đọc cả file), `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt` và `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt` (chú thích dẫn "CLAUDE.md, step 10"). Plan chỉ thêm câu vào ý có sẵn của bước 10 và mục Quy ước code, không đổi số bước hay tên mục, nên các chỗ dẫn vẫn đúng.
- Nhắc `docs/lessons.md`: `.github/ISSUE_TEMPLATE/from-app.yml` (mã bài học của app), không đọc nội dung sổ bài học của dự án mẫu.
- Nhắc `docs/features/loading/design/README.md`: `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenTest.kt` (KDoc "Text follows the text table of the design ..., section 2"). Mục 2 vẫn là bảng chữ, chỉ thêm cột tiếng Anh; test đọc chữ bằng `getString`, đúng với cả hai cột.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm code hay vị trí. Chữ của `:core:ads` không đổi; spec `ads` KB-19 tiêu chí 1 sửa chữ theo mục 8, câu 2
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. Chỉ đổi tài nguyên chuỗi và test.
- **Thư viện và SDK:** Không thêm. Test mới dùng thư viện đã có trên classpath test của từng module (`ui-test-junit4`, `androidx-test-core`, `androidx-test-ext-junit`, Robolectric).

### 3.4. Phương án dự phòng

Không có. Ghi ảnh, lint và test hai ngôn ngữ đã chạy thử trên máy dev (mục 4.1). Ảnh đổi không có bóng mờ (không `shadow`, `elevation`, `setShadowLayer` trong `NetworkDialogScreen`, `LoadingScreen`, `BrandCatalog`), nên ảnh vẽ trên macOS dùng được cho CI Linux; nếu `pr-check` vẫn báo lệch, lấy ảnh CI vẽ theo `CLAUDE.md`, mục Lệnh, sau khi xem `_compare.png`.

### 3.5. Hành vi đã có bị đổi

Plan không sửa Kotlin ngoài thư mục test; hành vi đổi là chữ hiện ra khi máy không để tiếng Việt. `scripts/existing-tests.py` không có tên nào để tìm (mục 4.1, ý 6). Đọc tay mọi test dùng 12 chuỗi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Máy để ngôn ngữ khác tiếng Việt và tiếng Anh: màn loading, khẩu hiệu, popup mất mạng hiện tiếng Việt | Hiện tiếng Anh (bước 1) | Không có (không test nào đặt ngôn ngữ thứ ba) | Câu mới: các test `..._otherLocale` của mục 4 |
| Máy để tiếng Anh: popup tiếng Anh (`values-en/`), màn loading và khẩu hiệu tiếng Việt | Tất cả tiếng Anh | `NetworkDialogHostTest.shown_usesAppStrings_en` (popup, `@Config(qualifiers = "en")`) | Giữ nguyên: `en` rơi về `values/` tiếng Anh, câu khẳng định không đổi, đã chạy qua (mục 4.1) |
| Máy để tiếng Việt: tiếng Việt | Không đổi (bước 1, `values-vi/`) | `NetworkDialogHostTest.shown_usesAppStrings_vi` | Giữ nguyên, đã chạy qua |
| Chữ của màn loading đúng tài nguyên | Không đổi | `LoadingScreenTest` (đọc chữ bằng `getString`, đo vị trí chân trang ở khung thấp), `LoadingRouteTest` | Giữ nguyên. Các test khung thấp nay đo với chữ tiếng Anh (Robolectric `en-US`); đã chạy qua. Bố cục tiếng Việt ở khung thấp (dòng hướng dẫn, chân trang tiếng Việt dài hơn) mất test, nên thêm `failed_onShortScreen_retryButtonFitsOnScreen_vi` (mục 4) |
| Máy để tiếng Việt: dòng báo vẫn chưa có mạng tiếng Việt | Không đổi (`values-vi/` của `:app`) | Hôm nay chỉ ảnh `NetworkLostScreen_stillOffline_*` (sẽ sang tiếng Anh); `shown_usesAppStrings_vi` không kiểm chuỗi này, và lint im vì `tools:ignore="MissingTranslation"` của `:app` | Câu mới: `shown_usesAppStrings_vi_stillOffline` (mục 4) |
| Hộp thoại mất mạng đúng bảng chữ, kích thước | Không đổi | `NetworkDialogTest` (chữ truyền qua tham số) | Giữ nguyên, không phụ thuộc tài nguyên |
| Ảnh `LoadingScreen_*` (4 ảnh thiết kế), `NetworkLostScreen_default_*` vẽ tiếng Việt | Không đổi (bước 3, ghim `vi`) | Ảnh chuẩn | Giữ ảnh, đã đo không đổi |
| Ảnh `_small`, `stillOffline_*`, `DesignSystem_brand_*` vẽ tiếng Việt | Tiếng Anh | Ảnh chuẩn | 6 ảnh ghi lại, mục 3.2, người xem từng ảnh |

Không câu khẳng định nào phải viết lại theo ADR-0031.

## 4. Kiểm thử

Test viết trước chạy trên tài nguyên cũ ở commit test; đã chạy thử chiều đỏ và chiều xanh bằng test tạm cùng câu khẳng định (mục 4.1).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn loading hiện tiếng Anh khi máy để ngôn ngữ khác tiếng Việt | Compose UI test (Robolectric, `@GraphicsMode(NATIVE)`, `createComposeRule`) | `LoadingScreenLanguageTest.progress_otherLocale_showsEnglish`, `@Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")`: `AppTheme { LoadingScreen(LoadingUiState.InProgress(0.68f), appName = "Todo App", onRetry = {}) }`; `onNodeWithText("Loading data")`, `"Preparing your space…"`, `"A private space for your work"`, `"Everything in rhythm."`, `"68%"` mỗi chữ `assertIsDisplayed()`. Đỏ trước code: `fr` rơi về `values/` tiếng Việt (đã đo: `expected:<[Loading data]> but was:<[Đang tải dữ liệu]>`) |
| Như trên, trạng thái lỗi | Compose UI test | `LoadingScreenLanguageTest.failed_otherLocale_showsEnglish`, cùng khung, `LoadingUiState.Failed(0.68f)`: `"Progress stopped"`, `"Unable to load data"`, `"The connection was interrupted. Check your network and try again to continue."`, `"Try again"` (nút, `useUnmergedTree = true`), `"68%"` hiện; `"Preparing your space…"` không có (`assertDoesNotExist`). Đỏ trước code |
| Máy để tiếng Việt vẫn hiện tiếng Việt (định nghĩa hoàn thành, ý 2) | Compose UI test | `LoadingScreenLanguageTest.progress_vietnamese_showsVietnamese` và `failed_vietnamese_showsVietnamese`, `@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")`: chữ tiếng Việt nguyên văn của mục 3.1, bước 1 (gồm "Mọi việc vào đúng nhịp.", "Thử lại", "68%"). Test này qua cả trước code (tài nguyên cũ là tiếng Việt), là đối chứng giữ tiếng Việt: commit code mà thiếu `values-vi/` thì nó đỏ. Bước đối chứng trong phiên: sau commit code, `rm feature/loading/src/main/res/values-vi/strings.xml`, chạy lớp test, thấy hai test `vietnamese` đỏ, rồi trả bằng `git checkout -- feature/loading/src/main/res/values-vi/strings.xml` và `git status --short` sạch |
| Bố cục tiếng Việt ở khung thấp | Compose UI test (lớp có sẵn) | `LoadingScreenTest.failed_onShortScreen_retryButtonFitsOnScreen_vi`, `@Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")`: thân và câu khẳng định chép nguyên test `failed_onShortScreen_retryButtonFitsOnScreen` (nút "Thử lại" kết thúc trong màn hình và trên mép trên của chân trang, bấm được một lần). Qua cả trước code (giữ hành vi); đối chứng là bước đối chứng `values-vi` ở dòng trên: không có `values-vi` thì test chạy với chữ tiếng Anh, vẫn qua, nên đây là test bố cục, không phải test chữ |
| Phần trăm không đổi theo ngôn ngữ | Compose UI test | Câu `"68%"` có trong cả bốn test trên |
| Khẩu hiệu theo ngôn ngữ | Unit test (Robolectric) | `BrandTaglineLanguageTest.otherLocale_isEnglish` (`@Config(qualifiers = "fr")`): `context.getString(R.string.brand_tagline) == "Everything in rhythm."`, đỏ trước code; `vietnamese_isVietnamese` (`"vi"`): `== "Mọi việc vào đúng nhịp."`, đối chứng như trên (`rm` rồi `git checkout --`) với `core/designsystem/src/main/res/values-vi/strings.xml` |
| Popup tiếng Việt đủ 4 chuỗi khi máy để tiếng Việt | Compose UI test (lớp có sẵn) | `NetworkDialogHostTest.shown_usesAppStrings_vi_stillOffline`, `@Config(qualifiers = "vi")`, `setContentButton(NetworkDialogState.Shown(checking = false, stillOffline = true))`: `onAllNodesWithText("Không có kết nối mạng")`, `"Ứng dụng cần Internet để hoạt động. Kiểm tra Wi-Fi hoặc dữ liệu di động rồi thử lại."`, `"Vẫn chưa có kết nối. Hãy thử lại sau ít phút."` mỗi chữ `assertCountEquals(1)`, nút `RETRY` `assertTextEquals("Thử lại")`. Qua cả trước code; đối chứng: sau commit code, `rm app/src/main/res/values-vi/strings.xml`, test này và `shown_usesAppStrings_vi` đỏ, rồi `git checkout -- app/src/main/res/values-vi/strings.xml` |
| Popup mất mạng tiếng Anh khi máy để ngôn ngữ khác | Compose UI test (lớp có sẵn) | `NetworkDialogHostTest.shown_usesAppStrings_otherLocale`, `@Config(qualifiers = "fr")`, dựng như `shown_usesAppStrings_en` (`Shown(checking = false, stillOffline = true)`) và cùng bốn câu khẳng định tiếng Anh. Đỏ trước code (`fr` rơi về `values/` tiếng Việt). `shown_usesAppStrings_vi` (đối chứng tiếng Việt) và `shown_usesAppStrings_en` giữ nguyên |
| Lint không báo thiếu bản dịch | Build | `:app:lintStagingDebug` trong `scripts/check.sh` (`checkDependencies = true` gồm `:feature:loading`, `:core:designsystem`). Đã đo: thiếu `translatable="false"` ở `loading_percent` thì lint dừng (mục 4.1) |
| Ảnh thiết kế không đổi | Screenshot test | `LoadingScreen_progress_light`, `LoadingScreen_progress_dark`, `LoadingScreen_failed_light`, `LoadingScreen_failed_dark`, `NetworkLostScreen_default_light`, `NetworkLostScreen_default_dark`: sau khi ghi ảnh, `git status --short -- feature/loading/src/test/screenshots/LoadingScreen_progress_light.png feature/loading/src/test/screenshots/LoadingScreen_progress_dark.png feature/loading/src/test/screenshots/LoadingScreen_failed_light.png feature/loading/src/test/screenshots/LoadingScreen_failed_dark.png app/src/test/screenshots/NetworkLostScreen_default_light.png app/src/test/screenshots/NetworkLostScreen_default_dark.png` in rỗng (đã đo). Không chạy `scripts/design-compare.sh` (mục 3.1, bước 8) |
| Ảnh đổi đúng danh sách | Screenshot test | Lệnh ghi ảnh của mục 3.1, bước 3; `git status --short -- '*/src/test/screenshots/*'` chỉ còn 6 ảnh của mục 3.2. Mức lệch đo lúc viết plan (máy dev macOS, so với nhánh chính, ảnh 1233 x 2673 hay 1080 x 1920 điểm ảnh): `LoadingScreen_failed_small` 60337 điểm (2,91%), lệch tối đa 235/255, vùng dp 48,259 tới 310,606 (khẩu hiệu tới chân trang); `LoadingScreen_progress_small` 28683 điểm (1,38%), tối đa 138, vùng 48,259 tới 282,606; `NetworkLostScreen_stillOffline_light` 60516 điểm (1,84%), tối đa 235, vùng 77,352 tới 334,563 (chữ trong thẻ và nút); `NetworkLostScreen_stillOffline_small` 60516 điểm (2,92%), tối đa 235, vùng 51,226 tới 308,437; `DesignSystem_brand_light` 7122 điểm (0,22%), tối đa 138, vùng 18,196 tới 173,209 (dòng khẩu hiệu); `DesignSystem_brand_dark` 7096 điểm (0,22%), tối đa 142, cùng vùng. Không ảnh nào đổi ngoài vùng chữ. Bố cục: lời báo lỗi tiếng Anh ở khung thấp 2 dòng, nút "Try again" không bị che hay cắt; popup khung thấp: nội dung 3 dòng, dòng báo 2 dòng, nút đủ |
| Ảnh ngoài danh sách | Screenshot test | `DesignSystem_spacing_components_light.png` đổi khi ghi lại (501 điểm, lệch tối đa 1, nhiễu vẽ): trả bằng `git checkout`; `verifyRoborazziDebug` của `:core:designsystem` qua với ảnh cũ (đã đo) |
| Toàn bộ | Build và test | `scripts/check.sh` (CI Linux, `pr-check`). Trên máy dev macOS, bước test script của `check.sh` có thể đỏ sẵn (plan T-096, mục 4.1): so danh sách `FAIL` với nhánh chính, rồi chạy lệnh Gradle của cổng chất lượng trong `check.sh` |
| Người xem từng ảnh đổi, xác nhận chữ tiếng Anh | Kiểm tra thủ công (người: xem 6 ảnh chuẩn đổi trong PR và xác nhận chữ của bảng mục 3.1, bước 1; định nghĩa hoàn thành ý 3 và 5) | Anh Nhật xem 6 ảnh của mục 3.2 trong tab Files của PR và trả lời mục 8, câu 3 |
| Spec `loading`, KB-19 của `ads` ghi đúng chữ | Kiểm tra thủ công (người: xác nhận spec `confirmed`) | Anh Nhật đọc ý mới của spec `loading` mục 4 và tiêu chí 1 của KB-19, xác nhận; AI ghi lại `confirmed` |

Không thêm kịch bản emulator: màn loading chỉ hiện thoáng qua, popup mất mạng cần tắt mạng và có hộp thoại đồng ý phủ lên bản staging mới cài; chữ theo ngôn ngữ đã kiểm trên JVM ở cả hai chiều (CLAUDE.md, bước 10).

**Ngoại lệ viết test trước:** đổi `@Config` và KDoc của hai lớp screenshot test (dữ liệu dựng test, kiểm bằng ảnh chuẩn), chú thích đầu `app/src/main/res/values/strings.xml`, ảnh chuẩn, tài liệu (`CLAUDE.md`, `docs/ops/to-app.md`, hai README thiết kế, hai spec, bài học, plan, ticket). Hai test `vietnamese`, `BrandTaglineLanguageTest.vietnamese_isVietnamese`, `NetworkDialogHostTest.shown_usesAppStrings_vi_stillOffline` và `LoadingScreenTest.failed_onShortScreen_retryButtonFitsOnScreen_vi` qua ngay trên tài nguyên cũ (giữ hành vi); ba test đầu có bước đối chứng đỏ ở bảng trên, test bố cục thì không (ghi ở bảng).

### 4.1. Chạy thử

Không bắt buộc (E dưới 2): ghim ngôn ngữ cho screenshot test đã có (`NetworkLostScreenScreenshotTest`, `NetworkDialogHostTest`). Đã chạy thử để có số đo thật cho mục 4, ngày 2026-11-10, trên nhánh chính `e078e7a`, máy dev macOS, JDK 21 của `scripts/local-setup.sh` (`source ~/.local/share/app-project-template/env.sh`). Mọi thay đổi tạm đã trả lại, không commit.

- **Đã chạy thử:**
  1. Lượt A, chỉ đổi tài nguyên như mục 3.1, bước 1 (chưa có `translatable="false"`), không đổi test: lệnh ghi ảnh của mục 3.1, bước 3, `exit=0` (mọi test qua, kể cả `NetworkDialogHostTest` `vi`, `en`). Ảnh đổi: 6 ảnh `LoadingScreen_*` (cả 4 ảnh thiết kế: `failed_light` 1,88%, `failed_dark` 1,88%, `progress_light` 0,88%, `progress_dark` 0,88%; và 2 ảnh `_small`), `DesignSystem_brand_*` (2), `DesignSystem_spacing_components_light` (nhiễu, lệch tối đa 1). `NetworkLostScreen_*` không đổi (đã ghim `vi`). Đây là phương án (b) của mục 8, câu 1.
  2. Lượt B, thêm ngôn ngữ của screenshot test như mục 3.1, bước 3 (phương án đề xuất): ảnh đổi đúng 6 ảnh của mục 3.2 cộng ảnh nhiễu `DesignSystem_spacing_components_light`; 4 ảnh thiết kế `LoadingScreen_*` và 2 ảnh `NetworkLostScreen_default_*` không đổi. Mức lệch ở mục 4, đo bằng một script Python so từng điểm ảnh (không commit). Đã xem ảnh `LoadingScreen_failed_small`, `NetworkLostScreen_stillOffline_small`, `DesignSystem_brand_light`: chữ tiếng Anh đủ, không bị cắt.
     Lưu ý lệnh: lần đầu đổi `@Config` bằng `perl -pi -e 's/\Q@Config(...)\E/.../'` hỏng tệp (`@Config@Config(`, lỗi biên dịch "This annotation is not repeatable"), vì `@Config` trong mẫu bị Perl hiểu là mảng rỗng dù có `\Q`. Phiên sửa hai tệp test bằng công cụ sửa tệp, không bằng `perl`; nếu dùng `perl` thì viết `\@Config`.
  3. Test tạm (một lớp, ba test, đọc chuỗi bằng `getString`): trên tài nguyên mới, `fr` ra "Loading data" và "Everything in rhythm.", mặc định Robolectric ra "Loading data", `vi` ra "Đang tải dữ liệu", "Mọi việc vào đúng nhịp.", `loading_percent` 68 ra "68%": `tests="3" failures="0"`. Trả `values/` về bản cũ, giữ `values-vi/`: `failures="2"`, `expected:<[Loading data]> but was:<[Đang tải dữ liệu]>` (test `fr` và mặc định), test `vi` qua. Câu khẳng định của mục 4 đỏ đúng lý do.
  4. `./gradlew :app:lintStagingDebug` với tài nguyên mới, chưa có `translatable="false"`: `Lint found 1 error`, `feature/loading/src/main/res/values/strings.xml:7: Error: "loading_percent" is not translated in "vi" (Vietnamese) [MissingTranslation]`. Thêm `translatable="false"`: `exit=0`. `app_name` không bị báo vì `tools:ignore="MissingTranslation"` vẫn ở thẻ `<resources>` của `:app`.
  5. Trả `DesignSystem_spacing_components_light.png` về bản cũ, chạy `./gradlew :core:designsystem:verifyRoborazziDebug :core:designsystem:testDebugUnitTest :feature:loading:verifyRoborazziDebug :feature:loading:testDebugUnitTest :app:verifyRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun` với 6 ảnh mới: `exit=0`.
  6. `python3 scripts/existing-tests.py` cho plan này: "Không có tên nào để tìm (plan không sửa file Kotlin ngoài test)." Bảng của mục 3.5 lập bằng cách đọc tay mọi test dùng 12 chuỗi.
  7. Ghi đè chuỗi theo thư mục ngôn ngữ (cho câu của `docs/ops/to-app.md`, mục 3.1, bước 6): tạm thêm `values-vi/` của `:core:designsystem` với `brand_tagline` "VI-LIB", và một tệp trong `values/` của `:app` đặt `brand_tagline` "APP-DEFAULT"; test tạm trong `:app` đọc `com.vglstudio.template.core.designsystem.R.string.brand_tagline`. `fr` ra "APP-DEFAULT"; `vi` ra "VI-LIB" (`expected:<[APP-DEFAULT]> but was:<[VI-LIB]>`): chuỗi trong `values/` của app không đè `values-vi/` của thư viện. Thêm `values-vi/` của `:app` với "APP-VI": `vi` ra "APP-VI", 2 test qua. Bỏ `values-vi/` của `:app`, thêm tạm `androidResources { localeFilters += listOf("en") }` vào `android { }` của `app/build.gradle.kts`: `vi` ra "APP-DEFAULT" (bản `values-vi/` của thư viện bị bỏ), 2 test qua. Mọi thay đổi đã trả lại.
- **Lệnh:** như trên. Chưa chạy `scripts/check.sh` đầy đủ: plan không dùng kỹ thuật mới; phiên chạy `check.sh` (CI chạy đủ).
- **Kết quả:** phương án đề xuất đổi đúng 6 ảnh, lint cần `translatable="false"`, test hai chiều đỏ và xanh đúng lý do. Phương án dự phòng không cần.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (module core dùng chung): sửa tài nguyên và ảnh chuẩn của `:core:designsystem`. Rủi ro Cao.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module: `:feature:loading`, `:core:designsystem`, `:app`, cùng hai spec, hai README thiết kế, `CLAUDE.md`, `docs/ops/to-app.md` |
| B. Độ rõ của yêu cầu | 2 | Còn câu hỏi ảnh hưởng tới cách làm: ngôn ngữ của screenshot test so với thiết kế (mục 8, câu 1) quyết ảnh nào đổi; chữ tiếng Anh và KB-19 chờ anh Nhật xác nhận (câu 2, 3) |
| C. Khả năng kiểm chứng tự động | 1 | Chữ theo hai ngôn ngữ, lint, ảnh đều có test tự động; người xem 6 ảnh và xác nhận chữ |
| D. Ảnh hưởng chéo | 3 | Đổi chữ mặc định mà mọi app tạo từ mẫu đang dựa vào: sau đồng bộ, `values-vi/` mới của mẫu thắng chuỗi app đã đổi trên máy tiếng Việt (kể cả `brand_tagline`), app một ngôn ngữ thấy ngôn ngữ kia (mục 6, mục 8, câu 4); ảnh chuẩn của app đổi khi đồng bộ |
| E. Độ mới của kỹ thuật | 0 | Ghim ngôn ngữ bằng `@Config(qualifiers)` đã có ở `NetworkLostScreenScreenshotTest`, `NetworkDialogHostTest` |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7, tổng 9)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `core/designsystem/src/main/res/values/strings.xml` (`brand_tagline`, `BrandLogo` của mọi app), ảnh chuẩn của `:core:designsystem`, tài nguyên và ảnh của `:app`, `:feature:loading`.
- Task khác có thể xung đột: không plan `approved` hay `draft` nào khác đang sửa các tệp `strings.xml`, hai lớp screenshot test hay ảnh trên (đã xem các plan `draft`, `approved` của mọi feature). T-098 (`done`) chỉ sửa `docs/ops/ads-guide.md`, plan này không sửa tệp đó.
- App nhận qua `scripts/template-sync.sh`: ảnh chuẩn của module mẫu đổi. Đợt đồng bộ mang về các tệp `values-vi/` mới, mà trên máy để tiếng Việt chúng thắng chuỗi app đặt trong `values/` (đã đo, mục 4.1, ý 7):
  - Mọi app đã thay `brand_tagline` (ADR-0008) sẽ hiện khẩu hiệu mẫu "Mọi việc vào đúng nhịp." trên máy tiếng Việt; chữ màn loading hay popup app đã sửa cũng vậy.
  - App chỉ tiếng Anh (Photune) sẽ hiện tiếng Việt trên máy tiếng Việt.
  - App chỉ tiếng Việt sẽ hiện tiếng Anh trên máy để ngôn ngữ khác.
  - Cách xử lý ở app theo câu mới của `docs/ops/to-app.md` (mục 3.1, bước 6): đặt giá trị của app ở cả `values-vi/` (hoặc xóa dòng trong `values-vi/` của mẫu); app một ngôn ngữ xóa `values-vi/` của mẫu hoặc đặt `localeFilters`; sau đồng bộ đối chiếu từng tệp `values-vi/` mới với chuỗi app đã đổi. Ghi vào mô tả PR của task và vào lời đóng issue #288 (mục 8, câu 4).
- Plan không sửa `.github/workflows/`, không cần `push-probe.sh`.
- Đề xuất thứ tự: làm có người, độc lập với các task khác.

## 7. Điều kiện dừng riêng

- Ghi ảnh mà một trong 6 ảnh thiết kế (`LoadingScreen_progress_*`, `LoadingScreen_failed_*`, `NetworkLostScreen_default_*`) đổi: dừng, ghim `vi` không có tác dụng như đã đo.
- Một ảnh ngoài mục 3.2 lệch quá nhiễu vẽ (lệch hơn 1 mỗi kênh hay ngoài vùng chữ): dừng.
- Lint đòi bỏ `tools:ignore="MissingTranslation"` hay sửa dòng `app_name` của `:app`: dừng, vì `scripts/new-app.sh` thay nguyên dòng đó.
- Anh Nhật chưa xác nhận chữ (mục 8, câu 3): không sửa hai spec, không merge.

## 8. Câu hỏi cho người duyệt

1. **Screenshot test so với thiết kế chạy tiếng Việt hay tiếng Anh; thiết kế có thêm chữ tiếng Anh không.** Ticket để plan quyết.
   - (a) Ảnh cùng tên ảnh thiết kế đặt `vi` (thiết kế giữ tiếng Việt, `design-compare.sh` so cùng chữ, test chữ khớp bảng chữ như ADR-0020 đòi); ảnh khác chạy tiếng Anh. 6 ảnh đổi. README thiết kế thêm cột chữ tiếng Anh, Figma không đổi.
   - (b) Mọi ảnh chạy tiếng Anh: 8 ảnh đổi (lượt A, mục 4.1), gồm 4 ảnh thiết kế; ảnh thiết kế (tiếng Việt) và ảnh chuẩn (tiếng Anh) khác chữ, tỷ lệ khác của `design-compare.sh` tăng, muốn so đúng phải dựng lại thiết kế tiếng Anh trên Figma và xuất lại.
   - (c) Mọi ảnh đặt `vi`: chỉ 2 ảnh `DesignSystem_brand_*` đổi (hay 0 nếu cũng ghim), nhưng không ảnh nào cho thấy chữ tiếng Anh, là ngôn ngữ mặc định mới.
   - Đề xuất: (a). Ảnh khung thấp `_small` chạy tiếng Anh nên kiểm được chữ mặc định ở màn hình thấp; tiếng Việt ở khung chuẩn vẫn có ảnh thiết kế. Chữ tiếng Việt ở khung thấp không còn ảnh: hai bản dài gần bằng nhau (lời báo lỗi 72 và 77 ký tự), không cần thêm ảnh.
2. **Chỗ lệch có sẵn ở spec `ads` KB-19 tiêu chí 1** (nút "Xem quảng cáo", "Không, cảm ơn", trong khi `:core:ads` hiện "Watch ad", "No, thanks"; anh để T-082 quyết ở câu 5 của plan T-098).
   - (a) T-082 không dịch `:core:ads` (ticket ghi ngoài phạm vi); sửa tiêu chí 1 thành nút xem quảng cáo và nút từ chối, chữ của khung "Watch ad", "No, thanks", app dịch theo hướng dẫn quảng cáo (câu đề xuất ở mục 3.1, bước 5).
   - (b) `:app` của mẫu dịch theo hướng dẫn: `app/src/main/res/values/ads_strings.xml` (chuỗi `ads_*` tiếng Anh) và `values-vi/ads_strings.xml`; KB-19 giữ chữ tiếng Việt, đúng trên máy để tiếng Việt. Thêm 2 tệp, app tạo từ mẫu phải giữ hay xóa tệp đó.
   - (c) Thêm `core/ads/src/main/res/values-vi/strings.xml` (9 chuỗi) theo đúng quy tắc mới cho mọi module mẫu. Mọi app tiếng Việt có sẵn chữ khung; nhưng nhãn "Quảng cáo" dài hơn "Ad" ở thẻ native nhỏ, cần ảnh và test bố cục tiếng Việt của `:core:ads`, và câu hướng dẫn dịch ở `docs/ops/ads-guide.md` phải sửa.
   - Đề xuất: (a) trong T-082, giữ đúng phạm vi ticket; nếu anh muốn app tiếng Việt có sẵn chữ khung thì mở ticket riêng cho (c), có kiểm bố cục thẻ native.
3. **Chữ tiếng Anh mới (anh xác nhận chữ của spec `loading`).** Popup lấy nguyên bản đã có ở `values-en/`. Tám chữ còn lại là đề xuất: "Loading data", "Preparing your space…", "Progress stopped", "Unable to load data", "The connection was interrupted. Check your network and try again to continue.", "Try again", "A private space for your work", khẩu hiệu "Everything in rhythm.". Đề xuất: dùng đúng các chữ này; anh sửa chữ nào thì AI đổi trong tài nguyên, test của mục 4 và đo lại ảnh trước khi ghi ảnh chuẩn. Lời "Xác nhận chữ T-082" (kèm chữ sửa nếu có) cũng là lời xác nhận cho ý mới của spec `loading` mục 4.
4. **App đã đổi chuỗi của mẫu, và app một ngôn ngữ, sau đồng bộ.** Android chọn chuỗi theo từng thư mục ngôn ngữ: chuỗi trong `values/` của app không đè `values-vi/` mới của module mẫu (đã đo, mục 4.1, ý 7). Nên sau đồng bộ, trên máy để tiếng Việt: app đã thay `brand_tagline` (ADR-0008) hay chữ loading, popup thì lại thấy chữ tiếng Việt của mẫu; app chỉ tiếng Anh như Photune thấy tiếng Việt. App chỉ tiếng Việt thấy tiếng Anh trên máy để ngôn ngữ khác.
   - Đề xuất: chấp nhận, kèm câu mới ở `docs/ops/to-app.md` mục 7 (mục 3.1, bước 6): app đã đổi chuỗi của mẫu đặt giá trị của mình ở cả `values-vi/` (hoặc xóa dòng trong `values-vi/` của mẫu); app một ngôn ngữ xóa các tệp `values-vi/` của mẫu hoặc đặt `localeFilters` ở `:app`; sau mỗi đợt đồng bộ đối chiếu từng tệp `values-vi/` mới với chuỗi app đã đổi.
   - Lời đóng issue #288 đề xuất (anh đăng sau khi task merge): "Đã làm ở T-082 (PR #<số>): `res/values/` của module mẫu là tiếng Anh, tiếng Việt ở `res/values-vi/`. Khi đồng bộ: app đã đổi chuỗi của mẫu (ví dụ `brand_tagline`) thì đặt giá trị của app ở cả `values-vi/`, vì chuỗi trong `values/` của app không đè `values-vi/` của mẫu; app chỉ tiếng Anh như Photune xóa các tệp `values-vi/` của mẫu hoặc đặt `androidResources { localeFilters += listOf("en") }` ở `:app`; đối chiếu từng tệp `values-vi/` mới sau đồng bộ (`docs/ops/to-app.md`, mục 7). Ảnh chuẩn của module mẫu đổi."
   - Anh cho biết các app hiện có của nhóm và ngôn ngữ của từng app, để báo riêng app nào cần làm bước này ở đợt đồng bộ sau.
5. **Làm có người, một PR.** Ticket ghi `auto_run: false`. Đề xuất: một PR chứa tài nguyên, test, ảnh, tài liệu, L-135 `applied`, nhật ký plan và ticket (như T-096); test viết trước commit riêng trước commit tài nguyên. Hai spec chỉ sửa sau lời xác nhận ở câu 2 và 3.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng). -->
