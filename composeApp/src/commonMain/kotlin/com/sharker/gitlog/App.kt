package com.sharker.gitlog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.git.log.common.components.AnimatedGradientBackground
import com.git.log.feature.home.Home
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AnimatedGradientBackground(
                modifier = Modifier,
                content = {
                   Home()
                }
            )
        }
    }
}