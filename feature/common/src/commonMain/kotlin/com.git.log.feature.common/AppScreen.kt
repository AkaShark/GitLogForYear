import com.git.log.common.git.model.SourcePackage
import com.git.log.common.git.result.ResultPackage

sealed class AppScreen {
    data object Home: AppScreen()
    data class Analysis(val sourcePackage: SourcePackage) : AppScreen()
    data class Result(val resultPackage: ResultPackage) : AppScreen()
}
