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
auto_run: 
created: 2026-10-29
---

# T-074: MAIN-GUARD KHÔNG CHẠY LẠI CỔNG CHẤT LƯỢNG CHO NỘI DUNG ĐÃ KIỂM Ở PR

## Mô tả

`main-guard.yml` chạy lại `scripts/check.sh` cho mọi commit code trên nhánh chính, tốn khoảng 21% phút Actions của dự án mẫu (757 phút từ 2026-10-22 tới 2026-10-29). `merge-if-green.sh` đã bắt PR cập nhật theo nhánh chính và xanh trên đúng commit cuối, rồi merge kiểu squash, nên commit merge thường có nội dung giống hệt commit đã kiểm. Sau ticket này, `main-guard` chỉ chạy cổng chất lượng khi commit trên nhánh chính khác phần đã kiểm ở PR ngoài tài liệu (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 2).

## Phạm vi

- `main-guard.yml` tìm PR của commit merge và commit cuối của PR mà `pr-check` đã xanh, rồi so nội dung: `git diff --name-only <commit đã kiểm> <commit trên main>` rỗng, hoặc chỉ gồm đường dẫn mà `paths-ignore` của `main-guard` đã bỏ (`docs/**`, `**/*.md`), thì ghi kết quả xanh "đã kiểm ở PR #N" và không chạy `check.sh`.
- Mọi trường hợp khác vẫn chạy như cũ: đẩy thẳng lên main, không tìm được PR, `pr-check` của commit đó không xanh, khác nội dung code, chạy tay (`workflow_dispatch`).
- Kết quả "xanh vì đã kiểm" vẫn phải được `find-breaking-commit.sh`, `main-guard-status.sh` và phần tự đóng issue `main-broken` (ADR-0025) hiểu là xanh. Plan kiểm từng chỗ đọc kết quả của `main-guard` (`grep -rn main-guard scripts/ .github/ docs/ops/`).
- ADR mới thay một phần mục 2 của ADR-0010 ("chạy lại cổng chất lượng cho từng commit"), sinh lại `docs/foundation.md`.
- Test trong `scripts/tests/run.sh` cho phần so nội dung (tách thành script, ví dụ `scripts/main-guard-verified.sh`, dùng `fake-gh`).

**Không bao gồm:**

- Chạy test emulator sau merge: T-075 (cùng sửa `main-guard.yml`, làm sau ticket này).

## Rủi ro sơ bộ

Sửa `.github/workflows/main-guard.yml` và có thể `scripts/main-guard-status.sh`, `scripts/find-breaking-commit.sh` (file "Không được làm" của `CLAUDE.md`): plan rủi ro cao, duyệt chéo. Plan phải liệt kê mọi kiểm tra hiện có mà trường hợp mới đi qua, mỗi kiểm tra một test (L-058). Mất một lớp phát hiện test chập chờn trên main (ADR-0025 ghi test chập chờn từ lần chạy lại của `main-guard`): plan nêu cách bù, ví dụ `pr-check` ghi nhận tương tự, hoặc chấp nhận và ghi vào ADR.

## Định nghĩa hoàn thành

- [ ] Commit merge từ PR đã xanh, cùng nội dung code: `main-guard` xanh trong dưới 1 phút, log ghi số PR và commit đã kiểm.
- [ ] Đẩy thẳng lên main, hay nội dung code khác: `main-guard` chạy `check.sh` như cũ. Có test cho cả hai phía.
- [ ] `merge-if-green.sh`, `find-breaking-commit.sh`, tự đóng `main-broken` vẫn đúng với kết quả mới (test).
- [ ] ADR mới `accepted`, `foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
