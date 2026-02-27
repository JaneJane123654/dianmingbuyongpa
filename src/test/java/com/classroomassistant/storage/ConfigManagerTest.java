package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.utils.AppPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigManager 单元测试
 *
 * <p>验证 Properties 配置文件的正确读取与默认值处理。
 * 
 * @author Code Assistant
 * @date 2026-02-01
 */
class ConfigManagerTest {

    private static final String APP_NAME = "ClassroomAssistant";
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        AppPaths appPaths = new AppPaths(APP_NAME);
        configManager = new ConfigManager(appPaths);
    }

    @Test
    @DisplayName("获取音频配置时应返回正确的默认值")
    void getAudioConfig_shouldReturnDefaultValues() {
        AudioConfig config = configManager.getAudioConfig();

        assertNotNull(config);
        assertEquals(16000, config.sampleRate());
        assertEquals(1, config.channels());
        assertEquals(16, config.bitsPerSample());
        assertEquals(20, config.frameMillis());
        assertEquals(240, config.defaultLookbackSeconds());
    }

    @Test
    @DisplayName("获取 VAD 默认配置时应返回正确的默认值")
    void getVadDefaults_shouldReturnDefaultValues() {
        VadDefaults defaults = configManager.getVadDefaults();

        assertNotNull(defaults);
        assertTrue(defaults.enabledDefault());
        assertEquals(5, defaults.quietThresholdSecondsDefault());
    }

    @Test
    @DisplayName("获取 AI 默认配置时应返回正确的默认值")
    void getAiDefaults_shouldReturnDefaultValues() {
        AiDefaults defaults = configManager.getAiDefaults();

        assertNotNull(defaults);
        assertEquals(30, defaults.timeoutSeconds());
        assertEquals(3, defaults.maxRetryCount());
        assertTrue(defaults.streaming());
        assertEquals(LLMConfig.ModelType.QIANFAN, defaults.providerDefault());
    }

    @Test
    @DisplayName("获取触发冷却时间应返回正确的值")
    void getTriggerCooldownSeconds_shouldReturnConfiguredValue() {
        int cooldown = configManager.getTriggerCooldownSeconds();

        assertEquals(10, cooldown);
    }

    @Test
    @DisplayName("获取语音引擎默认类型应返回正确的值")
    void getSpeechEngineDefault_shouldReturnConfiguredValue() {
        String engine = configManager.getSpeechEngineDefault();

        assertNotNull(engine);
        assertEquals("API", engine);
    }

    @Test
    @DisplayName("getString 对于不存在的键应返回默认值")
    void getString_forMissingKey_shouldReturnDefault() {
        String value = configManager.getString("non.existent.key", "defaultValue");

        assertEquals("defaultValue", value);
    }

    @Test
    @DisplayName("getInt 对于不存在的键应返回默认值")
    void getInt_forMissingKey_shouldReturnDefault() {
        int value = configManager.getInt("non.existent.int.key", 42);

        assertEquals(42, value);
    }

    @Test
    @DisplayName("getBoolean 对于不存在的键应返回默认值")
    void getBoolean_forMissingKey_shouldReturnDefault() {
        boolean value = configManager.getBoolean("non.existent.bool.key", true);

        assertTrue(value);
    }

    @Test
    @DisplayName("reload 后应正确重新加载配置")
    void reload_shouldReloadProperties() {
        AudioConfig beforeReload = configManager.getAudioConfig();
        
        configManager.reload();
        
        AudioConfig afterReload = configManager.getAudioConfig();
        assertEquals(beforeReload.sampleRate(), afterReload.sampleRate());
        assertEquals(beforeReload.channels(), afterReload.channels());
    }

    @Test
    @DisplayName("获取模型下载 URL 应返回配置值")
    void getModelDownloadBaseUrl_shouldReturnConfiguredValue() {
        String url = configManager.getModelDownloadBaseUrl();

        assertNotNull(url);
        assertTrue(url.startsWith("https://"));
    }

    @Test
    @DisplayName("获取 KWS 模型名称应返回非空值")
    void getKwsModelName_shouldReturnNonEmptyValue() {
        String modelName = configManager.getKwsModelName();

        assertNotNull(modelName);
        assertFalse(modelName.isEmpty());
    }

    @Test
    @DisplayName("获取 ASR 模型名称应返回非空值")
    void getAsrModelName_shouldReturnNonEmptyValue() {
        String modelName = configManager.getAsrModelName();

        assertNotNull(modelName);
        assertFalse(modelName.isEmpty());
    }

    @Test
    @DisplayName("获取 VAD 模型名称应返回非空值")
    void getVadModelName_shouldReturnNonEmptyValue() {
        String modelName = configManager.getVadModelName();

        assertNotNull(modelName);
        assertFalse(modelName.isEmpty());
    }
}
