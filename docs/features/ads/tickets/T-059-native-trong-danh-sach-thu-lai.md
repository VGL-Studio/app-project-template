---
id: T-059
title: Native small trong danh sách thử lại khi lỗi
feature: ads
spec: features/ads/spec.md
scenarios: [KB-3]
owner: Nhật
status: backlog
risk: high
size: S
depends_on: []
sprint:
plan: features/ads/plans/T-059-native-trong-danh-sach-thu-lai.md
auto_run: 
created: 2026-10-29
---

# T-059: NATIVE SMALL TRONG DANH SÁCH THỬ LẠI KHI LỖI

## Mô tả

Ô native small trong danh sách hôm nay chỉ tải một lần, không thử lại, không chờ foreground (`AdsController.kt:1553-1568`), lệch luật O-05. Sau ticket này ô thử lại theo nhịp của native và chỉ gửi khi app ở foreground, cổng đồng ý mở.

## Phạm vi

- Đưa lượt tải của ô native small trong danh sách qua cùng đường thử lại của native (`retryingNativeAdStates` hay nguồn một quảng cáo), áp O-03, O-05, O-09.
- Test cho từng đường: lượt đầu, thử lại, rời màn khi yêu cầu đang bay.

**Không bao gồm:**

- Ô medium và thẻ trong tập hợp (đã dùng pool).

## Rủi ro sơ bộ

Module core, quảng cáo (điểm chặn 1 và 7): sửa lỗi so với luật đã có, không đổi luật.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
