package com.git.log.common.git.result

import com.git.log.common.git.model.CommitTimeOfDay
import com.git.log.common.git.model.ReportDataSource
import com.git.log.common.git.model.ResultSectionUpdateRecipe
import com.git.log.common.git.model.SourceLanguage
import javax.xml.transform.Source

class ResultPackage(val dataSource: ReportDataSource) {
    var badgeEarned = listOf<ResultSectionUpdateRecipe>()
        private set

    // git分析 计算结果缓存
    var totalCommits: Int = 0; private set
    var totalIncreasedLines: Int = 0; private set
    var totalDecreasedLines: Int = 0; private set
    var totalEmptyLines: Int = 0; private set
    var activeDays: Int = 0; private set
    var dailyCommits: Map<Int, Int> = emptyMap(); private set
    var languageStats: Map<SourceLanguage, Pair<Int, Int>> = emptyMap(); private set
    var commitTimeStats: Map<CommitTimeOfDay, Int> = emptyMap(); private set
    var topWords: List<Pair<String, Int>> = emptyList(); private set
    var busiesDay: Pair<String, Int>? = null; private set
    var reportHash: String = ""; private set
    var monthlyStats: Map<Int, Int> = emptyMap(); private set

    fun update() {

    }

    private fun extractHourFromDate(date: String): Int? {
        TODO()
    }

    private fun extractMonthFromDate(date: String): Int? {
        TODO()
    }

    private fun generateReportHash(): String {
        TODO()
    }

    private fun calculateAchievements(): List<ResultSectionUpdateRecipe> {
        TODO()
    }



}