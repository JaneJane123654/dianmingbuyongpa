package com.classroomassistant.e2e;

import static org.junit.jupiter.api.Assertions.*;

import com.classroomassistant.ai.AnswerListener;
import com.classroomassistant.ai.LLMClient;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.audio.AudioListener;
import com.classroomassistant.audio.AudioRecorder;
import com.classroomassistant.runtime.HealthMonitor;
import com.classroomassistant.runtime.TaskScheduler;
import com.classroomassistant.session.ClassSessionManager;
import com.classroomassistant.speech.RecognitionListener;
import com.classroomassistant.speech.SilenceDetector;
import com.classroomassistant.speech.SilenceListener;
import com.classroomassistant.speech.SpeechRecognizer;
import com.classroomassistant.speech.SpeechServices;
import com.classroomassistant.speech.WakeWordDetector;
import com.classroomassistant.speech.WakeWordListener;
import com.classroomassistant.storage.AiDefaults;
import com.classroomassistant.storage.AudioConfig;
import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelCheckResult;
import com.classroomassistant.storage.ModelRepository;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.RecordingRepository;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.storage.VadDefaults;
import com.classroomassistant.utils.NotificationService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 端到端集成测试
 *
 * <p>使用模拟组件验证完整业务流程：音频采集 → 唤醒检测 → 语音识别 → AI 回答 → UI 更新。
 */
class EndToEndFlowTest {

    private ClassSessionManager manager;
    private MockAudioRecorder audioRecorder;
    private MockWakeWordDetector wakeWordDetector;
    private MockSilenceDetector silenceDetector;
    private MockSpeechRecognizer speechRecognizer;
    private MockLLMClient llmClient;
    private MockNotificationService notificationService;

    @BeforeEach
    void setUp() {
        audioRecorder = new MockAudioRecorder();
        wakeWordDetector = new MockWakeWordDetector();
        silenceDetector = new MockSilenceDetector();
        speechRecognizer = new MockSpeechRecognizer();
        llmClient = new MockLLMClient();
        notificationService = new MockNotificationService();

        SpeechServices speechServices = new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer);

        manager = new ClassSessionManager(
            new MockConfigManager(),
            new MockPreferencesManager("test-token"),
            new TaskScheduler(),
            new HealthMonitor(new TaskScheduler()),
            notificationService,
            new MockModelRepository(),
            new MockRecordingRepository(),
            audioRecorder,
            speechServices,
            llmClient
        );

        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .vadEnabled(true)
            .vadQuietThresholdSeconds(3)
            .audioLookbackSeconds(60)
            .recordingSaveEnabled(true)
            .recordingRetentionDays(7)
            .aiTokenPlainText("test-token")
            .build();
        manager.initialize(prefs);
    }

    @Test
    @DisplayName("完整业务流程：启动 → 唤醒 → 识别 → AI回答 → 停止")
    void fullBusinessFlow_wakeWordToAiAnswer() throws Exception {
        // 1. 启动上课
        manager.startClass();
        assertTrue(manager.recordingProperty().get(), "启动后应处于录音状态");
        assertTrue(audioRecorder.isRecording(), "录音器应已启动");

        // 2. 模拟唤醒词触发
        wakeWordDetector.triggerWakeWord("张三");

        // 3. 等待异步流程完成
        Thread.sleep(300);

        // 4. 验证识别被调用
        assertTrue(speechRecognizer.recognizeCalled, "应触发语音识别");
        assertEquals("这是一段测试音频内容", speechRecognizer.lastRecognizedText);

        // 5. 验证 AI 调用
        assertTrue(llmClient.generateCalled, "应触发 AI 回答生成");
        assertFalse(llmClient.receivedTokens.isEmpty(), "应收到流式 Token");

        // 6. 验证 UI 状态更新
        assertFalse(manager.lectureTextProperty().get().isBlank(), "课堂内容应已更新");
        assertFalse(manager.answerTextProperty().get().isBlank(), "AI 回答应已更新");

        // 7. 停止上课
        manager.stopClass();
        assertFalse(manager.recordingProperty().get(), "停止后应不再录音");
        assertFalse(audioRecorder.isRecording(), "录音器应已停止");
    }

    @Test
    @DisplayName("静音超时触发流程")
    void silenceTimeoutTriggersRecognition() throws Exception {
        manager.startClass();

        // 模拟静音超时
        silenceDetector.triggerSilenceTimeout();

        Thread.sleep(300);

        assertTrue(speechRecognizer.recognizeCalled, "静音超时应触发语音识别");
        assertTrue(llmClient.generateCalled, "静音超时应触发 AI 回答");

        manager.stopClass();
    }

    @Test
    @DisplayName("录音保存流程验证")
    void recordingSaveFlow() throws Exception {
        MockRecordingRepository recordingRepo = new MockRecordingRepository();

        SpeechServices speechServices = new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer);
        ClassSessionManager managerWithRecording = new ClassSessionManager(
            new MockConfigManager(),
            new MockPreferencesManager("test-token"),
            new TaskScheduler(),
            new HealthMonitor(new TaskScheduler()),
            notificationService,
            new MockModelRepository(),
            recordingRepo,
            audioRecorder,
            speechServices,
            llmClient
        );

        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .vadEnabled(true)
            .recordingSaveEnabled(true)
            .recordingRetentionDays(7)
            .aiTokenPlainText("test-token")
            .build();
        managerWithRecording.initialize(prefs);
        managerWithRecording.startClass();

        wakeWordDetector.triggerWakeWord("张三");
        Thread.sleep(300);

        assertTrue(recordingRepo.saveCalled, "应调用录音保存");
        assertNotNull(recordingRepo.lastSavedPrefix, "保存的文件名前缀不应为空");

        managerWithRecording.stopClass();
    }

    @Test
    @DisplayName("Token 缺失时的警告提示")
    void tokenMissing_showsWarning() {
        // 使用无 Token 的偏好
        ClassSessionManager managerNoToken = new ClassSessionManager(
            new MockConfigManager(),
            new MockPreferencesManager(""),
            new TaskScheduler(),
            new HealthMonitor(new TaskScheduler()),
            notificationService,
            new MockModelRepository(),
            new MockRecordingRepository(),
            audioRecorder,
            new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer),
            llmClient
        );

        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .aiTokenPlainText("")
            .build();
        managerNoToken.initialize(prefs);

        assertTrue(notificationService.warningShown, "缺少 Token 时应显示警告");
        assertTrue(notificationService.lastWarningTitle.contains("API Key"), "警告标题应包含 API Key");
    }

    @Test
    @DisplayName("多模型类型配置验证")
    void multiModelTypeSupport() {
        // 验证所有模型类型都能正确解析
        for (LLMConfig.ModelType type : LLMConfig.ModelType.values()) {
            LLMConfig config = LLMConfig.builder()
                .modelType(type)
                .apiKey("test-key")
                .build();
            assertNotNull(config.getModelType());
            assertEquals(type, config.getModelType());
        }
    }

    // ========== Mock 实现 ==========

    static class MockAudioRecorder implements AudioRecorder {
        boolean recording = false;
        AudioListener listener;

        @Override
        public void startRecording() {
            recording = true;
        }

        @Override
        public void stopRecording() {
            recording = false;
        }

        @Override
        public boolean isRecording() {
            return recording;
        }

        @Override
        public byte[] getAudioBefore(int seconds) {
            return new byte[16000 * 2 * seconds];
        }

        @Override
        public void addListener(AudioListener listener) {
            this.listener = listener;
        }

        @Override
        public void close() {
            recording = false;
        }
    }

    static class MockWakeWordDetector implements WakeWordDetector {
        WakeWordListener listener;

        @Override
        public void initialize(Path modelDir, String keywords) {
        }

        @Override
        public boolean detect(float[] frame) {
            return false;
        }

        @Override
        public void addListener(WakeWordListener listener) {
            this.listener = listener;
        }

        void triggerWakeWord(String keyword) {
            if (listener != null) {
                listener.onWakeWordDetected(keyword);
            }
        }
    }

    static class MockSilenceDetector implements SilenceDetector {
        SilenceListener listener;

        @Override
        public void initialize(Path modelFile) {
        }

        @Override
        public boolean detect(float[] frame, int durationMillis) {
            return false;
        }

        @Override
        public void setQuietThresholdSeconds(int seconds) {
        }

        @Override
        public void addListener(SilenceListener listener) {
            this.listener = listener;
        }

        void triggerSilenceTimeout() {
            if (listener != null) {
                listener.onSilenceTimeout();
            }
        }
    }

    static class MockSpeechRecognizer implements SpeechRecognizer {
        boolean recognizeCalled = false;
        String lastRecognizedText = "";

        @Override
        public void initialize(Path modelDir) {
        }

        @Override
        public String recognize(byte[] pcm) {
            recognizeCalled = true;
            lastRecognizedText = "这是一段测试音频内容";
            return lastRecognizedText;
        }

        @Override
        public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
            recognizeCalled = true;
            lastRecognizedText = "这是一段测试音频内容";
            listener.onResult(lastRecognizedText);
        }
    }

    static class MockLLMClient implements LLMClient {
        boolean generateCalled = false;
        List<String> receivedTokens = new ArrayList<>();

        @Override
        public void configure(LLMConfig config) {
        }

        @Override
        public String generateAnswer(String prompt) {
            generateCalled = true;
            return "这是 AI 生成的回答";
        }

        @Override
        public void generateAnswerAsync(String prompt, AnswerListener listener) {
            generateCalled = true;
            receivedTokens.clear();
            String[] tokens = {"这是", " AI ", "生成的", "回答"};
            for (String token : tokens) {
                receivedTokens.add(token);
                listener.onToken(token);
            }
            listener.onComplete("这是 AI 生成的回答");
        }
    }

    static class MockNotificationService implements NotificationService {
        boolean warningShown = false;
        String lastWarningTitle = "";

        @Override
        public void showInfo(String title, String message) {
        }

        @Override
        public void showWarning(String title, String message) {
            warningShown = true;
            lastWarningTitle = title;
        }

        @Override
        public void showError(String title, String message) {
        }
    }

    static class MockConfigManager extends ConfigManager {
        MockConfigManager() {
            super(null);
        }

        @Override
        public AudioConfig getAudioConfig() {
            return new AudioConfig(16000, 1, 16, 20, 60);
        }

        @Override
        public VadDefaults getVadDefaults() {
            return new VadDefaults(true, 3);
        }

        @Override
        public AiDefaults getAiDefaults() {
            return new AiDefaults(30, 3, true, LLMConfig.ModelType.OPENAI, "gpt-3.5-turbo");
        }

        @Override
        public int getTriggerCooldownSeconds() {
            return 0;
        }

        @Override
        public String getSpeechEngineDefault() {
            return "FAKE";
        }
    }

    static class MockPreferencesManager extends PreferencesManager {
        private final String token;

        MockPreferencesManager(String token) {
            this.token = token;
        }

        @Override
        public String loadAiTokenPlainText() {
            return token;
        }
    }

    static class MockModelRepository extends ModelRepository {
        MockModelRepository() {
            super(null, null);
        }

        @Override
        public Path getKwsModelDir() {
            return Path.of(".");
        }

        @Override
        public Path getVadModelFile() {
            return Path.of(".");
        }

        @Override
        public Path getAsrModelDir() {
            return Path.of(".");
        }

        @Override
        public ModelCheckResult checkRequiredModels(boolean requireSherpa) {
            return new ModelCheckResult(true, Collections.emptyList());
        }
    }

    static class MockRecordingRepository extends RecordingRepository {
        boolean saveCalled = false;
        String lastSavedPrefix = null;

        MockRecordingRepository() {
            super(null, null);
        }

        @Override
        public Path saveRecording(byte[] pcm, String prefix) {
            saveCalled = true;
            lastSavedPrefix = prefix;
            return Path.of("mock_recording.wav");
        }

        @Override
        public void cleanupOldRecordings(int retentionDays) {
        }
    }
}
