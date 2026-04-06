package com.classroomassistant.desktop.session;

import com.classroomassistant.core.audio.AudioFormatSpec;
import com.classroomassistant.core.platform.PlatformAudioRecorder;
import com.classroomassistant.desktop.ai.DesktopAiAnswerService;
import com.classroomassistant.desktop.model.DesktopAsrModelManager;
import com.classroomassistant.desktop.model.DesktopKwsModelManager;
import com.classroomassistant.desktop.platform.DesktopAudioRecorder;
import com.classroomassistant.desktop.platform.DesktopPlatformProvider;
import com.classroomassistant.desktop.speech.DesktopModelLocator;
import com.classroomassistant.desktop.speech.DesktopSpeechRecognitionService;
import com.classroomassistant.desktop.speech.DesktopWakeWordEngine;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 桌面端会话编排器（业务层）。
 *
 * <p>负责唤醒监听、回溯识别、AI 回答与状态通知，不直接依赖 JavaFX 组件。</p>
 */
public class DesktopMonitoringSessionManager {

    public interface Listener {
        void onRecordingStatus(String text);

        void onDetectionStatus(String text);

        void onQuietSeconds(String text);

        void onRecordingSaveStatus(String text);

        void onRecognitionText(String text);

        void onAnswerText(String text);

        void onHintText(String text);

        void onLog(String message);
    }

    private static final Logger logger = LoggerFactory.getLogger(DesktopMonitoringSessionManager.class);
    private static final DateTimeFormatter RECORDING_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final float VAD_SPEECH_RMS_THRESHOLD = 0.003f;
    private static final List<String> SIMPLE_LOG_NOISE_PREFIXES = List.of(
            "监听诊断:",
            "KWS 配置:",
            "KWS 模型目录:",
            "KWS 使用关键词文件:",
            "语音活动:",
            "ASR前置",
            "正在监听");

    private final DesktopPlatformProvider platformProvider;
    private final DesktopAudioRecorder audioRecorder;
    private final DesktopSettingsStore settingsStore;
    private final DesktopModelLocator modelLocator;
    private final DesktopKwsModelManager kwsModelManager;
    private final DesktopAsrModelManager asrModelManager;
    private final DesktopWakeWordEngine wakeWordEngine;
    private final DesktopSpeechRecognitionService speechRecognitionService;
    private final DesktopAiAnswerService aiAnswerService;

    private final ExecutorService recognitionExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "desktop-recognition-flow");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final AtomicBoolean asrInProgress = new AtomicBoolean(false);
    private final AtomicBoolean pendingWakeRecognition = new AtomicBoolean(false);
    private final AtomicLong vadQuietStartMs = new AtomicLong(0L);
    private final AtomicBoolean vadTimeoutNotified = new AtomicBoolean(false);
    private final AtomicLong vadLastTimedOutQuietDurationSec = new AtomicLong(0L);

    private volatile Listener listener;
    private volatile ScheduledExecutorService heartbeatScheduler;
    private volatile DesktopSettingsSnapshot activeSettings = DesktopSettingsSnapshot.defaults();

    public DesktopMonitoringSessionManager(DesktopPlatformProvider platformProvider) {
        this.platformProvider = Objects.requireNonNull(platformProvider, "platformProvider");
        this.audioRecorder = (DesktopAudioRecorder) platformProvider.getAudioRecorder();
        this.settingsStore = new DesktopSettingsStore(platformProvider.getPreferences(), platformProvider.getSecureStorage());
        this.modelLocator = new DesktopModelLocator(platformProvider.getStorage());
        this.kwsModelManager = new DesktopKwsModelManager(platformProvider.getStorage());
        this.asrModelManager = new DesktopAsrModelManager(platformProvider.getStorage());
        this.wakeWordEngine = new DesktopWakeWordEngine(modelLocator);
        this.speechRecognitionService = new DesktopSpeechRecognitionService();
        this.aiAnswerService = new DesktopAiAnswerService();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public DesktopSettingsSnapshot loadCurrentSettings() {
        DesktopSettingsSnapshot snapshot = settingsStore.load();
        activeSettings = snapshot;
        emitRecordingSaveStatus(snapshot);
        return snapshot;
    }

    public boolean isMonitoring() {
        return monitoring.get();
    }

    public synchronized boolean startMonitoring() {
        DesktopSettingsSnapshot settings = settingsStore.load();
        activeSettings = settings;
        emitRecordingSaveStatus(settings);

        if (!monitoring.compareAndSet(false, true)) {
            logUi("监听已在运行");
            return true;
        }

        List<String> keywords = parseKeywords(settings.getKeywords());
        if (keywords.isEmpty()) {
            monitoring.set(false);
            emitHint("请先设置唤醒词");
            emitDetectionStatus("未启动");
            emitRecordingStatus("未录音");
            logUi("未配置唤醒词，无法启动监听");
            return false;
        }

        String resolvedKwsModelId = resolveKwsModelId(settings);
        kwsModelManager.migrateLegacyModelIfNeeded(resolvedKwsModelId);
        emitRecordingStatus("准备中");
        emitDetectionStatus("下载模型中");
        emitHint("检查唤醒模型...");
        if (!ensureKwsModelReady(settings, resolvedKwsModelId)) {
            monitoring.set(false);
            emitRecordingStatus("未录音");
            emitDetectionStatus("未启动");
            emitHint("模型下载失败");
            logUi("监听启动失败：唤醒模型未就绪");
            return false;
        }

        emitRecordingStatus("录音中");
        emitDetectionStatus("初始化中");
        emitQuietSeconds("0s");
        emitHint("初始化监听链路...");

        startKeepAliveIfEnabled(settings);

        boolean wakeStarted = wakeWordEngine.start(
                keywords,
                resolvedKwsModelId,
                settings.getKwsThreshold(),
                this::logEngine,
                this::onWakeDetected);
        if (!wakeStarted) {
            logUi("本地唤醒引擎不可用，当前可使用手动触发（F8）");
        }

        boolean recorderStarted = audioRecorder.start(new PlatformAudioRecorder.AudioDataListener() {
            @Override
            public void onAudioData(byte[] data, int length) {
                if (!monitoring.get()) {
                    return;
                }
                int safeLength = Math.min(length, data.length);
                if (safeLength <= 0) {
                    return;
                }
                byte[] copy = Arrays.copyOf(data, safeLength);
                handleVad(copy);
                wakeWordEngine.feedAudio(copy, copy.length);
            }

            @Override
            public void onError(String error) {
                logUi("录音错误: " + error);
                emitHint("录音错误: " + error);
            }
        });

        if (!recorderStarted) {
            monitoring.set(false);
            stopKeepAlive();
            wakeWordEngine.stop();
            emitRecordingStatus("未录音");
            emitDetectionStatus("未启动");
            emitHint("录音设备启动失败");
            logUi("监听启动失败：录音设备不可用");
            return false;
        }

        emitDetectionStatus("监听中");
        emitHint("正在监听");
        logUi("开始本地监听，已进入唤醒检测状态");
        logEngine("监听启动生效配置: 唤醒阈值=" + String.format(Locale.ROOT, "%.2f", settings.getKwsThreshold())
                + ", 回溯=" + settings.getLookbackSeconds() + "s"
                + ", 识别=" + buildAsrRouteSummary(settings));
        return true;
    }

    public synchronized void stopMonitoring(String reason) {
        if (!monitoring.compareAndSet(true, false)) {
            return;
        }
        wakeWordEngine.stop();
        stopKeepAlive();
        audioRecorder.stop();

        asrInProgress.set(false);
        pendingWakeRecognition.set(false);
        vadQuietStartMs.set(0L);
        vadTimeoutNotified.set(false);
        vadLastTimedOutQuietDurationSec.set(0L);

        emitRecordingStatus("已停止");
        emitDetectionStatus("未启动");
        emitQuietSeconds("0s");
        emitHint("已停止");
        if (reason != null && !reason.isBlank()) {
            logUi(reason);
        }
    }

    public synchronized void reloadSettingsAndMaybeReinitialize() {
        DesktopSettingsSnapshot newSettings = settingsStore.load();
        DesktopSettingsSnapshot oldSettings = activeSettings;
        activeSettings = newSettings;
        emitRecordingSaveStatus(newSettings);

        if (monitoring.get() && !oldSettings.isMonitoringConfigEquivalent(newSettings)) {
            logUi("检测到设置变更，正在重启监听以应用新配置");
            stopMonitoring("已停止当前监听会话");
            startMonitoring();
        }
    }

    public void triggerManualRecognition() {
        if (!monitoring.get()) {
            logUi("监听未启动，无法手动触发识别");
            return;
        }
        emitDetectionStatus("手动触发");
        triggerSpeechRecognition("手动触发");
    }

    public void shutdown() {
        stopMonitoring("监听已停止");
        recognitionExecutor.shutdownNow();
    }

    private void onWakeDetected(String keyword, Float confidence) {
        if (!monitoring.get()) {
            return;
        }
        emitDetectionStatus("已唤醒");
        logUi("检测到唤醒词: " + keyword + " (conf=" + String.format(Locale.ROOT, "%.2f", confidence) + ")");
        notifyWakeAlert(activeSettings.getWakeAlertMode());
        triggerSpeechRecognition("唤醒触发");
    }

    private void triggerSpeechRecognition(String triggerReason) {
        if (!asrInProgress.compareAndSet(false, true)) {
            boolean alreadyPending = pendingWakeRecognition.getAndSet(true);
            if (!alreadyPending) {
                logUi("语音识别仍在处理中，当前识别结束后自动补跑一次");
            }
            return;
        }

        recognitionExecutor.submit(() -> {
            DesktopSettingsSnapshot startSettings = settingsStore.load();
            activeSettings = startSettings;
            try {
                int quietTimedOutDurationSec = (int) vadLastTimedOutQuietDurationSec.getAndSet(0L);
                int autoLookbackSeconds;
                if (startSettings.isVadEnabled() && startSettings.isQuietAutoLookbackEnabled()
                        && quietTimedOutDurationSec > 0) {
                    autoLookbackSeconds = Math.min(120,
                            quietTimedOutDurationSec + startSettings.getQuietAutoLookbackExtraSeconds());
                } else {
                    autoLookbackSeconds = startSettings.getLookbackSeconds();
                }
                int effectiveLookbackSeconds = Math.max(startSettings.getLookbackSeconds(), autoLookbackSeconds);

                String resolvedAsrModelId = resolveAsrModelId(startSettings);
                if (!startSettings.isCloudWhisperEnabled() && startSettings.isLocalAsrEnabled()) {
                    AsrModelPreparation preparation = ensureAsrModelReady(startSettings, resolvedAsrModelId);
                    if (!preparation.ready()) {
                        emitDetectionStatus("监听中");
                        return;
                    }
                    if (preparation.downloadedNow()) {
                        logUi("语音识别跳过：本次触发仅执行模型下载，待模型就绪后下次唤醒再识别");
                        emitDetectionStatus("监听中");
                        return;
                    }
                }

                byte[] pcm = audioRecorder.getAudioBefore(effectiveLookbackSeconds);
                if (pcm.length == 0) {
                    logUi("语音识别失败：回溯音频为空（" + effectiveLookbackSeconds + "秒）");
                    emitDetectionStatus("监听中");
                    return;
                }
                logEngine("唤醒后处理开始: trigger=" + triggerReason
                        + ", lookback=" + effectiveLookbackSeconds + "s"
                        + ", bytes=" + pcm.length);

                persistRecordingIfNeeded(pcm, startSettings);

                long asrStart = System.currentTimeMillis();
                String recognized = speechRecognitionService.recognize(
                        pcm,
                        startSettings,
                        modelLocator,
                        resolvedAsrModelId,
                        this::logEngine);
                long asrCost = System.currentTimeMillis() - asrStart;
                if (recognized.isBlank()) {
                    logUi("语音识别结果为空");
                    emitDetectionStatus("监听中");
                    return;
                }

                emitRecognitionText(recognized);
                logUi("语音识别文本: " + truncate(recognized, 200));
                logEngine("识别耗时统计: ASR=" + asrCost + "ms");

                String answer = aiAnswerService.generateAnswer(startSettings, recognized, this::logUi);
                if (!answer.isBlank()) {
                    emitAnswerText(answer);
                }
                emitDetectionStatus("监听中");
            } catch (Exception error) {
                logUi("语音识别异常: " + simplifyError(error));
                emitDetectionStatus("监听中");
            } finally {
                asrInProgress.set(false);
                if (pendingWakeRecognition.compareAndSet(true, false)) {
                    if (monitoring.get()) {
                        logUi("处理积压唤醒事件：自动补跑一次识别");
                        triggerSpeechRecognition("积压补跑");
                    } else {
                        logUi("监听已停止，丢弃积压触发");
                    }
                }
            }
        });
    }

    private void handleVad(byte[] audioData) {
        DesktopSettingsSnapshot settings = activeSettings;
        if (!settings.isVadEnabled()) {
            emitQuietSeconds("0s");
            return;
        }

        float rms = computeRms(audioData);
        long now = System.currentTimeMillis();
        if (rms < VAD_SPEECH_RMS_THRESHOLD) {
            long startedAt = vadQuietStartMs.updateAndGet(previous -> previous == 0L ? now : previous);
            long elapsedMs = Math.max(0L, now - startedAt);
            int elapsedSec = (int) (elapsedMs / 1000L);
            emitQuietSeconds(elapsedSec + "s");

            long thresholdMs = settings.getQuietThresholdSeconds() * 1000L;
            if (elapsedMs >= thresholdMs && vadTimeoutNotified.compareAndSet(false, true)) {
                int timeoutSec = Math.max(settings.getQuietThresholdSeconds(), elapsedSec);
                vadLastTimedOutQuietDurationSec.set(timeoutSec);
                logEngine("VAD: 安静超时，持续 " + timeoutSec + "s（阈值 " + settings.getQuietThresholdSeconds() + "s）");
                notifyQuietAlert(settings.getQuietAlertMode(), timeoutSec);
            }
            return;
        }

        long startedAt = vadQuietStartMs.getAndSet(0L);
        boolean hadTimeout = vadTimeoutNotified.getAndSet(false);
        emitQuietSeconds("0s");
        if (startedAt > 0L) {
            long elapsedMs = now - startedAt;
            if (hadTimeout) {
                int elapsedSec = (int) Math.max(settings.getQuietThresholdSeconds(), elapsedMs / 1000L);
                vadLastTimedOutQuietDurationSec.set(elapsedSec);
            }
            if (elapsedMs >= 1000L || hadTimeout) {
                logEngine("VAD: 检测到语音，安静结束（持续 "
                        + String.format(Locale.ROOT, "%.1f", elapsedMs / 1000f) + "s）");
            }
        }
    }

    private void notifyWakeAlert(String mode) {
        if ("SOUND".equalsIgnoreCase(mode)) {
            Toolkit.getDefaultToolkit().beep();
        }
        platformProvider.showToast("已检测到唤醒词");
    }

    private void notifyQuietAlert(String mode, int quietSeconds) {
        if ("SOUND".equalsIgnoreCase(mode)) {
            Toolkit.getDefaultToolkit().beep();
        }
        platformProvider.showToast("安静检测超时：已连续安静 " + quietSeconds + " 秒");
    }

    private void persistRecordingIfNeeded(byte[] pcm, DesktopSettingsSnapshot settings) {
        if (!settings.isRecordingSaveEnabled()) {
            return;
        }
        try {
            File appDataDir = platformProvider.getStorage().getAppDataDir();
            File recordingsDir = new File(appDataDir, "recordings");
            if (!recordingsDir.exists() && !recordingsDir.mkdirs()) {
                logUi("录音保存失败：无法创建目录 " + recordingsDir.getAbsolutePath());
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            File output = new File(recordingsDir,
                    "trigger_" + now.format(RECORDING_FILE_FORMAT) + ".wav");
            try (FileOutputStream stream = new FileOutputStream(output)) {
                stream.write(pcm16ToWav(pcm));
            }
            cleanupOldRecordings(recordingsDir, settings.getRetentionDays());
            logEngine("已保存触发录音: " + output.getName());
        } catch (IOException error) {
            logUi("录音保存失败: " + simplifyError(error));
        }
    }

    private void cleanupOldRecordings(File recordingsDir, int retentionDays) {
        if (retentionDays <= 0) {
            return;
        }
        long maxAgeMs = TimeUnit.DAYS.toMillis(retentionDays);
        long now = System.currentTimeMillis();
        File[] files = recordingsDir.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".wav"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            long age = now - file.lastModified();
            if (age <= maxAgeMs) {
                continue;
            }
            if (!file.delete()) {
                logger.debug("Delete old recording failed: {}", file.getAbsolutePath());
            }
        }
    }

    private byte[] pcm16ToWav(byte[] pcm16) {
        int channels = 1;
        int bitsPerSample = 16;
        int sampleRate = AudioFormatSpec.SAMPLE_RATE;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcm16.length;
        int riffSize = 36 + dataSize;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(out, "RIFF");
        writeIntLe(out, riffSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLe(out, 16);
        writeShortLe(out, 1);
        writeShortLe(out, channels);
        writeIntLe(out, sampleRate);
        writeIntLe(out, byteRate);
        writeShortLe(out, blockAlign);
        writeShortLe(out, bitsPerSample);
        writeAscii(out, "data");
        writeIntLe(out, dataSize);
        out.writeBytes(pcm16);
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeIntLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private void writeShortLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private void startKeepAliveIfEnabled(DesktopSettingsSnapshot settings) {
        stopKeepAlive();
        if (!settings.isBackgroundKeepAliveEnabled()) {
            return;
        }
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "desktop-monitor-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!monitoring.get()) {
                return;
            }
            logEngine("正在监听");
        }, 30, 30, TimeUnit.SECONDS);
        logUi("后台保活已启用");
    }

    private void stopKeepAlive() {
        ScheduledExecutorService scheduler = heartbeatScheduler;
        heartbeatScheduler = null;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.split("[,， ;；\\n\\t]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !result.contains(trimmed)) {
                result.add(trimmed);
            }
            String suffix = extractChineseSuffixKeyword(trimmed);
            if (suffix != null && !suffix.isBlank() && !result.contains(suffix)) {
                result.add(suffix);
            }
        }
        return result;
    }

    private String extractChineseSuffixKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        StringBuilder chineseChars = new StringBuilder();
        for (int i = 0; i < keyword.length(); i++) {
            char ch = keyword.charAt(i);
            if (ch >= 0x4E00 && ch <= 0x9FFF) {
                chineseChars.append(ch);
            }
        }
        if (chineseChars.length() < 3) {
            return null;
        }
        return chineseChars.substring(chineseChars.length() - 2);
    }

    private float computeRms(byte[] pcm16) {
        if (pcm16 == null || pcm16.length < 2) {
            return 0f;
        }
        int sampleCount = pcm16.length / 2;
        if (sampleCount <= 0) {
            return 0f;
        }

        double sum = 0d;
        for (int i = 0; i + 1 < pcm16.length; i += 2) {
            int low = pcm16[i] & 0xFF;
            int high = pcm16[i + 1];
            int sample = (high << 8) | low;
            float normalized = sample / 32768f;
            sum += normalized * normalized;
        }
        return (float) Math.sqrt(sum / sampleCount);
    }

    private String buildAsrRouteSummary(DesktopSettingsSnapshot settings) {
        String resolvedAsrModelId = resolveAsrModelId(settings);
        if (settings.isCloudWhisperEnabled() && settings.isLocalAsrEnabled()) {
            return "云端Whisper优先，本机ASR候选=" + resolvedAsrModelId;
        }
        if (settings.isCloudWhisperEnabled()) {
            return "云端Whisper";
        }
        if (settings.isLocalAsrEnabled()) {
            return "本机ASR=" + resolvedAsrModelId;
        }
        return "已关闭";
    }

    private String resolveKwsModelId(DesktopSettingsSnapshot settings) {
        if (settings == null) {
            return kwsModelManager.getDefaultModelId();
        }
        String currentModelId = settings.getCurrentKwsModelId();
        String resolved = (currentModelId == null || currentModelId.isBlank())
                ? kwsModelManager.getDefaultModelId()
                : currentModelId.trim();
        if (!settings.isCustomModelEnabled() && DesktopKwsModelManager.CUSTOM_MODEL_ID.equals(resolved)) {
            return kwsModelManager.getDefaultModelId();
        }
        return resolved;
    }

    private String resolveAsrModelId(DesktopSettingsSnapshot settings) {
        if (settings == null) {
            return asrModelManager.getDefaultModelId();
        }
        String currentModelId = settings.getLocalAsrModelId();
        String resolved = (currentModelId == null || currentModelId.isBlank())
                ? asrModelManager.getDefaultModelId()
                : currentModelId.trim();
        if (!settings.isCustomModelEnabled() && DesktopAsrModelManager.CUSTOM_MODEL_ID.equals(resolved)) {
            return asrModelManager.getDefaultModelId();
        }
        return resolved;
    }

    private boolean ensureKwsModelReady(DesktopSettingsSnapshot settings, String modelId) {
        if (kwsModelManager.isModelReady(modelId)) {
            return true;
        }
        DesktopKwsModelManager.KwsModelOption option = kwsModelManager.getOptionById(modelId);
        String modelName = option == null ? modelId : option.name();
        logUi("当前唤醒模型未就绪，准备自动下载: " + modelName + "(" + modelId + ")");

        final int[] lastPercent = {-1};
        BiConsumer<Long, Long> onProgress = (downloaded, total) -> {
            if (total <= 0) {
                emitHint("下载唤醒模型中...");
                return;
            }
            int percent = (int) ((downloaded * 100L) / total);
            if (percent == lastPercent[0]) {
                return;
            }
            lastPercent[0] = percent;
            emitHint("下载唤醒模型中 " + percent + "%");
            emitDetectionStatus("下载模型中");
        };
        Consumer<String> onEvent = event -> logUi("模型下载[" + modelName + "]: " + event);

        try {
            if (DesktopKwsModelManager.CUSTOM_MODEL_ID.equals(modelId)) {
                String customUrl = settings.getCustomKwsModelUrl();
                kwsModelManager.downloadAndPrepareFromUrl(modelId, customUrl, onProgress, onEvent);
            } else {
                kwsModelManager.downloadAndPrepare(modelId, onProgress, onEvent);
            }
            logUi("唤醒模型下载成功: " + modelName);
            return true;
        } catch (Exception error) {
            logUi("唤醒模型下载失败: " + simplifyError(error));
            return false;
        }
    }

    private AsrModelPreparation ensureAsrModelReady(DesktopSettingsSnapshot settings, String modelId) {
        if (asrModelManager.isModelReady(modelId)) {
            return new AsrModelPreparation(true, false);
        }
        DesktopAsrModelManager.AsrModelOption option = asrModelManager.getOptionById(modelId);
        String modelName = option == null ? modelId : option.name();
        logUi("本机ASR模型未就绪，准备自动下载: " + modelName + "(" + modelId + ")");

        final int[] lastPercent = {-1};
        BiConsumer<Long, Long> onProgress = (downloaded, total) -> {
            if (total <= 0) {
                emitHint("下载ASR模型中...");
                return;
            }
            int percent = (int) ((downloaded * 100L) / total);
            if (percent == lastPercent[0]) {
                return;
            }
            lastPercent[0] = percent;
            emitHint("下载ASR模型中 " + percent + "%");
        };
        Consumer<String> onEvent = event -> logUi("ASR下载[" + modelName + "]: " + event);

        try {
            if (DesktopAsrModelManager.CUSTOM_MODEL_ID.equals(modelId)) {
                String customUrl = settings.getCustomAsrModelUrl();
                asrModelManager.downloadAndPrepareFromUrl(modelId, customUrl, onProgress, onEvent);
            } else {
                asrModelManager.downloadAndPrepare(modelId, onProgress, onEvent);
            }
            logUi("本机ASR模型下载成功: " + modelName);
            return new AsrModelPreparation(true, true);
        } catch (Exception error) {
            logUi("本机ASR模型下载失败: " + simplifyError(error));
            return new AsrModelPreparation(false, false);
        }
    }

    private void emitRecordingStatus(String text) {
        Listener target = listener;
        if (target != null) {
            target.onRecordingStatus(text);
        }
    }

    private void emitDetectionStatus(String text) {
        Listener target = listener;
        if (target != null) {
            target.onDetectionStatus(text);
        }
    }

    private void emitQuietSeconds(String text) {
        Listener target = listener;
        if (target != null) {
            target.onQuietSeconds(text);
        }
    }

    private void emitRecordingSaveStatus(DesktopSettingsSnapshot settings) {
        Listener target = listener;
        if (target != null) {
            target.onRecordingSaveStatus(settings.isRecordingSaveEnabled() ? "启用" : "未启用");
        }
    }

    private void emitRecognitionText(String text) {
        Listener target = listener;
        if (target != null) {
            target.onRecognitionText(text);
        }
    }

    private void emitAnswerText(String text) {
        Listener target = listener;
        if (target != null) {
            target.onAnswerText(text);
        }
    }

    private void emitHint(String text) {
        Listener target = listener;
        if (target != null) {
            target.onHintText(text);
        }
    }

    private void logEngine(String message) {
        log(message, true);
    }

    private void logUi(String message) {
        log(message, false);
    }

    private void log(String message, boolean engine) {
        logger.info(message);
        DesktopSettingsSnapshot snapshot = activeSettings;
        if (!shouldDisplayLog(message, engine, snapshot)) {
            return;
        }
        Listener target = listener;
        if (target != null) {
            target.onLog(message);
        }
    }

    private boolean shouldDisplayLog(String message, boolean engine, DesktopSettingsSnapshot settings) {
        if (message == null || message.isBlank()) {
            return false;
        }

        if ("FULL".equalsIgnoreCase(settings.getLogMode())) {
            return true;
        }

        if (!engine) {
            String text = message.trim();
            if (text.contains("失败") || text.contains("错误") || text.contains("异常") || text.contains("HTTP 401")) {
                return true;
            }
            if (SIMPLE_LOG_NOISE_PREFIXES.stream().anyMatch(text::startsWith)) {
                return false;
            }
            return text.startsWith("开始")
                    || text.startsWith("停止")
                    || text.startsWith("检测到唤醒词")
                    || text.startsWith("语音识别文本:")
                    || text.startsWith("语音识别结果为空")
                    || text.startsWith("AI问答")
                    || text.startsWith("后台保活")
                    || text.startsWith("未配置唤醒词")
                    || text.startsWith("本地唤醒引擎")
                    || text.startsWith("监听");
        }

        if (message.startsWith("正在监听")) {
            return settings.isShowHeartbeatLogs();
        }
        if (message.startsWith("VAD:") || message.startsWith("语音活动:")) {
            return settings.isShowGainActivityLogs();
        }
        if (message.startsWith("ASR前置") || message.startsWith("唤醒后处理开始") || message.startsWith("识别耗时统计")) {
            return settings.isShowDiagnosticLogs();
        }
        if (message.startsWith("本地唤醒引擎") || message.startsWith("本机ASR") || message.startsWith("云端Whisper")) {
            return settings.isShowAudioDeviceLogs() || settings.isShowDiagnosticLogs();
        }
        return false;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength)) + "...";
    }

    private String simplifyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return error.getClass().getSimpleName();
    }

    private static final class AsrModelPreparation {

        private final boolean ready;
        private final boolean downloadedNow;

        private AsrModelPreparation(boolean ready, boolean downloadedNow) {
            this.ready = ready;
            this.downloadedNow = downloadedNow;
        }

        private boolean ready() {
            return ready;
        }

        private boolean downloadedNow() {
            return downloadedNow;
        }
    }
}
