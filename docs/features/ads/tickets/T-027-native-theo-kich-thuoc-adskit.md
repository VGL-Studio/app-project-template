---
id: T-027
title: Native một quảng cáo theo kích thước AdsKit: khe đáy, đầy đủ, toàn màn hình
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: [T-010, T-013, T-021, T-023, T-035]
sprint:
plan: features/ads/plans/T-027-native-theo-kich-thuoc-adskit.md
created: 2026-10-23
---

# T-027: NATIVE MỘT QUẢNG CÁO THEO KÍCH THƯỚC ADSKIT: KHE ĐÁY, ĐẦY ĐỦ, TOÀN MÀN HÌNH

## Mô tả

Native small, full và toàn màn hình của `:core:ads` đổi sang kích thước của AdsKit (`docs/ops/ads-business-rules.md`, mục 5.7), màu theo token. Mỗi lượt hiện sở hữu một quảng cáo, hủy khi rời màn, hủy quảng cáo về muộn, hết hạn 55 phút, thay nội dung tại chỗ mỗi 60 giây khi ở foreground, tùy chọn yêu cầu của PARTNER (O-01 tới O-08).

**Tách 2026-10-25** (câu hỏi 1 của plan, anh Nhật chọn theo đề xuất của AI): ticket này giữ phần A, giao diện và ảnh chuẩn (O-06 tới O-08). Phần B (khe và nguồn một quảng cáo, hủy, hết hạn, làm mới 60 giây; O-01 tới O-05) chuyển sang T-040.

## Phạm vi

- Composable native nhỏ 80 dp, full, toàn màn hình theo thiết kế vẽ lại (T-035).
- Ghi lại ảnh chuẩn screenshot của T-010 và T-013 theo plan.
- Khe đáy native nhỏ 80 dp cho cả vẽ và chừa chỗ, một hằng số (D-02, D-04, câu hỏi 22); khung chờ khi chờ lượt đầu, báo app có hay không có quảng cáo.
- Nhãn "Ad" góc trên trái, AdChoices góc trên phải, hàng sao (O-08).
- Test nghiệm thu gợi ý: `AT-NT-*`.

**Không bao gồm:**

- Khe và nguồn một quảng cáo, hủy quảng cáo về muộn, hết hạn 55 phút, làm mới 60 giây, lỗi mã 0 (T-040).
- Định dạng hay vị trí `native_full` (chỉ làm composable `NativeAdSize.FULL`, câu hỏi 2 của plan).
- Pool (T-028).
- Thanh đáy và thu gọn (T-030).

## Rủi ro sơ bộ

Module core, quảng cáo; ghi lại ảnh chuẩn. Cỡ M sau khi tách phần B sang T-040. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
