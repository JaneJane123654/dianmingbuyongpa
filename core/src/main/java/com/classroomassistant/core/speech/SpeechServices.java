package com.classroomassistant.core.speech;

/**
 * 语音服务容器
 * 包含所有语音相关服务的实例
 */
public class SpeechServices {
    
    private final WakeWordDetector wakeWordDetector;
    private final SilenceDetector silenceDetector;
    private final SpeechRecognizer speechRecognizer;
    
    public SpeechServices(WakeWordDetector wakeWordDetector,
                          SilenceDetector silenceDetector,
                          SpeechRecognizer speechRecognizer) {
        this.wakeWordDetector = wakeWordDetector;
        this.silenceDetector = silenceDetector;
        this.speechRecognizer = speechRecognizer;
    }
    
    public WakeWordDetector getWakeWordDetector() {
        return wakeWordDetector;
    }
    
    public SilenceDetector getSilenceDetector() {
        return silenceDetector;
    }
    
    public SpeechRecognizer getSpeechRecognizer() {
        return speechRecognizer;
    }
    
    /**
     * 释放所有资源
     */
    public void releaseAll() {
        if (wakeWordDetector != null) {
            wakeWordDetector.release();
        }
        if (silenceDetector != null) {
            silenceDetector.release();
        }
        if (speechRecognizer != null) {
            speechRecognizer.release();
        }
    }
}
