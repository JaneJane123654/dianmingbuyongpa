package com.classroomassistant.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppPaths 单元测试
 *
 * <p>验证应用路径工具类的目录创建与路径获取功能。
 * 
 * @author Code Assistant
 * @date 2026-02-01
 */
class AppPathsTest {

    private static final String APP_NAME = "ClassroomAssistant";
    private AppPaths appPaths;

    @BeforeEach
    void setUp() {
        appPaths = new AppPaths(APP_NAME);
    }

    @Test
    @DisplayName("getModelsDir 应返回非空路径")
    void getModelsDir_shouldReturnNonNullPath() {
        Path modelsDir = appPaths.getModelsDir();

        assertNotNull(modelsDir);
        assertTrue(modelsDir.toString().contains("models"));
    }

    @Test
    @DisplayName("getRecordingsDir 应返回非空路径")
    void getRecordingsDir_shouldReturnNonNullPath() {
        Path recordingsDir = appPaths.getRecordingsDir();

        assertNotNull(recordingsDir);
        assertTrue(recordingsDir.toString().contains("recordings"));
    }

    @Test
    @DisplayName("getCacheDir 应返回非空路径")
    void getCacheDir_shouldReturnNonNullPath() {
        Path cacheDir = appPaths.getCacheDir();

        assertNotNull(cacheDir);
        assertTrue(cacheDir.toString().contains("cache"));
    }

    @Test
    @DisplayName("getLogsDir 应返回非空路径")
    void getLogsDir_shouldReturnNonNullPath() {
        Path logsDir = appPaths.getLogsDir();

        assertNotNull(logsDir);
        assertTrue(logsDir.toString().contains("logs"));
    }

    @Test
    @DisplayName("getUserDataDir 应返回非空路径")
    void getUserDataDir_shouldReturnNonNullPath() {
        Path userDataDir = appPaths.getUserDataDir();

        assertNotNull(userDataDir);
    }

    @Test
    @DisplayName("各目录路径应为绝对路径")
    void allPaths_shouldBeAbsolute() {
        assertTrue(appPaths.getModelsDir().isAbsolute());
        assertTrue(appPaths.getRecordingsDir().isAbsolute());
        assertTrue(appPaths.getCacheDir().isAbsolute());
        assertTrue(appPaths.getLogsDir().isAbsolute());
        assertTrue(appPaths.getUserDataDir().isAbsolute());
    }

    @Test
    @DisplayName("getModelsDir 多次调用应返回相同路径")
    void getModelsDir_multipleCalls_shouldReturnSamePath() {
        Path first = appPaths.getModelsDir();
        Path second = appPaths.getModelsDir();

        assertEquals(first, second);
    }

    @Test
    @DisplayName("不同目录应指向不同路径")
    void differentDirs_shouldHaveDifferentPaths() {
        Path modelsDir = appPaths.getModelsDir();
        Path recordingsDir = appPaths.getRecordingsDir();
        Path cacheDir = appPaths.getCacheDir();
        Path logsDir = appPaths.getLogsDir();

        assertNotEquals(modelsDir, recordingsDir);
        assertNotEquals(modelsDir, cacheDir);
        assertNotEquals(modelsDir, logsDir);
        assertNotEquals(recordingsDir, cacheDir);
        assertNotEquals(recordingsDir, logsDir);
        assertNotEquals(cacheDir, logsDir);
    }

    @Test
    @DisplayName("所有目录应在 userDataDir 下")
    void allDirs_shouldBeUnderUserDataDir() {
        Path userDataDir = appPaths.getUserDataDir();

        assertTrue(appPaths.getModelsDir().startsWith(userDataDir));
        assertTrue(appPaths.getRecordingsDir().startsWith(userDataDir));
        assertTrue(appPaths.getCacheDir().startsWith(userDataDir));
        assertTrue(appPaths.getLogsDir().startsWith(userDataDir));
    }
}
