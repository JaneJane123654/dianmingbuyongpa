package com.classroomassistant.performance;

import static org.junit.jupiter.api.Assertions.*;

import com.classroomassistant.ai.CircuitBreaker;
import com.classroomassistant.ai.PromptTemplate;
import com.classroomassistant.audio.AudioFormatSpec;
import com.classroomassistant.audio.CircularBuffer;
import com.classroomassistant.utils.audio.AudioUtils;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 核心组件性能测试 (Core Component Performance Tests)
 *
 * <p>对系统核心组件进行性能基准测试，验证在高负载下的表现。
 * 支持中英双语输出，通过 application.properties 中的 app.language 配置切换。
 *
 * <p>运行方式：
 * <pre>
 * mvn test -Dtest=CorePerformanceTest -Dgroups=performance
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("performance")
class CorePerformanceTest {

    private static PerformanceTestMessages msg;
    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;

    @BeforeAll
    static void initMessages() {
        msg = MessageFactory.getMessages();
    }

    @BeforeEach
    void warmup() {
        // JVM 预热
        System.gc();
    }

    @AfterEach
    void cleanup() {
        System.gc();
    }

    // ================== CircularBuffer 性能测试 ==================

    @Test
    @DisplayName("CircularBuffer Write Performance / 环形缓冲区写入性能")
    void circularBufferWritePerformance() {
        String testName = msg.circularBufferWritePerformance();
        System.out.println(msg.testStarted(testName));

        CircularBuffer buffer = new CircularBuffer(10 * 1024 * 1024); // 10MB
        byte[] data = new byte[640]; // 20ms @ 16kHz, 16-bit, mono
        new Random().nextBytes(data);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buffer.write(data);
        }

        // 基准测试
        PerformanceResult result = PerformanceResult.builder()
            .operation(testName)
            .iterations(BENCHMARK_ITERATIONS)
            .startTimer()
            .build();

        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            buffer.write(data);
        }
        long end = System.nanoTime();

        result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        // 断言：写入性能应达到至少 100,000 ops/sec
        assertTrue(result.getThroughput() > 100_000, msg.shouldHandleOperationsPerSecond(100_000));
    }

    @Test
    @DisplayName("CircularBuffer Read Performance / 环形缓冲区读取性能")
    void circularBufferReadPerformance() {
        String testName = msg.circularBufferReadPerformance();
        System.out.println(msg.testStarted(testName));

        CircularBuffer buffer = new CircularBuffer(10 * 1024 * 1024);
        byte[] data = new byte[640];
        new Random().nextBytes(data);

        // 填充数据
        for (int i = 0; i < 5000; i++) {
            buffer.write(data);
        }

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buffer.readLatestBytes(640);
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            buffer.readLatestBytes(640);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 50_000, msg.shouldHandleOperationsPerSecond(50_000));
    }

    @Test
    @DisplayName("CircularBuffer Concurrent Performance / 环形缓冲区并发性能")
    void circularBufferConcurrentPerformance() throws Exception {
        String testName = msg.circularBufferConcurrentPerformance();
        System.out.println(msg.testStarted(testName));

        CircularBuffer buffer = new CircularBuffer(10 * 1024 * 1024);
        byte[] data = new byte[640];
        new Random().nextBytes(data);

        int writerThreads = 2;
        int readerThreads = 2;
        int operationsPerThread = 5000;
        
        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch latch = new CountDownLatch(writerThreads + readerThreads);
        AtomicInteger totalOps = new AtomicInteger(0);

        long start = System.nanoTime();

        // 启动写入线程
        for (int w = 0; w < writerThreads; w++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        buffer.write(data);
                        totalOps.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 启动读取线程
        for (int r = 0; r < readerThreads; r++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        buffer.readLatestBytes(320);
                        totalOps.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Concurrent test should complete within 30 seconds");
        long end = System.nanoTime();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        PerformanceResult result = new PerformanceResult(testName, totalOps.get(), end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 10_000, msg.shouldHandleOperationsPerSecond(10_000));
    }

    // ================== AudioUtils 性能测试 ==================

    @Test
    @DisplayName("Audio Format Conversion Performance / 音频格式转换性能")
    void audioFormatConversionPerformance() {
        String testName = msg.audioFormatConversionPerformance();
        System.out.println(msg.testStarted(testName));

        // 1秒音频数据 @ 16kHz, 16-bit, mono = 32000 bytes
        byte[] pcmData = new byte[32000];
        new Random().nextBytes(pcmData);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            AudioUtils.pcmToFloat(pcmData);
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            AudioUtils.pcmToFloat(pcmData);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        // 应能处理至少 1000 次/秒（每次处理 1 秒音频）
        assertTrue(result.getThroughput() > 1_000, msg.shouldHandleOperationsPerSecond(1_000));
    }

    // ================== PromptTemplate 性能测试 ==================

    @Test
    @DisplayName("Prompt Template Build Performance / 提示词模板构建性能")
    void promptTemplateBuildPerformance() {
        String testName = msg.promptTemplateBuildPerformance();
        System.out.println(msg.testStarted(testName));

        PromptTemplate template = new PromptTemplate();
        String content = "这是一段课堂内容，包含了老师讲解的知识点。" +
            "今天我们学习的是Java编程语言的基础知识，包括变量、数据类型和控制结构。";

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            template.build(content);
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            template.build(content);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 100_000, msg.shouldHandleOperationsPerSecond(100_000));
    }

    // ================== CircuitBreaker 性能测试 ==================

    @Test
    @DisplayName("Circuit Breaker State Transition Performance / 熔断器状态转换性能")
    void circuitBreakerStateTransitionPerformance() {
        String testName = msg.circuitBreakerStateTransitionPerformance();
        System.out.println(msg.testStarted(testName));

        // 预热
        for (int i = 0; i < 100; i++) {
            CircuitBreaker cb = new CircuitBreaker(3, Duration.ofMillis(100));
            for (int j = 0; j < 10; j++) {
                cb.allowRequest();
                cb.recordSuccess();
            }
        }

        // 基准测试
        int iterations = 50000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            CircuitBreaker cb = new CircuitBreaker(3, Duration.ofMillis(100));
            cb.allowRequest();
            cb.recordSuccess();
            cb.allowRequest();
            cb.recordFailure();
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, iterations, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 50_000, msg.shouldHandleOperationsPerSecond(50_000));
    }

    // ================== AudioFormatSpec 性能测试 ==================

    @Test
    @DisplayName("AudioFormatSpec Calculation Performance / 音频格式计算性能")
    void audioFormatSpecCalculationPerformance() {
        String testName = "AudioFormatSpec Calculation / 音频格式计算";
        System.out.println(msg.testStarted(testName));

        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            spec.bytesPerSecond();
            spec.frameBytes();
            spec.toAudioFormat();
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            spec.bytesPerSecond();
            spec.frameBytes();
            spec.toAudioFormat();
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 500_000, msg.shouldHandleOperationsPerSecond(500_000));
    }

    // ================== 内存性能测试 ==================

    @Test
    @DisplayName("Memory Usage Under Load / 高负载内存使用")
    void memoryUsageUnderLoad() {
        String testName = "Memory Usage Under Load / 高负载内存使用";
        System.out.println(msg.testStarted(testName));

        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // 创建多个大缓冲区
        CircularBuffer[] buffers = new CircularBuffer[10];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new CircularBuffer(1024 * 1024); // 1MB each
        }

        // 填充数据
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        for (CircularBuffer buffer : buffers) {
            for (int i = 0; i < 1000; i++) {
                buffer.write(data);
            }
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        System.out.println(msg.memoryUsed(memoryUsed));

        // 10MB 缓冲区 + 开销，不应超过 15MB
        long maxExpectedMemory = 15 * 1024 * 1024;
        assertTrue(memoryUsed < maxExpectedMemory, msg.shouldNotExceedMemory(maxExpectedMemory));
    }

    // ================== 延迟测试 ==================

    @Test
    @DisplayName("Latency Percentiles / 延迟百分位")
    void latencyPercentiles() {
        String testName = "Latency Percentiles / 延迟百分位";
        System.out.println(msg.testStarted(testName));

        CircularBuffer buffer = new CircularBuffer(1024 * 1024);
        byte[] data = new byte[640];
        new Random().nextBytes(data);

        int iterations = 10000;
        long[] latencies = new long[iterations];

        // 预热
        for (int i = 0; i < 1000; i++) {
            buffer.write(data);
        }

        // 收集延迟数据
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            buffer.write(data);
            latencies[i] = System.nanoTime() - start;
        }

        // 排序计算百分位
        java.util.Arrays.sort(latencies);
        
        double p50 = latencies[(int) (iterations * 0.50)] / 1000.0; // 微秒
        double p90 = latencies[(int) (iterations * 0.90)] / 1000.0;
        double p99 = latencies[(int) (iterations * 0.99)] / 1000.0;
        double p999 = latencies[(int) (iterations * 0.999)] / 1000.0;

        System.out.println("P50:  " + String.format("%.2f", p50) + " μs");
        System.out.println("P90:  " + String.format("%.2f", p90) + " μs");
        System.out.println("P99:  " + String.format("%.2f", p99) + " μs");
        System.out.println("P99.9:" + String.format("%.2f", p999) + " μs");

        // P99 延迟应小于 1ms
        assertTrue(p99 < 1000, "P99 latency should be less than 1ms");
    }
}
