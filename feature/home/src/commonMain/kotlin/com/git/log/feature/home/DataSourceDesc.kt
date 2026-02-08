package com.git.log.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import git_log_agent.feature.home.generated.resources.Res
import git_log_agent.feature.home.generated.resources.git
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class DataSourceType {
    GIT,
    GITHUB,
}

data class DataSourceItem(
    val type: DataSourceType,
    val name: String,
    val icon: DrawableResource,
    val uri: String? = null,
)

private val supportedDataSources = listOf<DataSourceItem>(
    DataSourceItem(DataSourceType.GIT, "Git", Res.drawable.git, uri = "https://git-scm.com/"),
)

@Composable
fun DataSourceDesc(
    modifier: Modifier = Modifier,
    onClickItem: (DataSourceItem) -> Unit
) {
    Column (
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "本报告支持以下数据源",
            fontSize = 10.sp,
            color = Color.White,

        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            supportedDataSources.forEach { item ->
                DataSourceRow(
                    item,
                    onClick = { onClickItem(item) }
                )
            }
        }

    }
}

@Composable
private fun DataSourceRow(
    item: DataSourceItem,
    onClick: (DataSourceItem) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row (
        modifier = Modifier
            .clickable(
                onClick = { onClick (item) },
                interactionSource = interactionSource,
                indication = null,
            ),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Image(
            painter = painterResource(item.icon),
            contentDescription = null,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = item.name,
            fontSize = 12.sp,
            color = Color.White,
        )
    }
}

