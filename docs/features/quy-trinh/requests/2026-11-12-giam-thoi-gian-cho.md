---
date: 2026-11-12
author: Nhật (lời nói trong phiên, AI ghi lại)
feature: quy-trinh
status: specified            # ticket quy trình, không có spec nghiệp vụ: T-104
---

# YÊU CẦU: GIẢM THỜI GIAN CHỜ CỦA QUY TRÌNH

## Mình muốn gì

Việc triển khai các tính năng con đang chậm. Tìm bước nào cải tiến được để quy trình chạy nhanh hơn, nhất là giảm thời gian chờ (lời anh Nhật: "hay xac định xem liệu chung ta co thể cải tiến ở bước nao để giup cho quy trinh chạy nhanh hơn. Đặc biệt la giảm thiểu thời gian chờ").

## Vì sao

Đo ngày 2026-11-12 trên lịch sử git, 200 PR gần nhất và dòng thời gian của năm phiên gần nhất:

- Máy chạy mỗi task khoảng 45 tới 60 phút: phiên con 20 tới 35 phút, rà soát 3 tới 15 phút, chờ `pr-check` 9 tới 15 phút. PR code của task mở trung bình 0,2 giờ là merge.
- Từ ticket tới merge mất từ 4 giờ (T-101, T-102) tới hơn 200 giờ (T-076 tới T-078, T-082, T-083). Phần lớn là chờ người: ticket chờ có plan, plan chờ duyệt, danh sách chạy chờ duyệt rồi chờ người bấm `start-run`.

## Các việc anh Nhật đã duyệt

Lời duyệt: "Duyệt đề xuất mới: làm việc 1, viết ADR proposed cho việc 2 và 4, viết ticket và plan cho việc 3, bỏ B6 và B7", rồi "Rút ADR-0049. Viết lại ADR-0048 thành ADR hợp nhất thay ADR-0034. Làm tiếp việc 3".

1. Báo ticket chờ người đang chặn hàng chờ plan: đã làm ở PR #476.
2. Duyệt plan và danh sách chạy một lần: ADR-0048 (`proposed`), PR #477.
3. **Phiên tự bắt đầu khi danh sách chạy `approved` lên nhánh chính**, không cần người bấm `start-run`; danh sách có ô chọn để giữ cách bấm tay (ví dụ muốn chờ hạn mức được làm mới). Việc này sửa `.github/workflows/`, nên đi bằng ticket và plan rủi ro cao: T-104.
4. Gối task khi chờ CI: đã rút, vì điều kiện của nó loại hết các cặp task liền nhau đã đo.
