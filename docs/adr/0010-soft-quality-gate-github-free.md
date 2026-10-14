---
id: ADR-0010
title: Cổng chất lượng mềm trên GitHub Free, merge chỉ qua script kiểm tra
status: accepted
date: 2026-10-14
deciders: [Nhóm phát triển]
amends: ADR-0002
---

# ADR-0010: CỔNG CHẤT LƯỢNG MỀM TRÊN GITHUB FREE, MERGE CHỈ QUA SCRIPT KIỂM TRA

## Bối cảnh

ADR-0002 quyết định chỉ merge vào nhánh chính khi qua cổng chất lượng, và nhánh chính được bảo vệ. Tổ chức đang dùng gói GitHub Free, với repo private thì GitHub không cho bật bảo vệ nhánh, ruleset hay kiểm tra bắt buộc. Nhóm quyết định giữ gói Free. Vì vậy phải bảo đảm nguyên tắc của ADR-0002 bằng cách khác. Gói Free cũng giới hạn số phút GitHub Actions cho repo private, nên CI phải tiết kiệm.

## Quyết định

**1. Merge chỉ qua script kiểm tra.** Mọi lần merge vào nhánh chính, của AI hay của người, đều dùng script `scripts/merge-if-green.sh`. Script chỉ merge khi đủ các điều kiện:

- PR nhắm vào nhánh chính và không ở trạng thái nháp.
- Toàn bộ kiểm tra của `pr-check.yml` trên đúng commit cuối của PR đều xanh. Kiểm tra trên emulator được phép ở trạng thái bỏ qua khi PR không chạm vào giao diện.
- Nhánh của PR đã cập nhật theo nhánh chính mới nhất.
- Merge theo kiểu gộp một commit (squash), kèm mã commit đã kiểm tra, để GitHub từ chối nếu có ai đẩy thêm commit trong lúc đó.

AI không được merge bằng bất kỳ cách nào khác, kể cả nút merge, `gh pr merge` hay gọi thẳng API.

**2. Gác nhánh chính sau khi merge.** Workflow `main-guard.yml` chạy lại cổng chất lượng mỗi khi nhánh chính thay đổi. Nếu đỏ:

- Tự động tạo PR revert commit vừa gây lỗi và gắn nhãn `main-broken`.
- Phiên tự chạy đang chạy phát hiện nhãn này thì dừng ngay, theo điều kiện "nhánh chính đỏ" trong `docs/auto-run.md`.
- Dev quyết định merge PR revert hay sửa tiếp.

**3. Quy ước cho người.** Không đẩy thẳng lên nhánh chính. Mọi thay đổi, kể cả tài liệu, đều đi qua PR và script merge.

**4. Tiết kiệm phút GitHub Actions.**

- Test trên emulator chỉ chạy khi PR chạm vào code giao diện hoặc test giao diện. PR chỉ sửa tài liệu thì bỏ qua toàn bộ phần build.
- Screenshot test và phần lớn test giao diện chạy trên JVM (Robolectric, Roborazzi), nhanh và rẻ hơn emulator.
- Lưu đệm Gradle giữa các lần chạy; lần chạy mới trên cùng PR hủy lần chạy cũ đang dở.
- `main-guard.yml` không chạy test emulator, vì commit trên nhánh chính đã qua cổng ở PR.

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Bảo vệ mềm trên gói Free | Không tốn thêm chi phí | Không ngăn được người cố tình bỏ qua; dựa vào kỷ luật và công cụ |
| Nâng lên GitHub Team | GitHub chặn merge ở mức hệ thống | Tốn chi phí theo số thành viên |
| Chuyển repo sang public | Có bảo vệ nhánh miễn phí | Lộ quy trình nội bộ, không áp dụng được cho app thật |

## Hệ quả

- **Tích cực:** giữ được nguyên tắc chỉ merge khi CI xanh mà không tốn thêm chi phí; quyết định merge nằm trong một script có thể đọc và kiểm tra, không phụ thuộc vào cách AI diễn giải.
- **Phải chấp nhận:** GitHub không chặn ở mức hệ thống, nên một lần merge sai vẫn có thể xảy ra và chỉ được phát hiện sau đó bởi `main-guard.yml`. Khi nhóm mở rộng hoặc có app đang kinh doanh, nên xem lại việc nâng lên gói trả phí.
- **Việc AI phải tuân thủ:** chỉ merge bằng `scripts/merge-if-green.sh`; không sửa script này, các workflow trong `.github/` hay nội dung kiểm tra của cổng chất lượng, trừ khi đó chính là plan rủi ro cao đã được duyệt chéo; thấy nhãn `main-broken` thì dừng phiên.
