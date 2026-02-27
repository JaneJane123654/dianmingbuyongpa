package com.classroomassistant.ai;

import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.PreferencesManager;
import java.time.Duration;
import java.util.Objects;

/**
 * LLM 客户端工厂 (LLM Client Factory)
 *
 * <p>负责根据当前系统配置和用户偏好创建并初始化 {@link LLMClient} 实例。
 * 它会自动识别是使用真实的 AI 客户端（如 LangChain4j 实现）还是默认的模拟客户端。
 *
 * <p>使用示例：
 * <pre>
 * LLMClientFactory factory = new LLMClientFactory(configManager, preferencesManager);
 * LLMClient client = factory.create();
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class LLMClientFactory {

    private final ConfigManager configManager;
    private final PreferencesManager preferencesManager;

    /**
     * 构造函数
     *
     * @param configManager      配置管理器，用于获取默认 AI 设置
     * @param preferencesManager 偏好管理器，用于获取用户设置的 API Key 等信息
     * @throws NullPointerException 如果任一参数为 null
     */
    public LLMClientFactory(ConfigManager configManager, PreferencesManager preferencesManager) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
        this.preferencesManager = Objects.requireNonNull(preferencesManager, "偏好管理器不能为空");
    }

    /**
     * 创建并配置 LLM 客户端实例
     *
     * @return 配置好的 {@link LLMClient} 实例。如果提供了 API Key 则返回真实客户端，否则返回默认实现。
     */
    public LLMClient create() {
        LLMConfig config = resolveConfig();
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            LangChain4jClient client = new LangChain4jClient();
            client.configure(config);
            return client;
        }
        DefaultLLMClient client = new DefaultLLMClient();
        client.configure(config);
        return client;
    }

    /**
     * 解析并生成当前的 AI 配置
     *
     * @return 组装好的 {@link LLMConfig} 对象
     */
    private LLMConfig resolveConfig() {
        var defaults = configManager.getAiDefaults();
        String token = preferencesManager.loadAiTokenPlainText();
        return LLMConfig.builder()
            .modelType(defaults.providerDefault())
            .modelName(defaults.modelNameDefault())
            .apiKey(token)
            .timeout(Duration.ofSeconds(defaults.timeoutSeconds()))
            .maxRetryCount(defaults.maxRetryCount())
            .streaming(defaults.streamingDefault())
            .build();
    }
}
