---
id: T-048
title: Dọn rewarded theo ghi chú rà soát T-047
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-047]
sprint:
plan: features/ads/plans/T-048-don-sau-ra-soat-t-047.md
created: 2026-10-28
---

# T-048: DỌN REWARDED THEO GHI CHÚ RÀ SOÁT T-047

## Mô tả

Gom bốn ghi chú mà rà soát độc lập của T-047 nêu nhưng phiên không sửa. Nguồn: báo cáo RUN-20261028-01, mục Cần người kiểm tra. Người dùng không thấy gì đổi ở đường thường; ngoại lệ hiếm: hết giờ chờ khóa khi cổng đồng ý đóng thì hành động chạy, nếu người duyệt chọn đổi luật (plan, câu hỏi 2). Test khóa thêm điều plan T-047 đã nói, chú thích nói đúng điều code làm.

## Phạm vi

- **Test `loadRound_switchStuck_releasesAfterWait`:** thêm câu khẳng định lượt nạp hết giờ đọc khóa không gọi `clearLoading` và `sendBlocked`. Lý do: hôm nay đổi `NOT_NEEDED` thành `SWITCHED_OFF` thì test vẫn xanh. Mỗi câu mới có bước đối chứng (L-055).
- **KDoc của `showDecided`:** nói "once", nhưng khi SDK gọi đồng bộ, một bộ xử lý nhập vào từ ba lần bấm trở lên nhận `OnScreen` ở mỗi lần `showAgain`. Plan quyết một trong hai cách: sửa KDoc cho đúng, hoặc sửa code cho đúng KDoc kèm test.
- **KDoc của `decide`:** đổi "asks for no load" thành "asks for no further load".
- **Hết hạn khi cổng đồng ý đang đóng:** hôm nay lần quyết định vẫn ghi `not_ready` và áp chính sách, đúng chữ plan T-047, và trường hợp này chưa xảy ra trong app thật. Plan quyết giữ nguyên (ghi lý do vào KDoc) hay đổi theo luật về cổng đồng ý (R-03, MC-05). Nếu đổi luật thì đặt câu hỏi cho người.

**Không bao gồm:**

- Thời gian chờ 5 giây và luật đã duyệt ở T-047.

## Rủi ro sơ bộ

Sửa test và KDoc của module core quảng cáo (điểm chặn 1 và 7). Phần cổng đồng ý có thể đổi hành vi nếu plan chọn đổi. Rủi ro cao theo luật, việc nhỏ.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
