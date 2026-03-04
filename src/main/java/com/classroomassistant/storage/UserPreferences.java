package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 用户配置信息 (User Preferences Value Object)
 *
 * <p>
 * 本类是一个不可变的值对象（Value Object），用于承载用户在界面上设置的各项参数。
 * 包含了唤醒词、静音检测配置、音频回溯时长、录音保存策略以及 AI 模型配置等信息。
 *
 * <p>
 * 通常由 {@link PreferencesManager} 构造并提供给业务逻辑使用。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class UserPreferences {

    private final String keywords;
    private final float kwsThreshold;
    private final boolean vadEnabled;
    private final int vadQuietThresholdSeconds;
    private final String quietAlertMode;
    private final boolean quietAutoLookbackEnabled;
    private final int quietAutoLookbackExtraSeconds;
    private final int audioLookbackSeconds;
    private final boolean recordingSaveEnabled;
    private final int recordingRetentionDays;
    private final LLMConfig.ModelType aiModelType;
    private final String aiModelName;
    private final String aiTokenPlainText;
    private final String aiSecretKey;
    private final String speechApiKey;
    private final boolean localAsrEnabled;
    private final String localAsrModelId;
    private final boolean cloudWhisperEnabled;
    private final Set<String> selectedKwsModelIds;
    private final String currentKwsModelId;
    private final boolean asrModelSelected;
    private final boolean vadModelSelected;
    private final String wakeAlertMode;
    private final String logMode;
    private final boolean showDiagnosticLogs;
    private final boolean showAudioDeviceLogs;
    private final boolean showGainActivityLogs;
    private final boolean showTtsSelfTestLogs;
    private final boolean showHeartbeatLogs;
    private final boolean ttsSelfTestEnabled;
    private final boolean backgroundKeepAliveEnabled;

    private UserPreferences(
            String keywords,
            float kwsThreshold,
            boolean vadEnabled,
            int vadQuietThresholdSeconds,
            String quietAlertMode,
            boolean quietAutoLookbackEnabled,
            int quietAutoLookbackExtraSeconds,
            int audioLookbackSeconds,
            boolean recordingSaveEnabled,
            int recordingRetentionDays,
            LLMConfig.ModelType aiModelType,
            String aiModelName,
            String aiTokenPlainText,
            String aiSecretKey,
            String speechApiKey,
            boolean localAsrEnabled,
            String localAsrModelId,
            boolean cloudWhisperEnabled,
            Set<String> selectedKwsModelIds,
            String currentKwsModelId,
            boolean asrModelSelected,
            boolean vadModelSelected,
            String wakeAlertMode,
            String logMode,
            boolean showDiagnosticLogs,
            boolean showAudioDeviceLogs,
            boolean showGainActivityLogs,
            boolean showTtsSelfTestLogs,
            boolean showHeartbeatLogs,
            boolean ttsSelfTestEnabled,
            boolean backgroundKeepAliveEnabled) {
        this.keywords = keywords;
        this.kwsThreshold = kwsThreshold;
        this.vadEnabled = vadEnabled;
        this.vadQuietThresholdSeconds = vadQuietThresholdSeconds;
        this.quietAlertMode = quietAlertMode == null ? "NOTIFICATION_ONLY" : quietAlertMode;
        this.quietAutoLookbackEnabled = quietAutoLookbackEnabled;
        this.quietAutoLookbackExtraSeconds = quietAutoLookbackExtraSeconds;
        this.audioLookbackSeconds = audioLookbackSeconds;
        this.recordingSaveEnabled = recordingSaveEnabled;
        this.recordingRetentionDays = recordingRetentionDays;
        this.aiModelType = aiModelType;
        this.aiModelName = aiModelName;
        this.aiTokenPlainText = aiTokenPlainText;
        this.aiSecretKey = aiSecretKey == null ? "" : aiSecretKey;
        this.speechApiKey = speechApiKey;
        this.localAsrEnabled = localAsrEnabled;
        this.localAsrModelId = localAsrModelId == null ? "" : localAsrModelId;
        this.cloudWhisperEnabled = cloudWhisperEnabled;
        this.selectedKwsModelIds = selectedKwsModelIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(selectedKwsModelIds));
        this.currentKwsModelId = currentKwsModelId == null ? "" : currentKwsModelId.trim();
        this.asrModelSelected = asrModelSelected;
        this.vadModelSelected = vadModelSelected;
        this.wakeAlertMode = wakeAlertMode == null ? "NOTIFICATION_ONLY" : wakeAlertMode;
        this.logMode = logMode == null ? "SIMPLE" : logMode;
        this.showDiagnosticLogs = showDiagnosticLogs;
        this.showAudioDeviceLogs = showAudioDeviceLogs;
        this.showGainActivityLogs = showGainActivityLogs;
        this.showTtsSelfTestLogs = showTtsSelfTestLogs;
        this.showHeartbeatLogs = showHeartbeatLogs;
        this.ttsSelfTestEnabled = ttsSelfTestEnabled;
        this.backgroundKeepAliveEnabled = backgroundKeepAliveEnabled;
    }

    /**
     * 获取用于构建 UserPreferences 实例的构建器
     *
     * @return {@link Builder} 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取用户设置的唤醒关键词（多个以逗号分隔）
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * 获取唤醒词触发阈值（0.05-0.8，推荐 0.25）
     */
    public float getKwsThreshold() {
        return kwsThreshold;
    }

    /**
     * 获取是否启用了静音检测 (VAD)
     */
    public boolean isVadEnabled() {
        return vadEnabled;
    }

    /**
     * 获取触发静音判定所需的连续时长（秒）
     */
    public int getVadQuietThresholdSeconds() {
        return vadQuietThresholdSeconds;
    }

    /**
     * 获取安静超时提醒方式（NOTIFICATION_ONLY 或 SOUND）
     */
    public String getQuietAlertMode() {
        return quietAlertMode;
    }

    /**
     * 获取是否启用安静超时后自动扩展回溯
     */
    public boolean isQuietAutoLookbackEnabled() {
        return quietAutoLookbackEnabled;
    }

    /**
     * 获取安静超时后额外回溯秒数（1-60，默认 8）
     */
    public int getQuietAutoLookbackExtraSeconds() {
        return quietAutoLookbackExtraSeconds;
    }

    /**
     * 获取音频循环缓冲区回溯的时长（秒）
     */
    public int getAudioLookbackSeconds() {
        return audioLookbackSeconds;
    }

    /**
     * 获取是否启用了录音自动保存
     */
    public boolean isRecordingSaveEnabled() {
        return recordingSaveEnabled;
    }

    /**
     * 获取录音文件在本地保留的天数
     */
    public int getRecordingRetentionDays() {
        return recordingRetentionDays;
    }

    /**
     * 获取选择的 AI 模型提供商类型
     */
    public LLMConfig.ModelType getAiModelType() {
        return aiModelType;
    }

    /**
     * 获取选择的具体 AI 模型名称
     */
    public String getAiModelName() {
        return aiModelName;
    }

    /**
     * 获取解密后的 AI API Token 明文
     */
    public String getAiTokenPlainText() {
        return aiTokenPlainText;
    }

    /**
     * 获取语音识别 API Key（用于云端语音识别）
     */
    public String getSpeechApiKey() {
        return speechApiKey;
    }

    /**
     * 获取 AI 平台 Secret Key（千帆等需要双密钥的平台）
     */
    public String getAiSecretKey() {
        return aiSecretKey;
    }

    /**
     * 获取是否启用本机ASR模型
     */
    public boolean isLocalAsrEnabled() {
        return localAsrEnabled;
    }

    /**
     * 获取本机ASR模型ID
     */
    public String getLocalAsrModelId() {
        return localAsrModelId;
    }

    /**
     * 获取是否启用云端Whisper
     */
    public boolean isCloudWhisperEnabled() {
        return cloudWhisperEnabled;
    }

    public Set<String> getSelectedKwsModelIds() {
        return selectedKwsModelIds;
    }

    public String getCurrentKwsModelId() {
        return currentKwsModelId;
    }

    public boolean isAsrModelSelected() {
        return asrModelSelected;
    }

    public boolean isVadModelSelected() {
        return vadModelSelected;
    }

    /**
     * 获取唤醒提示方式（NOTIFICATION_ONLY 或 SOUND）
     */
    public String getWakeAlertMode() {
        return wakeAlertMode;
    }

    /**
     * 获取日志模式（SIMPLE 或 FULL）
     */
    public String getLogMode() {
        return logMode;
    }

    public boolean isShowDiagnosticLogs() {
        return showDiagnosticLogs;
    }

    public boolean isShowAudioDeviceLogs() {
        return showAudioDeviceLogs;
    }

    public boolean isShowGainActivityLogs() {
        return showGainActivityLogs;
    }

    public boolean isShowTtsSelfTestLogs() {
        return showTtsSelfTestLogs;
    }

    public boolean isShowHeartbeatLogs() {
        return showHeartbeatLogs;
    }

    /**
     * 获取是否启用 TTS 自测
     */
    public boolean isTtsSelfTestEnabled() {
        return ttsSelfTestEnabled;
    }

    /**
     * 获取是否启用后台保活（Android 前台服务；桌面端保持监听不被系统休眠）
     */
    public boolean isBackgroundKeepAliveEnabled() {
        return backgroundKeepAliveEnabled;
    }

    /**
     * UserPreferences 的构建器类
     */
    public static final class Builder {

        private String keywords = "";
        private float kwsThreshold = 0.25f;
        private boolean vadEnabled = true;
        private int vadQuietThresholdSeconds = 5;
        private String quietAlertMode = "NOTIFICATION_ONLY";
        private boolean quietAutoLookbackEnabled = true;
        private int quietAutoLookbackExtraSeconds = 8;
        private int audioLookbackSeconds = 15;
        private boolean recordingSaveEnabled;
        private int recordingRetentionDays = 7;
        private LLMConfig.ModelType aiModelType = LLMConfig.ModelType.QIANFAN;
        private String aiModelName = "";
        private String aiTokenPlainText = "";
        private String aiSecretKey = "";
        private String speechApiKey = "";
        private boolean localAsrEnabled = true;
        private String localAsrModelId = "";
        private boolean cloudWhisperEnabled = false;
        private Set<String> selectedKwsModelIds = Collections.emptySet();
        private String currentKwsModelId = "";
        private boolean asrModelSelected = true;
        private boolean vadModelSelected = true;
        private String wakeAlertMode = "NOTIFICATION_ONLY";
        private String logMode = "SIMPLE";
        private boolean showDiagnosticLogs = false;
        private boolean showAudioDeviceLogs = false;
        private boolean showGainActivityLogs = false;
        private boolean showTtsSelfTestLogs = false;
        private boolean showHeartbeatLogs = false;
        private boolean ttsSelfTestEnabled = false;
        private boolean backgroundKeepAliveEnabled = true;

        private Builder() {
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords == null ? "" : keywords.trim();
            return this;
        }

        public Builder kwsThreshold(float kwsThreshold) {
            this.kwsThreshold = Math.max(0.05f, Math.min(0.8f, kwsThreshold));
            return this;
        }

        public Builder vadEnabled(boolean vadEnabled) {
            this.vadEnabled = vadEnabled;
            return this;
        }

        public Builder vadQuietThresholdSeconds(int vadQuietThresholdSeconds) {
            this.vadQuietThresholdSeconds = vadQuietThresholdSeconds;
            return this;
        }

        public Builder quietAlertMode(String quietAlertMode) {
            this.quietAlertMode = "SOUND".equals(quietAlertMode) ? "SOUND" : "NOTIFICATION_ONLY";
            return this;
        }

        public Builder quietAutoLookbackEnabled(boolean quietAutoLookbackEnabled) {
            this.quietAutoLookbackEnabled = quietAutoLookbackEnabled;
            return this;
        }

        public Builder quietAutoLookbackExtraSeconds(int quietAutoLookbackExtraSeconds) {
            this.quietAutoLookbackExtraSeconds = Math.max(1, Math.min(60, quietAutoLookbackExtraSeconds));
            return this;
        }

        public Builder audioLookbackSeconds(int audioLookbackSeconds) {
            this.audioLookbackSeconds = audioLookbackSeconds;
            return this;
        }

        public Builder recordingSaveEnabled(boolean recordingSaveEnabled) {
            this.recordingSaveEnabled = recordingSaveEnabled;
            return this;
        }

        public Builder recordingRetentionDays(int recordingRetentionDays) {
            this.recordingRetentionDays = recordingRetentionDays;
            return this;
        }

        public Builder aiModelType(LLMConfig.ModelType aiModelType) {
            this.aiModelType = Objects.requireNonNullElse(aiModelType, LLMConfig.ModelType.QIANFAN);
            return this;
        }

        public Builder aiModelName(String aiModelName) {
            this.aiModelName = aiModelName == null ? "" : aiModelName.trim();
            return this;
        }

        public Builder aiTokenPlainText(String aiTokenPlainText) {
            this.aiTokenPlainText = aiTokenPlainText == null ? "" : aiTokenPlainText.trim();
            return this;
        }

        public Builder aiSecretKey(String aiSecretKey) {
            this.aiSecretKey = aiSecretKey == null ? "" : aiSecretKey.trim();
            return this;
        }

        public Builder speechApiKey(String speechApiKey) {
            this.speechApiKey = speechApiKey == null ? "" : speechApiKey.trim();
            return this;
        }

        public Builder localAsrEnabled(boolean localAsrEnabled) {
            this.localAsrEnabled = localAsrEnabled;
            return this;
        }

        public Builder localAsrModelId(String localAsrModelId) {
            this.localAsrModelId = localAsrModelId == null ? "" : localAsrModelId.trim();
            return this;
        }

        public Builder cloudWhisperEnabled(boolean cloudWhisperEnabled) {
            this.cloudWhisperEnabled = cloudWhisperEnabled;
            return this;
        }

        public Builder selectedKwsModelIds(Set<String> selectedKwsModelIds) {
            if (selectedKwsModelIds == null || selectedKwsModelIds.isEmpty()) {
                this.selectedKwsModelIds = Collections.emptySet();
            } else {
                this.selectedKwsModelIds = new LinkedHashSet<>(selectedKwsModelIds);
            }
            return this;
        }

        public Builder currentKwsModelId(String currentKwsModelId) {
            this.currentKwsModelId = currentKwsModelId == null ? "" : currentKwsModelId.trim();
            return this;
        }

        public Builder asrModelSelected(boolean asrModelSelected) {
            this.asrModelSelected = asrModelSelected;
            return this;
        }

        public Builder vadModelSelected(boolean vadModelSelected) {
            this.vadModelSelected = vadModelSelected;
            return this;
        }

        public Builder wakeAlertMode(String wakeAlertMode) {
            this.wakeAlertMode = "SOUND".equals(wakeAlertMode) ? "SOUND" : "NOTIFICATION_ONLY";
            return this;
        }

        public Builder logMode(String logMode) {
            this.logMode = "FULL".equals(logMode) ? "FULL" : "SIMPLE";
            return this;
        }

        public Builder showDiagnosticLogs(boolean showDiagnosticLogs) {
            this.showDiagnosticLogs = showDiagnosticLogs;
            return this;
        }

        public Builder showAudioDeviceLogs(boolean showAudioDeviceLogs) {
            this.showAudioDeviceLogs = showAudioDeviceLogs;
            return this;
        }

        public Builder showGainActivityLogs(boolean showGainActivityLogs) {
            this.showGainActivityLogs = showGainActivityLogs;
            return this;
        }

        public Builder showTtsSelfTestLogs(boolean showTtsSelfTestLogs) {
            this.showTtsSelfTestLogs = showTtsSelfTestLogs;
            return this;
        }

        public Builder showHeartbeatLogs(boolean showHeartbeatLogs) {
            this.showHeartbeatLogs = showHeartbeatLogs;
            return this;
        }

        public Builder ttsSelfTestEnabled(boolean ttsSelfTestEnabled) {
            this.ttsSelfTestEnabled = ttsSelfTestEnabled;
            return this;
        }

        public Builder backgroundKeepAliveEnabled(boolean backgroundKeepAliveEnabled) {
            this.backgroundKeepAliveEnabled = backgroundKeepAliveEnabled;
            return this;
        }

        public UserPreferences build() {
            return new UserPreferences(
                    keywords,
                    kwsThreshold,
                    vadEnabled,
                    vadQuietThresholdSeconds,
                    quietAlertMode,
                    quietAutoLookbackEnabled,
                    quietAutoLookbackExtraSeconds,
                    audioLookbackSeconds,
                    recordingSaveEnabled,
                    recordingRetentionDays,
                    aiModelType,
                    aiModelName,
                    aiTokenPlainText,
                    aiSecretKey,
                    speechApiKey,
                    localAsrEnabled,
                    localAsrModelId,
                    cloudWhisperEnabled,
                    selectedKwsModelIds,
                    currentKwsModelId,
                    asrModelSelected,
                    vadModelSelected,
                    wakeAlertMode,
                    logMode,
                    showDiagnosticLogs,
                    showAudioDeviceLogs,
                    showGainActivityLogs,
                    showTtsSelfTestLogs,
                    showHeartbeatLogs,
                    ttsSelfTestEnabled,
                    backgroundKeepAliveEnabled);
        }
    }
}
