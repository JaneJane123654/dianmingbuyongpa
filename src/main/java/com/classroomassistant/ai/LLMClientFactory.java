package com.classroomassistant.ai;

import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.PreferencesManager;
import java.time.Duration;
import java.util.Objects;

/**
 * LLM 客户端工厂
 */
public class LLMClientFactory {

    private final ConfigManager configManager;
    private final PreferencesManager preferencesManager;

    public LLMClientFactory(ConfigManager configManager, PreferencesManager preferencesManager) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
        this.preferencesManager = Objects.requireNonNull(preferencesManager, "偏好管理器不能为空");
    }

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
