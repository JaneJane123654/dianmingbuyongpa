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
 * 桌面端音频录制器 (Desktop Audio Recorder Implementation)
 *
 * <p>基于 Java Sound API 实现。负责：
 * <ul>
 *   <li>开启并管理系统默认麦克风的音频采集。</li>
 *   <li>将采集到的原始 PCM 数据实时存入 {@link CircularBuffer}，支持历史音频回溯。</li>
 *   <li>多线程模型：在独立的后台线程中执行采集循环，避免阻塞主线程。</li>
 *   <li>健康监控：定期向 {@link HealthMonitor} 上报采集心跳，便于故障自发现。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
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

    /**
     * 构造桌面端录音器
     *
     * @param audioConfig   音频参数配置
     * @param healthMonitor 系统健康监控器
     * @throws NullPointerException 如果 audioConfig 为 null
     */
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

    /**
     * 启动异步采集任务
     */
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

    /**
     * 停止采集并释放资源
     */
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

    /**
     * 获取当前运行状态
     */
    @Override
    public boolean isRecording() {
        return recording.get();
    }

    /**
     * 从环形缓冲区回溯获取音频数据
     *
     * @param seconds 需要回溯的秒数（1-300）
     * @return 原始 PCM 字节数组
     */
    @Override
    public byte[] getAudioBefore(int seconds) {
        Validator.requireRange(seconds, 1, MAX_LOOKBACK_SECONDS, "回溯秒数");
        int length = formatSpec.bytesPerSecond() * seconds;
        return circularBuffer.readLatestBytes(length);
    }

    /**
     * 注册监听器
     */
    @Override
    public void addListener(AudioListener listener) {
        Objects.requireNonNull(listener, "监听器不能为空");
        listeners.add(listener);
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        stopRecording();
    }

    /**
     * 后台采集循环：持续读取 hardware buffer 并分发给内部 buffer 和监听器
     */
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

    /**
     * 打开麦克风输入行
     */
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

    /**
     * 分发音频数据给所有监听器
     */
    private void notifyAudioReady(byte[] data) {
        for (AudioListener listener : listeners) {
            try {
                listener.onAudioReady(data);
            } catch (Exception e) {
                logger.warn("音频监听器处理失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 通知所有监听器发生了错误
     */
    private void notifyError(String message) {
        for (AudioListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (Exception e) {
                logger.warn("音频监听器错误处理失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 优雅停用后台线程池
     */
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
