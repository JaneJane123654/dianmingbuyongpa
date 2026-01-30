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
 * 应用主入口（JavaFX）
 *
 * <p>负责初始化应用上下文、加载主界面 FXML，并启动 UI 线程。
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private AppContext appContext;

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

    @Override
    public void stop() {
        if (appContext != null) {
            appContext.shutdown();
        }
        logger.info("应用已退出");
    }
}

