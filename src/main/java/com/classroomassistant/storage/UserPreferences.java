package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import java.util.Objects;

/**
 * 用户配置信息 (User Preferences Value Object)
 *
 * <p>本类是一个不可变的值对象（Value Object），用于承载用户在界面上设置的各项参数。
 * 包含了唤醒词、静音检测配置、音频回溯时长、录音保存策略以及 AI 模型配置等信息。
 *
 * <p>通常由 {@link PreferencesManager} 构造并提供给业务逻辑使用。
 *
 * @author Code Assistant
 * @date 2026-01-31
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
    private final String speechApiKey;

    private UserPreferences(
        String keywords,
        boolean vadEnabled,
        int vadQuietThresholdSeconds,
        int audioLookbackSeconds,
        boolean recordingSaveEnabled,
        int recordingRetentionDays,
        LLMConfig.ModelType aiModelType,
        String aiModelName,
        String aiTokenPlainText,
        String speechApiKey
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
        this.speechApiKey = speechApiKey;
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
     * UserPreferences 的构建器类
     */
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
        private String speechApiKey = "";

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

        public Builder speechApiKey(String speechApiKey) {
            this.speechApiKey = speechApiKey == null ? "" : speechApiKey.trim();
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
                aiTokenPlainText,
                speechApiKey
            );
        }
    }
}

