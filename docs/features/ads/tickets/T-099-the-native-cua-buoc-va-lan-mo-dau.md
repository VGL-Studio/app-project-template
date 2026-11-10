---
id: T-099
title: Feature biết thẻ native của một bước có được phép không; vị trí chỉ hiện ở lần mở app đầu tiên
feature: ads
spec: features/ads/spec.md
scenarios: [KB-20]
owner: Nhật
status: planned              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quảng cáo và :core:ads (điểm chặn 1, 7)
size: M
depends_on: [T-098]          # cùng sửa docs/ops/ads-guide.md, ads-business-rules.md với T-097, T-098: chạy nối tiếp
sprint:
plan: features/ads/plans/T-099-the-native-cua-buoc-va-lan-mo-dau.md
auto_run:
created: 2026-11-09
---

# T-099: FEATURE BIẾT THẺ NATIVE CỦA MỘT BƯỚC CÓ ĐƯỢC PHÉP KHÔNG; VỊ TRÍ CHỈ HIỆN Ở LẦN MỞ APP ĐẦU TIÊN

## Mô tả

Nguồn: issue #381 phần `:core:ads` (camera-pose, PR #296 của app), bài học L-163. Luồng giới thiệu cần biết thẻ native của bước có được phép không (cổng đồng ý, công tắc chính, gói bỏ quảng cáo, khóa vị trí) để quyết có giữ nút chờ hay không; khung chỉ biết điều này bên trong `NativeStepAd`. Vị trí chỉ hiện ở lần mở app đầu tiên chưa khai báo được.

## Phạm vi

- `rememberNativeStepAdAllowed(placement)`: trạng thái "được phép" của thẻ bước, dùng đúng cổng mà `NativeStepAd` dùng (một hàm chung, test mỗi đường gọi nó).
- Khai báo vị trí có `firstOpenOnly`: công tắc chỉ bật ở lần mở app đầu tiên; dấu lần mở đầu đọc một lần mỗi tiến trình. Spec chưa có tiêu chí cho điều này (chỉ `show_open_ads_first_open` của app open, KB-13): plan đề xuất tiêu chí cho spec (KB-20 hay kịch bản mới) ở mục 8, và plan chỉ được duyệt sau khi người phụ trách sản phẩm xác nhận spec đã sửa.
- Luật mới ghi vào `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`; plan đặt câu hỏi cho người phụ trách sản phẩm.
- Test JVM, có test cổng mở rồi đóng lại (`docs/CLAUDE.md`, Khi viết plan).

**Không bao gồm:**

- Bộ lọc `enabled=true` của `scripts/device-scenario.py` và ghi chú về "Do not consent" (L-155, làm riêng).

## Rủi ro sơ bộ

Đổi điều kiện hiện quảng cáo (điểm chặn 1) và module core (điểm chặn 7): rủi ro cao, duyệt chéo, người phụ trách sản phẩm biết trước khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
