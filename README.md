# APP PROJECT TEMPLATE

Dự án mẫu cho các app Android của nhóm. Mọi app mới được tạo từ dự án này, theo quy trình phát triển lấy AI làm lõi.

## Bắt đầu

1. Mở thư mục dự án bằng Android Studio (JDK 17).
2. Chạy app với biến thể `stagingDebug`.
3. Kiểm tra toàn bộ, giống hệt CI: `scripts/check.sh`.

## Tài liệu

| Muốn biết | Đọc |
| --- | --- |
| Quy trình làm việc, cấu trúc tài liệu | [docs/README.md](docs/README.md) |
| Phiên AI tự chạy | [docs/auto-run.md](docs/auto-run.md) |
| Thang điểm rủi ro, điều kiện dừng | [docs/risk-scoring.md](docs/risk-scoring.md) |
| Các quyết định kỹ thuật | [docs/adr/](docs/adr/) |
| Hướng dẫn cho AI về code | [CLAUDE.md](CLAUDE.md) |
| Hướng dẫn cho AI về tài liệu | [docs/CLAUDE.md](docs/CLAUDE.md) |
| Phần còn phải làm của dự án mẫu | [TEMPLATE-ROADMAP.md](TEMPLATE-ROADMAP.md) |

## Merge

Mọi PR vào `main` chỉ được merge bằng `scripts/merge-if-green.sh <số PR>` (ADR-0010).
