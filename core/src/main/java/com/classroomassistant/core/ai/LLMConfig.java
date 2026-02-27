package com.classroomassistant.core.ai;

/**
 * 大语言模型配置
 */
public class LLMConfig {
    
    private String modelType;      // openai, qianfan, deepseek, kimi
    private String apiKey;
    private String secretKey;      // 百度千帆需要
    private String baseUrl;
    private String modelName;
    private int timeoutSeconds = 30;
    private double temperature = 0.7;
    private int maxTokens = 2048;
    
    public LLMConfig() {}
    
    public LLMConfig(String modelType, String apiKey) {
        this.modelType = modelType;
        this.apiKey = apiKey;
    }
    
    // Getters and Setters
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    
    // Builder 模式
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private final LLMConfig config = new LLMConfig();
        
        public Builder modelType(String modelType) { config.modelType = modelType; return this; }
        public Builder apiKey(String apiKey) { config.apiKey = apiKey; return this; }
        public Builder secretKey(String secretKey) { config.secretKey = secretKey; return this; }
        public Builder baseUrl(String baseUrl) { config.baseUrl = baseUrl; return this; }
        public Builder modelName(String modelName) { config.modelName = modelName; return this; }
        public Builder timeoutSeconds(int timeout) { config.timeoutSeconds = timeout; return this; }
        public Builder temperature(double temp) { config.temperature = temp; return this; }
        public Builder maxTokens(int max) { config.maxTokens = max; return this; }
        
        public LLMConfig build() { return config; }
    }
}
