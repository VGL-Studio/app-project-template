---
id: T-046
title: Dọn quảng cáo theo ghi chú rà soát RUN-20261026-05 và RUN-20261026-06
feature: ads
spec: features/ads/spec.md
scenarios: [KB-2, KB-5, KB-14, KB-19, KB-20]
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-041, T-042, T-043, T-044, T-045]
sprint:
plan: features/ads/plans/T-046-don-sau-ra-soat-quang-cao.md
created: 2026-10-27
---

# T-046: DỌN QUẢNG CÁO THEO GHI CHÚ RÀ SOÁT RUN-20261026-05 VÀ RUN-20261026-06

## Mô tả

Gom các điểm mức ghi chú mà rà soát độc lập của T-041 đến T-045 nêu nhưng phiên không sửa (báo cáo RUN-20261026-05 và RUN-20261026-06, mục Cần người kiểm tra). Người dùng không thấy gì đổi. Code bỏ tham số không còn nơi gọi, chú thích và tài liệu nói đúng điều code làm, đối chứng của hai test đi đúng nhánh mà tên test nêu.

## Phạm vi

- **T-041:** bỏ tham số `safeArea` của `NativeFullScreenFrame` (`NativeFullScreenAdScreen.kt`), vì không còn nơi gọi với `false`, và sửa KDoc còn nhắc trang của bước.
- **T-045:** bỏ tham số `growWithText` của `NativeFullScreenAdHeader`, vì mọi nơi gọi đều truyền `true`, và sửa KDoc.
- **T-044:** `docs/ops/ads-guide.md`, bảng kích thước: thẻ `FULL` ghi "mô tả ít nhất 2 dòng", trong khi code luôn đúng 2 dòng, cao ít nhất 40 dp. Ghi lại cho đúng.
- **T-042** (`RewardedTurnsTest`):
  - Đối chứng của `doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy` đi qua nhánh R-06 thay vì nhánh nhập vào, vì cổng thừa của lượt nạp còn trong hàng. Cách sửa rà soát đề xuất: `port.switchGates.clear()` trước lần gọi thứ hai, rồi chạy lại đối chứng.
  - Sửa chú thích về cổng thừa của `doubleTapAwaitingSwitch`.
- **T-043** (`AdsControllerResumeTest`): đối chứng của `resumeOff_gateHasNoPlacement_foregroundRecordsNothing` kiểm thêm `adFormat` của cơ hội được ghi.
- **Xem lại ô AdChoices giả ở cỡ chữ 2.0:** chữ "i" của ô AdChoices giả bị cắt trong ảnh `NativeFullScreenAdScreen_font-scale-2_light.png` và `NativeAdCard_font-scale-2_light.png`. Quảng cáo thật do SDK vẽ ô này. Nếu ô giả nằm trong code của màn xem thử thì cho chữ vừa ô; nếu chỉ có trong test thì ghi lý do giữ nguyên.

**Không bao gồm:**

- Lượt quyết định treo của rewarded và `onStuck` (T-047).
- Đổi bố cục hay số đo của native. Ở mọi cỡ chữ, ảnh chuẩn screenshot giữ nguyên từng byte, trừ ảnh có ô AdChoices giả nếu plan quyết sửa ô đó.

## Rủi ro sơ bộ

Sửa code và test của module core quảng cáo (điểm chặn 1). Không đổi hành vi: bỏ tham số không còn nơi gọi, sửa chú thích, làm chặt đối chứng. Rủi ro cao theo luật, việc nhỏ.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
