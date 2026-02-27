package com.classroomassistant.benchmark;

import static com.classroomassistant.benchmark.BenchmarkMessages.*;

import com.classroomassistant.ai.CircuitBreaker;
import com.classroomassistant.ai.PromptTemplate;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * AI 模块性能基准测试 (AI Module Performance Benchmark)
 *
 * <p>测试 AI 相关组件的性能表现，包括：
 * <ul>
 *   <li>Prompt 模板构建性能</li>
 *   <li>熔断器状态检查性能</li>
 * </ul>
 *
 * <p>注意：实际 LLM API 调用性能取决于网络和服务端，不在此测试范围内。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("benchmark")
class AIBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 100000;

    @BeforeAll
    static void setup() {
        String lang = System.getProperty("benchmark.language", "zh");
        BenchmarkMessages.setLanguage(lang);
    }

    @Test
    @DisplayName("Prompt 模板构建性能 / Prompt Template Build Performance")
    void benchmarkPromptBuild() {
        System.out.println("\n" + get("benchmark.ai.title"));
        System.out.println(">>> " + get("benchmark.prompt.build"));

        PromptTemplate template = new PromptTemplate();
        String lectureText = "今天我们学习的是微积分的基本概念。首先，我们来看一下导数的定义。" +
            "导数是函数在某一点的瞬时变化率，可以用极限来定义。设函数 f(x) 在点 x0 的邻域内有定义，" +
            "当自变量 x 在 x0 处取得增量 Δx 时，函数相应地取得增量 Δy。";

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            template.build(lectureText);
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            template.build(lectureText);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.prompt.build"), BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("Prompt 模板构建（长文本）/ Prompt Template Build (Long Text)")
    void benchmarkPromptBuildLongText() {
        System.out.println("\n>>> " + get("benchmark.prompt.build") + " (Long Text)");

        PromptTemplate template = new PromptTemplate();
        
        // 构建约 4000 字符的长文本
        StringBuilder sb = new StringBuilder();
        String paragraph = "今天我们学习的是微积分的基本概念。首先，我们来看一下导数的定义。" +
            "导数是函数在某一点的瞬时变化率，可以用极限来定义。设函数 f(x) 在点 x0 的邻域内有定义，" +
            "当自变量 x 在 x0 处取得增量 Δx 时，函数相应地取得增量 Δy。这个概念非常重要。";
        for (int i = 0; i < 20; i++) {
            sb.append(paragraph);
        }
        String longText = sb.toString();

        int iterations = 10000;

        // 预热
        for (int i = 0; i < 100; i++) {
            template.build(longText);
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            template.build(longText);
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.prompt.build") + " (~4KB text)", iterations, endTime - startTime);
    }

    @Test
    @DisplayName("熔断器状态检查性能 / Circuit Breaker Check Performance")
    void benchmarkCircuitBreakerCheck() {
        System.out.println("\n>>> " + get("benchmark.circuit.check"));

        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10));

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            breaker.allowRequest();
        }

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            breaker.allowRequest();
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.circuit.check"), BENCHMARK_ITERATIONS, endTime - startTime);
    }

    @Test
    @DisplayName("熔断器状态转换性能 / Circuit Breaker State Transition Performance")
    void benchmarkCircuitBreakerTransition() {
        System.out.println("\n>>> " + get("benchmark.circuit.check") + " (State Transition)");

        int iterations = 10000;

        // 基准测试
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(10));
            breaker.allowRequest();
            breaker.recordSuccess();
            breaker.allowRequest();
            breaker.recordFailure();
            breaker.recordFailure();
            breaker.recordFailure();
            breaker.allowRequest(); // 应该返回 false
        }
        long endTime = System.nanoTime();

        printResults(get("benchmark.circuit.check") + " (Full Cycle)", iterations, endTime - startTime);
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
