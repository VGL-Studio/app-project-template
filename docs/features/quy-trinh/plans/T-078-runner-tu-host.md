---
ticket: T-078
title: Runner tự host cho các job nặng, có đường quay về runner của GitHub
status: draft
version: 1
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

`scripts/wait-run.sh` có mã thoát mới 7 (`CHỜ RUNNER`) cho job đứng chờ runner tự host khi không còn gì của lần chạy đang chạy. `scripts/resume-check.py` không mở lại phiên khi có job như vậy. Kiểm bằng test tĩnh trên mọi workflow và test của ba script trong `scripts/tests/run.sh`. Dựng máy, cài runner, đặt biến và lần thử quay về là việc của người, ngoài phiên. Theo ADR-0042, đề xuất cùng PR plan này.

**Cần người đọc trước (mục 8, câu 1):** ngày 2026-10-30, `app-project-template` và `camera-pose` đang là repo công khai. Runner của GitHub miễn phí cho repo công khai, và GitHub khuyên không dùng runner tự host cho repo công khai. Với hai repo này, T-078 không bớt được đồng nào. Plan vẫn viết đủ để dùng cho các app private sau này. Người quyết làm tiếp, hoãn task (vẫn có thể duyệt ADR), hay dừng (mục 9).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 1. Ticket `auto_run: false`: làm có người.

| Yêu cầu (phạm vi và định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 2 | Job nặng đọc nhãn runner từ biến; xóa biến thì về runner của GitHub; job nhẹ giữ runner của GitHub | Biến cấp repo, không cấp tổ chức: gói Free không cho repo private đọc biến cấp tổ chức (mục 8, câu 4). "Job nặng" xác định theo nội dung job, không theo tên: chạy `scripts/check.sh`, `./gradlew` hay `android-emulator-runner`, và không có `secrets.` |
| Phạm vi 3 | Bước giả định máy sạch chạy được trên cả hai loại máy, hoặc bỏ qua trên máy tự host | Bước có `sudo` (giải phóng đĩa, bật KVM) thêm `if: runner.environment == 'github-hosted'`. Trên máy tự host, quyền KVM do máy chủ cấp cho container (luật udev, `--group-add`). `setup-java`, `setup-gradle` chạy được trên cả hai loại máy, giữ nguyên |
| Phạm vi 4 | `check.sh` không để lại trạng thái giữa hai lần chạy; mỗi job có thư mục làm việc sạch | Container mới cho mỗi job (ADR-0042 mục 2). `actions/checkout` giữ `clean` mặc định (`git clean -ffdx`). `runner-check.sh` từ chối thư mục làm việc có file ngoài git. `check.sh` trong CI (`GITHUB_ACTIONS` có giá trị) không tự ghi ảnh chuẩn, nên không đổi |
| Phạm vi 5 (an toàn) | Chọn: chạy một lần, dọn `~/.gradle`, hay tách runner cho job có bí mật | Chọn chạy một lần trong container và chỉ cho job không đọc bí mật; `staging`, `device-session` giữ runner của GitHub (mục 3.1, lý do) |
| Phạm vi 6 | Dòng bảng tra lỗi: máy tắt, hết đĩa, job chờ runner quá lâu | PB-16 (chờ runner tự host, `wait-run.sh` mã 7), PB-17 (runner mất kết nối hay bị tắt giữa job), PB-18 (runner không dùng được: cấu hình, đĩa, KVM) |
| DoD 1 | Job nặng chạy trên runner tự host; lịch sử tính tiền không còn phút của chúng | Việc của người sau khi đặt biến (mục 4, bước H5) |
| DoD 2 | Xóa biến thì job chạy lại trên runner của GitHub, thử một lần, ghi vào plan | Mục 4, bước H6 |
| DoD 3 | Tài liệu cài và vận hành máy, dòng bảng tra lỗi | `docs/ops/self-hosted-runner.md` mới; PB-16 tới PB-18 |
| DoD 4 | CI qua, đã merge | Như mọi task. Merge được trước khi có máy: biến chưa đặt thì mọi job vẫn chạy trên runner của GitHub |

**Giả định về T-075, T-076, T-077.** Ngày 2026-10-30 cả ba đang ở dạng plan draft: PR #318, #315, #316. Plan này lấy chúng làm gốc:

- T-075 (PR #318): `ui-test` rời `pr-check.yml`. Job mới `ui-test` trong `main-guard.yml` có `needs: guard`, `if: needs.guard.outputs.ui == 'yes'` và `timeout-minutes: 60`. Hai bước "Giải phóng đĩa cho emulator" và "Bật KVM cho emulator" chép nguyên từ `pr-check.yml`, không có `if:`. Job dùng quyền cấp workflow của `main-guard.yml` (`contents: write`, `issues: write`, `checks: read`, `actions: read`, `pull-requests: read`).
- T-076 (PR #315): `pr-check.yml` chỉ còn job `build-and-test`. Job mở đầu bằng bước `Lọc đường dẫn (dorny/paths-filter)` và bước `Kiểm kết quả lọc đường dẫn`, đứng trước `actions/checkout`. Phương án dự phòng của T-076 có thể dời bước lọc ra sau checkout.
- T-077 (PR #316):
  - `pr-check.yml`: khai báo `types`, và job `build-and-test` có `if:` cấp job `github.event.pull_request.draft == false`;
  - `wait-run.sh`: mã 6 cho PR nháp, và lần chạy `skipped` (lúc PR còn nháp) không tính là kết quả;
  - sửa `merge-if-green.sh`, `docs/auto-run.md` bước 8 và skill `merge-pr`.

  T-078 lấy mã thoát và thứ tự kiểm của `wait-run.sh` sau T-077 làm gốc. Mã mới là 7, không dùng lại 6.

Phiên đối chiếu lại với code trên main sau khi ba plan đó merge, trước bước 3 (mục 7). Test của mục 4 tìm job nặng theo nội dung, không theo tên.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0042 (mới, `proposed`), ADR-0009 và ADR-0017 (phát hành production ngoài GitHub, ngoài phạm vi), ADR-0010, ADR-0025, ADR-0039 (`main-guard`, giữ nguyên cách chọn `skip`, `tests`, `full`), ADR-0040 (T-075) và ADR-0041 (T-077), đều đang đề xuất, ADR-0029 (phạm vi file).

## 3. Giải pháp

### 3.1. Cách làm

**Phân loại job** (đọc ngày 2026-10-30; cột cuối tính cả thay đổi của T-075 tới T-077):

| Workflow / job | Nặng hay nhẹ | Bí mật | Quyền của `GITHUB_TOKEN` | Bước giả định máy sạch | Sau T-078 |
| --- | --- | --- | --- | --- | --- |
| `pr-check` / `changes` | nhẹ (bỏ ở T-076) | không | `contents: read`, `pull-requests: write` | không | không còn |
| `pr-check` / `build-and-test` | nặng (`check.sh`, R8) | không | `contents: read`, `pull-requests: write` (bình luận) | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `pr-check` / `ui-test` | nặng; T-075 chuyển sang `main-guard` | không | như trên | `sudo rm -rf` giải phóng đĩa, `sudo tee` udev bật KVM, `setup-java`, `setup-gradle` | không còn (nếu còn thì `HEAVY_RUNNER`) |
| `main-guard` / `guard` | nặng (`check.sh`, trừ cách `skip`) | không | `contents: write`, `issues: write`, `checks: read`, `actions: read`, `pull-requests: read` | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `main-guard` / `ui-test` (T-075) | nặng (emulator, tới 60 phút) | không | như `guard` (quyền cấp workflow) | hai bước `sudo` như `pr-check` / `ui-test`, `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `staging` / `distribute` | nặng | 5 bí mật `STAGING_*` | `contents: read` | `setup-java`, `setup-gradle` (`cache-read-only`) | giữ `ubuntu-latest` |
| `device-session` / `emulator` | nặng | 4 bí mật `STAGING_*` | `contents: read` | như `ui-test` | giữ `ubuntu-latest` |
| `env-canary` / `setup-check`, `fire` | nhẹ | `STAGING_*`, `ENV_CANARY_FIRE_*` | `contents: read`, `issues: write` | không | giữ |
| `start-run` / `check`, `close-setup-broken`, `fire` | nhẹ | `STAGING_*`, `ROUTINE_FIRE_*` | đọc; `issues: write`, `actions: write` ở job riêng | không | giữ |
| `resume-run`, `run-watchdog`, `status-page` | nhẹ (lịch, API) | `ROUTINE_FIRE_*` ở `resume-run` | `contents: write` ở `status-page`, `issues: write` ở `run-watchdog`, `actions: write` ở job `idle`/`schedules` | không | giữ |

**Chọn phương án an toàn: runner chạy một lần trong container, chỉ cho job không đọc bí mật.**

- Ba cách ticket nêu, và vì sao chọn cách này:
  - Dọn `~/.gradle` và thư mục làm việc sau mỗi job: không đủ. GitHub nói không bảo đảm được runner chạy lâu chỉ chạy một job, và tiến trình chạy nền hay file ngoài các thư mục đã dọn vẫn còn (mục 8, nguồn 6). Một init script trong `~/.gradle/init.d` chạy trong mọi lần build sau; `scripts/cloud-setup.sh` đã dùng đúng cơ chế này.
  - Tách runner cho job có bí mật và quyền ghi: đưa khóa ký staging lên máy của nhóm, thêm một dịch vụ phải giữ. Lợi: khoảng 13% số phút (`device-session` 9%, `staging` 4%). Không đáng ở bước đầu.
  - Chạy một lần (just-in-time) trong container mới: mỗi job một container, xóa sau job. PR không để lại gì cho `main-guard`. Đã chọn.
- **Hàng rào nằm ở đâu.** Với sự kiện `pull_request`, GitHub đọc workflow và script từ code của PR. Một PR có thể sửa `runner-check.sh`, ghi thẳng nhãn vào `runs-on`, hay bỏ bước kiểm. Vì vậy mọi kiểm tra trong repo (`runner-check.sh`, test tĩnh) chỉ bắt cấu hình nhầm, không chặn được người cố ý. Hàng rào thật nằm ở GitHub và ở máy:
  - nhóm runner không cho repo công khai dùng runner (người kiểm trước bước H4, mục 8 câu 4);
  - tắt chạy workflow từ fork của repo private;
  - container chạy một lần, không `sudo`, không socket Docker; token xin cấu hình runner không vào container.

  Người đẩy được nhánh lên repo private là thành viên tổ chức hay phiên AI của tổ chức. Với nhóm người này, rủi ro còn lại là nhầm lẫn, và hai lớp trên đủ.
- `main-guard` / `guard`, `ui-test` có `contents: write`, `issues: write` mà vẫn lên runner tự host. Code của chúng là code đã merge vào main, token chỉ sống trong job, và container mới không nhận gì từ job PR trước. Rủi ro còn lại giống trên runner của GitHub.
- **Đánh đổi:** ở cách `skip` (ADR-0039), `guard` chỉ đọc API khoảng 1 phút mà vẫn giữ một chỗ trên máy. Plan chấp nhận, vì hai lý do. Thứ nhất, `ui-test` có `needs: guard`; để `guard` trên runner của GitHub thì vẫn tính phút cho phần `full`. Thứ hai, một chỗ bị giữ 1 phút không đáng kể so với `ui-test` 60 phút.
- Job có `secrets.` giữ `ubuntu-latest`. Test tĩnh (mục 4, test 1) chặn mọi job có `secrets.` dùng `HEAVY_RUNNER`.

**Script mới `scripts/runner-check.sh [--emulator]`.**

- Mục đích: bắt cấu hình nhầm của máy hay của repo trước khi build. Không phải hàng rào an toàn (như trên).
- Viết bằng bash, `set -euo pipefail`, theo lối của các script khác: thông báo tiếng Việt, chú thích tiếng Anh.
- Chỉ được gọi trên runner tự host, ở bước có `if: runner.environment == 'self-hosted'`.
- Kiểm hết các điều dưới đây. Mỗi điều sai in một dòng `RUNNER KHÔNG DÙNG ĐƯỢC: <lý do> (docs/ops/self-hosted-runner.md).`, rồi thoát 1 nếu có ít nhất một dòng. Đủ hết thì in `RUNNER DÙNG ĐƯỢC: ...` và thoát 0.

1. `REPO_VISIBILITY` (workflow truyền `${{ github.event.repository.visibility }}`) là `private` hay `internal`. Rỗng hay `public` thì sai: bắt nhầm khi đặt biến cho repo công khai (ADR-0042 mục 6).
2. `VGL_RUNNER_EPHEMERAL` là `1`. Biến này do image của container đặt (`docs/ops/self-hosted-runner.md`). Nó chỉ đánh dấu container dựng theo tài liệu, không bảo đảm runner chạy một lần. Thiếu biến thì có thể máy đăng ký runner bằng cách khác.
3. Thư mục Gradle (`${GRADLE_USER_HOME:-$HOME/.gradle}`) không có `init.d/`, `init.gradle`, `init.gradle.kts`, `gradle.properties`. Bước này đứng trước `setup-gradle`, vì `setup-gradle` tự đặt init script vào đó.
4. `git status --porcelain --ignored` trong thư mục làm việc rỗng (không `build/`, không ảnh screenshot thừa).
5. Đĩa trống ở thư mục làm việc (`df -Pk .`) ít nhất `RUNNER_CHECK_MIN_FREE_GB` GB: mặc định 20, `--emulator` thì 30 (emulator cần khoảng 7,4 GB cho phân vùng dữ liệu, như chú thích của `ui-test`).
6. Có `--emulator`:
   - `${RUNNER_CHECK_KVM:-/dev/kvm}` mở được để đọc và ghi (`[ -r ] && [ -w ]`);
   - `ANDROID_HOME` có giá trị và ghi được (`[ -w "$ANDROID_HOME" ]`): `android-emulator-runner` cài gói emulator và ảnh hệ thống vào đó (`sdkmanager --install`, `unzip`).

`RUNNER_CHECK_MIN_FREE_GB` và `RUNNER_CHECK_KVM` chỉ để test đè. Script không gọi mạng, không đọc bí mật.

**Workflow**, trên cấu trúc sau T-075, T-076, T-077:

- `pr-check.yml`, job `build-and-test`:
  - `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`; `if:` cấp job của T-077 giữ nguyên.
  - Bước kiểm runner đứng ngay sau `actions/checkout`: `name: Kiểm runner tự host (scripts/runner-check.sh)`, `if: runner.environment == 'self-hosted'`, `env: REPO_VISIBILITY: ${{ github.event.repository.visibility }}`, `run: scripts/runner-check.sh`.
  - Hai bước lọc của T-076 đứng trước checkout. Chúng chỉ đọc API và ghi `$GITHUB_OUTPUT`, không ghi vào thư mục làm việc hay `~/.gradle`. Luật này (ADR-0042 mục 3) có test 3.
  - Chú thích đầu file: một dòng dẫn ADR-0042.
  - Nếu T-075 chưa merge và `ui-test` còn trong file: làm như `ui-test` của `main-guard` dưới đây.
- `main-guard.yml`:
  - Job `guard`: như `build-and-test`. Bước kiểm runner đứng sau `actions/checkout` và trước bước `mode` (`main-guard-verified.sh`). Ở cách `skip` job chỉ đọc API, nhưng vẫn kiểm runner, để một runner hỏng lộ ra sớm. Thứ tự các bước khác, bước `ui` của T-075, `outputs:` và mọi `if:` của ADR-0039 giữ nguyên.
  - Job `ui-test`: `runs-on` như trên. Bước kiểm runner ngay sau `actions/checkout`, gọi `scripts/runner-check.sh --emulator`. Hai bước `Giải phóng đĩa cho emulator` và `Bật KVM cho emulator` thêm `if: runner.environment == 'github-hosted'`; nếu T-075 đã đặt `if:` khác cho các bước này thì nối bằng `&&`. `needs`, `if:` cấp job, `timeout-minutes: 60` giữ nguyên.
  - Chú thích đầu file: một dòng dẫn ADR-0042.
- `staging.yml`, `device-session.yml`, `env-canary.yml`, `start-run.yml`, `run-watchdog.yml`, `status-page.yml`: không sửa. `resume-run.yml`: chỉ thêm `actions: read` cấp workflow cho `resume-check.py` (dưới), `runs-on` giữ nguyên. Test 1 kiểm mọi workflow này giữ `runs-on: ubuntu-latest`.

**`scripts/wait-run.sh`: mã 7, `CHỜ RUNNER`.** Gốc là bản sau T-077, với các mã 0 tới 6. Mã 4 giữ nguyên nghĩa: lần chạy đã xong mà GitHub không cấp máy. Cách xử lý của mã 4 (`rerun-failed-jobs`, `resume-run`) không hợp với lần chạy còn đang chờ, nên cần mã riêng.

Trong vòng chờ, khi lần chạy đã có mà chưa `completed`, script đọc `actions/runs/<id>/jobs?per_page=50` (đã dùng ở cuối script). Lần chạy là "chờ runner tự host" khi đủ cả bốn điều:

1. không job nào của lần chạy có `status == "in_progress"`;
2. có một job `status == "queued"` với `runner_name` rỗng hay `null`;
3. nhãn (`labels`) của job đó có ít nhất một nhãn không khớp `^(ubuntu|windows|macos)-`;
4. job đó được phép chạy từ hơn `WAIT_RUN_RUNNER_QUEUE_MINUTES` phút (mặc định 75).

"Được phép chạy từ" là lúc muộn nhất trong `completed_at` của các job đã xong của lần chạy, hay `created_at` của job khi chưa có job nào xong. Với `ui-test` có `needs: guard`, mốc này là lúc `guard` xong, không phải lúc lần chạy bắt đầu.

Ngưỡng 75 phút lớn hơn `timeout-minutes: 60` của `ui-test`. Máy có 2 chỗ chạy, dùng chung cho nhiều repo, nên một job có thể chờ chính đáng sau một `ui-test` của lần chạy khác. Script không đọc được trạng thái máy: API runner cần quyền quản trị, phiên không có. Vì vậy vẫn còn trường hợp báo sớm khi máy bận lâu. Cách xử lý của mã 7 không phá gì (không chạy lại, không sửa code), nên báo sớm chỉ làm phiên dừng sớm.

Đủ bốn điều thì script in một dòng rồi thoát 7:

`CHỜ RUNNER: job <tên> của <what>, lần chạy <id>, chờ runner tự host (nhãn <nhãn>) từ <mốc>, không còn job nào của lần chạy đang chạy. Không phải lỗi của code: không sửa code, không chạy lại, không revert; ghi vào "Cần người kiểm tra": máy runner hay biến HEAVY_RUNNER của repo (docs/ops/self-hosted-runner.md); phiên làm theo docs/auto-run.md, mục 4 (ADR-0042). <url>`

Chi tiết khác:

- Đọc giờ hiện tại bằng `date -u +%s`; test đè bằng `WAIT_RUN_NOW` (giây Unix).
- Đọc job bị lỗi thì bỏ qua lượt kiểm này và chờ tiếp như cũ.
- Thứ tự kiểm sau T-077 giữ nguyên: PR nháp (mã 6) và xung đột (mã 5) vẫn đi trước. Phần mới chỉ chạy khi đã có lần chạy chưa xong.

**`scripts/resume-check.py`: không mở lại phiên khi job đang chờ runner tự host.**

- Trước khi in danh sách chạy để tiếp tục, script đọc các lần chạy chưa xong của repo: `actions/runs?status=queued&per_page=20` và `actions/runs?status=in_progress&per_page=20`. Với mỗi lần chạy, đọc `actions/runs/<id>/jobs?per_page=50` và dùng cùng bốn điều như `wait-run.sh`, ngưỡng `RESUME_RUNNER_QUEUE_MINUTES` (mặc định 75).
- Có lần chạy như vậy thì không tiếp tục. Stderr ghi `Không tiếp tục: job <tên> (lần chạy <id>) chờ runner tự host ... (ADR-0042).` và mã thoát giữ 0, như các lý do không tiếp tục khác.
- Người sửa máy hay xóa biến, rồi hủy và chạy lại lần chạy đó. Hàng chờ sạch thì `resume-run.yml` tiếp tục phiên ở giờ kế.
- Đọc lỗi thì giữ cách cũ: mã 1, không đổi gì.

**Bảng tra lỗi `docs/ops/failure-playbook.md`.** Mã lấy số tiếp theo lúc làm task, hiện là PB-16 tới PB-18; T-075 cũng sửa bảng này (mục 6).

- PB-16:
  - mẫu `^CHỜ RUNNER:`; thấy ở `wait-run.sh` mã 7; loại `nguoi`;
  - việc phải làm: không chạy lại, không sửa code; làm theo dòng "Job chờ runner tự host" của `auto-run.md` mục 4; ghi vào "Cần người kiểm tra": máy runner tắt hay dịch vụ tạo container dừng; người bật lại máy, hay xóa biến `HEAVY_RUNNER`, rồi hủy và chạy lại lần chạy đang chờ (`docs/ops/self-hosted-runner.md`, mục Máy hỏng).
- PB-17:
  - mẫu `The self-hosted runner: .* lost communication with the server\|The runner has received a shutdown signal`; thấy ở job nặng; loại `nguoi`;
  - việc phải làm: máy tắt, khởi động lại hay mất mạng giữa job; không sửa code; xử lý như PB-16.
- PB-18:
  - mẫu `RUNNER KHÔNG DÙNG ĐƯỢC:`; thấy ở bước kiểm runner của job nặng; loại `nguoi`;
  - việc phải làm: máy runner thiếu đĩa, thiếu KVM hay cấu hình sai; không sửa code, không chạy lại; ghi dòng lỗi vào "Cần người kiểm tra".
- Hết đĩa giữa job (`No space left on device`) không có dòng riêng. Lỗi đó cũng xảy ra trên runner của GitHub. Log của bước lỗi (`gh run view --log-failed`) không có dấu hiệu chắc chắn của runner tự host. Bước kiểm đĩa của `runner-check.sh` đã chặn phần lớn trường hợp.

Mỗi dòng có tệp `scripts/tests/playbook/PB-NN.log`:

- PB-16 lấy từ đầu ra thật của `wait-run.sh` mới trong test;
- PB-17 lấy nguyên văn thông báo của GitHub;
- PB-18 lấy đầu ra thật của `runner-check.sh`.

Sau lần thử của người (mục 4, bước H5, H6), log thật thay cho tệp nếu khác. Mẫu thật của PB-17 ghi lại ở bước H6 (4).

**Tài liệu:**

- `docs/ops/self-hosted-runner.md` (mới), các mục:
  1. **Khi nào dùng.** Chỉ repo private (ADR-0042 mục 6). Job nào lên máy, job nào không (bảng phân loại ở trên, rút gọn). Hàng rào an toàn là cài đặt GitHub và container chạy một lần; `runner-check.sh` chỉ bắt cấu hình nhầm.
  2. **Máy.**
     - Linux x86_64 có KVM: máy chủ riêng, không phải máy ảo không bật ảo hóa lồng.
     - Đề xuất 8 nhân, 32 tới 64 GB RAM, 500 GB SSD; Ubuntu LTS; Docker.
     - Không dùng Mac Apple Silicon: emulator x86_64 của workflow cần KVM.
     - Luật udev trên máy chủ: `/dev/kvm` thuộc nhóm `kvm`, quyền `0660`.
  3. **Image của container.**
     - Nền là runner chính thức của GitHub.
     - Android SDK như `scripts/cloud-setup.sh` (gói `platform-tools`, `platforms;android-36`, `platforms;android-37.0`, `build-tools;36.0.0`), cộng gói emulator và ảnh hệ thống `system-images;android-34;google_apis;x86_64` cài sẵn để job không phải tải.
     - `ANDROID_HOME` thuộc người dùng runner, ghi được.
     - `python3`, `jq`, `git`, `curl`, `unzip`.
     - `ENV VGL_RUNNER_EPHEMERAL=1`.
     - Người dùng không có `sudo`; không init script nào trong `~/.gradle`.
  4. **Dịch vụ tạo runner.**
     - `systemd` chạy N bản (đề xuất 2) của một vòng lặp.
     - Mỗi vòng xin cấu hình just-in-time: `POST /orgs/VGL-Studio/actions/runners/generate-jitconfig` với nhãn `vgl-android` và nhóm mặc định, hay API cấp repo khi chỉ muốn một repo.
     - Chạy `docker run --rm --device /dev/kvm --group-add <gid của nhóm kvm trên máy chủ>` với cấu hình đó. Không mount socket Docker. Giới hạn CPU và RAM mỗi container.
     - Container thoát thì vòng lặp xin cấu hình mới.
     - Token xin cấu hình là fine-grained token chỉ có quyền "Self-hosted runners: write" của tổ chức, hay "Administration: write" của repo khi dùng API cấp repo. Lưu trong tệp chỉ người dùng của dịch vụ đọc được, không bao giờ vào container.
  5. **Cài đặt GitHub, bắt buộc trước khi đặt biến.**
     - Nhóm runner mặc định không cho repo công khai dùng runner: kiểm trong Organization settings, Actions, Runner groups, ghi kết quả.
     - Gói Free không chỉnh được điều này thì đăng ký runner cấp repo.
     - Tắt chạy workflow từ fork của repo private.
     - Rồi mới đặt biến `HEAVY_RUNNER=vgl-android` ở từng repo app private (Settings, Secrets and variables, Actions, Variables; hay `gh variable set HEAVY_RUNNER --body vgl-android --repo VGL-Studio/<repo>`).
  6. **Kiểm sau khi dựng:** các bước H4 tới H6 của plan T-078.
  7. **Vận hành:** dọn image cũ (`docker image prune`), cập nhật image khi runner hay SDK đổi, xem đĩa, theo dõi job chờ (`wait-run.sh` mã 7, PB-16; `resume-check.py` không mở lại phiên).
  8. **Máy hỏng:** xóa biến (`gh variable delete HEAVY_RUNNER --repo VGL-Studio/<repo>`); hủy lần chạy đang chờ rồi chạy lại (Re-run hay `gh workflow run`, hay đẩy commit mới nếu lần chạy lại vẫn giữ nhãn cũ); bật lại máy; đặt lại biến.
  9. **Ngoài phạm vi:** runner cho `scripts/release.sh` và bản production (ADR-0009, ADR-0017); `staging`, `device-session`.
- `docs/ops/staging-and-runs.md`, sau dòng 5 (biến của repo): một câu về biến `HEAVY_RUNNER` tùy chọn, dẫn tới `docs/ops/self-hosted-runner.md`.
- `docs/auto-run.md`:
  - bước 8, danh sách mã của `wait-run.sh` (dòng khoảng 198, sau mã 6 của T-077): thêm mã 7, `CHỜ RUNNER`, "job chờ runner tự host, không phải lỗi của code; làm theo mục 4, dòng Job chờ runner tự host (ADR-0042)";
  - bước 9, câu chờ `main-guard` (dòng khoảng 224): mã 7 cũng không phải đỏ;
  - mục 4, bảng điều kiện kết thúc: thêm dòng "Job chờ runner tự host (`wait-run.sh` mã 7, ADR-0042)". Xử lý: đẩy hết những gì đang có; dòng thời gian ghi workflow, mã lần chạy và mã 7; ghi "Cần người kiểm tra"; không chạy lại, không bắt đầu task mới; giữ danh sách `running`. `resume-check.py` không mở lại phiên khi job còn chờ;
  - mục Tiếp tục phiên, bước 3: dòng thời gian ghi mã 7 thì đọc lại lần chạy đó. Còn chờ: kết thúc phiên ngay, không chạy lại. Đã xong: xử lý như kết quả thường. Bị hủy: chạy lại một lần như mã 4.
- `.claude/skills/merge-pr/SKILL.md`, dòng mã thoát của `wait-run.sh` (T-077 cũng sửa dòng này): thêm "Mã 7 (chờ runner tự host): không chạy lại, báo dev kiểm máy hay xóa biến `HEAVY_RUNNER`".
- `CLAUDE.md`, mục Lệnh: một dòng "Kiểm máy runner tự host ... `scripts/runner-check.sh`, dựng máy theo `docs/ops/self-hosted-runner.md` (ADR-0042)".
- `docs/foundation.md`: sinh lại bằng `scripts/foundation.py` khi ADR-0042 đổi trạng thái.

**Bước của người, ngoài phiên** (`auto_run: false`; AI không làm, không có quyền):

- H1. Trả lời mục 8, nhất là câu 1 (repo công khai). Repo dự án mẫu và `camera-pose` công khai thì không đặt biến cho chúng.
- H2. Chọn và thuê hay bố trí máy (mục 8, câu 2, 3).
- H3. Dựng máy theo `docs/ops/self-hosted-runner.md` sau khi task merge: image, dịch vụ tạo runner, token xin cấu hình. Rồi làm cài đặt GitHub (mục 5 của tài liệu). Điều kiện bắt buộc trước H4: đã xác minh nhóm runner không cho repo công khai, hay đã chuyển sang runner cấp repo (mục 8, câu 4); ghi kết quả vào nhật ký.
- H4 tới H6: mục 4, dòng kiểm tra thủ công.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/runner-check.sh` | Kiểm cấu hình runner tự host trước khi build (mục 3.1) |
| Sửa | `.github/workflows/pr-check.yml` | `runs-on` của `build-and-test` (và `ui-test` nếu còn); bước kiểm runner; `if: runner.environment == 'github-hosted'` cho bước `sudo` nếu `ui-test` còn; một dòng chú thích |
| Sửa | `.github/workflows/main-guard.yml` | `runs-on` của `guard` và `ui-test`; bước kiểm runner; `if:` cho hai bước `sudo` của `ui-test`; một dòng chú thích |
| Sửa | `scripts/wait-run.sh` | Mã 7 `CHỜ RUNNER` (mục 3.1); chú thích cách dùng |
| Sửa | `scripts/resume-check.py` | Không tiếp tục phiên khi job chờ runner tự host (mục 3.1); docstring |
| Sửa | `.github/workflows/resume-run.yml` | `permissions:` cấp workflow thêm `actions: read` (chỉ đọc), để `resume-check.py` đọc được `actions/runs`; một dòng chú thích |
| Sửa | `scripts/tests/run.sh` | Test 1 tới 11 của mục 4 |
| Sửa | `docs/ops/failure-playbook.md` | PB-16, PB-17, PB-18 |
| Tạo | `scripts/tests/playbook/PB-16.log` | Đầu ra `wait-run.sh` mã 7 |
| Tạo | `scripts/tests/playbook/PB-17.log` | Thông báo runner mất kết nối hay bị tắt |
| Tạo | `scripts/tests/playbook/PB-18.log` | Đầu ra `runner-check.sh` khi hết đĩa |
| Tạo | `docs/ops/self-hosted-runner.md` | Dựng và vận hành máy runner (mục 3.1) |
| Sửa | `docs/ops/staging-and-runs.md` | Một câu về biến `HEAVY_RUNNER` |
| Sửa | `docs/auto-run.md` | Bước 8 và 9: mã 7; mục 4: dòng Job chờ runner tự host; mục Tiếp tục phiên bước 3 |
| Sửa | `.claude/skills/merge-pr/SKILL.md` | Mã 7 của `wait-run.sh` |
| Sửa | `CLAUDE.md` | Mục Lệnh: một dòng `runner-check.sh` |
| Sửa | `docs/adr/0042-runner-tu-host-cho-job-nang.md` | Chỉ khi rà soát độc lập thấy ADR lệch với code đã làm; không đổi `status` |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` khi cần |
| Sửa | `docs/features/quy-trinh/plans/T-078-runner-tu-host.md` | Nhật ký thực hiện, kết quả bước H3 tới H6 |
| Sửa | `docs/features/quy-trinh/tickets/T-078-runner-tu-host.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020). Tìm bằng `grep -rn 'pr-check.yml\|main-guard.yml\|wait-run.sh\|resume-check.py\|failure-playbook' scripts/ .github/ .claude/skills/ docs/ops/`:

- `scripts/main-guard-verified.sh` đọc tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)` của `pr-check.yml`. `GATE_FILES` có `pr-check.yml` và `main-guard.yml`.
  - Plan không đổi tên job hay bước.
  - Commit merge của task đổi `main-guard.yml`, nên `main-guard` chạy `full`. Đúng ý: đó là lần đầu cổng mới chạy trên main.
  - Không sửa.
- `scripts/merge-if-green.sh` đọc tên check run `build-and-test`. Plan không đổi tên. Không sửa (file "Không được làm").
- `scripts/find-breaking-commit.sh` và `scripts/main-guard-status.sh` đọc check run `guard`, và `ui-test` sau T-075. Plan không đổi tên, không thêm `if:` cấp job. Không sửa.
- `scripts/failure-kind.sh --playbook` đọc bảng tra lỗi theo thứ tự dòng. Mẫu của PB-16 tới PB-18 không trùng dòng nào có sẵn; test 8 kiểm.
- Các nơi chỉ in hay nhắc lệnh `wait-run.sh`, không xử lý mã thoát. Mã 7 mới không đổi nghĩa mã cũ. Không sửa:
  - `scripts/main-guard-status.sh` dòng 9, 56, 101;
  - `scripts/merge-if-green.sh` dòng 326;
  - `.claude/skills/quick-lane/SKILL.md`, `docs/CLAUDE.md`, `docs/templates/run.md`.

  Skill `merge-pr` liệt kê từng mã thoát nên được sửa.
- `.github/workflows/resume-run.yml` gọi `scripts/resume-check.py`.
  - Đầu ra (danh sách chạy hay rỗng) và mã thoát giữ nguyên.
  - `permissions:` cấp workflow hiện chỉ có `contents: read`, `issues: read` (đọc ngày 2026-10-30), nên plan thêm `actions: read` (có trong bảng trên). Job `idle` có `permissions:` riêng (`contents: read`, `actions: write`), không đổi.
  - Thiếu quyền thì `gh api` trả 403, và `resume-check.py` thoát 1 như mọi lỗi đọc: phiên không được tiếp tục, an toàn nhưng không làm việc. Test 11 kiểm dòng quyền.
- `scripts/check.sh` dòng 4, 5 và `scripts/failure-kind.sh`: chỉ chú thích nhắc tên workflow. Không sửa.
- `scripts/tests/playbook/PB-02.log` là log mẫu có tên `pr-check.yml`. Không đổi.
- `docs/ops/release.md`, `docs/ops/setup-new-app.md`, `docs/ops/pre-run-review.md`, `docs/ops/test-sessions.md`, `docs/ops/token-usage.md`, `docs/ops/quick-lane.md`, `docs/ops/session-capabilities.md`: nhắc `pr-check`, `main-guard` hay `wait-run.sh` mà không nói máy chạy hay mã thoát. Không sửa.
- Các workflow sau không sửa; test 1 đọc chúng: `.github/workflows/status-page.yml`, `.github/workflows/start-run.yml`, `.github/workflows/device-session.yml`, `.github/workflows/staging.yml`, `.github/workflows/run-watchdog.yml`, `.github/workflows/env-canary.yml`.
- `scripts/template-sync.sh` chép workflow và script của dự án mẫu về app. App nhận thay đổi ở đợt đồng bộ sau; biến đặt riêng ở từng repo app.

Các file khác nhắc tới file plan sửa nhưng không bị ảnh hưởng. Chúng chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của phần bị đổi. Plan không đổi tên, xóa hay đánh số lại mục nào, chỉ thêm câu, dòng hay mã thoát mới:

- dẫn `docs/auto-run.md`: `scripts/cloud-env-check.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py` (có trong mục 3.2), `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/token-usage.py`;
- dẫn `docs/ops/staging-and-runs.md`: `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`, `scripts/run-watchdog.py`, `scripts/setup-check.sh`. Plan chỉ thêm một câu sau dòng 5, số mục giữ nguyên;
- dẫn `docs/ops/failure-playbook.md`: `scripts/push-probe.sh` (dẫn PB-14). Plan thêm ba dòng, không đổi mã dòng cũ;
- dẫn `CLAUDE.md` (plan chỉ thêm một dòng vào mục Lệnh):
  - `.github/ISSUE_TEMPLATE/from-app.yml`;
  - `scripts/new-app.sh`: thay tên app trong `CLAUDE.md`, mà dòng mới không có tên app;
  - `scripts/local-setup.sh`, `scripts/run-watchdog.py`;
  - `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt`: chú thích dẫn quy ước;
- `scripts/local-setup.sh` nhắc `scripts/tests/run.sh`: không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN`: chỉ thêm `actions: read` (chỉ đọc) cấp workflow cho `resume-run.yml`; mọi quyền khác giữ nguyên; không thêm bí mật. Biến mới `HEAVY_RUNNER` (cấp repo, không bí mật) do người đặt.
- **Thư viện và SDK:** Không thêm. Action có sẵn: `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`, `reactivecircus/android-emulator-runner@v2`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** ba action chạy trong container không có `sudo`. Chưa chạy thử được vì chưa có máy.
  - `actions/setup-java@v5` và `gradle/actions/setup-gradle@v4` cài vào thư mục người dùng (`RUNNER_TOOL_CACHE`, `~/.gradle`).
  - `android-emulator-runner@v2` ghi vào `ANDROID_HOME` (`sdkmanager --install`, `unzip`) và cần quyền đọc ghi `/dev/kvm`. Hai điều này do image (`ANDROID_HOME` ghi được) và máy chủ (luật udev, `--group-add`) lo; `runner-check.sh --emulator` kiểm cả hai.

  Phần này chỉ lộ ra ở bước H5 của người, sau khi task đã merge.
- **Điều kiện chuyển:** ở bước H5, job nặng đỏ trên runner tự host ở một bước cài đặt hay khởi động emulator, trong khi cùng commit xanh trên runner của GitHub.
- **Cách làm thay:**
  - Sửa image hay cấu hình container theo `docs/ops/self-hosted-runner.md`, không sửa workflow: cài sẵn JDK 17, đặt `RUNNER_TOOL_CACHE` ghi được, cài sẵn gói emulator và ảnh hệ thống, kiểm `--group-add`.
  - Không sửa được bằng image thì xóa biến (mọi job về runner của GitHub) và mở ticket mới.
  - Tiêu chí và test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin.

- Với runner của GitHub (biến chưa đặt), mọi bước của workflow chạy như cũ: bước kiểm runner bị bỏ qua, bước `sudo` vẫn chạy (`runner.environment == 'github-hosted'`).
- Test có sẵn giữ nguyên câu khẳng định:
  - của `pr-check.yml`, `main-guard.yml`: bước, thứ tự, `if:` của ADR-0039 và của T-075 tới T-077;
  - của `wait-run.sh`: các mã 0 tới 6;
  - của `resume-check.py`: các lý do tiếp tục hay không.
- Các test có sẵn của `wait-run.sh` và `resume-check.py` dùng fixture không có trường `labels` hay không có lần chạy chưa xong: kết quả giữ nguyên. Test 7 và test 10 có ca đối chứng cho điều này.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn:

- `expect "<tên>" "<mong đợi>" "<thực tế>"`;
- `wf_job` (dòng khoảng 1369) in các dòng của một job;
- `gh` giả `scripts/tests/fake-gh` với `FIXTURE`.

Hai hàm phụ trợ mới vào commit test (L-025):

- `job_kind <workflow> <job>`: bỏ các dòng chú thích (`^[[:space:]]*#`) trước khi tìm. In `secret` khi job có `secrets.`; `heavy` khi job có `scripts/check.sh`, `./gradlew` hay `android-emulator-runner` mà không có `secrets.`; `light` khi khác.
- `heavy_jobs`: in mọi cặp `<workflow> <job>` loại `heavy` trong `.github/workflows/*.yml`, mỗi cặp một dòng. Liệt kê job bằng awk trên khối `jobs:` (dòng thụt hai dấu cách, kết thúc bằng `:`).

Các test chạy cả trên bản sao hỏng của workflow trong `$work` (như test 13 của T-074), để chứng minh test bắt được lỗi.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phạm vi 2: job nặng không bí mật đọc biến; job có bí mật và job nhẹ giữ runner của GitHub | Test cấu trúc | 1. `runners: heavy jobs read HEAVY_RUNNER, every other job stays on ubuntu-latest`. Với mọi job của mọi workflow: loại `heavy` có đúng một dòng `    runs-on: ${{ vars.HEAVY_RUNNER \|\| 'ubuntu-latest' }}`; loại `secret` và `light` có đúng một dòng `    runs-on: ubuntu-latest`. Mong `yes`. Bản sao hỏng: (a) `staging.yml` dùng `HEAVY_RUNNER`; (b) `guard` giữ `ubuntu-latest`; mong `no` cả hai. Thêm: một job nhẹ chỉ có `./gradlew` trong dòng chú thích vẫn là `light` (ghi chú 9) |
| Phạm vi 2: tập job nặng đúng | Test cấu trúc | 2. `runners: the heavy jobs are build-and-test, guard and ui-test`. `heavy_jobs` phải có `pr-check.yml build-and-test`, `main-guard.yml guard` và `main-guard.yml ui-test` (sau T-075; trước T-075 là `pr-check.yml ui-test`). Không có job nào của `staging.yml`, `device-session.yml`. Đối chứng L-055: test 1 qua trên một bộ workflow không có job nặng nào, còn test 2 thất bại khi đó |
| ADR-0042 mục 3: job nặng kiểm runner trước mọi bước cài hay build; bước trước đó không ghi vào thư mục làm việc hay `~/.gradle` | Test cấu trúc | 3. `runners: every heavy job checks the runner right after checkout`. Trong mỗi job nặng, bước đứng ngay sau `uses: actions/checkout@v5` phải có `if: runner.environment == 'self-hosted'`, `REPO_VISIBILITY: ${{ github.event.repository.visibility }}` và `run: scripts/runner-check.sh` (có `--emulator` khi job có `android-emulator-runner`, không có khi không). Bước đó đứng trước `setup-java`, `setup-gradle`, bước `mode` và mọi bước có `gradlew`, `check.sh`, `tests/run.sh`. Các bước đứng trước bước kiểm (kể cả bước lọc của T-076 trước checkout) chỉ được là `actions/checkout`, `dorny/paths-filter`, hay bước `run:` mà mọi lệnh ghi tệp chỉ ghi vào `"$GITHUB_OUTPUT"` hay `"$GITHUB_STEP_SUMMARY"`, và không chứa `gradle`, `$HOME`, `~/`. Bản sao hỏng, mong thất bại mỗi bản: bước kiểm đứng sau `setup-gradle`; thiếu `--emulator` ở `ui-test`; `if:` thiếu; một bước trước checkout có `echo x > ~/.gradle/init.d/a.gradle`. Đối chứng: `pr-check.yml` theo T-076 (lọc trước checkout) qua |
| Phạm vi 3: bước `sudo` chỉ trên máy của GitHub | Test cấu trúc | 4. `runners: sudo steps of heavy jobs run only on GitHub runners`. Mọi bước có `sudo ` trong job nặng có dòng `if:` chứa `runner.environment == 'github-hosted'`. Bản sao hỏng bỏ `if:` của bước `Bật KVM cho emulator`: mong thất bại. Đối chứng: `device-session.yml` (job `secret`) giữ bước `sudo` không có `if:` mà test không thất bại |
| Phạm vi 4: thư mục làm việc sạch | Test cấu trúc | 5. `runners: heavy jobs keep the default clean checkout`. Không job nặng nào có `clean: false`. Bản sao hỏng có `clean: false`: mong thất bại |
| `runner-check.sh` bắt cấu hình nhầm | Test của script | 6. `runner-check: ...` (chi tiết ngay dưới bảng) |
| ADR-0042 mục 5: job chờ runner tự host | Test của script | 7. `wait-run: a job waiting for a self-hosted runner with nothing running is CHỜ RUNNER` (chi tiết ngay dưới bảng) |
| Bảng tra lỗi | Test của script | 8. `playbook: PB-16 waiting for a self-hosted runner -> nguoi`, `playbook: PB-17 runner lost communication -> nguoi` (thêm một tệp tạm chỉ có `The runner has received a shutdown signal` → `PB-17 nguoi`), `playbook: PB-18 runner not usable -> nguoi`. Đối chứng: một tệp tạm chỉ có `No space left on device` không khớp PB-18. Test có sẵn "mỗi dòng có tệp log" kiểm ba tệp mới |
| Tệp PB-16 khớp đầu ra thật | Test của script | 9. `playbook: PB-16.log is what wait-run.sh prints`: dòng đầu của `scripts/tests/playbook/PB-16.log` bằng dòng mà test 7 nhận được, bỏ phần đường dẫn lần chạy |
| Không mở lại phiên khi job chờ runner tự host | Test của script | 10. `resume-check: no resume while a job waits for a self-hosted runner` (chi tiết ngay dưới bảng) |
| `resume-run.yml` đọc được lần chạy | Test cấu trúc | 11. `resume-run: the workflow may read Actions runs`: `wf_level .github/workflows/resume-run.yml` có dòng `  actions: read` và không có `actions: write`; test có sẵn "only the idle job writes Actions" giữ nguyên. Bản sao hỏng thiếu dòng: mong thất bại |
| DoD 4: không đổi gì khi chưa có biến | Test cấu trúc | Test 1 tới 5 kiểm: `runs-on` có `\|\| 'ubuntu-latest'`; bước kiểm runner bỏ qua trên máy GitHub; bước `sudo` chạy trên máy GitHub. CI của PR task chạy trên runner của GitHub với chính workflow mới: `pr-check` và `main-guard` xanh là bằng chứng |
| Tài liệu đúng và đủ (DoD 3) | Kiểm tra thủ công (người: đọc tài liệu trước khi dựng máy) | Người đọc `docs/ops/self-hosted-runner.md` và dựng đúng theo tài liệu ở bước H3. Chỗ thiếu ghi vào nhật ký, sửa tài liệu trong PR nhỏ. Ghi kết quả kiểm nhóm runner (repo công khai bị chặn hay đã dùng runner cấp repo) trước H4 |
| H4: runner nhận job | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | (1) Sau H3: `gh variable set HEAVY_RUNNER --body vgl-android --repo VGL-Studio/<repo>`. (2) Mở một PR code nhỏ, ví dụ một dòng chú thích trong `scripts/`. (3) `gh api "repos/VGL-Studio/<repo>/actions/runs/<id>/jobs" --jq '.jobs[] \| {name, labels, runner_name}'`: `build-and-test` có `labels` `["vgl-android"]` và `runner_name` là tên container; log có `RUNNER DÙNG ĐƯỢC`; job xanh. (4) Merge PR đó: `guard` kiểm như vậy |
| H5: `ui-test` và cách ly giữa job (DoD 1) | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | (1) Một commit chạm `app/` trên main: `ui-test` của `main-guard` chạy trên `vgl-android` sau `guard` và xanh; log có `RUNNER DÙNG ĐƯỢC` của `--emulator`. (2) Thử cách ly: trên một nhánh nháp, thêm tạm một bước ghi `~/.gradle/init.d/probe.gradle` vào job `build-and-test` (PR không merge). Rồi chạy một job nặng khác: bước kiểm runner của job sau vẫn `RUNNER DÙNG ĐƯỢC` (container mới). (3) Sau vài ngày, đọc `gh api "/organizations/VGL-Studio/settings/billing/usage?year=<năm>&month=<tháng>"`: phút `Actions Linux` của repo theo ngày giảm tương ứng với các job đã lên máy |
| H6: xóa biến thì quay về runner của GitHub (DoD 2), thử một lần | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | Các bước (1) tới (6) ngay dưới bảng |

**Test 6, `runner-check: ...`.**

- Dựng: một repo git tạm `"$work/rc"` (một commit, `.gitignore` có `build/`); `HOME` tạm; `GRADLE_USER_HOME` không đặt; `VGL_RUNNER_EPHEMERAL=1`; `REPO_VISIBILITY=private`; `RUNNER_CHECK_MIN_FREE_GB=0`.
- Hàm phụ trợ `rc [args]` in `<mã thoát> <số dòng RUNNER KHÔNG DÙNG ĐƯỢC>`.
- Các ca:
  - (a) đủ điều kiện → `0 0`, có dòng `RUNNER DÙNG ĐƯỢC`;
  - (b) `REPO_VISIBILITY` là `public`, rồi rỗng → `1 1` mỗi lần; `internal` → `0 0`;
  - (c) `VGL_RUNNER_EPHEMERAL` không đặt → `1 1`;
  - (d) lần lượt tạo `$HOME/.gradle/init.d/x.gradle`, `init.gradle`, `init.gradle.kts`, `gradle.properties` → `1 1` mỗi lần. Với `GRADLE_USER_HOME="$work/gh"` có `init.d/` → `1 1`. `$HOME/.gradle/init.d` có mà `GRADLE_USER_HOME` trỏ thư mục sạch → `0 0`. `$HOME/.gradle/caches/` có (thư mục khác) → `0 0`;
  - (e) tạo `build/x` (bị bỏ qua), rồi `core/x/src/test/screenshots/a.png` chưa track → `1 1` mỗi lần;
  - (f) `RUNNER_CHECK_MIN_FREE_GB=999999` → `1 1`;
  - (g) `--emulator`:
    - `RUNNER_CHECK_KVM="$work/nokvm"` (không có) → `1 1`;
    - một tệp tạm đọc ghi được, `ANDROID_HOME` là thư mục tạm ghi được → `0 0`;
    - không `--emulator` mà `RUNNER_CHECK_KVM` không có → `0 0` (đối chứng);
    - KVM được mà `ANDROID_HOME` rỗng → `1 1`;
    - KVM được mà `ANDROID_HOME` là thư mục chỉ đọc (`chmod 555`) → `1 1`. Bỏ qua ca này khi chạy bằng root (`id -u` là 0), như các test quyền có sẵn;
  - (h) ba lỗi cùng lúc (public, không ephemeral, `init.d`) → `1 3`: script kiểm hết, không dừng ở lỗi đầu;
  - tham số lạ → mã 2.

**Test 7, `wait-run: a job waiting for a self-hosted runner with nothing running is CHỜ RUNNER`.**

- Dựng: `WAIT_RUN_NOW` cố định, dùng `pending` có sẵn, `T` = `WAIT_RUN_NOW`.
- Ca chính: jobs `[{"id": 120, "name": "guard", "status": "queued", "runner_name": null, "labels": ["vgl-android"], "created_at": <T - 90 phút>}]` → `7 CHỜ`. Dòng in ra có `chờ runner tự host` và `vgl-android`.
- Ca `needs`, theo `ui-test` của T-075:
  - `guard` `completed`, `completed_at` <T - 10 phút>; `ui-test` `queued`, `runner_name` null, nhãn `vgl-android`, `created_at` <T - 90 phút> → `2 CHƯA`: mốc là lúc `guard` xong;
  - cùng dữ liệu, `guard` `completed_at` <T - 80 phút> → `7 CHỜ`.
- Đối chứng, mỗi ca → `2 CHƯA`:
  - cùng job, `created_at` <T - 30 phút>;
  - nhãn `["ubuntu-latest"]`, <T - 90 phút>;
  - job `queued` cũ mà một job khác của lần chạy `in_progress`;
  - job `queued` có `runner_name` khác rỗng;
  - `WAIT_RUN_RUNNER_QUEUE_MINUTES=120` với job <T - 90 phút>;
  - câu trả lời jobs lỗi (`errors` của `fake-gh`).
- Ca `--workflow main-guard.yml --since` cùng dữ liệu ca chính → `7 CHỜ`.
- Ca T-077: PR nháp vẫn ra mã 6 trước (đối chứng thứ tự kiểm). Test có sẵn của mã 4 giữ nguyên.

**Test 10, `resume-check: no resume while a job waits for a self-hosted runner`.**

- Dựng: dữ liệu của test có sẵn "phiên dừng được tiếp tục" (danh sách `running`, không commit 90 phút, mong in danh sách). Thêm `actions/runs?status=queued&per_page=20` có lần chạy 91, và `actions/runs/91/jobs?per_page=50` có job như ca chính của test 7. `RESUME_NOW` cố định.
- Ca chính: mong đầu ra rỗng, stderr có `chờ runner tự host`, mã 0.
- Đối chứng: job `queued` <T - 30 phút> → in danh sách (tiếp tục). Không có lần chạy chưa xong → in danh sách. Đọc `actions/runs` lỗi → mã 1, như lỗi đọc có sẵn.

**H6, các bước:**

1. `gh variable delete HEAVY_RUNNER --repo VGL-Studio/<repo>`.
2. `gh workflow run main-guard.yml --repo VGL-Studio/<repo> --ref main`.
3. `scripts/wait-run.sh --workflow main-guard.yml --since <giờ vừa chạy>` xanh. Lệnh `gh api .../jobs` như H4 cho `labels` `["ubuntu-latest"]`, `runner_name` bắt đầu bằng `GitHub Actions`. Log không có bước kiểm runner (bị bỏ qua); có bước `Bật KVM` của `ui-test` nếu job chạy.
4. Thử máy tắt:
   - đặt lại biến, dừng dịch vụ tạo runner, đẩy một PR code;
   - `wait-run.sh --pr <số>` trả mã 7, dòng có `chờ runner tự host`, khoảng 75 phút sau khi job được phép chạy (đặt `WAIT_RUN_RUNNER_QUEUE_MINUTES=5` để thử nhanh);
   - `python3 scripts/resume-check.py` (với `RESUME_RUNNER_QUEUE_MINUTES=5`) ghi `Không tiếp tục`;
   - giữa một job đang chạy, dừng container: ghi mẫu log thật của PB-17.
5. Xóa biến, bấm Re-run lần chạy đang chờ. Ghi lại lần chạy lại nhận nhãn mới hay giữ nhãn cũ (chưa xác minh, mục 8 câu 6). Cùng lúc thử một job có `needs` (`ui-test` sau `guard`) nếu commit có.
6. Bật dịch vụ, đặt lại biến.

Ghi lệnh, mã lần chạy và kết quả vào nhật ký.

**Ngoại lệ viết test trước:** các phần sau không có hành vi để kiểm trong phiên:

- tài liệu: `docs/ops/self-hosted-runner.md`, `docs/ops/staging-and-runs.md`, `docs/auto-run.md`, `.claude/skills/merge-pr/SKILL.md`, `CLAUDE.md`, `foundation.md`;
- dòng chú thích đầu hai workflow;
- các bước H3 tới H6, cần máy thật.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, trên bản sao của `pr-check.yml` và `main-guard.yml` hiện tại (commit `d7d5bc6`) trong thư mục tạm, không commit:
  - `build-and-test` và `guard` đổi sang `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`;
  - thêm bước `if: runner.environment == 'self-hosted'` gọi `scripts/runner-check.sh`;
  - bước `Giải phóng đĩa cho emulator` thêm `if: runner.environment == 'github-hosted'`.
- **Lệnh:** `actionlint -shellcheck= <hai bản sao>` (actionlint 1.7.12), với hai đối chứng:
  - đổi `vars.` thành `secrets.` trong `runs-on`;
  - viết sai `runner.enviroment`.

  Chưa chạy `scripts/check.sh` trên runner tự host vì chưa có máy; phần này là bước H4, H5 của người, sau khi task merge. Trong phiên, `scripts/check.sh` chạy như mọi task (bước 5) và không đổi hành vi trên máy dev.
- **Kết quả:**
  - Bản đúng: actionlint mã 0.
  - Đối chứng: mã 1, kèm hai thông báo `context "secrets" is not allowed here. available contexts are "github", "inputs", "matrix", "needs", "strategy", "vars"` và `property "enviroment" is not defined in object type {arch; debug; environment; name; os; temp; tool_cache}`.
  - Vậy `vars` dùng được trong `runs-on`, và `runner.environment` là thuộc tính có thật (khớp tài liệu GitHub, mục 8 nguồn 5).
  - Phần chưa thử được (action trong container không `sudo`, KVM) có phương án ở mục 3.4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI: `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/wait-run.sh`, `scripts/resume-check.py`, `.github/workflows/resume-run.yml`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai workflow, một script mới, hai script sửa, test, bảng tra lỗi, năm tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 3 | Ticket rõ. Nhưng repo dự án mẫu và `camera-pose` đã công khai (mục 8, câu 1), nên lợi ích của chính ticket không còn cho hai repo này. Máy và ngân sách chưa chọn. Ba plan T-075 tới T-077 chưa merge |
| C. Khả năng kiểm chứng tự động | 2 | Cấu trúc workflow và ba script có test; chạy thật trên runner tự host chỉ kiểm được bằng tay sau khi có máy |
| D. Ảnh hưởng chéo | 3 | Mọi PR và mọi commit của main, ở mọi app sau đồng bộ; máy hỏng mà biến còn thì mọi job nặng chờ |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng runner tự host, `runner.environment`, `vars` trong `runs-on`, runner just-in-time; đã chạy thử phần cú pháp (mục 4.1) |
| **Tổng** | 12 | **Mức: Cao (điểm chặn 7, tổng từ 9)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm:
  - `pr-check.yml`, `main-guard.yml`: mọi PR và mọi commit của main;
  - `wait-run.sh`: mọi phiên chờ CI;
  - `resume-check.py`: mọi lần tiếp tục phiên;
  - `failure-playbook.md`, `docs/auto-run.md`, skill `merge-pr`.
- Task khác có thể xung đột:
  - **T-075 (PR #318):** sửa `main-guard.yml` (job `ui-test` mới, bước `ui` của `guard`), `pr-check.yml` (bỏ `ui-test`), `docs/auto-run.md`. Viết lại PB-01, PB-10, PB-11, PB-13, PB-15 và có thể thêm dòng PB mới: mã của T-078 lấy số tiếp theo lúc làm.
  - **T-076 (PR #315):** đổi cấu trúc `pr-check.yml` (một job, hai bước lọc trước checkout).
  - **T-077 (PR #316):** sửa `pr-check.yml` (`types`, `if:` cấp job bỏ PR nháp), `wait-run.sh` (mã 6, lần chạy `skipped`), `merge-if-green.sh`, `docs/auto-run.md` bước 8 và skill `merge-pr` (cùng dòng mã thoát mà T-078 thêm mã 7).
  - Cả ba làm trước T-078 (`depends_on: [T-076, T-077]`; T-076 phụ thuộc T-075).
  - ADR-0040 (T-075) và ADR-0041 (T-077) có thể thêm vế vào `foundation.md`: sinh lại sau khi merge.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow và script của dự án mẫu. Biến `HEAVY_RUNNER` đặt riêng ở từng repo, không đồng bộ.
- Đề xuất thứ tự: làm có người, sau T-075, T-076, T-077. Merge được trước khi có máy, vì biến chưa đặt. Bước H3 tới H6 sau khi merge.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu `pr-check.yml`, `main-guard.yml`, `wait-run.sh` trên main với giả định ở mục 2. Dừng và soạn plan phiên bản 2 khi gặp một trong các trường hợp sau, vì file `.github/` mới ngoài mục 3.2 là file "Không được làm":
  - T-075 đặt `ui-test` ở workflow khác `main-guard.yml`;
  - T-076, T-077 đổi tên job `build-and-test`, `guard`;
  - T-077 đã dùng mã 7 của `wait-run.sh`.
- Cần thêm bí mật, quyền ghi mới, hay đưa một job có `secrets.` lên `HEAVY_RUNNER`: dừng.
- Cần sửa `scripts/merge-if-green.sh`, `scripts/check.sh` hay `scripts/release.sh`: dừng.
- Không giữ được hành vi cũ trên runner của GitHub khi biến chưa đặt (một bước mới chạy trên máy của GitHub, hay bước `sudo` bị bỏ qua ở đó): dừng.

## 8. Câu hỏi cho người duyệt

1. **Repo công khai.**
   - Hiện trạng ngày 2026-10-30:
     - `gh repo list VGL-Studio` cho thấy `app-project-template` và `camera-pose` (cùng `terms`, `dalita-studio`) là `PUBLIC`.
     - Bảng tính tiền của tổ chức (`/organizations/VGL-Studio/settings/billing/usage`, tháng 10): từ 2026-10-23 tới 2026-10-29, mỗi ngày dự án mẫu tốn 2 tới 3,6 USD. Ngày 2026-10-30, dự án mẫu 136 phút và `camera-pose` 34 phút đều 0 USD.
     - Runner chuẩn của GitHub miễn phí cho repo công khai (nguồn 2).
   - Có vẻ hai repo vừa được chuyển sang công khai và phút của chúng không còn tính tiền. Điều này mâu thuẫn với yêu cầu gốc ("repo private") và với lợi ích của T-078.
   - GitHub khuyên không dùng runner tự host cho repo công khai (nguồn 6), vì ai cũng mở được PR từ fork.
   - Đề xuất:
     - Nếu công khai là quyết định lâu dài: không đặt `HEAVY_RUNNER` cho hai repo này (ADR-0042 mục 6; `runner-check.sh` cũng bắt nhầm này). Duyệt ADR-0042 để có sẵn cho app private sau này, và hoãn task (mục 9) tới khi có app private tốn phút. Cũng nên xem lại T-075 tới T-077, vì phút của repo công khai không còn tính tiền.
     - Nếu công khai là tạm thời hay nhầm: làm theo plan này.
     - Thêm: repo công khai cho người lạ đọc toàn bộ quy trình, tài liệu, bài học. `scripts/secret-scan.py` vẫn chặn bí mật, nhưng nên xác nhận đây là điều mong muốn.
2. **Máy nào chạy runner** (ticket, câu 1).
   - Đề xuất: máy chủ riêng thuê có KVM, Linux x86_64, khoảng 8 nhân, 64 GB RAM, 500 GB SSD. Ticket ước khoảng 40 tới 60 USD mỗi tháng; giá chưa kiểm.
   - Lý do: emulator x86_64 của workflow cần KVM; máy ảo thường không bật ảo hóa lồng; máy văn phòng phụ thuộc điện và mạng.
   - Loại Mac Apple Silicon: không chạy được emulator x86_64 trong container.
   - Nếu văn phòng có máy Linux x86 bật 24/7 thì dùng máy đó trước.
   - So sánh: phần còn lại sau T-074 tới T-077 ước khoảng 100 tới 150 USD mỗi tháng cho hai repo (yêu cầu gốc, trừ phần cách 2 tới 5 bớt). Với repo công khai, con số đó là 0 (câu 1).
3. **Một máy hay mỗi app một máy** (ticket, câu 2).
   - Đề xuất: một máy cho cả tổ chức, 2 container chạy cùng lúc (một `ui-test` tốn khoảng 4 nhân, 8 GB RAM). Runner cấp tổ chức trong nhóm mặc định, chỉ repo private dùng.
   - Biến `HEAVY_RUNNER` đặt riêng từng repo app (câu 4).
   - Muốn giới hạn chắc chắn theo repo trên gói Free: đăng ký runner cấp repo, mỗi repo một dịch vụ tạo container trên cùng máy, token xin cấu hình cấp repo.
   - Máy 2 chỗ dùng chung thì job có thể chờ lâu khi nhiều repo chạy `ui-test` cùng lúc; ngưỡng 75 phút của mã 7 tính điều này.
4. **Kết quả tra cứu (ticket, câu 3 và 4)**, tài liệu GitHub đọc ngày 2026-10-30. **Câu 4 phải được trả lời và kiểm trên GitHub trước bước H4.**
   - **Câu 3, giá.**
     - Runner tự host không tính phút: "Self-hosted runners" nằm trong danh sách "remains free" (nguồn 2).
     - Phí nền tảng 0,002 USD mỗi phút công bố 2025-12-16, dự kiến từ 2026-03-01, đã bị hoãn: "We're postponing the announced billing change for self-hosted GitHub Actions to take time to re-evaluate our approach". Không có ngày mới (nguồn 1).
     - Hoãn không phải hủy, GitHub có thể thu lại. Khi đó 1.000 phút chỉ còn 2 USD, vẫn rẻ hơn runner của GitHub (khoảng 6 USD).
   - **Câu 4, nhóm runner.**
     - "Organization owners using the GitHub Team plan can create additional organization-level runner groups" (nguồn 3). Gói Free chỉ có nhóm mặc định.
     - "By default, only private repositories can access runners in a runner group" (nguồn 3).
     - Tài liệu không nói rõ gói Free đổi được quyền theo repo của nhóm mặc định hay không: **chưa xác minh**. Người kiểm trong Organization settings, Actions, Runner groups. Không chắc thì dùng runner cấp repo.
   - **Phát hiện thêm, đổi cách làm của ticket.** "Organization-level secrets and variables are not accessible by private repositories for GitHub Free" (nguồn 4). Biến chọn runner vì vậy là biến cấp repo, đặt ở từng repo, không phải biến tổ chức như ticket viết. Đồng ý?
5. **Phương án an toàn.** Đề xuất chạy một lần trong container, chỉ cho job không đọc bí mật. `staging` và `device-session` (khoảng 13% số phút) giữ runner của GitHub (mục 3.1). `runner-check.sh` chỉ bắt cấu hình nhầm; hàng rào là cài đặt GitHub và container. Đồng ý, hay muốn tách runner riêng cho hai job có bí mật (ticket mới)?
6. **Chạy lại sau khi xóa biến.** Chưa xác minh: bấm Re-run một lần chạy đã tạo khi biến còn có tính lại `runs-on` theo biến mới không. Bước H6 thử và ghi lại; tài liệu vận hành viết cả cách thay (`gh workflow run`, commit mới).
7. **Quyền đọc cho `resume-run.yml`.** Thêm `actions: read` cấp workflow để `resume-check.py` thấy job chờ runner tự host (mục 3.1). Chỉ đọc. Đề xuất đồng ý.
8. **ADR-0042.** Đồng ý thì khi duyệt plan, ghi `status: accepted` và chạy `scripts/foundation.py`. Có thể duyệt ADR mà hoãn task (mục 9).

Nguồn (đọc ngày 2026-10-30):

1. GitHub Changelog, "Update to GitHub Actions pricing", 2025-12-16: https://github.blog/changelog/2025-12-16-coming-soon-simpler-pricing-and-a-better-experience-for-github-actions/
2. GitHub Docs, "GitHub Actions billing": https://docs.github.com/en/billing/concepts/product-billing/github-actions
3. GitHub Docs, "Managing access to self-hosted runners using groups": https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/manage-access và "Runner groups": https://docs.github.com/en/actions/concepts/runners/runner-groups
4. GitHub Docs, "Store information in variables": https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-variables
5. GitHub Docs, "Contexts reference" (`runner.environment`; `vars` dùng được trong `jobs.<job_id>.runs-on`): https://docs.github.com/en/actions/reference/workflows-and-actions/contexts
6. GitHub Docs, "Secure use reference" (runner tự host, runner just-in-time): https://docs.github.com/en/actions/reference/security/secure-use

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện / Duyệt ADR-0042, hoãn task (plan giữ `draft`, ticket giữ `backlog` tới khi có repo private cần runner tự host; mục 8, câu 1)
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
