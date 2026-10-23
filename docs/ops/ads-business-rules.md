# LUẬT NGHIỆP VỤ QUẢNG CÁO (THEO ADSKIT PARTNER)

Tài liệu này là đặc tả nghiệp vụ cho cơ chế quảng cáo của dự án mẫu. Nó dành cho dev, AI viết plan và người duyệt. Quyết định nằm ở ADR-0026 (hành vi theo AdsKit PARTNER) và ADR-0027 (AppsFlyer). Cách thêm vị trí, thử trên staging và phát hành nằm ở [ads-guide.md](ads-guide.md); cách đặt tên khóa ở [remote-config.md](remote-config.md).

## 0. Cách đọc

- **Nguồn:** thư viện AdsKit (repo riêng `VGL-Studio/AdsKit`, nhánh `main`, commit `bec72bd`, ngày 2026-10-22), bộ hành vi `PARTNER`. AdsKit đã chạy ở nhiều app có doanh thu tốt. Ta chỉ lấy **một** bộ hành vi là PARTNER: không có bộ `CURRENT`, không đổi bộ hành vi qua Remote Config.
- **Ta tự viết lại:** Compose, `:core:ads`, `:core:config`, `:core:analytics`, Hilt, `AppLogger`, test viết trước. Không chép code của AdsKit. Không chép bất kỳ khóa thật nào (AdsKit có khóa SDK AppLovin trong resource; bản của ta không có, mục 9).
- **Dẫn chứng:** mỗi luật có cột AdsKit ghi `đường dẫn:dòng` để người duyệt kiểm lại. Quy ước: `M/` là `src/main/java/com/vgl/adskit/`, `T/` là `src/test/java/com/vgl/adskit/`, `R.md` là `README.md`, `DG` là `docs/deployment-guide.md` của AdsKit.
- **Ai làm:** cột "Chủ" ghi phần nào của app sở hữu luật.
  - `ads`: `:core:ads`. Mọi cổng, bộ đếm, nạp và hiển thị.
  - `app`: vỏ app `:app` (màn loading đóng vai splash, `MainActivity`, `AppNavHost`, luồng giới thiệu nếu app có). AdsKit cũng để phần này cho app chủ.
  - `feature`: module feature, chỉ gọi composable và hàm của `:core:ads`.
  - `analytics`: `:core:analytics` (Firebase Analytics, AppsFlyer).
- **Thời gian:** mọi khoảng chờ và mốc trong tài liệu này đo bằng đồng hồ đơn điệu của máy (thời gian kể từ lúc máy khởi động), không đo bằng giờ hệ thống. Người dùng đổi giờ máy thì luật vẫn đúng (mục 12, AR-01).

Từ ngữ:

| Từ | Nghĩa |
| --- | --- |
| Quảng cáo toàn màn hình | App open (AOA), interstitial (inter), rewarded. Native toàn màn hình sau inter tính là một phần của cụm inter |
| Cổng đồng ý | Được phép gửi yêu cầu quảng cáo hay không, theo UMP và quyền tạm của PARTNER |
| Quyền tạm | Quyền gửi yêu cầu khi UMP chưa trả lời xong: `CACHED_EARLY` hoặc `ERROR_ALLOWED` (mục 1) |
| Vị trí (placement) | Tên màn hình hay hành động đặt quảng cáo, dùng để bật tắt và để ghi doanh thu theo vị trí |
| Khe (slot) | Một đơn vị quảng cáo theo định dạng (inter, app open, banner, native đáy...). Nhiều vị trí dùng chung một khe |
| Chặn quảng cáo tự hiện | Tạm không cho quảng cáo khi quay lại app (resume) trong lúc app tự mở màn khác (mục 6) |

## 1. Đồng ý và khởi động SDK

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| S-01 | Khởi tạo `:core:ads` đúng một lần mỗi process. Gọi lại không làm gì | ads | `M/AdsKit.kt:312-538` |
| S-02 | Mỗi lần mở app, `MainActivity` gọi một lượt hỏi đồng ý: cập nhật thông tin đồng ý của UMP, hiện form nếu UMP báo cần, rồi đọc "được phép gửi yêu cầu". Ngoài vùng có luật, UMP trả gần như ngay, không có form | app gọi, ads làm | `M/consent/ConsentManager.kt:57-101` |
| S-03 | **Init sớm (`CACHED_EARLY`):** ngay sau khi gửi lượt hỏi, nếu UMP đã có đồng ý dùng được từ phiên trước thì khởi tạo SDK quảng cáo ngay, không chờ UMP trả lời | ads | `M/consent/AdsStartupState.kt:336-344,91-96` |
| S-04 | **Lỗi mạng của UMP (`ERROR_ALLOWED`):** lượt hỏi lỗi vì mạng (mã `INTERNET_ERROR`, `TIME_OUT`, ở bước cập nhật hay bước form) thì coi như được phép, nhưng không chốt kết quả | ads | `M/consent/ConsentGate.kt:126-131` |
| S-05 | Lỗi khác của UMP (`INTERNAL_ERROR`, `INVALID_OPERATION`, mã lạ) là lỗi cấu hình: không cấp quyền tạm. Form lỗi vì Activity bị hủy, đang đóng hay đổi cấu hình là "bị ngắt" (`FORM_INTERRUPTED`): người dùng chưa trả lời, lượt sau hỏi lại | ads | `M/consent/ConsentGate.kt:86-103` |
| S-06 | Chỉ chốt kết quả **sạch** (cập nhật thành công và form đóng không lỗi). Đã chốt thì các lượt hỏi sau trong process trả ngay, không gọi UMP | ads | `M/consent/ConsentGate.kt:137-139`, `M/consent/AdsStartupState.kt:288-293` |
| S-07 | Lượt hỏi thứ hai đến khi lượt đầu chưa xong thì chờ chung kết quả, không mở form thứ hai | ads | `M/consent/AdsStartupState.kt:294-301` |
| S-08 | `ERROR_ALLOWED` chỉ dùng khi app ở foreground. App ở nền thì không gửi yêu cầu quảng cáo nào bằng quyền này | ads | `M/consent/AdsStartupState.kt:207-208` |
| S-09 | Đang có `ERROR_ALLOWED` thì hỏi lại UMP khi mạng về, khi app lên foreground, hoặc khi nạp sẵn lại. Chỉ hỏi trên đúng Activity đã hỏi lần trước, khi nó đang hiện và không có quảng cáo toàn màn hình che. Hỏi xong mới nạp | ads | `M/consent/AdsStartupState.kt:357-426`, `M/AdsKit.kt:769-772` |
| S-10 | Đang có quyền tạm mà UMP báo cần hỏi lại (không còn được phép): đóng cổng **ngay**, kể cả lúc form đang mở. Giữ lại một lượt nạp sẵn để chạy khi cổng mở lại | ads | `M/consent/AdsStartupState.kt:109-116` |
| S-11 | Quảng cáo đã nạp bằng quyền tạm **không lên màn hình** khi cổng đã đóng: app open, rewarded, native (lấy ra từ khe hay pool), lượt đổi quảng cáo của native thu gọn | ads | `M/consent/ConsentGate.kt:150`, `T/manager/PartnerLoadedAdShowGuardTest.kt` |
| S-12 | Có công tắc tắt quyền tạm từ xa (`is_enable_consent_shortcut`, mục 10). Tắt thì thu hồi **ngay** quyền chỉ có nhờ `ERROR_ALLOWED`; quyền thật của UMP giữ nguyên | ads | `M/AdsKit.kt:876-892`, `M/consent/AdsStartupState.kt:146-152`, `T/PartnerConsentRevocationTest.kt` |
| S-13 | Đường đi của đồng ý ghi được để đo: `CACHED_EARLY`, `GRANTED`, `DENIED_OR_REQUIRED`, `ERROR_ALLOWED`, `INTERRUPTED_REGATHER` | ads | `M/consent/AdsStartupState.kt:27`, `M/consent/ConsentGate.kt:153-161` |
| S-14 | **Rút đồng ý:** nút quyền riêng tư (`PrivacyOptionsButton`) mở form của UMP. Form đóng thì đọc lại quyền: rút thì đóng mọi cổng; cấp lại khi SDK chưa khởi tạo thì khởi tạo; cấp lại khi đã khởi tạo thì nạp sẵn | ads | `M/AdsKit.kt:779-804`, `M/consent/ConsentGate.kt:44-53` |
| S-15 | Sau khi rút: cổng nạp đóng ở đầu **mọi** lượt nạp, kể cả các vòng tự nạp lại; cổng hiển thị đóng; quảng cáo đã nạp không hiện | ads | `M/AdsKit.kt:574-581,1142,1195`, commit `0a5c65b` |
| S-16 | Mọi form của UMP (form đồng ý và form quyền riêng tư) chặn quảng cáo khi quay lại app suốt lúc form mở; nhả đúng một lần khi form đóng, kể cả khi đóng có lỗi | ads | `M/consent/ConsentManager.kt:171-191`, `T/consent/ConsentFormAdSuppressTest.kt` |
| S-17 | Cài đặt gỡ lỗi của UMP (vùng địa lý giả, máy test) chỉ dùng ở staging; lượt hỏi lại dùng lại cài đặt của lượt trước. Staging giả lập vùng EEA (ADR-0013) | ads | `M/AdsKit.kt:709-713`, `M/consent/ConsentManager.kt:137-141` |

## 2. Thời điểm khởi tạo, nạp sẵn, mạng

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| S-20 | SDK quảng cáo khởi tạo đúng một lần. Không khe nào tự nạp khi được tạo; một kế hoạch nạp sẵn điều phối việc nạp | ads | `M/AdsKit.kt:544-564` |
| S-21 | "SDK sẵn sàng" khi callback khởi tạo về, **hoặc** sau tối đa 35 giây kể từ lúc khởi tạo. Chỉ nạp sẵn khi SDK sẵn sàng | ads | `M/AdsKit.kt:560-572`, `M/behavior/PartnerAdsConstants.kt:19`, `M/consent/AdsStartupState.kt:214-218` |
| S-22 | **Kế hoạch nạp sẵn:** danh sách nhóm khe chạy lần lượt. Nhóm sau bắt đầu khi mọi khe của nhóm trước xong (có quảng cáo hay lỗi đều tính), hoặc sau tối đa 6 giây. Kiểm mỗi 200 ms. Khe không có trong kế hoạch thì không nạp sẵn. Mặc định một nhóm gồm mọi khe; app xếp khe người dùng thấy đầu tiên (app open) ở nhóm đầu | ads | `M/AdPreloadPlan.kt:9-48`, `M/AdsKit.kt:91-98,639-677` |
| S-23 | Lượt nạp sẵn bị chặn vì cổng đóng được giữ lại, chạy đúng một lần khi cổng mở; không chạy chồng lên kế hoạch đang chạy | ads | `M/consent/AdsStartupState.kt:159-193,462-492` |
| S-24 | **Mạng về:** chỉ tính mạng có Internet đã được xác thực; gộp các lần đổi mạng trong 3 giây. Mạng về thì nạp sẵn lại và đặt lại thời gian chờ thử lại của native. Dùng theo dõi kết nối chung của app (T-016), không viết bộ theo dõi thứ hai | ads | `M/net/AdsNetworkMonitor.kt:37-92`, `M/AdsKit.kt:590-605` |
| S-25 | Khi Activity chính quay lại (resume), nạp bù các khe native theo thứ tự kế hoạch, chỉ khi kế hoạch đã chạy xong ít nhất một lần, không có kế hoạch đang chạy và không có quảng cáo toàn màn hình | ads | `M/manager/NativePolicyDecisions.kt:115-132`, `M/AdsKit.kt:613-622` |

## 3. Quảng cáo toàn màn hình: cổng chung

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| F-01 | Không bao giờ có hai quảng cáo toàn màn hình chồng nhau. Lượt bị chặn vì đang có một cái hiện thì **bỏ hẳn**, không xếp hàng (kết quả `busy`) | ads | `M/AdsKit.kt:946-949,1186-1195`, commit `e3c1551` |
| F-02 | Cờ "đang hiện" chỉ hạ ở callback thật của SDK (đóng, hiện lỗi). Có trần 10 phút, vì người dùng bấm quảng cáo rồi đọc trong trình duyệt vẫn hợp lệ | ads | `M/manager/FullscreenShowState.kt:21-66` |
| F-03 | **Hồi phục khi kẹt:** đúng Activity đã dùng để hiện quảng cáo bị che (pause) rồi hiện lại (resume) mà cờ vẫn bật thì coi là kẹt: hạ cờ của mọi loại toàn màn hình, ghi mốc `ads_interval` lúc phát hiện, chạy hàm hoàn tất đang chờ đúng một lần. Phải có "che trước rồi mới hiện lại". Không tự đóng màn hình quảng cáo đang treo | ads | `M/controller/StuckFullscreenAdRecovery.kt:34-74`, `M/controller/ShowHostResumeSignal.kt:18-40`, `M/AdsKit.kt:991-1019,1307-1314` |
| F-04 | Hỏi "hiện được không" mà chưa có quảng cáo thì kích nạp (không nạp chồng: không nạp khi đang nạp, đã có, hay đang chờ thử lại) | ads | `M/AdsKit.kt:1120-1133`, `M/manager/FullscreenPolicyDecisions.kt:35-36` |
| F-05 | **Thử lại sau lỗi nạp** (inter, app open, rewarded): chờ `min(n x 2 giây, 10 giây)` với lần lỗi thứ n, mọi mã lỗi, không giới hạn số lần, không ngẫu nhiên. Bộ đếm lỗi về 0 khi nạp thành công | ads | `M/retry/AdRetryPolicy.kt:54-200`, `M/behavior/PartnerAdsConstants.kt:42-43` |
| F-06 | **Nạp lại sau khi đóng hay hiện lỗi:** inter và rewarded chờ 2 giây; app open nạp ngay | ads | `M/manager/InterstitialAdManagerImpl.kt:270-272`, `M/manager/AppOpenAdManagerImpl.kt:253-266`, `M/behavior/PartnerAdsConstants.kt:38` |
| F-07 | Gọi hiện khi chưa có quảng cáo: chạy hàm hoàn tất ngay, nạp lại, không hiện thông báo nào | ads | `M/manager/InterstitialAdManagerImpl.kt:257-266` |

## 4. Mốc `ads_interval`

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| I-01 | Một mốc chung: "lần gần nhất một inter rời màn hình". Cổng mở khi thời gian từ mốc tới nay lớn hơn hoặc bằng `ads_interval` giây. Chưa có mốc thì không chặn. Đúng biên thì cho qua | ads | `M/controller/InterstitialThrottleDecision.kt:9-29` |
| I-02 | Chỉ inter đẩy mốc, ở ba điểm: khi inter đóng, khi hạ cờ kẹt (F-03), và khi đóng cả cụm inter rồi native (K-05). App open và rewarded **không** đẩy mốc | ads | `M/manager/FullscreenPolicyDecisions.kt:78-79` |
| I-03 | Mốc chỉ giữ trong bộ nhớ của process. Mở lại app (process mới) thì bắt đầu từ chưa có mốc. Không lưu qua phiên | ads | `M/controller/InterstitialThrottle.kt:51-88` |
| I-04 | Bỏ cổng khi đọc (`ignoreInterval`) và ghi mốc hay không (`recordInterval`) là hai lựa chọn độc lập của từng lượt gọi. Vị trí có bộ đếm riêng (ví dụ `ads_interval_<vị trí>`) gọi với `recordInterval=false`, áp ở mọi điểm ghi | ads | `M/AdsKit.kt:121-134,1150-1166`, commit `61981bb` |
| I-05 | Ghi mốc trước khi đếm lượt hiện hay ghi sự kiện: ghi sự kiện lỗi thì mốc vẫn còn. Ghi mốc ở callback thật của SDK, không ở hàm hoàn tất chung | ads | `M/AdsKit.kt:358-359`, commit `32afa80`, `f2dab3d` |

## 5. Từng định dạng

### 5.1 Interstitial

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| N-01 | Hiện được khi: cổng đồng ý mở, có quảng cáo, không có quảng cáo toàn màn hình khác, và qua mốc `ads_interval` (hoặc lượt gọi bỏ cổng) | ads | `M/AdsKit.kt:1117-1148` |
| N-02 | Feature gọi inter qua `rememberShowInterstitial(placement)` ở một hành động của người dùng (bấm nút sang màn sau, xong một việc). Bị chặn thì app đi tiếp ngay. Inter đóng thì hàm hoàn tất chạy **một lần**, sau khi màn quảng cáo đã đóng | feature gọi, ads làm | `M/AdsKit.kt:1157-1244`, `M/manager/InterstitialAdManagerImpl.kt:236-249` |
| N-03 | App biết inter có thật sự lên màn không (cờ "lần gần nhất đã hiện"), để chỉ nối màn sau quảng cáo khi quảng cáo đã hiện. Hàm hoàn tất cũng chạy khi chưa nạp, hiện lỗi hay hồi phục kẹt | ads | `M/AdsKit.kt:146-154,1225` |
| N-04 | Trong lúc inter hiện, `:core:ads` cho biết trạng thái "đang hiện" để vỏ app khóa nút quay lại | ads, app | `R.md:153-157` |
| N-05 | Đếm số lần inter đã hiện từ lúc cài app, lưu trên máy, tăng khi inter hiện. Dùng cho sự kiện `af_inters_displayed_{N}_times` (mục 8) | ads | `M/AdsKit.kt:1348-1377`, `T/InterDisplayCounterTest.kt` |

### 5.2 Native sau inter (combo)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| K-01 | Combo chạy khi `is_show_native_after_inter` bật. Giá trị đọc **lúc inter đóng**. Inter đóng thì mở màn native toàn màn hình; khóa tắt thì đi tiếp như inter thường | ads | `M/AdsKit.kt:1084-1115`, `T/controller/ResumeAdShowPlanTest.kt:154` |
| K-02 | Đếm ngược **3 giây**, bắt đầu khi inter đã rời màn hẳn; chờ inter rời màn tối đa 2 giây (kiểm mỗi 200 ms) để không kẹt khi cờ inter kẹt. Mốc kết thúc là thời điểm tuyệt đối, giữ qua lúc Activity bị tạo lại | ads | `M/compose/NativeAfterInterScreen.kt:34-48,99-106,142-158` |
| K-03 | Nút đóng ở góc **trên phải**, cách mép 12 dp, vòng tròn **36 dp**, biểu tượng X 20 dp. Trong lúc đếm, chỗ đó là vòng 36 dp chứa số giây (16 sp, đậm). Nút dùng được khi đếm xong **hoặc** khi nạp lỗi | ads | `M/compose/NativeAfterInterScreen.kt:116-121,183-195` |
| K-04 | Nạp lỗi thì tự đóng khi hết đếm ngược. Quảng cáo chưa về thì hiện khung chờ toàn màn hình và vẫn đóng được sau 3 giây. Bấm đóng chỉ ăn một lần | ads | `M/compose/NativeAfterInterScreen.kt:115,133-140,168-170`, commit `4362a78` |
| K-05 | Đóng màn native thì đẩy mốc `ads_interval` về lúc đóng cả cụm. Trong lúc màn native hiện, quảng cáo khi quay lại app bị chặn (mục 6) | ads | `M/AdsKit.kt:1021-1052`, `M/compose/NativeAfterInterScreen.kt:78-84` |
| K-06 | Mỗi lần hiện sở hữu một quảng cáo native riêng, hủy khi rời màn, rồi nạp sẵn cho lần sau | ads | `M/compose/NativeAfterInterScreen.kt:92-95,159-167` |
| K-07 | Mỗi lần hiện ghi đúng một kết quả cơ hội: `shown` nếu có quảng cáo; `not_ready` nếu nạp lỗi hay người dùng đóng khi quảng cáo chưa về | ads | `M/compose/NativeAfterInterScreen.kt:55-65`, commit `80e82a8` |
| K-08 | Nút quay lại của máy trên màn native: AdsKit không xử lý; ta chặn trong lúc đếm ngược và cho đóng khi nút đóng đã dùng được (câu hỏi mở của spec ads) | ads | `R.md:153-154` |

### 5.3 App open lúc mở app

AdsKit không có splash; app chủ tự làm. Ở dự án mẫu, màn loading (`:feature:loading`, ADR-0018) đóng vai splash.

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| A-01 | Lần mở đầu sau khi cài (hoặc sau khi xóa dữ liệu) dùng khóa `show_open_ads_first_open`; từ lần mở thứ hai dùng `show_open_ads`. App lưu dấu "đã mở lần đầu" trên máy | app | `M/AdFeatureFlags.kt:4-6`, `R.md:113-115` |
| A-02 | `open_ad_type` là `"aoa"` hay `"inter"`. Với `"inter"`: hiện inter, bỏ cổng `ads_interval` khi đọc, và lượt hiện vẫn đẩy mốc. Với `"aoa"`: hiện app open nếu sẵn sàng, không thì đi tiếp | app gọi, ads làm | `M/compose/AdInterstitialScreen.kt:53-94` |
| A-03 | Màn loading chờ có giới hạn: đồng ý tối đa 10 giây, SDK tối đa 35 giây, app open nạp xong tối đa 40 giây, kiểm mỗi 250 ms. Hết thời gian chờ đồng ý thì không cấp quyền: form còn mở sau 10 giây thì SDK vẫn chưa khởi tạo | app | `M/behavior/PartnerAdsConstants.kt:16-25` |
| A-04 | Trong lúc chờ, chỉ hỏi "đã sẵn sàng chưa" (không tự nạp) và "đảm bảo đang nạp" (không gửi thêm yêu cầu khi đang nạp, đã có hay đang chờ thử lại). Kiểm 250 ms suốt 40 giây mà không có quảng cáo thì chỉ gửi yêu cầu theo lịch thử lại | ads | `R.md:171-173`, `M/manager/FullscreenPolicyDecisions.kt:35-36` |
| A-05 | Luôn hiện qua một hàm của `:core:ads` có cổng đồng ý, cổng chồng nhau và hồi phục kẹt; vỏ app không gọi SDK | app gọi, ads làm | commit `5d814c2`, `ee7d875` |
| A-06 | Vỏ app tự bỏ app open (hết giờ chờ mà chưa có quảng cáo) thì vẫn ghi cơ hội `not_ready` cho vị trí đó | app | `M/AdsKit.kt:1273-1292`, commit `ff59e7a` |
| A-07 | Quảng cáo app open hết hạn từ đủ **1 giờ** sau khi nạp; lượt nạp bỏ quảng cáo hết hạn (trừ cái đang hiện) | ads | `M/manager/FullscreenPolicyDecisions.kt:12-22`, `M/manager/AppOpenAdManagerImpl.kt:122-130` |
| A-08 | Không hiện app open trong **1 giây** sau lần app open trước đóng | ads | `M/manager/FullscreenPolicyDecisions.kt:28-29` |

### 5.4 Quảng cáo khi quay lại app (resume)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| A-10 | Theo dõi vòng đời của cả process: app lên foreground thì xét hiện; app xuống nền thì ghi mốc. Lần lên foreground đầu tiên của process là lúc mở app (màn loading lo), bỏ qua, kể cả khi lúc đó cổng đồng ý chưa mở | ads | `M/controller/AppOpenAdController.kt:53-73,98-100` |
| A-11 | **Thứ tự cổng:** lần đầu của process, rồi đang bị chặn (rời app có chủ đích, màn tự mở, form UMP), rồi `show_resum_ads` tắt, rồi đang có quảng cáo toàn màn hình, rồi khóa 1 giây sau app open trước (chỉ khi resume là app open), rồi chưa có quảng cáo. Không có cổng thời gian nào: không có thời gian tối thiểu ở nền, không bị `ads_interval` chặn | ads | `M/controller/AppOpenAdPolicy.kt:127-162`, `T/controller/ResumeAdPartnerRulesTest.kt:130,165` |
| A-12 | `resume_ad_type` đúng bằng `"inter"` thì resume là inter (bỏ cổng `ads_interval`, rồi nối combo nếu `is_show_native_after_inter` bật). Mọi giá trị khác là app open | ads | `M/controller/AppOpenAdPolicy.kt:165-166,203-208` |
| A-13 | Bị chặn vì chưa có quảng cáo thì kích nạp lặng lẽ; lượt này bỏ, lượt sau có quảng cáo | ads | `M/controller/AppOpenAdController.kt:182-235` |
| A-14 | **Bấm quảng cáo thì bỏ lượt resume kế tiếp:** mọi quảng cáo (toàn màn hình, native, banner) khi được bấm đánh dấu "rời app"; dấu này nhả ở lần Activity chính hiện lại. Bấm nhiều lần chỉ giữ một dấu | ads | `M/manager/AdClickResumeSkip.kt:12-14`, `T/manager/AdClickSkipsResumeTest.kt` |
| A-15 | Vị trí của resume luôn là `resume`. Resume cũng đi qua hàm hiện có hồi phục kẹt | ads | `M/controller/AppOpenAdController.kt:126-153` |

### 5.5 Banner

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| B-01 | Banner cố định: thích ứng theo chiều rộng màn hình, neo ở đáy. Lần nạp đầu chờ cổng đồng ý, không nạp ngay lúc dựng giao diện | ads | `M/compose/BannerAdFixed.kt:39-157`, commit `0a5c65b` |
| B-02 | **Làm mới:** dùng đơn vị banner riêng (khe "banner partner") đã bật tự làm mới trên AdMob; app không tự nạp lại. Banner tạm dừng và chạy lại theo Activity chính. Lỗi thì thử lại `min(n x 2 giây, 10 giây)`; tới hạn thử lại khi Activity không hiện thì dời tới lần hiện lại kế tiếp | ads | `M/manager/NativePolicyDecisions.kt:84-92`, `M/compose/BannerAdFixed.kt:58-141` |
| B-03 | Banner thu gọn được (collapsible, bung từ đáy): mỗi yêu cầu có mã yêu cầu mới, nên mỗi lần làm mới là một banner bung ra mới | ads | `M/compose/BannerAd.kt:23-65` |
| B-04 | Mã đơn vị rỗng thì không gửi yêu cầu, không chiếm chỗ | ads | `T/compose/BannerAdRequestDecisionTest.kt` |
| B-05 | Mọi lượt nạp lại hay thử lại của banner chỉ chạy khi app ở foreground (AdsKit thiếu luật này ở banner, mục 12) | ads | `M/compose/BannerAdFixed.kt:142-150` |

### 5.6 Thanh quảng cáo ở đáy màn hình

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| D-01 | Hai lớp. Khe đáy luôn có: native nhỏ nếu `is_show_native_bottom` bật, không thì banner cố định. Lớp native thu gọn nổi đè lên khe đáy khi native bật. Người dùng thu gọn lớp nổi thì lộ khe đáy bên dưới | ads | `M/compose/BottomAdBar.kt:20-66` |
| D-02 | Khe đáy native cao **56 dp**; banner cao theo nội dung. Nạp lỗi thì khe co về 0 dp | ads | `M/compose/BottomAdSlot.kt:42-53`, commit `d7f0a6d` |
| D-03 | Lớp nổi chỉ lấp chiều rộng, không lấp cả khoảng trống, để không đẩy thanh điều hướng của app ra ngoài và không nhận chạm thay app | ads | `M/compose/BottomAdBar.kt:50-57`, commit `b4781fa` |
| D-04 | Khoảng chừa dưới nội dung chỉ tính khe đáy và chỉ khi khe đã có quảng cáo, đúng bằng chiều cao thật của khe (56 dp với native). Lớp nổi không tính | ads | `M/compose/BottomAdDecision.kt:26-45` |
| D-05 | Vị trí của thanh đáy đọc **lúc có sự kiện** (hiện, bấm, doanh thu), không chụp lúc dựng, vì thanh đáy sống qua nhiều màn hình | ads | `M/compose/BannerAdFixed.kt:55-57`, `M/compose/NativeAdSmall.kt:64-66` |

### 5.7 Native: các biến thể và kích thước

Kích thước theo layout của AdsKit (`src/main/res/layout/adskit_native_*.xml`). Màu lấy từ token của `:core:designsystem`, không lấy màu của AdsKit. Màu chữ native không đổi theo chế độ tối của máy khi nền quảng cáo không đổi (AdsKit commit `9994dbc`).

| Biến thể | Nguồn quảng cáo | Kích thước chính | Làm mới | AdsKit |
| --- | --- | --- | --- | --- |
| Native nhỏ ở khe đáy | Khe một quảng cáo `native_bottom` | Cao 56 dp; icon 36 dp; tiêu đề 14 sp; mô tả 12 sp; CTA cao 34 dp, chữ 13 sp; nhãn "Ad" 10 sp; không có media | Thay nội dung tại chỗ mỗi 60 giây, bắt đầu trễ 500 ms sau lớp thu gọn; chỉ ở foreground; qua cổng đồng ý; lỗi thì giữ quảng cáo cũ | `M/compose/NativeAdSmall.kt:41-149`, `res/layout/adskit_native_small.xml` |
| Native thu gọn (lớp nổi) | Khe `native_collap` (một quảng cáo và một quảng cáo chờ) | Cao 250 dp; media lấp phần còn lại; icon 32 dp; tiêu đề 13 sp; CTA cao 36 dp; nút thu gọn 32 dp (biểu tượng 20 dp, cách mép 4 dp) ở góc trên phải, nền đen 40% | Mỗi 60 giây (mục 5.8) | `M/compose/NativeAdCollap.kt:33-247`, `res/layout/adskit_native_collap.xml` |
| Native đầy đủ | Khe `native_full` | Media cao 200 dp; icon 40 dp; tiêu đề 14 sp; cao theo nội dung | Không | `M/compose/NativeAdFull.kt`, `res/layout/adskit_native_full.xml` |
| Native toàn màn hình (trang giới thiệu, sau inter) | Khe `native_full` | Lấp đầy màn; icon 48 dp; tiêu đề 16 sp; CTA cao 52 dp. Bản trên trang giới thiệu không có nút điều khiển, app tự đặt nút Tiếp; lỗi thì không vẽ gì | Không | `M/compose/NativeAdFullscreen.kt:19-56`, `res/layout/adskit_native_fullscreen.xml` |
| Native giới thiệu (onboarding) | Pool `native_onboarding`, 3 quảng cáo | Cao 250 dp; icon 32 dp; CTA cao 36 dp | Không | `M/compose/NativeAdOnboarding.kt:35-81`, `res/layout/adskit_native_onboarding.xml` |
| Native medium | Pool `native_medium`, 3 quảng cáo | Icon 40 dp; có nền mờ sau media và hàng sao; CTA cao 44 dp. Biến thể CTA lớn: CTA cao 56 dp, chữ 17 sp, không viết hoa toàn bộ. Chiều cao do nơi gọi quyết | Không | `M/compose/NativeAdMedium.kt`, `M/compose/NativeAdLayouts.kt:5-16`, `res/layout/adskit_native_medium*.xml` |
| Ô lưới và trang xem trước | Pool `native_content`, 5 quảng cáo, xin media dọc | Thẻ dọc: media lấp phần trên; icon 32 dp; tiêu đề 13 sp; khung chờ dạng thẻ dọc | Không | `M/compose/PooledNativeAd.kt:78-186`, `res/layout/adskit_native_card_portrait.xml` |

Luật chung của native:

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| O-01 | Mỗi lượt hiện sở hữu riêng một quảng cáo native: lấy ra thì khe tự nạp cái mới; rời màn thì hủy. Một quảng cáo chỉ ghi được một lượt hiển thị; dựng lại cùng một quảng cáo thì không có doanh thu | ads | `M/manager/NativeAdProvider.kt:31-61`, commit `51897f2` |
| O-02 | Hai chỗ cùng chờ một lượt nạp: chỗ thua thử lại một lần, không coi là hết quảng cáo | ads | `M/manager/NativeAdProvider.kt:12-29`, `T/manager/NativeAdClaimTest.kt` |
| O-03 | Quảng cáo về sau khi đã rời màn thì hủy ngay | ads | `M/compose/NativeAdSmall.kt:264-307`, `M/manager/NativeAdManagerImpl.kt:336-348` |
| O-04 | Native hết hạn từ đủ 55 phút sau khi nạp; quảng cáo chờ đã hết hạn thì bỏ, không đổi vào | ads | `M/manager/NativePolicyDecisions.kt:39-46`, `M/behavior/PartnerAdsConstants.kt:48` |
| O-05 | **Thử lại native:** còn giữ quảng cáo thì chờ 2, 4, 8, 16, 32 rồi 60 giây với mọi mã lỗi; chưa giữ quảng cáo nào thì thử lại cố định sau 5 giây. Bộ đếm về 0 khi thành công. Không gửi yêu cầu khi app ở nền; kiểm lại cổng đồng ý ngay trước khi gửi | ads | `M/manager/NativePolicyDecisions.kt:21-33,100-101`, `M/manager/NativeAdRequester.kt:33-58`, `M/behavior/PartnerAdsConstants.kt:50-56` |
| O-06 | Tùy chọn yêu cầu: video tắt tiếng, không có điều khiển riêng, một ảnh, media ngang (ô lưới xin media dọc) | ads | `M/manager/NativePolicyDecisions.kt:57-73` |
| O-07 | Đặt cách co giãn ảnh **trước** khi gắn quảng cáo vào khung, để ảnh không bị cắt | ads | commit `f3947af` |
| O-08 | Nhãn "Ad" luôn thấy được; biểu tượng AdChoices do SDK vẽ, không bị đè hay che | ads | `src/main/res/values/strings.xml` (`adskit_native_ad_label`) |

### 5.8 Native thu gọn

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| CL-01 | Quảng cáo đầu tiên mở rộng, trừ khi người dùng đã thu gọn. Lần nạp đầu chờ tối đa 10 giây (kiểm mỗi 200 ms); quá thời gian thì không vẽ | ads | `M/compose/NativeAdCollap.kt:69-93`, `M/compose/NativeAdCollapReloadPolicy.kt:29` |
| CL-02 | Làm mới mỗi 60 giây: nạp quảng cáo chờ, đợi tối đa 10 giây, rồi đổi vào và hủy cái cũ. Làm mới **chỉ đổi nội dung**, giữ nguyên trạng thái thu gọn. Lỗi thì giữ quảng cáo và trạng thái cũ. Ở nền thì bỏ nhịp | ads | `M/compose/NativeAdCollap.kt:97-188`, `M/manager/NativePolicyDecisions.kt:76-81` |
| CL-03 | **Bung lại:** hẹn giờ riêng, đếm từ lúc người dùng bấm thu gọn, kéo dài `time_expand_collap_ad` giây (mặc định 30). Giá trị 0 hay âm là không bao giờ bung lại. Làm mới không đặt lại hẹn giờ. Chỉ bung khi đang có quảng cáo | ads | `M/compose/NativeAdCollap.kt:190-217`, commit `28f6941`, `5e5deba` |
| CL-04 | Vòng làm mới đọc trạng thái thu gọn **hiện tại**, không dùng giá trị chụp lúc bắt đầu | ads | `M/compose/NativeAdCollap.kt:55-58`, commit `70d52db` |

### 5.9 Pool native (ô lưới, medium, giới thiệu)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| P-01 | Pool nạp bằng nhiều yêu cầu một quảng cáo riêng (yêu cầu nhiều quảng cáo một lần bỏ qua mediation). Mỗi lần lấy trả một quảng cáo chưa dùng, theo vòng tròn; không bao giờ trả cái đang hiện. Trả lại khi ô rời màn | ads | `M/manager/NativeAdPoolManager.kt:28-41`, `M/manager/RoundRobinPool.kt:12-77` |
| P-02 | Cỡ pool: ô lưới 5, giới thiệu 3, medium 3. Pool đếm cả quảng cáo đang dùng; dư một so với số ô cùng lúc | ads | `M/manager/NativeAdPoolManager.kt:425`, `M/AdsKit.kt:452-487` |
| P-03 | Nạp bù tuần tự, mỗi lúc một yêu cầu. Quảng cáo trong pool hết hạn từ đủ 55 phút | ads | `M/manager/NativeAdPoolManager.kt:113-141,354-359,427` |
| P-04 | Vị trí gán lúc **lấy** quảng cáo ra; bấm và doanh thu tính cho màn đang giữ quảng cáo | ads | `M/manager/NativeAdPoolManager.kt:363-381` |
| P-05 | **Pool cạn** khi không có quảng cáo rảnh, không có yêu cầu đang chạy, và không nạp được (chưa khởi tạo, mã rỗng, đang chờ thử lại, ở nền, cổng đồng ý đóng). Pool đầy mà mọi quảng cáo đang dùng thì không tính là cạn | ads | `M/manager/NativeAdPoolAvailability.kt:14-68` |
| P-06 | Ô chờ quảng cáo thử lấy mỗi 500 ms; khi pool cạn thì mỗi 2 giây, vẫn tiếp tục nên quảng cáo về muộn vẫn hiện. Ô cạn thì không vẽ gì, không giữ chỗ; nơi gọi chọn được hiện khung chờ thay vì ẩn | ads | `M/compose/PooledNativeAd.kt:26-29,62-71,101-133`, commit `a4b33ca` |
| P-07 | Ô trong pager chỉ tính là "đang hiện" khi trang đó đã dừng hẳn. Ô chưa từng hiện thì không ghi gì. Ghi `shown` khi ô vừa có quảng cáo vừa đang hiện; ghi `not_ready` khi ô rời màn mà chưa có quảng cáo. Native giới thiệu không có trạng thái này nên chỉ đặt **ngoài** pager | ads | `R.md:294`, `M/analytics/PoolCellOpportunity.kt`, `M/compose/NativeAdOnboarding.kt:28-30` |

### 5.10 Rewarded

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| R-01 | Feature mở hộp thoại hỏi xem quảng cáo (khóa `is_show_rw_get_item` bật cho hành động mở khóa hay xuất). Hộp thoại có tiêu đề, nút "Xem quảng cáo" (cao 56 dp, bo 28 dp), nút "Không, cảm ơn", và chỗ để app vẽ nội dung riêng | feature, ads | `M/compose/RewardedAdDialog.kt` |
| R-02 | Thưởng trao ngay trong callback thưởng của SDK, không chờ quảng cáo đóng | ads | `M/manager/RewardedAdManagerImpl.kt:215-276` |
| R-03 | Quảng cáo chưa sẵn sàng thì **chặn**: không cho qua mà không xem. Chỉ báo "kiểm tra kết nối" khi lỗi mạng; app luôn nhận "thất bại" để đóng hộp thoại | ads | `M/manager/RewardedAdManagerImpl.kt:277-288`, `R.md:226-228` |
| R-04 | Hiện lỗi thì báo thất bại. Đóng mà chưa được thưởng thì không gọi gì | ads | `M/manager/RewardedAdManagerImpl.kt:257-266` |
| R-05 | Rewarded không qua cổng `ads_interval` và không đẩy mốc | ads | `M/AdsKit.kt:426-444` |
| R-06 | Bấm "Xem" hai lần trước khi quảng cáo lên màn chỉ gọi SDK một lần (AdsKit gọi hai lần, mục 12) | ads | `M/manager/RewardedAdManagerImpl.kt:233-242` |

### 5.11 Native trên luồng giới thiệu

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| OB-01 | Khóa `is_show_native_onboarding` bật thì luồng giới thiệu có native. Vỏ app (hay feature giới thiệu) quyết trang nào có quảng cáo; `:core:ads` cung cấp native giới thiệu cao 250 dp (đặt ngoài pager) và native toàn màn hình không có nút điều khiển | app, ads | `DG:181`, `M/compose/NativeAdOnboarding.kt`, `M/compose/NativeAdFullscreen.kt` |
| OB-02 | Hai trang kề nhau không dùng chung một quảng cáo: mỗi ô lấy một quảng cáo riêng từ pool | ads | commit `bb15e59` |

## 6. Chặn quảng cáo tự hiện (hợp đồng với vỏ app)

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| G-01 | Chặn dùng **bộ đếm**, không dùng cờ đúng sai: mỗi nguồn nhả không xóa chặn của nguồn khác; bộ đếm không xuống âm | ads | `M/compose/ForegroundAdSuppressor.kt` |
| G-02 | Bật chặn thì báo xuống SDK **ngay** (Activity có thể dừng trước khung hình sau); nhả chặn thì để vỏ app hạ ở lần dựng lại sau | ads | như trên |
| G-03 | Ba cách dùng. Mở màn chọn ảnh, camera, hộp thoại quyền: bật ngay trước khi mở, nhả ở dòng đầu của callback, có lưới an toàn khi composable rời màn. Màn chiếm toàn màn hình (native sau inter, trang giới thiệu có native toàn màn): chặn theo trang đang hiện. Rời app có chủ đích (chia sẻ, mở link, mở Play): đánh dấu sau khi mở thành công, nhả khi Activity chính hiện lại | feature, ads | `M/compose/ForegroundAdSuppressor.kt`, `R.md:350-381` |
| G-04 | **Hợp đồng của vỏ app:** `MainActivity` nhả dấu "rời app" ở mỗi lần hiện lại; giao diện gốc đọc trạng thái chặn **trong composition** và báo xuống `:core:ads`. Thiếu bước này thì sau một form UMP hay một lần bấm quảng cáo, quảng cáo khi quay lại app tắt tới hết process | app | `R.md:350-374` |
| G-05 | Rời app mà lệnh mở hụt (app không xuống nền): dấu tự nhả sau đúng một lượt | ads | `T/compose/LeavesAppReleaseSignalTest.kt` |

## 7. Vị trí

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| PL-01 | Feature luôn truyền vị trí khai báo trong `AdPlacement`; không có vị trí rỗng | feature | `R.md:160` |
| PL-02 | Vị trí nội bộ của `:core:ads`: `preload` (lượt nạp chưa gắn màn), `pool` (lượt nạp của pool), `resume` | ads | `M/analytics/AdTelemetry.kt:181-190` |
| PL-03 | Vị trí gắn với lượt **hiện**, ghi đè ở mọi lượt hiện. Giữ giá trị cũ thì doanh thu của quảng cáo sau bị tính cho màn trước | ads | `M/manager/InterstitialAdManagerImpl.kt:70-76`, `T/manager/InterstitialTelemetryTest.kt:233` |
| PL-04 | Cổng và lượt hiện của cùng một thao tác ra cùng một vị trí. Hàm đổi nguồn ra vị trí ném lỗi thì coi như không có vị trí, luồng quảng cáo chạy tiếp | ads | `M/AdsKit.kt:1251-1256`, commit `435c390` |

## 8. Ghi số liệu

### 8.1 Doanh thu theo lượt hiện

| # | Luật | Chủ | AdsKit |
| --- | --- | --- | --- |
| T-01 | Mọi định dạng nghe sự kiện doanh thu (paid event) của SDK, gắn lúc nạp. Mỗi lượt ghi: định dạng, lớp adapter, mã đơn vị, tiền tệ, giá trị, mạng đã trả quảng cáo, vị trí (đọc lúc sự kiện về) | ads, analytics | `M/manager/InterstitialAdManagerImpl.kt:146-158`, `M/compose/BannerAdFixed.kt:105` |
| T-02 | Native có dự phòng: sau khi native hiện 2 giây mà chưa có sự kiện doanh thu thì vẫn ghi một lượt với `USD`, giá trị 0. Sự kiện doanh thu về trước thì hủy dự phòng. Mỗi quảng cáo chỉ ghi một lần. Banner không có dự phòng | ads | `M/manager/NativeAdManagerImpl.kt:221-245,316`, `M/manager/NativeAdPoolManager.kt:249-273` |
| T-03 | Giá trị bằng micro chia 1 000 000. Không biết mạng thì ghi `admob` | ads | `M/analytics/AdRevenueMapping.kt:4,17-18` |
| T-04 | Mã định dạng: `INTER`, `REWARDED`, `APP_OPEN`, `BANNER`, `NATIVE_FULL`, `NATIVE_COLLAP`, `NATIVE_SMALL`, `NATIVE_ONBOARDING`, `NATIVE_CONTENT`, `NATIVE_MEDIUM` | ads | `M/analytics/AdAnalyticsTracker.kt:4-15` |

### 8.2 Firebase

AdMob đã liên kết Firebase thì SDK tự ghi `ad_impression`. Ta ghi doanh thu theo vị trí bằng tên riêng **`ad_impression_placement`** để không đếm đôi (`M/analytics/FirebaseAdEventConfig.kt:23-33`, `R.md:392`).

| Sự kiện | Tham số | Khi nào gửi | AdsKit |
| --- | --- | --- | --- |
| `ad_impression_placement` | `ad_format`, `ad_platform` (`admob`), `ad_source` (tên mạng), `ad_unit_name` (mã đơn vị), `currency`, `value` (số thực), `ad_placement` | Mọi lượt hiện, luôn gửi, không phụ thuộc công tắc số liệu vòng đời hay trạng thái đồng ý (Analytics tự đọc chuỗi đồng ý TCF, ADR-0013) | `M/analytics/FirebaseAdTracker.kt:73-95` |
| `ad_load_success`, `ad_load_fail` | `ad_format`, `ad_placement`, `load_attempt`, `latency_ms`; khi lỗi thêm `load_error_code` (chuỗi), `load_state` | Nhóm của định dạng bật **và** cổng đồng ý mở | `M/analytics/FirebaseAdTracker.kt:97-129` |
| `ad_opportunity` | `ad_format`, `ad_placement`, `opportunity_result` (`shown`, `not_ready`, `throttled`, `busy`, `suppressed`), `load_state` (`ready`, `loading`, `idle`, `no_fill`, `network`, `sdk_throttled`, `invalid`, `internal`, `unknown`) | Như trên | `M/analytics/FirebaseAdTracker.kt:130-135`, `M/analytics/AdTelemetry.kt:74-100` |
| `ad_show_fail` | `ad_format`, `ad_placement`, `show_error_code`, `show_fail_reason` (`internal`, `ad_reused`, `not_ready`, `app_not_foreground`, `mediation_show_error`, `unknown`) | Như trên | `M/analytics/FirebaseAdTracker.kt:136-141` |
| `ad_clicked` | `ad_format`, `ad_placement` | Như trên | `M/analytics/FirebaseAdTracker.kt:142-145` |
| `ad_reward_earned` | `ad_placement` | Nhóm toàn màn hình | `M/analytics/FirebaseAdTracker.kt:146` |
| `reward_dialog` | `ad_placement`, `dialog_action` (`show`, `dismiss`) | Nhóm toàn màn hình | `M/analytics/FirebaseAdTracker.kt:147-150` |

Luật đi kèm:

- Nhóm **toàn màn hình**: `INTER`, `REWARDED`, `APP_OPEN`, `NATIVE_FULL`. Các định dạng khác thuộc nhóm **khe**. Mỗi nhóm có một khóa bật tắt số liệu vòng đời, mặc định tắt (mục 10) (`M/analytics/AdTelemetry.kt:165-168`, `M/analytics/FirebaseAdEventConfig.kt:13-21`).
- `load_attempt` chỉ có ở khe có bộ đếm lỗi liên tiếp (inter, app open, rewarded, native một quảng cáo, thu gọn, banner): thành công sau 2 lỗi thì là 3. Pool và lượt làm mới native nhỏ không có (`R.md:284`).
- `load_state` sau lỗi: offline thì luôn là `network`; mã 3 và 9 là `no_fill`; mã 1 là `sdk_throttled` nếu SDK báo tự chặn, không thì `invalid`; mã 8, 10, 11 là `invalid`; mã 0 là `internal`. Nhãn tính sau khi đã hẹn thử lại; trạng thái nạp chụp **trước** mọi nhánh làm đổi trạng thái (`M/analytics/AdTelemetry.kt:114-121`, commit `340f9bd`, `570769f`).
- **Khi nào ghi `ad_opportunity`:** cổng inter chặn một vị trí (xét `busy`, rồi `not_ready`, rồi `throttled`; không ghi khi cổng đồng ý đóng); mỗi lượt gọi hiện đúng một kết quả; resume bị chặn (vị trí `resume`; lần đầu của process và resume tắt **không** phải cơ hội); ô pool và native sau inter theo P-07, K-07 (`M/AdsKit.kt:1263-1271`, `M/controller/AppOpenAdPolicy.kt:39-55`, commit `3e934be`).
- Không gửi thông báo lỗi của SDK lên số liệu (có thể chứa mã đơn vị). Bộ ghi số liệu ném lỗi thì nuốt, không làm hỏng luồng quảng cáo (`M/analytics/FirebaseAdTracker.kt:112-115`, `M/analytics/AdTelemetry.kt:198-205`).

### 8.3 AppsFlyer (ADR-0027)

| Sự kiện | Tham số | Mặc định | AdsKit |
| --- | --- | --- | --- |
| `af_ad_revenue` | `monetization_network`, `mediation_network` (`admob`), `af_currency`, `af_revenue` (số thực), `country` (theo SIM, rồi mạng, rồi ngôn ngữ máy, viết hoa), `ad_placement`, `ad_unit`, `ad_type` | Bật. Gửi cho **mọi** lượt hiện, kể cả lượt dự phòng giá trị 0 | `M/analytics/AppsFlyerAdTracker.kt:128-160` |
| `af_revenue` | | **Không bao giờ gửi**: gửi cùng `af_ad_revenue` thì AppsFlyer cộng doanh thu hai lần | `M/analytics/AppsFlyerEventConfig.kt:20-24`, commit `aeaf575` |
| `af_inters_ad_eligible` | Không có | Bật. Gửi khi qua cổng, ngay trước khi hiện | `M/AdsKit.kt:1230` |
| `af_inters_api_called` | `ad_format` (`INTER`), `ad_unit_id`, `ad_source` | Bật. Gửi khi nạp thành công | `M/manager/InterstitialAdManagerImpl.kt:142` |
| `af_inters_displayed` | Như trên | Bật. Gửi khi quảng cáo hiện | `M/analytics/AppsFlyerEventConfig.kt:50-53` |
| `af_rewarded_ad_eligible`, `af_rewarded_api_called`, `af_rewarded_ad_displayed` | Như trên, `REWARDED` | Bật. Eligible gửi khi người dùng bấm xem | `M/analytics/AppsFlyerEventConfig.kt:71-90` |
| `af_native_ad_eligible`, `af_native_api_called`, `af_native_ad_displayed` | `ad_format` theo biến thể native | Bật. Eligible gửi khi native vào màn; displayed khi native ghi lượt hiển thị | `M/analytics/AppsFlyerEventConfig.kt:92-111` |
| `af_inters_displayed_{N}_times`, N từ 1 tới 20 | Không có | Bật. Gửi khi inter thứ N từ lúc cài hiện (N-05) | `M/analytics/AppsFlyerAdTracker.kt:117-121`, `M/analytics/AdRevenueMapping.kt:10-11,27` |

Luật đi kèm: gửi bằng hàm ghi sự kiện thường của AppsFlyer, không dùng hàm ghi doanh thu quảng cáo riêng, vì công cụ kiểm SDK của đối tác chỉ nhận sự kiện trong app (commit `0088661`); số liệu vòng đời của Firebase không chuyển sang AppsFlyer (`T/analytics/AppsFlyerAdTrackerTest.kt:301`); vị trí trong doanh thu AppsFlyer không phụ thuộc công tắc của Firebase (`R.md:395`).

## 9. Mediation và khe quảng cáo

| # | Luật | AdsKit |
| --- | --- | --- |
| M-01 | AdMob điều phối. Chín mạng con qua adapter chính thức của AdMob: AppLovin, Meta Audience Network, DT Exchange (Fyber), InMobi, ironSource, Mintegral, Moloco, Pangle, Unity Ads (kèm SDK Unity Ads) | `build.gradle.kts:41-50`, `DG:75` |
| M-02 | Phiên bản adapter: AdsKit không ghi; ta chọn bản mới nhất ổn định lúc làm plan, ghi trong `gradle/libs.versions.toml` | `build.gradle.kts:41-50` |
| M-03 | Khóa SDK AppLovin là cấu hình của từng app, đưa vào lúc build như mã quảng cáo (ADR-0013); không nằm trong resource của module hay trong repo. AdsKit để khóa trong resource của thư viện: không làm theo | `src/main/res/values/strings.xml:9` (không chép giá trị) |
| M-04 | UMP khai báo riêng, không đi kèm `play-services-ads` | `build.gradle.kts:34-38` |
| M-05 | Khe quảng cáo (một mã đơn vị mỗi khe): `app_open`, `interstitial`, `rewarded`, `banner`, `banner_partner`, `native_bottom`, `native_collap`, `native_full`, `native_onboarding`, `native_content`, `native_medium`. Vị trí của feature dùng khe theo định dạng; mã theo khe hay theo vị trí là câu hỏi mở của spec ads | `M/AdUnitIds.kt:3-25` |
| M-06 | Mã thử của Google cho staging theo định dạng (inter, banner, rewarded, app open, native) | `DG:127-133` |
| M-07 | Quy tắc ProGuard của từng adapter theo tài liệu của mạng đó; AdsKit để file rỗng dù tài liệu ghi có, nên ta kiểm bằng build release của staging | `consumer-rules.pro`, `DG:189-208` |

## 10. Khóa Remote Config

Tên khóa giống hệt AdsKit cho khái niệm chung, kể cả lỗi chính tả `show_resum_ads`. Khóa mới đặt theo cùng kiểu (ADR-0026 thay mục 1 của ADR-0023). Mọi khóa vẫn khai báo một chỗ, có kiểu, mặc định an toàn và mô tả, và vào bảng khóa sinh tự động (ADR-0023, các mục còn lại).

| Khóa | Kiểu | Mặc định | Nghĩa | Ai đọc | AdsKit |
| --- | --- | --- | --- | --- | --- |
| `show_open_ads` | boolean | false | App open lúc mở app, từ lần mở thứ hai | app | `M/AdFeatureFlags.kt:4`, `DG:171` |
| `show_open_ads_first_open` | boolean | false | App open ở lần mở đầu sau khi cài | app | `M/AdFeatureFlags.kt:5`, `DG:172` |
| `open_ad_type` | string | `aoa` | `aoa` hay `inter` lúc mở app | app | `M/AdFeatureFlags.kt:6`, `DG:173` |
| `show_resum_ads` | boolean | false | Quảng cáo khi quay lại app | ads | `M/AdFeatureFlags.kt:7`, `DG:174` |
| `resume_ad_type` | string | `aoa` | Chỉ đúng `inter` mới là interstitial; giá trị khác là app open | ads | `M/AdFeatureFlags.kt:8`, `DG:175` |
| `ads_interval` | long (giây) | 45 | Khoảng tối thiểu từ lúc inter gần nhất rời màn tới inter sau | ads | `M/AdFeatureFlags.kt:10`, `DG:176` |
| `is_show_native_after_inter` | boolean | false | Combo inter rồi native toàn màn hình | ads | `M/AdFeatureFlags.kt:11`, `DG:177` |
| `is_show_native_bottom` | boolean | false | Thanh đáy: native nhỏ và lớp thu gọn (true) hay chỉ banner (false) | ads | `M/AdFeatureFlags.kt:12`, `DG:178` |
| `time_expand_collap_ad` | long (giây) | 30 | Bung lại native thu gọn sau khi người dùng thu gọn; 0 là không bung lại. AdsKit chưa có tên khóa chính thức, tên suy ra từ trường `timeExpandCollapAd` | ads | `M/AdFeatureFlags.kt:16-23` |
| `is_show_rw_get_item` | boolean | false | Rewarded ở hành động mở khóa hay xuất | feature | `M/AdFeatureFlags.kt:24`, `DG:180` |
| `is_show_native_onboarding` | boolean | false | Native trên luồng giới thiệu | app | `M/AdFeatureFlags.kt:25`, `DG:181` |
| `is_show_<vị trí>` | boolean | false | **Khóa mới**, thay `ads_<vị trí>_enabled`: bật một vị trí của feature (ví dụ `is_show_inter_save`). Không áp cho vị trí đã có khóa AdsKit ở trên | ads | Kiểu đặt tên theo `DG:177-181` |
| `ads_interval_<vị trí>` | long (giây) | theo spec | **Khóa mới**, chỉ khi spec cần: vị trí có bộ đếm riêng, gọi với `recordInterval=false` (I-04) | ads | `M/AdsKit.kt:124` |
| `is_enable_consent_shortcut` | boolean | true | **Khóa mới**: cho phép quyền tạm `CACHED_EARLY` và `ERROR_ALLOWED` (S-03, S-04). Tắt thì thu hồi ngay `ERROR_ALLOWED` (S-12). AdsKit đọc công tắc này từ Remote Config, tên do app đặt | ads | `M/AdsKit.kt:876-879` |
| `is_log_ad_fullscreen_events` | boolean | false | **Khóa mới**: số liệu vòng đời Firebase của nhóm toàn màn hình (mục 8.2) | ads | `M/analytics/FirebaseAdEventConfig.kt:13-21` |
| `is_log_ad_slot_events` | boolean | false | **Khóa mới**: số liệu vòng đời Firebase của nhóm khe | ads | như trên |

Khóa của AdsKit **không** khai báo, vì PARTNER không dùng: `resume_ads_interval` (PARTNER không có cổng thời gian ở resume, A-11), `time_reload_collap_ad` và `time_reload_bottom_ad` (PARTNER làm mới native cố định 60 giây, CL-02). Khóa của ta bị bỏ hay đổi: xem ADR-0026 và câu hỏi mở của spec ads (công tắc tổng, cá nhân hóa ở vùng có luật).

## 11. Hằng số cứng (không đổi từ xa)

| Hằng số | Giá trị | AdsKit |
| --- | --- | --- |
| Chờ đồng ý, SDK, app open ở màn loading; nhịp kiểm | 10 giây, 35 giây, 40 giây; 250 ms | `M/behavior/PartnerAdsConstants.kt:16-25` |
| Hạn dùng app open; khóa sau khi đóng | 1 giờ; 1 giây | `M/behavior/PartnerAdsConstants.kt:30-33` |
| Nạp lại inter, rewarded sau khi đóng | 2 giây | `M/behavior/PartnerAdsConstants.kt:38` |
| Thử lại toàn màn hình, banner | `min(n x 2 giây, 10 giây)`, không giới hạn | `M/behavior/PartnerAdsConstants.kt:42-43` |
| Thử lại native | 2 giây nhân đôi tới 60 giây (n tối đa 10); 5 giây khi chưa có quảng cáo | `M/behavior/PartnerAdsConstants.kt:50-56` |
| Hạn dùng native | 55 phút | `M/behavior/PartnerAdsConstants.kt:48` |
| Làm mới native tại chỗ (đáy, thu gọn) | 60 giây; native đáy trễ 500 ms sau lớp thu gọn | `M/behavior/PartnerAdsConstants.kt:59`, `M/compose/NativeAdSmall.kt:39` |
| Chờ lần nạp đầu và lần làm mới của native thu gọn | 10 giây, kiểm mỗi 200 ms | `M/compose/NativeAdCollap.kt:77,147` |
| Đếm ngược native sau inter; chờ inter rời màn | 3 giây; 2 giây | `M/compose/NativeAfterInterScreen.kt:34,42` |
| Nút đóng native sau inter; nút thu gọn | 36 dp, X 20 dp, cách mép 12 dp; 32 dp, biểu tượng 20 dp, cách mép 4 dp | `M/compose/NativeAfterInterScreen.kt:183-193`, `M/compose/NativeAdCollap.kt:242-244` |
| Chiều cao native thu gọn, giới thiệu, khe đáy | 250 dp, 250 dp, 56 dp | `M/compose/NativeAdCollap.kt:221`, `M/compose/NativeAdOnboarding.kt:70`, `M/compose/BottomAdSlot.kt:48` |
| Cỡ pool ô lưới, giới thiệu, medium | 5, 3, 3 | `M/manager/NativeAdPoolManager.kt:425`, `M/AdsKit.kt:457,482` |
| Nhịp thử lấy của ô pool | 500 ms; 2 giây khi cạn | `M/compose/PooledNativeAd.kt:26-29` |
| Dự phòng lượt hiển thị native | 2 giây, USD 0 | `M/manager/NativeAdManagerImpl.kt:316` |
| Nhóm nạp sẵn: trần, nhịp kiểm | 6 giây, 200 ms | `M/AdsKit.kt:91,98` |
| Gộp đổi mạng | 3 giây | `M/net/AdsNetworkMonitor.kt:91` |
| Trần cờ "đang hiện" | 10 phút | `M/manager/FullscreenShowState.kt:64` |
| Số lần đếm `af_inters_displayed_N_times` | 1 tới 20 | `M/behavior/PartnerAdsConstants.kt:64` |

## 12. Lỗi AdsKit đã gặp: bản của ta phải tránh

Lỗi AdsKit đã sửa (mỗi dòng nên có test tương ứng trong plan):

| # | Lỗi | Bản của ta làm | AdsKit |
| --- | --- | --- | --- |
| L-A01 | Mốc ghi ở hàm hoàn tất chung sau một cờ chặn; lần hết giờ hạ cờ trước nên mốc không bao giờ được ghi | Ghi mốc ở callback thật của SDK (I-05) | `f2dab3d` |
| L-A02 | Hai quảng cáo toàn màn hình chồng nhau, callback của cái đầu bị nuốt | Một cổng chung, bỏ lượt (F-01) | `e3c1551` |
| L-A03 | Callback đóng thất lạc làm cờ kẹt tới hết process; hồi phục hiểu nhầm lần hiện lại ngay sau khi hiện quảng cáo | Trần 10 phút và hồi phục cần "che trước rồi hiện lại" (F-02, F-03) | `0bfd6be`, `47ad489` |
| L-A04 | Quảng cáo resume bung sau vài giây vì cụm inter và native chưa ghi mốc | Mốc ở cuối cụm; ghi mốc khi hạ cờ kẹt (I-02) | `47ad489` |
| L-A05 | Đếm cơ hội resume khi resume đang tắt | Resume tắt không phải cơ hội (mục 8.2) | `3e934be` |
| L-A06 | Dùng chung một quảng cáo native cho nhiều chỗ, mất lượt hiển thị và doanh thu | Mỗi lượt hiện một quảng cáo riêng (O-01) | `51897f2` |
| L-A07 | Vòng làm mới native nhỏ mất bộ ghi doanh thu | Luôn gắn bộ ghi tổng hợp | `51897f2` |
| L-A08 | Pager giới thiệu hiện hai trang kề nhau cùng một quảng cáo | Dùng pool (OB-02) | `bb15e59` |
| L-A09 | Pool không có quảng cáo thì khung chờ hiện mãi | Có trạng thái cạn, ẩn ô, vẫn thử lấy (P-05, P-06) | `a4b33ca` |
| L-A10 | Đồng hồ native sau inter chờ cờ inter không có trần, nút đóng không bao giờ hiện | Chờ tối đa 2 giây (K-02) | `4362a78` |
| L-A11 | Màn native sau inter tự chặn bằng cờ đúng sai, tạo lại Activity thì cờ bị hạ | Chặn bằng bộ đếm, nơi gọi sở hữu (G-01, K-05) | `4362a78`, `c7f90b1` |
| L-A12 | Vòng làm mới native thu gọn xóa thao tác thu gọn của người dùng | Đọc trạng thái hiện tại; bung lại có hẹn giờ riêng (CL-03, CL-04) | `70d52db`, `28f6941` |
| L-A13 | Lớp nổi lấp cả màn, đẩy thanh điều hướng ra ngoài, chạm rơi vào quảng cáo | Chỉ lấp chiều rộng (D-03) | `b4781fa` |
| L-A14 | Đồng ý chỉ chặn ở nạp sẵn và hiện; các vòng tự nạp vẫn chạy sau khi rút; banner nạp trước khi người dùng trả lời form | Cổng ở đầu mọi lượt nạp (S-15, B-01) | `0a5c65b` |
| L-A15 | Chốt đồng ý khi form lỗi chưa trả lời; hai form chồng nhau | Chỉ chốt kết quả sạch; gộp lượt hỏi (S-06, S-07) | `34e2fc8` |
| L-A16 | Không thử lại, hoặc thử lại cố định 30 giây; bật chế độ máy bay lúc mở app thì không bao giờ nạp lại | Thử lại theo luật; nạp lại khi mạng về (F-05, S-24) | `8022d95` |
| L-A17 | Bộ đếm lỗi cộng dồn suốt process, lần thứ 6 bỏ cuộc vĩnh viễn | Bộ đếm về 0 khi thành công (F-05, O-05) | `46b0cdd` |
| L-A18 | 15 yêu cầu gửi cùng lúc, quảng cáo người dùng thấy đầu tiên về muộn nhất | Kế hoạch nạp sẵn theo nhóm, một nguồn thứ tự (S-22) | `dce73a0` |
| L-A19 | Làm mới native vẫn chạy ở nền | Làm mới tại chỗ chỉ ở foreground (CL-02) | `c881b26` |
| L-A20 | Gửi cả `af_ad_revenue` và `af_revenue`, doanh thu nhân đôi; dùng hàm doanh thu riêng thì công cụ kiểm không nhận | Chỉ `af_ad_revenue`, bằng hàm ghi sự kiện thường (mục 8.3) | `aeaf575`, `0088661` |
| L-A21 | Hàm đổi nguồn ra vị trí ném lỗi làm vỡ luồng inter | Bọc lại, lỗi thì coi như không có vị trí (PL-04) | `435c390` |
| L-A22 | Vị trí trong doanh thu banner và native đáy đứng yên ở màn đầu | Đọc vị trí lúc có sự kiện (D-05) | `b4781fa` |
| L-A23 | Ảnh native bị cắt vì đặt cách co giãn sau khi gắn quảng cáo | Đặt trước (O-07) | `f3947af` |
| L-A24 | Chữ native theo theme tối của máy, không đọc được | Màu chữ theo token của nền quảng cáo, không theo theme máy | `9994dbc`, `94dbec5` |

Rủi ro còn trong AdsKit (chưa có test hay commit sửa), bản của ta làm khác:

| # | Rủi ro trong AdsKit | Bản của ta làm | AdsKit |
| --- | --- | --- | --- |
| AR-01 | Mốc `ads_interval` đo bằng giờ hệ thống; người dùng chỉnh giờ lùi thì inter có thể bị chặn rất lâu | Đo bằng đồng hồ đơn điệu của máy (thời gian từ lúc khởi động). Mốc chỉ sống trong process (I-03) nên đồng hồ này đủ dùng; khoảng âm coi như đã qua cổng | `M/controller/InterstitialThrottleDecision.kt:23-25` |
| AR-02 | Khoảng chừa dưới nội dung tính native đáy cao 80 dp, trong khi khe thật cao 56 dp | Một hằng số chiều cao cho cả khe và khoảng chừa; test so hai giá trị (D-04) | `M/compose/BottomAdDecision.kt:31`, `M/compose/BottomAdSlot.kt:48` |
| AR-03 | Banner nạp lại không kiểm foreground | Mọi lượt nạp lại, thử lại của banner chỉ ở foreground (B-05) | `M/compose/BannerAdFixed.kt:142-150` |
| AR-04 | Khóa SDK AppLovin nằm trong resource của thư viện, có trong repo | Khóa là cấu hình của app, đưa vào lúc build, không vào repo; `scripts/secret-scan.py` chặn (M-03) | `src/main/res/values/strings.xml:9` |
| AR-05 | Bấm "Xem" rewarded hai lần gọi SDK hiện hai lần | Lượt thứ hai bị bỏ khi lượt đầu đang mở quảng cáo (R-06) | `M/manager/RewardedAdManagerImpl.kt:233-242` |
| AR-06 | Tài liệu ghi có quy tắc ProGuard nhưng file rỗng | Kiểm bằng build release của staging (M-07) | `consumer-rules.pro` |

## 13. Ghi chú cho người viết plan

- Mọi luật thuần (mục 1 tới 7) tách thành hàm hoặc lớp thuần, có unit test trên JVM với đồng hồ giả, như AdsKit làm.
- Gom mọi cổng (đồng ý, chồng nhau, mốc, chặn) vào một chỗ trong `:core:ads`; feature và vỏ app không gọi SDK.
- Feature chỉ dùng composable và hàm của `:core:ads` với một vị trí trong `AdPlacement` (CLAUDE.md, bước 7).
- Không mang API chỉ để giữ chữ ký cũ của AdsKit; ta không phải thư viện dùng chung giữa nhiều repo.
- Ghi log bằng `AppLogger` (ADR-0019) ở mỗi quyết định cổng, đường đồng ý, kết quả nạp và hiện; không ghi mã đơn vị.
