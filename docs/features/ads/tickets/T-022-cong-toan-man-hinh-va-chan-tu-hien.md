---
id: T-022
title: Cổng quảng cáo toàn màn hình, mốc ads_interval, hồi phục kẹt, chặn quảng cáo tự hiện
feature: ads
spec: features/ads/spec.md
scenarios: [KB-6, KB-15, KB-23]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: [T-019, T-009, T-020]
sprint:
plan: features/ads/plans/T-022-cong-toan-man-hinh-va-chan-tu-hien.md
created: 2026-10-23
---

# T-022: CỔNG QUẢNG CÁO TOÀN MÀN HÌNH, MỐC ADS_INTERVAL, HỒI PHỤC KẸT, CHẶN QUẢNG CÁO TỰ HIỆN

## Mô tả

Một cổng chung cho quảng cáo toàn màn hình theo `docs/ops/ads-business-rules.md`, mục 3, 4 và 6: không chồng nhau, cờ đang hiện có trần 10 phút, hồi phục kẹt khi Activity bị che rồi hiện lại, mốc `ads_interval` chỉ do inter đẩy, sống trong process, đo bằng đồng hồ đơn điệu. Bộ đếm chặn quảng cáo tự hiện và hợp đồng với `MainActivity` (G-01 tới G-05).

## Phạm vi

- Thay bộ tần suất của T-009 bằng mốc `ads_interval` theo I-01 tới I-05; test đổi giờ hệ thống không ảnh hưởng (AR-01).
- Hồi phục kẹt: test thứ tự che rồi hiện lại (F-03).
- Bộ đếm chặn và ba cách dùng; `MainActivity` và giao diện gốc nối theo G-04, có test Robolectric dựng vỏ app (L-051).
- Nhóm giãn cách (MC-04): nhóm mặc định dùng `ads_interval`, nhóm riêng dùng `ads_interval_<nhóm>` và mốc riêng; `ignoreInterval`, `recordInterval` là thuộc tính của vị trí, không phải tham số của từng lượt gọi; quan hệ giữa các nhóm theo câu hỏi 30. Test: inter của nhóm riêng không dời mốc chung.
- Hoàn tất cưỡng bức khi Activity giao cho inter bị hủy (N-04, HD-03); tín hiệu "đã rời màn" bị tiêu sau lần dùng đầu (F-03); gỡ cờ kẹt chạy trước hỏi lại đồng ý (G-06).
- "Đã sẵn sàng chưa" không nạp, "nạp nếu cần" tôn trọng lịch thử lại (F-04).
- Test nghiệm thu gợi ý: `AT-FS-*`, `AT-INT-*`, `AT-SP-*`, `AT-HD-*`.

**Không bao gồm:**

- App open và resume (T-024, T-025).
- Combo (T-026).

## Rủi ro sơ bộ

Module core và vỏ app `:app`: điểm chặn. Sai hợp đồng chặn làm resume tắt tới hết process (G-04). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
