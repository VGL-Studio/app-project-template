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
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-app-process`

### L-033. Chạm theo tọa độ cũ trúng quảng cáo vừa hiện lên

- **Ngày, nguồn:** 2026-10-20, khảo sát `com.camera.filter.pose.guide.ideas`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** agent chạm nút Back theo tọa độ đọc từ trước, đúng lúc một quảng cáo native toàn màn hình hiện lên sau interstitial; hai lần mở trang Play của nhà quảng cáo. Banner thu gọn còn tự bung lại khoảng 20 giây sau khi thu.
- **Cải tiến:** mọi lệnh chạm của `ax.sh` đọc lại cây giao diện ngay trước khi chạm; `scripts/app-explore/guard.py` từ chối khi phần tử nhận cú chạm thuộc quảng cáo hay đang ở quảng cáo toàn màn hình, và `ax.sh` tự back, ghi `incidents.log` khi lỡ rời sang Play hay trình duyệt. Có test trong `scripts/tests/run.sh`.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-app-process`

### L-034. Chạm liên tục trong vòng lặp mở màn hình mua của Play

- **Ngày, nguồn:** 2026-10-20, khảo sát `com.vector123.posecamera`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** agent bấm nút chụp nhiều lần liên tiếp mà không xem lại màn hình. Hết lượt miễn phí, màn hình trả phí bật lên và lần bấm sau trúng "UNLOCK FOREVER", mở màn hình mua của Play hai lần. Công cụ tự back cả hai lần, không có bước mua nào được xác nhận.
- **Cải tiến:** `guard.py` từ chối nút mua, đăng ký, dùng thử, nút có giá, "Tiếp tục" trên màn hình có giá, và mọi chạm trên màn hình thanh toán của Play. `docs/ops/clone-study.md` mục 4 cấm chạm trong vòng lặp mà không xem lại màn hình. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-app-process`

### L-035. Khảo sát khi máy không có mạng thì thiếu quảng cáo và nội dung

- **Ngày, nguồn:** 2026-10-21, khảo sát `com.cam.simplepose.pose.camera.photo.guide`
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** Wi-Fi bật nhưng máy không có mạng suốt phiên. Agent không thấy quảng cáo, hộp thoại đồng ý, và một nửa danh mục tư thế trống; phải khảo sát lại phần đó.
- **Cải tiến:** `ax.sh check` kiểm tra mạng và thoát mã 8 khi không có; `docs/ops/clone-study.md` mục 1 ghi bước kết nối mạng.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-app-process`

### L-036. Báo cáo khảo sát app mẫu trích nguyên mã AdMob thật của app mẫu

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose (chép tài liệu khảo sát vào `docs/research/`)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** báo cáo PoseMate trích một dòng log có mã quảng cáo AdMob thật của app mẫu. Agent đọc APK được dặn không chép mã quảng cáo, agent khảo sát trên máy thì không. Phiên app phát hiện và che tay trước khi commit.
- **Cải tiến:** `scripts/secret-scan.py` quét file của repo (mã AdMob ngoài mã thử của Google, khóa API, khóa riêng, token GitHub) và chạy trong `scripts/tests/run.sh`, nên cổng chất lượng chặn. `clone-study.md` mục 4, mẫu báo cáo và `docs/CLAUDE.md` cấm chép các giá trị đó, trích log thì che.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-followups`

### L-037. Đường dẫn trong bản đề xuất gãy khi chép tài liệu khảo sát vào repo

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** `clone-study.md` để báo cáo trong `reports/` cạnh `proposal.md`, còn `docs/README.md` quy định `research/<package>.md` đặt phẳng. Phiên app phải sửa link trong bản đề xuất đã duyệt.
- **Cải tiến:** thư mục làm việc có `research/` cùng cấu trúc với `docs/research/`; chép nguyên, không sửa link. Mẫu đề xuất ghi link tới file bên cạnh và ảnh nằm ngoài repo.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-followups`

### L-038. App mới còn bài học của dự án mẫu trên trang tiến độ

- **Ngày, nguồn:** 2026-10-21, phiên khởi tạo app camera-pose
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `scripts/new-app.sh` xóa bài học của dự án mẫu khỏi `docs/lessons.md` nhưng không sinh lại `docs/STATUS.md`, nên trang của app mới còn L-032 đến L-035.
- **Cải tiến:** `new-app.sh` chạy `status.py` và `foundation.py` trước commit đầu tiên; test kiểm trang của app mới khớp với tài liệu.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/clone-followups`

### L-039. Test của script hỏng trong app mới: test chỉ dành cho dự án mẫu, và test cần remote origin

- **Ngày, nguồn:** 2026-10-21, app camera-pose (PR #1, #2 của repo app)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** trong app, `scripts/tests/run.sh` chạy test của `new-app.sh` mà app không có script đó, nên test "new-app.sh succeeds" hỏng và CI của mọi PR app đỏ. Trước khi đẩy repo lên GitHub, thêm 14 test của `merge-if-green`, `wait-run`, `env-canary` hỏng vì các script đọc tên repo từ remote `origin`.
- **Cải tiến:** test của `new-app.sh` chỉ chạy khi có script (repo dự án mẫu); bộ test đặt một remote `origin` giả cho mọi lệnh git qua `GIT_CONFIG_*`, không phụ thuộc remote thật. Không sửa script được bảo vệ.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/app-test-template-only`

### L-040. Commit chỉ có tài liệu bỏ qua CI, nên test kiểm tài liệu không chặn được lỗi

- **Ngày, nguồn:** 2026-10-21, PR #68 và #69
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-008 (PR #68, chỉ có tài liệu) có mã AdMob giả toàn số 0. Bộ quét mã thật (PR #65) coi là mã thật, nhưng CI bỏ qua commit chỉ có tài liệu nên `main` có test của script đỏ mà không ai biết; phát hiện khi chạy test trên máy ở PR #69.
- **Cải tiến:** bộ quét cho qua mã toàn số 0 (PR #69). Đề xuất: CI chạy `scripts/tests/run.sh` cả với commit chỉ có tài liệu (nhanh, không cần Gradle), vì nhiều test của nó kiểm tài liệu (`foundation.py`, `status.py`, quét mã thật). Đổi `.github/workflows/` là việc rủi ro cao, cần người quyết.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-041. Plan ghi sẵn phương án dự phòng đã duyệt, để phiên chuyển sang thay vì dừng task

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-039 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ba lần dừng task đầu tiên ở app đều cùng một dạng: kỹ thuật của plan không chạy (hết thời gian chờ khi chụp ảnh trong Robolectric, quy tắc của `check.sh`, thuộc tính trợ năng không lan xuống node con), và cách thay đã rõ ngay khi gặp lỗi. Phiên phải dừng vì cách thay chưa được duyệt; mỗi lần mất một phiên và một vòng duyệt.
- **Cải tiến:**
  - mẫu plan có mục 3.4 Phương án dự phòng: điều kiện chuyển, cách làm thay; file của phương án dự phòng ghi "(dự phòng)" ở mục 3.2;
  - `risk-scoring.md` mục 4: chuyển sang phương án dự phòng đã duyệt không phải dừng; chỉ chuyển một lần, không vượt các điều kiện dừng khác;
  - `auto-run.md` bước 4 và `docs/CLAUDE.md` mục Khi viết plan.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

### L-042. Kỹ thuật lần đầu dùng phải chạy thử bằng `scripts/check.sh` trước khi duyệt plan

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-001, L-010 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ở app, bốn lần dừng có cùng một gốc: kỹ thuật lần đầu dùng chưa được chạy thử, hoặc chạy thử bằng lệnh khác với lệnh phiên dùng (chỉ `./gradlew :module:testDebugUnitTest`, trong khi phiên dừng ở quy tắc của `scripts/check.sh`). Plan chạy thử bằng `scripts/check.sh` trước khi duyệt thì không dừng nữa.
- **Cải tiến:** mẫu plan có mục 4.1 Chạy thử; `docs/CLAUDE.md` mục Khi viết plan; `scripts/plan-check.py` quy tắc 7: plan `draft` có tiêu chí E từ 2 mà mục 4.1 không ghi lần chạy thử bằng `scripts/check.sh` là lỗi, plan `approved` chỉ chú ý (để phiên không bỏ qua plan đã duyệt trước quy tắc này). Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

### L-043. Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-028 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên merge một task, kiểm `main-broken` ngay (chưa có issue) rồi làm task sau. `main-guard` của commit merge xong 8 phút sau và mở `main-broken`; phiên chỉ thấy khi kiểm lại sau khi task sau đã xong bước 7, phí khoảng 45 phút và 22M token cho một task không merge được.
- **Cải tiến:** `auto-run.md` bước 9: sau khi merge, chờ `main-guard` của commit merge bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ merge>` ở chế độ nền; phải có kết quả rồi kiểm lại `main-broken` trước khi đẩy commit đầu tiên của task sau.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

### L-044. Phiên mở bằng `claude --cloud` không được `resume-run.yml` tiếp tục khi hết hạn mức

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-008, L-020 của app, PR VGL-Studio/camera-pose#39, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** app mở nhiều phiên bằng `claude --cloud` từ terminal để dùng cloud session credits (routine không dùng được credit). `resume-run.yml` chỉ kích hoạt lại routine, nên phiên loại này dừng hẳn khi hết hạn mức, người phải mở lại.
- **Cải tiến:** `auto-run.md` mục 1: ưu tiên `start-run.yml` hoặc Run now của routine; chỉ mở bằng `claude --cloud` khi có lý do (ví dụ dùng credit) và ghi lý do vào mục Kiểm tra đầu phiên.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

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
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

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
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-phien-tu-khac-phuc`

### L-047. Chạy lại `main-guard` trên đúng commit bị đổ lỗi, xanh, nhưng issue `main-broken` không tự đóng

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-036 của app, issue #79)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** dev chạy lại `main-guard` trên commit bị đổ lỗi và lần chạy xanh. Bước đóng issue chỉ đóng khi `compare/<commit bị đổ lỗi>...<commit xanh>` trả `ahead`; cùng một commit thì trả `identical`, nên issue vẫn mở và dev phải tự đóng trước khi phiên sau chạy được.
- **Cải tiến (đề xuất, chưa làm):** `main-guard.yml` đóng issue khi `status` là `ahead` hoặc `identical`; ADR-0010 mục 2 ghi thêm trường hợp này. Chạm `.github/` nên cần plan rủi ro cao có duyệt chéo.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-048. Một test chập chờn trên `main-guard` kết thúc cả phiên

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-029, L-037 của app, issue #79)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** một screenshot test chụp trạng thái có vòng chờ chạy mãi thất bại một lần trên `main-guard`, xanh ở PR, ở cục bộ và khi chạy lại. Một lần đỏ đó mở `main-broken`, phiên kết thúc khi mới xong 2 trên 7 task. Nguyên nhân, đo ở app: `captureRoboImage(path) { ... }` chờ luồng chính rảnh mà không có giới hạn; vòng chờ chạy mãi nên Robolectric vẽ khung hình khoảng 4 phút tới khi máy test hết bộ nhớ rồi mới chụp. Với bộ nhớ test mặc định, 5 lần qua; với 1g và 384m, ảnh lệch và test thất bại. Chụp bằng `runComposeUiTest` với đồng hồ dừng ở mốc cố định: 7 trên 7 lần qua với ba cỡ bộ nhớ, test chạy 0,25 giây.
- **Cải tiến (đề xuất, chưa làm):** `main-guard.yml`: khi bước kiểm tra đỏ chỉ vì test thất bại, chạy lại riêng các test đó một lần trên cùng commit; xanh thì không mở `main-broken` mà mở hoặc bình luận vào issue `flaky` ghi tên test, để sửa test trong một ticket thường; vẫn đỏ thì làm như hiện nay. ADR-0010 mục 2 thêm quy tắc này; cần plan rủi ro cao có duyệt chéo. Ngoài ra, `pre-run-review.md` đã coi test có nguy cơ chập chờn (hiệu ứng chạy mãi, đồng hồ thật, mạng thật) là mức chặn.
- **Trạng thái:** proposed
- **Áp dụng:** `CLAUDE.md` bước 10 (cách chụp trạng thái có hiệu ứng chạy mãi), nhánh `claude/from-camera-pose-phien-tu-khac-phuc`. Phần `main-guard.yml` chưa làm.

### L-049. Phiên không tự đưa nhánh chính về trạng thái xanh được, kể cả bằng một bản revert đúng

- **Ngày, nguồn:** 2026-10-22, app camera-pose (bài học L-038 của app, issue #79)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** khi `main-broken` mở, `merge-if-green.sh` từ chối mọi PR, kể cả PR từ nhánh `revert/<commit>` mà `main-guard` dựng sẵn; chỉ người đóng issue mới mở lại được. Lý do của ADR-0010 là đúng (tên nhánh không phải bằng chứng), nhưng hệ quả là một lần merge hỏng vào ban đêm làm mọi task còn lại chờ tới sáng.
- **Cải tiến (đề xuất, chưa làm):** ngoại lệ kiểm bằng nội dung thay vì tên: khi `main-broken` mở, `merge-if-green.sh` chỉ merge PR có đúng một commit mà cây file trùng khớp với kết quả `git revert <commit bị đổ lỗi>` do chính script dựng trên nhánh chính mới nhất, và CI xanh. Phiên được merge bản revert của commit do chính phiên merge, chuyển task đó sang `stopped`, rồi làm tiếp các task không phụ thuộc nó. Issue vẫn chỉ tự đóng bởi `main-guard`; AI vẫn không đóng hay sửa issue. Cần ADR sửa ADR-0010 và plan rủi ro cao có duyệt chéo.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-050. Quy tắc của `check.sh`, giới hạn mạng của phiên cloud và trạng thái ticket chỉ lộ ra khi phiên đã chạy

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-005, L-013, L-019 của app, issue #78)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** ở app, ba chỗ hở cùng một kiểu: điều plan phải biết chỉ được phát hiện khi phiên đã chạy hoặc khi người đối chiếu bằng tay. Quy tắc "mọi `captureRoboImage` ghi vào `src/test/screenshots/`" chỉ nằm trong `check.sh` và một câu của `CLAUDE.md`, người viết plan và phiên đều bỏ sót (T-007). Plan T-010 yêu cầu `curl` file trên R2 trước bước 3, nhưng proxy của `claude --cloud` trả 403 cho máy chủ R2. Ticket chuyển `approved` khi plan còn `draft`, plan được duyệt mà ticket còn `planned`.
- **Cải tiến:** `docs/auto-run.md` bước 5 có bảng những gì `scripts/check.sh` từ chối; `scripts/plan-check.py` quy tắc 9 dò trong plan lệnh `captureRoboImage` sai đường dẫn, ảnh chuẩn mới thiếu ở mục 3.2, ảnh nằm ngoài `src/test/screenshots/` (lỗi với plan `draft`, chú ý với plan `approved`). `docs/CLAUDE.md` mục Khi viết plan: điều kiện về file R2 kiểm bằng workflow trên GitHub hoặc "dev kiểm trước phiên", không bằng `curl` từ phiên. `scripts/status.py --sync` chuyển ticket theo plan `approved` hay `stopped`; `plan-check.py` quy tắc 10 báo chú ý khi lệch; `docs/CLAUDE.md` mục Trang tiến độ. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/issue-78-plan-checks`
