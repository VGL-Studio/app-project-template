---
ticket: T-100
title: merge-if-green.sh không từ chối vì một lần chạy bị hủy đã có lần chạy cùng tên sau nó
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 5
risk_level: high
token_estimate: 8.6M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-11-09
lane:
---

# PLAN T-100: MERGE-IF-GREEN.SH KHÔNG TỪ CHỐI VÌ MỘT LẦN CHẠY BỊ HỦY ĐÃ CÓ LẦN CHẠY CÙNG TÊN SAU NÓ

## 1. Tóm tắt

Bước "có kiểm tra không qua" của `scripts/merge-if-green.sh` bỏ qua một kiểm tra `cancelled` khi trên cùng commit có một lần khác của kiểm tra đó (cùng tên, cùng ứng dụng) bắt đầu sau nó hoặc chưa bắt đầu. Lần mới nhất vẫn đi qua `check_one` như nay: đang chạy thì chờ, `cancelled` thì từ chối. Mọi kết quả đỏ khác vẫn từ chối dù có lần sau. `check_one` chọn lần mới nhất theo cùng thứ tự, lần chưa bắt đầu là mới nhất. `docs/auto-run.md` bước 8 và skill `merge-pr` thêm cách xử lý PR không có lần chạy `pr-check`: đẩy commit rỗng, không đóng mở lại PR. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả. Task làm có người (`auto_run: false`).

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn là L-164, `accepted` ngày 2026-11-09 (anh Nhật: "Nhận L-164, tạo ticket và viết plan.").

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 1: lần hủy cũ không chặn | `cancelled` cũ cùng lần sau đang chạy: chờ (mã 2); lần sau xanh: merge | "Lần sau" là một check-run khác trên cùng commit, cùng `name`, cùng `app.id` (thiếu `app` thì coi như rỗng, cùng nhau), có `started_at` lớn hơn, hoặc `started_at` rỗng khi lần hủy có `started_at`. Bằng nhau thì không phải lần sau (mục 3.1 A) |
| Phạm vi 1: lần hủy mới nhất | Vẫn từ chối | Không có lần sau thì lần hủy còn trong danh sách đỏ, từ chối ngay như nay. Lần hủy có `started_at` rỗng: không lần nào sau nó |
| Phạm vi 1: đỏ khác | `failure`, `timed_out`, `action_required`, `startup_failure` vẫn từ chối dù có lần sau xanh | Chỉ đổi điều kiện cho `cancelled`. Endpoint `commits/<sha>/check-runs` mặc định `filter=latest`: lần chạy lại job trong cùng một lần chạy workflow (cùng check suite) đã che lần cũ, nên chạy lại job đỏ rồi xanh hôm nay đã merge được. Bộ lọc mới chỉ có tác dụng giữa hai lần chạy workflow (hai check suite), như dữ liệu L-164 |
| Phạm vi 2: `check_one` | Lần chưa bắt đầu là mới nhất | Hôm nay `sort_by(.started_at)` xếp `null` đầu tiên, nên một lần chưa bắt đầu bị coi là cũ nhất. Đổi thành `sort_by([.started_at == null, .started_at])` |
| Phạm vi 3: test | Mỗi ca một test, kèm đối chứng; mỗi kiểm tra khác một test | Mục 4, T1 tới T12 |
| Phạm vi 4: tài liệu | `docs/auto-run.md` bước 8 | Mục 3.1 C |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan:
- ADR-0010: cổng chất lượng là `merge-if-green.sh`, mọi kiểm tra đỏ bị từ chối. Plan chỉ coi lần hủy đã có lần sau là "không phải kết quả", như `wait-run.sh` đã làm.
- ADR-0041: thứ tự các kiểm tra giữ nguyên. Bước kiểm tra đỏ vẫn đứng sau bước nhánh chậm và trước `check_one`.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/merge-if-green.sh`, bước "có kiểm tra không qua" (trong `evaluate`, ngay sau khi đọc `RUNS`).**

Hôm nay:

```bash
failed="$(jq -r '[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")' <<<"$RUNS")"
```

Sau khi sửa, cùng biến `failed`, cùng thông báo, bộ lọc jq thành (đã chạy thử, mục 4.1):

```jq
def key: "\(.app.id // "")/\(.name)";
# true when check $b started after check $a; a check not started yet (started_at null) counts as the newest.
def later($a; $b): ($b.started_at == null and $a.started_at != null)
  or ($a.started_at != null and $b.started_at != null and $b.started_at > $a.started_at);
[to_entries[] | .value + {i: .key}] as $all
| [ $all[]
    | select(.status == "completed" and (.conclusion | IN("failure","cancelled","timed_out","action_required","startup_failure")))
    | . as $c
    | select($c.conclusion != "cancelled"
        or ([$all[] | select(.i != $c.i and key == ($c | key) and later($c; .))] | length) == 0)
    | .name ]
| unique | join(", ")
```

- Chỉ số `i` để một check-run không tự khớp với chính nó (lần thử đầu thiếu điều này, mục 4.1). Fixture của test không có `id`, nên không dùng `id`.
- So `started_at` bằng chuỗi: GitHub trả cùng dạng `YYYY-MM-DDTHH:MM:SSZ`, so chuỗi đúng thứ tự thời gian.
- Khóa là `app.id` cùng `name`, không tra workflow (mục 8, câu 1).
- Bộ lọc đặt trong một biến riêng ngay trước dòng `failed=`, nhiều dòng trong nháy đơn: dòng mở `failed_filter='` và dòng đóng `' # end of failed_filter`; dòng `failed=` thành `failed="$(jq -r "$failed_filter" <<<"$RUNS")"`. Hai mốc này để test đối chứng T11 thay được cả khối.
- "Lần mới nhất đi qua `check_one`" chỉ đúng với `REQUIRED_CHECKS` (`build-and-test`). Một kiểm tra khác có lần cũ `cancelled` và lần sau còn chạy thì không bị chờ, như một kiểm tra phụ đang chạy một mình hôm nay (test có sẵn "a running ui-test is not waited for"). `check_one` khóa theo tên, bộ lọc khóa theo `app.id` cộng tên; hành vi có sẵn, không đổi.
- Chú thích trên dòng (tiếng Anh): lần hủy đã có lần cùng tên sau nó không phải kết quả, vì `concurrency` của workflow hủy lần trùng; lần mới nhất đi qua `check_one`.

**B. `check_one`.** Dòng chọn lần mới nhất:

```bash
latest="$(jq -c --arg n "$name" '[.[] | select(.name==$n)] | sort_by([.started_at == null, .started_at]) | last // empty' <<<"$RUNS")"
```

Phần còn lại của hàm giữ nguyên. Lần mới nhất đang chạy thì `pending`; `cancelled` thì `die "kiểm tra '...' kết thúc với kết quả 'cancelled'."`, như nay.

Ca của L-164 sau khi sửa: lần hủy có lần sau đang chạy, nên `failed` rỗng; `check_one` chọn lần đang chạy và chờ. Với `--update`, script chờ tới khi lần sau xong; xanh thì merge.

**C. `docs/auto-run.md`, bước 8.** Thêm một gạch đầu dòng sau dòng mã 7:

- Mã 2 với `--pr` mà dòng kết quả là "chưa thấy lần chạy nào", đã chờ ít nhất 10 phút kể từ lần đẩy (lần chờ đầu ngắn hơn, ví dụ `--timeout 9`, thì gọi lại trước), PR không xung đột (không phải mã 5) và không phải nháp (không phải mã 6): GitHub có lúc không phát sự kiện của commit vừa đẩy. Đẩy một commit rỗng (`git commit --allow-empty`), rồi chờ lại bằng `wait-run.sh --pr <số> --sha <commit mới>` đủ thời gian chờ, không đóng mở lại PR: sự kiện trễ của commit trước cùng sự kiện mở lại sinh hai lần chạy, và `concurrency` hủy một lần. Không tính vào 3 lần sửa. Commit rỗng vẫn không có lần chạy thì ghi vào báo cáo, mục Cần người kiểm tra.

Skill `merge-pr`, bước 2: câu "Mã 2 (hết giờ chờ): gọi lại" thêm "; dòng kết quả 'chưa thấy lần chạy nào' thì theo bước 8", để dev merge có người cũng thấy cách xử lý này.

Viết theo ADR-0044: thì hiện tại, không mã bài học, không mã ticket. Chạy `scripts/history-refs.py`.

**D. Chú thích đầu file `merge-if-green.sh`.** Không đổi mã thoát. Thêm một câu vào khối chú thích đầu: lần chạy bị hủy mà có lần chạy cùng tên sau nó trên cùng commit không tính là đỏ.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | Mục 3.1 A, B, D |
| Sửa | `scripts/tests/run.sh` | Test T1 tới T12 của mục 4, cạnh các test `merge-if-green` có sẵn dùng `merge_routes`, `merge_run`, `one_job_with`. Thêm một hàm nhỏ dựng câu trả lời `check-runs` từ nhiều check-run (`runs_of`), không đổi hàm có sẵn |
| Sửa | `docs/auto-run.md` | Mục 3.1 C: bước 8, một gạch đầu dòng |
| Sửa | `.claude/skills/merge-pr/SKILL.md` | Mục 3.1 C: bước 2, câu về mã 2 trỏ về bước 8 |
| Sửa | `docs/lessons.md` | L-164: `applied`, dòng Áp dụng ghi số PR. Ghi trong PR của task, như T-083 |
| Sửa | `docs/features/quy-trinh/plans/T-100-merge-bo-qua-lan-huy-cu.md` | Mục 10, nhật ký "Làm có người", ghi ở lần đẩy cuối của PR task |
| Sửa | `docs/features/quy-trinh/tickets/T-100-merge-bo-qua-lan-huy-cu.md` | Trạng thái ticket theo kết quả |

Nơi đọc file bị đổi (`grep -rln 'merge-if-green' scripts/ .github/ .claude/`):
- Skill `to-app`, `whatnow` và `scripts/wait-run.sh` chỉ gọi script, đọc mã thoát hay nhắc tên. Mã thoát không đổi.
- `docs/ops/failure-playbook.md` dò chuỗi `merge conflict ...`, `unexpected end of JSON input`, `chưa thấy kiểm tra 'changes'` của script. Plan không đổi thông báo nào, nên không ảnh hưởng.
- `.github/workflows/pr-check.yml`, `main-guard.yml` chạy `scripts/tests/run.sh` và nhắc script trong chú thích; không đổi.
- `scripts/is-exact-revert.sh`, `scripts/main-guard-verified.sh`, `scripts/resume-check.py`, `scripts/run-watchdog.py`, `scripts/plan-check.py` chỉ nhắc script trong chú thích hay thông báo; không đọc bước kiểm tra đỏ.
Các file khác mà `plan-check.py` (L-020) thấy có nhắc tới file của plan; không file nào bị ảnh hưởng, không sửa:
- Nhắc `scripts/merge-if-green.sh` hay `scripts/tests/run.sh`:
  - `.github/workflows/main-guard.yml` chạy `scripts/tests/run.sh` và nhắc `merge-if-green.sh` trong chú thích; không đọc thông báo hay mã thoát của bước kiểm tra đỏ;
  - `scripts/local-setup.sh` nhắc `scripts/tests/run.sh` trong chú thích;
  - `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` nhắc `scripts/tests/run.sh` trong chú thích, không chạy nó.
- Nhắc `docs/auto-run.md` (dẫn mục hay bước khác, hay dẫn chung cả file). Plan chỉ thêm một gạch đầu dòng trong bước 8, không đổi tên mục hay số bước nào:
  - workflow `.github/workflows/device-session.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - script `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/run-check.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/token-usage.py`, `scripts/ui-test-verdict.sh`;
  - tệp log `scripts/tests/playbook/PB-17.log`.
- Nhắc `docs/lessons.md`:
  - `.github/ISSUE_TEMPLATE/from-app.yml`, `scripts/new-app.sh` dẫn cả file;
  - `scripts/status.py` đọc trạng thái bài học; `applied` là trạng thái nó đã biết.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Bộ lọc jq đã chạy thử với 10 ca trên jq 1.7.1 (mục 4.1); `IN` có từ jq 1.6 và script hôm nay đã dùng nó.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Hai hành vi của script đổi; không test có sẵn nào khẳng định hành vi cũ, vì mọi fixture có sẵn chỉ có một check-run mỗi tên và đều có `started_at`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mọi `cancelled` trên commit từ chối | `cancelled` có lần cùng khóa sau nó không chặn | Không có. `merge-if-green: a red build-and-test is refused`, `... a leftover red changes check still refuses` dùng `failure`, một check-run mỗi tên: giữ, vẫn mong 1 | Câu mới: T1, T2, T6 |
| `check_one` coi lần `started_at` rỗng là cũ nhất | Là mới nhất | Không có (mọi fixture có `started_at`) | Câu mới: T7 |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, cạnh các test `merge-if-green` có sẵn, dùng `gh` giả (`scripts/tests/fake-gh`). Hàm `runs_of` nhận nhiều bộ `<tên> <status> <conclusion JSON> <started_at JSON> <suite id> [app id]` và in `{"check_runs": [...]}`. Mỗi lần chạy workflow một `check_suite.id` riêng (lần cũ 5, lần sau 6), như dữ liệu L-164: GitHub không trả hai check-run cùng tên trong một suite (`filter=latest`). Thời điểm: `A` = `"2026-10-18T02:01:00Z"`, `B` = `"2026-10-18T02:02:00Z"`. Nhánh `claude/template-x`, `behind_by` 0, `merge_run 12 --dry-run` trừ khi ghi khác. Kiểm tra đang chạy cần route `actions/runs?head_sha=abc1234&per_page=50` như test "build-and-test still running waits" có sẵn. "Đỏ trước code" là kết quả dự kiến trên script hôm nay; phiên kiểm lại khi viết test.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Ca L-164: lần hủy cũ, lần sau đang chạy thì chờ | Test script | T1 `merge-if-green: a cancelled check with a newer run of the same check waits for that run`: `build-and-test` `cancelled` ở A (suite 5), `in_progress` ở B (suite 6); route `actions/runs?head_sha=abc1234&per_page=50` trả hai lần chạy, suite 5 `completed`/`cancelled` và suite 6 `in_progress`/`null`, để nhánh "job còn ghi chạy mà lần chạy đã xanh" của `check_one` tra đúng suite của lần mới nhất. Mong `2 1 0`: mã thoát; số dòng `đang ở trạng thái in_progress`; số dòng `có kiểm tra không qua`. Đỏ trước code (`1 0 1`) |
| Lần hủy cũ, lần sau xanh thì sẵn sàng | Test script | T2: `cancelled` ở A, `success` ở B. Mong 0. Đỏ trước code (1) |
| Lần mới nhất bị hủy vẫn từ chối | Test script | T3 (đối chứng): `success` ở A, `cancelled` ở B. Mong `1 1`, phần hai là số dòng `có kiểm tra không qua: build-and-test\.`. Qua trước code |
| Chỉ một lần, bị hủy | Test script | T4 (đối chứng): `cancelled` ở A. Mong `1 1` như T3. Qua trước code |
| Đỏ khác không được che | Test script | T5 (đối chứng): `failure` ở A, `success` ở B; rồi `timed_out` ở A, `success` ở B. Đầu ra `a b`, mong `1 1`. Qua trước code |
| Lần chưa bắt đầu là lần sau | Test script | T6: `cancelled` ở A, `queued` với `started_at` `null`. Mong `2 1`, phần hai là số dòng `đang ở trạng thái queued`. Đỏ trước code (`1 0`) |
| `check_one` chọn lần chưa bắt đầu | Test script | T7: `success` ở A, `queued` với `started_at` `null`, không có lần đỏ nào. Mong `2`. Đỏ trước code (0: `sort_by` hôm nay xếp `null` đầu, chọn lần xanh) |
| Không che giữa hai ứng dụng, giữa hai tên, khi cùng giờ | Test script | T8, ba ca, đều mong 1. (a) `cancelled` ở A với `app.id` 1, `success` ở B với `app.id` 2. (b) `build-and-test` `cancelled` ở A, `other` `success` ở B (cộng `build-and-test` không có lần nào khác). (c) `cancelled` ở A, `success` cũng ở A. Đầu ra `a b c`, mong `1 1 1`. Qua trước code |
| Lần hủy chưa bắt đầu không có lần sau | Test script | T9 (đối chứng): `success` ở A, `cancelled` với `started_at` `null`. Mong 1. Qua trước code. Bắt lỗi một check-run tự khớp với chính nó (mục 4.1) |
| Với `--update`: chờ rồi merge | Test script | T10: route `commits/abc1234/check-runs?per_page=100` là `{"__sequence__": [<T1>, <cancelled ở A, success ở B>]}`. `merge_run 12 --update`. Mong `0 1`: mã thoát; số lệnh `PUT pulls/12/merge` trong `$work/mg.json.writes` (`mg_merges`). Đỏ trước code (`1 0`) |
| Đối chứng của bộ lọc mới | Test script | T11: bản chép `$work/mgi-old-filter.sh` của script, khối từ `failed_filter='` tới `' # end of failed_filter` thay bằng bộ lọc hôm nay bằng `perl -0777 -pe` (mẫu `failed_filter=\x27.*?\x27 # end of failed_filter\n`, cờ `s`), bộ lọc cũ là `[.[] | select(.status=="completed" and (.conclusion|IN("failure","cancelled","timed_out","action_required","startup_failure"))) | .name] | unique | join(", ")`. Chạy dữ liệu của T2 như test "skipped-check control" có sẵn. Mong `differs 1`: bản chép khác bản gốc (`cmp`), và từ chối. Chứng minh T2 qua nhờ bộ lọc mới |
| Ca mới vẫn qua mọi kiểm tra khác | Test script | T12, dữ liệu của T2 (lần hủy cũ, lần sau xanh), mỗi ca sửa fixture bằng một lệnh `python3 -I -c` như các test có sẵn. (a) PR nháp: mong 1, có `bản nháp`. (b) Nhãn `needs-human`: mong 1, có `needs-human`. (c) Issue `main-broken` mở: dựng bằng `merge_while_broken "$one_issue" "$work/bin/revert-no" finished claude/template-x`, thêm route `commits/<sha của good>/check-runs?per_page=100` trả dữ liệu T2 và đổi `behind_by` thành 0. Hàm vừa ghi `broken.json` vừa chạy script trong một lần gọi, nên tách phần ghi tệp ra: thêm biến môi trường `MWB_EXTRA` (mặc định rỗng) chèn thêm route và `MWB_BEHIND` (mặc định 2), test có sẵn không đổi. Mong 1, `$work/broken.out` có `main-broken`. Đối chứng cùng dữ liệu với `revert-yes` là một câu `expect` riêng, T12b: mong 0 (bản revert đúng qua, bộ lọc mới cho qua lần hủy cũ); đỏ trước code (1). (d) Phiên `running` mà PR không thuộc phiên: thêm nhánh `claude/RUN-20261018-01-report` vào `branches?per_page=100&page=1` và route file danh sách `running`: mong 1, có `đang chạy`. (e) `behind_by` 2, không `--update`: mong 3. (f) PR `closed`: mong 1. (g) base `other`: mong 1, có `nhắm vào 'other'`. (h) Nhánh chậm chỉ vì tài liệu: dựng như các test "behind by docs" có sẵn (hàm `nu_edit`), check-runs là dữ liệu T2, và route `pr-check.yml/runs` thêm một lần chạy cũ `cancelled` có `created_at` sớm hơn lần 91, để kiểm `--gate-ran` chọn lần mới nhất; mong 0 và có `KHÔNG CẬP NHẬT NHÁNH`. Đầu ra `a b c d e f g h`, mong `1 1 1 1 3 1 1 0`. Kiểm tra "đã merge trước đó" (`merged_on_main`) đứng trước và chỉ trả kết quả khi nhánh chính có commit mang tiêu đề của PR; dữ liệu T2 không có commit đó, test có sẵn của nó giữ nguyên. Trước code (a) tới (g) cũng ra như vậy, vì các kiểm tra này đứng trước hay không phụ thuộc bước kiểm tra đỏ; (h) đỏ trước code (1, bộ lọc cũ từ chối dữ liệu T2), nên trước code chuỗi là `1 1 1 1 3 1 1 1`; chúng giữ cổng khi bộ lọc nới. Kiểm tra của `check_one` cho lần sau: T1, T6, T7 |
| `docs/auto-run.md` đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc gạch đầu dòng mới ở bước 8. `scripts/history-refs.py` qua |
| Toàn bộ | Test script | `scripts/tests/run.sh` trên CI (`pr-check`, rồi `main-guard` sau merge). Trên máy dev macOS: `source ~/.local/share/app-project-template/env.sh` trước (không có JDK thì bộ test dừng im lặng ở khoảng 624 dòng `ok`), bản đầy đủ in dòng tổng kết; 6 test đỏ sẵn trên nhánh chính (5 `pr-check: ... order check`, 1 `wait-run: a draft PR ...`), so danh sách `FAIL` với nhánh chính |

Các hàm sửa fixture có sẵn: T12 dùng lại cách của test T-083 (`merge_during_run`, `merge_while_broken`) khi hợp, hay sửa `$work/mg.json` trực tiếp. Phiên chọn cách ngắn hơn, câu khẳng định giữ như bảng.

**Ngoại lệ viết test trước:** sửa `docs/auto-run.md`, `.claude/skills/merge-pr/SKILL.md`, `docs/lessons.md`, nhật ký plan và ticket: chỉ là tài liệu. Câu chú thích đầu file của script: chỉ chú thích.

### 4.1. Chạy thử

Không cần (E dưới 2). Một lần chạy thử bộ lọc jq ngày 2026-11-09, để chọn bộ lọc ở mục 3.1 A:

- **Đã chạy thử:** bộ lọc trong tệp tạm, chạy bằng `jq -r -f` trên 10 mảng check-run: ca L-164; lần mới nhất bị hủy; `failure` cũ cùng lần sau xanh; lần sau chưa bắt đầu; hai ứng dụng; cùng giờ; lần hủy chưa bắt đầu; chỉ một lần hủy; tên khác; mảng rỗng. Cùng lệnh thử thứ tự `sort_by([.started_at == null, .started_at]) | last`.
- **Lệnh:** `jq -r -f failed.jq` với jq 1.7.1 (macOS), bash 3.2.57.
- **Kết quả:** bản đầu không loại chính check-run đang xét, nên lần hủy có `started_at` rỗng tự coi mình là "lần sau" và bị bỏ qua (ca "lần hủy chưa bắt đầu" ra rỗng, đúng ra phải từ chối). Thêm chỉ số `i`, cả 10 ca đúng: ca L-164, lần sau chưa bắt đầu và mảng rỗng ra rỗng; 7 ca còn lại ra `b`. `sort_by` mới chọn lần `started_at` rỗng. Ca đó thành T9.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Plan sửa `scripts/merge-if-green.sh`, file được bảo vệ, là cổng merge của mọi phiên. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Hai dòng jq của một script, test của nó, một gạch đầu dòng tài liệu |
| B. Độ rõ của yêu cầu | 1 | Bài học nêu rõ cách sửa. Còn một điểm người chọn: khóa theo ứng dụng hay theo workflow (mục 8, câu 1) |
| C. Khả năng kiểm chứng tự động | 0 | Mọi ca có test với `gh` giả, kèm đối chứng |
| D. Ảnh hưởng chéo | 3 | Mọi lần merge của người và phiên đi qua bước này. Sai hướng nới thì một PR chưa có CI xanh lọt vào nhánh chính |
| E. Độ mới của kỹ thuật | 0 | jq đã dùng khắp script |
| **Tổng** | 5 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `merge-if-green.sh`, mọi lần merge. Commit chỉ có một lần chạy mỗi kiểm tra (gần như mọi PR) không đổi kết quả: bộ lọc mới chỉ khác bộ lọc cũ khi có hai check-run cùng khóa.
- Task khác có thể xung đột: không plan `approved` hay `draft` nào khác đang sửa `merge-if-green.sh`.
- Cách làm:
  - Làm có người (`auto_run: false`), như T-083. Nhánh `claude/T-100-merge-bo-qua-lan-huy-cu`, làm khi không có phiên `running`.
  - PR của task merge bằng `merge-if-green.sh` bản cũ từ thư mục chính ở nhánh chính. Trước đó chạy bản mới từ worktree của task với `--dry-run` trên chính PR, để thấy bản mới chạy được với GitHub thật.
- App nhận qua `scripts/template-sync.sh`.
- Plan không sửa `.github/workflows/`, nên không cần `push-probe.sh`.
- Đề xuất thứ tự: độc lập, làm ngay khi không có phiên nào chạy.
- Khoảng hở còn lại: trong ca L-164, lần đầu kết thúc lúc 10:47:16 và check-run của lần sau mới có lúc 10:47:19. Gọi script đúng mấy giây đó vẫn bị từ chối, kể cả với `--update`; gọi lại là qua. Plan không xử lý khoảng này.
- Xử lý `started_at` rỗng là phòng xa: dữ liệu L-164 có `started_at` cho cả hai lần; chưa thấy GitHub trả `null` cho lần `queued`.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu nhánh chính. Dừng, cần plan phiên bản mới, khi dòng `failed=` hay dòng `latest=` của `check_one` không còn dạng ở mục 3.1 A, B. Chỉ lệch số dòng thì theo ADR-0032/L3.
- Một test có sẵn của `merge-if-green` đổi kết quả: dừng. Mục 3.5 nói không test nào phải đổi.
- Có phiên `running` lúc định merge PR của task: dừng, chờ phiên kết thúc.
- Cần sửa `.github/workflows/`, `scripts/check.sh` hay `scripts/release.sh`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Khóa để biết hai check-run là "cùng một kiểm tra".**
   - (a) Cùng `name` và cùng `app.id`. Đề xuất: (a). Chỉ `pr-check.yml` chạy trên sự kiện `pull_request`, nên trên commit đầu của PR không có hai workflow trùng tên job. `check_one` hôm nay cũng chỉ khóa theo tên. Không tốn thêm lời gọi API.
   - (b) Thêm workflow: tra `actions/runs?head_sha=...` để biết `check_suite.id` thuộc workflow nào. Chặt hơn khi sau này có workflow thứ hai trên PR, nhưng thêm một lời gọi API có thể lỗi; lỗi thì phải từ chối.
2. **Giữa hai lần chạy workflow, chỉ `cancelled` cũ được che, `failure` cũ thì không.** Chạy lại job đỏ trong cùng lần chạy đã merge được hôm nay (GitHub chỉ trả lần mới nhất của suite). Ca còn lại hiếm: hai lần chạy workflow trên cùng commit, lần cũ đỏ, lần sau xanh. Đề xuất: giữ từ chối, ngoài phạm vi L-164; muốn nới thì một bài học riêng.
3. **Ghi L-164 `applied` trong chính PR của task** (mục 3.2), như T-083. Đề xuất: đồng ý.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev: người duyệt cũng là người duyệt chéo, `risk-scoring.md` mục 3)
- **Ghi chú:**
  - AI ghi theo lời anh Nhật ngày 2026-11-09, trong phiên có người: "Duyệt T-100. merge #438".
  - Lời duyệt trả lời bản plan ở commit 9b171d9 (PR #438), sau rà soát độc lập và một vòng rà soát lại. Lời duyệt không nêu riêng từng câu, nên mục 8 theo đề xuất của plan, như AI đã trình ngay trước lời duyệt:
    - câu 1 (a): khóa theo tên cùng ứng dụng, không tra workflow;
    - câu 2: `failure` của lần chạy workflow cũ vẫn chặn;
    - câu 3: ghi L-164 `applied` trong PR của task.
  - Task làm có người (`auto_run: false`), khi không có phiên `running`.

## 10. Nhật ký thực hiện

### Làm có người 2026-11-09, phiên bản plan 1

- **Kết quả:** merge bằng PR #439 (CI của PR này), sau lời merge của anh Nhật.
- **Nhánh, PR:** `claude/T-100-merge-bo-qua-lan-huy-cu`, https://github.com/VGL-Studio/app-project-template/pull/439. Commit test `72c7b86`, code `b945fd9`.
- **Đã làm:** mục 3.1 A tới D. Bộ lọc kiểm tra đỏ của `merge-if-green.sh` trong biến `failed_filter` có hai mốc; `check_one` xếp lần chưa bắt đầu là mới nhất; chú thích đầu file. `docs/auto-run.md` bước 8 và skill `merge-pr`. L-164 `applied`.
- **Test viết trước:** T1 tới T12 trong `scripts/tests/run.sh` (vài ca gộp một dòng `expect`). 8 test đỏ đúng lý do trên code cũ, khớp "đỏ trước code" của mục 4: T1 `1 0 1`, T2 `1`, T6 `1 0`, T7 `0`, T10 `1 0`, T11 `same 1`, T12 đối chứng bản revert đúng `1`, T12 (h) `1 0`; các test còn lại qua cả trước code như plan ghi. File test không đổi sau commit test. Không test có sẵn nào đổi.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 6 ghi chú, ghi trong mô tả PR.
- **Kết quả CI:** CI của PR này. `scripts/tests/run.sh` trên máy dev (có JDK): trước code 14 FAIL (6 đỏ sẵn của macOS cộng 8 test mới), sau code 1541 ok và chỉ 6 đỏ sẵn.
- **Lệch so với plan:** ADR-0032/L4: T12 (c) dựng bằng hàm `broken_with_t2` theo mẫu test "behind by docs" (`nu_sha`), không thêm biến `MWB_EXTRA`, `MWB_BEHIND` vào `merge_while_broken` (mục 4 cho phiên chọn cách ngắn hơn, câu khẳng định giữ); T12 (a), (b), (d), (f), (g) đếm thêm dòng thông báo. Chú thích jq của bộ lọc thành chú thích bash phía trên khối. Không có file ngoài mục 3.2.
- **Việc còn lại:** chờ `main-guard` của commit merge. App nhận qua đợt đồng bộ của `scripts/template-sync.sh`.
- **Cần người xem:** lần sau một PR có hai lần chạy `pr-check` trên cùng commit, `merge-if-green.sh --update` chờ lần sau thay vì từ chối.
