package com.git.log.common.git.result

import com.git.log.common.git.model.ReportDataSource
import com.git.log.common.git.model.ResultSectionUpdateRecipe


interface ResultSectionData {
    fun update(scannerResult: ReportDataSource): ResultSectionUpdateRecipe?
}

interface ResultSectionBadgeData {
    fun setBadge(badges: List<ResultSectionUpdateRecipe>)
}