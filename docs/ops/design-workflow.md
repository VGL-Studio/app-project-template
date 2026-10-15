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
└── review/              do scripts/design-compare.sh sinh ra (bước 6), không sửa tay
```

Tên ảnh là `<Ten>Screen_<trạng thái>_<light|dark>.png`, trùng tên ảnh chuẩn screenshot mà plan sẽ tạo (ví dụ `LoadingScreen_failed_light.png` cho composable `LoadingScreen` ở trạng thái lỗi, giao diện sáng).

**Xuất từ Figma:** chọn frame, thêm Export setting **PNG, 3x**, bấm **Export** (Shift+Cmd+E). Hoặc trong phiên Claude Code có người theo dõi đã nối Figma MCP: dán link frame và nhờ AI dùng `get_screenshot` lưu ảnh vào `screens/`.

**Xuất từ Claude Design:** tài liệu chưa có xuất PNG. Mở bản mẫu trong Chrome, bật DevTools > chế độ thiết bị, đặt kích thước 411 x 891 và DPR 3, mở menu (dấu ba chấm) > **Capture screenshot**. Ảnh ra đúng 1233 x 2673. Có thể lưu kèm gói **Export > .zip** vào `design/handoff/` để tham khảo.

**Nối Figma MCP vào Claude Code (chỉ cho phiên có người theo dõi):** `claude mcp add --transport http figma https://mcp.figma.com/mcp`, rồi `/mcp` > figma > Authenticate. Gói Professional với Dev hoặc Full seat có 200 lượt mỗi ngày. Không thêm connector này vào routine.

Kiểm tra gói: mỗi ảnh trong `screens/` đúng 1233 x 2673 (`sips -g pixelWidth -g pixelHeight <ảnh>` trên macOS), tên đúng quy tắc, `README.md` có bảng chữ và bảng ánh xạ.

## 5. AI viết giao diện

Plan của ticket ghi trong mục 2 đường dẫn gói thiết kế, và trong mục 4 một screenshot test cho mỗi ảnh trong `screens/`, cùng tên. AI viết lại bằng Compose theo token, không chép code web của bản mẫu (ADR-0008).

## 6. Đối chiếu

Trong phiên tự chạy (`docs/auto-run.md`, bước 5 và 6):

1. Screenshot test và test giao diện qua (cổng chặn).
2. `scripts/design-compare.sh <feature>` sinh ảnh ghép thiết kế, thật, khác biệt và `review/summary.md` với tỷ lệ điểm ảnh khác. Không chặn merge.
3. Phiên rà soát độc lập xem từng ảnh ghép theo mục 7 của mẫu thiết kế và xếp sai lệch theo mức chặn, nên sửa, ghi chú.
4. `review/` được commit cùng PR của task, `summary.md` được dán vào mô tả PR.

Tỷ lệ khác biệt chỉ để tham khảo: phông chữ và cách vẽ chữ luôn làm lệch vài phần trăm. Mắt người và phiên rà soát quyết định.

## 7. Duyệt giao diện thật

Người phụ trách sản phẩm mở `design/review/*_compare.png` trên PR hoặc trong repo, và xem bản staging. Đồng ý thì ghi vào báo cáo phiên hoặc vào PR; ảnh chuẩn screenshot từ đó giữ giao diện. Không đồng ý thì ghi phản hồi (`docs/README.md`, bước phản hồi), plan sửa đi qua họp đánh giá như thường.
