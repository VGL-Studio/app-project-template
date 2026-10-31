---
id: T-082
title: Chữ mặc định của các module mẫu là tiếng Anh, tiếng Việt ở values-vi/
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-135 (issue #288); chữ của spec loading đổi theo, Nhật xác nhận
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-081]
sprint:
plan:
auto_run: false              # làm trong phiên có người theo dõi: người xem từng ảnh đổi, Nhật xác nhận chữ của spec loading
created: 2026-10-30
---

# T-082: CHỮ MẶC ĐỊNH CỦA CÁC MODULE MẪU LÀ TIẾNG ANH

## Mô tả

Theo bài học L-135 (issue #288, từ Photune): chữ trong `res/values/` của mọi module mẫu là tiếng Anh, bản tiếng Việt chuyển sang `res/values-vi/`. App giao diện tiếng Anh hay nhiều ngôn ngữ không phải ghi đè chuỗi của mẫu; app tiếng Việt vẫn hiện tiếng Việt trên máy để tiếng Việt. Ảnh chuẩn có các chuỗi này đổi theo, vì Robolectric chạy tiếng Anh.

## Phạm vi

- `feature/loading/src/main/res/values/strings.xml`: 7 chuỗi tiếng Việt thành tiếng Anh (`loading_percent` không theo ngôn ngữ, giữ nguyên); bản tiếng Việt ở `feature/loading/src/main/res/values-vi/strings.xml`.
- `core/designsystem/src/main/res/values/strings.xml`: `brand_tagline` thành tiếng Anh; bản tiếng Việt ở `values-vi/` của module.
- `app/src/main/res/values/strings.xml`: 4 chuỗi của popup mất mạng lấy bản tiếng Anh đang có ở `app/src/main/res/values-en/strings.xml`, rồi bỏ `values-en/`; bản tiếng Việt ở `values-vi/`.
- Test có chữ tiếng Việt viết cứng: `core/designsystem/src/test/kotlin/com/vglstudio/template/core/designsystem/NetworkDialogTest.kt`, `app/src/test/kotlin/com/vglstudio/template/network/NetworkDialogHostTest.kt`. Plan nói rõ câu khẳng định được viết lại (ADR-0031) hay test đặt ngôn ngữ tiếng Việt.
- Ảnh chuẩn có các chuỗi trên, khoảng 12: `LoadingScreen_*` (6), `NetworkLostScreen_*` (4), `DesignSystem_brand_*` (2). Plan liệt kê từng ảnh kèm mức lệch; người duyệt xem từng ảnh.
- Spec `docs/features/loading/spec.md` (tiêu chí có nhãn "Đang tải dữ liệu") và bảng chữ của thiết kế `loading`, `network` (`design/context/`): ghi chữ theo hai ngôn ngữ. Nhật xác nhận chữ của spec `loading`.
- `CLAUDE.md`, mục Quy ước code: chữ trong `res/values/` là tiếng Anh, bản tiếng Việt ở `res/values-vi/`.
- Chú thích "English in every module" của `SCENARIO_LANGUAGE` trong `scripts/device-scenario.py` và câu tương ứng ở `docs/ops/test-sessions.md`: kiểm lại cho đúng sau khi đổi.

**Không bao gồm:**

- Chuỗi đã là tiếng Anh: `:core:ads`, `:feature:sample` và bản staging (`app/src/staging/res/values/`).
- Đổi giao diện, bố cục, hay thêm ngôn ngữ thứ ba.
- Robolectric 4.17 (T-081).

## Rủi ro sơ bộ

Chạm `:core:designsystem` (module core, điểm chặn 7), ảnh chuẩn, câu khẳng định của test cũ và chữ của một spec đã `confirmed`, nên cần plan rủi ro cao được duyệt chéo. Làm sau T-081, để mỗi lần ảnh chuẩn đổi chỉ có một nguyên nhân. Các app nhận qua `scripts/template-sync.sh` sẽ có ảnh chuẩn của module mẫu đổi; app có ghi đè chuỗi phải kiểm lại. Câu hỏi để plan quyết: test so với thiết kế chạy tiếng Việt hay tiếng Anh; thiết kế có thêm chữ tiếng Anh hay giữ chữ tiếng Việt.

## Định nghĩa hoàn thành

- [ ] `res/values/` của mọi module mẫu chỉ có chữ tiếng Anh, hay chữ không theo ngôn ngữ như `%1$d%%`; `res/values-vi/` có bản tiếng Việt của mọi chuỗi đó; `:app` không còn `values-en/`.
- [ ] Trên máy để tiếng Việt, các màn hình của module mẫu hiện tiếng Việt như trước (test đặt ngôn ngữ `vi`).
- [ ] `scripts/check.sh` qua với ảnh chuẩn mới; danh sách ảnh đổi và mức lệch có trong plan, người duyệt đã xem từng ảnh.
- [ ] Không test nào bị nới câu khẳng định; câu viết lại đi theo ADR-0031.
- [ ] Spec `loading` cập nhật, Nhật đã xác nhận chữ.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
