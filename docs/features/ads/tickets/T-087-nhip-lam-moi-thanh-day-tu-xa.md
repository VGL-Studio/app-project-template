---
id: T-087
title: Nhịp làm mới native ở đáy, native thu gọn, banner thu gọn theo Remote Config
feature: ads
spec: features/ads/spec.md
scenarios: [KB-34, KB-1, KB-4, KB-18]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: []
sprint:
plan:
auto_run: 
created: 2026-11-02
---

# T-087: NHỊP LÀM MỚI NATIVE Ở ĐÁY, NATIVE THU GỌN, BANNER THU GỌN THEO REMOTE CONFIG

## Mô tả

Ba lớp mà `:core:ads` đã tự thay quảng cáo đọc nhịp từ Remote Config thay số viết cứng: `time_reload_native_bottom` (mặc định 60 giây), `time_reload_collap_ad` (60), `time_reload_banner_collap` (30). Người vận hành đổi nhịp trên Firebase mà không phát hành bản mới (KB-34, câu hỏi 54).

## Phạm vi

- Ba khóa mới trong `AdsSettings.kt` theo `docs/ops/remote-config.md`; `time_reload_collap_ad` chuyển từ danh sách khóa AdsKit không hiệu lực sang khóa có hiệu lực. Luật giá trị: 0 là không thay, dương dưới 30 dùng 30, âm hay sai kiểu dùng mặc định. Thêm vào `docs/remote-config-keys.md` (sinh lại), giá trị staging nếu cần.
- Thay `NativeAdPolicy.REFRESH_INTERVAL_MILLIS` (`NativeAdSource.kt`, `NativeAdRefresh.kt`, `bottomSlotFirstRefreshDelayMillis` của `BottomAdBar.kt`) và `COLLAPSIBLE_BANNER_RELOAD_MILLIS` (`BannerLoads.kt`) bằng giá trị đọc theo dõi được (`Flow`): giá trị mới áp từ nhịp sau, nhịp đang đếm không đặt lại. Giữ trễ 500 ms của khe đáy sau lớp thu gọn.
- Giữ nguyên CL-02, CL-04, B-03, B-05, B-07, D-06. Câu khẳng định cũ về 60 hay 30 giây cố định viết lại theo ADR-0031 nếu cần.
- Sửa `ads-business-rules.md` (CL-02, B-03, bảng 5.7, mục 10, mục 11) và `ads-guide.md`.

**Không bao gồm:**

- Banner cố định và MREC (T-088, T-089).
- Nạp sẵn (T-086).

## Rủi ro sơ bộ

Module core, quảng cáo, tần suất (điểm chặn 1 và 7); thêm khóa Remote Config. Có thể cùng sửa `BottomAdBar.kt` hay `NativeAdSource.kt` với T-086: nếu hai plan cùng file thì xếp ở hai danh sách chạy hay thêm `depends_on` lúc soạn plan.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
