package com.classroomassistant.performance;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 性能测试消息工厂 (Performance Test Messages Factory)
 *
 * <p>根据配置文件中的语言设置，返回对应语言版本的消息实现。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
public class MessageFactory {

    private static final String CONFIG_FILE = "/config/application.properties";
    private static final String LANGUAGE_KEY = "app.language";
    private static final String DEFAULT_LANGUAGE = "zh";

    private static volatile PerformanceTestMessages instance;
    private static volatile String currentLanguage;

    private MessageFactory() {
        // 私有构造函数
    }

    /**
     * 获取当前语言配置对应的消息实例
     *
     * @return 消息实例
     */
    public static PerformanceTestMessages getMessages() {
        String language = loadLanguage();
        if (instance == null || !language.equals(currentLanguage)) {
            synchronized (MessageFactory.class) {
                if (instance == null || !language.equals(currentLanguage)) {
                    currentLanguage = language;
                    instance = createMessages(language);
                }
            }
        }
        return instance;
    }

    /**
     * 获取指定语言的消息实例
     *
     * @param language 语言代码 (zh/en)
     * @return 消息实例
     */
    public static PerformanceTestMessages getMessages(String language) {
        return createMessages(language);
    }

    /**
     * 从配置文件加载语言设置
     */
    private static String loadLanguage() {
        try (InputStream inputStream = MessageFactory.class.getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                return properties.getProperty(LANGUAGE_KEY, DEFAULT_LANGUAGE);
            }
        } catch (IOException e) {
            // 忽略异常，使用默认语言
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * 根据语言代码创建消息实例
     */
    private static PerformanceTestMessages createMessages(String language) {
        if ("en".equalsIgnoreCase(language)) {
            return new EnglishMessages();
        }
        return new ChineseMessages();
    }

    /**
     * 重置缓存（用于测试）
     */
    public static void reset() {
        synchronized (MessageFactory.class) {
            instance = null;
            currentLanguage = null;
        }
    }
}
