package com.classroomassistant.ui;

import com.classroomassistant.AppContext;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.session.ClassSessionManager;
import com.classroomassistant.storage.ModelDescriptor;
import com.classroomassistant.storage.ModelDownloadManager;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.utils.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设置页面控制器 (Settings Controller)
 *
 * <p>负责“系统设置”界面的交互逻辑。
 * 主要功能包括：
 * <ul>
 *   <li>从 {@link PreferencesManager} 加载当前用户配置并展示在 UI 控件上。</li>
 *   <li>收集用户在界面上的修改。</li>
 *   <li>执行基本的表单校验。</li>
 *   <li>将新配置持久化，并通知 {@link ClassSessionManager} 即时应用。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final PreferencesManager preferencesManager;
    private final ClassSessionManager classSessionManager;
    private final ModelDownloadManager modelDownloadManager;

    @FXML
    private TextField keywordsField;

    @FXML
    private CheckBox vadEnabledCheckBox;

    @FXML
    private Spinner<Integer> quietThresholdSpinner;

    @FXML
    private Spinner<Integer> lookbackSecondsSpinner;

    @FXML
    private CheckBox recordingSaveCheckBox;

    @FXML
    private Spinner<Integer> recordingRetentionSpinner;

    @FXML
    private ComboBox<LLMConfig.ModelType> providerComboBox;

    @FXML
    private TextField modelNameField;

    @FXML
    private PasswordField tokenField;

    @FXML
    private PasswordField speechApiKeyField;

    @FXML
    private Label statusLabel;

    @FXML
    private Label modelDownloadStatusLabel;

    @FXML
    private Button openModelDownloadButton;

    /**
     * 构造设置页面控制器
     *
     * @param appContext 应用全局上下文
     */
    public SettingsController(AppContext appContext) {
        this.preferencesManager = appContext.getPreferencesManager();
        this.classSessionManager = appContext.getClassSessionManager();
        this.modelDownloadManager = new ModelDownloadManager(appContext.getModelRepository(), appContext.getConfigManager());
    }

    /**
     * JavaFX 初始化回调
     * <p>配置 Spinner 的取值范围，初始化 ComboBox 选项，并加载现有配置填充表单。
     */
    @FXML
    private void initialize() {
        quietThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 30, 5));
        lookbackSecondsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 240));
        recordingRetentionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 7));
        recordingRetentionSpinner.disableProperty().bind(recordingSaveCheckBox.selectedProperty().not());

        providerComboBox.setItems(FXCollections.observableArrayList(LLMConfig.ModelType.values()));
        providerComboBox.getSelectionModel().select(LLMConfig.ModelType.QIANFAN);

        // Token 输入实时校验提示
        tokenField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                statusLabel.setText("提示：未设置 Token 将无法使用 AI 问答");
                statusLabel.setStyle("-fx-text-fill: #FF9800;");
            } else {
                statusLabel.setText("");
            }
        });

        // Speech API Key 输入提示
        speechApiKeyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                statusLabel.setText("");
            }
        });

        UserPreferences prefs = preferencesManager.load();
        keywordsField.setText(prefs.getKeywords());
        vadEnabledCheckBox.setSelected(prefs.isVadEnabled());
        quietThresholdSpinner.getValueFactory().setValue(prefs.getVadQuietThresholdSeconds());
        lookbackSecondsSpinner.getValueFactory().setValue(prefs.getAudioLookbackSeconds());
        recordingSaveCheckBox.setSelected(prefs.isRecordingSaveEnabled());
        recordingRetentionSpinner.getValueFactory().setValue(prefs.getRecordingRetentionDays());
        providerComboBox.getSelectionModel().select(prefs.getAiModelType());
        modelNameField.setText(prefs.getAiModelName());
        refreshModelDownloadStatus();
        javafx.application.Platform.runLater(this::bindWindowClose);
    }

    /**
     * “保存”按钮点击事件
     * <p>读取表单数据，规整关键词，保存到持久化存储，并通知业务层应用更改。
     */
    @FXML
    private void save() {
        try {
            String keywords = Validator.normalizeKeywords(keywordsField.getText());
            int quietThreshold = quietThresholdSpinner.getValue();
            int lookbackSeconds = lookbackSecondsSpinner.getValue();
            int retentionDays = recordingRetentionSpinner.getValue();
            LLMConfig.ModelType modelType = providerComboBox.getSelectionModel().getSelectedItem();
            String modelName = modelNameField.getText() == null ? "" : modelNameField.getText().trim();
            String token = tokenField.getText() == null ? "" : tokenField.getText().trim();
            String speechApiKey = speechApiKeyField.getText() == null ? "" : speechApiKeyField.getText().trim();

            UserPreferences updated =
                UserPreferences.builder()
                    .keywords(keywords)
                    .vadEnabled(vadEnabledCheckBox.isSelected())
                    .vadQuietThresholdSeconds(quietThreshold)
                    .audioLookbackSeconds(lookbackSeconds)
                    .recordingSaveEnabled(recordingSaveCheckBox.isSelected())
                    .recordingRetentionDays(retentionDays)
                    .aiModelType(modelType)
                    .aiModelName(modelName)
                    .aiTokenPlainText(token)
                    .speechApiKey(speechApiKey)
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
     * “取消”按钮点击事件
     * <p>放弃所有未保存的更改并关闭窗口。
     */
    @FXML
    private void cancel() {
        closeWindow();
    }

    @FXML
    private void openModelManager() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModelDownloadView.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == ModelDownloadController.class) {
                    return new ModelDownloadController(modelDownloadManager);
                }
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("创建控制器失败: " + clazz.getName(), e);
                }
            });
            Parent root = loader.load();
            ModelDownloadController controller = loader.getController();
            controller.setOnCompleteCallback(this::refreshModelDownloadStatus);
            controller.setOnSkipCallback(this::refreshModelDownloadStatus);
            Stage stage = new Stage();
            stage.setTitle("模型管理");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(statusLabel.getScene().getWindow());
            stage.setScene(new Scene(root, 520, 480));
            stage.showAndWait();
            refreshModelDownloadStatus();
        } catch (Exception e) {
            logger.error("打开模型管理失败", e);
            statusLabel.setText("✗ 打开模型管理失败: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    private void refreshModelDownloadStatus() {
        if (modelDownloadStatusLabel == null) {
            return;
        }
        java.util.List<ModelDescriptor> missingModels = modelDownloadManager.checkMissingModels();
        if (missingModels.isEmpty()) {
            modelDownloadStatusLabel.setText("模型已就绪");
        } else {
            modelDownloadStatusLabel.setText("缺少模型数量: " + missingModels.size());
        }
    }

    private void bindWindowClose() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.setOnHidden(event -> modelDownloadManager.close());
    }

    /**
     * 关闭当前设置窗口
     * <p>获取当前控件关联的 {@link Stage} 并执行关闭操作。
     */
    private void closeWindow() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }
}
