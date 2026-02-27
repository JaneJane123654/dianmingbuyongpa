package com.classroomassistant.core.session;

import com.classroomassistant.core.ai.AnswerListener;
import com.classroomassistant.core.ai.LLMClient;
import com.classroomassistant.core.ai.PromptTemplate;
import com.classroomassistant.core.platform.PlatformAudioRecorder;
import com.classroomassistant.core.platform.PlatformProvider;
import com.classroomassistant.core.speech.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 核心会话管理器（平台无关）
 * 
 * 负责协调音频采集、语音识别、AI 回答等核心业务逻辑。
 * 通过 PlatformProvider 获取平台特定的实现。
 */
public class CoreSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CoreSessionManager.class);
    
    private final PlatformProvider platform;
    private final SpeechServices speechServices;
    private final LLMClient llmClient;
    
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.IDLE);
    private final CopyOnWriteArrayList<SessionListener> listeners = new CopyOnWriteArrayList<>();
    
    private PlatformAudioRecorder audioRecorder;
    
    public CoreSessionManager(PlatformProvider platform,
                              SpeechServices speechServices,
                              LLMClient llmClient) {
        this.platform = platform;
        this.speechServices = speechServices;
        this.llmClient = llmClient;
    }
    
    /**
     * 添加会话监听器
     */
    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除会话监听器
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 获取当前状态
     */
    public SessionState getState() {
        return state.get();
    }
    
    /**
     * 开始监听
     */
    public void startMonitoring() {
        if (state.get() != SessionState.IDLE) {
            log("无法开始：当前状态为 " + state.get());
            return;
        }
        
        log("开始监听...");
        setState(SessionState.MONITORING);
        
        // 获取平台音频录制器
        audioRecorder = platform.getAudioRecorder();
        
        // 启动唤醒词检测
        WakeWordDetector wakeWordDetector = speechServices.getWakeWordDetector();
        wakeWordDetector.startDetection((keyword, confidence) -> {
            log("检测到唤醒词: " + keyword);
            notifyWakeWordDetected(keyword);
            startSpeechRecognition();
        });
        
        // 开始音频采集
        audioRecorder.start(new PlatformAudioRecorder.AudioDataListener() {
            @Override
            public void onAudioData(byte[] data, int length) {
                if (state.get() == SessionState.MONITORING) {
                    wakeWordDetector.feedAudio(data, length);
                } else if (state.get() == SessionState.TRIGGER_HANDLING) {
                    speechServices.getSpeechRecognizer().feedAudio(data, length);
                    speechServices.getSilenceDetector().feedAudio(data, length);
                }
            }

            @Override
            public void onError(String error) {
                log("音频采集错误: " + error);
                notifyError(error);
            }
        });
    }
    
    /**
     * 停止监听
     */
    public void stopMonitoring() {
        log("停止监听...");
        
        if (audioRecorder != null) {
            audioRecorder.stop();
        }
        
        speechServices.getWakeWordDetector().stopDetection();
        speechServices.getSilenceDetector().stopDetection();
        speechServices.getSpeechRecognizer().stopRecognition();
        
        setState(SessionState.IDLE);
    }
    
    /**
     * 开始语音识别
     */
    private void startSpeechRecognition() {
        setState(SessionState.TRIGGER_HANDLING);
        log("开始语音识别...");
        
        SpeechRecognizer recognizer = speechServices.getSpeechRecognizer();
        SilenceDetector silenceDetector = speechServices.getSilenceDetector();
        
        // 设置静音检测
        silenceDetector.startDetection(new SilenceListener() {
            @Override
            public void onSilenceStart() {}
            
            @Override
            public void onSilenceEnd() {}
            
            @Override
            public void onSilenceTimeout(long durationMs) {
                log("检测到静音超时，结束语音输入");
                recognizer.stopRecognition();
            }
        });
        
        // 开始语音识别
        recognizer.startRecognition(new RecognitionListener() {
            @Override
            public void onPartialResult(String partialText) {
                log("识别中: " + partialText);
            }
            
            @Override
            public void onResult(String finalText) {
                log("识别结果: " + finalText);
                notifySpeechRecognized(finalText);
                generateAnswer(finalText);
            }
            
            @Override
            public void onError(String error) {
                log("识别错误: " + error);
                notifyError(error);
                setState(SessionState.MONITORING);
            }
        });
    }
    
    /**
     * 生成 AI 回答
     */
    private void generateAnswer(String question) {
        log("正在生成回答...");
        
        String prompt = PromptTemplate.buildPrompt(question);
        
        llmClient.generateAnswerAsync(prompt, new AnswerListener() {
            @Override
            public void onToken(String token) {
                notifyAnswerToken(token);
            }
            
            @Override
            public void onComplete(String answer) {
                log("回答完成");
                notifyAnswerComplete(answer);
                setState(SessionState.MONITORING);
            }
            
            @Override
            public void onError(String error) {
                log("生成回答出错: " + error);
                notifyError(error);
                setState(SessionState.MONITORING);
            }
        });
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopMonitoring();
        if (audioRecorder != null) {
            audioRecorder.release();
        }
        speechServices.releaseAll();
    }
    
    // ========== 内部方法 ==========
    
    private void setState(SessionState newState) {
        SessionState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            logger.info("状态变化: {} -> {}", oldState, newState);
            for (SessionListener listener : listeners) {
                platform.runOnMainThread(() -> listener.onStateChanged(oldState, newState));
            }
        }
    }
    
    private void log(String message) {
        logger.info(message);
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onLog(message));
        }
    }
    
    private void notifyWakeWordDetected(String keyword) {
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onWakeWordDetected(keyword));
        }
    }
    
    private void notifySpeechRecognized(String text) {
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onSpeechRecognized(text));
        }
    }
    
    private void notifyAnswerToken(String token) {
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onAnswerToken(token));
        }
    }
    
    private void notifyAnswerComplete(String answer) {
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onAnswerComplete(answer));
        }
    }
    
    private void notifyError(String error) {
        for (SessionListener listener : listeners) {
            platform.runOnMainThread(() -> listener.onError(error));
        }
    }
}
