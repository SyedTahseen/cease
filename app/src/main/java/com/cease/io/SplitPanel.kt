package com.cease.io

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val HALF = 0.65f
private const val FULL = 1f
private const val CLOSE_BELOW = 0.45f

private class PanelState {
    var startedAtHalf: Boolean? = null
    var animation: Job? = null

    fun resetGesture() { startedAtHalf = null }

    fun cancelAnimation() { animation?.cancel() }
}

@Composable
fun SplitPanel(
    onDismiss: () -> Unit,
    onFullChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var fraction by remember { mutableFloatStateOf(HALF) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state = remember { PanelState() }
    val dismiss by rememberUpdatedState(onDismiss)

    val isFull = fraction >= 0.97f
    LaunchedEffect(isFull) { onFullChange(isFull) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val total by rememberUpdatedState(constraints.maxHeight.toFloat())

        fun lowerBound() = if (state.startedAtHalf == true) 0f else HALF

        fun settle(velocityY: Float) {
            val f = fraction
            if (f < HALF - 0.001f) {
                val close = f < CLOSE_BELOW || velocityY > 800f
                state.cancelAnimation()
                state.animation = scope.launch {
                    animate(f, if (close) 0f else HALF) { v, _ -> fraction = v }
                    if (close) dismiss()
                }
                return
            }
            if (f <= HALF + 0.001f || f >= FULL) return
            val target = when {
                velocityY < -400f -> FULL
                velocityY > 400f -> HALF
                f > 0.825f -> FULL
                else -> HALF
            }
            state.cancelAnimation()
            state.animation = scope.launch { animate(f, target) { v, _ -> fraction = v } }
        }

        fun drag(dy: Float): Float {
            val old = fraction
            val new = (old - dy / total).coerceIn(lowerBound(), FULL)
            fraction = new
            return (old - new) * total
        }

        val connection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput && state.startedAtHalf == null) state.startedAtHalf = fraction <= HALF + 0.001f
                    if (available.y < 0f && fraction < FULL) {
                        state.cancelAnimation()
                        return Offset(0f, drag(available.y))
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    val canShrink = fraction > lowerBound() &&
                        (fraction > HALF || source == NestedScrollSource.UserInput)
                    if (available.y > 0f && canShrink) {
                        state.cancelAnimation()
                        return Offset(0f, drag(available.y))
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    state.resetGesture()
                    settle(available.y)
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    state.resetGesture()
                    settle(available.y)
                    return Velocity.Zero
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f * (fraction / HALF).coerceIn(0f, 1f)))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() },
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { (total * fraction).toDp() })
                .clip(
                    with(density) {
                        val r = (70f * (1f - ((fraction - 0.9f) / 0.1f).coerceIn(0f, 1f))).toDp()
                        RoundedCornerShape(topStart = r, topEnd = r)
                    },
                )
                .background(FZ.c.panel)
                .pointerInput(Unit) { detectTapGestures { } }
                .nestedScroll(connection),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}
