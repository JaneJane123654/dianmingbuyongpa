package com.classroomassistant.ui;

import com.classroomassistant.AppContext;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.session.ClassSessionManager;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.utils.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 设置页面控制器
 *
 * <p>负责用户配置的编辑与保存，保存后即时生效（调用会话编排层应用新配置）。
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final PreferencesManager preferencesManager;
    private final ClassSessionManager classSessionManager;

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
    private Label statusLabel;

    public SettingsController(AppContext appContext) {
        this.preferencesManager = appContext.getPreferencesManager();
        this.classSessionManager = appContext.getClassSessionManager();
    }

    @FXML
    private void initialize() {
        quietThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 30, 5));
        lookbackSecondsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 240));
        recordingRetentionSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 7));
        recordingRetentionSpinner.disableProperty().bind(recordingSaveCheckBox.selectedProperty().not());

        providerComboBox.setItems(FXCollections.observableArrayList(LLMConfig.ModelType.values()));
        providerComboBox.getSelectionModel().select(LLMConfig.ModelType.QIANFAN);

        UserPreferences prefs = preferencesManager.load();
        keywordsField.setText(prefs.getKeywords());
        vadEnabledCheckBox.setSelected(prefs.isVadEnabled());
        quietThresholdSpinner.getValueFactory().setValue(prefs.getVadQuietThresholdSeconds());
        lookbackSecondsSpinner.getValueFactory().setValue(prefs.getAudioLookbackSeconds());
        recordingSaveCheckBox.setSelected(prefs.isRecordingSaveEnabled());
        recordingRetentionSpinner.getValueFactory().setValue(prefs.getRecordingRetentionDays());
        providerComboBox.getSelectionModel().select(prefs.getAiModelType());
        modelNameField.setText(prefs.getAiModelName());
    }

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
                    .build();

            preferencesManager.save(updated);
            classSessionManager.applySettings(updated);
            statusLabel.setText("保存成功");
            closeWindow();
        } catch (Exception e) {
            logger.error("保存设置失败", e);
            statusLabel.setText("保存失败: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        stage.close();
    }
}

