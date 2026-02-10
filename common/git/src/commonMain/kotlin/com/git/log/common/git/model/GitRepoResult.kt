package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// 仓库分析结果
@Serializable
data class GitRepoResult(
    val repoPath: String = "",
    val commits: List<GitCommitResult> = emptyList()
)
