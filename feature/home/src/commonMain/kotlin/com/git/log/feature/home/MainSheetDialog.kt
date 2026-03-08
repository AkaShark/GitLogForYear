package com.git.log.feature.home

import LocalRepoDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.git.log.common.git.model.SourcePackage
import com.git.log.common.git.model.SourceRegister
import com.git.log.common.git.model.SourceRegistrationData


@Composable
fun MainSheetDialog(
    onDismiss: () -> Unit,
    onConfirm: (SourcePackage) -> Unit
) {
    var sources by remember { mutableStateOf(listOf<SourceRegistrationData>()) }
    var showLocalRepoSheet by remember { mutableStateOf(false) }
    var namespace by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "仓库配置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min=300.dp, max=500.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showLocalRepoSheet = true }
                    ) {
                        Text("添加本地仓库")
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = namespace,
                    onValueChange = { namespace = it},
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                if (sources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无数据源，点击添加本地仓库",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(sources) { index, source ->
                            SourceRow(
                                data = source,
                                onDelete = {
                                    sources = sources.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }
                            )
                            if (index < sources.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sources.isEmpty()) {
                        // 提示无数据源
                        return@Button
                    }
                    val tempDir = System.getProperty("java.io.tmpdir") + "/gitlogforyear_" + System.currentTimeMillis()
                    val sourcePackage = SourcePackage.create(sources, tempDir)
                    onConfirm(sourcePackage)
                }
            ) {
                Text("开始分析")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 本地仓库选择弹窗
    if (showLocalRepoSheet) {
        LocalRepoDialog(
            onDismiss = { showLocalRepoSheet = false },
            onConfirm = { repos ->
                showLocalRepoSheet = false
                val data = SourceRegistrationData(
                    id = repos.hashCode().toString(),
                    register = SourceRegister.LOCAL,
                    repos = repos.map { SourceRegistrationData.RepoElement.localRepo(it) },
                    displayPath = if (repos.isNotEmpty()) repos.first().substringBeforeLast('/') else ""
                )
                sources = sources + data
            }
        )
    }

}

@Composable
private fun SourceRow(
    data: SourceRegistrationData,
    onDelete: () -> Unit
) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier
            .weight(1f)
        ) {
            Text(
                text = data.register.displayName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = data.displayPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "共 ${data.repos.size} 个仓库",
                style = MaterialTheme.typography.labelSmall
            )
        }
        TextButton(onClick = onDelete) {
            Text("删除", color = MaterialTheme.colorScheme.error)
        }
    }
}