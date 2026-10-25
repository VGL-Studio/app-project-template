---
id: T-029
title: Banner cố định và banner thu gọn theo PARTNER
feature: ads
spec: features/ads/spec.md
scenarios: [KB-18]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-021, T-023]
sprint:
plan: features/ads/plans/T-029-banner-theo-partner.md
created: 2026-10-23
---

# T-029: BANNER CỐ ĐỊNH VÀ BANNER THU GỌN THEO PARTNER

## Mô tả

Banner cố định dùng khe `banner` (tự làm mới trên AdMob; mã nguồn AdsKit gọi `bannerPartner`), tạm dừng và chạy lại theo Activity, thử lại khi lỗi chỉ ở foreground; banner thu gọn dùng khe `banner_collapsible`, nạp tay mỗi 30 giây, mỗi yêu cầu một mã mới (B-01 tới B-06 của `docs/ops/ads-business-rules.md`).

## Phạm vi

- Nạp lần đầu sau cổng đồng ý; mã rỗng thì không gửi yêu cầu.
- Thử lại dời tới lần Activity hiện lại; test không nạp ở nền (AR-03).
- Khe theo tên của bản đặc tả: `banner` tự làm mới, `banner_collapsible` nạp tay mỗi 30 giây với đơn vị không tự làm mới, mỗi lượt qua cổng đồng ý (B-02, B-03).
- Rút đồng ý giữa phiên thì giữ khung banner và chỗ chừa (B-06, câu hỏi 25). Vòng nạp lại tay chỉ ở foreground (B-05, AR-03).
- Test nghiệm thu gợi ý: `AT-BN-*` phần banner.

**Không bao gồm:**

- Thanh đáy (T-030).

## Rủi ro sơ bộ

Module core, quảng cáo. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
