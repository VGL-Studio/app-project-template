---
id: T-009
title: Công tắc tổng, ân hạn phiên đầu, giới hạn mỗi phiên, khoảng cách lưu qua phiên
feature: ads
spec: features/ads/spec.md
scenarios: [KB-6, KB-7]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: [T-007]
sprint:
plan: features/ads/plans/T-009-tan-suat-va-cong-tac-tong.md
created: 2026-10-21
---

# T-009: CÔNG TẮC TỔNG, ÂN HẠN PHIÊN ĐẦU, GIỚI HẠN MỖI PHIÊN, KHOẢNG CÁCH LƯU QUA PHIÊN

## Mô tả

Quảng cáo toàn màn hình tôn trọng khoảng cách tối thiểu tính qua các lần mở app (lưu trên máy), ân hạn sau lần mở đầu tiên và số tối đa mỗi phiên. Công tắc tổng `ads_enabled` tắt mọi quảng cáo ở mọi định dạng. Áp dụng cho interstitial hiện có; native toàn màn hình (T-013) dùng chung.

## Phạm vi

- Lưu thời điểm quảng cáo toàn màn hình gần nhất và thời điểm mở app lần đầu (DataStore của `:core:data`).
- Đếm theo phiên; quyết định hiển thị là hàm thuần có unit test.
- Công tắc tổng chặn tải và hiển thị ở `AdSlot`, interstitial và mọi composable native về sau.

**Không bao gồm:**

- Native toàn màn hình (T-013).

## Rủi ro sơ bộ

Module core `:core:ads`, dữ liệu lưu trên máy. Sai có thể làm quảng cáo hiện dày hơn hay không hiện: unit test cho mọi nhánh của quyết định.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
