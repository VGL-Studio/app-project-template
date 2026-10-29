---
date: 2026-10-29
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: ĐƯA CÁCH LÀM TỐT CỦA JBASE VÀO QUẢNG CÁO CỦA DỰ ÁN MẪU (ĐỢT 4)

Ghi lại từ trao đổi với AI ngày 2026-10-29. AI đọc gói JBase (Unity Android, bản 1.0.0, của nhóm JGames) và đối chiếu với `:core:ads` của dự án mẫu. Hai bên cùng một gốc: hằng số và tên sự kiện của JBase trùng với AdsKit PARTNER. Phần lớn cơ chế của dự án mẫu đã chặt chẽ hơn; đợt này lấy những gì JBase có mà ta chưa có, và sửa những chỗ hở AI thấy khi rà lại `:core:ads`.

## Mình muốn gì

Áp dụng mọi đề xuất của bản đối chiếu, sau khi AI phân tích lại một lần:

- ~~Interstitial đã tải có hạn dùng, như app open và rewarded.~~ Bỏ: anh Nhật chọn lấy nghiệp vụ JBase làm chuẩn, JBase không có hạn dùng cho interstitial.
- Quảng cáo trong màn hình (banner, native làm mới) dừng làm mới khi một quảng cáo toàn màn hình vẽ trong app đang che màn hình (JBase dừng tự làm mới khi có quảng cáo toàn màn hình).
- Quảng cáo toàn màn hình chỉ gọi hiện khi màn hình của app còn dùng được; xoay máy không làm hành động của người dùng chạy trước khi quảng cáo đóng.
- Bản production có mã quảng cáo cho mọi lớp của thanh đáy (banner, native thu gọn), theo câu hỏi 14 đã chốt (mã theo khe).
- Bản staging thử được mediation trên máy thật: khai báo máy thử, ghi kết quả từng mạng của mỗi lượt tải.
- Native nhỏ trong danh sách thử lại khi tải lỗi, như mọi native khác.
- Dọn native: hàng sao đăng ký với SDK, chữ không bị cắt ở cỡ chữ lớn, chú thích cũ.
- Thanh đáy chừa chỗ ngay lúc đang tải, nội dung không bị đẩy khi quảng cáo về (anh Nhật chọn, đổi luật D-04).
- Thêm định dạng banner MREC 300 x 250 (anh Nhật chọn, đổi KB-2).
- Một cổng "không hiện quảng cáo" mà app nối vào khi người dùng đã mua gói bỏ quảng cáo; chưa làm phần mua hàng (anh Nhật chọn).
- Thêm AppLovin MAX và Amazon APS (anh Nhật chọn).

## Thêm 2026-10-29: chuẩn để ra quyết định

Anh Nhật: lấy nghiệp vụ của JBase làm tiêu chuẩn để ra quyết định, áp cho cả luật đã chốt ở `docs/ops/ads-business-rules.md`, không chỉ câu hỏi đang mở. Hai lựa chọn riêng của anh Nhật đứng trên chuẩn này: thanh đáy chừa chỗ lúc tải, cổng không hiện quảng cáo. Chỗ JBase trái luật hay chính sách (đồng ý viết cứng, chế độ thử của APS ở bản phát hành, khóa thật trong repo) là lỗi, không phải nghiệp vụ, nên không theo.

## Vì sao

Mỗi chỗ hở làm mất doanh thu hay có rủi ro chính sách: interstitial cũ hơn một giờ có thể không có doanh thu; banner làm mới dưới một màn che là lượt hiển thị không ai thấy; thanh đáy không có mã ở production thì không có quảng cáo. MREC, MAX và APS là thêm nguồn quảng cáo khi native và AdMob ít quảng cáo.

## Người dùng sẽ trải nghiệm thế nào

Người dùng app gần như không thấy khác: nội dung không nhảy khi thanh đáy có quảng cáo, không có quảng cáo hiện sai lúc, người đã mua gói bỏ quảng cáo không thấy quảng cáo nào. Người vận hành thấy nhiều quảng cáo được lấp hơn và kiểm được mediation trên staging.

## Những gì chưa cần làm lần này

- Mua hàng trong app (Play Billing), ghi doanh thu mua hàng, kiểm hóa đơn: để một spec riêng khi có app cần.
- Cặp mã giá sàn cao, thấp trong code.
- Chép code của JBase: không có mã nguồn, chỉ có `.aar` đã biên dịch; ta tự viết lại như với AdsKit.

## Quảng cáo, dữ liệu, file tĩnh

Có: định dạng mới `banner_mrec`; SDK mới AppLovin MAX và Amazon APS; sự kiện doanh thu của MAX. Chi tiết và câu hỏi ở spec `ads`, câu hỏi 35 trở đi.

## Tham khảo

- Gói JBase: `jbase-unity-v1.0.0` (không vào repo; chứa khóa thật trong `strings.xml`).
- Bản đối chiếu trong phiên làm việc ngày 2026-10-29.
