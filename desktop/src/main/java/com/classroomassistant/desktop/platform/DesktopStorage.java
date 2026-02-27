package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.platform.PlatformStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 桌面端文件存储实现
 */
public class DesktopStorage implements PlatformStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(DesktopStorage.class);
    
    private final File appDataDir;
    
    public DesktopStorage() {
        String userHome = System.getProperty("user.home");
        this.appDataDir = new File(userHome, ".classroom-assistant");
        
        // 确保目录存在
        if (!appDataDir.exists()) {
            appDataDir.mkdirs();
        }
    }
    
    @Override
    public File getAppDataDir() {
        return appDataDir;
    }
    
    @Override
    public File getModelsDir() {
        File modelsDir = new File(appDataDir, "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        return modelsDir;
    }
    
    @Override
    public File getLogsDir() {
        File logsDir = new File(appDataDir, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        return logsDir;
    }
    
    @Override
    public File getCacheDir() {
        File cacheDir = new File(appDataDir, "cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDir;
    }
    
    @Override
    public boolean exists(String relativePath) {
        return new File(appDataDir, relativePath).exists();
    }
    
    @Override
    public InputStream openInputStream(String relativePath) throws Exception {
        File file = new File(appDataDir, relativePath);
        return new FileInputStream(file);
    }
    
    @Override
    public OutputStream openOutputStream(String relativePath) throws Exception {
        File file = new File(appDataDir, relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return new FileOutputStream(file);
    }
    
    @Override
    public boolean delete(String relativePath) {
        File file = new File(appDataDir, relativePath);
        return file.delete();
    }
    
    @Override
    public long getAvailableSpace() {
        return appDataDir.getUsableSpace();
    }

    /**
     * 确保所有必需的目录存在
     */
    public void ensureDirectories() {
        getModelsDir();
        getLogsDir();
        getCacheDir();
        // 创建录音目录
        File recordingsDir = new File(appDataDir, "recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
    }
}
