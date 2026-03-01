package com.classroomassistant.desktop;

import com.classroomassistant.desktop.platform.DesktopPlatformProvider;
import com.classroomassistant.desktop.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端主应用程序
 *
 * <p>基于 JavaFX 框架的桌面应用启动类。
 * 核心职责：
 * <ul>
 *   <li>实例化并初始化桌面平台提供者 {@link DesktopPlatformProvider}。</li>
 *   <li>配置 FXML 加载器，实现控制器的手动依赖注入。</li>
 *   <li>设置主舞台（Stage）的初始场景、样式表和窗口属性。</li>
 *   <li>在应用退出时触发优雅停机流程。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class MainApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private DesktopPlatformProvider platformProvider;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            logger.info("启动桌面应用程序...");

            this.platformProvider = new DesktopPlatformProvider();
            this.platformProvider.initialize();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            loader.setControllerFactory(clazz -> {
                if (clazz == MainController.class) {
                    return new MainController(platformProvider);
                }
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new IllegalStateException("创建控制器失败: " + clazz.getName(), e);
                }
            });

            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 700);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            primaryStage.setTitle("课堂助手 v3.0.0");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            primaryStage.setOnCloseRequest(event -> {
                logger.info("应用程序关闭");
                shutdown();
            });

            primaryStage.show();
            logger.info("应用程序已启动");
        } catch (Exception e) {
            logger.error("应用启动失败", e);
            throw e;
        }
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        if (platformProvider != null) {
            platformProvider.shutdown();
        }
        logger.info("应用已退出");
    }
    
    public static void main(String[] args) {
        Path javafxHome = Paths.get("").toAbsolutePath().resolve(".javafx");
        System.setProperty("user.home", javafxHome.toString());
        System.setProperty("javafx.user.home", javafxHome.toString());
        try {
            Files.createDirectories(javafxHome);
        } catch (IOException e) {
            logger.warn("JavaFX 缓存目录创建失败: {}", javafxHome, e);
        }
        launch(args);
    }
}
