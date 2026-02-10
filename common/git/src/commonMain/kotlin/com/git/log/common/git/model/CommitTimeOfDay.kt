package com.git.log.common.git.model

// 提交时间段
enum class CommitTimeOfDay(val displayName: String) {
    MIDNIGHT("凌晨"),
    MORNING("早晨"),
    NOON("中午"),
    AFTERNOON("下午"),
    DINNER("晚餐时间"),
    NIGHT("晚上");

    companion object {
        fun fromHour(hour: Int): CommitTimeOfDay {
            return when(hour) {
                in 0..4 -> MIDNIGHT
                in 5..9 -> MORNING
                in 10..13 -> NOON
                in 14..16 -> AFTERNOON
                in 17..18 -> DINNER
                else -> NIGHT
            }
        }
    }
}
