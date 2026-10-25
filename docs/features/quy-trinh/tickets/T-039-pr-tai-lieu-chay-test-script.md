---
id: T-039
title: PR chỉ có tài liệu vẫn chạy test của script
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-040, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: approved
risk: high
size: S
depends_on: []
sprint:
plan: features/quy-trinh/plans/T-039-pr-tai-lieu-chay-test-script.md
created: 2026-10-25
---

# T-039: PR CHỈ CÓ TÀI LIỆU VẪN CHẠY TEST CỦA SCRIPT

## Mô tả

Hôm nay PR chỉ sửa tài liệu đi qua `pr-check` mà không chạy gì: job `build-and-test` chỉ in một dòng rồi xanh. Nhiều test trong `scripts/tests/run.sh` kiểm chính tài liệu (số bài học, `foundation.md`, `status.py`, quét mã thật, danh sách chạy mới nhất), nên lỗi tài liệu vào nhánh chính mà không ai biết (L-040). Sau ticket này, PR không chạm code vẫn chạy `scripts/tests/run.sh` trong một nhánh nhẹ của `build-and-test`, không Gradle, không Android SDK, khoảng 2 phút; đỏ thì `merge-if-green.sh` từ chối như mọi kiểm tra đỏ khác.

## Phạm vi

- `.github/workflows/pr-check.yml`: job `build-and-test` chạy `scripts/tests/run.sh` khi bộ lọc `code` không khớp; bình luận lỗi cho cả nhánh nhẹ.
- `scripts/tests/run.sh`: test cấu trúc workflow (bước nhẹ có, không dùng Gradle, tên job khớp `merge-if-green.sh`) và test `merge-if-green.sh` với hình dạng kiểm tra của PR chỉ có tài liệu.
- `docs/auto-run.md`: PR báo cáo của phiên giờ có CI khoảng 2 phút, cách chờ và cách xử lý khi đỏ.

**Không bao gồm:**

- `main-guard.yml` giữ `paths-ignore` cho tài liệu (lý do ở mục 8 của plan, câu hỏi 1).
- Không đổi `scripts/merge-if-green.sh` và `scripts/check.sh`.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI, quy trình phiên tự chạy): đổi cổng chất lượng của mọi PR. Rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] PR chỉ có tài liệu chạy `scripts/tests/run.sh` trong `build-and-test`, không Gradle, không Android SDK.
- [ ] PR có code vẫn chạy đúng như trước (`scripts/check.sh`, build release khi cần).
- [ ] Có test tự động cho cấu trúc workflow và cho cách `merge-if-green.sh` đọc kết quả.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Một PR thử chỉ có tài liệu, cố ý có lỗi, đỏ đúng chỗ và bị `merge-if-green.sh --dry-run` từ chối; PR đó đóng, không merge.
