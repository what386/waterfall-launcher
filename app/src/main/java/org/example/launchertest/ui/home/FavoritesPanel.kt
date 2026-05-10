package org.example.launchertest.ui.home

import android.appwidget.AppWidgetHostView
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.example.launchertest.ui.model.LauncherApp

/**
 * The favorites panel: widgets + favorite app rows, all in a vertically scrollable column
 * that overscrolls and springs back to center like the A–Z rail does.
 *
 * Overscroll resistance: drag distance is dampened by [FAVORITES_OVERSCROLL_RESISTANCE]
 * so the panel feels springy but not free-floating.
 */
@Composable
fun FavoritesPanel(
    favorites: List<LauncherApp>,
    widgetIds: List<Int>,
    favAlpha: Float,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .graphicsLayer { translationY = overscrollOffset.value }
            .pointerInput(Unit) {
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
                            dragAmount < 0 && !atBottom -> true  // scrolling down, not at bottom
                            dragAmount > 0 && !atTop    -> true  // scrolling up, not at top
                            else                        -> false
                        }

                        if (canNormalScroll) {
                            // Let the inner scroll consume it
                            scope.launch {
                                scrollState.scrollTo(
                                    (scrollState.value - dragAmount.toInt()).coerceIn(0, scrollState.maxValue)
                                )
                            }
                        } else {
                            // Overscroll — apply dampened translation
                            val dampened = dragAmount * FAVORITES_OVERSCROLL_RESISTANCE
                            scope.launch {
                                overscrollOffset.snapTo(overscrollOffset.value + dampened)
                            }
                        }
                    },
                )
            }
            .verticalScroll(scrollState, enabled = false), // scroll driven manually above
    ) {
        // Widgets
        widgetIds.forEach { appWidgetId ->
            WidgetRow(
                appWidgetId = appWidgetId,
                createWidgetView = createWidgetView,
                onRemoveWidget = onRemoveWidget,
                modifier = Modifier.graphicsLayer { alpha = favAlpha },
            )
        }

        TextButton(
            onClick = onAddWidget,
            modifier = Modifier
                .padding(
                    start = ADD_WIDGET_START_PADDING_DP.dp,
                    top = ADD_WIDGET_TOP_PADDING_DP.dp,
                    bottom = ADD_WIDGET_BOTTOM_PADDING_DP.dp,
                )
                .graphicsLayer { alpha = favAlpha },
        ) {
            Text("Add widget")
        }

        // Favorites header + rows
        if (favorites.isNotEmpty()) {
            SectionHeader(
                text = "FAVORITES",
                modifier = Modifier.graphicsLayer { alpha = favAlpha },
            )
            favorites.forEach { app ->
                AppRow(
                    app = app,
                    isFavorite = true,
                    onToggleFavorite = onToggleFavorite,
                    onHideApp = onHideApp,
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
            }
            Spacer(modifier = Modifier.height(APP_LIST_FAVORITES_BOTTOM_SPACER_DP.dp))
        }
    }
}

@Composable
private fun WidgetRow(
    appWidgetId: Int,
    createWidgetView: (Int) -> AppWidgetHostView?,
    onRemoveWidget: (Int) -> Unit,
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
        TextButton(
            onClick = { onRemoveWidget(appWidgetId) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Remove")
        }
    }
}
