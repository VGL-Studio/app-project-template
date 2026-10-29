---
id: T-061
title: Thanh đáy chừa chỗ lúc đang tải
feature: ads
spec: features/ads/spec.md
scenarios: [KB-17]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-065]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-061: THANH ĐÁY CHỪA CHỖ LÚC ĐANG TẢI

## Mô tả

Nội dung của màn có thanh đáy chừa chỗ ngay lúc khe đáy đang tải, nên không bị đẩy khi quảng cáo về; nạp lỗi thì co về 0 (ADR-0036, điểm 3, thay luật D-04; anh Nhật chọn 2026-10-29).

## Phạm vi

- Đổi `BottomAdBar.kt` (khoảng chừa hôm nay chỉ có khi khe đã có quảng cáo, dòng 68-72) theo câu hỏi 37: chiều cao khe theo KB-32 (câu hỏi 49), banner bằng chiều cao khung; nạp lại sau lỗi không chừa lại tới khi có quảng cáo.
- Viết lại câu khẳng định cũ của D-04 theo ADR-0031; ghi lại ảnh chuẩn bị đổi (liệt kê trong plan).
- Sửa `ads-business-rules.md` D-04 và mục 12, `ads-guide.md` (dòng về khoảng chừa của thanh đáy).

**Không bao gồm:**

- Lớp native thu gọn (vẫn không chừa, D-03).

## Rủi ro sơ bộ

Module core, quảng cáo, đổi luật đã chốt và ảnh chuẩn screenshot của màn mẫu và màn xem thử.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
