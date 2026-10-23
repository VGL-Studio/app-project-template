---
id: T-007
title: Khóa Remote Config có kiểu, khai báo một chỗ, bảng khóa sinh tự động
feature: ads
spec: features/ads/spec.md
scenarios: [KB-10]
owner: Nhật
status: planned
risk: high
size: M
depends_on: []
sprint:
plan: features/ads/plans/T-007-khoa-remote-config-co-kieu.md
created: 2026-10-21
---

# T-007: KHÓA REMOTE CONFIG CÓ KIỂU, KHAI BÁO MỘT CHỖ, BẢNG KHÓA SINH TỰ ĐỘNG

## Mô tả

`:core:config` có kiểu khóa `RemoteKey` (boolean, long, double, string) gồm tên, giá trị mặc định và mô tả, và `RemoteSettings` đọc theo `RemoteKey`. Mọi khóa hiện có (`FeatureFlag`, khóa của `:core:ads` gồm khóa bật tắt từng vị trí) và 5 khóa mới của spec mục 8 được khai báo theo cách này. Một test sinh và kiểm `docs/remote-config-keys.md` cùng file mẫu nhập vào Firebase, giống cách `DesignTokensTest` giữ `tokens.json` (ADR-0023).

API dự kiến, plan chốt: `RemoteKey<T>` với các hàm tạo `RemoteKey.boolean/long/double/string(name, default, description)`; `RemoteSettings.value(key: RemoteKey<T>): Flow<T>`; danh sách mọi khóa của app lấy được từ một chỗ để test sinh bảng.

## Phạm vi

- Kiểu `RemoteKey`, đọc kiểu double và string từ Firebase.
- Chuyển khóa hiện có sang `RemoteKey`; khai báo 5 khóa mới của spec mục 8 (chưa dùng).
- Test sinh, kiểm `docs/remote-config-keys.md` và file mẫu Firebase; lệnh ghi lại khi thêm khóa.
- Cập nhật `docs/ops/remote-config.md` bỏ ghi chú đợt 2 của phần này.

**Không bao gồm:**

- Giá trị cục bộ cho staging (T-008).
- Dùng các khóa mới để đổi hành vi quảng cáo (T-009).

## Rủi ro sơ bộ

Module core `:core:config` và `:core:ads` (điểm chặn của risk-scoring.md). Không đổi giá trị mặc định nào (ADR-0014).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
