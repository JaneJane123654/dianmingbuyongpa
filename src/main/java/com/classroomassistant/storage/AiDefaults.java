package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;

/**
 * AI 默认配置（值对象）
 */
public record AiDefaults(
    int timeoutSeconds,
    int maxRetryCount,
    boolean streamingDefault,
    LLMConfig.ModelType providerDefault,
    String modelNameDefault
) {
}

