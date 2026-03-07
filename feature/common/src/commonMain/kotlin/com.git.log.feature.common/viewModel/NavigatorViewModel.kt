package com.git.log.feature.common.viewModel

import AppScreen
import com.git.log.common.git.model.SourcePackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.git.log.common.git.result.ResultPackage


class NavigatorViewModel {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun startAnalysis(sourcePackage: SourcePackage) {
        _currentScreen.value = AppScreen.Analysis(sourcePackage)
    }

    fun showResult(resultPackage: ResultPackage) {
        _currentScreen.value = AppScreen.Result(resultPackage)
    }

    fun home() {
        _currentScreen.value = AppScreen.Home
    }

}