package com.classroomassistant.core.constants;

/**
 * 错误码定义
 */
public final class ErrorCode {
    
    private ErrorCode() {}
    
    // ========== 通用错误 (1xxx) ==========
    public static final int UNKNOWN_ERROR = 1000;
    public static final int INITIALIZATION_FAILED = 1001;
    public static final int CONFIGURATION_ERROR = 1002;
    
    // ========== 音频错误 (2xxx) ==========
    public static final int AUDIO_DEVICE_NOT_FOUND = 2001;
    public static final int AUDIO_PERMISSION_DENIED = 2002;
    public static final int AUDIO_RECORDING_FAILED = 2003;
    
    // ========== 语音识别错误 (3xxx) ==========
    public static final int SPEECH_ENGINE_INIT_FAILED = 3001;
    public static final int SPEECH_RECOGNITION_FAILED = 3002;
    public static final int WAKE_WORD_DETECTION_FAILED = 3003;
    public static final int MODEL_LOAD_FAILED = 3004;
    public static final int MODEL_NOT_FOUND = 3005;
    
    // ========== AI 错误 (4xxx) ==========
    public static final int LLM_API_ERROR = 4001;
    public static final int LLM_TIMEOUT = 4002;
    public static final int LLM_RATE_LIMITED = 4003;
    public static final int LLM_INVALID_API_KEY = 4004;
    
    // ========== 网络错误 (5xxx) ==========
    public static final int NETWORK_UNAVAILABLE = 5001;
    public static final int NETWORK_TIMEOUT = 5002;
    public static final int NETWORK_SSL_ERROR = 5003;
    
    // ========== 存储错误 (6xxx) ==========
    public static final int STORAGE_WRITE_FAILED = 6001;
    public static final int STORAGE_READ_FAILED = 6002;
    public static final int STORAGE_FULL = 6003;
    
    /**
     * 获取错误描述
     */
    public static String getDescription(int code) {
        switch (code) {
            case UNKNOWN_ERROR:
                return "未知错误";
            case INITIALIZATION_FAILED:
                return "初始化失败";
            case CONFIGURATION_ERROR:
                return "配置错误";
            case AUDIO_DEVICE_NOT_FOUND:
                return "未找到音频设备";
            case AUDIO_PERMISSION_DENIED:
                return "音频权限被拒绝";
            case AUDIO_RECORDING_FAILED:
                return "录音失败";
            case SPEECH_ENGINE_INIT_FAILED:
                return "语音引擎初始化失败";
            case SPEECH_RECOGNITION_FAILED:
                return "语音识别失败";
            case WAKE_WORD_DETECTION_FAILED:
                return "唤醒词检测失败";
            case MODEL_LOAD_FAILED:
                return "模型加载失败";
            case MODEL_NOT_FOUND:
                return "模型文件未找到";
            case LLM_API_ERROR:
                return "大模型 API 错误";
            case LLM_TIMEOUT:
                return "大模型请求超时";
            case LLM_RATE_LIMITED:
                return "大模型请求被限流";
            case LLM_INVALID_API_KEY:
                return "无效的 API Key";
            case NETWORK_UNAVAILABLE:
                return "网络不可用";
            case NETWORK_TIMEOUT:
                return "网络超时";
            case NETWORK_SSL_ERROR:
                return "SSL 证书错误";
            case STORAGE_WRITE_FAILED:
                return "存储写入失败";
            case STORAGE_READ_FAILED:
                return "存储读取失败";
            case STORAGE_FULL:
                return "存储空间不足";
            default:
                return "错误码: " + code;
        }
    }
}
