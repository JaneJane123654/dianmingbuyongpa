package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.utils.Validator;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
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
 * 用户配置管理器 (Preferences Manager)
 *
 * <p>
 * 基于 Java Preferences API 实现用户个性化设置的持久化存储。
 * 核心功能包括配置项的加载、保存，以及对敏感信息（如 AI API Token）的 AES-GCM 加密保护。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class PreferencesManager {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesManager.class);

    private static final String NODE_NAME = "com.classroomassistant";

    private static final String KEY_KEYWORDS = "user.keywords";
    private static final String KEY_KWS_TRIGGER_THRESHOLD = "speech.kws.triggerThreshold";
    private static final String KEY_AUDIO_LOOKBACK_SECONDS = "audio.lookbackSeconds";
    private static final String KEY_VAD_ENABLED = "vad.enabled";
    private static final String KEY_VAD_QUIET_THRESHOLD_SECONDS = "vad.quietThresholdSeconds";
    private static final String KEY_VAD_QUIET_ALERT_MODE = "vad.quietAlertMode";
    private static final String KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED = "vad.quietAutoLookbackEnabled";
    private static final String KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS = "vad.quietAutoLookbackExtraSeconds";
    private static final String KEY_AI_PROVIDER = "ai.provider";
    private static final String KEY_AI_MODEL_NAME = "ai.modelName";
    private static final String KEY_AI_TOKEN_ENCRYPTED = "ai.token.encrypted";
    private static final String KEY_AI_SECRET_ENCRYPTED = "ai.secret.encrypted";
    private static final String KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled";
    private static final String KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays";
    private static final String KEY_SPEECH_API_KEY_ENCRYPTED = "speech.apiKey.encrypted";
    private static final String KEY_ASR_LOCAL_ENABLED = "speech.asr.local.enabled";
    private static final String KEY_ASR_LOCAL_MODEL_ID = "speech.asr.local.model.id";
    private static final String KEY_ASR_CLOUD_WHISPER_ENABLED = "speech.asr.cloud.whisper.enabled";
    private static final String KEY_KWS_SELECTED_MODELS = "kws.selectedModels";
    private static final String KEY_KWS_CURRENT_MODEL = "kws.currentModel";
    private static final String KEY_ASR_SELECTED = "asr.selected";
    private static final String KEY_VAD_SELECTED = "vad.selected";
    private static final String KEY_WAKE_ALERT_MODE = "speech.kws.wakeAlertMode";
    private static final String KEY_LOG_MODE = "developer.log.mode";
    private static final String KEY_LOG_SHOW_DIAGNOSTIC = "developer.log.showDiagnostic";
    private static final String KEY_LOG_SHOW_AUDIO_DEVICE = "developer.log.showAudioDevice";
    private static final String KEY_LOG_SHOW_GAIN_ACTIVITY = "developer.log.showGainActivity";
    private static final String KEY_LOG_SHOW_TTS_SELF_TEST = "developer.log.showTtsSelfTest";
    private static final String KEY_LOG_SHOW_HEARTBEAT = "developer.log.showHeartbeat";
    private static final String KEY_TTS_SELF_TEST_ENABLED = "developer.tts.selfTest.enabled";
    private static final String KEY_BACKGROUND_KEEPALIVE_ENABLED = "listening.backgroundKeepAliveEnabled";

    private static final String KEY_CRYPTO_SALT = "crypto.salt";

    private final Preferences preferences;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 初始化配置管理器，绑定到指定的用户节点
     */
    public PreferencesManager() {
        this.preferences = Preferences.userRoot().node(NODE_NAME);
    }

    /**
     * 从持久化存储中加载所有用户偏好设置
     *
     * @return 包含所有设置项的 {@link UserPreferences} 对象
     */
    public UserPreferences load() {
        String keywords = preferences.get(KEY_KEYWORDS, "");
        float kwsThreshold = (preferences.getInt(KEY_KWS_TRIGGER_THRESHOLD, 25) / 100f);
        kwsThreshold = Math.max(0.05f, Math.min(0.8f, kwsThreshold));
        int lookbackSeconds = preferences.getInt(KEY_AUDIO_LOOKBACK_SECONDS, 15);
        lookbackSeconds = Math.max(8, Math.min(120, lookbackSeconds));
        boolean vadEnabled = preferences.getBoolean(KEY_VAD_ENABLED, true);
        int quietThresholdSeconds = preferences.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5);
        quietThresholdSeconds = Math.max(3, Math.min(30, quietThresholdSeconds));
        String quietAlertModeRaw = preferences.get(KEY_VAD_QUIET_ALERT_MODE, "NOTIFICATION_ONLY");
        String quietAlertMode = "SOUND".equals(quietAlertModeRaw) ? "SOUND" : "NOTIFICATION_ONLY";
        boolean quietAutoLookbackEnabled = preferences.getBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, true);
        int quietAutoLookbackExtraSeconds = preferences.getInt(KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS, 8);
        quietAutoLookbackExtraSeconds = Math.max(1, Math.min(60, quietAutoLookbackExtraSeconds));
        boolean recordingSaveEnabled = preferences.getBoolean(KEY_RECORDING_SAVE_ENABLED, false);
        int recordingRetentionDays = preferences.getInt(KEY_RECORDING_RETENTION_DAYS, 7);
        recordingRetentionDays = Math.max(0, Math.min(30, recordingRetentionDays));

        LLMConfig.ModelType modelType;
        try {
            modelType = LLMConfig.ModelType
                    .valueOf(preferences.get(KEY_AI_PROVIDER, LLMConfig.ModelType.QIANFAN.name()));
        } catch (Exception e) {
            modelType = LLMConfig.ModelType.QIANFAN;
        }
        String modelName = preferences.get(KEY_AI_MODEL_NAME, "");
        boolean localAsrEnabled = preferences.getBoolean(KEY_ASR_LOCAL_ENABLED, true);
        String localAsrModelId = preferences.get(KEY_ASR_LOCAL_MODEL_ID, "");
        boolean cloudWhisperEnabled = preferences.getBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, false);
        Set<String> selectedKwsModels = parseModelIds(preferences.get(KEY_KWS_SELECTED_MODELS, ""));
        String currentKwsModel = preferences.get(KEY_KWS_CURRENT_MODEL, "");
        boolean asrSelected = preferences.getBoolean(KEY_ASR_SELECTED, true);
        boolean vadSelected = preferences.getBoolean(KEY_VAD_SELECTED, true);
        String wakeAlertModeRaw = preferences.get(KEY_WAKE_ALERT_MODE, "NOTIFICATION_ONLY");
        String wakeAlertMode = "SOUND".equals(wakeAlertModeRaw) ? "SOUND" : "NOTIFICATION_ONLY";
        String logModeRaw = preferences.get(KEY_LOG_MODE, "SIMPLE");
        String logMode = "FULL".equals(logModeRaw) ? "FULL" : "SIMPLE";
        boolean showDiagnosticLogs = preferences.getBoolean(KEY_LOG_SHOW_DIAGNOSTIC, false);
        boolean showAudioDeviceLogs = preferences.getBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, false);
        boolean showGainActivityLogs = preferences.getBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, false);
        boolean showTtsSelfTestLogs = preferences.getBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, false);
        boolean showHeartbeatLogs = preferences.getBoolean(KEY_LOG_SHOW_HEARTBEAT, false);
        boolean ttsSelfTestEnabled = preferences.getBoolean(KEY_TTS_SELF_TEST_ENABLED, false);
        boolean backgroundKeepAliveEnabled = preferences.getBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, true);

        return UserPreferences.builder()
                .keywords(Validator.normalizeKeywords(keywords))
                .kwsThreshold(kwsThreshold)
                .audioLookbackSeconds(lookbackSeconds)
                .vadEnabled(vadEnabled)
                .vadQuietThresholdSeconds(quietThresholdSeconds)
                .quietAlertMode(quietAlertMode)
                .quietAutoLookbackEnabled(quietAutoLookbackEnabled)
                .quietAutoLookbackExtraSeconds(quietAutoLookbackExtraSeconds)
                .aiModelType(modelType)
                .aiModelName(modelName)
                .recordingSaveEnabled(recordingSaveEnabled)
                .recordingRetentionDays(recordingRetentionDays)
                .aiTokenPlainText("")
                .aiSecretKey("")
                .speechApiKey("")
                .localAsrEnabled(localAsrEnabled)
                .localAsrModelId(localAsrModelId)
                .cloudWhisperEnabled(cloudWhisperEnabled)
                .selectedKwsModelIds(selectedKwsModels)
                .currentKwsModelId(currentKwsModel)
                .asrModelSelected(asrSelected)
                .vadModelSelected(vadSelected)
                .wakeAlertMode(wakeAlertMode)
                .logMode(logMode)
                .showDiagnosticLogs(showDiagnosticLogs)
                .showAudioDeviceLogs(showAudioDeviceLogs)
                .showGainActivityLogs(showGainActivityLogs)
                .showTtsSelfTestLogs(showTtsSelfTestLogs)
                .showHeartbeatLogs(showHeartbeatLogs)
                .ttsSelfTestEnabled(ttsSelfTestEnabled)
                .backgroundKeepAliveEnabled(backgroundKeepAliveEnabled)
                .build();
    }

    /**
     * 将当前用户偏好设置保存到持久化存储中
     *
     * @param prefs 包含要保存设置的 {@link UserPreferences} 对象
     */
    public void save(UserPreferences prefs) {
        preferences.put(KEY_KEYWORDS, Validator.normalizeKeywords(prefs.getKeywords()));
        int kwsThresholdPercent = (int) (Math.max(0.05f, Math.min(0.8f, prefs.getKwsThreshold())) * 100f);
        preferences.putInt(KEY_KWS_TRIGGER_THRESHOLD, kwsThresholdPercent);
        preferences.putInt(KEY_AUDIO_LOOKBACK_SECONDS, prefs.getAudioLookbackSeconds());
        preferences.putBoolean(KEY_VAD_ENABLED, prefs.isVadEnabled());
        preferences.putInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, prefs.getVadQuietThresholdSeconds());
        preferences.put(KEY_VAD_QUIET_ALERT_MODE,
                "SOUND".equals(prefs.getQuietAlertMode()) ? "SOUND" : "NOTIFICATION_ONLY");
        preferences.putBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, prefs.isQuietAutoLookbackEnabled());
        preferences.putInt(KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS,
                Math.max(1, Math.min(60, prefs.getQuietAutoLookbackExtraSeconds())));
        preferences.put(KEY_AI_PROVIDER, prefs.getAiModelType().name());
        preferences.put(KEY_AI_MODEL_NAME, prefs.getAiModelName());
        preferences.putBoolean(KEY_RECORDING_SAVE_ENABLED, prefs.isRecordingSaveEnabled());
        preferences.putInt(KEY_RECORDING_RETENTION_DAYS, prefs.getRecordingRetentionDays());
        preferences.putBoolean(KEY_ASR_LOCAL_ENABLED, prefs.isLocalAsrEnabled());
        preferences.put(KEY_ASR_LOCAL_MODEL_ID, prefs.getLocalAsrModelId());
        preferences.putBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, prefs.isCloudWhisperEnabled());
        preferences.put(KEY_KWS_SELECTED_MODELS, joinModelIds(prefs.getSelectedKwsModelIds()));
        preferences.put(KEY_KWS_CURRENT_MODEL, prefs.getCurrentKwsModelId());
        preferences.putBoolean(KEY_ASR_SELECTED, prefs.isAsrModelSelected());
        preferences.putBoolean(KEY_VAD_SELECTED, prefs.isVadModelSelected());
        preferences.put(KEY_WAKE_ALERT_MODE, "SOUND".equals(prefs.getWakeAlertMode()) ? "SOUND" : "NOTIFICATION_ONLY");
        preferences.put(KEY_LOG_MODE, "FULL".equals(prefs.getLogMode()) ? "FULL" : "SIMPLE");
        preferences.putBoolean(KEY_LOG_SHOW_DIAGNOSTIC, prefs.isShowDiagnosticLogs());
        preferences.putBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, prefs.isShowAudioDeviceLogs());
        preferences.putBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, prefs.isShowGainActivityLogs());
        preferences.putBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, prefs.isShowTtsSelfTestLogs());
        preferences.putBoolean(KEY_LOG_SHOW_HEARTBEAT, prefs.isShowHeartbeatLogs());
        preferences.putBoolean(KEY_TTS_SELF_TEST_ENABLED, prefs.isTtsSelfTestEnabled());
        preferences.putBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, prefs.isBackgroundKeepAliveEnabled());

        String token = prefs.getAiTokenPlainText();
        if (token != null && !token.isBlank()) {
            String encrypted = encryptToken(token);
            preferences.put(KEY_AI_TOKEN_ENCRYPTED, encrypted);
        }

        String secretKey = prefs.getAiSecretKey();
        if (secretKey != null && !secretKey.isBlank()) {
            String encrypted = encryptToken(secretKey);
            preferences.put(KEY_AI_SECRET_ENCRYPTED, encrypted);
        }

        String speechKey = prefs.getSpeechApiKey();
        if (speechKey != null && !speechKey.isBlank()) {
            String encrypted = encryptToken(speechKey);
            preferences.put(KEY_SPEECH_API_KEY_ENCRYPTED, encrypted);
        }
    }

    private Set<String> parseModelIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(result::add);
        return result;
    }

    private String joinModelIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(id.trim());
        }
        return builder.toString();
    }

    /**
     * 读取并解密 AI 服务的 API Token
     *
     * @return 解密后的 Token 明文。如果不存在或解密失败，则返回空字符串。
     */
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

    /**
     * 读取并解密语音识别 API Key
     *
     * @return 解密后的 Key 明文。如果不存在或解密失败，则返回空字符串。
     */
    public String loadSpeechApiKey() {
        String encrypted = preferences.get(KEY_SPEECH_API_KEY_ENCRYPTED, "");
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        try {
            return decryptToken(encrypted);
        } catch (Exception e) {
            logger.warn("解密 Speech API Key 失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 读取并解密 AI 平台 Secret Key（如千帆双密钥）
     *
     * @return 解密后的 Secret Key 明文。如果不存在或解密失败，则返回空字符串。
     */
    public String loadAiSecretKey() {
        String encrypted = preferences.get(KEY_AI_SECRET_ENCRYPTED, "");
        if (encrypted == null || encrypted.isBlank()) {
            return "";
        }
        try {
            return decryptToken(encrypted);
        } catch (Exception e) {
            logger.warn("解密 Secret Key 失败: {}", e.getMessage());
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
