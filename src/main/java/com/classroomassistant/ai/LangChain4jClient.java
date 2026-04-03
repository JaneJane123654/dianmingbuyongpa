package com.classroomassistant.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanStreamingChatModel;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j 客户端实现 (LangChain4j Client Implementation)
 *
 * <p>基于 LangChain4j 框架实现。统一封装了对不同 LLM 厂商（如 OpenAI、百度千帆等）的调用逻辑。
 * 本类支持：
 * <ul>
 *   <li>同步/异步回答生成。</li>
 *   <li>流式输出：通过 {@link AnswerListener#onToken(String)} 实时回调内容片段。</li>
 *   <li>自动重试：在网络波动或服务异常时，根据配置执行有限次数的重试。</li>
 *   <li>熔断保护：集成 {@link CircuitBreaker}，防止持续失败的服务拖垮系统。</li>
 * </ul>
 *
 * <p>注意：使用完毕后应调用 {@link #close()} 释放线程池资源。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class LangChain4jClient implements LLMClient, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jClient.class);
    private static final Map<LLMConfig.ModelType, String> DEFAULT_BASE_URLS = buildDefaultBaseUrls();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "langchain4j-client");
        thread.setDaemon(true);
        return thread;
    });

    private ChatLanguageModel chatModel;
    private StreamingChatLanguageModel streamingModel;
    private LLMConfig config;
    private CircuitBreaker circuitBreaker;
    private int maxRetryCount;

    /**
     * 根据配置初始化模型客户端
     *
     * @param config 模型配置，包含 API Key、模型名称、超时时间等
     * @throws NullPointerException 如果 config 为 null
     */
    @Override
    public void configure(LLMConfig config) {
        this.config = Objects.requireNonNull(config, "模型配置不能为空");
        this.circuitBreaker = new CircuitBreaker(Math.max(1, config.getMaxRetryCount()), java.time.Duration.ofSeconds(10));
        this.maxRetryCount = Math.max(0, config.getMaxRetryCount());
        this.chatModel = createModel(config);
        this.streamingModel = config.isStreaming() ? createStreamingModel(config) : null;
    }

    /**
     * 同步生成回答
     *
     * @param prompt 提示词
     * @return AI 生成的完整文本。如果被熔断或参数为空，则返回空字符串。
     * @throws RuntimeException 如果所有重试均失败
     */
    @Override
    public String generateAnswer(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        if (!circuitBreaker.allowRequest()) {
            return "";
        }
        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= maxRetryCount; attempt++) {
            try {
                String answer = chatModel.generate(prompt);
                circuitBreaker.recordSuccess();
                return answer == null ? "" : answer;
            } catch (RuntimeException e) {
                lastError = e;
                circuitBreaker.recordFailure();
                logModelError("同步调用", attempt + 1, e);
                if (attempt < maxRetryCount) {
                    sleepRetry();
                }
            }
        }
        logger.error("[AI_SERVICE_FAILED] 模型同步调用耗尽重试次数，模型类型={}, 模型名称={}, 最后错误={}",
            config != null ? config.getModelType() : "null",
            config != null ? config.getModelName() : "null",
            lastError != null ? lastError.getMessage() : "unknown");
        throw lastError == null ? new IllegalStateException("模型调用失败") : lastError;
    }

    /**
     * 异步生成回答（支持流式输出）
     *
     * @param prompt   提示词
     * @param listener 监听器，用于接收 Token 和结果
     * @throws NullPointerException 如果 listener 为 null
     */
    @Override
    public void generateAnswerAsync(String prompt, AnswerListener listener) {
        Objects.requireNonNull(listener, "监听器不能为空");
        if (prompt == null || prompt.isBlank()) {
            listener.onComplete("");
            return;
        }
        if (!circuitBreaker.allowRequest()) {
            listener.onComplete("");
            return;
        }
        if (config != null && config.isStreaming() && streamingModel != null) {
            streamAnswerAsync(prompt, listener);
            return;
        }
        executor.submit(() -> {
            try {
                String answer = generateAnswer(prompt);
                if (answer.isBlank()) {
                    listener.onComplete("");
                    return;
                }
                for (String token : answer.split("")) {
                    listener.onToken(token);
                }
                listener.onComplete(answer);
            } catch (Exception e) {
                listener.onError("模型调用失败: " + e.getMessage());
            }
        });
    }

    /**
     * 创建对应的聊天模型实例（同步模式）
     *
     * @param config 配置信息
     * @return {@link ChatLanguageModel} 实例
     */
    protected ChatLanguageModel createModel(LLMConfig config) {
        String modelName = resolveModelName(config);
        LLMConfig.ModelType type = config.getModelType();

        // 百度千帆
        if (type == LLMConfig.ModelType.QIANFAN) {
            var builder = QianfanChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(modelName);
            if (config.getSecretKey() != null && !config.getSecretKey().isBlank()) {
                builder.secretKey(config.getSecretKey());
            }
            return builder.build();
        }

        return buildOpenAiCompatibleModel(config, modelName, type);
    }

    /**
     * 创建对应的流式聊天模型实例（流式模式）
     *
     * @param config 配置信息
     * @return {@link StreamingChatLanguageModel} 实例，如果不支持则返回 null
     */
    protected StreamingChatLanguageModel createStreamingModel(LLMConfig config) {
        String modelName = resolveModelName(config);
        LLMConfig.ModelType type = config.getModelType();

        // 百度千帆
        if (type == LLMConfig.ModelType.QIANFAN) {
            var builder = QianfanStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(modelName);
            if (config.getSecretKey() != null && !config.getSecretKey().isBlank()) {
                builder.secretKey(config.getSecretKey());
            }
            return builder.build();
        }

        return buildOpenAiCompatibleStreamingModel(config, modelName, type);
    }

    private ChatLanguageModel buildOpenAiCompatibleModel(LLMConfig config, String modelName, LLMConfig.ModelType type) {
        var builder = OpenAiChatModel.builder()
            .apiKey(config.getApiKey())
            .modelName(modelName);
        String defaultBaseUrl = DEFAULT_BASE_URLS.get(type);
        String resolvedBaseUrl = resolveBaseUrl(config, defaultBaseUrl);
        if (resolvedBaseUrl != null && !resolvedBaseUrl.isBlank()) {
            builder.baseUrl(resolvedBaseUrl);
        }
        return builder.build();
    }

    private StreamingChatLanguageModel buildOpenAiCompatibleStreamingModel(
        LLMConfig config,
        String modelName,
        LLMConfig.ModelType type
    ) {
        var builder = OpenAiStreamingChatModel.builder()
            .apiKey(config.getApiKey())
            .modelName(modelName);
        String defaultBaseUrl = DEFAULT_BASE_URLS.get(type);
        String resolvedBaseUrl = resolveBaseUrl(config, defaultBaseUrl);
        if (resolvedBaseUrl != null && !resolvedBaseUrl.isBlank()) {
            builder.baseUrl(resolvedBaseUrl);
        }
        return builder.build();
    }

    /**
     * 自动解析模型名称，如果配置中未提供，则根据厂商类型选择默认值
     */
    private String resolveModelName(LLMConfig config) {
        if (config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName();
        }
        switch (config.getModelType()) {
            case OPENAI:
                return "gpt-4o-mini";
            case OPENAI_COMPATIBLE:
                return "gpt-4o-mini";
            case ANTHROPIC:
                return "claude-3-5-sonnet-20241022";
            case GEMINI:
                return "gemini-2.0-flash";
            case QIANFAN:
                return "ernie-4.0-8k";
            case DEEPSEEK:
                return "deepseek-chat";
            case KIMI:
                return "moonshot-v1-8k";
            case DASHSCOPE:
                return "qwen-plus";
            case HUNYUAN:
                return "hunyuan-lite";
            case ZHIPU:
                return "glm-4-flash";
            case SILICONFLOW:
                return "Qwen/Qwen2.5-7B-Instruct";
            case MINIMAX:
                return "abab6.5s-chat";
            case MISTRAL:
                return "mistral-small-latest";
            case GROQ:
                return "llama-3.3-70b-versatile";
            case COHERE:
                return "command-r-plus";
            case OPENROUTER:
                return "openai/gpt-4o-mini";
            case AZURE_OPENAI:
                return "gpt-4o-mini";
            case BAICHUAN:
                return "Baichuan4";
            case YI:
                return "yi-large";
            case STEPFUN:
                return "step-2";
            case XAI:
                return "grok-2-1212";
            case FIREWORKS:
                return "accounts/fireworks/models/llama-v3p1-70b-instruct";
            case TOGETHER_AI:
                return "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo";
            case PERPLEXITY:
                return "sonar";
            case NOVITA:
                return "meta-llama/llama-3.1-70b-instruct";
            case REPLICATE:
                return "meta/meta-llama-3-70b-instruct";
            case CEREBRAS:
                return "llama3.1-8b";
            case SAMBANOVA:
                return "Meta-Llama-3.1-70B-Instruct";
            case OLLAMA:
                return "qwen2.5:7b";
            case LMSTUDIO:
                return "local-model";
            default:
                return "gpt-4o-mini";
        }
    }

    /**
     * 解析 baseUrl，优先使用配置中的值，否则使用默认值
     */
    private String resolveBaseUrl(LLMConfig config, String defaultUrl) {
        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            return config.getBaseUrl();
        }
        return defaultUrl == null ? "" : defaultUrl;
    }

    private static Map<LLMConfig.ModelType, String> buildDefaultBaseUrls() {
        Map<LLMConfig.ModelType, String> defaults = new EnumMap<>(LLMConfig.ModelType.class);
        defaults.put(LLMConfig.ModelType.OPENAI, "");
        defaults.put(LLMConfig.ModelType.OPENAI_COMPATIBLE, "");
        defaults.put(LLMConfig.ModelType.ANTHROPIC, "https://api.anthropic.com/v1");
        defaults.put(LLMConfig.ModelType.GEMINI, "https://generativelanguage.googleapis.com/v1beta/openai");
        defaults.put(LLMConfig.ModelType.DEEPSEEK, "https://api.deepseek.com");
        defaults.put(LLMConfig.ModelType.KIMI, "https://api.moonshot.cn/v1");
        defaults.put(LLMConfig.ModelType.DASHSCOPE, "https://dashscope.aliyuncs.com/compatible-mode/v1");
        defaults.put(LLMConfig.ModelType.HUNYUAN, "https://api.hunyuan.cloud.tencent.com/v1");
        defaults.put(LLMConfig.ModelType.ZHIPU, "https://open.bigmodel.cn/api/paas/v4");
        defaults.put(LLMConfig.ModelType.SILICONFLOW, "https://api.siliconflow.cn/v1");
        defaults.put(LLMConfig.ModelType.MINIMAX, "https://api.minimax.chat/v1");
        defaults.put(LLMConfig.ModelType.MISTRAL, "https://api.mistral.ai/v1");
        defaults.put(LLMConfig.ModelType.GROQ, "https://api.groq.com/openai/v1");
        defaults.put(LLMConfig.ModelType.COHERE, "https://api.cohere.ai/compatibility/v1");
        defaults.put(LLMConfig.ModelType.OPENROUTER, "https://openrouter.ai/api/v1");
        defaults.put(LLMConfig.ModelType.AZURE_OPENAI, "");
        defaults.put(LLMConfig.ModelType.BAICHUAN, "https://api.baichuan-ai.com/v1");
        defaults.put(LLMConfig.ModelType.YI, "https://api.lingyiwanwu.com/v1");
        defaults.put(LLMConfig.ModelType.STEPFUN, "https://api.stepfun.com/v1");
        defaults.put(LLMConfig.ModelType.XAI, "https://api.x.ai/v1");
        defaults.put(LLMConfig.ModelType.FIREWORKS, "https://api.fireworks.ai/inference/v1");
        defaults.put(LLMConfig.ModelType.TOGETHER_AI, "https://api.together.xyz/v1");
        defaults.put(LLMConfig.ModelType.PERPLEXITY, "https://api.perplexity.ai");
        defaults.put(LLMConfig.ModelType.NOVITA, "https://api.novita.ai/v3/openai");
        defaults.put(LLMConfig.ModelType.REPLICATE, "https://api.replicate.com/v1");
        defaults.put(LLMConfig.ModelType.CEREBRAS, "https://api.cerebras.ai/v1");
        defaults.put(LLMConfig.ModelType.SAMBANOVA, "https://api.sambanova.ai/v1");
        defaults.put(LLMConfig.ModelType.OLLAMA, "http://127.0.0.1:11434/v1");
        defaults.put(LLMConfig.ModelType.LMSTUDIO, "http://127.0.0.1:1234/v1");
        return defaults;
    }

    /**
     * 重试间隔休眠
     */
    private void sleepRetry() {
        try {
            Thread.sleep(5_000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 执行真实的异步流式生成
     */
    private void streamAnswerAsync(String prompt, AnswerListener listener) {
        executor.submit(() -> {
            RuntimeException lastError = null;
            for (int attempt = 0; attempt <= maxRetryCount; attempt++) {
                CountDownLatch done = new CountDownLatch(1);
                AtomicBoolean hasToken = new AtomicBoolean(false);
                AtomicReference<Throwable> error = new AtomicReference<>();
                StringBuilder buffer = new StringBuilder();
                streamingModel.generate(prompt, new StreamingResponseHandler<AiMessage>() {
                    @Override
                    public void onNext(String token) {
                        if (token == null) {
                            return;
                        }
                        hasToken.set(true);
                        buffer.append(token);
                        listener.onToken(token);
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        String answer = resolveAnswer(response, buffer);
                        listener.onComplete(answer);
                        done.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        error.set(throwable);
                        done.countDown();
                    }
                });

                boolean finished = awaitCompletion(done);
                if (!finished) {
                    error.compareAndSet(null, new IllegalStateException("模型流式响应超时"));
                }

                if (error.get() == null) {
                    circuitBreaker.recordSuccess();
                    return;
                }

                circuitBreaker.recordFailure();
                lastError = new RuntimeException(error.get());
                logModelError("流式调用", attempt + 1, error.get());
                if (hasToken.get() || attempt >= maxRetryCount) {
                    logger.error("[AI_SERVICE_FAILED] 模型流式调用失败，模型类型={}, 模型名称={}, 已收到Token={}, 错误={}",
                        config != null ? config.getModelType() : "null",
                        config != null ? config.getModelName() : "null",
                        hasToken.get(),
                        error.get().getMessage());
                    listener.onError("模型调用失败: " + error.get().getMessage());
                    return;
                }
                sleepRetry();
            }
            logger.error("[AI_SERVICE_FAILED] 模型流式调用耗尽重试次数，模型类型={}, 模型名称={}",
                config != null ? config.getModelType() : "null",
                config != null ? config.getModelName() : "null");
            listener.onError(lastError == null ? "模型调用失败" : "模型调用失败: " + lastError.getMessage());
        });
    }

    /**
     * 记录模型调用错误日志
     */
    private void logModelError(String callType, int attempt, Throwable e) {
        logger.warn("[AI_RETRY] {} 失败，第 {} 次重试，模型类型={}, 模型名称={}, 错误类型={}, 错误信息={}",
            callType,
            attempt,
            config != null ? config.getModelType() : "null",
            config != null ? config.getModelName() : "null",
            e.getClass().getSimpleName(),
            e.getMessage());
    }

    /**
     * 等待异步任务完成
     */
    private boolean awaitCompletion(CountDownLatch done) {
        long timeoutMillis = config == null || config.getTimeout() == null
            ? TimeUnit.SECONDS.toMillis(30)
            : Math.max(1L, config.getTimeout().toMillis());
        try {
            return done.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 解析最终答案。优先从 Response 中提取，如果没有则使用手动拼接的 buffer。
     */
    private String resolveAnswer(Response<AiMessage> response, StringBuilder fallback) {
        if (response != null && response.content() != null) {
            String text = response.content().text();
            if (text != null) {
                return text;
            }
        }
        return fallback == null ? "" : fallback.toString();
    }

    /**
     * 关闭客户端，释放线程池资源
     * <p>调用此方法后，不应再使用此客户端实例进行任何操作。
     */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("LangChain4jClient 已关闭");
    }
}
