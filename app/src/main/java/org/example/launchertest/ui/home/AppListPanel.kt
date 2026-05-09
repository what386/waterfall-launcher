package org.example.launchertest.ui.home

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import android.appwidget.AppWidgetHostView
import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.example.launchertest.ui.model.LauncherApp

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
        item(key = "category_pin_spacer") {
            Spacer(modifier = Modifier.height(categoryPinOffsetDp))
        }

        if (showFavoritesOnly) {
            items(
                items = widgetIds,
                key = { appWidgetId -> "widget_$appWidgetId" },
            ) { appWidgetId ->
                WidgetRow(
                    appWidgetId = appWidgetId,
                    createWidgetView = createWidgetView,
                    onRemoveWidget = onRemoveWidget,
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
            }

            item(key = "add_widget") {
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
            }
        }

        if (favorites.isNotEmpty()) {
            item(key = "favorites_header") {
                SectionHeader(
                    text = "FAVORITES",
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
            }

            items(
                items = favorites,
                key = { app -> app.packageName + app.activityName + "_fav" },
            ) { app ->
                AppRow(
                    app = app,
                    isFavorite = true,
                    onToggleFavorite = onToggleFavorite,
                    onHideApp = onHideApp,
                    modifier = Modifier.graphicsLayer { alpha = favAlpha },
                )
            }

            item(key = "favorites_bottom_spacer") {
                Spacer(
                    modifier = Modifier
                        .height(APP_LIST_FAVORITES_BOTTOM_SPACER_DP.dp)
                        .graphicsLayer { alpha = favAlpha },
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(APP_LIST_TOP_FADE_HEIGHT_DP.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 1f),
                        Color.Black.copy(alpha = 0f),
                    )
                ),
            ),
    )
}}

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
            factory = { context ->
                createWidgetView(appWidgetId) ?: FrameLayout(context)
            },
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

@Composable
private fun SectionHeader(
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
                            APP_ROW_FAVORITE_ICON_SIZE_DP.dp
                        } else {
                            APP_ROW_ICON_SIZE_DP.dp
                        },
                    ),
                )

                Spacer(modifier = Modifier.width(APP_ROW_ICON_SPACING_DP.dp))
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
