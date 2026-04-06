package com.classroomassistant.desktop.speech;

import com.classroomassistant.core.audio.AudioFormatSpec;
import java.io.File;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端唤醒词引擎（Sherpa JNI）。
 */
public class DesktopWakeWordEngine {

    private static final Logger logger = LoggerFactory.getLogger(DesktopWakeWordEngine.class);

    private final DesktopModelLocator modelLocator;
    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile long handle;
    private volatile long lastTriggerMs;
    private volatile Consumer<String> logSink = message -> {
    };
    private volatile BiConsumer<String, Float> wakeCallback = (keyword, confidence) -> {
    };

    public DesktopWakeWordEngine(DesktopModelLocator modelLocator) {
        this.modelLocator = modelLocator;
    }

    public boolean start(List<String> keywords,
            String preferredModelId,
            float keywordThreshold,
            Consumer<String> onLog,
            BiConsumer<String, Float> onWake) {
        stop();
        this.logSink = onLog == null ? message -> {
        } : onLog;
        this.wakeCallback = onWake == null ? (keyword, confidence) -> {
        } : onWake;

        if (keywords == null || keywords.isEmpty()) {
            log("未配置唤醒词，无法启动本地唤醒引擎");
            return false;
        }

        File kwsModelDir = modelLocator.findKwsModelDir(preferredModelId);
        if (kwsModelDir == null) {
            log("本地唤醒模型未找到：models/sherpa-onnx-kws（将保留手动触发能力）");
            return false;
        }

        String mergedKeywords = preprocessKeywords(keywords);
        if (mergedKeywords.isBlank()) {
            mergedKeywords = String.join(",", keywords);
        }
        float effectiveKeywordThreshold = toEffectiveKeywordThreshold(keywordThreshold);

        try {
            SherpaOnnxJNI.ensureLoaded();
            long localHandle = jni.initializeKws(kwsModelDir.toPath(), mergedKeywords);
            if (localHandle == 0L) {
                log("本地唤醒引擎初始化失败（JNI 返回空句柄）");
                return false;
            }
            this.handle = localHandle;
            this.running.set(true);
            this.lastTriggerMs = 0L;
            log(String.format(Locale.ROOT,
                    "本地唤醒引擎已启动: model=%s, keywordThreshold=%.2f(effective=%.2f), keywords=%s",
                    kwsModelDir.getName(),
                    keywordThreshold,
                    effectiveKeywordThreshold,
                    mergedKeywords));
            return true;
        } catch (Throwable error) {
            logger.warn("Start wake engine failed", error);
            log("本地唤醒引擎不可用: " + simplifyError(error));
            stop();
            return false;
        }
    }

    public void feedAudio(byte[] audioData, int length) {
        if (!running.get() || handle == 0L || audioData == null || length <= 0) {
            return;
        }

        float[] samples = pcm16ToFloat(audioData, length);
        if (samples.length == 0) {
            return;
        }

        try {
            boolean detected = jni.detectWakeWord(handle, samples, samples.length);
            if (!detected) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastTriggerMs < 1500L) {
                return;
            }
            lastTriggerMs = now;
            wakeCallback.accept("唤醒词", 0.9f);
        } catch (Throwable error) {
            log("本地唤醒检测异常: " + simplifyError(error));
            stop();
        }
    }

    public void stop() {
        running.set(false);
        long localHandle = handle;
        handle = 0L;
        if (localHandle == 0L) {
            return;
        }
        try {
            jni.release(localHandle);
        } catch (Throwable error) {
            logger.debug("Release wake handle failed", error);
        }
    }

    public boolean isRunning() {
        return running.get() && handle != 0L;
    }

    private float[] pcm16ToFloat(byte[] data, int length) {
        int safeLength = Math.min(length, data.length);
        int sampleCount = safeLength / AudioFormatSpec.FRAME_SIZE;
        if (sampleCount <= 0) {
            return new float[0];
        }

        float[] out = new float[sampleCount];
        int outIndex = 0;
        for (int i = 0; i + 1 < safeLength; i += 2) {
            int low = data[i] & 0xFF;
            int high = data[i + 1];
            int sample = (high << 8) | low;
            out[outIndex++] = Math.max(-1f, Math.min(1f, sample / 32768f));
        }
        return out;
    }

    private void log(String message) {
        logSink.accept(message);
    }

    private String simplifyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return error.getClass().getSimpleName();
    }

    private String preprocessKeywords(List<String> keywords) {
        LinkedHashSet<String> processed = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword == null) {
                continue;
            }
            String normalized = keyword.trim().replaceAll("\\s+", " ");
            if (normalized.isBlank()) {
                continue;
            }
            processed.add(normalized);

            String suffix = extractChineseSuffixKeyword(normalized);
            if (suffix != null && !suffix.isBlank()) {
                processed.add(suffix);
            }

            String pinyinTone = normalizeKeywordForEngine(normalized, true);
            addPinyinCandidates(processed, pinyinTone, true);

            String pinyinNoTone = normalizeKeywordForEngine(normalized, false);
            addPinyinCandidates(processed, pinyinNoTone, false);
        }
        return String.join(",", processed);
    }

    private void addPinyinCandidates(LinkedHashSet<String> processed, String normalizedPinyin, boolean keepTone) {
        if (normalizedPinyin == null || normalizedPinyin.isBlank()) {
            return;
        }
        processed.add(normalizedPinyin);
        List<String> syllables = List.of(normalizedPinyin.split("\\s+"));
        List<String> normalizedSyllables = normalizePinyinSyllables(syllables, keepTone);
        if (!normalizedSyllables.isEmpty()) {
            processed.add(String.join(" ", normalizedSyllables));
            List<String> tokenPieces = new ArrayList<>();
            for (String syllable : normalizedSyllables) {
                tokenPieces.addAll(splitPinyinSyllable(syllable));
            }
            if (!tokenPieces.isEmpty()) {
                processed.add(String.join(" ", tokenPieces));
            }
        }
    }

    private String normalizeKeywordForEngine(String keyword, boolean keepTone) {
        String transliterated = transliterateKeyword(keyword);
        String normalized = Normalizer.normalize(transliterated, Normalizer.Form.NFC);
        String processed = keepTone
                ? normalized
                : Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        String allowedPattern = keepTone ? "[^\\p{L}\\p{M}\\p{N}\\s]" : "[^\\p{L}\\p{N}\\s]";
        return processed
                .replaceAll(allowedPattern, " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String transliterateKeyword(String keyword) {
        try {
            Class<?> transliteratorClass = Class.forName("com.ibm.icu.text.Transliterator");
            Method getInstance = transliteratorClass.getMethod("getInstance", String.class);
            Object transliterator = getInstance.invoke(null, "Han-Latin");
            Method transliterate = transliteratorClass.getMethod("transliterate", String.class);
            Object output = transliterate.invoke(transliterator, keyword);
            return output == null ? keyword : String.valueOf(output);
        } catch (Throwable ignored) {
            return keyword;
        }
    }

    private String extractChineseSuffixKeyword(String keyword) {
        StringBuilder chinese = new StringBuilder();
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (ch >= 0x4E00 && ch <= 0x9FFF) {
                chinese.append(ch);
            }
        }
        if (chinese.length() < 3) {
            return null;
        }
        return chinese.substring(chinese.length() - 2);
    }

    private List<String> normalizePinyinSyllables(List<String> syllables, boolean keepTone) {
        List<String> result = new ArrayList<>();
        for (String syllable : syllables) {
            if (syllable == null || syllable.isBlank()) {
                continue;
            }
            String item = Normalizer.normalize(syllable.trim(), Normalizer.Form.NFC);
            if (!keepTone) {
                item = Normalizer.normalize(item, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            }
            if (!item.isBlank()) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> splitPinyinSyllable(String syllable) {
        String token = syllable == null ? "" : syllable.trim().toLowerCase(Locale.ROOT);
        if (token.isBlank()) {
            return List.of();
        }
        List<String> initials = List.of(
                "zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l",
                "g", "k", "h", "j", "q", "x", "r", "z", "c", "s", "y", "w");
        String initial = null;
        for (String current : initials) {
            if (token.startsWith(current)) {
                initial = current;
                break;
            }
        }
        if (initial == null || token.length() == initial.length()) {
            return List.of(token);
        }
        return List.of(initial, token.substring(initial.length()));
    }

    private float toEffectiveKeywordThreshold(float userThreshold) {
        float normalized = Math.max(0.05f, Math.min(0.8f, userThreshold));
        float remapped = normalized * 0.72f + 0.02f;
        return Math.max(0.08f, Math.min(0.45f, remapped));
    }
}
