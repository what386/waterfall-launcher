package org.example.launchertest.ui.home

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.model.LauncherApp

@Composable
fun LauncherHomeRoute(interactor: LauncherInteractor) {
    val vm: LauncherHomeViewModel = viewModel(
        factory = LauncherHomeViewModelFactory(interactor),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        vm.jumpToIndex.collectLatest { index ->
            listState.scrollToItem(index) // instant jump, not animated
        }
    }
    LauncherHomeScreen(
        state = state,
        listState = listState,
        onQueryChanged = vm::onQueryChanged,
        onSearchActivated = vm::onSearchActivated,
        onSearchDismissed = vm::onSearchDismissed,
        onToggleFavorite = vm::onToggleFavorite,
        onLetterSelected = vm::onLetterSelected,
    )
}

@Composable
private fun LauncherHomeScreen(
    state: LauncherHomeUiState,
    listState: LazyListState,
    onQueryChanged: (String) -> Unit,
    onSearchActivated: () -> Unit,
    onSearchDismissed: () -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onLetterSelected: (Char) -> Unit,
) {
    val dragThresholdPx = with(LocalDensity.current) { 32.dp.toPx() }

    // Which letter bucket is currently being scrubbed; null = not scrubbing
    var scrubbingLetter by remember { mutableStateOf<Char?>(null) }

    if (state.isSearchActive) BackHandler(onBack = onSearchDismissed)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── App list ──────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.isSearchActive) {
                        if (!state.isSearchActive) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val isAtBottom = !listState.canScrollForward
                                if (isAtBottom && dragAmount < -dragThresholdPx) onSearchActivated()
                            }
                        }
                    },
                state = listState,
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = if (state.isSearchActive) 80.dp else 24.dp,
                    end = 40.dp,
                ),
            ) {
                // Favorites — hidden while scrubbing
                if (state.favorites.isNotEmpty()) {
                    items(
                        items = state.favorites,
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

                // A–Z list
                itemsIndexed(
                    items = state.apps,
                    key = { _, app -> app.packageName + app.activityName },
                ) { index, app ->
                    val bucket = bucketFor(app.label)
                    val isSelected = scrubbingLetter == null || scrubbingLetter == bucket

                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = spring(stiffness = 300f, dampingRatio = 1f),
                        label = "itemAlpha_$index",
                    )

                    // Bucket header
                    val prevBucket = if (index > 0) bucketFor(state.apps[index - 1].label) else null
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

            // ── Search overlay ────────────────────────────────────────────────
            if (state.isSearchActive) {
                SearchOverlay(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }

            // ── A–Z Rail (always on top, never scrolls) ────────────────────
            AzRail(
                letters = state.letterIndexMap.keys.toList(),
                onLetterSelected = onLetterSelected,
                onScrubStart = { letter -> scrubbingLetter = letter },
                onScrubMove  = { letter -> scrubbingLetter = letter },
                onScrubEnd   = { scrubbingLetter = null },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.5f)
                    .padding(end = 4.dp),
            )
        }
    }
}

private fun bucketFor(label: String): Char {
    val first = label.trim().firstOrNull()?.uppercaseChar() ?: return '#'
    return if (first in 'A'..'Z') first else '#'
}

// ── Search overlay ─────────────────────────────────────────────────────────────

@Composable
private fun SearchOverlay(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
        )
    }
}

// ── A–Z Rail ──────────────────────────────────────────────────────────────────

@Composable
private fun AzRail(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    onScrubStart: (Char) -> Unit,
    onScrubMove:  (Char) -> Unit,
    onScrubEnd:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val itemTops = remember { mutableMapOf<Int, Float>() }
    var spotlightTargetY by remember { mutableFloatStateOf(0f) }

    val spotlightY by animateFloatAsState(
        targetValue = spotlightTargetY,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.75f),
        label = "spotlightY",
    )
    val spotlightAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 1f),
        label = "spotlightAlpha",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val spotlightSizeDp = 56.dp
    val spotlightSizePx  = with(LocalDensity.current) { spotlightSizeDp.toPx() }
    val spotlightOffsetPx = with(LocalDensity.current) { (-90).dp.toPx() }

    fun pickIndex(y: Float): Int {
        if (itemTops.isEmpty()) return 0
        var best = 0
        for ((idx, top) in itemTops) {
            if (top <= y && idx >= best) best = idx
        }
        return best.coerceIn(0, letters.lastIndex)
    }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .pointerInput(letters) {
                awaitEachGesture {
                    // Wait for initial finger down
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull() ?: return@awaitEachGesture
                    if (!down.pressed) return@awaitEachGesture
                    down.consume()

                    var currentIdx = pickIndex(down.position.y)
                    selectedIndex = currentIdx
                    spotlightTargetY = itemTops[currentIdx] ?: down.position.y
                    onScrubStart(letters[currentIdx])
                    onLetterSelected(letters[currentIdx])

                    // Track moves and release
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (!change.pressed) break
                        val idx = pickIndex(change.position.y)
                        if (idx != currentIdx) {
                            currentIdx = idx
                            selectedIndex = idx
                            spotlightTargetY = itemTops[idx] ?: change.position.y
                            onScrubMove(letters[idx])
                            onLetterSelected(letters[idx])
                        }
                    }

                    selectedIndex = -1
                    onScrubEnd()
                }
            },
    ) {
        // Spotlight bubble
        if (spotlightAlpha > 0f) {
            val letter = letters.getOrNull(selectedIndex.coerceAtLeast(0))?.toString() ?: ""
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = spotlightOffsetPx
                        translationY = spotlightY - spotlightSizePx / 2f
                        alpha = spotlightAlpha
                    }
                    .size(spotlightSizeDp)
                    .background(primaryColor, CircleShape),
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
            }
        }

        // Letter column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEachIndexed { index, letter ->
                val isActive = selectedIndex >= 0
                val d = if (!isActive) 0f else abs(index - selectedIndex).toFloat()
                val influence = if (isActive) exp(-(d * d) / 8f) else 0f

                val xOffset by animateFloatAsState(
                    targetValue = -36f * influence,
                    animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                    label = "railOffset_$index",
                )
                val alpha by animateFloatAsState(
                    targetValue = if (influence > 0.85f) 1f else 0.55f,
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.9f),
                    label = "railAlpha_$index",
                )
                val scale by animateFloatAsState(
                    targetValue = 1f + 0.5f * influence,
                    animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
                    label = "railScale_$index",
                )

                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            itemTops[index] = coords.positionInParent().y
                        }
                        .graphicsLayer {
                            translationX = xOffset
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                )
            }
        }
    }
}

// ── App icon helper ────────────────────────────────────────────────────────────

@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

// ── App row ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
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
