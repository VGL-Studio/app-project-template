package com.vglstudio.template.adgallery

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.template.R
import com.vglstudio.template.core.ads.AdInspectorResult
import com.vglstudio.template.core.ads.AdPlacement
import com.vglstudio.template.core.ads.AdSlot
import com.vglstudio.template.core.ads.AdsGlobalFlags
import com.vglstudio.template.core.ads.BottomAdBarScaffold
import com.vglstudio.template.core.ads.InListAds
import com.vglstudio.template.core.ads.LocalAdsController
import com.vglstudio.template.core.ads.NativeAdCell
import com.vglstudio.template.core.ads.NativeAdSlot
import com.vglstudio.template.core.ads.NativeFullScreenStep
import com.vglstudio.template.core.ads.NativeFullScreenStepDefaults
import com.vglstudio.template.core.ads.NativeStepAd
import com.vglstudio.template.core.ads.NativeStepAdDefaults
import com.vglstudio.template.core.ads.PrivacyOptionsButton
import com.vglstudio.template.core.ads.RewardedAdDialog
import com.vglstudio.template.core.ads.RewardedNoAdPolicy
import com.vglstudio.template.core.ads.isPageSettled
import com.vglstudio.template.core.ads.isPageVisible
import com.vglstudio.template.core.ads.itemsWithInListAds
import com.vglstudio.template.core.ads.rememberInListAds
import com.vglstudio.template.core.ads.rememberNativeFullScreenStepReady
import com.vglstudio.template.core.ads.rememberOpenAdInspector
import com.vglstudio.template.core.ads.rememberRewardedAction
import com.vglstudio.template.core.ads.rememberShowAppOpenAd
import com.vglstudio.template.core.ads.rememberShowFullScreenAd
import com.vglstudio.template.core.designsystem.component.PrimaryButton
import com.vglstudio.template.core.designsystem.theme.Spacing
import kotlinx.coroutines.launch

/** Gap between the two full-screen buttons; outside the design system scale (design context, default frame). */
private val ButtonGap = 12.dp

/** Top of the title in the design frames: 20 dp. */
private val TitleTop = Spacing.md + Spacing.xs

/** Touch target of the page buttons that are not in the design. */
private val MinTouchTarget = 48.dp

/** Columns of the grid page. */
private const val GRID_COLUMNS = 2

/** The page of the pager page with the card: the third one (index 2). */
private const val PAGER_CARD_PAGE = 2

/** Index of the native page of a flow step, after the first step, when the slot has an ad (plan T-032). */
private const val FLOW_FULL_PAGE = 1

/** The rewarded policy of `allow_after_<n>` on the gallery: 2 tries in a row. */
private const val GALLERY_ALLOW_AFTER_TRIES = 2

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
    const val APP_OPEN_BUTTON = "ad_gallery_app_open_button"
    const val BANNERS_BUTTON = "ad_gallery_banners_button"
    const val GRID_BUTTON = "ad_gallery_grid_button"
    const val PAGER_BUTTON = "ad_gallery_pager_button"
    const val FLOW_BUTTON = "ad_gallery_flow_button"
    const val REWARDED_BUTTON = "ad_gallery_rewarded_button"
    const val STATUS_BUTTON = "ad_gallery_status_button"
    const val BANNERS = "ad_gallery_banners"
    const val BANNERS_LIST = "ad_gallery_banners_list"
    const val GRID = "ad_gallery_grid"
    const val PAGER = "ad_gallery_pager"
    const val FLOW = "ad_gallery_flow"
    const val FLOW_PAGER = "ad_gallery_flow_pager"
    const val FLOW_NEXT = "ad_gallery_flow_next"
    const val FLOW_CARD_FRAME = "ad_gallery_flow_card_frame"
    const val FLOW_FULL_PAGE_TOO_SHORT = "ad_gallery_flow_full_page_too_short"
    const val REWARDED = "ad_gallery_rewarded"
    const val REWARDED_BLOCK = "ad_gallery_rewarded_block"
    const val REWARDED_ALLOW = "ad_gallery_rewarded_allow"
    const val REWARDED_ALLOW_AFTER = "ad_gallery_rewarded_allow_after"
    const val REWARDED_RUN = "ad_gallery_rewarded_run"
    const val STATUS = "ad_gallery_status"

    /** The item [number] of the grid page. */
    fun gridItem(number: Int): String = "ad_gallery_grid_item_$number"
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

    /** The fixed banner of the banners page (T-029). */
    @Composable
    fun Banner(modifier: Modifier)

    /** The collapsible banner at the bottom of the banners page (B-03). */
    @Composable
    fun CollapsibleBanner(modifier: Modifier)

    /** The MREC banner of the banners page (KB-29). */
    @Composable
    fun MediumRectangleBanner(modifier: Modifier)

    /** N of the grid page, or null while its ads are off. Called on the grid page, before its grid. */
    @Composable
    fun gridEvery(): Int?

    /** Adds [items] to the grid of the grid page, with a card in one cell where each ad goes (T-028). */
    fun LazyGridScope.gridWithAds(items: List<Int>, itemContent: @Composable LazyGridItemScope.(Int) -> Unit)

    /**
     * The card of the pager page; [viewed] once its page settled (P-07), [onScreen] while its page is in view, so a
     * page scrolled away gives its ad back (P-01).
     */
    @Composable
    fun CollectionCard(viewed: Boolean, onScreen: Boolean, modifier: Modifier)

    /** The native card of the flow step on screen, [step] its page (T-032). */
    @Composable
    fun StepCard(step: Int, modifier: Modifier)

    /** Whether the native page of a flow step has an ad now. */
    @Composable
    fun stepFullPageReady(): Boolean

    /** The native page of a flow step, [shown] while it is the page on screen. */
    @Composable
    fun StepFullPage(shown: Boolean, modifier: Modifier)

    /** The privacy options button (KB-12): withdraw or give consent again. */
    @Composable
    fun PrivacyButton(modifier: Modifier)
}

/**
 * Real ads of the gallery, from the staging-only placements `gallery_*`. In the list, small and medium take turns
 * (two placements, one format each); N comes from `native_every_<placement code>`, as for any list of the app. The
 * banners page has the fixed banner, the MREC banner (`gallery_banner_mrec`) and the collapsible banner.
 */
internal class SdkAdGallerySlots : AdGallerySlots {
    // Set by inListEvery() during the composition of the in-list page, read when its lazy list is built after it.
    private var inListAds: List<InListAds> = emptyList()

    // Set by gridEvery() during the composition of the grid page, read when its lazy grid is built after it.
    private var gridAds: InListAds? = null

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

    @Composable
    override fun Banner(modifier: Modifier) {
        AdSlot(AdPlacement.GALLERY_BANNER, modifier)
    }

    @Composable
    override fun CollapsibleBanner(modifier: Modifier) {
        AdSlot(AdPlacement.GALLERY_BANNER_COLLAPSIBLE, modifier)
    }

    @Composable
    override fun MediumRectangleBanner(modifier: Modifier) {
        AdSlot(AdPlacement.GALLERY_BANNER_MREC, modifier)
    }

    @Composable
    override fun gridEvery(): Int? {
        // Created at page level, outside the lazy grid, as for the in-list page.
        val ads = rememberInListAds(AdPlacement.GALLERY_IN_COLLECTION)
        gridAds = ads
        return ads.every
    }

    override fun LazyGridScope.gridWithAds(items: List<Int>, itemContent: @Composable LazyGridItemScope.(Int) -> Unit) {
        val ads = gridAds
        if (ads == null) {
            items(items.size, key = { items[it] }) { itemContent(items[it]) }
        } else {
            itemsWithInListAds(
                items = items,
                ads = ads,
                columns = GRID_COLUMNS,
                key = { it },
                itemContent = itemContent,
            )
        }
    }

    @Composable
    override fun CollectionCard(viewed: Boolean, onScreen: Boolean, modifier: Modifier) {
        NativeAdCell(AdPlacement.GALLERY_IN_COLLECTION, modifier, viewed = viewed, onScreen = onScreen)
    }

    @Composable
    override fun StepCard(step: Int, modifier: Modifier) {
        NativeStepAd(AdPlacement.GALLERY_NATIVE_STEP, step, modifier)
    }

    @Composable
    override fun stepFullPageReady(): Boolean = rememberNativeFullScreenStepReady(AdPlacement.GALLERY_NATIVE_STEP_FULL)

    @Composable
    override fun StepFullPage(shown: Boolean, modifier: Modifier) {
        NativeFullScreenStep(AdPlacement.GALLERY_NATIVE_STEP_FULL, modifier, shown = shown)
    }

    @Composable
    override fun PrivacyButton(modifier: Modifier) {
        PrivacyOptionsButton(modifier)
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
    val showAppOpen = rememberShowAppOpenAd(AdPlacement.GALLERY_APP_OPEN)
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
    // The policy picked on the rewarded page applies to the next taps (question 13 of the ads spec).
    val rewardedPolicy = (uiState as? AdGalleryUiState.Rewarded)?.policy ?: GalleryRewarded.BLOCK
    val rewardedAction = rememberRewardedAction(
        AdPlacement.GALLERY_RW_ACTION,
        onGranted = viewModel::onRewardGranted,
        noAdPolicyOverride = rewardedPolicy.toNoAdPolicy(),
    )
    RewardedAdDialog(rewardedAction, title = stringResource(R.string.ad_gallery_rewarded_dialog_title))
    val switches by viewModel.switches.collectAsStateWithLifecycle()
    val status = rememberGalleryAdStatus(switches)
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
        status = status,
        onShowAppOpen = {
            viewModel.onFullScreenRequested(GalleryFullScreen.APP_OPEN)
            showAppOpen()
        },
        onOpenBanners = viewModel::openBanners,
        onOpenGrid = viewModel::openGrid,
        onOpenPager = viewModel::openPager,
        onOpenFlowSteps = viewModel::openFlowSteps,
        onOpenRewarded = viewModel::openRewarded,
        onOpenStatus = viewModel::openStatus,
        onStartRewarded = {
            viewModel.onRewardedRequested()
            rewardedAction.start()
        },
        onSelectRewardedPolicy = viewModel::selectRewardedPolicy,
    )
}

/** The status of the "Ad status" page from the public flows of the ads controller; unknown without one. */
@Composable
private fun rememberGalleryAdStatus(switches: AdsGlobalFlags?): GalleryAdStatus {
    val ads = LocalAdsController.current ?: return GalleryAdStatus.Unknown
    val path by ads.consentPath.collectAsStateWithLifecycle()
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val privacyOptionsRequired by ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val sdkReady by ads.sdkReady.collectAsStateWithLifecycle()
    return galleryAdStatus(path, canRequestAds, privacyOptionsRequired, sdkReady, switches)
}

private fun GalleryRewarded.toNoAdPolicy(): RewardedNoAdPolicy = when (this) {
    GalleryRewarded.BLOCK -> RewardedNoAdPolicy.Block
    GalleryRewarded.ALLOW -> RewardedNoAdPolicy.Allow
    GalleryRewarded.ALLOW_AFTER -> RewardedNoAdPolicy.AllowAfter(GALLERY_ALLOW_AFTER_TRIES)
}

/** The actions of the pages added by plan T-036, passed down together. */
private class MoreFormatsActions(
    val onShowAppOpen: () -> Unit,
    val onOpenBanners: () -> Unit,
    val onOpenGrid: () -> Unit,
    val onOpenPager: () -> Unit,
    val onOpenFlowSteps: () -> Unit,
    val onOpenRewarded: () -> Unit,
    val onOpenStatus: () -> Unit,
)

/**
 * The staging ad gallery, stateless: the default page of the design (`AdGalleryScreen_*`) or one of its sub-pages.
 * The section "More formats" and its six pages (plan T-036) have no design frame.
 */
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
    status: GalleryAdStatus = GalleryAdStatus.Unknown,
    onShowAppOpen: () -> Unit = {},
    onOpenBanners: () -> Unit = {},
    onOpenGrid: () -> Unit = {},
    onOpenPager: () -> Unit = {},
    onOpenFlowSteps: () -> Unit = {},
    onOpenRewarded: () -> Unit = {},
    onOpenStatus: () -> Unit = {},
    onStartRewarded: () -> Unit = {},
    onSelectRewardedPolicy: (GalleryRewarded) -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(AdGalleryTestTags.SCREEN),
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
                more = MoreFormatsActions(
                    onShowAppOpen = onShowAppOpen,
                    onOpenBanners = onOpenBanners,
                    onOpenGrid = onOpenGrid,
                    onOpenPager = onOpenPager,
                    onOpenFlowSteps = onOpenFlowSteps,
                    onOpenRewarded = onOpenRewarded,
                    onOpenStatus = onOpenStatus,
                ),
                modifier = pageModifier,
            )
            is AdGalleryUiState.InList -> InListPage(uiState.itemCount, slots, pageModifier)
            is AdGalleryUiState.Collapsible -> CollapsiblePage(uiState.itemCount, slots, pageModifier)
            is AdGalleryUiState.Banners -> BannersPage(uiState.itemCount, slots, pageModifier)
            is AdGalleryUiState.Grid -> GridPage(uiState.itemCount, slots, pageModifier)
            is AdGalleryUiState.Pager -> PagerPage(uiState.pageCount, slots, pageModifier)
            is AdGalleryUiState.FlowSteps -> FlowStepsPage(uiState.stepCount, slots, pageModifier)
            is AdGalleryUiState.Rewarded -> RewardedPage(
                uiState = uiState,
                onSelectPolicy = onSelectRewardedPolicy,
                onRun = onStartRewarded,
                modifier = pageModifier,
            )
            AdGalleryUiState.Status -> StatusPage(status, slots, pageModifier)
        }
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
    more: MoreFormatsActions,
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
            MoreFormats(more)
        }
    }
}

/** Not in the design (plan T-036, question 1): app open and the pages of the formats of the third batch. */
@Composable
private fun MoreFormats(more: MoreFormatsActions) {
    SectionLabel(stringResource(R.string.ad_gallery_more_formats))
    PrimaryButton(
        text = stringResource(R.string.ad_gallery_app_open),
        onClick = more.onShowAppOpen,
        modifier = Modifier.fillMaxWidth().testTag(AdGalleryTestTags.APP_OPEN_BUTTON),
    )
    PageButtonRow(
        R.string.ad_gallery_open_banners to more.onOpenBanners,
        AdGalleryTestTags.BANNERS_BUTTON,
        R.string.ad_gallery_open_grid to more.onOpenGrid,
        AdGalleryTestTags.GRID_BUTTON,
    )
    PageButtonRow(
        R.string.ad_gallery_open_pager to more.onOpenPager,
        AdGalleryTestTags.PAGER_BUTTON,
        R.string.ad_gallery_open_flow to more.onOpenFlowSteps,
        AdGalleryTestTags.FLOW_BUTTON,
    )
    PageButtonRow(
        R.string.ad_gallery_open_rewarded to more.onOpenRewarded,
        AdGalleryTestTags.REWARDED_BUTTON,
        R.string.ad_gallery_open_status to more.onOpenStatus,
        AdGalleryTestTags.STATUS_BUTTON,
    )
}

@Composable
private fun PageButtonRow(start: Pair<Int, () -> Unit>, startTag: String, end: Pair<Int, () -> Unit>, endTag: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap)) {
        PageButton(
            text = stringResource(start.first),
            onClick = start.second,
            modifier = Modifier.weight(1f).testTag(startTag),
        )
        PageButton(
            text = stringResource(end.first),
            onClick = end.second,
            modifier = Modifier.weight(1f).testTag(endTag),
        )
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

/**
 * The banners page (T-029): a fixed banner under the title, the MREC banner under it (KB-29, outside the list so its
 * ad view is not built again on each scroll), the list, the collapsible banner at the bottom (B-03).
 */
@Composable
private fun BannersPage(itemCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier.testTag(AdGalleryTestTags.BANNERS)) {
        PageTitle(stringResource(R.string.ad_gallery_banners_title))
        slots.Banner(Modifier.fillMaxWidth())
        slots.MediumRectangleBanner(Modifier.fillMaxWidth().padding(vertical = Spacing.md))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.md)
                .testTag(AdGalleryTestTags.BANNERS_LIST),
        ) {
            items(itemCount, key = { it + 1 }) { ListItem(it + 1) }
        }
        slots.CollapsibleBanner(Modifier.fillMaxWidth())
    }
}

/** The grid page (T-028): two columns, a card in one cell after every N items. */
@Composable
private fun GridPage(itemCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier) {
        val every = slots.gridEvery()
        PageTitle(
            if (every == null) {
                stringResource(R.string.ad_gallery_grid_title_off)
            } else {
                stringResource(R.string.ad_gallery_grid_title, every)
            },
        )
        val items = remember(itemCount) { (1..itemCount).toList() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(AdGalleryTestTags.GRID),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            with(slots) { gridWithAds(items) { GridItem(it) } }
        }
    }
}

@Composable
private fun GridItem(number: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag(AdGalleryTestTags.gridItem(number))
            .padding(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.ad_gallery_grid_item, number),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The pager page (T-028): the third page is a card, seen only once that page settled (P-07). */
@Composable
private fun PagerPage(pageCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier) {
        PageTitle(stringResource(R.string.ad_gallery_pager_title))
        val pagerState = rememberPagerState { pageCount }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(AdGalleryTestTags.PAGER),
            contentPadding = PaddingValues(horizontal = Spacing.md),
            pageSpacing = Spacing.md,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (page == PAGER_CARD_PAGE) {
                    slots.CollectionCard(
                        viewed = pagerState.isPageSettled(page),
                        onScreen = pagerState.isPageVisible(page),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.ad_gallery_pager_page, page + 1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.ad_gallery_pager_hint, pagerState.settledPage + 1, pageCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

/**
 * The flow page (T-032): steps in a pager moved by the Next button; the native page of a step after the first one
 * when its slot has an ad, decided until the flow starts so the number of pages does not change while it runs. The
 * step card sits outside the pager, in a fixed frame laid over the bottom of the step area, built only on the steps
 * with a card: the native page takes the whole step area (one native ad per step; plan T-041). The frame is as high
 * as the card, `NativeStepAdDefaults.height()` (250 dp at the default font scale, more with large text; plan T-045),
 * never a fixed 250 dp. The pager has the same height on every step and Next stays at one place (OB-01). When the
 * step area is shorter than the page needs, `NativeFullScreenStepDefaults.minHeight()` (a phone in landscape with
 * large text; plan T-049), the native page is not built and a line says why, in place of an empty area.
 */
@Composable
private fun FlowStepsPage(stepCount: Int, slots: AdGallerySlots, modifier: Modifier) {
    val ready = slots.stepFullPageReady()
    // Saved, like the page of the pager: after a rotation the slot reads "not ready" in its first frame (T-032).
    var fixedFullPage by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val withFullPage = fixedFullPage ?: ready
    val pageCount = stepCount + if (withFullPage) 1 else 0
    val pagerState = rememberPagerState { pageCount }
    val scope = rememberCoroutineScope()
    val isFullPage = { page: Int -> withFullPage && page == FLOW_FULL_PAGE }
    // The fixed frame of the step card: the Next button stays at one place on every step (OB-01).
    val stepCardFrame = NativeStepAdDefaults.height()
    // The least height of the native page at the font scale: below it the page would take no ad (plan T-049).
    val fullPageLeast = NativeFullScreenStepDefaults.minHeight()
    Column(modifier = modifier.testTag(AdGalleryTestTags.FLOW)) {
        PageTitle(stringResource(R.string.ad_gallery_flow_title))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag(AdGalleryTestTags.FLOW_PAGER),
                userScrollEnabled = false,
            ) { page ->
                if (isFullPage(page)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        if (maxHeight >= fullPageLeast) {
                            slots.StepFullPage(
                                shown = pagerState.settledPage == page,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.ad_gallery_flow_full_page_too_short),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = Spacing.md)
                                    .testTag(AdGalleryTestTags.FLOW_FULL_PAGE_TOO_SHORT),
                            )
                        }
                    }
                } else {
                    val step = if (withFullPage && page > FLOW_FULL_PAGE) page else page + 1
                    // The bottom of the area is the card's: the text stays in the middle of the part above it.
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = stepCardFrame),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.ad_gallery_flow_step, step, stepCount),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            val page = pagerState.settledPage
            if (!isFullPage(page)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(stepCardFrame)
                        .padding(horizontal = Spacing.md)
                        .testTag(AdGalleryTestTags.FLOW_CARD_FRAME),
                ) {
                    slots.StepCard(step = page, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.ad_gallery_flow_next),
            onClick = {
                fixedFullPage = withFullPage
                val next = (pagerState.settledPage + 1) % pageCount
                scope.launch { pagerState.animateScrollToPage(next) }
            },
            modifier = Modifier.fillMaxWidth().padding(Spacing.md).testTag(AdGalleryTestTags.FLOW_NEXT),
        )
    }
}

/** The rewarded page (T-031): the policy of `gallery_rw_action` when no ad, and the sample action. */
@Composable
private fun RewardedPage(
    uiState: AdGalleryUiState.Rewarded,
    onSelectPolicy: (GalleryRewarded) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).testTag(AdGalleryTestTags.REWARDED)) {
        PageTitle(stringResource(R.string.ad_gallery_rewarded_title))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BodyText(stringResource(R.string.ad_gallery_rewarded_placement))
            Column(modifier = Modifier.selectableGroup()) {
                PolicyRow(
                    R.string.ad_gallery_rewarded_block,
                    AdGalleryTestTags.REWARDED_BLOCK,
                    uiState.policy == GalleryRewarded.BLOCK,
                ) { onSelectPolicy(GalleryRewarded.BLOCK) }
                PolicyRow(
                    R.string.ad_gallery_rewarded_allow,
                    AdGalleryTestTags.REWARDED_ALLOW,
                    uiState.policy == GalleryRewarded.ALLOW,
                ) { onSelectPolicy(GalleryRewarded.ALLOW) }
                PolicyRow(
                    R.string.ad_gallery_rewarded_allow_after,
                    AdGalleryTestTags.REWARDED_ALLOW_AFTER,
                    uiState.policy == GalleryRewarded.ALLOW_AFTER,
                ) { onSelectPolicy(GalleryRewarded.ALLOW_AFTER) }
            }
            PrimaryButton(
                text = stringResource(R.string.ad_gallery_rewarded_run),
                onClick = onRun,
                modifier = Modifier.fillMaxWidth().testTag(AdGalleryTestTags.REWARDED_RUN),
            )
            BodyText(stringResource(R.string.ad_gallery_rewarded_ran, uiState.actionsRun))
        }
    }
}

@Composable
private fun PolicyRow(label: Int, tag: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.md),
        )
    }
}

/** The status page (KB-11): consent path, temporary right, remote switches and the privacy options button. */
@Composable
private fun StatusPage(status: GalleryAdStatus, slots: AdGallerySlots, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).testTag(AdGalleryTestTags.STATUS)) {
        PageTitle(stringResource(R.string.ad_gallery_status_title))
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatusLine(
                R.string.ad_gallery_status_consent_path,
                status.consentPath?.name ?: stringResource(R.string.ad_gallery_status_pending),
            )
            StatusLine(R.string.ad_gallery_status_allowed, yesNo(status.adsAllowed))
            StatusLine(R.string.ad_gallery_status_temporary, yesNo(status.temporaryRight))
            StatusLine(R.string.ad_gallery_status_privacy, yesNo(status.privacyOptionsRequired))
            StatusLine(R.string.ad_gallery_status_sdk, yesNo(status.sdkReady))
            StatusLine(R.string.ad_gallery_status_shortcut, onOff(status.consentShortcutRemote))
            StatusLine(R.string.ad_gallery_status_combo, onOff(status.nativeAfterInter))
            StatusLine(R.string.ad_gallery_status_bottom, onOff(status.nativeBottom))
            StatusLine(R.string.ad_gallery_status_collapsible, onOff(status.nativeCollapsible))
        }
        slots.PrivacyButton(Modifier.padding(horizontal = Spacing.sm))
    }
}

@Composable
private fun StatusLine(label: Int, value: String) {
    BodyText(stringResource(R.string.ad_gallery_status_line, stringResource(label), value))
}

@Composable
private fun yesNo(value: Boolean): String =
    stringResource(if (value) R.string.ad_gallery_status_yes else R.string.ad_gallery_status_no)

@Composable
private fun onOff(value: Boolean?): String = stringResource(
    when (value) {
        null -> R.string.ad_gallery_status_pending
        true -> R.string.ad_gallery_status_on
        false -> R.string.ad_gallery_status_off
    },
)

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
