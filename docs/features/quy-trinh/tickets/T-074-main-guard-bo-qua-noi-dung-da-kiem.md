---
id: T-074
title: main-guard không chạy lại cổng chất lượng cho nội dung đã kiểm ở PR
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: false               # làm có người: phiên tự chạy dựa vào chính cổng chất lượng mà ticket sửa (như T-051)
created: 2026-10-29
---

# T-074: MAIN-GUARD KHÔNG CHẠY LẠI CỔNG CHẤT LƯỢNG CHO NỘI DUNG ĐÃ KIỂM Ở PR

## Mô tả

`main-guard.yml` chạy lại `scripts/check.sh` cho mọi commit trên nhánh chính trừ commit chỉ đổi `docs/**` hay `**/*.md` (kể cả commit chỉ đổi `.github/**`, `.claude/**`), tốn khoảng 21% phút Actions của dự án mẫu (757 phút từ 2026-10-22 tới 2026-10-29). `merge-if-green.sh` đã bắt PR cập nhật theo nhánh chính và xanh trên đúng commit cuối, rồi merge kiểu squash, nên commit merge thường có nội dung giống hệt commit đã kiểm. Sau ticket này, `main-guard` chỉ chạy cổng chất lượng khi commit trên nhánh chính khác phần đã kiểm ở PR ngoài tài liệu (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 2).

## Phạm vi

- `main-guard.yml` tìm PR của commit merge và commit cuối của PR có `build-and-test` xanh **đã đi nhánh chạy `check.sh`** (bộ lọc `code` bật; PR chỉ chạy `scripts/tests/run.sh` không tính là đã kiểm), rồi so nội dung bằng `git diff --name-only <commit đã kiểm> <commit trên main>`:
  - Rỗng: không chạy `check.sh`, ghi "đã kiểm ở PR #N, commit <sha>".
  - Chỉ gồm `docs/**`, `**/*.md` (nhánh chính có thêm tài liệu sau lần kiểm, T-077): chỉ chạy `scripts/tests/run.sh`, vì nó kiểm cả tài liệu (số bài học, `foundation.md`, danh sách chạy, quét bí mật) và tổ hợp tài liệu của PR với tài liệu mới của nhánh chính chưa ai kiểm.
- Luôn chạy `check.sh` đầy đủ: đẩy thẳng lên main, không tìm được PR, không có lần kiểm như trên, khác nội dung ngoài tài liệu, chạy tay (`workflow_dispatch`), và commit chạm `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/check.sh`, `scripts/failure-kind.sh`, `scripts/find-breaking-commit.sh` (PR sửa workflow được kiểm bằng chính bản đã sửa; `main-guard` sau merge là lần chạy thật đầu tiên của bản mới).
- Bỏ qua là một bước bên trong job `guard`, không phải `if:` ở cấp job: `find-breaking-commit.sh` và `main-guard-status.sh` đọc check run tên `guard` (`CHECK_NAME="guard"`), job bị skip thì script đầu chờ như `pending` tới 40 phút và script sau báo "CHƯA KIỂM", phiên tự chạy không bắt đầu được.
- Chỗ đọc kết quả `main-guard` phải hiểu đúng kết quả mới: `find-breaking-commit.sh`, `main-guard-status.sh`, `start-run.yml`, phần tự đóng issue `main-broken` (ADR-0025), `docs/auto-run.md` bước 9, và `scripts/release.sh` dòng khoảng 187 (chỉ build production từ commit có `main-guard` `success`). `release.sh` không sửa (`CLAUDE.md`); ADR ghi rõ "xanh vì đã kiểm ở PR" cũng là cổng cho bản production và người chấp nhận nghĩa mới này khi duyệt ADR. Plan tìm thêm bằng `grep -rn main-guard scripts/ .github/ docs/`.
- ADR mới thay một phần mục 2 của ADR-0010 ("chạy lại cổng chất lượng cho từng commit"), sinh lại `docs/foundation.md`.
- Test trong `scripts/tests/run.sh` cho phần so nội dung (tách thành script, ví dụ `scripts/main-guard-verified.sh`, dùng `fake-gh`).

**Không bao gồm:**

- Chạy test emulator sau merge: T-075 (cùng sửa `main-guard.yml`, làm sau ticket này).

## Rủi ro sơ bộ

Sửa `.github/workflows/main-guard.yml` và có thể `scripts/main-guard-status.sh`, `scripts/find-breaking-commit.sh` (file "Không được làm" của `CLAUDE.md`): plan rủi ro cao, duyệt chéo. Plan phải liệt kê mọi kiểm tra hiện có mà trường hợp mới đi qua, mỗi kiểm tra một test (L-058). Mất một lớp phát hiện test chập chờn trên main (ADR-0025 ghi test chập chờn từ lần chạy lại của `main-guard`): plan nêu cách bù, ví dụ `pr-check` ghi nhận tương tự, hoặc chấp nhận và ghi vào ADR.

## Định nghĩa hoàn thành

- [ ] Commit merge từ PR đã kiểm, cùng nội dung: job `guard` xong trong dưới 1 phút, log ghi số PR và commit đã kiểm. Chỉ khác tài liệu: chỉ chạy `scripts/tests/run.sh`.
- [ ] Đẩy thẳng lên main, nội dung khác ngoài tài liệu, PR chỉ chạy test của script, hay commit chạm workflow và script của cổng: `main-guard` chạy `check.sh` như cũ. Có test cho từng trường hợp.
- [ ] `find-breaking-commit.sh`, `main-guard-status.sh`, `start-run.yml`, tự đóng `main-broken` vẫn đúng với kết quả mới (test); `release.sh` không đổi.
- [ ] ADR mới `accepted`, `foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
