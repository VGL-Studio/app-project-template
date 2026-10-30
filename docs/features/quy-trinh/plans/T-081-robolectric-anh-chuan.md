---
ticket: T-081
title: Robolectric 4.17, không ảnh chuẩn nào đổi
status: draft
version: 1
risk_score: 7
risk_level: high
token_estimate: 5.4M
blocking_points: [build, sdk]
run_order:
reviewer:
cross_reviewer:
approved_at:
lane:
---

# PLAN T-081: ROBOLECTRIC 4.17, KHÔNG ẢNH CHUẨN NÀO ĐỔI

## 1. Tóm tắt

Plan nâng Robolectric từ 4.15.1 lên 4.17 bằng một dòng trong `gradle/libs.versions.toml`. Lần dựng thử trên main `661dbfa` (đã có T-080) cho kết quả: `scripts/check.sh` qua với 1498 test JVM và 89 ảnh chuẩn. Ghi lại ảnh chuẩn bằng Robolectric 4.17 cho ra 89 ảnh giống ảnh cũ từng byte, trừ một ảnh. Ảnh đó lệch vì một lý do có sẵn, không phải do Robolectric: bộ build trước T-080, với Robolectric 4.15.1, cũng vẽ ra đúng ảnh lệch đó (mục 4.1). Vì vậy plan không ghi lại ảnh chuẩn nào. Đây là bước 3 trong 3 bước tách từ T-052 (L-127, issue #276). Theo T-080 (plan mục 8, câu 1), Roborazzi đã lên 1.77.0 rồi, nên ticket T-081 chỉ còn Robolectric.

## 2. Hiểu yêu cầu

Ticket quy trình, không có spec nghiệp vụ. Yêu cầu là L-127, cộng phạm vi ticket T-081 đã sửa ở PR #320.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Định nghĩa hoàn thành 1 | `scripts/check.sh` qua với ảnh chuẩn mới | `check.sh` qua với Robolectric 4.17. Không có ảnh nào đổi do Robolectric, nên "ảnh chuẩn mới" chính là 89 ảnh hiện có. Số test không giảm so với `main-guard` gần nhất |
| Định nghĩa hoàn thành 2 | Danh sách ảnh đổi và mức lệch có trong plan, người duyệt đã xem từng ảnh | Danh sách ở mục 4.1: 0 ảnh đổi do Robolectric. Có 1 ảnh lệch có sẵn, ghi số đo; người duyệt xem ảnh đó (mục 4, kiểm tay) |
| Định nghĩa hoàn thành 3 | Không test nào bị nới câu khẳng định | Không sửa test nào |
| Định nghĩa hoàn thành 4 | CI qua, đã merge | Như mọi PR |
| Phạm vi: test đo màu, kích thước đỏ vì cách vẽ mới | Tìm nguyên nhân, chỉ sửa phần dựng test (ADR-0033/L11) | Lần thử: không test nào đỏ |
| Phạm vi: `CLAUDE.md` phần test | Sửa nếu quy tắc về `GraphicsMode`, `captureRoboImage` hay khung chụp đổi | Không đổi: mọi test của các quy tắc đó qua nguyên vẹn, ảnh giống từng byte ngoài ảnh lệch có sẵn (mục 3.1) |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0011 (cổng chất lượng, ảnh chuẩn không cập nhật ngoài plan), ADR-0040 (bộ build; không ghi phiên bản Robolectric, không cần sửa), ADR-0029, ADR-0033.

## 3. Giải pháp

### 3.1. Cách làm

0. Trước khi sửa gì, trên main mới nhất (Robolectric 4.15.1): chạy lệnh 3 của mục 4. Đây là lần đo chuẩn để so với lần đo sau khi đổi phiên bản.
1. `gradle/libs.versions.toml` (file được bảo vệ; plan nêu rõ để phiên được sửa): đổi `robolectric = "4.15.1"` thành `robolectric = "4.17"`. Không đổi gì khác. Thư viện `robolectric` chỉ được thêm ở `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt` (`testImplementation`), nên không cần sửa file build nào.
2. Không ghi lại ảnh chuẩn. Chạy lệnh 3 lần nữa (nhãn 4.17), rồi lệnh 3b. Lệnh ghi ảnh ở mục 4 chỉ để đo, không commit. Sau lệnh đó, `git checkout -- '*/src/test/screenshots/*'` đưa ảnh về như cũ. Pathspec phải có `*` ở cuối: `'*/src/test/screenshots/'` (dấu `/` cuối, không `*`) không khớp file nào, nên `git status` và `git checkout` với nó không làm gì mà vẫn không báo lỗi (mục 4.1, kết quả 7).
3. `robolectric.properties` của các module giữ `sdk=35`. Robolectric 4.17 tải `android-all-instrumented` bản mới cho SDK 35 lúc test chạy lần đầu. Trên máy dev lần thử đã tải được. Phiên cloud tải qua mirror Maven Central mà `cloud-setup.sh` đặt (`robolectric.dependency.repo.url`); CI tải từ Maven Central.
4. Ticket `T-081-robolectric-roborazzi-anh-chuan.md`: `plan:`, trạng thái theo plan, cỡ `size: S` (lần thử cho thấy không có ảnh nào phải ghi lại); tiêu đề và Mô tả theo kết quả: "Robolectric 4.17, không ảnh chuẩn nào đổi", bỏ câu "ghi lại các ảnh chuẩn screenshot đổi theo cách vẽ mới"; định nghĩa hoàn thành 1 và 2 ghi "không ảnh nào đổi, đo bằng lệnh của plan mục 4".

**Ảnh lệch có sẵn** (không thuộc plan, ghi để người biết): `core/designsystem/src/test/screenshots/DesignSystem_spacing_components_light.png`, ghi từ 2026-10-17 (T-004, commit `050997e`). Mọi bộ build đã thử trên máy dev đều vẽ nó khác bản trong repo ở 501 điểm ảnh (0,0152%), mỗi điểm lệch đúng 1 mức màu trên 255. Roborazzi so ảnh có ngưỡng, nên `check.sh` và CI vẫn qua. Ghi lại ảnh này là việc riêng, không do Robolectric; plan không đụng (mục 8, câu 2).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | File được bảo vệ. `robolectric = "4.17"` |
| Sửa | `docs/features/quy-trinh/tickets/T-081-robolectric-roborazzi-anh-chuan.md` | `plan:`, trạng thái theo plan, `size: S` |

Các file đọc file plan sửa (L-020; `grep -rn robolectric` trong `scripts/`, `.github/`, `build-logic/`, 2026-10-30):
- `gradle/libs.versions.toml`: `build-logic/settings.gradle.kts` nạp làm catalog (khóa `robolectric` giữ tên); `AndroidComposeConventionPlugin.kt` dùng `libs.lib("robolectric")`, không đọc số phiên bản; `scripts/cloud-env-check.sh` và `scripts/tests/run.sh` chỉ đọc dòng `compileSdk`; `scripts/plan-check.py` chỉ dùng tên file. `.github/workflows/pr-check.yml` có tên file trong bộ lọc đường dẫn, nên PR này chạy cả build release.
- `scripts/cloud-setup.sh` chỉ đặt `robolectric.dependency.repo.url` cho mọi task test, không phụ thuộc phiên bản.
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` chỉ nhắc `gradle/libs.versions.toml` trong một chú thích về lint ("Library versions are managed in gradle/libs.versions.toml"), không đọc nội dung.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: Không chạm
- **Quyền truy cập:** Không thêm quyền. Robolectric chỉ ở `testImplementation`, không vào APK.
- **Thư viện và SDK:** Không thêm thư viện. Nâng `org.robolectric:robolectric` (khóa `robolectric` trong `gradle/libs.versions.toml`) từ 4.15.1 lên 4.17, chỉ ở classpath test của các module có plugin `template.android.compose`; giấy phép MIT như cũ.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** CI (Linux, Java 17) và phiên cloud vẽ khác máy dev với Robolectric 4.17, hay không tải được `android-all-instrumented` bản mới.
- **Điều kiện chuyển:** `pr-check` báo ảnh chuẩn lệch hay test giao diện đỏ, mà cùng commit với Robolectric 4.15.1 thì xanh.
- **Cách làm thay:** không có cách thay trong plan này. Dừng theo mục 7, giữ 4.15.1. Ghi ảnh chuẩn theo cách vẽ của CI là một plan khác, vì ảnh chuẩn đổi cần người xem từng ảnh.

### 3.5. Hành vi đã có bị đổi

Không có: plan không sửa code Kotlin nào. Mọi test JVM có sẵn là test hồi quy, phải qua nguyên vẹn với Robolectric 4.17.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Robolectric 4.17 có trên classpath test | Lệnh | Lệnh 1 của khối dưới bảng in đúng một dòng `org.robolectric:robolectric:4.17` |
| Mọi test qua, không ảnh nào đổi | Cổng chất lượng | `scripts/check.sh`: `== Cổng chất lượng: QUA`; dòng `Test JVM:` có số test không ít hơn lần `main-guard` gần nhất, 0 lỗi; lệnh 2 của khối dưới bảng in `0` |
| Robolectric 4.17 không làm đổi ảnh nào | Lệnh đo, không commit | Lệnh 3 (hai lần) và lệnh 3b của khối dưới bảng, chạy trên main mới nhất; 3b in `same list, same bytes`: lần ghi bằng 4.15.1 (trước khi sửa) và lần ghi bằng 4.17 (sau khi sửa) cho cùng một danh sách ảnh đổi, và từng ảnh trong đó giống từng byte giữa hai lần (`cmp`). Lúc viết plan danh sách là đúng một ảnh, `DesignSystem_spacing_components_light.png` (lệch có sẵn, mục 3.1); ảnh mới của phiên quảng cáo cũng phải nằm ngoài danh sách hay giống từng byte giữa hai lần |
| So ảnh vẫn bắt được lệch màu rõ trên vùng nhỏ | Đối chứng, không commit | Lệnh 4 của khối dưới bảng: đỏ, 1 test lỗi; ảnh được trả về. Lệch rất nhỏ (1 mức màu) nằm dưới ngưỡng so của Roborazzi; lệnh 3 so từng byte nên bắt được nó |
| Bản release dựng được | Build | `pr-check` của PR (PR đổi `gradle/libs.versions.toml` nên chạy build release); trên máy không cần, vì Robolectric không vào APK |
| CI dựng và test được | Kiểm tra thủ công (người: GitHub Actions) | `pr-check` của PR xanh; sau merge, `main-guard` xanh, gồm job `ui-test` trên emulator (T-075, ADR-0043) |
| Phiên cloud tải được Robolectric bản mới | Kiểm tra thủ công (người: phiên cloud có người, sau khi merge) | Trong một phiên cloud mới trên nhánh chính: `scripts/check.sh` QUA |
| Ảnh lệch có sẵn | Kiểm tra thủ công (người: xem ảnh) | Người duyệt mở `DesignSystem_spacing_components_light.png` ở repo và bản ghi lại (mục 4.1), thấy không khác bằng mắt, và trả lời mục 8, câu 2 |

Lệnh của mục 4 (chạy ở gốc repo; pathspec ảnh luôn có `*` ở cuối):

```sh
# 1. Robolectric on the test classpath.
./gradlew -q :feature:sample:dependencies --configuration debugUnitTestRuntimeClasspath \
  | grep -o 'org.robolectric:robolectric:[0-9.]*' | sort -u
# 2. Reference images changed in the working tree (must print 0 after check.sh).
git status --short -- '*/src/test/screenshots/*' | wc -l
# 3. Record every module that has reference images, list the changed ones, keep a copy, put the images back.
#    Run with bash (zsh does not split words). Once on the latest main before the version change, once after;
#    the label is the Robolectric version of gradle/libs.versions.toml.
bash << 'SH'
set -euo pipefail
label=$(sed -n 's/^robolectric = "\(.*\)"$/\1/p' gradle/libs.versions.toml)
out="${TMPDIR:-/tmp}/t081-$label"; rm -rf "$out"; mkdir -p "$out"
args=()
while read -r m; do
  g=":${m//\//:}"
  if [ "$m" = app ]; then args+=("$g:recordRoborazziStagingDebug" "$g:testStagingDebugUnitTest" --rerun)
  else args+=("$g:recordRoborazziDebug" "$g:testDebugUnitTest" --rerun); fi
done < <(git ls-files -- '*/src/test/screenshots/*.png' | sed 's|/src/test/screenshots/.*||' | sort -u)
./gradlew "${args[@]}"
git status --short -- '*/src/test/screenshots/*' | tee "$out/changed.txt"
awk '{print $2}' "$out/changed.txt" | while read -r f; do cp "$f" "$out/$(basename "$f")"; done
git checkout -- '*/src/test/screenshots/*'
echo "label $label: $(wc -l < "$out/changed.txt" | tr -d ' ') changed image(s)"
SH
# 3b. After both runs: the same list, and every changed image identical between the runs.
bash << 'SH'
set -euo pipefail
a="${TMPDIR:-/tmp}/t081-4.15.1"; b="${TMPDIR:-/tmp}/t081-4.17"
diff "$a/changed.txt" "$b/changed.txt"
awk '{print $2}' "$b/changed.txt" | while read -r f; do cmp "$a/$(basename "$f")" "$b/$(basename "$f")"; done
echo "same list, same bytes"
SH
# 4. Control: invert a 4 x 4 block of one image; the comparison must fail, then put the image back.
img=feature/sample/src/test/screenshots/SampleScreen_ad-loading_light.png
python3 -c "import sys; from PIL import Image; im=Image.open(sys.argv[1]).convert('RGBA'); px=im.load(); w,h=im.size
for y in range(h//2,h//2+4):
    for x in range(w//2,w//2+4): r,g,b,a=px[x,y]; px[x,y]=(255-r,255-g,255-b,a)
im.save(sys.argv[1])" "$img"
./gradlew :feature:sample:verifyRoborazziDebug :feature:sample:testDebugUnitTest --rerun; echo "exit $? (must be 1)"
git checkout -- "$img"
```

Lệnh 4 cần Pillow (`python3 -c "import PIL"`); thiếu thì đổi màu khối đó bằng cách khác, miễn ảnh lệch rõ trên vùng nhỏ.

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085).

**Ngoại lệ viết test trước:** đổi một dòng phiên bản trong `gradle/libs.versions.toml` và ticket. Không có hành vi mới nào để viết test đỏ trước; bằng chứng là cổng chất lượng, lệnh đo ảnh và đối chứng ở trên.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-30, máy dev (macOS, JDK 21.0.13), nhánh thử `claude/trial-T-081`, chưa đẩy. Lần đầu trên main `661dbfa`, lần đo lại trên main `dfab197` (có T-080, T-067, T-075). Log ở thư mục tạm của phiên.
- **Lệnh:** `scripts/check.sh`; so điểm ảnh bằng Pillow; lệnh 1 tới 4 của mục 4. Lần chạy cuối (trên `dfab197`, 2026-10-30) chép nguyên khối lệnh của mục 4 ra file, chạy bằng zsh như máy dev, theo đúng thứ tự của mục 3.1: lệnh 3 (nhãn 4.15.1), đổi phiên bản, lệnh 3 (nhãn 4.17), lệnh 3b, lệnh 1, lệnh 4, lệnh 2.
- **Kết quả:**
  1. `check.sh` với Robolectric 4.17 và ảnh cũ: QUA, `Test JVM: 1498 test, 0 lỗi. Ảnh screenshot: 89`, không test nào đỏ (cả hai lần). Sau `check.sh`, lệnh 2 in `0`.
  2. Classpath test của `:feature:sample` có `org.robolectric:robolectric:4.17`. Robolectric 4.17 tải `android-all-instrumented` bản `15-robolectric-13954326-i7` (bản cũ `12650502-i7`) lúc test chạy lần đầu.
  3. Lần đo lại, trên `dfab197`: lệnh 3 chạy 5 task ghi ảnh (năm module có ảnh: `:app` 30 ảnh, `:core:ads` 41, `:core:designsystem`, `:feature:loading`, `:feature:sample` mỗi module 6). Với Robolectric 4.15.1: đúng một ảnh khác bản trong repo, `DesignSystem_spacing_components_light.png`, 501 điểm ảnh (0,0152%), lệch tối đa 1 mức màu. Với Robolectric 4.17: cùng một ảnh đó, cùng số đo. 88 ảnh còn lại giống bản trong repo từng byte ở cả hai bản.
  4. Ảnh lệch giống nhau từng byte giữa các lần ghi: 4.17 (lần đầu, ghi chung năm module), 4.15.1 (ghi riêng `:core:designsystem`, hai lần) và bộ build trước T-080 (AGP 8.13, Roborazzi 1.43, Robolectric 4.15.1; `883c2c7`). Cả bốn bản cùng mã băm md5 `36ee2cae…`, 60.577 byte; bản trong repo là `c4c9a9a8…`, 60.397 byte, ghi ngày 2026-10-17 (T-004, commit `050997e`). Lệch này nằm dưới ngưỡng so của Roborazzi, nên `check.sh` và CI vẫn qua.
  5. Đối chứng (lệnh 4) với Robolectric 4.17: `:feature:sample:testDebugUnitTest` đỏ, "27 tests completed, 1 failed"; trả ảnh về thì `git status` sạch. Ngưỡng của Roborazzi là theo độ lệch màu từng điểm, nên lệch 1 mức màu (kết quả 3) không làm test đỏ; chỉ lệnh 3 (so byte sau khi ghi) bắt được lệch đó.
  6. Lần chạy cuối của khối lệnh: lệnh 3 in `label 4.15.1: 1 changed image(s)` rồi `label 4.17: 1 changed image(s)` (ảnh spacing); lệnh 3b in `same list, same bytes`; lệnh 1 in `org.robolectric:robolectric:4.17`; lệnh 4 đỏ (`SampleScreenScreenshotTest > ad_loading_light FAILED`, "27 tests completed, 1 failed", `exit 1 (must be 1)`); lệnh 2 in `0`. Sau đó nhánh thử sạch (`git status` rỗng).
  7. Lần thử đầu báo "0 ảnh đổi" sai, vì dùng pathspec `'*/src/test/screenshots/'`. Pathspec glob có dấu `/` cuối mà không có `*` không khớp file nào (`git ls-files -- '*/src/test/screenshots/'` in 0 file; `'*/src/test/screenshots/*'` in 90). Lệnh `git status` không lọc mới cho thấy ảnh spacing đã đổi. Lần đo lại dùng pathspec có `*`. Pathspec sai này có sẵn ở `CLAUDE.md` (mục Lệnh), `docs/templates/plan.md`, L-114 và plan T-080 (mục 2, mục 4, mục 4.1).
- **Chưa thử:** CI Linux và phiên cloud (kiểm tay ở mục 4).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (cấu hình build: `gradle/libs.versions.toml`). Điểm 2 tính là chạm vì plan đổi phiên bản trong catalog (`scripts/plan-check.py` cũng tính vậy); Robolectric chỉ ở test, không vào APK, và lên bản phụ. Rủi ro Cao, cần duyệt chéo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Một dòng, nhưng đổi môi trường chạy test giao diện của mọi module có Compose |
| B. Độ rõ của yêu cầu | 0 | Ticket rõ; lần thử trả lời câu hỏi chính (ảnh có đổi không) |
| C. Khả năng kiểm chứng tự động | 1 | Cổng chất lượng, lệnh đo ảnh và đối chứng kiểm hết; CI Linux và phiên cloud kiểm tay |
| D. Ảnh hưởng chéo | 2 | T-068 (cùng danh sách RUN-20261030-01) cũng sửa `gradle/libs.versions.toml`, ở dòng khác; các task quảng cáo thêm test chạy trên Robolectric |
| E. Độ mới của kỹ thuật | 1 | Nâng bản phụ của thư viện đã dùng |
| **Tổng** | 7 | **Mức: Cao (điểm chặn 2, 7)** |

## 6. Ảnh hưởng chéo

- File dùng chung bị chạm: `gradle/libs.versions.toml`.
- Phiên RUN-20261030-01 (các plan quảng cáo) đang chạy lúc viết plan (`status: running`). Các task của nó có thể thêm hay đổi ảnh chuẩn và test giao diện. Lần thử dùng main `661dbfa`, lần đo lại dùng `dfab197`; phiên làm task phải chạy lại lệnh đo ảnh trên main mới nhất, và ảnh mới của phiên quảng cáo cũng phải giống từng byte.
- `merge-if-green.sh` cho mọi nhánh `claude/T-*` merge khi có phiên đang chạy (nhật ký T-080, PR #323). Plan này chỉ làm khi không có phiên nào đang chạy (mục 7).
- T-068 thêm dòng vào nhóm `# Ads` của `gradle/libs.versions.toml`, khác dòng `robolectric`: git gộp được.
- Đề xuất thứ tự: sau khi RUN-20261030-01 kết thúc, trong phiên có người.

## 7. Điều kiện dừng riêng

- Lúc bắt đầu task hay lúc gọi `merge-if-green.sh`, có danh sách chạy `running` trên nhánh `claude/RUN-*-report`: dừng, chờ phiên kết thúc.
- Lệnh 3b báo khác: danh sách ảnh đổi của hai lần ghi (4.15.1 và 4.17) khác nhau, hay `cmp` báo một ảnh khác giữa hai lần: dừng. Ảnh đổi do Robolectric cần một plan mới, liệt kê ảnh và mức lệch cho người xem. Ảnh có trong cả hai danh sách và giống nhau từng byte là lệch có sẵn, không do Robolectric; ghi tên vào nhật ký.
- `check.sh` hay `pr-check` đỏ ở một test giao diện mà Robolectric 4.15.1 xanh: dừng.

## 8. Câu hỏi cho người duyệt

1. Ticket T-081 dự kiến ghi lại ảnh chuẩn, nhưng lần thử cho thấy không ảnh nào đổi do Robolectric 4.17. Đề xuất: không ghi lại ảnh nào, và chuyển cỡ ticket từ M sang S.
2. Ảnh lệch có sẵn `DesignSystem_spacing_components_light.png` (501 điểm ảnh, lệch 1 mức màu, dưới ngưỡng so). Đề xuất: không đụng trong plan này. Nếu anh muốn ảnh trong repo khớp đúng cách vẽ hiện tại, đó là một ticket nhỏ riêng: ghi lại một ảnh, có người xem.
3. Thứ tự. Đề xuất: làm sau khi phiên RUN-20261030-01 kết thúc, vì task quảng cáo có thể thêm ảnh, và vì `merge-if-green.sh` không chặn nhánh `claude/T-*` khi có phiên đang chạy.
4. Pathspec `'*/src/test/screenshots/'` (không có `*` cuối) của `CLAUDE.md` mục Lệnh, `docs/templates/plan.md` và L-114 không khớp file nào, nên lệnh "xem ảnh nào đã đổi" luôn in rỗng (mục 4.1, kết quả 7). Plan T-080 dùng nó cho câu "không ảnh chuẩn nào đổi"; câu đó vẫn đúng ở mức `check.sh` so ảnh qua (dưới ngưỡng), và lần đo lại ở đây xác nhận ở mức byte: bộ build mới không đổi ảnh nào ngoài ảnh spacing có sẵn. Đề xuất: ghi một bài học mới và sửa ba chỗ đó trong một PR riêng, không gộp vào plan này.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
