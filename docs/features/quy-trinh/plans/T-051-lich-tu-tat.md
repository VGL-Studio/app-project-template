---
ticket: T-051
title: Lịch định kỳ không cấp runner khi không có phiên tự chạy
status: draft
version: 1
risk_score:
risk_level: high
token_estimate: 23.9M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-051: LỊCH ĐỊNH KỲ KHÔNG CẤP RUNNER KHI KHÔNG CÓ PHIÊN TỰ CHẠY

## 1. Tóm tắt

`resume-run.yml` và `run-watchdog.yml` tự tắt mình (REST `disable`) khi không có phiên `running`, không có danh sách chạy vừa duyệt chờ bắt đầu, và không vừa được bật. Ba nơi bật lại (REST `enable`): `status-page.yml` khi một danh sách `approved` lên nhánh chính, `start-run.yml` trước khi kích hoạt routine, và chính phiên ở đầu phiên. Phần quyết định nằm trong script mới `scripts/schedule-idle.py`, kiểm bằng test của `scripts/tests/run.sh` với `gh` giả. Lúc rảnh, mỗi lịch chỉ còn vài lần chạy sau khi phiên xong, thay vì khoảng 2.160 phút mỗi tháng của hai lịch (L-126).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-126 (issue #275) và quyết định của anh Nhật ngày 2026-10-29: phương án A (tự tắt, phiên bật lại), không thêm bí mật, làm trong phiên có người; danh sách chạy `approved` chưa bắt đầu cũng giữ lịch, và `status-page.yml` bật lại lịch khi danh sách như vậy lên nhánh chính (quyết định sau rà soát độc lập).

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | Khi không có phiên `running` thì hai lịch không cấp runner | Lần chạy đầu tiên thấy rảnh thì tắt workflow của chính nó; GitHub không chạy lịch và không nhận `workflow_dispatch` tới khi được bật lại |
| Định nghĩa hoàn thành 2 | Khi có phiên `running` thì hai lịch làm như cũ | Có phiên `running` thì không tắt. Mọi cách mở phiên đều có lịch bật: danh sách `approved` lên nhánh chính thì `status-page.yml` bật (che Run now, hẹn giờ, `claude --cloud`, `local`); `start-run.yml` bật lại; phiên bật lại ở đầu phiên |
| Định nghĩa hoàn thành 3 | Tài liệu ghi cách làm mới | Mục 3.1, phần Tài liệu |

**Các đường mở phiên** (`docs/auto-run.md`, mục 1; `docs/ops/session-capabilities.md`):

| Đường | Ai bật lịch trước khi phiên cần |
| --- | --- |
| Duyệt danh sách (merge vào nhánh chính) rồi `start-run.yml` | `status-page.yml` lúc merge; `start-run.yml` lúc kích hoạt; phiên ở đầu phiên |
| Duyệt rồi Run now hay hẹn giờ của routine (có thể nhiều giờ sau) | `status-page.yml` lúc merge; lịch giữ tới 72 giờ vì có danh sách `approved` chưa bắt đầu; phiên ở đầu phiên |
| Duyệt rồi `claude --cloud` hay `local` | Như trên |
| Phiên hết hạn mức giữa chừng, `resume-run` phải kích hoạt lại | Lịch đang bật vì có phiên `running` |
| Phiên tiếp tục sau khi người mở lại | Phiên chạy lại `--enable` (mục 4 của `docs/auto-run.md`) |

**Khe tranh chấp.** Lịch đọc "rảnh" đúng lúc một phiên vừa đẩy nhánh báo cáo `running`. Che bằng ba lớp: mốc `updated_at` của workflow mới trong `GRACE_HOURS` (3) thì giữ (lệnh `enable` cập nhật mốc này, mục 4.1); lần chạy gần nhất của `start-run.yml` mới trong `GRACE_HOURS` thì giữ; sau khi tắt, script đọc lại, thấy phiên `running` hay danh sách chờ thì bật lại ngay.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0032 (PB-14, quyền `workflow` khi đẩy file workflow). Không đổi quy tắc của ADR nào.

## 3. Giải pháp

### 3.1. Cách làm

**Script `scripts/schedule-idle.py`** (Python, chỉ `gh api` REST với `-X PUT` cho lệnh ghi, cùng kiểu với `scripts/resume-check.py`: `api`, `api_all`, `front_matter`, `moment`, `GITHUB_REPOSITORY` hay remote `origin`). Hai workflow bật tắt là `resume-run.yml` và `run-watchdog.yml` (hằng `SCHEDULES`).

`busy()` trả lý do giữ lịch, hay `None`:
1. Phiên `running`: nhánh `claude/RUN-YYYYMMDD-NN-report` có `docs/runs/<RUN>.md` với `status: running`. Đọc file trả 404 thì bỏ qua nhánh đó (như `resume-check.py`); lỗi đọc khác thì coi như bận (giữ lịch).
2. Danh sách chờ: `docs/runs/*.md` trên nhánh chính (`contents/docs/runs?ref=main`) có `status: approved`, chưa có nhánh báo cáo, và commit gần nhất của file trên nhánh chính (`commits?path=docs/runs/<RUN>.md&sha=main&per_page=1`) mới trong `PENDING_HOURS` (72).

Các chế độ:

- `--disable-if-idle <tệp workflow>`:
  1. `busy()` có lý do: in "giữ: <lý do>", thoát 0.
  2. `updated_at` của workflow mới trong `GRACE_HOURS`: in "giữ: vừa được bật lúc ...", thoát 0. Không đọc được thì coi như cũ.
  3. Lần chạy gần nhất của `start-run.yml` (`actions/workflows/start-run.yml/runs?per_page=1`) mới trong `GRACE_HOURS`: in "giữ: start-run vừa chạy", thoát 0. Không có lần chạy nào (app mới) thì bỏ qua bước này.
  4. `PUT actions/workflows/<tệp>/disable`. Lỗi: in CHÚ Ý, thoát 0.
  5. Đọc lại `busy()`. Có lý do, hay đọc lỗi: `PUT .../enable`, in "bật lại: <lý do>". Không: in "đã tắt: không có phiên".
  - Không đọc được GitHub ở bước 1 tới 3: không tắt, in "Không đọc được GitHub", thoát 0.
  - Mọi lỗi khác (dữ liệu thiếu trường) được bắt ở `main` như lỗi đọc: không tắt, thoát 0. Workflow không bao giờ đỏ vì script này.
- `--enable`: `PUT .../enable` cho từng workflow trong `SCHEDULES`, rồi đọc lại `state`. In một dòng mỗi workflow. Thoát 0 khi cả hai `active`, 1 khi có cái lỗi hay không `active` (thông báo nêu tên workflow).
- `--enable-if-pending`: `busy()` có lý do thì như `--enable`; không thì in "không có phiên hay danh sách chờ", thoát 0.
- `SCHEDULE_IDLE_NOW` (giờ ISO), `SCHEDULE_IDLE_GRACE_HOURS`, `SCHEDULE_IDLE_PENDING_HOURS` thay mặc định (test).

**Workflow:**

- `resume-run.yml`: `permissions` thêm `actions: write`; checkout thêm `persist-credentials: false` (job không đẩy gì). Bước cuối "Tắt lịch khi không có phiên": `if: always()`, `env: GH_TOKEN: ${{ github.token }}`, chạy `python3 -I scripts/schedule-idle.py --disable-if-idle resume-run.yml | tee -a "${GITHUB_STEP_SUMMARY}"`. Sửa chú thích đầu file.
- `run-watchdog.yml`: `actions: read` thành `actions: write`. Bước cuối như trên với `run-watchdog.yml`, sau bước của `run-watchdog.py` (issue `session-stalled` của danh sách không còn `running` được đóng trước), `if: always()`, `GH_TOKEN`. Sửa chú thích đầu file (bỏ "48 a day", "Disable workflow"; ghi cách tự tắt).
- `start-run.yml`: job `fire` thêm `permissions: actions: write` và bước "Bật lịch của phiên" trước bước kích hoạt, `env: GH_TOKEN: ${{ github.token }}`: hai lệnh `gh api -X PUT "repos/${GITHUB_REPOSITORY}/actions/workflows/<tệp>/enable"`, lỗi thì `::warning::` và đi tiếp. Job không checkout nên không gọi script.
- `status-page.yml`: thêm job `schedules` (độc lập với job `page`, `permissions: actions: write, contents: read`, checkout `persist-credentials: false`), chạy `python3 -I scripts/schedule-idle.py --enable-if-pending` với `GH_TOKEN`. Job `page` giữ nguyên quyền `contents: write`. Sửa chú thích đầu file. `concurrency` có `cancel-in-progress: true`: lần chạy mới cũng bật, nên hủy lần cũ không mất gì.

**Phiên:** `docs/auto-run.md` mục 2, dòng "`resume-run.yml` và `run-watchdog.yml` đang bật" (thêm ở #283) thay bằng: sau khi đẩy nhánh báo cáo `running`, chạy `scripts/schedule-idle.py --enable`. Mã 0: ghi vào Kiểm tra đầu phiên. Mã 1: ghi vào Kiểm tra đầu phiên và một dòng `- [ ]` ở Cần người kiểm tra, làm tiếp; phiên không tìm cách khác. Mục 4 (Tiếp tục phiên) cũng chạy lại lệnh này.

**Tài liệu:**

- `docs/auto-run.md`: mục 2 như trên; mục 4, đoạn Phiên đứng im (dòng "App không cần thì tắt: ... Disable workflow") thành "lịch tự tắt khi không có phiên; app không cần thì xóa file"; đoạn "Muốn dừng hẳn một phiên đang chờ" bỏ "Hoặc tắt workflow `resume-run`" (lần duyệt hay `start-run` sau sẽ bật lại, và `resume-run` tiếp tục danh sách cũ còn `running`): chỉ sửa danh sách sang `finished`.
- `docs/ops/staging-and-runs.md`: mục 2.3 bỏ dòng bật tay; mục 2.6 sửa hai câu "Disable workflow" như trên, thêm "workflow đang tắt thì bật trước rồi mới Run workflow"; mục 2.7 ghi issue `session-stalled` có khi lịch đang bật (danh sách duyệt hay phiên đã bật); mục 2.8 thay phần tắt bật tay bằng cách làm mới (bảng phút thêm cột "Sau T-051"), thêm cách kiểm `gh workflow list --all` và "bật trước khi chạy tay".
- `docs/ops/session-capabilities.md`: dòng mới `workflow-enable` (không có mẫu đường dẫn; cloud `thử`, routine `thử`, local `có`); ghi chú của `auto-resume` thêm "lịch được `status-page.yml` bật khi danh sách được duyệt (T-051)".
- `docs/templates/run.md`: mục 2 (Kiểm tra đầu phiên), dòng về hai lịch thành "`scripts/schedule-idle.py --enable`: mã 0".
- `CLAUDE.md`, mục Lệnh: dòng `run-watchdog.yml` "chạy nó mỗi 30 phút" thêm "khi có phiên hay danh sách chờ (tự tắt khi rảnh, T-051)"; thêm một dòng cho `scripts/schedule-idle.py`.
- `scripts/run-watchdog.py`, `scripts/resume-check.py`: chú thích nói "every 30 minutes", "every hour" đúng khi lịch bật; thêm một câu "the workflow turns itself off when idle (scripts/schedule-idle.py)" ở docstring.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/schedule-idle.py` | Script ở mục 3.1 |
| Sửa | `.github/workflows/resume-run.yml` | Bước tự tắt, `actions: write`, `persist-credentials: false`, chú thích |
| Sửa | `.github/workflows/run-watchdog.yml` | Bước tự tắt, `actions: write`, chú thích |
| Sửa | `.github/workflows/start-run.yml` | Job `fire`: `actions: write`, bước bật hai lịch |
| Sửa | `.github/workflows/status-page.yml` | Job `schedules`, chú thích |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4 |
| Sửa | `scripts/run-watchdog.py` | Một câu ở docstring |
| Sửa | `scripts/resume-check.py` | Một câu ở docstring |
| Sửa | `docs/auto-run.md` | Mục 2, mục 4 |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.3, 2.6, 2.7, 2.8 |
| Sửa | `docs/ops/session-capabilities.md` | Dòng `workflow-enable`, ghi chú `auto-resume` |
| Sửa | `docs/templates/run.md` | Mục 2, dòng về hai lịch |
| Sửa | `CLAUDE.md` | Mục Lệnh |
| Sửa | `docs/features/quy-trinh/tickets/T-051-lich-dinh-ky-tiet-kiem-phut.md` | `plan:`, quyết định, trạng thái theo plan |
| Sửa | `docs/lessons.md` | L-126 sang `applied` sau khi merge |

`grep -rn 'resume-run\|run-watchdog\|status-page' CLAUDE.md scripts/ .github/ docs/ .claude/skills/` đã chạy: `.claude/skills/whatnow/SKILL.md` và `docs/ops/to-app.md` không nhắc hai lịch; báo cáo phiên cũ trong `docs/runs/` là lịch sử, không sửa.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi, và plan không đổi tên hay xóa mục nào:
- dẫn `docs/auto-run.md` hay `docs/ops/staging-and-runs.md`: `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/env-canary.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/save-branch.sh`, `scripts/setup-check.sh`, `scripts/token-usage.py`, `scripts/wait-run.sh`;
- dẫn `.github/workflows/start-run.yml`: `.github/workflows/env-canary.yml`, `scripts/env-canary.sh`, `scripts/main-guard-status.sh`, `scripts/setup-check.sh` (không đọc job `fire`);
- `scripts/run-check.py` đọc bảng khả năng của `docs/ops/session-capabilities.md`: dòng không có mẫu đường dẫn trong dấu backtick bị bỏ qua (`capability_rows`), nên dòng `workflow-enable` mới không áp cho plan nào; ghi chú của `auto-resume` không đổi cột nào;
- `scripts/status.py` đọc trạng thái bài học trong `docs/lessons.md`, mục Cần người kiểm tra của báo cáo phiên, và mẫu của `docs/auto-run.md`: plan chỉ đổi trạng thái L-126 sang `applied` đúng khuôn, và dòng của `docs/templates/run.md` nằm ở mục 2, không ở Cần người kiểm tra;
- `scripts/new-app.sh` và `.github/ISSUE_TEMPLATE/from-app.yml` chỉ nhắc tên `docs/lessons.md`; `scripts/new-app.sh` thay đoạn đầu của `CLAUDE.md`, không chạm mục Lệnh;
- `.github/workflows/pr-check.yml` và `scripts/local-setup.sh` chạy hay nhắc `scripts/tests/run.sh`, không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của workflow: `actions: write` cho `resume-run.yml`, `run-watchdog.yml`, job `fire` của `start-run.yml` và job `schedules` của `status-page.yml`; không thêm bí mật.
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** token của phiên cloud và routine có gọi được `enable` không (dòng `workflow-enable`, `thử`). `GITHUB_TOKEN` đã thử được (mục 4.1).
- **Điều kiện chuyển:** sau khi merge, (a) một lần chạy của `resume-run` hay `run-watchdog` in CHÚ Ý `disable` lỗi, hay (b) `status-page`, `start-run` hay `--enable` của phiên báo không bật được (403, 404).
- **Cách làm thay:** (a) không đổi code: script không làm đỏ workflow, lịch giữ như hiện nay. (b) dev bật bằng `scripts/schedule-idle.py --enable` trên máy khi duyệt danh sách, ghi vào nhật ký; lỗi ở cả `status-page` và `start-run` (cùng `GITHUB_TOKEN` đã thử) thì revert PR cùng dev và mở ticket mới. Tiêu chí và test ở mục 4 không đổi.

### 3.5. Hành vi đã có bị đổi

Không có code Kotlin. Hành vi của workflow bị đổi:

| Hành vi cũ | Hành vi mới | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `resume-run`, `run-watchdog` chạy mọi lúc theo lịch | Tự tắt khi rảnh, được bật lại khi có danh sách duyệt, `start-run` hay phiên | Không có test nào khẳng định lịch luôn chạy (`grep -n 'resume-run.yml\|run-watchdog.yml' scripts/tests/run.sh`: chỉ các khối `resume-check`, `run-watchdog`, kiểm logic của script) | Giữ mọi test có sẵn |
| `run-watchdog.yml` chỉ có `actions: read`; `status-page.yml` chỉ có một job | `actions: write`; thêm job `schedules` | Không có test kiểm khối `permissions` hay số job của hai file | Thêm test mới ở mục 4 |

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, dùng `scripts/tests/fake-gh`: đường đi khớp từng ký tự (ví dụ `branches?per_page=100&page=1`, `actions/workflows/start-run.yml/runs?per_page=1`); `__sequence__` đếm theo từng đường, dùng cho lần đọc lại; lệnh ghi phải là `-X PUT` (fake-gh chỉ nhận `-X`) và được ghi vào `$FIXTURE.writes`; `fail_writes` cho lệnh ghi lỗi; `errors` cho lần đọc lỗi; đường không khai trả 404. Hàm dựng `schedule_idle` theo kiểu `resume_check`, nhận trạng thái nhánh báo cáo, danh sách trên nhánh chính, `updated_at`, giờ của `start-run`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hoàn thành 2: có phiên `running` thì không tắt | Test script | `schedule-idle: a running session keeps the schedule`: không có dòng ghi |
| Hoàn thành 1: rảnh thì tắt | Test script | `schedule-idle: no session, no pending list, enabled long ago, no recent start-run: disables`: đúng một dòng `PUT actions/workflows/resume-run.yml/disable` |
| Danh sách duyệt chưa bắt đầu giữ lịch | Test script | `schedule-idle: an approved run list without a report branch keeps the schedule`: commit của file 10 giờ trước: không có dòng ghi; đối chứng 80 giờ trước: tắt; đối chứng có nhánh báo cáo `finished`: tắt |
| Vừa được bật thì giữ | Test script | `schedule-idle: enabled within the grace hours keeps`: `updated_at` 1 giờ trước: không có dòng ghi; đối chứng 4 giờ: tắt |
| `start-run` vừa chạy thì giữ | Test script | `schedule-idle: start-run ran within the grace hours keeps`: 1 giờ: không có dòng ghi; đối chứng 4 giờ: tắt |
| App mới chưa từng chạy `start-run` | Test script | `schedule-idle: no start-run run at all still disables, exit 0`: `workflow_runs: []` |
| Phiên hiện ra sau khi tắt | Test script | `schedule-idle: a session that appears after the disable turns the schedule back on`: đường `contents/...` của nhánh báo cáo `__sequence__` `finished` rồi `running`: hai dòng `disable` rồi `enable` theo thứ tự |
| Đọc lại lỗi thì bật lại | Test script | `schedule-idle: a failed second read turns the schedule back on`: `__sequence__` của `branches?...` trả danh sách rồi lần sau lỗi (`errors` không theo lượt được thì dùng đường khác): `disable` rồi `enable` |
| Danh sách `finished` cũ không giữ lịch | Test script | `schedule-idle: a finished run does not keep the schedule`: tắt |
| File danh sách không có trên nhánh báo cáo (404) | Test script | `schedule-idle: a report branch without its run file does not keep the schedule`: đường không khai: tắt |
| Lỗi đọc khác thì giữ | Test script | `schedule-idle: an unreadable run file keeps the schedule, exit 0`: `errors` cho `contents/...` (HTTP 500): không có dòng ghi, mã 0 |
| Không đọc được GitHub thì không tắt | Test script | `schedule-idle: GitHub unreadable keeps, exit 0`: `errors` cho `branches?...`: không có dòng ghi, mã 0, có "Không đọc được GitHub" |
| `disable` lỗi không làm đỏ workflow | Test script | `schedule-idle: a failed disable warns and exits 0`: `fail_writes`: mã 0, có "CHÚ Ý" |
| `--enable` bật cả hai | Test script | `schedule-idle --enable: enables both schedules`: hai dòng `PUT .../resume-run.yml/enable`, `PUT .../run-watchdog.yml/enable`; `state` `active`: mã 0 |
| `--enable` báo khi không bật được | Test script | `schedule-idle --enable: a failed enable exits 1 and names the workflow`: `fail_writes` cho `run-watchdog.yml/enable`: mã 1, có `run-watchdog.yml`; và `schedule-idle --enable: a schedule still disabled after enable exits 1`: `state` `disabled_manually`: mã 1 |
| `--enable-if-pending` | Test script | `schedule-idle --enable-if-pending: enables when an approved list waits, nothing otherwise`: có danh sách chờ: hai dòng `enable`; không có: không có dòng ghi, mã 0 |
| Workflow tự tắt đúng tệp, đủ quyền | Test đọc file | `workflows: resume-run and run-watchdog disable themselves when idle`: mỗi file có `schedule-idle.py --disable-if-idle <chính tên file>`, `actions: write`, `GH_TOKEN`, `if: always()` ở bước đó; trong `run-watchdog.yml` bước tắt nằm sau bước `run-watchdog.py`; `resume-run.yml` có `persist-credentials: false` |
| `start-run.yml` bật trước khi kích hoạt | Test đọc file | `start-run: enables both schedules before firing the routine`: job `fire` có `actions: write`, `GH_TOKEN` và hai lệnh `.../resume-run.yml/enable`, `.../run-watchdog.yml/enable` trước dòng `curl` |
| `status-page.yml` bật khi có danh sách chờ | Test đọc file | `status-page: a separate job enables the schedules for a pending run list`: job `schedules` có `actions: write`, `--enable-if-pending`; job `page` không có `actions: write` |
| Lịch tự tắt và được bật trên GitHub thật | Kiểm tra thủ công (người: cần GitHub thật sau khi merge) | Không có phiên: chạy tay `resume-run` và `run-watchdog`; tóm tắt có "đã tắt", `gh workflow list --all` thấy `disabled_manually`. Merge một danh sách `approved` thử (hay chạy tay `status-page` khi có danh sách chờ): hai lịch `active` |
| Phiên bật lại được | Kiểm tra thủ công (người: chỉ thấy ở phiên thật) | Phiên tự chạy đầu tiên sau khi merge: Kiểm tra đầu phiên có dòng của `--enable`; ghi kết quả vào dòng `workflow-enable` của `docs/ops/session-capabilities.md` |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085). Mọi test script và test đọc file thất bại trước khi có script và trước khi sửa workflow.

**Ngoại lệ viết test trước:** tài liệu (`docs/`, `CLAUDE.md`) và chú thích (đầu workflow, docstring của hai script).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-29, trên repo dự án mẫu, anh Nhật cho phép.
  - Bằng `gh` của máy dev: đọc `state`, `updated_at` của `run-watchdog.yml`; `PUT .../enable` lên workflow đang bật; đọc `created_at` của lần chạy `start-run` gần nhất.
  - Bằng `GITHUB_TOKEN`: workflow tạm `tmp-t051-probe.yml` trên nhánh nháp `claude/tmp-t051-probe` (`on: push` của chính nhánh đó, `permissions: actions: write`), lần chạy 40865254015; đã xóa nhánh sau khi chạy.
- **Lệnh:** `gh api repos/<repo>/actions/workflows/run-watchdog.yml --jq '.state + " " + .updated_at'`; `gh api -X PUT .../disable -i`; `gh api -X POST .../dispatches -f ref=main -i`; `gh api -X PUT .../enable -i`.
- **Kết quả:**
  - `enable` lên workflow đang bật: `HTTP 204`, `updated_at` đổi sang giờ gọi.
  - `GITHUB_TOKEN`: `disable` trả `204`, `state` thành `disabled_manually`, `updated_at` đổi; `workflow_dispatch` lên workflow đã tắt trả `422`; `enable` trả `204`, `state` về `active`, `updated_at` đổi. `run-watchdog` tắt khoảng một giây và đang `active`.
  - Chưa thử token của phiên cloud và routine (mục 3.4). `scripts/check.sh` không cần (không có code Kotlin); phiên chạy `scripts/tests/run.sh`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và quy trình phiên tự chạy: `.github/workflows/`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Bốn workflow, một script mới, test, năm tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 1 | Phương án đã chốt; còn mốc `GRACE_HOURS`, `PENDING_HOURS` (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 2 | Logic có test với `gh` giả; lịch thật, thời gian và token của phiên chỉ kiểm tay sau khi merge |
| D. Ảnh hưởng chéo | 2 | `start-run.yml` và `status-page.yml` chạy với mọi phiên và mọi lần merge tài liệu; lịch tắt nhầm thì phiên không tự tiếp tục |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng API bật tắt workflow trong dự án; phổ biến, có tài liệu, đã thử bằng `GITHUB_TOKEN` |
| **Tổng** | 9 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `start-run.yml`, `status-page.yml`, `docs/auto-run.md` mục 2 và 4, mẫu danh sách chạy, `CLAUDE.md` mục Lệnh.
- Task khác có thể xung đột: ba ticket của T-052 (chưa có) không chạm các file này. Không có danh sách chạy nào đang `draft` hay `approved`.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow và script là của dự án mẫu; `CLAUDE.md` gộp tay. App đang tắt tay các lịch (Photune) vẫn đúng: lần duyệt danh sách kế tiếp bật lại.
- Đề xuất thứ tự: làm trong phiên có người, trước T-052.

## 7. Điều kiện dừng riêng

- Cần thêm bí mật, token cá nhân hay biến repo để làm được: dừng (trái quyết định đã chốt).
- Sau khi merge, `resume-run`, `run-watchdog`, `start-run` hay `status-page` đỏ (không chỉ CHÚ Ý): revert PR cùng dev.

## 8. Câu hỏi cho người duyệt

1. `GRACE_HOURS` 3 và `PENDING_HOURS` 72. 3 giờ che khoảng từ `start-run` hay một lần bật tới lúc phiên đẩy nhánh báo cáo; 72 giờ che một danh sách duyệt vào thứ Sáu, chạy sáng thứ Hai. Đề xuất giữ hai số này.
2. Phiên mở bằng Run now, hẹn giờ hay `claude --cloud` mà danh sách đã được duyệt hơn 72 giờ, và token của phiên không bật được workflow: dòng của mục 2 ghi vào Cần người kiểm tra, người bật tay. Đề xuất chấp nhận; phiên đầu tiên sau khi merge cho biết token của phiên có bật được không.
3. Danh sách bị bỏ quên ở `running` (nhánh báo cáo cũ) giữ lịch bật mãi, như hiện nay; `run-watchdog` vẫn báo bằng issue `session-stalled`. Đề xuất giữ.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
