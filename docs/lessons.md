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

Bài học `chung` ghi ở app thì được đưa về dự án mẫu qua issue `from-app` (`docs/ops/from-app.md`). Ở dự án mẫu, bài học từ app ghi nguồn là app và số issue. Chiều ngược lại, các app đã tạo trước đó nhận cải tiến của dự án mẫu qua PR đồng bộ dựng bằng `scripts/template-sync.sh` (`docs/ops/to-app.md`, ADR-0030).

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
- **Trạng thái:** applied
- **Áp dụng:** phần quét mã toàn số 0: PR #69; test của script chạy khi PR sửa `scripts/`: L-073 (PR #139); commit chỉ có tài liệu: nhánh nhẹ của `build-and-test`, PR #199 (T-039).

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
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`; PR #87.
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
- **Trạng thái:** applied
- **Kiểm chứng:** RUN-20261026-06 (T-045): phiên chính chạy nền `push-on-commit.sh` trong lúc phiên con làm bước 3 đến 5 (khoảng 28 phút); hai commit của phiên con được đẩy trong vòng một phút (`737593f` lúc 06:56, `550f9b9` lúc 07:08), không commit nào chỉ nằm trong container. Có hiệu quả. RUN-20261028-01 (T-047): `push-on-commit.sh` chạy nền từ 18:43, hai commit của phiên con (`bd30a6d`, `0668ae3`) được đẩy lúc 18:44, ngay khi có. Có hiệu quả.
- **Áp dụng:** `scripts/push-on-commit.sh` (vòng nền có giới hạn, đẩy khi có commit mới, mã 3 khi không đẩy được; có test), `docs/auto-run.md` mục Giao bước 3 đến 5, `CLAUDE.md` mục Lệnh. Anh Nhật duyệt trong retro 2026-10-27.

### L-091. Plan có bước chờ trước khi hiện quảng cáo không nêu trường hợp người dùng rời app giữa lúc chờ

- **Ngày, nguồn:** 2026-10-25, RUN-20261026-02 (T-024, PR #170)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** màn loading chờ quảng cáo mở app tới 40 giây. Rà soát độc lập (mức nên sửa) thấy: người dùng bấm Home trong lúc chờ thì vòng chờ vẫn chạy, và quảng cáo hiện khi người dùng quay lại, có thể sau nhiều phút. Khi đó nó thành một quảng cáo khi quay lại app, nhưng không theo khóa và luật của KB-14. Spec KB-13 và plan không nói tới trường hợp này, nên phiên không sửa mà chuyển cho người quyết. Test viết trước có dựng lại activity và tắt khóa giữa chừng (L-061), nhưng không có trường hợp rời foreground.
- **Bằng chứng:** nhật ký plan T-024 (RUN-20261026-02), mục Rà soát độc lập; mô tả PR #170.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi viết plan, gạch L-061: thêm "plan có bước chờ có giới hạn trước khi hiện thứ gì đó (quảng cáo, hộp thoại, điều hướng): nêu điều xảy ra khi app rời foreground giữa lúc chờ, mục 4 có test `pause().stop()` rồi `resume()`". Spec mẫu (`docs/templates/spec.md`) có thể nhắc ca này cho kịch bản có thời gian chờ.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan, nối vào gạch L-061: nêu điều xảy ra khi app ra nền giữa lúc chờ, mục 4 có test đưa activity ra nền rồi trở lại. Retro 2026-10-27.

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
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục Giao bước 3 đến 5 (mọi thay đổi ở file test sau commit test liệt kê riêng) và bước 6 (phiên rà soát nhận `git diff <commit test>..HEAD` của file test). Retro 2026-10-27.

### L-094. Hết hạn mức thanh toán GitHub Actions giữa phiên dài; kiểm tra đầu phiên không đọc hạn mức Actions

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (sau T-033, PR #193)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** sau 6 lần merge, GitHub không cấp máy cho job `main-guard` của commit merge T-033 ("recent account payments have failed or your spending limit needs to be increased"). Phiên dừng trước task thứ 7 theo `auto-run.md` mục 4. Phiên mở bằng `claude --cloud` nên không được `resume-run.yml` tiếp tục (L-044); `resume-run.yml` cũng là workflow của Actions nên cũng không chạy được khi hết hạn mức. Kiểm tra đầu phiên và mục Kiểm tra trước khi duyệt không có bước nào xem hạn mức Actions còn đủ cho danh sách.
- **Bằng chứng:** `scripts/wait-run.sh` mã 4, lần chạy 40280561101; `scripts/failure-kind.sh --playbook` ra PB-02; dòng thời gian của `docs/runs/RUN-20261026-04.md` (06:34). CI của 6 task khoảng 90 phút `pr-check` (T-033 33 phút vì build release) cộng 6 lần `main-guard`.
- **Đề xuất sửa:** `docs/auto-run.md` mục 1: dev kiểm Billing & plans của Actions trước phiên dài, ước tính số phút CI của danh sách (mỗi task khoảng 10 phút `pr-check` và 10 phút `main-guard`, gấp ba khi đổi thư viện). `docs/templates/run.md`: thêm dòng "hạn mức Actions còn đủ cho danh sách" ở mục Kiểm tra trước khi duyệt.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục 1, điểm 6 (dev xem hạn mức Actions, cách ước tính số phút CI); `docs/templates/run.md` dòng "Hạn mức Actions" ở mục Kiểm tra trước khi duyệt. Retro 2026-10-27.

### L-095. Phiên con xóa một test cũ thêm sau khi plan viết, kéo theo phần đối chứng của hành vi vẫn giữ

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (T-030, PR #189)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `CollapsibleNativeAdTest.loadingExpanded_placeholderCtaStays36dp` do T-028 thêm sau khi plan T-030 viết, nên không có trong bảng "Test đã có" của plan. Nửa đầu của test kiểm hành vi mà T-030 bỏ (CL-01), nửa đối chứng kiểm CTA 44 dp của khung chờ medium (T-028, vẫn giữ). Phiên con xóa cả test; rà soát độc lập xếp mức chặn (ADR-0029, quyết định 2) và phần đối chứng được giữ lại ở `NativeAdCardTest.mediumPlaceholder_ctaIs44dp`. Sau khi phiên chính thêm vào lời giao việc một dòng về việc này, bốn task sau không lặp lại.
- **Bằng chứng:** nhật ký plan T-030 (RUN-20261026-04), mục Rà soát độc lập; commit `df7f6f6`.
- **Đề xuất sửa:** `docs/auto-run.md`, mục Giao bước 3 đến 5 cho phiên con, phần giao việc chung: "không xóa test cũ nào, kể cả test thêm sau khi plan viết; hành vi cũ bị plan thay thì chỉ bỏ câu khẳng định của hành vi đó, phần còn lại giữ (chuyển file nếu cần, báo là ngoài mục 3.2)".
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục Giao bước 3 đến 5 (không xóa test cũ nào, kể cả test thêm sau khi plan viết) và bước 6 (phiên rà soát nhận danh sách test bị xóa, xóa là mức chặn). Retro 2026-10-27.

### L-096. Dọn vòng đẩy nền bằng `pkill -f` dù L-082 đã cấm

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-04 (sau T-026)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên chính chạy một vòng nền đẩy nhánh task mỗi khi có commit mới của phiên con (L-070). Sau khi merge T-026, phiên dọn vòng đó bằng `pkill -f "Push T-026"`; lệnh thoát 144 vì chuỗi khớp dòng lệnh của chính shell đó. Không mất gì (lệnh gỡ worktree chạy trước trong cùng dòng), nhưng L-082 đã cấm đúng việc này. Từ T-030 phiên dừng vòng nền bằng công cụ dừng tác vụ nền của môi trường.
- **Bằng chứng:** dòng thời gian 01:54 của `docs/runs/RUN-20261026-04.md`; lệnh `git worktree remove ../task-T-026 && pkill -f "Push T-026"` thoát 144.
- **Đề xuất sửa:** `docs/auto-run.md` bước 9 (đoạn L-082): nói rõ cách dừng vòng nền: công cụ dừng tác vụ nền của phiên, hay một vòng có giới hạn tự hết; không kết thúc vòng nền bằng lệnh tìm theo chuỗi. Mục Giao bước 3 đến 5 có thể nêu mẫu vòng đẩy nền (đọc `git rev-parse <nhánh>` mỗi phút, gọi `save-branch.sh` khi đổi).
- **Trạng thái:** applied
- **Áp dụng:** Cùng L-090: `scripts/push-on-commit.sh` thay vòng đẩy tự viết; `docs/auto-run.md` mục Giao bước 3 đến 5: dừng sớm bằng công cụ dừng tác vụ nền, không `pkill -f`. Retro 2026-10-27.

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
- **Kiểm chứng:** RUN-20261026-06: T-045 tạo nhánh ngay sau khi T-041 merge nhưng chỉ chạy `push-on-commit.sh` sau khi `main-guard` của T-041 xanh (06:53) và không có issue `main-broken`; commit đầu của T-045 đẩy lúc 06:56. Có hiệu quả. RUN-20261028-01: nhánh T-047 tạo lúc 18:28 (chưa có commit), `push-on-commit.sh` chỉ chạy sau khi `main-guard` của T-046 xanh (18:43) và không có issue `main-broken`. Có hiệu quả. RUN-20261028-02: phiên con commit test T-049 lúc 08:23 khi `main-guard` của T-048 còn chạy; `push-on-commit.sh` chỉ chạy sau khi `main-guard` xanh (08:32) và không có issue `main-broken`, commit đẩy lúc đó. Có hiệu quả.
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

### L-104. Lệnh tải log job của `auto-run.md` bước 8 dùng cờ `--allow-escape-sequences` mà `gh` của môi trường cloud không có

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-05, task T-044 (PR #208)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `docs/auto-run.md` bước 8 ghi `gh api --allow-escape-sequences "repos/{owner}/{repo}/actions/jobs/<job id>/logs" > ci.log`. `gh` 2.89.0 của môi trường cloud trả "unknown flag: --allow-escape-sequences" và in phần trợ giúp vào `ci.log`; `scripts/failure-kind.sh --playbook ci.log` vẫn chạy trên tệp đó và báo `khong-co`, dễ hiểu nhầm là lỗi thật chưa có trong bảng. Bỏ cờ thì tải được log (1185 dòng).
- **Đề xuất sửa:** `docs/auto-run.md` bước 8 bỏ cờ `--allow-escape-sequences`; `scripts/failure-kind.sh` báo lỗi riêng khi tệp log là phần trợ giúp của `gh` (dòng `Usage:  gh api`).
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` bước 8 tải log bằng `gh run view <id> --log-failed`; `scripts/failure-kind.sh --playbook` in `log-hong` khi tệp là thông báo lỗi hay trợ giúp của `gh` (có test). Thực tế hai chiều: `gh` mới đòi cờ `--allow-escape-sequences`, `gh` 2.89.0 không biết cờ đó. Retro 2026-10-27.

### L-105. Job `ui-test` trên máy của GitHub nhận 403 từ Maven Central khi tải thư viện của test trên emulator

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-05, task T-044 (PR #208, lần chạy 40369736285)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `:app:connectedStagingDebugAndroidTest` đỏ với "Could not GET 'https://repo.maven.apache.org/maven2/.../proto-google-common-protos-2.17.0.pom'. Received status code 403 from server: Forbidden" (thư viện của `com.android.tools.utp`), rồi "Không có test nào chạy trên emulator". `failure-kind.sh --playbook` báo `khong-co`. Chạy lại job đỏ một lần thì xanh. Lỗi tải, không phải lỗi của code.
- **Đề xuất sửa:** thêm một dòng vào `docs/ops/failure-playbook.md`: mẫu `repo\.maven\.apache\.org.*Received status code 403`, job `ui-test` hay `build-and-test`, loại `chay-lai` (một lần, không tính vào 3 lần sửa), kèm tệp log thật trong `scripts/tests/playbook/`.
- **Trạng thái:** applied
- **Áp dụng:** `docs/ops/failure-playbook.md` dòng PB-11 (`chay-lai`, một lần), tệp `scripts/tests/playbook/PB-11.log` chép từ lần chạy 40369736285 lần 1. Retro 2026-10-27.

### L-106. Chạy thử của plan dùng khung tạm cao 0 dp nên không thấy test có sẵn mâu thuẫn với bố cục mới

- **Ngày, nguồn:** 2026-10-26, RUN-20261026-05, task T-041 (dừng ở bước 5)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-041 đổi vị trí khung thẻ 250 dp của màn xem thử (đè ở đáy vùng bước thay vì nằm dưới pager). Mục 4.1 chạy thử với khung thẻ cao 0 dp ở bước toàn trang, nên không đo được bố cục ở bước thẻ. Test có sẵn `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep` khẳng định thẻ nằm dưới pager về hình học; trong phiên test này đỏ và task phải dừng (điều kiện 3), sau khi đã viết xong code.
- **Đề xuất sửa:** `docs/CLAUDE.md`, mục Khi viết plan: plan đổi vị trí hay kích thước của một phần tử giao diện có tag thì tìm mọi test đo `bounds` hay vị trí của tag đó (`grep -rn "<TAG>" */src/test*`) và liệt kê ở mục 3.2 từng câu khẳng định sẽ còn đúng hay phải đổi (đặt thành câu hỏi khi phải đổi); chạy thử ở mục 4.1 dựng đúng bố cục mới, không dùng khung tạm.
- **Trạng thái:** applied (cách khác: anh Nhật chọn 2026-10-27; không bắt chạy thử lúc soạn plan, theo ADR-0029)
- **Áp dụng:** ADR-0031. Lúc soạn plan: mục 3.5 "Hành vi đã có bị đổi" của mẫu plan, `scripts/existing-tests.py` (trên plan T-041 phiên bản 1, test gây dừng đứng đầu 4 test "Cần xem trước"), quy tắc 13 của `scripts/plan-check.py`, `docs/CLAUDE.md` mục Khi viết plan, `docs/ops/pre-run-review.md`. Trong phiên: câu khẳng định cũ trái điều plan đã duyệt nói rõ được viết lại khi đủ sáu điều (`risk-scoring.md` mục 4, `auto-run.md` bước 4, 6, 10).


### L-107. Plan nâng `token_estimate` gần gấp đôi số gợi ý theo một hai task của phiên trước

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-081 của app), issue #204
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** sau một phiên có task dùng nhiều token, plan ở app nâng `token_estimate` gần gấp đôi số `scripts/token-usage.py estimate` gợi ý. Task thật dùng 0,4 đến 0,7 lần số nâng tay, vì mẫu của script đã gồm token của phiên con.
- **Đề xuất sửa:** `docs/CLAUDE.md` mục Khi viết plan, dòng `token_estimate`: không nâng số gợi ý theo một hai task của một phiên.
- **Trạng thái:** applied
- **Áp dụng:** `docs/CLAUDE.md` mục Khi viết plan; issue #204.

### L-108. Điểm nên sửa chỉ đòi thêm test cho code đã đúng, mà luật bắt test viết trước phải thất bại

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-082 của app), issue #204
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** rà soát độc lập xếp mức nên sửa một điểm chỉ đòi thêm test cho hành vi code đã làm đúng. Luật "phải sửa khi có test viết trước thất bại đúng lý do" không áp được, vì test đó không thể thất bại trước; phiên phải đoán nên thêm hay ghi báo cáo.
- **Đề xuất sửa:** `docs/auto-run.md` bước 6, dòng Nên sửa: thêm test khi test nằm trong file của mục 3.2 và thất bại khi tạm bỏ dòng code đó (đột biến), ghi kết quả vào nhật ký; không thì ghi vào báo cáo.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` bước 6, dòng Nên sửa; issue #204.
- **Kiểm chứng:** RUN-20261030-01: ba điểm nên sửa hay chặn đòi test cho hành vi đã đúng (T-054 dòng nối cổng gửi của nguồn, T-059 `LIST_ROW`, T-055 đọc tracker) đều có lần đột biến làm test mới đỏ, ghi vào nhật ký plan; rà soát lại dựa vào kết quả đột biến để đóng điểm.

### L-109. Phiên con chết giữa bước 3 vì lỗi API 403 `oauth_org_not_allowed`, phiên chính đứng chờ 2 giờ 30 phút

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-084 của app, RUN-20261026-03), issue #204
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** phiên con trả "API Error: 403 oauth_org_not_allowed" rồi kết thúc mà không có báo cáo. Phiên `claude --cloud` không biết làm gì tiếp và đứng tới khi người nhắn.
- **Đề xuất sửa:** `docs/auto-run.md` mục Giao bước 3 đến 5: phiên con kết thúc không báo cáo thì phiên chính đọc worktree và làm tiếp từ phần đã có, không tính là task dừng; bảng tra lỗi thêm dòng cho `oauth_org_not_allowed` (loại `cho`) kèm log thật.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục Giao bước 3 đến 5; `docs/ops/failure-playbook.md` dòng PB-12, tệp `scripts/tests/playbook/PB-12.log` và test; issue #204.

### L-110. PR xuất lại ảnh thiết kế không sinh lại `design/review/`, task sau thấy ảnh so sánh đổi ngoài mục 3.2

- **Ngày, nguồn:** 2026-10-26, app camera-pose (bài học L-085 của app), issue #204
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** một PR xuất lại ảnh thiết kế của feature mà không chạy `scripts/design-compare.sh`. Task sau của feature chạy script ở bước 5 và thấy ảnh so sánh, `summary.md` đổi mà không do task, ngoài mục 3.2 của plan.
- **Đề xuất sửa:** `docs/ops/design-workflow.md`: PR xuất lại ảnh chạy `design-compare.sh` và commit `design/review/` cùng PR. `docs/auto-run.md` bước 5: ảnh so sánh đổi mà ảnh chuẩn của task không đổi thì không commit, ghi vào mô tả PR.
- **Trạng thái:** applied
- **Áp dụng:** `docs/ops/design-workflow.md`, `docs/auto-run.md` bước 5; issue #204.

### L-111. Token của phiên chính làm task trong worktree bị tính vào dòng Chung

- **Ngày, nguồn:** 2026-10-27, RUN-20261026-06
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** T-041 là task tiếp tục, phiên chính tự làm phần còn lại (câu hỏi 9, `check.sh`, mở PR) trong worktree `../task-T-041`, không giao phiên con. Thư mục chính ở trên nhánh báo cáo suốt lúc đó, nên `scripts/token-usage.py` gán các request này vào dòng Chung: T-041 chỉ ghi 2,2M so với ước tính 22M (0,1 lần), dòng Chung 12,8M. Số trong `docs/metrics/token-usage.csv` làm mẫu ước tính của `estimate` vì vậy bị thấp.
- **Bằng chứng:** bảng Token của báo cáo RUN-20261026-06; nhật ký T-041.
- **Đề xuất sửa:** `docs/auto-run.md` mục Giao bước 3 đến 5 (hay `docs/ops/token-usage.md`): phiên chính tự làm bước của task thì làm trong thư mục chính trên nhánh task (như trước L-103), chỉ dùng worktree cho phiên con; hoặc `scripts/token-usage.py` gán request theo thư mục làm việc của lệnh (`cwd` của worktree `task-T-NNN`). Ghi chú trong CSV các dòng task tiếp tục để `estimate` bỏ qua.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/token-usage.py estimate` cộng các dòng của cùng một task qua nhiều phiên thành một mẫu, có test (PR #221). Gán token của phiên chính: lệnh `scripts/token-usage.py mark T-NNN` / `mark chung` từ app camera-pose (T-063, bài học L-083 của app, PR VGL-Studio/camera-pose#186), thay quy tắc "làm ở thư mục chính" của PR #221; `docs/auto-run.md` bước 1, 10, mục Giao bước 3 đến 5, mục Tiếp tục, mục 4 bước 0; `docs/ops/token-usage.md`; test trong `scripts/tests/run.sh`.

### L-112. Phiên dừng hay chờ người vì lệch nhỏ mà người sẽ đồng ý ngay, và kết thúc cả phiên khi môi trường chỉ chặn một loại task

- **Ngày, nguồn:** 2026-10-28, app camera-pose (ADR-1005 và T-067 của app, bài học L-086 của app, RUN-20261027-01)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở app camera-pose, nhiều lệch nhỏ khỏi chữ của plan không có quy tắc: ngân sách kịch bản emulator vượt, `need` còn giữ khi emulator đã có camera, mốc tài liệu dời chỗ sau đồng bộ dự án mẫu, test viết trước khác kỳ vọng của plan, chi tiết do ktlint buộc. Phiên phải hỏi người trước phiên, soạn plan phiên bản mới dù nội dung không đổi, hay tự quyết mà không có quy tắc cho phép. Một task sửa `.github/workflows/` bị GitHub từ chối lệnh đẩy, vì token của phiên `claude --cloud` không có quyền `workflow`. `docs/auto-run.md` mục Khi không đẩy được chỉ nói kết thúc phiên khi lần đẩy sau cũng bị từ chối, không nói tới việc chỉ một loại task bị chặn.
- **Bằng chứng:** thông báo thật của lệnh đẩy ở `scripts/tests/playbook/PB-14.log` (`refusing to allow an OAuth App to create or update workflow ... without workflow scope`); plan T-067 của app liệt kê nguồn của từng lệch.
- **Đề xuất sửa:** một ADR liệt kê các lệch nhỏ phiên được tự làm rồi báo sau, ranh giới của từng lệch và các việc vẫn cấm; báo cáo phiên có mục "Quyết định AI đã tự làm"; bảng tra lỗi có loại `bo-qua-loai` và dòng cho lỗi thiếu quyền `workflow`; `scripts/plan-check.py --touches` cho biết task nào thuộc loại bị chặn.
- **Trạng thái:** applied
- **Áp dụng:** ADR-0032; `docs/risk-scoring.md` mục 4; `docs/auto-run.md` bước 6, mục Khi không đẩy được bước 5 và 6, bảng của mục 4, mục 5; `docs/templates/run.md` mục "Quyết định AI đã tự làm"; `docs/ops/failure-playbook.md` loại `bo-qua-loai`, dòng PB-14, tệp `scripts/tests/playbook/PB-14.log`; `docs/ops/test-sessions.md` mục 5; `scripts/plan-check.py --touches`; test trong `scripts/tests/run.sh`.

### L-113. Kiểm tra thiết lập đỏ vì một lần R2 trục trặc hay CDN giữ 404 cũ, và test của script dựa vào cấu hình git của máy

- **Ngày, nguồn:** 2026-10-28, app camera-pose (bài học L-087 và plan T-065 của app, PR VGL-Studio/camera-pose#200)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `scripts/setup-check.sh` hỏi mỗi file R2 một lần. Một lần R2 trả 429, 5xx hay không kết nối được là mở issue `setup-broken` cả ngày và chặn `start-run`. Một file tải lên sau khi CDN đã trả 404 thì CDN giữ câu trả lời 404 đó tới một năm; script chỉ báo "thiếu file", người đi tìm file đã có trên R2. Issue `setup-broken` chỉ đóng ở lần kiểm buổi sáng, nên phiên mở sau khi sửa vẫn dừng vì issue cũ. Ở test của script: khối test dựng kho git giả qua trên máy có `~/.gitconfig`, đỏ trên runner (không có tên người commit, không có `init.defaultBranch`).
- **Bằng chứng:** camera-pose 2026-10-27: một 404 được giữ với `max-age=31536000` sau khi tải file lên; PR camera-pose #198, `pr-check` lần chạy 40566657337: `fatal: empty ident name ... not allowed`.
- **Đề xuất sửa:** hỏi lại một lần khi gặp 000, 429, 5xx; với 404, 403 hỏi lại kèm tham số chống bộ nhớ đệm để báo đúng lỗi CDN. `env-canary.yml` kiểm lại sau 90 giây trước khi mở issue. `start-run.yml` đóng `setup-broken` khi kiểm tra xanh. Có chỗ móc cho kiểm tra và test riêng của app. `scripts/tests/run.sh` chạy như runner trên mọi máy.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/setup-check.sh`: `http_status` (hỏi lại khi 000, 429, 5xx), `cdn_hint`, nạp `scripts/setup-check-app.sh` của app nếu có (`SETUP_CHECK_APP_CHECKS`). `env-canary.yml`: kiểm lại sau 90 giây, ô `setup_only`, `persist-credentials: false`, `env -u GH_TOKEN`. `start-run.yml`: job `close-setup-broken` riêng (chỉ job này có `issues: write`), chạy trước job kích hoạt routine. `scripts/tests/run.sh`: `GIT_CONFIG_GLOBAL=/dev/null`, `GIT_AUTHOR_*`, `GIT_COMMITTER_*` ở đầu; nạp `scripts/tests/app.sh` của app nếu có. Có test. `docs/ops/to-app.md` mục 6, `docs/ops/staging-and-runs.md`. Phần riêng của camera-pose (catalog tư thế, bí mật Remote Config, thời hạn job dài hơn) ở lại app.

### L-114. `recordRoborazzi*` không ghi lại ảnh khi task test đã cập nhật

- **Ngày, nguồn:** 2026-10-28, RUN-20261028-02 (T-049)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** danh sách chạy và `CLAUDE.md` (mục Lệnh) bảo ghi ảnh chuẩn bằng `./gradlew :core:ads:recordRoborazziDebug` và `:app:recordRoborazziStagingDebug`. Phiên con chạy hai lệnh này sau khi đã chạy test, Gradle coi task test là đã cập nhật nên không chạy lại test, ảnh chuẩn không đổi. Phiên con phải tự đoán và chạy lại với `testDebugUnitTest --rerun` / `testStagingDebugUnitTest --rerun` cùng lệnh ghi ảnh. Không gây lỗi vì phiên con thấy ảnh chưa đổi, nhưng phiên khác có thể tưởng ảnh đã ghi và đi tiếp tới `verifyRoborazzi` đỏ.
- **Bằng chứng:** báo cáo của phiên con T-049 (nhật ký plan T-049, Lệch so với plan, ý 5); ảnh chỉ đổi sau lần chạy có `--rerun`.
- **Đề xuất sửa:** `CLAUDE.md` mục Lệnh và `docs/templates/plan.md` (cách ghi ảnh chuẩn): ghi lệnh kèm `--rerun` của task test, ví dụ `./gradlew :core:ads:recordRoborazziDebug :core:ads:testDebugUnitTest --rerun`, rồi `git status --short -- '*/src/test/screenshots/'` để thấy ảnh đã đổi.
- **Trạng thái:** applied
- **Áp dụng:** `CLAUDE.md` mục Lệnh, `docs/templates/plan.md` mục 4, `docs/ops/new-app.md`: lệnh ghi ảnh kèm `--rerun` của task test, rồi `git status` thư mục ảnh. Đã thử trên máy dev (2026-10-28): chạy `:core:ads:recordRoborazziDebug` lần hai báo `testDebugUnitTest UP-TO-DATE`, không chạy test; thêm `:core:ads:testDebugUnitTest --rerun` thì test chạy lại.
- **Kiểm chứng:** 2026-10-30, rà soát độc lập lần chạy thử plan T-081 (PR #332): mẫu `'*/src/test/screenshots/'` không khớp tệp nào, lệnh luôn in rỗng. PR #333 sửa (chưa merge lúc ghi dòng này).

### L-115. Dòng KDoc dài hơn 120 ký tự qua ktlint, và điểm nên sửa chỉ về định dạng không sửa được vì không có test thất bại trước

- **Ngày, nguồn:** 2026-10-28, RUN-20261028-02 (T-049)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** rà soát độc lập của T-049 xếp nên sửa một dòng KDoc 163 ký tự ở `NativeFlowStep.kt:258`, quá `max_line_length = 120` của `.editorconfig`; `ktlintCheck` của `check.sh` không báo. Luật Nên sửa của `docs/auto-run.md` bước 6 đòi test viết trước thất bại đúng lý do, mà chú thích không có test, nên phiên không sửa và để người quyết. Một sửa chỉ là xuống dòng chú thích phải chờ một PR riêng.
- **Bằng chứng:** PR #244, mục Rà soát độc lập; nhật ký plan T-049.
- **Đề xuất sửa:** (1) xem vì sao `max-line-length` của ktlint bỏ qua dòng này (kiểu `android_studio` có thể bỏ qua chú thích) và bật kiểm cho chú thích nếu được; (2) ADR-0032 thêm một lệch nhỏ: điểm nên sửa chỉ về chú thích hay định dạng, không đổi code chạy, trong file của mục 3.2, thì phiên sửa không cần test, ghi vào mục Quyết định AI đã tự làm.
- **Trạng thái:** applied
- **Áp dụng:** (2) ADR-0033, lệch nhỏ L12: điểm nên sửa chỉ về chú thích hay định dạng trong file được phép thì phiên sửa không cần test viết trước (`git diff -w` chỉ có chú thích hay khoảng trắng, `scripts/check.sh` qua), ghi vào mục Quyết định AI đã tự làm; `docs/auto-run.md` bước 6, `docs/risk-scoring.md` mục 4, `docs/templates/run.md`. (1) Ticket T-050: ktlint không kiểm dòng dài ở cả repo (145 dòng Kotlin dài hơn 120 ký tự, 129 dòng là chú thích, đếm 2026-10-28), không chỉ dòng này. Không mở PR riêng cho dòng KDoc ở `NativeFlowStep.kt:258`; T-050 sửa cùng các dòng khác. Cập nhật 2026-10-29 (T-050): ktlint 1.5.0 có bật `max-line-length` và đọc `.editorconfig`, nhưng bỏ qua dòng chỉ có chú thích (thử `//`, KDoc, `/* */`: không báo; dòng code hay code kèm chú thích cuối dòng: báo), không có tùy chọn bật lại. Nhóm chốt: giới hạn 120 chỉ áp cho code, chú thích dài không phải lỗi; không thêm kiểm riêng, không sửa các dòng chú thích dài (17 dòng ngày 2026-10-29, đếm theo ký tự; con số 145 cũ đếm theo byte).

### L-116. Phiên mở `local` không có bước kiểm môi trường đúng: `cloud-env-check.sh` luôn báo lỗi ở máy dev

- **Ngày, nguồn:** 2026-10-29, camera-pose (issue #258, L-089 của app; RUN-20261028-01, RUN-20261028-02)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `docs/auto-run.md` mục 2 chỉ có `scripts/cloud-env-check.sh` để kiểm môi trường đầu phiên. Ở máy dev script này luôn báo lỗi, vì đòi tệp phiên bản của `scripts/cloud-setup.sh` và init script mirror Maven. Theo chữ của bảng, phiên `local` phải kết thúc ngay; hai phiên của app tự quyết không coi đó là điều kiện dừng, không để lại quy tắc.
- **Bằng chứng:** `cloud-env-check.sh` ở máy dev ngày 2026-10-28: "môi trường dựng từ setup script phiên bản không có ... thiếu ~/.gradle/init.d/maven-central-mirror.gradle"; báo cáo RUN-20261028-01 của camera-pose, mục Quyết định AI đã tự làm; PR VGL-Studio/camera-pose#218.
- **Đề xuất sửa:** phiên `local` chạy `scripts/local-setup.sh --check` (JDK, SDK có nền tảng `compileSdk`, tệp `env.sh`; thoát khác 0 khi thiếu) thay cho `cloud-env-check.sh`, qua thì `source` tệp `env.sh`; không qua thì kết thúc phiên như dòng cũ, để người chạy `scripts/local-setup.sh`.
- **Trạng thái:** applied
- **Áp dụng:** `docs/auto-run.md` mục 2 (dòng môi trường của bảng, bước 1 của Kiểm tra build ở chế độ nền), `docs/ops/session-capabilities.md` (dòng `local`). Máy dev từng build bằng JDK, SDK trong thư mục tạm của phiên Claude thì `--check` báo thiếu dù build được: chạy `scripts/local-setup.sh` một lần trước khi mở phiên `local`.
### L-117. `ui-test` đỏ vì runner tải gói Android Emulator lỗi, bảng tra lỗi xếp là lỗi thật

- **Ngày, nguồn:** 2026-10-29, camera-pose (issue #259, L-090 của app; RUN-20261028-02, T-071, PR VGL-Studio/camera-pose#216)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** sau `merge-if-green.sh --update`, job `ui-test` đỏ sau khoảng 2 phút: `sdkmanager` tải gói Android Emulator lỗi ("Error on ZipFile unknown archive"), emulator không khởi động, không test nào chạy. `failure-kind.sh --playbook` trả `khong-co`, nên theo bước 8 phiên phải coi là lỗi thật dù không có gì để sửa. Lần chạy lại ngay cũng lỗi; phiên tự xếp là hạ tầng, lần sau 2 phút thì xanh.
- **Bằng chứng:** camera-pose, `ui-test` lần chạy 40734943741, lần thử 1 và 2 (17:23 và 17:36 UTC ngày 2026-10-28) đỏ cùng mẫu, lần thử 3 xanh.
- **Đề xuất sửa:** dòng `lich-thu-lai` trong bảng tra lỗi, chỉ khớp khi log có cả dòng tải gói lỗi và dòng "Không có test nào chạy trên emulator", để lỗi emulator do code (test chạy rồi đỏ) không bị xếp nhầm.
- **Trạng thái:** applied
- **Áp dụng:** PB-15 trong `docs/ops/failure-playbook.md`, log thật ở `scripts/tests/playbook/PB-15.log`. Cột Mẫu nhận nhiều mẫu nối bằng "và" (`scripts/failure-kind.sh`: dòng khớp khi log có đủ mọi mẫu), có test cho cả hai phía và lần đột biến chỉ dò mẫu đầu làm test đỏ.
### L-118. Rà soát độc lập nêu điểm chặn bằng đọc code, test chạy thật không tái hiện được, mà bước 6 không có lối ra

- **Ngày, nguồn:** 2026-10-29, camera-pose (issue #250, L-052 của app; RUN-20261023-04, T-015)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** rà soát độc lập của T-015 nêu điểm chặn bằng cách đọc code: vùng chạm của thanh trượt dưới 48 dp. Phiên viết code viết test chạm thật ở cả hai phía (`AdjustPanelTest.adjustSlider_touchHeight48`), test qua, không tái hiện được. Bước 6 chỉ có Chặn, Nên sửa, Ghi chú; mức Chặn luôn quay lại bước 4, nên phiên phải tự quyết tin test, không để lại quy tắc.
- **Bằng chứng:** báo cáo RUN-20261023-04 của camera-pose, T-015; issue #250.
- **Đề xuất sửa:** điểm chặn được đổi sang "không tái hiện" khi có test chạy thật kiểm đúng điều rà soát nêu, test qua trên code của task, và test thất bại khi đột biến code theo hướng điểm chặn; test vào commit của task; báo cáo ghi ở Quyết định AI đã tự làm và thêm một dòng Cần người kiểm tra; phần đó qua rà soát lại.
- **Trạng thái:** applied
- **Áp dụng:** ADR-0035. `docs/auto-run.md` bước 6 (dòng Chặn của bảng mức, mô tả PR), `docs/templates/run.md` (mục Quyết định AI đã tự làm, dòng mới ở Cần người kiểm tra). Giới hạn thêm so với đề xuất: chỉ cho điểm chặn nói code chạy sai, không cho điểm chặn về quy tắc (lệch plan, file ngoài phạm vi, test bị đổi hay xóa, bí mật, thiết kế, hiểu spec khác nhau); mỗi điểm một lần; rà soát lại tính vào giới hạn 2 vòng.
### L-119. `release.sh` luôn đòi khóa dev AppsFlyer, nên app giữ khung mà chưa bật AppsFlyer không phát hành được

- **Ngày, nguồn:** 2026-10-29, camera-pose (issue #261; spec `docs/features/ads/spec.md` của app, câu 3)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** camera-pose lấy khung quảng cáo của dự án mẫu (T-074) nhưng chưa bật AppsFlyer: không có SDK, quyền hay dữ liệu gửi đi. `scripts/release.sh` và build production (`app/build.gradle.kts`) luôn đòi `appsFlyerDevKey`, nên app không phát hành được bằng script mẫu nguyên vẹn; app không được sửa `release.sh`, điền khóa giả thì bước kiểm mất ý nghĩa, giữ `release.sh` cũ (`hold=`) thì app lệch dần khỏi mẫu.
- **Bằng chứng:** issue #261; ADR-0027, quyết định 2.
- **Đề xuất sửa:** chỉ đòi khóa khi app có SDK AppsFlyer, nhận biết từ build chứ không từ việc thiếu dòng trong `release.properties`; app có SDK mà thiếu khóa vẫn dừng; không có SDK mà có khóa thì in chú ý; có test cho cả ba trường hợp.
- **Trạng thái:** applied
- **Áp dụng:** `scripts/release.sh` (anh Nhật cho phép sửa trong phiên 2026-10-29) đòi `appsFlyerDevKey` khi một file `*.gradle.kts` khai báo `libs.appsflyer` hay `com.appsflyer:`, không có SDK mà có khóa thì in CHÚ Ý và không truyền khóa; `app/build.gradle.kts` đòi `-PappsFlyerDevKey` (khác rỗng) khi `com.appsflyer` có trong classpath runtime của biến thể production. Test của `release.sh` trong `scripts/tests/run.sh`; build production thử tay bốn trường hợp. Ghi chú 2026-10-29 ở ADR-0027, `docs/ops/release.md` mục 2.2, 2.3.

### L-120. Test đọc file build không khai báo file đó làm đầu vào, nên bộ nhớ đệm của Gradle giấu một lần làm nhánh chính đỏ

- **Ngày, nguồn:** 2026-10-29, dự án mẫu (PR #265, phát hiện ở CI của PR #266)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** #265 đổi phần kiểm khóa AppsFlyer trong `app/build.gradle.kts`. `ProductionPropertiesTest` đọc chữ của file này và vẫn đòi dạng cũ, nên đáng lẽ phải đỏ. Nhưng CI của #265 và `main-guard` trên commit merge đều xanh, vì file build không phải đầu vào của task test: Gradle trả kết quả cũ trong bộ nhớ đệm, không chạy lại test. CI của #266 đổi `core/ads`, task test của `:app` chạy lại, và lỗi mới lộ ra. Lúc làm #265, em chỉ chạy `scripts/tests/run.sh` và build production bằng tay, không chạy unit test của `:app`.
- **Bằng chứng:** CI của #266, lần chạy 40821119792 (`ProductionPropertiesTest > appsFlyerDevKey_isRequiredForProduction FAILED`); main sạch tại 47782ec với `--rerun` cũng đỏ.
- **Đề xuất sửa:** test đọc file của repo khai báo file đó làm đầu vào của task test (`inputs.file(...)`), như `remoteConfigKeysDocs`. Sửa file build thì chạy unit test của module đó với `--rerun`.
- **Trạng thái:** applied
- **Áp dụng:** `app/build.gradle.kts` khai báo `build.gradle.kts` làm đầu vào của mọi task test của `:app`. `ProductionPropertiesTest` viết lại câu khẳng định theo hành vi mới của #265 (ghi chú 2026-10-29 ở ADR-0027), giữ ý định: production có SDK thì không build được khi thiếu khóa. Đột biến bỏ `hasAppsFlyer &&`, chạy không kèm `--rerun`: test đỏ, tức vừa bắt được đột biến vừa chứng minh file build đã là đầu vào.

### L-121. Init script của máy cloud hỏi Plugin Portal trước kho Google, nên Maven Central giới hạn tần suất là dừng build

- **Ngày, nguồn:** 2026-10-29, Photune (issue #270; PR #9 của app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** lần `env-canary` đầu của app đỏ trên máy cloud. Init script của `scripts/cloud-setup.sh` khai báo kho cho `pluginManagement` theo thứ tự: bản sao Maven Central, rồi Plugin Portal. Plugin Portal chuyển các plugin nó không giữ (Android Gradle plugin, Google services, Crashlytics) sang Maven Central; Maven Central trả 429, và Gradle dừng trước khi hỏi kho Google.
- **Bằng chứng:** issue #270; sau khi sửa, `env-canary` và hai phiên tự chạy của app tải được plugin.
- **Đề xuất sửa:** init script thêm `google()` ngay sau bản sao Maven Central, trước Plugin Portal; tăng `SETUP_VERSION`; test kiểm thứ tự kho; thêm dòng vào bảng sự cố của `docs/ops/setup-new-app.md`.
- **Trạng thái:** applied
- **Áp dụng:** PR #278: `scripts/cloud-setup.sh` thêm `google()` trước `gradlePluginPortal()`, `SETUP_VERSION` 4; test thứ tự kho trong `scripts/tests/run.sh`; dòng sự cố ở `docs/ops/setup-new-app.md`; ghi chú gộp `SETUP_VERSION` ở `docs/ops/to-app.md` mục 7. Bước vận hành ngoài PR: người dán lại setup script vào môi trường cloud.

### L-122. Hộp thoại đồng ý trên Xiaomi HyperOS đặt nhãn nút ở `content-desc`, kịch bản không bấm được nút từ chối

- **Ngày, nguồn:** 2026-10-29, Photune (issue #271; commit 1e98ea3 của app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** trên Xiaomi HyperOS (Android 16), WebView của hộp thoại đồng ý UMP chỉ đặt nhãn nút ở `content-desc`, không ở `text`. `CONSENT_DECLINE` của `scripts/device-scenario.py` chỉ khớp `text`, nên kịch bản trên máy thật kẹt ở hộp thoại.
- **Bằng chứng:** issue #271; trên máy Xiaomi, kịch bản đi qua hộp thoại sau khi sửa.
- **Đề xuất sửa:** `CONSENT_DECLINE` khớp cả `desc=Do not consent | desc=Không đồng ý`; fixture cây giao diện có nhãn chỉ ở `content-desc` và test trong `scripts/tests/run.sh`.
- **Trạng thái:** applied
- **Áp dụng:** PR #279: `CONSENT_DECLINE` của `scripts/device-scenario.py` khớp cả `desc=`; fixture `consent-desc.xml` và test.

### L-123. Commit test viết trước mà chưa định dạng, nên `check.sh` đỏ vì ktlint và tốn một vòng sửa

- **Ngày, nguồn:** 2026-10-29, Photune (issue #272; L-001 của app; PR #11, #17 của app)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** RUN-20261028-01 của app, T-002: test viết trước được commit sau khi chỉ chạy task test. Ở bước 5, `scripts/check.sh` đỏ ở `ktlintTestSourceSetCheck` vì thứ tự import, phải chạy `ktlintFormat` rồi commit thêm một lần chỉ để sửa định dạng. ADR-0011 chỉ dặn chạy `ktlintFormat` khi ktlint đã báo lỗi.
- **Bằng chứng:** issue #272; RUN-20261029-01 của app (4 task) không có commit nào chỉ để sửa định dạng (app chưa đánh dấu `verified`).
- **Đề xuất sửa:** `docs/auto-run.md` bước 3 và bước 4: chạy `./gradlew ktlintFormat` trước khi commit test và trước khi commit code; lệnh định dạng mọi file Kotlin nên chỉ commit file của task; lệnh chỉ đổi định dạng nên không tính là sửa test.
- **Trạng thái:** applied
- **Áp dụng:** PR #280: `docs/auto-run.md` bước 3, bước 4 và lời giao cho phiên con; thay đổi định dạng sau commit test là lệch nhỏ L5 (ADR-0032).
- **Kiểm chứng:** RUN-20261030-01: 11 phiên con chạy `./gradlew ktlintFormat` trước mỗi commit; không lần `scripts/check.sh` nào và không lần `pr-check` nào đỏ vì `ktlintCheck`.

### L-124. Nhánh của task sau tạo từ `origin/main` cũ vì phiên không `git fetch` sau khi merge task trước

- **Ngày, nguồn:** 2026-10-29, Photune (issue #273; L-003 của app; PR #22 của app)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** RUN-20261029-01 của app: nhánh của T-008 được tạo từ `origin/main` chưa có commit merge của T-007. Phiên phát hiện khi đối chiếu `git log origin/main` và tạo lại nhánh. Bước 1 của `docs/auto-run.md` chỉ ghi "từ nhánh chính mới nhất", không ghi cách lấy.
- **Bằng chứng:** issue #273; báo cáo RUN-20261029-01 của app, mục Bài học.
- **Hậu quả nếu không phát hiện:** task sau làm trên nền thiếu task trước; task phụ thuộc nhau thì build lỗi hay PR xung đột.
- **Đề xuất sửa:** bước 1 ghi `git fetch origin main` ngay trước khi tạo nhánh `claude/T-NNN-...` từ `origin/main`.
- **Trạng thái:** applied
- **Áp dụng:** PR #281: `docs/auto-run.md` bước 1, việc 0 cuối phiên và mục Tiếp tục trên nhánh cũ có `git fetch origin main`.
- **Kiểm chứng:** RUN-20261030-01: mười task liên tiếp tạo nhánh sau `git fetch origin main`, mỗi nhánh dựng trên commit merge của task trước (ví dụ `claude/T-067-...` từ `fbf8b05` của T-054); không task nào phải gộp lại vì thiếu commit của phiên (hai lần `--update` là do PR của người, L-129).

### L-125. `release.sh` chỉ chạy thật vào đêm phát hành, test của nó không đọc file build thật, nên hai bên lệch mà không ai biết

- **Ngày, nguồn:** 2026-10-29, Photune (issue #274; commit 1e98ea3 của app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** test của `scripts/release.sh` dùng fixture, không dùng `app/build.gradle.kts` thật. Script truyền thêm thuộc tính Gradle hay file build đổi tên thuộc tính đọc thì lỗi chỉ lộ ra lúc phát hành, hoặc bundle build xong mà thiếu giá trị. L-119 và L-120 là hai lần gần đây phần kiểm phát hành và file build lệch nhau.
- **Bằng chứng:** issue #274; L-119, L-120.
- **Đề xuất sửa:** `release.sh --check-only` chạy mọi bước kiểm rồi dừng trước bước hỏi mật khẩu và build; test hợp đồng: mọi `ORG_GRADLE_PROJECT_<tên>` mà `release.sh` xuất ra được `app/build.gradle.kts` đọc; test chạy `release.sh --check-only` thật với `app/build.gradle.kts` thật. Phủ mã quảng cáo, `assetsBaseUrl`, khóa AppsFlyer. `release.sh` là file AI không được sửa (ADR-0009): chỉ làm trong phiên có người theo dõi, khi người phụ trách phát hành cho phép; phiên tự chạy không làm (anh Nhật cho phép trong phiên 2026-10-29).
- **Trạng thái:** applied
- **Áp dụng:** PR #282: `scripts/release.sh --check-only` (anh Nhật cho phép sửa trong phiên 2026-10-29); test hợp đồng hai chiều và test chạy `release.sh --check-only` thật trong `scripts/tests/run.sh`; `docs/ops/release.md` mục 2.3.

### L-126. Các lịch định kỳ tốn phút Actions cả khi không có phiên nào

- **Ngày, nguồn:** 2026-10-29, Photune (issue #275)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** org dùng gói GitHub Free (2.000 phút mỗi tháng cho mọi repo private, mỗi job tính tối thiểu 1 phút). `resume-run.yml` mỗi giờ (khoảng 720 phút mỗi tháng), `run-watchdog.yml` mỗi 30 phút (khoảng 1.440 phút mỗi tháng) và `env-canary.yml` hằng ngày chạy cả khi không có phiên. Dev của app phải tắt tay `resume-run` và `env-canary` bằng `gh workflow disable`.
- **Bằng chứng:** issue #275.
- **Đề xuất sửa:** trước mắt, một PR tài liệu riêng: `docs/ops/staging-and-runs.md` ghi số phút mỗi tháng của từng lịch, cách tắt và khi nào bật lại. Sau đó, workflow định kỳ không cấp runner khi không có danh sách chạy `running` (T-051, cần plan rủi ro cao vì sửa `.github/`).
- **Trạng thái:** applied
- **Áp dụng:** PR #283 (phần tài liệu): `docs/ops/staging-and-runs.md` mục 2.8. Phần workflow: T-051, PR #291: `resume-run.yml` và `run-watchdog.yml` tự tắt khi không có phiên hay danh sách chờ; `status-page.yml`, `start-run.yml` và phiên bật lại (`scripts/schedule-idle.py`). `env-canary.yml` giữ lịch hằng ngày.

### L-127. Bộ công cụ build AGP 9.1, Gradle 9.3.1, compileSdk 37 đã chạy được ở một app

- **Ngày, nguồn:** 2026-10-29, Photune (issue #276; ADR 1004 của app; CI từ PR #9 tới PR #25 của app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** dự án mẫu ở AGP 8.13.0, Gradle 8.14.3, compileSdk 36. Photune build và test bằng AGP 9.1.0 (Kotlin dựng sẵn), Gradle 9.3.1, Kotlin 2.2.10, compileSdk 37, KSP 2.3.12, Hilt 2.59.2, Robolectric 4.17, Roborazzi 1.77.0, ktlint-gradle 14.2.0; `pr-check`, `main-guard`, `staging` xanh qua 2 phiên và khoảng 15 PR. Kotlin giữ 2.2.10 như mẫu, nhưng plugin `kotlin-android` bỏ vì AGP 9 có Kotlin dựng sẵn; KSP 2.3.2 thì cần `android.disallowKotlinSourceSets=false`; build-tools 36.0.0 vẫn là mặc định của AGP 9.1; Navigation Compose 2.9.8. Hai chỗ script phải sửa: gói nền tên `platforms;android-37.0` trong `cloud-setup.sh`; `cloud-env-check.sh` đọc `compileSdk` từ đúng một dòng của `libs.versions.toml`. App chưa phát hành bản production nào.
- **Bằng chứng:** issue #276.
- **Đề xuất sửa:** ticket nâng bộ công cụ build của dự án mẫu, plan rủi ro cao duyệt chéo (`build-logic/`, `gradle/`); làm lại phần cấu hình Kotlin của `build-logic` theo AGP 9.
- **Trạng thái:** accepted
- **Áp dụng:** T-052, tách thành T-079 (môi trường), T-080 (bộ build AGP 9), T-081 (Robolectric, Roborazzi, ảnh chuẩn).

### L-128. Phiên chạy `merge-if-green.sh` từ nhánh báo cáo cũ, chờ 30 phút một kiểm tra đã bỏ

- **Ngày, nguồn:** 2026-10-30, RUN-20261030-01 (T-058, PR #327)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** thư mục chính của phiên ở trên nhánh báo cáo, gộp nhánh chính lần cuối lúc 04:51. #324 (T-075, ADR-0043) merge lúc 05:24, bỏ job `ui-test` khỏi `pr-check` và sửa `scripts/merge-if-green.sh` cho khớp. Phiên gọi `merge-if-green.sh 327 --update` bằng bản trên máy (bản trước #324), script chờ `ui-test` trên commit của PR tới hết 30 phút rồi trả mã 2. Cập nhật nhánh báo cáo theo nhánh chính rồi gọi lại thì merge ngay.
- **Bằng chứng:** dòng thời gian của RUN-20261030-01, 05:33 tới 06:04; `CHƯA XONG: chưa thấy kiểm tra 'ui-test' trên commit 3670c12`.
- **Đề xuất sửa:** `docs/auto-run.md` bước 9: trước mỗi lần gọi `merge-if-green.sh` (và `wait-run.sh`), `git fetch origin main` và chạy script từ bản của nhánh chính mới nhất (gộp nhánh chính vào nhánh báo cáo, hay `git show origin/main:scripts/merge-if-green.sh`). Hoặc `merge-if-green.sh` tự so mã băm của chính nó với bản trên `origin/main` và dừng với thông báo rõ khi lệch.
- **Trạng thái:** proposed

### L-129. Người merge PR vào nhánh chính khi phiên tự chạy đang `running`

- **Ngày, nguồn:** 2026-10-30, RUN-20261030-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** trong phiên, hai PR không phải của phiên được merge vào nhánh chính: #320 (T-080, AGP 9.1, Gradle 9.3.1, compileSdk 37) lúc 03:38 và #324 (T-075, emulator sau merge) lúc 05:24. Hai task (T-054, T-058) phải gộp nhánh chính và chạy lại CI một vòng; L-128 xảy ra vì #324; plan T-068 chạy thử trên bộ build cũ nên phiên phải so lại classpath trên nhánh mới. `docs/auto-run.md` mục 1, ý 4 nói người không merge khi phiên đang chạy, và `merge-if-green.sh` từ chối PR ngoài phiên khi danh sách đang `running`, nên hai PR này đã đi đường khác.
- **Bằng chứng:** `merged_at` của #320 (03:38:49Z) và #324 (05:24:08Z); phiên bắt đầu 03:19; dòng thời gian 04:08 và 05:33.
- **Đề xuất sửa:** người xem cách hai PR được merge (nút merge hay luật bảo vệ nhánh). Nếu muốn chặn cứng: luật bảo vệ nhánh chính chỉ cho merge qua một kiểm tra mà `merge-if-green.sh` đặt, hay một kiểm tra bắt buộc báo đỏ khi có danh sách `running` mà PR không phải của phiên.
- **Trạng thái:** proposed

### L-130. Daemon Gradle của phiên bản cũ còn chạy sau khi nhánh chính đổi phiên bản Gradle, daemon mới bị giết vì thiếu bộ nhớ

- **Ngày, nguồn:** 2026-10-30, RUN-20261030-01 (T-067)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `check.sh` đầu phiên chạy trên Gradle 8.14.3; #320 đổi sang 9.3.1 giữa phiên. Daemon 8.14.3 (khoảng 2,8 GB) và daemon Kotlin của nó (khoảng 2,5 GB) vẫn sống. Lần `check.sh` đầu của T-067 dừng với "Gradle build daemon disappeared unexpectedly" (máy 15 GB, không swap); chạy lại thì qua. Phiên dừng hai tiến trình cũ (bộ nhớ trống từ 5 lên 10 GB), các task sau không gặp lại.
- **Bằng chứng:** báo cáo phiên con T-067; `ps` lúc 04:42 (PID daemon 8.14.3 và daemon Kotlin 2.2.10).
- **Đề xuất sửa:** bảng tra lỗi thêm dòng cho "Gradle build daemon disappeared unexpectedly": chạy `./gradlew --stop` bằng wrapper của phiên bản cũ (hay dừng các daemon có phiên bản khác `gradle-wrapper.properties`), rồi chạy lại một lần, không tính vào 3 lần sửa. `scripts/check.sh` có thể in cảnh báo khi có daemon Gradle của phiên bản khác đang chạy.
- **Trạng thái:** proposed

### L-131. Plan dựng test qua điểm nối test của task trước mà không kiểm điểm nối đó mang đủ dữ liệu cho câu khẳng định

- **Ngày, nguồn:** 2026-10-30, RUN-20261030-01 (T-059, PR #335)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-059 bản 2 viết test `inListRow_failedThenRetriesAt5s_listRowMetrics` qua `nativeLoaderForTest` của T-054 với `fail(3)` và kiểm số liệu `LIST_ROW` theo `RecordingAdMetrics`. Điểm nối đó bỏ `NativeAdEvents`, nên không có số liệu nào tới `RecordingAdMetrics`. Phiên con viết test bỏ phần số liệu; rà soát độc lập xếp chặn (đột biến `LIST_ROW` thành `SINGLE_AD` không test nào bắt). Lần sửa thứ nhất dùng loader giả ném lỗi trong file test (đường `onFailed(0, null)`), thêm một vòng rà soát.
- **Bằng chứng:** nhật ký plan T-059; commit 424118f.
- **Đề xuất sửa:** `docs/CLAUDE.md`, Khi viết plan: test dùng điểm nối test (`...ForTest`) của task khác thì plan ghi điểm nối đó truyền gì, bỏ gì, và mỗi câu khẳng định đọc dữ liệu từ đâu; chạy thử câu khẳng định đó bằng một test tạm (như L-029) khi điểm nối đã có trên nhánh chính.
- **Trạng thái:** proposed

### L-132. Plan khởi tạo SDK sau cổng đồng ý chỉ xét lần mở đầu, bỏ sót lúc cổng đóng lại trước khi SDK chạy

- **Ngày, nguồn:** 2026-10-30, RUN-20261030-01 (T-068, dừng ở bước 6)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan T-068 bản 2 chốt "cổng đã mở" một lần ở `onAdmobReady` và ghi "Không có đường dừng". Rà soát độc lập của task chỉ ra MAX vẫn khởi tạo khi người dùng đã rút đồng ý (đổi `ads_mediation_order` sau khi rút; cổng đóng trong 35 giây chờ `sdkReady`), trái KB-31 và ADR-0037. Hai lần rà soát plan trước phiên không bắt được điểm này. Task dừng; plan bản 3 thêm kiểm cổng ngay trước `initialize`.
- **Bằng chứng:** nhật ký plan T-068; plan bản 3, mục 1 và câu hỏi 9.
- **Đề xuất sửa:** `docs/CLAUDE.md`, Khi viết plan, ý của L-072: với điều kiện "không chạy trước khi đồng ý", mục 4 liệt kê cả các đường tới lượt khởi tạo sau khi cổng đã mở rồi đóng lại (rút đồng ý, thay đổi từ xa, hết giờ chờ), và kiểm trạng thái cổng ngay trước lời gọi SDK, không chỉ lúc nhận tín hiệu đầu.
- **Trạng thái:** proposed

### L-133. Task có plan làm ngoài phiên tự chạy, trên nhánh `claude/T-*`, được merge giữa phiên tự chạy

- **Ngày, nguồn:** 2026-10-30, T-080 (PR #320) và phiên RUN-20261030-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** T-080 làm trong phiên có người theo dõi, trên nhánh `claude/T-080-bo-build-agp-9`, đúng quy ước tên nhánh task của `docs/CLAUDE.md`. Danh sách RUN-20261030-01 merge lúc 03:06 UTC (PR #319), phiên bắt đầu lúc 03:19 UTC, T-080 merge lúc 03:38:49 UTC. `merge-if-green.sh` cho mọi nhánh `claude/T-[0-9]*` merge khi có phiên đang chạy, vì coi là task của phiên; nó không đối chiếu mã task với danh sách chạy. Nhánh chính đổi bộ build giữa phiên: task 1 của phiên (T-054, PR #322) được viết và kiểm trên bộ cũ, rồi gặp bộ mới khi gộp main. Đường làm nhanh đã tránh lỗ này bằng tên `claude/quick-T-*` (`docs/CLAUDE.md`), nhưng task có plan làm ngoài phiên thì chưa có quy ước tên. L-129 ghi cùng sự cố từ phía phiên (hai PR, #320 và #324); bài học này ghi nguyên nhân trong script: script không chặn nhánh `claude/T-*`, nên cả #324 (`claude/T-075-...`) cũng đi qua `merge-if-green.sh`, không cần đường khác.
- **Bằng chứng:** `scripts/merge-if-green.sh`, khối kiểm phiên đang chạy (`"$HEAD_REF" != claude/T-[0-9]*`); nhật ký plan T-080, mục Lệch so với plan (`docs/features/quy-trinh/plans/T-080-bo-build-agp-9.md` trên nhánh `claude/log-T-080`, PR #323).
- **Đề xuất sửa:** cách nhẹ: `docs/CLAUDE.md` thêm quy ước nhánh cho task có plan làm ngoài phiên tự chạy, không bắt đầu bằng `claude/T-`, để script từ chối khi có phiên. `scripts/token-usage.py` chỉ gán token cho nhánh `claude/T-NNN` và `claude/fix-T-NNN` (`docs/ops/token-usage.md`, mục 1), nên tên mới cần thêm vào đó; `scripts/resume-check.py` và `scripts/run-watchdog.py` không cần đổi, vì nhánh làm ngoài phiên không có trong danh sách chạy nào. Cách chặt: `merge-if-green.sh` chỉ cho nhánh task merge khi mã task có trong danh sách của phiên đang chạy, như `scripts/resume-check.py` và `scripts/run-watchdog.py` đã đối chiếu nhánh task với ticket của danh sách; script là file được bảo vệ, cần plan rủi ro cao duyệt chéo. Anh Nhật chọn cách chặt ngày 2026-10-30, thành ticket T-083.
- **Trạng thái:** proposed

### L-136. Mẫu đường dẫn của lệnh xem ảnh chuẩn đã đổi không khớp tệp nào, nên lệnh luôn in rỗng

- **Ngày, nguồn:** 2026-10-30, dự án mẫu (rà soát độc lập lần chạy thử plan T-081; plan T-081 ở PR #332)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** L-114 đưa vào `CLAUDE.md`, mục Lệnh, và mẫu plan lệnh `git status --short -- '*/src/test/screenshots/'` để xem ảnh chuẩn nào đã đổi sau khi ghi lại. Mẫu có ký tự đại diện được so với cả đường dẫn của tệp, nên mẫu kết thúc bằng `/` không khớp tệp nào, kể cả ảnh sửa hay ảnh mới: lệnh luôn in rỗng. Lần chạy thử plan T-081 ghi lại 89 ảnh và một ảnh đổi, nhưng lệnh báo không ảnh nào đổi; rà soát độc lập phát hiện. Plan đã duyệt T-053, T-060, T-062 chờ danh sách 9, 2 và 2 ảnh, còn T-067 và plan T-080 cũng dùng lệnh này: trên máy, phép kiểm của chúng luôn ra rỗng. `pr-check` vẫn so ảnh, nhưng không thấy ảnh ngoài plan bị ghi lại.
- **Bằng chứng:** worktree thử của T-081 (git 2.39.5): `git status --short` có ` M core/designsystem/src/test/screenshots/DesignSystem_spacing_components_light.png`, lệnh của `CLAUDE.md` in rỗng, còn mẫu `'*/src/test/screenshots/*'` in đúng tệp đó. Thử lại trên một repo tạm có một ảnh sửa và một ảnh mới: mẫu cũ không in tệp nào, mẫu có `*` ở cuối in cả hai.
- **Đề xuất sửa:** `CLAUDE.md` và mẫu plan dùng `'*/src/test/screenshots/*'`; một test chạy đúng lệnh của hai tài liệu đó trên repo tạm. Plan đã duyệt không sửa (`docs/CLAUDE.md`, mục Những điều không được làm): báo người phụ trách trước khi T-053 và T-060 chạy.
- **Trạng thái:** applied
- **Áp dụng:** PR #333: `CLAUDE.md` mục Lệnh và `docs/templates/plan.md` mục 4 dùng `'*/src/test/screenshots/*'`, kèm câu nói vì sao phải có `*`. Test trong `scripts/tests/run.sh` lấy lệnh từ hai tài liệu và chạy trên repo tạm có một ảnh sửa và một ảnh mới; với mẫu cũ, test đỏ. Plan đã duyệt T-053, T-060, T-062, T-067 giữ nguyên; người phụ trách được báo ở PR #332.
