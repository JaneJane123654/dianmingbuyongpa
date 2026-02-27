package com.classroomassistant.benchmark;

import static com.classroomassistant.benchmark.BenchmarkMessages.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 综合性能基准测试运行器 (Comprehensive Performance Benchmark Runner)
 *
 * <p>运行所有模块的性能测试并生成汇总报告。
 *
 * <h3>运行方式 / How to Run:</h3>
 * <pre>
 * # 中文输出 (Chinese)
 * mvn test -Dtest=BenchmarkRunner -Dbenchmark.language=zh
 *
 * # 英文输出 (English)
 * mvn test -Dtest=BenchmarkRunner -Dbenchmark.language=en
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("benchmark")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BenchmarkRunner {

    private static long totalStartTime;
    private static int totalTests = 0;
    private static int passedTests = 0;

    @BeforeAll
    static void setup() {
        String lang = System.getProperty("benchmark.language", "zh");
        BenchmarkMessages.setLanguage(lang);
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            " + get("benchmark.title") + "                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Language / 语言: " + lang.toUpperCase());
        System.out.println("JVM: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println();
        
        totalStartTime = System.currentTimeMillis();
    }

    @Test
    @Order(1)
    @DisplayName("Audio: CircularBuffer Write")
    void audioBufferWrite() {
        runBenchmark("CircularBuffer Write", () -> {
            com.classroomassistant.audio.CircularBuffer buffer = 
                new com.classroomassistant.audio.CircularBuffer(10 * 1024 * 1024);
            byte[] data = new byte[640];
            
            for (int i = 0; i < 100000; i++) {
                buffer.write(data);
            }
        });
    }

    @Test
    @Order(2)
    @DisplayName("Audio: CircularBuffer Read")
    void audioBufferRead() {
        runBenchmark("CircularBuffer Read", () -> {
            com.classroomassistant.audio.CircularBuffer buffer = 
                new com.classroomassistant.audio.CircularBuffer(10 * 1024 * 1024);
            byte[] data = new byte[640];
            for (int i = 0; i < 1000; i++) {
                buffer.write(data);
            }
            
            for (int i = 0; i < 100000; i++) {
                buffer.readLatestBytes(32000);
            }
        });
    }

    @Test
    @Order(3)
    @DisplayName("Audio: PCM to Float")
    void audioPcmToFloat() {
        runBenchmark("PCM to Float", () -> {
            byte[] pcm = new byte[32000];
            
            for (int i = 0; i < 100000; i++) {
                com.classroomassistant.utils.audio.AudioUtils.pcmToFloat(pcm);
            }
        });
    }

    @Test
    @Order(4)
    @DisplayName("AI: Prompt Template Build")
    void aiPromptBuild() {
        runBenchmark("Prompt Template", () -> {
            com.classroomassistant.ai.PromptTemplate template = 
                new com.classroomassistant.ai.PromptTemplate();
            String text = "今天我们学习的是微积分的基本概念，导数是函数在某一点的瞬时变化率。";
            
            for (int i = 0; i < 100000; i++) {
                template.build(text);
            }
        });
    }

    @Test
    @Order(5)
    @DisplayName("AI: Circuit Breaker Check")
    void aiCircuitBreaker() {
        runBenchmark("Circuit Breaker", () -> {
            com.classroomassistant.ai.CircuitBreaker breaker = 
                new com.classroomassistant.ai.CircuitBreaker(3, java.time.Duration.ofSeconds(10));
            
            for (int i = 0; i < 100000; i++) {
                breaker.allowRequest();
            }
        });
    }

    @Test
    @Order(6)
    @DisplayName("Storage: UserPreferences Build")
    void storageUserPreferences() {
        runBenchmark("UserPreferences", () -> {
            for (int i = 0; i < 50000; i++) {
                com.classroomassistant.storage.UserPreferences.builder()
                    .keywords("张三")
                    .vadEnabled(true)
                    .vadQuietThresholdSeconds(5)
                    .build();
            }
        });
    }

    @Test
    @Order(7)
    @DisplayName("Utils: Validator")
    void utilsValidator() {
        runBenchmark("Validator", () -> {
            String input = "张三, 李四，王五";
            
            for (int i = 0; i < 50000; i++) {
                com.classroomassistant.utils.Validator.normalizeKeywords(input);
                com.classroomassistant.utils.Validator.requireRange(50, 1, 100, "test");
            }
        });
    }

    @Test
    @Order(100)
    @DisplayName("Summary / 汇总")
    void printSummary() {
        long totalTime = System.currentTimeMillis() - totalStartTime;
        
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    " + get("benchmark.summary") + "                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("  Total Tests / 总测试数: %d%n", totalTests);
        System.out.printf("  Passed / 通过: %d%n", passedTests);
        System.out.printf("  Failed / 失败: %d%n", totalTests - passedTests);
        System.out.printf("  Total Time / 总耗时: %d ms%n", totalTime);
        System.out.println();
        System.out.println(get("benchmark.complete") + " ✓");
        System.out.println();
    }

    private void runBenchmark(String name, Runnable task) {
        totalTests++;
        System.out.printf("%-25s ", name + "...");
        
        try {
            // 预热
            task.run();
            
            // 正式测试
            long start = System.nanoTime();
            task.run();
            long end = System.nanoTime();
            
            double ms = (end - start) / 1_000_000.0;
            System.out.printf("%.2f ms  [%s]%n", ms, get("benchmark.passed"));
            passedTests++;
        } catch (Exception e) {
            System.out.printf("ERROR: %s  [%s]%n", e.getMessage(), get("benchmark.failed"));
        }
    }
}
