package com.vglstudio.template.core.ads

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.core.designsystem.theme.Spacing
import com.vglstudio.template.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "Ads"

/**
 * Ads of one in-list placement on one screen. Create it with [rememberInListAds], outside the lazy list, so an ad
 * outlives the list row that shows it: scrolling a row away and back shows the same ad, never a new request
 * (ads-guide.md, section 6). Every ad is destroyed when the screen leaves the composition.
 */
@Stable
class InListAds internal constructor(
    val placement: AdPlacement,
    every: State<Int?>,
    private val loader: InListAdLoader?,
    private val scope: CoroutineScope,
    private val logger: AppLogger?,
) {
    private val everyState = every

    // Touched on the main thread only (composition and the screen's coroutine scope).
    private val slots = mutableMapOf<Int, MutableState<NativeAdState>>()
    private val loads = mutableMapOf<Int, Job>()
    private var destroyed = false

    /** Current N, or null while no ad row may be added. */
    val every: Int? get() = everyState.value

    init {
        // Consent withdrawn or placement switched off while the screen is open: the rows go away, so do their ads.
        // Slots load again if ads are allowed again later.
        scope.launch {
            snapshotFlow { everyState.value }.collect { if (it == null) release() }
        }
    }

    /** The ad of the [index]-th ad row of the list: loaded on the first call, then the same state every time. */
    internal fun slot(index: Int): State<NativeAdState> {
        slots[index]?.let { return it }
        if (destroyed || loader == null) return mutableStateOf(NativeAdState.Unavailable)
        val state = mutableStateOf<NativeAdState>(NativeAdState.Loading)
        slots[index] = state
        loads[index] = scope.launch {
            val result = loader.load(placement)
            if (destroyed || slots[index] !== state) {
                // The screen was left while the ad arrived: nobody shows it.
                (result as? NativeAdState.Loaded)?.ad?.destroy()
                return@launch
            }
            state.value = result
            val outcome = if (result is NativeAdState.Loaded) "LOADED" else "UNAVAILABLE"
            logger?.info(TAG, "in-list ${placement.key} slot $index: $outcome")
        }
        return state
    }

    /** Destroys every loaded ad and cancels the loads still running. Called when the screen is left. */
    internal fun destroyAll() {
        destroyed = true
        release()
    }

    /** Destroys every loaded ad and cancels the running loads; later calls to [slot] load again. */
    private fun release() {
        loads.values.forEach { it.cancel() }
        loads.clear()
        slots.values.forEach { state -> (state.value as? NativeAdState.Loaded)?.ad?.destroy() }
        slots.clear()
    }
}

/** Loads one native ad for an in-list row. Named apart from the callback-based [NativeAdLoader] of the same package. */
internal fun interface InListAdLoader {
    /** Loaded or Unavailable, after the native load timeout at most. The caller owns a Loaded ad. */
    suspend fun load(placement: AdPlacement): NativeAdState
}

/**
 * Only small and medium native ads go inside a list. Not an exhaustive `when` on purpose: new formats (collapsible,
 * full screen) do not belong in a list and must not have to change this file.
 */
internal fun requireInListFormat(format: AdFormat) {
    require(format in setOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM)) { "$format is not an in-list ad format" }
}

/**
 * The in-list ads of [placement] (native small or medium) for one screen. Call it outside the lazy list, then pass it
 * to [itemsWithInListAds]. No row is added until ads are allowed (consent given, SDK started, `ads_enabled` and the
 * placement switched on remotely); N comes from `ads_inlist_every_count` and follows remote changes. Without an ads
 * controller (tests, previews) no row is ever added.
 */
@Composable
fun rememberInListAds(placement: AdPlacement): InListAds {
    requireInListFormat(placement.format)
    val controller = LocalAdsController.current
    val every: State<Int?> = if (controller == null) {
        remember { mutableStateOf(null) }
    } else {
        remember(controller, placement) { controller.inListEvery(placement) }
            .collectAsStateWithLifecycle(initialValue = null)
    }
    val scope = rememberCoroutineScope()
    val ads = remember(placement, controller, every, scope) {
        InListAds(
            placement = placement,
            every = every,
            loader = controller?.let { ads -> InListAdLoader { ads.loadNativeAd(it) } },
            scope = scope,
            logger = controller?.logger,
        )
    }
    DisposableEffect(ads) {
        onDispose { ads.destroyAll() }
    }
    return ads
}

/**
 * Adds [items] to a LazyColumn (vertical lists only) with a native ad row after every N items of [ads] (never first, never last).
 * The ad rows have their own keys and content type, so they never clash with the keys of [items].
 */
fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    val every = ads.every
    itemsWithInListAds(
        items = items,
        positions = if (every == null) emptyList() else inListAdPositions(items.size, every),
        placementKey = ads.placement.key,
        key = key,
        contentType = contentType,
        adContent = { slot -> InListAdRow(ads, slot) },
        itemContent = itemContent,
    )
}

/** Adds [items] with an ad row at each of [positions] (indexes in the combined list), drawn by [adContent]. */
internal fun <T> LazyListScope.itemsWithInListAds(
    items: List<T>,
    positions: List<Int>,
    placementKey: String,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    forEachInListRun(items.size, positions) { run ->
        when (run) {
            is InListRun.Items -> items(
                count = run.end - run.start,
                key = key?.let { itemKey -> { index -> itemKey(items[run.start + index]) } },
                contentType = { index -> contentType(items[run.start + index]) },
            ) { index -> itemContent(items[run.start + index]) }
            is InListRun.Ad -> item(key = InListAdKey(placementKey, run.slot), contentType = InListAdContentType) {
                adContent(run.slot)
            }
        }
    }
}

/**
 * Adds [items] to a LazyVerticalGrid (vertical grids only) of [columns] columns, with a full-width ad row after every
 * N items of [ads], N rounded up to whole rows ([gridAdEvery]). The grid does not tell its column count while the list
 * is built, so the caller passes it.
 */
fun <T> LazyGridScope.itemsWithInListAds(
    items: List<T>,
    ads: InListAds,
    columns: Int,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) {
    val every = ads.every
    itemsWithInListAds(
        items = items,
        positions = if (every == null) emptyList() else inListAdPositions(items.size, gridAdEvery(every, columns)),
        placementKey = ads.placement.key,
        key = key,
        contentType = contentType,
        adContent = { slot -> InListAdRow(ads, slot) },
        itemContent = itemContent,
    )
}

/** Grid version of the list function above: each ad row spans the whole grid width. */
internal fun <T> LazyGridScope.itemsWithInListAds(
    items: List<T>,
    positions: List<Int>,
    placementKey: String,
    key: ((item: T) -> Any)? = null,
    contentType: (item: T) -> Any? = { null },
    adContent: @Composable (slot: Int) -> Unit,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
) {
    forEachInListRun(items.size, positions) { run ->
        when (run) {
            is InListRun.Items -> items(
                count = run.end - run.start,
                key = key?.let { itemKey -> { index -> itemKey(items[run.start + index]) } },
                contentType = { index -> contentType(items[run.start + index]) },
            ) { index -> itemContent(items[run.start + index]) }
            is InListRun.Ad -> item(
                key = InListAdKey(placementKey, run.slot),
                span = { GridItemSpan(maxLineSpan) },
                contentType = InListAdContentType,
            ) { adContent(run.slot) }
        }
    }
}

/** A stretch of the combined list: app items `[start, end)`, or one ad row. */
private sealed interface InListRun {
    data class Items(val start: Int, val end: Int) : InListRun

    data class Ad(val slot: Int) : InListRun
}

/** Splits [itemCount] items around the ad [positions] into runs, in order. Empty item runs are skipped. */
private inline fun forEachInListRun(itemCount: Int, positions: List<Int>, action: (InListRun) -> Unit) {
    var start = 0
    positions.forEachIndexed { slot, position ->
        // Items before this ad: its position minus the ads before it.
        val end = (position - slot).coerceIn(start, itemCount)
        if (end > start) action(InListRun.Items(start, end))
        action(InListRun.Ad(slot))
        start = end
    }
    if (itemCount > start) action(InListRun.Items(start, itemCount))
}

/**
 * Key of an ad row; a type of its own, so it never equals the key of an app item. Parcelable because a lazy list saves
 * its item keys in the Bundle of the screen.
 */
internal data class InListAdKey(val placementKey: String, val slot: Int) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(placementKey)
        dest.writeInt(slot)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InListAdKey> = object : Parcelable.Creator<InListAdKey> {
            override fun createFromParcel(source: Parcel): InListAdKey =
                InListAdKey(placementKey = source.readString().orEmpty(), slot = source.readInt())

            override fun newArray(size: Int): Array<InListAdKey?> = arrayOfNulls(size)
        }
    }
}

/** Content type of ad rows, so Compose never reuses an item's row for an ad. */
private object InListAdContentType

/** Test tag of the [slot]-th ad row of a list. */
internal fun inListAdTag(slot: Int): String = "inlist_ad_$slot"

/**
 * One ad row, the "In-list ad slot" of the design: 16 dp above and below while loading or loaded, nothing (0 dp) when
 * no ad came.
 */
@Composable
internal fun InListAdRow(ads: InListAds, slot: Int, modifier: Modifier = Modifier) {
    val size = requireNativeFormat(ads.placement.format)
    val state by remember(ads, slot) { ads.slot(slot) }
    Box(modifier = modifier.fillMaxWidth().testTag(inListAdTag(slot))) {
        when (val current = state) {
            NativeAdState.Loading -> Box(Modifier.padding(vertical = Spacing.md)) { NativeAdPlaceholder(size) }
            is NativeAdState.Loaded ->
                Box(Modifier.padding(vertical = Spacing.md)) { BoundNativeAd(current.ad, size) }
            NativeAdState.Unavailable -> Unit
        }
    }
}
