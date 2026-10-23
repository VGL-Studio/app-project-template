package com.vglstudio.template.core.config

/**
 * Every feature flag of the app, declared in one place (ADR-0003).
 * Naming rule for remote keys: `feature_<name>_enabled`.
 * [defaultValue] is what the app uses until remote values are fetched. It is always `false`
 * (checked by a unit test): a feature stays hidden in production until it is approved.
 * [description] is one sentence in English, shown on Firebase and in docs/remote-config-keys.md (ADR-0023).
 * To see a feature while it is being built, enable it for the staging build only (see [LocallyEnabledFlags]).
 */
enum class FeatureFlag(val key: String, val description: String, val defaultValue: Boolean = false) {
    SAMPLE("feature_sample_enabled", "Shows the sample feature screen, the reference pattern for new features."),
    LOADING("feature_loading_enabled", "Shows the loading screen that prepares the app before the start screen."),
    ;

    /** The Remote Config key of this flag, listed with every other key of the app (ADR-0023). */
    val remoteKey: RemoteKey<Boolean> = RemoteKey.boolean(key, defaultValue, description)
}
