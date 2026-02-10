package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// Git 文件差异
@Serializable
data class GitFileDiff (
    val mode: DiffMode = DiffMode.MODIFY,
    val filePath: String = "",
    val language: SourceLanguage? = null,
    val increasedLine: Int = 0,
    val decreasedLine: Int = 0,
    val emptyLineAdded: Int = 0,
)
