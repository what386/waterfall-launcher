package org.example.launchertest.ui.home.applist

import android.appwidget.AppWidgetHostView
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesPanel(
    favorites: List<LauncherApp>,
    widgetIds: List<Int>,
    favAlpha: Float,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onReorderFavorites: (List<LauncherApp>) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val reorderSwapThresholdPx = with(density) { FAVORITES_REORDER_SWAP_THRESHOLD_DP.dp.toPx() }
    var showPanelMenu by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var activeDragComponentId by remember { mutableStateOf<String?>(null) }
    var activeDragStartIndex by remember { mutableIntStateOf(-1) }
    var activeDragTargetIndex by remember { mutableIntStateOf(-1) }
    var activeDragOffsetY by remember { mutableFloatStateOf(0f) }
    val rowHeightsPx = remember { mutableStateMapOf<String, Float>() }
    val orderedFavorites = remember { mutableStateListOf<LauncherApp>() }

    LaunchedEffect(favorites) {
        val incomingById = favorites.associateBy(::favoriteComponentId)
        val currentIds = orderedFavorites.map(::favoriteComponentId)
        val kept = currentIds.mapNotNull(incomingById::get)
        val appended = favorites.filter { favoriteComponentId(it) !in currentIds.toSet() }
        orderedFavorites.clear()
        orderedFavorites.addAll(kept + appended)
    }

    fun persistFavoriteOrder() {
        onReorderFavorites(orderedFavorites.toList())
    }

    fun clearDragState() {
        activeDragComponentId = null
        activeDragStartIndex = -1
        activeDragTargetIndex = -1
        activeDragOffsetY = 0f
    }

    fun commitDragReorderIfNeeded() {
        if (activeDragStartIndex == -1 || activeDragTargetIndex == -1) {
            clearDragState()
            return
        }

        if (activeDragStartIndex != activeDragTargetIndex &&
            activeDragStartIndex in orderedFavorites.indices &&
            activeDragTargetIndex in orderedFavorites.indices
        ) {
            val moved = orderedFavorites.removeAt(activeDragStartIndex)
            orderedFavorites.add(activeDragTargetIndex, moved)
            persistFavoriteOrder()
        }

        clearDragState()
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
                } else {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showPanelMenu = true },
                    )
                }
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .offset(y = (-FAVORITES_CENTER_BIAS_UP_DP).dp)
                .graphicsLayer { translationY = overscrollOffset.value }
                .pointerInput(reorderMode) {
                    if (reorderMode) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
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
                                    overscrollOffset.snapTo(overscrollOffset.value + dampened)
                                }
                            }
                        },
                    )
                }
                .verticalScroll(scrollState, enabled = false),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(FAVORITES_PANEL_TOP_MARGIN_DP.dp))

            widgetIds.forEach { appWidgetId ->
                WidgetRow(
                    appWidgetId = appWidgetId,
                    createWidgetView = createWidgetView,
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
            }

            if (widgetIds.isNotEmpty()) {
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
                            val activeRowHeight = rowHeightsPx[activeDragComponentId].orZero()
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
                                    activeDragComponentId = componentId
                                    activeDragStartIndex = index
                                    activeDragTargetIndex = activeDragStartIndex
                                    activeDragOffsetY = 0f
                                },
                                onDragDelta = { deltaY ->
                                    if (activeDragComponentId != componentId) return@ReorderableFavoriteRow
                                    activeDragOffsetY += deltaY
                                    val startIndex = activeDragStartIndex
                                    if (startIndex !in orderedFavorites.indices) return@ReorderableFavoriteRow

                                    val stepOffset = (activeDragOffsetY / reorderSwapThresholdPx).toInt()
                                    activeDragTargetIndex = (startIndex + stepOffset)
                                        .coerceIn(0, orderedFavorites.lastIndex)
                                },
                                onDragEnd = {
                                    if (activeDragComponentId == componentId) {
                                        commitDragReorderIfNeeded()
                                    }
                                },
                                onMeasured = { heightPx ->
                                    rowHeightsPx[componentId] = heightPx
                                },
                                modifier = Modifier.graphicsLayer { alpha = favAlpha },
                            )
                        } else {
                            AppRow(
                                app = app,
                                isFavorite = true,
                                onToggleFavorite = onToggleFavorite,
                                onHideApp = onHideApp,
                                modifier = Modifier.graphicsLayer { alpha = favAlpha },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(APP_LIST_FAVORITES_BOTTOM_SPACER_DP.dp))
            } else if (widgetIds.isEmpty()) {
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

        DropdownMenu(
            expanded = showPanelMenu,
            onDismissRequest = { showPanelMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Add widget") },
                onClick = {
                    showPanelMenu = false
                    if (reorderMode) {
                        reorderMode = false
                        persistFavoriteOrder()
                    }
                    onAddWidget()
                },
            )

            if (widgetIds.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Clear all widgets") },
                    onClick = {
                        showPanelMenu = false
                        if (reorderMode) {
                            reorderMode = false
                            persistFavoriteOrder()
                        }
                        widgetIds.toList().forEach(onRemoveWidget)
                    },
                )
            }

            if (orderedFavorites.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(if (reorderMode) "Done reordering" else "Reorder favorites") },
                    onClick = {
                        showPanelMenu = false
                        val nextMode = !reorderMode
                        reorderMode = nextMode
                        if (!nextMode) {
                            commitDragReorderIfNeeded()
                            persistFavoriteOrder()
                        }
                    },
                )
            }
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
    onMeasured: (Float) -> Unit,
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
                onMeasured(coordinates.size.height.toFloat())
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
        )
    }
}

private fun favoriteComponentId(app: LauncherApp): String = "${app.packageName}/${app.activityName}"

private fun Float?.orZero(): Float = this ?: 0f
