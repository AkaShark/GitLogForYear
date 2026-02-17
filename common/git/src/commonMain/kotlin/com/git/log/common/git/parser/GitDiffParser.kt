package com.git.log.common.git.parser

import com.git.log.common.git.model.DiffMode
import com.git.log.common.git.model.GitFileDiff
import com.git.log.common.git.model.SourceLanguage
import com.git.log.common.git.utils.DictionaryBuilder

// Git Diff 解析器

// 状态机
enum class DiffParseStatus {
    NONE, // 无状态
    HEADER, // 解析 Header
    BODY, // 解析 Body
}

object GitDiffParser {
    suspend fun parse(
        diffOutput: String,
        dictIncreaseSession: String,
        dictDecreaseSession: String
    ): List<GitFileDiff> {
        val result = mutableListOf<GitFileDiff>()

        var status: DiffParseStatus = DiffParseStatus.NONE
        var currentDiff: GitFileDiff? = null
        var increased = 0
        var decreased = 0
        var emptyLine = 0

        // 后续可以加一些 body 的分析
        fun commitBodyForAnalysis() {

        }

        fun commitBodyBarrier() {
            commitBodyForAnalysis()

            currentDiff?.let {
                result.add(
                    it.copy(
                        increasedLine = increased,
                        decreasedLine = decreased,
                        emptyLineAdded = emptyLine
                    )
                )
            }
            currentDiff = null
            increased = 0
            decreased = 0
            emptyLine = 0
        }

        suspend fun switchStatus(newStatus: DiffParseStatus) {
            val preStatus = status
            status = newStatus

            when (preStatus) {
                DiffParseStatus.BODY -> commitBodyBarrier()
                DiffParseStatus.HEADER -> {

                }
                DiffParseStatus.NONE -> {

                }
            }

            for (line in diffOutput.lines()) {
                when {
                    line.startsWith("diff --git") -> {
                        switchStatus(DiffParseStatus.HEADER)
                        val parts = line.split(" ")
                        val filePath = if (parts.size >= 4) {
                            parts.last().removePrefix("b/").trim()
                        } else {
                            ""
                        }
                        val language = SourceLanguage.fromFilePath(filePath)
                        currentDiff = GitFileDiff(
                            mode = DiffMode.MODIFY,
                            filePath = filePath,
                            language = language
                        )
                    }

                    line.startsWith("new file") -> {
                        currentDiff = currentDiff?.copy(mode = DiffMode.ADD)
                    }

                    line.startsWith("deleted file") -> {
                        currentDiff = currentDiff?.copy(mode = DiffMode.DELETE)
                    }

                    line.startsWith("@@") -> {
                        if (status != DiffParseStatus.BODY) {
                            switchStatus(DiffParseStatus.BODY)
                        }
                    }

                    status == DiffParseStatus.BODY -> {
                        when {
                            line.startsWith("+") && !line.startsWith("+++") -> {
                                val content = line.drop(1)
                                increased ++
                                if (content.isBlank()) emptyLine++
                                DictionaryBuilder.sharedIncrease.feed(content, dictIncreaseSession)
                            }

                            line.startsWith("-") && !line.startsWith("---") -> {
                                decreased ++
                                DictionaryBuilder.sharedDecrease.feed(line.drop(1), dictDecreaseSession)
                            }
                        }
                    }
                }
            }
        }

        commitBodyBarrier()

        return result.toList()
    }
}