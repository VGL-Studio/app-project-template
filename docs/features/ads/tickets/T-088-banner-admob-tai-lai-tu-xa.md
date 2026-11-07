---
id: T-088
title: Banner cố định và MREC (AdMob và MAX) tải lại theo Remote Config
feature: ads
spec: features/ads/spec.md
scenarios: [KB-34, KB-18, KB-29, KB-31]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-087]
sprint:
plan: features/ads/plans/T-088-banner-admob-tai-lai-tu-xa.md
auto_run: 
created: 2026-11-02
---

# T-088: BANNER CỐ ĐỊNH VÀ MREC (ADMOB VÀ MAX) TẢI LẠI THEO REMOTE CONFIG

## Mô tả

Banner cố định và banner MREC thôi dựa vào tự làm mới của đơn vị AdMob và MAX: `:core:ads` tải lại cả khung hai nền tảng theo `time_reload_banner` và `time_reload_banner_mrec` (mặc định 60 giây), cùng cơ chế với banner thu gọn sau T-087. Ticket làm KB-34 tiêu chí 1 tới 6 cho hai lớp này, tiêu chí 5 trọn vẹn (câu hỏi 52).

## Phạm vi

- `BannerLoads.kt`: `BANNER` và `BANNER_MREC` tải lại tay, nhịp đọc từ khóa của định dạng, theo dõi được như T-087 (giá trị mới áp từ nhịp sau, 0 sang dương và dương sang 0 theo KB-34 tiêu chí 3); mỗi lượt qua cổng đồng ý, chỉ ở foreground, không khi bị che (B-05, B-07), lượt tới hạn trong lúc bị che gửi một lần khi màn che đóng; khóa là 0 thì vẫn thử lại sau lỗi tới khi có quảng cáo. Tách "ai làm mới" khỏi "yêu cầu thu gọn": hôm nay `bannerAdRequest(MANUAL)` (`BannerAdView.kt`) gắn `collapsible=bottom` và `collapsible_request_id`, banner cố định và MREC không được thành banner thu gọn; sửa chú thích ở `AdSlot.kt` theo.
- MAX (`MaxBannerAdView.kt`, `PlatformBanners.kt`, `BannerFrame.kt`): view MAX không tự làm mới, kể cả ở `resume()` (hôm nay gọi `startAutoRefresh()`); mỗi lượt tải lại đi theo `ads_mediation_order` như lượt đầu, giữ luật lỗi và thử lại của B-09 (kể cả phần của T-085). Làm cùng AdMob trong ticket này để không có lúc MAX làm mới hai lần.
- Hai khóa trong `AdsSettings.kt`, sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json`; `scripts/ads-plan.py` như T-087.
- Số liệu: lượt tải lại nay do `:core:ads` gửi nên có `latency_ms`, `load_attempt` như banner thu gọn; sửa mục 8 (Ghi số liệu) của `ads-business-rules.md` cho khớp.
- Sửa `ads-business-rules.md` (B-02, B-08, B-09, mục 5.7, 10, 11), `ads-guide.md`, `ads-plan.md`, và `docs/ops/to-app.md` (việc của người ở mỗi app khi đồng bộ: tắt tự làm mới trên đơn vị banner, MREC của AdMob và MAX, vì mã thử ở staging không cho thấy lỗi làm mới hai lần).

**Không bao gồm:**

- Banner thu gọn (T-087).
- Tạo hay sửa đơn vị quảng cáo production (người làm ở mỗi app).

## Rủi ro sơ bộ

Module core, quảng cáo, tần suất, SDK MAX (điểm chặn 1 và 7). Đổi luật đã chốt B-02, B-09. Khung hai nền tảng vừa dọn ở T-085, logic chờ và bỏ kết quả của lượt thử lại dễ vỡ khi thêm vòng tải lại tay. Đơn vị production còn bật tự làm mới thì banner làm mới hai lần.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
