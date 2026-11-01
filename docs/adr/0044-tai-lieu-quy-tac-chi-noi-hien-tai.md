---
id: ADR-0044
title: Tài liệu quy tắc chỉ nói quy tắc hiện tại; lịch sử nằm ở những chỗ dành riêng và không sang app
status: proposed             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-01
deciders: [Nhật]
supersedes:                  # bổ sung ADR-0017 (thứ script khởi tạo bỏ) và ADR-0030 (bản dự án mẫu mà script đồng bộ dựng)
superseded_by:
---

# ADR-0044: TÀI LIỆU QUY TẮC CHỈ NÓI QUY TẮC HIỆN TẠI

## Bối cảnh

Repo này vừa là dự án mẫu vừa là nơi chạy quy trình hằng ngày. Mỗi bài học, sự cố hay lần sửa đều để lại dấu vết trong tài liệu quy tắc: mã dẫn chiếu trong ngoặc như `(L-114)` hay `(T-084, L-143)`, và những câu kể lại cách làm cũ ("trước đây…", "đã gặp với…"). Ngày 2026-11-01 có khoảng 430 mã L-/T-/PB-/RUN- nằm trong `CLAUDE.md`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/ops/`, `docs/templates/` và `.claude/skills/`. Bảy file chiếm khoảng 85%: `docs/ops/ads-guide.md`, `docs/auto-run.md`, `docs/ops/ads-business-rules.md`, `docs/ops/failure-playbook.md`, `docs/CLAUDE.md`, `docs/ops/pre-run-review.md` và `docs/ops/staging-and-runs.md`. Chú thích trong code và script có thêm khoảng 130 mã, trong 7 file.

`scripts/new-app.sh` đã bỏ sổ bài học, báo cáo phiên và ticket khi tạo app (ADR-0017). Vì vậy ở app, các mã trên trỏ tới những thứ không có. AI của app vẫn đọc mọi chữ đó trong mỗi phiên, nên tốn token và dễ làm theo một quyết định đã cũ. Ngoài ra ADR có những chuỗi sửa chồng nhau: quảng cáo qua ADR-0026, 0036, 0037, 0038; lệch phạm vi qua ADR-0028, 0029, 0032, 0033. ADR cũ của mỗi chuỗi vẫn `accepted` nhưng chỉ còn đúng một phần, nên người đọc phải đọc cả chuỗi mới biết quy tắc hiện tại.

Hiện có hai app tạo từ repo này, cả hai nhận thay đổi qua `scripts/template-sync.sh`. Bản app nhận phải ổn định từ commit này sang commit khác, để đồng bộ không sinh xung đột giả.

## Quyết định

1. **Tài liệu quy tắc** gồm `CLAUDE.md`, `README.md`, `docs/CLAUDE.md`, `docs/auto-run.md`, `docs/risk-scoring.md`, `docs/ops/`, `docs/templates/`, `.claude/skills/` và mục "Việc AI phải tuân thủ" của ADR. Các tài liệu này viết quy tắc ở thì hiện tại. Lý do cần thiết viết thành một vế câu, ví dụ "thêm `--rerun`, vì không có nó Gradle có thể bỏ qua test và giữ ảnh cũ". Tài liệu quy tắc không ghi mã L-/T-/PB-/RUN- và không kể lại cách làm cũ. Dẫn chiếu `ADR-NNNN` vẫn được dùng.
2. **Lịch sử nằm ở những chỗ dành riêng:** `docs/lessons.md`, `docs/runs/`, ticket, plan, phản hồi và yêu cầu trong `docs/features/`, `docs/metrics/`, `docs/inbox/`, `docs/test-sessions/`, phần Bối cảnh của ADR, và git log. Muốn biết vì sao có một quy tắc thì tra ngược từ dòng **Áp dụng** của bài học, hoặc dùng `git blame`.
3. **Áp dụng bài học** (`/lesson apply`) là sửa quy tắc theo mục 1. Mã bài học chỉ ghi ở `lessons.md` và trong mô tả PR.
4. **ADR mới sửa một phần một ADR còn hiệu lực** thì là ADR hợp nhất: nó chép lại toàn bộ quy tắc còn đúng của ADR cũ cùng phần mới, ghi `supersedes`, và ADR cũ chuyển `superseded`. Không để một quy tắc phải đọc từ hai ADR còn hiệu lực trở lên. ADR `accepted` vẫn không bị sửa nội dung (`docs/CLAUDE.md`). Chuỗi đang có được hợp nhất trong đợt dọn.
5. **Bản cho app bỏ ADR `superseded`.** `new-app.sh` bỏ chúng khi tạo app. `template-sync.sh` bỏ chúng ở cả hai phía khi dựng dự án mẫu, nên ADR vừa bị thay sẽ bị xóa khỏi app trong đợt đồng bộ. Trong repo này ADR `superseded` vẫn được giữ (không xóa tài liệu). Tài liệu quy tắc không dẫn tới ADR `superseded`.
6. **Kiểm bằng máy** trong `scripts/tests/`, chạy ở CI:
   - tài liệu quy tắc có mã L-/T-/PB-/RUN- thì báo đỏ, trừ file có tên trong danh sách chưa dọn;
   - bản app do `new-app.sh` dựng có link tương đối gãy thì báo đỏ.

   Danh sách chưa dọn chỉ được ngắn đi: mỗi PR dọn xóa dòng của file mình đã dọn, không PR nào thêm dòng.
7. **Dọn từng đợt**, mỗi PR vài file. Trong một PR dọn, mọi quy tắc của bản cũ phải còn ở bản mới, không bị nới. Người rà soát đối chiếu từng quy tắc. Sau mỗi đợt lớn, chạy `template-sync` sang một trong hai app để thử, theo `docs/ops/to-app.md`. Chú thích trong code và script dọn ở đợt cuối, cùng quy tắc.
8. **Khởi đầu lại repo** (bỏ lịch sử git, chuyển repo cũ thành repo lưu trữ) chưa quyết. Việc này xét sau khi dọn xong, bằng một ADR riêng. Nếu làm thì phải đồng bộ cả hai app lên commit cuối của repo cũ và gỡ hết dòng `hold=` trước, rồi mới đổi `base=` sang commit gốc của repo mới.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Dọn tại chỗ, lịch sử ở chỗ dành riêng (đã chọn) | Một bản duy nhất; chính repo này cũng sạch; `template-sync` gần như không đổi | Một đợt dọn lớn; người bảo trì mất lối tắt mã dẫn chiếu ngay trong câu |
| Repo mẫu riêng, giữ bằng tay | App nhận bản sạch nhất | Hai nguồn sự thật; repo mẫu không ai chạy thật nên cũ dần; phải định lại vai của `template-sync` và `from-app` |
| Lọc bằng script khi tạo và đồng bộ app | Một nguồn, không viết lại văn | Script chỉ cắt được mã dẫn chiếu, không cắt được câu kể chuyện hay chuỗi ADR |
| Thư mục lớp phủ chứa bản sạch của các tài liệu nhiều lịch sử | Bản sạch viết tốt, đồng bộ ổn định | Hai bản của mỗi tài liệu trong lớp phủ; cần kiểm cặp file để chúng không lệch |
| Bỏ ngay lịch sử git | Repo trông sạch | Không giúp gì cho app (app mới đã có repo git riêng); làm hỏng `base=` và `hold=` của hai app; mất đường tra ngược lý do |

## Hệ quả

- **Tích cực:**
  - App nhận tài liệu chỉ nói quy tắc đang dùng.
  - AI ở cả repo này lẫn app đọc ít chữ hơn mỗi phiên.
  - Quy tắc hiện tại đọc được từ một ADR, không phải ghép từ một chuỗi.
- **Phải chấp nhận:**
  - Một đợt dọn kéo dài nhiều PR.
  - Lý do chi tiết của một quy tắc phải tra ngược qua `lessons.md` hoặc `git blame`.
  - Khi viết lại có thể làm mất một ý tinh tế; bước đối chiếu từng quy tắc ở mục 7 để chặn điều này.
  - Phần Bối cảnh của ADR cũ còn mã dẫn chiếu, vì ADR `accepted` không được sửa.
- **Việc AI phải tuân thủ:** tài liệu quy tắc viết quy tắc ở thì hiện tại, lý do cần thiết viết thành một vế câu; không ghi mã L-, T-, PB- hay RUN- trong tài liệu quy tắc và không kể lại cách làm cũ, các mã đó chỉ ghi ở `docs/lessons.md`, báo cáo phiên, ticket, plan và mô tả PR; áp dụng bài học là sửa quy tắc theo cách này; ADR mới sửa một phần ADR còn hiệu lực thì hợp nhất, chép lại quy tắc còn đúng và cho ADR cũ `superseded`; tài liệu quy tắc không dẫn tới ADR `superseded`; không thêm file vào danh sách chưa dọn; PR dọn tài liệu giữ đủ mọi quy tắc của bản cũ, không nới quy tắc nào.
