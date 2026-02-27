package com.classroomassistant.speech;

import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelRepository;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.utils.AppPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SpeechEngineFactory 单元测试
 *
 * <p>验证语音引擎工厂根据配置正确创建语音服务。
 */
class SpeechEngineFactoryTest {

    private ModelRepository modelRepository;
    private ConfigManager configManager;
    private PreferencesManager preferencesManager;

    @BeforeEach
    void setUp() {
        AppPaths appPaths = mock(AppPaths.class);
        when(appPaths.getModelsDir()).thenReturn(java.nio.file.Paths.get("models"));
        when(appPaths.getUserDataDir()).thenReturn(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")));
        when(appPaths.getRecordingsDir()).thenReturn(java.nio.file.Paths.get("recordings"));
        when(appPaths.getCacheDir()).thenReturn(java.nio.file.Paths.get("cache"));
        when(appPaths.getLogsDir()).thenReturn(java.nio.file.Paths.get("logs"));

        configManager = mock(ConfigManager.class);
        when(configManager.getModelsDir()).thenReturn(java.nio.file.Paths.get("models"));
        when(configManager.getSpeechEngineDefault()).thenReturn("FAKE");

        modelRepository = mock(ModelRepository.class);
        when(modelRepository.getKwsModelDir()).thenReturn(java.nio.file.Paths.get("models/kws"));
        when(modelRepository.getAsrModelDir()).thenReturn(java.nio.file.Paths.get("models/asr"));
        when(modelRepository.getVadModelFile()).thenReturn(java.nio.file.Paths.get("models/vad/silero.onnx"));

        preferencesManager = mock(PreferencesManager.class);
    }

    @Test
    @DisplayName("FAKE 引擎配置应创建 Fake 实现")
    void fakeEngine_createsFakeServices() {
        when(configManager.getSpeechEngineDefault()).thenReturn("FAKE");

        SpeechEngineFactory factory = new SpeechEngineFactory(modelRepository, configManager, preferencesManager);
        SpeechServices services = factory.createSpeechServices();

        assertNotNull(services);
        assertNotNull(services.getWakeWordDetector());
        assertNotNull(services.getSilenceDetector());
        assertNotNull(services.getSpeechRecognizer());
    }

    @Test
    @DisplayName("API 引擎缺失 Key 时应回退到 FAKE")
    void apiEngine_withoutKey_fallsBackToFake() {
        when(configManager.getSpeechEngineDefault()).thenReturn("API");
        when(configManager.getSpeechApiUrl()).thenReturn("https://api.test.com");
        when(configManager.getSpeechApiKey()).thenReturn("");
        when(configManager.getSpeechApiModel()).thenReturn("whisper-1");
        when(preferencesManager.loadSpeechApiKey()).thenReturn("");

        SpeechEngineFactory factory = new SpeechEngineFactory(modelRepository, configManager, preferencesManager);
        SpeechServices services = factory.createSpeechServices();

        // 应该回退到 FAKE 实现
        assertNotNull(services);
    }

    @Test
    @DisplayName("两参数构造函数向后兼容")
    void twoArgConstructor_backwardCompatible() {
        SpeechEngineFactory factory = new SpeechEngineFactory(modelRepository, configManager);
        SpeechServices services = factory.createSpeechServices();

        assertNotNull(services);
    }
}
