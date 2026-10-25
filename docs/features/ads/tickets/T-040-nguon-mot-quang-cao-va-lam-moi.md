---
id: T-040
title: Khe và nguồn native một quảng cáo: lấy, hủy, hết hạn, làm mới 60 giây
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-027]
sprint:
plan:
created: 2026-10-25
---

# T-040: KHE VÀ NGUỒN NATIVE MỘT QUẢNG CÁO: LẤY, HỦY, HẾT HẠN, LÀM MỚI 60 GIÂY

## Mô tả

Phần B của T-027, tách ngày 2026-10-25 (câu hỏi 1 của plan T-027, anh Nhật chọn theo đề xuất của AI). Mỗi lượt hiện native một quảng cáo sở hữu quảng cáo của nó qua một nguồn theo vị trí: lấy, hủy khi rời màn, hủy quảng cáo về muộn, hết hạn 55 phút, thay nội dung tại chỗ mỗi 60 giây khi ở foreground, tùy chọn yêu cầu của PARTNER (`docs/ops/ads-business-rules.md`, O-01 tới O-05, O-09, CL-02).

## Phạm vi

- Nguồn một quảng cáo theo vị trí: lấy, trả, thử lại khi hai chỗ cùng chờ; chỉ giữ quảng cáo chờ khi còn ít nhất một khe của vị trí trên màn (câu hỏi 12 của plan T-027).
- Hủy quảng cáo về muộn và trả chỗ đang chờ (O-03); hết hạn 55 phút; yêu cầu lỗi là lỗi mã 0 (O-09).
- Làm mới 60 giây ở foreground; nhịp rơi lúc ở nền thì bỏ, không dồn (CL-02, câu hỏi 14).
- Số liệu: lượt nạp tiếp cho nhịp làm mới ghi như lượt làm mới (`REFRESH`, không `load_attempt`); lượt đầu và thử lại ghi `SINGLE_AD` có `load_attempt` (câu hỏi 13).
- Mọi đường gửi đi qua một cổng gửi chung, mỗi đường có test ở nền và khi cổng đóng (L-072).

**Không bao gồm:**

- Composable, kích thước và ảnh chuẩn (T-027).
- Pool và nạp sẵn native (T-028).
- Thanh đáy và thu gọn (T-030).

## Rủi ro sơ bộ

Module core, quảng cáo, số liệu gửi đi. Phần nguồn và làm mới lấy từ bản plan T-027 trước khi tách (PR #167, các điểm rà soát chặn 1 tới 4 đã sửa).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
