---
id: T-084
title: Test của :core:ads vẽ trong theme cố định của test, ảnh chuẩn không đổi theo theme của app
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-143 (issue #321), không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run:
created: 2026-10-31
---

# T-084: TEST CỦA :CORE:ADS VẼ TRONG THEME CỐ ĐỊNH CỦA TEST

## Mô tả

Theo bài học L-143 (issue #321, từ Photune): test và ảnh chuẩn của `:core:ads` vẽ trong `AppTheme` của `:core:designsystem`. Mỗi app đặt màu và chữ của mình trong `:core:designsystem` (ADR-0008), còn `:core:ads` là phần của dự án mẫu mà app không sửa (`docs/ops/to-app.md`, mục 2; `TEMPLATE_OWNED` của `scripts/template-sync.sh`). App đổi theme thì 43 ảnh chuẩn của `:core:ads` không còn khớp, và mỗi đợt đồng bộ lấy ảnh của mẫu lại làm `scripts/check.sh` của app đỏ.

Ticket này cho test của `:core:ads` vẽ trong một theme cố định khai báo trong mã test của module, có cùng giá trị với theme hiện tại của dự án mẫu. Ảnh chuẩn của mẫu không đổi; ở app, ảnh của `:core:ads` không còn phụ thuộc theme của app.

## Phạm vi

- Một theme của test trong `core/ads/src/test/` (bảng màu sáng và tối, chữ, hình dạng), giá trị chép từ `AppTheme` hiện tại của mẫu. Mọi test của `:core:ads` đang gọi `AppTheme` (21 tệp, `grep -rlE 'AppTheme ?[({]' core/ads/src/test`) chuyển sang theme này.
- Test kiểm theme của test còn khớp `AppTheme` của dự án mẫu, chỉ chạy ở repo mẫu (như các test chỉ có ở mẫu của `scripts/tests/run.sh`), để mẫu đổi màu thì biết phải đổi theme của test.
- Hai chỗ còn phụ thuộc `:core:designsystem` mà plan phải quyết, kèm lý do:
  - `Spacing` được mã chính của `:core:ads` dùng (9 tệp). Theme của test không chặn được; app đổi `Spacing` thì ảnh vẫn đổi.
  - `PrimaryButton` được hai test của `NativeFlowStep` dùng. Có thể thay bằng nút dựng trong test.
- Phông chữ: chữ của `AppTheme` dùng phông trong `res/font` của `:core:designsystem`, mà app có thể thay. Plan quyết theme của test dùng phông nào (phông mặc định của Robolectric, hay một phông đặt trong `core/ads/src/test/resources`) và ghi số ảnh đổi nếu có.
- `docs/ops/to-app.md`, mục 2: ghi ảnh chuẩn của `:core:ads` không đổi theo theme của app.

**Không bao gồm:**

- Hàm `@Preview` trong mã chính của `:core:ads` (`NativeAdCard.kt`, `CollapsibleNativeAd.kt`, `NativeFullScreenAdScreen.kt`) vẫn dùng `AppTheme`, vì preview nên giống app.
- Ảnh chuẩn của `:app` có quảng cáo (`AdGalleryScreen_*`): vẽ theo theme của app là đúng.
- Module mẫu khác (`:feature:loading`, `:feature:sample`): app đổi theme thì ghi lại ảnh của chúng như `docs/ops/new-app.md` mục 3 đã nói.

## Rủi ro sơ bộ

Chạm test và ảnh chuẩn của `:core:ads` (module core, điểm chặn 7), nên cần plan rủi ro cao được duyệt chéo. Theme của test có cùng giá trị với theme hiện tại nên ảnh chuẩn của mẫu không đổi; ảnh nào đổi (phông, `Spacing`) thì plan liệt kê kèm mức lệch. Các app nhận qua `scripts/template-sync.sh`: ảnh của `:core:ads` trở về đúng bản của mẫu.

## Định nghĩa hoàn thành

- [ ] Không test nào của `:core:ads` gọi `AppTheme`; mọi test dùng theme của test.
- [ ] Ảnh chuẩn của `:core:ads` không đổi, hoặc plan liệt kê từng ảnh đổi và người duyệt đã xem.
- [ ] Đổi màu của `AppTheme` trong một bản thử (không commit) thì ảnh chuẩn của `:core:ads` vẫn khớp; ghi lệnh và kết quả vào nhật ký plan.
- [ ] Có test báo khi theme của test lệch `AppTheme` của dự án mẫu, chỉ chạy ở repo mẫu.
- [ ] `docs/ops/to-app.md` cập nhật.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
