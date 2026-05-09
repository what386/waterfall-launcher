package org.example.launchertest.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.example.launchertest.ui.model.LauncherApp

@Composable
fun AppListPanel(
    apps: List<LauncherApp>,
    favorites: List<LauncherApp>,
    scrubbingLetter: Char?,
    isSearchActive: Boolean,
    listState: LazyListState,
    onSearchActivated: () -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() }

    LazyColumn(
        modifier = modifier
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
            top = 16.dp,
            bottom = if (isSearchActive) 80.dp else 24.dp,
            end = 40.dp,
        ),
    ) {
        // ── Favorites ──────────────────────────────────────────────────────
        if (favorites.isNotEmpty()) {
            items(
                items = favorites,
                key = { app -> app.packageName + app.activityName + "_fav" },
            ) { app ->
                val alpha by animateFloatAsState(
                    targetValue = if (scrubbingLetter != null) 0f else 1f,
                    animationSpec = spring(stiffness = 300f, dampingRatio = 1f),
                    label = "favAlpha",
                )
                Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
                    AppRow(app = app, isFavorite = true, onToggleFavorite = onToggleFavorite)
                }
            }
            item {
                val alpha by animateFloatAsState(
                    targetValue = if (scrubbingLetter != null) 0f else 1f,
                    animationSpec = spring(stiffness = 300f, dampingRatio = 1f),
                    label = "favSpacerAlpha",
                )
                Spacer(modifier = Modifier
                    .height(24.dp)
                    .graphicsLayer { this.alpha = alpha })
            }
        }

        // ── A–Z list ───────────────────────────────────────────────────────
        itemsIndexed(
            items = apps,
            key = { _, app -> app.packageName + app.activityName },
        ) { index, app ->
            val bucket = bucketFor(app.label)
            val isSelected = scrubbingLetter == null || scrubbingLetter == bucket
            val itemAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = spring(stiffness = 300f, dampingRatio = 1f),
                label = "itemAlpha_$index",
            )

            val prevBucket = if (index > 0) bucketFor(apps[index - 1].label) else null
            if (prevBucket == null || bucket != prevBucket) {
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bucket.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 20.dp, top = 8.dp, bottom = 2.dp)
                        .graphicsLayer { alpha = itemAlpha },
                )
            }

            Box(modifier = Modifier.graphicsLayer { alpha = itemAlpha }) {
                AppRow(app = app, isFavorite = false, onToggleFavorite = onToggleFavorite)
            }
        }
    }
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
) {
    val context = LocalContext.current
    val icon = rememberAppIcon(app.packageName)

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
                        Toast.makeText(context, "Unable to launch ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { onToggleFavorite(app) },
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isFavorite) 40.dp else 32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = app.label,
            style = if (isFavorite) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.titleMedium,
        )
    }
}
