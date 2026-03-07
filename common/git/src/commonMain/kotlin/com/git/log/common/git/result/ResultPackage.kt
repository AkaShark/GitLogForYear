package com.git.log.common.git.result

import com.git.log.common.git.model.Achievement
import com.git.log.common.git.model.CommitTimeOfDay
import com.git.log.common.git.model.GitCommitResult
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
        val allCommits = dataSource.repoResult.flatMap { it.commits }
        val allDiffs = allCommits.flatMap { it.diffFiles }

        // 基础统计
        totalCommits = allCommits.size
        totalIncreasedLines = allDiffs.sumOf { it.increasedLine }
        totalDecreasedLines = allDiffs.sumOf { it.decreasedLine }
        totalEmptyLines = allDiffs.sumOf { it.emptyLineAdded }

        // 每日提交统计
        val dailyMap = mutableMapOf<String, Int>()
        for (commit in allCommits) {
            val dayKey = commit.date.take(15)
            dailyMap[dayKey] = (dailyMap[dayKey] ?: 0) + 1
        }
        activeDays = dailyMap.size

        // dayOfYear 简化计算
        val dayOfYearMap = mutableMapOf<Int, Int>()
        for ((index, entry) in dailyMap.entries.withIndex()) {
            dayOfYearMap[index] = entry.value
        }
        dailyCommits = dayOfYearMap

        // 语言统计
        val langMap = mutableMapOf<SourceLanguage, Pair<Int, Int>>()
        for (diff in allDiffs) {
            val lang = diff.language ?: continue
            val current = langMap[lang] ?: Pair(0, 0)
            langMap[lang] = Pair(current.first + diff.increasedLine, current.second + diff.decreasedLine)
        }
        languageStats = langMap.toList().sortedByDescending {
            it.second.first + it.second.second
        }.toMap()

        // 提交时间统计
        val timeMap = mutableMapOf<CommitTimeOfDay, Int>()
        for (commit in allCommits) {
            val hour = extractHourFromDate(commit.date) ?: continue
            val timeOfDay = CommitTimeOfDay.fromHour(hour)
            timeMap[timeOfDay] = (timeMap[timeOfDay] ?: 0) + 1
        }
        commitTimeStats = timeMap

        // 高频词
        val allDict = mutableMapOf<String, Int>()
        dataSource.dictionaryIncrease.forEach { (k, v) -> allDict[k] = (allDict[k] ?: 0) + v }
        dataSource.dictionaryCommit.forEach { (k, v) -> allDict[k] = (allDict[k] ?: 0) + v }
        topWords = allDict.toList().sortedByDescending { it.second }.take(20)

        // 最忙碌的一天
        busiesDay = dailyMap.maxByOrNull { it.value }?.toPair()

        // 报告 hash
        reportHash = generateReportHash(allCommits)

        // 月度统计
        val monthMap = mutableMapOf<Int, Int>()
        for (commit in allCommits) {
            val month = extractMonthFromDate(commit.date) ?: continue
            val lines = commit.diffFiles.sumOf { it.increasedLine + it.decreasedLine }
            monthMap[month] = (monthMap[month] ?: 0) + lines
        }
        monthlyStats = monthMap

        // 成就
        badgeEarned = calculateAchievements()
    }

    private fun extractHourFromDate(date: String): Int? {
        // 尝试从 "Sun Apr 19 01:20:44 2020 +0800" 格式提取小时
        val timePattern = Regex("""(\d{2}):\d{2}:\d{2}""")
        val match = timePattern.find(date) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun extractMonthFromDate(date: String): Int? {
        val months = mapOf(
            "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
            "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
            "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
        )
        for ((name, number) in months) {
            if (date.contains(name)) return number
        }
        return null
    }

    private fun generateReportHash(commits: List<GitCommitResult>): String {
        val hashInput = commits.joinToString("") { it.hash }
        // 简单 hash: 取字符串的 hashCode 转16进制
        val hash = hashInput.hashCode().toUInt().toString(16).uppercase()
        return "0x${hash.padStart(8, '0')}"
    }

    private fun calculateAchievements(): List<ResultSectionUpdateRecipe> {
        val achievements = mutableListOf<ResultSectionUpdateRecipe>()
        // 编程语言大师
        if (languageStats.size >= 6) {
            achievements.add(
                ResultSectionUpdateRecipe(
                    Achievement("编程语言大师", "今年的提交中熟练使用了超过六种语言")
                )
            )
        }

        // 时间段成就
        val mostActiveTime = commitTimeStats.maxByOrNull { it.value }?.key
        when (mostActiveTime) {
            CommitTimeOfDay.MIDNIGHT -> achievements.add(
                ResultSectionUpdateRecipe(Achievement("夜猫子", "喜欢在午夜时分提交代码"))
            )
            CommitTimeOfDay.MORNING -> achievements.add(
                ResultSectionUpdateRecipe(Achievement("早睡早起身体好", "喜欢在早晨提交代码"))
            )
            CommitTimeOfDay.NOON -> achievements.add(
                ResultSectionUpdateRecipe(Achievement("干饭人！干饭魂！", "喜欢在中午提交代码"))
            )
            else -> {}
        }

        // 空行大师
        if (totalEmptyLines > 233333) {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("摸鱼流量百分百", "写了超过 233333 行空行"))
            )
        }

        // 单日提交成就
        val maxDayCommits = busiesDay?.second ?: 0
        if (maxDayCommits > 100) {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("我是奥特曼", "今年有一天的提交次数超过百次"))
            )
        } else if (maxDayCommits > 50) {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("Bugfeature 制造机", "今年有一天的提交次数超过五十次"))
            )
        }

        // 全勤战士
        if (activeDays >= 365) {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("全勤战士", "全年每天都有提交记录"))
            )
        }

        // 文明语言检查
        val profanityWords = setOf("fuck", "shit", "damn", "hell", "crap")
        val hasProfanity = topWords.any { profanityWords.contains(it.first.lowercase()) }
        if (hasProfanity) {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("文明语言学者", "在高频词中发现了特别的表达"))
            )
        } else {
            achievements.add(
                ResultSectionUpdateRecipe(Achievement("文明语言大师", "全年代码中保持了文明用语"))
            )
        }

        return achievements
    }



}