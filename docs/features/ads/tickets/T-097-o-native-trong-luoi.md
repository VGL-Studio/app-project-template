---
id: T-097
title: Ô native trong lưới bỏ ô khi hết quảng cáo, nhận chiều cao ô và thẻ, vẽ được ở màn không trạng thái
feature: ads
spec: features/ads/spec.md
scenarios: [KB-3, KB-21]     # "khi không còn quảng cáo thì ô không hiện": phần pool cạn là lỗi so với spec
owner: Nhật
status: planned              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # quảng cáo và :core:ads (điểm chặn 1, 7)
size: L                      # bản ở app chạm 6 tệp nguồn, 13 test; plan cân nhắc tách
depends_on: []
sprint:
plan: features/ads/plans/T-097-o-native-trong-luoi.md
auto_run:
created: 2026-11-09
---

# T-097: Ô NATIVE TRONG LƯỚI BỎ Ô KHI HẾT QUẢNG CÁO, NHẬN CHIỀU CAO Ô VÀ THẺ, VẼ ĐƯỢC Ở MÀN KHÔNG TRẠNG THÁI

## Mô tả

Nguồn: issue #379 (camera-pose, PR #287 của app), bài học L-161. App có lưới nội dung với ô native một cột gặp ba chỗ khung chưa làm được: pool cạn thì ô vẫn chiếm chỗ, để lại lỗ trong lưới; ô không cao bằng thẻ bên cạnh; màn không trạng thái (screenshot test, preview) không vẽ được ô ở đúng chỗ như lưới thật.

## Phạm vi

- `:core:ads`, `InListAds`: ô một cột tự bỏ khi pool cạn và quay lại khi pool có quảng cáo hay khóa vị trí bật lại; hàng toàn chiều rộng và danh sách giữ hành vi cũ.
- Tham số chiều cao ô và chiều cao thẻ cho `NativeAdCard`, `NativeAdCell`, `NativeAdSlot` (mặc định giữ ảnh chuẩn cũ).
- `itemsWithFixedAds`: đặt ô ở cùng chỗ như lưới thật, cho màn không trạng thái và screenshot test.
- Luật mới hay đổi ghi vào `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`; plan đặt câu hỏi cho người phụ trách sản phẩm khi luật hiển thị đổi.
- Test JVM cho từng phần, có đột biến đối chứng.

**Không bao gồm:**

- Vị trí quảng cáo mới.

## Rủi ro sơ bộ

Đổi cách hiện quảng cáo trong danh sách (điểm chặn 1) và module core (điểm chặn 7): rủi ro cao, duyệt chéo, người phụ trách sản phẩm biết trước khi duyệt.

## Định nghĩa hoàn thành

- [ ] Mọi điểm code của phạm vi có test tự động, test viết trước và đã thất bại đúng lý do; phần chỉ là tài liệu không cần test.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Bài học liên quan ghi `applied` với số PR; issue `from-app` đóng kèm số PR sau khi dev đồng ý.
