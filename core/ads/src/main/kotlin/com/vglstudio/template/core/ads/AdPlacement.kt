package com.vglstudio.template.core.ads

/**
 * Every ad placement of the app, declared in one place (ADR-0004).
 * A placement is a business decision: it must come from an approved spec.
 * Adding, removing or moving a placement is a high-risk change (risk-scoring.md, blocking point 1).
 */
enum class AdPlacement(val key: String) {
    SAMPLE_BOTTOM_BANNER("sample_bottom_banner"),
}
