---
id: T-038
title: Xóa lượt giữ khi lượt xin nạp sẵn mới được chạy
feature: ads
spec: features/ads/spec.md
scenarios: [KB-24]
owner: Nhật
status: on-staging
risk: high
size: S
depends_on: [T-037]
sprint:
plan: features/ads/plans/T-038-xoa-luot-giu-khi-nap-san-chay.md
created: 2026-10-25
---

# T-038: XÓA LƯỢT GIỮ KHI LƯỢT XIN NẠP SẴN MỚI ĐƯỢC CHẠY

## Mô tả

Khi đang có một lượt nạp sẵn bị giữ vì cổng đóng, cổng mở rồi một lượt xin mới (ví dụ `GATE_REOPENED`) tới `AdPreloader.request` trước collector cổng và ra `RUN`: kế hoạch chạy cả, nhưng lượt giữ không bị xóa, nên khi lượt đó xong thì `onGateChanged` chạy cả kế hoạch thêm một lần nữa. Lỗi có từ T-021, không thành vòng lặp (thêm đúng một lượt). Sau task này, lượt xin ra `RUN` thì lượt đang giữ được tính là đã chạy cùng lượt đó (S-23: "chạy đúng một lần"), không có lượt thừa.

Nguồn: rà soát độc lập T-037, mức ghi chú (1) (`docs/runs/RUN-20261025-02.md`, mục Cần người kiểm tra).

## Phạm vi

- `AdPreloader.request`: nhánh `RUN` xóa lượt giữ (`held = null`) trước khi chạy kế hoạch, có log.
- Unit test với thời gian ảo: lượt giữ vì cổng đóng, và lượt giữ vì đang chạy mà còn giữ do cổng đóng lúc chạy xong; test T-037 không lùi.
- `docs/ops/ads-guide.md`, mục 6: một câu và một dòng log.

**Không bao gồm:**

- Đổi `decidePreload`, `onGateChanged`, `launch`, `AdsController` (thứ tự collector cổng và `onGateOpened` giữ nguyên).
- Ghi chú (2) của cùng rà soát (`runner.run` ném lỗi thì `onGateChanged` cuối `launch` không chạy).

## Rủi ro sơ bộ

Module core và quảng cáo (điểm chặn 1 và 7): bớt một lượt chạy kế hoạch nạp sẵn thừa. Lượt `RUN` vừa chạy đã xin cả kế hoạch, sau lượt giữ; khe đã có quảng cáo, đang nạp hay đang chờ thử lại không gửi lại (F-04), nên bỏ lượt thừa không làm khe nào thiếu lượt nạp.

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
