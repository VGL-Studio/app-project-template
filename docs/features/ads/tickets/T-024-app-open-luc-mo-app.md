---
id: T-024
title: Quảng cáo mở app ở màn loading (lần đầu sau khi cài và lần thường)
feature: ads
spec: features/ads/spec.md
scenarios: [KB-13]
owner: Nhật
status: stopped
risk: high
size: M
depends_on: [T-020, T-021, T-022, T-023]
sprint:
plan: features/ads/plans/T-024-app-open-luc-mo-app.md
created: 2026-10-23
---

# T-024: QUẢNG CÁO MỞ APP Ở MÀN LOADING (LẦN ĐẦU SAU KHI CÀI VÀ LẦN THƯỜNG)

## Mô tả

Màn loading chờ đồng ý, SDK và quảng cáo mở app với các trần của PARTNER (A-01 tới A-08 của `docs/ops/ads-business-rules.md`), hiện app open hoặc inter theo `open_ad_type`, chọn khóa lần đầu hay lần thường bằng dấu "đã mở lần đầu" lưu trên máy, ghi cơ hội `not_ready` khi tự bỏ.

## Phạm vi

- Khe app open trong `:core:ads`: hạn 1 giờ, khóa 1 giây sau khi đóng, nạp lại ngay.
- Hàm hiện có cổng của `:core:ads` cho vỏ app; vỏ app không gọi SDK.
- Màn loading (`:feature:loading`) và `AppNavHost` nối luồng; test Robolectric dựng vỏ app (L-051).
- Ghi chú vào ADR-0018 về thời gian chờ khi khóa bật (câu hỏi 12).
- Màn loading dò bằng "nạp nếu cần", lượt dò không đo (A-04, HD-06); chỉ bỏ đúng app open vừa chiếu (A-09); hàm ba tham số trả "đã hiện inter hay không" (A-02).
- Test nghiệm thu gợi ý: `AT-AOA-*`.

**Không bao gồm:**

- Quảng cáo khi quay lại app (T-025).

## Rủi ro sơ bộ

Module core, vỏ app, màn loading đã có plan và ảnh chuẩn: điểm chặn. Hành vi có rủi ro chính sách (ADR-0026). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
