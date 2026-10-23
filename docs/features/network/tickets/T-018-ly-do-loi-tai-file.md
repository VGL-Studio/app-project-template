---
id: T-018
title: Lý do tải file R2 thất bại (AssetFailureReason)
feature: network
spec: features/network/spec.md
scenarios: [KB-3]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: []
sprint:
plan: features/network/plans/T-018-ly-do-loi-tai-file.md
created: 2026-10-23
---

# T-018: LÝ DO TẢI FILE R2 THẤT BẠI (ASSETFAILUREREASON)

## Mô tả

`AssetResult.Unavailable` của `:core:assets` cho biết vì sao không tải được file: không tới được máy chủ (`NETWORK`), máy chủ trả 404 (`NOT_FOUND`), hay lỗi khác (`OTHER`). Nhờ vậy popup mất mạng (T-017) chỉ phản ứng với lỗi mạng, còn lỗi máy chủ để màn hình của feature tự báo và cho thử lại (spec network KB-3). Log của lần tải lỗi ghi lý do và tên lớp lỗi. Lấy từ app camera-pose (issue #73, PR VGL-Studio/camera-pose#12).

## Phạm vi

- Enum `AssetFailureReason`, `HttpStatusException`, `AssetResult.Unavailable(reason)`, suy lý do từ ngoại lệ và nguyên nhân của nó.
- Dòng log khi tải lỗi ghi lý do và tên lớp lỗi (Android bỏ stack trace của `UnknownHostException`).
- Sửa test hiện có dùng `AssetResult.Unavailable` (`:core:assets`, `:feature:loading`), test mới cho từng lý do.

**Không bao gồm:**

- Báo lỗi mạng cho popup (`NetworkFailureReporter`, T-017).
- Đổi cách màn hình loading báo lỗi (spec loading giữ nguyên).

## Rủi ro sơ bộ

Đổi API công khai của module core `:core:assets` (`Unavailable` từ `data object` thành `data class`): điểm chặn module core dùng chung. App đã tạo từ dự án mẫu phải sửa chỗ dùng `AssetResult.Unavailable` khi kéo thay đổi về.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
