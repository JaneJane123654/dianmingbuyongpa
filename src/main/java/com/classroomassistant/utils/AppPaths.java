package com.classroomassistant.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用路径计算工具
 *
 * <p>用于统一计算用户数据目录与子目录，避免硬编码路径导致跨平台问题。
 */
public class AppPaths {

    private final String appName;
    private final Path userDataDir;

    public AppPaths(String appName) {
        this.appName = appName;
        this.userDataDir = resolveUserDataDir(appName);
    }

    public Path getUserDataDir() {
        return userDataDir;
    }

    public Path getModelsDir() {
        return userDataDir.resolve("models");
    }

    public Path getRecordingsDir() {
        return userDataDir.resolve("recordings");
    }

    public Path getLogsDir() {
        return userDataDir.resolve("logs");
    }

    public Path getCacheDir() {
        return userDataDir.resolve("cache");
    }

    private static Path resolveUserDataDir(String appName) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, appName);
            }
            return Paths.get(userHome, "AppData", "Roaming", appName);
        }

        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", appName);
        }

        return Paths.get(userHome, ".local", "share", appName);
    }
}

