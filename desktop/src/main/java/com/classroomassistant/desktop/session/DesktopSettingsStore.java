package com.classroomassistant.desktop.session;

import com.classroomassistant.core.platform.PlatformPreferences;
import com.classroomassistant.core.platform.PlatformSecureStorage;

/**
 * 桌面端设置读取器。
 *
 * <p>使用与 Android 一致的 key 命名，保证跨平台配置语义一致。</p>
 */
public class DesktopSettingsStore {

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
    private static final String KEY_AI_BASE_URL = "ai.baseUrl";
    private static final String KEY_RECORDING_SAVE_ENABLED = "recording.saveEnabled";
    private static final String KEY_RECORDING_RETENTION_DAYS = "recording.retentionDays";
    private static final String KEY_ASR_LOCAL_ENABLED = "speech.asr.local.enabled";
    private static final String KEY_ASR_LOCAL_MODEL_ID = "speech.asr.local.modelId";
    private static final String KEY_ASR_CLOUD_WHISPER_ENABLED = "speech.asr.cloud.whisper.enabled";
    private static final String KEY_KWS_MODEL_ID = "speech.kws.modelId";
    private static final String KEY_CUSTOM_MODEL_ENABLED = "model.custom.enabled";
    private static final String KEY_CUSTOM_KWS_MODEL_URL = "model.custom.kws.url";
    private static final String KEY_CUSTOM_ASR_MODEL_URL = "model.custom.asr.url";
    private static final String KEY_WAKE_ALERT_MODE = "speech.kws.wakeAlertMode";
    private static final String KEY_LOG_MODE = "developer.log.mode";
    private static final String KEY_LOG_SHOW_DIAGNOSTIC = "developer.log.showDiagnostic";
    private static final String KEY_LOG_SHOW_AUDIO_DEVICE = "developer.log.showAudioDevice";
    private static final String KEY_LOG_SHOW_GAIN_ACTIVITY = "developer.log.showGainActivity";
    private static final String KEY_LOG_SHOW_TTS_SELF_TEST = "developer.log.showTtsSelfTest";
    private static final String KEY_LOG_SHOW_HEARTBEAT = "developer.log.showHeartbeat";
    private static final String KEY_TTS_SELF_TEST_ENABLED = "developer.tts.selfTest.enabled";
    private static final String KEY_BACKGROUND_KEEPALIVE_ENABLED = "listening.backgroundKeepAliveEnabled";

    private static final String SECURE_AI_TOKEN = "ai.token";
    private static final String SECURE_AI_SECRET = "ai.secret";
    private static final String SECURE_SPEECH_API_KEY = "speech.apiKey";

    private final PlatformPreferences preferences;
    private final PlatformSecureStorage secureStorage;

    public DesktopSettingsStore(PlatformPreferences preferences, PlatformSecureStorage secureStorage) {
        this.preferences = preferences;
        this.secureStorage = secureStorage;
    }

    public DesktopSettingsSnapshot load() {
        String wakeAlertMode = normalizeMode(preferences.getString(KEY_WAKE_ALERT_MODE, "NOTIFICATION_ONLY"));
        String quietAlertMode = normalizeMode(preferences.getString(KEY_VAD_QUIET_ALERT_MODE, "NOTIFICATION_ONLY"));
        String logMode = normalizeLogMode(preferences.getString(KEY_LOG_MODE, "SIMPLE"));

        return DesktopSettingsSnapshot.builder()
                .keywords(preferences.getString(KEY_KEYWORDS, ""))
                .kwsThreshold(preferences.getInt(KEY_KWS_TRIGGER_THRESHOLD, 5) / 100f)
                .lookbackSeconds(preferences.getInt(KEY_AUDIO_LOOKBACK_SECONDS, 15))
                .vadEnabled(preferences.getBoolean(KEY_VAD_ENABLED, true))
                .quietThresholdSeconds(preferences.getInt(KEY_VAD_QUIET_THRESHOLD_SECONDS, 5))
                .quietAlertMode(quietAlertMode)
                .quietAutoLookbackEnabled(preferences.getBoolean(KEY_VAD_QUIET_AUTO_LOOKBACK_ENABLED, true))
                .quietAutoLookbackExtraSeconds(preferences.getInt(KEY_VAD_QUIET_AUTO_LOOKBACK_EXTRA_SECONDS, 8))
                .recordingSaveEnabled(preferences.getBoolean(KEY_RECORDING_SAVE_ENABLED, false))
                .retentionDays(preferences.getInt(KEY_RECORDING_RETENTION_DAYS, 7))
                .aiProvider(preferences.getString(KEY_AI_PROVIDER, "OPENAI_COMPATIBLE"))
                .aiModelName(preferences.getString(KEY_AI_MODEL_NAME, ""))
                .aiBaseUrl(preferences.getString(KEY_AI_BASE_URL, ""))
                .aiToken(safeSecureValue(SECURE_AI_TOKEN))
                .aiSecretKey(safeSecureValue(SECURE_AI_SECRET))
                .speechApiKey(safeSecureValue(SECURE_SPEECH_API_KEY))
                .localAsrEnabled(preferences.getBoolean(KEY_ASR_LOCAL_ENABLED, true))
                .localAsrModelId(preferences.getString(
                        KEY_ASR_LOCAL_MODEL_ID,
                        "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16"))
                .cloudWhisperEnabled(preferences.getBoolean(KEY_ASR_CLOUD_WHISPER_ENABLED, false))
                .currentKwsModelId(preferences.getString(KEY_KWS_MODEL_ID, ""))
                .customModelEnabled(preferences.getBoolean(KEY_CUSTOM_MODEL_ENABLED, false))
                .customKwsModelUrl(preferences.getString(KEY_CUSTOM_KWS_MODEL_URL, ""))
                .customAsrModelUrl(preferences.getString(KEY_CUSTOM_ASR_MODEL_URL, ""))
                .wakeAlertMode(wakeAlertMode)
                .logMode(logMode)
                .showDiagnosticLogs(preferences.getBoolean(KEY_LOG_SHOW_DIAGNOSTIC, false))
                .showAudioDeviceLogs(preferences.getBoolean(KEY_LOG_SHOW_AUDIO_DEVICE, false))
                .showGainActivityLogs(preferences.getBoolean(KEY_LOG_SHOW_GAIN_ACTIVITY, false))
                .showTtsSelfTestLogs(preferences.getBoolean(KEY_LOG_SHOW_TTS_SELF_TEST, false))
                .showHeartbeatLogs(preferences.getBoolean(KEY_LOG_SHOW_HEARTBEAT, false))
                .ttsSelfTestEnabled(preferences.getBoolean(KEY_TTS_SELF_TEST_ENABLED, false))
                .backgroundKeepAliveEnabled(preferences.getBoolean(KEY_BACKGROUND_KEEPALIVE_ENABLED, true))
                .build();
    }

    private String safeSecureValue(String key) {
        String value = secureStorage.retrieveSecure(key);
        return value == null ? "" : value.trim();
    }

    private String normalizeMode(String raw) {
        if ("SOUND".equalsIgnoreCase(raw)) {
            return "SOUND";
        }
        return "NOTIFICATION_ONLY";
    }

    private String normalizeLogMode(String raw) {
        if ("FULL".equalsIgnoreCase(raw)) {
            return "FULL";
        }
        return "SIMPLE";
    }
}
