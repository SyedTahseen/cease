package com.freezer.io

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val CenterButtonRise = 28.dp

@Composable
fun SlideToAct(
    label: String,
    enabled: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    style: SliderStyle = SliderStyle.LEFT,
    onTrackClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    if (style == SliderStyle.CENTER) {
        CenterFreezeButton(label, enabled, onComplete, modifier, onTrackClick, trailing)
        return
    }
    val right = style == SliderStyle.RIGHT
    val sign = if (right) -1 else 1
    val colors = FZ.c
    val trackHeight = 58.dp
    val density = LocalDensity.current
    val thumbPx = with(density) { trackHeight.toPx() }

    var widthPx by remember { mutableIntStateOf(0) }
    val maxOffset = (widthPx - thumbPx).coerceAtLeast(1f)
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val progress = (offset.value / maxOffset).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
            .background(colors.track)
            .trackClickable(onTrackClick)
            .onSizeChanged { widthPx = it.width },
        contentAlignment = if (right) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(with(density) { (offset.value + thumbPx).toDp() })
                .background(colors.thumb.copy(alpha = 0.22f * progress)),
        )

        Text(
            text = label,
            color = colors.barInk,
            fontSize = FZType.title,
            modifier = Modifier.align(Alignment.Center).alpha((1f - progress * 1.6f).coerceIn(0f, 1f)),
        )

        Row(
            Modifier
                .align(if (right) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 10.dp)
                .alpha((1f - progress * 2f).coerceIn(0f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
        ) { trailing() }

        Box(
            Modifier
                .offset { IntOffset(sign * offset.value.roundToInt(), 0) }
                .size(trackHeight)
                .clip(CircleShape)
                .background(if (enabled) colors.thumb else colors.thumbOff, CircleShape)
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + sign * delta).coerceIn(0f, maxOffset)) }
                    },
                    onDragStopped = {
                        val done = offset.value >= maxOffset * 0.85f
                        if (done) onComplete()
                        scope.launch { offset.animateTo(0f) }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            SnowflakeIcon(color = Color.White, iconSize = 30.dp)
        }
    }
}

@Composable
private fun CenterFreezeButton(
    label: String,
    enabled: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier,
    onTrackClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    val colors = FZ.c
    val barHeight = 68.dp
    val buttonSize = 64.dp
    val rise = CenterButtonRise
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }

    Box(modifier.fillMaxWidth().height(barHeight + rise)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(barHeight).background(colors.track).trackClickable(onTrackClick))

        Box(
            Modifier.align(Alignment.BottomStart).height(barHeight).padding(start = 32.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = label, color = colors.barInk, fontSize = FZType.title)
        }

        Row(
            Modifier.align(Alignment.BottomEnd).height(barHeight).padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { trailing() }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(if (enabled) colors.thumb else colors.thumbOff, CircleShape)
                .clickable(enabled = enabled && !spinning) {
                    spinning = true
                    onComplete()
                    scope.launch {
                        rotation.snapTo(0f)
                        rotation.animateTo(720f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
                        spinning = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            SnowflakeIcon(Modifier.rotate(rotation.value), Color.White, 30.dp)
        }
    }
}

@Composable
private fun Modifier.trackClickable(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) this
    else this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
