package com.classroomassistant.core.platform;

/**
 * 平台加密存储接口
 * 桌面端使用 AES-GCM，安卓端使用 Android Keystore
 */
public interface PlatformSecureStorage {
    
    /**
     * 安全存储敏感数据（如 API Key）
     * @param key 存储键名
     * @param value 敏感值（明文）
     */
    void storeSecure(String key, String value);
    
    /**
     * 读取安全存储的数据
     * @param key 存储键名
     * @return 解密后的明文，不存在则返回 null
     */
    String retrieveSecure(String key);
    
    /**
     * 删除安全存储的数据
     * @param key 存储键名
     */
    void deleteSecure(String key);
    
    /**
     * 检查是否存在
     */
    boolean hasSecure(String key);
}
