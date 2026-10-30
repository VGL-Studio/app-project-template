# BẢNG KHÓA REMOTE CONFIG

<!-- Sinh từ code bởi RemoteConfigKeysTest (:app). Không sửa tay. Sinh lại: ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true -->

Mọi khóa Remote Config của bản app hiện tại (ADR-0023). File mẫu để nhập vào Firebase: `remote-config-template.json`.

| Khóa | Kiểu | Mặc định | Module | Mô tả |
| --- | --- | --- | --- | --- |
| `feature_loading_enabled` | boolean | `false` | `:core:config` | Shows the loading screen that prepares the app before the start screen. |
| `feature_sample_enabled` | boolean | `false` | `:core:config` | Shows the sample feature screen, the reference pattern for new features. |
| `ads_interval` | long | `45` | `:core:ads` | Minimum time between two full-screen ads of the default interval group, in seconds. |
| `ads_mediation_order` | string | `"max_admob"` | `:core:ads` | Order of the two ad platforms for banner, MREC, interstitial, rewarded and resume app open ads: admob, max, admob_max or max_admob; any other value uses max_admob. Without a MAX SDK key in the build, admob. Name set by the template, not an AdsKit key. |
| `is_enable_consent_shortcut` | boolean | `false` | `:core:ads` | Remote switch of ERROR_ALLOWED, the temporary right when UMP fails on the network (S-12); also needs the build setting of the app. CACHED_EARLY is always on (ADR-0038). False takes ERROR_ALLOWED back at once. Name set by the template, not an AdsKit key. |
| `is_log_ad_fullscreen_events` | boolean | `false` | `:core:ads` | Logs the Firebase lifecycle events of full-screen ads. Name set by the template, not an AdsKit key. |
| `is_log_ad_slot_events` | boolean | `false` | `:core:ads` | Logs the Firebase lifecycle events of ad slots (banner, native). Name set by the template, not an AdsKit key. |
| `is_show_ads` | boolean | `true` | `:core:ads` | Master switch for every ad. When false, the app loads and shows no ad at all. Name set by the template, not an AdsKit key. |
| `is_show_gallery_app_open` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_app_open (app_open). |
| `is_show_gallery_banner` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_banner (banner). |
| `is_show_gallery_banner_collapsible` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_banner_collapsible (banner_collapsible). |
| `is_show_gallery_in_collection` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_in_collection (native_in_collection). |
| `is_show_gallery_inlist_medium` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_inlist_medium (native_medium). |
| `is_show_gallery_inlist_small` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_inlist_small (native_small). |
| `is_show_gallery_interstitial` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_interstitial (interstitial). |
| `is_show_gallery_native_full_screen` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_native_full_screen (native_fullscreen). |
| `is_show_gallery_native_medium` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_native_medium (native_medium). |
| `is_show_gallery_native_small` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_native_small (native_small). |
| `is_show_gallery_native_step` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_native_step (native_step). |
| `is_show_gallery_native_step_full` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_native_step_full (native_fullscreen). |
| `is_show_gallery_rw_action` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Staging ad gallery only. Derived key of placement gallery_rw_action (rewarded). |
| `is_show_native_after_inter` | boolean | `false` | `:core:ads` | Shows a full-screen native ad right after an interstitial (combo). |
| `is_show_native_bottom` | boolean | `false` | `:core:ads` | Bottom slot of every bottom ad bar: a small native ad when true, a banner when false. |
| `is_show_native_collap` | boolean | `true` | `:core:ads` | Collapsible native layer on the bottom ad bar, only when is_show_native_bottom is also true. Name set by the template, not an AdsKit key. |
| `is_show_sample_native_small` | boolean | `false` | `:core:ads` | Shows ads in this placement. Off by default. Derived key of placement sample_native_small (native_small). |
| `native_every_gallery_in_collection` | long | `4` | `:core:ads` | Number of items between two native ads of this placement, in items. Derived key of placement gallery_in_collection (native_in_collection). |
| `native_every_gallery_inlist_medium` | long | `8` | `:core:ads` | Number of items between two native ads of this placement, in items. Derived key of placement gallery_inlist_medium (native_medium). |
| `native_every_gallery_inlist_small` | long | `8` | `:core:ads` | Number of items between two native ads of this placement, in items. Derived key of placement gallery_inlist_small (native_small). |
| `open_ad_type` | string | `"aoa"` | `:core:ads` | Ad shown when the app opens: inter for an interstitial, any other value for an app open ad. |
| `resume_ad_type` | string | `"aoa"` | `:core:ads` | Ad shown when the user comes back: only inter gives an interstitial, any other value an app open ad. |
| `show_open_ads` | boolean | `false` | `:core:ads` | Shows the ad when the app opens, from the second open on. |
| `show_open_ads_first_open` | boolean | `false` | `:core:ads` | Shows the ad when the app opens for the first time after install. |
| `show_resum_ads` | boolean | `false` | `:core:ads` | Shows an ad when the user comes back to the app. Name kept with the AdsKit typo. |
| `time_close_native` | long | `5` | `:core:ads` | Countdown before the full-screen native ad can be closed, in seconds; 1 to 30, any other value uses 5. Name from JBase, not an AdsKit key. |
| `time_expand_collap_ad` | long | `10` | `:core:ads` | Time after the user collapses the collapsible native ad before it expands again, in seconds; 0 means never. Name set by the template, not an AdsKit key. |
| `is_log_af_ad_revenue` | boolean | `true` | `:core:analytics` | Sends af_ad_revenue for every paid ad impression. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_inters_ad_eligible` | boolean | `true` | `:core:analytics` | Sends af_inters_ad_eligible. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_inters_api_called` | boolean | `true` | `:core:analytics` | Sends af_inters_api_called. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_inters_displayed` | boolean | `true` | `:core:analytics` | Sends af_inters_displayed. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_inters_displayed_n_times` | boolean | `true` | `:core:analytics` | Sends af_inters_displayed_{N}_times. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_native_ad_displayed` | boolean | `true` | `:core:analytics` | Sends af_native_ad_displayed. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_native_ad_eligible` | boolean | `true` | `:core:analytics` | Sends af_native_ad_eligible. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_native_api_called` | boolean | `true` | `:core:analytics` | Sends af_native_api_called. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_rewarded_ad_displayed` | boolean | `true` | `:core:analytics` | Sends af_rewarded_ad_displayed. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_rewarded_ad_eligible` | boolean | `true` | `:core:analytics` | Sends af_rewarded_ad_eligible. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `is_log_af_rewarded_api_called` | boolean | `true` | `:core:analytics` | Sends af_rewarded_api_called. AppsFlyer event switch. Name set by the template, not an AdsKit key. |
| `network_lost_delay_seconds` | long | `2` | `:core:network` | Seconds without usable Internet before the network dialog shows (ADR-0024). |
