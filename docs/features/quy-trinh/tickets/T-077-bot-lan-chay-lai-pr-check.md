---
id: T-077
title: Bớt lần chạy lại pr-check: PR nháp và nhánh chính chỉ đổi tài liệu
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-074, T-076]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-077: BỚT LẦN CHẠY LẠI PR-CHECK: PR NHÁP VÀ NHÁNH CHÍNH CHỈ ĐỔI TÀI LIỆU

## Mô tả

`pr-check` chạy trung bình 1,7 lần mỗi PR: mỗi lần đẩy thêm commit, và mỗi lần `merge-if-green.sh --update` cập nhật nhánh vì nhánh chính có commit mới, kể cả commit chỉ đổi tài liệu (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 5). Sau ticket này, PR nháp không chạy `pr-check`, và PR code không phải cập nhật nhánh rồi chạy lại khi nhánh chính chỉ đổi tài liệu từ lúc PR được kiểm.

## Phạm vi

- `pr-check.yml`: thêm loại sự kiện `ready_for_review`, bỏ qua PR nháp ở cấp job. `merge-if-green.sh` đã từ chối PR nháp nên không cần kiểm thêm; plan kiểm PR nháp của phiên (PR plan, PR sửa nguyên nhân) và các chỗ đang chờ CI của PR nháp (`docs/auto-run.md`).
- `merge-if-green.sh`: khi nhánh chậm hơn nhánh chính và mọi commit của nhánh chính từ điểm tách tới nay chỉ đổi tài liệu (cùng tập đường dẫn `paths-ignore` của `main-guard`), merge mà không cập nhật nhánh. Squash merge với mã commit đã kiểm vẫn giữ. Xung đột thì vẫn cập nhật.
- `main-guard` của T-074 so nội dung commit merge với commit đã kiểm, phần khác chỉ là tài liệu thì vẫn bỏ qua: plan kiểm hai ticket khớp nhau.
- Sửa ADR-0010 mục 1 ("nhánh của PR đã cập nhật theo nhánh chính mới nhất") bằng ADR mới hay mục mới của ADR của T-074.

**Không bao gồm:**

- Đổi `concurrency` của `pr-check` (đã hủy lần chạy cũ khi đẩy commit mới).

## Rủi ro sơ bộ

Sửa `scripts/merge-if-green.sh` và `.github/workflows/pr-check.yml`: plan rủi ro cao, duyệt chéo, mỗi kiểm tra của script merge một test (L-058). Sai điều kiện "chỉ tài liệu" thì code chưa kiểm cùng code mới của main vào main mà không chạy lại; `main-guard` vẫn bắt được vì nội dung khác phần đã kiểm.

## Định nghĩa hoàn thành

- [ ] PR nháp không chạy `pr-check`; chuyển sang sẵn sàng thì chạy.
- [ ] Nhánh chính chỉ có thêm commit tài liệu: `merge-if-green.sh` merge không cập nhật nhánh. Có commit code: vẫn cập nhật và chờ CI. Test cho cả hai.
- [ ] ADR cập nhật, `foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
