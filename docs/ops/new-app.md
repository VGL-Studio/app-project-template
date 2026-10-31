# TẠO APP MỚI TỪ DỰ ÁN MẪU

Tài liệu này chỉ có trong repo dự án mẫu. Script khởi tạo không chép nó sang app mới (ADR-0017).

## 1. Chuẩn bị

App clone: làm bước khảo sát app mẫu trước (`docs/ops/clone-study.md`); tên và tên gói lấy từ bản đề xuất đã được duyệt.

- Tên gói của app, cũng là `applicationId`, ví dụ `com.vglstudio.notes`. Sau khi phát hành lên Google Play thì không đổi được.
- Tên app hiển thị cho người dùng.
- Một thư mục staging riêng của app trên R2, có địa chỉ công khai https.

## 2. Chạy script

Từ repo dự án mẫu, ở commit mới nhất của `main`:

```sh
scripts/new-app.sh --dest ../notes --package com.vglstudio.notes --name "Ghi chú" \
  --staging-assets-url https://<địa chỉ staging của app trên R2>/
```

Script làm những việc sau:

1. Chép những file đã commit của dự án mẫu vào thư mục mới, bỏ tài liệu này và chính script khởi tạo.
2. Đổi tên gói `com.vglstudio.template` thành tên gói mới trong code, file build, `google-services.json` giả của staging, `staging.yml` và tài liệu trong `docs/ops/`. Các ADR giữ nguyên văn.
3. Đặt tên app, tên project Gradle (lấy theo tên thư mục) và địa chỉ R2 staging.
4. Đổi đoạn mở đầu của `README.md` và `CLAUDE.md` cho app mới.
5. Bỏ lịch sử của dự án mẫu, vẫn đọc được ở repo mẫu: sổ bài học, báo cáo phiên trong `docs/runs/`, và ticket, plan, phản hồi, yêu cầu trong `docs/features/` (bỏ hẳn thư mục `quy-trinh`). Spec và thiết kế của `ads`, `loading`, `network` giữ lại, vì code của các module mẫu dẫn tới chúng. Mã ticket của app bắt đầu từ T-001, không nối tiếp mã của mẫu (L-141). Chỗ còn dẫn tới ticket hay plan của mẫu (ADR giữ nguyên văn, `docs/ops/ads-guide.md`) thì đọc ở repo mẫu.
6. Tạo repo git với một commit đầu tiên ghi mã commit của dự án mẫu.

`:feature:sample` được giữ lại, vì `CLAUDE.md` hướng dẫn mọi feature làm theo đúng mẫu này. Script không có tùy chọn xóa feature mẫu. Dev muốn xóa thì tự xóa, thường khi feature thật đầu tiên thay màn hình bắt đầu: xóa `:feature:sample`, cờ `SAMPLE`, vị trí quảng cáo `SAMPLE_NATIVE_SMALL` cùng các test dùng chúng, rồi trỏ `CLAUDE.md` sang feature thật làm mẫu.

## 3. Sau khi chạy

1. Chạy `scripts/check.sh` trong thư mục app mới.
2. Thương hiệu. App mới dùng thương hiệu của dự án mẫu cho tới khi thay. Trong `:core:designsystem`, thay:
   - Bảng màu sáng và tối trong `theme/Color.kt`.
   - Dấu trên logo `res/drawable/brand_mark.xml`.
   - Khẩu hiệu `brand_tagline` trong `res/values/strings.xml`.
   - Phông trong `res/font` và `theme/Type.kt`, nếu app dùng phông khác Inter. Phông mới cần giấy phép cho phép đóng gói vào app; đặt giấy phép vào `third_party/`.

   Sau đó sinh lại `docs/design-system/tokens.json` bằng `./gradlew :core:designsystem:testDebugUnitTest -PdesignTokens.update=true`, ghi lại ảnh chuẩn screenshot bằng `./gradlew recordRoborazziDebug testDebugUnitTest --rerun :app:recordRoborazziStagingDebug :app:testStagingDebugUnitTest --rerun` (L-114), xem lại ảnh catalog và các màn hình, rồi chạy lại `scripts/check.sh` (ADR-0008, ADR-0020).
3. Làm tiếp theo `docs/ops/setup-new-app.md` của app mới, từ phần 2.
