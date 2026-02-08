package com.git.log.feature.home.viewmodel

import com.git.log.common.tool.inter.openUri
import com.git.log.feature.home.DataSourceItem

class HomeViewModel {
    fun onDataSourceClicked(item: DataSourceItem) {
        item.uri?.let { openUri(it) }
    }
}