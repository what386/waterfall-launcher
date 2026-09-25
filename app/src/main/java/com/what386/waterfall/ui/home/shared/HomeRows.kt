package com.what386.waterfall.ui.home.shared

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.what386.waterfall.R
import com.what386.waterfall.ui.home.LocalHomeLayoutMetrics
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    topPaddingDp: Float? = null,
    bottomPaddingDp: Float? = null,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val resolvedTopPaddingDp = topPaddingDp ?: layoutMetrics.sectionHeaderTopPaddingDp
    val resolvedBottomPaddingDp = bottomPaddingDp ?: layoutMetrics.sectionHeaderBottomPaddingDp

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color =
            MaterialTheme.colorScheme.primary.copy(
                alpha = HOME_LIST_SECTION_HEADER_ALPHA,
            ),
        modifier =
            modifier.padding(
                start = layoutMetrics.sectionHeaderStartPaddingDp.dp,
                top = resolvedTopPaddingDp.dp,
                bottom = resolvedBottomPaddingDp.dp,
            ),
    )
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
    hideAppIcons: Boolean,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onDragStart: (() -> Unit)? = null,
    onDragDelta: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val context = LocalContext.current
    val icon = if (hideAppIcons) null else rememberAppIcon(app)
    var showMenu by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }
    var isHeld by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val appActionsLabel = stringResource(R.string.app_actions)
    val unableToLaunchMessage = stringResource(R.string.unable_to_launch, app.label)
    val unableToOpenSettingsMessage = stringResource(R.string.unable_to_open_settings, app.label)
    val unableToUninstallMessage = stringResource(R.string.unable_to_uninstall, app.label)

    val rowScale by animateFloatAsState(
        targetValue =
            when {
                isLaunching -> HOME_ROW_PRESS_SCALE
                isHeld -> HOME_ROW_HOLD_SCALE
                else -> 1f
            },
        animationSpec = spring(),
        label = "rowScale",
    )
    val rowTintAlpha by animateFloatAsState(
        targetValue =
            when {
                isLaunching -> HOME_ROW_PRESS_TINT_ALPHA
                isHeld -> HOME_ROW_HOLD_TINT_ALPHA
                isHighlighted -> HOME_ROW_HIGHLIGHT_TINT_ALPHA
                else -> 0f
            },
        animationSpec = spring(),
        label = "rowTintAlpha",
    )

    fun launchApp() {
        if (isLaunching) return

        isLaunching = true
        scope.launch {
            delay(HOME_ROW_PRESS_LAUNCH_DELAY_MS)

            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(app.packageName, app.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast
                    .makeText(context, unableToLaunchMessage, Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isLaunching = false
            }
        }
    }

    val rowInteraction =
        if (onDragStart != null && onDragDelta != null && onDragEnd != null) {
            Modifier
                .pointerInput(app.componentId) {
                    detectFavoriteHoldDrag(
                        onTap = ::launchApp,
                        onHoldStart = { isHeld = true },
                        onHoldEnd = { isHeld = false },
                        onHoldRelease = { showMenu = true },
                        onDragStart = onDragStart,
                        onDragDelta = onDragDelta,
                        onDragEnd = onDragEnd,
                    )
                }.semantics {
                    onClick {
                        launchApp()
                        true
                    }
                    onLongClick(label = appActionsLabel) {
                        showMenu = true
                        true
                    }
                }
        } else {
            Modifier.combinedClickable(
                onLongClickLabel = appActionsLabel,
                onClick = ::launchApp,
                onLongClick = { showMenu = true },
            )
        }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = rowScale
                        scaleY = rowScale
                    }.background(
                        color = Color.White.copy(alpha = rowTintAlpha),
                        shape = MaterialTheme.shapes.medium,
                    ).then(rowInteraction)
                    .padding(
                        horizontal = layoutMetrics.rowHorizontalPaddingDp.dp,
                        vertical = layoutMetrics.rowVerticalPaddingDp.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier =
                        Modifier.size(
                            if (isFavorite) {
                                layoutMetrics.favoriteRowIconSizeDp.dp
                            } else {
                                layoutMetrics.appRowIconSizeDp.dp
                            },
                        ),
                )

                Spacer(modifier = Modifier.width(layoutMetrics.rowIconSpacingDp.dp))
            }

            Text(
                text = app.label,
                style =
                    if (isFavorite) {
                        MaterialTheme.typography.headlineSmall.copy(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * HOME_ROW_TEXT_SCALE,
                        )
                    } else {
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * HOME_ROW_TEXT_SCALE,
                        )
                    },
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (app.isFavorite) R.string.unfavorite else R.string.favorite,
                        ),
                    )
                },
                onClick = {
                    showMenu = false
                    onToggleFavorite(app)
                },
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.app_info)) },
                onClick = {
                    showMenu = false

                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast
                            .makeText(
                                context,
                                unableToOpenSettingsMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                },
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.uninstall)) },
                onClick = {
                    showMenu = false

                    val intent =
                        Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast
                            .makeText(
                                context,
                                unableToUninstallMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                },
            )

            DropdownMenuItem(
                text = {
                    Text(stringResource(if (isHiddenMode) R.string.unhide else R.string.hide))
                },
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

private suspend fun PointerInputScope.detectFavoriteHoldDrag(
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onHoldRelease: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        var travel = Offset.Zero
        val held =
            withTimeoutOrNull(HOME_ROW_HOLD_TIMEOUT_MS) {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.isConsumed) return@withTimeoutOrNull false
                    if (change.changedToUp()) {
                        onTap()
                        return@withTimeoutOrNull false
                    }
                    travel += change.positionChange()
                    if (travel.getDistance() > viewConfiguration.touchSlop) {
                        return@withTimeoutOrNull false
                    }
                }
            } == null

        if (held) {
            var dragging = false
            var dragTravel = Offset.Zero
            var showingHoldFeedback = true
            onHoldStart()
            try {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.isConsumed) {
                        break
                    }
                    if (change.changedToUp()) {
                        if (!dragging) onHoldRelease()
                        change.consume()
                        break
                    }

                    val delta = change.positionChange()
                    if (delta != Offset.Zero) {
                        change.consume()
                        if (!dragging) {
                            dragTravel += delta
                            if (dragTravel.getDistance() > viewConfiguration.touchSlop) {
                                dragging = true
                                showingHoldFeedback = false
                                onHoldEnd()
                                onDragStart()
                                onDragDelta(dragTravel.y)
                            }
                        } else {
                            onDragDelta(delta.y)
                        }
                    }
                }
            } finally {
                if (dragging) onDragEnd()
                if (showingHoldFeedback) onHoldEnd()
            }
        }
    }
}
