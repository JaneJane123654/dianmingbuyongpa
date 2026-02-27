package com.classroomassistant.core.platform;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 平台文件存储接口
 * 桌面端使用标准文件系统，安卓端使用 Context.getFilesDir/getExternalFilesDir
 */
public interface PlatformStorage {
    
    /**
     * 获取应用数据目录
     * 桌面: ~/.classroom-assistant/
     * 安卓: Context.getFilesDir()
     */
    File getAppDataDir();
    
    /**
     * 获取模型存储目录
     * 桌面: ~/.classroom-assistant/models/
     * 安卓: Context.getExternalFilesDir("models")
     */
    File getModelsDir();
    
    /**
     * 获取日志目录
     */
    File getLogsDir();
    
    /**
     * 获取缓存目录
     */
    File getCacheDir();
    
    /**
     * 检查文件是否存在
     */
    boolean exists(String relativePath);
    
    /**
     * 获取文件输入流
     * @param relativePath 相对于 appDataDir 的路径
     */
    InputStream openInputStream(String relativePath) throws Exception;
    
    /**
     * 获取文件输出流
     * @param relativePath 相对于 appDataDir 的路径
     */
    OutputStream openOutputStream(String relativePath) throws Exception;
    
    /**
     * 删除文件
     */
    boolean delete(String relativePath);
    
    /**
     * 获取可用存储空间（字节）
     */
    long getAvailableSpace();
}
