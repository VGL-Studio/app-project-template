---
ticket: T-095
title: Runner tự host: dòng lỗi của runner-check.sh vào build.log, issue session-stalled nói rõ job chờ runner quá lâu
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 5
risk_level: high
token_estimate: 7.4M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-095: RUNNER TỰ HOST: DÒNG LỖI CỦA RUNNER-CHECK.SH VÀO BUILD.LOG, ISSUE SESSION-STALLED NÓI RÕ JOB CHỜ RUNNER QUÁ LÂU

## 1. Tóm tắt

**`build.log`.**
- `scripts/runner-check.sh` thêm biến `RUNNER_CHECK_LOG`: có biến thì script ghi thêm các dòng kết quả vào tệp đó, sau khi đã kiểm xong.
- Bước kiểm runner của `pr-check.yml` đặt `RUNNER_CHECK_LOG: build.log`. Nhờ vậy bình luận lỗi trên PR có dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` thay vì "Không có log.".
- Nối `| tee -a build.log` vào lệnh của bước thì không được: `tee` tạo `build.log` trong thư mục làm việc ngay lúc lệnh bắt đầu, trong khi `runner-check.sh` đòi thư mục đó không có file ngoài git.

**Job chờ runner.**
- `scripts/run-watchdog.py`: khi mở hay cập nhật issue `session-stalled`, script xem có job nào chờ runner tự host quá 75 phút không. Ngưỡng 75 phút và quy tắc tính là của `wait-run.sh` mã 7.
- Có thì nội dung issue thêm một đoạn: người sửa máy runner trước, chưa dán lời gọi tiếp tục phiên.
- `resume-check.py` không đổi: nó đã không tiếp tục phiên khi có job chờ.

Kiểm bằng test của `scripts/tests/run.sh`, có đối chứng.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn là hai ghi chú rà soát T-078 trong báo cáo RUN-20261108-01. Anh Nhật chọn ngày 2026-11-08: "một ticket backlog cho build.log và queued".

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| 1. `build.log` của `pr-check.yml` | Bình luận lỗi trên PR có dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` khi bước kiểm runner đỏ | Bước `Bình luận lỗi vào PR` in `tail -n 60 build.log`. Chỉ cần các dòng của `runner-check.sh` vào `build.log` mà không làm hỏng lần kiểm "không có file ngoài git" của chính script |
| 1. `main-guard.yml` | Plan nói rõ có làm cùng cách không | Không làm. `build.log` của `main-guard.yml` chỉ vào nội dung issue `main-broken`, mà issue đó chỉ mở khi bước `check` đỏ (`if: failure() && steps.check.outcome == 'failure'`). Bước kiểm runner đỏ thì lần chạy dừng trước `check`, không mở issue nào, nên không có chỗ nào in `build.log`. Người thấy lần chạy đỏ trên tab Actions, dòng lỗi nằm trong log của bước |
| 2. Job `queued` quá ngưỡng | Có xử lý job `queued` quá ngưỡng, kèm test đối chứng | Ticket ghi `resume-check.py` và để plan chọn mở issue mới hay dùng `session-stalled`. Plan chọn `session-stalled`: `run-watchdog.py` đã mở issue này khi phiên không có commit 60 phút. Khi phiên dừng ở mã 7, đó đúng là lúc nó mở, nhưng nội dung hiện chỉ bảo dán lời gọi tiếp tục phiên, và phiên mới sẽ dừng lại đúng chỗ đó. `resume-check.py` đã đúng hành vi (không tiếp tục khi có job chờ), nên không đổi. Đây là một lệch so với chữ của ticket: câu hỏi 1 ở mục 8 |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan:
- ADR-0042: runner tự host, `runner-check.sh`, mã 7.
- ADR-0043: `ui-test` của `main-guard`.

Bài học L-109 (`run-watchdog.py`) được nhắc làm bối cảnh.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/runner-check.sh`: biến `RUNNER_CHECK_LOG`.**
- `refuse` và hai dòng `RUNNER DÙNG ĐƯỢC` vẫn in ra stdout như nay, và gom thêm vào một biến `report`.
- Một hàm `finish <mã>` chạy ở cả hai lối ra (`exit 1` khi có vấn đề, thoát 0 khi dùng được):
  - `RUNNER_CHECK_LOG` khác rỗng thì `printf '%s' "$report" >> "$RUNNER_CHECK_LOG"`;
  - ghi lỗi thì in `CHÚ Ý: không ghi được $RUNNER_CHECK_LOG` ra stderr, mã thoát giữ nguyên;
  - rồi `exit <mã>`.
- Lần ghi đứng sau mọi điểm kiểm (sau `git status --porcelain --ignored`). Tệp tương đối như `build.log` khi đó không làm script tự từ chối.
- Lỗi cách dùng (mã 2) không ghi gì.
- Chú thích đầu file thêm biến mới.

**B. `.github/workflows/pr-check.yml`, bước `Kiểm runner tự host (scripts/runner-check.sh)`.**
- `env:` thêm `RUNNER_CHECK_LOG: build.log`.
- Chú thích trên bước thêm câu: các dòng kết quả vào `build.log`, mà bước bình luận lỗi in ra.
- `run:` giữ nguyên `scripts/runner-check.sh`. Test thứ tự bước có sẵn (`runner_order`) vẫn đòi đúng chữ đó.
- `main-guard.yml` không đổi (mục 2).

**C. `scripts/run-watchdog.py`: job chờ runner tự host.**
- Hằng mới `RUNNER_QUEUE_MINUTES = int(os.environ.get("WATCHDOG_RUNNER_QUEUE_MINUTES", "75"))`. 75 là mặc định của `WAIT_RUN_RUNNER_QUEUE_MINUTES` ở `wait-run.sh`.
- Hàm mới `runner_waits(now)`:
  - đọc `actions/runs?status=queued&per_page=100` và `actions/runs?status=in_progress&per_page=100` (cùng hai đường mà `busy_workflows` đọc), rồi `actions/runs/<id>/jobs?per_page=50` của từng lần chạy;
  - bỏ qua lần chạy có job `in_progress`;
  - còn lại, `since` là `completed_at` muộn nhất của các job đã xong, không có thì `created_at` của job;
  - lấy job `queued`, không có `runner_name`, có nhãn không khớp `^(ubuntu|windows|macos)-`, mà `now - since` từ `RUNNER_QUEUE_MINUTES` trở lên. Đây đúng quy tắc của `wait-run.sh` mã 7;
  - trả danh sách `(tên job, mã lần chạy, since)`.
  
  Đáp không đúng dạng (không có `workflow_runs` hay `jobs` dạng danh sách) là `LookupError`, như `resume-check.py`.
- Trong `main()`, `runner_waits` chỉ chạy khi script sắp mở hay cập nhật issue: sau hai lần lọc "chưa đủ 60 phút" và "đang có workflow chạy, chưa tới 90 phút". Nhờ vậy lần chạy bình thường không đọc thêm. Lần đọc này nằm trong vòng đọc, trước mọi lần ghi, nên đọc lỗi thì thoát 1 và không đổi issue nào, như nay.
- `body_for` nhận thêm `waits`. Khác rỗng thì ngay sau đoạn đầu (trước "Người làm: ...") có thêm một đoạn, mỗi job một dòng. Chữ dự kiến:
  - `**Job chờ runner tự host:** job \`<tên>\` (lần chạy <mã>) chờ runner tự host từ <since, YYYY-MM-DD HH:MM UTC>, quá <RUNNER_QUEUE_MINUTES> phút, không còn job nào của lần chạy đó đang chạy.`
  - Rồi một câu chung: `Phiên dừng vì máy runner, không vì code (\`scripts/wait-run.sh\` mã 7): người phụ trách hạ tầng xem máy runner hay biến \`HEAVY_RUNNER\` trước (docs/ops/self-hosted-runner.md, mục 8). Job còn chờ thì chưa dán đoạn dưới: phiên mới cũng dừng ở đó. Job đã chạy thì phiên routine được \`resume-run.yml\` tự tiếp tục; phiên \`claude --cloud\` thì dán đoạn dưới.`
- Tiêu đề issue, nhãn, lúc mở, lúc đóng không đổi.
- Chú thích đầu file thêm `WATCHDOG_RUNNER_QUEUE_MINUTES` và đoạn mới.

**D. `docs/ops/self-hosted-runner.md`, mục 7.**
- Dòng "Issue `session-stalled` có sẵn lệnh tiếp tục phiên ...": thêm vế "issue ghi sẵn job chờ runner quá 75 phút, nếu có (`scripts/run-watchdog.py`)". Việc xem hàng chờ bằng `gh api` giữ cho job chờ dưới ngưỡng.
- Dòng "Job chờ máy": thêm câu: bước kiểm runner của `pr-check` đỏ thì bình luận lỗi trên PR có các dòng `RUNNER KHÔNG DÙNG ĐƯỢC:`; của `main-guard` thì xem log của bước trên tab Actions.

**E. `scripts/tests/run.sh`, hàm phụ trợ.** Hai thay đổi vào commit test của bước 3, để test viết trước đỏ vì script, không vì phần dựng:
- `rc` thêm `-u RUNNER_CHECK_LOG` vào `env -u ...` và biến `rc_log` (đặt `RUNNER_CHECK_LOG` khi có);
- `watchdog` thêm biến `WD_RUNS` (phần tử JSON thêm vào danh sách `queued` hay `in_progress`, có `id`) và `WD_ROUTES` (route thêm, mở đầu bằng dấu phẩy), như `RESUME_ROUTES` của `resume_check`.

Mẫu có sẵn:
- `waiting_job` của `resume-check.py`;
- truy vấn jq của `wait-run.sh` mã 7, viết lại bằng python;
- `busy_workflows` và `body_for(note)` của `run-watchdog.py`;
- hàm `rc` và `watchdog` của test.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/runner-check.sh` | Mục A: `RUNNER_CHECK_LOG`, hàm `finish`, chú thích |
| Sửa | `.github/workflows/pr-check.yml` | Mục B: `RUNNER_CHECK_LOG: build.log` trong `env:` của bước kiểm runner, chú thích |
| Sửa | `scripts/run-watchdog.py` | Mục C: `RUNNER_QUEUE_MINUTES`, `runner_waits`, đoạn mới của `body_for`, chú thích |
| Sửa | `scripts/tests/run.sh` | Mục E và các test của mục 4 |
| Sửa | `docs/ops/self-hosted-runner.md` | Mục D: hai dòng của mục 7 |

Nơi đọc các file bị đổi (`grep -rn 'runner-check\|run-watchdog\|self-hosted-runner.md' scripts/ .github/ .claude/ docs/*.md docs/ops/`):
- `main-guard.yml` gọi `runner-check.sh` và `runner-check.sh --emulator` mà không đặt `RUNNER_CHECK_LOG`, nên chạy như nay (test R3).
- `scripts/main-guard-verified.sh` có `runner-check.sh` và `pr-check.yml` trong `GATE_FILES`, nên commit merge chạy `full`; không sửa.
- `run-watchdog.yml` gọi `run-watchdog.py` không tham số và đã có `actions: read`, đủ để đọc `jobs`; không sửa.
- `docs/auto-run.md`, mục 4, dòng "Job chờ runner tự host" nói việc của phiên, không đổi.
- `docs/ops/staging-and-runs.md`, mục 2.7, có lời gọi mà issue chép lại, không đổi.

Các file khác mà `plan-check.py` (L-020) thấy có nhắc tới file của plan; không file nào bị ảnh hưởng, không sửa:
- `.github/workflows/main-guard.yml`: gọi `scripts/runner-check.sh` không đặt biến mới, nên chạy như nay (R3). Chạy `scripts/tests/run.sh`, nhắc `pr-check.yml` trong chú thích.
- `.github/workflows/run-watchdog.yml`: gọi `scripts/run-watchdog.py` không tham số, đã có `actions: read`; tiêu đề và nhãn issue không đổi.
- `scripts/ui-test-needed.sh`:
  - có `scripts/runner-check.sh` trong danh sách file làm `ui-test` chạy, nên commit merge chạy cả `ui-test`; đúng như mong;
  - nhắc `pr-check.yml` trong chú thích.
- `scripts/merge-if-green.sh`: nhắc `pr-check.yml` (tên job) và `scripts/tests/run.sh` trong chú thích; tên job không đổi.
- `scripts/tests/playbook/PB-02.log`, `scripts/tests/playbook/PB-17.log`, `scripts/tests/playbook/PB-19.log`: dòng mẫu của bảng tra lỗi (`HẠ TẦNG`, `CHỜ RUNNER`, `RUNNER KHÔNG DÙNG ĐƯỢC`). Plan không đổi chữ của các dòng đó.
- `scripts/local-setup.sh`: nhắc `scripts/tests/run.sh` trong chú thích (chỗ test nối vào).
- `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt`: nhắc `scripts/tests/run.sh` trong chú thích dòng 87.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm (quyền của Android). Quyền của workflow cũng không thêm: `run-watchdog.yml` đã có `actions: read`, `pr-check.yml` không đổi `permissions`.
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Chỉ dùng mẫu có sẵn.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Bảng ghi các câu `expect` có sẵn mà phần đổi chạm tới; không câu nào phải viết lại.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Bước kiểm runner của `pr-check` có `run: scripts/runner-check.sh`, `env` có `REPO_VISIBILITY` | Như cũ, `env` thêm một dòng | `runners: the order check passes on pr-check ...`, `runners: every heavy job checks the runner right after checkout` | Giữ: `runner_order` so chữ `run` và tìm dòng `REPO_VISIBILITY` trong `env`, không đòi `env` chỉ có một dòng |
| `runner-check.sh` in kết quả ra stdout, mã 0, 1, 2 | Như cũ khi không có biến | Các câu `runner-check:` | Giữ; `rc` bỏ `RUNNER_CHECK_LOG` của máy (mục E) |
| Issue `session-stalled` có lời gọi, câu về `resume-run.yml`, ghi chú 24 giờ | Như cũ khi không có job chờ quá ngưỡng | Các câu `run-watchdog:` (dòng 2534 tới 2600 hôm nay) | Giữ: fixture có sẵn không có lần chạy nào có `id`, nên script không đọc `jobs` |
| `run-watchdog` thoát 1 và không đổi issue khi đọc lỗi | Như cũ, thêm lần đọc `jobs` | `a server error on the workflow runs exits 1` | Giữ; thêm W7 |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`. Tên test là chuỗi đầu của `expect`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| A. Ghi kết quả vào tệp | Script test | R1 `runner-check: RUNNER_CHECK_LOG gets the same result lines`. (a) `rc_reset`, `rc_log="$work/rc.log"`, runner dùng được: mong `0 0`, và `rc.log` giống hệt stdout (`cmp`), một dòng `RUNNER DÙNG ĐƯỢC`. (b) `rc_vis=public rc_eph=''`: mong `1 2`, và `rc.log` có đúng 2 dòng `RUNNER KHÔNG DÙNG ĐƯỢC:`, giống stdout. (c) Tệp đã có một dòng `x` thì giữ dòng đó ở đầu (ghi nối thêm). Trên code hiện tại không có tệp: đỏ |
| A. Tệp trong thư mục làm việc không làm script tự từ chối | Script test | R2 `runner-check: a RUNNER_CHECK_LOG inside the work directory is written after the checks`. `rc_reset`, `rc_log=build.log` (đường dẫn tương đối, trong `$rc_repo`): mong `0 0`, và `$rc_repo/build.log` có dòng `RUNNER DÙNG ĐƯỢC`. Gọi lần hai (tệp đã có, nằm ngoài git): mong `1 1`, và dòng `file ngoài git: build.log`, đúng như phải thế với một container không sạch. Lần đầu trên code hiện tại không có tệp: đỏ |
| A. Đối chứng: không có biến | Script test | R3 `runner-check: without RUNNER_CHECK_LOG nothing is written`. `rc_reset`, không đặt `rc_log`: mong `0 0`, và `git -C "$rc_repo" status --porcelain --ignored` rỗng. Qua ngay; đứng cho `main-guard.yml` |
| A. Lỗi cách dùng không ghi | Script test | R4 `runner-check: a usage error writes no log`. `rc_log="$work/rc2.log"`, gọi với tham số `--x`: mong mã 2, không có `rc2.log`. Qua ngay; đối chứng của hàm `finish` |
| B. Workflow đặt biến | Script test | P1 `pr-check: the runner check writes its result lines to build.log`. Trong khối `env:` của bước `Kiểm runner tự host (scripts/runner-check.sh)` thuộc `build-and-test` có đúng một dòng `          RUNNER_CHECK_LOG: build.log`; trong `main-guard.yml` không có dòng nào. Mong `1 0`. Đối chứng: bản chép bỏ dòng đó, mong `0`. Trên workflow hiện tại `0 0`: đỏ |
| 1. Bình luận lỗi có dòng của runner (định nghĩa hoàn thành 1) | Script test | P2 `pr-check: the failure comment shows the lines of a refused runner`. Trong một thư mục tạm, `runner-check.sh` thật chạy với `RUNNER_CHECK_LOG=build.log`, `REPO_VISIBILITY=public`, `VGL_RUNNER_EPHEMERAL=1`: mong mã 1. Rồi chạy khối `run` của bước `Bình luận lỗi vào PR` lấy từ `pr-check.yml`: `${{ steps.release.outcome }}` thay bằng rỗng, `${{ github.event.pull_request.number }}` bằng `1`, `${{ github.repository }}` bằng `o/r`, và một `gh` giả thoát 0. `comment.md` mong có một dòng `RUNNER KHÔNG DÙNG ĐƯỢC: repo không phải private` và không có `Không có log.`. Trên code hiện tại `build.log` không có, nên `comment.md` có `Không có log.`: đỏ (đỏ ở phần script, không ở workflow, vì P2 đặt biến trực tiếp) |
| 2. Issue nói rõ job chờ runner | Script test | W1 `run-watchdog: a job waiting for a self-hosted runner past 75 minutes is named in the issue`. `watchdog running 70` có `WD_RUNS`: lần chạy `{"id": 120, "name": "pr-check", "head_branch": "claude/T-062-x", "event": "pull_request", "created_at": <200 phút trước>}` trong `queued`. `WD_ROUTES`: `actions/runs/120/jobs?per_page=50` có job `build-and-test`, `queued`, `runner_name` null, nhãn `["vgl-android"]`, `created_at` 100 phút trước. Mong `POST issues`, đầu ra có `Job chờ runner tự host:`, ``job `build-and-test` (lần chạy 120)`` và `quá 75 phút`. Lần chạy tạo trước commit cuối, nên `busy_workflows` không giữ issue lại. Trên code hiện tại không có đoạn đó: đỏ |
| 2. Đối chứng dưới ngưỡng | Script test | W2 `run-watchdog: a job queued 30 minutes ago is not named`. Như W1, job tạo 30 phút trước: mong `POST issues`, 0 dòng `Job chờ runner tự host`. Qua ngay; sau khi sửa vẫn phải qua |
| 2. Đối chứng runner của GitHub | Script test | W3 `run-watchdog: a job waiting for a GitHub runner is not named`. Như W1, nhãn `["ubuntu-latest"]`: mong 0 dòng. Qua ngay |
| 2. Đối chứng: lần chạy còn job đang chạy | Script test | W4 `run-watchdog: a run with a job in progress is not named`. Như W1, thêm job `guard` `in_progress`: mong 0 dòng. Qua ngay |
| 2. Tính giờ từ job xong muộn nhất | Script test | W5 `run-watchdog: the wait counts from the latest finished job of the run`. Như W1, thêm job `guard` `completed`, `completed_at` 20 phút trước: mong 0 dòng. Lần hai với `completed_at` 80 phút trước: mong 1 dòng. Mong `0 1`. Trên code hiện tại `0 0`: đỏ ở lần hai |
| 2. Ngưỡng đặt được | Script test | W6 `run-watchdog: WATCHDOG_RUNNER_QUEUE_MINUTES sets the wait`. Dữ liệu của W2 với `WATCHDOG_RUNNER_QUEUE_MINUTES=20`: mong 1 dòng. Trên code hiện tại 0: đỏ |
| 2. Đọc lỗi không đổi issue | Script test | W7 `run-watchdog: a server error on the jobs of a run exits 1 and opens no issue`. Như W1, `errors` có `actions/runs/120/jobs?per_page=50: HTTP 500`. Mong `1 none`. Trên code hiện tại script không đọc `jobs`, mở issue: `0 POST issues`, đỏ |
| 2. Issue đang mở được cập nhật | Script test | W8 `run-watchdog: an open issue gets the waiting job on update`. Như W1, có issue 41 đang mở (`wd_open`): mong `PATCH issues/41` và 1 dòng `Job chờ runner tự host:`. Trên code hiện tại 0 dòng: đỏ |
| 2. Phiên chưa im không đọc thêm | Script test | W9 `run-watchdog: a session with a recent commit reads no jobs`. `watchdog running 30` với dữ liệu của W1 và `errors` của W7: mong `0 none` (không đọc `jobs`, nên lỗi không lộ). Qua ngay; sau khi sửa vẫn phải qua, giữ điều "chỉ đọc khi sắp ghi" |
| Toàn bộ | CI | `scripts/check.sh` qua trên PR. Sau merge, `main-guard` của commit merge chạy `full` (đổi `pr-check.yml`, `runner-check.sh`, `scripts/tests/run.sh`, đều trong `GATE_FILES`) và xanh |
| Trên runner tự host thật | Kiểm tra thủ công (máy thật: khác, cần một máy runner tự host và biến `HEAVY_RUNNER`, chưa có ở repo nào; cùng nhóm H1 tới H6 của T-078) | Khi dựng máy theo `docs/ops/self-hosted-runner.md`, mục 6: đặt `VGL_RUNNER_EPHEMERAL` sai, đẩy một PR code, xem bình luận lỗi trên PR có dòng `RUNNER KHÔNG DÙNG ĐƯỢC:`. Ở bước 5 của mục đó (máy tắt): sau 60 phút không commit, issue `session-stalled` có đoạn `Job chờ runner tự host:` |

Thứ tự trong commit test (bước 3):
1. phần dựng (mục E);
2. R1 tới R4, P1, P2, W1 tới W9.

Đỏ đúng lý do mong ở: R1, R2, P1, P2, W1, W5, W6, W7, W8.

Qua ngay, như bảng đã ghi (đối chứng): R3, R4, W2, W3, W4, W9.

**Ngoại lệ viết test trước:** hai dòng của `docs/ops/self-hosted-runner.md` và các chú thích. Chúng chỉ là tài liệu, không có hành vi để test.

### 4.1. Chạy thử

Không cần (E dưới 2).

AI đã đối chiếu trên nhánh chính `f95cc92` (2026-11-08):
- `runner-check.sh` kiểm `git status --porcelain --ignored`, có test `an ignored or an untracked file in the work directory is refused`. Vì vậy cách `| tee -a build.log` làm script từ chối chính thư mục của nó: `tee` mở tệp song song với lệnh.
- Bước `Đánh dấu nhánh chính đang đỏ` của `main-guard.yml` chỉ chạy khi `steps.check.outcome == 'failure'`.
- `run-watchdog.yml` đã có `actions: read`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `.github/workflows/pr-check.yml`, `scripts/runner-check.sh`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một dòng của workflow, hai script nhỏ, test, một tài liệu vận hành |
| B. Độ rõ của yêu cầu | 1 | Ticket rõ; plan chọn `run-watchdog.py` thay cho `resume-check.py` (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi nhánh có test; bình luận trên PR thật và issue thật chỉ thấy khi có runner tự host |
| D. Ảnh hưởng chéo | 2 | Mọi PR đi qua `pr-check.yml`, nhưng bước đổi chỉ chạy trên runner tự host, chưa repo nào dùng. `run-watchdog` chạy trong mọi phiên |
| E. Độ mới của kỹ thuật | 0 | Mẫu có sẵn |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm:
  - `pr-check.yml`: mọi PR, nhưng bước đổi chỉ chạy khi `runner.environment == 'self-hosted'`;
  - `runner-check.sh`: không đổi gì khi không có biến;
  - `run-watchdog.py`: mọi phiên tự chạy, chỉ thêm chữ vào issue khi có job chờ quá ngưỡng.
- Task khác có thể xung đột:
  - T-094 (`backlog`, plan soạn cùng ngày) sửa `scripts/tests/run.sh` ở phần `merge-if-green`, `main-guard-verified`, `pr_sim`, `pr_filter_check`, khác chỗ với plan này. `run-check.py` sẽ báo chú ý vì chung file.
  - Không plan nào khác đang sửa `pr-check.yml`, `runner-check.sh` hay `run-watchdog.py`.
- Cách chạy:
  - Phiên tự chạy. Plan sửa `.github/workflows/pr-check.yml`, nên phiên cần `workflow-push` (`docs/ops/session-capabilities.md`).
  - Danh sách chạy ghi cách mở, và phiên chạy `scripts/push-probe.sh` ở đầu phiên.
- Các app nhận qua đợt đồng bộ: workflow, script và test là của dự án mẫu.
- Đề xuất thứ tự: độc lập với T-094.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu nhánh chính với mục 4.1. Dừng, cần plan phiên bản mới, khi:
  - bước kiểm runner của `pr-check.yml` đã không còn `run: scripts/runner-check.sh`;
  - bước bình luận lỗi không còn đọc `build.log`;
  - `run-watchdog.py` đã không còn `body_for` hay `busy_workflows`.
  
  Chỉ lệch số dòng thì theo ADR-0032/L3.
- Cần thêm quyền cho một workflow, sửa `main-guard.yml`, `resume-run.yml`, `run-watchdog.yml`, hay đổi tiêu đề, nhãn, lúc mở, lúc đóng của issue `session-stalled`: dừng.
- Một câu `runners:` hay `run-watchdog:` có sẵn đổi kết quả: dừng.

## 8. Câu hỏi cho người duyệt

1. **Chỗ báo job chờ runner quá lâu.** Ticket ghi `resume-check.py` và để plan chọn mở issue mới hay dùng `session-stalled`.
   - (a) `run-watchdog.py` thêm đoạn vào issue `session-stalled` mà nó đã mở, như mục 3.1 C. Không thêm issue hay quyền mới. Đề xuất: (a).
   - (b) `resume-check.py` in một dòng riêng, `resume-run.yml` mở issue mới (nhãn mới, cần `issues: write`). Khi đó có hai workflow cùng viết issue về một phiên.

   Lý do chọn (a): khi phiên dừng ở mã 7, `run-watchdog.py` đã mở `session-stalled` sau 60 phút, chỉ thiếu lý do; `resume-check.py` đã làm đúng. Chọn (a) thì ý 2 của định nghĩa hoàn thành đọc là "`run-watchdog.py` xử lý job `queued` quá ngưỡng, có test kèm đối chứng". AI sửa dòng đó của ticket khi anh duyệt.
2. **Ngưỡng 75 phút.** Lấy theo `wait-run.sh` mã 7. Một biến riêng (`WATCHDOG_RUNNER_QUEUE_MINUTES`) cho phép chỉnh mà không đổi `wait-run.sh`. Đề xuất: giữ 75.
3. **`main-guard.yml` không đổi** (mục 2): bước kiểm runner đỏ không mở issue `main-broken`, nên không có chỗ in `build.log`. Đề xuất đồng ý. Muốn có issue cho trường hợp đó thì làm ticket riêng.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
