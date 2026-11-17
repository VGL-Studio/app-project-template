# SỐ LIỆU THIẾT KẾ: AdGalleryScreen_collapsible-expanded_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=6-204
- Frame: `6:204`, giao diện sáng
- Ngày lấy: 2026-11-17

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 4 | xs |
| khoảng cách | 6 | ngoài hệ thống |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 500 | labelMedium |
| kiểu chữ | 13/18, đậm 400 | ngoài hệ thống |
| kiểu chữ | 13/18, đậm 500 | ngoài hệ thống |
| kiểu chữ | 16/22, đậm 500 | ngoài hệ thống |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 18/24, đậm 500 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 400 | titleLarge |
| màu chữ | #001F2A | onTertiaryContainer |
| màu chữ | #17352F | onBackground, onSurface |
| màu chữ | #6D8079 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #000000 (độ mờ 0.40, lớp tô 0.40) | ngoài hệ thống |
| màu nền | #3F6374 | tertiary |
| màu nền | #C3E8FB | tertiaryContainer |
| màu nền | #CEE9DE | secondaryContainer |
| màu nền | #DFEAE4 | surfaceVariant, surfaceContainerHighest |
| màu nền | #EBF1EE | surfaceContainer |
| màu nền | #F1F6F3 | surfaceContainerLow |
| màu nền | #F7FAF8 | background, surface, surfaceBright |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "AdGalleryScreen_collapsible-expanded_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright)
  - Text "Title", vị trí 16,20, cỡ 147 x 28, chữ "Collapsible ad", kiểu titleLarge (22/28, đậm 400), màu #17352F (onBackground, onSurface)
  - Frame "Content", vị trí 16,72, cỡ 379 x 342, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "Item 1", vị trí 16,72, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,88, cỡ 379 x 24, chữ "List item 1", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 2", vị trí 16,129, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,145, cỡ 379 x 24, chữ "List item 2", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 3", vị trí 16,186, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,202, cỡ 379 x 24, chữ "List item 3", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 4", vị trí 16,243, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,259, cỡ 379 x 24, chữ "List item 4", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 5", vị trí 16,300, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,316, cỡ 379 x 24, chữ "List item 5", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 6", vị trí 16,357, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,373, cỡ 379 x 24, chữ "List item 6", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
  - Instance "Native/Collapsible", vị trí 16,576, cỡ 379 x 299, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "Extra area (media)", vị trí 16,576, cỡ 379 x 193, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 12 (ngoài hệ thống)/12 (ngoài hệ thống)/0/12 (ngoài hệ thống), căn MIN/MIN
      - Frame "Media (MediaView)", vị trí 28,588, cỡ 355 x 181, nền #EBF1EE (surfaceContainer), bo góc 8
        - Text "Media label", vị trí 64,596, cỡ 36 x 16, chữ "Media", kiểu labelMedium (12/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Content (as small slot)", vị trí 16,769, cỡ 379 x 106, xếp dọc, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 6 (ngoài hệ thống)/12 (ngoài hệ thống)/6 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
      - Frame "Top row", vị trí 28,775, cỡ 355 x 44, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/16 (md)/0/0, căn MIN/CENTER
        - Instance "Ad/Icon asset", vị trí 28,775, cỡ 44 x 44, nền #CEE9DE (secondaryContainer), bo góc 8
        - Frame "Texts", vị trí 84,775, cỡ 283 x 44, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
          - Frame "Title row", vị trí 84,775, cỡ 283 x 22, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
            - Instance "Ad/Badge", vị trí 84,777, cỡ 26 x 18, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
              - Text "Label", vị trí 88,777, cỡ 18 x 18, chữ "Ad", kiểu ngoài hệ thống (13/18, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
            - Text "Headline", vị trí 116,775, cỡ 251 x 22, chữ "Headline of the ad", kiểu ngoài hệ thống (16/22, đậm 500), màu #17352F (onBackground, onSurface)
          - Text "Body", vị trí 84,801, cỡ 283 x 18, chữ "Short description of the ad", kiểu ngoài hệ thống (13/18, đậm 400), màu #6D8079 (onSurfaceVariant)
      - Instance "Ad/CTA", vị trí 28,825, cỡ 355 x 44, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
        - Text "Label", vị trí 180,835, cỡ 52 x 24, chữ "Install", kiểu ngoài hệ thống (18/24, đậm 500), màu #001F2A (onTertiaryContainer)
    - Instance "Ad/AdChoices (SDK)", vị trí 375,580, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
      - Text "i", vị trí 381,580, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Collapse button (27 dp, touch 48 dp)", vị trí 20,580, cỡ 27 x 27, nền #000000 (độ mờ 0.40, lớp tô 0.40) (ngoài hệ thống), bo góc 14, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Instance "Icon/chevron-down", vị trí 25,585, cỡ 17 x 17
        - Vector "Vector", vị trí 29,591, cỡ 8 x 4, viền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), dày 2.0

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
