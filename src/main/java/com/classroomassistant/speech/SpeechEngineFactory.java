package com.classroomassistant.speech;

import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelRepository;
import com.classroomassistant.storage.PreferencesManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 语音引擎工厂 (Speech Engine Factory)
 *
 * <p>核心职责是根据应用配置（{@link ConfigManager}）动态创建和组装语音服务组件（{@link SpeechServices}）。
 * 它支持两种模式：
 * <ul>
 *   <li><b>SHERPA</b>: 使用基于 Sherpa-ONNX 的高性能本地识别引擎。</li>
 *   <li><b>API</b>: 使用云端 API（如 OpenAI Whisper、Groq）进行语音识别。</li>
 *   <li><b>FAKE</b>: 使用占位实现，不依赖外部模型文件，主要用于开发和调试。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SpeechEngineFactory {

    private static final Logger logger = LoggerFactory.getLogger(SpeechEngineFactory.class);

    private final ModelRepository modelRepository;
    private final ConfigManager configManager;
    private final PreferencesManager preferencesManager;

    /**
     * 构造工厂
     *
     * @param modelRepository    模型仓库，用于定位和检查模型文件
     * @param configManager      配置管理器，用于获取引擎类型配置
     * @param preferencesManager 偏好管理器，用于获取用户配置的 API Key
     */
    public SpeechEngineFactory(ModelRepository modelRepository, ConfigManager configManager, PreferencesManager preferencesManager) {
        this.modelRepository = Objects.requireNonNull(modelRepository, "模型仓库不能为空");
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
        this.preferencesManager = preferencesManager; // 可以为 null，此时从配置文件读取
    }

    /**
     * 构造工厂（向后兼容，不带 PreferencesManager）
     *
     * @param modelRepository 模型仓库
     * @param configManager   配置管理器
     */
    public SpeechEngineFactory(ModelRepository modelRepository, ConfigManager configManager) {
        this(modelRepository, configManager, null);
    }

    /**
     * 创建并初始化语音服务集合
     *
     * @return 组装好的 {@link SpeechServices} 实例
     */
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

        if ("API".equalsIgnoreCase(engine) || "WHISPER".equalsIgnoreCase(engine)) {
            try {
                return createApiServices();
            } catch (Exception e) {
                logger.warn("API 语音引擎初始化失败，回退到 FAKE: {}", e.getMessage());
            }
        }

        return new SpeechServices(new FakeWakeWordDetector(), new FakeSilenceDetector(), new FakeSpeechRecognizer());
    }

    /**
     * 创建基于云端 API（如 OpenAI Whisper、Groq）的语音服务
     * <p>唤醒词和静音检测仍使用本地 Fake 实现，仅 ASR 使用云端 API。
     */
    private SpeechServices createApiServices() {
        String apiUrl = configManager.getSpeechApiUrl();
        String modelName = configManager.getSpeechApiModel();

        // 优先使用用户在设置中配置的 API Key
        String apiKey = null;
        if (preferencesManager != null) {
            apiKey = preferencesManager.loadSpeechApiKey();
        }
        // 如果用户没有配置，回退到配置文件中的值
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = configManager.getSpeechApiKey();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API 语音识别需要配置 Speech API Key（在设置页面或配置文件中设置）");
        }

        var audioConfig = configManager.getAudioConfig();
        ApiSpeechRecognizer apiRecognizer = new ApiSpeechRecognizer(
            apiUrl,
            apiKey,
            modelName,
            audioConfig.sampleRate(),
            audioConfig.channels(),
            audioConfig.bitsPerSample()
        );
        apiRecognizer.initialize(null);

        logger.info("API 语音识别器已创建，端点: {}, 模型: {}", apiUrl, modelName);
        return new SpeechServices(new FakeWakeWordDetector(), new FakeSilenceDetector(), apiRecognizer);
    }

    /**
     * 创建并初始化基于 Sherpa-ONNX 的真实语音服务
     */
    private SpeechServices createSherpaServices() {
        String currentKwsModelId = "";
        if (preferencesManager != null) {
            currentKwsModelId = preferencesManager.load().getCurrentKwsModelId();
        }
        boolean kwsReady = modelRepository.isKwsModelReady(currentKwsModelId);
        boolean asrReady = Files.isDirectory(modelRepository.getAsrModelDir());
        boolean vadReady = Files.exists(modelRepository.getVadModelFile());
        if (!kwsReady || !asrReady || !vadReady) {
            throw new IllegalStateException("缺少 Sherpa 模型文件");
        }
        SherpaWakeWordDetector wakeWordDetector = new SherpaWakeWordDetector();
        SherpaSilenceDetector silenceDetector = new SherpaSilenceDetector();
        SherpaSpeechRecognizer speechRecognizer = new SherpaSpeechRecognizer();
        wakeWordDetector.initialize(modelRepository.getKwsModelDir(currentKwsModelId), "");
        silenceDetector.initialize(modelRepository.getVadModelFile());
        speechRecognizer.initialize(modelRepository.getAsrModelDir());
        return new SpeechServices(wakeWordDetector, silenceDetector, speechRecognizer);
    }

    /**
     * 唤醒词检测器的占位实现 (Mock Wake Word Detector)
     *
     * <p>仅用于演示和非生产环境下的流程测试。它不会对音频内容进行真实的特征分析，
     * 默认情况下始终返回未检测到。
     */
    private static class FakeWakeWordDetector implements WakeWordDetector {

        private final List<WakeWordListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public void initialize(Path modelDir, String keywords) {
            logger.info("初始化 Fake 唤醒词检测器");
        }

        @Override
        public boolean detect(float[] frame) {
            // 模拟逻辑：永远不会自动检测到，需通过其他手段触发
            return false;
        }

        @Override
        public void addListener(WakeWordListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        /**
         * 模拟触发唤醒通知（供调试使用）
         *
         * @param keyword 触发的关键词
         */
        @SuppressWarnings("unused")
        private void notifyDetected(String keyword) {
            for (WakeWordListener listener : listeners) {
                listener.onWakeWordDetected(keyword);
            }
        }
    }

    /**
     * 静音检测器的占位实现 (Mock Silence Detector)
     *
     * <p>通过简单的时长累计模拟静音超时逻辑。它不分析音频能量或频谱，
     * 而是根据传入的音频帧时长进行盲累加，达到阈值后即触发超时通知。
     */
    private static class FakeSilenceDetector implements SilenceDetector {

        private final List<SilenceListener> listeners = new CopyOnWriteArrayList<>();
        private int quietThresholdSeconds = 5;
        private int accumulatedMillis;

        @Override
        public void initialize(Path modelFile) {
            logger.info("初始化 Fake 静音检测器");
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

        /**
         * 触发静音超时通知
         */
        private void notifyTimeout() {
            for (SilenceListener listener : listeners) {
                listener.onSilenceTimeout();
            }
        }
    }

    /**
     * 语音识别器的占位实现 (Mock Speech Recognizer)
     *
     * <p>不进行真实的 ASR 识别，对于任何有效的音频输入，均返回预设的模拟文本。
     */
    private static class FakeSpeechRecognizer implements SpeechRecognizer {

        private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "fake-asr");
            thread.setDaemon(true);
            return thread;
        });

        @Override
        public void initialize(Path modelDir) {
            logger.info("初始化 Fake 语音识别器");
        }

        @Override
        public String recognize(byte[] pcm) {
            if (pcm == null || pcm.length == 0) {
                return "";
            }
            return "（模拟识别文本：这是一段测试文字，用于验证 AI 问答链路是否通畅。）";
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
