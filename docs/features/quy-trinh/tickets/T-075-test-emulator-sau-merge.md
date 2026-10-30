---
id: T-075
title: Test emulator chạy sau khi merge, không chạy ở từng PR
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-074]
sprint:
plan: features/quy-trinh/plans/T-075-emulator-sau-merge.md
auto_run: false               # làm có người: phiên tự chạy dựa vào chính cổng chất lượng mà ticket sửa (như T-051)
created: 2026-10-29
---

# T-075: TEST EMULATOR CHẠY SAU KHI MERGE, KHÔNG CHẠY Ở TỪNG PR

## Mô tả

Job `ui-test` của `pr-check.yml` chạy test emulator (`app/src/androidTest`) ở mọi PR chạm code app, tốn khoảng 18% phút Actions (637 phút một tuần), và chạy lại mỗi lần đẩy hay cập nhật nhánh. Sau ticket này, test emulator chạy một lần cho mỗi commit code trên nhánh chính, không chạy ở PR (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 3). Đổi lại, lỗi chỉ lộ ra trên emulator bị phát hiện sau khi merge.

## Phạm vi

- Bỏ `ui-test` khỏi `pr-check.yml`; `merge-if-green.sh` không còn chờ hay đòi kết quả `ui-test` (hiện được phép bỏ qua khi PR không chạm code app).
- Chạy test emulator sau merge: trong `main-guard.yml` (job riêng, chỉ khi commit chạm đường dẫn `ui` hiện có), kể cả khi T-074 bỏ qua `check.sh`. Plan so với cách chạy mỗi đêm một lần cho commit mới nhất có đổi code.
- Nêu tên job mới và cách các chỗ đọc kết quả nhìn thấy nó: `find-breaking-commit.sh`, `main-guard-status.sh` chỉ đọc check run `guard`, nên emulator đỏ mà `guard` xanh vẫn báo xanh; bước mở `main-broken` và đẩy nhánh revert nằm trong job `guard`. `scripts/release.sh` (không sửa) đọc kết luận của cả workflow `main-guard`: emulator chạy trong `main-guard` thì emulator đỏ chặn phát hành, chạy ở workflow khác thì không. Plan ghi rõ hướng chọn.
- Test emulator đỏ trên main: xử lý như cổng chất lượng đỏ (issue `main-broken`, tìm commit gây lỗi, nhánh revert, ADR-0010, ADR-0025), hay nhãn riêng nhẹ hơn. Plan đề xuất, người chọn.
- Emulator đỏ vì hạ tầng (tải R2 hết giờ chờ, emulator không khởi động) không được mở `main-broken`: một lần chập chờn sẽ chặn mọi lần merge. Viết lại các dòng PB-01, PB-10, PB-11, PB-13, PB-15 của `docs/ops/failure-playbook.md`: cách xử lý hiện nay (chạy lại job, commit rỗng lên nhánh task) không dùng được sau merge.
- Cập nhật: ADR mới hay mục mới của ADR của T-074, thay ADR-0010 mục 4 ("`main-guard` không chạy test emulator") và ý emulator của mục 1 ("Kiểm tra trên emulator được phép ở trạng thái bỏ qua"); `docs/auto-run.md` (bước 8 về `ui-test`, bước 9 chờ `main-guard`, ước tính phút ở mục 1 ý 6, dòng khoảng 21); `scripts/run-check.py` (thông báo L-045 nhắc `ui-test`); `CLAUDE.md` (dòng "Test trên emulator chỉ chạy trong CI").
- Phiên tự chạy vẫn có `device-session` cuối phiên; plan ghi hai lớp này khác nhau thế nào.

**Không bao gồm:**

- Runner tự host cho emulator: T-078.

## Rủi ro sơ bộ

Sửa `.github/workflows/pr-check.yml`, `main-guard.yml`, `scripts/merge-if-green.sh`: plan rủi ro cao, duyệt chéo, test cho từng kiểm tra của script merge (L-058). Lỗi luồng chỉ lộ ra ở emulator (L-051) sẽ vào main rồi mới bị bắt; phiên tự chạy phải dừng đúng khi việc đó xảy ra.

## Định nghĩa hoàn thành

- [ ] PR chạm code app không chạy test emulator; `merge-if-green.sh` merge được khi `build-and-test` xanh.
- [ ] Commit code trên main chạy test emulator một lần; đỏ thì xử lý đúng cách đã chọn, có test.
- [ ] Tài liệu và ADR cập nhật, `foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
