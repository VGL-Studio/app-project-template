---
feature: ads
status: approved             # draft | approved
spec: features/ads/spec.md
tool: figma
link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ (App Template: Design System, section "Ads" trên trang "Screens / Ads")
designer: AI (Figma MCP), Nhật duyệt
approved_by: Nhật
approved_at: 2026-10-21
---

# THIẾT KẾ: QUẢNG CÁO NATIVE

<!--
Quy trình: docs/ops/design-workflow.md (mục 8: AI dựng, người duyệt). Quyết định: ADR-0008, ADR-0020, ADR-0022.
Ảnh và số liệu sinh bằng scripts/figma-export.py từ section "Ads" (node 7:239). Component nằm ở trang
"Components / Native ads" của cùng file; nền móng (biến màu Light/Dark, khoảng cách, bo góc, kiểu chữ) dựng từ
docs/design-system/tokens.json.
-->

## 1. Các màn hình và trạng thái

| Màn hình (composable) | Trạng thái | Ảnh trong `screens/`, số liệu trong `context/` | Kịch bản | Ticket |
| --- | --- | --- | --- | --- |
| `SampleScreen` | Sẵn sàng, native small ở đáy | `SampleScreen_ready_light` | KB-1 | T-010 |
| `SampleScreen` | Sẵn sàng, chế độ tối | `SampleScreen_ready_dark` | KB-1 | T-010 |
| `SampleScreen` | Sẵn sàng, điện thoại gập mở ra (673 x 841), khe đáy 200 dp | `SampleScreen_ready_light_unfolded` | KB-32 | T-064 (code: T-065) |
| `SampleScreen` | Quảng cáo đang tải | `SampleScreen_ad-loading_light` | KB-1 | T-010 |
| `AdGalleryScreen` (chỉ staging) | Small, medium, nút gọi quảng cáo toàn màn hình, Ad Inspector | `AdGalleryScreen_default_light` | KB-1, KB-2, KB-8 | T-014 |
| `AdGalleryScreen` | Như trên, chế độ tối | `AdGalleryScreen_default_dark` | KB-1, KB-2, KB-8 | T-014 |
| `AdGalleryScreen` | Native trong danh sách (khung minh họa với N = 3) | `AdGalleryScreen_inlist_light` | KB-3 | T-014 (T-011 làm phần chèn, có test riêng) |
| `AdGalleryScreen` | Native thu gọn được, đang mở | `AdGalleryScreen_collapsible-expanded_light` | KB-4 | T-030 |
| `AdGalleryScreen` | Native thu gọn được, đã thu gọn | `AdGalleryScreen_collapsible-collapsed_light` | KB-4 | T-030 |
| `NativeFullScreenAdScreen` | Native toàn màn hình | `NativeFullScreenAdScreen_default_light` | KB-5 | T-013 |
| `NativeFullScreenAdScreen` | Như trên, chế độ tối | `NativeFullScreenAdScreen_default_dark` | KB-5 | T-013 |

Ảnh `SampleScreen_disabled_light` (đã có) không đổi: trạng thái tắt không có quảng cáo.

Khu "More formats" của trang mặc định (dưới y 786, ngoài khung thiết kế) và sáu trang "Banners", "Grid ads", "Pager ads", "Flow steps", "Rewarded", "Ad status" của `AdGalleryScreen` (T-036) không có ảnh thiết kế: màn xem thử là công cụ của nhóm, dựng theo token và component có sẵn; ảnh chuẩn `AdGalleryScreen_*` của plan T-036 trong `app/src/test/screenshots` là bản gốc (câu hỏi 1 của plan đó).

**Vẽ lại theo kích thước AdsKit (T-035, 2026-10-25, anh Nhật duyệt).** Component trên trang "Components / Native ads" theo `docs/ops/ads-business-rules.md`, mục 5.7:

- `Native/Small` (khe đáy): cao 80 dp, icon 36 dp, CTA cao 34 dp chữ 13 sp, nhãn "Ad" 10 sp.
- `Native/Collapsible`: lớp mở cao 250 dp, media lấp phần còn lại, icon 32 dp, tiêu đề 13 sp, CTA 36 dp, nút thu gọn 32 dp (biểu tượng 20 dp) cách góc trên phải 4 dp, nền đen 40%. Thu gọn thì khe đáy còn native nhỏ 80 dp. AdChoices đặt ở góc trên trái vì nút thu gọn giữ góc trên phải.
- `Native/Medium`: icon 40 dp, nền mờ sau media, hàng sao, CTA 44 dp; biến thể `CTA=Large` (thuộc tính `cta_large`): CTA 56 dp, chữ 17 sp, không viết hoa toàn bộ.
- `Native/FullScreen`: CTA 52 dp; hai biến thể `After interstitial` (ảnh vẽ nút đóng 36 dp; code vẽ 48 dp từ T-053 theo ADR-0038, ảnh chờ vẽ lại ở T-064) và `Flow step` (không có nút điều khiển, app tự đặt nút Tiếp).
- Mới: `Native/Step` (thẻ ở bước của luồng, 250 dp, icon 32 dp, CTA 36 dp) và `Native/Card` (thẻ dọc trong tập hợp; 180 x 300 dp là ví dụ, ô do lưới quyết định), mỗi component có trạng thái chờ.
- Cỡ chữ 13, 10 và 17 sp không có trong bảng kiểu chữ: ghi đè trên component, code đặt cỡ chữ riêng trong composable quảng cáo của `:core:ads`.

Ảnh đã đổi theo đợt này, thay ảnh của đợt 2: mọi ảnh trong bảng trên (khe đáy 80 dp đặt cách đáy 16 dp, lớp thu gọn cách đáy 16 dp, cùng mép dưới với khe, medium và toàn màn hình theo số mới), và `NetworkLostScreen_default_light`, `NetworkLostScreen_default_dark` của feature network (khe đáy). Ticket code ghi lại ảnh chuẩn screenshot theo các ảnh này: T-027 (khe đáy và lớp thu gọn), T-030 (thanh đáy), T-032 (native trong tập hợp và bước của luồng). `Native/Step` và `Native/Card` chưa có màn hình riêng; ticket dùng chúng thêm ảnh màn hình khi cần.

**Vẽ lại theo bề rộng màn hình (T-064, 2026-11-14, anh Nhật duyệt trong phiên: "Duyệt.").** Spec, câu hỏi 48, nêu số của JBase; anh Nhật quyết không dùng số riêng của JBase cho kích thước ba biến thể mà nội suy tuyến tính (spec, câu hỏi 64; phạm vi chỉ là kích thước, JBase vẫn là chuẩn thứ hai của nghiệp vụ, ADR-0045, mục 1.2). Spec chỉ cho mốc bề rộng (320 và 600 dp) của khe đáy; mốc của lớp thu gọn (bề rộng 320 và 600 dp) và của medium (chiều cao 640 và 891 dp, khung thấp và khung chuẩn của ADR-0020; theo bản phân tích JBase ngày 2026-10-29, JBase dùng 600 và 900 dp, spec câu hỏi 64) do thiết kế chọn, được duyệt cùng lời duyệt trên. Ngoài khoảng mốc thì giữ sàn và trần: khe đáy 90 và 200 dp, vùng ảnh của lớp thu gọn 160 và 200 dp, medium 320 và 400 dp. Số riêng của JBase trong câu hỏi 48 (ví dụ "ảnh 180 dp" của lớp thu gọn) không dùng: ảnh lấp vùng ảnh nội suy (MediaView 161 dp ở màn 411 dp, vùng 173 dp trừ lề 12 dp). Các số dưới đây thay số của đợt T-035 ở mục trên cho `Native/Small`, `Native/Collapsible`, `Native/Medium` và biến thể `After interstitial` của `Native/FullScreen`; `Native/Step`, `Native/Card` và biến thể `Flow step` không đổi.

- **Khe đáy theo bề rộng màn hình:** cao 90 dp ở bề rộng 320 dp, 200 dp ở 600 dp, nội suy tuyến tính giữa hai mốc: 106 dp ở 360 dp, 126 dp ở 411 dp, 200 dp ở 673 dp (trên 600 dp giữ 200 dp). Code tính theo cạnh ngắn của cửa sổ (spec, câu hỏi 65), nên màn ngang giữ số của màn dọc (126 dp ở 891 x 411 dp); lớp thu gọn cộng lề đáy 16 dp không cao quá nửa chiều cao cửa sổ, vùng ảnh co trước, MediaView không dưới 120 dp, không đủ chỗ thì không có lớp. Thiết kế không có ảnh màn ngang; ảnh chuẩn screenshot ở khung ngang, chia đôi và cửa sổ thấp là bản tham chiếu. Frame tham khảo "Native/Small slot height by screen width (reference)" trên trang component vẽ ba bề rộng 328, 379 và 641 dp (màn 360, 411, 673 trừ lề 16 dp mỗi bên). Khung 360 x 640 chỉ có ở frame tham khảo vì `scripts/figma-export.py` chỉ nhận frame 411 x 891 và `_unfolded`; ảnh `_small` của code là ảnh chuẩn screenshot.
- `Native/Small` (khe đáy): một hàng ngang căn giữa theo chiều dọc ở mọi chiều cao: icon 44 dp; cột chữ gồm hàng tiêu đề (nhãn "Ad" 13 sp đứng trước tiêu đề 16 sp, một dòng, cắt cuối) và mô tả 13 sp tối đa hai dòng; CTA cao 44 dp, chữ 18 sp, rộng theo chữ, bên phải. Lề trái 12 dp, phải 24 dp, các phần cách nhau 12 dp. Trạng thái chờ: cùng chiều cao, khối icon 44 dp, hai vạch chữ (vạch trên rộng hết cột chữ, vạch dưới 120 dp), khối CTA 96 x 44 dp.
- `Native/Collapsible`: lớp mở = vùng ảnh nội suy 160 dp ở bề rộng 320 dp tới 200 dp ở 600 dp (173 dp ở 411 dp; lề 12 dp, ảnh lấp vùng) cộng phần nội dung giống hệt khe đáy (126 dp ở 411 dp), tổng 299 dp. Nút thu gọn 27 dp tròn tại (4, 4) ở góc trên trái, biểu tượng 17 dp, nền đen 40%; AdChoices ở góc trên phải. Thu gọn thì lớp ẩn, còn khe đáy.
- `Native/Medium`: cao cố định theo chiều cao màn hình, 320 dp ở chiều cao 640 dp tới 400 dp ở 891 dp (384 dp ở 841 dp); icon 44 dp; nhãn "Ad" 13 sp trước tiêu đề 16 sp, nhà quảng cáo 13 sp ở dòng dưới; giữ hàng sao và nền mờ sau media; CTA 44 dp chữ 18 sp (`CTA=Large` giữ 56 dp); media lấp phần còn lại (182 dp, 170 dp với `CTA=Large`).
- `Native/FullScreen`, biến thể `After interstitial`: hàng trên cao 48 dp, nút đóng 48 dp tròn tại (16, 24), biểu tượng 20 dp, cùng khung với vòng đếm ngược; media từ y = 88. Ảnh nay khớp code từ T-053 (ADR-0038).
- AdChoices ghim góc trên phải và tiêu đề cắt một dòng ở mọi biến thể khi đổi bề rộng.
- Cỡ chữ 13, 16 và 18 sp không có trong bảng kiểu chữ: `context/` đánh dấu "ngoài hệ thống", plan code (T-065) đặt cỡ chữ riêng trong composable quảng cáo của `:core:ads` như đợt T-035.

Ảnh đã đổi theo đợt này, thay ảnh của đợt T-035: mọi ảnh trong bảng mục 1 (khe đáy 126 dp và lớp thu gọn 299 dp, mép dưới cách đáy 16 dp; medium 400 dp; nút đóng 48 dp và media từ y = 88 của toàn màn hình), ảnh mới `SampleScreen_ready_light_unfolded`, và `NetworkLostScreen_default_light`, `_dark` của feature network (khe đáy). Code chưa đổi: T-065 ghi lại ảnh chuẩn screenshot theo các ảnh này.

## 2. Bảng chữ

Chữ của quảng cáo (tiêu đề, nhà quảng cáo, mô tả, CTA, ảnh) do SDK trả về; trên thiết kế là chữ minh họa, không vào `strings.xml`. Chữ của app:

| Màn hình, trạng thái | Vị trí | Chữ | Kiểu chữ |
| --- | --- | --- | --- |
| Mọi native | Nhãn quảng cáo | Ad | `labelSmall` |
| `SampleScreen` | Tiêu đề, nhãn, số, nút | Sample feature, Times tapped, 3, Tap | như hiện có |
| `AdGalleryScreen` | Tiêu đề | Ad gallery (staging) | `titleLarge` |
| `AdGalleryScreen` | Nhãn khu vực | Native small, Native medium | `titleSmall` |
| `AdGalleryScreen` | Nút | Interstitial, Native full screen, Open Ad Inspector | `labelLarge` đậm 600 (`PrimaryButton`) |
| `AdGalleryScreen`, trong danh sách | Tiêu đề, mục | In-list ads (every 3 items), List item 1 đến 7 | `titleLarge`, `bodyLarge` |
| `AdGalleryScreen`, thu gọn | Tiêu đề, mục | Collapsible ad, List item 1 đến 6 | `titleLarge`, `bodyLarge` |

Chữ của màn hình xem thử chỉ ở staging, tiếng Anh, vẫn đặt trong `strings.xml` của source set staging.

## 3. Ánh xạ sang hệ thống thiết kế

| Phần tử trên thiết kế | Thành phần trong code | Màu (vai trò) | Kiểu chữ | Khoảng cách |
| --- | --- | --- | --- | --- |
| Khung native small, medium, thu gọn | composable native của `:core:ads` (T-010) | nền `surfaceContainerLow`, viền 1 dp `outlineVariant` | | bo `MaterialTheme.shapes.medium` (12 dp); lề 12 dp. Native nhỏ cao theo cạnh ngắn của cửa sổ (90 dp ở 320 dp tới 200 dp ở 600 dp; 126 dp ở 411 dp, cả màn ngang), medium theo chiều cao màn hình (320 dp ở 640 dp tới 400 dp ở 891 dp), một hàm cho thẻ, khung chờ và khoảng chừa (T-065). Lớp thu gọn = vùng ảnh nội suy theo cạnh ngắn (160 tới 200 dp) + nội dung của native nhỏ: 299 dp ở 411 x 891 dp, không cao thêm khi chữ lớn; cộng lề đáy không quá nửa chiều cao cửa sổ (vùng ảnh co trước, MediaView ít nhất 120 dp), không đủ chỗ thì không có lớp. Thẻ ở bước 250 dp ở cỡ chữ 1.0, cao thêm khi media chạm 120 dp (chữ lớn; T-045) |
| Nhãn "Ad" | trong composable native; small, medium và lớp thu gọn: đứng trước tiêu đề cùng hàng; thẻ ở bước, đầy đủ: đầu dòng dưới tiêu đề | nền `tertiary`, chữ `onTertiary` | `labelSmall` (small, medium: cỡ 13 sp, chiều cao dòng giữ của `labelSmall`) | bo 4 dp, lề ngang 4 dp, cách tiêu đề 6 dp |
| Tiêu đề quảng cáo | asset headline | `onSurface` | small, medium: 16/22 sp, đậm 500 (hằng riêng); `titleSmall` (đầy đủ), `titleMedium` (toàn màn hình) | |
| Nhà quảng cáo, mô tả | asset advertiser, body | `onSurfaceVariant` | small (dòng dưới tiêu đề: nhà quảng cáo, không có thì mô tả, tối đa 2 dòng) và nhà quảng cáo của medium: 13/18 sp; mô tả của medium `bodyMedium` | small: dòng dưới cách hàng tiêu đề 4 dp |
| CTA quảng cáo | asset call to action | nền `tertiaryContainer`, chữ `onTertiaryContainer` | `labelLarge` (small, medium: cỡ 18 sp; medium `cta_large`: 17 sp) | cao 44 dp ở small (rộng theo chữ, 80 tới 120 dp) và medium (56 dp khi `cta_large`), 36 dp ở đầy đủ và thẻ dọc, 52 dp ở toàn màn hình; bo 8 dp. Số trên là ở cỡ chữ 1.0; chữ lớn thì CTA cao vừa một dòng chữ (T-045) |
| Icon quảng cáo | asset icon | | | 44 dp ở small và medium, 40 dp ở đầy đủ, 48 dp ở toàn màn hình; bo 8 dp. Đầu quảng cáo của toàn màn hình 48 dp ở cỡ chữ 1.0, cao theo chữ khi chữ lớn (nhãn "Ad" không bị cắt; T-045) |
| Media | `MediaView` | nền chờ `surfaceContainer` | | ít nhất 120 x 120 dp; medium lấp phần còn lại (182 dp trong thẻ 400 dp, 170 dp khi `cta_large`), đầy đủ 200 dp ở cỡ chữ 1.0, lớp thu gọn lấp vùng ảnh trừ lề 12 dp (161 dp ở 411 dp). Khi chữ lớn (cỡ chữ hệ thống), media co trước, không dưới 120 dp: các phần khác giữ cỡ, thẻ giữ chiều cao; chỉ khi media đã chạm 120 dp thì thẻ cao thêm (medium ở màn 891 dp, cỡ chữ 2.0: 406 dp, 418 dp khi `cta_large`) |
| AdChoices | do SDK vẽ, góc trên phải (cả lớp thu gọn) | | | không đè, không vẽ lại |
| Nút thu gọn | trong lớp thu gọn của thanh đáy | nền `scrim` độ mờ 40%, icon trắng (ngoại lệ có tên `CollapseIconTint`, plan T-030, câu hỏi 8) | | vẽ 27 dp tròn tại (4, 4) góc trên trái, icon 17 dp; vùng chạm 48 dp do Compose tự nới |
| Nút đóng toàn màn hình | trong `NativeFullScreenAdScreen` | nền `surfaceContainerHigh`, icon `onSurface` | | vẽ 48 dp tròn tại (16, 24), vùng chạm bằng khung vẽ, góc trên trái; vòng đếm ngược cùng khung (T-053, ADR-0038; ảnh thiết kế vẽ 48 dp từ T-064) |
| Trạng thái đang tải | trong composable native | khối `surfaceContainerHighest` | | giữ đúng chiều cao của quảng cáo (small và medium theo màn hình như khung, ví dụ 126 dp và 400 dp ở 411 x 891 dp; đầy đủ 418 dp, thẻ dọc 300 dp hay cỡ ô); small: khối icon 44 dp, hai vạch, khối CTA 96 x 44 dp |
| Nút của app | `PrimaryButton` | `primary`, `onPrimary` | `labelLarge` đậm 600 | |
| Thẻ native ở bước của luồng (component `Native/Step`, T-032) | `NativeStepAd`, cỡ `NativeAdSize.STEP` | như khung native small, medium | tiêu đề `titleSmall` cỡ 13 sp | cao 250 dp ở cỡ chữ 1.0 (chữ lớn thì cao thêm, `NativeStepAdDefaults.height()`; T-045), lề trong 12 dp, các phần cách nhau 8 dp; media ngang lấp phần trên, icon 32 dp, CTA 36 dp rộng hết thẻ; AdChoices góc trên phải; khung chờ cùng chiều cao với thẻ |
| Trang native ở bước của luồng (biến thể `Flow step` của `Native/FullScreen`, T-032) | `NativeFullScreenStep`, `NativeFullScreenStepContent` | như native toàn màn hình | như native toàn màn hình | không nút đóng, không đếm ngược; hàng trên 36 dp chỉ có AdChoices (media bắt đầu ở y = 76; bản sau inter ở y = 88 vì hàng nút 48 dp, T-053); không đệm vùng an toàn; CTA 52 dp; nút Tiếp của app nằm ngoài vùng của trang. Hai bố cục theo chiều cao (T-041): cột như bản sau inter khi trang cao từ 360 dp (ở cỡ chữ 1.0; media co trước, không dưới 120 dp, rồi mô tả bớt dòng), hàng ngang gọn từ 152 dp tới dưới 360 dp (lề 16 dp, media bên trái cao hết hàng; bên phải: đầu quảng cáo và ngay bên phải nó ô 36 dp chừa AdChoices, giữ góc trên phải của trang trống (T-049), rồi mô tả nếu còn chỗ, CTA ở đáy); đầu quảng cáo cao ít nhất 48 dp, lớn theo cỡ chữ. Trong lúc chờ quảng cáo: khung chờ tĩnh cùng bố cục (khối media, đầu, CTA; không chữ) |

- **Số đo của bảng trên theo thiết kế T-064, code từ T-065.** Câu cuối mục 5 (CTA cao 36 dp) là của thẻ đầy đủ và thẻ dọc; CTA của small, medium cao 44 dp.
- **Hai quy tắc ngoài thiết kế (plan T-065):** native nhỏ (khe đáy, khe riêng, hàng trong danh sách, nội dung của lớp thu gọn) từ cỡ chữ 1.3 các phần cách nhau 8 dp, lề phải 16 dp (tiêu đề còn chỗ cạnh CTA dài 120 dp, câu hỏi 15); từ cỡ chữ 1.5 nhãn "Ad" xuống đầu dòng mô tả và dòng đó còn 1 dòng (câu hỏi 8, 14). Medium ở khung thấp (thẻ dưới 350 dp, màn cao tới 732 dp): mô tả 1 dòng, các phần cách nhau 8 dp, nên thẻ 320 dp giữ media 134 dp (câu hỏi 7).
- **Giá trị "ngoài hệ thống" trong `context/`:** khoảng cách 2, 6 và 12 dp bên trong quảng cáo (giữa tiêu đề và dòng nhà quảng cáo, giữa nhãn "Ad" và chữ, lề trong khung). Plan T-010 quyết: giữ làm hằng số riêng của composable native trong `:core:ads` (kích thước của một component, như `MinHeight` của `PrimaryButton`), không thêm token.
- **Thành phần mới cần thêm vào hệ thống thiết kế:** các composable native nằm ở `:core:ads` (ADR-0022), dùng token của `:core:designsystem`; không thêm thành phần vào `:core:designsystem`.

## 4. Luồng thao tác

- `SampleScreen`: vào màn hình, native small ở đáy hiện trạng thái đang tải rồi tới quảng cáo.
- `AdGalleryScreen` (staging): bấm "Interstitial" hay "Native full screen" thì gọi quảng cáo toàn màn hình; bấm "Open Ad Inspector" thì mở Ad Inspector của AdMob.
- Thu gọn: vào màn hình thì lớp thu gọn (299 dp ở màn 411 dp) mở trên khe đáy; chạm nút thu gọn thì lớp ẩn, lộ khe native nhỏ (126 dp ở màn 411 dp) bên dưới; lớp tự mở lại sau `time_expand_collap_ad` giây (mặc định 10, 0 là không mở lại), đếm từ lúc chạm (CL-03, T-030).
- Thiết kế T-064 đổi số của luồng thu gọn: lớp mở 299 dp và khe đáy 126 dp ở màn 411 dp (mục 1); luồng không đổi.
- `NativeFullScreenAdScreen`: chạm nút đóng hay nút quay lại của máy thì về màn hình trước.

## 5. Vị trí quảng cáo

| Vị trí | Định dạng | Màn hình | Ghi chú |
| --- | --- | --- | --- |
| `sample_native_small` (thay `sample_bottom_banner`) | Native small | `SampleScreen` | Ghim ở đáy, cách mép dưới 16 dp, không nằm ngay dưới nút "Tap" |
| Các vị trí của màn hình xem thử | Mọi định dạng | `AdGalleryScreen` | Chỉ staging |

Mọi vùng quảng cáo có nền và viền tách khỏi nội dung; cách nút, mục danh sách của app ít nhất 16 dp; CTA khác `PrimaryButton` (màu tertiary nhạt, cao 36 dp, bo 8 dp so với primary, 52 dp, bo 16 dp).

## 6. Chuyển sang Compose

- **Phải giống bản mẫu:** bố cục và thứ tự thành phần của từng cỡ native, vị trí AdChoices, nút thu gọn, nút đóng, chiều cao giữ chỗ khi tải, vai trò màu và kiểu chữ (mục 3).
- **Được phép khác:** chữ và ảnh của quảng cáo (do SDK), cách vẽ chữ của hệ thống, chênh lệch nhỏ theo lưới 4 dp. Screenshot test dùng quảng cáo giả trong test để có chữ và ảnh cố định.

## 7. Tiêu chí khi duyệt

- [x] Dùng đúng màu, chữ, khoảng cách, thành phần của hệ thống thiết kế.
- [x] Đủ các trạng thái trong mục 1, kể cả chế độ tối.
- [x] Chữ đúng bảng chữ, không bị cắt hay tràn.
- [ ] Bố cục và thứ tự thành phần giống bản mẫu; lệch về vị trí không quá một bước lưới 4 dp (kiểm khi code).
- [x] Vùng chạm đủ lớn (ít nhất 48 dp), chữ dễ đọc.
- [ ] Dùng được ở màn hình thấp (360 x 640) (kiểm khi code, ảnh `_small`).
- [x] Vùng quảng cáo tách bạch, không gây bấm nhầm.
- [x] Giao diện và nội dung không được thiết kế để thu hút chủ yếu trẻ em.

## 8. Kết quả đối chiếu

Màn hình xem thử (T-014, 2026-10-23). Ảnh chuẩn ở `app/src/test/screenshots/` (`:app`, staging); script so sánh bọc sẵn chưa nhận `:app` nên chạy thẳng `scripts/design-compare/DesignCompare.java` (plan T-014, mục 3.2). Ảnh so sánh trong `review/`; kết quả chỉ ghi ở mục này, `review/summary.md` không đổi.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_default_light` | 1.9% | Khác chính: hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế (plan T-014, câu hỏi 7). Quảng cáo giả: ô AdChoices không có chữ "i", media không có nhãn "Media", medium hiện "Advertiser or short body" thay "Advertiser". Vị trí tiêu đề, nhãn, quảng cáo, nút trùng thiết kế |
| `AdGalleryScreen_default_dark` | 1.9% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 1.6% | Thứ tự và khoảng cách trùng (small sau mục 3, medium sau mục 6, cách 16 dp); viền mục chỉ ở cạnh dưới; khác ở chữ quảng cáo giả như trên |
| `AdGalleryScreen_collapsible-expanded_light` | 0.7% | Ghim đáy, cách mép 16 dp. Dạng mở không vẽ ô AdChoices giả (dạng thu gọn có), đã biết từ T-012; chữ quảng cáo giả như trên |
| `AdGalleryScreen_collapsible-collapsed_light` | 0.7% | Trùng thiết kế, khác ở chữ "i" của ô AdChoices giả |

Nhận xét của rà soát độc lập (RUN-20261024-01): chữ, thứ tự và vai trò màu (nút toàn màn hình `primary`, nút chuyển trang viền, bản tối) trùng thiết kế; chỉ khác ở hàng nút chuyển trang đã duyệt và quảng cáo giả; vị trí lệch nhiều nhất khoảng 1 px. Không có điểm chặn.

Native theo kích thước AdsKit (T-027, 2026-10-26), so với thiết kế vẽ lại (T-035). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho từng module (`core/ads`, `feature/sample`, `app`), chép ảnh so sánh vào `review/`; `review/summary.md` không đổi.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `SampleScreen_ready_light` | 1.3% | Khe đáy 80 dp, icon 36, CTA 34 dp, nhãn "Ad" đầu dòng mô tả trùng thiết kế. Khác: nút "Tap" thấp hơn khoảng 2 dp (không thuộc task này), ô AdChoices giả không có chữ "i" |
| `SampleScreen_ready_dark` | 1.3% | Như bản sáng |
| `SampleScreen_ad-loading_light` | 0.9% | Khung chờ 80 dp, khối CTA 80 x 34 dp trùng thiết kế |
| `NativeFullScreenAdScreen_default_light` | 0.5% | Nút đóng 36 dp tại (16, 24), media từ y = 76, icon 48, CTA 52 dp trùng thiết kế; khác ở chữ "i" của AdChoices giả |
| `NativeFullScreenAdScreen_default_dark` | 0.5% | Như bản sáng |
| `AdGalleryScreen_default_light` | 14.7% | Native small trùng thiết kế. Lệch dự kiến: thiết kế vẽ medium mới (cao 410 dp, hàng sao, nền mờ sau media, CTA 44 dp), code giữ medium 386 dp (plan T-027, Q3; T-028 đổi), nên mọi thứ dưới thẻ medium lệch khoảng 24 dp. Hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế như T-014 |
| `AdGalleryScreen_default_dark` | 14.5% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 3.1% | Dòng native small 80 dp (cả dòng 112 dp) trùng thiết kế; phần lệch ở thẻ medium phía dưới như trên |

`AdGalleryScreen_collapsible-collapsed_light` cũng đổi (dạng thu gọn 80 dp) nhưng là ảnh thiết kế của T-030: ảnh so sánh và dòng ở bảng trên chưa ghi lại ở task này.

Native medium theo thiết kế vẽ lại (T-028, 2026-10-26): medium cao 410 dp (hàng đầu 56 dp có hàng sao, CTA 44 dp, lớp nền sau media). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `app`, chép ba ảnh so sánh vào `review/`; `review/summary.md` không đổi. Phần lệch 24 dp của medium mà T-027 ghi ở bảng trên không còn.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_default_light` | 2.0% | Thẻ medium 410 dp, vị trí tiêu đề, media, mô tả, CTA và các nút bên dưới trùng thiết kế. Khác: quảng cáo giả của màn xem thử không có điểm sao nên không vẽ sao (hàng sao vẫn giữ 16 dp), media giả che lớp nền sau media và không có nhãn "Media", "Advertiser or short body" thay "Advertiser", ô AdChoices giả không có chữ "i"; hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế như T-014 |
| `AdGalleryScreen_default_dark` | 2.0% | Như bản sáng |
| `AdGalleryScreen_inlist_light` | 1.7% | Dòng small 112 dp và dòng medium 442 dp (410 + 16 + 16) đúng vị trí; khác ở chữ quảng cáo giả như trên |

Thanh đáy và lớp native thu gọn 250 dp (T-030, 2026-10-26). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `app`, chép hai ảnh `AdGalleryScreen_collapsible-*_compare.png` vào `review/`; `review/summary.md` không đổi. Hai dòng `AdGalleryScreen_collapsible-*` ở bảng của T-014 phía trên là kết quả cũ (dạng 342 dp).

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `AdGalleryScreen_collapsible-expanded_light` | 0.7% | Lớp mở 250 dp ở y 625 tới 875, cách mép 16 dp; media 134 dp, icon 32 dp, tiêu đề 13 sp, CTA 36 dp, nút thu gọn 32 dp nền `scrim` 40% cách góc trên phải 4 dp trùng thiết kế. Khác: ô AdChoices giả (góc trên trái) không có chữ "i", media giả không có nhãn "Media", hai mép CTA lệch khoảng 1 dp (lề 12 dp tính từ mép ngoài, như các native khác) |
| `AdGalleryScreen_collapsible-collapsed_light` | 0.7% | Lớp ẩn, khe đáy 80 dp ở y 795 tới 875 trùng thiết kế; khác ở chữ "i" của ô AdChoices giả |

Đếm ngược và nút đóng 48 dp theo JBase (T-053, 2026-10-30). Ảnh `NativeFullScreenAdScreen_default_*` trong `screens/` còn vẽ nút đóng 36 dp và media từ y = 76; spec (KB-32) và ADR-0038, quyết định 1, đứng trên ảnh, chờ vẽ lại biến thể `After interstitial` ở T-064 (câu hỏi 1 của plan T-053). Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `core/ads` như T-027, chép hai ảnh so sánh vào `review/`; `review/summary.md` không đổi.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `NativeFullScreenAdScreen_default_light` | 0.6% | Khác có chủ đích (ADR-0038): nút đóng 48 dp tại (16, 24) thay 36 dp, AdChoices giả thấp xuống 6 dp vì hàng trên cao 48 dp, media bắt đầu ở y = 88 thay 76 và thấp hơn 12 dp. Đầu quảng cáo, mô tả, CTA 52 dp giữ chỗ như thiết kế; khác ở chữ "i" của AdChoices giả như T-027 |
| `NativeFullScreenAdScreen_default_dark` | 0.6% | Như bản sáng |

Native ở bước của luồng (T-032, 2026-10-26): không có ảnh thiết kế hay file `context/` cho component `Native/Step` và biến thể `Flow step` của `Native/FullScreen`, nên không chạy `design-compare`. Số đo theo mục 5.7 của luật và bố cục 250 dp của `AdGalleryScreen_collapsible-expanded_light.md` (plan T-032, câu hỏi 2). Ảnh chuẩn mới ở `core/ads/src/test/screenshots/`; người duyệt PR so với component trên trang "Components / Native ads" của Figma, lệch quá một bước lưới 4 dp thì ghi vào bảng dưới.

| Ảnh chuẩn | Nội dung (AI xem ảnh) | So với Figma (người duyệt) |
| --- | --- | --- |
| `NativeAdCard_step_light`, `_dark` | Thẻ 250 dp, lề 12 dp, media ngang lấp phần trên, ô AdChoices giả góc trên phải trên media, icon 32 dp, tiêu đề 13 sp, nhãn "Ad", CTA 36 dp rộng hết thẻ | chưa xem |
| `NativeAdPlaceholder_step_light` | Khung chờ 250 dp cùng bố cục: khối media, icon, hai vạch chữ, khối CTA 36 dp | chưa xem |
| `NativeFlowStep_card_light`, `_dark`, `_small` | Bước ví dụ, thẻ 250 dp ngoài pager ngay trên nút Tiếp 52 dp; khung thấp với chữ dài nhất: thẻ và nút Tiếp thấy đủ | chưa xem |
| `NativeFlowStep_full-page_light`, `_dark`, `_small` | Trang không nút đóng, không đếm ngược; hàng trên chỉ có AdChoices, media từ y = 76; nút Tiếp dưới vùng của trang, không bị CTA che | chưa xem |
| `NativeFlowStep_full-page-compact_light` (T-041, T-049, khung `w640dp-h360dp`) | Bố cục hàng ngang gọn, trang 276 dp: media bên trái; bên phải đầu quảng cáo, ô AdChoices giả cạnh đầu quảng cáo ở góc trên phải, mô tả (tối đa 3 dòng; bộ chữ thiết kế có 2), CTA 52 dp ở đáy; nút Tiếp dưới trang | không có trong Figma, anh Nhật duyệt 2026-10-26; ô AdChoices cạnh đầu quảng cáo duyệt 2026-10-28 (T-049) |
| `AdGalleryScreen_flow-full-page_landscape-phone` (T-049, khung `w873dp-h393dp-440dpi`, lề hệ thống giả của điện thoại) | Màn xem thử "Flow steps" ở màn ngang, lề trái 34 dp của khoét camera: trang 186,5 dp theo bố cục hàng gọn, media bên trái, đầu quảng cáo, mô tả 1 dòng, CTA 52 dp; nút Next dưới trang | không có trong Figma |
| `AdGalleryScreen_flow-full-page_too-short` (T-049, cùng khung, cỡ chữ 2.0) | Vùng trang thấp hơn mức tối thiểu: dòng "Not enough room for the full-page ad" giữa vùng thay cho vùng trắng; nút Next cùng chỗ | không có trong Figma |
| `NativeFlowStep_full-page-placeholder_light` (T-041) | Khung chờ tĩnh của trang theo bố cục cột: khối media, khối icon và hai vạch chữ, khối CTA 52 dp; không chữ, không nhãn "Ad"; nút Tiếp dưới trang | không có trong Figma, anh Nhật duyệt 2026-10-26 |

Thiết kế vẽ lại theo bề rộng màn hình (T-064, 2026-11-14), so với ảnh chuẩn screenshot hiện có. Code chưa đổi (T-065), nên phần khác là lệch dự kiến giữa thiết kế mới và code của đợt T-035, T-053. Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `core/ads`, `feature/sample` và `app` như T-027, chép ảnh so sánh vào `review/`; `review/summary.md` không đổi. `SampleScreen_ready_light_unfolded` chưa có ảnh chuẩn screenshot (T-065 thêm).

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `SampleScreen_ready_light`, `_dark` | 3.0%, 2.9% | Khe đáy thiết kế 126 dp (y 749 tới 875), code 80 dp (y 795 tới 875); icon, nhãn "Ad", CTA theo số mới |
| `SampleScreen_ad-loading_light` | 1.0% | Khung chờ 126 dp so với 80 dp |
| `AdGalleryScreen_default_light`, `_dark` | 21.0%, 21.0% | Small 126 dp và medium 400 dp đẩy mọi thứ bên dưới lệch so với code (small 80, medium 410) |
| `AdGalleryScreen_inlist_light` | 4.9% | Dòng small và medium theo chiều cao mới |
| `AdGalleryScreen_collapsible-expanded_light` | 5.4% | Lớp mở 299 dp so với 250 dp; nút thu gọn 27 dp góc trên trái, AdChoices góc trên phải (code ngược lại) |
| `AdGalleryScreen_collapsible-collapsed_light` | 2.4% | Khe đáy 126 dp so với 80 dp |
| `NativeFullScreenAdScreen_default_light`, `_dark` | 0.5%, 0.5% | Nút đóng 48 dp tại (16, 24) và media từ y = 88 nay trùng code (T-053); còn khác ở chữ "i" của AdChoices giả |

Kích thước native theo JBase (T-065, 2026-11-16), so với thiết kế T-064. Chạy thẳng `scripts/design-compare/DesignCompare.java` cho `feature/sample` và `app` như T-027, chép chín ảnh so sánh vào `review/`; `review/summary.md` không đổi. Bảng T-064 ngay trên là lệch dự kiến trước khi có code, nay không còn.

| Màn hình | Điểm ảnh khác | Nhận xét |
| --- | --- | --- |
| `SampleScreen_ready_light`, `_dark` | 1.8%, 1.8% | Khe đáy 126 dp ở y 749 tới 875, icon 44 dp, nhãn "Ad" trước tiêu đề 16 sp, CTA 44 dp chữ 18 sp trùng thiết kế. Khác: dòng dưới tiêu đề là nhà quảng cáo của quảng cáo giả ("Advertiser or short body", 1 dòng) thay mô tả 2 dòng của thiết kế (plan T-065, câu hỏi 9), nên hàng tiêu đề thấp hơn khoảng 9 dp vì cột chữ căn giữa; nút "Tap" thấp hơn khoảng 2 dp như T-027; ô AdChoices giả không có chữ "i" |
| `SampleScreen_ready_light_unfolded` | 1.2% | Ảnh chuẩn mới: khe đáy 200 dp ở màn 673 dp trùng thiết kế; khác ở dòng dưới tiêu đề và chữ "i" như trên |
| `SampleScreen_ad-loading_light` | 0.9% | Khung chờ 126 dp: khối icon 44 dp, hai vạch, khối CTA 96 x 44 dp trùng thiết kế |
| `AdGalleryScreen_default_light`, `_dark` | 2.7%, 2.7% | Small 126 dp, medium 400 dp (đầu thẻ 60 dp, media từ y = 383, CTA 44 dp chữ 18 sp) và các nút bên dưới trùng vị trí. Khác: quảng cáo giả của màn xem thử không có điểm sao (hàng sao giữ 16 dp, không vẽ sao), media giả không có nền mờ và nhãn "Media", dòng dưới tiêu đề của small như trên; hàng nút "In-list ads", "Collapsible ad" ngoài thiết kế như T-014 |
| `AdGalleryScreen_inlist_light` | 2.0% | Dòng small 158 dp, dòng medium 432 dp đúng vị trí; khác ở quảng cáo giả như trên |
| `AdGalleryScreen_collapsible-expanded_light` | 1.2% | Lớp mở 299 dp ở y 576 tới 875: vùng ảnh 173 dp (MediaView 161 dp), nội dung như native nhỏ 126 dp, nút thu gọn 27 dp tại (4, 4) góc trên trái, AdChoices góc trên phải trùng thiết kế. Khác: dòng dưới tiêu đề "Advertiser" thay mô tả 2 dòng, ô AdChoices giả không có chữ "i", media giả không có nhãn "Media"; media lệch khoảng 1 dp vì lề tính từ mép ngoài như các native khác |
| `AdGalleryScreen_collapsible-collapsed_light` | 1.2% | Khe đáy 126 dp ở y 749 tới 875 trùng thiết kế; khác ở dòng dưới tiêu đề và chữ "i" như trên |

Lệch theo chiều cao của khe đáy, lớp thu gọn và medium đều dưới 4 dp (mục 7 của plan T-065).
