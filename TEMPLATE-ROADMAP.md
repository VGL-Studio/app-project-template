# KẾ HOẠCH HOÀN THIỆN DỰ ÁN MẪU

File này chỉ dùng cho repo dự án mẫu. Nó ghi những phần còn phải làm để dự án mẫu đủ dùng cho app đầu tiên. Khi tạo app mới từ dự án mẫu, script khởi tạo (phần 5) không chép file này sang app.

## Đã xong

- Bộ tài liệu quy trình trong `docs/`: quy trình, phiên tự chạy, thang điểm rủi ro, các mẫu tài liệu, ADR-0001 đến ADR-0011.
- Khung dự án: `:app` với hai biến thể `staging` và `production`, các module core, convention plugin trong `build-logic`.
- Feature mẫu `:feature:sample` làm theo đúng mẫu trong `CLAUDE.md`, có unit test, test giao diện, screenshot test và test trên emulator.
- Cổng chất lượng: `scripts/check.sh`, `pr-check.yml`, `main-guard.yml`, `scripts/merge-if-green.sh`, `scripts/find-breaking-commit.sh` và test của các script.

Hiện `:core:analytics`, `:core:config` và `:core:ads` mới chỉ có interface và bản cài đặt tạm, chưa nối với dịch vụ thật.

## Cách làm việc trong giai đoạn này

- Làm trong phiên Claude Code tương tác, có người theo dõi. Chưa dùng phiên tự chạy.
- Các phần dưới đây phải sửa `build-logic/`, `gradle/libs.versions.toml` và `.github/`. Đó là những file `CLAUDE.md` cấm sửa nếu không có plan rủi ro cao đã duyệt. Ở giai đoạn này, file kế hoạch này thay cho plan, và anh Nhật là người duyệt. Mỗi phần là một PR riêng. Mô tả PR nêu rõ những file bị cấm đã sửa và lý do.
- Nhánh đặt tên `claude/template-<phan>`, ví dụ `claude/template-firebase`.
- Chạy `scripts/check.sh` trên máy trước khi đẩy code. Người merge bằng `scripts/merge-if-green.sh`.
- Không đưa vào repo bất kỳ cấu hình production nào: file cấu hình Firebase production, mã quảng cáo thật, khóa R2 (ADR-0007, ADR-0009).
- Mỗi phần cập nhật ADR liên quan và `CLAUDE.md` nếu thay đổi cách viết feature.

## Các phần còn lại

Làm theo thứ tự. Phần sau dựa vào phần trước.

### 1. Firebase

Mục tiêu: `:core:analytics` ghi sự kiện vào Firebase Analytics, `:core:config` đọc cờ từ Remote Config, app có Crashlytics.

- Thêm Firebase BoM, Analytics, Crashlytics, Remote Config và các plugin Gradle `google-services`, `crashlytics`.
- `:core:config`: Remote Config là nguồn giá trị cờ. Giữ nguyên ba điều đã có: cờ mặc định tắt, `FeatureFlags.enabled()` trả về `Flow`, cờ bật riêng cho staging qua `StagingFeatureFlagsModule`. Chỉ đọc giá trị đã tải; không chặn màn hình chờ Remote Config.
- `:core:analytics`: bản cài đặt gửi sự kiện tới Firebase. Test dùng bản giả, không gọi Firebase.
- Feature vẫn không gọi Firebase trực tiếp.

Cần quyết định trước khi làm: CI build PR không có bí mật, nên không có `google-services.json` thật. Đề xuất commit một file `google-services.json` giả cho biến thể staging, chỉ để build qua được. File thật của staging được `staging.yml` đưa vào từ bí mật của repo (phần 4). File của production do người giữ.

### 2. Quảng cáo

Mục tiêu: `:core:ads` hiển thị quảng cáo theo vị trí qua AdMob, có hộp thoại xin đồng ý cho người dùng châu Âu (ADR-0004).

- Thêm Google Mobile Ads SDK và User Messaging Platform (UMP). Adapter của AppLovin và Unity thêm khi bật mediation thật, không cần ngay.
- Xin đồng ý qua UMP trước khi tải quảng cáo. Có lối mở lại phần cài đặt quyền riêng tư theo yêu cầu của Google.
- Quảng cáo cá nhân hóa ở châu Âu bật tắt qua Remote Config, sau khi đã có sự đồng ý.
- Rào chắn chính sách nằm trong module: giới hạn tần suất quảng cáo toàn màn hình, không hiện khi người dùng đang thao tác.
- Bản staging chỉ dùng mã ứng dụng và mã quảng cáo thử của Google. Mã thật của production được đưa vào lúc build phát hành, qua tham số build, không nằm trong repo.
- Test: hộp thoại đồng ý thử được bằng chế độ gỡ lỗi của UMP với vùng địa lý châu Âu.

### 3. File tĩnh trên R2

Mục tiêu: module mới `:core:assets` tải file tĩnh từ R2 (ADR-0006).

- Địa chỉ gốc khác nhau cho staging và production, khai báo theo biến thể build.
- Lưu đệm trên máy, dùng được khi mất mạng; khi chưa có bản lưu đệm thì báo trạng thái lỗi cho feature xử lý.
- Đường dẫn file gắn phiên bản hoặc mã băm, không bao giờ ghi đè.
- Thêm `:core:assets` vào bảng module trong `CLAUDE.md`.

### 4. Workflow staging và kích hoạt phiên tự chạy

- `staging.yml`: build biến thể staging từ nhánh chính, phát cho nhóm qua Firebase App Distribution. Bí mật dùng: `google-services.json` của staging và tài khoản dịch vụ cho App Distribution.
- `start-run.yml`: gọi API của routine để bắt đầu phiên tự chạy. Bí mật dùng: mã kích hoạt routine.
- Người tự thêm các bí mật vào repo. AI không đọc hay in ra bí mật.

### 5. Script phát hành và script khởi tạo app mới

- `scripts/release.sh`: chạy trên máy người phụ trách. Chỉ build từ commit đã có trên nhánh chính và đã qua `main-guard.yml`. Lấy cấu hình production từ trình quản lý mật khẩu, build bản ký, đẩy lên Google Play. AI chỉ viết script này trong giai đoạn hoàn thiện dự án mẫu; sau đó không chạm vào nữa (ADR-0009).
- Script khởi tạo app mới: đổi tên gói `com.vglstudio.template`, `applicationId`, tên app; xóa feature mẫu nếu muốn; không chép file kế hoạch này.

## Việc cần anh Nhật chuẩn bị

| Khi nào | Việc |
| --- | --- |
| Phần 1, khi muốn chạy thử thật | Tạo project Firebase cho staging, thêm app Android, tải `google-services.json` của staging |
| Phần 2 | Không cần. Dùng mã thử của Google |
| Phần 3 | Tạo bucket hoặc thư mục staging trên R2, cho biết địa chỉ công khai |
| Phần 4 | Thêm bí mật vào repo; tạo routine Claude Code và lấy mã kích hoạt |
| Phần 5 | Chuẩn bị khóa upload, cấu hình Firebase production, mã quảng cáo thật trong trình quản lý mật khẩu |

## Việc còn phải kiểm chứng

- Môi trường cloud của routine tải được thư viện từ Google Maven (`dl.google.com`, `maven.google.com`) và chạy được `scripts/check.sh`. Môi trường cloud dùng để dựng khung không tải được, nên toàn bộ build lúc đó phải chạy trên CI.
- Phiên Claude Code mới chỉ đọc `CLAUDE.md` và `docs/` mà làm đúng quy trình. Chỗ nào phiên đó làm sai hay phải hỏi lại thì bổ sung vào tài liệu.
- `main-guard.yml` khi nhánh chính đỏ: tạo issue `main-broken` và nhánh revert. Phần này chưa từng chạy thật.
