---
id: T-047
title: Rewarded: lượt quyết định treo không giữ mãi các lần bấm sau
feature: ads
spec: features/ads/spec.md
scenarios: [KB-19]
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-042]
sprint:
plan:
created: 2026-10-27
---

# T-047: REWARDED: LƯỢT QUYẾT ĐỊNH TREO KHÔNG GIỮ MÃI CÁC LẦN BẤM SAU

## Mô tả

Rà soát vòng 3 của T-042 (RUN-20261026-05) nêu hai chỗ của `RewardedTurns`:

1. **Lượt quyết định treo:** lần bấm đầu chờ ở `port.enabled()` hay lúc nạp mà không bao giờ xong, thì `decisions` còn giữ lượt đó. Mọi lần bấm sau của vị trí đều nhập vào lượt ấy và không nhận kết quả. Người dùng bấm "Xem quảng cáo" mà không có gì xảy ra cho tới khi mở lại app. `onStuck()` chỉ xóa `turns`, không xóa `decisions`.
2. **`held` khi SDK gọi đồng bộ:** khi SDK gọi `onShowed` rồi `onReward` ngay trong lần gọi `show`, `held` chỉ giữ kết quả cuối, nên có thể mất kết quả đầu.

Ticket kiểm hai trường hợp trên bằng test. Test nào đỏ thì sửa, để lần bấm sau một lượt treo vẫn có kết quả theo luật trong `docs/ops/ads-business-rules.md`. Chưa biết có lỗi thật hay không: plan phải chạy thử test trước khi đề xuất sửa.

## Phạm vi

- Test cho lượt quyết định treo ở `port.enabled()` và lúc nạp: lần bấm sau có kết quả (theo luật mục 8.2 và R-06) sau một thời gian chờ có giới hạn, hoặc khi `onStuck()` chạy.
- Test cho SDK gọi đồng bộ `onShowed`, `onReward` trong lần gọi `show`: hành động chạy đúng một lần.
- Sửa `RewardedTurns` nếu test đỏ. Thời gian chờ và việc `onStuck()` có xóa `decisions` hay không là câu hỏi của plan, nếu luật chưa nói.

**Không bao gồm:**

- Dọn test và chú thích của T-042 (T-046).

## Rủi ro sơ bộ

Đổi luồng rewarded trong module core quảng cáo (điểm chặn 1, luật nghiệp vụ quảng cáo): rủi ro cao. Có thể phải thêm hay làm rõ một luật trong `ads-business-rules.md`, việc này cần người duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
