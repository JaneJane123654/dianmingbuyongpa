package com.classroomassistant.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.classroomassistant.android.ai.AndroidAiAnswerService
import com.classroomassistant.android.model.LocalAsrModelManager
import com.classroomassistant.android.model.LocalKwsModelManager
import com.classroomassistant.android.platform.AndroidAudioRecorder
import com.classroomassistant.android.platform.AndroidPreferences
import com.classroomassistant.android.platform.AndroidSecureStorage
import com.classroomassistant.android.platform.AndroidStorage
import com.classroomassistant.android.platform.AppCrashMonitor
import com.classroomassistant.android.ui.AppLanguage
import com.classroomassistant.android.ui.resolveEffectiveLanguage
import com.classroomassistant.android.speech.AndroidSpeechRecognitionService
import com.classroomassistant.android.speech.AndroidWakeWordEngine
import com.classroomassistant.android.ui.SettingsData
import com.classroomassistant.android.ui.SettingsScreen
import com.classroomassistant.android.ui.tr
import com.classroomassistant.android.ui.theme.ClassroomAssistantTheme
import com.classroomassistant.core.audio.AudioFormatSpec
import com.classroomassistant.core.ai.PromptTemplate
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

private const val KEY_KEYWORDS = "user.keywords"
private const val KEY_AUDIO_LOOKBACK_SECONDS = "audio.lookbackSeconds"
private const val KEY_VAD_ENABLED = "vad.enabled"
private const val KEY_VAD_QUIET_THRESHOLD_SECONDS = "vad.quietThresholdSeconds"
private const val KEY_VAD_QUIET_ALERT_MODE = "vad.quietAlertMode"
private const val KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED = "vad.quietAutoLookbackEnabled"
private const val KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS = "vad.quietAutoLookbackExtraSeconds"
private const val KEY_AI_PROVIDER = "ai.provider"
private const val KEY_AI_MODEL_NAME = "ai.modelName"
private const val KEY_AI_BASE_URL = "ai.baseUrl"
private const val KEY_AI_SYSTEM_PROMPT = "ai.systemPrompt"
private const val KEY_AI_TOKEN = "ai.token.encrypted"
private const val KEY_AI_SECRET = "ai.secret.encrypted"
private const val KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled"
private const val KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays"
private const val KEY_SPEECH_API_KEY = "speech.apiKey.encrypted"
private const val KEY_ASR_LOCAL_ENABLED = "speech.asr.local.enabled"
private const val KEY_ASR_LOCAL_MODEL_ID = "speech.asr.local.model.id"
private const val KEY_ASR_CLOUD_WHISPER_ENABLED = "speech.asr.cloud.whisper.enabled"
private const val KEY_KWS_MODEL_ID = "speech.kws.model.id"
private const val KEY_KWS_MODEL_SELECTED = "speech.kws.model.selected"
private const val KEY_KWS_TRIGGER_THRESHOLD = "speech.kws.triggerThreshold"
private const val KEY_WAKE_ALERT_MODE = "speech.kws.wakeAlertMode"
private const val KEY_LOG_MODE = "developer.log.mode"
private const val KEY_LOG_SHOW_DIAGNOSTIC = "developer.log.showDiagnostic"
private const val KEY_LOG_SHOW_AUDIO_DEVICE = "developer.log.showAudioDevice"
private const val KEY_LOG_SHOW_GAIN_ACTIVITY = "developer.log.showGainActivity"
private const val KEY_LOG_SHOW_TTS_SELF_TEST = "developer.log.showTtsSelfTest"
private const val KEY_LOG_SHOW_HEARTBEAT = "developer.log.showHeartbeat"
private const val KEY_TTS_SELF_TEST_ENABLED = "developer.tts.selfTest.enabled"
private const val KEY_BACKGROUND_KEEPALIVE_ENABLED = "listening.backgroundKeepAliveEnabled"
private const val KEY_CUSTOM_MODEL_ENABLED = "model.custom.enabled"
private const val KEY_CUSTOM_KWS_MODEL_URL = "model.custom.kws.url"
private const val KEY_CUSTOM_ASR_MODEL_URL = "model.custom.asr.url"
private const val KEY_APP_LANGUAGE = "app.language"
private const val KEY_UPDATE_IGNORE_BEFORE_VERSION_CODE = "app.update.ignoreBeforeVersionCode"
private const val KEY_UPDATE_LAST_PROMPTED_VERSION_CODE = "app.update.lastPromptedVersionCode"
private const val KEY_PENDING_UPDATE_APK_PATH = "app.update.pendingApkPath"
private const val KEY_PENDING_UPDATE_APK_SAVED_AT = "app.update.pendingApkSavedAt"
private const val KEY_PENDING_UPDATE_VERSION_KEY = "app.update.pendingVersionKey"
private const val UPDATE_GITHUB_REPO = "JaneJane123654/dianmingbuyongpa"
private const val UPDATE_RELEASES_API_URL = "https://api.github.com/repos/$UPDATE_GITHUB_REPO/releases/latest"
private const val UPDATE_RELEASES_WEB_URL = "https://github.com/$UPDATE_GITHUB_REPO/releases"
private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
private const val NOTIFICATION_CHANNEL_WAKE_SILENT = "wake_alert_silent"
private const val NOTIFICATION_CHANNEL_WAKE_SOUND = "wake_alert_sound"
private const val NOTIFICATION_CHANNEL_QUIET_SILENT = "quiet_alert_silent"
private const val NOTIFICATION_CHANNEL_QUIET_SOUND = "quiet_alert_sound"
private const val NOTIFICATION_ID_WAKE = 11001
private const val NOTIFICATION_ID_QUIET = 11002
private const val WAKE_STATUS_DISPLAY_MS = 3_000L
private const val PENDING_UPDATE_APK_TTL_MS = 24L * 60L * 60L * 1000L
private const val VAD_SPEECH_RMS_THRESHOLD = 0.003f
private const val LOOKBACK_SECONDS_DEFAULT = 20
private const val LOOKBACK_SECONDS_MIN = 8
private const val LOOKBACK_SECONDS_MAX = 120

private val aiProviders = setOf(
    "OPENAI",
    "OPENAI_COMPATIBLE",
    "ANTHROPIC",
    "GEMINI",
    "QIANFAN",
    "DEEPSEEK",
    "KIMI",
    "DASHSCOPE",
    "HUNYUAN",
    "ZHIPU",
    "SILICONFLOW",
    "MINIMAX",
    "MISTRAL",
    "GROQ",
    "COHERE",
    "OPENROUTER",
    "AZURE_OPENAI",
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
private val logTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val wakeNotificationSequence = AtomicInteger(0)
private val quietNotificationSequence = AtomicInteger(0)
private const val DEBUG_LISTENING_HEARTBEAT_MS = 10_000L
private const val RELEASE_LISTENING_HEARTBEAT_MS = 10 * 60 * 1000L
private val simpleLogNoisePrefixes = setOf(
    "监听诊断:",
    "KWS 初始化阶段:",
    "KWS 配置:",
    "KWS 使用关键词文件:",
    "KWS 模型目录:",
    "文件检查[",
    "tokens.txt 词表大小:",
    "关键词文件:",
    "关键词预览:",
    "关键词自动扩展:",
    "自动追加词:",
    "未找到内置关键词文件",
    "会话统计:",
    "⚠ 监听期间检测到微弱语音",
    "检测结果: keyword=",
    "语音识别配置：",
    "回溯音频获取成功：",
    "识别耗时统计:",
    "唤醒事件：",
    "监听启动生效配置",
    "开始监听前配置检查",
    "说明：",
    "当前生效参数:",
    "VAD:",
    "模型下载[",
    "ASR下载[",
    "AI问答完成:"
)

private fun formatLogLine(message: String): String {
    val time = LocalDateTime.now().format(logTimeFormatter)
    return "[$time] $message"
}

private fun ensureWakeNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val silentChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_WAKE_SILENT,
        "唤醒提醒（静音）",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "检测到唤醒词时弹出系统通知（无提示音）"
        setSound(null, null)
    }

    val soundAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val soundChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_WAKE_SOUND,
        "唤醒提醒（含声音）",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "检测到唤醒词时弹出系统通知并播放提示音"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes)
    }

    manager.createNotificationChannel(silentChannel)
    manager.createNotificationChannel(soundChannel)

    val quietSilentChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_QUIET_SILENT,
        "安静检测提醒（静音）",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "安静超时后弹出系统通知（无提示音）"
        setSound(null, null)
    }

    val quietSoundChannel = NotificationChannel(
        NOTIFICATION_CHANNEL_QUIET_SOUND,
        "安静检测提醒（含声音）",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "安静超时后弹出系统通知并播放提示音"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), soundAttributes)
    }

    manager.createNotificationChannel(quietSilentChannel)
    manager.createNotificationChannel(quietSoundChannel)
}

private fun showWakeNotification(context: Context, alertMode: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    ensureWakeNotificationChannels(context)
    val channelId = if (alertMode == "SOUND") {
        NOTIFICATION_CHANNEL_WAKE_SOUND
    } else {
        NOTIFICATION_CHANNEL_WAKE_SILENT
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("课堂助手")
        .setContentText("已检测到唤醒词")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setWhen(System.currentTimeMillis())
        .setShowWhen(true)
        .build()
    val dynamicNotificationId = NOTIFICATION_ID_WAKE + wakeNotificationSequence.incrementAndGet()
    NotificationManagerCompat.from(context).notify(dynamicNotificationId, notification)
    return true
}

private fun showQuietNotification(context: Context, alertMode: String, quietSeconds: Int): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    ensureWakeNotificationChannels(context)
    val channelId = if (alertMode == "SOUND") {
        NOTIFICATION_CHANNEL_QUIET_SOUND
    } else {
        NOTIFICATION_CHANNEL_QUIET_SILENT
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("课堂助手")
        .setContentText("安静检测超时：已连续安静 ${quietSeconds} 秒")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setWhen(System.currentTimeMillis())
        .setShowWhen(true)
        .build()
    val dynamicNotificationId = NOTIFICATION_ID_QUIET + quietNotificationSequence.incrementAndGet()
    NotificationManagerCompat.from(context).notify(dynamicNotificationId, notification)
    return true
}

private data class UpdateReleaseAsset(
    val name: String,
    val downloadUrl: String
)

private data class UpdateReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val versionKey: Long,
    val apkAsset: UpdateReleaseAsset?
)

private fun parseVersionParts(rawVersion: String): List<Int> {
    return Regex("\\d+").findAll(rawVersion)
        .mapNotNull { match ->
            runCatching { match.value.toInt() }.getOrNull()
        }
        .toList()
}

private fun buildVersionKey(rawVersion: String): Long {
    val parts = parseVersionParts(rawVersion)
    if (parts.isEmpty()) {
        return 0L
    }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 1_000_000L + minor * 1_000L + patch.toLong()
}

private fun compareVersionNames(left: String, right: String): Int {
    val leftParts = parseVersionParts(left)
    val rightParts = parseVersionParts(right)
    val maxSize = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until maxSize) {
        val leftPart = leftParts.getOrElse(index) { 0 }
        val rightPart = rightParts.getOrElse(index) { 0 }
        if (leftPart != rightPart) {
            return leftPart.compareTo(rightPart)
        }
    }
    return 0
}

private fun currentAppVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    }.getOrDefault("0.0.0")
}

private fun parseLatestReleaseInfo(jsonText: String): UpdateReleaseInfo? {
    return runCatching {
        val root = JsonParser.parseString(jsonText).asJsonObject
        val tagName = root.get("tag_name")?.asString?.trim().orEmpty()
        if (tagName.isBlank()) {
            return@runCatching null
        }
        val title = root.get("name")?.asString?.trim().orEmpty().ifBlank { tagName }
        val notes = root.get("body")?.asString.orEmpty()
        val htmlUrl = root.get("html_url")?.asString?.trim().orEmpty().ifBlank { UPDATE_RELEASES_WEB_URL }
        val assets = root.getAsJsonArray("assets")
        var apkAsset: UpdateReleaseAsset? = null
        assets?.forEach { element ->
            if (apkAsset != null || !element.isJsonObject) {
                return@forEach
            }
            val obj = element.asJsonObject
            val assetName = obj.get("name")?.asString?.trim().orEmpty()
            val downloadUrl = obj.get("browser_download_url")?.asString?.trim().orEmpty()
            if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                apkAsset = UpdateReleaseAsset(assetName, downloadUrl)
            }
        }
        UpdateReleaseInfo(
            tagName = tagName,
            title = title,
            notes = notes,
            htmlUrl = htmlUrl,
            versionKey = buildVersionKey(tagName),
            apkAsset = apkAsset
        )
    }.getOrNull()
}

private fun fetchLatestReleaseInfo(client: OkHttpClient): UpdateReleaseInfo {
    val request = Request.Builder()
        .url(UPDATE_RELEASES_API_URL)
        .addHeader("Accept", "application/vnd.github+json")
        .addHeader("User-Agent", "ClassroomAssistant-Android")
        .build()
    client.newCall(request).execute().use { response ->
        val bodyText = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IOException("检查更新失败: HTTP ${response.code}")
        }
        return parseLatestReleaseInfo(bodyText)
            ?: throw IOException("检查更新失败: 解析发布信息失败")
    }
}

private fun hasNewerRelease(localVersionName: String, remoteTagName: String): Boolean {
    return compareVersionNames(remoteTagName, localVersionName) > 0
}

private fun getManagedUpdateDirs(context: Context): List<File> {
    return listOf(
        File(context.filesDir, "updates"),
        File(context.cacheDir, "updates")
    )
}

private fun isManagedUpdateApkPath(context: Context, file: File): Boolean {
    val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return false
    if (!candidate.name.endsWith(".apk", ignoreCase = true)) {
        return false
    }
    val candidatePath = candidate.path
    return getManagedUpdateDirs(context).any { dir ->
        val canonicalDir = runCatching { dir.canonicalFile }.getOrNull() ?: return@any false
        val prefix = canonicalDir.path + File.separator
        candidatePath.startsWith(prefix)
    }
}

private fun safeDeleteDownloadedApk(context: Context, absolutePath: String): Boolean {
    return runCatching {
        val file = File(absolutePath).canonicalFile
        if (!isManagedUpdateApkPath(context, file)) {
            return@runCatching false
        }
        file.delete() || !file.exists()
    }.getOrDefault(false)
}

private fun cleanupPendingInstallerPackage(
    context: Context,
    preferences: AndroidPreferences,
    onLog: (String) -> Unit
) {
    val pendingPath = preferences.getString(KEY_PENDING_UPDATE_APK_PATH, "").orEmpty().trim()
    if (pendingPath.isBlank()) {
        return
    }
    val deleted = safeDeleteDownloadedApk(context, pendingPath)
    clearPendingInstallerRecord(preferences)
    if (deleted) {
        onLog("已清理旧更新安装包")
    } else {
        onLog("旧更新安装包不存在或不在安全目录，已跳过删除")
    }
}

private fun isPendingInstallerStillValid(preferences: AndroidPreferences): Boolean {
    val savedAt = preferences.getLong(KEY_PENDING_UPDATE_APK_SAVED_AT, 0L)
    if (savedAt <= 0L) {
        return false
    }
    val age = System.currentTimeMillis() - savedAt
    return age in 0..PENDING_UPDATE_APK_TTL_MS
}

private fun clearPendingInstallerRecord(preferences: AndroidPreferences) {
    preferences.remove(KEY_PENDING_UPDATE_APK_PATH)
    preferences.remove(KEY_PENDING_UPDATE_APK_SAVED_AT)
    preferences.remove(KEY_PENDING_UPDATE_VERSION_KEY)
    preferences.flush()
}

private fun clearPendingInstallerIfInstalled(
    context: Context,
    preferences: AndroidPreferences,
    onLog: (String) -> Unit
) {
    val pendingPath = preferences.getString(KEY_PENDING_UPDATE_APK_PATH, "").orEmpty().trim()
    if (pendingPath.isBlank()) {
        return
    }
    val pendingVersionKey = preferences.getLong(KEY_PENDING_UPDATE_VERSION_KEY, 0L)
    if (pendingVersionKey <= 0L) {
        return
    }
    val currentVersionKey = buildVersionKey(currentAppVersionName(context))
    if (currentVersionKey <= 0L || currentVersionKey < pendingVersionKey) {
        return
    }
    safeDeleteDownloadedApk(context, pendingPath)
    clearPendingInstallerRecord(preferences)
    onLog("检测到应用已升级，已清理旧安装包记录")
}

private fun tryInstallPendingApk(
    context: Context,
    preferences: AndroidPreferences,
    onLog: (String) -> Unit
): Boolean {
    val pendingPath = preferences.getString(KEY_PENDING_UPDATE_APK_PATH, "").orEmpty().trim()
    if (pendingPath.isBlank()) {
        return false
    }
    if (!isPendingInstallerStillValid(preferences)) {
        safeDeleteDownloadedApk(context, pendingPath)
        clearPendingInstallerRecord(preferences)
        onLog("旧安装包记录已过期，已清理")
        return false
    }
    val pendingFile = runCatching { File(pendingPath).canonicalFile }.getOrNull()
    if (pendingFile == null || !pendingFile.exists() || !isManagedUpdateApkPath(context, pendingFile)) {
        clearPendingInstallerRecord(preferences)
        onLog("安装包不存在，已清理安装记录")
        return false
    }
    return runCatching {
        startApkInstallIntent(context, pendingFile)
        onLog("检测到未完成更新安装，已重新打开安装器")
        true
    }.getOrElse {
        onLog("重新打开安装器失败: ${it.message ?: it.javaClass.simpleName}")
        false
    }
}

private fun startApkInstallIntent(context: Context, apkFile: File) {
    val canonicalApk = apkFile.canonicalFile
    if (!canonicalApk.exists()) {
        throw IOException("安装包不存在: ${canonicalApk.absolutePath}")
    }
    if (!isManagedUpdateApkPath(context, canonicalApk)) {
        throw IOException("安装包路径不在安全目录: ${canonicalApk.absolutePath}")
    }
    val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}$FILE_PROVIDER_AUTHORITY_SUFFIX",
        canonicalApk
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private fun sanitizeFileName(raw: String): String {
    val sanitized = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return if (sanitized.isBlank()) "release" else sanitized
}

private fun downloadReleaseApk(
    client: OkHttpClient,
    context: Context,
    releaseTag: String,
    asset: UpdateReleaseAsset
): File {
    val updatesDir = File(context.filesDir, "updates")
    if (!updatesDir.exists()) {
        updatesDir.mkdirs()
    }
    val fileName = "${sanitizeFileName(releaseTag)}-${sanitizeFileName(asset.name)}"
    val targetFile = File(updatesDir, fileName)
    val request = Request.Builder()
        .url(asset.downloadUrl)
        .addHeader("Accept", "application/octet-stream")
        .addHeader("User-Agent", "ClassroomAssistant-Android")
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("下载更新包失败: HTTP ${response.code}")
        }
        val body = response.body ?: throw IOException("下载更新包失败: 响应体为空")
        body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    return targetFile
}

private fun defaultSettings(): SettingsData {
    return SettingsData(
        keywords = "",
        kwsThreshold = 0.05f,
        vadEnabled = true,
        quietThreshold = 5,
        quietAlertMode = "NOTIFICATION_ONLY",
        quietAutoLookbackEnabled = true,
        quietAutoLookbackExtraSeconds = 20,
        lookbackSeconds = LOOKBACK_SECONDS_DEFAULT,
        recordingSaveEnabled = false,
        retentionDays = 7,
        aiProvider = "OPENAI_COMPATIBLE",
        modelName = "",
        aiBaseUrl = "",
        aiSystemPrompt = "",
        apiToken = "",
        apiSecretKey = "",
        speechApiKey = "",
        localAsrEnabled = true,
        localAsrModelId = "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16",
        cloudWhisperEnabled = false,
        customModelEnabled = false,
        customKwsModelUrl = "",
        customAsrModelUrl = "",
        wakeAlertMode = "NOTIFICATION_ONLY",
        logMode = "SIMPLE",
        showDiagnosticLogs = false,
        showAudioDeviceLogs = false,
        showGainActivityLogs = false,
        showTtsSelfTestLogs = false,
        showHeartbeatLogs = false,
        ttsSelfTestEnabled = false,
        backgroundKeepAliveEnabled = true,
        appLanguage = AppLanguage.AUTO
    )
}

private fun classifyEngineLogCategory(message: String): String {
    return when {
        message.startsWith("监听诊断:") -> "DIAGNOSTIC"
        message.startsWith("KWS 初始化阶段:") ||
            message.startsWith("KWS 配置:") ||
            message.startsWith("KWS 使用关键词文件:") ||
            message.startsWith("KWS 模型目录:") ||
            message.startsWith("文件检查[") ||
            message.startsWith("tokens.txt 词表大小:") ||
            message.startsWith("关键词文件:") ||
            message.startsWith("关键词预览:") ||
            message.startsWith("关键词自动扩展:") ||
            message.startsWith("自动追加词:") ||
            message.startsWith("未找到内置关键词文件") ||
            message.startsWith("会话统计:") ||
            message.startsWith("⚠ 监听期间检测到微弱语音") ||
            message.startsWith("检测结果: keyword=") ||
            message.startsWith("语音识别配置：") -> "DIAGNOSTIC"
        message.startsWith("音频输入设备:") || message.startsWith("录音参数:") || message.contains("模拟器环境") -> "AUDIO_DEVICE"
        message.contains("自动切换音频源") -> "AUDIO_DEVICE"
        message.startsWith("输入增益已启用:") || message.startsWith("语音活动:") || message.startsWith("VAD:") -> "GAIN_ACTIVITY"
        message.startsWith("TTS合成自检") || message.startsWith("TTS-WAV:") || message.startsWith("模型管道自检") || message.startsWith("音频源切换后触发TTS") || message.startsWith("切源后TTS复测") -> "TTS_SELF_TEST"
        message == "正在监听" -> "HEARTBEAT"
        else -> "GENERAL"
    }
}

private fun shouldDisplayEngineLog(message: String, settings: SettingsData): Boolean {
    val category = classifyEngineLogCategory(message)
    if (settings.logMode != "FULL") {
        return category == "GENERAL"
    }
    return when (category) {
        "DIAGNOSTIC" -> settings.showDiagnosticLogs
        "AUDIO_DEVICE" -> settings.showAudioDeviceLogs
        "GAIN_ACTIVITY" -> settings.showGainActivityLogs
        "TTS_SELF_TEST" -> settings.showTtsSelfTestLogs
        "HEARTBEAT" -> settings.showHeartbeatLogs
        else -> true
    }
}

private fun shouldDisplayUiLog(message: String, settings: SettingsData): Boolean {
    val text = message.trim()
    if (text.isBlank()) {
        return false
    }
    if (settings.logMode == "FULL") {
        return true
    }
    if (text.contains("失败") || text.contains("错误") || text.contains("异常") || text.contains("HTTP 401")) {
        return true
    }
    if (simpleLogNoisePrefixes.any { text.startsWith(it) }) {
        return false
    }
    return text.startsWith("应用已启动") ||
        text.startsWith("开始") ||
        text.startsWith("停止") ||
        text.startsWith("检测到唤醒词") ||
        text.startsWith("语音识别文本:") ||
        text.startsWith("语音识别结果为空") ||
        text.startsWith("AI问答") ||
        text.startsWith("模型下载成功:") ||
        text.startsWith("本机ASR模型下载成功:") ||
        text.startsWith("切换当前模型:") ||
        text.startsWith("后台保活") ||
        text.startsWith("未配置唤醒词") ||
        text.startsWith("当前模型未就绪") ||
        text.startsWith("未选择任何模型")
}

private fun loadSettings(
    preferences: AndroidPreferences,
    secureStorage: AndroidSecureStorage?
): SettingsData {
    val defaults = defaultSettings()
    val keywords = preferences.getString(KEY_KEYWORDS, "") ?: ""
    val wakeAlertModeRaw = preferences.getString(KEY_WAKE_ALERT_MODE, defaults.wakeAlertMode) ?: defaults.wakeAlertMode
    val wakeAlertMode = if (wakeAlertModeRaw == "SOUND") "SOUND" else "NOTIFICATION_ONLY"
    val quietAlertModeRaw = preferences.getString(KEY_VAD_QUIET_ALERT_MODE, defaults.quietAlertMode) ?: defaults.quietAlertMode
    val quietAlertMode = if (quietAlertModeRaw == "SOUND") "SOUND" else "NOTIFICATION_ONLY"
    val logModeRaw = preferences.getString(KEY_LOG_MODE, defaults.logMode) ?: defaults.logMode
    val logMode = if (logModeRaw == "FULL") "FULL" else "SIMPLE"
    val kwsThreshold = (preferences.getInt(KEY_KWS_TRIGGER_THRESHOLD, 5) / 100f).coerceIn(0.05f, 0.8f)
    val lookbackSeconds = preferences.getInt(KEY_AUDIO_LOOKBACK_SECONDS, defaults.lookbackSeconds)
        .coerceIn(LOOKBACK_SECONDS_MIN, LOOKBACK_SECONDS_MAX)
    val vadEnabled = preferences.getBoolean(KEY_VAD_ENABLED, true)
    val quietThreshold = preferences.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5).coerceIn(3, 30)
    val quietAutoLookbackEnabled = preferences.getBoolean(
        KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED,
        defaults.quietAutoLookbackEnabled
    )
    val quietAutoLookbackExtraSeconds = preferences.getInt(
        KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS,
        defaults.quietAutoLookbackExtraSeconds
    ).coerceIn(1, 60)
    val recordingSaveEnabled = preferences.getBoolean(KEY_RECORDING_SAVE_ENABLED, false)
    val retentionDays = preferences.getInt(KEY_RECORDING_RETENTION_DAYS, 7).coerceIn(0, 30)
    val providerRaw = preferences.getString(KEY_AI_PROVIDER, "OPENAI_COMPATIBLE") ?: "OPENAI_COMPATIBLE"
    val aiProvider = if (aiProviders.contains(providerRaw)) providerRaw else "OPENAI_COMPATIBLE"
    val modelName = preferences.getString(KEY_AI_MODEL_NAME, "") ?: ""
    val aiBaseUrl = preferences.getString(KEY_AI_BASE_URL, "") ?: ""
    val aiSystemPrompt = preferences.getString(KEY_AI_SYSTEM_PROMPT, defaults.aiSystemPrompt) ?: defaults.aiSystemPrompt
    val apiToken = secureStorage?.retrieveSecure(KEY_AI_TOKEN) ?: ""
    val apiSecretKey = secureStorage?.retrieveSecure(KEY_AI_SECRET) ?: ""
    val speechApiKey = secureStorage?.retrieveSecure(KEY_SPEECH_API_KEY) ?: ""
    val localAsrEnabled = preferences.getBoolean(KEY_ASR_LOCAL_ENABLED, defaults.localAsrEnabled)
    val localAsrModelId = preferences.getString(KEY_ASR_LOCAL_MODEL_ID, defaults.localAsrModelId) ?: defaults.localAsrModelId
    val cloudWhisperEnabled = preferences.getBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, defaults.cloudWhisperEnabled)
    val customModelEnabled = preferences.getBoolean(KEY_CUSTOM_MODEL_ENABLED, defaults.customModelEnabled)
    val customKwsModelUrl = preferences.getString(KEY_CUSTOM_KWS_MODEL_URL, defaults.customKwsModelUrl) ?: defaults.customKwsModelUrl
    val customAsrModelUrl = preferences.getString(KEY_CUSTOM_ASR_MODEL_URL, defaults.customAsrModelUrl) ?: defaults.customAsrModelUrl
    val showDiagnosticLogs = preferences.getBoolean(KEY_LOG_SHOW_DIAGNOSTIC, defaults.showDiagnosticLogs)
    val showAudioDeviceLogs = preferences.getBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, defaults.showAudioDeviceLogs)
    val showGainActivityLogs = preferences.getBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, defaults.showGainActivityLogs)
    val showTtsSelfTestLogs = preferences.getBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, defaults.showTtsSelfTestLogs)
    val showHeartbeatLogs = preferences.getBoolean(KEY_LOG_SHOW_HEARTBEAT, defaults.showHeartbeatLogs)
    val ttsSelfTestEnabled = preferences.getBoolean(KEY_TTS_SELF_TEST_ENABLED, defaults.ttsSelfTestEnabled)
    val backgroundKeepAliveEnabled = preferences.getBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, defaults.backgroundKeepAliveEnabled)
    val appLanguage = AppLanguage.fromCode(preferences.getString(KEY_APP_LANGUAGE, defaults.appLanguage.code))
    return SettingsData(
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
        aiSystemPrompt = aiSystemPrompt,
        apiToken = apiToken,
        apiSecretKey = apiSecretKey,
        speechApiKey = speechApiKey,
        localAsrEnabled = localAsrEnabled,
        localAsrModelId = localAsrModelId,
        cloudWhisperEnabled = cloudWhisperEnabled,
        customModelEnabled = customModelEnabled,
        customKwsModelUrl = customKwsModelUrl,
        customAsrModelUrl = customAsrModelUrl,
        wakeAlertMode = wakeAlertMode,
        logMode = logMode,
        showDiagnosticLogs = showDiagnosticLogs,
        showAudioDeviceLogs = showAudioDeviceLogs,
        showGainActivityLogs = showGainActivityLogs,
        showTtsSelfTestLogs = showTtsSelfTestLogs,
        showHeartbeatLogs = showHeartbeatLogs,
        ttsSelfTestEnabled = ttsSelfTestEnabled,
        backgroundKeepAliveEnabled = backgroundKeepAliveEnabled,
        appLanguage = appLanguage
    )
}

private fun saveSettings(
    preferences: AndroidPreferences,
    secureStorage: AndroidSecureStorage?,
    settings: SettingsData
) {
    preferences.putString(KEY_KEYWORDS, settings.keywords.trim())
    val kwsThresholdPercent = (settings.kwsThreshold.coerceIn(0.05f, 0.8f) * 100f).toInt()
    preferences.putInt(KEY_KWS_TRIGGER_THRESHOLD, kwsThresholdPercent)
    preferences.putInt(KEY_AUDIO_LOOKBACK_SECONDS, settings.lookbackSeconds)
    preferences.putBoolean(KEY_VAD_ENABLED, settings.vadEnabled)
    preferences.putInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, settings.quietThreshold)
    preferences.putString(KEY_VAD_QUIET_ALERT_MODE, if (settings.quietAlertMode == "SOUND") "SOUND" else "NOTIFICATION_ONLY")
    preferences.putBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, settings.quietAutoLookbackEnabled)
    preferences.putInt(
        KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS,
        settings.quietAutoLookbackExtraSeconds.coerceIn(1, 60)
    )
    preferences.putBoolean(KEY_RECORDING_SAVE_ENABLED, settings.recordingSaveEnabled)
    preferences.putInt(KEY_RECORDING_RETENTION_DAYS, settings.retentionDays)
    preferences.putString(KEY_AI_PROVIDER, settings.aiProvider)
    preferences.putString(KEY_AI_MODEL_NAME, settings.modelName)
    preferences.putString(KEY_AI_BASE_URL, settings.aiBaseUrl.trim())
    preferences.putString(KEY_AI_SYSTEM_PROMPT, settings.aiSystemPrompt.trim())
    preferences.putBoolean(KEY_ASR_LOCAL_ENABLED, settings.localAsrEnabled)
    preferences.putString(KEY_ASR_LOCAL_MODEL_ID, settings.localAsrModelId)
    preferences.putBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, settings.cloudWhisperEnabled)
    preferences.putBoolean(KEY_CUSTOM_MODEL_ENABLED, settings.customModelEnabled)
    preferences.putString(KEY_CUSTOM_KWS_MODEL_URL, settings.customKwsModelUrl.trim())
    preferences.putString(KEY_CUSTOM_ASR_MODEL_URL, settings.customAsrModelUrl.trim())
    preferences.putString(KEY_WAKE_ALERT_MODE, if (settings.wakeAlertMode == "SOUND") "SOUND" else "NOTIFICATION_ONLY")
    preferences.putString(KEY_LOG_MODE, if (settings.logMode == "FULL") "FULL" else "SIMPLE")
    preferences.putBoolean(KEY_LOG_SHOW_DIAGNOSTIC, settings.showDiagnosticLogs)
    preferences.putBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, settings.showAudioDeviceLogs)
    preferences.putBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, settings.showGainActivityLogs)
    preferences.putBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, settings.showTtsSelfTestLogs)
    preferences.putBoolean(KEY_LOG_SHOW_HEARTBEAT, settings.showHeartbeatLogs)
    preferences.putBoolean(KEY_TTS_SELF_TEST_ENABLED, settings.ttsSelfTestEnabled)
    preferences.putBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, settings.backgroundKeepAliveEnabled)
    preferences.putString(KEY_APP_LANGUAGE, settings.appLanguage.code)
    secureStorage?.storeSecure(KEY_AI_TOKEN, settings.apiToken)
    secureStorage?.storeSecure(KEY_AI_SECRET, settings.apiSecretKey)
    secureStorage?.storeSecure(KEY_SPEECH_API_KEY, settings.speechApiKey)
    preferences.flush()
}

private fun parseKeywords(raw: String): List<String> {
    if (raw.isBlank()) {
        return emptyList()
    }
    return raw.split(",", "，", " ", "；", ";", "\n", "\t")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
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

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCrashMonitor.install(applicationContext)

        setContent {
            ClassroomAssistantTheme {
                AppNavigation(
                    onRequestAudioPermission = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
        }
    }
}

private data class AppRuntimeDependencies(
    val preferences: AndroidPreferences,
    val storage: AndroidStorage,
    val modelManager: LocalKwsModelManager,
    val asrModelManager: LocalAsrModelManager,
    val asrService: AndroidSpeechRecognitionService,
    val aiAnswerService: AndroidAiAnswerService,
    val audioRecorder: AndroidAudioRecorder,
    val wakeWordEngine: AndroidWakeWordEngine
)

private fun createAppRuntimeDependencies(context: Context): AppRuntimeDependencies {
    val appContext = context.applicationContext
    val preferences = AndroidPreferences(appContext)
    val storage = AndroidStorage(appContext)
    val modelManager = LocalKwsModelManager(appContext, storage)
    val asrModelManager = LocalAsrModelManager(appContext, storage)
    val asrService = AndroidSpeechRecognitionService(asrModelManager)
    val aiAnswerService = AndroidAiAnswerService()
    val audioRecorder = AndroidAudioRecorder(appContext)
    val wakeWordEngine = AndroidWakeWordEngine(appContext, audioRecorder)
    return AppRuntimeDependencies(
        preferences = preferences,
        storage = storage,
        modelManager = modelManager,
        asrModelManager = asrModelManager,
        asrService = asrService,
        aiAnswerService = aiAnswerService,
        audioRecorder = audioRecorder,
        wakeWordEngine = wakeWordEngine
    )
}

@Composable
private fun AppStartupFailureScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("应用启动失败", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PendingCrashSummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("检测到上次异常退出", style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val runtimeDependenciesResult = remember(context.applicationContext) {
        runCatching {
            createAppRuntimeDependencies(context.applicationContext)
        }
    }
    val runtimeDependencies = runtimeDependenciesResult.getOrNull()
    val runtimeInitError = runtimeDependenciesResult.exceptionOrNull()

    LaunchedEffect(runtimeInitError) {
        if (runtimeInitError != null) {
            AppCrashMonitor.recordHandledError(
                context.applicationContext,
                "应用启动依赖初始化失败",
                runtimeInitError
            )
        }
    }

    if (runtimeDependencies == null) {
        AppStartupFailureScreen(
            message = runtimeInitError?.message ?: "启动阶段发生未知异常，请重新运行并查看日志"
        )
        return
    }

    val preferences = runtimeDependencies.preferences
    val storage = runtimeDependencies.storage
    val coroutineScope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val modelManager = runtimeDependencies.modelManager
    val asrModelManager = runtimeDependencies.asrModelManager
    val asrService = runtimeDependencies.asrService
    val aiAnswerService = runtimeDependencies.aiAnswerService
    val availableModels = remember { modelManager.getAvailableModels() }
    val customKwsModelId = remember { LocalKwsModelManager.CUSTOM_MODEL_ID }
    val customAsrModelId = remember { LocalAsrModelManager.CUSTOM_MODEL_ID }
    val customModelEnabledAtLaunch = remember {
        preferences.getBoolean(KEY_CUSTOM_MODEL_ENABLED, false)
    }
    val defaultModelId = remember { modelManager.getDefaultModelId() }
    val savedModelId = remember { preferences.getString(KEY_KWS_MODEL_ID, defaultModelId) ?: defaultModelId }
    var currentModelId by remember {
        mutableStateOf(
            when {
                !customModelEnabledAtLaunch && savedModelId == customKwsModelId -> defaultModelId
                availableModels.any { it.id == savedModelId } -> savedModelId
                else -> defaultModelId
            }
        )
    }
    val savedSelectedRaw = remember { preferences.getString(KEY_KWS_MODEL_SELECTED, "") ?: "" }
    val savedSelected = remember {
        savedSelectedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    var selectedModelIds by remember {
        mutableStateOf(
            (savedSelected + currentModelId).filter { id ->
                availableModels.any { it.id == id } &&
                    (customModelEnabledAtLaunch || id != customKwsModelId)
            }.toSet()
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
    val availableAsrModels = remember { asrModelManager.getAvailableModels() }
    val defaultAsrModelId = remember { asrModelManager.getDefaultModelId() }
    var asrModelStatusMap by remember {
        mutableStateOf(
            availableAsrModels.associate { option ->
                option.id to if (asrModelManager.isModelReady(option.id)) "已就绪" else "未下载"
            }
        )
    }
    var asrModelProgressMap by remember {
        mutableStateOf(availableAsrModels.associate { it.id to 0f })
    }
    var asrModelFailureMap by remember {
        mutableStateOf(availableAsrModels.associate { it.id to "" })
    }
    var isDownloadingAsr by remember { mutableStateOf(false) }
    var pendingAsrDownloadModelId by remember { mutableStateOf<String?>(null) }
    val audioRecorder = runtimeDependencies.audioRecorder
    val wakeWordEngine = runtimeDependencies.wakeWordEngine
    var isMonitoring by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("空闲") }
    var activeListeningSettings by remember { mutableStateOf<SettingsData?>(null) }
    var activeListeningModelId by remember { mutableStateOf<String?>(null) }
    var secureStorage by remember { mutableStateOf<AndroidSecureStorage?>(null) }
    var settings by remember { mutableStateOf(defaultSettings()) }
    val effectiveLanguage by remember(settings.appLanguage) {
        mutableStateOf(resolveEffectiveLanguage(settings.appLanguage))
    }
    fun t(zhText: String, enText: String): String = tr(effectiveLanguage, zhText, enText)
    val releaseHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }
    var updateStatusMessage by remember { mutableStateOf("") }
    var pendingUpdateInfo by remember { mutableStateOf<UpdateReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isInstallingUpdate by remember { mutableStateOf(false) }
    var hasAutoCheckedUpdate by remember { mutableStateOf(false) }
    var settingsLoaded by remember { mutableStateOf(false) }
    var logVersion by remember { mutableStateOf(0) }
    var logText by remember { mutableStateOf("日志输出...") }
    var recognitionText by remember { mutableStateOf("") }
    var aiAnswerText by remember { mutableStateOf("") }
    var isAiAnswering by remember { mutableStateOf(false) }
    var wakeStatusResetRunnable by remember { mutableStateOf<Runnable?>(null) }
    val vadQuietStartMs = remember { AtomicLong(0L) }
    val vadTimeoutNotified = remember { AtomicBoolean(false) }
    val vadLastTimedOutQuietDurationSec = remember { AtomicLong(0L) }
    val asrInProgress = remember { AtomicBoolean(false) }
    val pendingWakeRecognition = remember { AtomicBoolean(false) }
    var lastAutoDownloadedAsrModelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onRequestAudioPermission()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onRequestNotificationPermission()
        }
    }

    fun appendLog(message: String) {
        val normalizedMessage = message.trim()
        if (!shouldDisplayUiLog(normalizedMessage, settings)) {
            return
        }
        val line = formatLogLine(normalizedMessage)
        val maxLines = if (settings.logMode == "FULL") 500 else 120
        mainHandler.post {
            logText = if (logText == "日志输出..." || logText.isBlank()) {
                line
            } else {
                val combined = "$logText\n$line"
                val lines = combined.split("\n")
                if (lines.size > maxLines) {
                    lines.takeLast(maxLines).joinToString("\n")
                } else {
                    combined
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val logFile = File(storage.getLogsDir(), "app.log")
                logFile.parentFile?.mkdirs()
                logFile.appendText("$line\n")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "写入应用日志失败", e)
                AppCrashMonitor.recordHandledError(
                    context.applicationContext,
                    "写入应用日志失败",
                    e
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        clearPendingInstallerIfInstalled(context.applicationContext, preferences) { appendLog(it) }
        tryInstallPendingApk(context.applicationContext, preferences) { appendLog(it) }
    }

    fun appendEngineLog(message: String) {
        if (shouldDisplayEngineLog(message, settings)) {
            appendLog(message)
        }
    }

    fun markReleaseIgnored(release: UpdateReleaseInfo) {
        if (release.versionKey <= 0L) {
            return
        }
        preferences.putLong(KEY_UPDATE_IGNORE_BEFORE_VERSION_CODE, release.versionKey)
        preferences.putLong(KEY_UPDATE_LAST_PROMPTED_VERSION_CODE, release.versionKey)
        preferences.flush()
    }

    fun clearReleaseIgnoreIfNeeded(release: UpdateReleaseInfo) {
        val ignoredBeforeVersion = preferences.getLong(KEY_UPDATE_IGNORE_BEFORE_VERSION_CODE, 0L)
        if (ignoredBeforeVersion > 0L && release.versionKey > ignoredBeforeVersion) {
            preferences.remove(KEY_UPDATE_IGNORE_BEFORE_VERSION_CODE)
            preferences.flush()
        }
    }

    fun shouldSkipPromptByUserChoice(release: UpdateReleaseInfo): Boolean {
        val ignoredBeforeVersion = preferences.getLong(KEY_UPDATE_IGNORE_BEFORE_VERSION_CODE, 0L)
        if (ignoredBeforeVersion <= 0L) {
            return false
        }
        if (release.versionKey <= 0L) {
            return false
        }
        return release.versionKey <= ignoredBeforeVersion
    }

    fun checkForUpdate(manual: Boolean) {
        if (isCheckingUpdate || isInstallingUpdate) {
            return
        }
        isCheckingUpdate = true
        if (manual) {
            updateStatusMessage = t("正在检查更新...", "Checking for updates...")
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val release = fetchLatestReleaseInfo(releaseHttpClient)
                val localVersionName = currentAppVersionName(context.applicationContext)
                val hasNewer = hasNewerRelease(localVersionName, release.tagName)
                if (!hasNewer) {
                    withContext(Dispatchers.Main) {
                        if (manual) {
                            updateStatusMessage = t(
                                "当前已是最新版本（$localVersionName）",
                                "You are up to date ($localVersionName)"
                            )
                        }
                        isCheckingUpdate = false
                    }
                    return@launch
                }
                clearReleaseIgnoreIfNeeded(release)
                if (shouldSkipPromptByUserChoice(release)) {
                    withContext(Dispatchers.Main) {
                        if (manual) {
                            updateStatusMessage = t(
                                "该版本已被设置为“下个新版本再提醒”",
                                "This version is snoozed until a newer release"
                            )
                        }
                        isCheckingUpdate = false
                    }
                    return@launch
                }
                preferences.putLong(KEY_UPDATE_LAST_PROMPTED_VERSION_CODE, release.versionKey)
                preferences.flush()
                withContext(Dispatchers.Main) {
                    pendingUpdateInfo = release
                    showUpdateDialog = true
                    updateStatusMessage = t(
                        "发现新版本 ${release.tagName}",
                        "New version available: ${release.tagName}"
                    )
                    isCheckingUpdate = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (manual) {
                        updateStatusMessage = t(
                            "检查更新失败: ${e.message ?: "未知错误"}",
                            "Update check failed: ${e.message ?: "unknown error"}"
                        )
                    }
                    appendLog("检查更新失败: ${e.message ?: e.javaClass.simpleName}")
                    isCheckingUpdate = false
                }
            }
        }
    }

    fun installReleaseUpdate(release: UpdateReleaseInfo) {
        val asset = release.apkAsset
        if (asset == null) {
            updateStatusMessage = t(
                "该版本未提供 APK 资源，请到 Release 页面手动下载。",
                "No APK asset in this release. Please download manually from Releases page."
            )
            return
        }
        if (isInstallingUpdate) {
            return
        }
        isInstallingUpdate = true
        showUpdateDialog = false
        updateStatusMessage = t("正在下载更新包...", "Downloading update package...")
        coroutineScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            try {
                cleanupPendingInstallerPackage(appContext, preferences) { appendLog(it) }
                val apkFile = downloadReleaseApk(releaseHttpClient, appContext, release.tagName, asset)
                preferences.putString(KEY_PENDING_UPDATE_APK_PATH, apkFile.absolutePath)
                preferences.putLong(KEY_PENDING_UPDATE_APK_SAVED_AT, System.currentTimeMillis())
                preferences.putLong(KEY_PENDING_UPDATE_VERSION_KEY, release.versionKey)
                preferences.flush()
                withContext(Dispatchers.Main) {
                    updateStatusMessage = t("下载完成，正在打开安装器...", "Download complete, opening installer...")
                    startApkInstallIntent(appContext, apkFile)
                }
                withContext(Dispatchers.Main) {
                    updateStatusMessage = t(
                        "安装器已打开，请按系统提示完成安装；若中断，重新打开应用会自动继续安装。",
                        "Installer opened. Complete installation in system UI; if interrupted, reopen the app to resume."
                    )
                }
            } catch (e: Exception) {
                val pendingPath = preferences.getString(KEY_PENDING_UPDATE_APK_PATH, "").orEmpty().trim()
                if (pendingPath.isNotBlank()) {
                    safeDeleteDownloadedApk(appContext, pendingPath)
                    clearPendingInstallerRecord(preferences)
                }
                withContext(Dispatchers.Main) {
                    updateStatusMessage = t(
                        "更新失败: ${e.message ?: "未知错误"}",
                        "Update failed: ${e.message ?: "unknown error"}"
                    )
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isInstallingUpdate = false
                }
            }
        }
    }

    fun startKeepAliveIfEnabled(): Boolean {
        val result = ListeningKeepAliveService.start(context.applicationContext)
        result.exceptionOrNull()?.let { error ->
            appendLog("后台保活启动失败: ${error.message ?: error.javaClass.simpleName}")
        }
        return result.isSuccess
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

    LaunchedEffect(settings.customModelEnabled, currentModelId) {
        if (settings.customModelEnabled) {
            return@LaunchedEffect
        }
        if (currentModelId == customKwsModelId) {
            val fallbackKwsModelId = availableModels.firstOrNull { it.id != customKwsModelId }?.id ?: defaultModelId
            currentModelId = fallbackKwsModelId
            preferences.putString(KEY_KWS_MODEL_ID, fallbackKwsModelId)
            preferences.flush()
            currentModelReady = modelManager.isModelReady(fallbackKwsModelId)
            selectedModelIds = selectedModelIds.filter { it != customKwsModelId }.toSet()
            persistSelectedModels()
            appendLog("已关闭自行增加模型，自动切换为内置唤醒模型")
        }
    }

    fun updateCurrentModel(modelId: String) {
        currentModelId = modelId
        preferences.putString(KEY_KWS_MODEL_ID, modelId)
        preferences.flush()
        currentModelReady = modelManager.isModelReady(modelId)
        appendLog("切换当前模型: ${availableModels.firstOrNull { it.id == modelId }?.name ?: modelId}")
        appendLog("生效说明：唤醒仅使用“当前模型”，勾选列表仅用于批量下载缓存")
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

    fun resolveKwsModelName(modelId: String): String {
        return availableModels.firstOrNull { it.id == modelId }?.name ?: modelId
    }

    fun resolveAsrModelId(modelId: String): String {
        return modelId.ifBlank { defaultAsrModelId }
    }

    fun resolveAsrModelName(modelId: String): String {
        return availableAsrModels.firstOrNull { it.id == modelId }?.name ?: modelId
    }

    fun buildAsrRouteSummary(startSettings: SettingsData): String {
        val resolvedAsrModelId = resolveAsrModelId(startSettings.localAsrModelId)
        val resolvedAsrModelName = resolveAsrModelName(resolvedAsrModelId)
        return when {
            startSettings.cloudWhisperEnabled && startSettings.localAsrEnabled ->
                "云端Whisper优先，本机ASR候选=$resolvedAsrModelName($resolvedAsrModelId)"
            startSettings.cloudWhisperEnabled -> "云端Whisper"
            startSettings.localAsrEnabled -> "本机ASR=$resolvedAsrModelName($resolvedAsrModelId)"
            else -> "已关闭"
        }
    }

    fun appendActivePipelineLog(prefix: String, startSettings: SettingsData = settings) {
        appendLog(
            "$prefix: 唤醒模型=${resolveKwsModelName(currentModelId)}($currentModelId), 语音识别=${buildAsrRouteSummary(startSettings)}"
        )
    }

    fun updateAsrModelStatus(modelId: String, status: String, progress: Float = 0f) {
        asrModelStatusMap = asrModelStatusMap.toMutableMap().apply { put(modelId, status) }
        asrModelProgressMap = asrModelProgressMap.toMutableMap().apply { put(modelId, progress) }
    }

    fun refreshAsrModelStatus() {
        asrModelStatusMap = availableAsrModels.associate { option ->
            option.id to when {
                asrModelManager.isModelReady(option.id) -> "已就绪"
                pendingAsrDownloadModelId == option.id -> "排队中"
                else -> "未下载"
            }
        }
        asrModelProgressMap = asrModelProgressMap.toMutableMap().apply {
            availableAsrModels.forEach { option ->
                if (asrModelManager.isModelReady(option.id)) {
                    put(option.id, 1f)
                }
            }
        }
    }

    LaunchedEffect(settings.customModelEnabled, settings.localAsrModelId) {
        if (settings.customModelEnabled) {
            return@LaunchedEffect
        }
        if (settings.localAsrModelId == customAsrModelId) {
            val fallbackAsrModelId = availableAsrModels.firstOrNull { it.id != customAsrModelId }?.id ?: defaultAsrModelId
            settings = settings.copy(localAsrModelId = fallbackAsrModelId)
            saveSettings(preferences, secureStorage, settings)
            appendLog("已关闭自行增加模型，自动切换为内置本机ASR模型")
        }
    }

    fun downloadCustomKwsModel(url: String) {
        if (isDownloading) {
            appendLog("当前有下载任务，稍后再试")
            return
        }
        val customUrl = url.trim()
        if (customUrl.isBlank()) {
            appendLog("请先填写自定义唤醒模型下载链接")
            return
        }
        val customModel = availableModels.firstOrNull { it.id == customKwsModelId }
        if (customModel == null) {
            appendLog("未找到自定义唤醒模型入口")
            return
        }
        isDownloading = true
        selectedModelIds = selectedModelIds + customKwsModelId
        persistSelectedModels()
        appendLog("开始下载自定义唤醒模型")
        coroutineScope.launch(Dispatchers.IO) {
            mainHandler.post {
                updateModelStatus(customKwsModelId, "下载中", 0f)
                modelFailureMap = modelFailureMap.toMutableMap().apply { put(customKwsModelId, "") }
            }
            val result = modelManager.downloadAndPrepareFromUrl(
                modelId = customKwsModelId,
                archiveUrl = customUrl,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    mainHandler.post {
                        val status = when {
                            total > 0 && progress >= 0.999f -> "下载完成，处理中"
                            total > 0 -> "下载中 ${(progress * 100).toInt()}%"
                            else -> "下载中"
                        }
                        updateModelStatus(customKwsModelId, status, progress)
                    }
                },
                onEvent = { event -> appendLog("模型下载[${customModel.name}]: $event") }
            )
            withContext(Dispatchers.Main) {
                isDownloading = false
                if (result.isSuccess) {
                    updateModelStatus(customKwsModelId, "已就绪", 1f)
                    modelFailureMap = modelFailureMap.toMutableMap().apply { put(customKwsModelId, "") }
                    currentModelId = customKwsModelId
                    preferences.putString(KEY_KWS_MODEL_ID, customKwsModelId)
                    preferences.flush()
                    currentModelReady = modelManager.isModelReady(customKwsModelId)
                    appendLog("自定义唤醒模型下载成功，已切换为当前模型")
                } else {
                    updateModelStatus(customKwsModelId, "失败", 0f)
                    val reason = buildFailureReason(result.exceptionOrNull())
                    modelFailureMap = modelFailureMap.toMutableMap().apply { put(customKwsModelId, reason) }
                    appendLog("自定义唤醒模型下载失败: $reason")
                }
            }
        }
    }

    fun downloadCustomAsrModel(url: String) {
        if (isDownloadingAsr) {
            appendLog("当前有本机ASR模型下载任务，稍后再试")
            return
        }
        val customUrl = url.trim()
        if (customUrl.isBlank()) {
            appendLog("请先填写自定义语音识别模型下载链接")
            return
        }
        val customModel = availableAsrModels.firstOrNull { it.id == customAsrModelId }
        if (customModel == null) {
            appendLog("未找到自定义语音识别模型入口")
            return
        }
        isDownloadingAsr = true
        appendLog("开始下载自定义语音识别模型")
        coroutineScope.launch(Dispatchers.IO) {
            mainHandler.post {
                updateAsrModelStatus(customAsrModelId, "下载中", 0f)
                asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(customAsrModelId, "") }
            }
            val result = asrModelManager.downloadAndPrepareFromUrl(
                modelId = customAsrModelId,
                archiveUrl = customUrl,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    mainHandler.post {
                        val status = when {
                            total > 0 && progress >= 0.999f -> "下载完成，处理中"
                            total > 0 -> "下载中 ${(progress * 100).toInt()}%"
                            else -> "下载中"
                        }
                        updateAsrModelStatus(customAsrModelId, status, progress)
                    }
                },
                onEvent = { event -> appendLog("ASR下载[${customModel.name}]: $event") }
            )
            withContext(Dispatchers.Main) {
                isDownloadingAsr = false
                if (result.isSuccess) {
                    updateAsrModelStatus(customAsrModelId, "已就绪", 1f)
                    asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(customAsrModelId, "") }
                    settings = settings.copy(localAsrModelId = customAsrModelId)
                    saveSettings(preferences, secureStorage, settings)
                    appendLog("自定义语音识别模型下载成功，已设为当前本机ASR模型")
                } else {
                    updateAsrModelStatus(customAsrModelId, "失败", 0f)
                    val reason = buildFailureReason(result.exceptionOrNull())
                    asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(customAsrModelId, reason) }
                    appendLog("自定义语音识别模型下载失败: $reason")
                }
            }
        }
    }

    fun downloadAsrModel(modelId: String, reason: String = "用户触发") {
        val modelName = availableAsrModels.firstOrNull { it.id == modelId }?.name ?: modelId
        if (isDownloadingAsr) {
            if (pendingAsrDownloadModelId == modelId) {
                appendLog("本机ASR模型已在排队中: $modelName")
                return
            }
            pendingAsrDownloadModelId = modelId
            updateAsrModelStatus(modelId, "排队中", 0f)
            appendLog("ASR 模型下载任务进行中，已加入队列: $modelName")
            return
        }
        isDownloadingAsr = true
        if (pendingAsrDownloadModelId == modelId) {
            pendingAsrDownloadModelId = null
        }
        appendLog("开始下载本机ASR模型($reason): $modelName")
        coroutineScope.launch(Dispatchers.IO) {
            mainHandler.post {
                updateAsrModelStatus(modelId, "下载中", 0f)
                asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(modelId, "") }
            }
            val result = asrModelManager.downloadAndPrepare(
                modelId = modelId,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    mainHandler.post {
                        val status = when {
                            total > 0 && progress >= 0.999f -> "下载完成，处理中"
                            total > 0 -> "下载中 ${(progress * 100).toInt()}%"
                            else -> "下载中"
                        }
                        updateAsrModelStatus(modelId, status, progress)
                    }
                },
                onEvent = { event ->
                    appendLog("ASR下载[$modelName]: $event")
                }
            )
            withContext(Dispatchers.Main) {
                isDownloadingAsr = false
                if (result.isSuccess) {
                    updateAsrModelStatus(modelId, "已就绪", 1f)
                    asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(modelId, "") }
                    appendLog("本机ASR模型下载成功: $modelName")
                } else {
                    updateAsrModelStatus(modelId, "失败", 0f)
                    val failReason = buildFailureReason(result.exceptionOrNull())
                    asrModelFailureMap = asrModelFailureMap.toMutableMap().apply { put(modelId, failReason) }
                    appendLog("本机ASR模型下载失败: $modelName, 原因: $failReason")
                }
                val queuedModelId = pendingAsrDownloadModelId
                if (!queuedModelId.isNullOrBlank() && queuedModelId != modelId) {
                    pendingAsrDownloadModelId = null
                    downloadAsrModel(queuedModelId, "排队自动开始")
                }
            }
        }
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
        appendLog(
            "批量下载唤醒模型: 已选${targets.size}个，当前生效唤醒模型=${resolveKwsModelName(currentModelId)}($currentModelId)"
        )
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
                val result = modelManager.downloadAndPrepare(
                    modelId = modelId,
                    onProgress = { downloaded, total ->
                        val progress = if (total > 0) downloaded.toFloat() / total else 0f
                        mainHandler.post {
                            val status = when {
                                total > 0 && progress >= 0.999f -> "下载完成，处理中"
                                total > 0 -> "下载中 ${(progress * 100).toInt()}%"
                                else -> "下载中"
                            }
                            updateModelStatus(modelId, status, progress)
                        }
                    },
                    onEvent = { event ->
                        appendLog("模型下载[$modelName]: $event")
                    }
                )
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
            val result = modelManager.downloadAndPrepare(
                modelId = modelId,
                onProgress = { downloaded, total ->
                    val progress = if (total > 0) downloaded.toFloat() / total else 0f
                    mainHandler.post {
                        val status = when {
                            total > 0 && progress >= 0.999f -> "下载完成，处理中"
                            total > 0 -> "下载中 ${(progress * 100).toInt()}%"
                            else -> "下载中"
                        }
                        updateModelStatus(modelId, status, progress)
                    }
                },
                onEvent = { event ->
                    appendLog("模型下载[$modelName]: $event")
                }
            )
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

    fun stopMonitoringSession(reason: String) {
        wakeStatusResetRunnable?.let { mainHandler.removeCallbacks(it) }
        wakeStatusResetRunnable = null
        wakeWordEngine.stop { appendLog(it) }
        ListeningKeepAliveService.stop(context.applicationContext)
        asrInProgress.set(false)
        pendingWakeRecognition.set(false)
        isMonitoring = false
        isStarting = false
        statusText = "空闲"
        vadQuietStartMs.set(0L)
        vadTimeoutNotified.set(false)
        activeListeningSettings = null
        activeListeningModelId = null
        appendLog(reason)
        AppCrashMonitor.clearListeningStage(context.applicationContext)
    }

    fun notifyWakeAlert() {
        val notificationShown = showWakeNotification(context.applicationContext, settings.wakeAlertMode)
        if (!notificationShown) {
            appendLog("唤醒提醒未显示：通知权限可能未授予")
            Toast.makeText(context, "已检测到唤醒词", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerAiAnswerAfterRecognition(recognition: String, startSettings: SettingsData) {
        val trimmedText = recognition.trim()
        if (trimmedText.isBlank()) {
            appendLog("AI问答跳过：识别文本为空")
            return
        }
        if (startSettings.apiToken.isBlank()) {
            appendLog("AI问答跳过：未配置 API Token")
            return
        }
        if (isAiAnswering) {
            appendLog("AI问答进行中，忽略本次请求")
            return
        }
        isAiAnswering = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                appendLog("开始AI问答: provider=${startSettings.aiProvider}")
                val prompt = PromptTemplate.buildPrompt(trimmedText)
                val answer = aiAnswerService.generateAnswer(
                    provider = startSettings.aiProvider,
                    modelName = startSettings.modelName,
                    baseUrl = startSettings.aiBaseUrl,
                    apiToken = startSettings.apiToken,
                    apiSecretKey = startSettings.apiSecretKey,
                    prompt = prompt,
                    languageCode = if (resolveEffectiveLanguage(startSettings.appLanguage) == AppLanguage.ZH_CN) "zh-CN" else "en-US",
                    customSystemPrompt = startSettings.aiSystemPrompt,
                    onLog = { appendLog(it) }
                )
                if (answer.isBlank()) {
                    appendLog("AI问答返回空结果")
                    return@launch
                }
                mainHandler.post {
                    aiAnswerText = answer
                }
            } catch (t: Throwable) {
                appendLog("AI问答异常: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                mainHandler.post { isAiAnswering = false }
            }
        }
    }

    fun triggerAiAnswerForQuestion(question: String, startSettings: SettingsData) {
        val trimmedQuestion = question.trim()
        if (trimmedQuestion.isBlank()) {
            appendLog("AI问答跳过：问题为空")
            return
        }
        if (startSettings.apiToken.isBlank()) {
            appendLog("AI问答跳过：未配置 API Token")
            return
        }
        if (isAiAnswering) {
            appendLog("AI问答进行中，忽略本次请求")
            return
        }
        isAiAnswering = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                appendLog("开始AI问答(手动): provider=${startSettings.aiProvider}")
                val prompt = PromptTemplate.buildPrompt(trimmedQuestion)
                val answer = aiAnswerService.generateAnswer(
                    provider = startSettings.aiProvider,
                    modelName = startSettings.modelName,
                    baseUrl = startSettings.aiBaseUrl,
                    apiToken = startSettings.apiToken,
                    apiSecretKey = startSettings.apiSecretKey,
                    prompt = prompt,
                    languageCode = if (resolveEffectiveLanguage(startSettings.appLanguage) == AppLanguage.ZH_CN) "zh-CN" else "en-US",
                    customSystemPrompt = startSettings.aiSystemPrompt,
                    onLog = { appendLog(it) }
                )
                if (answer.isBlank()) {
                    appendLog("AI问答返回空结果")
                    return@launch
                }
                mainHandler.post {
                    aiAnswerText = answer
                }
            } catch (t: Throwable) {
                appendLog("AI问答异常: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                mainHandler.post { isAiAnswering = false }
            }
        }
    }

    fun triggerSpeechRecognitionAfterWake(startSettings: SettingsData) {
        if (!asrInProgress.compareAndSet(false, true)) {
            val alreadyPending = pendingWakeRecognition.getAndSet(true)
            if (!alreadyPending) {
                appendLog("语音识别仍在处理中，已记录本次触发，当前识别结束后自动补跑一次")
            }
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val recognitionFlowStartedAt = System.currentTimeMillis()
                appendLog("唤醒后处理开始：回溯音频 -> 语音识别 -> AI问答")
                val useCloudWhisper = startSettings.cloudWhisperEnabled
                val useLocalAsr = startSettings.localAsrEnabled
                val resolvedAsrModelId = resolveAsrModelId(startSettings.localAsrModelId)
                val resolvedAsrModelName = resolveAsrModelName(resolvedAsrModelId)
                appendLog(
                    "语音识别配置：cloudWhisper=${if (useCloudWhisper) "开" else "关"}, localAsr=${if (useLocalAsr) "开" else "关"}, localModel=$resolvedAsrModelName($resolvedAsrModelId)"
                )
                if (!useCloudWhisper && !useLocalAsr) {
                    appendLog("语音识别跳过：本机识别与云端Whisper均未开启")
                    return@launch
                }

                if (!useCloudWhisper && useLocalAsr && !asrModelManager.isModelReady(resolvedAsrModelId)) {
                    appendLog("本机ASR模型未就绪，准备自动下载: $resolvedAsrModelName($resolvedAsrModelId)")
                    withContext(Dispatchers.Main) {
                        downloadAsrModel(resolvedAsrModelId, "唤醒触发自动下载")
                    }
                    appendLog("语音识别跳过：本次触发仅执行模型下载，待模型就绪后下次唤醒再识别")
                    return@launch
                }

                val quietTimedOutDurationSec = vadLastTimedOutQuietDurationSec.getAndSet(0L).toInt().coerceAtLeast(0)
                val autoLookbackSeconds = if (
                    startSettings.vadEnabled &&
                    startSettings.quietAutoLookbackEnabled &&
                    quietTimedOutDurationSec > 0
                ) {
                    (quietTimedOutDurationSec + startSettings.quietAutoLookbackExtraSeconds)
                        .coerceIn(LOOKBACK_SECONDS_MIN, LOOKBACK_SECONDS_MAX)
                } else {
                    startSettings.lookbackSeconds
                }
                val effectiveLookbackSeconds = maxOf(startSettings.lookbackSeconds, autoLookbackSeconds)

                val audioFetchStartedAt = System.currentTimeMillis()
                val pcm = audioRecorder.getAudioBefore(effectiveLookbackSeconds)
                val audioFetchCostMs = System.currentTimeMillis() - audioFetchStartedAt
                if (pcm.isEmpty()) {
                    appendLog("语音识别失败：回溯音频为空（${effectiveLookbackSeconds}秒）")
                    return@launch
                }
                appendLog("回溯音频获取成功：${effectiveLookbackSeconds}s, 字节数=${pcm.size}")
                appendLog("识别耗时统计: 回溯取音=${audioFetchCostMs}ms")

                val pcmDurationSec =
                    pcm.size.toDouble() / (AudioFormatSpec.SAMPLE_RATE.toDouble() * AudioFormatSpec.FRAME_SIZE.toDouble())

                if (effectiveLookbackSeconds != startSettings.lookbackSeconds) {
                    appendLog(
                        "安静超时后自动回溯生效：安静${quietTimedOutDurationSec}s + ${startSettings.quietAutoLookbackExtraSeconds}s，实际回溯=${effectiveLookbackSeconds}s"
                    )
                }
                appendLog(
                    "开始语音识别：模式=${if (useCloudWhisper) "云端Whisper" else "本机模型"}, 回溯=${effectiveLookbackSeconds}s"
                )

                val asrStartedAt = System.currentTimeMillis()
                val text = asrService.recognize(
                    pcm16 = pcm,
                    useCloudWhisper = useCloudWhisper,
                    localAsrEnabled = useLocalAsr,
                    speechApiKey = startSettings.speechApiKey,
                    localModelId = resolvedAsrModelId,
                    cloudWhisperLanguage = if (resolveEffectiveLanguage(startSettings.appLanguage) == AppLanguage.ZH_CN) "zh" else "en",
                    enableVadGate = startSettings.vadEnabled,
                    onLog = { appendLog(it) }
                )
                val asrCostMs = System.currentTimeMillis() - asrStartedAt
                val totalCostMs = System.currentTimeMillis() - recognitionFlowStartedAt
                val rtf = if (pcmDurationSec <= 0.0) 0.0 else asrCostMs / (pcmDurationSec * 1000.0)
                appendLog(
                    "识别耗时统计: 音频=${String.format(Locale.ROOT, "%.2f", pcmDurationSec)}s, ASR=${asrCostMs}ms, 总耗时=${totalCostMs}ms, RTF=${String.format(Locale.ROOT, "%.2f", rtf)}"
                )

                if (text.isBlank()) {
                    appendLog("语音识别结果为空")
                } else {
                    mainHandler.post {
                        recognitionText = text
                    }
                    appendLog("语音识别文本: ${text.take(200)}")
                    appendLog("准备触发AI问答")
                    triggerAiAnswerAfterRecognition(text, startSettings)
                }
            } catch (t: Throwable) {
                appendLog("语音识别异常: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                asrInProgress.set(false)
                if (pendingWakeRecognition.compareAndSet(true, false)) {
                    if (isMonitoring && wakeWordEngine.isRunning()) {
                        appendLog("处理积压唤醒事件：自动补跑一次唤醒后识别")
                        triggerSpeechRecognitionAfterWake(settings)
                    } else {
                        appendLog("监听已停止，丢弃积压唤醒事件")
                    }
                }
            }
        }
    }

    fun startWakeWordDetectionFromNavigation(startSettings: SettingsData) {
        val keywords = parseKeywords(startSettings.keywords)
        if (keywords.isEmpty()) {
            statusText = "请先设置唤醒词"
            isMonitoring = false
            appendLog("未配置唤醒词，无法启动监听")
            AppCrashMonitor.clearListeningStage(context.applicationContext)
            return
        }

        isStarting = true
        statusText = "初始化中..."
        vadQuietStartMs.set(0L)
        vadTimeoutNotified.set(false)
        vadLastTimedOutQuietDurationSec.set(0L)
        appendLog("当前生效参数: threshold=${String.format("%.2f", startSettings.kwsThreshold)}（仅保存后的设置会生效）")
        if (startSettings.vadEnabled) {
            appendLog(
                "VAD 已启用: 安静阈值=${startSettings.quietThreshold}秒, 提醒方式=${if (startSettings.quietAlertMode == "SOUND") "系统通知+提示音" else "仅系统通知"}, 安静后自动回溯=${if (startSettings.quietAutoLookbackEnabled) "开(额外${startSettings.quietAutoLookbackExtraSeconds}s)" else "关"}"
            )
        } else {
            appendLog("VAD 已关闭")
        }
        appendActivePipelineLog("监听启动生效配置", startSettings)
        appendLog("说明：即使已下载多个唤醒模型，监听时仅使用当前模型")
        if (startSettings.backgroundKeepAliveEnabled) {
            startKeepAliveIfEnabled()
            appendLog("后台保活已启用")
        }
        val modelDir = modelManager.getModelDir(currentModelId)
        coroutineScope.launch(Dispatchers.Default) {
            val started = wakeWordEngine.start(
                modelDir = modelDir,
                keywords = keywords,
                keywordThreshold = startSettings.kwsThreshold,
                onWake = {
                    mainHandler.post {
                        statusText = "已唤醒"
                        appendLog("检测到唤醒词")
                        appendLog("唤醒事件：开始执行回溯识别流程")
                        val wakeActionSettings = settings
                        appendLog("唤醒事件：使用当前最新已保存设置执行语音识别与AI问答")
                        notifyWakeAlert()
                        triggerSpeechRecognitionAfterWake(wakeActionSettings)
                        wakeStatusResetRunnable?.let { mainHandler.removeCallbacks(it) }
                        wakeStatusResetRunnable = Runnable {
                            if (isMonitoring && statusText == "已唤醒") {
                                statusText = "监听中"
                            }
                        }
                        wakeStatusResetRunnable?.let { mainHandler.postDelayed(it, WAKE_STATUS_DISPLAY_MS) }
                    }
                },
                onLog = { message ->
                    coroutineScope.launch {
                        appendEngineLog(message)
                    }
                },
                onAudioLevel = { rawRms ->
                    if (!startSettings.vadEnabled) {
                        return@start
                    }
                    val now = System.currentTimeMillis()
                    if (rawRms < VAD_SPEECH_RMS_THRESHOLD) {
                        if (vadQuietStartMs.compareAndSet(0L, now)) {
                            appendEngineLog(
                                "VAD: 进入安静状态，rawRms=${"%.5f".format(Locale.ROOT, rawRms)}"
                            )
                        }
                        val silenceStartedAt = vadQuietStartMs.get()
                        val elapsedMs = now - silenceStartedAt
                        val thresholdMs = startSettings.quietThreshold * 1000L
                        if (silenceStartedAt > 0L && elapsedMs >= thresholdMs && vadTimeoutNotified.compareAndSet(false, true)) {
                            val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtLeast(startSettings.quietThreshold)
                            vadLastTimedOutQuietDurationSec.set(elapsedSec.toLong())
                            appendEngineLog("VAD: 安静超时，持续 ${elapsedSec}s（阈值 ${startSettings.quietThreshold}s）")
                            val shown = showQuietNotification(context.applicationContext, startSettings.quietAlertMode, elapsedSec)
                            if (!shown) {
                                mainHandler.post {
                                    Toast.makeText(context, "安静检测超时：已连续安静 ${elapsedSec} 秒", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        val silenceStartedAt = vadQuietStartMs.getAndSet(0L)
                        if (silenceStartedAt > 0L) {
                            val elapsedMs = now - silenceStartedAt
                            val hadTimeout = vadTimeoutNotified.getAndSet(false)
                            if (hadTimeout) {
                                val elapsedSec = (elapsedMs / 1000L).toInt().coerceAtLeast(startSettings.quietThreshold)
                                vadLastTimedOutQuietDurationSec.set(elapsedSec.toLong())
                            }
                            if (elapsedMs >= 1000L || hadTimeout) {
                                appendEngineLog("VAD: 检测到语音，安静结束（持续 ${"%.1f".format(Locale.ROOT, elapsedMs / 1000f)}s）")
                            }
                        }
                    }
                },
                enableTtsSelfTest = startSettings.ttsSelfTestEnabled
            )
            withContext(Dispatchers.Main) {
                isStarting = false
                if (!started) {
                    statusText = "启动失败"
                    isMonitoring = false
                    ListeningKeepAliveService.stop(context.applicationContext)
                    activeListeningSettings = null
                    activeListeningModelId = null
                    wakeStatusResetRunnable?.let { mainHandler.removeCallbacks(it) }
                    wakeStatusResetRunnable = null
                    appendLog("启动唤醒失败")
                    AppCrashMonitor.clearListeningStage(context.applicationContext)
                } else {
                    statusText = "监听中"
                    isMonitoring = true
                    activeListeningSettings = startSettings
                    activeListeningModelId = currentModelId
                    appendLog("启动唤醒成功")
                    AppCrashMonitor.markListeningStage(context.applicationContext, "WAKEWORD_RUNNING")
                }
            }
        }
    }

    fun startMonitoringSession() {
        val startSettings = settings
        val keywords = parseKeywords(startSettings.keywords)
        if (keywords.isEmpty()) {
            statusText = "请先设置唤醒词"
            appendLog("开始本地监听失败：未配置唤醒词")
            return
        }
        isMonitoring = true
        isStarting = false
        statusText = "监听中..."
        AppCrashMonitor.markListeningStage(context.applicationContext, "CLICK_START_LISTENING")
        appendLog("开始本地监听，唤醒词: ${startSettings.keywords.ifBlank { "未设置" }}")
        appendActivePipelineLog("开始监听前配置检查", startSettings)
        if (!currentModelReady) {
            appendLog("当前模型未就绪，准备下载")
            statusText = "下载模型中..."
            AppCrashMonitor.markListeningStage(context.applicationContext, "MODEL_DOWNLOADING")
        }
        ensureModelReady(
            currentModelId,
            { startWakeWordDetectionFromNavigation(startSettings) },
            { reason ->
                statusText = "空闲"
                isMonitoring = false
                activeListeningSettings = null
                activeListeningModelId = null
                appendLog("模型下载失败，原因: $reason")
                AppCrashMonitor.clearListeningStage(context.applicationContext)
            }
        )
    }

    fun reinitializeMonitoringSession(updatedSettings: SettingsData) {
        appendLog("检测到设置变更，正在重初始化监听并保持持续监听")
        wakeWordEngine.stop { appendLog(it) }
        if (updatedSettings.backgroundKeepAliveEnabled) {
            startKeepAliveIfEnabled()
        } else {
            ListeningKeepAliveService.stop(context.applicationContext)
        }
        isMonitoring = true
        isStarting = true
        statusText = "重新初始化中..."
        vadQuietStartMs.set(0L)
        vadTimeoutNotified.set(false)
        activeListeningSettings = null
        activeListeningModelId = null
        AppCrashMonitor.markListeningStage(context.applicationContext, "REINIT_AFTER_SETTINGS_SAVE")

        ensureModelReady(
            currentModelId,
            { startWakeWordDetectionFromNavigation(updatedSettings) },
            { reason ->
                statusText = "空闲"
                isMonitoring = false
                isStarting = false
                activeListeningSettings = null
                activeListeningModelId = null
                appendLog("重初始化失败，原因: $reason")
                AppCrashMonitor.clearListeningStage(context.applicationContext)
            }
        )
    }

    fun hasMonitoringConfigChanged(oldSettings: SettingsData?, newSettings: SettingsData, oldModelId: String?, newModelId: String): Boolean {
        if (oldSettings == null || oldModelId == null) {
            return true
        }
        return oldModelId != newModelId ||
            oldSettings.keywords != newSettings.keywords ||
            oldSettings.kwsThreshold != newSettings.kwsThreshold ||
            oldSettings.vadEnabled != newSettings.vadEnabled ||
            oldSettings.quietThreshold != newSettings.quietThreshold ||
            oldSettings.quietAlertMode != newSettings.quietAlertMode ||
            oldSettings.quietAutoLookbackEnabled != newSettings.quietAutoLookbackEnabled ||
            oldSettings.quietAutoLookbackExtraSeconds != newSettings.quietAutoLookbackExtraSeconds ||
            oldSettings.lookbackSeconds != newSettings.lookbackSeconds ||
            oldSettings.localAsrEnabled != newSettings.localAsrEnabled ||
            oldSettings.localAsrModelId != newSettings.localAsrModelId ||
            oldSettings.cloudWhisperEnabled != newSettings.cloudWhisperEnabled ||
            oldSettings.recordingSaveEnabled != newSettings.recordingSaveEnabled ||
            oldSettings.retentionDays != newSettings.retentionDays ||
            oldSettings.ttsSelfTestEnabled != newSettings.ttsSelfTestEnabled ||
            oldSettings.backgroundKeepAliveEnabled != newSettings.backgroundKeepAliveEnabled
    }

    fun runManualTtsSelfTest() {
        val allKeywords = parseKeywords(settings.keywords)
        if (allKeywords.isEmpty()) {
            appendLog("TTS合成自检: 请先在设置中配置至少一个唤醒词")
            return
        }
        val started = wakeWordEngine.runComprehensiveTtsSelfTest(allKeywords) { appendLog(it) }
        if (!started) {
            appendLog("TTS综合自检: 未启动（需先开始监听）")
        }
    }

    LaunchedEffect(Unit) {
        AppCrashMonitor.consumePendingSummary(context.applicationContext)?.let {
            appendLog(it)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val storageInstance = withContext(Dispatchers.IO) {
                AndroidSecureStorage(context.applicationContext)
            }
            secureStorage = storageInstance
            settings = withContext(Dispatchers.IO) {
                loadSettings(preferences, storageInstance)
            }
            settingsLoaded = true
        } catch (e: Exception) {
            settings = withContext(Dispatchers.IO) {
                loadSettings(preferences, null)
            }
            settingsLoaded = true
            appendLog("安全存储初始化失败，将继续使用非敏感设置: ${e.message ?: "未知错误"}")
        }
    }

    LaunchedEffect(logVersion) {
        try {
            val text = withContext(Dispatchers.IO) {
                val logFile = File(storage.getLogsDir(), "app.log")
                if (logFile.exists()) {
                    val lines = logFile.readLines()
                    val tail = if (lines.size > 500) lines.takeLast(500) else lines
                    tail.joinToString("\n")
                } else {
                    ""
                }
            }
            withContext(Dispatchers.Main) {
                logText = if (text.isBlank()) "日志输出..." else text
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppCrashMonitor.recordHandledError(
                context.applicationContext,
                "读取应用日志失败",
                e
            )
            logText = "日志读取失败：${e.message ?: "未知错误"}"
        }
    }

    LaunchedEffect(Unit) {
        try {
            val migrated = withContext(Dispatchers.IO) {
                modelManager.migrateLegacyModelIfNeeded(currentModelId)
            }
            if (migrated) {
                refreshModelStatus()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppCrashMonitor.recordHandledError(
                context.applicationContext,
                "迁移旧版KWS模型失败",
                e
            )
            appendLog("迁移旧版KWS模型失败: ${e.message ?: "未知错误"}")
        }
    }

    LaunchedEffect(settings.localAsrEnabled, settings.localAsrModelId) {
        try {
            val modelId = settings.localAsrModelId.ifBlank { defaultAsrModelId }
            if (!settings.localAsrEnabled) {
                appendLog("本机ASR已关闭：默认不下载本地识别模型")
                return@LaunchedEffect
            }
            if (lastAutoDownloadedAsrModelId == modelId) {
                return@LaunchedEffect
            }
            if (asrModelManager.isModelReady(modelId)) {
                refreshAsrModelStatus()
                lastAutoDownloadedAsrModelId = modelId
                appendLog("本机ASR模型已就绪：$modelId，跳过默认下载")
                return@LaunchedEffect
            }
            lastAutoDownloadedAsrModelId = modelId
            downloadAsrModel(modelId, "首次启用本机ASR默认下载")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppCrashMonitor.recordHandledError(
                context.applicationContext,
                "自动检查本机ASR模型失败",
                e
            )
            appendLog("自动检查本机ASR模型失败: ${e.message ?: "未知错误"}")
        }
    }

    LaunchedEffect(settingsLoaded, hasAutoCheckedUpdate) {
        if (!settingsLoaded || hasAutoCheckedUpdate) {
            return@LaunchedEffect
        }
        hasAutoCheckedUpdate = true
        checkForUpdate(manual = false)
    }

    if (showUpdateDialog && pendingUpdateInfo != null) {
        val release = pendingUpdateInfo!!
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = false
            },
            title = {
                Text(
                    t(
                        "发现新版本 ${release.tagName}",
                        "New version ${release.tagName} available"
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        t(
                            "当前版本：${currentAppVersionName(context.applicationContext)}",
                            "Current version: ${currentAppVersionName(context.applicationContext)}"
                        )
                    )
                    Text(
                        t(
                            "新版本：${release.title}",
                            "Latest release: ${release.title}"
                        )
                    )
                    Text(
                        t(
                            "说明：${release.notes.takeIf { it.isNotBlank() }?.take(240) ?: "暂无发布说明"}",
                            "Notes: ${release.notes.takeIf { it.isNotBlank() }?.take(240) ?: "No notes"}"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isInstallingUpdate,
                    onClick = {
                        installReleaseUpdate(release)
                    }
                ) {
                    Text(t("立即更新", "Update now"))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showUpdateDialog = false
                            markReleaseIgnored(release)
                            updateStatusMessage = t(
                                "已设置“下个新版本再提醒”",
                                "You will be reminded when a newer release appears"
                            )
                        }
                    ) {
                        Text(t("下个新版本再提醒", "Remind me next version"))
                    }
                    TextButton(
                        onClick = {
                            showUpdateDialog = false
                            updateStatusMessage = t("已取消本次更新", "Update dismissed")
                        }
                    ) {
                        Text(t("稍后", "Later"))
                    }
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            wakeWordEngine.stop()
            ListeningKeepAliveService.stop(context.applicationContext)
            AppCrashMonitor.clearListeningStage(context.applicationContext)
        }
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    appendLog("打开设置")
                    logVersion += 1
                    navController.navigate("settings")
                },
                settings = settings,
                logText = logText,
                isMonitoring = isMonitoring,
                isStarting = isStarting,
                statusText = statusText,
                onClearLogs = { clearLogs() },
                currentModelName = availableModels.firstOrNull { it.id == currentModelId }?.name ?: currentModelId,
                currentModelStatus = modelStatusMap[currentModelId] ?: "未下载",
                currentModelReady = currentModelReady,
                currentModelFailureReason = modelFailureMap[currentModelId].orEmpty(),
                currentModelProgress = modelProgressMap[currentModelId] ?: 0f,
                currentAsrRouteSummary = buildAsrRouteSummary(settings),
                isDownloading = isDownloading,
                recognitionText = recognitionText,
                aiAnswerText = aiAnswerText,
                isAiAnswering = isAiAnswering,
                onSendDirectQuestion = { question -> triggerAiAnswerForQuestion(question, settings) },
                onStartMonitoring = { startMonitoringSession() },
                onRunTtsSelfTest = { runManualTtsSelfTest() },
                onStopMonitoring = {
                    stopMonitoringSession("停止监听")
                },
                onAppendLog = { appendLog(it) },
                topExtraContent = {}
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                initialSettings = settings,
                onSave = { updatedSettings ->
                    val normalizedAsrModelId = when {
                        updatedSettings.localAsrModelId.isBlank() -> defaultAsrModelId
                        !updatedSettings.customModelEnabled && updatedSettings.localAsrModelId == customAsrModelId ->
                            availableAsrModels.firstOrNull { it.id != customAsrModelId }?.id ?: defaultAsrModelId
                        else -> updatedSettings.localAsrModelId
                    }
                    if (!updatedSettings.customModelEnabled && currentModelId == customKwsModelId) {
                        val fallbackKwsModelId = availableModels.firstOrNull { it.id != customKwsModelId }?.id ?: defaultModelId
                        currentModelId = fallbackKwsModelId
                        preferences.putString(KEY_KWS_MODEL_ID, fallbackKwsModelId)
                        preferences.flush()
                        currentModelReady = modelManager.isModelReady(fallbackKwsModelId)
                        selectedModelIds = selectedModelIds.filter { it != customKwsModelId }.toSet()
                        persistSelectedModels()
                    }
                    val normalizedSettings = updatedSettings.copy(localAsrModelId = normalizedAsrModelId)
                    val wasListening = isMonitoring || isStarting || wakeWordEngine.isRunning()
                    val configChanged = hasMonitoringConfigChanged(
                        oldSettings = activeListeningSettings,
                        newSettings = normalizedSettings,
                        oldModelId = activeListeningModelId,
                        newModelId = currentModelId
                    )
                    saveSettings(preferences, secureStorage, normalizedSettings)
                    settings = normalizedSettings
                    appendLog("保存设置成功（唤醒词触发值=${String.format("%.2f", normalizedSettings.kwsThreshold)}）")
                    appendActivePipelineLog("保存后生效配置", normalizedSettings)
                    if (wasListening) {
                        if (normalizedSettings.backgroundKeepAliveEnabled) {
                            startKeepAliveIfEnabled()
                        } else {
                            ListeningKeepAliveService.stop(context.applicationContext)
                        }
                    }
                    if (wasListening && configChanged) {
                        reinitializeMonitoringSession(normalizedSettings)
                    } else if (wasListening) {
                        appendLog("设置保存后参数未变化，继续保持当前监听")
                    }
                    val languageSavedMessage = when (normalizedSettings.appLanguage) {
                        AppLanguage.AUTO -> t("语言已设置为跟随系统", "Language set to follow system")
                        AppLanguage.ZH_CN -> t("语言已设置为中文", "Language set to Chinese")
                        AppLanguage.EN_US -> t("语言已设置为英文", "Language set to English")
                    }
                    updateStatusMessage = languageSavedMessage
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
                onRefreshModelStatus = { refreshModelStatus() },
                asrModelOptions = availableAsrModels,
                asrCurrentModelId = settings.localAsrModelId.ifBlank { defaultAsrModelId },
                asrModelStatusMap = asrModelStatusMap,
                asrModelProgressMap = asrModelProgressMap,
                asrModelFailureMap = asrModelFailureMap,
                isAsrDownloading = isDownloadingAsr,
                onDownloadAsrModel = { modelId -> downloadAsrModel(modelId) },
                onRefreshAsrModelStatus = { refreshAsrModelStatus() },
                onDownloadCustomKwsModel = { url -> downloadCustomKwsModel(url) },
                onDownloadCustomAsrModel = { url -> downloadCustomAsrModel(url) },
                onCheckUpdate = { checkForUpdate(manual = true) },
                updateStatusMessage = updateStatusMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    settings: SettingsData,
    logText: String,
    isMonitoring: Boolean,
    isStarting: Boolean,
    statusText: String,
    onClearLogs: () -> Unit,
    currentModelName: String,
    currentModelStatus: String,
    currentModelReady: Boolean,
    currentModelFailureReason: String,
    currentModelProgress: Float,
    currentAsrRouteSummary: String,
    isDownloading: Boolean,
    recognitionText: String,
    aiAnswerText: String,
    isAiAnswering: Boolean,
    onSendDirectQuestion: (String) -> Unit,
    onStartMonitoring: () -> Unit,
    onRunTtsSelfTest: () -> Unit,
    onStopMonitoring: () -> Unit,
    onAppendLog: (String) -> Unit,
    topExtraContent: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val latestSettings by rememberUpdatedState(settings)
    val isDebuggable = remember(context.applicationContext) {
        (context.applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    val listeningHeartbeatMs = if (isDebuggable) {
        DEBUG_LISTENING_HEARTBEAT_MS
    } else {
        RELEASE_LISTENING_HEARTBEAT_MS
    }

    val keywords = parseKeywords(latestSettings.keywords)
    val hasKeywords = keywords.isNotEmpty()
    val keywordsDisplay = if (hasKeywords) keywords.joinToString("，") else "未设置"
    var directQuestion by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(isMonitoring) {
        if (!isMonitoring) {
            return@LaunchedEffect
        }
        while (true) {
            delay(listeningHeartbeatMs)
            if (isDebuggable) {
                if (latestSettings.logMode == "FULL" && latestSettings.showHeartbeatLogs) {
                    onAppendLog("正在监听")
                }
            } else {
                Toast.makeText(context, "正在监听", Toast.LENGTH_SHORT).show()
            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            topExtraContent?.let { content -> item { content() } }
            // 状态卡片
            item {
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
                        val displayStatus = when {
                            isDownloading && !currentModelReady -> "下载模型中..."
                            !hasKeywords && !isMonitoring -> "请先设置唤醒词"
                            else -> statusText
                        }
                        Text(
                            text = displayStatus,
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
                            text = "唤醒词：$keywordsDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasKeywords) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "模型状态：$currentModelStatus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "语音识别路线：$currentAsrRouteSummary",
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
            }

            // 控制按钮
            item {
                Button(
                    onClick = {
                        if (isMonitoring) {
                            onStopMonitoring()
                            return@Button
                        }
                        onStartMonitoring()
                    },
                    enabled = !isDownloading && !isStarting && (hasKeywords || isMonitoring),
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
                        text = if (isMonitoring) "监听中（点击停止）" else if (isStarting) "初始化中..." else if (isDownloading) "下载模型中..." else "开始监听",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (latestSettings.ttsSelfTestEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onRunTtsSelfTest() },
                        enabled = isMonitoring && !isStarting && !isDownloading && hasKeywords,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("运行 TTS 综合自测")
                    }
                }
                if (!hasKeywords) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "提示：请先在设置中填写唤醒词，再开始监听",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 识别文本 + AI问答卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "最近识别文本",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (recognitionText.isBlank()) "暂无" else recognitionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI 直接问答",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = directQuestion,
                            onValueChange = { directQuestion = it },
                            label = { Text("输入你的问题") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val question = directQuestion
                                    directQuestion = ""
                                    onSendDirectQuestion(question)
                                },
                                enabled = directQuestion.isNotBlank() && !isAiAnswering,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isAiAnswering) "问答中..." else "发送")
                            }
                            OutlinedButton(
                                onClick = { directQuestion = "" },
                                enabled = directQuestion.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("清空")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI 回答",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (aiAnswerText.isBlank()) "暂无" else aiAnswerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 日志区域
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (logText.isBlank() || logText == "日志输出...") {
                                            Toast.makeText(context, "暂无日志可复制", Toast.LENGTH_SHORT).show()
                                        } else {
                                            clipboardManager.setText(AnnotatedString(logText))
                                            Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("复制日志")
                                }
                                OutlinedButton(
                                    onClick = { onClearLogs() }
                                ) {
                                    Text("清空日志")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = logText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 设置按钮
            item {
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
}
