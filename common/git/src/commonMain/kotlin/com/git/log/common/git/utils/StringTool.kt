package com.git.log.common.git.utils

/*
 * 驼峰拆分
 * "handleUserClick" → ["handle", "User", "Click"]
 * "XMLParser" → ["XML", "Parser"]
 */
fun String.splitByCamelCase(): List<String> {
    return this.split(Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])"))
}

/*
* 判断字符串是否符合字典条目
* */
fun String.isElegantForDictionary(): Boolean {
    if (isEmpty()) return false
    if (length <= 1) return false
    return all { it.isLetter() }
}