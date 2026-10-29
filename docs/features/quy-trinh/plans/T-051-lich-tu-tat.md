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

`resume-run.yml` và `run-watchdog.yml` tự tắt mình (REST `disable`) khi không có phiên `running` và không có dấu hiệu một phiên sắp bắt đầu. Đầu mỗi phiên, phiên bật lại cả hai (REST `enable`); `start-run.yml` cũng bật lại trước khi kích hoạt routine. Phần quyết định nằm trong một script mới, `scripts/schedule-idle.py`, kiểm bằng test của `scripts/tests/run.sh` với `gh` giả. Lúc rảnh, mỗi app chỉ còn một lần chạy của mỗi lịch sau khi phiên xong, thay vì khoảng 2.160 phút mỗi tháng (L-126).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là bài học L-126 (issue #275) và quyết định của anh Nhật ngày 2026-10-29: phương án A (tự tắt, phiên bật lại), không thêm bí mật, làm trong phiên có người.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | Khi không có phiên `running` thì hai lịch không cấp runner | Lần chạy đầu tiên thấy rảnh thì tắt workflow của chính nó. Từ đó GitHub không chạy lịch nữa, tới khi có người hay phiên bật lại |
| Định nghĩa hoàn thành 2 | Khi có phiên `running` thì hai lịch làm như cũ | Phiên `running` thì không tắt. Mọi cách mở phiên đều bật lại được: `start-run.yml` bật trước khi kích hoạt; phiên bật ở đầu phiên (Run now, hẹn giờ, `claude --cloud`, `local`) |
| Định nghĩa hoàn thành 3 | Tài liệu ghi cách làm mới | `docs/ops/staging-and-runs.md` mục 2.8 thay phần tắt bật tay; `docs/auto-run.md` mục 2 thay dòng kiểm bằng dòng bật |

"Dấu hiệu một phiên sắp bắt đầu" (khe tranh chấp): `start-run.yml` bật lịch rồi kích hoạt routine, nhưng phiên cần vài phút mới đẩy nhánh báo cáo `running`. Lịch chạy trong khoảng đó không được tự tắt. Hai dấu hiệu, đọc được bằng REST mà không cần bí mật:

- `updated_at` của workflow mới trong `GRACE_HOURS` (mặc định 3): lệnh `enable` cập nhật mốc này, kể cả khi workflow đang bật (đã thử 2026-10-29, mục 4.1).
- Lần chạy gần nhất của `start-run.yml` mới trong `GRACE_HOURS`.

Còn một khe nhỏ: lịch đọc "không có phiên", rồi phiên đẩy `running` và bật lịch, rồi lịch mới gọi `disable`. Script đọc lại sau khi tắt; thấy phiên `running` thì bật lại ngay.

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0025 (nhánh chính đỏ, `main-guard`), ADR-0032 (lệch nhỏ, PB-14 về quyền `workflow`). Không đổi quy tắc của ADR nào.

## 3. Giải pháp

### 3.1. Cách làm

**Script `scripts/schedule-idle.py`** (Python, chỉ `gh api` REST, cùng kiểu với `scripts/resume-check.py`: hàm `api`, `api_all`, `front_matter`, `moment`, đọc `GITHUB_REPOSITORY` hay remote `origin`):

- `--disable-if-idle <tệp workflow>`, ví dụ `resume-run.yml`:
  1. Phiên `running`: có nhánh `claude/RUN-YYYYMMDD-NN-report` mà `docs/runs/<RUN>.md` trên nhánh đó có `status: running` (cùng cách `resume-check.py` và `merge-if-green.sh`). Có thì in "giữ: có phiên ..." và thoát 0.
  2. `updated_at` của workflow (`actions/workflows/<tệp>`) mới hơn `GRACE_HOURS`: in "giữ: vừa được bật lúc ..." và thoát 0.
  3. Lần chạy gần nhất của `start-run.yml` (`actions/workflows/start-run.yml/runs?per_page=1`, `created_at`) mới hơn `GRACE_HOURS`: in "giữ: start-run vừa chạy" và thoát 0.
  4. Không thì `PUT actions/workflows/<tệp>/disable`, rồi đọc lại bước 1. Có phiên `running` thì `PUT .../enable` và in "bật lại: phiên vừa bắt đầu". Không thì in "đã tắt: không có phiên".
  - Không đọc được GitHub ở bước 1 tới 3: không tắt, in lý do, thoát 0, vì giữ lịch bật chỉ tốn phút như hiện nay. Gọi `disable` lỗi: in CHÚ Ý, thoát 0 (workflow không đỏ).
  - `SCHEDULE_IDLE_NOW` (giờ ISO, cho test) và `SCHEDULE_IDLE_GRACE_HOURS` thay mặc định.
- `--enable`: `PUT actions/workflows/<tệp>/enable` cho `resume-run.yml` và `run-watchdog.yml`, rồi đọc `state` của từng cái và in một dòng mỗi workflow. Thoát 0 khi cả hai `active`, 1 khi có cái không bật được (thông báo nêu tên và lỗi), để phiên ghi lại.

**Workflow:**

- `resume-run.yml`: thêm bước cuối "Tắt lịch khi không có phiên" chạy `python3 -I scripts/schedule-idle.py --disable-if-idle resume-run.yml`, có `if: always()` để chạy cả khi bước kích hoạt lỗi. `permissions` thêm `actions: write`. Sửa chú thích đầu file.
- `run-watchdog.yml`: thêm bước cuối như trên với `run-watchdog.yml`, sau bước của `run-watchdog.py`, để issue `session-stalled` của danh sách không còn `running` được đóng trước khi tắt. `permissions` đổi `actions: read` thành `actions: write`. Sửa chú thích đầu file (bỏ "about 48 a day", thêm cách tự tắt).
- `start-run.yml`: job `fire` thêm `permissions: actions: write` và bước "Bật lịch của phiên" trước bước kích hoạt: hai lệnh `gh api -X PUT "repos/${GITHUB_REPOSITORY}/actions/workflows/<tệp>/enable"`, lỗi thì `::warning::` và đi tiếp (không chặn phiên). Job này không checkout, nên không gọi script.

**Phiên:** `docs/auto-run.md` mục 2, dòng "`resume-run.yml` và `run-watchdog.yml` đang bật" (thêm ở #283) đổi thành: sau khi đẩy nhánh báo cáo `running`, chạy `scripts/schedule-idle.py --enable`. Mã 0: ghi vào Kiểm tra đầu phiên. Mã 1: ghi vào Kiểm tra đầu phiên và một dòng `- [ ]` ở Cần người kiểm tra (phiên không tự tiếp tục khi hết hạn mức, không ai báo phiên đứng im), làm tiếp. Phiên tiếp tục (mục 4) cũng chạy lại. Lệnh chạy sau khi có nhánh báo cáo, để lần đọc lại ở bước 4 của script thấy `running`.

**Tài liệu:** `docs/ops/staging-and-runs.md` mục 2.8 thay phần tắt bật tay bằng cách làm mới (bảng phút giữ, thêm cột "Sau T-051"), mục 2.3 bỏ dòng bật tay, mục 2.6 sửa câu "Disable workflow" (lịch tự tắt, người vẫn tắt tay được; muốn dừng hẳn thì tắt rồi không mở phiên). `docs/templates/run.md` dòng kiểm trước khi duyệt về lịch bỏ đi (phiên tự bật). `docs/ops/session-capabilities.md` thêm dòng `workflow-enable` (cloud `thử`, routine `thử`, local `có`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/schedule-idle.py` | Script ở mục 3.1 |
| Sửa | `.github/workflows/resume-run.yml` | Bước tự tắt, `actions: write`, chú thích |
| Sửa | `.github/workflows/run-watchdog.yml` | Bước tự tắt, `actions: write`, chú thích |
| Sửa | `.github/workflows/start-run.yml` | Job `fire`: `actions: write`, bước bật hai lịch |
| Sửa | `scripts/tests/run.sh` | Test ở mục 4 |
| Sửa | `docs/auto-run.md` | Mục 2 dòng bật lịch; mục 4 (Tiếp tục phiên, Phiên đứng im) nhắc lịch tự tắt |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 2.3, 2.6, 2.8 |
| Sửa | `docs/ops/session-capabilities.md` | Dòng `workflow-enable` |
| Sửa | `docs/templates/run.md` | Bỏ dòng kiểm lịch đang bật |
| Sửa | `docs/features/quy-trinh/tickets/T-051-lich-dinh-ky-tiet-kiem-phut.md` | `plan:`, trạng thái theo plan |
| Sửa | `docs/lessons.md` | L-126 sang `applied` sau khi merge |

`grep -rn 'resume-run\|run-watchdog' scripts/ .github/ docs/ .claude/` đã chạy: ngoài các file trên, `scripts/resume-check.py` và `scripts/run-watchdog.py` chỉ nhắc tên trong chú thích, không đổi; `.claude/skills/whatnow/SKILL.md` không nhắc.

Các file khác nhắc tới file plan sửa, không bị ảnh hưởng vì chỉ dẫn tên hay đường dẫn trong chú thích, thông báo hay liên kết, không đọc nội dung của mục bị đổi, và plan không đổi tên hay xóa mục nào:
- dẫn `docs/auto-run.md` hay `docs/ops/staging-and-runs.md`: `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/pr-check.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/env-canary.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/save-branch.sh`, `scripts/setup-check.sh`, `scripts/token-usage.py`, `scripts/wait-run.sh`;
- dẫn `.github/workflows/start-run.yml`: `.github/workflows/env-canary.yml`, `scripts/env-canary.sh`, `scripts/main-guard-status.sh`, `scripts/setup-check.sh` (không đọc job `fire`);
- `scripts/run-check.py` đọc bảng khả năng của `docs/ops/session-capabilities.md`: dòng `workflow-enable` mới không có mẫu đường dẫn (cột thứ hai `-`), như `cross-repo-read`, nên quy tắc 8 không áp cho plan nào; plan không đổi dòng có sẵn;
- `scripts/status.py` đọc trạng thái bài học trong `docs/lessons.md` và mẫu của `docs/auto-run.md`: plan chỉ đổi trạng thái L-126 sang `applied`, đúng khuôn;
- `scripts/new-app.sh` và `.github/ISSUE_TEMPLATE/from-app.yml` chỉ nhắc tên `docs/lessons.md`;
- `.github/workflows/pr-check.yml` và `scripts/local-setup.sh` chạy hay nhắc `scripts/tests/run.sh`, không phụ thuộc test mới.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền của app. Quyền của workflow: `actions: write` cho `resume-run.yml`, `run-watchdog.yml` và job `fire` của `start-run.yml`; không thêm bí mật.
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** `GITHUB_TOKEN` với `actions: write` có gọi được `disable` của chính workflow đang chạy không (tài liệu của GitHub ghi API này nhận token có `actions: write`; chưa thử trên một workflow thật, vì chỉ thử được sau khi file workflow lên nhánh chính).
- **Điều kiện chuyển:** sau khi merge, lần chạy tay đầu tiên (`workflow_dispatch`) của `resume-run` khi không có phiên in CHÚ Ý `disable` lỗi (403 hay 404).
- **Cách làm thay:** không cần đổi code: script đã không làm đỏ workflow khi `disable` lỗi, lịch giữ như hiện nay. Mở ticket mới cho phương án B (biến repo) hay để người tắt tay theo mục 2.8 cũ; ghi vào nhật ký. Tiêu chí và test ở mục 4 không đổi.

### 3.5. Hành vi đã có bị đổi

Không có code Kotlin. Hành vi của workflow bị đổi:

| Hành vi cũ | Hành vi mới | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `resume-run`, `run-watchdog` chạy mọi lúc theo lịch | Tự tắt khi rảnh | Không có test nào khẳng định lịch luôn chạy (`grep -n 'resume-run.yml\|run-watchdog.yml' scripts/tests/run.sh`: chỉ các khối `resume-check`, `run-watchdog`, kiểm logic của script) | Giữ mọi test có sẵn |
| `run-watchdog.yml` chỉ có `actions: read` | `actions: write` | Không có test kiểm khối `permissions` của file này | Thêm test mới ở mục 4 |

## 4. Kiểm thử

Test trong `scripts/tests/run.sh`, dùng `scripts/tests/fake-gh` (đường đi cố định, `__sequence__` cho lần đọc lại, lệnh ghi vào `$FIXTURE.writes`, `fail_writes`). Hàm dựng `schedule_idle <status> <giờ updated_at> <giờ start-run> [lần đọc lại]` theo kiểu `resume_check`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Hoàn thành 2: có phiên `running` thì không tắt | Test script | `schedule-idle: a running session keeps the schedule`: không có dòng ghi |
| Hoàn thành 1: rảnh thì tắt | Test script | `schedule-idle: no session, enabled long ago, no recent start-run: disables`: đúng một dòng `PUT actions/workflows/resume-run.yml/disable` |
| Khe tranh chấp: vừa được bật | Test script | `schedule-idle: enabled within the grace hours: keeps`: `updated_at` 1 giờ trước, không có dòng ghi; đối chứng 4 giờ trước thì tắt |
| Khe tranh chấp: `start-run` vừa chạy | Test script | `schedule-idle: start-run ran within the grace hours: keeps`: không có dòng ghi; đối chứng 4 giờ thì tắt |
| Khe tranh chấp: phiên hiện ra sau khi tắt | Test script | `schedule-idle: a session that appears after the disable turns the schedule back on`: lần đọc đầu `finished`, lần đọc lại `running` (`__sequence__`); hai dòng `disable` rồi `enable` theo thứ tự |
| Danh sách `finished` cũ không giữ lịch | Test script | `schedule-idle: a finished run does not keep the schedule`: nhánh báo cáo có `status: finished`: tắt |
| Không đọc được GitHub thì không tắt | Test script | `schedule-idle: GitHub unreadable: keeps, exit 0`: `errors` cho `branches?...`: không có dòng ghi, mã 0, thông báo có "Không đọc được GitHub" |
| `disable` lỗi không làm đỏ workflow | Test script | `schedule-idle: a failed disable warns and exits 0`: `fail_writes` cho `disable`: mã 0, có "CHÚ Ý" |
| `--enable` bật cả hai | Test script | `schedule-idle --enable: enables both schedules`: hai dòng `PUT .../resume-run.yml/enable`, `PUT .../run-watchdog.yml/enable`; `state` đọc `active`: mã 0 |
| `--enable` báo khi không bật được | Test script | `schedule-idle --enable: a failed enable exits 1 and names the workflow`: `fail_writes` cho `run-watchdog.yml/enable`: mã 1, thông báo có `run-watchdog.yml` |
| Workflow gọi script với đúng tệp của mình và có quyền | Test đọc file | `workflows: resume-run and run-watchdog disable themselves when idle`: mỗi file có dòng `schedule-idle.py --disable-if-idle <chính tên file>` và `actions: write`; trong `run-watchdog.yml` bước tắt nằm sau bước `run-watchdog.py` |
| `start-run.yml` bật hai lịch trước khi kích hoạt | Test đọc file | `start-run: enables both schedules before firing the routine`: job `fire` có `actions: write` và hai lệnh `actions/workflows/resume-run.yml/enable`, `.../run-watchdog.yml/enable` trước dòng `curl` |
| Lịch tự tắt trên GitHub thật | Kiểm tra thủ công (người: cần GitHub thật sau khi merge) | Khi không có phiên: chạy tay `resume-run` và `run-watchdog` (Run workflow); phần tóm tắt có "đã tắt", `gh workflow list --all` thấy `disabled_manually`. Rồi `scripts/schedule-idle.py --enable` trên máy dev: cả hai `active` |
| Phiên bật lại được | Kiểm tra thủ công (người: chỉ thấy ở phiên thật) | Phiên tự chạy đầu tiên sau khi merge: mục Kiểm tra đầu phiên có dòng của `--enable`; ghi kết quả vào `docs/ops/session-capabilities.md`, dòng `workflow-enable` |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** tài liệu (`docs/`) và chú thích đầu workflow. Hai test đọc file workflow thất bại trước khi sửa workflow, nên vẫn viết trước.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-29, trên repo dự án mẫu, bằng `gh` của máy dev (không đổi trạng thái).
- **Lệnh:** `gh api repos/VGL-Studio/app-project-template/actions/workflows/run-watchdog.yml --jq '.state + " " + .updated_at'`; `gh api -X PUT repos/VGL-Studio/app-project-template/actions/workflows/run-watchdog.yml/enable -i`; đọc lại; `gh api "repos/.../actions/workflows/start-run.yml/runs?per_page=1" --jq '.workflow_runs[0].created_at'`.
- **Kết quả:** `state` đọc được (`active`). `enable` lên workflow đang bật trả `HTTP 204` và đổi `updated_at` từ `2026-10-28T08:47:04+07:00` sang giờ gọi (`2026-10-29T18:10:12+07:00`), nên dùng được làm mốc "vừa được bật". `created_at` của lần chạy `start-run` gần nhất đọc được. Chưa thử `disable` (đổi trạng thái thật) và chưa thử bằng `GITHUB_TOKEN` hay token của phiên cloud: xem mục 3.4 và hai dòng kiểm tra thủ công. `scripts/check.sh` không cần (không có code Kotlin); chạy `scripts/tests/run.sh`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và quy trình phiên tự chạy: `.github/workflows/`, `docs/auto-run.md`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Ba workflow, một script mới, test, bốn tài liệu; không chạm code app |
| B. Độ rõ của yêu cầu | 1 | Phương án đã chốt; còn mốc `GRACE_HOURS` (câu hỏi 1) |
| C. Khả năng kiểm chứng tự động | 1 | Logic có test với `gh` giả; quyền thật của `GITHUB_TOKEN` và phiên chỉ thấy sau khi merge |
| D. Ảnh hưởng chéo | 2 | `start-run.yml` là đường mở phiên chính; lịch tắt nhầm thì phiên không tự tiếp tục |
| E. Độ mới của kỹ thuật | 2 | Lần đầu dùng API bật tắt workflow trong dự án; phổ biến, có tài liệu |
| **Tổng** | 8 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `start-run.yml`, `docs/auto-run.md` mục 2, mẫu danh sách chạy.
- Task khác có thể xung đột: T-052 (ba ticket nâng bộ build, chưa có plan) không chạm các file này. Không có danh sách chạy nào đang `draft` hay `approved`.
- Các app nhận thay đổi qua đợt đồng bộ (`scripts/template-sync.sh`): workflow và script là của dự án mẫu. App đang tắt tay các lịch (Photune) vẫn đúng: lịch đã tắt thì không chạy, phiên kế tiếp bật lại.
- Đề xuất thứ tự: làm trong phiên có người, trước T-052.

## 7. Điều kiện dừng riêng

- Cần thêm bí mật, token cá nhân hay biến repo để làm được: dừng (trái quyết định đã chốt).
- Lần chạy tay đầu tiên sau khi merge mà `resume-run` hay `run-watchdog` đỏ (không chỉ CHÚ Ý): revert theo ADR-0025 cùng dev.

## 8. Câu hỏi cho người duyệt

1. `GRACE_HOURS` mặc định 3: đủ cho khoảng từ lúc `start-run` hay một lần `enable` tới lúc phiên đẩy nhánh báo cáo, kể cả khi routine xếp hàng. Lớn hơn thì sau mỗi lần bật tay lịch còn chạy lâu hơn (3 giờ là khoảng 6 lần `run-watchdog` và 3 lần `resume-run`). Đề xuất: 3.
2. Phiên mở bằng Run now, hẹn giờ hay `claude --cloud` mà token của phiên không bật được workflow: dòng của mục 2 ghi vào Cần người kiểm tra, người bật tay. Đề xuất chấp nhận; phiên đầu tiên sau khi merge cho biết (dòng kiểm tra thủ công cuối).
3. Danh sách chạy bị bỏ quên ở `running` (nhánh báo cáo cũ) giữ lịch bật mãi, như hiện nay. `run-watchdog` vẫn báo bằng issue `session-stalled`. Đề xuất giữ, không thêm giới hạn tuổi.

## 9. Quyết định duyệt

- **Quyết định:**
- **Người duyệt:** , **duyệt chéo:**
- **Ghi chú:**

## 10. Nhật ký thực hiện
