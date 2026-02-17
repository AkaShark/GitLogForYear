package com.git.log.common.git.executor

// 包装命令行
expect class GitExecutor() {
    suspend fun log(repoPath: String): Result<String>
    suspend fun diff(repoPath: String, commitHash: String): Result<String>
    suspend fun clone(url: String, destPath: String): Result<Unit>
    suspend fun resetHard(repoPath: String): Result<Unit>
}