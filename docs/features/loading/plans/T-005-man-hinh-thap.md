---
ticket: T-005
title: Màn hình loading dùng được trên màn hình thấp
status: approved
version: 1
risk_score: 4
risk_level: low
blocking_points: []
run_order:
reviewer: Nhật
cross_reviewer: Nhật (vì là app mẫu)
approved_at: 2026-10-17
---

# PLAN T-005: MÀN HÌNH LOADING DÙNG ĐƯỢC TRÊN MÀN HÌNH THẤP

## 1. Tóm tắt

Sửa FB-20261017-01: trên màn hình 320 x 640 dp, ở trạng thái lỗi, chân trang đè lời hướng dẫn và nút "Thử lại" bị cắt, vì nội dung luôn bắt đầu ở 25% chiều cao, không cuộn, còn chân trang ghim riêng ở đáy. Nội dung và chân trang chuyển vào chung một cột cuộn được; khoảng trống phía trên co lại trên màn hình thấp. Kiểm chứng bằng test giao diện và screenshot test ở khung thấp `w360dp-h640dp-xxhdpi` (L-028), và bốn ảnh chuẩn ở khung 411 x 891 giữ nguyên.

## 2. Hiểu yêu cầu

Ticket xử lý phản hồi `features/loading/feedback/FB-20261017-01-man-hinh-thap.md` (phát hiện ở phiên thử `docs/test-sessions/TS-20261017-01.md`), cho các kịch bản sau trong `features/loading/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-2 | Khi lỗi thì có nút "Thử lại" | Nút phải thấy và bấm được trên mọi cỡ màn hình điện thoại, không chỉ khung chuẩn |
| KB-3 | Báo lỗi như KB-2 | Như trên |

Thiết kế tham chiếu: `features/loading/design/README.md` (mục 3: vị trí theo chiều dọc; mục 6: được phép khác). Thiết kế chỉ có khung 411 x 891; hành vi ở màn hình thấp do plan này quy định và ghi lại vào README.

ADR liên quan: ADR-0008, ADR-0020. Bài học: L-028.

## 3. Giải pháp

### 3.1. Cách làm

**Bố cục (`LoadingScreen.kt`).** Trong `BoxWithConstraints` hiện có:

- Một `Column` ngoài: `Modifier.fillMaxSize().verticalScroll(rememberScrollState())`.
- Bên trong, một `Column` với `Modifier.fillMaxWidth().heightIn(min = maxHeight)` và `verticalArrangement = Arrangement.SpaceBetween`, gồm hai phần:
  1. Cột nội dung như hiện tại (logo tới khối lỗi hoặc dòng hướng dẫn), lề trái phải giữ `Spacing.xl + Spacing.md`, khoảng trống phía trên là `topSpace`.
  2. `LoadingFooter`, căn giữa ngang, `padding(top = Spacing.lg, bottom = Spacing.xl)`.
- `topSpace`: màn hình cao từ `TALL_SCREEN_MIN_HEIGHT` (800 dp) trở lên thì `maxHeight * CONTENT_TOP_FRACTION` (0,25, như thiết kế); thấp hơn thì `maxHeight * SHORT_SCREEN_TOP_FRACTION` (0,08). Hai hằng số mới, `private`, có chú thích lý do (L-028, FB-20261017-01).
- Không dùng `Modifier.weight` trong cột cuộn. `SpaceBetween` với chiều cao tối thiểu bằng màn hình giữ chân trang ở đáy khi nội dung ngắn, và đẩy chân trang xuống dưới nội dung khi nội dung dài.
- Vị trí logo vẫn không đổi giữa hai trạng thái trên cùng một màn hình (`topSpace` không phụ thuộc trạng thái).

Ở khung 411 x 891, `topSpace` và vị trí chân trang bằng đúng như trước, nên bốn ảnh chuẩn hiện có phải giữ nguyên.

**Thiết kế (`design/README.md`).** Thêm vào mục 6 (được phép khác) một dòng: "Màn hình thấp hơn 800 dp: khoảng trống phía trên co lại còn 8% chiều cao; nội dung và chân trang cuộn chung khi dài hơn màn hình (T-005, L-028)". Thêm vào mục 7 tiêu chí "Dùng được ở màn hình thấp (360 x 640)". Không sửa mục khác.

**Nơi khác đọc các file trên** (docs/CLAUDE.md, mục Khi viết plan), không cần sửa:

- `docs/features/loading/design/README.md`: `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenTest.kt` chỉ nhắc mục 2 (bảng chữ) trong chú thích; plan chỉ thêm dòng ở mục 6 và 7.

**Commit test và commit code** (L-025): commit test chỉ gồm `LoadingScreenTest.kt` và `LoadingScreenScreenshotTest.kt`; commit code gồm `LoadingScreen.kt`, `design/README.md` và hai ảnh chuẩn mới.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/loading/src/main/kotlin/com/vglstudio/template/feature/loading/LoadingScreen.kt` | Cột cuộn chung cho nội dung và chân trang, `topSpace` theo chiều cao, hai hằng số |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenTest.kt` | Hai test ở khung thấp (mục 4) |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/template/feature/loading/LoadingScreenScreenshotTest.kt` | `failed_small`, `progress_small` ở khung thấp |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_failed_small.png` | Trạng thái lỗi, khung thấp, giao diện sáng |
| Tạo | `feature/loading/src/test/screenshots/LoadingScreen_progress_small.png` | Đang tải, khung thấp, giao diện sáng |
| Sửa | `docs/features/loading/design/README.md` | Chỉ mục 6 và mục 7, như mục 3.1 |

Không ghi lại bốn ảnh chuẩn hiện có (`LoadingScreen_progress_light.png`, `LoadingScreen_progress_dark.png`, `LoadingScreen_failed_light.png`, `LoadingScreen_failed_dark.png`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: Không chạm.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-2: nút "Thử lại" thấy và bấm được trên màn hình thấp | Compose UI test (Robolectric), `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` trên hàm test | `LoadingScreenTest.failed_onShortScreen_retryButtonFitsOnScreen`: `Failed(0.68f)`; đáy của `RETRY` (`getBoundsInRoot().bottom`) không lớn hơn đáy của `SCREEN`; bấm `RETRY` gọi `onRetry` một lần. Viết trước: thất bại vì nút tràn khỏi màn hình |
| Chữ không chồng nhau trên màn hình thấp | Compose UI test, khung thấp | `LoadingScreenTest.failed_onShortScreen_footerBelowError`: đỉnh của dòng chân trang (tìm theo chữ `loading_footer`) không nhỏ hơn đáy của `ERROR`. Viết trước: thất bại vì chân trang đè khối lỗi |
| Khung chuẩn giữ nguyên | Screenshot test | Bốn ảnh chuẩn hiện có so khớp, không ghi lại; `LoadingScreenTest` hiện có (kể cả `appName_staysInPlace_whenFailed`) qua, không sửa |
| Màn hình thấp (L-028) | Screenshot test, khung `w360dp-h640dp-xxhdpi` | `LoadingScreenScreenshotTest.failed_small`, `progress_small` ghi `LoadingScreen_failed_small.png`, `LoadingScreen_progress_small.png`, `appName = "Todo App"`, tiến độ 0,68, giao diện sáng |
| Trên máy thật và emulator | Kiểm tra thủ công, phiên thử | Sau khi phát staging: `device-session.yml` (emulator 320 x 640), xem `offline.png`; người thử trên máy thấp nếu có |

**Ngoại lệ viết test trước:** hai ảnh chuẩn mới và dòng thêm vào `design/README.md` không viết trước được; hai test giao diện ở trên được viết trước và phải thất bại đúng lý do.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một file code và test trong một module feature |
| B. Độ rõ của yêu cầu | 1 | Lỗi rõ, có ảnh; hành vi ở màn hình thấp do plan quy định, thiết kế chưa có |
| C. Khả năng kiểm chứng tự động | 0 | Cả hai tiêu chí có test giao diện; khung chuẩn được ảnh chuẩn giữ |
| D. Ảnh hưởng chéo | 0 | Chỉ trong `:feature:loading` |
| E. Độ mới của kỹ thuật | 2 | Cột cuộn có chiều cao tối thiểu và `SpaceBetween` chưa có trong dự án, nhưng phổ biến |
| **Tổng** | 4 | **Mức: Thấp** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: Không có.
- Task khác trong sprint có thể xung đột: Không có.
- Đề xuất thứ tự chạy: bất kỳ.

## 7. Điều kiện dừng riêng

1. Một trong bốn ảnh chuẩn ở khung 411 x 891 khác đi: dừng, không ghi lại ảnh.
2. Ở khung thấp, nội dung vẫn tràn khi không cuộn và test `retryButtonFitsOnScreen` chỉ qua được nhờ cuộn: dừng, ghi số đo, để người chọn tỷ lệ `SHORT_SCREEN_TOP_FRACTION` khác.

## 8. Câu hỏi cho người duyệt

1. **Phân loại FB-20261017-01:** AI đề xuất "lỗi so với spec", xử lý bằng ticket mới T-005 thay vì sửa plan T-003 (đã `done`). Anh xác nhận ở phần Phân loại của phản hồi.
- Trả lời: Đồng ý với AI.
2. **Ngưỡng 800 dp và tỷ lệ 8%:** ước tính trạng thái lỗi cao khoảng 620 dp ở chiều rộng 360. Trong khung test 360 x 640 (không có thanh hệ thống) nội dung vừa màn hình; trên máy thật, sau khi trừ thanh trạng thái và thanh điều hướng còn khoảng 570 dp, nên có thể phải cuộn một chút để thấy hết nút. Đề xuất: đồng ý; nếu muốn nút luôn thấy mà không cần cuộn trên máy 640 dp thì cần rút gọn nội dung (ví dụ ẩn khẩu hiệu ở màn hình thấp), là đổi thiết kế.
- Trả lời: Đồng ý với AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (vì là app mẫu)
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
