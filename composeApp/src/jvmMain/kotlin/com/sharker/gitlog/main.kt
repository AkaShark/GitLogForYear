package com.sharker.gitlog

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "git_log_agent",
    ) {
        App()
    }
}