package com.git.log.common.git.parser

import com.git.log.common.git.model.GitLogElement

// Git Log 解析器
object GitLogParser {
    fun parse(output: String): List<GitLogElement> {
        val results = mutableListOf<GitLogElement>()

        var currentHash: String? = null
        var authorEmail: String? = null
        var authorName: String? = null
        var date: String? = null
        val lineBuffer = mutableListOf<String>()

        for (line in output.lines()) {
            when {
                // 首行
                line.startsWith("commit ") -> {
                    // 提交上一个
                    submitBarrier(currentHash, authorEmail, date, authorName, lineBuffer, results)
                    currentHash = line.substringAfter("commit ").trim().lowercase()
                    authorEmail = null
                    date = null
                    authorName = null
                    lineBuffer.clear()
                }

                line.startsWith("Author: ") -> {
                    authorEmail = line.substringAfter("<").substringBefore(">").trim().lowercase()
                    authorName = line.substringBefore(" <").substringAfter("Author: ").trim()
                }


                line.startsWith("Date: ") -> {
                    date = line.substringAfter("Date:").trim()
                }

                else -> {
                    lineBuffer.add(line.trim())
                }
            }
        }
        // 最后一个 commit
        submitBarrier(currentHash, authorEmail, date, authorName, lineBuffer, results)
        return results
    }

    private fun submitBarrier(
        hash: String?,
        email: String?,
        date: String?,
        name: String?,
        lineBuffer: MutableList<String>,
        results: MutableList<GitLogElement>
    ) {
        if (hash == null || email == null || date == null || name == null) return

        val note = lineBuffer
            .filter { it.isNotBlank() }
            .joinToString { "\n" }
            .trim()

        results.add(
            GitLogElement(
                hash = hash,
                email = email,
                date = date,
                note = note,
                name = name
            )
        )
    }
}