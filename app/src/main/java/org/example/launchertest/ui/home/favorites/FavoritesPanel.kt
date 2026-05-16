package org.example.launchertest.ui.home.favorites

import android.appwidget.AppWidgetHostView
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.launchertest.data.HomeRowNavigationMode
import org.example.launchertest.data.LauncherFont
import org.example.launchertest.data.LauncherSettings
import org.example.launchertest.ui.home.HomeLayoutMetrics
import org.example.launchertest.ui.home.shared.AppRow
import org.example.launchertest.ui.home.shared.HOME_LIST_SEARCH_DRAG_THRESHOLD_DP
import org.example.launchertest.ui.home.shared.HOME_LIST_SECTION_HEADER_BOTTOM_PADDING_DP
import org.example.launchertest.ui.home.shared.HOME_LIST_SECTION_HEADER_TOP_PADDING_DP
import org.example.launchertest.ui.home.shared.HOME_ROW_CONTENT_SCALE
import org.example.launchertest.ui.home.shared.HOME_ROW_FAVORITE_ICON_SIZE_DP
import org.example.launchertest.ui.home.shared.HOME_ROW_HORIZONTAL_PADDING_DP
import org.example.launchertest.ui.home.shared.HOME_ROW_ICON_SPACING_DP
import org.example.launchertest.ui.home.shared.HOME_ROW_TEXT_SCALE
import org.example.launchertest.ui.home.shared.HOME_ROW_VERTICAL_PADDING_DP
import org.example.launchertest.ui.home.shared.SectionHeader
import org.example.launchertest.ui.home.shared.rememberAppIcon
import org.example.launchertest.ui.theme.toPreviewFontFamily
import org.example.launchertest.ui.model.LauncherApp
import org.example.launchertest.widgets.WidgetStack
import kotlin.math.hypot

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
    onHomeRowNavigationModeChanged: (HomeRowNavigationMode) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onResetSettings: () -> Unit,
    onRestartLauncher: () -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    layoutMetrics: HomeLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val overscrollTriggerPx = with(density) { HOME_LIST_SEARCH_DRAG_THRESHOLD_DP.dp.toPx() }
    val hasScrollableContent = scrollState.maxValue > 0
    var showPanelMenu by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }
    var showHomeRowSheet by remember { mutableStateOf(false) }
    var hideStatusBarAtSettingsOpen by remember { mutableStateOf<Boolean?>(null) }
    var hideStatusBarSettingsDraft by remember { mutableStateOf<Boolean?>(null) }
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
    var activeDragWidgetStackId by remember { mutableIntStateOf(-1) }
    var activeDragWidgetStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetOffsetY by remember { mutableFloatStateOf(0f) }
    val widgetRowMetrics = remember { mutableStateMapOf<Int, FavoriteRowMetrics>() }
    val orderedWidgetStacks = remember { mutableStateListOf<WidgetStack>() }

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
        hideStatusBarAtSettingsOpen = settings.hideStatusBar
        hideStatusBarSettingsDraft = settings.hideStatusBar
        showPanelMenu = false
        showHomeRowSheet = false
        showSettingsSheet = true
    }

    fun closeSettingsSheet(reopenFavorites: Boolean) {
        val didHideStatusBarChange = hideStatusBarAtSettingsOpen != null &&
            hideStatusBarAtSettingsOpen != hideStatusBarSettingsDraft

        showSettingsSheet = false
        showFontSheet = false
        showHomeRowSheet = false
        hideStatusBarAtSettingsOpen = null
        hideStatusBarSettingsDraft = null
        showPanelMenu = reopenFavorites

        if (didHideStatusBarChange) {
            scope.launch {
                delay(250)
                onRestartLauncher()
            }
        }
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

        val currentIndex = orderedFavorites.indexOfFirst {
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
        activeDragWidgetStackId = -1
        activeDragWidgetStartIndex = -1
        activeDragWidgetTargetIndex = -1
        activeDragWidgetOffsetY = 0f
    }

    fun commitWidgetDragReorderIfNeeded() {
        val activeStackId = activeDragWidgetStackId
        if (activeStackId == -1 || activeDragWidgetTargetIndex == -1) {
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

    fun updateWidgetDragTarget(stackId: Int) {
        val startIndex = activeDragWidgetStartIndex
        if (startIndex !in orderedWidgetStacks.indices) return

        val activeMetrics = widgetRowMetrics[stackId] ?: return
        val draggedCenterY = activeMetrics.centerY + activeDragWidgetOffsetY
        var targetIndex = startIndex

        if (activeDragWidgetOffsetY > 0f && startIndex < orderedWidgetStacks.lastIndex) {
            for (index in (startIndex + 1)..orderedWidgetStacks.lastIndex) {
                val candidateId = widgetStackId(orderedWidgetStacks[index])
                val metrics = widgetRowMetrics[candidateId] ?: continue
                val replaceThresholdY = metrics.topY +
                    (metrics.height * FAVORITES_REORDER_SWAP_FRACTION_OF_ROW)

                if (draggedCenterY >= replaceThresholdY) {
                    targetIndex = index
                }
            }
        } else if (activeDragWidgetOffsetY < 0f && startIndex > 0) {
            for (index in (startIndex - 1) downTo 0) {
                val candidateId = widgetStackId(orderedWidgetStacks[index])
                val metrics = widgetRowMetrics[candidateId] ?: continue
                val replaceThresholdY = metrics.topY +
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
                val replaceThresholdY = metrics.topY +
                    (metrics.height * FAVORITES_REORDER_SWAP_FRACTION_OF_ROW)

                if (draggedCenterY >= replaceThresholdY) {
                    targetIndex = index
                }
            }
        } else if (activeDragOffsetY < 0f && startIndex > 0) {
            for (index in (startIndex - 1) downTo 0) {
                val candidateId = favoriteComponentId(orderedFavorites[index])
                val metrics = rowMetrics[candidateId] ?: continue
                val replaceThresholdY = metrics.topY +
                    (metrics.height * (1f - FAVORITES_REORDER_SWAP_FRACTION_OF_ROW))

                if (draggedCenterY <= replaceThresholdY) {
                    targetIndex = index
                }
            }
        }

        activeDragTargetIndex = targetIndex
    }

    Box(
        modifier = modifier
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
                }
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = layoutMetrics.appListContentStartPaddingDp.dp,
                    end = layoutMetrics.appListContentEndPaddingDp.dp,
                )
                .offset(
                    y = if (hasScrollableContent) {
                        0.dp
                    } else {
                        (-layoutMetrics.favoritesCenterBiasUpDp).dp
                    },
                )
                .graphicsLayer { translationY = overscrollOffset.value }
                .pointerInput(reorderMode, widgetReorderMode) {
                    if (reorderMode || widgetReorderMode) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
                            didTriggerSearchDuringDrag = false
                            didTriggerAppListDuringDrag = false
                            scope.launch {
                                overscrollOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
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
                                    animationSpec = spring(
                                        stiffness = FAVORITES_OVERSCROLL_SPRING_STIFFNESS,
                                        dampingRatio = FAVORITES_OVERSCROLL_SPRING_DAMPING,
                                    ),
                                )
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            val atTop = scrollState.value == 0
                            val atBottom = scrollState.value == scrollState.maxValue

                            val canNormalScroll = when {
                                dragAmount < 0 && !atBottom -> true
                                dragAmount > 0 && !atTop    -> true
                                else                        -> false
                            }

                            if (canNormalScroll) {
                                scope.launch {
                                    scrollState.scrollTo(
                                        (scrollState.value - dragAmount.toInt()).coerceIn(0, scrollState.maxValue)
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
                }
                .verticalScroll(scrollState, enabled = false),
            verticalArrangement = if (hasScrollableContent) {
                Arrangement.Top
            } else {
                Arrangement.Center
            },
        ) {
            Spacer(modifier = Modifier.height(layoutMetrics.favoritesTopMarginDp.dp))

            orderedWidgetStacks.forEach { widgetStack ->
                val stackId = widgetStackId(widgetStack)
                key(stackId) {
                    val index = orderedWidgetStacks.indexOfFirst { stack -> widgetStackId(stack) == stackId }
                    val activeRowHeight = widgetRowMetrics[activeDragWidgetStackId]?.height.orZero()
                    val laneShiftY = when {
                        activeDragWidgetStackId == -1 || index == -1 -> 0f
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
                            dragOffsetY = if (activeDragWidgetStackId == stackId) {
                                activeDragWidgetOffsetY
                            } else {
                                0f
                            },
                            laneShiftY = laneShiftY,
                            createWidgetView = createWidgetView,
                            getWidgetMinHeightDp = getWidgetMinHeightDp,
                            onAddWidgetToStack = onAddWidgetToStack,
                            onRemoveWidget = onRemoveWidget,
                            onDragStart = {
                                val freshIndex = orderedWidgetStacks.indexOfFirst { stack ->
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
                Spacer(modifier = Modifier.height(ADD_WIDGET_BOTTOM_PADDING_DP.dp))
            }

            if (favorites.isNotEmpty()) {
                SectionHeader(
                    text = "FAVORITES",
                    topPaddingDp = if (orderedWidgetStacks.isNotEmpty()) {
                        0f
                    } else {
                        HOME_LIST_SECTION_HEADER_TOP_PADDING_DP
                    },
                    bottomPaddingDp = if (orderedWidgetStacks.isNotEmpty()) {
                        0f
                    } else {
                        HOME_LIST_SECTION_HEADER_BOTTOM_PADDING_DP
                    },
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )

                orderedFavorites.forEach { app ->
                    key(favoriteComponentId(app)) {
                        if (reorderMode) {
                            val componentId = favoriteComponentId(app)
                            val index = orderedFavorites.indexOfFirst {
                                favoriteComponentId(it) == componentId
                            }
                            val activeRowHeight = rowMetrics[activeDragComponentId]?.height.orZero()
                            val laneShiftY = when {
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
                                dragOffsetY = if (activeDragComponentId == componentId) {
                                    activeDragOffsetY
                                } else {
                                    0f
                                },
                                laneShiftY = laneShiftY,
                                onDragStart = {
                                    val freshIndex = orderedFavorites.indexOfFirst {
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
                    modifier = Modifier.height(
                        if (hasScrollableContent) {
                            0.dp
                        } else {
                            APP_LIST_FAVORITES_BOTTOM_SPACER_DP.dp
                        },
                    ),
                )
            } else if (orderedWidgetStacks.isEmpty()) {
                Text(
                    text = "No favorites yet. Long-press any app and choose Favorite.",
                    modifier = Modifier
                        .padding(
                            horizontal = HOME_ROW_HORIZONTAL_PADDING_DP.dp,
                            vertical = HOME_ROW_VERTICAL_PADDING_DP.dp,
                        )
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
                    hasFavorites = orderedFavorites.isNotEmpty(),
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
                    settings = settings.copy(
                        hideStatusBar = hideStatusBarSettingsDraft ?: settings.hideStatusBar,
                    ),
                    onHideStatusBarChanged = { enabled ->
                        hideStatusBarSettingsDraft = enabled
                        onHideStatusBarChanged(enabled)
                    },
                    onHideAppIconsChanged = onHideAppIconsChanged,
                    onHomeRowClicked = ::openHomeRowSheet,
                    onFontClicked = ::openFontSheet,
                    onResetClicked = {
                        hideStatusBarSettingsDraft = LauncherSettings().hideStatusBar
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

@Composable
private fun FavoritesOptionsSheet(
    isHiddenMode: Boolean,
    reorderMode: Boolean,
    widgetReorderMode: Boolean,
    hasFavorites: Boolean,
    onHiddenModeClicked: () -> Unit,
    onReorderWidgetsClicked: () -> Unit,
    onReorderFavoritesClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SheetActionRow(
            icon = if (isHiddenMode) "×" else "◌",
            title = if (isHiddenMode) "Exit unhide mode" else "Unhide mode",
            subtitle = if (isHiddenMode) {
                "Return to normal apps"
            } else {
                "Show hidden apps and hide normal apps"
            },
            active = isHiddenMode,
            onClick = onHiddenModeClicked,
        )

        SheetActionRow(
            icon = "↕",
            title = if (widgetReorderMode) "Done editing widgets" else "Edit widgets",
            subtitle = if (widgetReorderMode) {
                "Save widget order and removals"
            } else {
                "Reorder, remove, or add widgets"
            },
            active = widgetReorderMode,
            onClick = onReorderWidgetsClicked,
        )

        if (hasFavorites) {
            SheetActionRow(
                icon = "↕",
                title = if (reorderMode) "Done reordering" else "Reorder favorites",
                subtitle = if (reorderMode) {
                    "Save the current favorite order"
                } else {
                    "Drag favorites into a custom order"
                },
                active = reorderMode,
                onClick = onReorderFavoritesClicked,
            )
        }

        SheetActionRow(
            icon = "⚙",
            title = "Settings",
            subtitle = "Launcher settings",
            onClick = onSettingsClicked,
        )
    }
}

@Composable
private fun SettingsSheet(
    settings: LauncherSettings,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onHomeRowClicked: () -> Unit,
    onFontClicked: () -> Unit,
    onResetClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = "Back",
            subtitle = "Return to Favorites",
            onClick = onBackClicked,
        )

        SettingsToggleRow(
            title = "Hide the statusbar",
            subtitle = "Restarts the launcher when Settings closes",
            checked = settings.hideStatusBar,
            onCheckedChange = onHideStatusBarChanged,
        )

        SettingsToggleRow(
            title = "Hide app icons",
            subtitle = "Show app names without icons",
            checked = settings.hideAppIcons,
            onCheckedChange = onHideAppIconsChanged,
        )

        SheetActionRow(
            icon = "⌂",
            title = "Home row",
            subtitle = settings.homeRowNavigationMode.displayName,
            onClick = onHomeRowClicked,
        )

        SheetActionRow(
            icon = "Aa",
            title = "Font",
            subtitle = settings.font.displayName,
            onClick = onFontClicked,
        )

        SheetActionRow(
            icon = "↺",
            title = "Reset to default",
            subtitle = "Restore original launcher settings",
            onClick = onResetClicked,
        )
    }
}

@Composable
private fun HomeRowSheet(
    selectedMode: HomeRowNavigationMode,
    onModeSelected: (HomeRowNavigationMode) -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Home row",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = "Back",
            subtitle = "Return to Settings",
            onClick = onBackClicked,
        )

        HomeRowNavigationMode.entries.forEach { mode ->
            SheetActionRow(
                icon = if (mode == selectedMode) "✓" else " ",
                title = mode.displayName,
                subtitle = mode.description,
                active = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
private fun FontSheet(
    selectedFont: LauncherFont,
    onFontSelected: (LauncherFont) -> Unit,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        Text(
            text = "Font",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = "Back",
            subtitle = "Return to Settings",
            onClick = onBackClicked,
        )

        LauncherFont.entries.forEach { font ->
            FontOptionRow(
                icon = if (font == selectedFont) "✓" else " ",
                font = font,
                subtitle = if (font == LauncherFont.System) {
                    "Use Android's default font"
                } else {
                    "Preview in ${font.displayName}"
                },
                active = font == selectedFont,
                onClick = { onFontSelected(font) },
            )
        }
    }
}

@Composable
private fun FontOptionRow(
    icon: String,
    font: LauncherFont,
    subtitle: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(40.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = font.displayName,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = font.toPreviewFontFamily(),
                ),
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SheetActionRow(
    icon: String,
    title: String,
    subtitle: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(40.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetStackRow(
    widgetStack: WidgetStack,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    modifier: Modifier = Modifier,
) {
    WidgetStackContent(
        widgetStack = widgetStack,
        createWidgetView = createWidgetView,
        getWidgetMinHeightDp = getWidgetMinHeightDp,
        showAddPlaceholder = false,
        onWidgetHoldRelease = null,
        modifier = modifier
            .fillMaxWidth(),
    )
}

@Composable
private fun AddWidgetEditRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = APP_WIDGET_ROW_START_PADDING_DP.dp,
                end = APP_WIDGET_ROW_END_PADDING_DP.dp,
                top = APP_WIDGET_ROW_TOP_PADDING_DP.dp,
                bottom = APP_WIDGET_ROW_BOTTOM_PADDING_DP.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
        ) {
            Text(
                text = "Add stack",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Place a new widget stack above Favorites",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableWidgetStackRow(
    widgetStack: WidgetStack,
    stackIndex: Int,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    onAddWidgetToStack: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var dragDistancePx by remember { mutableFloatStateOf(0f) }
    val pagerState = rememberPagerState(pageCount = { widgetStack.widgetIds.size + 1 })
    val currentWidgetId = widgetStack.widgetIds.getOrNull(pagerState.currentPage)
    val density = LocalDensity.current
    val contextMenuDragThresholdPx = with(density) {
        WIDGET_CONTEXT_MENU_DRAG_THRESHOLD_DP.dp.toPx()
    }
    val settleSpec = spring<Float>(
        stiffness = FAVORITES_REORDER_SETTLE_STIFFNESS,
        dampingRatio = FAVORITES_REORDER_SETTLE_DAMPING,
    )
    val activeScale by animateFloatAsState(
        targetValue = if (isActiveDrag) FAVORITES_REORDER_ACTIVE_SCALE else 1f,
        animationSpec = settleSpec,
        label = "widgetReorderScale",
    )
    val activeTint by animateFloatAsState(
        targetValue = if (isActiveDrag) FAVORITES_REORDER_ACTIVE_TINT_ALPHA else 0f,
        animationSpec = settleSpec,
        label = "widgetReorderTint",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = dragOffsetY + laneShiftY,
        animationSpec = settleSpec,
        label = "widgetReorderTranslationY",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isActiveDrag) 1f else 0f)
            .graphicsLayer {
                translationY = animatedTranslationY
                scaleX = activeScale
                scaleY = activeScale
                shadowElevation = if (isActiveDrag) {
                    FAVORITES_REORDER_ACTIVE_SHADOW_Y_DP.dp.toPx()
                } else {
                    0f
                }
            }
            .onGloballyPositioned { coordinates ->
                onMeasured(
                    FavoriteRowMetrics(
                        topY = coordinates.positionInParent().y,
                        height = coordinates.size.height.toFloat(),
                    )
                )
            }
            .pointerInput(widgetStack, currentWidgetId, contextMenuDragThresholdPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragDistancePx = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        if (dragDistancePx < contextMenuDragThresholdPx && currentWidgetId != null) {
                            showContextMenu = true
                        }
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistancePx += dragAmount.getDistance()
                        onDragDelta(dragAmount.y)
                    },
                )
            }
            .background(
                color = Color.White.copy(alpha = activeTint),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(
                start = APP_WIDGET_ROW_START_PADDING_DP.dp,
                end = APP_WIDGET_ROW_END_PADDING_DP.dp,
                top = APP_WIDGET_ROW_TOP_PADDING_DP.dp,
                bottom = APP_WIDGET_ROW_BOTTOM_PADDING_DP.dp,
            ),
    ) {
        WidgetStackContent(
            widgetStack = widgetStack,
            createWidgetView = createWidgetView,
            getWidgetMinHeightDp = getWidgetMinHeightDp,
            showAddPlaceholder = true,
            onAddWidgetToStack = { onAddWidgetToStack(stackIndex) },
            onWidgetHoldRelease = { showContextMenu = true },
            pagerState = pagerState,
            modifier = Modifier.fillMaxWidth(),
        )

        EditDragHandle(
            text = "↕",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Remove") },
                onClick = {
                    showContextMenu = false
                    currentWidgetId?.let(onRemoveWidget)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetStackContent(
    widgetStack: WidgetStack,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    showAddPlaceholder: Boolean,
    onAddWidgetToStack: (() -> Unit)? = null,
    onWidgetHoldRelease: (() -> Unit)?,
    modifier: Modifier = Modifier,
    pagerState: androidx.compose.foundation.pager.PagerState = rememberPagerState(
        pageCount = { widgetStack.widgetIds.size },
    ),
) {
    val stackHeightDp = remember(widgetStack, getWidgetMinHeightDp) {
        widgetStack.widgetIds
            .mapNotNull(getWidgetMinHeightDp)
            .maxOrNull()
            ?.coerceAtLeast(APP_WIDGET_MIN_HEIGHT_DP.toInt())
            ?: APP_WIDGET_MIN_HEIGHT_DP.toInt()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            start = APP_WIDGET_ROW_START_PADDING_DP.dp,
            end = APP_WIDGET_ROW_END_PADDING_DP.dp,
            top = APP_WIDGET_ROW_TOP_PADDING_DP.dp,
            bottom = APP_WIDGET_ROW_BOTTOM_PADDING_DP.dp,
        ),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val appWidgetId = widgetStack.widgetIds.getOrNull(page)
            if (appWidgetId != null) {
                AndroidView(
                    factory = { context -> createWidgetView(appWidgetId) ?: FrameLayout(context) },
                    update = { view ->
                        installWidgetHoldReleaseMenu(
                            view = view,
                            enabled = onWidgetHoldRelease != null,
                            onHoldRelease = onWidgetHoldRelease,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stackHeightDp.dp),
                )
            } else if (showAddPlaceholder && onAddWidgetToStack != null) {
                AddWidgetToStackPlaceholder(
                    onClick = onAddWidgetToStack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stackHeightDp.dp),
                )
            }
        }

        val pageCount = widgetStack.widgetIds.size + if (showAddPlaceholder) 1 else 0
        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                repeat(pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 5.dp else 4.dp)
                            .background(
                                color = Color.White.copy(
                                    alpha = if (pagerState.currentPage == index) 0.82f else 0.38f,
                                ),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWidgetToStackPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "+",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Add widget",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun installWidgetHoldReleaseMenu(
    view: View,
    enabled: Boolean,
    onHoldRelease: (() -> Unit)?,
) {
    if (!enabled || onHoldRelease == null) {
        clearWidgetHoldReleaseMenu(view)
        return
    }

    view.setOnLongClickListener { true }
    view.setOnTouchListener(
        WidgetHoldReleaseTouchListener(
            onHoldRelease = onHoldRelease,
        ),
    )

    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            installWidgetHoldReleaseMenu(
                view = view.getChildAt(index),
                enabled = true,
                onHoldRelease = onHoldRelease,
            )
        }
    }
}

private fun clearWidgetHoldReleaseMenu(view: View) {
    view.setOnLongClickListener(null)
    view.setOnTouchListener(null)

    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            clearWidgetHoldReleaseMenu(view.getChildAt(index))
        }
    }
}

private class WidgetHoldReleaseTouchListener(
    private val onHoldRelease: () -> Unit,
) : View.OnTouchListener {
    private var downX = 0f
    private var downY = 0f
    private var longPressReached = false
    private var cancelled = false
    private var longPressRunnable: Runnable? = null

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressReached = false
                cancelled = false
                longPressRunnable = Runnable {
                    longPressReached = true
                }.also { runnable ->
                    view.postDelayed(runnable, ViewConfiguration.getLongPressTimeout().toLong())
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val distance = hypot(
                    (event.x - downX).toDouble(),
                    (event.y - downY).toDouble(),
                ).toFloat()
                if (distance > view.context.scaledTouchSlopForWidgets()) {
                    cancelled = true
                    cancelLongPressRunnable(view)
                }
                return longPressReached && !cancelled
            }

            MotionEvent.ACTION_UP -> {
                val shouldOpenMenu = longPressReached && !cancelled
                cancelLongPressRunnable(view)
                if (shouldOpenMenu) {
                    onHoldRelease()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelLongPressRunnable(view)
                return false
            }
        }

        return false
    }

    private fun cancelLongPressRunnable(view: View) {
        longPressRunnable?.let(view::removeCallbacks)
        longPressRunnable = null
    }
}

private fun android.content.Context.scaledTouchSlopForWidgets(): Float {
    return ViewConfiguration.get(this).scaledTouchSlop.toFloat()
}

@Composable
private fun EditDragHandle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.84f),
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier
            .padding(horizontal = 6.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableFavoriteRow(
    app: LauncherApp,
    hideAppIcons: Boolean,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = if (hideAppIcons) null else rememberAppIcon(app.packageName)

    val settleSpec = spring<Float>(
        stiffness = FAVORITES_REORDER_SETTLE_STIFFNESS,
        dampingRatio = FAVORITES_REORDER_SETTLE_DAMPING,
    )
    val activeScale by animateFloatAsState(
        targetValue = if (isActiveDrag) FAVORITES_REORDER_ACTIVE_SCALE else 1f,
        animationSpec = settleSpec,
        label = "reorderScale",
    )
    val activeTint by animateFloatAsState(
        targetValue = if (isActiveDrag) FAVORITES_REORDER_ACTIVE_TINT_ALPHA else 0f,
        animationSpec = settleSpec,
        label = "reorderTint",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = dragOffsetY + laneShiftY,
        animationSpec = settleSpec,
        label = "reorderTranslationY",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = animatedTranslationY
                scaleX = activeScale
                scaleY = activeScale
                alpha = 1f
                shadowElevation = if (isActiveDrag) {
                    FAVORITES_REORDER_ACTIVE_SHADOW_Y_DP.dp.toPx()
                } else {
                    0f
                }
            }
            .onGloballyPositioned { coordinates ->
                onMeasured(
                    FavoriteRowMetrics(
                        topY = coordinates.positionInParent().y,
                        height = coordinates.size.height.toFloat(),
                    )
                )
            }
            .pointerInput(app.packageName, app.activityName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDragStart()
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.y)
                    },
                )
            }
            .background(
                color = Color.White.copy(alpha = activeTint),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(
                horizontal = HOME_ROW_HORIZONTAL_PADDING_DP.dp,
                vertical = HOME_ROW_VERTICAL_PADDING_DP.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy((HOME_ROW_ICON_SPACING_DP * HOME_ROW_CONTENT_SCALE).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size((HOME_ROW_FAVORITE_ICON_SIZE_DP * HOME_ROW_CONTENT_SCALE).dp),
            )
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * HOME_ROW_TEXT_SCALE,
            ),
            modifier = Modifier.weight(1f),
        )

        EditDragHandle(
            text = "↕",
        )
    }
}

private data class FavoriteRowMetrics(
    val topY: Float,
    val height: Float,
) {
    val centerY: Float
        get() = topY + (height / 2f)
}

private fun favoriteComponentId(app: LauncherApp): String = "${app.packageName}/${app.activityName}"

private fun widgetStackId(stack: WidgetStack): Int = stack.widgetIds.firstOrNull() ?: -1

private fun Float?.orZero(): Float = this ?: 0f
