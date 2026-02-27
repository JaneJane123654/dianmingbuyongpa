package com.classroomassistant.storage;

import com.classroomassistant.utils.AppPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 模型文件仓库 (Model Repository)
 *
 * <p>负责统一管理本地 AI 模型（KWS、ASR、VAD）的存储路径，并提供完整性检查功能。
 * 所有返回的路径均保证为绝对路径。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ModelRepository {

    private final AppPaths appPaths;
    private final ConfigManager configManager;

    /**
     * 构造模型仓库
     *
     * @param appPaths      应用路径工具
     * @param configManager 配置管理器
     * @throws NullPointerException 如果任一参数为 null
     */
    public ModelRepository(AppPaths appPaths, ConfigManager configManager) {
        this.appPaths = Objects.requireNonNull(appPaths);
        this.configManager = Objects.requireNonNull(configManager);
    }

    /**
     * 获取模型存储根目录
     *
     * @return 模型根目录的绝对路径
     */
    public Path getModelsDir() {
        return configManager.getModelsDir().toAbsolutePath();
    }

    /**
     * 获取唤醒词 (KWS) 模型目录
     *
     * @return KWS 模型目录的绝对路径
     */
    public Path getKwsModelDir() {
        return getModelsDir().resolve("sherpa-onnx-kws");
    }

    /**
     * 获取语音识别 (ASR) 模型目录
     *
     * @return ASR 模型目录的绝对路径
     */
    public Path getAsrModelDir() {
        return getModelsDir().resolve("sherpa-onnx-asr");
    }

    /**
     * 获取静音检测 (VAD) 模型文件路径
     *
     * @return VAD 模型文件的绝对路径
     */
    public Path getVadModelFile() {
        return getModelsDir().resolve("sherpa-onnx-vad").resolve("silero_vad.onnx");
    }

    /**
     * 检查核心模型文件是否存在
     *
     * @param requireSherpaModels 是否强制要求 Sherpa 模型存在（如果使用 FAKE 引擎则可不要求）
     * @return {@link ModelCheckResult} 包含检查结果和缺失项列表
     */
    public ModelCheckResult checkRequiredModels(boolean requireSherpaModels) {
        if (!requireSherpaModels) {
            return new ModelCheckResult(true, List.of());
        }

        List<String> missing = new ArrayList<>();
        if (!Files.isDirectory(getKwsModelDir())) {
            missing.add("KWS 模型目录: " + getKwsModelDir());
        }
        if (!Files.isDirectory(getAsrModelDir())) {
            missing.add("ASR 模型目录: " + getAsrModelDir());
        }
        if (!Files.exists(getVadModelFile())) {
            missing.add("VAD 模型文件: " + getVadModelFile());
        }
        return new ModelCheckResult(missing.isEmpty(), missing);
    }
}

