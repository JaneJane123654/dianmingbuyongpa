package com.classroomassistant.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用路径计算工具 (Application Paths Utility)
 *
 * <p>该类负责统一计算和管理应用在不同操作系统下的用户数据目录及其子目录（如模型、录音、日志等）。
 * 旨在避免在代码中硬编码绝对路径，确保应用具备良好的跨平台兼容性。
 *
 * <p>遵循不同操作系统的标准路径规范：
 * <ul>
 *   <li>Windows: {@code %APPDATA%\[appName]}</li>
 *   <li>macOS: {@code ~/Library/Application Support/[appName]}</li>
 *   <li>Linux: {@code ~/.local/share/[appName]}</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class AppPaths {

    private final String appName;
    private final Path userDataDir;

    /**
     * 构造路径工具
     *
     * @param appName 应用名称，用于在系统数据目录下创建对应的子文件夹
     */
    public AppPaths(String appName) {
        this.appName = appName;
        this.userDataDir = resolveUserDataDir(appName);
    }

    /**
     * 获取用户数据根目录
     *
     * @return 根目录路径
     */
    public Path getUserDataDir() {
        return userDataDir;
    }

    /**
     * 获取模型文件存储目录
     *
     * @return {@code [userDataDir]/models}
     */
    public Path getModelsDir() {
        return userDataDir.resolve("models");
    }

    /**
     * 获取录音文件存储目录
     *
     * @return {@code [userDataDir]/recordings}
     */
    public Path getRecordingsDir() {
        return userDataDir.resolve("recordings");
    }

    /**
     * 获取日志文件存储目录
     *
     * @return {@code [userDataDir]/logs}
     */
    public Path getLogsDir() {
        return userDataDir.resolve("logs");
    }

    /**
     * 获取临时缓存目录
     *
     * @return {@code [userDataDir]/cache}
     */
    public Path getCacheDir() {
        return userDataDir.resolve("cache");
    }

    /**
     * 根据当前操作系统解析用户数据目录的基准路径
     *
     * @param appName 应用名称
     * @return 解析后的路径对象
     */
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

