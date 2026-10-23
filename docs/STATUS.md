# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Danh sách chạy `draft` chờ duyệt: [RUN-20261023-01.md](runs/RUN-20261023-01.md)
- Ticket đã duyệt, chưa vào danh sách chạy nào: [T-006-cuon-man-hinh-rat-thap.md](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md)
- Bài học `proposed` chờ retro: L-032 (Máy Xiaomi chặn thao tác giả lập qua adb nếu chưa bật "Gỡ lỗi USB (Cài đặt bảo mật)")
- Bài học `proposed` chờ retro: L-033 (Chạm theo tọa độ cũ trúng quảng cáo vừa hiện lên)
- Bài học `proposed` chờ retro: L-034 (Chạm liên tục trong vòng lặp mở màn hình mua của Play)
- Bài học `proposed` chờ retro: L-035 (Khảo sát khi máy không có mạng thì thiếu quảng cáo và nội dung)
- Bài học `proposed` chờ retro: L-036 (Báo cáo khảo sát app mẫu trích nguyên mã AdMob thật của app mẫu)
- Bài học `proposed` chờ retro: L-037 (Đường dẫn trong bản đề xuất gãy khi chép tài liệu khảo sát vào repo)
- Bài học `proposed` chờ retro: L-038 (App mới còn bài học của dự án mẫu trên trang tiến độ)
- Bài học `proposed` chờ retro: L-039 (Test của script hỏng trong app mới: test chỉ dành cho dự án mẫu, và test cần remote origin)
- Bài học `proposed` chờ retro: L-040 (Commit chỉ có tài liệu bỏ qua CI, nên test kiểm tài liệu không chặn được lỗi)
- Bài học `proposed` chờ retro: L-041 (Plan ghi sẵn phương án dự phòng đã duyệt, để phiên chuyển sang thay vì dừng task)
- Bài học `proposed` chờ retro: L-042 (Kỹ thuật lần đầu dùng phải chạy thử bằng `scripts/check.sh` trước khi duyệt plan)
- Bài học `proposed` chờ retro: L-043 (Phiên kiểm `main-broken` trước khi `main-guard` của lần merge vừa rồi chạy xong)
- Bài học `proposed` chờ retro: L-044 (Phiên mở bằng `claude --cloud` không được `resume-run.yml` tiếp tục khi hết hạn mức)
- Bài học `proposed` chờ retro: L-045 (Mâu thuẫn và test yếu chỉ được tìm thấy khi phiên đã chạy)
- Bài học `proposed` chờ retro: L-046 (Quyền và thư viện cần thêm chỉ lộ ra khi viết code, phiên phải dừng hoặc đi vòng)
- Bài học `proposed` chờ retro: L-047 (Chạy lại `main-guard` trên đúng commit bị đổ lỗi, xanh, nhưng issue `main-broken` không tự đóng)
- Bài học `proposed` chờ retro: L-048 (Một test chập chờn trên `main-guard` kết thúc cả phiên)
- Bài học `proposed` chờ retro: L-049 (Phiên không tự đưa nhánh chính về trạng thái xanh được, kể cả bằng một bản revert đúng)
- Bài học `proposed` chờ retro: L-050 (Quy tắc của `check.sh`, giới hạn mạng của phiên cloud và trạng thái ticket chỉ lộ ra khi phiên đã chạy)
- Bài học `proposed` chờ retro: L-051 (Điều hướng sau một hàm `suspend` chạy trên luồng nền, lỗi chỉ lộ ra ở `ui-test`)
- Bài học `proposed` chờ retro: L-052 (Giao bước 3 đến 5 cho phiên con, trong worktree riêng)
- Bài học `proposed` chờ retro: L-053 (Nút chữ trên nền khác màu tự đặt màu chữ, đè màu thiết kế)
- Bài học `proposed` chờ retro: L-054 (Hai plan cùng ghi Tạo một file mà task chạy trước tạo ra)
- Bài học `proposed` chờ retro: L-055 (Test kiểm giá trị mặc định qua ngay trên khung rỗng)
- Bài học `proposed` chờ retro: L-056 (Bài học thêm ngoài phiên trùng số với bài học mà phiên thêm cùng lúc)
- Bài học `proposed` chờ retro: L-057 (Trang tiến độ của app đếm 0 bài học vì sổ viết khác mẫu)
- Bài học `proposed` chờ retro: L-058 (Plan đổi cổng chất lượng bỏ sót một kiểm tra mà trường hợp mới phải đi qua)
- ADR `proposed` chờ quyết định: [0020-design-handoff-and-conformance.md](adr/0020-design-handoff-and-conformance.md)
- ADR `proposed` chờ quyết định: [0021-clone-reference-apps.md](adr/0021-clone-reference-apps.md)
- ADR `proposed` chờ quyết định: [0022-ad-formats-native-standard.md](adr/0022-ad-formats-native-standard.md)
- ADR `proposed` chờ quyết định: [0023-remote-config-conventions.md](adr/0023-remote-config-conventions.md)
- ADR `proposed` chờ quyết định: [0024-network-required.md](adr/0024-network-required.md)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Quảng cáo native và remote config cho dự án mẫu (đợt 2) ([2026-10-21-native-va-remote-config.md](features/ads/requests/2026-10-21-native-va-remote-config.md)) | 2026-10-21 | ads | `specified` đã có spec | 0/9 |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 5/6 |
| App luôn cần mạng ([2026-10-21-app-luon-can-mang.md](features/network/requests/2026-10-21-app-luon-can-mang.md)) | 2026-10-21 | network | `specified` đã có spec | 0/2 |

## 3. Feature

### ads

Spec: [spec.md](features/ads/spec.md) `confirmed`. Ticket: 9 `approved`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-007](features/ads/tickets/T-007-khoa-remote-config-co-kieu.md) | Khóa Remote Config có kiểu, khai báo một chỗ, bảng khóa sinh tự động | `approved` | `approved` (bản 1) | 25M | RUN-20261023-01 |
| [T-008](features/ads/tickets/T-008-gia-tri-cuc-bo-cho-staging.md) | Giá trị Remote Config cục bộ cho bản staging | `approved` | `approved` (bản 1) | 10M | RUN-20261023-01 |
| [T-009](features/ads/tickets/T-009-tan-suat-va-cong-tac-tong.md) | Công tắc tổng, ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên | `approved` | `approved` (bản 1) | 20M | RUN-20261023-01 |
| [T-010](features/ads/tickets/T-010-native-small-va-medium.md) | Native small và medium; màn hình mẫu dùng native small thay banner | `approved` | `approved` (bản 1) | 40M | RUN-20261023-01 |
| [T-011](features/ads/tickets/T-011-native-trong-danh-sach.md) | Native trong danh sách | `approved` | `approved` (bản 1) | 20M | RUN-20261023-01 |
| [T-012](features/ads/tickets/T-012-native-thu-gon.md) | Native thu gọn được | `approved` | `approved` (bản 1) | 20M | RUN-20261023-01 |
| [T-013](features/ads/tickets/T-013-native-toan-man-hinh.md) | Native toàn màn hình thay interstitial | `approved` | `approved` (bản 1) | 25M | RUN-20261023-01 |
| [T-014](features/ads/tickets/T-014-man-hinh-xem-thu-quang-cao.md) | Màn hình xem thử quảng cáo chỉ ở bản staging | `approved` | `approved` (bản 2) | 30M | RUN-20261023-01 |
| [T-015](features/ads/tickets/T-015-mediation-applovin.md) | Mediation AdMob và AppLovin | `approved` | `approved` (bản 2) | 10M | RUN-20261023-01 |

Phản hồi: chưa có.

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 5 `on-staging`, 1 `approved`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-001](features/loading/tickets/T-001-man-hinh-loading.md) | Màn hình loading có thanh tiến trình khi mở app | `on-staging` | `done` (bản 2) | - | - |
| [T-002](features/loading/tickets/T-002-thuong-hieu-xanh-luc.md) | Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo) | `on-staging` | `done` (bản 2) | - | - |
| [T-003](features/loading/tickets/T-003-giao-dien-loading-theo-thiet-ke.md) | Màn hình loading theo thiết kế Figma | `on-staging` | `done` (bản 1) | - | - |
| [T-004](features/loading/tickets/T-004-mau-con-thieu-va-gian-chu.md) | Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế | `on-staging` | `done` (bản 1) | - | - |
| [T-005](features/loading/tickets/T-005-man-hinh-thap.md) | Màn hình loading dùng được trên màn hình thấp | `on-staging` | `done` (bản 1) | - | - |
| [T-006](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md) | Test cuộn tới nút "Thử lại" trên màn hình rất thấp | `approved` | `approved` (bản 1) | - | - |

Phản hồi: 1 `fixed`, 1 `wontfix`.

### network

Spec: [spec.md](features/network/spec.md) `confirmed`. Ticket: 2 `approved`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-016](features/network/tickets/T-016-theo-doi-ket-noi.md) | Theo dõi có Internet dùng được, dùng chung cho cả app | `approved` | `approved` (bản 1) | 20M | RUN-20261023-01 |
| [T-017](features/network/tickets/T-017-popup-mat-mang.md) | Popup mất mạng chung và chặn vào app khi không có mạng | `approved` | `approved` (bản 2) | 20M | RUN-20261023-01 |

Phản hồi: chưa có.

## 4. Phiên tự chạy

| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |
| --- | --- | --- | --- | --- |
| [RUN-20261023-01](runs/RUN-20261023-01.md) | `draft` |  | T-007, T-008, T-009, T-015, T-010, T-016, T-011, T-012, T-013, T-017, T-014 | - |

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 27 bài; 27 `proposed`.
- ADR: 24; 19 `accepted`, 5 `proposed`.
