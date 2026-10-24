---
id: T-021
title: Nạp sẵn theo nhóm, chờ SDK, thử lại và nạp lại khi có mạng
feature: ads
spec: features/ads/spec.md
scenarios: [KB-24]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-020, T-016, T-022, T-023]
sprint:
plan: features/ads/plans/T-021-nap-san-va-nap-lai.md
created: 2026-10-23
---

# T-021: NẠP SẴN THEO NHÓM, CHỜ SDK, THỬ LẠI VÀ NẠP LẠI KHI CÓ MẠNG

## Mô tả

`:core:ads` nạp sẵn theo kế hoạch nhóm (S-20 tới S-25), chờ SDK sẵn sàng tối đa 35 giây, thử lại sau lỗi theo PARTNER (F-05, F-06, O-05) và nạp lại khi mạng về, dùng theo dõi kết nối chung của T-016.

## Phạm vi

- Kế hoạch nạp sẵn: lớp thuần, test thứ tự nhóm và trần 6 giây với đồng hồ giả.
- Luật thử lại toàn màn hình và native là hàm thuần có test biên.
- Nạp lại khi mạng về (gộp 3 giây), đặt lại thời gian chờ của native; nạp bù native khi Activity chính hiện lại (S-25).
- Hỏi cổng ngay trước khi gửi; lượt bị chặn lúc gửi không tính là lỗi, không hẹn thử lại (S-15); hai ngoại lệ không giữ lượt nạp sẵn và lệnh nạp sẵn khi quyền là "không" (S-23); chuẩn bị yêu cầu lỗi là lỗi mã 0 (O-09).
- Kế hoạch nạp sẵn mặc định suy ra từ danh sách vị trí, `:app` đặt lại trong `Application` trước khi đồng ý có kết quả (HD-02).
- Test nghiệm thu gợi ý: `AT-KD-*`, phần thử lại của `AT-FS-*` và `AT-NT-*`.

**Không bao gồm:**

- Từng định dạng dùng luật này (ticket định dạng).

## Rủi ro sơ bộ

Module core, quảng cáo: điểm chặn. Thử lại sai có thể gửi quá nhiều yêu cầu và bị SDK tự chặn. Plan viết sau khi đợt 2 (RUN-20261023-01) merge, chạy thử trên code thật (docs/CLAUDE.md, mục Khi viết plan).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
