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
import com.classroomassistant.android.model.LocalAsrModelManager
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
    onRefreshModelStatus: () -> Unit,
    asrModelOptions: List<LocalAsrModelManager.AsrModelOption>,
    asrCurrentModelId: String,
    asrModelStatusMap: Map<String, String>,
    asrModelProgressMap: Map<String, Float>,
    asrModelFailureMap: Map<String, String>,
    isAsrDownloading: Boolean,
    onDownloadAsrModel: (String) -> Unit,
    onRefreshAsrModelStatus: () -> Unit
) {
    var keywords by remember { mutableStateOf(initialSettings.keywords) }
    var kwsThreshold by remember { mutableStateOf(initialSettings.kwsThreshold) }
    var vadEnabled by remember { mutableStateOf(initialSettings.vadEnabled) }
    var quietThreshold by remember { mutableStateOf(initialSettings.quietThreshold) }
    var quietAlertMode by remember { mutableStateOf(initialSettings.quietAlertMode) }
    var quietAutoLookbackEnabled by remember { mutableStateOf(initialSettings.quietAutoLookbackEnabled) }
    var quietAutoLookbackExtraSeconds by remember { mutableStateOf(initialSettings.quietAutoLookbackExtraSeconds) }
    var lookbackSeconds by remember { mutableStateOf(initialSettings.lookbackSeconds) }
    var recordingSaveEnabled by remember { mutableStateOf(initialSettings.recordingSaveEnabled) }
    var retentionDays by remember { mutableStateOf(initialSettings.retentionDays) }
    var aiProvider by remember { mutableStateOf(initialSettings.aiProvider) }
    var modelName by remember { mutableStateOf(initialSettings.modelName) }
    var apiToken by remember { mutableStateOf(initialSettings.apiToken) }
    var apiSecretKey by remember { mutableStateOf(initialSettings.apiSecretKey) }
    var speechApiKey by remember { mutableStateOf(initialSettings.speechApiKey) }
    var localAsrEnabled by remember { mutableStateOf(initialSettings.localAsrEnabled) }
    var localAsrModelId by remember { mutableStateOf(initialSettings.localAsrModelId.ifBlank { asrCurrentModelId }) }
    var cloudWhisperEnabled by remember { mutableStateOf(initialSettings.cloudWhisperEnabled) }
    var wakeAlertMode by remember { mutableStateOf(initialSettings.wakeAlertMode) }
    var logMode by remember { mutableStateOf(initialSettings.logMode) }
    var showDiagnosticLogs by remember { mutableStateOf(initialSettings.showDiagnosticLogs) }
    var showAudioDeviceLogs by remember { mutableStateOf(initialSettings.showAudioDeviceLogs) }
    var showGainActivityLogs by remember { mutableStateOf(initialSettings.showGainActivityLogs) }
    var showTtsSelfTestLogs by remember { mutableStateOf(initialSettings.showTtsSelfTestLogs) }
    var showHeartbeatLogs by remember { mutableStateOf(initialSettings.showHeartbeatLogs) }
    var ttsSelfTestEnabled by remember { mutableStateOf(initialSettings.ttsSelfTestEnabled) }
    var backgroundKeepAliveEnabled by remember { mutableStateOf(initialSettings.backgroundKeepAliveEnabled) }
    var infoDialogModel by remember { mutableStateOf<LocalKwsModelManager.KwsModelOption?>(null) }
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.surface,
        uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
        uncheckedBorderColor = MaterialTheme.colorScheme.primary
    )
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primary,
        unselectedColor = MaterialTheme.colorScheme.primary
    )

    LaunchedEffect(initialSettings) {
        keywords = initialSettings.keywords
        kwsThreshold = initialSettings.kwsThreshold
        vadEnabled = initialSettings.vadEnabled
        quietThreshold = initialSettings.quietThreshold
        quietAlertMode = initialSettings.quietAlertMode
        quietAutoLookbackEnabled = initialSettings.quietAutoLookbackEnabled
        quietAutoLookbackExtraSeconds = initialSettings.quietAutoLookbackExtraSeconds
        lookbackSeconds = initialSettings.lookbackSeconds
        recordingSaveEnabled = initialSettings.recordingSaveEnabled
        retentionDays = initialSettings.retentionDays
        aiProvider = initialSettings.aiProvider
        modelName = initialSettings.modelName
        apiToken = initialSettings.apiToken
        apiSecretKey = initialSettings.apiSecretKey
        speechApiKey = initialSettings.speechApiKey
        localAsrEnabled = initialSettings.localAsrEnabled
        localAsrModelId = initialSettings.localAsrModelId.ifBlank { asrCurrentModelId }
        cloudWhisperEnabled = initialSettings.cloudWhisperEnabled
        wakeAlertMode = initialSettings.wakeAlertMode
        logMode = initialSettings.logMode
        showDiagnosticLogs = initialSettings.showDiagnosticLogs
        showAudioDeviceLogs = initialSettings.showAudioDeviceLogs
        showGainActivityLogs = initialSettings.showGainActivityLogs
        showTtsSelfTestLogs = initialSettings.showTtsSelfTestLogs
        showHeartbeatLogs = initialSettings.showHeartbeatLogs
        ttsSelfTestEnabled = initialSettings.ttsSelfTestEnabled
        backgroundKeepAliveEnabled = initialSettings.backgroundKeepAliveEnabled
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

    val saveSettingsAction = {
        onSave(
            SettingsData(
                keywords = keywords,
                kwsThreshold = kwsThreshold,
                vadEnabled = vadEnabled,
                quietThreshold = quietThreshold,
                quietAlertMode = quietAlertMode,
                quietAutoLookbackEnabled = quietAutoLookbackEnabled,
                quietAutoLookbackExtraSeconds = quietAutoLookbackExtraSeconds,
                lookbackSeconds = lookbackSeconds,
                recordingSaveEnabled = recordingSaveEnabled,
                retentionDays = retentionDays,
                aiProvider = aiProvider,
                modelName = modelName,
                apiToken = apiToken,
                apiSecretKey = apiSecretKey,
                speechApiKey = speechApiKey,
                localAsrEnabled = localAsrEnabled,
                localAsrModelId = localAsrModelId,
                cloudWhisperEnabled = cloudWhisperEnabled,
                wakeAlertMode = wakeAlertMode,
                logMode = logMode,
                showDiagnosticLogs = showDiagnosticLogs,
                showAudioDeviceLogs = showAudioDeviceLogs,
                showGainActivityLogs = showGainActivityLogs,
                showTtsSelfTestLogs = showTtsSelfTestLogs,
                showHeartbeatLogs = showHeartbeatLogs,
                ttsSelfTestEnabled = ttsSelfTestEnabled,
                backgroundKeepAliveEnabled = backgroundKeepAliveEnabled
            )
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
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { saveSettingsAction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("保存设置", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
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
                        text = "支持多个唤醒词（支持中文逗号“，”和英文逗号“,”）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("例如：陈慧萍，小张,张老师") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "中文词条长度≥3时，会自动把后两个字加入候选（如“张三四”自动追加“三四”）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "唤醒词触发值：${String.format("%.2f", kwsThreshold)}（推荐 0.25）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = kwsThreshold,
                        onValueChange = { kwsThreshold = it },
                        valueRange = 0.05f..0.8f,
                        steps = 14
                    )
                    Text(
                        text = "说明：值越低越灵敏、越高越稳健；推荐先用 0.25，再按环境微调。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "唤醒提示",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "检测到唤醒词时的手机提醒方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("仅系统通知")
                        RadioButton(
                            selected = wakeAlertMode != "SOUND",
                            onClick = { wakeAlertMode = "NOTIFICATION_ONLY" },
                            colors = radioColors
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("系统通知 + 提示音")
                        RadioButton(
                            selected = wakeAlertMode == "SOUND",
                            onClick = { wakeAlertMode = "SOUND" },
                            colors = radioColors
                        )
                    }
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
                            onCheckedChange = { vadEnabled = it },
                            colors = switchColors
                        )
                    }
                    if (vadEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "阈值：${quietThreshold} 秒（推荐 5 秒）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = quietThreshold.toFloat(),
                            onValueChange = { quietThreshold = it.toInt() },
                            valueRange = 3f..30f,
                            steps = 26
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "安静超时提醒方式",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("仅系统通知")
                            RadioButton(
                                selected = quietAlertMode != "SOUND",
                                onClick = { quietAlertMode = "NOTIFICATION_ONLY" },
                                colors = radioColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("系统通知 + 提示音")
                            RadioButton(
                                selected = quietAlertMode == "SOUND",
                                onClick = { quietAlertMode = "SOUND" },
                                colors = radioColors
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("安静超时后自动扩展回溯")
                            Switch(
                                checked = quietAutoLookbackEnabled,
                                onCheckedChange = { quietAutoLookbackEnabled = it },
                                colors = switchColors
                            )
                        }
                        if (quietAutoLookbackEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "额外回溯：${quietAutoLookbackExtraSeconds} 秒（默认 8 秒）",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = quietAutoLookbackExtraSeconds.toFloat(),
                                onValueChange = { quietAutoLookbackExtraSeconds = it.toInt() },
                                valueRange = 1f..60f,
                                steps = 58
                            )
                            Text(
                                text = "生效规则：实际回溯会按“安静时长 + 额外秒数”动态扩大（且不小于语音回溯设置）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "说明：关闭后仍会按“语音回溯”中的固定秒数回溯；仅不再因安静超时自动加长回溯。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        text = "回溯秒数：${lookbackSeconds} 秒（推荐 12~20 秒）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = lookbackSeconds.toFloat(),
                        onValueChange = { lookbackSeconds = it.toInt() },
                        valueRange = 8f..120f,
                        steps = 0
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "后台保活",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("监听时启用后台保活")
                        Switch(
                            checked = backgroundKeepAliveEnabled,
                            onCheckedChange = { backgroundKeepAliveEnabled = it },
                            colors = switchColors
                        )
                    }
                    Text(
                        text = "开启后会显示持续通知，降低切到桌面后被系统回收导致监听中断的概率。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onCheckedChange = { recordingSaveEnabled = it },
                            colors = switchColors
                        )
                    }
                    if (recordingSaveEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "保留天数：${retentionDays} 天（推荐 7 天）",
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
                    val modelNameOptions = remember(aiProvider) {
                        when (aiProvider.uppercase()) {
                            "OPENAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini")
                            "DEEPSEEK" -> listOf("deepseek-chat", "deepseek-reasoner")
                            "KIMI" -> listOf("moonshot-v1-8k", "moonshot-v1-32k")
                            "QIANFAN" -> listOf("ernie-4.0-8k", "ernie-3.5-8k")
                            else -> emptyList()
                        }
                    }
                    var modelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            label = { Text("模型名称（可选）") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            modelNameOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        modelName = option
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiToken,
                        onValueChange = { apiToken = it },
                        label = { Text("API Token / Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (aiProvider.uppercase() == "QIANFAN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = apiSecretKey,
                            onValueChange = { apiSecretKey = it },
                            label = { Text("Secret Key（千帆，可选）") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 语音识别 API 设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "语音识别路线",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用本机ASR模型")
                        Switch(
                            checked = localAsrEnabled,
                            onCheckedChange = { localAsrEnabled = it },
                            colors = switchColors
                        )
                    }

                    if (localAsrEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "本机模型（默认推荐中英双语快速模型）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var asrExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = asrExpanded,
                            onExpandedChange = { asrExpanded = !asrExpanded }
                        ) {
                            OutlinedTextField(
                                value = asrModelOptions.firstOrNull { it.id == localAsrModelId }?.name ?: localAsrModelId,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("本机ASR模型") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = asrExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = asrExpanded,
                                onDismissRequest = { asrExpanded = false }
                            ) {
                                asrModelOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.name) },
                                        onClick = {
                                            localAsrModelId = option.id
                                            asrExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val asrStatus = asrModelStatusMap[localAsrModelId] ?: "未下载"
                        Text(
                            text = "模型状态：$asrStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (asrStatus.startsWith("下载中")) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { asrModelProgressMap[localAsrModelId] ?: 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        val asrFailureReason = asrModelFailureMap[localAsrModelId].orEmpty()
                        if (asrStatus == "失败" && asrFailureReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "原因：$asrFailureReason",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onDownloadAsrModel(localAsrModelId) },
                                enabled = !isAsrDownloading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("下载本机模型")
                            }
                            OutlinedButton(
                                onClick = { onRefreshAsrModelStatus() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("刷新状态")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用云端 Whisper")
                        Switch(
                            checked = cloudWhisperEnabled,
                            onCheckedChange = { cloudWhisperEnabled = it },
                            colors = switchColors
                        )
                    }
                    Text(
                        text = "说明：启用后将优先走云端Whisper路线；默认关闭。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cloudWhisperEnabled) {
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "开发者选项",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "普通用户建议保持“简洁日志”；开发调试可切换“全面日志”并按分类开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("简洁日志")
                        RadioButton(
                            selected = logMode != "FULL",
                            onClick = { logMode = "SIMPLE" },
                            colors = radioColors
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("全面日志")
                        RadioButton(
                            selected = logMode == "FULL",
                            onClick = { logMode = "FULL" },
                            colors = radioColors
                        )
                    }

                    if (logMode == "FULL") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("监听诊断日志")
                            Switch(
                                checked = showDiagnosticLogs,
                                onCheckedChange = { showDiagnosticLogs = it },
                                colors = switchColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("音频设备日志")
                            Switch(
                                checked = showAudioDeviceLogs,
                                onCheckedChange = { showAudioDeviceLogs = it },
                                colors = switchColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("增益/语音活动日志")
                            Switch(
                                checked = showGainActivityLogs,
                                onCheckedChange = { showGainActivityLogs = it },
                                colors = switchColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TTS 自测日志")
                            Switch(
                                checked = showTtsSelfTestLogs,
                                onCheckedChange = { showTtsSelfTestLogs = it },
                                colors = switchColors
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("心跳日志（正在监听）")
                            Switch(
                                checked = showHeartbeatLogs,
                                onCheckedChange = { showHeartbeatLogs = it },
                                colors = switchColors
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用 TTS 自测")
                        Switch(
                            checked = ttsSelfTestEnabled,
                            onCheckedChange = { ttsSelfTestEnabled = it },
                            colors = switchColors
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设置数据类
 */
data class SettingsData(
    val keywords: String,
    val kwsThreshold: Float,
    val vadEnabled: Boolean,
    val quietThreshold: Int,
    val quietAlertMode: String,
    val quietAutoLookbackEnabled: Boolean,
    val quietAutoLookbackExtraSeconds: Int,
    val lookbackSeconds: Int,
    val recordingSaveEnabled: Boolean,
    val retentionDays: Int,
    val aiProvider: String,
    val modelName: String,
    val apiToken: String,
    val apiSecretKey: String,
    val speechApiKey: String,
    val localAsrEnabled: Boolean,
    val localAsrModelId: String,
    val cloudWhisperEnabled: Boolean,
    val wakeAlertMode: String,
    val logMode: String,
    val showDiagnosticLogs: Boolean,
    val showAudioDeviceLogs: Boolean,
    val showGainActivityLogs: Boolean,
    val showTtsSelfTestLogs: Boolean,
    val showHeartbeatLogs: Boolean,
    val ttsSelfTestEnabled: Boolean,
    val backgroundKeepAliveEnabled: Boolean
)
