package com.git.log.common.git.executor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class GitExecutor actual constructor() {
    private fun findGitBinary(): String {
        val candidates = listOf("/usr/bin/git", "/usr/local/bin/git", "/opt/homebrew/bin/git", "git")
        for (candidate in candidates) {
            try {
                val process = ProcessBuilder(candidate, "--version").start()
                val exitCode = process.waitFor()
                if (exitCode == 0) return candidate
            } catch (_: Exception) {

            }
        }
        return "git"
    }

    private val gitBinary: String = findGitBinary()

    private suspend fun runGit(
        repoPath: String,
        vararg args: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(gitBinary, *args)
                .directory(File(repoPath))
                .redirectErrorStream(true) // 把 stderr 合并进 stdout
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw RuntimeException("git ${args.joinToString(" ")} failed (exit=$exitCode): $output")
            }

            output
        }
    }

    actual suspend fun log(repoPath: String): Result<String> {
        return runGit(repoPath, "log", "--all")
    }

    actual suspend fun diff(
        repoPath: String,
        commitHash: String
    ): Result<String> {
        return runGit(repoPath, "diff", "$commitHash^!")
    }

    actual suspend fun clone(
        url: String,
        destPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(gitBinary, "clone", "--depth", "1", url, destPath)
                .redirectErrorStream(true)
                .start()
            val resultCode = process.waitFor()
            if (resultCode != 0) {
                throw RuntimeException("git clone failed: ${process.inputStream.bufferedReader().readText()}")
            }
        }
    }

    actual suspend fun resetHard(repoPath: String): Result<Unit> {
        return runGit(repoPath, "reset", "--hard").map { Unit }
    }
}