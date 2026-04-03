package com.classroomassistant.ai;

import java.time.Duration;
import java.util.Objects;

/**
 * 大模型配置 (LLM Configuration)
 *
 * <p>用于描述模型类型、模型名称、API Token、超时时间、重试次数及是否启用流式输出等信息。
 * 推荐使用内置的 {@link Builder} 进行对象构建。
 *
 * <p>使用示例：
 * <pre>
 * LLMConfig config = LLMConfig.builder()
 *     .modelType(LLMConfig.ModelType.QIANFAN)
 *     .modelName("ernie-bot-4")
 *     .apiKey("your-api-key")
 *     .timeout(Duration.ofSeconds(60))
 *     .build();
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class LLMConfig {

    /**
     * 支持的大模型类型枚举
     */
    public enum ModelType {
        /** OpenAI 系列模型 */
        OPENAI,
        /** OpenAI 兼容中转站（自定义 Base URL） */
        OPENAI_COMPATIBLE,
        /** Anthropic Claude */
        ANTHROPIC,
        /** Google Gemini（OpenAI兼容入口） */
        GEMINI,
        /** DeepSeek 系列模型 */
        DEEPSEEK,
        /** 百度千帆（文心一言）系列模型 */
        QIANFAN,
        /** 月之暗面 (Kimi) 系列模型 */
        KIMI,
        /** 阿里云百炼 / 通义千问（兼容模式） */
        DASHSCOPE,
        /** 腾讯混元（兼容模式） */
        HUNYUAN,
        /** 智谱 AI（兼容模式） */
        ZHIPU,
        /** 硅基流动 SiliconFlow */
        SILICONFLOW,
        /** MiniMax */
        MINIMAX,
        /** Mistral */
        MISTRAL,
        /** Groq */
        GROQ,
        /** Cohere */
        COHERE,
        /** OpenRouter */
        OPENROUTER,
        /** Azure OpenAI */
        AZURE_OPENAI,
        /** 百川智能 */
        BAICHUAN,
        /** 零一万物 Yi */
        YI,
        /** 阶跃星辰 StepFun */
        STEPFUN,
        /** xAI (Grok) */
        XAI,
        /** Fireworks AI */
        FIREWORKS,
        /** Together AI */
        TOGETHER_AI,
        /** Perplexity */
        PERPLEXITY,
        /** Novita */
        NOVITA,
        /** Replicate */
        REPLICATE,
        /** Cerebras */
        CEREBRAS,
        /** SambaNova */
        SAMBANOVA,
        /** Ollama（本地） */
        OLLAMA,
        /** LM Studio（本地） */
        LMSTUDIO
    }

    private final ModelType modelType;
    private final String modelName;
    private final String apiKey;
    private final String secretKey;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetryCount;
    private final boolean streaming;

    private LLMConfig(
        ModelType modelType,
        String modelName,
        String apiKey,
        String secretKey,
        String baseUrl,
        Duration timeout,
        int maxRetryCount,
        boolean streaming
    ) {
        this.modelType = modelType;
        this.modelName = modelName;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetryCount = maxRetryCount;
        this.streaming = streaming;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取模型类型
     *
     * @return {@link ModelType}
     */
    public ModelType getModelType() {
        return modelType;
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称字符串
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 获取 API Key
     *
     * @return API Key 字符串
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 获取 Secret Key（用于百度千帆等需要双密钥的平台）
     *
     * @return Secret Key 字符串
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * 获取自定义 API 基础地址
     *
     * @return 基础地址字符串，为空时使用默认地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 获取超时时间
     *
     * @return {@link Duration} 超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 获取最大重试次数
     *
     * @return 重试次数
     */
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    /**
     * 是否启用流式输出
     *
     * @return true 表示启用流式输出
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * LLMConfig 构建器
     */
    public static final class Builder {

        private ModelType modelType = ModelType.QIANFAN;
        private String modelName = "";
        private String apiKey = "";
        private String secretKey = "";
        private String baseUrl = "";
        private Duration timeout = Duration.ofSeconds(30);
        private int maxRetryCount = 3;
        private boolean streaming = true;

        private Builder() {
        }

        /**
         * 设置模型类型
         *
         * @param modelType 模型类型
         * @return 当前 Builder 实例
         */
        public Builder modelType(ModelType modelType) {
            this.modelType = Objects.requireNonNullElse(modelType, ModelType.QIANFAN);
            return this;
        }

        /**
         * 设置模型名称
         *
         * @param modelName 模型名称
         * @return 当前 Builder 实例
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName == null ? "" : modelName.trim();
            return this;
        }

        /**
         * 设置 API Key
         *
         * @param apiKey API Key
         * @return 当前 Builder 实例
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            return this;
        }

        /**
         * 设置 Secret Key（用于百度千帆等平台）
         *
         * @param secretKey Secret Key
         * @return 当前 Builder 实例
         */
        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey == null ? "" : secretKey.trim();
            return this;
        }

        /**
         * 设置自定义 API 基础地址
         *
         * @param baseUrl 基础地址（如 https://api.deepseek.com）
         * @return 当前 Builder 实例
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
            return this;
        }

        /**
         * 设置超时时间
         *
         * @param timeout 超时时间
         * @return 当前 Builder 实例
         */
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNullElse(timeout, Duration.ofSeconds(30));
            return this;
        }

        /**
         * 设置最大重试次数
         *
         * @param maxRetryCount 重试次数
         * @return 当前 Builder 实例
         */
        public Builder maxRetryCount(int maxRetryCount) {
            this.maxRetryCount = Math.max(0, maxRetryCount);
            return this;
        }

        /**
         * 设置是否启用流式输出
         *
         * @param streaming true 表示启用
         * @return 当前 Builder 实例
         */
        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        /**
         * 构建 LLMConfig 实例
         *
         * @return {@link LLMConfig} 实例
         */
        public LLMConfig build() {
            return new LLMConfig(modelType, modelName, apiKey, secretKey, baseUrl, timeout, maxRetryCount, streaming);
        }
    }
}
