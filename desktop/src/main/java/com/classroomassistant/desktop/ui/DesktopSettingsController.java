package com.classroomassistant.desktop.ui;

import com.classroomassistant.core.platform.PlatformPreferences;
import com.classroomassistant.core.platform.PlatformSecureStorage;
import com.classroomassistant.desktop.ai.OpenAiModelCatalogService;
import com.classroomassistant.desktop.platform.DesktopPlatformProvider;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端设置页面控制器
 *
 * <p>
 * 与安卓端 SettingsScreen 保持功能对齐，使用 {@link PlatformPreferences} 和
 * {@link PlatformSecureStorage} (core 模块接口) 进行配置持久化，
 * 无需依赖 src 模块的 PreferencesManager。
 *
 * <p>
 * 偏好Key名称与安卓端/src端完全一致，确保跨平台配置可读性。
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
    private static final String KEY_AI_BASE_URL = "ai.baseUrl";
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

    /** AI 平台类型枚举（与 src 端 ModelType 保持一致） */
    private enum AiProvider {
        OPENAI,
        OPENAI_COMPATIBLE,
        ANTHROPIC,
        GEMINI,
        DEEPSEEK,
        QIANFAN,
        DASHSCOPE,
        HUNYUAN,
        ZHIPU,
        KIMI,
        GROQ,
        MISTRAL,
        COHERE,
        OPENROUTER,
        AZURE_OPENAI,
        SILICONFLOW,
        MINIMAX,
        BAICHUAN,
        YI,
        STEPFUN,
        XAI,
        FIREWORKS,
        TOGETHER_AI,
        PERPLEXITY,
        NOVITA,
        REPLICATE,
        CEREBRAS,
        SAMBANOVA,
        OLLAMA,
        LMSTUDIO
    }

    /** AI 平台按主流程度排序（30种） */
    private static final List<AiProvider> PROVIDER_ORDER = List.of(
            AiProvider.OPENAI,
            AiProvider.OPENAI_COMPATIBLE,
            AiProvider.ANTHROPIC,
            AiProvider.GEMINI,
            AiProvider.DEEPSEEK,
            AiProvider.QIANFAN,
            AiProvider.DASHSCOPE,
            AiProvider.HUNYUAN,
            AiProvider.ZHIPU,
            AiProvider.KIMI,
            AiProvider.GROQ,
            AiProvider.MISTRAL,
            AiProvider.COHERE,
            AiProvider.OPENROUTER,
            AiProvider.AZURE_OPENAI,
            AiProvider.SILICONFLOW,
            AiProvider.MINIMAX,
            AiProvider.BAICHUAN,
            AiProvider.YI,
            AiProvider.STEPFUN,
            AiProvider.XAI,
            AiProvider.FIREWORKS,
            AiProvider.TOGETHER_AI,
            AiProvider.PERPLEXITY,
            AiProvider.NOVITA,
            AiProvider.REPLICATE,
            AiProvider.CEREBRAS,
            AiProvider.SAMBANOVA,
            AiProvider.OLLAMA,
            AiProvider.LMSTUDIO);

    /** AI 平台展示名 */
    private static final Map<AiProvider, String> PROVIDER_DISPLAY_NAMES = Map.ofEntries(
            Map.entry(AiProvider.OPENAI, "OpenAI 官方"),
            Map.entry(AiProvider.OPENAI_COMPATIBLE, "OpenAI 兼容 / 中转站"),
            Map.entry(AiProvider.ANTHROPIC, "Anthropic"),
            Map.entry(AiProvider.GEMINI, "Google Gemini"),
            Map.entry(AiProvider.QIANFAN, "百度千帆"),
            Map.entry(AiProvider.DEEPSEEK, "DeepSeek"),
            Map.entry(AiProvider.KIMI, "Kimi (Moonshot)"),
            Map.entry(AiProvider.DASHSCOPE, "阿里百炼 (DashScope)"),
            Map.entry(AiProvider.HUNYUAN, "腾讯混元"),
            Map.entry(AiProvider.ZHIPU, "智谱 AI"),
            Map.entry(AiProvider.SILICONFLOW, "硅基流动"),
            Map.entry(AiProvider.MINIMAX, "MiniMax"),
            Map.entry(AiProvider.MISTRAL, "Mistral"),
            Map.entry(AiProvider.GROQ, "Groq"),
            Map.entry(AiProvider.COHERE, "Cohere"),
            Map.entry(AiProvider.OPENROUTER, "OpenRouter"),
            Map.entry(AiProvider.AZURE_OPENAI, "Azure OpenAI"),
            Map.entry(AiProvider.BAICHUAN, "百川智能"),
            Map.entry(AiProvider.YI, "零一万物 Yi"),
            Map.entry(AiProvider.STEPFUN, "阶跃星辰 StepFun"),
            Map.entry(AiProvider.XAI, "xAI"),
            Map.entry(AiProvider.FIREWORKS, "Fireworks AI"),
            Map.entry(AiProvider.TOGETHER_AI, "Together AI"),
            Map.entry(AiProvider.PERPLEXITY, "Perplexity"),
            Map.entry(AiProvider.NOVITA, "Novita"),
            Map.entry(AiProvider.REPLICATE, "Replicate"),
            Map.entry(AiProvider.CEREBRAS, "Cerebras"),
            Map.entry(AiProvider.SAMBANOVA, "SambaNova"),
            Map.entry(AiProvider.OLLAMA, "Ollama（本地）"),
            Map.entry(AiProvider.LMSTUDIO, "LM Studio（本地）"));

    /** 各 AI 平台的推荐模型名称映射 */
    private static final Map<AiProvider, List<String>> MODEL_NAME_SUGGESTIONS = Map.ofEntries(
            Map.entry(AiProvider.OPENAI, List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini")),
            Map.entry(AiProvider.OPENAI_COMPATIBLE,
                    List.of("gpt-4o-mini", "deepseek-chat", "qwen-plus", "claude-3-5-sonnet-20241022")),
            Map.entry(AiProvider.ANTHROPIC,
                    List.of("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")),
            Map.entry(AiProvider.GEMINI,
                    List.of("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")),
            Map.entry(AiProvider.QIANFAN, List.of("ernie-4.0-8k", "ernie-3.5-8k")),
            Map.entry(AiProvider.DEEPSEEK, List.of("deepseek-chat", "deepseek-reasoner")),
            Map.entry(AiProvider.KIMI, List.of("moonshot-v1-8k", "moonshot-v1-32k")),
            Map.entry(AiProvider.DASHSCOPE, List.of("qwen-plus", "qwen-turbo", "qwen-max")),
            Map.entry(AiProvider.HUNYUAN, List.of("hunyuan-lite", "hunyuan-standard", "hunyuan-pro")),
            Map.entry(AiProvider.ZHIPU, List.of("glm-4-flash", "glm-4-plus", "glm-4-air")),
            Map.entry(AiProvider.SILICONFLOW,
                    List.of("Qwen/Qwen2.5-7B-Instruct", "deepseek-ai/DeepSeek-V3", "Llama-3.1-8B-Instruct")),
            Map.entry(AiProvider.MINIMAX, List.of("abab6.5s-chat", "abab6.5t-chat", "MiniMax-Text-01")),
            Map.entry(AiProvider.MISTRAL,
                    List.of("mistral-small-latest", "mistral-large-latest", "open-mistral-nemo")),
            Map.entry(AiProvider.GROQ,
                    List.of("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")),
            Map.entry(AiProvider.COHERE, List.of("command-r-plus", "command-r", "command-light")),
            Map.entry(AiProvider.OPENROUTER,
                    List.of("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001")),
            Map.entry(AiProvider.AZURE_OPENAI, List.of("gpt-4o", "gpt-4o-mini", "o3-mini")),
            Map.entry(AiProvider.BAICHUAN, List.of("Baichuan4", "Baichuan3-Turbo", "Baichuan2-Turbo")),
            Map.entry(AiProvider.YI, List.of("yi-large", "yi-medium", "yi-lightning")),
            Map.entry(AiProvider.STEPFUN, List.of("step-2", "step-1v-8k", "step-1-8k")),
            Map.entry(AiProvider.XAI, List.of("grok-2-1212", "grok-beta", "grok-vision-beta")),
            Map.entry(AiProvider.FIREWORKS,
                    List.of("accounts/fireworks/models/llama-v3p1-70b-instruct", "accounts/fireworks/models/qwen2p5-72b-instruct")),
            Map.entry(AiProvider.TOGETHER_AI,
                    List.of("meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo", "Qwen/Qwen2.5-72B-Instruct-Turbo")),
            Map.entry(AiProvider.PERPLEXITY,
                    List.of("sonar", "sonar-pro", "llama-3.1-sonar-large-128k-online")),
            Map.entry(AiProvider.NOVITA,
                    List.of("meta-llama/llama-3.1-70b-instruct", "deepseek/deepseek-r1", "qwen/qwen2.5-72b-instruct")),
            Map.entry(AiProvider.REPLICATE,
                    List.of("meta/meta-llama-3-70b-instruct", "mistralai/mistral-7b-instruct", "anthropic/claude-3-opus")),
            Map.entry(AiProvider.CEREBRAS,
                    List.of("llama3.1-8b", "llama3.1-70b", "qwen-2.5-72b")),
            Map.entry(AiProvider.SAMBANOVA,
                    List.of("Meta-Llama-3.1-405B-Instruct", "Meta-Llama-3.1-70B-Instruct", "Qwen2.5-72B-Instruct")),
            Map.entry(AiProvider.OLLAMA, List.of("qwen2.5:7b", "llama3.1:8b", "deepseek-r1:8b")),
            Map.entry(AiProvider.LMSTUDIO, List.of("local-model", "qwen2.5-7b-instruct", "llama-3.1-8b")));

    /** 各平台默认 Base URL（OpenAI 兼容类型） */
    private static final Map<AiProvider, String> DEFAULT_BASE_URLS = Map.ofEntries(
            Map.entry(AiProvider.OPENAI, "https://api.openai.com"),
            Map.entry(AiProvider.OPENAI_COMPATIBLE, ""),
            Map.entry(AiProvider.ANTHROPIC, "https://api.anthropic.com/v1"),
            Map.entry(AiProvider.GEMINI, "https://generativelanguage.googleapis.com/v1beta/openai"),
            Map.entry(AiProvider.QIANFAN, ""),
            Map.entry(AiProvider.DEEPSEEK, "https://api.deepseek.com"),
            Map.entry(AiProvider.KIMI, "https://api.moonshot.cn/v1"),
            Map.entry(AiProvider.DASHSCOPE, "https://dashscope.aliyuncs.com/compatible-mode/v1"),
            Map.entry(AiProvider.HUNYUAN, "https://api.hunyuan.cloud.tencent.com/v1"),
            Map.entry(AiProvider.ZHIPU, "https://open.bigmodel.cn/api/paas/v4"),
            Map.entry(AiProvider.SILICONFLOW, "https://api.siliconflow.cn/v1"),
            Map.entry(AiProvider.MINIMAX, "https://api.minimax.chat/v1"),
            Map.entry(AiProvider.MISTRAL, "https://api.mistral.ai/v1"),
            Map.entry(AiProvider.GROQ, "https://api.groq.com/openai/v1"),
            Map.entry(AiProvider.COHERE, "https://api.cohere.ai/compatibility/v1"),
            Map.entry(AiProvider.OPENROUTER, "https://openrouter.ai/api/v1"),
            Map.entry(AiProvider.AZURE_OPENAI, ""),
            Map.entry(AiProvider.BAICHUAN, "https://api.baichuan-ai.com/v1"),
            Map.entry(AiProvider.YI, "https://api.lingyiwanwu.com/v1"),
            Map.entry(AiProvider.STEPFUN, "https://api.stepfun.com/v1"),
            Map.entry(AiProvider.XAI, "https://api.x.ai/v1"),
            Map.entry(AiProvider.FIREWORKS, "https://api.fireworks.ai/inference/v1"),
            Map.entry(AiProvider.TOGETHER_AI, "https://api.together.xyz/v1"),
            Map.entry(AiProvider.PERPLEXITY, "https://api.perplexity.ai"),
            Map.entry(AiProvider.NOVITA, "https://api.novita.ai/v3/openai"),
            Map.entry(AiProvider.REPLICATE, "https://api.replicate.com/v1"),
            Map.entry(AiProvider.CEREBRAS, "https://api.cerebras.ai/v1"),
            Map.entry(AiProvider.SAMBANOVA, "https://api.sambanova.ai/v1"),
            Map.entry(AiProvider.OLLAMA, "http://127.0.0.1:11434/v1"),
            Map.entry(AiProvider.LMSTUDIO, "http://127.0.0.1:1234/v1"));

    private final DesktopPlatformProvider platformProvider;
    private final PlatformPreferences prefs;
    private final PlatformSecureStorage secureStorage;
    private final OpenAiModelCatalogService openAiModelCatalogService = new OpenAiModelCatalogService();
    private final ExecutorService modelCatalogExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "desktop-openai-model-catalog-fetcher");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, List<String>> dynamicModelSuggestionsCache = new ConcurrentHashMap<>();
    private final AtomicLong modelFetchSequence = new AtomicLong(0);

    // ToggleGroups
    private final ToggleGroup wakeAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup quietAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup logModeToggleGroup = new ToggleGroup();

    // ── 唤醒词 ──────────────────────────────────────────────────────────
    @FXML
    private javafx.scene.control.TextField keywordsField;
    @FXML
    private Slider kwsThresholdSlider;
    @FXML
    private Label kwsThresholdLabel;

    // ── 唤醒提示 ────────────────────────────────────────────────────────
    @FXML
    private RadioButton wakeAlertNotificationOnly;
    @FXML
    private RadioButton wakeAlertSound;

    // ── 安静检测（VAD）──────────────────────────────────────────────────
    @FXML
    private CheckBox vadEnabledCheckBox;
    @FXML
    private Spinner<Integer> quietThresholdSpinner;
    @FXML
    private RadioButton quietAlertNotificationOnly;
    @FXML
    private RadioButton quietAlertSound;
    @FXML
    private CheckBox quietAutoLookbackCheckBox;
    @FXML
    private Spinner<Integer> quietAutoLookbackExtraSpinner;

    // ── 语音回溯 ────────────────────────────────────────────────────────
    @FXML
    private Spinner<Integer> lookbackSecondsSpinner;

    // ── 录音保存 ────────────────────────────────────────────────────────
    @FXML
    private CheckBox recordingSaveCheckBox;
    @FXML
    private Spinner<Integer> recordingRetentionSpinner;

    // ── AI 问答 ─────────────────────────────────────────────────────────
    @FXML
    private ComboBox<AiProvider> providerComboBox;
    @FXML
    private ComboBox<String> modelNameComboBox;
    @FXML
    private TextField baseUrlField;
    @FXML
    private Button refreshModelListButton;
    @FXML
    private PasswordField tokenField;
    @FXML
    private VBox secretKeyBox;
    @FXML
    private PasswordField secretKeyField;

    // ── 语音识别路线 ────────────────────────────────────────────────────
    @FXML
    private CheckBox localAsrEnabledCheckBox;
    @FXML
    private CheckBox cloudWhisperEnabledCheckBox;
    @FXML
    private VBox speechApiKeyBox;
    @FXML
    private PasswordField speechApiKeyField;

    // ── 开发者选项 ──────────────────────────────────────────────────────
    @FXML
    private RadioButton logModeSimple;
    @FXML
    private RadioButton logModeFull;
    @FXML
    private VBox logSubCategoryBox;
    @FXML
    private CheckBox showDiagnosticLogsCheckBox;
    @FXML
    private CheckBox showAudioDeviceLogsCheckBox;
    @FXML
    private CheckBox showGainActivityLogsCheckBox;
    @FXML
    private CheckBox showTtsSelfTestLogsCheckBox;
    @FXML
    private CheckBox showHeartbeatLogsCheckBox;
    @FXML
    private CheckBox ttsSelfTestEnabledCheckBox;
    // ── 后台保活 ──────────────────────────────────────────────────────────
    @FXML
    private CheckBox backgroundKeepAliveCheckBox;
    // ── 通用 ────────────────────────────────────────────────────────────
    @FXML
    private Label statusLabel;

    // ── 模型管理区（桌面端精简版，无 ModelDownloadManager）──
    @FXML
    private ComboBox<?> currentModelComboBox;
    @FXML
    private VBox modelOptionsBox;
    @FXML
    private VBox auxModelOptionsBox;
    @FXML
    private Button downloadSelectedButton;
    @FXML
    private Button refreshModelStatusButton;
    @FXML
    private Label modelDownloadStatusLabel;

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

        // ── AI 提供商 ComboBox（按主流程度排序）──
        providerComboBox.setItems(FXCollections.observableArrayList(PROVIDER_ORDER));
        providerComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(AiProvider object) {
                if (object == null) {
                    return "";
                }
                return PROVIDER_DISPLAY_NAMES.getOrDefault(object, object.name());
            }

            @Override
            public AiProvider fromString(String string) {
                return null;
            }
        });
        providerComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AiProvider item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(PROVIDER_DISPLAY_NAMES.getOrDefault(item, item.name()));
                }
            }
        });
        providerComboBox.getSelectionModel().select(AiProvider.QIANFAN);
        providerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateModelNameSuggestions(newVal);
            boolean isQianfan = newVal == AiProvider.QIANFAN;
            secretKeyBox.setVisible(isQianfan);
            secretKeyBox.setManaged(isQianfan);
            applyDefaultBaseUrlIfNeeded(oldVal, newVal);
            if (refreshModelListButton != null) {
                boolean compatible = isOpenAiCompatibleProvider(newVal);
                refreshModelListButton.setVisible(compatible);
                refreshModelListButton.setManaged(compatible);
            }
            triggerAutoModelRefresh();
        });
        if (refreshModelListButton != null) {
            refreshModelListButton.setDisable(false);
        }

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
        if (baseUrlField != null) {
            baseUrlField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (Boolean.FALSE.equals(newVal)) {
                    triggerAutoModelRefresh();
                }
            });
        }

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
        Platform.runLater(this::bindWindowClose);
    }

    /**
     * 从 PlatformPreferences / PlatformSecureStorage 加载配置并填充 UI
     */
    private void loadPreferences() {
        // 唤醒词
        keywordsField.setText(prefs.getString(KEY_KEYWORDS, ""));
        float kwsThreshold = prefs.getInt(KEY_KWS_TRIGGER_THRESHOLD, 5) / 100f;
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
        if (baseUrlField != null) {
            String savedBaseUrl = prefs.getString(KEY_AI_BASE_URL, "");
            if (savedBaseUrl != null && !savedBaseUrl.isBlank()) {
                baseUrlField.setText(savedBaseUrl);
            } else {
                baseUrlField.setText(defaultBaseUrlFor(provider));
            }
        }
        if (refreshModelListButton != null) {
            boolean compatible = isOpenAiCompatibleProvider(provider);
            refreshModelListButton.setVisible(compatible);
            refreshModelListButton.setManaged(compatible);
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
        triggerAutoModelRefresh();

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
        List<String> suggestions = resolveModelSuggestions(provider);
        modelNameComboBox.setItems(FXCollections.observableArrayList(suggestions));
        if (currentText != null && !currentText.isBlank()) {
            modelNameComboBox.getEditor().setText(currentText);
        } else if (!suggestions.isEmpty()) {
            modelNameComboBox.getEditor().setText(suggestions.get(0));
        }
    }

    private List<String> resolveModelSuggestions(AiProvider provider) {
        String cacheKey = buildDynamicModelCacheKey(provider, resolveBaseUrlInput(), resolveTokenInput());
        List<String> dynamic = dynamicModelSuggestionsCache.get(cacheKey);
        if (dynamic != null && !dynamic.isEmpty()) {
            return dynamic;
        }
        return MODEL_NAME_SUGGESTIONS.getOrDefault(provider, List.of());
    }

    private String defaultBaseUrlFor(AiProvider provider) {
        if (provider == null) {
            return "";
        }
        return DEFAULT_BASE_URLS.getOrDefault(provider, "");
    }

    private void applyDefaultBaseUrlIfNeeded(AiProvider oldProvider, AiProvider newProvider) {
        if (baseUrlField == null || newProvider == null) {
            return;
        }
        String current = baseUrlField.getText();
        String oldDefault = normalizeBaseUrl(defaultBaseUrlFor(oldProvider));
        String newDefault = normalizeBaseUrl(defaultBaseUrlFor(newProvider));
        String currentNormalized = normalizeBaseUrl(current);
        if (currentNormalized.isBlank() || currentNormalized.equals(oldDefault)) {
            baseUrlField.setText(newDefault);
        }
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String resolveBaseUrlInput() {
        String userInput = normalizeBaseUrl(baseUrlField == null ? "" : baseUrlField.getText());
        if (!userInput.isBlank()) {
            return userInput;
        }
        AiProvider selected = providerComboBox == null ? null : providerComboBox.getSelectionModel().getSelectedItem();
        return normalizeBaseUrl(defaultBaseUrlFor(selected));
    }

    private String resolveTokenInput() {
        return tokenField == null || tokenField.getText() == null ? "" : tokenField.getText().trim();
    }

    private String buildDynamicModelCacheKey(AiProvider provider, String baseUrl, String token) {
        String providerPart = provider == null ? "" : provider.name();
        String tokenPart = token == null ? "" : token;
        return providerPart + "|" + baseUrl + "|" + tokenPart;
    }

    private void triggerAutoModelRefresh() {
        AiProvider provider = providerComboBox.getSelectionModel().getSelectedItem();
        if (provider == null || !isOpenAiCompatibleProvider(provider)) {
            return;
        }
        String baseUrl = resolveBaseUrlInput();
        if (baseUrl.isBlank()) {
            return;
        }
        String token = resolveTokenInput();
        String key = buildDynamicModelCacheKey(provider, baseUrl, token);
        List<String> cached = dynamicModelSuggestionsCache.get(key);
        if (cached != null && !cached.isEmpty()) {
            modelNameComboBox.setItems(FXCollections.observableArrayList(cached));
            return;
        }
        refreshModelNames();
    }

    private boolean isOpenAiCompatibleProvider(AiProvider provider) {
        return provider != AiProvider.QIANFAN;
    }

    @FXML
    private void refreshModelNames() {
        AiProvider provider = providerComboBox.getSelectionModel().getSelectedItem();
        if (provider == null) {
            statusLabel.setText("请选择模型平台");
            statusLabel.setStyle("-fx-text-fill: #FF9800;");
            return;
        }
        if (!isOpenAiCompatibleProvider(provider)) {
            statusLabel.setText("当前平台不支持 /v1/models 自动拉取");
            statusLabel.setStyle("-fx-text-fill: #FF9800;");
            return;
        }
        String baseUrlInput = resolveBaseUrlInput();
        if (baseUrlInput.isBlank()) {
            statusLabel.setText("请先填写 Base URL");
            statusLabel.setStyle("-fx-text-fill: #FF9800;");
            return;
        }
        String token = resolveTokenInput();
        long seq = modelFetchSequence.incrementAndGet();
        if (refreshModelListButton != null) {
            refreshModelListButton.setDisable(true);
            refreshModelListButton.setText("拉取中...");
        }
        statusLabel.setText("正在拉取模型列表...");
        statusLabel.setStyle("-fx-text-fill: #2196F3;");
        modelCatalogExecutor.submit(() -> {
            try {
                String providerName = provider == null ? AiProvider.OPENAI_COMPATIBLE.name() : provider.name();
                List<String> fetched = openAiModelCatalogService.fetchModelNames(providerName, baseUrlInput, token);
                String key = buildDynamicModelCacheKey(provider, baseUrlInput, token);
                dynamicModelSuggestionsCache.put(key, fetched);
                Platform.runLater(() -> {
                    if (seq != modelFetchSequence.get()) {
                        return;
                    }
                    String currentText = modelNameComboBox.getEditor().getText();
                    modelNameComboBox.setItems(FXCollections.observableArrayList(fetched));
                    if ((currentText == null || currentText.isBlank()) && !fetched.isEmpty()) {
                        modelNameComboBox.getEditor().setText(fetched.get(0));
                    }
                    statusLabel.setText("已拉取模型 " + fetched.size() + " 个");
                    statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                });
            } catch (Exception e) {
                logger.warn("拉取模型列表失败: {}", e.getMessage());
                Platform.runLater(() -> {
                    if (seq != modelFetchSequence.get()) {
                        return;
                    }
                    statusLabel.setText("拉取模型失败: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #F44336;");
                });
            } finally {
                Platform.runLater(() -> {
                    if (seq != modelFetchSequence.get()) {
                        return;
                    }
                    if (refreshModelListButton != null) {
                        refreshModelListButton.setDisable(false);
                        refreshModelListButton.setText("拉取模型");
                    }
                });
            }
        });
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
            prefs.putString(KEY_AI_BASE_URL, resolveBaseUrlInput());

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

    private void bindWindowClose() {
        if (statusLabel == null || statusLabel.getScene() == null || statusLabel.getScene().getWindow() == null) {
            return;
        }
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.setOnHidden(event -> modelCatalogExecutor.shutdownNow());
    }

    private void closeWindow() {
        modelCatalogExecutor.shutdownNow();
        if (statusLabel != null && statusLabel.getScene() != null) {
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.close();
        }
    }
}
