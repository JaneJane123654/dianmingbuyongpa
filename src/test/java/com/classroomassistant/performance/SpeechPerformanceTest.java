package com.classroomassistant.performance;

import static org.junit.jupiter.api.Assertions.*;

import com.classroomassistant.audio.AudioFormatSpec;
import com.classroomassistant.audio.CircularBuffer;
import com.classroomassistant.speech.RecognitionListener;
import com.classroomassistant.speech.SilenceDetector;
import com.classroomassistant.speech.SilenceListener;
import com.classroomassistant.speech.SpeechRecognizer;
import com.classroomassistant.speech.WakeWordDetector;
import com.classroomassistant.speech.WakeWordListener;
import com.classroomassistant.utils.audio.AudioUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 语音模块性能测试 (Speech Module Performance Tests)
 *
 * <p>对语音处理模块进行性能基准测试，包括：
 * <ul>
 *   <li>唤醒词检测模拟性能</li>
 *   <li>静音检测（VAD）模拟性能</li>
 *   <li>语音识别模拟性能</li>
 *   <li>音频处理流水线性能</li>
 * </ul>
 *
 * <p>支持中英双语输出，通过 application.properties 中的 app.language 配置切换。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("performance")
class SpeechPerformanceTest {

    private static PerformanceTestMessages msg;
    private static final int WARMUP_ITERATIONS = 500;
    private static final int BENCHMARK_ITERATIONS = 10000;

    @BeforeAll
    static void initMessages() {
        msg = MessageFactory.getMessages();
    }

    @BeforeEach
    void warmup() {
        System.gc();
    }

    @AfterEach
    void cleanup() {
        System.gc();
    }

    // ================== 唤醒词检测性能测试 ==================

    @Test
    @DisplayName("Wake Word Detection Mock Performance / 唤醒词检测模拟性能")
    void wakeWordDetectionMockPerformance() {
        String testName = "Wake Word Detection Mock / 唤醒词检测模拟";
        System.out.println(msg.testStarted(testName));

        MockWakeWordDetector detector = new MockWakeWordDetector();
        detector.initialize(Path.of("mock"), "张三,李四");

        // 生成模拟音频帧 (20ms @ 16kHz = 320 samples)
        float[] frame = new float[320];
        Random random = new Random();
        for (int i = 0; i < frame.length; i++) {
            frame[i] = random.nextFloat() * 2 - 1; // -1 to 1
        }

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            detector.detect(frame);
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            detector.detect(frame);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        // 每帧 20ms，需处理 50 帧/秒 = 实时，目标至少 500 帧/秒 (10x 实时)
        assertTrue(result.getThroughput() > 500, msg.shouldHandleOperationsPerSecond(500));
    }

    @Test
    @DisplayName("Silence Detection (VAD) Mock Performance / 静音检测模拟性能")
    void silenceDetectionMockPerformance() {
        String testName = msg.speechRecognitionMockPerformance();
        System.out.println(msg.testStarted(testName));

        MockSilenceDetector detector = new MockSilenceDetector();
        detector.initialize(Path.of("mock"));
        detector.setQuietThresholdSeconds(5);

        float[] frame = new float[320];
        Random random = new Random();
        for (int i = 0; i < frame.length; i++) {
            frame[i] = random.nextFloat() * 0.1f; // 低音量模拟静音
        }

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            detector.detect(frame, 20);
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            detector.detect(frame, 20);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 500, msg.shouldHandleOperationsPerSecond(500));
    }

    @Test
    @DisplayName("Speech Recognition Mock Performance / 语音识别模拟性能")
    void speechRecognitionMockPerformance() {
        String testName = "Speech Recognition Mock / 语音识别模拟";
        System.out.println(msg.testStarted(testName));

        MockSpeechRecognizer recognizer = new MockSpeechRecognizer(50); // 50ms 模拟延迟
        recognizer.initialize(Path.of("mock"));

        // 生成 1 秒音频数据
        byte[] pcm = new byte[32000];
        new Random().nextBytes(pcm);

        // 预热
        for (int i = 0; i < 10; i++) {
            recognizer.recognize(pcm);
        }

        // 基准测试（由于模拟延迟，减少迭代）
        int iterations = 100;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String result = recognizer.recognize(pcm);
            assertNotNull(result);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, iterations, end - start, 0);
        System.out.println(result.toReport());

        // 模拟 50ms 延迟，约 15-20 次/秒
        assertTrue(result.getThroughput() > 10, msg.shouldHandleOperationsPerSecond(10));
    }

    @Test
    @DisplayName("Audio Pipeline Performance / 音频处理流水线性能")
    void audioPipelinePerformance() {
        String testName = "Audio Pipeline / 音频处理流水线";
        System.out.println(msg.testStarted(testName));

        // 模拟完整的音频处理流水线
        CircularBuffer buffer = new CircularBuffer(5 * 1024 * 1024);
        MockWakeWordDetector wakeWordDetector = new MockWakeWordDetector();
        MockSilenceDetector silenceDetector = new MockSilenceDetector();
        
        wakeWordDetector.initialize(Path.of("mock"), "张三");
        silenceDetector.initialize(Path.of("mock"));
        silenceDetector.setQuietThresholdSeconds(5);

        // 生成 20ms 音频帧
        byte[] pcmFrame = new byte[640]; // 20ms @ 16kHz, 16-bit, mono
        new Random().nextBytes(pcmFrame);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buffer.write(pcmFrame);
            float[] floatFrame = AudioUtils.pcmToFloat(pcmFrame);
            wakeWordDetector.detect(floatFrame);
            silenceDetector.detect(floatFrame, 20);
        }

        // 基准测试：完整流水线
        int iterations = 50000; // 模拟约 1000 秒音频
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            // 1. 写入缓冲区
            buffer.write(pcmFrame);
            // 2. PCM 转浮点
            float[] floatFrame = AudioUtils.pcmToFloat(pcmFrame);
            // 3. 唤醒词检测
            wakeWordDetector.detect(floatFrame);
            // 4. 静音检测
            silenceDetector.detect(floatFrame, 20);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, iterations, end - start, 0);
        System.out.println(result.toReport());

        // 每帧 20ms = 50 帧/秒 实时，目标至少 500 帧/秒 (10x 实时)
        assertTrue(result.getThroughput() > 500, msg.shouldHandleOperationsPerSecond(500));
    }

    @Test
    @DisplayName("Concurrent Detection Performance / 并发检测性能")
    void concurrentDetectionPerformance() throws Exception {
        String testName = "Concurrent Detection / 并发检测";
        System.out.println(msg.testStarted(testName));

        int threads = 4;
        int framesPerThread = 5000;
        
        MockWakeWordDetector wakeWordDetector = new MockWakeWordDetector();
        MockSilenceDetector silenceDetector = new MockSilenceDetector();
        wakeWordDetector.initialize(Path.of("mock"), "张三");
        silenceDetector.initialize(Path.of("mock"));

        float[] frame = new float[320];
        new Random().nextFloats(frame);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalOps = new AtomicInteger(0);

        long start = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < framesPerThread; i++) {
                        wakeWordDetector.detect(frame);
                        silenceDetector.detect(frame, 20);
                        totalOps.addAndGet(2);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Concurrent test should complete");
        long end = System.nanoTime();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        PerformanceResult result = new PerformanceResult(testName, totalOps.get(), end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 10_000, msg.shouldHandleOperationsPerSecond(10_000));
    }

    @Test
    @DisplayName("Audio Buffer Lookback Performance / 音频缓冲区回溯性能")
    void audioBufferLookbackPerformance() {
        String testName = "Audio Buffer Lookback / 音频缓冲区回溯";
        System.out.println(msg.testStarted(testName));

        // 创建 5 分钟缓冲区
        int bytesPerSecond = 32000; // 16kHz, 16-bit, mono
        int bufferSeconds = 300;
        CircularBuffer buffer = new CircularBuffer(bytesPerSecond * bufferSeconds);

        // 填充 4 分钟数据
        byte[] frame = new byte[640];
        new Random().nextBytes(frame);
        int framesToFill = (bytesPerSecond * 240) / frame.length;
        for (int i = 0; i < framesToFill; i++) {
            buffer.write(frame);
        }

        // 预热
        for (int i = 0; i < 100; i++) {
            buffer.readLatestBytes(bytesPerSecond * 60); // 回溯 1 分钟
        }

        // 基准测试：回溯不同时长
        int iterations = 1000;
        int[] lookbackSeconds = {30, 60, 120, 240};
        
        for (int seconds : lookbackSeconds) {
            int bytesToRead = bytesPerSecond * seconds;
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                byte[] data = buffer.readLatestBytes(bytesToRead);
                assertNotNull(data);
            }
            long end = System.nanoTime();

            double durationMs = (end - start) / 1_000_000.0;
            double avgMs = durationMs / iterations;
            System.out.println("Lookback " + seconds + "s: avg " + String.format("%.3f", avgMs) + " ms");
        }
    }

    // ================== Mock 实现 ==================

    private static class MockWakeWordDetector implements WakeWordDetector {
        private final List<WakeWordListener> listeners = new ArrayList<>();
        private String keywords;

        @Override
        public void initialize(Path modelDir, String keywords) {
            this.keywords = keywords;
        }

        @Override
        public boolean detect(float[] frame) {
            // 模拟检测逻辑：简单计算 RMS
            float sum = 0;
            for (float f : frame) {
                sum += f * f;
            }
            float rms = (float) Math.sqrt(sum / frame.length);
            // 永远返回 false，除非 RMS 特别高（模拟唤醒词检测）
            return rms > 0.9f;
        }

        @Override
        public void addListener(WakeWordListener listener) {
            listeners.add(listener);
        }
    }

    private static class MockSilenceDetector implements SilenceDetector {
        private final List<SilenceListener> listeners = new ArrayList<>();
        private int quietThresholdMs = 5000;
        private int quietAccumulatedMs = 0;

        @Override
        public void initialize(Path modelFile) {
            // 无操作
        }

        @Override
        public boolean detect(float[] frame, int durationMillis) {
            // 模拟 VAD：简单计算 RMS
            float sum = 0;
            for (float f : frame) {
                sum += f * f;
            }
            float rms = (float) Math.sqrt(sum / frame.length);
            
            boolean hasSpeech = rms > 0.1f;
            if (!hasSpeech) {
                quietAccumulatedMs += durationMillis;
                if (quietAccumulatedMs >= quietThresholdMs) {
                    quietAccumulatedMs = 0;
                    for (SilenceListener listener : listeners) {
                        listener.onSilenceTimeout();
                    }
                }
            } else {
                quietAccumulatedMs = 0;
            }
            return hasSpeech;
        }

        @Override
        public void setQuietThresholdSeconds(int seconds) {
            this.quietThresholdMs = seconds * 1000;
        }

        @Override
        public void addListener(SilenceListener listener) {
            listeners.add(listener);
        }
    }

    private static class MockSpeechRecognizer implements SpeechRecognizer {
        private final long simulatedDelayMs;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        MockSpeechRecognizer(long simulatedDelayMs) {
            this.simulatedDelayMs = simulatedDelayMs;
        }

        @Override
        public void initialize(Path modelDir) {
            // 无操作
        }

        @Override
        public String recognize(byte[] pcm) {
            if (simulatedDelayMs > 0) {
                try {
                    Thread.sleep(simulatedDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "这是模拟的语音识别结果，音频长度: " + pcm.length + " 字节";
        }

        @Override
        public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
            executor.submit(() -> {
                try {
                    String result = recognize(pcm);
                    listener.onResult(result);
                } catch (Exception e) {
                    listener.onError(e.getMessage());
                }
            });
        }
    }
}
