package com.sharker.gitlog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform