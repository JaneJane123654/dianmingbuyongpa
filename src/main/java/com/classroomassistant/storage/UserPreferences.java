package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import java.util.Objects;

/**
 * 用户配置（值对象）
 *
 * <p>对应 Java Preferences 的键值结构，供 UI 与业务逻辑读取使用。
 */
public class UserPreferences {

    private final String keywords;
    private final boolean vadEnabled;
    private final int vadQuietThresholdSeconds;
    private final int audioLookbackSeconds;
    private final boolean recordingSaveEnabled;
    private final int recordingRetentionDays;
    private final LLMConfig.ModelType aiModelType;
    private final String aiModelName;
    private final String aiTokenPlainText;

    private UserPreferences(
        String keywords,
        boolean vadEnabled,
        int vadQuietThresholdSeconds,
        int audioLookbackSeconds,
        boolean recordingSaveEnabled,
        int recordingRetentionDays,
        LLMConfig.ModelType aiModelType,
        String aiModelName,
        String aiTokenPlainText
    ) {
        this.keywords = keywords;
        this.vadEnabled = vadEnabled;
        this.vadQuietThresholdSeconds = vadQuietThresholdSeconds;
        this.audioLookbackSeconds = audioLookbackSeconds;
        this.recordingSaveEnabled = recordingSaveEnabled;
        this.recordingRetentionDays = recordingRetentionDays;
        this.aiModelType = aiModelType;
        this.aiModelName = aiModelName;
        this.aiTokenPlainText = aiTokenPlainText;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeywords() {
        return keywords;
    }

    public boolean isVadEnabled() {
        return vadEnabled;
    }

    public int getVadQuietThresholdSeconds() {
        return vadQuietThresholdSeconds;
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

    public LLMConfig.ModelType getAiModelType() {
        return aiModelType;
    }

    public String getAiModelName() {
        return aiModelName;
    }

    public String getAiTokenPlainText() {
        return aiTokenPlainText;
    }

    public static final class Builder {

        private String keywords = "";
        private boolean vadEnabled = true;
        private int vadQuietThresholdSeconds = 5;
        private int audioLookbackSeconds = 240;
        private boolean recordingSaveEnabled;
        private int recordingRetentionDays = 7;
        private LLMConfig.ModelType aiModelType = LLMConfig.ModelType.QIANFAN;
        private String aiModelName = "";
        private String aiTokenPlainText = "";

        private Builder() {
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords == null ? "" : keywords.trim();
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

        public UserPreferences build() {
            return new UserPreferences(
                keywords,
                vadEnabled,
                vadQuietThresholdSeconds,
                audioLookbackSeconds,
                recordingSaveEnabled,
                recordingRetentionDays,
                aiModelType,
                aiModelName,
                aiTokenPlainText
            );
        }
    }
}

