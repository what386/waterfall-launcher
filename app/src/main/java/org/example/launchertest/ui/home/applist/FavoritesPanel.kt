package org.example.launchertest.ui.home.applist

import android.appwidget.AppWidgetHostView
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.example.launchertest.ui.home.shared.rememberAppIcon
import org.example.launchertest.ui.model.LauncherApp

/**
 * The favorites panel: widgets + favorite app rows, all in a vertically scrollable column
 * that overscrolls and springs back to center like the A–Z rail does.
 *
 * Overscroll resistance: drag distance is dampened by [FAVORITES_OVERSCROLL_RESISTANCE]
 * so the panel feels springy but not free-floating.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesPanel(
    favorites: List<LauncherApp>,
    widgetIds: List<Int>,
    favAlpha: Float,
    isSearchActive: Boolean,
    isHiddenMode: Boolean,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onReorderFavorites: (List<LauncherApp>) -> Unit,
    onSearchActivated: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgets: (List<Int>) -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val searchTriggerPx = with(density) { APP_LIST_SEARCH_DRAG_THRESHOLD_DP.dp.toPx() }
    var showPanelMenu by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var widgetReorderMode by remember { mutableStateOf(false) }
    var didTriggerSearchDuringDrag by remember { mutableStateOf(false) }
    val panelInteractionSource = remember { MutableInteractionSource() }
    var activeDragComponentId by remember { mutableStateOf<String?>(null) }
    var activeDragStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowMetrics = remember { mutableStateMapOf<String, FavoriteRowMetrics>() }
    val orderedFavorites = remember { mutableStateListOf<LauncherApp>() }
    var activeDragWidgetId by remember { mutableIntStateOf(-1) }
    var activeDragWidgetStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragWidgetOffsetY by remember { mutableFloatStateOf(0f) }
    val widgetRowMetrics = remember { mutableStateMapOf<Int, FavoriteRowMetrics>() }
    val orderedWidgetIds = remember { mutableStateListOf<Int>() }

    LaunchedEffect(favorites) {
        val incomingById = favorites.associateBy(::favoriteComponentId)
        val currentIds = orderedFavorites.map(::favoriteComponentId)
        val kept = currentIds.mapNotNull(incomingById::get)
        val appended = favorites.filter { favoriteComponentId(it) !in currentIds.toSet() }
        orderedFavorites.clear()
        orderedFavorites.addAll(kept + appended)
    }

    LaunchedEffect(widgetIds) {
        val incomingIds = widgetIds.toSet()
        val kept = orderedWidgetIds.filter { it in incomingIds }
        val appended = widgetIds.filter { it !in orderedWidgetIds.toSet() }
        orderedWidgetIds.clear()
        orderedWidgetIds.addAll(kept + appended)
    }

    fun persistFavoriteOrder() {
        onReorderFavorites(orderedFavorites.toList())
    }

    fun persistWidgetOrder() {
        onReorderWidgets(orderedWidgetIds.toList())
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
        activeDragWidgetId = -1
        activeDragWidgetStartIndex = -1
        activeDragWidgetTargetIndex = -1
        activeDragWidgetOffsetY = 0f
    }

    fun commitWidgetDragReorderIfNeeded() {
        val activeWidgetId = activeDragWidgetId
        if (activeWidgetId == -1 || activeDragWidgetTargetIndex == -1) {
            clearWidgetDragState()
            return
        }

        val currentIndex = orderedWidgetIds.indexOf(activeWidgetId)

        if (currentIndex != -1 &&
            currentIndex != activeDragWidgetTargetIndex &&
            activeDragWidgetTargetIndex in orderedWidgetIds.indices
        ) {
            val moved = orderedWidgetIds.removeAt(currentIndex)
            orderedWidgetIds.add(activeDragWidgetTargetIndex, moved)
            persistWidgetOrder()
        }

        clearWidgetDragState()
    }

    fun updateWidgetDragTarget(appWidgetId: Int) {
        val startIndex = activeDragWidgetStartIndex
        if (startIndex !in orderedWidgetIds.indices) return

        val activeMetrics = widgetRowMetrics[appWidgetId] ?: return
        val draggedCenterY = activeMetrics.centerY + activeDragWidgetOffsetY
        var targetIndex = startIndex

        if (activeDragWidgetOffsetY > 0f && startIndex < orderedWidgetIds.lastIndex) {
            for (index in (startIndex + 1)..orderedWidgetIds.lastIndex) {
                val candidateId = orderedWidgetIds[index]
                val metrics = widgetRowMetrics[candidateId] ?: continue
                val replaceThresholdY = metrics.topY +
                    (metrics.height * FAVORITES_REORDER_SWAP_FRACTION_OF_ROW)

                if (draggedCenterY >= replaceThresholdY) {
                    targetIndex = index
                }
            }
        } else if (activeDragWidgetOffsetY < 0f && startIndex > 0) {
            for (index in (startIndex - 1) downTo 0) {
                val candidateId = orderedWidgetIds[index]
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
                        onLongClick = { showPanelMenu = true },
                    )
                }
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = APP_LIST_CONTENT_START_PADDING_DP.dp,
                    end = APP_LIST_CONTENT_END_PADDING_DP.dp,
                )
                .offset(y = (-FAVORITES_CENTER_BIAS_UP_DP).dp)
                .graphicsLayer { translationY = overscrollOffset.value }
                .pointerInput(reorderMode, widgetReorderMode) {
                    if (reorderMode || widgetReorderMode) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
                            didTriggerSearchDuringDrag = false
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
                                        nextOverscroll >= searchTriggerPx
                                    ) {
                                        didTriggerSearchDuringDrag = true
                                        onSearchActivated()
                                    }
                                }
                            }
                        },
                    )
                }
                .verticalScroll(scrollState, enabled = false),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(FAVORITES_PANEL_TOP_MARGIN_DP.dp))

            orderedWidgetIds.forEach { appWidgetId ->
                key(appWidgetId) {
                    val index = orderedWidgetIds.indexOf(appWidgetId)
                    val activeRowHeight = widgetRowMetrics[activeDragWidgetId]?.height.orZero()
                    val laneShiftY = when {
                        activeDragWidgetId == -1 || index == -1 -> 0f
                        index == activeDragWidgetStartIndex -> 0f
                        activeDragWidgetStartIndex < activeDragWidgetTargetIndex &&
                            index in (activeDragWidgetStartIndex + 1)..activeDragWidgetTargetIndex -> -activeRowHeight
                        activeDragWidgetStartIndex > activeDragWidgetTargetIndex &&
                            index in activeDragWidgetTargetIndex until activeDragWidgetStartIndex -> activeRowHeight
                        else -> 0f
                    }

                    if (widgetReorderMode) {
                        ReorderableWidgetRow(
                            appWidgetId = appWidgetId,
                            isActiveDrag = activeDragWidgetId == appWidgetId,
                            dragOffsetY = if (activeDragWidgetId == appWidgetId) {
                                activeDragWidgetOffsetY
                            } else {
                                0f
                            },
                            laneShiftY = laneShiftY,
                            createWidgetView = createWidgetView,
                            onRemoveWidget = onRemoveWidget,
                            onDragStart = {
                                val freshIndex = orderedWidgetIds.indexOf(appWidgetId)
                                if (freshIndex == -1) return@ReorderableWidgetRow

                                activeDragWidgetId = appWidgetId
                                activeDragWidgetStartIndex = freshIndex
                                activeDragWidgetTargetIndex = freshIndex
                                activeDragWidgetOffsetY = 0f
                            },
                            onDragDelta = { deltaY ->
                                if (activeDragWidgetId != appWidgetId) return@ReorderableWidgetRow
                                activeDragWidgetOffsetY += deltaY
                                updateWidgetDragTarget(appWidgetId)
                            },
                            onDragEnd = {
                                if (activeDragWidgetId == appWidgetId) {
                                    commitWidgetDragReorderIfNeeded()
                                }
                            },
                            onMeasured = { metrics ->
                                widgetRowMetrics[appWidgetId] = metrics
                            },
                            modifier = Modifier.graphicsLayer { alpha = favAlpha },
                        )
                    } else {
                        WidgetRow(
                            appWidgetId = appWidgetId,
                            createWidgetView = createWidgetView,
                            modifier = Modifier.graphicsLayer { alpha = favAlpha },
                        )
                    }
                }
            }

            if (orderedWidgetIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(ADD_WIDGET_BOTTOM_PADDING_DP.dp))
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
                                modifier = Modifier.graphicsLayer { alpha = favAlpha },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(APP_LIST_FAVORITES_BOTTOM_SPACER_DP.dp))
            } else if (orderedWidgetIds.isEmpty()) {
                Text(
                    text = "No favorites yet. Long-press any app and choose Favorite.",
                    modifier = Modifier
                        .padding(
                            horizontal = APP_ROW_HORIZONTAL_PADDING_DP.dp,
                            vertical = APP_ROW_VERTICAL_PADDING_DP.dp,
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
private fun WidgetRow(
    appWidgetId: Int,
    createWidgetView: (Int) -> AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = APP_WIDGET_ROW_START_PADDING_DP.dp,
                end = APP_WIDGET_ROW_END_PADDING_DP.dp,
                top = APP_WIDGET_ROW_TOP_PADDING_DP.dp,
                bottom = APP_WIDGET_ROW_BOTTOM_PADDING_DP.dp,
            ),
    ) {
        AndroidView(
            factory = { context -> createWidgetView(appWidgetId) ?: FrameLayout(context) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = APP_WIDGET_MIN_HEIGHT_DP.dp),
        )
    }
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
                text = "Add widget",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Place another widget above Favorites",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableWidgetRow(
    appWidgetId: Int,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    createWidgetView: (Int) -> AppWidgetHostView?,
    onRemoveWidget: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }
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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = { showContextMenu = true },
            )
            .pointerInput(appWidgetId) {
                detectDragGestures(
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
                start = APP_WIDGET_ROW_START_PADDING_DP.dp,
                end = APP_WIDGET_ROW_END_PADDING_DP.dp,
                top = APP_WIDGET_ROW_TOP_PADDING_DP.dp,
                bottom = APP_WIDGET_ROW_BOTTOM_PADDING_DP.dp,
            ),
    ) {
        AndroidView(
            factory = { context -> createWidgetView(appWidgetId) ?: FrameLayout(context) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = APP_WIDGET_MIN_HEIGHT_DP.dp),
        )

        EditDragHandle(
            text = "↕",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        )

        WidgetEditButton(
            text = "Delete",
            onClick = { onRemoveWidget(appWidgetId) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Remove widget") },
                onClick = {
                    showContextMenu = false
                    onRemoveWidget(appWidgetId)
                },
            )
        }
    }
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun WidgetEditButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.58f),
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableFavoriteRow(
    app: LauncherApp,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = rememberAppIcon(app.packageName)

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
                horizontal = APP_ROW_HORIZONTAL_PADDING_DP.dp,
                vertical = APP_ROW_VERTICAL_PADDING_DP.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy((APP_ROW_ICON_SPACING_DP * APP_ROW_CONTENT_SCALE).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size((APP_ROW_FAVORITE_ICON_SIZE_DP * APP_ROW_CONTENT_SCALE).dp),
            )
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = MaterialTheme.typography.headlineSmall.fontSize * APP_ROW_TEXT_SCALE,
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

private fun Float?.orZero(): Float = this ?: 0f
