---
id: T-058
title: Máy thử và log từng mạng ở bản staging
feature: ads
spec: features/ads/spec.md
scenarios: [KB-28]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-054]
sprint:
plan: features/ads/plans/T-058-may-thu-va-log-tung-mang.md
auto_run: 
created: 2026-10-29
---

# T-058: MÁY THỬ VÀ LOG TỪNG MẠNG Ở BẢN STAGING

## Mô tả

Dev khai báo máy thật của mình là máy thử trên máy dev, không vào repo; bản staging ghi log từng mạng đã thử của mỗi lượt tải. JBase ghi waterfall của MAX ở mỗi lượt doanh thu; hôm nay ta không có `RequestConfiguration` nào nên không kiểm được mediation trên máy thật (câu hỏi 40).

## Phạm vi

- Đọc `ads.testDeviceIds` từ `local.properties` chỉ ở biến thể staging, đặt `RequestConfiguration.setTestDeviceIds` trước khi khởi tạo SDK.
- Log thẻ `Ads` cho từng mạng trong `ResponseInfo.adapterResponses` (tên lớp adapter, thời gian, mã lỗi) ở staging; không ghi mã đơn vị, mã máy.
- Hướng dẫn ở `docs/ops/ads-guide.md` (mục thử trên máy thật).

**Không bao gồm:**

- Máy thử cho MAX (KB-31).

## Rủi ro sơ bộ

Module core và cấu hình build của `:app` (điểm chặn 7). Mã máy là định danh: không vào git, không vào log.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
