package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.platform.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端平台提供者实现
 *
 * <p>为桌面平台（Windows/macOS/Linux）提供平台特定的服务实现。
 * 实现 {@link PlatformProvider} 接口，提供音频录制、偏好设置、
 * 文件存储和安全存储等平台服务。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class DesktopPlatformProvider implements PlatformProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(DesktopPlatformProvider.class);
    
    private final DesktopAudioRecorder audioRecorder;
    private final DesktopPreferences preferences;
    private final DesktopStorage storage;
    private final DesktopSecureStorage secureStorage;

    private volatile boolean initialized = false;
    
    public DesktopPlatformProvider() {
        this.audioRecorder = new DesktopAudioRecorder();
        this.preferences = new DesktopPreferences();
        this.storage = new DesktopStorage();
        this.secureStorage = new DesktopSecureStorage();
    }

    /**
     * 初始化平台提供者
     * <p>加载配置、准备资源目录等。
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        logger.info("初始化桌面平台提供者...");
        storage.ensureDirectories();
        initialized = true;
        logger.info("桌面平台提供者初始化完成");
    }

    /**
     * 关闭平台提供者，释放资源
     */
    public void shutdown() {
        logger.info("关闭桌面平台提供者...");
        if (audioRecorder != null) {
            audioRecorder.release();
        }
        logger.info("桌面平台提供者已关闭");
    }
    
    @Override
    public String getPlatformName() {
        return "desktop";
    }
    
    @Override
    public PlatformAudioRecorder getAudioRecorder() {
        return audioRecorder;
    }
    
    @Override
    public PlatformPreferences getPreferences() {
        return preferences;
    }
    
    @Override
    public PlatformStorage getStorage() {
        return storage;
    }
    
    @Override
    public PlatformSecureStorage getSecureStorage() {
        return secureStorage;
    }
    
    @Override
    public void runOnMainThread(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
    
    @Override
    public void showToast(String message) {
        runOnMainThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    @Override
    public String getAppVersion() {
        return "3.0.0";
    }
}
