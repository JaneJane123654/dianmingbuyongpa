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
 * 应用上下文 (Application Context)
 *
 * <p>作为系统的“依赖注入中心”和“单例容器”。
 * 负责在系统启动时，按照正确的依赖顺序创建并装配各功能模块的单例对象。
 * 这种轻量级的手动依赖注入方式避免了引入 Spring 等大型框架的复杂性，
 * 同时也保证了组件之间的松耦合。
 *
 * @author Code Assistant
 * @date 2026-01-31
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

    /**
     * 执行全系统的初始化与组件装配
     * <p>按照从底层工具到高层业务的顺序进行实例化。
     * 1. 初始化文件路径与基础配置
     * 2. 创建任务调度与健康监控
     * 3. 准备模型资源与存储仓库
     * 4. 实例化音频采集与语音处理核心
     * 5. 组装 {@link ClassSessionManager} 业务编排器
     */
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
        this.speechServices = new SpeechEngineFactory(modelRepository, configManager, preferencesManager).createSpeechServices();
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

    /**
     * 获取核心业务编排管理器
     * @return {@link ClassSessionManager} 实例
     */
    public ClassSessionManager getClassSessionManager() {
        return classSessionManager;
    }

    /**
     * 获取用户配置管理器
     * @return {@link PreferencesManager} 实例
     */
    public PreferencesManager getPreferencesManager() {
        return preferencesManager;
    }

    /**
     * 获取全局配置管理器
     * @return {@link ConfigManager} 实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取模型资源仓库
     * @return {@link ModelRepository} 实例
     */
    public ModelRepository getModelRepository() {
        return modelRepository;
    }

    /**
     * 获取消息通知服务
     * @return {@link NotificationService} 实例
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * 优雅关闭应用上下文
     * <p>按照依赖关系的反序关闭各组件：
     * 1. 停止业务会话
     * 2. 关闭音频采集硬件连接
     * 3. 销毁线程池任务调度器
     */
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

