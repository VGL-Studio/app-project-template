package com.vglstudio.template.core.ads

/** A full-screen placement in the preload plan. */
internal class FullScreenPreloadSlot(
    private val placement: AdPlacement,
    private val enabled: () -> Boolean,
    private val state: () -> FullScreenLoadState,
    private val load: (AdPlacement) -> Unit,
) : PreloadSlot {
    override val format: AdFormat = placement.format

    override fun loadIfNeeded() {
        if (enabled()) load(placement)
    }

    override fun settled(): Boolean = true
}
