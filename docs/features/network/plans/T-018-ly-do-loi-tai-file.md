---
ticket: T-018
title: Lý do tải file R2 thất bại (AssetFailureReason)
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (core)
risk_level: high             # low | medium | high
token_estimate: 10M          # cỡ S, số gợi ý của scripts/token-usage.py estimate (2026-10-23)
blocking_points: [core]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-10-23
---

# PLAN T-018: LÝ DO TẢI FILE R2 THẤT BẠI (ASSETFAILUREREASON)

## 1. Tóm tắt

Chép `AssetFailureReason` của app camera-pose (issue #73, PR VGL-Studio/camera-pose#12) vào `:core:assets`: `AssetResult.Unavailable` mang lý do `NETWORK`, `NOT_FOUND` hay `OTHER`, suy từ ngoại lệ và chuỗi nguyên nhân; `HttpAssetDownloader` ném `HttpStatusException` có mã HTTP. Dòng log khi tải lỗi ghi thêm lý do và tên lớp lỗi. Kiểm chứng bằng unit test cho từng loại lỗi và cho `DefaultRemoteAssets`.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/network/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-3 | Khi tải thất bại vì lỗi mạng (không kết nối được, quá thời gian) thì popup hiện ngay; máy chủ trả lỗi thì màn hình của feature báo lỗi và có nút thử lại như trước, popup không hiện (spec sửa 2026-10-23) | Phần của T-018: phân biệt lỗi không tới được máy chủ (`UnknownHostException`, `SocketException` gồm `ConnectException`, `NoRouteToHostException`, `SocketTimeoutException`, xét cả nguyên nhân tới 10 tầng) với lỗi máy chủ trả lời (`HttpStatusException`: 404 là `NOT_FOUND`, mã khác là `OTHER`) và lỗi khác (`OTHER`). `Unavailable` vẫn là kết quả không tải được, nên feature vẫn báo lỗi và cho thử lại như trước. Popup chỉ xét `NETWORK` và còn kiểm trạng thái mạng của máy (T-017), nên quá thời gian khi vẫn có mạng không làm hiện popup |

Ghi chú: quá thời gian (`SocketTimeoutException`) được xếp là `NETWORK` như app, vì từ lỗi tải không biết máy còn mạng hay không; T-017 quyết bằng trạng thái mạng của máy.

Thiết kế tham chiếu: Không có thiết kế riêng (không có giao diện).

ADR liên quan: ADR-0015 (`:core:assets`, `AssetResult`), ADR-0006 (file tĩnh trên R2), ADR-0019 (log không có dữ liệu cá nhân; đường dẫn file R2 không phải dữ liệu cá nhân và đã có trong log hiện tại), ADR-0024 (popup mất mạng), ADR-0001.

## 3. Giải pháp

### 3.1. Cách làm

**Nguồn code.** PR VGL-Studio/camera-pose#12 (T-009 của app, merge `fdc71175dd14366bfe52f0e72597436b5b78caa3`, đọc bằng `gh pr diff 12 -R VGL-Studio/camera-pose`), chỉ phần `core/assets/`, đổi gói `com.vglstudio.camera.pose.line.guide` thành `com.vglstudio.template`:

| File ở dự án mẫu | Nguồn ở app | Cách lấy |
| --- | --- | --- |
| `core/assets/.../AssetFailureReason.kt` | `core/assets/.../AssetFailureReason.kt` | Chép; thêm hàm `downloadFailureMessage` |
| `core/assets/.../AssetDownloader.kt` (sửa) | cùng tên | Chép `HttpStatusException` và dòng ném nó |
| `core/assets/.../RemoteAssets.kt` (sửa) | cùng tên | Chép `Unavailable(reason)` và KDoc |
| `core/assets/.../DefaultRemoteAssets.kt` (sửa) | cùng tên | Chép `AssetResult.Unavailable(e.toFailureReason())` |
| `core/assets/src/test/.../AssetFailureReasonTest.kt` | cùng tên | Chép 7 test, thêm test của dòng log |
| `core/assets/src/test/.../DefaultRemoteAssetsFailureReasonTest.kt` | cùng tên | Chép 4 test |
| `core/assets/src/test/.../DefaultRemoteAssetsTest.kt` (sửa) | cùng tên | Chép hai dòng `Unavailable(AssetFailureReason.OTHER)` |

**Phần đổi so với app:**

| Điểm | App camera-pose (PR #12) | Dự án mẫu (plan này) | Lý do |
| --- | --- | --- | --- |
| Log khi tải lỗi | `Log.w("Assets", "Could not download <path>", error)` | `Log.w("Assets", downloadFailureMessage(path, error), error)`, thông điệp `Could not download <path>: <REASON> (<SimpleClassName>)` | Issue #73, L-027, FB-20261022-04 của app: `Log.getStackTraceString` trả chuỗi rỗng với `UnknownHostException`, log không cho biết vì sao |
| `:core:poses`, `:core:config`, `CLAUDE.md` của PR #12 | Có | Không lấy | Riêng của app (module tư thế), không thuộc issue #73 |
| `DefaultPreparationRepositoryTest` của `:feature:loading` | App sửa trong PR #12 theo code riêng | Sửa một dòng `AssetResult.Unavailable` thành `AssetResult.Unavailable()` | Biên dịch được sau khi `Unavailable` thành lớp |
| Tên gói | `com.vglstudio.camera.pose.line.guide` | `com.vglstudio.template` | Dự án mẫu |

**API.**

```kotlin
enum class AssetFailureReason { NETWORK, NOT_FOUND, OTHER }

sealed interface AssetResult {
    data class Ready(val file: File) : AssetResult
    data class Unavailable(val reason: AssetFailureReason = AssetFailureReason.OTHER) : AssetResult
}

internal class HttpStatusException(val status: Int, url: String) : IOException("HTTP $status for $url")
internal fun IOException.toFailureReason(): AssetFailureReason
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String
```

Giá trị mặc định `OTHER` giữ cho nơi tạo `Unavailable()` không cần lý do (bản giả trong test). Nơi so `is AssetResult.Unavailable` không đổi; nơi dùng `AssetResult.Unavailable` như một đối tượng (so bằng `==`, gán) phải sửa: trong repo chỉ có `DefaultRemoteAssetsTest.kt` (2 chỗ) và `DefaultPreparationRepositoryTest.kt` (1 chỗ) (`grep -rn "Unavailable" core feature app`). `docs/adr/0015-r2-assets-module.md` nhắc `AssetResult.Unavailable` trong bảng: ADR `accepted` không sửa; nội dung vẫn đúng (vẫn là kết quả "chưa có trên máy và không tải được").

**Thứ tự commit (L-025).** Commit test (bước 3): `AssetFailureReasonTest.kt`, `DefaultRemoteAssetsFailureReasonTest.kt`, sửa `DefaultRemoteAssetsTest.kt` và `DefaultPreparationRepositoryTest.kt`. Không biên dịch được vì `AssetFailureReason`, `HttpStatusException`, `Unavailable(reason)` chưa có: đúng lý do. Commit code (bước 4): bốn file `src/main`.

**Thay đổi có ích cho mọi app (from-app).** Đây là phần lấy từ app về dự án mẫu; mô tả PR ghi "Từ camera-pose: issue #73".

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/AssetFailureReason.kt` | Enum, `toFailureReason()`, `downloadFailureMessage()` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/AssetDownloader.kt` | `HttpStatusException`; ném nó khi mã khác 200 |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/RemoteAssets.kt` | `Unavailable(reason)`, KDoc |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/DefaultRemoteAssets.kt` | `AssetResult.Unavailable(e.toFailureReason())` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/template/core/assets/di/AssetsModule.kt` | Dòng log dùng `downloadFailureMessage` |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/template/core/assets/AssetFailureReasonTest.kt` | Từng loại lỗi, dòng log |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/template/core/assets/DefaultRemoteAssetsFailureReasonTest.kt` | Lý do qua `DefaultRemoteAssets` |
| Sửa | `core/assets/src/test/kotlin/com/vglstudio/template/core/assets/DefaultRemoteAssetsTest.kt` | Hai chỗ `Unavailable(AssetFailureReason.OTHER)` |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/data/DefaultPreparationRepositoryTest.kt` | `AssetResult.Unavailable()` |
| Sửa | `docs/features/network/tickets/T-018-ly-do-loi-tai-file.md` | Trạng thái ticket theo phiên tự chạy |
| Sửa | `docs/features/network/plans/T-018-ly-do-loi-tai-file.md` | Nhật ký thực hiện |

Không sửa `core/assets/build.gradle.kts` (T-017 thêm phụ thuộc `:core:network`), `feature/loading/src/main` (chỉ dùng `is AssetResult.Unavailable`, đã kiểm bằng `grep`), ADR-0015.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: không đổi đường dẫn, cách tải hay thời gian chờ; chỉ thêm lý do vào kết quả lỗi.
- **Quyền truy cập:** Không thêm. `HttpURLConnection` đã dùng quyền `INTERNET` có sẵn.
- **Thư viện và SDK:** Không thêm. Chỉ dùng `java.net`, `java.io` và thư viện test có sẵn (JUnit, coroutines-test qua `KotlinAndroid.kt`).

### 3.4. Phương án dự phòng

Không có (Kotlin thuần, đã chạy thử qua `scripts/check.sh`, mục 4.1).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-3: không tới được máy chủ là `NETWORK` | Unit test | `AssetFailureReasonTest.unknownHost_isNetwork`, `connectException_isNetwork`, `socketTimeout_isNetwork`, `networkErrorAsCause_isNetwork` (`IOException` bọc `UnknownHostException`) |
| KB-3: máy chủ trả lời lỗi không phải `NETWORK` | Unit test | `AssetFailureReasonTest.http404_isNotFound`, `http500_isOther`, `plainIOException_isOther` |
| Qua `DefaultRemoteAssets` | Unit test | `DefaultRemoteAssetsFailureReasonTest.downloaderThrowsUnknownHost_returnsUnavailableNetwork`, `downloaderThrowsHttp404_returnsUnavailableNotFound`, `downloaderThrowsPlainIOException_returnsUnavailableOther`, `keptFile_returnsReadyWithoutReason` |
| KB-3: feature vẫn báo lỗi và thử lại như trước | Unit test có sẵn | `DefaultRemoteAssetsTest.offlineWithoutKeptFile_isUnavailable_thenRetrySucceeds`, `failedDownload_leavesNoFileBehind` (so `Unavailable(OTHER)`), `DefaultPreparationRepositoryTest` và `LoadingViewModelTest` của `:feature:loading` qua không đổi hành vi |
| Log ghi lý do và tên lớp lỗi | Unit test | `AssetFailureReasonTest.logMessage_namesReasonAndErrorClass`: `downloadFailureMessage(AssetPath("images/v2/hero.webp"), UnknownHostException("assets.example.com"))` bằng `"Could not download images/v2/hero.webp: NETWORK (UnknownHostException)"`; thông điệp không chứa tên máy chủ |
| Log trên máy | Kiểm tra thủ công | Bản staging: chế độ máy bay, xóa dữ liệu app, mở app (loading bật): `adb logcat \| grep ' Assets'` có dòng `NETWORK (UnknownHostException)` |

**Ngoại lệ viết test trước:** dòng log trong `AssetsModule.kt` (gọi `android.util.Log`, không test trên JVM); nội dung thông điệp được test qua `downloadFailureMessage`.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, cùng worktree tạm ngoài repo với T-016, T-017 (`origin/main`, commit `80cc3e2`), không commit, đã xóa. Code thử: `AssetFailureReason.kt` (chép từ app, có `downloadFailureMessage`), `HttpStatusException`, `Unavailable(reason)`, sửa `DefaultRemoteAssets`, `AssetsModule`, `DefaultRemoteAssetsTest`, `DefaultPreparationRepositoryTest`; test tạm `AssetFailureReasonTest` (4 test gộp các trường hợp của mục 4).
- **Lệnh:** `scripts/check.sh` (`ANDROID_HOME=$HOME/Library/Android/sdk`, `JAVA_HOME` của Android Studio).
- **Kết quả:** qua: "./core/assets: 14 test, 0 lỗi; ./feature/loading: 27 test, 0 lỗi", lint và build staging qua, "Cổng chất lượng: QUA". Thông điệp log đúng chuỗi mong đợi. Không gặp lỗi biên dịch ngoài ba chỗ `Unavailable` đã liệt kê.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** module core dùng chung `:core:assets` (mục 7): đổi API công khai `AssetResult.Unavailable`. Không chạm quảng cáo, quyền, SDK, dữ liệu thu thập, cấu hình build.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một module core và một file test của `:feature:loading` |
| B. Độ rõ của yêu cầu | 0 | Code app đã chạy, issue nêu rõ |
| C. Khả năng kiểm chứng tự động | 0 | Toàn bộ là logic thuần có unit test |
| D. Ảnh hưởng chéo | 2 | API công khai đổi: mọi nơi dùng `Unavailable` như đối tượng phải sửa; T-017 dựa vào lý do này; app đã tạo từ dự án mẫu phải sửa khi kéo về |
| E. Độ mới của kỹ thuật | 0 | Kotlin thuần, mẫu có sẵn |
| **Tổng** | 3 | **Mức: Cao** (chạm điểm chặn module core) |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `RemoteAssets.kt` (API công khai), `DefaultRemoteAssets.kt`, `AssetsModule.kt`, `AssetDownloader.kt`.
- Task khác: T-017 sửa tiếp `DefaultRemoteAssets.kt`, `AssetsModule.kt`, `core/assets/build.gradle.kts` (báo lỗi mạng), nên T-018 chạy trước T-017. Không phụ thuộc T-007, T-016; không xung đột file với T-007 đến T-016.
- Đề xuất thứ tự chạy: task độc lập, chạy sớm trong phiên, trước T-017.

## 7. Điều kiện dừng riêng

- Ngoài ba chỗ đã liệt kê, còn nơi khác dùng `AssetResult.Unavailable` như một đối tượng (ví dụ code mới merge sau khi lập plan) nằm ngoài danh sách file.
- Test có sẵn của `:feature:loading` đổi kết quả (không chỉ lỗi biên dịch).

## 8. Câu hỏi cho người duyệt

1. **Feature T-018 thuộc `network` hay `assets`.** Dự án mẫu chưa có thư mục feature cho `:core:assets`; ticket đặt ở `network` vì phục vụ việc báo lỗi mạng (spec network KB-3). Đề xuất: giữ ở `network`.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.
2. **Quá thời gian là `NETWORK`.** Như app. Spec KB-3 sửa 2026-10-23: lỗi mạng, kể cả quá thời gian, hiện popup ngay (T-017). Đề xuất: giữ như app.
   - Trả lời (anh Nhật, 2026-10-23): theo đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev, risk-scoring.md mục 3; plan đã qua hai lần rà soát độc lập trước khi chạy, ghi ở RUN-20261023-01)
- **Ghi chú:** AI ghi theo lời anh Nhật, 2026-10-23; câu hỏi ở mục 8 đã trả lời ngay tại đó.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
