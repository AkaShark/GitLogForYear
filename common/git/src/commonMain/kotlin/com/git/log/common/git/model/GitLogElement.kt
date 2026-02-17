package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// Git Log 元素
@Serializable
data class GitLogElement(
    val hash: String,
    val email: String,
    val name: String,
    val date: String,
    val note: String
)