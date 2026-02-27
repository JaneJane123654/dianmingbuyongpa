package com.classroomassistant.benchmark;

import static com.classroomassistant.benchmark.BenchmarkMessages.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 存储模块性能基准测试 (Storage Module Performance Benchmark)
 *
 * <p>测试存储相关组件的性能表现，包括：
 * <ul>
 *   <li>用户偏好构建性能</li>
 *   <li>配置读取性能</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("benchmark")
class StorageBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 50000;

    @BeforeAll
    static void setup() {
        String lang = System.getProperty("benchmark.language", "zh");
        BenchmarkMessages.setLanguage(lang);
    }

    @Test
    @DisplayName("UserPreferences 构建性能 / UserPreferences Build Performance")
    void benchmarkUserPreferencesBuild() {
        System.out.println("\n【存储模块性能测试】/ [Storage Module Benchmark]");
        System.out.println(">>> UserPreferences Builder");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.classroomassistant.storage.UserPreferences.builder()
                .keywords("张三,李四")
                .vadEnabled(true)
                .vadQuietThresholdSeconds(5)
                .audioLookbackSeconds(240)
                .recordingSaveEnabled(true)
                .recordingRetentionDays(7)
                .aiTokenPlainText("test-token")
                .build();
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.classroomassistant.storage.UserPreferences.builder()
                .keywords("张三,李四")
                .vadEnabled(true)
                .vadQuietThresholdSeconds(5)
                .audioLookbackSeconds(240)
                .recordingSaveEnabled(true)
                .recordingRetentionDays(7)
                .aiTokenPlainText("test-token")
                .build();
        }
        long endTime = System.nanoTime();

        printResults("UserPreferences Build", BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("Validator 性能测试 / Validator Performance")
    void benchmarkValidator() {
        System.out.println("\n>>> Validator.normalizeKeywords");

        String input = "张三, 李四，王五 , 赵六";

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.classroomassistant.utils.Validator.normalizeKeywords(input);
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.classroomassistant.utils.Validator.normalizeKeywords(input);
        }
        long endTime = System.nanoTime();

        printResults("Validator.normalizeKeywords", BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("Validator 范围校验性能 / Validator Range Check Performance")
    void benchmarkValidatorRange() {
        System.out.println("\n>>> Validator.requireRange");

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            com.classroomassistant.utils.Validator.requireRange(50, 1, 100, "test");
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            com.classroomassistant.utils.Validator.requireRange(50, 1, 100, "test");
        }
        long endTime = System.nanoTime();

        printResults("Validator.requireRange", BENCHMARK_ITERATIONS, endTime - startTime);
    }

    private void printResults(String testName, int iterations, long totalNanos) {
        double totalMs = totalNanos / 1_000_000.0;
        double avgNs = (double) totalNanos / iterations;
        double opsPerSec = iterations / (totalMs / 1000.0);

        System.out.println("----------------------------------------");
        System.out.printf("  %s: %d%n", get("benchmark.iterations"), iterations);
        System.out.printf("  %s: %.2f %s%n", get("benchmark.total.time"), totalMs, get("benchmark.ms"));
        System.out.printf("  %s: %.0f %s%n", get("benchmark.avg.time"), avgNs, get("benchmark.ns"));
        System.out.printf("  %s: %.0f%n", get("benchmark.ops.per.sec"), opsPerSec);
        System.out.println("----------------------------------------");
    }
}
