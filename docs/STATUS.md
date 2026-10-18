# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Yêu cầu chưa có spec: [2026-10-17-log-co-cau-truc.md](inbox/2026-10-17-log-co-cau-truc.md) (Log có cấu trúc và ghi hành trình, để AI phân tích phiên thử)
- Ticket đã duyệt, chưa vào danh sách chạy nào: [T-006-cuon-man-hinh-rat-thap.md](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md)
- ADR `proposed` chờ quyết định: [0020-design-handoff-and-conformance.md](adr/0020-design-handoff-and-conformance.md)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 5/6 |
| Log có cấu trúc và ghi hành trình, để AI phân tích phiên thử ([2026-10-17-log-co-cau-truc.md](inbox/2026-10-17-log-co-cau-truc.md)) | 2026-10-17 | - | `new` chưa có spec | - |

## 3. Feature

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 5 `on-staging`, 1 `approved`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-001](features/loading/tickets/T-001-man-hinh-loading.md) | Màn hình loading có thanh tiến trình khi mở app | `on-staging` | `done` (bản 2) | - | RUN-20261015-01, RUN-20261015-02, RUN-20261015-03 |
| [T-002](features/loading/tickets/T-002-thuong-hieu-xanh-luc.md) | Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo) | `on-staging` | `done` (bản 2) | - | RUN-20261016-01, RUN-20261016-02, RUN-20261017-01 |
| [T-003](features/loading/tickets/T-003-giao-dien-loading-theo-thiet-ke.md) | Màn hình loading theo thiết kế Figma | `on-staging` | `done` (bản 1) | - | RUN-20261016-01, RUN-20261016-02, RUN-20261017-01 |
| [T-004](features/loading/tickets/T-004-mau-con-thieu-va-gian-chu.md) | Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế | `on-staging` | `done` (bản 1) | - | RUN-20261017-02 |
| [T-005](features/loading/tickets/T-005-man-hinh-thap.md) | Màn hình loading dùng được trên màn hình thấp | `on-staging` | `done` (bản 1) | - | RUN-20261017-03 |
| [T-006](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md) | Test cuộn tới nút "Thử lại" trên màn hình rất thấp | `approved` | `approved` (bản 1) | - | - |

Phản hồi: 1 `fixed`, 1 `wontfix`.

## 4. Phiên tự chạy

| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |
| --- | --- | --- | --- | --- |
| [RUN-20261017-03](runs/RUN-20261017-03.md) | `finished` | 2026-10-17 16:19 UTC | T-005 | 0.1.0-staging.5 |
| [RUN-20261017-02](runs/RUN-20261017-02.md) | `finished` | 2026-10-17 08:13 UTC | T-004 | 0.1.0-staging.4 |
| [RUN-20261017-01](runs/RUN-20261017-01.md) | `finished` | 2026-10-17 03:21 UTC | T-002, T-003 | 0.1.0-staging.3 |
| [RUN-20261016-02](runs/RUN-20261016-02.md) | `finished` | 2026-10-16 16:29 UTC | T-002, T-003 | - |
| [RUN-20261016-01](runs/RUN-20261016-01.md) | `finished` | 2026-10-16 15:29 UTC | T-002, T-003 | - |
| [RUN-20261015-03](runs/RUN-20261015-03.md) | `finished` | 2026-10-15 15:23 UTC | T-001 | 0.1.0-staging.2 (versionCode 2), staging.yml run 38765200550 |
| [RUN-20261015-02](runs/RUN-20261015-02.md) | `finished` | 2026-10-15 (phiên tự chạy) | T-001 | - |
| [RUN-20261015-01](runs/RUN-20261015-01.md) | `finished` | 2026-10-15 | T-001 | không có (không task nào được merge) |

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 31 bài; 16 `verified`, 14 `applied`, 1 `accepted`.
- ADR: 20; 19 `accepted`, 1 `proposed`.
