package com.classroomassistant.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.classroomassistant.android.model.LocalAsrModelManager
import com.classroomassistant.android.model.LocalKwsModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    onRefreshAsrModelStatus: () -> Unit,
    onCheckUpdate: () -> Unit,
    updateStatusMessage: String
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
    var aiBaseUrl by remember { mutableStateOf(initialSettings.aiBaseUrl) }
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
    var infoDialogAsrModel by remember { mutableStateOf<LocalAsrModelManager.AsrModelOption?>(null) }
    var showAsrModelCatalogDialog by remember { mutableStateOf(false) }
    var showAiUsageDialog by remember { mutableStateOf(false) }
    var showSettingsGuideDialog by remember { mutableStateOf(false) }
    var appLanguageSelection by remember { mutableStateOf(initialSettings.appLanguage) }
    val providerOptions = remember {
        listOf(
            "OPENAI",
            "OPENAI_COMPATIBLE",
            "ANTHROPIC",
            "GEMINI",
            "DEEPSEEK",
            "QIANFAN",
            "DASHSCOPE",
            "HUNYUAN",
            "ZHIPU",
            "KIMI",
            "GROQ",
            "MISTRAL",
            "COHERE",
            "OPENROUTER",
            "AZURE_OPENAI",
            "SILICONFLOW",
            "MINIMAX",
            "BAICHUAN",
            "YI",
            "STEPFUN",
            "XAI",
            "FIREWORKS",
            "TOGETHER_AI",
            "PERPLEXITY",
            "NOVITA",
            "REPLICATE",
            "CEREBRAS",
            "SAMBANOVA",
            "OLLAMA",
            "LMSTUDIO"
        )
    }
    val providerDisplayNames = remember {
        mapOf(
            "OPENAI" to "OpenAI 官方",
            "OPENAI_COMPATIBLE" to "OpenAI 兼容 / 中转站",
            "ANTHROPIC" to "Anthropic",
            "GEMINI" to "Google Gemini",
            "DEEPSEEK" to "DeepSeek",
            "QIANFAN" to "百度千帆",
            "DASHSCOPE" to "阿里百炼 (DashScope)",
            "HUNYUAN" to "腾讯混元",
            "ZHIPU" to "智谱 AI",
            "KIMI" to "Kimi (Moonshot)",
            "GROQ" to "Groq",
            "MISTRAL" to "Mistral",
            "COHERE" to "Cohere",
            "OPENROUTER" to "OpenRouter",
            "AZURE_OPENAI" to "Azure OpenAI",
            "SILICONFLOW" to "硅基流动",
            "MINIMAX" to "MiniMax",
            "BAICHUAN" to "百川智能",
            "YI" to "零一万物 Yi",
            "STEPFUN" to "阶跃星辰 StepFun",
            "XAI" to "xAI",
            "FIREWORKS" to "Fireworks AI",
            "TOGETHER_AI" to "Together AI",
            "PERPLEXITY" to "Perplexity",
            "NOVITA" to "Novita",
            "REPLICATE" to "Replicate",
            "CEREBRAS" to "Cerebras",
            "SAMBANOVA" to "SambaNova",
            "OLLAMA" to "Ollama（本地）",
            "LMSTUDIO" to "LM Studio（本地）"
        )
    }
    val modelSuggestionsByProvider = remember {
        mapOf(
            "OPENAI_COMPATIBLE" to listOf("gpt-4o-mini", "deepseek-chat", "qwen-plus"),
            "OPENAI" to listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
            "ANTHROPIC" to listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"),
            "GEMINI" to listOf("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash"),
            "DEEPSEEK" to listOf("deepseek-chat", "deepseek-reasoner"),
            "KIMI" to listOf("moonshot-v1-8k", "moonshot-v1-32k"),
            "QIANFAN" to listOf("ernie-4.0-8k", "ernie-3.5-8k"),
            "DASHSCOPE" to listOf("qwen-plus", "qwen-max", "qwen-turbo"),
            "HUNYUAN" to listOf("hunyuan-lite", "hunyuan-standard", "hunyuan-pro"),
            "ZHIPU" to listOf("glm-4-flash", "glm-4-plus", "glm-4-air"),
            "SILICONFLOW" to listOf("Qwen/Qwen2.5-7B-Instruct", "deepseek-ai/DeepSeek-V3"),
            "MINIMAX" to listOf("abab6.5s-chat", "abab6.5t-chat"),
            "MISTRAL" to listOf("mistral-small-latest", "mistral-large-latest"),
            "GROQ" to listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            "COHERE" to listOf("command-r-plus", "command-r", "command-light"),
            "OPENROUTER" to listOf("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001"),
            "AZURE_OPENAI" to listOf("gpt-4o", "gpt-4o-mini", "o3-mini"),
            "BAICHUAN" to listOf("Baichuan4", "Baichuan3-Turbo", "Baichuan2-Turbo"),
            "YI" to listOf("yi-large", "yi-medium", "yi-lightning"),
            "STEPFUN" to listOf("step-2", "step-1v-8k", "step-1-8k"),
            "XAI" to listOf("grok-2-1212", "grok-beta", "grok-vision-beta"),
            "FIREWORKS" to listOf("accounts/fireworks/models/llama-v3p1-70b-instruct", "accounts/fireworks/models/qwen2p5-72b-instruct"),
            "TOGETHER_AI" to listOf("meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo", "Qwen/Qwen2.5-72B-Instruct-Turbo"),
            "PERPLEXITY" to listOf("sonar", "sonar-pro", "llama-3.1-sonar-large-128k-online"),
            "NOVITA" to listOf("meta-llama/llama-3.1-70b-instruct", "deepseek/deepseek-r1", "qwen/qwen2.5-72b-instruct"),
            "REPLICATE" to listOf("meta/meta-llama-3-70b-instruct", "mistralai/mistral-7b-instruct", "anthropic/claude-3-opus"),
            "CEREBRAS" to listOf("llama3.1-8b", "llama3.1-70b", "qwen-2.5-72b"),
            "SAMBANOVA" to listOf("Meta-Llama-3.1-405B-Instruct", "Meta-Llama-3.1-70B-Instruct", "Qwen2.5-72B-Instruct"),
            "OLLAMA" to listOf("qwen2.5:7b", "llama3.1:8b", "deepseek-r1:8b"),
            "LMSTUDIO" to listOf("local-model", "qwen2.5-7b-instruct", "llama-3.1-8b")
        )
    }
    val dynamicModelSuggestions = remember(aiProvider, aiBaseUrl, apiToken) {
        mutableStateListOf<String>()
    }
    val effectiveLanguage = remember(appLanguageSelection) {
        resolveEffectiveLanguage(appLanguageSelection)
    }
    fun t(zhText: String, enText: String): String = tr(effectiveLanguage, zhText, enText)
    fun providerLabel(provider: String): String {
        return when (provider) {
            "OPENAI" -> t("OpenAI 官方", "OpenAI Official")
            "OPENAI_COMPATIBLE" -> t("OpenAI 兼容 / 中转站", "OpenAI Compatible / Gateway")
            "QIANFAN" -> t("百度千帆", "Baidu Qianfan")
            "DASHSCOPE" -> t("阿里百炼 (DashScope)", "Alibaba DashScope")
            "HUNYUAN" -> t("腾讯混元", "Tencent Hunyuan")
            "ZHIPU" -> t("智谱 AI", "Zhipu AI")
            "SILICONFLOW" -> t("硅基流动", "SiliconFlow")
            "BAICHUAN" -> t("百川智能", "Baichuan")
            "YI" -> t("零一万物 Yi", "Yi")
            "STEPFUN" -> t("阶跃星辰 StepFun", "StepFun")
            "OLLAMA" -> t("Ollama（本地）", "Ollama (Local)")
            "LMSTUDIO" -> t("LM Studio（本地）", "LM Studio (Local)")
            else -> providerDisplayNames[provider] ?: provider
        }
    }
    var isFetchingModels by remember { mutableStateOf(false) }
    var modelFetchMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        checkedBorderColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline
    )
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primary,
        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        aiBaseUrl = initialSettings.aiBaseUrl
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
        appLanguageSelection = initialSettings.appLanguage
    }

    if (infoDialogModel != null) {
        val kwsOption = infoDialogModel
        AlertDialog(
            onDismissRequest = { infoDialogModel = null },
            title = { Text(kwsOption?.name ?: "模型说明") },
            text = {
                Text(
                    text = buildString {
                        append(kwsOption?.description ?: "暂无说明")
                        append("\n\n")
                        append("模型大小：")
                        append(kwsOption?.sizeLabel ?: "未标注")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialogModel = null }) {
                    Text("知道了")
                }
            }
        )
    }

    if (infoDialogAsrModel != null) {
        val asrOption = infoDialogAsrModel
        AlertDialog(
            onDismissRequest = { infoDialogAsrModel = null },
            title = { Text(asrOption?.name ?: "本机ASR模型说明") },
            text = {
                Text(
                    text = buildString {
                        append(asrOption?.description ?: "暂无说明")
                        append("\n\n")
                        append("模型大小：")
                        append(asrOption?.sizeLabel ?: "未标注")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { infoDialogAsrModel = null }) {
                    Text("知道了")
                }
            }
        )
    }

    LaunchedEffect(aiProvider) {
        val suggestions = modelSuggestionsByProvider[aiProvider].orEmpty()
        if (suggestions.isNotEmpty() && !suggestions.contains(modelName)) {
            modelName = suggestions.first()
        }
    }

    fun defaultBaseUrlFor(provider: String): String {
        return when (provider) {
            "OPENAI" -> "https://api.openai.com"
            "OPENAI_COMPATIBLE" -> ""
            "ANTHROPIC" -> "https://api.anthropic.com/v1"
            "GEMINI" -> "https://generativelanguage.googleapis.com/v1beta/openai"
            "DEEPSEEK" -> "https://api.deepseek.com"
            "KIMI" -> "https://api.moonshot.cn/v1"
            "QIANFAN" -> ""
            "DASHSCOPE" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "HUNYUAN" -> "https://api.hunyuan.cloud.tencent.com/v1"
            "ZHIPU" -> "https://open.bigmodel.cn/api/paas/v4"
            "SILICONFLOW" -> "https://api.siliconflow.cn/v1"
            "MINIMAX" -> "https://api.minimax.chat/v1"
            "MISTRAL" -> "https://api.mistral.ai/v1"
            "GROQ" -> "https://api.groq.com/openai/v1"
            "OPENROUTER" -> "https://openrouter.ai/api/v1"
            "COHERE" -> "https://api.cohere.ai/compatibility/v1"
            "AZURE_OPENAI" -> ""
            "BAICHUAN" -> "https://api.baichuan-ai.com/v1"
            "YI" -> "https://api.lingyiwanwu.com/v1"
            "STEPFUN" -> "https://api.stepfun.com/v1"
            "XAI" -> "https://api.x.ai/v1"
            "FIREWORKS" -> "https://api.fireworks.ai/inference/v1"
            "TOGETHER_AI" -> "https://api.together.xyz/v1"
            "PERPLEXITY" -> "https://api.perplexity.ai"
            "NOVITA" -> "https://api.novita.ai/v3/openai"
            "REPLICATE" -> "https://api.replicate.com/v1"
            "CEREBRAS" -> "https://api.cerebras.ai/v1"
            "SAMBANOVA" -> "https://api.sambanova.ai/v1"
            "OLLAMA" -> "http://127.0.0.1:11434/v1"
            "LMSTUDIO" -> "http://127.0.0.1:1234/v1"
            else -> ""
        }
    }

    fun normalizeBaseUrl(raw: String): String {
        var normalized = raw.trim()
        while (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }
        return normalized
    }

    fun applyProviderDefaultBaseUrlIfEmpty(nextProvider: String) {
        if (aiBaseUrl.isBlank()) {
            aiBaseUrl = defaultBaseUrlFor(nextProvider)
        }
    }

    suspend fun fetchModels(
        suggestions: SnapshotStateList<String>
    ) {
        val baseUrl = normalizeBaseUrl(aiBaseUrl)
        if (aiProvider == "QIANFAN") {
            modelFetchMessage = "千帆暂不支持 /v1/models 自动拉取"
            return
        }
        if (baseUrl.isBlank()) {
            modelFetchMessage = "请先填写 Base URL"
            return
        }
        isFetchingModels = true
        modelFetchMessage = "正在拉取模型列表..."
        val token = apiToken.trim()
        val result = withContext(Dispatchers.IO) {
            runCatching {
                com.classroomassistant.android.ai.AndroidAiAnswerService().fetchModelNames(
                    provider = aiProvider,
                    baseUrl = baseUrl,
                    apiToken = token,
                    onLog = {}
                )
            }
        }
        isFetchingModels = false
        result.onSuccess { fetched ->
            suggestions.clear()
            suggestions.addAll(fetched)
            if (fetched.isNotEmpty()) {
                if (modelName.isBlank() || !fetched.contains(modelName)) {
                    modelName = fetched.first()
                }
                modelFetchMessage = "已拉取模型 ${fetched.size} 个"
            } else {
                modelFetchMessage = "未获取到模型"
            }
        }.onFailure { e ->
            modelFetchMessage = "拉取失败: ${e.message ?: "未知错误"}"
        }
    }

    if (showAsrModelCatalogDialog) {
        AlertDialog(
            onDismissRequest = { showAsrModelCatalogDialog = false },
            title = { Text("本机ASR模型基础信息") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    asrModelOptions.forEach { option ->
                        Text(
                            text = buildString {
                                append(option.name)
                                append("\n")
                                append("大小：")
                                append(option.sizeLabel)
                                append("\n")
                                append(option.description)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAsrModelCatalogDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }

    if (showAiUsageDialog) {
        AlertDialog(
            onDismissRequest = { showAiUsageDialog = false },
            title = { Text(t("AI 问答使用说明", "AI Usage Guide")) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = t(
                            "1. 先在“模型平台”里选择你要使用的平台（如 OpenAI、DeepSeek、硅基流动等）。",
                            "1. Choose your provider first (for example OpenAI, DeepSeek, or others)."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "2. 打开对应平台官网创建 API Token / Key。平台通常在控制台或 API 管理页提供创建入口。",
                            "2. Create an API Token/Key on the provider console or API page."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "3. 将 Token 填入“API Token / Key”；若你使用百度千帆，还需要填写“API Secret”。",
                            "3. Fill in API Token/Key. For Qianfan, also fill API Secret."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "4. Base URL 一般保持默认；若使用中转站或私有网关，再填写对方提供的 Base URL。",
                            "4. Keep Base URL default unless your gateway provider gives a custom URL."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "5. 点击“拉取模型”，再在“模型名称”下拉列表中选择可用模型。",
                            "5. Tap Fetch Models, then choose or type a model name quickly."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "6. 保存设置后，主界面“AI 直接问答”即可手动提问；唤醒后的识别文本也会自动触发 AI 回答。",
                            "6. After saving, ask in Direct AI Q&A. Wake recognition also triggers AI reply automatically."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "提示：本说明可在“AI 问答”标题右侧 i 图标随时查看。",
                            "Tip: Open this guide any time from the info icon in AI settings."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiUsageDialog = false }) {
                    Text(t("知道了", "Got it"))
                }
            }
        )
    }

    if (showSettingsGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsGuideDialog = false },
            title = { Text(t("设置项使用说明", "Settings Usage Guide")) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = t(
                            "唤醒词：用于触发监听流程。课堂点名、提问前缀等场景建议设置多个词。",
                            "Wake words: trigger listening flow. Add multiple names for roll-call and Q&A cues."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "安静检测（VAD）：用于长时间安静时提醒并自动扩大回溯，适合怕漏听场景。",
                            "Silence detection (VAD): reminds on long silence and can auto-extend lookback."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "语音回溯：控制每次识别回看多少秒音频，课堂节奏快建议保持 20 秒以上。",
                            "Audio lookback: controls seconds of pre-wake audio for ASR, 20s+ is usually safer."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "语音识别路线：本机 ASR 更稳定、离线友好；云端 Whisper 识别质量更高但依赖网络。",
                            "ASR route: local ASR is offline-friendly; cloud Whisper may be better quality with network."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "AI 问答：用于识别后自动生成回答建议。先配置平台、密钥、模型，再保存生效。",
                            "AI Q&A: generates suggested answers after recognition. Configure provider/token/model then save."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "模型管理：当前模型用于实时监听；勾选列表用于提前下载缓存。",
                            "Model management: current model is used for live listening; checks are for batch downloading."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "开发者选项：默认简洁日志即可；出现问题再开启全面日志并按分类排查。",
                            "Developer options: keep simple logs by default; switch to full logs only for troubleshooting."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "录音保存：用于课后复盘，建议结合保留天数控制存储占用。",
                            "Recording retention: useful for post-class review, tune retention days for storage control."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = t(
                            "语言与更新：语言默认跟随系统；也可手动切换。更新支持自动检查和手动检查。",
                            "Language & update: language follows system by default and can be set manually."
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsGuideDialog = false }) {
                    Text(t("知道了", "Got it"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("设置", "Settings")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = t("返回", "Back"))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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
                        label = { Text("例如：小张,张老师,王同学") },
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
                        text = t(
                            "唤醒词触发值：${String.format("%.2f", kwsThreshold)}（默认 0.05）",
                            "Wake threshold: ${String.format(Locale.ROOT, "%.2f", kwsThreshold)} (default 0.05)"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = kwsThreshold,
                        onValueChange = { kwsThreshold = it },
                        valueRange = 0.05f..0.8f,
                        steps = 14
                    )
                    Text(
                        text = t(
                            "说明：值越低越灵敏、越高越稳健；默认值为 0.05，可按环境微调。",
                            "Tip: lower is more sensitive, higher is steadier. Start from default 0.05 and tune."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }

            item {
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
                            text = t(
                                "阈值：${quietThreshold} 秒（默认 5 秒）",
                                "Threshold: ${quietThreshold}s (default 5s)"
                            ),
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
                                text = t(
                                    "额外回溯：${quietAutoLookbackExtraSeconds} 秒（默认 20 秒）",
                                    "Extra lookback: ${quietAutoLookbackExtraSeconds}s (default 20s)"
                                ),
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
            }

            item {
                // 语音回溯设置
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "语音回溯",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t(
                            "回溯秒数：${lookbackSeconds} 秒（默认 20 秒）",
                            "Lookback seconds: ${lookbackSeconds}s (default 20s)"
                        ),
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
            }

            item {
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
            }

            item {
                // 语音识别 API 设置
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "语音识别路线",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showAsrModelCatalogDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "查看全部本机ASR模型信息"
                            )
                        }
                    }
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
                            text = t(
                                "本机模型（默认中文增强模型）",
                                "Local model (default: Chinese-enhanced model)"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val selectedAsrModel = asrModelOptions.firstOrNull { it.id == localAsrModelId }
                            IconButton(
                                onClick = {
                                    if (selectedAsrModel != null) {
                                        infoDialogAsrModel = selectedAsrModel
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "本机ASR模型说明"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                        if (isAsrDownloading && !asrStatus.startsWith("下载中")) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "当前有其他本机ASR模型在下载，点击下载会自动排队。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isSelectedAsrDownloading = asrStatus.startsWith("下载中")
                            Button(
                                onClick = { onDownloadAsrModel(localAsrModelId) },
                                enabled = !isSelectedAsrDownloading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isSelectedAsrDownloading) "下载中..." else "下载本机模型")
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
            }

            item {
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
            }

            item {
                // AI 问答设置
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI 问答",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { showAiUsageDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI 问答使用说明"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "使用方式：先选平台，再去官网获取 Token，填入后拉取并选择模型，最后保存设置。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var providerExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = providerExpanded,
                        onExpandedChange = { providerExpanded = !providerExpanded }
                    ) {
                        OutlinedTextField(
                            value = providerLabel(aiProvider),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("模型平台") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {
                            providerOptions.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(providerLabel(provider)) },
                                    onClick = {
                                        aiProvider = provider
                                        applyProviderDefaultBaseUrlIfEmpty(provider)
                                        providerExpanded = false
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
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiSecretKey,
                        onValueChange = { apiSecretKey = it },
                        label = { Text("API Secret（千帆必填）") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiBaseUrl,
                        onValueChange = { aiBaseUrl = it },
                        label = { Text("Base URL（平台默认可不填）") },
                        placeholder = { Text("如 https://api.openai.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                modelFetchMessage = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清空提示")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    fetchModels(dynamicModelSuggestions)
                                }
                            },
                            enabled = !isFetchingModels,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isFetchingModels) "拉取中..." else "拉取模型")
                        }
                    }
                    if (modelFetchMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = modelFetchMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val mergedModelSuggestions = remember(aiProvider, dynamicModelSuggestions.toList()) {
                        val staticList = modelSuggestionsByProvider[aiProvider].orEmpty()
                        (dynamicModelSuggestions + staticList).distinct()
                    }
                    LaunchedEffect(aiProvider, mergedModelSuggestions) {
                        if (mergedModelSuggestions.isNotEmpty() && !mergedModelSuggestions.contains(modelName)) {
                            modelName = mergedModelSuggestions.first()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (mergedModelSuggestions.isEmpty()) {
                        Text(
                            text = t(
                                "暂无可选模型：可直接手动输入模型名称，或先填写 Token 后点击“拉取模型”。",
                                "No model list yet. You can type model name directly, or fetch after filling token."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    var modelExpanded by remember { mutableStateOf(false) }
                    val filteredModelSuggestions = remember(modelName, mergedModelSuggestions) {
                        val query = modelName.trim().lowercase(Locale.ROOT)
                        if (query.isBlank()) {
                            mergedModelSuggestions
                        } else {
                            mergedModelSuggestions.filter { it.lowercase(Locale.ROOT).contains(query) }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded && filteredModelSuggestions.isNotEmpty(),
                        onExpandedChange = {
                            modelExpanded = !modelExpanded
                        }
                    ) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = {
                                modelName = it
                                modelExpanded = true
                            },
                            label = { Text(t("模型名称", "Model name")) },
                            placeholder = { Text(t("可输入或下拉选择", "Type or choose from dropdown")) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = modelExpanded && filteredModelSuggestions.isNotEmpty()
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded && filteredModelSuggestions.isNotEmpty(),
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            filteredModelSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        modelName = suggestion
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }

            item {
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "说明：监听时只使用“当前模型”；下方勾选仅用于批量下载缓存。",
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
            }

            item {
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

            item {
                // 录音保存设置
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "录音保存",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "说明：开启后会保存每次触发对应的回溯音频（WAV），用于课后回听与问题复盘。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
                            text = t(
                                "保留天数：${retentionDays} 天（默认 7 天）",
                                "Retention: ${retentionDays} days (default 7)"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = retentionDays.toFloat(),
                            onValueChange = { retentionDays = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                        Text(
                            text = "保留时间越长，占用存储越多；建议按课堂复盘周期设置。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("使用说明", "Usage Guide"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t(
                            "汇总说明各配置项的作用和适用场景，首次使用建议先阅读。",
                            "Read this first for what each setting does and when to use it."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showSettingsGuideDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t("查看设置说明", "Open settings guide"))
                    }
                }
            }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = t("语言与更新", "Language & Update"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t(
                            "语言默认跟随系统，可手动切换。应用每次启动会自动检查更新。",
                            "Language follows system by default and can be changed manually."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    var languageExpanded by remember { mutableStateOf(false) }
                    val languageOptions = listOf(AppLanguage.AUTO, AppLanguage.ZH_CN, AppLanguage.EN_US)
                    val languageLabel = when (appLanguageSelection) {
                        AppLanguage.AUTO -> t("跟随系统", "Follow system")
                        AppLanguage.ZH_CN -> "中文"
                        AppLanguage.EN_US -> "English"
                    }
                    ExposedDropdownMenuBox(
                        expanded = languageExpanded,
                        onExpandedChange = { languageExpanded = !languageExpanded }
                    ) {
                        OutlinedTextField(
                            value = languageLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(t("界面语言", "App language")) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false }
                        ) {
                            languageOptions.forEach { option ->
                                val optionLabel = when (option) {
                                    AppLanguage.AUTO -> t("跟随系统", "Follow system")
                                    AppLanguage.ZH_CN -> "中文"
                                    AppLanguage.EN_US -> "English"
                                }
                                DropdownMenuItem(
                                    text = { Text(optionLabel) },
                                    onClick = {
                                        appLanguageSelection = option
                                        languageExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onCheckUpdate() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(t("检查更新", "Check for updates"))
                    }
                    if (updateStatusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                // 保存按钮
                Button(
                onClick = {
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
                            aiBaseUrl = aiBaseUrl,
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
                            backgroundKeepAliveEnabled = backgroundKeepAliveEnabled,
                            appLanguage = appLanguageSelection
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(t("保存设置", "Save settings"), style = MaterialTheme.typography.titleMedium)
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
    val aiBaseUrl: String,
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
    val backgroundKeepAliveEnabled: Boolean,
    val appLanguage: AppLanguage
)
