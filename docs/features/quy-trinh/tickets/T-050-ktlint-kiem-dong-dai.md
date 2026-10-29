---
id: T-050
title: ktlint kiểm dòng dài hơn 120 ký tự
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là bài học L-115, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: on-staging
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
- Sửa luôn hai chú thích lỗi thời của `decide` trong `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/RewardedTurns.kt`, ghi chú của rà soát T-048 (báo cáo RUN-20261028-02): chú thích nhánh `CONSENT_CLOSED` chỉ dẫn "Question 8 of the plan, option (a)" mà nhánh này nay cũng phục vụ đường hết giờ chờ khóa của T-048; chú thích nhánh `NO_AD, null` chưa nhắc ngoại lệ cổng đồng ý đóng.

**Không bao gồm:**

- Không thêm `@Suppress` hay lint baseline cho dòng dài.
- Không đổi luật ktlint khác.

## Rủi ro sơ bộ

Có thể sửa `.editorconfig`, cấu hình ktlint trong `build-logic/` hay `gradle/libs.versions.toml` (file "Không được làm" của `CLAUDE.md`, cần plan rủi ro cao được duyệt chéo), và chạm nhiều file của mọi module. Rủi ro cao.

## Định nghĩa hoàn thành

- [x] Ghi rõ vì sao ktlint hiện không báo dòng dài (mục Kết quả; không cần plan vì không chạm file "Không được làm").
- [x] Dòng Kotlin dài hơn 120 ký tự làm `scripts/check.sh` đỏ, có một kiểm tra chứng minh (hay quyết định không kiểm được ghi trong plan và `.editorconfig` khớp quyết định đó). Dòng code: ktlint báo (đã thử); dòng chỉ có chú thích: quyết định không kiểm, ghi trong `.editorconfig`.
- [x] Không còn dòng vượt giới hạn đã chốt (giới hạn chỉ áp cho code; không dòng code nào vượt).
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.

## Kết quả (2026-10-29)

**Vì sao ktlint không báo.** Plugin 13.0.0 dùng ktlint 1.5.0. Luật `standard:max-line-length` có bật và đọc `max_line_length = 120` của `.editorconfig`. Nhưng luật bỏ qua dòng chỉ có chú thích, và không có tùy chọn bật lại. Thử với `core/network` (dòng giả, đã bỏ sau khi thử):

| Dòng dài hơn 120 ký tự | ktlint báo |
| --- | --- |
| Dòng code | có |
| Code kèm chú thích cuối dòng | có |
| Chú thích `//` | không |
| KDoc `* ...` | không |
| Chú thích khối `/* ... */` | không |

**Số dòng.** Đếm theo ký tự ngày 2026-10-29 có 17 dòng dài hơn 120, đều là chú thích (15 ở `core/ads`, 1 ở `core/network`, 1 ở `core/analytics`). Con số 145 của mô tả đếm bằng `awk` của macOS, mà `awk` này đếm theo byte.

**Quyết định của anh Nhật.** Bỏ giới hạn cho chú thích. Không thêm kiểm riêng, không sửa các dòng chú thích dài. Giới hạn 120 chỉ áp cho dòng có code, và ktlint đã kiểm phần đó. Rà soát độc lập không nêu chú thích dài thành điểm rà soát.

**Đã đổi:**
- `.editorconfig`: ghi chú trên dòng `max_line_length`.
- `CLAUDE.md`: mục Quy ước code.
- `docs/auto-run.md`: bước 6, nội dung rà soát.
- `docs/lessons.md`: L-115 có thêm phần cập nhật.
- Hai chú thích lỗi thời của `decide` trong `RewardedTurns.kt`, ghi chú của rà soát T-048: nhánh `CONSENT_CLOSED` nay nói cả đường hết giờ chờ khóa khi cổng đồng ý đóng; nhánh `NO_AD, null` nói cổng đồng ý mở.
