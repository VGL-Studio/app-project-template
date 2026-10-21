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

