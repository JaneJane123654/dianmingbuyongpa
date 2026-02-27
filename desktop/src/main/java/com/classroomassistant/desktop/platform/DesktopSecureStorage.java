package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.platform.PlatformSecureStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;

/**
 * 桌面端安全存储实现
 * 使用 AES-GCM 加密
 */
public class DesktopSecureStorage implements PlatformSecureStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(DesktopSecureStorage.class);
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String KEY_PREF = "secure_key";
    
    private final Preferences prefs;
    private final SecretKey secretKey;
    
    public DesktopSecureStorage() {
        this.prefs = Preferences.userRoot().node("classroom-assistant/secure");
        this.secretKey = getOrCreateSecretKey();
    }
    
    private SecretKey getOrCreateSecretKey() {
        try {
            String encodedKey = prefs.get(KEY_PREF, null);
            if (encodedKey != null) {
                byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
                return new SecretKeySpec(decodedKey, "AES");
            } else {
                // 生成新密钥
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                SecretKey newKey = keyGen.generateKey();
                
                // 保存密钥
                String encoded = Base64.getEncoder().encodeToString(newKey.getEncoded());
                prefs.put(KEY_PREF, encoded);
                prefs.flush();
                
                return newKey;
            }
        } catch (Exception e) {
            logger.error("初始化加密密钥失败", e);
            throw new RuntimeException("初始化加密密钥失败", e);
        }
    }
    
    @Override
    public void storeSecure(String key, String value) {
        if (value == null || value.isEmpty()) {
            prefs.remove(key);
            return;
        }
        
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            
            // IV + 密文
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            prefs.put(key, Base64.getEncoder().encodeToString(combined));
            prefs.flush();
            
        } catch (Exception e) {
            logger.error("加密存储失败: {}", key, e);
            throw new RuntimeException("加密存储失败", e);
        }
    }
    
    @Override
    public String retrieveSecure(String key) {
        String encoded = prefs.get(key, null);
        if (encoded == null) {
            return null;
        }
        
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("解密失败: {}", key, e);
            return null;
        }
    }
    
    @Override
    public void deleteSecure(String key) {
        prefs.remove(key);
        try {
            prefs.flush();
        } catch (Exception e) {
            logger.error("删除安全存储失败: {}", key, e);
        }
    }
    
    @Override
    public boolean hasSecure(String key) {
        return prefs.get(key, null) != null;
    }
}
