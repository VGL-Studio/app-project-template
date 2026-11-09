---
id: T-097
title: Plugin feature có activity-compose trên classpath biên dịch, để feature dùng được LocalActivity với AGP 9
feature: quy-trinh
spec:                        # ticket quy trình: nguồn là issue #369 (camera-pose), bài học L-161; không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # sửa build-logic/: plan rủi ro cao, duyệt chéo
size: S
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-09
---

# T-097: PLUGIN FEATURE CÓ ACTIVITY-COMPOSE TRÊN CLASSPATH BIÊN DỊCH, ĐỂ FEATURE DÙNG ĐƯỢC LOCALACTIVITY VỚI AGP 9

## Mô tả

Nguồn: issue #369 (camera-pose), bài học L-161. Với AGP 9, feature dùng `androidx.activity.compose.LocalActivity` (ví dụ để hỏi quyền) báo `Unresolved reference 'LocalActivity'`, vì `activity-compose` không còn trên classpath biên dịch của feature. Mẫu không thấy lỗi vì `:feature:sample` không dùng `LocalActivity`.

## Phạm vi

- Convention plugin `template.android.feature` trong `build-logic/` thêm `implementation(libs.androidx.activity.compose)`, như `:core:ads` đã khai báo.
- Test chứng minh: một feature mẫu biên dịch được với `LocalActivity` (plan chọn cách: chạy thử bằng `scripts/check.sh` với một chỗ dùng tạm, hay một test của `:feature:sample` dùng `LocalActivity`), có đối chứng đỏ khi bỏ dòng của plugin.
- Plan xét quyền và thư viện theo `docs/CLAUDE.md`, Khi viết plan: thư viện đã có trong `gradle/libs.versions.toml`; so manifest gộp trước và sau.

**Không bao gồm:**

- Thêm thư viện mới vào `gradle/libs.versions.toml`.

## Rủi ro sơ bộ

Sửa `build-logic/` (file cấm sửa ngoài plan rủi ro cao): rủi ro cao, duyệt chéo. Mọi feature của mọi app nhận thêm một phụ thuộc.

## Định nghĩa hoàn thành

- [ ] Mọi điểm của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
