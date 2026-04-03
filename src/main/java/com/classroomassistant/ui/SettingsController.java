package com.classroomassistant.ui;

import com.classroomassistant.AppContext;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.ai.OpenAiModelCatalogService;
import com.classroomassistant.session.ClassSessionManager;
import com.classroomassistant.storage.ModelDescriptor;
import com.classroomassistant.storage.ModelDownloadManager;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.utils.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设置页面控制器 (Settings Controller)
 *
 * <p>
 * 负责"系统设置"界面的交互逻辑，与安卓端 SettingsScreen 保持功能对齐。
 * 主要功能包括：
 * <ul>
 * <li>从 {@link PreferencesManager} 加载当前用户配置并展示在 UI 控件上。</li>
 * <li>收集用户在界面上的修改。</li>
 * <li>执行基本的表单校验。</li>
 * <li>将新配置持久化，并通知 {@link ClassSessionManager} 即时应用。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final String ASR_OPTION_ID = "ASR_MODEL";
    private static final String VAD_OPTION_ID = "VAD_MODEL";

    /** 模型平台按主流程度排序（30种） */
    private static final List<LLMConfig.ModelType> PROVIDER_ORDER = List.of(
            LLMConfig.ModelType.OPENAI,
            LLMConfig.ModelType.OPENAI_COMPATIBLE,
            LLMConfig.ModelType.ANTHROPIC,
            LLMConfig.ModelType.GEMINI,
            LLMConfig.ModelType.DEEPSEEK,
            LLMConfig.ModelType.QIANFAN,
            LLMConfig.ModelType.DASHSCOPE,
            LLMConfig.ModelType.HUNYUAN,
            LLMConfig.ModelType.ZHIPU,
            LLMConfig.ModelType.KIMI,
            LLMConfig.ModelType.GROQ,
            LLMConfig.ModelType.MISTRAL,
            LLMConfig.ModelType.COHERE,
            LLMConfig.ModelType.OPENROUTER,
            LLMConfig.ModelType.AZURE_OPENAI,
            LLMConfig.ModelType.SILICONFLOW,
            LLMConfig.ModelType.MINIMAX,
            LLMConfig.ModelType.BAICHUAN,
            LLMConfig.ModelType.YI,
            LLMConfig.ModelType.STEPFUN,
            LLMConfig.ModelType.XAI,
            LLMConfig.ModelType.FIREWORKS,
            LLMConfig.ModelType.TOGETHER_AI,
            LLMConfig.ModelType.PERPLEXITY,
            LLMConfig.ModelType.NOVITA,
            LLMConfig.ModelType.REPLICATE,
            LLMConfig.ModelType.CEREBRAS,
            LLMConfig.ModelType.SAMBANOVA,
            LLMConfig.ModelType.OLLAMA,
            LLMConfig.ModelType.LMSTUDIO);

    /** 各 AI 平台的推荐模型名称映射 */
    private static final Map<LLMConfig.ModelType, List<String>> MODEL_NAME_SUGGESTIONS = Map.ofEntries(
            Map.entry(LLMConfig.ModelType.OPENAI, List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini")),
            Map.entry(LLMConfig.ModelType.OPENAI_COMPATIBLE,
                    List.of("gpt-4o-mini", "deepseek-chat", "qwen-plus", "claude-3-5-sonnet-20241022")),
            Map.entry(LLMConfig.ModelType.ANTHROPIC,
                    List.of("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")),
            Map.entry(LLMConfig.ModelType.GEMINI,
                    List.of("gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash")),
            Map.entry(LLMConfig.ModelType.QIANFAN, List.of("ernie-4.0-8k", "ernie-3.5-8k")),
            Map.entry(LLMConfig.ModelType.DEEPSEEK, List.of("deepseek-chat", "deepseek-reasoner")),
            Map.entry(LLMConfig.ModelType.KIMI, List.of("moonshot-v1-8k", "moonshot-v1-32k")),
            Map.entry(LLMConfig.ModelType.DASHSCOPE, List.of("qwen-plus", "qwen-turbo", "qwen-max")),
            Map.entry(LLMConfig.ModelType.HUNYUAN, List.of("hunyuan-lite", "hunyuan-standard", "hunyuan-pro")),
            Map.entry(LLMConfig.ModelType.ZHIPU, List.of("glm-4-flash", "glm-4-plus", "glm-4-air")),
            Map.entry(LLMConfig.ModelType.SILICONFLOW,
                    List.of("Qwen/Qwen2.5-7B-Instruct", "deepseek-ai/DeepSeek-V3", "Llama-3.1-8B-Instruct")),
            Map.entry(LLMConfig.ModelType.MINIMAX, List.of("abab6.5s-chat", "abab6.5t-chat", "MiniMax-Text-01")),
            Map.entry(LLMConfig.ModelType.MISTRAL,
                    List.of("mistral-small-latest", "mistral-large-latest", "open-mistral-nemo")),
            Map.entry(LLMConfig.ModelType.GROQ,
                    List.of("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")),
            Map.entry(LLMConfig.ModelType.COHERE, List.of("command-r-plus", "command-r", "command-light")),
            Map.entry(LLMConfig.ModelType.OPENROUTER,
                    List.of("openai/gpt-4o-mini", "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001")),
            Map.entry(LLMConfig.ModelType.AZURE_OPENAI, List.of("gpt-4o", "gpt-4o-mini", "o3-mini")),
            Map.entry(LLMConfig.ModelType.BAICHUAN, List.of("Baichuan4", "Baichuan3-Turbo", "Baichuan2-Turbo")),
            Map.entry(LLMConfig.ModelType.YI, List.of("yi-large", "yi-medium", "yi-lightning")),
            Map.entry(LLMConfig.ModelType.STEPFUN, List.of("step-2", "step-1v-8k", "step-1-8k")),
            Map.entry(LLMConfig.ModelType.XAI, List.of("grok-2-1212", "grok-beta", "grok-vision-beta")),
            Map.entry(LLMConfig.ModelType.FIREWORKS,
                    List.of("accounts/fireworks/models/llama-v3p1-70b-instruct", "accounts/fireworks/models/qwen2p5-72b-instruct")),
            Map.entry(LLMConfig.ModelType.TOGETHER_AI,
                    List.of("meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo", "Qwen/Qwen2.5-72B-Instruct-Turbo")),
            Map.entry(LLMConfig.ModelType.PERPLEXITY,
                    List.of("sonar", "sonar-pro", "llama-3.1-sonar-large-128k-online")),
            Map.entry(LLMConfig.ModelType.NOVITA,
                    List.of("meta-llama/llama-3.1-70b-instruct", "deepseek/deepseek-r1", "qwen/qwen2.5-72b-instruct")),
            Map.entry(LLMConfig.ModelType.REPLICATE,
                    List.of("meta/meta-llama-3-70b-instruct", "mistralai/mistral-7b-instruct", "anthropic/claude-3-opus")),
            Map.entry(LLMConfig.ModelType.CEREBRAS,
                    List.of("llama3.1-8b", "llama3.1-70b", "qwen-2.5-72b")),
            Map.entry(LLMConfig.ModelType.SAMBANOVA,
                    List.of("Meta-Llama-3.1-405B-Instruct", "Meta-Llama-3.1-70B-Instruct", "Qwen2.5-72B-Instruct")),
            Map.entry(LLMConfig.ModelType.OLLAMA, List.of("qwen2.5:7b", "llama3.1:8b", "deepseek-r1:8b")),
            Map.entry(LLMConfig.ModelType.LMSTUDIO, List.of("local-model", "qwen2.5-7b-instruct", "llama-3.1-8b")));

    /** 各平台默认 Base URL（OpenAI 兼容类型） */
    private static final Map<LLMConfig.ModelType, String> DEFAULT_BASE_URLS = Map.ofEntries(
            Map.entry(LLMConfig.ModelType.OPENAI, "https://api.openai.com"),
            Map.entry(LLMConfig.ModelType.OPENAI_COMPATIBLE, ""),
            Map.entry(LLMConfig.ModelType.ANTHROPIC, "https://api.anthropic.com/v1"),
            Map.entry(LLMConfig.ModelType.GEMINI, "https://generativelanguage.googleapis.com/v1beta/openai"),
            Map.entry(LLMConfig.ModelType.QIANFAN, ""),
            Map.entry(LLMConfig.ModelType.DEEPSEEK, "https://api.deepseek.com"),
            Map.entry(LLMConfig.ModelType.KIMI, "https://api.moonshot.cn/v1"),
            Map.entry(LLMConfig.ModelType.DASHSCOPE, "https://dashscope.aliyuncs.com/compatible-mode/v1"),
            Map.entry(LLMConfig.ModelType.HUNYUAN, "https://api.hunyuan.cloud.tencent.com/v1"),
            Map.entry(LLMConfig.ModelType.ZHIPU, "https://open.bigmodel.cn/api/paas/v4"),
            Map.entry(LLMConfig.ModelType.SILICONFLOW, "https://api.siliconflow.cn/v1"),
            Map.entry(LLMConfig.ModelType.MINIMAX, "https://api.minimax.chat/v1"),
            Map.entry(LLMConfig.ModelType.MISTRAL, "https://api.mistral.ai/v1"),
            Map.entry(LLMConfig.ModelType.GROQ, "https://api.groq.com/openai/v1"),
            Map.entry(LLMConfig.ModelType.COHERE, "https://api.cohere.ai/compatibility/v1"),
            Map.entry(LLMConfig.ModelType.OPENROUTER, "https://openrouter.ai/api/v1"),
            Map.entry(LLMConfig.ModelType.AZURE_OPENAI, ""),
            Map.entry(LLMConfig.ModelType.BAICHUAN, "https://api.baichuan-ai.com/v1"),
            Map.entry(LLMConfig.ModelType.YI, "https://api.lingyiwanwu.com/v1"),
            Map.entry(LLMConfig.ModelType.STEPFUN, "https://api.stepfun.com/v1"),
            Map.entry(LLMConfig.ModelType.XAI, "https://api.x.ai/v1"),
            Map.entry(LLMConfig.ModelType.FIREWORKS, "https://api.fireworks.ai/inference/v1"),
            Map.entry(LLMConfig.ModelType.TOGETHER_AI, "https://api.together.xyz/v1"),
            Map.entry(LLMConfig.ModelType.PERPLEXITY, "https://api.perplexity.ai"),
            Map.entry(LLMConfig.ModelType.NOVITA, "https://api.novita.ai/v3/openai"),
            Map.entry(LLMConfig.ModelType.REPLICATE, "https://api.replicate.com/v1"),
            Map.entry(LLMConfig.ModelType.CEREBRAS, "https://api.cerebras.ai/v1"),
            Map.entry(LLMConfig.ModelType.SAMBANOVA, "https://api.sambanova.ai/v1"),
            Map.entry(LLMConfig.ModelType.OLLAMA, "http://127.0.0.1:11434/v1"),
            Map.entry(LLMConfig.ModelType.LMSTUDIO, "http://127.0.0.1:1234/v1"));

    /** AI 平台展示名 */
    private static final Map<LLMConfig.ModelType, String> PROVIDER_DISPLAY_NAMES = Map.ofEntries(
            Map.entry(LLMConfig.ModelType.OPENAI, "OpenAI 官方"),
            Map.entry(LLMConfig.ModelType.OPENAI_COMPATIBLE, "OpenAI 兼容 / 中转站"),
            Map.entry(LLMConfig.ModelType.ANTHROPIC, "Anthropic"),
            Map.entry(LLMConfig.ModelType.GEMINI, "Google Gemini"),
            Map.entry(LLMConfig.ModelType.QIANFAN, "百度千帆"),
            Map.entry(LLMConfig.ModelType.DEEPSEEK, "DeepSeek"),
            Map.entry(LLMConfig.ModelType.KIMI, "Kimi (Moonshot)"),
            Map.entry(LLMConfig.ModelType.DASHSCOPE, "阿里百炼 (DashScope)"),
            Map.entry(LLMConfig.ModelType.HUNYUAN, "腾讯混元"),
            Map.entry(LLMConfig.ModelType.ZHIPU, "智谱 AI"),
            Map.entry(LLMConfig.ModelType.SILICONFLOW, "硅基流动"),
            Map.entry(LLMConfig.ModelType.MINIMAX, "MiniMax"),
            Map.entry(LLMConfig.ModelType.MISTRAL, "Mistral"),
            Map.entry(LLMConfig.ModelType.GROQ, "Groq"),
            Map.entry(LLMConfig.ModelType.COHERE, "Cohere"),
            Map.entry(LLMConfig.ModelType.OPENROUTER, "OpenRouter"),
            Map.entry(LLMConfig.ModelType.AZURE_OPENAI, "Azure OpenAI"),
            Map.entry(LLMConfig.ModelType.BAICHUAN, "百川智能"),
            Map.entry(LLMConfig.ModelType.YI, "零一万物 Yi"),
            Map.entry(LLMConfig.ModelType.STEPFUN, "阶跃星辰 StepFun"),
            Map.entry(LLMConfig.ModelType.XAI, "xAI"),
            Map.entry(LLMConfig.ModelType.FIREWORKS, "Fireworks AI"),
            Map.entry(LLMConfig.ModelType.TOGETHER_AI, "Together AI"),
            Map.entry(LLMConfig.ModelType.PERPLEXITY, "Perplexity"),
            Map.entry(LLMConfig.ModelType.NOVITA, "Novita"),
            Map.entry(LLMConfig.ModelType.REPLICATE, "Replicate"),
            Map.entry(LLMConfig.ModelType.CEREBRAS, "Cerebras"),
            Map.entry(LLMConfig.ModelType.SAMBANOVA, "SambaNova"),
            Map.entry(LLMConfig.ModelType.OLLAMA, "Ollama（本地）"),
            Map.entry(LLMConfig.ModelType.LMSTUDIO, "LM Studio（本地）"));

    private final PreferencesManager preferencesManager;
    private final ClassSessionManager classSessionManager;
    private final ModelDownloadManager modelDownloadManager;
    private final OpenAiModelCatalogService openAiModelCatalogService = new OpenAiModelCatalogService();
    private final ExecutorService modelCatalogExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "openai-model-catalog-fetcher");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, List<String>> dynamicModelSuggestionsCache = new ConcurrentHashMap<>();
    private final AtomicLong modelFetchSequence = new AtomicLong(0);
    private final Map<String, ModelDownloadManager.KwsModelOption> optionMap = new HashMap<>();
    private final Map<String, ProgressBar> progressBars = new HashMap<>();
    private final Map<String, Label> statusLabels = new HashMap<>();
    private final Map<String, CheckBox> optionCheckBoxes = new HashMap<>();
    private final Set<String> selectedModelIds = new LinkedHashSet<>();
    private String currentModelId = "";
    private boolean asrModelSelected = true;
    private boolean vadModelSelected = true;

    // ToggleGroups for RadioButton sets
    private final ToggleGroup wakeAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup quietAlertToggleGroup = new ToggleGroup();
    private final ToggleGroup logModeToggleGroup = new ToggleGroup();

    // ── 唤醒词 ──────────────────────────────────────────────────────────
    @FXML
    private TextField keywordsField;
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
    private ComboBox<LLMConfig.ModelType> providerComboBox;
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

    // ── 模型管理 ────────────────────────────────────────────────────────
    @FXML
    private ComboBox<ModelDownloadManager.KwsModelOption> currentModelComboBox;
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

    /**
     * 构造设置页面控制器
     *
     * @param appContext 应用全局上下文
     */
    public SettingsController(AppContext appContext) {
        this.preferencesManager = appContext.getPreferencesManager();
        this.classSessionManager = appContext.getClassSessionManager();
        this.modelDownloadManager = new ModelDownloadManager(appContext.getModelRepository(),
                appContext.getConfigManager());
    }

    // =========================================================================
    // 初始化
    // =========================================================================

    /**
     * JavaFX 初始化回调
     * <p>
     * 配置控件取值范围、初始化 ComboBox 与 ToggleGroup，并加载现有配置填充表单。
     */
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
            public String toString(LLMConfig.ModelType object) {
                if (object == null) {
                    return "";
                }
                return PROVIDER_DISPLAY_NAMES.getOrDefault(object, object.name());
            }

            @Override
            public LLMConfig.ModelType fromString(String string) {
                return null;
            }
        });
        providerComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(LLMConfig.ModelType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(PROVIDER_DISPLAY_NAMES.getOrDefault(item, item.name()));
                }
            }
        });
        providerComboBox.getSelectionModel().select(LLMConfig.ModelType.QIANFAN);
        providerComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // 千帆平台需要 Secret Key
            boolean isQianfan = newVal == LLMConfig.ModelType.QIANFAN;
            secretKeyBox.setVisible(isQianfan);
            secretKeyBox.setManaged(isQianfan);
            applyDefaultBaseUrlIfNeeded(oldVal, newVal);
            if (refreshModelListButton != null) {
                boolean compatible = isOpenAiCompatibleProvider(newVal);
                refreshModelListButton.setVisible(compatible);
                refreshModelListButton.setManaged(compatible);
            }
            updateModelNameSuggestions(newVal);
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

        // ── 加载已有配置 ──
        loadPreferences();

        // ── 初始化模型管理区 ──
        initModelSection();
        refreshModelStatus();
        Platform.runLater(this::bindWindowClose);
    }

    /**
     * 从 PreferencesManager 加载配置并填充到 UI 控件
     */
    private void loadPreferences() {
        UserPreferences prefs = preferencesManager.load();

        // 唤醒词
        keywordsField.setText(prefs.getKeywords());
        kwsThresholdSlider.setValue(prefs.getKwsThreshold());
        kwsThresholdLabel.setText(String.format("%.2f", prefs.getKwsThreshold()));

        // 唤醒提示
        if ("SOUND".equals(prefs.getWakeAlertMode())) {
            wakeAlertSound.setSelected(true);
        } else {
            wakeAlertNotificationOnly.setSelected(true);
        }

        // 安静检测
        vadEnabledCheckBox.setSelected(prefs.isVadEnabled());
        quietThresholdSpinner.getValueFactory().setValue(prefs.getVadQuietThresholdSeconds());
        if ("SOUND".equals(prefs.getQuietAlertMode())) {
            quietAlertSound.setSelected(true);
        } else {
            quietAlertNotificationOnly.setSelected(true);
        }
        quietAutoLookbackCheckBox.setSelected(prefs.isQuietAutoLookbackEnabled());
        quietAutoLookbackExtraSpinner.getValueFactory().setValue(prefs.getQuietAutoLookbackExtraSeconds());

        // 语音回溯
        lookbackSecondsSpinner.getValueFactory().setValue(prefs.getAudioLookbackSeconds());

        // 录音保存
        recordingSaveCheckBox.setSelected(prefs.isRecordingSaveEnabled());
        recordingRetentionSpinner.getValueFactory().setValue(prefs.getRecordingRetentionDays());

        // AI 问答
        providerComboBox.getSelectionModel().select(prefs.getAiModelType());
        updateModelNameSuggestions(prefs.getAiModelType());
        String savedModelName = prefs.getAiModelName();
        if (savedModelName != null && !savedModelName.isBlank()) {
            modelNameComboBox.getEditor().setText(savedModelName);
        }
        String savedBaseUrl = prefs.getAiBaseUrl();
        if (baseUrlField != null) {
            if (savedBaseUrl != null && !savedBaseUrl.isBlank()) {
                baseUrlField.setText(savedBaseUrl);
            } else {
                baseUrlField.setText(defaultBaseUrlFor(prefs.getAiModelType()));
            }
        }
        if (refreshModelListButton != null) {
            refreshModelListButton.setVisible(isOpenAiCompatibleProvider(prefs.getAiModelType()));
            refreshModelListButton.setManaged(isOpenAiCompatibleProvider(prefs.getAiModelType()));
        }
        // 加载解密后的 Token 和密钥
        String token = preferencesManager.loadAiTokenPlainText();
        if (token != null && !token.isBlank()) {
            tokenField.setText(token);
        }
        boolean isQianfan = prefs.getAiModelType() == LLMConfig.ModelType.QIANFAN;
        secretKeyBox.setVisible(isQianfan);
        secretKeyBox.setManaged(isQianfan);
        String secretKey = preferencesManager.loadAiSecretKey();
        if (secretKey != null && !secretKey.isBlank()) {
            secretKeyField.setText(secretKey);
        }
        triggerAutoModelRefresh();

        // 语音识别路线
        localAsrEnabledCheckBox.setSelected(prefs.isLocalAsrEnabled());
        cloudWhisperEnabledCheckBox.setSelected(prefs.isCloudWhisperEnabled());
        speechApiKeyBox.setVisible(prefs.isCloudWhisperEnabled());
        speechApiKeyBox.setManaged(prefs.isCloudWhisperEnabled());
        String speechKey = preferencesManager.loadSpeechApiKey();
        if (speechKey != null && !speechKey.isBlank()) {
            speechApiKeyField.setText(speechKey);
        }

        // 开发者选项
        if ("FULL".equals(prefs.getLogMode())) {
            logModeFull.setSelected(true);
            logSubCategoryBox.setVisible(true);
            logSubCategoryBox.setManaged(true);
        } else {
            logModeSimple.setSelected(true);
            logSubCategoryBox.setVisible(false);
            logSubCategoryBox.setManaged(false);
        }
        showDiagnosticLogsCheckBox.setSelected(prefs.isShowDiagnosticLogs());
        showAudioDeviceLogsCheckBox.setSelected(prefs.isShowAudioDeviceLogs());
        showGainActivityLogsCheckBox.setSelected(prefs.isShowGainActivityLogs());
        showTtsSelfTestLogsCheckBox.setSelected(prefs.isShowTtsSelfTestLogs());
        showHeartbeatLogsCheckBox.setSelected(prefs.isShowHeartbeatLogs());
        ttsSelfTestEnabledCheckBox.setSelected(prefs.isTtsSelfTestEnabled());

        // 后台保活
        backgroundKeepAliveCheckBox.setSelected(prefs.isBackgroundKeepAliveEnabled());

        // KWS 模型选择
        selectedModelIds.clear();
        selectedModelIds.addAll(prefs.getSelectedKwsModelIds());
        currentModelId = prefs.getCurrentKwsModelId();
        asrModelSelected = prefs.isAsrModelSelected();
        vadModelSelected = prefs.isVadModelSelected();
    }

    /**
     * 根据选中的 AI 平台更新模型名称下拉建议（与安卓端一致）
     */
    private void updateModelNameSuggestions(LLMConfig.ModelType modelType) {
        if (modelNameComboBox == null || modelType == null) {
            return;
        }
        String currentText = modelNameComboBox.getEditor().getText();
        List<String> suggestions = resolveModelSuggestions(modelType);
        modelNameComboBox.setItems(FXCollections.observableArrayList(suggestions));
        // 保留用户已输入的文本；如果为空则填入第一个推荐值
        if (currentText != null && !currentText.isBlank()) {
            modelNameComboBox.getEditor().setText(currentText);
        } else if (!suggestions.isEmpty()) {
            modelNameComboBox.getEditor().setText(suggestions.get(0));
        }
    }

    private List<String> resolveModelSuggestions(LLMConfig.ModelType modelType) {
        String cacheKey = buildDynamicModelCacheKey(modelType, resolveBaseUrlInput(), resolveTokenInput());
        List<String> dynamic = dynamicModelSuggestionsCache.get(cacheKey);
        if (dynamic != null && !dynamic.isEmpty()) {
            return dynamic;
        }
        return MODEL_NAME_SUGGESTIONS.getOrDefault(modelType, List.of());
    }

    private String defaultBaseUrlFor(LLMConfig.ModelType modelType) {
        if (modelType == null) {
            return "";
        }
        return DEFAULT_BASE_URLS.getOrDefault(modelType, "");
    }

    private void applyDefaultBaseUrlIfNeeded(LLMConfig.ModelType oldType, LLMConfig.ModelType newType) {
        if (baseUrlField == null || newType == null) {
            return;
        }
        String current = baseUrlField.getText();
        String oldDefault = normalizeBaseUrl(defaultBaseUrlFor(oldType));
        String newDefault = normalizeBaseUrl(defaultBaseUrlFor(newType));
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
        LLMConfig.ModelType selected = providerComboBox == null ? null : providerComboBox.getSelectionModel().getSelectedItem();
        return normalizeBaseUrl(defaultBaseUrlFor(selected));
    }

    private String resolveTokenInput() {
        return tokenField == null || tokenField.getText() == null ? "" : tokenField.getText().trim();
    }

    private String buildDynamicModelCacheKey(LLMConfig.ModelType type, String baseUrl, String token) {
        String provider = type == null ? "" : type.name();
        String tokenPart = token == null ? "" : token;
        return provider + "|" + baseUrl + "|" + tokenPart;
    }

    private String buildDynamicModelCacheKey(LLMConfig.ModelType type) {
        return buildDynamicModelCacheKey(type, resolveBaseUrlInput(), resolveTokenInput());
    }

    private void triggerAutoModelRefresh() {
        LLMConfig.ModelType provider = providerComboBox.getSelectionModel().getSelectedItem();
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

    private boolean isOpenAiCompatibleProvider(LLMConfig.ModelType type) {
        return type != LLMConfig.ModelType.QIANFAN;
    }

    @FXML
    private void refreshModelNames() {
        LLMConfig.ModelType provider = providerComboBox.getSelectionModel().getSelectedItem();
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
                List<String> fetched = openAiModelCatalogService.fetchModelNames(provider, baseUrlInput, token);
                String key = buildDynamicModelCacheKey(provider, baseUrlInput, token);
                dynamicModelSuggestionsCache.put(key, fetched);
                Platform.runLater(() -> {
                    if (seq != modelFetchSequence.get()) {
                        return;
                    }
                    modelNameComboBox.setItems(FXCollections.observableArrayList(fetched));
                    if (!fetched.isEmpty()) {
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

    private void invalidateProviderModelCache(LLMConfig.ModelType provider) {
        if (provider == null) {
            return;
        }
        String prefix = provider.name() + "|";
        dynamicModelSuggestionsCache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    // =========================================================================
    // 保存 / 取消
    // =========================================================================

    /**
     * "保存"按钮点击事件
     * <p>
     * 读取表单数据，规整关键词，保存到持久化存储，并通知业务层应用更改。
     */
    @FXML
    private void save() {
        try {
            String keywords = Validator.normalizeKeywords(keywordsField.getText());
            float kwsThreshold = (float) kwsThresholdSlider.getValue();
            int quietThreshold = quietThresholdSpinner.getValue();
            int lookbackSeconds = lookbackSecondsSpinner.getValue();
            int retentionDays = recordingRetentionSpinner.getValue();
            LLMConfig.ModelType modelType = providerComboBox.getSelectionModel().getSelectedItem();
            String modelName = modelNameComboBox.getEditor().getText();
            modelName = modelName == null ? "" : modelName.trim();
            String baseUrl = resolveBaseUrlInput();
            String token = tokenField.getText() == null ? "" : tokenField.getText().trim();
            String secretKey = secretKeyField.getText() == null ? "" : secretKeyField.getText().trim();
            String speechApiKey = speechApiKeyField.getText() == null ? "" : speechApiKeyField.getText().trim();

            // 唤醒提示
            String wakeAlertMode = wakeAlertSound.isSelected() ? "SOUND" : "NOTIFICATION_ONLY";

            // 安静检测提醒
            String quietAlertMode = quietAlertSound.isSelected() ? "SOUND" : "NOTIFICATION_ONLY";
            boolean quietAutoLookbackEnabled = quietAutoLookbackCheckBox.isSelected();
            int quietAutoLookbackExtraSeconds = quietAutoLookbackExtraSpinner.getValue();

            // 语音识别路线
            boolean localAsrEnabled = localAsrEnabledCheckBox.isSelected();
            boolean cloudWhisperEnabled = cloudWhisperEnabledCheckBox.isSelected();

            // 开发者选项
            String logMode = logModeFull.isSelected() ? "FULL" : "SIMPLE";
            boolean showDiagnosticLogs = showDiagnosticLogsCheckBox.isSelected();
            boolean showAudioDeviceLogs = showAudioDeviceLogsCheckBox.isSelected();
            boolean showGainActivityLogs = showGainActivityLogsCheckBox.isSelected();
            boolean showTtsSelfTestLogs = showTtsSelfTestLogsCheckBox.isSelected();
            boolean showHeartbeatLogs = showHeartbeatLogsCheckBox.isSelected();
            boolean ttsSelfTestEnabled = ttsSelfTestEnabledCheckBox.isSelected();

            // 后台保活
            boolean backgroundKeepAliveEnabled = backgroundKeepAliveCheckBox.isSelected();

            // KWS 模型
            String resolvedCurrentModelId = currentModelId == null ? "" : currentModelId.trim();
            if (resolvedCurrentModelId.isEmpty()) {
                resolvedCurrentModelId = modelDownloadManager.getDefaultKwsModelId();
            }
            Set<String> resolvedSelectedModelIds = new LinkedHashSet<>(selectedModelIds);
            if (!resolvedCurrentModelId.isBlank()) {
                resolvedSelectedModelIds.add(resolvedCurrentModelId);
            }
            if (resolvedSelectedModelIds.isEmpty()) {
                resolvedSelectedModelIds.add(modelDownloadManager.getDefaultKwsModelId());
            }

            if (modelType != null) {
                String dynamicKey = buildDynamicModelCacheKey(modelType);
                if (dynamicModelSuggestionsCache.containsKey(dynamicKey)) {
                    invalidateProviderModelCache(modelType);
                }
            }

            UserPreferences updated = UserPreferences.builder()
                    .keywords(keywords)
                    .kwsThreshold(kwsThreshold)
                    .vadEnabled(vadEnabledCheckBox.isSelected())
                    .vadQuietThresholdSeconds(quietThreshold)
                    .quietAlertMode(quietAlertMode)
                    .quietAutoLookbackEnabled(quietAutoLookbackEnabled)
                    .quietAutoLookbackExtraSeconds(quietAutoLookbackExtraSeconds)
                    .audioLookbackSeconds(lookbackSeconds)
                    .recordingSaveEnabled(recordingSaveCheckBox.isSelected())
                    .recordingRetentionDays(retentionDays)
                    .aiModelType(modelType)
                    .aiModelName(modelName)
                    .aiBaseUrl(baseUrl)
                    .aiTokenPlainText(token)
                    .aiSecretKey(secretKey)
                    .speechApiKey(speechApiKey)
                    .localAsrEnabled(localAsrEnabled)
                    .cloudWhisperEnabled(cloudWhisperEnabled)
                    .selectedKwsModelIds(resolvedSelectedModelIds)
                    .currentKwsModelId(resolvedCurrentModelId)
                    .asrModelSelected(asrModelSelected)
                    .vadModelSelected(vadModelSelected)
                    .wakeAlertMode(wakeAlertMode)
                    .logMode(logMode)
                    .showDiagnosticLogs(showDiagnosticLogs)
                    .showAudioDeviceLogs(showAudioDeviceLogs)
                    .showGainActivityLogs(showGainActivityLogs)
                    .showTtsSelfTestLogs(showTtsSelfTestLogs)
                    .showHeartbeatLogs(showHeartbeatLogs)
                    .ttsSelfTestEnabled(ttsSelfTestEnabled)
                    .backgroundKeepAliveEnabled(backgroundKeepAliveEnabled)
                    .build();

            preferencesManager.save(updated);
            classSessionManager.applySettings(updated);
            statusLabel.setText("✓ 保存成功");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");

            // 延迟关闭窗口，让用户看到保存成功提示
            new Thread(() -> {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(this::closeWindow);
            }).start();
        } catch (Exception e) {
            logger.error("保存设置失败", e);
            statusLabel.setText("✗ 保存失败: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    /**
     * "取消"按钮点击事件
     * <p>
     * 放弃所有未保存的更改并关闭窗口。
     */
    @FXML
    private void cancel() {
        closeWindow();
    }

    // =========================================================================
    // 模型下载管理（与原有逻辑一致）
    // =========================================================================

    @FXML
    private void downloadSelectedModels() {
        Set<String> targetIds = new LinkedHashSet<>(selectedModelIds);
        if (currentModelId != null && !currentModelId.isBlank()) {
            targetIds.add(currentModelId);
        }
        if (targetIds.isEmpty()) {
            String fallbackId = modelDownloadManager.getDefaultKwsModelId();
            if (fallbackId != null && !fallbackId.isBlank()) {
                targetIds.add(fallbackId);
            }
        }
        List<ModelDescriptor> models = new ArrayList<>();
        Map<String, String> nameToId = new HashMap<>();
        for (String id : targetIds) {
            ModelDownloadManager.KwsModelOption option = optionMap.get(id);
            if (option == null) {
                continue;
            }
            ModelDescriptor descriptor = modelDownloadManager.buildKwsModelDescriptor(option);
            models.add(descriptor);
            nameToId.put(descriptor.name(), id);
        }
        if (asrModelSelected) {
            ModelDescriptor descriptor = modelDownloadManager.buildAsrModelDescriptor();
            models.add(descriptor);
            nameToId.put(descriptor.name(), ASR_OPTION_ID);
        }
        if (vadModelSelected) {
            ModelDescriptor descriptor = modelDownloadManager.buildVadModelDescriptor();
            models.add(descriptor);
            nameToId.put(descriptor.name(), VAD_OPTION_ID);
        }
        if (models.isEmpty()) {
            modelDownloadStatusLabel.setText("请选择至少一个模型");
            return;
        }
        modelDownloadStatusLabel.setText("开始下载所选模型...");
        modelDownloadManager.downloadAllAsync(models, new ModelDownloadManager.DetailedDownloadCallback() {
            @Override
            public void onProgress(String modelName, double progress, long downloadedBytes, long totalBytes) {
                String modelId = nameToId.get(modelName);
                ProgressBar bar = progressBars.get(modelId);
                if (bar != null) {
                    bar.setVisible(true);
                    bar.setProgress(progress);
                }
                Label status = statusLabels.get(modelId);
                if (status != null) {
                    status.setText(String.format("下载中 %.0f%%", progress * 100));
                }
            }

            @Override
            public void onModelComplete(String modelName) {
                String modelId = nameToId.get(modelName);
                ProgressBar bar = progressBars.get(modelId);
                if (bar != null) {
                    bar.setProgress(1.0);
                }
                Label status = statusLabels.get(modelId);
                if (status != null) {
                    status.setText("已完成");
                }
            }

            @Override
            public void onAllComplete() {
                refreshModelStatus();
                modelDownloadStatusLabel.setText("模型下载完成");
            }

            @Override
            public void onError(String modelName, String error) {
                String modelId = nameToId.get(modelName);
                Label status = statusLabels.get(modelId);
                if (status != null) {
                    status.setText("下载失败: " + error);
                }
                modelDownloadStatusLabel.setText("部分模型下载失败");
            }
        });
    }

    @FXML
    private void refreshModelStatus() {
        if (modelDownloadStatusLabel == null || optionMap.isEmpty()) {
            return;
        }
        int readyCount = 0;
        for (ModelDownloadManager.KwsModelOption option : optionMap.values()) {
            String modelId = option.getId();
            boolean ready = modelDownloadManager.isKwsModelReady(modelId);
            Label status = statusLabels.get(modelId);
            if (status != null) {
                status.setText(ready ? "已就绪" : "未就绪");
            }
            ProgressBar bar = progressBars.get(modelId);
            if (bar != null && ready) {
                bar.setVisible(false);
            }
            if (ready) {
                readyCount++;
            }
        }
        boolean asrReady = modelDownloadManager.isAsrModelReady();
        updateAuxStatus(ASR_OPTION_ID, asrReady);
        if (asrReady) {
            readyCount++;
        }
        boolean vadReady = modelDownloadManager.isVadModelReady();
        updateAuxStatus(VAD_OPTION_ID, vadReady);
        if (vadReady) {
            readyCount++;
        }
        String current = currentModelId == null ? "" : currentModelId.trim();
        boolean currentReady = current.isEmpty() || modelDownloadManager.isKwsModelReady(current);
        int totalCount = optionMap.size() + 2;
        String summary = "本地就绪 " + readyCount + "/" + totalCount;
        if (!currentReady) {
            summary = summary + "，当前模型未就绪";
        }
        modelDownloadStatusLabel.setText(summary);
    }

    // =========================================================================
    // 模型管理 UI 构建（原有逻辑保持不变）
    // =========================================================================

    private void initModelSection() {
        if (currentModelComboBox == null || modelOptionsBox == null) {
            return;
        }
        List<ModelDownloadManager.KwsModelOption> options = modelDownloadManager.getKwsModelOptions();
        optionMap.clear();
        progressBars.clear();
        statusLabels.clear();
        optionCheckBoxes.clear();
        modelOptionsBox.getChildren().clear();
        if (options == null || options.isEmpty()) {
            currentModelComboBox.setItems(FXCollections.observableArrayList());
            currentModelComboBox.getSelectionModel().clearSelection();
        } else {
            for (ModelDownloadManager.KwsModelOption option : options) {
                optionMap.put(option.getId(), option);
            }
            currentModelComboBox.setItems(FXCollections.observableArrayList(options));
        }
        currentModelComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModelDownloadManager.KwsModelOption object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public ModelDownloadManager.KwsModelOption fromString(String string) {
                return null;
            }
        });
        currentModelComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ModelDownloadManager.KwsModelOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getName());
                }
            }
        });
        if (!optionMap.isEmpty()) {
            String resolvedCurrentId = currentModelId;
            if (resolvedCurrentId == null || resolvedCurrentId.isBlank() || !optionMap.containsKey(resolvedCurrentId)) {
                resolvedCurrentId = modelDownloadManager.getDefaultKwsModelId();
            }
            currentModelId = resolvedCurrentId == null ? "" : resolvedCurrentId;
            ModelDownloadManager.KwsModelOption selectedOption = optionMap.get(currentModelId);
            if (selectedOption != null) {
                currentModelComboBox.getSelectionModel().select(selectedOption);
                selectedModelIds.add(currentModelId);
            } else {
                currentModelComboBox.getSelectionModel().clearSelection();
            }
            currentModelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                currentModelId = newValue.getId();
                selectedModelIds.add(currentModelId);
                CheckBox checkBox = optionCheckBoxes.get(currentModelId);
                if (checkBox != null && !checkBox.isSelected()) {
                    checkBox.setSelected(true);
                }
                refreshModelStatus();
            });
            for (ModelDownloadManager.KwsModelOption option : options) {
                modelOptionsBox.getChildren().add(buildModelOptionRow(option));
            }
        }
        initAuxModelSection();
        if (downloadSelectedButton != null) {
            downloadSelectedButton.disableProperty().bind(modelDownloadManager.downloadingProperty());
        }
        if (refreshModelStatusButton != null) {
            refreshModelStatusButton.disableProperty().bind(modelDownloadManager.downloadingProperty());
        }
    }

    private VBox buildModelOptionRow(ModelDownloadManager.KwsModelOption option) {
        CheckBox checkBox = new CheckBox(option.getName());
        checkBox.setSelected(selectedModelIds.contains(option.getId()));
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                selectedModelIds.add(option.getId());
                return;
            }
            if (option.getId().equals(currentModelId)) {
                checkBox.setSelected(true);
                statusLabel.setText("当前模型必须被选中");
                statusLabel.setStyle("-fx-text-fill: #FF9800;");
                return;
            }
            selectedModelIds.remove(option.getId());
        });
        optionCheckBoxes.put(option.getId(), checkBox);

        Label status = new Label();
        statusLabels.put(option.getId(), status);

        HBox header = new HBox(10, checkBox, status);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(checkBox, Priority.ALWAYS);

        Label description = new Label(option.getDescription());
        description.setWrapText(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(360);
        progressBar.setVisible(false);
        progressBars.put(option.getId(), progressBar);

        VBox container = new VBox(6, header, description, progressBar);
        container.setPadding(new Insets(6, 0, 6, 0));
        return container;
    }

    private void initAuxModelSection() {
        if (auxModelOptionsBox == null) {
            return;
        }
        auxModelOptionsBox.getChildren().clear();
        auxModelOptionsBox.getChildren().add(buildAuxModelRow(
                ASR_OPTION_ID,
                "ASR 语音识别模型",
                "用于本地语音识别（Sherpa-ONNX）",
                asrModelSelected,
                selected -> asrModelSelected = selected));
        auxModelOptionsBox.getChildren().add(buildAuxModelRow(
                VAD_OPTION_ID,
                "VAD 静音检测模型",
                "用于静音段检测与分段触发",
                vadModelSelected,
                selected -> vadModelSelected = selected));
    }

    private VBox buildAuxModelRow(
            String optionId,
            String name,
            String descriptionText,
            boolean selected,
            java.util.function.Consumer<Boolean> onSelectedChange) {
        CheckBox checkBox = new CheckBox(name);
        checkBox.setSelected(selected);
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> onSelectedChange.accept(newVal));
        optionCheckBoxes.put(optionId, checkBox);

        Label status = new Label();
        statusLabels.put(optionId, status);

        HBox header = new HBox(10, checkBox, status);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(checkBox, Priority.ALWAYS);

        Label description = new Label(descriptionText);
        description.setWrapText(true);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(360);
        progressBar.setVisible(false);
        progressBars.put(optionId, progressBar);

        VBox container = new VBox(6, header, description, progressBar);
        container.setPadding(new Insets(6, 0, 6, 0));
        return container;
    }

    private void updateAuxStatus(String optionId, boolean ready) {
        Label status = statusLabels.get(optionId);
        if (status != null) {
            status.setText(ready ? "已就绪" : "未就绪");
        }
        ProgressBar bar = progressBars.get(optionId);
        if (bar != null && ready) {
            bar.setVisible(false);
        }
    }

    // =========================================================================
    // 窗口管理
    // =========================================================================

    private void bindWindowClose() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.setOnHidden(event -> {
            modelDownloadManager.close();
            modelCatalogExecutor.shutdownNow();
        });
    }

    /**
     * 关闭当前设置窗口
     */
    private void closeWindow() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }
}
