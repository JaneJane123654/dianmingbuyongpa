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
 * 基于 JavaFX Popup 的简易通知实现
 *
 * <p>用于替代平台原生通知，满足“简单文本通知”的需求。
 */
public class PopupNotificationService implements NotificationService {

    @Override
    public void showInfo(String title, String message) {
        show(title, message, "#2196F3");
    }

    @Override
    public void showWarning(String title, String message) {
        show(title, message, "#FF9800");
    }

    @Override
    public void showError(String title, String message) {
        show(title, message, "#F44336");
    }

    private void show(String title, String message, String color) {
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

