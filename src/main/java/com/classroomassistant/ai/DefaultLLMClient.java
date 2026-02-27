package com.classroomassistant.ai;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 LLM 客户端 (Default/Mock LLM Client)
 *
 * <p>提供一个占位用的 Mock 实现，主要用于：
 * <ul>
 *   <li>系统开发初期，在尚未接入真实大模型 API 时保证应用逻辑链路可通。</li>
 *   <li>作为 fallback 机制，当真实客户端不可用或未配置 API Key 时使用。</li>
 * </ul>
 *
 * <p>它不产生真实的 AI 推理，仅在异步模式下模拟 Token 输出效果。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class DefaultLLMClient implements LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLLMClient.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "llm-client");
        thread.setDaemon(true);
        return thread;
    });

    private LLMConfig config;

    /**
     * 配置客户端
     *
     * @param config 配置对象
     * @throws NullPointerException 如果 config 为 null
     */
    @Override
    public void configure(LLMConfig config) {
        this.config = Objects.requireNonNull(config, "模型配置不能为空");
    }

    /**
     * 同步生成回答（Mock）
     *
     * @param prompt 提示词
     * @return 带有模拟前缀的提示词文本
     */
    @Override
    public String generateAnswer(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        return "（模拟回答）" + prompt;
    }

    /**
     * 异步生成回答（Mock 流式效果）
     *
     * @param prompt   提示词
     * @param listener 监听器
     */
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
                    if (Thread.currentThread().isInterrupted()) {
                        listener.onError("任务已取消");
                        return;
                    }
                    listener.onToken(token);
                    // 模拟网络延迟感
                    Thread.sleep(20);
                }
                listener.onComplete(answer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                listener.onError("任务已取消");
            } catch (Exception e) {
                logger.warn("生成回答失败: {}", e.getMessage());
                listener.onError("生成回答失败: " + e.getMessage());
            }
        });
    }

    /**
     * 获取当前配置
     *
     * @return {@link LLMConfig}
     */
    public LLMConfig getConfig() {
        return config;
    }
}
