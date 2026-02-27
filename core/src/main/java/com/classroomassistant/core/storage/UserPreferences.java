package com.classroomassistant.core.storage;

/**
 * 用户偏好设置（平台无关）
 */
public class UserPreferences {
    
    private String llmModelType = "openai";
    private String llmApiKey = "";
    private String llmSecretKey = "";
    private String llmBaseUrl = "";
    private String llmModelName = "";
    
    private String speechApiKey = "";
    private String speechApiUrl = "";
    
    private String wakeWord = "小助手";
    private int silenceTimeoutMs = 2000;
    private float silenceThreshold = 0.1f;
    
    private boolean autoStart = false;
    private String language = "zh-CN";
    
    // Getters and Setters
    public String getLlmModelType() { return llmModelType; }
    public void setLlmModelType(String llmModelType) { this.llmModelType = llmModelType; }
    
    public String getLlmApiKey() { return llmApiKey; }
    public void setLlmApiKey(String llmApiKey) { this.llmApiKey = llmApiKey; }
    
    public String getLlmSecretKey() { return llmSecretKey; }
    public void setLlmSecretKey(String llmSecretKey) { this.llmSecretKey = llmSecretKey; }
    
    public String getLlmBaseUrl() { return llmBaseUrl; }
    public void setLlmBaseUrl(String llmBaseUrl) { this.llmBaseUrl = llmBaseUrl; }
    
    public String getLlmModelName() { return llmModelName; }
    public void setLlmModelName(String llmModelName) { this.llmModelName = llmModelName; }
    
    public String getSpeechApiKey() { return speechApiKey; }
    public void setSpeechApiKey(String speechApiKey) { this.speechApiKey = speechApiKey; }
    
    public String getSpeechApiUrl() { return speechApiUrl; }
    public void setSpeechApiUrl(String speechApiUrl) { this.speechApiUrl = speechApiUrl; }
    
    public String getWakeWord() { return wakeWord; }
    public void setWakeWord(String wakeWord) { this.wakeWord = wakeWord; }
    
    public int getSilenceTimeoutMs() { return silenceTimeoutMs; }
    public void setSilenceTimeoutMs(int silenceTimeoutMs) { this.silenceTimeoutMs = silenceTimeoutMs; }
    
    public float getSilenceThreshold() { return silenceThreshold; }
    public void setSilenceThreshold(float silenceThreshold) { this.silenceThreshold = silenceThreshold; }
    
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    // Builder
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final UserPreferences prefs = new UserPreferences();
        
        public Builder llmModelType(String v) { prefs.llmModelType = v; return this; }
        public Builder llmApiKey(String v) { prefs.llmApiKey = v; return this; }
        public Builder llmSecretKey(String v) { prefs.llmSecretKey = v; return this; }
        public Builder llmBaseUrl(String v) { prefs.llmBaseUrl = v; return this; }
        public Builder llmModelName(String v) { prefs.llmModelName = v; return this; }
        public Builder speechApiKey(String v) { prefs.speechApiKey = v; return this; }
        public Builder speechApiUrl(String v) { prefs.speechApiUrl = v; return this; }
        public Builder wakeWord(String v) { prefs.wakeWord = v; return this; }
        public Builder silenceTimeoutMs(int v) { prefs.silenceTimeoutMs = v; return this; }
        public Builder silenceThreshold(float v) { prefs.silenceThreshold = v; return this; }
        public Builder autoStart(boolean v) { prefs.autoStart = v; return this; }
        public Builder language(String v) { prefs.language = v; return this; }
        
        public UserPreferences build() { return prefs; }
    }
}
