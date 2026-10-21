---
date: 2026-10-21
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: QUẢNG CÁO NATIVE VÀ REMOTE CONFIG CHO DỰ ÁN MẪU (ĐỢT 2)

Ghi lại từ trao đổi với AI ngày 2026-10-21, sau khi merge PR #67 (ADR-0022, ADR-0023, `docs/ops/ads-guide.md`, `docs/ops/remote-config.md`).

## Mình muốn gì

Dự án mẫu có sẵn những gì ADR-0022 và ADR-0023 đã quyết, để mọi app tạo từ dự án mẫu dùng được ngay:

- Native small (thay banner), native medium (thay MREC), native trong danh sách, native thu gọn được, native toàn màn hình (thay interstitial). Kích thước tính bằng dp theo thiết kế của nhóm; file Excel "Quy chuẩn Native" chỉ tham khảo.
- Mediation AdMob và AppLovin.
- Giới hạn tần suất lưu qua các phiên, ân hạn phiên đầu, công tắc tổng tắt mọi quảng cáo.
- Remote Config: thêm kiểu số thực và chuỗi, bảng khóa sinh tự động, bản staging có giá trị cục bộ để thử quảng cáo.
- Sự kiện hiển thị quảng cáo lấy từ liên kết AdMob và Firebase, app không tự ghi.

## Vì sao

App đầu tiên theo quy trình clone (Camera Pose: Lines and Guides) cần native và mức quảng cáo vừa. Làm ở dự án mẫu trước để các app sau không phải làm lại, và để `:core:ads` của các app không lệch nhau.

## Những gì chưa cần làm lần này

App open, rewarded.
