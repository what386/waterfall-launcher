package org.example.launchertest.ui.home.applist

import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.launchertest.ui.home.shared.rememberAppIcon
import org.example.launchertest.ui.model.LauncherApp

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
fun AppListPanel(
    listLayout: AppListLayout,
    widgetIds: List<Int>,
    scrubbingLetter: State<Char?>,
    isScrubbing: State<Boolean>,
    isHiddenMode: Boolean,
    showFavoritesOnly: Boolean,
    isSearchActive: Boolean,
    listState: LazyListState,
    categoryPinOffsetPx: Int,
    onSearchActivated: () -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onUnhideApp: (LauncherApp) -> Unit,
    onReorderFavorites: (List<LauncherApp>) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgets: (List<Int>) -> Unit,
    createWidgetView: (Int) -> AppWidgetHostView?,
    modifier: Modifier = Modifier,
) {
    val apps = listLayout.apps
    val favorites = listLayout.favorites

    val density = LocalDensity.current
    val dragThresholdPx = with(density) { APP_LIST_SEARCH_DRAG_THRESHOLD_DP.dp.toPx() }
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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // This is the "eraser" that creates transparency at the top
                .fadingEdge(topFadeBrush)
                .pointerInput(isSearchActive) {
                    if (!isSearchActive) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (!listState.canScrollForward && dragAmount < -dragThresholdPx) {
                                onSearchActivated()
                            }
                        }
                    }
                }
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
                    APP_LIST_CONTENT_START_PADDING_DP.dp
                },
                top = if (isSearchActive) {
                    APP_LIST_SEARCH_TOP_PADDING_DP.dp
                } else {
                    APP_LIST_CONTENT_TOP_PADDING_DP.dp
                },
                end = if (showFavoritesOnly) {
                    0.dp
                } else {
                    APP_LIST_CONTENT_END_PADDING_DP.dp
                },
                bottom = if (isSearchActive) {
                    APP_LIST_SEARCH_BOTTOM_PADDING_DP.dp
                } else {
                    APP_LIST_CONTENT_BOTTOM_PADDING_DP.dp
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
                        widgetIds = widgetIds,
                        favAlpha = favAlpha,
                        isSearchActive = isSearchActive,
                        isHiddenMode = isHiddenMode,
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onHiddenModeChanged = onHiddenModeChanged,
                        onReorderFavorites = onReorderFavorites,
                        onSearchActivated = onSearchActivated,
                        onAddWidget = onAddWidget,
                        onRemoveWidget = onRemoveWidget,
                        onReorderWidgets = onReorderWidgets,
                        createWidgetView = createWidgetView,
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
                        modifier = rowModifier,
                    )
                }
            }
        }
    }
}
@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(
            alpha = APP_LIST_SECTION_HEADER_ALPHA,
        ),
        modifier = modifier.padding(
            start = APP_LIST_SECTION_HEADER_START_PADDING_DP.dp,
            top = APP_LIST_SECTION_HEADER_TOP_PADDING_DP.dp,
            bottom = APP_LIST_SECTION_HEADER_BOTTOM_PADDING_DP.dp,
        ),
    )
}

internal fun bucketFor(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}

@Composable
private fun HiddenModeIndicator() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(
            start = APP_ROW_HORIZONTAL_PADDING_DP.dp,
            end = APP_ROW_HORIZONTAL_PADDING_DP.dp,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppRow(
    app: LauncherApp,
    isFavorite: Boolean,
    isHiddenMode: Boolean,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onUnhideApp: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(app.packageName)
    var showMenu by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rowScale by animateFloatAsState(
        targetValue = if (isLaunching) APP_ROW_PRESS_SCALE else 1f,
        animationSpec = spring(),
        label = "rowScale",
    )
    val rowTintAlpha by animateFloatAsState(
        targetValue = if (isLaunching) APP_ROW_PRESS_TINT_ALPHA else 0f,
        animationSpec = spring(),
        label = "rowTintAlpha",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .background(
                    color = Color.White.copy(alpha = rowTintAlpha),
                    shape = MaterialTheme.shapes.medium,
                )
                .combinedClickable(
                    onClick = {
                        if (isLaunching) return@combinedClickable

                        isLaunching = true
                        scope.launch {
                            delay(APP_ROW_PRESS_LAUNCH_DELAY_MS)

                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                component = ComponentName(app.packageName, app.activityName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }

                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to launch ${app.label}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } finally {
                                isLaunching = false
                            }
                        }
                    },
                    onLongClick = { showMenu = true },
                )
                .padding(
                    horizontal = APP_ROW_HORIZONTAL_PADDING_DP.dp,
                    vertical = APP_ROW_VERTICAL_PADDING_DP.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(
                        if (isFavorite) {
                            (APP_ROW_FAVORITE_ICON_SIZE_DP * APP_ROW_CONTENT_SCALE).dp
                        } else {
                            (APP_ROW_ICON_SIZE_DP * APP_ROW_CONTENT_SCALE).dp
                        },
                    ),
                )

                Spacer(modifier = Modifier.width((APP_ROW_ICON_SPACING_DP * APP_ROW_CONTENT_SCALE).dp))
            }

            Text(
                text = app.label,
                style = if (isFavorite) {
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize * APP_ROW_TEXT_SCALE,
                    )
                } else {
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = MaterialTheme.typography.titleLarge.fontSize * APP_ROW_TEXT_SCALE,
                    )
                },
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(if (app.isFavorite) "Unfavorite" else "Favorite") },
                onClick = {
                    showMenu = false
                    onToggleFavorite(app)
                },
            )

            DropdownMenuItem(
                text = { Text("App Info") },
                onClick = {
                    showMenu = false

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "Unable to open settings for ${app.label}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )

            DropdownMenuItem(
                text = { Text(if (isHiddenMode) "Unhide" else "Hide") },
                onClick = {
                    showMenu = false
                    if (isHiddenMode) {
                        onUnhideApp(app)
                    } else {
                        onHideApp(app)
                    }
                },
            )
        }
    }
}
