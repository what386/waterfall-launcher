package org.example.launchertest.ui.home.applist

import android.appwidget.AppWidgetHostView
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (reorderMode) {
                        reorderMode = false
                        persistFavoriteOrder()
                    }
                },
                onLongClick = { showPanelMenu = true },
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
                    if (reorderMode) {
                        ReorderableFavoriteRow(
                            app = app,
                            favorites = orderedFavorites,
                            swapThresholdPx = reorderSwapThresholdPx,
                            onOrderChanged = ::persistFavoriteOrder,
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
    favorites: MutableList<LauncherApp>,
    swapThresholdPx: Float,
    onOrderChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val componentId = favoriteComponentId(app)
    val icon = rememberAppIcon(app.packageName)
    var dragAccumulator by remember(componentId) { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(componentId, swapThresholdPx, favorites.size) {
                detectDragGesturesAfterLongPress(
                    onDragEnd = {
                        dragAccumulator = 0f
                        onOrderChanged()
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                        onOrderChanged()
                    },
                    onDrag = { change, dragAmount ->
                        dragAccumulator += dragAmount.y

                        val direction = when {
                            dragAccumulator <= -swapThresholdPx -> -1
                            dragAccumulator >= swapThresholdPx -> 1
                            else -> 0
                        }

                        if (direction == 0) return@detectDragGesturesAfterLongPress

                        val currentIndex = favorites.indexOfFirst {
                            favoriteComponentId(it) == componentId
                        }
                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                        val targetIndex = (currentIndex + direction).coerceIn(0, favorites.lastIndex)
                        if (targetIndex != currentIndex) {
                            val swapped = favorites[targetIndex]
                            favorites[targetIndex] = favorites[currentIndex]
                            favorites[currentIndex] = swapped
                        }

                        dragAccumulator = 0f
                    },
                )
            }
            .padding(
                horizontal = APP_ROW_HORIZONTAL_PADDING_DP.dp,
                vertical = APP_ROW_VERTICAL_PADDING_DP.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy((APP_ROW_ICON_SPACING_DP * APP_ROW_CONTENT_SCALE).dp),
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
