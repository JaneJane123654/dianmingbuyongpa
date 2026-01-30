package com.classroomassistant.ai;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 LLM 客户端（占位实现）
 *
 * <p>在未接入真实模型时，提供可运行的基础实现。
 */
public class DefaultLLMClient implements LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLLMClient.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "llm-client");
        thread.setDaemon(true);
        return thread;
    });

    private LLMConfig config;

    @Override
    public void configure(LLMConfig config) {
        this.config = Objects.requireNonNull(config, "模型配置不能为空");
    }

    @Override
    public String generateAnswer(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        return "（模拟回答）" + prompt;
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
                logger.warn("生成回答失败: {}", e.getMessage());
                listener.onError("生成回答失败: " + e.getMessage());
            }
        });
    }

    public LLMConfig getConfig() {
        return config;
    }
}
