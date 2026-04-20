package com.classroomassistant.shared.domain.model

enum class LlmProvider {
    OPENAI,
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    GEMINI,
    DEEPSEEK,
    QIANFAN,
    KIMI,
    DASHSCOPE,
    HUNYUAN,
    ZHIPU,
    SILICONFLOW,
    MINIMAX,
    MISTRAL,
    GROQ,
    COHERE,
    OPENROUTER,
    AZURE_OPENAI,
    BAICHUAN,
    YI,
    STEPFUN,
    XAI,
    FIREWORKS,
    TOGETHER_AI,
    PERPLEXITY,
    NOVITA,
    REPLICATE,
    CEREBRAS,
    SAMBANOVA,
    OLLAMA,
    LMSTUDIO,
    ;

    companion object {
        fun fromStorageValue(value: String): LlmProvider = valueOf(value)

        fun fromStorageValueOrNull(value: String?): LlmProvider? = if (value == null) {
            null
        } else {
            runCatching { fromStorageValue(value) }.getOrNull()
        }
    }
}
