# QUY TRÌNH THIẾT KẾ: TỪ CLAUDE DESIGN HOẶC FIGMA TỚI GIAO DIỆN THẬT

Tài liệu này dành cho chủ feature (người dựng thiết kế), người phụ trách sản phẩm (người duyệt) và dev. Quyết định nằm ở ADR-0008 và ADR-0020. Nhóm dùng Claude Design hoặc Figma tùy dự án; từ bước bàn giao trở đi, hai công cụ đi chung một đường.

## Tổng quan

| Bước | Việc | Ai làm |
| --- | --- | --- |
| 1 | Dựng hệ thống thiết kế trên công cụ từ code (một lần cho mỗi app, làm lại khi hệ thống đổi) | Chủ feature |
| 2 | Dựng màn hình ở khung chuẩn, đủ trạng thái | Chủ feature |
| 3 | Duyệt thiết kế | Người phụ trách sản phẩm |
| 4 | Xuất ảnh, lập gói bàn giao trong repo | Chủ feature, AI giúp trong phiên có người theo dõi |
| 5 | AI viết giao diện bằng Compose theo plan | Phiên tự chạy |
| 6 | Đối chiếu: screenshot test, ảnh so sánh, rà soát độc lập | Phiên tự chạy |
| 7 | Duyệt giao diện thật trên ảnh so sánh và bản staging | Người phụ trách sản phẩm |

## 1. Hệ thống thiết kế: từ code sang công cụ

Code là gốc (ADR-0008). Nguồn để dựng trên công cụ:

- Ảnh catalog: `core/designsystem/src/test/screenshots/DesignSystem_*.png` (màu sáng, màu tối, chữ, khoảng cách và thành phần).
- Giá trị: `docs/design-system/tokens.json` (mã màu theo vai trò Material 3, cỡ chữ, chiều cao dòng, độ đậm, khoảng cách).

**Claude Design:** tạo design system mới, ở bước đưa nguồn vào, tải lên các ảnh catalog và `tokens.json` (Claude Design chỉ ghi rõ cách đọc mã nguồn React, nên không dùng cách trỏ thẳng vào repo Compose). Xem lại kết quả, chạy thử một dự án nhỏ, rồi bật **Published**. Hướng dẫn chính thức: https://support.claude.com/en/articles/14604397-set-up-your-design-system-in-claude-design

**Figma:** trong file thư viện của app, tạo biến màu (hai chế độ Light, Dark), kiểu chữ và biến khoảng cách theo `tokens.json`, đặt tên giống tên trong file (ví dụ `primary`, `bodyLarge`, `md`). Dựng thành phần `PrimaryButton` giống ảnh catalog. Gói Professional không có API biến, nên làm bằng tay.

Khi `:core:designsystem` đổi (plan rủi ro cao, ADR-0008), test `DesignTokensTest` và ảnh catalog đổi theo. Chủ feature cập nhật lại công cụ theo các file mới.

## 2. Dựng màn hình

- Khung **411 x 891** (Figma: frame 411 x 891; Claude Design: yêu cầu bản mẫu cho điện thoại kích thước 411 x 891).
- Chỉ dùng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế. Cần thành phần mới thì ghi vào mục 3 của mẫu thiết kế; đó là thay đổi module core, rủi ro cao.
- Đủ trạng thái có trong spec: thường là bình thường, đang tải, rỗng, lỗi, và chế độ tối.
- Chữ trên thiết kế là chữ thật, đúng như sẽ hiện trong app.

## 3. Duyệt thiết kế

Người phụ trách sản phẩm xem trên công cụ (link chia sẻ quyền xem hoặc bình luận), góp ý bằng bình luận, đồng ý thì ghi `approved_by`, `approved_at` trong `design/README.md` (bước 4).

## 4. Xuất ảnh và lập gói bàn giao

Gói nằm ở `docs/features/<feature>/design/`, theo [mẫu thiết kế](../templates/design.md):

```text
docs/features/<feature>/design/
├── README.md            mẫu thiết kế: màn hình, trạng thái, bảng chữ, ánh xạ thành phần, link công cụ
├── screens/             ảnh PNG 1233 x 2673, mỗi màn hình và trạng thái một ảnh
│   ├── LoadingScreen_progress_light.png
│   └── LoadingScreen_failed_light.png
├── context/             số liệu của từng màn hình lấy từ công cụ, cùng tên với ảnh, đuôi .md
│   ├── LoadingScreen_progress_light.md
│   └── LoadingScreen_failed_light.md
└── review/              do scripts/design-compare.sh sinh ra (bước 6), không sửa tay
```

`context/` cho AI số liệu chính xác của bản đã duyệt (bố cục, khoảng cách, màu, chữ, tên biến) mà không phải đọc công cụ thiết kế lúc làm. Phiên tự chạy không nối với Figma hay Claude Design: thiết kế có thể đổi sau khi duyệt, công cụ có thể lỗi hay hết lượt giữa phiên, và connector có cả quyền sửa thiết kế (ADR-0020).

Mỗi file `context/<tên ảnh>.md` gồm:

1. **Nguồn:** link frame (Figma: link có `node-id`), ngày lấy, người lấy.
2. **Biến dùng:** tên biến và giá trị (Figma: kết quả `get_variable_defs`), đối chiếu với `docs/design-system/tokens.json`. Giá trị không có trong `tokens.json` được đánh dấu "ngoài hệ thống" để plan quyết định.
3. **Cấu trúc và số liệu:** cây phần tử theo thứ tự từ trên xuống, với kích thước, khoảng cách, căn lề, màu, kiểu chữ và chữ của từng phần tử (Figma: rút từ kết quả `get_design_context`).
4. **Code tham khảo của công cụ** (nếu có), để nguyên trong khối code, ghi rõ "chỉ để đọc số liệu, không chép": công cụ sinh code web, app viết bằng Compose (ADR-0008).

AI lập `context/` trong phiên có người theo dõi, cùng lúc với `screens/`; người duyệt xem file trước khi commit. Thiết kế đổi sau khi duyệt thì lấy lại cả ảnh và `context/`.

Tên ảnh là `<Ten>Screen_<trạng thái>_<light|dark>.png`, trùng tên ảnh chuẩn screenshot mà plan sẽ tạo (ví dụ `LoadingScreen_failed_light.png` cho composable `LoadingScreen` ở trạng thái lỗi, giao diện sáng).

**Xuất từ Figma (cách chính):** trong phiên Claude Code có người theo dõi đã nối Figma MCP, gửi AI link từng frame. AI dùng `get_screenshot` lưu ảnh vào `screens/` (kiểm tra đúng 1233 x 2673), và `get_design_context`, `get_variable_defs` để viết `context/`. Cách tay: chọn frame, thêm Export setting **PNG, 3x**, bấm **Export** (Shift+Cmd+E); khi đó vẫn nhờ AI lập `context/` qua MCP.

**Xuất từ Claude Design:** tài liệu chưa có xuất PNG. Mở bản mẫu trong Chrome, bật DevTools > chế độ thiết bị, đặt kích thước 411 x 891 và DPR 3, mở menu (dấu ba chấm) > **Capture screenshot**. Ảnh ra đúng 1233 x 2673. Lưu kèm gói **Export > .zip** vào `design/handoff/`, rồi nhờ AI đọc HTML/CSS trong gói để viết `context/` theo cùng cấu trúc (không có tên biến thì ghi giá trị và token tương ứng trong `tokens.json`).

**Nối Figma MCP vào Claude Code (chỉ cho phiên có người theo dõi):** `claude mcp add --transport http figma https://mcp.figma.com/mcp`, rồi `/mcp` > figma > Authenticate. Gói Professional với Dev hoặc Full seat có 200 lượt mỗi ngày. Không thêm connector này vào routine.

Kiểm tra gói: mỗi ảnh trong `screens/` đúng 1233 x 2673 (`sips -g pixelWidth -g pixelHeight <ảnh>` trên macOS) và có file `context/` cùng tên, tên đúng quy tắc, `README.md` có bảng chữ và bảng ánh xạ.

## 5. AI viết giao diện

Plan của ticket ghi trong mục 2 đường dẫn gói thiết kế, và trong mục 4 một screenshot test cho mỗi ảnh trong `screens/`, cùng tên. AI lấy số liệu từ `context/`, nhìn ảnh trong `screens/` để hiểu tổng thể, viết lại bằng Compose theo token, không chép code web của bản mẫu (ADR-0008). Giá trị đánh dấu "ngoài hệ thống" trong `context/` được plan quyết định trước: làm tròn về token gần nhất, hoặc thêm token (thay đổi module core, rủi ro cao).

## 6. Đối chiếu

Trong phiên tự chạy (`docs/auto-run.md`, bước 5 và 6):

1. Screenshot test và test giao diện qua (cổng chặn).
2. `scripts/design-compare.sh <feature>` sinh ảnh ghép thiết kế, thật, khác biệt và `review/summary.md` với tỷ lệ điểm ảnh khác. Không chặn merge.
3. Phiên rà soát độc lập xem từng ảnh ghép theo mục 7 của mẫu thiết kế và xếp sai lệch theo mức chặn, nên sửa, ghi chú.
4. `review/` được commit cùng PR của task, `summary.md` được dán vào mô tả PR.

Tỷ lệ khác biệt chỉ để tham khảo: phông chữ và cách vẽ chữ luôn làm lệch vài phần trăm. Mắt người và phiên rà soát quyết định.

## 7. Duyệt giao diện thật

Người phụ trách sản phẩm mở `design/review/*_compare.png` trên PR hoặc trong repo, và xem bản staging. Đồng ý thì ghi vào báo cáo phiên hoặc vào PR; ảnh chuẩn screenshot từ đó giữ giao diện. Không đồng ý thì ghi phản hồi (`docs/README.md`, bước phản hồi), plan sửa đi qua họp đánh giá như thường.
