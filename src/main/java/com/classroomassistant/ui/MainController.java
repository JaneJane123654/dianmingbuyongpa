package com.classroomassistant.ui;

import com.classroomassistant.AppContext;
import com.classroomassistant.session.ClassSessionManager;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主界面控制器
 *
 * <p>负责主界面交互逻辑与状态展示，耗时逻辑交由会话编排层处理。
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final AppContext appContext;
    private final ClassSessionManager classSessionManager;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Label recordingStatusLabel;

    @FXML
    private Label detectionStatusLabel;

    @FXML
    private Label quietSecondsLabel;

    @FXML
    private Label recordingSaveStatusLabel;

    @FXML
    private TextArea lectureTextArea;

    @FXML
    private TextArea answerTextArea;

    @FXML
    private Label hintLabel;

    public MainController(AppContext appContext) {
        this.appContext = appContext;
        this.classSessionManager = appContext.getClassSessionManager();
    }

    @FXML
    private void initialize() {
        recordingStatusLabel.textProperty().bind(classSessionManager.recordingStatusTextProperty());
        detectionStatusLabel.textProperty().bind(classSessionManager.detectionStatusTextProperty());
        quietSecondsLabel.textProperty().bind(classSessionManager.quietSecondsTextProperty());
        recordingSaveStatusLabel.textProperty().bind(classSessionManager.recordingSaveStatusTextProperty());
        lectureTextArea.textProperty().bind(classSessionManager.lectureTextProperty());
        answerTextArea.textProperty().bind(classSessionManager.answerTextProperty());
        hintLabel.textProperty().bind(classSessionManager.hintTextProperty());

        stopButton.disableProperty().bind(classSessionManager.recordingProperty().not());
        startButton.disableProperty().bind(classSessionManager.recordingProperty());
    }

    @FXML
    private void startClass() {
        classSessionManager.startClass();
    }

    @FXML
    private void stopClass() {
        classSessionManager.stopClass();
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettingsView.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == SettingsController.class) {
                    return new SettingsController(appContext);
                }
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("创建控制器失败: " + clazz.getName(), e);
                }
            });
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("设置");
            Scene scene = new Scene(root, 520, 650);
            scene.getStylesheets().add("/css/styles.css");
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            logger.error("打开设置页面失败", e);
            Platform.runLater(() -> hintLabel.setText("打开设置失败: " + e.getMessage()));
        }
    }
}

