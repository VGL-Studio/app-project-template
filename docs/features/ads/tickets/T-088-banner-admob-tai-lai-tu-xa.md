---
id: T-088
title: Banner cố định và MREC của AdMob tải lại theo Remote Config
feature: ads
spec: features/ads/spec.md
scenarios: [KB-34, KB-18, KB-29]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-087]
sprint:
plan:
auto_run: 
created: 2026-11-02
---

# T-088: BANNER CỐ ĐỊNH VÀ MREC CỦA ADMOB TẢI LẠI THEO REMOTE CONFIG

## Mô tả

Banner cố định và banner MREC của AdMob thôi dựa vào tự làm mới của đơn vị quảng cáo: `:core:ads` tải lại theo `time_reload_banner` và `time_reload_banner_mrec` (mặc định 60 giây), cùng cơ chế với banner thu gọn sau T-087 (KB-34, câu hỏi 52).

## Phạm vi

- `bannerRefreshOf` (`BannerLoads.kt`): `BANNER` và `BANNER_MREC` chuyển sang tải lại tay, nhịp đọc từ khóa của định dạng; mỗi lượt qua cổng đồng ý, chỉ ở foreground, không khi bị che (B-05, B-07), lượt tới hạn trong lúc bị che gửi một lần khi màn che đóng.
- Hai khóa mới trong `AdsSettings.kt`, `docs/remote-config-keys.md`; luật giá trị như T-087.
- Số liệu: lượt tải lại nay do `:core:ads` gửi nên có `latency_ms`, `load_attempt` như banner thu gọn; sửa mục 8 (Ghi số liệu) của `ads-business-rules.md` cho khớp.
- Sửa `ads-business-rules.md` (B-02, B-08, mục 5.7, 10, 11), `ads-guide.md`, và chỗ hướng dẫn tạo đơn vị quảng cáo (`ads-guide.md`, `ads-plan.md`): tắt tự làm mới trên đơn vị banner và MREC của AdMob.

**Không bao gồm:**

- Banner và MREC của MAX (T-089).
- Tạo hay sửa đơn vị quảng cáo production (người làm ở mỗi app, theo hướng dẫn phát hành).

## Rủi ro sơ bộ

Module core, quảng cáo, tần suất (điểm chặn 1 và 7). Nếu đơn vị production vẫn bật tự làm mới thì banner làm mới hai lần; hướng dẫn phát hành phải nói rõ. Đổi luật đã chốt B-02.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
