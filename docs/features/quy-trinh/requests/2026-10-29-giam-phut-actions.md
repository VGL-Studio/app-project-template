---
date: 2026-10-29
author: Nhật (lời nói trong phiên, AI ghi lại)
feature: quy-trinh
status: specified            # ticket quy trình, không có spec nghiệp vụ: T-074 tới T-078
---

# YÊU CẦU: GIẢM CHI PHÍ GITHUB ACTIONS

## Mình muốn gì

Tiếp tục dùng GitHub, nhưng giảm mạnh tiền trả cho GitHub Actions. Anh Nhật đồng ý áp dụng cả sáu cách AI đề xuất (lời trong phiên: "Áp dụng tất cả các cách này").

## Vì sao

Số liệu lấy từ API tính tiền của tổ chức VGL-Studio ngày 2026-10-29:

- Tháng 10 tới ngày 29 dùng khoảng 8.700 phút Linux, vượt 2.000 phút miễn phí của gói Free, phải trả khoảng 40 USD (21,5 USD cho dự án mẫu, 18,5 USD cho camera-pose). Từ 2026-10-23 mỗi ngày khoảng 5 tới 7 USD, tức khoảng 150 tới 200 USD mỗi tháng, và tăng theo số app.
- Phút tính tiền của dự án mẫu từ 2026-10-22 tới 2026-10-29 (179 PR, khoảng 3.540 phút, mỗi job làm tròn lên phút):

| Job | Phút | Tỷ lệ |
| --- | --- | --- |
| `pr-check` / `build-and-test` | 1.378 | 39% |
| `main-guard` / `guard` | 757 | 21% |
| `pr-check` / `ui-test` (emulator) | 637 | 18% |
| `device-session` | 311 | 9% |
| `pr-check` / `changes` (job 4 giây, tính 1 phút) | 309 | 9% |
| `staging` | 151 | 4% |

Các lịch định kỳ (`run-watchdog`, `resume-run`, `env-canary`, `status-page`) gần như không tốn; phần của chúng do T-051 lo. `pr-check` chạy trung bình 1,7 lần mỗi PR.

## Các cách đã chọn

| # | Cách | Ước giảm | Ticket |
| --- | --- | --- | --- |
| 1 | Runner tự host cho các job nặng | 80 tới 95% phần còn lại | T-078 |
| 2 | `main-guard` không chạy lại cổng chất lượng cho commit có nội dung đã kiểm ở PR | khoảng 20% | T-074 |
| 3 | Test emulator chạy sau khi merge, không chạy ở từng PR (chỉ bớt phần chạy lại: khoảng 41% của 18%) | khoảng 7 tới 8% | T-075 |
| 4 | Bỏ job `changes` riêng của `pr-check` | khoảng 9% | T-076 |
| 5 | PR nháp không chạy `pr-check`; không bắt chạy lại khi nhánh chính chỉ đổi tài liệu | 10 tới 20% | T-077 |
| 6 | Đặt giới hạn chi tiêu cho Actions | chặn vượt | người làm, không có ticket |

Cách 2 tới 5 giảm khoảng 35 tới 45% mà không cần máy mới; các cách chồng nhau (T-077 bớt lần chạy lại của job mà T-075, T-076 đã cắt; T-074 vẫn chạy test của script) nên tổng nhỏ hơn cộng từng dòng.

## Những gì chưa cần làm lần này

- Chuyển sang GitLab: đã cân nhắc và bỏ, vì phiên tự chạy (routine của Claude Code) gắn với GitHub.
- Lịch định kỳ rảnh: T-051 (PR #291).
- Đưa thay đổi về camera-pose và các app khác: sau khi merge, bằng `scripts/template-sync.sh` (ADR-0030).

## Việc chỉ người làm

- **Giới hạn chi tiêu (cách 6):** chủ tổ chức vào GitHub, Organization settings → Billing and licensing → Budgets and alerts, tạo budget cho sản phẩm Actions, chọn mức tiền mỗi tháng, bật cảnh báo 75% và 90%. Chọn có dừng khi chạm mức hay không: dừng thì CI ngừng tới tháng sau, kể cả `main-guard`.
- **Máy cho runner tự host (cách 1):** chọn máy và cài runner, xem câu hỏi trong T-078.

## Tham khảo

- ADR-0010 (cổng chất lượng mềm trên GitHub Free), ADR-0025 (nhánh chính tự hồi phục), L-126 (phút Actions của các lịch định kỳ).
