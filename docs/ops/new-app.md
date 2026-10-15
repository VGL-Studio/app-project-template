# TẠO APP MỚI TỪ DỰ ÁN MẪU

Tài liệu này chỉ có trong repo dự án mẫu. Script khởi tạo không chép nó sang app mới (ADR-0017).

## 1. Chuẩn bị

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

1. Chép những file đã commit của dự án mẫu vào thư mục mới, bỏ `TEMPLATE-ROADMAP.md`, tài liệu này và chính script khởi tạo.
2. Đổi tên gói `com.vglstudio.template` thành tên gói mới trong code, file build, `google-services.json` giả của staging, `staging.yml` và tài liệu trong `docs/ops/`. Các ADR giữ nguyên văn.
3. Đặt tên app, tên project Gradle (lấy theo tên thư mục) và địa chỉ R2 staging.
4. Đổi đoạn mở đầu của `README.md` và `CLAUDE.md` cho app mới.
5. Tạo repo git với một commit đầu tiên ghi mã commit của dự án mẫu.

`:feature:sample` được giữ lại, vì `CLAUDE.md` hướng dẫn mọi feature làm theo đúng mẫu này. Khi feature thật đầu tiên thay màn hình bắt đầu, một ticket riêng xóa `:feature:sample`, cờ `SAMPLE` và vị trí quảng cáo `SAMPLE_BOTTOM_BANNER`, rồi trỏ `CLAUDE.md` sang feature thật làm mẫu.

## 3. Sau khi chạy

1. Chạy `scripts/check.sh` trong thư mục app mới.
2. Tạo repo private trong tổ chức trên GitHub, đẩy nhánh `main` lên.
3. Cài đặt staging và phiên tự chạy theo `docs/ops/staging-and-runs.md`.
4. Chuẩn bị phát hành production theo `docs/ops/release.md`.
