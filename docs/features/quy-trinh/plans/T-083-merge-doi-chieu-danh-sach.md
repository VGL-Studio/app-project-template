---
ticket: T-083
title: Khi phiên tự chạy đang chạy, merge-if-green.sh chỉ cho merge nhánh task có trong danh sách của phiên
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 6
risk_level: high
token_estimate: 8.6M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-11-09
lane:
---

# PLAN T-083: KHI PHIÊN TỰ CHẠY ĐANG CHẠY, MERGE-IF-GREEN.SH CHỈ CHO MERGE NHÁNH TASK CÓ TRONG DANH SÁCH CỦA PHIÊN

## 1. Tóm tắt

Khối kiểm phiên đang chạy của `scripts/merge-if-green.sh` thôi cho mọi nhánh `claude/T-*` đi qua. Khi danh sách trên nhánh báo cáo đang `running`, nhánh `claude/T-NNN-...` và `claude/fix-T-NNN-...` chỉ qua khi `T-NNN` có một dòng trong bảng danh sách chạy, đọc bằng đúng mẫu mà `scripts/resume-check.py` và `scripts/run-watchdog.py` dùng. Nhánh báo cáo của phiên và bản revert đúng (ADR-0025) qua như nay. Mọi kiểm tra khác giữ nguyên. `scripts/resume-check.py` và `scripts/run-watchdog.py` chỉ thêm chú thích trỏ về mẫu chung. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả: mỗi nhánh của khối kiểm một test, và với task có trong danh sách thì mỗi kiểm tra đứng trước hay sau khối này cũng một test. Task làm có người (`auto_run: false`), không qua phiên tự chạy.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn là hai bài học, cả hai `accepted` ngày 2026-11-09 (anh Nhật: "Nhận L-128 (applied theo #428), L-129, L-130, L-131, L-132, L-133."):
- L-129: người merge PR vào nhánh chính khi phiên đang `running`. #320 và #324 merge giữa RUN-20261030-01.
- L-133: nguyên nhân nằm trong script. Khối kiểm phiên coi mọi nhánh `claude/T-[0-9]*` là task của phiên.

Anh Nhật chọn cách sửa chặt ngày 2026-10-30: chặn bằng script.

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| Phạm vi 1: đối chiếu danh sách | Có phiên `running`: PR từ `claude/T-*` của task ngoài danh sách bị từ chối; task trong danh sách vẫn merge được | Mã task lấy từ tên nhánh bằng `^claude/(fix-)?(T-[0-9]+)-`, như `resume-check.py` (`^claude/(?:fix-)?(T-\d+)-`). Danh sách là các mã khớp `^\|\s*\d+\s*\|\s*(T-\d+)\s*\|` trong file `docs/runs/<RUN>.md` của nhánh báo cáo, đúng mẫu của `resume-check.py`, `run-watchdog.py`. Mẫu đọc cả file như hai script đó; chỉ bảng mục 1 có cột đầu là số và cột hai là mã ticket (mục 4.1) |
| Phạm vi 1: nhánh `fix-` | Nhánh `claude/fix-T-NNN-...` của task trong danh sách được merge | Hôm nay nhánh này bị từ chối khi có phiên (không khớp `claude/T-[0-9]*`). Plan cho qua theo ticket; vẫn phải qua `needs-human` và nháp như nay (mục 8, câu 1) |
| Phạm vi 1: nhánh báo cáo, revert | Giữ như hôm nay | Không đổi điều kiện `HEAD_REF == run_branch` và `EXACT_REVERT == yes` |
| Phạm vi 2: thông báo | Nêu mã task, mã phiên, cách làm tiếp | Giữ đầu câu `phiên <RUN> đang chạy` (bảng lỗi của `docs/ops/setup-new-app.md` và test có sẵn dò chuỗi này), thêm mã task, file danh sách, "chờ phiên kết thúc rồi gọi lại" và "việc gấp thì người dừng phiên trước" (mục 8, câu 3 về đường người tự merge) |
| Phạm vi 3: test | Mỗi nhánh của kiểm tra một test, kể cả hai phiên `running` | Mục 4, T1 tới T19 |
| Phạm vi 4: tài liệu | `docs/auto-run.md` mục 1; L-129, L-133 `applied` | Mục 3.2 |
| Phạm vi 5: nút merge trên GitHub | Plan xem đường đó, đề xuất không bắt buộc làm | Mục 3.1 E và mục 8, câu 4: không làm trong ticket này |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan:
- ADR-0010: cổng chất lượng không dùng tính năng trả phí của GitHub. Luật bảo vệ nhánh nằm ngoài tầm (mục 3.1 E).
- ADR-0025: bản revert đúng qua được kiểm tra phiên đang chạy (điểm 4). Plan giữ nguyên.
- ADR-0041: mọi kiểm tra khác giữ nguyên và đứng trước bước nhánh chậm. Plan không đổi thứ tự.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/merge-if-green.sh`, khối kiểm phiên đang chạy (trong `evaluate`, sau khối `main-broken`).**

Hôm nay, với mỗi nhánh báo cáo `claude/RUN-YYYYMMDD-NN-report`, script đọc file danh sách và lấy `status` trong front matter. Khi `running`, nó từ chối mọi PR không phải nhánh báo cáo đó, không khớp `claude/T-[0-9]*` và không phải bản revert đúng.

Sau khi sửa:
1. File danh sách đọc một lần vào biến `run_text`; `run_status` lấy từ biến đó bằng đúng lệnh `sed` hôm nay. Không đọc được file thì `run_text` rỗng và phiên coi như không chạy, như hôm nay (mục 8, câu 6). Đọc được file `running` mà không có dòng bảng nào thì mọi nhánh task bị từ chối (T7).
2. `status` khác `running` thì bỏ qua phiên đó, như nay.
3. PR là nhánh báo cáo của phiên này, hay `EXACT_REVERT == yes`, thì qua phiên này, như nay.
4. Tên nhánh khớp `^claude/(fix-)?(T-[0-9]+)-` thì `task` là nhóm 2.
   - Mẫu đặt trong một biến rồi `[[ "$HEAD_REF" =~ $task_branch_re ]]`; `BASH_REMATCH[2]` là mã task. Cách này chạy trên bash 3.2 của macOS (máy dev gọi script này) và bash 5 của CI.
   - `task` có trong bảng thì qua phiên này. Kiểm bằng `grep -qE -- "^\|[[:space:]]*[0-9]+[[:space:]]*\|[[:space:]]*${task}[[:space:]]*\|" <<<"$run_text"`. `task` chỉ gồm `T-` và chữ số, nên không cần thoát ký tự. Dấu `\|` sau mã chặn `T-001` khớp nhầm `T-0010`.
   - Không có thì từ chối:
     `TỪ CHỐI: phiên <RUN> đang chạy; task <T-NNN> (nhánh '<HEAD_REF>') không có trong bảng danh sách chạy docs/runs/<RUN>.md trên nhánh claude/<RUN>-report. Trong lúc phiên chạy chỉ merge PR của chính phiên đó (docs/auto-run.md, mục 1). Chờ phiên kết thúc rồi gọi lại; việc gấp thì người dừng phiên trước (docs/auto-run.md, mục 4, Tiếp tục phiên sau khi hết hạn mức).`
5. Nhánh khác (của người, `claude/quick-T-*`, `claude/plan-T-*`, `claude/T-NNN` không có hậu tố) bị từ chối bằng đúng câu hôm nay.

Vòng lặp vẫn xét từng phiên `running`. Có hai phiên `running` cùng lúc thì PR phải qua cả hai. Đây là cách vòng lặp đang làm với nhánh báo cáo (mục 8, câu 2). `docs/auto-run.md` mục 1 nói mỗi lúc chỉ có một phiên; hai phiên `running` chỉ xảy ra khi một danh sách cũ kẹt ở `running`, và khi đó người xem.

Chú thích của khối viết lại: PR của phiên là nhánh báo cáo, nhánh task và nhánh `fix-` của ticket có trong bảng danh sách, cùng bản revert đúng. Chú thích nêu mẫu bảng và mẫu nhánh là mẫu của `scripts/resume-check.py` và `scripts/run-watchdog.py`, đổi thì đổi cả ba. Chú thích đầu file không nhắc khối này, nên không đổi.

Chiều ngược lại: `scripts/resume-check.py` và `scripts/run-watchdog.py` mỗi file thêm một dòng chú thích ngay trên dòng `re.findall` của bảng, nói `scripts/merge-if-green.sh` đọc bảng và tên nhánh theo cùng mẫu, đổi thì đổi cả ba. Chỉ thêm chú thích, code không đổi.

**Vì sao viết mẫu bằng bash, không gọi chung một hàm Python.**
- `merge-if-green.sh` hôm nay chỉ cần `gh`, `jq`, `git`; gọi Python thêm một phụ thuộc cho cổng merge.
- Ba script cùng một mẫu, có chú thích trỏ nhau theo cả hai chiều.
- T6 kiểm các dạng dòng mà mẫu phải phân biệt.
- Mục 4.1 đã so mẫu bash với mẫu Python trên mọi danh sách chạy có trong repo.

**B. Mọi kiểm tra mà PR của task trong danh sách đi qua** (`docs/CLAUDE.md`, mục Khi viết plan). Thứ tự trong `evaluate` không đổi.

| Kiểm tra | Task trong danh sách, có phiên `running` | Test |
| --- | --- | --- |
| PR mở | Đóng thì từ chối, như nay, đứng trước khối phiên | T19 (a) |
| Đã nằm trên nhánh chính (`merged_on_main`, L-080) | Đứng trước khối phiên, không đổi: báo đã merge, mã 0 | Có sẵn, với PR của người: `reports a person's PR already on main while a run is running`. Ca này không phụ thuộc tên nhánh |
| Nháp | Từ chối | T11 |
| Nhãn `needs-human` | Từ chối (nhánh `fix-` thường mang nhãn này) | T12 |
| Base khác `main` | Từ chối, như nay, đứng trước khối phiên | T19 (b) |
| `main-broken` mở, PR không phải bản revert đúng | Từ chối, đứng trước khối phiên | T13 |
| Phiên đang chạy | Qua | T1, T3 |
| Nhánh chậm, không `--update` | Mã 3 | T1 (mã 3 là "đã qua khối phiên") |
| Nhánh chậm, `--update` | Cập nhật, chờ CI, rồi xét lại mọi điều kiện ở vòng sau | T18 (ca ngược: task ngoài danh sách bị từ chối ở vòng sau) |
| Nhánh chậm chỉ vì tài liệu, merge không cập nhật (ADR-0041) | Merge không cập nhật | Có sẵn, dữ liệu mới: `behind by docs: a running session still refuses a person's PR`, phần thứ hai (`claude/T-901-x` có trong bảng, mã 0) |
| Kiểm tra đỏ | Từ chối | T14 |
| Mọi kiểm tra xanh | Sẵn sàng (`--dry-run` mã 0) | T14 |

Task ngoài danh sách bị từ chối ngay ở khối phiên, trước bước cập nhật nhánh. Với `--update`, script không ghi gì lên GitHub (T15).

**C. Vòng chờ của `--update`.** `evaluate` chạy lại ở mỗi vòng. Phiên chuyển sang `running` trong lúc chờ thì vòng sau từ chối task ngoài danh sách (T18). Không đổi code, chỉ thêm test.

**D. Tài liệu.**
- `docs/auto-run.md` mục 1, ý 4: câu "`merge-if-green.sh` từ chối mọi PR không phải của phiên" nói rõ PR của phiên là gì: nhánh báo cáo; nhánh `claude/T-NNN-...` và `claude/fix-T-NNN-...` của ticket có trong bảng danh sách chạy; bản revert đúng. Task có plan làm ngoài phiên, kể cả trên nhánh `claude/T-NNN-...`, chờ phiên kết thúc. Nếu câu 1 của mục 8 chọn (a): câu "người không merge vào nhánh chính" của cùng ý thêm ngoại lệ PR sửa nguyên nhân của task trong danh sách, sau khi người xem và gỡ `needs-human`; đoạn về nhánh `claude/fix-T-NNN-...` (mục 3, Chuẩn bị cho người khi task dừng) ghi người merge được PR đó cả khi phiên đang chạy. Chọn (b) thì không đổi hai câu này.
- `docs/ops/setup-new-app.md`, bảng lỗi, dòng `merge-if-green.sh báo "phiên ... đang chạy"`: cột nguyên nhân thêm "nhánh task không có trong danh sách chạy của phiên". Cột việc phải làm giữ "Chờ phiên kết thúc".
- `docs/auto-run.md` mục 4, đoạn "Muốn dừng hẳn một phiên đang chờ": nói rõ đoạn này áp cho phiên đang chờ hạn mức; phiên đang chạy thật thì người dừng phiên đó trước (dừng routine hay phiên cloud), rồi mới sửa trạng thái danh sách, để lần sửa không xung đột với commit phiên đang đẩy.
- Tài liệu quy tắc viết theo ADR-0044: không mã bài học, ticket hay phiên. Chạy `scripts/history-refs.py`.

**E. Đường merge bằng nút trên GitHub (phạm vi 5 của ticket).**
- Script không chặn được đường này. Luật bảo vệ nhánh hay kiểm tra bắt buộc trên repo private cần gói trả phí, mà ADR-0010 chọn không dùng.
- Một kiểm tra báo đỏ trong `pr-check` khi có phiên `running` cũng không chặn nút merge nếu không có luật bảo vệ nhánh. Nó còn làm đỏ PR của người cả khi người chỉ muốn chờ.
- L-133 xác nhận #320 và #324 đều qua `merge-if-green.sh`, không qua nút. Plan không làm gì cho đường này; quy tắc "chỉ merge bằng `merge-if-green.sh`" của `CLAUDE.md` giữ nó (mục 8, câu 4).

Mẫu có sẵn mà plan làm theo:
- Đối chiếu nhánh với ticket của danh sách: `scripts/resume-check.py`, `scripts/run-watchdog.py`.
- Test khối phiên: hàm `merge_during_run` của `scripts/tests/run.sh`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/merge-if-green.sh` | Mục 3.1 A: đọc file danh sách một lần, đối chiếu mã task với bảng, thông báo từ chối mới, chú thích của khối |
| Sửa | `scripts/tests/run.sh` | Test T1 tới T19 của mục 4. Hàm `merge_during_run`: giữ đầu ra của script vào `$work/merge.out` thay cho `/dev/null` (như `merge_run` giữ `mg.out`), vẫn in mã thoát; nhận thêm tham số qua biến môi trường đọc trong hàm, mặc định như nay: `MDR_LIST` phần thân file danh sách của RUN-20261016-01 sau front matter (mặc định `\n\n\| 1 \| T-001 \| demo \|\n`), `MDR_OLD_STATUS` và `MDR_OLD_LIST` trạng thái và phần thân của RUN-20261015-01 (mặc định `finished`, rỗng), `MDR_DRAFT` (mặc định `false`), `MDR_STATE` (mặc định `open`), `MDR_BASE` (mặc định `main`). Hàm `merge_while_broken` nhận thêm tham số thứ tư là tên nhánh (mặc định `revert/<culprit>` như nay); file danh sách giả trong hàm giữ như nay, không có bảng. Dữ liệu của hai test có sẵn (mục 3.5) |
| Sửa | `docs/auto-run.md` | Mục 3.1 D: mục 1, ý 4; theo câu 1 của mục 8, đoạn về nhánh `claude/fix-T-NNN-...` ở mục 3; mục 4, đoạn dừng hẳn một phiên |
| Sửa | `scripts/resume-check.py` | Mục 3.1 A: một dòng chú thích trên dòng `re.findall` của bảng, trỏ `scripts/merge-if-green.sh`. Không đổi code |
| Sửa | `scripts/run-watchdog.py` | Như `resume-check.py` |
| Sửa | `docs/ops/setup-new-app.md` | Mục 3.1 D: dòng bảng lỗi "phiên ... đang chạy" |
| Sửa | `docs/lessons.md` | L-129, L-133: `applied`, dòng Áp dụng ghi số PR. Dòng Áp dụng của L-128 bỏ "chưa làm", ghi phần còn lại đã đóng bằng PR của task. Ghi trong PR của task (cùng PR, như các PR bài học trước) |
| Sửa | `docs/features/quy-trinh/plans/T-083-merge-doi-chieu-danh-sach.md` | Mục 10, nhật ký "Làm có người", ghi ở lần đẩy cuối của PR task (như T-075) |
| Sửa | `docs/features/quy-trinh/tickets/T-083-merge-doi-chieu-danh-sach.md` | Trạng thái ticket theo kết quả |

Nơi đọc file bị đổi (`grep -rn 'merge-if-green' scripts/ .github/ .claude/`):
- Skill `merge-pr`, `docs/auto-run.md` và `scripts/wait-run.sh` chỉ gọi script và đọc mã thoát. Mã thoát không đổi: ca mới từ chối bằng mã 1, như mọi lần từ chối khác.
- `.github/workflows/pr-check.yml` và `main-guard.yml` chỉ nhắc script trong chú thích và chạy `scripts/tests/run.sh`; không đổi.
- `scripts/is-exact-revert.sh` chỉ nhắc script trong chú thích.
- `docs/ops/setup-new-app.md` dò đầu câu thông báo, nên đầu câu giữ nguyên (mục 3.1 A, ý 4).
- `scripts/resume-check.py`, `scripts/run-watchdog.py`: chỉ thêm chú thích. Chúng là nguồn của mẫu.

Các file khác mà `plan-check.py` (L-020) thấy có nhắc tới file của plan; không file nào bị ảnh hưởng, không sửa:
- Nhắc `scripts/merge-if-green.sh` hay `scripts/tests/run.sh`:
  - `.github/workflows/main-guard.yml` chạy `scripts/tests/run.sh` và nhắc `merge-if-green.sh` trong chú thích. Không đọc thông báo hay mã thoát của khối phiên;
  - `scripts/main-guard-verified.sh` chỉ nhắc hai file trong chú thích;
  - `scripts/local-setup.sh` nhắc `scripts/tests/run.sh` trong chú thích;
  - `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` nhắc `scripts/tests/run.sh` trong chú thích, không chạy nó.
- Nhắc `docs/auto-run.md` (dẫn mục hay bước khác, hay dẫn chung cả file). Plan chỉ đổi câu trong mục 1, ý 4, không đổi tên mục hay số bước nào:
  - workflow `.github/workflows/device-session.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`;
  - script `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/failure-kind.sh`, `scripts/main-guard-status.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/run-check.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/status.py`, `scripts/token-usage.py`, `scripts/ui-test-verdict.sh`;
  - tệp log `scripts/tests/playbook/PB-17.log`.
- Nhắc `docs/ops/setup-new-app.md`: `.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`, `scripts/java-check.sh`, `scripts/new-app.sh`, `scripts/setup-check.sh`. Chúng dẫn các mục khác của tài liệu; plan chỉ đổi một ô trong bảng lỗi, không đổi tên mục.
- Nhắc `docs/lessons.md`:
  - `.github/ISSUE_TEMPLATE/from-app.yml`, `scripts/new-app.sh` dẫn cả file;
  - `scripts/status.py` đọc trạng thái bài học. Hai bài học chuyển sang `applied`, một trạng thái nó đã biết.
- Nhắc `scripts/resume-check.py` hay `scripts/run-watchdog.py`: `.github/workflows/resume-run.yml`, `.github/workflows/run-watchdog.yml` chạy hai script này. Plan chỉ thêm chú thích, nên lệnh chạy và kết quả không đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Mẫu đã chạy thử trên mọi danh sách chạy của repo (mục 4.1). Cách đọc `BASH_REMATCH` chạy trên bash 3.2 và 5.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Hai test có sẵn của `scripts/tests/run.sh` dùng nhánh `claude/T-*` khi có phiên `running` mà file danh sách giả chưa có bảng. Chúng đỏ chỉ vì dữ liệu chưa có phần plan thêm (ADR-0029), nên plan thêm dòng bảng vào dữ liệu, câu khẳng định giữ nguyên.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Mọi nhánh `claude/T-*` qua khối phiên | Chỉ task có trong bảng danh sách | `merge-if-green lets the session's task PR through` (`claude/T-001-loading`, mong 3) | Giữ câu và mong 3. Dữ liệu: file danh sách giả có `\| 1 \| T-001 \| demo \|` (mặc định mới của `merge_during_run`) |
| Như trên | Như trên | `merge-if-green, behind by docs: a running session still refuses a person's PR` (`claude/T-901-x`, mong `1 0`) | Giữ câu và `1 0`. Dữ liệu: file danh sách giả trong `nu_edit` thêm `\n\n\| 1 \| T-901 \| demo \|\n` |
| Nhánh `claude/fix-T-*` bị từ chối khi có phiên | Qua khi task có trong bảng | Không có test có sẵn (test `refuses a PR labelled needs-human` dùng phiên `finished`, chặn bởi nhãn) | Câu mới: T3 |
| Nhánh `claude/T-001` (không hậu tố) qua khối phiên | Bị từ chối | Không có | Câu mới: T8 |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, cạnh các test `merge-if-green` có sẵn, dùng `gh` giả (`scripts/tests/fake-gh`). "Đỏ trước code" là kết quả dự kiến trên `merge-if-green.sh` hôm nay; phiên kiểm lại khi viết test.

Hàm `merge_during_run` (in mã thoát; mã 3 nghĩa là PR đã qua khối phiên rồi dừng ở bước nhánh chậm của dữ liệu; đầu ra của script ở `$work/merge.out`, các test đếm dòng trên file này). Phiên RUN-20261016-01 có bảng `| 1 | T-001 | demo |`, trừ khi test ghi khác (`MDR_LIST`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Task trong danh sách qua | Test script | T1 `merge-if-green lets the session's task PR through`: `claude/T-001-loading`, `running`, mong 3. Test có sẵn, dữ liệu mới (mục 3.5). Qua cả trước code |
| Task ngoài danh sách bị từ chối | Test script | T2 `merge-if-green refuses a task PR not in the running list`: `claude/T-099-other`, `running`, mong `1 1 1`. Ba phần: mã thoát; số dòng của `$work/merge.out` chứa `phiên RUN-20261016-01 đang chạy; task T-099`; số dòng chứa `docs/runs/RUN-20261016-01.md`. Đỏ trước code (`3 0 0`) |
| Nhánh `fix-` của task trong danh sách qua | Test script | T3 `merge-if-green lets a fix branch of a listed task through`: `claude/fix-T-001-x`, `running`, không nhãn, mong 3. Đỏ trước code (1) |
| Nhánh `fix-` của task ngoài danh sách bị từ chối | Test script | T4: `claude/fix-T-099-x`, `running`, mong 1. Qua trước code (đối chứng cho T3) |
| Không có phiên `running` thì không đổi | Test script | T5: `claude/T-099-other`, RUN-20261016-01 `finished`, mong 3. Qua trước code |
| Đọc bảng đúng mẫu | Test script | T6, ba ca. (a) Mã T-099 chỉ nằm ở dòng của bảng khác (`\| T-099 \| Đã merge \| ... \|`, `\| Chặn \| T-099 \| ... \|`), nhánh `claude/T-099-other`: mong 1, đỏ trước code (3). (b) Dòng có nhiều dấu cách `\|  2  \|  T-001  \| demo \|` là dòng duy nhất, nhánh `claude/T-001-loading`: mong 3. (c) Bảng chỉ có `T-0010`, nhánh `claude/T-001-x`: mong 1, đỏ trước code (3). Đầu ra một dòng `a b c`, mong `1 3 1` |
| File danh sách không có bảng | Test script | T7: thân file chỉ có front matter `running` (như dữ liệu cũ), nhánh `claude/T-001-loading`, mong 1. Đỏ trước code (3). File danh sách `running` không có bảng thì từ chối, chiều an toàn. Đọc file lỗi là ca khác, giữ như nay (mục 8, câu 6) |
| Nhánh không phải nhánh task | Test script | T8: bảng có T-001; ba nhánh `claude/quick-T-001-x`, `claude/plan-T-001-x`, `claude/T-001`. Mong `1 1 1`; trước code `1 1 3` |
| Hai phiên `running` | Test script | T9, ba ca. RUN-20261015-01 cũng `running`, bảng `\| 1 \| T-002 \| demo \|`. (a) `claude/T-001-loading`: mong 1 và `$work/merge.out` nêu `phiên RUN-20261015-01 đang chạy; task T-001`, đỏ trước code. (b) Bảng của cả hai phiên có T-001: mong 3. (c) Nhánh `claude/RUN-20261016-01-report`: mong 1, như nay. Đầu ra `a b c`, mong `1 3 1` |
| Bản revert đúng qua khi có phiên | Test script | Có sẵn: `merge-if-green lets the exact revert through while a run is running`, `... still refuses a revert branch that is not checked ...`. Không đổi |
| Nhánh báo cáo qua khi có phiên | Test script | Có sẵn: `merge-if-green lets the session's report PR through`. Không đổi; dữ liệu mặc định mới có bảng, không ảnh hưởng |
| Thông báo cũ cho PR của người | Test script | Có sẵn: `merge-if-green: a running session on page 2 of the branches still blocks other PRs` (dò `phiên RUN-20261016-01 đang chạy`). Không đổi |
| Task trong danh sách vẫn qua mọi kiểm tra khác: nháp | Test script | T11: `MDR_DRAFT=true merge_during_run claude/T-001-loading running`, mong 1 và `$work/merge.out` chứa `bản nháp`. Qua trước code (nháp đứng trước khối phiên) |
| ... nhãn `needs-human` | Test script | T12: `claude/fix-T-001-x`, `running`, nhãn `needs-human`, mong 1 và `$work/merge.out` chứa `needs-human`. Qua trước code |
| ... `main-broken` | Test script | T13: `merge_while_broken "$one_issue" "$work/bin/revert-no" running claude/T-099-x`; file danh sách giả trong hàm không có bảng, nên T-099 không có trong danh sách. Mong 1 và `$work/broken.out` chứa `main-broken`. Qua trước code. Đối chứng `revert-yes` cùng nhánh: mong 3, chứng minh `EXACT_REVERT` cho một nhánh task ngoài danh sách qua khối phiên. Đầu ra `a b c`, mong `1 1 3` |
| ... CI | Test script | T14, dựng bằng `merge_routes claude/T-001-x '{"behind_by": 0}' <runs>`, rồi sửa dữ liệu bằng một lệnh `python3 -I -c` như test "page 2": thêm nhánh báo cáo vào `branches?per_page=100&page=1` và route file danh sách `running` có bảng T-001. (a) `$green_runs`, `--dry-run`: mong 0. (b) `build-and-test` kết thúc `failure`: mong 1 và dòng `có kiểm tra không qua`. Đầu ra `a b`, mong `0 1 1`. Trước code (a) cũng 0: khối phiên cho mọi `claude/T-*` qua |
| Task ngoài danh sách: không ghi gì lên GitHub | Test script | T15: như T14 nhưng nhánh `claude/T-099-x`, `behind_by` 2, gọi `merge_run 12 --update`: mong `1 0`. Phần hai là số lệnh ghi trong `$work/mg.json.writes`. Đỏ trước code (script cập nhật nhánh) |
| Dữ liệu `nu_edit` có sẵn | Test script | T16: test có sẵn ở mục 3.5 giữ `1 0` với dữ liệu mới. Thêm ca `claude/T-902-x` (không có trong bảng) cùng dữ liệu: mong `1` và số lệnh ghi 0. Đỏ trước code |
| Thông báo nói cách làm tiếp | Test script | T17: `$work/merge.out` của T2 chứa `Chờ phiên kết thúc rồi gọi lại`, `docs/auto-run.md, mục 1` và `người dừng phiên trước`. Đỏ trước code |
| Phiên bắt đầu trong lúc `--update` chờ | Test script | T18: `merge_routes claude/T-099-x '{"behind_by": 2}' ...`, route `branches?per_page=100&page=1` là `{"__sequence__": [[{"name": "main"}], [{"name": "main"}, {"name": "claude/RUN-20261016-01-report"}]]}`, cộng route file danh sách `running` có bảng T-001. Lần một chưa có phiên nên cập nhật nhánh; lần hai thấy phiên và từ chối. Route `branches?...` có sẵn trong `merge_routes` được thay bằng một lệnh `python3 -I -c` như T14. Gọi `UPDATE_TIMEOUT=1 merge_run 12 --update` (hạn chờ ngắn để ca trước code không chờ 5 giây). Mong `1 1 1`: mã thoát; số dòng `task T-099` trong `$work/mg.out`; số dòng `PUT pulls/12/update-branch` trong `$work/mg.json.writes` (chứng minh vòng một đã cho qua và cập nhật nhánh). Đỏ trước code (`2 0 1`: lần hai cho qua khối phiên, rồi chờ "GitHub chưa cập nhật xong" vì head không đổi, tới hết giờ). Dòng `ĐÃ CẬP NHẬT` không chứa `task T-099`, nên phần hai là đúng một dòng. `__sequence__` của `fake-gh` giữ phần tử cuối khi hết danh sách (`scripts/tests/fake-gh`, chú thích của `__sequence__`) |
| Task trong danh sách vẫn qua mọi kiểm tra khác: PR mở, base | Test script | T19: `claude/T-001-loading`, `running`. (a) `MDR_STATE=closed`: mong 1, `$work/merge.out` chứa `không ở trạng thái mở`. (b) `MDR_BASE=other`: mong 1, chứa `nhắm vào 'other'`. Đầu ra `a b`, mong `1 1`. Qua trước code (hai kiểm tra đứng trước khối phiên) |
| `docs/auto-run.md`, `setup-new-app.md` đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc mục 1 ý 4 và dòng bảng lỗi. `scripts/history-refs.py` qua |
| Toàn bộ | Test script | `scripts/tests/run.sh` trên CI (`pr-check`, rồi `main-guard` sau merge). Trên máy dev macOS có 6 test đỏ sẵn trên nhánh chính (5 `pr-check: ... order check`, 1 `wait-run: a draft PR ...`); phiên so danh sách `FAIL` với nhánh chính, không coi là lỗi của task |

Các test có số chưa dùng (T10) để trống; phiên đánh số lại liền nhau được.

**Ngoại lệ viết test trước:** sửa `docs/auto-run.md`, `docs/ops/setup-new-app.md`, `docs/lessons.md`, nhật ký plan và ticket: chỉ là tài liệu. Dòng chú thích ở `scripts/resume-check.py` và `scripts/run-watchdog.py`: chỉ chú thích, code không đổi.

### 4.1. Chạy thử

Không cần (E dưới 2). Một lần so mẫu ngày 2026-11-09, trên nhánh chính `a01d63e`, để chọn mẫu bash ở mục 3.1 A:

- **Đã chạy thử:** với mỗi file `docs/runs/RUN-*.md` (22 file), so hai tập ticket. Một tập từ regex Python của `resume-check.py` (`re.findall(r"^\|\s*\d+\s*\|\s*(T-\d+)\s*\|", text, re.M)`). Tập kia từ `grep -oE '^\|[[:space:]]*[0-9]+[[:space:]]*\|[[:space:]]*T-[0-9]+[[:space:]]*\|'` rồi `grep -oE 'T-[0-9]+'`.
- **Lệnh:** vòng `for f in docs/runs/RUN-*.md` chạy hai lệnh trên, so chuỗi đã sắp xếp; bash 3.2.57 của macOS.
- **Kết quả:** `files=22 diff=0`. Hai mẫu cho cùng tập ticket trên mọi danh sách có trong repo.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Plan sửa `scripts/merge-if-green.sh`, file được bảo vệ, là cổng merge của mọi phiên. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một khối của một script, test của nó, một dòng chú thích ở hai script Python, hai tài liệu |
| B. Độ rõ của yêu cầu | 1 | Ticket nêu rõ cách sửa và các ca test. Còn bốn điểm người chọn: nhánh `fix-`, hai phiên, đường người tự merge, đọc file danh sách lỗi (mục 8, câu 1, 2, 3, 6) |
| C. Khả năng kiểm chứng tự động | 0 | Mọi nhánh của khối có test với `gh` giả. Không có phần nào chỉ kiểm được trên GitHub thật |
| D. Ảnh hưởng chéo | 3 | Mọi phiên tự chạy dựa vào khối này để merge task của mình. Đọc bảng sai thì phiên không merge được task nào, hay lại để lọt PR ngoài phiên |
| E. Độ mới của kỹ thuật | 1 | Biến thể của mẫu có sẵn: mẫu bảng của `resume-check.py`, `run-watchdog.py` viết lại bằng `grep -E`; `BASH_REMATCH` lần đầu dùng trong script này |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `merge-if-green.sh`, mọi lần merge khi có phiên `running`. Không có phiên thì khối không chạy, nên mọi merge khác không đổi.
- Task khác có thể xung đột: không plan `approved` hay `draft` nào khác đang sửa `merge-if-green.sh`. T-094 (`done`) và T-095 (`done`) đã merge.
- Cách làm:
  - Làm có người (`auto_run: false`), không đưa vào danh sách chạy, như T-075.
  - Nhánh `claude/T-083-merge-doi-chieu-danh-sach`, làm khi không có phiên `running`. Script cũ còn cho mọi `claude/T-*` qua khi có phiên, nên chính PR này cũng không được merge giữa phiên.
  - PR của task merge bằng `merge-if-green.sh` bản cũ, chạy từ thư mục chính đứng ở nhánh chính, như mọi PR. Trước đó chạy thêm bản mới từ worktree của task với `--dry-run` trên chính PR đó. Lúc không có phiên, khối phiên không chạy; lần chạy này chỉ để thấy bản mới chạy được với GitHub thật (gọi API, đọc danh sách nhánh), điều mà `gh` giả của test không thấy.
- Chuyển tiếp:
  - Danh sách chạy đang `running` lúc merge (nếu có) vẫn đúng: bảng của nó đã có mọi task của phiên.
  - Worktree tạo trước khi merge chạy bản cũ tới khi gộp nhánh chính (`docs/auto-run.md`, bước 9).
- App nhận qua `scripts/template-sync.sh`. App chưa đồng bộ vẫn cho mọi `claude/T-*` qua.
- Plan không sửa `.github/workflows/`, nên không cần `push-probe.sh`.
- Đề xuất thứ tự: độc lập, làm ngay khi không có phiên nào chạy.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu nhánh chính. Dừng, cần plan phiên bản mới, khi:
  - khối kiểm phiên của `merge-if-green.sh` không còn dạng của mục 3.1 A (vòng qua nhánh báo cáo, `sed` lấy `status`, điều kiện `claude/T-[0-9]*`);
  - mẫu bảng của `resume-check.py` hay `run-watchdog.py` khác `^\|\s*\d+\s*\|\s*(T-\d+)\s*\|`;
  - mẫu nhánh của chúng khác `^claude/(?:fix-)?(T-\d+)-`.

  Chỉ lệch số dòng thì theo ADR-0032/L3.
- Một test có sẵn khác của `merge-if-green` đổi kết quả, ngoài hai test ở mục 3.5: dừng.
- Có phiên `running` lúc định merge PR của task: dừng, chờ phiên kết thúc.
- Cần sửa `.github/workflows/`, `scripts/check.sh` hay `scripts/release.sh`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Nhánh `claude/fix-T-NNN-...` của task trong danh sách được merge khi phiên đang chạy.**
   - Hôm nay nhánh này bị từ chối khi có phiên.
   - Phiên tạo nó ở dạng nháp có nhãn `needs-human` khi task dừng vì nguyên nhân ngoài plan. Người gỡ nhãn rồi mới merge được.
   - (a) Cho qua như ticket ghi: người sửa nguyên nhân xong thì merge ngay, task dừng có thể chạy lại ở phiên tiếp tục. Nhánh chính đổi giữa phiên, nhưng do chính người quyết. Đề xuất: (a).
   - (b) Giữ như hôm nay: chỉ nhánh `claude/T-NNN-...`.

   Cùng câu: nhánh `claude/T-NNN` không có hậu tố `-<tu-khoa>` bị từ chối khi có phiên, như cách `resume-check.py` đọc tên nhánh. Quy ước của `docs/CLAUDE.md` luôn có hậu tố.
2. **Hai phiên `running` cùng lúc.**
   - (a) PR phải qua từng phiên, như vòng lặp đang làm với nhánh báo cáo: task chỉ có trong một danh sách bị từ chối. Đề xuất: (a). Hai phiên `running` chỉ xảy ra khi một danh sách cũ kẹt ở `running`; từ chối buộc người xem danh sách đó.
   - (b) Qua khi task có trong danh sách của ít nhất một phiên.
3. **Không có đường để người tự merge có chủ đích giữa phiên.**
   - Ticket nhắc "người tự merge có chủ đích".
   - (a) Không thêm biến hay tham số nào cho qua. Thông báo nói chờ phiên kết thúc. Việc gấp thì người xử lý phiên trước: chờ, hay kết thúc phiên theo `docs/auto-run.md`. Đề xuất: (a). Mọi cửa vượt là chỗ hở L-133 mở lại.
   - (b) Thêm biến môi trường, ví dụ `MERGE_DURING_RUN=<số PR>`, có test riêng.
4. **Nút merge trên GitHub (phạm vi 5 của ticket).**
   - (a) Không làm (mục 3.1 E). L-129 có đủ cách sửa khi T-083 merge: cả hai PR của sự cố đều đi qua `merge-if-green.sh`. Đề xuất: (a).
   - (b) Ticket mới cho một kiểm tra của `pr-check` báo đỏ khi có phiên `running` mà PR không thuộc phiên. Không chặn được nút merge khi thiếu luật bảo vệ nhánh (ADR-0010).
5. **Ghi L-129, L-133 `applied` trong chính PR của task** (mục 3.2), như các PR bài học trước, không chờ sau merge. Đề xuất: đồng ý.
6. **Không đọc được file danh sách của một nhánh báo cáo** (lỗi mạng, lỗi API).
   - Hôm nay script coi phiên đó như không chạy, nên mọi PR qua khối phiên. Ticket không nhắc ca này.
   - (a) Giữ như nay, ngoài phạm vi ticket. Đề xuất: (a). Nhánh báo cáo cũ của các phiên đã xong còn trên GitHub; từ chối khi đọc lỗi thì một lỗi tạm của bất kỳ nhánh nào chặn mọi merge.
   - (b) Từ chối khi đọc lỗi, thêm test. Plan lên phiên bản 2.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật (nhóm một dev: người duyệt cũng là người duyệt chéo, `risk-scoring.md` mục 3)
- **Ghi chú:**
  - AI ghi theo lời anh Nhật ngày 2026-11-09, trong phiên có người: "Duyệt T-083".
  - Lời duyệt trả lời bản plan ở commit ad6bff0 (PR #430), sau hai vòng rà soát độc lập. Lời duyệt không nêu riêng từng câu, nên mục 8 theo đề xuất của plan, như AI đã trình ngay trước lời duyệt:
    - câu 1 (a): cho qua nhánh `fix-` của task trong danh sách;
    - câu 2 (a): có hai phiên `running` thì PR phải qua từng phiên;
    - câu 3 (a): không thêm cửa vượt;
    - câu 4 (a): không làm gì cho nút merge trên GitHub;
    - câu 5: ghi L-129, L-133 `applied` trong PR của task;
    - câu 6 (a): đọc file danh sách lỗi thì giữ như nay.
  - Task làm có người (`auto_run: false`), không qua phiên tự chạy, khi không có phiên `running`.

## 10. Nhật ký thực hiện

### Làm có người 2026-11-09, phiên bản plan 1

- **Kết quả:** merge bằng PR #431 (CI của PR này), sau lời "merge #431" của anh Nhật.
- **Nhánh, PR:** `claude/T-083-merge-doi-chieu-danh-sach`, https://github.com/VGL-Studio/app-project-template/pull/431. Commit test `d1c1e56`, code `dfbb791`.
- **Đã làm:** mục 3.1 A tới D. Khối kiểm phiên của `merge-if-green.sh` đọc file danh sách một lần; nhánh `claude/T-NNN-...`, `claude/fix-T-NNN-...` qua khi `T-NNN` có dòng trong bảng; thông báo từ chối mới. Một dòng chú thích ở `resume-check.py`, `run-watchdog.py`. `docs/auto-run.md` mục 1 ý 4, đoạn PR sửa nguyên nhân, đoạn dừng hẳn phiên; bảng lỗi của `docs/ops/setup-new-app.md`. L-128, L-129, L-133 trong `docs/lessons.md`.
- **Test viết trước:** T1 tới T19 trong `scripts/tests/run.sh` (vài ca gộp một dòng `expect`). 10 test đỏ đúng lý do trên code cũ, khớp "đỏ trước code" của mục 4 (T2 `3 0 0`, T3 `1`, T6 `3 3 3`, T7 `3`, T8 `1 1 3`, T9 `3 0 3 1`, T15 `2 1`, T16 `0 2`, T17, T18 `2 0 1`); các test còn lại qua cả trước code như plan ghi. Rà soát độc lập chạy lại test của nhánh với script của nhánh chính và xác nhận. File test không đổi sau commit test. Hai test có sẵn chỉ đổi dữ liệu (mục 3.5, ADR-0029).
- **Rà soát độc lập:** 0 chặn, 1 nên sửa (thêm các file ghi ở lần đẩy cuối: bài học, nhật ký, ticket; đã thêm), 8 ghi chú, ghi trong mô tả PR.
- **Kết quả CI:** `pr-check` xanh trên `dfbb791` (lần chạy 42582068861); lần đẩy cuối chạy lại. `scripts/tests/run.sh` trên máy dev: 6 test đỏ sẵn của nhánh chính (macOS), giống trước và sau.
- **Lệch so với plan:** ADR-0032/L4: T14 dùng `$code_runs` thay `$green_runs` (cùng kết quả `--dry-run` 0); T14, T15, T18 sửa dữ liệu bằng hàm `mrun_session` (gọi `nu_edit`) thay một lệnh `python3 -I -c`. Không có file ngoài mục 3.2.
- **Việc còn lại:** chờ `main-guard` của commit merge. App nhận qua đợt đồng bộ của `scripts/template-sync.sh`.
- **Cần người xem:** phiên tự chạy kế tiếp merge được task của mình (thông báo `ĐÃ MERGE` cho mỗi task trong báo cáo phiên).
