package org.example.launchertest.ui.home.applist

import android.appwidget.AppWidgetHostView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import org.example.launchertest.data.HomeRowNavigationMode
import org.example.launchertest.data.LauncherFont
import org.example.launchertest.data.LauncherSettings
import org.example.launchertest.ui.home.HomeLayoutMetrics
import org.example.launchertest.ui.home.favorites.FAVORITES_OVERSCROLL_RESISTANCE
import org.example.launchertest.ui.home.favorites.FavoritesPanel
import org.example.launchertest.ui.home.shared.AppRow
import org.example.launchertest.ui.home.shared.HOME_LIST_SEARCH_DRAG_THRESHOLD_DP
import org.example.launchertest.ui.home.shared.HOME_ROW_HORIZONTAL_PADDING_DP
import org.example.launchertest.ui.home.shared.SectionHeader
import org.example.launchertest.ui.model.LauncherApp
import org.example.launchertest.widgets.WidgetStack

/**
 * Extension to erase content at the edges using a gradient mask.
 */
fun Modifier.fadingEdge(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }

@Composable
internal fun AppListPanel(
    listLayout: AppListLayout,
    widgetStacks: List<WidgetStack>,
    scrubbingLetter: State<Char?>,
    isScrubbing: State<Boolean>,
    isHiddenMode: Boolean,
    showFavoritesOnly: Boolean,
    isSearchActive: Boolean,
    highlightedAppComponentId: String?,
    listState: LazyListState,
    categoryPinOffsetPx: Int,
    layoutMetrics: HomeLayoutMetrics,
    onSearchActivated: () -> Unit,
    onAppListActivated: () -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onUnhideApp: (LauncherApp) -> Unit,
    onReorderFavorites: (List<LauncherApp>) -> Unit,
    onAddWidget: () -> Unit,
    onAddWidgetToStack: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgetStacks: (List<WidgetStack>) -> Unit,
    settings: LauncherSettings,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onHomeRowNavigationModeChanged: (HomeRowNavigationMode) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onResetSettings: () -> Unit,
    onRestartLauncher: () -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    modifier: Modifier = Modifier,
) {
    val apps = listLayout.apps
    val favorites = listLayout.favorites

    val density = LocalDensity.current
    val dragThresholdPx = with(density) { HOME_LIST_SEARCH_DRAG_THRESHOLD_DP.dp.toPx() }
    val categoryPinOffsetDp = with(density) { categoryPinOffsetPx.toDp() }

    // Mask for the top fade: starts transparent at 0dp and becomes fully opaque by 80dp
    val topFadeBrush = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.1f to Color.Black // Adjust this stop (0.1f) to control fade length
        )
    }

    val favAlpha by animateFloatAsState(
        targetValue = if (isScrubbing.value && !showFavoritesOnly) 0f else 1f,
        animationSpec = spring(
            stiffness = APP_LIST_FAVORITES_FADE_STIFFNESS,
            dampingRatio = APP_LIST_FAVORITES_FADE_DAMPING,
        ),
        label = "favAlpha",
    )

    val searchOverscrollConnection = remember(
        dragThresholdPx,
        isHiddenMode,
        showFavoritesOnly,
        isSearchActive,
        listState,
        onSearchActivated,
    ) {
        object : NestedScrollConnection {
            private var accumulatedOverscrollPx = 0f
            private var didTriggerSearch = false

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) {
                    reset()
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val isNormalAppList = !isHiddenMode && !showFavoritesOnly && !isSearchActive
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0

                if (source != NestedScrollSource.UserInput ||
                    !isNormalAppList ||
                    !isAtTop ||
                    available.y <= 0f
                ) {
                    reset()
                    return Offset.Zero
                }

                accumulatedOverscrollPx += available.y * FAVORITES_OVERSCROLL_RESISTANCE
                if (!didTriggerSearch && accumulatedOverscrollPx >= dragThresholdPx) {
                    didTriggerSearch = true
                    onSearchActivated()
                }

                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                reset()
                return Velocity.Zero
            }

            private fun reset() {
                accumulatedOverscrollPx = 0f
                didTriggerSearch = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // This is the "eraser" that creates transparency at the top
                .fadingEdge(topFadeBrush)
                .nestedScroll(searchOverscrollConnection)
                .pointerInput(isHiddenMode, showFavoritesOnly, isSearchActive) {
                    if (isHiddenMode && !showFavoritesOnly && !isSearchActive) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Final,
                            )
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                            if (up != null && !down.isConsumed && !up.isConsumed) {
                                onHiddenModeChanged(false)
                            }
                        }
                    }
                },
            state = listState,
            contentPadding = PaddingValues(
                start = if (showFavoritesOnly) {
                    0.dp
                } else {
                    layoutMetrics.appListContentStartPaddingDp.dp
                },
                top = if (isSearchActive) {
                    layoutMetrics.appListSearchTopPaddingDp.dp
                } else if (showFavoritesOnly) {
                    0.dp
                } else {
                    layoutMetrics.appListContentTopPaddingDp.dp
                },
                end = if (showFavoritesOnly) {
                    0.dp
                } else {
                    layoutMetrics.appListContentEndPaddingDp.dp
                },
                bottom = if (isSearchActive) {
                    layoutMetrics.appListSearchBottomPaddingDp.dp
                } else if (showFavoritesOnly) {
                    0.dp
                } else {
                    layoutMetrics.appListContentBottomPaddingDp.dp
                },
            ),
        ) {
            if (!showFavoritesOnly && !isSearchActive) {
                item(key = "category_pin_spacer") {
                    Spacer(modifier = Modifier.height(categoryPinOffsetDp))
                }
            }

            if (showFavoritesOnly) {
                item(key = "favorites_panel") {
                    FavoritesPanel(
                        favorites = favorites,
                        widgetStacks = widgetStacks,
                        favAlpha = favAlpha,
                        isSearchActive = isSearchActive,
                        isHiddenMode = isHiddenMode,
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onHiddenModeChanged = onHiddenModeChanged,
                        onReorderFavorites = onReorderFavorites,
                        onSearchActivated = onSearchActivated,
                        onAppListActivated = onAppListActivated,
                        onAddWidget = onAddWidget,
                        onAddWidgetToStack = onAddWidgetToStack,
                        onRemoveWidget = onRemoveWidget,
                        onReorderWidgetStacks = onReorderWidgetStacks,
                        settings = settings,
                        onHideStatusBarChanged = onHideStatusBarChanged,
                        onHideAppIconsChanged = onHideAppIconsChanged,
                        onHomeRowNavigationModeChanged = onHomeRowNavigationModeChanged,
                        onFontChanged = onFontChanged,
                        onResetSettings = onResetSettings,
                        onRestartLauncher = onRestartLauncher,
                        createWidgetView = createWidgetView,
                        getWidgetMinHeightDp = getWidgetMinHeightDp,
                        layoutMetrics = layoutMetrics,
                        modifier = Modifier.fillParentMaxHeight(),
                    )
                }
            }

            if (!showFavoritesOnly) {
                if (isHiddenMode) {
                    item(key = "hidden_mode_indicator") {
                        HiddenModeIndicator()
                    }
                }

                itemsIndexed(
                    items = apps,
                    key = { _, app -> app.packageName + app.activityName },
                ) { index, app ->
                    val bucket = bucketFor(app.label)
                    val previousBucket = if (index > 0) bucketFor(apps[index - 1].label) else null

                    val rowModifier = Modifier.graphicsLayer {
                        val activeLetter = scrubbingLetter.value
                        alpha = if (activeLetter == null || activeLetter == bucket) 1f else 0f
                    }

                    if (previousBucket == null || bucket != previousBucket) {
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(APP_LIST_BUCKET_SPACER_HEIGHT_DP.dp))
                        }

                        SectionHeader(
                            text = bucket.toString(),
                            modifier = rowModifier,
                        )
                    }

                    AppRow(
                        app = app,
                        isFavorite = false,
                        isHiddenMode = isHiddenMode,
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onUnhideApp = onUnhideApp,
                        hideAppIcons = settings.hideAppIcons,
                        isHighlighted = isSearchActive && app.componentId() == highlightedAppComponentId,
                        modifier = rowModifier,
                    )
                }
            }
        }
    }
}
internal fun bucketFor(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}

private fun LauncherApp.componentId(): String = "${packageName}/${activityName}"

@Composable
private fun HiddenModeIndicator() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(
            start = HOME_ROW_HORIZONTAL_PADDING_DP.dp,
            end = HOME_ROW_HORIZONTAL_PADDING_DP.dp,
            bottom = 18.dp,
        ),
    ) {
        Text(
            text = "HIDDEN APPS",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
