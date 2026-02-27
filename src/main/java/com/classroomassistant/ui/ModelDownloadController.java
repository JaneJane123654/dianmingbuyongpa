package com.classroomassistant.ui;

import com.classroomassistant.storage.ModelDescriptor;
import com.classroomassistant.storage.ModelDownloadManager;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型下载页面控制器 (Model Download Controller)
 *
 * <p>负责展示缺失模型列表，并提供下载/跳过操作。
 * 通过绑定 {@link ModelDownloadManager} 的 Property 实时显示下载进度。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class ModelDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(ModelDownloadController.class);

    private final ModelDownloadManager downloadManager;
    private List<ModelDescriptor> missingModels;
    private Runnable onCompleteCallback;
    private Runnable onSkipCallback;

    @FXML
    private Label totalStatusLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private VBox modelItemsBox;

    @FXML
    private Label currentModelLabel;

    @FXML
    private ProgressBar downloadProgressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Label sizeLabel;

    @FXML
    private Label speedLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Button startButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button skipButton;

    /**
     * 构造控制器
     *
     * @param downloadManager 模型下载管理器
     */
    public ModelDownloadController(ModelDownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    /**
     * 设置完成回调
     *
     * @param callback 下载完成后的回调
     */
    public void setOnCompleteCallback(Runnable callback) {
        this.onCompleteCallback = callback;
    }

    /**
     * 设置跳过回调
     *
     * @param callback 用户选择跳过后的回调
     */
    public void setOnSkipCallback(Runnable callback) {
        this.onSkipCallback = callback;
    }

    /**
     * JavaFX 初始化回调
     */
    @FXML
    private void initialize() {
        // 绑定下载状态
        currentModelLabel.textProperty().bind(downloadManager.currentModelProperty());
        downloadProgressBar.progressProperty().bind(downloadManager.progressProperty());
        progressLabel.textProperty().bind(
            Bindings.format("%.1f%%", downloadManager.progressProperty().multiply(100))
        );

        // 按钮状态绑定
        startButton.disableProperty().bind(downloadManager.downloadingProperty());
        cancelButton.disableProperty().bind(downloadManager.downloadingProperty().not());
        skipButton.disableProperty().bind(downloadManager.downloadingProperty());

        // 加载缺失模型列表
        loadMissingModels();
    }

    /**
     * 加载并显示缺失模型列表
     */
    private void loadMissingModels() {
        missingModels = downloadManager.checkMissingModels();

        if (missingModels.isEmpty()) {
            descriptionLabel.setText("✓ 所有模型已就绪，无需下载。");
            totalStatusLabel.setText("已就绪");
            totalStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
            startButton.setDisable(true);
            skipButton.setDisable(true);
            return;
        }

        totalStatusLabel.setText("缺少 " + missingModels.size() + " 个模型");
        totalStatusLabel.setStyle("-fx-text-fill: #FF9800;");

        modelItemsBox.getChildren().clear();
        for (ModelDescriptor model : missingModels) {
            HBox item = createModelItem(model);
            modelItemsBox.getChildren().add(item);
        }
    }

    /**
     * 创建单个模型项 UI
     */
    private HBox createModelItem(ModelDescriptor model) {
        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(8));
        hbox.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 4px;");

        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(true);
        checkBox.setDisable(true); // 暂时禁用选择

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(model.name());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label urlLabel = new Label(truncateUrl(model.downloadUrl().toString()));
        urlLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        infoBox.getChildren().addAll(nameLabel, urlLabel);

        StackPane infoIcon = createInfoIcon(model);

        Label statusLabel = new Label("待下载");
        statusLabel.setStyle("-fx-text-fill: #FF9800;");

        hbox.getChildren().addAll(checkBox, infoBox, infoIcon, statusLabel);
        return hbox;
    }

    /**
     * 截断过长的 URL
     */
    private String truncateUrl(String url) {
        if (url.length() <= 60) {
            return url;
        }
        return url.substring(0, 30) + "..." + url.substring(url.length() - 25);
    }

    private StackPane createInfoIcon(ModelDescriptor model) {
        StackPane icon = new StackPane();
        icon.getStyleClass().add("model-info-icon");
        Label text = new Label("!");
        text.getStyleClass().add("model-info-icon-text");
        icon.getChildren().add(text);
        Tooltip tooltip = new Tooltip(buildModelHint(model));
        Tooltip.install(icon, tooltip);
        return icon;
    }

    private String buildModelHint(ModelDescriptor model) {
        String name = model.name();
        if (name.contains("KWS") || name.contains("唤醒")) {
            return "唤醒词模型：用于检测唤醒词，建议填写常用姓名/变体。";
        }
        if (name.contains("ASR") || name.contains("识别")) {
            return "语音识别模型：用于转写课堂语音，建议在稳定网络下载。";
        }
        if (name.contains("VAD") || name.contains("静音")) {
            return "静音检测模型：用于判断是否安静超时，建议保持开启。";
        }
        return "模型说明：用于本地语音处理，确保相关功能可用。";
    }

    /**
     * "开始下载"按钮点击
     */
    @FXML
    private void startDownload() {
        if (missingModels == null || missingModels.isEmpty()) {
            return;
        }

        errorLabel.setText("");
        logger.info("开始下载 {} 个模型", missingModels.size());

        downloadManager.downloadAllAsync(missingModels, new ModelDownloadManager.DetailedDownloadCallback() {
            @Override
            public void onProgress(String modelName, double progress, long downloadedBytes, long totalBytes) {
                Platform.runLater(() -> {
                    sizeLabel.setText(formatSize(downloadedBytes) + " / " + formatSize(totalBytes));
                });
            }

            @Override
            public void onModelComplete(String modelName) {
                Platform.runLater(() -> {
                    logger.info("模型下载完成: {}", modelName);
                });
            }

            @Override
            public void onAllComplete() {
                Platform.runLater(() -> {
                    totalStatusLabel.setText("下载完成");
                    totalStatusLabel.setStyle("-fx-text-fill: #4CAF50;");
                    currentModelLabel.textProperty().unbind();
                    currentModelLabel.setText("✓ 所有模型下载完成");
                    if (onCompleteCallback != null) {
                        onCompleteCallback.run();
                    }
                    closeWindow();
                });
            }

            @Override
            public void onError(String modelName, String error) {
                Platform.runLater(() -> {
                    errorLabel.setText("下载失败: " + modelName + " - " + error);
                    logger.error("模型下载失败: {} - {}", modelName, error);
                });
            }
        });
    }

    /**
     * "取消"按钮点击
     */
    @FXML
    private void cancel() {
        downloadManager.cancelDownload();
        errorLabel.setText("下载已取消");
    }

    /**
     * "跳过"按钮点击
     */
    @FXML
    private void skip() {
        logger.info("用户跳过模型下载，将使用云端 API 模式");
        if (onSkipCallback != null) {
            onSkipCallback.run();
        }
        closeWindow();
    }

    /**
     * 关闭窗口
     */
    private void closeWindow() {
        Stage stage = (Stage) startButton.getScene().getWindow();
        stage.close();
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
