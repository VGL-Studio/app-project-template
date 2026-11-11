---
id: T-101
title: Feature hỏi được pool native_step đã có quảng cáo cho bước kế tiếp của luồng chưa
feature: ads
spec: features/ads/spec.md
scenarios: [KB-20]
owner: Nhật
status: backlog              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quảng cáo và :core:ads (điểm chặn 1, 7)
size: M
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-11
---

# T-101: FEATURE HỎI ĐƯỢC POOL NATIVE_STEP ĐÃ CÓ QUẢNG CÁO CHO BƯỚC KẾ TIẾP CỦA LUỒNG CHƯA

## Mô tả

Nguồn: issue #444 (camera-pose, PR #312 của app), bài học L-166. Luồng nhiều bước có thẻ `native_step` ở mọi bước; người dùng bấm "Tiếp" nhanh thì bước sau thường chưa có thẻ, vì pool nạp bù lần lượt. App muốn giữ nút "Tiếp" có giới hạn cho tới khi pool có quảng cáo cho bước kế tiếp, nhưng khung không cho hỏi điều này; `hasFreeAd` chưa đủ vì quảng cáo rảnh có thể là cái thẻ hiện tại sắp lấy.

## Phạm vi

- `:core:ads`: cách chỉ đọc (không nạp, không hủy quảng cáo) để biết pool cấp được ngay bao nhiêu quảng cáo (chưa ghi lượt hiển thị, chưa hết hạn, `canShowNow()`).
- Hàm công khai cho feature biết bước kế tiếp đã có quảng cáo: thẻ hiện tại chưa có quảng cáo thì cần ít nhất 2, có rồi thì ít nhất 1 (chừa một quảng cáo cho thẻ hiện tại). Bản `@Composable` đọc ở khung đầu của bước và đọc lại định kỳ; đã đúng thì giữ đúng cho bước đó. Plan đối chiếu bản của app (PR #312) và quy tắc của mẫu (P-01a, P-02, P-03, P-06).
- Luật mới ghi vào `docs/ops/ads-business-rules.md`, cách dùng vào `docs/ops/ads-guide.md`; plan xét có cần tiêu chí mới cho spec (KB-20) và đặt câu hỏi cho người phụ trách sản phẩm.
- Test JVM, có đột biến đối chứng cho luật chừa quảng cáo, đọc cờ mới nhất, `canShowNow`, vòng đọc lại.

**Không bao gồm:**

- Cơ chế giữ nút "Tiếp" của luồng (thuộc feature của app).

## Rủi ro sơ bộ

API công khai mới và đọc trạng thái pool của `:core:ads` (điểm chặn 7), liên quan cách hiện quảng cáo (điểm chặn 1): rủi ro cao, duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
