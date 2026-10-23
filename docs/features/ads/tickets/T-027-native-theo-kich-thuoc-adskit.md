---
id: T-027
title: Native một quảng cáo theo kích thước AdsKit: khe đáy, đầy đủ, toàn màn hình
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: backlog
risk: high
size: L
depends_on: [T-010, T-013, T-021, T-023, T-035]
sprint:
plan:
created: 2026-10-23
---

# T-027: NATIVE MỘT QUẢNG CÁO THEO KÍCH THƯỚC ADSKIT: KHE ĐÁY, ĐẦY ĐỦ, TOÀN MÀN HÌNH

## Mô tả

Native small, full và toàn màn hình của `:core:ads` đổi sang kích thước của AdsKit (`docs/ops/ads-business-rules.md`, mục 5.7), màu theo token. Mỗi lượt hiện sở hữu một quảng cáo, hủy khi rời màn, hủy quảng cáo về muộn, hết hạn 55 phút, thay nội dung tại chỗ mỗi 60 giây khi ở foreground, tùy chọn yêu cầu của PARTNER (O-01 tới O-08).

## Phạm vi

- Composable native nhỏ 80 dp, full, toàn màn hình theo thiết kế vẽ lại (T-035).
- Khe một quảng cáo: lấy, hủy, thử lại khi hai chỗ cùng chờ.
- Ghi lại ảnh chuẩn screenshot của T-010 và T-013 theo plan.
- Khe đáy native nhỏ 80 dp cho cả vẽ và chừa chỗ, một hằng số (D-02, D-04, câu hỏi 22); khung chờ khi chờ lượt đầu, báo app có hay không có quảng cáo.
- Hủy quảng cáo về muộn và trả chỗ đang chờ (O-03); chuẩn bị yêu cầu lỗi là lỗi mã 0 (O-09); nhãn "Ad" góc trên trái, AdChoices góc trên phải, hàng sao (O-08).
- Test nghiệm thu gợi ý: `AT-NT-*`.

**Không bao gồm:**

- Pool (T-028).
- Thanh đáy và thu gọn (T-030).

## Rủi ro sơ bộ

Module core, quảng cáo; ghi lại ảnh chuẩn. Cỡ L: cân nhắc tách khi viết plan (composable; khe và làm mới). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
