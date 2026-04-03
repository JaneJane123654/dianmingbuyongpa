package com.classroomassistant.core.storage;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户偏好设置（平台无关的不可变值对象）
 *
 * <p>
 * 字段与 Android 端 SettingsData / src 端 UserPreferences 完全对齐，
 * 但不依赖 src 模块的 LLMConfig 枚举，统一用字符串表示 AI 平台类型。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class UserPreferences {

    // ── 唤醒词 ──
    private final String keywords;
    private final float kwsThreshold;

    // ── 安静检测（VAD）──
    private final boolean vadEnabled;
    private final int vadQuietThresholdSeconds;
    private final String quietAlertMode;
    private final boolean quietAutoLookbackEnabled;
    private final int quietAutoLookbackExtraSeconds;

    // ── 语音回溯 ──
    private final int audioLookbackSeconds;

    // ── 录音保存 ──
    private final boolean recordingSaveEnabled;
    private final int recordingRetentionDays;

    // ── AI 问答 ──
    private final String aiModelType;
    private final String aiModelName;
    private final String aiBaseUrl;
    private final String aiTokenPlainText;
    private final String aiSecretKey;

    // ── 语音识别 ──
    private final String speechApiKey;
    private final boolean localAsrEnabled;
    private final String localAsrModelId;
    private final boolean cloudWhisperEnabled;

    // ── 模型管理 ──
    private final Set<String> selectedKwsModelIds;
    private final String currentKwsModelId;
    private final boolean asrModelSelected;
    private final boolean vadModelSelected;

    // ── 唤醒提示 ──
    private final String wakeAlertMode;

    // ── 开发者选项 ──
    private final String logMode;
    private final boolean showDiagnosticLogs;
    private final boolean showAudioDeviceLogs;
    private final boolean showGainActivityLogs;
    private final boolean showTtsSelfTestLogs;
    private final boolean showHeartbeatLogs;
    private final boolean ttsSelfTestEnabled;

    // ── 后台保活 ──
    private final boolean backgroundKeepAliveEnabled;

    // ── 旧版兼容字段 ──
    private final boolean autoStart;
    private final String language;

    private UserPreferences(Builder b) {
        this.keywords = b.keywords;
        this.kwsThreshold = b.kwsThreshold;
        this.vadEnabled = b.vadEnabled;
        this.vadQuietThresholdSeconds = b.vadQuietThresholdSeconds;
        this.quietAlertMode = b.quietAlertMode == null ? "NOTIFICATION_ONLY" : b.quietAlertMode;
        this.quietAutoLookbackEnabled = b.quietAutoLookbackEnabled;
        this.quietAutoLookbackExtraSeconds = b.quietAutoLookbackExtraSeconds;
        this.audioLookbackSeconds = b.audioLookbackSeconds;
        this.recordingSaveEnabled = b.recordingSaveEnabled;
        this.recordingRetentionDays = b.recordingRetentionDays;
        this.aiModelType = b.aiModelType;
        this.aiModelName = b.aiModelName;
        this.aiBaseUrl = b.aiBaseUrl == null ? "" : b.aiBaseUrl;
        this.aiTokenPlainText = b.aiTokenPlainText;
        this.aiSecretKey = b.aiSecretKey == null ? "" : b.aiSecretKey;
        this.speechApiKey = b.speechApiKey;
        this.localAsrEnabled = b.localAsrEnabled;
        this.localAsrModelId = b.localAsrModelId == null ? "" : b.localAsrModelId;
        this.cloudWhisperEnabled = b.cloudWhisperEnabled;
        this.selectedKwsModelIds = b.selectedKwsModelIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(b.selectedKwsModelIds));
        this.currentKwsModelId = b.currentKwsModelId == null ? "" : b.currentKwsModelId.trim();
        this.asrModelSelected = b.asrModelSelected;
        this.vadModelSelected = b.vadModelSelected;
        this.wakeAlertMode = b.wakeAlertMode == null ? "NOTIFICATION_ONLY" : b.wakeAlertMode;
        this.logMode = b.logMode == null ? "SIMPLE" : b.logMode;
        this.showDiagnosticLogs = b.showDiagnosticLogs;
        this.showAudioDeviceLogs = b.showAudioDeviceLogs;
        this.showGainActivityLogs = b.showGainActivityLogs;
        this.showTtsSelfTestLogs = b.showTtsSelfTestLogs;
        this.showHeartbeatLogs = b.showHeartbeatLogs;
        this.ttsSelfTestEnabled = b.ttsSelfTestEnabled;
        this.backgroundKeepAliveEnabled = b.backgroundKeepAliveEnabled;
        this.autoStart = b.autoStart;
        this.language = b.language;
    }

    // ── Getters ─────────────────────────────────────────────────────────

    public String getKeywords() {
        return keywords;
    }

    public float getKwsThreshold() {
        return kwsThreshold;
    }

    public boolean isVadEnabled() {
        return vadEnabled;
    }

    public int getVadQuietThresholdSeconds() {
        return vadQuietThresholdSeconds;
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

    public int getAudioLookbackSeconds() {
        return audioLookbackSeconds;
    }

    public boolean isRecordingSaveEnabled() {
        return recordingSaveEnabled;
    }

    public int getRecordingRetentionDays() {
        return recordingRetentionDays;
    }

    public String getAiModelType() {
        return aiModelType;
    }

    public String getAiModelName() {
        return aiModelName;
    }

    public String getAiBaseUrl() {
        return aiBaseUrl;
    }

    public String getAiTokenPlainText() {
        return aiTokenPlainText;
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

    public boolean isAutoStart() {
        return autoStart;
    }

    public String getLanguage() {
        return language;
    }

    // ── Builder ─────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String keywords = "";
        private float kwsThreshold = 0.05f;
        private boolean vadEnabled = true;
        private int vadQuietThresholdSeconds = 5;
        private String quietAlertMode = "NOTIFICATION_ONLY";
        private boolean quietAutoLookbackEnabled = true;
        private int quietAutoLookbackExtraSeconds = 8;
        private int audioLookbackSeconds = 15;
        private boolean recordingSaveEnabled = false;
        private int recordingRetentionDays = 7;
        private String aiModelType = "QIANFAN";
        private String aiModelName = "";
        private String aiBaseUrl = "";
        private String aiTokenPlainText = "";
        private String aiSecretKey = "";
        private String speechApiKey = "";
        private boolean localAsrEnabled = true;
        private String localAsrModelId = "";
        private boolean cloudWhisperEnabled = false;
        private Set<String> selectedKwsModelIds = Collections.emptySet();
        private String currentKwsModelId = "";
        private boolean asrModelSelected = false;
        private boolean vadModelSelected = false;
        private String wakeAlertMode = "NOTIFICATION_ONLY";
        private String logMode = "SIMPLE";
        private boolean showDiagnosticLogs = false;
        private boolean showAudioDeviceLogs = false;
        private boolean showGainActivityLogs = false;
        private boolean showTtsSelfTestLogs = false;
        private boolean showHeartbeatLogs = false;
        private boolean ttsSelfTestEnabled = false;
        private boolean backgroundKeepAliveEnabled = true;
        private boolean autoStart = false;
        private String language = "zh-CN";

        public Builder keywords(String v) {
            this.keywords = v;
            return this;
        }

        public Builder kwsThreshold(float v) {
            this.kwsThreshold = v;
            return this;
        }

        public Builder vadEnabled(boolean v) {
            this.vadEnabled = v;
            return this;
        }

        public Builder vadQuietThresholdSeconds(int v) {
            this.vadQuietThresholdSeconds = v;
            return this;
        }

        public Builder quietAlertMode(String v) {
            this.quietAlertMode = v;
            return this;
        }

        public Builder quietAutoLookbackEnabled(boolean v) {
            this.quietAutoLookbackEnabled = v;
            return this;
        }

        public Builder quietAutoLookbackExtraSeconds(int v) {
            this.quietAutoLookbackExtraSeconds = v;
            return this;
        }

        public Builder audioLookbackSeconds(int v) {
            this.audioLookbackSeconds = v;
            return this;
        }

        public Builder recordingSaveEnabled(boolean v) {
            this.recordingSaveEnabled = v;
            return this;
        }

        public Builder recordingRetentionDays(int v) {
            this.recordingRetentionDays = v;
            return this;
        }

        public Builder aiModelType(String v) {
            this.aiModelType = v;
            return this;
        }

        public Builder aiModelName(String v) {
            this.aiModelName = v;
            return this;
        }

        public Builder aiBaseUrl(String v) {
            this.aiBaseUrl = v;
            return this;
        }

        public Builder aiTokenPlainText(String v) {
            this.aiTokenPlainText = v;
            return this;
        }

        public Builder aiSecretKey(String v) {
            this.aiSecretKey = v;
            return this;
        }

        public Builder speechApiKey(String v) {
            this.speechApiKey = v;
            return this;
        }

        public Builder localAsrEnabled(boolean v) {
            this.localAsrEnabled = v;
            return this;
        }

        public Builder localAsrModelId(String v) {
            this.localAsrModelId = v;
            return this;
        }

        public Builder cloudWhisperEnabled(boolean v) {
            this.cloudWhisperEnabled = v;
            return this;
        }

        public Builder selectedKwsModelIds(Set<String> v) {
            this.selectedKwsModelIds = v;
            return this;
        }

        public Builder currentKwsModelId(String v) {
            this.currentKwsModelId = v;
            return this;
        }

        public Builder asrModelSelected(boolean v) {
            this.asrModelSelected = v;
            return this;
        }

        public Builder vadModelSelected(boolean v) {
            this.vadModelSelected = v;
            return this;
        }

        public Builder wakeAlertMode(String v) {
            this.wakeAlertMode = v;
            return this;
        }

        public Builder logMode(String v) {
            this.logMode = v;
            return this;
        }

        public Builder showDiagnosticLogs(boolean v) {
            this.showDiagnosticLogs = v;
            return this;
        }

        public Builder showAudioDeviceLogs(boolean v) {
            this.showAudioDeviceLogs = v;
            return this;
        }

        public Builder showGainActivityLogs(boolean v) {
            this.showGainActivityLogs = v;
            return this;
        }

        public Builder showTtsSelfTestLogs(boolean v) {
            this.showTtsSelfTestLogs = v;
            return this;
        }

        public Builder showHeartbeatLogs(boolean v) {
            this.showHeartbeatLogs = v;
            return this;
        }

        public Builder ttsSelfTestEnabled(boolean v) {
            this.ttsSelfTestEnabled = v;
            return this;
        }

        public Builder backgroundKeepAliveEnabled(boolean v) {
            this.backgroundKeepAliveEnabled = v;
            return this;
        }

        public Builder autoStart(boolean v) {
            this.autoStart = v;
            return this;
        }

        public Builder language(String v) {
            this.language = v;
            return this;
        }

        public UserPreferences build() {
            return new UserPreferences(this);
        }
    }
}
