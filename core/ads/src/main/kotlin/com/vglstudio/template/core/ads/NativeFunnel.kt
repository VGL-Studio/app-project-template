package com.vglstudio.template.core.ads

// SKELETON (T-033, step 3).
internal class NativeFunnel(
    private val telemetry: AdTelemetry,
    private val format: AdFormat,
    private val unitId: String,
) {
    fun onLoaded(adapterClassName: String?) = Unit

    fun onImpression() = Unit
}
