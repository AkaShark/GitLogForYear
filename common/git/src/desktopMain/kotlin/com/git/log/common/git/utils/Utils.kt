package com.git.log.common.git.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val blockedDirectorNames = setOf<String>(
    "node_modules", ".build", "DerivedData", "Pods",
    ".gradle", "build", ".idea", ".vscode"
)

actual suspend fun searchGitRepositories(
    searchRoots: List<String>,
    maxDepth: Int,
    onCancel: () -> Boolean
): List<String> = withContext(Dispatchers.IO) {
    val results = mutableSetOf<String>()

    fun search(dir: File, depth: Int) {
        if (onCancel()) return
        if (depth > maxDepth) return
        if (!dir.isDirectory) return
        if (blockedDirectorNames.contains(dir.name)) return

        val gitDir = File(dir, ".git")
        if (gitDir.exists() && gitDir.isDirectory) {
            results.add(dir.absolutePath)
            return
        }

        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                search(child, depth + 1)
            }
        }
    }

    for (root in searchRoots) {
        search(File(root), 0)
    }

    results.sorted()
}

internal actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}