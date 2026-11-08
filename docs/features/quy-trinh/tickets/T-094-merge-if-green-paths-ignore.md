---
id: T-094
title: merge-if-green.sh xét "main-guard có chạy không" theo paths-ignore, không cảnh báo sai khi xóa nhánh; hai chỗ hở của pr_sim
feature: quy-trinh
spec:                        # ticket quy trình: nguồn là báo cáo RUN-20261108-01 (điểm nên sửa của T-077), bài học L-149, L-152 và ghi chú rà soát T-076; không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa scripts/merge-if-green.sh: plan rủi ro cao, duyệt chéo
size: M
depends_on: [T-077]
sprint:
plan: docs/features/quy-trinh/plans/T-094-merge-if-green-paths-ignore.md
auto_run:                    # để trống: chạy trong phiên tự chạy, như T-077
created: 2026-11-08
---

# T-094: MERGE-IF-GREEN.SH XÉT "MAIN-GUARD CÓ CHẠY KHÔNG" THEO PATHS-IGNORE, KHÔNG CẢNH BÁO SAI KHI XÓA NHÁNH; HAI CHỖ HỞ CỦA PR_SIM

## Mô tả

Ba sửa nhỏ cho các script của cổng merge, gom từ retro của RUN-20261108-01 (anh Nhật chọn ngày 2026-11-08: "Chọn (a): ticket T-094 gộp L-149 và hai ghi chú pr_sim").

1. `docs_only_behind` (bước 3) của `scripts/merge-if-green.sh` xét "PR chỉ đổi file mà `main-guard` bỏ qua" bằng `main-guard-verified.sh --docs-only`, lệnh coi tài liệu mà Gradle đọc (`GRADLE_READ_DOCS`) là code. Việc `main-guard` có chạy hay không lại do `paths-ignore` của `main-guard.yml` quyết (`docs/**`, `**/*.md`). Ca lọt: PR chỉ đổi `feature/x/README.md` và `docs/remote-config-keys.md` được merge không cập nhật nhánh, và `main-guard` không chạy trên commit merge, nên tổ hợp tài liệu đó không được kiểm ở đâu (L-152).
2. Sau merge, `merge-if-green.sh` in "không xóa được nhánh" dù GitHub đã tự xóa nhánh ("Automatically delete head branches"); người đọc báo cáo tưởng phải dọn tay (L-149).
3. Hai chỗ hở của hàm `pr_sim` và `pr_filter_check` trong `scripts/tests/run.sh` (ghi chú rà soát T-076, RUN-20261101-02).

## Phạm vi

- `scripts/merge-if-green.sh`, `docs_only_behind` bước 3: xét theo đúng `paths-ignore` của `main-guard.yml` (đọc từ workflow, hay một danh sách dùng chung có test giữ hai bên khớp; plan chọn). Test 8 của T-077 thêm ca `feature/x/README.md` cùng `docs/remote-config-keys.md`, mong mã 3 (cập nhật nhánh).
- `scripts/merge-if-green.sh`, bước xóa nhánh: lệnh xóa thất bại thì đọc lại nhánh vài lần trong khoảng ngắn (ví dụ 3 lần, mỗi 5 giây); nhánh đã mất thì không in cảnh báo. Test với nhánh mất ở lần đọc thứ hai, và đối chứng nhánh còn sau mọi lần đọc (vẫn cảnh báo).
- `scripts/tests/run.sh`, `pr_sim`: dòng mẫu của bộ lọc không có nháy đơn (ví dụ `- docs/**`) hiện bị bỏ qua im lặng, nên câu P6b có thể xanh nhầm. In `ERROR mẫu chưa hỗ trợ` cho mọi dòng không khớp `- '...'`, có test.
- `scripts/tests/run.sh`, `pr_filter_check`: ngừng đọc khối `run` ở dòng trống đầu tiên (sai theo chiều an toàn). Đọc tới hết khối, có test với một dòng trống giữa khối.

**Không bao gồm:**

- Đổi `paths-ignore` của `main-guard.yml` hay `GRADLE_READ_DOCS`.
- Quy tắc cho người viết plan về hai quy tắc này (`docs/CLAUDE.md`, mục Khi viết plan, PR #420).

## Rủi ro sơ bộ

Sửa `scripts/merge-if-green.sh`, điểm chặn 7 (`ci`): plan rủi ro cao, duyệt chéo; mỗi kiểm tra có sẵn của script mà ca mới đi qua một test (`docs/CLAUDE.md`, mục Khi viết plan). Sai hướng ngược lại (xét rộng hơn `paths-ignore`) chỉ làm PR cập nhật nhánh thừa, không mở chỗ hở.

## Định nghĩa hoàn thành

- [ ] PR chỉ đổi file trong `paths-ignore` mà có file của `GRADLE_READ_DOCS` được cập nhật nhánh trước khi merge; PR code vẫn merge không cập nhật như T-077.
- [ ] Không còn cảnh báo "không xóa được nhánh" khi GitHub đã tự xóa nhánh.
- [ ] `pr_sim` báo lỗi với dòng mẫu chưa hỗ trợ; `pr_filter_check` đọc hết khối `run`.
- [ ] Có test tự động cho từng điểm, kèm đối chứng.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, `main-guard` xanh.
