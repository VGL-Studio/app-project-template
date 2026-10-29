---
ticket: T-066
title: Khởi động sớm bằng đồng ý của lần mở trước luôn bật
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # để trống: chạm điểm chặn 5 (đồng ý, thứ tự khởi tạo SDK) và 7 (module core)
risk_level: high             # low | medium | high
token_estimate: 5.4M         # cỡ S theo ticket, số gợi ý 5.4M (scripts/token-usage.py estimate, 2026-10-29); không chỉnh
blocking_points: [consent, init-order, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-066: KHỞI ĐỘNG SỚM BẰNG ĐỒNG Ý CỦA LẦN MỞ TRƯỚC LUÔN BẬT

## 1. Tóm tắt

Tách điều kiện của nhánh rút gọn trong `ConsentGate`: `CACHED_EARLY` (UMP đã có đồng ý dùng được của phiên trước ngay lúc gửi lượt hỏi) mở cổng và khởi tạo SDK ở mọi bản, không theo cấu hình build hay `is_enable_consent_shortcut`; `ERROR_ALLOWED` giữ đúng S-04 tới S-12, cần cả hai công tắc, mặc định tắt (ADR-0038, quyết định 3). Luật chốt S-06 đọc công tắc như "`ERROR_ALLOWED` có dùng được không", nên khi công tắc tắt thì form lỗi sau khi UMP đã cho phép vẫn chốt, kể cả khi đường đang là `CACHED_EARLY`. Kiểm chứng bằng unit test của máy trạng thái và bộ điều phối với UMP giả, test mô tả khóa, test khớp bảng khóa sinh tự động, và kiểm tay trên staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/ads/spec.md` (spec `confirmed` 2026-10-29; ADR-0038 `accepted`):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-11, tiêu chí 1 | Khi người dùng đã đồng ý ở lần mở trước thì quảng cáo bắt đầu tải ngay lúc mở app, không chờ hộp thoại đồng ý cập nhật xong. | S-03: ngay sau `requestUpdate`, nếu UMP trả `canRequestAds() == true` và chưa có đường nào thì đường `CACHED_EARLY`, cổng mở, `onGateOpened(true)` khởi tạo SDK. Từ nay không đọc công tắc. Hôm nay tiêu chí này chỉ đạt khi cả hai công tắc bật; ở production cấu hình build tắt nên không bao giờ đạt. |
| KB-11, tiêu chí 3, 4, 5 | Lỗi mạng ở foreground vẫn tải, hỏi lại khi mạng về; ở nền không có yêu cầu bằng quyền tạm lỗi mạng; hỏi lại báo cần hỏi thì dừng và không hiện quảng cáo đã tải bằng quyền tạm. | Không đổi. Tiêu chí 5 (S-10, S-11) từ nay cũng áp cho `CACHED_EARLY` khi công tắc tắt, vì `CACHED_EARLY` có ở mọi bản. |
| KB-11, tiêu chí 6 (đã đổi 2026-10-29) | Khi app chưa bật quyền tạm thì quảng cáo chỉ tải khi hộp thoại đồng ý cho phép; lỗi mạng của việc hỏi đồng ý không cho tải. **Đổi 2026-10-29:** `CACHED_EARLY` luôn bật, không cần cấu hình hay công tắc; phần `ERROR_ALLOWED` giữ đúng tiêu chí này. | Công tắc tắt: lỗi mạng (hay lỗi cấu hình) không bao giờ cho đường `ERROR_ALLOWED`. Nếu cổng đã mở bằng `CACHED_EARLY` trước lỗi đó thì giữ `CACHED_EARLY` (quyền thật của phiên trước, như S-04, S-05 đã làm khi công tắc bật). Câu hỏi 2. |
| KB-11, tiêu chí 7 | Khi người vận hành tắt quyền tạm từ xa thì quyền tạm khi lỗi mạng bị thu hồi ngay; quyền người dùng đã cho vẫn giữ. | S-12: tắt công tắc giữa phiên chỉ thu hồi `ERROR_ALLOWED` chưa chốt (đã có, giữ). `CACHED_EARLY` và `GRANTED` giữ. Lần mở app sau, công tắc vẫn tắt, `CACHED_EARLY` vẫn mở cổng. |
| KB-11, tiêu chí 8 | Khi quyền tạm tắt mà hộp thoại đồng ý lỗi sau khi người dùng đã cho phép thì kết quả được giữ cho cả lần mở app, không hỏi lại. | S-06, nhánh tắt: form lỗi (`CONFIGURATION` hay `NETWORK`) với `canRequestAds() == true` thì chốt `GRANTED`, cả khi đường đang là `CACHED_EARLY`. Hôm nay nhánh `cachedEarly -> current` đứng trước nên sẽ giữ `CACHED_EARLY` không chốt; plan đổi thứ tự cho nhánh tắt (mục 3.1, câu hỏi 1). |
| KB-12 | Rút đồng ý thì không còn yêu cầu mới, quảng cáo đã tải không hiện; đồng ý lại thì tải lại. | Không đổi (S-14, S-15). Rút đồng ý xong, lần mở sau UMP trả `canRequestAds() == false` nên không có `CACHED_EARLY`. |
| KB-32, tiêu chí 4 | Khi người dùng đã đồng ý ở lần mở app trước thì SDK quảng cáo khởi động ngay lúc mở app, không chờ hộp thoại đồng ý trả lời, không cần cấu hình hay công tắc nào (`CACHED_EARLY`, S-03, S-12). | Như KB-11 tiêu chí 1. Ở JBase: `admobadshelper-release.aar`, lớp `com.jb.admobadshelper.AdmobHelper`, hàm `startConsentFlow` gọi `GoogleMobileAdsConsentManager.gatherConsent` rồi ngay sau đó `canRequestAds()` đúng thì `initializeMobileAdsSdk` (đọc bằng `javap -c`, 2026-10-29), không có cờ nào. Phần JBase bỏ qua đồng ý khi mở app lúc không có mạng (`LibraryBridge.java:820-824`) là lỗi, không theo (ADR-0038, quyết định 2). |

Câu hỏi 21 của spec (công tắc mặc định tắt) vẫn đúng cho `ERROR_ALLOWED`; ADR-0038 thay S-12 cho phần `CACHED_EARLY`.

Phản hồi đã đọc: FB-20261025-01 và FB-20261026-01 (`fixed`): chạm "Do not consent" mà UMP vẫn cho quảng cáo không cá nhân hóa (`canRequestAds() == true`), app đi đường `GRANTED` đúng luật (anh Nhật xác nhận 2026-10-27). Hệ quả cho plan này: lần mở sau của người đã từ chối như vậy cũng đi `CACHED_EARLY`. Đây là cùng quyết định "theo `canRequestAds` của UMP", không phải hành vi mới. Kịch bản `app/src/deviceScenarios/T-024-mo-app-tu-choi-dong-y.txt` bắt đầu bằng `fresh` (xóa dữ liệu), nên lần mở đầu không có đồng ý cũ và kịch bản không đổi.

Plan trước đã đọc: T-020 (nhật ký RUN-20261024-02: dòng "Cần người xem" hỏi người đã đồng ý mở app lúc mất mạng ở production thì không có quảng cáo cả phiên; plan này trả lời phần đó: `CACHED_EARLY` giữ qua lỗi mạng), T-036 (trang "Ad status": bước kiểm tay 2 bật công tắc để thấy `CACHED_EARLY`, nay không cần).

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện; trang "Ad status" giữ nguyên chữ, câu hỏi 3).

ADR liên quan: ADR-0013 (đồng ý và chặn), ADR-0026 (theo PARTNER), ADR-0038 (quyết định 3: `CACHED_EARLY` luôn bật), ADR-0031 (câu khẳng định cũ đổi), ADR-0029.

## 3. Giải pháp

### 3.1. Cách làm

**Bước 1. `ConsentGate.kt` (`:core:ads`).**

- `onRequestSent(umpCanRequestAds)`: bỏ vế `shortcut &&`. Điều kiện còn `umpCanRequestAds && current.path == null && !current.settled`, ra `CACHED_EARLY`, `allowed = true`, `temporary = true`, `settled = false` như cũ. `temporary` giữ đúng: S-10 và S-11 dựa vào nó để đóng cổng và không hiện quảng cáo đã nạp khi UMP báo cần hỏi lại.
- `onFailure(...)`: thêm một nhánh ngay sau `INTERRUPTED`, trước `cachedEarly -> current`:

  ```kotlin
  // S-06 with ERROR_ALLOWED off: a form error once UMP allows ads settles, CACHED_EARLY included (KB-11, criterion 8).
  formStep && umpCanRequestAds && !shortcut -> ConsentGateState(
      path = ConsentPath.GRANTED,
      allowed = true,
      temporary = false,
      settled = true,
  )
  ```

  Nhánh `else` cuối đổi `settled = umpCanRequestAds && formStep && !shortcut` thành `settled = false`: trường hợp duy nhất chốt ở nhánh đó nay đi nhánh mới, nên kết quả với mọi đầu vào không có `CACHED_EARLY` giữ nguyên (bảng ở dưới). Các nhánh còn lại giữ thứ tự: `INTERRUPTED`; nhánh mới; `cachedEarly -> current`; `NETWORK && shortcut -> ERROR_ALLOWED`; `else`.
- `onShortcutChanged`: giữ nguyên (chỉ thu hồi `ERROR_ALLOWED` chưa chốt). `onUpdateSucceeded`, `onFormFinished`, `onPrivacyFormClosed`, `onReset`, `mayRequestNow`, `regatherDue` giữ nguyên.
- KDoc: `shortcutActive` "S-12: ERROR_ALLOWED needs both the build switch of :app and is_enable_consent_shortcut; CACHED_EARLY needs neither (ADR-0038)."; trường `shortcut` "The ERROR_ALLOWED switch (S-12), read when a result comes (S-06); CACHED_EARLY does not read it."; `onRequestSent` "S-03: consent of an earlier session opens the gate before UMP answers, in every build (ADR-0038)."; `onUpdateFailed`, `onFormFinished` nêu luật chốt mới.
- Giữ tên `shortcut`, `onShortcutChanged`, `shortcutActive`, `ConsentShortcutConfig`, khóa `is_enable_consent_shortcut` (câu hỏi 4).

Bảng luật chốt sau khi đổi (S-06; `C` là đường đang là `CACHED_EARLY` và cổng mở; "tắt", "bật" là công tắc `ERROR_ALLOWED` đọc lúc có kết quả):

| Kết quả | Công tắc | Không có `C` (như hôm nay) | Có `C` (mới có ở nhánh tắt) |
| --- | --- | --- | --- |
| Kết quả sạch (form đóng không lỗi) | bất kỳ | Chốt theo `canRequestAds` | Chốt theo `canRequestAds` (không đổi) |
| Bước cập nhật lỗi (mạng, cấu hình) | tắt | Không chốt, cổng đóng, `DENIED_OR_REQUIRED` | Không chốt, giữ `C`, cổng mở, không hỏi lại theo S-09; lượt `gather` sau hỏi lại UMP |
| Bước cập nhật lỗi mạng | bật | `ERROR_ALLOWED` | Giữ `C` (không đổi) |
| Form lỗi, `canRequestAds` đúng | tắt | Chốt `GRANTED` | Chốt `GRANTED` (nhánh mới) |
| Form lỗi, `canRequestAds` đúng | bật | Không chốt (lỗi mạng: `ERROR_ALLOWED`) | Giữ `C`, không chốt (không đổi) |
| Form lỗi, `canRequestAds` sai | bất kỳ | Không chốt, cổng đóng (lỗi mạng và công tắc bật: `ERROR_ALLOWED`) | Không xảy ra: `canRequestAds` sai ở bước cập nhật đã đóng `C` theo S-10 |
| Form bị ngắt | bất kỳ | `INTERRUPTED_REGATHER`, không chốt | `INTERRUPTED_REGATHER`, không chốt (không đổi) |

Mọi đường mở cổng lần đầu (L-072): chỉ có `ConsentGate` đặt `allowed = true`; `onGateOpened` của `ConsentCoordinator.afterGateChange` gọi `startSdk` ở lần mở đầu. `CACHED_EARLY` chỉ đặt ở `onRequestSent`, mà chỉ `ConsentCoordinator.gather` gọi: lượt đầu, lượt hỏi lại (S-09, lúc đó `path != null` nên không ra `CACHED_EARLY`), lượt sau đặt lại (S-18, staging) và lượt `gather` của lần tạo activity sau. Test ở mục 4 chạy hàm thuần (`ConsentGateTest`) và đường `gather` (`ConsentCoordinatorTest`).

**Bước 2. `ConsentCoordinator.kt`: không sửa.** HD-04 giữ: lượt hỏi đầu vẫn chờ giá trị đầu của công tắc, vì `ERROR_ALLOWED` và luật chốt đọc nó lúc có kết quả; `AdsController.start` đưa giá trị đã lưu hay mặc định ngay khi bắt đầu (`flags.global`), nên `CACHED_EARLY` không chậm thêm (câu hỏi 5). Log `Consent path: CACHED_EARLY` có sẵn.

**Bước 3. Mô tả khóa và chú thích.**

- `AdsSettings.kt`: mô tả của `IS_ENABLE_CONSENT_SHORTCUT` thành `"Remote switch of ERROR_ALLOWED, the temporary right when UMP fails on the network (S-12); also needs the build setting of the app. CACHED_EARLY is always on (ADR-0038). False takes ERROR_ALLOWED back at once."` (hàm `booleanKey` tự thêm câu "Name set by the template, not an AdsKit key."). Tên, kiểu, mặc định `false` giữ.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json`: sinh lại bằng `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` (cách của `RemoteConfigKeysTest`), chỉ dòng của khóa này đổi. Nơi khác đọc hai file (`grep -rn remote-config-keys scripts/ .github/ */src/test`): `scripts/ads-plan.py` chỉ đọc tên, kiểu, mặc định; `scripts/template-sync.sh` chép nguyên; `scripts/tests/run.sh` dùng file giả của chính nó. Không file nào bị ảnh hưởng bởi mô tả (L-020).
- `ConsentShortcutConfig.kt`: KDoc "The build switch of ERROR_ALLOWED, bound in :app per build flavor (S-12). ERROR_ALLOWED works only when this and the remote key is_enable_consent_shortcut are both on; CACHED_EARLY needs neither (ADR-0038)."
- `app/src/staging/.../StagingAdsModule.kt`: KDoc "The build half of the ERROR_ALLOWED switch is on, so testers can try ERROR_ALLOWED by switching is_enable_consent_shortcut on in the staging Firebase project; the remote key stays off by default (S-12). CACHED_EARLY needs no switch (ADR-0038)." Giá trị `ConsentShortcutConfig(enabledInBuild = true)` giữ.
- `app/src/production/.../ProductionAdsModule.kt`: không chạm (cấu hình production, `CLAUDE.md`, Không được làm). Chú thích "The consent shortcut is off in production builds (S-12)" vẫn đúng với nghĩa mới; giá trị `OFF` giữ, nên production có `CACHED_EARLY` mà không có `ERROR_ALLOWED`.
- Trang "Ad status" (`AdGalleryScreen.kt`, `strings.xml` của staging): không đổi. Dòng "Consent shortcut (remote)" chỉ đọc khóa từ xa, không nói khóa điều khiển `CACHED_EARLY`; dòng "Temporary right" vẫn đúng ở `CACHED_EARLY` và `ERROR_ALLOWED` (câu hỏi 3).

**Bước 4. Tài liệu luật (ADR-0038, "Việc AI phải tuân thủ": mỗi luật đổi ghi kèm mã ADR và nguồn JBase).**

`docs/ops/ads-business-rules.md`:

- S-03 thành: "**Init sớm (`CACHED_EARLY`), luôn bật:** ngay sau khi gửi lượt hỏi, nếu UMP đã có đồng ý dùng được từ phiên trước thì khởi tạo SDK quảng cáo ngay, không chờ UMP trả lời; không cần cấu hình build hay công tắc từ xa. Lỗi của lượt hỏi sau đó không hạ quyền này (S-04, S-05); UMP báo cần hỏi lại thì đóng cổng (S-10). **Đổi 2026-10-29 (ADR-0038):** trước đây cần cả hai công tắc (S-12)". Cột nguồn thêm "JBase: `admobadshelper-release.aar`, `com.jb.admobadshelper.AdmobHelper.startConsentFlow` (`gatherConsent` rồi `canRequestAds()` thì `initializeMobileAdsSdk`)".
- S-06 thành: "**Chốt kết quả.** Công tắc `ERROR_ALLOWED` bật (S-12): chỉ chốt kết quả **sạch** (cập nhật thành công và form đóng không lỗi). Công tắc tắt: chốt khi kết quả sạch; bước cập nhật lỗi thì không chốt (đang có `CACHED_EARLY` thì giữ nó); form lỗi thì chỉ chốt khi quyền của UMP đã là "cho phép", kể cả khi đường đang là `CACHED_EARLY` (chốt thành `GRANTED`). Trạng thái công tắc đọc lúc có kết quả. Đã chốt thì các lượt hỏi sau trong process trả ngay, không gọi UMP. **Đổi 2026-10-29 (ADR-0038, T-066):** `CACHED_EARLY` có cả khi công tắc tắt".
- S-12 thành: "**Quyền tạm khi lỗi mạng** (`ERROR_ALLOWED`) chỉ có hiệu lực khi **cả** cấu hình lúc build của `:app` **và** công tắc từ xa `is_enable_consent_shortcut` (mục 10) cùng bật; **mặc định cả hai tắt**. Tắt thì lỗi của UMP không cho tải. `CACHED_EARLY` không theo hai công tắc này (S-03). Tắt công tắc giữa phiên thì thu hồi **ngay** quyền chỉ có nhờ `ERROR_ALLOWED`, không chốt kết quả (lượt hỏi sau theo luật công tắc tắt); quyền thật của UMP, kể cả `CACHED_EARLY`, giữ nguyên. Bật công tắc không bao giờ thu hồi gì. **Đổi 2026-10-29 (ADR-0038):** thay phần `CACHED_EARLY` của luật cũ". Cột nguồn giữ, thêm "ADR-0038".
- Mục 10, dòng `is_enable_consent_shortcut`, cột mô tả: "Công tắc từ xa của quyền tạm khi lỗi mạng `ERROR_ALLOWED` (S-12). Cần thêm cấu hình lúc build của `:app`, mặc định cũng tắt. Tắt thì thu hồi ngay `ERROR_ALLOWED`. `CACHED_EARLY` luôn bật, không theo khóa này (ADR-0038)".
- HD-04, mục 12 (L-A15) giữ nguyên: HD-04 vẫn cần cho S-06 và S-12; L-A15 vẫn đúng (chỉ chốt kết quả sạch khi công tắc bật; gộp lượt hỏi).

`docs/ops/ads-guide.md`:

- Đoạn "Với ADR-0026 (xem lại ở T-034)": "`CACHED_EARLY`, S-03" thành "`CACHED_EARLY`, S-03, luôn bật từ ADR-0038"; "và nhánh rút gọn mặc định tắt (S-12)" thành "và `ERROR_ALLOWED` mặc định tắt (S-12)". Kết luận về provider giữ (provider vẫn chạy cả lần đầu sau khi cài, khi chưa có đồng ý).
- Dòng "Hộp thoại đồng ý" của phần thử trên staging: "mở lại app sau khi đã đồng ý (`CACHED_EARLY`)" thêm "(luôn bật, không cần công tắc)"; "tắt mạng khi mở app (quyền tạm `ERROR_ALLOWED`)" thêm "(cần bật `is_enable_consent_shortcut` trên Firebase staging)".
- Đoạn màn hình xem thử, trang "Ad status": "công tắc từ xa `is_enable_consent_shortcut`" thành "công tắc từ xa `is_enable_consent_shortcut` (chỉ `ERROR_ALLOWED`)"; câu "thử: bật `is_enable_consent_shortcut` trên Firebase staging rồi mở lại app (`CACHED_EARLY`), hoặc xóa dữ liệu app, bật chế độ máy bay rồi mở (`ERROR_ALLOWED`)" thành "thử: đồng ý rồi mở lại app (`CACHED_EARLY`, không cần công tắc); hoặc bật `is_enable_consent_shortcut` trên Firebase staging, xóa dữ liệu app, bật chế độ máy bay rồi mở (`ERROR_ALLOWED`)".

Spec không sửa: KB-11 tiêu chí 6, KB-32 tiêu chí 4 và mục 8 đã ghi đổi ngày 2026-10-29.

**Thứ tự commit.** Bước 3 của phiên (test viết trước): các test mới và câu khẳng định viết lại ở mục 4 và 3.5, trong `ConsentGateTest.kt`, `ConsentCoordinatorTest.kt`, `AdsSettingsTest.kt`. Bước 4 (code): `ConsentGate.kt`, `AdsSettings.kt`, chú thích, hai tài liệu khóa sinh lại, tài liệu luật. `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` đỏ sau khi đổi `AdsSettings.kt` cho tới khi sinh lại hai tài liệu trong cùng commit code (L-025).

**Các điều của "Khi viết plan" áp dụng cho plan này:** danh sách file đầy đủ; file nơi khác đọc (`remote-config-keys.md`, `remote-config-template.json`, L-020); mục 3.5 (sửa Kotlin ngoài test); mỗi tiêu chí có test; không phải lỗi giao diện hay lỗi đo được (không cần số đo); E là 0 (không cần chạy thử); quyền và thư viện quyết ở 3.3; rủi ro có lý do từng tiêu chí; `token_estimate`; kiểm tay ghi nơi chạy; bước đối chứng cho test (L-055); mọi đường mở cổng (L-072); cờ tắt giữa chừng (L-061, có test); không thêm bước chờ, không thêm trạng thái ở vỏ app, không đổi hàm `suspend` vỏ app gọi; không quét mã nguồn, không dịch chữ, không chép code từ JBase (chỉ dẫn lớp và hàm của bytecode).

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentGate.kt` | `onRequestSent` không đọc công tắc; nhánh chốt mới của `onFailure`; nhánh `else` không chốt; KDoc (bước 1) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/ConsentShortcutConfig.kt` | Chỉ KDoc: công tắc build của `ERROR_ALLOWED` |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/template/core/ads/AdsSettings.kt` | Mô tả của `IS_ENABLE_CONSENT_SHORTCUT` (bước 3) |
| Sửa | `app/src/staging/kotlin/com/vglstudio/template/di/StagingAdsModule.kt` | Chỉ KDoc của `consentShortcutConfig` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentGateTest.kt` | Test mới của mục 4; dữ liệu của `shortcutOff_networkFailureGivesNothing` và khối thêm của `reset_backToInitialKeepsShortcut` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/ConsentCoordinatorTest.kt` | Test mới của mục 4; `cachedConsent_shortcutOff_waitsForUpdate` thành `cachedConsent_shortcutOff_opensBeforeUpdateReturns` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdsSettingsTest.kt` | Test mô tả khóa `consentShortcut_describesErrorAllowedOnly` |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: mô tả của `is_enable_consent_shortcut` |
| Sửa | `docs/remote-config-template.json` | Sinh lại: mô tả của `is_enable_consent_shortcut` |
| Sửa | `docs/ops/ads-business-rules.md` | S-03, S-06, S-12, mục 10 dòng `is_enable_consent_shortcut` (bước 4) |
| Sửa | `docs/ops/ads-guide.md` | Ba chỗ nhắc nhánh rút gọn và cách thử `CACHED_EARLY` (bước 4) |

Nơi khác đọc file plan sửa (L-020, `scripts/plan-check.py`):

- `app/build.gradle.kts` chỉ truyền đường dẫn của `docs/remote-config-keys.md`, `docs/remote-config-template.json` cho task test và khai báo chúng là đầu vào; không đọc nội dung. Không sửa.
- `app/src/test/kotlin/com/vglstudio/template/config/RemoteConfigKeysTest.kt` so hai tài liệu đó với code (`keysFile_matchesCode`, `templateFile_matchesCode`) và ghi lại chúng khi chạy với `-PremoteConfigKeys.update=true`: đây chính là cách sinh lại ở mục 3.1, bước 3. Test không sửa; hai tài liệu sinh lại trong commit code.
- `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdFormatTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/AdTelemetryRulesTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/FullScreenAdGuardTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/LoadAttemptCounterTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardScreenshotTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/template/core/ads/NativeAdCardTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerEventsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSettingsTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerSwitchesTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/AppsFlyerTrackerTest.kt`, `core/analytics/src/test/kotlin/com/vglstudio/template/core/analytics/FirebaseAdMetricsTest.kt` chỉ nhắc `docs/ops/ads-business-rules.md` trong chú thích (mục 4, 5.7, 8, 10.1, MC-02), không mở file; plan chỉ đổi S-03, S-06, S-12 và một dòng của mục 10 (`is_enable_consent_shortcut`), không phải mục chúng dẫn. Không sửa.

Không có ảnh chuẩn nào đổi: không đổi giao diện, chuỗi hay dữ liệu của test ảnh. Không có tệp kịch bản emulator: trình chạy kịch bản chỉ mở app bằng `fresh` (xóa dữ liệu), không mở lại app mà giữ đồng ý đã lưu, nên không tái hiện được `CACHED_EARLY` (mục 4).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: không thêm. Khóa `is_enable_consent_shortcut` giữ tên, kiểu, mặc định `false`; chỉ đổi nghĩa (chỉ còn `ERROR_ALLOWED`) và mô tả.
- Quảng cáo: không thêm, bớt hay di chuyển vị trí, không đổi định dạng hay tần suất. Đổi thời điểm mở cổng đồng ý và khởi tạo SDK quảng cáo (điểm chặn 5): ở mọi bản, người đã có đồng ý dùng được từ phiên trước thì SDK khởi tạo ngay sau khi gửi lượt hỏi. Ở production đây là hành vi mới (hôm nay cấu hình build tắt nên luôn chờ UMP trả lời). Mất công tắc tắt khẩn từ xa cho phần này (ADR-0038, Hệ quả).
- Sự kiện phân tích: không thêm, không đổi sự kiện. AppsFlyer bắt đầu theo vế (a) của `awaitAttributionStart` (SDK quảng cáo đã khởi tạo), nên với người đã đồng ý ở phiên trước, AppsFlyer bắt đầu sớm hơn (cùng lúc với `CACHED_EARLY`) thay vì chờ kết quả của lượt hỏi. Đồng ý dùng là đồng ý thật của UMP, như ADR-0027 quyết định 3 cho phép.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. Code chỉ đổi điều kiện trong `ConsentGate` (Kotlin thuần); test dùng JUnit, MockK, `RecordingAppLogger`, `FakeConsentPlatform` đã có trên classpath test của `:core:ads`.

### 3.4. Phương án dự phòng

Không có. Đổi một điều kiện và thêm một nhánh trong máy trạng thái thuần đã có test; không có điểm kỹ thuật chưa chắc.

### 3.5. Hành vi đã có bị đổi

`scripts/existing-tests.py` cho plan này (chạy 2026-10-29) liệt kê 108 test dùng tên của bốn file Kotlin; kết quả ở dòng cuối của bảng. Đã đọc hết `ConsentGateTest` (20 test), `ConsentCoordinatorTest` (26 test), các test của `AdsSettingsTest` và `RemoteConfigKeysTest` nhắc khóa này, `GalleryAdStatusTest`, `AdGalleryScreenTest.status_showsConsentAndSwitches`. Các test dựng `AdsController` (`AdsControllerFixtures.testAdsController`) dùng UMP thật qua Robolectric và không gọi `gatherConsent`, nên không đi qua `onRequestSent`.

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Công tắc tắt: UMP đã có đồng ý lúc gửi lượt hỏi thì cổng vẫn đóng tới khi UMP trả lời | `onRequestSent` không đọc công tắc: `CACHED_EARLY`, cổng mở, SDK khởi tạo một lần (bước 1) | `ConsentCoordinatorTest.cachedConsent_shortcutOff_waitsForUpdate`: `assertFalse(env.state.allowed)`, `assertTrue(env.opened.isEmpty())` ngay sau `gather` | Đổi tên thành `cachedConsent_shortcutOff_opensBeforeUpdateReturns`. Ý định: "công tắc tắt, người đã đồng ý ở phiên trước thì cổng mở đúng một lần và kết quả cuối là `GRANTED`". Câu mới thay hai câu trên: `assertEquals(ConsentPath.CACHED_EARLY, env.state.path)`, `assertTrue(env.state.allowed)`, `assertEquals(listOf(true), env.opened)`. Phần sau giữ nguyên: sau `completeUpdate()`, `closeForm(null)` thì `GRANTED`, `allowed`, `assertEquals(listOf(true), env.opened)` (nay khẳng định SDK không khởi tạo lần hai). Thêm `assertTrue(env.state.settled)` ở cuối. Đối chứng: tạm đặt lại vế `shortcut &&` trong `onRequestSent` thì `CACHED_EARLY` đỏ |
| Công tắc tắt: `onRequestSent(true)` không mở; lỗi mạng sau đó không mở | Có `CACHED_EARLY` thì lỗi của bước cập nhật giữ nó (bước 1, bảng chốt) | `ConsentGateTest.shortcutOff_networkFailureGivesNothing`, khối đầu: `onRequestSent(umpCanRequestAds = true)` rồi `assertFalse(allowed)` hai lần | Ý định: "công tắc tắt thì lỗi mạng không bao giờ cho `ERROR_ALLOWED`". Giữ mọi câu khẳng định; đổi dữ liệu của khối đầu thành `onRequestSent(umpCanRequestAds = false)` và `onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)`, để khối kiểm đúng ý định khi không có đồng ý cũ. Trường hợp có đồng ý cũ chuyển sang test mới `cachedConsent_switchOff_keptThroughUpdateFailure` (mục 4), test này khẳng định `path != ERROR_ALLOWED`. Đối chứng ở cuối test (công tắc bật thì mở) giữ |
| Sau `onReset`, `CACHED_EARLY` chứng minh công tắc còn bật | `CACHED_EARLY` không đọc công tắc nên câu cũ vẫn qua nhưng không còn chứng minh công tắc được giữ | `ConsentGateTest.reset_backToInitialKeepsShortcut`: `onRequestSent(true)` rồi `assertEquals(CACHED_EARLY, path)` | Giữ câu cũ (nay khẳng định `CACHED_EARLY` có lại sau khi đặt lại); đổi chú thích thành "CACHED_EARLY opens early again after a reset". Thêm khối mới: gate thứ hai `gate(shortcut = true)`, chốt `GRANTED`, `onReset()`, `onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)` thì `assertEquals(ConsentPath.ERROR_ALLOWED, path)`; đối chứng `gate(shortcut = false)` cùng bước thì `assertNotEquals(ConsentPath.ERROR_ALLOWED, path)` |
| Mô tả khóa: "Remote switch of the consent shortcut (CACHED_EARLY, ERROR_ALLOWED)..." | Mô tả mới (bước 3) | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` (so tài liệu sinh với code); `AdsSettingsTest.keysNamedByTemplate_areMarked`, `consentShortcut_isOffByDefault` | Giữ: hai tài liệu sinh lại trong commit code; mô tả mới vẫn có câu "not an AdsKit key" do `booleanKey` thêm; tên, kiểu, mặc định không đổi |
| Luật chốt khi không có `CACHED_EARLY`; `CACHED_EARLY` khi công tắc bật; thu hồi `ERROR_ALLOWED`; `ERROR_ALLOWED` ở nền; hỏi lại | Không đổi (bảng chốt, bước 1) | `ConsentGateTest`: `cachedConsent_shortcutOn_opensAtRequestSent`, `shortcutActive_needsBoth`, `networkFailure_shortcutOn_allowsWithoutSettling`, `networkFailure_afterCachedEarly_keepsCachedEarly`, `errorAllowed_background_mayNotRequest`, `temporaryThenRequired_closesAtUpdate`, `shortcutTurnedOff_revokesErrorAllowedOnly`, `turningOn_revokesNothing`, `shortcutOff_formErrorAfterGrant_settles`, `shortcutOff_formErrorWithoutGrant_doesNotSettle`, `shortcutOn_formError_doesNotSettle`, `settleRule_readsShortcutAtResultTime`, `updateFailure_neverSettles`, `configurationError_givesNoTemporaryRight`, `interruptedForm_isNotSettled`, `privacyForm_withdrawOrGrant`, `initialState_isClosedWithNoPath`, `umpFailureOf_mapsCodes`. `ConsentCoordinatorTest`: `cachedConsent_shortcutOn_opensBeforeUpdateReturns`, `temporaryThenRequired_closesBeforeForm`, `settled_nextGatherSkipsUmp`, `shortcutOn_formError_doesNotSettle`, các test `errorAllowed_*`, `regather_*`, `gatherWaitsForShortcutValue`, `beforeStart_gatesClosedAndSignalsReadable`, `finished_atFinalResult_clearedByReset`, `interruptedForm_regathersNextTime`, `requiredForm_holdsSuppressionWhileOpen`, `consentFormOpen_trueOnlyWhileFormShows`, `secondGatherWhileRunning_sharesTheFirst`, `gather_updatesThenShowsFormThenReads`, `start_twice_isIgnored` | Giữ. Đã soát từng test: đầu vào của chúng hoặc bật công tắc, hoặc để `canRequestAds` sai lúc `gather` (mặc định của `FakeConsentPlatform`), nên không đi qua nhánh mới. `updateFailure_neverSettles` có vòng công tắc tắt nhưng không gọi `onRequestSent`, vẫn không chốt |
| Công tắc tắt, `canRequest = true` trước `gather` (`Env.granted()`, `outsideRegulatedRegion_grantsWithoutForm`) | Đường đi qua `CACHED_EARLY` trước `GRANTED`; cổng mở ở `gather` thay vì ở lúc đóng form | `ConsentCoordinatorTest`: `outsideRegulatedRegion_grantsWithoutForm`, `privacyForm_alwaysHoldsSuppression`, `privacyFormWithdrawn_closesEveryGate`, `privacyFormRegranted_initsOrPreloads`, `reset_onlyWhenAllowed`, `everyPath_isLogged` | Giữ. Các câu kiểm trạng thái cuối (`GRANTED`, `settled`, `allowed`), số lần `onGateOpened` (`[true]`, rồi `[true, false]`: cổng mở một lần ở `CACHED_EARLY`, `GRANTED` không mở lại), số lần gọi UMP. `everyPath_isLogged` lọc log theo tên từng đường, nên dòng `Consent path: CACHED_EARLY` thêm vào môi trường `granted` không làm số dòng `GRANTED` khác 1 |
| Trang "Ad status": đường đồng ý, quyền tạm, công tắc | Không đổi | `GalleryAdStatusTest.*`, `AdGalleryScreenTest.status_showsConsentAndSwitches`, ảnh `AdGalleryScreen_status_light.png`, `_small.png` | Giữ: trang chỉ đọc luồng công khai và khóa từ xa, không đọc điều kiện của `ConsentGate` |
| Ba test "Cần xem trước" của `existing-tests.py`: `CollapsibleNativeAdTest.layer_smallScreen_fitsOnScreen`, `NativeAdCardTest.mediumPlaceholder_ctaIs44dp`, `RewardedActionTest.backgroundWhileDecisionWaits_timeout_closesOnce` | Không đổi | Chúng đo kích thước hay thời gian chờ và chỉ dùng `AdsSettings.all`, `placementEnabled` để dựng giá trị Remote Config | Giữ: plan không đổi kích thước, khóa nào khác hay danh sách `all` |
| Các test còn lại của danh sách `existing-tests.py` (`AdPlacementTest`, `AdsFlagsTest`, `AdsSettingsTest` khác, `RewardedActionTest`, `NativeAdCardTest`, `CollapsibleNativeAdTest`, `AdTelemetryTest`, `AdsControllerNativeComboTest`, `AdsControllerResumeTest`, `AdGalleryViewModelTest`, `StagingRemoteValuesModuleTest`, `RemoteConfigKeysTest` khác) | Không đổi | Chúng dùng `AdsSettings` (`all`, `placementEnabled`, `nativeEvery`, `keysFor`) hay `ConsentPath`, không dùng mô tả của khóa này hay điều kiện của `ConsentGate` | Giữ |

Câu khẳng định đổi: hai câu của `cachedConsent_shortcutOff_waitsForUpdate` (đổi tên test) và dữ liệu của một khối trong `shortcutOff_networkFailureGivesNothing`. Đây là câu viết sẵn theo ADR-0031, quyết định 1: phiên đưa chúng vào commit test của bước 3 và dùng như test viết trước (câu mới của `cachedConsent_shortcutOff_opensBeforeUpdateReturns` đỏ trên code hiện tại vì `path` là `null`). Tổng cộng ba chỗ, trong giới hạn ba câu của điều 6. Câu khẳng định cũ khác mà đỏ vì đường `CACHED_EARLY` mới thì phiên không viết lại, mà dừng theo mục 7.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-11 tiêu chí 1, KB-32 tiêu chí 4 (S-03, máy trạng thái) | Unit test | `ConsentGateTest.cachedConsent_switchOff_opensAtRequestSent`: `gate(shortcut = false)`, `onRequestSent(umpCanRequestAds = true)`: `path == CACHED_EARLY`, `allowed`, `temporary`, không `settled`, `mayRequestNow(foreground = false)` đúng. Đối chứng (L-055): `gate(shortcut = false)`, `onRequestSent(umpCanRequestAds = false)`: `path` null, không `allowed`. Trên code hiện tại thất bại ở `path` (`null`) |
| KB-11 tiêu chí 1, KB-32 tiêu chí 4 (đường `gather`, SDK khởi tạo một lần) | Unit test | `ConsentCoordinatorTest.cachedConsent_shortcutOff_opensBeforeUpdateReturns` (viết lại từ `cachedConsent_shortcutOff_waitsForUpdate`, mục 3.5): `started(shortcut = false)`, `canRequest = true`, `gather(A)`: `CACHED_EARLY`, `allowed`, `opened == [true]` trước `completeUpdate()`; sau `completeUpdate()`, `closeForm(null)`: `GRANTED`, `settled`, `opened` vẫn `[true]`. Thất bại trên code hiện tại vì `path` null |
| KB-11 tiêu chí 1 khi chưa biết công tắc (HD-04) | Unit test | `ConsentCoordinatorTest.gatherWaitsForShortcutValue_thenCachedEarlyWithSwitchOff`: `start()`, `canRequest = true`, `gather(A)` trước `onShortcutChanged`: 0 lượt UMP, `opened` rỗng; `onShortcutChanged(false)`: 1 lượt, `CACHED_EARLY`, `opened == [true]`. Thất bại trên code hiện tại ở `CACHED_EARLY`. Test cũ `gatherWaitsForShortcutValue` giữ (công tắc bật, `ERROR_ALLOWED`) |
| KB-11 tiêu chí 6 (công tắc tắt: lỗi không cho `ERROR_ALLOWED`, `CACHED_EARLY` giữ) | Unit test | `ConsentGateTest.cachedConsent_switchOff_keptThroughUpdateFailure`: cho mỗi `UmpFailure.NETWORK`, `UmpFailure.CONFIGURATION`: `gate(shortcut = false)`, `onRequestSent(true)`, `onUpdateFailed(failure, true)`: `path == CACHED_EARLY`, `path != ERROR_ALLOWED`, `allowed`, không `settled`, `regatherDue` sai. Đối chứng: cùng bước không có `onRequestSent(true)` (bắt đầu từ `INITIAL`): không `allowed`, `path != ERROR_ALLOWED`. Thất bại trên code hiện tại ở `allowed` |
| KB-11 tiêu chí 6 trên đường `gather` (không hỏi lại theo S-09, lượt sau hỏi lại UMP) | Unit test | `ConsentCoordinatorTest.cachedConsent_switchOff_updateFails_keepsAdsAndAsksNextGather`: `started(shortcut = false)`, `canRequest = true`, `gather(A)`, `failUpdate(INTERNET_ERROR)`: `CACHED_EARLY`, `allowed`, `finished` đúng, không `settled`; `onInternetChanged(false)` rồi `true`: số lượt UMP vẫn 1 (không có hỏi lại của `ERROR_ALLOWED`); `gather(A)` lần hai: số lượt UMP 2. Đối chứng: `errorAllowed()` (công tắc bật, không có đồng ý cũ) cùng sự kiện mạng: số lượt 2. Thất bại trên code hiện tại ở `allowed` |
| KB-11 tiêu chí 5 khi công tắc tắt (S-10) | Unit test | `ConsentGateTest.cachedConsent_switchOff_thenRequired_closesAtUpdate`: `gate(shortcut = false)`, `onRequestSent(true)`: `allowed`; `onUpdateSucceeded(false)`: không `allowed`, `path == DENIED_OR_REQUIRED`, không `temporary`. Thất bại trên code hiện tại ở `allowed` sau `onRequestSent`. S-11 (không hiện quảng cáo đã nạp khi cổng đóng) đọc `allowed` của cùng cổng, đã có test ở T-020 và T-022, không đổi |
| KB-11 tiêu chí 7 (tắt công tắc chỉ thu hồi `ERROR_ALLOWED`; `CACHED_EARLY` vẫn mở ở lần mở sau) | Unit test | `ConsentGateTest.switchOff_revokesErrorAllowed_nextOpenStillCachedEarly`: phiên 1 `gate(shortcut = true)`, `onUpdateFailed(NETWORK, false)`: `ERROR_ALLOWED`, `allowed`; `onShortcutChanged(false)`: không `allowed`. Phiên 2 (gate mới, `onShortcutChanged(false)` như giá trị đã lưu): `onRequestSent(true)`: `CACHED_EARLY`, `allowed`; `onShortcutChanged(true)` rồi `false`: vẫn `CACHED_EARLY`, `allowed`. Thất bại trên code hiện tại ở phiên 2. Test cũ `shortcutTurnedOff_revokesErrorAllowedOnly`, `turningOn_revokesNothing` giữ |
| KB-11 tiêu chí 8 (công tắc tắt, form lỗi sau khi đã cho phép, có `CACHED_EARLY`: chốt cho cả lần mở) | Unit test | `ConsentGateTest.cachedConsent_switchOff_formErrorAfterGrant_settles`: `gate(shortcut = false)`, `onRequestSent(true)`: `CACHED_EARLY` (thất bại trên code hiện tại ở đây); `onUpdateSucceeded(true)`, `onFormFinished(UmpFailure.CONFIGURATION, true)`: `path == GRANTED`, `settled`, `allowed`, không `temporary`. Cùng với `UmpFailure.NETWORK`. Đối chứng: `gate(shortcut = true)` cùng bước: `CACHED_EARLY`, không `settled` |
| KB-11 tiêu chí 8 trên đường `gather` (lượt sau không gọi UMP) | Unit test | `ConsentCoordinatorTest.cachedConsent_switchOff_formError_settledNextGatherSkipsUmp`: `started(shortcut = false)`, `canRequest = true`, `gather(A)`: `CACHED_EARLY`, `opened == [true]`; `completeUpdate()`, `closeForm(INTERNAL_ERROR)`: `GRANTED`, `settled`, `opened` vẫn `[true]`; `gather(A)` lần hai: số lượt UMP vẫn 1, `finished` đúng. Đối chứng: `started(shortcut = true)` cùng bước: không `settled`, lần hai gọi UMP (2). Thất bại trên code hiện tại ở câu `CACHED_EARLY` ngay sau `gather` (code hiện tại chỉ mở cổng lúc đóng form) |
| Đặt lại giữ công tắc (S-18, ý định của test cũ) | Unit test | Khối thêm của `ConsentGateTest.reset_backToInitialKeepsShortcut` (mục 3.5): sau `onReset`, lỗi mạng ra `ERROR_ALLOWED` khi công tắc bật, không ra khi tắt |
| Mô tả khóa `is_enable_consent_shortcut` | Unit test | `AdsSettingsTest.consentShortcut_describesErrorAllowedOnly`: `description` bắt đầu bằng `"Remote switch of ERROR_ALLOWED"`, có `"CACHED_EARLY is always on"`, không có `"(CACHED_EARLY, ERROR_ALLOWED)"`. Thất bại trên code hiện tại ở câu đầu. `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` (có sẵn) qua sau khi sinh lại hai tài liệu |
| Luật cũ giữ nguyên | Unit test có sẵn | Các test của dòng "Không đổi" ở mục 3.5, chạy trong `scripts/check.sh` |
| KB-11 tiêu chí 1 trên máy, công tắc tắt (staging, EEA giả) | Kiểm tra thủ công (máy thật: Remote Config; cần `is_enable_consent_shortcut` tắt trên Firebase staging, và mở lại app mà giữ dữ liệu, điều trình chạy kịch bản emulator chưa có) | Bản staging, `is_enable_consent_shortcut` tắt (mặc định): (1) xóa dữ liệu, mở app, đồng ý: log `Consent path: GRANTED`. (2) Vuốt bỏ app, mở lại: log `Consent path: CACHED_EARLY` trước `Consent update done`, có dòng `Adapter ...` (SDK khởi tạo) trước `Consent update done`; trang "Ad status" ghi `CACHED_EARLY` rồi `GRANTED`, "Temporary right: yes" rồi "no". (3) Bật chế độ máy bay, mở lại (không xóa dữ liệu): `CACHED_EARLY`, không có `ERROR_ALLOWED`. (4) Xóa dữ liệu, vẫn chế độ máy bay, mở: không có quảng cáo, không có `CACHED_EARLY` hay `ERROR_ALLOWED`. (5) Bật `is_enable_consent_shortcut` trên Firebase staging, lặp bước 4: `ERROR_ALLOWED`. Lệnh: `adb logcat -v threadtime \| grep ' VGL\.Ads'` |
| Tài liệu luật và hướng dẫn | Kiểm tra thủ công (người: đọc tài liệu) | Người duyệt PR đọc S-03, S-06, S-12, mục 10 của `ads-business-rules.md` và ba chỗ của `ads-guide.md` khớp mục 3.1, bước 4 |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-085). Không có kịch bản emulator: lệnh `fresh` xóa dữ liệu nên không có đồng ý của phiên trước; hai test JVM của đường `gather` thay phần kiểm được.

**Ngoại lệ viết test trước:** (1) khối thêm của `reset_backToInitialKeepsShortcut` qua ngay trên code hiện tại: nó giữ ý định của test cũ (đặt lại giữ công tắc), không kiểm hành vi mới; đối chứng là vế công tắc tắt. (2) Chú thích KDoc, mô tả khóa trong tài liệu sinh lại và hai tài liệu luật: không có hành vi; mô tả khóa có test `consentShortcut_describesErrorAllowedOnly`.

### 4.1. Chạy thử

Không cần (E dưới 2).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 5 (hộp thoại đồng ý, thứ tự khởi tạo SDK: SDK quảng cáo và AppsFlyer khởi tạo trước khi UMP trả lời ở mọi bản, kể cả production), điểm 7 (module core `:core:ads`). Plan rủi ro cao chạm đồng ý: anh Nhật (người phụ trách sản phẩm của spec `ads`) cần biết trước khi duyệt (`risk-scoring.md`, mục 3); anh Nhật đã chọn hành vi này ở ADR-0038, nhưng plan vẫn nêu ở câu hỏi 6 hệ quả ở production.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Logic đổi trong một module (`ConsentGate` của `:core:ads`), không đổi chữ ký công khai; `:app` chỉ đổi chú thích; thêm hai tài liệu sinh và hai tài liệu luật |
| B. Độ rõ của yêu cầu | 2 | Spec `confirmed`, ADR-0038 rõ phần `CACHED_EARLY`. Luật chốt khi có `CACHED_EARLY` mà công tắc tắt, và việc giữ `CACHED_EARLY` qua lỗi của bước cập nhật, do AI đề xuất (câu hỏi 1, 2): ảnh hưởng tới cách làm |
| C. Khả năng kiểm chứng tự động | 1 | Mọi luật có unit test JVM với UMP giả; phần trên máy (UMP thật, đồng ý đã lưu, Remote Config) kiểm tay trên staging |
| D. Ảnh hưởng chéo | 3 | Đổi thời điểm khởi tạo SDK quảng cáo, AppsFlyer, thời điểm màn loading thấy SDK sẵn sàng (T-024) ở production cho mọi người đã đồng ý; T-053, T-067 cùng sửa `ads-business-rules.md`, T-053 cùng sửa `AdsSettings.kt` và hai tài liệu khóa sinh |
| E. Độ mới của kỹ thuật | 0 | Lặp lại đúng mẫu máy trạng thái thuần và test của T-020 |
| **Tổng** | 8 | **Mức: Cao** (chạm điểm chặn 5 và 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `ConsentGate` (cổng đồng ý của mọi quảng cáo, nạp sẵn, AppsFlyer); `AdsSettings` và hai tài liệu khóa sinh (`docs/remote-config-keys.md`, `docs/remote-config-template.json`, mà app tạo từ dự án mẫu nhập vào Firebase); `docs/ops/ads-business-rules.md`, `docs/ops/ads-guide.md`. Màn loading (`:feature:loading`, T-024) đọc `consentPath`, `sdkReady`, `consentFlowFinished`: không cần sửa, nhưng với người đã đồng ý, SDK sẵn sàng sớm hơn nên quảng cáo mở app có thể nạp xong sớm hơn trong khung chờ của màn loading.
- Task khác trong sprint có thể xung đột: T-053 (đếm ngược và bung lại theo JBase: `AdsSettings.kt`, mục 10 của `ads-business-rules.md`, hai tài liệu khóa sinh); T-067 (banner: B-01 của `ads-business-rules.md`); T-068 tới T-073 (MAX, ADR-0037) có thể chạm cổng đồng ý cho MAX, ngoài phạm vi ticket này. Xung đột chỉ ở chữ của tài liệu và tệp sinh: task chạy sau sinh lại hai tài liệu khóa bằng lệnh ở mục 3.1, bước 3.
- Đề xuất thứ tự chạy: độc lập; xếp trước các task MAX nếu chúng đọc `ConsentGate`, để chúng dựa trên luật mới.

## 7. Điều kiện dừng riêng

- Cần sửa `ProductionAdsModule.kt` hay bất kỳ file nào trong `app/src/production/` để đạt tiêu chí: dừng (cấu hình production, `CLAUDE.md`, Không được làm).
- Một test có sẵn ngoài các dòng đổi của mục 3.5 đỏ vì đường `CACHED_EARLY` mới (ví dụ một test kỳ vọng cổng đóng khi công tắc tắt mà plan không nêu): không viết lại theo ADR-0031; dừng theo điều kiện 3 để người quyết, vì đó là luật đồng ý.
- Lệnh sinh lại tài liệu khóa đổi dòng của khóa khác ngoài `is_enable_consent_shortcut`: dừng (bảng khóa sinh tự động lệch với code của task khác).

## 8. Câu hỏi cho người duyệt

1. **Luật chốt khi công tắc tắt mà đường đang là `CACHED_EARLY` và form lỗi sau khi UMP đã cho phép.** Hôm nay nhánh này không xảy ra (`CACHED_EARLY` cần công tắc bật). Code hiện tại sẽ giữ `CACHED_EARLY`, không chốt, lượt `gather` sau hỏi lại UMP. Đề xuất của AI: chốt `GRANTED` như mọi form lỗi sau khi đã cho phép khi công tắc tắt (KB-11 tiêu chí 8: "kết quả được giữ cho cả lần mở app, không hỏi lại"); công tắc tắt nghĩa là không có `ERROR_ALLOWED`, nên không có gì cần hỏi lại. Phương án khác: giữ `CACHED_EARLY` không chốt (ít code hơn, nhưng trái chữ của tiêu chí 8).
2. **Công tắc tắt, có `CACHED_EARLY`, bước cập nhật lỗi (mạng hay cấu hình).** Đề xuất của AI: giữ `CACHED_EARLY` (cổng mở, quảng cáo ở nền vẫn được vì là đồng ý thật, không chốt, không hỏi lại theo S-09; lần tạo activity sau gọi `gather` thì hỏi lại UMP), như S-04, S-05 đã làm khi công tắc bật và như JBase. Việc này cũng trả lời câu hỏi ở nhật ký T-020 (người đã đồng ý mở app lúc mất mạng ở production nay có quảng cáo). Phương án khác: đóng cổng khi công tắc tắt (giữ chữ cũ "lỗi mạng của việc hỏi đồng ý không cho tải" cho cả `CACHED_EARLY`), nhưng khi đó `CACHED_EARLY` mở rồi đóng lại trong vài giây lúc mất mạng.
3. **Trang "Ad status" giữ dòng "Consent shortcut (remote)".** Dòng này chỉ hiện giá trị khóa, không nói khóa điều khiển `CACHED_EARLY`, nên plan không đổi chữ. Nếu anh muốn đổi nhãn (ví dụ "ERROR_ALLOWED switch (remote)") thì cần sửa `strings.xml` của staging, một câu của `AdGalleryScreenTest.status_showsConsentAndSwitches` và ghi lại hai ảnh `AdGalleryScreen_status_light.png`, `AdGalleryScreen_status_small.png`; plan sẽ lên phiên bản 2. Đề xuất của AI: giữ.
4. **Giữ tên trong code** (`shortcut`, `onShortcutChanged`, `shortcutActive`, `ConsentShortcutConfig`, khóa `is_enable_consent_shortcut`). Đổi tên khóa từ xa làm mất giá trị đã đặt trên Firebase của app đã tạo; đổi tên lớp chỉ làm diff rộng hơn. Đề xuất của AI: giữ tên, sửa chú thích và mô tả.
5. **HD-04 giữ:** lượt hỏi đầu vẫn chờ giá trị đầu của công tắc, vì `ERROR_ALLOWED` và luật chốt đọc nó. Giá trị đầu là bản đã lưu hay mặc định, có ngay khi `AdsController.start` chạy, nên `CACHED_EARLY` không chậm thêm. Đề xuất của AI: giữ, không đổi `ConsentCoordinator`.
6. **Hệ quả ở production, cần anh Nhật biết trước khi duyệt (`risk-scoring.md`, mục 3):** người đã có đồng ý dùng được ở phiên trước (kể cả người chạm "Do not consent" mà UMP vẫn cho quảng cáo không cá nhân hóa, FB-20261025-01) sẽ có SDK quảng cáo và AppsFlyer khởi tạo ngay khi mở app, trước khi UMP trả lời; nếu UMP sau đó báo cần hỏi lại thì cổng đóng ngay (S-10). Không còn công tắc từ xa để tắt riêng phần này (ADR-0038, Hệ quả). Anh xác nhận giữ như ADR-0038?

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này, trừ plan `lane: quick` sau khi dev đồng ý trong phiên: ghi "Duyệt trong phiên làm nhanh" kèm lời đồng ý (ADR-0034). -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
