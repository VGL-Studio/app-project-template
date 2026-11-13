---
ticket: T-105
title: Làm theo ADR-0048 - run-check.py --batch cho danh sách của một đợt, tài liệu duyệt cả đợt và bỏ họp
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn 7 (quy trình phiên tự chạy), không tính điểm
risk_level: high             # low | medium | high
token_estimate: 19.4M        # cỡ M, số gợi ý 19.4M của scripts/token-usage.py estimate (2026-11-13, trung vị 30 task M); không chỉnh
blocking_points: [ci]
run_order:                   # AI điền khi lập danh sách chạy
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-105: LÀM THEO ADR-0048: RUN-CHECK.PY --BATCH, TÀI LIỆU DUYỆT CẢ ĐỢT VÀ BỎ HỌP

## 1. Tóm tắt

`scripts/run-check.py` có thêm chế độ `--batch`, để kiểm danh sách chạy `draft` của một đợt khi các plan còn `draft`: chỉ bỏ qua lỗi trạng thái của plan `draft`, ticket `planned` và spec chưa `confirmed` mà chính nhánh này đổi so với điểm tách khỏi nhánh chính; mọi kiểm tra khác giữ nguyên. Tài liệu quy tắc, mẫu và command sửa theo danh sách ở mục Hệ quả của ADR-0048: duyệt cả đợt trong một PR, không bắt buộc họp, AI ghi trạng thái duyệt theo phần C, đường làm nhanh gộp nhánh chính trước khi kiểm. Kiểm chứng bằng test của `run-check.py` trong `scripts/tests/run.sh` (kho git tạm có hai commit) và `scripts/history-refs.py`.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Nguồn: ADR-0048 (phần B, phần C, mục Hệ quả), `features/quy-trinh/requests/2026-11-12-giam-thoi-gian-cho.md` việc 2; ticket `T-105`.

| Điều cần đạt | Cách hiểu của AI |
| --- | --- |
| Kiểm danh sách khi plan còn `draft` (ADR-0048, B3) | `run-check.py --batch <danh sách>`. "Đổi trong cùng PR" là có trong `git diff --name-only <điểm so>...HEAD`, điểm so mặc định `origin/main` (người gọi chạy `git fetch origin` trước, như mọi chỗ khác của tài liệu dùng `origin/main`). Chỉ ba lỗi được bỏ qua, mỗi lỗi một điều kiện: plan có trong diff và `status: draft`; ticket có trong diff và `status: planned`; spec có trong diff (trạng thái gì cũng được). Plan `approved` có sẵn trên nhánh chính vẫn qua như thường |
| Mọi kiểm tra khác giữ nguyên mức | Câu hỏi còn mở của spec, phụ thuộc, kịch bản không có dòng test, `plan-check.py`, `auto_run: false`, quy tắc `start`, các quy tắc chéo: không đổi gì. `--batch` chỉ cho danh sách `draft`; danh sách khác `draft` mà gọi `--batch` là lỗi, vì danh sách `approved` phải qua mà không có `--batch` (B5) |
| Thấy được cái gì đã được bỏ qua | Mỗi lỗi trạng thái bị bỏ qua in một dòng `CHÚ Ý: (--batch) ...` nêu file và trạng thái, để người rà soát và người duyệt thấy |
| Tài liệu theo ADR-0048 | Danh sách file ở mục Hệ quả của ADR, cộng các câu cùng loại tìm được bằng `grep` (mục 3.1 B). Các chuỗi "Đường làm nhanh (ADR-0034)" đã đổi ở PR chấp nhận ADR; còn lại là câu bắt buộc họp, câu "chỉ dev (người) chuyển sang", hướng dẫn duyệt cả đợt, bước gộp nhánh chính của đường làm nhanh, và số "khoảng 15 phút mỗi task" (nay đo được 45 tới 60 phút) |

Thiết kế tham chiếu: Không có thiết kế riêng (ticket quy trình).

ADR liên quan: ADR-0048 (nguồn của mọi thay đổi), ADR-0049 (với `start: auto`, merge danh sách `approved` là bắt đầu phiên: tài liệu duyệt cả đợt phải giữ câu đó), ADR-0044 (tài liệu quy tắc không ghi mã ticket, mã bài học).

## 3. Giải pháp

### 3.1. Cách làm

**A. `scripts/run-check.py` (sửa).**

- `main` nhận `scripts/run-check.py [--batch] <danh sách>`; sai tham số vẫn in cách dùng và trả 2.
- Với `--batch`:
  - Danh sách phải `status: draft`; khác thì một dòng `LỖI: --batch chỉ dùng cho danh sách draft (đang '<status>'); danh sách approved phải qua run-check.py không có --batch.`
  - Lấy tập file đổi: `git -C <ROOT> diff --name-only <điểm so>...HEAD`, `<điểm so>` là biến `RUN_CHECK_BASE` hay mặc định `origin/main`. Lệnh lỗi thì một dòng `LỖI: --batch không đọc được diff so với <điểm so> (<thông báo của git>); chạy git fetch origin rồi thử lại.` và không bỏ qua gì.
  - Đường dẫn trong tập so với đường dẫn của plan, ticket, spec tính từ gốc repo (`docs/features/...`).
- Trong `check(run_path, batch_files=None)`: ba chỗ báo lỗi trạng thái (plan chưa `approved`, ticket chưa `approved`, `spec_problems` báo spec chưa `confirmed`) hỏi một hàm `waived(path, status, allowed)`: đúng khi `batch_files` không rỗng, `path` có trong đó và `status` thuộc `allowed` (plan: `{"draft"}`; ticket: `{"planned"}`; spec: mọi trạng thái). Đúng thì thêm cảnh báo `(--batch) bỏ qua trạng thái: <đường dẫn> đang '<status>', đổi trong nhánh này.` thay cho lỗi, mỗi file một dòng (spec dùng chung cho nhiều ticket chỉ một dòng). `spec_problems` nhận thêm tham số `waive_status` (mặc định `False`) để chỉ bỏ dòng lỗi trạng thái, các lỗi câu hỏi còn mở giữ nguyên. `scripts/plan-queue.py` gọi `spec_problems` với hai tham số như cũ, nên không đổi.
- Không có `--batch`: đường đi y như hôm nay.
- Chú thích đầu file thêm quy tắc 11 mô tả `--batch` và `RUN_CHECK_BASE`.

**B. Tài liệu, mẫu và command.** Mỗi file sửa đúng những câu nêu dưới đây; câu nào còn nói họp như điều kiện, hay "chỉ dev (người) chuyển sang" trái phần C, viết lại theo ADR-0048. Không đổi tên mục nào.

- `docs/README.md`:
  - bảng Ai tạo, ai xác nhận: dòng Phiên tự chạy bỏ "lập sau họp";
  - bước 3: spec được xác nhận bởi người phụ trách sản phẩm, hoặc AI ghi theo lời đồng ý trong phiên (ADR-0048, phần C);
  - bước 6 và 7: bước 7 đổi tên thành duyệt plan và danh sách chạy; hai cách: duyệt cả đợt trong một PR (AI lập danh sách `draft` cùng plan `draft`, kiểm bằng `run-check.py --batch` và rà soát, người duyệt một lần), hay duyệt plan lẻ rồi lập danh sách; họp không phải điều kiện;
  - bước 9: plan sửa theo phản hồi được duyệt lại như plan mới;
  - đoạn Đường làm nhanh: "bỏ họp" giữ ý (không có họp, như mọi đường), thêm bước gộp nhánh chính trên máy trước khi kiểm danh sách chạy.
- `docs/CLAUDE.md`:
  - mục Khi sinh spec từ yêu cầu, ý cuối: chuyển `confirmed` theo phần C (người có quyền xác nhận, hoặc AI ghi theo lời đồng ý trong phiên, nguyên văn ở mục lịch sử);
  - mục Khi viết plan, ý cuối: plan mới `draft`; AI chỉ ghi `approved` theo phần C; plan của một đợt đi chung PR với danh sách (mục mới bên dưới);
  - mục Khi lập danh sách chạy: ý đầu bỏ "Sau họp đánh giá rủi ro"; số "khoảng 15 phút" đổi thành "khoảng 45 tới 60 phút mỗi task (phiên con, rà soát, chờ CI)"; ý "Chỉ dev mới chuyển sang `approved`" theo phần C, giữ câu về `start: auto`;
  - mục mới **Khi duyệt cả đợt** (sau mục Khi lập danh sách chạy): các bước B1 tới B5 của ADR-0048 thành việc AI làm (nhánh `claude/batch-YYYYMMDD-NN`; spec đi cùng; `run-check.py --batch` và rà soát trên bản `draft`, chạy lại cả hai sau mỗi lần sửa; tóm tắt cả đợt kèm câu duyệt có sẵn và điều xảy ra khi chỉ duyệt một phần; lời duyệt chỉ có hiệu lực với phần người nói có quyền; ghi trạng thái trong một commit rồi `run-check.py` không có `--batch`; gộp `origin/main` trên máy, không đẩy, chạy lại `plan-check.py` và `run-check.py` ngay trước `merge-if-green.sh`; chỉ đóng PR plan cũ khi dev đồng ý). Với `start: auto`, câu duyệt có sẵn ghi rõ bắt đầu phiên khi merge;
  - mục Khi làm nhanh có người theo dõi, ý "Trước khi gọi `merge-if-green.sh`": thêm `git fetch origin` và gộp `origin/main` trên máy, không đẩy, xung đột thì dừng và báo dev (ADR-0048, A7); nhắc không merge danh sách `start: auto` khi PR làm nhanh còn mở;
  - mục Khi xử lý phản hồi: "Plan sửa đi qua họp đánh giá rủi ro như plan mới" thành "được duyệt lại như plan mới".
- `docs/risk-scoring.md`: dòng mở đầu bỏ "trước khi đưa vào họp đánh giá" (thành "trước khi đưa người duyệt"); cột Ghi chú của bảng mục 3: "Nêu ngắn khi duyệt", "Nêu trong tóm tắt duyệt, dev còn lại được quyền yêu cầu duyệt chéo"; câu đường làm nhanh bỏ "không cần họp" (không còn họp ở đâu), thêm một câu: duyệt trên PR hay bằng lời trong phiên theo ADR-0048 là đủ cho mọi mức, họp không bắt buộc, quy tắc duyệt chéo giữ nguyên.
- `docs/auto-run.md`: mục 1 bước 1 bỏ "Sau họp đánh giá rủi ro", thêm "danh sách có thể đi cùng PR với các plan `draft` của đợt (`docs/CLAUDE.md`, mục Khi duyệt cả đợt)"; bước 2: dev duyệt, hoặc AI ghi `approved` theo lời đồng ý trong phiên của dev (ADR-0048, phần C).
- `docs/ops/pre-run-review.md`: đoạn mở đầu thêm rằng hai lớp kiểm tra chạy được trên bản `draft` của một đợt bằng `run-check.py --batch`; bảng mục 1 thêm dòng `--batch` (lỗi trạng thái bị bỏ qua thành chú ý, chỉ với file đổi trong nhánh; danh sách phải `draft`).
- `docs/ops/quick-lane.md`: dòng bước 4 của bảng thêm gộp nhánh chính trên máy trước khi kiểm; câu "Muốn làm tiếp thì plan có phiên bản mới, đi đường thường: họp đánh giá rủi ro, danh sách chạy, phiên tự chạy" bỏ "họp đánh giá rủi ro".
- `docs/ops/token-usage.md`: tiêu đề mục 4 "Dùng số liệu ở retro và họp đánh giá rủi ro" đổi thành "Dùng số liệu ở retro và khi duyệt plan" (không có link nào trỏ vào tiêu đề này; `grep -rn "token-usage.md#"` rỗng).
- `docs/ops/setup-new-app.md`, mục 7, bước 2 và 3: người duyệt, hoặc AI theo lời đồng ý trong phiên của người duyệt, chuyển plan, ticket, danh sách sang `approved`; nhắc cách duyệt cả đợt.
- `docs/templates/plan.md`: chú thích `run_order` "AI điền khi lập danh sách chạy"; chú thích mục 9 "Người duyệt điền, hoặc AI ghi theo lời đồng ý trong phiên của người duyệt, nguyên văn (ADR-0048, phần C)". Các chữ giữ chỗ của mục 9 (`Duyệt / Dừng`, `<tên>`, `<tên, nếu rủi ro cao>`) giữ nguyên, vì `scripts/plan-check.py` đọc chúng.
- `docs/templates/run.md`: chú thích mục 1 bỏ "lập sau họp đánh giá rủi ro", thêm "có thể đi cùng PR với plan `draft` của đợt"; tiêu đề "Đề xuất cho buổi họp tiếp theo" thành "Đề xuất cho đợt sau"; chú thích `approved_by` "người duyệt danh sách (AI ghi theo lời đồng ý trong phiên khi người duyệt nói trong phiên)".
- `.claude/skills/run-list/SKILL.md`: description bỏ "sau họp đánh giá rủi ro"; thêm cách lập cho một đợt: plan `draft` trên cùng nhánh `claude/batch-YYYYMMDD-NN`, bước 7 chạy `run-check.py --batch`, bước 10 tóm tắt cả đợt và câu duyệt có sẵn theo `docs/CLAUDE.md`, mục Khi duyệt cả đợt.
- `.claude/skills/write-plan/SKILL.md`: description bỏ "đưa vào họp đánh giá rủi ro"; bước 6: plan của một đợt viết trên nhánh của đợt; không tự ghi `approved` trừ khi có lời đồng ý trong phiên theo phần C.
- `.claude/skills/quick-lane/SKILL.md`: bước kiểm danh sách chạy chưa chạy thêm `git fetch origin` và gộp `origin/main` trên máy, không đẩy.
- `.claude/skills/spec/SKILL.md`: bước 5 "Không tự ghi `confirmed`, trừ đường làm nhanh" thành "ghi `confirmed` chỉ theo lời đồng ý trong phiên của người xác nhận spec, nguyên văn ở mục lịch sử (ADR-0048, phần C)".
- `.claude/skills/triage-feedback/SKILL.md`: "qua họp đánh giá rủi ro" thành "duyệt lại như plan mới".
- `.claude/skills/adr/SKILL.md`, bước 5: "Chỉ đổi trạng thái khi dev đã quyết trong phiên" thêm "ghi lời đồng ý nguyên văn trong mô tả PR (ADR-0048, C2)".

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/run-check.py` | Chế độ `--batch` (3.1 A) |
| Sửa | `scripts/tests/run.sh` | Test của mục 4 |
| Sửa | `docs/README.md` | 3.1 B |
| Sửa | `docs/CLAUDE.md` | 3.1 B, kể cả mục mới Khi duyệt cả đợt |
| Sửa | `docs/risk-scoring.md` | 3.1 B |
| Sửa | `docs/auto-run.md` | Mục 1, bước 1 và 2 |
| Sửa | `docs/ops/pre-run-review.md` | Đoạn mở đầu, bảng mục 1 |
| Sửa | `docs/ops/quick-lane.md` | Bảng bước 4, mục khi dừng |
| Sửa | `docs/ops/token-usage.md` | Tiêu đề mục 4 |
| Sửa | `docs/ops/setup-new-app.md` | Mục 7, bước 2 và 3 |
| Sửa | `docs/templates/plan.md` | Chú thích `run_order`, mục 9 |
| Sửa | `docs/templates/run.md` | Chú thích mục 1, tiêu đề đề xuất, `approved_by` |
| Sửa | `.claude/skills/run-list/SKILL.md` | Lập cho một đợt, `--batch` |
| Sửa | `.claude/skills/write-plan/SKILL.md` | Description, bước 6 |
| Sửa | `.claude/skills/quick-lane/SKILL.md` | Gộp nhánh chính trước khi kiểm |
| Sửa | `.claude/skills/spec/SKILL.md` | Bước 5 |
| Sửa | `.claude/skills/triage-feedback/SKILL.md` | Bỏ "qua họp" |
| Sửa | `.claude/skills/adr/SKILL.md` | Bước 5 |

Nơi đọc file bị đổi (`scripts/plan-check.py`, L-020):

- `scripts/plan-queue.py` nạp `scripts/run-check.py` để dùng `front_matter`, `ids`, `spec_problems`: `spec_problems` thêm một tham số có mặc định, lời gọi cũ không đổi. `scripts/plan-check.py` và `scripts/auto-start-check.py` chỉ nhắc `run-check.py` trong chú thích hay thông báo.
- `docs/templates/plan.md`: `scripts/plan-check.py` đọc các chữ giữ chỗ của mục 9 (`DECISION_PLACEHOLDERS`) và tiêu đề các mục; `scripts/existing-tests.py`, `scripts/ads-plan.py` chỉ nhắc tên file; `scripts/tests/run.sh` đọc lệnh `git status --short -- '...'` của mẫu. Plan chỉ đổi hai dòng chú thích, không đổi các chuỗi đó.
- `docs/templates/run.md`: các script nhắc tên `run.md` là đường dẫn danh sách chạy, không đọc mẫu; `scripts/status.py`, `scripts/run-check.py` đọc frontmatter của danh sách thật. Plan không đổi khóa frontmatter nào, chỉ chú thích và một tiêu đề trong phần báo cáo; `grep -rn "buổi họp\|Đề xuất cho" scripts .github` rỗng.
- Nhắc `docs/auto-run.md`, `docs/CLAUDE.md` hay `docs/ops/setup-new-app.md` bằng đường dẫn trong chú thích hay thông báo, trỏ tới mục khác với các đoạn plan sửa, không đọc nội dung; plan không đổi tên mục nào: `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/main-guard.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/cloud-setup.sh`, `scripts/device-session.sh`, `scripts/failure-kind.sh`, `scripts/java-check.sh`, `scripts/local-setup.sh`, `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/push-on-commit.sh`, `scripts/push-probe.sh`, `scripts/resume-check.py`, `scripts/run-watchdog.py`, `scripts/save-branch.sh`, `scripts/schedule-idle.py`, `scripts/setup-check.sh`, `scripts/ui-test-verdict.sh`, `scripts/wait-run.sh`, `scripts/tests/playbook/PB-17.log`.
- Chạy hay nhắc `scripts/tests/run.sh`: `.github/workflows/main-guard.yml`, `.github/workflows/pr-check.yml`, `scripts/check.sh`, `scripts/local-setup.sh`, `scripts/main-guard-verified.sh`, `scripts/merge-if-green.sh`, `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt`. Plan chỉ thêm test, không đổi cách gọi hay tên file.
- `scripts/new-app.sh` và `scripts/history-refs.py` đọc cả file tài liệu theo mẫu chuỗi (tên app ở dòng đầu, mã lịch sử): plan không đổi dòng đầu và không thêm mã ticket hay mã bài học. `docs/ops/pre-run-review.md`, `docs/ops/quick-lane.md`, `docs/risk-scoring.md` (chỉ được nhắc trong thông báo của `scripts/plan-check.py`, trỏ tới mục 1 và 4, plan không đổi hai mục đó): không có nơi nào đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm
- **Thư viện và SDK:** Không thêm. `run-check.py` chỉ dùng thư viện chuẩn của Python 3 và `git`

### 3.4. Phương án dự phòng

Không có.

### 3.5. Hành vi đã có bị đổi

Không sửa code Kotlin nên `scripts/existing-tests.py` không áp dụng. Không có `--batch`, `run-check.py` chạy y như cũ, nên mọi câu khẳng định có sẵn của khối `run-check` trong `scripts/tests/run.sh` giữ nguyên. Không có hành vi nào đã có bị đổi.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, ngay sau khối `run-check` có sẵn (dùng lại `write_spec`, `write_ticket`, `write_run_plan`, `write_run`, `run_row` và thư mục `$rr`). Khối mới biến `$rr` thành kho git: `git init`, commit A là trạng thái nhánh chính (spec `confirmed`, T-001 và T-002 `approved` như khối có sẵn), rồi commit B đổi các file theo từng ca. Hàm `run_batch` gọi `RUN_CHECK_BASE=<A> RUN_CHECK_ROOT="$rr" PLAN_CHECK_ROOT="$rr" python3 -I scripts/run-check.py --batch "$rr/docs/runs/RUN-1.md"`, in `pass` hay `stop`, đầu ra trong `$work/run-check.out`. `write_run` ghi danh sách `status: draft` sẵn.

| Điều cần đạt | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đợt mới: plan `draft`, ticket `planned`, spec `changed`, đều đổi trong nhánh | Test script | B1 `run-check: --batch passes a batch whose draft plans, planned tickets and changed spec are in the branch`: B đổi plan T-002 sang `draft`, ticket T-002 sang `planned`, spec sang `changed` (câu hỏi đã trả lời). Mong `pass` và đúng 3 dòng `(--batch) bỏ qua trạng thái`. Cùng cây file không có `--batch`: mong `stop` (đối chứng). Đỏ trước code (`--batch` chưa có, script trả 2) |
| Plan `draft` không đổi trong nhánh | Test script | B2: plan T-002 `draft` đã có ở A, B chỉ đổi ticket. Mong `stop`, có dòng `plan đang 'draft', chưa approved` |
| Trạng thái khác điều kiện | Test script | B3: ticket T-002 đổi trong nhánh nhưng `ready`: mong `stop`. Plan đổi trong nhánh nhưng `stopped`: mong `stop` |
| Spec còn câu hỏi chưa trả lời | Test script | B4: spec đổi trong nhánh, `changed`, trả lời trống: mong `stop`, có `câu hỏi còn mở chưa có trả lời`, và không có dòng lỗi `chưa confirmed` (trạng thái được bỏ qua, câu hỏi thì không) |
| Kiểm tra khác giữ nguyên | Test script | B5: như B1 nhưng ticket T-001 phụ thuộc T-002 đứng sau: mong `stop`, có `phụ thuộc T-002 đứng sau` |
| Danh sách không `draft` | Test script | B6: như B1, danh sách `approved`: mong `stop`, có `--batch chỉ dùng cho danh sách draft` |
| Không đọc được diff | Test script | B7: `RUN_CHECK_BASE=khong-co`: mong `stop`, có `--batch không đọc được diff`, và có dòng `plan đang 'draft'` (không bỏ qua gì) |
| Sai tham số | Test script | B8: `run-check.py --batch` không có đường dẫn: mã 2. `run-check.py --batch a b`: mã 2 |
| Không có `--batch` giữ nguyên | Test script | Toàn bộ câu khẳng định có sẵn của khối `run-check` (không sửa câu nào) |
| Tài liệu đúng ý | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc các đoạn sửa ở mục 3.1 B; `grep -rn "họp" docs/README.md docs/CLAUDE.md docs/risk-scoring.md docs/auto-run.md docs/ops/pre-run-review.md docs/ops/quick-lane.md docs/templates .claude/skills` chỉ còn chỗ nói họp như việc tùy chọn hay "biên bản họp" của yêu cầu. `scripts/history-refs.py` qua |
| Toàn bộ | Test script | `scripts/tests/run.sh` trên CI (`pr-check`, rồi `main-guard` sau merge) |

**Ngoại lệ viết test trước:** tài liệu, mẫu và command ở mục 3.1 B: chỉ là chữ, kiểm bằng người đọc và `scripts/history-refs.py`.

### 4.1. Chạy thử

Không cần (E dưới 2): đọc diff bằng `git diff --name-only` đã dùng ở `scripts/auto-start-check.py` và `scripts/plan-queue.py`.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7. Quy trình phiên tự chạy: cách duyệt plan và danh sách chạy, quy tắc ghi trạng thái duyệt. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script, test, mười hai tài liệu và mẫu, sáu command |
| B. Độ rõ của yêu cầu | 1 | ADR-0048 nói rõ; còn vài cách hiểu cần người chốt (mục 8) |
| C. Khả năng kiểm chứng tự động | 1 | `--batch` có test đủ ca; phần tài liệu chỉ kiểm bằng người đọc |
| D. Ảnh hưởng chéo | 2 | Mọi danh sách chạy và mọi lần duyệt sau đều theo tài liệu này |
| E. Độ mới của kỹ thuật | 0 | Theo mẫu có sẵn của `run-check.py` |
| **Tổng** | 6 | **Mức: Cao (điểm chặn 7)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `run-check.py` (mọi danh sách), các tài liệu quy tắc mà mọi phiên đọc, mẫu plan và mẫu danh sách. `plan-queue.py` dùng `spec_problems` (tham số mới có mặc định).
- Task khác trong sprint có thể xung đột: không có task nào đang mở sửa các file này.
- Đề xuất thứ tự chạy: không phụ thuộc task nào. Làm trước đợt đầu tiên muốn duyệt cả đợt.

## 7. Điều kiện dừng riêng

- Thấy cần bỏ qua thêm một loại lỗi (ngoài ba lỗi trạng thái) để một đợt thật qua `--batch`: dừng, người quyết, vì ADR-0048 chỉ cho ba lỗi đó.
- Viết lại một câu tài liệu mà ý của nó nới quy tắc duyệt chéo hay cho AI ghi trạng thái khi chưa có lời đồng ý trong phiên: dừng, người quyết.

## 8. Câu hỏi cho người duyệt

1. **Ai làm.** Ticket để `auto_run` trống: phiên tự chạy làm được (không sửa `.github/`, không cần quyền `workflow`). Nhưng task sửa chính các tài liệu mà phiên đang đọc để làm việc (`docs/CLAUDE.md`, `docs/auto-run.md`). Đề xuất: để phiên tự chạy làm; phiên đọc tài liệu ở đầu phiên, và PR của task chỉ có hiệu lực sau khi merge. Anh muốn làm có người thì ghi `auto_run: false`.
2. **Trạng thái ticket được bỏ qua.** ADR-0048 ghi "ticket `planned`". Đề xuất: giữ đúng `planned` (ticket có plan `draft` luôn ở `planned`); ticket `backlog` hay `ready` trong một đợt là dấu hiệu quên cập nhật, nên để báo lỗi.
3. **`git fetch`.** Đề xuất: script không tự `git fetch` (chạy được không có mạng, test không cần remote); tài liệu bảo AI chạy `git fetch origin` trước, và dòng lỗi khi không đọc được diff nhắc điều đó.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0048). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

Chưa có.
