package com.classroomassistant.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.classroomassistant.android.model.LocalKwsModelManager
import com.classroomassistant.android.platform.AndroidAudioRecorder
import com.classroomassistant.android.platform.AndroidPreferences
import com.classroomassistant.android.platform.AndroidSecureStorage
import com.classroomassistant.android.platform.AndroidStorage
import com.classroomassistant.android.speech.AndroidWakeWordEngine
import com.classroomassistant.android.ui.SettingsData
import com.classroomassistant.android.ui.SettingsScreen
import com.classroomassistant.android.ui.theme.ClassroomAssistantTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val KEY_KEYWORDS = "user.keywords"
private const val KEY_AUDIO_LOOKBACK_SECONDS = "audio.lookbackSeconds"
private const val KEY_VAD_ENABLED = "vad.enabled"
private const val KEY_VAD_QUIET_THRESHOLD_SECONDS = "vad.quietThresholdSeconds"
private const val KEY_AI_PROVIDER = "ai.provider"
private const val KEY_AI_MODEL_NAME = "ai.modelName"
private const val KEY_AI_TOKEN = "ai.token.encrypted"
private const val KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled"
private const val KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays"
private const val KEY_SPEECH_API_KEY = "speech.apiKey.encrypted"
private const val KEY_KWS_MODEL_ID = "speech.kws.model.id"
private const val KEY_KWS_MODEL_SELECTED = "speech.kws.model.selected"

private val aiProviders = setOf("OPENAI", "QIANFAN", "DEEPSEEK", "KIMI")
private val logTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun formatLogLine(message: String): String {
    val time = LocalDateTime.now().format(logTimeFormatter)
    return "[$time] $message"
}

private fun loadSettings(
    preferences: AndroidPreferences,
    secureStorage: AndroidSecureStorage
): SettingsData {
    val keywords = preferences.getString(KEY_KEYWORDS, "") ?: ""
    val lookbackSeconds = preferences.getInt(KEY_AUDIO_LOOKBACK_SECONDS, 240).coerceIn(60, 300)
    val vadEnabled = preferences.getBoolean(KEY_VAD_ENABLED, true)
    val quietThreshold = preferences.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5).coerceIn(3, 30)
    val recordingSaveEnabled = preferences.getBoolean(KEY_RECORDING_SAVE_ENABLED, false)
    val retentionDays = preferences.getInt(KEY_RECORDING_RETENTION_DAYS, 7).coerceIn(0, 30)
    val providerRaw = preferences.getString(KEY_AI_PROVIDER, "QIANFAN") ?: "QIANFAN"
    val aiProvider = if (aiProviders.contains(providerRaw)) providerRaw else "QIANFAN"
    val modelName = preferences.getString(KEY_AI_MODEL_NAME, "") ?: ""
    val apiToken = secureStorage.retrieveSecure(KEY_AI_TOKEN) ?: ""
    val speechApiKey = secureStorage.retrieveSecure(KEY_SPEECH_API_KEY) ?: ""
    return SettingsData(
        keywords = keywords,
        vadEnabled = vadEnabled,
        quietThreshold = quietThreshold,
        lookbackSeconds = lookbackSeconds,
        recordingSaveEnabled = recordingSaveEnabled,
        retentionDays = retentionDays,
        aiProvider = aiProvider,
        modelName = modelName,
        apiToken = apiToken,
        speechApiKey = speechApiKey
    )
}

private fun saveSettings(
    preferences: AndroidPreferences,
    secureStorage: AndroidSecureStorage,
    settings: SettingsData
) {
    preferences.putString(KEY_KEYWORDS, settings.keywords.trim())
    preferences.putInt(KEY_AUDIO_LOOKBACK_SECONDS, settings.lookbackSeconds)
    preferences.putBoolean(KEY_VAD_ENABLED, settings.vadEnabled)
    preferences.putInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, settings.quietThreshold)
    preferences.putBoolean(KEY_RECORDING_SAVE_ENABLED, settings.recordingSaveEnabled)
    preferences.putInt(KEY_RECORDING_RETENTION_DAYS, settings.retentionDays)
    preferences.putString(KEY_AI_PROVIDER, settings.aiProvider)
    preferences.putString(KEY_AI_MODEL_NAME, settings.modelName)
    secureStorage.storeSecure(KEY_AI_TOKEN, settings.apiToken)
    secureStorage.storeSecure(KEY_SPEECH_API_KEY, settings.speechApiKey)
    preferences.flush()
}

private fun parseKeywords(raw: String): List<String> {
    if (raw.isBlank()) {
        return emptyList()
    }
    return raw.split(",", "，", " ", "；", ";", "\n", "\t")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/**
 * Android 主界面
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予
        } else {
            Toast.makeText(this, "需要麦克风权限才能使用语音功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ClassroomAssistantTheme {
                AppNavigation(
                    onRequestAudioPermission = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(onRequestAudioPermission: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferences = remember { AndroidPreferences(context.applicationContext) }
    val storage = remember { AndroidStorage(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val modelManager = remember { LocalKwsModelManager(context.applicationContext, storage) }
    val availableModels = remember { modelManager.getAvailableModels() }
    val defaultModelId = remember { modelManager.getDefaultModelId() }
    val savedModelId = remember { preferences.getString(KEY_KWS_MODEL_ID, defaultModelId) ?: defaultModelId }
    var currentModelId by remember {
        mutableStateOf(if (availableModels.any { it.id == savedModelId }) savedModelId else defaultModelId)
    }
    val savedSelectedRaw = remember { preferences.getString(KEY_KWS_MODEL_SELECTED, "") ?: "" }
    val savedSelected = remember {
        savedSelectedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    var selectedModelIds by remember {
        mutableStateOf(
            (savedSelected + currentModelId).filter { id -> availableModels.any { it.id == id } }.toSet()
        )
    }
    var modelStatusMap by remember {
        mutableStateOf(
            availableModels.associate { option ->
                option.id to if (modelManager.isModelReady(option.id)) "已就绪" else "未下载"
            }
        )
    }
    var modelProgressMap by remember {
        mutableStateOf(availableModels.associate { it.id to 0f })
    }
    var modelFailureMap by remember {
        mutableStateOf(availableModels.associate { it.id to "" })
    }
    var currentModelReady by remember { mutableStateOf(modelManager.isModelReady(currentModelId)) }
    var isDownloading by remember { mutableStateOf(false) }
    var secureStorage by remember { mutableStateOf<AndroidSecureStorage?>(null) }
    var settings by remember { mutableStateOf<SettingsData?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var logVersion by remember { mutableStateOf(0) }
    var logText by remember { mutableStateOf("日志输出...") }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onRequestAudioPermission()
        }
    }

    fun appendLog(message: String) {
        val line = formatLogLine(message)
        mainHandler.post {
            logText = if (logText == "日志输出..." || logText.isBlank()) {
                line
            } else {
                val combined = "$logText\n$line"
                val lines = combined.split("\n")
                if (lines.size > 500) {
                    lines.takeLast(500).joinToString("\n")
                } else {
                    combined
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            val logFile = File(storage.getLogsDir(), "app.log")
            logFile.parentFile?.mkdirs()
            logFile.appendText("$line\n")
        }
    }

    fun clearLogs() {
        coroutineScope.launch(Dispatchers.IO) {
            val logFile = File(storage.getLogsDir(), "app.log")
            if (logFile.exists()) {
                logFile.writeText("")
            }
        }
        logText = "日志已清空"
    }

    fun persistSelectedModels() {
        preferences.putString(KEY_KWS_MODEL_SELECTED, selectedModelIds.joinToString(","))
        preferences.flush()
    }

    fun updateModelStatus(modelId: String, status: String, progress: Float = 0f) {
        modelStatusMap = modelStatusMap.toMutableMap().apply { put(modelId, status) }
        modelProgressMap = modelProgressMap.toMutableMap().apply { put(modelId, progress) }
        if (modelId == currentModelId) {
            currentModelReady = modelManager.isModelReady(modelId)
        }
    }

    fun refreshModelStatus() {
        modelStatusMap = availableModels.associate { option ->
            option.id to if (modelManager.isModelReady(option.id)) "已就绪" else "未下载"
        }
        modelProgressMap = modelProgressMap.toMutableMap().apply {
            availableModels.forEach { option ->
                if (modelManager.isModelReady(option.id)) {
                    put(option.id, 1f)
                }
            }
        }
        currentModelReady = modelManager.isModelReady(currentModelId)
    }

    fun updateCurrentModel(modelId: String) {
        currentModelId = modelId
        preferences.putString(KEY_KWS_MODEL_ID, modelId)
        preferences.flush()
        currentModelReady = modelManager.isModelReady(modelId)
        appendLog("切换当前模型: ${availableModels.firstOrNull { it.id == modelId }?.name ?: modelId}")
    }

    fun toggleModelSelection(modelId: String, isChecked: Boolean) {
        selectedModelIds = if (isChecked) {
            selectedModelIds + modelId
        } else {
            selectedModelIds - modelId
        }
        persistSelectedModels()
        appendLog("选择模型下载: ${availableModels.firstOrNull { it.id == modelId }?.name ?: modelId}, 选中: $isChecked")
    }

    fun buildFailureReason(error: Throwable?): String {
        val message = error?.message?.takeIf { it.isNotBlank() }
        return message ?: error?.javaClass?.simpleName ?: "未知错误"
    }

    fun downloadSelectedModels() {
        if (isDownloading) {
            appendLog("当前有下载任务，稍后再试")
            return
        }
        val targets = selectedModelIds.toList()
        if (targets.isEmpty()) {
            appendLog("未选择任何模型")
            return
        }
        isDownloading = true
        coroutineScope.launch(Dispatchers.IO) {
            for (modelId in targets) {
                val modelName = availableModels.firstOrNull { it.id == modelId }?.name ?: modelId
                if (modelManager.isModelReady(modelId)) {
                    mainHandler.post { updateModelStatus(modelId, "已就绪", 1f) }
                    appendLog("模型已存在: $modelName")
                    continue
                }
                mainHandler.post {
                    updateModelStatus(modelId, "下载中", 0f)
                    modelFailureMap = modelFailureMap.toMutableMap().apply { put(modelId, "") }
                }
                appendLog("开始下载模型: $modelName")
                val result = modelManager.downloadAndPrepare(modelId) { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    mainHandler.post {
                        val status = if (total > 0) "下载中 ${(progress * 100).toInt()}%" else "下载中"
                        updateModelStatus(modelId, status, progress)
                    }
                }
                mainHandler.post {
                    if (result.isSuccess) {
                        updateModelStatus(modelId, "已就绪", 1f)
                        modelFailureMap = modelFailureMap.toMutableMap().apply { put(modelId, "") }
                    } else {
                        updateModelStatus(modelId, "失败", 0f)
                        modelFailureMap = modelFailureMap.toMutableMap().apply {
                            put(modelId, buildFailureReason(result.exceptionOrNull()))
                        }
                    }
                }
                if (result.isSuccess) {
                    appendLog("模型下载成功: $modelName")
                } else {
                    appendLog("模型下载失败: $modelName, 原因: ${buildFailureReason(result.exceptionOrNull())}")
                }
            }
            mainHandler.post { isDownloading = false }
        }
    }

    fun ensureModelReady(
        modelId: String,
        onReady: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (modelManager.isModelReady(modelId)) {
            updateModelStatus(modelId, "已就绪", 1f)
            currentModelReady = true
            onReady()
            return
        }
        if (isDownloading) {
            return
        }
        isDownloading = true
        val modelName = availableModels.firstOrNull { it.id == modelId }?.name ?: modelId
        appendLog("开始下载模型: $modelName")
        coroutineScope.launch(Dispatchers.IO) {
            mainHandler.post {
                updateModelStatus(modelId, "下载中", 0f)
                modelFailureMap = modelFailureMap.toMutableMap().apply { put(modelId, "") }
            }
            val result = modelManager.downloadAndPrepare(modelId) { downloaded, total ->
                val progress = if (total > 0) downloaded.toFloat() / total else 0f
                mainHandler.post {
                    val status = if (total > 0) "下载中 ${(progress * 100).toInt()}%" else "下载中"
                    updateModelStatus(modelId, status, progress)
                }
            }
            withContext(Dispatchers.Main) {
                isDownloading = false
                if (result.isSuccess) {
                    updateModelStatus(modelId, "已就绪", 1f)
                    modelFailureMap = modelFailureMap.toMutableMap().apply { put(modelId, "") }
                    currentModelReady = true
                    appendLog("模型已就绪: $modelName")
                    onReady()
                } else {
                    updateModelStatus(modelId, "失败", 0f)
                    val reason = buildFailureReason(result.exceptionOrNull())
                    modelFailureMap = modelFailureMap.toMutableMap().apply { put(modelId, reason) }
                    appendLog("模型下载失败: $modelName, 原因: $reason")
                    onFailure(reason)
                }
            }
        }
    }

    LaunchedEffect(reloadKey) {
        loadError = null
        try {
            val storageInstance = withContext(Dispatchers.IO) {
                AndroidSecureStorage(context.applicationContext)
            }
            secureStorage = storageInstance
            settings = withContext(Dispatchers.IO) {
                loadSettings(preferences, storageInstance)
            }
        } catch (e: Exception) {
            loadError = e.message ?: "初始化失败"
        }
    }

    LaunchedEffect(logVersion) {
        withContext(Dispatchers.IO) {
            val logFile = File(storage.getLogsDir(), "app.log")
            val text = if (logFile.exists()) {
                val lines = logFile.readLines()
                val tail = if (lines.size > 500) lines.takeLast(500) else lines
                tail.joinToString("\n")
            } else {
                ""
            }
            withContext(Dispatchers.Main) {
                logText = if (text.isBlank()) "日志输出..." else text
            }
        }
    }

    LaunchedEffect(Unit) {
        val migrated = withContext(Dispatchers.IO) {
            modelManager.migrateLegacyModelIfNeeded(currentModelId)
        }
        if (migrated) {
            refreshModelStatus()
        }
    }

    val currentSettings = settings
    val currentSecureStorage = secureStorage
    if (currentSettings == null || currentSecureStorage == null) {
        LoadingScreen(
            message = loadError,
            onRetry = { reloadKey += 1 }
        )
        return
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    appendLog("打开设置")
                    logVersion += 1
                    navController.navigate("settings")
                },
                settings = currentSettings,
                logText = logText,
                onClearLogs = { clearLogs() },
                currentModelName = availableModels.firstOrNull { it.id == currentModelId }?.name ?: currentModelId,
                currentModelStatus = modelStatusMap[currentModelId] ?: "未下载",
                currentModelReady = currentModelReady,
                currentModelFailureReason = modelFailureMap[currentModelId].orEmpty(),
                currentModelProgress = modelProgressMap[currentModelId] ?: 0f,
                onEnsureModelReady = { onReady, onFailure ->
                    ensureModelReady(currentModelId, onReady, onFailure)
                },
                onAppendLog = { appendLog(it) },
                modelDir = modelManager.getModelDir(currentModelId)
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                initialSettings = currentSettings,
                onSave = { updatedSettings ->
                    saveSettings(preferences, currentSecureStorage, updatedSettings)
                    settings = updatedSettings
                    appendLog("保存设置成功")
                    logVersion += 1
                    navController.popBackStack()
                },
                modelOptions = availableModels,
                currentModelId = currentModelId,
                selectedModelIds = selectedModelIds,
                modelStatusMap = modelStatusMap,
                modelProgressMap = modelProgressMap,
                modelFailureMap = modelFailureMap,
                isDownloading = isDownloading,
                onCurrentModelChange = { updateCurrentModel(it) },
                onToggleModelSelection = { id, checked -> toggleModelSelection(id, checked) },
                onDownloadSelected = { downloadSelectedModels() },
                onRefreshModelStatus = { refreshModelStatus() }
            )
        }
    }
}

@Composable
private fun LoadingScreen(message: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(message ?: "正在初始化…")
            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    settings: SettingsData,
    logText: String,
    onClearLogs: () -> Unit,
    currentModelName: String,
    currentModelStatus: String,
    currentModelReady: Boolean,
    currentModelFailureReason: String,
    currentModelProgress: Float,
    onEnsureModelReady: (onReady: () -> Unit, onFailure: (String) -> Unit) -> Unit,
    onAppendLog: (String) -> Unit,
    modelDir: File
) {
    val context = LocalContext.current
    var isMonitoring by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("空闲") }
    val latestSettings by rememberUpdatedState(settings)
    val audioRecorder = remember { AndroidAudioRecorder(context.applicationContext) }
    val wakeWordEngine = remember { AndroidWakeWordEngine(audioRecorder) }

    fun startWakeWordDetection() {
        val keywords = parseKeywords(latestSettings.keywords)
        if (keywords.isEmpty()) {
            onAppendLog("未配置唤醒词，使用空列表启动")
        }
        val started = wakeWordEngine.start(
            modelDir = modelDir,
            keywords = keywords,
            onWake = {
                statusText = "已唤醒"
                onAppendLog("检测到唤醒词")
            },
            onLog = onAppendLog
        )
        if (!started) {
            statusText = "空闲"
            isMonitoring = false
            onAppendLog("启动唤醒失败")
        } else {
            onAppendLog("启动唤醒成功")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            wakeWordEngine.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课堂助手") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "当前状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isMonitoring) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "当前模型：$currentModelName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "模型状态：$currentModelStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentModelStatus == "失败" && currentModelFailureReason.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "失败原因：$currentModelFailureReason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (!currentModelReady && currentModelProgress > 0f) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { currentModelProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(24.dp))

            // 控制按钮
            Button(
                onClick = {
                    isMonitoring = !isMonitoring
                    if (isMonitoring) {
                        statusText = "监听中..."
                        onAppendLog("开始本地监听，唤醒词: ${latestSettings.keywords.ifBlank { "未设置" }}")
                        if (!currentModelReady) {
                            onAppendLog("当前模型未就绪，准备下载")
                            statusText = "下载模型中..."
                        }
                        onEnsureModelReady(
                            { startWakeWordDetection() },
                            { reason ->
                                statusText = "空闲"
                                isMonitoring = false
                                onAppendLog("模型下载失败，原因: $reason")
                            }
                        )
                    } else {
                        statusText = "空闲"
                        onAppendLog("停止监听")
                        wakeWordEngine.stop()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isMonitoring) "停止监听" else "开始监听",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 日志区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "运行日志",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(
                            onClick = { onClearLogs() }
                        ) {
                            Text("清空日志")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = logText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 设置按钮
            OutlinedButton(
                onClick = {
                    onAppendLog("点击设置")
                    onNavigateToSettings()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("设置")
            }
        }
    }
}
