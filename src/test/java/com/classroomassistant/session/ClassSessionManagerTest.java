package com.classroomassistant.session;

import static org.junit.jupiter.api.Assertions.*;

import com.classroomassistant.ai.AnswerListener;
import com.classroomassistant.ai.LLMClient;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.audio.AudioListener;
import com.classroomassistant.audio.AudioRecorder;
import com.classroomassistant.runtime.HealthMonitor;
import com.classroomassistant.runtime.TaskScheduler;
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
import org.junit.jupiter.api.Test;

/**
 * ClassSessionManager 集成测试
 *
 * <p>使用模拟组件验证会话编排核心业务逻辑。
 */
class ClassSessionManagerTest {

    private ClassSessionManager manager;
    private StubAudioRecorder audioRecorder;
    private StubWakeWordDetector wakeWordDetector;
    private StubSilenceDetector silenceDetector;
    private StubSpeechRecognizer speechRecognizer;
    private StubLLMClient llmClient;
    private StubNotificationService notificationService;

    @BeforeEach
    void setUp() {
        audioRecorder = new StubAudioRecorder();
        wakeWordDetector = new StubWakeWordDetector();
        silenceDetector = new StubSilenceDetector();
        speechRecognizer = new StubSpeechRecognizer();
        llmClient = new StubLLMClient();
        notificationService = new StubNotificationService();

        SpeechServices speechServices = new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer);

        manager = new ClassSessionManager(
            new StubConfigManager(),
            new StubPreferencesManager(),
            new TaskScheduler(),
            new HealthMonitor(new TaskScheduler()),
            notificationService,
            new StubModelRepository(),
            new StubRecordingRepository(),
            audioRecorder,
            speechServices,
            llmClient
        );

        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .vadEnabled(true)
            .vadQuietThresholdSeconds(3)
            .audioLookbackSeconds(60)
            .recordingSaveEnabled(false)
            .aiTokenPlainText("test-token")
            .build();
        manager.initialize(prefs);
    }

    @Test
    void startClass_setsRecordingTrue() {
        manager.startClass();
        assertTrue(manager.recordingProperty().get(), "开始上课后 recording 应为 true");
        assertTrue(audioRecorder.recording, "录音器应处于录音状态");
    }

    @Test
    void stopClass_setsRecordingFalse() {
        manager.startClass();
        manager.stopClass();
        assertFalse(manager.recordingProperty().get(), "停止上课后 recording 应为 false");
        assertFalse(audioRecorder.recording, "录音器应停止录音");
    }

    @Test
    void wakeWordDetected_triggersRecognitionAndAiAnswer() throws Exception {
        manager.startClass();

        // 模拟唤醒词检测回调
        wakeWordDetector.triggerWakeWord("张三");

        // 等待异步流程完成
        Thread.sleep(200);

        // 验证识别与 AI 调用
        assertTrue(speechRecognizer.recognizeCalled, "应触发语音识别");
        assertTrue(llmClient.generateCalled, "应触发 AI 回答");
    }

    @Test
    void aiTokenMissing_showsWarningNotification() {
        // 使用无 Token 的偏好重新初始化
        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .vadEnabled(true)
            .aiTokenPlainText("")
            .build();
        manager.applySettings(prefs);

        assertTrue(notificationService.warningShown, "缺少 Token 时应显示警告");
        assertTrue(notificationService.lastWarningTitle.contains("API Key"), "警告标题应包含 API Key");
    }

    // ========== 桩/模拟实现 ==========

    static class StubAudioRecorder implements AudioRecorder {
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

    static class StubWakeWordDetector implements WakeWordDetector {
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

    static class StubSilenceDetector implements SilenceDetector {
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
    }

    static class StubSpeechRecognizer implements SpeechRecognizer {
        boolean recognizeCalled = false;

        @Override
        public void initialize(Path modelDir) {
        }

        @Override
        public String recognize(byte[] pcm) {
            recognizeCalled = true;
            return "测试识别结果";
        }

        @Override
        public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
            recognizeCalled = true;
            listener.onResult("测试识别结果");
        }
    }

    static class StubLLMClient implements LLMClient {
        boolean generateCalled = false;

        @Override
        public void configure(LLMConfig config) {
        }

        @Override
        public String generateAnswer(String prompt) {
            generateCalled = true;
            return "AI 回答";
        }

        @Override
        public void generateAnswerAsync(String prompt, AnswerListener listener) {
            generateCalled = true;
            listener.onToken("AI ");
            listener.onToken("回答");
            listener.onComplete("AI 回答");
        }
    }

    static class StubNotificationService implements NotificationService {
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

    static class StubConfigManager extends ConfigManager {
        StubConfigManager() {
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

    static class StubPreferencesManager extends PreferencesManager {
        @Override
        public String loadAiTokenPlainText() {
            return "";
        }
    }

    static class StubModelRepository extends ModelRepository {
        StubModelRepository() {
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

    static class StubRecordingRepository extends RecordingRepository {
        StubRecordingRepository() {
            super(null, null);
        }

        @Override
        public void saveRecording(byte[] pcm, String prefix) {
        }

        @Override
        public void cleanupOldRecordings(int retentionDays) {
        }
    }
}
