package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// 报告数据源
@Serializable
data class ReportDataSource(
    val repoResult: List<GitRepoResult>,
    val dictionaryIncrease: Map<String, Int> = emptyMap(),
    val dictionaryDecrease: Map<String, Int> = emptyMap(),
    val dictionaryCommit: Map<String, Int> = emptyMap()
)
