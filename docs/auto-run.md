# QUY TRÌNH PHIÊN AI TỰ CHẠY

Phiên tự chạy là khoảng thời gian AI tự thực hiện các plan đã duyệt mà không cần người theo dõi. Phiên không gắn với giờ cố định: phiên bắt đầu khi dev đã duyệt xong và kích hoạt, có thể ngay từ buổi chiều, và kết thúc khi hết việc hoặc gặp một điều kiện kết thúc. Thường phiên chạy qua đêm, để tận dụng thời gian ngoài giờ làm việc và hạn mức sử dụng được làm mới.

Bốn nguyên tắc của phiên tự chạy:

1. **Tuần tự.** Mỗi lúc chỉ làm một task. Task sau bắt đầu từ nhánh chính đã có kết quả của task trước.
2. **Test viết trước.** Test được viết từ plan và phải thất bại đúng lý do trước khi có code.
3. **Rà soát độc lập.** Một phiên AI khác, không tham gia viết code, rà soát thay đổi trước khi đẩy lên CI.
4. **Không cố định giờ.** Người quyết định lúc bắt đầu; thời điểm muốn có bản staging (nếu cần) ghi trong danh sách chạy.

## 1. Kích hoạt phiên

1. Sau họp đánh giá rủi ro, AI lập danh sách chạy `runs/RUN-YYYYMMDD-NN.md` theo [mẫu phiên tự chạy](templates/run.md): các plan đã duyệt, thứ tự chạy, số task tối đa, thời điểm muốn có bản staging (nếu có).
2. Dev xem lại danh sách, chuyển trạng thái sang `approved`.
3. Dev kích hoạt phiên bằng workflow `start-run.yml` trên GitHub, hoặc bằng Run now của routine. Muốn bắt đầu vào một thời điểm cụ thể (ví dụ lúc hạn mức sử dụng được làm mới) thì hẹn một lần chạy cho routine.
4. Mỗi lúc chỉ có một phiên tự chạy. Trong khi phiên đang chạy, người không merge vào nhánh chính.
5. Phiên chạy dưới tài khoản Claude và tài khoản GitHub hiện có của người phụ trách (ADR-0009). Vì dùng chung hạn mức, nên tránh kích hoạt phiên khi tài khoản đó vừa dùng nhiều trong ngày.

## 2. Kiểm tra đầu phiên

AI chuyển trạng thái phiên sang `running` và kiểm tra:

| Điều kiện | Không thỏa thì |
| --- | --- |
| Nhánh chính đang xanh: lần chạy gần nhất của `main-guard.yml` qua, và không có issue hay PR nào đang mở mang nhãn `main-broken` | Không chạy task nào, kết thúc phiên, báo cáo lý do |
| Plan có trạng thái `approved` và nằm trong danh sách chạy | Bỏ qua task, ghi lý do |
| Ticket có trạng thái `approved` | Bỏ qua task, ghi lý do |
| Các ticket phụ thuộc đã `on-staging`, hoặc được merge trước đó trong cùng phiên | Bỏ qua task, ghi lý do |

## 3. Các bước cho từng task

Mỗi task đi qua 10 bước theo thứ tự. Điều kiện dừng ở mục 4 của [risk-scoring.md](risk-scoring.md) áp dụng ở mọi bước.

| Bước | Việc làm | Kết quả |
| --- | --- | --- |
| 1. Tạo nhánh | Tạo nhánh `claude/T-NNN-<tu-khoa>` từ nhánh chính mới nhất | Nhánh riêng của task |
| 2. Đọc tài liệu | Đọc theo thứ tự trong [CLAUDE.md](CLAUDE.md) | Đủ ngữ cảnh |
| 3. Viết test trước | Viết test theo mục Kiểm thử của plan, chạy và xác nhận test thất bại đúng lý do | Commit chứa test |
| 4. Viết code | Viết code cho đến khi test qua, chỉ trong danh sách file của plan | Code chạy được |
| 5. Kiểm tra cục bộ | Build, lint, unit test, screenshot test, quy tắc phụ thuộc module, đối chiếu danh sách file | Sẵn sàng rà soát |
| 6. Rà soát độc lập | Phiên AI khác rà soát thay đổi | Danh sách điểm phát hiện đã xử lý |
| 7. Commit và đẩy | Commit theo quy ước, đẩy nhánh, mở PR | PR liên kết tới plan |
| 8. CI đầy đủ | `pr-check.yml` chạy cổng chất lượng, thêm test trên emulator khi PR chạm vào code chạy trong app | CI xanh |
| 9. Merge | Merge bằng `scripts/merge-if-green.sh` | Nhánh chính có thay đổi của task |
| 10. Ghi nhật ký | Ghi vào cuối plan, cập nhật trạng thái | Người đọc được kết quả |

### Bước 3. Viết test trước

- Viết toàn bộ test được nêu trong mục Kiểm thử của plan, trước khi viết bất kỳ dòng code nào của chức năng.
- Chạy test và xác nhận test **thất bại đúng lý do**: vì chức năng chưa có, không phải vì test viết sai cú pháp hay lỗi môi trường. Với màn hình hay lớp chưa tồn tại, AI tạo khung rỗng (hàm trả về giá trị mặc định) để test biên dịch được và thất bại ở phần kiểm tra.
- Test nào qua ngay khi chưa có code là test không kiểm tra gì, phải viết lại.
- Commit test riêng, trước commit code, để lịch sử cho thấy test có trước.
- Chỉ được bỏ qua bước này với phần thay đổi nằm trong mục ngoại lệ đã được người duyệt đồng ý trong plan.
- Screenshot test cho màn hình mới: mỗi lệnh `captureRoboImage` ghi rõ đường dẫn ảnh, dạng `"src/test/screenshots/<Ten>.png"`. Chạy cục bộ, `scripts/check.sh` tự ghi ảnh chuẩn cho những ảnh còn thiếu và giữ nguyên ảnh đã có. Commit ảnh chuẩn cùng PR (CI báo lỗi cho từng ảnh thiếu) và đánh dấu trong báo cáo để người xem. Ảnh chuẩn của màn hình cũ chỉ được cập nhật khi plan nêu rõ.

### Bước 4. Viết code

- Chỉ tạo và sửa các file trong danh sách file của plan.
- Không sửa test đã viết ở bước 3 để làm cho test qua. Nếu test sai ở chi tiết không làm thay đổi điều nó kiểm tra (ví dụ sai tên một hàm), được sửa và ghi lý do vào nhật ký. Nếu cần thay đổi điều test kiểm tra thì dừng task.

### Bước 5. Kiểm tra cục bộ

- Chạy `scripts/check.sh`. Script gồm quy tắc phụ thuộc module, ktlint, unit test, test giao diện trên JVM, screenshot test, Android Lint và build bản staging. CI dùng đúng script này, nên qua ở bước 5 thì thường cũng qua ở bước 8.
- So sánh danh sách file thay đổi thực tế với danh sách file trong plan. Có file ngoài danh sách thì dừng task.

### Bước 6. Rà soát độc lập

Một phiên AI riêng được khởi tạo để rà soát. Phiên này chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi; không nhận quá trình làm việc của phiên viết code, để đánh giá với con mắt mới.

Nội dung rà soát:

- Thay đổi có đáp ứng đủ tiêu chí chấp nhận trong spec không.
- Test có thực sự kiểm tra tiêu chí chấp nhận không, hay chỉ kiểm tra những gì code đang làm.
- Có tuân thủ mục "Việc AI phải tuân thủ" của các ADR không.
- Có chạm vào phần ngoài phạm vi plan không.
- Có cấu hình production, khóa bí mật, mã quảng cáo thật trong thay đổi không.
- Code có theo đúng mẫu đã có trong dự án không.

Kết quả rà soát chia ba mức:

| Mức | Xử lý |
| --- | --- |
| Chặn | Quay lại bước 4. Tính vào giới hạn 3 lần sửa cho cùng một lỗi. |
| Nên sửa | Sửa nếu nằm trong phạm vi plan, nếu không thì ghi vào báo cáo. |
| Ghi chú | Ghi vào báo cáo để người xem. |

Nếu phiên rà soát và phiên viết code hiểu spec khác nhau, AI không tự chọn bên nào mà dừng task, vì đây là dấu hiệu spec hoặc plan chưa rõ.

### Bước 7. Commit và đẩy

- Nội dung commit theo dạng `<loại>(T-NNN): <mô tả ngắn>`, với loại là `test`, `feat`, `fix`, `refactor` hoặc `docs`. Mỗi commit có dòng `Co-Authored-By: Claude`.
- PR có tiêu đề `T-NNN: <tên task>`, phần mô tả gồm đường dẫn tới plan, tóm tắt thay đổi, kết quả rà soát độc lập và đường dẫn tới phiên chạy. Vì PR mang tên tài khoản của người, các dấu hiệu này giúp phân biệt việc của AI.
- Trong môi trường cloud của routine, `gh pr` và `gh issue` không chạy được (proxy GitHub chặn GraphQL, trả lỗi 403). Mọi thao tác với GitHub trong phiên đều dùng REST qua `gh api`, kể cả kiểm tra issue `main-broken` ở mục 2, bước 9 và khởi động `staging.yml` ở mục 4. Ví dụ:
  - Mở PR: `gh api -X POST repos/{owner}/{repo}/pulls -f title=... -f head=<nhánh> -f base=main -f body=...`
  - Tìm issue `main-broken` đang mở: `gh api "repos/{owner}/{repo}/issues?state=open&labels=main-broken"`
  - Khởi động `staging.yml`: `gh api -X POST repos/{owner}/{repo}/actions/workflows/staging.yml/dispatches -f ref=main`

### Bước 8. CI đầy đủ

- `pr-check.yml` chạy toàn bộ kiểm tra ở bước 5. Test trên emulator chỉ chạy khi PR chạm vào code giao diện hoặc test giao diện (ADR-0010).
- AI theo dõi kết quả CI qua GitHub cho đến khi mọi kiểm tra kết thúc.
- CI đỏ thì AI đọc log để phân biệt lỗi thật với lỗi chập chờn. Lỗi chập chờn được chạy lại một lần. Lỗi thật thì quay lại bước 4, tối đa 3 lần cho cùng một lỗi, sau đó dừng task.

### Bước 9. Merge

- Chỉ merge bằng `scripts/merge-if-green.sh <số PR>`. Script tự kiểm tra mọi điều kiện của cổng chất lượng và từ chối merge nếu thiếu điều kiện nào (ADR-0010). AI không merge bằng bất kỳ cách nào khác.
- Script báo nhánh chưa cập nhật theo nhánh chính thì cập nhật nhánh, đợi CI chạy lại rồi gọi lại script.
- Script từ chối vì lý do khác thì không tìm cách vượt qua, mà dừng task và ghi lý do vào nhật ký.
- Trước khi bắt đầu task tiếp theo, kiểm tra lại không có issue hay PR nào đang mở mang nhãn `main-broken`. Có thì kết thúc phiên.

### Bước 10. Ghi nhật ký

- Ghi vào mục Nhật ký thực hiện ở cuối plan theo mẫu.
- Plan chuyển sang `done`, ticket chuyển sang `on-staging`.
- Task bị dừng: plan và ticket chuyển sang `stopped`, nhánh được giữ nguyên, không merge.

Sau đó AI chuyển sang task tiếp theo trong danh sách. Task phụ thuộc vào task bị dừng được bỏ qua.

## 4. Kết thúc phiên

Phiên kết thúc khi gặp một trong các điều kiện sau:

| Điều kiện | Xử lý task đang làm |
| --- | --- |
| Hết task trong danh sách, hoặc đạt số task tối đa | Không có |
| Gần đến thời điểm muốn có bản staging, không đủ thời gian cho task tiếp theo | Không bắt đầu task mới |
| Hết hạn mức sử dụng | Commit phần đang làm vào nhánh của task, không merge, ghi nhật ký |
| Nhánh chính đỏ sau một lần merge (xuất hiện issue mang nhãn `main-broken`) | Dừng ngay, không làm thêm task nào |

Sau đó AI:

1. Nếu trong phiên có ít nhất một task được merge, khởi động workflow `staging.yml` để build bản staging từ nhánh chính và phát cho nhóm qua Firebase App Distribution, rồi chờ workflow kết thúc.
2. Viết báo cáo phiên vào file danh sách chạy.
3. Chuyển trạng thái phiên sang `finished`.

## 5. Báo cáo phiên

Báo cáo phiên là thứ dev đọc đầu tiên khi bắt đầu làm việc lại. Báo cáo gồm:

- Kết quả từng task: đã merge, bị dừng, bị bỏ qua, kèm lý do.
- Những gì người cần tự kiểm tra: ảnh chuẩn screenshot mới, tiêu chí kiểm tra thủ công, điểm rà soát mức ghi chú.
- Task bị dừng: dừng ở bước nào, vì sao, đề xuất hướng xử lý.
- Thông tin bản staging: phiên bản, kênh phát.
- Thời gian chạy và mức sử dụng của phiên.

## 6. Vùng an toàn của phiên

- Phiên chỉ có cấu hình staging. Không có mã quảng cáo thật, cấu hình Firebase production, khóa R2 production hay khóa ký ứng dụng.
- Phiên không đọc, in ra hay chuyển đi bất kỳ bí mật nào của repo. Bí mật trên GitHub chỉ gồm cấu hình staging và mã kích hoạt routine.
- Phiên không chạm vào `scripts/release.sh`. Phát hành production do người làm từ máy của mình (ADR-0009).
- Phiên không đẩy thẳng lên nhánh chính. Mọi thay đổi đi qua nhánh của task, PR, CI và script merge.
- Phiên không sửa `.github/`, `scripts/merge-if-green.sh` hay quyền truy cập, trừ khi đó chính là nội dung của một plan rủi ro cao đã được duyệt chéo.
- Vì repo dùng gói GitHub Free, GitHub không chặn merge ở mức hệ thống. Lớp bảo vệ là script merge và workflow `main-guard.yml` (ADR-0010).
- Mọi việc phiên làm đều để lại dấu vết trong nhánh, PR, nhật ký plan và báo cáo phiên.

## 7. Những điểm sẽ hiệu chỉnh

Các con số dưới đây được đặt tạm và sẽ hiệu chỉnh qua retro dựa trên số liệu thực tế của các phiên:

- Số task tối đa mỗi phiên.
- Giới hạn 3 lần sửa cho cùng một lỗi.
- Thời gian dự trù để build và phát bản staging cuối phiên.
