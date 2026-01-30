package com.classroomassistant.ai;

import java.time.Duration;
import java.util.Objects;

/**
 * 大模型配置
 *
 * <p>用于描述模型类型、模型名称、API Token 等信息。
 */
public class LLMConfig {

    public enum ModelType {
        OPENAI,
        QIANFAN,
        DEEPSEEK,
        KIMI
    }

    private final ModelType modelType;
    private final String modelName;
    private final String apiKey;
    private final Duration timeout;
    private final int maxRetryCount;
    private final boolean streaming;

    private LLMConfig(
        ModelType modelType,
        String modelName,
        String apiKey,
        Duration timeout,
        int maxRetryCount,
        boolean streaming
    ) {
        this.modelType = modelType;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.maxRetryCount = maxRetryCount;
        this.streaming = streaming;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ModelType getModelType() {
        return modelType;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public static final class Builder {

        private ModelType modelType = ModelType.QIANFAN;
        private String modelName = "";
        private String apiKey = "";
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetryCount = 3;
        private boolean streaming = true;

        private Builder() {
        }

        public Builder modelType(ModelType modelType) {
            this.modelType = Objects.requireNonNullElse(modelType, ModelType.QIANFAN);
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName == null ? "" : modelName.trim();
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNullElse(timeout, Duration.ofSeconds(30));
            return this;
        }

        public Builder maxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public LLMConfig build() {
            return new LLMConfig(modelType, modelName, apiKey, timeout, maxRetryCount, streaming);
        }
    }
}
