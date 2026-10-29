---
id: ADR-0039
title: main-guard không chạy lại cổng chất lượng cho commit có nội dung đã được pr-check kiểm đầy đủ, chỉ chạy test của script khi phần khác chỉ là tài liệu
status: proposed             # proposed | accepted | superseded
origin: template             # template: quy tắc từ dự án mẫu, app không sửa | app: quyết định riêng của app
date: 2026-10-29
deciders: [Nhật]
supersedes:                  # thay một phần mục 2 của ADR-0010 ("chạy lại cổng chất lượng cho từng commit"); các quy tắc khác của ADR-0010 và ADR-0025 giữ nguyên
superseded_by:
---

# ADR-0039: MAIN-GUARD KHÔNG CHẠY LẠI CỔNG CHẤT LƯỢNG CHO COMMIT CÓ NỘI DUNG ĐÃ ĐƯỢC PR-CHECK KIỂM ĐẦY ĐỦ, CHỈ CHẠY TEST CỦA SCRIPT KHI PHẦN KHÁC CHỈ LÀ TÀI LIỆU

Nguồn: yêu cầu giảm chi phí GitHub Actions (`docs/features/quy-trinh/requests/2026-10-29-giam-phut-actions.md`), ticket T-074.

## Bối cảnh

ADR-0010 mục 2: `main-guard.yml` chạy lại `scripts/check.sh` cho mọi commit trên nhánh chính, trừ commit chỉ đổi tài liệu. Từ 2026-10-22 tới 2026-10-29 việc này tốn 757 phút Actions của dự án mẫu, khoảng 21% số phút, trong khi gói Free chỉ có 2.000 phút mỗi tháng cho cả tổ chức và tổ chức đã trả tiền cho phần vượt.

`scripts/merge-if-green.sh` (ADR-0010 mục 1) chỉ merge khi nhánh của PR đã cập nhật theo nhánh chính và `pr-check` xanh trên đúng commit cuối, rồi merge kiểu squash kèm mã commit đó. Commit merge vì vậy có cây file giống hệt commit đã kiểm. Chạy lại `check.sh` trên cùng một cây file chỉ bắt được test chập chờn (ADR-0025), không bắt được lỗi mới.

Nhưng "`pr-check` xanh" không phải lúc nào cũng có nghĩa đã chạy `check.sh`. PR không đổi code chỉ chạy `scripts/tests/run.sh` (L-040). Và PR sửa `pr-check.yml` được kiểm bằng chính bản đã sửa.

## Quyết định

**1. Ba cách kiểm.** Job `guard` của `main-guard.yml` gọi `scripts/main-guard-verified.sh` trước khi kiểm. Script chọn một trong ba cách:

- `skip`: không chạy `check.sh`, job xanh, log ghi "đã kiểm ở PR #N, commit <sha>". Chọn cách này khi đủ ba điều:
  - commit là commit merge của một PR vào nhánh chính;
  - lần `pr-check` mới nhất trên commit cuối của PR xanh, và bước "Cổng chất lượng (scripts/check.sh)" của job `build-and-test` có kết quả `success`, tức PR đã chạy cổng đầy đủ;
  - cây file của commit trên nhánh chính giống hệt cây file của commit đã kiểm.
- `tests`: chỉ chạy `scripts/tests/run.sh`. Chọn cách này khi đủ hai điều đầu, và mọi file khác nhau giữa hai cây đều nằm trong `docs/**` hay là `**/*.md` (nhánh chính có thêm tài liệu sau lần kiểm). `run.sh` kiểm cả tài liệu (số bài học, `foundation.md`, danh sách chạy, quét bí mật), và tài liệu của PR ghép với tài liệu mới của nhánh chính chưa được kiểm ở đâu khác.
- `full`: chạy `check.sh` như trước. Đây là cách cho mọi trường hợp khác, kể cả khi script không đọc được GitHub hay không lấy được commit đã kiểm.

**2. Luôn chạy đầy đủ khi commit đổi chính cổng kiểm.** Commit có thay đổi so với commit cha ở `.github/workflows/pr-check.yml`, `.github/workflows/main-guard.yml`, `scripts/check.sh`, `scripts/failure-kind.sh`, `scripts/find-breaking-commit.sh` hay `scripts/main-guard-verified.sh` luôn dùng `full`. Lần chạy lại với cổng đã sửa là lần đầu cổng mới chạy thật trên nhánh chính. Chạy tay (`workflow_dispatch`) cũng luôn dùng `full`.

**3. Kết quả vẫn là check run `guard`.** Cách `skip` là một bước bên trong job `guard`, không phải điều kiện `if:` ở cấp job, để check run `guard` có kết luận `success`. Các nơi đọc kết quả `main-guard` không đổi:

- `scripts/find-breaking-commit.sh` và `scripts/main-guard-status.sh` đọc check run `guard`;
- `start-run.yml` gọi `main-guard-status.sh`;
- `scripts/release.sh` chỉ build bản production từ commit có `main-guard.yml` kết luận `success`.

Commit xanh theo cách `skip` vì vậy cũng mở cổng cho bản production. Người duyệt ADR này chấp nhận nghĩa mới của "xanh": cây file đã qua `check.sh` đầy đủ ở PR, không nhất thiết chạy lại sau merge.

**4. Đỏ ở cách `tests` là nhánh chính đỏ**, như đỏ ở cách `full`: tìm commit gây lỗi, mở issue `main-broken`, đẩy nhánh revert (ADR-0010, ADR-0025).

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Tin lần kiểm đầy đủ của PR khi cây file giống hệt (đã chọn) | Bớt khoảng 6 phút cho mỗi lần merge code; giữ check run `guard` và mọi nơi đọc nó; không đổi `merge-if-green.sh` | Mất lần chạy lại bắt test chập chờn trên nhánh chính; thêm một script phải giữ đúng |
| Giữ như cũ | Không đổi gì | Khoảng 21% số phút Actions cho một lần kiểm trùng |
| Bỏ hẳn `main-guard`, chỉ dựa vào `merge-if-green.sh` | Rẻ nhất | Mất lớp bắt commit đẩy thẳng lên main, PR sửa cổng, và `main-broken` (ADR-0010 mục 2, ADR-0025) |
| `if:` ở cấp job để bỏ qua | Không tốn runner | Check run `guard` có kết luận `skipped`: `find-breaking-commit.sh` chờ như còn đang chạy, `main-guard-status.sh` báo "CHƯA KIỂM", phiên tự chạy không bắt đầu được |

## Hệ quả

- Lần merge code của một PR đã kiểm tốn khoảng 1 phút `main-guard` thay vì 5 tới 12 phút.
- Test chập chờn chỉ được ghi `flaky` khi `main-guard` chạy `full` (ADR-0025 mục 1). Ở cách `skip`, test chập chờn lộ ra ở `pr-check` của PR.
- Đổi tên bước "Cổng chất lượng (scripts/check.sh)" hay tên job `build-and-test` của `pr-check.yml` thì phải sửa `main-guard-verified.sh` theo. Test của script giữ hai file khớp nhau.

## Việc AI phải tuân thủ

- `main-guard.yml` chỉ bỏ qua `check.sh` qua `scripts/main-guard-verified.sh`, bằng một bước bên trong job `guard`, không bằng `if:` ở cấp job; script không chắc thì chọn `full`; commit đổi chính cổng kiểm (bảng ở mục 2) luôn chạy `full`; đổi tên bước cổng chất lượng hay tên job `build-and-test` của `pr-check.yml` thì sửa `main-guard-verified.sh` và test của nó cùng PR
