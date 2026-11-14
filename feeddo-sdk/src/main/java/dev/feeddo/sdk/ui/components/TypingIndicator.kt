package dev.feeddo.sdk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.feeddo.sdk.theme.FeeddoTheme

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: Dp = 6.dp,
    theme: FeeddoTheme? = null
) {
    val indicatorColor = color ?: theme?.colors?.textSecondary ?: Color(0xFF6B7280)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BouncingDot(delayMillis = 0, color = indicatorColor, size = size)
        BouncingDot(delayMillis = 200, color = indicatorColor, size = size)
        BouncingDot(delayMillis = 400, color = indicatorColor, size = size)
    }
}

@Composable
private fun BouncingDot(
    delayMillis: Int,
    color: Color,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BouncingDot")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotOffset"
    )
    val alphaVal by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { translationY = offsetY }
            .clip(CircleShape)
            .background(color.copy(alpha = alphaVal))
    )
}
