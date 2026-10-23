# SỐ LIỆU THIẾT KẾ: AdGalleryScreen_collapsible-collapsed_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=6-242
- Frame: `6:242`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 6 | ngoài hệ thống |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 22/28, đậm 400 | titleLarge |
| màu chữ | #001F2A | onTertiaryContainer |
| màu chữ | #17352F | onBackground, onSurface |
| màu chữ | #6D8079 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #3F6374 | tertiary |
| màu nền | #C3E8FB | tertiaryContainer |
| màu nền | #CEE9DE | secondaryContainer |
| màu nền | #DFEAE4 | surfaceVariant, surfaceContainerHighest |
| màu nền | #F1F6F3 | surfaceContainerLow |
| màu nền | #F7FAF8 | background, surface, surfaceBright |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "AdGalleryScreen_collapsible-collapsed_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright)
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
  - Instance "Native/Collapsible", vị trí 16,803, cỡ 379 x 72, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/24 (lg)/0/12 (ngoài hệ thống), căn MIN/CENTER
    - Instance "Ad/Icon asset", vị trí 29,819, cỡ 40 x 40, nền #CEE9DE (secondaryContainer), bo góc 8
    - Frame "Texts", vị trí 81,820, cỡ 197 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "Headline", vị trí 81,820, cỡ 197 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #17352F (onBackground, onSurface)
      - Frame "Attribution", vị trí 81,842, cỡ 197 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Instance "Ad/Badge", vị trí 81,842, cỡ 23 x 16, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
          - Text "Label", vị trí 85,842, cỡ 15 x 16, chữ "Ad", kiểu labelSmall (11/16, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
        - Text "Body", vị trí 110,842, cỡ 168 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #6D8079 (onSurfaceVariant)
    - Instance "Ad/CTA", vị trí 290,821, cỡ 80 x 36, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Label", vị trí 310,829, cỡ 40 x 20, chữ "Install", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #001F2A (onTertiaryContainer)
    - Instance "Ad/AdChoices (SDK)", vị trí 375,807, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
      - Text "i", vị trí 381,807, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
