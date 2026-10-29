---
id: T-051
title: Lịch định kỳ không cấp runner khi không có phiên tự chạy
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-126 (issue #275), không có spec nghiệp vụ
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

# T-051: LỊCH ĐỊNH KỲ KHÔNG CẤP RUNNER KHI KHÔNG CÓ PHIÊN TỰ CHẠY

## Mô tả

`resume-run.yml` (mỗi giờ), `run-watchdog.yml` (mỗi 30 phút) và `env-canary.yml` (hằng ngày) chạy cả khi không có phiên tự chạy, mỗi job tính tối thiểu 1 phút Actions. Với gói GitHub Free (2.000 phút mỗi tháng cho cả org), riêng `resume-run` và `env-canary` của một app đã tốn khoảng 780 phút mỗi tháng, gần 40% hạn mức; từ 2 đến 3 app trở lên thì các lịch rảnh vượt hạn mức (L-126, issue #275). Sau ticket này, các lịch không cấp runner khi không có phiên cần chúng, mà vẫn làm đúng việc khi có phiên.

## Phạm vi

- Chọn cách nhận biết "có phiên cần lịch" mà không tốn một job: biến repo (`vars.AUTO_RUN_ACTIVE`) do `start-run.yml` bật và PR báo cáo cuối phiên tắt, điều kiện `if:` ở cấp job, hay cách khác. Ghi so sánh vào plan, kể cả trường hợp phiên dừng giữa chừng mà biến vẫn bật.
- Áp cho `resume-run.yml`, `run-watchdog.yml`.
- Cập nhật `docs/ops/staging-and-runs.md` và `docs/auto-run.md` theo cách đã chọn.

**Không bao gồm:**

- Ghi số phút và cách tắt bằng tay vào tài liệu: PR tài liệu riêng của L-126.
- `env-canary.yml`: giữ nguyên. Nó kiểm môi trường để chuẩn bị cho phiên sau (`start-run` bị chặn khi có `env-broken`), vốn bỏ qua ngày có phiên đang chạy, và chỉ tốn khoảng 60 phút mỗi tháng.

## Rủi ro sơ bộ

Sửa `.github/workflows/` (file "Không được làm" của `CLAUDE.md`), cần plan rủi ro cao được duyệt chéo. Lịch tắt nhầm thì phiên hết hạn mức không được tiếp tục và phiên đứng im không được báo. Phiên tự chạy có thể không đẩy được thay đổi `.github/workflows/` (quyền `workflow`, PB-14, ADR-0032): danh sách chạy có plan này thì đầu phiên chạy `scripts/push-probe.sh`.

## Định nghĩa hoàn thành

- [ ] Không có phiên `running` thì các lịch đã chọn không cấp runner; có thể kiểm bằng lịch sử chạy trên GitHub.
- [ ] Có phiên `running` thì `resume-run` và `run-watchdog` làm như cũ. Plan nêu điều kiện cụ thể (biến hay điều kiện `if:` nào) và test trong `scripts/tests/run.sh` cho cả hai phía: có phiên thì chạy, không có thì không cấp runner.
- [ ] Tài liệu ghi cách làm mới.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
