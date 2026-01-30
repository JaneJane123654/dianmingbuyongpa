package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.utils.Validator;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户配置管理器（Java Preferences）
 *
 * <p>用于存储与读取用户配置；敏感信息（Token）采用 AES-GCM 加密保存。
 */
public class PreferencesManager {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesManager.class);

    private static final String NODE_NAME = "com.classroomassistant";

    private static final String KEY_KEYWORDS = "user.keywords";
    private static final String KEY_AUDIO_LOOKBACK_SECONDS = "audio.lookbackSeconds";
    private static final String KEY_VAD_ENABLED = "vad.enabled";
    private static final String KEY_VAD_QUIET_THRESHOLD_SECONDS = "vad.quietThresholdSeconds";
    private static final String KEY_AI_PROVIDER = "ai.provider";
    private static final String KEY_AI_MODEL_NAME = "ai.modelName";
    private static final String KEY_AI_TOKEN_ENCRYPTED = "ai.token.encrypted";
    private static final String KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled";
    private static final String KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays";

    private static final String KEY_CRYPTO_SALT = "crypto.salt";

    private final Preferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    public PreferencesManager() {
        this.preferences = Preferences.userRoot().node(NODE_NAME);
    }

    public UserPreferences load() {
        String keywords = preferences.get(KEY_KEYWORDS, "");
        int lookbackSeconds = preferences.getInt(KEY_AUDIO_LOOKBACK_SECONDS, 240);
        boolean vadEnabled = preferences.getBoolean(KEY_VAD_ENABLED, true);
        int quietThresholdSeconds = preferences.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5);
        boolean recordingSaveEnabled = preferences.getBoolean(KEY_RECORDING_SAVE_ENABLED, false);
        int recordingRetentionDays = preferences.getInt(KEY_RECORDING_RETENTION_DAYS, 7);

        LLMConfig.ModelType modelType;
        try {
            modelType = LLMConfig.ModelType.valueOf(preferences.get(KEY_AI_PROVIDER, LLMConfig.ModelType.QIANFAN.name()));
        } catch (Exception e) {
            modelType = LLMConfig.ModelType.QIANFAN;
        }
        String modelName = preferences.get(KEY_AI_MODEL_NAME, "");

        Validator.requireRange(lookbackSeconds, 1, 300, "回溯秒数");
        Validator.requireRange(quietThresholdSeconds, 3, 30, "安静阈值（秒）");
        Validator.requireRange(recordingRetentionDays, 0, 30, "录音保留天数");

        return UserPreferences.builder()
            .keywords(Validator.normalizeKeywords(keywords))
            .audioLookbackSeconds(lookbackSeconds)
            .vadEnabled(vadEnabled)
            .vadQuietThresholdSeconds(quietThresholdSeconds)
            .aiModelType(modelType)
            .aiModelName(modelName)
                .recordingSaveEnabled(recordingSaveEnabled)
                .recordingRetentionDays(recordingRetentionDays)
            .aiTokenPlainText("")
            .build();
    }

    public void save(UserPreferences prefs) {
        preferences.put(KEY_KEYWORDS, Validator.normalizeKeywords(prefs.getKeywords()));
        preferences.putInt(KEY_AUDIO_LOOKBACK_SECONDS, prefs.getAudioLookbackSeconds());
        preferences.putBoolean(KEY_VAD_ENABLED, prefs.isVadEnabled());
        preferences.putInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, prefs.getVadQuietThresholdSeconds());
        preferences.put(KEY_AI_PROVIDER, prefs.getAiModelType().name());
        preferences.put(KEY_AI_MODEL_NAME, prefs.getAiModelName());
        preferences.putBoolean(KEY_RECORDING_SAVE_ENABLED, prefs.isRecordingSaveEnabled());
        preferences.putInt(KEY_RECORDING_RETENTION_DAYS, prefs.getRecordingRetentionDays());

        String token = prefs.getAiTokenPlainText();
        if (token != null && !token.isBlank()) {
            String encrypted = encryptToken(token);
            preferences.put(KEY_AI_TOKEN_ENCRYPTED, encrypted);
        }
    }

    public String loadAiTokenPlainText() {
        String encrypted = preferences.get(KEY_AI_TOKEN_ENCRYPTED, "");
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        try {
            return decryptToken(encrypted);
        } catch (Exception e) {
            logger.warn("解密 Token 失败: {}", e.getMessage());
            return "";
        }
    }

    private String encryptToken(String plainText) {
        try {
            SecretKey key = deriveKey();
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("加密 Token 失败: " + e.getMessage(), e);
        }
    }

    private String decryptToken(String encryptedBase64) throws GeneralSecurityException {
        byte[] payload = Base64.getDecoder().decode(encryptedBase64);
        if (payload.length < 13) {
            throw new GeneralSecurityException("加密数据格式非法");
        }
        byte[] iv = new byte[12];
        byte[] cipherText = new byte[payload.length - 12];
        System.arraycopy(payload, 0, iv, 0, 12);
        System.arraycopy(payload, 12, cipherText, 0, cipherText.length);

        SecretKey key = deriveKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private SecretKey deriveKey() throws GeneralSecurityException {
        byte[] salt = loadOrCreateSalt();
        String passwordMaterial = System.getProperty("user.name", "") + ":" + System.getProperty("user.home", "");
        PBEKeySpec spec = new PBEKeySpec(passwordMaterial.toCharArray(), salt, 200_000, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] loadOrCreateSalt() {
        String saltBase64 = preferences.get(KEY_CRYPTO_SALT, "");
        if (saltBase64 != null && !saltBase64.isBlank()) {
            try {
                return Base64.getDecoder().decode(saltBase64);
            } catch (IllegalArgumentException ignored) {
            }
        }

        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        preferences.put(KEY_CRYPTO_SALT, Base64.getEncoder().encodeToString(salt));
        return salt;
    }
}

