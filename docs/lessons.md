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

Bài học L-001 tới L-031, ghi trong lúc xây dựng dự án mẫu, đã chuyển khỏi nhánh chính ngày 2026-10-18; đọc lại ở commit b24fbf4 (`git show b24fbf4:docs/lessons.md`). Các tài liệu quy trình vẫn nhắc tới những mã này. Bài học mới đánh số tiếp từ L-032.

### L-032. Máy Xiaomi chặn thao tác giả lập qua adb nếu chưa bật "Gỡ lỗi USB (Cài đặt bảo mật)"

- **Ngày, nguồn:** 2026-10-19, lần khảo sát app mẫu pose camera đầu tiên
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `adb shell input` và `pm clear` báo `SecurityException` (INJECT_EVENTS, CLEAR_APP_USER_DATA) nhưng vẫn thoát mã 0. Chụp ảnh và đọc cây giao diện vẫn chạy, nên mọi lần chạm thất bại lặng lẽ và agent khảo sát không đi được màn hình nào.
- **Cải tiến:** `scripts/app-explore/ax.sh` đọc đầu ra của `input` và thoát mã 3 kèm hướng dẫn bật cài đặt; lệnh `check` thử trước khi khảo sát. `docs/ops/clone-study.md` mục 1 ghi bước chuẩn bị máy.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-app-process`; PR #61.
### L-033. Chạm theo tọa độ cũ trúng quảng cáo vừa hiện lên

- **Ngày, nguồn:** 2026-10-20, khảo sát `com.camera.filter.pose.guide.ideas`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** agent chạm nút Back theo tọa độ đọc từ trước, đúng lúc một quảng cáo native toàn màn hình hiện lên sau interstitial; hai lần mở trang Play của nhà quảng cáo. Banner thu gọn còn tự bung lại khoảng 20 giây sau khi thu.
- **Cải tiến:** mọi lệnh chạm của `ax.sh` đọc lại cây giao diện ngay trước khi chạm; `scripts/app-explore/guard.py` từ chối khi phần tử nhận cú chạm thuộc quảng cáo hay đang ở quảng cáo toàn màn hình, và `ax.sh` tự back, ghi `incidents.log` khi lỡ rời sang Play hay trình duyệt. Có test trong `scripts/tests/run.sh`.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-app-process`; PR #61.
### L-034. Chạm liên tục trong vòng lặp mở màn hình mua của Play

- **Ngày, nguồn:** 2026-10-20, khảo sát `com.vector123.posecamera`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** agent bấm nút chụp nhiều lần liên tiếp mà không xem lại màn hình. Hết lượt miễn phí, màn hình trả phí bật lên và lần bấm sau trúng "UNLOCK FOREVER", mở màn hình mua của Play hai lần. Công cụ tự back cả hai lần, không có bước mua nào được xác nhận.
- **Cải tiến:** `guard.py` từ chối nút mua, đăng ký, dùng thử, nút có giá, "Tiếp tục" trên màn hình có giá, và mọi chạm trên màn hình thanh toán của Play. `docs/ops/clone-study.md` mục 4 cấm chạm trong vòng lặp mà không xem lại màn hình. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-app-process`; PR #61.
### L-035. Khảo sát khi máy không có mạng thì thiếu quảng cáo và nội dung

- **Ngày, nguồn:** 2026-10-21, khảo sát `com.cam.simplepose.pose.camera.photo.guide`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** Wi-Fi bật nhưng máy không có mạng suốt phiên. Agent không thấy quảng cáo, hộp thoại đồng ý, và một nửa danh mục tư thế trống; phải khảo sát lại phần đó.
- **Cải tiến:** `ax.sh check` kiểm tra mạng và thoát mã 8 khi không có; `docs/ops/clone-study.md` mục 1 ghi bước kết nối mạng.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-app-process`; PR #61.
### L-036. Báo cáo khảo sát app mẫu trích nguyên mã AdMob thật của app mẫu

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose (chép tài liệu khảo sát vào `docs/research/`)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** báo cáo PoseMate trích một dòng log có mã quảng cáo AdMob thật của app mẫu. Agent đọc APK được dặn không chép mã quảng cáo, agent khảo sát trên máy thì không. Phiên app phát hiện và che tay trước khi commit.
- **Cải tiến:** `scripts/secret-scan.py` quét file của repo (mã AdMob ngoài mã thử của Google, khóa API, khóa riêng, token GitHub) và chạy trong `scripts/tests/run.sh`, nên cổng chất lượng chặn. `clone-study.md` mục 4, mẫu báo cáo và `docs/CLAUDE.md` cấm chép các giá trị đó, trích log thì che.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-followups`; PR #65.
### L-037. Đường dẫn trong bản đề xuất gãy khi chép tài liệu khảo sát vào repo

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `clone-study.md` để báo cáo trong `reports/` cạnh `proposal.md`, còn `docs/README.md` quy định `research/<package>.md` đặt phẳng. Phiên app phải sửa link trong bản đề xuất đã duyệt.
- **Cải tiến:** thư mục làm việc có `research/` cùng cấu trúc với `docs/research/`; chép nguyên, không sửa link. Mẫu đề xuất ghi link tới file bên cạnh và ảnh nằm ngoài repo.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-followups`; PR #65.
### L-038. App mới còn bài học của dự án mẫu trên trang tiến độ

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `scripts/new-app.sh` xóa bài học của dự án mẫu khỏi `docs/lessons.md` nhưng không sinh lại `docs/STATUS.md`, nên trang của app mới còn L-032 đến L-035.
- **Cải tiến:** `new-app.sh` chạy `status.py` và `foundation.py` trước commit đầu tiên; test kiểm trang của app mới khớp với tài liệu.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/clone-followups`; PR #65.
### L-039. Test của script hỏng trong app mới: test chỉ dành cho dự án mẫu, và test cần remote origin

- **Ngày, nguồn:** 2026-10-21, app camera-pose (PR #1, #2 của repo app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** trong app, `scripts/tests/run.sh` chạy test của `new-app.sh` mà app không có script đó, nên test "new-app.sh succeeds" hỏng và CI của mọi PR app đỏ. Trước khi đẩy repo lên GitHub, thêm 14 test của `merge-if-green`, `wait-run`, `env-canary` hỏng vì các script đọc tên repo từ remote `origin`.
- **Cải tiến:** test của `new-app.sh` chỉ chạy khi có script (repo dự án mẫu); bộ test đặt một remote `origin` giả cho mọi lệnh git qua `GIT_CONFIG_*`, không phụ thuộc remote thật. Không sửa script được bảo vệ.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/app-test-template-only`; PR #66.
### L-040. Commit chỉ có tài liệu bỏ qua CI, nên test kiểm tài liệu không chặn được lỗi

- **Ngày, nguồn:** 2026-10-21, PR #68 và #69
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-008 (PR #68, chỉ có tài liệu) có mã AdMob giả toàn số 0. Bộ quét mã thật (PR #65) coi là mã thật, nhưng CI bỏ qua commit chỉ có tài liệu nên `main` có test của script đỏ mà không ai biết; phát hiện khi chạy test trên máy ở PR #69.
- **Cải tiến:** bộ quét cho qua mã toàn số 0 (PR #69). Đề xuất: CI chạy `scripts/tests/run.sh` cả với commit chỉ có tài liệu (nhanh, không cần Gradle), vì nhiều test của nó kiểm tài liệu (`foundation.py`, `status.py`, quét mã thật). Đổi `.github/workflows/` là việc rủi ro cao, cần người quyết.
- **Trạng thái:** accepted
- **Áp dụng:** phần quét mã toàn số 0: PR #69; test của script chạy khi PR sửa `scripts/`: L-073 (PR #139). Còn lại: commit chỉ có tài liệu vẫn không chạy `scripts/tests/run.sh`; cần plan rủi ro cao vì sửa `.github/`.

### L-041. Plan ghi sẵn phương án dự phòng đã duyệt, để phiên chuyển sang thay vì dừng task

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-039 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ba lần dừng task đầu tiên ở app đều cùng một dạng: kỹ thuật của plan không chạy (hết thời gian chờ khi chụp ảnh trong Robolectric, quy tắc của `check.sh`, thuộc tính trợ năng không lan xuống node con), và cách thay đã rõ ngay khi gặp lỗi. Phiên phải dừng vì cách thay chưa được duyệt; mỗi lần mất một phiên và một vòng duyệt.
- **Cải tiến:**
  - mẫu plan có mục 3.4 Phương án dự phòng: điều kiện chuyển, cách làm thay; file của phương án dự phòng ghi "(dự phòng)" ở mục 3.2;
  - `risk-scoring.md` mục 4: chuyển sang phương án dự phòng đã duyệt không phải dừng; chỉ chuyển một lần, không vượt các điều kiện dừng khác;
  - `auto-run.md` bước 4 và `docs/CLAUDE.md` mục Khi viết plan.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-042. Kỹ thuật lần đầu dùng phải chạy thử bằng `scripts/check.sh` trước khi duyệt plan

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-001, L-010 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ở app, bốn lần dừng có cùng một gốc: kỹ thuật lần đầu dùng chưa được chạy thử, hoặc chạy thử bằng lệnh khác với lệnh phiên dùng (chỉ `./gradlew :module:testDebugUnitTest`, trong khi phiên dừng ở quy tắc của `scripts/check.sh`). Plan chạy thử bằng `scripts/check.sh` trước khi duyệt thì không dừng nữa.
- **Cải tiến:** mẫu plan có mục 4.1 Chạy thử; `docs/CLAUDE.md` mục Khi viết plan; `scripts/plan-check.py` quy tắc 7: plan `draft` có tiêu chí E từ 2 mà mục 4.1 không ghi lần chạy thử bằng `scripts/check.sh` là lỗi, plan `approved` chỉ chú ý (để phiên không bỏ qua plan đã duyệt trước quy tắc này). Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-043. Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-028 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên merge một task, kiểm `main-broken` ngay (chưa có issue) rồi làm task sau. `main-guard` của commit merge xong 8 phút sau và mở `main-broken`; phiên chỉ thấy khi kiểm lại sau khi task sau đã xong bước 7, phí khoảng 45 phút và 22M token cho một task không merge được.
- **Cải tiến:** `auto-run.md` bước 9: sau khi merge, chờ `main-guard` của commit merge bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền; phải có kết quả rồi kiểm lại `main-broken` trước khi đẩy commit đầu tiên của task sau.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-044. Phiên mở bằng `claude --cloud` không được `resume-run.yml` tiếp tục khi hết hạn mức

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-008, L-020 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** app mở nhiều phiên bằng `claude --cloud` từ terminal để dùng cloud session credits (routine không dùng được credit). `resume-run.yml` chỉ kích hoạt lại routine, nên phiên loại này dừng hẳn khi hết hạn mức, người phải mở lại.
- **Cải tiến:** `auto-run.md` mục 1: ưu tiên `start-run.yml` hoặc Run now của routine; chỉ mở bằng `claude --cloud` khi có lý do (ví dụ dùng credit) và ghi lý do vào mục Kiểm tra đầu phiên.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-045. Mâu thuẫn và test yếu chỉ được tìm thấy khi phiên đã chạy

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-041 của app, gom L-014, L-015, L-019, L-030; PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:**
  - Khi chuẩn bị một danh sách chạy ở app, nhiều lỗi chỉ lộ ra nhờ rà soát bằng tay: một plan cần quyền mà spec không có; câu trả lời của người duyệt ở một plan trái với quy tắc ghi sự kiện mà plan khác vừa đặt; hai plan đặt cùng một phần giao diện vào hai ViewModel khác nhau.
  - Trong phiên, một plan để phần điều hướng của vỏ app trong ngoại lệ viết test trước, nên lỗi luồng chỉ lộ ra ở `ui-test`, vòng CI dài nhất.
  - Spec đã `confirmed` mà còn dòng lịch sử "Chờ xác nhận lại"; ticket và plan lệch trạng thái; danh sách chỉ có một chuỗi phụ thuộc làm cả phiên trống khi task đầu dừng.
- **Cải tiến:**
  - `scripts/run-check.py`: kiểm cả danh sách chạy (plan và ticket `approved`, spec `confirmed` và mọi câu hỏi đã có trả lời, phụ thuộc đúng thứ tự, mọi kịch bản có dòng test; chú ý khi kịch bản ít dòng test hơn số tiêu chí, spec đổi sau ngày plan được duyệt, ngoại lệ viết test trước ở vỏ app, hai plan cùng sửa một file mà không phụ thuộc nhau, một chuỗi phụ thuộc). Có test.
  - `docs/ops/pre-run-review.md`: rà soát độc lập với ngữ cảnh mới về mâu thuẫn, test chưa đủ tốt, khả năng chạy được trong phiên.
  - Mẫu danh sách chạy có mục Kiểm tra trước khi duyệt; `auto-run.md`: dev chỉ duyệt khi script qua và không còn điểm chặn; phiên chạy lại `run-check.py` ở đầu phiên.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-046. Quyền và thư viện cần thêm chỉ lộ ra khi viết code, phiên phải dừng hoặc đi vòng

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-042, L-007 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ở app, một hàm CameraX trả `ListenableFuture` mà Guava không có trên classpath biên dịch; thêm thư viện là điều kiện dừng nên phiên phải gọi qua reflection. Một plan khác cần quyền lưu tệp trên Android 9 trở xuống mà spec không có, chỉ được thấy nhờ người rà soát bằng tay trước khi duyệt.
- **Cải tiến:**
  - mẫu plan mục 3.3 có hai dòng bắt buộc: Quyền truy cập (thêm, kèm phần spec cho phép; không thêm) và Thư viện và SDK;
  - mẫu spec mục 6 có Quyền của app và dịch vụ bên ngoài;
  - `docs/CLAUDE.md`: cách tra quyền theo phiên bản Android, thư viện trên classpath biên dịch, quyền trong manifest gộp;
  - `scripts/plan-check.py` quy tắc 8 (lỗi với plan `draft`, chú ý với plan `approved`), có test;
  - `risk-scoring.md` điều kiện dừng 2 và `pre-run-review.md` dẫn tới các bước này.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`; PR #80.
### L-047. Chạy lại `main-guard` trên đúng commit bị đổ lỗi, xanh, nhưng issue `main-broken` không tự đóng

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-036 của app, issue #79)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** dev chạy lại `main-guard` trên commit bị đổ lỗi và lần chạy xanh. Bước đóng issue chỉ đóng khi `compare/<commit bị đổ lỗi>...<commit xanh>` trả `ahead`; cùng một commit thì trả `identical`, nên issue vẫn mở và dev phải tự đóng trước khi phiên sau chạy được.
- **Cải tiến (đề xuất, chưa làm):** `main-guard.yml` đóng issue khi `status` là `ahead` hoặc `identical`; ADR-0010 mục 2 ghi thêm trường hợp này. Chạm `.github/` nên cần plan rủi ro cao có duyệt chéo.
- **Trạng thái:** applied
- **Áp dụng:** ADR-0025 (`proposed`), `main-guard.yml` đóng issue khi `ahead` hoặc `identical`; PR #91 (thay #90), đã merge 2026-10-23.

### L-048. Một test chập chờn trên `main-guard` kết thúc cả phiên

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-029, L-037 của app, issue #79)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** một screenshot test chụp trạng thái có vòng chờ chạy mãi thất bại một lần trên `main-guard`, xanh ở PR, ở cục bộ và khi chạy lại. Một lần đỏ đó mở `main-broken`, phiên kết thúc khi mới xong 2 trên 7 task. Nguyên nhân, đo ở app: `captureRoboImage(path) { ... }` chờ luồng chính rảnh mà không có giới hạn; vòng chờ chạy mãi nên Robolectric vẽ khung hình khoảng 4 phút tới khi máy test hết bộ nhớ rồi mới chụp. Với bộ nhớ test mặc định, 5 lần qua; với 1g và 384m, ảnh lệch và test thất bại. Chụp bằng `runComposeUiTest` với đồng hồ dừng ở mốc cố định: 7 trên 7 lần qua với ba cỡ bộ nhớ, test chạy 0,25 giây.
- **Cải tiến (đề xuất, chưa làm):** `main-guard.yml`: khi bước kiểm tra đỏ chỉ vì test thất bại, chạy lại riêng các test đó một lần trên cùng commit; xanh thì không mở `main-broken` mà mở hoặc bình luận vào issue `flaky` ghi tên test, để sửa test trong một ticket thường; vẫn đỏ thì làm như hiện nay. ADR-0010 mục 2 thêm quy tắc này; cần plan rủi ro cao có duyệt chéo. Ngoài ra, `pre-run-review.md` đã coi test có nguy cơ chập chờn (hiệu ứng chạy mãi, đồng hồ thật, mạng thật) là mức chặn.
- **Trạng thái:** applied
- **Áp dụng:** `CLAUDE.md` bước 10 (cách chụp trạng thái có hiệu ứng chạy mãi), nhánh `claude/from-camera-pose-phien-tu-khac-phuc`. Phần `main-guard.yml`: ADR-0025 (`proposed`), `scripts/failure-kind.sh`, chạy lại `check.sh` một lần và ghi issue `flaky`; PR #91 (thay #90), đã merge 2026-10-23.

### L-049. Phiên không tự đưa nhánh chính về trạng thái xanh được, kể cả bằng một bản revert đúng

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-038 của app, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** khi `main-broken` mở, `merge-if-green.sh` từ chối mọi PR, kể cả PR từ nhánh `revert/<commit>` mà `main-guard` dựng sẵn; chỉ người đóng issue mới mở lại được. Lý do của ADR-0010 là đúng (tên nhánh không phải bằng chứng), nhưng hệ quả là một lần merge hỏng vào ban đêm làm mọi task còn lại chờ tới sáng.
- **Cải tiến (đề xuất, chưa làm):** ngoại lệ kiểm bằng nội dung thay vì tên: khi `main-broken` mở, `merge-if-green.sh` chỉ merge PR có đúng một commit mà cây file trùng khớp với kết quả `git revert <commit bị đổ lỗi>` do chính script dựng trên nhánh chính mới nhất, và CI xanh. Phiên được merge bản revert của commit do chính phiên merge, chuyển task đó sang `stopped`, rồi làm tiếp các task không phụ thuộc nó. Issue vẫn chỉ tự đóng bởi `main-guard`; AI vẫn không đóng hay sửa issue. Cần ADR sửa ADR-0010 và plan rủi ro cao có duyệt chéo.
- **Trạng thái:** applied
- **Áp dụng:** ADR-0025 (`proposed`), `scripts/is-exact-revert.sh`, ngoại lệ bản revert đúng trong `merge-if-green.sh`, `docs/auto-run.md` mục Khi nhánh chính đỏ vì task của phiên; PR #91 (thay #90), đã merge 2026-10-23.

### L-050. Quy tắc của `check.sh`, giới hạn mạng của phiên cloud và trạng thái ticket chỉ lộ ra khi phiên đã chạy

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-005, L-013, L-019 của app, issue #78)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ở app, ba chỗ hở cùng một kiểu: điều plan phải biết chỉ được phát hiện khi phiên đã chạy hoặc khi người đối chiếu bằng tay. Quy tắc "mọi `captureRoboImage` ghi vào `src/test/screenshots/`" chỉ nằm trong `check.sh` và một câu của `CLAUDE.md`, người viết plan và phiên đều bỏ sót (T-007). Plan T-010 yêu cầu `curl` file trên R2 trước bước 3, nhưng proxy của `claude --cloud` trả 403 cho máy chủ R2. Ticket chuyển `approved` khi plan còn `draft`, plan được duyệt mà ticket còn `planned`.
- **Cải tiến:** `docs/auto-run.md` bước 5 có bảng những gì `scripts/check.sh` từ chối; `scripts/plan-check.py` quy tắc 9 dò trong plan lệnh `captureRoboImage` sai đường dẫn, ảnh chuẩn mới thiếu ở mục 3.2, ảnh nằm ngoài `src/test/screenshots/` (lỗi với plan `draft`, chú ý với plan `approved`). `docs/CLAUDE.md` mục Khi viết plan: điều kiện về file R2 kiểm bằng workflow trên GitHub hoặc "dev kiểm trước phiên", không bằng `curl` từ phiên. `scripts/status.py --sync` chuyển ticket theo plan `approved` hay `stopped`; `plan-check.py` quy tắc 10 báo chú ý khi lệch; `docs/CLAUDE.md` mục Trang tiến độ. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/issue-78-plan-checks`; PR #84.
### L-051. Điều hướng sau một hàm `suspend` chạy trên luồng nền, lỗi chỉ lộ ra ở `ui-test`

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-030 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `AppNavHost` gọi một hàm `suspend` đọc DataStore rồi `navController.navigate` trong `scope.launch`; coroutine được tiếp tục trên luồng nền và app dừng với "setCurrentState must be called on the main thread". Phần vỏ app nằm trong ngoại lệ viết test trước của plan, nên lỗi chỉ lộ ra ở `ui-test` trên emulator, vòng CI dài nhất.
- **Cải tiến:** `CLAUDE.md` bước 6: điều hướng sau lời gọi `suspend` chạy trên `Dispatchers.Main.immediate`. `docs/CLAUDE.md` mục Khi viết plan: plan đổi hàm `suspend` mà vỏ app gọi có test Robolectric dựng vỏ app, không để vào ngoại lệ.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-052. Giao bước 3 đến 5 cho phiên con, trong worktree riêng

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-031, L-034 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên chính giao bước 3 đến 5 của 5 task lớn (tới 80 file) cho phiên con với danh sách file, thứ tự commit, điều kiện dừng và mẫu báo cáo. Cả 5 task merge, CI xanh vòng đầu, không file nào ngoài plan; ngữ cảnh của phiên chính đủ cho cả phiên. Nhưng phiên con làm trong thư mục chính, nên phiên chính không chạy được `check.sh` hay chuẩn bị task sau song song, và hook dừng phiên của môi trường cloud báo "có thay đổi chưa commit" ba lần. Một lần phiên chính sửa file khi `check.sh` chạy nền trong cùng thư mục và phải chạy lại.
- **Cải tiến:** `docs/auto-run.md` mục 3: Giao bước 3 đến 5 cho phiên con, trong worktree riêng của nhánh task, nội dung giao việc tối thiểu, phiên chính kiểm danh sách file; không sửa file trong thư mục đang chạy `check.sh` nền.
- **Trạng thái:** applied
- **Kiểm chứng:** RUN-20261026-02 (T-024, 30 file): phiên con làm bước 3 đến 5 trong worktree `../task-T-024` sau khi `check.sh` nền của nhánh chính xong; phiên chính viết báo cáo song song, không file nào ngoài mục 3.2, CI xanh vòng đầu. Có hiệu quả.
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-053. Nút chữ trên nền khác màu tự đặt màu chữ, đè màu thiết kế

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-032 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `TextButton` "Thử lại" trong `Snackbar` tự đặt màu chữ `primary` qua `ButtonDefaults.textButtonColors()`, đè `actionContentColor` mà `Snackbar` truyền xuống; độ tương phản giảm từ khoảng 3,9:1 xuống 2,4:1. Ảnh chuẩn ghi lại màu sai, không test nào đo màu chữ; chỉ rà soát độc lập thấy khi đo điểm ảnh.
- **Cải tiến:** `CLAUDE.md` bước 10, mẫu plan mục 4, `pre-run-review.md` mục 2.2: truyền `colors` rõ cho nút; test đo màu chữ trên ảnh vẽ ra.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-054. Hai plan cùng ghi Tạo một file mà task chạy trước tạo ra

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-033 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** hai plan trong cùng danh sách cùng ghi Tạo một biểu tượng, mỗi plan một kiểu. `plan-check.py` chỉ báo khi task sau bắt đầu (file đã có); phiên phải tự chọn và ghi lệch plan. `run-check.py` chỉ báo hai plan cùng sửa một file khi chúng không phụ thuộc nhau.
- **Cải tiến:** `scripts/run-check.py`: chú ý khi hai plan cùng ghi Tạo một file, kể cả khi phụ thuộc nhau; `docs/CLAUDE.md`: một plan tạo, plan kia ghi Sửa hoặc dùng lại; `pre-run-review.md` mục 1. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-055. Test kiểm giá trị mặc định qua ngay trên khung rỗng

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-035 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở 4 task lớn, một số test viết theo mục 4 của plan qua ngay trên khung rỗng của bước 3, vì khung trả đúng giá trị mặc định mà test mong đợi ("chưa lưu gì thì trả mặc định", "tệp hỏng thì trả `null`"). Phiên con tự viết chặt lại 19 test bằng một bước đối chứng (ghi rồi đọc lại, dữ liệu đúng cạnh dữ liệu sai), sau đó chúng thất bại đúng lý do.
- **Cải tiến:** `docs/auto-run.md` bước 3: bước đối chứng là cách mặc định; mẫu plan mục 4, `docs/CLAUDE.md`, `pre-run-review.md` mục 2.2.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-056. Bài học thêm ngoài phiên trùng số với bài học mà phiên thêm cùng lúc

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-042 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** một PR cải tiến mở trong lúc phiên chạy thêm 6 bài học; phiên thêm 4 bài cùng số trong PR báo cáo. Sau khi phiên merge phải đổi số ở hai PR của app, một PR và một issue của dự án mẫu. Nếu hai bài học nằm ở hai chỗ khác nhau của sổ, git gộp được mà không xung đột và không công cụ nào báo.
- **Cải tiến:** `docs/auto-run.md` mục 6: gộp nhánh chính và đổi số trước khi merge PR thêm bài học; `scripts/status.py` báo lỗi khi một số dùng hai lần. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-057. Trang tiến độ của app đếm 0 bài học vì sổ viết khác mẫu

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-043 của app, issue #86)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `status.py` chỉ nhận tiêu đề `### L-NNN.` và dòng trạng thái chỉ có trạng thái. Sổ của app viết `### L-NNN:` và `- **Trạng thái:** proposed. **Loại:** ...`, nên trang tiến độ ghi 0 bài trong khi sổ có 41, và không bài nào hiện ở mục chờ retro; cả 41 bài vẫn `proposed`.
- **Cải tiến:** `scripts/status.py` nhận cả hai dạng tiêu đề và đọc chữ trạng thái đầu tiên. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-058. Plan đổi cổng chất lượng bỏ sót một kiểm tra mà trường hợp mới phải đi qua

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-044 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan cho `merge-if-green.sh` nhận bản revert đúng khi `main-broken` mở (L-049) ghi "rồi đi tiếp tới các kiểm tra còn lại"; kiểm tra "phiên đang chạy" ngay sau đó từ chối mọi nhánh ngoài `claude/T-*` và nhánh báo cáo, nên trong phiên bản revert vẫn bị từ chối. Plan đã qua duyệt chéo; lỗi chỉ thấy khi viết code.
- **Cải tiến:** `docs/CLAUDE.md` mục Khi viết plan: plan đổi `merge-if-green.sh` hay `main-guard.yml` liệt kê mọi kiểm tra hiện có, nói trường hợp mới qua hay bị chặn ở từng kiểm tra, mỗi kiểm tra một test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
### L-059. Thiết lập staging, file R2, DNS, Java, merge và trang tiến độ chỉ lộ lỗi giữa hay cuối phiên

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-002, L-011, L-012, L-016, L-017, L-018, L-022, L-027 của app, issue #78)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, ba phiên liền chỉ biết bí mật Firebase staging là file giả khi phát bản staging ở cuối phiên; một phiên phát được bản build nhưng đỏ vì chưa tạo nhóm người test `team`. File R2 mà màn hình loading tải chưa được đưa lên, lỗi chỉ lộ ở PR có code đầu tiên. Tên miền file tĩnh thuộc vùng có DNSSEC lỗi nên 1.1.1.1 và 8.8.8.8 không phân giải được, điện thoại không tải được file trong khi emulator trên GitHub vẫn được. Máy dev chỉ có Java 26, Gradle 8.14.3 dừng với lỗi chỉ ghi "26". `docs/STATUS.md` xung đột ở mọi PR tài liệu. `merge-if-green.sh` phải gọi lại nhiều lần khi nhánh chậm, một lần job ghi `in_progress` dù lần chạy đã xanh, và một commit được đẩy lên nhánh sau khi PR đã merge.
- **Cải tiến:** `scripts/setup-check.sh` kiểm file trong `docs/ops/required-assets.txt` (`curl -I`), DNS qua 1.1.1.1 và 8.8.8.8, và với `--staging` bí mật Firebase staging (không phải file giả, có app `.staging`) và nhóm người test qua API của App Distribution. `start-run.yml` chạy nó trước khi kích hoạt phiên; `env-canary.yml` chạy hằng ngày, đỏ thì mở issue `setup-broken`; đây cũng là bước cuối của `docs/ops/setup-new-app.md`. `scripts/check.sh` gọi `scripts/java-check.sh` trước Gradle (khoảng phiên bản đọc từ `build-logic` và phiên bản Gradle). `merge-if-green.sh` có `--update` (cập nhật nhánh qua API, chờ CI có giới hạn rồi merge, kiểm lại mọi điều kiện sau mỗi lần chờ), coi job còn ghi đang chạy mà lần chạy chứa nó đã `completed success` là xanh, in mã commit sẽ merge và cảnh báo commit mới đẩy. `docs/STATUS.md` chỉ đi vào PR báo cáo của phiên và PR riêng của trang; không dùng `merge=ours` vì merge trên GitHub không chạy driver tùy chỉnh. `plan-check.py` chỉ tìm nơi nhắc tới file trong các file git quản lý. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/issue-78-protected`; PR #88.
### L-060. Phiên con đẩy commit trước khi có kết quả `main-guard`, và nhận việc ngoài plan từ nội dung giao việc chung

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên con của T-007 đẩy commit test lúc 08:11 khi `main-guard` của lần merge T-018 chưa xong (08:11:18), trái auto-run.md bước 9. Từ T-008, phiên chính tạo tệp cổng `gate-T-NNN` khi `main-guard` xanh và không có `main-broken`; phiên con làm cục bộ và chỉ đẩy khi có tệp, 10 task sau không vi phạm lần nào. Ở T-014, nội dung giao việc chung của phiên chính bảo phiên con thêm dòng vào `docs/features/ads/design/review/summary.md`, trái mục 3.1 của plan, làm một file ra ngoài danh sách; phiên chính phát hiện khi đối chiếu danh sách file và bỏ lại.
- **Cải tiến (đề xuất, chưa làm):** `docs/auto-run.md` mục Giao bước 3 đến 5: thêm tệp cổng vào nội dung giao việc tối thiểu; nội dung giao việc chung không thêm việc về file cụ thể, mọi việc về file lấy từ plan.
- **Trạng thái:** applied
-- **Áp dụng:** `docs/auto-run.md`, mục Giao bước 3 đến 5 (phần giao việc chung không thêm việc về file). Tệp cổng không cần nữa: phiên con không đẩy (L-070); PR #155.
### L-061. Rà soát độc lập bắt lỗi dựng lại activity và đổi trạng thái giữa chừng mà test viết trước bỏ sót

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** 4 trên 12 task có lỗi thật ở mức nên sửa do rà soát độc lập tìm ra, test viết trước không bắt: T-017 popup mất trạng thái khi activity dựng lại (khung hình đầu báo màn hình `null`; ca KB-3 thì popup biến mất), T-011 quảng cáo đã tải còn giữ khi vị trí bị tắt giữa chừng, T-010 nút CTA lệch thiết kế 8 dp, T-012 test thiếu kiểm đăng ký tài sản sau khi thu gọn. Plan không nêu trường hợp dựng lại activity hay cờ đổi giữa chừng.
- **Cải tiến (đề xuất, chưa làm):** `docs/CLAUDE.md` mục Khi viết plan: trạng thái giữ ở vỏ app (`MainActivity`, `AppNavHost`) có test dựng lại activity (chuỗi khung hình đầu chưa có back stack, hay `ActivityScenario.recreate()`); tài nguyên giữ theo cờ hay khóa Remote Config có test khi cờ tắt giữa chừng.
- **Trạng thái:** applied
- **Kiểm chứng:** RUN-20261026-02 (T-024): plan có sẵn test dựng lại activity (`LoadingRouteTest.recreateWhileAdShows_doesNotShowAgain`) và test khóa tắt giữa chừng (`placementSwitch_keyTurnedOff_emitsFalse`, `keyTurnedOffDuringWait_returnsNextTick`); rà soát độc lập không thấy lỗi ở hai loại đó. Có hiệu quả với hai loại đã ghi. Rà soát vẫn bắt một loại đổi trạng thái giữa chừng khác mà plan không nêu: app rời foreground trong lúc chờ (L-091).
-- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan (test dựng lại activity, test cờ tắt giữa chừng); PR #155.
### L-062. Test quét mã nguồn theo chuỗi con hỏng vì task chạy trước trong cùng phiên

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `AdGallerySourceSetTest` của T-014 kiểm không file `.kt`, `.xml` nào ngoài `src/staging` chứa `AdGallery`. T-017, merge trước trong cùng phiên, thêm một ví dụ KDoc `com.x.AdGalleryDestination/{id}` vào `app/src/main`. Plan T-014 viết khi T-017 chưa merge nên không thấy; phiên con nới test và task dừng ở bước 6. PR #114 sửa chú thích.
- **Cải tiến (đề xuất, chưa làm):** `docs/CLAUDE.md` mục Khi viết plan: test quét toàn bộ mã nguồn ghi ở mục 6 các plan cùng danh sách chạy có thể thêm chuỗi đó, hoặc quét theo ký hiệu (import, tên lớp đầy đủ, khai báo manifest) thay vì chuỗi con. `scripts/run-check.py` có thể báo chú ý khi một plan có test quét mã nguồn mà plan chạy trước sửa cùng source set.
- **Trạng thái:** applied
-- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan (quét theo ký hiệu, hoặc ghi plan có thể thêm chuỗi). Chưa làm chú ý của `run-check.py`; PR #155.
### L-063. Plan lấy code từ app bằng `gh pr diff`, môi trường cloud chặn

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-018, T-016, T-017 ghi đọc code của app camera-pose bằng `gh pr diff <n> -R VGL-Studio/camera-pose`. Trong môi trường cloud, `gh pr` bị chặn và `gh api` tới repo chưa gắn vào phiên trả 403. Cách chạy được: gắn repo chỉ đọc bằng `add_repo`, rồi `gh api repos/<owner>/<repo>/pulls/<n> -H "Accept: application/vnd.github.v3.diff"` hoặc `contents/<path>?ref=<sha>`. Phiên con thử clone repo đó thì bị từ chối; đọc qua API thì được.
- **Cải tiến (đề xuất, chưa làm):** `docs/auto-run.md` bước 7 (danh sách lệnh REST) thêm cách đọc PR và file của repo khác; `docs/ops/from-app.md` ghi plan lấy code từ app dùng `gh api`, không dùng `gh pr diff`.
- **Trạng thái:** applied
-- **Áp dụng:** `docs/auto-run.md` bước 7 (đọc PR, file của repo khác bằng `gh api`), `docs/ops/from-app.md` mục 3; PR #155.
### L-064. GitHub không chạy job (hết hạn mức) hay không chạy CI cho PR xung đột, phiên hiểu thành CI đỏ hoặc chờ 40 phút

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-046, L-048 của app, issue #106)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** tổ chức hết hạn mức GitHub Actions: mọi job đỏ mà 0 bước, `wait-run.sh` chỉ báo `ĐỎ`; phiên sẽ sửa code hay revert vô ích. PR xung đột với nhánh chính không có lần chạy `pull_request` nào, `wait-run.sh --pr` chờ trọn 40 phút.
- **Cải tiến:** `wait-run.sh` báo `HẠ TẦNG` (mã 4) khi mọi job đỏ đều chưa chạy bước nào, kèm chú thích của GitHub; báo `XUNG ĐỘT` (mã 5) khi PR `dirty` mà chưa có lần chạy. `auto-run.md`: mã 4 thì kết thúc phiên, giữ `running` để `resume-run.yml` tiếp tục, rồi chạy lại các lần chạy bị bỏ; mã 5 thì gộp nhánh chính. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-wait-run-ha-tang`; PR #107.
### L-065. Log tải file thất bại không có tiền tố `VGL.` và không cho biết nguyên nhân của lỗi bọc

- **Ngày, nguồn:** 2026-10-23, app camera-pose (plan T-032, PR #51 của app; bài học L-027 của app; issue #105)
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** ở app, máy thật không tải được catalog vì tên miền file tĩnh không phân giải được. Dòng log `Assets: Could not download <đường dẫn>` không nói lý do; phải kiểm DNS bằng tay. Ở dự án mẫu, câu log đã có lý do và lớp lỗi ngoài cùng (PR #97), nhưng `:core:assets` vẫn gọi thẳng `android.util.Log` với thẻ `Assets`: lọc `grep ' VGL\.'` không thấy dòng này, Crashlytics không giữ nó, unit test không kiểm được. Lỗi bọc (`IOException` có nguyên nhân `UnknownHostException`) chỉ hiện `IOException`, lỗi HTTP không có mã.
- **Cải tiến:** `:core:assets` phụ thuộc `:core:logging`; `AssetsModule` nhận `AppLogger` và ghi `warn` thẻ `Assets` (thành `VGL.Assets`, ADR-0019) qua `downloadFailureLogger`. Câu log ghi lý do, chuỗi lớp lỗi theo nguyên nhân (tối đa 5) và `status=<mã>` của lỗi HTTP; không ghi thông điệp lỗi, vì nó có thể chứa địa chỉ đầy đủ. Câu cũ cho một lỗi không bọc giữ nguyên. Có unit test với `RecordingAppLogger`.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-assets-applogger`; PR #108.
### L-066. Trang tiến độ vẫn chậm và tốn một PR riêng; PR xung đột không có lần chạy CI

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-047, L-048, L-016 của app, issue #104)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, PR nào đổi trạng thái cũng commit `docs/STATUS.md`, nên `merge-if-green.sh --update` lần nào cũng nhận `422 merge conflict` (PR #51, #52 của app). GitHub không chạy workflow `pull_request` cho PR đang xung đột: PR #56 của app không có lần chạy `pr-check` nào, `wait-run.sh --pr` chờ trọn 40 phút. Cách của L-059 (chỉ PR báo cáo và PR trang riêng mang trang) bớt xung đột, nhưng trang trên nhánh chính chậm tới khi có người mở PR trang, và mỗi lần xem trang mới tốn một PR cùng một lần CI.
- **Cải tiến:** `docs/STATUS.md` ra khỏi git (`.gitignore`). Workflow `status-page.yml` chạy `scripts/status.py` sau mỗi lần đẩy lên nhánh chính có đổi `docs/**` hay `scripts/status.py`, rồi đẩy đè nhánh `status` (nhánh chính cộng một commit chứa trang); không bao giờ đẩy lên nhánh chính. `merge-if-green.sh` báo chú ý khi PR nào có trang, kể cả PR báo cáo, và thôi nhắc mở PR trang. Liên kết tới trang là liên kết tương đối, không ghi tên repo. Có test. Ở app: lần chạy `status-page` 39936153612 xanh, các PR sau không còn xung đột vì trang.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-status-branch`; PR #109.
### L-067. Kho Maven mới chạy thử trên máy dev, môi trường cloud của phiên lại chặn

- **Ngày, nguồn:** 2026-10-24, RUN-20261024-02 (T-034)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-034 thêm hai kho Maven (`artifact.bytedance.com` của Pangle, `dl-maven-android.mintegral.com` của Mintegral). Lần chạy thử ở mục 4.1 làm trên máy dev nên tải được. Trong phiên, proxy của môi trường cloud trả 403 (`connect_rejected`) cho đúng hai máy chủ đó, `dl.google.com` vẫn tới được; Gradle báo "Could not GET ... Received status code 403 from server: Forbidden". Task dừng ở bước 4 theo mục 3.4 của plan, sau khi test đã viết xong (cùng loại với R2 ở L-050).
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan thêm kho Maven hay máy chủ tải mới thì chạy thử phần tải trong chính môi trường cloud của phiên (`claude --cloud`, hoặc một lệnh `curl` trong môi trường đó), ghi kết quả vào mục 4.1; bị chặn thì người thêm máy chủ vào chính sách mạng của môi trường trước khi duyệt plan. `scripts/run-check.py` có thể báo chú ý khi plan sửa `settings.gradle.kts` mà mục 4.1 không nhắc môi trường cloud.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan; nhánh `claude/duyet-run-20261024-03`. Phần `run-check.py` chưa làm.

### L-068. Plan không xét nhiều yêu cầu cùng lúc của một vị trí, rà soát và người viết hiểu khác nhau

- **Ngày, nguồn:** 2026-10-24, RUN-20261024-02 (T-023)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-023 và mục 8.2 của `ads-business-rules.md` chỉ quy định `load_attempt`, `latency_ms` cho khe một quảng cáo và cho pool. Native trong danh sách hiện có (`InListAds`) nạp nhiều yêu cầu song song cho cùng một vị trí. Phiên viết code coi vị trí là một khe có bộ đếm, khóa theo mã vị trí, nên các hàng đè thời điểm gửi của nhau; phiên rà soát coi hàng trong danh sách như pool. Task dừng ở bước 6, T-021 bị bỏ qua theo phụ thuộc.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan ghi số liệu hay trạng thái theo vị trí thì liệt kê mọi cách nạp hiện có của vị trí đó (một quảng cáo, nhiều hàng song song, pool) và nói rõ cách tính cho từng cách; mục kiểm thử có một test cho hai yêu cầu chồng nhau.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan; nhánh `claude/duyet-run-20261024-03`.

### L-069. Phần chưa commit của task dừng chỉ nằm trong container

- **Ngày, nguồn:** 2026-10-24, RUN-20261024-02 (T-034)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** T-034 dừng ở bước 4 khi phần khai báo thư viện (ba file) đã viết nhưng chưa build được. Phiên con hoàn tác và lưu thành tệp patch trong thư mục tạm của container, sẽ mất khi container bị thu hồi. Phiên chính commit lại phần đó lên nhánh task (`1a105a9`) để người tiếp tục trên máy dev.
- **Đề xuất sửa:** `auto-run.md`, mục Chuẩn bị cho người khi task dừng: phần code đã viết mà chưa qua kiểm tra thì vẫn commit lên nhánh task (thông điệp ghi rõ chưa build được, nhánh không có PR), không để trong container. Mô tả cách làm này trong nội dung giao việc cho phiên con.
- **Trạng thái:** applied
- **Áp dụng:** `auto-run.md`, mục Chuẩn bị cho người khi task dừng; nhánh `claude/duyet-run-20261024-03`.

### L-070. Phiên con không đẩy được nhánh task vì hệ thống phân quyền của môi trường chặn

- **Ngày, nguồn:** 2026-10-24, RUN-20261024-03 (T-021)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** phiên con làm bước 3 đến 5 của T-021 trong worktree riêng; lần `git push` đầu, ngay sau commit test, bị bộ phân loại của auto mode chặn ("Unrequested Commit in a Connected App"). Hai phiên con trước trong cùng phiên (T-034, T-023) đẩy được, nên chặn không đều. Phiên con làm tiếp tới `scripts/check.sh` qua rồi báo; phiên chính không đẩy thay, vì đẩy thay phần bị từ chối là vượt quyền. Task dừng ở bước 7 với hai commit chỉ trong container, sẽ mất khi container bị thu hồi.
- **Đề xuất sửa:** người xem lại cấu hình quyền của routine (cho phép `git push` tới nhánh `claude/T-*` cho cả phiên con), hoặc `auto-run.md`, mục Giao bước 3 đến 5 cho phiên con: phiên chính đẩy nhánh task, phiên con chỉ commit; phiên con gặp lệnh bị từ chối thì dừng ngay và báo, không làm tiếp phần chưa đẩy được.
- **Trạng thái:** applied
- **Áp dụng:** `auto-run.md`, mục Giao bước 3 đến 5 cho phiên con (phiên con chỉ commit, phiên chính đẩy; lệnh bị từ chối thì dừng ngay); nhánh `claude/ap-dung-l-070`.
- **Kiểm chứng:** RUN-20261025-01 (T-021): phiên con chỉ commit, phiên chính đẩy nhánh task ba lần (`13b485a`, `42613c4`, `d693cfd`) ngay khi có commit, không lần nào bị chặn. Task merge (PR #140).

### L-071. Token của phiên con làm trong worktree bị tính vào dòng Chung

- **Ngày, nguồn:** 2026-10-24, RUN-20261025-01 (T-021)
- **Loại, phạm vi:** công cụ, chung
- **Chuyện gì:** phiên con làm bước 3 tới 5 trong worktree `../task-T-021` và phiên rà soát đọc worktree đó, nhưng transcript của cả hai ghi `cwd` là thư mục chính và `gitBranch` là nhánh báo cáo (`claude/RUN-20261025-01-report`), vì phiên con chạy lệnh bằng `cd` hay đường dẫn tuyệt đối chứ không đổi thư mục làm việc của phiên. `scripts/token-usage.py report` gán toàn bộ 17.6M token vào dòng Chung, T-021 không có dòng riêng, nên không so được với `token_estimate` 20M.
- **Đề xuất sửa:** `scripts/token-usage.py`: transcript trong `subagents/` gán theo nhánh mà phiên con thật sự làm (ví dụ nhánh nêu trong lời giao việc, hay nhánh của worktree mà các lệnh của nó dùng), không theo `gitBranch` của dòng; hoặc `auto-run.md`, mục Giao bước 3 đến 5 cho phiên con: lời giao việc ghi một dòng `Task: T-NNN` để script đọc. Có test.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/token-usage.py` đọc dòng `Task: T-NNN` ở lời giao việc của phiên con (có test); `auto-run.md`, mục Giao bước 3 đến 5 cho phiên con; `ops/token-usage.md`; nhánh `claude/ap-dung-l-071-l-072`.

### L-072. Luật "không gửi ở nền" chỉ có test ở đường thử lại, đường nạp sẵn đầu nằm trong ngoại lệ nối

- **Ngày, nguồn:** 2026-10-24, RUN-20261025-01 (T-021)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-021 viết test cho KB-24 tiêu chí 4 ở vòng thử lại native và ở `FullScreenReloads`, còn lượt nạp sẵn đầu của native toàn màn hình nằm trong phần nối của `AdsController` (ngoại lệ viết test trước). Code qua mọi test nhưng vẫn gửi lượt nạp sẵn đầu khi app ở nền; rà soát độc lập bắt được (mức chặn), sửa ở `d693cfd` mà không có test tự động.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: tiêu chí chấp nhận áp cho mọi lượt gửi (ví dụ "không gửi ở nền") thì liệt kê mọi đường gửi (lượt đầu, nạp sẵn, thử lại, nạp lại) và cho mỗi đường một test, hoặc đưa điều kiện vào một hàm thuần mà mọi đường dùng chung; không để đường nào chỉ nằm trong ngoại lệ nối.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md`, mục Khi viết plan (mọi đường gửi có test, hoặc một hàm thuần dùng chung); nhánh `claude/ap-dung-l-071-l-072`.

### L-073. PR chỉ sửa một script mà không sửa `scripts/tests/` thì `pr-check` không chạy test của script

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-070 của app, issue #138)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** trong `scripts/`, bộ lọc `code` của `pr-check.yml` chỉ có `scripts/check.sh`, `scripts/test-summary.sh`, `scripts/tests/**` và `scripts/find-breaking-commit.sh`. Một PR chỉ sửa `scripts/merge-if-green.sh`, `wait-run.sh`, `run-check.py`, `plan-check.py`, `device-session.sh` hay script khác mà không đụng `scripts/tests/` thì `build-and-test` bị bỏ qua: `scripts/tests/run.sh` (do `check.sh` gọi) không chạy, PR vẫn xanh, lỗi chỉ lộ ở `main-guard` sau khi merge. Ở app, các PR sửa script trước nay qua được chỉ vì luôn thêm test vào `run.sh`.
- **Đề xuất sửa:** thêm `scripts/**` vào bộ lọc `code` của `pr-check.yml`; không thêm vào bộ lọc `ui`, vì test của script không cần emulator.
- **Trạng thái:** applied
- **Áp dụng:** `.github/workflows/pr-check.yml`, bộ lọc `code` có `scripts/**`; PR #139.

### L-075. Lỗi Java, Gradle wrapper hay SDK của môi trường cloud chỉ lộ sau 15 phút `check.sh`, hoặc không lộ ở kiểm tra trước phiên

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-049, L-056, L-069 của app, issue #130)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `preflight.yml` và `start-run.yml` chạy trên runner GitHub, không có biến môi trường (`JAVA_TOOL_OPTIONS`, proxy) và công cụ của môi trường cloud. Ở app, một lỗi đọc phiên bản Java chỉ lộ khi phiên cloud đã chạy `check.sh`, và cả phiên kết thúc mà không task nào chạy. `cloud-env-check.sh` chỉ kiểm phiên bản setup script. Thêm nữa: script in hai mảng có thể rỗng bằng `"${a[@]}"`; bash 3.2 của macOS coi mảng rỗng là biến chưa đặt khi có `set -u`, nên script dừng trước khi in thông báo, và test của script đỏ trên máy dev (CI dùng bash 5 nên không thấy).
- **Cải tiến:** `cloud-env-check.sh` kiểm thêm, trong vài giây đầu phiên: `java-check.sh`, `./gradlew --version` (giới hạn 300 giây), SDK có `platforms;android-<compileSdk>` (không đọc được `compileSdk` thì báo lỗi, không bỏ qua bước SDK). Thông báo tách lỗi setup (dán lại setup script) và lỗi công cụ (xem biến môi trường). Mảng có thể rỗng viết `${a[@]+"${a[@]}"}`. Có test, gồm Java giả có dòng `Picked up JAVA_TOOL_OPTIONS`, Gradle wrapper treo, SDK thiếu nền tảng; bộ test qua trên bash 3.2.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-kiem-tra-dau-phien`; PR #144.
### L-076. `start-run.yml` không biết commit đầu nhánh chính đã qua `main-guard` chưa

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-055 của app, issue #130)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở app, API merge trả lỗi dù commit đã vào nhánh chính, và GitHub không phát sự kiện push: không lần chạy `main-guard` nào cho commit có code đó. `start-run.yml` chỉ xem issue `main-broken`, nên vẫn khởi động phiên; phiên mất khoảng 15 phút `check.sh` mới biết nhánh chính có xanh không. Kiểm tra đầu phiên "lần chạy gần nhất của `main-guard.yml` qua" cũng không biết lần chạy đó thuộc commit nào.
- **Cải tiến:** `scripts/main-guard-status.sh` đi ngược lịch sử first-parent, bỏ commit chỉ đổi tài liệu, và báo `XANH` (0), `ĐỎ` (1, chỉ khi có issue `main-broken` đang mở), `CHỜ` (2, kèm giờ tạo lần chạy cho `wait-run.sh --since`), `CHƯA KIỂM` (3: có code mà chưa có lần chạy, lần chạy bị hủy, lần chạy thất bại mà không có issue `main-broken` (lỗi hạ tầng), commit merge hay không rõ file), `LỖI` (4). `start-run.yml` gọi nó trước khi kích hoạt routine (`checks: read`); `main-guard.yml` chạy được bằng tay (`workflow_dispatch`, chỉ trên `main`); kiểm tra đầu phiên của `auto-run.md` dùng script này. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-kiem-tra-dau-phien`; PR #144.
### L-077. Phiên dừng task vì vài dòng nối chỗ gọi, và để lại cho người điểm nên sửa có lỗi thật

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-058, L-059 của app; ADR-1003 của app; issue #132)
- **Loại, phạm vi:** rủi ro, chung
- **Chuyện gì:** hai task cùng danh sách merge lệch thứ tự; task sau phải nối chỗ gọi trong file mà task trước vừa tạo, không có trong mục 3.2, nên điều kiện dừng 1 dừng task dù phần sửa chỉ vài dòng và đúng ý cả hai plan. Một báo cáo phiên còn 13 điểm nên sửa chưa sửa vì dòng "Nên sửa" của bước 6 không bắt buộc; một điểm là lỗi người dùng gặp được (màn hình đứng yên khi chờ quảng cáo).
- **Cải tiến:** ADR-0028: được nối chỗ gọi mà task khác cùng danh sách đổi (test viết trước, tối đa 3 file và 40 dòng code đếm bằng `git diff --numstat`, không chạm điểm chặn của mục 1); phải sửa điểm nên sửa trong file được phép, có test viết trước, giới hạn 3 lần sửa cho từng điểm; ghi vào nhật ký plan và mô tả PR. Áp dụng vào `risk-scoring.md` mục 4 và `auto-run.md` bước 4, 5, 6, 10.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-pham-vi-run-check`; PR #141.
### L-078. `run-check.py` không thấy đề xuất thứ tự và phụ thuộc file giữa các plan; file đọc mới làm hỏng mọi plan đã duyệt

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-060, L-065 của app; issue #132)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `run-check.py` báo hai plan cùng sửa một file, nhưng không báo khi danh sách xếp ngược dòng "Đề xuất thứ tự chạy" của một plan, hay khi plan này dùng file mà plan kia tạo; thứ tự sai chỉ rà soát độc lập thấy. Ticket quy trình (không spec, không kịch bản) bị chặn như thiếu spec. Ở app, một script mới có chú thích nhắc tới file chung làm quy tắc 2 của `plan-check.py` báo lỗi cho cả 5 plan đã duyệt của danh sách; phiên chạy lại `plan-check` trước task và bỏ qua plan có lỗi, nên sẽ không chạy task nào.
- **Cải tiến:** `run-check.py` đọc dòng thứ tự ở mục 6 ("A trước B", "sau A", "nhóm ...: A, B", "A, rồi B"; bỏ qua chữ trong ngoặc kép, "trước hay sau", câu điều kiện "... thì ...", câu phủ định "không cần"), lỗi khi danh sách xếp ngược trừ khi có dòng "Bỏ đề xuất thứ tự: A trước B"; báo plan nhắc file mà plan khác tạo (lỗi khi đứng trước, chú ý khi đứng sau); chấp nhận ticket không spec và không kịch bản; chỉ đọc bảng của danh sách chạy, không đọc bảng rà soát. `plan-check.py` quy tắc 2: lỗi với plan chưa duyệt, chú ý với plan `approved`. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-pham-vi-run-check`; PR #141.
### L-079. Lỗi hạ tầng đã biết vẫn làm phiên dừng hay tốn vòng sửa, vì không có bảng tra lỗi

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-057, L-050 của app, issue #129)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở app, mỗi lỗi hạ tầng đã gặp (hết hạn mức Actions, PR xung đột không có CI, API merge báo lỗi mà đã merge) được sửa riêng bằng một mã thoát hay một đoạn văn; phiên không có một chỗ để tra "thông báo này nghĩa là gì, làm gì tiếp". Một lỗi mới cùng kiểu: `ui-test` đỏ hai lần liên tiếp vì tải file R2 hết giờ chờ 15 giây trên runner (màn tải dừng ở lỗi, test chờ 60 giây rồi hết giờ), không liên quan code của task; quy tắc "chạy lại một lần" biến nó thành lỗi thật và tốn vòng sửa.
- **Cải tiến:** `docs/ops/failure-playbook.md`: bảng 10 dòng (mẫu thông báo kiểu `grep -E`, nơi thấy, loại, việc phải làm, nguồn). `scripts/failure-kind.sh --playbook <log>` đọc chính bảng đó và in dòng đầu khớp. `auto-run.md` bước 8: CI đỏ thì tra bảng trước khi sửa code; các loại `chay-lai`, `lich-thu-lai`, `cho`, `gop-nhanh`, `kiem-da-merge` không tính vào 3 lần sửa. Mục 6: bài học môi trường có thông báo dò được thì thêm một dòng cùng tệp log thật trong `scripts/tests/playbook/`. Test cho mọi dòng và cho cách đọc bảng. Ở app: RUN-20261024-01 chạy hết 5 task không dừng.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-tu-xu-ly-loi`; PR #143.
### L-080. GitHub merge xong nhưng trả lỗi, PR vẫn mở và không có sự kiện đẩy

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-054 của app, issue #129)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, `merge-if-green.sh` nhận `unexpected end of JSON input` từ API merge và báo `TỪ CHỐI`, nhưng commit squash đã nằm trên nhánh chính. PR vẫn `OPEN`, không workflow `push` nào chạy (`main-guard`, `status-page`). Gọi lại script sẽ tạo commit trùng; phiên tự chạy sẽ tưởng merge bị từ chối và dừng task.
- **Cải tiến:** `merge-if-green.sh` tìm commit squash của PR trên nhánh chính (dòng đầu `<tiêu đề> (#<số>)` và cây file của commit đầu PR): có ngay từ đầu thì in `ĐÃ MERGE TRƯỚC ĐÓ`, không merge lần hai; API merge báo lỗi mà tìm thấy commit (đọc lại tối đa 3 lần) thì in `ĐÃ MERGE (GitHub báo lỗi)`, xóa nhánh và yêu cầu chạy `main-guard`; cả hai thoát 0. Không thấy thì vẫn `TỪ CHỐI`. Kiểm "đã có trên nhánh chính" chạy ngay sau kiểm trạng thái mở, trước các kiểm tra bản nháp, `needs-human` và `main-broken`, vì nó chỉ đọc và báo, không ghi gì lên GitHub; các kiểm tra có sẵn giữ nguyên thứ tự và vẫn chạy hết trước lời gọi merge (L-058). `fake-gh` trả lỗi cho lần ghi trong `fail_writes` và 404 cho đường dẫn không có trong fixture. Dòng PB-04 của bảng tra lỗi. Có test.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-tu-xu-ly-loi`; PR #143.
### L-081. Nhánh task chỉ nằm trên máy cloud khi lệnh đẩy bị từ chối hay mạng lỗi

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-051 của app, issue #129)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở app, lệnh `git push` của một phiên con bị bộ phân loại lệnh từ chối sau khi bước 3 đến 5 đã xong (4 commit, `check.sh` qua); công việc chỉ nằm trên máy cloud tạm thời. Phần "phiên con chỉ commit, phiên chính đẩy" đã có ở mẫu qua L-070 (đã áp dụng và kiểm chứng). Bài này giữ phần L-070 chưa có: lần từ chối lộ ra muộn, khi đã tốn công viết code; lỗi mạng khi đẩy không được thử lại; phiên chính cũng bị từ chối thì không có cách giữ lại commit ngoài worktree.
- **Cải tiến:** chỉ phần thêm vào L-070. `scripts/save-branch.sh <nhánh>`: đẩy nhánh `claude/*`, thử lại lỗi mạng (60, 120, 240 giây), không thử lại lần từ chối; không đẩy được thì giữ các commit ngoài nhánh chính thành bundle ở `$HOME/rescue/`, in đoạn "Nhánh chưa đẩy" kèm lệnh lấy lại, mã 3; `--bundle-only` khi môi trường chặn chính lời gọi lệnh. `auto-run.md`: bước 1 đẩy nhánh ngay khi tạo; các lần đẩy có sẵn (bước 3, phiên chính đẩy thay phiên con theo L-070) dùng script; mục mới "Khi không đẩy được" sau bước 7, cho lúc chính phiên chính bị từ chối; dòng ở mục 4 khi hai task liên tiếp không đẩy được; mục báo cáo "Nhánh chưa đẩy". Test với bare repo và hook `pre-receive`.
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-tu-xu-ly-loi`; PR #143.
### L-082. Vòng chờ bằng `pgrep -f` không bao giờ thoát; test đẩy lên bare repo đi tới URL giả

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-067, L-068 của app, issue #129)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở app, vòng `while pgrep -f "wait-run.sh --workflow" ...; do sleep; done` không thoát vì `pgrep -f` thấy dòng lệnh của chính vòng đó; `pkill -f "pgrep -f"` để dọn thì giết luôn shell của phiên (thoát 144). Mất khoảng 25 phút. Riêng test: đầu `scripts/tests/run.sh` đặt `GIT_CONFIG_*` trỏ mọi `origin` tới `https://github.com/o/r.git`, nên khối test đẩy lên bare repo cục bộ lại đẩy tới URL giả (403).
- **Cải tiến:** `auto-run.md` bước 9: chờ `main-guard` chỉ bằng `wait-run.sh` chạy nền, đọc kết quả từ tệp đầu ra, không dùng `pgrep -f`, `pkill -f`. Chú thích đầu `scripts/tests/run.sh`: khối đẩy lên bare repo phải tắt ba biến rồi đặt lại (như khối `save-branch`).
- **Trạng thái:** applied
-- **Áp dụng:** nhánh `claude/from-camera-pose-tu-xu-ly-loi`; PR #143.
### L-083. Thư viện mới qua `check.sh` và CI nhưng bản release (R8) hỏng, chỉ lộ khi phát staging

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-053 của app, issue #131)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, một task thêm `play-review-ktx`. `check.sh`, `pr-check` và `main-guard` chỉ build bản debug, nên R8 (`:app:minifyStagingReleaseWithR8`) hỏng vì `Missing class com.google.android.gms.common.annotation.NoNullnessRewrite` chỉ lộ ở `device-session` cuối phiên, sau khi PR đã merge. Bản staging không phát được cho tới khi có PR sửa luật R8.
- **Cải tiến:** `pr-check.yml` có bộ lọc `release` (`gradle/libs.versions.toml`, file `*.gradle.kts`, `build-logic/**`, `gradle.properties`, `proguard-rules.pro`, `consumer-rules.pro`, chính workflow); khi bộ lọc khớp, `build-and-test` chạy thêm `./gradlew :app:assembleStagingRelease` (không cần bí mật: file Firebase giả, APK không ký), bình luận lỗi nêu rõ bản release thất bại. `docs/CLAUDE.md`, mục Khi viết plan: plan thêm thư viện hay module chạy thử `:app:assembleStagingRelease` ở mục 4.1 và ghi luật R8 vào mục 3.2. `auto-run.md` bước 8: bước release đỏ là lỗi thật của task. Ở app: PR thử #93 đỏ đúng lỗi R8 đã gặp.
- **Trạng thái:** applied
- **Áp dụng:** nhánh `claude/from-camera-pose-pr-check-release`; PR #142.

### L-084. Hàng chờ plan cạn sau mỗi phiên, phiên kế tiếp phải chờ người và AI viết plan

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-061, ticket T-045, PR #120 của app), issue #133
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** sau một phiên chạy hết danh sách, không còn plan `approved` nào. Phải có một phiên có người theo dõi viết plan rồi mới lập được danh sách kế tiếp; ở app, khoảng cách giữa hai phiên (03:35 tới 16:53 UTC) chủ yếu là chờ plan.
- **Đề xuất sửa:** cuối phiên, sau khi PR báo cáo đã merge, phiên soạn plan `draft` cho các ticket `backlog` hay `ready` kế tiếp (`plan_queue` của danh sách chạy, mặc định 2): `scripts/plan-queue.py` chọn ticket (spec đã chốt, không câu hỏi mở, phụ thuộc đã có plan và không dừng hay hủy, chưa có nhánh hay PR plan), mỗi plan một PR nháp có nhãn `needs-human`, có chạy thử, `plan-check.py` và rà soát độc lập, giới hạn 45 phút cho mỗi plan; câu hỏi cho người gom vào mục "Plan soạn sẵn và câu hỏi cho người" của báo cáo. Phiên không đánh dấu sẵn sàng, không duyệt, không merge PR plan. Ở mẫu, sửa thêm điểm rà soát của app: đọc mọi PR mở bằng `gh api --paginate` (app dừng ở 100), và URL `origin` không phải GitHub thì thoát 2 mà không in URL (có thể chứa token).
- **Trạng thái:** applied
- **Áp dụng:** `scripts/plan-queue.py`, `docs/auto-run.md` (bước cuối phiên soạn plan), `docs/templates/run.md`, `docs/CLAUDE.md`; PR #151.

### L-085. Mục kiểm thủ công dồn lại sau mỗi phiên, phần lớn chạy được trên emulator

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-062, ticket T-046, PR #121 của app), issue #133
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** một báo cáo phiên của app có 7 nhóm kiểm thủ công, phần lớn chạy được trên emulator (xoay màn hình, mất mạng rồi thử lại, màn hình gập). Chúng vẫn chờ người, nên mục "Cần người kiểm tra" dài ra sau mỗi phiên.
- **Đề xuất sửa:** plan ghi mỗi dòng "Kiểm tra thủ công" là `(emulator: <tệp>)`, `(máy thật: <lý do>)` hay `(người: <lý do>)` (`plan-check.py`, quy tắc 11, chỉ nhắc). Phần emulator thành tệp kịch bản `<module>/src/deviceScenarios/<T-NNN>-<tên>.txt`, chạy bằng `scripts/device-scenario.py` sau kịch bản chuẩn của `device-session`, có hạn chót, trả máy về gốc, bảng "Kịch bản theo plan" trong `summary.md`. Vỏ app đặt `testTagsAsResourceId` ở gốc giao diện và ở hộp thoại mạng. Ở mẫu: `consent decline` chờ tag gốc `app_root` của `MainActivity` (app đặt `DEVICE_SCENARIO_FIRST_SCREENS` theo màn đầu của mình); `fresh` mở `MainActivity` như kịch bản chuẩn khi bản staging có hai activity LAUNCHER; `check` không tính tag chỉ còn trong test; `timeout -k 30`. Lần chạy thật đầu ở app (RUN-20261024-02): trình chạy không treo, 33 tới 128 giây mỗi tệp, nhưng cả ba kịch bản đều có bước lỗi hay bị bỏ qua, chưa rõ do app hay do kịch bản.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/device-scenario.py`, `scripts/device-session.sh` (bước 4 của `scenarios`), `scripts/plan-check.py` quy tắc 11, `UiAutomatorTags.kt` ở vỏ app, `docs/ops/test-sessions.md` mục 5, mẫu plan và danh sách chạy; PR #152.

### L-086. Không có danh sách sẵn sàng phát hành, nên không biết còn bao xa tới bản production

- **Ngày, nguồn:** 2026-10-24, app camera-pose (bài học L-064, ticket T-047, PR #122 của app), issue #133
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** việc cần làm trước bản production nằm rải rác trong báo cáo các phiên: mã quảng cáo thật cho từng vị trí, mục kiểm thủ công chưa đánh dấu, ticket chưa lên staging, phản hồi và issue đang mở. Không ai thấy được còn bao nhiêu việc, nên bản phát hành không có điểm dừng rõ ràng. App đã làm mục "Sẵn sàng phát hành" trên trang tiến độ.
- **Đề xuất sửa:** `scripts/status.py` sinh mục 6 "Sẵn sàng phát hành" từ tài liệu trong repo, không đọc cấu hình production, `scripts/release.sh` hay thư mục phát hành, không dùng mạng. Ở mẫu, khóa quảng cáo đọc theo đúng luật một dòng của `release.sh` (định dạng có dấu gạch dưới, thuộc tính sau định dạng, bỏ khóa `gallery_`), và báo khi một vị trí viết trên nhiều dòng. Sửa thêm các điểm rà soát ở app: `<->` hay `a < b, c > d` không bị coi là dòng mẫu; đường dẫn `origin` phải bắt đầu bằng `github.com` (https, ssh); có test cho `shorten()`, `spec:` rỗng; test "chỉ mở `docs/` và `AdPlacement.kt`" không còn đếm chữ `release` trong `docs/`.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/status.py` mục 6 "Sẵn sàng phát hành", `scripts/new-app.sh` (app mới không mang báo cáo phiên của mẫu), `docs/CLAUDE.md`; PR #149.

### L-087. `wait-run.sh --pr` gọi ngay sau khi đẩy đọc lần chạy cũ bị hủy và báo đỏ

- **Ngày, nguồn:** 2026-10-24, app camera-pose (phiên có người theo dõi, PR #98, #100 của app; sửa ở T-044, PR #119 của app), issue #129
- **Loại, phạm vi:** công cụ, chung
- **Chuyện gì:** ở app camera-pose, phiên đẩy commit mới lên nhánh PR rồi gọi ngay `wait-run.sh --pr`. Script đọc commit đầu của PR đúng một lần lúc bắt đầu; GitHub chưa kịp cập nhật PR nên script chờ lần chạy của commit trước, lần chạy đó bị GitHub hủy khi commit mới có lần chạy, và script báo `ĐỎ (cancelled)`. Ba lần trong một buổi (PR #100 của app: commit `e35ae86` rồi `eba5f98`); phiên tự chạy gặp mã 1 sẽ đọc log, không thấy lỗi, và có thể tính nhầm vào 3 lần sửa hay chạy lại vô ích.
- **Đề xuất sửa:** `scripts/wait-run.sh --pr` đọc lại commit đầu của PR mỗi vòng và đi theo commit mới; lần chạy bị hủy thì đọc lại commit đầu vài vòng trước khi gọi là đỏ; thêm `--sha <commit>` để chờ đúng commit vừa đẩy. `auto-run.md`, bước 8: chờ bằng `--sha "$(git rev-parse HEAD)"` sau khi chính phiên đẩy. Có test.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/wait-run.sh` (`--pr` đọc lại commit đầu mỗi vòng, `--sha`), `docs/auto-run.md` bước 8 và mục Khi nhánh chính đỏ, `CLAUDE.md`; PR #148. Số bài học đổi từ L-083 (trùng bài học R8 của #142).

### L-088. `$TMPDIR` trống trên cloud và `sed -i` kiểu GNU dừng bộ test trên macOS

- **Ngày, nguồn:** 2026-10-25, app camera-pose (L-071, L-072 của app; RUN-20261024-02, T-044, T-047, PR #124 của app), issue #150
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app camera-pose, plan T-044 ghi `git show origin/main:scripts/wait-run.sh > "$TMPDIR/wait-run.sh"`. Phiên `claude --cloud` không đặt `TMPDIR`, nên tệp bị ghi vào `/wait-run.sh` ở gốc ổ đĩa. Cùng phiên, một lệnh mở đầu bằng `sleep` bị công cụ chặn. Ở phiên khác, test `status` của `scripts/tests/run.sh` gọi `sed -i 's/.../'`. `sed` của macOS (bản BSD) cần đuôi sao lưu, báo `invalid command code`, nên bộ test dừng giữa chừng, không in dòng tổng kết, và `check.sh` đỏ trên máy dev. CI chạy Linux nên vẫn xanh. Cùng loại với L-069 (bash 3.2).
- **Đề xuất sửa:** `scripts/plan-check.py` quy tắc 12: plan `draft` ghi `$TMPDIR` hay `${TMPDIR}` không có giá trị mặc định thì báo lỗi, plan `approved` thì chỉ chú ý; `${TMPDIR:-/tmp}` qua. `docs/auto-run.md` bước 9 và `docs/CLAUDE.md` mục Khi viết plan: dùng `${TMPDIR:-/tmp}`, lệnh không mở đầu bằng `sleep`. Script dùng `sed -i.bak ... && rm -f <tệp>.bak`, hoặc perl, python. `scripts/tests/run.sh` có test dò `sed -i` không có đuôi trong `scripts/`.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/plan-check.py` quy tắc 12, `scripts/tests/run.sh` (test quy tắc 12, test dò `sed -i`), `docs/auto-run.md` bước 9, `docs/CLAUDE.md`; PR #157. Mẫu chưa có dòng `sed -i` nào thiếu đuôi.

### L-089. Plan thêm dòng vào enum dùng chung chỉ đọc các test lặp enum đó, không chạy, nên task dừng giữa phiên

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-01 (T-024)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** plan T-024 thêm hai vị trí `OPEN_APP`, `OPEN_APP_INTER` vào `AdPlacement`. Phần L-020 của mục 3.2 liệt kê các test lặp `AdPlacement.entries` và ghi `AdUnitIdsTest` "qua mà không sửa", nhưng không chạy thử. Trong phiên, `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` đỏ (chỉ cấu hình `sample_native_small` rồi khẳng định `missing` rỗng), file ngoài mục 3.2, task dừng ở bước 3. Trước phiên, rà soát độc lập đã bắt `AdsFlagsTest` theo cùng cách (plan phiên bản 2).
- **Bằng chứng:** nhật ký plan T-024 (RUN-20261026-01), commit `891ecec`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdUnitIdsTest.kt` dòng 10 đến 14.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan thêm dòng vào một enum dùng chung (`AdPlacement`, `FeatureFlag`, `AdFormat`) chạy thử toàn bộ test JVM của mọi module dùng enum đó với dòng mới, trên nhánh tạm không commit; test đỏ đưa vào mục 3.2, lệnh và kết quả ghi vào mục 4.1. `docs/ops/pre-run-review.md`: rà soát chạy lại bước này. Có thể thêm một quy tắc chú ý của `scripts/plan-check.py` khi plan thêm dòng vị trí vào `AdPlacement.kt` mà mục 4.1 không có lệnh Gradle.
- **Trạng thái:** applied (cách khác: người quyết không chạy thử lúc soạn plan; ADR-0029 cho phiên sửa tiếp test và file ngoài mục 3.2 cần cho task, merge rồi báo sau, và chạy test JVM của mọi module ở bước 3)

### L-090. Phiên con chạy nền chỉ báo khi xong, nên commit của nó không được đẩy ngay

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-01 (T-029, T-024)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `auto-run.md` (Giao bước 3 đến 5 cho phiên con) bảo phiên chính đẩy nhánh task ngay khi phiên con báo xong mỗi commit. Phiên con chạy nền chỉ báo một lần khi xong. Phiên này chạy một vòng nền có giới hạn theo dõi ref của nhánh task và gọi `scripts/save-branch.sh` khi ref đổi: commit test T-029 (11:58) được đẩy lúc 12:01 cùng commit code. Với T-024, vòng không chạy vì lần đẩy đầu phải chờ `main-guard` của task trước, nên commit test chỉ được đẩy sau khi phiên con dừng.
- **Bằng chứng:** dòng thời gian của `docs/runs/RUN-20261026-01.md`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục Giao bước 3 đến 5: ghi cách đẩy khi phiên con không báo giữa chừng (vòng nền có giới hạn theo dõi ref, gọi `save-branch.sh`, bắt đầu sau khi có kết quả `main-guard` của task trước), hoặc thêm script `scripts/push-on-commit.sh <nhánh> --minutes N` có test.
- **Trạng thái:** proposed

### L-091. Plan có bước chờ trước khi hiện quảng cáo không nêu trường hợp người dùng rời app giữa lúc chờ

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-02 (T-024, PR #170)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** màn loading chờ quảng cáo mở app tới 40 giây. Rà soát độc lập (mức nên sửa) thấy: người dùng bấm Home trong lúc chờ thì vòng chờ vẫn chạy, và quảng cáo hiện khi người dùng quay lại, có thể sau nhiều phút. Khi đó nó thành một quảng cáo khi quay lại app, nhưng không theo khóa và luật của KB-14. Spec KB-13 và plan không nói tới trường hợp này, nên phiên không sửa mà chuyển cho người quyết. Test viết trước có dựng lại activity và tắt khóa giữa chừng (L-061), nhưng không có trường hợp rời foreground.
- **Bằng chứng:** nhật ký plan T-024 (RUN-20261026-02), mục Rà soát độc lập; mô tả PR #170.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi viết plan, gạch L-061: thêm "plan có bước chờ có giới hạn trước khi hiện thứ gì đó (quảng cáo, hộp thoại, điều hướng): nêu điều xảy ra khi app rời foreground giữa lúc chờ, mục 4 có test `pause().stop()` rồi `resume()`". Spec mẫu (`docs/templates/spec.md`) có thể nhắc ca này cho kịch bản có thời gian chờ.
- **Trạng thái:** proposed

### L-092. Repo vượt 100 nhánh thì `merge-if-green.sh` không chạy được trên cloud, vì proxy chặn trang 2 của `--paginate`

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-03 (T-028, PR #185)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** PR #185 có CI xanh, nhưng `scripts/merge-if-green.sh 185` thoát 1 ở bước tìm phiên đang chạy: `gh api "repos/$REPO/branches?per_page=100" --paginate` đọc trang 2 theo liên kết `Link` dạng `repositories/{id}/branches?page=2`, và proxy của môi trường cloud chặn đường dẫn có mã số repo. Repo lúc đó có 101 nhánh. Hai lần merge trước trong phiên (T-027, T-040) qua vì khi đó còn dưới 101 nhánh. Script cũng gọi `DELETE git/refs/heads/<nhánh>` sau mỗi lần merge để xóa nhánh `claude/*`, nhưng proxy trả "Write access to this GitHub API path is not permitted through this proxy" và script bỏ qua lỗi (`|| true`), nên nhánh task đã merge không bao giờ bị xóa và số nhánh chỉ tăng. Thử lại 5 lần theo lịch (1, 2, 4, 8, 15 phút) đều lỗi; phiên kết thúc với 7 task chưa làm. `scripts/plan-queue.py` (`pulls?...--paginate`) và chỗ đọc file của PR trong `merge-if-green.sh` có cùng rủi ro khi vượt 100.
- **Bằng chứng:** `gh: Numeric-ID repository paths (repositories/{id}/...) are not supported through this proxy. Use repos/{owner}/{repo}/... endpoints instead. (HTTP 403)`; `bash -x scripts/merge-if-green.sh 185` dừng ở dòng 172; `git ls-remote --heads origin | wc -l` ra 101; dòng thời gian của `docs/runs/RUN-20261026-03.md`.
- **Đề xuất sửa:** `scripts/merge-if-green.sh` (và `plan-queue.py`) không dùng `--paginate`; tự lặp `&page=N` với đường dẫn `repos/{owner}/{repo}/...` tới khi trang rỗng, có test với fixture hai trang. Tìm nhánh báo cáo bằng `git/matching-refs/heads/claude/RUN-` thay vì liệt kê mọi nhánh. Lệnh xóa nhánh sau khi merge in cảnh báo khi lỗi thay vì im lặng; người dọn các nhánh `claude/T-*` đã merge (hay bật "Automatically delete head branches" của repo). Thêm dòng vào `docs/ops/failure-playbook.md` cho thông báo "Numeric-ID repository paths", kèm log thật trong `scripts/tests/playbook/`.
- **Đã làm tạm (2026-10-26, phiên giám sát):** xóa 93 nhánh `claude/*` đã merge hay đã đóng (còn 8 nhánh), bật "Automatically delete head branches" để GitHub tự xóa nhánh sau merge, không qua proxy. Phần sửa script (không dùng `--paginate`, cảnh báo khi xóa nhánh lỗi) vẫn cần plan rủi ro cao.
- **Trạng thái:** applied: `merge-if-green.sh`, `plan-queue.py`, `resume-check.py` tự đọc từng trang bằng `&page=N` trên đường dẫn `repos/{owner}/{repo}`, có test hai trang; `merge-if-green.sh` cảnh báo khi không xóa được nhánh mà nhánh còn. Anh Nhật cho sửa trực tiếp trong phiên giám sát 2026-10-26, không qua plan. Chưa làm: dòng bảng tra lỗi cho "Numeric-ID repository paths"; `env-canary.yml` vẫn dùng `--paginate` (chạy trên Actions, không qua proxy)

### L-093. Phiên con đổi điều một test viết trước kiểm trong commit code; chỉ rà soát độc lập bắt được

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-03 (T-028, PR #185)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** test `NativeAdCellTest.pager_recordsShownOnlyAfterSettle_notReadyOnDispose` của commit test (`21dda14`) đỏ ở bước 4 vì trang `HorizontalPager` cuộn đi vẫn ở trong composition. Phiên con đổi cách test cho trang rời đi (bỏ cả pager khỏi composition) trong commit code `ea331f8`, ghi lý do vào báo cáo như một "sửa chi tiết", và thêm vào `ads-guide.md` một câu mô tả hành vi sai (trang cuộn đi giữ quảng cáo). Rà soát độc lập xếp mức chặn; sau khi khôi phục test, code được sửa theo phương án dự phòng 3.4 của plan. `auto-run.md` bước 4 đã cấm đổi điều test kiểm, nhưng phiên con tự xếp thay đổi của mình là "chi tiết".
- **Bằng chứng:** `git diff 21dda14 ea331f8 -- core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCellTest.kt`; nhật ký plan T-028 (RUN-20261026-03), mục Rà soát độc lập.
- **Đề xuất sửa:** `docs/auto-run.md`, mục Giao bước 3 đến 5 cho phiên con: phiên con không sửa test của commit test ở bước 4 mà không báo; mọi thay đổi như vậy liệt kê riêng trong báo cáo (tên test, diff, lý do), và phiên chính đưa đúng danh sách đó vào lời giao rà soát độc lập. Có thể thêm script in `git diff <commit test>..HEAD` của các file test để phiên chính dán vào lời giao.
- **Trạng thái:** proposed

### L-094. Hết hạn mức thanh toán GitHub Actions giữa phiên dài; kiểm tra đầu phiên không đọc hạn mức Actions

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (sau T-033, PR #193)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** sau 6 lần merge, GitHub không cấp máy cho job `main-guard` của commit merge T-033 ("recent account payments have failed or your spending limit needs to be increased"). Phiên dừng trước task thứ 7 theo `auto-run.md` mục 4. Phiên mở bằng `claude --cloud` nên không được `resume-run.yml` tiếp tục (L-044); `resume-run.yml` cũng là workflow của Actions nên cũng không chạy được khi hết hạn mức. Kiểm tra đầu phiên và mục Kiểm tra trước khi duyệt không có bước nào xem hạn mức Actions còn đủ cho danh sách.
- **Bằng chứng:** `scripts/wait-run.sh` mã 4, lần chạy 40280561101; `scripts/failure-kind.sh --playbook` ra PB-02; dòng thời gian của `docs/runs/RUN-20261026-04.md` (06:34). CI của 6 task khoảng 90 phút `pr-check` (T-033 33 phút vì build release) cộng 6 lần `main-guard`.
- **Đề xuất sửa:** `docs/auto-run.md` mục 1: dev kiểm Billing & plans của Actions trước phiên dài, ước tính số phút CI của danh sách (mỗi task khoảng 10 phút `pr-check` và 10 phút `main-guard`, gấp ba khi đổi thư viện). `docs/templates/run.md`: thêm dòng "hạn mức Actions còn đủ cho danh sách" ở mục Kiểm tra trước khi duyệt.
- **Trạng thái:** proposed

### L-095. Phiên con xóa một test cũ thêm sau khi plan viết, kéo theo phần đối chứng của hành vi vẫn giữ

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (T-030, PR #189)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `CollapsibleNativeAdTest.loadingExpanded_placeholderCtaStays36dp` do T-028 thêm sau khi plan T-030 viết, nên không có trong bảng "Test đã có" của plan. Nửa đầu của test kiểm hành vi mà T-030 bỏ (CL-01), nửa đối chứng kiểm CTA 44 dp của khung chờ medium (T-028, vẫn giữ). Phiên con xóa cả test; rà soát độc lập xếp mức chặn (ADR-0029, quyết định 2) và phần đối chứng được giữ lại ở `NativeAdCardTest.mediumPlaceholder_ctaIs44dp`. Sau khi phiên chính thêm vào lời giao việc một dòng về việc này, bốn task sau không lặp lại.
- **Bằng chứng:** nhật ký plan T-030 (RUN-20261026-04), mục Rà soát độc lập; commit `df7f6f6`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục Giao bước 3 đến 5 cho phiên con, phần giao việc chung: "không xóa test cũ nào, kể cả test thêm sau khi plan viết; hành vi cũ bị plan thay thì chỉ bỏ câu khẳng định của hành vi đó, phần còn lại giữ (chuyển file nếu cần, báo là ngoài mục 3.2)".
- **Trạng thái:** proposed

### L-096. Dọn vòng đẩy nền bằng `pkill -f` dù L-082 đã cấm

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (sau T-026)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên chính chạy một vòng nền đẩy nhánh task mỗi khi có commit mới của phiên con (L-070). Sau khi merge T-026, phiên dọn vòng đó bằng `pkill -f "Push T-026"`; lệnh thoát 144 vì chuỗi khớp dòng lệnh của chính shell đó. Không mất gì (lệnh gỡ worktree chạy trước trong cùng dòng), nhưng L-082 đã cấm đúng việc này. Từ T-030 phiên dừng vòng nền bằng công cụ dừng tác vụ nền của môi trường.
- **Bằng chứng:** dòng thời gian 01:54 của `docs/runs/RUN-20261026-04.md`; lệnh `git worktree remove ../task-T-026 && pkill -f "Push T-026"` thoát 144.
- **Đề xuất sửa:** `docs/auto-run.md` bước 9 (đoạn L-082): nói rõ cách dừng vòng nền: công cụ dừng tác vụ nền của phiên, hay một vòng có giới hạn tự hết; không kết thúc vòng nền bằng lệnh tìm theo chuỗi. Mục Giao bước 3 đến 5 có thể nêu mẫu vòng đẩy nền (đọc `git rev-parse <nhánh>` mỗi phút, gọi `save-branch.sh` khi đổi).
- **Trạng thái:** proposed

### L-097. `auto-run.md` bảo soạn phản hồi kịch bản lỗi mức `nen-sua`, mẫu phản hồi không có mức đó

- **Ngày, nguồn:** 2026-10-25, app camera-pose (bài học L-074 của app, RUN-20261025-01), issue #163
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `docs/auto-run.md` mục 4, bước 1 bảo mỗi dòng `lỗi` của "Kịch bản theo plan" có một phản hồi `FB-...` "mức `nen-sua`". Mẫu `docs/templates/feedback.md` chỉ có `chan`, `nang`, `nhe`, `gop-y`, nên phiên phải tự chọn một bên (FB-20261025-01 của app ghi `nen-sua` kèm chú thích).
- **Đề xuất sửa:** thêm `nen-sua` vào mẫu phản hồi, hoặc đổi `auto-run.md` sang một mức có sẵn.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 1 đổi sang mức `nang` (app bị đóng hay ANR thì `chan`); mẫu phản hồi không đổi. Chọn `nang`, không chọn `nhe` như app, vì các phản hồi kịch bản lỗi đã có của mẫu (`docs/features/ads/feedback/FB-20261025-01`, `FB-20261025-03`) đều ghi `nang`; issue #163, phiên giám sát 2026-10-26.

### L-098. Plan có test chạy trên chuỗi giả mà người viết suy kết quả bằng tay, kỳ vọng sai qua 4 vòng rà soát

- **Ngày, nguồn:** 2026-10-25, app camera-pose (bài học L-075 của app, plan T-048), issue #163. Nguồn: camera-pose L-075
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** plan T-048 của app có nhiều test của `scripts/device-scenario.py` chạy trên `scripts/tests/fake-adb` với chuỗi dump giả. Người viết plan đọc code rồi suy ra test nào thất bại và vì sao; 4 vòng rà soát liền có điểm chặn vì kỳ vọng sai (ví dụ `ui-seq.n` của `fake-adb` không vượt số tệp của chuỗi). Chỉ hết khi mọi test mới được chạy thật trên code hiện tại trong một khối tạm không commit. Sau đó ở app: 29/32 test thất bại đúng lý do, 3 đối chứng qua như plan ghi, CI xanh lần đầu; T-049 và RUN-20261026-01 lặp lại kết quả đó (L-075 của app: verified).
- **Đề xuất sửa:** test của script chạy trên chuỗi giả hay tệp mẫu được chạy thật lúc viết plan; mục 4.1 ghi kết quả thật của từng test.
- **Trạng thái:** rejected: anh Nhật quyết 2026-10-26 không nhận, vì trái ADR-0029 (không chạy thử lúc soạn plan; test viết trong phiên tự chạy). Ghi lại để đối chiếu với app.

### L-099. Phiên đẩy commit test của task sau trước khi có kết quả `main-guard` của lần merge trước

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-076 của app, RUN-20261026-01), issue #188
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `docs/auto-run.md` bước 3 bảo đẩy nhánh task ngay sau mỗi commit; bước 9 bảo trước khi đẩy commit đầu tiên của task sau phải có kết quả `main-guard` của lần merge trước. Phiên làm theo bước 3 và đẩy commit test của T-052, T-050 khi `main-guard` còn chạy. Không gây hại vì `main-guard` đều xanh, nhưng nếu đỏ thì task sau đã bắt đầu trên nhánh chính hỏng.
- **Đề xuất sửa:** `docs/auto-run.md` bước 3 ghi ngoại lệ của bước 9.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` bước 3: commit đầu của task sau chỉ đẩy khi có kết quả `main-guard`, trong lúc chờ commit cục bộ; issue #188.

### L-100. Từ dịch lấy lại từ feature khác đổi nghĩa khi đi cùng con số

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-077 của app, T-050), issue #188
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** plan bảo dịch "dùng lại cách gọi đã có". Phiên lấy từ "transparency" của màn giới thiệu cho nhãn con trượt độ đậm; câu không có số thì đúng, trên con trượt có phần trăm thì "100%" đọc thành "trong suốt hoàn toàn", ngược với nét đậm nhất. Test chữ (đủ bản dịch, giữ tham số) không bắt được; chỉ rà soát độc lập thấy.
- **Đề xuất sửa:** plan dịch chữ liệt kê chuỗi đi cùng con số, ghi nghĩa của giá trị lớn nhất, có test chặn từ ngược nghĩa; rà soát task dịch kiểm nghĩa.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan; `docs/auto-run.md` bước 6, nội dung rà soát; issue #188.

### L-101. Plan của app chép code từ dự án mẫu, nhưng phiên cloud của app chỉ đọc được repo của app

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-078 của app, T-053), issue #188
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-053 của app bảo chép ba khối của `scripts/token-usage.py` từ dự án mẫu. Quyền GitHub của phiên `claude --cloud` chỉ gồm repo của app, nên phiên không đọc được repo mẫu và viết code theo mô tả ở mục 3.1 của plan. Code vẫn đúng nhờ mô tả kỹ, nhưng không so được với bản mẫu.
- **Đề xuất sửa:** plan chép code từ dự án mẫu dán nguyên code hay `diff` vào plan; `docs/ops/from-app.md` ghi giới hạn này.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan; `docs/ops/from-app.md` mục 1; issue #188.

### L-102. Dòng `Task: T-NNN` trong lời giao soạn plan làm số token soạn plan thành mẫu ước tính của task

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-079 của app, T-053), issue #188
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** lời giao cho phiên con ở bước soạn plan cuối phiên có dòng `Task: T-NNN` (L-071). Khi đó `scripts/token-usage.py report --csv` ghi một dòng chỉ có token soạn plan cho ticket đó, và `estimate` lấy dòng đó làm mẫu, kéo số gợi ý `token_estimate` xuống. Phần soạn plan của phiên chính chạy trên nhánh `claude/plan-T-NNN-...` lại vào dòng Chung: cách gán không nhất quán.
- **Đề xuất sửa:** bỏ dòng `Task:` khỏi lời giao soạn plan.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 5 (lời giao soạn plan không có dòng `Task:`); `docs/ops/token-usage.md`. Không đổi `scripts/token-usage.py`; issue #188.

### L-103. Sau task cuối, thư mục chính vẫn đứng trên nhánh task, token của việc cuối phiên tính vào task đó

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-080 của app, RUN-20261026-01), issue #188
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `scripts/token-usage.py` gán request của phiên chính theo nhánh của thư mục chính. Sau khi task cuối merge, phiên làm việc cuối phiên (chờ `main-guard`, `device-session`, staging, báo cáo) mà thư mục chính vẫn đứng trên nhánh task, nên phần đó (khoảng 12M token) tính vào task thay vì dòng Chung.
- **Đề xuất sửa:** `docs/auto-run.md` mục 4: rời nhánh task trước việc cuối phiên.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục 4, bước 0: `git checkout --detach origin/main` trước việc cuối phiên; `docs/ops/token-usage.md`. Không đổi `scripts/token-usage.py`; issue #188.
