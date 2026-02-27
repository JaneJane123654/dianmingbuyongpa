package com.classroomassistant.core.platform;

/**
 * 平台服务提供者
 * 各平台实现此接口，提供平台特定的服务实例
 */
public interface PlatformProvider {
    
    /**
     * 获取平台名称
     * @return "desktop" / "android"
     */
    String getPlatformName();
    
    /**
     * 获取音频录制器
     */
    PlatformAudioRecorder getAudioRecorder();
    
    /**
     * 获取偏好设置存储
     */
    PlatformPreferences getPreferences();
    
    /**
     * 获取文件存储
     */
    PlatformStorage getStorage();
    
    /**
     * 获取安全存储
     */
    PlatformSecureStorage getSecureStorage();
    
    /**
     * 在主线程运行任务
     * 桌面端: Platform.runLater()
     * 安卓端: runOnUiThread()
     */
    void runOnMainThread(Runnable task);
    
    /**
     * 显示提示消息
     * 桌面端: Alert
     * 安卓端: Toast
     */
    void showToast(String message);
    
    /**
     * 获取应用版本
     */
    String getAppVersion();
}
