# BẢNG KHÓA REMOTE CONFIG

<!-- Sinh từ code bởi RemoteConfigKeysTest (:app). Không sửa tay. Sinh lại: ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true -->

Mọi khóa Remote Config của bản app hiện tại (ADR-0023). File mẫu để nhập vào Firebase: `remote-config-template.json`.

| Khóa | Kiểu | Mặc định | Module | Mô tả |
| --- | --- | --- | --- | --- |
| `feature_loading_enabled` | boolean | `false` | `:core:config` | Shows the loading screen that prepares the app before the start screen. |
| `feature_sample_enabled` | boolean | `false` | `:core:config` | Shows the sample feature screen, the reference pattern for new features. |
| `ads_enabled` | boolean | `true` | `:core:ads` | Master switch for every ad. When false, the app shows no ad at all. |
| `ads_first_session_grace_seconds` | long | `180` | `:core:ads` | Time without full-screen ads at the start of the first session after install, in seconds. |
| `ads_inlist_every_count` | long | `8` | `:core:ads` | Number of list items between two native ads inside a list. |
| `ads_interstitial_max_per_session_count` | long | `4` | `:core:ads` | Maximum number of full-screen ads shown in one session. |
| `ads_interstitial_min_interval_seconds` | long | `45` | `:core:ads` | Minimum time between two full-screen ads (interstitial and full-screen native), in seconds. |
| `ads_native_load_timeout_seconds` | long | `10` | `:core:ads` | Time to wait for a native ad before its space is removed when no ad is available, in seconds. |
| `ads_personalized_regulated_regions_enabled` | boolean | `true` | `:core:ads` | Allows personalized ads in regions that require consent (EEA, UK, Switzerland), for users who consented. Users who declined never get them. |
| `ads_sample_native_small_enabled` | boolean | `false` | `:core:ads` | Shows ads in the placement sample_native_small (native_small). Off by default. |
