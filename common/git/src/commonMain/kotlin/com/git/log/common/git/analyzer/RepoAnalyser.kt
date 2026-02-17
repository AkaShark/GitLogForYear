package com.git.log.common.git.analyzer

import com.git.log.common.git.filter.CommitFileFilter
import com.git.log.common.git.model.GitCommitResult
import com.git.log.common.git.model.GitRepoResult
import com.git.log.common.git.model.ReportDataSource
import com.git.log.common.git.parser.GitDiffParser
import com.git.log.common.git.parser.GitLogParser
import com.git.log.common.git.utils.DictionaryBuilder
import kotlin.random.Random

class RepoAnalyser {
    companion object {
        val shared = RepoAnalyser()
    }

    // 会话管理
    private var currentSession: String = ""
    private var currentResult = mutableListOf<GitRepoResult>()
    private var requiredEmails = listOf<String>()
    private var commitHashSet = mutableListOf<String>()

    // 字典会话
    private var dictIncreaseSession: String = ""
    private var dictDecreaseSession: String = ""
    private var dictCommitSession: String = ""

    // 分析年份
    var requireYear: Int = 2025

    fun beginSession(): String {
        // 仓库 session
        val session = Random.nextLong().toString(36)
        currentSession = session
        currentResult.clear()
        commitHashSet.clear()

        // 词频 session
        dictIncreaseSession = DictionaryBuilder.sharedIncrease.beginSession()
        dictDecreaseSession = DictionaryBuilder.sharedDecrease.beginSession()
        dictCommitSession = DictionaryBuilder.sharedCommit.beginSession()

        return session
    }

    fun submitEmails(emails: List<String>) {
        requiredEmails = emails.map {
            it.lowercase()
        }
    }

    // 分析单个仓库
    suspend fun analysis(
        repoPath: String,
        session: String,
        gitLogOutput: String,
        gitDiffProvider: suspend (String) -> String, // hash -> diffOutput
        onProgress: (String) -> Unit = {}
    ) {
        if (session != currentSession) return

        // 解析 Git Log
        val logElements = GitLogParser.parse(gitLogOutput)
        // 过滤: 匹配邮箱 + 年份 + 去重
        val filtered = logElements.filter { element ->
            val emailMatch = requiredEmails.isEmpty() || requiredEmails.contains(element.email.lowercase())
            val yearMatch = element.date.contains(requireYear.toString())
            val notDuplicate = commitHashSet.add(element.hash)
            emailMatch && yearMatch && notDuplicate
        }

        onProgress("正在分析 ${filtered.size} 条提交记录...")

        // step2: 解析每个 commit 的 diff
        val commits = mutableListOf<GitCommitResult>()

        for ((index, element) in filtered.withIndex()) {
            if (session != currentSession) return

            try {
                val diffOutput = gitDiffProvider(element.hash)
                val diffFiles = GitDiffParser.parse(diffOutput, dictIncreaseSession, dictDecreaseSession)

                val filteredDiffs = diffFiles.filter { it ->
                    !CommitFileFilter.shared.shouldBlock(it.filePath)
                }

                // 提交信息
                DictionaryBuilder.sharedCommit.feed(element.note, dictCommitSession)

                commits.add(
                    GitCommitResult(
                        hash = element.hash,
                        email = element.email,
                        date = element.date,
                        note = element.note,
                        diffFiles = filteredDiffs
                    )
                )
            } catch (e: Exception) {
                // commit 解释失败
            }

            if (index % 10 == 0) {
                onProgress("正在分析 ${repoPath.substringAfterLast('/')}... ($index/${filtered.size})")
            }
        }

        currentResult.add(GitRepoResult(repoPath = repoPath, commits = commits))
    }

    fun commitResult(): ReportDataSource {
        val dataSource = ReportDataSource (
            repoResult = currentResult.toList(),
            dictionaryIncrease = DictionaryBuilder.sharedIncrease.commitSession(dictIncreaseSession),
            dictionaryDecrease = DictionaryBuilder.sharedDecrease.commitSession(dictDecreaseSession),
            dictionaryCommit = DictionaryBuilder.sharedCommit.commitSession(dictCommitSession)
        )

        // 结束会话重置状态
        beginSession()

        return dataSource
    }


}