---
ticket: T-104
title: Phiên tự bắt đầu khi danh sách chạy approved có start auto lên nhánh chính
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn 7, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 19.4M        # cỡ M, số gợi ý 19.4M của scripts/token-usage.py estimate (2026-11-12, trung vị 30 task M); không chỉnh
blocking_points: [ci]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:
cross_reviewer:
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-104: PHIÊN TỰ BẮT ĐẦU KHI DANH SÁCH CHẠY APPROVED CÓ START AUTO LÊN NHÁNH CHÍNH

## 1. Tóm tắt

`start-run.yml` chạy thêm khi có push lên nhánh chính đổi `docs/runs/RUN-*.md`. Một script mới, `scripts/auto-start-check.py`, chọn danh sách mà chính lần push đó vừa chuyển sang `approved`, có `start: auto` và `open_with: routine`. Cùng script chặn mọi lần mở phiên (cả lần bấm tay) khi danh sách đã có nhánh báo cáo, khi một phiên khác đang chạy, hay khi `start-run` vừa (hay đang) kích hoạt routine mà phiên đó chưa có nhánh báo cáo; kiểm tra chạy hai lần, ở job đầu và ngay trước lời gọi API trong job nằm trong hàng chờ `start-run`, để hai lần chạy sát nhau không cùng qua. Có danh sách thì workflow đi qua đúng các kiểm tra hiện có rồi kích hoạt routine; không có thì dừng xanh, không gọi API. Kiểm chứng bằng test của script với `gh` và git giả, test của `run-check.py`, và test cấu trúc của workflow trong `scripts/tests/run.sh`.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn: `features/quy-trinh/requests/2026-11-12-giam-thoi-gian-cho.md`, việc 3; ticket `T-104`.

| Điều cần đạt | Cách hiểu của AI |
| --- | --- |
| Danh sách `approved` lên nhánh chính thì phiên tự bắt đầu | "Lên nhánh chính" là lần push mà file danh sách ở commit sau có `status: approved` còn ở commit trước thì chưa có file hoặc khác `approved`. Commit trước là `github.event.before`; không có trong kho hay toàn số 0 thì dùng cha thứ nhất của commit sau; không có cha thì không chọn gì. Chỉ dựa vào chính lần push đó, nên lần push sau đổi cùng file (PR báo cáo, sửa chữ) không kích hoạt lại, và danh sách cũ mà GitHub đã không phát push lúc merge không bị mở ở một lần push về sau |
| Có ô chọn để giữ cách bấm tay | Trường `start:` trong frontmatter. `auto`: tự bắt đầu; `manual` hay trống: như cũ. Danh sách cũ không có trường nên không đổi hành vi. Mẫu mới ghi `auto` |
| Không mở phiên sai, không mở hai phiên | Chỉ `open_with: routine` (workflow chỉ kích hoạt được routine). Cả lần chạy do push lẫn lần bấm tay đều bị từ chối khi: danh sách đã có nhánh `claude/<RUN>-report`; một danh sách khác đang `running` trên nhánh báo cáo của nó; hay một job `fire` khác đang chạy, đang chờ, hoặc xong xanh trong 90 phút qua mà chưa có nhánh báo cáo `running` nào (phiên vừa kích hoạt chưa kịp đẩy nhánh báo cáo, hay đang chờ hạn mức). Lần bấm tay có ô `force` để bỏ qua điều cuối, và để mở lại phiên của chính danh sách đó khi nhánh báo cáo của nó đang `running` (như hôm nay người mở lại phiên mà `resume-run` không tiếp tục). Lần chạy bị từ chối thì đỏ, có lý do |
| Vẫn qua các kiểm tra của `start-run` | Giữ nguyên bước kiểm danh sách, `main-broken`, `env-broken`, `main-guard-status.sh`, `setup-check.sh --staging`, đóng `setup-broken` cũ, bật lịch. Chỉ thêm: với lần chạy do push, `main-guard` đang chạy (mã 2) thì chờ có giới hạn rồi kiểm lại, vì merge danh sách thường ngay sau merge code |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan: ADR-0009 và ADR-0016 (cả hai ghi `start-run.yml` chạy tay; xem mục 8, câu 1), ADR-0048 (`proposed`, duyệt cả đợt một lần; hai việc bổ sung nhau nhưng không phụ thuộc), ADR-0044.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/auto-start-check.py` (mới).** Theo mẫu `scripts/resume-check.py`: chỉ dùng `gh api` (REST) và `git`, `GITHUB_REPOSITORY` hay remote `origin` cho tên repo, thông báo tiếng Việt ra stderr. Đọc frontmatter như `resume-check.py` (bỏ phần chú thích sau `#`, nên `start: auto   # ...` đọc ra `auto`).

- Cách dùng:
  - `scripts/auto-start-check.py push --before <sha> --after <sha>`: chọn danh sách từ một lần push.
  - `scripts/auto-start-check.py dispatch <run_file> [--force]`: kiểm một lần bấm tay.
  - In ra stdout đường dẫn danh sách được phép bắt đầu, hay không in gì. Mã 0 khi đã kiểm và có danh sách, hay (chế độ `push`) lần push không có danh sách nào đủ điều kiện; 3 khi một danh sách bị từ chối vì kiểm tra chung bên dưới (stderr nêu lý do và việc dev nên làm); 1 khi không đọc được git hay GitHub; 2 khi sai tham số, kể cả `run_file` không khớp mẫu (kiểm bằng `re.fullmatch(r"docs/runs/RUN-[0-9]{8}-[0-9]{2}\.md", ...)`, không có ký tự xuống dòng).
- Chế độ `push`, các file được xét: `git diff --name-status --diff-filter=AMR <trước> <after> -- 'docs/runs/'`, chỉ tên khớp mẫu trên (file bị xóa không được xét; file đổi tên được xét theo tên mới, nhưng trạng thái ở `<trước>` đọc theo tên cũ, để đổi tên một danh sách đã `approved` không kích hoạt). `<trước>` là `--before` khi `git cat-file -e` thấy nó; không thấy hay toàn số 0 thì `<after>^1`; `<after>` không có cha thì không chọn gì, stderr nêu lý do. Một file đủ điều kiện khi:
  1. Ở `<after>`: `status: approved`, `start: auto`, `open_with: routine`.
  2. Ở `<trước>`: không có file (theo tên cũ khi đổi tên), hay `status` khác `approved`.
  - Thiếu điều nào thì stderr một dòng lý do (ví dụ `RUN-...: start là manual, không tự bắt đầu.`, `RUN-...: đã approved trước lần push này.`).
  - Nhiều file đủ điều kiện: lấy mã nhỏ nhất, stderr nêu các danh sách còn lại để dev bấm tay sau.
- Chế độ `dispatch`: file là ô nhập, đã kiểm định dạng; các kiểm tra trạng thái của danh sách vẫn do job `check` làm như hôm nay.
- Kiểm tra chung cho cả hai chế độ, với danh sách vừa có (thiếu một điều thì không in gì, stderr nêu lý do và việc dev nên làm):
  1. Không có nhánh `claude/<RUN>-report` trên GitHub (`gh api repos/<repo>/branches/claude%2F<RUN>-report`: 404 là chưa có; lỗi khác là mã 1).
  2. Không danh sách nào khác đang `running` trên nhánh báo cáo của nó: duyệt các nhánh `claude/RUN-*-report` từng trang (`&page=N`, như `resume-check.py`), đọc file danh sách trên nhánh đó. Đọc danh sách nhánh lỗi là mã 1.
  3. Không có job `fire` của một lần chạy `start-run.yml` khác (hay của lần thử trước của chính lần chạy này) đang `in_progress`, `queued`, hoặc kết luận `success` với `completed_at` trong `AUTO_START_FIRE_MINUTES` phút qua (mặc định 90), trừ khi đã có một nhánh báo cáo `running` (lúc đó điều 2 đã từ chối). Đọc `actions/workflows/start-run.yml/runs?per_page=20` rồi lọc trong script, và `actions/runs/<id>/jobs?filter=all&per_page=50` cho từng lần chạy; với chính lần chạy này (`GITHUB_RUN_ID`) chỉ tính các lần thử có `run_attempt` nhỏ hơn `GITHUB_RUN_ATTEMPT`, để job `fire` đang gọi script không tự chặn mình còn "Re-run all jobs" một lần chạy đã `fire` xanh thì bị chặn. Giờ hiện tại lấy từ `AUTO_START_NOW` (giờ ISO, cho test) hay đồng hồ.
- `--force` (chỉ chế độ `dispatch`) bỏ qua điều 3, và bỏ qua điều 1 khi nhánh báo cáo của chính danh sách đang `running` (mở lại phiên dừng giữa chừng). Điều 2 không bao giờ bỏ qua.

**B. `.github/workflows/start-run.yml` (sửa).**

- `on:` thêm `push: branches: [main], paths: ['docs/runs/RUN-*.md']`. Giữ `workflow_dispatch` và ô `run_file`; thêm ô `force` (boolean, mặc định `false`, mô tả: bỏ qua kiểm tra "vừa kích hoạt một phiên", chỉ dùng khi biết chắc phiên trước không bắt đầu).
- `concurrency` chuyển từ mức workflow xuống mức job: `check` và `fire` có `concurrency: { group: start-run, cancel-in-progress: false }`, `pick` không có. Lần chạy do push không chọn được gì vì thế không vào hàng chờ và không làm hủy lần chạy khác đang chờ trong nhóm.
- Job mới `pick` (đầu tiên, `if: github.ref == 'refs/heads/main'`, `timeout-minutes: 5`, không có khối `permissions` riêng, chỉ đọc theo `permissions` chung): `actions/checkout@v5` với `fetch-depth: 0` và `persist-credentials: false`. Một bước đặt output `run_file`, mọi giá trị từ sự kiện đi qua `env`, không chèn thẳng vào lệnh:
  - `push`: `python3 -I scripts/auto-start-check.py push --before "${BEFORE}" --after "${GITHUB_SHA}"`, với `BEFORE: ${{ github.event.before }}`.
  - `workflow_dispatch`: `python3 -I scripts/auto-start-check.py dispatch "${RUN_FILE_INPUT}"` (thêm `--force` khi `FORCE == 'true'`), với `RUN_FILE_INPUT: ${{ inputs.run_file }}`, `FORCE: ${{ inputs.force }}`.
  - Chỉ ghi vào `$GITHUB_OUTPUT` khi stdout khớp mẫu đường dẫn. Stderr của script vào phần tóm tắt của job. Mã 3 thì bước in `::error::` kèm lý do và thoát khác 0 (cả lần chạy do push lẫn bấm tay), để dev thấy lần chạy đỏ thay vì tưởng phiên đã chạy; mã 0 không có đường dẫn (lần push không đổi danh sách nào đủ điều kiện) thì xanh.
- Job `check`: `needs: pick`, `if: needs.pick.outputs.run_file != ''`, `RUN_FILE` lấy từ `needs.pick.outputs.run_file`; khai báo output `run_file` của chính nó, để `fire` đọc `needs.check.outputs.run_file` mà dòng `needs: [check, close-setup-broken]` giữ nguyên. `timeout-minutes` từ 5 lên 30.
- Bước "Kiểm tra main-guard trên nhánh chính", chỉ khi `github.event_name == 'push'`: một vòng tối đa 20 lần, mỗi lần đặt `code=0` rồi gọi `GITHUB_ACTIONS= scripts/main-guard-status.sh "${GITHUB_SHA}" || code=$?`; mã 2 thì chờ 60 giây rồi gọi lại, mã khác thì thoát vòng. Sau vòng, gọi một lần như hôm nay (có `::error::` khi không qua). Vòng không cần đọc giờ tạo lần chạy, nên dòng "lần chạy tạo lúc không rõ" không làm hỏng gì. `workflow_dispatch` giữ đúng một lần gọi như hôm nay.
- Job `fire`: khối `permissions` riêng thêm `contents: read` cạnh `actions: write` (khối mức job thay khối chung; `actions: write` đã gồm quyền đọc lần chạy và job, và một khóa YAML không được lặp lại, nên không thêm `actions: read`). Bước đầu: `actions/checkout@v5` với `persist-credentials: false`, rồi kiểm lại `python3 -I scripts/auto-start-check.py dispatch "${RUN_FILE}"` (thêm `--force` khi lần bấm tay chọn `force`), trước bước bật lịch và lời gọi API. Script không in đường dẫn thì bước đỏ với `::error::` và lý do, không gọi API. `fire` nằm trong nhóm `start-run` cùng `resume-run`, nên các lần `fire` chạy lần lượt và lần kiểm này thấy job `fire` của lần chạy trước (đang chạy hay đã xong). `RUN_FILE` lấy từ `needs.check.outputs.run_file`. Payload thêm dòng `Cách bắt đầu: tự động khi danh sách approved lên nhánh chính` cho lần chạy do push (prompt của routine chỉ lấy đường dẫn, dòng này chỉ để đọc).
- Lần chạy do push mà `pick` không chọn gì thì `check`, `close-setup-broken`, `fire` bị bỏ qua: workflow xanh, không gọi API.
- Chú thích đầu file nêu hai cách chạy, kiểm tra chống mở hai phiên, và vì sao không tự thử lại. Chú thích nhắc `main-broken` trước `scripts/main-guard-status.sh`, không viết chữ `continue-on-error` và không viết lại chuỗi `actions: write`, để giữ các test có sẵn đọc file theo chuỗi (mục 3.5).

**C. `scripts/run-check.py` (sửa), quy tắc mới.** Đọc `start` từ frontmatter của file danh sách: khác `auto`, `manual` hay trống là lỗi. `start: auto` mà `open_with` trong file khác `routine` là lỗi ("phiên tự bắt đầu chỉ kích hoạt được routine; đổi open_with hay ghi start: manual"). Quy tắc chỉ đọc `open_with` trong file, không đọc `RUN_CHECK_OPEN_WITH`, nên phiên mở tay bằng cách khác không bị lỗi cấp danh sách. Thêm dòng vào bảng của `docs/ops/pre-run-review.md`, mục 1.

**D. Tài liệu.**

- `docs/templates/run.md`: frontmatter thêm `start: auto   # auto: phiên tự bắt đầu khi danh sách approved lên nhánh chính (start-run.yml); manual: dev bấm start-run`.
- `docs/auto-run.md`, mục 1, bước 3: danh sách `start: auto` thì merge danh sách `approved` là kích hoạt. Sau merge, xem lần chạy `start-run` của lần push đó: job `fire` xanh thì không bấm tay nữa; không có lần chạy (GitHub không phát sự kiện push) hay lần chạy `cancelled` thì bấm tay như cũ; lần chạy đỏ thì đọc lý do trong tóm tắt của job đỏ (`pick` hay `fire`), sửa nguyên nhân rồi bấm tay, kèm `force` khi lý do là vừa kích hoạt một phiên mà dev biết chắc phiên đó không bắt đầu. Danh sách `start: auto` thì không dùng Run now hay lịch hẹn của routine (hai đường này không qua kiểm tra chống mở hai phiên); muốn chọn giờ thì ghi `start: manual`. Mở lại phiên dừng giữa chừng mà `resume-run` không tiếp tục: bấm `start-run` với `force`. Không "Re-run" một lần chạy `start-run` đã có `fire` xanh. Bước 6 (hạn mức Actions) đọc trước khi duyệt, vì duyệt lúc này là bắt đầu.
- `docs/ops/staging-and-runs.md`: mục 2.3 cùng nội dung cho người, kèm ô `force`; mục 2.8 thêm một dòng: mỗi push đổi danh sách chạy làm `start-run` chạy khoảng 1 phút và giữ lịch của `resume-run`, `run-watchdog` thêm 3 giờ.
- `docs/ops/setup-new-app.md`: phiên đầu của app mới, ghi rằng danh sách `start: auto` tự bắt đầu khi merge, bấm tay chỉ khi `start: manual` hay lần tự chạy không có.
- `docs/ops/to-app.md`: dòng về thời hạn của job `check` trong `start-run.yml` đổi 5 thành 30 phút, và ghi app đã tùy biến `start-run.yml` thì lần đồng bộ phải giữ phần tùy biến của app cùng trigger mới.
- `docs/CLAUDE.md`, mục Khi lập danh sách chạy: ghi `start:` (mặc định `auto`; `manual` khi dev muốn chọn giờ, ví dụ chờ hạn mức), và câu "Chỉ dev mới chuyển sang `approved` và kích hoạt phiên" thêm "với `start: auto`, lần merge danh sách `approved` là lần kích hoạt: lời duyệt của dev phải gồm việc bắt đầu phiên khi merge, không thì ghi `start: manual`; AI không merge danh sách `start: auto` khi lời duyệt chưa gồm điều đó".
- `.claude/skills/run-list/SKILL.md`: ghi `start:` khi lập; câu duyệt có sẵn nói rõ "bắt đầu phiên khi merge" với `start: auto`; bước báo dev nói merge danh sách `approved` có `start: auto` là bắt đầu phiên, và cách xem job `fire`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/auto-start-check.py` | Chọn và kiểm danh sách trước khi mở phiên (3.1 A) |
| Sửa | `.github/workflows/start-run.yml` | Trigger `push`, ô `force`, `concurrency` mức job, job `pick`, `check` và `fire` đọc `run_file` từ job trước, `fire` kiểm lại trước khi gọi API, chờ `main-guard` đang chạy với lần chạy do push (3.1 B) |
| Sửa | `scripts/run-check.py` | Quy tắc `start:` (3.1 C) |
| Sửa | `scripts/tests/run.sh` | Test của mục 4 |
| Sửa | `docs/templates/run.md` | Trường `start:` |
| Sửa | `docs/auto-run.md` | Mục 1, bước 3 và 6 |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.3, mục 2.8 |
| Sửa | `docs/ops/setup-new-app.md` | Phiên đầu của app mới |
| Sửa | `docs/ops/to-app.md` | Thời hạn job `check`, ghi chú đồng bộ `start-run.yml` |
| Sửa | `docs/ops/pre-run-review.md` | Mục 1, dòng của quy tắc `start:` |
| Sửa | `docs/CLAUDE.md` | Mục Khi lập danh sách chạy |
| Sửa | `.claude/skills/run-list/SKILL.md` | Ghi `start:`, câu báo dev |
| Sửa | `docs/features/quy-trinh/plans/T-104-tu-kich-hoat-phien.md` | Nhật ký thực hiện (ticket `auto_run: false`, làm có người, một PR) |
| Sửa | `docs/features/quy-trinh/tickets/T-104-tu-kich-hoat-phien.md` | Trạng thái |

Nơi đọc file bị đổi (`scripts/plan-check.py`, L-020). Hai file phải sửa đã ở bảng trên (`docs/ops/to-app.md`, `docs/ops/setup-new-app.md`); các file dưới đây không bị ảnh hưởng, không sửa:

- Nhắc `.github/workflows/start-run.yml` trong chú thích, thông báo hay để bật lịch, không đọc cấu trúc job, tên ô `run_file` hay trigger của nó: `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml` (giữ nhóm `concurrency: start-run` ở mức workflow; `check` và `fire` của `start-run.yml` dùng cùng tên nhóm ở mức job), `.github/workflows/run-watchdog.yml`, `scripts/env-canary.sh`, `scripts/run-watchdog.py`, `scripts/schedule-idle.py` (đọc giờ lần chạy gần nhất của `start-run.yml` để giữ lịch bật 3 giờ; lần chạy do push mà `pick` không chọn gì cũng được tính, chỉ làm lịch bật lâu hơn, không hại), `scripts/setup-check.sh`.
- Nhắc `docs/auto-run.md`, `docs/ops/staging-and-runs.md` hay `docs/CLAUDE.md` bằng đường dẫn trong chú thích hay thông báo trỏ tới mục khác với các mục plan sửa (mục 1 bước 3 và 6 của `auto-run.md`, mục 2.3 của `staging-and-runs.md`, mục Khi lập danh sách chạy của `docs/CLAUDE.md`): `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/env-canary.sh`, `scripts/failure-kind.sh`, `scripts/local-setup.sh`, `scripts/merge-if-green.sh`, `scripts/new-app.sh`, `scripts/push-on-commit.sh`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/setup-check.sh`, `scripts/status.py` (đọc trạng thái danh sách, không đọc trường `start`), `scripts/ui-test-verdict.sh`, `scripts/tests/playbook/PB-17.log`. Plan không đổi tên mục nào.
- Chạy hay nhắc `scripts/tests/run.sh`: `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml`, `scripts/check.sh`, `scripts/local-setup.sh`, `scripts/main-guard-verified.sh`, `scripts/merge-if-green.sh`, `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt`. Plan chỉ thêm test, không đổi cách gọi hay tên file.
- `scripts/java-check.sh` nhắc `docs/ops/setup-new-app.md`, `scripts/template-sync.sh` nhắc `docs/ops/to-app.md`, `scripts/wait-run.sh` nhắc `docs/auto-run.md`: đều trỏ tới mục khác với đoạn plan sửa (giới hạn Java, cách đồng bộ chung, bước 8), không đọc nội dung.
- `scripts/plan-queue.py` nạp `scripts/run-check.py` để dùng `front_matter`, `ids`, `spec_problems`; plan chỉ thêm một quy tắc trong phần kiểm danh sách, không đổi các hàm đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. Quyền của workflow: job `pick` chỉ đọc (`contents: read`, `actions: read` của `permissions` chung, đủ để đọc lần chạy và job của `start-run.yml`); `fire` thêm `contents: read` để checkout; quyền đọc lần chạy và job đã có trong `actions: write`; không job nào thêm quyền ghi; `fire` vẫn là job duy nhất có `actions: write`, `close-setup-broken` vẫn là job duy nhất có `issues: write`
- **Thư viện và SDK:** Không thêm. Script chỉ dùng thư viện chuẩn của Python 3, `git` và `gh`, như `scripts/resume-check.py`

### 3.4. Phương án dự phòng

Không có. Cách xử lý khi không có commit trước đã nằm trong 3.1 A (cha thứ nhất), có test T9.

### 3.5. Hành vi đã có bị đổi

Không sửa code Kotlin nên `scripts/existing-tests.py` không áp dụng. Các câu khẳng định có sẵn của `scripts/tests/run.sh` đọc `start-run.yml` theo số đếm hay thứ tự dòng, và một hành vi của lần bấm tay đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Workflow có một bước checkout, bước đó `persist-credentials: false` | Thêm checkout của `pick` và của `fire`, cũng `persist-credentials: false` | `start-run: the setup check runs without a GitHub token` (đếm `persist-credentials: false` bằng 1) | Câu mới, giữ ý định "không checkout nào để token trong `.git/config`": số dòng `persist-credentials: false` bằng số dòng `uses: actions/checkout` (mong `3 3 1`: hai số đếm và số dòng `env -u GH_TOKEN ... setup-check.sh --staging`). Đối chứng: bỏ `persist-credentials` ở checkout của `pick` thì đỏ |
| `fire` có `needs: [check, close-setup-broken]`; `check` không có `needs` | `check` có `needs: pick`; `fire` giữ nguyên dòng `needs` | `start-run: only close-setup-broken writes issues, between the checks and the routine` | Giữ nguyên (dòng `needs` của `fire` và `close-setup-broken` không đổi) |
| Lần nhắc đầu của `main-broken` đứng trước lần nhắc đầu của `scripts/main-guard-status.sh`, trước bước thiết lập staging và bước kích hoạt; không có `continue-on-error` | Chú thích đầu file và vòng chờ mới | `start-run: main-guard step runs after the run-file checks and before the routine` (mong `yes yes 0`) | Giữ nguyên: chú thích nhắc `main-broken` trước `main-guard-status.sh`, vòng chờ bắt mã bằng `|| code=$?`, không chỗ nào viết chữ `continue-on-error` |
| `actions: write` xuất hiện đúng một lần | `fire` thêm `contents: read`, không thêm quyền ghi, không có `actions: read` | Test có sẵn đếm `actions: write` của `start-run.yml` | Giữ nguyên: không chú thích nào viết lại chuỗi đó |
| Bấm `start-run` cho danh sách đang `running` trên nhánh báo cáo thì kích hoạt, phiên tiếp tục nhánh đó | Phải chọn `force` (3.1 A) | Không có test | Câu mới ở T6: `dispatch` không `--force` mong mã 3; với `--force` khi nhánh `running` mong đường dẫn |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`. Test của script dùng `gh` giả (`scripts/tests/fake-gh`, route theo `FIXTURE`) và một kho git tạm có hai commit: commit A (trước) và commit B (sau, cha là A), dựng như khối test `plan-queue` (biến `GIT_AUTHOR_*`, `GIT_CONFIG_GLOBAL=/dev/null` của bộ test). Hàm `asc_run <args...>` in stdout của script, stderr vào `$work/asc.err`; `asc_code` in mã thoát. Danh sách thử viết bằng hàm `write_run <id> <status> <start> <open_with>`, frontmatter có chú thích sau `#` như mẫu. Fixture mặc định: nhánh báo cáo trả 404, danh sách nhánh rỗng, không có lần chạy `start-run` nào; `GITHUB_RUN_ID=999`.

| Điều cần đạt | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Danh sách vừa chuyển sang `approved`, đủ điều kiện | Test script | T1 `auto-start-check: a run list approved by this push with start auto is picked`: A có `RUN-20261201-01` `draft`, B có cùng file `approved`, `start: auto   # ...`, `open_with: routine`. `push --before A --after B`. Mong `docs/runs/RUN-20261201-01.md`. Đỏ trước code (script chưa có) |
| File mới thêm ở trạng thái `approved` | Test script | T2: A không có file, B có file đủ điều kiện. Mong đường dẫn |
| Lần push sau không kích hoạt lại | Test script | T3 (đối chứng): A và B đều `approved` (B sửa một dòng chữ). Mong rỗng, stderr có `đã approved trước lần push này` |
| `start: manual` hay không có trường | Test script | T4: hai ca, mong rỗng cả hai, stderr có `start là manual` và `không ghi start` |
| `open_with` khác `routine` | Test script | T5: `open_with: cloud`, mong rỗng, stderr nêu `open_with` |
| Đã có nhánh báo cáo | Test script | T6: route `branches/claude%2FRUN-20261201-01-report` trả một nhánh, file trên đó `finished`. Mong stdout rỗng và mã 3 cho `push` và cho `dispatch docs/runs/RUN-20261201-01.md`, kể cả `--force`; stderr `đã có nhánh báo cáo`. File trên nhánh đó `running` (mở lại): `dispatch` không `--force` mã 3, có `--force` mong đường dẫn |
| Có phiên khác đang chạy | Test script | T7: danh sách nhánh có `claude/RUN-20261130-01-report`, file trên đó `running`. Mong stdout rỗng, mã 3 cho cả `push` và `dispatch` (kể cả `--force`), stderr `Đang có phiên RUN-20261130-01 chạy`. Đối chứng: file trên nhánh đó `finished` thì mong đường dẫn |
| Nhánh báo cáo của phiên khác ở trang 2 | Test script | T8: trang 1 có 100 nhánh khác, trang 2 có nhánh báo cáo `running`. Mong rỗng |
| Không có commit trước | Test script | T9: `--before` toàn số 0, rồi một mã không có trong kho; cha của B (A) có file `draft`: mong đường dẫn cả hai. Cha có file `approved`: mong rỗng. Kho một commit (B không có cha): mong rỗng, stderr nêu không có commit trước |
| Hai danh sách cùng đủ điều kiện | Test script | T10: B có `RUN-20261201-02` và `RUN-20261201-01` cùng đủ. Mong `docs/runs/RUN-20261201-01.md`, stderr nêu `RUN-20261201-02` |
| File không phải danh sách, file bị xóa, file đổi tên | Test script | T11: B đổi `docs/runs/README.md` và `docs/runs/patches/x.md`: mong rỗng, mã 0. B xóa một danh sách `approved`: mong rỗng, mã 0. B đổi tên một danh sách `draft` thành tên khác và để `approved`: mong tên mới. Đối chứng: B đổi tên một danh sách đã `approved` (giữ `approved`): mong rỗng |
| Vừa (hay đang) kích hoạt một phiên, chưa có nhánh báo cáo | Test script | T12: `AUTO_START_NOW=2026-12-01T10:00:00Z`, `GITHUB_RUN_ID=999`, `GITHUB_RUN_ATTEMPT=1`. Route `actions/workflows/start-run.yml/runs?per_page=20` trả lần chạy id 500; route `actions/runs/500/jobs?filter=all&per_page=50` có job `fire` `completed`, `success`, `completed_at` `2026-12-01T09:30:00Z`. Mong stdout rỗng, mã 3 cho `push` và cho `dispatch`, stderr `vừa kích hoạt một phiên`; `dispatch ... --force`: mong đường dẫn. Các ca khác của cùng route: `fire` `in_progress` hay `queued`: mã 3; `fire` `success` xong lúc `08:00` (quá 90 phút): mong đường dẫn; `fire` `skipped`: mong đường dẫn. Lần chạy là chính 999 với `fire` `in_progress` ở lần thử 1: mong đường dẫn (không tự chặn); lần chạy 999 với `GITHUB_RUN_ATTEMPT=2` và `fire` `success` ở lần thử 1: mã 3 |
| GitHub lỗi | Test script | T13: route nhánh báo cáo trả lỗi (`fake-gh` thoát 1): mã 1, stdout rỗng. Route danh sách nhánh lỗi: mã 1. Route lần chạy `start-run` lỗi: mã 1 |
| Sai tham số | Test script | T14: thiếu `--after`; `dispatch` với `docs/runs/../x.md`; `dispatch` với `docs/runs/RUN-20261201-01.md` cộng ký tự xuống dòng ở cuối; `dispatch` với chữ số Unicode. Mong mã 2 cả bốn, stdout rỗng |
| Quy tắc `start:` của `run-check.py` | Test script | T15, bốn ca trên danh sách thử của khối `run-check` có sẵn: `start: soon` lỗi (`start` phải là auto hay manual); `start: auto` với `open_with: cloud` lỗi; `start: auto` với `open_with: routine` không có dòng lỗi mới; `start: auto`, `open_with: routine` và `RUN_CHECK_OPEN_WITH=cloud` không có dòng lỗi về `start`. Hai ca đầu đỏ trước code |
| Cấu trúc workflow: trigger và ô nhập | Test script | T16 `start-run: runs on a push to main that changes a run list`: có khối `push:` với `branches: [main]` và `paths: ['docs/runs/RUN-*.md']`; vẫn có `workflow_dispatch` với `run_file`; có ô `force` kiểu `boolean` mặc định `false`. Đỏ trước code |
| Cấu trúc workflow: chọn danh sách | Test script | T17: job `pick` gọi `scripts/auto-start-check.py push --before "${BEFORE}"` và `scripts/auto-start-check.py dispatch "${RUN_FILE_INPUT}"`; không dòng `run:` nào chứa `${{ github.event.before }}` hay `${{ inputs.run_file }}` (chỉ ở `env:`); `check` có `needs: pick` và `if: needs.pick.outputs.run_file != ''`; `fire` đọc `needs.check.outputs.run_file`. Đỏ trước code |
| Cấu trúc workflow: quyền và hàng chờ | Test script | T18: `actions: write` đúng một lần (trong `fire`), `issues: write` đúng một lần; `pick` không có khối `permissions` hay `concurrency`; trong phần của job `fire` (đọc bằng `wf_job .github/workflows/start-run.yml fire` như các test có sẵn) có `contents: read` và `actions: write`, không có dòng `actions: read` (khóa lặp làm GitHub từ chối file; khối `permissions` chung vẫn có `actions: read` cho các job khác); `check` và `fire` có `group: start-run`; không còn `concurrency` ở mức workflow. Phần đếm quyền ghi qua trước code (đối chứng), phần còn lại đỏ trước code |
| Cấu trúc workflow: `fire` kiểm lại trước khi gọi API | Test script | T20: trong job `fire`, dòng gọi `scripts/auto-start-check.py dispatch` đứng trước dòng `name: Bật lịch của phiên` và trước `curl`; có `--force` theo ô `force`. Đỏ trước code |
| Bước `pick` đỏ khi bị từ chối | Test script | T21: bước đặt output của `pick` có nhánh mã 3 in `::error::` và `exit 1`. Đỏ trước code |
| Cấu trúc workflow: chờ `main-guard` | Test script | T19: bước kiểm `main-guard` có vòng `GITHUB_ACTIONS= scripts/main-guard-status.sh` với giới hạn 20 lần, `code=0` trong vòng và `|| code=$?`, chỉ trong nhánh `push`; `check` có `timeout-minutes: 30`; không có `continue-on-error`. Đỏ trước code |
| Không checkout nào để token | Test script | Câu mới của mục 3.5 |
| Tự chạy thật trên GitHub | Kiểm tra thủ công (người: cần merge thật lên nhánh chính và routine thật) | Sau merge, khi không có phiên nào chạy: một danh sách thử `approved`, `start: auto`, một task nhỏ, merge bằng `merge-if-green.sh`. Thấy đúng một lần chạy `start-run` do push có `fire` xanh, một phiên. Bấm tay `start-run` cho cùng danh sách ngay sau đó: lần chạy đỏ ở `pick` (`vừa kích hoạt một phiên` hay `đã có nhánh báo cáo`). Lần merge PR báo cáo cuối phiên (danh sách sang `finished`) là lần push thứ hai: lần chạy `start-run` xanh, `pick` không chọn gì, không có phiên mới. Phiên này đi đủ các bước cuối phiên và tốn hạn mức như một phiên thường |
| Tài liệu đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc các đoạn sửa ở mục 3.1 D. `scripts/history-refs.py` qua |
| Toàn bộ | Test script | `scripts/tests/run.sh` trên CI (`pr-check`, rồi `main-guard` sau merge). Trên máy dev macOS: `source ~/.local/share/app-project-template/env.sh` trước; bản đầy đủ in dòng tổng kết, khoảng 1570 dòng `ok`; 6 test đỏ sẵn trên máy Mac (5 `pr-check: ... order check/filter`, 1 `wait-run: a draft PR`) |

**Ngoại lệ viết test trước:** tài liệu ở mục 3.1 D và chú thích đầu workflow: chỉ là chữ. Vòng chờ `main-guard` trong workflow chỉ chạy được trên GitHub: test cấu trúc T19 kiểm có vòng, giới hạn và cách bắt mã, không chạy nó.

### 4.1. Chạy thử

Không cần (E dưới 2): trigger `push` có `paths` đã dùng ở `status-page.yml`; output giữa các job, `needs.<job>.outputs` và `concurrency` mức job là cách GitHub Actions thông dụng; script theo mẫu `resume-check.py`. `github.event.before` lần đầu dùng, nhưng có cách xử lý khi thiếu (3.1 A) và test T9.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Plan sửa `.github/workflows/start-run.yml`, workflow mở phiên tự chạy. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một workflow, một script mới, một quy tắc của `run-check.py`, test, bảy tài liệu |
| B. Độ rõ của yêu cầu | 1 | Yêu cầu rõ. Còn quyết định của người về ADR-0009, ADR-0016 (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Script và quy tắc có test đầy đủ với `gh` giả; trigger, hàng chờ và vòng chờ `main-guard` của workflow chỉ kiểm cấu trúc, phần chạy thật kiểm sau merge |
| D. Ảnh hưởng chéo | 3 | Sai hướng thì mở phiên ngoài ý người, hay hai phiên cùng lúc; mỗi lần gọi API là một phiên và không chống trùng |
| E. Độ mới của kỹ thuật | 1 | `github.event.before` lần đầu dùng; có cách xử lý khi thiếu |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `start-run.yml` (mọi phiên), `run-check.py` (mọi danh sách), mẫu danh sách chạy. `resume-run.yml` giữ nhóm `concurrency: start-run` ở mức workflow; `check` và `fire` dùng cùng tên nhóm ở mức job, nên không chạy cùng lúc với `resume-run`. GitHub chỉ giữ một lần chờ cho mỗi nhóm: một lần chờ có thể bị lần sau thay và thành `cancelled`; tài liệu ở bước 3 ghi bấm tay khi thấy `cancelled`.
- ADR-0048 (`proposed`) có tương tác: theo nó AI ghi danh sách `approved` và merge PR của đợt, nên với `start: auto` lần merge đó bắt đầu phiên. Tài liệu (3.1 D) buộc lời duyệt phải gồm việc bắt đầu phiên khi merge, không thì `start: manual`.
- Task khác có thể xung đột: ADR-0048 sẽ sửa `docs/CLAUDE.md` mục Khi lập danh sách chạy, `docs/ops/pre-run-review.md`, `docs/templates/run.md`, `.claude/skills/run-list/SKILL.md` và thêm `run-check.py --batch`. Hai việc sửa các đoạn khác nhau của cùng file; việc làm sau tìm chỗ sửa theo tên mục, không theo số dòng.
- App đã tùy biến `start-run.yml` (thời hạn job, bí mật riêng): lần đồng bộ về app phải gộp tay (`docs/ops/to-app.md`).
- Đề xuất thứ tự chạy: làm sau khi ADR thay ADR-0009, ADR-0016 được quyết (mục 8, câu 1). Không cần chờ ADR-0048.

## 7. Điều kiện dừng riêng

- Lần thử sau merge (dòng kiểm tra thủ công của mục 4) thấy một lần push mở hai phiên, hay lần bấm tay ngay sau đó không bị từ chối: dừng, người đổi mẫu về `start: manual` trong một PR, rồi xét lại.
- Không chuyển sang cách "mọi danh sách `approved` chưa có nhánh báo cáo" dưới bất kỳ lý do nào: nhánh báo cáo bị xóa sau merge, nên cách đó mở lại danh sách cũ ở một lần push về sau.

## 8. Câu hỏi cho người duyệt

1. **ADR-0009 và ADR-0016 đều ghi `start-run.yml` "chạy tay"** (ADR-0009 ở đoạn hạ tầng và bảng workflow, ADR-0016 ở phần `start-run.yml`). Plan này đổi điều đó, nên theo ADR-0044 cần một ADR hợp nhất, được chấp nhận trước khi duyệt plan. Đề xuất: một ADR hợp nhất thay ADR-0016 (chép mọi quy tắc về `staging.yml`, `start-run.yml` và bí mật; đổi "chạy tay" thành "chạy tay, hoặc tự chạy khi lần push lên nhánh chính chuyển một danh sách `start: auto`, `open_with: routine` sang `approved`"), và nêu rõ nó thay đoạn nào của ADR-0009; nếu ADR-0009 còn quy tắc khác cùng đoạn đó thì hợp nhất cả ADR-0009. AI viết ADR đó (`proposed`) trong một PR riêng; anh duyệt ADR rồi duyệt plan.
2. **Ai làm.** Ticket ghi `auto_run: false`: làm có người, một PR, vì task sửa chính workflow mở phiên và đẩy thay đổi `.github/workflows/` cần token có quyền `workflow` (phiên `routine` ghi `thử`). Đề xuất: giữ `auto_run: false`, AI làm cùng anh trên máy dev. Anh muốn chạy trong phiên tự chạy thì bỏ `auto_run: false` và danh sách chạy chạy `scripts/push-probe.sh` ở đầu phiên.
3. **Mặc định của mẫu.** Plan để mẫu ghi `start: auto`. Danh sách cũ không có trường thì không tự chạy. Đề xuất: giữ `auto`, vì mục tiêu là bớt bước bấm tay; muốn chọn giờ thì ghi `manual`.
4. **Khoảng chặn sau một lần kích hoạt.** Plan chặn mọi lần mở phiên khi một job `fire` khác đang chạy, đang chờ, hay xong xanh trong 90 phút qua mà chưa có nhánh báo cáo `running` (lần bấm tay bỏ qua được bằng `force`). Đề xuất: 90 phút tính từ lúc `fire` xong, vì phiên bình thường đẩy nhánh báo cáo trong vài phút đầu, còn phiên đang chờ hạn mức thì `resume-run` lo khi đã có nhánh.
5. **Mở lại phiên bằng tay.** Hôm nay bấm `start-run` cho danh sách đang `running` là mở lại phiên đó; sau plan phải chọn `force`. Đề xuất: chấp nhận, vì `force` là chỗ dev xác nhận mình biết đang làm gì.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ.
Đường làm nhanh (ADR-0034): tiêu đề "Làm nhanh YYYY-MM-DD, phiên bản plan N", ghi trong lần đẩy cuối của PR, sau rà soát độc lập và trước CI; Kết quả ghi "merge bằng PR này", Kết quả CI ghi "CI của PR này", bỏ dòng Cần người xem sáng nay, ghi việc cần người kiểm. -->

Chưa có.
