package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.platform.PlatformPreferences;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 桌面端偏好设置存储实现
 * 使用 java.util.prefs.Preferences
 */
public class DesktopPreferences implements PlatformPreferences {
    
    private final Preferences prefs;
    
    public DesktopPreferences() {
        this.prefs = Preferences.userRoot().node("classroom-assistant");
    }
    
    @Override
    public void putString(String key, String value) {
        if (value != null) {
            prefs.put(key, value);
        } else {
            prefs.remove(key);
        }
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }
    
    @Override
    public void putInt(String key, int value) {
        prefs.putInt(key, value);
    }
    
    @Override
    public int getInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }
    
    @Override
    public void putBoolean(String key, boolean value) {
        prefs.putBoolean(key, value);
    }
    
    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }
    
    @Override
    public void putLong(String key, long value) {
        prefs.putLong(key, value);
    }
    
    @Override
    public long getLong(String key, long defaultValue) {
        return prefs.getLong(key, defaultValue);
    }
    
    @Override
    public void remove(String key) {
        prefs.remove(key);
    }
    
    @Override
    public void clear() {
        try {
            prefs.clear();
        } catch (BackingStoreException e) {
            throw new RuntimeException("清除偏好设置失败", e);
        }
    }
    
    @Override
    public void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException("保存偏好设置失败", e);
        }
    }
}
