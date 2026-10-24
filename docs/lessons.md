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
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/issue-78-plan-checks`

### L-051. Điều hướng sau một hàm `suspend` chạy trên luồng nền, lỗi chỉ lộ ra ở `ui-test`

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-030 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `AppNavHost` gọi một hàm `suspend` đọc DataStore rồi `navController.navigate` trong `scope.launch`; coroutine được tiếp tục trên luồng nền và app dừng với "setCurrentState must be called on the main thread". Phần vỏ app nằm trong ngoại lệ viết test trước của plan, nên lỗi chỉ lộ ra ở `ui-test` trên emulator, vòng CI dài nhất.
- **Cải tiến:** `CLAUDE.md` bước 6: điều hướng sau lời gọi `suspend` chạy trên `Dispatchers.Main.immediate`. `docs/CLAUDE.md` mục Khi viết plan: plan đổi hàm `suspend` mà vỏ app gọi có test Robolectric dựng vỏ app, không để vào ngoại lệ.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-052. Giao bước 3 đến 5 cho phiên con, trong worktree riêng

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-031, L-034 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên chính giao bước 3 đến 5 của 5 task lớn (tới 80 file) cho phiên con với danh sách file, thứ tự commit, điều kiện dừng và mẫu báo cáo. Cả 5 task merge, CI xanh vòng đầu, không file nào ngoài plan; ngữ cảnh của phiên chính đủ cho cả phiên. Nhưng phiên con làm trong thư mục chính, nên phiên chính không chạy được `check.sh` hay chuẩn bị task sau song song, và hook dừng phiên của môi trường cloud báo "có thay đổi chưa commit" ba lần. Một lần phiên chính sửa file khi `check.sh` chạy nền trong cùng thư mục và phải chạy lại.
- **Cải tiến:** `docs/auto-run.md` mục 3: Giao bước 3 đến 5 cho phiên con, trong worktree riêng của nhánh task, nội dung giao việc tối thiểu, phiên chính kiểm danh sách file; không sửa file trong thư mục đang chạy `check.sh` nền.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-053. Nút chữ trên nền khác màu tự đặt màu chữ, đè màu thiết kế

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-032 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `TextButton` "Thử lại" trong `Snackbar` tự đặt màu chữ `primary` qua `ButtonDefaults.textButtonColors()`, đè `actionContentColor` mà `Snackbar` truyền xuống; độ tương phản giảm từ khoảng 3,9:1 xuống 2,4:1. Ảnh chuẩn ghi lại màu sai, không test nào đo màu chữ; chỉ rà soát độc lập thấy khi đo điểm ảnh.
- **Cải tiến:** `CLAUDE.md` bước 10, mẫu plan mục 4, `pre-run-review.md` mục 2.2: truyền `colors` rõ cho nút; test đo màu chữ trên ảnh vẽ ra.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-054. Hai plan cùng ghi Tạo một file mà task chạy trước tạo ra

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-033 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** hai plan trong cùng danh sách cùng ghi Tạo một biểu tượng, mỗi plan một kiểu. `plan-check.py` chỉ báo khi task sau bắt đầu (file đã có); phiên phải tự chọn và ghi lệch plan. `run-check.py` chỉ báo hai plan cùng sửa một file khi chúng không phụ thuộc nhau.
- **Cải tiến:** `scripts/run-check.py`: chú ý khi hai plan cùng ghi Tạo một file, kể cả khi phụ thuộc nhau; `docs/CLAUDE.md`: một plan tạo, plan kia ghi Sửa hoặc dùng lại; `pre-run-review.md` mục 1. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-055. Test kiểm giá trị mặc định qua ngay trên khung rỗng

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-035 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** ở 4 task lớn, một số test viết theo mục 4 của plan qua ngay trên khung rỗng của bước 3, vì khung trả đúng giá trị mặc định mà test mong đợi ("chưa lưu gì thì trả mặc định", "tệp hỏng thì trả `null`"). Phiên con tự viết chặt lại 19 test bằng một bước đối chứng (ghi rồi đọc lại, dữ liệu đúng cạnh dữ liệu sai), sau đó chúng thất bại đúng lý do.
- **Cải tiến:** `docs/auto-run.md` bước 3: bước đối chứng là cách mặc định; mẫu plan mục 4, `docs/CLAUDE.md`, `pre-run-review.md` mục 2.2.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-056. Bài học thêm ngoài phiên trùng số với bài học mà phiên thêm cùng lúc

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-042 của app, issue #86)
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** một PR cải tiến mở trong lúc phiên chạy thêm 6 bài học; phiên thêm 4 bài cùng số trong PR báo cáo. Sau khi phiên merge phải đổi số ở hai PR của app, một PR và một issue của dự án mẫu. Nếu hai bài học nằm ở hai chỗ khác nhau của sổ, git gộp được mà không xung đột và không công cụ nào báo.
- **Cải tiến:** `docs/auto-run.md` mục 6: gộp nhánh chính và đổi số trước khi merge PR thêm bài học; `scripts/status.py` báo lỗi khi một số dùng hai lần. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-057. Trang tiến độ của app đếm 0 bài học vì sổ viết khác mẫu

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-043 của app, issue #86)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** `status.py` chỉ nhận tiêu đề `### L-NNN.` và dòng trạng thái chỉ có trạng thái. Sổ của app viết `### L-NNN:` và `- **Trạng thái:** proposed. **Loại:** ...`, nên trang tiến độ ghi 0 bài trong khi sổ có 41, và không bài nào hiện ở mục chờ retro; cả 41 bài vẫn `proposed`.
- **Cải tiến:** `scripts/status.py` nhận cả hai dạng tiêu đề và đọc chữ trạng thái đầu tiên. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-058. Plan đổi cổng chất lượng bỏ sót một kiểm tra mà trường hợp mới phải đi qua

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-044 của app, issue #86)
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** plan cho `merge-if-green.sh` nhận bản revert đúng khi `main-broken` mở (L-049) ghi "rồi đi tiếp tới các kiểm tra còn lại"; kiểm tra "phiên đang chạy" ngay sau đó từ chối mọi nhánh ngoài `claude/T-*` và nhánh báo cáo, nên trong phiên bản revert vẫn bị từ chối. Plan đã qua duyệt chéo; lỗi chỉ thấy khi viết code.
- **Cải tiến:** `docs/CLAUDE.md` mục Khi viết plan: plan đổi `merge-if-green.sh` hay `main-guard.yml` liệt kê mọi kiểm tra hiện có, nói trường hợp mới qua hay bị chặn ở từng kiểm tra, mỗi kiểm tra một test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-bai-hoc-RUN-20261023-02`

### L-059. Thiết lập staging, file R2, DNS, Java, merge và trang tiến độ chỉ lộ lỗi giữa hay cuối phiên

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-002, L-011, L-012, L-016, L-017, L-018, L-022, L-027 của app, issue #78)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, ba phiên liền chỉ biết bí mật Firebase staging là file giả khi phát bản staging ở cuối phiên; một phiên phát được bản build nhưng đỏ vì chưa tạo nhóm người test `team`. File R2 mà màn hình loading tải chưa được đưa lên, lỗi chỉ lộ ở PR có code đầu tiên. Tên miền file tĩnh thuộc vùng có DNSSEC lỗi nên 1.1.1.1 và 8.8.8.8 không phân giải được, điện thoại không tải được file trong khi emulator trên GitHub vẫn được. Máy dev chỉ có Java 26, Gradle 8.14.3 dừng với lỗi chỉ ghi "26". `docs/STATUS.md` xung đột ở mọi PR tài liệu. `merge-if-green.sh` phải gọi lại nhiều lần khi nhánh chậm, một lần job ghi `in_progress` dù lần chạy đã xanh, và một commit được đẩy lên nhánh sau khi PR đã merge.
- **Cải tiến:** `scripts/setup-check.sh` kiểm file trong `docs/ops/required-assets.txt` (`curl -I`), DNS qua 1.1.1.1 và 8.8.8.8, và với `--staging` bí mật Firebase staging (không phải file giả, có app `.staging`) và nhóm người test qua API của App Distribution. `start-run.yml` chạy nó trước khi kích hoạt phiên; `env-canary.yml` chạy hằng ngày, đỏ thì mở issue `setup-broken`; đây cũng là bước cuối của `docs/ops/setup-new-app.md`. `scripts/check.sh` gọi `scripts/java-check.sh` trước Gradle (khoảng phiên bản đọc từ `build-logic` và phiên bản Gradle). `merge-if-green.sh` có `--update` (cập nhật nhánh qua API, chờ CI có giới hạn rồi merge, kiểm lại mọi điều kiện sau mỗi lần chờ), coi job còn ghi đang chạy mà lần chạy chứa nó đã `completed success` là xanh, in mã commit sẽ merge và cảnh báo commit mới đẩy. `docs/STATUS.md` chỉ đi vào PR báo cáo của phiên và PR riêng của trang; không dùng `merge=ours` vì merge trên GitHub không chạy driver tùy chỉnh. `plan-check.py` chỉ tìm nơi nhắc tới file trong các file git quản lý. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/issue-78-protected`

### L-060. Phiên con đẩy commit trước khi có kết quả `main-guard`, và nhận việc ngoài plan từ nội dung giao việc chung

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** quy trình, chung
- **Chuyện gì:** phiên con của T-007 đẩy commit test lúc 08:11 khi `main-guard` của lần merge T-018 chưa xong (08:11:18), trái auto-run.md bước 9. Từ T-008, phiên chính tạo tệp cổng `gate-T-NNN` khi `main-guard` xanh và không có `main-broken`; phiên con làm cục bộ và chỉ đẩy khi có tệp, 10 task sau không vi phạm lần nào. Ở T-014, nội dung giao việc chung của phiên chính bảo phiên con thêm dòng vào `docs/features/ads/design/review/summary.md`, trái mục 3.1 của plan, làm một file ra ngoài danh sách; phiên chính phát hiện khi đối chiếu danh sách file và bỏ lại.
- **Cải tiến (đề xuất, chưa làm):** `docs/auto-run.md` mục Giao bước 3 đến 5: thêm tệp cổng vào nội dung giao việc tối thiểu; nội dung giao việc chung không thêm việc về file cụ thể, mọi việc về file lấy từ plan.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-061. Rà soát độc lập bắt lỗi dựng lại activity và đổi trạng thái giữa chừng mà test viết trước bỏ sót

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** 4 trên 12 task có lỗi thật ở mức nên sửa do rà soát độc lập tìm ra, test viết trước không bắt: T-017 popup mất trạng thái khi activity dựng lại (khung hình đầu báo màn hình `null`; ca KB-3 thì popup biến mất), T-011 quảng cáo đã tải còn giữ khi vị trí bị tắt giữa chừng, T-010 nút CTA lệch thiết kế 8 dp, T-012 test thiếu kiểm đăng ký tài sản sau khi thu gọn. Plan không nêu trường hợp dựng lại activity hay cờ đổi giữa chừng.
- **Cải tiến (đề xuất, chưa làm):** `docs/CLAUDE.md` mục Khi viết plan: trạng thái giữ ở vỏ app (`MainActivity`, `AppNavHost`) có test dựng lại activity (chuỗi khung hình đầu chưa có back stack, hay `ActivityScenario.recreate()`); tài nguyên giữ theo cờ hay khóa Remote Config có test khi cờ tắt giữa chừng.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-062. Test quét mã nguồn theo chuỗi con hỏng vì task chạy trước trong cùng phiên

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** plan, chung
- **Chuyện gì:** `AdGallerySourceSetTest` của T-014 kiểm không file `.kt`, `.xml` nào ngoài `src/staging` chứa `AdGallery`. T-017, merge trước trong cùng phiên, thêm một ví dụ KDoc `com.x.AdGalleryDestination/{id}` vào `app/src/main`. Plan T-014 viết khi T-017 chưa merge nên không thấy; phiên con nới test và task dừng ở bước 6. PR #114 sửa chú thích.
- **Cải tiến (đề xuất, chưa làm):** `docs/CLAUDE.md` mục Khi viết plan: test quét toàn bộ mã nguồn ghi ở mục 6 các plan cùng danh sách chạy có thể thêm chuỗi đó, hoặc quét theo ký hiệu (import, tên lớp đầy đủ, khai báo manifest) thay vì chuỗi con. `scripts/run-check.py` có thể báo chú ý khi một plan có test quét mã nguồn mà plan chạy trước sửa cùng source set.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-063. Plan lấy code từ app bằng `gh pr diff`, môi trường cloud chặn

- **Ngày, nguồn:** 2026-10-23, RUN-20261023-01
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** plan T-018, T-016, T-017 ghi đọc code của app camera-pose bằng `gh pr diff <n> -R VGL-Studio/camera-pose`. Trong môi trường cloud, `gh pr` bị chặn và `gh api` tới repo chưa gắn vào phiên trả 403. Cách chạy được: gắn repo chỉ đọc bằng `add_repo`, rồi `gh api repos/<owner>/<repo>/pulls/<n> -H "Accept: application/vnd.github.v3.diff"` hoặc `contents/<path>?ref=<sha>`. Phiên con thử clone repo đó thì bị từ chối; đọc qua API thì được.
- **Cải tiến (đề xuất, chưa làm):** `docs/auto-run.md` bước 7 (danh sách lệnh REST) thêm cách đọc PR và file của repo khác; `docs/ops/from-app.md` ghi plan lấy code từ app dùng `gh api`, không dùng `gh pr diff`.
- **Trạng thái:** proposed
- **Áp dụng:**

### L-064. GitHub không chạy job (hết hạn mức) hay không chạy CI cho PR xung đột, phiên hiểu thành CI đỏ hoặc chờ 40 phút

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-046, L-048 của app, issue #106)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** tổ chức hết hạn mức GitHub Actions: mọi job đỏ mà 0 bước, `wait-run.sh` chỉ báo `ĐỎ`; phiên sẽ sửa code hay revert vô ích. PR xung đột với nhánh chính không có lần chạy `pull_request` nào, `wait-run.sh --pr` chờ trọn 40 phút.
- **Cải tiến:** `wait-run.sh` báo `HẠ TẦNG` (mã 4) khi mọi job đỏ đều chưa chạy bước nào, kèm chú thích của GitHub; báo `XUNG ĐỘT` (mã 5) khi PR `dirty` mà chưa có lần chạy. `auto-run.md`: mã 4 thì kết thúc phiên, giữ `running` để `resume-run.yml` tiếp tục, rồi chạy lại các lần chạy bị bỏ; mã 5 thì gộp nhánh chính. Có test.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-wait-run-ha-tang`

### L-065. Log tải file thất bại không có tiền tố `VGL.` và không cho biết nguyên nhân của lỗi bọc

- **Ngày, nguồn:** 2026-10-23, app camera-pose (plan T-032, PR #51 của app; bài học L-027 của app; issue #105)
- **Loại, phạm vi:** quy tắc code, chung
- **Chuyện gì:** ở app, máy thật không tải được catalog vì tên miền file tĩnh không phân giải được. Dòng log `Assets: Could not download <đường dẫn>` không nói lý do; phải kiểm DNS bằng tay. Ở dự án mẫu, câu log đã có lý do và lớp lỗi ngoài cùng (PR #97), nhưng `:core:assets` vẫn gọi thẳng `android.util.Log` với thẻ `Assets`: lọc `grep ' VGL\.'` không thấy dòng này, Crashlytics không giữ nó, unit test không kiểm được. Lỗi bọc (`IOException` có nguyên nhân `UnknownHostException`) chỉ hiện `IOException`, lỗi HTTP không có mã.
- **Cải tiến:** `:core:assets` phụ thuộc `:core:logging`; `AssetsModule` nhận `AppLogger` và ghi `warn` thẻ `Assets` (thành `VGL.Assets`, ADR-0019) qua `downloadFailureLogger`. Câu log ghi lý do, chuỗi lớp lỗi theo nguyên nhân (tối đa 5) và `status=<mã>` của lỗi HTTP; không ghi thông điệp lỗi, vì nó có thể chứa địa chỉ đầy đủ. Câu cũ cho một lỗi không bọc giữ nguyên. Có unit test với `RecordingAppLogger`.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-assets-applogger`

### L-066. Trang tiến độ vẫn chậm và tốn một PR riêng; PR xung đột không có lần chạy CI

- **Ngày, nguồn:** 2026-10-23, app camera-pose (bài học L-047, L-048, L-016 của app, issue #104)
- **Loại, phạm vi:** môi trường, chung
- **Chuyện gì:** ở app, PR nào đổi trạng thái cũng commit `docs/STATUS.md`, nên `merge-if-green.sh --update` lần nào cũng nhận `422 merge conflict` (PR #51, #52 của app). GitHub không chạy workflow `pull_request` cho PR đang xung đột: PR #56 của app không có lần chạy `pr-check` nào, `wait-run.sh --pr` chờ trọn 40 phút. Cách của L-059 (chỉ PR báo cáo và PR trang riêng mang trang) bớt xung đột, nhưng trang trên nhánh chính chậm tới khi có người mở PR trang, và mỗi lần xem trang mới tốn một PR cùng một lần CI.
- **Cải tiến:** `docs/STATUS.md` ra khỏi git (`.gitignore`). Workflow `status-page.yml` chạy `scripts/status.py` sau mỗi lần đẩy lên nhánh chính có đổi `docs/**` hay `scripts/status.py`, rồi đẩy đè nhánh `status` (nhánh chính cộng một commit chứa trang); không bao giờ đẩy lên nhánh chính. `merge-if-green.sh` báo chú ý khi PR nào có trang, kể cả PR báo cáo, và thôi nhắc mở PR trang. Liên kết tới trang là liên kết tương đối, không ghi tên repo. Có test. Ở app: lần chạy `status-page` 39936153612 xanh, các PR sau không còn xung đột vì trang.
- **Trạng thái:** proposed
- **Áp dụng:** nhánh `claude/from-camera-pose-status-branch`

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
- **Trạng thái:** proposed

