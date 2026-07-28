package com.what386.waterfall.ui.home.favorites

import android.appwidget.AppWidgetHostView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.what386.waterfall.R
import com.what386.waterfall.data.HomeRowNavigationMode
import com.what386.waterfall.data.LauncherFont
import com.what386.waterfall.data.LauncherSettings
import com.what386.waterfall.ui.home.HomeLayoutMetrics
import com.what386.waterfall.ui.home.shared.AppRow
import com.what386.waterfall.ui.home.shared.SectionHeader
import com.what386.waterfall.ui.model.LauncherApp
import com.what386.waterfall.widgets.WidgetStack
import kotlinx.coroutines.launch

/**
 * The favorites panel: widgets + favorite app rows, all in a vertically scrollable column
 * that overscrolls and springs back to center like the A–Z rail does.
 *
 * Overscroll resistance: drag distance is dampened by [FAVORITES_OVERSCROLL_RESISTANCE]
 * so the panel feels springy but not free-floating.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesPanel(
    favorites: List<LauncherApp>,
    widgetStacks: List<WidgetStack>,
    favAlpha: Float,
    isSearchActive: Boolean,
    isHiddenMode: Boolean,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onReorderFavorites: (List<LauncherApp>) -> Unit,
    onSearchActivated: () -> Unit,
    onAppListActivated: () -> Unit,
    onAddWidget: () -> Unit,
    onAddWidgetToStack: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgetStacks: (List<WidgetStack>) -> Unit,
    settings: LauncherSettings,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onHideSearchButtonChanged: (Boolean) -> Unit,
    onCleanHomeScreenChanged: (Boolean) -> Unit,
    onHomeRowNavigationModeChanged: (HomeRowNavigationMode) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onResetSettings: () -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    layoutMetrics: HomeLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val overscrollTriggerPx = with(density) { layoutMetrics.searchDragThresholdDp.dp.toPx() }
    val hasScrollableContent = scrollState.maxValue > 0
    val showFavoriteApps = !settings.cleanHomeScreen
    var showPanelMenu by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }
    var showHomeRowSheet by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var widgetReorderMode by remember { mutableStateOf(false) }
    var didTriggerSearchDuringDrag by remember { mutableStateOf(false) }
    var didTriggerAppListDuringDrag by remember { mutableStateOf(false) }
    val panelInteractionSource = remember { MutableInteractionSource() }
    var activeDragComponentId by remember { mutableStateOf<String?>(null) }
    var activeDragStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowMetrics = remember { mutableStateMapOf<String, FavoriteRowMetrics>() }
    val orderedFavorites = remember { mutableStateListOf<LauncherApp>() }
    var activeDragWidgetStackId by remember { mutableStateOf<String?>(null) }
    var activeDragWidgetStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetOffsetY by remember { mutableFloatStateOf(0f) }
    val widgetRowMetrics = remember { mutableStateMapOf<String, FavoriteRowMetrics>() }
    val orderedWidgetStacks = remember { mutableStateListOf<WidgetStack>() }
    val useCleanHomeWidgetLayout = settings.cleanHomeScreen && orderedWidgetStacks.isNotEmpty()

    LaunchedEffect(favorites) {
        val incomingById = favorites.associateBy(::favoriteComponentId)
        val currentIds = orderedFavorites.map(::favoriteComponentId)
        val kept = currentIds.mapNotNull(incomingById::get)
        val appended = favorites.filter { favoriteComponentId(it) !in currentIds.toSet() }
        orderedFavorites.clear()
        orderedFavorites.addAll(kept + appended)
    }

    LaunchedEffect(widgetStacks) {
        val incomingById = widgetStacks.associateBy(::widgetStackId)
        val currentIds = orderedWidgetStacks.map(::widgetStackId)
        val kept = currentIds.mapNotNull(incomingById::get)
        val appended = widgetStacks.filter { widgetStackId(it) !in currentIds.toSet() }
        orderedWidgetStacks.clear()
        orderedWidgetStacks.addAll(kept + appended)
    }

    fun persistFavoriteOrder() {
        onReorderFavorites(orderedFavorites.toList())
    }

    fun persistWidgetOrder() {
        onReorderWidgetStacks(orderedWidgetStacks.toList())
    }

    fun openSettingsSheet() {
        showPanelMenu = false
        showHomeRowSheet = false
        showSettingsSheet = true
    }

    fun closeSettingsSheet(reopenFavorites: Boolean) {
        showSettingsSheet = false
        showFontSheet = false
        showHomeRowSheet = false
        showPanelMenu = reopenFavorites
    }

    fun openFontSheet() {
        showSettingsSheet = false
        showHomeRowSheet = false
        showFontSheet = true
    }

    fun closeFontSheet() {
        showFontSheet = false
        showSettingsSheet = true
    }

    fun openHomeRowSheet() {
        showSettingsSheet = false
        showFontSheet = false
        showHomeRowSheet = true
    }

    fun closeHomeRowSheet() {
        showHomeRowSheet = false
        showSettingsSheet = true
    }

    fun clearDragState() {
        activeDragComponentId = null
        activeDragStartIndex = -1
        activeDragTargetIndex = -1
        activeDragOffsetY = 0f
    }

    fun commitDragReorderIfNeeded() {
        val activeComponentId = activeDragComponentId
        if (activeComponentId == null || activeDragTargetIndex == -1) {
            clearDragState()
            return
        }

        val currentIndex =
            orderedFavorites.indexOfFirst {
                favoriteComponentId(it) == activeComponentId
            }

        if (currentIndex != -1 &&
            currentIndex != activeDragTargetIndex &&
            activeDragTargetIndex in orderedFavorites.indices
        ) {
            val moved = orderedFavorites.removeAt(currentIndex)
            orderedFavorites.add(activeDragTargetIndex, moved)
            persistFavoriteOrder()
        }

        clearDragState()
    }

    fun clearWidgetDragState() {
        activeDragWidgetStackId = null
        activeDragWidgetStartIndex = -1
        activeDragWidgetTargetIndex = -1
        activeDragWidgetOffsetY = 0f
    }

    fun commitWidgetDragReorderIfNeeded() {
        val activeStackId = activeDragWidgetStackId
        if (activeStackId == null || activeDragWidgetTargetIndex == -1) {
            clearWidgetDragState()
            return
        }

        val currentIndex = orderedWidgetStacks.indexOfFirst { stack -> widgetStackId(stack) == activeStackId }

        if (currentIndex != -1 &&
            currentIndex != activeDragWidgetTargetIndex &&
            activeDragWidgetTargetIndex in orderedWidgetStacks.indices
        ) {
            val moved = orderedWidgetStacks.removeAt(currentIndex)
            orderedWidgetStacks.add(activeDragWidgetTargetIndex, moved)
            persistWidgetOrder()
        }

        clearWidgetDragState()
    }

    fun updateWidgetDragTarget(stackId: String) {
        val startIndex = activeDragWidgetStartIndex
        if (startIndex !in orderedWidgetStacks.indices) return

        val activeMetrics = widgetRowMetrics[stackId] ?: return
        val draggedCenterY = activeMetrics.centerY + activeDragWidgetOffsetY
        var targetIndex = startIndex

        if (activeDragWidgetOffsetY > 0f && startIndex < orderedWidgetStacks.lastIndex) {
            for (index in (startIndex + 1)..orderedWidgetStacks.lastIndex) {
                val candidateId = widgetStackId(orderedWidgetStacks[index])
                val metrics = widgetRowMetrics[candidateId] ?: continue
                val replaceThresholdY =
                    metrics.topY +
                        (metrics.height * FAVORITES_REORDER_SWAP_FRACTION_OF_ROW)

                if (draggedCenterY >= replaceThresholdY) {
                    targetIndex = index
                }
            }
        } else if (activeDragWidgetOffsetY < 0f && startIndex > 0) {
            for (index in (startIndex - 1) downTo 0) {
                val candidateId = widgetStackId(orderedWidgetStacks[index])
                val metrics = widgetRowMetrics[candidateId] ?: continue
                val replaceThresholdY =
                    metrics.topY +
                        (metrics.height * (1f - FAVORITES_REORDER_SWAP_FRACTION_OF_ROW))

                if (draggedCenterY <= replaceThresholdY) {
                    targetIndex = index
                }
            }
        }

        activeDragWidgetTargetIndex = targetIndex
    }

    fun updateDragTarget(componentId: String) {
        val startIndex = activeDragStartIndex
        if (startIndex !in orderedFavorites.indices) return

        val activeMetrics = rowMetrics[componentId] ?: return
        val draggedCenterY = activeMetrics.centerY + activeDragOffsetY
        var targetIndex = startIndex

        if (activeDragOffsetY > 0f && startIndex < orderedFavorites.lastIndex) {
            for (index in (startIndex + 1)..orderedFavorites.lastIndex) {
                val candidateId = favoriteComponentId(orderedFavorites[index])
                val metrics = rowMetrics[candidateId] ?: continue
                val replaceThresholdY =
                    metrics.topY +
                        (metrics.height * FAVORITES_REORDER_SWAP_FRACTION_OF_ROW)

                if (draggedCenterY >= replaceThresholdY) {
                    targetIndex = index
                }
            }
        } else if (activeDragOffsetY < 0f && startIndex > 0) {
            for (index in (startIndex - 1) downTo 0) {
                val candidateId = favoriteComponentId(orderedFavorites[index])
                val metrics = rowMetrics[candidateId] ?: continue
                val replaceThresholdY =
                    metrics.topY +
                        (metrics.height * (1f - FAVORITES_REORDER_SWAP_FRACTION_OF_ROW))

                if (draggedCenterY <= replaceThresholdY) {
                    targetIndex = index
                }
            }
        }

        activeDragTargetIndex = targetIndex
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .then(
                    if (reorderMode) {
                        Modifier.pointerInput(reorderMode) {
                            detectTapGestures {
                                reorderMode = false
                                commitDragReorderIfNeeded()
                            }
                        }
                    } else if (widgetReorderMode) {
                        Modifier.pointerInput(widgetReorderMode) {
                            detectTapGestures {
                                widgetReorderMode = false
                                commitWidgetDragReorderIfNeeded()
                            }
                        }
                    } else {
                        Modifier.combinedClickable(
                            interactionSource = panelInteractionSource,
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                showSettingsSheet = false
                                showPanelMenu = true
                            },
                        )
                    },
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = layoutMetrics.appListContentStartPaddingDp.dp,
                        end = layoutMetrics.appListContentEndPaddingDp.dp,
                    ).offset(
                        y =
                            if (hasScrollableContent || useCleanHomeWidgetLayout) {
                                0.dp
                            } else {
                                (-layoutMetrics.favoritesCenterBiasUpDp).dp
                            },
                    ).graphicsLayer { translationY = overscrollOffset.value }
                    .pointerInput(reorderMode, widgetReorderMode) {
                        if (reorderMode || widgetReorderMode) return@pointerInput
                        detectVerticalDragGestures(
                            onDragEnd = {
                                didTriggerSearchDuringDrag = false
                                didTriggerAppListDuringDrag = false
                                scope.launch {
                                    overscrollOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec =
                                            spring(
                                                stiffness = FAVORITES_OVERSCROLL_SPRING_STIFFNESS,
                                                dampingRatio = FAVORITES_OVERSCROLL_SPRING_DAMPING,
                                            ),
                                    )
                                }
                            },
                            onDragCancel = {
                                didTriggerSearchDuringDrag = false
                                didTriggerAppListDuringDrag = false
                                scope.launch {
                                    overscrollOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec =
                                            spring(
                                                stiffness = FAVORITES_OVERSCROLL_SPRING_STIFFNESS,
                                                dampingRatio = FAVORITES_OVERSCROLL_SPRING_DAMPING,
                                            ),
                                    )
                                }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                val atTop = scrollState.value == 0
                                val atBottom = scrollState.value == scrollState.maxValue

                                val canNormalScroll =
                                    when {
                                        dragAmount < 0 && !atBottom -> true
                                        dragAmount > 0 && !atTop -> true
                                        else -> false
                                    }

                                if (canNormalScroll) {
                                    scope.launch {
                                        scrollState.scrollTo(
                                            (scrollState.value - dragAmount.toInt()).coerceIn(0, scrollState.maxValue),
                                        )
                                    }
                                } else {
                                    val dampened = dragAmount * FAVORITES_OVERSCROLL_RESISTANCE
                                    scope.launch {
                                        val nextOverscroll = overscrollOffset.value + dampened
                                        overscrollOffset.snapTo(nextOverscroll)
                                        if (!isSearchActive &&
                                            !didTriggerSearchDuringDrag &&
                                            atTop &&
                                            dragAmount > 0f &&
                                            nextOverscroll >= overscrollTriggerPx
                                        ) {
                                            didTriggerSearchDuringDrag = true
                                            onSearchActivated()
                                        }
                                        if (!isSearchActive &&
                                            !didTriggerAppListDuringDrag &&
                                            atBottom &&
                                            dragAmount < 0f &&
                                            nextOverscroll <= -overscrollTriggerPx
                                        ) {
                                            didTriggerAppListDuringDrag = true
                                            onAppListActivated()
                                        }
                                    }
                                }
                            },
                        )
                    }.verticalScroll(scrollState, enabled = false),
            verticalArrangement =
                if (hasScrollableContent || useCleanHomeWidgetLayout) {
                    Arrangement.Top
                } else {
                    Arrangement.Center
                },
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        if (useCleanHomeWidgetLayout) {
                            layoutMetrics.cleanHomeWidgetTopPaddingDp.dp
                        } else {
                            layoutMetrics.favoritesTopMarginDp.dp
                        },
                    ),
            )

            orderedWidgetStacks.forEach { widgetStack ->
                val stackId = widgetStackId(widgetStack)
                key(stackId) {
                    val index = orderedWidgetStacks.indexOfFirst { stack -> widgetStackId(stack) == stackId }
                    val activeRowHeight = widgetRowMetrics[activeDragWidgetStackId]?.height.orZero()
                    val laneShiftY =
                        when {
                            activeDragWidgetStackId == null || index == -1 -> 0f
                            index == activeDragWidgetStartIndex -> 0f
                            activeDragWidgetStartIndex < activeDragWidgetTargetIndex &&
                                index in (activeDragWidgetStartIndex + 1)..activeDragWidgetTargetIndex -> -activeRowHeight
                            activeDragWidgetStartIndex > activeDragWidgetTargetIndex &&
                                index in activeDragWidgetTargetIndex until activeDragWidgetStartIndex -> activeRowHeight
                            else -> 0f
                        }

                    if (widgetReorderMode) {
                        ReorderableWidgetStackRow(
                            widgetStack = widgetStack,
                            stackIndex = index,
                            isActiveDrag = activeDragWidgetStackId == stackId,
                            dragOffsetY =
                                if (activeDragWidgetStackId == stackId) {
                                    activeDragWidgetOffsetY
                                } else {
                                    0f
                                },
                            laneShiftY = laneShiftY,
                            createWidgetView = createWidgetView,
                            getWidgetMinHeightDp = getWidgetMinHeightDp,
                            onAddWidgetToStack = onAddWidgetToStack,
                            onRemoveWidget = onRemoveWidget,
                            onMoveUp = {
                                if (index > 0) {
                                    val moved = orderedWidgetStacks.removeAt(index)
                                    orderedWidgetStacks.add(index - 1, moved)
                                    persistWidgetOrder()
                                }
                            },
                            onMoveDown = {
                                if (index in 0 until orderedWidgetStacks.lastIndex) {
                                    val moved = orderedWidgetStacks.removeAt(index)
                                    orderedWidgetStacks.add(index + 1, moved)
                                    persistWidgetOrder()
                                }
                            },
                            onDragStart = {
                                val freshIndex =
                                    orderedWidgetStacks.indexOfFirst { stack ->
                                        widgetStackId(stack) == stackId
                                    }
                                if (freshIndex == -1) return@ReorderableWidgetStackRow

                                activeDragWidgetStackId = stackId
                                activeDragWidgetStartIndex = freshIndex
                                activeDragWidgetTargetIndex = freshIndex
                                activeDragWidgetOffsetY = 0f
                            },
                            onDragDelta = { deltaY ->
                                if (activeDragWidgetStackId != stackId) return@ReorderableWidgetStackRow
                                activeDragWidgetOffsetY += deltaY
                                updateWidgetDragTarget(stackId)
                            },
                            onDragEnd = {
                                if (activeDragWidgetStackId == stackId) {
                                    commitWidgetDragReorderIfNeeded()
                                }
                            },
                            onMeasured = { metrics ->
                                widgetRowMetrics[stackId] = metrics
                            },
                            modifier = Modifier.graphicsLayer { alpha = favAlpha },
                        )
                    } else {
                        WidgetStackRow(
                            widgetStack = widgetStack,
                            createWidgetView = createWidgetView,
                            getWidgetMinHeightDp = getWidgetMinHeightDp,
                            modifier = Modifier.graphicsLayer { alpha = favAlpha },
                        )
                    }
                }
            }

            if (widgetReorderMode) {
                AddWidgetEditRow(
                    onClick = onAddWidget,
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
                Spacer(modifier = Modifier.height(layoutMetrics.addWidgetBottomPaddingDp.dp))
            }

            if (showFavoriteApps && favorites.isNotEmpty()) {
                SectionHeader(
                    text = stringResource(R.string.favorites).uppercase(),
                    topPaddingDp =
                        if (orderedWidgetStacks.isNotEmpty()) {
                            0f
                        } else {
                            null
                        },
                    bottomPaddingDp =
                        if (orderedWidgetStacks.isNotEmpty()) {
                            0f
                        } else {
                            null
                        },
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )

                orderedFavorites.forEach { app ->
                    key(favoriteComponentId(app)) {
                        if (reorderMode) {
                            val componentId = favoriteComponentId(app)
                            val index =
                                orderedFavorites.indexOfFirst {
                                    favoriteComponentId(it) == componentId
                                }
                            val activeRowHeight = rowMetrics[activeDragComponentId]?.height.orZero()
                            val laneShiftY =
                                when {
                                    activeDragComponentId == null || index == -1 -> 0f
                                    index == activeDragStartIndex -> 0f
                                    activeDragStartIndex < activeDragTargetIndex &&
                                        index in (activeDragStartIndex + 1)..activeDragTargetIndex -> -activeRowHeight
                                    activeDragStartIndex > activeDragTargetIndex &&
                                        index in activeDragTargetIndex until activeDragStartIndex -> activeRowHeight
                                    else -> 0f
                                }

                            ReorderableFavoriteRow(
                                app = app,
                                hideAppIcons = settings.hideAppIcons,
                                isActiveDrag = activeDragComponentId == componentId,
                                dragOffsetY =
                                    if (activeDragComponentId == componentId) {
                                        activeDragOffsetY
                                    } else {
                                        0f
                                    },
                                laneShiftY = laneShiftY,
                                onMoveUp = {
                                    if (index > 0) {
                                        val moved = orderedFavorites.removeAt(index)
                                        orderedFavorites.add(index - 1, moved)
                                        persistFavoriteOrder()
                                    }
                                },
                                onMoveDown = {
                                    if (index in 0 until orderedFavorites.lastIndex) {
                                        val moved = orderedFavorites.removeAt(index)
                                        orderedFavorites.add(index + 1, moved)
                                        persistFavoriteOrder()
                                    }
                                },
                                onDragStart = {
                                    val freshIndex =
                                        orderedFavorites.indexOfFirst {
                                            favoriteComponentId(it) == componentId
                                        }
                                    if (freshIndex == -1) return@ReorderableFavoriteRow

                                    activeDragComponentId = componentId
                                    activeDragStartIndex = freshIndex
                                    activeDragTargetIndex = freshIndex
                                    activeDragOffsetY = 0f
                                },
                                onDragDelta = { deltaY ->
                                    if (activeDragComponentId != componentId) return@ReorderableFavoriteRow
                                    activeDragOffsetY += deltaY
                                    updateDragTarget(componentId)
                                },
                                onDragEnd = {
                                    if (activeDragComponentId == componentId) {
                                        commitDragReorderIfNeeded()
                                    }
                                },
                                onMeasured = { metrics ->
                                    rowMetrics[componentId] = metrics
                                },
                                modifier = Modifier.graphicsLayer { alpha = favAlpha },
                            )
                        } else {
                            AppRow(
                                app = app,
                                isFavorite = true,
                                isHiddenMode = false,
                                onToggleFavorite = onToggleFavorite,
                                onHideApp = onHideApp,
                                onUnhideApp = {},
                                hideAppIcons = settings.hideAppIcons,
                                modifier = Modifier.graphicsLayer { alpha = favAlpha },
                            )
                        }
                    }
                }
                Spacer(
                    modifier =
                        Modifier.height(
                            if (hasScrollableContent) {
                                0.dp
                            } else {
                                layoutMetrics.favoritesBottomSpacerDp.dp
                            },
                        ),
                )
            } else if (showFavoriteApps && orderedWidgetStacks.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_favorites),
                    modifier =
                        Modifier
                            .padding(
                                horizontal = layoutMetrics.rowHorizontalPaddingDp.dp,
                                vertical = layoutMetrics.rowVerticalPaddingDp.dp,
                            ).clickable(onClick = onAppListActivated)
                            .graphicsLayer { alpha = favAlpha * 0.78f },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (showPanelMenu) {
            ModalBottomSheet(
                onDismissRequest = { showPanelMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                FavoritesOptionsSheet(
                    isHiddenMode = isHiddenMode,
                    reorderMode = reorderMode,
                    widgetReorderMode = widgetReorderMode,
                    hasFavorites = showFavoriteApps && orderedFavorites.isNotEmpty(),
                    onHiddenModeClicked = {
                        showPanelMenu = false
                        if (reorderMode) {
                            reorderMode = false
                            commitDragReorderIfNeeded()
                        }
                        if (widgetReorderMode) {
                            widgetReorderMode = false
                            commitWidgetDragReorderIfNeeded()
                        }
                        onHiddenModeChanged(!isHiddenMode)
                    },
                    onReorderWidgetsClicked = {
                        showPanelMenu = false
                        if (reorderMode) {
                            reorderMode = false
                            commitDragReorderIfNeeded()
                        }
                        val nextMode = !widgetReorderMode
                        widgetReorderMode = nextMode
                        if (!nextMode) {
                            commitWidgetDragReorderIfNeeded()
                        }
                    },
                    onReorderFavoritesClicked = {
                        showPanelMenu = false
                        if (widgetReorderMode) {
                            widgetReorderMode = false
                            commitWidgetDragReorderIfNeeded()
                        }
                        val nextMode = !reorderMode
                        reorderMode = nextMode
                        if (!nextMode) {
                            commitDragReorderIfNeeded()
                        }
                    },
                    onSettingsClicked = {
                        openSettingsSheet()
                    },
                )
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { closeSettingsSheet(reopenFavorites = false) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                SettingsSheet(
                    settings = settings,
                    onHideStatusBarChanged = onHideStatusBarChanged,
                    onHideAppIconsChanged = onHideAppIconsChanged,
                    onHideSearchButtonChanged = onHideSearchButtonChanged,
                    onCleanHomeScreenChanged = onCleanHomeScreenChanged,
                    onHomeRowClicked = ::openHomeRowSheet,
                    onFontClicked = ::openFontSheet,
                    onResetClicked = {
                        onResetSettings()
                    },
                    onBackClicked = {
                        closeSettingsSheet(reopenFavorites = true)
                    },
                )
            }
        }

        if (showFontSheet) {
            ModalBottomSheet(
                onDismissRequest = ::closeFontSheet,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                FontSheet(
                    selectedFont = settings.font,
                    onFontSelected = { font ->
                        onFontChanged(font)
                        closeFontSheet()
                    },
                    onBackClicked = ::closeFontSheet,
                )
            }
        }

        if (showHomeRowSheet) {
            ModalBottomSheet(
                onDismissRequest = ::closeHomeRowSheet,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                HomeRowSheet(
                    selectedMode = settings.homeRowNavigationMode,
                    onModeSelected = { mode ->
                        onHomeRowNavigationModeChanged(mode)
                        closeHomeRowSheet()
                    },
                    onBackClicked = ::closeHomeRowSheet,
                )
            }
        }
    }
}
