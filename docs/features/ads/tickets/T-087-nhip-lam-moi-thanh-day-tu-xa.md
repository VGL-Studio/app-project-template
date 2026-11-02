---
id: T-087
title: Nhịp làm mới native nhỏ, native thu gọn, banner thu gọn theo Remote Config
feature: ads
spec: features/ads/spec.md
scenarios: [KB-34, KB-1, KB-4, KB-18]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-086]
sprint:
plan:
auto_run: 
created: 2026-11-02
---

# T-087: NHỊP LÀM MỚI NATIVE NHỎ, NATIVE THU GỌN, BANNER THU GỌN THEO REMOTE CONFIG

## Mô tả

Ba lớp mà `:core:ads` đã tự thay quảng cáo đọc nhịp từ Remote Config thay số viết cứng: `time_reload_native_small` (mặc định 60 giây), `time_reload_collap_ad` (60), `time_reload_banner_collap` (30). Người vận hành đổi nhịp trên Firebase mà không phát hành bản mới. Ticket làm KB-34 tiêu chí 1 tới 4 và 6 cho ba lớp này (tiêu chí 4 chỉ phần banner thu gọn); tiêu chí 5 và phần banner cố định, MREC của các tiêu chí khác thuộc T-088 (câu hỏi 54).

## Phạm vi

- Ba khóa trong `AdsSettings.kt` theo `docs/ops/remote-config.md`; `time_reload_collap_ad` chuyển từ danh sách khóa AdsKit không khai báo sang khóa có hiệu lực. Câu khẳng định của `AdsSettingsTest.keysAdsKitDropped_areNotDeclared` về khóa này viết lại theo ADR-0031. Luật giá trị: 0 là không thay, dương dưới 30 dùng 30, âm hay sai kiểu dùng mặc định.
- Thay `NativeAdPolicy.REFRESH_INTERVAL_MILLIS` (`NativeAdSource.kt`, `NativeAdSources.kt` (`singleAdSlotStates`), `NativeAdRefresh.kt`, `CollapsibleNativeAd.kt`, `bottomSlotFirstRefreshDelayMillis` của `BottomAdBar.kt`, `firstRefreshDelayMillis` ở `AdsController.kt`) và `COLLAPSIBLE_BANNER_RELOAD_MILLIS` (`BannerLoads.kt`) bằng giá trị theo dõi được (`Flow`): giá trị mới áp từ nhịp sau, nhịp đang đếm không đặt lại; đổi từ 0 sang dương thì nhịp đầu tính từ lúc nhận giá trị, đổi từ dương sang 0 thì bỏ lần đang đếm. Hằng hôm nay dùng chung cho native nhỏ và lớp thu gọn: tách thành hai nguồn giá trị, mỗi lớp đọc khóa của nó. Khóa native nhỏ áp cho mọi khe native nhỏ một quảng cáo, cả khe đứng riêng. Giữ trễ 500 ms của khe đáy sau lớp thu gọn.
- Giữ nguyên CL-02, CL-04, B-03, B-05, B-07, D-06. Câu khẳng định cũ về 60 hay 30 giây cố định viết lại theo ADR-0031 nếu cần.
- Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json`; giá trị staging nếu cần.
- `scripts/ads-plan.py`: ba khóa vào đúng nhóm (thanh đáy, banner), bỏ chữ "không còn hiệu lực" của `time_reload_collap_ad` và các dòng ghi 60, 30 giây là hằng số; test của script.
- Sửa `ads-business-rules.md` (CL-02, B-03, bảng 5.7, mục 10 và 10.1, mục 11), `ads-guide.md`, `docs/ops/ads-plan.md` (mục Hằng số, câu "Không đổi được qua Remote Config" về nhịp làm mới). Ghi cảnh báo: app cũ đã đặt `time_reload_collap_ad` theo AdsKit thì khóa nay có hiệu lực.

**Không bao gồm:**

- Banner cố định và MREC (T-088).
- Kho nạp sẵn (T-086).

## Rủi ro sơ bộ

Module core, quảng cáo, tần suất (điểm chặn 1 và 7); thêm khóa Remote Config. Phụ thuộc T-086 vì cùng sửa `AdsController.kt`, `BottomAdBar.kt`, `NativeAdSource.kt` và tài liệu quảng cáo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
