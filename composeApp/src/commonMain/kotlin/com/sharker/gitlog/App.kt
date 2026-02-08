package com.sharker.gitlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.git.log.common.components.AnimatedGradientBackground
import com.git.log.common.tool.AppColors
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
                   AppContent()
                }
            )
        }
    }
}

@Composable
fun AppContent() {
    Column (
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // title
        Text(
            text = "Git Log For Year",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.TextPrimary,
            modifier = Modifier
                .padding(top = 20.dp)
        )
        // content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Home()
        }
        // tail
        Column (
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Made by ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = AppColors.TextSecondary,
                )

                Text(
                    text = "@AkaShark",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                        fontSize = 10.sp
                    ),
                    color = AppColors.TextSecondary,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            uriHandler.openUri("https://github.com/AkaShark")
                        }
                )
            }
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thanks to ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "@Lakr233",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.Underline,
                        fontSize = 10.sp
                    ),
                    color = AppColors.TextSecondary,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            uriHandler.openUri("https://github.com/Lakr233")
                        }
                )
            }

        }
    }
}