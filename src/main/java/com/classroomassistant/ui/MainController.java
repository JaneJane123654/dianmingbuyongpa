package com.classroomassistant.ui;

import com.classroomassistant.AppContext;
import com.classroomassistant.session.ClassSessionManager;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
 * 主界面控制器 (Main Controller)
 *
 * <p>负责 JavaFX 主界面的 UI 交互逻辑与状态同步。
 * 本类遵循“瘦控制器”原则，仅处理 UI 事件的分发和界面的数据绑定，
 * 核心业务逻辑（如录音控制、语音处理等）完全委托给 {@link ClassSessionManager} 处理。
 *
 * @author Code Assistant
 * @date 2026-01-31
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

    @FXML
    private Label engineStatusLabel;

    @FXML
    private Label modelStatusLabel;

    @FXML
    private TextArea logTextArea;

    @FXML
    private Button clearLogButton;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 构造主界面控制器
     *
     * @param appContext 应用全局上下文，用于获取业务组件
     */
    public MainController(AppContext appContext) {
        this.appContext = appContext;
        this.classSessionManager = appContext.getClassSessionManager();
    }

    /**
     * JavaFX 初始化回调
     * <p>在此处建立 UI 控件与 {@link ClassSessionManager} 中 Property 的双向或单向绑定。
     */
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

        // 注入"打开设置"回调，供 Token 缺失时自动触发
        classSessionManager.setOpenSettingsCallback(this::openSettings);

        // 注入日志回调，用于 UI 显示运行日志
        classSessionManager.setLogCallback(this::appendLog);

        // 显示当前语音引擎状态
        String engine = appContext.getConfigManager().getSpeechEngineDefault();
        engineStatusLabel.setText("引擎: " + engine);
        if ("FAKE".equalsIgnoreCase(engine)) {
            engineStatusLabel.setStyle("-fx-text-fill: #FF9800;");
        } else if ("API".equalsIgnoreCase(engine)) {
            engineStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            engineStatusLabel.setStyle("-fx-text-fill: #2196F3;");
        }

        // 初始日志
        appendLog("应用已启动，语音引擎: " + engine);
    }

    /**
     * “开始上课”按钮点击事件
     * <p>调用会话管理器启动音频采集和实时监测。
     */
    @FXML
    private void startClass() {
        appendLog("开始录音，进入监听状态");
        classSessionManager.startClass();
    }

    /**
     * "下课"按钮点击事件
     * <p>停止当前所有正在进行的音频任务和 AI 生成任务。
     */
    @FXML
    private void stopClass() {
        appendLog("停止录音");
        classSessionManager.stopClass();
    }

    /**
     * "清空日志"按钮点击事件
     */
    @FXML
    private void clearLog() {
        logTextArea.clear();
    }

    /**
     * 向日志面板追加一条日志
     *
     * @param message 日志内容
     */
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(TIME_FORMATTER);
            String line = "[" + time + "] " + message + "\n";
            logTextArea.appendText(line);
        });
    }

    /**
     * “设置”按钮点击事件或内部自动触发，弹出设置窗口
     * <p>该方法会以模态对话框 (Modal) 的方式加载并显示设置界面。
     */
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

