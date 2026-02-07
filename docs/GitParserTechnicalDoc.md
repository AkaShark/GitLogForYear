# Git 信息解析技术文档

> 版本: 1.0
> 更新日期: 2026-02-07

---

## 一、概述

本文档详细描述 MyYearWithGit 项目中 Git 信息解析模块的技术实现方案，包括 `git log` 和 `git diff` 的解析、数据模型设计、词频统计以及跨平台实现策略。

---

## 二、模块架构

### 2.1 整体设计

```
┌────────────────────────────────────────────────────────────────┐
│                      Git Parser Module                          │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐    │
│  │ GitExecutor  │   │  GitLogParser │   │  GitDiffParser   │    │
│  │  命令执行器   │ → │  日志解析器   │ → │   差异解析器     │    │
│  └──────────────┘   └──────────────┘   └──────────────────┘    │
│          │                  │                    │               │
│          ▼                  ▼                    ▼               │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐    │
│  │    Result    │   │ GitLogElement │   │  GitFileDiff     │    │
│  │   <String>   │   │   数据模型    │   │    数据模型      │    │
│  └──────────────┘   └──────────────┘   └──────────────────┘    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    RepoAnalyser                           │  │
│  │                      仓库分析器                            │  │
│  │  ┌────────────────┐ ┌─────────────────┐ ┌─────────────┐  │  │
│  │  │DictionaryBuilder│ │SourceLanguage   │ │CommitFilter │  │  │
│  │  │   词频统计器    │ │  语言识别器     │ │ 提交过滤器  │  │  │
│  │  └────────────────┘ └─────────────────┘ └─────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│                    ┌──────────────────┐                         │
│                    │  GitRepoResult   │                         │
│                    │    仓库分析结果   │                         │
│                    └──────────────────┘                         │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责

| 模块 | 职责 | 输入 | 输出 |
|------|------|------|------|
| GitExecutor | 执行 Git 命令 | 仓库路径、命令参数 | 命令输出字符串 |
| GitLogParser | 解析 git log 输出 | log 字符串 | `List<GitLogElement>` |
| GitDiffParser | 解析 git diff 输出 | diff 字符串 | `List<GitFileDiff>` |
| SourceLanguage | 识别文件编程语言 | 文件扩展名 | `SourceLanguage?` |
| DictionaryBuilder | 统计代码词频 | 代码文本 | `Map<String, Int>` |
| CommitFilter | 过滤指定用户提交 | 提交列表、邮箱列表 | 过滤后的提交列表 |
| RepoAnalyser | 协调各模块完成分析 | 仓库路径、配置 | `GitRepoResult` |

---

## 三、数据模型定义

### 3.1 核心数据模型

```kotlin
// 文件路径: core/model/src/commonMain/kotlin/core/model/git/

/**
 * Git 日志元素 - 单次提交的基本信息
 */
@Serializable
data class GitLogElement(
    val hash: String,           // 提交哈希 (40位小写)
    val authorEmail: String,    // 作者邮箱 (小写)
    val date: String,           // 提交日期原始字符串
    val note: String            // 提交信息
) {
    /**
     * 解析日期为 LocalDateTime
     * 支持格式: "Sun Apr 19 01:20:44 2020 +0800"
     */
    fun parseDateTime(): LocalDateTime? {
        return try {
            // 解析逻辑
            parseDateTimeFromGitFormat(date)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 文件差异模式
 */
enum class DiffMode {
    ADD,      // 新增文件
    MODIFY,   // 修改文件
    DELETE    // 删除文件
}

/**
 * 单个文件的差异信息
 */
@Serializable
data class GitFileDiff(
    val filePath: String,            // 文件路径
    val language: SourceLanguage?,   // 编程语言
    val mode: DiffMode,              // 差异模式
    val increasedLine: Int,          // 新增行数
    val decreasedLine: Int,          // 删除行数
    val emptyLineAdded: Int          // 新增空行数
)

/**
 * 单次提交的完整结果
 */
@Serializable
data class GitCommitResult(
    val hash: String,                    // 提交哈希
    val email: String,                   // 提交者邮箱
    val date: LocalDateTime,             // 提交时间
    val message: String,                 // 提交信息
    val diffFiles: List<GitFileDiff>     // 文件差异列表
) {
    /**
     * 计算本次提交的总新增行数
     */
    val totalIncreasedLines: Int
        get() = diffFiles.sumOf { it.increasedLine }
    
    /**
     * 计算本次提交的总删除行数
     */
    val totalDecreasedLines: Int
        get() = diffFiles.sumOf { it.decreasedLine }
    
    /**
     * 获取提交时间段
     */
    val timeOfDay: CommitTimeOfDay
        get() = CommitTimeOfDay.fromHour(date.hour)
}

/**
 * 仓库分析结果
 */
@Serializable
data class GitRepoResult(
    val repoName: String,                    // 仓库名称
    val repoPath: String,                    // 仓库路径
    val commits: List<GitCommitResult>,      // 提交列表
    val analyzedAt: LocalDateTime            // 分析时间
) {
    /**
     * 按日期分组的提交
     */
    val commitsByDate: Map<LocalDate, List<GitCommitResult>>
        get() = commits.groupBy { it.date.date }
    
    /**
     * 按语言分组的代码统计
     */
    val linesByLanguage: Map<SourceLanguage, Int>
        get() = commits
            .flatMap { it.diffFiles }
            .filter { it.language != null }
            .groupBy { it.language!! }
            .mapValues { (_, diffs) -> 
                diffs.sumOf { it.increasedLine - it.decreasedLine }
            }
}
```

### 3.2 提交时间段枚举

```kotlin
/**
 * 提交时间段定义
 */
enum class CommitTimeOfDay(
    val displayName: String,
    val englishName: String,
    val hourRange: IntRange
) {
    MIDNIGHT("凌晨", "Night Owl", 0..4),
    MORNING("早晨", "Early Bird", 5..9),
    NOON("中午", "Lunch Time", 10..13),
    AFTERNOON("下午", "Afternoon Tea", 14..16),
    DINNER("晚餐", "Dinner Time", 17..18),
    NIGHT("晚上", "Night", 19..23);
    
    companion object {
        fun fromHour(hour: Int): CommitTimeOfDay {
            return entries.find { hour in it.hourRange } ?: NIGHT
        }
    }
}
```

### 3.3 编程语言枚举

```kotlin
/**
 * 支持的编程语言
 */
enum class SourceLanguage(
    val displayName: String,
    val extensions: Set<String>,
    val color: Long  // 用于可视化展示的颜色
) {
    C("C", setOf("c", "h"), 0xFF555555),
    CSHARP("C#", setOf("cs", "csharp"), 0xFF178600),
    CPP("C++", setOf("cpp", "cc", "cxx", "c++", "hpp", "hxx"), 0xFFF34B7D),
    CSS("CSS", setOf("css", "scss", "sass", "less"), 0xFF563D7C),
    DART("Dart", setOf("dart"), 0xFF00B4AB),
    ELIXIR("Elixir", setOf("ex", "exs"), 0xFF6E4A7E),
    GO("Go", setOf("go"), 0xFF00ADD8),
    GROOVY("Groovy", setOf("groovy", "gvy", "gy", "gsh"), 0xFFE69F56),
    HTML("HTML", setOf("html", "htm", "xhtml"), 0xFFE34C26),
    JAVA("Java", setOf("java", "jav", "j"), 0xFFB07219),
    JAVASCRIPT("JavaScript", setOf("js", "jsx", "mjs"), 0xFFF1E05A),
    JSON("JSON", setOf("json"), 0xFF292929),
    KOTLIN("Kotlin", setOf("kt", "ktm", "kts"), 0xFFA97BFF),
    MARKDOWN("Markdown", setOf("md", "markdown"), 0xFF083FA1),
    OBJC("Objective-C", setOf("m", "mm"), 0xFF438EFF),
    PERL("Perl", setOf("pl", "pm"), 0xFF0298C3),
    PHP("PHP", setOf("php", "phtml"), 0xFF4F5D95),
    POWERSHELL("PowerShell", setOf("ps1", "psm1", "psd1"), 0xFF012456),
    PYTHON("Python", setOf("py", "pyw", "pyi"), 0xFF3572A5),
    RUBY("Ruby", setOf("rb", "rake", "gemspec"), 0xFF701516),
    RUST("Rust", setOf("rs"), 0xFFDEA584),
    SCALA("Scala", setOf("scala", "sc"), 0xFFC22D40),
    SHELL("Shell", setOf("sh", "bash", "zsh", "command"), 0xFF89E051),
    SQL("SQL", setOf("sql"), 0xFFE38C00),
    SWIFT("Swift", setOf("swift"), 0xFFF05138),
    TYPESCRIPT("TypeScript", setOf("ts", "tsx"), 0xFF3178C6),
    VUE("Vue", setOf("vue"), 0xFF41B883),
    XML("XML", setOf("xml", "plist", "xsl"), 0xFF0060AC),
    YAML("YAML", setOf("yaml", "yml"), 0xFFCB171E);
    
    companion object {
        private val extensionMap: Map<String, SourceLanguage> by lazy {
            entries.flatMap { lang -> 
                lang.extensions.map { ext -> ext to lang }
            }.toMap()
        }
        
        /**
         * 根据文件扩展名识别编程语言
         */
        fun fromExtension(ext: String): SourceLanguage? {
            return extensionMap[ext.lowercase().removePrefix(".")]
        }
        
        /**
         * 根据文件路径识别编程语言
         */
        fun fromFilePath(path: String): SourceLanguage? {
            val ext = path.substringAfterLast('.', "")
            return if (ext.isNotEmpty()) fromExtension(ext) else null
        }
    }
}
```

---

## 四、Git Log 解析器

### 4.1 命令与输出格式

**执行命令**:
```bash
git log --all --format=fuller
```

**输出示例**:
```
commit abc123def456789abcdef0123456789abcdef01234
Author:     John Doe <john@example.com>
AuthorDate: Sun Apr 19 01:20:44 2020 +0800
Commit:     John Doe <john@example.com>
CommitDate: Sun Apr 19 01:20:44 2020 +0800

    Initial commit
    
    This is the commit message body.

commit def456789abcdef0123456789abcdef0123456789
Author:     Jane Smith <jane@example.com>
AuthorDate: Mon Apr 20 14:35:22 2020 +0800
Commit:     Jane Smith <jane@example.com>
CommitDate: Mon Apr 20 14:35:22 2020 +0800

    Add feature X
```

### 4.2 解析器实现

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/parser/GitLogParser.kt

package core.git.parser

/**
 * Git 日志解析器
 */
class GitLogParser {
    
    companion object {
        private const val COMMIT_PREFIX = "commit "
        private const val AUTHOR_PREFIX = "Author:"
        private const val AUTHOR_DATE_PREFIX = "AuthorDate:"
        private const val DATE_PREFIX = "Date:"
    }
    
    /**
     * 解析 git log 输出
     * 
     * @param output git log 命令的完整输出
     * @return 解析后的提交列表
     */
    fun parse(output: String): List<GitLogElement> {
        val results = mutableListOf<GitLogElement>()
        
        var currentHash: String? = null
        var authorEmail: String? = null
        var date: String? = null
        val messageBuffer = StringBuilder()
        
        val lines = output.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                line.startsWith(COMMIT_PREFIX) -> {
                    // 保存上一个提交
                    submitCommit(
                        currentHash, authorEmail, date, 
                        messageBuffer.toString().trim(),
                        results
                    )
                    
                    // 开始新提交
                    currentHash = line.removePrefix(COMMIT_PREFIX)
                        .trim()
                        .lowercase()
                        .take(40)  // 只取前40位
                    authorEmail = null
                    date = null
                    messageBuffer.clear()
                }
                
                line.startsWith(AUTHOR_PREFIX) -> {
                    // 解析 "Author: Name <email@example.com>"
                    authorEmail = extractEmail(line)
                }
                
                line.startsWith(AUTHOR_DATE_PREFIX) -> {
                    // 解析 "AuthorDate: Sun Apr 19 01:20:44 2020 +0800"
                    date = line.removePrefix(AUTHOR_DATE_PREFIX).trim()
                }
                
                line.startsWith(DATE_PREFIX) && date == null -> {
                    // 备用日期格式 "Date:   Sun Apr 19 01:20:44 2020 +0800"
                    date = line.removePrefix(DATE_PREFIX).trim()
                }
                
                line.startsWith("    ") || line.isBlank() -> {
                    // 提交信息行 (以4个空格开头)
                    if (currentHash != null && authorEmail != null) {
                        messageBuffer.appendLine(line.removePrefix("    "))
                    }
                }
            }
            
            i++
        }
        
        // 处理最后一个提交
        submitCommit(currentHash, authorEmail, date, messageBuffer.toString().trim(), results)
        
        return results
    }
    
    /**
     * 从 Author 行提取邮箱
     */
    private fun extractEmail(line: String): String? {
        val start = line.indexOf('<')
        val end = line.indexOf('>')
        return if (start != -1 && end != -1 && end > start) {
            line.substring(start + 1, end).lowercase()
        } else {
            null
        }
    }
    
    /**
     * 提交解析结果
     */
    private fun submitCommit(
        hash: String?,
        email: String?,
        date: String?,
        message: String,
        results: MutableList<GitLogElement>
    ) {
        if (hash != null && email != null && date != null) {
            results.add(
                GitLogElement(
                    hash = hash,
                    authorEmail = email,
                    date = date,
                    note = message
                )
            )
        }
    }
}
```

### 4.3 日期解析工具

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/util/GitDateParser.kt

package core.git.util

import kotlinx.datetime.*

/**
 * Git 日期格式解析器
 */
object GitDateParser {
    
    // 月份映射
    private val monthMap = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )
    
    /**
     * 解析 Git 日期格式
     * 
     * 支持格式:
     * - "Sun Apr 19 01:20:44 2020 +0800"
     * - "2020-04-19 01:20:44 +0800"
     * 
     * @param dateStr Git 格式的日期字符串
     * @return LocalDateTime 或 null
     */
    fun parse(dateStr: String): LocalDateTime? {
        return try {
            parseStandardGitFormat(dateStr) 
                ?: parseIsoFormat(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析标准 Git 格式: "Sun Apr 19 01:20:44 2020 +0800"
     */
    private fun parseStandardGitFormat(dateStr: String): LocalDateTime? {
        val parts = dateStr.trim().split(Regex("\\s+"))
        if (parts.size < 5) return null
        
        // 格式: Day Month Date Time Year [Timezone]
        // 例如: Sun Apr 19 01:20:44 2020 +0800
        
        val monthStr = parts[1].lowercase()
        val month = monthMap[monthStr] ?: return null
        
        val dayOfMonth = parts[2].toIntOrNull() ?: return null
        val timeParts = parts[3].split(":")
        if (timeParts.size != 3) return null
        
        val hour = timeParts[0].toIntOrNull() ?: return null
        val minute = timeParts[1].toIntOrNull() ?: return null
        val second = timeParts[2].toIntOrNull() ?: return null
        
        val year = parts[4].toIntOrNull() ?: return null
        
        return LocalDateTime(
            year = year,
            monthNumber = month,
            dayOfMonth = dayOfMonth,
            hour = hour,
            minute = minute,
            second = second
        )
    }
    
    /**
     * 解析 ISO 格式: "2020-04-19 01:20:44 +0800"
     */
    private fun parseIsoFormat(dateStr: String): LocalDateTime? {
        val cleaned = dateStr.trim()
            .replace(Regex("\\s*[+-]\\d{4}$"), "")  // 移除时区
            .replace(" ", "T")
        
        return try {
            LocalDateTime.parse(cleaned)
        } catch (e: Exception) {
            null
        }
    }
}
```

---

## 五、Git Diff 解析器

### 5.1 命令与输出格式

**执行命令**:
```bash
git diff <commit_hash>^!
```

`^!` 表示该提交与其父提交的差异。

**输出示例**:
```diff
diff --git a/src/main.kt b/src/main.kt
new file mode 100644
index 0000000..abc1234
--- /dev/null
+++ b/src/main.kt
@@ -0,0 +1,15 @@
+package com.example
+
+fun main() {
+    println("Hello, World!")
+}

diff --git a/README.md b/README.md
index abc1234..def5678 100644
--- a/README.md
+++ b/README.md
@@ -1,3 +1,5 @@
 # Project Name
 
+## Description
+
 This is a project.
```

### 5.2 解析器实现

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/parser/GitDiffParser.kt

package core.git.parser

/**
 * Git Diff 解析器
 */
class GitDiffParser {
    
    companion object {
        private const val DIFF_HEADER = "diff --git"
        private const val NEW_FILE_MODE = "new file mode"
        private const val DELETED_FILE_MODE = "deleted file mode"
        private const val INDEX_LINE = "index "
        private const val ADD_LINE_PREFIX = "+"
        private const val DELETE_LINE_PREFIX = "-"
        private const val HUNK_HEADER = "@@"
    }
    
    /**
     * 解析 git diff 输出
     * 
     * @param output git diff 命令的完整输出
     * @return 解析后的文件差异列表
     */
    fun parse(output: String): List<GitFileDiff> {
        val results = mutableListOf<GitFileDiff>()
        
        // 按 diff 头部分割
        val diffBlocks = splitByDiffHeader(output)
        
        for (block in diffBlocks) {
            parseDiffBlock(block)?.let { results.add(it) }
        }
        
        return results
    }
    
    /**
     * 按 diff 头部分割输出
     */
    private fun splitByDiffHeader(output: String): List<String> {
        val blocks = mutableListOf<String>()
        val lines = output.lines()
        
        var currentBlock = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith(DIFF_HEADER)) {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(currentBlock.toString())
                }
                currentBlock = StringBuilder()
            }
            currentBlock.appendLine(line)
        }
        
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock.toString())
        }
        
        return blocks
    }
    
    /**
     * 解析单个 diff 块
     */
    private fun parseDiffBlock(block: String): GitFileDiff? {
        val lines = block.lines()
        if (lines.isEmpty()) return null
        
        // 解析文件路径
        val filePath = extractFilePath(lines.first()) ?: return null
        
        // 判断差异模式
        val mode = detectDiffMode(lines)
        
        // 统计行数变化
        val (increased, decreased, emptyAdded) = countLineChanges(lines)
        
        // 识别编程语言
        val language = SourceLanguage.fromFilePath(filePath)
        
        return GitFileDiff(
            filePath = filePath,
            language = language,
            mode = mode,
            increasedLine = increased,
            decreasedLine = decreased,
            emptyLineAdded = emptyAdded
        )
    }
    
    /**
     * 从 diff 头部提取文件路径
     * 输入: "diff --git a/path/to/file.kt b/path/to/file.kt"
     * 输出: "path/to/file.kt"
     */
    private fun extractFilePath(headerLine: String): String? {
        val match = Regex("diff --git a/(.*) b/(.*)").find(headerLine)
        return match?.groupValues?.getOrNull(2)
            ?: match?.groupValues?.getOrNull(1)
    }
    
    /**
     * 检测差异模式
     */
    private fun detectDiffMode(lines: List<String>): DiffMode {
        for (line in lines.take(10)) {  // 只检查前10行
            when {
                line.startsWith(NEW_FILE_MODE) -> return DiffMode.ADD
                line.startsWith(DELETED_FILE_MODE) -> return DiffMode.DELETE
            }
        }
        return DiffMode.MODIFY
    }
    
    /**
     * 统计行数变化
     * @return Triple(新增行数, 删除行数, 新增空行数)
     */
    private fun countLineChanges(lines: List<String>): Triple<Int, Int, Int> {
        var increased = 0
        var decreased = 0
        var emptyAdded = 0
        var inHunk = false
        
        for (line in lines) {
            if (line.startsWith(HUNK_HEADER)) {
                inHunk = true
                continue
            }
            
            if (!inHunk) continue
            
            when {
                line.startsWith(ADD_LINE_PREFIX) && !line.startsWith("+++") -> {
                    increased++
                    val content = line.removePrefix(ADD_LINE_PREFIX)
                    if (content.isBlank()) {
                        emptyAdded++
                    }
                }
                line.startsWith(DELETE_LINE_PREFIX) && !line.startsWith("---") -> {
                    decreased++
                }
            }
        }
        
        return Triple(increased, decreased, emptyAdded)
    }
}
```

### 5.3 Diff 内容提取器 (用于词频统计)

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/parser/DiffContentExtractor.kt

package core.git.parser

/**
 * Diff 内容提取器
 * 从 git diff 输出中提取新增和删除的代码内容
 */
class DiffContentExtractor {
    
    /**
     * 提取新增的代码行内容
     */
    fun extractAddedContent(diffOutput: String): String {
        return extractContent(diffOutput, '+')
    }
    
    /**
     * 提取删除的代码行内容
     */
    fun extractDeletedContent(diffOutput: String): String {
        return extractContent(diffOutput, '-')
    }
    
    private fun extractContent(diffOutput: String, prefix: Char): String {
        val builder = StringBuilder()
        var inHunk = false
        
        for (line in diffOutput.lines()) {
            if (line.startsWith("@@")) {
                inHunk = true
                continue
            }
            
            if (!inHunk) continue
            
            if (line.startsWith(prefix.toString()) && 
                !line.startsWith("$prefix$prefix$prefix")) {
                builder.appendLine(line.drop(1))
            }
        }
        
        return builder.toString()
    }
}
```

---

## 六、词频统计器

### 6.1 核心实现

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/analyzer/DictionaryBuilder.kt

package core.git.analyzer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 词频统计器
 * 线程安全，支持内存优化
 */
class DictionaryBuilder(
    private val maxSize: Int = 65535,
    private val minWordLength: Int = 3
) {
    private val dictionary = mutableMapOf<String, Int>()
    private val lock = Mutex()
    
    /**
     * 喂入文本进行词频统计
     */
    suspend fun feed(text: String) {
        lock.withLock {
            tokenize(text)
                .filter { isValidWord(it) }
                .forEach { word ->
                    val key = word.lowercase()
                    dictionary[key] = (dictionary[key] ?: 0) + 1
                }
            
            trimIfNeeded()
        }
    }
    
    /**
     * 获取词频结果
     * @param topN 返回前N个高频词，null表示全部返回
     */
    suspend fun getResult(topN: Int? = null): Map<String, Int> {
        return lock.withLock {
            val sorted = dictionary.entries
                .sortedByDescending { it.value }
            
            if (topN != null) {
                sorted.take(topN)
            } else {
                sorted
            }.associate { it.key to it.value }
        }
    }
    
    /**
     * 重置统计
     */
    suspend fun reset() {
        lock.withLock {
            dictionary.clear()
        }
    }
    
    /**
     * 分词
     * 支持驼峰命名拆分和符号分隔
     */
    private fun tokenize(text: String): List<String> {
        return text
            // 按非字母数字分隔
            .split(Regex("[^a-zA-Z0-9]+"))
            // 驼峰拆分
            .flatMap { it.splitByCamelCase() }
            .filter { it.isNotBlank() }
    }
    
    /**
     * 验证是否为有效单词
     */
    private fun isValidWord(word: String): Boolean {
        // 长度检查
        if (word.length < minWordLength) return false
        
        // 纯数字排除
        if (word.toDoubleOrNull() != null) return false
        
        // 常见无意义词排除
        if (word.lowercase() in STOP_WORDS) return false
        
        // 只包含相同字符的排除 (如 "aaa", "111")
        if (word.all { it == word[0] }) return false
        
        return true
    }
    
    /**
     * 内存优化：超过上限时裁剪低频词
     */
    private fun trimIfNeeded() {
        if (dictionary.size <= maxSize) return
        
        var threshold = 1
        while (dictionary.size > maxSize) {
            dictionary.entries.removeAll { it.value <= threshold }
            threshold++
        }
    }
    
    companion object {
        /**
         * 停用词列表
         */
        private val STOP_WORDS = setOf(
            "the", "and", "for", "with", "this", "that", "from",
            "are", "was", "were", "been", "being", "have", "has",
            "had", "does", "did", "will", "would", "could", "should",
            "may", "might", "must", "shall", "can", "need", "not",
            "get", "set", "var", "val", "let", "const", "function",
            "return", "null", "undefined", "true", "false", "void",
            "public", "private", "protected", "static", "final",
            "class", "interface", "enum", "struct", "import", "export"
        )
    }
}

/**
 * 驼峰命名拆分扩展函数
 */
fun String.splitByCamelCase(): List<String> {
    return this
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
        .split(" ")
        .filter { it.isNotBlank() }
}
```

### 6.2 脏话检测器 (用于成就系统)

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/analyzer/ProfanityDetector.kt

package core.git.analyzer

/**
 * 脏话检测器
 * 用于"文明语言大师"等成就判断
 */
object ProfanityDetector {
    
    // 常见脏话词汇 (已脱敏)
    private val PROFANITY_WORDS = setOf(
        "fuck", "shit", "damn", "crap", "hell",
        "wtf", "omfg", "ass", "bastard", "idiot"
    )
    
    /**
     * 检测文本中是否包含脏话
     */
    fun containsProfanity(text: String): Boolean {
        val words = text.lowercase().split(Regex("[^a-z]+"))
        return words.any { it in PROFANITY_WORDS }
    }
    
    /**
     * 统计脏话出现次数
     */
    fun countProfanity(text: String): Int {
        val words = text.lowercase().split(Regex("[^a-z]+"))
        return words.count { it in PROFANITY_WORDS }
    }
    
    /**
     * 检测某个词是否为脏话
     */
    fun isProfanity(word: String): Boolean {
        return word.lowercase() in PROFANITY_WORDS
    }
}
```

---

## 七、Git 命令执行器 (跨平台)

### 7.1 接口定义

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/executor/GitExecutor.kt

package core.git.executor

/**
 * Git 命令执行器接口
 */
expect class GitExecutor() {
    /**
     * 执行 git log 命令
     */
    suspend fun log(repoPath: String): Result<String>
    
    /**
     * 执行 git diff 命令
     */
    suspend fun diff(repoPath: String, commitHash: String): Result<String>
    
    /**
     * 克隆仓库
     */
    suspend fun clone(url: String, destPath: String, onProgress: ((Float) -> Unit)?): Result<Unit>
    
    /**
     * 检查是否为有效的 Git 仓库
     */
    suspend fun isValidRepo(path: String): Boolean
    
    /**
     * 获取仓库名称
     */
    suspend fun getRepoName(repoPath: String): String?
}
```

### 7.2 Desktop 实现

```kotlin
// 文件路径: core/git/src/desktopMain/kotlin/core/git/executor/GitExecutor.desktop.kt

package core.git.executor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class GitExecutor {
    
    actual suspend fun log(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                "git", "log", "--all", "--format=fuller"
            )
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .start()
            
            process.inputStream.bufferedReader().readText().also {
                process.waitFor()
            }
        }
    }
    
    actual suspend fun diff(repoPath: String, commitHash: String): Result<String> = 
        withContext(Dispatchers.IO) {
            runCatching {
                val process = ProcessBuilder(
                    "git", "diff", "$commitHash^!"
                )
                    .directory(File(repoPath))
                    .redirectErrorStream(true)
                    .start()
                
                process.inputStream.bufferedReader().readText().also {
                    process.waitFor()
                }
            }
        }
    
    actual suspend fun clone(
        url: String, 
        destPath: String, 
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                "git", "clone", "--progress", url, destPath
            )
                .redirectErrorStream(true)
                .start()
            
            // 读取进度信息
            process.inputStream.bufferedReader().forEachLine { line ->
                parseCloneProgress(line)?.let { progress ->
                    onProgress?.invoke(progress)
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("Git clone failed with exit code: $exitCode")
            }
        }
    }
    
    actual suspend fun isValidRepo(path: String): Boolean = withContext(Dispatchers.IO) {
        val gitDir = File(path, ".git")
        gitDir.exists() && gitDir.isDirectory
    }
    
    actual suspend fun getRepoName(repoPath: String): String? = withContext(Dispatchers.IO) {
        File(repoPath).name
    }
    
    /**
     * 解析克隆进度
     */
    private fun parseCloneProgress(line: String): Float? {
        // 格式: "Receiving objects:  42% (100/238)"
        val match = Regex("(\\d+)%").find(line)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull()?.div(100f)
    }
}
```

### 7.3 Android 实现 (使用 JGit)

```kotlin
// 文件路径: core/git/src/androidMain/kotlin/core/git/executor/GitExecutor.android.kt

package core.git.executor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

actual class GitExecutor {
    
    actual suspend fun log(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val repo = FileRepositoryBuilder()
                .setGitDir(File(repoPath, ".git"))
                .build()
            
            Git(repo).use { git ->
                val logs = git.log().all().call()
                formatLogOutput(logs)
            }
        }
    }
    
    actual suspend fun diff(repoPath: String, commitHash: String): Result<String> = 
        withContext(Dispatchers.IO) {
            runCatching {
                val repo = FileRepositoryBuilder()
                    .setGitDir(File(repoPath, ".git"))
                    .build()
                
                Git(repo).use { git ->
                    val commit = repo.parseCommit(repo.resolve(commitHash))
                    val parent = commit.parentCount.takeIf { it > 0 }
                        ?.let { commit.getParent(0) }
                    
                    val diffOutput = StringBuilder()
                    val diffFormatter = org.eclipse.jgit.diff.DiffFormatter(
                        org.eclipse.jgit.util.io.DisabledOutputStream.INSTANCE
                    )
                    diffFormatter.setRepository(repo)
                    
                    val diffs = diffFormatter.scan(
                        parent?.tree,
                        commit.tree
                    )
                    
                    for (diff in diffs) {
                        diffOutput.appendLine(formatDiffEntry(diff, repo, parent, commit))
                    }
                    
                    diffOutput.toString()
                }
            }
        }
    
    actual suspend fun clone(
        url: String,
        destPath: String,
        onProgress: ((Float) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(File(destPath))
                .setProgressMonitor(object : ProgressMonitor {
                    private var totalWork = 0
                    private var completed = 0
                    
                    override fun start(totalTasks: Int) {}
                    
                    override fun beginTask(title: String?, totalWork: Int) {
                        this.totalWork = totalWork
                        this.completed = 0
                    }
                    
                    override fun update(completed: Int) {
                        this.completed += completed
                        if (totalWork > 0) {
                            onProgress?.invoke(this.completed.toFloat() / totalWork)
                        }
                    }
                    
                    override fun endTask() {}
                    
                    override fun isCancelled() = false
                    
                    override fun showDuration(enabled: Boolean) {}
                })
                .call()
                .close()
        }
    }
    
    actual suspend fun isValidRepo(path: String): Boolean = withContext(Dispatchers.IO) {
        val gitDir = File(path, ".git")
        gitDir.exists() && gitDir.isDirectory
    }
    
    actual suspend fun getRepoName(repoPath: String): String? = withContext(Dispatchers.IO) {
        File(repoPath).name
    }
    
    /**
     * 格式化日志输出为标准 git log 格式
     */
    private fun formatLogOutput(logs: Iterable<RevCommit>): String {
        val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.US)
        val builder = StringBuilder()
        
        for (commit in logs) {
            builder.appendLine("commit ${commit.name}")
            builder.appendLine("Author: ${commit.authorIdent.name} <${commit.authorIdent.emailAddress}>")
            builder.appendLine("Date:   ${dateFormat.format(commit.authorIdent.`when`)}")
            builder.appendLine()
            commit.fullMessage.lines().forEach { line ->
                builder.appendLine("    $line")
            }
            builder.appendLine()
        }
        
        return builder.toString()
    }
}
```

---

## 八、仓库分析器

### 8.1 核心实现

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/analyzer/RepoAnalyser.kt

package core.git.analyzer

import core.git.executor.GitExecutor
import core.git.parser.DiffContentExtractor
import core.git.parser.GitDiffParser
import core.git.parser.GitLogParser
import core.git.util.GitDateParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 仓库分析器
 */
class RepoAnalyser(
    private val gitExecutor: GitExecutor = GitExecutor(),
    private val logParser: GitLogParser = GitLogParser(),
    private val diffParser: GitDiffParser = GitDiffParser(),
    private val contentExtractor: DiffContentExtractor = DiffContentExtractor()
) {
    /**
     * 分析进度状态
     */
    sealed class AnalysisState {
        data class Progress(
            val current: Int,
            val total: Int,
            val message: String
        ) : AnalysisState()
        
        data class Success(val result: AnalysisResult) : AnalysisState()
        data class Error(val error: Throwable) : AnalysisState()
    }
    
    /**
     * 分析结果
     */
    data class AnalysisResult(
        val repoResult: GitRepoResult,
        val dictionaryIncrease: Map<String, Int>,
        val dictionaryDecrease: Map<String, Int>,
        val dictionaryCommit: Map<String, Int>
    )
    
    /**
     * 分析仓库
     * 
     * @param repoPath 仓库路径
     * @param filterEmails 过滤的邮箱列表，为空则不过滤
     * @param year 分析的年份，为null则分析全部
     */
    fun analyze(
        repoPath: String,
        filterEmails: Set<String> = emptySet(),
        year: Int? = null
    ): Flow<AnalysisState> = flow {
        try {
            // 1. 获取 git log
            emit(AnalysisState.Progress(0, 100, "正在读取提交记录..."))
            
            val logOutput = gitExecutor.log(repoPath).getOrThrow()
            val logElements = logParser.parse(logOutput)
            
            // 2. 过滤提交
            val filteredLogs = logElements
                .let { logs ->
                    if (filterEmails.isNotEmpty()) {
                        logs.filter { it.authorEmail in filterEmails }
                    } else logs
                }
                .let { logs ->
                    if (year != null) {
                        logs.filter { 
                            GitDateParser.parse(it.date)?.year == year 
                        }
                    } else logs
                }
            
            if (filteredLogs.isEmpty()) {
                throw IllegalStateException("没有找到符合条件的提交记录")
            }
            
            emit(AnalysisState.Progress(10, 100, "找到 ${filteredLogs.size} 条提交记录"))
            
            // 3. 初始化词频统计器
            val dictIncrease = DictionaryBuilder()
            val dictDecrease = DictionaryBuilder()
            val dictCommit = DictionaryBuilder()
            
            // 4. 逐个分析提交
            val commits = mutableListOf<GitCommitResult>()
            
            filteredLogs.forEachIndexed { index, logElement ->
                val progress = 10 + (index * 80 / filteredLogs.size)
                emit(AnalysisState.Progress(
                    progress, 100, 
                    "正在分析提交 ${index + 1}/${filteredLogs.size}"
                ))
                
                // 获取 diff
                val diffOutput = gitExecutor.diff(repoPath, logElement.hash)
                    .getOrNull() ?: ""
                
                // 解析 diff
                val diffFiles = diffParser.parse(diffOutput)
                
                // 词频统计
                val addedContent = contentExtractor.extractAddedContent(diffOutput)
                val deletedContent = contentExtractor.extractDeletedContent(diffOutput)
                
                dictIncrease.feed(addedContent)
                dictDecrease.feed(deletedContent)
                dictCommit.feed(logElement.note)
                
                // 构建提交结果
                val dateTime = GitDateParser.parse(logElement.date)
                if (dateTime != null) {
                    commits.add(
                        GitCommitResult(
                            hash = logElement.hash,
                            email = logElement.authorEmail,
                            date = dateTime,
                            message = logElement.note,
                            diffFiles = diffFiles
                        )
                    )
                }
            }
            
            emit(AnalysisState.Progress(90, 100, "正在生成分析报告..."))
            
            // 5. 构建最终结果
            val repoName = gitExecutor.getRepoName(repoPath) ?: "Unknown"
            val result = AnalysisResult(
                repoResult = GitRepoResult(
                    repoName = repoName,
                    repoPath = repoPath,
                    commits = commits,
                    analyzedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                ),
                dictionaryIncrease = dictIncrease.getResult(topN = 100),
                dictionaryDecrease = dictDecrease.getResult(topN = 100),
                dictionaryCommit = dictCommit.getResult(topN = 100)
            )
            
            emit(AnalysisState.Progress(100, 100, "分析完成"))
            emit(AnalysisState.Success(result))
            
        } catch (e: Exception) {
            emit(AnalysisState.Error(e))
        }
    }
}
```

---

## 九、提交过滤器

```kotlin
// 文件路径: core/git/src/commonMain/kotlin/core/git/filter/CommitFilter.kt

package core.git.filter

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * 提交过滤器
 */
class CommitFilter {
    
    /**
     * 按邮箱过滤
     */
    fun <T> filterByEmails(
        items: List<T>,
        emails: Set<String>,
        emailSelector: (T) -> String
    ): List<T> {
        if (emails.isEmpty()) return items
        return items.filter { emailSelector(it).lowercase() in emails.map { e -> e.lowercase() } }
    }
    
    /**
     * 按年份过滤
     */
    fun <T> filterByYear(
        items: List<T>,
        year: Int,
        dateSelector: (T) -> LocalDateTime?
    ): List<T> {
        return items.filter { dateSelector(it)?.year == year }
    }
    
    /**
     * 按日期范围过滤
     */
    fun <T> filterByDateRange(
        items: List<T>,
        startDate: LocalDate,
        endDate: LocalDate,
        dateSelector: (T) -> LocalDateTime?
    ): List<T> {
        return items.filter { item ->
            val date = dateSelector(item)?.date ?: return@filter false
            date >= startDate && date <= endDate
        }
    }
    
    /**
     * 排除合并提交
     */
    fun <T> excludeMergeCommits(
        items: List<T>,
        messageSelector: (T) -> String
    ): List<T> {
        return items.filter { item ->
            val message = messageSelector(item).lowercase()
            !message.startsWith("merge") && 
            !message.contains("merge branch") &&
            !message.contains("merge pull request")
        }
    }
}
```

---

## 十、使用示例

### 10.1 基本使用

```kotlin
// 分析本地仓库
val analyser = RepoAnalyser()

analyser.analyze(
    repoPath = "/path/to/repo",
    filterEmails = setOf("john@example.com"),
    year = 2025
).collect { state ->
    when (state) {
        is RepoAnalyser.AnalysisState.Progress -> {
            println("进度: ${state.current}% - ${state.message}")
        }
        is RepoAnalyser.AnalysisState.Success -> {
            val result = state.result
            println("分析完成!")
            println("总提交数: ${result.repoResult.commits.size}")
            println("最常用语言: ${result.repoResult.linesByLanguage.maxByOrNull { it.value }?.key}")
            println("最高频词: ${result.dictionaryIncrease.maxByOrNull { it.value }?.key}")
        }
        is RepoAnalyser.AnalysisState.Error -> {
            println("分析失败: ${state.error.message}")
        }
    }
}
```

### 10.2 在 ViewModel 中使用

```kotlin
class AnalysisViewModel : ViewModel() {
    private val analyser = RepoAnalyser()
    
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun startAnalysis(repoPath: String, emails: Set<String>, year: Int) {
        viewModelScope.launch {
            analyser.analyze(repoPath, emails, year)
                .collect { analysisState ->
                    _state.value = when (analysisState) {
                        is RepoAnalyser.AnalysisState.Progress -> 
                            UiState.Loading(analysisState.current, analysisState.message)
                        is RepoAnalyser.AnalysisState.Success -> 
                            UiState.Success(analysisState.result)
                        is RepoAnalyser.AnalysisState.Error -> 
                            UiState.Error(analysisState.error.message ?: "Unknown error")
                    }
                }
        }
    }
    
    sealed class UiState {
        object Idle : UiState()
        data class Loading(val progress: Int, val message: String) : UiState()
        data class Success(val result: RepoAnalyser.AnalysisResult) : UiState()
        data class Error(val message: String) : UiState()
    }
}
```

---

## 十一、测试策略

### 11.1 单元测试

```kotlin
// 文件路径: core/git/src/commonTest/kotlin/core/git/parser/GitLogParserTest.kt

class GitLogParserTest {
    private val parser = GitLogParser()
    
    @Test
    fun `parse standard git log output`() {
        val output = """
            commit abc123def456789abcdef0123456789abcdef01234
            Author: John Doe <john@example.com>
            Date:   Sun Apr 19 01:20:44 2020 +0800

                Initial commit
        """.trimIndent()
        
        val result = parser.parse(output)
        
        assertEquals(1, result.size)
        assertEquals("abc123def456789abcdef0123456789abcdef01234", result[0].hash)
        assertEquals("john@example.com", result[0].authorEmail)
        assertEquals("Initial commit", result[0].note)
    }
    
    @Test
    fun `parse multiple commits`() {
        val output = """
            commit abc123
            Author: John <john@example.com>
            Date:   Sun Apr 19 01:20:44 2020 +0800

                First commit

            commit def456
            Author: Jane <jane@example.com>
            Date:   Mon Apr 20 14:35:22 2020 +0800

                Second commit
        """.trimIndent()
        
        val result = parser.parse(output)
        
        assertEquals(2, result.size)
    }
}
```

### 11.2 集成测试

```kotlin
class RepoAnalyserIntegrationTest {
    
    @Test
    fun `analyze real repository`() = runTest {
        val analyser = RepoAnalyser()
        var finalResult: RepoAnalyser.AnalysisResult? = null
        
        analyser.analyze(
            repoPath = getTestRepoPath(),
            year = 2025
        ).collect { state ->
            if (state is RepoAnalyser.AnalysisState.Success) {
                finalResult = state.result
            }
        }
        
        assertNotNull(finalResult)
        assertTrue(finalResult!!.repoResult.commits.isNotEmpty())
    }
}
```

---

## 附录 A: 模块依赖关系

```
core:model          (数据模型定义)
    ↑
core:git            (Git 解析与分析)
    ├── GitExecutor (expect/actual)
    ├── GitLogParser
    ├── GitDiffParser
    ├── DictionaryBuilder
    └── RepoAnalyser
    ↑
feature:analysis    (分析功能模块)
    └── AnalysisViewModel
```

---

## 附录 B: 依赖配置

```kotlin
// core/git/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        
        androidMain.dependencies {
            implementation(libs.jgit)
        }
    }
}
```

---

*文档版本: 1.0*  
*最后更新: 2026-02-07*
