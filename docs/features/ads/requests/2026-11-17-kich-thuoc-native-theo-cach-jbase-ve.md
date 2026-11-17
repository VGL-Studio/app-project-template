---
date: 2026-11-17
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: KÍCH THƯỚC NATIVE THEO ĐÚNG CÁCH JBASE VẼ

Ghi lại từ trao đổi với AI ngày 2026-11-17.

## Mình muốn gì

Anh Nhật thấy native ở khe đáy cao hơn bản trước. AI đọc lại mã của gói JBase và thấy JBase không vẽ thẻ native cao 90 tới 200 dp: con số đó chỉ là chiều cao mà game hỏi để tự chừa chỗ. Thẻ quảng cáo JBase vẽ ra cao theo nội dung. Anh Nhật: "Sửa theo JBase."

Bốn lựa chọn anh Nhật chọn trong phiên:

- Native nhỏ ở đáy: bố cục hai tầng như JBase (hàng icon và chữ, dưới là nút ngang hết bề rộng), cao theo nội dung, khoảng 106 dp ở cỡ chữ mặc định, không đổi theo bề rộng màn hình.
- Chỗ chừa ở đáy màn hình bằng đúng chiều cao thẻ, không chừa thêm khoảng trống như game JBase.
- Lớp thu gọn khi mở: tổng chiều cao tính như JBase, ảnh lấy phần còn lại sau phần nội dung.
- Native medium: 250 dp như JBase; trường hợp ảnh còn dưới 120 dp thì thẻ cao thêm để giữ ảnh 120 dp.

## Vì sao

Mẫu lấy JBase làm chuẩn cho kích thước ba biến thể native, nhưng bản đối chiếu trước đọc nhầm chiều cao để chừa chỗ thành chiều cao vẽ. Thẻ ở khe đáy hiện cao hơn JBase khoảng 20 dp ở điện thoại thường và tới gần gấp đôi ở máy rộng; medium cao hơn JBase 70 tới 150 dp. Quảng cáo chiếm nhiều chỗ của nội dung hơn mức nhóm đã chọn.

## Người dùng sẽ trải nghiệm thế nào

Khe quảng cáo ở đáy thấp hơn, cùng một chiều cao trên điện thoại thường lẫn máy rộng, với nút cài đặt to ngang hết thẻ. Lớp thu gọn khi mở cao gần như hôm nay, ảnh to hơn. Native medium trong nội dung thấp hơn rõ.

## Những gì chưa cần làm lần này

- Màu và kiểu nút của JBase (ảnh nền tím, chữ trắng viết cứng): mẫu vẫn lấy màu từ token.
- Biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ, native toàn màn hình).

## Quảng cáo, dữ liệu, file tĩnh

Không thêm vị trí, sự kiện hay file tĩnh nào. Chỉ đổi kích thước và bố cục của native nhỏ, lớp thu gọn và native medium.

## Tham khảo

- Gói JBase `jbase-unity-v1.0.0` (không vào repo; chứa khóa thật trong `strings.xml`):
  - `AndroidBuild_Template/unityLibrary/src/main/res/layout/gnt_banner_shared_content.xml`: khối nội dung dùng chung, `wrap_content`, đệm trên dưới 6 dp, hàng icon 44 dp, nút 44 dp cách hàng trên 6 dp.
  - `gnt_small_template_view.xml`: native nhỏ, `wrap_content`, đệm trái phải 8 dp.
  - `gnt_collapsible_banner_template_view.xml`: ảnh trên khối nội dung, nút thu gọn 27 dp góc trên trái.
  - `gnt_medium_template_view.xml`: cao 250 dp, ảnh lấp phần còn lại, dưới là khối nội dung như native nhỏ.
  - `java/game/NativeBannerTemplate.java`, dòng 121 tới 133: thẻ đặt `MATCH_PARENT` x `WRAP_CONTENT`; dòng 567 tới 589 (`computeExpandedMediaHeightPx`): ảnh của lớp thu gọn bằng tổng (90 tới 200 dp cộng 160 tới 200 dp theo bề rộng) trừ chiều cao đo được của khối nội dung.
  - `java/game/NativePositionedTemplateBase.java`, dòng 81 tới 94: khung chứa `MATCH_PARENT` x `WRAP_CONTENT`, đặt ở đáy bằng `gravity`.
  - `java/game/NativeAdsManager.java`, dòng 497 tới 605: `getNativeBannerHeight`, `getNativeCollapsibleHeight`, `getNativeMrecHeight` chỉ trả số cho game qua `LibraryBridge` và `Assets/JBase/Scripts/Bridge/AndroidBridge.cs`, dòng 564 tới 571; không chỗ nào phía Java đặt chiều cao thẻ theo các số này.
