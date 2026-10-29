# LÀM NHANH CÓ NGƯỜI THEO DÕI

Hướng dẫn cho dev muốn làm ngay một việc nhỏ của code app trong ngày, cùng AI, không chờ phiên tự chạy. Quy tắc gốc ở ADR-0034; phần AI phải làm ở [docs/CLAUDE.md](../CLAUDE.md), mục Khi làm nhanh có người theo dõi.

## 1. Khi nào dùng

Dùng đường làm nhanh khi đủ mọi điều sau:

- Anh ngồi cùng AI tới khi xong, gồm cả lúc chờ CI và `main-guard` (khoảng 5 đến 12 phút sau khi merge).
- Việc nhỏ: cỡ S, hoặc M nếu anh thấy ổn; rủi ro Thấp theo [risk-scoring.md](../risk-scoring.md).
- Không chạm quảng cáo, sự kiện phân tích, dữ liệu gửi ra ngoài, hộp thoại đồng ý, quyền, thư viện hay SDK mới, module `:core:*`, cấu hình build, CI, đường dẫn R2, khóa DataStore.
- Việc đã có trong spec đã chốt, hoặc anh là người xác nhận spec của feature đó.
- Không có phiên tự chạy đang chạy.

Ví dụ hợp: sửa chữ hiển thị sai, chỉnh khoảng cách một màn hình, thêm một trạng thái lỗi kèm nút thử lại, sửa một lỗi giao diện đã rõ nguyên nhân.

Không hợp, đi đường thường: thêm vị trí quảng cáo, thêm màn hình mới có thiết kế chưa duyệt, đổi thành phần dùng chung của `:core:designsystem`, việc cần chạy thử kỹ thuật mới.

Việc về script, tài liệu hay quy trình không cần đường này: vẫn làm bằng PR thường trong phiên có người theo dõi.

## 2. Bắt đầu

Mở Claude Code trong repo của app và nói việc cần làm, ví dụ:

> Làm nhanh: màn hình kết quả của feature `photo-filter` hiện sai chữ "Lưu ảnh" thành "Luu anh".

AI kiểm điều kiện ở mục 1. Thiếu điều nào thì AI nói rõ và đề xuất đi đường thường.

## 3. Diễn biến

| Bước | AI làm | Anh làm |
| --- | --- | --- |
| 1. Tài liệu | Ghi lời anh thành file yêu cầu, sửa spec nếu cần, viết ticket và plan đủ mục, chạy `plan-check.py` | Trả lời câu hỏi nếu có; xác nhận phần spec đổi, nếu có |
| 2. Duyệt plan | Đưa tóm tắt: phạm vi, file sẽ sửa, test, điểm rủi ro | Đồng ý, hoặc yêu cầu sửa. Lời đồng ý được ghi vào plan |
| 3. Làm | Viết test trước, viết code, chạy `scripts/check.sh`, nhờ một agent khác rà soát độc lập, sửa theo rà soát | Trả lời khi AI hỏi về chỗ lệch so với plan |
| 4. PR và CI | Ghi nhật ký vào plan, mở một PR chứa mọi thứ, kiểm các danh sách chạy chưa chạy có bị ảnh hưởng không, chờ CI | Xem PR nếu muốn; quyết khi AI báo ảnh hưởng tới danh sách chạy |
| 5. Merge | Merge bằng `scripts/merge-if-green.sh`, chờ `main-guard` | Không kích hoạt phiên tự chạy khi PR còn mở |
| 6. Kiểm tra | Chạy tay `device-session.yml` hay `staging.yml` khi cần | Thử trên máy nếu plan có dòng kiểm tra thủ công; báo kết quả để AI ghi vào nhật ký |

Cả task là một PR, một vòng CI. Không có họp, danh sách chạy hay báo cáo phiên.

## 4. Khi phải dừng

AI dừng đường làm nhanh khi gặp điều kiện dừng của [risk-scoring.md](../risk-scoring.md) mục 4, khi thấy việc chạm một điểm ở mục 1 ở trên, hoặc khi anh muốn bỏ:

- PR làm nhanh được đóng, nhánh giữ lại.
- Yêu cầu, ticket (`stopped`) và plan có nhật ký lần dừng vẫn lên nhánh chính bằng một PR tài liệu, để không mất lịch sử.
- Muốn làm tiếp thì plan có phiên bản mới, đi đường thường: họp đánh giá rủi ro, danh sách chạy, phiên tự chạy.

Nhánh chính đỏ sau khi merge (issue `main-broken`) vì PR làm nhanh: AI cùng anh revert theo ADR-0025, rồi ticket chuyển `stopped`.

## 5. So với phiên tự chạy

| | Phiên tự chạy | Làm nhanh |
| --- | --- | --- |
| Người | Không cần ngồi cùng | Dev ngồi cùng suốt task |
| Việc | Mọi mức rủi ro, nhiều task | Một task nhỏ, rủi ro Thấp, không điểm chặn |
| Duyệt plan | Họp, PR plan riêng, duyệt chéo khi rủi ro cao | Dev đồng ý ngay trong phiên |
| Số PR | Ticket, plan, danh sách chạy, mỗi task một PR, báo cáo | Một PR |
| Giữ nguyên | Yêu cầu, ticket, plan, test viết trước, rà soát độc lập, CI, `merge-if-green.sh`, nhật ký | Như cột bên |
| Xem lại sau | Báo cáo phiên | Nhật ký plan và PR; plan ghi `lane: quick` |
