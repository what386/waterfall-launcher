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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp

const val FavoritesRailItem = '★'

// How far left (in px) the user has dragged relative to the rail's own width.
// 0 = touching the rail edge, more negative = further left.
// Clamped so dragging right of the rail doesn't invert things.
private fun touchXInfluence(touchX: Float, railWidthPx: Float): Float {
    // touchX is in rail-local coords; rail sits at the right edge of the screen.
    // Moving left means smaller touchX values (toward 0 or negative).
    // We express influence as a 0..1 value where 1 = dragged ~150dp left.
    val leftwardPx = (railWidthPx - touchX).coerceAtLeast(0f)
    val maxPx = 400f // ~150dp on a typical screen; influence saturates here
    return (leftwardPx / maxPx).coerceIn(0f, 1f)
}

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
    var railWidthPx by remember { mutableFloatStateOf(0f) }
    var railDragY by remember { mutableFloatStateOf(0f) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var isRailDragging by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val spotlightSizeDp = 56.dp
    val spotlightSizePx = with(density) { spotlightSizeDp.toPx() }
    val spotlightBaseOffsetPx = with(density) { (-104).dp.toPx() }

    val pullDistancePx = with(density) { 220.dp.toPx() }

    val rawPull = if (isRailDragging > 0 && railWidthPx > 0f) {
        val railCenterX = railWidthPx / 2f
        ((railCenterX - touchX) / pullDistancePx).coerceIn(-1f, 1f)
    } else {
        0f
    }

    val animatedPull by animateFloatAsState(
        targetValue = rawPull,
        animationSpec = if (isRailDragging > 0) {
            snap()
        } else {
            spring(stiffness = 420f, dampingRatio = 0.82f)
        },
        label = "azRailPull",
    )

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

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.82f),
        label = "railSelectedIndex",
    )

    val railActiveFraction by animateFloatAsState(
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

    fun pickIndex(y: Float): Int {
        return pickRailIndex(y, letters.size, railHeightPx)
    }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                railHeightPx = coords.size.height.toFloat()
                railWidthPx = coords.size.width.toFloat()
            }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                        ?: return@awaitEachGesture

                    if (!down.pressed) return@awaitEachGesture
                    down.consume()

                    touchX = down.position.x
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

                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScrubStart(letters[currentIdx])
                    onLetterSelected(letters[currentIdx])

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()

                        if (!change.pressed) break

                        touchX = change.position.x
                        railDragY = shiftedRailOffsetFor(change.position.y, railDragY, railHeightPx)

                        val idx = pickIndex(change.position.y - railDragY)
                        if (idx != currentIdx) {
                            currentIdx = idx
                            selectedIndex = idx
                            spotlightTargetY = railItemCenter(idx, letters.size, railHeightPx)
                                .takeUnless { it.isNaN() } ?: change.position.y

                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onScrubMove(letters[idx])
                            onLetterSelected(letters[idx])
                        }
                    }

                    selectedIndex = -1
                    onScrubEnd()
                    isRailDragging = 0
                    railDragY = 0f
                    touchX = railWidthPx / 2f
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = renderedRailDragY
                },
        ) {
            if (spotlightAlpha > 0f) {
                val letter = letters.getOrNull(selectedIndex.coerceAtLeast(0))?.toString() ?: ""

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = spotlightBaseOffsetPx - 70f * animatedPull
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
                            val d = index - animatedSelectedIndex
                            val distance = abs(d)

                            val influence = railActiveFraction * exp(-(distance * distance) / 8f)

                            val leftPull = animatedPull.coerceAtLeast(0f)
                            val rightPull = (-animatedPull).coerceAtLeast(0f)

                            val amplitude = 210f + 180f * leftPull - 90f * rightPull
                            val setpoint = -20f * leftPull + 105f * rightPull

                            translationX = setpoint - amplitude * influence

                            val scaleBoost = 0.5f + 0.25f * leftPull - 0.18f * rightPull
                            scaleX = 1f + scaleBoost * influence
                            scaleY = 1f + scaleBoost * influence

                            this.alpha = if (influence > 0.85f) 1f else 0.55f
                        },
                    )
                }
            }
        }
    }
}
