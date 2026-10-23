# SỐ LIỆU THIẾT KẾ: SampleScreen_ready_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/jDs7KImtrNL3ABPYnDhDgJ/?node-id=5-24
- Frame: `5:24`, giao diện tối
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
| kiểu chữ | 14/20, đậm 600 | ngoài hệ thống |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 28/36, đậm 400 | headlineMedium |
| kiểu chữ | 45/52, đậm 400 | displayMedium |
| màu chữ | #0A3445 | onTertiary |
| màu chữ | #101F1D | onPrimary, background, surface, surfaceDim |
| màu chữ | #96ADA5 | onSurfaceVariant |
| màu chữ | #C3E8FB | onTertiaryContainer |
| màu chữ | #EFF8F4 | onBackground, onSurface, inverseSurface |
| màu nền | #101F1D | onPrimary, background, surface, surfaceDim |
| màu nền | #14241F | surfaceContainerLow |
| màu nền | #264B5B | tertiaryContainer |
| màu nền | #293D36 | surfaceVariant, surfaceContainerHighest |
| màu nền | #344C44 | secondaryContainer |
| màu nền | #84DCC1 | primary, surfaceTint |
| màu nền | #A7CCE0 | tertiary |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "SampleScreen_ready_dark", vị trí 0,0, cỡ 411 x 891, nền #101F1D (onPrimary, background, surface, surfaceDim)
  - Frame "Content", vị trí 16,16, cỡ 379 x 212, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Text "Title", vị trí 105,16, cỡ 201 x 36, chữ "Sample feature", kiểu headlineMedium (28/36, đậm 400), màu #EFF8F4 (onBackground, onSurface, inverseSurface)
    - Text "Count label", vị trí 154,68, cỡ 104 x 24, chữ "Times tapped", kiểu bodyLarge (16/24, đậm 400), màu #EFF8F4 (onBackground, onSurface, inverseSurface)
    - Text "Count", vị trí 191,108, cỡ 29 x 52, chữ "3", kiểu displayMedium (45/52, đậm 400), màu #EFF8F4 (onBackground, onSurface, inverseSurface)
    - Instance "Button/Primary", vị trí 16,176, cỡ 379 x 52, nền #84DCC1 (primary, surfaceTint), bo góc 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/24 (lg)/0/24 (lg), căn CENTER/CENTER
      - Text "Label", vị trí 193,192, cỡ 25 x 20, chữ "Tap", kiểu ngoài hệ thống (14/20, đậm 600), màu #101F1D (onPrimary, background, surface, surfaceDim)
  - Instance "Native small (sample_native_small)", vị trí 16,803, cỡ 379 x 72, nền #14241F (surfaceContainerLow), viền #3F534C (outlineVariant), dày 1.0, bo góc 12, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/24 (lg)/0/12 (ngoài hệ thống), căn MIN/CENTER
    - Instance "Ad/Icon asset", vị trí 29,819, cỡ 40 x 40, nền #344C44 (secondaryContainer), bo góc 8
    - Frame "Texts", vị trí 81,820, cỡ 197 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "Headline", vị trí 81,820, cỡ 197 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #EFF8F4 (onBackground, onSurface, inverseSurface)
      - Frame "Attribution", vị trí 81,842, cỡ 197 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Instance "Ad/Badge", vị trí 81,842, cỡ 23 x 16, nền #A7CCE0 (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/MIN
          - Text "Label", vị trí 85,842, cỡ 15 x 16, chữ "Ad", kiểu labelSmall (11/16, đậm 500), màu #0A3445 (onTertiary)
        - Text "Body", vị trí 110,842, cỡ 168 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #96ADA5 (onSurfaceVariant)
    - Instance "Ad/CTA", vị trí 290,821, cỡ 80 x 36, nền #264B5B (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Label", vị trí 310,829, cỡ 40 x 20, chữ "Install", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #C3E8FB (onTertiaryContainer)
    - Instance "Ad/AdChoices (SDK)", vị trí 375,807, cỡ 16 x 16, nền #293D36 (surfaceVariant, surfaceContainerHighest), bo góc 4
      - Text "i", vị trí 381,807, cỡ 3 x 16, chữ "i", kiểu labelSmall (11/16, đậm 500), màu #96ADA5 (onSurfaceVariant)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
