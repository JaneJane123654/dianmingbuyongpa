package com.classroomassistant.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能测试结果记录器 (Performance Test Result Recorder)
 *
 * <p>用于记录和汇总性能测试结果，支持生成测试报告。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class PerformanceResult {

    private final String operationName;
    private final int iterations;
    private final long totalTimeNanos;
    private final long memoryUsedBytes;

    public PerformanceResult(String operationName, int iterations, long totalTimeNanos, long memoryUsedBytes) {
        this.operationName = operationName;
        this.iterations = iterations;
        this.totalTimeNanos = totalTimeNanos;
        this.memoryUsedBytes = memoryUsedBytes;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getIterations() {
        return iterations;
    }

    public long getTotalTimeNanos() {
        return totalTimeNanos;
    }

    public long getTotalTimeMs() {
        return totalTimeNanos / 1_000_000;
    }

    public double getAverageTimeNanos() {
        return iterations > 0 ? (double) totalTimeNanos / iterations : 0;
    }

    public double getAverageTimeMs() {
        return getAverageTimeNanos() / 1_000_000.0;
    }

    public double getThroughput() {
        double seconds = totalTimeNanos / 1_000_000_000.0;
        return seconds > 0 ? iterations / seconds : 0;
    }

    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    /**
     * 生成性能报告（支持中英双语）
     */
    public String toReport() {
        PerformanceTestMessages msg = MessageFactory.getMessages();
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append(msg.performanceReport()).append("\n");
        sb.append("========================================\n");
        sb.append(msg.operation()).append(": ").append(operationName).append("\n");
        sb.append(msg.iterations()).append(": ").append(iterations).append("\n");
        sb.append(msg.totalTime()).append(": ").append(getTotalTimeMs()).append(" ms\n");
        sb.append(msg.averageTimeTitle()).append(": ").append(String.format("%.3f", getAverageTimeMs())).append(" ms\n");
        sb.append(msg.throughputTitle()).append(": ").append(String.format("%.2f", getThroughput())).append(" ops/sec\n");
        if (memoryUsedBytes > 0) {
            sb.append(msg.memoryUsed(memoryUsedBytes)).append("\n");
        }
        sb.append("========================================\n");
        return sb.toString();
    }

    /**
     * 性能结果构建器
     */
    public static class Builder {
        private String operationName;
        private int iterations;
        private long startTimeNanos;
        private long endTimeNanos;
        private long memoryBefore;
        private long memoryAfter;
        private final List<Long> individualTimes = new ArrayList<>();

        public Builder operation(String name) {
            this.operationName = name;
            return this;
        }

        public Builder iterations(int count) {
            this.iterations = count;
            return this;
        }

        public Builder startTimer() {
            this.startTimeNanos = System.nanoTime();
            this.memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return this;
        }

        public Builder stopTimer() {
            this.endTimeNanos = System.nanoTime();
            this.memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return this;
        }

        public Builder recordIteration(long durationNanos) {
            individualTimes.add(durationNanos);
            return this;
        }

        public PerformanceResult build() {
            long totalTime = endTimeNanos - startTimeNanos;
            long memoryUsed = Math.max(0, memoryAfter - memoryBefore);
            return new PerformanceResult(operationName, iterations, totalTime, memoryUsed);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
