---
id: T-065
title: Code kích thước native theo JBase
feature: ads
spec: features/ads/spec.md
scenarios: [KB-1, KB-2, KB-4, KB-17, KB-32]
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-064]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-065: CODE KÍCH THƯỚC NATIVE THEO JBASE

## Mô tả

Đổi kích thước native nhỏ ở đáy (theo bề rộng màn hình, 90 tới 200 dp), native thu gọn và native medium trong `NativeAdCard` và thanh đáy theo thiết kế T-064 (ADR-0038, quyết định 3).

## Phạm vi

- `NativeAdDimens` và cách tính chiều cao theo bề rộng; một hằng số hay một hàm cho cả vẽ và chừa chỗ (câu hỏi 22, 49).
- Viết lại câu khẳng định cũ của D-02 (80 dp) theo ADR-0031; ghi lại ảnh chuẩn bị đổi (liệt kê trong plan).
- Cỡ chữ 200%: kiểm lại theo tiêu chí của KB-1 (T-060 làm trên kích thước cũ; plan thống nhất thứ tự với T-060).
- Giới hạn 120 dp của nút CTA ở native nhỏ (T-060): bỏ hay giữ theo layout JBase; bỏ thì viết lại hai test 120 dp của T-060 (`small_ctaAtMost120dpWide_shortCtaKeepsWidth`, `sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible`) theo ADR-0031, vẫn giữ kiểm tiêu đề còn ít nhất 4 ký tự và nhãn "Ad" hiện đủ (rà soát plan T-060).
- Sửa `ads-business-rules.md` mục 5.7, D-02, D-04, mục 11 kèm ADR-0038.

**Không bao gồm:**

- Biến thể JBase không có (bước của luồng, thẻ trong tập hợp, native đầy đủ).

## Rủi ro sơ bộ

Module core, giao diện quảng cáo, nhiều ảnh chuẩn screenshot.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
