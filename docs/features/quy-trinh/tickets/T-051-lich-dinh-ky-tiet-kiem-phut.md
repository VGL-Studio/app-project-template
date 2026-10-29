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
created: 2026-10-29
---

# T-051: LỊCH ĐỊNH KỲ KHÔNG CẤP RUNNER KHI KHÔNG CÓ PHIÊN TỰ CHẠY

## Mô tả

`resume-run.yml` (mỗi giờ), `run-watchdog.yml` (mỗi 30 phút) và `env-canary.yml` (hằng ngày) chạy cả khi không có phiên tự chạy, mỗi job tính tối thiểu 1 phút Actions. Với gói GitHub Free (2.000 phút mỗi tháng cho cả org), vài app đã vượt hạn mức chỉ vì các lịch rảnh (L-126, issue #275). Sau ticket này, các lịch không cấp runner khi không có phiên cần chúng, mà vẫn làm đúng việc khi có phiên.

## Phạm vi

- Chọn cách nhận biết "có phiên cần lịch" mà không tốn một job: biến repo (`vars.AUTO_RUN_ACTIVE`) do `start-run.yml` bật và PR báo cáo cuối phiên tắt, điều kiện `if:` ở cấp job, hay cách khác. Ghi so sánh vào plan, kể cả trường hợp phiên dừng giữa chừng mà biến vẫn bật.
- Áp cho `resume-run.yml`, `run-watchdog.yml`. Xét riêng `env-canary.yml`: phần `setup-check` có ích cả khi không có phiên.
- Cập nhật `docs/ops/staging-and-runs.md` và `docs/auto-run.md` theo cách đã chọn.

**Không bao gồm:**

- Ghi số phút và cách tắt bằng tay vào tài liệu: làm ngay ở PR của L-126.

## Rủi ro sơ bộ

Sửa `.github/workflows/` (file "Không được làm" của `CLAUDE.md`), cần plan rủi ro cao được duyệt chéo. Lịch tắt nhầm thì phiên hết hạn mức không được tiếp tục và phiên đứng im không được báo.

## Định nghĩa hoàn thành

- [ ] Không có phiên `running` thì các lịch đã chọn không cấp runner; có thể kiểm bằng lịch sử chạy trên GitHub.
- [ ] Có phiên `running` thì `resume-run` và `run-watchdog` làm như cũ; test của script hay workflow chứng minh.
- [ ] Tài liệu ghi cách làm mới.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
