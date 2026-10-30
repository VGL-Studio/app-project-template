---
id: T-077
title: Bớt lần chạy lại pr-check: PR nháp và nhánh chính chỉ đổi tài liệu
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: planned
risk: high
size: M
depends_on: [T-074, T-075, T-076]
sprint:
plan: features/quy-trinh/plans/T-077-bot-chay-lai-pr-check.md
auto_run: false               # làm có người: phiên tự chạy dựa vào chính cổng chất lượng mà ticket sửa (như T-051)
created: 2026-10-29
---

# T-077: BỚT LẦN CHẠY LẠI PR-CHECK: PR NHÁP VÀ NHÁNH CHÍNH CHỈ ĐỔI TÀI LIỆU

## Mô tả

`pr-check` chạy trung bình 1,7 lần mỗi PR: mỗi lần đẩy thêm commit, và mỗi lần `merge-if-green.sh --update` cập nhật nhánh vì nhánh chính có commit mới, kể cả commit chỉ đổi tài liệu (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 5). Sau ticket này, PR nháp không chạy `pr-check`, và PR code không phải cập nhật nhánh rồi chạy lại khi nhánh chính chỉ đổi tài liệu từ lúc PR được kiểm.

## Phạm vi

- `pr-check.yml`: khai báo đủ `types: [opened, synchronize, reopened, ready_for_review]` (khai báo `types` thì bộ mặc định mất, thiếu `synchronize` là không kiểm commit mới), bỏ qua PR nháp ở cấp job. `merge-if-green.sh` đã từ chối PR nháp nên không cần kiểm thêm; plan kiểm PR nháp của phiên (PR plan, PR sửa nguyên nhân) và các chỗ đang chờ CI của PR nháp (`docs/auto-run.md`).
- `merge-if-green.sh`: chỉ với PR mà `build-and-test` của commit cuối đã chạy nhánh `check.sh` (PR code), khi nhánh chậm hơn nhánh chính và mọi commit của nhánh chính từ điểm tách tới nay chỉ đổi tài liệu (cùng tập đường dẫn `paths-ignore` của `main-guard`), merge mà không cập nhật nhánh. Squash merge với mã commit đã kiểm vẫn giữ. Xung đột thì vẫn cập nhật. PR chỉ có tài liệu (báo cáo phiên, bài học, ADR) vẫn cập nhật nhánh và chạy lại `pr-check` (khoảng 2 phút): commit của nó không kích hoạt `main-guard` (`paths-ignore`), nên đây là lần duy nhất tổ hợp tài liệu hai bên được kiểm, ví dụ bài học trùng số.
- `main-guard` của T-074 so nội dung commit merge với commit đã kiểm; phần khác chỉ là tài liệu thì chạy `scripts/tests/run.sh` để kiểm tổ hợp tài liệu của PR với tài liệu mới của nhánh chính. Plan kiểm hai ticket khớp nhau.
- Sửa ADR-0010 mục 1 ("nhánh của PR đã cập nhật theo nhánh chính mới nhất") bằng ADR mới hay mục mới của ADR của T-074.

**Không bao gồm:**

- Đổi `concurrency` của `pr-check` (đã hủy lần chạy cũ khi đẩy commit mới).

## Rủi ro sơ bộ

Sửa `scripts/merge-if-green.sh` và `.github/workflows/pr-check.yml`: plan rủi ro cao, duyệt chéo, mỗi kiểm tra của script merge một test (L-058). Sai điều kiện "chỉ tài liệu" thì tổ hợp code của PR với code mới của main vào main mà chưa kiểm; `main-guard` bắt được vì nội dung khác phần đã kiểm ngoài tài liệu. Với PR code, tổ hợp tài liệu (bài học trùng số, `foundation.md` lệch) chỉ bị bắt sau merge bằng `scripts/tests/run.sh` của `main-guard` (T-074), không còn chặn trước merge. Áp nhầm quy tắc cho PR chỉ có tài liệu thì tổ hợp tài liệu không được kiểm ở đâu: test phải có trường hợp này.

## Định nghĩa hoàn thành

- [ ] PR nháp không chạy `pr-check`; chuyển sang sẵn sàng thì chạy.
- [ ] Nhánh chính chỉ có thêm commit tài liệu: `merge-if-green.sh` merge không cập nhật nhánh. Có commit code: vẫn cập nhật và chờ CI. PR chỉ có tài liệu: luôn cập nhật. Test cho cả ba.
- [ ] ADR cập nhật, `foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
