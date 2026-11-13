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

`start-run.yml` chạy thêm khi có push lên nhánh chính đổi `docs/runs/RUN-*.md`. Một script mới, `scripts/auto-start-check.py`, chọn danh sách mà chính lần push đó vừa chuyển sang `approved`, có `start: auto` và `open_with: routine`, chưa có nhánh báo cáo và không có phiên khác đang chạy. Có danh sách thì workflow đi qua đúng các kiểm tra hiện có rồi kích hoạt routine; không có thì dừng xanh, không làm gì. Kiểm chứng bằng test của script với `gh` và git giả, test của `run-check.py`, và test cấu trúc của workflow trong `scripts/tests/run.sh`.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn: `features/quy-trinh/requests/2026-11-12-giam-thoi-gian-cho.md`, việc 3; ticket `T-104`.

| Điều cần đạt | Cách hiểu của AI |
| --- | --- |
| Danh sách `approved` lên nhánh chính thì phiên tự bắt đầu | "Lên nhánh chính" là lần push mà file danh sách ở commit sau có `status: approved` còn ở commit trước thì chưa có file hoặc khác `approved`. Chỉ dựa vào chính lần push đó, nên lần push sau đổi cùng file (PR báo cáo, sửa chữ) không kích hoạt lại |
| Có ô chọn để giữ cách bấm tay | Trường `start:` trong frontmatter. `auto`: tự bắt đầu; `manual` hay trống: như cũ. Danh sách cũ không có trường nên không đổi hành vi. Mẫu mới ghi `auto` |
| Không mở phiên sai | Chỉ `open_with: routine` (workflow chỉ kích hoạt được routine; phiên `cloud`, `local` do người mở). Chưa có nhánh `claude/<RUN>-report` (danh sách chưa chạy). Không có danh sách nào khác đang `running` trên nhánh báo cáo của nó (mỗi lúc một phiên) |
| Vẫn qua các kiểm tra của `start-run` | Giữ nguyên bước kiểm danh sách, `main-broken`, `env-broken`, `main-guard-status.sh`, `setup-check.sh --staging`, đóng `setup-broken` cũ, bật lịch. Chỉ thêm: với lần chạy do push, `main-guard` đang chạy (mã 2) thì chờ có giới hạn rồi kiểm lại, vì merge danh sách thường ngay sau merge code |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan: ADR-0016 (`start-run.yml` chạy tay, không tự thử lại; xem mục 8, câu 1), ADR-0009 (hạ tầng phiên), ADR-0048 (`proposed`, duyệt cả đợt một lần; hai việc bổ sung nhau nhưng không phụ thuộc), ADR-0044.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/auto-start-check.py` (mới).** Theo mẫu `scripts/resume-check.py`: chỉ dùng `gh api` (REST) và `git`, `GITHUB_REPOSITORY` hay remote `origin` cho tên repo, thông báo tiếng Việt ra stderr.

- Cách dùng: `scripts/auto-start-check.py --before <sha> --after <sha>`. In ra stdout đường dẫn danh sách cần bắt đầu (`docs/runs/RUN-YYYYMMDD-NN.md`) hay không in gì. Mã 0 khi đã kiểm (có hay không có danh sách), 1 khi không đọc được git hay GitHub, 2 khi sai tham số.
- Các file được xét: `git diff --name-only <before> <after> -- 'docs/runs/RUN-*.md'`, chỉ tên khớp `^docs/runs/RUN-\d{8}-\d{2}\.md$`. `<before>` toàn số 0 (nhánh mới) hay không có trong kho: xét mọi file danh sách ở `<after>` coi như trước đó chưa có.
- Một file được chọn khi đủ:
  1. Ở `<after>`: frontmatter có `status: approved`, `start: auto`, `open_with: routine` (đọc bằng `git show <after>:<file>`, cách đọc frontmatter như `resume-check.py`, bỏ phần chú thích sau `#`).
  2. Ở `<before>`: không có file, hay `status` khác `approved`.
  3. Không có nhánh `claude/<RUN>-report` trên GitHub (`gh api repos/<repo>/branches/claude%2F<RUN>-report`: 404 là chưa có; lỗi khác là không đọc được, mã 1).
- Thiếu điều 1 hay 2: stderr một dòng lý do (ví dụ `RUN-...: start là manual, không tự bắt đầu.`), bỏ qua.
- Không có phiên khác đang chạy: duyệt các nhánh `claude/RUN-*-report` (từng trang `&page=N` như `resume-check.py`), đọc file danh sách trên nhánh đó; có `status: running` thì không chọn gì, stderr: `Đang có phiên <RUN khác> chạy: <RUN> không tự bắt đầu. Bấm start-run sau khi phiên kia xong.`
- Nhiều file đủ điều kiện trong cùng một lần push: chọn mã nhỏ nhất, stderr nêu các danh sách còn lại để dev bấm tay sau.

**B. `.github/workflows/start-run.yml` (sửa).**

- `on:` thêm `push: branches: [main], paths: ['docs/runs/RUN-*.md']`. Giữ `workflow_dispatch` và ô `run_file` như cũ.
- Job mới `pick` (đầu tiên, `if: github.ref == 'refs/heads/main'`, `timeout-minutes: 5`, quyền chỉ đọc theo `permissions` chung): `actions/checkout@v5` với `fetch-depth: 0` và `persist-credentials: false`. Một bước đặt output `run_file`:
  - `workflow_dispatch`: `run_file` là ô nhập, như cũ.
  - `push`: `run_file="$(python3 -I scripts/auto-start-check.py --before "${BEFORE}" --after "${GITHUB_SHA}")"`, với `BEFORE: ${{ github.event.before }}` qua `env`, không chèn thẳng vào lệnh. Ghi stderr của script vào phần tóm tắt của job.
- Job `check`: `needs: pick`, `if: needs.pick.outputs.run_file != ''`, `RUN_FILE` lấy từ `needs.pick.outputs.run_file` thay cho `inputs.run_file`; khai báo output `run_file` của chính nó (giá trị đó), để `fire` đọc `needs.check.outputs.run_file` mà dòng `needs: [check, close-setup-broken]` giữ nguyên.
- Bước "Kiểm tra main-guard trên nhánh chính": với `github.event_name == 'push'`, mã 2 (`CHỜ`) thì lấy giờ tạo lần chạy từ dòng script in, chờ bằng `scripts/wait-run.sh --workflow main-guard.yml --since <giờ đó> --timeout 25`, rồi gọi lại `scripts/main-guard-status.sh`; mọi mã khác 0 vẫn đỏ như hôm nay. Với `workflow_dispatch` giữ nguyên hành vi. `timeout-minutes` của `check` từ 5 lên 30.
- Job `fire`: `RUN_FILE` lấy từ `needs.check.outputs.run_file`. Payload thêm dòng `Cách bắt đầu: tự động khi danh sách approved lên nhánh chính` cho lần chạy do push (prompt của routine chỉ lấy đường dẫn, dòng này chỉ để đọc).
- Lần chạy do push mà `pick` không chọn gì thì `check`, `close-setup-broken`, `fire` bị bỏ qua: workflow xanh, không gọi API.
- Chú thích đầu file nêu hai cách chạy và vì sao không tự thử lại.

**C. `scripts/run-check.py` (sửa), quy tắc mới.** Đọc `start` của danh sách: khác `auto`, `manual` hay trống là lỗi. `start: auto` mà `open_with` khác `routine` là lỗi ("phiên tự bắt đầu chỉ kích hoạt được routine; đổi open_with hay ghi start: manual"). Thêm dòng vào bảng của `docs/ops/pre-run-review.md`, mục 1.

**D. Tài liệu.**

- `docs/templates/run.md`: frontmatter thêm `start: auto   # auto: phiên tự bắt đầu khi danh sách approved lên nhánh chính (start-run.yml); manual: dev bấm start-run`.
- `docs/auto-run.md`, mục 1, bước 3: danh sách `start: auto` thì merge danh sách `approved` là kích hoạt; xem lần chạy `start-run` của lần push đó (Actions); không có lần chạy (GitHub không phát sự kiện push) hay lần chạy đỏ thì bấm tay như cũ. Bước 6 (hạn mức Actions) đọc trước khi duyệt, vì duyệt lúc này là bắt đầu.
- `docs/ops/staging-and-runs.md`, mục 2.3: cùng nội dung cho người.
- `docs/CLAUDE.md`, mục Khi lập danh sách chạy: ghi `start:` (mặc định `auto`; `manual` khi dev muốn chọn giờ, ví dụ chờ hạn mức), và câu "Chỉ dev mới chuyển sang `approved` và kích hoạt phiên" thêm "với `start: auto`, lần merge danh sách `approved` là lần kích hoạt".
- `.claude/skills/run-list/SKILL.md`: ghi `start:` khi lập; bước báo dev nói rõ merge danh sách `approved` có `start: auto` là bắt đầu phiên.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/auto-start-check.py` | Chọn danh sách cần tự bắt đầu từ một lần push (3.1 A) |
| Sửa | `.github/workflows/start-run.yml` | Trigger `push`, job `pick`, `check` và `fire` đọc `run_file` từ job trước, chờ `main-guard` đang chạy với lần chạy do push (3.1 B) |
| Sửa | `scripts/run-check.py` | Quy tắc `start:` (3.1 C) |
| Sửa | `scripts/tests/run.sh` | Test của mục 4 |
| Sửa | `docs/templates/run.md` | Trường `start:` |
| Sửa | `docs/auto-run.md` | Mục 1, bước 3 và 6 |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.3 |
| Sửa | `docs/ops/pre-run-review.md` | Mục 1, dòng của quy tắc `start:` |
| Sửa | `docs/CLAUDE.md` | Mục Khi lập danh sách chạy |
| Sửa | `.claude/skills/run-list/SKILL.md` | Ghi `start:`, câu báo dev |
| Sửa | `docs/features/quy-trinh/plans/T-104-tu-kich-hoat-phien.md` | Nhật ký thực hiện (ticket `auto_run: false`, làm có người, một PR) |
| Sửa | `docs/features/quy-trinh/tickets/T-104-tu-kich-hoat-phien.md` | Trạng thái |

Nơi đọc file bị đổi (`scripts/plan-check.py`, L-020), không file nào bị ảnh hưởng, không sửa:

- Nhắc `.github/workflows/start-run.yml` trong chú thích, thông báo hay để bật lịch, không đọc cấu trúc job, tên ô `run_file` hay trigger của nó: `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml` (dùng chung nhóm `concurrency: start-run`, giữ nguyên), `.github/workflows/run-watchdog.yml`, `scripts/env-canary.sh`, `scripts/run-watchdog.py`, `scripts/schedule-idle.py` (đọc giờ lần chạy gần nhất của `start-run.yml` để giữ lịch bật 3 giờ; lần chạy do push mà `pick` không chọn gì cũng được tính, chỉ làm lịch bật lâu hơn, không hại), `scripts/setup-check.sh`.
- Nhắc `docs/auto-run.md`, `docs/ops/staging-and-runs.md` hay `docs/CLAUDE.md` bằng đường dẫn trong chú thích hay thông báo trỏ tới mục khác với các mục plan sửa (mục 1 bước 3 và 6 của `auto-run.md`, mục 2.3 của `staging-and-runs.md`, mục Khi lập danh sách chạy của `docs/CLAUDE.md`): `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/env-canary.sh`, `scripts/failure-kind.sh`, `scripts/local-setup.sh`, `scripts/merge-if-green.sh`, `scripts/new-app.sh`, `scripts/push-on-commit.sh`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/setup-check.sh`, `scripts/status.py` (đọc trạng thái danh sách, không đọc trường `start`), `scripts/ui-test-verdict.sh`, `scripts/tests/playbook/PB-17.log`. Plan không đổi tên mục nào.
- Chạy hay nhắc `scripts/tests/run.sh`: `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml`, `scripts/check.sh`, `scripts/local-setup.sh`, `scripts/main-guard-verified.sh`, `scripts/merge-if-green.sh`, `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt`. Plan chỉ thêm test, không đổi cách gọi hay tên file.
- `scripts/plan-queue.py` nạp `scripts/run-check.py` để dùng `front_matter`, `ids`, `spec_problems`; plan chỉ thêm một quy tắc trong phần kiểm danh sách, không đổi các hàm đó.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm. Quyền của workflow: job `pick` chỉ đọc (`contents: read`, `actions: read` của `permissions` chung); không job nào thêm quyền ghi; `fire` vẫn là job duy nhất có `actions: write`, `close-setup-broken` vẫn là job duy nhất có `issues: write`
- **Thư viện và SDK:** Không thêm. Script chỉ dùng thư viện chuẩn của Python 3, `git` và `gh`, như `scripts/resume-check.py`

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `github.event.before` của lần push do merge PR (squash hay merge commit) có khi không có trong kho của job, ví dụ commit trước bị đẩy đè.
- **Điều kiện chuyển:** test T9 hay lần thử trên GitHub sau merge thấy `git cat-file -e <before>` thất bại với lần push bình thường.
- **Cách làm thay:** script dùng `<after>^1` (cha thứ nhất) khi `<before>` không có trong kho; vẫn giữ quy tắc "trước đó khác `approved`", nên lần push sau không kích hoạt lại. Test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không sửa code Kotlin nên `scripts/existing-tests.py` không áp dụng. Hai câu khẳng định có sẵn của `scripts/tests/run.sh` về `start-run.yml` đọc file theo số đếm, và plan đổi đúng chỗ chúng đếm:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Workflow có một bước checkout, bước đó `persist-credentials: false` | Thêm checkout của `pick`, cũng `persist-credentials: false` | `start-run: the setup check runs without a GitHub token` (đếm `persist-credentials: false` bằng 1) | Câu mới, giữ ý định "không checkout nào để token trong `.git/config`": số dòng `persist-credentials: false` bằng số dòng `uses: actions/checkout` (mong `2 2 1`: hai số đếm và số dòng `env -u GH_TOKEN ... setup-check.sh --staging`). Đối chứng: bỏ `persist-credentials` ở checkout của `pick` thì đỏ |
| `fire` có `needs: [check, close-setup-broken]`; `check` không có `needs` | `check` có `needs: pick`; `fire` giữ nguyên dòng `needs` | `start-run: only close-setup-broken writes issues, between the checks and the routine` | Giữ nguyên (dòng `needs` của `fire` và `close-setup-broken` không đổi) |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`. Test của script dùng `gh` giả (`scripts/tests/fake-gh`, route theo `FIXTURE`) và một kho git tạm có hai commit: commit A (trước) và commit B (sau), dựng như khối test `plan-queue` (biến `GIT_AUTHOR_*`, `GIT_CONFIG_GLOBAL=/dev/null` của bộ test). Hàm `asc_run <before> <after>` in stdout của script, stderr vào `$work/asc.err`, và `asc_code` in mã thoát. Danh sách thử viết bằng một hàm `write_run <id> <status> <start> <open_with>`.

| Điều cần đạt | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Danh sách vừa chuyển sang `approved`, đủ điều kiện | Test script | T1 `auto-start-check: a run list approved by this push with start auto is picked`: A có `RUN-20261201-01` `draft`, B có cùng file `approved`, `start: auto`, `open_with: routine`; route nhánh báo cáo trả 404, danh sách nhánh rỗng. Mong `docs/runs/RUN-20261201-01.md`. Đỏ trước code (script chưa có) |
| File mới thêm ở trạng thái `approved` | Test script | T2: A không có file, B có file `approved` đủ điều kiện. Mong đường dẫn. Đỏ trước code |
| Lần push sau không kích hoạt lại | Test script | T3 (đối chứng): A và B đều `approved` (B sửa một dòng chữ). Mong rỗng, stderr có `đã approved trước lần push này`. Qua sau code; trước code đỏ vì không có script, nên kèm T1 trong cùng lần chạy để thấy hướng |
| `start: manual` hay không có trường | Test script | T4: hai ca, mong rỗng cả hai, stderr có `start là manual` và `không ghi start` |
| `open_with` khác `routine` | Test script | T5: `open_with: cloud`, mong rỗng, stderr nêu `open_with` |
| Đã có nhánh báo cáo | Test script | T6: route `branches/claude%2FRUN-20261201-01-report` trả một nhánh. Mong rỗng, stderr `đã có nhánh báo cáo` |
| Có phiên khác đang chạy | Test script | T7: danh sách nhánh có `claude/RUN-20261130-01-report`, file trên đó `running`. Mong rỗng, stderr `Đang có phiên RUN-20261130-01 chạy`. Kèm ca đối chứng: file trên nhánh đó `finished` thì mong đường dẫn |
| Nhánh báo cáo của phiên khác ở trang 2 | Test script | T8: trang 1 có 100 nhánh khác, trang 2 có nhánh báo cáo `running`. Mong rỗng |
| `before` toàn số 0 hay không có trong kho | Test script | T9: `before` là `0000000000000000000000000000000000000000`, rồi một mã không có. Mong đường dẫn cả hai (coi như trước đó chưa có file) |
| Hai danh sách cùng đủ điều kiện | Test script | T10: B có `RUN-20261201-02` và `RUN-20261201-01` cùng đủ. Mong `docs/runs/RUN-20261201-01.md`, stderr nêu `RUN-20261201-02` |
| File không phải danh sách | Test script | T11: B đổi `docs/runs/README.md` và `docs/runs/patches/x.md`. Mong rỗng, mã 0 |
| GitHub lỗi | Test script | T12: route nhánh báo cáo trả lỗi 500 (`fake-gh` thoát 1). Mong mã 1, stdout rỗng |
| Sai tham số | Test script | T13: thiếu `--after`. Mong mã 2 |
| Quy tắc `start:` của `run-check.py` | Test script | T14, ba ca trên danh sách thử của khối `run-check` có sẵn: `start: soon` lỗi (`start` phải là auto hay manual); `start: auto` với `open_with: cloud` lỗi; `start: auto` với `open_with: routine` không có dòng lỗi mới. Hai ca đầu đỏ trước code |
| Cấu trúc workflow: trigger | Test script | T15 `start-run: runs on a push to main that changes a run list`: có khối `push:` với `branches: [main]` và `paths: ['docs/runs/RUN-*.md']`, vẫn có `workflow_dispatch` với `run_file`. Đỏ trước code |
| Cấu trúc workflow: chọn danh sách | Test script | T16: job `pick` gọi `scripts/auto-start-check.py --before "${BEFORE}"`, `BEFORE` qua `env`; không dòng `run:` nào chứa `github.event.before` trực tiếp; `check` có `needs: pick` và `if: needs.pick.outputs.run_file != ''`; `fire` đọc `needs.check.outputs.run_file`; không còn `inputs.run_file` ngoài bước đặt output của `pick`. Đỏ trước code |
| Cấu trúc workflow: quyền | Test script | T17: `actions: write` chỉ một lần (trong `fire`), `issues: write` chỉ một lần; `pick` không có khối `permissions` riêng. Qua trước code (đối chứng) |
| Cấu trúc workflow: chờ `main-guard` | Test script | T18: bước kiểm `main-guard` có `scripts/wait-run.sh --workflow main-guard.yml` với `--timeout 25` trong nhánh `push`; `check` có `timeout-minutes: 30`. Đỏ trước code |
| Không checkout nào để token | Test script | Câu mới của mục 3.5 |
| Tự chạy thật trên GitHub | Kiểm tra thủ công (người: cần merge thật lên nhánh chính và routine thật) | Sau merge: một danh sách thử `approved`, `start: auto`, một task nhỏ (hay danh sách rỗng mà phiên kết thúc ngay theo mục 2 của `auto-run.md`), merge bằng `merge-if-green.sh`; thấy đúng một lần chạy `start-run` do push, một phiên. Merge thêm một sửa chữ của cùng file: lần chạy `start-run` xanh, `pick` không chọn gì, không có phiên mới |
| Tài liệu đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc các đoạn sửa ở mục 3.1 D. `scripts/history-refs.py` qua |
| Toàn bộ | Test script | `scripts/tests/run.sh` trên CI (`pr-check`, rồi `main-guard` sau merge). Trên máy dev macOS: `source ~/.local/share/app-project-template/env.sh` trước; bản đầy đủ in dòng tổng kết, khoảng 1570 dòng `ok`; 6 test đỏ sẵn trên máy Mac (5 `pr-check: ... order check/filter`, 1 `wait-run: a draft PR`) |

**Ngoại lệ viết test trước:** tài liệu ở mục 3.1 D và chú thích đầu workflow: chỉ là chữ. Phần chờ `main-guard` trong workflow chỉ chạy được trên GitHub: test cấu trúc T18 kiểm có lệnh chờ và giới hạn, không chạy nó.

### 4.1. Chạy thử

Không cần (E dưới 2): trigger `push` có `paths` đã dùng ở `status-page.yml`; output giữa các job và `needs.<job>.outputs` là cách GitHub Actions thông dụng; script theo mẫu `resume-check.py`. Điểm duy nhất chưa dùng trong dự án là `github.event.before`, đã có phương án dự phòng ở mục 3.4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Plan sửa `.github/workflows/start-run.yml`, workflow mở phiên tự chạy. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một workflow, một script mới, một quy tắc của `run-check.py`, test, năm tài liệu |
| B. Độ rõ của yêu cầu | 1 | Yêu cầu rõ. Còn quyết định của người về ADR-0016 (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 1 | Script và quy tắc có test đầy đủ với `gh` giả; trigger và chờ `main-guard` của workflow chỉ kiểm cấu trúc, phần chạy thật kiểm sau merge |
| D. Ảnh hưởng chéo | 3 | Sai hướng thì mở phiên ngoài ý người, hay hai phiên cùng lúc; mỗi lần gọi API là một phiên và không chống trùng |
| E. Độ mới của kỹ thuật | 1 | `github.event.before` lần đầu dùng; có dự phòng |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `start-run.yml` (mọi phiên), `run-check.py` (mọi danh sách), mẫu danh sách chạy. `resume-run.yml` dùng chung nhóm `concurrency: start-run`, nên một lần chạy do push xếp hàng sau lần `resume-run` đang chạy, không chạy cùng lúc.
- Task khác có thể xung đột: ADR-0048 (`proposed`) sẽ sửa `docs/CLAUDE.md` mục Khi lập danh sách chạy, `docs/ops/pre-run-review.md`, `.claude/skills/run-list/SKILL.md` và thêm `run-check.py --batch`. Hai việc sửa các đoạn khác nhau của cùng file; việc làm sau tìm chỗ sửa theo tên mục, không theo số dòng.
- Đề xuất thứ tự chạy: làm sau khi ADR thay ADR-0016 được quyết (mục 8, câu 1). Không cần chờ ADR-0048.

## 7. Điều kiện dừng riêng

- Lần thử sau merge (dòng kiểm tra thủ công của mục 4) thấy một lần push mở hai phiên: dừng, người tắt `start: auto` ở mẫu (đổi thành `manual`) trong một PR, rồi xét lại.
- `github.event.before` và phương án dự phòng của mục 3.4 đều không cho được commit trước: dừng, không chuyển sang cách "mọi danh sách `approved` chưa có nhánh báo cáo", vì cách đó mở phiên ở mọi lần push đổi danh sách.

## 8. Câu hỏi cho người duyệt

1. **ADR-0016 ghi `start-run.yml` "chạy tay".** Plan này đổi điều đó, nên theo ADR-0044 cần một ADR hợp nhất thay ADR-0016 (chép mọi quy tắc của ADR-0016 về `staging.yml`, `start-run.yml` và bí mật, đổi dòng "chạy tay" thành "chạy tay, hoặc tự chạy khi lần push lên nhánh chính chuyển một danh sách `start: auto`, `open_with: routine` sang `approved`"), được chấp nhận trước khi duyệt plan. Đề xuất: AI viết ADR đó (`proposed`) trong một PR riêng; anh duyệt ADR rồi duyệt plan.
2. **Ai làm.** Ticket ghi `auto_run: false`: làm có người, một PR, vì task sửa chính workflow mở phiên và đẩy thay đổi `.github/workflows/` cần token có quyền `workflow` (phiên `routine` ghi `thử`). Đề xuất: giữ `auto_run: false`, AI làm cùng anh trên máy dev. Anh muốn chạy trong phiên tự chạy thì bỏ `auto_run: false` và danh sách chạy chạy `scripts/push-probe.sh` ở đầu phiên.
3. **Mặc định của mẫu.** Plan để mẫu ghi `start: auto`. Danh sách cũ không có trường thì không tự chạy. Đề xuất: giữ `auto`, vì mục tiêu là bớt bước bấm tay; muốn chọn giờ thì ghi `manual`.
4. **Khi có phiên khác đang chạy.** Plan không tự xếp hàng danh sách mới; dev bấm tay sau khi phiên kia xong. Đề xuất: giữ như vậy, vì mỗi lúc chỉ một phiên và trường hợp này hiếm.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ.
Đường làm nhanh (ADR-0034): tiêu đề "Làm nhanh YYYY-MM-DD, phiên bản plan N", ghi trong lần đẩy cuối của PR, sau rà soát độc lập và trước CI; Kết quả ghi "merge bằng PR này", Kết quả CI ghi "CI của PR này", bỏ dòng Cần người xem sáng nay, ghi việc cần người kiểm. -->

Chưa có.
