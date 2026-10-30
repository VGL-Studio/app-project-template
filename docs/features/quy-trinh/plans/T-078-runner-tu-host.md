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

Job nặng không đọc bí mật (`build-and-test` của `pr-check`, `guard` và job emulator của `main-guard`) khai báo `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`. Biến `HEAVY_RUNNER` của repo chọn runner tự host; xóa biến là quay về runner của GitHub. Runner tự host chạy một lần trong container mới cho mỗi job. Mỗi job nặng tự kiểm runner bằng script mới `scripts/runner-check.sh` trước khi build. `staging`, `device-session` và job nhẹ giữ runner của GitHub. `scripts/wait-run.sh` báo job chờ runner tự host quá lâu là `HẠ TẦNG`. Kiểm bằng test tĩnh trên mọi workflow và test của hai script trong `scripts/tests/run.sh`. Dựng máy, cài runner, đặt biến và lần thử quay về là việc của người, ngoài phiên. Theo ADR-0042, đề xuất cùng PR plan này.

**Cần người đọc trước (mục 8, câu 1):** ngày 2026-10-30, `app-project-template` và `camera-pose` đang là repo công khai. Runner của GitHub miễn phí cho repo công khai, và GitHub khuyên không dùng runner tự host cho repo công khai. Với hai repo này, T-078 không bớt được đồng nào. Plan vẫn viết đủ để dùng cho các app private sau này, và để người quyết làm tiếp, làm sau hay dừng.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu: `features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`, cách 1. Ticket `auto_run: false`: làm có người.

| Yêu cầu (phạm vi và định nghĩa hoàn thành của ticket) | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 2 | Job nặng đọc nhãn runner từ biến; xóa biến thì về runner của GitHub; job nhẹ giữ runner của GitHub | Biến cấp repo, không cấp tổ chức: gói Free không cho repo private đọc biến cấp tổ chức (mục 8, câu 4). "Job nặng" xác định theo nội dung job, không theo tên: chạy `scripts/check.sh`, `./gradlew` hay `android-emulator-runner`, và không có `secrets.` |
| Phạm vi 3 | Bước giả định máy sạch chạy được trên cả hai loại máy, hoặc bỏ qua trên máy tự host | Bước có `sudo` (giải phóng đĩa, bật KVM): `if: runner.environment == 'github-hosted'`. `setup-java`, `setup-gradle` chạy được trên cả hai loại máy, giữ nguyên |
| Phạm vi 4 | `check.sh` không để lại trạng thái giữa hai lần chạy; mỗi job có thư mục làm việc sạch | Container mới cho mỗi job (ADR-0042 mục 2). `actions/checkout` giữ `clean` mặc định (`git clean -ffdx`). `runner-check.sh` từ chối thư mục làm việc có file ngoài git. `check.sh` trong CI (`GITHUB_ACTIONS` có giá trị) không tự ghi ảnh chuẩn, nên không đổi |
| Phạm vi 5 (an toàn) | Chọn: chạy một lần, dọn `~/.gradle`, hay tách runner cho job có bí mật | Chọn chạy một lần trong container và chỉ cho job không đọc bí mật; `staging`, `device-session` giữ runner của GitHub (mục 3.1, lý do) |
| Phạm vi 6 | Dòng bảng tra lỗi: máy tắt, hết đĩa, job chờ runner quá lâu | PB-16 (chờ runner tự host), PB-17 (runner mất kết nối giữa job), PB-18 (runner không dùng được, hết đĩa) |
| DoD 1 | Job nặng chạy trên runner tự host; lịch sử tính tiền không còn phút của chúng | Việc của người sau khi đặt biến (mục 4, bước H5) |
| DoD 2 | Xóa biến thì job chạy lại trên runner của GitHub, thử một lần, ghi vào plan | Mục 4, bước H6 |
| DoD 3 | Tài liệu cài và vận hành máy, dòng bảng tra lỗi | `docs/ops/self-hosted-runner.md` mới; PB-16 tới PB-18 |
| DoD 4 | CI qua, đã merge | Như mọi task. Merge được trước khi có máy: biến chưa đặt thì mọi job vẫn chạy trên runner của GitHub |

**Giả định về T-075, T-076, T-077** (đang soạn plan ở phiên khác, chưa merge ngày 2026-10-30):

- T-075: `ui-test` rời `pr-check.yml`, test emulator chạy trong một job mới của `main-guard.yml` (tên chưa biết, plan gọi là "job emulator"). Job này có hai bước `sudo` như `ui-test` hiện nay.
- T-076: `pr-check.yml` chỉ còn job `build-and-test`, bước lọc đường dẫn nằm trong job đó.
- T-077: `pr-check.yml` khai báo `types` và bỏ qua PR nháp ở cấp job; `merge-if-green.sh` đổi cách cập nhật nhánh. T-078 không chạm phần này.

Phiên đối chiếu lại tên job và workflow khi ba plan đó merge, trước bước 3 (mục 7). Test của mục 4 tìm job nặng theo nội dung, không theo tên, nên tên mới không làm test sai. Nếu T-075 đặt job emulator ở một workflow mới thay vì `main-guard.yml`, plan phải có phiên bản 2 thêm file đó vào mục 3.2, vì `.github/` là file "Không được làm" (mục 7).

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0042 (mới, `proposed`), ADR-0009 và ADR-0017 (phát hành production ngoài GitHub, ngoài phạm vi), ADR-0010, ADR-0025, ADR-0039 (`main-guard`, giữ nguyên cách chọn `skip`, `tests`, `full`), ADR-0029 (phạm vi file).

## 3. Giải pháp

### 3.1. Cách làm

**Phân loại job hiện có** (đọc ngày 2026-10-30, trước T-075, T-076, T-077):

| Workflow / job | Nặng hay nhẹ | Bí mật | Quyền của `GITHUB_TOKEN` | Bước giả định máy sạch | Sau T-078 |
| --- | --- | --- | --- | --- | --- |
| `pr-check` / `changes` | nhẹ (bỏ ở T-076) | không | `contents: read`, `pull-requests: write` | không | không còn |
| `pr-check` / `build-and-test` | nặng (`check.sh`, R8) | không | `contents: read`, `pull-requests: write` (bình luận) | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `pr-check` / `ui-test` | nặng (emulator; chuyển sang `main-guard` ở T-075) | không | như trên | `sudo rm -rf` giải phóng đĩa, `sudo tee` udev bật KVM, `setup-java`, `setup-gradle` | `HEAVY_RUNNER` nếu còn |
| `main-guard` / `guard` | nặng (`check.sh`, trừ cách `skip`) | không | `contents: write`, `issues: write`, `checks: read`, `actions: read`, `pull-requests: read` | `setup-java`, `setup-gradle` | `HEAVY_RUNNER` |
| `main-guard` / job emulator (T-075) | nặng | không (giả định) | theo plan T-075 | như `ui-test` | `HEAVY_RUNNER` |
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
- `main-guard` / `guard` có `contents: write`, `issues: write` mà vẫn lên runner tự host: code của nó là code đã merge vào main, token chỉ sống trong job, và container của nó mới, không nhận gì từ job PR trước. Rủi ro còn lại giống trên runner của GitHub.
- Job có `secrets.` giữ `ubuntu-latest`. Test tĩnh (mục 4, test 1) chặn mọi job có `secrets.` dùng `HEAVY_RUNNER`.

**Script mới `scripts/runner-check.sh [--emulator]`.** Bash, `set -euo pipefail`, theo lối của các script khác (thông báo tiếng Việt, chú thích tiếng Anh). Chỉ được gọi trên runner tự host (bước có `if: runner.environment == 'self-hosted'`). Kiểm hết các điều sau, in một dòng `RUNNER KHÔNG DÙNG ĐƯỢC: <lý do> (docs/ops/self-hosted-runner.md).` cho mỗi điều sai, rồi thoát 1 nếu có ít nhất một dòng; đủ hết thì in `RUNNER DÙNG ĐƯỢC: ...` và thoát 0:

1. `REPO_VISIBILITY` (workflow truyền `${{ github.event.repository.visibility }}`) là `private` hay `internal`. Rỗng hay `public` thì sai (ADR-0042 mục 6).
2. `VGL_RUNNER_EPHEMERAL` là `1`. Biến này do container của máy đặt (`docs/ops/self-hosted-runner.md`); thiếu nghĩa là runner không chạy một lần.
3. Thư mục Gradle (`${GRADLE_USER_HOME:-$HOME/.gradle}`) không có `init.d/`, `init.gradle`, `init.gradle.kts`, `gradle.properties`. Bước này đứng trước `setup-gradle`, vì `setup-gradle` tự đặt init script vào đó.
4. `git status --porcelain --ignored` trong thư mục làm việc rỗng (không `build/`, không ảnh screenshot thừa).
5. Đĩa trống ở thư mục làm việc (`df -Pk .`) ít nhất `RUNNER_CHECK_MIN_FREE_GB` GB: mặc định 20, `--emulator` thì 30 (emulator cần khoảng 7,4 GB cho phân vùng dữ liệu, như chú thích của `ui-test`).
6. Có `--emulator`: `${RUNNER_CHECK_KVM:-/dev/kvm}` mở được để đọc và ghi (`[ -r ] && [ -w ]`), và `ANDROID_HOME` có giá trị.

`RUNNER_CHECK_MIN_FREE_GB` và `RUNNER_CHECK_KVM` chỉ để test đè. Script không gọi mạng, không đọc bí mật.

**Workflow** (sau T-075, T-076, T-077):

- `pr-check.yml`, job `build-and-test`: `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`. Ngay sau `actions/checkout`: bước `name: Kiểm runner tự host (scripts/runner-check.sh)`, `if: runner.environment == 'self-hosted'`, `env: REPO_VISIBILITY: ${{ github.event.repository.visibility }}`, `run: scripts/runner-check.sh`. Chú thích đầu file: một dòng dẫn ADR-0042. Nếu T-075 chưa merge và `ui-test` còn trong file: làm như job emulator dưới đây.
- `main-guard.yml`:
  - job `guard`: như `build-and-test`. Bước kiểm runner đứng sau `actions/checkout` và trước bước `mode` (`main-guard-verified.sh`): ở cách `skip` job chỉ đọc API, nhưng vẫn kiểm runner, để một runner hỏng lộ ra sớm. Thứ tự các bước khác và mọi `if:` của ADR-0039 giữ nguyên;
  - job emulator của T-075: `runs-on` như trên; bước kiểm runner gọi `scripts/runner-check.sh --emulator`; bước `Giải phóng đĩa cho emulator` và `Bật KVM cho emulator` thêm `if: runner.environment == 'github-hosted'` (nếu T-075 đã đặt `if:` khác cho các bước này thì nối bằng `&&`);
  - chú thích đầu file: một dòng dẫn ADR-0042.
- `staging.yml`, `device-session.yml`, `env-canary.yml`, `start-run.yml`, `resume-run.yml`, `run-watchdog.yml`, `status-page.yml`: không sửa. Test 1 kiểm chúng giữ `runs-on: ubuntu-latest`.

**`scripts/wait-run.sh`:** trong vòng chờ, khi lần chạy đã có mà chưa `completed`, đọc `actions/runs/<id>/jobs?per_page=50` (đã dùng ở cuối script). Có job `status == "queued"`, nhãn (`labels`) có ít nhất một nhãn không khớp `^(ubuntu|windows|macos)-`, và `created_at` cũ hơn `WAIT_RUN_RUNNER_QUEUE_MINUTES` phút (mặc định 20) thì in một dòng rồi thoát 4:

`HẠ TẦNG: job <tên> của <what>, lần chạy <id>, chờ runner tự host (nhãn <nhãn>) từ <created_at>. Không phải lỗi của code: không sửa code, không revert; người kiểm máy runner hay xóa biến HEAVY_RUNNER của repo (docs/ops/self-hosted-runner.md), phiên làm theo docs/auto-run.md, mục 4 (ADR-0042). <url>`

Dùng lại mã 4 và chữ `HẠ TẦNG` nên mọi nơi đã xử lý mã 4 (`docs/auto-run.md` bước 8 và mục 4, skill `merge-pr`, `quick-lane`) làm đúng mà không sửa. Đọc giờ hiện tại bằng `date -u +%s`; `WAIT_RUN_NOW` (giây Unix) đè cho test. Đọc job lỗi thì bỏ qua lượt kiểm này, chờ tiếp như cũ.

**Bảng tra lỗi `docs/ops/failure-playbook.md`** (mã tiếp theo lúc làm task; hiện là PB-16 tới PB-18, T-075 cũng sửa bảng này, mục 6):

- PB-16, đặt trước PB-02 (cùng chữ `HẠ TẦNG:`, dòng cụ thể trước dòng chung): mẫu `chờ runner tự host`, thấy ở `wait-run.sh` mã 4, loại `cho`. Việc phải làm: như PB-02; ghi vào "Cần người kiểm tra": máy runner tắt hay dịch vụ tạo container dừng, người bật lại hay xóa biến `HEAVY_RUNNER`, rồi hủy và chạy lại lần chạy đang chờ (`docs/ops/self-hosted-runner.md`, mục Máy hỏng).
- PB-17: mẫu `The self-hosted runner: .* lost communication with the server`, thấy ở job nặng, loại `cho`. Máy tắt hay mất mạng giữa job. Không sửa code; xử lý như PB-16.
- PB-18: mẫu `RUNNER KHÔNG DÙNG ĐƯỢC:\|No space left on device`, thấy ở bước kiểm runner hay giữa job nặng, loại `nguoi`. Máy runner thiếu đĩa hay cấu hình sai; không sửa code, không chạy lại; ghi dòng lỗi vào "Cần người kiểm tra".

Mỗi dòng có tệp `scripts/tests/playbook/PB-NN.log`. PB-16 lấy từ đầu ra thật của `wait-run.sh` mới trong test; PB-17 lấy nguyên văn thông báo của GitHub; PB-18 lấy đầu ra thật của `runner-check.sh`. Sau lần thử của người (mục 4, bước H5, H6), log thật thay cho tệp nếu khác.

**Tài liệu:**

- `docs/ops/self-hosted-runner.md` (mới), các mục:
  1. Khi nào dùng: chỉ repo private (ADR-0042 mục 6); job nào lên máy, job nào không (bảng phân loại ở trên, rút gọn).
  2. Máy: Linux x86_64 có KVM (máy chủ riêng, không phải máy ảo không bật ảo hóa lồng), đề xuất 8 nhân, 32 tới 64 GB RAM, 500 GB SSD; Ubuntu LTS; Docker; không dùng Mac Apple Silicon (emulator x86_64 của workflow cần KVM).
  3. Image của container: runner chính thức của GitHub làm nền, thêm Android SDK như `scripts/cloud-setup.sh` (gói `platform-tools`, `platforms;android-36`, `platforms;android-37.0`, `build-tools;36.0.0`, cộng gói emulator và ảnh hệ thống `system-images;android-34;google_apis;x86_64` cho job emulator), `python3`, `jq`, `git`, `curl`, `unzip`; `ENV VGL_RUNNER_EPHEMERAL=1`, `ANDROID_HOME`; người dùng không có `sudo`; không init script nào trong `~/.gradle`.
  4. Dịch vụ tạo runner: `systemd` chạy N bản (đề xuất 2) của một vòng lặp: xin cấu hình just-in-time (`POST /orgs/VGL-Studio/actions/runners/generate-jitconfig` với nhãn `vgl-android`, nhóm mặc định; hay API cấp repo khi chỉ muốn một repo), `docker run --rm --device /dev/kvm` với cấu hình đó, không mount socket Docker, giới hạn CPU và RAM mỗi container; container thoát thì vòng lặp xin cấu hình mới. Token xin cấu hình là fine-grained token chỉ có quyền "Self-hosted runners: write" của tổ chức (hay "Administration: write" của repo khi dùng API cấp repo), lưu trong tệp chỉ người dùng của dịch vụ đọc được, không bao giờ vào container.
  5. Cài đặt GitHub: nhóm mặc định không cho repo công khai dùng runner (kiểm trong Organization settings, Actions, Runner groups); tắt chạy workflow từ fork của repo private; đặt biến `HEAVY_RUNNER=vgl-android` ở từng repo app private (Settings, Secrets and variables, Actions, Variables; hay `gh variable set HEAVY_RUNNER --body vgl-android --repo VGL-Studio/<repo>`).
  6. Kiểm sau khi dựng: các bước H4 tới H6 của plan T-078.
  7. Vận hành: dọn image cũ (`docker image prune`), cập nhật image khi runner hay SDK đổi, xem đĩa; theo dõi job chờ (`wait-run.sh` mã 4, PB-16).
  8. Máy hỏng: xóa biến (`gh variable delete HEAVY_RUNNER --repo VGL-Studio/<repo>`), hủy lần chạy đang chờ rồi chạy lại (Re-run hay `gh workflow run`, hay đẩy commit mới nếu lần chạy lại vẫn giữ nhãn cũ), bật lại máy, đặt lại biến.
  9. Ngoài phạm vi: runner cho `scripts/release.sh` và bản production (ADR-0009, ADR-0017); `staging`, `device-session`.
- `docs/ops/staging-and-runs.md`, sau dòng 5 (biến của repo): một câu về biến `HEAVY_RUNNER` tùy chọn, dẫn tới `docs/ops/self-hosted-runner.md`.
- `docs/auto-run.md`, bước 8 dòng 199 (mã 4): thêm "hay job chờ runner tự host quá lâu (ADR-0042)" vào câu giải thích mã 4. Mục 4 không đổi.
- `CLAUDE.md`, mục Lệnh: một dòng "Kiểm máy runner tự host ... `scripts/runner-check.sh`, dựng máy theo `docs/ops/self-hosted-runner.md` (ADR-0042)".
- `docs/foundation.md`: sinh lại bằng `scripts/foundation.py` khi ADR-0042 đổi trạng thái.

**Bước của người, ngoài phiên** (`auto_run: false`; AI không làm, không có quyền):

- H1. Trả lời mục 8, nhất là câu 1 (repo công khai). Repo dự án mẫu và `camera-pose` công khai thì không đặt biến cho chúng.
- H2. Chọn và thuê hay bố trí máy (mục 8, câu 2, 3).
- H3. Dựng máy theo `docs/ops/self-hosted-runner.md` sau khi task merge: image, dịch vụ tạo runner, token xin cấu hình, cài đặt GitHub.
- H4 tới H6: mục 4, dòng kiểm tra thủ công.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/runner-check.sh` | Kiểm runner tự host trước khi build (mục 3.1) |
| Sửa | `.github/workflows/pr-check.yml` | `runs-on` của `build-and-test` (và `ui-test` nếu còn); bước kiểm runner; `if: runner.environment == 'github-hosted'` cho bước `sudo` nếu `ui-test` còn; một dòng chú thích |
| Sửa | `.github/workflows/main-guard.yml` | `runs-on` của `guard` và job emulator; bước kiểm runner; `if:` cho hai bước `sudo` của job emulator; một dòng chú thích |
| Sửa | `scripts/wait-run.sh` | Job chờ runner tự host quá lâu: mã 4, dòng `HẠ TẦNG` (mục 3.1); chú thích cách dùng |
| Sửa | `scripts/tests/run.sh` | Test 1 tới 9 của mục 4 |
| Sửa | `docs/ops/failure-playbook.md` | PB-16, PB-17, PB-18 |
| Tạo | `scripts/tests/playbook/PB-16.log` | Đầu ra `wait-run.sh` khi job chờ runner tự host |
| Tạo | `scripts/tests/playbook/PB-17.log` | Thông báo runner mất kết nối |
| Tạo | `scripts/tests/playbook/PB-18.log` | Đầu ra `runner-check.sh` khi hết đĩa |
| Tạo | `docs/ops/self-hosted-runner.md` | Dựng và vận hành máy runner (mục 3.1) |
| Sửa | `docs/ops/staging-and-runs.md` | Một câu về biến `HEAVY_RUNNER` |
| Sửa | `docs/auto-run.md` | Bước 8, câu về mã 4 |
| Sửa | `CLAUDE.md` | Mục Lệnh: một dòng `runner-check.sh` |
| Sửa | `docs/adr/0042-runner-tu-host-cho-job-nang.md` | Chỉ khi rà soát độc lập thấy ADR lệch với code đã làm; không đổi `status` |
| Sửa | `docs/foundation.md` | Sinh lại bằng `scripts/foundation.py` khi cần |
| Sửa | `docs/features/quy-trinh/plans/T-078-runner-tu-host.md` | Nhật ký thực hiện, kết quả bước H4 tới H6 |
| Sửa | `docs/features/quy-trinh/tickets/T-078-runner-tu-host.md` | Trạng thái ticket |

Nơi đọc file bị đổi (L-020), tìm bằng `grep -rn 'pr-check.yml\|main-guard.yml\|wait-run.sh\|failure-playbook' scripts/ .github/ .claude/skills/ docs/ops/`:

- `scripts/main-guard-verified.sh` đọc tên job `build-and-test` và tên bước `Cổng chất lượng (scripts/check.sh)` của `pr-check.yml`; `GATE_FILES` có `pr-check.yml`, `main-guard.yml`: plan không đổi tên job hay bước. Commit merge của task đổi `main-guard.yml` nên `main-guard` chạy `full`: đúng ý (lần đầu cổng mới chạy trên main). Không sửa.
- `scripts/merge-if-green.sh` đọc tên check run `build-and-test` (và `changes` trước T-076): không đổi tên. Không sửa (file "Không được làm").
- `scripts/find-breaking-commit.sh`, `scripts/main-guard-status.sh` đọc check run `guard`: không đổi tên, không thêm `if:` cấp job. Không sửa.
- `scripts/failure-kind.sh --playbook` đọc bảng tra lỗi theo thứ tự dòng: PB-16 đặt trước PB-02 có chủ ý; test 8 kiểm.
- `scripts/main-guard-status.sh` dòng 9, 56, 101, `scripts/merge-if-green.sh` dòng 326, `.claude/skills/merge-pr/SKILL.md`, `.claude/skills/quick-lane/SKILL.md`, `docs/CLAUDE.md`, `docs/templates/run.md`: chỉ in hay nhắc lệnh `wait-run.sh`; mã 4 giữ nghĩa "không phải lỗi của code". Không sửa.
- `scripts/check.sh` dòng 4, 5 và `scripts/failure-kind.sh`: chỉ chú thích nhắc tên workflow. Không sửa.
- `scripts/tests/playbook/PB-02.log` là log mẫu có tên `pr-check.yml`: không đổi.
- `docs/ops/release.md`, `docs/ops/setup-new-app.md`, `docs/ops/pre-run-review.md`, `docs/ops/test-sessions.md`, `docs/ops/token-usage.md`, `docs/ops/quick-lane.md`, `docs/ops/session-capabilities.md`: nhắc `pr-check`, `main-guard` hay `wait-run.sh` mà không nói máy chạy. Không sửa.
- `.github/workflows/status-page.yml`, `.github/workflows/start-run.yml`, `.github/workflows/device-session.yml`, `.github/workflows/staging.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/run-watchdog.yml`, `.github/workflows/env-canary.yml`: không sửa; test 1 đọc chúng.
- `scripts/template-sync.sh` chép workflow và script của dự án mẫu về app: app nhận thay đổi ở đợt đồng bộ sau; biến đặt riêng ở từng repo app.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của phần bị đổi, và plan không đổi tên, xóa hay đánh số lại mục nào (chỉ thêm một câu hay một dòng):

- dẫn `docs/auto-run.md`: `scripts/cloud-env-check.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py`, `scripts/run-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/token-usage.py`. Plan chỉ thêm vài chữ vào câu về mã 4 ở bước 8;
- dẫn `docs/ops/staging-and-runs.md`: `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`, `scripts/run-watchdog.py`, `scripts/setup-check.sh`. Plan chỉ thêm một câu sau dòng 5, số mục giữ nguyên;
- dẫn `docs/ops/failure-playbook.md`: `scripts/push-probe.sh` (dẫn PB-14). Plan thêm ba dòng, không đổi mã dòng cũ;
- dẫn `CLAUDE.md`: `.github/ISSUE_TEMPLATE/from-app.yml`, `scripts/new-app.sh` (thay tên app trong `CLAUDE.md`: dòng mới không có tên app), `scripts/local-setup.sh`, `scripts/run-watchdog.py`, `app/src/androidTest/kotlin/com/vglstudio/template/AppLaunchTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeFullScreenOverlayScreenshotTest.kt` (chú thích dẫn quy ước). Plan chỉ thêm một dòng vào mục Lệnh;
- `scripts/local-setup.sh` nhắc `scripts/tests/run.sh`: không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của `GITHUB_TOKEN` trong mọi workflow giữ nguyên; không thêm bí mật. Biến mới `HEAVY_RUNNER` (cấp repo, không bí mật) do người đặt.
- **Thư viện và SDK:** Không thêm. Action có sẵn: `actions/checkout@v5`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4`, `reactivecircus/android-emulator-runner@v2`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `actions/setup-java@v5`, `gradle/actions/setup-gradle@v4` và `android-emulator-runner@v2` trong container không có `sudo`. Hai action đầu cài vào thư mục người dùng (`RUNNER_TOOL_CACHE`), action thứ ba dùng `ANDROID_HOME` có sẵn; chưa chạy thử được vì chưa có máy. Phần này chỉ lộ ra ở bước H5 của người, sau khi task đã merge.
- **Điều kiện chuyển:** ở bước H5, job nặng đỏ trên runner tự host ở một bước cài đặt, trong khi cùng commit xanh trên runner của GitHub.
- **Cách làm thay:** sửa image của container theo `docs/ops/self-hosted-runner.md` (cài sẵn JDK 17, đặt `RUNNER_TOOL_CACHE` ghi được, cài gói emulator), không sửa workflow. Không sửa được bằng image thì xóa biến (mọi job về runner của GitHub) và mở ticket mới. Tiêu chí và test của mục 4 giữ nguyên.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin. Với runner của GitHub (biến chưa đặt), mọi bước của workflow chạy như cũ: bước kiểm runner bị bỏ qua, bước `sudo` vẫn chạy (`runner.environment == 'github-hosted'`). Test có sẵn của `pr-check.yml`, `main-guard.yml` (bước, thứ tự, `if:` của ADR-0039) và của `wait-run.sh` (mã 0, 1, 2, 4, 5) giữ nguyên câu khẳng định. Test có sẵn `wait-run: a run still going when time runs out asks to call again` dùng fixture không có trường `labels`: giữ "2 CHƯA" (test 7, ca đối chứng).

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, viết trước code (bước 3), theo lối có sẵn: `expect "<tên>" "<mong đợi>" "<thực tế>"`; `wf_job` (dòng khoảng 1369) in các dòng của một job; `gh` giả `scripts/tests/fake-gh` với `FIXTURE`. Hàm phụ trợ mới `heavy_jobs` và `job_kind` vào commit test (L-025):

- `job_kind <workflow> <job>` in `secret` khi job có `secrets.`, `heavy` khi job có `scripts/check.sh`, `./gradlew` hay `android-emulator-runner` mà không có `secrets.`, `light` khi khác.
- `heavy_jobs` in mọi cặp `<workflow> <job>` loại `heavy` trong `.github/workflows/*.yml`, mỗi cặp một dòng. Liệt kê job bằng awk trên khối `jobs:` (dòng hai dấu cách thụt lề, kết thúc bằng `:`).

Các test chạy cả trên bản sao hỏng của workflow trong `$work` (như test 13 của T-074) để chứng minh test bắt được lỗi.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Phạm vi 2: job nặng không bí mật đọc biến; job có bí mật và job nhẹ giữ runner của GitHub | Test cấu trúc | 1. `runners: heavy jobs read HEAVY_RUNNER, every other job stays on ubuntu-latest`: với mọi job của mọi workflow, `heavy` thì đúng một dòng `    runs-on: ${{ vars.HEAVY_RUNNER \|\| 'ubuntu-latest' }}`, `secret` và `light` thì đúng một dòng `    runs-on: ubuntu-latest`. Mong `yes`. Bản sao hỏng: (a) `staging.yml` dùng `HEAVY_RUNNER`; (b) `guard` giữ `ubuntu-latest`; mong `no` cả hai |
| Phạm vi 2: tập job nặng đúng | Test cấu trúc | 2. `runners: the heavy jobs are build-and-test and guard (and the emulator job)`: `heavy_jobs` có `pr-check.yml build-and-test`, `main-guard.yml guard`, và một job của `main-guard.yml` có `android-emulator-runner` (sau T-075); không có job nào của `staging.yml`, `device-session.yml`. Đối chứng L-055: test 1 qua trên một bộ workflow không có job nặng nào; test 2 thất bại khi đó |
| ADR-0042 mục 3: job nặng kiểm runner trước khi cài | Test cấu trúc | 3. `runners: every heavy job checks the runner right after checkout`: trong mỗi job nặng, bước kế ngay sau `uses: actions/checkout@v5` có `if: runner.environment == 'self-hosted'`, `REPO_VISIBILITY: ${{ github.event.repository.visibility }}` và `run: scripts/runner-check.sh` (có `--emulator` khi job có `android-emulator-runner`, không có khi không); bước đó đứng trước `setup-java`, `setup-gradle` và bước `mode`. Bản sao hỏng: bước kiểm đứng sau `setup-gradle`; thiếu `--emulator` ở job emulator; `if:` thiếu; mong thất bại mỗi bản |
| Phạm vi 3: bước `sudo` chỉ trên máy của GitHub | Test cấu trúc | 4. `runners: sudo steps of heavy jobs run only on GitHub runners`: mọi bước có `sudo ` trong job nặng có dòng `if:` chứa `runner.environment == 'github-hosted'`. Bản sao hỏng bỏ `if:` của bước `Bật KVM cho emulator`: mong thất bại. Đối chứng: `device-session.yml` (job `secret`) giữ bước `sudo` không có `if:` và không làm test thất bại |
| Phạm vi 4: thư mục làm việc sạch | Test cấu trúc | 5. `runners: heavy jobs keep the default clean checkout`: không job nặng nào có `clean: false`. Bản sao hỏng có `clean: false`: mong thất bại |
| `runner-check.sh` từ chối runner không an toàn | Test của script | 6. `runner-check: ...`, trong một repo git tạm `"$work/rc"` (một commit, `.gitignore` có `build/`), `HOME` tạm, `GRADLE_USER_HOME` không đặt, `VGL_RUNNER_EPHEMERAL=1`, `REPO_VISIBILITY=private`, `RUNNER_CHECK_MIN_FREE_GB=0`. Hàm phụ trợ `rc [args]` in `<mã thoát> <số dòng RUNNER KHÔNG DÙNG ĐƯỢC>`. Ca: (a) đủ điều kiện → `0 0`, có dòng `RUNNER DÙNG ĐƯỢC`; (b) `REPO_VISIBILITY` là `public`, rồi rỗng → `1 1` mỗi lần; `internal` → `0 0`; (c) `VGL_RUNNER_EPHEMERAL` không đặt → `1 1`; (d) lần lượt tạo `$HOME/.gradle/init.d/x.gradle`, `init.gradle`, `init.gradle.kts`, `gradle.properties` → `1 1` mỗi lần; cùng ca với `GRADLE_USER_HOME="$work/gh"` có `init.d/` → `1 1`, và `$HOME/.gradle/init.d` có mà `GRADLE_USER_HOME` trỏ thư mục sạch → `0 0`; `$HOME/.gradle/caches/` có (thư mục khác) → `0 0`; (e) tạo `build/x` (bị bỏ qua), rồi `core/x/src/test/screenshots/a.png` chưa track → `1 1` mỗi lần; (f) `RUNNER_CHECK_MIN_FREE_GB=999999` → `1 1`; (g) `--emulator` với `RUNNER_CHECK_KVM="$work/nokvm"` (không có) → `1 1`; với một tệp tạm đọc ghi được và `ANDROID_HOME` đặt → `0 0`; không `--emulator` mà `RUNNER_CHECK_KVM` không có → `0 0` (đối chứng); `--emulator`, KVM được, `ANDROID_HOME` rỗng → `1 1`; (h) ba lỗi cùng lúc (public, không ephemeral, `init.d`) → `1 3`: script kiểm hết, không dừng ở lỗi đầu. Tham số lạ → mã 2 |
| ADR-0042 mục 5: job chờ runner tự host | Test của script | 7. `wait-run: a job waiting for a self-hosted runner too long is infrastructure`, với `WAIT_RUN_NOW` cố định và `pending` có sẵn: jobs `[{"id": 120, "name": "guard", "status": "queued", "labels": ["vgl-android"], "created_at": <WAIT_RUN_NOW trừ 30 phút>}]` → `4 HẠ`, dòng có `chờ runner tự host` và `vgl-android`. Đối chứng: cùng job nhưng `created_at` trừ 5 phút → `2 CHƯA`; nhãn `["ubuntu-latest"]` trừ 30 phút → `2 CHƯA`; job `in_progress` nhãn `vgl-android` → `2 CHƯA`; `WAIT_RUN_RUNNER_QUEUE_MINUTES=60` với job trừ 30 phút → `2 CHƯA`; câu trả lời jobs lỗi (`errors` của `fake-gh`) → `2 CHƯA`. Ca `--workflow main-guard.yml --since` cùng dữ liệu → `4 HẠ` |
| Bảng tra lỗi | Test của script | 8. `playbook: PB-16 waiting for a self-hosted runner -> cho` (`PB-16 cho`, không phải `PB-02`), `playbook: PB-17 runner lost communication -> cho`, `playbook: PB-18 runner not usable -> nguoi`; thêm một tệp tạm chỉ có `No space left on device` → `PB-18 nguoi`. Test có sẵn "mỗi dòng có tệp log" kiểm ba tệp mới |
| Tệp PB-16 khớp đầu ra thật | Test của script | 9. `playbook: PB-16.log is what wait-run.sh prints`: dòng đầu của `scripts/tests/playbook/PB-16.log` bằng dòng mà test 7 nhận được, bỏ phần đường dẫn lần chạy |
| DoD 4: không đổi gì khi chưa có biến | Test cấu trúc | Test 1 tới 5 kiểm `runs-on` có `\|\| 'ubuntu-latest'`, bước kiểm runner bỏ qua trên máy GitHub, bước `sudo` chạy trên máy GitHub. CI của PR task chạy trên runner của GitHub với chính workflow mới (pr-check và main-guard xanh là bằng chứng) |
| Tài liệu đúng và đủ (DoD 3) | Kiểm tra thủ công (người: đọc tài liệu trước khi dựng máy) | Người đọc `docs/ops/self-hosted-runner.md`, dựng theo đúng tài liệu ở bước H3; chỗ thiếu ghi vào nhật ký và sửa tài liệu trong PR nhỏ |
| H4: runner nhận job | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | Sau H3, đặt `gh variable set HEAVY_RUNNER --body vgl-android --repo VGL-Studio/<repo>`; mở một PR code nhỏ (ví dụ một dòng chú thích trong `scripts/`); `gh api "repos/VGL-Studio/<repo>/actions/runs/<id>/jobs" --jq '.jobs[] \| {name, labels, runner_name}'`: `build-and-test` có `labels` `["vgl-android"]`, `runner_name` là tên container; log có `RUNNER DÙNG ĐƯỢC`; job xanh. Merge PR đó: `guard` cùng kiểm như vậy |
| H5: job emulator và cách ly giữa job (DoD 1) | Kiểm tra thủ công (người: máy runner chưa có trước khi duyệt) | Một commit chạm `app/` trên main: job emulator của `main-guard` chạy trên `vgl-android` và xanh. Thử cách ly: trên một nhánh nháp, thêm tạm một bước ghi `~/.gradle/init.d/probe.gradle` vào job `build-and-test` (PR không merge), rồi chạy một job nặng khác: bước kiểm runner của job sau vẫn `RUNNER DÙNG ĐƯỢC` (container mới). Sau vài ngày: `gh api "/organizations/VGL-Studio/settings/billing/usage?year=<năm>&month=<tháng>"`, phút `Actions Linux` của repo theo ngày giảm tương ứng với các job đã lên máy |
| H6: xóa biến thì quay về runner của GitHub (DoD 2), thử một lần | Kiểm tra thủ công (người: chỉ thấy trên GitHub thật) | (1) `gh variable delete HEAVY_RUNNER --repo VGL-Studio/<repo>`; (2) `gh workflow run main-guard.yml --repo VGL-Studio/<repo> --ref main`; (3) `scripts/wait-run.sh --workflow main-guard.yml --since <giờ vừa chạy>` xanh, và lệnh `gh api .../jobs` như H4 cho `labels` `["ubuntu-latest"]`, `runner_name` bắt đầu bằng `GitHub Actions`; log không có bước kiểm runner (bị bỏ qua), có bước `Bật KVM` của job emulator nếu commit có; (4) thử máy tắt: đặt lại biến, dừng dịch vụ tạo runner, đẩy một PR code: `wait-run.sh --pr <số>` trả mã 4, dòng có `chờ runner tự host` sau khoảng 20 phút; xóa biến, bấm Re-run: ghi lại lần chạy lại nhận nhãn mới hay giữ nhãn cũ (chưa xác minh, mục 8 câu 6); (5) bật dịch vụ, đặt lại biến. Ghi lệnh, mã lần chạy và kết quả vào nhật ký |

**Ngoại lệ viết test trước:** tài liệu (`docs/ops/self-hosted-runner.md`, `docs/ops/staging-and-runs.md`, `docs/auto-run.md`, `CLAUDE.md`, `foundation.md`), dòng chú thích đầu hai workflow, và các bước H4 tới H6 cần máy thật. Không có hành vi để kiểm trong phiên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, trên bản sao của `pr-check.yml` và `main-guard.yml` hiện tại (commit `d7d5bc6`) trong thư mục tạm, không commit: `build-and-test` và `guard` đổi sang `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`, thêm bước `if: runner.environment == 'self-hosted'` gọi `scripts/runner-check.sh`, bước `Giải phóng đĩa cho emulator` thêm `if: runner.environment == 'github-hosted'`.
- **Lệnh:** `actionlint -shellcheck= <hai bản sao>` (actionlint 1.7.12). Đối chứng: đổi `vars.` thành `secrets.` trong `runs-on`, và viết sai `runner.enviroment`. Chưa chạy `scripts/check.sh` trên runner tự host: chưa có máy; phần này là bước H4, H5 của người, sau khi task merge. Trong phiên, `scripts/check.sh` chạy như mọi task (bước 5) và không đổi hành vi trên máy dev.
- **Kết quả:** bản đúng: actionlint mã 0. Đối chứng: mã 1, `context "secrets" is not allowed here. available contexts are "github", "inputs", "matrix", "needs", "strategy", "vars"` và `property "enviroment" is not defined in object type {arch; debug; environment; name; os; temp; tool_cache}`. Vậy `vars` dùng được trong `runs-on` và `runner.environment` là thuộc tính có thật (khớp tài liệu GitHub, mục 8 nguồn 5). Phần chưa thử được (action trong container không `sudo`) có phương án ở mục 3.4.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI: `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/wait-run.sh`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai workflow, một script mới, một script sửa, test, bảng tra lỗi, bốn tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 3 | Ticket rõ, nhưng repo dự án mẫu và `camera-pose` đã công khai (mục 8, câu 1), làm lợi ích của chính ticket không còn cho hai repo này; máy và ngân sách chưa chọn; ba plan T-075 tới T-077 chưa merge |
| C. Khả năng kiểm chứng tự động | 2 | Cấu trúc workflow và hai script có test; chạy thật trên runner tự host chỉ kiểm được bằng tay sau khi có máy |
| D. Ảnh hưởng chéo | 3 | Mọi PR và mọi commit của main, ở mọi app sau đồng bộ; máy hỏng mà biến còn thì mọi job nặng chờ |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng runner tự host, `runner.environment`, `vars` trong `runs-on`, runner just-in-time; đã chạy thử phần cú pháp (mục 4.1) |
| **Tổng** | 12 | **Mức: Cao (điểm chặn 7, tổng từ 9)** |
## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `pr-check.yml`, `main-guard.yml` (mọi PR và mọi commit của main), `wait-run.sh` (mọi phiên chờ CI), `failure-playbook.md`.
- Task khác có thể xung đột:
  - T-075 sửa `main-guard.yml` (job emulator mới), `pr-check.yml` (bỏ `ui-test`), `docs/auto-run.md`, và viết lại PB-01, PB-10, PB-11, PB-13, PB-15 (có thể thêm dòng PB mới: mã của T-078 lấy tiếp theo lúc làm).
  - T-076 đổi cấu trúc `pr-check.yml` (một job).
  - T-077 sửa `pr-check.yml` (`types`, bỏ qua PR nháp ở cấp job) và `merge-if-green.sh`.
  - Cả ba làm trước T-078 (`depends_on: [T-076, T-077]`; T-076 phụ thuộc T-075). ADR-0040 (T-075) và ADR-0041 (T-077) có thể thêm vế vào `foundation.md`: sinh lại sau khi merge.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): workflow và script của dự án mẫu; biến `HEAVY_RUNNER` đặt riêng ở từng repo, không đồng bộ.
- Đề xuất thứ tự: làm có người, sau T-075, T-076, T-077; merge được trước khi có máy (biến chưa đặt). Bước H3 tới H6 sau khi merge.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu `pr-check.yml`, `main-guard.yml` trên main với giả định ở mục 2. T-075 đặt job emulator ở workflow khác `main-guard.yml`, hay T-076, T-077 đổi tên job `build-and-test`, `guard`: dừng, soạn plan phiên bản 2 (file `.github/` mới ngoài mục 3.2 là file "Không được làm").
- Cần thêm bí mật, quyền ghi mới, hay đưa một job có `secrets.` lên `HEAVY_RUNNER`: dừng.
- Cần sửa `scripts/merge-if-green.sh`, `scripts/check.sh` hay `scripts/release.sh`: dừng.
- Không giữ được hành vi cũ trên runner của GitHub khi biến chưa đặt (một bước mới chạy trên máy của GitHub, hay bước `sudo` bị bỏ qua ở đó): dừng.

## 8. Câu hỏi cho người duyệt

1. **Repo công khai.** Ngày 2026-10-30, `gh repo list VGL-Studio` cho thấy `app-project-template`, `camera-pose` (cùng `terms`, `dalita-studio`) là `PUBLIC`. Bảng tính tiền của tổ chức (`/organizations/VGL-Studio/settings/billing/usage`, tháng 10): ngày 2026-10-23 tới 2026-10-29 mỗi ngày tính 2 tới 3,6 USD cho dự án mẫu; ngày 2026-10-30 dự án mẫu 136 phút và `camera-pose` 34 phút đều 0 USD. Runner chuẩn của GitHub miễn phí cho repo công khai (nguồn 2). Vậy có vẻ hai repo vừa được chuyển sang công khai và phút của chúng không còn tính tiền. Điều này mâu thuẫn với yêu cầu gốc ("repo private") và với lợi ích của T-078. Mặt khác, GitHub khuyên không dùng runner tự host cho repo công khai (nguồn 6): ai cũng mở được PR từ fork. Đề xuất:
   - Nếu công khai là quyết định lâu dài: không đặt `HEAVY_RUNNER` cho hai repo này (ADR-0042 mục 6, `runner-check.sh` cũng từ chối). Hoãn T-078 tới khi có app private tốn phút, hay dừng ticket. Cũng nên xem lại T-075 tới T-077, vì phút của repo công khai không còn tính tiền.
   - Nếu công khai là tạm thời hay nhầm: làm theo plan này.
   - Thêm: repo công khai cho người lạ đọc toàn bộ quy trình, tài liệu, bài học. `scripts/secret-scan.py` vẫn chặn bí mật, nhưng nên xác nhận đây là điều mong muốn.
2. **Máy nào chạy runner** (ticket, câu 1). Đề xuất: máy chủ riêng thuê có KVM, Linux x86_64, khoảng 8 nhân, 64 GB RAM, 500 GB SSD (ticket ước khoảng 40 tới 60 USD mỗi tháng; chưa kiểm giá). Lý do: emulator x86_64 của workflow cần KVM; máy ảo thường không bật ảo hóa lồng; máy văn phòng phụ thuộc điện và mạng. Mac Apple Silicon không chạy được emulator x86_64 trong container, nên loại. Nếu văn phòng có máy Linux x86 bật 24/7 thì dùng máy đó trước. So sánh: phần còn lại sau T-074 tới T-077 ước khoảng 100 tới 150 USD mỗi tháng cho hai repo (yêu cầu gốc, trừ phần cách 2 tới 5 bớt); với repo công khai thì là 0 (câu 1).
3. **Một máy hay mỗi app một máy** (ticket, câu 2). Đề xuất: một máy cho cả tổ chức, 2 container chạy cùng lúc (một job emulator tốn khoảng 4 nhân, 8 GB RAM), runner cấp tổ chức trong nhóm mặc định, chỉ repo private dùng. Biến `HEAVY_RUNNER` đặt riêng từng repo app (câu 4). Muốn giới hạn chắc chắn theo repo trên gói Free thì đăng ký runner cấp repo: mỗi repo một dịch vụ tạo container trên cùng máy, token xin cấu hình cấp repo.
4. **Kết quả tra cứu (ticket, câu 3 và 4)**, tài liệu GitHub đọc ngày 2026-10-30:
   - Câu 3, giá: runner tự host không tính phút ("Self-hosted runners" nằm trong danh sách "remains free", nguồn 2). Phí nền tảng 0,002 USD mỗi phút công bố 2025-12-16, dự kiến từ 2026-03-01, đã bị hoãn: "We're postponing the announced billing change for self-hosted GitHub Actions to take time to re-evaluate our approach", không có ngày mới (nguồn 1). Hoãn không phải hủy: GitHub có thể thu lại; khi đó 1.000 phút chỉ còn 2 USD, vẫn rẻ hơn runner của GitHub (khoảng 6 USD).
   - Câu 4, nhóm runner: "Organization owners using the GitHub Team plan can create additional organization-level runner groups" (nguồn 3). Gói Free chỉ có nhóm mặc định. "By default, only private repositories can access runners in a runner group" (nguồn 3); tài liệu không nói rõ gói Free đổi được quyền theo repo của nhóm mặc định hay không: **chưa xác minh**, người kiểm trong Organization settings, Actions, Runner groups.
   - Phát hiện thêm, đổi cách làm của ticket: "Organization-level secrets and variables are not accessible by private repositories for GitHub Free" (nguồn 4). Biến chọn runner vì vậy là biến cấp repo, đặt ở từng repo, không phải biến tổ chức như ticket viết. Đồng ý?
5. **Phương án an toàn.** Đề xuất chạy một lần trong container, chỉ cho job không đọc bí mật; `staging` và `device-session` (khoảng 13% số phút) giữ runner của GitHub (mục 3.1). Đồng ý, hay muốn tách runner riêng cho hai job đó (ticket mới)?
6. **Chạy lại sau khi xóa biến.** Chưa xác minh: bấm Re-run một lần chạy đã tạo khi biến còn có tính lại `runs-on` theo biến mới không. Bước H6 thử và ghi lại; tài liệu vận hành viết cả cách thay (`gh workflow run`, commit mới).
7. **ADR-0042.** Đồng ý thì khi duyệt plan ghi `status: accepted` và chạy `scripts/foundation.py`.

Nguồn (đọc ngày 2026-10-30):

1. GitHub Changelog, "Update to GitHub Actions pricing", 2025-12-16: https://github.blog/changelog/2025-12-16-coming-soon-simpler-pricing-and-a-better-experience-for-github-actions/
2. GitHub Docs, "GitHub Actions billing": https://docs.github.com/en/billing/concepts/product-billing/github-actions
3. GitHub Docs, "Managing access to self-hosted runners using groups": https://docs.github.com/en/actions/how-tos/manage-runners/self-hosted-runners/manage-access và "Runner groups": https://docs.github.com/en/actions/concepts/runners/runner-groups
4. GitHub Docs, "Store information in variables": https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-variables
5. GitHub Docs, "Contexts reference" (`runner.environment`; `vars` dùng được trong `jobs.<job_id>.runs-on`): https://docs.github.com/en/actions/reference/workflows-and-actions/contexts
6. GitHub Docs, "Secure use reference" (runner tự host, runner just-in-time): https://docs.github.com/en/actions/reference/security/secure-use

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
