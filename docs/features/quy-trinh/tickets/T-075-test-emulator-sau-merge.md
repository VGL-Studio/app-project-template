---
id: T-075
title: Test emulator chạy sau khi merge, không chạy ở từng PR
feature: quy-trinh
spec:                        # ticket quy trình: yêu cầu là requests/2026-10-29-giam-phut-actions.md, không có spec nghiệp vụ
scenarios: []
owner: Nhật
status: backlog
risk: high
size: M
depends_on: [T-074]
sprint:
plan:
auto_run: 
created: 2026-10-29
---

# T-075: TEST EMULATOR CHẠY SAU KHI MERGE, KHÔNG CHẠY Ở TỪNG PR

## Mô tả

Job `ui-test` của `pr-check.yml` chạy test emulator (`app/src/androidTest`) ở mọi PR chạm code app, tốn khoảng 18% phút Actions (637 phút một tuần), và chạy lại mỗi lần đẩy hay cập nhật nhánh. Sau ticket này, test emulator chạy một lần cho mỗi commit code trên nhánh chính, không chạy ở PR (yêu cầu `requests/2026-10-29-giam-phut-actions.md`, cách 3). Đổi lại, lỗi chỉ lộ ra trên emulator bị phát hiện sau khi merge.

## Phạm vi

- Bỏ `ui-test` khỏi `pr-check.yml`; `merge-if-green.sh` không còn chờ hay đòi kết quả `ui-test` (hiện được phép bỏ qua khi PR không chạm code app).
- Chạy test emulator sau merge: trong `main-guard.yml` (job riêng, chỉ khi commit chạm đường dẫn `ui` hiện có), kể cả khi T-074 bỏ qua `check.sh`. Plan so với cách chạy mỗi đêm một lần cho commit mới nhất có đổi code.
- Test emulator đỏ trên main: xử lý như cổng chất lượng đỏ (issue `main-broken`, tìm commit gây lỗi, nhánh revert, ADR-0010, ADR-0025), hay nhãn riêng nhẹ hơn. Plan đề xuất, người chọn.
- Cập nhật ADR (ADR mới hay mục mới của ADR của T-074), `docs/auto-run.md` (bước chờ CI, phiên chờ `main-guard` sau merge), `docs/ops/failure-playbook.md` nếu đổi tên job, `CLAUDE.md` (dòng "Test trên emulator chỉ chạy trong CI").
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
