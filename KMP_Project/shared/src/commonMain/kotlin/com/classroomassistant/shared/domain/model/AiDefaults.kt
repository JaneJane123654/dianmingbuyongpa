package com.classroomassistant.shared.domain.model

data class AiDefaults(
    val timeoutSeconds: Int,
    val maxRetryCount: Int,
    val streamingDefault: Boolean,
    val providerDefault: LlmProvider,
    val modelNameDefault: String,
) {
    val streaming: Boolean
        get() = streamingDefault

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30
        const val DEFAULT_MAX_RETRY_COUNT = 3
        const val DEFAULT_STREAMING = true
        const val DEFAULT_MODEL_NAME = ""

        val Default = AiDefaults(
            timeoutSeconds = DEFAULT_TIMEOUT_SECONDS,
            maxRetryCount = DEFAULT_MAX_RETRY_COUNT,
            streamingDefault = DEFAULT_STREAMING,
            providerDefault = LlmProvider.QIANFAN,
            modelNameDefault = DEFAULT_MODEL_NAME,
        )
    }
}
