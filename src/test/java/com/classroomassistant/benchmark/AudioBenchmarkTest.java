package com.classroomassistant.benchmark;

import static com.classroomassistant.benchmark.BenchmarkMessages.*;

import com.classroomassistant.audio.AudioFormatSpec;
import com.classroomassistant.audio.CircularBuffer;
import com.classroomassistant.audio.WavWriter;
import com.classroomassistant.utils.audio.AudioUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 音频模块性能基准测试 (Audio Module Performance Benchmark)
 *
 * <p>测试音频相关组件的性能表现，包括：
 * <ul>
 *   <li>环形缓冲区读写性能</li>
 *   <li>WAV 文件写入性能</li>
 *   <li>PCM 数据转换性能</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("benchmark")
class AudioBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 10000;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setup() {
        // 从系统属性读取语言配置，默认中文
        String lang = System.getProperty("benchmark.language", "zh");
        BenchmarkMessages.setLanguage(lang);
    }

    @Test
    @DisplayName("环形缓冲区写入性能 / Circular Buffer Write Performance")
    void benchmarkCircularBufferWrite() {
        System.out.println("\n" + get("benchmark.audio.title"));
        System.out.println(">>> " + get("benchmark.buffer.write"));

        // 10MB 缓冲区
        CircularBuffer buffer = new CircularBuffer(10 * 1024 * 1024);
        byte[] data = new byte[640]; // 20ms @ 16kHz 16bit mono

        // 预热
        System.out.println(get("benchmark.warmup"));
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buffer.write(data);
        }

        // 基准测试
        System.out.println(get("benchmark.running"));
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            buffer.write(data);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.buffer.write"), BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("环形缓冲区读取性能 / Circular Buffer Read Performance")
    void benchmarkCircularBufferRead() {
        System.out.println("\n>>> " + get("benchmark.buffer.read"));

        CircularBuffer buffer = new CircularBuffer(10 * 1024 * 1024);
        byte[] data = new byte[640];
        
        // 填充数据
        for (int i = 0; i < 1000; i++) {
            buffer.write(data);
        }

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            buffer.readLatestBytes(32000); // 1秒数据
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            buffer.readLatestBytes(32000);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.buffer.read"), BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("WAV 文件写入性能 / WAV File Write Performance")
    void benchmarkWavWrite() throws Exception {
        System.out.println("\n>>> " + get("benchmark.wav.write"));

        AudioFormatSpec format = new AudioFormatSpec(16000, 1, 16, 20);
        byte[] pcm = new byte[32000 * 10]; // 10秒音频

        int iterations = 100; // WAV 写入较慢，减少迭代次数

        // 预热
        for (int i = 0; i < 10; i++) {
            Path file = tempDir.resolve("warmup_" + i + ".wav");
            WavWriter.write(file, pcm, format);
            Files.delete(file);
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Path file = tempDir.resolve("bench_" + i + ".wav");
            WavWriter.write(file, pcm, format);
            Files.delete(file);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.wav.write") + " (10s audio)", iterations, endTime - startTime);
    }

    @Test
    @DisplayName("PCM 转浮点数性能 / PCM to Float Conversion Performance")
    void benchmarkPcmToFloat() {
        System.out.println("\n>>> " + get("benchmark.pcm.convert"));

        byte[] pcm = new byte[32000]; // 1秒数据

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            AudioUtils.pcmToFloat(pcm);
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            AudioUtils.pcmToFloat(pcm);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.pcm.convert") + " (1s audio)", BENCHMARK_ITERATIONS, endTime - startTime);
    }

    private void printResults(String testName, int iterations, long totalNanos) {
        double totalMs = totalNanos / 1_000_000.0;
        double avgUs = totalNanos / 1000.0 / iterations;
        double opsPerSec = iterations / (totalMs / 1000.0);

        System.out.println("----------------------------------------");
        System.out.printf("  %s: %d%n", get("benchmark.iterations"), iterations);
        System.out.printf("  %s: %.2f %s%n", get("benchmark.total.time"), totalMs, get("benchmark.ms"));
        System.out.printf("  %s: %.2f %s%n", get("benchmark.avg.time"), avgUs, get("benchmark.us"));
        System.out.printf("  %s: %.0f%n", get("benchmark.ops.per.sec"), opsPerSec);
        System.out.println("----------------------------------------");
    }
}
