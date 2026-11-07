---
id: T-076
title: pr-check bỏ job changes riêng
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-075]
sprint:
plan: features/quy-trinh/plans/T-076-pr-check-mot-job.md
auto_run:                    # để trống từ 2026-11-01: anh Nhật quyết chạy trong phiên tự chạy RUN-20261101-02 (lời anh: "Không, hãy chạy phiên"; chọn "Cả ba, nối tiếp"; sau rà soát chỉ T-076 chạy ở RUN-20261101-02, T-077, T-078 để phiên sau); trước đó false: làm có người vì phiên dựa vào chính cổng chất lượng mà ticket sửa
created: 2026-10-29
---

# T-076: PR-CHECK BỎ JOB CHANGES RIÊNG

## Mô tả

Job `changes` của `pr-check.yml` chỉ chạy 4 giây để lọc đường dẫn, nhưng mỗi job tính tối thiểu 1 phút: 309 phút một tuần, khoảng 9% (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 4). Sau ticket này, bước lọc đường dẫn nằm trong job `build-and-test`, `pr-check` chỉ còn một job.

## Phạm vi

- Chuyển bước `dorny/paths-filter` (hay `git diff` theo nhánh gốc) vào đầu `build-and-test`; các bước sau đọc `steps.filter.outputs`.
- Sửa chỗ đọc tên job `changes`: `merge-if-green.sh` (`REQUIRED_CHECKS=("changes" "build-and-test")`), chú thích đầu `pr-check.yml`, dữ liệu test ở `scripts/tests/run.sh` (khoảng dòng 319, 416, 704). Plan tìm thêm bằng `grep -rn '"changes"\|changes:' scripts/ .github/ docs/ops/`.

**Không bao gồm:**

- Bỏ `ui-test`: T-075 (cùng file, làm trước ticket này để không xung đột).

## Rủi ro sơ bộ

Sửa `.github/workflows/pr-check.yml`: plan rủi ro cao theo `CLAUDE.md`, dù thay đổi nhỏ. Lọc sai thì PR code chỉ chạy test của script và vẫn xanh: test phải có PR mẫu chạm code.

## Định nghĩa hoàn thành

- [ ] `pr-check` của PR chỉ tài liệu và PR code đều chỉ có một job, PR code vẫn chạy `check.sh` (kiểm trên PR của chính ticket này).
- [ ] Script merge và chờ CI không đọc job `changes`, có test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
