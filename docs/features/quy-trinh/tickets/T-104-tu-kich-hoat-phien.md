---
id: T-104
title: Phiên tự bắt đầu khi danh sách chạy approved có start auto lên nhánh chính
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-11-12-giam-thoi-gian-cho.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: approved              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa .github/workflows/start-run.yml: điểm chặn 7, plan rủi ro cao, duyệt chéo
size: M
depends_on: []
sprint:
plan: features/quy-trinh/plans/T-104-tu-kich-hoat-phien.md
auto_run: false              # làm có người: sửa chính workflow mở phiên, và đẩy thay đổi .github/workflows/ cần quyền workflow (mục 8 của plan, câu 2)
created: 2026-11-12
---

# T-104: PHIÊN TỰ BẮT ĐẦU KHI DANH SÁCH CHẠY APPROVED CÓ START AUTO LÊN NHÁNH CHÍNH

## Mô tả

Hiện nay danh sách chạy được duyệt và merge xong vẫn phải chờ dev vào GitHub bấm **Actions > start-run > Run workflow**. Có lần danh sách đã duyệt nằm chờ nhiều ngày, và mỗi lần dev phải quay lại thêm một bước. Sau ticket này, danh sách ghi `start: auto` thì phiên tự bắt đầu ngay khi lần merge làm nó `approved` lên nhánh chính, qua đúng các kiểm tra của `start-run.yml`. Danh sách ghi `start: manual` (hay không ghi) thì giữ cách bấm tay như cũ (yêu cầu `requests/2026-11-12-giam-thoi-gian-cho.md`, việc 3).

## Phạm vi

- Danh sách chạy có trường `start:` (`auto` hay `manual`); mẫu mặc định `auto`. `scripts/run-check.py` kiểm giá trị, và `start: auto` chỉ đi với `open_with: routine`.
- Script mới chọn danh sách cần tự bắt đầu từ một lần push lên nhánh chính: chỉ danh sách mà chính lần push đó chuyển sang `approved`, có `start: auto` và `open_with: routine`. Cùng script chặn mọi lần mở phiên, cả lần bấm tay, khi danh sách trên đầu nhánh chính không còn `approved`, khi danh sách đã có nhánh báo cáo, khi có phiên khác đang chạy, hay khi vừa kích hoạt một phiên cho một danh sách chưa có nhánh báo cáo.
- `start-run.yml` chạy cả khi có push lên nhánh chính đổi `docs/runs/RUN-*.md`; giữ chạy tay như cũ. Mọi kiểm tra trước khi kích hoạt giữ nguyên; `main-guard` đang chạy thì chờ có giới hạn thay vì đỏ ngay.
- Tài liệu: bước kích hoạt phiên (`docs/auto-run.md`, `docs/ops/staging-and-runs.md`, `docs/ops/setup-new-app.md`), `docs/ops/to-app.md`, mục Khi lập danh sách chạy, mẫu danh sách chạy, `/run-list`, bảng kiểm tra của `docs/ops/pre-run-review.md`.

**Không bao gồm:**

- Hẹn giờ bắt đầu (`start_at`). Muốn chờ hạn mức được làm mới thì ghi `start: manual` và bấm tay hay hẹn giờ routine như cũ.
- Tự bắt đầu danh sách đang chờ vì có phiên khác chạy, sau khi phiên kia xong. Lúc đó dev bấm tay.
- Đổi `resume-run.yml`, `status-page.yml` hay prompt của routine.

## Rủi ro sơ bộ

Sửa `.github/workflows/start-run.yml`, điểm chặn 7 (`ci`): plan rủi ro cao, duyệt chéo. Sai hướng thì một lần push mở hai phiên (tốn hạn mức, hai phiên cùng đẩy), hay mở phiên cho danh sách không định chạy. Mỗi lần gọi API kích hoạt thành công là một phiên mới và không chống trùng (ADR-0016), nên điều kiện chọn danh sách phải chặt và có test.

## Định nghĩa hoàn thành

- [ ] Merge một danh sách `approved` có `start: auto`, `open_with: routine` thì `start-run` tự chạy và kích hoạt routine đúng một lần.
- [ ] Lần push sau đó đổi cùng file (ví dụ PR báo cáo) không kích hoạt lại, và lần bấm tay ngay sau đó bị từ chối.
- [ ] `start: manual` hay không có trường: không tự chạy.
- [ ] `scripts/tests/run.sh` có test cho script chọn danh sách, cho `run-check.py` và cho cấu trúc mới của `start-run.yml`.
