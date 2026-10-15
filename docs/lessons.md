# SỔ BÀI HỌC

Sổ này ghi những điều nhóm và AI học được khi làm việc, và đã làm gì với chúng. Đây là đầu vào của buổi retro sau mỗi phiên tự chạy (auto-run.md, mục 6).

## Cách ghi

- Mỗi bài học một mục, mã `L-NNN` tăng dần. Chỉ thêm mục mới hoặc cập nhật dòng **Trạng thái**, **Áp dụng**, **Kiểm chứng** của mục cũ; không sửa phần mô tả.
- Trạng thái:
  - `proposed`: AI hoặc người vừa ghi, chưa ai quyết định.
  - `accepted`: retro đồng ý làm, chưa làm.
  - `applied`: đã làm, ghi PR ở dòng **Áp dụng**.
  - `verified`: phiên sau cho thấy cải tiến có hiệu quả, ghi bằng chứng ở dòng **Kiểm chứng**.
  - `rejected`: retro quyết định không làm, ghi lý do.
- Loại: `quy trình` (auto-run, docs/CLAUDE.md, mẫu tài liệu), `môi trường` (cloud, CI, script), `quy tắc code` (CLAUDE.md), `plan` (cách viết plan, test), `rủi ro` (thang điểm).
- Phạm vi: `chung` (đúng với mọi app, làm trong repo dự án mẫu trước), `app` (chỉ app này).
- Chỉ người chuyển trạng thái sang `accepted`, `rejected` hay `verified`. AI chỉ ghi `proposed`, và ghi `applied` khi PR cải tiến đã merge.

Bài học `chung` được làm trong repo dự án mẫu. Các app đã tạo trước đó đọc sổ của dự án mẫu để chép những cải tiến cần thiết về (chưa có cách đồng bộ tự động).

## Bài học

<!-- Các bài học ghi dưới dòng này. -->

### L-001. Báo cáo phiên nằm trong PR của task, không lên nhánh chính khi task dừng

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** báo cáo phiên, nhật ký plan và trạng thái được commit vào PR #13 của T-001. Task dừng nên PR không merge; trên nhánh chính danh sách chạy vẫn `approved`, có thể bị chạy lại.
- **Cải tiến:** tài liệu của phiên đi qua nhánh và PR báo cáo riêng, phiên tự merge; danh sách chạy chỉ chạy một lần.
- **Trạng thái:** applied
- **Áp dụng:** PR #14
- **Kiểm chứng:** RUN-20261015-02 tự merge PR báo cáo #16, danh sách chạy chuyển `finished` trên nhánh chính (AI ghi bằng chứng, chờ retro xác nhận `verified`).

### L-002. Routine làm việc trên nhánh tên ngẫu nhiên

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên dùng nhánh `claude/trusting-ritchie-kr8pph` do routine tạo, khác `claude/T-NNN-<tu-khoa>`; một phiên nhiều task không có nhánh riêng cho từng task.
- **Cải tiến:** prompt routine có dòng "Nhánh"; auto-run.md yêu cầu tạo nhánh riêng cho từng task.
- **Trạng thái:** applied
- **Áp dụng:** PR #14, prompt routine cập nhật
- **Kiểm chứng:** RUN-20261015-02 dùng `claude/T-001-loading` và `claude/RUN-20261015-02-report` (AI ghi bằng chứng, chờ retro xác nhận `verified`).

### L-003. Test trên emulator khẳng định một màn hình thoáng qua

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-001 phiên bản 1 yêu cầu `AppLaunchTest` thấy màn hình loading; màn hình đã qua hoặc bị che lúc test kiểm tra, `ui-test` đỏ 3 lần.
- **Cải tiến:** test emulator chỉ kiểm tra trạng thái ổn định sau khi chờ có giới hạn (CLAUDE.md, bước 10; mẫu plan).
- **Trạng thái:** applied
- **Áp dụng:** PR #14, plan T-001 phiên bản 2 (PR #15)
- **Kiểm chứng:** chờ phiên chạy T-001 phiên bản 2.

### L-004. Hộp thoại đồng ý quảng cáo che app mới cài trên staging

- **Ngày, nguồn:** 2026-10-15, phân tích sau RUN-20261015-01
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** bản stagingDebug giả lập vùng EEA nên hộp thoại UMP hiện sau vài giây, phủ lên app; `assertIsDisplayed` báo "không hiển thị".
- **Cải tiến:** test emulator kiểm tra nút có trong cây giao diện, không dùng `assertIsDisplayed` (CLAUDE.md, bước 10).
- **Trạng thái:** applied
- **Áp dụng:** PR #14
- **Kiểm chứng:** chờ phiên chạy T-001 phiên bản 2.

### L-005. Không đọc được vì sao test emulator đỏ

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** Gradle cắt thông báo lỗi; app không ghi log; CI không giữ logcat. Phiên phải thêm lệnh in chẩn đoán vào test và chạy lại.
- **Cải tiến:** `ui-test` ghi logcat, in toàn văn lỗi và log app khi đỏ, lưu artifact; module `:core:logging` (ADR-0019).
- **Trạng thái:** applied
- **Áp dụng:** PR #14
- **Kiểm chứng:** chờ lần `ui-test` đỏ đầu tiên.

### L-006. Nhật ký plan ghi trước khi biết kết quả CI

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên ghi "gửi để merge" rồi phải thêm mục "thay cho mục ngay trên" khi CI đỏ.
- **Cải tiến:** chỉ ghi nhật ký khi task có kết quả cuối (auto-run.md, bước 10).
- **Trạng thái:** applied
- **Áp dụng:** PR #14
- **Kiểm chứng:** RUN-20261015-02 ghi một mục nhật ký sau khi có kết quả (AI ghi bằng chứng, chờ retro xác nhận `verified`).

### L-007. Maven Central trả 429 cho môi trường cloud

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** Gradle nhận 429 từ `repo.maven.apache.org` cả 3 lần thử; phiên kết thúc ở bước 3.
- **Cải tiến:** setup script cho Gradle tải Maven Central qua mirror của Google; đầu phiên chạy `scripts/check.sh` để tải sẵn thư viện.
- **Trạng thái:** applied
- **Áp dụng:** PR #17, setup script của môi trường đã dán lại
- **Kiểm chứng:** chờ RUN-20261015-03: log Gradle không còn `repo.maven.apache.org`.

### L-008. Thử lại khi lỗi hạ tầng quá ngắn cho phiên qua đêm

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** 3 lần thử trong khoảng 3 phút rồi kết thúc phiên, trong khi phiên qua đêm còn nhiều giờ.
- **Cải tiến:** thử lại sau 1, 2, 4, 8, 15 phút (auto-run.md, bước 5).
- **Trạng thái:** applied
- **Áp dụng:** PR #17
- **Kiểm chứng:** chờ lần lỗi hạ tầng tiếp theo.

### L-009. Mất phần việc khi phiên kết thúc sớm

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** test đã viết của T-001 chỉ nằm trên nhánh cục bộ, mất khi phiên kết thúc.
- **Cải tiến:** kết thúc vì hạ tầng hay hạn mức thì đẩy nhánh đang làm, lần sau tiếp tục trên nhánh đó.
- **Trạng thái:** applied
- **Áp dụng:** PR #17
- **Kiểm chứng:** chờ lần kết thúc sớm tiếp theo.

### L-010. Routine chạy model khác dự kiến

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** commit của phiên ghi `Claude Sonnet 5.5`; hướng dẫn dự kiến Opus 5.5 cho task rủi ro cao chạy qua đêm.
- **Cải tiến:** ghi model trong hướng dẫn tạo routine; người chọn model trong routine.
- **Trạng thái:** accepted
- **Áp dụng:** PR #19 (tài liệu); đổi model trong routine do người làm
- **Kiểm chứng:** commit của phiên sau ghi model.

### L-011. API kích hoạt routine cần header beta

- **Ngày, nguồn:** 2026-10-15, đối chiếu tài liệu Claude Code trước phiên đầu tiên
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `start-run.yml` thiếu header `anthropic-beta`; lần thử bằng mã giả dừng ở lỗi 401 nên không lộ ra.
- **Cải tiến:** thêm header.
- **Trạng thái:** applied
- **Áp dụng:** PR #11
- **Kiểm chứng:** `start-run` của RUN-20261015-01 và RUN-20261015-02 thành công (AI ghi bằng chứng, chờ retro xác nhận `verified`).

### L-012. Phiên cloud không dùng được `gh pr` và `gh issue`

- **Ngày, nguồn:** 2026-10-15, đối chiếu tài liệu Claude Code
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** proxy GitHub của môi trường cloud chặn GraphQL.
- **Cải tiến:** mọi thao tác GitHub trong phiên dùng `gh api` (auto-run.md, bước 7).
- **Trạng thái:** applied
- **Áp dụng:** PR #11
- **Kiểm chứng:** RUN-20261015-01 mở PR #13, RUN-20261015-02 mở và merge PR #16 (AI ghi bằng chứng, chờ retro xác nhận `verified`).

### L-013. Hướng dẫn thiết lập rời rạc, dev nhóm khác phải hỏi

- **Ngày, nguồn:** 2026-10-15, anh Nhật
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** các bước tạo tài khoản dịch vụ, R2, bí mật, routine nằm rải rác hoặc chỉ có trong trao đổi.
- **Cải tiến:** `docs/ops/setup-new-app.md` theo thứ tự, có bước kiểm tra và bảng sự cố.
- **Trạng thái:** applied
- **Áp dụng:** PR #19
- **Kiểm chứng:** một dev nhóm khác thiết lập app thử theo tài liệu mà không phải hỏi.
