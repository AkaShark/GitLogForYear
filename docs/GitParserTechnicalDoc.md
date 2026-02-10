# Git 分析模块 — CMP 迁移技术设计文档

> 基于 MyYearWithGit 原始项目深度源码分析
>
> 文档版本: 1.0 | 日期: 2026-02-10

---

## 一、模块总览

Git 分析模块是 MyYearWithGit 的核心业务链路，涵盖从用户触发入口到最终分析报告展示的完整闭环。本文档将该模块拆分为四大子系统进行详细设计：

| 子系统 | 功能范围 | 原始文件 |
|--------|---------|---------|
| **UI 入口与导航** | 首页展示、配置弹窗、数据源选择 | `NavigatorView` / `MainView` / `MainSheet` |
| **文件选择与数据源管理** | 本地仓库选择、远程仓库配置、邮箱/过滤配置 | `LocalRepoSheet` / `PickSourceSheet` / `SourceRegister` |
| **Git 分析引擎** | git log 解析、git diff 解析、词频统计、多线程调度 | `RepoAnalyser` / `GitLog` / `GitDiff` / `DictionaryBuilder` |
| **分析报告 UI** | 10 张报告卡片、成就系统、导出功能 | `ResultView` / `ResultPackage` / `RS0-RS9` |

### 1.1 完整业务流转图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          NavigatorView (导航控制器)                       │
│   state: sourcePackage / resultPackage / progress                       │
│   路由逻辑: nil→MainView | sourcePackage→AnalysisView | result→ResultView│
├───────────┬───────────────────┬──────────────────┬──────────────────────┤
│  Phase 1  │     Phase 2       │     Phase 3      │      Phase 4         │
│           │                   │                  │                      │
│ MainView  │   MainSheet       │  AnalysisView    │   ResultView         │
│ 首页入口   │   数据源配置       │  分析进度展示     │   报告卡片展示        │
│           │                   │                  │                      │
│ ┌───────┐ │ ┌───────────────┐ │ ┌──────────────┐ │ ┌──────────────────┐ │
│ │打字机  │ │ │PickSourceSheet│ │ │RepoAnalyser  │ │ │RS0: 热力图+哈希  │ │
│ │效果标题│ │ │选择数据源类型  │ │ │  ├ GitLog     │ │ │RS1: 年度总览     │ │
│ │       │ │ │               │ │ │  ├ GitDiff    │ │ │RS2: 编程语言     │ │
│ │渐变背景│ │ │LocalRepoSheet │ │ │  ├ Dictionary │ │ │RS3: 提交时间段   │ │
│ │动画   │ │ │GitHubRepoSheet│ │ │  └ CommitFilter│ │ │RS4: 高频词汇    │ │
│ │       │ │ │GitLabRepoSheet│ │ │              │ │ │RS5: 空行统计     │ │
│ │开启按钮│ │ │BitbucketSheet │ │ │ProgressView  │ │ │RS6: 特别日统计   │ │
│ │       │ │ │ConfigEmailSheet│ │ │进度条+状态文字 │ │ │RS9: 时光印记     │ │
│ │数据源  │ │ │CommitFilter   │ │ │              │ │ │RS7: 成就墙       │ │
│ │链接   │ │ │Sheet          │ │ │              │ │ │RS8: 尾页+二维码  │ │
│ └───────┘ │ └───────────────┘ │ └──────────────┘ │ │                  │ │
│           │                   │                  │ │导出/截图/打印/重置│ │
│           │                   │                  │ └──────────────────┘ │
└───────────┴───────────────────┴──────────────────┴──────────────────────┘
```

### 1.2 核心事件通信链（原项目基于 NotificationCenter）

```
MainView
  └─ [用户点击 "开启我的年度报告"]
      └─ MainSheet (打开 Sheet)
          ├─ [用户点击 "添加"] → PickSourceSheet → .openSheet 通知
          │   └─ 根据选择打开对应 Sheet (Local/GitHub/GitLab/Bitbucket)
          │       └─ .sourceAdd 通知 → MainSheet 接收并存储
          ├─ [用户点击 "配置邮箱地址"] → ConfigEmailSheet
          ├─ [用户点击 "配置排除项"] → FilterSheet
          └─ [用户确认] → SourcePackage.postToAnalysis()
              └─ .postAnalysis 通知 → NavigatorView 接收
                  └─ sourcePackage 被赋值 → 路由到 AnalysisView
                      └─ RepoAnalyser 开始工作
                          └─ .analysisComplete 通知 → NavigatorView 接收
                              └─ resultPackage 被赋值 → 路由到 ResultView
```

**CMP 迁移方案**: 将 `NotificationCenter` 通信改为 **Kotlin SharedFlow / StateFlow** + **Navigation** 组件的类型安全导航。

---

## 二、UI 入口子系统

### 2.1 NavigatorView — 全局导航控制器

#### 2.1.1 原始实现分析

```swift
// 原始 Swift 状态机
struct NavigatorView: View {
    @State var sourcePackage: SourcePackage? = nil   // 非空时显示 AnalysisView
    @State var resultPackage: ResultPackage? = nil    // 非空时显示 ResultView
    // 三态路由: MainView → AnalysisView → ResultView
}
```

#### 2.1.2 CMP 设计

```kotlin
// ===== 导航状态定义 =====
sealed class AppScreen {
    object Home : AppScreen()
    data class Analysis(val sourcePackage: SourcePackage) : AppScreen()
    data class Result(val resultPackage: ResultPackage) : AppScreen()
}

// ===== ViewModel =====
class NavigatorViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun startAnalysis(sourcePackage: SourcePackage) {
        _currentScreen.value = AppScreen.Analysis(sourcePackage)
    }
    
    fun showResult(resultPackage: ResultPackage) {
        _currentScreen.value = AppScreen.Result(resultPackage)
    }
    
    fun reset() {
        _currentScreen.value = AppScreen.Home
    }
    
    /**
     * 支持拖拽导入已有报告文件 (.mygitreport)
     * 原项目通过 onDrop 修饰符实现
     */
    fun loadReportFromFile(data: ByteArray) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dataSource = Json.decodeFromString<ReportDataSource>(data.decodeToString())
                val resultPackage = ResultPackage(dataSource)
                resultPackage.update()
                _currentScreen.value = AppScreen.Result(resultPackage)
            } catch (e: Exception) {
                // 错误处理
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// ===== Compose 导航 =====
@Composable
fun NavigatorView(viewModel: NavigatorViewModel = viewModel()) {
    val screen by viewModel.currentScreen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = screen) {
            is AppScreen.Home -> MainView(
                onStartAnalysis = { sourcePackage ->
                    viewModel.startAnalysis(sourcePackage)
                }
            )
            is AppScreen.Analysis -> AnalysisView(
                sourcePackage = current.sourcePackage,
                onAnalysisComplete = { resultPackage ->
                    viewModel.showResult(resultPackage)
                }
            )
            is AppScreen.Result -> ResultView(
                resultPackage = current.resultPackage,
                onReset = { viewModel.reset() }
            )
        }
        
        if (isLoading) {
            // 全局加载遮罩
            LoadingOverlay(text = "正在解析数据...")
        }
    }
}
```

#### 2.1.3 平台差异处理

| 能力 | Desktop | Android | iOS |
|------|---------|---------|-----|
| 拖拽导入报告 | Swing DnD → Compose onDrop | Intent/ContentResolver | UIDocumentPickerViewController |
| 窗口关闭保护 | Window.isCloseRequested | Activity.onBackPressed | 无需处理 |

### 2.2 MainView — 首页

#### 2.2.1 组件拆解

```
┌──────────────────────────────────────┐
│  [ColorfulView 渐变背景动画]          │
│                                      │
│  ┌──────────────────────────────┐    │
│  │ TextTypeEffectView           │    │
│  │ "我和我的代码，还有这一年。"    │    │
│  │ + 各语言 Hello World 循环    │    │
│  └──────────────────────────────┘    │
│                                      │
│  [→ 开启我的年度报告]  [致谢]         │
│                                      │
│  此年度报告支持以下数据源:             │
│  🔀 Git                              │
│  🦊 GitLab                           │
│  🐙 GitHub                           │
│  🪣 Bitbucket                        │
│                                      │
│           由 标准件厂长@砍砍 制作       │
└──────────────────────────────────────┘
```

#### 2.2.2 CMP Compose 设计

```kotlin
@Composable
fun MainView(onStartAnalysis: (SourcePackage) -> Unit) {
    var showMainSheet by remember { mutableStateOf(false) }
    var showThanksSheet by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 渐变背景动画层
        GradientAnimationBackground()
        
        // 内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(60.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(80.dp))
            
            // 打字机效果标题
            TextTypeEffectView(
                textList = mainTitleTextList,
                modifier = Modifier.height(60.dp)
            )
            
            // 按钮行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showMainSheet = true }) {
                    Icon(Icons.Default.ArrowForward, null)
                    Text("开启我的年度报告")
                }
                Button(onClick = { showThanksSheet = true }) {
                    Text("致谢")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // 数据源链接列表
            DataSourceLinks()
        }
        
        // 底部署名
        FooterSignature(modifier = Modifier.align(Alignment.BottomCenter))
    }
    
    // 弹窗
    if (showMainSheet) {
        MainSheetDialog(
            onDismiss = { showMainSheet = false },
            onConfirm = { sourcePackage ->
                showMainSheet = false
                onStartAnalysis(sourcePackage)
            }
        )
    }
    if (showThanksSheet) {
        ThanksDialog(onDismiss = { showThanksSheet = false })
    }
}
```

#### 2.2.3 打字机效果 — 详细实现

原项目使用 Timer 驱动，CMP 使用 `LaunchedEffect` + `delay`:

```kotlin
@Composable
fun TextTypeEffectView(
    textList: List<String>,
    typingSpeed: Long = 100L,  // 每字符毫秒
    pauseDuration: Long = 1000L,  // 每段文字完成后暂停
    modifier: Modifier = Modifier
) {
    var displayText by remember { mutableStateOf("") }
    
    LaunchedEffect(textList) {
        var currentIndex = -1
        while (true) {
            currentIndex = (currentIndex + 1) % textList.size
            val targetText = textList[currentIndex]
            
            // 逐字打出
            for (i in 1..targetText.length) {
                displayText = targetText.substring(0, i) + "_"
                delay(typingSpeed)
            }
            
            // 完成后暂停
            displayText = targetText + "_"
            delay(pauseDuration)
            
            // 清除
            displayText = ""
            delay(200L)
        }
    }
    
    Text(
        text = displayText,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
        ),
        modifier = modifier
    )
}
```

#### 2.2.4 渐变背景动画

原项目使用 `ColorfulX` 库，CMP 使用 `Canvas` + `Animatable` 自绘：

```kotlin
@Composable
fun GradientAnimationBackground(modifier: Modifier = Modifier) {
    val isDarkTheme = isSystemInDarkTheme()
    var colorSet by remember { mutableStateOf(getRandomColorSet(isDarkTheme)) }
    val animatedColors = colorSet.map { animateColorAsState(it, tween(3000)) }
    
    // 每5秒切换配色
    LaunchedEffect(isDarkTheme) {
        while (true) {
            delay(5000L)
            colorSet = getRandomColorSet(isDarkTheme)
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize().alpha(0.25f)) {
        val brush = Brush.linearGradient(
            colors = animatedColors.map { it.value },
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
        drawRect(brush = brush)
    }
}

private fun getRandomColorSet(isDark: Boolean): List<Color> {
    val lightThemes = listOf(
        listOf(Color(0xFFA8E6CF), Color(0xFFDCEDC1), Color(0xFFFFD3B6)),
        listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7))
    )
    val darkThemes = listOf(
        listOf(Color(0xFF1A237E), Color(0xFF4A148C), Color(0xFF006064)),
        listOf(Color(0xFF0D47A1), Color(0xFF1B5E20), Color(0xFF4E342E))
    )
    return if (isDark) darkThemes.random() else lightThemes.random()
}
```

---

## 三、文件选择与数据源管理子系统

### 3.1 数据源管理架构

#### 3.1.1 数据源类型枚举

```kotlin
enum class SourceRegister(val displayName: String) {
    LOCAL("本地仓库"),
    GITHUB("GitHub"),
    GITLAB("GitLab"),
    BITBUCKET("Bitbucket");
}
```

#### 3.1.2 数据源注册数据结构

```kotlin
@Serializable
data class SourceRegistrationData(
    val id: String = uuid4().toString(),
    val register: SourceRegister,
    val mainUrl: String,         // 本地路径或远程 URL
    val repos: List<RepoElement>
) {
    @Serializable
    data class RepoElement(
        val representedData: Map<RepresentedKey, String>
    ) {
        enum class RepresentedKey {
            REMOTE_URL, LOCAL_URL, USERNAME, TOKEN, IDENTIFIER
        }
        
        companion object {
            fun localRepo(localUrl: String) = RepoElement(
                mapOf(
                    RepresentedKey.LOCAL_URL to localUrl,
                    RepresentedKey.IDENTIFIER to uuid4().toString()
                )
            )
            fun remoteRepo(remoteUrl: String, username: String, token: String) = RepoElement(
                mapOf(
                    RepresentedKey.REMOTE_URL to remoteUrl,
                    RepresentedKey.USERNAME to username,
                    RepresentedKey.TOKEN to token,
                    RepresentedKey.IDENTIFIER to uuid4().toString()
                )
            )
        }
    }
}
```

#### 3.1.3 SourcePackage（分析任务包）

```kotlin
data class SourcePackage(
    val tempDir: String,              // 临时目录路径
    val representedObjects: List<SourceRegistrationData>
) {
    companion object {
        fun create(sources: List<SourceRegistrationData>): SourcePackage {
            val tempDir = createTempAnalysisDirectory()
            return SourcePackage(tempDir, sources)
        }
    }
}

// expect/actual：临时目录创建
expect fun createTempAnalysisDirectory(): String
```

### 3.2 MainSheet — 配置弹窗主面板

#### 3.2.1 UI 布局

```
┌─────────────────────────────────────────────┐
│  数据源                          [取消] [确定]│
├─────────────────────────────────────────────┤
│  [添加] [配置邮箱地址] [配置排除项]            │
│  namespace:: [输入昵称 可选___]               │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐    │
│  │ 本地仓库                             │    │
│  │ /Users/xxx/Projects                 │    │
│  │                  共 12 个仓库 [编辑][删除]│   │
│  ├─────────────────────────────────────┤    │
│  │ GitHub                              │    │
│  │ https://github.com                  │    │
│  │                  共 25 个仓库 [编辑][删除]│   │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

#### 3.2.2 CMP Compose 实现

```kotlin
@Composable
fun MainSheetDialog(
    onDismiss: () -> Unit,
    onConfirm: (SourcePackage) -> Unit
) {
    val viewModel: MainSheetViewModel = viewModel()
    val sources by viewModel.currentSources.collectAsState()
    val namespace by viewModel.namespace.collectAsState()
    
    var showPickSourceSheet by remember { mutableStateOf(false) }
    var showEmailSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var activeSourceSheet by remember { mutableStateOf<SourceRegister?>(null) }
    
    SheetTemplate(
        title = "数据源",
        onCancel = {
            if (sources.isNotEmpty()) {
                // 弹出确认对话框
                viewModel.showDiscardConfirmation()
            } else {
                onDismiss()
            }
        },
        onConfirm = {
            if (sources.isEmpty()) {
                // 提示"没有可用的数据源"
                viewModel.showNoSourceAlert()
                onDismiss()
            } else {
                val sourcePackage = SourcePackage.create(sources)
                onConfirm(sourcePackage)
            }
        }
    ) {
        Column {
            // 按钮栏
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPickSourceSheet = true }) { Text("添加") }
                Button(onClick = { showEmailSheet = true }) { Text("配置邮箱地址") }
                Button(onClick = { showFilterSheet = true }) { Text("配置排除项") }
                Spacer(Modifier.width(20.dp))
                Text("namespace::", fontFamily = FontFamily.Monospace)
                TextField(
                    value = namespace,
                    onValueChange = { viewModel.updateNamespace(it) },
                    placeholder = { Text("输入昵称 可选") },
                    singleLine = true
                )
            }
            
            Divider()
            
            // 已添加的数据源列表
            LazyColumn {
                itemsIndexed(sources) { index, source ->
                    SourceRegistrationRow(
                        data = source,
                        onEdit = { viewModel.editSource(index) },
                        onDelete = { viewModel.removeSource(index) }
                    )
                    if (index < sources.lastIndex) Divider()
                }
            }
        }
    }
    
    // 子弹窗
    if (showPickSourceSheet) {
        PickSourceDialog(
            onDismiss = { showPickSourceSheet = false },
            onSourceSelected = { register ->
                showPickSourceSheet = false
                activeSourceSheet = register
            }
        )
    }
    
    activeSourceSheet?.let { register ->
        when (register) {
            SourceRegister.LOCAL -> LocalRepoDialog(
                onDismiss = { activeSourceSheet = null },
                onSourceAdd = { data ->
                    viewModel.addSource(data)
                    activeSourceSheet = null
                }
            )
            SourceRegister.GITHUB -> GitHubRepoDialog(...)
            SourceRegister.GITLAB -> GitLabRepoDialog(...)
            SourceRegister.BITBUCKET -> BitbucketDialog(...)
        }
    }
}

// ===== ViewModel =====
class MainSheetViewModel : ViewModel() {
    private val _currentSources = MutableStateFlow<List<SourceRegistrationData>>(emptyList())
    val currentSources: StateFlow<List<SourceRegistrationData>> = _currentSources
    
    private val _namespace = MutableStateFlow(User.current.namespace)
    val namespace: StateFlow<String> = _namespace
    
    fun addSource(data: SourceRegistrationData) {
        _currentSources.value = _currentSources.value + data
    }
    
    fun removeSource(index: Int) {
        _currentSources.value = _currentSources.value.toMutableList().apply { removeAt(index) }
    }
    
    fun updateNamespace(value: String) {
        _namespace.value = value
        User.current.namespace = value
    }
}
```

### 3.3 本地仓库选择 — LocalRepoSheet

#### 3.3.1 完整交互流程

```
用户点击 "选择..."
    ↓
平台文件选择器 (expect/actual)      ← 允许多选目录
    ↓
递归搜索 .git 目录                  ← 后台线程，深度限制 64
    ├ 跳过 node_modules 等黑名单  
    ├ 显示进度弹窗 "正在查找代码仓库..."
    └ 用户可点击 "取消" 中止
    ↓
搜索结果显示在列表中                  ← 每行: 路径 + [删除] 按钮
    ↓
用户可通过关键词批量删除仓库
    ↓
用户确认 → 构建 SourceRegistrationData → 发送给 MainSheet
```

#### 3.3.2 UI 布局

```
┌──────────────────────────────────────────────┐
│  添加来自本地的仓库                  [取消][确定]│
├──────────────────────────────────────────────┤
│  [请选择文件夹_____________]       [选择...]   │
├──────────────────────────────────────────────┤
│  删除包含 <关键词> 的仓库: [________] [删除关键词]│
├──────────────────────────────────────────────┤
│  /Users/xxx/ProjectA/.git          [删除]     │
│  ──────────────────────────────────           │
│  /Users/xxx/ProjectB/.git          [删除]     │
│  ──────────────────────────────────           │
│  /Users/xxx/LibC/.git              [删除]     │
│  ...                                         │
└──────────────────────────────────────────────┘
```

#### 3.3.3 仓库搜索算法 (核心)

```kotlin
/**
 * 递归搜索指定目录下的所有 Git 仓库
 * 
 * 算法逻辑:
 * 1. 从搜索根目录开始递归遍历
 * 2. 如果当前目录下存在 .git 子目录，标记为仓库找到，不继续深入
 * 3. 如果当前目录在黑名单中，跳过
 * 4. 深度限制: 64 层（防止栈溢出）
 * 5. 支持中途取消
 */
private val blockedDirectoryNames = setOf("node_modules")

suspend fun searchGitRepositories(
    searchRoots: List<String>,
    onCancel: () -> Boolean  // 返回 true 表示取消
): List<String> = withContext(Dispatchers.IO) {
    val results = mutableSetOf<String>()
    
    fun search(root: String, depth: Int) {
        if (depth >= 64) return
        if (onCancel()) return
        
        val dirName = root.substringAfterLast("/")
        if (blockedDirectoryNames.contains(dirName.lowercase())) return
        
        if (!fileExists(root)) return
        
        val gitDir = "$root/.git"
        if (fileExists(gitDir) && isDirectory(gitDir)) {
            // 找到仓库，不再深入
            results.add(root)
            return
        }
        
        // 继续递归搜索子目录
        listDirectory(root)
            .filter { isDirectory("$root/$it") }
            .forEach { child ->
                search("$root/$child", depth + 1)
            }
    }
    
    searchRoots.forEach { root ->
        search(root, 0)
    }
    
    results.sorted()
}

// ===== expect/actual 文件系统抽象 =====
expect fun fileExists(path: String): Boolean
expect fun isDirectory(path: String): Boolean
expect fun listDirectory(path: String): List<String>
```

#### 3.3.4 文件选择器 (expect/actual)

```kotlin
// ===== commonMain =====
expect class DirectoryPicker {
    /**
     * 启动目录选择器
     * @param allowMultiple 是否允许多选
     * @param onResult 选择结果回调，null 表示取消
     */
    fun launch(allowMultiple: Boolean, onResult: (List<String>?) -> Unit)
}

// ===== desktopMain =====
actual class DirectoryPicker {
    actual fun launch(allowMultiple: Boolean, onResult: (List<String>?) -> Unit) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isMultiSelectionEnabled = allowMultiple
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val paths = if (allowMultiple) {
                chooser.selectedFiles.map { it.absolutePath }
            } else {
                listOfNotNull(chooser.selectedFile?.absolutePath)
            }
            onResult(paths)
        } else {
            onResult(null)
        }
    }
}

// ===== androidMain =====
actual class DirectoryPicker(private val activityResultLauncher: ActivityResultLauncher<Uri?>) {
    actual fun launch(allowMultiple: Boolean, onResult: (List<String>?) -> Unit) {
        // 使用 SAF (Storage Access Framework)
        activityResultLauncher.launch(null)
    }
}

// ===== iosMain =====
actual class DirectoryPicker {
    actual fun launch(allowMultiple: Boolean, onResult: (List<String>?) -> Unit) {
        // 使用 UIDocumentPickerViewController
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes: listOf(UTType.folder),
            asCopy: false
        )
        picker.allowsMultipleSelection = allowMultiple
        // ... 委托回调
    }
}
```

### 3.4 提交过滤器 — CommitFileFilter

#### 3.4.1 过滤规则类型

```kotlin
@Serializable
enum class BlockType(val displayName: String) {
    NAME_KEYWORD("文件名关键词"),
    NAME_KEYWORD_CASE_SENSITIVE("文件名关键词 匹配大小写"),
    PATH_KEYWORD("路径关键词"),
    PATH_KEYWORD_CASE_SENSITIVE("路径关键词 匹配大小写"),
    PATH_COMPONENT_FULL_MATCH("路径中存在文件名"),
    PATH_COMPONENT_FULL_MATCH_CASE_SENSITIVE("路径中存在文件名 匹配大小写"),
    NAME_REGEX_FULL_MATCH("文件名正则表达式完整匹配");
}

@Serializable
data class BlockItem(
    val type: BlockType,
    val value: String
) {
    /**
     * 判断给定文件路径是否被此规则过滤
     * @return true = 匹配（应被排除）
     */
    fun matches(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast("/")
        val parentPath = filePath.substringBeforeLast("/")
        
        return when (type) {
            BlockType.NAME_KEYWORD ->
                fileName.lowercase().contains(value.lowercase())
            BlockType.NAME_KEYWORD_CASE_SENSITIVE ->
                fileName.contains(value)
            BlockType.PATH_KEYWORD ->
                parentPath.lowercase().contains(value.lowercase())
            BlockType.PATH_KEYWORD_CASE_SENSITIVE ->
                parentPath.contains(value)
            BlockType.PATH_COMPONENT_FULL_MATCH ->
                parentPath.split("/").any { it.equals(value, ignoreCase = true) }
            BlockType.PATH_COMPONENT_FULL_MATCH_CASE_SENSITIVE ->
                parentPath.split("/").any { it == value }
            BlockType.NAME_REGEX_FULL_MATCH ->
                Regex(value).matches(fileName)
        }
    }
}

/**
 * 过滤器管理器 (单例)
 * 规则持久化到本地存储
 */
class CommitFileFilter {
    companion object {
        val shared = CommitFileFilter()
    }
    
    var blockList: List<BlockItem> = loadFromStorage()
    
    /**
     * 判断文件是否通过过滤（不被排除）
     * @return true = 通过, false = 被排除
     */
    fun filter(filePath: String): Boolean {
        return blockList.none { it.matches(filePath) }
    }
    
    private fun loadFromStorage(): List<BlockItem> {
        val json = UserSettings.getString("wiki.qaq.block.list") ?: return emptyList()
        return Json.decodeFromString(json)
    }
    
    fun save() {
        val json = Json.encodeToString(blockList)
        UserSettings.setString("wiki.qaq.block.list", json)
    }
}
```

---

## 四、Git 分析引擎子系统

### 4.1 总体架构

```
┌────────────────────────────────────────────────────┐
│                 RepoAnalyser (单例)                 │
│                                                    │
│  ┌──────────────┐  ┌────────────────┐              │
│  │ beginSession()│  │ submitEmails() │              │
│  └──────┬───────┘  └────────────────┘              │
│         │                                          │
│  ┌──────▼──────────────────────────────────────┐   │
│  │         analysis(repoPath, session)          │   │
│  │                                              │   │
│  │  Step 1: grabGitCommitLog()                  │   │
│  │  ┌─────────────────────────────────────┐     │   │
│  │  │ exec: git log --all                 │     │   │
│  │  │ 解析: commit hash / author email /  │     │   │
│  │  │       date / note                   │     │   │
│  │  │ 过滤: requiredEmails + requiredYear │     │   │
│  │  │ 去重: commitHash Set                │     │   │
│  │  └─────────────────────────────────────┘     │   │
│  │                                              │   │
│  │  Step 2: 多线程处理每个 commit               │   │
│  │  ┌─────────────────────────────────────┐     │   │
│  │  │ grabGitCommitDetail(hash)           │     │   │
│  │  │ ┌──────────────────────────────┐    │     │   │
│  │  │ │ exec: git diff <hash>^!      │    │     │   │
│  │  │ │ 解析: file mode / path /     │    │     │   │
│  │  │ │       language / +/- lines   │    │     │   │
│  │  │ │ 过滤: CommitFileFilter       │    │     │   │
│  │  │ └──────────────────────────────┘    │     │   │
│  │  │ DictionaryBuilder.feed(增/删/提交)   │     │   │
│  │  └─────────────────────────────────────┘     │   │
│  │                                              │   │
│  │  Step 3: 构建 GitRepoResult                  │   │
│  └──────────────────────────────────────────────┘   │
│                                                    │
│  ┌──────────────────────────────────────────┐      │
│  │ commitResult() → ResultPackage            │      │
│  │  合并所有 repo 结果 + 词典数据              │      │
│  └──────────────────────────────────────────┘      │
└────────────────────────────────────────────────────┘
```

### 4.2 RepoAnalyser — 仓库分析器

#### 4.2.1 会话管理机制

原项目采用 UUID 会话机制防止并发分析互相干扰：

```kotlin
class RepoAnalyser {
    companion object {
        val shared = RepoAnalyser()
    }
    
    // ===== 会话管理 =====
    private var currentSession: String = ""
    private var currentResults = mutableListOf<GitRepoResult>()
    private var requiredEmails = listOf<String>()
    private var commitHashSet = mutableSetOf<String>() // 去重
    
    // ===== 词典会话 =====
    private var dictIncreaseSession: String = ""
    private var dictDecreaseSession: String = ""
    private var dictCommitSession: String = ""
    
    /**
     * 开始新分析会话
     * 重置所有状态，返回会话 ID
     */
    fun beginSession(): String {
        val session = uuid4().toString()
        currentSession = session
        currentResults.clear()
        requiredEmails = emptyList()
        commitHashSet.clear()
        dictIncreaseSession = DictionaryBuilder.sharedIncrease.beginSession()
        dictDecreaseSession = DictionaryBuilder.sharedDecrease.beginSession()
        dictCommitSession = DictionaryBuilder.sharedCommit.beginSession()
        return session
    }
    
    fun submitEmails(emails: List<String>) {
        requiredEmails = emails
    }
```

#### 4.2.2 分析主流程

```kotlin
    /**
     * 分析单个仓库
     *
     * 执行流程:
     * 1. 切换工作目录到仓库路径
     * 2. 执行 git log --all 获取所有提交
     * 3. 过滤: 仅保留匹配邮箱 + 目标年份的提交
     * 4. 去重: 同一 commit hash 只处理一次（多仓库可能有相同 commit）
     * 5. 并行对每个 commit 执行 git diff <hash>^! 分析文件差异
     * 6. 同时将代码内容喂给 DictionaryBuilder 做词频统计
     * 7. 收集所有 commit 结果构建 GitRepoResult
     * 8. 分析完毕后删除临时目录
     */
    suspend fun analysis(repoPath: String, session: String) {
        if (session != currentSession) return
        
        try {
            // Step 1: 获取 git log
            val logElements = grabGitCommitLog(repoPath) ?: return
            
            // Step 2: 过滤 + 并行分析
            val results = coroutineScope {
                val semaphore = Semaphore(processCount)  // 并发限制
                
                logElements
                    .filter { element ->
                        requiredEmails.contains(element.authorEmail)
                            && parseDate(element.date)?.year == requiredYear
                            && commitHashSet.add(element.hash)  // 原子去重
                    }
                    .map { element ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                val diffFiles = grabGitCommitDetail(repoPath, element.hash)
                                
                                // 将提交信息喂给词频统计
                                DictionaryBuilder.sharedCommit
                                    .feed(element.note, dictCommitSession)
                                    
                                GitCommitResult(
                                    email = element.authorEmail,
                                    date = parseDate(element.date)!!,
                                    diffFiles = diffFiles
                                )
                            }
                        }
                    }
                    .awaitAll()
            }
            
            if (results.isNotEmpty()) {
                currentResults.add(GitRepoResult(commits = results))
            }
        } finally {
            // 清理临时目录
            deletePath(repoPath)
        }
    }
    
    /**
     * 提交所有结果，生成报告包
     */
    fun commitResult(): ResultPackage {
        val dataSource = ReportDataSource(
            repoResult = currentResults.toList(),
            dictionaryIncrease = DictionaryBuilder.sharedIncrease.commitSession(dictIncreaseSession),
            dictionaryDecrease = DictionaryBuilder.sharedDecrease.commitSession(dictDecreaseSession),
            dictionaryCommit = DictionaryBuilder.sharedCommit.commitSession(dictCommitSession)
        )
        val resultPackage = ResultPackage(dataSource)
        resultPackage.update()
        beginSession()  // 重置状态
        return resultPackage
    }
}
```

### 4.3 GitLog 解析器

#### 4.3.1 git log 输出格式

```
commit abc123def456789...                    ← 以 "commit " 开头
Author: Lakr Aream <lakr@example.com>       ← 以 "Author: " 开头，提取 <> 内邮箱
Date:   Sun Apr 19 01:20:44 2020 +0800      ← 以 "Date:" 开头

    Commit message first line                ← 缩进的提交信息
    Commit message second line               ← 可多行

commit def456789abc123...                    ← 下一个提交开始
...
```

#### 4.3.2 解析实现

```kotlin
data class GitLogElement(
    val hash: String,
    val authorEmail: String,
    val date: String,
    val note: String
)

/**
 * 解析 git log --all 输出
 * 
 * 状态机:
 *   遇到 "commit " → 提交上一个记录，开始新记录
 *   遇到 "Author: " → 提取 <email>
 *   遇到 "Date:" → 提取日期字符串
 *   其他行 → 追加到 lineBuffer（提交信息）
 */
fun parseGitLog(output: String): List<GitLogElement>? {
    val results = mutableListOf<GitLogElement>()
    
    var currentHash: String? = null
    var authorEmail: String? = null
    var date: String? = null
    val lineBuffer = mutableListOf<String>()
    
    fun submitBarrier() {
        val h = currentHash ?: return
        val e = authorEmail ?: return
        val d = date ?: return
        
        val commitLog = lineBuffer
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        
        results.add(GitLogElement(h, e, d, commitLog))
        
        currentHash = null
        authorEmail = null
        date = null
        lineBuffer.clear()
    }
    
    for (line in output.lines()) {
        when {
            line.startsWith("commit ") -> {
                submitBarrier()
                currentHash = line.substringAfter("commit ").trim().lowercase()
            }
            line.startsWith("Author: ") -> {
                authorEmail = line.substringAfter("<")
                    .substringBefore(">")
                    .trim()
                    .lowercase()
            }
            line.startsWith("Date:") -> {
                date = line.substringAfter("Date:").trim()
            }
            else -> lineBuffer.add(line)
        }
    }
    submitBarrier()
    
    return results.ifEmpty { null }
}
```

#### 4.3.3 日期解析器

```kotlin
/**
 * 支持多种日期格式的解析器
 * 原项目支持 5 种格式以兼容不同 git 配置
 */
private val dateFormats = listOf(
    "E MMM d HH:mm:ss yyyy Z",          // Sun Apr 19 01:20:44 2020 +0800
    "E MMM d HH:mm:ss yyyy",            // Sun Apr 19 01:20:44 2020
    "E, d MMM yyyy HH:mm:ss Z",         // Sun, 19 Apr 2020 01:20:44 +0800
    "MM-dd-yyyy HH:mm",                 // 04-19-2020 01:20
    "EEEE, MMM d, yyyy"                 // Sunday, Apr 19, 2020
)

expect fun parseGitDate(dateString: String): LocalDateTime?
```

### 4.4 GitDiff 解析器

#### 4.4.1 git diff 输出格式

```
diff --git a/src/Main.swift b/src/Main.swift    ← diff 头，标记新文件块开始
new file mode 100644                              ← 新增文件（或 deleted / index / similarity）
--- /dev/null                                     ← 旧文件路径
+++ b/src/Main.swift                              ← 新文件路径（用于提取语言信息）
@@ -0,0 +1,10 @@                                  ← hunk 头，标记代码块开始
+import Foundation                                 ← + 开头：新增行
+                                                  ← 空白 + 行：新增的空行
+func main() {                                     ← + 开头：新增行
-old code                                          ← - 开头：删除行
 unchanged line                                    ← 空格开头：上下文行（忽略）
```

#### 4.4.2 状态机设计

```kotlin
/**
 * Diff 解析状态机
 * 
 * 三种状态:
 *   NONE   → 初始/idle 状态
 *   HEADER → 正在解析 diff 头部（文件模式、路径等）
 *   BODY   → 正在解析代码变更（+/- 行统计）
 * 
 * 状态转换规则:
 *   "diff --git" 或 "diff --cc" → 切换到 HEADER
 *   "@@" (hunk 头)              → 切换到 BODY
 *   其他行                      → 追加到当前 buffer
 * 
 * 切换时的处理：
 *   HEADER → 其他: 分析 header buffer，确定文件模式和语言
 *   BODY → 其他: 统计 +/- 行数，喂给 DictionaryBuilder
 */
enum class DiffParseStatus { NONE, HEADER, BODY }

fun parseGitDiff(
    diffOutput: String,
    dictIncreaseSession: String,
    dictDecreaseSession: String
): List<GitFileDiff> {
    val result = mutableListOf<GitFileDiff>()
    
    var currentDiff: GitFileDiff? = null
    var status = DiffParseStatus.NONE
    var buffer = mutableListOf<String>()
    
    fun commitHeaderForAnalysis() {
        val decisionLine = buffer.firstOrNull() ?: return
        val decisionWord = decisionLine.split(" ").firstOrNull() ?: return
        
        var language: SourceLanguage? = null
        var mode: DiffMode? = null
        
        when (decisionWord) {
            "index", "old" -> {
                mode = DiffMode.MODIFY
                // 从 "+++ " 行提取文件路径
                extractPathFromBuffer(buffer, "+++ ")?.let { path ->
                    if (CommitFileFilter.shared.filter(path)) {
                        language = SourceLanguage.fromExtension(path.extension())
                    }
                }
            }
            "new" -> {
                mode = DiffMode.ADD
                extractPathFromBuffer(buffer, "+++ ")?.let { path ->
                    if (CommitFileFilter.shared.filter(path)) {
                        language = SourceLanguage.fromExtension(path.extension())
                    }
                }
            }
            "similarity" -> {
                mode = DiffMode.MODIFY
                extractPathFromBuffer(buffer, "rename to")?.let { path ->
                    if (CommitFileFilter.shared.filter(path)) {
                        language = SourceLanguage.fromExtension(path.extension())
                    }
                }
            }
            "deleted" -> {
                mode = DiffMode.DELETE
                extractPathFromBuffer(buffer, "--- ")?.let { path ->
                    if (CommitFileFilter.shared.filter(path)) {
                        language = SourceLanguage.fromExtension(path.extension())
                    }
                }
            }
        }
        
        mode?.let {
            currentDiff = GitFileDiff(language, it, 0, 0, 0)
        }
    }
    
    fun commitBodyForAnalysis() {
        val diff = currentDiff ?: return
        var increased = diff.increasedLine
        var decreased = diff.decreasedLine
        var emptyLine = diff.emptyLineAdded
        
        for (line in buffer) {
            when {
                line.startsWith("+") -> {
                    val content = line.drop(1)
                    increased++
                    if (content.isBlank()) emptyLine++
                    DictionaryBuilder.sharedIncrease.feed(content, dictIncreaseSession)
                }
                line.startsWith("-") -> {
                    decreased++
                    DictionaryBuilder.sharedDecrease.feed(line.drop(1), dictDecreaseSession)
                }
            }
        }
        
        currentDiff = diff.copy(
            increasedLine = increased,
            decreasedLine = decreased,
            emptyLineAdded = emptyLine
        )
    }
    
    fun commitBodyBarrier() {
        commitBodyForAnalysis()
        currentDiff?.let { result.add(it) }
        currentDiff = null
    }
    
    fun switchStatus(newStatus: DiffParseStatus) {
        val prevStatus = status
        status = newStatus
        
        when (prevStatus) {
            DiffParseStatus.HEADER -> commitHeaderForAnalysis()
            DiffParseStatus.BODY -> commitBodyForAnalysis()
            DiffParseStatus.NONE -> {}
        }
        
        if (newStatus == DiffParseStatus.NONE && prevStatus == DiffParseStatus.BODY) {
            commitBodyBarrier()
        }
        if (newStatus == DiffParseStatus.HEADER && prevStatus == DiffParseStatus.BODY) {
            commitBodyBarrier()
        }
        
        buffer = mutableListOf()
    }
    
    // 主循环
    for (line in diffOutput.lines()) {
        when {
            line.startsWith("diff --git ") || line.startsWith("diff --cc ") ->
                switchStatus(DiffParseStatus.HEADER)
            line.startsWith("@@") ->
                switchStatus(DiffParseStatus.BODY)
            else -> buffer.add(line)
        }
    }
    switchStatus(DiffParseStatus.NONE)  // 处理最后一个块
    
    return result
}
```

### 4.5 DictionaryBuilder — 词频统计器

#### 4.5.1 核心设计

```kotlin
/**
 * 词频统计器
 * 
 * 设计要点:
 * 1. 三个独立实例: 新增代码词频 / 删除代码词频 / 提交信息词频
 * 2. 线程安全: 使用 Mutex 保护共享数据
 * 3. 内存限制: 超过 65535 个词条时自动裁剪低频词
 * 4. 过滤规则: 长度 > 3、纯字母、非数字
 * 5. 驼峰拆分: "handleUserClick" → ["handle", "User", "Click"]
 */
class DictionaryBuilder private constructor() {
    companion object {
        val sharedIncrease = DictionaryBuilder()
        val sharedDecrease = DictionaryBuilder()
        val sharedCommit = DictionaryBuilder()
    }
    
    private var currentSession = ""
    private var dictionary = mutableMapOf<String, Int>()
    private val mutex = Mutex()
    private var trimCounter = 1024
    
    fun beginSession(): String {
        val session = uuid4().toString()
        currentSession = session
        dictionary.clear()
        return session
    }
    
    suspend fun feed(buffer: String, session: String) {
        mutex.withLock {
            if (session != currentSession) return@withLock
            
            buffer.splitByCamelCase()
                .filter { it.length > 3 }
                .filter { it.isElegantForDictionary() }
                .filter { it.toDoubleOrNull() == null }
                .forEach { word ->
                    val key = word.lowercase()
                    dictionary[key] = (dictionary[key] ?: 0) + 1
                    trimCounter--
                    if (trimCounter < 0) {
                        trimCounter = 1024
                    }
                    if (trimCounter == 1024) {
                        trimMemory()
                    }
                }
        }
    }
    
    /**
     * 内存裁剪策略:
     * 当词条超过 65535 时，从频率 0 开始逐步删除低频词
     * 直到词条数降到 65535 以下
     */
    private fun trimMemory() {
        var threshold = 0
        while (dictionary.size > 65535) {
            dictionary.entries.removeAll { it.value == threshold }
            threshold++
        }
    }
    
    fun commitSession(session: String): Map<String, Int> {
        if (session != currentSession) return emptyMap()
        val copy = dictionary.toMap()
        beginSession()  // 重置
        return copy
    }
}

/**
 * 驼峰拆分 + 分词
 * "handleUserClick" → ["handle", "User", "Click"]
 * "XMLParser" → ["XML", "Parser"]
 */
fun String.splitByCamelCase(): List<String> {
    return this.split(Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])"))
}

/**
 * 判断字符串是否适合作为词典条目
 * 排除: 控制字符、非法字符、空白、标点、数字
 */
fun String.isElegantForDictionary(): Boolean {
    if (isEmpty()) return false
    return all { it.isLetter() }
}
```

### 4.6 Git 命令执行层 (expect/actual)

```kotlin
// ===== commonMain =====
expect class GitExecutor() {
    /**
     * 执行 git log --all
     */
    suspend fun log(repoPath: String): Result<String>
    
    /**
     * 执行 git diff <hash>^!
     * timeout: 30 秒（大型 commit 可能很慢）
     */
    suspend fun diff(repoPath: String, hash: String): Result<String>
    
    /**
     * 执行 git clone
     */
    suspend fun clone(url: String, destPath: String): Result<Unit>
    
    /**
     * 执行 git reset --hard
     * 用于本地仓库分析前重置工作区
     */
    suspend fun resetHard(repoPath: String): Result<Unit>
}

// ===== desktopMain =====
actual class GitExecutor actual constructor() {
    private val gitBinary: String = findGitBinary()
    
    actual suspend fun log(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(gitBinary, "log", "--all")
                .directory(File(repoPath))
                .start()
            process.inputStream.bufferedReader().readText()
        }
    }
    
    actual suspend fun diff(repoPath: String, hash: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(gitBinary, "diff", "$hash^!")
                .directory(File(repoPath))
                .start()
            
            // 30 秒超时
            val completed = process.waitFor(30, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                throw TimeoutException("git diff timeout for $hash")
            }
            if (process.exitValue() != 0) {
                throw RuntimeException("git diff failed: ${process.errorStream.bufferedReader().readText()}")
            }
            process.inputStream.bufferedReader().readText()
        }
    }
    
    private fun findGitBinary(): String {
        val searchPaths = listOf("/usr/local/bin/git", "/usr/bin/git", "/bin/git")
        return searchPaths.firstOrNull { File(it).exists() }
            ?: throw IllegalStateException("git not found")
    }
}

// ===== androidMain (使用 JGit) =====
actual class GitExecutor actual constructor() {
    actual suspend fun log(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val repo = FileRepositoryBuilder()
                .setGitDir(File(repoPath, ".git"))
                .build()
            val git = Git(repo)
            val logs = git.log().all().call()
            
            // 将 JGit RevCommit 转换为 git log 标准格式
            buildString {
                for (commit in logs) {
                    appendLine("commit ${commit.name}")
                    appendLine("Author: ${commit.authorIdent.name} <${commit.authorIdent.emailAddress}>")
                    appendLine("Date:   ${formatGitDate(commit.authorIdent.getWhen())}")
                    appendLine()
                    appendLine("    ${commit.fullMessage}")
                    appendLine()
                }
            }
        }
    }
}
```

### 4.7 本地仓库分析流程（AnalysisView 中的调度逻辑）

```kotlin
/**
 * 本地仓库分析流程:
 * 
 * 1. 创建临时目录
 * 2. 仅复制 .git 目录到临时位置（避免 node_modules 等大文件）
 * 3. 在临时位置执行 git reset --hard 恢复工作区
 * 4. 调用 RepoAnalyser.analysis() 执行分析
 * 5. 分析完成后自动删除临时目录
 */
suspend fun prepareLocalRepos(
    package: SourceRegistrationData,
    tempDir: String,
    onProgress: (String) -> Unit
) {
    for (repo in package.repos) {
        val localUrl = repo.representedData[RepresentedKey.LOCAL_URL] ?: continue
        val identifier = repo.representedData[RepresentedKey.IDENTIFIER] ?: continue
        val destPath = "$tempDir/$identifier"
        val repoName = localUrl.substringAfterLast("/")
        
        onProgress("正在创建分析副本 $repoName...")
        
        // 仅复制 .git 目录
        copyDirectory("$localUrl/.git", "$destPath/.git")
        
        // git reset --hard
        GitExecutor().resetHard(destPath)
        
        onProgress("正在分析 $repoName...")
        RepoAnalyser.shared.analysis(destPath, currentSession)
    }
}

/**
 * 远程仓库分析流程:
 * 
 * 1. 构造带认证信息的 clone URL
 * 2. git clone 到临时目录
 * 3. 调用 RepoAnalyser.analysis()
 */
suspend fun prepareRemoteRepos(
    package: SourceRegistrationData,
    tempDir: String,
    onProgress: (String) -> Unit
) {
    for (repo in package.repos) {
        val remoteUrl = repo.representedData[RepresentedKey.REMOTE_URL] ?: continue
        val token = repo.representedData[RepresentedKey.TOKEN] ?: continue
        val identifier = repo.representedData[RepresentedKey.IDENTIFIER] ?: continue
        val destPath = "$tempDir/$identifier"
        
        val cloneUrl = when (package.register) {
            SourceRegister.GITHUB -> buildGitHubCloneUrl(token, remoteUrl)
            SourceRegister.GITLAB -> buildGitLabCloneUrl(package.mainUrl, token, remoteUrl)
            SourceRegister.BITBUCKET -> {
                val username = repo.representedData[RepresentedKey.USERNAME] ?: "broken-auth"
                buildBitbucketCloneUrl(package.mainUrl, username, token, remoteUrl)
            }
            else -> continue
        }
        
        onProgress("正在从 ${package.register.displayName} 下载仓库 $remoteUrl...")
        GitExecutor().clone(cloneUrl, destPath)
        
        onProgress("正在分析 $remoteUrl...")
        RepoAnalyser.shared.analysis(destPath, currentSession)
    }
}
```

### 4.8 AnalysisView — 分析进度 UI

```kotlin
@Composable
fun AnalysisView(
    sourcePackage: SourcePackage,
    onAnalysisComplete: (ResultPackage) -> Unit
) {
    var progressTitle by remember { mutableStateOf("正在处理...") }
    var completed by remember { mutableIntStateOf(0) }
    val total = remember {
        sourcePackage.representedObjects.sumOf { it.repos.size } + 1
    }
    val progress = completed.toFloat() / total
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = progress)
            Spacer(Modifier.height(8.dp))
            Text(
                text = progressTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
    
    LaunchedEffect(sourcePackage) {
        withContext(Dispatchers.IO) {
            val session = RepoAnalyser.shared.beginSession()
            RepoAnalyser.shared.submitEmails(User.current.emails.toList())
            
            for (pkg in sourcePackage.representedObjects) {
                when (pkg.register) {
                    SourceRegister.LOCAL -> prepareLocalRepos(pkg, sourcePackage.tempDir,
                        onProgress = { title -> progressTitle = title })
                    else -> prepareRemoteRepos(pkg, sourcePackage.tempDir,
                        onProgress = { title -> progressTitle = title })
                }
                completed++
            }
            
            progressTitle = "正在生成汇总..."
            val result = RepoAnalyser.shared.commitResult()
            
            withContext(Dispatchers.Main) {
                onAnalysisComplete(result)
            }
        }
    }
}
```

---

## 五、分析报告 UI 子系统

### 5.1 数据模型

#### 5.1.1 核心数据结构

```kotlin
// ===== 报告数据源 =====
@Serializable
data class ReportDataSource(
    val repoResult: List<GitRepoResult>,
    val dictionaryIncrease: Map<String, Int>,
    val dictionaryDecrease: Map<String, Int>,
    val dictionaryCommit: Map<String, Int>
)

// ===== 仓库分析结果 =====
@Serializable
data class GitRepoResult(
    val commits: List<GitCommitResult>
)

// ===== 单次提交结果 =====
@Serializable
data class GitCommitResult(
    val email: String,
    val date: LocalDateTime,
    val diffFiles: List<GitFileDiff>
)

// ===== 文件差异 =====
@Serializable
data class GitFileDiff(
    val language: SourceLanguage?,
    val mode: DiffMode,
    val emptyLineAdded: Int,
    val increasedLine: Int,
    val decreasedLine: Int
)

@Serializable
enum class DiffMode { ADD, MODIFY, DELETE }

// ===== 成就 =====
@Serializable
data class Achievement(
    val name: String,
    val describe: String
)

data class ResultSectionUpdateRecipe(
    val achievement: Achievement
)
```

### 5.2 ResultPackage — 报告包管理器

```kotlin
class ResultPackage(private val dataSource: ReportDataSource) {
    var badgeEarned = listOf<ResultSectionUpdateRecipe>()
    
    /**
     * 10 张报告卡片，按展示顺序排列
     * 注意: RS9（时光印记）插在 RS7（成就墙）之前
     */
    val resultSections: List<ResultSection> = listOf(
        ResultSection0(),  // 年度日历热力图 + 报告哈希
        ResultSection1(),  // 年度总览
        ResultSection2(),  // 编程语言
        ResultSection3(),  // 提交时间段
        ResultSection4(),  // 高频词汇
        ResultSection5(),  // 空行统计
        ResultSection6(),  // 特别的一天
        ResultSection9(),  // 时光印记（热力图 + 月度柱状图）
        ResultSection7(),  // 成就墙
        ResultSection8(),  // 尾页
    )
    
    fun update() {
        // 遍历所有卡片，计算数据并收集成就
        badgeEarned = resultSections
            .mapNotNull { it.update(dataSource) }
        
        // 将成就列表传递给成就墙卡片
        resultSections.filterIsInstance<ResultSectionBadgeData>()
            .forEach { it.setBadge(badgeEarned) }
    }
}

// ===== 报告卡片协议 =====
interface ResultSection {
    /**
     * 根据分析数据更新卡片内容
     * @return 如果触发了成就，返回对应的 Recipe
     */
    fun update(scannerResult: ReportDataSource): ResultSectionUpdateRecipe?
    
    /**
     * 生成展示用的 Composable
     */
    @Composable
    fun CardContent()
    
    /**
     * 生成截图用的 Composable（无动画版本）
     */
    @Composable
    fun ScreenShotContent()
}

interface ResultSectionBadgeData {
    fun setBadge(badges: List<ResultSectionUpdateRecipe>)
}
```

### 5.3 报告卡片详细设计

#### 5.3.1 RS0 — 年度日历热力图 + 报告唯一哈希

```
┌──────────────────────────────────────────────────┐
│                                            🔀    │
│  ┌─ CodeTiles ──────────┐                        │
│  │  ███ ██ █ ██ ███     │  2025 年               │
│  │  ██ ███ █████ ██     │  我和我的代码，还有这一年。│
│  │  ███ ████ ██ ███     │  ─────────────────      │
│  │  █ ████ ██████       │  校验码: 0xABCD1234EFGH │
│  │  ████ ██ ██ ██       │                        │
│  └──────────────────────┘                        │
└──────────────────────────────────────────────────┘
```

**数据计算**:
- 日历热力图: 统计每个 `dayOfYear` 的提交次数
- 报告哈希: 对所有 diff 的数据进行 SHA256，取后 16 位

```kotlin
class ResultSection0 : ResultSection {
    var reportHash = ""
    var dailyCommits = mapOf<Int, Int>()  // dayOfYear → count
    
    override fun update(scannerResult: ReportDataSource): ResultSectionUpdateRecipe? {
        // 统计每日提交数
        val daily = mutableMapOf<Int, Int>()
        val hashSeeds = mutableListOf<ULong>()
        
        for (repo in scannerResult.repoResult) {
            for (commit in repo.commits) {
                val dayOfYear = commit.date.dayOfYear
                daily[dayOfYear] = (daily[dayOfYear] ?: 0) + 1
                hashSeeds.add(dayOfYear.toULong() * 114514u)
                
                for (diff in commit.diffFiles) {
                    hashSeeds.add(diff.increasedLine.toULong())
                    hashSeeds.add(diff.decreasedLine.toULong())
                    hashSeeds.add(diff.emptyLineAdded.toULong())
                }
            }
        }
        dailyCommits = daily
        
        // 哈希计算
        reportHash = hashSeeds.sorted()
            .joinToString("")
            .sha256()
            .uppercase()
            .takeLast(16)
        
        return null  // RS0 不触发成就
    }
}
```

#### 5.3.2 RS1 — 年度总览

```
┌──────────────────────────────────────────────────┐
│  在 2025 年                                       │
│  拼搏 / 尝试 / 探索 是我今年的代言词。               │
│                                                  │
│  这一年里，我总共进行了  1,234  次代码提交。          │
│  感谢我的仓库们，他们记录着我生活的点点滴滴。         │
│                                                  │
│  提交记录告诉咱：                                  │
│  仓库因你增添了  45,678  行代码，也减去了  12,345  行│
│  的重量。                                         │
│                                                  │
│  回过头来看看这一年，咱一共卷了  256  天。            │
│  风雨兼程，目的地是我向往的星辰大海。🥺              │
└──────────────────────────────────────────────────┘
```

**成就触发逻辑**:
- 全年每天都有提交 → "全勤战士"

**文案选择逻辑**:
| 总提交数 | 代言词 | 描述风格 |
|---------|--------|---------|
| < 0 (黑客) | 黑客 | 幽默讽刺 |
| > 1000 | 拼搏 | "咱一共卷了 X 天" |
| > 365 | 尝试 | "似乎付出了不少" |
| > 50 | 探索 | "星星有月亮" |
| ≤ 50 | — | "每一次都心意满满" |

#### 5.3.3 RS2 — 编程语言统计

**数据计算**: 统计所有文件差异中各语言的增删行数，按行数排序

#### 5.3.4 RS3 — 提交时间段统计

```kotlin
enum class CommitTimeOfDay(val displayName: String) {
    MIDNIGHT("凌晨"),    // 0:00 - 5:00
    MORNING("早晨"),     // 5:00 - 10:00
    NOON("中午"),        // 10:00 - 14:00
    AFTERNOON("下午"),   // 14:00 - 17:00
    DINNER("晚餐时间"),  // 17:00 - 19:00
    NIGHT("晚上");       // 19:00 - 24:00
}
```

**成就触发**: 根据最常提交时间段触发对应成就（夜猫子 / 早起 / 干饭人等）

#### 5.3.5 RS4 — 高频词汇

展示代码和提交信息中最常用的单词，使用 `DictionaryBuilder` 的统计结果。

**成就触发**: 检查高频词是否包含脏话 → "文明语言大师" / "文明语言学者"

#### 5.3.6 RS5 — 空行统计

统计全年新增的空行总数。

**成就触发**: 空行 > 233333 → "摸鱼流量百分百"

#### 5.3.7 RS6 — 特别的一天

找出全年提交次数最多的那一天。

**成就触发**:
- 单日提交 > 50 → "Bugfeature 制造机"
- 单日提交 > 100 → "我是奥特曼"

#### 5.3.8 RS9 — 时光印记

热力图 + 每月代码量柱状图的组合卡片。

#### 5.3.9 RS7 — 成就墙

展示所有收集到的成就徽章。实现 `ResultSectionBadgeData` 接口接收成就列表。

#### 5.3.10 RS8 — 尾页

二维码 + 版权信息 + 项目链接。

### 5.4 ResultView — 报告展示页

#### 5.4.1 UI 布局

```
┌──────────────────────────────────────────────────┐
│  ↓ 向下滑动开启报告 ↓                              │
│                                                  │
│  ┌──────────────────────────────────────┐        │
│  │          RS0: 年度日历热力图          │        │
│  │          (width × 0.9, height × 0.9)  │       │
│  └──────────────────────────────────────┘        │
│                  spacing: 25dp                    │
│  ┌──────────────────────────────────────┐        │
│  │          RS1: 年度总览               │        │
│  └──────────────────────────────────────┘        │
│                                                  │
│  ... RS2 ~ RS8 ...                               │
│                                                  │
│  [导出分析数据] [生成截图] [打印] [重新开始]         │
└──────────────────────────────────────────────────┘
```

#### 5.4.2 CMP Compose 实现

```kotlin
@Composable
fun ResultView(
    resultPackage: ResultPackage,
    onReset: () -> Unit
) {
    val scrollState = rememberLazyListState()
    
    LazyColumn(
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Spacer(Modifier.height(30.dp))
            Text(
                text = "↓ 向下滑动开启报告 ↓",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.alpha(0.5f)
            )
            Spacer(Modifier.height(10.dp))
        }
        
        // 报告卡片
        itemsIndexed(resultPackage.resultSections) { index, section ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(
                        preferredApplicationSize.width / preferredApplicationSize.height
                    )
                    .shadow(4.dp, RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                section.CardContent()
            }
            Spacer(Modifier.height(25.dp))
        }
        
        // 底部操作按钮
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { exportReport(resultPackage) }) {
                    Text("导出分析数据")
                }
                Button(onClick = { captureScreenshot(resultPackage) }) {
                    Text("生成截图")
                }
                Button(onClick = { printReport(resultPackage) }) {
                    Text("打印")
                }
                Button(onClick = { onReset() }) {
                    Text("重新开始")
                }
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}
```

#### 5.4.3 导出功能

```kotlin
/**
 * 三种导出方式:
 * 1. 导出分析数据 → JSON 文件 (.mygitreport)
 * 2. 生成截图 → PNG 图片
 * 3. 打印 → PDF 文档
 */

// expect/actual: 导出抽象
expect class ReportExporter {
    /**
     * 导出 JSON 数据
     */
    suspend fun exportJson(data: ReportDataSource, fileName: String): Result<Unit>
    
    /**
     * 导出 PNG 截图
     * 将所有卡片渲染为一张长图
     */
    suspend fun exportPng(
        sections: List<ResultSection>,
        fileName: String
    ): Result<Unit>
    
    /**
     * 导出 PDF
     * 每张卡片一页，横向排列
     */
    suspend fun exportPdf(
        sections: List<ResultSection>,
        fileName: String
    ): Result<Unit>
}
```

### 5.5 卡片尺寸规格

| 属性 | 值 | CMP 实现 |
|------|-----|---------|
| 卡片宽度 | 窗口宽度 × 0.9 | `Modifier.fillMaxWidth(0.9f)` |
| 卡片高度 | 窗口高度 × 0.9 | `Modifier.aspectRatio(16f/10f)` |
| 卡片间距 | 25dp | `Spacer(Modifier.height(25.dp))` |
| 内边距 | 50dp | `Modifier.padding(50.dp)` |
| 标题字号 | 24sp | `MaterialTheme.typography.headlineSmall` |
| 正文字号 | 12sp | `MaterialTheme.typography.bodySmall` |
| 数字高亮 | 24sp + Color.Blue | `fontSize = 24.sp, color = Color.Blue` |
| 行高 | 30dp | `Modifier.height(30.dp)` |

### 5.6 热力图组件实现

```kotlin
/**
 * GitHub 风格的年度提交热力图
 * 53 列（周） × 7 行（天）
 * 颜色通过提交数量映射到 5 级色阶
 */
@Composable
fun HeatmapView(
    dailyCommits: Map<Int, Int>,  // dayOfYear → count
    modifier: Modifier = Modifier
) {
    val maxCommits = dailyCommits.values.maxOrNull() ?: 1
    val columns = 53
    val rows = 7
    val cellSize = 10.dp
    val cellSpacing = 2.dp
    
    Canvas(
        modifier = modifier.size(
            width = (cellSize + cellSpacing) * columns,
            height = (cellSize + cellSpacing) * rows
        )
    ) {
        for (col in 0 until columns) {
            for (row in 0 until rows) {
                val dayIndex = col * 7 + row + 1
                val count = dailyCommits[dayIndex] ?: 0
                val intensity = count.toFloat() / maxCommits
                
                val color = when {
                    count == 0 -> Color(0xFFEBEDF0)
                    intensity < 0.25f -> Color(0xFF9BE9A8)
                    intensity < 0.50f -> Color(0xFF40C463)
                    intensity < 0.75f -> Color(0xFF30A14E)
                    else -> Color(0xFF216E39)
                }
                
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        x = col * (cellSize + cellSpacing).toPx(),
                        y = row * (cellSize + cellSpacing).toPx()
                    ),
                    size = Size(cellSize.toPx(), cellSize.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}
```

---

## 六、模块间接口定义汇总

### 6.1 expect/actual 接口清单

| 接口 | commonMain | desktopMain | androidMain | iosMain |
|------|-----------|------------|------------|---------|
| `GitExecutor` | 协议定义 | ProcessBuilder | JGit | 服务端 API / libgit2 |
| `DirectoryPicker` | 协议定义 | JFileChooser | SAF | UIDocumentPicker |
| `ReportExporter` | 协议定义 | ImageIO + iText | Canvas + ContentResolver | UIGraphics + FileManager |
| `UserSettings` | 协议定义 | java.util.prefs | DataStore | NSUserDefaults |
| `createTempDir()` | 协议定义 | File.createTempFile | Context.cacheDir | NSTemporaryDirectory |
| `fileExists()` | 协议定义 | File.exists() | File.exists() | NSFileManager |
| `parseGitDate()` | 协议定义 | SimpleDateFormat | SimpleDateFormat | NSDateFormatter |

### 6.2 数据流向图

```
用户配置 (MainSheet)
    │
    ▼
SourcePackage
    ├─ tempDir: String
    └─ representedObjects: List<SourceRegistrationData>
            │
            ▼
    AnalysisView (进度 UI)
            │
    ┌───────┴───────┐
    │ RepoAnalyser  │
    │  ├ GitLog      │ → List<GitLogElement>
    │  ├ GitDiff     │ → List<GitFileDiff>
    │  └ DictBuilder │ → Map<String, Int> × 3
    └───────┬───────┘
            │
            ▼
    ReportDataSource
    ├─ repoResult: List<GitRepoResult>
    ├─ dictionaryIncrease: Map<String, Int>
    ├─ dictionaryDecrease: Map<String, Int>
    └─ dictionaryCommit: Map<String, Int>
            │
            ▼
    ResultPackage
    ├─ RS0~RS9: 10 张报告卡片各自计算
    ├─ badgeEarned: 成就列表
    └─ representedData: ReportDataSource (可序列化导出)
            │
            ▼
    ResultView (报告展示)
    ├─ 卡片滚动列表
    ├─ 导出 JSON / PNG / PDF
    └─ 重新开始
```

---

## 七、开发优先级与任务拆解

### Phase 1: 核心数据层 (2 周)

| 任务 | 优先级 | 依赖 | 估时 |
|------|-------|------|------|
| 定义所有 Kotlin 数据模型 | P0 | 无 | 1d |
| 实现 GitLog 解析器 + 单元测试 | P0 | 数据模型 | 2d |
| 实现 GitDiff 解析器 + 状态机 + 单元测试 | P0 | 数据模型 | 3d |
| 实现 DictionaryBuilder + 驼峰拆分 | P0 | 无 | 1d |
| 实现 SourceLanguage 语言识别 | P1 | 无 | 0.5d |
| 实现 CommitFileFilter 过滤器 | P1 | 无 | 1d |
| 实现日期解析器 (expect/actual) | P0 | 无 | 0.5d |
| 实现 GitExecutor (Desktop) | P0 | 无 | 1d |

### Phase 2: 文件选择与数据源 (1.5 周)

| 任务 | 优先级 | 依赖 | 估时 |
|------|-------|------|------|
| 实现 DirectoryPicker (Desktop) | P0 | 无 | 1d |
| 实现仓库搜索算法 | P0 | DirectoryPicker | 1d |
| 实现 LocalRepoSheet UI | P0 | 搜索算法 | 1.5d |
| 实现 MainSheet 配置面板 UI | P0 | LocalRepoSheet | 2d |
| 实现 PickSourceSheet | P1 | 无 | 0.5d |
| 实现 SourcePackage 构建 | P0 | MainSheet | 0.5d |

### Phase 3: 分析引擎集成 (1.5 周)

| 任务 | 优先级 | 依赖 | 估时 |
|------|-------|------|------|
| 实现 RepoAnalyser 会话管理 | P0 | Phase 1 | 1d |
| 实现本地仓库分析流程 | P0 | RepoAnalyser | 2d |
| 实现 AnalysisView 进度 UI | P0 | 分析流程 | 1d |
| 实现 NavigatorView 路由 | P0 | 所有页面 | 1d |
| 并发调优与错误处理 | P1 | 分析流程 | 1d |

### Phase 4: 报告 UI (2.5 周)

| 任务 | 优先级 | 依赖 | 估时 |
|------|-------|------|------|
| 实现 ResultPackage 框架 | P0 | 数据模型 | 1d |
| 实现 HeatmapView 热力图 | P0 | 无 | 1.5d |
| 实现 RS0 日历热力图卡片 | P0 | HeatmapView | 1d |
| 实现 RS1 年度总览卡片 | P0 | 无 | 1d |
| 实现 RS2 编程语言卡片 | P1 | 无 | 1d |
| 实现 RS3-RS6 统计卡片 | P1 | 无 | 2d |
| 实现 RS7 成就墙 | P1 | 所有成就逻辑 | 1d |
| 实现 RS8 尾页 | P2 | 无 | 0.5d |
| 实现 RS9 时光印记 | P1 | HeatmapView | 1d |
| 实现 ResultView 滚动列表 | P0 | 所有卡片 | 1d |
| 实现导出功能 (JSON/PNG/PDF) | P1 | ResultView | 2d |
| 实现打字机效果组件 | P1 | 无 | 0.5d |

---

## 八、关键技术决策

### 8.1 多线程策略

| 原项目 | CMP 方案 |
|--------|---------|
| `DispatchQueue.concurrent` + `DispatchSemaphore` | `coroutineScope` + `Semaphore(processCount)` |
| `NSLock` | `Mutex` (kotlinx.coroutines) |
| `DispatchGroup` | `awaitAll()` |

```kotlin
// 并发度控制
val processCount = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)
```

### 8.2 内存管理

- **词典内存限制**: 65535 条目上限，逐级裁剪低频词
- **大型仓库**: 仅复制 `.git` 目录，避免 `node_modules` 等
- **临时文件**: 分析完立即删除

### 8.3 错误恢复

- git diff 失败 → 跳过该 commit，继续下一个
- git clone 超时 → 跳过该仓库
- 日期解析失败 → 跳过该 commit

---

*文档版本: 1.0*
*最后更新: 2026-02-10*
