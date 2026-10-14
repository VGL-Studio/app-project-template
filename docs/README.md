# BỘ TÀI LIỆU DỰ ÁN

Thư mục `docs/` chứa mọi thứ mà người và AI cần để làm việc với app: yêu cầu, spec, thiết kế, ticket, plan, phản hồi, quyết định kỹ thuật và báo cáo. Tài liệu nằm cùng repo với code, cùng được quản lý phiên bản, và AI luôn đọc tài liệu trước khi code. Nhóm không dùng Confluence, SharePoint hay công cụ tài liệu nào khác.

## Cấu trúc thư mục

Tài liệu được chia **theo feature**. Mỗi feature có một thư mục riêng, khớp với module `:feature:<ten>` trong code, chứa toàn bộ tài liệu của feature đó. Những gì liên quan tới cả app thì nằm ở các thư mục dùng chung.

```
docs/
├── features/
│   └── <feature>/
│       ├── spec.md          spec hiện hành, lịch sử thay đổi ghi trong file
│       ├── requests/        yêu cầu gốc, biên bản họp
│       ├── design/          gói chuyển giao Claude Design, ảnh màn hình đã duyệt
│       ├── tickets/         ticket của feature
│       ├── plans/           plan của từng ticket, kèm nhật ký thực hiện
│       └── feedback/        phản hồi đã phân loại về feature này
├── adr/                     quyết định kỹ thuật, dùng chung cả app
├── inbox/                   yêu cầu và phản hồi chưa rõ thuộc feature nào
├── runs/                    các phiên AI tự chạy: danh sách chạy và báo cáo
├── ops/                     báo cáo vận hành định kỳ
├── templates/               các mẫu tài liệu
├── auto-run.md              quy trình phiên AI tự chạy
├── risk-scoring.md          thang điểm rủi ro và điều kiện dừng
├── CLAUDE.md                hướng dẫn AI làm việc với bộ tài liệu
└── README.md
```

| Tài liệu | Ai tạo | Ai xác nhận |
| --- | --- | --- |
| Yêu cầu | Người phụ trách sản phẩm, chủ feature | Không cần |
| Spec | AI sinh từ yêu cầu | Người phụ trách sản phẩm |
| Thiết kế | Chủ feature cùng Claude Design | Người phụ trách sản phẩm |
| Ticket | AI sinh từ spec | Chủ feature |
| Plan | AI sinh | Chủ feature, duyệt chéo nếu rủi ro cao |
| Phản hồi | Bất kỳ ai trong nhóm | Chủ feature phân loại |
| ADR | Dev, hoặc AI theo yêu cầu của dev | Cả hai dev |
| Phiên tự chạy | Danh sách chạy do AI lập sau họp, báo cáo do AI viết | Dev kích hoạt phiên duyệt danh sách chạy |
| Báo cáo vận hành | AI tổng hợp | Người phụ trách sản phẩm |

## Vòng đời của một yêu cầu

1. **Yêu cầu.** Người phụ trách sản phẩm hoặc chủ feature ghi yêu cầu vào `features/<feature>/requests/`, viết tự nhiên, không bắt buộc theo mẫu. Có thể dán biên bản họp, hoặc dựa vào [mẫu gợi ý](templates/request.md). Chưa rõ thuộc feature nào, hoặc là feature mới chưa có tên, thì ghi vào `inbox/`.
2. **Spec.** AI đọc yêu cầu, sinh hoặc cập nhật `features/<feature>/spec.md` theo [mẫu spec](templates/spec.md). Với feature mới, AI tạo thư mục feature. Chỗ nào chưa rõ, AI ghi vào mục câu hỏi còn mở, không tự đoán.
3. **Xác nhận spec.** Người phụ trách sản phẩm đọc lại, trả lời câu hỏi, chuyển trạng thái spec sang `confirmed`.
4. **Thiết kế.** Chủ feature dựng bản mẫu trên Claude Design, người phụ trách sản phẩm duyệt. Gói chuyển giao lưu vào `features/<feature>/design/` theo [mẫu thiết kế](templates/design.md).
5. **Ticket.** AI tách spec thành các ticket trong `features/<feature>/tickets/` theo [mẫu ticket](templates/ticket.md). Ticket chỉ chuyển sang `ready` khi spec đã xác nhận và thiết kế (nếu cần) đã duyệt.
6. **Plan (ngày 1).** AI sinh plan cho từng ticket được chọn vào sprint, theo [mẫu plan](templates/plan.md), kèm điểm rủi ro.
7. **Họp đánh giá rủi ro.** Nhóm xem plan, đánh giá ảnh hưởng chéo, duyệt hoặc dừng. AI sắp thứ tự chạy các plan đã duyệt và lập danh sách chạy trong `runs/`.
8. **Phiên AI tự chạy.** Khi dev đã duyệt xong và kích hoạt, AI thực hiện lần lượt từng plan theo [quy trình phiên tự chạy](auto-run.md): viết test trước, viết code, rà soát độc lập, qua cổng chất lượng thì merge vào nhánh chính. Cuối phiên, AI build bản staging và viết báo cáo phiên.
9. **Kiểm tra và phản hồi (ngày 2).** Dev đọc báo cáo phiên, kiểm tra bản staging, ghi phản hồi theo [mẫu phản hồi](templates/feedback.md). AI cập nhật plan theo phản hồi, plan sửa lại đi qua họp đánh giá rủi ro trước phiên tự chạy tiếp theo.
10. **Nghiệm thu (ngày 3).** Dev nghiệm thu chéo, người phụ trách sản phẩm nghiệm thu cuối. Ticket đạt chuyển sang `accepted`.
11. **Phát hành và vận hành.** Người phụ trách sản phẩm quyết định phát hành. AI tổng hợp định kỳ số liệu từ store thành [báo cáo vận hành](templates/ops-report.md) trong `ops/`. Vấn đề cần xử lý được tách thành phản hồi, ghi vào feature tương ứng hoặc vào `inbox/`.

Khi một quyết định kỹ thuật có ảnh hưởng lâu dài được đưa ra ở bất kỳ bước nào, ghi lại thành ADR theo [mẫu ADR](templates/adr.md). Các quyết định nền tảng đã chốt khi thiết lập quy trình được ghi sẵn từ ADR-0001 đến ADR-0008, mọi app clone từ dự án mẫu đều kế thừa.

## Quy ước đặt tên

| Loại | Mã | Đường dẫn | Ví dụ |
| --- | --- | --- | --- |
| Thư mục feature | tên feature | `features/<feature>/` | `features/daily-reminder/` |
| Yêu cầu | không có | `features/<feature>/requests/YYYY-MM-DD-<tu-khoa>.md` | `.../requests/2026-10-20-nhac-nho-hang-ngay.md` |
| Spec | tên feature | `features/<feature>/spec.md` | `features/daily-reminder/spec.md` |
| Thiết kế | tên feature | `features/<feature>/design/README.md` | `features/daily-reminder/design/README.md` |
| Ticket | `T-NNN` | `features/<feature>/tickets/T-NNN-<tu-khoa>.md` | `.../tickets/T-012-reminder-settings.md` |
| Plan | trùng mã ticket | `features/<feature>/plans/T-NNN-<tu-khoa>.md` | `.../plans/T-012-reminder-settings.md` |
| Phản hồi | `FB-YYYYMMDD-NN` | `features/<feature>/feedback/FB-YYYYMMDD-NN-<tu-khoa>.md` | `.../feedback/FB-20261021-01-reminder-time.md` |
| ADR | `ADR-NNNN` | `adr/NNNN-<tu-khoa>.md` | `adr/0009-local-notifications.md` |
| Phiên tự chạy | `RUN-YYYYMMDD-NN` | `runs/RUN-YYYYMMDD-NN.md` | `runs/RUN-20261020-01.md` |
| Báo cáo vận hành | `OPS-YYYY-MM-DD` | `ops/OPS-YYYY-MM-DD.md` | `ops/OPS-2026-11-02.md` |

- Tên feature viết thường, nối bằng dấu gạch ngang, trùng với tên module `:feature:<ten>`.
- Từ khóa trong tên file viết không dấu, ngắn gọn.
- **Mã ticket đánh số chung cho cả app**, không đánh lại cho từng feature, để "T-012" luôn chỉ đúng một task.
- Mã số tăng dần, không dùng lại mã đã cấp, kể cả khi ticket bị hủy.
- Phản hồi trong `inbox/` được chuyển vào thư mục feature khi phân loại xong. Phản hồi liên quan cả app (ví dụ tốc độ khởi động) ở lại `inbox/` và được liên kết tới ticket xử lý.

## Trạng thái

Trạng thái được ghi ở trường `status` trong phần đầu (frontmatter) của mỗi file, để người và AI cùng đọc được.

| Loại | Các trạng thái |
| --- | --- |
| Spec | `draft` → `confirmed` → `changed` (khi sửa sau xác nhận, cần xác nhận lại) |
| Thiết kế | `draft` → `approved` |
| Ticket | `backlog` → `ready` → `planned` → `approved` → `on-staging` → `accepted` → `released`, hoặc `stopped`, `cancelled` |
| Plan | `draft` → `approved` hoặc `stopped` → `done` |
| Phản hồi | `new` → `triaged` → `planned` → `fixed`, hoặc `wontfix` |
| ADR | `proposed` → `accepted`, sau này có thể `superseded` |
| Phiên tự chạy | `draft` → `approved` → `running` → `finished` |

## Nguyên tắc chung

- **Tài liệu là nguồn sự thật.** Code làm khác tài liệu thì một trong hai đang sai, phải sửa cho khớp ngay, không để lệch.
- **Không sửa lịch sử.** Spec đã xác nhận, plan đã duyệt, ADR đã chấp nhận không bị viết đè. Thay đổi được ghi thành mục mới, hoặc tạo bản thay thế có liên kết tới bản cũ.
- **Viết cho người đọc nhanh.** Spec viết bằng ngôn ngữ nghiệp vụ, tránh thuật ngữ kỹ thuật, để người phụ trách sản phẩm đọc và xác nhận nhanh. Plan được phép dùng thuật ngữ kỹ thuật.
- **Tiếng Việt tự nhiên, dễ hiểu.** Thuật ngữ kỹ thuật thông dụng (module, test, build, staging) giữ nguyên tiếng Anh.
