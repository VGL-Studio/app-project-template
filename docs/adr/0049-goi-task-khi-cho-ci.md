---
id: ADR-0049
title: Trong phiên tự chạy, task sau được bắt đầu khi PR của task trước đang chờ CI, nếu hai task độc lập và không sửa chung file; mỗi lúc vẫn chỉ một PR chờ merge
status: proposed             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-12
deciders: [Nhật]
supersedes:                  # không thay ADR nào; đổi nguyên tắc 1 "Tuần tự" và bước 1, 3, 7 của docs/auto-run.md, mục Trong phiên tự chạy của docs/CLAUDE.md
superseded_by:
---

# ADR-0049: TRONG PHIÊN TỰ CHẠY, TASK SAU ĐƯỢC BẮT ĐẦU KHI PR CỦA TASK TRƯỚC ĐANG CHỜ CI, NẾU HAI TASK ĐỘC LẬP VÀ KHÔNG SỬA CHUNG FILE; MỖI LÚC VẪN CHỈ MỘT PR CHỜ MERGE

## Bối cảnh

Nguyên tắc 1 của phiên tự chạy là tuần tự: task sau chỉ tạo nhánh sau khi task trước đã merge, để nhánh của task sau có sẵn kết quả của task trước. Dòng thời gian của năm phiên gần nhất cho thấy mỗi task tốn 9 tới 15 phút chờ `pr-check` mà phiên không làm gì: phiên con của task sau chưa được bắt đầu. Ví dụ ở RUN-20261110-01: T-098 đẩy PR lúc 16:50, CI xong 17:05, merge 17:06, rồi T-097 mới tạo nhánh. Tương tự với T-089 tới T-088 ở RUN-20261107-01 và T-093, T-077, T-078 ở RUN-20261108-01. Phần chờ `main-guard` đã chạy song song với task sau, nên chỉ còn khoảng chờ `pr-check` là lãng phí.

Phần lớn các task liền nhau trong một danh sách không đụng nhau: `scripts/run-check.py` đã biết khi hai plan cùng sửa hay cùng tạo một file, khi một plan nhắc file mà plan khác tạo, và khi hai ticket phụ thuộc nhau. Với cặp task không có các dấu hiệu đó, viết test và code của task sau trên nhánh chính chưa có task trước cho ra cùng kết quả như viết sau khi task trước đã merge, trừ khi hai task đụng nhau theo nghĩa (ví dụ task trước đổi chữ ký một hàm mà task sau gọi, ở file khác). Trường hợp đó vẫn lộ ra, vì task sau gộp nhánh chính và chạy lại `scripts/check.sh` trước khi rà soát, và `pr-check` là cổng chặn cuối như mọi task.

## Quyết định

**1. Khi nào được gối.** Task N vừa mở PR và đang chờ `pr-check` (bước 8). Phiên chính được bắt đầu task N+1 (task kế trong danh sách) khi đủ mọi điều:

- N+1 không phụ thuộc N (`depends_on`), trực tiếp hay qua chuỗi.
- `scripts/run-check.py` không báo cho cặp N, N+1 dòng "cùng sửa một file", "cùng ghi Tạo một file", hay "nhắc một file mà plan khác tạo".
- Không plan nào trong hai plan sửa file build dùng chung (`gradle/libs.versions.toml`, `build-logic/`, `settings.gradle.kts`, file `build.gradle.kts`), `scripts/check.sh` hay `.github/`.
- N không đang trong vòng sửa vì CI đỏ.

Thiếu một điều thì làm tuần tự như cũ. Mỗi lúc chỉ gối một task: N+2 chờ tới khi N đã merge.

**2. Cách làm.** Phiên chính chạy `scripts/plan-check.py` cho N+1, tạo nhánh `claude/T-NNN-...` của N+1 từ `origin/main` hiện tại (chưa có N), rồi giao bước 3 tới 5 cho phiên con trong worktree riêng, như mục Giao bước 3 đến 5 cho phiên con. Đẩy nhánh N+1 ngay như mọi task (nhánh không có PR thì `pr-check` không chạy). Phiên chính tiếp tục chờ CI và merge N như thường.

**3. Gộp nhánh chính trước khi rà soát.** Sau khi N đã merge và `main-guard` của N xanh, trước bước 6 của N+1: gộp nhánh chính vào nhánh N+1 (`git merge origin/main`, không rebase, để không phải đẩy đè), rồi chạy lại `scripts/check.sh`. Xung đột khi gộp, hay `check.sh` đỏ vì phần của N, thì sửa trong phạm vi file của plan N+1; không sửa được trong phạm vi đó thì dừng N+1 theo `risk-scoring.md` mục 4. Rà soát độc lập nhận diff sau khi gộp.

**4. Khi N không merge.** N dừng hay bị revert thì N+1 vẫn đi tiếp, vì hai task độc lập; N+1 gộp nhánh chính như ý 3 trước khi rà soát. `main-guard` của N đỏ thì N+1 không đẩy thêm commit và không mở PR tới khi nhánh chính xanh trở lại, như quy tắc chờ `main-guard` hiện có.

**5. Mỗi lúc một PR chờ merge.** PR của N+1 chỉ mở sau khi N đã merge (hay đã dừng), để `merge-if-green.sh` không phải xử lý hai PR của phiên cùng lúc và mỗi lần merge vẫn dựa trên nhánh chính đã có task trước.

**6. Ghi lại.** Dòng thời gian của báo cáo ghi giờ bắt đầu gối và giờ gộp nhánh chính. Nhật ký plan của N+1 ghi "gối với T-NNN" và kết quả của lần gộp và `check.sh` sau gộp.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Gối một task khi độc lập và không chung file, gộp nhánh chính trước rà soát, một PR chờ merge (đã chọn) | Tiết kiệm 9 tới 15 phút mỗi task được gối; rủi ro giới hạn trong cặp task mà `run-check.py` đã xét là không đụng nhau; cổng `pr-check` và `merge-if-green.sh` giữ nguyên | Thêm một lần `check.sh` sau khi gộp (vài phút, nhưng chạy khi phiên đang chờ `main-guard`); quy trình phức tạp hơn; đụng nhau theo nghĩa giữa file khác nhau chỉ lộ ra ở lần `check.sh` sau gộp |
| Giữ tuần tự | Đơn giản nhất | Mỗi task tiếp tục mất khoảng chờ `pr-check` |
| Chạy song song nhiều task, nhiều PR chờ merge | Nhanh nhất | Mỗi lần merge làm PR khác phải cập nhật và chạy lại CI; `merge-if-green.sh` và quy tắc chờ `main-guard` phải đổi; khó lần lỗi khi nhánh chính đỏ |
| Bỏ chờ `pr-check`, merge khi `check.sh` cục bộ qua | Bỏ hẳn khoảng chờ | Mất cổng chặn chạy trên máy sạch; trái quy tắc chỉ merge qua `merge-if-green.sh` khi CI xanh |

## Hệ quả

- **Tích cực:** danh sách nhiều task độc lập ngắn đi khoảng 10 tới 15 phút mỗi task; phần chờ của phiên chính được dùng để làm việc.
- **Phải chấp nhận:** nguyên tắc 1 của `docs/auto-run.md` đổi thành "mỗi lúc một PR chờ merge"; bước 1, 3 (ngoại lệ đẩy commit đầu khi chờ `main-guard`), 7, mục Giao bước 3 đến 5 cho phiên con và mục Trong phiên tự chạy của `docs/CLAUDE.md` phải sửa theo; nên có một lệnh của `scripts/run-check.py` trả lời "cặp N, N+1 có gối được không" để phiên không tự đọc các dòng chú ý; các việc này làm trong PR thực hiện ADR sau khi nó được chấp nhận.
- **Việc AI phải tuân thủ:** chỉ bắt đầu task sau khi PR của task trước đang chờ `pr-check` và hai task không phụ thuộc nhau, `run-check.py` không báo cùng sửa, cùng tạo hay nhắc file của nhau, không plan nào sửa file build dùng chung, `scripts/check.sh` hay `.github/`, và task trước không đang sửa vì CI đỏ; mỗi lúc chỉ gối một task; tạo nhánh task sau từ `origin/main` và giao bước 3 tới 5 cho phiên con trong worktree riêng; sau khi task trước merge và `main-guard` xanh thì gộp nhánh chính vào nhánh task sau bằng `git merge`, chạy lại `scripts/check.sh` rồi mới rà soát độc lập; xung đột hay lỗi ngoài phạm vi file của plan thì dừng task sau theo `risk-scoring.md` mục 4; chỉ mở PR của task sau khi task trước đã merge hay đã dừng; ghi giờ gối và kết quả gộp vào dòng thời gian và nhật ký plan.
