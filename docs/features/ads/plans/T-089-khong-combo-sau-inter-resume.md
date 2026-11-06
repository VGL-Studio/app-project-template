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

Lượt quảng cáo khi quay lại app loại inter (`resume_inter`) gọi interstitial không kèm combo. Inter đóng xong thì người dùng về app ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật.

Phần sửa:

- Một dòng code trong `ResumeAdTurns.kt` (`combo = true` thành `combo = false`).
- Chú thích của `AdsController`, mô tả khóa `is_show_native_after_inter` trong `AdsSettings.kt` (sinh lại hai file bảng khóa).
- Tài liệu và script còn nói resume inter nối combo.

Inter lúc mở app đã không nối combo; plan thêm hai test với `AdsController` thật để khóa hành vi đó. Kiểm chứng bằng test JVM ở hai tầng: `ResumeAdTurnsTest` (bản giả của cổng) và `AdsController` thật có quảng cáo giả trong khe (`FakeInterstitialAd`).

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-11-06, yêu cầu `requests/2026-11-06-combo-chi-sau-inter-trong-app.md`):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-14, tiêu chí 1 (đổi 2026-11-06) | Khi người vận hành bật quảng cáo khi quay lại thì mỗi lần app trở lại màn hình có quảng cáo mở app (hoặc interstitial nếu chọn loại "inter"); interstitial khi quay lại app không nối native toàn màn hình, kể cả khi người vận hành bật native sau inter | `resume_ad_type = inter`: inter qua cổng thì hiện; mọi đường kết thúc của lượt (đóng, lỗi hiện, mất quảng cáo, kẹt, Activity bị hủy, quá trần) chạy phần hoàn tất ngay, không qua `NativeFullScreenTurns.afterInterstitial`. Không đổi cổng, cơ hội, nạp hay mốc `ads_interval` của resume |
| KB-13, tiêu chí 4 (thêm 2026-11-06) | Khi interstitial lúc mở app đóng thì không có native toàn màn hình, kể cả khi người vận hành bật native sau inter | Code đã đúng (`showStartupAd` truyền `combo = false`). Plan chỉ thêm test đi qua đường thật: inter qua cổng, hiện, rồi đóng hoặc lỗi hiện |
| KB-16, tiêu chí 1 và 3 (đổi, thêm 2026-11-06) | Chỉ interstitial của một vị trí trong app mới nối native toàn màn hình; inter lúc mở app và khi quay lại app đóng xong thì người dùng đi tiếp ngay | Inter của vị trí trong app (`showInterstitial`, `rememberShowFullScreenAd`) giữ nguyên combo; test có sẵn của `AdsControllerNativeComboTest` giữ nguyên và phải qua. Test mới có đối chứng: cùng controller, cùng khóa bật, inter của vị trí trong app vẫn mở combo |
| KB-16, tiêu chí 8 | Inter đã qua cổng mà không lên được màn (mất quảng cáo, lỗi hiển thị) và native sau inter bật thì vẫn có native | Tiêu chí này chưa có ghi chú đổi 2026-11-06. AI hiểu nó chỉ áp cho inter của vị trí trong app, theo tiêu chí 3 của KB-16 và luật K-01 (inter lúc mở app và của resume "không bao giờ nối combo"). Chưa tự quyết: câu hỏi 4. Test mới cho inter lúc mở app và inter của resume hiện lỗi viết theo cách hiểu này: không có native |

Luật: `docs/ops/ads-business-rules.md` K-01, A-02, A-12 (đã sửa 2026-11-06, plan không sửa file này), mục 12 (đoạn sau AS-01: lệch có chủ đích khỏi PARTNER).

Thiết kế tham chiếu: Không có thiết kế riêng (docs/ops/design-workflow.md); không đổi giao diện, không đổi ảnh chuẩn.

ADR liên quan: ADR-0045 (nghiệp vụ quảng cáo theo AdsKit PARTNER, mục 1.2: người phụ trách sản phẩm quyết chỗ lệch), ADR-0023 (bảng khóa Remote Config sinh từ code), ADR-0019 (log), ADR-0029 (phần ngoài mục 3.2), ADR-0031 (câu khẳng định cũ).

## 3. Giải pháp

### 3.1. Cách làm

1. **`ResumeAdTurns.kt`.**
   - Trong `onAppActivityResumed`, nhánh `AdFormat.INTERSTITIAL` gọi `port.showInterstitial(activity, placement, combo = false)`.
   - Giữ tham số `combo` của `ResumeAdPort.showInterstitial` (câu hỏi 1).
   - Thêm KDoc cho `ResumeAdPort.showInterstitial`: "The resume interstitial never opens the combo (K-01, A-12)".
   - Phần cài của cổng trong `AdsController` (`showInterstitialTurn(activity, placement, combo, onFinished = {})`) giữ nguyên.
2. **`AdsController.kt`, chỉ chú thích.**
   - KDoc của lớp, đoạn combo (dòng 102 tới 106): "The startup interstitial never opens the combo" thành "Only the interstitial of an in-app placement opens it: the startup interstitial and the resume interstitial never do (K-01)".
   - KDoc của lớp, đoạn resume (dòng 116): "or the interstitial of `resume_inter` with the combo, which skips the `ads_interval` gate" thành "or the interstitial of `resume_inter` without the combo (A-12), which skips the `ads_interval` gate".
   - Chú thích trong `showStartupAd` (dòng 698): "The startup interstitial never opens the combo (question 5 of plan T-026)." thành "The startup interstitial never opens the combo (K-01, A-02)."
   - KDoc của `showInterstitialTurn` (dòng 931): "(never for the startup ad)" thành "(never for the startup or resume ad)".
   - Không đổi dòng code nào của `AdsController.kt`.
3. **`AdsSettings.kt`.**
   - Mô tả của `IS_SHOW_NATIVE_AFTER_INTER` thành "Shows a full-screen native ad right after the interstitial of an in-app placement (combo); never after the startup or resume interstitial." (câu hỏi 2).
   - Sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json` bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`, commit cùng code.
   - Chỉ dòng của khóa này đổi (`git diff --stat` hai file, mỗi file một dòng).
4. **`docs/ops/ads-guide.md`.**
   - Bảng định dạng, dòng "Quảng cáo khi quay lại app": bỏ `is_show_native_after_inter` khỏi cột khóa (câu hỏi 3); vế "inter của resume nối combo khi `is_show_native_after_inter` bật lúc inter kết thúc" thành "inter của resume không nối combo, kể cả khi `is_show_native_after_inter` bật".
   - Dòng "Native toàn màn hình sau inter (combo)": "Inter lúc mở app không nối combo" thành "Inter lúc mở app và inter khi quay lại app không nối combo".
   - Phần thử trên staging: "Đặt `resume_ad_type = inter` (Firebase staging): quay lại thì có inter rồi combo." thành "Đặt `resume_ad_type = inter` (Firebase staging): quay lại thì có inter; đóng inter thì về app ngay, không có native toàn màn hình, kể cả khi `is_show_native_after_inter` bật (logcat có dòng chứa `Interstitial shown: resume_inter`, không có dòng nào chứa `Native combo opened`)."
   - Tài liệu quy tắc: không ghi mã ticket hay plan (ADR-0044).
5. **`scripts/ads-plan.py`, chỉ chữ tiếng Việt của bảng.**
   - `APP_WIDE_TEXT["resume_inter"]`: "nối native combo nếu bật" thành "không nối native combo".
   - `APP_WIDE_TEXT["native_combo"]`: "Ngay sau mọi inter của feature đã qua cổng (kể cả khi inter không lên màn)" thêm "; không sau inter lúc mở app hay khi quay lại app".
   - `KNOWN_KEYS["resume_ad_type"]`, cột ghi chú: "inter thì nối native combo khi is_show_native_after_inter bật." thành "Inter khi quay lại app không nối native combo.".
   - `KNOWN_KEYS["is_show_native_after_inter"]`, cột ghi chú: "Áp cho mọi inter của feature và resume_inter." thành "Chỉ áp cho inter của vị trí trong app; inter lúc mở app và resume_inter không nối.".
   - Chạy `scripts/tests/run.sh` (qua `scripts/check.sh`); các test `ads-plan:` không kiểm chữ này.

Mẫu theo:

- `AdsControllerHostTest`: controller đã `start`, cổng đồng ý mở, Activity dựng sau `start` nên bộ theo dõi Activity thấy nó, `FakeInterstitialAd` đặt vào khe bằng `putLoadedInterstitialForTest`, gọi callback của SDK bằng `fake.callback!!`.
- `AdsControllerNativeComboTest`: khóa combo bật, `nativeFullScreenView`, log `Native combo ...`.

Mọi đường kết thúc của lượt inter đi qua một chỗ. `beginInterstitialTurn` chọn phần hoàn tất theo `combo` một lần, lúc lượt mở (`finish = if (combo) afterInterstitial else onFinished`). Đóng, lỗi hiện, mất quảng cáo, kẹt, Activity bị hủy, quá trần đều chạy `finish` đó. Nên test hai tầng là đủ cho mọi đường:

- tầng `ResumeAdTurns` kiểm giá trị `combo` truyền đi;
- tầng controller kiểm hai đường thật (đóng, lỗi hiện);
- `startupTurn_neverOpensCombo` có sẵn kiểm đường Activity bị hủy với `combo = false`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ResumeAdTurns.kt` | Bước 1: `combo = false` ở nhánh inter; thêm KDoc cho `ResumeAdPort.showInterstitial` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` | Bước 2: chỉ chú thích, KDoc (bốn chỗ) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Bước 3: mô tả của `IS_SHOW_NATIVE_AFTER_INTER` (theo câu hỏi 2; bỏ dòng này và hai dòng sinh dưới nếu người duyệt chọn giữ mô tả) |
| Sửa | `docs/remote-config-keys.md` | Bước 3: sinh lại bằng `RemoteConfigKeysTest`, chỉ dòng `is_show_native_after_inter` |
| Sửa | `docs/remote-config-template.json` | Bước 3: sinh lại cùng lệnh, chỉ `description` của `is_show_native_after_inter` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ResumeAdTurnsTest.kt` | Hai câu khẳng định cũ viết lại (mục 3.5), đổi tên `interResume_showsInterstitialWithCombo` thành `interResume_showsInterstitialWithoutCombo`, thêm phần kiểm thêm (mục 4) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerResumeTest.kt` | Hai test mới của resume inter với khóa combo bật (mục 4); thêm import cần cho chúng (`Robolectric`, `ComponentActivity`, `AdError`, `assertNotNull`) |
| Tạo | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsControllerStartupInterComboTest.kt` | Hai test mới của inter lúc mở app với khóa combo bật (mục 4) |
| Sửa | `docs/ops/ads-guide.md` | Bước 4: ba chỗ |
| Sửa | `scripts/ads-plan.py` | Bước 5: bốn chuỗi tiếng Việt |
| Sửa | `docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md` | Nhật ký thực hiện |
| Sửa | `docs/features/ads/tickets/T-089-khong-combo-sau-inter-resume.md` | Trạng thái ticket |

Nơi đọc file bị đổi:

- `docs/remote-config-keys.md`:
  - `scripts/ads-plan.py` đọc khóa, kiểu, mặc định; mô tả tiếng Anh chỉ dùng cho khóa không có trong `KNOWN_KEYS`, mà `is_show_native_after_inter` có.
  - `scripts/history-refs.py` bỏ qua (file sinh); `scripts/main-guard-verified.sh` và `scripts/template-sync.sh` chỉ chép, so.
- `docs/remote-config-template.json`: `RemoteConfigKeysTest` so, dev nhập vào Firebase.
- `app/build.gradle.kts` chỉ truyền đường dẫn hai file vào test và khai báo chúng là đầu vào của task test.
- `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` sinh và so hai file với khai báo khóa trong code, không chứa chữ mô tả nào.
- Không file nào trong số đó cần sửa: chúng không phụ thuộc chữ mô tả.
- `scripts/ads-plan.py` không có test nào kiểm bốn chuỗi plan đổi (`scripts/tests/run.sh`, khối `ads-plan:`).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí. Bớt một quảng cáo toàn màn hình (native `native_combo`) sau inter của vị trí toàn app `resume_inter` khi `is_show_native_after_inter` bật, theo spec `ads`, KB-14, KB-16 (đổi 2026-11-06). Inter của vị trí trong app giữ combo. Không đổi khóa Remote Config nào (chỉ chữ mô tả).
- Sự kiện phân tích: không đổi định nghĩa hay tham số sự kiện nào. Lượt resume inter vẫn ghi cơ hội, `shown`, hiển thị như cũ. Sau nó không còn lượt combo, nên không còn cơ hội của `native_combo` và không còn sự kiện eligible của native toàn màn hình (`telemetry.eligible(AdFormat.NATIVE_FULL_SCREEN)`, `af_native_ad_eligible` của AppsFlyer). Số liệu đó giảm theo luật mới.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Test dùng `com.google.android.gms.ads.AdError` của `play-services-ads` đã có trên classpath test của `:core:ads` (`FakeFullScreenSdkAds.kt` đã dùng các lớp cùng gói).

### 3.4. Phương án dự phòng

Không có: một dòng code và chú thích; đường test đã có mẫu chạy được (`AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck`, `startup_destroyedHost_appOpen_showFail_finishesFalse`).

### 3.5. Hành vi đã có bị đổi

`python3 scripts/existing-tests.py docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md`, chạy lại trên bản plan sau rà soát (2026-11-06):

- 303 test dùng tên của ba file code.
- Bản cuối của plan: "Cần xem trước" 0, "Nên xem" 162, plan đã nhắc 32; 109 test chỉ dùng tên plan không nêu.
- "Cần xem trước" ở các bản trước của plan, 8 test (nay plan đã nêu tên nên còn 0): `AdGalleryScreenTest.collapsibleCollapsed_slotAt795`, `collapsible_pinsAdAtBottom`, `RewardedActionTest.longContent_small_buttonsInsideWindow`, `noThanksText_usesOnSurfaceVariant_light_and_dark`, `realController_adFreeWhileDialogOpen_watchRunsAction`, `watchButton_is56dpHigh_fullCardWidth`, `BannerAdSizeTest.admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob`, `CollapsibleNativeAdTest.layer_leavesAdChoicesTopStartCornerFree`. Chúng chỉ trùng tên chung (`start`, `AdsController`, `AdsSettings`), đo giao diện banner, native, hộp thoại rewarded; không kiểm combo, resume hay inter lúc mở app: giữ.
- "Nên xem": đã lọc theo "inter", "combo", "resume", "startup", "fullscreen" và đọc các test khớp. Không test nào trong số đó dựng lượt resume loại inter hay `showStartupAd` với inter qua cổng và khóa combo bật, nên không test nào sẽ đỏ ngoài hai câu dưới:
  - `ResumeAdTurnsTest` (các test khác): chỉ dùng `RESUME` (app open) hay kiểm `interShows` rỗng.
  - `AdsControllerResumeTest.resumeOff_gateHasNoPlacement_foregroundRecordsNothing`, `readResumeGate_mapsControllerState`, `adFreeMidSession_resumeOff_noOpportunity`: chỉ đọc cổng, không hiện inter.
  - `AdPlacementTest.resumePlacements_areAppWide`, `nativeCombo_isAppWideNativeFullScreen`: thuộc tính vị trí, không đổi.
  - `AdsSettingsTest.resumeKey_keepsAdsKitTypo` và test kiểm mô tả khóa AdsKit không chứa "not an AdsKit key": mô tả mới không chứa chuỗi đó.
  - `AdsControllerHostTest.*`, `AdsControllerNativeComboTest.blockedAtGate_*`, `standalone_*`: inter hay native của vị trí trong app, không đổi.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Lượt resume loại inter gọi `showInterstitial(..., combo = true)` | Bước 1: gọi với `combo = false` | `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)` | Viết lại (câu A dưới) |
| Như trên, ở lượt sau khi loại resume đổi sang inter | Bước 1 | `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing`, câu đối chứng cuối: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)` | Viết lại (câu B dưới) |
| Inter lúc mở app không nối combo | Không đổi | `AdsControllerNativeComboTest.startupTurn_neverOpensCombo`, `showStartupAd_blocked_neverOpensCombo` | Giữ |
| Inter của vị trí trong app nối combo ở mọi đường kết thúc | Không đổi | `AdsControllerNativeComboTest.closed_opensCombo`, `showFailed_opensCombo`, `adLost_opensCombo`, `stuckRecovered_opensCombo`, `hostDestroyed_opensCombo`, `capExpired_reachesCombo_busyFinishesAtOnce`, `onFinished_runsOnMainThread`, `AdsControllerHostTest.interstitial_hostGone_comboOn_noNativeFullScreen` | Giữ |
| Lượt inter của vị trí trong app: số liệu phễu, cửa sổ quảng cáo trong màn, Activity giữ lượt khi xoay | Không đổi | `AdsControllerFunnelTest.combo_recordsNativeEligibleAfterInter`, `comboOff_recordsInterEligibleOnly`, `interTurn_recordsEligibleOnce`, `AdsControllerInScreenWindowTest.comboTurn_noOpenWindowAcrossInterClose`, `AdsControllerHostTest.handover_*`, `rotation_*`, `interstitial_*Host_notShown_keptInSlot*` | Giữ: đều gọi `beginInterstitialTurn` hay `showInterstitial` của vị trí trong app, không qua `ResumeAdTurns` hay `showStartupAd` |
| Cổng, cơ hội, nạp, lần đầu, chặn, đổi loại của resume | Không đổi | Các test còn lại của `ResumeAdTurnsTest`, `ResumeAdRulesTest`, `AdsControllerResumeTest`, `ResumeAdLifecycleTest`, `AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck` | Giữ |
| Mô tả khóa `is_show_native_after_inter` | Bước 3 | `RemoteConfigKeysTest` (`:app`) so hai file sinh với code; `AdsSettingsTest` kiểm mô tả khóa AdsKit | Giữ câu khẳng định; dữ liệu là hai file sinh lại trong cùng commit code |

Hai câu dưới là câu mới viết sẵn trong plan theo quyết định 1 của ADR-0031; câu cũ là test có sẵn, kiểm luật A-12 trước khi đổi. Mỗi câu ghi đủ sáu điều của quyết định 2.

**Câu A**, `ResumeAdTurnsTest.interResume_showsInterstitialWithCombo`, đổi tên thành `interResume_showsInterstitialWithoutCombo`:

1. Câu plan nói điều mới: mục 3.1 bước 1, "nhánh `AdFormat.INTERSTITIAL` gọi `port.showInterstitial(activity, placement, combo = false)`"; spec KB-14 tiêu chí 1 (đổi 2026-11-06), luật A-12.
2. Ý định: lượt resume loại inter hiện đúng một interstitial của `resume_inter`, trên Activity vừa resume, không hiện app open, và mang đúng lựa chọn combo của luật (A-12, A-15). Câu mới: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; giữ `assertTrue(port.appOpenShows.isEmpty())`.
3. Không nới: vẫn là phép bằng trên cả bộ ba (Activity, vị trí, combo), không bớt câu nào.
4. Đối chứng: lần chạy ở bước viết test trước, trên code hiện tại (lượt resume còn truyền `combo = true`): câu mới đỏ, kỳ vọng `expected:<[(..., RESUME_INTER, false)]> but was:<[(..., RESUME_INTER, true)]>`. Code hiện tại chính là bản phá ý định, nên lần chạy này là đối chứng; ghi vào nhật ký.
5. Là câu của test JVM, không phải ảnh chuẩn. Câu cũ kiểm luật A-12 trước khi đổi; luật đã đổi 2026-11-06 và plan thực hiện đúng phần đổi đó.
6. Tính chung với câu B: 2 câu, dưới giới hạn 3.

**Câu B**, `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing`, câu đối chứng cuối:

1. Câu plan nói điều mới: như câu A.
2. Ý định: lượt bị bỏ vì loại đổi giữa lúc quyết và lúc hiện không hiện gì; lượt sau, quyết với loại mới, hiện đúng một interstitial của `resume_inter` (L-A05 mở rộng cho đổi loại). Câu mới: `assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, false)), port.interShows)`; giữ `assertTrue(port.appOpenShows.isEmpty())` và hai câu đầu của test.
3. Không nới: như câu A.
4. Đối chứng: như câu A (cùng lần chạy trên code hiện tại).
5. Như câu A.
6. Như câu A.

## 4. Kiểm thử

Test JVM của `:core:ads`, Robolectric như các lớp có sẵn. Không có giao diện, không có ảnh chuẩn.

Log của combo thật là `"Native combo opened (inter gone: $gone)"` (`NativeFullScreenTurns.kt`, dòng 127). Test JVM so `message` của `RecordingAppLogger` bằng `startsWith`, không so bằng (kiểm logcat trên máy thì dùng "có dòng chứa", vì logcat in ngày giờ, PID, thẻ trước nội dung):

- "không có combo": `assertTrue(logger.entries.none { it.message.startsWith("Native combo") })`;
- "có combo" (đối chứng): `assertTrue(logger.entries.any { it.message.startsWith("Native combo opened") })`.

**Bước dựng chung của test resume** (`AdsControllerResumeTest`, hai test mới), gọi là "dựng resume":

1. `settings` bật `IS_LOG_AD_FULLSCREEN_EVENTS`, `SHOW_RESUM_ADS`, `IS_SHOW_NATIVE_AFTER_INTER`, `placementEnabled(GALLERY_INTERSTITIAL)`; `RESUME_AD_TYPE = "inter"`.
2. `startedController()`, `openConsentGateForTest()`, `idle()`, `idleFor(45_100)` (lượt mở app hết).
3. `fake = FakeInterstitialAd()` vào khe `RESUME_INTER` bằng `putLoadedInterstitialForTest`.
4. `host = Robolectric.buildActivity(ComponentActivity::class.java).setup()`; `host.pause().stop()`, `host.start().resume()`, `idle()`.
5. Kiểm: `fake.shows == 1`, `fake.shownOn.single() === host.get()`, log có `"Interstitial shown: resume_inter"`.

**Bước dựng chung của test mở app** (`AdsControllerStartupInterComboTest`), gọi là "dựng mở app", theo `AdsControllerHostTest`:

1. Controller có `application`, `AdUnitIds { null }`, `DefaultAdsFlags(settings)`, `RecordingAdMetrics`, `RecordingAppLogger`.
2. `settings` bật `IS_LOG_AD_FULLSCREEN_EVENTS`, `SHOW_OPEN_ADS`, `SHOW_OPEN_ADS_FIRST_OPEN`, `IS_SHOW_NATIVE_AFTER_INTER`, `placementEnabled(GALLERY_INTERSTITIAL)`; `OPEN_AD_TYPE = "inter"`.
3. `start(application, AppActivityFilter { true })`, `idle()`, `openConsentGateForTest()`, `idle()`.
4. `fake = FakeInterstitialAd()` vào khe `OPEN_APP_INTER`; `activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()`.
5. `controller.showStartupAd(activity, StartupAdTurn(AdPlacement.OPEN_APP_INTER)) { results += it }`, `idle()`.
6. Kiểm: `fake.shows == 1`, `results` rỗng.

**Đối chứng combo** (dùng trong test có ghi; khóa bật thật và combo vẫn chạy cho inter của vị trí trong app):

1. `idleFor(45_100)` (mốc `ads_interval` do inter vừa rồi đẩy).
2. `gallery = FakeInterstitialAd()` vào khe `GALLERY_INTERSTITIAL`.
3. `CoroutineScope(Dispatchers.Main).launch { controller.showInterstitial(<Activity đang resume>, AdPlacement.GALLERY_INTERSTITIAL) {} }`, `idle()`; kiểm `gallery.shows == 1`.
4. Gọi kết thúc của `gallery` cùng loại với test (đóng hay lỗi hiện); ngay sau đó `assertTrue(controller.fullScreenAdShowing.value)`.
5. `idleFor(2_100)`: `assertNotNull(controller.nativeFullScreenView.value)` và `assertTrue(logger.entries.any { it.message.startsWith("Native combo opened") })`.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-14 tiêu chí 1, A-12: lượt resume loại inter quyết không combo (tầng `ResumeAdTurns`) | Unit test | `ResumeAdTurnsTest.interResume_showsInterstitialWithoutCombo` (câu A, mục 3.5). Kiểm thêm trong cùng test, sau câu A: `returnToApp()`, `turns.onAppActivityResumed(activity)`; `assertEquals(listOf(false, false), port.interShows.map { it.third })` (lượt sau cũng không combo) |
| KB-14, L-A05 mở rộng: lượt sau khi đổi loại cũng không combo | Unit test | `ResumeAdTurnsTest.typeChangedBeforeResume_showsNothing` (câu B, mục 3.5) |
| KB-14 tiêu chí 1, KB-16 tiêu chí 3: inter của resume đóng xong, khóa combo bật, không có native (đường thật của controller) | Unit test | `AdsControllerResumeTest.resumeInter_closed_comboOn_noNativeFullScreen`: "dựng resume"; `fake.callback!!.onAdDismissedFullScreenContent()`; ngay sau đó `assertFalse(controller.fullScreenAdShowing.value)` (combo giữ cờ nếu có); `idleFor(2_100)`: `assertNull(controller.nativeFullScreenView.value)`, không có `message` log nào bắt đầu bằng `Native combo`; `idleFor(5_000)`: `nativeFullScreenView` vẫn `null`. Rồi "đối chứng combo" với `host.get()`, đường đóng |
| K-01, KB-16 tiêu chí 8 (theo cách hiểu ở mục 2, câu hỏi 4): inter của resume hiện lỗi, khóa combo bật, không có native | Unit test | `AdsControllerResumeTest.resumeInter_showFailed_comboOn_noNativeFullScreen`: "dựng resume"; `fake.callback!!.onAdFailedToShowFullScreenContent(AdError(1, "Show failed.", "com.google.android.gms.ads"))`; ngay sau đó `assertFalse(controller.fullScreenAdShowing.value)`; `idleFor(2_100)`: `nativeFullScreenView` `null`, không có `message` log nào bắt đầu bằng `Native combo`. Rồi "đối chứng combo" với `host.get()`, đường lỗi hiện (K-01: inter vị trí trong app hiện lỗi vẫn mở combo) |
| KB-13 tiêu chí 4, A-02: inter lúc mở app đóng xong, khóa combo bật, không có native, người dùng đi tiếp ngay | Unit test (khóa hành vi đã có) | `AdsControllerStartupInterComboTest.startupInter_closed_comboOn_noNativeFullScreen`: "dựng mở app"; `fake.callback!!.onAdDismissedFullScreenContent()`; ngay sau đó `assertEquals(listOf(true), results)` và `assertFalse(controller.fullScreenAdShowing.value)`; `idleFor(2_100)`: `nativeFullScreenView` `null`, không có `message` log nào bắt đầu bằng `Native combo`; `idleFor(5_000)`: `results` vẫn `[true]`. Rồi "đối chứng combo" với `activity`, đường đóng |
| KB-13, K-01 (theo cách hiểu ở mục 2): inter lúc mở app qua cổng mà hiện lỗi, khóa combo bật, không có native | Unit test (khóa hành vi đã có) | `AdsControllerStartupInterComboTest.startupInter_showFailed_comboOn_noNativeFullScreen`: "dựng mở app"; `fake.callback!!.onAdFailedToShowFullScreenContent(AdError(1, "Show failed.", "com.google.android.gms.ads"))`; ngay sau đó `results == [true]` (inter đã gọi hiện), `fullScreenAdShowing` `false`; `idleFor(2_100)`: `nativeFullScreenView` `null`, không có `message` log nào bắt đầu bằng `Native combo` |
| KB-16 tiêu chí 1: inter của vị trí trong app vẫn nối combo ở mọi đường | Unit test có sẵn | `AdsControllerNativeComboTest` (mọi test `*_opensCombo`, `capExpired_*`, `onFinished_runsOnMainThread`), `AdsControllerHostTest.interstitial_hostGone_comboOn_noNativeFullScreen` giữ nguyên và phải qua |
| Bảng khóa sinh khớp code | Unit test có sẵn | `RemoteConfigKeysTest` (`:app`), `AdsSettingsTest` qua sau khi sinh lại |
| Tài liệu và bảng Excel không còn nói resume inter nối combo | Kiểm tra thủ công (người: đọc tài liệu) | Đọc ba chỗ của `docs/ops/ads-guide.md` (mục 3.1 bước 4); chạy `python3 scripts/ads-plan.py --dump` và xem chữ của `resume_inter`, `native_combo`, `resume_ad_type`, `is_show_native_after_inter` |
| KB-14 trên máy: quay lại app với `resume_ad_type = inter` thì có inter, đóng inter về app ngay | Kiểm tra thủ công (máy thật: Remote Config) | Các bước ở danh sách "Bước kiểm tay trên staging" dưới bảng |

**Bước kiểm tay trên staging** (máy thật: Remote Config):

1. Firebase staging đặt `resume_ad_type = inter`; `is_show_native_after_inter` bật sẵn ở staging.
2. Đồng ý quảng cáo, mở app, bấm Home rồi mở lại: có inter thử của Google. Log `Resume ad NOT_READY: resume_inter` thì inter chưa nạp xong: quay lại app lần nữa.
3. Đóng inter: về màn trước, không có native toàn màn hình.
4. Log `adb logcat -v threadtime | grep ' VGL\.Ads'`: có dòng chứa `Resume ad SHOW: resume_inter`, `Resume ad shown on resume: resume_inter`, `Interstitial shown: resume_inter`; không có dòng nào chứa `Native combo opened`. Logcat in ngày giờ, PID và thẻ trước nội dung, nên kiểm bằng "có dòng chứa", không bằng "bắt đầu bằng".
5. Đối chứng: màn xem thử, nút "Interstitial", đóng inter thì có native toàn màn hình (có dòng chứa `Native combo opened`).
6. Dòng log dẫn: `ResumeAdTurns.onProcessForegroundChanged` (`Resume ad $decision: ...`), `onAppActivityResumed` (`Resume ad shown on resume: ...`), `AdsController.showInterstitialTurn` (`Interstitial shown: ...`), `NativeFullScreenTurns.afterInterstitial` (`Native combo opened (inter gone: $gone)`).

Thứ tự viết test của phiên (bước 3 của `auto-run.md`):

- `ResumeAdTurnsTest` (câu A, câu B) và hai test của `AdsControllerResumeTest` đỏ trên code hiện tại, vì lượt resume đang truyền `combo = true`: combo mở, cờ toàn màn hình còn giữ sau khi inter đóng, có `nativeFullScreenView`. Đó là lý do đúng. Lần chạy đỏ này cũng là đối chứng (điều 4 của ADR-0031) cho câu A, B: ghi kết quả vào nhật ký và mục "Test cũ viết lại (ADR-0031)" của mô tả PR. Sau bước 1 chúng xanh. Phần kiểm thêm của câu A nằm sau câu A, nên chỉ chạy tới sau bước 1 (câu A đỏ thì test dừng trước nó).
- Hai test của `AdsControllerStartupInterComboTest` khóa hành vi đã có nên qua ngay trên code hiện tại. Để chúng thất bại đúng lý do, trước khi sửa code (trước bước 1 và bước 2, để lệnh trả file không xóa phần sửa), phiên chạy chúng một lần với đột biến tạm, không commit:
  - `perl -pi -e 's/\QshowInterstitialTurn(activity, turn.placement, combo = false, finish)\E/showInterstitialTurn(activity, turn.placement, combo = true, finish)/' core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt`
  - `git diff --stat -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt` phải thấy 1 dòng đổi (không đổi thì mẫu không khớp, lần thử không có giá trị).
  - `./gradlew :core:ads:testDebugUnitTest --tests '*AdsControllerStartupInterComboTest*'`: cả hai đỏ (kỳ vọng ở câu `assertEquals(listOf(true), results)` hay `assertFalse(controller.fullScreenAdShowing.value)`: combo giữ cờ và hoãn phần hoàn tất).
  - `git checkout -- core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsController.kt`.
  - Ghi lần chạy vào nhật ký.

**Ngoại lệ viết test trước:** (1) hai test của `AdsControllerStartupInterComboTest` qua ngay trên code hiện tại vì chúng khóa hành vi đã có (ticket: "giữ nguyên hành vi đó và thêm test khóa nó lại"); bằng chứng thất bại đúng lý do là lần chạy với đột biến tạm ở trên. (2) Chú thích, KDoc (bước 1, 2), mô tả khóa (bước 3), tài liệu (bước 4) và chữ của `scripts/ads-plan.py` (bước 5) là chữ, không có test đỏ trước; `RemoteConfigKeysTest` có sẵn bắt hai file sinh lệch với code.

### 4.1. Chạy thử

Không cần (E dưới 2). Không chạy test tạm: máy soạn plan chưa có JDK (`scripts/local-setup.sh --check` báo thiếu JDK). Mẫu dựng test có sẵn và đang qua trên nhánh chính:

- `AdsControllerHostTest.resumeAd_fromOnActivityResumed_passesHostCheck`: resume qua bộ theo dõi Activity với quảng cáo giả trong khe.
- `startup_destroyedHost_appOpen_showFail_finishesFalse`: `showStartupAd` với quảng cáo giả.
- `interstitial_hostGone_comboOn_noNativeFullScreen`: khóa combo bật với controller đã `start`.

`python3 scripts/existing-tests.py docs/features/ads/plans/T-089-khong-combo-sau-inter-resume.md`, chạy lại trên bản plan sau rà soát (2026-11-06, nhánh từ `origin/main` tại `ac7335d`):

- Số liệu và cách xếp ở mục 3.5.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 1 (quảng cáo: đổi tần suất, bớt native toàn màn hình sau inter của resume khi combo bật), 7 (module core `:core:ads`). Không chạm 2, 3, 4 (không đổi sự kiện hay tham số), 5, 6, 8, 9. Người phụ trách sản phẩm (anh Nhật) là người yêu cầu và đã xác nhận spec 2026-11-06.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một dòng code, nhưng nhiều phần của `:core:ads` (`ResumeAdTurns.kt`, chú thích `AdsController.kt`, mô tả trong `AdsSettings.kt`), hai file bảng khóa sinh lại, một script và một tài liệu hướng dẫn; không đổi giao diện công khai của lớp nào |
| B. Độ rõ của yêu cầu | 1 | Spec `confirmed`, tiêu chí rõ ở KB-13, KB-14, KB-16; luật K-01, A-12 đã ghi. Còn chi tiết AI đề xuất (câu hỏi 1, 2, 3) và một chỗ spec chưa ghi đổi (KB-16 tiêu chí 8, câu hỏi 4) mà luật K-01 đã nói rõ |
| C. Khả năng kiểm chứng tự động | 1 | Mọi tiêu chí có test JVM với controller thật và quảng cáo giả; hành vi với quảng cáo thật và Remote Config thật chỉ kiểm trên bản staging (một bước thủ công) |
| D. Ảnh hưởng chéo | 2 | `AdsController.kt`, `AdsSettings.kt`, `scripts/ads-plan.py`, `docs/ops/ads-guide.md` và hai file bảng khóa cũng nằm trong phạm vi của T-086, T-087, T-088 (chưa có plan); đổi hành vi dùng chung của mọi app (resume), nhưng không feature nào dựa vào combo sau resume |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của test có sẵn: lần đầu đặt `FakeInterstitialAd` vào khe `RESUME_INTER` và `OPEN_APP_INTER` |
| **Tổng** | 7 | **Mức: Cao** (chạm điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `ResumeAdTurns.kt` (lượt resume của mọi app), chú thích của `AdsController.kt`, mô tả khóa trong `AdsSettings.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/ops/ads-guide.md`, `scripts/ads-plan.py`.
- Task khác có thể xung đột (tất cả chưa có plan):
  - T-086: `AdsController.kt`, `ads-guide.md`.
  - T-087: `AdsController.kt`, `AdsSettings.kt`, `ads-plan.py`, `ads-guide.md`, bảng khóa.
  - T-088: `AdsSettings.kt`, `ads-plan.py`, `ads-guide.md`, bảng khóa.
  - T-090, T-091, T-092: `ads-guide.md`, `ads-plan.py`.
  - Chỗ T-089 sửa trong các file đó là dòng riêng (chú thích, một mô tả khóa, bốn chuỗi), nên chỉ có thể xung đột khi merge, không xung đột ý. Hai file bảng khóa sinh từ code: task chạy sau sinh lại trên nhánh chính mới nhất.
- Đề xuất thứ tự chạy: chạy trước các task ads khác trong cùng phiên (thay đổi nhỏ nhất, ít xung đột merge nhất cho task sau). `run-check.py` sẽ báo cùng sửa file; không cần thứ tự bắt buộc.

## 7. Điều kiện dừng riêng

- Một test có sẵn của `AdsControllerNativeComboTest` hay `AdsControllerHostTest` (combo của inter vị trí trong app) đỏ sau bước 1: dừng, vì plan không đổi combo của vị trí trong app.
- Sinh lại bảng khóa đổi dòng nào khác ngoài `is_show_native_after_inter`: dừng (nhánh chính đã lệch bảng khóa, ngoài phạm vi).
- Một test có sẵn của `ResumeAdTurnsTest`, `ResumeAdRulesTest` hay `AdsControllerResumeTest` ngoài hai câu của mục 3.5 đỏ: dừng, vì plan không đổi cổng hay cơ hội của resume.

## 8. Câu hỏi cho người duyệt

1. **Giữ hay bỏ tham số `combo` của `ResumeAdPort.showInterstitial`?** Sau đổi, lượt resume luôn truyền `false`.
   - Phương án A (AI đề xuất): giữ tham số, truyền `false`. Ưu: một dòng code; `ResumeAdTurnsTest` kiểm được lựa chọn combo ở tầng thuần (câu A, B), và giá trị `true` bị bắt ở cả hai tầng; nếu sau này cần đổi lại chỉ sửa một chỗ. Nhược: tham số chỉ còn một giá trị.
   - Phương án B: bỏ tham số; phần cài của cổng trong `AdsController` gọi `showInterstitialTurn(..., combo = false, ...)`. Ưu: chữ ký gọn. Nhược: lựa chọn combo chuyển hẳn vào phần nối trong `AdsController`, chỉ test controller bắt được; câu A, B phải đổi kiểu dữ liệu của bản giả (`Pair` thay `Triple`), thêm thay đổi cho `FakeResumeAdPort`. Chọn B thì plan lên version 2: phải đổi bản giả `FakeResumeAdPort` và câu A, B, cùng mục 3.1, 3.5, 4.
   - Phương án C (để sau): suy combo từ vị trí (`AdPlacement.appWide` thì không combo) trong `beginInterstitialTurn`, bỏ tham số ở mọi chỗ. Gom luật K-01 về một chỗ, nhưng đổi nhiều test của `AdsControllerNativeComboTest`; ngoài phạm vi ticket cỡ S.
2. **Đổi mô tả khóa `is_show_native_after_inter` trong `AdsSettings.kt` không?** Mô tả hiện tại "Shows a full-screen native ad right after an interstitial (combo)." nay không đúng hết (inter lúc mở app và khi quay lại không nối). Mô tả này vào `docs/remote-config-keys.md` và `docs/remote-config-template.json` (dev nhập vào Firebase).
   - AI đề xuất: đổi thành "Shows a full-screen native ad right after the interstitial of an in-app placement (combo); never after the startup or resume interstitial." và sinh lại hai file trong cùng PR. Khóa, kiểu, mặc định không đổi.
   - Nếu chọn giữ: bỏ ba dòng tương ứng của mục 3.2 (`AdsSettings.kt` và hai file sinh), bỏ bước 3.
3. **Bảng định dạng của `docs/ops/ads-guide.md`, dòng "Quảng cáo khi quay lại app": bỏ `is_show_native_after_inter` khỏi cột khóa?** Spec mục 5 đã gạch khóa này khỏi dòng `resume` (2026-11-06). AI đề xuất bỏ, như spec.
4. **KB-16 tiêu chí 8 chỉ áp cho inter của vị trí trong app?** Tiêu chí 8 ("inter đã được phép hiện nhưng không lên được màn ... và native sau inter đang bật thì vẫn có màn native") chưa có ghi chú đổi 2026-11-06. Tiêu chí 3 của KB-16 và luật K-01 nói inter lúc mở app và của resume không bao giờ nối combo.
   - AI đề xuất: xác nhận tiêu chí 8 chỉ áp cho inter của vị trí trong app (plan viết hai test "hiện lỗi" theo cách hiểu này), và ghi chú đổi vào tiêu chí 8 của spec trong một PR tài liệu riêng, không trong PR của task; người duyệt hay AI làm sau khi duyệt, ngoài phiên tự chạy.
   - Nếu người duyệt hiểu khác (inter lúc mở app hay của resume hiện lỗi vẫn có native): plan lên version 2, vì `combo` khi đó phụ thuộc đường kết thúc.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
