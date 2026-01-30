package com.classroomassistant.utils;

/**
 * 系统通知服务
 *
 * <p>用于以跨平台方式向用户展示关键信息（如唤醒词命中、安静超时等）。
 */
public interface NotificationService {

    void showInfo(String title, String message);

    void showWarning(String title, String message);

    void showError(String title, String message);
}

