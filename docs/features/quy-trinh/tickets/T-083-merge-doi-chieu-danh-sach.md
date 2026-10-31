---
id: T-083
title: Khi phiên tự chạy đang chạy, merge-if-green.sh chỉ cho merge nhánh task có trong danh sách của phiên
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-129 (RUN-20261030-01) và L-133 (PR #337), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # đánh giá sơ bộ: low | medium | high
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-010]
sprint:                      # điền khi được chọn vào sprint
plan:                        # đường dẫn plan khi đã có
auto_run: false              # làm có người: sửa chính cổng merge mà phiên tự chạy dùng (như T-075)
created: 2026-10-30
---

# T-083: KHI PHIÊN TỰ CHẠY ĐANG CHẠY, MERGE-IF-GREEN.SH CHỈ CHO MERGE NHÁNH TASK CÓ TRONG DANH SÁCH CỦA PHIÊN

## Mô tả

Trong phiên RUN-20261030-01, hai PR làm ngoài phiên được merge vào nhánh chính khi phiên đang `running`: #320 (T-080, nhánh `claude/T-080-bo-build-agp-9`) lúc 03:38 UTC và #324 (T-075, nhánh `claude/T-075-emulator-sau-merge`) lúc 05:24 UTC (L-129). Hai task của phiên phải gộp nhánh chính và chạy lại CI; plan T-068 phải so lại classpath trên bộ build mới. Nguyên nhân: khối kiểm phiên đang chạy của `scripts/merge-if-green.sh` cho mọi nhánh khớp `claude/T-[0-9]*` merge, coi là task của phiên, mà không đối chiếu mã task với danh sách chạy (L-133, PR #337). Anh Nhật chọn cách sửa chặt ngày 2026-10-30: chặn bằng script, không chỉ bằng quy ước tên nhánh.

## Phạm vi

- `scripts/merge-if-green.sh`, khối kiểm phiên đang chạy: khi danh sách trên nhánh báo cáo `claude/RUN-<id>-report` có `status: running`, nhánh `claude/T-NNN-...` và `claude/fix-T-NNN-...` chỉ được merge khi `T-NNN` có trong bảng mục 1 của danh sách đó. Đọc bảng theo đúng cách `scripts/resume-check.py` và `scripts/run-watchdog.py` đã đọc (`^\|\s*\d+\s*\|\s*(T-\d+)\s*\|`), hay dùng chung một hàm, để ba script không hiểu danh sách khác nhau. Nhánh báo cáo của phiên và bản revert đúng (ADR-0025) giữ như hôm nay.
- Thông báo từ chối nêu mã task, mã phiên và cách làm tiếp (chờ phiên xong, hay người tự merge có chủ đích).
- Test trong `scripts/tests/run.sh` cho từng nhánh của kiểm tra (L-058): task trong danh sách được merge; task ngoài danh sách bị từ chối; nhánh `fix-` của task trong danh sách được merge; không có phiên `running` thì không đổi gì; hai phiên `running` cùng lúc (nếu có thể xảy ra).
- Cập nhật `docs/auto-run.md` mục 1 (người không merge khi phiên chạy; script nay chặn cả nhánh `claude/T-*` ngoài danh sách), bài học L-129 và L-133 (`applied`, dòng Áp dụng).
- Plan xem cả đường merge bằng nút trên GitHub: script không chặn được đường đó. Đề xuất (không bắt buộc làm trong ticket này): luật bảo vệ nhánh hay một kiểm tra bắt buộc báo đỏ khi có phiên `running` mà PR không thuộc phiên.

**Không bao gồm:**

- Quy ước tên nhánh mới cho task làm ngoài phiên (cách sửa nhẹ của L-133): không cần khi script đã đối chiếu danh sách.

## Rủi ro sơ bộ

`scripts/merge-if-green.sh` là file được bảo vệ và là cổng merge của mọi phiên tự chạy: plan rủi ro cao, duyệt chéo. Sai một nhánh của kiểm tra thì phiên không merge được task của chính nó (phiên dừng hết) hay lại để lọt PR ngoài phiên. Làm có người theo dõi, không đưa vào phiên tự chạy.

## Định nghĩa hoàn thành

- [ ] Khi có phiên `running`, PR từ nhánh `claude/T-*` của task ngoài danh sách bị từ chối; task trong danh sách, nhánh báo cáo và bản revert đúng vẫn merge được; có test cho từng trường hợp.
- [ ] `docs/auto-run.md`, L-129, L-133 cập nhật.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
