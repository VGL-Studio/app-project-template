---
ticket: T-078
title: Runner tự host cho các job nặng, có đường quay về runner của GitHub
status: draft
version: 2
risk_score: 12
risk_level: high
token_estimate: 30.0M
blocking_points: [ci]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-078: RUNNER TỰ HOST CHO CÁC JOB NẶNG, CÓ ĐƯỜNG QUAY VỀ RUNNER CỦA GITHUB

## 1. Tóm tắt

Các job nặng không đọc bí mật là `build-and-test` của `pr-check`, `guard` và `ui-test` của `main-guard`. Chúng khai báo `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`. Biến `HEAVY_RUNNER` của repo chọn runner tự host; xóa biến là quay về runner của GitHub.

Runner tự host chạy một lần, mỗi job một container mới. Đây là hàng rào an toàn thật, cùng với cài đặt phía GitHub: nhóm runner không cho repo công khai, tắt workflow từ fork. Script mới `scripts/runner-check.sh` chỉ bắt cấu hình nhầm trước khi build, không phải hàng rào an toàn, vì PR sửa được chính script đó. `staging`, `device-session` và các job nhẹ giữ runner của GitHub.

`scripts/wait-run.sh` có mã thoát mới 7 (`CHỜ RUNNER`) cho job đứng chờ runner tự host khi không còn gì của lần chạy đang chạy. `scripts/resume-check.py` không mở lại phiên khi có job như vậy. Kiểm bằng test tĩnh trên mọi workflow và test của các script trong `scripts/tests/run.sh`. Dựng máy, cài runner, đặt biến và lần thử quay về là việc của người, sau khi task merge. Theo ADR-0042 (`accepted` ngày 2026-10-30).

**Phiên bản 2 (2026-11-08).** Bản 1 duyệt ngày 2026-10-30 (anh Nhật), viết trước khi T-076 và T-077 merge. Rà soát độc lập danh sách chạy RUN-20261101-02 (2026-11-01, AI đối chiếu lại trên nhánh chính `043d0682`) tìm ra các điểm chặn sau, nên T-078 không chạy ở phiên đó. Bản 1 chưa đủ tốt vì nó viết diff và mục 3.5 theo giả định về ba plan chưa merge, mà không chạy test có sẵn của chúng trên workflow đã sửa; và vì ADR-0044 merge sau khi bản 1 được duyệt. Cách sửa:

1. **`pr_sim` của T-076 in `ERROR` với bước kiểm runner.** Hàm mô phỏng PR mẫu chỉ hiểu `steps.filter.outputs`, `always()`, `failure()`, `&&`, `||` và so sánh chuỗi; bước `if: runner.environment == 'self-hosted'` làm mọi câu P6 in `ERROR điều kiện lạ: runner.environment == 'self-hosted'`. AI đã tái hiện điều này ngày 2026-11-08 trên bản sao của `pr-check.yml` có thay đổi của T-077 và T-078 (mục 4.1). Bản 2: `pr_sim` thay `runner.environment` bằng giá trị của biến `PR_SIM_RUNNER` (mặc định `github-hosted`, chỉ nhận thêm `self-hosted`), phần mở rộng vào commit test của bước 3, có test 12 và đối chứng (mục 3.1 H, mục 4). Mọi câu P6 có sẵn chạy với giá trị mặc định, kết quả không đổi (đã chạy thử).
2. **`scripts/runner-check.sh` chưa vào `GATE_FILES` của `scripts/main-guard-verified.sh` (ADR-0039) và `UI_PATHS` của `scripts/ui-test-needed.sh` (ADR-0043).** Job `guard` chạy script này nên commit đổi nó phải được `main-guard` kiểm `full`; job `ui-test` chạy nó nên commit đổi nó phải chạy emulator. Bản 2 thêm vào cả hai danh sách và vào dữ liệu của hai test có sẵn (test 13, 14; mục 3.5).
3. **Mã `PB-16` đã dùng** (dòng job hết giờ, `has exceeded the maximum execution time`). Bản 2 dùng ba mã kế tiếp còn trống: PB-17 (chờ runner), PB-18 (runner tự host mất kết nối), PB-19 (runner không dùng được). Thêm: PB-18 bỏ mẫu "The runner has received a shutdown signal" của bản 1, vì câu này cũng gặp trên runner của GitHub và sẽ biến một lỗi nên chạy lại thành việc của người (mục 8, câu 5).
4. **ADR-0044:** câu chép vào tài liệu quy tắc không được mang mã `L-`, `T-`, `RUN-`. Bản 1 có câu như "các bước H4 tới H6 của plan T-078" cho `docs/ops/self-hosted-runner.md`. Bản 2 viết nguyên văn mọi câu thêm vào tài liệu quy tắc (mục 3.1 F, G), kể cả toàn bộ tài liệu mới; AI đã áp vào một bản sao của nhánh chính (cùng các câu của T-077 bản 2 ở cùng chỗ) và chạy `scripts/history-refs.py`: sạch; đối chứng thêm `L-080` vào tài liệu mới thì script báo lỗi.

Thay đổi khác so với bản 1:

5. **Viết trên trạng thái sau T-076 (đã merge, d83c392) và sau T-077 phiên bản 2 (giả định, mục 2).** Diff của hai workflow viết trên tệp thật; phần của T-077 ghi rõ. Bỏ phương án và điều kiện dừng cho trường hợp T-075, T-076 làm khác (đã merge, khớp).
6. **Ghi chú duyệt bản 1, điểm (a) tới (d)** (mục 9, bản 1) đưa vào thân test 3; đối chứng (d) dùng chính `pr-check.yml` trên nhánh chính.
7. **Cách chạy:** anh Nhật quyết ngày 2026-11-01 chạy T-078 trong phiên tự chạy, chỉ với plan phiên bản 2 đã duyệt (ticket để trống `auto_run`). Câu "làm có người" của bản 1 được thay. Phiên kiểm DoD 4 trên GitHub thật bằng `gh api` trước khi merge và sau khi merge (mục 4). Bước của người (H1 tới H6) giữ, sau merge.
8. **Image cần `gh`** (bước bình luận lỗi của `pr-check`, các bước issue của `main-guard` gọi `gh`) và phải gỡ `sudo` của image runner chính thức; bản 1 thiếu cả hai.
9. **`wait-run.sh`:** biểu thức đọc job chịu được câu trả lời không có `jobs` (`(.jobs // [])`), đã chạy thử; kiểm `WAIT_RUN_RUNNER_QUEUE_MINUTES` là số.
10. **`resume_check` của `scripts/tests/run.sh`** nhận thêm dữ liệu cho hai đường `actions/runs?status=...` (mục 3.5).

Không đổi cách làm của bản 1: phân loại job, phương án an toàn, các điều `runner-check.sh` kiểm, mã 7 và bốn điều của nó, luật của `resume-check.py`, `actions: read` cho `resume-run.yml`, bước của người. Câu hỏi 1 tới 8 của bản 1 đã được trả lời (mục 9, bản 1) và giữ; mục 8 chỉ còn câu hỏi về phần đổi của bản 2.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 1. **Đổi 2026-11-01:** anh Nhật quyết chạy T-076, T-077, T-078 trong phiên tự chạy (lời anh: "Không, hãy chạy phiên"; chọn "Cả ba, nối tiếp"); T-078 chỉ với plan phiên bản 2 đã duyệt. Ticket để trống `auto_run`.

| Yêu cầu (phạm vi và định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 2 | Job nặng đọc nhãn runner từ biến; xóa biến thì về runner của GitHub; job nhẹ giữ runner của GitHub | Biến cấp repo, không cấp tổ chức: gói Free không cho repo private đọc biến cấp tổ chức (đã duyệt ở bản 1). "Job nặng" xác định theo nội dung job, không theo tên: chạy `scripts/check.sh`, `./gradlew` hay `android-emulator-runner`, và không có `secrets.` |
| Phạm vi 3 | Bước giả định máy sạch chạy được trên cả hai loại máy, hoặc bỏ qua trên máy tự host | Bước có `sudo` (giải phóng đĩa, bật KVM) thêm `if: runner.environment == 'github-hosted'`. Trên máy tự host, quyền KVM do máy chủ cấp cho container (luật udev, `--group-add`). `setup-java`, `setup-gradle` chạy được trên cả hai loại máy, giữ nguyên |
| Phạm vi 4 | `check.sh` không để lại trạng thái giữa hai lần chạy; mỗi job có thư mục làm việc sạch | Container mới cho mỗi job (ADR-0042 mục 2). `actions/checkout` giữ `clean` mặc định. `runner-check.sh` từ chối thư mục làm việc có file ngoài git. `check.sh` trong CI không tự ghi ảnh chuẩn, nên không đổi |
| Phạm vi 5 (an toàn) | Chọn: chạy một lần, dọn `~/.gradle`, hay tách runner cho job có bí mật | Chạy một lần trong container, chỉ cho job không đọc bí mật; `staging`, `device-session` giữ runner của GitHub (mục 3.1) |
| Phạm vi 6 | Dòng bảng tra lỗi: máy tắt, hết đĩa, job chờ runner quá lâu | PB-17 (chờ runner tự host, `wait-run.sh` mã 7), PB-18 (runner tự host mất kết nối giữa job), PB-19 (runner không dùng được: cấu hình, đĩa, KVM) |
| DoD 1 | Job nặng chạy trên runner tự host; lịch sử tính tiền không còn phút của chúng | Việc của người sau khi đặt biến (mục 4, H4, H5) |
| DoD 2 | Xóa biến thì job chạy lại trên runner của GitHub, thử một lần, ghi vào plan | Mục 4, H6 |
| DoD 3 | Tài liệu cài và vận hành máy, dòng bảng tra lỗi | `docs/ops/self-hosted-runner.md` mới (nguyên văn ở mục 3.1 G); PB-17 tới PB-19 |
| DoD 4 | CI qua, đã merge | Như mọi task. Merge được trước khi có máy: biến chưa đặt thì mọi job vẫn chạy trên runner của GitHub; phiên kiểm điều này trên GitHub thật (mục 4) |

**Trạng thái nhánh chính mà plan dựa vào** (đọc ngày 2026-11-08, commit 2fd2701, sau T-076 ở d83c392):

- `.github/workflows/pr-check.yml`: một job `build-and-test` (dòng 31), `runs-on: ubuntu-latest` (32), `timeout-minutes: 45` (33). Bước 1 `Lọc đường dẫn (dorny/paths-filter)` (`id: filter`, dòng 36), bước 2 `Kiểm kết quả lọc đường dẫn` (72; trên đường lỗi ghi `build.log` bằng `tee -a`), `- uses: actions/checkout@v5` (87, không có `with:`), `actions/setup-java@v5` (89).
- `.github/workflows/main-guard.yml`: job `guard` (43) có `if: github.ref == 'refs/heads/main'` (45), `runs-on: ubuntu-latest` (46), checkout với `fetch-depth: 50` (51 tới 53), bước `Chọn cách kiểm (scripts/main-guard-verified.sh)` `id: mode` (55). Job `ui-test` (216): `needs: guard`, `if: needs.guard.outputs.ui != 'no'`, `runs-on: ubuntu-latest` (220), `timeout-minutes: 60`, checkout (223 tới 226), `setup-java` (228), `setup-gradle` (233), `Giải phóng đĩa cho emulator` (238, `sudo rm -rf`), `Bật KVM cho emulator` (244, `sudo tee`), cả hai không có `if:`.
- `.github/workflows/resume-run.yml`: `permissions:` cấp workflow `contents: read`, `issues: read` (dòng 27 tới 29); job `resume` không có `permissions:` riêng; job `idle` có `actions: write` riêng.
- `scripts/main-guard-verified.sh`: `GATE_FILES` dòng 30 tới 32 (chín file, cuối là `scripts/ui-test-needed.sh`). `scripts/ui-test-needed.sh`: `UI_PATHS` dòng 22 tới 23.
- `scripts/wait-run.sh`: mã 0 tới 5 (chú thích dòng 13 tới 19); vòng chờ dòng 92 tới 143; kiểm xung đột (mã 5) dòng 126 tới 129, kiểm hết giờ dòng 130.
- `scripts/resume-check.py`: kiểm `main-broken` dòng 121, `env-broken` dòng 124, in danh sách dòng 127 tới 130.
- `docs/ops/failure-playbook.md`: dòng cuối là PB-16; tệp `scripts/tests/playbook/PB-16.log` có.
- `scripts/tests/run.sh`: hàm `pr_*` dòng 272 tới 316; `pr_job_if` 304; `pr_sim` 393 tới 466; P6 512 tới 536; P6d 537 tới 547; G1 548 tới 553; `pr_no_emulator` 554 tới 559; `mgv_gate_files` 685; `mg_steps` 748, `mg_wiring` 758, `ui_steps` 794, `ui_wiring` 820, `mg_own_issues` 881 tới 890; khối `playbook` 1215 tới 1300 (câu "every row has a log file" 1296); `uin_code` 1361; khối `wait-run` 1722 tới 1866; `resume_check` 1868 tới 1900; `wf_job`, `wf_level` 2177 tới 2180; `si_idle_job` 2181.

**Giả định về T-077 (phụ thuộc).** Ngày soạn bản này, T-077 phiên bản 2 là plan `draft` ở PR #411 (nhánh `claude/plan-T-077-ban-2`, commit 9f7d8de sau rà soát độc lập). Plan này viết cho trạng thái sau khi T-077 bản 2 merge, như plan đó mô tả (mục 3.1 C, D, E, F và mục 6 của nó):

- `pr-check.yml`: `on.pull_request` thêm `types: [opened, synchronize, reopened, ready_for_review]` (kèm một dòng chú thích); `build-and-test` có đúng một dòng `    if: github.event.pull_request.draft == false`, ngay sau `    runs-on:`; chú thích đầu file đổi câu "The job is never skipped." thành ba dòng. Tên job `build-and-test`, tên bước `Cổng chất lượng (scripts/check.sh)`, `id: filter` giữ nguyên.
- `scripts/tests/run.sh`: `pr_job_if` thay bằng `pr_job_if_draft` (đòi đúng một dòng `^    if:` cấp job, đúng điều kiện nháp); `pr_sim` đọc `if:` cấp job (dòng bắt đầu đúng bốn dấu cách `    if: `), có `import os` và biến `PR_SIM_DRAFT`; `mig_run` chạy trong `$work/mig-repo`, có biến `MIG_SCRIPT`; khối mới `nu_routes` / `nu_git_run` ngay sau `merge_while_broken`; test 3, 4 của T-077 trong khối `wait-run`.
- `scripts/wait-run.sh`: mã 6 (`NHÁP`), kiểm nháp ngay sau `read_head` trong vòng; lần chạy `completed`/`skipped` coi như chưa có (`run=""`). Không có mã 7.
- `scripts/merge-if-green.sh`: thêm `MAIN_GUARD_VERIFIED`, `docs_only_behind`, `refuse_same_title`; kiểm tra `skipped` là chờ. Dòng `  return 3` là dòng duy nhất bắt đầu như vậy (bản sao đối chứng R4 của T-077 dựa vào). T-078 không sửa `merge-if-green.sh`, nên không thêm dòng nào như thế.
- `scripts/main-guard-verified.sh`: thêm `--docs-only`, `--gate-ran <sha>`; ba dòng tính `REPO` dời lên trước phần kiểm tham số. `GATE_FILES` không đổi.
- `docs/auto-run.md` bước 8: câu mã thoát thêm "6 PR là bản nháp (`NHÁP`)" và một ý "Mã 6 (`NHÁP`): ..." ngay sau ý mã 5; skill `merge-pr` bước 2 thêm câu mã 6 sau "Mã 4 (hạ tầng), mã 5 (xung đột): theo bước 8.".

Đối chiếu lúc soạn (mục 4.1): trên bản sao `pr-check.yml` có cả thay đổi của T-077 bản 2 lẫn T-078, `pr_job_if_draft` của T-077 in `1 yes`; mọi hàm `pr_*` có sẵn cho kết quả như trên nhánh chính. Lệch ở số dòng lúc bắt đầu task thì theo ADR-0032/L3; lệch ở tên hàm, tên test, tên bước hay chữ trích ở trên thì dừng (mục 7). T-077 bản 2 đổi sau rà soát hay lúc duyệt thì đối chiếu lại plan này trước khi duyệt (mục 8, câu 1).

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0042 (quyết định của task, `accepted`), ADR-0039 (`GATE_FILES`; cách chọn `skip`, `tests`, `full` giữ nguyên), ADR-0043 (`ui-test`, `UI_PATHS`), ADR-0041 (T-077: PR nháp, mã 6), ADR-0044 (tài liệu quy tắc không ghi mã lịch sử), ADR-0029 (phạm vi file), ADR-0031 (câu khẳng định cũ), ADR-0009 và ADR-0017 (production, ngoài phạm vi).

## 3. Giải pháp

### 3.1. Cách làm

**Phân loại job** (đọc ngày 2026-11-08 trên nhánh chính):

| Workflow / job | Nặng hay nhẹ | Bí mật | Quyền của `GITHUB_TOKEN` | Bước giả định máy sạch | Sau T-078 |
| --- | --- | --- | --- | --- | --- |
| `pr-check` / `build-and-test` | nặng (`check.sh`, R8) | không | `contents: read`, `pull-requests: write` (bình luận) | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `main-guard` / `guard` | nặng (`check.sh`, trừ cách `skip`) | không | `contents: write`, `issues: write`, `checks: read`, `actions: read`, `pull-requests: read` | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `main-guard` / `ui-test` | nặng (emulator, tới 60 phút) | không | như `guard` | `sudo rm -rf` giải phóng đĩa, `sudo tee` udev bật KVM, `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `staging` / `distribute` | nặng | `STAGING_*` | `contents: read` | `setup-java`, `setup-gradle` | giữ `ubuntu-latest` |
| `device-session` / `emulator` | nặng | `STAGING_*` | `contents: read` | như `ui-test` | giữ `ubuntu-latest` |
| `env-canary`, `start-run`, `resume-run`, `run-watchdog`, `status-page` | nhẹ (lịch, API) | có ở `env-canary`, `start-run`, `resume-run` | như hôm nay | không | giữ `ubuntu-latest` |

**Phương án an toàn: runner chạy một lần trong container, chỉ cho job không đọc bí mật** (giữ như bản 1, ADR-0042 mục 2):

- Dọn `~/.gradle` sau mỗi job không đủ: GitHub không bảo đảm runner chạy lâu chỉ nhận một job, và tiến trình chạy nền hay tệp ngoài các thư mục đã dọn vẫn còn. Tách runner cho job có bí mật đưa khóa ký staging lên máy của nhóm, lợi khoảng 13% số phút: không đáng ở bước đầu. Chạy một lần trong container mới: PR không để lại gì cho `main-guard`.
- **Hàng rào nằm ở GitHub và ở máy, không trong repo.** Với `pull_request`, GitHub đọc workflow và script từ code của PR, nên mọi kiểm tra trong repo chỉ bắt cấu hình nhầm. Hàng rào thật: nhóm runner không cho repo công khai; tắt workflow từ fork của repo private; container chạy một lần, không `sudo`, không socket Docker, token xin cấu hình không vào container.
- `guard`, `ui-test` có quyền ghi mà vẫn lên runner tự host: code của chúng là code đã merge, token chỉ sống trong job, container mới không nhận gì từ job PR trước.
- Đánh đổi: ở cách `skip`, `guard` vẫn giữ một chỗ trên máy khoảng 1 phút; `ui-test` có `needs: guard` nên để `guard` trên runner của GitHub cũng không bớt được phút đó.
- Job có `secrets.` giữ `ubuntu-latest`; test 1 chặn.

**A. Script mới `scripts/runner-check.sh [--emulator]`.**

- Mục đích: bắt cấu hình nhầm của máy hay của repo trước khi build; không phải hàng rào an toàn. Bash, `set -euo pipefail`, thông báo tiếng Việt, chú thích tiếng Anh; chú thích đầu file nói script chỉ chạy trên runner tự host, ở bước `if: runner.environment == 'self-hosted'`, và "Changing this file changes the quality gate: high-risk plan" như các script cổng khác. Không gọi mạng, không đọc bí mật. Bit chạy `100755` trong git.
- Tham số: không có, hay đúng `--emulator`. Khác thì in `LỖI: cách dùng: scripts/runner-check.sh [--emulator]` ra stderr và thoát 2.
- Kiểm hết các điều dưới, không dừng ở lỗi đầu. Mỗi điều sai in một dòng bắt đầu bằng `RUNNER KHÔNG DÙNG ĐƯỢC: `, kết thúc bằng ` (docs/ops/self-hosted-runner.md).`; có ít nhất một dòng thì thoát 1. Đủ hết thì in một dòng `RUNNER DÙNG ĐƯỢC: repo <visibility>, đĩa trống <n> GB<, KVM và ANDROID_HOME dùng được khi --emulator>.` và thoát 0.
  1. `REPO_VISIBILITY` (workflow truyền `${{ github.event.repository.visibility }}`) là `private` hay `internal`. Khác (rỗng, `public`): `RUNNER KHÔNG DÙNG ĐƯỢC: repo không phải private (REPO_VISIBILITY='<giá trị>'); runner tự host chỉ dành cho repo private (docs/ops/self-hosted-runner.md).`
  2. `VGL_RUNNER_EPHEMERAL` là `1`. Khác: `RUNNER KHÔNG DÙNG ĐƯỢC: container không có dấu VGL_RUNNER_EPHEMERAL=1 (docs/ops/self-hosted-runner.md).`
  3. Thư mục Gradle `${GRADLE_USER_HOME:-$HOME/.gradle}` không có `init.d`, `init.gradle`, `init.gradle.kts`, `gradle.properties`; mỗi mục có in một dòng `RUNNER KHÔNG DÙNG ĐƯỢC: <thư mục>/<mục> có từ trước (docs/ops/self-hosted-runner.md).` Bước này đứng trước `setup-gradle`, vì `setup-gradle` tự đặt init script vào đó.
  4. `git status --porcelain --ignored` trong thư mục làm việc chạy được và rỗng. Lệnh lỗi: `RUNNER KHÔNG DÙNG ĐƯỢC: thư mục làm việc không phải repo git (docs/ops/self-hosted-runner.md).` Có dòng: `RUNNER KHÔNG DÙNG ĐƯỢC: thư mục làm việc có file ngoài git: <đường dẫn đầu tiên> (docs/ops/self-hosted-runner.md).`
  5. Đĩa trống ở thư mục làm việc (`df -Pk .`, cột 4, chia 1048576) ít nhất `RUNNER_CHECK_MIN_FREE_GB` GB: mặc định 20, có `--emulator` thì 30 (emulator cần khoảng 7,4 GB, như chú thích của `ui-test`). Thiếu: `RUNNER KHÔNG DÙNG ĐƯỢC: đĩa trống <n> GB, cần ít nhất <m> GB (docs/ops/self-hosted-runner.md).`
  6. Có `--emulator`: `${RUNNER_CHECK_KVM:-/dev/kvm}` đọc và ghi được (`[ -r ] && [ -w ]`), khác thì `RUNNER KHÔNG DÙNG ĐƯỢC: <đường KVM> không mở được để đọc và ghi (docs/ops/self-hosted-runner.md).`; `ANDROID_HOME` có giá trị và ghi được (`[ -w ]`), khác thì `RUNNER KHÔNG DÙNG ĐƯỢC: ANDROID_HOME trống hay không ghi được (docs/ops/self-hosted-runner.md).` (`android-emulator-runner` cài gói emulator và ảnh hệ thống vào đó).
- `RUNNER_CHECK_MIN_FREE_GB` và `RUNNER_CHECK_KVM` chỉ để test đè; chú thích đầu file ghi như vậy.

**B. Workflow.** Diff của `pr-check.yml` viết trên bản sau T-077 bản 2 (dòng đánh dấu "T-077" là của plan đó, giữ nguyên):

```diff
 # build-and-test has two paths: ... (T-077: three comment lines about draft PRs)
+# build-and-test runs on the runner the repository variable HEAVY_RUNNER names, else on ubuntu-latest (ADR-0042).
 # Changing this file is a high-risk plan and needs cross-review.
 ...
 jobs:
   build-and-test:
-    runs-on: ubuntu-latest
+    runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}
     if: github.event.pull_request.draft == false        # T-077
     timeout-minutes: 45
 ...
       - uses: actions/checkout@v5
 
+      # A self-hosted runner must be one-shot and clean before anything is set up or built (ADR-0042). Only a check
+      # for configuration mistakes: a PR can change the script.
+      - name: Kiểm runner tự host (scripts/runner-check.sh)
+        if: runner.environment == 'self-hosted'
+        env:
+          REPO_VISIBILITY: ${{ github.event.repository.visibility }}
+        run: scripts/runner-check.sh
+
       - uses: actions/setup-java@v5
```

Không thêm hay đổi `if:` cấp job: `runs-on` theo biểu thức không phải điều kiện cấp job, `pr_job_if_draft` và phần cấp job của `pr_sim` (T-077) chỉ đọc dòng `^    if:`, nên không cần sửa (AI đã chạy `pr_job_if_draft` trên bản sao: `1 yes`). Hai bước lọc của T-076 đứng trước checkout: chúng đọc API, ghi `$GITHUB_OUTPUT`, và trên đường lỗi ghi `build.log` vào thư mục làm việc rồi dừng job đỏ ngay, nên checkout và bước kiểm runner không chạy; container bị xóa sau job. Luật này có test 3.

Diff của `main-guard.yml` (T-077 không sửa file này):

```diff
+# - Jobs guard and ui-test run on the runner the repository variable HEAVY_RUNNER names, else on ubuntu-latest;
+#   steps that use sudo run only on GitHub runners (ADR-0042).
 # Changing this file is a high-risk plan; cross-review required.
 ...
   guard:
     # A run by hand on another branch would open main-broken, push a revert or close an issue for that branch.
     if: github.ref == 'refs/heads/main'
-    runs-on: ubuntu-latest
+    runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}
 ...
       - uses: actions/checkout@v5
         with:
           fetch-depth: 50
 
+      # Even in mode skip, so that a broken self-hosted runner shows at once (ADR-0042).
+      - name: Kiểm runner tự host (scripts/runner-check.sh)
+        if: runner.environment == 'self-hosted'
+        env:
+          REPO_VISIBILITY: ${{ github.event.repository.visibility }}
+        run: scripts/runner-check.sh
+
       - name: Chọn cách kiểm (scripts/main-guard-verified.sh)
 ...
   ui-test:
     needs: guard
     # An empty output (the step ui could not write it) still runs the emulator.
     if: needs.guard.outputs.ui != 'no'
-    runs-on: ubuntu-latest
+    runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}
     timeout-minutes: 60
     steps:
       - uses: actions/checkout@v5
         with:
           # ui-test-verdict.sh compares the commit with its parent; find-breaking-commit.sh walks back from it.
           fetch-depth: 50
 
+      - name: Kiểm runner tự host (scripts/runner-check.sh)
+        if: runner.environment == 'self-hosted'
+        env:
+          REPO_VISIBILITY: ${{ github.event.repository.visibility }}
+        run: scripts/runner-check.sh --emulator
+
       - uses: actions/setup-java@v5
 ...
       - name: Giải phóng đĩa cho emulator
+        if: runner.environment == 'github-hosted'
         run: |
 ...
       - name: Bật KVM cho emulator
+        if: runner.environment == 'github-hosted'
         run: |
```

Thứ tự các bước khác, bước `ui`, `outputs:`, mọi `if:` của ADR-0039, ADR-0043 giữ nguyên. AI đã chạy `mg_wiring`, `ui_wiring`, `mg_own_issues` có sẵn trên bản sao đã sửa đúng như diff trên: `yes` ở mọi điểm như nhánh chính (mục 4.1).

Hệ quả khi bước kiểm runner đỏ: ở `guard`, các bước sau không chạy, bước `Kết quả cổng chất lượng` bị bỏ qua nên `Đánh dấu nhánh chính đang đỏ` (đòi `steps.check.outcome == 'failure'`) không chạy: không mở `main-broken`, như lỗi hạ tầng (checkout, Java). Ở `ui-test`, bước emulator bị bỏ qua (`skipped`), `ui-test-verdict.sh` kết luận `infra` ("Any other outcome ... is infra", test có sẵn `ui-test-verdict: an unknown outcome is infra`), chú thích `EMULATOR HẠ TẦNG (` mà `find-breaking-commit.sh --check ui-test` không đổ lỗi. Log có dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` khớp PB-19. Vì vậy không thêm `runner-check.sh` vào `own_job_changed` của `ui-test-verdict.sh` (hàm đó chỉ xét bước emulator `failure` không có log).

`resume-run.yml`: thêm `actions: read` cấp workflow (dưới `issues: read`) kèm một dòng chú thích "resume-check.py reads the queued Actions runs (ADR-0042)." `runs-on` giữ. `staging.yml`, `device-session.yml`, `env-canary.yml`, `start-run.yml`, `run-watchdog.yml`, `status-page.yml`: không sửa; test 1 kiểm chúng giữ `runs-on: ubuntu-latest`.

**C. Hai danh sách của cổng.**

- `scripts/main-guard-verified.sh`: `GATE_FILES` thêm `scripts/runner-check.sh` ở cuối (job `guard` chạy nó); chú thích ở đầu file ("GATE_FILES, with scripts/ui-test-needed.sh that its step ui runs") thêm "and scripts/runner-check.sh that checks a self-hosted runner, ADR-0042". Sau T-077 dòng `GATE_FILES` vẫn là một mảng ba dòng như hôm nay.
- `scripts/ui-test-needed.sh`: `UI_PATHS` thêm `scripts/runner-check.sh` sau `scripts/ui-test-needed.sh` (job `ui-test` chạy nó với `--emulator`); chú thích đầu file "the scripts the ui-test job runs" đã bao trường hợp này.

**D. `scripts/wait-run.sh`: mã 7, `CHỜ RUNNER`.** Gốc là bản sau T-077 (mã 0 tới 6). Mã 4 giữ nghĩa: lần chạy đã xong mà GitHub không cấp máy.

- Đầu script: `queue_minutes="${WAIT_RUN_RUNNER_QUEUE_MINUTES:-75}"`, không phải số thì `die "WAIT_RUN_RUNNER_QUEUE_MINUTES phải là số phút."` (mã 3).
- Trong vòng, ngay sau khối kiểm xung đột (mã 5) và trước khối hết giờ: khi `run` khác rỗng và `status` khác `completed`, đọc `actions/runs/<id>/jobs?per_page=50` (đường đã đọc ở cuối script) và lọc bằng:

```jq
(.jobs // []) as $j
| if any($j[]; .status == "in_progress") then empty else
  ([$j[] | select(.status == "completed") | .completed_at // empty] | max) as $done
  | [$j[] | select(.status == "queued" and ((.runner_name // "") == "")
      and any(.labels[]?; test("^(ubuntu|windows|macos)-") | not))
    | . + {since: ($done // .created_at)}
    | select((.since | fromdateiso8601) <= ($now - $min * 60))]
  | first // empty | "\(.name)\t\(.labels | join(","))\t\(.since)"
  end
```

  với `--argjson now "${WAIT_RUN_NOW:-$(date -u +%s)}" --argjson min "$queue_minutes"`. Nghĩa là đủ bốn điều của ADR-0042 mục 5: không job nào `in_progress`; một job `queued` chưa có runner; mang ít nhất một nhãn không phải của GitHub; được phép chạy (lúc muộn nhất mà một job của lần chạy xong, hay `created_at` của job khi chưa job nào xong) từ `queue_minutes` phút trở lên. Ra một dòng thì in rồi thoát 7:

  `CHỜ RUNNER: job <tên> của <what>, lần chạy <id>, chờ runner tự host (nhãn <nhãn>) từ <mốc>, không còn job nào của lần chạy đang chạy. Không phải lỗi của code: không sửa code, không chạy lại, không revert; ghi vào "Cần người kiểm tra": máy runner hay biến HEAVY_RUNNER của repo (docs/ops/self-hosted-runner.md, mục 8); phiên làm theo docs/auto-run.md, mục 4 (ADR-0042). <url>`
- Đọc job lỗi hay `jq` lỗi (câu trả lời lạ) thì bỏ qua lượt kiểm này, chờ tiếp như cũ (`|| true`). AI đã chạy biểu thức trên với mười câu trả lời mẫu (mục 4.1).
- Thứ tự kiểm sau T-077 giữ: nháp (mã 6) ngay sau `read_head`, rồi đọc lần chạy, rồi `completed`, rồi xung đột (mã 5), rồi phần mới, rồi hết giờ. Chế độ `--workflow` cũng có phần mới (ví dụ `ui-test` của `main-guard` chờ máy).
- Chú thích mã thoát thêm: `7 a job waits for a self-hosted runner for WAIT_RUN_RUNNER_QUEUE_MINUTES (default 75) and nothing of the run is going: a person must act, not a code failure (ADR-0042)`; dòng biến đè thêm `WAIT_RUN_RUNNER_QUEUE_MINUTES` và `WAIT_RUN_NOW` (giây Unix, test).
- Ngưỡng 75 phút lớn hơn `timeout-minutes: 60` của `ui-test`: máy 2 chỗ dùng chung nhiều repo, một job có thể chờ chính đáng sau một `ui-test` khác. Script không đọc được trạng thái máy (API runner cần quyền quản trị); báo sớm chỉ làm phiên dừng sớm, không phá gì.

**E. `scripts/resume-check.py`: không mở lại phiên khi job chờ runner tự host.**

- Hàm mới `waiting_job()`: với `status` lần lượt `queued`, `in_progress`, đọc `api(f"actions/runs?status={status}&per_page=20")`, rồi với mỗi lần chạy `api(f"actions/runs/{id}/jobs?per_page=50")`. Trả `(tên job, mã lần chạy)` của job đầu tiên có `status == "queued"` và ít nhất một nhãn không khớp `^(ubuntu|windows|macos)-`; không có thì `None`. Câu trả lời không phải object có `workflow_runs` / `jobs` là danh sách thì `raise LookupError(path)`.
- Trong `main()`, sau kiểm `env-broken` (dòng 124 tới 126), trước dòng `since = ...`: có job thì `say(f"Không tiếp tục: job {name} (lần chạy {run_id}) đang chờ runner tự host (ADR-0042).")` rồi `continue`. Không có ngưỡng thời gian: phiên bị coi đứng im sau 90 phút không commit, còn mốc của mã 7 tính từ lúc job được phép chạy; và phiên cũ có thể vẫn đang chờ chính job đó.
- Đọc lỗi thì `LookupError` như mọi lỗi đọc có sẵn: thoát 1, không tiếp tục, không đổi gì; `resume-run.yml` thử lại giờ sau. Đây là đường lỗi mới (mục 3.5). Thiếu quyền `actions: read` cũng rơi vào đây (test 11 chặn).
- Docstring: câu "Nor while main is red (main-broken) or the cloud environment is broken (env-broken)." thêm ", or while a job of the repository waits for a self-hosted runner (ADR-0042)".

**F. Bảng tra lỗi `docs/ops/failure-playbook.md`.** Ba dòng thêm ở cuối bảng, ngay trước `<!-- playbook:end -->`, nguyên văn (mã lấy số kế tiếp lúc làm task; đã có người dùng thì theo ADR-0032/L10):

| Mã | Mẫu | Thấy ở | Loại | Việc phải làm | Nguồn |
| --- | --- | --- | --- | --- | --- |
| PB-17 | `CHỜ RUNNER:` | `wait-run.sh` mã 7: một job chờ runner tự host quá lâu (ADR-0042) | `nguoi` | Máy runner tự host tắt, dịch vụ tạo container dừng hay máy bận quá lâu; không phải lỗi của code. Không sửa code, không chạy lại, không revert: làm theo `auto-run.md` mục 4, dòng "Job chờ runner tự host", ghi vào Cần người kiểm tra. Người bật lại máy hay xóa biến `HEAVY_RUNNER`, rồi hủy và chạy lại lần chạy đang chờ (`docs/ops/self-hosted-runner.md`, mục 8) | dự án mẫu, ADR-0042: chưa gặp thật, log lấy từ đầu ra của `wait-run.sh` |
| PB-18 | `The self-hosted runner: .* lost communication with the server` | chú thích của job nặng chạy trên runner tự host trong `gh run view <id>` | `nguoi` | Máy runner tự host tắt, khởi động lại hay mất mạng giữa job; không phải lỗi của code. Không sửa code, không chạy lại: xử lý như PB-17. Runner của GitHub mất kết nối ghi "The hosted runner", không khớp dòng này | dự án mẫu, ADR-0042: thông báo của GitHub, chưa gặp thật |
| PB-19 | `RUNNER KHÔNG DÙNG ĐƯỢC:` | bước `Kiểm runner tự host (scripts/runner-check.sh)` của job nặng | `nguoi` | Máy hay image của runner tự host cấu hình sai: repo không private, thiếu dấu của container, `~/.gradle` có sẵn tệp, thư mục làm việc có file ngoài git, thiếu đĩa, thiếu KVM hay `ANDROID_HOME` không ghi được. Không sửa code, không chạy lại; chép dòng lỗi vào Cần người kiểm tra; người sửa máy hay xóa biến `HEAVY_RUNNER` (`docs/ops/self-hosted-runner.md`, mục 8). Task đang làm sửa chính `scripts/runner-check.sh` thì là lỗi thật, xử lý như `that` | dự án mẫu, ADR-0042: chưa gặp thật, log lấy từ đầu ra của `runner-check.sh` |

Không có dòng riêng cho hết đĩa giữa job (`No space left on device`): lỗi đó cũng xảy ra trên runner của GitHub, log của bước lỗi không có dấu hiệu chắc chắn của runner tự host, và điều 5 của `runner-check.sh` đã chặn phần lớn trường hợp. Ba dòng không trùng dòng nào có sẵn (AI đã chạy `scripts/failure-kind.sh --playbook` với bảng đã thêm: `CHỜ RUNNER: ...` ra PB-17, câu "The self-hosted runner: ... lost communication" ra PB-18, `RUNNER KHÔNG DÙNG ĐƯỢC: ...` ra PB-19; "The hosted runner: GitHub Actions 2 lost communication", "The runner has received a shutdown signal", `No space left on device`, `EMULATOR HẠ TẦNG (kết quả lạ): x` đều `khong-co`).

Tệp log, mỗi tệp một dòng, vào commit test của bước 3:

- `scripts/tests/playbook/PB-17.log`: dòng `CHỜ RUNNER: ...` mà ca chính của test 7 nhận được, bỏ phần ` u` (đường dẫn lần chạy của dữ liệu test) ở cuối;
- `scripts/tests/playbook/PB-18.log`: `The self-hosted runner: vgl-android-1 lost communication with the server. Verify the machine is running and has a healthy network connection. Anything in your workflow that terminates the runner process, starves it for CPU/Memory, or blocks its network access can cause this error.` (câu của GitHub);
- `scripts/tests/playbook/PB-19.log`: dòng mà ca (c) của test 6 in ra (`RUNNER KHÔNG DÙNG ĐƯỢC: container không có dấu VGL_RUNNER_EPHEMERAL=1 (docs/ops/self-hosted-runner.md).`).

Sau lần thử của người (H5, H6), log thật thay cho tệp nếu khác (PR tài liệu nhỏ).

**G. Tài liệu, nguyên văn** (AI đã áp mọi câu dưới, cùng các câu của T-077 bản 2 ở cùng chỗ, vào một bản sao của nhánh chính và chạy `scripts/history-refs.py`: sạch; đối chứng thêm `L-080` vào tài liệu mới: script báo `docs/ops/self-hosted-runner.md: L-080`):

- `docs/ops/self-hosted-runner.md` (mới), toàn văn:

```markdown
# RUNNER TỰ HOST CHO JOB NẶNG

Tài liệu này dành cho người phụ trách hạ tầng: dựng một máy chạy runner tự host cho các job nặng của CI, chọn máy đó bằng biến `HEAVY_RUNNER` của repo, và quay về runner của GitHub khi máy hỏng (ADR-0042). AI không dựng máy, không đăng ký runner, không đặt hay xóa biến này.

## 1. Khi nào dùng

- Chỉ cho repo private. Repo công khai dùng runner của GitHub: vừa miễn phí, vừa không cho người lạ chạy code trên máy của nhóm qua PR từ fork. `scripts/runner-check.sh` báo lỗi khi repo không phải private.
- Job lên máy khi biến được đặt: `build-and-test` của `pr-check`, `guard` và `ui-test` của `main-guard`. Chúng khai báo `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`.
- Job giữ runner của GitHub: job đọc bí mật (`staging`, `device-session`, `env-canary`, `start-run`, `resume-run`) và job nhẹ (lịch định kỳ, `status-page`, `run-watchdog`).
- Hàng rào an toàn là cài đặt phía GitHub (mục 5) và container chạy một lần (mục 3, mục 4). `scripts/runner-check.sh` chỉ bắt cấu hình nhầm, vì một PR sửa được chính script đó.

## 2. Máy

- Linux x86_64 có KVM: máy chủ riêng, hay máy ảo có bật ảo hóa lồng. Emulator x86_64 của job `ui-test` cần KVM, nên Mac Apple Silicon không dùng được.
- Đề xuất: 8 nhân, 32 tới 64 GB RAM, 500 GB SSD, Ubuntu LTS, Docker. Hai container chạy cùng lúc; một `ui-test` dùng khoảng 4 nhân và 8 GB RAM.
- Luật udev trên máy chủ: `/dev/kvm` thuộc nhóm `kvm`, quyền `0660`.

## 3. Image của container

- Nền là image runner chính thức của GitHub (`ghcr.io/actions/actions-runner`).
- Người dùng của runner không có `sudo`. Image chính thức cho người dùng `runner` dùng `sudo` không cần mật khẩu, nên image của nhóm gỡ quyền đó.
- Android SDK với các gói mà `scripts/cloud-setup.sh` cài, cộng gói `emulator` và `system-images;android-34;google_apis;x86_64` cài sẵn. `ANDROID_HOME` trỏ tới SDK đó, và người dùng của runner ghi được vào đó.
- Công cụ mà workflow và script gọi: `git`, `gh`, `jq`, `python3`, `curl`, `unzip`.
- `ENV VGL_RUNNER_EPHEMERAL=1`: dấu cho biết container dựng theo tài liệu này. `scripts/runner-check.sh` đòi dấu này; dấu không bảo đảm runner chạy một lần.
- Không đặt sẵn gì trong `~/.gradle` (`init.d/`, `init.gradle`, `init.gradle.kts`, `gradle.properties`).

## 4. Dịch vụ tạo runner

- Một dịch vụ `systemd` chạy N bản (đề xuất 2) của cùng một vòng lặp:
  1. xin cấu hình runner chạy một lần (just-in-time): `POST /orgs/<tổ chức>/actions/runners/generate-jitconfig` với nhãn `vgl-android`, hay `POST /repos/<tổ chức>/<repo>/actions/runners/generate-jitconfig` khi đăng ký runner cho một repo;
  2. chạy `docker run --rm --device /dev/kvm --group-add <gid của nhóm kvm trên máy chủ>` với cấu hình đó, có giới hạn CPU và RAM, không mount socket Docker;
  3. container thoát sau một job thì quay lại bước 1.
- Token xin cấu hình là fine-grained token chỉ có quyền "Self-hosted runners: write" của tổ chức, hay "Administration: write" của repo khi đăng ký cấp repo. Lưu trong tệp chỉ người dùng của dịch vụ đọc được; token không bao giờ vào container.

## 5. Cài đặt GitHub, bắt buộc trước khi đặt biến

1. Nhóm runner không cho repo công khai dùng runner: kiểm ở Organization settings, Actions, Runner groups và ghi lại kết quả. Gói Free không chỉnh được điều này thì đăng ký runner ở cấp repo.
2. Tắt chạy workflow từ fork của repo private (Settings, Actions, phần Fork pull request workflows, ở tổ chức và ở repo).
3. Rồi mới đặt biến ở từng repo app private: Settings, Secrets and variables, Actions, Variables; hay `gh variable set HEAVY_RUNNER --body vgl-android --repo <tổ chức>/<repo>`. Không dùng biến cấp tổ chức: gói Free không cho repo private đọc biến cấp tổ chức.

## 6. Kiểm sau khi dựng

Ghi lệnh, mã lần chạy và kết quả của từng bước vào nhật ký của plan đã dựng máy.

1. Runner nhận job: mở một PR code nhỏ (không nháp: PR nháp không chạy `pr-check`). `gh api "repos/<tổ chức>/<repo>/actions/runs/<id>/jobs" --jq '.jobs[] | {name, labels, runner_name}'` cho `build-and-test` có `labels` là `["vgl-android"]` và `runner_name` là tên container; log có dòng `RUNNER DÙNG ĐƯỢC`; job xanh. Merge PR đó: `guard` của commit merge kiểm như vậy.
2. Emulator: một commit đổi code app trên nhánh chính. `ui-test` chạy trên `vgl-android` sau `guard` và xanh; log có dòng `RUNNER DÙNG ĐƯỢC` của `--emulator`.
3. Cách ly giữa job: trên một PR thử không merge, thêm tạm vào `build-and-test` một bước ghi `~/.gradle/init.d/probe.gradle`. Rồi chạy một job nặng khác: bước kiểm runner của job sau vẫn in `RUNNER DÙNG ĐƯỢC` (container mới). Đóng PR thử.
4. Quay về runner của GitHub: `gh variable delete HEAVY_RUNNER --repo <tổ chức>/<repo>`, rồi `gh workflow run main-guard.yml --repo <tổ chức>/<repo> --ref main`. Job có `labels` là `["ubuntu-latest"]`, `runner_name` bắt đầu bằng `GitHub Actions`, bước kiểm runner bị bỏ qua, bước bật KVM chạy khi `ui-test` chạy. Đặt lại biến.
5. Máy tắt: dừng dịch vụ tạo runner, đẩy một PR code. `WAIT_RUN_RUNNER_QUEUE_MINUTES=5 scripts/wait-run.sh --pr <số>` trả mã 7 (`CHỜ RUNNER`); `python3 scripts/resume-check.py` ghi "đang chờ runner tự host" khi có danh sách chạy `running`. Dừng một container giữa job đang chạy và ghi lại dòng log của GitHub cho bảng tra lỗi. Rồi làm như mục 8, ghi lại lần chạy lại nhận nhãn mới hay giữ nhãn cũ.
6. Sau vài ngày: bảng tính tiền của tổ chức cho thấy phút `Actions Linux` của repo giảm tương ứng với các job đã lên máy.

## 7. Vận hành

- Dọn image cũ (`docker image prune`), cập nhật image khi runner hay SDK đổi, xem đĩa trống.
- Job chờ máy: `scripts/wait-run.sh` trả mã 7 (`CHỜ RUNNER`) khi một job chờ runner tự host quá `WAIT_RUN_RUNNER_QUEUE_MINUTES` phút (mặc định 75) mà không còn job nào của lần chạy đang chạy. `scripts/resume-check.py` không tiếp tục phiên tự chạy khi repo còn job chờ runner tự host.
- Issue `session-stalled` có sẵn lệnh tiếp tục phiên. Trước khi dán lệnh đó, xem hàng chờ: `gh api "repos/<tổ chức>/<repo>/actions/runs?status=queued"`, rồi `jobs` của từng lần chạy. Có job mang nhãn tự host thì phiên cũ có thể vẫn đang chờ máy: sửa máy hay xóa biến trước, nếu không sẽ có hai phiên chồng nhau.

## 8. Máy hỏng

1. Xóa biến: `gh variable delete HEAVY_RUNNER --repo <tổ chức>/<repo>`.
2. Hủy lần chạy đang chờ rồi chạy lại: Re-run, `gh workflow run`, hay đẩy một commit mới khi lần chạy lại vẫn giữ nhãn cũ.
3. Sửa hay bật lại máy, kiểm theo mục 6, rồi đặt lại biến.

## 9. Ngoài phạm vi

- Runner cho bản production và `scripts/release.sh` (ADR-0009, ADR-0017).
- `staging`, `device-session`: đọc bí mật, giữ runner của GitHub.
```

- `docs/ops/staging-and-runs.md`: sau đoạn dòng 5 ("Bí mật thêm ở ... Biến thường thêm ở tab **Variables** cùng trang."), thêm một đoạn: "Biến `HEAVY_RUNNER` (tùy chọn, chỉ repo private) chọn runner tự host cho các job nặng của CI; không đặt thì mọi job chạy trên runner của GitHub. Dựng máy và đặt biến theo `docs/ops/self-hosted-runner.md`."
- `docs/auto-run.md`:
  - bước 8, mục Chờ workflow: "6 PR là bản nháp (`NHÁP`)." (câu của T-077) thành "6 PR là bản nháp (`NHÁP`), 7 job đang chờ runner tự host (`CHỜ RUNNER`)."
  - bước 8, sau ý "Mã 6 (`NHÁP`): ..." của T-077 (hay sau ý mã 5 nếu ý mã 6 nằm chỗ khác), thêm ý: "- Mã 7 (`CHỜ RUNNER`): một job chờ runner tự host quá lâu mà không còn job nào của lần chạy đang chạy (ADR-0042); máy runner tắt hay bận, không phải lỗi của code. Không sửa code, không chạy lại, không revert, không tính vào 3 lần sửa: làm theo mục 4, dòng "Job chờ runner tự host"."
  - bước 9, sau câu "Lần chờ này trả mã 4 thì nhánh chính chưa được kiểm, không phải đỏ: không revert (ADR-0025 chỉ áp dụng khi có issue `main-broken`), làm theo mục 4, dòng "GitHub không chạy job"." thêm "Mã 7 cũng chưa phải đỏ: làm theo mục 4, dòng "Job chờ runner tự host"."
  - mục 4, bảng điều kiện kết thúc, dòng mới ngay sau dòng "GitHub không chạy job (`wait-run.sh` mã 4)": "| Job chờ runner tự host (`wait-run.sh` mã 7, ADR-0042) | Đẩy hết những gì đang có: nhánh của task đang làm, nhánh báo cáo với dòng thời gian ghi giờ, tên workflow, mã lần chạy và mã 7. Ghi vào Cần người kiểm tra: máy runner tự host hay biến `HEAVY_RUNNER` của repo (`docs/ops/self-hosted-runner.md`, mục 8). Không chạy lại, không bắt đầu task mới; giữ danh sách chạy `running`, plan và ticket giữ trạng thái. `resume-check.py` không tiếp tục phiên khi repo còn job chờ runner tự host |"
  - mục Tiếp tục phiên sau khi hết hạn mức, ý 1: "lần chờ dài nhất (CI, emulator) dưới 40 phút." thành "lần chờ dài nhất (CI, emulator) dưới 40 phút; trên runner tự host một job có thể chờ máy lâu hơn, và khi đó ý 3 giữ phiên lại."
  - cùng mục, ý 3: "nhánh chính đỏ (`main-broken`), môi trường lỗi (`env-broken`)." thành "nhánh chính đỏ (`main-broken`), môi trường lỗi (`env-broken`), repo còn job chờ runner tự host (`queued`, nhãn không phải của GitHub, ADR-0042)."
  - cùng mục, Phiên tiếp tục làm như sau, bước 3: sau câu "...lần chạy `main-guard` thì kết thúc phiên, báo cáo ghi lần chạy cần người chạy lại." thêm "Dòng thời gian ghi mã 7 (job chờ runner tự host): đọc lại lần chạy đó, `gh api "repos/{owner}/{repo}/actions/runs/<id>" --jq .status`. Còn `queued` hay `in_progress`: kết thúc phiên như lần trước, không chạy lại. Đã `completed`: xử lý kết quả như thường (bước 8). Bị hủy: chạy lại một lần như mã 4."
- `.claude/skills/merge-pr/SKILL.md`: bước 2, sau câu mã 6 của T-077 ("Mã 6 (PR nháp, `pr-check` không chạy): báo dev; người chuyển PR sang sẵn sàng rồi gọi lại.") thêm "Mã 7 (`CHỜ RUNNER`, job chờ runner tự host): không chạy lại, không sửa code; báo dev kiểm máy runner hay xóa biến `HEAVY_RUNNER` của repo (`docs/ops/self-hosted-runner.md`, mục 8)."; bước 4, sau "PR chỉ đổi tài liệu thì `main-guard` không chạy." thêm "Lần chờ này trả mã 7: như bước 2."
- `CLAUDE.md`, mục Lệnh, dòng mới ngay sau dòng "Kiểm thiết lập của app ... `scripts/setup-check.sh`; danh sách file ở `docs/ops/required-assets.txt`.": "- Kiểm máy runner tự host trước khi build (job nặng của CI tự gọi, chỉ trên runner tự host): `scripts/runner-check.sh [--emulator]`; dựng máy và chọn máy bằng biến `HEAVY_RUNNER` của repo theo `docs/ops/self-hosted-runner.md` (ADR-0042)."
- `docs/foundation.md`: không đổi (ADR-0042 đã `accepted`, `scripts/foundation.py --check` khớp ngày 2026-11-08); sinh lại chỉ khi rà soát độc lập thấy dòng quy tắc của ADR-0042 phải sửa.

Câu mới trong script và workflow (thông báo, chú thích) không phải tài liệu quy tắc; vẫn chỉ dẫn ADR, không thêm mã bài học hay ticket.

**H. `scripts/tests/run.sh`: hàm phụ trợ và dữ liệu** (vào commit test của bước 3, cùng các câu của mục 4 và 3.5, để test viết trước thất bại đúng lý do):

- `pr_sim` (bản sau T-077): `import os, re, sys` (T-077 đã thêm `os`); ngay trước dòng `out = ["code=%s release=%s" ...]`: `runner = os.environ.get("PR_SIM_RUNNER", "github-hosted")`, giá trị khác `github-hosted`, `self-hosted` thì in `ERROR PR_SIM_RUNNER lạ: <giá trị>` và thoát; trong vòng các bước, ngay sau dòng thay `steps.filter.outputs.*`: `expr = re.sub(r"\brunner\.environment\b", repr(runner), expr)`. Phần còn lại giữ: điều kiện có tên khác (`runner.os`, `runner.enviroment`) vẫn còn lại sau khi thay, nên vẫn `ERROR điều kiện lạ`. Chú thích của hàm thêm "PR_SIM_RUNNER (github-hosted by default, or self-hosted) stands for runner.environment". `pr_sim_summary` không đổi (không thêm trường).
- Hàm mới `pr_sim_has <workflow> <step name> <path>...`: in số dòng của đầu ra `pr_sim` đúng bằng tên bước (`grep -cxF`), hay dòng `ERROR` khi có.
- Hàm mới, đặt sau test `pr-check: has no emulator job`: `job_kind <workflow> <job>` (in `secret` khi phần job, bỏ dòng `^[[:space:]]*#`, có `secrets.`; `heavy` khi có `scripts/check.sh`, `./gradlew` hay `android-emulator-runner` mà không có `secrets.`; `light` khi khác); `wf_jobs <workflow>` (tên các job của khối `jobs:`, như `pr_jobs`); `heavy_jobs [<thư mục>]` (mọi cặp `<tệp> <job>` loại `heavy` trong `<thư mục>/*.yml`, mặc định `.github/workflows`); `runner_order <workflow> <job>` (test 3). Chúng dùng `wf_job` (dòng 2177), nên khối test 1 tới 5 đặt sau định nghĩa `wf_job` (sau câu `status-page: a separate job enables the schedules`), hay `wf_job` được dời lên trước; chọn đặt khối sau.
- `resume_check` (dòng 1868): heredoc thêm hai đường `"actions/runs?status=queued&per_page=20": ${RESUME_QUEUED:-<rỗng>}` và `"actions/runs?status=in_progress&per_page=20": ${RESUME_IN_PROGRESS:-<rỗng>}`, với `<rỗng>` là một biến cục bộ `{"total_count": 0, "workflow_runs": []}`, và thêm `${RESUME_ROUTES:-}` (các đường thêm, bắt đầu bằng dấu phẩy) trước dấu đóng của `routes`; thêm khóa `errors` lấy từ `RESUME_ERRORS` (mặc định `{}`, qua một biến cục bộ). Không đặt biến nào thì dữ liệu như nay cộng hai câu trả lời rỗng.
- Dữ liệu `mgv_gate_files` (dòng 685) thêm `scripts/runner-check.sh`; `uin_code` (dòng 1361) thêm `scripts/runner-check.sh` (test 13, 14).

Không sửa: `scripts/merge-if-green.sh` (cấm sửa; không thêm dòng `  return 3` nào), `scripts/check.sh`, `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh`, `scripts/ui-test-verdict.sh`, `scripts/tests/fake-gh` (mọi đường mới là đường đọc có tên cố định), `scripts/release.sh`.

**Bước của người, ngoài phiên** (phiên không có quyền quản trị repo, `docs/ops/session-capabilities.md`; ADR-0042 cấm phiên cài runner hay đặt biến):

- H1. Repo nào đặt biến: chỉ repo app private (repo dự án mẫu đang công khai thì không, mục 8 câu 4).
- H2. Thuê máy (đã quyết ở bản 1: máy chủ riêng Linux x86_64 có KVM).
- H3. Sau khi task merge, dựng máy theo `docs/ops/self-hosted-runner.md` mục 2 tới 5. Điều kiện bắt buộc trước H4: đã xác minh nhóm runner không cho repo công khai, hay đã dùng runner cấp repo; ghi kết quả vào nhật ký.
- H4 tới H6: mục 4, dòng kiểm tra thủ công (tài liệu mới, mục 6, cùng các bước).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/runner-check.sh` | Kiểm cấu hình runner tự host trước khi build (mục 3.1 A); bit chạy `100755` |
| Sửa | `.github/workflows/pr-check.yml` | `runs-on` của `build-and-test`; bước kiểm runner sau checkout; một dòng chú thích (mục 3.1 B) |
| Sửa | `.github/workflows/main-guard.yml` | `runs-on` của `guard` và `ui-test`; bước kiểm runner sau checkout của hai job; `if: runner.environment == 'github-hosted'` cho hai bước `sudo`; hai dòng chú thích (mục 3.1 B) |
| Sửa | `.github/workflows/resume-run.yml` | `permissions:` cấp workflow thêm `actions: read`, một dòng chú thích (mục 3.1 B) |
| Sửa | `scripts/main-guard-verified.sh` | `GATE_FILES` thêm `scripts/runner-check.sh`; chú thích (mục 3.1 C) |
| Sửa | `scripts/ui-test-needed.sh` | `UI_PATHS` thêm `scripts/runner-check.sh` (mục 3.1 C) |
| Sửa | `scripts/wait-run.sh` | Mã 7 `CHỜ RUNNER`; kiểm `WAIT_RUN_RUNNER_QUEUE_MINUTES`; chú thích (mục 3.1 D) |
| Sửa | `scripts/resume-check.py` | Không tiếp tục khi job chờ runner tự host; docstring (mục 3.1 E) |
| Sửa | `scripts/tests/run.sh` | Test 1 tới 14 của mục 4; `pr_sim`, `pr_sim_has`, `job_kind`, `wf_jobs`, `heavy_jobs`, `runner_order`, `resume_check`, dữ liệu `mgv_gate_files`, `uin_code` (mục 3.1 H, mục 3.5) |
| Sửa | `docs/ops/failure-playbook.md` | PB-17, PB-18, PB-19 (mục 3.1 F) |
| Tạo | `scripts/tests/playbook/PB-17.log` | Đầu ra `wait-run.sh` mã 7 |
| Tạo | `scripts/tests/playbook/PB-18.log` | Câu GitHub báo runner tự host mất kết nối |
| Tạo | `scripts/tests/playbook/PB-19.log` | Đầu ra `runner-check.sh` |
| Tạo | `docs/ops/self-hosted-runner.md` | Dựng và vận hành máy runner, toàn văn ở mục 3.1 G |
| Sửa | `docs/ops/staging-and-runs.md` | Một đoạn về biến `HEAVY_RUNNER` (mục 3.1 G) |
| Sửa | `docs/auto-run.md` | Bước 8, 9: mã 7; mục 4: dòng Job chờ runner tự host; mục Tiếp tục phiên ý 1, ý 3, bước 3 (mục 3.1 G) |
| Sửa | `.claude/skills/merge-pr/SKILL.md` | Mã 7 của `wait-run.sh` (mục 3.1 G) |
| Sửa | `CLAUDE.md` | Mục Lệnh: một dòng `runner-check.sh` (mục 3.1 G) |
| Sửa | `docs/adr/0042-runner-tu-host-cho-job-nang.md` | Chỉ khi rà soát độc lập thấy ADR lệch với code đã làm; không đổi `status` |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` chỉ khi dòng quy tắc của ADR-0042 đổi |
| Sửa | `docs/features/quy-trinh/plans/T-078-runner-tu-host.md` | Nhật ký thực hiện; kết quả H3 tới H6 sau này |
| Sửa | `docs/features/quy-trinh/tickets/T-078-runner-tu-host.md` | Trạng thái ticket |

Nơi đọc file bị đổi, tìm ngày 2026-11-08 bằng `grep -rln -e pr-check.yml -e main-guard.yml -e resume-run.yml -e wait-run.sh -e resume-check.py -e main-guard-verified.sh -e ui-test-needed.sh -e failure-playbook scripts .github .claude docs/ops docs/auto-run.md CLAUDE.md`:

- `scripts/main-guard-verified.sh` đọc tên job `build-and-test` và bước `Cổng chất lượng (scripts/check.sh)` của `pr-check.yml` (không đổi tên); `GATE_FILES` có `pr-check.yml`, `main-guard.yml`: commit merge của task chạy `full`, đúng ý.
- `scripts/merge-if-green.sh` đọc check run `build-and-test`: không đổi tên, không sửa. Chỉ in lệnh `wait-run.sh`, không xử lý mã thoát.
- `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh` đọc check run `guard`, `ui-test`: không đổi tên, không thêm `if:` cấp job. `main-guard-status.sh` dòng 9, 56, 101 chỉ nhắc lệnh `wait-run.sh --since`. Không sửa.
- `scripts/ui-test-verdict.sh` đọc `main-guard.yml` qua `own_job_changed` (commit đổi file đó mà emulator không có log là đỏ: rủi ro đã chấp nhận ở ADR-0043) và mã PB trong `INFRA_PB` (PB-01, PB-11, PB-13, PB-15): plan không đổi mã cũ, mã mới không vào `INFRA_PB`. Không sửa (mục 3.1 B, hệ quả khi bước kiểm runner đỏ). `scripts/ui-test-on-emulator.sh` chỉ nhắc `main-guard.yml` trong chú thích.
- `scripts/failure-kind.sh --playbook` đọc bảng theo thứ tự dòng: ba mẫu mới không trùng dòng nào (mục 3.1 F); test 8.
- `.github/workflows/resume-run.yml` gọi `resume-check.py`: đầu ra và mã thoát giữ nguyên nghĩa; quyền `actions: read` thêm ở cùng PR.
- `scripts/check.sh`, `scripts/plan-check.py`, `scripts/push-probe.sh`, `scripts/is-exact-revert.sh`, `scripts/cloud-env-check.sh`: chỉ nhắc tên workflow, script hay bảng tra lỗi trong chú thích, thông báo hay danh sách tên file rủi ro cao. Không sửa.
- `scripts/tests/playbook/PB-02.log` có tên `pr-check.yml`: log mẫu, không đổi.
- `.claude/skills/quick-lane/SKILL.md`, `.claude/skills/whatnow/SKILL.md`, `docs/ops/release.md`, `docs/ops/setup-new-app.md`, `docs/ops/pre-run-review.md`, `docs/ops/test-sessions.md`, `docs/ops/token-usage.md`, `docs/ops/quick-lane.md`, `docs/ops/session-capabilities.md`, `docs/ops/to-app.md`: nhắc `pr-check`, `main-guard` hay `wait-run.sh` mà không nói máy chạy hay mã 7. Không sửa.
- Workflow không sửa mà test 1 đọc: `.github/workflows/status-page.yml`, `.github/workflows/start-run.yml`, `.github/workflows/device-session.yml`, `.github/workflows/staging.yml`, `.github/workflows/run-watchdog.yml`, `.github/workflows/env-canary.yml`.
- `scripts/template-sync.sh` chép workflow và script về app; biến đặt riêng ở từng repo.
- Test "every skill names ... real files" đọc đường dẫn trong skill `merge-pr`: `docs/ops/self-hosted-runner.md` có trong cùng commit.
- `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` nhắc `scripts/tests/run.sh` trong chú thích dòng 87: không bị ảnh hưởng.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung phần bị đổi; plan không đổi tên, xóa hay đánh số lại mục nào, chỉ thêm câu, dòng hay mã thoát:

- dẫn `docs/auto-run.md`: `.github/workflows/device-session.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/push-on-commit.sh`, `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/token-usage.py`;
- dẫn `CLAUDE.md`: `.github/ISSUE_TEMPLATE/from-app.yml`, `scripts/new-app.sh` (thay tên app, dòng mới không có tên app), `scripts/local-setup.sh`, `scripts/run-watchdog.py`, `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt`;
- dẫn `docs/ops/staging-and-runs.md`: `scripts/env-canary.sh`, `scripts/setup-check.sh`; plan chỉ thêm một đoạn sau dòng 5;
- dẫn `docs/ops/failure-playbook.md`: `scripts/push-probe.sh` (PB-14);
- `scripts/local-setup.sh` nhắc `scripts/tests/run.sh`: không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN`: chỉ thêm `actions: read` (chỉ đọc) cấp workflow cho `resume-run.yml`; mọi quyền khác giữ nguyên; không thêm bí mật. Biến mới `HEAVY_RUNNER` (cấp repo, không bí mật) do người đặt.
- **Thư viện và SDK:** Không thêm. Action có sẵn, không đổi phiên bản: `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`, `reactivecircus/android-emulator-runner@v2`, `dorny/paths-filter@v3`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** ba action chạy trong container không có `sudo` chưa chạy thử được vì chưa có máy: `setup-java`, `setup-gradle` cài vào thư mục người dùng; `android-emulator-runner` ghi vào `ANDROID_HOME` và cần `/dev/kvm`. Phần này chỉ lộ ra ở H5, sau khi task merge; trong phiên mọi job chạy trên runner của GitHub.
- **Điều kiện chuyển:** ở H5, job nặng đỏ trên runner tự host ở một bước cài đặt hay khởi động emulator, trong khi cùng commit xanh trên runner của GitHub.
- **Cách làm thay:** sửa image hay cấu hình container theo `docs/ops/self-hosted-runner.md`, không sửa workflow: cài sẵn JDK 17, đặt `RUNNER_TOOL_CACHE` ghi được, cài sẵn gói emulator và ảnh hệ thống, kiểm `--group-add`. Không sửa được bằng image thì xóa biến và mở ticket mới. Tiêu chí và test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Mục này ghi các câu khẳng định có sẵn của `scripts/tests/run.sh` mà thay đổi chạm tới (ADR-0031). Không câu khẳng định nào phải viết lại; ba chỗ chỉ thêm dữ liệu cho phần plan thêm (ADR-0029), vào commit test của bước 3.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `pr_sim` chỉ hiểu điều kiện của bộ lọc | Hiểu thêm `runner.environment`, mặc định `github-hosted` (mục 3.1 H) | P6 của T-076 (các câu `pr-check: a PR touching ...`, `a docs-only PR ...`, `a PR changing a build file ...`, bản sao P6d), test 28, 29 của T-077 | Giữ nguyên. Trên workflow mới, với giá trị mặc định, bước kiểm runner bị bỏ qua nên mọi tóm tắt không đổi (đã chạy thử: `code=true release=false gate=yes gradle=yes docs=no r8=no` cho `app/src/main/java/A.kt`, `code=false ... docs=yes` cho `docs/a.md README.md`). Không có phần mở rộng thì mọi câu P6 in `ERROR điều kiện lạ` (điểm chặn của bản 1) |
| `pr-check.yml`: `runs-on: ubuntu-latest`, không có bước sau checkout ngoài `setup-java` | `runs-on` theo biến, bước kiểm runner sau checkout | `pr-check: checkout and Java run for every PR`, `the docs-only path ... uses no Gradle`, `the docs-only step checks python3, jq and java`, `has exactly one job`, `the path filter and its check come before every step that reads them` (`yes yes yes yes 6`), `every filter output read is defined` (`other=0`), `pr_job_if_draft` của T-077 (`1 yes`) | Giữ nguyên: đã chạy từng hàm trên bản sao có thay đổi của T-077 và T-078, kết quả như mong đợi (mục 4.1). Bước kiểm runner không đọc bộ lọc, không có chữ `scripts/check.sh`, `gradlew`, `ANDROID_HOME` |
| `main-guard.yml`: hai job `runs-on: ubuntu-latest`; hai bước `sudo` không `if:` | `runs-on` theo biến; bước kiểm runner; `if: runner.environment == 'github-hosted'` | `main-guard: chooses the mode before the gate ...` (`mg_wiring`, điểm (b): checkout trước bước `mode`), `ui-test runs after guard only for app code` (`ui_wiring`), `guard comments on and closes only its own main-broken issues` | Giữ nguyên: đã chạy trên bản sao, mọi điểm `yes` |
| Commit đổi `scripts/runner-check.sh` (chưa có) | Nằm trong `GATE_FILES` | `main-guard-verified: a commit changing a file the guard job runs is full` (đếm theo `mgv_gate_files`) | Câu giữ (số bằng độ dài mảng); dữ liệu `mgv_gate_files` thêm `scripts/runner-check.sh` (test 13) |
| Commit đổi `scripts/runner-check.sh` | Nằm trong `UI_PATHS` | `ui-test-needed: a commit changing app code is yes` (đếm theo `uin_code`) | Câu giữ; dữ liệu `uin_code` thêm `scripts/runner-check.sh` (test 14) |
| `wait-run.sh` chờ lần chạy chưa xong tới hết giờ | Lần chạy chưa xong mà có job chờ runner tự host đủ lâu → mã 7 | Các câu `wait-run:` có sẵn, kể cả test 3, 4 của T-077 | Giữ nguyên: dữ liệu của chúng không có job `queued` mang nhãn tự host (`no_jobs`, `ui_red_jobs` không có `status`), hay không có đường `jobs` (đọc lỗi thì bỏ qua lượt kiểm) |
| `resume-check.py` tiếp tục phiên sau 90 phút không commit | Đọc thêm `actions/runs?status=queued` và `in_progress`; đọc lỗi thì thoát 1 | Sáu câu `resume-check:` (dòng 1885 tới 1900) | Câu giữ; dữ liệu `resume_check` thêm hai câu trả lời rỗng `{"total_count": 0, "workflow_runs": []}` (mục 3.1 H), cả cho câu trang 2 dùng lại `$work/resume.json`. Không có hai đường đó, câu "a running session silent for 2 hours is continued" và câu trang 2 đỏ (đường lỗi mới: thoát 1) |
| `resume-run.yml` cấp workflow: `contents: read`, `issues: read` | Thêm `actions: read` | `workflows: resume-run and run-watchdog disable themselves when idle` (`si_idle_job`: không có `actions: write` cấp workflow), `the jobs with secrets or issue writes keep their permissions` | Giữ nguyên: `actions: read` không khớp `actions: write` |
| Bảng tra lỗi tới PB-16 | Thêm PB-17 tới PB-19 | `playbook: every row has a log file and every log file a row`, `every pattern of the real table is valid` | Giữ nguyên: ba tệp log vào commit test (câu đầu đỏ tới khi ba dòng vào bảng ở commit code, đúng lý do) |

Đường lỗi mới: `resume-check.py` thoát 1 khi đọc `actions/runs` hay `jobs` lỗi, kể cả khi trước đây nó đã tiếp tục phiên. Thoát 1 nghĩa là không tiếp tục, không đổi gì; `resume-run.yml` thử lại giờ sau.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`; `gh` giả `scripts/tests/fake-gh` với `FIXTURE` (`routes`, `errors`, `__sequence__`). Các test cấu trúc chạy cả trên bản sao hỏng trong `$work/rwf/` để chứng minh test bắt được lỗi.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phạm vi 2: job nặng không bí mật đọc biến; job có bí mật và job nhẹ giữ runner của GitHub | Test cấu trúc | 1. `runners: heavy jobs read HEAVY_RUNNER, every other job stays on ubuntu-latest`. Với mọi job của mọi `.github/workflows/*.yml`: loại `heavy` có đúng một dòng `    runs-on: ${{ vars.HEAVY_RUNNER \|\| 'ubuntu-latest' }}`; loại `secret` và `light` có đúng một dòng `    runs-on: ubuntu-latest`. Mong `yes`. Bản sao hỏng (thư mục `$work/rwf/a`, `b` chép mọi workflow rồi sửa một tệp): (a) `staging.yml` dùng `HEAVY_RUNNER`; (b) `guard` giữ `ubuntu-latest`; mong `no` cả hai. Thêm: `job_kind` của một job tạm chỉ có `./gradlew` trong dòng chú thích là `light` |
| Phạm vi 2: tập job nặng đúng | Test cấu trúc | 2. `runners: the heavy jobs are build-and-test, guard and ui-test`: `heavy_jobs` in đúng ba dòng `main-guard.yml guard`, `main-guard.yml ui-test`, `pr-check.yml build-and-test` (sắp xếp). Đối chứng: test 1 qua trên một thư mục chỉ có `status-page.yml` (không job nặng), còn test 2 thất bại trên thư mục đó |
| ADR-0042 mục 3: job nặng kiểm runner trước mọi bước cài hay build | Test cấu trúc | 3. `runners: every heavy job checks the runner right after checkout` (chi tiết dưới bảng) |
| Phạm vi 3: bước `sudo` chỉ trên máy của GitHub | Test cấu trúc | 4. `runners: sudo steps of heavy jobs run only on GitHub runners`: mọi bước có `sudo ` (trong `run:`, bỏ dòng chú thích) của job nặng có dòng `if:` chứa `runner.environment == 'github-hosted'`. Mong `yes`. Bản sao bỏ `if:` của bước `Bật KVM cho emulator`: `no`. Đối chứng: `device-session.yml` (job `secret`) giữ bước `sudo` không `if:` mà test vẫn `yes` |
| Phạm vi 4: thư mục làm việc sạch | Test cấu trúc | 5. `runners: heavy jobs keep the default clean checkout`: không job nặng nào có `clean: false`. Bản sao thêm `clean: false` vào `with:` checkout của `guard`: mong thất bại |
| `runner-check.sh` bắt cấu hình nhầm | Test của script | 6. `runner-check: ...` (chi tiết dưới bảng) |
| ADR-0042 mục 5: job chờ runner tự host | Test của script | 7. `wait-run: a job waiting for a self-hosted runner with nothing running is CHỜ RUNNER` (chi tiết dưới bảng) |
| Bảng tra lỗi | Test của script | 8. `playbook: PB-17 waiting for a self-hosted runner -> nguoi`, `playbook: PB-18 a self-hosted runner lost communication -> nguoi`, `playbook: PB-19 runner not usable -> nguoi` (`pb_head` trên ba tệp log). Đối chứng, mỗi tệp tạm một dòng, mong `khong-co`: `The hosted runner: GitHub Actions 2 lost communication with the server.`; `The runner has received a shutdown signal. This can happen when the runner service is stopped, or a manually started runner is canceled.`; `No space left on device` |
| Tệp log khớp đầu ra thật | Test của script | 9. `playbook: PB-17.log is what wait-run.sh prints` (dòng đầu của `PB-17.log` bằng dòng của ca chính test 7, bỏ ` u` ở cuối) và `playbook: PB-19.log is what runner-check.sh prints` (bằng dòng ca (c) của test 6) |
| Không mở lại phiên khi job chờ runner tự host | Test của script | 10. `resume-check: no resume while a job waits for a self-hosted runner` (chi tiết dưới bảng) |
| `resume-run.yml` đọc được lần chạy | Test cấu trúc | 11. `resume-run: the workflow may read Actions runs`: `wf_level .github/workflows/resume-run.yml` có dòng `  actions: read` và không có `actions: write`. Bản sao thiếu dòng: thất bại |
| Mô phỏng PR mẫu hiểu bước kiểm runner (điểm chặn 1) | Test PR mẫu | 12. `pr-check: the sample PR runs the runner check only on a self-hosted runner` (chi tiết dưới bảng) |
| `GATE_FILES` có `runner-check.sh` (điểm chặn 2) | Test của script | 13. Câu có sẵn `main-guard-verified: a commit changing a file the guard job runs is full` với `mgv_gate_files` thêm `scripts/runner-check.sh`: mong 10, trước code ra 9 |
| `UI_PATHS` có `runner-check.sh` (điểm chặn 2) | Test của script | 14. Câu có sẵn `ui-test-needed: a commit changing app code is yes` với `uin_code` thêm `scripts/runner-check.sh`: mong 15, trước code ra 14 |
| DoD 4: không đổi gì khi chưa có biến, trên GitHub thật | Kiểm trong phiên (lệnh `gh api` trên GitHub thật) | Trước khi gọi `merge-if-green.sh` cho PR của task: `scripts/wait-run.sh --pr <số>` xanh, rồi `gh api "repos/{owner}/{repo}/actions/runs/<id>/jobs?per_page=50" --jq '.jobs[] \| {name, labels, steps: [.steps[] \| select(.name \| test("Kiểm runner\|Cổng chất lượng")) \| {name, conclusion}]}'`: `build-and-test` có `labels` `["ubuntu-latest"]`, bước `Kiểm runner tự host (scripts/runner-check.sh)` `skipped`, `Cổng chất lượng (scripts/check.sh)` `success`. Sau merge: `main-guard` của commit merge xanh (`full`; `ui-test` chạy vì đổi `main-guard.yml`); lệnh `jobs` tương tự cho `guard`, `ui-test`: nhãn `["ubuntu-latest"]`, bước kiểm runner `skipped`, `Giải phóng đĩa cho emulator` và `Bật KVM cho emulator` `success`. Ghi vào nhật ký |
| Tài liệu đúng và đủ (DoD 3) | Kiểm tra thủ công (người: đọc tài liệu khi dựng máy) | Người dựng đúng theo `docs/ops/self-hosted-runner.md` ở H3; chỗ thiếu ghi vào nhật ký, sửa tài liệu bằng PR nhỏ. Ghi kết quả kiểm nhóm runner trước H4 |
| H4: runner nhận job | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | Mục 6, bước 1 của tài liệu mới, trên một repo app private |
| H5: `ui-test` và cách ly giữa job (DoD 1) | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | Mục 6, bước 2, 3 và 6 của tài liệu mới |
| H6: xóa biến thì quay về runner của GitHub (DoD 2), máy tắt | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Mục 6, bước 4, 5 của tài liệu mới; ghi lần chạy lại sau khi xóa biến nhận nhãn mới hay giữ nhãn cũ, và câu log thật của PB-18 |

**Test 3, `runners: every heavy job checks the runner right after checkout`.** Hàm `runner_order <workflow> <job>` in `yes` hay `no <lý do>`. Trong mỗi job nặng:

- Bước đứng ngay sau `uses: actions/checkout@v5` có `name: Kiểm runner tự host (scripts/runner-check.sh)`, `if: runner.environment == 'self-hosted'`, `REPO_VISIBILITY: ${{ github.event.repository.visibility }}`, và `run: scripts/runner-check.sh` có `--emulator` khi job có `android-emulator-runner`, không có khi không.
- Trong các bước sau checkout, bước kiểm đứng trước `setup-java`, `setup-gradle`, bước `id: mode`, và mọi bước mà nội dung `run:` (đã bỏ dòng `^[[:space:]]*#`) có `gradlew`, `check.sh` (khác `runner-check.sh`) hay `tests/run.sh`.
- Các bước đứng trước bước kiểm, kể cả trước checkout:
  - chỉ được là `actions/checkout`, bước `uses: dorny/paths-filter@...` (nguyên khối, không xét nội dung), hay bước `run:`; bước `uses:` khác (ví dụ `setup-gradle`) là `no`;
  - bước `run:` chỉ có các khóa `name`, `id`, `if`, `env`, `run` (không `working-directory`, `shell`);
  - nội dung `run:` (bỏ dòng chú thích) không chứa `gradle`, `$HOME`, `${HOME}`, `~/`, `/home/`, `/root/`; không có lệnh ghi tệp `cp`, `mv`, `mkdir`, `touch`, `ln`, `install`, `curl -o`, `curl -O`, `wget`, `sed -i`, `dd`; và mọi chuyển hướng `>`, `>>`, `tee` chỉ tới `"$GITHUB_OUTPUT"`, `"$GITHUB_STEP_SUMMARY"`, `build.log` (đường tương đối) hay `/dev/null`.
- Bản sao hỏng, mong `no` mỗi bản: bước kiểm đứng sau `setup-gradle` (`guard`); thiếu `--emulator` ở `ui-test`; thiếu `if:`; một bước trước checkout có `run: echo x > ~/.gradle/init.d/a.gradle`; một bước trước checkout có `run: echo x > "$HOME/build.log"`; có `run: echo x > "${HOME}/a"`; có `run: curl -o a.gradle https://example.com/a`; có `working-directory: /home/runner/.gradle/init.d`; `uses: gradle/actions/setup-gradle@v4` trước checkout.
- Đối chứng, mong `yes`: `pr-check.yml` trên nhánh chính sau khi code của task vào (hai bước lọc trước checkout, chú thích có `scripts/check.sh` và `gradlew`, `tee -a build.log` trên đường lỗi); `guard`, `ui-test` của `main-guard.yml`.

**Test 6, `runner-check: ...`.**

- Dựng: repo git tạm `"$work/rc"` (một commit, `.gitignore` có `build/`); `HOME` tạm; `GRADLE_USER_HOME` không đặt; `VGL_RUNNER_EPHEMERAL=1`; `REPO_VISIBILITY=private`; `RUNNER_CHECK_MIN_FREE_GB=0`. Hàm `rc [args]` chạy script trong `"$work/rc"`, in `<mã thoát> <số dòng RUNNER KHÔNG DÙNG ĐƯỢC>`.
- Ca: (a) đủ điều kiện → `0 0`, có dòng `RUNNER DÙNG ĐƯỢC`; (b) `REPO_VISIBILITY` `public`, rồi rỗng → `1 1` mỗi lần; `internal` → `0 0`; (c) `VGL_RUNNER_EPHEMERAL` không đặt → `1 1`; (d) lần lượt `$HOME/.gradle/init.d/x.gradle`, `init.gradle`, `init.gradle.kts`, `gradle.properties` → `1 1` mỗi lần; `GRADLE_USER_HOME="$work/gh"` có `init.d/` → `1 1`; `$HOME/.gradle/init.d` có mà `GRADLE_USER_HOME` trỏ thư mục sạch → `0 0`; `$HOME/.gradle/caches/` có → `0 0`; (e) `build/x` (bị bỏ qua), rồi `core/x/src/test/screenshots/a.png` chưa track → `1 1` mỗi lần; chạy trong thư mục không phải repo git → `1 1`; (f) `RUNNER_CHECK_MIN_FREE_GB=999999` → `1 1`; (g) `--emulator`: `RUNNER_CHECK_KVM="$work/nokvm"` (không có) → `1 1`; tệp tạm đọc ghi được và `ANDROID_HOME` thư mục tạm ghi được → `0 0`; không `--emulator` mà `RUNNER_CHECK_KVM` không có → `0 0`; KVM được mà `ANDROID_HOME` rỗng → `1 1`; KVM được mà `ANDROID_HOME` chỉ đọc (`chmod 555`) → `1 1`, bỏ qua ca này khi `id -u` là 0; (h) ba lỗi cùng lúc (public, không ephemeral, `init.d`) → `1 3`; (i) tham số lạ (`--emu`, `--emulator x`) → mã 2; (j) `git ls-files -s scripts/runner-check.sh` có `100755`.

**Test 7, `wait-run: a job waiting for a self-hosted runner with nothing running is CHỜ RUNNER`.** Đặt cuối khối `wait-run` (sau test của T-077).

- Dựng: `WAIT_RUN_NOW` cố định (`T`, giây của 2026-11-08T12:00:00Z), giờ trong dữ liệu tính từ `T`; dữ liệu `pr_routes "$pending" <jobs>`; gọi qua `wait_code` hay `wait_line` (thời gian chờ 2 giây).
- Ca chính: jobs `[{"id": 120, "name": "build-and-test", "status": "queued", "runner_name": null, "labels": ["vgl-android"], "created_at": <T - 90 phút>}]` → `7 CHỜ`; dòng có `chờ runner tự host`, `vgl-android` và `lần chạy 91`.
- Ca `needs` (theo `ui-test`): `guard` `completed`, `completed_at` <T - 10 phút>; `ui-test` `queued`, không runner, nhãn `vgl-android`, `created_at` <T - 90 phút> → `2 CHƯA`; cùng dữ liệu, `guard` xong <T - 80 phút> → `7 CHỜ`.
- Đối chứng, mỗi ca `2 CHƯA`: `created_at` <T - 30 phút>; nhãn `["ubuntu-latest"]` <T - 90 phút>; một job khác `in_progress`; `runner_name` khác rỗng; `WAIT_RUN_RUNNER_QUEUE_MINUTES=120`; `errors` cho đường `jobs`; câu trả lời `{}`.
- `WAIT_RUN_RUNNER_QUEUE_MINUTES=abc` → `3 LỖI`.
- Ca `--workflow main-guard.yml --since` với lần chạy `in_progress` và job như ca chính → `7 CHỜ`.
- Thứ tự (T-077): PR nháp (`"draft": true`) với cùng job → `6 NHÁP`; PR xung đột không có lần chạy → `5 XUNG` như câu có sẵn.

**Test 10, `resume-check: no resume while a job waits for a self-hosted runner`.**

- Dựng: `resume_check running 120` (dữ liệu của câu "a running session silent for 2 hours is continued") với `RESUME_QUEUED='{"total_count": 1, "workflow_runs": [{"id": 91}]}'` và `RESUME_ROUTES=', "actions/runs/91/jobs?per_page=50": {"jobs": [<job như ca chính test 7>]}'`; stderr ghi vào `$work/resume.err` (hàm thêm biến cho đường stderr, mặc định `/dev/null`).
- Ca chính: đầu ra rỗng, stderr có `đang chờ runner tự host`, mã 0. Job cùng nhãn tạo <T - 1 phút> → cũng rỗng (không có ngưỡng). Lần chạy 91 trong `RESUME_IN_PROGRESS` thay vì `RESUME_QUEUED` → rỗng.
- Đối chứng, mỗi ca in `docs/runs/RUN-20261018-01.md`: job nhãn `["ubuntu-latest"]`; job `"labels": []`; job nhãn `vgl-android` mà `in_progress`; không có lần chạy chưa xong. Đọc lỗi: `RESUME_ERRORS='{"actions/runs?status=queued&per_page=20": "HTTP 500"}'` → đầu ra rỗng, mã 1; câu trả lời `null` cho đường đó → mã 1.

**Test 12, `pr-check: the sample PR runs the runner check only on a self-hosted runner`** (đặt ngay sau G1). Phần mở rộng của `pr_sim` nằm trong commit test, nên trước code test này đỏ vì workflow chưa có bước (số đếm 0), P6 vẫn xanh.

- `pr_sim_has "$pr_wf" 'Kiểm runner tự host (scripts/runner-check.sh)' app/src/main/java/A.kt` mong `0`; cùng lệnh với `PR_SIM_RUNNER=self-hosted` mong `1`; với `PR_SIM_RUNNER=self-hosted` và `docs/a.md` mong `1`.
- Thứ tự: với `PR_SIM_RUNNER=self-hosted`, dòng ngay sau `actions/checkout@v5` trong đầu ra `pr_sim` là tên bước kiểm runner.
- `PR_SIM_RUNNER=self-hosted pr_sim_summary "$pr_wf" app/src/main/java/A.kt` bằng tóm tắt mặc định (`code=true release=false gate=yes gradle=yes docs=no r8=no`).
- Đối chứng của phần mở rộng: bản sao `$work/prwf/runner-typo.yml` (`runner.enviroment`) → `ERROR điều kiện lạ: runner.enviroment == 'self-hosted'`; bản sao `runner-os.yml` (điều kiện thành `runner.os == 'Linux'`) → `ERROR điều kiện lạ: runner.os == 'Linux'`; `PR_SIM_RUNNER=mac` → `ERROR PR_SIM_RUNNER lạ: mac`; bản sao `runner-github.yml` (điều kiện thành `runner.environment == 'github-hosted'`) → `pr_sim_has` mặc định ra `1` (khác `0`: test bắt bước kiểm chạy nhầm trên máy GitHub).

**Test viết trước thất bại đúng lý do (bước 3).** Đỏ trên code hiện tại: 1 (hai workflow chưa dùng biến), 3, 4, 6 (script chưa có), 7, 8 (PB-17 tới PB-19 chưa có dòng), 9, 10, 11, 12 (số đếm `1` của ca `self-hosted`), 13, 14, câu `every row has a log file`. Qua cả trước code, đúng dự kiến vì là đối chứng hay kiểm hành vi cũ: 2, 5, các ca đối chứng của 1, 3, 7, 8, 10, 12, mọi câu P6, `mg_wiring`, `ui_wiring`. Ca nào khác danh sách này thì ghi vào nhật ký (ADR-0032/L4).

**Ngoại lệ viết test trước:** tài liệu (`docs/ops/self-hosted-runner.md`, `docs/ops/staging-and-runs.md`, `docs/auto-run.md`, `.claude/skills/merge-pr/SKILL.md`, `CLAUDE.md`), dòng chú thích của ba workflow và hai script cổng: không có hành vi để kiểm; câu tài liệu được `scripts/history-refs.py` (trong `scripts/tests/run.sh`) kiểm sau khi viết. Các bước H3 tới H6 cần máy thật.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-11-08, trong thư mục tạm, không commit, trên nhánh chính 2fd2701:
  - bản sao `pr-check.yml` có thay đổi của T-077 bản 2 (`types`, `if:` cấp job) và của T-078 (mục 3.1 B); `pr_sim` hiện có in `ERROR điều kiện lạ: runner.environment == 'self-hosted'` (tái hiện điểm chặn); `pr_sim` mở rộng như mục 3.1 H cho tóm tắt không đổi với `github-hosted`, có thêm bước kiểm runner ngay sau `actions/checkout@v5` với `self-hosted`, và `ERROR` với `runner.enviroment`, `PR_SIM_RUNNER=mac`; `pr_job_if_draft` (T-077) `1 yes`; `pr_docs_steps` 1, `pr_unconditional` `yes yes`, `pr_gradle_gated` `yes`, `pr_docs_step_tools` 5, `pr_jobs` `build-and-test`, `pr_filter_order` `yes yes yes yes 6`, `pr_filter_outputs` như nhánh chính;
  - bản sao `main-guard.yml` sửa đúng như diff mục 3.1 B: `mg_wiring` mười `yes`, `ui_wiring` chín `yes`, `mg_own_issues` `yes yes`;
  - biểu thức `jq` của mục 3.1 D trên mười câu trả lời mẫu (ca chính, mới tạo, `needs` hai mốc, nhãn GitHub, job khác `in_progress`, có runner, nhãn rỗng, `jobs` rỗng, `{}`): ra đúng ca chính và ca `needs` xong lâu, rỗng ở các ca còn lại, `{}` lỗi `jq` khi chưa có `(.jobs // [])` nên bản cuối dùng `(.jobs // [])`;
  - bảng tra lỗi có ba dòng mới qua `scripts/failure-kind.sh --playbook` (mục 3.1 F);
  - các câu tài liệu của mục 3.1 G qua `scripts/history-refs.py` (mục 3.1 G).
- **Bản 1, 2026-10-30:** `actionlint -shellcheck=` (1.7.12) trên bản sao có `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}` và `if: runner.environment == ...`: mã 0; đối chứng `secrets.` trong `runs-on` và `runner.enviroment`: mã 1 với `context "secrets" is not allowed here` và `property "enviroment" is not defined`. Máy soạn bản 2 không có `actionlint`, nên không chạy lại.
- **Lệnh:** chưa chạy `scripts/check.sh` trên runner tự host vì chưa có máy (H4, H5 sau merge). Trong phiên, `scripts/check.sh` chạy như mọi task (bước 5) trên máy của phiên; thay đổi không đổi hành vi trên máy dev hay runner của GitHub.
- **Kết quả:** phần chạy được đều như mong đợi; phần chưa thử (action trong container không `sudo`, KVM) có phương án ở mục 3.4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và quy trình phiên: `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `scripts/main-guard-verified.sh`, `scripts/ui-test-needed.sh`, `scripts/wait-run.sh`, `scripts/resume-check.py`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Ba workflow, một script mới, bốn script sửa (cổng `main-guard`, emulator, chờ CI, tiếp tục phiên), test, bảng tra lỗi, sáu tài liệu; nhiều phần độc lập của CI và quy trình (bản 1 chấm 2 khi chưa có hai script cổng) |
| B. Độ rõ của yêu cầu | 2 | Câu hỏi của bản 1 đã được trả lời (mục 9, bản 1), T-075, T-076 đã merge (bản 1 chấm 3 vì repo công khai và ba plan chưa merge). Còn mở và ảnh hưởng cách làm: T-077 bản 2 chưa duyệt; nhóm runner trên gói Free và Re-run sau khi xóa biến chưa xác minh |
| C. Khả năng kiểm chứng tự động | 2 | Cấu trúc workflow và các script có test; chạy thật trên runner tự host chỉ kiểm được bằng tay sau khi có máy |
| D. Ảnh hưởng chéo | 3 | Mọi PR và mọi commit của main, mọi phiên chờ CI và tiếp tục phiên, ở mọi app sau đồng bộ; máy hỏng mà biến còn thì mọi job nặng chờ; cùng file với T-077 |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng runner tự host, `runner.environment`, `vars` trong `runs-on`, runner just-in-time; đã chạy thử phần cú pháp và phần test (mục 4.1) |
| **Tổng** | 12 | **Mức: Cao (điểm chặn 7, tổng từ 9)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `pr-check.yml`, `main-guard.yml` (mọi PR, mọi commit của main); `main-guard-verified.sh`, `ui-test-needed.sh` (cách chọn của `main-guard`); `wait-run.sh` (mọi phiên chờ CI); `resume-check.py` (mọi lần tiếp tục phiên); `failure-playbook.md`, `docs/auto-run.md`, skill `merge-pr`.
- **T-077** (PR #411, bản 2 `draft`): sửa cùng `pr-check.yml` (`types`, `if:` cấp job), `wait-run.sh` (mã 6), `scripts/tests/run.sh` (`pr_job_if_draft`, `pr_sim` cấp job, `mig_run`, khối `nu_*`, test `wait-run`), `docs/auto-run.md` bước 8, skill `merge-pr` (cùng dòng mã thoát). T-078 làm sau (`depends_on`), sửa trên bản đã có T-077. Một danh sách chạy có thể có cả hai, T-078 sau cùng.
- **T-076** (đã merge): P6, G1 giữ nguyên nhờ phần mở rộng `pr_sim` (mục 3.5).
- Các app nhận workflow và script qua đợt đồng bộ (`scripts/template-sync.sh`); biến `HEAVY_RUNNER` đặt riêng ở từng repo, không đồng bộ.
- `scripts/run-watchdog.py` mở issue `session-stalled` sau 60 tới 90 phút không commit, kể cả khi phiên chờ runner tự host chính đáng. Chấp nhận (như bản 1): issue chỉ là thông báo; tài liệu mới, mục 7, dặn xem hàng chờ trước khi dán lệnh tiếp tục.
- Luật "còn job `queued` nhãn tự host thì không tiếp tục" có thể giữ phiên tới hết 24 giờ khi máy bận liên tục. Chấp nhận: phiên không nên chồng lên hàng chờ của máy.
- Một PR làm hỏng `runner-check.sh` chỉ lộ ra trên runner tự host: `pr-check` của chính PR đỏ ở bước kiểm (đã ở trên máy), không merge được. Lọt vào main (ví dụ PR chạy trên runner của GitHub) thì `guard` và `ui-test` đỏ không mở `main-broken` (mục 3.1 B); `main-guard-status.sh` trả mã 3, phiên chạy lại một lần rồi kết thúc, log khớp PB-19 mà dòng đó dặn task sửa chính script thì là lỗi thật.
- **Chuyển tiếp.** PR của task chạy `pr-check` theo workflow mới với biến chưa đặt: nhãn `ubuntu-latest`, bước kiểm runner bị bỏ qua (phiên kiểm, mục 4). `main-guard` của commit merge chạy `full` (đổi `pr-check.yml`, `main-guard.yml`, `scripts/tests/run.sh`, `main-guard-verified.sh`, đều trong `GATE_FILES`) và `ui-test` (đổi `main-guard.yml`, trong `UI_PATHS`), khoảng 60 phút. Bản `wait-run.sh` cũ trong worktree tạo trước khi merge không biết mã 7; vô hại vì chưa có máy.
- **Cách chạy.** Phiên tự chạy (quyết định ngày 2026-11-01). Plan sửa `.github/workflows/`: phiên cần khả năng `workflow-push` (`docs/ops/session-capabilities.md`, ô `thử`), danh sách chạy ghi cách mở và phiên chạy `scripts/push-probe.sh` ở đầu phiên. Không cần `repo-secrets`, emulator, quyền quản trị repo.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu nhánh chính với mục 2. Dừng (cần plan phiên bản mới) khi: T-077 chưa merge; `build-and-test` không có đúng dòng `if:` cấp job của T-077, hay `on.pull_request` không có `types`; `pr_sim` không có phần cấp job của T-077 hay không còn tên đó; `pr_job_if_draft` không còn; `wait-run.sh` đã dùng mã 7 hay kiểm nháp nằm sau phần đọc job; tên job `build-and-test`, `guard`, `ui-test`, tên bước `Cổng chất lượng (scripts/check.sh)` hay `id: filter` khác. Chỉ lệch số dòng thì theo ADR-0032/L3.
- Cần thêm bí mật, quyền ghi mới, hay đưa một job có `secrets.` lên `HEAVY_RUNNER`: dừng.
- Cần sửa `scripts/merge-if-green.sh`, `scripts/check.sh` hay `scripts/release.sh`: dừng.
- Cần thêm hay đổi `if:` cấp job của `build-and-test` (khi đó phải sửa `pr_job_if_draft` và phần cấp job của `pr_sim`): dừng.
- Trên GitHub thật (mục 4, dòng DoD 4), job của PR task có nhãn khác `ubuntu-latest` (biến đã được đặt), hay bước kiểm runner không `skipped`: dừng trước khi merge, để PR mở, ghi vào Cần người kiểm tra. Sau merge, bước `Bật KVM cho emulator` của `ui-test` không `success` trên runner của GitHub: ghi vào Cần người kiểm tra; `ui-test` đỏ thật thì làm theo `docs/auto-run.md`, Khi nhánh chính đỏ vì task của phiên.

## 8. Câu hỏi cho người duyệt

Câu 1 tới 8 của bản 1 đã được trả lời (mục 9, bản 1) và giữ nguyên. Câu mới của bản 2:

1. **Giả định về T-077 bản 2 (PR #411, commit 9f7d8de).** Plan này dựa trên T-077 bản 2 như mô tả ở mục 2. Nếu T-077 bản 2 đổi sau rà soát hay lúc duyệt (nhất là phần cấp job của `pr_sim`, `pr_job_if_draft`, chỗ kiểm nháp và mã 6 của `wait-run.sh`, câu mã 6 của `docs/auto-run.md` và skill `merge-pr`), AI đối chiếu lại plan này trước khi duyệt. Đề xuất: duyệt T-078 bản 2 sau T-077 bản 2.
2. **Phần mở rộng `pr_sim` (mục 3.1 H, test 12).** Chỉ hiểu `runner.environment` với hai giá trị, mặc định `github-hosted`; `runs-on` theo biểu thức không phải điều kiện cấp job nên `pr_sim` và `pr_job_if_draft` không đọc nó, không sửa phần cấp job của T-077. Đề xuất đồng ý.
3. **Ai làm gì, khả năng của phiên.** Phiên tự chạy không có quyền quản trị repo (không đặt biến, không đăng ký runner; `docs/ops/session-capabilities.md` không có khả năng nào như vậy, ADR-0042 cấm phiên làm), chỉ cần `workflow-push` (`thử`, `push-probe.sh` đầu phiên). Đề xuất: task chạy trong phiên tự chạy theo quyết định ngày 2026-11-01; phiên kiểm DoD 4 trên GitHub thật bằng `gh api` (mục 4); H1 tới H6 (thuê máy, dựng, cài đặt GitHub, đặt biến, thử) là việc của người sau merge. Đồng ý?
4. **Repo để thử.** Ngày 2026-11-01 repo dự án mẫu vẫn công khai (RUN-20261101-02 ghi). Bản 1 quyết coi là private; nhưng ADR-0042 mục 6 và `runner-check.sh` không cho runner tự host ở repo công khai, nên H4 tới H6 chỉ làm được trên một repo app private. Có repo nào như vậy chưa? Chưa có thì task vẫn merge được, H để sau và DoD 1, 2 của ticket mở tới lúc đó. Đề xuất: ghi điều này vào nhật ký khi merge, ticket `done` sau H6.
5. **PB-18 bỏ mẫu "shutdown signal"** (đổi so với bản 1): câu "The runner has received a shutdown signal" cũng gặp trên runner của GitHub; giữ thì một lỗi nên chạy lại sẽ thành việc của người. Mẫu chỉ còn câu riêng của runner tự host. Đề xuất đồng ý.
6. **`GATE_FILES` và `UI_PATHS` thêm `runner-check.sh`; không thêm vào `own_job_changed` của `ui-test-verdict.sh`** (mục 3.1 B, C). Đề xuất đồng ý.
7. **Điểm rủi ro:** A từ 2 lên 3, B từ 3 xuống 2, tổng giữ 12 (mục 5). Xác nhận.
8. **Kiểm thêm trong `runner-check.sh`** (người dùng có `sudo`, thiếu `gh`, `jq`) không làm ở bản 2, vì ADR-0042 mục 3 liệt kê đủ các điều kiểm; tài liệu mới dặn image gỡ `sudo` và cài `gh`. Muốn kiểm thì ticket sau, kèm ghi chú ADR. Đồng ý?
9. **`token_estimate` 30.0M** theo cỡ L (`scripts/token-usage.py estimate`: một task cỡ L đã đo, 49.0M; số tạm 30.0M). Task có khoảng 14 nhóm test mới, một script mới và bốn script sửa; theo `docs/CLAUDE.md` không nâng tay.

## 9. Quyết định duyệt

Phiên bản 2:

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

Phiên bản 1:

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** Duyệt trong phiên có người ngày 2026-10-30, sau khi T-075 merge (PR #324) và plan T-076, T-077 được duyệt (PR #315, #316). Câu 1: làm tiếp; repo công khai chỉ là tạm thời, coi như private (anh Nhật, 2026-10-30), không cần xem lại T-075 tới T-077. Câu 2: thuê máy chủ riêng Linux x86_64 có KVM (khoảng 8 nhân, 64 GB RAM, 500 GB SSD). Câu 3: một máy cho cả tổ chức, 2 container chạy cùng lúc. Câu 4 tới 8 theo đề xuất: biến `HEAVY_RUNNER` cấp repo (kiểm cài đặt nhóm runner trước H4); chỉ job không đọc bí mật lên runner tự host, `staging` và `device-session` giữ runner của GitHub; Re-run sau khi xóa biến thử ở H6; thêm `actions: read` cho `resume-run.yml`; ADR-0042 ghi `accepted`. Đối chiếu lúc duyệt: job `ui-test` của `main-guard.yml` trên nhánh chính khớp mục 2 (`needs: guard`, `if: needs.guard.outputs.ui != 'no'`, `timeout-minutes: 60`, hai bước `sudo`). Rà soát lại phần sửa test 3 (commit b4c54f1): không còn điểm chặn; đối chứng theo hình dạng sau T-076 ra qua, năm bản sao hỏng đều thất bại. Task sửa trong file của mục 3.2, như điểm nên sửa (ADR-0029), bốn điều sau: (a) bước `run:` đứng trước bước kiểm runner chỉ được có các khóa `name`, `id`, `if`, `env`, `run` (không `working-directory`, `shell`), thêm bản sao hỏng có `working-directory: /home/runner/.gradle/init.d`; (b) định nghĩa rõ "lệnh ghi tệp" của test 3: ngoài `>`, `>>`, `tee`, dò cả `cp`, `mv`, `mkdir`, `touch`, `ln`, `install`, `curl -o`/`-O`, `wget`, `sed -i`, `dd`, và cấm cả `${HOME}`, `/home/`, `/root/`; thêm bản sao hỏng dùng `${HOME}` và `curl -o`; (c) thêm bản sao hỏng đặt `uses: gradle/actions/setup-gradle@v4` trước checkout, mong thất bại; (d) nguồn của đối chứng "pr-check.yml theo T-076 qua" là `pr-check.yml` trên nhánh chính sau khi T-076 merge (không phải commit d8f714e của nhánh plan), và mục 7 thêm điều kiện dừng: T-076 merge theo phương án dự phòng (bước lọc sau checkout) thì test P3(c) của T-076 và test 3 của plan này không cùng thỏa được: dừng, soạn plan phiên bản 2. Dựng máy, cài runner, đặt biến và thử quay về (H1 tới H6) là việc của người, sau khi merge. Task làm có người (`auto_run: false`), sau khi T-076, T-077 merge.

## 10. Nhật ký thực hiện
