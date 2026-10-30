---
id: T-054
title: Quảng cáo trong màn hình dừng khi native toàn màn hình che
feature: ads
spec: features/ads/spec.md
scenarios: [KB-27]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-054: QUẢNG CÁO TRONG MÀN HÌNH DỪNG KHI NATIVE TOÀN MÀN HÌNH CHE

## Mô tả

Khi native toàn màn hình của `:core:ads` đang hiện (sau inter hay độc lập), banner và native làm mới ở màn bên dưới không gửi yêu cầu, không làm mới, không thử lại; quảng cáo đang có giữ nguyên (ADR-0036, điểm 1). JBase dừng tự làm mới khi có quảng cáo toàn màn hình; hôm nay ta chỉ dừng theo vòng đời Activity, mà native toàn màn hình là Dialog trong app nên Activity vẫn resumed.

## Phạm vi

- Đưa cờ "đang hiện quảng cáo toàn màn hình" (`fullScreenAdShowing`) vào điều kiện chạy của banner (`bannerShouldRun`), vòng làm mới native small và lớp thu gọn, và mọi lượt tải, thử lại của khe trong màn hình (câu hỏi 36).
- Một hàm thuần cho điều kiện "được gửi lúc này" mà mọi đường cùng gọi (`docs/CLAUDE.md`, mục Khi viết plan, L-072).
- Ghi luật mới vào `ads-business-rules.md` mục 5.5, 5.6, 5.7 và mục 12.

**Không bao gồm:**

- Trang native toàn trang ở bước của luồng (không phải lớp phủ, câu hỏi 36).
- Banner MREC (T-062 áp luật này).

## Rủi ro sơ bộ

Module core, quảng cáo, đổi hành vi so với PARTNER. Nhiều đường gửi yêu cầu (lượt đầu, làm mới, thử lại, nạp bù), mỗi đường một test.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
