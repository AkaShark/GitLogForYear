package com.git.log.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.git.log.common.tool.AppColors

@Composable
fun ActionButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = AppColors.TextPrimary
    val borderColor = AppColors.ButtonBorder

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp
        )
    }
}