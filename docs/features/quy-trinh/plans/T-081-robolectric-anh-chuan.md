---
ticket: T-081
title: Robolectric 4.17, không ảnh chuẩn nào đổi
status: draft
version: 1
risk_score: 6
risk_level: high
token_estimate: 5.4M
blocking_points: [build, sdk]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-081: ROBOLECTRIC 4.17, KHÔNG ẢNH CHUẨN NÀO ĐỔI

## 1. Tóm tắt

Plan nâng Robolectric từ 4.15.1 lên 4.17 bằng một dòng trong `gradle/libs.versions.toml`. Lần dựng thử trên main `661dbfa` (đã có T-080) cho kết quả: `scripts/check.sh` qua với 1498 test JVM và 89 ảnh chuẩn. Ghi lại ảnh chuẩn bằng Robolectric 4.17 cho ra 89 ảnh giống ảnh cũ từng byte, trừ một ảnh. Ảnh đó lệch vì một lý do có sẵn, không phải do Robolectric: bộ build trước T-080, với Robolectric 4.15.1, cũng vẽ ra đúng ảnh lệch đó (mục 4.1). Vì vậy plan không ghi lại ảnh chuẩn nào. Đây là bước 3 trong 3 bước tách từ T-052 (L-127, issue #276). Theo T-080 (plan mục 8, câu 1), Roborazzi đã lên 1.77.0 rồi, nên ticket T-081 chỉ còn Robolectric.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là L-127, cộng phạm vi ticket T-081 đã sửa ở PR #320.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | `scripts/check.sh` qua với ảnh chuẩn mới | `check.sh` qua với Robolectric 4.17. Không có ảnh nào đổi do Robolectric, nên "ảnh chuẩn mới" chính là 89 ảnh hiện có. Số test không giảm so với `main-guard` gần nhất |
| Định nghĩa hoàn thành 2 | Danh sách ảnh đổi và mức lệch có trong plan, người duyệt đã xem từng ảnh | Danh sách ở mục 4.1: 0 ảnh đổi do Robolectric. Có 1 ảnh lệch có sẵn, ghi số đo; người duyệt xem ảnh đó (mục 4, kiểm tay) |
| Định nghĩa hoàn thành 3 | Không test nào bị nới câu khẳng định | Không sửa test nào |
| Định nghĩa hoàn thành 4 | CI qua, đã merge | Như mọi PR |
| Phạm vi: test đo màu, kích thước đỏ vì cách vẽ mới | Tìm nguyên nhân, chỉ sửa phần dựng test (ADR-0033/L11) | Lần thử: không test nào đỏ |
| Phạm vi: `CLAUDE.md` phần test | Sửa nếu quy tắc về `GraphicsMode`, `captureRoboImage` hay khung chụp đổi | Không đổi: mọi test của các quy tắc đó qua nguyên vẹn, ảnh giống từng byte |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0011 (cổng chất lượng, ảnh chuẩn không cập nhật ngoài plan), ADR-0040 (bộ build; không ghi phiên bản Robolectric, không cần sửa), ADR-0029, ADR-0033.

## 3. Giải pháp

### 3.1. Cách làm

1. `gradle/libs.versions.toml` (file được bảo vệ; plan nêu rõ để phiên được sửa): đổi `robolectric = "4.15.1"` thành `robolectric = "4.17"`. Không đổi gì khác. Thư viện `robolectric` chỉ được thêm ở `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt` (`testImplementation`), nên không cần sửa file build nào.
2. Không ghi lại ảnh chuẩn. Lệnh ghi ảnh ở mục 4 chỉ để đo, không commit. Sau lệnh đó, `git checkout -- '*/src/test/screenshots/'` đưa ảnh về như cũ.
3. `robolectric.properties` của các module giữ `sdk=35`. Robolectric 4.17 tải `android-all-instrumented` bản mới cho SDK 35 lúc test chạy lần đầu. Trên máy dev lần thử đã tải được. Phiên cloud tải qua mirror Maven Central mà `cloud-setup.sh` đặt (`robolectric.dependency.repo.url`); CI tải từ Maven Central.
4. Ticket `T-081-robolectric-roborazzi-anh-chuan.md`: `plan:`, trạng thái theo plan, cỡ `size: S` (lần thử cho thấy không có ảnh nào phải ghi lại).

**Ảnh lệch có sẵn** (không thuộc plan, ghi để người biết): `core/designsystem/src/test/screenshots/DesignSystem_spacing_components_light.png`, ghi từ 2026-10-17 (T-004, commit `050997e`). Mọi bộ build đã thử trên máy dev đều vẽ nó khác bản trong repo ở 501 điểm ảnh (0,0152%), mỗi điểm lệch đúng 1 mức màu trên 255. Roborazzi so ảnh có ngưỡng, nên `check.sh` và CI vẫn qua. Ghi lại ảnh này là việc riêng, không do Robolectric; plan không đụng (mục 8, câu 2).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. `robolectric = "4.17"` |
| Sửa | `docs/features/quy-trinh/tickets/T-081-robolectric-roborazzi-anh-chuan.md` | `plan:`, trạng thái theo plan, `size: S` |

Các file đọc file plan sửa (L-020; `grep -rn robolectric` trong `scripts/`, `.github/`, `build-logic/`, 2026-10-30):
- `gradle/libs.versions.toml`: `build-logic/settings.gradle.kts` nạp làm catalog (khóa `robolectric` giữ tên); `AndroidComposeConventionPlugin.kt` dùng `libs.lib("robolectric")`, không đọc số phiên bản; `scripts/cloud-env-check.sh` và `scripts/tests/run.sh` chỉ đọc dòng `compileSdk`; `scripts/plan-check.py` chỉ dùng tên file. `.github/workflows/pr-check.yml` có tên file trong bộ lọc đường dẫn, nên PR này chạy cả build release.
- `scripts/cloud-setup.sh` chỉ đặt `robolectric.dependency.repo.url` cho mọi task test, không phụ thuộc phiên bản.
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` chỉ nhắc `gradle/libs.versions.toml` trong một chú thích về lint ("Library versions are managed in gradle/libs.versions.toml"), không đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền. Robolectric chỉ ở `testImplementation`, không vào APK.
- **Thư viện và SDK:** Không thêm thư viện. Nâng `org.robolectric:robolectric` (khóa `robolectric` trong `gradle/libs.versions.toml`) từ 4.15.1 lên 4.17, chỉ ở classpath test của các module có plugin `template.android.compose`; giấy phép MIT như cũ.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** CI (Linux, Java 17) và phiên cloud vẽ khác máy dev với Robolectric 4.17, hay không tải được `android-all-instrumented` bản mới.
- **Điều kiện chuyển:** `pr-check` báo ảnh chuẩn lệch hay test giao diện đỏ, mà cùng commit với Robolectric 4.15.1 thì xanh.
- **Cách làm thay:** không có cách thay trong plan này. Dừng theo mục 7, giữ 4.15.1. Ghi ảnh chuẩn theo cách vẽ của CI là một plan khác, vì ảnh chuẩn đổi cần người xem từng ảnh.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin nào. Mọi test JVM có sẵn là test hồi quy, phải qua nguyên vẹn với Robolectric 4.17.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Robolectric 4.17 có trên classpath test | Lệnh | `./gradlew -q :feature:sample:dependencies --configuration debugUnitTestRuntimeClasspath \| grep -o 'org.robolectric:robolectric:[0-9.]*' \| sort -u` in đúng `org.robolectric:robolectric:4.17` |
| Mọi test qua, không ảnh nào đổi | Cổng chất lượng | `scripts/check.sh`: `== Cổng chất lượng: QUA`; dòng `Test JVM:` có số test không ít hơn lần `main-guard` gần nhất, 0 lỗi; `git status --short -- '*/src/test/screenshots/'` rỗng |
| Robolectric 4.17 không làm đổi ảnh nào | Lệnh đo, không commit | Lệnh ghi ảnh của `CLAUDE.md` (mục Lệnh, có `--rerun`) cho cả năm module có ảnh: `./gradlew :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun :core:designsystem:recordRoborazziDebug :core:designsystem:testDebugUnitTest --rerun :feature:loading:recordRoborazziDebug :feature:loading:testDebugUnitTest --rerun :feature:sample:recordRoborazziDebug :feature:sample:testDebugUnitTest --rerun`; rồi `git status --short -- '*/src/test/screenshots/'` chỉ có `DesignSystem_spacing_components_light.png`, hay rỗng; ảnh mới thêm từ phiên quảng cáo cũng phải giống từng byte. Rồi `git checkout -- '*/src/test/screenshots/'` |
| So ảnh vẫn bắt được lệch nhỏ | Đối chứng, không commit | Đảo màu một khối 4 x 4 điểm ảnh ở giữa `feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png`, chạy `./gradlew :feature:sample:verifyRoborazziDebug :feature:sample:testDebugUnitTest --rerun`: đỏ, 1 test lỗi; trả ảnh về bằng `git checkout --` |
| Bản release dựng được | Build | `pr-check` của PR (PR đổi `gradle/libs.versions.toml` nên chạy build release); trên máy không cần, vì Robolectric không vào APK |
| CI dựng và test được | Kiểm tra thủ công (người: GitHub Actions) | `pr-check` xanh, gồm `ui-test` |
| Phiên cloud tải được Robolectric bản mới | Kiểm tra thủ công (người: phiên cloud có người, sau khi merge) | Trong một phiên cloud mới trên nhánh chính: `scripts/check.sh` QUA |
| Ảnh lệch có sẵn | Kiểm tra thủ công (người: xem ảnh) | Người duyệt mở `DesignSystem_spacing_components_light.png` ở repo và bản ghi lại (mục 4.1), thấy không khác bằng mắt, và trả lời mục 8, câu 2 |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** đổi một dòng phiên bản trong `gradle/libs.versions.toml` và ticket. Không có hành vi mới nào để viết test đỏ trước; bằng chứng là cổng chất lượng, lệnh đo ảnh và đối chứng ở trên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, máy dev (macOS, JDK 21.0.13), nhánh thử `claude/trial-T-081` từ main `661dbfa` (có T-080 và T-067), chưa đẩy. Log ở thư mục tạm của phiên.
- **Lệnh:** `scripts/check.sh` (với ảnh cũ); lệnh ghi ảnh của mục 4 cho năm module; so điểm ảnh từng ảnh với bản ở `HEAD` (Pillow); `scripts/check.sh` lần nữa; đối chứng 4 x 4 điểm ảnh; `dependencies --configuration debugUnitTestRuntimeClasspath`.
- **Kết quả:**
  1. `check.sh` với Robolectric 4.17 và ảnh cũ: QUA, `Test JVM: 1498 test, 0 lỗi. Ảnh screenshot: 89`, không test nào đỏ.
  2. Classpath test của `:feature:sample` có `org.robolectric:robolectric:4.17`.
  3. Lệnh ghi ảnh chạy thật 10 task (5 task test, 5 task ghi ảnh) và ghi lại cả 89 ảnh (30 của `:app`, 41 của `:core:ads`, 6 của mỗi module `:core:designsystem`, `:feature:loading`, `:feature:sample`). 88 ảnh giống bản cũ từng byte. Ảnh thứ 89, `DesignSystem_spacing_components_light.png`, lệch 501 điểm ảnh (0,0152%), mỗi điểm lệch tối đa 1 mức màu.
  4. Ảnh đó không lệch vì Robolectric. Ghi lại `:core:designsystem` hai lần bằng Robolectric 4.15.1 trên cùng main: cả hai lần ra đúng ảnh lệch, giống từng byte bản của 4.17. Ghi lại trên bộ build trước T-080 (AGP 8.13, Roborazzi 1.43, Robolectric 4.15.1; nhánh `claude/plan-T-080-agp-9`, `883c2c7`): cũng ra đúng ảnh đó. Ảnh trong repo là bản ghi ngày 2026-10-17 (T-004), có lẽ trên một máy khác; lệch nằm dưới ngưỡng so của Roborazzi, nên `check.sh` và CI vẫn qua.
  5. `check.sh` lần hai (sau khi ghi): QUA, 1498 test, 89 ảnh.
  6. Đối chứng với Robolectric 4.17: đảo màu khối 4 x 4 điểm ảnh của `SampleScreen_ad-loading_light.png` thì `:feature:sample:testDebugUnitTest` đỏ, "27 tests completed, 1 failed"; trả ảnh về thì `git status` sạch.
- **Chưa thử:** CI Linux và phiên cloud (kiểm tay ở mục 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (cấu hình build: `gradle/libs.versions.toml`). Điểm 2 tính là chạm vì plan đổi phiên bản trong catalog (`scripts/plan-check.py` cũng tính vậy); Robolectric chỉ ở test, không vào APK, và lên bản phụ. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Một dòng, nhưng đổi môi trường chạy test giao diện của mọi module có Compose |
| B. Độ rõ của yêu cầu | 0 | Ticket rõ; lần thử trả lời câu hỏi chính (ảnh có đổi không) |
| C. Khả năng kiểm chứng tự động | 1 | Cổng chất lượng, lệnh đo ảnh và đối chứng kiểm hết; CI Linux và phiên cloud kiểm tay |
| D. Ảnh hưởng chéo | 1 | Các task quảng cáo của RUN-20261030-01 thêm test chạy trên Robolectric; T-068 cũng sửa `gradle/libs.versions.toml`, ở dòng khác |
| E. Độ mới của kỹ thuật | 1 | Nâng bản phụ của thư viện đã dùng |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 2, 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `gradle/libs.versions.toml`.
- Phiên RUN-20261030-01 (các plan quảng cáo) đang chạy lúc viết plan (`status: running`). Các task của nó có thể thêm hay đổi ảnh chuẩn và test giao diện. Lần thử dùng main `661dbfa`; phiên làm task phải chạy lại lệnh đo ảnh trên main mới nhất, và ảnh mới của phiên quảng cáo cũng phải giống từng byte.
- `merge-if-green.sh` cho mọi nhánh `claude/T-*` merge khi có phiên đang chạy (nhật ký T-080, PR #323). Plan này chỉ làm khi không có phiên nào đang chạy (mục 7).
- T-068 thêm dòng vào nhóm `# Ads` của `gradle/libs.versions.toml`, khác dòng `robolectric`: git gộp được.
- Đề xuất thứ tự: sau khi RUN-20261030-01 kết thúc, trong phiên có người.

## 7. Điều kiện dừng riêng

- Lúc bắt đầu task hay lúc gọi `merge-if-green.sh`, có danh sách chạy `running` trên nhánh `claude/RUN-*-report`: dừng, chờ phiên kết thúc.
- Lệnh đo ảnh cho thấy một ảnh nào khác `DesignSystem_spacing_components_light.png` đổi: dừng. Ảnh đổi do Robolectric cần một plan mới, liệt kê ảnh và mức lệch cho người xem.
- `check.sh` hay `pr-check` đỏ ở một test giao diện mà Robolectric 4.15.1 xanh: dừng.

## 8. Câu hỏi cho người duyệt

1. Ticket T-081 dự kiến ghi lại ảnh chuẩn, nhưng lần thử cho thấy không ảnh nào đổi do Robolectric 4.17. Đề xuất: không ghi lại ảnh nào, và chuyển cỡ ticket từ M sang S.
2. Ảnh lệch có sẵn `DesignSystem_spacing_components_light.png` (501 điểm ảnh, lệch 1 mức màu, dưới ngưỡng so). Đề xuất: không đụng trong plan này. Nếu anh muốn ảnh trong repo khớp đúng cách vẽ hiện tại, đó là một ticket nhỏ riêng: ghi lại một ảnh, có người xem.
3. Thứ tự. Đề xuất: làm sau khi phiên RUN-20261030-01 kết thúc, vì task quảng cáo có thể thêm ảnh, và vì `merge-if-green.sh` không chặn nhánh `claude/T-*` khi có phiên đang chạy.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
