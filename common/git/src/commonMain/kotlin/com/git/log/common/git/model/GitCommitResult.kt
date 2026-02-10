package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// git 提交结果
@Serializable
data class GitCommitResult(
    val hash: String,
    val email: String,
    val date: String,
    val note: String,
    val diffFiles: List<GitFileDiff> = emptyList()
)