package com.classroomassistant;

import com.classroomassistant.ai.LLMClient;
import com.classroomassistant.ai.LLMClientFactory;
import com.classroomassistant.audio.AudioRecorder;
import com.classroomassistant.audio.AudioRecorderDesktop;
import com.classroomassistant.runtime.HealthMonitor;
import com.classroomassistant.runtime.TaskScheduler;
import com.classroomassistant.session.ClassSessionManager;
import com.classroomassistant.speech.SpeechEngineFactory;
import com.classroomassistant.speech.SpeechServices;
import com.classroomassistant.storage.ConfigManager;
import com.classroomassistant.storage.ModelRepository;
import com.classroomassistant.storage.PreferencesManager;
import com.classroomassistant.storage.RecordingRepository;
import com.classroomassistant.storage.UserPreferences;
import com.classroomassistant.utils.AppPaths;
import com.classroomassistant.utils.NotificationService;
import com.classroomassistant.utils.PopupNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用上下文（轻量依赖装配）
 *
 * <p>负责创建并持有各模块单例对象，遵循“如无必要，勿增实体”原则，避免引入完整 DI 框架。
 */
public class AppContext {

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    private AppPaths appPaths;
    private ConfigManager configManager;
    private PreferencesManager preferencesManager;
    private TaskScheduler taskScheduler;
    private HealthMonitor healthMonitor;
    private NotificationService notificationService;
    private ModelRepository modelRepository;
    private RecordingRepository recordingRepository;

    private AudioRecorder audioRecorder;
    private SpeechServices speechServices;
    private LLMClient llmClient;
    private ClassSessionManager classSessionManager;

    public void initialize() {
        this.appPaths = new AppPaths("ClassroomAssistant");
        this.configManager = new ConfigManager(appPaths);
        this.preferencesManager = new PreferencesManager();
        this.taskScheduler = new TaskScheduler();
        this.healthMonitor = new HealthMonitor(taskScheduler);
        this.notificationService = new PopupNotificationService();
        this.modelRepository = new ModelRepository(appPaths, configManager);
        this.recordingRepository = new RecordingRepository(appPaths, configManager.getAudioConfig());

        UserPreferences userPreferences = preferencesManager.load();

        this.audioRecorder = new AudioRecorderDesktop(configManager.getAudioConfig(), healthMonitor);
        this.speechServices = new SpeechEngineFactory(modelRepository, configManager).createSpeechServices();
        this.llmClient = new LLMClientFactory(configManager, preferencesManager).create();

        this.classSessionManager =
            new ClassSessionManager(
                configManager,
                preferencesManager,
                taskScheduler,
                healthMonitor,
                notificationService,
                modelRepository,
                recordingRepository,
                audioRecorder,
                speechServices,
                llmClient
            );

        this.classSessionManager.initialize(userPreferences);

        logger.info("应用上下文初始化完成，数据目录：{}", appPaths.getUserDataDir());
    }

    public ClassSessionManager getClassSessionManager() {
        return classSessionManager;
    }

    public PreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ModelRepository getModelRepository() {
        return modelRepository;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void shutdown() {
        if (classSessionManager != null) {
            classSessionManager.shutdown();
        }
        if (audioRecorder != null) {
            audioRecorder.close();
        }
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }
}

