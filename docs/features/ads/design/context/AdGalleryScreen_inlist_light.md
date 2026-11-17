# SỐ LIỆU THIẾT KẾ: AdGalleryScreen_inlist_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=6-157
- Frame: `6:157`, giao diện sáng
- Ngày lấy: 2026-11-17

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
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
| màu nền | #3F6374 | tertiary |
| màu nền | #C3D1CA | outlineVariant |
| màu nền | #C3E8FB | tertiaryContainer |
| màu nền | #CEE9DE | secondaryContainer |
| màu nền | #DFEAE4 | surfaceVariant, surfaceContainerHighest |
| màu nền | #EBF1EE | surfaceContainer |
| màu nền | #F1F6F3 | surfaceContainerLow |
| màu nền | #F7FAF8 | background, surface, surfaceBright |
| độ mờ của lớp | 0.60 | - |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "AdGalleryScreen_inlist_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright)
  - Text "Title", vị trí 16,20, cỡ 266 x 28, chữ "In-list ads (every 3 items)", kiểu titleLarge (22/28, đậm 400), màu #17352F (onBackground, onSurface)
  - Frame "Content", vị trí 16,72, cỡ 379 x 819, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "Item 1", vị trí 16,72, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,88, cỡ 379 x 24, chữ "List item 1", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 2", vị trí 16,129, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,145, cỡ 379 x 24, chữ "List item 2", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 3", vị trí 16,186, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,202, cỡ 379 x 24, chữ "List item 3", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "In-list ad slot", vị trí 16,243, cỡ 379 x 138, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/MIN
      - Instance "Native/Small", vị trí 16,259, cỡ 379 x 106, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 6 (ngoài hệ thống)/12 (ngoài hệ thống)/6 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
        - Frame "Top row", vị trí 28,265, cỡ 355 x 44, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/16 (md)/0/0, căn MIN/CENTER
          - Instance "Ad/Icon asset", vị trí 28,265, cỡ 44 x 44, nền #CEE9DE (secondaryContainer), bo góc 8
          - Frame "Texts", vị trí 84,265, cỡ 283 x 44, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
            - Frame "Title row", vị trí 84,265, cỡ 283 x 22, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
              - Instance "Ad/Badge", vị trí 84,267, cỡ 26 x 18, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
                - Text "Label", vị trí 88,267, cỡ 18 x 18, chữ "Ad", kiểu ngoài hệ thống (13/18, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
              - Text "Headline", vị trí 116,265, cỡ 251 x 22, chữ "Headline of the ad", kiểu ngoài hệ thống (16/22, đậm 500), màu #17352F (onBackground, onSurface)
            - Text "Body", vị trí 84,291, cỡ 283 x 18, chữ "Short description of the ad", kiểu ngoài hệ thống (13/18, đậm 400), màu #6D8079 (onSurfaceVariant)
        - Instance "Ad/CTA", vị trí 28,315, cỡ 355 x 44, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
          - Text "Label", vị trí 180,325, cỡ 52 x 24, chữ "Install", kiểu ngoài hệ thống (18/24, đậm 500), màu #001F2A (onTertiaryContainer)
        - Instance "Ad/AdChoices (SDK)", vị trí 375,263, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
          - Text "i", vị trí 381,263, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Item 4", vị trí 16,381, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,397, cỡ 379 x 24, chữ "List item 4", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 5", vị trí 16,438, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,454, cỡ 379 x 24, chữ "List item 5", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "Item 6", vị trí 16,495, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,511, cỡ 379 x 24, chữ "List item 6", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)
    - Frame "In-list ad slot", vị trí 16,552, cỡ 379 x 282, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/MIN
      - Instance "Native/Medium", vị trí 16,568, cỡ 379 x 250, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "Media (MediaView, min 120 x 120 dp)", vị trí 16,568, cỡ 379 x 144, nền #EBF1EE (surfaceContainer), bo góc 8
          - Rectangle "Blurred backdrop", vị trí 16,568, cỡ 379 x 144, nền #C3D1CA (outlineVariant), độ mờ của lớp 0.60
          - Text "Media label", vị trí 28,576, cỡ 36 x 16, chữ "Media", kiểu labelMedium (12/16, đậm 500), màu #6D8079 (onSurfaceVariant)
        - Frame "Content (as small)", vị trí 16,712, cỡ 379 x 106, xếp dọc, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 6 (ngoài hệ thống)/12 (ngoài hệ thống)/6 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
          - Frame "Top row", vị trí 28,718, cỡ 355 x 44, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
            - Instance "Ad/Icon asset", vị trí 28,718, cỡ 44 x 44, nền #CEE9DE (secondaryContainer), bo góc 8
            - Frame "Texts", vị trí 84,718, cỡ 299 x 44, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
              - Frame "Title row", vị trí 84,718, cỡ 299 x 22, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
                - Instance "Ad/Badge", vị trí 84,720, cỡ 26 x 18, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
                  - Text "Label", vị trí 88,720, cỡ 18 x 18, chữ "Ad", kiểu ngoài hệ thống (13/18, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
                - Text "Headline", vị trí 116,718, cỡ 267 x 22, chữ "Headline of the ad", kiểu ngoài hệ thống (16/22, đậm 500), màu #17352F (onBackground, onSurface)
              - Frame "Advertiser row", vị trí 84,744, cỡ 299 x 18, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
                - Text "Advertiser", vị trí 84,744, cỡ 223 x 18, chữ "Advertiser", kiểu ngoài hệ thống (13/18, đậm 400), màu #6D8079 (onSurfaceVariant)
                - Frame "Rating", vị trí 313,745, cỡ 70 x 16, xếp ngang, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
                  - Instance "Icon/star", vị trí 313,745, cỡ 16 x 16
                    - Star "Vector", vị trí 314,746, cỡ 14 x 14, nền #3F6374 (tertiary)
                  - Instance "Icon/star", vị trí 331,745, cỡ 16 x 16
                    - Star "Vector", vị trí 332,746, cỡ 14 x 14, nền #3F6374 (tertiary)
                  - Instance "Icon/star", vị trí 349,745, cỡ 16 x 16
                    - Star "Vector", vị trí 350,746, cỡ 14 x 14, nền #3F6374 (tertiary)
                  - Instance "Icon/star", vị trí 367,745, cỡ 16 x 16
                    - Star "Vector", vị trí 368,746, cỡ 14 x 14, nền #3F6374 (tertiary)
          - Instance "Ad/CTA", vị trí 28,768, cỡ 355 x 44, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
            - Text "Label", vị trí 180,778, cỡ 52 x 24, chữ "Install", kiểu ngoài hệ thống (18/24, đậm 500), màu #001F2A (onTertiaryContainer)
        - Instance "Ad/AdChoices (SDK)", vị trí 375,572, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
          - Text "i", vị trí 381,572, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Item 7", vị trí 16,834, cỡ 379 x 57, viền #C3D1CA (outlineVariant), dày 1.0, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 16 (md)/0/16 (md)/0, căn MIN/CENTER
      - Text "Label", vị trí 16,850, cỡ 379 x 24, chữ "List item 7", kiểu bodyLarge (16/24, đậm 400), màu #17352F (onBackground, onSurface)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
