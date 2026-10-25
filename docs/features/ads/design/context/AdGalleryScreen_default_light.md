# SỐ LIỆU THIẾT KẾ: AdGalleryScreen_default_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=6-74
- Frame: `6:74`, giao diện sáng
- Ngày lấy: 2026-10-25

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
| kiểu chữ | 10/16, đậm 500 | ngoài hệ thống |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 12/16, đậm 500 | labelMedium |
| kiểu chữ | 13/20, đậm 500 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 14/20, đậm 600 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 400 | titleLarge |
| màu chữ | #001F2A | onTertiaryContainer |
| màu chữ | #17352F | onBackground, onSurface |
| màu chữ | #6D8079 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #147D68 | primary, surfaceTint |
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

- Frame "AdGalleryScreen_default_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright)
  - Text "Title", vị trí 16,20, cỡ 204 x 28, chữ "Ad gallery (staging)", kiểu titleLarge (22/28, đậm 400), màu #17352F (onBackground, onSurface)
  - Frame "Content", vị trí 16,72, cỡ 379 x 714, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Text "Section", vị trí 16,72, cỡ 82 x 20, chữ "Native small", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Instance "Native/Small", vị trí 16,108, cỡ 379 x 80, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/24 (lg)/0/12 (ngoài hệ thống), căn MIN/CENTER
      - Instance "Ad/Icon asset", vị trí 29,130, cỡ 36 x 36, nền #CEE9DE (secondaryContainer), bo góc 8
      - Frame "Texts", vị trí 77,129, cỡ 201 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "Headline", vị trí 77,129, cỡ 201 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #17352F (onBackground, onSurface)
        - Frame "Attribution", vị trí 77,151, cỡ 201 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
          - Instance "Ad/Badge", vị trí 77,151, cỡ 22 x 16, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
            - Text "Label", vị trí 81,151, cỡ 14 x 16, chữ "Ad", kiểu ngoài hệ thống (10/16, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
          - Text "Body", vị trí 105,151, cỡ 173 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #6D8079 (onSurfaceVariant)
      - Instance "Ad/CTA", vị trí 290,131, cỡ 80 x 34, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
        - Text "Label", vị trí 312,138, cỡ 37 x 20, chữ "Install", kiểu ngoài hệ thống (13/20, đậm 500), màu #001F2A (onTertiaryContainer)
      - Instance "Ad/AdChoices (SDK)", vị trí 375,112, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
        - Text "i", vị trí 381,112, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Text "Section", vị trí 16,204, cỡ 101 x 20, chữ "Native medium", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Instance "Native/Medium", vị trí 16,240, cỡ 379 x 410, nền #F1F6F3 (surfaceContainerLow), viền #C3D1CA (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 24 (lg)/12 (ngoài hệ thống)/12 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
      - Frame "Header", vị trí 29,265, cỡ 353 x 56, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Instance "Ad/Icon asset", vị trí 29,273, cỡ 40 x 40, nền #CEE9DE (secondaryContainer), bo góc 8
        - Frame "Texts", vị trí 81,265, cỡ 301 x 56, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
          - Text "Headline", vị trí 81,265, cỡ 301 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #17352F (onBackground, onSurface)
          - Frame "Attribution", vị trí 81,287, cỡ 301 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
            - Instance "Ad/Badge", vị trí 81,287, cỡ 23 x 16, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
              - Text "Label", vị trí 85,287, cỡ 15 x 16, chữ "Ad", kiểu labelSmall (11/16, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
            - Text "Advertiser", vị trí 110,287, cỡ 272 x 16, chữ "Advertiser", kiểu bodySmall (12/16, đậm 400), màu #6D8079 (onSurfaceVariant)
          - Frame "Rating", vị trí 81,305, cỡ 70 x 16, xếp ngang, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
            - Instance "Icon/star", vị trí 81,305, cỡ 16 x 16
              - Star "Vector", vị trí 82,306, cỡ 14 x 14, nền #3F6374 (tertiary)
            - Instance "Icon/star", vị trí 99,305, cỡ 16 x 16
              - Star "Vector", vị trí 100,306, cỡ 14 x 14, nền #3F6374 (tertiary)
            - Instance "Icon/star", vị trí 117,305, cỡ 16 x 16
              - Star "Vector", vị trí 118,306, cỡ 14 x 14, nền #3F6374 (tertiary)
            - Instance "Icon/star", vị trí 135,305, cỡ 16 x 16
              - Star "Vector", vị trí 136,306, cỡ 14 x 14, nền #3F6374 (tertiary)
      - Frame "Media (MediaView, min 120 x 120 dp)", vị trí 29,333, cỡ 353 x 196, nền #EBF1EE (surfaceContainer), bo góc 8
        - Rectangle "Blurred backdrop", vị trí 29,333, cỡ 353 x 196, nền #C3D1CA (outlineVariant), độ mờ của lớp 0.60
        - Text "Media label", vị trí 41,341, cỡ 36 x 16, chữ "Media", kiểu labelMedium (12/16, đậm 500), màu #6D8079 (onSurfaceVariant)
      - Text "Body", vị trí 29,541, cỡ 353 x 40, chữ "Body text of the ad, two lines at most so the call to action stays in view.", kiểu bodyMedium (14/20, đậm 400), màu #6D8079 (onSurfaceVariant)
      - Instance "Ad/CTA", vị trí 29,593, cỡ 353 x 44, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
        - Text "Label", vị trí 186,605, cỡ 40 x 20, chữ "Install", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #001F2A (onTertiaryContainer)
      - Instance "Ad/AdChoices (SDK)", vị trí 375,244, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
        - Text "i", vị trí 381,244, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Full-screen buttons", vị trí 16,666, cỡ 379 x 52, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "Button/Primary", vị trí 16,666, cỡ 184 x 52, nền #147D68 (primary, surfaceTint), bo góc 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/24 (lg)/0/24 (lg), căn CENTER/CENTER
        - Text "Label", vị trí 73,682, cỡ 69 x 20, chữ "Interstitial", kiểu ngoài hệ thống (14/20, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
      - Instance "Button/Primary", vị trí 212,666, cỡ 184 x 52, nền #147D68 (primary, surfaceTint), bo góc 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/24 (lg)/0/24 (lg), căn CENTER/CENTER
        - Text "Label", vị trí 244,682, cỡ 118 x 20, chữ "Native full screen", kiểu ngoài hệ thống (14/20, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
    - Instance "Button/Primary", vị trí 16,734, cỡ 379 x 52, nền #147D68 (primary, surfaceTint), bo góc 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/24 (lg)/0/24 (lg), căn CENTER/CENTER
      - Text "Label", vị trí 142,750, cỡ 127 x 20, chữ "Open Ad Inspector", kiểu ngoài hệ thống (14/20, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
