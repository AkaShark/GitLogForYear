package com.git.log.common.git.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// 词频统计器
class DictionaryBuilder private constructor() {
    companion object {
        val sharedIncrease = DictionaryBuilder()
        val sharedDecrease = DictionaryBuilder()
        val sharedCommit = DictionaryBuilder()
    }

    private var currentSession = ""
    private var dictionary = mutableMapOf<String, Int>()
    private val mutex = Mutex()
    private var trimCounter = 2048

    fun beginSession(): String {
        val session = generateSessionId()
        currentSession = session
        dictionary.clear()
        trimCounter = 2048
        return session
    }

    suspend fun feed(buffer: String, session: String) {
        mutex.withLock {
            if (session != currentSession) return@withLock

            buffer.splitByCamelCase()
                .filter { it.length > 3 }
                .filter { it.isElegantForDictionary() }
                .filter { it.toDoubleOrNull() == null }
                .forEach { word ->
                    val key = word.lowercase()
                    dictionary[key] = (dictionary[key] ?: 0) + 1
                }

            trimCounter --
            if (trimCounter <= 0) {
                trimCounter = 2048
                trimMemory()
            }

        }
    }

    private fun trimMemory() {
        var threshold = 0
        while (dictionary.size > 65535) {
            dictionary.entries.removeAll { it.value == threshold}
            threshold ++
        }
    }

    fun commitSession(session: String): Map<String, Int> {
        if (session != currentSession) return emptyMap()
        val copy = dictionary.toMap()
        beginSession()
        return copy
    }

    private fun generateSessionId(): String {
        return (System.currentTimeMillis().toString() + (Math.random() * 100000).toInt().toString())
    }
}

internal expect fun currentTimeMillis(): Long
