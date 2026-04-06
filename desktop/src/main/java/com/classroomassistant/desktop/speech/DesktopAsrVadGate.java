package com.classroomassistant.desktop.speech;

import com.classroomassistant.core.audio.AudioFormatSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 轻量 ASR 前置 VAD 过滤。
 */
public class DesktopAsrVadGate {

    public static final class Result {
        public final byte[] filteredPcm16;
        public final boolean hasSpeech;
        public final int originalDurationMs;
        public final int keptDurationMs;
        public final float speechRatio;
        public final float maxRms;
        public final float thresholdRms;

        Result(byte[] filteredPcm16,
                boolean hasSpeech,
                int originalDurationMs,
                int keptDurationMs,
                float speechRatio,
                float maxRms,
                float thresholdRms) {
            this.filteredPcm16 = filteredPcm16;
            this.hasSpeech = hasSpeech;
            this.originalDurationMs = originalDurationMs;
            this.keptDurationMs = keptDurationMs;
            this.speechRatio = speechRatio;
            this.maxRms = maxRms;
            this.thresholdRms = thresholdRms;
        }

        public String summary() {
            return String.format(Locale.ROOT,
                    "VAD: speech=%s kept=%dms/%dms ratio=%.1f%% maxRms=%.4f threshold=%.4f",
                    hasSpeech ? "yes" : "no",
                    keptDurationMs,
                    originalDurationMs,
                    speechRatio * 100f,
                    maxRms,
                    thresholdRms);
        }
    }

    private static final int FRAME_MS = 30;
    private static final int PRE_PADDING_FRAMES = 2;
    private static final int POST_PADDING_FRAMES = 4;

    public Result filter(byte[] pcm16) {
        if (pcm16 == null || pcm16.length == 0) {
            return new Result(new byte[0], false, 0, 0, 0f, 0f, 0f);
        }

        int frameBytes = AudioFormatSpec.SAMPLE_RATE * AudioFormatSpec.FRAME_SIZE * FRAME_MS / 1000;
        int frameCount = Math.max(1, (pcm16.length + frameBytes - 1) / frameBytes);
        float[] rmsValues = new float[frameCount];
        int[] startBytes = new int[frameCount];
        int[] endBytes = new int[frameCount];

        float maxRms = 0f;
        for (int i = 0; i < frameCount; i++) {
            int start = i * frameBytes;
            int end = Math.min(start + frameBytes, pcm16.length);
            startBytes[i] = start;
            endBytes[i] = end;
            float rms = computeRms(pcm16, start, end);
            rmsValues[i] = rms;
            if (rms > maxRms) {
                maxRms = rms;
            }
        }

        float noiseFloor = percentile(rmsValues, 0.2f);
        float threshold = Math.max(0.003f, Math.min(0.05f, noiseFloor * 3f + 0.002f));
        if (maxRms < threshold * 1.2f) {
            int durationMs = durationMs(pcm16.length);
            return new Result(new byte[0], false, durationMs, 0, 0f, maxRms, threshold);
        }

        List<int[]> ranges = new ArrayList<>();
        int currentStartFrame = -1;
        int lastSpeechFrame = -1;
        for (int i = 0; i < frameCount; i++) {
            boolean speech = rmsValues[i] >= threshold;
            if (speech) {
                if (currentStartFrame < 0) {
                    currentStartFrame = i;
                }
                lastSpeechFrame = i;
            } else if (currentStartFrame >= 0 && lastSpeechFrame >= currentStartFrame) {
                if (i - lastSpeechFrame > 3) {
                    int paddedStart = Math.max(0, currentStartFrame - PRE_PADDING_FRAMES);
                    int paddedEnd = Math.min(frameCount - 1, lastSpeechFrame + POST_PADDING_FRAMES);
                    ranges.add(new int[] {startBytes[paddedStart], endBytes[paddedEnd]});
                    currentStartFrame = -1;
                    lastSpeechFrame = -1;
                }
            }
        }
        if (currentStartFrame >= 0 && lastSpeechFrame >= currentStartFrame) {
            int paddedStart = Math.max(0, currentStartFrame - PRE_PADDING_FRAMES);
            int paddedEnd = Math.min(frameCount - 1, lastSpeechFrame + POST_PADDING_FRAMES);
            ranges.add(new int[] {startBytes[paddedStart], endBytes[paddedEnd]});
        }

        if (ranges.isEmpty()) {
            int durationMs = durationMs(pcm16.length);
            return new Result(new byte[0], false, durationMs, 0, 0f, maxRms, threshold);
        }

        List<int[]> merged = mergeRanges(ranges);
        int keptBytes = 0;
        for (int[] range : merged) {
            keptBytes += Math.max(0, range[1] - range[0]);
        }
        if (keptBytes <= 0) {
            int durationMs = durationMs(pcm16.length);
            return new Result(new byte[0], false, durationMs, 0, 0f, maxRms, threshold);
        }

        byte[] out = new byte[keptBytes];
        int offset = 0;
        for (int[] range : merged) {
            int len = Math.max(0, range[1] - range[0]);
            if (len <= 0) {
                continue;
            }
            System.arraycopy(pcm16, range[0], out, offset, len);
            offset += len;
        }

        int originalMs = durationMs(pcm16.length);
        int keptMs = durationMs(out.length);
        float ratio = originalMs <= 0 ? 0f : Math.min(1f, Math.max(0f, keptMs / (float) originalMs));
        return new Result(out, true, originalMs, keptMs, ratio, maxRms, threshold);
    }

    private List<int[]> mergeRanges(List<int[]> ranges) {
        ranges.sort((left, right) -> Integer.compare(left[0], right[0]));
        List<int[]> merged = new ArrayList<>();
        int[] current = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            int[] next = ranges.get(i);
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private float computeRms(byte[] pcm16, int start, int endExclusive) {
        int validStart = Math.max(0, start);
        int validEnd = Math.min(pcm16.length, endExclusive);
        int sampleCount = Math.max(0, (validEnd - validStart) / AudioFormatSpec.FRAME_SIZE);
        if (sampleCount <= 0) {
            return 0f;
        }

        double sum = 0d;
        for (int i = validStart; i + 1 < validEnd; i += 2) {
            int low = pcm16[i] & 0xFF;
            int high = pcm16[i + 1];
            int sample = (high << 8) | low;
            float normalized = sample / 32768f;
            sum += normalized * normalized;
        }
        return (float) Math.sqrt(sum / sampleCount);
    }

    private float percentile(float[] values, float percentile) {
        if (values.length == 0) {
            return 0f;
        }
        float[] copy = values.clone();
        java.util.Arrays.sort(copy);
        int index = Math.min(copy.length - 1, Math.max(0, Math.round((copy.length - 1) * percentile)));
        return copy[index];
    }

    private int durationMs(int bytes) {
        if (bytes <= 0) {
            return 0;
        }
        double samples = bytes / (double) AudioFormatSpec.FRAME_SIZE;
        return (int) Math.round(samples / AudioFormatSpec.SAMPLE_RATE * 1000.0d);
    }
}
