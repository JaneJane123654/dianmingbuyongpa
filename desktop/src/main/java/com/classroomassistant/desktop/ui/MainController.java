package com.classroomassistant.desktop.ui;

import com.classroomassistant.desktop.platform.DesktopPlatformProvider;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 桌面端主界面控制器
 *
 * <p>
 * 负责 JavaFX 主界面的 UI 交互逻辑与状态同步。
 * 本类遵循"瘦控制器"原则，仅处理 UI 事件的分发和界面的数据绑定。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DesktopPlatformProvider platformProvider;

    // UI 状态属性
    private final StringProperty recordingStatusText = new SimpleStringProperty("未录音");
    private final StringProperty detectionStatusText = new SimpleStringProperty("未启动");
    private final StringProperty quietSecondsText = new SimpleStringProperty("0s");
    private final StringProperty lectureText = new SimpleStringProperty("");
    private final StringProperty answerText = new SimpleStringProperty("");
    private final StringProperty hintText = new SimpleStringProperty("就绪");

    private volatile boolean isRecording = false;

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

    /**
     * 构造主界面控制器
     *
     * @param platformProvider 桌面平台提供者
     */
    public MainController(DesktopPlatformProvider platformProvider) {
        this.platformProvider = platformProvider;
    }

    /**
     * JavaFX 初始化回调
     */
    @FXML
    private void initialize() {
        // 绑定状态属性
        recordingStatusLabel.textProperty().bind(recordingStatusText);
        detectionStatusLabel.textProperty().bind(detectionStatusText);
        quietSecondsLabel.textProperty().bind(quietSecondsText);
        lectureTextArea.textProperty().bind(lectureText);
        answerTextArea.textProperty().bind(answerText);
        hintLabel.textProperty().bind(hintText);

        // 按钮状态
        stopButton.setDisable(true);

        // 显示引擎状态
        engineStatusLabel.setText("引擎: API");
        engineStatusLabel.setStyle("-fx-text-fill: #4CAF50;");

        // 录音保存状态
        recordingSaveStatusLabel.setText("未启用");

        appendLog("应用已启动");
    }

    /**
     * "开始上课"按钮点击事件
     */
    @FXML
    private void startClass() {
        if (isRecording) {
            return;
        }

        isRecording = true;
        startButton.setDisable(true);
        stopButton.setDisable(false);

        recordingStatusText.set("录音中");
        detectionStatusText.set("监听中");
        hintText.set("正在监听");

        appendLog("开始录音，进入监听状态");

        // 启动音频采集
        platformProvider.getAudioRecorder()
                .start(new com.classroomassistant.core.platform.PlatformAudioRecorder.AudioDataListener() {
                    @Override
                    public void onAudioData(byte[] data, int length) {
                        // 处理音频数据
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            appendLog("录音错误: " + error);
                            hintText.set("录音错误: " + error);
                        });
                    }
                });
    }

    /**
     * "下课"按钮点击事件
     */
    @FXML
    private void stopClass() {
        if (!isRecording) {
            return;
        }

        isRecording = false;
        startButton.setDisable(false);
        stopButton.setDisable(true);

        recordingStatusText.set("已停止");
        detectionStatusText.set("未启动");
        hintText.set("已停止");

        platformProvider.getAudioRecorder().stop();

        appendLog("停止录音");
    }

    /**
     * "设置"按钮点击事件
     * <p>
     * 打开设置窗口（模态对话框），使用 DesktopSettingsController 管理设置。
     */
    @FXML
    private void openSettings() {
        appendLog("打开设置页面");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SettingsView.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == DesktopSettingsController.class) {
                    return new DesktopSettingsController(platformProvider);
                }
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("创建控制器失败: " + clazz.getName(), e);
                }
            });

            Parent root = loader.load();
            Stage settingsStage = new Stage();
            settingsStage.setTitle("设置");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(settingsButton.getScene().getWindow());

            Scene scene = new Scene(root, 700, 680);
            // 复用主窗口的样式表
            scene.getStylesheets().addAll(settingsButton.getScene().getStylesheets());
            settingsStage.setScene(scene);
            settingsStage.setMinWidth(600);
            settingsStage.setMinHeight(500);
            settingsStage.showAndWait();

            appendLog("设置窗口已关闭");
        } catch (IOException e) {
            logger.error("打开设置窗口失败", e);
            platformProvider.showToast("打开设置失败: " + e.getMessage());
        }
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
     */
    private void appendLog(String message) {
        Platform.runLater(() -> {
            String time = LocalTime.now().format(TIME_FORMATTER);
            String line = "[" + time + "] " + message + "\n";
            logTextArea.appendText(line);
        });
    }
}
