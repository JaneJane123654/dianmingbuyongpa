package com.classroomassistant.core.platform;

/**
 * 平台偏好设置存储接口
 * 桌面端使用 java.util.prefs.Preferences，安卓端使用 SharedPreferences
 */
public interface PlatformPreferences {
    
    /**
     * 存储字符串
     */
    void putString(String key, String value);
    
    /**
     * 获取字符串
     */
    String getString(String key, String defaultValue);
    
    /**
     * 存储整数
     */
    void putInt(String key, int value);
    
    /**
     * 获取整数
     */
    int getInt(String key, int defaultValue);
    
    /**
     * 存储布尔值
     */
    void putBoolean(String key, boolean value);
    
    /**
     * 获取布尔值
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * 存储长整数
     */
    void putLong(String key, long value);
    
    /**
     * 获取长整数
     */
    long getLong(String key, long defaultValue);
    
    /**
     * 删除键值
     */
    void remove(String key);
    
    /**
     * 清空所有设置
     */
    void clear();
    
    /**
     * 同步保存（确保写入持久化）
     */
    void flush();
}
