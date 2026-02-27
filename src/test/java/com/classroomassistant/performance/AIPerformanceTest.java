package com.classroomassistant.performance;

import static org.junit.jupiter.api.Assertions.*;

import com.classroomassistant.ai.AnswerListener;
import com.classroomassistant.ai.CircuitBreaker;
import com.classroomassistant.ai.LLMClient;
import com.classroomassistant.ai.LLMConfig;
import com.classroomassistant.ai.PromptTemplate;
import java.time.Duration;
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
 * AI 模块性能测试 (AI Module Performance Tests)
 *
 * <p>对 AI 问答模块进行性能基准测试，包括：
 * <ul>
 *   <li>LLM 客户端模拟调用性能</li>
 *   <li>流式回答处理性能</li>
 *   <li>熔断器高并发性能</li>
 *   <li>Prompt 模板批量构建性能</li>
 * </ul>
 *
 * <p>支持中英双语输出，通过 application.properties 中的 app.language 配置切换。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
@Tag("performance")
class AIPerformanceTest {

    private static PerformanceTestMessages msg;
    private static final int WARMUP_ITERATIONS = 500;
    private static final int BENCHMARK_ITERATIONS = 5000;

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

    // ================== LLM 客户端模拟性能测试 ==================

    @Test
    @DisplayName("LLM Client Mock Performance / LLM 客户端模拟性能")
    void llmClientMockPerformance() {
        String testName = msg.llmClientMockPerformance();
        System.out.println(msg.testStarted(testName));

        MockLLMClient client = new MockLLMClient(50); // 模拟50ms延迟
        String prompt = "这是一个测试提示词，请生成一个回答。";

        // 预热
        for (int i = 0; i < 10; i++) {
            client.generateAnswer(prompt);
        }

        int iterations = 100; // 由于模拟延迟，减少迭代次数
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String answer = client.generateAnswer(prompt);
            assertNotNull(answer);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, iterations, end - start, 0);
        System.out.println(result.toReport());

        // 模拟延迟50ms，应能达到约 15-20 次/秒
        assertTrue(result.getThroughput() > 10, msg.shouldHandleOperationsPerSecond(10));
    }

    @Test
    @DisplayName("LLM Streaming Response Performance / LLM 流式响应性能")
    void llmStreamingResponsePerformance() throws Exception {
        String testName = "LLM Streaming Response / LLM 流式响应";
        System.out.println(msg.testStarted(testName));

        MockLLMClient client = new MockLLMClient(10);
        String prompt = "这是一个测试提示词。";

        int iterations = 50;
        AtomicLong totalTokens = new AtomicLong(0);
        AtomicInteger completedCalls = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(iterations);

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            client.generateAnswerAsync(prompt, new AnswerListener() {
                @Override
                public void onToken(String token) {
                    totalTokens.incrementAndGet();
                }

                @Override
                public void onComplete(String answer) {
                    completedCalls.incrementAndGet();
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All async calls should complete");
        long end = System.nanoTime();

        double durationSeconds = (end - start) / 1_000_000_000.0;
        double tokensPerSecond = totalTokens.get() / durationSeconds;

        System.out.println("Total tokens received: " + totalTokens.get());
        System.out.println("Completed calls: " + completedCalls.get());
        System.out.println("Tokens/second: " + String.format("%.2f", tokensPerSecond));

        assertEquals(iterations, completedCalls.get(), "All calls should complete");
    }

    @Test
    @DisplayName("Circuit Breaker Concurrent Performance / 熔断器并发性能")
    void circuitBreakerConcurrentPerformance() throws Exception {
        String testName = "Circuit Breaker Concurrent / 熔断器并发";
        System.out.println(msg.testStarted(testName));

        CircuitBreaker breaker = new CircuitBreaker(5, Duration.ofSeconds(10));
        int threads = 8;
        int operationsPerThread = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalOps = new AtomicInteger(0);

        long start = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < operationsPerThread; i++) {
                        if (breaker.allowRequest()) {
                            if (random.nextBoolean()) {
                                breaker.recordSuccess();
                            } else {
                                breaker.recordFailure();
                            }
                        }
                        totalOps.incrementAndGet();
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

        assertTrue(result.getThroughput() > 100_000, msg.shouldHandleOperationsPerSecond(100_000));
    }

    @Test
    @DisplayName("Prompt Template Batch Build Performance / 提示词批量构建性能")
    void promptTemplateBatchBuildPerformance() {
        String testName = "Prompt Template Batch Build / 提示词批量构建";
        System.out.println(msg.testStarted(testName));

        PromptTemplate template = new PromptTemplate();
        
        // 准备不同长度的输入
        String[] inputs = new String[100];
        Random random = new Random();
        for (int i = 0; i < inputs.length; i++) {
            StringBuilder sb = new StringBuilder();
            int length = 100 + random.nextInt(900); // 100-1000 字符
            for (int j = 0; j < length; j++) {
                sb.append((char) ('a' + random.nextInt(26)));
            }
            inputs[i] = sb.toString();
        }

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            template.build(inputs[i % inputs.length]);
        }

        // 基准测试
        int iterations = 50000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            template.build(inputs[i % inputs.length]);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, iterations, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 50_000, msg.shouldHandleOperationsPerSecond(50_000));
    }

    @Test
    @DisplayName("LLM Config Creation Performance / LLM 配置创建性能")
    void llmConfigCreationPerformance() {
        String testName = "LLM Config Creation / LLM 配置创建";
        System.out.println(msg.testStarted(testName));

        // 预热
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            LLMConfig config = new LLMConfig();
            config.setModelType(LLMConfig.ModelType.QIANFAN);
            config.setModelName("ERNIE-Bot-turbo");
            config.setToken("test-token");
        }

        // 基准测试
        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            LLMConfig config = new LLMConfig();
            config.setModelType(LLMConfig.ModelType.values()[i % LLMConfig.ModelType.values().length]);
            config.setModelName("model-" + i);
            config.setToken("token-" + i);
            config.setStreaming(i % 2 == 0);
        }
        long end = System.nanoTime();

        PerformanceResult result = new PerformanceResult(testName, BENCHMARK_ITERATIONS, end - start, 0);
        System.out.println(result.toReport());

        assertTrue(result.getThroughput() > 100_000, msg.shouldHandleOperationsPerSecond(100_000));
    }

    // ================== Mock LLM 客户端 ==================

    private static class MockLLMClient implements LLMClient {
        private final long simulatedDelayMs;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        MockLLMClient(long simulatedDelayMs) {
            this.simulatedDelayMs = simulatedDelayMs;
        }

        @Override
        public void configure(LLMConfig config) {
            // 无操作
        }

        @Override
        public String generateAnswer(String prompt) {
            if (simulatedDelayMs > 0) {
                try {
                    Thread.sleep(simulatedDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "这是一个模拟的 AI 回答，针对您的问题：" + prompt.substring(0, Math.min(20, prompt.length())) + "...";
        }

        @Override
        public void generateAnswerAsync(String prompt, AnswerListener listener) {
            executor.submit(() -> {
                try {
                    if (simulatedDelayMs > 0) {
                        Thread.sleep(simulatedDelayMs);
                    }
                    // 模拟流式输出
                    String answer = "这是一个模拟的回答";
                    for (char c : answer.toCharArray()) {
                        listener.onToken(String.valueOf(c));
                        Thread.sleep(1);
                    }
                    listener.onComplete(answer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    listener.onError("Interrupted");
                }
            });
        }
    }
}
