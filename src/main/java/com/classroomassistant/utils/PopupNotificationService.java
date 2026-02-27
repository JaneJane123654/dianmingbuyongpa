package com.classroomassistant.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * 基于 JavaFX Popup 的弹窗通知服务实现 (JavaFX Popup Notification Service)
 *
 * <p>该类实现了 {@link NotificationService} 接口，采用 JavaFX 的 {@link Popup} 组件构建轻量级通知窗口。
 * 它可以替代操作系统原生通知，提供自定义样式（如背景色、圆角、边框色）和点击交互功能。
 *
 * <p>特性：
 * <ul>
 *   <li>自动定位：在当前活动窗口的右上角弹出。</li>
 *   <li>自动消失：默认显示 3 秒后自动隐藏。</li>
 *   <li>样式区分：针对 Info、Warning、Error 采用不同的主题色。</li>
 *   <li>交互支持：支持注册点击回调逻辑。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class PopupNotificationService implements NotificationService {

    /**
     * 显示信息级通知
     *
     * @param title   标题
     * @param message 消息内容
     */
    @Override
    public void showInfo(String title, String message) {
        show(title, message, "#2196F3", null);
    }

    /**
     * 显示警告级通知
     *
     * @param title   标题
     * @param message 消息内容
     */
    @Override
    public void showWarning(String title, String message) {
        show(title, message, "#FF9800", null);
    }

    /**
     * 显示警告级通知，并指定点击后的回调逻辑
     *
     * @param title   标题
     * @param message 消息内容
     * @param action  点击通知后执行的任务
     */
    @Override
    public void showWarning(String title, String message, Runnable action) {
        show(title, message, "#FF9800", action);
    }

    /**
     * 显示错误级通知
     *
     * @param title   标题
     * @param message 消息内容
     */
    @Override
    public void showError(String title, String message) {
        show(title, message, "#F44336", null);
    }

    /**
     * 核心通知渲染与显示逻辑
     * <p>通过 {@link Platform#runLater(Runnable)} 确保在 UI 线程执行。
     *
     * @param title       标题文本
     * @param message     详细内容
     * @param color       主题色（十六进制字符串）
     * @param clickAction 点击后的可选回调
     */
    private void show(String title, String message, String color, Runnable clickAction) {
        Platform.runLater(() -> {
            Window window = Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
            if (window == null) {
                return;
            }

            Popup popup = new Popup();
            VBox box = new VBox(6);
            box.setPadding(new Insets(10));
            box.setStyle(
                "-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 2px; -fx-background-radius: 6px; -fx-border-radius: 6px;"
            );

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);

            box.getChildren().addAll(titleLabel, messageLabel);

            // 点击通知时触发回调
            if (clickAction != null) {
                box.setOnMouseClicked(e -> {
                    popup.hide();
                    clickAction.run();
                });
                box.setStyle(box.getStyle() + " -fx-cursor: hand;");
            }

            Scene scene = new Scene(box);
            popup.getContent().add(scene.getRoot());
            popup.setAutoHide(true);

            double x = window.getX() + window.getWidth() - 320;
            double y = window.getY() + 40;
            popup.show(window, x, y);

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> popup.hide());
            delay.play();
        });
    }
}

