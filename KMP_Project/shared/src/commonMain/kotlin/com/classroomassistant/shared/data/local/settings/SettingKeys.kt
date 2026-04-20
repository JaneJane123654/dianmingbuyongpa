package com.classroomassistant.shared.data.local.settings

enum class SettingsNamespace(
    val wireName: String,
) {
    CONFIG_DEFAULTS("config_defaults"),
    USER_PREFERENCES("user_preferences"),
    SECURE_CREDENTIALS("secure_credentials"),
}

sealed interface TypedSettingKey<T> {
    val namespace: SettingsNamespace
    val storageKey: String
    val defaultValue: T
}

data class StringSettingKey(
    override val namespace: SettingsNamespace,
    override val storageKey: String,
    override val defaultValue: String,
) : TypedSettingKey<String>

data class IntSettingKey(
    override val namespace: SettingsNamespace,
    override val storageKey: String,
    override val defaultValue: Int,
) : TypedSettingKey<Int>

data class LongSettingKey(
    override val namespace: SettingsNamespace,
    override val storageKey: String,
    override val defaultValue: Long,
) : TypedSettingKey<Long>

data class BooleanSettingKey(
    override val namespace: SettingsNamespace,
    override val storageKey: String,
    override val defaultValue: Boolean,
) : TypedSettingKey<Boolean>

data class SecureSettingKey(
    val storageKey: String,
)

object ConfigDefaultSettingKeys {
    private val namespace = SettingsNamespace.CONFIG_DEFAULTS

    val AudioSampleRate = IntSettingKey(namespace, "audio.sampleRate", 16000)
    val AudioChannels = IntSettingKey(namespace, "audio.channels", 1)
    val AudioBitsPerSample = IntSettingKey(namespace, "audio.bitsPerSample", 16)
    val AudioFrameMillis = IntSettingKey(namespace, "audio.frameMillis", 20)
    val AudioLookbackSecondsDefault = IntSettingKey(namespace, "audio.lookbackSeconds.default", 240)
    val VadEnabledDefault = BooleanSettingKey(namespace, "vad.enabled.default", true)
    val VadQuietThresholdSecondsDefault = IntSettingKey(namespace, "vad.quietThresholdSeconds.default", 5)
    val AiTimeoutSecondsDefault = IntSettingKey(namespace, "ai.timeoutSeconds.default", 30)
    val AiMaxRetryCountDefault = IntSettingKey(namespace, "ai.maxRetryCount.default", 3)
    val AiStreamingDefault = BooleanSettingKey(namespace, "ai.streaming.default", true)
    val AiProviderDefault = StringSettingKey(namespace, "ai.provider.default", "QIANFAN")
    val AiModelNameDefault = StringSettingKey(namespace, "ai.modelName.default", "")
    val TriggerCooldownSeconds = IntSettingKey(namespace, "app.triggerCooldownSeconds", 10)
    val SpeechEngineDefault = StringSettingKey(namespace, "speech.engine.default", "API")
    val SpeechApiUrl = StringSettingKey(
        namespace,
        "speech.api.url",
        "https://api.groq.com/openai/v1/audio/transcriptions",
    )
    val SpeechApiKey = StringSettingKey(namespace, "speech.api.key", "")
    val SpeechApiModel = StringSettingKey(namespace, "speech.api.model", "whisper-large-v3")
    val ModelDownloadBaseUrl = StringSettingKey(
        namespace,
        "model.download.baseUrl",
        "https://github.com/k2-fsa/sherpa-onnx/releases/download",
    )
    val KwsModelName = StringSettingKey(
        namespace,
        "model.download.kwsModel",
        "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01",
    )
    val AsrModelName = StringSettingKey(
        namespace,
        "model.download.asrModel",
        "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20",
    )
    val VadModelName = StringSettingKey(namespace, "model.download.vadModel", "silero_vad.onnx")

    val all: List<TypedSettingKey<*>> = listOf(
        AudioSampleRate,
        AudioChannels,
        AudioBitsPerSample,
        AudioFrameMillis,
        AudioLookbackSecondsDefault,
        VadEnabledDefault,
        VadQuietThresholdSecondsDefault,
        AiTimeoutSecondsDefault,
        AiMaxRetryCountDefault,
        AiStreamingDefault,
        AiProviderDefault,
        AiModelNameDefault,
        TriggerCooldownSeconds,
        SpeechEngineDefault,
        SpeechApiUrl,
        SpeechApiKey,
        SpeechApiModel,
        ModelDownloadBaseUrl,
        KwsModelName,
        AsrModelName,
        VadModelName,
    )
}

object PreferenceSettingKeys {
    private val namespace = SettingsNamespace.USER_PREFERENCES

    val Keywords = StringSettingKey(namespace, "user.keywords", "")
    val KwsTriggerThresholdPercent = IntSettingKey(namespace, "speech.kws.triggerThreshold", 5)
    val AudioLookbackSeconds = IntSettingKey(namespace, "audio.lookbackSeconds", 15)
    val VadEnabled = BooleanSettingKey(namespace, "vad.enabled", true)
    val VadQuietThresholdSeconds = IntSettingKey(namespace, "vad.quietThresholdSeconds", 5)
    val VadQuietAlertMode = StringSettingKey(namespace, "vad.quietAlertMode", "NOTIFICATION_ONLY")
    val VadQuietAutoLookbackEnabled = BooleanSettingKey(namespace, "vad.quietAutoLookbackEnabled", true)
    val VadQuietAutoLookbackExtraSeconds = IntSettingKey(namespace, "vad.quietAutoLookbackExtraSeconds", 8)
    val AiProvider = StringSettingKey(namespace, "ai.provider", "QIANFAN")
    val AiModelName = StringSettingKey(namespace, "ai.modelName", "")
    val AiBaseUrl = StringSettingKey(namespace, "ai.baseUrl", "")
    val RecordingSaveEnabled = BooleanSettingKey(namespace, "recording.saveEnabled", false)
    val RecordingRetentionDays = IntSettingKey(namespace, "recording.retentionDays", 7)
    val AsrLocalEnabled = BooleanSettingKey(namespace, "speech.asr.local.enabled", true)
    val AsrLocalModelId = StringSettingKey(namespace, "speech.asr.local.model.id", "")
    val AsrCloudWhisperEnabled = BooleanSettingKey(namespace, "speech.asr.cloud.whisper.enabled", false)
    val KwsSelectedModels = StringSettingKey(namespace, "kws.selectedModels", "")
    val KwsCurrentModel = StringSettingKey(namespace, "kws.currentModel", "")
    val AsrSelected = BooleanSettingKey(namespace, "asr.selected", true)
    val VadSelected = BooleanSettingKey(namespace, "vad.selected", true)
    val WakeAlertMode = StringSettingKey(namespace, "speech.kws.wakeAlertMode", "NOTIFICATION_ONLY")
    val LogMode = StringSettingKey(namespace, "developer.log.mode", "SIMPLE")
    val LogShowDiagnostic = BooleanSettingKey(namespace, "developer.log.showDiagnostic", false)
    val LogShowAudioDevice = BooleanSettingKey(namespace, "developer.log.showAudioDevice", false)
    val LogShowGainActivity = BooleanSettingKey(namespace, "developer.log.showGainActivity", false)
    val LogShowTtsSelfTest = BooleanSettingKey(namespace, "developer.log.showTtsSelfTest", false)
    val LogShowHeartbeat = BooleanSettingKey(namespace, "developer.log.showHeartbeat", false)
    val TtsSelfTestEnabled = BooleanSettingKey(namespace, "developer.tts.selfTest.enabled", false)
    val BackgroundKeepAliveEnabled = BooleanSettingKey(
        namespace,
        "listening.backgroundKeepAliveEnabled",
        true,
    )

    val all: List<TypedSettingKey<*>> = listOf(
        Keywords,
        KwsTriggerThresholdPercent,
        AudioLookbackSeconds,
        VadEnabled,
        VadQuietThresholdSeconds,
        VadQuietAlertMode,
        VadQuietAutoLookbackEnabled,
        VadQuietAutoLookbackExtraSeconds,
        AiProvider,
        AiModelName,
        AiBaseUrl,
        RecordingSaveEnabled,
        RecordingRetentionDays,
        AsrLocalEnabled,
        AsrLocalModelId,
        AsrCloudWhisperEnabled,
        KwsSelectedModels,
        KwsCurrentModel,
        AsrSelected,
        VadSelected,
        WakeAlertMode,
        LogMode,
        LogShowDiagnostic,
        LogShowAudioDevice,
        LogShowGainActivity,
        LogShowTtsSelfTest,
        LogShowHeartbeat,
        TtsSelfTestEnabled,
        BackgroundKeepAliveEnabled,
    )
}

object SecurePreferenceKeys {
    val AiToken = SecureSettingKey("ai.token.encrypted")
    val AiSecretKey = SecureSettingKey("ai.secret.encrypted")
    val SpeechApiKey = SecureSettingKey("speech.apiKey.encrypted")

    val all: List<SecureSettingKey> = listOf(
        AiToken,
        AiSecretKey,
        SpeechApiKey,
    )
}
