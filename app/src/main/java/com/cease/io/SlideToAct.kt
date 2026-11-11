package com.cease.io

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val CenterButtonRise = 28.dp

@Composable
fun SlideToAct(
    label: String,
    enabled: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    style: SliderStyle = SliderStyle.LEFT,
    onLabelClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    if (style == SliderStyle.CENTER) {
        CenterFreezeButton(label, enabled, onComplete, modifier, onLabelClick, trailing)
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
            modifier = Modifier
                .align(Alignment.Center)
                .alpha((1f - progress * 1.6f).coerceIn(0f, 1f))
                .labelClickable(onLabelClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                .background(colors.thumbOff, CircleShape)
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
            HandleBackground(enabled)
            CeaseStarIcon(color = Color.White, iconSize = 38.dp)
        }
    }
}

@Composable
private fun CenterFreezeButton(
    label: String,
    enabled: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier,
    onLabelClick: (() -> Unit)?,
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
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(barHeight).background(colors.track))

        Box(
            Modifier.align(Alignment.BottomStart).height(barHeight).padding(start = 24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                color = colors.barInk,
                fontSize = FZType.title,
                modifier = Modifier.labelClickable(onLabelClick).padding(horizontal = 8.dp, vertical = 14.dp),
            )
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
                .background(colors.thumbOff, CircleShape)
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
            HandleBackground(enabled)
            CeaseStarIcon(Modifier.rotate(rotation.value), Color.White, 42.dp)
        }
    }
}

@Composable
private fun Modifier.labelClickable(onClick: (() -> Unit)?): Modifier =
    if (onClick == null) this
    else this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

@Composable
private fun HandleBackground(enabled: Boolean) {
    if (!enabled) return
    val transition = rememberInfiniteTransition(label = "handleFlow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val t = phase
        val w = size.width
        drawRect(FlowRoyal)
        fun blob(color: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = Offset(w * cx, w * cy),
                    radius = w * r,
                ),
                radius = w * r,
                center = Offset(w * cx, w * cy),
            )
        }
        blob(FlowRoyal, 0.5f + 0.45f * cos(t + 3.5f), 0.5f + 0.40f * sin(2 * t + 1f), 0.70f)
        blob(FlowVivid, 0.5f + 0.40f * cos(t + 1f), 0.5f + 0.35f * sin(t + 2f), 0.80f)
        blob(FlowSky, 0.5f + 0.35f * sin(t), 0.5f + 0.30f * cos(2 * t), 0.65f)
        blob(FlowLight.copy(alpha = 0.9f), 0.5f + 0.40f * sin(2 * t + 0.5f), 0.5f + 0.40f * cos(t), 0.45f)
    }
}

private val FlowRoyal = Color(0xFF244FD1)
private val FlowVivid = Color(0xFF0D86EE)
private val FlowSky = Color(0xFF3CBCFD)
private val FlowLight = Color(0xFF8BD5FC)
