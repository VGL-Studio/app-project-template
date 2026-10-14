---
id: ADR-0009
title: Phiên tự chạy dùng Claude Code routine trên tài khoản riêng, CI dùng GitHub Actions
status: accepted
date: 2026-10-14
deciders: [Nhóm phát triển]
---

# ADR-0009: PHIÊN TỰ CHẠY DÙNG CLAUDE CODE ROUTINE TRÊN TÀI KHOẢN RIÊNG, CI DÙNG GITHUB ACTIONS

## Bối cảnh

Phiên tự chạy (xem `docs/auto-run.md`) phải chạy được khi không có ai ngồi máy, bắt đầu vào thời điểm do dev chọn, và để lại dấu vết rõ ràng. Cần chọn nơi chạy phiên, tài khoản mà phiên dùng, và nơi chạy CI.

## Quyết định

**Nơi chạy phiên tự chạy:** Claude Code routine, chạy trên hạ tầng cloud của Anthropic.

- Dev kích hoạt bằng **Run now**, hoặc hẹn một lần chạy vào thời điểm cụ thể. Không đặt lịch lặp cố định.
- Routine dùng hạn mức của gói Claude, giống phiên làm việc thường.
- Prompt của routine yêu cầu AI thực hiện danh sách chạy đã duyệt theo `docs/auto-run.md`.

**Tài khoản riêng cho phiên tự chạy:**

- Một tài khoản Claude riêng sở hữu routine.
- Một tài khoản GitHub riêng được kết nối với tài khoản Claude đó, chỉ có quyền ghi (write) vào repo, không có quyền quản trị (admin, maintain).
- Mọi commit, PR, lần merge do AI làm đều mang tên tài khoản này, tách bạch với việc của người.

**Môi trường cloud của routine:**

- Mức mạng **Custom**: giữ danh sách tên miền mặc định, thêm `dl.google.com` để tải Android SDK.
- Setup script cài Android SDK (command-line tools, platform, build-tools) vào môi trường. JDK 21 và Gradle có sẵn.
- Biến môi trường tăng thời gian chờ lệnh (`BASH_DEFAULT_TIMEOUT_MS`, `BASH_MAX_TIMEOUT_MS`) vì build Android có thể vượt 2 phút.
- Không đưa vào môi trường bất kỳ cấu hình production nào.

**Kiểm thử chia hai nơi:**

- Trong phiên tự chạy: unit test, test giao diện bằng Robolectric, screenshot test bằng Roborazzi. Các test này chạy trên JVM, không cần emulator.
- Trong GitHub Actions: toàn bộ kiểm tra trên, thêm test trên emulator.

**Tên nhánh:** `claude/T-NNN-<tu-khoa>`, khớp với tiền tố mặc định của routine.

**CI và phát hành bằng GitHub Actions:**

| Workflow | Khi nào chạy | Việc làm | Cấu hình |
| --- | --- | --- | --- |
| `pr-check.yml` | Mọi PR vào nhánh chính | Cổng chất lượng (xem ADR-0010) | Không cần bí mật |
| `main-guard.yml` | Mỗi lần nhánh chính thay đổi | Kiểm tra lại nhánh chính, xử lý khi đỏ (xem ADR-0010) | Không cần bí mật |
| `staging.yml` | Cuối phiên tự chạy, hoặc dev chạy tay | Build biến thể staging, phát qua Firebase App Distribution | GitHub Environment `staging` |
| `release.yml` | Chạy tay | Build bản ký, đẩy lên Google Play | GitHub Environment `production`, cần người phụ trách sản phẩm duyệt |

## Các phương án đã cân nhắc

| Phương án | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Routine trên cloud của Anthropic | Không cần máy riêng, dùng hạn mức gói Claude, kích hoạt linh hoạt | Môi trường giới hạn tài nguyên, không có emulator |
| Claude Code chạy trong GitHub Actions | Gần CI | Tính phí theo token qua API, tốn thêm phút Actions, giới hạn thời gian mỗi job |
| Máy riêng trong văn phòng | Toàn quyền về cấu hình, chạy được emulator | Phải vận hành máy, rủi ro khi máy tắt |
| Dùng tài khoản của một dev | Không tốn thêm chỗ ngồi | Lẫn việc của người và AI; nếu dev là quản trị viên thì mọi giới hạn trên repo không có tác dụng với AI |

## Hệ quả

- **Tích cực:** phiên tự chạy không phụ thuộc máy của ai; lịch sử thay đổi tách bạch giữa người và AI; cấu hình production nằm ngoài tầm với của AI.
- **Phải chấp nhận:** tốn thêm một chỗ ngồi Claude và một tài khoản GitHub; test trên emulator chỉ chạy được trong CI, nên lỗi loại này phát hiện muộn hơn.
- **Việc AI phải tuân thủ:** đặt tên nhánh theo `claude/T-NNN-<tu-khoa>`; không cố cài hay chạy emulator trong phiên; không yêu cầu thêm quyền hay bí mật cho môi trường.
