---
id: ADR-0048
title: Plan và danh sách chạy của một đợt được duyệt một lần trong một PR, không cần họp, sau khi đã qua plan-check, run-check và rà soát độc lập
status: proposed             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-11-12
deciders: [Nhật]
supersedes:                  # không thay ADR nào; đổi bước 6, 7 của docs/README.md, mục Khi lập danh sách chạy của docs/CLAUDE.md, cột Trong họp của docs/risk-scoring.md
superseded_by:
---

# ADR-0048: PLAN VÀ DANH SÁCH CHẠY CỦA MỘT ĐỢT ĐƯỢC DUYỆT MỘT LẦN TRONG MỘT PR, KHÔNG CẦN HỌP, SAU KHI ĐÃ QUA PLAN-CHECK, RUN-CHECK VÀ RÀ SOÁT ĐỘC LẬP

## Bối cảnh

Từ lúc có ticket tới lúc merge, máy chạy mỗi task khoảng 45 tới 60 phút (đo trên dòng thời gian của năm phiên gần nhất), còn phần lớn thời gian còn lại là chờ người ở các bước duyệt. Một đợt việc hiện đi qua hai vòng duyệt nối tiếp:

1. Mỗi plan một PR. Dev duyệt, AI chuyển plan sang `approved`, PR được merge.
2. Sau đó AI mới lập danh sách chạy (`/run-list`): `run-check.py` đòi plan đã `approved`, rồi một lần rà soát độc lập, rồi một PR nữa để dev duyệt danh sách.

Hai phiên gần nhất cho thấy khoảng cách này: plan T-101, T-102 merge lúc 03:36 tới 03:49, PR danh sách chạy mở lúc 04:14 và merge lúc 04:41, phiên bắt đầu 05:09; plan T-097 tới T-099 merge lúc 14:11, danh sách mở lúc 15:46 và merge lúc 16:17. Mỗi đợt mất thêm 1 tới 2 giờ và một lần dev phải quay lại đọc, dù những gì dev cần quyết ở danh sách (thứ tự, số task, cách mở phiên) đã biết từ lúc duyệt plan.

Quy trình còn ghi một "họp đánh giá rủi ro" trước khi duyệt. Nhóm hiện chỉ có một dev; dev duyệt trên PR, không có buổi họp thật. Ghi họp là bước bắt buộc làm người đọc tưởng phải chờ tới buổi họp mới duyệt được.

Lớp kiểm tra của danh sách (`run-check.py` và rà soát độc lập theo `docs/ops/pre-run-review.md`) tìm mâu thuẫn giữa spec và plan, giữa các plan, và test chưa đủ tốt. Các điểm chặn ở đó thường dẫn tới sửa plan. Kiểm sau khi plan đã `approved` thì phải soạn plan phiên bản mới; kiểm trước khi duyệt thì sửa ngay trên bản `draft`.

## Quyết định

**1. Một PR duyệt cho cả đợt.** Khi các plan `draft` của một đợt đã qua `scripts/plan-check.py` và rà soát độc lập của từng plan, AI lập danh sách chạy `draft` ngay trong cùng PR với các plan đó (nhánh `claude/batch-YYYYMMDD-NN`). Các plan đã có PR riêng thì gom vào nhánh của đợt; PR riêng được đóng, ghi số PR của đợt.

**2. Kiểm danh sách trước khi duyệt.** `scripts/run-check.py` kiểm danh sách khi plan còn `draft`: lỗi "plan và ticket chưa `approved`" được bỏ qua cho plan có trong cùng PR của đợt, mọi kiểm tra khác giữ nguyên mức. Rà soát độc lập của danh sách theo `docs/ops/pre-run-review.md`, mục 2, chạy trên bản `draft` đó. Điểm chặn thì sửa plan `draft` ngay trong PR, rồi rà soát lại phần vừa sửa.

**3. Dev duyệt một lần.** AI đưa dev một tóm tắt cho cả đợt: từng plan (phạm vi, mức rủi ro, điểm chặn, câu hỏi cần trả lời), thứ tự chạy, số task, `open_with`, ước tính token và thời gian, việc dev phải kiểm trước phiên, kèm câu duyệt có sẵn. Dev duyệt cả đợt, duyệt một phần (AI bỏ plan chưa duyệt khỏi danh sách và chạy lại `run-check.py`), hoặc yêu cầu sửa. Lời duyệt chỉ có hiệu lực với đúng nội dung đã đưa; nội dung đổi sau đó thì đưa lại và hỏi lại.

**4. Chuyển trạng thái trong một commit.** Sau lời duyệt, AI chuyển các plan sang `approved` (ghi `reviewer`, `cross_reviewer` khi rủi ro Cao, `approved_at`, mục 9 kèm lời duyệt), chạy `scripts/status.py --sync` cho ticket, chuyển danh sách sang `approved`, chạy lại `run-check.py` (lúc này không bỏ qua lỗi nào), rồi đẩy. PR merge bằng `scripts/merge-if-green.sh` như mọi PR.

**5. Không bắt buộc họp.** Duyệt trên PR là đủ cho mọi mức rủi ro. Quy tắc người duyệt của `docs/risk-scoring.md` giữ nguyên: mức Thấp và Trung bình do chủ feature duyệt, mức Cao và plan chạm điểm chặn phải duyệt chéo (nhóm một dev thì người đó duyệt cả hai vai, ghi lý do ở mục 9). Nhóm có thể họp khi muốn bàn ảnh hưởng chéo; họp không phải điều kiện để duyệt.

**6. Đường cũ vẫn dùng được.** Plan duyệt lẻ (một PR một plan) rồi `/run-list` sau vẫn hợp lệ, ví dụ khi một plan phải chờ spec được xác nhận còn các plan khác đã sẵn sàng. Đường làm nhanh (ADR-0034) không đổi.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Một PR cho plan và danh sách, kiểm danh sách trên bản `draft` (đã chọn) | Bớt một vòng duyệt và 1 tới 2 giờ chờ mỗi đợt; điểm chặn của rà soát danh sách sửa được ngay trên `draft`, không cần plan phiên bản mới | PR lớn hơn; `run-check.py` cần chế độ bỏ qua lỗi trạng thái cho plan trong cùng PR; một plan chưa sẵn sàng có thể giữ cả đợt nếu không tách ra |
| Giữ hai vòng, AI lập danh sách ngay khi plan cuối được merge | Không đổi script | Dev vẫn phải quay lại duyệt lần hai; điểm chặn của rà soát danh sách vẫn cần plan phiên bản mới |
| Bỏ duyệt danh sách, phiên tự lấy mọi plan `approved` | Ít bước nhất | Mất lớp kiểm tra chéo giữa các plan và quyết định của dev về số task, cách mở phiên; trái nguyên tắc chỉ chạy danh sách đã duyệt |

## Hệ quả

- **Tích cực:** mỗi đợt dev đọc và duyệt một lần; danh sách sẵn sàng ngay khi dev duyệt plan; cùng với việc phiên tự bắt đầu khi danh sách `approved` lên nhánh chính (ticket riêng, sửa `.github/workflows/`), một lời duyệt đủ để phiên chạy.
- **Phải chấp nhận:** PR của đợt dài hơn PR một plan; `scripts/run-check.py` có thêm chế độ cho plan `draft` cùng PR (cần test riêng); `/run-list`, `/write-plan`, `docs/README.md` bước 6 và 7, `docs/CLAUDE.md` mục Khi lập danh sách chạy, `docs/ops/pre-run-review.md` và cột Trong họp của `docs/risk-scoring.md` phải sửa theo, trong PR làm ADR này sau khi nó được chấp nhận.
- **Việc AI phải tuân thủ:** lập danh sách chạy `draft` cùng PR với các plan `draft` của đợt khi các plan đã qua `plan-check.py` và rà soát độc lập của từng plan; chạy `run-check.py` và rà soát danh sách theo `docs/ops/pre-run-review.md` trên bản `draft`, chỉ bỏ qua lỗi trạng thái `approved` của plan có trong cùng PR; sửa điểm chặn ngay trên plan `draft` rồi rà soát lại phần sửa; đưa dev một tóm tắt cho cả đợt kèm câu duyệt có sẵn; lời duyệt chỉ áp cho đúng nội dung đã đưa, nội dung đổi thì hỏi lại; sau lời duyệt chuyển plan, ticket và danh sách sang `approved` trong một commit rồi chạy lại `run-check.py` không bỏ qua lỗi nào; không coi họp là điều kiện để duyệt, nhưng giữ quy tắc duyệt chéo của `docs/risk-scoring.md`.
