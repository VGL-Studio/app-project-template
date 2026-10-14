package com.vglstudio.template.core.config

/**
 * Every feature flag of the app, declared in one place (ADR-0003).
 * Naming rule for remote keys: `feature_<name>_enabled`.
 * [defaultValue] is what the app uses until remote values are fetched; new features default to off.
 */
enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    SAMPLE("feature_sample_enabled", defaultValue = true),
}
