package com.classroomassistant.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.classroomassistant.android.model.LocalKwsModelManager

/**
 * Android 设置页面
 *
 * @param onNavigateBack 返回上一页的回调
 * @param onSave 保存设置的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    initialSettings: SettingsData,
    onSave: (SettingsData) -> Unit,
    modelOptions: List<LocalKwsModelManager.KwsModelOption>,
    currentModelId: String,
    selectedModelIds: Set<String>,
    modelStatusMap: Map<String, String>,
    modelProgressMap: Map<String, Float>,
    modelFailureMap: Map<String, String>,
    isDownloading: Boolean,
    onCurrentModelChange: (String) -> Unit,
    onToggleModelSelection: (String, Boolean) -> Unit,
    onDownloadSelected: () -> Unit,
    onRefreshModelStatus: () -> Unit
) {
    var keywords by remember { mutableStateOf(initialSettings.keywords) }
    var vadEnabled by remember { mutableStateOf(initialSettings.vadEnabled) }
    var quietThreshold by remember { mutableStateOf(initialSettings.quietThreshold) }
    var lookbackSeconds by remember { mutableStateOf(initialSettings.lookbackSeconds) }
    var recordingSaveEnabled by remember { mutableStateOf(initialSettings.recordingSaveEnabled) }
    var retentionDays by remember { mutableStateOf(initialSettings.retentionDays) }
    var aiProvider by remember { mutableStateOf(initialSettings.aiProvider) }
    var modelName by remember { mutableStateOf(initialSettings.modelName) }
    var apiToken by remember { mutableStateOf(initialSettings.apiToken) }
    var speechApiKey by remember { mutableStateOf(initialSettings.speechApiKey) }
    var infoDialogModel by remember { mutableStateOf<LocalKwsModelManager.KwsModelOption?>(null) }

    LaunchedEffect(initialSettings) {
        keywords = initialSettings.keywords
        vadEnabled = initialSettings.vadEnabled
        quietThreshold = initialSettings.quietThreshold
        lookbackSeconds = initialSettings.lookbackSeconds
        recordingSaveEnabled = initialSettings.recordingSaveEnabled
        retentionDays = initialSettings.retentionDays
        aiProvider = initialSettings.aiProvider
        modelName = initialSettings.modelName
        apiToken = initialSettings.apiToken
        speechApiKey = initialSettings.speechApiKey
    }

    if (infoDialogModel != null) {
        AlertDialog(
            onDismissRequest = { infoDialogModel = null },
            title = { Text(infoDialogModel?.name ?: "模型说明") },
            text = { Text(infoDialogModel?.description ?: "暂无说明") },
            confirmButton = {
                TextButton(onClick = { infoDialogModel = null }) {
                    Text("知道了")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 唤醒词设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "唤醒词",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "姓名/变体（逗号分隔）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("例如：张三,张三同学") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // VAD 设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "安静检测（VAD）",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用安静检测")
                        Switch(
                            checked = vadEnabled,
                            onCheckedChange = { vadEnabled = it }
                        )
                    }
                    if (vadEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "阈值：${quietThreshold} 秒",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = quietThreshold.toFloat(),
                            onValueChange = { quietThreshold = it.toInt() },
                            valueRange = 3f..30f,
                            steps = 26
                        )
                    }
                }
            }

            // 语音回溯设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "语音回溯",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "回溯秒数：${lookbackSeconds} 秒",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = lookbackSeconds.toFloat(),
                        onValueChange = { lookbackSeconds = it.toInt() },
                        valueRange = 60f..300f,
                        steps = 23
                    )
                }
            }

            // 录音保存设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "录音保存",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("保存触发录音（WAV）")
                        Switch(
                            checked = recordingSaveEnabled,
                            onCheckedChange = { recordingSaveEnabled = it }
                        )
                    }
                    if (recordingSaveEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "保留天数：${retentionDays} 天",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = retentionDays.toFloat(),
                            onValueChange = { retentionDays = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                    }
                }
            }

            // 语音识别 API 设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "语音识别（云端 API）",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "使用 Groq Whisper 等云端服务进行语音识别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = speechApiKey,
                        onValueChange = { speechApiKey = it },
                        label = { Text("Speech API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // AI 问答设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI 问答",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 模型平台选择
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = aiProvider,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("模型平台") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("OPENAI", "QIANFAN", "DEEPSEEK", "KIMI").forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider) },
                                    onClick = {
                                        aiProvider = provider
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("模型名称（可选）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("API Token / Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "模型管理",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "本地唤醒模型下载与选择",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = modelOptions.firstOrNull { it.id == currentModelId }?.name ?: currentModelId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("当前模型") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            modelOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        expanded = false
                                        onCurrentModelChange(option.id)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modelOptions.forEach { option ->
                            val checked = selectedModelIds.contains(option.id)
                            val status = modelStatusMap[option.id] ?: "未下载"
                            val progress = modelProgressMap[option.id] ?: 0f
                            val failureReason = modelFailureMap[option.id].orEmpty()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        onToggleModelSelection(option.id, isChecked)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { infoDialogModel = option }) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "模型说明"
                                            )
                                        }
                                    }
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (status.startsWith("下载中") && progress > 0f) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (status == "失败" && failureReason.isNotBlank()) {
                                        Text(
                                            text = "原因：$failureReason",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onDownloadSelected() },
                            enabled = !isDownloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("下载选中模型")
                        }
                        OutlinedButton(
                            onClick = { onRefreshModelStatus() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("刷新状态")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = {
                    onSave(
                        SettingsData(
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
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("保存设置", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * 设置数据类
 */
data class SettingsData(
    val keywords: String,
    val vadEnabled: Boolean,
    val quietThreshold: Int,
    val lookbackSeconds: Int,
    val recordingSaveEnabled: Boolean,
    val retentionDays: Int,
    val aiProvider: String,
    val modelName: String,
    val apiToken: String,
    val speechApiKey: String
)
