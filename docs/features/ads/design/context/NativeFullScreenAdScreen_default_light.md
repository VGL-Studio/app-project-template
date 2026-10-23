# SỐ LIỆU THIẾT KẾ: NativeFullScreenAdScreen_default_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=6-272
- Frame: `6:272`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 32 | xl |
| khoảng cách | 4 | xs |
| khoảng cách | 6 | ngoài hệ thống |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 12/16, đậm 500 | labelMedium |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| màu chữ | #001F2A | onTertiaryContainer |
| màu chữ | #17352F | onBackground, onSurface |
| màu chữ | #6D8079 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #3F6374 | tertiary |
| màu nền | #C3E8FB | tertiaryContainer |
| màu nền | #CEE9DE | secondaryContainer |
| màu nền | #DFEAE4 | surfaceVariant, surfaceContainerHighest |
| màu nền | #E5ECE8 | surfaceContainerHigh |
| màu nền | #EBF1EE | surfaceContainer |
| màu nền | #F7FAF8 | background, surface, surfaceBright |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "NativeFullScreenAdScreen_default_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright)
  - Instance "Native/FullScreen", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (background, surface, surfaceBright), xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 24 (lg)/16 (md)/32 (xl)/16 (md), căn MIN/MIN
    - Frame "Top bar", vị trí 16,24, cỡ 379 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/CENTER
      - Frame "Close button (48 dp)", vị trí 16,24, cỡ 48 x 48, nền #E5ECE8 (surfaceContainerHigh), bo góc 24, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Instance "Icon/close", vị trí 28,36, cỡ 24 x 24
          - Vector "Vector", vị trí 34,42, cỡ 12 x 12, viền #17352F (onBackground, onSurface), dày 2.0
      - Instance "Ad/AdChoices (SDK)", vị trí 379,40, cỡ 16 x 16, nền #DFEAE4 (surfaceVariant, surfaceContainerHighest), bo góc 4
        - Text "i", vị trí 385,40, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Media (MediaView)", vị trí 16,88, cỡ 379 x 587, nền #EBF1EE (surfaceContainer), bo góc 16
      - Text "Media label", vị trí 32,100, cỡ 36 x 16, chữ "Media", kiểu labelMedium (12/16, đậm 500), màu #6D8079 (onSurfaceVariant)
    - Frame "Header", vị trí 16,691, cỡ 379 x 48, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Instance "Ad/Icon asset", vị trí 16,691, cỡ 48 x 48, nền #CEE9DE (secondaryContainer), bo góc 8
      - Frame "Texts", vị trí 76,693, cỡ 319 x 44, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "Headline", vị trí 76,693, cỡ 319 x 24, chữ "Headline of the ad", kiểu titleMedium (16/24, đậm 600), màu #17352F (onBackground, onSurface)
        - Frame "Attribution", vị trí 76,721, cỡ 88 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
          - Instance "Ad/Badge", vị trí 76,721, cỡ 23 x 16, nền #3F6374 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
            - Text "Label", vị trí 80,721, cỡ 15 x 16, chữ "Ad", kiểu labelSmall (11/16, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
          - Text "Advertiser", vị trí 105,721, cỡ 59 x 16, chữ "Advertiser", kiểu bodySmall (12/16, đậm 400), màu #6D8079 (onSurfaceVariant)
    - Text "Body", vị trí 16,755, cỡ 379 x 40, chữ "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.", kiểu bodyMedium (14/20, đậm 400), màu #6D8079 (onSurfaceVariant)
    - Instance "Ad/CTA", vị trí 16,811, cỡ 379 x 48, nền #C3E8FB (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Label", vị trí 186,825, cỡ 40 x 20, chữ "Install", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #001F2A (onTertiaryContainer)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
