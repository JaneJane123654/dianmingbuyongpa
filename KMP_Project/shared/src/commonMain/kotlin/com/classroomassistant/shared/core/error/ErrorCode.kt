package com.classroomassistant.shared.core.error

enum class ErrorCode(
    override val key: String,
    override val numericCode: Int?,
    override val defaultMessage: String,
    val category: AppErrorCategory,
    val retryableByDefault: Boolean = false,
    val requiresUserAction: Boolean = false,
    val gatesCapability: Boolean = false,
    val legacyAliases: Set<String> = emptySet(),
) : AppErrorCode {
    UNKNOWN(
        key = "unknown",
        numericCode = 1000,
        defaultMessage = "未知错误",
        category = AppErrorCategory.Unknown,
        legacyAliases = setOf("UNKNOWN_ERROR"),
    ),
    INITIALIZATION_FAILED(
        key = "initialization_failed",
        numericCode = 1001,
        defaultMessage = "初始化失败",
        category = AppErrorCategory.Configuration,
        requiresUserAction = true,
    ),
    CONFIG_INVALID(
        key = "config_invalid",
        numericCode = 1002,
        defaultMessage = "配置错误",
        category = AppErrorCategory.Configuration,
        requiresUserAction = true,
        gatesCapability = true,
        legacyAliases = setOf("CONFIGURATION_ERROR"),
    ),
    AUDIO_DEVICE_NOT_FOUND(
        key = "audio_device_not_found",
        numericCode = 2001,
        defaultMessage = "未找到音频设备",
        category = AppErrorCategory.Audio,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    AUDIO_PERMISSION_DENIED(
        key = "audio_permission_denied",
        numericCode = 2002,
        defaultMessage = "音频权限被拒绝",
        category = AppErrorCategory.Permission,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    AUDIO_CAPTURE_FAILED(
        key = "audio_capture_failed",
        numericCode = 2003,
        defaultMessage = "录音失败",
        category = AppErrorCategory.Audio,
        retryableByDefault = true,
        requiresUserAction = true,
        legacyAliases = setOf("AUDIO_RECORDING_FAILED"),
    ),
    SPEECH_ENGINE_INIT_FAILED(
        key = "speech_engine_init_failed",
        numericCode = 3001,
        defaultMessage = "语音引擎初始化失败",
        category = AppErrorCategory.Speech,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    SPEECH_RECOGNITION_FAILED(
        key = "speech_recognition_failed",
        numericCode = 3002,
        defaultMessage = "语音识别失败",
        category = AppErrorCategory.Speech,
        retryableByDefault = true,
    ),
    WAKE_WORD_DETECTION_FAILED(
        key = "wake_word_detection_failed",
        numericCode = 3003,
        defaultMessage = "唤醒词检测失败",
        category = AppErrorCategory.Speech,
        retryableByDefault = true,
    ),
    MODEL_LOAD_FAILED(
        key = "model_load_failed",
        numericCode = 3004,
        defaultMessage = "模型加载失败",
        category = AppErrorCategory.Speech,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    MODEL_NOT_FOUND(
        key = "model_not_found",
        numericCode = 3005,
        defaultMessage = "模型文件未找到",
        category = AppErrorCategory.Speech,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    AI_SERVICE_FAILED(
        key = "ai_service_failed",
        numericCode = 4001,
        defaultMessage = "大模型 API 错误",
        category = AppErrorCategory.AiService,
        legacyAliases = setOf("LLM_API_ERROR"),
    ),
    LLM_TIMEOUT(
        key = "llm_timeout",
        numericCode = 4002,
        defaultMessage = "大模型请求超时",
        category = AppErrorCategory.AiService,
        retryableByDefault = true,
    ),
    LLM_RATE_LIMITED(
        key = "llm_rate_limited",
        numericCode = 4003,
        defaultMessage = "大模型请求被限流",
        category = AppErrorCategory.AiService,
        retryableByDefault = true,
    ),
    LLM_INVALID_API_KEY(
        key = "llm_invalid_api_key",
        numericCode = 4004,
        defaultMessage = "无效的 API Key",
        category = AppErrorCategory.Configuration,
        requiresUserAction = true,
        gatesCapability = true,
    ),
    NETWORK_UNAVAILABLE(
        key = "network_unavailable",
        numericCode = 5001,
        defaultMessage = "网络不可用",
        category = AppErrorCategory.Network,
        retryableByDefault = true,
        requiresUserAction = true,
    ),
    NETWORK_TIMEOUT(
        key = "network_timeout",
        numericCode = 5002,
        defaultMessage = "网络超时",
        category = AppErrorCategory.Network,
        retryableByDefault = true,
    ),
    NETWORK_SSL_ERROR(
        key = "network_ssl_error",
        numericCode = 5003,
        defaultMessage = "SSL 证书错误",
        category = AppErrorCategory.Network,
        requiresUserAction = true,
    ),
    STORAGE_WRITE_FAILED(
        key = "storage_write_failed",
        numericCode = 6001,
        defaultMessage = "存储写入失败",
        category = AppErrorCategory.Storage,
    ),
    STORAGE_READ_FAILED(
        key = "storage_read_failed",
        numericCode = 6002,
        defaultMessage = "存储读取失败",
        category = AppErrorCategory.Storage,
    ),
    STORAGE_FULL(
        key = "storage_full",
        numericCode = 6003,
        defaultMessage = "存储空间不足",
        category = AppErrorCategory.Storage,
        requiresUserAction = true,
        gatesCapability = true,
    );

    companion object {
        private val keyIndex: Map<String, ErrorCode> = buildMap {
            for (value in ErrorCode.entries) {
                put(value.name, value)
                put(value.key, value)
                for (alias in value.legacyAliases) {
                    put(alias, value)
                }
            }
        }

        private val numericCodeIndex: Map<Int, ErrorCode> = ErrorCode.entries
            .filter { it.numericCode != null }
            .associateBy { it.numericCode!! }

        fun fromKeyOrNull(key: String?): ErrorCode? = key?.let(keyIndex::get)

        fun fromNumericCodeOrNull(code: Int?): ErrorCode? = code?.let(numericCodeIndex::get)

        fun descriptionForNumericCode(code: Int): String = when (code) {
            UNKNOWN.numericCode -> UNKNOWN.defaultMessage
            INITIALIZATION_FAILED.numericCode -> INITIALIZATION_FAILED.defaultMessage
            CONFIG_INVALID.numericCode -> CONFIG_INVALID.defaultMessage
            AUDIO_DEVICE_NOT_FOUND.numericCode -> AUDIO_DEVICE_NOT_FOUND.defaultMessage
            AUDIO_PERMISSION_DENIED.numericCode -> AUDIO_PERMISSION_DENIED.defaultMessage
            AUDIO_CAPTURE_FAILED.numericCode -> AUDIO_CAPTURE_FAILED.defaultMessage
            SPEECH_ENGINE_INIT_FAILED.numericCode -> SPEECH_ENGINE_INIT_FAILED.defaultMessage
            SPEECH_RECOGNITION_FAILED.numericCode -> SPEECH_RECOGNITION_FAILED.defaultMessage
            WAKE_WORD_DETECTION_FAILED.numericCode -> WAKE_WORD_DETECTION_FAILED.defaultMessage
            MODEL_LOAD_FAILED.numericCode -> MODEL_LOAD_FAILED.defaultMessage
            MODEL_NOT_FOUND.numericCode -> MODEL_NOT_FOUND.defaultMessage
            AI_SERVICE_FAILED.numericCode -> AI_SERVICE_FAILED.defaultMessage
            LLM_TIMEOUT.numericCode -> LLM_TIMEOUT.defaultMessage
            LLM_RATE_LIMITED.numericCode -> LLM_RATE_LIMITED.defaultMessage
            LLM_INVALID_API_KEY.numericCode -> LLM_INVALID_API_KEY.defaultMessage
            NETWORK_UNAVAILABLE.numericCode -> NETWORK_UNAVAILABLE.defaultMessage
            NETWORK_TIMEOUT.numericCode -> NETWORK_TIMEOUT.defaultMessage
            NETWORK_SSL_ERROR.numericCode -> NETWORK_SSL_ERROR.defaultMessage
            STORAGE_WRITE_FAILED.numericCode -> STORAGE_WRITE_FAILED.defaultMessage
            STORAGE_READ_FAILED.numericCode -> STORAGE_READ_FAILED.defaultMessage
            STORAGE_FULL.numericCode -> STORAGE_FULL.defaultMessage
            else -> "错误码: $code"
        }
    }
}
