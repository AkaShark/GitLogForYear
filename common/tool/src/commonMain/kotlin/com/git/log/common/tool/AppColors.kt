package com.git.log.common.tool

import androidx.compose.ui.graphics.Color

object AppColors {
    // 背景渐变色
    val GradientStart1 = Color(0xFF2D1B3D)
    val GradientEnd1 = Color(0xFF1A2A3D)
    val GradientStart2 = Color(0xFF3D2B4D)
    val GradientEnd2 = Color(0xFF2D3B4D)
    val GradientStart3 = Color(0xFF4D3B5D)
    val GradientEnd3 = Color(0xFF3D4B5D)

    // 数据源图标颜色
    val GitColor = Color(0xFFE84D31)
    val GitLabColor = Color(0xFFFC6D26)
    val GitHubColor = Color.White
    val BitbucketColor = Color(0xFF2684FF)

    // 文本颜色
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.7f)
    val TextTertiary = Color.White.copy(alpha = 0.4f)
    val CursorColor = Color.White.copy(alpha = 0.7f)

    // 按钮颜色
    val ButtonBorder = Color.White.copy(alpha = 0.5f)
    val SpecialButtonColor = Color(0xFFE8A849)
}