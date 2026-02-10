package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// 成就系统
@Serializable
data class Achievement(
    val name: String,
    val describe: String
)