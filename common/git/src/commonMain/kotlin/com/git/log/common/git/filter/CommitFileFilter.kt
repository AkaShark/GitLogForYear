package com.git.log.common.git.filter

import kotlinx.serialization.Serializable

@Serializable
enum class BlockType(val displayName: String) {
    NAME_KEYWORD("文件名关键词"),
    NAME_KEYWORD_CASE_SENSITIVE("文件名关键词 匹配大小写"),
    PATH_KEYWORD("路径关键词"),
    PATH_KEYWORD_CASE_SENSITIVE("路径关键词 匹配大小写"),
    PATH_COMPONENT_FULL_MATCH("路径中存在文件名"),
    PATH_COMPONENT_FULL_MATCH_CASE_SENSITIVE("路径中存在文件名 匹配大小写"),
    NAME_REGEX_FULL_MATCH("文件名正则表达式完整匹配")
}

@Serializable
data class BlockItem(
    val type: BlockType,
    val value: String
) {
    fun shouldBlock(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')
        return when (type) {
            BlockType.NAME_KEYWORD -> fileName.contains(value, ignoreCase = true)
            BlockType.NAME_KEYWORD_CASE_SENSITIVE -> fileName.contains(value)
            BlockType.PATH_KEYWORD -> filePath.contains(value, ignoreCase = true)
            BlockType.PATH_KEYWORD_CASE_SENSITIVE -> filePath.contains(value)
            BlockType.PATH_COMPONENT_FULL_MATCH -> filePath.split("/").any { it.equals(value, ignoreCase = true) }
            BlockType.PATH_COMPONENT_FULL_MATCH_CASE_SENSITIVE -> filePath.split("/").any { it == value }
            BlockType.NAME_REGEX_FULL_MATCH -> {
                try {
                    Regex(value).matches(fileName)
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}

class CommitFileFilter {
    companion object {
        val shared = CommitFileFilter()
    }

    private val _blockList = mutableListOf<BlockItem>()
    val blockList: List<BlockItem> get() = _blockList.toList()

    fun addBlock(item: BlockItem) {
        _blockList.add(item)
    }

    fun removeBlock(index: Int) {
        if (index in _blockList.indices) _blockList.removeAt(index)
    }

    fun clearAll() {
        _blockList.clear()
    }

    fun shouldBlock(filePath: String): Boolean {
        return _blockList.any { it.shouldBlock(filePath) }
    }
}