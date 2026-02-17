package com.git.log.common.git.utils

expect suspend fun searchGitRepositories(
    searchRoots: List<String>,
    maxDepth: Int = 64,
    onCancel: () -> Boolean = { false }
): List<String>
