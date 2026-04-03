package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.classroomassistant.core.speech.KeywordPreprocessor;
import com.classroomassistant.core.speech.WakeWordListener;
import com.classroomassistant.core.speech.WakeWordDetector;

/**
 * Sherpa-ONNX 唤醒词检测器 (KWS) 实现类
 *
 * <p>基于 Sherpa-ONNX 框架实现。通过 JNI 调用底层 ONNX Runtime 进行关键字检测（Keyword Spotting）。
 * 该类负责在音频流中识别预设的唤醒词，并触发通知。
 *
 * <p>支持中文唤醒词自动预处理：将中文关键词转换为拼音 tokens，与 KWS 模型词表兼容。
 *
 * <p>注意：在不再使用时，务必调用 {@link #close()} 释放 JNI 持有的 native 句柄。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SherpaWakeWordDetector implements WakeWordDetector, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SherpaWakeWordDetector.class);

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final List<WakeWordListener> listeners = new CopyOnWriteArrayList<>();

    private volatile long handle;

    /**
     * 初始化唤醒词引擎
     *
     * @param modelDir 模型目录路径（应包含 encoder, decoder, joiner 等模型文件）
     * @param keywords 唤醒词列表，多个关键词用英文逗号或空格分隔
     * @throws NullPointerException 如果 modelDir 为 null
     * @throws RuntimeException     如果 JNI 初始化失败
     */
    @Override
    public void initialize(Path modelDir, String keywords) {
        Objects.requireNonNull(modelDir, "模型目录不能为空");
        SherpaOnnxJNI.ensureLoaded();
        
        // 预处理中文唤醒词，转换为拼音 tokens
        String processedKeywords = preprocessKeywords(keywords);
        
        logger.info("唤醒词初始化: 原始={}, 处理后={}, 模型目录={}", 
            keywords, processedKeywords, modelDir);
        
        handle = jni.initializeKws(modelDir, processedKeywords == null ? "" : processedKeywords);
        logger.info("唤醒词检测器初始化完成");
    }

    /**
     * 预处理关键词，将中文转为拼音 tokens
     * 
     * @param keywords 原始关键词（可能包含中文）
     * @return 处理后的 token 字符串，用逗号分隔多个关键词
     */
    private String preprocessKeywords(String keywords) {
        if (keywords == null || keywords.trim().isEmpty()) {
            logger.warn("唤醒词为空");
            return "";
        }
        
        try {
            List<String> processed = KeywordPreprocessor.preprocessKeywords(keywords);
            if (processed.isEmpty()) {
                logger.warn("唤醒词预处理结果为空，使用原值");
                return keywords;
            }
            
            String result = KeywordPreprocessor.mergeProcessedKeywords(processed);
            logger.info("唤醒词预处理成功: {} → {}", keywords, result);
            return result;
        } catch (Exception e) {
            logger.error("唤醒词预处理异常，将使用原值", e);
            return keywords;
        }
    }

    /**
     * 在音频帧中检测唤醒词
     *
     * @param frame 音频采样数据（单声道，float 格式）
     * @return 如果检测到任一唤醒词返回 true，否则返回 false
     */
    @Override
    public boolean detect(float[] frame) {
        if (handle == 0 || frame == null || frame.length == 0) {
            return false;
        }
        boolean detected = jni.detectWakeWord(handle, frame, frame.length);
        if (detected) {
            notifyDetected();
        }
        return detected;
    }

    /**
     * 注册唤醒词监听器
     *
     * @param listener 监听器实例
     */
    @Override
    public void addListener(WakeWordListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 触发唤醒词检测成功通知
     */
    private void notifyDetected() {
        for (WakeWordListener listener : listeners) {
            listener.onWakeWordDetected("检测到唤醒词");
        }
    }

    /**
     * 释放资源，销毁 JNI 句柄
     */
    @Override
    public void close() {
        if (handle != 0) {
            jni.release(handle);
            handle = 0;
        }
    }
}
