package com.classroomassistant.core.storage;

import com.classroomassistant.core.platform.PlatformPreferences;
import com.classroomassistant.core.platform.PlatformSecureStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心偏好管理器（平台无关）
 * 
 * 通过 PlatformPreferences 和 PlatformSecureStorage 接口实现跨平台存储
 */
public class CorePreferencesManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CorePreferencesManager.class);
    
    // 键名常量
    private static final String KEY_LLM_MODEL_TYPE = "llm.model.type";
    private static final String KEY_LLM_BASE_URL = "llm.base.url";
    private static final String KEY_LLM_MODEL_NAME = "llm.model.name";
    private static final String KEY_WAKE_WORD = "speech.wake.word";
    private static final String KEY_SILENCE_TIMEOUT = "speech.silence.timeout";
    private static final String KEY_SILENCE_THRESHOLD = "speech.silence.threshold";
    private static final String KEY_AUTO_START = "app.auto.start";
    private static final String KEY_LANGUAGE = "app.language";
    private static final String KEY_SPEECH_API_URL = "speech.api.url";
    
    // 敏感数据键名（存储在安全存储中）
    private static final String KEY_LLM_API_KEY = "llm.api.key";
    private static final String KEY_LLM_SECRET_KEY = "llm.secret.key";
    private static final String KEY_SPEECH_API_KEY = "speech.api.key";
    
    private final PlatformPreferences preferences;
    private final PlatformSecureStorage secureStorage;
    
    public CorePreferencesManager(PlatformPreferences preferences, 
                                   PlatformSecureStorage secureStorage) {
        this.preferences = preferences;
        this.secureStorage = secureStorage;
    }
    
    /**
     * 加载用户偏好设置
     */
    public UserPreferences load() {
        logger.debug("加载用户偏好设置...");
        
        return UserPreferences.builder()
            .aiModelType(preferences.getString(KEY_LLM_MODEL_TYPE, "QIANFAN"))
            .aiTokenPlainText(secureStorage.retrieveSecure(KEY_LLM_API_KEY))
            .aiSecretKey(secureStorage.retrieveSecure(KEY_LLM_SECRET_KEY))
            .aiModelName(preferences.getString(KEY_LLM_MODEL_NAME, ""))
            .speechApiKey(secureStorage.retrieveSecure(KEY_SPEECH_API_KEY))
            .keywords(preferences.getString(KEY_WAKE_WORD, "小助手"))
            .vadQuietThresholdSeconds(preferences.getInt(KEY_SILENCE_TIMEOUT, 2000) / 1000)
            .autoStart(preferences.getBoolean(KEY_AUTO_START, false))
            .language(preferences.getString(KEY_LANGUAGE, "zh-CN"))
            .build();
    }
    
    /**
     * 保存用户偏好设置
     */
    public void save(UserPreferences prefs) {
        logger.debug("保存用户偏好设置...");
        
        // 普通设置
        preferences.putString(KEY_LLM_MODEL_TYPE, prefs.getAiModelType());
        preferences.putString(KEY_LLM_MODEL_NAME, prefs.getAiModelName());
        preferences.putString(KEY_WAKE_WORD, prefs.getKeywords());
        preferences.putInt(KEY_SILENCE_TIMEOUT, prefs.getVadQuietThresholdSeconds() * 1000);
        preferences.putBoolean(KEY_AUTO_START, prefs.isAutoStart());
        preferences.putString(KEY_LANGUAGE, prefs.getLanguage());
        
        // 敏感数据（加密存储）
        if (prefs.getAiTokenPlainText() != null && !prefs.getAiTokenPlainText().isEmpty()) {
            secureStorage.storeSecure(KEY_LLM_API_KEY, prefs.getAiTokenPlainText());
        }
        if (prefs.getAiSecretKey() != null && !prefs.getAiSecretKey().isEmpty()) {
            secureStorage.storeSecure(KEY_LLM_SECRET_KEY, prefs.getAiSecretKey());
        }
        if (prefs.getSpeechApiKey() != null && !prefs.getSpeechApiKey().isEmpty()) {
            secureStorage.storeSecure(KEY_SPEECH_API_KEY, prefs.getSpeechApiKey());
        }
        
        preferences.flush();
        logger.info("用户偏好设置已保存");
    }
    
    /**
     * 重置为默认设置
     */
    public void reset() {
        logger.info("重置用户偏好设置...");
        preferences.clear();
        secureStorage.deleteSecure(KEY_LLM_API_KEY);
        secureStorage.deleteSecure(KEY_LLM_SECRET_KEY);
        secureStorage.deleteSecure(KEY_SPEECH_API_KEY);
        preferences.flush();
    }
}
