package com.vglstudio.template.adgallery

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.R
import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.BottomAdBarScaffold
import com.vglstudio.template.core.ads.InListAds
import com.vglstudio.template.core.ads.NativeAdSlot
import com.vglstudio.template.core.ads.itemsWithInListAds
import com.vglstudio.template.core.ads.rememberInListAds
import com.vglstudio.template.core.ads.rememberOpenAdInspector
import com.vglstudio.template.core.ads.rememberShowFullScreenAd
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.theme.Spacing

/** Gap between the two full-screen buttons; outside the design system scale (design context, default frame). */
private val ButtonGap = 12.dp

/** Top of the title in the design frames: 20 dp. */
private val TitleTop = Spacing.md + Spacing.xs

/** Touch target of the page buttons that are not in the design. */
private val MinTouchTarget = 48.dp

object AdGalleryTestTags {
    const val SCREEN = "ad_gallery_screen"
    const val TITLE = "ad_gallery_title"
    const val DEFAULT = "ad_gallery_default"
    const val IN_LIST = "ad_gallery_inlist"
    const val COLLAPSIBLE = "ad_gallery_collapsible"
    const val COLLAPSIBLE_LIST = "ad_gallery_collapsible_list"
    const val INTERSTITIAL_BUTTON = "ad_gallery_interstitial_button"
    const val NATIVE_FULL_SCREEN_BUTTON = "ad_gallery_native_full_screen_button"
    const val INSPECTOR_BUTTON = "ad_gallery_inspector_button"
    const val IN_LIST_BUTTON = "ad_gallery_inlist_button"
    const val COLLAPSIBLE_BUTTON = "ad_gallery_collapsible_button"
}

/**
 * The ads of the gallery. The app draws real ads with the composables of :core:ads ([SdkAdGallerySlots]); tests and
 * screenshots use fake ads with fixed content.
 */
interface AdGallerySlots {
    @Composable
    fun Small(modifier: Modifier)

    @Composable
    fun Medium(modifier: Modifier)

    /** The bottom bar of the collapsible page around [content]. */
    @Composable
    fun BottomBar(content: @Composable (PaddingValues) -> Unit)

    /** N of the in-list page, or null while in-list ads are off. Called on the in-list page, before its list. */
    @Composable
    fun inListEvery(): Int?

    /** Adds [items] to the list of the in-list page, with ads where they go. */
    fun LazyListScope.listWithAds(items: List<Int>, itemContent: @Composable LazyItemScope.(Int) -> Unit)
}

/**
 * Real ads of the gallery, from the staging-only placements `gallery_*`. In the list, small and medium take turns
 * (two placements, one format each); N comes from `native_every_<placement code>`, as for any list of the app.
 */
internal class SdkAdGallerySlots : AdGallerySlots {
    // Set by inListEvery() during the composition of the in-list page, read when its lazy list is built after it.
    private var inListAds: List<InListAds> = emptyList()

    @Composable
    override fun Small(modifier: Modifier) {
        NativeAdSlot(AdPlacement.GALLERY_NATIVE_SMALL, modifier)
    }

    @Composable
    override fun Medium(modifier: Modifier) {
        NativeAdSlot(AdPlacement.GALLERY_NATIVE_MEDIUM, modifier)
    }

    @Composable
    override fun BottomBar(content: @Composable (PaddingValues) -> Unit) {
        BottomAdBarScaffold(AdPlacement.GALLERY_NATIVE_SMALL, content = content)
    }

    @Composable
    override fun inListEvery(): Int? {
        // Created at page level, outside the lazy list, so a row scrolled away and back keeps its ad.
        val small = rememberInListAds(AdPlacement.GALLERY_INLIST_SMALL)
        val medium = rememberInListAds(AdPlacement.GALLERY_INLIST_MEDIUM)
        inListAds = listOf(small, medium)
        return small.every
    }

    override fun LazyListScope.listWithAds(items: List<Int>, itemContent: @Composable LazyItemScope.(Int) -> Unit) {
        if (inListAds.isEmpty()) {
            items(items.size, key = { items[it] }) { itemContent(items[it]) }
        } else {
            itemsWithInListAds(items = items, ads = inListAds, key = { it }, itemContent = itemContent)
        }
    }
}

/** The staging ad gallery with real ads, its ViewModel and the device's back button. */
@Composable
fun AdGalleryRoute(viewModel: AdGalleryViewModel, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.back()) onExit() }

    val context = LocalContext.current
    val showInterstitial = rememberShowFullScreenAd(AdPlacement.GALLERY_INTERSTITIAL)
    val showNativeFullScreen = rememberShowFullScreenAd(AdPlacement.GALLERY_NATIVE_FULL_SCREEN)
    val openAdInspector = rememberOpenAdInspector { result ->
        viewModel.onAdInspectorResult(result)
        val message = when (result) {
            AdInspectorResult.Closed -> null
            is AdInspectorResult.Failed -> context.getString(R.string.ad_gallery_inspector_failed, result.code)
            AdInspectorResult.NotReady -> context.getString(R.string.ad_gallery_inspector_not_ready)
        }
        // The SDK may answer off the main thread; a toast needs it.
        if (message != null) {
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val slots = remember { SdkAdGallerySlots() }

    AdGalleryScreen(
        uiState = uiState,
        slots = slots,
        onOpenInList = viewModel::openInList,
        onOpenCollapsible = viewModel::openCollapsible,
        onShowInterstitial = {
            viewModel.onFullScreenRequested(GalleryFullScreen.INTERSTITIAL)
            showInterstitial()
        },
        onShowNativeFullScreen = {
            viewModel.onFullScreenRequested(GalleryFullScreen.NATIVE_FULL_SCREEN)
            showNativeFullScreen()
        },
        onOpenAdInspector = openAdInspector,
        modifier = modifier,
    )
}

/** The staging ad gallery, stateless: one of three pages (design `AdGalleryScreen_*`). */
@Composable
fun AdGalleryScreen(
    uiState: AdGalleryUiState,
    slots: AdGallerySlots,
    onOpenInList: () -> Unit,
    onOpenCollapsible: () -> Unit,
    onShowInterstitial: () -> Unit,
    onShowNativeFullScreen: () -> Unit,
    onOpenAdInspector: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(AdGalleryTestTags.SCREEN),
    ) {
        AdGalleryPage(
            uiState = uiState,
            slots = slots,
            onOpenInList = onOpenInList,
            onOpenCollapsible = onOpenCollapsible,
            onShowInterstitial = onShowInterstitial,
            onShowNativeFullScreen = onShowNativeFullScreen,
            onOpenAdInspector = onOpenAdInspector,
        )
    }
}

@Composable
private fun AdGalleryPage(
    uiState: AdGalleryUiState,
    slots: AdGallerySlots,
    onOpenInList: () -> Unit,
    onOpenCollapsible: () -> Unit,
    onShowInterstitial: () -> Unit,
    onShowNativeFullScreen: () -> Unit,
    onOpenAdInspector: () -> Unit,
) {
    val pageModifier = Modifier.fillMaxSize()
    when (uiState) {
        AdGalleryUiState.Default -> DefaultPage(
            slots = slots,
            onOpenInList = onOpenInList,
            onOpenCollapsible = onOpenCollapsible,
            onShowInterstitial = onShowInterstitial,
            onShowNativeFullScreen = onShowNativeFullScreen,
            onOpenAdInspector = onOpenAdInspector,
            modifier = pageModifier,
        )
        is AdGalleryUiState.InList -> InListPage(uiState.itemCount, slots, pageModifier)
        is AdGalleryUiState.Collapsible -> CollapsiblePage(uiState.itemCount, slots, pageModifier)
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(start = Spacing.md, top = TitleTop, end = Spacing.md, bottom = Spacing.lg)
            .testTag(AdGalleryTestTags.TITLE),
    )
}

@Composable
private fun DefaultPage(
    slots: AdGallerySlots,
    onOpenInList: () -> Unit,
    onOpenCollapsible: () -> Unit,
    onShowInterstitial: () -> Unit,
    onShowNativeFullScreen: () -> Unit,
    onOpenAdInspector: () -> Unit,
    modifier: Modifier,
) {
    // One scrolling column, so every button stays reachable on a low screen (L-028).
    Column(modifier = modifier.verticalScroll(rememberScrollState()).testTag(AdGalleryTestTags.DEFAULT)) {
        PageTitle(stringResource(R.string.ad_gallery_title))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionLabel(stringResource(R.string.ad_gallery_native_small))
            slots.Small(Modifier.fillMaxWidth())
            SectionLabel(stringResource(R.string.ad_gallery_native_medium))
            slots.Medium(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap)) {
                PrimaryButton(
                    text = stringResource(R.string.ad_gallery_interstitial),
                    onClick = onShowInterstitial,
                    modifier = Modifier.weight(1f).testTag(AdGalleryTestTags.INTERSTITIAL_BUTTON),
                )
                PrimaryButton(
                    text = stringResource(R.string.ad_gallery_native_full_screen),
                    onClick = onShowNativeFullScreen,
                    modifier = Modifier.weight(1f).testTag(AdGalleryTestTags.NATIVE_FULL_SCREEN_BUTTON),
                )
            }
            PrimaryButton(
                text = stringResource(R.string.ad_gallery_open_inspector),
                onClick = onOpenAdInspector,
                modifier = Modifier.fillMaxWidth().testTag(AdGalleryTestTags.INSPECTOR_BUTTON),
            )
            // Not in the design (plan T-014, question 7): the way to the two other pages.
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap)) {
                PageButton(
                    text = stringResource(R.string.ad_gallery_open_inlist),
                    onClick = onOpenInList,
                    modifier = Modifier.weight(1f).testTag(AdGalleryTestTags.IN_LIST_BUTTON),
                )
                PageButton(
                    text = stringResource(R.string.ad_gallery_open_collapsible),
                    onClick = onOpenCollapsible,
                    modifier = Modifier.weight(1f).testTag(AdGalleryTestTags.COLLAPSIBLE_BUTTON),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PageButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = MinTouchTarget)) {
        Text(text = text)
    }
}

@Composable
private fun InListPage(itemCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier) {
        val every = slots.inListEvery()
        PageTitle(
            if (every == null) {
                stringResource(R.string.ad_gallery_inlist_title_off)
            } else {
                stringResource(R.string.ad_gallery_inlist_title, every)
            },
        )
        val items = remember(itemCount) { (1..itemCount).toList() }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.md)
                .testTag(AdGalleryTestTags.IN_LIST),
        ) {
            with(slots) { listWithAds(items) { ListItem(it) } }
        }
    }
}

/**
 * The collapsible page: the list in the bottom bar of the gallery (plan T-030), which keeps the slot's space below
 * the list once the slot has an ad; the collapsible layer floats over the bottom of the list.
 */
@Composable
private fun CollapsiblePage(itemCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier.testTag(AdGalleryTestTags.COLLAPSIBLE)) {
        PageTitle(stringResource(R.string.ad_gallery_collapsible_title))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            slots.BottomBar { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Spacing.md)
                        .testTag(AdGalleryTestTags.COLLAPSIBLE_LIST),
                ) {
                    items(itemCount, key = { it + 1 }) { ListItem(it + 1) }
                }
            }
        }
    }
}

@Composable
private fun ListItem(number: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ad_gallery_list_item, number),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = Spacing.md),
        )
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
