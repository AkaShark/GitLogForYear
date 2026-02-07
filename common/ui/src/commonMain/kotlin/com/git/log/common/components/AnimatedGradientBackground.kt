package com.git.log.common.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.git.log.common.tool.AppColors

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val color1 by infiniteTransition.animateColor(
        initialValue = AppColors.GradientStart1,
        targetValue = AppColors.GradientEnd1,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )

    val color2 by infiniteTransition.animateColor(
        initialValue = AppColors.GradientStart2,
        targetValue = AppColors.GradientEnd2,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    val color3 by infiniteTransition.animateColor(
        initialValue = AppColors.GradientStart3,
        targetValue = AppColors.GradientEnd3,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(color1, color2, color3)
                )
            )
    ) {
        content()
    }

}