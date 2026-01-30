package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.utils.AppPaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用配置管理器（Properties）
 *
 * <p>负责加载 classpath 默认配置，并合并用户数据目录下的覆盖配置（如存在）。
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private final AppPaths appPaths;
    private final Properties properties = new Properties();

    public ConfigManager(AppPaths appPaths) {
        this.appPaths = Objects.requireNonNull(appPaths);
        load();
    }

    public void reload() {
        properties.clear();
        load();
    }

    public AudioConfig getAudioConfig() {
        int sampleRate = getInt("audio.sampleRate", 16000);
        int channels = getInt("audio.channels", 1);
        int bitsPerSample = getInt("audio.bitsPerSample", 16);
        int frameMillis = getInt("audio.frameMillis", 20);
        int defaultLookbackSeconds = getInt("audio.lookbackSeconds.default", 240);
        return new AudioConfig(sampleRate, channels, bitsPerSample, frameMillis, defaultLookbackSeconds);
    }

    public VadDefaults getVadDefaults() {
        boolean enabled = getBoolean("vad.enabled.default", true);
        int quietThresholdSecondsDefault = getInt("vad.quietThresholdSeconds.default", 5);
        return new VadDefaults(enabled, quietThresholdSecondsDefault);
    }

    public AiDefaults getAiDefaults() {
        int timeoutSeconds = getInt("ai.timeoutSeconds.default", 30);
        int maxRetryCount = getInt("ai.maxRetryCount.default", 3);
        boolean streaming = getBoolean("ai.streaming.default", true);
        LLMConfig.ModelType providerDefault =
            LLMConfig.ModelType.valueOf(getString("ai.provider.default", "QIANFAN"));
        String modelNameDefault = getString("ai.modelName.default", "");
        return new AiDefaults(timeoutSeconds, maxRetryCount, streaming, providerDefault, modelNameDefault);
    }

    public int getTriggerCooldownSeconds() {
        return getInt("app.triggerCooldownSeconds", 10);
    }

    public String getSpeechEngineDefault() {
        return getString("speech.engine.default", "FAKE").trim();
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public Path getModelsDir() {
        return appPaths.getModelsDir();
    }

    public Path getRecordingsDir() {
        return appPaths.getRecordingsDir();
    }

    public Path getCacheDir() {
        return appPaths.getCacheDir();
    }

    private void load() {
        ensureDirectories();
        loadFromClasspath("/config/application.properties");
        loadOverrides(appPaths.getUserDataDir().resolve("config").resolve("overrides.properties"));
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(appPaths.getUserDataDir());
            Files.createDirectories(appPaths.getUserDataDir().resolve("config"));
            Files.createDirectories(appPaths.getModelsDir());
            Files.createDirectories(appPaths.getRecordingsDir());
            Files.createDirectories(appPaths.getCacheDir());
            Files.createDirectories(appPaths.getLogsDir());
        } catch (IOException e) {
            logger.warn("创建应用目录失败: {}", e.getMessage(), e);
        }
    }

    private void loadFromClasspath(String resourcePath) {
        try (InputStream inputStream = ConfigManager.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.warn("未找到默认配置文件: {}", resourcePath);
                return;
            }
            properties.load(inputStream);
        } catch (IOException e) {
            logger.warn("加载默认配置失败: {}", e.getMessage(), e);
        }
    }

    private void loadOverrides(Path overrideFile) {
        if (!Files.exists(overrideFile)) {
            return;
        }
        Properties override = new Properties();
        try (InputStream inputStream = Files.newInputStream(overrideFile)) {
            override.load(inputStream);
            override.forEach((k, v) -> properties.put(k, v));
            logger.info("已加载覆盖配置: {}", overrideFile);
        } catch (IOException e) {
            logger.warn("加载覆盖配置失败: {}", e.getMessage(), e);
        }
    }
}

