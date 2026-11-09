---
ticket: T-094
title: merge-if-green.sh xét "main-guard có chạy không" theo paths-ignore, không cảnh báo sai khi xóa nhánh; hai chỗ hở của pr_sim
status: done                 # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 7
risk_level: high
token_estimate: 22.2M
blocking_points: [ci]
run_order:
reviewer: Nhật
cross_reviewer: Nhật
approved_at: 2026-11-08
lane:
---

# PLAN T-094: MERGE-IF-GREEN.SH XÉT "MAIN-GUARD CÓ CHẠY KHÔNG" THEO PATHS-IGNORE, KHÔNG CẢNH BÁO SAI KHI XÓA NHÁNH; HAI CHỖ HỞ CỦA PR_SIM

## 1. Tóm tắt

`scripts/main-guard-verified.sh` thêm câu hỏi thứ ba, `--guard-ignores`: mọi đường dẫn đều nằm trong `paths-ignore` của `main-guard.yml` (`docs/**`, `**/*.md`) không. Bước 3 của `docs_only_behind` trong `scripts/merge-if-green.sh` dùng câu này thay cho `--docs-only`, nên PR chỉ gồm file mà `main-guard` bỏ qua luôn được cập nhật nhánh, kể cả khi có file `docs/` mà Gradle đọc. Sau merge, lệnh xóa nhánh thất bại thì script đọc lại nhánh tối đa 3 lần, mỗi lần cách 5 giây, và chỉ cảnh báo khi nhánh còn sau mọi lần đọc. Hai hàm của `scripts/tests/run.sh` hết chỗ hở: `pr_sim` báo `ERROR mẫu chưa hỗ trợ` cho mọi dòng lạ trong khối `filters`; `pr_filter_check` đọc hết khối `run`, kể cả sau dòng trống. Kiểm bằng test của `scripts/tests/run.sh` với `gh` giả, mỗi kiểm tra có sẵn của `merge-if-green.sh` mà ca mới đi qua có một test.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn gồm:
- báo cáo RUN-20261108-01 (điểm nên sửa của T-077);
- bài học L-149 và L-152 (`accepted`);
- ghi chú rà soát T-076 ở RUN-20261101-02.

Anh Nhật chọn ngày 2026-11-08: "Chọn (a): ticket T-094 gộp L-149 và hai ghi chú pr_sim".

| Điểm của ticket | Tiêu chí (định nghĩa hoàn thành) | Cách hiểu của AI |
| --- | --- | --- |
| 1. `docs_only_behind` bước 3 | PR chỉ đổi file trong `paths-ignore`, có file của `GRADLE_READ_DOCS`, được cập nhật nhánh trước khi merge; PR code vẫn merge không cập nhật như T-077 | Chỉ đổi câu hỏi thứ hai (danh sách file của PR). Câu hỏi thứ nhất (file nhánh chính đổi từ điểm tách) giữ `--docs-only`: commit merge phải được `main-guard` kiểm bằng cách `tests`, nên nhánh chính không được có file Gradle đọc. `docs_only_merge` (L-080) cũng giữ `--docs-only` |
| 2. Xóa nhánh | Không còn cảnh báo "không xóa được nhánh" khi GitHub đã tự xóa nhánh | Lệnh xóa thất bại (proxy của phiên cloud từ chối, L-149) thì đọc lại nhánh vài lần trong khoảng ngắn. Nhánh mất ở bất kỳ lần đọc nào thì không in gì; còn sau mọi lần thì cảnh báo như nay |
| 3. `pr_sim` | Báo lỗi với dòng mẫu chưa hỗ trợ | Mọi dòng không trống, không chú thích, trong khối `filters: \|`, mà không phải tên bộ lọc (`code:`) hay mẫu trong nháy đơn (`- '...'`), đều là `ERROR mẫu chưa hỗ trợ: <dòng>`. Khối kết thúc ở dòng đầu tiên thụt vào không sâu hơn dòng `filters: \|` |
| 4. `pr_filter_check` | Đọc hết khối `run` | Dòng trống (hay chỉ có dấu cách) trong khối được giữ thành dòng trống; khối kết thúc ở dòng không trống đầu tiên thụt vào ít hơn 10 dấu cách |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan:
- ADR-0041: merge không cập nhật khi nhánh chính chỉ đổi tài liệu. Mục 2 nói `merge-if-green.sh` không tự viết lại quy tắc mà hỏi `main-guard-verified.sh`; plan giữ điều đó bằng câu hỏi mới.
- ADR-0039: cách `main-guard` chọn `full`, `tests`, `skip`; không đổi.
- ADR-0010, ADR-0025: cổng merge, nhánh chính đỏ.

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/main-guard-verified.sh`: câu hỏi `--guard-ignores`.**
- Hàm mới `ignored_reason`: đọc đường dẫn ở stdin (bỏ dòng trống). In đường dẫn đầu tiên không nằm trong `paths-ignore` của `main-guard.yml`, tức không phải `docs/*` và không phải `*.md`. In rỗng khi mọi đường dẫn đều nằm trong đó.
  - `[[ ]]` của bash cho `*` khớp cả `/`, nên `*.md` khớp `**/*.md` của GitHub, kể cả `README.md` ở gốc, đúng như `docs_only_reason` đang dùng.
- `docs_only_reason` giữ nguyên chữ và kết quả.
- Lệnh `--guard-ignores` có cùng dạng với `--docs-only`:
  - đúng một tham số, không thì in `no` kèm dòng cách dùng ở stderr;
  - in `yes` hay `no` ở stdout, lý do ở stderr, luôn thoát 0;
  - danh sách rỗng là `yes`.
- Chú thích đầu file thêm câu hỏi thứ ba và hai câu:
  - "`main-guard` có chạy trên một commit không" là câu hỏi này;
  - "`main-guard` kiểm commit đó thế nào" là `--docs-only` và cách chọn.

**B. `scripts/merge-if-green.sh`, `docs_only_behind`.**
- Lệnh `mgv_ask --docs-only <<<"$pr_files_list"` (dòng 169 hôm nay) đổi thành `mgv_ask --guard-ignores <<<"$pr_files_list"`. Chú thích ngay trên dòng đó nói rõ đây là quy tắc của `paths-ignore`, không phải quy tắc `tests` của `main-guard`.
- Chỉ đáp `no` mới đi tiếp. Mọi đáp khác cập nhật nhánh, với `BEHIND_REASON="không đọc được PR có chỉ đổi file mà main-guard bỏ qua không"`. Đáp khác là: rỗng, `full` của một `main-guard-verified.sh` cũ không biết câu hỏi này, hay chữ lạ. Cập nhật nhánh là chiều an toàn; sai chiều ngược lại mới mở chỗ hở.
- Đáp `yes` thì giữ nguyên lý do `PR chỉ đổi file mà main-guard bỏ qua`.
- Chú thích đầu file (đoạn ADR-0041) thêm: PR chỉ gồm file mà `paths-ignore` của `main-guard.yml` bỏ qua thì luôn cập nhật nhánh.

Mọi kiểm tra có sẵn mà ca mới (PR chỉ gồm file `paths-ignore` bỏ qua, nhánh chậm, nhánh chính chỉ đổi tài liệu) đi qua (`docs/CLAUDE.md`, mục Khi viết plan):

| Kiểm tra | Ca mới | Test |
| --- | --- | --- |
| PR `closed`, nháp, `needs-human`, base khác, `main-broken`, phiên `running` | Chặn như nay, trước bước cập nhật | T8 |
| `merged_on_main` (L-080) | Chạy đầu `evaluate`, trước mọi kiểm tra; ca mới không đổi nó | Test 19, 27 có sẵn |
| Nhánh chậm, không có `--update` (cả `--dry-run`) | Mã 3, như PR code chậm vì code | T4 |
| `--update`: `update-branch` | Cập nhật | T5 |
| `update-branch` bị GitHub từ chối | Từ chối (mã 1), như nay | T20 (chưa có test có sẵn) |
| "GitHub chưa cập nhật xong" (head chưa đổi) | Chờ như nay | T21 (chưa có test có sẵn) |
| `MAX_UPDATES` | Từ chối sau 3 lần, như nay | T22 (chưa có test có sẵn) |
| CI trên head mới: đỏ / chưa xong / `skipped` | Từ chối / chờ (mã 2) / chờ | T9, T19; `skipped` theo câu có sẵn của `check_one` |
| Merge ghim vào head đã kiểm, xóa nhánh | Như nay | T5 |

**C. `scripts/merge-if-green.sh`, bước xóa nhánh sau merge.**
- Thêm hai cấu hình ở đầu file:
  - `BRANCH_GONE_TRIES="${MERGE_BRANCH_TRIES:-3}"`;
  - `BRANCH_GONE_WAIT_SECONDS="${MERGE_BRANCH_WAIT_SECONDS:-5}"` (test đặt 0).
- Lệnh `DELETE` thất bại thì đọc `git/ref/heads/$HEAD_REF` tối đa `BRANCH_GONE_TRIES` lần:
  - lần đọc thất bại (404 hay lỗi khác, như nay) nghĩa là nhánh đã mất: dừng, không cảnh báo;
  - lần đọc thành công mà chưa phải lần cuối thì `sleep "$BRANCH_GONE_WAIT_SECONDS"` rồi đọc lại;
  - còn sau lần cuối thì in đúng dòng `CHÚ Ý: không xóa được nhánh ...` như nay.
- Lệnh `DELETE` thành công thì không đọc lần nào.
- Chú thích của khối nói lý do: GitHub tự xóa nhánh vài giây sau merge, còn lệnh xóa của phiên cloud có thể bị proxy từ chối.

**D. `scripts/tests/fake-gh`.**
- Một phần tử của `{"__sequence__": [...]}` có thể là `{"__error__": "<thông báo>"}`. Khi tới lượt phần tử đó, `gh` giả in thông báo ra stderr và thoát 1, như một lần đọc lỗi.
- Test cần cách này để nhánh "còn ở lần đọc đầu, mất ở lần thứ hai". Route không có `__error__` chạy như cũ.

**E. `scripts/tests/run.sh`, `pr_sim`.**
- Vòng đọc khối `filters` ghi lại độ thụt của dòng `filters: |`.
- Một dòng không trống thụt không sâu hơn mức đó thì kết thúc khối (khóa anh em trong `with:`).
- Trong khối, dòng nào không phải chú thích, không phải tên bộ lọc và không phải mẫu `- '...'` của một bộ lọc thì in `ERROR mẫu chưa hỗ trợ: <dòng đã bỏ khoảng trắng hai đầu>` và dừng, như mẫu có ký tự lạ hôm nay. Mẫu nằm trước mọi tên bộ lọc cũng là dòng như vậy.

**F. `scripts/tests/run.sh`, `pr_filter_check`.**
- Trong khối `run`, awk in dòng chỉ có dấu cách thành dòng trống và đọc tiếp.
- Khối kết thúc ở dòng không trống đầu tiên không bắt đầu bằng 10 dấu cách, như nay.

**G. `scripts/tests/run.sh`, hàm phụ trợ `merge_run`.**
- Thêm `MERGE_BRANCH_WAIT_SECONDS=0`, để các test có nhánh còn sau lệnh xóa không phải chờ 10 giây thật.
- Thay đổi này vào commit test của bước 3, cùng phần `__error__` của `fake-gh` (mục D): test viết trước khi đó đỏ vì script chưa đọc lại nhánh, không đỏ vì `gh` giả chưa biết `__error__`.

Mẫu có sẵn mà plan làm theo:
- `--docs-only` và `--gate-ran`: dạng câu hỏi, test `mgv_docs_only`;
- vòng đọc lại `mergeable` (`MERGEABLE_TRIES`, `MERGEABLE_WAIT_SECONDS`): dạng vòng đọc lại nhánh;
- `__sequence__` của `fake-gh`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/main-guard-verified.sh` | Mục A: `ignored_reason`, lệnh `--guard-ignores`, chú thích đầu file |
| Sửa | `scripts/merge-if-green.sh` | Mục B: `docs_only_behind` bước 3 hỏi `--guard-ignores`, chỉ `no` mới đi tiếp. Mục C: đọc lại nhánh sau lệnh xóa thất bại. Chú thích đầu file |
| Sửa | `scripts/tests/fake-gh` | Mục D: phần tử `__error__` của `__sequence__` |
| Sửa | `scripts/tests/run.sh` | Mục E, F, G và các test của mục 4; chú thích trên `mgv_docs_only` ("asks main-guard-verified.sh two questions") thành ba câu hỏi |
| Sửa | `docs/adr/0041-merge-khong-cap-nhat-khi-main-chi-doi-tai-lieu.md` | Thêm một dòng ghi chú sau tiêu đề, như ghi chú của ADR-0043. Nội dung: `merge-if-green.sh` hỏi thêm `--guard-ignores` cho danh sách file của PR, vì việc `main-guard` có chạy hay không do `paths-ignore` quyết; quyết định của ADR giữ nguyên |
| Sửa | `docs/lessons.md` | L-149, L-152: `applied` kèm số PR, sau khi PR merge (`docs/auto-run.md`, mục 6, ý 3; phiên ghi trên nhánh báo cáo) |
| Sửa | `docs/features/quy-trinh/plans/T-094-merge-if-green-paths-ignore.md` | Mục 10, nhật ký thực hiện (phiên ghi trên nhánh báo cáo) |
| Sửa | `docs/features/quy-trinh/tickets/T-094-merge-if-green-paths-ignore.md` | Trạng thái ticket theo kết quả task (phiên ghi trên nhánh báo cáo) |

Nơi đọc các file bị đổi (`grep -rn 'main-guard-verified\|merge-if-green\|fake-gh' scripts/ .github/ .claude/`):
- `main-guard.yml` gọi `main-guard-verified.sh <sha>`. Phần này không đổi; test 14 (`--docs-only agrees with the mode`) giữ nó.
- Skill `merge-pr` và `docs/auto-run.md` chỉ gọi `merge-if-green.sh` và đọc mã thoát. Mã thoát không đổi: ca mới trả 3 như mọi nhánh cần cập nhật.
- Mọi test dùng `fake-gh` không dùng `__error__`, nên chạy như cũ.

Các file khác mà `plan-check.py` (L-020) thấy có nhắc tới file của plan; không file nào bị ảnh hưởng, không sửa:
- `.github/workflows/main-guard.yml`:
  - gọi `scripts/main-guard-verified.sh <sha>`; lệnh đó không đổi;
  - chạy `scripts/tests/run.sh`;
  - không nhắc `merge-if-green.sh` ngoài chú thích; `paths-ignore` giữ nguyên.
- `.github/workflows/pr-check.yml`: nhắc `merge-if-green.sh` và `main-guard-verified.sh` trong chú thích (tên job, tên bước), chạy `scripts/tests/run.sh`. Tên job và tên bước không đổi.
- `scripts/is-exact-revert.sh`, `scripts/wait-run.sh`: chỉ nhắc `merge-if-green.sh` trong chú thích.
- `scripts/local-setup.sh`: nhắc `scripts/tests/run.sh` trong chú thích (chỗ test nối vào).
- `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt`: nhắc `scripts/tests/run.sh` trong chú thích dòng 87.
- Các file đọc `docs/lessons.md`. Plan chỉ đổi dòng Trạng thái và Áp dụng của hai bài học có sẵn, đúng dạng mà các file này đọc:
  - `scripts/status.py` đọc dòng Trạng thái;
  - `scripts/history-refs.py` coi sổ là chỗ giữ lịch sử;
  - `scripts/new-app.sh` tìm dòng đánh dấu đầu sổ;
  - `.github/ISSUE_TEMPLATE/from-app.yml` chỉ nhắc tên sổ.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm

### 3.4. Phương án dự phòng

Không có. Mọi phần là bash và python có sẵn, theo mẫu đã có trong cùng file.

### 3.5. Hành vi đã có bị đổi

Plan không sửa code Kotlin. Bảng ghi các câu `expect` có sẵn của `scripts/tests/run.sh` mà phần đổi chạm tới; không câu nào phải viết lại.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| PR chỉ có `feature/x/README.md` thì cập nhật nhánh (`--docs-only` đáp `yes`) | Như cũ (`--guard-ignores` đáp `yes`) | Test 8, ca cuối: `a PR that did not run check.sh is always updated` (`3 1 3 3 3 1`) | Giữ |
| Lệnh xóa thất bại, nhánh còn: cảnh báo sau một lần đọc | Cảnh báo sau `BRANCH_GONE_TRIES` lần đọc | `a branch left after a failed delete gets a warning` | Giữ; `merge_run` thêm `MERGE_BRANCH_WAIT_SECONDS=0` (mục G, phần dựng của test) |
| Lệnh xóa thất bại, nhánh mất: không cảnh báo | Như cũ, dừng ở lần đọc đầu | `a branch already deleted by GitHub gets no warning` | Giữ |
| `main-guard-verified.sh` cũ (đáp `full` cho mọi câu) thì cập nhật nhánh | Như cũ: câu hỏi thứ nhất đã đáp `full` | Test 12, `mgv-old.sh` | Giữ |
| `pr_sim` báo mẫu có ký tự lạ (`app/{a,b}/**`) | Như cũ; thêm dòng lạ không có nháy đơn | `the sample PR check refuses a pattern it does not support` | Giữ |
| `pr_filter_check` trên `pr-check.yml` thật | Như cũ: khối `run` thật không có dòng trống | Câu `pr-check: the filter check ...` (dòng 542 hôm nay, sáu lần gọi) | Giữ |
| `--docs-only` đáp theo `paths-ignore` trừ `GRADLE_READ_DOCS` | Không đổi | Test 14 (`agrees with the mode main-guard picks`), `accepts only what main-guard ignores` | Giữ |

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`. `check.sh` chạy file này, và `pr-check` chạy nó cho PR không có code. Tên test là chuỗi đầu của `expect`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| A. `--guard-ignores` đáp theo `paths-ignore` | Script test | T1 `main-guard-verified: --guard-ignores answers by the paths-ignore of main-guard`. Mỗi hàng một lần gọi, theo thứ tự: `docs/a.md`, `README.md`, `feature/x/README.md`, `.claude/skills/x/SKILL.md`, `docs/x.json`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/design-system/tokens.json`, `app/x.kt`, `scripts/a.sh`, `docs.kt`. Mong `yes yes yes yes yes yes yes yes no no no`. Trên code hiện tại script đáp `full` (dòng cách dùng): đỏ |
| A. Danh sách rỗng, tham số thừa | Script test | T2 `main-guard-verified: --guard-ignores of an empty list is yes, with an extra argument no`: mong `yes no` |
| A. Khớp đúng `paths-ignore` của workflow | Script test | T3 `main-guard-verified: --guard-ignores agrees with the paths-ignore patterns`. Đọc các mẫu `paths-ignore` từ `main-guard.yml` bằng awk của `mgv_ignored`, đổi mỗi mẫu thành mẫu bash (`'docs/**'` thành `docs/*`, `'**/*.md'` thành `*.md`; mẫu khác thì test in `unknown pattern`). Với từng hàng của T1, đáp của `--guard-ignores` là `yes` đúng khi hàng khớp một trong các mẫu đó. Mong `agree=11`. Câu `accepts only what main-guard ignores` có sẵn giữ để `paths-ignore` chỉ gồm hai mẫu đó |
| A. `--docs-only` không đổi | Script test | Test 14 có sẵn (mục 3.5) |
| 1. Ca lọt của L-152 | Script test | T4, thuộc test 8: `merge-if-green: a PR with only files main-guard ignores is updated, even a docs file Gradle reads`. `nu_routes`; file của PR là `feature/x/README.md` và `docs/remote-config-keys.md`. Mong mã 3 và đúng một dòng có `PR chỉ đổi file mà main-guard bỏ qua`. Ca thứ hai: PR chỉ có `docs/remote-config-keys.md`, mong `3`. Ca này chỉ đỏ trong dữ liệu giả (bước cổng `success`); trên GitHub thật tệp đó không thuộc bộ lọc `code`, nên `--gate-ran` đã buộc cập nhật. Ca lọt thật của L-152 là ca thứ nhất. Ca thứ ba: dữ liệu của ca thứ nhất với `--dry-run`, mong `3`. Đầu ra mong `3 1 3 3`. Trên code hiện tại cả ba ca thoát 0: đỏ |
| 1. Cập nhật thật với `--update` | Script test | T5 `merge-if-green --update: a PR with only files main-guard ignores is updated, then merged`. Dữ liệu của T4, cộng phần `def5678` như test 6 (`compare/main...def5678` không chậm, check-run trống rồi xanh, `pulls/12` lần sau có head `def5678`). Mong mã 0, lần ghi `PUT pulls/12/update-branch;PUT pulls/12/merge;DELETE git/refs/heads/claude/T-901-x;`. Trên code hiện tại không có `update-branch`: đỏ |
| 1. Đối chứng: PR code vẫn merge không cập nhật | Script test | T6 `merge-if-green: a code PR with a docs file Gradle reads still merges without updating`. File của PR là `app/src/main/x.kt` và `docs/remote-config-keys.md`. Mong mã 0 và một dòng `KHÔNG CẬP NHẬT NHÁNH`. Qua ngay trên code hiện tại; là đối chứng của định nghĩa hoàn thành 1, vế 2 |
| 1. Đáp lạ của câu hỏi mới | Script test | T7 `merge-if-green: an unreadable --guard-ignores answer updates`. Hai bản `main-guard-verified` giả: (a) đáp `yes` cho `--docs-only` và `full` cho mọi câu khác; (b) đáp `yes` cho `--docs-only` và in rỗng cho câu khác. Mong `3 3`. Bản (a) đứng cho script cũ không biết câu hỏi. Trên code hiện tại cả hai qua bước 3 vì script gọi `--docs-only` và nhận `yes`, rồi cập nhật vì PR "chỉ tài liệu": `3 3`. Đúng kết quả nhưng sai lý do, nên câu thêm số dòng `không đọc được PR có chỉ đổi file mà main-guard bỏ qua không`, mong `3 1 3 1`: đỏ |
| 1. Kiểm tra có sẵn, trước bước cập nhật | Script test | T8 `merge-if-green, only files main-guard ignores: every earlier check still refuses`. Dữ liệu của T4, mỗi lần một điều, như test 18 tới 24: (a) PR `closed`; (b) PR nháp; (c) nhãn `needs-human`; (d) base khác `main`; (e) phiên `running`, PR của người (`claude/template-x`); (f) issue `main-broken` không phải bản revert, dựng như test 22 (repo `$rev`, `revert-no`). Mong `1 1 1 1 1 1` và không có lần ghi nào. Qua ngay (các kiểm tra đứng trước bước 3 và không đổi): là đối chứng theo `docs/CLAUDE.md`, mục Khi viết plan |
| 1. Kiểm tra CI sau khi cập nhật | Script test | T9 `merge-if-green --update, only files main-guard ignores: a red check after the update is refused`. Như T5, check-run của `def5678` là `build-and-test` `failure`. Mong mã 1, có `PUT pulls/12/update-branch`, không có `PUT pulls/12/merge`. Trên code hiện tại không cập nhật mà merge `abc1234` (mã 0): đỏ |
| 1. CI chưa xong sau khi cập nhật | Script test | T19 `merge-if-green --update, only files main-guard ignores: CI still going after the update waits`. Như T5, check-run của `def5678` là `build-and-test` `in_progress` mãi; hạn chờ mặc định của `merge_run` (5 giây, `MERGE_POLL_SECONDS=0`), không đặt 0: hạn 0 làm script thoát ngay sau lần cập nhật, trước khi đọc check-run của head mới. Đầu ra: mã, số lần ghi `update-branch`, `yes` khi có ít nhất một dòng `đang ở trạng thái in_progress`, số lần ghi `PUT pulls/12/merge`. Mong `2 1 yes 0`. Trên code hiện tại merge `abc1234` (mã 0): đỏ |
| 1. `update-branch` bị từ chối | Script test | T20 `merge-if-green --update, only files main-guard ignores: a refused update-branch is refused`. Dữ liệu của T4, `fail_writes` có `PUT pulls/12/update-branch`. Mong mã 1 và một dòng `không cập nhật được nhánh`. Trên code hiện tại merge không cập nhật (mã 0): đỏ |
| 1. GitHub chưa cập nhật xong | Script test | T21 `merge-if-green --update, only files main-guard ignores: a head that does not move waits`. Dữ liệu của T4, `pulls/12` giữ head `abc1234` sau lần cập nhật; hạn chờ mặc định 5 giây của `merge_run`, như T19 (hạn 1 giây làm test chập chờn: hạn tính bằng giây nguyên và được kiểm ngay sau lần cập nhật đầu). Mong mã 2, đúng một lần ghi `update-branch`, và `yes` khi có ít nhất một dòng `chưa cập nhật xong`. Trên code hiện tại merge (mã 0): đỏ |
| 1. `MAX_UPDATES` | Script test | T22 `merge-if-green --update, only files main-guard ignores: three updates without catching up are refused`. Dữ liệu của T4; `pulls/12` là `__sequence__` của bốn bản chép `pulls/12` của `nu_routes`, chỉ đổi `head.sha`, theo bốn head (`abc1234`, `b100001`, `b100002`, `b100003`), mỗi head có `compare/main...<head>` chậm 2 và `compare/<head>...main` như `nu_routes`, nên lần nào cũng chậm. Mong mã 1, ba lần ghi `update-branch`, một dòng `Đã cập nhật nhánh 3 lần`. Trên code hiện tại merge (mã 0): đỏ |
| 2. Nhánh mất ở lần đọc thứ hai | Script test | T10 `merge-if-green: a branch gone at the second read after a failed delete gets no warning`. `merge_routes claude/template-x` như test có sẵn; `fail_writes` có `DELETE git/refs/heads/claude/template-x`; `git/ref/heads/claude/template-x` là `{"__sequence__": [{"ref": "x"}, {"__error__": "HTTP 404: Not Found"}]}`. Mong `0 2`: không dòng cảnh báo, và `mg.json.seen` ghi 2 lần đọc route đó. Trên code hiện tại `1 1`: đỏ |
| 2. Nhánh còn sau mọi lần đọc | Script test | T11 `merge-if-green: a branch still there after every read gets the warning`. Route là `{"__sequence__": [{"ref": "x"}]}` (một phần tử, lặp lại mãi; `fake-gh` chỉ ghi số lần đọc vào `.seen` cho route `__sequence__`). Mong `1 3`: một dòng cảnh báo, 3 lần đọc. Trên code hiện tại `1 1`: đỏ ở số lần đọc |
| 2. Lệnh xóa thành công | Script test | T12 `merge-if-green: a branch deleted by the script is not read again`. Không có `fail_writes`, route của nhánh là `{"__sequence__": [{"ref": "x"}]}` như T11. Mong `0 0`: không cảnh báo, không đọc. Qua ngay; đối chứng để vòng đọc chỉ chạy khi lệnh xóa thất bại |
| 2. Số lần đọc và thời gian chờ đặt được | Script test | T13 `merge-if-green: MERGE_BRANCH_TRIES sets the reads`. Như T11 với `MERGE_BRANCH_TRIES=1`, mong `1 1`. Trên code hiện tại `1 1`: qua ngay, chỉ giữ biến hoạt động sau khi sửa; đi cùng T11 |
| D. `fake-gh` trả lỗi trong chuỗi | Script test | T14 `fake-gh: an __error__ item of a sequence fails that read`. Route `x` là `{"__sequence__": [{"a": 1}, {"__error__": "HTTP 404: Not Found"}, {"a": 2}]}`. Đọc 3 lần, mong `0 1 0` (mã thoát) và stderr của lần 2 có `HTTP 404`. Trên `fake-gh` hiện tại lần 2 thoát 0: đỏ. Đi trong commit test cùng `fake-gh` mới (mục G), nên chỉ là test của phần dựng |
| 3. `pr_sim`, mẫu không có nháy | Script test | T15 `pr-check: the sample PR check refuses an unquoted or double-quoted pattern`. Hai bản chép `pr-check.yml` (`$work/prwf/unquoted.yml`, `$work/prwf/dquoted.yml`), thêm sau dòng `              - 'feature/**'` một dòng `              - docs/**` hay `              - "docs/**"`. `pr_sim_summary` với `docs/a.md` mong `ERROR mẫu chưa hỗ trợ: - docs/**` và `ERROR mẫu chưa hỗ trợ: - "docs/**"`. Trên code hiện tại cả hai bỏ qua dòng đó và in `code=false ...`: đỏ |
| 3. `pr_sim`, mẫu trước tên bộ lọc | Script test | T16 `pr-check: the sample PR check refuses a pattern before any filter name`. Bản chép thêm `              - 'docs/**'` ngay sau dòng `          filters: \|`. Mong `ERROR mẫu chưa hỗ trợ: - 'docs/**'`. Trên code hiện tại bỏ qua: đỏ |
| 3. `pr_sim`, đối chứng | Script test | T17 `pr-check: a key after the filters block ends it, comments and blank lines inside are fine`. Bản chép thêm, ngay sau dòng cuối của bộ lọc `release` (lần thứ hai của dòng `              - '.github/workflows/pr-check.yml'`, chèn bằng awk có biến đếm), một dòng `          list-files: none` (thụt bằng `filters:`). Trong khối `code`, sau dòng `              - 'app/**'`, thêm một dòng chỉ có 14 dấu cách (dòng rỗng hẳn không vào bước của `pr_sim`) và một dòng chú thích `              # x`. `pr_sim_summary` với `app/src/main/java/A.kt` mong `code=true release=false gate=yes gradle=yes docs=no r8=no`. Các câu `pr_sim` có sẵn (mục 3.5) giữ đúng trên file thật. Qua ngay trên code hiện tại; sau khi sửa vẫn phải qua, để quy tắc kết thúc khối không báo nhầm |
| 4. `pr_filter_check` đọc qua dòng trống | Script test | T18 `pr-check: the filter check test reads the run block past a blank line`. Bản chép `pr-check.yml` thêm vào cuối khối `run` của bước `Kiểm kết quả lọc đường dẫn`, tức ngay sau dòng `          done` đầu tiên của file (awk có biến trạng thái: chỉ trong bước đó; dòng `          done` có hai lần trong file), một dòng trống rồi `          exit 3`. `pr_filter_check <bản chép> true false \| cut -d' ' -f1` mong `3`. Trên code hiện tại awk dừng ở dòng trống, lệnh thoát 0: đỏ |
| Toàn bộ | CI | `scripts/check.sh` qua trên PR. Sau merge, `main-guard` của commit merge chạy `full` (đổi `main-guard-verified.sh`, `scripts/tests/run.sh`, `scripts/tests/fake-gh`, đều trong `GATE_FILES`) và xanh |
| Ca thật trên GitHub (L-149) | Kiểm tra thủ công (người: chỉ thấy khi phiên cloud merge một PR thật mà proxy từ chối lệnh xóa) | Phiên tự chạy kế tiếp sau merge: báo cáo không còn dòng `không xóa được nhánh` cho nhánh mà GitHub đã tự xóa. Người đọc báo cáo phiên đó, đối chiếu `git ls-remote --heads origin 'claude/T-*'` |

Thứ tự trong commit test (bước 3):
1. phần `fake-gh` (mục D) và `merge_run` (mục G);
2. T1 tới T22.

Thất bại đúng lý do mong ở: T1, T2, T3 (script đáp `full` cho câu hỏi chưa có, nên 8 hàng lệch), T4, T5, T7, T9, T19, T20, T21, T22, T10, T11, T15, T16, T18. T14 qua cùng commit (phần dựng).

Qua ngay, như bảng đã ghi: T6, T8, T12, T13, T17.

**Ngoại lệ viết test trước:**
- Dòng ghi chú của ADR-0041 và trạng thái hai bài học trong `docs/lessons.md`: chỉ là tài liệu, không có hành vi để test. `scripts/tests/run.sh` kiểm định dạng của sổ bài học.
- Phần `fake-gh` và `merge_run` của mục D, G: là phần dựng của test, vào cùng commit test, có T14 giữ.

### 4.1. Chạy thử

Không cần (E dưới 2). Mọi phần theo mẫu có sẵn trong cùng file: câu hỏi của `main-guard-verified.sh`, vòng đọc lại `mergeable`, `__sequence__` của `fake-gh`.

AI đã đối chiếu trên nhánh chính `f95cc92` (2026-11-08):
- `mgv_ask --docs-only <<<"$pr_files_list"` ở `scripts/merge-if-green.sh` dòng 169;
- khối xóa nhánh ở dòng 479 tới 487;
- `pr_sim` (dòng 411) chỉ nhận dòng `^\s+- '(.+)'$`, các dòng khác bỏ qua im lặng;
- `pr_filter_check` (dòng 402) dừng ở dòng không bắt đầu bằng 10 dấu cách.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI và cổng chất lượng: `scripts/merge-if-green.sh`, `scripts/main-guard-verified.sh`). Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai script của cổng merge, `gh` giả và test, hai tài liệu; không chạm code app hay workflow |
| B. Độ rõ của yêu cầu | 1 | Ticket và L-149, L-152 nêu rõ ca lọt và cách sửa; plan chọn cách hỏi `main-guard-verified.sh` (câu hỏi 1 ở mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi nhánh quyết có test với `gh` giả; chỉ ca proxy từ chối lệnh xóa trên GitHub thật là người xem ở phiên sau |
| D. Ảnh hưởng chéo | 2 | Mọi lần merge đi qua `docs_only_behind` và bước xóa nhánh. Ca mới chỉ làm thêm một lần cập nhật nhánh cho PR hiếm; không plan nào đang chờ sửa cùng file |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của mẫu có sẵn: phần tử `__error__` trong `__sequence__` của `fake-gh` là cách mới, còn lại theo mẫu trong cùng file |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm:
  - `merge-if-green.sh`: mọi lần merge, của người và của phiên;
  - `main-guard-verified.sh`: thêm câu hỏi, cách chọn cho `main-guard` không đổi;
  - `fake-gh`: mọi test dùng `gh` giả; phần mới chỉ chạy khi route có `__error__`.
- Task khác có thể xung đột:
  - T-095 (`backlog`, plan soạn cùng ngày) sửa `scripts/tests/run.sh` ở phần `pr-check: the runner check` và `run-watchdog`, khác chỗ với plan này. T-095 cũng sửa bước kiểm runner của `pr-check.yml`, tệp mà T15 tới T18 chép; chỗ chèn của các test đó (bộ lọc, bước kiểm lọc) không đổi. Hai plan có thể cùng một danh sách chạy; `run-check.py` sẽ báo chú ý vì chung file.
  - Không plan `approved` nào khác đang sửa `merge-if-green.sh` hay `main-guard-verified.sh`.
- Chuyển tiếp:
  - PR của chính task được merge bằng `merge-if-green.sh` bản cũ trên nhánh chính, như mọi PR. Điều này không đổi gì: PR có code (`scripts/`) và đi đường cũ.
  - Bản cũ còn ở worktree tạo trước khi merge vẫn chạy đúng như trước, chỉ thiếu phần sửa.
- Các app nhận qua đợt đồng bộ (`scripts/template-sync.sh`): script và test là của dự án mẫu. App chưa đồng bộ vẫn giữ ca lọt hiếm của L-152.
- Cách chạy:
  - Phiên tự chạy, như T-077.
  - Plan không sửa `.github/workflows/`, nên không cần `workflow-push` hay `push-probe.sh`.
- Đề xuất thứ tự: độc lập. Có thể chạy trước hay sau T-095.

## 7. Điều kiện dừng riêng

- Trước bước 3, đối chiếu nhánh chính với mục 4.1. Dừng, cần plan phiên bản mới, khi:
  - `docs_only_behind` không còn hỏi danh sách file của PR bằng `--docs-only`;
  - `paths-ignore` của `main-guard.yml` khác `docs/**` và `**/*.md`;
  - `fake-gh` đã có cách khác để trả lỗi trong `__sequence__`.
  
  Chỉ lệch số dòng thì theo ADR-0032/L3.
- Sau khi sửa, test 14 (`--docs-only agrees with the mode main-guard picks`) hay một câu có sẵn khác của `merge-if-green:` đổi kết quả: dừng. Plan không được đổi đường đi của PR code hay của cách `main-guard` chọn.
- Cần sửa `.github/workflows/`, `scripts/check.sh` hay `scripts/release.sh`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Đặt quy tắc `paths-ignore` ở đâu.**
   - (a) Câu hỏi mới `--guard-ignores` của `main-guard-verified.sh`, như mục 3.1 A. Đề xuất: (a).
   - (b) Viết thẳng hai mẫu trong `merge-if-green.sh`, có test giữ khớp với workflow.

   Lý do chọn (a): ADR-0041 mục 2 đặt mọi quy tắc của `main-guard` ở `main-guard-verified.sh`, và test T3 cùng `mgv_ignored` giữ nó khớp `paths-ignore` ở một chỗ.
2. **Ba lần đọc, mỗi lần 5 giây (mục 3.1 C).** L-149 đề xuất con số này. Chưa có số đo GitHub tự xóa nhánh sau bao lâu: báo cáo RUN-20261107-01 chỉ cho biết ba nhánh đã merge đều đã mất ở lần kiểm sau đó, không cho biết lúc mất. Nếu GitHub xóa chậm hơn 10 giây, cảnh báo sai vẫn còn và chỉ lộ ra ở phiên sau.
   - (a) Giữ 3 lần, 5 giây như mục 3.1 C; báo cáo phiên kế tiếp còn cảnh báo sai thì tăng bằng biến, không cần plan mới. Đề xuất: (a).
   - (b) Thêm: nhánh còn sau mọi lần đọc mà repo bật `delete_branch_on_merge` (đọc `repos/<repo>`) thì in một dòng thông tin thay cho `CHÚ Ý`. Thêm một lần đọc, một nhánh code và hai test (người duyệt chọn (b) thì plan lên phiên bản 2, có test và route `repos/<repo>`, trước khi duyệt); đổi lại, cảnh báo sai không còn dù GitHub chậm, nhưng nhánh thật sự không xóa được (ví dụ nhánh được bảo vệ) cũng chỉ còn dòng thông tin.
3. **Ghi chú vào ADR-0041 (mục 3.2).** Đề xuất: một dòng ghi chú như ADR-0043, không viết lại ADR.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Nhật
- **Ghi chú:** AI ghi theo lời anh Nhật ngày 2026-11-08, trong phiên có người: "Duyệt T-094 (câu 1 chọn a, câu 2 chọn a, câu 3 đồng ý) và T-095 (câu 1 chọn a, câu 2 giữ 75 phút, câu 3 đồng ý)." Trả lời mục 8, theo đề xuất: (1) câu hỏi mới `--guard-ignores` của `main-guard-verified.sh`; (2) giữ 3 lần đọc, mỗi lần 5 giây; báo cáo phiên kế tiếp còn cảnh báo sai thì tăng bằng biến; (3) một dòng ghi chú vào ADR-0041. Duyệt ở commit a1ba097 (PR #422).

## 10. Nhật ký thực hiện

### RUN-20261108-02, phiên bản plan 1

- **Kết quả:** đã merge (PR #425, commit `fb0d08d`, 2026-11-09 02:34 UTC).
- **Nhánh, PR:** `claude/T-094-merge-if-green-paths-ignore`, https://github.com/VGL-Studio/app-project-template/pull/425. Commit test `5690bac`, code `731cda0`.
- **Đã làm:** mục 3.1 A tới G. `main-guard-verified.sh --guard-ignores` (`ignored_reason`); `docs_only_behind` bước 3 hỏi `--guard-ignores`, chỉ đáp `no` mới đi tiếp; đọc lại nhánh sau lệnh xóa thất bại (`MERGE_BRANCH_TRIES` 3, `MERGE_BRANCH_WAIT_SECONDS` 5); phần tử `__error__` của `fake-gh`; `pr_sim` báo mọi dòng lạ của khối `filters`; `pr_filter_check` đọc qua dòng trống; `merge_run` đặt `MERGE_BRANCH_WAIT_SECONDS=0` và đọc hạn chờ từ `UPDATE_TIMEOUT` (mặc định 5) theo ghi chú đã duyệt của danh sách chạy, T5, T9, T22 gọi với 60. Dòng ghi chú ở ADR-0041. Bước 3 đến 5 giao cho một phiên con.
- **Test viết trước:** 22 test (T1 tới T22). 16 test đỏ đúng lý do trước code, khớp danh sách của mục 4; rà soát độc lập đã chạy lại commit test và xác nhận. T14 qua cùng commit test (phần dựng; trên `fake-gh` cũ đỏ `0 0 0`). T6, T8, T12, T13, T17 qua ngay như plan ghi. 0 test phải viết chặt lại; ba test qua ngay có thêm đối chứng (T8 `broken=1`, T17 và T18 đếm dòng chèn). Thay đổi ở file test sau commit test: chỉ thân `pr_sim` và `pr_filter_check` (phần code của mục E, F nằm trong file test), không câu khẳng định nào đổi.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 4 ghi chú: (1) `wc -l < build.log 2>/dev/null` có sẵn của `pr_filter_check` vẫn in một dòng stderr khi T18 chạy, kết quả đúng; (2) `MERGE_BRANCH_TRIES=0` thì không bao giờ cảnh báo, giống `MERGEABLE_TRIES`; (3) câu chữ ghi chú ADR-0041 ("hỏi thêm ... thay cho"); (4) T19, T21 dùng hạn 5 giây đồng hồ thật, chỗ xem đầu tiên nếu `main-guard` chập chờn.
- **Kết quả CI:** `pr-check` xanh (lần chạy 42551028038). `scripts/tests/run.sh` cục bộ: qua (1488 ok). `check.sh` QUA (1871 test JVM, 0 lỗi). `main-guard` của `fb0d08d`: xanh (lần chạy 42551706889), không có issue `main-broken`.
- **Lệch so với plan:** ADR-0032/L3: khối T8 đặt sau test 24 vì dùng hàm `nu_sha` chỉ có ở đoạn test 18 tới 26; đầu ra một số test thêm đối chứng (trên). Không có file ngoài mục 3.2.
- **Việc còn lại:** kiểm thủ công của mục 4 (L-149 trên GitHub thật) ở phiên kế tiếp. PR #425 tự merge bằng `merge-if-green.sh` bản cũ, nên lần merge đó vẫn in `CHÚ Ý: không xóa được nhánh 'claude/T-094-merge-if-green-paths-ignore'`.
- **Cần người xem:** bốn ghi chú của rà soát; kiểm thủ công L-149 ở phiên kế tiếp (đối chiếu `git ls-remote --heads origin 'claude/T-*'`).
