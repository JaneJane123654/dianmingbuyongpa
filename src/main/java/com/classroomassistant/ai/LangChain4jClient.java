package com.classroomassistant.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LangChain4j 客户端实现
 *
 * <p>基于 LangChain4j 统一封装模型调用，异步模式使用后台线程模拟流式回调。
 */
public class LangChain4jClient implements LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jClient.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "langchain4j-client");
        thread.setDaemon(true);
        return thread;
    });

    private ChatLanguageModel chatModel;
    private LLMConfig config;
    private CircuitBreaker circuitBreaker;
    private int maxRetryCount;

    @Override
    public void configure(LLMConfig config) {
        this.config = Objects.requireNonNull(config, "模型配置不能为空");
        this.circuitBreaker = new CircuitBreaker(Math.max(1, config.getMaxRetryCount()), java.time.Duration.ofSeconds(10));
        this.maxRetryCount = Math.max(0, config.getMaxRetryCount());
        this.chatModel = createModel(config);
    }

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
                logger.warn("模型调用失败，第 {} 次重试: {}", attempt + 1, e.getMessage());
                if (attempt < maxRetryCount) {
                    sleepRetry();
                }
            }
        }
        throw lastError == null ? new IllegalStateException("模型调用失败") : lastError;
    }

    @Override
    public void generateAnswerAsync(String prompt, AnswerListener listener) {
        Objects.requireNonNull(listener, "监听器不能为空");
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

    private ChatLanguageModel createModel(LLMConfig config) {
        String modelName = resolveModelName(config);
        if (config.getModelType() == LLMConfig.ModelType.OPENAI) {
            return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(modelName)
                .build();
        }
        if (config.getModelType() == LLMConfig.ModelType.QIANFAN) {
            return QianfanChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(modelName)
                .build();
        }
        throw new IllegalStateException("当前模型类型暂不支持: " + config.getModelType());
    }

    private String resolveModelName(LLMConfig config) {
        if (config.getModelName() != null && !config.getModelName().isBlank()) {
            return config.getModelName();
        }
        if (config.getModelType() == LLMConfig.ModelType.OPENAI) {
            return "gpt-3.5-turbo";
        }
        if (config.getModelType() == LLMConfig.ModelType.QIANFAN) {
            return "ERNIE-Speed";
        }
        return "";
    }

    private void sleepRetry() {
        try {
            Thread.sleep(5_000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
