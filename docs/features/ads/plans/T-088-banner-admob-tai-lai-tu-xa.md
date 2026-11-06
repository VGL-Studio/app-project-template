---
ticket: T-088
title: Banner cố định và MREC (AdMob và MAX) tải lại theo Remote Config
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 22.2M        # cỡ M, số gợi ý 22.2M của scripts/token-usage.py estimate (2026-11-06, trung vị 22 task M); không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-088: BANNER CỐ ĐỊNH VÀ MREC (ADMOB VÀ MAX) TẢI LẠI THEO REMOTE CONFIG

## 1. Tóm tắt

Banner cố định (cả lớp banner của thanh đáy) và banner MREC thôi dựa vào tự làm mới của đơn vị: `runBannerLoads` tải lại mọi banner bằng tay sau mỗi lần có quảng cáo, chờ nhịp bằng `awaitReloadTick` của T-087 với luồng của `time_reload_banner` hay `time_reload_banner_mrec` (mặc định 60 giây). Enum `BannerRefresh` bị bỏ: "ai làm mới" luôn là `:core:ads`, còn "yêu cầu thu gọn" (`collapsible=bottom`, `collapsible_request_id`) chỉ theo định dạng `BANNER_COLLAPSIBLE`. View MAX không bao giờ tự làm mới (dừng ngay khi dựng, `resume()` không bật lại). Kiểm chứng bằng unit test JVM cho vòng nạp, khung hai nền tảng và view MAX giả, Compose UI test cho `BannerSlotHost` (bị che, ra nền, đổi khóa giữa chừng), và một lần kiểm trên máy thật có mã MAX.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-34 | 1. Banner cố định `time_reload_banner` (60), banner MREC `time_reload_banner_mrec` (60) | Hai khóa mới; `reloadIntervalMillis(BANNER)` và `(BANNER_MREC)` của T-087 trả luồng của chúng. Lớp banner của thanh đáy là định dạng `BANNER` nên đọc `time_reload_banner` |
| KB-34 | 2. 0 không thay, dương dưới 30 dùng 30, âm hay sai kiểu dùng mặc định | `reloadSeconds` của T-087, không đổi |
| KB-34 | 3. Giá trị mới áp từ nhịp sau; 0 sang dương đếm từ lúc nhận; dương sang 0 bỏ lần đang đếm | `awaitReloadTick` của T-087, không đổi; test cho đường banner cố định và MREC |
| KB-34 | 4. Khóa banner là 0 thì vẫn thử lại sau lỗi tới khi có quảng cáo, có rồi giữ | Vòng thử lại B-02 không đổi; chỉ bước nạp lại sau `Loaded` chờ nhịp |
| KB-34 | 5. Banner cố định và MREC của AdMob và MAX do `:core:ads` tải lại theo khóa, không dùng tự làm mới của đơn vị; hướng dẫn phát hành ghi việc tắt tự làm mới trên đơn vị AdMob và MAX | Code: mọi banner đi vòng nạp tay; MAX: `stopAutoRefresh()` khi dựng view, không bao giờ `startAutoRefresh()`. AdMob: tự làm mới là cài đặt của đơn vị trên AdMob, code không tắt được; tài liệu ghi việc tắt ở `ads-guide.md` (mục 5 bước 6, mục 9) và `to-app.md` |
| KB-34 | 6. Luật đang có giữ: chỉ thay ở foreground, không khi bị che (B-05, B-07); thay lỗi giữ quảng cáo cũ; mỗi lượt qua cổng đồng ý (S-15) | Lượt nạp lại đi qua `sendWhenAllowed` có sẵn (chờ màn hiện và không bị che, hỏi cổng); lượt tới hạn lúc bị che hay ở nền gửi một lần khi màn che đóng hay màn hiện lại; lỗi thì quảng cáo cũ vẫn hiện (khung không đổi, lượt thử lại theo B-02) |
| KB-18 | 1. Đơn vị banner tắt tự làm mới; `:core:ads` tải lại theo `time_reload_banner` | Như KB-34 tiêu chí 1, 5 |
| KB-18 | 2, 4, 5: không tải lại ở nền; chưa đồng ý không gửi; rút đồng ý giữ khung, đồng ý lại tải lại trong khung | Giữ: vòng nạp chỉ chạy khi khung `ACTIVE` (B-06), mọi lượt qua `sendWhenAllowed` |
| KB-18 | 3. Banner thu gọn: mỗi lần làm mới là một banner bung ra mới | Giữ: `BANNER_COLLAPSIBLE` vẫn gắn `collapsible=bottom` và mã yêu cầu mới mỗi lượt; banner cố định và MREC không được gắn |
| KB-29 | 2. MREC theo cùng luật banner cố định, làm mới theo `time_reload_banner_mrec` | Như KB-34 tiêu chí 1 |
| KB-31 | 1, 2, 5, 6: thứ tự `ads_mediation_order`, chỉ một nền tảng khi thứ tự là `admob` hay `max`, MAX chỉ sau đồng ý, luật chung áp như nhau | Mỗi lượt nạp lại là một `send()` của `OrderedBannerAdView`, bắt đầu từ nền tảng thứ nhất như lượt đầu (B-09); lỗi của nền tảng thứ nhất chuyển ngay sang nền tảng thứ hai trong cùng lượt; cả hai lỗi là một lần lỗi của B-02, quảng cáo cũ vẫn hiện |

Thiết kế tham chiếu: không có thiết kế riêng (giao diện không đổi).

ADR liên quan: ADR-0045 (mục 2.3 hai nền tảng, mục 7.1 bị che), ADR-0037 (AppLovin MAX), ADR-0046, ADR-0031 (viết lại câu khẳng định cũ), ADR-0044.

Các gạch đầu dòng của `docs/CLAUDE.md`, mục Khi viết plan, áp dụng: danh sách file đủ; đổi file nơi khác đọc (bảng khóa sinh tự động, `scripts/tests/run.sh`, `AdPlacement.kt`: mục 6); sửa Kotlin nên có mục 3.5; tiêu chí áp cho mọi lượt gửi (liệt kê đường ở mục 4: lượt đầu, thử lại, nạp lại, lượt chuyển nền tảng); số liệu theo đối tượng (một khung, hai nền tảng, hai khung một vị trí: mục 3.1 bước 6, test chồng nhau); bước chờ trước khi gửi (nạp lại tới hạn khi app ở nền: test ra nền rồi trở lại); bước kiểm tay đổi Remote Config đối chiếu `StagingRemoteValuesModule` (hai khóa không có giá trị cục bộ); `token_estimate`. Không áp dụng: đo lỗi giao diện, kỹ thuật mới (dừng tự làm mới của MAX đã dùng ở B-07), thư viện, R2, dịch chữ, vỏ app.

## 3. Giải pháp

### 3.1. Cách làm

Plan dựa trên kết quả của T-087: `ReloadInterval.kt` (`reloadSeconds`, `awaitReloadTick`), `AdsController.reloadIntervalMillis(format)`, `runBannerLoads(reloadMillis)`, nhóm "Banner" của `scripts/ads-plan.py`. Theo mẫu có sẵn của banner thu gọn (B-03) và của khung hai nền tảng (`OrderedBannerAdView`, T-071, T-085).

1. **Khóa** (`AdsSettings.kt`, `AdsFlags.kt`, `AdsController.kt`): `TIME_RELOAD_BANNER` (`time_reload_banner`, long, 60), `TIME_RELOAD_BANNER_MREC` (`time_reload_banner_mrec`, long, 60), mô tả tiếng Anh như ba khóa của T-087 (tên do ta đặt). `AdsGlobalFlags` thêm `bannerReloadSeconds = 60`, `bannerMrecReloadSeconds = 60` qua `reloadSeconds`. `reloadIntervalMillis` nhận thêm `BANNER`, `BANNER_MREC`.
2. **Vòng nạp** (`BannerLoads.kt`): bỏ `enum class BannerRefresh` và `bannerRefreshOf`. `runBannerLoads(view, reloadMillis, visible, mayRequestNow, retryResets, onLoaded, logger, placementKey, onFailed)`: sau `Loaded` luôn `awaitReloadTick(reloadMillis)` rồi `sendWhenAllowed()`. Thêm `internal fun bannerRequestIsCollapsible(format: AdFormat): Boolean`: `BANNER_COLLAPSIBLE` là `true`, `BANNER`, `BANNER_MREC` là `false`, định dạng khác ném `IllegalArgumentException` (thay `bannerRefreshOf`). KDoc của vòng nạp sửa: không còn nhánh "SDK tự làm mới".
3. **Yêu cầu AdMob** (`BannerAdView.kt`): `bannerAdRequest(collapsible: Boolean, requestId)`: chỉ `collapsible` mới có `collapsible=bottom` và `collapsible_request_id`. `SdkBannerAdView` nhận `collapsible: Boolean` thay `refresh`. Mỗi `send()` vẫn ghi `requestSent` nên lượt nạp lại có `latency_ms` và theo bộ đếm `load_attempt` của khe như lượt đầu.
4. **MAX** (`MaxBannerAdView.kt`): `MaxBannerSdkView` giữ nguyên (`AppLovinMaxBannerView` vẫn có `startAutoRefresh()`, test của view thật gọi nó), nhưng `MaxBannerAdView` không bao giờ gọi `startAutoRefresh()`. `MaxBannerAdView.send()`: view mới dựng thì gọi `stopAutoRefresh()` trước `loadAd()` đầu (cách tải tay của AppLovin) và gọi lại ngay sau `loadAd()` như đường "dừng trước khi dựng" có sẵn, vì chưa kiểm trên máy thật SDK 13.6.4 nhận lệnh dừng trước lượt nạp đầu hay không (câu hỏi 2). `resume()` chỉ đặt lại cờ, không gọi SDK; `pause()` vẫn gọi `stopAutoRefresh()` (không hại, giữ dừng ngay khi bị che). Giữ `allow_pause_auto_refresh_immediately`. Mỗi lượt nạp lại là `loadAd()` trên cùng view.
5. **Khung hai nền tảng** (`PlatformBanners.kt`): logic không đổi; mỗi lượt nạp lại là một `send()`, nền tảng được hỏi là `next` (về 0 sau `Loaded`), nên lượt nạp lại đi theo `ads_mediation_order` như lượt đầu; nền tảng thứ nhất lỗi thì `TryNext` sang nền tảng thứ hai ngay; cả hai lỗi thì `Failed` (một lần lỗi của B-02), quảng cáo đang hiện giữ. Luật giữ một lỗi làm mới của con đang hiện (B-09) giữ cho đơn vị AdMob còn bật tự làm mới; MAX không còn lượt tự làm mới nào. KDoc sửa theo.
6. **Slot** (`AdSlot.kt`): `BannerSlot` lấy `ads.reloadIntervalMillis(format)` cho mọi định dạng banner, `SdkBannerAdView(collapsible = bannerRequestIsCollapsible(format))`; `BannerSlotHost`, `BannerFrameContent` bỏ tham số `refresh`, khóa của view là `key(placementKey, unitId)` (`placementKey` đã gồm định dạng của lớp, `nativeSlotKey`). KDoc của `AdSlot`, `BottomBannerSlot` sửa. `BannerFrame.kt` không đổi (khung, chiều cao, dừng khi không hiện giữ nguyên). Số liệu theo cách dùng: một khung một nền tảng (mỗi lượt nạp lại một `ad_load_success` hay `ad_load_fail` có `latency_ms`, vị trí của màn); một khung hai nền tảng (mỗi nền tảng bộ đếm riêng, `<slot>/max`); hai khung một vị trí (hai bộ đếm riêng như hôm nay, L-068).
7. **KDoc của `AdFormat`** (`AdPlacement.kt`, chỉ KDoc của `BANNER`, `BANNER_MREC`, không đổi dòng khai báo vị trí): "refreshed by its ad unit" thành "loaded again by :core:ads every `time_reload_banner` / `time_reload_banner_mrec` seconds".
8. **Bảng khóa:** sinh lại `docs/remote-config-keys.md`, `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Không thêm giá trị cục bộ của staging.
9. **`scripts/ads-plan.py`:** hai khóa vào nhóm "Banner" (nhóm T-087 thêm); dòng `FORMATS` của `banner`, `banner_mrec`: "Nạp lại tay theo time_reload_banner (time_reload_banner_mrec), mặc định 60 giây; đơn vị tắt tự làm mới trên AdMob và MAX". Test trong `scripts/tests/run.sh`.
10. **Tài liệu** (không ghi mã ticket vào tài liệu quy tắc, ADR-0044): `ads-business-rules.md`: B-02 (`:core:ads` nạp lại tay theo `time_reload_banner`, đơn vị tắt tự làm mới trên AdMob và MAX; thử lại giữ), B-06 (rút đồng ý: không gửi, không nạp lại, giữ khung), B-07 (bị che: không gửi, AdView của AdMob `pause()`, lượt nạp lại tới hạn gửi một lần khi màn che đóng), B-08 (MREC nạp lại theo `time_reload_banner_mrec`, đơn vị tắt tự làm mới), B-09 (mỗi lượt nạp lại hỏi theo `ads_mediation_order` như lượt đầu; view MAX không tự làm mới; tạm dừng MAX không còn là cách dừng tự làm mới), mục 8.2 (`latency_ms`: lượt nạp lại do `:core:ads` gửi nên có; chỉ lượt tự làm mới của đơn vị AdMob còn bật tự làm mới mới không có), mục 10.1 (hai khóa), mục 11 (mức sàn 30 giây áp cả banner cố định và MREC). Mục 5.7 không có chữ về làm mới banner, không sửa. `ads-guide.md`: bảng mục 3 (dòng banner cố định, MREC), mục 5 bước 6 (đơn vị banner, MREC, banner thu gọn tắt tự làm mới trên AdMob và MAX), mục 6 (nhịp hiển thị của banner), mục 7 (AppLovin MAX: view không tự làm mới), mục 9 (trước khi phát hành: kiểm tự làm mới đã tắt trên từng đơn vị). `docs/ops/ads-plan.md`: mục Định dạng (cách làm mới của banner). `docs/ops/to-app.md` mục 7 (Lưu ý): banner cố định, MREC, banner thu gọn do `:core:ads` tải lại theo Remote Config; khi đồng bộ, người ở app kiểm và tắt tự làm mới trên từng đơn vị production của AdMob và MAX, vì mã thử ở staging không cho thấy lỗi làm mới hai lần.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Hai khóa (mục 3.1 bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsFlags.kt` | Hai trường của `AdsGlobalFlags` (bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | `reloadIntervalMillis` thêm `BANNER`, `BANNER_MREC` (bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerLoads.kt` | Bỏ `BannerRefresh`, mọi banner nạp lại tay, `bannerRequestIsCollapsible` (bước 2) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/BannerAdView.kt` | `bannerAdRequest(collapsible, ...)`, `SdkBannerAdView(collapsible)` (bước 3) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/MaxBannerAdView.kt` | `MaxBannerAdView` dừng tự làm mới khi dựng, `resume()` không bật lại, không gọi `startAutoRefresh` (bước 4) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/PlatformBanners.kt` | Chỉ KDoc (bước 5) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdSlot.kt` | Luồng nhịp cho mọi banner, bỏ `refresh`, KDoc (bước 6) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdPlacement.kt` | Chỉ KDoc của `AdFormat.BANNER`, `AdFormat.BANNER_MREC` (bước 7) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ReloadInterval.kt` | Chỉ KDoc (nhịp áp cho năm lớp) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Test hai khóa; dữ liệu `globalKeys_matchAdsKitTable` (21 khóa) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsFlagsTest.kt` | Test hai trường |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerReloadIntervalTest.kt` | Test `BANNER`, `BANNER_MREC`; câu khẳng định "`BANNER` ném lỗi" của T-087 viết lại (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerLoadsTest.kt` | Test nạp lại tay của mọi banner; viết lại hai test theo mục 3.5; dữ liệu dựng (bỏ `refresh`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/SdkBannerAdViewTest.kt` | Test yêu cầu không thu gọn của banner cố định, MREC; dữ liệu dựng |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/MaxBannerAdViewTest.kt` | Test dừng khi dựng, không bật lại; viết lại hai test theo mục 3.5 |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/PlatformBannersTest.kt` | Test lượt nạp lại theo thứ tự nền tảng; dữ liệu dựng |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BannerSlotHostTest.kt` | Test nạp lại banner cố định, MREC, MAX khi bị che, ở nền, đổi khóa; viết lại `covered_maxShown_stopsAutoRefresh_uncovered_starts` (mục 3.5); dữ liệu dựng |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/BottomAdBarTest.kt` | Dữ liệu dựng (bỏ `BannerRefresh`) |
| Ghi lại | `docs/remote-config-keys.md` | Sinh lại (bước 8) |
| Ghi lại | `docs/remote-config-template.json` | Sinh lại (bước 8) |
| Sửa | `scripts/ads-plan.py` | Hai khóa, dòng `FORMATS` (bước 9) |
| Sửa | `scripts/tests/run.sh` | Test của `ads-plan.py` (mục 4) |
| Sửa | `docs/ops/ads-business-rules.md` | B-02, B-06, B-07, B-08, B-09, mục 8.2, 10.1, 11 (bước 10) |
| Sửa | `docs/ops/ads-guide.md` | Mục 3, 5, 6, 7, 9 (bước 10) |
| Sửa | `docs/ops/ads-plan.md` | Mục Định dạng (bước 10) |
| Sửa | `docs/ops/to-app.md` | Mục 7, việc của người khi đồng bộ (bước 10) |
| Sửa | `docs/features/ads/tickets/T-088-banner-admob-tai-lai-tu-xa.md` | `status: planned`, `plan:` (khi tạo plan); phiên ghi trạng thái theo `auto-run.md` |

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Hai khóa Remote Config mới, mặc định 60 giây.
- Quảng cáo: không thêm, bớt hay dời vị trí. Đổi người làm mới banner cố định và MREC (từ đơn vị sang `:core:ads`), nhịp chỉnh từ xa (KB-34). Đơn vị production của mỗi app phải tắt tự làm mới (việc của người, ghi ở `ads-guide.md`, `to-app.md`).
- Sự kiện phân tích: không thêm sự kiện hay tham số. Lượt nạp lại của banner cố định và MREC nay có `latency_ms` và `load_attempt` như lượt đầu (mục 8.2).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `MaxAdView.stopAutoRefresh()`, `loadAd()` của SDK AppLovin MAX đã có (`applovin` trong `gradle/libs.versions.toml`, phụ thuộc của `:core:ads`); `AdView`, `AdRequest` của `play-services-ads` đã có.

### 3.4. Phương án dự phòng

Không có. Thứ tự gọi `stopAutoRefresh()` quanh lượt `loadAd()` đầu của MAX chỉ thấy được trên máy thật có khóa SDK và mã đơn vị MAX; plan gọi cả trước và sau (mục 3.1 bước 4) để một trong hai cách chắc chắn có hiệu lực, và kiểm tay ở mục 4.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` liệt kê các test dùng tên của file plan sửa (kết quả ở mô tả PR). Các test chỉ đổi phần dựng theo chữ ký mới (bỏ `BannerRefresh`, `refresh`; ADR-0029) mà câu khẳng định giữ: phần lớn `BannerLoadsTest` (thử lại, ẩn, cổng đóng, mạng về, `TryNext`, hai khe một vị trí), `BannerSlotHostTest` (khung, chiều cao, MREC, rút đồng ý, hai nền tảng), `PlatformBannersTest` (mọi test của B-09, T-085), `SdkBannerAdViewTest.collapsibleSends_haveBottomAnchorAndNewRequestIdEachTime`, `BottomAdBarTest`. Hai mươi mốt test đo vị trí, kích thước mà script xếp "Cần xem trước" (`BannerAdSizeTest` gồm `bottomBannerSlot_reportsAnchoredHeight`, `BannerSlotHostTest.shorterPlatform_drawnAtFrameTop_atItsOwnHeight`, `InListAdsTest`, `NativeAdCellTest`, `NativeAdSlotTest`, `NativeFlowStepTest`, `LoadingScreenTest`) chỉ trùng tên chung (`AdFormat`, `Loaded`, `Failed`) hay đọc chiều cao khung banner; plan không đổi cỡ hay khung, nên câu khẳng định giữ. Các câu phải đổi:

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `BANNER`, `BANNER_MREC` làm mới theo đơn vị, `BANNER_COLLAPSIBLE` bằng tay, định dạng khác ném lỗi | Mọi banner nạp tay; chỉ `BANNER_COLLAPSIBLE` là yêu cầu thu gọn (bước 2) | `BannerLoadsTest.refreshOf_bannerByAdUnit_collapsibleManual_othersThrow` | Câu mới (ý định: mỗi định dạng banner có đúng một cách yêu cầu, định dạng khác bị từ chối; đổi tên `requestIsCollapsible_onlyCollapsible_othersThrow`): `bannerRequestIsCollapsible(BANNER) == false`, `(BANNER_MREC) == false`, `(BANNER_COLLAPSIBLE) == true`, `NATIVE_SMALL` ném `IllegalArgumentException` |
| Banner làm mới theo đơn vị: sau `Loaded` app không gửi lại | App gửi lại sau nhịp của khóa; khóa 0 thì không gửi lại (bước 2) | `BannerLoadsTest.byAdUnit_afterLoaded_appNeverSendsAgain` | Câu mới (ý định: sau `Loaded` không có lượt gửi nào ngoài nhịp của khóa; đổi tên `afterLoaded_sendsOnlyOnReloadTick`): luồng 0: sau `Loaded` tới 10 phút một lượt gửi; đối chứng: luồng 60 000: lượt gửi thứ hai đúng lúc 60 giây, không trước |
| `pause()` dừng, `resume()` bật lại tự làm mới của MAX | `pause()` dừng; `resume()` không gọi SDK (bước 4) | `MaxBannerAdViewTest.pauseResume_mapToAutoRefresh` | Câu mới (ý định: MAX không tự làm mới khi khung không chạy; đổi tên `pauseStops_resumeNeverStartsAutoRefresh`): sau `pause()` có một `stopAutoRefresh`; sau `resume()` `startCount == 0` và `calls` của bản giả không có lệnh nào sau `resume()` |
| View dựng sau một `pause()` thì dừng ngay sau lượt nạp đầu; view dựng khi đang chạy thì tự làm mới | Mọi view MAX dừng tự làm mới khi dựng, trước và sau lượt nạp đầu (bước 4) | `MaxBannerAdViewTest.pauseBeforeBuild_stopsRightAfterBuild` | Giữ câu khẳng định (view dựng sau `pause()` có `stopAutoRefresh` sau `loadAd`); thêm test mới `newView_stopsAutoRefreshAroundFirstLoad` cho view dựng khi đang chạy |
| Màn che đóng thì MAX đang hiện bật lại tự làm mới | Màn che đóng: MAX không bật lại; lượt nạp lại tới hạn lúc bị che gửi một lần (B-07) | `BannerSlotHostTest.covered_maxShown_stopsAutoRefresh_uncovered_starts` | Câu mới (ý định: bị che thì MAX không làm mới, hết che thì lượt nạp tới hạn đi đúng một lần; đổi tên `covered_maxShown_noRefresh_uncovered_dueReloadSentOnce`): lúc bị che có `stopAutoRefresh`, không `loadAd`; nhịp 60 giây rơi lúc bị che; hết che: đúng một `loadAd`, không có lệnh bật tự làm mới |
| `reloadIntervalMillis(BANNER)` ném lỗi (test viết trước của T-087) | Trả luồng của `time_reload_banner` | `AdsControllerReloadIntervalTest.reloadIntervalMillis_followsKeyOfEachLayer` | Câu mới: dòng "`BANNER` ném lỗi" thay bằng `BANNER` là 60 000 và `BANNER_MREC` là 60 000 khi chưa đặt gì; đối chứng: `NATIVE_MEDIUM` ném `IllegalArgumentException` |
| Yêu cầu của banner cố định không có phần thu gọn (`BannerRefresh.BY_AD_UNIT`) | Giữ, qua `collapsible = false` | `SdkBannerAdViewTest.fixedBannerRequest_hasNoCollapsibleExtras` | Giữ câu khẳng định; dữ liệu dựng `collapsible = false` |

## 4. Kiểm thử

Test viết trước code, thất bại đúng lý do trước khi viết code. Thời gian ảo của `runTest`; bản giả `FakeBannerAdView`, `FakeMaxBannerSdkView` có sẵn.

Mọi đường tới một lượt gửi của banner cố định và MREC: lượt đầu, lượt thử lại sau lỗi (B-02), lượt chuyển sang nền tảng thứ hai (`TryNext`), lượt nạp lại sau nhịp (mới). Cả bốn đi qua `sendWhenAllowed` (chờ màn hiện và không bị che, hỏi cổng đồng ý); test có sẵn giữ ba đường đầu, mục dưới thêm test cho đường nạp lại ở mỗi điều kiện.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-34, tiêu chí 1: khóa | Unit test | `AdsSettingsTest.bannerReloadKeys_declaredWithDefaults`: `time_reload_banner`, `time_reload_banner_mrec` trong `AdsSettings.all()`, `LONG`, 60, mô tả kết thúc bằng "Name set by the template" |
| KB-34, tiêu chí 2 | Unit test | `AdsFlagsTest.bannerReloadKeys_rules`: chưa đặt gì 60, 60; đối chứng: đặt 90 đọc 90, 10 đọc 30, -1 đọc 60, 0 đọc 0, cho từng khóa |
| KB-34, tiêu chí 1: lớp đọc khóa của nó | Robolectric | `AdsControllerReloadIntervalTest.bannerLayers_followTheirKeys`: đặt `time_reload_banner = 45`, `time_reload_banner_mrec = 90`: luồng `BANNER` 45 000, `BANNER_MREC` 90 000, `BANNER_COLLAPSIBLE` vẫn theo khóa của nó |
| KB-34, tiêu chí 1, KB-18 tiêu chí 1: banner cố định nạp lại tay | Unit test | `BannerLoadsTest.fixed_reloadsByHandAfterKeySeconds`: luồng 60 000; `Loaded` lúc 0; lượt gửi thứ hai lúc 60 giây, không trước |
| KB-29, tiêu chí 2: MREC theo khóa của nó | Compose UI test | `BannerSlotHostTest.mrec_reloadsAtMrecKey`: `BannerSlotHost` kiểu `MEDIUM_RECTANGLE` với luồng 90 000: lượt gửi thứ hai lúc 90 giây, khung giữ 250 dp suốt lượt nạp lại |
| KB-34, tiêu chí 3 cho banner cố định | Compose UI test | `BannerSlotHostTest.fixed_keyChangedOnScreen_nextReloadUsesNew`: luồng 60 000, lúc 20 giây đổi 120 000: lượt nạp lại lúc 60 giây, lượt sau lúc 180 giây; `fixed_keyPositiveToZero_dropsCount_zeroToPositive_countsFromReceipt` |
| KB-34, tiêu chí 4 | Unit test | `BannerLoadsTest.reloadZero_fixedBanner_retriesUntilLoaded_thenKeeps`: luồng 0; lỗi lúc 0, thử lại lúc 2, 6 giây, `Loaded` lúc 6 giây; tới 10 phút không gửi thêm |
| KB-34, tiêu chí 6: không nạp lại ở nền | Compose UI test | `BannerSlotHostTest.fixed_reloadDueWhileHidden_sentOnceWhenVisible`: màn ra nền (luồng `visible` false) lúc 30 giây, nhịp rơi lúc 60 giây không gửi; hiện lại lúc 200 giây: đúng một lượt gửi |
| KB-34, tiêu chí 6: bị che (B-07) | Compose UI test | `BannerSlotHostTest.fixed_reloadDueWhileCovered_sendsOnceWhenUncovered`, `mrec_reloadDueWhileCovered_sendsOnceWhenUncovered` (như test có sẵn của banner thu gọn `manualReloadDueWhileCovered_sendsOnceWhenUncovered`) |
| KB-34, tiêu chí 6: cổng đồng ý (S-15) | Unit test | `BannerLoadsTest.fixedReload_gateClosedAtSend_sendsNothingAndStops`: nhịp tới, `mayRequestNow` false: không gửi, vòng kết thúc, không thử lại |
| KB-34, tiêu chí 6: lỗi giữ quảng cáo cũ | Unit test | `PlatformBannersTest.reload_bothFail_shownChildKept_oneFailure`: khung hai nền tảng, AdMob đang hiện; lượt nạp lại: MAX lỗi, AdMob lỗi: một `Failed`, `showing` vẫn là AdMob (`Content` vẽ AdMob), lượt thử lại bắt đầu từ MAX |
| KB-34 tiêu chí 5, KB-31: lượt nạp lại theo thứ tự | Unit test | `PlatformBannersTest.reload_asksFirstPlatformAgain`: thứ tự `max_admob`, MAX lỗi, AdMob hiện; lượt nạp lại: `send` tới MAX trước; MAX được thì MAX hiện, AdMob bị dừng. Đối chứng thứ tự `admob`: chỉ AdMob được gửi |
| KB-34 tiêu chí 5: MAX không tự làm mới | Unit test | `MaxBannerAdViewTest.newView_stopsAutoRefreshAroundFirstLoad`: `calls` của bản giả lúc gửi đầu là `[stopAutoRefresh, loadAd, stopAutoRefresh]`; lượt gửi sau chỉ `loadAd`; qua mọi lượt `startCount == 0` |
| KB-34 tiêu chí 5: MAX trong khung qua nhiều lượt | Compose UI test | `BannerSlotHostTest.maxShown_reloadsByHand_neverStartsAutoRefresh`: MAX hiện, qua ba nhịp 60 giây, che rồi hết che, ra nền rồi hiện lại: bản giả có ba `loadAd` nạp lại, không lệnh nào bật tự làm mới |
| KB-18, tiêu chí 3: chỉ banner thu gọn là yêu cầu thu gọn | Unit test | `SdkBannerAdViewTest.fixedAndMrecReloads_haveNoCollapsibleExtras`: `SdkBannerAdView` của `BANNER` và `BANNER_MREC` qua ba lượt `send()`: không yêu cầu nào có `collapsible` hay `collapsible_request_id`; test có sẵn `collapsibleSends_haveBottomAnchorAndNewRequestIdEachTime` giữ |
| Số liệu của lượt nạp lại; hai khung chồng nhau | Unit test | `SdkBannerAdViewTest.reloadSends_eachHasLatency`: ba lượt gửi, mỗi `ad_load_success` có `latency_ms` tính từ lượt gửi của chính nó; `BannerSlotHostTest.twoFixedFramesSamePlacement_reloadsApart_countsApart`: hai khung của một vị trí, nhịp lệch nhau 10 giây: mỗi khung một bộ đếm, lượt nạp lại của khung này không làm khung kia gửi |
| Lớp banner của thanh đáy | Unit test | `AdsControllerReloadIntervalTest.bannerLayers_followTheirKeys` (luồng của `BANNER`); `BannerSlot` truyền `format` của lớp (`BottomBannerSlot` là `BANNER`), xem mục 3.1 bước 6 |
| Bảng khóa | Unit test có sẵn | `RemoteConfigKeysTest` (`:app`) qua sau khi sinh lại |
| `scripts/ads-plan.py` | Test của script | `scripts/tests/run.sh`: `ads-plan: banner reload keys go in the Banner group` (`time_reload_banner`, `time_reload_banner_mrec`, đọc bảng khóa thật của repo); `ads-plan: banner and MREC formats say reload by hand` (dòng `FORMATS` không còn chữ "Tự làm mới theo đơn vị") |
| Banner AdMob thật trên staging | Kiểm tra thủ công (máy thật: quảng cáo thật) | Cài staging, đồng ý, mở màn xem thử trang "Banners": log `VGL.Ads` có `Banner gallery_banner reload in 60000 ms` sau mỗi lượt có quảng cáo và lượt gửi mới khoảng 60 giây một lần; mở native toàn màn hình hơn 60 giây rồi đóng: một lượt gửi ngay khi đóng, không hai |
| Banner, MREC của MAX trên máy có khóa và mã MAX | Kiểm tra thủ công (máy thật: quảng cáo thật; cần khóa SDK MAX và mã đơn vị MAX trong `local.properties`, CI và emulator không có) | Dev đặt `ads.maxSdkKey`, `ads.maxAdUnits`, cài staging, mở màn xem thử: log có `Banner ... MAX loaded` khoảng 60 giây một lần, không có dòng `loaded` nào giữa hai nhịp (MAX không tự làm mới); nếu thấy `loaded` lặp nhanh hơn nhịp hay không có `loaded` sau lượt đầu, ghi vào nhật ký và báo người duyệt (câu hỏi 2) |
| Đổi khóa trên Firebase staging | Kiểm tra thủ công (máy thật: Remote Config) | Đặt `time_reload_banner = 45`, `time_reload_banner_mrec = 0`, công bố: log `Banner gallery_banner reload in 45000 ms` từ nhịp sau; MREC giữ quảng cáo, log `Banner gallery_banner_mrec reload off`. Trả về mặc định sau khi thử |
| Tài liệu B-02, B-06 tới B-09, 8.2, 10.1, 11, `ads-guide.md`, `ads-plan.md`, `to-app.md` | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc các đoạn đã sửa, đối chiếu mục 3.1 bước 10; `scripts/history-refs.py` qua |

**Ngoại lệ viết test trước:** sửa tài liệu, KDoc; hai file bảng khóa sinh tự động. `MaxBannerAdViewTest.pauseBeforeBuild_stopsRightAfterBuild` và `SdkBannerAdViewTest.fixedBannerRequest_hasNoCollapsibleExtras` giữ câu khẳng định nên qua ngay; chúng không phải test viết trước.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi tần suất và người làm mới banner cố định, MREC của AdMob và MAX), 7 (module core `:core:ads`). Không chạm 2 (không nâng SDK), 3, 4 (không thêm hay sửa sự kiện; lượt nạp lại có `latency_ms` theo luật có sẵn), 5, 6, 8, 9.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Mười file code trong `:core:ads`, đổi giao diện nội bộ của vòng nạp, view AdMob, view MAX; script và tài liệu sinh tự động |
| B. Độ rõ của yêu cầu | 1 | Spec đã xác nhận, câu hỏi 52 đã trả lời; còn chi tiết AI tự quyết (thứ tự gọi dừng của MAX, câu hỏi 2) |
| C. Khả năng kiểm chứng tự động | 2 | Luật có test JVM và Compose UI; MAX không tự làm mới, và lỗi làm mới hai lần khi đơn vị production còn bật tự làm mới, chỉ thấy trên máy thật có mã thật |
| D. Ảnh hưởng chéo | 2 | `BannerLoads.kt`, `AdSlot.kt`, `AdsSettings.kt`, `AdsFlags.kt`, `AdsController.kt`, `scripts/ads-plan.py`, tài liệu quảng cáo cũng do T-087, T-089 sửa; T-072 (doanh thu MAX, chưa có plan) sẽ sửa `MaxBannerAdView.kt` |
| E. Độ mới của kỹ thuật | 1 | Biến thể của nạp tay banner thu gọn và của dừng tự làm mới MAX đã có |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: mọi banner cố định, MREC và lớp banner của thanh đáy (`AdSlot.kt`, `BannerLoads.kt`, `BannerAdView.kt`, `MaxBannerAdView.kt`, `PlatformBanners.kt`), `AdsSettings.kt`, `AdsFlags.kt`, bảng khóa sinh tự động, `scripts/ads-plan.py`, tài liệu quảng cáo, `docs/ops/to-app.md`.
- Nơi đọc file plan đổi (L-020), không bị ảnh hưởng: `scripts/check.sh` (dòng 33), `.github/workflows/pr-check.yml` (dòng 82 tới 89), `.github/workflows/main-guard.yml` (dòng 80) chỉ chạy `scripts/tests/run.sh` như một lệnh; `scripts/main-guard-verified.sh` có `scripts/tests/run.sh` trong danh sách file của cổng (dòng 32) và hai file bảng khóa trong `GRADLE_READ_DOCS` (dòng 38), plan không đổi tên hay chỗ, nên main-guard chạy đầy đủ cho commit của task, đúng ý; `scripts/local-setup.sh` (dòng 25) và `app/src/test/kotlin/com/vglstudio/template/ProductionPropertiesTest.kt` (dòng 87) chỉ nhắc `run.sh` trong chú thích; `app/build.gradle.kts` (dòng 169, 170) là task sinh và so bảng khóa mà plan dùng; `scripts/template-sync.sh` (dòng 34, 35) để hai file bảng khóa trong `NEVER_SYNCED`. `docs/ops/to-app.md` không có script nào đọc nội dung: `scripts/new-app.sh` (dòng 136) chỉ nhắc tên trong chú thích, `template-sync.sh` đồng bộ nó như mọi tài liệu của dự án mẫu. `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` so hai file bảng khóa với khóa trong code: qua sau khi sinh lại (mục 4). `AdPlacement.kt` đọc bởi `scripts/release.sh` (dòng khai báo vị trí), `scripts/ads-plan.py` (KDoc của từng vị trí) và `AdPlacementSourceTest`: plan chỉ sửa KDoc của hai mục `AdFormat`, không đổi dòng khai báo hay KDoc của vị trí. `docs/ops/ads-business-rules.md` chỉ được các test sau nhắc trong chú thích, không mở lúc chạy: `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdNetworkResponsesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt`; `AdsSettingsTest.kt` chép tay bảng 10.1 (có trong mục 3.2). Plan không đổi MC-02, kích thước 5.7, tên tham số của mục 8 hay khóa `is_log_af_*`.
- Task khác có thể xung đột: T-087 (phải xong trước: `awaitReloadTick`, `reloadIntervalMillis`, `runBannerLoads(reloadMillis)`, nhóm "Banner"); T-072 (doanh thu và phễu của MAX, chưa có plan) sẽ thêm bộ nghe doanh thu vào `MaxBannerAdView.kt`: plan T-072 đọc lại luật B-09 mới; T-089 (người khác viết) sửa `AdsSettings.kt`, `scripts/ads-plan.py`, bảng khóa: chạy khác phiên hay sau để không xung đột dòng ở hai file sinh tự động; T-091 (một quảng cáo neo đáy) sẽ chạm `AdSlot.kt` của banner cố định, thu gọn: làm sau T-088.
- Đề xuất thứ tự chạy: sau T-087. Trước khi phát hành bản app đầu tiên có thay đổi này, người ở app tắt tự làm mới trên đơn vị production (mục 3.1 bước 10).

## 7. Điều kiện dừng riêng

- Một test có sẵn của B-09 hay T-085 trong `PlatformBannersTest` đỏ vì logic khung (không vì chữ ký): plan không đổi logic của `OrderedBannerAdView`; dừng.
- Banner thu gọn đổi hành vi (một test `manual_*` của `BannerLoadsTest` hay `collapsibleSends_*` đỏ không vì chữ ký): dừng.
- Ảnh chuẩn screenshot nào đổi: plan không đổi giao diện; dừng.
- Phiên thấy cần đổi phiên bản SDK MAX hay AdMob để dừng tự làm mới: dừng (điểm chặn 2).

## 8. Câu hỏi cho người duyệt

1. **Đơn vị production còn bật tự làm mới.** Code không tắt được tự làm mới của đơn vị AdMob (cài đặt trên AdMob); đơn vị còn bật thì banner làm mới hai lần (theo đơn vị và theo khóa). AI chỉ ghi việc tắt vào `ads-guide.md` (mục 5, 9) và `to-app.md` (mục 7). Anh có muốn thêm một dòng vào danh sách kiểm của `docs/ops/release.md` không? Plan không sửa `scripts/release.sh` (không được chạm).
2. **Thứ tự dừng tự làm mới của MAX.** AI gọi `stopAutoRefresh()` cả trước và sau `loadAd()` đầu của view MAX, vì chưa kiểm trên máy thật SDK 13.6.4 nhận lệnh nào. Cách này không hại nếu SDK nhận cả hai. Anh đồng ý, hay muốn chờ một lần thử trên máy có mã MAX trước khi duyệt?
3. **Lỗi lượt nạp lại khi quảng cáo cũ còn hiện.** Lượt nạp lại lỗi (cả hai nền tảng) tính một lần lỗi của B-02 và thử lại sau 2, 4, 6, 8, 10 giây, trong lúc quảng cáo cũ vẫn hiện; như banner thu gọn hôm nay. AI giữ như vậy cho banner cố định và MREC. Anh đồng ý, hay muốn lượt nạp lại lỗi chỉ chờ nhịp sau?

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
