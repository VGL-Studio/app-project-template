---
id: T-078
title: Runner tự host cho các job nặng, có đường quay về runner của GitHub
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: approved
risk: high
size: L
depends_on: [T-076, T-077]
sprint:
plan: features/quy-trinh/plans/T-078-runner-tu-host.md
auto_run: false
created: 2026-10-29
---

# T-078: RUNNER TỰ HOST CHO CÁC JOB NẶNG, CÓ ĐƯỜNG QUAY VỀ RUNNER CỦA GITHUB

## Mô tả

Sau T-074 tới T-077, phút còn lại chủ yếu là `build-and-test` của PR code, `main-guard`, test emulator, `device-session` và `staging`. Chạy các job này trên máy của nhóm (runner tự host của tổ chức VGL-Studio) thì GitHub không tính phút, giảm phần lớn chi phí còn lại cho mọi app (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 1). `auto_run: false`: người chọn và dựng máy, cấp quyền cho runner trên GitHub.

## Phạm vi

- Người: chọn máy (câu hỏi bên dưới), cài runner cho cả tổ chức với nhãn riêng (ví dụ `vgl-android`), giới hạn cho các repo app; ghi cách cài vào `docs/ops/self-hosted-runner.md`.
- Các job nặng đọc nhãn runner từ biến tổ chức (ví dụ `runs-on: ${{ vars.HEAVY_RUNNER || 'ubuntu-latest' }}`): máy hỏng hay bảo trì thì xóa biến là quay về runner của GitHub, không sửa workflow. Job nhẹ (lịch định kỳ, `start-run`, `status-page`) giữ runner của GitHub.
- Bước của workflow giả định máy sạch (giải phóng đĩa, bật KVM bằng `sudo`, cài Java) chạy được trên cả hai loại máy, hoặc bỏ qua trên máy tự host.
- `scripts/check.sh` không để lại trạng thái giữa hai lần chạy (thư mục `build/`, ảnh screenshot) làm lệch kết quả; mỗi job chạy trong thư mục làm việc sạch.
- An toàn: runner chạy lâu dùng chung máy cho code của PR (`pr-check`) và job có quyền ghi hay bí mật (`main-guard` có `contents: write`, `issues: write`; `staging` dùng `STAGING_KEYSTORE_*`, `STAGING_APP_DISTRIBUTION_CREDENTIALS`). PR có thể để lại init script trong `~/.gradle` làm sai cổng chất lượng hay đọc bí mật của job sau. Plan chọn: runner chạy một lần (ephemeral), hay dọn `~/.gradle` và thư mục làm việc sau mỗi job, hay tách runner của job có bí mật và quyền ghi; `staging` giữ runner của GitHub nếu không tách được.
- `docs/ops/failure-playbook.md`: thêm dòng cho lỗi runner tự host (máy tắt, hết đĩa, job chờ runner quá lâu).

**Không bao gồm:**

- Runner cho `scripts/release.sh` và bản production (ADR-0009, ADR-0017).

## Câu hỏi cho người

1. Máy nào chạy runner? (a) Máy Linux hay Mac sẵn có ở văn phòng: không tốn tiền thuê, nhưng phụ thuộc điện, mạng và máy bật. (b) Máy chủ thuê có KVM (ví dụ máy chủ riêng 8 nhân, khoảng 40 tới 60 USD mỗi tháng): chạy được emulator x86 như hiện nay. (c) Chỉ chuyển build và test JVM sang máy tự host, emulator vẫn ở GitHub. AI đề xuất (b), hoặc (a) nếu có máy Linux x86 bật 24/7.
2. Một máy cho mọi app, hay mỗi app một máy? AI đề xuất một máy cho cả tổ chức, chạy hai job cùng lúc.
3. Kiểm lại chính sách giá của GitHub cho runner tự host lúc làm ticket (có thời điểm GitHub công bố thu phí nền tảng theo phút rồi hoãn).
4. Gói GitHub Free có cho tạo nhóm runner (runner group) để giới hạn runner theo repo không? Không có thì runner của tổ chức dùng được cho mọi repo, kể cả repo công khai nếu có.

## Rủi ro sơ bộ

Sửa nhiều file `.github/workflows/`: plan rủi ro cao, duyệt chéo. Máy tự host tắt thì mọi job nặng chờ mãi: cần biến quay về và cảnh báo (`run-watchdog` hay `wait-run.sh` báo job chờ runner quá lâu). Máy giữ mã nguồn và token tạm của các repo private: chỉ người trong tổ chức dùng, không cho repo công khai dùng runner này.

## Định nghĩa hoàn thành

- [ ] Các job nặng chạy trên runner tự host; lịch sử tính tiền không còn phút của chúng.
- [ ] Xóa biến tổ chức thì các job đó chạy lại trên runner của GitHub (thử một lần, ghi vào plan).
- [ ] Tài liệu cài và vận hành máy, dòng bảng tra lỗi.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
