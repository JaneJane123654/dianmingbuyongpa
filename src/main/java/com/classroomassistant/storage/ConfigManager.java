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
 * 应用配置管理器 (Application Configuration Manager)
 *
 * <p>负责加载和管理应用的全局配置项（基于 Properties 文件）。
 * 它首先从 classpath 加载默认配置 `application.properties`，
 * 然后会尝试从用户数据目录加载覆盖配置（如果存在），实现灵活的配置管理。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private final AppPaths appPaths;
    private final Properties properties = new Properties();

    /**
     * 构造配置管理器并执行首次加载
     *
     * @param appPaths 应用路径工具类，用于定位配置文件位置
     * @throws NullPointerException 如果 appPaths 为 null
     */
    public ConfigManager(AppPaths appPaths) {
        this.appPaths = Objects.requireNonNull(appPaths);
        load();
    }

    /**
     * 重新加载所有配置项
     * <p>清除当前已加载的属性并重新从文件读取。
     */
    public void reload() {
        properties.clear();
        load();
    }

    /**
     * 获取音频相关配置
     *
     * @return 包含采样率、声道数等信息的 {@link AudioConfig} 对象
     */
    public AudioConfig getAudioConfig() {
        int sampleRate = getInt("audio.sampleRate", 16000);
        int channels = getInt("audio.channels", 1);
        int bitsPerSample = getInt("audio.bitsPerSample", 16);
        int frameMillis = getInt("audio.frameMillis", 20);
        int defaultLookbackSeconds = getInt("audio.lookbackSeconds.default", 240);
        return new AudioConfig(sampleRate, channels, bitsPerSample, frameMillis, defaultLookbackSeconds);
    }

    /**
     * 获取静音检测 (VAD) 的默认配置
     *
     * @return {@link VadDefaults} 对象
     */
    public VadDefaults getVadDefaults() {
        boolean enabled = getBoolean("vad.enabled.default", true);
        int quietThresholdSecondsDefault = getInt("vad.quietThresholdSeconds.default", 5);
        return new VadDefaults(enabled, quietThresholdSecondsDefault);
    }

    /**
     * 获取 AI 服务的默认配置
     *
     * @return {@link AiDefaults} 对象
     */
    public AiDefaults getAiDefaults() {
        int timeoutSeconds = getInt("ai.timeoutSeconds.default", 30);
        int maxRetryCount = getInt("ai.maxRetryCount.default", 3);
        boolean streaming = getBoolean("ai.streaming.default", true);
        LLMConfig.ModelType providerDefault =
            LLMConfig.ModelType.valueOf(getString("ai.provider.default", "QIANFAN"));
        String modelNameDefault = getString("ai.modelName.default", "");
        return new AiDefaults(timeoutSeconds, maxRetryCount, streaming, providerDefault, modelNameDefault);
    }

    /**
     * 获取触发后的冷却时间
     *
     * @return 冷却秒数
     */
    public int getTriggerCooldownSeconds() {
        return getInt("app.triggerCooldownSeconds", 10);
    }

    /**
     * 获取默认的语音引擎类型
     *
     * @return 引擎标识字符串（如 "SHERPA", "API", "FAKE" 等）
     */
    public String getSpeechEngineDefault() {
        return getString("speech.engine.default", "API").trim();
    }

    /**
     * 获取语音 API 配置（云端识别）
     *
     * @return API 端点 URL
     */
    public String getSpeechApiUrl() {
        return getString("speech.api.url", "https://api.groq.com/openai/v1/audio/transcriptions").trim();
    }

    /**
     * 获取语音 API Key
     *
     * @return API Key（可能为空）
     */
    public String getSpeechApiKey() {
        return getString("speech.api.key", "").trim();
    }

    /**
     * 获取语音识别模型名称
     *
     * @return 模型名称
     */
    public String getSpeechApiModel() {
        return getString("speech.api.model", "whisper-large-v3").trim();
    }

    /**
     * 获取模型下载基础 URL
     *
     * @return 模型下载服务器的基础 URL
     */
    public String getModelDownloadBaseUrl() {
        return getString("model.download.baseUrl", "https://github.com/k2-fsa/sherpa-onnx/releases/download").trim();
    }

    /**
     * 获取 KWS 模型名称
     *
     * @return KWS 模型标识
     */
    public String getKwsModelName() {
        return getString("model.download.kwsModel", "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01").trim();
    }

    /**
     * 获取 ASR 模型名称
     *
     * @return ASR 模型标识
     */
    public String getAsrModelName() {
        return getString("model.download.asrModel", "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20").trim();
    }

    /**
     * 获取 VAD 模型文件名
     *
     * @return VAD 模型文件名
     */
    public String getVadModelName() {
        return getString("model.download.vadModel", "silero_vad.onnx").trim();
    }

    /**
     * 获取字符串类型的配置项
     *
     * @param key          配置键名
     * @param defaultValue 缺省值
     * @return 配置值或缺省值
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取整数类型的配置项
     *
     * @param key          配置键名
     * @param defaultValue 缺省值
     * @return 转换后的整数值。如果键不存在或格式错误，则返回缺省值。
     */
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

    /**
     * 获取布尔类型的配置项
     *
     * @param key          配置键名
     * @param defaultValue 缺省值
     * @return 转换后的布尔值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 获取模型存储根目录
     *
     * @return 目录 Path
     */
    public Path getModelsDir() {
        return appPaths.getModelsDir();
    }

    /**
     * 获取录音存储根目录
     *
     * @return 目录 Path
     */
    public Path getRecordingsDir() {
        return appPaths.getRecordingsDir();
    }

    /**
     * 获取缓存目录
     *
     * @return 目录 Path
     */
    public Path getCacheDir() {
        return appPaths.getCacheDir();
    }

    /**
     * 加载所有配置
     * <p>按照 默认配置 -> 覆盖配置 的顺序加载。
     */
    private void load() {
        ensureDirectories();
        loadFromClasspath("/config/application.properties");
        loadOverrides(appPaths.getUserDataDir().resolve("config").resolve("overrides.properties"));
    }

    /**
     * 确保应用所需的各个目录已创建
     */
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

    /**
     * 从 Classpath 加载默认配置文件
     *
     * @param resourcePath 资源路径
     */
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

    /**
     * 加载用户定义的覆盖配置
     *
     * @param overrideFile 覆盖配置文件路径
     */
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

