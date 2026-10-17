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

Bài học `chung` ghi ở app thì được đưa về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`). Ở dự án mẫu, bài học từ app ghi nguồn là app và số issue. Các app đã tạo trước đó đọc sổ của dự án mẫu để chép những cải tiến cần thiết về (chưa có cách đồng bộ tự động).

## Bài học

<!-- Các bài học ghi dưới dòng này. -->

### L-001. Báo cáo phiên nằm trong PR của task, không lên nhánh chính khi task dừng

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** báo cáo phiên, nhật ký plan và trạng thái được commit vào PR #13 của T-001. Task dừng nên PR không merge; trên nhánh chính danh sách chạy vẫn `approved`, có thể bị chạy lại.
- **Cải tiến:** tài liệu của phiên đi qua nhánh và PR báo cáo riêng, phiên tự merge; danh sách chạy chỉ chạy một lần.
- **Trạng thái:** verified
- **Áp dụng:** PR #14
- **Kiểm chứng:** RUN-20261015-02 và RUN-20261015-03 tự merge PR báo cáo (#16, #22), danh sách chạy chuyển `finished` trên nhánh chính. Retro 2026-10-15 xác nhận.

### L-002. Routine làm việc trên nhánh tên ngẫu nhiên

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên dùng nhánh `claude/trusting-ritchie-kr8pph` do routine tạo, khác `claude/T-NNN-<tu-khoa>`; một phiên nhiều task không có nhánh riêng cho từng task.
- **Cải tiến:** prompt routine có dòng "Nhánh"; auto-run.md yêu cầu tạo nhánh riêng cho từng task.
- **Trạng thái:** verified
- **Áp dụng:** PR #14, prompt routine cập nhật
- **Kiểm chứng:** RUN-20261015-02 và RUN-20261015-03 dùng `claude/T-001-loading` và nhánh báo cáo riêng. Retro 2026-10-15 xác nhận.

### L-003. Test trên emulator khẳng định một màn hình thoáng qua

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-001 phiên bản 1 yêu cầu `AppLaunchTest` thấy màn hình loading; màn hình đã qua hoặc bị che lúc test kiểm tra, `ui-test` đỏ 3 lần.
- **Cải tiến:** test emulator chỉ kiểm tra trạng thái ổn định sau khi chờ có giới hạn (CLAUDE.md, bước 10; mẫu plan).
- **Trạng thái:** verified
- **Áp dụng:** PR #14, plan T-001 phiên bản 2 (PR #15)
- **Kiểm chứng:** `AppLaunchTest` chỉ chờ màn hình mẫu có trong cây giao diện; `ui-test` xanh ngay lần đầu trên PR #21 (RUN-20261015-03). Retro 2026-10-15 xác nhận.

### L-004. Hộp thoại đồng ý quảng cáo che app mới cài trên staging

- **Ngày, nguồn:** 2026-10-15, phân tích sau RUN-20261015-01
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** bản stagingDebug giả lập vùng EEA nên hộp thoại UMP hiện sau vài giây, phủ lên app; `assertIsDisplayed` báo "không hiển thị".
- **Cải tiến:** test emulator kiểm tra nút có trong cây giao diện, không dùng `assertIsDisplayed` (CLAUDE.md, bước 10).
- **Trạng thái:** verified
- **Áp dụng:** PR #14
- **Kiểm chứng:** `ui-test` xanh ngay lần đầu trên PR #21 dù bản staging hiện hộp thoại đồng ý (RUN-20261015-03). Retro 2026-10-15 xác nhận.

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
- **Trạng thái:** verified
- **Áp dụng:** PR #14
- **Kiểm chứng:** RUN-20261015-02 và RUN-20261015-03 ghi nhật ký plan một lần, sau khi có kết quả. Retro 2026-10-15 xác nhận.

### L-007. Maven Central trả 429 cho môi trường cloud

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** Gradle nhận 429 từ `repo.maven.apache.org` cả 3 lần thử; phiên kết thúc ở bước 3.
- **Cải tiến:** setup script cho Gradle tải Maven Central qua mirror của Google; đầu phiên chạy `scripts/check.sh` để tải sẵn thư viện.
- **Trạng thái:** applied
- **Áp dụng:** PR #17, setup script của môi trường đã dán lại
- **Kiểm chứng:** RUN-20261015-03: log Gradle không còn `repo.maven.apache.org`; Robolectric vẫn tải thẳng từ Maven Central, xử lý ở L-014.

### L-008. Thử lại khi lỗi hạ tầng quá ngắn cho phiên qua đêm

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-02
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** 3 lần thử trong khoảng 3 phút rồi kết thúc phiên, trong khi phiên qua đêm còn nhiều giờ.
- **Cải tiến:** thử lại sau 1, 2, 4, 8, 15 phút (auto-run.md, bước 5).
- **Trạng thái:** applied
- **Áp dụng:** PR #17
- **Kiểm chứng:** RUN-20261016-01: phiên chờ đủ lịch (khoảng 30 phút) nhưng lỗi kéo dài hơn; Gradle ghi nhớ kết quả lỗi nên các lần thử giữa lịch không gọi lại mạng (L-018).

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
- **Trạng thái:** verified
- **Áp dụng:** PR #19 (tài liệu); anh Nhật đổi model của routine sang Opus 5.5
- **Kiểm chứng:** Commit của RUN-20261015-03 ghi `Claude Opus 5.5`. Retro 2026-10-15 xác nhận.

### L-011. API kích hoạt routine cần header beta

- **Ngày, nguồn:** 2026-10-15, đối chiếu tài liệu Claude Code trước phiên đầu tiên
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `start-run.yml` thiếu header `anthropic-beta`; lần thử bằng mã giả dừng ở lỗi 401 nên không lộ ra.
- **Cải tiến:** thêm header.
- **Trạng thái:** verified
- **Áp dụng:** PR #11
- **Kiểm chứng:** `start-run` thành công ở RUN-20261015-01, 02, 03. Retro 2026-10-15 xác nhận.

### L-012. Phiên cloud không dùng được `gh pr` và `gh issue`

- **Ngày, nguồn:** 2026-10-15, đối chiếu tài liệu Claude Code
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** proxy GitHub của môi trường cloud chặn GraphQL.
- **Cải tiến:** mọi thao tác GitHub trong phiên dùng `gh api` (auto-run.md, bước 7).
- **Trạng thái:** verified
- **Áp dụng:** PR #11
- **Kiểm chứng:** RUN-20261015-03 mở PR #21, #22, khởi động `staging.yml` và đọc issue `main-broken` bằng `gh api`. Retro 2026-10-15 xác nhận.

### L-013. Hướng dẫn thiết lập rời rạc, dev nhóm khác phải hỏi

- **Ngày, nguồn:** 2026-10-15, anh Nhật
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** các bước tạo tài khoản dịch vụ, R2, bí mật, routine nằm rải rác hoặc chỉ có trong trao đổi.
- **Cải tiến:** `docs/ops/setup-new-app.md` theo thứ tự, có bước kiểm tra và bảng sự cố.
- **Trạng thái:** applied
- **Áp dụng:** PR #19
- **Kiểm chứng:** một dev nhóm khác thiết lập app thử theo tài liệu mà không phải hỏi.

### L-014. Robolectric tải thẳng từ Maven Central, không qua mirror

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-03
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `scripts/check.sh` đầu phiên đỏ vì Robolectric tự tải `android-all-instrumented` từ `repo1.maven.org` và nhận 429. Mirror của setup script chỉ áp dụng cho Gradle. Phiên đặt `-Drobolectric.dependency.repo.url=https://maven-central.storage-download.googleapis.com/maven2/` vào `JAVA_TOOL_OPTIONS` rồi chạy lại thì qua.
- **Cải tiến:** đề xuất `scripts/cloud-setup.sh` đặt sẵn thuộc tính đó; auto-run.md bước 5 ghi rõ được dùng mirror thay cho lịch thử lại.
- **Trạng thái:** applied
- **Áp dụng:** PR #23, setup script của môi trường cần dán lại
- **Kiểm chứng:** RUN-20261016-01: chưa kiểm chứng được, phiên không chạy tới Robolectric; `JAVA_TOOL_OPTIONS` của môi trường không có thuộc tính mirror (L-019). RUN-20261016-02: có hiệu quả. `scripts/check.sh` đầu phiên qua lần đầu (8 phút 38 giây), test Robolectric chạy được, log không có `repo.maven.apache.org` hay `repo1.maven.org`.

### L-015. Người merge vào nhánh chính khi phiên đang chạy

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-03
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** PR #19 và #20 (tài liệu) được merge trong lúc phiên chạy. `merge-if-green.sh` báo nhánh task chậm 4 commit, phiên phải cập nhật nhánh và chạy lại CI một vòng (khoảng 11 phút).
- **Cải tiến:** đề xuất nhắc lại quy tắc khi kích hoạt phiên, hoặc cho phép PR chỉ có tài liệu và ghi rõ chi phí một vòng CI.
- **Trạng thái:** applied
- **Áp dụng:** PR #23. Retro chọn cách chặn hẳn thay cho hai đề xuất trên: `merge-if-green.sh` từ chối mọi PR không phải của phiên khi danh sách chạy trên nhánh báo cáo đang `running`.
- **Kiểm chứng:**

### L-016. So sánh đúng đối tượng ngoại lệ trong test coroutine

- **Ngày, nguồn:** 2026-10-15, RUN-20261015-03
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** `RemoteConfigRefresherTest.fetchFails_logsWarning` so sánh `assertEquals(error, entry.error)` và thất bại, vì coroutine chép lại ngoại lệ khi khôi phục stack trace. Phải sửa chi tiết test sau khi viết code.
- **Cải tiến:** đề xuất CLAUDE.md, bước 10: unit test kiểm tra loại lỗi, không so sánh đúng đối tượng lỗi.
- **Trạng thái:** applied
- **Áp dụng:** PR #23
- **Kiểm chứng:**

### L-017. Commit trong phiên có người theo dõi kéo theo file riêng của người

- **Ngày, nguồn:** 2026-10-15, PR #25
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** AI dùng `git add -A`, đưa hai thư mục chưa từng commit của người (`.vscode/`, `build-logic/convention/bin/`) vào PR #25; phải đẩy đè nhánh để gỡ.
- **Cải tiến:** đề xuất ghi vào CLAUDE.md, mục Quy ước code: chỉ `git add` theo đường dẫn, xem danh sách file của PR trước khi đẩy; cân nhắc thêm hai thư mục đó vào `.gitignore`.
- **Trạng thái:** applied
- **Áp dụng:** PR #35: CLAUDE.md, mục Quy ước code; `.vscode/` và `build-logic/convention/bin/` vào `.gitignore`. Retro 2026-10-17 nhận
- **Kiểm chứng:**

### L-018. Thư viện plugin của build-logic tải qua plugin portal, không qua mirror

- **Ngày, nguồn:** 2026-10-16, RUN-20261016-01
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `scripts/check.sh` đầu phiên đỏ suốt lịch thử lại (15:30 tới 16:03 UTC). Plugin `kotlin-dsl` của `build-logic` cần `kotlin-stdlib` và các thư viện Kotlin, tải qua `gradlePluginPortal()`; kho này chuyển hướng sang `repo.maven.apache.org`, nơi trả 429 rồi 404. Init script của setup script chỉ đổi kho `mavenCentral()`. Gradle ghi nhớ kết quả lỗi, các lần sau báo "Could not find" mà không gọi lại mạng. Phiên thử thêm init script riêng của phiên để dùng mirror, nhưng lệnh chạy sau đó bị bộ phân loại quyền chặn. Phiên kết thúc, không chạy task nào.
- **Cải tiến:** đề xuất `scripts/cloud-setup.sh` thêm mirror vào đầu `settings.pluginManagement.repositories` (giữ `gradlePluginPortal()` phía sau); auto-run.md bước 5 ghi: thử lại sau lỗi tải thì dùng `--refresh-dependencies`, và phiên không tự thêm cấu hình mirror được nên mirror phải có sẵn trong setup script.
- **Trạng thái:** verified
- **Áp dụng:** PR #32
- **Kiểm chứng:** RUN-20261016-02: `scripts/cloud-env-check.sh` qua, `scripts/check.sh` đầu phiên xanh ngay lần đầu, log không có `repo.maven.apache.org` hay `repo1.maven.org`. Retro 2026-10-17 xác nhận.

### L-019. JAVA_TOOL_OPTIONS của phiên thiếu mirror của Robolectric

- **Ngày, nguồn:** 2026-10-16, RUN-20261016-01
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** môi trường đặt `JAVA_TOOL_OPTIONS` cho proxy; thuộc tính `robolectric.dependency.repo.url` của L-014 không có, và `~/.bashrc` không có dòng `export JAVA_TOOL_OPTIONS=...` của setup script. Phiên phải tự nối thêm thuộc tính bằng biến môi trường.
- **Cải tiến:** đề xuất setup script nối thêm vào `JAVA_TOOL_OPTIONS` hiện có thay vì gán đè, đặt ở file mà shell không tương tác cũng đọc; kiểm tra môi trường đã dán lại setup script mới nhất.
- **Trạng thái:** verified
- **Áp dụng:** PR #32
- **Kiểm chứng:** RUN-20261016-02: `scripts/cloud-env-check.sh` qua, `scripts/check.sh` đầu phiên xanh ngay lần đầu, log không có `repo.maven.apache.org` hay `repo1.maven.org`. Retro 2026-10-17 xác nhận.

### L-020. Test của figma-export đọc tokens.json thật, chặn mọi thay đổi hệ thống thiết kế

- **Ngày, nguồn:** 2026-10-16, RUN-20261016-02
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** T-002 đổi hệ thống thiết kế đúng plan, `tokens.json` sinh lại khớp. `scripts/check.sh` đỏ vì 3 test của `figma-export` trong `scripts/tests/run.sh` (dòng 120 đến 122) chạy với `docs/design-system/tokens.json` thật và mong giá trị cũ (`titleMedium` đậm 500, nền `#FEF7FF`). File này ngoài danh sách file của plan, trong `scripts/`, nên task dừng ở bước 5 (điều kiện dừng 1 và 5). Plan không thấy được phụ thuộc này.
- **Cải tiến:** đề xuất `figma-export.py` nhận đường dẫn `tokens.json` qua biến môi trường (như `FIGMA_EXPORT_FEATURES_DIR`), `scripts/tests/run.sh` dùng `scripts/tests/fixtures/tokens.json` cố định, để test của script không đổi theo thương hiệu của từng app. Khi viết plan đổi một file dùng chung (`tokens.json`, ảnh chuẩn), tìm mọi nơi đọc file đó (`grep -rn tokens.json scripts/ .github/`) và ghi vào danh sách file.
- **Trạng thái:** applied
- **Áp dụng:** PR #35: `figma-export.py` đọc `FIGMA_EXPORT_TOKENS`, test dùng `scripts/tests/fixtures/tokens.json`; docs/CLAUDE.md, mục Khi viết plan: tìm mọi nơi đọc file dùng chung. Retro 2026-10-17 nhận
- **Kiểm chứng:**

### L-021. Đo kích thước thành phần bằng Robolectric cần chế độ đồ họa thật

- **Ngày, nguồn:** 2026-10-16, RUN-20261016-02
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** `ComponentsTest` của T-002 kiểm tra nút cao tối thiểu 52 dp. Ở chế độ đồ họa mặc định của Robolectric, chữ đo cao 36 dp nên nút cũ đã cao 52 dp và test qua khi chưa có code. Thêm `@GraphicsMode(GraphicsMode.Mode.NATIVE)` thì chữ đo đúng và test thất bại đúng lý do. Cũng trong task, `LinearProgressIndicator` tự nới vùng ngữ nghĩa thêm 10 dp mỗi phía, nên `assertHeightIsEqualTo` trên chính thanh đo được 28 dp thay vì 8 dp.
- **Cải tiến:** đề xuất CLAUDE.md, mục Viết một feature mới, bước 10: test giao diện có kiểm tra kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`; thành phần Material tự nới vùng chạm hay vùng ngữ nghĩa thì đo qua phần tử bọc ngoài. Mẫu plan, mục Kiểm thử: ghi rõ đo kích thước trên nút nào.
- **Trạng thái:** applied
- **Áp dụng:** PR #35: CLAUDE.md, mục Viết một feature mới, bước 10. Retro 2026-10-17 nhận
- **Kiểm chứng:**

### L-022. Lỗi của plan và môi trường chỉ lộ ra khi phiên qua đêm đang chạy

- **Ngày, nguồn:** 2026-10-17, retro RUN-20261016-02, nhìn lại năm phiên từ RUN-20261015-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** bốn trong năm phiên dừng hoặc không làm được task nào. Hai lần vì môi trường (RUN-20261015-02, RUN-20261016-01), hai lần vì plan thiếu (RUN-20261015-01: test khẳng định màn hình thoáng qua; RUN-20261016-02: test của script đọc file mà plan đổi). Mỗi lỗi chỉ lộ ra giữa đêm, tốn một phiên cộng một vòng người sửa và lập lại danh sách; một task dừng còn kéo theo task phụ thuộc.
- **Cải tiến:** bắt lỗi trước phiên và làm mỗi lần dừng rẻ hơn, không nới điều kiện dừng: (1) `scripts/plan-check.py` khi viết plan và ngay trước mỗi task; (2) kiểm tra môi trường hằng ngày `env-canary`, `start-run.yml` từ chối khi có issue `env-broken`; (3) khi task dừng, phiên soạn sẵn plan phiên bản mới, PR sửa nguyên nhân dạng nháp gắn `needs-human` (`merge-if-green.sh` từ chối) và danh sách chạy tiếp theo; (4) danh sách chạy xếp task độc lập trước chuỗi phụ thuộc.
- **Trạng thái:** applied
- **Áp dụng:** PR cải tiến tính tự chạy (anh Nhật đồng ý cả bốn ngày 2026-10-17)
- **Kiểm chứng:**
