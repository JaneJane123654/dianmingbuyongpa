package com.classroomassistant.audio;

import com.classroomassistant.runtime.HealthMonitor;
import com.classroomassistant.storage.AudioConfig;
import com.classroomassistant.utils.Validator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端音频录制器（Java Sound API）
 *
 * <p>负责采集麦克风 PCM 数据并写入环形缓冲区，同时通知监听器。
 */
public class AudioRecorderDesktop implements AudioRecorder {

    private static final Logger logger = LoggerFactory.getLogger(AudioRecorderDesktop.class);

    private static final int MAX_LOOKBACK_SECONDS = 300;

    private final AudioFormatSpec formatSpec;
    private final CircularBuffer circularBuffer;
    private final HealthMonitor healthMonitor;
    private final List<AudioListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    private ExecutorService executorService;
    private TargetDataLine targetDataLine;

    public AudioRecorderDesktop(AudioConfig audioConfig, HealthMonitor healthMonitor) {
        AudioConfig config = Objects.requireNonNull(audioConfig, "音频配置不能为空");
        this.healthMonitor = healthMonitor;
        this.formatSpec = new AudioFormatSpec(
            config.sampleRate(),
            config.channels(),
            config.bitsPerSample(),
            config.frameMillis()
        );
        int bufferBytes = formatSpec.bytesPerSecond() * MAX_LOOKBACK_SECONDS;
        if (bufferBytes <= 0) {
            bufferBytes = 10 * 1024 * 1024;
        }
        this.circularBuffer = new CircularBuffer(bufferBytes);
    }

    @Override
    public void startRecording() {
        synchronized (lifecycleLock) {
            if (recording.get()) {
                return;
            }
            try {
                this.targetDataLine = openLine(formatSpec.toAudioFormat());
                this.executorService = Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "audio-capture-thread");
                    thread.setDaemon(true);
                    return thread;
                });
                this.recording.set(true);
                this.executorService.submit(this::captureLoop);
                logger.info("音频录制已启动");
            } catch (Exception e) {
                notifyError("启动录音失败: " + e.getMessage());
                logger.error("启动录音失败", e);
                stopRecording();
            }
        }
    }

    @Override
    public void stopRecording() {
        synchronized (lifecycleLock) {
            if (!recording.get()) {
                return;
            }
            recording.set(false);
            if (targetDataLine != null) {
                targetDataLine.stop();
                targetDataLine.close();
                targetDataLine = null;
            }
            shutdownExecutor();
            logger.info("音频录制已停止");
        }
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }

    @Override
    public byte[] getAudioBefore(int seconds) {
        Validator.requireRange(seconds, 1, MAX_LOOKBACK_SECONDS, "回溯秒数");
        int length = formatSpec.bytesPerSecond() * seconds;
        return circularBuffer.readLatestBytes(length);
    }

    @Override
    public void addListener(AudioListener listener) {
        Objects.requireNonNull(listener, "监听器不能为空");
        listeners.add(listener);
    }

    @Override
    public void close() {
        stopRecording();
    }

    private void captureLoop() {
        int frameBytes = formatSpec.frameBytes();
        byte[] frameBuffer = new byte[frameBytes];
        while (recording.get() && targetDataLine != null) {
            int read = targetDataLine.read(frameBuffer, 0, frameBuffer.length);
            if (read <= 0) {
                continue;
            }
            byte[] data = new byte[read];
            System.arraycopy(frameBuffer, 0, data, 0, read);
            circularBuffer.write(data);
            notifyAudioReady(data);
            if (healthMonitor != null) {
                healthMonitor.markAudioFrameReceived();
            }
        }
    }

    private TargetDataLine openLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("当前系统不支持麦克风采集");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        return line;
    }

    private void notifyAudioReady(byte[] data) {
        for (AudioListener listener : listeners) {
            try {
                listener.onAudioReady(data);
            } catch (Exception e) {
                logger.warn("音频监听器处理失败: {}", e.getMessage());
            }
        }
    }

    private void notifyError(String message) {
        for (AudioListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (Exception e) {
                logger.warn("音频监听器错误处理失败: {}", e.getMessage());
            }
        }
    }

    private void shutdownExecutor() {
        if (executorService == null) {
            return;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executorService = null;
        }
    }
}
