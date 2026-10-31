---
id: T-081
title: Robolectric 4.17, không ảnh chuẩn nào đổi
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-127 (issue #276), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: approved
risk: high
size: S
depends_on: [T-080]
sprint:
plan: features/quy-trinh/plans/T-081-robolectric-anh-chuan.md
auto_run: false              # làm trong phiên có người theo dõi (quyết định 2026-10-29); người xem từng ảnh đổi
created: 2026-10-29
---

# T-081: ROBOLECTRIC 4.17, KHÔNG ẢNH CHUẨN NÀO ĐỔI

## Mô tả

Bước 3 trong 3 bước tách từ T-052 (L-127, issue #276). Bước này nâng Robolectric lên 4.17, bản Photune đang dùng. Roborazzi đã lên 1.77.0 ở T-080, vì bản 1.43 không chạy với DSL mới của AGP 9 (plan T-080, mục 8, câu 1), và 89 ảnh chuẩn vẫn khớp. Lần chạy thử của plan cho thấy Robolectric 4.17 không làm đổi ảnh chuẩn nào (plan mục 4.1), nên ticket không ghi lại ảnh. Ảnh `DesignSystem_spacing_components_light.png` lệch có sẵn từ trước, không do Robolectric, và để ngoài ticket này (plan mục 8, câu 2).

## Phạm vi

- `gradle/libs.versions.toml`: Robolectric 4.17.
- Ghi lại ảnh chuẩn bằng lệnh của `CLAUDE.md`, mục Lệnh, có `--rerun` (L-114). Mỗi ảnh đổi được liệt kê trong plan, kèm mức lệch (`scripts/design-compare.sh` hay so điểm ảnh).
- Test giao diện đo màu hay kích thước mà đỏ vì cách vẽ mới: tìm nguyên nhân. Chỉ sửa phần dựng test theo ADR-0033/L11, không nới câu khẳng định.
- `CLAUDE.md`, phần test (nếu quy tắc về `GraphicsMode`, `captureRoboImage` hay khung chụp đổi theo bản mới).

**Không bao gồm:**

- Đổi giao diện hay code app.
- Bộ build AGP 9 và Roborazzi 1.77.0 (T-080).

## Rủi ro sơ bộ

Ảnh chuẩn là bằng chứng của giao diện. Ghi lại hàng loạt có thể che một lỗi giao diện thật, nên cần plan rủi ro cao được duyệt chéo, và người xem từng ảnh đổi. Chạm `gradle/libs.versions.toml`. Roborazzi 1.77.0 giữ DSL và tên task `recordRoborazzi`, `verifyRoborazzi` mà `build-logic/` và `scripts/check.sh` dùng (đã chạy ở T-080). Các app nhận qua `template-sync.sh` phải ghi lại ảnh chuẩn của riêng chúng.

## Định nghĩa hoàn thành

- [ ] `scripts/check.sh` qua với Robolectric 4.17, không ảnh nào đổi, đo bằng lệnh của plan mục 4.
- [ ] Lần ghi ảnh bằng 4.15.1 và bằng 4.17 cho cùng danh sách ảnh đổi, giống nhau từng byte, đo bằng lệnh của plan mục 4.
- [ ] Không test nào bị nới câu khẳng định.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
