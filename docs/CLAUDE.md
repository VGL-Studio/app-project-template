# HƯỚNG DẪN AI LÀM VIỆC VỚI THƯ MỤC DOCS

Tài liệu này dành cho AI. Nó nói rõ AI đọc gì, viết gì và không được làm gì với các tài liệu trong `docs/`. Tổng quan quy trình nằm ở [README.md](README.md), quy trình phiên tự chạy ở [auto-run.md](auto-run.md), thang điểm rủi ro ở [risk-scoring.md](risk-scoring.md).

## Đọc trước khi làm

Trước khi viết spec, plan hay code cho một ticket, đọc theo thứ tự:

1. Ticket trong `features/<feature>/tickets/`.
2. `features/<feature>/spec.md`, kể cả mục câu hỏi đã được trả lời.
3. Thiết kế trong `features/<feature>/design/`, nếu có.
4. Tất cả ADR có trạng thái `accepted` trong `adr/`.
5. Phản hồi trong `features/<feature>/feedback/` có trạng thái `triaged` hoặc `planned`, và phản hồi trong `inbox/` được liên kết tới ticket.
6. Các plan trước đó của cùng feature, nhất là mục nhật ký thực hiện, để biết những gì đã làm và đã gặp trở ngại.

Nếu tài liệu mâu thuẫn nhau, không tự chọn bên nào. Ghi mâu thuẫn vào mục câu hỏi của tài liệu đang viết và báo cho người.

## Khi sinh spec từ yêu cầu

- Feature mới thì tạo thư mục `features/<feature>/` với tên trùng tên module `:feature:<ten>` dự kiến. Đặt tên chưa chắc thì đề xuất tên trong mục câu hỏi.
- Yêu cầu nằm trong `inbox/` được chuyển vào `features/<feature>/requests/` khi đã xác định feature.
- Dùng đúng [mẫu spec](templates/spec.md), giữ nguyên các mục, mục nào không áp dụng thì ghi "Không có" thay vì xóa.
- Viết bằng ngôn ngữ nghiệp vụ. Mục tiêu, phạm vi và kịch bản sử dụng không chứa tên lớp, tên thư viện hay thuật ngữ kỹ thuật.
- Mỗi kịch bản có tiêu chí chấp nhận cụ thể, kiểm chứng được, viết theo dạng "Khi ... thì ...".
- Không tự thêm vị trí quảng cáo, sự kiện phân tích hay dữ liệu thu thập. Chỉ ghi những gì yêu cầu nêu; nếu thấy cần, đặt thành câu hỏi.
- Chỗ nào yêu cầu chưa rõ, ghi thành câu hỏi trong mục câu hỏi còn mở, kèm phương án AI đề xuất. Không đoán.
- Spec mới luôn có trạng thái `draft`. Spec đã `confirmed` mà có yêu cầu mới thì cập nhật nội dung, chuyển sang `changed` và ghi vào mục lịch sử. Chỉ người mới chuyển sang `confirmed`.

## Khi tách spec thành ticket

- Mỗi ticket là một phần giao được, kiểm chứng được, chạy xong trong một lượt của phiên tự chạy. Ticket quá lớn thì tách tiếp.
- Mã ticket lấy số tiếp theo của cả app: tìm mã lớn nhất trong mọi thư mục `features/*/tickets/` rồi cộng một.
- Ghi rõ ticket xử lý kịch bản nào của spec và phụ thuộc ticket nào.
- Chấm rủi ro sơ bộ và ước lượng cỡ (S, M, L) theo [mẫu ticket](templates/ticket.md).

## Khi viết plan

- Dùng đúng [mẫu plan](templates/plan.md).
- Danh sách file sẽ tạo và sửa phải đầy đủ và cụ thể. Danh sách này là ranh giới: trong phiên tự chạy, AI chỉ được chạm vào những file đã liệt kê.
- Mỗi tiêu chí chấp nhận trong spec phải có cách kiểm chứng tương ứng trong mục kiểm thử. Test được viết trước code, nên mục kiểm thử phải đủ cụ thể để viết được test ngay từ plan.
- Chấm rủi ro theo [risk-scoring.md](risk-scoring.md), ghi lý do cho từng tiêu chí. Không hạ điểm để plan dễ được duyệt.
- Nêu thẳng điều chưa chắc chắn trong mục câu hỏi cho người duyệt.
- Plan mới có trạng thái `draft`. Không bao giờ tự chuyển sang `approved`.

## Khi lập danh sách chạy

- Sau họp đánh giá rủi ro, lập `runs/RUN-YYYYMMDD-NN.md` theo [mẫu phiên tự chạy](templates/run.md), sắp các plan đã duyệt theo thứ tự đã thống nhất.
- Danh sách chạy có trạng thái `draft`. Chỉ dev mới chuyển sang `approved` và kích hoạt phiên.

## Trong phiên tự chạy

- Làm đúng các bước trong [auto-run.md](auto-run.md), lần lượt từng task, không chạy song song.
- Chỉ thực hiện plan có trạng thái `approved` có trong danh sách chạy đã duyệt.
- Tuân thủ điều kiện dừng ở mục 4 của [risk-scoring.md](risk-scoring.md).
- Sau mỗi task, ghi nhật ký vào cuối plan và cập nhật trạng thái ticket: `on-staging` khi đã merge, `stopped` khi dừng.
- Cuối phiên, viết báo cáo vào file phiên chạy.

## Khi xử lý phản hồi

- Phản hồi mới trong `inbox/` mà xác định được feature thì đề xuất chuyển vào `features/<feature>/feedback/`.
- Đề xuất phân loại: lỗi so với spec, thay đổi yêu cầu, hay góp ý mới. Chủ feature quyết định.
- Phản hồi là thay đổi yêu cầu thì không sửa thẳng vào code. Đề xuất cập nhật spec, chờ người phụ trách sản phẩm xác nhận.
- Phản hồi là lỗi so với spec thì cập nhật plan (tăng phiên bản plan, thêm mục sửa lỗi). Plan sửa đi qua họp đánh giá rủi ro như plan mới.

## Những điều không được làm

- Không sửa nội dung đã xác nhận: spec `confirmed`, plan `approved`, ADR `accepted`. Muốn thay đổi thì thêm mục mới hoặc tạo bản thay thế.
- Không xóa tài liệu. Tài liệu hết hiệu lực được chuyển trạng thái, không bị xóa.
- Không đưa vào tài liệu bất kỳ khóa bí mật, mã quảng cáo thật, thông tin đăng nhập hay dữ liệu cá nhân của người dùng.
- Không tự cấp quyền duyệt, không tự chuyển trạng thái thay cho người.

## Cách viết

- Tiếng Việt tự nhiên, câu ngắn, dễ hiểu. Không dùng từ hiếm, không dịch từng chữ từ tiếng Anh.
- Thuật ngữ kỹ thuật thông dụng giữ tiếng Anh: module, test, build, staging, commit, merge.
- Không dùng dấu gạch ngang dài, không dùng biểu tượng cảm xúc.
- Ngày viết theo dạng `YYYY-MM-DD`.
