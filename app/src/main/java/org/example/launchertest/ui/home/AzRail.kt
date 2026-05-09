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
    val animatedSelectedIndex = animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
        label = "railSelectedIndex",
    )
    val railActiveFraction = animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.9f),
        label = "railActiveFraction",
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

    fun pickIndex(y: Float): Int {
        return pickRailIndex(y, letters.size, railHeightPx)
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

                    railDragY = renderedRailDragY
                    isRailDragging = 1
                    railDragY = shiftedRailOffsetFor(down.position.y, railDragY, railHeightPx)

                    var currentIdx = pickIndex(down.position.y - railDragY)
                    if (currentIdx < 0) {
                        isRailDragging = 0
                        return@awaitEachGesture
                    }
                    selectedIndex = currentIdx
                    spotlightTargetY = railItemCenter(currentIdx, letters.size, railHeightPx)
                        .takeUnless { it.isNaN() } ?: down.position.y
                    onScrubStart(letters[currentIdx])
                    onLetterSelected(letters[currentIdx])

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (!change.pressed) break
                        railDragY = shiftedRailOffsetFor(change.position.y, railDragY, railHeightPx)
                        val idx = pickIndex(change.position.y - railDragY)
                        if (idx != currentIdx) {
                            currentIdx = idx
                            selectedIndex = idx
                            spotlightTargetY = railItemCenter(idx, letters.size, railHeightPx)
                                .takeUnless { it.isNaN() } ?: change.position.y
                            onScrubMove(letters[idx])
                            onLetterSelected(letters[idx])
                        }
                    }

                    selectedIndex = -1
                    onScrubEnd()
                    isRailDragging = 0
                    railDragY = 0f
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
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.graphicsLayer {
                            val d = abs(index - animatedSelectedIndex.value)
                            val influence = railActiveFraction.value * exp(-(d * d) / 8f)
                            translationX = -200f * influence
                            scaleX = 1f + 0.5f * influence
                            scaleY = 1f + 0.5f * influence
                            this.alpha = if (influence > 0.85f) 1f else 0.55f
                        },
                    )
                }
            }
        }
    }
}
