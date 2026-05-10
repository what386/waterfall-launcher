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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    showFavoritesOnly: Boolean,
    isSearchActive: Boolean,
    listState: LazyListState,
    categoryPinOffsetPx: Int,
    onSearchActivated: () -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
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
                },
            state = listState,
            contentPadding = PaddingValues(
                start = APP_LIST_CONTENT_START_PADDING_DP.dp,
                top = APP_LIST_CONTENT_TOP_PADDING_DP.dp,
                end = APP_LIST_CONTENT_END_PADDING_DP.dp,
                bottom = if (isSearchActive) {
                    APP_LIST_SEARCH_BOTTOM_PADDING_DP.dp
                } else {
                    APP_LIST_CONTENT_BOTTOM_PADDING_DP.dp
                },
            ),
        ) {
            if (!showFavoritesOnly) {
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
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
                        onAddWidget = onAddWidget,
                        onRemoveWidget = onRemoveWidget,
                        createWidgetView = createWidgetView,
                        modifier = Modifier.fillParentMaxHeight(),
                    )
                }
            }

            if (!showFavoritesOnly) {
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
                        onToggleFavorite = onToggleFavorite,
                        onHideApp = onHideApp,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppRow(
    app: LauncherApp,
    isFavorite: Boolean,
    onToggleFavorite: (LauncherApp) -> Unit,
    onHideApp: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(app.packageName)
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
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
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleLarge
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
                text = { Text("Hide") },
                onClick = {
                    showMenu = false
                    onHideApp(app)
                },
            )
        }
    }
}
