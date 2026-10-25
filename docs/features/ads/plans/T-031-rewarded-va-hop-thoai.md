---
ticket: T-031
title: Rewarded là hành động có điều kiện, kèm hộp thoại xem quảng cáo
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn (ads, core, analytics)
risk_level: high             # low | medium | high
token_estimate: 15M          # cỡ M, số gợi ý 9.5M (scripts/token-usage.py estimate, 2026-10-25); nâng vì định dạng quảng cáo mới, API công khai mới và hộp thoại có giao diện
blocking_points: [ads, core, analytics]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-031: REWARDED LÀ HÀNH ĐỘNG CÓ ĐIỀU KIỆN, KÈM HỘP THOẠI XEM QUẢNG CÁO

## 1. Tóm tắt

(nháp) `:core:ads` thêm khe rewarded trong `AdsController`, chính sách khi không có quảng cáo khai báo theo vị trí, và một composable chung `rememberRewardedAction(placement, onGranted)` mở hộp thoại xem quảng cáo. Kiểm chứng bằng unit test hàm quyết định thuần, test Robolectric của hộp thoại và screenshot test.

## 2. Hiểu yêu cầu

(nháp, viết tiếp)

## 3. Giải pháp

(nháp, viết tiếp)
