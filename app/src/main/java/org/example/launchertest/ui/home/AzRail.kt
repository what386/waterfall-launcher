package org.example.launchertest.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
    val spotlightSizePx = with(LocalDensity.current) { spotlightSizeDp.toPx() }
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
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                        ?: return@awaitEachGesture
                    if (!down.pressed) return@awaitEachGesture
                    down.consume()

                    var currentIdx = pickIndex(down.position.y)
                    selectedIndex = currentIdx
                    spotlightTargetY = itemTops[currentIdx] ?: down.position.y
                    onScrubStart(letters[currentIdx])
                    onLetterSelected(letters[currentIdx])

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
