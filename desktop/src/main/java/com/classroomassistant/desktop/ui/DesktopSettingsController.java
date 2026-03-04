package com.classroomassistant.desktop.ui;

import com.classroomassistant.core.platform.PlatformPreferences;
import com.classroomassistant.core.platform.PlatformSecureStorage;
import com.classroomassistant.desktop.platform.DesktopPlatformProvider;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端设置页面控制器
 *
 * <p>与安卓端 SettingsScreen 保持功能对齐，使用 {@link PlatformPreferences} 和
 * {@link PlatformSecureStorage} (core 模块接口) 进行配置持久化，
 * 无需依赖 src 模块的 PreferencesManager。
 *
 * <p>偏好Key名称与安卓端/src端完全一致，确保跨平台配置可读性。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class DesktopSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(DesktopSettingsController.class);

    // ── 偏好Key常量（与 PreferencesManager / Android 保持一致）──
    private static final String KEY_KEYWORDS = "user.keywords";
    private static final String KEY_KWS_TRIGGER_THRESHOLD = "speech.kws.triggerThreshold";
    private static final String KEY_AUDIO_LOOKBACK_SECONDS = "audio.lookbackSeconds";
    private static final String KEY_VAD_ENABLED = "vad.enabled";
    private static final String KEY_VAD_QUIET_THRESHOLD_SECONDS = "vad.quietThresholdSeconds";
    private static final String KEY_VAD_QUIET_ALERT_MODE = "vad.quietAlertMode";
    private static final String KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED = "vad.quietAutoLookbackEnabled";
    private static final String KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS = "vad.quietAutoLookbackExtraSeconds";
    private static final String KEY_AI_PROVIDER = "ai.provider";
    private static final String KEY_AI_MODEL_NAME = "ai.modelName";
    private static final String KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled";
    private static final String KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays";
    private static final String KEY_ASR_LOCAL_ENABLED = "speech.asr.local.enabled";
    private static final String KEY_ASR_CLOUD_WHISPER_ENABLED = "speech.asr.cloud.whisper.enabled";
    private static final String KEY_WAKE_ALERT_MODE = "speech.kws.wakeAlertMode";
    private static final String KEY_LOG_MODE = "developer.log.mode";
    private static final String KEY_LOG_SHOW_DIAGNOSTIC = "developer.log.showDiagnostic";
    private static final String KEY_LOG_SHOW_AUDIO_DEVICE = "developer.log.showAudioDevice";
    private static final String KEY_LOG_SHOW_GAIN_ACTIVITY = "developer.log.showGainActivity";
    private static final String KEY_LOG_SHOW_TTS_SELF_TEST = "developer.log.showTtsSelfTest";
    private static final String KEY_LOG_SHOW_HEARTBEAT = "developer.log.showHeartbeat";
    private static final String KEY_TTS_SELF_TEST_ENABLED = "developer.tts.selfTest.enabled";
    private static final String KEY_BACKGROUND_KEEPALIVE_ENABLED = "listening.backgroundKeepAliveEnabled";

    // 加密存储 Key
    private static final String SECURE_AI_TOKEN = "ai.token";
    private static final String SECURE_AI_SECRET = "ai.secret";
    private static final String SECURE_SPEECH_API_KEY = "speech.apiKey";

    /** AI 平台类型枚举（与 LLMConfig.ModelType 保持一致，桌面端仅依赖 core 所以本地定义） */
    private enum AiProvider {
        OPENAI, QIANFAN, DEEPSEEK, KIMI
    }

    /** 各 AI 平台的推荐模型名称映射，与安卓端 SettingsScreen 一致 */
    private static final Map<AiProvider, List<String>> MODEL_NAME_SUGGESTIONS = Map.of(
        AiProvider.OPENAI, List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini"),
        AiProvider.DEEPSEEK, List.of("deepseek-chat", "deepseek-reasoner"),
        AiProvider.KIMI, List.of("moonshot-v1-8k", "moonshot-v1-32k"),
        AiProvider.QIANFAN, List.of("ernie-4.0-8k", "ernie-3.5-8k")
    );

    private final DesktopPlatformProvider platformProvider;
    private final PlatformPreferences prefs;
    private final PlatformSecureStorage secureStorage;

    // ToggleGroups
    private final ToggleGroup wakeAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup quietAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup logModeToggleGroup = new ToggleGroup();

    // ── 唤醒词 ──────────────────────────────────────────────────────────
    @FXML private javafx.scene.control.TextField keywordsField;
    @FXML private Slider kwsThresholdSlider;
    @FXML private Label kwsThresholdLabel;

    // ── 唤醒提示 ────────────────────────────────────────────────────────
    @FXML private RadioButton wakeAlertNotificationOnly;
    @FXML private RadioButton wakeAlertSound;

    // ── 安静检测（VAD）──────────────────────────────────────────────────
    @FXML private CheckBox vadEnabledCheckBox;
    @FXML private Spinner<Integer> quietThresholdSpinner;
    @FXML private RadioButton quietAlertNotificationOnly;
    @FXML private RadioButton quietAlertSound;
    @FXML private CheckBox quietAutoLookbackCheckBox;
    @FXML private Spinner<Integer> quietAutoLookbackExtraSpinner;

    // ── 语音回溯 ────────────────────────────────────────────────────────
    @FXML private Spinner<Integer> lookbackSecondsSpinner;

    // ── 录音保存 ────────────────────────────────────────────────────────
    @FXML private CheckBox recordingSaveCheckBox;
    @FXML private Spinner<Integer> recordingRetentionSpinner;

    // ── AI 问答 ─────────────────────────────────────────────────────────
    @FXML private ComboBox<AiProvider> providerComboBox;
    @FXML private ComboBox<String> modelNameComboBox;
    @FXML private PasswordField tokenField;
    @FXML private VBox secretKeyBox;
    @FXML private PasswordField secretKeyField;

    // ── 语音识别路线 ────────────────────────────────────────────────────
    @FXML private CheckBox localAsrEnabledCheckBox;
    @FXML private CheckBox cloudWhisperEnabledCheckBox;
    @FXML private VBox speechApiKeyBox;
    @FXML private PasswordField speechApiKeyField;

    // ── 开发者选项 ──────────────────────────────────────────────────────
    @FXML private RadioButton logModeSimple;
    @FXML private RadioButton logModeFull;
    @FXML private VBox logSubCategoryBox;
    @FXML private CheckBox showDiagnosticLogsCheckBox;
    @FXML private CheckBox showAudioDeviceLogsCheckBox;
    @FXML private CheckBox showGainActivityLogsCheckBox;
    @FXML private CheckBox showTtsSelfTestLogsCheckBox;
    @FXML private CheckBox showHeartbeatLogsCheckBox;
    @FXML private CheckBox ttsSelfTestEnabledCheckBox;
    // ── 后台保活 ──────────────────────────────────────────────────────────
    @FXML private CheckBox backgroundKeepAliveCheckBox;
    // ── 通用 ────────────────────────────────────────────────────────────
    @FXML private Label statusLabel;

    // ── 模型管理区（桌面端精简版，无 ModelDownloadManager）──
    @FXML private ComboBox<?> currentModelComboBox;
    @FXML private VBox modelOptionsBox;
    @FXML private VBox auxModelOptionsBox;
    @FXML private Button downloadSelectedButton;
    @FXML private Button refreshModelStatusButton;
    @FXML private Label modelDownloadStatusLabel;

    /**
     * 构造桌面端设置控制器
     *
     * @param platformProvider 桌面平台提供者
     */
    public DesktopSettingsController(DesktopPlatformProvider platformProvider) {
        this.platformProvider = platformProvider;
        this.prefs = platformProvider.getPreferences();
        this.secureStorage = platformProvider.getSecureStorage();
    }

    // =========================================================================
    // 初始化
    // =========================================================================

    @FXML
    private void initialize() {
        // ── Spinner 取值范围 ──
        quietThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 30, 5));
        lookbackSecondsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 120, 15));
        recordingRetentionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 7));
        recordingRetentionSpinner.disableProperty().bind(recordingSaveCheckBox.selectedProperty().not());
        quietAutoLookbackExtraSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 8));
        quietAutoLookbackExtraSpinner.disableProperty().bind(quietAutoLookbackCheckBox.selectedProperty().not());

        // ── KWS 阈值滑块 ──
        kwsThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double rounded = Math.round(newVal.doubleValue() * 100.0) / 100.0;
            kwsThresholdLabel.setText(String.format("%.2f", rounded));
        });

        // ── ToggleGroup 设置 ──
        wakeAlertNotificationOnly.setToggleGroup(wakeAlertToggleGroup);
        wakeAlertSound.setToggleGroup(wakeAlertToggleGroup);
        quietAlertNotificationOnly.setToggleGroup(quietAlertToggleGroup);
        quietAlertSound.setToggleGroup(quietAlertToggleGroup);
        logModeSimple.setToggleGroup(logModeToggleGroup);
        logModeFull.setToggleGroup(logModeToggleGroup);

        // ── AI 提供商 ComboBox ──
        providerComboBox.setItems(FXCollections.observableArrayList(AiProvider.values()));
        providerComboBox.getSelectionModel().select(AiProvider.QIANFAN);
        providerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateModelNameSuggestions(newVal);
            boolean isQianfan = newVal == AiProvider.QIANFAN;
            secretKeyBox.setVisible(isQianfan);
            secretKeyBox.setManaged(isQianfan);
        });

        // ── 云端 Whisper 条件可见性 ──
        cloudWhisperEnabledCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            speechApiKeyBox.setVisible(newVal);
            speechApiKeyBox.setManaged(newVal);
        });

        // ── 日志模式条件可见性 ──
        logModeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFull = newVal == logModeFull;
            logSubCategoryBox.setVisible(isFull);
            logSubCategoryBox.setManaged(isFull);
        });

        // ── Token 输入实时校验提示 ──
        tokenField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                statusLabel.setText("提示：未设置 Token 将无法使用 AI 问答");
                statusLabel.setStyle("-fx-text-fill: #FF9800;");
            } else {
                statusLabel.setText("");
            }
        });

        // ── 桌面端模型管理区暂不可用，隐藏相关按钮 ──
        if (downloadSelectedButton != null) {
            downloadSelectedButton.setDisable(true);
        }
        if (refreshModelStatusButton != null) {
            refreshModelStatusButton.setDisable(true);
        }
        if (modelDownloadStatusLabel != null) {
            modelDownloadStatusLabel.setText("桌面端模型下载功能开发中");
        }

        // ── 加载已有配置 ──
        loadPreferences();
    }

    /**
     * 从 PlatformPreferences / PlatformSecureStorage 加载配置并填充 UI
     */
    private void loadPreferences() {
        // 唤醒词
        keywordsField.setText(prefs.getString(KEY_KEYWORDS, ""));
        float kwsThreshold = prefs.getInt(KEY_KWS_TRIGGER_THRESHOLD, 25) / 100f;
        kwsThreshold = Math.max(0.05f, Math.min(0.8f, kwsThreshold));
        kwsThresholdSlider.setValue(kwsThreshold);
        kwsThresholdLabel.setText(String.format("%.2f", kwsThreshold));

        // 唤醒提示
        String wakeAlertMode = prefs.getString(KEY_WAKE_ALERT_MODE, "NOTIFICATION_ONLY");
        if ("SOUND".equals(wakeAlertMode)) {
            wakeAlertSound.setSelected(true);
        } else {
            wakeAlertNotificationOnly.setSelected(true);
        }

        // 安静检测
        vadEnabledCheckBox.setSelected(prefs.getBoolean(KEY_VAD_ENABLED, true));
        int quietThreshold = Math.max(3, Math.min(30, prefs.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5)));
        quietThresholdSpinner.getValueFactory().setValue(quietThreshold);
        String quietAlertMode = prefs.getString(KEY_VAD_QUIET_ALERT_MODE, "NOTIFICATION_ONLY");
        if ("SOUND".equals(quietAlertMode)) {
            quietAlertSound.setSelected(true);
        } else {
            quietAlertNotificationOnly.setSelected(true);
        }
        quietAutoLookbackCheckBox.setSelected(prefs.getBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, true));
        int extraSeconds = Math.max(1, Math.min(60, prefs.getInt(KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS, 8)));
        quietAutoLookbackExtraSpinner.getValueFactory().setValue(extraSeconds);

        // 语音回溯
        int lookback = Math.max(8, Math.min(120, prefs.getInt(KEY_AUDIO_LOOKBACK_SECONDS, 15)));
        lookbackSecondsSpinner.getValueFactory().setValue(lookback);

        // 录音保存
        recordingSaveCheckBox.setSelected(prefs.getBoolean(KEY_RECORDING_SAVE_ENABLED, false));
        int retention = Math.max(0, Math.min(30, prefs.getInt(KEY_RECORDING_RETENTION_DAYS, 7)));
        recordingRetentionSpinner.getValueFactory().setValue(retention);

        // AI 问答
        String providerName = prefs.getString(KEY_AI_PROVIDER, AiProvider.QIANFAN.name());
        AiProvider provider;
        try {
            provider = AiProvider.valueOf(providerName);
        } catch (Exception e) {
            provider = AiProvider.QIANFAN;
        }
        providerComboBox.getSelectionModel().select(provider);
        updateModelNameSuggestions(provider);
        String savedModelName = prefs.getString(KEY_AI_MODEL_NAME, "");
        if (!savedModelName.isBlank()) {
            modelNameComboBox.getEditor().setText(savedModelName);
        }
        boolean isQianfan = provider == AiProvider.QIANFAN;
        secretKeyBox.setVisible(isQianfan);
        secretKeyBox.setManaged(isQianfan);

        // 加载加密存储的密钥
        String token = secureStorage.retrieveSecure(SECURE_AI_TOKEN);
        if (token != null && !token.isBlank()) {
            tokenField.setText(token);
        }
        String secretKey = secureStorage.retrieveSecure(SECURE_AI_SECRET);
        if (secretKey != null && !secretKey.isBlank()) {
            secretKeyField.setText(secretKey);
        }

        // 语音识别路线
        localAsrEnabledCheckBox.setSelected(prefs.getBoolean(KEY_ASR_LOCAL_ENABLED, true));
        boolean cloudWhisperEnabled = prefs.getBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, false);
        cloudWhisperEnabledCheckBox.setSelected(cloudWhisperEnabled);
        speechApiKeyBox.setVisible(cloudWhisperEnabled);
        speechApiKeyBox.setManaged(cloudWhisperEnabled);
        String speechKey = secureStorage.retrieveSecure(SECURE_SPEECH_API_KEY);
        if (speechKey != null && !speechKey.isBlank()) {
            speechApiKeyField.setText(speechKey);
        }

        // 开发者选项
        String logMode = prefs.getString(KEY_LOG_MODE, "SIMPLE");
        if ("FULL".equals(logMode)) {
            logModeFull.setSelected(true);
            logSubCategoryBox.setVisible(true);
            logSubCategoryBox.setManaged(true);
        } else {
            logModeSimple.setSelected(true);
            logSubCategoryBox.setVisible(false);
            logSubCategoryBox.setManaged(false);
        }
        showDiagnosticLogsCheckBox.setSelected(prefs.getBoolean(KEY_LOG_SHOW_DIAGNOSTIC, false));
        showAudioDeviceLogsCheckBox.setSelected(prefs.getBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, false));
        showGainActivityLogsCheckBox.setSelected(prefs.getBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, false));
        showTtsSelfTestLogsCheckBox.setSelected(prefs.getBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, false));
        showHeartbeatLogsCheckBox.setSelected(prefs.getBoolean(KEY_LOG_SHOW_HEARTBEAT, false));
        ttsSelfTestEnabledCheckBox.setSelected(prefs.getBoolean(KEY_TTS_SELF_TEST_ENABLED, false));

        // 后台保活
        backgroundKeepAliveCheckBox.setSelected(prefs.getBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, true));
    }

    /**
     * 根据选中的 AI 平台更新模型名称下拉建议
     */
    private void updateModelNameSuggestions(AiProvider provider) {
        if (modelNameComboBox == null || provider == null) {
            return;
        }
        String currentText = modelNameComboBox.getEditor().getText();
        List<String> suggestions = MODEL_NAME_SUGGESTIONS.getOrDefault(provider, List.of());
        modelNameComboBox.setItems(FXCollections.observableArrayList(suggestions));
        if (currentText != null && !currentText.isBlank()) {
            modelNameComboBox.getEditor().setText(currentText);
        } else if (!suggestions.isEmpty()) {
            modelNameComboBox.getEditor().setText(suggestions.get(0));
        }
    }

    // =========================================================================
    // 保存 / 取消
    // =========================================================================

    @FXML
    private void save() {
        try {
            // 唤醒词
            String keywords = keywordsField.getText() == null ? "" : keywordsField.getText().trim();
            prefs.putString(KEY_KEYWORDS, keywords);

            // KWS 阈值（以百分比整数存储，与 PreferencesManager 一致）
            float kwsThreshold = (float) kwsThresholdSlider.getValue();
            prefs.putInt(KEY_KWS_TRIGGER_THRESHOLD, (int) (Math.max(0.05f, Math.min(0.8f, kwsThreshold)) * 100));

            // 唤醒提示
            prefs.putString(KEY_WAKE_ALERT_MODE, wakeAlertSound.isSelected() ? "SOUND" : "NOTIFICATION_ONLY");

            // 安静检测
            prefs.putBoolean(KEY_VAD_ENABLED, vadEnabledCheckBox.isSelected());
            prefs.putInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, quietThresholdSpinner.getValue());
            prefs.putString(KEY_VAD_QUIET_ALERT_MODE, quietAlertSound.isSelected() ? "SOUND" : "NOTIFICATION_ONLY");
            prefs.putBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, quietAutoLookbackCheckBox.isSelected());
            prefs.putInt(KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS, quietAutoLookbackExtraSpinner.getValue());

            // 语音回溯
            prefs.putInt(KEY_AUDIO_LOOKBACK_SECONDS, lookbackSecondsSpinner.getValue());

            // 录音保存
            prefs.putBoolean(KEY_RECORDING_SAVE_ENABLED, recordingSaveCheckBox.isSelected());
            prefs.putInt(KEY_RECORDING_RETENTION_DAYS, recordingRetentionSpinner.getValue());

            // AI 问答
            AiProvider provider = providerComboBox.getSelectionModel().getSelectedItem();
            prefs.putString(KEY_AI_PROVIDER, provider != null ? provider.name() : AiProvider.QIANFAN.name());
            String modelName = modelNameComboBox.getEditor().getText();
            prefs.putString(KEY_AI_MODEL_NAME, modelName == null ? "" : modelName.trim());

            // 加密保存密钥
            String token = tokenField.getText();
            if (token != null && !token.isBlank()) {
                secureStorage.storeSecure(SECURE_AI_TOKEN, token.trim());
            }
            String secretKey = secretKeyField.getText();
            if (secretKey != null && !secretKey.isBlank()) {
                secureStorage.storeSecure(SECURE_AI_SECRET, secretKey.trim());
            }

            // 语音识别路线
            prefs.putBoolean(KEY_ASR_LOCAL_ENABLED, localAsrEnabledCheckBox.isSelected());
            prefs.putBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, cloudWhisperEnabledCheckBox.isSelected());
            String speechKey = speechApiKeyField.getText();
            if (speechKey != null && !speechKey.isBlank()) {
                secureStorage.storeSecure(SECURE_SPEECH_API_KEY, speechKey.trim());
            }

            // 开发者选项
            prefs.putString(KEY_LOG_MODE, logModeFull.isSelected() ? "FULL" : "SIMPLE");
            prefs.putBoolean(KEY_LOG_SHOW_DIAGNOSTIC, showDiagnosticLogsCheckBox.isSelected());
            prefs.putBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, showAudioDeviceLogsCheckBox.isSelected());
            prefs.putBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, showGainActivityLogsCheckBox.isSelected());
            prefs.putBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, showTtsSelfTestLogsCheckBox.isSelected());
            prefs.putBoolean(KEY_LOG_SHOW_HEARTBEAT, showHeartbeatLogsCheckBox.isSelected());
            prefs.putBoolean(KEY_TTS_SELF_TEST_ENABLED, ttsSelfTestEnabledCheckBox.isSelected());

            // 后台保活
            prefs.putBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, backgroundKeepAliveCheckBox.isSelected());

            prefs.flush();

            statusLabel.setText("✓ 保存成功");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");

            // 延迟关闭
            new Thread(() -> {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException ignored) {
                }
                Platform.runLater(this::closeWindow);
            }).start();
        } catch (Exception e) {
            logger.error("保存设置失败", e);
            statusLabel.setText("✗ 保存失败: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    // ── 模型管理桩方法（桌面端暂未实现完整的模型下载管理）──
    @FXML
    private void downloadSelectedModels() {
        platformProvider.showToast("桌面端模型下载功能开发中");
    }

    @FXML
    private void refreshModelStatus() {
        if (modelDownloadStatusLabel != null) {
            modelDownloadStatusLabel.setText("桌面端模型状态查询开发中");
        }
    }

    // =========================================================================
    // 窗口管理
    // =========================================================================

    private void closeWindow() {
        if (statusLabel != null && statusLabel.getScene() != null) {
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.close();
        }
    }
}
