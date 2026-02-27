package com.classroomassistant;

/**
 * 启动器类 - 用于打包成可执行 JAR 时绑定 JavaFX 模块
 * 
 * JavaFX 11+ 不再包含在 JDK 中，且作为模块化库，直接从 MainApp 启动
 * 在打包的 fat JAR 中会遇到模块系统问题。
 * 
 * 此启动器类作为非 JavaFX Application 类，可以正常作为 JAR 入口点。
 * 
 * 打包命令: mvn clean package
 * 运行命令: java -jar target/classroom-assistant-3.0.0-all.jar
 */
public class Launcher {
    
    /**
     * 主入口点 - 转发到 JavaFX 应用
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
