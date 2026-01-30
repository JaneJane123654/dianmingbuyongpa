package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sherpa-ONNX 语音识别实现
 */
public class SherpaSpeechRecognizer implements SpeechRecognizer, AutoCloseable {

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sherpa-asr");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long handle;

    @Override
    public void initialize(Path modelDir) {
        SherpaOnnxJNI.ensureLoaded();
        handle = jni.initializeAsr(modelDir);
    }

    @Override
    public String recognize(byte[] pcm) {
        if (handle == 0 || pcm == null || pcm.length == 0) {
            return "";
        }
        return jni.recognize(handle, pcm);
    }

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

    @Override
    public void close() {
        if (handle != 0) {
            jni.release(handle);
            handle = 0;
        }
        executor.shutdown();
    }
}
