---
id: T-050
title: ktlint kiểm dòng dài hơn 120 ký tự
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-115, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
created: 2026-10-28
---

# T-050: KTLINT KIỂM DÒNG DÀI HƠN 120 KÝ TỰ

## Mô tả

`.editorconfig` đặt `max_line_length = 120` cho Kotlin, nhưng `ktlintCheck` trong `scripts/check.sh` không báo dòng dài. Ngày 2026-10-28, cả repo có 145 dòng Kotlin dài hơn 120 ký tự, 129 dòng trong đó là chú thích; riêng `:core:ads` có 13 dòng, dài nhất 201 ký tự (`InListAds.kt:135`). Rà soát độc lập của T-049 phải tự đếm và xếp một dòng KDoc 163 ký tự thành điểm nên sửa (L-115). Sau ticket này, dòng dài làm `scripts/check.sh` đỏ, hay nhóm ghi rõ vì sao không kiểm.

Cách đếm lại: `find . -name '*.kt' -not -path '*/build/*' -not -path './build-logic/convention/bin/*' -print0 | xargs -0 awk 'length > 120'`.

## Phạm vi

- Tìm vì sao luật `max-line-length` của ktlint (plugin `org.jlleitschuh.gradle.ktlint` 13.0.0, kiểu `android_studio`) không báo: luật bị tắt theo kiểu, bỏ qua chú thích, hay `.editorconfig` không được đọc. Ghi kết quả vào plan.
- Bật kiểm nếu được, và sửa các dòng đang vượt (xuống dòng chú thích, ngắt dòng code) trong cùng PR hay theo từng module.
- Không bật được hay chi phí lớn: đề xuất bỏ dòng `max_line_length` hay đặt số khác, kèm lý do.

**Không bao gồm:**

- Không thêm `@Suppress` hay lint baseline cho dòng dài.
- Không đổi luật ktlint khác.

## Rủi ro sơ bộ

Có thể sửa `.editorconfig`, cấu hình ktlint trong `build-logic/` hay `gradle/libs.versions.toml` (file "Không được làm" của `CLAUDE.md`, cần plan rủi ro cao được duyệt chéo), và chạm nhiều file của mọi module. Rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Plan ghi rõ vì sao ktlint hiện không báo dòng dài.
- [ ] Dòng Kotlin dài hơn 120 ký tự làm `scripts/check.sh` đỏ, có một kiểm tra chứng minh (hay quyết định không kiểm được ghi trong plan và `.editorconfig` khớp quyết định đó).
- [ ] Không còn dòng vượt giới hạn đã chốt.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
