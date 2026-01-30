package com.classroomassistant.speech;

import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 语音引擎工厂
 *
 * <p>根据配置选择具体实现，当前默认使用假实现以便工程可运行。
 */
public class SpeechEngineFactory {

    private static final Logger logger = LoggerFactory.getLogger(SpeechEngineFactory.class);

    private final ModelRepository modelRepository;
    private final ConfigManager configManager;

    public SpeechEngineFactory(ModelRepository modelRepository, ConfigManager configManager) {
        this.modelRepository = Objects.requireNonNull(modelRepository, "模型仓库不能为空");
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
    }

    public SpeechServices createSpeechServices() {
        String engine = configManager.getSpeechEngineDefault();
        logger.info("当前语音引擎配置: {}", engine);
        if ("SHERPA".equalsIgnoreCase(engine)) {
            try {
                return createSherpaServices();
            } catch (Exception e) {
                logger.warn("Sherpa 引擎初始化失败，回退到 FAKE: {}", e.getMessage());
            }
        }
        return new SpeechServices(new FakeWakeWordDetector(), new FakeSilenceDetector(), new FakeSpeechRecognizer());
    }

    private SpeechServices createSherpaServices() {
        if (!modelRepository.checkRequiredModels(true).ready()) {
            throw new IllegalStateException("缺少 Sherpa 模型文件");
        }
        SherpaWakeWordDetector wakeWordDetector = new SherpaWakeWordDetector();
        SherpaSilenceDetector silenceDetector = new SherpaSilenceDetector();
        SherpaSpeechRecognizer speechRecognizer = new SherpaSpeechRecognizer();
        wakeWordDetector.initialize(modelRepository.getKwsModelDir(), "");
        silenceDetector.initialize(modelRepository.getVadModelFile());
        speechRecognizer.initialize(modelRepository.getAsrModelDir());
        return new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer);
    }

    private static class FakeWakeWordDetector implements WakeWordDetector {

        private final List<WakeWordListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public void initialize(Path modelDir, String keywords) {
            // 当前为占位实现
        }

        @Override
        public boolean detect(float[] frame) {
            return false;
        }

        @Override
        public void addListener(WakeWordListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        @SuppressWarnings("unused")
        private void notifyDetected(String keyword) {
            for (WakeWordListener listener : listeners) {
                listener.onWakeWordDetected(keyword);
            }
        }
    }

    private static class FakeSilenceDetector implements SilenceDetector {

        private final List<SilenceListener> listeners = new CopyOnWriteArrayList<>();
        private int quietThresholdSeconds = 5;
        private int accumulatedMillis;

        @Override
        public void initialize(Path modelFile) {
            // 当前为占位实现
        }

        @Override
        public boolean detect(float[] frame, int durationMillis) {
            accumulatedMillis += Math.max(0, durationMillis);
            if (accumulatedMillis >= quietThresholdSeconds * 1000L) {
                accumulatedMillis = 0;
                notifyTimeout();
            }
            return false;
        }

        @Override
        public void setQuietThresholdSeconds(int seconds) {
            if (seconds > 0) {
                this.quietThresholdSeconds = seconds;
            }
        }

        @Override
        public void addListener(SilenceListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        @SuppressWarnings("unused")
        private void notifyTimeout() {
            for (SilenceListener listener : listeners) {
                listener.onSilenceTimeout();
            }
        }
    }

    private static class FakeSpeechRecognizer implements SpeechRecognizer {

        private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "fake-asr");
            thread.setDaemon(true);
            return thread;
        });

        @Override
        public void initialize(Path modelDir) {
            // 当前为占位实现
        }

        @Override
        public String recognize(byte[] pcm) {
            if (pcm == null || pcm.length == 0) {
                return "";
            }
            return "（模拟识别文本）";
        }

        @Override
        public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
            executor.submit(() -> {
                if (listener != null) {
                    listener.onResult(recognize(pcm));
                }
            });
        }
    }
}
