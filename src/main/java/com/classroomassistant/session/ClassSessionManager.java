package com.classroomassistant.session;

import com.classroomassistant.ai.AnswerListener;
import com.classroomassistant.ai.LLMClient;
import com.classroomassistant.ai.PromptTemplate;
import com.classroomassistant.audio.AudioRecorder;
import com.classroomassistant.runtime.HealthMonitor;
import com.classroomassistant.runtime.TaskScheduler;
import com.classroomassistant.speech.RecognitionListener;
import com.classroomassistant.speech.SpeechServices;
import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelCheckResult;
import com.classroomassistant.storage.ModelRepository;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.RecordingRepository;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.utils.NotificationService;
import com.classroomassistant.utils.Validator;
import com.classroomassistant.utils.audio.AudioUtils;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 课堂会话编排器
 *
 * <p>负责串联录音、识别与 AI 处理的流程。
 */
public class ClassSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ClassSessionManager.class);

    private final ConfigManager configManager;
    private final PreferencesManager preferencesManager;
    private final TaskScheduler taskScheduler;
    private final HealthMonitor healthMonitor;
    private final NotificationService notificationService;
    private final ModelRepository modelRepository;
    private final RecordingRepository recordingRepository;
    private final AudioRecorder audioRecorder;
    private final SpeechServices speechServices;
    private final LLMClient llmClient;
    private final PromptTemplate promptTemplate = new PromptTemplate();

    private final BooleanProperty recordingProperty = new SimpleBooleanProperty(false);
    private final StringProperty recordingStatusTextProperty = new SimpleStringProperty("未录音");
    private final StringProperty detectionStatusTextProperty = new SimpleStringProperty("未启动");
    private final StringProperty quietSecondsTextProperty = new SimpleStringProperty("0s");
    private final StringProperty recordingSaveStatusTextProperty = new SimpleStringProperty("未启用");
    private final StringProperty lectureTextProperty = new SimpleStringProperty("");
    private final StringProperty answerTextProperty = new SimpleStringProperty("");
    private final StringProperty hintTextProperty = new SimpleStringProperty("");

    private final AtomicReference<SessionState> sessionState = new AtomicReference<>(SessionState.IDLE);
    private final AtomicBoolean listenersRegistered = new AtomicBoolean(false);
    private final AtomicBoolean aiTokenWarned = new AtomicBoolean(false);
    private UserPreferences currentPreferences;
    private long lastTriggerAtMillis;
    private int quietMillis;

    public ClassSessionManager(
        ConfigManager configManager,
        PreferencesManager preferencesManager,
        TaskScheduler taskScheduler,
        HealthMonitor healthMonitor,
        NotificationService notificationService,
        ModelRepository modelRepository,
        RecordingRepository recordingRepository,
        AudioRecorder audioRecorder,
        SpeechServices speechServices,
        LLMClient llmClient
    ) {
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
        this.preferencesManager = Objects.requireNonNull(preferencesManager, "偏好管理器不能为空");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "任务调度器不能为空");
        this.healthMonitor = Objects.requireNonNull(healthMonitor, "健康监控不能为空");
        this.notificationService = Objects.requireNonNull(notificationService, "通知服务不能为空");
        this.modelRepository = Objects.requireNonNull(modelRepository, "模型仓库不能为空");
        this.recordingRepository = Objects.requireNonNull(recordingRepository, "录音仓库不能为空");
        this.audioRecorder = Objects.requireNonNull(audioRecorder, "录音器不能为空");
        this.speechServices = Objects.requireNonNull(speechServices, "语音服务不能为空");
        this.llmClient = Objects.requireNonNull(llmClient, "LLM 客户端不能为空");
    }

    /**
     * 初始化会话
     *
     * @param prefs 用户配置
     */
    public void initialize(UserPreferences prefs) {
        applySettings(prefs);
        checkModelsOnStartup();
        sessionState.set(SessionState.IDLE);
        updateText(recordingStatusTextProperty, "未录音");
        updateText(detectionStatusTextProperty, "未启动");
        updateText(quietSecondsTextProperty, "0s");
        updateText(recordingSaveStatusTextProperty, prefs.isRecordingSaveEnabled() ? "启用" : "关闭");
        updateText(hintTextProperty, "就绪");
        registerListenersIfNeeded();
    }

    /**
     * 启动上课流程
     */
    public void startClass() {
        if (!sessionState.compareAndSet(SessionState.IDLE, SessionState.MONITORING)) {
            return;
        }
        audioRecorder.startRecording();
        recordingProperty.set(true);
        updateText(recordingStatusTextProperty, "录音中");
        updateText(detectionStatusTextProperty, "监听中");
        updateText(hintTextProperty, "正在监听");
        healthMonitor.startWatchdog();
        quietMillis = 0;
        logger.info("会话已进入监听状态");
    }

    /**
     * 停止上课流程
     */
    public void stopClass() {
        sessionState.set(SessionState.IDLE);
        audioRecorder.stopRecording();
        recordingProperty.set(false);
        updateText(recordingStatusTextProperty, "已停止");
        updateText(detectionStatusTextProperty, "未启动");
        updateText(hintTextProperty, "已停止");
        healthMonitor.stopWatchdog();
        quietMillis = 0;
        logger.info("会话已停止");
    }

    /**
     * 应用设置
     *
     * @param prefs 配置
     */
    public void applySettings(UserPreferences prefs) {
        Objects.requireNonNull(prefs, "配置不能为空");
        Validator.requireRange(prefs.getAudioLookbackSeconds(), 1, 300, "回溯秒数");
        this.currentPreferences = prefs;
        recordingRepository.cleanupOldRecordings(prefs.getRecordingRetentionDays());
        speechServices.getWakeWordDetector().initialize(modelRepository.getKwsModelDir(), prefs.getKeywords());
        speechServices.getSilenceDetector().initialize(modelRepository.getVadModelFile());
        speechServices.getSilenceDetector().setQuietThresholdSeconds(prefs.getVadQuietThresholdSeconds());
        speechServices.getSpeechRecognizer().initialize(modelRepository.getAsrModelDir());
        String recordingHint = prefs.isRecordingSaveEnabled() ? "录音保存已启用" : "录音保存已关闭";
        updateText(hintTextProperty, "设置已更新，" + recordingHint);
        updateText(recordingSaveStatusTextProperty, prefs.isRecordingSaveEnabled() ? "启用" : "关闭");
        quietMillis = 0;
        maybeWarnAiToken();
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        stopClass();
    }

    public BooleanProperty recordingProperty() {
        return recordingProperty;
    }

    public StringProperty recordingStatusTextProperty() {
        return recordingStatusTextProperty;
    }

    public StringProperty detectionStatusTextProperty() {
        return detectionStatusTextProperty;
    }

    public StringProperty quietSecondsTextProperty() {
        return quietSecondsTextProperty;
    }

    public StringProperty recordingSaveStatusTextProperty() {
        return recordingSaveStatusTextProperty;
    }

    public StringProperty lectureTextProperty() {
        return lectureTextProperty;
    }

    public StringProperty answerTextProperty() {
        return answerTextProperty;
    }

    public StringProperty hintTextProperty() {
        return hintTextProperty;
    }

    private void checkModelsOnStartup() {
        boolean requireSherpa = !"FAKE".equalsIgnoreCase(configManager.getSpeechEngineDefault());
        ModelCheckResult result = modelRepository.checkRequiredModels(requireSherpa);
        if (!result.ready()) {
            String message = "缺少模型: " + String.join("; ", result.missingItems());
            updateText(hintTextProperty, message);
            notificationService.showWarning("模型缺失", message);
        }
    }

    private void updateText(StringProperty property, String value) {
        if (Platform.isFxApplicationThread()) {
            property.set(value);
            return;
        }
        Platform.runLater(() -> property.set(value));
    }

    private void registerListenersIfNeeded() {
        if (!listenersRegistered.compareAndSet(false, true)) {
            return;
        }
        audioRecorder.addListener(this::handleAudioFrame);
        speechServices.getSilenceDetector().addListener(this::handleSilenceTimeout);
        speechServices.getWakeWordDetector().addListener(this::handleWakeWordDetected);
    }

    private void handleAudioFrame(byte[] data) {
        if (sessionState.get() != SessionState.MONITORING) {
            return;
        }
        float[] frame = AudioUtils.pcmToFloat(data);
        speechServices.getWakeWordDetector().detect(frame);

        int frameMillis = configManager.getAudioConfig().frameMillis();
        if (currentPreferences != null && currentPreferences.isVadEnabled()) {
            boolean hasSpeech = speechServices.getSilenceDetector().detect(frame, frameMillis);
            if (hasSpeech) {
                quietMillis = 0;
                updateText(quietSecondsTextProperty, "0s");
            } else {
                quietMillis += frameMillis;
                updateText(quietSecondsTextProperty, quietMillis / 1000 + "s");
            }
        }
    }

    private void handleWakeWordDetected(String keyword) {
        triggerRecognition("检测到唤醒词: " + keyword);
    }

    private void handleSilenceTimeout() {
        triggerRecognition("安静超时触发");
        quietMillis = 0;
        updateText(quietSecondsTextProperty, "0s");
    }

    private void triggerRecognition(String reason) {
        if (!sessionState.compareAndSet(SessionState.MONITORING, SessionState.TRIGGER_HANDLING)) {
            return;
        }
        long now = System.currentTimeMillis();
        int cooldownSeconds = configManager.getTriggerCooldownSeconds();
        if (now - lastTriggerAtMillis < cooldownSeconds * 1000L) {
            sessionState.set(SessionState.MONITORING);
            return;
        }
        lastTriggerAtMillis = now;
        updateText(detectionStatusTextProperty, reason);

        int lookbackSeconds = currentPreferences == null
            ? configManager.getAudioConfig().defaultLookbackSeconds()
            : currentPreferences.getAudioLookbackSeconds();
        byte[] pcm = audioRecorder.getAudioBefore(lookbackSeconds);
        speechServices.getSpeechRecognizer().recognizeAsync(pcm, new RecognitionListener() {
            @Override
            public void onResult(String text) {
                updateText(lectureTextProperty, text);
                maybeSaveRecording(pcm, text);
                handleAiAnswer(text);
                sessionState.set(SessionState.MONITORING);
                updateText(detectionStatusTextProperty, "监听中");
            }

            @Override
            public void onError(String error) {
                updateText(hintTextProperty, error);
                notificationService.showError("识别失败", error);
                sessionState.set(SessionState.MONITORING);
                updateText(detectionStatusTextProperty, "监听中");
            }
        });
    }

    private void maybeSaveRecording(byte[] pcm, String text) {
        if (currentPreferences == null || !currentPreferences.isRecordingSaveEnabled()) {
            return;
        }
        String prefix = (text == null || text.isBlank()) ? "recording" : "lecture";
        try {
            recordingRepository.saveRecording(pcm, prefix);
        } catch (Exception e) {
            logger.warn("保存录音失败: {}", e.getMessage());
        }
    }

    private void handleAiAnswer(String lectureText) {
        maybeWarnAiToken();
        String prompt = promptTemplate.build(lectureText);
        llmClient.generateAnswerAsync(prompt, new AnswerListener() {
            private final StringBuilder builder = new StringBuilder();

            @Override
            public void onToken(String token) {
                builder.append(token);
                updateText(answerTextProperty, builder.toString());
            }

            @Override
            public void onComplete(String answer) {
                updateText(answerTextProperty, answer);
            }

            @Override
            public void onError(String error) {
                updateText(hintTextProperty, error);
                notificationService.showError("AI 处理失败", error);
            }
        });
    }

    private void maybeWarnAiToken() {
        String token = preferencesManager.loadAiTokenPlainText();
        if (token != null && !token.isBlank()) {
            return;
        }
        if (!aiTokenWarned.compareAndSet(false, true)) {
            return;
        }
        notificationService.showWarning("未配置 API Key", "请在设置中填写 AI 平台的 Token / API Key");
        updateText(hintTextProperty, "未配置 API Key，请打开设置填写");
    }
}
