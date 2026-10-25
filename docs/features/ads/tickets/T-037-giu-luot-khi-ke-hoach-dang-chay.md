---
id: T-037
title: Giữ lượt nạp sẵn tới khi kế hoạch đang chạy xong
feature: ads
spec: features/ads/spec.md
scenarios: [KB-24]
owner: Nhật
status: approved
risk: high
size: S
depends_on: [T-021]
sprint:
plan: features/ads/plans/T-037-giu-luot-khi-ke-hoach-dang-chay.md
created: 2026-10-25
---

# T-037: GIỮ LƯỢT NẠP SẴN TỚI KHI KẾ HOẠCH ĐANG CHẠY XONG

## Mô tả

Sau T-021, lượt xin nạp sẵn tới đúng lúc kế hoạch nạp sẵn hay lượt nạp bù native đang chạy (tối đa 6 giây mỗi nhóm) bị bỏ (`IGNORE_RUNNING`), không được giữ. Hệ quả: vị trí toàn màn hình bật từ xa giữa lượt chạy chưa được nạp sẵn tới lần xin sau; người dùng quay lại app rồi mạng về trong vòng 6 giây thì interstitial đã về `Idle` không nạp lại (KB-24, tiêu chí 3). Sau task này, lượt đó được giữ như lượt bị chặn vì cổng đóng: giữ tối đa một lượt, chạy đúng một lần khi lượt đang chạy xong, và khi chạy vẫn quyết lại theo bảng S-23.

Nguồn: rà soát độc lập T-021, mức nên sửa (`docs/runs/RUN-20261025-01.md`, mục Cần người kiểm tra); anh Nhật quyết làm ticket và plan mới.

## Phạm vi

- `decidePreload`: lượt xin nạp sẵn cả kế hoạch (`SDK_READY`, `GATE_REOPENED`, `NETWORK_BACK`, `PLACEMENT_ENABLED`) khi đang chạy ra `HOLD`; nạp bù native (`NATIVE_REFILL`) khi đang chạy vẫn bỏ (S-25).
- `AdPreloader`: lượt giữ chạy một lần khi lượt đang chạy xong (cả kế hoạch, kể cả khi lượt đang chạy chỉ là nạp bù native); lượt giữ mà lúc chạy quyết lại ra `IGNORE_*` thì bỏ, có log.
- Test cho mọi đường xin nạp sẵn và mọi kiểu lượt đang chạy (L-072).
- `docs/ops/ads-guide.md`, mục 6: câu về lượt giữ.

**Không bao gồm:**

- Đổi `AdsController`, kế hoạch nạp sẵn, luật thử lại hay nạp bù native (T-021 giữ nguyên).
- Khe native trong kế hoạch (T-027, T-028).

## Rủi ro sơ bộ

Module core và quảng cáo (điểm chặn 1 và 7): đổi lúc gửi yêu cầu nạp sẵn. Mỗi lượt giữ cộng tối đa một lượt chạy kế hoạch; khe đã có quảng cáo, đang nạp hay đang chờ thử lại không gửi thêm (F-04), nên số yêu cầu không tăng vọt.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
