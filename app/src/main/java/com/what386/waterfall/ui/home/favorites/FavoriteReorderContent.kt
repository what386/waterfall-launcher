package com.what386.waterfall.ui.home.favorites

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.what386.waterfall.R
import com.what386.waterfall.ui.home.LocalHomeLayoutMetrics
import com.what386.waterfall.ui.home.shared.HOME_ROW_TEXT_SCALE
import com.what386.waterfall.ui.home.shared.rememberAppIcon
import com.what386.waterfall.ui.model.LauncherApp
import com.what386.waterfall.widgets.WidgetStack
import kotlin.math.hypot

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WidgetStackRow(
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
        modifier =
            modifier
                .fillMaxWidth(),
    )
}

@Composable
internal fun AddWidgetEditRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    start = layoutMetrics.widgetRowStartPaddingDp.dp,
                    end = layoutMetrics.widgetRowEndPaddingDp.dp,
                    top = layoutMetrics.widgetRowTopPaddingDp.dp,
                    bottom = layoutMetrics.widgetRowBottomPaddingDp.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.size(layoutMetrics.sheetIconSizeDp.dp),
        )
        Column(
            modifier = Modifier.padding(start = layoutMetrics.sheetContentStartPaddingDp.dp),
        ) {
            Text(
                text = stringResource(R.string.add_stack),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.add_stack_summary),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReorderableWidgetStackRow(
    widgetStack: WidgetStack,
    stackIndex: Int,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    onAddWidgetToStack: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var dragDistancePx by remember { mutableFloatStateOf(0f) }
    val pagerState = rememberPagerState(pageCount = { widgetStack.widgetIds.size + 1 })
    val currentWidgetId = widgetStack.widgetIds.getOrNull(pagerState.currentPage)
    val density = LocalDensity.current
    val contextMenuDragThresholdPx =
        with(density) {
            layoutMetrics.widgetContextMenuDragThresholdDp.dp.toPx()
        }
    val settleSpec =
        spring<Float>(
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
        modifier =
            modifier
                .fillMaxWidth()
                .zIndex(if (isActiveDrag) 1f else 0f)
                .graphicsLayer {
                    translationY = animatedTranslationY
                    scaleX = activeScale
                    scaleY = activeScale
                    shadowElevation =
                        if (isActiveDrag) {
                            layoutMetrics.reorderActiveShadowYDp.dp.toPx()
                        } else {
                            0f
                        }
                }.onGloballyPositioned { coordinates ->
                    onMeasured(
                        FavoriteRowMetrics(
                            topY = coordinates.positionInParent().y,
                            height = coordinates.size.height.toFloat(),
                        ),
                    )
                }.pointerInput(widgetStack, currentWidgetId, contextMenuDragThresholdPx) {
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
                }.background(
                    color = Color.White.copy(alpha = activeTint),
                    shape = MaterialTheme.shapes.medium,
                ).padding(
                    start = layoutMetrics.widgetRowStartPaddingDp.dp,
                    end = layoutMetrics.widgetRowEndPaddingDp.dp,
                    top = layoutMetrics.widgetRowTopPaddingDp.dp,
                    bottom = layoutMetrics.widgetRowBottomPaddingDp.dp,
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

        ReorderButtons(
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = layoutMetrics.widgetDragHandleTopPaddingDp.dp,
                        end = layoutMetrics.widgetDragHandleEndPaddingDp.dp,
                    ),
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove)) },
                onClick = {
                    showContextMenu = false
                    showRemoveConfirmation = currentWidgetId != null
                },
            )
        }

        if (showRemoveConfirmation) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirmation = false },
                title = { Text(stringResource(R.string.remove_widget_title)) },
                text = { Text(stringResource(R.string.remove_widget_summary)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRemoveConfirmation = false
                            currentWidgetId?.let(onRemoveWidget)
                        },
                    ) {
                        Text(stringResource(R.string.remove))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WidgetStackContent(
    widgetStack: WidgetStack,
    createWidgetView: (Int) -> AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
    showAddPlaceholder: Boolean,
    modifier: Modifier = Modifier,
    onAddWidgetToStack: (() -> Unit)? = null,
    onWidgetHoldRelease: (() -> Unit)?,
    pagerState: androidx.compose.foundation.pager.PagerState =
        rememberPagerState(
            pageCount = { widgetStack.widgetIds.size },
        ),
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val stackHeightDp =
        remember(widgetStack, getWidgetMinHeightDp, layoutMetrics.widgetMinHeightDp) {
            widgetStack.widgetIds
                .mapNotNull(getWidgetMinHeightDp)
                .maxOrNull()
                ?.coerceAtLeast(layoutMetrics.widgetMinHeightDp.toInt())
                ?: layoutMetrics.widgetMinHeightDp.toInt()
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier.padding(
                start = layoutMetrics.widgetRowStartPaddingDp.dp,
                end = layoutMetrics.widgetRowEndPaddingDp.dp,
                top = layoutMetrics.widgetRowTopPaddingDp.dp,
                bottom = layoutMetrics.widgetRowBottomPaddingDp.dp,
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(stackHeightDp.dp),
                )
            } else if (showAddPlaceholder && onAddWidgetToStack != null) {
                AddWidgetToStackPlaceholder(
                    onClick = onAddWidgetToStack,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(stackHeightDp.dp),
                )
            }
        }

        val pageCount = widgetStack.widgetIds.size + if (showAddPlaceholder) 1 else 0
        if (pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(layoutMetrics.widgetPageIndicatorSpacingDp.dp),
                modifier = Modifier.padding(top = layoutMetrics.widgetPageIndicatorTopPaddingDp.dp),
            ) {
                repeat(pageCount) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    if (pagerState.currentPage == index) {
                                        layoutMetrics.widgetPageIndicatorSelectedSizeDp.dp
                                    } else {
                                        layoutMetrics.widgetPageIndicatorSizeDp.dp
                                    },
                                ).background(
                                    color =
                                        Color.White.copy(
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
internal fun AddWidgetToStackPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium,
                ).clickable(onClick = onClick),
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
                text = stringResource(R.string.add_widget),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

internal fun installWidgetHoldReleaseMenu(
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

internal fun clearWidgetHoldReleaseMenu(view: View) {
    view.setOnLongClickListener(null)
    view.setOnTouchListener(null)

    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            clearWidgetHoldReleaseMenu(view.getChildAt(index))
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
internal class WidgetHoldReleaseTouchListener(
    private val onHoldRelease: () -> Unit,
) : View.OnTouchListener {
    private var downX = 0f
    private var downY = 0f
    private var longPressReached = false
    private var cancelled = false
    private var longPressRunnable: Runnable? = null

    override fun onTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressReached = false
                cancelled = false
                longPressRunnable =
                    Runnable {
                        longPressReached = true
                    }.also { runnable ->
                        view.postDelayed(runnable, ViewConfiguration.getLongPressTimeout().toLong())
                    }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val distance =
                    hypot(
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

internal fun android.content.Context.scaledTouchSlopForWidgets(): Float = ViewConfiguration.get(this).scaledTouchSlop.toFloat()

@Composable
internal fun EditDragHandle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Text(
        text = text,
        color = Color.White.copy(alpha = 0.84f),
        style = MaterialTheme.typography.headlineMedium,
        modifier =
            modifier
                .padding(horizontal = layoutMetrics.editDragHandleHorizontalPaddingDp.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ReorderableFavoriteRow(
    app: LauncherApp,
    hideAppIcons: Boolean,
    isActiveDrag: Boolean,
    dragOffsetY: Float,
    laneShiftY: Float,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onMeasured: (FavoriteRowMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val icon = if (hideAppIcons) null else rememberAppIcon(app)

    val settleSpec =
        spring<Float>(
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
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = animatedTranslationY
                    scaleX = activeScale
                    scaleY = activeScale
                    alpha = 1f
                    shadowElevation =
                        if (isActiveDrag) {
                            layoutMetrics.reorderActiveShadowYDp.dp.toPx()
                        } else {
                            0f
                        }
                }.onGloballyPositioned { coordinates ->
                    onMeasured(
                        FavoriteRowMetrics(
                            topY = coordinates.positionInParent().y,
                            height = coordinates.size.height.toFloat(),
                        ),
                    )
                }.pointerInput(app.packageName, app.activityName) {
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
                }.background(
                    color = Color.White.copy(alpha = activeTint),
                    shape = MaterialTheme.shapes.medium,
                ).padding(
                    horizontal = layoutMetrics.rowHorizontalPaddingDp.dp,
                    vertical = layoutMetrics.rowVerticalPaddingDp.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(layoutMetrics.rowIconSpacingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(layoutMetrics.favoriteRowIconSizeDp.dp),
            )
        }

        Text(
            text = app.label,
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize * HOME_ROW_TEXT_SCALE,
                ),
            modifier = Modifier.weight(1f),
        )

        ReorderButtons(
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )
    }
}

@Composable
private fun ReorderButtons(
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moveUpLabel = stringResource(R.string.move_up)
    val moveDownLabel = stringResource(R.string.move_down)
    Column(modifier = modifier) {
        EditDragHandle(
            text = "↑",
            modifier =
                Modifier
                    .semantics { contentDescription = moveUpLabel }
                    .clickable(onClick = onMoveUp),
        )
        EditDragHandle(
            text = "↓",
            modifier =
                Modifier
                    .semantics { contentDescription = moveDownLabel }
                    .clickable(onClick = onMoveDown),
        )
    }
}

internal data class FavoriteRowMetrics(
    val topY: Float,
    val height: Float,
) {
    val centerY: Float
        get() = topY + (height / 2f)
}

internal fun favoriteComponentId(app: LauncherApp): String = app.componentId

internal fun widgetStackId(stack: WidgetStack): String = stack.id

internal fun Float?.orZero(): Float = this ?: 0f
