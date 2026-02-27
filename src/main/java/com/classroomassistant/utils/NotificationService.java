package com.classroomassistant.utils;

/**
 * 系统通知服务接口 (Notification Service Interface)
 *
 * <p>该接口定义了应用向用户展示即时通知（Toast/Popup）的标准方法。
 * 旨在通过统一的接口，以跨平台的方式提示关键事件，如“识别命中”、“安静超时”或“服务异常”。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface NotificationService {

    /**
     * 显示信息通知（通常为蓝色标识）
     *
     * @param title   通知标题
     * @param message 通知详细内容
     */
    void showInfo(String title, String message);

    /**
     * 显示警告通知（通常为橙色标识）
     *
     * @param title   通知标题
     * @param message 通知详细内容
     */
    void showWarning(String title, String message);

    /**
     * 显示警告通知，并允许在用户点击通知时执行特定回调
     *
     * @param title   通知标题
     * @param message 通知详细内容
     * @param action  用户点击或处理通知后执行的 {@link Runnable} 回调逻辑
     */
    default void showWarning(String title, String message, Runnable action) {
        showWarning(title, message);
        if (action != null) {
            action.run();
        }
    }

    /**
     * 显示错误通知（通常为红色标识）
     *
     * @param title   通知标题
     * @param message 通知详细内容
     */
    void showError(String title, String message);
}

