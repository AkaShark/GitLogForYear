package com.git.log.common.git.model

import kotlinx.serialization.Serializable

// Diff 模式
@Serializable
enum class DiffMode {
    ADD,
    MODIFY,
    DELETE
}