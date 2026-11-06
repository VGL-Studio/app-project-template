---
date: 2026-11-06
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: QUẢNG CÁO TRONG MÀN HÌNH KHÔNG CHỒNG LÊN NHAU

Ghi lại từ trao đổi với AI ngày 2026-11-06.

## Mình muốn gì

"Không có các ads hiện chồng lên nhau (banner/native small, collapsible)."

## Hiện trạng lúc ghi

- Lớp native thu gọn của thanh đáy (cao 250 dp khi mở) nổi đè lên khe đáy (native nhỏ); khe đáy bên dưới vẫn nạp và làm mới dù bị che (luật D-01, theo AdsKit PARTNER).
- Lớp nổi không chừa chỗ, nên khi mở nó đè cả lên nội dung phía trên khe đáy, kể cả quảng cáo trong nội dung nếu có.
- Chưa có luật cấm một màn vừa có thanh đáy vừa có banner thu gọn hay banner cố định ở đáy.

## Anh Nhật chọn (2026-11-06)

- **Khe đáy chờ:** khi lớp native thu gọn đang mở thì khe đáy không vẽ quảng cáo, không nạp, không làm mới; người dùng thu gọn lớp nổi thì khe đáy mới hiện và nạp.
- **Lớp nổi mở lại** (bung lại sau `time_expand_collap_ad` giây): khe đáy hủy quảng cáo đang có; lần thu gọn sau thì nạp quảng cáo mới.
- **Quảng cáo trong nội dung và lớp nổi không cùng màn.** Khi cả hai được khai báo hay bật trên một màn thì quảng cáo trong nội dung thắng: thanh đáy chỉ còn khe đáy, banner thu gọn không nạp.
- **Mỗi màn tối đa một quảng cáo neo đáy:** thanh đáy, hoặc banner cố định, hoặc banner thu gọn. Màn đặt hai cái thì cái đăng ký trước chạy; cái sau không nạp, không chiếm chỗ, ghi log cảnh báo.
- **Mức áp dụng:** ghi luật vào spec và `ads-business-rules.md`; `:core:ads` tự chặn khi chạy, có test.

## Vì sao

Quảng cáo bị che mà vẫn tính lượt hiển thị có thể bị AdMob coi là xếp chồng quảng cáo; hai quảng cáo đè nhau dễ gây bấm nhầm.
