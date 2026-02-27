package com.classroomassistant;

import com.classroomassistant.ui.MainController;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用主入口 (Main Application Entry)
 *
 * <p>基于 JavaFX 框架的应用启动类。
 * 核心职责：
 * <ul>
 *   <li>实例化并初始化 {@link AppContext} 全局上下文。</li>
 *   <li>配置 FXML 加载器，实现控制器的手动依赖注入。</li>
 *   <li>设置主舞台（Stage）的初始场景、样式表和窗口属性。</li>
 *   <li>在应用退出时触发优雅停机流程。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private AppContext appContext;

    /**
     * JavaFX 启动回调
     * <p>完成应用上下文初始化和主界面的加载显示。
     *
     * @param primaryStage 主舞台窗口
     * @throws IOException 如果 FXML 资源加载失败
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        this.appContext = new AppContext();
        this.appContext.initialize();

        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/MainView.fxml"));
        loader.setControllerFactory(clazz -> {
            if (clazz == MainController.class) {
                return new MainController(appContext);
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("创建控制器失败: " + clazz.getName(), e);
            }
        });

        Scene scene = new Scene(loader.load(), 900, 700);
        scene.getStylesheets().add("/css/styles.css");

        primaryStage.setTitle("学生课堂挂机程序 v3.0");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        logger.info("应用启动完成");
    }

    /**
     * JavaFX 退出回调
     * <p>调用 {@link AppContext#shutdown()} 以释放线程池、关闭录音等资源。
     */
    @Override
    public void stop() {
        if (appContext != null) {
            appContext.shutdown();
        }
        logger.info("应用已退出");
    }
}

