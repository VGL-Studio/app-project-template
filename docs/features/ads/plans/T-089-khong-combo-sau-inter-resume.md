---
ticket: T-089
title: Inter khi quay lại app không nối native toàn màn hình
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # chạm điểm chặn, không tính điểm
risk_level: high             # low | medium | high
token_estimate: 7.2M         # cỡ S, số gợi ý 7.2M của scripts/token-usage.py estimate (2026-11-06, trung vị 19 task S); không chỉnh
blocking_points: [ads, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-089: INTER KHI QUAY LẠI APP KHÔNG NỐI NATIVE TOÀN MÀN HÌNH

## 1. Tóm tắt

Lượt quảng cáo khi quay lại app loại inter (`resume_inter`) gọi interstitial không kèm combo, nên inter đóng xong người dùng về app ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật. Sửa một dòng trong `ResumeAdTurns.kt` (`combo = true` thành `combo = false`), sửa chú thích của `AdsController`, mô tả khóa `is_show_native_after_inter` trong `AdsSettings.kt` (sinh lại hai file bảng khóa), và tài liệu, script nói resume inter nối combo. Inter lúc mở app đã không nối combo; plan thêm hai test với `AdsController` thật để khóa hành vi đó. Kiểm chứng bằng test JVM ở hai tầng: `ResumeAdTurnsTest` (bản giả của cổng) và `AdsController` thật có quảng cáo giả trong khe (`FakeInterstitialAd`).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-06, yêu cầu `requests/2026-11-06-combo-chi-sau-inter-trong-app.md`):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-14, tiêu chí 1 (đổi 2026-11-06) | Khi người vận hành bật quảng cáo khi quay lại thì mỗi lần app trở lại màn hình có quảng cáo mở app (hoặc interstitial nếu chọn loại "inter"); interstitial khi quay lại app không nối native toàn màn hình, kể cả khi người vận hành bật native sau inter | `resume_ad_type = inter`: inter qua cổng thì hiện; mọi đường kết thúc của lượt (đóng, lỗi hiện, mất quảng cáo, kẹt, Activity bị hủy, quá trần) chạy phần hoàn tất ngay, không qua `NativeFullScreenTurns.afterInterstitial`. Không đổi cổng, cơ hội, nạp hay mốc `ads_interval` của resume |
| KB-13, tiêu chí 4 (thêm 2026-11-06) | Khi interstitial lúc mở app đóng thì không có native toàn màn hình, kể cả khi người vận hành bật native sau inter | Code đã đúng (`showStartupAd` truyền `combo = false`). Plan chỉ thêm test đi qua đường thật: inter qua cổng, hiện, rồi đóng hoặc lỗi hiện |
| KB-16, tiêu chí 1 và 3 (đổi, thêm 2026-11-06) | Chỉ interstitial của một vị trí trong app mới nối native toàn màn hình; inter lúc mở app và khi quay lại app đóng xong thì người dùng đi tiếp ngay | Inter của vị trí trong app (`showInterstitial`, `rememberShowFullScreenAd`) giữ nguyên combo; test có sẵn của `AdsControllerNativeComboTest` giữ nguyên và phải qua. Test mới có đối chứng: cùng controller, cùng khóa bật, inter của vị trí trong app vẫn mở combo |
| KB-16, tiêu chí 8 | Inter đã qua cổng mà không lên được màn (mất quảng cáo, lỗi hiển thị) và native sau inter bật thì vẫn có native | Sau đổi 2026-11-06, tiêu chí này chỉ áp cho inter của vị trí trong app (K-01: inter lúc mở app và của resume "không bao giờ nối combo"). Test mới cho inter lúc mở app và inter của resume hiện lỗi: không có native |

Luật: `docs/ops/ads-business-rules.md` K-01, A-02, A-12 (đã sửa 2026-11-06, plan không sửa file này), mục 12 (đoạn sau AS-01: lệch có chủ đích khỏi PARTNER).

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); không đổi giao diện, không đổi ảnh chuẩn.

ADR liên quan: ADR-0045 (nghiệp vụ quảng cáo theo AdsKit PARTNER, mục 1.2: người phụ trách sản phẩm quyết chỗ lệch), ADR-0023 (bảng khóa Remote Config sinh từ code), ADR-0019 (log), ADR-0029 (phần ngoài mục 3.2), ADR-0031 (câu khẳng định cũ).

## 3. Giải pháp

### 3.1. Cách làm

1. **`ResumeAdTurns.kt`.** Trong `onAppActivityResumed`, nhánh `AdFormat.INTERSTITIAL` gọi `port.showInterstitial(activity, placement, combo = false)`. Giữ tham số `combo` của `ResumeAdPort.showInterstitial` (câu hỏi 1). Sửa KDoc của `ResumeAdPort.showInterstitial` (hiện chưa có, thêm một dòng): "The resume interstitial never opens the combo (K-01, A-12)". Phần cài của cổng trong `AdsController` (`showInterstitialTurn(activity, placement, combo, onFinished = {})`) giữ nguyên.
2. **`AdsController.kt`, chỉ chú thích.**
   - KDoc của lớp, đoạn combo (dòng 102 tới 106): "The startup interstitial never opens the combo" thành "Only the interstitial of an in-app placement opens it: the startup interstitial and the resume interstitial never do (K-01)".
   - KDoc của lớp, đoạn resume (dòng 116): "or the interstitial of `resume_inter` with the combo, which skips the `ads_interval` gate" thành "or the interstitial of `resume_inter` without the combo (A-12), which skips the `ads_interval` gate".
   - Chú thích trong `showStartupAd` (dòng 698): "The startup interstitial never opens the combo (question 5 of plan T-026)." thành "The startup interstitial never opens the combo (K-01, A-02)."
   - KDoc của `showInterstitialTurn` (dòng 931): "(never for the startup ad)" thành "(never for the startup or resume ad)".
   Không đổi dòng code nào của `AdsController.kt`.
3. **`AdsSettings.kt`.** Mô tả của `IS_SHOW_NATIVE_AFTER_INTER` thành "Shows a full-screen native ad right after the interstitial of an in-app placement (combo); never after the startup or resume interstitial." (câu hỏi 2). Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`, commit cùng code; chỉ dòng của khóa này đổi (`git diff --stat` hai file, mỗi file một dòng).
4. **`docs/ops/ads-guide.md`.**
   - Bảng định dạng, dòng "Quảng cáo khi quay lại app": bỏ `is_show_native_after_inter` khỏi cột khóa; vế "inter của resume nối combo khi `is_show_native_after_inter` bật lúc inter kết thúc" thành "inter của resume không nối combo, kể cả khi `is_show_native_after_inter` bật".
   - Dòng "Native toàn màn hình sau inter (combo)": "Inter lúc mở app không nối combo" thành "Inter lúc mở app và inter khi quay lại app không nối combo".
   - Phần thử trên staging: "Đặt `resume_ad_type = inter` (Firebase staging): quay lại thì có inter rồi combo." thành "Đặt `resume_ad_type = inter` (Firebase staging): quay lại thì có inter; đóng inter thì về app ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật (log có `Interstitial shown: resume_inter`, không có `Native combo opened`)."
   - Tài liệu quy tắc: không ghi mã ticket hay plan (ADR-0044).
5. **`scripts/ads-plan.py`, chỉ chữ tiếng Việt của bảng.**
   - `APP_WIDE_TEXT["resume_inter"]`: "nối native combo nếu bật" thành "không nối native combo".
   - `APP_WIDE_TEXT["native_combo"]`: "Ngay sau mọi inter của feature đã qua cổng (kể cả khi inter không lên màn)" thêm "; không sau inter lúc mở app hay khi quay lại app".
   - `KNOWN_KEYS["resume_ad_type"]`, cột ghi chú: "inter thì nối native combo khi is_show_native_after_inter bật." thành "Inter khi quay lại app không nối native combo.".
   - `KNOWN_KEYS["is_show_native_after_inter"]`, cột ghi chú: "Áp cho mọi inter của feature và resume_inter." thành "Chỉ áp cho inter của vị trí trong app; inter lúc mở app và resume_inter không nối.".
   - Chạy `scripts/tests/run.sh` (qua `scripts/check.sh`); các test `ads-plan:` không kiểm chữ này.

Mẫu theo: `AdsControllerHostTest` (controller đã `start`, cổng đồng ý mở, Activity dựng sau `start` nên bộ theo dõi Activity thấy nó, `FakeInterstitialAd` đặt vào khe bằng `putLoadedInterstitialForTest`, gọi callback của SDK bằng `fake.callback!!`), `AdsControllerNativeComboTest` (khóa combo bật, `nativeFullScreenView`, log `Native combo ...`).

Mọi đường kết thúc của lượt inter đi qua một chỗ: `beginInterstitialTurn` chọn phần hoàn tất theo `combo` một lần, lúc lượt mở (`finish = if (combo) afterInterstitial else onFinished`). Đóng, lỗi hiện, mất quảng cáo, kẹt, Activity bị hủy, quá trần đều chạy `finish` đó. Nên test hai tầng là đủ cho mọi đường: tầng `ResumeAdTurns` kiểm giá trị `combo` truyền đi; tầng controller kiểm hai đường thật (đóng, lỗi hiện) và `startupTurn_neverOpensCombo` có sẵn kiểm đường Activity bị hủy với `combo = false`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdTurns.kt` | Bước 1: `combo = false` ở nhánh inter; KDoc của `ResumeAdPort.showInterstitial` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 2: chỉ chú thích, KDoc (bốn chỗ) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Bước 3: mô tả của `IS_SHOW_NATIVE_AFTER_INTER` (theo câu hỏi 2; bỏ dòng này và hai dòng sinh dưới nếu người duyệt chọn giữ mô tả) |
| Sửa | `docs/remote-config-keys.md` | Bước 3: sinh lại bằng `RemoteConfigKeysTest`, chỉ dòng `is_show_native_after_inter` |
| Sửa | `docs/remote-config-template.json` | Bước 3: sinh lại cùng lệnh, chỉ `description` của `is_show_native_after_inter` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdTurnsTest.kt` | Hai câu khẳng định cũ viết lại (mục 3.5), đổi tên `interResume_showsInterstitialWithCombo` thành `interResume_showsInterstitialWithoutCombo`, thêm đối chứng (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Hai test mới của resume inter với khóa combo bật (mục 4); thêm import cần cho chúng (`Robolectric`, `ComponentActivity`, `AdError`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerStartupInterComboTest.kt` | Hai test mới của inter lúc mở app với khóa combo bật (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Bước 4: ba chỗ |
| Sửa | `scripts/ads-plan.py` | Bước 5: bốn chuỗi tiếng Việt |
| Sửa | `docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md` | Nhật ký thực hiện |
| Sửa | `docs/features/ads/tickets/T-089-khong-combo-sau-inter-resume.md` | Trạng thái ticket |

Nơi đọc file bị đổi: `docs/remote-config-keys.md` được `scripts/ads-plan.py` (đọc khóa, kiểu, mặc định; không đọc mô tả tiếng Anh vào bảng khi khóa có trong `KNOWN_KEYS`), `scripts/history-refs.py` (bỏ qua, file sinh), `scripts/main-guard-verified.sh` và `scripts/template-sync.sh` (chỉ chép, so) đọc; `docs/remote-config-template.json` do `RemoteConfigKeysTest` so và dev nhập vào Firebase. `app/build.gradle.kts` chỉ truyền đường dẫn hai file vào test và khai báo chúng là đầu vào của task test; `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` sinh và so hai file với khai báo khóa trong code, không chứa chữ mô tả nào. Không file nào trong số đó cần sửa: chúng không phụ thuộc chữ mô tả. `scripts/ads-plan.py` không có test nào kiểm bốn chuỗi plan đổi (`scripts/tests/run.sh`, khối `ads-plan:`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Bớt một quảng cáo toàn màn hình (native `native_combo`) sau inter của vị trí toàn app `resume_inter` khi `is_show_native_after_inter` bật, theo spec `ads`, KB-14, KB-16 (đổi 2026-11-06). Inter của vị trí trong app giữ combo. Không đổi khóa Remote Config nào (chỉ chữ mô tả).
- Sự kiện phân tích: Không chạm. Lượt resume inter vẫn ghi cơ hội, `shown`, hiển thị như cũ; sau nó không còn cơ hội nào của `native_combo` (vì không có lượt combo), đúng như luật.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng `com.google.android.gms.ads.AdError` của `play-services-ads` đã có trên classpath test của `:core:ads` (`FakeFullScreenSdkAds.kt` đã dùng các lớp cùng gói).

### 3.4. Phương án dự phòng

Không có: một dòng code và chú thích; đường test đã có mẫu chạy được (`AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck`, `startup_destroyedHost_appOpen_showFail_finishesFalse`).

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md` (2026-11-06), kết quả tóm ở mục 4.1. Đã đọc toàn bộ `ResumeAdTurnsTest`, `AdsControllerResumeTest`, `AdsControllerNativeComboTest`, và các test của `AdsControllerHostTest` gọi `showStartupAd` hay resume. Script tìm 303 test dùng tên của ba file code; "Cần xem trước" 8 test (`AdGalleryScreenTest.collapsible*`, `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`, `RewardedActionTest.*`) chỉ trùng tên chung như `start`, `AdsController`, `AdsSettings`, không đo hay kiểm combo, resume hay inter lúc mở app: giữ. "Nên xem" 133 test: đã lọc theo combo, resume, startup, inter; các test liên quan nằm trong bảng dưới, còn lại chỉ trùng tên chung: giữ.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lượt resume loại inter gọi `showInterstitial(..., combo = true)` | Bước 1: gọi với `combo = false` | `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)` | Viết lại (câu A dưới) |
| Như trên, ở lượt sau khi loại resume đổi sang inter | Bước 1 | `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing`, câu đối chứng cuối: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)` | Viết lại (câu B dưới) |
| Inter lúc mở app không nối combo | Không đổi | `AdsControllerNativeComboTest.startupTurn_neverOpensCombo`, `showStartupAd_blocked_neverOpensCombo` | Giữ |
| Inter của vị trí trong app nối combo ở mọi đường kết thúc | Không đổi | `AdsControllerNativeComboTest.closed_opensCombo`, `showFailed_opensCombo`, `adLost_opensCombo`, `stuckRecovered_opensCombo`, `hostDestroyed_opensCombo`, `capExpired_reachesCombo_busyFinishesAtOnce`, `onFinished_runsOnMainThread`, `AdsControllerHostTest.interstitial_hostGone_comboOn_noNativeFullScreen` | Giữ |
| Cổng, cơ hội, nạp, lần đầu, chặn, đổi loại của resume | Không đổi | Các test còn lại của `ResumeAdTurnsTest`, `ResumeAdRulesTest`, `AdsControllerResumeTest`, `AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck` | Giữ |
| Lượt inter của vị trí trong app: số liệu phễu, cửa sổ quảng cáo trong màn, Activity giữ lượt khi xoay | Không đổi | `AdsControllerFunnelTest.combo_recordsNativeEligibleAfterInter`, `comboOff_recordsInterEligibleOnly`, `interTurn_recordsEligibleOnce`, `AdsControllerInScreenWindowTest.comboTurn_noOpenWindowAcrossInterClose`, `AdsControllerHostTest.handover_*`, `rotation_*`, `interstitial_*Host_notShown_keptInSlot*` (nhóm "Nên xem") | Giữ: đều gọi `beginInterstitialTurn` hay `showInterstitial` của vị trí trong app, không qua `ResumeAdTurns` hay `showStartupAd` |
| Mô tả khóa `is_show_native_after_inter` | Bước 3 | `RemoteConfigKeysTest` (`:app`) so hai file sinh với code | Giữ câu khẳng định; dữ liệu là hai file sinh lại trong cùng commit code |

**Câu A**, `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`, đổi tên thành `interResume_showsInterstitialWithoutCombo`. Đủ sáu điều của ADR-0031, quyết định 2:

1. Câu plan nói điều mới: mục 3.1 bước 1, "nhánh `AdFormat.INTERSTITIAL` gọi `port.showInterstitial(activity, placement, combo = false)`"; spec KB-14 tiêu chí 1 (đổi 2026-11-06), luật A-12.
2. Ý định: lượt resume loại inter hiện đúng một interstitial của `resume_inter`, trên Activity vừa resume, không hiện app open, và mang đúng lựa chọn combo của luật (A-12, A-15). Câu mới: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; giữ `assertTrue(port.appOpenShows.isEmpty())`.
3. Không nới: vẫn là phép bằng trên cả bộ ba (Activity, vị trí, combo), không bớt câu nào.
4. Đối chứng: trên code hiện tại câu mới đỏ (`expected:<[(..., RESUME_INTER, false)]> but was:<[(..., RESUME_INTER, true)]>`); sau bước 1, tạm đổi lại `combo = true` bằng lệnh đột biến ở mục 4 thì đỏ lại.
5. Là câu của test JVM, không phải ảnh chuẩn; câu cũ kiểm luật A-12 cũ, nay luật đã đổi (2026-11-06) và plan thực hiện đúng phần đổi đó. Câu mới là test viết trước của plan này.
6. Tính chung với câu B: 2 câu, dưới giới hạn 3.

**Câu B**, `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing`, câu đối chứng cuối:

1. Câu plan nói điều mới: như câu A.
2. Ý định: lượt bị bỏ vì loại đổi giữa lúc quyết và lúc hiện không hiện gì, và lượt sau, quyết với loại mới, hiện đúng một interstitial của `resume_inter` (L-A05 mở rộng cho đổi loại). Câu mới: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; giữ `assertTrue(port.appOpenShows.isEmpty())` và hai câu đầu của test.
3. Không nới: như câu A.
4. Đối chứng: như câu A (cùng một dòng code quyết).
5. Như câu A.
6. Như câu A.

## 4. Kiểm thử

Test JVM của `:core:ads`, Robolectric như các lớp có sẵn. Không có giao diện, không có ảnh chuẩn.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-14 tiêu chí 1, A-12: lượt resume loại inter quyết không combo (tầng `ResumeAdTurns`) | Unit test | `ResumeAdTurnsTest.interResume_showsInterstitialWithoutCombo` (câu A, mục 3.5). Thêm đối chứng trong cùng test, sau câu A: `returnToApp()`, `turns.onAppActivityResumed(activity)`: `port.interShows` có đúng hai phần tử, cả hai `combo` là `false` (`assertEquals(listOf(false, false), port.interShows.map { it.third })`), để chắc lượt sau cũng không combo |
| KB-14, L-A05 mở rộng: lượt sau khi đổi loại cũng không combo | Unit test | `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing` (câu B, mục 3.5) |
| KB-14 tiêu chí 1, KB-16 tiêu chí 3: inter của resume đóng xong, khóa combo bật, không có native (đường thật của controller) | Unit test | `AdsControllerResumeTest.resumeInter_closed_comboOn_noNativeFullScreen`. Dựng: `settings` bật `IS_LOG_AD_FULLSCREEN_EVENTS`, `SHOW_RESUM_ADS`, `IS_SHOW_NATIVE_AFTER_INTER`, `RESUME_AD_TYPE = "inter"`, `placementEnabled(GALLERY_INTERSTITIAL)`; `startedController()`, `openConsentGateForTest()`, `idle()`, `idleFor(45_100)` (lượt mở app hết); `FakeInterstitialAd` đặt vào khe `RESUME_INTER` bằng `putLoadedInterstitialForTest`; `host = Robolectric.buildActivity(ComponentActivity::class.java).setup()`; `host.pause().stop()`, `host.start().resume()`, `idle()`. Kiểm: `fake.shows == 1`, `fake.shownOn.single() === host.get()`, log có `"Interstitial shown: resume_inter"`. `fake.callback!!.onAdDismissedFullScreenContent()`; ngay sau đó (không chờ): `assertFalse(controller.fullScreenAdShowing.value)` (combo giữ cờ nếu có). `idleFor(2_100)`: `assertNull(controller.nativeFullScreenView.value)`; `assertTrue(logger.entries.none { it.message.startsWith("Native combo") })`. `idleFor(5_000)`: `nativeFullScreenView` vẫn `null`. Đối chứng trong cùng test (khóa bật thật, combo vẫn chạy cho inter của vị trí trong app): `idleFor(45_100)` (mốc `ads_interval` do `resume_inter` đẩy), `FakeInterstitialAd` thứ hai vào khe `GALLERY_INTERSTITIAL`, `CoroutineScope(Dispatchers.Main).launch { controller.showInterstitial(host.get(), AdPlacement.GALLERY_INTERSTITIAL) {} }`, `idle()`, `gallery.shows == 1`, `gallery.callback!!.onAdDismissedFullScreenContent()`: `assertTrue(controller.fullScreenAdShowing.value)`; `idleFor(2_100)`: `assertNotNull(controller.nativeFullScreenView.value)`, log có `"Native combo opened"` |
| K-01, KB-16 tiêu chí 8 (sau đổi): inter của resume hiện lỗi, khóa combo bật, không có native | Unit test | `AdsControllerResumeTest.resumeInter_showFailed_comboOn_noNativeFullScreen`: dựng như test trên tới `fake.shows == 1`; `fake.callback!!.onAdFailedToShowFullScreenContent(AdError(1, "Show failed.", "com.google.android.gms.ads"))`; ngay sau đó `assertFalse(controller.fullScreenAdShowing.value)`; `idleFor(2_100)`: `nativeFullScreenView` `null`, không có log `Native combo`. Đối chứng: như test trên, với đường lỗi hiện của inter vị trí trong app (`gallery.callback!!.onAdFailedToShowFullScreenContent(...)`: combo mở, K-01) |
| KB-13 tiêu chí 4, A-02: inter lúc mở app đóng xong, khóa combo bật, không có native, người dùng đi tiếp ngay | Unit test (khóa hành vi đã có) | `AdsControllerStartupInterComboTest.startupInter_closed_comboOn_noNativeFullScreen`. Dựng theo `AdsControllerHostTest`: controller có `application`, `AdUnitIds { null }`, `DefaultAdsFlags(settings)`, `RecordingAdMetrics`, `RecordingAppLogger`; `settings` bật `IS_LOG_AD_FULLSCREEN_EVENTS`, `SHOW_OPEN_ADS`, `SHOW_OPEN_ADS_FIRST_OPEN`, `IS_SHOW_NATIVE_AFTER_INTER`, `OPEN_AD_TYPE = "inter"`, `placementEnabled(GALLERY_INTERSTITIAL)`; `start(application, AppActivityFilter { true })`, `idle()`, `openConsentGateForTest()`, `idle()`; `FakeInterstitialAd` vào khe `OPEN_APP_INTER`; `activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()`; `controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { results += it }`, `idle()`. Kiểm: `fake.shows == 1`, `results` rỗng. `fake.callback!!.onAdDismissedFullScreenContent()`: ngay sau đó `assertEquals(listOf(true), results)` và `assertFalse(controller.fullScreenAdShowing.value)`. `idleFor(2_100)`: `nativeFullScreenView` `null`, không có log `Native combo`. `idleFor(5_000)`: `results` vẫn `[true]`. Đối chứng trong cùng test: `idleFor(45_100)`, inter của `GALLERY_INTERSTITIAL` (như đối chứng của resume): combo mở |
| KB-13, K-01 (sau đổi): inter lúc mở app qua cổng mà hiện lỗi, khóa combo bật, không có native | Unit test (khóa hành vi đã có) | `AdsControllerStartupInterComboTest.startupInter_showFailed_comboOn_noNativeFullScreen`: dựng như trên tới `fake.shows == 1`; `fake.callback!!.onAdFailedToShowFullScreenContent(AdError(1, "Show failed.", "com.google.android.gms.ads"))`: ngay sau đó `results == [true]` (inter đã gọi hiện), `fullScreenAdShowing` `false`; `idleFor(2_100)`: `nativeFullScreenView` `null`, không có log `Native combo` |
| KB-16 tiêu chí 1: inter của vị trí trong app vẫn nối combo ở mọi đường | Unit test có sẵn | `AdsControllerNativeComboTest` (mọi test `*_opensCombo`, `capExpired_*`, `onFinished_runsOnMainThread`), `AdsControllerHostTest.interstitial_hostGone_comboOn_noNativeFullScreen` giữ nguyên và phải qua |
| Bảng khóa sinh khớp code | Unit test có sẵn | `RemoteConfigKeysTest` (`:app`) qua sau khi sinh lại |
| Tài liệu và bảng Excel không còn nói resume inter nối combo | Kiểm tra thủ công (người: đọc tài liệu) | Đọc ba chỗ của `docs/ops/ads-guide.md` (mục 3.1 bước 4); chạy `python3 scripts/ads-plan.py --dump` và xem chữ của `resume_inter`, `native_combo`, `resume_ad_type`, `is_show_native_after_inter` |
| KB-14 trên máy: quay lại app với `resume_ad_type = inter` thì có inter, đóng inter về app ngay | Kiểm tra thủ công (máy thật: Remote Config) | Trên bản staging: Firebase staging đặt `resume_ad_type = inter` (khóa `is_show_native_after_inter` bật sẵn ở staging), đồng ý quảng cáo, mở app, bấm Home rồi mở lại: có inter thử của Google; đóng inter thì về màn trước, không có native toàn màn hình. Log `adb logcat -v threadtime \| grep ' VGL\.Ads'`: có `Resume ad SHOW: resume_inter`, `Resume ad shown on resume: resume_inter`, `Interstitial shown: resume_inter`; không có `Native combo opened`. Đối chứng: màn xem thử, nút "Interstitial", đóng inter thì có native toàn màn hình (`Native combo opened`). Dòng log dẫn: `ResumeAdTurns.onProcessForegroundChanged` (`Resume ad $decision: ...`), `onAppActivityResumed` (`Resume ad shown on resume: ...`), `AdsController.showInterstitialTurn` (`Interstitial shown: ...`), `NativeFullScreenTurns.afterInterstitial` (`Native combo opened ...`) |

Thứ tự viết test của phiên (bước 3 của `auto-run.md`):

- `ResumeAdTurnsTest` (câu A, câu B, đối chứng) và hai test của `AdsControllerResumeTest` đỏ trên code hiện tại vì lượt resume đang truyền `combo = true` (combo mở, cờ toàn màn hình còn giữ sau khi inter đóng, có `nativeFullScreenView`). Đó là lý do đúng. Sau bước 1 chúng xanh.
- Hai test của `AdsControllerStartupInterComboTest` khóa hành vi đã có nên qua ngay trên code hiện tại. Để chúng thất bại đúng lý do, trước khi sửa code (trước bước 1 và bước 2, để lệnh trả file không xóa phần sửa), phiên chạy chúng một lần với đột biến tạm, không commit:
  - `perl -pi -e 's/\QshowInterstitialTurn(activity, turn.placement, combo = false, finish)\E/showInterstitialTurn(activity, turn.placement, combo = true, finish)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt`
  - `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` phải thấy 1 dòng đổi (không đổi thì mẫu không khớp, lần thử không có giá trị).
  - `./gradlew :core:ads:testDebugUnitTest --tests '*AdsControllerStartupInterComboTest*'`: cả hai đỏ (kỳ vọng ở câu `assertEquals(listOf(true), results)` hay `assertFalse(controller.fullScreenAdShowing.value)`: combo giữ cờ và hoãn phần hoàn tất).
  - `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt`.
  - Ghi lần chạy vào nhật ký.
- Sau bước 1, đối chứng của câu A và B (điều 4 của ADR-0031): `perl -pi -e 's/\Qport.showInterstitial(activity, placement, combo = false)\E/port.showInterstitial(activity, placement, combo = true)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdTurns.kt`, `git diff --stat -- <file đó>` thấy 1 dòng đổi, chạy `./gradlew :core:ads:testDebugUnitTest --tests '*ResumeAdTurnsTest*' --tests '*AdsControllerResumeTest*'`: bốn test của mục này đỏ; rồi đặt lại `combo = false` bằng lệnh `perl` ngược chiều (không dùng `git checkout`, vì file đã có phần sửa của bước 1), `git diff -- <file>` chỉ còn phần sửa của bước 1. Ghi vào nhật ký và mục "Test cũ viết lại (ADR-0031)" của mô tả PR.

**Ngoại lệ viết test trước:** (1) hai test của `AdsControllerStartupInterComboTest` qua ngay trên code hiện tại vì chúng khóa hành vi đã có (ticket: "giữ nguyên hành vi đó và thêm test khóa nó lại"); bằng chứng thất bại đúng lý do là lần chạy với đột biến tạm ở trên. (2) Chú thích, KDoc (bước 1, 2), mô tả khóa (bước 3), tài liệu (bước 4) và chữ của `scripts/ads-plan.py` (bước 5) là chữ, không có test đỏ trước; `RemoteConfigKeysTest` có sẵn bắt hai file sinh lệch với code.

### 4.1. Chạy thử

Không cần (E dưới 2). Không chạy test tạm: máy soạn plan chưa có JDK (`scripts/local-setup.sh --check` báo thiếu JDK). Mẫu dựng test có sẵn và đang qua trên nhánh chính: `AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck` (resume qua bộ theo dõi Activity với quảng cáo giả trong khe), `startup_destroyedHost_appOpen_showFail_finishesFalse` (`showStartupAd` với quảng cáo giả), `interstitial_hostGone_comboOn_noNativeFullScreen` (khóa combo bật với controller đã `start`).

- `python3 scripts/existing-tests.py docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md` (2026-11-06, nhánh từ `origin/main` tại `ac7335d`): file của plan `ResumeAdTurns.kt`, `AdsController.kt`, `AdsSettings.kt`; 303 test có sẵn dùng tên của chúng; "Cần xem trước" 8, "Nên xem" 133, plan đã nhắc 14; 148 test chỉ dùng tên plan không nêu. Cách xếp ở mục 3.5.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi tần suất, bớt native toàn màn hình sau inter của resume khi combo bật), 7 (module core `:core:ads`). Không chạm 2, 3, 4 (không đổi sự kiện hay tham số), 5, 6, 8, 9. Người phụ trách sản phẩm (anh Nhật) là người yêu cầu và đã xác nhận spec 2026-11-06.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một dòng code, nhưng nhiều phần của `:core:ads` (`ResumeAdTurns.kt`, chú thích `AdsController.kt`, mô tả trong `AdsSettings.kt`), hai file bảng khóa sinh lại, một script và một tài liệu hướng dẫn; không đổi giao diện công khai của lớp nào |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, tiêu chí rõ ở KB-13, KB-14, KB-16; luật K-01, A-12 đã ghi. Còn hai chi tiết AI đề xuất (câu hỏi 1, 2): giữ tham số `combo`, đổi chữ mô tả khóa |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM với controller thật và quảng cáo giả; hành vi với quảng cáo thật và Remote Config thật chỉ kiểm trên bản staging (một bước thủ công) |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `AdsSettings.kt`, `scripts/ads-plan.py`, `docs/ops/ads-guide.md` và hai file bảng khóa cũng nằm trong phạm vi của T-086, T-087, T-088 (chưa có plan); đổi hành vi dùng chung của mọi app (resume), nhưng không feature nào dựa vào combo sau resume |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của test có sẵn: lần đầu đặt `FakeInterstitialAd` vào khe `RESUME_INTER` và `OPEN_APP_INTER` |
| **Tổng** | 7 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `ResumeAdTurns.kt` (lượt resume của mọi app), chú thích của `AdsController.kt`, mô tả khóa trong `AdsSettings.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/ops/ads-guide.md`, `scripts/ads-plan.py`.
- Task khác có thể xung đột: T-086 (`AdsController.kt`, `ads-guide.md`), T-087 (`AdsController.kt`, `AdsSettings.kt`, `ads-plan.py`, `ads-guide.md`, bảng khóa), T-088 (`AdsSettings.kt`, `ads-plan.py`, `ads-guide.md`, bảng khóa), T-090, T-091, T-092 (`ads-guide.md`, `ads-plan.py`); tất cả chưa có plan. Chỗ T-089 sửa trong các file đó là dòng riêng (chú thích, một mô tả khóa, bốn chuỗi), nên chỉ có thể xung đột khi merge, không xung đột ý. Hai file bảng khóa sinh từ code: task chạy sau sinh lại trên nhánh chính mới nhất.
- Đề xuất thứ tự chạy: chạy trước các task ads khác trong cùng phiên (thay đổi nhỏ nhất, ít xung đột merge nhất cho task sau). `run-check.py` sẽ báo cùng sửa file; không cần thứ tự bắt buộc.

## 7. Điều kiện dừng riêng

- Một test có sẵn của `AdsControllerNativeComboTest` hay `AdsControllerHostTest` (combo của inter vị trí trong app) đỏ sau bước 1: dừng, vì plan không đổi combo của vị trí trong app.
- Sinh lại bảng khóa đổi dòng nào khác ngoài `is_show_native_after_inter`: dừng (nhánh chính đã lệch bảng khóa, ngoài phạm vi).
- Một test có sẵn của `ResumeAdTurnsTest`, `ResumeAdRulesTest` hay `AdsControllerResumeTest` ngoài hai câu của mục 3.5 đỏ: dừng, vì plan không đổi cổng hay cơ hội của resume.

## 8. Câu hỏi cho người duyệt

1. **Giữ hay bỏ tham số `combo` của `ResumeAdPort.showInterstitial`?** Sau đổi, lượt resume luôn truyền `false`.
   - Phương án A (AI đề xuất): giữ tham số, truyền `false`. Ưu: một dòng code; `ResumeAdTurnsTest` kiểm được lựa chọn combo ở tầng thuần (câu A, B) và đột biến về `true` bị bắt ở cả hai tầng; nếu sau này cần đổi lại chỉ sửa một chỗ. Nhược: tham số chỉ còn một giá trị.
   - Phương án B: bỏ tham số; phần cài của cổng trong `AdsController` gọi `showInterstitialTurn(..., combo = false, ...)`. Ưu: chữ ký gọn. Nhược: lựa chọn combo chuyển hẳn vào phần nối trong `AdsController`, chỉ test controller bắt được; câu A, B phải đổi kiểu dữ liệu của bản giả (`Pair` thay `Triple`), thêm thay đổi cho `FakeResumeAdPort`.
   - Phương án C (để sau): suy combo từ vị trí (`AdPlacement.appWide` thì không combo) trong `beginInterstitialTurn`, bỏ tham số ở mọi chỗ. Gom luật K-01 về một chỗ, nhưng đổi nhiều test của `AdsControllerNativeComboTest`; ngoài phạm vi ticket cỡ S.
2. **Đổi mô tả khóa `is_show_native_after_inter` trong `AdsSettings.kt` không?** Mô tả hiện tại "Shows a full-screen native ad right after an interstitial (combo)." nay không đúng hết (inter lúc mở app và khi quay lại không nối). Mô tả này vào `docs/remote-config-keys.md` và `docs/remote-config-template.json` (dev nhập vào Firebase).
   - AI đề xuất: đổi thành "Shows a full-screen native ad right after the interstitial of an in-app placement (combo); never after the startup or resume interstitial." và sinh lại hai file trong cùng PR. Khóa, kiểu, mặc định không đổi.
   - Nếu chọn giữ: bỏ ba dòng tương ứng của mục 3.2 (`AdsSettings.kt` và hai file sinh), bỏ bước 3.
3. **Bảng định dạng của `docs/ops/ads-guide.md`, dòng "Quảng cáo khi quay lại app": bỏ `is_show_native_after_inter` khỏi cột khóa?** Spec mục 5 đã gạch khóa này khỏi dòng `resume` (2026-11-06). AI đề xuất bỏ, như spec.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
