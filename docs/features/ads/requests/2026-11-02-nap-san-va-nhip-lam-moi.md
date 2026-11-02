---
date: 2026-11-02
author: Nhật
feature: ads
status: specified            # new | specified | done | dropped. AI ghi specified khi đã có spec; người ghi done hay dropped
---

# YÊU CẦU: NẠP SẴN MỌI NATIVE VÀ NHỊP LÀM MỚI CHỈNH TỪ XA

Ghi lại từ trao đổi với AI ngày 2026-11-02.

## Mình muốn gì

- Mọi loại quảng cáo đều được nạp sẵn. Hôm nay native nhỏ ở khe đáy và lớp native thu gọn chưa được nạp sẵn: màn vào rồi mới gửi yêu cầu, nên thanh đáy trống lúc đầu.
- Thời gian thay quảng cáo mới (ví dụ ở thanh đáy) chỉnh được qua Remote Config, thay các số đang viết cứng trong code (native 60 giây, banner thu gọn 30 giây) và cài đặt tự làm mới trên đơn vị AdMob, MAX.

## Anh Nhật chọn (2026-11-02)

- Banner cố định và banner MREC: tắt tự làm mới trên đơn vị AdMob và MAX, `:core:ads` tự nạp lại theo khóa Remote Config, như banner thu gọn. Mọi lớp dùng một cơ chế.
- Khóa Remote Config: một khóa cho mỗi lớp quảng cáo, không theo từng vị trí.
- Chỉ nạp sẵn native. Banner (cố định, thu gọn, MREC) vẫn tải khi màn vào: banner của AdMob được tính lượt hiển thị khi tải xong, nạp sẵn sẽ sinh lượt hiển thị không ai thấy.

## Vì sao

Thanh đáy trống vài giây mỗi lần vào màn là mất lượt hiển thị. Nhịp làm mới là đòn bẩy doanh thu mà nhóm vận hành cần chỉnh theo từng app, từng giai đoạn, không phải chờ phát hành bản mới hay vào console của từng mạng.

## Người dùng sẽ trải nghiệm thế nào

Thanh đáy có quảng cáo ngay khi vào màn đầu tiên có thanh đáy (khi nạp sẵn đã xong). Người vận hành đổi nhịp làm mới trên Firebase, app áp dụng ở lần đọc Remote Config sau.

## Quảng cáo, dữ liệu, file tĩnh

Không thêm vị trí, định dạng, SDK hay sự kiện. Thêm khóa Remote Config (spec `ads`, KB-33, KB-34, câu hỏi 52 tới 54).
