---
id: T-095
title: Runner tự host: dòng lỗi của runner-check.sh vào build.log, ngưỡng thời gian cho job queued của resume-check.py
feature: quy-trinh
spec:                        # ticket quy trình: nguồn là ghi chú rà soát T-078 (báo cáo RUN-20261108-01); không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa .github/workflows/: plan rủi ro cao, duyệt chéo
size: S
depends_on: [T-078]
sprint:
plan: docs/features/quy-trinh/plans/T-095-runner-check-log-va-queued.md
auto_run:                    # để trống
created: 2026-11-08
---

# T-095: RUNNER TỰ HOST: DÒNG LỖI CỦA RUNNER-CHECK.SH VÀO BUILD.LOG, NGƯỠNG THỜI GIAN CHO JOB QUEUED CỦA RESUME-CHECK.PY

## Mô tả

Hai ghi chú rà soát của T-078 mà anh Nhật chọn làm thành một ticket `backlog` (2026-11-08: "một ticket backlog cho build.log và queued"). Chỉ có ảnh hưởng khi repo đặt biến `HEAVY_RUNNER`, nên không gấp.

1. Khi bước `Kiểm runner tự host (scripts/runner-check.sh)` của job nặng đỏ, dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` không vào `build.log`, nên bình luận lỗi mà CI viết vào PR in "Không có log." thay vì dòng lỗi.
2. `scripts/resume-check.py` đã không tiếp tục phiên khi có job chờ runner tự host, nhưng không có ngưỡng thời gian: job `queued` chờ mãi (máy runner tắt) thì phiên đứng im mà không ai được báo, tới khi `run-watchdog` thấy phiên không có commit.

## Phạm vi

- `pr-check.yml`: đầu ra của `runner-check.sh` vào `build.log` (tệp mà bước bình luận lỗi trên PR đọc), có test của `scripts/tests/run.sh` theo cách các test workflow có sẵn. `main-guard.yml` cũng đọc `build.log` cho nội dung issue: plan nói rõ có làm cùng cách ở đó không.
- `scripts/resume-check.py`: job `queued` quá một ngưỡng (plan đề xuất, ví dụ bằng ngưỡng của `wait-run.sh` mã 7) thì báo người: in một dòng có nhãn riêng mà `resume-run.yml` đưa vào issue (plan chọn mở issue mới hay dùng `session-stalled`), có test kèm đối chứng job `queued` dưới ngưỡng.

**Không bao gồm:**

- Kiểm thêm `sudo`, `gh`, `jq` trong `runner-check.sh` (câu hỏi 8 của plan T-078: ticket riêng nếu cần).

## Rủi ro sơ bộ

Sửa `.github/workflows/`: điểm chặn 7 (`ci`), plan rủi ro cao, duyệt chéo; phiên cần `workflow-push`.

## Định nghĩa hoàn thành

- [ ] Bình luận lỗi trên PR có dòng `RUNNER KHÔNG DÙNG ĐƯỢC:` khi bước kiểm runner đỏ.
- [ ] `resume-check.py` xử lý job `queued` quá ngưỡng, có test kèm đối chứng.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, `main-guard` xanh.
