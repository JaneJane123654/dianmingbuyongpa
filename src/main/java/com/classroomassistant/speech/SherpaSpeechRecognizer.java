package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sherpa-ONNX 语音识别 (ASR) 实现类
 *
 * <p>基于 Sherpa-ONNX 框架实现。通过 JNI 调用底层 ONNX Runtime 进行推理。
 * 本类支持同步和异步两种识别方式。
 *
 * <p>注意：在不再使用时，务必调用 {@link #close()} 释放 JNI 持有的 native 句柄和后台线程资源。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SherpaSpeechRecognizer implements SpeechRecognizer, AutoCloseable {

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sherpa-asr");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long handle;

    /**
     * 初始化 ASR 引擎
     *
     * @param modelDir 包含 ONNX 模型文件和配置的目录绝对路径
     * @throws RuntimeException 如果 JNI 加载或引擎初始化失败
     */
    @Override
    public void initialize(Path modelDir) {
        SherpaOnnxJNI.ensureLoaded();
        handle = jni.initializeAsr(modelDir);
    }

    /**
     * 同步识别音频数据
     *
     * @param pcm 原始 16-bit PCM 字节数据
     * @return 识别出的文本结果。如果识别失败或 handle 为空，则返回空字符串。
     */
    @Override
    public String recognize(byte[] pcm) {
        if (handle == 0 || pcm == null || pcm.length == 0) {
            return "";
        }
        return jni.recognize(handle, pcm);
    }

    /**
     * 异步识别音频数据
     * <p>任务将被提交到专用的单线程线程池中执行，避免阻塞 UI 线程。
     *
     * @param pcm      原始 16-bit PCM 字节数据
     * @param listener 识别结果监听器，用于接收结果或错误回调
     */
    @Override
    public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
        executor.submit(() -> {
            if (listener == null) {
                return;
            }
            try {
                listener.onResult(recognize(pcm));
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        });
    }

    /**
     * 释放 Native 资源和关闭后台线程池
     */
    @Override
    public void close() {
        if (handle != 0) {
            jni.release(handle);
            handle = 0;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
