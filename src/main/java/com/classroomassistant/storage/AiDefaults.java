package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;

import java.util.Objects;

/**
 * AI 服务默认配置 (AI Service Default Configuration)
 *
 * <p>作为一个不可变的值对象，它封装了从系统配置文件加载的 AI 相关默认参数。
 * 这些参数在用户尚未在偏好设置中进行个性化配置时作为回退（Fallback）值使用。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class AiDefaults {

    private final int timeoutSeconds;
    private final int maxRetryCount;
    private final boolean streamingDefault;
    private final LLMConfig.ModelType providerDefault;
    private final String modelNameDefault;

    /**
     * 构造 AI 默认配置
     *
     * @param timeoutSeconds   默认请求超时时间（秒）
     * @param maxRetryCount    默认最大重试次数
     * @param streamingDefault 默认是否启用流式响应
     * @param providerDefault  默认的模型供应商类型（如 QIANFAN）
     * @param modelNameDefault 默认的模型名称
     */
    public AiDefaults(int timeoutSeconds, int maxRetryCount, boolean streamingDefault,
                      LLMConfig.ModelType providerDefault, String modelNameDefault) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetryCount = maxRetryCount;
        this.streamingDefault = streamingDefault;
        this.providerDefault = providerDefault;
        this.modelNameDefault = modelNameDefault;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public int maxRetryCount() {
        return maxRetryCount;
    }

    public boolean streamingDefault() {
        return streamingDefault;
    }

    public LLMConfig.ModelType providerDefault() {
        return providerDefault;
    }

    public String modelNameDefault() {
        return modelNameDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AiDefaults)) return false;
        AiDefaults that = (AiDefaults) o;
        return timeoutSeconds == that.timeoutSeconds
                && maxRetryCount == that.maxRetryCount
                && streamingDefault == that.streamingDefault
                && providerDefault == that.providerDefault
                && Objects.equals(modelNameDefault, that.modelNameDefault);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeoutSeconds, maxRetryCount, streamingDefault, providerDefault, modelNameDefault);
    }

    @Override
    public String toString() {
        return "AiDefaults{" +
                "timeoutSeconds=" + timeoutSeconds +
                ", maxRetryCount=" + maxRetryCount +
                ", streamingDefault=" + streamingDefault +
                ", providerDefault=" + providerDefault +
                ", modelNameDefault='" + modelNameDefault + '\'' +
                '}';
    }
}

