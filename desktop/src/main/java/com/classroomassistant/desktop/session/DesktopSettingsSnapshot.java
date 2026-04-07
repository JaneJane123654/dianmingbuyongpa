package com.classroomassistant.desktop.session;

import java.util.Objects;

/**
 * 桌面端监听配置快照。
 *
 * <p>
 * 字段与 Android 端关键设置保持对齐，供桌面端业务层统一读取。
 * </p>
 */
public final class DesktopSettingsSnapshot {

    private final String keywords;
    private final float kwsThreshold;
    private final boolean vadEnabled;
    private final int quietThresholdSeconds;
    private final String quietAlertMode;
    private final boolean quietAutoLookbackEnabled;
    private final int quietAutoLookbackExtraSeconds;
    private final int lookbackSeconds;
    private final boolean recordingSaveEnabled;
    private final int retentionDays;
    private final String aiProvider;
    private final String aiModelName;
    private final String aiBaseUrl;
    private final String aiToken;
    private final String aiSecretKey;
    private final String speechApiKey;
    private final boolean localAsrEnabled;
    private final String localAsrModelId;
    private final boolean cloudWhisperEnabled;
    private final String currentKwsModelId;
    private final boolean customModelEnabled;
    private final String customKwsModelUrl;
    private final String customAsrModelUrl;
    private final String wakeAlertMode;
    private final String logMode;
    private final boolean showDiagnosticLogs;
    private final boolean showAudioDeviceLogs;
    private final boolean showGainActivityLogs;
    private final boolean showTtsSelfTestLogs;
    private final boolean showHeartbeatLogs;
    private final boolean ttsSelfTestEnabled;
    private final boolean backgroundKeepAliveEnabled;

    private DesktopSettingsSnapshot(Builder builder) {
        this.keywords = builder.keywords;
        this.kwsThreshold = builder.kwsThreshold;
        this.vadEnabled = builder.vadEnabled;
        this.quietThresholdSeconds = builder.quietThresholdSeconds;
        this.quietAlertMode = builder.quietAlertMode;
        this.quietAutoLookbackEnabled = builder.quietAutoLookbackEnabled;
        this.quietAutoLookbackExtraSeconds = builder.quietAutoLookbackExtraSeconds;
        this.lookbackSeconds = builder.lookbackSeconds;
        this.recordingSaveEnabled = builder.recordingSaveEnabled;
        this.retentionDays = builder.retentionDays;
        this.aiProvider = builder.aiProvider;
        this.aiModelName = builder.aiModelName;
        this.aiBaseUrl = builder.aiBaseUrl;
        this.aiToken = builder.aiToken;
        this.aiSecretKey = builder.aiSecretKey;
        this.speechApiKey = builder.speechApiKey;
        this.localAsrEnabled = builder.localAsrEnabled;
        this.localAsrModelId = builder.localAsrModelId;
        this.cloudWhisperEnabled = builder.cloudWhisperEnabled;
        this.currentKwsModelId = builder.currentKwsModelId;
        this.customModelEnabled = builder.customModelEnabled;
        this.customKwsModelUrl = builder.customKwsModelUrl;
        this.customAsrModelUrl = builder.customAsrModelUrl;
        this.wakeAlertMode = builder.wakeAlertMode;
        this.logMode = builder.logMode;
        this.showDiagnosticLogs = builder.showDiagnosticLogs;
        this.showAudioDeviceLogs = builder.showAudioDeviceLogs;
        this.showGainActivityLogs = builder.showGainActivityLogs;
        this.showTtsSelfTestLogs = builder.showTtsSelfTestLogs;
        this.showHeartbeatLogs = builder.showHeartbeatLogs;
        this.ttsSelfTestEnabled = builder.ttsSelfTestEnabled;
        this.backgroundKeepAliveEnabled = builder.backgroundKeepAliveEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DesktopSettingsSnapshot defaults() {
        return builder().build();
    }

    public String getKeywords() {
        return keywords;
    }

    public float getKwsThreshold() {
        return kwsThreshold;
    }

    public boolean isVadEnabled() {
        return vadEnabled;
    }

    public int getQuietThresholdSeconds() {
        return quietThresholdSeconds;
    }

    public String getQuietAlertMode() {
        return quietAlertMode;
    }

    public boolean isQuietAutoLookbackEnabled() {
        return quietAutoLookbackEnabled;
    }

    public int getQuietAutoLookbackExtraSeconds() {
        return quietAutoLookbackExtraSeconds;
    }

    public int getLookbackSeconds() {
        return lookbackSeconds;
    }

    public boolean isRecordingSaveEnabled() {
        return recordingSaveEnabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModelName() {
        return aiModelName;
    }

    public String getAiBaseUrl() {
        return aiBaseUrl;
    }

    public String getAiToken() {
        return aiToken;
    }

    public String getAiSecretKey() {
        return aiSecretKey;
    }

    public String getSpeechApiKey() {
        return speechApiKey;
    }

    public boolean isLocalAsrEnabled() {
        return localAsrEnabled;
    }

    public String getLocalAsrModelId() {
        return localAsrModelId;
    }

    public boolean isCloudWhisperEnabled() {
        return cloudWhisperEnabled;
    }

    public String getCurrentKwsModelId() {
        return currentKwsModelId;
    }

    public boolean isCustomModelEnabled() {
        return customModelEnabled;
    }

    public String getCustomKwsModelUrl() {
        return customKwsModelUrl;
    }

    public String getCustomAsrModelUrl() {
        return customAsrModelUrl;
    }

    public String getWakeAlertMode() {
        return wakeAlertMode;
    }

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

    public boolean isTtsSelfTestEnabled() {
        return ttsSelfTestEnabled;
    }

    public boolean isBackgroundKeepAliveEnabled() {
        return backgroundKeepAliveEnabled;
    }

    /**
     * 对齐 Android 端“保存后是否需要重启监听”的判定维度。
     */
    public boolean isMonitoringConfigEquivalent(DesktopSettingsSnapshot other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(currentKwsModelId, other.currentKwsModelId)
                && Objects.equals(keywords, other.keywords)
                && Float.compare(kwsThreshold, other.kwsThreshold) == 0
                && vadEnabled == other.vadEnabled
                && quietThresholdSeconds == other.quietThresholdSeconds
                && Objects.equals(quietAlertMode, other.quietAlertMode)
                && quietAutoLookbackEnabled == other.quietAutoLookbackEnabled
                && quietAutoLookbackExtraSeconds == other.quietAutoLookbackExtraSeconds
                && lookbackSeconds == other.lookbackSeconds
                && localAsrEnabled == other.localAsrEnabled
                && Objects.equals(localAsrModelId, other.localAsrModelId)
                && cloudWhisperEnabled == other.cloudWhisperEnabled
                && customModelEnabled == other.customModelEnabled
                && Objects.equals(customKwsModelUrl, other.customKwsModelUrl)
                && Objects.equals(customAsrModelUrl, other.customAsrModelUrl)
                && recordingSaveEnabled == other.recordingSaveEnabled
                && retentionDays == other.retentionDays
                && ttsSelfTestEnabled == other.ttsSelfTestEnabled
                && backgroundKeepAliveEnabled == other.backgroundKeepAliveEnabled;
    }

    public static final class Builder {

        private String keywords = "";
        private float kwsThreshold = 0.05f;
        private boolean vadEnabled = true;
        private int quietThresholdSeconds = 5;
        private String quietAlertMode = "NOTIFICATION_ONLY";
        private boolean quietAutoLookbackEnabled = true;
        private int quietAutoLookbackExtraSeconds = 8;
        private int lookbackSeconds = 15;
        private boolean recordingSaveEnabled = false;
        private int retentionDays = 7;
        private String aiProvider = "OPENAI_COMPATIBLE";
        private String aiModelName = "";
        private String aiBaseUrl = "";
        private String aiToken = "";
        private String aiSecretKey = "";
        private String speechApiKey = "";
        private boolean localAsrEnabled = true;
        private String localAsrModelId = "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16";
        private boolean cloudWhisperEnabled = false;
        private String currentKwsModelId = "";
        private boolean customModelEnabled = false;
        private String customKwsModelUrl = "";
        private String customAsrModelUrl = "";
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

        public Builder keywords(String value) {
            this.keywords = value == null ? "" : value.trim();
            return this;
        }

        public Builder kwsThreshold(float value) {
            this.kwsThreshold = value;
            return this;
        }

        public Builder vadEnabled(boolean value) {
            this.vadEnabled = value;
            return this;
        }

        public Builder quietThresholdSeconds(int value) {
            this.quietThresholdSeconds = value;
            return this;
        }

        public Builder quietAlertMode(String value) {
            this.quietAlertMode = value == null ? "NOTIFICATION_ONLY" : value;
            return this;
        }

        public Builder quietAutoLookbackEnabled(boolean value) {
            this.quietAutoLookbackEnabled = value;
            return this;
        }

        public Builder quietAutoLookbackExtraSeconds(int value) {
            this.quietAutoLookbackExtraSeconds = value;
            return this;
        }

        public Builder lookbackSeconds(int value) {
            this.lookbackSeconds = value;
            return this;
        }

        public Builder recordingSaveEnabled(boolean value) {
            this.recordingSaveEnabled = value;
            return this;
        }

        public Builder retentionDays(int value) {
            this.retentionDays = value;
            return this;
        }

        public Builder aiProvider(String value) {
            this.aiProvider = value == null ? "OPENAI_COMPATIBLE" : value.trim();
            return this;
        }

        public Builder aiModelName(String value) {
            this.aiModelName = value == null ? "" : value.trim();
            return this;
        }

        public Builder aiBaseUrl(String value) {
            this.aiBaseUrl = value == null ? "" : value.trim();
            return this;
        }

        public Builder aiToken(String value) {
            this.aiToken = value == null ? "" : value.trim();
            return this;
        }

        public Builder aiSecretKey(String value) {
            this.aiSecretKey = value == null ? "" : value.trim();
            return this;
        }

        public Builder speechApiKey(String value) {
            this.speechApiKey = value == null ? "" : value.trim();
            return this;
        }

        public Builder localAsrEnabled(boolean value) {
            this.localAsrEnabled = value;
            return this;
        }

        public Builder localAsrModelId(String value) {
            this.localAsrModelId = value == null ? "" : value.trim();
            return this;
        }

        public Builder cloudWhisperEnabled(boolean value) {
            this.cloudWhisperEnabled = value;
            return this;
        }

        public Builder currentKwsModelId(String value) {
            this.currentKwsModelId = value == null ? "" : value.trim();
            return this;
        }

        public Builder customModelEnabled(boolean value) {
            this.customModelEnabled = value;
            return this;
        }

        public Builder customKwsModelUrl(String value) {
            this.customKwsModelUrl = value == null ? "" : value.trim();
            return this;
        }

        public Builder customAsrModelUrl(String value) {
            this.customAsrModelUrl = value == null ? "" : value.trim();
            return this;
        }

        public Builder wakeAlertMode(String value) {
            this.wakeAlertMode = value == null ? "NOTIFICATION_ONLY" : value;
            return this;
        }

        public Builder logMode(String value) {
            this.logMode = value == null ? "SIMPLE" : value;
            return this;
        }

        public Builder showDiagnosticLogs(boolean value) {
            this.showDiagnosticLogs = value;
            return this;
        }

        public Builder showAudioDeviceLogs(boolean value) {
            this.showAudioDeviceLogs = value;
            return this;
        }

        public Builder showGainActivityLogs(boolean value) {
            this.showGainActivityLogs = value;
            return this;
        }

        public Builder showTtsSelfTestLogs(boolean value) {
            this.showTtsSelfTestLogs = value;
            return this;
        }

        public Builder showHeartbeatLogs(boolean value) {
            this.showHeartbeatLogs = value;
            return this;
        }

        public Builder ttsSelfTestEnabled(boolean value) {
            this.ttsSelfTestEnabled = value;
            return this;
        }

        public Builder backgroundKeepAliveEnabled(boolean value) {
            this.backgroundKeepAliveEnabled = value;
            return this;
        }

        public DesktopSettingsSnapshot build() {
            this.kwsThreshold = Math.max(0.05f, Math.min(0.8f, this.kwsThreshold));
            this.quietThresholdSeconds = Math.max(3, Math.min(30, this.quietThresholdSeconds));
            this.quietAutoLookbackExtraSeconds = Math.max(1, Math.min(60, this.quietAutoLookbackExtraSeconds));
            this.lookbackSeconds = Math.max(8, Math.min(120, this.lookbackSeconds));
            this.retentionDays = Math.max(0, Math.min(30, this.retentionDays));
            return new DesktopSettingsSnapshot(this);
        }
    }
}
