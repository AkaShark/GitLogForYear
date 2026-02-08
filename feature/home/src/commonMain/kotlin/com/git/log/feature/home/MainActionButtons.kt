package com.git.log.feature.home

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.git.log.common.components.ActionButton

@Composable
fun MainActionButtons(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    borderColor: Color = Color.Transparent,
    contentColor: Color = Color.White
) {
    Row {
        ActionButton(
            title = "我的代码年度报告",
            leadingIcon = {},
            onClick = {
                onClick()
            },
            modifier = modifier,
            titleColor = titleColor,
            borderColor = borderColor,
            contentColor = contentColor
        )
    }
}