---
id: ADR-0034
title: Việc nhỏ, rủi ro thấp được làm ngay ban ngày theo đường làm nhanh, AI làm cùng dev, vẫn đủ yêu cầu, ticket, plan, nhật ký, CI và merge-if-green.sh
status: proposed             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # không thay ADR nào; thêm một đường đi bên cạnh phiên tự chạy (docs/auto-run.md)
superseded_by:
---

# ADR-0034: VIỆC NHỎ, RỦI RO THẤP ĐƯỢC LÀM NGAY BAN NGÀY THEO ĐƯỜNG LÀM NHANH, AI LÀM CÙNG DEV, VẪN ĐỦ YÊU CẦU, TICKET, PLAN, NHẬT KÝ, CI VÀ MERGE-IF-GREEN.SH

## Bối cảnh

Code của app hiện chỉ có một đường lên nhánh chính: phiên tự chạy, thường chạy qua đêm. Ban ngày dev hay muốn sửa ngay một chỗ nhỏ hay thêm một tính năng nhỏ. Đi đủ đường của phiên tự chạy thì chậm:

- T-048 (dọn rewarded, cỡ S) cần 5 PR: ticket (#231), plan (#239), danh sách chạy (#241), code (#242), báo cáo phiên (#245).
- Mỗi phiên tốn khoảng 30 phút cố định (kiểm đầu phiên, emulator, staging, báo cáo), chưa kể thời gian soạn, rà soát và duyệt plan, danh sách chạy.
- Các bước đó có vì phiên chạy không có người (ADR-0031): mọi quyết định phải nằm sẵn trong tài liệu đã duyệt. Khi dev ngồi cùng AI, dev trả lời và quyết ngay được, nên phần lớn các bước duyệt và rà soát trước khi chạy không còn cần.

Ngoài code app, việc làm nhanh ban ngày đã có trên thực tế (ví dụ #217, #246, #247: script, tài liệu, có test, merge bằng `merge-if-green.sh`), nhưng không tài liệu nào nói khi nào được làm vậy.

Không thể bỏ tài liệu: không có yêu cầu, ticket, plan và nhật ký thì không theo dõi được lịch sử phát triển app, `scripts/status.py`, retro và bài học mất dấu, và sau này không ai biết vì sao code thành như vậy. Không thể bỏ CI và `merge-if-green.sh`: test trên emulator (`ui-test`) và bản release có R8 chỉ chạy trong CI, và nhánh chính đỏ thì phiên đêm sau hỏng cả đêm.

## Quyết định

**1. Có đường làm nhanh bên cạnh phiên tự chạy.** Dev và AI làm cùng nhau, ngay trên máy dev, trong phiên có người theo dõi. AI làm gần như mọi việc; dev trả lời câu hỏi, đồng ý plan một lần và xem kết quả.

**2. Điều kiện vào đường làm nhanh.** Đủ mọi điều sau; thiếu một điều thì đi đường của phiên tự chạy.

- Dev ngồi cùng suốt task và trả lời được ngay.
- Plan không chạm điểm chặn nào ở mục 1 của `risk-scoring.md`, kể cả điểm 7 (module `:core:*`, cấu hình build, CI, quy trình phiên tự chạy). Không thêm quyền, thư viện, SDK.
- Mức rủi ro Thấp (tổng 0 đến 4), tiêu chí E dưới 2: không có kỹ thuật mới cần chạy thử (mục 4.1 của plan).
- Ticket cỡ S, hoặc M khi dev đồng ý.
- Kịch bản đã có trong spec `confirmed`. Việc chưa có trong spec chỉ đi đường làm nhanh khi dev ngồi cùng là người xác nhận spec của feature đó: AI cập nhật spec (`confirmed` sang `changed` rồi `confirmed`, ghi mục lịch sử), dev xác nhận trong phiên. Không thì đi đường thường.
- Không có phiên tự chạy nào đang chạy (danh sách `running`), và không có danh sách chạy `approved` sắp chạy mà plan của nó sửa cùng file với plan làm nhanh. Có danh sách như vậy thì AI báo dev; dev chọn chờ hay chuyển task sang đường thường.

**3. Vẫn làm như đường chính thống.**

- **Yêu cầu:** lời dev nói trong phiên được ghi thành file yêu cầu trong `features/<feature>/requests/`, giữ nguyên ý của dev.
- **Ticket** theo mẫu ticket, mã lấy số kế tiếp của cả app.
- **Plan** theo đúng mẫu plan, đủ các mục, kể cả mục 3.5 "Hành vi đã có bị đổi" (`scripts/existing-tests.py`) và chấm rủi ro. Frontmatter có `lane: quick`. Chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi.
- **Test viết trước**, thất bại đúng lý do, commit trước code (`docs/auto-run.md`, bước 3).
- **Kiểm tra cục bộ** bằng `scripts/check.sh` (bước 5).
- **Rà soát độc lập** bằng một agent con ngữ cảnh mới, chỉ nhận plan, spec, thiết kế, ADR và phần thay đổi, theo nội dung và ba mức của bước 6.
- **CI** (`pr-check.yml`) và **merge** chỉ bằng `scripts/merge-if-green.sh`, rồi chờ `main-guard` như bước 8, 9.
- **Nhật ký thực hiện** ở cuối plan, trạng thái plan `done` và ticket `on-staging`.
- **Bài học** vào `docs/lessons.md` khi gặp điều đáng ghi, như mọi phiên.

**4. Được bỏ.**

- PR riêng cho ticket, plan, danh sách chạy; họp đánh giá rủi ro; `scripts/run-check.py` và rà soát trước khi chạy (`docs/ops/pre-run-review.md`).
- Kích hoạt phiên (`start-run.yml`, routine, `claude --cloud`), kiểm tra đầu phiên, nhánh báo cáo, báo cáo phiên, hàng chờ plan cuối phiên.
- Lượt duyệt plan riêng: dev đọc tóm tắt plan (phạm vi, danh sách file, test, điểm rủi ro, câu hỏi) và đồng ý trong phiên. Đó là lần duyệt. AI chuyển plan sang `approved`, ghi `reviewer` là dev, `approved_at`, và mục 9 ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý của dev. Đây là ngoại lệ duy nhất của quy tắc "AI không tự chuyển sang `approved`" và "AI không điền mục 9": chỉ khi dev đã đồng ý rõ ràng trong chính phiên đó.

**5. Một PR cho cả task.** Nhánh `claude/quick-T-NNN-<tu-khoa>`, tạo từ nhánh chính mới nhất. Tên nhánh không bắt đầu bằng `claude/T-` để `merge-if-green.sh` từ chối PR khi có phiên tự chạy đang chạy, như mọi PR không thuộc phiên. PR chứa yêu cầu, spec nếu đổi, ticket, plan, test, code, ảnh chuẩn và nhật ký. Tiêu đề `T-NNN: <tên task>`, mô tả ghi "Đường làm nhanh (ADR-0034)", người duyệt plan, kết quả `plan-check.py`, kết quả rà soát độc lập. Commit cuối trước khi gọi `merge-if-green.sh` ghi nhật ký, plan `done`, ticket `on-staging`: PR không merge thì các trạng thái đó cũng không vào nhánh chính.

**6. Trong lúc làm, hỏi dev thay vì tự quyết.** Các lệch mà phiên tự chạy tự làm rồi báo (ADR-0029, ADR-0032, ADR-0033) được hỏi dev trước, rồi ghi vào dòng "Lệch so với plan" của nhật ký. Gặp điều kiện dừng (`risk-scoring.md`, mục 4) hay phát hiện task chạm điểm chặn thì không làm tiếp theo đường làm nhanh: tăng phiên bản plan, để `draft`, đổi PR thành nháp gắn nhãn `needs-human`, và task đi đường thường. Dev bỏ task giữa chừng thì vẫn đưa yêu cầu, ticket (`stopped`) và plan (`stopped`) lên nhánh chính bằng một PR tài liệu, để lịch sử không mất.

**7. Số liệu và kiểm tra sau merge.**

- Token: `scripts/token-usage.py mark T-NNN` khi bắt đầu, `mark chung` khi xong (`docs/ops/token-usage.md`).
- Dòng kiểm tra thủ công của plan: `(emulator: ...)` thì chạy tay `device-session.yml` sau khi merge, hoặc dev thử ngay trên máy; `(máy thật: ...)` và `(người: ...)` thì dev kiểm trong phiên khi được, kết quả ghi vào nhật ký. Muốn có bản staging ngay thì chạy tay `staging.yml`.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Đường làm nhanh, đủ tài liệu, dev duyệt trong phiên, giữ CI và `merge-if-green.sh` (đã chọn) | Một PR, không chờ phiên; lịch sử đầy đủ; cổng chất lượng như cũ | AI có thêm một quy trình phải nhớ; dev phải ngồi cùng |
| Giữ như cũ, việc nhỏ cũng chờ phiên tự chạy | Một đường duy nhất | Việc 15 phút chờ tới hôm sau, tốn nhiều PR |
| Làm nhanh không ticket, không plan, chỉ mô tả PR | Nhanh nhất | Mất lịch sử; `status.py`, retro, bài học không thấy; plan sau không đọc được việc trước |
| Làm nhanh, bỏ cả CI, chỉ chạy `check.sh` trên máy | Không chờ CI | `ui-test` và R8 chỉ chạy trong CI; trái quy tắc chỉ merge bằng `merge-if-green.sh`; nhánh chính đỏ làm hỏng phiên đêm |
| Phiên tự chạy ban ngày với danh sách một task | Không cần quy tắc mới | Vẫn đủ chi phí cố định của phiên và các PR duyệt |

## Hệ quả

- **Tích cực:**
  - Việc nhỏ lên nhánh chính trong cùng buổi, bằng một PR.
  - Mọi việc, dù đi đường nào, đều có yêu cầu, ticket, plan và nhật ký.
  - Cổng chất lượng không đổi; `merge-if-green.sh` tự chặn đường làm nhanh khi phiên tự chạy đang chạy.
- **Phải chấp nhận:**
  - Plan rủi ro thấp được duyệt bằng lời trong phiên, không qua họp.
  - Không có lượt rà soát cả danh sách trước khi chạy; xung đột với danh sách sắp chạy do AI tìm và báo dev.
  - Không có báo cáo phiên; muốn xem việc làm nhanh thì đọc nhật ký plan và PR (`lane: quick`).
- **Việc AI phải tuân thủ:** chỉ đi đường làm nhanh khi dev ngồi cùng suốt task, plan không chạm điểm chặn nào kể cả điểm 7, không thêm quyền, thư viện hay SDK, mức rủi ro Thấp và tiêu chí E dưới 2, ticket cỡ S hoặc M khi dev đồng ý, kịch bản có trong spec `confirmed` hoặc dev ngồi cùng là người xác nhận spec của feature và đã xác nhận phần đổi; không đi đường làm nhanh khi có phiên tự chạy đang chạy, và báo dev khi danh sách chạy `approved` sắp chạy sửa cùng file; vẫn ghi yêu cầu, ticket, plan đủ mục theo mẫu với `lane: quick`, chạy `scripts/plan-check.py` khi plan còn `draft` và sửa hết lỗi; chỉ chuyển plan sang `approved` và ghi mục 9 sau khi dev đồng ý rõ ràng trong chính phiên đó, ghi người duyệt, ngày và lời đồng ý; vẫn viết test trước, chạy `scripts/check.sh`, rà soát độc lập bằng agent con ngữ cảnh mới theo bước 6 của `docs/auto-run.md`, chờ CI và merge chỉ bằng `scripts/merge-if-green.sh`, chờ `main-guard`; một PR cho cả task trên nhánh `claude/quick-T-NNN-<tu-khoa>`, mô tả ghi "Đường làm nhanh (ADR-0034)", commit cuối trước khi merge ghi nhật ký, plan `done`, ticket `on-staging`; hỏi dev trước mọi lệch mà phiên tự chạy được tự làm rồi báo, ghi vào nhật ký; gặp điều kiện dừng hay điểm chặn thì tăng phiên bản plan, để `draft`, đổi PR thành nháp gắn `needs-human` và chuyển task sang đường thường; dev bỏ task thì vẫn đưa yêu cầu, ticket và plan `stopped` lên nhánh chính bằng PR tài liệu; đánh dấu token bằng `scripts/token-usage.py mark T-NNN` khi bắt đầu và `mark chung` khi xong.
