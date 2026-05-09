package org.example.launchertest.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp

const val FavoritesRailItem = '★'

@Composable
fun AzRail(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    onScrubStart: (Char) -> Unit,
    onScrubMove: (Char) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val itemCenters = remember(letters) { FloatArray(letters.size) { Float.NaN } }
    var spotlightTargetY by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    var railDragY by remember { mutableFloatStateOf(0f) }
    var isRailDragging by remember { mutableIntStateOf(0) }

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
    val renderedRailDragY by animateFloatAsState(
        targetValue = if (isRailDragging > 0) railDragY else 0f,
        animationSpec = if (isRailDragging > 0) {
            snap()
        } else {
            spring(stiffness = 520f, dampingRatio = 0.78f)
        },
        label = "railDragY",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val spotlightSizeDp = 56.dp
    val spotlightSizePx = with(LocalDensity.current) { spotlightSizeDp.toPx() }
    val spotlightOffsetPx = with(LocalDensity.current) { (-104).dp.toPx() }

    fun overscrollOffsetFor(y: Float): Float {
        return when {
            y < 0f -> y
            railHeightPx > 0f && y > railHeightPx -> y - railHeightPx
            else -> 0f
        }
    }

    fun pickIndex(y: Float): Int {
        if (letters.isEmpty()) return -1
        var best = 0
        var bestDistance = Float.MAX_VALUE
        for (idx in itemCenters.indices) {
            val center = itemCenters[idx]
            if (center.isNaN()) continue
            val distance = abs(center - y)
            if (distance < bestDistance) {
                best = idx
                bestDistance = distance
            }
        }
        return best.coerceIn(0, letters.lastIndex)
    }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                railHeightPx = coords.size.height.toFloat()
            }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                        ?: return@awaitEachGesture
                    if (!down.pressed) return@awaitEachGesture
                    down.consume()

                    isRailDragging = 1
                    railDragY = overscrollOffsetFor(down.position.y)

                    var currentIdx = pickIndex(down.position.y - railDragY)
                    if (currentIdx < 0) {
                        isRailDragging = 0
                        return@awaitEachGesture
                    }
                    selectedIndex = currentIdx
                    spotlightTargetY = itemCenters[currentIdx].takeUnless { it.isNaN() } ?: down.position.y
                    onScrubStart(letters[currentIdx])
                    onLetterSelected(letters[currentIdx])

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (!change.pressed) break
                        railDragY = overscrollOffsetFor(change.position.y)
                        val idx = pickIndex(change.position.y - railDragY)
                        if (idx != currentIdx) {
                            currentIdx = idx
                            selectedIndex = idx
                            spotlightTargetY = itemCenters[idx].takeUnless { it.isNaN() } ?: change.position.y
                            onScrubMove(letters[idx])
                            onLetterSelected(letters[idx])
                        }
                    }

                    selectedIndex = -1
                    onScrubEnd()
                    isRailDragging = 0
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = renderedRailDragY },
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
                        targetValue = -200f * influence,
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
                                itemCenters[index] = coords.positionInParent().y + coords.size.height / 2f
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
}
