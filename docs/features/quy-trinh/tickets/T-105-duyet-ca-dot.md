---
id: T-105
title: Làm theo ADR-0048 - run-check.py --batch cho danh sách của một đợt, tài liệu duyệt cả đợt và bỏ họp
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là ADR-0048 (mục Hệ quả) và requests/2026-11-12-giam-thoi-gian-cho.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: planned              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quy trình phiên tự chạy: điểm chặn 7, plan rủi ro cao, duyệt chéo
size: M
depends_on: []
sprint:
plan: features/quy-trinh/plans/T-105-duyet-ca-dot.md
auto_run:
created: 2026-11-13
---

# T-105: LÀM THEO ADR-0048: RUN-CHECK.PY --BATCH, TÀI LIỆU DUYỆT CẢ ĐỢT VÀ BỎ HỌP

## Mô tả

ADR-0048 đã được chấp nhận: plan và danh sách chạy của một đợt được duyệt một lần trong một PR, không cần họp, và AI ghi trạng thái duyệt theo lời đồng ý trong phiên của người có quyền duyệt. PR chấp nhận ADR chỉ đổi chỗ dẫn ADR và một dòng của `docs/CLAUDE.md`; phần còn lại của mục Hệ quả chưa làm, nên hôm nay vẫn chưa dùng được cách duyệt cả đợt: `scripts/run-check.py` đòi plan `approved`, và tài liệu vẫn nói "sau họp", "qua họp". Sau ticket này, AI lập được danh sách chạy `draft` cùng PR với các plan `draft` của đợt, kiểm bằng `run-check.py --batch`, và tài liệu nói đúng quy trình mới (yêu cầu `requests/2026-11-12-giam-thoi-gian-cho.md`, việc 2).

## Phạm vi

- `scripts/run-check.py --batch <danh sách>`: bỏ qua lỗi trạng thái (plan chưa `approved`, ticket chưa `approved`, spec chưa `confirmed`) chỉ với plan `draft`, ticket `planned` và spec có trong diff của nhánh so với điểm tách khỏi nhánh chính; mọi kiểm tra khác giữ nguyên; danh sách phải `draft`. Test trong `scripts/tests/run.sh`.
- Tài liệu theo danh sách ở mục Hệ quả của ADR-0048: `docs/README.md`, `docs/CLAUDE.md`, `docs/risk-scoring.md`, `docs/auto-run.md`, `docs/ops/pre-run-review.md`, `docs/ops/quick-lane.md`, `docs/ops/token-usage.md`, `docs/ops/setup-new-app.md`, mẫu plan và mẫu danh sách chạy, các command `run-list`, `write-plan`, `quick-lane`, `spec`, `triage-feedback`, `adr`.

**Không bao gồm:**

- Đổi `merge-if-green.sh`, workflow hay `status.py`.
- Gộp các PR plan đang mở thành một đợt (việc của đợt đầu tiên dùng cách mới).

## Rủi ro sơ bộ

Quy trình phiên tự chạy (điểm chặn 7): plan rủi ro cao, duyệt chéo. Sai hướng thì `--batch` cho qua một plan chưa ai duyệt, hay tài liệu cho AI ghi `approved` khi chưa có lời đồng ý. Phần bỏ qua của `--batch` phải hẹp và có test cho từng trường hợp không được bỏ qua.

## Định nghĩa hoàn thành

- [ ] `run-check.py --batch` qua với danh sách `draft` mà plan `draft`, ticket `planned`, spec chưa `confirmed` đều đổi trong nhánh; vẫn báo lỗi khi file đó không đổi trong nhánh, khi trạng thái khác, khi spec còn câu hỏi chưa trả lời, và với danh sách không `draft`.
- [ ] Không có `--batch`, mọi test có sẵn của `run-check.py` giữ nguyên.
- [ ] Tài liệu quy tắc không còn câu bắt buộc họp hay câu "chỉ dev chuyển trạng thái" trái phần C của ADR-0048; có hướng dẫn duyệt cả đợt.
