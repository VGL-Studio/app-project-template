# TIẾN ĐỘ DỰ ÁN

<!-- Sinh bởi scripts/status.py từ trạng thái trong các tài liệu. Không sửa tay: sửa tài liệu gốc rồi chạy lại script (docs/CLAUDE.md, mục Trang tiến độ). -->

## 1. Đang chờ người

- Bài học `proposed` chờ retro: L-169 (Container khởi động lại giữa lúc phiên con làm bước 3 đến 5: gửi tin tiếp tục cho chính phiên con giữ được ngữ cảnh)

## 2. Yêu cầu

| Yêu cầu | Ngày | Feature | Trạng thái | Ticket xong |
| --- | --- | --- | --- | --- |
| Quảng cáo native và remote config cho dự án mẫu (đợt 2) ([2026-10-21-native-va-remote-config.md](features/ads/requests/2026-10-21-native-va-remote-config.md)) | 2026-10-21 | ads | `specified` đã có spec | 64/75 |
| Đưa cách làm tốt của jbase vào quảng cáo của dự án mẫu (đợt 4) ([2026-10-29-hoc-tu-jbase.md](features/ads/requests/2026-10-29-hoc-tu-jbase.md)) | 2026-10-29 | ads | `specified` đã có spec | 64/75 |
| Nạp sẵn mọi native và nhịp làm mới chỉnh từ XA ([2026-11-02-nap-san-va-nhip-lam-moi.md](features/ads/requests/2026-11-02-nap-san-va-nhip-lam-moi.md)) | 2026-11-02 | ads | `specified` đã có spec | 64/75 |
| Native toàn màn hình chỉ sau interstitial của vị trí trong app ([2026-11-06-combo-chi-sau-inter-trong-app.md](features/ads/requests/2026-11-06-combo-chi-sau-inter-trong-app.md)) | 2026-11-06 | ads | `specified` đã có spec | 64/75 |
| Quảng cáo trong màn hình không chồng lên nhau ([2026-11-06-quang-cao-khong-chong-nhau.md](features/ads/requests/2026-11-06-quang-cao-khong-chong-nhau.md)) | 2026-11-06 | ads | `specified` đã có spec | 64/75 |
| Giãn dần thời gian thử lại native khi khe chưa có quảng cáo ([2026-11-11-gian-thu-lai-native-khi-chua-co-quang-cao.md](features/ads/requests/2026-11-11-gian-thu-lai-native-khi-chua-co-quang-cao.md)) | 2026-11-11 | ads | `specified` đã có spec | 64/75 |
| Màn hình loading có thanh tiến trình ([2026-10-15-man-hinh-loading.md](features/loading/requests/2026-10-15-man-hinh-loading.md)) | 2026-10-15 | loading | `specified` đã có spec | 6/6 |
| App luôn cần mạng ([2026-10-21-app-luon-can-mang.md](features/network/requests/2026-10-21-app-luon-can-mang.md)) | 2026-10-21 | network | `specified` đã có spec | 3/3 |
| Giảm chi phí github actions ([2026-10-29-giam-phut-actions.md](features/quy-trinh/requests/2026-10-29-giam-phut-actions.md)) | 2026-10-29 | quy-trinh | `specified` đã có spec | 20/20 |
| Giảm thời gian chờ của quy trình ([2026-11-12-giam-thoi-gian-cho.md](features/quy-trinh/requests/2026-11-12-giam-thoi-gian-cho.md)) | 2026-11-12 | quy-trinh | `specified` đã có spec | 20/20 |

## 3. Feature

### ads

Spec: [spec.md](features/ads/spec.md) `confirmed`. Ticket: 63 `on-staging`, 11 `backlog`, 1 `accepted`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-007](features/ads/tickets/T-007-khoa-remote-config-co-kieu.md) | Khóa Remote Config có kiểu, khai báo một chỗ, bảng khóa sinh tự động | `on-staging` | `done` (bản 2) | 25M | RUN-20261023-01 |
| [T-008](features/ads/tickets/T-008-gia-tri-cuc-bo-cho-staging.md) | Giá trị Remote Config cục bộ cho bản staging | `on-staging` | `done` (bản 2) | 10M | RUN-20261023-01 |
| [T-009](features/ads/tickets/T-009-tan-suat-va-cong-tac-tong.md) | Công tắc tổng, ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên | `on-staging` | `done` (bản 2) | 20M | RUN-20261023-01 |
| [T-010](features/ads/tickets/T-010-native-small-va-medium.md) | Native small và medium; màn hình mẫu dùng native small thay banner | `on-staging` | `done` (bản 2) | 40M | RUN-20261023-01 |
| [T-011](features/ads/tickets/T-011-native-trong-danh-sach.md) | Native trong danh sách | `on-staging` | `done` (bản 2) | 20M | RUN-20261023-01 |
| [T-012](features/ads/tickets/T-012-native-thu-gon.md) | Native thu gọn được | `on-staging` | `done` (bản 2) | 20M | RUN-20261023-01 |
| [T-013](features/ads/tickets/T-013-native-toan-man-hinh.md) | Native toàn màn hình thay interstitial | `on-staging` | `done` (bản 2) | 25M | RUN-20261023-01 |
| [T-014](features/ads/tickets/T-014-man-hinh-xem-thu-quang-cao.md) | Màn hình xem thử quảng cáo chỉ ở bản staging | `on-staging` | `done` (bản 4) | 30M | RUN-20261023-01, RUN-20261024-01 |
| [T-015](features/ads/tickets/T-015-mediation-applovin.md) | Mediation AdMob và AppLovin | `on-staging` | `done` (bản 3) | 10M | RUN-20261023-01 |
| [T-019](features/ads/tickets/T-019-khoa-remote-config-theo-adskit.md) | Khóa Remote Config quảng cáo theo tên AdsKit và danh sách vị trí theo mô hình chung | `on-staging` | `done` (bản 1) | 20M | RUN-20261024-02 |
| [T-020](features/ads/tickets/T-020-dong-y-va-khoi-dong-partner.md) | Đồng ý quảng cáo và khởi động SDK theo PARTNER | `on-staging` | `done` (bản 1) | 30M | RUN-20261024-02 |
| [T-021](features/ads/tickets/T-021-nap-san-va-nap-lai.md) | Nạp sẵn theo nhóm, chờ SDK, thử lại và nạp lại khi có mạng | `on-staging` | `done` (bản 2) | 20M | RUN-20261024-02, RUN-20261024-03, RUN-20261025-01 |
| [T-022](features/ads/tickets/T-022-cong-toan-man-hinh-va-chan-tu-hien.md) | Cổng quảng cáo toàn màn hình, mốc ads_interval, hồi phục kẹt, chặn quảng cáo tự hiện | `on-staging` | `done` (bản 1) | 20M | RUN-20261024-02 |
| [T-023](features/ads/tickets/T-023-doanh-thu-theo-vi-tri.md) | Doanh thu theo vị trí ad_impression_placement và số liệu vòng đời Firebase | `on-staging` | `done` (bản 2) | 20M | RUN-20261024-02, RUN-20261024-03 |
| [T-024](features/ads/tickets/T-024-app-open-luc-mo-app.md) | Quảng cáo mở app ở màn loading (lần đầu sau khi cài và lần thường) | `on-staging` | `done` (bản 3) | 25M | RUN-20261026-01, RUN-20261026-02 |
| [T-025](features/ads/tickets/T-025-quang-cao-khi-quay-lai-app.md) | Quảng cáo khi quay lại app, bỏ lượt sau khi bấm quảng cáo | `on-staging` | `done` (bản 1) | 20M | RUN-20261026-03, RUN-20261026-04 |
| [T-026](features/ads/tickets/T-026-inter-va-native-sau-inter.md) | Interstitial theo PARTNER và combo native toàn màn hình sau inter | `on-staging` | `done` (bản 1) | 20M | RUN-20261026-03, RUN-20261026-04 |
| [T-027](features/ads/tickets/T-027-native-theo-kich-thuoc-adskit.md) | Native một quảng cáo theo kích thước AdsKit: khe đáy, đầy đủ, toàn màn hình | `on-staging` | `done` (bản 2) | 15M | RUN-20261026-03 |
| [T-028](features/ads/tickets/T-028-pool-native.md) | Pool native và native trong tập hợp (lưới, danh sách, pager), medium | `on-staging` | `done` (bản 1) | 30M | RUN-20261026-03 |
| [T-029](features/ads/tickets/T-029-banner-theo-partner.md) | Banner cố định và banner thu gọn theo PARTNER | `on-staging` | `done` (bản 1) | 15M | RUN-20261026-01 |
| [T-030](features/ads/tickets/T-030-thanh-day-va-native-thu-gon.md) | Thanh đáy ghép theo màn: native nhỏ 80 dp hoặc banner, lớp native thu gọn 250 dp tự mở lại | `on-staging` | `done` (bản 1) | 30M | RUN-20261026-03, RUN-20261026-04 |
| [T-031](features/ads/tickets/T-031-rewarded-va-hop-thoai.md) | Rewarded là hành động có điều kiện, kèm hộp thoại xem quảng cáo | `on-staging` | `done` (bản 1) | 20M | RUN-20261026-03, RUN-20261026-04 |
| [T-032](features/ads/tickets/T-032-native-gioi-thieu.md) | Native ở một bước của luồng (giới thiệu và luồng khác) | `on-staging` | `done` (bản 1) | 15M | RUN-20261026-03, RUN-20261026-04 |
| [T-033](features/ads/tickets/T-033-appsflyer.md) | AppsFlyer trong :core:analytics: doanh thu và phễu quảng cáo | `on-staging` | `done` (bản 1) | 25M | RUN-20261026-03, RUN-20261026-04 |
| [T-034](features/ads/tickets/T-034-mediation-chin-mang.md) | Mediation chín mạng | `on-staging` | `done` (bản 2) | 15M | RUN-20261024-02, RUN-20261024-03 |
| [T-035](features/ads/tickets/T-035-ve-lai-native-tren-figma.md) | Vẽ lại component native trên Figma theo kích thước AdsKit | `accepted` | chưa có | - | - |
| [T-036](features/ads/tickets/T-036-man-xem-thu-them-dinh-dang.md) | Màn hình xem thử quảng cáo thêm các định dạng mới | `on-staging` | `done` (bản 1) | 25M | RUN-20261026-03, RUN-20261026-04 |
| [T-037](features/ads/tickets/T-037-giu-luot-khi-ke-hoach-dang-chay.md) | Giữ lượt nạp sẵn tới khi kế hoạch đang chạy xong | `on-staging` | `done` (bản 1) | 5M | RUN-20261025-02 |
| [T-038](features/ads/tickets/T-038-xoa-luot-giu-khi-nap-san-chay.md) | Xóa lượt giữ khi lượt xin nạp sẵn mới được chạy | `on-staging` | `done` (bản 1) | 5M | RUN-20261026-01 |
| [T-040](features/ads/tickets/T-040-nguon-mot-quang-cao-va-lam-moi.md) | Khe và nguồn native một quảng cáo: lấy, hủy, hết hạn, làm mới 60 giây | `on-staging` | `done` (bản 1) | 15M | RUN-20261026-03 |
| [T-041](features/ads/tickets/T-041-trang-toan-trang-cua-luong.md) | Native toàn trang của luồng giữ CTA trong màn thấp và giữ quảng cáo khi xoay, gập máy | `on-staging` | `done` (bản 2) | 22M | RUN-20261026-05, RUN-20261026-06 |
| [T-042](features/ads/tickets/T-042-rewarded-bam-dup-thuong-mot-lan.md) | Rewarded: bấm "Xem quảng cáo" hai lần khi chưa có quảng cáo chỉ làm hành động một lần | `on-staging` | `done` (bản 1) | 5M | RUN-20261026-05 |
| [T-043](features/ads/tickets/T-043-resume-kiem-vi-tri-truoc-khi-chan.md) | Quảng cáo khi quay lại app: kiểm vị trí tắt trước cổng chặn (L-A05) | `on-staging` | `done` (bản 1) | 4.1M | RUN-20261026-05 |
| [T-044](features/ads/tickets/T-044-bo-sung-test-quang-cao.md) | Bổ sung test quảng cáo còn yếu theo rà soát RUN-20261026-03 và RUN-20261026-04 | `on-staging` | `done` (bản 1) | 7M | RUN-20261026-05 |
| [T-045](features/ads/tickets/T-045-cta-native-khi-chu-lon.md) | Native giữ trọn CTA, nhãn "Ad" và media 120 dp khi cỡ chữ hệ thống lớn | `on-staging` | `done` (bản 1) | 22M | RUN-20261026-05, RUN-20261026-06 |
| [T-046](features/ads/tickets/T-046-don-sau-ra-soat-quang-cao.md) | Dọn quảng cáo theo ghi chú rà soát RUN-20261026-05 và RUN-20261026-06 | `on-staging` | `done` (bản 1) | 4.6M | RUN-20261028-01 |
| [T-047](features/ads/tickets/T-047-rewarded-quyet-dinh-treo.md) | Rewarded: lượt quyết định treo không giữ mãi các lần bấm sau | `on-staging` | `done` (bản 3) | 4.6M | RUN-20261028-01 |
| [T-048](features/ads/tickets/T-048-don-sau-ra-soat-t-047.md) | Dọn rewarded theo ghi chú rà soát T-047 | `on-staging` | `done` (bản 1) | 4.9M | RUN-20261028-02 |
| [T-049](features/ads/tickets/T-049-trang-toan-trang-man-ngang.md) | Trang toàn trang của luồng không để trống khi xoay ngang trên điện thoại thật | `on-staging` | `done` (bản 1) | 10M | RUN-20261028-02 |
| [T-053](features/ads/tickets/T-053-dem-nguoc-va-bung-lai-theo-jbase.md) | Đếm ngược native sau inter và bung lại native thu gọn theo JBase | `on-staging` | `done` (bản 1) | 23.9M | RUN-20261030-01 |
| [T-054](features/ads/tickets/T-054-dung-quang-cao-khi-bi-che.md) | Quảng cáo trong màn hình dừng khi native toàn màn hình che | `on-staging` | `done` (bản 1) | 24M | RUN-20261030-01 |
| [T-055](features/ads/tickets/T-055-hien-toan-man-hinh-an-toan.md) | Kiểm màn hình trước khi hiện quảng cáo toàn màn hình, xoay máy | `on-staging` | `done` (bản 2) | 5.4M | RUN-20261030-01 |
| [T-056](features/ads/tickets/T-056-ma-theo-khe-trong-code.md) | Mã đơn vị theo khe trong code: mọi lớp của thanh đáy có mã ở production | `on-staging` | `done` (bản 2) | 23.9M | RUN-20261030-01 |
| [T-057](features/ads/tickets/T-057-release-sh-doc-ma-theo-khe.md) | release.sh đọc và kiểm mã theo khe | `backlog` | chưa có | - | - |
| [T-058](features/ads/tickets/T-058-may-thu-va-log-tung-mang.md) | Máy thử và log từng mạng ở bản staging | `on-staging` | `done` (bản 4) | 8M | RUN-20261030-01 |
| [T-059](features/ads/tickets/T-059-native-trong-danh-sach-thu-lai.md) | Native small trong danh sách thử lại khi lỗi | `on-staging` | `done` (bản 2) | 5.4M | RUN-20261030-01 |
| [T-060](features/ads/tickets/T-060-don-native-sao-va-chu-lon.md) | Dọn native: hàng sao, chữ lớn ở native nhỏ, chú thích cũ | `on-staging` | `done` (bản 1) | 5.4M | RUN-20261030-01 |
| [T-061](features/ads/tickets/T-061-thanh-day-chua-cho-luc-tai.md) | Thanh đáy chừa chỗ lúc đang tải | `backlog` | chưa có | - | - |
| [T-062](features/ads/tickets/T-062-dinh-dang-banner-mrec.md) | Định dạng banner MREC | `on-staging` | `done` (bản 2) | 24M | RUN-20261030-02 |
| [T-063](features/ads/tickets/T-063-cong-khong-hien-quang-cao.md) | Cổng không hiện quảng cáo cho người đã mua gói bỏ quảng cáo | `on-staging` | `done` (bản 2) | 23.9M | RUN-20261030-01 |
| [T-064](features/ads/tickets/T-064-ve-lai-native-theo-jbase.md) | Vẽ lại native nhỏ, thu gọn và medium trên Figma theo kích thước JBase | `backlog` | chưa có | - | - |
| [T-065](features/ads/tickets/T-065-kich-thuoc-native-theo-jbase.md) | Code kích thước native theo JBase | `backlog` | chưa có | - | - |
| [T-066](features/ads/tickets/T-066-cached-early-luon-bat.md) | Khởi động sớm bằng đồng ý của lần mở trước luôn bật | `on-staging` | `done` (bản 1) | 5.4M | RUN-20261030-01 |
| [T-067](features/ads/tickets/T-067-banner-thich-ung-thuong.md) | Banner cố định dùng cỡ thích ứng neo đáy thường | `on-staging` | `done` (bản 1) | 5.4M | RUN-20261030-01 |
| [T-068](features/ads/tickets/T-068-sdk-max-va-khoi-tao.md) | SDK AppLovin MAX và khởi tạo sau cổng đồng ý | `on-staging` | `done` (bản 4) | 23.9M | RUN-20261030-01, RUN-20261030-02 |
| [T-069](features/ads/tickets/T-069-inter-va-rewarded-cua-max.md) | Interstitial và rewarded của MAX qua cổng toàn màn hình chung | `backlog` | chưa có | - | - |
| [T-070](features/ads/tickets/T-070-app-open-cua-max-khi-quay-lai.md) | App open của MAX khi quay lại app | `backlog` | chưa có | - | - |
| [T-071](features/ads/tickets/T-071-banner-va-mrec-cua-max.md) | Banner và MREC của MAX | `on-staging` | `done` (bản 3) | 26M | RUN-20261031-01 |
| [T-072](features/ads/tickets/T-072-doanh-thu-va-pheu-cua-max.md) | Doanh thu và phễu của MAX | `backlog` | chưa có | - | - |
| [T-073](features/ads/tickets/T-073-release-sh-ma-max.md) | release.sh đọc và kiểm khóa, mã đơn vị MAX | `backlog` | chưa có | - | - |
| [T-085](features/ads/tickets/T-085-don-khung-hai-nen-tang.md) | Dọn test và luật biên của khung banner hai nền tảng | `on-staging` | `done` (bản 1) | 7.3M | RUN-20261101-01 |
| [T-086](features/ads/tickets/T-086-nap-san-native-thanh-day.md) | Kho nạp sẵn cho native nhỏ và native thu gọn | `on-staging` | `done` (bản 2) | 30.0M | RUN-20261107-01 |
| [T-087](features/ads/tickets/T-087-nhip-lam-moi-thanh-day-tu-xa.md) | Nhịp làm mới native nhỏ, native thu gọn, banner thu gọn theo Remote Config | `on-staging` | `done` (bản 2) | 22.2M | RUN-20261107-01 |
| [T-088](features/ads/tickets/T-088-banner-admob-tai-lai-tu-xa.md) | Banner cố định và MREC (AdMob và MAX) tải lại theo Remote Config | `on-staging` | `done` (bản 2) | 22.2M | RUN-20261107-01 |
| [T-089](features/ads/tickets/T-089-khong-combo-sau-inter-resume.md) | Inter khi quay lại app không nối native toàn màn hình | `on-staging` | `done` (bản 1) | 7.2M | RUN-20261107-01 |
| [T-090](features/ads/tickets/T-090-khe-day-nhuong-lop-noi.md) | Khe đáy nhường lớp native thu gọn | `backlog` | chưa có | - | - |
| [T-091](features/ads/tickets/T-091-mot-quang-cao-neo-day.md) | Mỗi màn tối đa một quảng cáo neo đáy | `backlog` | chưa có | - | - |
| [T-092](features/ads/tickets/T-092-quang-cao-noi-dung-thang-lop-noi.md) | Quảng cáo trong nội dung thắng lớp nổi | `backlog` | chưa có | - | - |
| [T-093](features/ads/tickets/T-093-thong-diep-loi-trong-log-tai.md) | Thông điệp lỗi của SDK trong log lỗi tải quảng cáo | `on-staging` | `done` (bản 1) | 24.1M | RUN-20261108-01 |
| [T-097](features/ads/tickets/T-097-o-native-trong-luoi.md) | Ô native trong lưới bỏ ô khi hết quảng cáo, nhận chiều cao ô và thẻ, vẽ được ở màn không trạng thái | `on-staging` | `done` (bản 1) | 30.0M | RUN-20261110-01 |
| [T-098](features/ads/tickets/T-098-the-hop-thoai-co-thuong.md) | Thẻ hộp thoại quảng cáo có thưởng công khai cho screenshot test, cách dịch chữ của khung ở :app | `on-staging` | `done` (bản 1) | 8.6M | RUN-20261110-01 |
| [T-099](features/ads/tickets/T-099-the-native-cua-buoc-va-lan-mo-dau.md) | Feature biết thẻ native của một bước có được phép không; vị trí chỉ hiện ở lần mở app đầu tiên | `on-staging` | `done` (bản 1) | 22.0M | RUN-20261110-01 |
| [T-101](features/ads/tickets/T-101-pool-cho-buoc-ke-tiep.md) | Feature hỏi được pool native_step đã có quảng cáo cho bước kế tiếp của luồng chưa | `on-staging` | `done` (bản 1) | 21.1M | RUN-20261111-01 |
| [T-102](features/ads/tickets/T-102-combo-khong-treo-khi-thieu-host.md) | Lượt native combo không treo khi không có FullScreenAdHost nào đang vẽ | `on-staging` | `done` (bản 1) | 21.1M | RUN-20261111-01 |
| [T-103](features/ads/tickets/T-103-gian-thu-lai-native-khi-chua-co-quang-cao.md) | Giãn dần thời gian thử lại native khi khe chưa có quảng cáo | `on-staging` | `done` (bản 1) | 18.6M | RUN-20261113-02 |

Phản hồi: 6 `fixed`, 2 `triaged`, 1 `wontfix`.
- Đang mở: [FB-20261023-02-native-loi-0-tren-emulator.md](features/ads/feedback/FB-20261023-02-native-loi-0-tren-emulator.md) `triaged`, mức `nhe`: Native sample_native_small tải thất bại với mã 0 trên emulator của CI
- Đang mở: [FB-20261024-02-log-adapter-thieu-tam-mang.md](features/ads/feedback/FB-20261024-02-log-adapter-thieu-tam-mang.md) `triaged`, mức `nang`: Log trạng thái adapter trên emulator chỉ có AppLovin, thiếu tám mạng của T-034

### loading

Spec: [spec.md](features/loading/spec.md) `confirmed`. Ticket: 6 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-001](features/loading/tickets/T-001-man-hinh-loading.md) | Màn hình loading có thanh tiến trình khi mở app | `on-staging` | `done` (bản 2) | - | - |
| [T-002](features/loading/tickets/T-002-thuong-hieu-xanh-luc.md) | Hệ thống thiết kế theo thương hiệu mới (xanh lục, Inter, logo) | `on-staging` | `done` (bản 2) | - | - |
| [T-003](features/loading/tickets/T-003-giao-dien-loading-theo-thiet-ke.md) | Màn hình loading theo thiết kế Figma | `on-staging` | `done` (bản 1) | - | - |
| [T-004](features/loading/tickets/T-004-mau-con-thieu-va-gian-chu.md) | Đủ vai trò màu thương hiệu và giãn cách chữ như thiết kế | `on-staging` | `done` (bản 1) | - | - |
| [T-005](features/loading/tickets/T-005-man-hinh-thap.md) | Màn hình loading dùng được trên màn hình thấp | `on-staging` | `done` (bản 1) | - | - |
| [T-006](features/loading/tickets/T-006-cuon-man-hinh-rat-thap.md) | Test cuộn tới nút "Thử lại" trên màn hình rất thấp | `on-staging` | `done` (bản 1) | 5M | RUN-20261024-02 |

Phản hồi: 1 `fixed`, 1 `wontfix`.

### network

Spec: [spec.md](features/network/spec.md) `confirmed`. Ticket: 3 `on-staging`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-016](features/network/tickets/T-016-theo-doi-ket-noi.md) | Theo dõi có Internet dùng được, dùng chung cho cả app | `on-staging` | `done` (bản 2) | 20M | RUN-20261023-01 |
| [T-017](features/network/tickets/T-017-popup-mat-mang.md) | Popup mất mạng chung và chặn vào app khi không có mạng | `on-staging` | `done` (bản 3) | 30M | RUN-20261023-01 |
| [T-018](features/network/tickets/T-018-ly-do-loi-tai-file.md) | Lý do tải file R2 thất bại (AssetFailureReason) | `on-staging` | `done` (bản 1) | 10M | RUN-20261023-01 |

Phản hồi: chưa có.

### quy-trinh

Spec: chưa có spec. Ticket: 20 `on-staging`, 1 `cancelled`.

| Ticket | Tên | Trạng thái | Plan | Ước tính token | Phiên chạy |
| --- | --- | --- | --- | --- | --- |
| [T-039](features/quy-trinh/tickets/T-039-pr-tai-lieu-chay-test-script.md) | PR chỉ có tài liệu vẫn chạy test của script | `on-staging` | `done` (bản 1) | 5M | - |
| [T-050](features/quy-trinh/tickets/T-050-ktlint-kiem-dong-dai.md) | ktlint kiểm dòng dài hơn 120 ký tự | `on-staging` | chưa có | - | - |
| [T-051](features/quy-trinh/tickets/T-051-lich-dinh-ky-tiet-kiem-phut.md) | Lịch định kỳ không cấp runner khi không có phiên tự chạy | `on-staging` | `done` (bản 1) | 23.9M | - |
| [T-052](features/quy-trinh/tickets/T-052-nang-bo-cong-cu-build.md) | Nâng bộ công cụ build lên AGP 9.1, Gradle 9.3.1, compileSdk 37 | `cancelled` | chưa có | - | - |
| [T-074](features/quy-trinh/tickets/T-074-main-guard-bo-qua-noi-dung-da-kiem.md) | main-guard không chạy lại cổng chất lượng cho nội dung đã kiểm ở PR | `on-staging` | `done` (bản 1) | 23.9M | - |
| [T-075](features/quy-trinh/tickets/T-075-test-emulator-sau-merge.md) | Test emulator chạy sau khi merge, không chạy ở từng PR | `on-staging` | `done` (bản 1) | 23.9M | - |
| [T-076](features/quy-trinh/tickets/T-076-pr-check-mot-job.md) | pr-check bỏ job changes riêng | `on-staging` | `done` (bản 2) | 5.4M | RUN-20261101-02 |
| [T-077](features/quy-trinh/tickets/T-077-bot-lan-chay-lai-pr-check.md) | Bớt lần chạy lại pr-check: PR nháp và nhánh chính chỉ đổi tài liệu | `on-staging` | `done` (bản 2) | 24.1M | RUN-20261108-01 |
| [T-078](features/quy-trinh/tickets/T-078-runner-tu-host.md) | Runner tự host cho các job nặng, có đường quay về runner của GitHub | `on-staging` | `done` (bản 2) | 30.0M | RUN-20261108-01 |
| [T-079](features/quy-trinh/tickets/T-079-moi-truong-sdk-37.md) | Môi trường cloud và máy dev có sẵn Android SDK 37, kiểm compileSdk theo đúng tên gói | `on-staging` | `done` (bản 1) | 5.4M | - |
| [T-080](features/quy-trinh/tickets/T-080-bo-build-agp-9.md) | Bộ build AGP 9.1, Gradle 9.3.1, compileSdk 37, không đổi ảnh chuẩn | `on-staging` | `done` (bản 1) | 30.0M | - |
| [T-081](features/quy-trinh/tickets/T-081-robolectric-roborazzi-anh-chuan.md) | Robolectric 4.17, không ảnh chuẩn nào đổi | `on-staging` | `done` (bản 1) | 5.4M | - |
| [T-082](features/quy-trinh/tickets/T-082-chu-mac-dinh-tieng-anh.md) | Chữ mặc định của các module mẫu là tiếng Anh, tiếng Việt ở values-vi/ | `on-staging` | `done` (bản 1) | 21.1M | - |
| [T-083](features/quy-trinh/tickets/T-083-merge-doi-chieu-danh-sach.md) | Khi phiên tự chạy đang chạy, merge-if-green.sh chỉ cho merge nhánh task có trong danh sách của phiên | `on-staging` | `done` (bản 1) | 8.6M | - |
| [T-084](features/quy-trinh/tickets/T-084-theme-co-dinh-cho-test-core-ads.md) | Test của :core:ads vẽ trong theme cố định của test, ảnh chuẩn không đổi theo theme của app | `on-staging` | `done` (bản 1) | 22.3M | RUN-20261101-01 |
| [T-094](features/quy-trinh/tickets/T-094-merge-if-green-paths-ignore.md) | merge-if-green.sh xét "main-guard có chạy không" theo paths-ignore, không cảnh báo sai khi xóa nhánh; hai chỗ hở của pr_sim | `on-staging` | `done` (bản 1) | 22.2M | RUN-20261108-02 |
| [T-095](features/quy-trinh/tickets/T-095-runner-check-log-va-queued.md) | Runner tự host: dòng lỗi của runner-check.sh vào build.log, ngưỡng thời gian cho job queued của resume-check.py | `on-staging` | `done` (bản 1) | 7.4M | RUN-20261108-02 |
| [T-096](features/quy-trinh/tickets/T-096-activity-compose-cho-feature.md) | Plugin feature có activity-compose trên classpath biên dịch, để feature dùng được LocalActivity với AGP 9 | `on-staging` | `done` (bản 1) | 8.6M | - |
| [T-100](features/quy-trinh/tickets/T-100-merge-bo-qua-lan-huy-cu.md) | merge-if-green.sh không từ chối vì một lần chạy bị hủy đã có lần chạy cùng tên sau nó | `on-staging` | `done` (bản 1) | 8.6M | - |
| [T-104](features/quy-trinh/tickets/T-104-tu-kich-hoat-phien.md) | Phiên tự bắt đầu khi danh sách chạy approved có start auto lên nhánh chính | `on-staging` | `done` (bản 1) | 19.4M | - |
| [T-105](features/quy-trinh/tickets/T-105-duyet-ca-dot.md) | Làm theo ADR-0048 - run-check.py --batch cho danh sách của một đợt, tài liệu duyệt cả đợt và bỏ họp | `on-staging` | `done` (bản 2) | 19.4M | RUN-20261113-01 |

Phản hồi: chưa có.

## 4. Phiên tự chạy

| Phiên | Trạng thái | Bắt đầu | Ticket | Bản staging |
| --- | --- | --- | --- | --- |
| [RUN-20261113-02](runs/RUN-20261113-02.md) | `finished` | 2026-11-13T17:34Z | T-103 | 0.1.0-staging.31 |
| [RUN-20261113-01](runs/RUN-20261113-01.md) | `finished` | 2026-11-13T13:49Z | T-105 | 0.1.0-staging.30 |
| [RUN-20261111-01](runs/RUN-20261111-01.md) | `finished` | 2026-11-11T05:09Z | T-101, T-102 | 0.1.0-staging.29 |
| [RUN-20261110-01](runs/RUN-20261110-01.md) | `finished` | 2026-11-10T16:20Z | T-098, T-097, T-099 | 0.1.0-staging.28 |
| [RUN-20261108-02](runs/RUN-20261108-02.md) | `finished` | 2026-11-09T01:42Z | T-094, T-095 | 0.1.0-staging.27 |
| [RUN-20261108-01](runs/RUN-20261108-01.md) | `finished` | 2026-11-08T06:05Z | T-093, T-077, T-078 | 0.1.0-staging.26 |
| [RUN-20261107-01](runs/RUN-20261107-01.md) | `finished` | 2026-11-07T11:03Z | T-089, T-086, T-087, T-088 | 0.1.0-staging.24 |
| [RUN-20261101-02](runs/RUN-20261101-02.md) | `finished` | 2026-11-07T16:31Z | T-076 | 0.1.0-staging.25 |
| [RUN-20261101-01](runs/RUN-20261101-01.md) | `finished` | 2026-11-01T05:01Z | T-084, T-085 | 0.1.0-staging.23 |
| [RUN-20261031-01](runs/RUN-20261031-01.md) | `finished` | 2026-10-31T16:41Z | T-071 | - |
| [RUN-20261030-02](runs/RUN-20261030-02.md) | `finished` | 2026-10-30T15:45Z | T-068, T-062 | 0.1.0-staging.21 |
| [RUN-20261030-01](runs/RUN-20261030-01.md) | `finished` | 2026-10-30 03:19 UTC | T-054, T-067, T-058, T-059, T-055, T-053, T-066, T-063, T-056, T-060, T-068 | 0.1.0-staging.20 |
| [RUN-20261028-02](runs/RUN-20261028-02.md) | `finished` | 2026-10-28T07:33Z | T-048, T-049 | 0.1.0-staging.18 |
| [RUN-20261028-01](runs/RUN-20261028-01.md) | `finished` | 2026-10-27T17:43Z | T-046, T-047 | 0.1.0-staging.17 |
| [RUN-20261026-06](runs/RUN-20261026-06.md) | `finished` | 2026-10-27T05:53Z | T-041, T-045 | 0.1.0-staging.16 |
| [RUN-20261026-05](runs/RUN-20261026-05.md) | `finished` | 2026-10-26T15:11Z | T-043, T-042, T-044, T-041, T-045 | 0.1.0-staging.15 |
| [RUN-20261026-04](runs/RUN-20261026-04.md) | `finished` | 2026-10-26T00:59Z | T-026, T-030, T-031, T-032, T-025, T-033, T-036 | 0.1.0-staging.14 |
| [RUN-20261026-03](runs/RUN-20261026-03.md) | `finished` | 2026-10-25 17:25 UTC | T-027, T-040, T-028, T-026, T-030, T-031, T-032, T-025, T-033, T-036 | - |
| [RUN-20261026-02](runs/RUN-20261026-02.md) | `finished` | 2026-10-25T15:17Z | T-024 | 0.1.0-staging.13 |
| [RUN-20261026-01](runs/RUN-20261026-01.md) | `finished` | 2026-10-25 11:24 UTC | T-038, T-029, T-024 | 0.1.0-staging.12 |
| [RUN-20261025-02](runs/RUN-20261025-02.md) | `finished` | 2026-10-25 04:20 UTC | T-037 | 0.1.0-staging.11 |
| [RUN-20261025-01](runs/RUN-20261025-01.md) | `finished` | 2026-10-24 15:07 UTC | T-021 | 0.1.0-staging.10 |
| [RUN-20261024-03](runs/RUN-20261024-03.md) | `finished` | 2026-10-24 12:16 | T-034, T-023, T-021 | 0.1.0-staging.9 |
| [RUN-20261024-02](runs/RUN-20261024-02.md) | `finished` | 2026-10-24T03:50Z | T-006, T-034, T-019, T-020, T-022, T-023, T-021 | 0.1.0-staging.8 |
| [RUN-20261024-01](runs/RUN-20261024-01.md) | `finished` | 2026-10-23 16:12 UTC | T-014 | 0.1.0-staging.7 |
| [RUN-20261023-01](runs/RUN-20261023-01.md) | `finished` | 2026-10-23T07:42Z | T-018, T-007, T-008, T-009, T-015, T-010, T-016, T-011, T-012, T-013, T-017, T-014 | 0.1.0-staging.6 |

## 5. Bài học và ADR

- Bài học ([lessons.md](lessons.md)): 141 bài; 136 `applied`, 2 `verified`, 1 `accepted`, 1 `proposed`, 1 `rejected`.
- ADR: 49; 33 `accepted`, 16 `superseded`.

## 6. Sẵn sàng phát hành

<!-- Chỉ từ tài liệu trong repo. Mã quảng cáo thật, cấu hình production và tình trạng trên Google Play
không nằm trong repo: người phát hành kiểm theo docs/ops/release.md. -->

### Quảng cáo

- Bản production cần một mã quảng cáo thật cho mỗi khóa dưới đây (docs/ops/release.md, mục 1.2): `sample_native_small` (native_small), `open_app` (app_open), `open_app_inter` (interstitial), `native_combo` (native_full_screen), `resume` (app_open), `resume_inter` (interstitial).

### Kiểm thủ công chưa đánh dấu

125 mục trong 25 phiên. Người kiểm xong thì đánh dấu [x] trong báo cáo phiên.
- [RUN-20261113-02](runs/RUN-20261113-02.md): (Đã merge 2026-11-14 tại `7603940`, `main-guard` xanh, phát `0.1.0-staging.31`; AI làm theo lời anh Nhật, người đánh dấu) **Merge PR #489 (T-103)** bằng...
- [RUN-20261113-02](runs/RUN-20261113-02.md): Merge PR báo cáo này bằng `scripts/merge-if-green.sh` (phiên không merge được, L-173)
- [RUN-20261113-02](runs/RUN-20261113-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-103, mục 4 của plan, dòng cuối: ghi một phiên khoảng 3 phút ở "Ad gallery" và "Grid ads" khi native không về,...
- [RUN-20261113-02](runs/RUN-20261113-02.md): Kịch bản emulator lỗi hay bỏ qua: Không chạy (không có task merge)
- [RUN-20261113-02](runs/RUN-20261113-02.md): Câu khẳng định cũ viết lại (ADR-0031): T-103, 6 câu viết sẵn ở mục 3.5 của plan (ADR-0031, quyết định 1):...
- [RUN-20261113-02](runs/RUN-20261113-02.md): Quyết định AI đã tự làm: 2, xem mục trên
- [RUN-20261113-02](runs/RUN-20261113-02.md): Điểm rà soát mức ghi chú (T-103): (1) `slot_successThenFailure_waits5s` cũng qua trên code cũ (plan đã nêu, dịp "nạp được" của khe kiểm qua đường mở lại); (2)...
- [RUN-20261113-01](runs/RUN-20261113-01.md): Tiêu chí kiểm tra thủ công (người): T-105, "Tài liệu đúng ý" (mục 4 của plan): đọc các đoạn sửa ở mục 3.1 B trong PR #483, nhất là mục "Khi duyệt cả đợt" của...
- [RUN-20261113-01](runs/RUN-20261113-01.md): Điểm rà soát mức ghi chú: T-105: (1) `run-check.py --batch` đọc `git diff --name-only` không có `-z` và tách theo khoảng trắng, nên file có đường dẫn không...
- [RUN-20261111-01](runs/RUN-20261111-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-102, trên bản staging có `FullScreenAdHost`, bật `is_show_native_after_inter`: sau interstitial combo vẫn hiện...
- [RUN-20261111-01](runs/RUN-20261111-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261111-01](runs/RUN-20261111-01.md): Quyết định AI đã tự làm: 3, xem mục trên (quan trọng nhất: `start(...)` trong phần dựng test của T-102).
- [RUN-20261111-01](runs/RUN-20261111-01.md): Điểm rà soát mức ghi chú: T-101 (6) và T-102 (6), xem nhật ký plan; đáng xem: T-101 `existingPool` trả 0 khi vị trí chưa từng có pool dù pool cùng mã đơn vị có...
- [RUN-20261111-01](runs/RUN-20261111-01.md): Đóng issue #444 (PR #462) và #445 phần (a) (PR #463) khi đồng ý; phiên không tự đóng.
- [RUN-20261110-01](runs/RUN-20261110-01.md): Tiêu chí kiểm tra thủ công (người): T-098, đọc câu mới của `docs/ops/ads-guide.md` (mục 5, bước 4, ý "Rewarded") đối chiếu mục 4.1 của plan, và gạch đầu dòng...
- [RUN-20261110-01](runs/RUN-20261110-01.md): T-099, hai dòng nối không có test tự động (mục 4 của plan, "Giới hạn"): tham số cuối của `AdPlacementDeclaration(...)` trong `AdPlacement.kt`, mặc định...
- [RUN-20261110-01](runs/RUN-20261110-01.md): Luật mới cần người phụ trách sản phẩm xem: P-06 (ô một ô của lưới), MC-06, A-01, OB-03, OB-04 trong `docs/ops/ads-business-rules.md`.
- [RUN-20261110-01](runs/RUN-20261110-01.md): Đóng issue #380 (PR #449), #379 (PR #450), #381 phần `:core:ads` (PR #451) khi đồng ý; phiên không tự đóng.
- [RUN-20261110-01](runs/RUN-20261110-01.md): Quyết định AI đã tự làm: 7, xem mục trên.
- [RUN-20261110-01](runs/RUN-20261110-01.md): Điểm rà soát mức ghi chú: (3 dòng con)
- [RUN-20261108-02](runs/RUN-20261108-02.md): Tiêu chí kiểm tra thủ công: T-094, L-149 trên GitHub thật (kết quả lần merge PR báo cáo ghi trong mô tả PR báo cáo; phiên kế tiếp đối chiếu thêm...
- [RUN-20261108-02](runs/RUN-20261108-02.md): Quyết định AI đã tự làm: 2, xem mục trên.
- [RUN-20261108-02](runs/RUN-20261108-02.md): Điểm rà soát mức ghi chú: T-094 (4 điểm, nhật ký plan T-094 và mô tả PR #425: dòng stderr của `wc -l < build.log` có sẵn khi T18 chạy; `MERGE_BRANCH_TRIES=0`...
- [RUN-20261108-02](runs/RUN-20261108-02.md): Nhánh thử còn trên GitHub: `git push origin --delete claude/probe-RUN-20261108-01-060550-400 claude/probe-RUN-20261108-02-014232-678`.
- [RUN-20261108-01](runs/RUN-20261108-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): (3 dòng con)
- [RUN-20261108-01](runs/RUN-20261108-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261108-01](runs/RUN-20261108-01.md): Dọn nhánh: `merge-if-green.sh` báo không xóa được `claude/T-093-thong-diep-loi`, `claude/T-077-bo-qua-cap-nhat`, `claude/T-078-runner-tu-host` (có thể GitHub...
- [RUN-20261107-01](runs/RUN-20261107-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): (4 dòng con)
- [RUN-20261107-01](runs/RUN-20261107-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261107-01](runs/RUN-20261107-01.md): Phần ngoài mục 3.2 đã merge (ADR-0029): T-087, `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/NativeSendGate.kt`, chỉ một dòng KDoc, PR #404.
- [RUN-20261107-01](runs/RUN-20261107-01.md): Câu khẳng định cũ viết lại (ADR-0031): T-086, `NativeAdRefreshTest.cancelled_destroysShownAd` và `cancelledBetweenClaimAndEmit_newAdKeptByStock` (đổi từ "quảng...
- [RUN-20261107-01](runs/RUN-20261107-01.md): Quyết định AI đã tự làm: 12, xem mục trên (nhất là cách đọc chuỗi thử lại của T-088). **2026-11-08, AI ghi theo lời anh Nhật:** chấp nhận cách đọc chuỗi thử...
- [RUN-20261107-01](runs/RUN-20261107-01.md): Điểm rà soát mức ghi chú: ghi trong mô tả PR #402, #403, #404, #406. Đáng xem: T-086, khe `NativeAdSlot(placement)` của vị trí trong danh sách đi vào kho mà...
- [RUN-20261107-01](runs/RUN-20261107-01.md): Nhánh task đã merge: `merge-if-green.sh` báo không xóa được nhánh cả bốn lần, nhưng GitHub tự xóa (bài học L-149); kiểm `claude/T-088-banner-tai-lai` đã mất.
- [RUN-20261107-01](runs/RUN-20261107-01.md): PR #405 (ads: phân loại hai phản hồi, của người, mở 15:27 lúc phiên đang chạy): `merge-if-green.sh` từ chối khi danh sách còn `running`; merge sau khi PR báo...
- [RUN-20261101-02](runs/RUN-20261101-02.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261101-02](runs/RUN-20261101-02.md): Điểm rà soát mức ghi chú (T-076): (1) commit a2a19a5 như trên; (2) `pr_sim` bỏ qua im lặng dòng mẫu không có nháy đơn (ví dụ `- docs/**`): P6b có thể xanh...
- [RUN-20261101-02](runs/RUN-20261101-02.md): Sau T-076: anh Nhật và Tuấn kéo nhánh chính (`git pull`) trước lần merge kế tiếp; worktree hay nhánh tạo trước 17:09 UTC ngày 2026-11-07 gọi...
- [RUN-20261101-02](runs/RUN-20261101-02.md): Xóa nhánh thử của `push-probe.sh`: `git push origin --delete claude/probe-RUN-20261101-02-163129-415`.
- [RUN-20261101-02](runs/RUN-20261101-02.md): Việc của plan T-076 để sau (mục 8 câu 5, 7): PR quy trình thêm dòng bảng tra lỗi cho `chưa thấy kiểm tra 'changes'`; PR riêng thêm một dòng ghi chú vào...
- [RUN-20261101-01](runs/RUN-20261101-01.md): Tiêu chí kiểm tra thủ công (người: đọc tài liệu): T-084, đoạn mới ở `docs/ops/to-app.md` mục 2 và câu ở `docs/ops/ads-guide.md` mục 5 ý 5; T-085, dòng B-09 mới...
- [RUN-20261031-01](runs/RUN-20261031-01.md): Merge PR báo cáo này bằng `scripts/merge-if-green.sh` (phiên không merge được, L-146)
- [RUN-20261031-01](runs/RUN-20261031-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-071, mục 4 của plan (khóa SDK MAX và `ads.maxAdUnits` thật): thứ tự `max_admob`, `admob`, `max`; `MAX ready`...
- [RUN-20261031-01](runs/RUN-20261031-01.md): Kịch bản emulator lỗi hay bỏ qua: Không chạy (không có task merge)
- [RUN-20261030-02](runs/RUN-20261030-02.md): Ảnh chuẩn screenshot ghi lại (T-062, plan cho phép): `app/src/test/screenshots/AdGalleryScreen_banners_light.png`, `AdGalleryScreen_banners_small.png` (hộp...
- [RUN-20261030-02](runs/RUN-20261030-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-068 kiểm thủ công 1 và 2 của mục 4 (máy có khóa MAX thật: log vẫn đủ chín adapter AdMob, Mediation Debugger...
- [RUN-20261030-02](runs/RUN-20261030-02.md): Xóa tay nhánh `claude/T-062-dinh-dang-banner-mrec` (`merge-if-green.sh` không xóa được, L-092)
- [RUN-20261030-02](runs/RUN-20261030-02.md): Câu khẳng định cũ viết lại (ADR-0031): T-068, `AdsSettingsTest.globalKeys_matchAdsKitTable` (15 thành 16 khóa, thêm `ads_mediation_order`) và...
- [RUN-20261030-02](runs/RUN-20261030-02.md): Quyết định AI đã tự làm: 2, xem mục trên
- [RUN-20261030-02](runs/RUN-20261030-02.md): Điểm rà soát mức ghi chú, T-068 (PR #347): (1) `AdsSdkStartTest.startSdk_wiresAdmobThenMax` chưa buộc `onAdmobReady` nằm sau `awaitAdmobReady`; (2) cổng của...
- [RUN-20261030-02](runs/RUN-20261030-02.md): Điểm rà soát mức ghi chú, T-062 (PR #349): (1) chú thích của `frame()` nêu lý do chưa đúng hẳn; (2) lề `padding(vertical = Spacing.md)` quanh MREC vẫn còn khi...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Ảnh chuẩn screenshot mới: T-053 ghi lại 9 ảnh `NativeFullScreenOverlay_*` của mục 3.2 (nút 48 dp, media từ y = 88), PR #338; T-060 thêm...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Ảnh so sánh với thiết kế: ads, `docs/features/ads/design/review/` cập nhật ở T-053 (lệch 0,6%: nút 48 dp, AdChoices thấp 6 dp, media y = 88), rà soát độc lập...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-054 (thanh đáy dừng khi native sau inter che, log `In-screen ads paused` và `resumed`); T-058 (Ad Inspector...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Kịch bản emulator lỗi hay bỏ qua: xem mục Kiểm thử trên máy ảo.
- [RUN-20261030-01](runs/RUN-20261030-01.md): Phần ngoài mục 3.2 đã merge (ADR-0029): T-053, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerInListRowTest.kt` và...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Câu khẳng định cũ viết lại (ADR-0031): T-059 `InListAdsStateTest.unavailableSlot_notRetriedOnSameScreen` thành `unavailableSlot_retriedAfter5s_notOnScrollBack`...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Quyết định AI đã tự làm: 14, xem mục trên.
- [RUN-20261030-01](runs/RUN-20261030-01.md): Điểm rà soát mức ghi chú: T-054 (`publish()` đặt `_showing` trước `onPublished`; `refreshClaimWhileCovered_...` dựa vào thứ tự bộ lập lịch ảo); T-058 (dòng...
- [RUN-20261030-01](runs/RUN-20261030-01.md): Điểm nên sửa chưa sửa: T-056, lệnh `adb logcat -s Ads` ở `docs/ops/ads-guide.md` mục 9 (và "thẻ `Ads`" ở mục 5 ý 6) không thấy dòng cảnh báo vì logcat ghi thẻ...
- [RUN-20261030-01](runs/RUN-20261030-01.md): T-054: mốc thời gian của hai test viết trước khác mục 4 của plan (xếp theo `docs/auto-run.md` bước 4, không phải L4); xem mục Quyết định AI đã tự làm.
- [RUN-20261030-01](runs/RUN-20261030-01.md): T-059: `fail(3)` của test `listRowMetrics` thành loader giả ném lỗi vì `nativeLoaderForTest` của T-054 không nhận `NativeAdEvents` (L-131).
- [RUN-20261030-01](runs/RUN-20261030-01.md): Hai PR không phải của phiên merge trong lúc phiên `running`: #320 (03:38), #324 (05:24) (L-129). Người xem vì sao `merge-if-green.sh` không chặn được.
- [RUN-20261030-01](runs/RUN-20261030-01.md): Xóa tay các nhánh task đã merge (L-092; `merge-if-green.sh` không xóa được): `claude/T-054-dung-quang-cao-khi-bi-che`, `claude/T-067-banner-thich-ung-thuong`,...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Tiêu chí kiểm tra thủ công (người, đọc tài liệu): T-046, ô "Khung" cột `FULL` của bảng kích thước trong `docs/ops/ads-guide.md`, so với `FULL_BODY_LINES`,...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Tiêu chí kiểm tra thủ công (người, đọc luật): T-047, dòng mới của mục 11, nửa câu R-06 và mục 8.2 trong `docs/ops/ads-business-rules.md`, câu mới ở gạch đầu...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Điểm rà soát mức ghi chú: T-046: (1) KDoc của `NativeFullScreenFrame` trỏ `[FlowStepPageFrame]`, hàm `private` cùng package (theo nguyên văn plan); (2) chú...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Điểm rà soát mức ghi chú: T-047: (1) `loadRound_switchStuck_releasesAfterWait` không khẳng định "không gọi `clearLoading`, `sendBlocked`" khi hết hạn (đột biến...
- [RUN-20261028-01](runs/RUN-20261028-01.md): Nhánh đã merge không xóa được (L-092): `claude/T-046-don-sau-ra-soat`, `claude/T-047-rewarded-treo`; người xóa tay.
- [RUN-20261026-06](runs/RUN-20261026-06.md): Ảnh chuẩn screenshot mới: T-041 `core/ads/src/test/screenshots/NativeFlowStep_full-page-compact_light.png`, `NativeFlowStep_full-page-placeholder_light.png`,...
- [RUN-20261026-06](runs/RUN-20261026-06.md): Tiêu chí kiểm tra thủ công (máy thật): T-041, bản staging, màn xem thử, "Flow steps", Next tới trang toàn trang, xoay ngang rồi dọc: thấy khung chờ rồi quảng...
- [RUN-20261026-06](runs/RUN-20261026-06.md): Test cũ viết lại (ADR-0031): T-041, `AdGalleryScreenTest.flowSteps_cardOutsidePager_followsStep`: câu "thẻ nằm dưới pager" thành ba câu (đáy khung thẻ bằng đáy...
- [RUN-20261026-06](runs/RUN-20261026-06.md): Điểm rà soát mức ghi chú: T-041: tham số `safeArea` của `NativeFullScreenFrame` không còn nơi gọi với `false`, KDoc còn nhắc trang của bước; ở 400 dp media 124...
- [RUN-20261026-06](runs/RUN-20261026-06.md): Nhánh đã merge mà script không xóa được (L-092): `claude/T-041-trang-toan-trang`, `claude/T-045-cta-chu-lon`; người xóa tay.
- [RUN-20261026-05](runs/RUN-20261026-05.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-043, tắt `show_resum_ads` trên Firebase staging đúng lúc form đồng ý đang mở giữa lúc quay lại và lúc Activity...
- [RUN-20261026-05](runs/RUN-20261026-05.md): Kịch bản emulator lỗi hay bỏ qua: `T-024-mo-app-tu-choi-dong-y.txt` lỗi ở bước 3 (`wait id=sample_screen 60`): chạm "Do not consent" vẫn ra `GRANTED`, app open...
- [RUN-20261026-05](runs/RUN-20261026-05.md): Điểm rà soát mức ghi chú: T-043, đối chứng của `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` không kiểm `adFormat`. T-042, điểm nên sửa vòng 3 không...
- [RUN-20261026-05](runs/RUN-20261026-05.md): Xóa tay các nhánh đã merge mà `merge-if-green.sh` không xóa được (L-092): `claude/T-043-resume-kiem-vi-tri`, `claude/T-042-rewarded-bam-dup`,...
- [RUN-20261026-04](runs/RUN-20261026-04.md): Ảnh chuẩn screenshot mới: T-026 `core/ads/src/test/screenshots/NativeFullScreenOverlay_countdown_light.png`, `_countdown_dark`, `_loading_light`,...
- [RUN-20261026-04](runs/RUN-20261026-04.md): Ảnh so sánh với thiết kế: ads, `docs/features/ads/design/review/AdGalleryScreen_collapsible-expanded_light_compare.png`, `_collapsed_light_compare.png` (T-030,...
- [RUN-20261026-04](runs/RUN-20261026-04.md): Tiêu chí kiểm tra thủ công (máy thật): T-026 combo native sau inter với quảng cáo thật, dựng lại activity khi lớp phủ mở; T-030 chạm `NativeAdView` trong lớp...
- [RUN-20261026-04](runs/RUN-20261026-04.md): `scripts/release.sh` chưa truyền `-PappsFlyerDevKey` (T-033, câu hỏi 2 của plan): bản production kế tiếp dừng build tới khi người sửa script.
- [RUN-20261026-04](runs/RUN-20261026-04.md): Luật rewarded khi cổng đồng ý đóng (T-031, câu hỏi 8 phương án (a)) chưa có trong `docs/ops/ads-business-rules.md` (MC-05 hay R-03).
- [RUN-20261026-04](runs/RUN-20261026-04.md): Kịch bản emulator lỗi hay bỏ qua: chưa chạy (`device-session` chưa khởi động vì phiên dừng trước phần cuối).
- [RUN-20261026-04](runs/RUN-20261026-04.md): Phần ngoài mục 3.2 đã merge (ADR-0029): T-026 `NativeAdCard.kt` (`placeholderAlpha` thành `internal`, PR #187); T-030 `NativeAdCardTest.kt` (giữ khẳng định CTA...
- [RUN-20261026-04](runs/RUN-20261026-04.md): Điểm rà soát nên sửa không sửa: T-026 hai test qua ngay (`standalone_noAd_*` không đi qua `skipTurn(NOT_READY)`, dòng `nativeTurns.busy ||` chưa có test);...
- [RUN-20261026-04](runs/RUN-20261026-04.md): Điểm rà soát mức ghi chú: xem nhật ký từng plan; đáng chú ý: T-032 trang toàn trang mất quảng cáo khi xoay hay gập máy (đề xuất ticket); T-026 sau trần 10 phút...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Ảnh chuẩn screenshot mới và ghi lại: T-027, 20 ảnh (`core/ads`: `NativeAdCard_small_*`, `NativeAdCard_full_*`, `NativeAdPlaceholder_small_light`,...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Ảnh so sánh với thiết kế: `docs/features/ads/design/review/` (T-027: `SampleScreen_*` 0.9 tới 1.3%, `NativeFullScreenAdScreen_*` 0.5%,...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Tiêu chí kiểm tra thủ công (máy thật, quảng cáo thật): T-027, native toàn màn hình có video bắt đầu tắt tiếng, không có thanh điều khiển riêng; native nhỏ ở...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Điểm rà soát chưa sửa: T-027, nên sửa: `full_placeholderHeight_equalsCardHeight_shortAndLongTexts` không đỏ được vì thẻ `FULL` bị ép chiều cao; nên kiểm đáy...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Điểm rà soát mức ghi chú: T-027: cỡ chữ hệ thống lớn đẩy hàng sao, CTA ra khỏi thẻ `FULL` 418 dp; native thu gọn chuyển sang media ngang (O-06);...
- [RUN-20261026-03](runs/RUN-20261026-03.md): Lựa chọn của AI ở chỗ plan không nói rõ: T-027, chiều cao thẻ `FULL` 418 dp; T-040, ba lựa chọn (nhật ký plan, Lệch so với plan); T-028, vị trí chưa biết công...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Tiêu chí kiểm tra thủ công (máy thật, người): T-024, mục 4 của plan, dòng "KB-13, tiêu chí 1 đến 4 trên máy". Năm bước trên bản staging, sau khi đồng ý quảng...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Kịch bản emulator lỗi hay bỏ qua: `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt` lỗi ở bước 3 `wait id=sample_screen 60`. Trên emulator, từ chối...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Điểm rà soát mức nên sửa, chưa sửa (T-024): người dùng rời app (Home) trong lúc màn loading chờ quảng cáo thì vòng chờ vẫn chạy, và quảng cáo hiện khi họ quay...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Điểm rà soát mức ghi chú (T-024): (6 dòng con)
- [RUN-20261026-02](runs/RUN-20261026-02.md): Lệch so với plan (T-024, đã ghi trong nhật ký và PR): probe đọc `consentOpen` từ trạng thái cổng đồng ý thay `canRequestAds`; vị trí đang chọn giữ trong...
- [RUN-20261026-02](runs/RUN-20261026-02.md): Bản phát hành sau cần mã đơn vị thật cho `open_app`, `open_app_inter` (`release.properties`, việc của người).
- [RUN-20261026-01](runs/RUN-20261026-01.md): Tiêu chí kiểm tra thủ công (máy thật), T-038, không bắt buộc: mở form quyền riêng tư, từ chối rồi đồng ý lại khi mạng vừa về; log `VGL.Ads` có...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Tiêu chí kiểm tra thủ công (máy thật), T-029: chưa kiểm được, chưa có vị trí banner (câu hỏi 1 của plan, T-036). Khi có: banner cố định tự làm mới, banner thu...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Điểm rà soát mức ghi chú, T-038: (1) `runner.run` ném lỗi thì lượt giữ nay mất cùng lượt chạy lỗi (trước đây còn lại), liên quan ghi chú (2) của rà soát T-037;...
- [RUN-20261026-01](runs/RUN-20261026-01.md): Điểm rà soát mức ghi chú, T-029: (1) `hadAd` mất khi chiều rộng đổi, và không theo khóa mới khi đổi vị trí (khe có thể giữ khung trống, rất hiếm); (2) bước bỏ...
- [RUN-20261026-01](runs/RUN-20261026-01.md): T-024: duyệt plan phiên bản 3 (câu hỏi 7: ba điểm phiên đã tự quyết) và danh sách chạy nháp `RUN-20261026-02`. Ghi chú từ rà soát trước phiên:...
- [RUN-20261025-02](runs/RUN-20261025-02.md): Tiêu chí kiểm tra thủ công: T-037, không bắt buộc. Mở app lần đầu sau khi cài, xem `adb logcat -v threadtime | grep ' VGL\.Ads'`: nếu có...
- [RUN-20261025-02](runs/RUN-20261025-02.md): Điểm rà soát mức ghi chú: T-037, (1) `AdPreloader.request` ra `RUN` khi đang có lượt giữ vì cổng đóng (cổng mở, `request` tới trước collector cổng) không xóa...
- [RUN-20261025-01](runs/RUN-20261025-01.md): Tiêu chí kiểm tra thủ công: T-021, plan mục 4 dòng "Trên máy (staging)", năm bước với chế độ máy bay: thử lại toàn màn hình 2 tới 10 giây, native thử lại mỗi 5...
- [RUN-20261025-01](runs/RUN-20261025-01.md): Điểm rà soát mức nên sửa (T-021, không sửa vì phải đổi bảng quyết định S-23 của plan): lượt `PLACEMENT_ENABLED`, `NETWORK_BACK`, `GATE_REOPENED` tới khi kế...
- [RUN-20261025-01](runs/RUN-20261025-01.md): Điểm rà soát mức ghi chú (T-021): interstitial thử lại mỗi 10 giây không ngừng, kể cả ở nền, khi có mạng mà không có quảng cáo (đúng F-05 và plan; khoảng 360...
- [RUN-20261025-01](runs/RUN-20261025-01.md): GitHub Actions: lần chạy `device-session` đầu bị bỏ vì thanh toán hay hạn mức chi tiêu (16:00); anh đã tăng ngân sách, các lần chạy sau đều chạy được. Nên xem...
- [RUN-20261024-03](runs/RUN-20261024-03.md): Quyền đẩy nhánh của phiên con (T-021, L-070): phiên con bị chặn `git push` với lý do "Unrequested Commit in a Connected App", trong khi phiên con của T-034,...
- [RUN-20261024-03](runs/RUN-20261024-03.md): Tiêu chí kiểm tra thủ công: T-034 kiểm thủ công 1 đến 3 trên bản staging (log có 10 dòng `Adapter ...`, chín adapter `NOT_READY` vì thiếu mã mạng; không mạng...
- [RUN-20261024-03](runs/RUN-20261024-03.md): Điểm rà soát mức ghi chú: T-034: mẫu khóa AppLovin của `secret-scan.py` chỉ áp dòng có chữ `applovin`; `hasNoAppLovinSdkKey` chỉ kiểm meta-data; ADR-0026 mục 8...
- [RUN-20261024-02](runs/RUN-20261024-02.md): Tiêu chí kiểm tra thủ công: (4 dòng con)
- [RUN-20261024-02](runs/RUN-20261024-02.md): Điểm rà soát mức ghi chú: (4 dòng con)
- [RUN-20261024-01](runs/RUN-20261024-01.md): Ảnh chuẩn screenshot mới: màn hình xem thử quảng cáo, `app/src/test/screenshots/AdGalleryScreen_*.png` (8 ảnh: `default_light`, `default_dark`, `inlist_light`,...
- [RUN-20261024-01](runs/RUN-20261024-01.md): Ảnh so sánh với thiết kế: `ads`, `docs/features/ads/design/review/AdGalleryScreen_*_compare.png` (lệch 1.9%, 1.9%, 1.6%, 0.7%, 0.7%). Rà soát độc lập: chữ, thứ...
- [RUN-20261024-01](runs/RUN-20261024-01.md): Tiêu chí kiểm tra thủ công: T-014, plan mục 4 dòng cuối. Cài bản staging, thấy biểu tượng "Ad gallery"; đồng ý ở hộp thoại; trang mặc định có small và medium...
- [RUN-20261024-01](runs/RUN-20261024-01.md): Điểm rà soát mức ghi chú: T-014, `SdkAdGallerySlots` giữ `InListAds` trong biến thường gán lúc dựng giao diện (chấp nhận được, như RUN-20261023-01).
- [RUN-20261023-01](runs/RUN-20261023-01.md): Ảnh chuẩn screenshot mới (đã merge): (5 dòng con)
- [RUN-20261023-01](runs/RUN-20261023-01.md): Ảnh so sánh với thiết kế: (2 dòng con)
- [RUN-20261023-01](runs/RUN-20261023-01.md): Tiêu chí kiểm tra thủ công (chi tiết ở mục "Cần người xem" của nhật ký từng plan): (8 dòng con)
- [RUN-20261023-01](runs/RUN-20261023-01.md): Điểm cần người quyết: (2 dòng con)
- [RUN-20261023-01](runs/RUN-20261023-01.md): Điểm rà soát mức ghi chú: xem dòng "Rà soát độc lập" trong nhật ký từng plan. Đáng chú ý: KDoc trong `AdsSettings.kt` còn ghi "not read yet" cho `ENABLED`,...
- [RUN-20261023-01](runs/RUN-20261023-01.md): PR của người khác mở trong lúc phiên chạy (phiên không đụng tới): #107, #108, #109 ("Từ camera-pose: ..."). #109 dùng mã L-061; báo cáo này thêm L-060 đến...

### Ticket chưa lên staging

11 ticket.
- ads: T-057 `backlog`, T-061 `backlog`, T-064 `backlog`, T-065 `backlog`, T-069 `backlog`, T-070 `backlog`, T-072 `backlog`, T-073 `backlog`, T-090 `backlog`, T-091 `backlog`, T-092 `backlog`

### Phản hồi đang mở

- FB-20261023-02 `triaged`, mức `nhe`: Native sample_native_small tải thất bại với mã 0 trên emulator của CI
- FB-20261024-02 `triaged`, mức `nang`: Log trạng thái adapter trên emulator chỉ có AppLovin, thiếu tám mạng của T-034

### Issue đang mở

- [Mọi issue đang mở](https://github.com/VGL-Studio/app-project-template/issues?q=is%3Aissue+is%3Aopen), [issue main-broken](https://github.com/VGL-Studio/app-project-template/issues?q=is%3Aissue+is%3Aopen+label%3Amain-broken)
