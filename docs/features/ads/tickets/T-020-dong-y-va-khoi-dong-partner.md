---
id: T-020
title: Đồng ý quảng cáo và khởi động SDK theo PARTNER
feature: ads
spec: features/ads/spec.md
scenarios: [KB-11, KB-12]
owner: Nhật
status: backlog
risk: high
size: L
depends_on: [T-019]
sprint:
plan: features/ads/plans/T-020-dong-y-va-khoi-dong-partner.md
created: 2026-10-23
---

# T-020: ĐỒNG Ý QUẢNG CÁO VÀ KHỞI ĐỘNG SDK THEO PARTNER

## Mô tả

Cổng đồng ý của `:core:ads` theo `docs/ops/ads-business-rules.md`, mục 1: khởi tạo sớm khi còn đồng ý của phiên trước (`CACHED_EARLY`), quyền tạm khi UMP lỗi mạng (`ERROR_ALLOWED`) chỉ ở foreground, hỏi lại khi mạng về hay app lên foreground, đóng cổng ngay khi UMP báo cần hỏi lại hay người dùng rút đồng ý, không hiện quảng cáo nạp bằng quyền tạm khi cổng đóng, công tắc `is_enable_consent_shortcut`.

## Phạm vi

- Máy trạng thái đồng ý là lớp thuần có unit test cho từng luật S-01 tới S-17 (UMP giả, đồng hồ giả).
- Cổng ở đầu mọi lượt nạp và mọi lượt hiện của `:core:ads`.
- Form UMP chặn quảng cáo khi quay lại app suốt lúc mở (S-16).
- Log mỗi đường đồng ý bằng `AppLogger`.
- Nhánh rút gọn cần cả cấu hình lúc build của `:app` và công tắc từ xa, mặc định cả hai tắt (S-12); thu hồi `ERROR_ALLOWED` không chốt, bật công tắc không thu hồi gì.
- Luật chốt theo hai nhánh (S-06): nhánh tắt thì form lỗi vẫn chốt khi UMP đã cho phép.
- Nút quyền riêng tư chỉ hiện khi UMP báo bắt buộc (S-14); form đồng ý chỉ giữ suất chặn khi `REQUIRED` (S-16); đặt lại đồng ý chỉ ở staging (S-18); tín hiệu khởi động đọc được trước khi khởi tạo (S-19).
- Việc của `:app`: khởi tạo trong `Application` (HD-01); đưa công tắc nhánh rút gọn vào trước bước hỏi đồng ý (HD-04).
- Test nghiệm thu gợi ý: `AT-CS-*`, `AT-KD-*` của bản đặc tả độc lập (`02-khoi-dong-va-consent.md`).

**Không bao gồm:**

- Kế hoạch nạp sẵn và nạp lại khi có mạng (T-021).
- App open lúc mở app (T-024).

## Rủi ro sơ bộ

Module core, quảng cáo và dữ liệu đồng ý (GDPR): điểm chặn. Sai luật có thể gửi yêu cầu quảng cáo khi chưa được phép; mỗi luật một test. Cỡ L: cân nhắc tách khi viết plan (khởi tạo sớm và quyền tạm; rút đồng ý). Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
