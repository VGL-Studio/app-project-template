---
id: T-100
title: merge-if-green.sh không từ chối vì một lần chạy bị hủy đã có lần chạy cùng tên sau nó
feature: quy-trinh
spec:                        # ticket quy trình: nguồn là bài học L-164; không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: on-staging               # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa scripts/merge-if-green.sh: plan rủi ro cao, duyệt chéo
size: S
depends_on: []
sprint:
plan: docs/features/quy-trinh/plans/T-100-merge-bo-qua-lan-huy-cu.md
auto_run: false              # làm có người, như T-083
created: 2026-11-09
---

# T-100: MERGE-IF-GREEN.SH KHÔNG TỪ CHỐI VÌ MỘT LẦN CHẠY BỊ HỦY ĐÃ CÓ LẦN CHẠY CÙNG TÊN SAU NÓ

## Mô tả

Ngày 2026-11-09, commit đầu của PR #432 có hai lần chạy `pr-check`. `concurrency` của workflow hủy lần đầu. `scripts/merge-if-green.sh` gom mọi kiểm tra `completed` có kết quả `cancelled` trên commit, không xét lần nào mới nhất, nên từ chối ngay (`TỪ CHỐI: có kiểm tra không qua: build-and-test.`), kể cả với `--update`, trong khi lần cùng tên sau đó đang chạy. `scripts/wait-run.sh` thì xét lần chạy mới nhất. Phải đẩy thêm một commit rỗng mới merge được (L-164, anh Nhật nhận ngày 2026-11-09: "Nhận L-164, tạo ticket và viết plan.").

## Phạm vi

- `scripts/merge-if-green.sh`, bước "có kiểm tra không qua": một kiểm tra `cancelled` không chặn khi trên cùng commit có một lần chạy khác của kiểm tra đó (cùng tên, cùng ứng dụng) bắt đầu sau nó, hoặc chưa bắt đầu. Lần mới nhất `cancelled` vẫn từ chối. Các kết quả đỏ khác (`failure`, `timed_out`, `action_required`, `startup_failure`) vẫn từ chối dù có lần sau xanh.
- `scripts/merge-if-green.sh`, `check_one`: chọn lần mới nhất theo cùng thứ tự, lần chưa bắt đầu (`started_at` rỗng) là mới nhất.
- `scripts/tests/run.sh`: test cho từng ca, kèm đối chứng, và mỗi kiểm tra khác mà ca mới phải đi qua một test.
- `docs/auto-run.md`, bước 8, và skill `merge-pr`: `wait-run.sh --pr` hết giờ mà chưa thấy lần chạy nào của PR thì đẩy một commit rỗng rồi chờ lại, không đóng mở lại PR.

**Không bao gồm:**

- Đổi `concurrency` của `pr-check.yml`.
- Cho qua một lần `failure` của một lần chạy workflow cũ khi lần chạy workflow sau xanh. Chạy lại job đỏ trong cùng lần chạy thì GitHub đã chỉ trả lần mới nhất (`filter=latest`), không cần sửa.

## Rủi ro sơ bộ

Sửa `scripts/merge-if-green.sh`, điểm chặn 7 (`ci`): plan rủi ro cao, duyệt chéo. Sai hướng nới (bỏ qua một lần hủy không có lần sau) cho merge PR chưa có CI xanh; test đối chứng giữ hướng đó.

## Định nghĩa hoàn thành

- [ ] PR có một lần `cancelled` cũ và một lần cùng tên đang chạy: script chờ (mã 2), không từ chối; lần sau xanh thì merge.
- [ ] Lần mới nhất `cancelled`, hay lần đỏ khác bất kể có lần sau: vẫn từ chối.
- [ ] `check_one` coi lần chưa bắt đầu là mới nhất.
- [ ] `docs/auto-run.md` bước 8 nói cách xử lý PR không có lần chạy `pr-check`.
- [ ] Có test tự động cho từng điểm, kèm đối chứng.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, `main-guard` xanh. L-164 `applied`.
