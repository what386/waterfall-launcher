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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.what386.waterfall.ui.home.LocalHomeLayoutMetrics
import com.what386.waterfall.ui.model.LauncherApp

@Composable
internal fun SectionHeader(
    text: String,
    topPaddingDp: Float? = null,
    bottomPaddingDp: Float? = null,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val resolvedTopPaddingDp = topPaddingDp ?: layoutMetrics.sectionHeaderTopPaddingDp
    val resolvedBottomPaddingDp = bottomPaddingDp ?: layoutMetrics.sectionHeaderBottomPaddingDp

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary.copy(
            alpha = HOME_LIST_SECTION_HEADER_ALPHA,
        ),
        modifier = modifier.padding(
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
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val context = LocalContext.current
    val icon = if (hideAppIcons) null else rememberAppIcon(app.packageName)
    var showMenu by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rowScale by animateFloatAsState(
        targetValue = if (isLaunching) HOME_ROW_PRESS_SCALE else 1f,
        animationSpec = spring(),
        label = "rowScale",
    )
    val rowTintAlpha by animateFloatAsState(
        targetValue = when {
            isLaunching -> HOME_ROW_PRESS_TINT_ALPHA
            isHighlighted -> HOME_ROW_HIGHLIGHT_TINT_ALPHA
            else -> 0f
        },
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
                            delay(HOME_ROW_PRESS_LAUNCH_DELAY_MS)

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
                    horizontal = layoutMetrics.rowHorizontalPaddingDp.dp,
                    vertical = layoutMetrics.rowVerticalPaddingDp.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(
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
                style = if (isFavorite) {
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
                text = { Text("Uninstall") },
                onClick = {
                    showMenu = false

                    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                        data = Uri.fromParts("package", app.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "Unable to uninstall ${app.label}",
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
