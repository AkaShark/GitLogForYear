package com.git.log.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.git.log.common.tool.AppColors

@Composable
fun ActionButton(
    title: String,
    titleColor: Color,
    borderColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable () -> Unit = {},
    onClick: () -> Unit,
    paddingValues: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
) {

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(percent = 50),
        contentPadding = paddingValues
    ) {
       Row(
           verticalAlignment = Alignment.CenterVertically
       ) {
           if (leadingIcon != {}) {
               leadingIcon()
               Spacer(Modifier.width(3.dp))
           }
           Text(
               text = title,
               fontSize = 13.sp,
               color = titleColor
           )
       }
    }
}