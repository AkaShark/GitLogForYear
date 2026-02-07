package com.git.log.common.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    textList: List<String>,
    modifier: Modifier = Modifier,
    typingSpeed: Long = 80L,
    pauseDuration: Long = 2000L,
    fontSize: TextUnit = 30.sp,
    textColor: Color = Color.White,
    cursorColor: Color = Color.White
) {
    if (textList.isEmpty()) return

    var displayText by remember { mutableStateOf("") }
    var currentTextIndex by remember { mutableIntStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            showCursor = !showCursor
        }
    }

    LaunchedEffect(currentTextIndex) {
        val currentText = textList.getOrNull(currentTextIndex) ?: return@LaunchedEffect
        displayText = ""

        for (i in currentText.indices) {
            delay(typingSpeed)
            displayText = currentText.substring(0, i + 1)
        }

        delay(pauseDuration)
        currentTextIndex = (currentTextIndex + 1) % textList.size
    }

    Text(
        text = buildAnnotatedString {
            withStyle<Unit>(
                style = SpanStyle(
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            ) {
                append(displayText)
            }
            if (showCursor) {
                withStyle(
                    style = SpanStyle(
                        color = cursorColor,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Light
                    )
                ) {
                    append("_")
                }
            }
        },
        modifier = modifier
    )

}