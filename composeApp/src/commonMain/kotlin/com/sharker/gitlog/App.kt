package com.sharker.gitlog

import AppScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.git.log.feature.common.viewModel.NavigatorViewModel
import com.git.log.feature.home.Home

@Composable
fun App() {
    val viewModel = remember { NavigatorViewModel() }
    val screen by viewModel.currentScreen.collectAsState()

    when (val current = screen) {
        is AppScreen.Home -> {
            Home(
                onStartAnalysis = { sourcePackage ->
                    viewModel.startAnalysis(sourcePackage)
                }
            )
        }

        is AppScreen.Analysis -> {

        }

        is AppScreen.Result -> {

        }
    }

}